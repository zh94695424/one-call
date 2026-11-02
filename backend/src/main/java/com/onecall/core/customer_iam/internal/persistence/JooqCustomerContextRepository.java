package com.onecall.core.customer_iam.internal.persistence;

import static com.onecall.core.customer_iam.internal.application.CustomerContext.*;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

public final class JooqCustomerContextRepository implements CustomerContextRepository {
    private final DSLContext context;

    public JooqCustomerContextRepository(DSLContext context) { this.context = context; }

    @Override
    public Optional<CustomerContext> findActiveContext(TrustedCustomerCaller caller) {
        var rows = context.fetch("""
                SELECT a.id, a.display_name, a.status,
                       w.organization_id, w.organization_display_name, w.role
                FROM customer_iam.customer_account a
                LEFT JOIN (
                    SELECT m.account_id, o.id AS organization_id,
                           o.display_name AS organization_display_name, m.role
                    FROM organization_access.organization_membership m
                    JOIN organization_access.organization o ON o.id = m.organization_id
                    WHERE m.status = ? AND o.status = ?
                ) w ON w.account_id = a.id
                WHERE a.id = ? AND a.status = ?
                ORDER BY w.organization_id ASC
                """, "ACTIVE", "ACTIVE", caller.accountId(), "ACTIVE");
        if (rows.isEmpty()) { return Optional.empty(); }
        var first = rows.getFirst();
        var account = new AccountSummary(first.get("id", UUID.class), first.get("display_name", String.class),
                AccountStatus.valueOf(first.get("status", String.class)));
        var workspaces = new ArrayList<WorkspaceSummary>();
        workspaces.add(new WorkspaceSummary(WorkspaceType.PERSONAL, null, null, null, WorkspaceStatus.ACTIVE));
        for (var row : rows) {
            UUID organization = row.get("organization_id", UUID.class);
            if (organization != null) {
                workspaces.add(new WorkspaceSummary(WorkspaceType.ORGANIZATION, organization,
                        row.get("organization_display_name", String.class),
                        OrganizationRole.valueOf(row.get("role", String.class)), WorkspaceStatus.ACTIVE));
            }
        }
        return Optional.of(new CustomerContext(account, workspaces, caller.channel()));
    }
}
