package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.support.MediaGatewayTestProviders.*;

import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.sharing.internal.gateway.*;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class MediaGatewayWebTests {
    private CapturedOutput capturedOutput;
    @BeforeEach void captureOutput(CapturedOutput output) { capturedOutput = output; }
    private void common(MockHttpServletResponse response) {
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        for (String header : List.of("Set-Cookie", "Location", "Content-Disposition", "ETag", "Last-Modified", "Vary", "Authorization",
                "Access-Control-Allow-Origin", "Content-Encoding")) assertThat(response.getHeader(header)).as(header).isNull();
    }
    private void problem(MockHttpServletResponse response, int status, String code) throws Exception {
        common(response);
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentLengthLong()).isEqualTo(response.getContentAsByteArray().length);
        var json = JsonMapper.builder().build().readTree(response.getContentAsByteArray());
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(json.path("type").asString()).isEqualTo("about:blank");
        assertThat(json.path("code").asString()).isEqualTo(code);
        assertThat(json.path("status").asInt()).isEqualTo(status);
        assertThat(UUID.fromString(json.path("traceId").asString()).version()).isEqualTo(4);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(status == 401 ? "Bearer" : null);
        if (status != 416) {
            assertThat(response.getHeader("Content-Range")).isNull();
            assertThat(response.getHeader("Accept-Ranges")).isNull();
        }
        assertThat(response.getContentAsString()).doesNotContain(PRIVATE_CANARY, POSITIVE_HTTP_BEARER, "0123456789", "sha256", "bucket", "exception");
    }
    private void untouched(Harness harness) {
        assertThat(harness.verifies).isZero(); assertThat(harness.checks).isZero();
        assertThat(harness.resolves).isZero(); assertThat(harness.metadata).isZero();
        assertThat(harness.source.opens).hasValue(0); assertThat(harness.source.reads).hasValue(0);
    }
    private void cleanup(Harness harness, int aborts) {
        assertThat(harness.source.opens).hasValue(1);
        assertThat(harness.source.abortAttempts).hasValue(aborts);
        assertThat(harness.source.closeAttempts).hasValue(1);
    }

    @Test void fullGetReturnsExactAuthorizedBytesAndHeaders() throws Exception {
        var harness = new Harness();
        var response = harness.get(null);
        common(response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsByteArray()).isEqualTo(PAYLOAD);
        assertThat(response.getContentType()).isEqualTo("audio/wav");
        assertThat(response.getContentLengthLong()).isEqualTo(16);
        assertThat(response.getHeader("Accept-Ranges")).isEqualTo("bytes");
        assertThat(response.getHeader("Content-Range")).isNull();
        assertThat(harness.operations).containsExactly("verify", "state", "evidence", "metadata");
        assertThat(harness.source.operations).containsExactly("open", "read", "close");
        assertThat(harness.source.buffers).hasSize(1).allSatisfy(buffer -> assertThat(buffer).hasSize(8192).containsOnly((byte) 0));
        cleanup(harness, 0);
    }
    @Test void singleRangeGetReturnsExactIntervalAnd206Headers() throws Exception {
        String[][] examples = {{"bytes=2-5", "2345", "2-5"}, {"bytes=14-99", "EF", "14-15"}, {"bytes=12-", "CDEF", "12-15"},
                {"bytes=-4", "CDEF", "12-15"}, {"bytes=-99", "0123456789ABCDEF", "0-15"}};
        for (String[] example : examples) {
            var harness = new Harness(); var response = harness.get(example[0]); common(response);
            assertThat(response.getStatus()).isEqualTo(206);
            assertThat(response.getContentAsString()).isEqualTo(example[1]);
            assertThat(response.getContentLengthLong()).isEqualTo(example[1].length());
            assertThat(response.getHeader("Content-Range")).isEqualTo("bytes " + example[2] + "/16");
            assertThat(harness.source.reads).hasValue(1); cleanup(harness, 0);
        }
    }
    @Test void bearerIsPropagatedOpaqueWithoutCustomerIdentity() throws Exception {
        var harness = new Harness(); var request = request(null);
        request.removeHeader("Authorization"); request.addHeader("Authorization", "bEaReR AbC._~+/-==");
        request.setUserPrincipal(() -> PRIVATE_CANARY);
        request.addHeader("Cookie", "credential=" + PRIVATE_CANARY);
        request.addHeader("X-Forwarded-User", PRIVATE_CANARY);
        assertThat(harness.send(request).getStatus()).isEqualTo(200);
        assertThat(harness.received).isEqualTo("AbC._~+/-==");
        var absent = request(null); absent.removeHeader("Authorization"); absent.addHeader("Cookie", "credential=" + POSITIVE_HTTP_BEARER);
        problem(new Harness().send(absent), 401, "UNAUTHENTICATED");
    }
    @Test void gatewayRouteMethodQueryAndBodyAreClosed() throws Exception {
        var harness = new Harness();
        for (String path : List.of("/internal/media-gateway", "/internal/media-gateway/", "/internal/media-gatewayx", "/internal/media-gatewayx/v1/playback",
                "/internal/media-gateway/v1/playback/", "/internal/media-gateway/v1/playback/extra", "/internal/media-gateway//v1/playback",
                "/internal/media-gateway/V1/playback", "/internal/media-gateway/v1/Playback", "/internal/media-gateway/v1/%70layback",
                "/internal/media-gateway%2Fv1/playback", "/internal/media-gateway/v1/%2570layback", "/internal/media-gateway;v=1/v1/playback",
                "/internal/media-gateway/v1/playback;v=1")) {
            var request = request(null); request.setRequestURI(path); request.setServletPath(path);
            problem(harness.send(request), 404, "NOT_FOUND");
        }
        for (String path : List.of("/internal//media-gateway/v1/playback", "//internal/media-gateway/v1/playback", "/Internal/media-gateway/v1/playback",
                "/internal/Media-gateway/v1/playback", "/internal/%6dedia-gateway/v1/playback", "/internal%2Fmedia-gateway/v1/playback",
                "/internal/medi%61-gateway/v1/playback", "/internal;v=1/media-gateway/v1/playback", "/internal/other", "/api/v1/customer/me", "/api/v1/public/shares/probe")) {
            var request = request(null); request.setRequestURI(path); request.setServletPath(path); request.setAttribute("probe", "original");
            var response = new MockHttpServletResponse(); response.setHeader("probe", "original"); var calls = new AtomicInteger();
            harness.filter().doFilter(request, response, (incoming, outgoing) -> calls.incrementAndGet());
            assertThat(calls).hasValue(1); assertThat(response.getHeaderNames()).containsExactly("probe");
            assertThat(response.getContentAsByteArray()).isEmpty(); assertThat(request.getAttribute("probe")).isEqualTo("original");
        }
        for (String field : List.of("context", "servlet", "pathInfo")) {
            var request = request(null);
            if (field.equals("context")) request.setContextPath("/other");
            if (field.equals("servlet")) request.setServletPath("/other");
            if (field.equals("pathInfo")) request.setPathInfo("/other");
            problem(harness.send(request), 404, "NOT_FOUND");
        }
        for (String method : List.of("POST", "PUT", "HEAD", "OPTIONS", "DELETE")) {
            var request = request(null); request.setMethod(method); var response = harness.send(request);
            assertThat(response.getStatus()).isEqualTo(405); assertThat(response.getHeader("Allow")).isEqualTo("GET");
            if (method.equals("HEAD")) { assertThat(response.getContentAsByteArray()).isEmpty(); assertThat(response.getContentLengthLong()).isPositive(); }
            else problem(response, 405, "METHOD_NOT_ALLOWED");
        }
        for (String shape : List.of("query", "emptyQuery", "body", "Transfer-Encoding", "Content-Length", "duplicateLength", "If-Range", "If-Match",
                "If-None-Match", "If-Modified-Since", "If-Unmodified-Since")) {
            var request = request(null);
            switch (shape) {
                case "query" -> request.setQueryString("purpose=PLAYBACK");
                case "emptyQuery" -> request.setQueryString("");
                case "body" -> request.setContent(new byte[]{1});
                case "duplicateLength" -> { request.addHeader("Content-Length", "0"); request.addHeader("Content-Length", "0"); }
                default -> request.addHeader(shape, shape.equals("Content-Length") ? "1" : "probe");
            }
            problem(harness.send(request), 400, "MEDIA_REQUEST_INVALID");
        }
        untouched(harness);
        var context = request(null); context.setContextPath("/app"); context.setRequestURI("/app/internal/media-gateway/v1/playback");
        assertThat(harness.send(context).getStatus()).isEqualTo(200);
    }
    @Test void malformedAndDuplicateBearerStopBeforeVerification() throws Exception {
        var harness = new Harness();
        for (String bearer : List.of("", "Basic token", "Bearer ", "Bearer a,b", "Bearer a b", "Bearer\ttoken", "Bearer  token", "Bearer token\t",
                "Bearer token\n", "Bearer tést", "Bearer a=b", "Bearer " + PRIVATE_CANARY, "Bearer " + "a".repeat(2049))) {
            var request = request(null); request.removeHeader("Authorization"); request.addHeader("Authorization", bearer);
            problem(harness.send(request), 401, "UNAUTHENTICATED");
        }
        var request = request(null); request.addHeader("Authorization", "Bearer second"); problem(harness.send(request), 401, "UNAUTHENTICATED");
        request = request(null); request.removeHeader("Authorization"); problem(harness.send(request), 401, "UNAUTHENTICATED");
        untouched(harness);
    }
    @Test void malformedMultipleAndOverflowRangesStopBeforeAuthorization() throws Exception {
        var harness = new Harness();
        for (String range : List.of("", "items=0-1", "Bytes=0-1", "bytes=0-1,4-5", "bytes=5-2", "bytes=0-9223372036854775808", "bytes=-9223372036854775808",
                "bytes=9223372036854775808-", " bytes=0-1", "bytes=0-1 ", "bytes=0--1", "bytes=-", "bytes=+0-1", "bytes=١-2", "bytes=0-1\r\n", "bytes=" + "0".repeat(122) + "-1"))
            problem(harness.send(request(range)), 400, "MEDIA_REQUEST_INVALID");
        var duplicate = request("bytes=0-1"); duplicate.addHeader("Range", "bytes=2-3");
        problem(harness.send(duplicate), 400, "MEDIA_REQUEST_INVALID"); untouched(harness);
    }
    @Test void unsatisfiableRangeDisclosesSizeOnlyAfterAuthorization() throws Exception {
        for (String range : List.of("bytes=16-", "bytes=99-100", "bytes=-0")) {
            var harness = new Harness(); var response = harness.get(range);
            problem(response, 416, "MEDIA_RANGE_UNSATISFIABLE");
            assertThat(response.getHeader("Content-Range")).isEqualTo("bytes */16");
            assertThat(response.getHeader("Accept-Ranges")).isEqualTo("bytes");
            assertThat(harness.metadata).isEqualTo(1); assertThat(harness.source.opens).hasValue(0);
            harness = new Harness(); harness.verification = new MediaCredentialVerifier.Result.Invalid();
            problem(harness.get(range), 403, "MEDIA_FORBIDDEN");
            harness = new Harness(); harness.verification = new MediaCredentialVerifier.Result.Unavailable();
            problem(harness.get(range), 503, "MEDIA_UNAVAILABLE");
        }
    }
    @Test void rangePresenceSelectsExpectedPurposeWithoutClaimFallback() throws Exception {
        for (Purpose purpose : Purpose.values()) for (String range : Arrays.asList(null, "bytes=0-1")) {
            var harness = new Harness(); harness.claim = purpose(harness.claim, purpose);
            var response = harness.send(request(range));
            boolean allowed = purpose == (range == null ? Purpose.PLAYBACK : Purpose.SEEK);
            assertThat(response.getStatus()).isEqualTo(allowed ? (range == null ? 200 : 206) : 403);
            assertThat(harness.source.opens).hasValue(allowed ? 1 : 0);
        }
    }
    @Test void allPlaybackDenialsHaveOneNonDisclosingEnvelope() throws Exception {
        for (String denial : List.of("invalid", "expired", "future", "audience", "purpose", "object", "admission", "stale", "revoked", "missing")) {
            var harness = new Harness(); var claim = harness.claim;
            switch (denial) {
                case "invalid" -> harness.verification = new MediaCredentialVerifier.Result.Invalid();
                case "expired" -> harness.claim = copy(claim, claim.audience(), claim.shareGrantId(), claim.shareAdmissionId(), claim.evidenceId(), claim.mediaObjectId(), claim.purpose(), claim.issuedAt(), claim.notBefore(), NOW, claim.window());
                case "future" -> harness.claim = copy(claim, claim.audience(), claim.shareGrantId(), claim.shareAdmissionId(), claim.evidenceId(), claim.mediaObjectId(), claim.purpose(), NOW.plusSeconds(1), NOW.plusSeconds(1), claim.expiresAt(), claim.window());
                case "audience" -> harness.claim = copy(claim, "wrong", claim.shareGrantId(), claim.shareAdmissionId(), claim.evidenceId(), claim.mediaObjectId(), claim.purpose(), claim.issuedAt(), claim.notBefore(), claim.expiresAt(), claim.window());
                case "purpose" -> harness.claim = purpose(claim, Purpose.RENEW);
                case "object" -> harness.media = new ResolvedMediaObject(UUID.randomUUID(), harness.media.sha256(), 16, "audio/wav", 10);
                case "missing" -> harness.evidence = new EvidenceMediaObjectReader.Result.Missing();
                default -> harness.state = new PlaybackStateReader.Result.Denied(denial.equals("revoked") ? MediaPlaybackService.Reason.REVOKED
                        : denial.equals("stale") ? MediaPlaybackService.Reason.STALE_GENERATION : MediaPlaybackService.Reason.ADMISSION_MISMATCH);
            }
            var response = harness.send(request(null)); problem(response, 403, "MEDIA_FORBIDDEN");
            assertThat(JsonMapper.builder().build().readTree(response.getContentAsByteArray()).path("title").asString()).isEqualTo("Media access denied");
            assertThat(harness.metadata).isZero(); assertThat(harness.source.opens).hasValue(0);
            assertThat(harness.checks).isEqualTo(List.of("invalid", "expired", "future", "audience", "purpose").contains(denial) ? 0 : 1);
        }
    }
    @Test void unavailableDependenciesAndDefaultByteProviderFailClosed() throws Exception {
        for (String dependency : List.of("verify", "state", "evidence", "metadata", "physicalUnavailable", "physicalNull")) {
            var harness = new Harness();
            switch (dependency) {
                case "verify" -> harness.verification = new MediaCredentialVerifier.Result.Unavailable();
                case "state" -> harness.state = new PlaybackStateReader.Result.Unavailable();
                case "evidence" -> harness.evidence = new EvidenceMediaObjectReader.Result.Unavailable();
                case "metadata" -> harness.metadataUnavailable = true;
                case "physicalUnavailable" -> harness.source.fault = Fault.UNAVAILABLE;
                case "physicalNull" -> harness.source.fault = Fault.NULL;
            }
            problem(harness.get(null), 503, "MEDIA_UNAVAILABLE"); assertThat(harness.source.reads).hasValue(0);
        }
        for (String dependency : List.of("verify", "state", "evidence", "metadata")) {
            var harness = new Harness(); harness.throwing = dependency;
            problem(harness.get(null), 503, "MEDIA_UNAVAILABLE"); assertThat(harness.source.opens).hasValue(0);
        }
    }
    @Test void partialTemporalWindowAndUnsafeMimeNeverOpenBytes() throws Exception {
        for (String range : List.of("bytes=2-5", "bytes=99-", "bytes=5-2")) {
            var harness = new Harness(); var claim = harness.claim;
            harness.claim = copy(claim, claim.audience(), claim.shareGrantId(), claim.shareAdmissionId(), claim.evidenceId(), claim.mediaObjectId(), Purpose.SEEK,
                    claim.issuedAt(), claim.notBefore(), claim.expiresAt(), new VerifiedMediaClaim.PlaybackWindow(Duration.ofSeconds(1), Duration.ofSeconds(8)));
            var response = harness.send(request(range)); boolean malformed = range.equals("bytes=5-2");
            problem(response, malformed ? 400 : 403, malformed ? "MEDIA_REQUEST_INVALID" : "MEDIA_FORBIDDEN");
            assertThat(harness.metadata).isEqualTo(malformed ? 0 : 1); assertThat(harness.source.opens).hasValue(0);
            for (String fault : List.of("query", "body", "bearer")) {
                int before = harness.verifies; var request = request(range);
                if (fault.equals("query")) request.setQueryString("");
                if (fault.equals("body")) request.setContent(new byte[]{1});
                if (fault.equals("bearer")) request.removeHeader("Authorization");
                problem(harness.send(request), fault.equals("bearer") ? 401 : 400, fault.equals("bearer") ? "UNAUTHENTICATED" : "MEDIA_REQUEST_INVALID");
                assertThat(harness.verifies).isEqualTo(before);
            }
        }
        for (String mime : List.of("text/html", "audio/WAV", "audio/*", "audio/wav; charset=utf-8", "audio/wav\r\nX: value", "audio/é", "audio/wav\n")) {
            var harness = new Harness(); harness.media = new ResolvedMediaObject(harness.media.mediaObjectId(), harness.media.sha256(), 16, mime, 10);
            problem(harness.get(null), 500, "INTERNAL"); assertThat(harness.source.opens).hasValue(0);
        }
    }
    @Test void misboundSessionAbortsAndClosesBeforeAnyRead() throws Exception {
        for (Fault fault : List.of(Fault.MISMATCH, Fault.HASH, Fault.SIZE, Fault.INTERVAL, Fault.DESCRIPTOR)) {
            var harness = new Harness(); harness.source.fault = fault;
            problem(harness.get(null), 500, "INTERNAL"); assertThat(harness.source.reads).hasValue(0); cleanup(harness, 1);
        }
    }
    @Test void shortZeroAndImpossibleReadsFailWithoutExtraRead() throws Exception {
        for (Fault fault : List.of(Fault.EOF, Fault.ZERO, Fault.IMPOSSIBLE, Fault.READ, Fault.OPEN)) {
            var harness = new Harness(); harness.source.fault = fault;
            problem(harness.get(null), 500, "INTERNAL");
            if (fault == Fault.OPEN) { assertThat(harness.source.closeAttempts).hasValue(0); assertThat(harness.source.partialReleases).isEqualTo(1); }
            else { assertThat(harness.source.reads).hasValue(1); cleanup(harness, 1); }
        }
        var harness = new Harness(); harness.source.chunk = 4; harness.source.failAfter = 4;
        problem(harness.get(null), 500, "INTERNAL"); assertThat(harness.source.reads).hasValue(2); cleanup(harness, 1);
    }
    @Test void disconnectAndCommittedFailuresNeverAppendProblems() throws Exception {
        var harness = new Harness(); var response = new FailingResponse();
        assertThatThrownBy(() -> harness.filter().doFilter(request(null), response, (incoming, outgoing) -> fail("chain")))
                .isInstanceOf(IOException.class).hasMessage("Media transfer failed").hasNoCause().satisfies(failure -> assertThat(failure.getSuppressed()).isEmpty());
        assertThat(response.getStatus()).isEqualTo(200); assertThat(response.getHeader("Content-Type")).isEqualTo("audio/wav");
        assertThat(response.getContentAsString()).isEqualTo("0123"); assertThat(response.resetsAfterCommit).isZero(); cleanup(harness, 1);
    }
    @Test void abortCloseAndFlushFailuresPreserveCleanupCounts() throws Exception {
        for (Fault fault : List.of(Fault.CLEANUP, Fault.CLOSE)) {
            var harness = new Harness(); harness.source.fault = fault;
            problem(harness.get(null), 500, "INTERNAL"); cleanup(harness, 1);
        }
        var harness = new Harness(); var response = new FailingResponse(); response.failAt = Integer.MAX_VALUE; response.flushFailure = true;
        harness.filter().doFilter(request(null), response, (incoming, outgoing) -> fail("chain"));
        problem(response, 500, "INTERNAL"); cleanup(harness, 1);
        harness = new Harness(); response = new FailingResponse(); response.commitOnFailure = false;
        harness.claim = purpose(harness.claim, Purpose.SEEK); response.failAt = 2;
        harness.filter().doFilter(request("bytes=2-5"), response, (incoming, outgoing) -> fail("chain"));
        problem(response, 500, "INTERNAL"); cleanup(harness, 1);
    }
    @Test void ambientTransactionsAndBoundResourcesPreventTransfer() throws Exception {
        for (String binding : List.of("actual", "synchronization", "resource", "leak")) {
            var harness = new Harness();
            try {
                if (binding.equals("actual")) TransactionSynchronizationManager.setActualTransactionActive(true);
                if (binding.equals("synchronization")) TransactionSynchronizationManager.initSynchronization();
                if (binding.equals("resource")) TransactionSynchronizationManager.bindResource(harness, "fixture");
                if (binding.equals("leak")) harness.leakAtMetadata = true;
                problem(harness.get(null), 503, "MEDIA_UNAVAILABLE");
                assertThat(harness.source.opens).hasValue(0);
                assertThat(harness.verifies).isEqualTo(binding.equals("leak") ? 1 : 0);
            } finally {
                TransactionSynchronizationManager.unbindResourceIfPossible(harness);
                TransactionSynchronizationManager.clear();
            }
        }
    }
    @Test void responsesAndCapturedLogsNeverLeakPrivateCanaries() throws Exception {
        var harness = new Harness(); harness.source.fault = Fault.READ;
        var request = request(null); request.addHeader("X-Request-ID", PRIVATE_CANARY); request.addHeader("X-Trace-ID", PRIVATE_CANARY);
        var first = harness.send(request); problem(first, 500, "INTERNAL");
        var second = new Harness(); second.verification = new MediaCredentialVerifier.Result.Invalid();
        var denied = second.send(request(null)); problem(denied, 403, "MEDIA_FORBIDDEN");
        var mapper = JsonMapper.builder().build();
        assertThat(mapper.readTree(first.getContentAsByteArray()).path("traceId").asString())
                .isNotEqualTo(mapper.readTree(denied.getContentAsByteArray()).path("traceId").asString());
        assertThat(capturedOutput.getAll()).doesNotContain(PRIVATE_CANARY, POSITIVE_HTTP_BEARER, harness.media.mediaObjectId().toString(), harness.media.sha256(), "0123456789ABCDEF");
    }
    @Test void largeObjectTransferUsesFixedBufferAndLongAccounting() throws Exception {
        long size = (long) Integer.MAX_VALUE + 100;
        var harness = new Harness(size);
        var response = harness.get("bytes=2147483648-2147483651");
        assertThat(response.getStatus()).isEqualTo(206);
        assertThat(response.getContentAsString()).isEqualTo("0123");
        assertThat(response.getHeader("Content-Range")).isEqualTo("bytes 2147483648-2147483651/2147483747");
        assertThat(harness.source.positions).containsExactly(2147483648L); cleanup(harness, 0);
        harness = new Harness(size); var failing = new FailingResponse();
        var finalHarness = harness;
        assertThatThrownBy(() -> finalHarness.filter().doFilter(request(null), failing, (incoming, outgoing) -> fail("chain"))).hasMessage("Media transfer failed");
        assertThat(failing.getContentLengthLong()).isEqualTo(size); assertThat(harness.source.requested).containsExactly(8192); cleanup(harness, 1);
        assertThat(harness.source.buffers).hasSize(1).allSatisfy(buffer -> assertThat(buffer).hasSize(8192).containsOnly((byte) 0));
    }
}
