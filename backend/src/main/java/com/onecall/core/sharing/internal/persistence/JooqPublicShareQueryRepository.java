package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.publicquery.PublicShareView.Status;
import com.onecall.core.sharing.internal.publicquery.PublicShareView.VerificationMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.beans.factory.annotation.Qualifier;

public final class JooqPublicShareQueryRepository implements PublicShareQueryRepository {
    private final DSLContext context;

    public JooqPublicShareQueryRepository(@Qualifier("publicShareQueryDsl") DSLContext context) {
        this.context = context;
    }

    @Override
    public Optional<PublicGrant> findByTokenDigest(String digest) {
        var row = context.fetchOne("SELECT evidence_id, owner_organization_id, verification_mode, status, "
                + "valid_from, expires_at, download_allowed, content_snapshot "
                + "FROM sharing.evidence_share_grant WHERE capability_token_hash = ?", digest);
        if (row == null) return Optional.empty();
        return Optional.of(new PublicGrant(java.util.Objects.requireNonNull(row.get("evidence_id", UUID.class)),
                Optional.ofNullable(row.get("owner_organization_id", UUID.class)),
                VerificationMode.valueOf(row.get("verification_mode", String.class)), Status.valueOf(row.get("status", String.class)),
                row.get("valid_from", OffsetDateTime.class).toInstant(), row.get("expires_at", OffsetDateTime.class).toInstant(),
                java.util.Objects.requireNonNull(row.get("download_allowed", Boolean.class)),
                row.get("content_snapshot", JSONB.class).data()));
    }

    @Override
    public Instant sampleDatabaseTime() {
        return context.fetchOne("SELECT clock_timestamp()").get(0, OffsetDateTime.class).toInstant();
    }
}
