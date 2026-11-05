package com.onecall.core.sharing.internal.command;

import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public sealed interface CustomerShareCommand permits CustomerShareCommand.Create {
    Optional<UUID> traceId();

    record Create(String creationRequestId, UUID evidenceId, VerificationMode verificationMode,
            Instant expiresAt, boolean downloadAllowed, Optional<UUID> traceId,
            Integer maxAdmissions, String designatedMobile) implements CustomerShareCommand {
        public Create {
            Objects.requireNonNull(creationRequestId);
            Objects.requireNonNull(evidenceId);
            Objects.requireNonNull(verificationMode);
            Objects.requireNonNull(expiresAt);
            Objects.requireNonNull(traceId);
        }
    }
}
