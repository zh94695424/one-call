package com.onecall.core.catalog_order.customerorder;

public interface CustomerOrderSnapshotReader {
    CustomerOrderCreationService.Snapshot read(String storedSnapshot, String snapshotSha256);

    final class InvalidSnapshotException extends RuntimeException {
        public InvalidSnapshotException() { super("Invalid stored order snapshot"); }
        public InvalidSnapshotException(Throwable cause) { super("Invalid stored order snapshot", cause); }
    }
}
