package com.onecall.core.operations_iam.internal.web;

import com.onecall.core.operations_iam.internal.security.EmployeeBearerAuthenticator;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Code;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Failure;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.UUID;
import org.springframework.http.MediaType;

public final class OperationsBoundaryFilter implements Filter {
    private static final String TRACE = OperationsBoundaryFilter.class.getName() + ".trace";
    private static final String ACTION = OperationsBoundaryFilter.class.getName() + ".action";
    private final EmployeeBearerAuthenticator authenticator;
    private final OperationsProblemWriter problems;
    public OperationsBoundaryFilter(EmployeeBearerAuthenticator authenticator, OperationsProblemWriter problems) { this.authenticator = authenticator; this.problems = problems; }
    static UUID trace(HttpServletRequest request) { return request.getAttribute(TRACE) instanceof UUID trace ? trace : UUID.randomUUID(); }
    static String idempotencyKey(HttpServletRequest request) { return (String) request.getAttribute(ACTION); }
    @Override public void doFilter(ServletRequest incoming, ServletResponse outgoing, FilterChain chain) throws IOException, ServletException {
        var request = (HttpServletRequest) incoming;
        var response = (HttpServletResponse) outgoing;
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            if ((request.getContextPath() + "/api/v1/operations/activations").equals(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))
                    && "TRACE".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))) {
                problems.write(response, Code.NOT_FOUND, UUID.randomUUID());
            } else {
                chain.doFilter(incoming, outgoing);
            }
            return;
        }
        UUID trace = UUID.randomUUID();
        request.setAttribute(TRACE, trace);
        problems.headers(response);
        String path = request.getServletPath();
        boolean read = "GET".equals(request.getMethod()) && path != null
                && ("/api/v1/operations/subjects".equals(path) || "/api/v1/operations/activations".equals(path)
                || "/api/v1/operations/ivr-sessions".equals(path)
                || path.matches("/api/v1/operations/ivr-sessions/[^/;%]+")
                || path.matches("/api/v1/operations/subjects/[^/;%]+"));
        if (!(read || "POST".equals(request.getMethod()) && path != null && path.matches("/api/v1/operations/subjects/[^/;%]+/(suspensions|resumptions)"))
                || !(request.getContextPath() + path).equals(request.getRequestURI())) {
            problems.write(response, Code.NOT_FOUND, trace); return;
        }
        try {
            var principal = new OperationsRequestPrincipal(authenticator.authenticate(request.getHeaders("Authorization")));
            if (!read) {
            var keys = Collections.list(request.getHeaders("Idempotency-Key"));
            if (keys.size() != 1 || !OperationsSubjectLifecycleJsonBinder.validText(keys.getFirst(), 128)) throw new Failure(Code.VALIDATION_FAILED);
            request.setAttribute(ACTION, keys.getFirst());
            try {
                var types = Collections.list(request.getHeaders("Content-Type"));
                if (types.size() != 1) throw new IllegalArgumentException();
                var type = MediaType.parseMediaType(types.getFirst());
                if (!"application".equalsIgnoreCase(type.getType()) || !"json".equalsIgnoreCase(type.getSubtype())
                        || type.getParameters().keySet().stream().anyMatch(key -> !"charset".equalsIgnoreCase(key))
                        || type.getCharset() != null && !StandardCharsets.UTF_8.equals(type.getCharset())) throw new IllegalArgumentException();
            } catch (IllegalArgumentException invalid) { throw new Failure(Code.VALIDATION_FAILED); }
            var accepted = MediaType.parseMediaTypes(Collections.list(request.getHeaders("Accept")));
            var compatible = accepted.stream().filter(media -> media.isCompatibleWith(MediaType.APPLICATION_JSON))
                    .sorted((left, right) -> {
                        int leftSpecificity = (left.isWildcardType() ? 0 : 1) + (left.isWildcardSubtype() ? 0 : 1);
                        int rightSpecificity = (right.isWildcardType() ? 0 : 1) + (right.isWildcardSubtype() ? 0 : 1);
                        int specificity = Integer.compare(rightSpecificity, leftSpecificity);
                        return specificity != 0 ? specificity : Double.compare(right.getQualityValue(), left.getQualityValue());
                    }).findFirst();
            if (!accepted.isEmpty() && (compatible.isEmpty() || compatible.get().getQualityValue() == 0)) throw new Failure(Code.INTERNAL);
            }
            var trusted = new HttpServletRequestWrapper(request) {
                @Override public Principal getUserPrincipal() { return principal; }
                @Override public String getRemoteUser() { return principal.getName(); }
            };
            var buffered = new BufferedResponse(response);
            chain.doFilter(trusted, buffered);
            if (buffered.getStatus() >= 400 && !String.valueOf(buffered.getContentType()).startsWith("application/problem+json")) throw new Failure(Code.INTERNAL);
            buffered.finish();
        } catch (Failure known) { problems.write(response, known.code(), trace); }
        catch (Exception unknown) { problems.write(response, Code.INTERNAL, trace); }
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
            throw new IOException("Downstream response failed");
        }
        @Override
        public void sendError(int status, String message) throws IOException { sendError(status); }

        private void finish() throws IOException {
            flushBuffer();
            bytes.writeTo(super.getOutputStream());
        }
    }
}
