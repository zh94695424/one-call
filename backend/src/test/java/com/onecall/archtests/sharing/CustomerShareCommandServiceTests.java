package com.onecall.archtests.sharing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.shareprojection.*;
import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import com.onecall.core.sharing.internal.command.*;
import com.onecall.core.sharing.internal.commandservice.DefaultCustomerShareCommandService;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository.*;
import com.onecall.core.sharing.internal.provider.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class CustomerShareCommandServiceTests {
    static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    static final Instant EXPIRY = Instant.parse("2026-11-05T10:00:00.123456Z");
    static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    static final UUID EVIDENCE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    static final UUID SUBJECT = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void createBuildsDeterministicIdentityAndFingerprint() {
        for (VerificationMode mode : VerificationMode.values()) {
            Fixture fixture = new Fixture(mode);
            var created = (CustomerShareCommandResult.Created) fixture.execute(" raw ключ 😀 ", EXPIRY);
            ShareGrant grant = fixture.stored.get();
            var mapper = JsonMapper.builder().build();
            var snapshot = mapper.readTree(grant.contentSnapshot().data());
            assertThat(grant.shareId()).isEqualTo("share:" + DeterministicShareCommandProviders.digest(
                    "{\"creationRequestId\":\" raw ключ 😀 \",\"creatorAccountId\":\"" + ACCOUNT + "\"}"));
            assertThat(snapshot.path("content").path("artifacts").get(0).path("artifactRole").asString()).isEqualTo("ORIGINAL");
            var creation = snapshot.path("creation").deepCopy();
            String fingerprint = creation.path("creationFingerprint").asString();
            ((tools.jackson.databind.node.ObjectNode) creation).remove("creationFingerprint");
            assertThat(fingerprint).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(creation)));
            assertThat(creation.path("expiresAt").asString()).isEqualTo("2026-11-05T10:00:00.123456Z");
            assertThat(created.state().validFrom()).isEqualTo(NOW);
            assertThat(grant.codeGeneration()).isEqualTo(mode == VerificationMode.EXTRACTION_CODE ? 1 : 0);
            assertThat(created.extractionCode().isPresent()).isEqualTo(mode == VerificationMode.EXTRACTION_CODE);
            assertThat(snapshot.path("verification").has("mobileHmac")).isEqualTo(mode == VerificationMode.DESIGNATED_MOBILE);
            assertThat(grant.contentSnapshot().data()).doesNotContain("test-token", "test-code", "test_salt", "13800138000");
            var order = inOrder(fixture.repository, fixture.reader, fixture.policy, fixture.tokens);
            order.verify(fixture.repository).assertCommandRole(fixture.dsl);
            order.verify(fixture.repository).acquireCreatorWorkspaceLocks(fixture.dsl, fixture.caller);
            order.verify(fixture.repository).readCommandContext(fixture.dsl, fixture.caller, EVIDENCE);
            order.verify(fixture.repository).switchToEvidenceRole(fixture.dsl);
            order.verify(fixture.reader).readShareableProjection(fixture.dsl, EVIDENCE);
            order.verify(fixture.repository).restoreAndAssertCommandRole(fixture.dsl);
            order.verify(fixture.repository).sampleDatabaseTime(fixture.dsl);
            order.verify(fixture.repository).readRiskOwnership(fixture.dsl, fixture.reference);
            order.verify(fixture.policy).evaluate(any());
            order.verify(fixture.repository).findCreationIdentity(fixture.dsl, grant.shareId());
            order.verify(fixture.tokens).issue(grant.id());
            order.verify(fixture.repository).findGrantIdByTokenHash(fixture.dsl, grant.capabilityTokenHash());
            order.verify(fixture.repository).insertGrantReturning(fixture.dsl, grant);
            order.verify(fixture.repository).appendOutbox(eq(fixture.dsl), any());
            order.verify(fixture.repository).appendAudit(eq(fixture.dsl), any());
            Instant offsetExpiry = java.time.OffsetDateTime.parse("2026-11-05T12:00:00.123456+02:00").toInstant();
            clearInvocations(fixture.repository, fixture.tokens, fixture.codes);
            var replay = (CustomerShareCommandResult.CreationReplayed) fixture.execute(" raw ключ 😀 ", offsetExpiry);
            assertThat(replay.state()).isEqualTo(created.state());
            assertThat(replay.state().expiresAt()).isEqualTo(EXPIRY);
            assertThat(fixture.stored.get()).isSameAs(grant);
            verifyNoInteractions(fixture.tokens, fixture.codes);
            verify(fixture.repository, never()).insertGrantReturning(any(), any());
            verify(fixture.repository, never()).appendOutbox(any(), any());
            verify(fixture.repository, never()).appendAudit(any(), any());
            fixture.stored.set(null);
            var offsetCreated = (CustomerShareCommandResult.Created) fixture.execute(" raw ключ 😀 ", offsetExpiry);
            ShareGrant offsetGrant = fixture.stored.get();
            var offsetSnapshot = mapper.readTree(offsetGrant.contentSnapshot().data());
            assertThat(offsetGrant.shareId()).isEqualTo(grant.shareId());
            assertThat(offsetSnapshot.path("creation").path("creationFingerprint").asString()).isEqualTo(fingerprint);
            assertThat(offsetSnapshot.path("creation").path("scopeDigest")).isEqualTo(snapshot.path("creation").path("scopeDigest"));
            assertThat(offsetSnapshot.path("creation").path("expiresAt").asString()).isEqualTo("2026-11-05T10:00:00.123456Z");
            assertThat(offsetGrant.expiresAt()).isEqualTo(grant.expiresAt()).isEqualTo(EXPIRY);
            assertThat(offsetCreated.state()).isEqualTo(created.state());
            assertThat(((CustomerShareCommandResult.CreationReplayed) fixture.execute(" raw ключ 😀 ", EXPIRY)).state())
                    .isEqualTo(created.state());
        }
        assertThat(DefaultCustomerShareCommandService.utc(Instant.parse("2026-11-05T10:00:00.1Z"))).isEqualTo("2026-11-05T10:00:00.100000Z");
        assertThat(DefaultCustomerShareCommandService.utc(NOW)).isEqualTo("2026-11-04T10:00:00Z");
        var canonicalMapper = JsonMapper.builder().build();
        assertThat(DefaultCustomerShareCommandService.canonical(canonicalMapper.readTree(
                "[333333333.33333329,1E30,4.50,2e-3,0.000000000000000000000000001,4.9e-324,-0.0]")))
                .isEqualTo("[333333333.3333333,1e+30,4.5,0.002,1e-27,5e-324,0]");
        String controls = java.util.stream.IntStream.rangeClosed(0, 31)
                .mapToObj(codePoint -> String.valueOf((char) codePoint)).collect(java.util.stream.Collectors.joining());
        String escapedControls = "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000b\\f\\r\\u000e\\u000f"
                + "\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f";
        assertThat(DefaultCustomerShareCommandService.canonical(canonicalMapper.createObjectNode()
                .put("z", " ordinary ключ 😀 ").put(controls, controls)))
                .isEqualTo("{\"" + escapedControls + "\":\"" + escapedControls + "\",\"z\":\" ordinary ключ 😀 \"}");
        for (String requestId : List.of(" raw ключ 😀 ", " raw\013\037 ключ 😀 ")) {
            Fixture controlled = new Fixture(VerificationMode.NONE);
            String escapedRequestId = requestId.equals(" raw ключ 😀 ") ? requestId : " raw\\u000b\\u001f ключ 😀 ";
            String escapedMimeType = "audio/\\u000b\\u001fwav";
            when(controlled.reader.readShareableProjection(any(), any())).thenReturn(
                    new ShareableEvidenceProjectionReader.ReadResult.Found(new ShareableEvidenceProjection(
                            EVIDENCE, SUBJECT, "SOLIDIFIED", "CALL_RECORDING", "control-source", controlled.reference.identityAtCapture(), List.of(
                                    new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), 10, "audio/\013\037wav", 1)))));
            var controlledCreated = (CustomerShareCommandResult.Created) controlled.execute(requestId, EXPIRY);
            ShareGrant controlledGrant = controlled.stored.get();
            String expectedContent = "{\"artifacts\":[{\"artifactRole\":\"ORIGINAL\",\"mediaMimeType\":\"" + escapedMimeType
                    + "\",\"mediaSha256\":\"" + "a".repeat(64) + "\",\"mediaSizeBytes\":10,\"validatedDurationSeconds\":1}],\"evidenceId\":\""
                    + EVIDENCE + "\",\"identityAtCapture\":{\"subjectId\":\"" + SUBJECT + "\"},\"schemaVersion\":1,\"sourceRefId\":\"control-source\","
                    + "\"sourceType\":\"CALL_RECORDING\",\"subjectId\":\"" + SUBJECT + "\"}";
            String expectedScope = "{\"contentObject\":" + expectedContent + ",\"verificationObject\":{\"mode\":\"NONE\"}}";
            String expectedCreation = "{\"creationRequestDigest\":\"" + DeterministicShareCommandProviders.digest(requestId)
                    + "\",\"creatorAccountId\":\"" + ACCOUNT + "\",\"downloadAllowed\":false,\"evidenceId\":\"" + EVIDENCE
                    + "\",\"expiresAt\":\"2026-11-05T10:00:00.123456Z\",\"maxAdmissions\":null,\"ownerOrganizationId\":null,\"schemaVersion\":1,"
                    + "\"scopeDigest\":\"" + DeterministicShareCommandProviders.digest(expectedScope) + "\",\"verificationMode\":\"NONE\"}";
            String expectedFingerprint = DeterministicShareCommandProviders.digest(expectedCreation);
            String fingerprintedCreation = "{\"creationFingerprint\":\"" + expectedFingerprint + "\"," + expectedCreation.substring(1);
            String expectedSnapshot = "{\"content\":" + expectedContent + ",\"creation\":" + fingerprintedCreation + ",\"verification\":{\"mode\":\"NONE\"}}";
            String expectedComplete = "{\"contentObject\":" + expectedContent + ",\"creationObject\":" + fingerprintedCreation
                    + ",\"verificationObject\":{\"mode\":\"NONE\"}}";
            assertThat(controlledGrant.shareId()).isEqualTo("share:" + DeterministicShareCommandProviders.digest(
                    "{\"creationRequestId\":\"" + escapedRequestId + "\",\"creatorAccountId\":\"" + ACCOUNT + "\"}"));
            assertThat(controlledGrant.contentSnapshot().data()).isEqualTo(expectedSnapshot);
            assertThat(controlledGrant.contentSha256()).isEqualTo(DeterministicShareCommandProviders.digest(expectedComplete));
            assertThat(((CustomerShareCommandResult.CreationReplayed) controlled.execute(requestId, EXPIRY)).state()).isEqualTo(controlledCreated.state());
            controlled.stored.set(null);
            controlled.execute(requestId, EXPIRY);
            assertThat(controlled.stored.get().shareId()).isEqualTo(controlledGrant.shareId());
            assertThat(controlled.stored.get().contentSnapshot()).isEqualTo(controlledGrant.contentSnapshot());
            assertThat(controlled.stored.get().contentSha256()).isEqualTo(controlledGrant.contentSha256());
        }
        Fixture invalid = new Fixture(VerificationMode.NONE);
        fails(() -> invalid.execute("key", EXPIRY.plusNanos(1)), ShareCommandException.Code.VALIDATION_FAILED);
        verifyNoInteractions(invalid.repository, invalid.tokens);
        when(invalid.tokens.issue(any())).thenReturn(new ProviderResult.Allowed<>(new CapabilityTokenProvider.CapabilityTokenMaterial("token", "a".repeat(64))));
        fails(() -> invalid.execute("key", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        verify(invalid.repository, never()).insertGrantReturning(any(), any());
        Fixture invalidCode = new Fixture(VerificationMode.EXTRACTION_CODE);
        when(invalidCode.codes.generate(any(), anyInt())).thenReturn(new ProviderResult.Allowed<>(new ExtractionCodeMaterial("code", "short", "a".repeat(64))));
        fails(() -> invalidCode.execute("key", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        when(invalidCode.codes.generate(any(), anyInt())).thenReturn(new ProviderResult.Allowed<>(new ExtractionCodeMaterial("code", "s".repeat(22), "A".repeat(64))));
        fails(() -> invalidCode.execute("key", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        Fixture invalidMobile = new Fixture(VerificationMode.DESIGNATED_MOBILE);
        when(invalidMobile.mobiles.bind(anyString())).thenReturn(new ProviderResult.Allowed<>(new DesignatedMobileBindingProvider.MobileHmacMaterial("A".repeat(64), "v1")));
        fails(() -> invalidMobile.execute("key", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        when(invalidMobile.mobiles.bind(anyString())).thenReturn(new ProviderResult.Allowed<>(new DesignatedMobileBindingProvider.MobileHmacMaterial("a".repeat(64), "v".repeat(65))));
        fails(() -> invalidMobile.execute("key", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        assertThatNullPointerException().isThrownBy(() -> new ProviderResult.Allowed<>(null));
        assertThatNullPointerException().isThrownBy(() -> new ShareCommandCaller(ACCOUNT, null));
        assertThatNullPointerException().isThrownBy(() -> new CustomerShareCommand.Create("key", EVIDENCE, VerificationMode.NONE, EXPIRY, false, null, null, null));
    }

    @Test
    void createChecksRiskAndPolicyBeforeReplay() {
        Fixture fixture = new Fixture(VerificationMode.NONE);
        fixture.execute("key", EXPIRY);
        clearInvocations(fixture.repository, fixture.policy, fixture.tokens);
        when(fixture.repository.readRiskOwnership(any(), any())).thenReturn(Optional.empty());
        fails(() -> fixture.execute("key", EXPIRY), ShareCommandException.Code.SHARE_RISK_BLOCKED);
        verify(fixture.repository, never()).findCreationIdentity(any(), any());
        verifyNoInteractions(fixture.policy, fixture.tokens);
        when(fixture.repository.readRiskOwnership(any(), any())).thenReturn(Optional.of(fixture.risk));
        when(fixture.policy.evaluate(any())).thenReturn(new ProviderResult.Denied<>(ShareCommandException.Code.FORBIDDEN));
        fails(() -> fixture.execute("key", EXPIRY), ShareCommandException.Code.SHARE_CREATION_CONFLICT);
        when(fixture.policy.evaluate(any())).thenReturn(new ProviderResult.Unavailable<>(ShareCommandException.Code.SERVICE_UNAVAILABLE));
        fails(() -> fixture.execute("key", EXPIRY), ShareCommandException.Code.SHARE_ACCESS_POLICY_UNAVAILABLE);
        when(fixture.policy.evaluate(any())).thenReturn(new ProviderResult.Allowed<>(ShareCreationPolicyProvider.CreationPolicyDecision.DENY));
        fails(() -> fixture.execute("key", EXPIRY), ShareCommandException.Code.SHARE_CREATION_CONFLICT);
        fails(() -> fixture.execute("key", NOW), ShareCommandException.Code.VALIDATION_FAILED);
        fails(() -> fixture.service.execute(new CustomerShareCommand.Create("personal-org-member", EVIDENCE, VerificationMode.ORGANIZATION_MEMBER,
                EXPIRY, false, Optional.empty(), null, null), fixture.caller), ShareCommandException.Code.VALIDATION_FAILED);
        Fixture mobile = new Fixture(VerificationMode.DESIGNATED_MOBILE);
        when(mobile.mobiles.bind(anyString())).thenReturn(new ProviderResult.Denied<>(ShareCommandException.Code.VALIDATION_FAILED));
        fails(() -> mobile.execute("key", EXPIRY), ShareCommandException.Code.VALIDATION_FAILED);
        when(mobile.mobiles.bind(anyString())).thenReturn(new ProviderResult.Unavailable<>(ShareCommandException.Code.SERVICE_UNAVAILABLE));
        fails(() -> mobile.execute("key", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        when(mobile.reader.readShareableProjection(any(), any())).thenReturn(new ShareableEvidenceProjectionReader.ReadResult.Missing());
        fails(() -> mobile.execute("key", EXPIRY), ShareCommandException.Code.FORBIDDEN);
        when(mobile.reader.readShareableProjection(any(), any())).thenReturn(new ShareableEvidenceProjectionReader.ReadResult.Unavailable());
        fails(() -> mobile.execute("key", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        verify(mobile.repository, atLeast(1)).restoreAndAssertCommandRole(mobile.dsl);
        Fixture restore = new Fixture(VerificationMode.NONE);
        doThrow(new ShareCommandException(ShareCommandException.Code.SERVICE_UNAVAILABLE)).when(restore.repository).restoreAndAssertCommandRole(restore.dsl);
        fails(() -> restore.execute("restore", EXPIRY), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        verify(restore.repository, never()).sampleDatabaseTime(any());
        verifyNoInteractions(restore.tokens, restore.policy);
    }

    @Test
    void createReplaysMetadataWithoutSecrets() {
        for (VerificationMode mode : VerificationMode.values()) {
            Fixture fixture = new Fixture(mode);
            var created = (CustomerShareCommandResult.Created) fixture.execute("key", EXPIRY);
            clearInvocations(fixture.repository, fixture.tokens, fixture.codes, fixture.mobiles);
            when(fixture.repository.sampleDatabaseTime(any())).thenReturn(NOW.plusSeconds(100));
            var replayed = (CustomerShareCommandResult.CreationReplayed) fixture.execute("key", EXPIRY);
            assertThat(replayed.state()).isEqualTo(created.state());
            verifyNoInteractions(fixture.tokens, fixture.codes);
            verify(fixture.repository, never()).insertGrantReturning(any(), any());
            verify(fixture.repository, never()).appendOutbox(any(), any());
            verify(fixture.repository, never()).appendAudit(any(), any());
            if (mode == VerificationMode.DESIGNATED_MOBILE) {
                verify(fixture.mobiles).bind("13800138000");
                var equivalent = new CustomerShareCommand.Create("key", EVIDENCE, mode, EXPIRY, false, Optional.empty(), null, " 13800138000 ");
                assertThat(fixture.service.execute(equivalent, fixture.caller)).isEqualTo(replayed);
                var changed = new CustomerShareCommand.Create("key", EVIDENCE, mode, EXPIRY, false, Optional.empty(), null, "13800138001");
                fails(() -> fixture.service.execute(changed, fixture.caller), ShareCommandException.Code.SHARE_CREATION_CONFLICT);
                verifyNoInteractions(fixture.tokens, fixture.codes);
            }
        }
    }

    @Test
    void createRejectsChangedFingerprint() {
        Fixture fixture = new Fixture(VerificationMode.NONE);
        fixture.execute("key", EXPIRY);
        clearInvocations(fixture.repository, fixture.tokens);
        fails(() -> fixture.execute("key", EXPIRY.plusSeconds(1)), ShareCommandException.Code.SHARE_CREATION_CONFLICT);
        verifyNoInteractions(fixture.tokens);
        verify(fixture.repository, never()).appendOutbox(any(), any());
        Fixture collision = new Fixture(VerificationMode.NONE);
        when(collision.repository.findGrantIdByTokenHash(any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        fails(() -> collision.execute("key", EXPIRY), ShareCommandException.Code.INTERNAL);
        verify(collision.repository, never()).insertGrantReturning(any(), any());
    }

    private static void fails(Runnable action, ShareCommandException.Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ShareCommandException.class, failure -> assertThat(failure.code()).isEqualTo(code));
    }

    static final class Fixture {
        final DSLContext dsl = mock(DSLContext.class);
        final SharingCommandRepository repository = mock(SharingCommandRepository.class);
        final ShareableEvidenceProjectionReader reader = mock(ShareableEvidenceProjectionReader.class);
        final ShareCreationPolicyProvider policy = mock(ShareCreationPolicyProvider.class);
        final CapabilityTokenProvider tokens = mock(CapabilityTokenProvider.class);
        final ShareExtractionCodeProvider codes = mock(ShareExtractionCodeProvider.class);
        final DesignatedMobileBindingProvider mobiles = mock(DesignatedMobileBindingProvider.class);
        final AtomicReference<ShareGrant> stored = new AtomicReference<>();
        final ShareCommandCaller caller;
        final EvidenceReference reference = new EvidenceReference(EVIDENCE, SUBJECT, "SOLIDIFIED", JSONB.valueOf("{\"subjectId\":\"" + SUBJECT + "\"}"));
        final RiskOwnershipSnapshot risk;
        final VerificationMode mode;
        final CustomerShareCommandService service;

        Fixture(VerificationMode mode) {
            this.mode = mode;
            when(policy.evaluate(any())).thenAnswer(invocation -> DeterministicShareCommandProviders.policy().evaluate(invocation.getArgument(0)));
            caller = new ShareCommandCaller(ACCOUNT, mode == VerificationMode.ORGANIZATION_MEMBER ? Optional.of(UUID.randomUUID()) : Optional.empty());
            risk = new RiskOwnershipSnapshot(SUBJECT, Optional.of(ACCOUNT), caller.organizationId(), true,
                    Optional.empty(), Optional.of(UUID.randomUUID()), true, false, "CURRENT");
            TransactionTemplate template = mock(TransactionTemplate.class);
            when(template.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(new SimpleTransactionStatus()));
            when(repository.readCommandContext(any(), any(), any())).thenReturn(Optional.of(new CommandContext(ACCOUNT, "ACTIVE", caller.organizationId(),
                    Optional.empty(), Optional.empty(), Optional.empty(), reference)));
            when(repository.sampleDatabaseTime(any())).thenReturn(NOW);
            when(repository.readRiskOwnership(any(), any())).thenReturn(Optional.of(risk));
            when(repository.findCreationIdentity(any(), any())).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
            when(repository.findGrantIdByTokenHash(any(), any())).thenReturn(Optional.empty());
            when(repository.insertGrantReturning(any(), any())).thenAnswer(invocation -> { stored.set(invocation.getArgument(1)); return Optional.of(stored.get()); });
            when(reader.readShareableProjection(any(), any())).thenReturn(new ShareableEvidenceProjectionReader.ReadResult.Found(new ShareableEvidenceProjection(
                    EVIDENCE, SUBJECT, "SOLIDIFIED", "CALL_RECORDING", UUID.randomUUID().toString(), reference.identityAtCapture(), List.of(
                            new ShareableEvidenceProjection.ArtifactProjection("DERIVED", "b".repeat(64), 20, "audio/wav", null),
                            new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), 10, "audio/wav", 1)))));
            when(tokens.issue(any())).thenAnswer(invocation -> DeterministicShareCommandProviders.tokens().issue(invocation.getArgument(0)));
            when(codes.generate(any(), anyInt())).thenAnswer(invocation -> DeterministicShareCommandProviders.codes().generate(invocation.getArgument(0), invocation.getArgument(1)));
            when(mobiles.bind(anyString())).thenAnswer(invocation -> DeterministicShareCommandProviders.mobiles().bind(invocation.getArgument(0)));
            service = new DefaultCustomerShareCommandService(template, dsl, repository, reader, policy, tokens, codes, mobiles, DeterministicShareCommandProviders.sha256());
        }

        CustomerShareCommandResult execute(String key, Instant expiry) {
            return service.execute(new CustomerShareCommand.Create(key, EVIDENCE, mode, expiry, false, Optional.of(UUID.randomUUID()), null,
                    mode == VerificationMode.DESIGNATED_MOBILE ? "13800138000" : null), caller);
        }
    }
}
