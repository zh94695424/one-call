package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.application.ShareCursor;
import com.onecall.core.sharing.internal.application.SharePageRequest;
import com.onecall.core.sharing.internal.application.ShareQueryException;
import com.onecall.core.sharing.internal.application.ShareSummary;
import com.onecall.core.sharing.internal.application.TrustedShareCaller;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public final class JooqShareQueryRepository implements ShareQueryRepository {
    private static final Field<String> SHARE_ID = DSL.field(DSL.name("share_id"), SQLDataType.VARCHAR);
    private static final Field<UUID> EVIDENCE_ID = DSL.field(DSL.name("evidence_id"), SQLDataType.UUID);
    private static final Field<String> MODE = DSL.field(DSL.name("verification_mode"), SQLDataType.VARCHAR);
    private static final Field<String> STATUS = DSL.field(DSL.name("status"), SQLDataType.VARCHAR);
    private static final Field<OffsetDateTime> VALID_FROM = DSL.field(DSL.name("valid_from"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<OffsetDateTime> EXPIRES_AT = DSL.field(DSL.name("expires_at"), SQLDataType.TIMESTAMPWITHTIMEZONE);
    private static final Field<Integer> MAX = DSL.field(DSL.name("max_admissions"), SQLDataType.INTEGER);
    private static final Field<Integer> USED = DSL.field(DSL.name("admissions_used"), SQLDataType.INTEGER);
    private static final Field<Boolean> DOWNLOAD = DSL.field(DSL.name("download_allowed"), SQLDataType.BOOLEAN);
    private static final Field<Integer> CODE = DSL.field(DSL.name("code_generation"), SQLDataType.INTEGER);
    private static final Field<Integer> REVOCATION = DSL.field(DSL.name("revocation_generation"), SQLDataType.INTEGER);
    private final DSLContext context;

    public JooqShareQueryRepository(DSLContext context) { this.context = context; }

    @Override
    public void requireActiveCreatorContext(TrustedShareCaller caller) {
        Field<UUID> id = DSL.field(DSL.name("id"), SQLDataType.UUID);
        boolean account = context.select(id, STATUS).from(DSL.table(DSL.name("customer_iam", "customer_account")))
                .where(id.eq(caller.accountId()).and(STATUS.eq("ACTIVE"))).fetch().isNotEmpty();
        if (!account) { throw new ShareQueryException(ShareQueryException.Code.FORBIDDEN); }
        caller.organizationId().ifPresent(organization -> {
            boolean active = context.select(id, STATUS).from(DSL.table(DSL.name("organization_access", "organization")))
                    .where(id.eq(organization).and(STATUS.eq("ACTIVE"))).fetch().isNotEmpty();
            Field<UUID> organizationId = DSL.field(DSL.name("organization_id"), SQLDataType.UUID);
            Field<UUID> accountId = DSL.field(DSL.name("account_id"), SQLDataType.UUID);
            boolean member = active && context.select(organizationId, accountId, STATUS)
                    .from(DSL.table(DSL.name("organization_access", "organization_membership")))
                    .where(organizationId.eq(organization).and(accountId.eq(caller.accountId())).and(STATUS.eq("ACTIVE")))
                    .fetch().isNotEmpty();
            if (!member) { throw new ShareQueryException(ShareQueryException.Code.FORBIDDEN); }
        });
    }

    @Override
    public List<ShareSummary> fetch(TrustedShareCaller caller, SharePageRequest request, int fetchSize) {
        Field<UUID> creator = DSL.field(DSL.name("created_by_account_id"), SQLDataType.UUID);
        Field<UUID> organization = DSL.field(DSL.name("owner_organization_id"), SQLDataType.UUID);
        Condition scope = creator.eq(caller.accountId()).and(caller.organizationId()
                .map(organization::eq).orElseGet(organization::isNull));
        if (request.cursor().isPresent()) {
            ShareCursor cursor = request.cursor().get();
            OffsetDateTime anchor = cursor.validFrom().atOffset(ZoneOffset.UTC);
            scope = scope.and(VALID_FROM.lt(anchor).or(VALID_FROM.eq(anchor).and(SHARE_ID.lt(cursor.shareId()))));
        }
        return context.select(SHARE_ID, EVIDENCE_ID, MODE, STATUS, VALID_FROM, EXPIRES_AT, MAX, USED, DOWNLOAD, CODE, REVOCATION)
                .from(DSL.table(DSL.name("sharing", "evidence_share_grant"))).where(scope)
                .orderBy(VALID_FROM.desc(), SHARE_ID.desc()).limit(fetchSize).fetch(row -> new ShareSummary(
                        row.get(SHARE_ID), row.get(EVIDENCE_ID), ShareSummary.VerificationMode.valueOf(row.get(MODE)),
                        ShareSummary.ShareStatus.valueOf(row.get(STATUS)), Objects.requireNonNull(row.get(VALID_FROM)).toInstant(),
                        Objects.requireNonNull(row.get(EXPIRES_AT)).toInstant(), row.get(MAX),
                        Objects.requireNonNull(row.get(USED)), Objects.requireNonNull(row.get(DOWNLOAD)),
                        Objects.requireNonNull(row.get(CODE)), Objects.requireNonNull(row.get(REVOCATION))));
    }
}
