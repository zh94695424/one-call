package com.onecall.core.sharing.internal.provider;

import com.onecall.core.sharing.internal.application.ShareSummary;
import com.onecall.core.sharing.internal.command.ShareCommandCaller;
import java.util.Objects;
import java.util.UUID;

public interface ShareRotationPolicyProvider {
    ProviderResult<RotationPolicyDecision> evaluate(RotationPolicyRequest request);

    record RotationPolicyRequest(ShareCommandCaller caller, UUID grantId,
            ShareSummary.VerificationMode verificationMode, ShareSummary.ShareStatus status,
            int codeGeneration, int revocationGeneration) {
        public RotationPolicyRequest {
            Objects.requireNonNull(caller);
            Objects.requireNonNull(grantId);
            Objects.requireNonNull(verificationMode);
            Objects.requireNonNull(status);
        }
    }

    enum RotationPolicyDecision { ALLOW, DENY }
}
