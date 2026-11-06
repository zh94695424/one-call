package com.onecall.archtests.sharing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.shareprojection.*;
import com.onecall.core.sharing.internal.persistence.PublicShareQueryRepository;
import com.onecall.core.sharing.internal.persistence.PublicShareQueryRepository.PublicGrant;
import com.onecall.core.sharing.internal.provider.Sha256Provider;
import com.onecall.core.sharing.internal.publicquery.*;
import com.onecall.core.sharing.internal.publicquery.PublicShareView.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class PublicShareQueryServiceTests {
    private static final Instant NOW = Instant.parse("2026-11-06T00:00:00Z");
    private static final UUID EVIDENCE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SUBJECT = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final String SOURCE = "50000000-0000-0000-0000-000000000001";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void exactUtf8DigestLookupConcealsUnknownTokens() {
        Fixture fixture = new Fixture(VerificationMode.NONE);
        when(fixture.repository.findByTokenDigest(anyString())).thenReturn(Optional.empty());
        for (String token : List.of("a", "a".repeat(512), "A", " a ", "é", "e\u0301", "😀".repeat(512))) {
            assertThat(new OpaqueCapabilityToken(token)).hasToString("[REDACTED]");
            denied(() -> fixture.service.getPublicShare(new OpaqueCapabilityToken(token), fixture.member), PublicShareQueryException.Code.SHARE_TOKEN_INVALID);
            verify(fixture.sha).digest(argThat(bytes -> java.util.Arrays.equals(bytes, token.getBytes(StandardCharsets.UTF_8))));
            verify(fixture.repository).findByTokenDigest(DeterministicShareCommandProviders.digest(token));
        }
        verifyNoInteractions(fixture.reader, fixture.member);
        verify(fixture.repository, never()).sampleDatabaseTime();
        for (String token : new String[] {null, "", "a".repeat(513), "\0", "\ud800", "\udc00", "😀".repeat(513)}) {
            denied(() -> new OpaqueCapabilityToken(token), PublicShareQueryException.Code.VALIDATION_FAILED);
        }
        for (String digest : new String[] {null, "", "A".repeat(64), "a".repeat(63)}) {
            doReturn(digest).when(fixture.sha).digest(any());
            denied(() -> fixture.read(), PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        }
        doThrow(new IllegalStateException("SECRET_CANARY")).when(fixture.sha).digest(any());
        denied(() -> fixture.read(), PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        doThrow(new PublicShareQueryException(PublicShareQueryException.Code.FORBIDDEN)).when(fixture.sha).digest(any());
        denied(() -> fixture.read(), PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        var missing = new PublicShareQueryService(fixture.transaction, fixture.dsl, fixture.repository, fixture.reader, null);
        denied(() -> missing.getPublicShare(new OpaqueCapabilityToken("a"), fixture.member), PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
    }

    @Test
    void stateAndDatabaseTimeChecksNeverMutateGrant() {
        for (Status status : Status.values()) {
            for (Instant time : List.of(NOW.minusSeconds(20), NOW, NOW.plusSeconds(10), NOW.plusSeconds(20))) {
                Fixture fixture = new Fixture(VerificationMode.NONE);
                fixture.grant(status, NOW, NOW.plusSeconds(10), fixture.snapshot.replace(NOW.plusSeconds(60).toString(), NOW.plusSeconds(10).toString()));
                when(fixture.repository.sampleDatabaseTime()).thenReturn(time);
                if (status == Status.REVOKED) denied(fixture::read, PublicShareQueryException.Code.SHARE_REVOKED);
                else if (status == Status.EXPIRED || !time.isBefore(NOW.plusSeconds(10))) denied(fixture::read, PublicShareQueryException.Code.SHARE_EXPIRED);
                else if (time.isBefore(NOW)) denied(fixture::read, PublicShareQueryException.Code.SHARE_TOKEN_INVALID);
                else assertThat(fixture.read().status()).isEqualTo(status);
                verify(fixture.repository, times(1)).sampleDatabaseTime();
                verify(fixture.repository, times(1)).findByTokenDigest(anyString());
                verifyNoMoreInteractions(fixture.repository);
                verifyNoInteractions(fixture.member);
                if (status == Status.REVOKED || status == Status.EXPIRED || !time.equals(NOW)) verifyNoInteractions(fixture.reader);
            }
        }
    }

    @Test
    void immutableEvidenceProjectionIsOrderedAndFailClosed() {
        Fixture fixture = new Fixture(VerificationMode.NONE);
        var content = fixture.read().content();
        assertThat(content.capturedAt()).isEqualTo(NOW.minusSeconds(100));
        assertThat(content.validatedDurationSeconds()).isEqualTo(12);
        assertThat(content.artifacts()).extracting(Artifact::artifactRole).containsExactly(ArtifactRole.ORIGINAL, ArtifactRole.DERIVED);
        assertThat(fixture.grant).hasToString("[REDACTED]");
        var order = inOrder(fixture.dsl, fixture.repository, fixture.reader);
        order.verify(fixture.dsl).execute("SET TRANSACTION READ ONLY");
        order.verify(fixture.dsl).fetchOne("SELECT current_setting('transaction_read_only')");
        order.verify(fixture.dsl).execute("SET LOCAL ROLE sharing_command_role");
        order.verify(fixture.dsl).fetchOne("SELECT current_user");
        order.verify(fixture.repository).findByTokenDigest(anyString());
        order.verify(fixture.repository).sampleDatabaseTime();
        order.verify(fixture.dsl).execute("SET LOCAL ROLE evidence_role");
        order.verify(fixture.dsl).fetchOne("SELECT current_user");
        order.verify(fixture.dsl).fetchOne("SELECT current_setting('transaction_read_only')");
        order.verify(fixture.reader).readShareableProjection(fixture.dsl, EVIDENCE);
        order.verify(fixture.dsl).execute("SET LOCAL ROLE sharing_command_role");
        for (var result : List.of(new ShareableEvidenceProjectionReader.ReadResult.Missing(),
                new ShareableEvidenceProjectionReader.ReadResult.Unavailable(),
                new ShareableEvidenceProjectionReader.ReadResult.Found(fixture.projection("QUARANTINED")))) {
            when(fixture.reader.readShareableProjection(any(), any())).thenReturn(result);
            denied(fixture::read, result instanceof ShareableEvidenceProjectionReader.ReadResult.Unavailable
                    ? PublicShareQueryException.Code.SERVICE_UNAVAILABLE : PublicShareQueryException.Code.FORBIDDEN);
        }
        when(fixture.reader.readShareableProjection(any(), any())).thenReturn(null);
        denied(fixture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        when(fixture.reader.readShareableProjection(any(), any())).thenThrow(new IllegalStateException("SQL_CANARY"));
        denied(fixture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        fixture = new Fixture(VerificationMode.NONE);
        for (String bad : List.of("{}", "null", "{", fixture.snapshot.replace("capturedAt", "missing"),
                fixture.snapshot.replace("audio/wav", "audio/mp3"), fixture.snapshot.replace(SUBJECT.toString(), UUID.randomUUID().toString()))) {
            fixture.grant(Status.ACTIVE, NOW.minusSeconds(1), NOW.plusSeconds(60), bad);
            denied(fixture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        }
        Fixture malformedCapture = new Fixture(VerificationMode.NONE);
        String malformedIdentity = malformedCapture.identity.replace("2026-11-05T23:58:20Z", "2026-11-05T23:58:20");
        String malformedSnapshot = malformedCapture.snapshot.replace(malformedCapture.identity, malformedIdentity);
        var projection = malformedCapture.projection("SOLIDIFIED");
        var malformedProjection = new ShareableEvidenceProjection(projection.evidenceId(), projection.subjectId(), projection.status(),
                projection.sourceType(), projection.sourceRefId(), JSONB.valueOf(malformedIdentity), projection.artifacts());
        assertThat(MAPPER.readTree(malformedProjection.identityAtCapture().data()))
                .isEqualTo(MAPPER.readTree(malformedSnapshot).path("content").path("identityAtCapture"));
        malformedCapture.grant(Status.ACTIVE, NOW.minusSeconds(1), NOW.plusSeconds(60), malformedSnapshot);
        when(malformedCapture.reader.readShareableProjection(any(), any()))
                .thenReturn(new ShareableEvidenceProjectionReader.ReadResult.Found(malformedProjection));
        denied(malformedCapture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        verify(malformedCapture.reader).readShareableProjection(malformedCapture.dsl, EVIDENCE);
        assertThat(malformedCapture.role).isEqualTo("sharing_command_role");
        for (var malformedArtifacts : List.of(
                List.of(new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), 20, "audio/mp3", 12)),
                List.of(new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), 20, "audio/wav", -1)),
                List.of(new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), -1, "audio/wav", 12)),
                List.of(new ShareableEvidenceProjection.ArtifactProjection("DERIVED", "b".repeat(64), 10, "audio/wav", 99)),
                List.of(new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), 20, "audio/wav", 12),
                        new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "b".repeat(64), 10, "audio/wav", 99)))) {
            Fixture malformed = new Fixture(VerificationMode.NONE);
            malformed.artifacts = malformedArtifacts;
            denied(malformed::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        }
        for (String state : List.of("PENDING", "FAILED", "QUARANTINED", "UNKNOWN")) {
            Fixture evidence = new Fixture(VerificationMode.NONE);
            when(evidence.reader.readShareableProjection(any(), any())).thenReturn(new ShareableEvidenceProjectionReader.ReadResult.Found(evidence.projection(state)));
            denied(evidence::read, state.equals("UNKNOWN") ? PublicShareQueryException.Code.SERVICE_UNAVAILABLE : PublicShareQueryException.Code.FORBIDDEN);
        }
        Fixture absentDuration = new Fixture(VerificationMode.NONE);
        absentDuration.artifacts = List.of(new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), 20, "audio/wav", null));
        absentDuration.snapshot = absentDuration.snapshot(VerificationMode.NONE);
        absentDuration.grant(Status.ACTIVE, NOW.minusSeconds(1), NOW.plusSeconds(60), absentDuration.snapshot);
        when(absentDuration.reader.readShareableProjection(any(), any())).thenAnswer(invocation -> new ShareableEvidenceProjectionReader.ReadResult.Found(absentDuration.projection("SOLIDIFIED")));
        assertThat(absentDuration.read().content().validatedDurationSeconds()).isNull();
        var restores = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            if (restores.incrementAndGet() > 1) throw new IllegalStateException("RESTORE_CANARY");
            absentDuration.role = "sharing_command_role";
            return 0;
        }).when(absentDuration.dsl).execute("SET LOCAL ROLE sharing_command_role");
        denied(absentDuration::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
    }

    @Test
    void organizationContentRequiresCurrentMatchingMember() {
        for (var outcome : PublicShareQueryService.MemberMatch.values()) {
            Fixture fixture = new Fixture(VerificationMode.ORGANIZATION_MEMBER);
            when(fixture.member.check(ORGANIZATION)).thenReturn(outcome);
            switch (outcome) {
                case MATCH -> assertThat(fixture.read().content()).isNotNull();
                case NONMATCH -> {
                    var view = fixture.read();
                    assertThat(view.content()).isNull();
                    assertThat(view.verificationChallenge().customerSessionRequired()).isTrue();
                    verifyNoInteractions(fixture.reader);
                }
                case UNAUTHENTICATED -> denied(fixture::read, PublicShareQueryException.Code.UNAUTHENTICATED);
                case UNAVAILABLE -> denied(fixture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
                case INTERNAL -> denied(fixture::read, PublicShareQueryException.Code.INTERNAL);
            }
            verify(fixture.member).check(ORGANIZATION);
        }
        Fixture fixture = new Fixture(VerificationMode.ORGANIZATION_MEMBER);
        when(fixture.member.check(any())).thenReturn(null);
        denied(fixture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        when(fixture.member.check(any())).thenThrow(new IllegalStateException("MEMBER_CANARY"));
        denied(fixture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        denied(() -> fixture.service.getPublicShare(new OpaqueCapabilityToken("token"), null), PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        fixture.grant = new PublicGrant(EVIDENCE, Optional.empty(), VerificationMode.ORGANIZATION_MEMBER, Status.ACTIVE,
                NOW.minusSeconds(1), NOW.plusSeconds(60), false, fixture.snapshot);
        when(fixture.repository.findByTokenDigest(anyString())).thenReturn(Optional.of(fixture.grant));
        denied(fixture::read, PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        verifyNoInteractions(fixture.reader);
    }

    private static void denied(Runnable action, PublicShareQueryException.Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(PublicShareQueryException.class, failure -> {
            assertThat(failure.code()).isEqualTo(code);
            assertThat(failure.getMessage()).isEqualTo(code.name());
            assertThat(failure.getCause()).isNull();
            assertThat(failure.getStackTrace()).isEmpty();
        });
    }

    static final class Fixture {
        final TransactionTemplate transaction = mock(TransactionTemplate.class);
        final DSLContext dsl = mock(DSLContext.class);
        final PublicShareQueryRepository repository = mock(PublicShareQueryRepository.class);
        final ShareableEvidenceProjectionReader reader = mock(ShareableEvidenceProjectionReader.class);
        final Sha256Provider sha = mock(Sha256Provider.class);
        final PublicShareQueryService.MemberAccess member = mock(PublicShareQueryService.MemberAccess.class);
        final PublicShareQueryService service = new PublicShareQueryService(transaction, dsl, repository, reader, sha);
        List<ShareableEvidenceProjection.ArtifactProjection> artifacts = List.of(
                new ShareableEvidenceProjection.ArtifactProjection("DERIVED", "b".repeat(64), 10, "audio/wav", 99),
                new ShareableEvidenceProjection.ArtifactProjection("ORIGINAL", "a".repeat(64), 20, "audio/wav", 12));
        final String identity = "{\"capturedAt\":\"2026-11-05T23:58:20Z\"}";
        PublicGrant grant;
        String snapshot;
        String role = "sharing_command_role";

        Fixture(VerificationMode mode) {
            when(transaction.execute(any())).thenAnswer(invocation -> invocation.<TransactionCallback<Object>>getArgument(0).doInTransaction(new SimpleTransactionStatus()));
            when(sha.digest(any())).thenAnswer(invocation -> DeterministicShareCommandProviders.digest(new String(invocation.<byte[]>getArgument(0), StandardCharsets.UTF_8)));
            when(dsl.execute(anyString())).thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);
                if (sql.startsWith("SET LOCAL ROLE ")) role = sql.substring("SET LOCAL ROLE ".length());
                return 0;
            });
            when(dsl.fetchOne(anyString())).thenAnswer(invocation -> {
                Record record = mock(Record.class);
                when(record.get(0, String.class)).thenReturn(invocation.<String>getArgument(0).contains("current_user") ? role : "on");
                return record;
            });
            when(repository.sampleDatabaseTime()).thenReturn(NOW);
            snapshot = snapshot(mode);
            grant(Status.ACTIVE, NOW.minusSeconds(1), NOW.plusSeconds(60), snapshot);
            when(reader.readShareableProjection(any(), any())).thenAnswer(invocation -> new ShareableEvidenceProjectionReader.ReadResult.Found(projection("SOLIDIFIED")));
            when(member.check(any())).thenReturn(PublicShareQueryService.MemberMatch.MATCH);
        }

        PublicShareView read() { return service.getPublicShare(new OpaqueCapabilityToken("token"), member); }
        void grant(Status status, Instant from, Instant expiry, String content) {
            grant = new PublicGrant(EVIDENCE, Optional.of(ORGANIZATION), grant == null ? VerificationMode.valueOf(MAPPER.readTree(content).path("verification").path("mode").asString()) : grant.verificationMode(),
                    status, from, expiry, false, content);
            when(repository.findByTokenDigest(anyString())).thenReturn(Optional.of(grant));
        }
        ShareableEvidenceProjection projection(String status) { return new ShareableEvidenceProjection(EVIDENCE, SUBJECT, status, "CALL", SOURCE, JSONB.valueOf(identity), artifacts); }
        String snapshot(VerificationMode mode) {
            var root = MAPPER.createObjectNode();
            var content = root.putObject("content").put("schemaVersion", 1).put("evidenceId", EVIDENCE.toString())
                    .put("subjectId", SUBJECT.toString()).put("sourceType", "CALL").put("sourceRefId", SOURCE);
            content.set("identityAtCapture", MAPPER.readTree(identity));
            var array = content.putArray("artifacts");
            artifacts.stream().sorted(java.util.Comparator.comparing(artifact -> artifact.artifactRole().equals("ORIGINAL") ? 0 : 1)).forEach(artifact -> {
                var item = array.addObject().put("artifactRole", artifact.artifactRole()).put("mediaSha256", artifact.mediaSha256())
                        .put("mediaSizeBytes", artifact.mediaSizeBytes()).put("mediaMimeType", artifact.mediaMimeType());
                if (artifact.validatedDurationSeconds() == null) item.putNull("validatedDurationSeconds");
                else item.put("validatedDurationSeconds", artifact.validatedDurationSeconds());
            });
            var verification = root.putObject("verification").put("mode", mode.name());
            if (mode == VerificationMode.DESIGNATED_MOBILE) verification.put("mobileHmac", "a".repeat(64)).put("keyVersion", "test-key");
            root.putObject("creation").put("schemaVersion", 1).put("evidenceId", EVIDENCE.toString()).put("ownerOrganizationId", ORGANIZATION.toString())
                    .put("verificationMode", mode.name()).put("downloadAllowed", false).put("expiresAt", NOW.plusSeconds(60).toString())
                    .put("creatorAccountId", "10000000-0000-0000-0000-000000000001").putNull("maxAdmissions")
                    .put("scopeDigest", "a".repeat(64)).put("creationRequestDigest", "b".repeat(64)).put("creationFingerprint", "c".repeat(64));
            return root.toString();
        }
    }
}
