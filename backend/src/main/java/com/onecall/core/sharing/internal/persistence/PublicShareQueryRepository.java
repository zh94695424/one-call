package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.publicquery.PublicShareView.Status;
import com.onecall.core.sharing.internal.publicquery.PublicShareView.VerificationMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PublicShareQueryRepository {
    Optional<PublicGrant> findByTokenDigest(String digest);
    Instant sampleDatabaseTime();

    record PublicGrant(UUID evidenceId, Optional<UUID> ownerOrganizationId, VerificationMode verificationMode,
            Status status, Instant validFrom, Instant expiresAt, boolean downloadAllowed, String contentSnapshot) {
        @Override
        public String toString() { return "[REDACTED]"; }
    }
}
