package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.security.CustomerBearerAuthenticator;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;

public final class CustomerBoundaryFilter implements Filter {
    static final String TRACE_ATTRIBUTE = CustomerBoundaryFilter.class.getName() + ".traceId";
    private final CustomerBearerAuthenticator authenticator;
    private final CustomerProblemWriter problems;

    public CustomerBoundaryFilter(CustomerBearerAuthenticator authenticator, CustomerProblemWriter problems) {
        this.authenticator = authenticator;
        this.problems = problems;
    }

    static UUID trace(HttpServletRequest request) {
        Object value = request.getAttribute(TRACE_ATTRIBUTE);
        return value instanceof UUID traceId ? traceId : UUID.randomUUID();
    }

    static String idempotencyKey(HttpServletRequest request) {
        return (String) request.getAttribute(CustomerBoundaryFilter.class.getName() + ".idempotencyKey");
    }

    @Override
    public void doFilter(ServletRequest incoming, ServletResponse outgoing, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) incoming;
        HttpServletResponse response = (HttpServletResponse) outgoing;
        UUID traceId = UUID.randomUUID();
        request.setAttribute(TRACE_ATTRIBUTE, traceId);
        problems.headers(response);
        String servletPath = request.getServletPath();
        boolean admittedPath = "/api/v1/customer/me".equals(servletPath)
                || "/api/v1/customer/shares".equals(servletPath);
        boolean create = "POST".equals(request.getMethod()) && "/api/v1/customer/shares".equals(servletPath);
        boolean revoke = "POST".equals(request.getMethod())
                && servletPath.matches("/api/v1/customer/shares/[^/;%]+/revocations");
        if (!("GET".equals(request.getMethod()) && admittedPath || create || revoke)
                || !(request.getContextPath() + servletPath).equals(request.getRequestURI())) {
            problems.notFound(response, traceId);
            return;
        }
        try {
            var principal = new CustomerRequestPrincipal(authenticator.authenticate(request.getHeaders("Authorization")));
            if (revoke) {
                var keys = java.util.Collections.list(request.getHeaders("Idempotency-Key"));
                if (keys.size() != 1) throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
                String key = keys.getFirst();
                if (key.isBlank() || key.codePointCount(0, key.length()) > 128 || key.indexOf('\0') >= 0
                        || !StandardCharsets.UTF_8.newEncoder().canEncode(key))
                    throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
                request.setAttribute(CustomerBoundaryFilter.class.getName() + ".idempotencyKey", key);
            }
            if (create || revoke) {
                try {
                    var types = java.util.Collections.list(request.getHeaders("Content-Type"));
                    if (types.size() != 1) throw new IllegalArgumentException();
                    var type = org.springframework.http.MediaType.parseMediaType(types.getFirst());
                    if (!"application".equalsIgnoreCase(type.getType()) || !"json".equalsIgnoreCase(type.getSubtype()))
                        throw new IllegalArgumentException();
                } catch (IllegalArgumentException invalid) {
                    throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
                }
            }
            var accepted = org.springframework.http.MediaType.parseMediaTypes(java.util.Collections.list(request.getHeaders("Accept")));
            var compatible = accepted.stream().filter(media -> media.isCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                    .sorted((left, right) -> {
                        int leftSpecificity = (left.isWildcardType() ? 0 : 1) + (left.isWildcardSubtype() ? 0 : 1);
                        int rightSpecificity = (right.isWildcardType() ? 0 : 1) + (right.isWildcardSubtype() ? 0 : 1);
                        int specificity = Integer.compare(rightSpecificity, leftSpecificity);
                        return specificity != 0 ? specificity : Double.compare(right.getQualityValue(), left.getQualityValue());
                    }).findFirst();
            if (!accepted.isEmpty() && (compatible.isEmpty() || compatible.get().getQualityValue() == 0)) {
                throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
            }
            HttpServletRequestWrapper trusted = new HttpServletRequestWrapper(request) {
                @Override
                public Principal getUserPrincipal() { return principal; }
                @Override
                public String getRemoteUser() { return principal.getName(); }
            };
            BufferedResponse buffered = new BufferedResponse(response);
            chain.doFilter(trusted, buffered);
            if (buffered.errorStatus == 0) {
                buffered.finish();
            }
        } catch (CustomerContextException known) {
            problems.write(response, known.code(), traceId);
        } catch (Exception failure) {
            problems.write(response, CustomerContextException.Code.INTERNAL, traceId);
        }
    }

    private static final class BufferedResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private PrintWriter writer;
        private int errorStatus;
        private final ServletOutputStream stream = new ServletOutputStream() {
            @Override
            public void write(int value) { bytes.write(value); }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener listener) { throw new IllegalStateException("Synchronous response"); }
        };

        private BufferedResponse(HttpServletResponse response) { super(response); }

        @Override
        public ServletOutputStream getOutputStream() { return stream; }
        @Override
        public PrintWriter getWriter() {
            if (writer == null) { writer = new PrintWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8)); }
            return writer;
        }
        @Override
        public void flushBuffer() { if (writer != null) { writer.flush(); } }
        @Override
        public void resetBuffer() { bytes.reset(); }
        @Override
        public void reset() { super.reset(); bytes.reset(); errorStatus = 0; }
        @Override
        public void sendError(int status) throws IOException {
            errorStatus = status;
            bytes.reset();
            super.sendError(status);
        }
        @Override
        public void sendError(int status, String message) throws IOException { sendError(status); }

        private void finish() throws IOException {
            flushBuffer();
            bytes.writeTo(super.getOutputStream());
        }
    }
}
