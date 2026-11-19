package com.onecall.core.operations_iam.internal.web;

import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Code;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

public final class OperationsProblemWriter {
    private final JsonMapper mapper = JsonMapper.builder().build();
    public record Problem(String type, String title, int status, String code, String traceId) { }
    public void headers(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }
    public void write(HttpServletResponse response,
            com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code code, UUID traceId) throws IOException {
        if (response.isCommitted()) return;
        int status = switch (code) {
            case CURSOR_INVALID, CURSOR_FILTER_MISMATCH -> 400;
            case UNAUTHENTICATED -> 401; case FORBIDDEN -> 403; case NOT_FOUND -> 404;
            case VALIDATION_FAILED -> 422; case RATE_LIMITED -> 429; case SERVICE_UNAVAILABLE -> 503; case INTERNAL -> 500;
        };
        String title = switch (code) {
            case CURSOR_INVALID -> "Invalid cursor"; case CURSOR_FILTER_MISMATCH -> "Cursor filter mismatch";
            case UNAUTHENTICATED -> "Authentication required"; case FORBIDDEN -> "Access denied"; case NOT_FOUND -> "Not found";
            case VALIDATION_FAILED -> "Request validation failed"; case RATE_LIMITED -> "Too many requests";
            case SERVICE_UNAVAILABLE -> "Service unavailable"; case INTERNAL -> "Internal error";
        };
        response.reset();
        headers(response);
        response.setStatus(status);
        response.setContentType("application/problem+json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        byte[] body = mapper.writeValueAsBytes(new Problem("about:blank", title, status, code.name(), traceId.toString()));
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }
    public void write(HttpServletResponse response, Code code, UUID traceId) throws IOException {
        if (response.isCommitted()) return;
        int status = switch (code) {
            case UNAUTHENTICATED -> 401; case FORBIDDEN -> 403; case NOT_FOUND -> 404;
            case ACTIVATION_STATE_CONFLICT -> 409; case VALIDATION_FAILED -> 422; case RATE_LIMITED -> 429;
            case SERVICE_UNAVAILABLE -> 503; case INTERNAL -> 500;
        };
        String title = switch (code) {
            case UNAUTHENTICATED -> "Authentication required"; case FORBIDDEN -> "Access denied"; case NOT_FOUND -> "Not found";
            case ACTIVATION_STATE_CONFLICT -> "Activation state conflict"; case VALIDATION_FAILED -> "Request validation failed";
            case RATE_LIMITED -> "Too many requests"; case SERVICE_UNAVAILABLE -> "Service unavailable"; case INTERNAL -> "Internal error";
        };
        response.reset();
        headers(response);
        response.setStatus(status);
        response.setContentType("application/problem+json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        byte[] body = mapper.writeValueAsBytes(new Problem("about:blank", title, status, code.name(), traceId.toString()));
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    public void write(HttpServletResponse response, com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.Code code, UUID traceId) throws IOException {
        if (response.isCommitted()) return;
        int status = switch (code) {
            case UNAUTHENTICATED -> 401; case FORBIDDEN -> 403; case NOT_FOUND -> 404;
            case PUBLICATION_STALE_VIEW, CATALOG_PUBLICATION_CONFLICT -> 409;
            case VALIDATION_FAILED -> 422; case RATE_LIMITED -> 429; case SERVICE_UNAVAILABLE -> 503; case INTERNAL -> 500;
        };
        String title = switch (code) {
            case UNAUTHENTICATED -> "Authentication required"; case FORBIDDEN -> "Access denied"; case NOT_FOUND -> "Not found";
            case PUBLICATION_STALE_VIEW -> "Publication stale view"; case CATALOG_PUBLICATION_CONFLICT -> "Catalog publication conflict";
            case VALIDATION_FAILED -> "Request validation failed"; case RATE_LIMITED -> "Too many requests";
            case SERVICE_UNAVAILABLE -> "Service unavailable"; case INTERNAL -> "Internal error";
        };
        response.reset(); headers(response); response.setStatus(status); response.setContentType("application/problem+json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        byte[] body = mapper.writeValueAsBytes(new Problem("about:blank", title, status, code.name(), traceId.toString()));
        response.setContentLength(body.length); response.getOutputStream().write(body);
    }
}
