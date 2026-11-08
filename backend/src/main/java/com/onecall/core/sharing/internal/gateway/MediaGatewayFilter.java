package com.onecall.core.sharing.internal.gateway;

import com.onecall.core.evidence.mediaread.ControlledMediaByteSource;
import com.onecall.core.sharing.internal.playback.MediaPlaybackService;
import com.onecall.core.sharing.internal.playback.OpaquePlaybackCredential;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import com.onecall.core.sharing.internal.gateway.MediaGatewayProblemWriter.Problem;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MediaGatewayFilter implements Filter {
    private static final String ROUTE = "/internal/media-gateway/v1/playback";
    private final MediaPlaybackService service;
    private final ControlledMediaByteSource source;
    private final MediaGatewayProblemWriter problems;

    public MediaGatewayFilter(MediaPlaybackService service, ControlledMediaByteSource source, MediaGatewayProblemWriter problems) {
        if (service == null || source == null || problems == null) throw new IllegalArgumentException("Invalid gateway dependencies");
        this.service = service; this.source = source; this.problems = problems;
    }

    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest http) || !(response instanceof HttpServletResponse reply)) {
            chain.doFilter(request, response);
            return;
        }
        String uri = http.getRequestURI();
        String context = http.getContextPath();
        String rawPath = uri;
        if (context != null && !context.isEmpty() && uri != null && (uri.equals(context) || uri.startsWith(context + "/")))
            rawPath = uri.substring(context.length());
        if (rawPath == null || !rawPath.startsWith("/internal/media-gateway")) {
            chain.doFilter(request, response);
            return;
        }
        UUID traceId = UUID.randomUUID();
        boolean head = "HEAD".equals(http.getMethod());
        try {
            if (!ROUTE.equals(rawPath) || !ROUTE.equals(http.getServletPath()) || context == null
                    || !uri.equals(context + http.getServletPath()) || http.getPathInfo() != null) {
                problems.write(reply, Problem.NOT_FOUND, traceId, head, null); return;
            }
            if (!"GET".equals(http.getMethod())) { problems.write(reply, Problem.METHOD, traceId, head, null); return; }
            if (!validShape(http)) { problems.write(reply, Problem.REQUEST, traceId, false, null); return; }
            var authorization = headers(http, "Authorization");
            if (authorization.size() != 1 || !authorization.getFirst().matches("(?i:Bearer) [A-Za-z0-9._~+/-]+={0,}")) {
                problems.write(reply, Problem.AUTHENTICATION, traceId, false, null); return;
            }
            String credential = authorization.getFirst().substring(7);
            if (credential.length() > 2048) { problems.write(reply, Problem.AUTHENTICATION, traceId, false, null); return; }
            var parsed = SingleByteRange.parse(headers(http, "Range"));
            if (!(parsed instanceof SingleByteRange.ParseResult.Accepted accepted)) {
                problems.write(reply, Problem.REQUEST, traceId, false, null); return;
            }
            if (bound()) { problems.write(reply, Problem.UNAVAILABLE, traceId, false, null); return; }
            var result = service.authorizeAndOpen(new OpaquePlaybackCredential(credential),
                    accepted.spec() instanceof SingleByteRange.Full ? Purpose.PLAYBACK : Purpose.SEEK);
            if (result instanceof MediaPlaybackService.Result.Denied) {
                problems.write(reply, Problem.FORBIDDEN, traceId, false, null); return;
            }
            if (!(result instanceof MediaPlaybackService.Result.Opened opened)) {
                problems.write(reply, Problem.UNAVAILABLE, traceId, false, null); return;
            }
            var scope = opened.handle().scope();
            var media = scope.media();
            if (!Duration.ZERO.equals(scope.window().startInclusive())
                    || !Duration.ofSeconds(media.durationSeconds()).equals(scope.window().endExclusive())) {
                problems.write(reply, Problem.FORBIDDEN, traceId, false, null); return;
            }
            if (!media.mimeType().matches("^audio/[a-z0-9!#$%&'+.^_`|~-]+$")) {
                problems.write(reply, Problem.INTERNAL, traceId, false, null); return;
            }
            var resolution = SingleByteRange.resolve(accepted.spec(), media.sizeBytes());
            if (!(resolution instanceof SingleByteRange.Resolution.Selected selected)) {
                problems.write(reply, Problem.RANGE, traceId, false, media.sizeBytes()); return;
            }
            if (bound()) { problems.write(reply, Problem.UNAVAILABLE, traceId, false, null); return; }
            transfer(reply, new ControlledMediaByteSource.OpenRequest(media, selected.interval()), selected.partial(), traceId);
        } catch (IOException failure) {
            if (reply.isCommitted()) throw transferFailure();
            safeProblem(reply, traceId, head);
        } catch (RuntimeException failure) {
            if (reply.isCommitted()) throw transferFailure();
            safeProblem(reply, traceId, head);
        }
    }

    private void transfer(HttpServletResponse reply, ControlledMediaByteSource.OpenRequest request, boolean partial, UUID traceId) throws IOException {
        ControlledMediaByteSource.ReadSession session = null;
        boolean closeAttempted = false;
        byte[] buffer = new byte[8192];
        try {
            var result = source.open(request);
            if (!(result instanceof ControlledMediaByteSource.Result.Opened opened)) {
                problems.write(reply, Problem.UNAVAILABLE, traceId, false, null); return;
            }
            session = opened.session();
            if (!request.equals(session.request())) throw transferFailure();
            problems.common(reply);
            reply.setStatus(partial ? 206 : 200);
            reply.setContentType(request.media().mimeType());
            reply.setContentLengthLong(request.interval().length());
            reply.setHeader("Accept-Ranges", "bytes");
            if (partial) reply.setHeader("Content-Range", "bytes " + request.interval().startInclusive() + "-"
                    + (request.interval().endExclusive() - 1) + "/" + request.media().sizeBytes());
            long remaining = request.interval().length();
            var output = reply.getOutputStream();
            while (remaining > 0) {
                int requested = (int) Math.min(buffer.length, remaining);
                int count = session.read(buffer, 0, requested);
                if (count <= 0 || count > requested) throw transferFailure();
                output.write(buffer, 0, count);
                remaining -= count;
            }
            closeAttempted = true;
            session.close();
            reply.flushBuffer();
        } catch (IOException | RuntimeException failure) {
            if (session != null) {
                try { session.abort(); } catch (IOException | RuntimeException ignored) { }
                if (!closeAttempted) {
                    closeAttempted = true;
                    try { session.close(); } catch (IOException | RuntimeException ignored) { }
                }
            }
            if (reply.isCommitted()) throw transferFailure();
            safeProblem(reply, traceId, false);
        } finally { Arrays.fill(buffer, (byte) 0); }
    }

    private void safeProblem(HttpServletResponse reply, UUID traceId, boolean head) throws IOException {
        try { problems.write(reply, Problem.INTERNAL, traceId, head, null); }
        catch (IOException | RuntimeException failure) { throw transferFailure(); }
    }

    private static IOException transferFailure() { return new IOException("Media transfer failed"); }

    private static boolean bound() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.getResourceMap().isEmpty();
    }

    private static List<String> headers(HttpServletRequest request, String name) {
        var values = request.getHeaders(name);
        return values == null ? List.of() : Collections.list(values);
    }

    private static boolean validShape(HttpServletRequest request) {
        if (request.getQueryString() != null || !headers(request, "Transfer-Encoding").isEmpty()) return false;
        for (String conditional : List.of("If-Range", "If-Match", "If-None-Match", "If-Modified-Since", "If-Unmodified-Since"))
            if (!headers(request, conditional).isEmpty()) return false;
        var lengths = headers(request, "Content-Length");
        return lengths.size() <= 1 && (lengths.isEmpty() || "0".equals(lengths.getFirst()))
                && request.getContentLengthLong() <= 0;
    }
}
