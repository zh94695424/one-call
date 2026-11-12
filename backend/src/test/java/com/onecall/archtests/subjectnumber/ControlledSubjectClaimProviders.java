package com.onecall.archtests.subjectnumber;

import com.onecall.core.subject_number.internal.claim.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimProofVerifier.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimReferenceCipher.SealedReference;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class ControlledSubjectClaimProviders implements SubjectClaimProofVerifier, SubjectClaimReferenceCipher {
    private final Map<String, ApprovedProof> approvals = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> opens = new ConcurrentHashMap<>();
    private final Map<String, SubjectClaimProviderResult<byte[]>> forced = new ConcurrentHashMap<>();
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();
    public Runnable probe = () -> { };
    public final AtomicInteger verifications = new AtomicInteger();
    public final AtomicInteger seals = new AtomicInteger();
    public boolean unavailable;

    public ControlledSubjectClaimProviders() {
        try { var generator = KeyGenerator.getInstance("AES"); generator.init(256); key = generator.generateKey(); }
        catch (Exception failure) { throw new IllegalStateException("Controlled cipher initialization failed"); }
    }
    public void approve(String attempt, ApprovedProof proof) {
        var previous = approvals.putIfAbsent(attempt, proof);
        if (previous != null && !previous.equals(proof)) throw new IllegalArgumentException("Immutable approval");
    }
    public void force(String envelope, SubjectClaimProviderResult<byte[]> result) { forced.put(envelope, result); }
    public int opens(String envelope) { return opens.getOrDefault(envelope, new AtomicInteger()).get(); }
    @Override public SubjectClaimProviderResult<ApprovedProof> verify(ProofRequest request) {
        probe.run(); verifications.incrementAndGet();
        if (unavailable) return new SubjectClaimProviderResult.Unavailable<>();
        ApprovedProof approved = approvals.get(request.verificationProofId());
        if (approved == null) return new SubjectClaimProviderResult.Invalid<>();
        if (!request.accountId().equals(approved.accountId()) || !request.subjectId().equals(approved.subjectId())
                || !request.bindingId().equals(approved.bindingId()) || request.method() != approved.method()) return new SubjectClaimProviderResult.Invalid<>();
        return new SubjectClaimProviderResult.Allowed<>(approved);
    }
    @Override public SubjectClaimProviderResult<SealedReference> seal(byte[] payload) {
        probe.run(); seals.incrementAndGet();
        if (unavailable) return new SubjectClaimProviderResult.Unavailable<>();
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD("v1.test/key.1".getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(payload.clone());
            byte[] packed = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, packed, 0, nonce.length); System.arraycopy(encrypted, 0, packed, nonce.length, encrypted.length);
            return new SubjectClaimProviderResult.Allowed<>(new SealedReference("v1.test/key.1." + Base64.getUrlEncoder().withoutPadding().encodeToString(packed)));
        } catch (Exception failure) { return new SubjectClaimProviderResult.Unavailable<>(); }
    }
    @Override public SubjectClaimProviderResult<byte[]> open(SealedReference reference) {
        probe.run(); opens.computeIfAbsent(reference.envelope(), ignored -> new AtomicInteger()).incrementAndGet();
        if (unavailable) return new SubjectClaimProviderResult.Unavailable<>();
        if (forced.containsKey(reference.envelope())) return forced.get(reference.envelope());
        try {
            if (!SubjectClaimVerificationReference.envelope(reference.envelope())) return new SubjectClaimProviderResult.Invalid<>();
            int separator = reference.envelope().lastIndexOf('.');
            if (!reference.envelope().substring(0, separator).equals("v1.test/key.1")) return new SubjectClaimProviderResult.Invalid<>();
            byte[] packed = Base64.getUrlDecoder().decode(reference.envelope().substring(separator + 1));
            if (packed.length < 29) return new SubjectClaimProviderResult.Invalid<>();
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, packed, 0, 12));
            cipher.updateAAD("v1.test/key.1".getBytes(StandardCharsets.UTF_8));
            return new SubjectClaimProviderResult.Allowed<>(cipher.doFinal(packed, 12, packed.length - 12));
        } catch (Exception failure) { return new SubjectClaimProviderResult.Invalid<>(); }
    }
    public String sealPayload(SubjectClaimVerificationReference.Payload payload) {
        var sealed = (SubjectClaimProviderResult.Allowed<SealedReference>) seal(new SubjectClaimVerificationReference().encode(payload));
        return sealed.value().envelope();
    }
    @Override public String toString() { return "ControlledSubjectClaimProviders[REDACTED]"; }
}
