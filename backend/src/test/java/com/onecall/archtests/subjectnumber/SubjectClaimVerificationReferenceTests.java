package com.onecall.archtests.subjectnumber;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.subject_number.customerclaim.*;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.*;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException.Code;
import com.onecall.core.subject_number.internal.claim.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimProofVerifier.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimReferenceCipher.SealedReference;
import com.onecall.core.subject_number.internal.claim.SubjectClaimRepository.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimVerificationReference.Payload;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class SubjectClaimVerificationReferenceTests {
    private org.springframework.boot.test.system.CapturedOutput captured;
    @org.junit.jupiter.api.BeforeEach
    void captureOutput(org.springframework.boot.test.system.CapturedOutput output) { captured = output; }
    private final SubjectClaimVerificationReference codec = new SubjectClaimVerificationReference();
    private final UUID subject = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID binding = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private Payload payload(String proof) { return new Payload(1, subject, binding, "a".repeat(64), VerificationMethod.SMS, proof); }

    @Test
    void v1PayloadHasExactCanonicalKeysAndNoAccountOrRawProof() {
        byte[] encoded = codec.encode(payload("server-reference"));
        String expected = "{\"bindingId\":\"" + binding + "\",\"numberHmac\":\"" + "a".repeat(64)
                + "\",\"proofRef\":\"server-reference\",\"schemaVersion\":1,\"subjectId\":\"" + subject + "\",\"verificationMethod\":\"SMS\"}";
        assertThat(Arrays.equals(encoded, expected.getBytes(StandardCharsets.UTF_8))).isTrue();
        assertThat(codec.decode(encoded).equals(payload("server-reference"))).isTrue();
        var root = tools.jackson.databind.json.JsonMapper.builder().build().readTree(encoded);
        assertThat(root.propertyNames()).containsExactly("bindingId", "numberHmac", "proofRef", "schemaVersion", "subjectId", "verificationMethod");
        String control = "reference\u000b😀\n\"\\";
        byte[] controlBytes = codec.encode(payload(control));
        assertThat(new String(controlBytes, StandardCharsets.UTF_8).contains("\\u000b")).isTrue();
        assertThat(codec.decode(controlBytes).equals(payload(control))).isTrue();
    }
    @Test
    void strictPayloadRejectsMissingExtraDuplicateAndMalformedValues() {
        String original = new String(codec.encode(payload("reference")), StandardCharsets.UTF_8);
        for (String invalid : List.of(original + "{}", " " + original, original.replace("\"schemaVersion\":1,", ""),
                original.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"extra\":1"),
                original.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"),
                original.replace("\"schemaVersion\":1", "\"schemaVersion\":1.0"), original.replace("\"SMS\"", "\"sms\""),
                original.replace("a".repeat(64), "A".repeat(64)), original.replace("reference", " "), original.replace("reference", "x".repeat(129)),
                original.replace(subject.toString(), "1-1-1-1-1"), original.replace("reference", "\\u0000"))) {
            byte[] bytes = invalid.getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> codec.decode(bytes)).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid verification reference");
        }
        assertThatThrownBy(() -> codec.decode(new byte[]{(byte) 0xc3, 0x28})).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void wrongVersionAndInvalidEnvelopeFailClosed() {
        assertThatThrownBy(() -> new Payload(2, subject, binding, "a".repeat(64), VerificationMethod.SMS, "reference")).isInstanceOf(IllegalArgumentException.class);
        for (String envelope : List.of("v2.key.AAAA", "v1..AAAA", "v1.key.", "v1.key.a+b", "v1.key.a=b", "v1.key.a b"))
            assertThat(SubjectClaimVerificationReference.envelope(envelope)).isFalse();
        assertThat(SubjectClaimVerificationReference.envelope("v1.path/key.ref:one-2_A.AAAA")).isTrue();
        var providers = new ControlledSubjectClaimProviders();
        assertThat(providers.open(new SealedReference("v1.test/key.1.AAAA"))).isInstanceOf(SubjectClaimProviderResult.Invalid.class);
    }
    @Test
    void undecryptableAndUnavailableReferencesRemainDistinct() {
        for (boolean unavailable : List.of(false, true)) {
            var harness = new CustomerSubjectClaimServiceTests.Harness(); var row = harness.stored(ClaimStatus.REJECTED);
            when(harness.repository.findByRequestId(harness.context, "request")).thenReturn(Optional.of(row));
            harness.providers.force(row.verificationRef(), unavailable ? new SubjectClaimProviderResult.Unavailable<>() : new SubjectClaimProviderResult.Invalid<>());
            harness.fails(unavailable ? Code.SERVICE_UNAVAILABLE : Code.VALIDATION_FAILED);
            if (unavailable) { assertThat(harness.audits).isEmpty(); assertThat(harness.rollbacks).hasValue(1); }
            else harness.audit(Reason.CLAIM_REPLAY_UNRESOLVABLE, Outcome.DENIED, RiskEvaluation.NO_OPEN);
        }
    }
    @Test
    void randomizedCiphertextsCompareByDecodedLogicalIdentity() {
        var providers = new ControlledSubjectClaimProviders(); var payload = payload("reference");
        String first = providers.sealPayload(payload), second = providers.sealPayload(payload);
        assertThat(first.equals(second)).isFalse();
        var openedFirst = (SubjectClaimProviderResult.Allowed<byte[]>) providers.open(new SealedReference(first));
        var openedSecond = (SubjectClaimProviderResult.Allowed<byte[]>) providers.open(new SealedReference(second));
        assertThat(codec.decode(openedFirst.value()).equals(codec.decode(openedSecond.value()))).isTrue();
        var harness = new CustomerSubjectClaimServiceTests.Harness(); var row = harness.stored(ClaimStatus.ACCEPTED); harness.accepted(row);
        when(harness.repository.findByRequestId(harness.context, "request")).thenReturn(Optional.of(row));
        assertThat(harness.run().claimId()).isEqualTo(row.id()); assertThat(harness.rows).isEmpty();
    }
    @Test
    void rowSubjectMethodAndBindingHmacMustMatchPayload() {
        for (int dimension = 0; dimension < 4; dimension++) {
            var harness = new CustomerSubjectClaimServiceTests.Harness(); var row = harness.stored(ClaimStatus.REJECTED);
            var changed = new Payload(1, dimension == 0 ? UUID.randomUUID() : harness.subject, dimension == 2 ? UUID.randomUUID() : harness.binding,
                    dimension == 3 ? "b".repeat(64) : harness.hmac, dimension == 1 ? VerificationMethod.MANUAL_REVIEW : VerificationMethod.SMS, "server-proof");
            harness.providers.force(row.verificationRef(), new SubjectClaimProviderResult.Allowed<>(codec.encode(changed)));
            when(harness.repository.findByRequestId(harness.context, "request")).thenReturn(Optional.of(row));
            harness.fails(dimension < 2 ? Code.VALIDATION_FAILED : Code.CLAIM_REPLAY_CONFLICT);
        }
        var harness = new CustomerSubjectClaimServiceTests.Harness();
        SubjectClaimReferenceCipher substitute = new SubjectClaimReferenceCipher() {
            public SubjectClaimProviderResult<SealedReference> seal(byte[] bytes) { return new SubjectClaimProviderResult.Allowed<>(new SealedReference("v1.key.AAAA")); }
            public SubjectClaimProviderResult<byte[]> open(SealedReference reference) { return new SubjectClaimProviderResult.Allowed<>(codec.encode(payload("other"))); }
        };
        harness.service = new DefaultCustomerSubjectClaimService(harness.transaction, harness.context, harness.repository, harness.providers, substitute);
        harness.fails(Code.SERVICE_UNAVAILABLE); assertThat(harness.rows).isEmpty();
    }
    @Test
    void serverResolvedProofReferenceNotClientTextDefinesIdentity() {
        for (boolean same : List.of(false, true)) {
            var harness = new CustomerSubjectClaimServiceTests.Harness(); var row = harness.stored(ClaimStatus.REJECTED);
            when(harness.repository.findByRequestId(harness.context, "request")).thenReturn(Optional.of(row));
            harness.approve("different-client-attempt", VerificationMethod.SMS, same ? "server-proof" : "different-server-proof");
            harness.command = harness.command("request", VerificationMethod.SMS, "different-client-attempt");
            if (same) assertThat(harness.run().claimId()).isEqualTo(row.id()); else harness.fails(Code.CLAIM_REPLAY_CONFLICT);
        }
    }
    @Test
    void sensitiveValuesAndProviderFailuresHaveRedactedRepresentations() {
        String canary = "SENSITIVE_CANARY";
        byte[] plaintext = canary.getBytes(StandardCharsets.UTF_8);
        var allowed = new SubjectClaimProviderResult.Allowed<>(plaintext); plaintext[0] = 0;
        assertThat(allowed.value()[0] == 'S').isTrue(); byte[] output = allowed.value(); output[0] = 0; assertThat(allowed.value()[0] == 'S').isTrue();
        var harness = new CustomerSubjectClaimServiceTests.Harness();
        List<Object> values = List.of(payload(canary), new ProofRequest(subject, subject, binding, VerificationMethod.SMS, canary),
                new ApprovedProof(subject, subject, binding, VerificationMethod.SMS, canary), new SealedReference(canary),
                new SubjectClaimProviderResult.Allowed<>(canary), allowed, new SubjectClaimProviderResult.Invalid<>(), new SubjectClaimProviderResult.Unavailable<>(),
                new ClaimCommand(subject, binding, VerificationMethod.SMS, canary, canary, UUID.randomUUID()),
                new BindingIdentity(binding, subject, canary), new BindingRow(binding, subject, canary, canary, harness.time),
                new SubjectRow(subject, subject, null, 1), new AccountRow(subject, canary), new RiskRow(subject, canary),
                new AcceptedOwnerRow(subject, subject, null), new ClaimRow(subject, subject, subject, null, VerificationMethod.SMS, canary, canary, ClaimStatus.REJECTED, harness.time, null),
                new NewClaim(subject, subject, subject, null, VerificationMethod.SMS, canary, canary, ClaimStatus.REJECTED, harness.time, null),
                new AuditDecision(harness.time, subject, subject, canary, subject, Reason.CLAIM_REPLAYED, Outcome.REPLAYED, RiskEvaluation.NO_OPEN));
        for (Object value : values) assertThat(value.toString().contains(canary)).isFalse();
        var failure = new SubjectClaimException(Code.SERVICE_UNAVAILABLE); assertThat(failure.getCause()).isNull();
        assertThat(failure.toString().contains(canary)).isFalse();
        assertThat(captured.getAll().contains(canary)).isFalse();
    }
}
