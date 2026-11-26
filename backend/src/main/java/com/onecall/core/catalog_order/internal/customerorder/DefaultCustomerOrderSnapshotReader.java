package com.onecall.core.catalog_order.internal.customerorder;

import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.Snapshot;
import com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class DefaultCustomerOrderSnapshotReader implements CustomerOrderSnapshotReader {
    @Override
    public Snapshot read(String storedSnapshot, String snapshotSha256) {
        try {
            CustomerOrderSemantics.storedSnapshot(storedSnapshot);
            if (snapshotSha256 == null || !snapshotSha256.matches("[0-9a-f]{64}")
                    || !MessageDigest.isEqual(snapshotSha256.getBytes(StandardCharsets.US_ASCII),
                            CustomerOrderSemantics.sha(CustomerOrderSemantics.canonical(CustomerOrderSemantics.parse(storedSnapshot)))
                                    .getBytes(StandardCharsets.US_ASCII)))
                throw new InvalidSnapshotException();
            return CustomerOrderSemantics.projection(storedSnapshot, snapshotSha256);
        } catch (OrderCreationException failure) {
            throw new InvalidSnapshotException(failure);
        }
    }
}
