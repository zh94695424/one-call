package com.onecall.core.catalog_order.internal.operationscommand;

import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.CatalogVersionState;
import java.time.Instant;
import java.util.UUID;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;

interface CatalogSaleLifecycleRepository {
    TransactionWitness begin();
    void catalogRole(TransactionWitness witness);
    void recoveryRole(TransactionWitness witness);
    void restore(TransactionWitness witness);
    CatalogVersionState publish(UUID versionId, UUID expectedActiveVersionId, int expectedRevision, UUID employeeId, UUID traceId);
    CatalogVersionState offsale(UUID versionId, int expectedRevision, UUID employeeId, UUID traceId);
    CatalogVersionState recover(UUID versionId, UUID expectedActiveVersionId, int expectedRevision, boolean publication);
}
