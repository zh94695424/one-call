package com.onecall.archtests.sharing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.core.sharing.internal.playback.MediaPlaybackService.Reason.*;

import com.onecall.core.sharing.internal.admission.*;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository.*;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SharingPlaybackStateReaderTests {
    static final Instant NOW = MediaPlaybackServiceTests.NOW;
    static final JsonMapper JSON = JsonMapper.builder().build();

    @Test void activeAndAdmittedLockedStateAllowWithoutMutation() {
        Fixture fixture = new Fixture();
        for (String status : List.of("ACTIVE", "LOCKED")) {
            fixture.grant = fixture.grant(status, NOW.plusSeconds(600), 0, 0); fixture.stub();
            assertThat(fixture.reader().check(fixture.claim)).isEqualTo(new PlaybackStateReader.Result.Allowed(NOW));
        }
        fixture.noWrites();
        verify(fixture.repository, never()).findGrantByTokenDigest(anyString());
    }

    @Test void grantAdmissionAndEvidenceTupleMustMatchVerifiedIdentity() {
        Fixture fixture = new Fixture();
        assertThat(fixture.reader().check(null)).isEqualTo(denied(MALFORMED_CLAIM));
        verifyNoInteractions(fixture.repository);
        fixture.stub();
        when(fixture.repository.findGrantById(fixture.grantId)).thenReturn(Optional.empty());
        assertThat(fixture.check()).isEqualTo(denied(ADMISSION_MISMATCH));
        fixture.stub();
        when(fixture.repository.findGrantById(fixture.grantId)).thenReturn(Optional.of(new Fixture().grant));
        assertThat(fixture.check()).isEqualTo(denied(ADMISSION_MISMATCH));
        fixture.stub();
        when(fixture.repository.findAdmissionById(fixture.grantId, fixture.admissionId)).thenReturn(Optional.empty());
        assertThat(fixture.check()).isEqualTo(denied(ADMISSION_MISMATCH));
        fixture.stub();
        when(fixture.repository.findAdmissionById(fixture.grantId, fixture.admissionId)).thenReturn(Optional.of(new Fixture().admission));
        assertThat(fixture.check()).isEqualTo(denied(ADMISSION_MISMATCH));
        fixture.stub();
        assertThat(fixture.reader().check(MediaPlaybackServiceTests.replace(fixture.claim, "evidenceId", UUID.randomUUID())))
                .isEqualTo(denied(SCOPE_MISMATCH));
        verify(fixture.repository, atLeastOnce()).findGrantById(fixture.grantId);
        verify(fixture.repository, atLeastOnce()).findAdmissionById(fixture.grantId, fixture.admissionId);
    }

    @Test void witnessShapeHashTypesAndBoundsMustBeCanonical() {
        Fixture fixture = new Fixture(); fixture.stub();
        var original = fixture.witness;
        for (String payload : List.of("[]", "{}", fixture.payload().replace("\"codeGeneration\":0", "\"codeGeneration\":\"0\""),
                fixture.payload().replace("\"codeGeneration\":0", "\"codeGeneration\":0.0"),
                fixture.payload().replace("\"codeGeneration\":0", "\"codeGeneration\":2147483648"),
                fixture.payload().replace("}", ",\"extra\":true}"),
                fixture.payload().replace("00:00:40Z", "00:00:40.000000Z"))) {
            fixture.witness = fixture.witness(JSON.readTree(payload)); fixture.stub();
            assertThat(fixture.check()).isEqualTo(new PlaybackStateReader.Result.Unavailable());
        }
        fixture.witness = original;
        for (Instant expiry : Arrays.asList(null, NOW.minusSeconds(100), NOW.plusSeconds(10))) {
            fixture.grant = fixture.grant("ACTIVE", expiry, 0, 0); fixture.stub();
            assertThat(fixture.check()).isInstanceOf(PlaybackStateReader.Result.Unavailable.class);
        }
        for (Instant from : Arrays.asList(null, NOW.plusSeconds(600), NOW.plusSeconds(601))) {
            fixture.validFrom = from;
            fixture.grant = fixture.grant("ACTIVE", NOW.plusSeconds(600), 0, 0); fixture.stub();
            assertThat(fixture.check()).isInstanceOf(PlaybackStateReader.Result.Unavailable.class);
        }
        fixture.validFrom = NOW.minusSeconds(60);
        fixture.grantRevision++;
        fixture.grant = fixture.grant("ACTIVE", NOW.plusSeconds(1200), 0, 0); fixture.stub();
        assertThat(fixture.check()).isEqualTo(new PlaybackStateReader.Result.Allowed(NOW));
        VerifiedMediaClaim cap = MediaPlaybackServiceTests.replace(fixture.claim, "expiresAt", NOW.plusSeconds(30));
        assertThat(fixture.reader().check(cap)).isEqualTo(new PlaybackStateReader.Result.Allowed(NOW));
        assertThat(fixture.reader().check(MediaPlaybackServiceTests.replace(cap, "expiresAt", NOW.plusSeconds(31))))
                .isEqualTo(denied(SCOPE_MISMATCH));
        for (long seconds : List.of(61L, 601L)) assertThat(fixture.reader().check(MediaPlaybackServiceTests.replace(cap, "expiresAt", NOW.plusSeconds(seconds))))
                .isEqualTo(denied(SCOPE_MISMATCH));
        when(fixture.repository.now()).thenReturn(NOW.plusSeconds(30));
        assertThat(fixture.reader().check(cap)).isEqualTo(denied(EXPIRED));
        assertThat(fixture.witness).isSameAs(original);
        assertThat(fixture.grant.revision()).isEqualTo(3);
        fixture.noWrites();
    }

    @Test void revokedExpiredNotYetValidAndBothStaleGenerationsDeny() {
        Fixture fixture = new Fixture();
        for (String status : List.of("REVOKED", "EXPIRED", "UNKNOWN")) {
            fixture.grant = fixture.grant(status, NOW.plusSeconds(600), 0, 0); fixture.stub();
            assertThat(fixture.check()).isEqualTo(denied(status.equals("REVOKED") ? REVOKED : status.equals("EXPIRED") ? EXPIRED : CURRENT_STATE_DENIED));
        }
        for (boolean code : List.of(true, false)) {
            fixture.grant = fixture.grant("ACTIVE", NOW.plusSeconds(600), code ? 1 : 0, code ? 0 : 1); fixture.stub();
            assertThat(fixture.check()).isEqualTo(denied(STALE_GENERATION));
        }
        fixture.grant = fixture.grant("ACTIVE", NOW.plusSeconds(600), 0, 0); fixture.stub();
        when(fixture.repository.now()).thenReturn(NOW.minusSeconds(61));
        assertThat(fixture.check()).isEqualTo(denied(NOT_YET_VALID));
        when(fixture.repository.now()).thenReturn(NOW, fixture.claim.expiresAt());
        assertThat(fixture.check()).isEqualTo(denied(EXPIRED));
    }

    @Test void organizationMemberWitnessRequiresCurrentAccountTenantAndMembership() {
        Fixture fixture = new Fixture(); fixture.member = true;
        fixture.grant = fixture.grant("ACTIVE", NOW.plusSeconds(600), 0, 0);
        fixture.admission = fixture.admission(); fixture.witness = fixture.witness(JSON.readTree(fixture.payload())); fixture.stub();
        assertThat(fixture.check()).isEqualTo(new PlaybackStateReader.Result.Allowed(NOW));
        when(fixture.repository.readCurrentAccount(fixture.accountId)).thenReturn(Optional.of(new AccountState(fixture.accountId, "DISABLED")));
        assertThat(fixture.check()).isEqualTo(denied(CURRENT_STATE_DENIED));
        fixture.stub();
        when(fixture.repository.readCurrentMembership(fixture.organizationId, fixture.accountId)).thenReturn(Optional.of(new MembershipState(UUID.randomUUID(), fixture.accountId, "ACTIVE")));
        assertThat(fixture.check()).isEqualTo(denied(CURRENT_STATE_DENIED));
        fixture.stub();
        when(fixture.repository.readCurrentOrganization(fixture.organizationId)).thenReturn(Optional.empty());
        assertThat(fixture.check()).isEqualTo(denied(CURRENT_STATE_DENIED));
    }

    @Test void ownershipCapturedBindingAndCurrentRiskFailClosed() {
        Fixture fixture = new Fixture(); fixture.stub();
        when(fixture.repository.readSubjectOwnership(fixture.subjectId)).thenReturn(Optional.empty());
        assertThat(fixture.check()).isEqualTo(new PlaybackStateReader.Result.Unavailable());
        fixture.stub();
        when(fixture.repository.readCurrentBindings(fixture.subjectId)).thenReturn(List.of());
        assertThat(fixture.check()).isEqualTo(denied(CURRENT_STATE_DENIED));
        fixture.stub();
        when(fixture.repository.readCurrentBindings(fixture.subjectId)).thenReturn(List.of(fixture.binding, fixture.binding));
        assertThat(fixture.check()).isEqualTo(denied(CURRENT_STATE_DENIED));
        for (String state : List.of("OPEN", "UNKNOWN")) {
            fixture.stub(); when(fixture.repository.readSubjectRisk(fixture.subjectId, List.of(fixture.bindingId)))
                    .thenReturn(Optional.of(new RiskState(fixture.subjectId, List.of(fixture.bindingId), List.of(new RiskRow(UUID.randomUUID(), "CONFLICT", state, 1)))));
            assertThat(fixture.check()).isEqualTo(state.equals("OPEN") ? denied(CURRENT_STATE_DENIED) : new PlaybackStateReader.Result.Unavailable());
        }
    }

    @Test void readOnlyRepeatableSnapshotClosesBeforeReturningAndOriginalTemplateIsUnchanged() {
        Fixture fixture = new Fixture(); fixture.stub();
        assertThat(fixture.check()).isEqualTo(new PlaybackStateReader.Result.Allowed(NOW));
        assertThat(fixture.transactions.history).containsExactly("begin", "commit");
        assertThat(fixture.transactions.definition.isReadOnly()).isTrue();
        assertThat(fixture.transactions.definition.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThat(fixture.transactions.definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(fixture.template.isReadOnly()).isFalse();
        assertThat(fixture.template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
        assertThat(fixture.template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        fixture.noWrites();
    }

    @Test void repositoryHashAndTransactionFaultsAreUnavailableAndRedacted() {
        Fixture fixture = new Fixture(); fixture.stub();
        when(fixture.repository.findGrantById(fixture.grantId)).thenThrow(new IllegalStateException("private-bucket https://private/secret"));
        assertThat(fixture.check()).isEqualTo(new PlaybackStateReader.Result.Unavailable());
        fixture.stub();
        assertThat(new SharingPlaybackStateReader(fixture.template, fixture.repository, bytes -> "BAD").check(fixture.claim))
                .isEqualTo(new PlaybackStateReader.Result.Unavailable());
        fixture.transactions.failCommit = true;
        assertThat(fixture.check()).isEqualTo(new PlaybackStateReader.Result.Unavailable());
        assertThat(fixture.check().toString()).doesNotContain("private", "https://");
    }

    static PlaybackStateReader.Result denied(MediaPlaybackService.Reason reason) { return new PlaybackStateReader.Result.Denied(reason); }
    static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    static final class Transactions extends AbstractPlatformTransactionManager {
        final List<String> history = new ArrayList<>();
        TransactionDefinition definition;
        boolean failCommit;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { this.definition = definition; history.add("begin"); }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            history.add("commit"); if (failCommit) throw new IllegalStateException("private commit failure");
        }
        @Override protected void doRollback(DefaultTransactionStatus status) { history.add("rollback"); }
    }
    static final class Fixture {
        final UUID grantId = UUID.randomUUID(), admissionId = UUID.randomUUID(), evidenceId = UUID.randomUUID(), mediaId = UUID.randomUUID(),
                accountId = UUID.randomUUID(), organizationId = UUID.randomUUID(), subjectId = UUID.randomUUID(), bindingId = UUID.randomUUID();
        final ShareAdmissionRepository repository = mock(ShareAdmissionRepository.class);
        final Transactions transactions = new Transactions();
        final TransactionTemplate template = new TransactionTemplate(transactions);
        boolean member;
        Instant validFrom = NOW.minusSeconds(60);
        long grantRevision = 2;
        GrantSnapshot grant = grant("ACTIVE", NOW.plusSeconds(600), 0, 0);
        AdmissionRow admission = admission();
        AdmissionWitness witness = witness(JSON.readTree(payload()));
        final BindingState binding = new BindingState(bindingId, subjectId, "ACTIVE", NOW.minusSeconds(60), 1);
        final VerifiedMediaClaim claim = new VerifiedMediaClaim("onecall:media-playback:v1", grantId, admissionId, evidenceId, mediaId, 0, 0, Purpose.PLAYBACK,
                NOW.minusSeconds(2), NOW.minusSeconds(1), NOW.plusSeconds(20), 10, new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10)));
        Fixture() { }
        GrantSnapshot grant(String status, Instant expiry, int code, int revocation) {
            return new GrantSnapshot(grantId, "synthetic", evidenceId, accountId, member ? organizationId : null, status, member ? "ORGANIZATION_MEMBER" : "NONE",
                    validFrom, expiry, code, revocation, grantRevision, 1L, 1, null, null, subjectId, null, false, JSON.readTree("{}"));
        }
        AdmissionRow admission() { return new AdmissionRow(admissionId, grantId, "admission:fixture", 1, member ? "ORGANIZATION_MEMBER" : "NONE", member ? "VERIFIED" : "NOT_REQUIRED", NOW.minusSeconds(5)); }
        String payload() {
            Map<String,Object> values = new TreeMap<>(Map.of("verificationMode", member ? "ORGANIZATION_MEMBER" : "NONE", "codeGeneration", 0, "revocationGeneration", 0,
                    "grantExpiresAt", NOW.plusSeconds(600).toString(), "admissionExpiresAt", NOW.plusSeconds(60).toString(), "sessionExpiresAt", NOW.plusSeconds(30).toString()));
            if (member) { values.put("recipientAccountId", accountId.toString()); values.put("recipientOrganizationId", organizationId.toString()); }
            return JSON.writeValueAsString(values);
        }
        AdmissionWitness witness(JsonNode payload) {
            return new AdmissionWitness("admission-granted:" + grantId + ":" + admissionId, payload,
                    digest(JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8)), member ? "ORGANIZATION_MEMBER" : "NONE", 0, 0,
                    NOW.plusSeconds(600), NOW.plusSeconds(60), NOW.plusSeconds(30), member ? accountId : null, member ? organizationId : null);
        }
        void stub() {
            reset(repository);
            when(repository.findGrantById(grantId)).thenReturn(Optional.of(grant));
            when(repository.findAdmissionById(grantId, admissionId)).thenReturn(Optional.of(admission));
            when(repository.findAdmissionWitness(grantId, admissionId)).thenReturn(Optional.of(witness));
            when(repository.now()).thenReturn(NOW);
            when(repository.readSubjectOwnership(subjectId)).thenReturn(Optional.of(new SubjectOwnership(subjectId, member ? null : accountId, member ? organizationId : null)));
            when(repository.readCurrentBindings(subjectId)).thenReturn(List.of(binding));
            when(repository.readSubjectRisk(subjectId, List.of(bindingId))).thenReturn(Optional.of(new RiskState(subjectId, List.of(bindingId), List.of(new RiskRow(UUID.randomUUID(), "CONFLICT", "CLEARED", 1)))));
            when(repository.readCurrentOrganization(organizationId)).thenReturn(Optional.of(new OrganizationState(organizationId, "ACTIVE")));
            when(repository.readCurrentMembership(organizationId, accountId)).thenReturn(Optional.of(new MembershipState(organizationId, accountId, "ACTIVE")));
            when(repository.readCurrentAccount(accountId)).thenReturn(Optional.of(new AccountState(accountId, "ACTIVE")));
            clearInvocations(repository);
        }
        SharingPlaybackStateReader reader() { return new SharingPlaybackStateReader(template, repository, SharingPlaybackStateReaderTests::digest); }
        PlaybackStateReader.Result check() { return reader().check(claim); }
        void noWrites() {
            verify(repository, never()).lockGrant(any());
            verify(repository, never()).insertAdmission(any());
            verify(repository, never()).insertAccessEvent(any());
            verify(repository, never()).expireGrant(any(), anyLong());
            verify(repository, never()).updateGrantAdmissionCount(any(), anyLong(), anyLong());
            verify(repository, never()).acquireAccountLifecycleLock(any());
            verify(repository, never()).acquireOrganizationLifecycleLock(any());
            verify(repository, never()).acquireMembershipLifecycleLock(any(), any());
        }
    }
}
