package com.onecall.core.sharing.internal.provider;

import java.util.Objects;

public interface DesignatedMobileBindingProvider {
    ProviderResult<MobileHmacMaterial> bind(String clientDesignatedMobile);

    record MobileHmacMaterial(String mobileHmac, String keyVersion) {
        public MobileHmacMaterial {
            Objects.requireNonNull(mobileHmac);
            Objects.requireNonNull(keyVersion);
        }
    }
}
