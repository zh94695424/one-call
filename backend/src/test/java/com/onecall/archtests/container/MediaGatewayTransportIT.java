package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static com.onecall.archtests.support.MediaGatewayTestProviders.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.evidence.mediaread.ControlledMediaByteSource;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider;
import com.onecall.core.sharing.internal.credential.*;
import com.onecall.archtests.sharing.DeterministicShareCommandProviders;
import com.onecall.core.sharing.internal.gateway.*;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(OutputCaptureExtension.class)
class MediaGatewayTransportIT {
    private CapturedOutput capturedOutput;
    @BeforeEach void captureOutput(CapturedOutput output) { capturedOutput = output; }
    @Test void realHttpFullAndRangeUseQualifiedAuthorizationThenExactBytes() throws Exception {
        try (var transport = new Transport(PAYLOAD)) {
            assertThat(POSITIVE_HTTP_BEARER).matches("[A-Za-z0-9._~+/-]+={0,}");
            for (String range : Arrays.asList(null, "bytes=2-5", "bytes=14-99", "bytes=12-", "bytes=-4", "bytes=-99")) {
                transport.prepare(range == null ? Purpose.PLAYBACK : Purpose.SEEK);
                String before = transport.fixture.snapshot();
                var response = transport.get(range, POSITIVE_HTTP_BEARER, "");
                assertThat(response.statusCode()).isEqualTo(range == null ? 200 : 206);
                var selected = (SingleByteRange.Resolution.Selected) SingleByteRange.resolve(
                        ((SingleByteRange.ParseResult.Accepted) SingleByteRange.parse(range == null ? List.of() : List.of(range))).spec(), PAYLOAD.length);
                assertThat(response.body()).isEqualTo(Arrays.copyOfRange(PAYLOAD, (int) selected.interval().startInclusive(), (int) selected.interval().endExclusive()));
                assertThat(response.headers().firstValue("Content-Length")).contains(Long.toString(selected.interval().length()));
                assertThat(response.headers().firstValue("Content-Type")).contains("audio/wav");
                assertThat(response.headers().firstValue("Accept-Ranges")).contains("bytes");
                assertThat(transport.fixture.verifications).hasValue(1);
                assertThat(transport.fixture.opens).hasValue(1); assertThat(transport.fixture.resolutions).hasValue(1);
                assertThat(transport.fixture.evidenceKeys).containsExactly(transport.scenario.evidence());
                assertThat(transport.fixture.reads.stream().filter(read -> read.role().equals("sharing_admission_role")))
                        .isNotEmpty().allSatisfy(read -> { assertThat(read.readOnly()).isEqualTo("on"); assertThat(read.isolation()).isEqualTo("repeatable read"); });
                assertThat(transport.fixture.reads.stream().filter(read -> read.role().equals("evidence_media_reader_role")))
                        .hasSize(1).allSatisfy(read -> { assertThat(read.readOnly()).isEqualTo("on"); assertThat(read.isolation()).isEqualTo("read committed"); assertThat(read.sessionRole()).isEqualTo(read.role()); });
                transport.counts(0); assertThat(transport.fixture.snapshot()).isEqualTo(before);
            }
            System.out.println("PA20 PostgreSQL identity: " + transport.fixture.text("SELECT version()")
                    + "; image=" + transport.fixture.base.postgres.getDockerImageName()
                    + "; imageId=" + transport.fixture.base.postgres.getContainerInfo().getImageId());
        }
    }

    @Test void realHttpMalformedUnsatisfiableAndDeniedRequestsNeverReadBytes() throws Exception {
        try (var transport = new Transport(PAYLOAD)) {
            for (String kind : List.of("malformed", "privateBearer", "invalid", "expired", "purpose", "object", "admission", "unsatisfiable", "unavailable", "partial", "partialUnsatisfiable", "partialMalformed", "partialQuery", "partialBody", "partialBearer")) {
                transport.prepare(Purpose.SEEK);
                String range = "bytes=2-5"; String bearer = POSITIVE_HTTP_BEARER; String query = ""; int expected = 403;
                switch (kind) {
                    case "malformed", "partialMalformed" -> { range = "bytes=5-2"; expected = 400; }
                    case "privateBearer", "partialBearer" -> { bearer = PRIVATE_CANARY; expected = 401; }
                    case "invalid" -> transport.invalid = true;
                    case "expired" -> {
                        var claim = transport.claim;
                        transport.claim = copy(claim, claim.audience(), claim.shareGrantId(), claim.shareAdmissionId(), claim.evidenceId(), claim.mediaObjectId(), claim.purpose(),
                                claim.issuedAt().minusSeconds(60), claim.notBefore().minusSeconds(60), claim.issuedAt().minusSeconds(1), claim.window());
                    }
                    case "purpose" -> transport.claim = purpose(transport.claim, Purpose.RENEW);
                    case "object" -> transport.claim = transport.changed(UUID.randomUUID(), transport.claim.shareAdmissionId(), transport.claim.expiresAt(), transport.claim.window());
                    case "admission" -> transport.claim = transport.changed(transport.claim.mediaObjectId(), UUID.randomUUID(), transport.claim.expiresAt(), transport.claim.window());
                    case "unsatisfiable" -> { range = "bytes=99-"; expected = 416; }
                    case "unavailable" -> { transport.unavailable = true; expected = 503; }
                    case "partialUnsatisfiable" -> range = "bytes=99-";
                    case "partialQuery" -> { query = "?probe"; expected = 400; }
                    case "partialBody" -> expected = 400;
                }
                if (kind.startsWith("partial")) transport.claim = transport.changed(transport.claim.mediaObjectId(), transport.claim.shareAdmissionId(), transport.claim.expiresAt(),
                        new VerifiedMediaClaim.PlaybackWindow(Duration.ofSeconds(1), Duration.ofSeconds(8)));
                String before = transport.fixture.snapshot();
                HttpResponse<byte[]> response;
                if (kind.equals("partialBody")) {
                    synchronized (transport.fixture) {
                        response = transport.client.send(transport.http(range, bearer, query).method("GET", HttpRequest.BodyPublishers.ofString("probe")).build(), HttpResponse.BodyHandlers.ofByteArray());
                    }
                } else response = transport.get(range, bearer, query);
                assertThat(response.statusCode()).as(kind).isEqualTo(expected);
                assertThat(transport.source.opens).hasValue(0); assertThat(transport.source.reads).hasValue(0);
                if (expected == 400 || expected == 401) assertThat(transport.fixture.verifications).hasValue(0);
                if (kind.equals("partial") || kind.equals("partialUnsatisfiable")) assertThat(transport.fixture.opens).hasValue(1);
                if (expected != 416) {
                    assertThat(response.headers().firstValue("Content-Range")).isEmpty();
                    assertThat(response.headers().firstValue("Accept-Ranges")).isEmpty();
                } else assertThat(response.headers().firstValue("Content-Range")).contains("bytes */16");
                transport.redacted(response); assertThat(transport.fixture.snapshot()).isEqualTo(before);
            }
        }
    }

    @Test void committedRevocationRotationAndRiskDenyNextByteRequest() throws Exception {
        try (var transport = new Transport(PAYLOAD)) {
            for (String mutation : List.of("LOCKED", "REVOKE", "CODE", "RISK", "BINDING", "MEMBER")) {
                transport.newScenario(mutation.equals("MEMBER") ? "ORGANIZATION_MEMBER" : "EXTRACTION_CODE");
                var scenario = transport.scenario;
                switch (mutation) {
                    case "LOCKED" -> transport.fixture.sql("UPDATE sharing.evidence_share_grant SET status='LOCKED',revision=revision+1 WHERE id=?", scenario.grant());
                    case "REVOKE" -> transport.fixture.sql("UPDATE sharing.evidence_share_grant SET status='REVOKED',revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?", scenario.grant());
                    case "CODE" -> transport.fixture.sql("UPDATE sharing.evidence_share_grant SET code_salt=repeat('T',22),code_hash=repeat('c',64),code_generation=code_generation+1,revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=?", scenario.grant());
                    case "RISK" -> transport.fixture.sql("INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,observed_at) VALUES (?,?,'RECYCLED','OPEN','OPERATIONS',clock_timestamp())", UUID.randomUUID(), scenario.binding());
                    case "BINDING" -> transport.fixture.sql("UPDATE subject_number.caller_number_binding SET status='RETIRED',revision=revision+1 WHERE id=?", scenario.binding());
                    case "MEMBER" -> transport.fixture.sql("UPDATE organization_access.organization_membership SET status='SUSPENDED',revision=revision+1 WHERE organization_id=? AND account_id=?", scenario.organization(), scenario.account());
                }
                transport.prepare(Purpose.PLAYBACK); String before = transport.fixture.snapshot();
                var response = transport.get(null, POSITIVE_HTTP_BEARER, "");
                boolean allowed = mutation.equals("LOCKED");
                assertThat(response.statusCode()).as(mutation).isEqualTo(allowed ? 200 : 403);
                assertThat(transport.source.opens).hasValue(allowed ? 1 : 0);
                assertThat(transport.fixture.snapshot()).isEqualTo(before);
            }
        }
    }

    @Test void byteOpenReadAbortAndCloseHoldNoDatabaseTransactionOrLock() throws Exception {
        try (var transport = new Transport(PAYLOAD); var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            transport.prepare(Purpose.PLAYBACK);
            int admissionPid = pid(transport.fixture.admissionPool); int evidencePid = pid(transport.fixture.evidencePool);
            var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
            var blocked = new java.util.concurrent.atomic.AtomicBoolean();
            Runnable guards = transport.source.callback;
            transport.source.callback = () -> {
                guards.run();
                if (transport.source.operation.equals("read") && blocked.compareAndSet(false, true)) {
                    entered.countDown();
                    try { if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Transfer release timeout"); }
                    catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError("Interrupted transfer"); }
                }
            };
            Future<HttpResponse<byte[]>> pending = executor.submit(() -> transport.get(null, POSITIVE_HTTP_BEARER, ""));
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                try (var connection = transport.fixture.base.admin(); var statement = connection.createStatement()) {
                    statement.execute("SET lock_timeout='1000ms'"); statement.execute("SET statement_timeout='3000ms'");
                    try (var rows = statement.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE pid IN (" + admissionPid + "," + evidencePid + ") AND (xact_start IS NOT NULL OR state LIKE 'idle in transaction%')")) {
                        assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isZero();
                    }
                    try (var rows = statement.executeQuery("SELECT count(*) FROM pg_locks WHERE pid IN (" + admissionPid + "," + evidencePid + ") AND locktype IN ('transactionid','tuple','advisory')")) {
                        assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isZero();
                    }
                    try (var update = connection.prepareStatement("UPDATE sharing.evidence_share_grant SET status='LOCKED',revision=revision+1 WHERE id=?")) {
                        update.setObject(1, transport.scenario.grant()); assertThat(update.executeUpdate()).isEqualTo(1);
                    }
                }
            } finally { release.countDown(); }
            assertThat(pending.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            transport.counts(0);
        }
    }

    @Test void transferFaultsReleaseOneSessionAndPreserveDurableSnapshots() throws Exception {
        try (var transport = new Transport(PAYLOAD)) {
            for (String kind : List.of("success", "unsatisfiable", "denied", "unavailable", "short", "read", "cleanup", "close", "writeCommitted", "writeBuffered", "flush")) {
                transport.prepare(Purpose.PLAYBACK);
                var response = new FailingResponse(); response.failAt = Integer.MAX_VALUE;
                String range = null;
                switch (kind) {
                    case "unsatisfiable" -> { range = "bytes=99-"; transport.claim = purpose(transport.claim, Purpose.SEEK); }
                    case "denied" -> transport.invalid = true;
                    case "unavailable" -> transport.source.fault = Fault.UNAVAILABLE;
                    case "short" -> { transport.source.chunk = 4; transport.source.failAfter = 4; }
                    case "read" -> transport.source.fault = Fault.READ;
                    case "cleanup" -> transport.source.fault = Fault.CLEANUP;
                    case "close" -> transport.source.fault = Fault.CLOSE;
                    case "writeCommitted" -> response.failAt = 4;
                    case "writeBuffered" -> { response.failAt = 4; response.commitOnFailure = false; }
                    case "flush" -> response.flushFailure = true;
                }
                String before = transport.fixture.snapshot();
                var request = request(range);
                if (kind.equals("writeCommitted")) {
                    assertThatThrownBy(() -> transport.filter().doFilter(request, response, (incoming, outgoing) -> fail("chain")))
                            .isInstanceOf(IOException.class).hasMessage("Media transfer failed").hasNoCause()
                            .satisfies(failure -> assertThat(failure.getSuppressed()).isEmpty());
                    assertThat(response.getStatus()).isEqualTo(200); assertThat(response.getContentAsString()).isEqualTo("0123");
                    assertThat(response.getContentType()).isEqualTo("audio/wav"); assertThat(response.getContentLengthLong()).isEqualTo(16);
                    assertThat(response.resetsAfterCommit).isZero(); transport.counts(1);
                } else {
                    transport.filter().doFilter(request, response, (incoming, outgoing) -> fail("chain"));
                    int expected = switch (kind) { case "success" -> 200; case "unsatisfiable" -> 416; case "denied" -> 403; case "unavailable" -> 503; default -> 500; };
                    assertThat(response.getStatus()).as(kind).isEqualTo(expected);
                    if (expected == 500) {
                        assertThat(response.getContentAsString()).contains("\"code\":\"INTERNAL\"").doesNotContain(PRIVATE_CANARY, "0123");
                        assertThat(response.getHeader("Content-Range")).isNull(); assertThat(response.getHeader("Accept-Ranges")).isNull();
                        transport.counts(1);
                    }
                    if (expected == 200) transport.counts(0);
                }
                assertThat(transport.fixture.snapshot()).isEqualTo(before);
            }
        }
    }

    @Test void realHttpAndLogsRedactSecretsAndLeaveExistingPublicFlowUnchanged() throws Exception {
        byte[] bytes = new byte[65536];
        for (int index = 0; index < bytes.length; index++) bytes[index] = (byte) ('A' + index % 26);
        try (var transport = new Transport(bytes); var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            transport.prepare(Purpose.PLAYBACK); transport.source.failAfter = 32768;
            String before = transport.fixture.snapshot();
            var response = transport.client.send(transport.http(null, POSITIVE_HTTP_BEARER, "").build(), HttpResponse.BodyHandlers.ofInputStream());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Length")).contains("65536");
            var received = new ByteArrayOutputStream();
            try (var body = response.body()) {
                var reading = executor.submit(() -> {
                    byte[] buffer = new byte[1024];
                    try { int count; while ((count = body.read(buffer)) != -1) received.write(buffer, 0, count); }
                    catch (IOException expected) { return true; }
                    return false;
                });
                assertThat(reading.get(5, TimeUnit.SECONDS) || received.size() < bytes.length).isTrue();
            }
            assertThat(transport.responseBuffer).isEqualTo(8192);
            assertThat(transport.source.transferred).isGreaterThan(transport.responseBuffer).isEqualTo(32768);
            assertThat(received.size()).isPositive().isLessThan(bytes.length);
            assertThat(received.toByteArray()).isEqualTo(Arrays.copyOf(bytes, received.size()));
            transport.counts(1); assertThat(transport.fixture.snapshot()).isEqualTo(before);
            transport.prepare(Purpose.PLAYBACK);
            var denied = transport.get(null, PRIVATE_CANARY, ""); assertThat(denied.statusCode()).isEqualTo(401); transport.redacted(denied);
            assertThat(transport.fixture.verifications).hasValue(0);
            var customer = transport.client.send(HttpRequest.newBuilder(transport.uri("/api/v1/customer/me")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(customer.statusCode()).isEqualTo(401);
            var publicRead = transport.client.send(HttpRequest.newBuilder(transport.uri("/api/v1/public/shares/probe")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(publicRead.statusCode()).isEqualTo(503);
            assertThat(capturedOutput.getAll()).doesNotContain(POSITIVE_HTTP_BEARER, PRIVATE_CANARY, transport.scenario.media().toString(), digest(bytes), "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
    }

    @Test void realAuthorityIssuedCredentialsReachHttpFullAndRange() throws Exception {
        try(var transport=new Transport(PAYLOAD,AuthorityMode.REAL)) {
            assertThat(transport.context.getBeansOfType(MediaPlaybackCredentialAuthority.class)).hasSize(1);
            assertThat(transport.context.getBeansOfType(ShareMediaCredentialProvider.class)).hasSize(1);
            assertThat(transport.context.getBeansOfType(MediaCredentialVerifier.class)).hasSize(1);
            assertThat(transport.context.getBeansOfType(MediaPlaybackSigningKeyProvider.class)).hasSize(1);
            var authority=transport.context.getBean(MediaPlaybackCredentialAuthority.class);
            assertThat(transport.context.getBean(MediaCredentialVerifier.class)).isSameAs(authority);
            assertThat(transport.context.getBean(ShareMediaCredentialProvider.class)).isSameAs(authority);
            assertThat(org.springframework.test.util.ReflectionTestUtils.getField(transport.service,"verifier")).isSameAs(authority);
            for(String range:Arrays.asList(null,"bytes=2-5")) {
                Purpose purpose=range==null ? Purpose.PLAYBACK:Purpose.SEEK;
                transport.prepare(purpose); String credential=transport.issue(purpose);
                transport.fixture.resetTrace(); String before=transport.fixture.snapshot();
                var response=transport.get(range,credential,"");
                assertThat(response.statusCode()).isEqualTo(range==null ? 200:206);
                assertThat(response.body()).isEqualTo(range==null ? PAYLOAD:Arrays.copyOfRange(PAYLOAD,2,6));
                assertThat(response.headers().firstValue("Content-Length")).contains(range==null ? "16":"4");
                assertThat(response.headers().firstValue("Content-Type")).contains("audio/wav");
                assertThat(response.headers().firstValue("Accept-Ranges")).contains("bytes");
                if(range!=null) assertThat(response.headers().firstValue("Content-Range")).contains("bytes 2-5/16");
                transport.counts(0); assertThat(transport.fixture.opens).hasValue(1); assertThat(transport.fixture.resolutions).hasValue(1);
                assertThat(transport.fixture.snapshot()).isEqualTo(before);
                assertThat(response.headers().map().toString()+capturedOutput.getAll()).doesNotContain(credential,transport.scenario.media().toString());
            }
        }
    }

    @Test void realAuthorityTamperScopeAndWindowFailuresNeverReadBytes() throws Exception {
        try(var transport=new Transport(PAYLOAD,AuthorityMode.REAL)) {
            for(String fault:List.of("TAMPER","UNKNOWN","EXPIRED","FUTURE","PURPOSE","OBJECT","GENERATION","WINDOW")) {
                transport.newScenario("NONE"); transport.prepare(Purpose.PLAYBACK);
                String credential=transport.issue(Purpose.PLAYBACK);
                if(fault.equals("TAMPER")) {
                    String[] parts=credential.split("\\."); byte[] bytes=Base64.getUrlDecoder().decode(parts[2]); bytes[51]^=1;
                    credential="ocmp1.K1."+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)+"."+parts[3];
                } else if(fault.equals("UNKNOWN")) credential=credential.replace(".K1.",".removed.");
                else credential=adversarial(credential,fault);
                transport.fixture.resetTrace(); String before=transport.fixture.snapshot();
                var response=transport.get(null,credential,"");
                assertThat(response.statusCode()).as(fault).isEqualTo(403);
                assertThat(transport.source.opens).hasValue(0); assertThat(transport.source.reads).hasValue(0);
                assertThat(transport.fixture.snapshot()).isEqualTo(before); transport.redacted(response);
                assertThat(capturedOutput.getAll()).doesNotContain(credential);
            }
        }
    }

    private static String adversarial(String credential,String fault) throws Exception {
        String[] parts=credential.split("\\."); byte[] bytes=Base64.getUrlDecoder().decode(parts[2]); var payload=java.nio.ByteBuffer.wrap(bytes);
        switch(fault) {
            case "EXPIRED" -> { payload.putLong(133,1); payload.putLong(141,1); payload.putLong(149,2); }
            case "FUTURE" -> { long future=Instant.now().plusSeconds(86400).getEpochSecond()*1_000_000L; payload.putLong(133,future); payload.putLong(141,future); payload.putLong(149,future+1_000_000L); }
            case "PURPOSE" -> bytes=replacePurpose(bytes,"RENEW");
            case "OBJECT" -> bytes[114]^=1;
            case "GENERATION" -> payload.putInt(115,99);
            case "WINDOW" -> payload.putLong(161,1_000_000_000L);
            default -> throw new AssertionError(fault);
        }
        var key=((MediaPlaybackSigningKeyProvider.Result.Available)DeterministicShareCommandProviders.mediaPlaybackSigningKeys().currentKeys()).active().material();
        var mac=javax.crypto.Mac.getInstance("HmacSHA256"); mac.init(new javax.crypto.spec.SecretKeySpec(key,"HmacSHA256"));
        mac.update("onecall-media-playback\0v1\0K1\0".getBytes(StandardCharsets.US_ASCII));
        return "ocmp1.K1."+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(bytes));
    }

    private static byte[] replacePurpose(byte[] bytes,String purpose) {
        byte[] name=purpose.getBytes(StandardCharsets.US_ASCII); var result=java.nio.ByteBuffer.allocate(bytes.length-8+name.length);
        result.put(bytes,0,123).putShort((short)name.length).put(name).put(bytes,133,bytes.length-133); return result.array();
    }

    private static int pid(javax.sql.DataSource pool) throws Exception {
        try (var connection = pool.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT pg_backend_pid()")) {
            assertThat(rows.next()).isTrue(); return rows.getInt(1);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class HttpBeans {
        @Bean ControlledMediaByteSource transportByteSource(Transport transport) { return request -> transport.source.open(request); }
        @Bean WebServerFactoryCustomizer<TomcatServletWebServerFactory> transportBuffer(Transport transport) {
            return factory -> factory.addContextValves(new ValveBase() {
                @Override public void invoke(Request request, Response response) throws IOException, jakarta.servlet.ServletException {
                    transport.responseBuffer = response.getBufferSize();
                    getNext().invoke(request, response);
                }
            });
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class StubPlaybackBeans {
        @Bean @Primary MediaPlaybackService transportPlaybackService(Transport transport) { return transport.service; }
    }

    @Configuration(proxyBeanMethods = false)
    static class RealAuthorityHttpBeans {
        @Bean MediaPlaybackSigningKeyProvider transportKeys() { return DeterministicShareCommandProviders.mediaPlaybackSigningKeys(); }
        @Bean PlaybackStateReader transportState(Transport transport) { return claim -> transport.fixture.state.check(claim); }
        @Bean EvidenceMediaObjectReader transportEvidence(Transport transport) { return evidence -> transport.fixture.evidence.resolve(evidence); }
        @Bean ControlledMediaReader transportReader(Transport transport) { return transport::typedOpen; }
    }

    enum AuthorityMode { STUB, REAL }

    static final class Transport implements AutoCloseable {
        final MediaPlaybackIT.Fixture fixture;
        final byte[] bytes;
        MediaPlaybackIT.Scenario scenario;
        VerifiedMediaClaim claim;
        Source source;
        MediaPlaybackService service;
        final ConfigurableApplicationContext context;
        final HttpClient client = HttpClient.newHttpClient();
        boolean invalid;
        boolean unavailable;
        int sealedCheckouts;
        volatile int responseBuffer;

        Transport(byte[] bytes) throws Exception {
            this(bytes,AuthorityMode.STUB);
        }
        Transport(byte[] bytes,AuthorityMode mode) throws Exception {
            this.bytes = bytes.clone(); fixture = new MediaPlaybackIT.Fixture();
            try {
                newScenario("NONE");
                if(mode==AuthorityMode.STUB) service = new MediaPlaybackService(credential -> {
                    assertThat(fixture.afterOpen).isFalse(); fixture.verifications.incrementAndGet();
                    if (unavailable) return new MediaCredentialVerifier.Result.Unavailable();
                    return invalid || !POSITIVE_HTTP_BEARER.equals(credential.value()) ? new MediaCredentialVerifier.Result.Invalid()
                            : new MediaCredentialVerifier.Result.Verified(claim);
                }, fixture.state, fixture.evidence, scope -> {
                    assertUnbound(); assertThat(fixture.afterOpen).isFalse();
                    fixture.opens.incrementAndGet(); sealedCheckouts = fixture.checkouts.get(); fixture.afterOpen = true;
                    return new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope));
                }, new Clock() {
                    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
                    @Override public Clock withZone(ZoneId zone) { return this; }
                    @Override public Instant instant() { assertThat(fixture.afterOpen).isFalse(); return Instant.now(); }
                });
                context = new SpringApplicationBuilder(OneCallApplication.class, HttpBeans.class,mode==AuthorityMode.STUB ? StubPlaybackBeans.class:RealAuthorityHttpBeans.class).profiles("web")
                        .properties("server.port=0", "spring.main.banner-mode=off")
                        .initializers(application -> application.getBeanFactory().registerSingleton("transportFixture", this)).run();
                if(mode==AuthorityMode.REAL) service=context.getBean(MediaPlaybackService.class);
            } catch (Exception | Error failure) { client.close(); fixture.close(); throw failure; }
        }
        ControlledMediaReader.Result typedOpen(ControlledMediaReader.ReadScope scope) {
            assertUnbound(); assertThat(fixture.afterOpen).isFalse(); fixture.opens.incrementAndGet();
            sealedCheckouts=fixture.checkouts.get(); fixture.afterOpen=true;
            return new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope));
        }
        String issue(Purpose purpose) {
            var found=(EvidenceMediaObjectReader.Result.Found)fixture.evidence.resolve(scenario.evidence()); var media=found.media();
            assertThat(media.mediaObjectId()).isEqualTo(scenario.media()); assertThat(media.sha256()).isEqualTo(digest(bytes));
            assertThat(media.sizeBytes()).isEqualTo(bytes.length); assertThat(media.mimeType()).isEqualTo("audio/wav"); assertThat(media.durationSeconds()).isEqualTo(10);
            var facts=new ShareMediaCredentialProvider.AuthorizedMediaFacts(scenario.evidence(),media.mediaObjectId(),media.sha256(),media.sizeBytes(),media.mimeType(),media.durationSeconds(),0,10_000_000_000L);
            var witness=scenario.witness(); Instant bound=List.of(witness.grantExpiresAt(),witness.admissionExpiresAt(),witness.sessionExpiresAt()).stream().min(Instant::compareTo).orElseThrow();
            String event=switch(purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; };
            var issuer=MediaPlaybackIT.realAuthority(scenario.claim().issuedAt(),context.getBean(MediaPlaybackSigningKeyProvider.class));
            var result=issuer.issueOrReplay(new ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest(scenario.grant(),scenario.claim().shareAdmissionId(),purpose.name(),event,
                    "a".repeat(64),scenario.claim().codeGeneration(),scenario.claim().revocationGeneration(),witness.grantExpiresAt(),witness.admissionExpiresAt(),witness.sessionExpiresAt(),bound,
                    "b".repeat(64),"event",facts,null));
            assertThat(result).isInstanceOf(ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued.class);
            var issued=(ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued)result;
            assertThat(issued.expiresAt()).isEqualTo(bound.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            long micros=scenario.claim().issuedAt().getEpochSecond()*1_000_000L+scenario.claim().issuedAt().getNano()/1_000;
            assertThat(issued.replayAnchor()).isEqualTo(new ShareMediaCredentialProvider.ReplayAnchor(micros,micros,"K1"));
            return issued.credential();
        }
        void newScenario(String mode) throws Exception {
            scenario = fixture.scenario(mode, "VALIDATED", false, digest(bytes), bytes.length);
            prepare(Purpose.PLAYBACK);
        }
        void prepare(Purpose purpose) {
            fixture.resetTrace(); claim = purpose(scenario.claim(), purpose); invalid = false; unavailable = false;
            source = new Source(scenario.media(), bytes);
            source.callback = () -> {
                assertUnbound(); assertThat(fixture.afterOpen).isTrue();
                assertThat(fixture.checkouts).hasValue(sealedCheckouts);
                assertThat(fixture.admissionPool.getHikariPoolMXBean().getActiveConnections()).isZero();
                assertThat(fixture.evidencePool.getHikariPoolMXBean().getActiveConnections()).isZero();
            };
        }
        VerifiedMediaClaim changed(UUID media, UUID admission, Instant expires, VerifiedMediaClaim.PlaybackWindow window) {
            return copy(claim, claim.audience(), claim.shareGrantId(), admission, claim.evidenceId(), media, claim.purpose(), claim.issuedAt(), claim.notBefore(), expires, window);
        }
        void assertUnbound() {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
            assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
        }
        MediaGatewayFilter filter() { return new MediaGatewayFilter(service, source, new MediaGatewayProblemWriter()); }
        URI uri(String path) { return URI.create("http://localhost:" + ((ServletWebServerApplicationContext) context).getWebServer().getPort() + path); }
        HttpRequest.Builder http(String range, String bearer, String query) {
            var request = HttpRequest.newBuilder(uri("/internal/media-gateway/v1/playback" + query)).timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + bearer).GET();
            if (range != null) request.header("Range", range);
            return request;
        }
        HttpResponse<byte[]> get(String range, String bearer, String query) throws Exception {
            synchronized (fixture) { return client.send(http(range, bearer, query).build(), HttpResponse.BodyHandlers.ofByteArray()); }
        }
        void counts(int aborts) {
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                assertThat(source.opens).hasValue(1); assertThat(source.abortAttempts).hasValue(aborts); assertThat(source.closeAttempts).hasValue(1);
            });
            assertThat(source.requested).allMatch(count -> count > 0 && count <= 8192);
            assertThat(fixture.checkouts).hasValue(sealedCheckouts);
        }
        void redacted(HttpResponse<byte[]> response) {
            assertThat(response.headers().map().toString() + new String(response.body(), StandardCharsets.UTF_8))
                    .doesNotContain(POSITIVE_HTTP_BEARER, PRIVATE_CANARY, scenario.media().toString(), digest(bytes), "0123456789ABCDEF");
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        }
        @Override public void close() throws Exception {
            try { client.close(); } finally { try { context.close(); } finally { fixture.close(); } }
        }
    }
}
