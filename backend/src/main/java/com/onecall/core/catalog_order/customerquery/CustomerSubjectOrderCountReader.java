package com.onecall.core.catalog_order.customerquery;

import java.util.UUID;

public interface CustomerSubjectOrderCountReader {
    long countForWorkspace(UUID subjectId, UUID accountId, UUID organizationId);
}
