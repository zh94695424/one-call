package com.onecall.core.operations_iam.internal.catalogread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import org.jooq.DSLContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultOperationsCatalogProductReadService implements OperationsCatalogProductReadService {
    private final TransactionTemplate transaction;
    private final DSLContext dsl;
    private final OperationsReadContext context;
    private final ObjectProvider<OperationsSubjectReadProperties> properties;
    private final ObjectProvider<CatalogProductReader> readers;
    private final LongSupplier nanoTime;

    public DefaultOperationsCatalogProductReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<CatalogProductReader> readers) {
        this(transaction, dsl, context, properties, readers, System::nanoTime);
    }
    DefaultOperationsCatalogProductReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<CatalogProductReader> readers, LongSupplier nanoTime) {
        this.transaction = transaction; this.dsl = dsl; this.context = context; this.properties = properties; this.readers = readers;
        this.nanoTime = nanoTime;
    }
    @Override public CatalogProductPage list(Caller caller, RawListRequest raw) {
        return execute(caller, deadline -> {
            var request = OperationsCatalogProductReadRequest.parse(raw);
            checkBudget(deadline);
            var codec = new OperationsCatalogProductCursorCodec(one(properties));
            Optional<Anchor> anchor = request.cursor() == null ? Optional.empty() : Optional.of(codec.decode(request.cursor(), request));
            var reader = one(readers);
            checkBudget(deadline);
            var roots = reader.roots(anchor, request.limit() + 1);
            checkBudget(deadline);
            require(roots != null && roots.size() <= request.limit() + 1);
            Anchor previous = anchor.orElse(null);
            var ids = new HashSet<UUID>();
            for (var root : roots) {
                validate(root);
                var current = new Anchor(root.productCode(), root.productId());
                require(ids.add(root.productId()) && (previous == null || compare(previous, current) < 0));
                previous = current;
            }
            boolean hasMore = roots.size() > request.limit();
            var emitted = roots.subList(0, Math.min(roots.size(), request.limit()));
            var productIds = emitted.stream().map(ProductFacts::productId).toList();
            checkBudget(deadline);
            var versions = productIds.isEmpty() ? List.<VersionFacts>of() : reader.summaries(productIds);
            checkBudget(deadline);
            var grouped = summaries(versions, Set.copyOf(productIds));
            var items = emitted.stream().map(root -> new CatalogProductSummary(root.productId(), root.productCode(), root.productFamily(),
                    grouped.getOrDefault(root.productId(), List.of()))).toList();
            String cursor = null;
            if (hasMore) {
                checkBudget(deadline);
                cursor = codec.encode(new Anchor(emitted.getLast().productCode(), emitted.getLast().productId()), request);
            }
            return new CatalogProductPage(items, cursor, hasMore);
        });
    }
    @Override public CatalogProductDetail detail(Caller caller, RawDetailRequest raw) {
        return execute(caller, deadline -> {
            UUID productId = OperationsCatalogProductReadRequest.detail(raw);
            var reader = one(readers);
            checkBudget(deadline);
            var root = reader.product(productId);
            checkBudget(deadline);
            if (root == null) throw new Failure(Code.NOT_FOUND);
            validate(root); require(productId.equals(root.productId()));
            checkBudget(deadline);
            var versions = reader.versions(productId);
            checkBudget(deadline);
            summaries(versions, Set.of(productId));
            var versionIds = versions.stream().map(VersionFacts::catalogVersionId).toList();
            checkBudget(deadline);
            var benefits = versionIds.isEmpty() ? List.<BenefitFacts>of() : reader.benefits(versionIds);
            checkBudget(deadline);
            require(benefits != null);
            var grouped = new HashMap<UUID, List<CatalogBenefitLine>>();
            var previousIds = new HashMap<UUID, UUID>();
            var entitlementTypes = new HashSet<String>();
            for (var benefit : benefits) {
                require(benefit != null && benefit.benefitId() != null && versionIds.contains(benefit.catalogVersionId())
                        && benefit.entitlementType() != null && Set.of("RECORDING_SECONDS", "AI_SECONDS", "EVIDENCE_COUNT", "STORAGE_BYTES").contains(benefit.entitlementType())
                        && benefit.quantity() != null && benefit.quantity() >= 1 && benefit.validityDays() != null && benefit.validityDays() >= 1
                        && benefit.grantIntervalMonths() != null && benefit.grantIntervalMonths() >= 0);
                UUID previous = previousIds.put(benefit.catalogVersionId(), benefit.benefitId());
                require((previous == null || compareUuid(previous, benefit.benefitId()) < 0)
                        && entitlementTypes.add(benefit.catalogVersionId() + ":" + benefit.entitlementType()));
                grouped.computeIfAbsent(benefit.catalogVersionId(), ignored -> new ArrayList<>()).add(new CatalogBenefitLine(
                        benefit.entitlementType(), benefit.quantity(), benefit.validityDays(), benefit.grantIntervalMonths()));
            }
            var items = new ArrayList<CatalogVersionSummary>();
            for (var version : versions) {
                validateDetail(version);
                items.add(new CatalogVersionSummary(version.catalogVersionId(), version.versionNo(), version.saleStatus(), version.offerType(),
                        version.audience(), version.priceCents(), version.currency(), version.billingCycle(), version.installments(),
                        grouped.getOrDefault(version.catalogVersionId(), List.of()), version.publishedAt(), version.revision()));
            }
            return new CatalogProductDetail(root.productId(), root.productCode(), root.productFamily(), items);
        });
    }
    private <Result> Result execute(Caller caller, LongFunction<Result> action) {
        long deadline = nanoTime.getAsLong() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        try {
            Result result = transaction.execute(status -> {
                try {
                    checkBudget(deadline);
                    context.enter();
                    checkBudget(deadline);
                    authority(caller);
                    checkBudget(deadline);
                    return action.apply(deadline);
                } finally { context.exit(); }
            });
            require(result != null);
            return result;
        } catch (Failure known) { throw known; }
        catch (org.jooq.exception.DataAccessException | org.springframework.dao.DataAccessException | TransactionException | BeansException failure) {
            throw unavailable();
        } catch (RuntimeException unexpected) { throw new Failure(Code.INTERNAL); }
    }
    private void checkBudget(long deadline) { if (deadline - nanoTime.getAsLong() <= 0) throw unavailable(); }
    private void authority(Caller caller) {
        if (caller == null || caller.employeeId() == null || caller.audience() == null || caller.audience().isBlank()) throw unavailable();
        if (!"onecall:employee-session:v1".equals(caller.audience())) throw new Failure(Code.UNAUTHENTICATED);
        var row = dsl.fetchOne("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)", caller.employeeId());
        if (row == null) throw new Failure(Code.UNAUTHENTICATED);
        String status = row.get("status", String.class);
        String role = row.get("role", String.class);
        Integer revision = row.get("revision", Integer.class);
        require(caller.employeeId().equals(row.get("id", UUID.class)) && status != null && Set.of("ACTIVE", "DISABLED").contains(status)
                && role != null && Set.of("ADMIN", "OPS").contains(role) && revision != null && revision >= 1);
        if ("DISABLED".equals(status)) throw new Failure(Code.FORBIDDEN);
    }
    private static <Port> Port one(ObjectProvider<Port> provider) {
        try {
            var candidates = provider.stream().limit(2).toList();
            if (candidates.size() != 1 || candidates.getFirst() == null) throw unavailable();
            return candidates.getFirst();
        } catch (RuntimeException failure) { throw unavailable(); }
    }
    private static Map<UUID, List<CatalogProductVersionSummary>> summaries(List<VersionFacts> versions, Set<UUID> productIds) {
        require(versions != null);
        var grouped = new HashMap<UUID, List<CatalogProductVersionSummary>>();
        var identities = new HashSet<UUID>();
        var activeProducts = new HashSet<UUID>();
        VersionFacts previous = null;
        for (var version : versions) {
            require(version != null && version.productId() != null && productIds.contains(version.productId()) && version.catalogVersionId() != null
                    && identities.add(version.catalogVersionId()) && version.versionNo() != null && version.versionNo() >= 1
                    && version.saleStatus() != null && Set.of("DRAFT", "ACTIVE", "OFFSALE").contains(version.saleStatus())
                    && version.offerType() != null && Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE").contains(version.offerType())
                    && version.audience() != null && Set.of("PERSONAL", "ENTERPRISE").contains(version.audience())
                    && ("ENTERPRISE".equals(version.audience()) == "QUOTE".equals(version.offerType())));
            if ("ACTIVE".equals(version.saleStatus())) require(activeProducts.add(version.productId()));
            if (previous != null) {
                int productOrder = compareUuid(previous.productId(), version.productId());
                require(productOrder < 0 || productOrder == 0 && previous.versionNo() < version.versionNo());
            }
            grouped.computeIfAbsent(version.productId(), ignored -> new ArrayList<>()).add(new CatalogProductVersionSummary(
                    version.catalogVersionId(), version.versionNo(), version.saleStatus(), version.offerType(), version.audience()));
            previous = version;
        }
        return grouped;
    }
    private static void validate(ProductFacts product) {
        require(product != null && product.productId() != null && product.productCode() != null && product.productCode().matches("[A-Z][A-Z0-9_-]{1,63}")
                && product.productFamily() != null && product.productFamily().matches("[A-Z][A-Z0-9_]{0,63}"));
    }
    private static void validateDetail(VersionFacts version) {
        require("CNY".equals(version.currency()) && version.revision() != null && version.revision() >= 1
                && ("DRAFT".equals(version.saleStatus()) == (version.publishedAt() == null))
                && (version.publishedAt() == null || version.publishedAt().getNano() % 1000 == 0)
                && ("QUOTE".equals(version.offerType()) == (version.priceCents() == null))
                && (version.priceCents() == null || version.priceCents() >= 0)
                && version.installments() != null && version.installments() >= 1 && version.grantIntervalMonths() != null);
        if ("SUBSCRIPTION".equals(version.offerType())) {
            require(version.billingCycle() != null && Set.of("MONTHLY", "QUARTERLY", "ANNUAL").contains(version.billingCycle()) && version.grantIntervalMonths() >= 1);
        } else {
            require(("QUOTE".equals(version.offerType()) ? "QUOTE" : "ONCE").equals(version.billingCycle())
                    && version.installments() == 1 && version.grantIntervalMonths() == 0);
        }
    }
    private static int compare(Anchor left, Anchor right) {
        int code = Arrays.compareUnsigned(left.productCode().getBytes(StandardCharsets.UTF_8), right.productCode().getBytes(StandardCharsets.UTF_8));
        return code == 0 ? compareUuid(left.productId(), right.productId()) : code;
    }
    private static int compareUuid(UUID left, UUID right) {
        int most = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        return most == 0 ? Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits()) : most;
    }
    private static void require(boolean valid) { if (!valid) throw unavailable(); }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
