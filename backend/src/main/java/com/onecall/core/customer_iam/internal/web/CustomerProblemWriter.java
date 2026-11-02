package com.onecall.core.customer_iam.internal.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

public final class CustomerProblemWriter {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Problem(String type, String title, int status, String code, String traceId) { }

    public void headers(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    public void write(HttpServletResponse response, CustomerContextException.Code code, UUID traceId) throws IOException {
        write(response, code.name(), traceId);
    }

    public void notFound(HttpServletResponse response, UUID traceId) throws IOException {
        write(response, "NOT_FOUND", traceId);
    }

    private void write(HttpServletResponse response, String code, UUID traceId) throws IOException {
        if (response.isCommitted()) { return; }
        int status = switch (code) {
            case "UNAUTHENTICATED" -> 401;
            case "FORBIDDEN" -> 403;
            case "NOT_FOUND" -> 404;
            case "VALIDATION_FAILED" -> 422;
            case "RATE_LIMITED" -> 429;
            case "SERVICE_UNAVAILABLE" -> 503;
            default -> 500;
        };
        String title = switch (code) {
            case "UNAUTHENTICATED" -> "Authentication required";
            case "FORBIDDEN" -> "Access denied";
            case "NOT_FOUND" -> "Not found";
            case "VALIDATION_FAILED" -> "Request validation failed";
            case "RATE_LIMITED" -> "Too many requests";
            case "SERVICE_UNAVAILABLE" -> "Service unavailable";
            default -> "Internal error";
        };
        response.reset();
        headers(response);
        response.setStatus(status);
        response.setContentType("application/problem+json");
        if (status == 401) { response.setHeader("WWW-Authenticate", "Bearer"); }
        byte[] bytes = mapper.writeValueAsBytes(new Problem("about:blank", title, status, code, traceId.toString()));
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}
