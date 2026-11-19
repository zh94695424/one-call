package com.onecall.core.operations_iam.operationscommand;

import java.time.Instant;
import java.util.UUID;

public interface OperationsCatalogSaleCommand {
    CatalogVersionState publish(PublishCommand command);
    CatalogVersionState offsale(OffsaleCommand command);

    record Caller(UUID employeeId, UUID traceId) {
        public Caller { if (employeeId == null || traceId == null) throw new Failure(Code.UNAUTHENTICATED); }
    }
    record PublishCommand(UUID versionId, UUID expectedActiveVersionId, int expectedRevision, Caller caller) {
        public PublishCommand { validate(versionId, expectedRevision, caller); }
    }
    record OffsaleCommand(UUID versionId, int expectedRevision, Caller caller) {
        public OffsaleCommand { validate(versionId, expectedRevision, caller); }
    }
    private static void validate(UUID versionId, int revision, Caller caller) {
        if (versionId == null || revision < 1) throw new Failure(Code.VALIDATION_FAILED);
        if (caller == null) throw new Failure(Code.UNAUTHENTICATED);
    }
    record CatalogVersionState(UUID catalogVersionId, UUID productId, int versionNo, String saleStatus,
            int revision, Instant publishedAt, UUID currentActiveVersionId) { }
    enum Code { UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, PUBLICATION_STALE_VIEW,
        CATALOG_PUBLICATION_CONFLICT, VALIDATION_FAILED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL }
    final class Failure extends RuntimeException {
        private final Code code;
        public Failure(Code code) { super("Catalog sale command failed", null, false, false); this.code = code; }
        public Code code() { return code; }
    }
}
