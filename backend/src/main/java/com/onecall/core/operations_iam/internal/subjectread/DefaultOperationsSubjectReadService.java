package com.onecall.core.operations_iam.internal.subjectread;

import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultOperationsSubjectReadService implements OperationsSubjectReadService {
    private static final String AUDIENCE = "onecall:employee-session:v1";
    private static final long REQUEST_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final Set<String> ACTIVATION_STATUSES = Set.of("INACTIVE", "ACTIVE", "SUSPENDED");
    private static final Set<String> CLAIM_STATUSES = Set.of("ACCEPTED", "REJECTED", "CONFLICT");
    private final TransactionTemplate transaction;
    private final DSLContext dsl;
    private final OperationsReadContext context;
    private final OperationsSubjectReadProperties properties;
    private final ObjectProvider<SubjectReader> subjects;
    private final ObjectProvider<ActivationReader> activations;
    private final ObjectProvider<TrialReader> trials;
    private final ObjectProvider<EvidenceCounter> evidence;
    private final ObjectProvider<OrderCounter> orders;
    private final LongSupplier nanoTime;

    public DefaultOperationsSubjectReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            OperationsSubjectReadProperties properties, ObjectProvider<SubjectReader> subjects, ObjectProvider<ActivationReader> activations,
            ObjectProvider<TrialReader> trials, ObjectProvider<EvidenceCounter> evidence, ObjectProvider<OrderCounter> orders) {
        this(transaction, dsl, context, properties, subjects, activations, trials, evidence, orders, System::nanoTime);
    }

    DefaultOperationsSubjectReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            OperationsSubjectReadProperties properties, ObjectProvider<SubjectReader> subjects, ObjectProvider<ActivationReader> activations,
            ObjectProvider<TrialReader> trials, ObjectProvider<EvidenceCounter> evidence, ObjectProvider<OrderCounter> orders,
            LongSupplier nanoTime) {
        this.transaction = transaction; this.dsl = dsl; this.context = context; this.properties = properties;
        this.subjects = subjects; this.activations = activations; this.trials = trials; this.evidence = evidence; this.orders = orders;
        this.nanoTime = nanoTime;
    }

    @Override public SubjectPage list(Caller caller, RawListRequest raw) {
        return read(caller, (now, deadline) -> {
            var request = OperationsSubjectReadRequest.list(raw);
            var codec = new OperationsSubjectCursorCodec(properties);
            byte[] digest = request.digest();
            Optional<Anchor> anchor = request.cursor() == null ? Optional.empty() : Optional.of(codec.decode(request.cursor(), digest));
            SubjectReader subjectReader = one(subjects);
            ActivationReader activationReader = one(activations);
            var matched = new ArrayList<SubjectSummary>();
            boolean filtered = request.filters().activationStatus() != null;
            int batchSize = filtered ? 101 : request.limit() + 1;
            boolean exhausted = false;
            for (int batch = 0; batch < (filtered ? 100 : 1); batch++) {
                checkBudget(deadline);
                Optional<Anchor> seek = anchor;
                List<Candidate> candidates = owner(deadline, () -> subjectReader.fetchCandidates(request.filters(), seek, batchSize));
                require(candidates != null && candidates.size() <= batchSize);
                var identifiers = new LinkedHashSet<UUID>();
                Anchor previous = seek.orElse(null);
                for (Candidate candidate : candidates) {
                    require(candidate != null);
                    SubjectFacts facts = candidate.subject();
                    validateSubject(facts);
                    require(matches(facts, request.filters()) && identifiers.add(facts.subjectId()));
                    Anchor current = new Anchor(facts.createdAt(), facts.subjectId());
                    if (previous != null) require(before(current, previous));
                    previous = current;
                }
                Map<UUID, ActivationFacts> states = owner(deadline, () -> activationReader.find(Set.copyOf(identifiers)));
                validateActivations(identifiers, states);
                for (Candidate candidate : candidates) {
                    SubjectFacts facts = candidate.subject();
                    ActivationFacts state = states.get(facts.subjectId());
                    String status = state == null ? "INACTIVE" : state.status();
                    if (!filtered || request.filters().activationStatus().equals(status)) {
                        matched.add(summary(facts, status));
                        if (matched.size() == request.limit() + 1) break;
                    }
                }
                exhausted = candidates.size() < batchSize;
                if (matched.size() == request.limit() + 1 || exhausted || !filtered) break;
                require(previous != null);
                anchor = Optional.of(previous);
            }
            if (filtered && matched.size() < request.limit() + 1 && !exhausted) throw unavailable();
            boolean hasMore = matched.size() > request.limit();
            List<SubjectSummary> items = List.copyOf(matched.subList(0, Math.min(matched.size(), request.limit())));
            String next = null;
            if (hasMore) {
                SubjectSummary last = items.getLast();
                next = codec.encode(new Anchor(last.createdAt(), last.subjectId()), digest);
            }
            return new SubjectPage(items, next, hasMore);
        });
    }

    @Override public OperationsSubjectDetail detail(Caller caller, RawDetailRequest raw) {
        return read(caller, (now, deadline) -> {
            UUID subjectId = OperationsSubjectReadRequest.detail(raw);
            SubjectReader subjectReader = one(subjects);
            ActivationReader activationReader = one(activations);
            TrialReader trialReader = one(trials);
            EvidenceCounter evidenceCounter = one(evidence);
            OrderCounter orderCounter = one(orders);
            Optional<DetailFacts> found = owner(deadline, () -> subjectReader.detail(subjectId, 101));
            require(found != null);
            DetailFacts facts = found.orElseThrow(() -> new Failure(Code.NOT_FOUND));
            validateSubject(facts.subject());
            require(subjectId.equals(facts.subject().subjectId()) && facts.bindings().size() <= 100 && facts.claims().size() <= 100);
            List<BindingSummary> bindings = bindings(facts.bindings());
            List<ClaimSummary> claims = claims(facts.claims());
            require(Objects.equals(facts.subject().claimStatus(), claims.isEmpty() ? null : claims.getFirst().status()));
            BindingFacts display = facts.bindings().stream().min((left, right) -> {
                int rank = Integer.compare(bindingRank(left.status()), bindingRank(right.status()));
                return rank != 0 ? rank : left.bindingId().toString().compareTo(right.bindingId().toString());
            }).orElse(null);
            require(Objects.equals(masked(facts.subject().maskedNumber()), display == null ? null : masked(display.numberMasked())));
            Map<UUID, ActivationFacts> activation = owner(deadline, () -> activationReader.find(Set.of(subjectId)));
            validateActivations(Set.of(subjectId), activation);
            ActivationFacts state = activation.get(subjectId);
            Optional<TrialFacts> trial = owner(deadline, () -> trialReader.find(subjectId, now));
            require(trial != null);
            TrialSummary trialSummary = trial.map(value -> trial(value, subjectId, now)).orElse(null);
            long evidenceCount = owner(deadline, () -> evidenceCounter.count(subjectId));
            long orderCount = owner(deadline, () -> orderCounter.count(subjectId));
            require(evidenceCount >= 0 && orderCount >= 0);
            SubjectFacts subject = facts.subject();
            return new OperationsSubjectDetail(subject.subjectId(), subject.source(), masked(subject.maskedNumber()), subject.realNameStatus(),
                    subject.riskOpen() ? "OPEN" : "CLEARED", state == null ? null : new ActivationState(state.status(), state.revision()),
                    trialSummary, bindings, claims, evidenceCount, orderCount, subject.createdAt());
        });
    }

    private <Result> Result read(Caller caller, BiFunction<Instant, Long, Result> operation) {
        long deadline = nanoTime.getAsLong() + REQUEST_BUDGET_NANOS;
        try {
            return transaction.execute(status -> {
                Result result;
                try {
                    checkBudget(deadline);
                    context.enter();
                    checkBudget(deadline);
                    result = operation.apply(authority(caller, deadline), deadline);
                    checkBudget(deadline);
                } finally { context.exit(); }
                checkBudget(deadline);
                return result;
            });
        } catch (Failure known) { throw known; }
        catch (org.jooq.exception.DataAccessException | org.springframework.dao.DataAccessException | TransactionException | BeansException unavailable) {
            throw unavailable();
        }
    }

    private Instant authority(Caller caller, long deadline) {
        if (caller == null || caller.employeeId() == null || caller.audience() == null || caller.audience().isBlank()) throw unavailable();
        if (!AUDIENCE.equals(caller.audience())) throw new Failure(Code.UNAUTHENTICATED);
        var row = owner(deadline, () -> dsl.fetchOne("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)", caller.employeeId()));
        if (row == null) throw new Failure(Code.UNAUTHENTICATED);
        String status = row.get("status", String.class);
        String role = row.get("role", String.class);
        Integer revision = row.get("revision", Integer.class);
        require(caller.employeeId().equals(row.get("id", UUID.class)) && status != null && Set.of("ACTIVE", "DISABLED").contains(status)
                && role != null && Set.of("ADMIN", "OPS").contains(role) && revision != null && revision >= 1);
        if ("DISABLED".equals(status)) throw new Failure(Code.FORBIDDEN);
        var time = owner(deadline, () -> dsl.fetchSingle("SELECT transaction_timestamp() AS now")).get("now", OffsetDateTime.class);
        require(time != null);
        return time.toInstant();
    }

    private static <Port> Port one(ObjectProvider<Port> provider) {
        try {
            var candidates = provider.stream().limit(2).toList();
            if (candidates.size() != 1) throw unavailable();
            return candidates.getFirst();
        } catch (RuntimeException failure) { throw unavailable(); }
    }

    private <Result> Result owner(long deadline, Supplier<Result> operation) {
        checkBudget(deadline);
        try {
            Result result = operation.get();
            checkBudget(deadline);
            return result;
        } catch (Failure known) { throw known; }
        catch (org.jooq.exception.DataAccessException | org.springframework.dao.DataAccessException | TransactionException | BeansException failure) {
            throw unavailable();
        }
    }

    private void checkBudget(long deadline) {
        if (deadline - nanoTime.getAsLong() <= 0) throw unavailable();
    }

    private static void validateSubject(SubjectFacts subject) {
        require(subject != null && subject.subjectId() != null && subject.source() != null
                && Set.of("IVR", "MINIAPP", "WEB", "OPERATIONS").contains(subject.source()) && subject.realNameStatus() != null
                && Set.of("UNVERIFIED", "VERIFIED").contains(subject.realNameStatus())
                && (subject.claimStatus() == null || CLAIM_STATUSES.contains(subject.claimStatus())));
        timestamp(subject.createdAt());
    }

    private static boolean matches(SubjectFacts subject, Filters filters) {
        return (filters.subjectId() == null || filters.subjectId().equals(subject.subjectId()))
                && (filters.source() == null || filters.source().equals(subject.source()))
                && (filters.claimStatus() == null || filters.claimStatus().equals(subject.claimStatus()))
                && (filters.realNameStatus() == null || filters.realNameStatus().equals(subject.realNameStatus()))
                && (filters.riskState() == null || "OPEN".equals(filters.riskState()) == subject.riskOpen());
    }

    private static boolean before(Anchor current, Anchor previous) {
        int time = current.createdAt().compareTo(previous.createdAt());
        return time < 0 || time == 0 && current.subjectId().toString().compareTo(previous.subjectId().toString()) < 0;
    }

    private static SubjectSummary summary(SubjectFacts facts, String activation) {
        return new SubjectSummary(facts.subjectId(), facts.source(), masked(facts.maskedNumber()), facts.claimStatus(),
                activation, facts.realNameStatus(), facts.riskOpen() ? "OPEN" : "CLEARED", facts.createdAt());
    }

    private static void validateActivations(Set<UUID> requested, Map<UUID, ActivationFacts> states) {
        require(states != null && states.size() <= requested.size());
        for (var entry : states.entrySet()) {
            ActivationFacts value = entry.getValue();
            require(entry.getKey() != null && requested.contains(entry.getKey()) && value != null && value.status() != null
                    && ACTIVATION_STATUSES.contains(value.status()) && value.revision() >= 1);
        }
    }

    private static List<BindingSummary> bindings(List<BindingFacts> facts) {
        var result = new ArrayList<BindingSummary>();
        UUID previous = null;
        for (var binding : facts) {
            require(binding != null && binding.bindingId() != null && binding.numberMasked() != null && binding.numberType() != null
                    && Set.of("MOBILE", "LANDLINE").contains(binding.numberType()) && binding.status() != null
                    && Set.of("ACTIVE", "DISABLED", "RETIRED").contains(binding.status()));
            if (previous != null) require(previous.toString().compareTo(binding.bindingId().toString()) < 0);
            if ("ACTIVE".equals(binding.status())) require(binding.verifiedAt() != null);
            if (binding.verifiedAt() != null) timestamp(binding.verifiedAt());
            result.add(new BindingSummary(binding.bindingId(), masked(binding.numberMasked()), binding.numberType(), binding.status(), binding.verifiedAt()));
            previous = binding.bindingId();
        }
        return List.copyOf(result);
    }

    private static List<ClaimSummary> claims(List<ClaimFacts> facts) {
        var result = new ArrayList<ClaimSummary>();
        var seen = new HashSet<UUID>();
        Anchor previous = null;
        for (var claim : facts) {
            require(claim != null && claim.claimId() != null && seen.add(claim.claimId()) && claim.status() != null
                    && CLAIM_STATUSES.contains(claim.status()) && claim.verificationMethod() != null
                    && Set.of("SMS", "WECHAT_PHONE", "CALLER_CHALLENGE", "MANUAL_REVIEW").contains(claim.verificationMethod())
                    && "ACCEPTED".equals(claim.status()) == (claim.verifiedAt() != null));
            timestamp(claim.riskCheckedAt());
            if (claim.verifiedAt() != null) timestamp(claim.verifiedAt());
            Anchor current = new Anchor(claim.riskCheckedAt(), claim.claimId());
            if (previous != null) require(before(current, previous));
            previous = current;
            result.add(new ClaimSummary(claim.claimId(), claim.status(), claim.verificationMethod(), claim.riskCheckedAt(), claim.verifiedAt()));
        }
        return List.copyOf(result);
    }

    private static TrialSummary trial(TrialFacts facts, UUID subjectId, Instant now) {
        require(facts.trialId() != null && subjectId.equals(facts.subjectId()) && "FIRST-TRIAL".equals(facts.campaignKey())
                && facts.entitlementGrantId() != null && facts.entitlementGrantId().equals(facts.grantId())
                && subjectId.equals(facts.grantSubjectId()) && "TRIAL".equals(facts.sourceType()) && "RECORDING_SECONDS".equals(facts.entitlementType())
                && facts.balanceId() != null && facts.grantId().equals(facts.balanceGrantId())
                && facts.grantedUnits() != null && facts.grantedUnits() > 0 && facts.grantedUnits().equals(facts.balanceGrantedUnits())
                && facts.reservedUnits() != null && facts.consumedUnits() != null && facts.reservedUnits() >= 0 && facts.consumedUnits() >= 0
                && facts.consumedUnits() <= facts.grantedUnits() && facts.reservedUnits() <= facts.grantedUnits() - facts.consumedUnits()
                && facts.balanceStatus() != null && Set.of("AVAILABLE", "EXHAUSTED", "EXPIRED").contains(facts.balanceStatus())
                && "EXHAUSTED".equals(facts.balanceStatus()) == facts.consumedUnits().equals(facts.grantedUnits())
                && facts.trialExpiresAt() != null && facts.trialExpiresAt().equals(facts.grantExpiresAt()));
        timestamp(facts.trialExpiresAt());
        String status = !now.isBefore(facts.trialExpiresAt()) || "EXPIRED".equals(facts.balanceStatus()) ? "EXPIRED"
                : facts.consumedUnits().equals(facts.grantedUnits()) ? "EXHAUSTED" : "GRANTED";
        return new TrialSummary(status, facts.grantedUnits(), facts.grantedUnits() - facts.reservedUnits() - facts.consumedUnits(), facts.trialExpiresAt());
    }

    private static int bindingRank(String status) { return switch (status) { case "ACTIVE" -> 0; case "DISABLED" -> 1; default -> 2; }; }
    private static String masked(String text) {
        if (text == null) return null;
        return text.length() >= 1 && text.length() <= 64 && text.indexOf('*') >= 0 && text.matches("[0-9+ ()*\\-]+")
                ? text : "[REDACTED:PHONE]";
    }
    private static void timestamp(Instant value) { require(value != null && value.getNano() % 1000 == 0); }
    private static void require(boolean valid) { if (!valid) throw unavailable(); }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
