package com.onecall.core.customer_iam.internal.application;

import static com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.*;

import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Anchor;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Artifact;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Header;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionTemplate;

public final class CustomerEvidenceQueryService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final JooqCustomerBusinessNumberAccessRepository lifecycle;
    private final CustomerSubjectAccessReader subjects;
    private final CustomerEvidenceReader evidence;

    public CustomerEvidenceQueryService(TransactionTemplate transaction, DSLContext context,
            JooqCustomerBusinessNumberAccessRepository lifecycle, CustomerSubjectAccessReader subjects,
            CustomerEvidenceReader evidence) {
        this.transaction = transaction;
        this.context = context;
        this.lifecycle = lifecycle;
        this.subjects = subjects;
        this.evidence = evidence;
    }

    public record Request(Optional<Anchor> anchor, Optional<Integer> limit) {
        public Request {
            if (anchor == null || limit == null || limit.filter(value -> value < 1 || value > 100).isPresent())
                throw new CustomerContextException(VALIDATION_FAILED);
            if (anchor.isPresent() && (anchor.get().evidenceId() == null || !validTime(anchor.get().createdAt())))
                throw new CustomerContextException(CURSOR_INVALID);
        }
        public int effectiveLimit() { return limit.orElse(20); }
    }

    public record Summary(UUID evidenceId, UUID subjectId, String status, String sourceType,
                          Instant capturedAt, String consentVersion, Instant createdAt, List<String> artifactRoles) {
        public Summary { artifactRoles = List.copyOf(artifactRoles); }
    }
    public record Page(List<Summary> items, Optional<Anchor> nextAnchor, boolean hasMore) {
        public Page { items = List.copyOf(items); }
    }
    public record Detail(UUID evidenceId, UUID subjectId, String status, String sourceType,
                         Instant capturedAt, String consentVersion, Instant solidifiedAt, List<Artifact> artifacts) {
        public Detail { artifacts = List.copyOf(artifacts); }
    }

    public Page listCustomerEvidence(TrustedCustomerCaller caller, Request request) {
        var scope = CustomerBusinessNumberQueryService.requireScope(caller);
        if (request == null) throw new CustomerContextException(VALIDATION_FAILED);
        return compose(() -> {
            lifecycle.requireActiveContext(scope);
            var owned = subjects.findOwnedSubjectIds(scope.accountId(), scope.organizationId());
            requireIds(owned);
            if (owned.isEmpty()) return new Page(List.of(), Optional.empty(), false);
            var present = evidence.findSubjectIdsWithEvidence(owned);
            requireIds(present);
            if (!owned.containsAll(present)) throw new CustomerContextException(INTERNAL);
            if (present.isEmpty()) return new Page(List.of(), Optional.empty(), false);
            if (subjects.hasOpenRisk(present)) throw new CustomerContextException(FORBIDDEN);
            var headers = evidence.fetchPage(present, request.anchor(), request.effectiveLimit() + 1);
            if (headers == null || headers.size() > request.effectiveLimit() + 1)
                throw new CustomerContextException(INTERNAL);
            var identifiers = new HashSet<UUID>();
            Anchor previous = request.anchor().orElse(null);
            for (var header : headers) {
                requireHeader(header);
                if (!present.contains(header.subjectId()) || !identifiers.add(header.evidenceId())
                        || !validHeader(header)) throw new CustomerContextException(INTERNAL);
                var current = new Anchor(header.createdAt(), header.evidenceId());
                if (previous != null && compare(current, previous) >= 0) throw new CustomerContextException(INTERNAL);
                previous = current;
            }
            boolean more = headers.size() > request.effectiveLimit();
            var returned = headers.subList(0, Math.min(headers.size(), request.effectiveLimit()));
            var returnedIds = returned.stream().map(Header::evidenceId).collect(java.util.stream.Collectors.toSet());
            var roles = returned.isEmpty() ? java.util.Map.<UUID, List<String>>of() : evidence.findArtifactRoles(returnedIds);
            if (roles == null || !returnedIds.containsAll(roles.keySet())) throw new CustomerContextException(INTERNAL);
            roles.values().forEach(values -> {
                if (values == null || values.stream().anyMatch(value -> value == null || !Set.of("ORIGINAL", "DERIVED").contains(value)))
                    throw new CustomerContextException(INTERNAL);
            });
            var summaries = returned.stream().map(header -> new Summary(header.evidenceId(), header.subjectId(),
                    header.status(), header.sourceType(), capture(header), header.consentVersion(), header.createdAt(),
                    roles.getOrDefault(header.evidenceId(), List.of()).stream().distinct()
                            .sorted(Comparator.comparingInt(CustomerEvidenceQueryService::rank)).toList())).toList();
            Optional<Anchor> next = more ? Optional.of(new Anchor(returned.getLast().createdAt(), returned.getLast().evidenceId()))
                    : Optional.empty();
            return new Page(summaries, next, more);
        });
    }

    public Optional<Detail> getCustomerEvidence(TrustedCustomerCaller caller, UUID evidenceId) {
        var scope = CustomerBusinessNumberQueryService.requireScope(caller);
        if (evidenceId == null) throw new CustomerContextException(VALIDATION_FAILED);
        return compose(() -> {
            lifecycle.requireActiveContext(scope);
            var found = evidence.findHeader(evidenceId);
            if (found == null) throw new CustomerContextException(INTERNAL);
            if (found.isEmpty()) return Optional.empty();
            var header = found.get();
            requireHeader(header);
            if (!evidenceId.equals(header.evidenceId())) throw new CustomerContextException(INTERNAL);
            var owned = subjects.findOwnedSubject(header.subjectId(), scope.accountId(), scope.organizationId());
            if (owned == null) throw new CustomerContextException(INTERNAL);
            if (owned.isEmpty()) return Optional.empty();
            if (!header.subjectId().equals(owned.get())) throw new CustomerContextException(INTERNAL);
            if (subjects.hasOpenRisk(header.subjectId())) throw new CustomerContextException(FORBIDDEN);
            if (!validHeader(header)) return Optional.empty();
            var artifacts = evidence.findArtifacts(evidenceId);
            if (artifacts == null || artifacts.stream().anyMatch(java.util.Objects::isNull))
                throw new CustomerContextException(INTERNAL);
            if (artifacts.stream().anyMatch(value -> !validArtifact(value))) return Optional.empty();
            var ordered = artifacts.stream().sorted(Comparator.comparingInt((Artifact value) -> rank(value.artifactRole()))
                    .thenComparing(Artifact::sha256).thenComparing(Artifact::sizeBytes)
                    .thenComparing(Artifact::mimeType)).toList();
            return Optional.of(new Detail(header.evidenceId(), header.subjectId(), header.status(), header.sourceType(),
                    capture(header), header.consentVersion(), header.solidifiedAt(), ordered));
        });
    }

    private <Result> Result compose(Supplier<Result> action) {
        try {
            Result result = transaction.execute(status -> {
                try {
                    context.execute("SET LOCAL ROLE customer_iam_role");
                    var role = context.fetchOne("select current_user");
                    if (role == null || !"customer_iam_role".equals(role.get(0, String.class)))
                        throw new CustomerContextException(SERVICE_UNAVAILABLE);
                } catch (RuntimeException failure) {
                    throw new CustomerContextException(SERVICE_UNAVAILABLE);
                }
                return action.get();
            });
            if (result == null) throw new CustomerContextException(INTERNAL);
            return result;
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

    private static void requireIds(Set<UUID> identifiers) {
        if (identifiers == null || identifiers.stream().anyMatch(java.util.Objects::isNull))
            throw new CustomerContextException(INTERNAL);
    }

    private static void requireHeader(Header header) {
        if (header == null || header.evidenceId() == null || header.subjectId() == null)
            throw new CustomerContextException(INTERNAL);
    }

    private static int compare(Anchor left, Anchor right) {
        int time = left.createdAt().compareTo(right.createdAt());
        return time == 0 ? left.evidenceId().toString().compareTo(right.evidenceId().toString()) : time;
    }

    private static boolean validTime(Instant value) {
        if (value == null || value.getNano() % 1000 != 0) return false;
        try {
            int year = value.atOffset(ZoneOffset.UTC).getYear();
            return year >= -4712 && year <= 294276;
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean validText(String value, int maximum) {
        return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= maximum
                && value.indexOf('\0') < 0 && StandardCharsets.UTF_8.newEncoder().canEncode(value);
    }

    private static Instant capture(Header header) {
        try { return OffsetDateTime.parse(header.capturedAtText()).toInstant(); }
        catch (RuntimeException invalid) { return null; }
    }

    private static boolean validHeader(Header header) {
        return header.status() != null && Set.of("PENDING", "SOLIDIFIED", "QUARANTINED", "FAILED").contains(header.status())
                && "CALL_RECORDING".equals(header.sourceType()) && validText(header.consentVersion(), 64)
                && validTime(header.createdAt()) && capture(header) != null
                && ("SOLIDIFIED".equals(header.status()) == (header.solidifiedAt() != null))
                && (header.solidifiedAt() == null || validTime(header.solidifiedAt()));
    }

    private static int rank(String role) { return "ORIGINAL".equals(role) ? 0 : 1; }

    private static boolean validArtifact(Artifact value) {
        return value.artifactRole() != null && Set.of("ORIGINAL", "DERIVED").contains(value.artifactRole())
                && validText(value.mimeType(), 128) && value.sizeBytes() != null && value.sizeBytes() >= 0
                && value.sha256() != null && value.sha256().matches("[0-9a-f]{64}")
                && (value.validatedDurationSeconds() == null || ("ORIGINAL".equals(value.artifactRole())
                    && value.validatedDurationSeconds() > 0));
    }
}
