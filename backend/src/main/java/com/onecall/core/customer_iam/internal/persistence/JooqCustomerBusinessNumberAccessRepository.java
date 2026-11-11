package com.onecall.core.customer_iam.internal.persistence;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import org.jooq.DSLContext;

public class JooqCustomerBusinessNumberAccessRepository {
    private final DSLContext context;

    public JooqCustomerBusinessNumberAccessRepository(DSLContext context) { this.context = context; }

    public void requireActiveContext(Scope scope) {
        var account = context.fetchOne("SELECT id, status FROM customer_iam.customer_account WHERE id = ? AND status = ?",
                scope.accountId(), "ACTIVE");
        if (account == null) throw new CustomerContextException(CustomerContextException.Code.FORBIDDEN);
        if (scope.organizationId() == null) return;
        var membership = context.fetchOne("""
                SELECT o.id, o.status, m.account_id, m.organization_id, m.status, m.role
                FROM organization_access.organization o
                JOIN organization_access.organization_membership m ON m.organization_id = o.id
                WHERE o.id = ? AND o.status = ? AND m.account_id = ? AND m.status = ?
                  AND m.role IN (?, ?, ?, ?)
                """, scope.organizationId(), "ACTIVE", scope.accountId(), "ACTIVE",
                "ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR");
        if (membership == null) throw new CustomerContextException(CustomerContextException.Code.FORBIDDEN);
    }
}
