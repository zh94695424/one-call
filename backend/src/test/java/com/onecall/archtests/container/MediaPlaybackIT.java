package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.evidence.internal.mediaread.JooqEvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import com.onecall.core.sharing.internal.admission.*;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository.*;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.credential.*;
import com.onecall.archtests.sharing.DeterministicShareCommandProviders;
import com.onecall.core.sharing.internal.publicadmission.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class MediaPlaybackIT {
    private static final String CANARY = "PA19_PRIVATE_cos://bucket/key?version=secret";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Fixture fixture;
    private CapturedOutput output;

    @BeforeEach void start(CapturedOutput output) throws Exception { this.output = output; fixture = new Fixture(); }
    @AfterEach void stop() throws Exception { if (fixture != null) fixture.close(); }

    @Test void qualifiedAdmissionAndEvidenceRolesResolveOneObjectAndOpenOutsideTransactions() throws Exception {
        var scenario = fixture.scenario("NONE", "VALIDATED", false);
        String before = fixture.snapshot();
        for (String status : List.of("ACTIVE", "LOCKED")) {
            if (status.equals("LOCKED")) fixture.sql("UPDATE sharing.evidence_share_grant SET status='LOCKED',revision=revision+1 WHERE id=?", scenario.grant);
            String state = fixture.snapshot();
            fixture.resetTrace();
            var result = fixture.play(scenario.claim);
            assertThat(result).isInstanceOf(MediaPlaybackService.Result.Opened.class);
            var scope = ((MediaPlaybackService.Result.Opened) result).handle().scope();
            assertThat(scope.media().mediaObjectId()).isEqualTo(scenario.media);
            assertThat(scope.media().durationSeconds()).isEqualTo(10);
            assertThat(scope.purpose()).isEqualTo(Purpose.PLAYBACK);
            assertThat(scope.window()).isEqualTo(scenario.claim.window());
            assertThat(scope.expiresAt()).isEqualTo(scenario.claim.expiresAt());
            assertThat(fixture.verifications).hasValue(1);
            assertThat(fixture.resolutions).hasValue(1);
            assertThat(fixture.opens).hasValue(1);
            assertThat(fixture.evidenceKeys).containsExactly(scenario.evidence);
            assertThat(fixture.reads).anySatisfy(read -> {
                assertThat(read.role).isEqualTo("sharing_admission_role");
                assertThat(read.sessionRole).isEqualTo(read.role);
                assertThat(read.readOnly).isEqualTo("on");
                assertThat(read.isolation).isEqualTo("repeatable read");
            }).anySatisfy(read -> {
                assertThat(read.role).isEqualTo("evidence_media_reader_role");
                assertThat(read.sessionRole).isEqualTo(read.role);
                assertThat(read.readOnly).isEqualTo("on");
            });
            assertThat(fixture.reads).filteredOn(read -> read.role.equals("sharing_admission_role")).allSatisfy(read -> {
                assertThat(read.sessionRole).isEqualTo("sharing_admission_role");
                assertThat(read.readOnly).isEqualTo("on");
                assertThat(read.isolation).isEqualTo("repeatable read");
                assertThat(read.sql.toUpperCase(Locale.ROOT)).doesNotContain("INSERT", "UPDATE", "DELETE", "FOR SHARE", "PG_ADVISORY");
            });
            var evidenceReads = fixture.reads.stream().filter(read -> read.role.equals("evidence_media_reader_role")).toList();
            assertThat(evidenceReads).hasSize(1);
            assertThat(evidenceReads.getFirst().parameters).isEqualTo(1);
            assertThat(evidenceReads.getFirst().bindings).containsExactly(entry(1, scenario.evidence));
            assertThat(evidenceReads.getFirst().columns).isEqualTo(15);
            assertThat(evidenceReads.getFirst().sql).endsWith("WHERE er.id = ?");
            assertThat(fixture.snapshot()).isEqualTo(state);
        }
        assertThat(before).isNotEmpty();
        assertThat(fixture.admissionTransactions.isReadOnly()).isFalse();
        assertThat(fixture.admissionTransactions.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Test void committedRevocationRotationAndAdmissionMismatchDenyBeforeEvidence() throws Exception {
        for (String mutation : List.of("REVOKE", "CODE", "REVOCATION", "CLAIM_CODE", "CLAIM_REVOCATION", "SHARE", "ADMISSION", "EVIDENCE")) {
            var scenario = fixture.scenario("EXTRACTION_CODE", "VALIDATED", false);
            VerifiedMediaClaim claim = scenario.claim;
            var reason = MediaPlaybackService.Reason.STALE_GENERATION;
            switch (mutation) {
                case "REVOKE" -> {
                    fixture.sql("UPDATE sharing.evidence_share_grant SET status='REVOKED',revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?", scenario.grant);
                    reason = MediaPlaybackService.Reason.REVOKED;
                }
                case "CODE" -> fixture.sql("UPDATE sharing.evidence_share_grant SET code_salt=repeat('T',22),code_hash=repeat('c',64),code_generation=code_generation+1,revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?", scenario.grant);
                case "REVOCATION" -> fixture.sql("UPDATE sharing.evidence_share_grant SET revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?", scenario.grant);
                case "CLAIM_CODE", "CLAIM_REVOCATION" -> claim = new VerifiedMediaClaim(claim.audience(), claim.shareGrantId(), claim.shareAdmissionId(), claim.evidenceId(), claim.mediaObjectId(),
                        claim.revocationGeneration() + (mutation.equals("CLAIM_REVOCATION") ? 1 : 0), claim.codeGeneration() + (mutation.equals("CLAIM_CODE") ? 1 : 0),
                        claim.purpose(), claim.issuedAt(), claim.notBefore(), claim.expiresAt(), claim.durationSeconds(), claim.window());
                case "SHARE" -> {
                    var other = fixture.scenario("NONE", "VALIDATED", false);
                    claim = copy(claim, other.grant, claim.shareAdmissionId(), claim.evidenceId(), claim.expiresAt());
                    reason = MediaPlaybackService.Reason.ADMISSION_MISMATCH;
                }
                case "ADMISSION" -> {
                    var other = fixture.scenario("NONE", "VALIDATED", false);
                    claim = copy(claim, claim.shareGrantId(), other.claim.shareAdmissionId(), claim.evidenceId(), claim.expiresAt());
                    reason = MediaPlaybackService.Reason.ADMISSION_MISMATCH;
                }
                case "EVIDENCE" -> {
                    claim = copy(claim, claim.shareGrantId(), claim.shareAdmissionId(), UUID.randomUUID(), claim.expiresAt());
                    reason = MediaPlaybackService.Reason.SCOPE_MISMATCH;
                }
                default -> throw new AssertionError("Unknown fixture mutation");
            }
            String before = fixture.snapshot();
            fixture.resetTrace();
            assertThat(fixture.play(claim)).as(mutation).isEqualTo(new MediaPlaybackService.Result.Denied(reason));
            fixture.noEvidenceOrOpen();
            assertThat(fixture.snapshot()).isEqualTo(before);
        }
    }

    @Test void witnessExpiryMembershipBindingAndRiskChangesFailClosedWithoutWrites() throws Exception {
        var extension = fixture.scenario("NONE", "VALIDATED", false);
        String frozen = fixture.frozen();
        long revision = fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?", extension.grant);
        fixture.sql("UPDATE sharing.evidence_share_grant SET expires_at=expires_at+interval '1 day',revision=revision+1 WHERE id=?", extension.grant);
        String extended = fixture.snapshot();
        assertThat(fixture.state.check(extension.claim)).isInstanceOf(PlaybackStateReader.Result.Allowed.class);
        fixture.resetTrace();
        assertThat(fixture.play(extension.claim)).isInstanceOf(MediaPlaybackService.Result.Opened.class);
        assertThat(fixture.opens).hasValue(1);
        assertThat(fixture.snapshot()).isEqualTo(extended);
        assertThat(fixture.frozen()).isEqualTo(frozen);
        for (Instant beyond : List.of(extension.witness.sessionExpiresAt().plusSeconds(1),
                extension.witness.admissionExpiresAt().plusSeconds(1), extension.witness.grantExpiresAt().plusSeconds(1))) {
            fixture.resetTrace();
            var wide = copy(extension.claim, extension.grant, extension.claim.shareAdmissionId(), extension.evidence, beyond);
            assertThat(fixture.play(wide)).isEqualTo(new MediaPlaybackService.Result.Denied(MediaPlaybackService.Reason.SCOPE_MISMATCH));
            fixture.noEvidenceOrOpen();
            assertThat(fixture.snapshot()).isEqualTo(extended);
            assertThat(fixture.frozen()).isEqualTo(frozen);
        }
        fixture.databaseNow = extension.witness.sessionExpiresAt();
        fixture.resetTrace();
        assertThat(fixture.play(extension.claim)).isEqualTo(new MediaPlaybackService.Result.Denied(MediaPlaybackService.Reason.EXPIRED));
        fixture.noEvidenceOrOpen();
        fixture.databaseNow = null;
        assertThat(fixture.snapshot()).isEqualTo(extended);
        assertThat(fixture.frozen()).isEqualTo(frozen);
        assertThat(fixture.scalar("SELECT revision FROM sharing.evidence_share_grant WHERE id=?", extension.grant)).isEqualTo(revision + 1);

        for (String mutation : List.of("MEMBERSHIP", "ACCOUNT", "ORGANIZATION", "OWNERSHIP", "BINDING", "RISK", "MALFORMED_WITNESS")) {
            var scenario = fixture.scenario(mutation.equals("OWNERSHIP") ? "NONE" : "ORGANIZATION_MEMBER", "VALIDATED", false);
            VerifiedMediaClaim tested = scenario.claim;
            switch (mutation) {
                case "MEMBERSHIP" -> fixture.sql("UPDATE organization_access.organization_membership SET status='SUSPENDED',revision=revision+1 WHERE organization_id=? AND account_id=?", scenario.organization, scenario.account);
                case "ACCOUNT" -> fixture.sql("UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", scenario.account);
                case "ORGANIZATION" -> fixture.sql("UPDATE organization_access.organization SET status='SUSPENDED',revision=revision+1 WHERE id=?", scenario.organization);
                case "OWNERSHIP" -> tested = fixture.ownershipMismatchClaim(scenario);
                case "BINDING" -> fixture.sql("UPDATE subject_number.caller_number_binding SET status='RETIRED',revision=revision+1 WHERE id=?", scenario.binding);
                case "RISK" -> fixture.sql("INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,observed_at) VALUES (?,?,'RECYCLED','OPEN','OPERATIONS',clock_timestamp())", UUID.randomUUID(), scenario.binding);
                case "MALFORMED_WITNESS" -> tested = fixture.malformedWitnessClaim(scenario);
                default -> throw new AssertionError("Unknown fixture mutation");
            }
            String before = fixture.snapshot();
            fixture.resetTrace();
            var result = fixture.play(tested);
            if (mutation.equals("MALFORMED_WITNESS") || mutation.equals("OWNERSHIP")) assertThat(result).isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
            else assertThat(result).as(mutation).isEqualTo(new MediaPlaybackService.Result.Denied(MediaPlaybackService.Reason.CURRENT_STATE_DENIED));
            fixture.noEvidenceOrOpen();
            assertThat(fixture.snapshot()).isEqualTo(before);
        }
    }

    @Test void missingAmbiguousAndInconsistentEvidenceNeverOpensControlledRead() throws Exception {
        fixture.resetTrace();
        assertThat(fixture.evidence.resolve(UUID.randomUUID())).isInstanceOf(EvidenceMediaObjectReader.Result.Missing.class);
        assertThat(fixture.opens).hasValue(0);
        var scenario = fixture.scenario("NONE", "VALIDATED", false);
        UUID addedMedia = UUID.randomUUID();
        UUID addedArtifact = UUID.randomUUID();
        String index = fixture.text("SELECT pg_get_indexdef('evidence.uq_evidence_original_artifact'::regclass)");
        assertThat(index).isEqualTo("CREATE UNIQUE INDEX uq_evidence_original_artifact ON evidence.evidence_artifact USING btree (evidence_id) WHERE (artifact_role = 'ORIGINAL'::text)");
        Map<String, String> triggers = fixture.triggers();
        assertThat(triggers).hasSize(2);
        try {
            fixture.sql("DROP INDEX evidence.uq_evidence_original_artifact");
            fixture.sql("DROP TRIGGER trg_evidence_artifact_immutable ON evidence.evidence_artifact");
            fixture.sql("DROP TRIGGER trg_media_object_immutable ON evidence.media_object");
            fixture.media(addedMedia);
            fixture.sql("INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,'ORIGINAL')", addedArtifact, scenario.evidence, addedMedia);
            String before = fixture.snapshot();
            fixture.resetTrace();
            assertThat(fixture.play(scenario.claim)).isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
            assertThat(fixture.resolutions).hasValue(1);
            assertThat(fixture.opens).hasValue(0);
            assertThat(fixture.snapshot()).isEqualTo(before);
        } finally {
            try (Connection connection = fixture.base.admin()) {
                connection.setAutoCommit(false);
                SharingQueryFixture.execute(connection, "DELETE FROM evidence.evidence_artifact WHERE id=? AND evidence_id=? AND media_object_id=?", addedArtifact, scenario.evidence, addedMedia);
                SharingQueryFixture.execute(connection, "DELETE FROM evidence.media_object WHERE id=?", addedMedia);
                SharingQueryFixture.execute(connection, "CREATE TRIGGER trg_evidence_artifact_immutable BEFORE UPDATE OR DELETE ON evidence.evidence_artifact FOR EACH ROW EXECUTE FUNCTION evidence.reject_row_mutation()");
                SharingQueryFixture.execute(connection, "CREATE TRIGGER trg_media_object_immutable BEFORE UPDATE OR DELETE ON evidence.media_object FOR EACH ROW EXECUTE FUNCTION evidence.reject_row_mutation()");
                SharingQueryFixture.execute(connection, "CREATE UNIQUE INDEX uq_evidence_original_artifact ON evidence.evidence_artifact(evidence_id) WHERE artifact_role='ORIGINAL'");
                connection.commit();
            }
            assertThat(fixture.triggers()).isEqualTo(triggers);
            assertThat(fixture.text("SELECT pg_get_indexdef('evidence.uq_evidence_original_artifact'::regclass)")).isEqualTo(index);
            assertThat(fixture.scalar("SELECT count(*) FROM evidence.evidence_artifact WHERE id=?", addedArtifact)).isZero();
            assertThat(fixture.scalar("SELECT count(*) FROM evidence.media_object WHERE id=?", addedMedia)).isZero();
            assertThatThrownBy(() -> fixture.sql("INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) SELECT ?,?,media_object_id,'ORIGINAL' FROM evidence.evidence_artifact WHERE evidence_id=? AND artifact_role='DERIVED'", UUID.randomUUID(), scenario.evidence, fixture.base.evidence))
                    .isInstanceOfSatisfying(SQLException.class, failure -> {
                        assertThat(failure.getSQLState()).isEqualTo("23505");
                        assertThat(failure.getMessage()).contains("uq_evidence_original_artifact");
                    });
        }
        for (String defect : List.of("RELATIONSHIP", "STATUS", "DURATION")) {
            var inconsistent = fixture.scenario("NONE", defect.equals("STATUS") ? "QUARANTINED" : "VALIDATED", defect.equals("RELATIONSHIP"));
            fixture.missingDuration = defect.equals("DURATION");
            String before = fixture.snapshot();
            fixture.resetTrace();
            assertThat(fixture.play(inconsistent.claim)).as(defect).isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
            assertThat(fixture.resolutions).hasValue(1);
            assertThat(fixture.opens).hasValue(0);
            assertThat(fixture.snapshot()).isEqualTo(before);
            fixture.missingDuration = false;
        }
        fixture.resetTrace();
        var missing = copy(scenario.claim, scenario.grant, scenario.claim.shareAdmissionId(), UUID.randomUUID(), scenario.claim.expiresAt());
        var service = fixture.service(claim -> new PlaybackStateReader.Result.Allowed(claim.issuedAt()), fixture.evidence, false);
        fixture.claim = missing;
        assertThat(service.authorizeAndOpen(new OpaquePlaybackCredential(CANARY), Purpose.PLAYBACK))
                .isEqualTo(new MediaPlaybackService.Result.Denied(MediaPlaybackService.Reason.EVIDENCE_MISSING));
        assertThat(fixture.opens).hasValue(0);
    }

    @Test void readFailuresPreserveAdmissionRevisionCountsAndAccessEvents() throws Exception {
        var scenario = fixture.scenario("NONE", "VALIDATED", false);
        String before = fixture.snapshot();
        assertThat(fixture.play(scenario.claim)).isInstanceOf(MediaPlaybackService.Result.Opened.class);
        for (String failure : List.of("ADMISSION_LOGIN", "EVIDENCE_LOGIN", "CONNECTION", "READER")) {
            fixture.resetTrace();
            fixture.claim = scenario.claim;
            fixture.failRole = failure.equals("CONNECTION") ? "sharing_admission_role" : null;
            String role = failure.equals("ADMISSION_LOGIN") ? "sharing_admission_role" : "evidence_media_reader_role";
            try {
                if (failure.endsWith("LOGIN")) {
                    fixture.sql("ALTER ROLE " + role + " PASSWORD 'test-only-invalid'");
                    fixture.pool(role).getHikariPoolMXBean().softEvictConnections();
                }
                var service = fixture.service(fixture.state, fixture.evidence, failure.equals("READER"));
                assertThat(service.authorizeAndOpen(new OpaquePlaybackCredential(CANARY), Purpose.PLAYBACK))
                        .isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
                assertThat(fixture.opens).hasValue(failure.equals("READER") ? 1 : 0);
                if (failure.equals("ADMISSION_LOGIN") || failure.equals("CONNECTION")) assertThat(fixture.resolutions).hasValue(0);
            } finally {
                fixture.failRole = null;
                if (failure.endsWith("LOGIN")) {
                    fixture.sql("ALTER ROLE " + role + " PASSWORD '" + fixture.base.password + "'");
                    fixture.pool(role).getHikariPoolMXBean().softEvictConnections();
                }
                fixture.afterOpen = false;
            }
            assertThat(fixture.snapshot()).isEqualTo(before);
            fixture.resetTrace();
            assertThat(fixture.play(scenario.claim)).isInstanceOf(MediaPlaybackService.Result.Opened.class);
            assertThat(fixture.snapshot()).isEqualTo(before);
        }
    }

    @Test void typedOpenHasNoPostOpenDatabaseIoAndNeverDisclosesPrivateFacts() throws Exception {
        var scenario = fixture.scenario("NONE", "VALIDATED", false);
        String before = fixture.snapshot();
        fixture.resetTrace();
        var result = fixture.play(scenario.claim);
        assertThat(result).isInstanceOf(MediaPlaybackService.Result.Opened.class);
        assertThat(fixture.afterOpen).isTrue();
        assertThat(fixture.verifications).hasValue(1);
        assertThat(fixture.opens).hasValue(1);
        var handle = ((MediaPlaybackService.Result.Opened) result).handle();
        int checkouts = fixture.checkouts.get();
        int queries = fixture.reads.size();
        String serialized = JSON.writeValueAsString(Map.of("result", result, "claim", scenario.claim,
                "credential", new OpaquePlaybackCredential(CANARY), "handle", handle, "scope", handle.scope(), "window", scenario.claim.window()));
        String rendered = result.toString() + scenario.claim + handle + handle.scope() + scenario.claim.window();
        assertThat(serialized + rendered + output.getAll()).doesNotContain(CANARY, scenario.evidence.toString(), scenario.media.toString(), "a".repeat(64));
        assertThat(fixture.checkouts).hasValue(checkouts);
        assertThat(fixture.reads).hasSize(queries);
        assertThat(fixture.snapshot()).isEqualTo(before);
        fixture.resetTrace();
        fixture.claim = scenario.claim;
        var failed = fixture.service(fixture.state, fixture.evidence, true).authorizeAndOpen(new OpaquePlaybackCredential(CANARY), Purpose.PLAYBACK);
        assertThat(failed).isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
        assertThat(JSON.writeValueAsString(failed) + failed + output.getAll()).doesNotContain(CANARY);
        fixture.afterOpen = false;
        for (String sql : List.of("SELECT media_object_id FROM evidence.evidence_artifact", "SELECT validated_duration FROM evidence.recording", "SELECT bucket FROM evidence.media_object"))
            fixture.denied("sharing_admission_role", sql);
        for (String sql : List.of("SELECT bucket FROM evidence.media_object", "SELECT object_key FROM evidence.media_object", "SELECT object_version_id FROM evidence.media_object", "SELECT id FROM sharing.evidence_share_grant"))
            fixture.denied("evidence_media_reader_role", sql);
        assertThat(fixture.snapshot()).isEqualTo(before);
    }

    @Test void realAuthorityRoundTripsAllPurposesThroughCurrentState() throws Exception {
        var scenario=fixture.scenario("NONE","VALIDATED",false);
        String before=fixture.snapshot();
        for(Purpose purpose:Purpose.values()) {
            fixture.resetTrace();
            var authority=realAuthority(scenario.claim().issuedAt(),DeterministicShareCommandProviders.mediaPlaybackSigningKeys());
            var issued=issue(authority,scenario,purpose);
            fixture.resetTrace();
            var result=realService(authority).authorizeAndOpen(new OpaquePlaybackCredential(issued.credential()),purpose);
            assertThat(result).isInstanceOf(MediaPlaybackService.Result.Opened.class);
            assertThat(fixture.opens).hasValue(1); assertThat(fixture.resolutions).hasValue(1);
            assertThat(fixture.snapshot()).isEqualTo(before);
        }
    }

    @Test void realAuthorityStaleAndCrossObjectClaimsNeverOpen() throws Exception {
        for(String mutation:List.of("REVOKE","CODE","OBJECT")) {
            var scenario=fixture.scenario("NONE","VALIDATED",false);
            var authority=realAuthority(scenario.claim().issuedAt(),DeterministicShareCommandProviders.mediaPlaybackSigningKeys());
            var issued=mutation.equals("OBJECT") ? issue(authority,scenario,Purpose.PLAYBACK,UUID.randomUUID()):issue(authority,scenario,Purpose.PLAYBACK);
            if(mutation.equals("REVOKE")) fixture.sql("UPDATE sharing.evidence_share_grant SET status='REVOKED',revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",scenario.grant());
            else if(mutation.equals("CODE")) fixture.sql("UPDATE sharing.evidence_share_grant SET revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?",scenario.grant());
            String before=fixture.snapshot(); fixture.resetTrace();
            assertThat(realService(authority).authorizeAndOpen(new OpaquePlaybackCredential(issued.credential()),Purpose.PLAYBACK)).isInstanceOf(MediaPlaybackService.Result.Denied.class);
            assertThat(fixture.opens).hasValue(0); assertThat(fixture.snapshot()).isEqualTo(before);
        }
    }

    @Test void realAuthorityEvidenceMismatchAndUnavailableKeysNeverOpen() throws Exception {
        var scenario=fixture.scenario("NONE","VALIDATED",false);
        var authority=realAuthority(scenario.claim().issuedAt(),DeterministicShareCommandProviders.mediaPlaybackSigningKeys());
        var issued=issue(authority,scenario,Purpose.PLAYBACK);
        fixture.resetTrace(); fixture.missingDuration=true;
        assertThat(realService(authority).authorizeAndOpen(new OpaquePlaybackCredential(issued.credential()),Purpose.PLAYBACK)).isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
        assertThat(fixture.opens).hasValue(0);
        fixture.resetTrace();
        var unavailable=realAuthority(scenario.claim().issuedAt(),() -> new MediaPlaybackSigningKeyProvider.Result.Unavailable());
        assertThat(realService(unavailable).authorizeAndOpen(new OpaquePlaybackCredential(issued.credential()),Purpose.PLAYBACK)).isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
        assertThat(fixture.opens).hasValue(0); assertThat(fixture.resolutions).hasValue(0);
    }

    private MediaPlaybackService realService(MediaCredentialVerifier verifier) {
        return new MediaPlaybackService(verifier,fixture.state,fixture.evidence,scope -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty(); fixture.opens.incrementAndGet();
            return new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope));
        },Clock.systemUTC());
    }

    static MediaPlaybackCredentialAuthority realAuthority(Instant now,MediaPlaybackSigningKeyProvider provider) {
        var beans=new org.springframework.beans.factory.support.DefaultListableBeanFactory(); beans.registerSingleton("keys",provider);
        return new MediaPlaybackCredentialAuthority(beans.getBeanProvider(MediaPlaybackSigningKeyProvider.class),Clock.fixed(now,ZoneOffset.UTC));
    }

    private ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued issue(MediaPlaybackCredentialAuthority authority,Scenario scenario,Purpose purpose) {
        return issue(authority,scenario,purpose,null);
    }

    private ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued issue(MediaPlaybackCredentialAuthority authority,Scenario scenario,Purpose purpose,UUID objectOverride) {
        var found=(EvidenceMediaObjectReader.Result.Found)fixture.evidence.resolve(scenario.evidence());
        var media=found.media(); var witness=scenario.witness();
        var facts=new ShareMediaCredentialProvider.AuthorizedMediaFacts(scenario.evidence(),objectOverride==null ? media.mediaObjectId():objectOverride,media.sha256(),media.sizeBytes(),media.mimeType(),media.durationSeconds(),0,
                Math.multiplyExact((long)media.durationSeconds(),1_000_000_000L));
        String event=switch(purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; };
        Instant bound=List.of(witness.grantExpiresAt(),witness.admissionExpiresAt(),witness.sessionExpiresAt()).stream().min(Instant::compareTo).orElseThrow();
        var result=authority.issueOrReplay(new ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest(scenario.grant(),scenario.claim().shareAdmissionId(),purpose.name(),event,
                "a".repeat(64),scenario.claim().codeGeneration(),scenario.claim().revocationGeneration(),witness.grantExpiresAt(),witness.admissionExpiresAt(),witness.sessionExpiresAt(),bound,
                "b".repeat(64),"event",facts,null));
        assertThat(result).isInstanceOf(ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued.class);
        return (ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued)result;
    }

    private static VerifiedMediaClaim copy(VerifiedMediaClaim claim, UUID grant, UUID admission, UUID evidence, Instant expiry) {
        return new VerifiedMediaClaim(claim.audience(), grant, admission, evidence, claim.mediaObjectId(),
                claim.revocationGeneration(), claim.codeGeneration(), claim.purpose(), claim.issuedAt(), claim.notBefore(),
                expiry, claim.durationSeconds(), claim.window());
    }

    record Scenario(UUID grant, UUID evidence, UUID media, UUID subject, UUID binding, UUID account,
            UUID organization, VerifiedMediaClaim claim, AdmissionWitness witness) { }
    record Read(String role, String sessionRole, String readOnly, String isolation, String sql, int parameters, int columns, Map<Integer, Object> bindings) { }

    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception failure) { throw new AssertionError("Fixture SHA unavailable"); }
    }

    static final class Fixture implements AutoCloseable {
        final SharingQueryFixture base = new SharingQueryFixture();
        final HikariDataSource admissionPool;
        final HikariDataSource evidencePool;
        final List<Read> reads = new ArrayList<>();
        final List<UUID> evidenceKeys = new ArrayList<>();
        final AtomicInteger opens = new AtomicInteger();
        final AtomicInteger resolutions = new AtomicInteger();
        final AtomicInteger verifications = new AtomicInteger();
        final AtomicInteger checkouts = new AtomicInteger();
        final ShareAdmissionRepository repository;
        final TransactionTemplate admissionTransactions;
        final PlaybackStateReader state;
        final EvidenceMediaObjectReader evidence;
        boolean afterOpen;
        boolean missingDuration;
        String failRole;
        Instant databaseNow;
        VerifiedMediaClaim claim;

        Fixture() throws Exception {
            base.start();
            base.reset();
            sql("ALTER ROLE sharing_admission_role PASSWORD '" + base.password + "'");
            sql("ALTER ROLE evidence_media_reader_role PASSWORD '" + base.password + "'");
            admissionPool = poolFor("sharing_admission_role");
            evidencePool = poolFor("evidence_media_reader_role");
            DataSource admissionSource = capture(admissionPool, "sharing_admission_role");
            DataSource evidenceSource = capture(evidencePool, "evidence_media_reader_role");
            var real = new JooqShareAdmissionRepository(DSL.using(new TransactionAwareDataSourceProxy(admissionSource), SQLDialect.POSTGRES, new Settings().withExecuteLogging(false)));
            repository = (ShareAdmissionRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ShareAdmissionRepository.class}, (proxy, method, arguments) -> {
                assertThat(afterOpen).as("repository after open").isFalse();
                try {
                    Object value = method.invoke(real, arguments);
                    if (method.getName().equals("now") && databaseNow != null) return databaseNow;
                    return value;
                } catch (InvocationTargetException failure) { throw failure.getCause(); }
            });
            admissionTransactions = transactions(admissionSource);
            state = new SharingPlaybackStateReader(admissionTransactions, repository, MediaPlaybackIT::digest);
            var evidenceTransactions = transactions(evidenceSource);
            evidenceTransactions.setReadOnly(true);
            var realEvidence = new JooqEvidenceMediaObjectReader(DSL.using(new TransactionAwareDataSourceProxy(evidenceSource), SQLDialect.POSTGRES, new Settings().withExecuteLogging(false)), evidenceTransactions);
            evidence = key -> {
                assertThat(afterOpen).isFalse();
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
                resolutions.incrementAndGet();
                evidenceKeys.add(key);
                var result = realEvidence.resolve(key);
                assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
                if (missingDuration && result instanceof EvidenceMediaObjectReader.Result.Found found) {
                    var media = found.media();
                    return new EvidenceMediaObjectReader.Result.Found(new ResolvedMediaObject(media.mediaObjectId(), media.sha256(), media.sizeBytes(), media.mimeType(), null));
                }
                return result;
            };
        }

        Scenario scenario(String mode, String recordingStatus, boolean wrongRelationship) throws Exception {
            return scenario(mode, recordingStatus, wrongRelationship, "a".repeat(64), 123);
        }

        Scenario scenario(String mode, String recordingStatus, boolean wrongRelationship, String mediaSha256, long mediaSize) throws Exception {
            afterOpen = false;
            UUID account = UUID.randomUUID();
            UUID organization = UUID.randomUUID();
            UUID subject = UUID.randomUUID();
            UUID binding = UUID.randomUUID();
            UUID media = UUID.randomUUID();
            UUID recording = UUID.randomUUID();
            UUID evidenceId = UUID.randomUUID();
            UUID session = UUID.randomUUID();
            UUID grant = UUID.randomUUID();
            String call = UUID.randomUUID().toString();
            String event = UUID.randomUUID().toString();
            String token = UUID.randomUUID().toString();
            sql("INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'fixture')", account);
            sql("INSERT INTO organization_access.organization(id,display_name) VALUES (?,'fixture')", organization);
            sql("INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role) VALUES (?,?,?,'MEMBER')", UUID.randomUUID(), organization, account);
            sql("INSERT INTO subject_number.business_subject(id,source,owner_organization_id) VALUES (?,'IVR',?)", subject, organization);
            sql("INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,verified_at) VALUES (?,?,'MOBILE','v1.fixture.payload',?,'synthetic',clock_timestamp())", binding, subject, digest(binding.toString().getBytes(StandardCharsets.UTF_8)));
            sql("INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,observed_at,cleared_at) VALUES (?,?,'CONFLICT','CLEARED','OPERATIONS',clock_timestamp(),clock_timestamp())", UUID.randomUUID(), binding);
            sql("INSERT INTO ivr.inbound_session(id,call_id,subject_id,trusted_caller,access_number_masked,node_id,protocol_version,prompt_version) VALUES (?,?,?,false,'synthetic','fixture','v1','v1')", session, call, subject);
            sql("INSERT INTO ivr.call_record(id,call_id,session_id,subject_id,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version) VALUES (?,?,?,?,'v1.fixture.payload',?,'synthetic','synthetic','fixture','v1')", UUID.randomUUID(), call, session, subject, digest(call.getBytes(StandardCharsets.UTF_8)));
            media(media, mediaSha256, mediaSize);
            UUID recordingMedia = media;
            if (wrongRelationship) { recordingMedia = UUID.randomUUID(); media(recordingMedia, mediaSha256, mediaSize); }
            sql("INSERT INTO evidence.recording(id,call_id,recording_id,media_object_id,declared_format,declared_size,declared_duration,declared_sha256,validated_duration,validated_sha256,status,upload_expires_at,validated_at) VALUES (?,?,?,?,'wav',?,10,?,10,?,?,clock_timestamp()+interval '1 hour',clock_timestamp())", recording, call, UUID.randomUUID().toString(), recordingMedia, mediaSize, mediaSha256, mediaSha256, recordingStatus);
            try (Connection connection = base.admin()) {
                connection.setAutoCommit(false);
                SharingQueryFixture.execute(connection, """
                        INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                            identity_at_capture,consent_version,snapshot,snapshot_sha256,call_id,session_id,status,solidified_at)
                        VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,
                            jsonb_build_object('subjectId',?::text,'bindingId',?::text,'capturedAt','2026-01-01T00:00:00Z','sourceChannel','IVR'),'v1',
                            jsonb_build_object('capture','{}'::jsonb,'content','{}'::jsonb,'sourceEventDomain','EVIDENCE','sourceEventId',?::text,
                                'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text,'callId',?::text,'sessionId',?::text)),
                            repeat('a',64),?,?,'SOLIDIFIED',clock_timestamp())
                        """, evidenceId, subject, recording, event, subject, binding, event, subject, recording, call, session, call, session);
                SharingQueryFixture.execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED','{}'::jsonb,repeat('a',64))", UUID.randomUUID(), evidenceId, event);
                SharingQueryFixture.execute(connection, "INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,'ORIGINAL')", UUID.randomUUID(), evidenceId, media);
                connection.commit();
            }
            sql("""
                    INSERT INTO sharing.evidence_share_grant(id,share_id,capability_token_hash,evidence_id,created_by_account_id,
                        owner_organization_id,verification_mode,valid_from,expires_at,content_snapshot,content_sha256,code_salt,code_hash,code_generation)
                    VALUES (?,?,?,?,?,?,?,clock_timestamp()-interval '60 seconds',clock_timestamp()+interval '1 day',
                        jsonb_build_object('content',jsonb_build_object('evidenceId',?::text),'verification',jsonb_build_object('mode',?::text),
                            'creation',jsonb_build_object('creatorAccountId',?::text,'evidenceId',?::text,'ownerOrganizationId',?::text,
                                'scopeDigest',repeat('d',64),'creationRequestDigest',repeat('e',64),'creationFingerprint',repeat('f',64))),
                        repeat('a',64),CASE WHEN ?='EXTRACTION_CODE' THEN repeat('S',22) END,
                        CASE WHEN ?='EXTRACTION_CODE' THEN repeat('b',64) END,CASE WHEN ?='EXTRACTION_CODE' THEN 1 ELSE 0 END)
                    """, grant, UUID.randomUUID().toString(), digest(token.getBytes(StandardCharsets.UTF_8)), evidenceId, account, organization, mode,
                    evidenceId, mode, account, evidenceId, organization, mode, mode, mode);
            var admissionService = new DefaultShareAdmissionService(admissionTransactions, repository, MediaPlaybackIT::digest,
                    request -> new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(Duration.ofHours(1), Duration.ofMinutes(30)),
                    request -> new ShareAdmissionCodeVerifier.Verified(), request -> new DesignatedMobileProofVerifier.Verified(),
                    request -> new ShareMediaCredentialProvider.Issued("fixture-credential", request.expiresAt()), request -> new ShareAdmissionTicketProvider.Written());
            ShareAdmissionRequest.Verification verification = switch (mode) {
                case "NONE" -> new ShareAdmissionRequest.Verification.None();
                case "EXTRACTION_CODE" -> new ShareAdmissionRequest.Verification.ExtractionCode("fixture-code");
                case "ORGANIZATION_MEMBER" -> new ShareAdmissionRequest.Verification.OrganizationMember();
                default -> throw new AssertionError("Unknown fixture mode");
            };
            var admitted = admissionService.admit(new ShareAdmissionCapabilityToken(token), new ShareAdmissionRequest("fixture-request", verification),
                    mode.equals("ORGANIZATION_MEMBER") ? Optional.of(new TrustedShareAdmissionCaller(account, organization)) : Optional.empty());
            assertThat(admitted).isInstanceOf(ShareAdmissionService.AdmissionCommitted.class);
            UUID admission = ((ShareAdmissionService.AdmissionCommitted) admitted).view().admissionId();
            var witness = admissionTransactions.execute(status -> repository.findAdmissionWitness(grant, admission).orElseThrow());
            Instant issuedAt = admissionTransactions.execute(status -> repository.now());
            assertThat(witness).isNotNull();
            var verified = new VerifiedMediaClaim("onecall:media-playback:v1", grant, admission, evidenceId, media, 0,
                    mode.equals("EXTRACTION_CODE") ? 1 : 0, Purpose.PLAYBACK, issuedAt, issuedAt, witness.sessionExpiresAt(), 10,
                    new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10)));
            return new Scenario(grant, evidenceId, media, subject, binding, account, organization, verified, witness);
        }

        MediaPlaybackService.Result play(VerifiedMediaClaim verified) {
            claim = verified;
            return service(state, evidence, false).authorizeAndOpen(new OpaquePlaybackCredential(CANARY), Purpose.PLAYBACK);
        }

        VerifiedMediaClaim malformedWitnessClaim(Scenario scenario) throws SQLException {
            UUID admission = UUID.randomUUID();
            try (Connection connection = base.admin()) {
                connection.setAutoCommit(false);
                SharingQueryFixture.execute(connection, "INSERT INTO sharing.share_admission(id,share_grant_id,admission_key,admission_no,verification_mode,verification_result,admitted_at) VALUES (?,?,?,2,'ORGANIZATION_MEMBER','VERIFIED',?::timestamptz)",
                        admission, scenario.grant, "malformed-fixture", scenario.claim.issuedAt().toString());
                SharingQueryFixture.execute(connection, "UPDATE sharing.evidence_share_grant SET admissions_used=admissions_used+1,revision=revision+1 WHERE id=?", scenario.grant);
                SharingQueryFixture.execute(connection, "INSERT INTO sharing.share_access_event(id,share_grant_id,share_admission_id,event_domain,event_id,event_type,result,payload_redacted,payload_sha256) VALUES (?,?,?,'SHARE_ACCESS',?,'ADMISSION_GRANTED','SUCCESS',?::jsonb || '{\"unexpected\":true}'::jsonb,?)",
                        UUID.randomUUID(), scenario.grant, admission, "admission-granted:" + scenario.grant + ":" + admission,
                        JSON.writeValueAsString(scenario.witness.payload()), scenario.witness.payloadSha256());
                connection.commit();
            }
            return copy(scenario.claim, scenario.grant, admission, scenario.evidence, scenario.claim.expiresAt());
        }

        VerifiedMediaClaim ownershipMismatchClaim(Scenario scenario) throws SQLException {
            UUID grant = UUID.randomUUID();
            UUID admission = UUID.randomUUID();
            try (Connection connection = base.admin()) {
                connection.setAutoCommit(false);
                SharingQueryFixture.execute(connection, """
                        INSERT INTO sharing.evidence_share_grant(id,share_id,capability_token_hash,evidence_id,created_by_account_id,
                            owner_organization_id,verification_mode,valid_from,expires_at,content_snapshot,content_sha256)
                        SELECT ?,?,?,evidence_id,created_by_account_id,?,'NONE',valid_from,expires_at,
                            jsonb_set(content_snapshot,'{creation,ownerOrganizationId}',to_jsonb(?::text)),content_sha256
                        FROM sharing.evidence_share_grant WHERE id=?
                        """, grant, UUID.randomUUID().toString(), digest(grant.toString().getBytes(StandardCharsets.UTF_8)), base.org2, base.org2, scenario.grant);
                SharingQueryFixture.execute(connection, "INSERT INTO sharing.share_admission(id,share_grant_id,admission_key,admission_no,verification_mode,verification_result,admitted_at) VALUES (?,?,?,1,'NONE','NOT_REQUIRED',?::timestamptz)",
                        admission, grant, "ownership-fixture", scenario.claim.issuedAt().toString());
                SharingQueryFixture.execute(connection, "UPDATE sharing.evidence_share_grant SET admissions_used=1,revision=revision+1 WHERE id=?", grant);
                SharingQueryFixture.execute(connection, "INSERT INTO sharing.share_access_event(id,share_grant_id,share_admission_id,event_domain,event_id,event_type,result,payload_redacted,payload_sha256) VALUES (?,?,?,'SHARE_ACCESS',?,'ADMISSION_GRANTED','SUCCESS',?::jsonb,?)",
                        UUID.randomUUID(), grant, admission, "admission-granted:" + grant + ":" + admission,
                        JSON.writeValueAsString(scenario.witness.payload()), scenario.witness.payloadSha256());
                connection.commit();
            }
            return copy(scenario.claim, grant, admission, scenario.evidence, scenario.claim.expiresAt());
        }

        MediaPlaybackService service(PlaybackStateReader stateReader, EvidenceMediaObjectReader evidenceReader, boolean failOpen) {
            return new MediaPlaybackService(credential -> {
                assertThat(afterOpen).isFalse();
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                verifications.incrementAndGet();
                assertThat(credential.value()).isEqualTo(CANARY);
                return new MediaCredentialVerifier.Result.Verified(claim);
            }, stateReader, evidenceReader, scope -> {
                assertThat(afterOpen).isFalse();
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
                opens.incrementAndGet();
                afterOpen = true;
                if (failOpen) throw new IllegalStateException(CANARY);
                return new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope));
            }, new Clock() {
                @Override public ZoneId getZone() { return ZoneOffset.UTC; }
                @Override public Clock withZone(ZoneId zone) { return this; }
                @Override public Instant instant() {
                    assertThat(afterOpen).as("clock after open").isFalse();
                    return claim == null ? Instant.now() : claim.issuedAt();
                }
            });
        }

        HikariDataSource poolFor(String role) {
            var source = new HikariDataSource();
            source.setJdbcUrl(base.url);
            source.setUsername(role);
            source.setPassword(base.password);
            source.setPoolName("pa19-" + role);
            source.setMaximumPoolSize(1);
            source.setMinimumIdle(0);
            source.setConnectionTimeout(1000);
            source.setInitializationFailTimeout(-1);
            return source;
        }

        HikariDataSource pool(String role) { return role.equals("sharing_admission_role") ? admissionPool : evidencePool; }

        DataSource capture(DataSource source, String role) {
            return (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{DataSource.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("getConnection")) {
                    assertThat(afterOpen).as("checkout after open").isFalse();
                    checkouts.incrementAndGet();
                    if (role.equals(failRole)) throw new SQLException(CANARY, "08006");
                }
                try {
                    Object value = method.invoke(source, arguments);
                    if (!(value instanceof Connection connection)) return value;
                    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (connectionProxy, operation, values) -> {
                        if (!operation.getName().equals("close")) assertThat(afterOpen).as("connection after open").isFalse();
                        try {
                            Object result = operation.invoke(connection, values);
                            if (operation.getName().equals("prepareStatement") && result instanceof PreparedStatement statement) {
                                String sql = (String) values[0];
                                Map<Integer, Object> bindings = new TreeMap<>();
                                return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, (statementProxy, action, parameters) -> {
                                    assertThat(afterOpen).as("SQL after open").isFalse();
                                    if (action.getName().startsWith("set") && parameters != null && parameters.length >= 2 && parameters[0] instanceof Integer position)
                                        bindings.put(position, parameters[1]);
                                    try {
                                        Object executed = action.invoke(statement, parameters);
                                        if (action.getName().startsWith("execute")) {
                                            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                                            try (var status = connection.createStatement(); var rows = status.executeQuery("SELECT current_user,session_user,current_setting('transaction_read_only'),current_setting('transaction_isolation')")) {
                                                assertThat(rows.next()).isTrue();
                                                var metadata = statement.getMetaData();
                                                int columns = metadata == null ? 0 : metadata.getColumnCount();
                                                reads.add(new Read(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4), sql, statement.getParameterMetaData().getParameterCount(), columns, Collections.unmodifiableMap(new TreeMap<>(bindings))));
                                            }
                                        }
                                        return executed;
                                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                                });
                            }
                            return result;
                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
                } catch (InvocationTargetException failure) { throw failure.getCause(); }
            });
        }

        static TransactionTemplate transactions(DataSource source) {
            var manager = new DataSourceTransactionManager(source);
            manager.setEnforceReadOnly(true);
            var template = new TransactionTemplate(manager);
            template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return template;
        }

        void resetTrace() {
            afterOpen = false;
            reads.clear(); evidenceKeys.clear();
            opens.set(0); resolutions.set(0); verifications.set(0); checkouts.set(0);
        }

        void noEvidenceOrOpen() { assertThat(resolutions).hasValue(0); assertThat(opens).hasValue(0); }
        void sql(String sql, Object... values) throws SQLException {
            try (Connection connection = base.admin()) { SharingQueryFixture.execute(connection, sql, values); }
        }
        String text(String sql) throws SQLException {
            try (Connection connection = base.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
                assertThat(rows.next()).isTrue(); return rows.getString(1);
            }
        }
        long scalar(String sql, Object... values) throws SQLException {
            try (Connection connection = base.admin(); var statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
                try (var rows = statement.executeQuery()) { assertThat(rows.next()).isTrue(); return rows.getLong(1); }
            }
        }
        String frozen() throws SQLException {
            return text("SELECT jsonb_build_object('admissions',(SELECT jsonb_agg(to_jsonb(a) ORDER BY id) FROM sharing.share_admission a),'events',(SELECT jsonb_agg(to_jsonb(e) ORDER BY id) FROM sharing.share_access_event e))::text");
        }
        String snapshot() throws SQLException {
            return frozen() + text("SELECT coalesce(jsonb_agg(to_jsonb(g) ORDER BY id),'[]')::text FROM sharing.evidence_share_grant g");
        }
        void media(UUID id) throws SQLException {
            media(id, "a".repeat(64), 123);
        }
        void media(UUID id, String mediaSha256, long mediaSize) throws SQLException {
            sql("INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES (?,'COS','fixture',?,'v1',?,?,'audio/wav','fixture-key')", id, CANARY + id, mediaSha256, mediaSize);
        }
        Map<String, String> triggers() throws SQLException {
            Map<String, String> definitions = new TreeMap<>();
            try (Connection connection = base.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT tgname,pg_get_triggerdef(oid),tgenabled FROM pg_trigger WHERE tgrelid IN ('evidence.evidence_artifact'::regclass,'evidence.media_object'::regclass) AND tgname IN ('trg_evidence_artifact_immutable','trg_media_object_immutable')")) {
                while (rows.next()) {
                    assertThat(rows.getString(3)).isEqualTo("O");
                    assertThat(rows.getString(2)).contains("BEFORE DELETE OR UPDATE", "FOR EACH ROW EXECUTE FUNCTION evidence.reject_row_mutation()");
                    definitions.put(rows.getString(1), rows.getString(2));
                }
            }
            return definitions;
        }
        void denied(String role, String sql) throws SQLException {
            try (Connection connection = pool(role).getConnection(); var statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeQuery(sql)).isInstanceOfSatisfying(SQLException.class,
                        failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            }
        }
        @Override public void close() throws Exception {
            afterOpen = false;
            try { admissionPool.close(); }
            finally {
                try { evidencePool.close(); }
                finally {
                    try {
                        sql("ALTER ROLE sharing_admission_role PASSWORD NULL");
                        sql("ALTER ROLE evidence_media_reader_role PASSWORD NULL");
                    } finally { base.close(); }
                }
            }
        }
    }
}
