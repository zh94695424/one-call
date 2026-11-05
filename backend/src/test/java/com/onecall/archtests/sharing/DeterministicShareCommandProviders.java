package com.onecall.archtests.sharing;

import com.onecall.core.sharing.internal.provider.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class DeterministicShareCommandProviders {
    private DeterministicShareCommandProviders() { }

    public static String digest(String value) { return sha256().digest(value.getBytes(StandardCharsets.UTF_8)); }

    public static Sha256Provider sha256() {
        return bytes -> {
            try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (java.security.NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
        };
    }

    public static CapabilityTokenProvider tokens() {
        return grantId -> {
            String token = "test-token-" + grantId;
            return new ProviderResult.Allowed<>(new CapabilityTokenProvider.CapabilityTokenMaterial(token, digest(token)));
        };
    }

    public static ShareExtractionCodeProvider codes() {
        return (grantId, generation) -> new ProviderResult.Allowed<>(new ExtractionCodeMaterial(
                "test-code", "test_salt_abcdefghijklmnop", digest(grantId + ":" + generation)));
    }

    public static DesignatedMobileBindingProvider mobiles() {
        return mobile -> new ProviderResult.Allowed<>(new DesignatedMobileBindingProvider.MobileHmacMaterial(digest(mobile.strip()), "test-key-v1"));
    }

    public static ShareCreationPolicyProvider policy() {
        return request -> new ProviderResult.Allowed<>(ShareCreationPolicyProvider.CreationPolicyDecision.ALLOW);
    }
}
