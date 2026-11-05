package com.onecall.core.sharing.internal.provider;

import java.util.Objects;
import java.util.UUID;

public interface CapabilityTokenProvider {
    ProviderResult<CapabilityTokenMaterial> issue(UUID grantId);

    record CapabilityTokenMaterial(String plaintext, String sha256) {
        public CapabilityTokenMaterial {
            Objects.requireNonNull(plaintext);
            Objects.requireNonNull(sha256);
        }
    }
}
