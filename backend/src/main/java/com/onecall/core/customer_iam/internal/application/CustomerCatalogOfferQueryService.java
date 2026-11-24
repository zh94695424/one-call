package com.onecall.core.customer_iam.internal.application;

import static com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.*;

import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.Anchor;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.BenefitRow;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.OfferRow;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.Scope;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionTemplate;

public final class CustomerCatalogOfferQueryService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final JooqCustomerBusinessNumberAccessRepository lifecycle;
    private final CustomerCatalogOfferReader reader;

    public CustomerCatalogOfferQueryService(TransactionTemplate transaction, DSLContext context,
            JooqCustomerBusinessNumberAccessRepository lifecycle, CustomerCatalogOfferReader reader) {
        this.transaction = transaction;
        this.context = context;
        this.lifecycle = lifecycle;
        this.reader = reader;
    }

    public record Request(Optional<Anchor> anchor, Optional<Integer> limit, Optional<String> channel,
            Optional<String> audience, Optional<String> offerType) {
        public Request {
            Objects.requireNonNull(anchor);
            Objects.requireNonNull(limit);
            Objects.requireNonNull(channel);
            Objects.requireNonNull(audience);
            Objects.requireNonNull(offerType);
            if (limit.isPresent() && (limit.get() < 1 || limit.get() > 100))
                throw new CustomerContextException(VALIDATION_FAILED);
        }
        public int effectiveLimit() { return limit.orElse(20); }
    }

    public record Item(String productCode, UUID catalogVersionId, UUID catalogOfferId, String channel,
            int versionNo, String offerType, String audience, Long priceCents, String currency,
            String billingCycle, int installments, List<BenefitLine> benefitLines, String versionToken) {
        public Item { benefitLines = List.copyOf(benefitLines); }
    }

    public record BenefitLine(String entitlementType, long quantity, int validityDays, int grantIntervalMonths) { }

    public record Page(List<Item> items, Optional<Anchor> nextAnchor, boolean hasMore) {
        public Page { items = List.copyOf(items); }
    }

    public Page listCustomerCatalogOffers(TrustedCustomerCaller caller, Request request) {
        var scope = CustomerBusinessNumberQueryService.requireScope(caller);
        if (request == null) throw new CustomerContextException(VALIDATION_FAILED);
        if (caller.channel() == null) throw new CustomerContextException(SERVICE_UNAVAILABLE);
        String trustedChannel = caller.channel().name();
        String trustedAudience = caller.selectedWorkspace().workspaceType() == CustomerContext.WorkspaceType.PERSONAL
                ? "PERSONAL" : "ENTERPRISE";
        if (request.channel().isPresent() && !request.channel().get().equals(trustedChannel)
                || request.audience().isPresent() && !request.audience().get().equals(trustedAudience))
            throw new CustomerContextException(FORBIDDEN);
        try {
            Page page = transaction.execute(status -> {
                try {
                    context.execute("SET LOCAL ROLE customer_iam_role");
                    var role = context.fetchOne("select current_user");
                    if (role == null || !"customer_iam_role".equals(role.get(0, String.class)))
                        throw new CustomerContextException(SERVICE_UNAVAILABLE);
                } catch (RuntimeException failure) {
                    throw new CustomerContextException(SERVICE_UNAVAILABLE);
                }
                lifecycle.requireActiveContext(scope);
                return compose(trustedChannel, trustedAudience, request);
            });
            if (page == null) throw new CustomerContextException(INTERNAL);
            return page;
        } catch (RuntimeException failure) {
            if (failure instanceof CustomerContextException known) throw new CustomerContextException(known.code());
            Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException || cause instanceof org.springframework.dao.DataAccessException
                        || cause instanceof org.jooq.exception.DataAccessException
                        || cause instanceof org.springframework.transaction.TransactionException
                        || cause instanceof org.springframework.beans.BeansException)
                    throw new CustomerContextException(SERVICE_UNAVAILABLE);
            }
            throw new CustomerContextException(INTERNAL);
        }
    }

    private Page compose(String channel, String audience, Request request) {
        var rows = reader.fetchOffers(new Scope(channel, audience, request.offerType()), request.anchor(), request.effectiveLimit() + 1);
        if (rows == null || rows.size() > request.effectiveLimit() + 1) throw new CustomerContextException(INTERNAL);
        Set<UUID> offers = new HashSet<>();
        Anchor previous = request.anchor().orElse(null);
        for (var row : rows) {
            if (row == null || row.productCode() == null || !row.productCode().matches("^[A-Z][A-Z0-9_-]{1,63}$")
                    || row.catalogVersionId() == null || row.catalogOfferId() == null || !offers.add(row.catalogOfferId())
                    || row.versionNo() < 1 || !channel.equals(row.channel()) || !audience.equals(row.audience())
                    || !validOffer(row)) throw new CustomerContextException(INTERNAL);
            Anchor current = new Anchor(row.productCode(), row.versionNo(), row.catalogOfferId());
            if (previous != null && compare(current, previous) <= 0) throw new CustomerContextException(INTERNAL);
            previous = current;
        }
        boolean more = rows.size() > request.effectiveLimit();
        var returned = rows.subList(0, Math.min(rows.size(), request.effectiveLimit()));
        Set<UUID> versions = new HashSet<>();
        for (var row : returned) versions.add(row.catalogVersionId());
        Map<UUID, List<BenefitRow>> benefits = versions.isEmpty() ? Map.of() : reader.fetchBenefits(versions);
        if (benefits == null || !versions.containsAll(benefits.keySet())) throw new CustomerContextException(INTERNAL);
        for (var entry : benefits.entrySet()) {
            var lines = entry.getValue();
            if (lines == null) throw new CustomerContextException(INTERNAL);
            Set<String> types = new HashSet<>();
            String previousType = null;
            for (var line : lines) {
                if (line == null || !entry.getKey().equals(line.catalogVersionId()) || line.entitlementType() == null
                        || !Set.of("RECORDING_SECONDS", "AI_SECONDS", "EVIDENCE_COUNT", "STORAGE_BYTES").contains(line.entitlementType())
                        || !types.add(line.entitlementType()) || line.quantity() < 1 || line.validityDays() < 1 || line.grantIntervalMonths() < 0
                        || previousType != null && line.entitlementType().compareTo(previousType) <= 0)
                    throw new CustomerContextException(INTERNAL);
                previousType = line.entitlementType();
            }
        }
        List<Item> items = new ArrayList<>();
        for (var row : returned) {
            List<BenefitLine> lines = new ArrayList<>();
            for (var benefit : benefits.getOrDefault(row.catalogVersionId(), List.of()))
                lines.add(new BenefitLine(benefit.entitlementType(), benefit.quantity(), benefit.validityDays(), benefit.grantIntervalMonths()));
            items.add(new Item(row.productCode(), row.catalogVersionId(), row.catalogOfferId(), row.channel(), row.versionNo(),
                    row.offerType(), row.audience(), row.priceCents(), row.currency(), row.billingCycle(), row.installments(),
                    lines, row.versionToken()));
        }
        Optional<Anchor> next = more ? Optional.of(new Anchor(returned.getLast().productCode(), returned.getLast().versionNo(),
                returned.getLast().catalogOfferId())) : Optional.empty();
        return new Page(items, next, more);
    }

    private static boolean validOffer(OfferRow row) {
        if (row.offerType() == null || !Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE").contains(row.offerType())) return false;
        if (!"CNY".equals(row.currency())) return false;
        if (row.versionToken() == null || row.versionToken().isEmpty() || row.versionToken().length() > 128) return false;
        if (row.billingCycle() == null) return false;
        if ("ENTERPRISE".equals(row.audience()) != "QUOTE".equals(row.offerType())) return false;
        return switch (row.offerType()) {
            case "SUBSCRIPTION" -> Set.of("MONTHLY", "QUARTERLY", "ANNUAL").contains(row.billingCycle())
                    && row.installments() >= 1 && row.priceCents() != null && row.priceCents() >= 0;
            case "ONE_TIME", "ADD_ON" -> "ONCE".equals(row.billingCycle()) && row.installments() == 1
                    && row.priceCents() != null && row.priceCents() >= 0;
            case "QUOTE" -> "QUOTE".equals(row.billingCycle()) && row.installments() == 1
                    && row.priceCents() == null && "ENTERPRISE".equals(row.audience());
            default -> false;
        };
    }

    private static int compare(Anchor current, Anchor previous) {
        int code = current.productCode().compareTo(previous.productCode());
        if (code != 0) return code;
        int version = Integer.compare(current.versionNo(), previous.versionNo());
        if (version != 0) return -version;
        return current.catalogOfferId().toString().compareTo(previous.catalogOfferId().toString());
    }
}
