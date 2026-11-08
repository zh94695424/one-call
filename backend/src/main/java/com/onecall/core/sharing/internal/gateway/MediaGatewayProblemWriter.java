package com.onecall.core.sharing.internal.gateway;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class MediaGatewayProblemWriter {
    public enum Problem {
        RANGE(416, "MEDIA_RANGE_UNSATISFIABLE", "Range not satisfiable"),
        REQUEST(400, "MEDIA_REQUEST_INVALID", "Invalid media request"),
        AUTHENTICATION(401, "UNAUTHENTICATED", "Authentication required"),
        FORBIDDEN(403, "MEDIA_FORBIDDEN", "Media access denied"),
        UNAVAILABLE(503, "MEDIA_UNAVAILABLE", "Media unavailable"),
        INTERNAL(500, "INTERNAL", "Internal error"),
        NOT_FOUND(404, "NOT_FOUND", "Not found"),
        METHOD(405, "METHOD_NOT_ALLOWED", "Method not allowed");

        private final int status;
        private final String code;
        private final String title;
        Problem(int status, String code, String title) {
            this.status = status; this.code = code; this.title = title;
        }
    }

    public void common(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    public void write(HttpServletResponse response, Problem problem, UUID traceId, boolean head, Long size) throws IOException {
        response.reset();
        common(response);
        response.setStatus(problem.status);
        response.setContentType("application/problem+json");
        if (problem == Problem.AUTHENTICATION) response.setHeader("WWW-Authenticate", "Bearer");
        if (problem == Problem.METHOD) response.setHeader("Allow", "GET");
        if (problem == Problem.RANGE) {
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Range", "bytes */" + size);
        }
        byte[] body = ("{\"type\":\"about:blank\",\"title\":\"" + problem.title + "\",\"status\":" + problem.status
                + ",\"code\":\"" + problem.code + "\",\"traceId\":\"" + traceId + "\"}").getBytes(StandardCharsets.UTF_8);
        response.setContentLengthLong(body.length);
        if (!head) response.getOutputStream().write(body);
    }
}
