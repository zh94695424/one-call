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
            String expectedScope = "{\"content\":" + expectedContent + ",\"verification\":{\"mode\":\"NONE\"}}";
            String expectedCreation = "{\"creationRequestDigest\":\"" + DeterministicShareCommandProviders.digest(requestId)
                    + "\",\"creatorAccountId\":\"" + ACCOUNT + "\",\"downloadAllowed\":false,\"evidenceId\":\"" + EVIDENCE
                    + "\",\"expiresAt\":\"2026-11-05T10:00:00.123456Z\",\"maxAdmissions\":null,\"ownerOrganizationId\":null,\"schemaVersion\":1,"
                    + "\"scopeDigest\":\"" + DeterministicShareCommandProviders.digest(expectedScope) + "\",\"verificationMode\":\"NONE\"}";
            String expectedFingerprint = DeterministicShareCommandProviders.digest(expectedCreation);
            String fingerprintedCreation = "{\"creationFingerprint\":\"" + expectedFingerprint + "\"," + expectedCreation.substring(1);
            String expectedSnapshot = "{\"content\":" + expectedContent + ",\"creation\":" + fingerprintedCreation + ",\"verification\":{\"mode\":\"NONE\"}}";
            String expectedComplete = "{\"content\":" + expectedContent + ",\"creation\":" + fingerprintedCreation
                    + ",\"verification\":{\"mode\":\"NONE\"}}";
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

    @Test
    void revokeReplaysBeforeStatePreconditions() {
        Fixture fixture = new Fixture(VerificationMode.NONE);
        fixture.execute("create", EXPIRY);
        ShareGrant grant = fixture.stored.get();
        when(fixture.repository.readActiveCallerContext(any(), any())).thenReturn(Optional.of(new ActiveCallerContext(
                ACCOUNT, "ACTIVE", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));
        when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.of(grant));
        var mapper = JsonMapper.builder().build();
        String raw = "revoke,raw-key";
        String requestDigest = DeterministicShareCommandProviders.digest(raw);
        var identity = mapper.createObjectNode().put("shareGrantId", grant.id().toString()).put("actionType", "REVOKE");
        identity.set("sourceIdentity", mapper.createObjectNode().put("kind", "REQUEST").put("actionRequestId", "revoke,raw-key"));
        String eventId = "share-transition:" + DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(identity));
        var semantics = mapper.createObjectNode().put("shareGrantId", grant.id().toString()).put("actionType", "REVOKE")
                .put("reasonCode", " exact reason ").putNull("newExpiresAt").put("actionRequestDigest", requestDigest);
        String fingerprint = DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(semantics));
        var transition = new PersistedTransition(grant.id(), eventId, "REVOKE", requestDigest, fingerprint,
                com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.REVOKED, 0, 1, 2, NOW);
        when(fixture.repository.findTransition(any(), eq(grant.id()), eq(eventId))).thenReturn(Optional.of(transition));
        clearInvocations(fixture.repository, fixture.reader, fixture.policy, fixture.tokens, fixture.codes, fixture.mobiles);
        for (var status : com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.values()) {
            ShareGrant current = grantWithState(grant, status, 9);
            when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.of(current));
            when(fixture.repository.sampleDatabaseTime(any())).thenReturn(EXPIRY.plusSeconds(1));
            for (Integer witness : new Integer[] {null, 0, 9, 10}) {
                var result = (CustomerShareCommandResult.Revoked) fixture.service.execute(new CustomerShareCommand.Revoke(
                        grant.shareId(), raw, Optional.of(UUID.randomUUID()), witness, " exact reason "), fixture.caller);
                assertThat(result.state().status()).isEqualTo(transition.status());
                assertThat(result.state().revocationGeneration()).isEqualTo(1);
            }
        }
        var order = inOrder(fixture.repository);
        order.verify(fixture.repository).assertCommandRole(fixture.dsl);
        order.verify(fixture.repository).acquireCreatorWorkspaceLocks(fixture.dsl, fixture.caller);
        order.verify(fixture.repository).readActiveCallerContext(fixture.dsl, fixture.caller);
        order.verify(fixture.repository).lockGrant(fixture.dsl, grant.shareId(), fixture.caller);
        order.verify(fixture.repository).sampleDatabaseTime(fixture.dsl);
        order.verify(fixture.repository).findTransition(fixture.dsl, grant.id(), eventId);
        fails(() -> fixture.service.execute(new CustomerShareCommand.Revoke(grant.shareId(), raw, Optional.empty(), 0,
                "exact reason"), fixture.caller), ShareCommandException.Code.SHARE_TRANSITION_CONFLICT);
        when(fixture.repository.readActiveCallerContext(any(), any())).thenReturn(Optional.empty());
        fails(() -> fixture.service.execute(new CustomerShareCommand.Revoke(grant.shareId(), raw, Optional.empty(), null,
                " exact reason "), fixture.caller), ShareCommandException.Code.FORBIDDEN);
        when(fixture.repository.readActiveCallerContext(any(), any())).thenReturn(Optional.of(new ActiveCallerContext(
                ACCOUNT, "ACTIVE", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));
        when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.empty());
        fails(() -> fixture.service.execute(new CustomerShareCommand.Revoke(grant.shareId(), raw, Optional.empty(), null,
                " exact reason "), fixture.caller), ShareCommandException.Code.NOT_FOUND);
        when(fixture.repository.findTransition(any(), any(), any())).thenReturn(Optional.empty());
        for (var status : com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.values()) {
            when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.of(grantWithState(grant, status, 2)));
            for (Instant sampled : List.of(NOW, EXPIRY, EXPIRY.plusSeconds(1))) {
                when(fixture.repository.sampleDatabaseTime(any())).thenReturn(sampled);
                var expected = status.name().equals("REVOKED") ? ShareCommandException.Code.SHARE_REVOKED
                        : status.name().equals("EXPIRED") || !sampled.isBefore(EXPIRY) ? ShareCommandException.Code.SHARE_TRANSITION_CONFLICT
                        : ShareCommandException.Code.SHARE_GENERATION_STALE;
                fails(() -> fixture.service.execute(new CustomerShareCommand.Revoke(grant.shareId(), raw, Optional.empty(), 0, null), fixture.caller), expected);
            }
        }
        verify(fixture.repository, never()).appendAccessEvent(any(), any());
        verify(fixture.repository, never()).transitionGrant(any(), any(), anyInt(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), any());
        verify(fixture.repository, never()).appendOutbox(any(), any());
        verify(fixture.repository, never()).appendAudit(any(), any());
        verifyNoInteractions(fixture.reader, fixture.policy, fixture.tokens, fixture.codes, fixture.mobiles);
        clearInvocations(fixture.repository, fixture.template);
        for (var invalid : List.of(new CustomerShareCommand.Revoke(" ", raw, Optional.empty(), null, null),
                new CustomerShareCommand.Revoke("x".repeat(129), raw, Optional.empty(), null, null),
                new CustomerShareCommand.Revoke(grant.shareId(), " ", Optional.empty(), null, null),
                new CustomerShareCommand.Revoke(grant.shareId(), "😀".repeat(129), Optional.empty(), null, null),
                new CustomerShareCommand.Revoke(grant.shareId(), "bad\0", Optional.empty(), null, null),
                new CustomerShareCommand.Revoke(grant.shareId(), raw, Optional.empty(), -1, null),
                new CustomerShareCommand.Revoke(grant.shareId(), raw, Optional.empty(), null, " "),
                new CustomerShareCommand.Revoke(grant.shareId(), raw, Optional.empty(), null, "x".repeat(129)))) {
            fails(() -> fixture.service.execute(invalid, fixture.caller), ShareCommandException.Code.VALIDATION_FAILED);
        }
        verifyNoInteractions(fixture.repository);
        verifyNoInteractions(fixture.template);
    }

    @Test
    void rotateReplayNeverReturnsCode() {
        Fixture fixture = new Fixture(VerificationMode.EXTRACTION_CODE);
        fixture.execute("rotation-create", EXPIRY);
        ShareGrant grant = grantWithState(fixture.stored.get(),
                com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.LOCKED, 4);
        when(fixture.repository.readActiveCallerContext(any(), any())).thenReturn(Optional.of(new ActiveCallerContext(
                ACCOUNT, "ACTIVE", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));
        when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.of(grant));
        String raw = " raw ключ,😀 \u0001";
        String requestDigest = DeterministicShareCommandProviders.digest(raw);
        var mapper = JsonMapper.builder().build();
        var identity = mapper.createObjectNode().put("shareGrantId", grant.id().toString()).put("actionType", "CODE_ROTATE");
        identity.set("sourceIdentity", mapper.createObjectNode().put("kind", "REQUEST").put("actionRequestId", raw));
        String eventId = "share-transition:" + DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(identity));
        var semantics = mapper.createObjectNode().put("shareGrantId", grant.id().toString()).put("actionType", "CODE_ROTATE")
                .putNull("reasonCode").putNull("newExpiresAt").put("actionRequestDigest", requestDigest);
        String fingerprint = DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(semantics));
        var transition = new PersistedTransition(grant.id(), eventId, "CODE_ROTATE", requestDigest, fingerprint,
                com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.ACTIVE, 2, 5, 3, NOW);
        when(fixture.repository.findTransition(any(), eq(grant.id()), eq(eventId))).thenReturn(Optional.of(transition));
        clearInvocations(fixture.repository, fixture.reader, fixture.policy, fixture.rotationPolicy, fixture.tokens, fixture.codes, fixture.mobiles);
        for (var status : com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.values()) {
            when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.of(grantWithState(grant, status, 99)));
            when(fixture.repository.sampleDatabaseTime(any())).thenReturn(EXPIRY.plusSeconds(1));
            for (Integer witness : new Integer[] {null, 0, 1, 99}) {
                var result = (CustomerShareCommandResult.RotationReplayed) fixture.service.execute(new CustomerShareCommand.Rotate(
                        grant.shareId(), raw, Optional.of(UUID.randomUUID()), witness), fixture.caller);
                assertThat(result.status()).isEqualTo(transition.status());
                assertThat(result.codeGeneration()).isEqualTo(2);
                assertThat(result.revocationGeneration()).isEqualTo(5);
                assertThat(mapper.writeValueAsString(result)).doesNotContain("extractionCode", "capabilityToken", "test-code", "test-token");
            }
        }
        var replayOrder = inOrder(fixture.repository);
        replayOrder.verify(fixture.repository).assertCommandRole(fixture.dsl);
        replayOrder.verify(fixture.repository).acquireCreatorWorkspaceLocks(fixture.dsl, fixture.caller);
        replayOrder.verify(fixture.repository).readActiveCallerContext(fixture.dsl, fixture.caller);
        replayOrder.verify(fixture.repository).lockGrant(fixture.dsl, grant.shareId(), fixture.caller);
        replayOrder.verify(fixture.repository).sampleDatabaseTime(fixture.dsl);
        replayOrder.verify(fixture.repository).findTransition(fixture.dsl, grant.id(), eventId);
        verifyNoInteractions(fixture.reader, fixture.policy, fixture.rotationPolicy, fixture.tokens, fixture.codes, fixture.mobiles);
        verify(fixture.repository, never()).appendAccessEvent(any(), any());
        when(fixture.repository.findTransition(any(), any(), any())).thenReturn(Optional.of(new PersistedTransition(
                grant.id(), eventId, "CODE_ROTATE", requestDigest, "a".repeat(64), transition.status(), 2, 5, 3, NOW)));
        fails(() -> fixture.service.execute(new CustomerShareCommand.Rotate(grant.shareId(), raw, Optional.empty(), null), fixture.caller),
                ShareCommandException.Code.SHARE_TRANSITION_CONFLICT);
        when(fixture.repository.readActiveCallerContext(any(), any())).thenReturn(Optional.empty());
        fails(() -> fixture.service.execute(new CustomerShareCommand.Rotate(grant.shareId(), raw, Optional.empty(), null), fixture.caller),
                ShareCommandException.Code.FORBIDDEN);
        when(fixture.repository.readActiveCallerContext(any(), any())).thenReturn(Optional.of(new ActiveCallerContext(
                ACCOUNT, "ACTIVE", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())));
        when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.empty());
        fails(() -> fixture.service.execute(new CustomerShareCommand.Rotate(grant.shareId(), raw, Optional.empty(), null), fixture.caller),
                ShareCommandException.Code.NOT_FOUND);
        when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.of(grant));
        when(fixture.repository.findTransition(any(), any(), any())).thenReturn(Optional.empty());
        when(fixture.repository.sampleDatabaseTime(any())).thenReturn(NOW);
        var command = new CustomerShareCommand.Rotate(grant.shareId(), raw, Optional.empty(), 1);
        for (var refusal : List.<ProviderResult<ShareRotationPolicyProvider.RotationPolicyDecision>>of(
                new ProviderResult.Denied<>(ShareCommandException.Code.INTERNAL),
                new ProviderResult.Allowed<>(ShareRotationPolicyProvider.RotationPolicyDecision.DENY),
                new ProviderResult.Unavailable<>(ShareCommandException.Code.INTERNAL))) {
            when(fixture.rotationPolicy.evaluate(any())).thenReturn(refusal);
            fails(() -> fixture.service.execute(command, fixture.caller), refusal instanceof ProviderResult.Unavailable<?>
                    ? ShareCommandException.Code.SERVICE_UNAVAILABLE : ShareCommandException.Code.FORBIDDEN);
        }
        when(fixture.rotationPolicy.evaluate(any())).thenReturn(null);
        fails(() -> fixture.service.execute(command, fixture.caller), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        when(fixture.rotationPolicy.evaluate(any())).thenThrow(new IllegalStateException("POLICY_CANARY"));
        fails(() -> fixture.service.execute(command, fixture.caller), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        verifyNoInteractions(fixture.codes);
        doReturn(new ProviderResult.Allowed<>(ShareRotationPolicyProvider.RotationPolicyDecision.ALLOW)).when(fixture.rotationPolicy).evaluate(any());
        for (var material : List.of(new ExtractionCodeMaterial("", "valid_salt_abcdefghijklmnop", "b".repeat(64)),
                new ExtractionCodeMaterial("code", "short", "b".repeat(64)),
                new ExtractionCodeMaterial("code", "valid_salt_abcdefghijklmnop", "INVALID"),
                new ExtractionCodeMaterial("code", grant.codeSalt(), "b".repeat(64)),
                new ExtractionCodeMaterial("code", "valid_salt_abcdefghijklmnop", grant.codeHash()))) {
            when(fixture.codes.generate(any(), anyInt())).thenReturn(new ProviderResult.Allowed<>(material));
            fails(() -> fixture.service.execute(command, fixture.caller), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        }
        verify(fixture.repository, never()).appendAccessEvent(any(), any());
        when(fixture.codes.generate(any(), anyInt())).thenAnswer(invocation -> DeterministicShareCommandProviders.codes()
                .generate(invocation.getArgument(0), invocation.getArgument(1)));
        when(fixture.repository.transitionGrant(any(), any(), anyInt(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    ExtractionCodeMaterial material = ((Optional<ExtractionCodeMaterial>) invocation.getArgument(10)).orElseThrow();
                    return new TransitionResult.Updated(new ShareGrant(grant.id(), grant.shareId(), grant.capabilityTokenHash(), grant.evidenceId(),
                            grant.createdByAccountId(), grant.grantOwnerOrganizationId(), grant.verificationMode(), material.salt(), material.codeHash(),
                            2, grant.validFrom(), grant.expiresAt(), grant.maxAdmissions(), grant.admissionsUsed(), grant.downloadAllowed(),
                            grant.contentSnapshot(), grant.contentSha256(), transition.status(), 5, grant.revision() + 1));
                });
        clearInvocations(fixture.repository, fixture.rotationPolicy, fixture.codes);
        var rotated = (CustomerShareCommandResult.Rotated) fixture.service.execute(command, fixture.caller);
        assertThat(rotated.extractionCode()).isEqualTo("test-code-" + grant.id() + "-2");
        var order = inOrder(fixture.repository, fixture.rotationPolicy, fixture.codes);
        order.verify(fixture.repository).assertCommandRole(fixture.dsl);
        order.verify(fixture.repository).acquireCreatorWorkspaceLocks(fixture.dsl, fixture.caller);
        order.verify(fixture.repository).readActiveCallerContext(fixture.dsl, fixture.caller);
        order.verify(fixture.repository).lockGrant(fixture.dsl, grant.shareId(), fixture.caller);
        order.verify(fixture.repository).sampleDatabaseTime(fixture.dsl);
        order.verify(fixture.repository).findTransition(fixture.dsl, grant.id(), eventId);
        order.verify(fixture.rotationPolicy).evaluate(new ShareRotationPolicyProvider.RotationPolicyRequest(fixture.caller,
                grant.id(), grant.verificationMode(), grant.status(), 1, 4));
        order.verify(fixture.codes).generate(grant.id(), 2);
        order.verify(fixture.repository).appendAccessEvent(eq(fixture.dsl), any());
        order.verify(fixture.repository).transitionGrant(eq(fixture.dsl), eq(grant.id()), eq(grant.revision()), eq(grant.status()),
                eq(Optional.empty()), eq(Optional.of(1)), eq(transition.status()), eq(grant.revision() + 1), eq(2), eq(5), any());
        order.verify(fixture.repository).appendOutbox(eq(fixture.dsl), any());
        order.verify(fixture.repository).appendAudit(eq(fixture.dsl), any());
        for (String overflow : List.of("revision", "code", "revocation")) {
            ShareGrant maximum = new ShareGrant(grant.id(), grant.shareId(), grant.capabilityTokenHash(), grant.evidenceId(), grant.createdByAccountId(),
                    grant.grantOwnerOrganizationId(), grant.verificationMode(), grant.codeSalt(), grant.codeHash(), overflow.equals("code") ? Integer.MAX_VALUE : 1,
                    grant.validFrom(), grant.expiresAt(), grant.maxAdmissions(), grant.admissionsUsed(), grant.downloadAllowed(), grant.contentSnapshot(),
                    grant.contentSha256(), grant.status(), overflow.equals("revocation") ? Integer.MAX_VALUE : 4,
                    overflow.equals("revision") ? Integer.MAX_VALUE : grant.revision());
            when(fixture.repository.lockGrant(any(), any(), any())).thenReturn(Optional.of(maximum));
            clearInvocations(fixture.codes, fixture.repository);
            fails(() -> fixture.service.execute(new CustomerShareCommand.Rotate(grant.shareId(), raw, Optional.empty(), null), fixture.caller),
                    ShareCommandException.Code.SERVICE_UNAVAILABLE);
            verifyNoInteractions(fixture.codes);
            verify(fixture.repository, never()).appendAccessEvent(any(), any());
        }
        clearInvocations(fixture.repository, fixture.template);
        for (var invalid : List.of(new CustomerShareCommand.Rotate(" ", raw, Optional.empty(), null),
                new CustomerShareCommand.Rotate("x".repeat(129), raw, Optional.empty(), null),
                new CustomerShareCommand.Rotate(grant.shareId(), " ", Optional.empty(), null),
                new CustomerShareCommand.Rotate(grant.shareId(), "😀".repeat(129), Optional.empty(), null),
                new CustomerShareCommand.Rotate(grant.shareId(), "bad\0", Optional.empty(), null),
                new CustomerShareCommand.Rotate(grant.shareId(), "\uD800", Optional.empty(), null),
                new CustomerShareCommand.Rotate(grant.shareId(), raw, Optional.empty(), -1)))
            fails(() -> fixture.service.execute(invalid, fixture.caller), ShareCommandException.Code.VALIDATION_FAILED);
        assertThatThrownBy(() -> new CustomerShareCommand.Rotate(grant.shareId(), raw, null, null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(fixture.repository, fixture.template);
    }

    private static ShareGrant grantWithState(ShareGrant grant,
            com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus status, int generation) {
        return new ShareGrant(grant.id(), grant.shareId(), grant.capabilityTokenHash(), grant.evidenceId(), grant.createdByAccountId(),
                grant.grantOwnerOrganizationId(), grant.verificationMode(), grant.codeSalt(), grant.codeHash(), grant.codeGeneration(),
                grant.validFrom(), grant.expiresAt(), grant.maxAdmissions(), grant.admissionsUsed(), grant.downloadAllowed(),
                grant.contentSnapshot(), grant.contentSha256(), status, generation, grant.revision());
    }

    static final class Fixture {
        final DSLContext dsl = mock(DSLContext.class);
        final SharingCommandRepository repository = mock(SharingCommandRepository.class);
        final TransactionTemplate template = mock(TransactionTemplate.class);
        final ShareableEvidenceProjectionReader reader = mock(ShareableEvidenceProjectionReader.class);
        final ShareCreationPolicyProvider policy = mock(ShareCreationPolicyProvider.class);
        final ShareRotationPolicyProvider rotationPolicy = mock(ShareRotationPolicyProvider.class);
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
            when(rotationPolicy.evaluate(any())).thenAnswer(invocation -> DeterministicShareCommandProviders.rotationPolicy().evaluate(invocation.getArgument(0)));
            caller = new ShareCommandCaller(ACCOUNT, mode == VerificationMode.ORGANIZATION_MEMBER ? Optional.of(UUID.randomUUID()) : Optional.empty());
            risk = new RiskOwnershipSnapshot(SUBJECT, Optional.of(ACCOUNT), caller.organizationId(), true,
                    Optional.empty(), Optional.of(UUID.randomUUID()), true, false, "CURRENT");
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
            service = new DefaultCustomerShareCommandService(template, dsl, repository, reader, policy, rotationPolicy, tokens, codes, mobiles, DeterministicShareCommandProviders.sha256());
        }

        CustomerShareCommandResult execute(String key, Instant expiry) {
            return service.execute(new CustomerShareCommand.Create(key, EVIDENCE, mode, expiry, false, Optional.of(UUID.randomUUID()), null,
                    mode == VerificationMode.DESIGNATED_MOBILE ? "13800138000" : null), caller);
        }
    }
}
