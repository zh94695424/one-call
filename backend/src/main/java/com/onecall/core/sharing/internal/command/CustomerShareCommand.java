package com.onecall.core.sharing.internal.command;

import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public sealed interface CustomerShareCommand permits CustomerShareCommand.Create, CustomerShareCommand.Revoke, CustomerShareCommand.Rotate {
    Optional<UUID> traceId();

    record Rotate(String shareId, String actionRequestId, Optional<UUID> traceId,
            Integer expectedCodeGeneration) implements CustomerShareCommand {
        public Rotate {
            Objects.requireNonNull(shareId);
            Objects.requireNonNull(actionRequestId);
            Objects.requireNonNull(traceId);
        }
    }

    record Revoke(String shareId, String actionRequestId, Optional<UUID> traceId,
            Integer expectedRevocationGeneration, String reason) implements CustomerShareCommand {
        public Revoke {
            Objects.requireNonNull(shareId);
            Objects.requireNonNull(actionRequestId);
            Objects.requireNonNull(traceId);
        }
    }

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
