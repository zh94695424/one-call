package com.onecall.core.sharing.internal.provider;

import com.onecall.core.sharing.internal.application.ShareSummary;
import com.onecall.core.sharing.internal.command.ShareCommandCaller;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.JSONB;

public interface ShareCreationPolicyProvider {
    ProviderResult<CreationPolicyDecision> evaluate(CreationPolicyRequest request);

    record CreationPolicyRequest(ShareCommandCaller caller, UUID evidenceId,
            ShareSummary.VerificationMode verificationMode, Optional<UUID> grantOwnerOrganizationId,
            JSONB content, JSONB verification) {
        public CreationPolicyRequest {
            Objects.requireNonNull(caller);
            Objects.requireNonNull(evidenceId);
            Objects.requireNonNull(verificationMode);
            Objects.requireNonNull(grantOwnerOrganizationId);
            Objects.requireNonNull(content);
            Objects.requireNonNull(verification);
        }
    }

    enum CreationPolicyDecision { ALLOW, DENY }
}
