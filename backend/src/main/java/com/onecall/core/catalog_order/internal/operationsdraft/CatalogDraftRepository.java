package com.onecall.core.catalog_order.internal.operationsdraft;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;

interface CatalogDraftRepository {
    TransactionWitness begin();
    void catalogRole(TransactionWitness witness);
    void restore(TransactionWitness witness);
    CreateResult create(CreateCommand command);
    CatalogVersionDraft replace(ReplaceCommand command);
    CreateResult recoverCreate(CreateCommand command);
    CatalogVersionDraft recoverReplace(ReplaceCommand command);
}
