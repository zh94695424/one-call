package com.onecall.core.customer_iam.internal.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.sharing.internal.command.ShareCommandException;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryException;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException;
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

    public void write(HttpServletResponse response, ShareAdmissionException.Code code, UUID traceId) throws IOException {
        if (response.isCommitted()) return;
        int status = switch (code) {
            case UNAUTHENTICATED -> 401;
            case FORBIDDEN -> 403;
            case VALIDATION_FAILED -> 422;
            case SHARE_VERIFICATION_FAILED -> 403;
            case SHARE_RISK_BLOCKED -> 403;
            case SHARE_WRONG_TENANT -> 403;
            case SHARE_TOKEN_INVALID -> 404;
            case SHARE_REVOKED -> 410;
            case SHARE_EXPIRED -> 410;
            case SHARE_LOCKED -> 409;
            case SHARE_ADMISSION_LIMIT_REACHED -> 409;
            case SHARE_GENERATION_STALE -> 409;
            case SHARE_SESSION_EXPIRED -> 410;
            case SHARE_SESSION_POLICY_UNAVAILABLE -> 503;
            case DEPENDENCY_UNAVAILABLE -> 503;
            case RATE_LIMITED -> 429;
            case SERVICE_UNAVAILABLE -> 503;
            case INTERNAL -> 500;
        };
        String title = switch (code) {
            case UNAUTHENTICATED -> "Authentication required";
            case FORBIDDEN -> "Access denied";
            case VALIDATION_FAILED -> "Request validation failed";
            case SHARE_VERIFICATION_FAILED -> "Share verification failed";
            case SHARE_RISK_BLOCKED -> "Share risk blocked";
            case SHARE_WRONG_TENANT -> "Share tenant mismatch";
            case SHARE_TOKEN_INVALID -> "Invalid share token";
            case SHARE_REVOKED -> "Share revoked";
            case SHARE_EXPIRED -> "Share expired";
            case SHARE_LOCKED -> "Share locked";
            case SHARE_ADMISSION_LIMIT_REACHED -> "Share admission limit reached";
            case SHARE_GENERATION_STALE -> "Share generation stale";
            case SHARE_SESSION_EXPIRED -> "Share session expired";
            case SHARE_SESSION_POLICY_UNAVAILABLE -> "Share session policy unavailable";
            case DEPENDENCY_UNAVAILABLE -> "Dependency unavailable";
            case RATE_LIMITED -> "Too many requests";
            case SERVICE_UNAVAILABLE -> "Service unavailable";
            case INTERNAL -> "Internal error";
        };
        response.reset();
        headers(response);
        response.setStatus(status);
        response.setContentType("application/problem+json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        byte[] bytes = mapper.writeValueAsBytes(new Problem("about:blank", title, status, code.name(), traceId.toString()));
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    public void notFound(HttpServletResponse response, UUID traceId) throws IOException {
        write(response, "NOT_FOUND", traceId);
    }

    public void write(HttpServletResponse response, PublicShareQueryException.Code code, UUID traceId) throws IOException {
        write(response, code.name(), traceId);
    }

    public void write(HttpServletResponse response, ShareCommandException.Code code, UUID traceId) throws IOException {
        write(response, code.name(), traceId);
    }

    public void write(HttpServletResponse response, com.onecall.core.subject_number.customerclaim.SubjectClaimException.Code code, UUID traceId) throws IOException {
        write(response, code.name(), traceId);
    }

    public void write(HttpServletResponse response, com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code code, UUID traceId) throws IOException {
        write(response, code.name(), traceId);
    }

    private void write(HttpServletResponse response, String code, UUID traceId) throws IOException {
        if (response.isCommitted()) { return; }
        int status = switch (code) {
            case "UNAUTHENTICATED" -> 401;
            case "FORBIDDEN" -> 403;
            case "NOT_FOUND", "SHARE_TOKEN_INVALID" -> 404;
            case "VALIDATION_FAILED" -> 422;
            case "CURSOR_INVALID", "CURSOR_FILTER_MISMATCH" -> 400;
            case "CONSENT_REQUIRED", "ACTIVATION_CONSENT_DECLINED" -> 409;
            case "RATE_LIMITED" -> 429;
            case "SERVICE_UNAVAILABLE" -> 503;
            case "SHARE_RISK_BLOCKED", "SHARE_ACCESS_POLICY_UNAVAILABLE" -> 403;
            case "SHARE_CREATION_CONFLICT", "SHARE_TRANSITION_CONFLICT", "SHARE_GENERATION_STALE" -> 409;
            case "CLAIM_RISK_BLOCKED", "CLAIM_REPLAY_CONFLICT", "CLAIM_CONTEXT_CONFLICT" -> 409;
            case "SHARE_REVOKED", "SHARE_EXPIRED" -> 410;
            default -> 500;
        };
        String title = switch (code) {
            case "UNAUTHENTICATED" -> "Authentication required";
            case "FORBIDDEN" -> "Access denied";
            case "NOT_FOUND" -> "Not found";
            case "SHARE_TOKEN_INVALID" -> "Invalid share token";
            case "SHARE_EXPIRED" -> "Share expired";
            case "VALIDATION_FAILED" -> "Request validation failed";
            case "CURSOR_INVALID" -> "Invalid cursor";
            case "CURSOR_FILTER_MISMATCH" -> "Cursor filter mismatch";
            case "CONSENT_REQUIRED" -> "Current consent required";
            case "ACTIVATION_CONSENT_DECLINED" -> "Activation consent declined";
            case "RATE_LIMITED" -> "Too many requests";
            case "SERVICE_UNAVAILABLE" -> "Service unavailable";
            case "SHARE_RISK_BLOCKED" -> "Share risk blocked";
            case "SHARE_ACCESS_POLICY_UNAVAILABLE" -> "Sharing policy unavailable";
            case "SHARE_CREATION_CONFLICT" -> "Share creation conflict";
            case "CLAIM_RISK_BLOCKED" -> "Claim risk blocked";
            case "CLAIM_REPLAY_CONFLICT" -> "Claim replay conflict";
            case "CLAIM_CONTEXT_CONFLICT" -> "Claim context conflict";
            case "SHARE_TRANSITION_CONFLICT" -> "Share transition conflict";
            case "SHARE_GENERATION_STALE" -> "Share generation stale";
            case "SHARE_REVOKED" -> "Share revoked";
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
