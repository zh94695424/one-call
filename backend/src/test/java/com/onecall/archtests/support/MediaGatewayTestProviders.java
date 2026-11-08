package com.onecall.archtests.support;

import com.onecall.core.evidence.mediaread.ControlledMediaByteSource;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import com.onecall.core.sharing.internal.gateway.MediaGatewayFilter;
import com.onecall.core.sharing.internal.gateway.MediaGatewayProblemWriter;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public final class MediaGatewayTestProviders {
    public static final String POSITIVE_HTTP_BEARER = "pa20-positive-playback-token";
    public static final String PRIVATE_CANARY = "PA19_PRIVATE_cos://bucket/key?version=secret";
    public static final byte[] PAYLOAD = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    public static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");

    private MediaGatewayTestProviders() { }

    public static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception failure) { throw new AssertionError("Fixture digest unavailable"); }
    }

    public static String patternedDigest(byte[] pattern, long size) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] block = new byte[8192];
            if (pattern.length == 0 || pattern.length > block.length) throw new IllegalArgumentException("Invalid fixture pattern");
            int blockSize = block.length - block.length % pattern.length;
            for (int index = 0; index < blockSize; index++) block[index] = pattern[index % pattern.length];
            long position = 0;
            while (position < size) {
                int count = (int) Math.min(blockSize, size - position);
                digest.update(block, 0, count); position += count;
            }
            Arrays.fill(block, (byte) 0);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception failure) { throw new AssertionError("Fixture digest unavailable"); }
    }

    public static VerifiedMediaClaim claim(UUID mediaId, Purpose purpose) {
        return new VerifiedMediaClaim("onecall:media-playback:v1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                mediaId, 0, 0, purpose, NOW.minusSeconds(1), NOW.minusSeconds(1), NOW.plusSeconds(60), 10,
                new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10)));
    }

    public static VerifiedMediaClaim purpose(VerifiedMediaClaim claim, Purpose purpose) {
        return copy(claim, claim.audience(), claim.shareGrantId(), claim.shareAdmissionId(), claim.evidenceId(), claim.mediaObjectId(),
                purpose, claim.issuedAt(), claim.notBefore(), claim.expiresAt(), claim.window());
    }

    public static VerifiedMediaClaim copy(VerifiedMediaClaim claim, String audience, UUID grant, UUID admission, UUID evidence,
            UUID media, Purpose purpose, Instant issued, Instant notBefore, Instant expires, VerifiedMediaClaim.PlaybackWindow window) {
        return new VerifiedMediaClaim(audience, grant, admission, evidence, media, claim.revocationGeneration(), claim.codeGeneration(),
                purpose, issued, notBefore, expires, claim.durationSeconds(), window);
    }

    public static MockHttpServletRequest request(String range) {
        var request = new MockHttpServletRequest("GET", "/internal/media-gateway/v1/playback");
        request.setServletPath("/internal/media-gateway/v1/playback");
        request.addHeader("Authorization", "Bearer " + POSITIVE_HTTP_BEARER);
        if (range != null) request.addHeader("Range", range);
        return request;
    }

    public enum Fault { NONE, UNAVAILABLE, NULL, OPEN, DESCRIPTOR, MISMATCH, HASH, SIZE, INTERVAL, EOF, ZERO, IMPOSSIBLE, READ, CLOSE, CLEANUP }

    public static final class Source implements ControlledMediaByteSource {
        private final UUID mediaId;
        private final String hash;
        private final long size;
        private final byte[] bytes;
        public final AtomicInteger opens = new AtomicInteger();
        public final AtomicInteger reads = new AtomicInteger();
        public final AtomicInteger aborts = new AtomicInteger();
        public final AtomicInteger closes = new AtomicInteger();
        public final AtomicInteger abortAttempts = new AtomicInteger();
        public final AtomicInteger closeAttempts = new AtomicInteger();
        public final List<Integer> requested = new ArrayList<>();
        public final List<Long> positions = new ArrayList<>();
        public final List<String> operations = new ArrayList<>();
        public final Set<byte[]> buffers = Collections.newSetFromMap(new IdentityHashMap<>());
        public Fault fault = Fault.NONE;
        public int chunk = 8192;
        public long failAfter = Long.MAX_VALUE;
        public long transferred;
        public int partialReleases;
        public Runnable callback = () -> { };
        public ReadSession last;
        public String operation;

        public Source(UUID mediaId, byte[] bytes) {
            this(mediaId, digest(bytes), bytes.length, bytes);
        }

        public Source(UUID mediaId, String hash, long size, byte[] pattern) {
            this.mediaId = mediaId; this.hash = hash; this.size = size; this.bytes = pattern.clone();
        }

        @Override public Result open(OpenRequest request) throws IOException {
            opens.incrementAndGet(); operations.add("open"); operation = "open"; callback.run();
            if (fault == Fault.OPEN) { partialReleases++; throw new IOException(PRIVATE_CANARY); }
            if (fault == Fault.NULL) return null;
            if (fault == Fault.UNAVAILABLE || !mediaId.equals(request.media().mediaObjectId())
                    || !hash.equals(request.media().sha256()) || size != request.media().sizeBytes()) return new Result.Unavailable();
            last = new ReadSession() {
                private long position = request.interval().startInclusive();
                private boolean aborted;
                private boolean closed;
                @Override public OpenRequest request() {
                    if (fault == Fault.DESCRIPTOR) throw new IllegalStateException(PRIVATE_CANARY);
                    if (fault == Fault.MISMATCH) return new OpenRequest(new ResolvedMediaObject(UUID.randomUUID(), hash, size, "audio/wav", 10), request.interval());
                    if (fault == Fault.HASH) return new OpenRequest(new ResolvedMediaObject(mediaId, "f".repeat(64), size, "audio/wav", 10), request.interval());
                    if (fault == Fault.SIZE) return new OpenRequest(new ResolvedMediaObject(mediaId, hash, size + 1, "audio/wav", 10), request.interval());
                    if (fault == Fault.INTERVAL) return new OpenRequest(request.media(), new ByteInterval(1, 2));
                    return request;
                }
                @Override public int read(byte[] destination, int offset, int length) throws IOException {
                    operation = "read";
                    callback.run();
                    if (aborted || closed) throw new IOException("Media session closed");
                    Objects.checkFromIndexSize(offset, length, destination.length);
                    if (length == 0) return 0;
                    reads.incrementAndGet(); operations.add("read"); requested.add(length); positions.add(position);
                    buffers.add(destination);
                    if (fault == Fault.READ || fault == Fault.CLEANUP || transferred >= failAfter) throw new IOException(PRIVATE_CANARY);
                    if (fault == Fault.EOF) return -1;
                    if (fault == Fault.ZERO) return 0;
                    if (fault == Fault.IMPOSSIBLE) return length + 1;
                    if (position == request.interval().endExclusive()) return -1;
                    int count = (int) Math.min(Math.min(length, chunk), Math.min(request.interval().endExclusive() - position, failAfter - transferred));
                    for (int index = 0; index < count; index++) destination[offset + index] = bytes[(int) ((position + index) % bytes.length)];
                    position += count; transferred += count;
                    return count;
                }
                @Override public void abort() throws IOException {
                    abortAttempts.incrementAndGet();
                    operation = "abort";
                    callback.run();
                    if (aborted || closed) return;
                    aborted = true; aborts.incrementAndGet(); operations.add("abort");
                    if (fault == Fault.CLEANUP) throw new IOException(PRIVATE_CANARY);
                }
                @Override public void close() throws IOException {
                    closeAttempts.incrementAndGet();
                    operation = "close";
                    callback.run();
                    if (closed) return;
                    closed = true; closes.incrementAndGet(); operations.add("close");
                    if (fault == Fault.CLOSE || fault == Fault.CLEANUP) throw new IOException(PRIVATE_CANARY);
                }
                @Override public String toString() { return redacted(); }
            };
            return new Result.Opened(last);
        }
        @Override public String toString() { return "TestByteSource[REDACTED]"; }
    }

    public static final class Harness {
        public ResolvedMediaObject media;
        public VerifiedMediaClaim claim;
        public final Source source;
        public final List<String> operations = new ArrayList<>();
        public String received;
        public MediaCredentialVerifier.Result verification;
        public PlaybackStateReader.Result state;
        public com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result evidence;
        public boolean metadataUnavailable;
        public boolean leakAtMetadata;
        public String throwing;
        public int verifies;
        public int checks;
        public int resolves;
        public int metadata;
        public final MediaPlaybackService service;

        public Harness() { this(PAYLOAD.length); }
        public Harness(long size) {
            UUID id = UUID.randomUUID();
            media = new ResolvedMediaObject(id, size == PAYLOAD.length ? digest(PAYLOAD) : patternedDigest(PAYLOAD, size), size, "audio/wav", 10);
            claim = claim(id, Purpose.PLAYBACK);
            source = new Source(id, media.sha256(), size, PAYLOAD);
            service = new MediaPlaybackService(credential -> {
                verifies++; operations.add("verify"); received = credential.value(); fail("verify");
                return verification != null ? verification : new MediaCredentialVerifier.Result.Verified(claim);
            }, value -> {
                checks++; operations.add("state"); fail("state");
                return state != null ? state : new PlaybackStateReader.Result.Allowed(NOW);
            }, idValue -> {
                resolves++; operations.add("evidence"); fail("evidence");
                return evidence != null ? evidence : new com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result.Found(media);
            }, scope -> {
                metadata++; operations.add("metadata"); fail("metadata");
                if (leakAtMetadata) org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(this, "fixture");
                return metadataUnavailable ? new ControlledMediaReader.Result.Unavailable()
                        : new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope));
            }, Clock.fixed(NOW, ZoneOffset.UTC));
        }
        private void fail(String operation) { if (operation.equals(throwing)) throw new IllegalStateException(PRIVATE_CANARY); }
        public MediaGatewayFilter filter() { return new MediaGatewayFilter(service, source, new MediaGatewayProblemWriter()); }
        public MockHttpServletResponse get(String range) throws Exception {
            claim = purpose(claim, range == null ? Purpose.PLAYBACK : Purpose.SEEK);
            return send(request(range));
        }
        public MockHttpServletResponse send(MockHttpServletRequest request) throws Exception {
            var response = new MockHttpServletResponse();
            filter().doFilter(request, response, (incoming, outgoing) -> { throw new AssertionError("Gateway escaped"); });
            return response;
        }
    }

    public static final class FailingResponse extends MockHttpServletResponse {
        public int failAt = 4;
        public boolean commitOnFailure = true;
        public boolean flushFailure;
        public int resetsAfterCommit;
        private boolean failed;
        private int written;
        private ServletOutputStream stream;

        @Override public ServletOutputStream getOutputStream() {
            if (stream == null) {
                ServletOutputStream delegate = super.getOutputStream();
                stream = new ServletOutputStream() {
                    @Override public boolean isReady() { return true; }
                    @Override public void setWriteListener(WriteListener listener) { }
                    @Override public void write(int value) throws IOException {
                        if (!failed && written >= failAt) {
                            failed = true;
                            if (commitOnFailure) setCommitted(true);
                            throw new IOException(PRIVATE_CANARY);
                        }
                        delegate.write(value); written++;
                    }
                };
            }
            return stream;
        }
        @Override public void reset() {
            if (isCommitted()) resetsAfterCommit++;
            super.reset();
        }
        @Override public void flushBuffer() {
            if (flushFailure) throw new IllegalStateException(PRIVATE_CANARY);
            super.flushBuffer();
        }
    }
}
