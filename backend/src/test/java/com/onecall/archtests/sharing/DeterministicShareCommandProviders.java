package com.onecall.archtests.sharing;

import com.onecall.core.sharing.internal.provider.*;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.AuthorizedMediaFacts;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.ReplayAnchor;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class DeterministicShareCommandProviders {
    private DeterministicShareCommandProviders() { }

    public static MediaPlaybackSigningKeyProvider mediaPlaybackSigningKeys() {
        return () -> new MediaPlaybackSigningKeyProvider.Result.Available(new MediaPlaybackSigningKeyProvider.Entry("K1",
                HexFormat.of().parseHex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"),
                MediaPlaybackSigningKeyProvider.Role.ACTIVE), List.of());
    }

    public static ReplayAnchor mediaReplayAnchor() { return new ReplayAnchor(1767225600000000L, 1767225600000000L, "K1"); }

    public static AuthorizedMediaFacts mediaFacts(UUID evidenceId) {
        return new AuthorizedMediaFacts(evidenceId, UUID.fromString("00000000-0000-0000-0000-000000000004"),
                "a".repeat(64), 16, "audio/wav", 3600, 0, 3_600_000_000_000L);
    }

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
                generation == 1 ? "test-code" : "test-code-" + grantId + "-" + generation,
                generation == 1 ? "test_salt_abcdefghijklmnop" : "test_salt_" + digest(grantId + ":salt:" + generation),
                digest(grantId + ":" + generation)));
    }

    public static DesignatedMobileBindingProvider mobiles() {
        return mobile -> new ProviderResult.Allowed<>(new DesignatedMobileBindingProvider.MobileHmacMaterial(digest(mobile.strip()), "test-key-v1"));
    }

    public static ShareCreationPolicyProvider policy() {
        return request -> new ProviderResult.Allowed<>(ShareCreationPolicyProvider.CreationPolicyDecision.ALLOW);
    }

    public static ShareRotationPolicyProvider rotationPolicy() {
        return request -> new ProviderResult.Allowed<>(ShareRotationPolicyProvider.RotationPolicyDecision.ALLOW);
    }
}
