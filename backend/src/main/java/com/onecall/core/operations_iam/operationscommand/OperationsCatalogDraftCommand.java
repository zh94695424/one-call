package com.onecall.core.operations_iam.operationscommand;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface OperationsCatalogDraftCommand {
    CreateResult create(CreateCommand command);
    CatalogVersionDraft replace(ReplaceCommand command);

    record Caller(UUID employeeId, UUID traceId) {
        public Caller { Objects.requireNonNull(employeeId); Objects.requireNonNull(traceId); }
    }
    record Commercial(String offerType, String audience, Long priceCents, String currency,
            String billingCycle, int installments, int grantIntervalMonths) { }
    record BenefitLine(String entitlementType, long quantity, int validityDays, int grantIntervalMonths) { }
    record CreateCommand(UUID productId, int versionNo, Commercial commercial, List<BenefitLine> benefitLines,
            List<String> channels, Caller caller) {
        public CreateCommand {
            Objects.requireNonNull(productId); Objects.requireNonNull(commercial); Objects.requireNonNull(caller);
            benefitLines = List.copyOf(benefitLines); channels = List.copyOf(channels);
        }
    }
    record ReplaceCommand(UUID versionId, int expectedRevision, Commercial commercial, Caller caller) {
        public ReplaceCommand { Objects.requireNonNull(versionId); Objects.requireNonNull(commercial); Objects.requireNonNull(caller); }
    }
    record CatalogVersionDraft(UUID catalogVersionId, UUID productId, int versionNo, String saleStatus,
            String offerType, String audience, Long priceCents, String currency, String billingCycle,
            int installments, int grantIntervalMonths, List<BenefitLine> benefitLines, List<String> channels,
            int revision, Instant createdAt, Instant publishedAt, String snapshotSha256) {
        public CatalogVersionDraft { benefitLines = List.copyOf(benefitLines); channels = List.copyOf(channels); }
    }
    record CreateResult(CatalogVersionDraft draft, boolean created) {
        public CreateResult { Objects.requireNonNull(draft); }
    }
    enum Code { UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CATALOG_DRAFT_CONFLICT, CATALOG_DRAFT_STALE_VIEW,
        VALIDATION_FAILED, SERVICE_UNAVAILABLE, INTERNAL }
    final class Failure extends RuntimeException {
        private final Code code;
        public Failure(Code code) { super("Catalog draft command failed", null, false, false); this.code = Objects.requireNonNull(code); }
        public Code code() { return code; }
    }
}
