package com.onecall.core.sharing.internal.command;

import com.onecall.core.sharing.internal.application.ShareSummary;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public sealed interface CustomerShareCommandResult {
    record Revoked(ShareState state) implements CustomerShareCommandResult {
        public Revoked { Objects.requireNonNull(state); }
    }

    record ShareState(String shareId, UUID evidenceId, ShareSummary.VerificationMode verificationMode,
            ShareSummary.ShareStatus status, Instant validFrom, Instant expiresAt, Integer maxAdmissions,
            int admissionsUsed, boolean downloadAllowed, int codeGeneration, int revocationGeneration) { }

    record Created(ShareState state, String capabilityToken, Optional<String> extractionCode)
            implements CustomerShareCommandResult {
        public Created {
            Objects.requireNonNull(state);
            Objects.requireNonNull(capabilityToken);
            Objects.requireNonNull(extractionCode);
        }
    }

    record CreationReplayed(ShareState state) implements CustomerShareCommandResult {
        public CreationReplayed { Objects.requireNonNull(state); }
    }
}
