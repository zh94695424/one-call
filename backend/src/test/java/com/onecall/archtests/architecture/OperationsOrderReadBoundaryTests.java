package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class OperationsOrderReadBoundaryTests {
    private static final String EXPECTED_PREFIX = """
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
""";
    private static final String BASELINE_ERROR = """
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            if ((request.getContextPath() + "/api/v1/operations/activations").equals(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))
                    && "TRACE".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))) {
                problems.write(response, Code.NOT_FOUND, UUID.randomUUID());
            } else {
                chain.doFilter(incoming, outgoing);
            }
            return;
        }
""";
    private static final String EXPECTED_REQUIRED_ERROR = """
        if (request.getDispatcherType() == DispatcherType.ERROR) {
            if ("TRACE".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))
                    && (request.getContextPath() + "/api/v1/operations/activations").equals(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))
                    || request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String errorUri
                    && errorUri.matches(java.util.regex.Pattern.quote(request.getContextPath()) + "/api/v1/operations/orders([/;%].*)?")
                    && (request.getAttribute(RequestDispatcher.ERROR_METHOD) instanceof String errorMethod && !"GET".equals(errorMethod)
                    || "GET".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))
                    && !errorUri.equals(request.getContextPath() + "/api/v1/operations/orders")
                    && !errorUri.matches(java.util.regex.Pattern.quote(request.getContextPath()) + "/api/v1/operations/orders/[^/;%]+")
                    && (Integer.valueOf(400).equals(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
                    || Integer.valueOf(405).equals(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))))) {
                problems.write(response, Code.NOT_FOUND, UUID.randomUUID());
            } else {
                chain.doFilter(incoming, outgoing);
            }
            return;
        }
""";
    private static final String EXPECTED_BETWEEN = """
        UUID trace = UUID.randomUUID();
        request.setAttribute(TRACE, trace);
        problems.headers(response);
        String path = request.getServletPath();
""";
    private static final String BASELINE_READ = """
        boolean read = "GET".equals(request.getMethod()) && path != null
                && ("/api/v1/operations/subjects".equals(path) || "/api/v1/operations/activations".equals(path)
                || "/api/v1/operations/ivr-sessions".equals(path)
                || "/api/v1/operations/catalog/products".equals(path)
                || path.matches("/api/v1/operations/catalog/products/[^/;%]+")
                || path.matches("/api/v1/operations/ivr-sessions/[^/;%]+")
                || path.matches("/api/v1/operations/subjects/[^/;%]+"));
""";
    private static final String EXPECTED_READ = """
        boolean read = "GET".equals(request.getMethod()) && path != null
                && ("/api/v1/operations/subjects".equals(path) || "/api/v1/operations/activations".equals(path)
                || "/api/v1/operations/ivr-sessions".equals(path)
                || "/api/v1/operations/catalog/products".equals(path)
                || path.matches("/api/v1/operations/catalog/products/[^/;%]+")
                || path.matches("/api/v1/operations/ivr-sessions/[^/;%]+")
                || path.matches("/api/v1/operations/subjects/[^/;%]+")
                || "/api/v1/operations/orders".equals(path)
                || path.matches("/api/v1/operations/orders/[^/;%]+"));
""";
    private static final String EXPECTED_SUFFIX = """
        boolean catalog = "POST".equals(request.getMethod()) && path != null
                && path.matches("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)")
                && (request.getContextPath() + path).equals(request.getRequestURI());
        boolean subject = "POST".equals(request.getMethod()) && path != null && path.matches("/api/v1/operations/subjects/[^/;%]+/(suspensions|resumptions)");
        boolean draft = path != null && ("POST".equals(request.getMethod()) && "/api/v1/operations/catalog/versions".equals(path)
                || "PUT".equals(request.getMethod()) && path.matches("/api/v1/operations/catalog/versions/[^/;%]+/draft"))
                && (request.getContextPath() + path).equals(request.getRequestURI());
        if (!(read || subject || catalog || draft)
                || !(request.getContextPath() + path).equals(request.getRequestURI())) {
            problems.write(response, Code.NOT_FOUND, trace); return;
        }
        try {
            var principal = new OperationsRequestPrincipal(authenticator.authenticate(request.getHeaders("Authorization")));
            if (subject) {
            var keys = Collections.list(request.getHeaders("Idempotency-Key"));
            if (keys.size() != 1 || !OperationsSubjectLifecycleJsonBinder.validText(keys.getFirst(), 128)) throw new Failure(Code.VALIDATION_FAILED);
            request.setAttribute(ACTION, keys.getFirst());
            }
            if (!read) {
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
""";
    private static final String CUSTOMER_BASELINE_GET = """
        if ("GET".equals(request.getMethod()) && operationsPath != null
                && ("/api/v1/operations/subjects".equals(operationsPath) || "/api/v1/operations/activations".equals(operationsPath)
                || "/api/v1/operations/ivr-sessions".equals(operationsPath)
                || "/api/v1/operations/catalog/products".equals(operationsPath)
                || operationsPath.matches("/api/v1/operations/catalog/products/[^/;%]+")
                || operationsPath.matches("/api/v1/operations/ivr-sessions/[^/;%]+")
                || operationsPath.matches("/api/v1/operations/subjects/[^/;%]+"))
                && (request.getContextPath() + operationsPath).equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
""";
    private static final String CUSTOMER_EXPECTED_GET = """
        if ("GET".equals(request.getMethod()) && operationsPath != null
                && ("/api/v1/operations/subjects".equals(operationsPath) || "/api/v1/operations/activations".equals(operationsPath)
                || "/api/v1/operations/ivr-sessions".equals(operationsPath)
                || "/api/v1/operations/catalog/products".equals(operationsPath)
                || operationsPath.matches("/api/v1/operations/catalog/products/[^/;%]+")
                || operationsPath.matches("/api/v1/operations/ivr-sessions/[^/;%]+")
                || operationsPath.matches("/api/v1/operations/subjects/[^/;%]+")
                || "/api/v1/operations/orders".equals(operationsPath)
                || operationsPath.matches("/api/v1/operations/orders/[^/;%]+"))
                && (request.getContextPath() + operationsPath).equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
""";
    private static final String EXPECTED_NO_ERROR_DELTA = EXPECTED_PREFIX + BASELINE_ERROR + EXPECTED_BETWEEN + EXPECTED_READ + EXPECTED_SUFFIX;
    private static final String EXPECTED_PROBE_REQUIRED_ERROR_DELTA = EXPECTED_PREFIX + EXPECTED_REQUIRED_ERROR + EXPECTED_BETWEEN + EXPECTED_READ + EXPECTED_SUFFIX;
    private static final String EXPECTED_NO_ERROR_DELTA_SHA256 = "9b1515baa01e2c5bf47a50828e5365094e66304a41d83829d152ab752fbc80ec";
    private static final String EXPECTED_PROBE_REQUIRED_ERROR_DELTA_SHA256 = "9818a08d947121c36b2303ff6d054aad6d66935e0cb39f846d12deb2a5b99146";
    private static final List<String> READ_ROUTES = List.of("/api/v1/operations/subjects", "/api/v1/operations/activations",
            "/api/v1/operations/ivr-sessions", "/api/v1/operations/catalog/products", "/api/v1/operations/catalog/products/[^/;%]+",
            "/api/v1/operations/ivr-sessions/[^/;%]+", "/api/v1/operations/subjects/[^/;%]+",
            "/api/v1/operations/orders", "/api/v1/operations/orders/[^/;%]+");
    private static final List<String> COMMAND_ROUTES = List.of("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)",
            "/api/v1/operations/subjects/[^/;%]+/(suspensions|resumptions)", "/api/v1/operations/catalog/versions",
            "/api/v1/operations/catalog/versions/[^/;%]+/draft");
    private static final Path BACKEND = backendRoot();
    private static final Path MAIN = BACKEND.resolve("src/main/java/com/onecall/core");
    private static final boolean PROBE_REQUIRES_ERROR_DELTA = true;
    private static final List<String> PRODUCTION = List.of(
            "operations_iam/subjectread/OperationsOrderReadService.java", "operations_iam/subjectread/OperationsOrderReadPorts.java",
            "operations_iam/internal/orderread/OperationsOrderReadRequest.java", "operations_iam/internal/orderread/OperationsOrderCursorCodec.java",
            "operations_iam/internal/orderread/DefaultOperationsOrderReadService.java", "operations_iam/internal/orderread/OperationsOrderReadConfiguration.java",
            "operations_iam/internal/web/OperationsOrderReadController.java", "catalog_order/internal/operationsread/JooqOperationsOrderReader.java",
            "entitlement_metering/internal/operationsread/JooqOperationsOrderIssuanceReader.java");

    private static Path backendRoot() {
        for (Path current = Path.of("").toAbsolutePath(); current != null; current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("pom.xml")) && Files.isDirectory(current.resolve("src/main/java/com/onecall/core")))
                return current;
            Path nested = current.resolve("backend");
            if (Files.isRegularFile(nested.resolve("pom.xml")) && Files.isDirectory(nested.resolve("src/main/java/com/onecall/core")))
                return nested;
        }
        throw new IllegalStateException("backend module root not found above " + Path.of("").toAbsolutePath());
    }
    private static String productionFile(String fileName) {
        var matches = PRODUCTION.stream().filter(path -> path.endsWith("/" + fileName)).toList();
        assertThat(matches).as("unique production file " + fileName).hasSize(1);
        return matches.getFirst();
    }
    private record Token(String text, int start, int end, boolean literal) { }
    private static List<Token> tokens(String source) {
        var result = new ArrayList<Token>();
        int offset = 0;
        while (offset < source.length()) {
            char current = source.charAt(offset);
            if (Character.isWhitespace(current)) { offset++; continue; }
            if (source.startsWith("//", offset)) {
                int end = source.indexOf('\n', offset + 2);
                offset = end < 0 ? source.length() : end + 1;
                continue;
            }
            if (source.startsWith("/*", offset)) {
                int end = source.indexOf("*/", offset + 2);
                assertThat(end).as("complete comment").isGreaterThanOrEqualTo(0);
                offset = end + 2;
                continue;
            }
            int start = offset++;
            if (current == '"' || current == '\'') {
                boolean closed = false;
                while (offset < source.length()) {
                    char next = source.charAt(offset++);
                    if (next == '\\') {
                        assertThat(offset).isLessThan(source.length());
                        offset++;
                    } else if (next == current) { closed = true; break; }
                }
                assertThat(closed).as("complete Java literal").isTrue();
                result.add(new Token(source.substring(start, offset), start, offset, current == '"'));
            } else {
                if (Character.isJavaIdentifierStart(current)) {
                    while (offset < source.length() && Character.isJavaIdentifierPart(source.charAt(offset))) offset++;
                }
                result.add(new Token(source.substring(start, offset), start, offset, false));
            }
        }
        return result;
    }
    private static int marker(List<Token> source, String marker) {
        var expected = tokens(marker);
        var matches = new ArrayList<Integer>();
        for (int index = 0; index + expected.size() <= source.size(); index++) {
            boolean same = true;
            for (int next = 0; next < expected.size(); next++)
                if (!source.get(index + next).text().equals(expected.get(next).text())) { same = false; break; }
            if (same) matches.add(index);
        }
        assertThat(matches).as("unique executable marker " + marker).hasSize(1);
        return matches.getFirst();
    }
    private static String statement(String source, String startMarker, String nextMarker, boolean block) {
        var lexed = tokens(source);
        int start = marker(lexed, startMarker);
        int next = marker(lexed, nextMarker);
        assertThat(next).isGreaterThan(start);
        int parentheses = 0, braces = 0;
        boolean entered = false;
        int end = -1;
        for (int index = start; index < next; index++) {
            var token = lexed.get(index);
            if (token.literal()) continue;
            switch (token.text()) {
                case "(" -> parentheses++;
                case ")" -> parentheses--;
                case "{" -> { braces++; entered = true; }
                case "}" -> braces--;
                default -> { }
            }
            assertThat(parentheses).as("balanced parentheses").isGreaterThanOrEqualTo(0);
            assertThat(braces).as("balanced braces").isGreaterThanOrEqualTo(0);
            if (parentheses == 0 && braces == 0 && (block && entered || !block && token.text().equals(";"))) {
                end = index;
                break;
            }
        }
        assertThat(end).as("complete statement before next marker").isEqualTo(next - 1);
        return source.substring(lexed.get(start).start(), lexed.get(end).end());
    }
    private static List<String> normalized(String source) { return tokens(source).stream().map(Token::text).toList(); }
    private static List<String> literals(String source) {
        return tokens(source).stream().filter(Token::literal).map(Token::text)
                .filter(text -> text.startsWith("\"/api/v1/operations")).map(text -> text.substring(1, text.length() - 1)).toList();
    }
    private static void readSentinels(String statement, String variable) {
        assertThat(literals(statement)).as("independent nine-alternative truncation sentinel").containsExactlyElementsOf(READ_ROUTES);
        for (String predicate : List.of("\"/api/v1/operations/orders\".equals(" + variable + ")",
                variable + ".matches(\"/api/v1/operations/orders/[^/;%]+\")")) {
            assertThat(countPredicate(statement, predicate)).as("complete order predicate " + predicate).isEqualTo(1);
        }
    }
    private static int countPredicate(String statement, String predicate) {
        var expected = normalized(predicate);
        var actual = normalized(statement);
        int count = 0;
        for (int offset = 0; offset + expected.size() <= actual.size(); offset++)
            if (actual.subList(offset, offset + expected.size()).equals(expected)) count++;
        return count;
    }
    private static void errorSentinels(String error, boolean delta) {
        assertThat(countPredicate(error, "\"TRACE\".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))"))
                .as("R1 retained TRACE gate once").isEqualTo(1);
        assertThat(countPredicate(error, "(request.getContextPath() + \"/api/v1/operations/activations\").equals(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))"))
                .as("retained activations exact-URI predicate once").isEqualTo(1);
        if (!delta) return;
        assertThat(countPredicate(error, "errorUri.matches(java.util.regex.Pattern.quote(request.getContextPath()) + \"/api/v1/operations/orders([/;%].*)?\")"))
                .as("bounded order-family regex once").isEqualTo(1);
        assertThat(countPredicate(error, "request.getAttribute(RequestDispatcher.ERROR_METHOD) instanceof String errorMethod && !\"GET\".equals(errorMethod)"))
                .as("type-safe R2a non-GET gate once").isEqualTo(1);
        assertThat(countPredicate(error, "\"GET\".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))"))
                .as("R2b GET gate once").isEqualTo(1);
        assertThat(countPredicate(error, "!errorUri.equals(request.getContextPath() + \"/api/v1/operations/orders\")"))
                .as("canonical-list exclusion once").isEqualTo(1);
        assertThat(countPredicate(error, "!errorUri.matches(java.util.regex.Pattern.quote(request.getContextPath()) + \"/api/v1/operations/orders/[^/;%]+\")"))
                .as("canonical-detail exclusion once").isEqualTo(1);
        assertThat(countPredicate(error, "Integer.valueOf(400).equals(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))"))
                .as("native 400 status gate once").isEqualTo(1);
        assertThat(countPredicate(error, "Integer.valueOf(405).equals(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))"))
                .as("native 405 status gate once").isEqualTo(1);
    }
    private static void census(String source, String read, String error, boolean delta, boolean customer) {
        var expected = new ArrayList<String>(READ_ROUTES);
        expected.addAll(COMMAND_ROUTES);
        if (!customer) {
            expected.add("/api/v1/operations/activations");
            if (delta) expected.addAll(List.of("/api/v1/operations/orders([/;%].*)?", "/api/v1/operations/orders", "/api/v1/operations/orders/[^/;%]+"));
        }
        assertThat(literals(source)).as("whole-file route multiset").containsExactlyInAnyOrderElementsOf(expected);
        assertThat(literals(read)).as("GET region census").containsExactlyElementsOf(READ_ROUTES);
        String outside = source.replace(read, "");
        if (!customer) {
            var errorExpected = delta ? List.of("/api/v1/operations/activations", "/api/v1/operations/orders([/;%].*)?", "/api/v1/operations/orders", "/api/v1/operations/orders/[^/;%]+")
                    : List.of("/api/v1/operations/activations");
            assertThat(literals(error)).as("ERROR region census").containsExactlyElementsOf(errorExpected);
            outside = outside.replace(error, "");
        }
        assertThat(literals(outside)).as("unchanged command-only region census").containsExactlyInAnyOrderElementsOf(COMMAND_ROUTES);
    }
    private static void operationsRoutes(String source, boolean delta) {
        String read = statement(source, "boolean read =", "boolean catalog =", false);
        assertThat(normalized(read)).as("independent read expression").isEqualTo(normalized(EXPECTED_READ));
        readSentinels(read, "path");
        String error = statement(source, "if (request.getDispatcherType() == DispatcherType.ERROR)", "UUID trace =", true);
        assertThat(normalized(error)).as("independent complete ERROR branch").isEqualTo(normalized(delta ? EXPECTED_REQUIRED_ERROR : BASELINE_ERROR));
        errorSentinels(error, delta);
        census(source, read, error, delta, false);
    }
    private static void operationsFrozen(String source, boolean delta) throws Exception {
        assertThat(source).as("frozen remainder / entire independent source").isEqualTo(delta ? EXPECTED_PROBE_REQUIRED_ERROR_DELTA : EXPECTED_NO_ERROR_DELTA);
    }
    private static void operationsHash(String source, boolean delta) throws Exception {
        assertThat(hash(source)).as("fixed independent source hash")
                .isEqualTo(delta ? EXPECTED_PROBE_REQUIRED_ERROR_DELTA_SHA256 : EXPECTED_NO_ERROR_DELTA_SHA256);
    }
    private static void operationsGuard(String source, boolean delta) throws Exception {
        operationsRoutes(source, delta);
        operationsFrozen(source, delta);
        operationsHash(source, delta);
    }
    private static void customerGuard(String source) throws Exception {
        String block = statement(source, "if (\"GET\".equals(request.getMethod()) && operationsPath != null", "UUID traceId =", true);
        assertThat(normalized(block)).as("independent complete customer GET block").isEqualTo(normalized(CUSTOMER_EXPECTED_GET));
        readSentinels(block, "operationsPath");
        census(source, block, "", false, true);
        String exact = CUSTOMER_EXPECTED_GET.strip();
        assertThat(source).contains(exact);
        String baseline = source.replace(exact, CUSTOMER_BASELINE_GET.strip());
        assertThat(hash(baseline)).as("all non-addition customer bytes frozen")
                .isEqualTo("4c254b856c643f13a6f5f2172ecd16707daaa36f4e5a1e3f8e675657bf37e237");
    }
    @Test void independentVariantsRejectRouteGateAndNonRouteMutations() throws Exception {
        for (boolean delta : List.of(false, true)) {
            String valid = delta ? EXPECTED_PROBE_REQUIRED_ERROR_DELTA : EXPECTED_NO_ERROR_DELTA;
            operationsGuard(valid, delta);
            for (String mutant : List.of(
                    valid.replace("boolean read = \"GET\"", "boolean read = \"POST\""),
                    valid.replace(EXPECTED_READ, EXPECTED_READ.replace("));", ") || path.matches(\"/api/v1/operations/orders/[^/;%]+/extra\"));")),
                    valid.replace("boolean catalog = \"POST\"", "boolean catalog = \"/api/v1/operations/orders\".equals(path) || \"POST\""),
                    valid.replace("\"TRACE\".equals", "\"GET\".equals")))
                assertThatThrownBy(() -> operationsGuard(mutant, delta)).isInstanceOf(AssertionError.class);
            String denial = "problems.write(response, Code.NOT_FOUND, trace); return;";
            assertThat(valid.split(Pattern.quote(denial), -1)).hasSize(2);
            String nonRoute = valid.replace(denial, "problems.write(response, Code.INTERNAL, trace); return;");
            operationsRoutes(nonRoute, delta);
            assertThatThrownBy(() -> operationsFrozen(nonRoute, delta)).isInstanceOf(AssertionError.class).hasMessageContaining("frozen remainder");
            assertThatThrownBy(() -> operationsHash(nonRoute, delta)).isInstanceOf(AssertionError.class).hasMessageContaining("fixed independent");
            String truncated = EXPECTED_READ.substring(0, EXPECTED_READ.indexOf(';'));
            assertThat(truncated).as("truncation precedes both order alternatives").doesNotContain("\"/api/v1/operations/orders\"");
            assertThatThrownBy(() -> tokens(truncated)).as("truncation cuts inside a route literal")
                    .isInstanceOf(AssertionError.class).hasMessageContaining("complete Java literal");
            assertThatThrownBy(() -> readSentinels(truncated, "path")).isInstanceOf(AssertionError.class);
            assertThatThrownBy(() -> operationsGuard(delta ? EXPECTED_NO_ERROR_DELTA : EXPECTED_PROBE_REQUIRED_ERROR_DELTA, delta))
                    .isInstanceOf(AssertionError.class);
            if (delta) {
                for (String mutant : List.of(
                        valid.replace("\"/api/v1/operations/orders([/;%].*)?\"", "\"/api/v1/operations/orders.*\""),
                        valid.replace("                    && !errorUri.equals(request.getContextPath() + \"/api/v1/operations/orders\")\n"
                                + "                    && !errorUri.matches(java.util.regex.Pattern.quote(request.getContextPath()) + \"/api/v1/operations/orders/[^/;%]+\")\n", ""),
                        valid.replace("instanceof String errorMethod && !\"GET\".equals(errorMethod)", "instanceof String errorMethod"),
                        valid.replace("                    && (Integer.valueOf(400).equals(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))\n"
                                + "                    || Integer.valueOf(405).equals(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))))) {", "                    )) {"),
                        valid.replace("|| \"GET\".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))",
                                "|| errorUri.equals(request.getContextPath() + \"/api/v1/operations/subjects\") || \"GET\".equals(request.getAttribute(RequestDispatcher.ERROR_METHOD))")))
                    assertThatThrownBy(() -> operationsGuard(mutant, true)).isInstanceOf(AssertionError.class);
                errorBehavior("");
                errorBehavior("/mounted");
            }
        }
    }
    private static int errorDispatch(String context, Object method, String uri, Integer status, boolean[] chained) throws Exception {
        var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        var filter = new com.onecall.core.operations_iam.internal.web.OperationsBoundaryFilter(
                new com.onecall.core.operations_iam.internal.security.EmployeeBearerAuthenticator(
                        beans.getBeanProvider(com.onecall.core.operations_iam.internal.security.EmployeeSessionVerifier.class)),
                new com.onecall.core.operations_iam.internal.web.OperationsProblemWriter());
        var request = new org.springframework.mock.web.MockHttpServletRequest(method instanceof String text ? text : "GET", context + uri);
        request.setContextPath(context);
        request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
        if (method != null) request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_METHOD, method);
        request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, context + uri);
        if (status != null) request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, status);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        filter.doFilter(request, response, (incoming, outgoing) -> chained[0] = true);
        return response.getStatus();
    }
    private static void errorBehavior(String context) throws Exception {
        for (String uri : List.of("/api/v1/operations/orders", "/api/v1/operations/orders/00000000-0000-0000-0000-000000000001",
                "/api/v1/operations/orders/", "/api/v1/operations/orders//x", "/api/v1/operations/orders/x;y",
                "/api/v1/operations/orders/%2fx", "/api/v1/operations/orders/a/b")) {
            var chained = new boolean[1];
            assertThat(errorDispatch(context, "TRACE", uri, 405, chained)).as("non-GET family concealed " + uri).isEqualTo(404);
            assertThat(chained[0]).isFalse();
        }
        var chained = new boolean[1];
        assertThat(errorDispatch(context, "GET", "/api/v1/operations/orders/%zz", 400, chained)).isEqualTo(404);
        assertThat(chained[0]).isFalse();
        for (String uri : List.of("/api/v1/operations/orders", "/api/v1/operations/orders/00000000-0000-0000-0000-000000000001")) {
            for (Integer status : List.of(500, 503)) {
                chained = new boolean[1];
                errorDispatch(context, "GET", uri, status, chained);
                assertThat(chained[0]).as("canonical GET application failure chained " + uri).isTrue();
            }
            chained = new boolean[1];
            errorDispatch(context, null, uri, 500, chained);
            assertThat(chained[0]).as("missing ERROR_METHOD never conceals canonical GET").isTrue();
            chained = new boolean[1];
            errorDispatch(context, 42, uri, 500, chained);
            assertThat(chained[0]).as("non-String ERROR_METHOD never conceals canonical GET").isTrue();
        }
        chained = new boolean[1];
        errorDispatch(context, "GET", "/api/v1/operations/orders/", 500, chained);
        assertThat(chained[0]).as("GET alias with non-native status chained").isTrue();
        chained = new boolean[1];
        assertThat(errorDispatch(context, "TRACE", "/api/v1/operations/activations", 405, chained)).isEqualTo(404);
        assertThat(chained[0]).isFalse();
        for (String uri : List.of("/api/v1/operations/subjects", "/api/v1/operations/ordersfoo", "/api/v1/customer/orders")) {
            chained = new boolean[1];
            errorDispatch(context, "TRACE", uri, 405, chained);
            assertThat(chained[0]).as("sibling/unrelated/lookalike chained " + uri).isTrue();
        }
    }
    @Test void selectedProductionFiltersMatchIndependentExpressionsCensusRemainderAndHashes() throws Exception {
        operationsGuard(Files.readString(MAIN.resolve("operations_iam/internal/web/OperationsBoundaryFilter.java")).replace("\r\n", "\n"),
                PROBE_REQUIRES_ERROR_DELTA);
        customerGuard(Files.readString(MAIN.resolve("customer_iam/internal/web/CustomerBoundaryFilter.java")).replace("\r\n", "\n"));
    }
    @Test void customerAndErrorFixturesKillAdmissionAndTruncationMutations() throws Exception {
        String customer = Files.readString(MAIN.resolve("customer_iam/internal/web/CustomerBoundaryFilter.java")).replace("\r\n", "\n");
        customerGuard(customer);
        for (String mutant : List.of(customer.replace("if (\"GET\".equals(request.getMethod()) && operationsPath", "if (\"POST\".equals(request.getMethod()) && operationsPath"),
                customer.replace(CUSTOMER_EXPECTED_GET.strip(), CUSTOMER_EXPECTED_GET.strip().replace(
                        "|| operationsPath.matches(\"/api/v1/operations/orders/[^/;%]+\"))",
                        "|| operationsPath.matches(\"/api/v1/operations/orders/[^/;%]+\") || operationsPath.matches(\"/api/v1/operations/orders/extra/[^/;%]+\"))")),
                customer.replace("String operationsPath =", "String unused = \"/api/v1/operations/orders\"; String operationsPath ="),
                customer.replace("problems.notFound(response, traceId)", "problems.notFound(response, UUID.randomUUID())")))
            assertThatThrownBy(() -> customerGuard(mutant)).isInstanceOf(AssertionError.class);
        String truncated = CUSTOMER_EXPECTED_GET.substring(0, CUSTOMER_EXPECTED_GET.indexOf(';'));
        assertThat(truncated).as("truncation precedes both order alternatives").doesNotContain("\"/api/v1/operations/orders\"");
        assertThatThrownBy(() -> tokens(truncated)).as("truncation cuts inside a route literal")
                .isInstanceOf(AssertionError.class).hasMessageContaining("complete Java literal");
        assertThatThrownBy(() -> readSentinels(truncated, "operationsPath")).isInstanceOf(AssertionError.class);
        String errorTruncated = EXPECTED_REQUIRED_ERROR.substring(0, EXPECTED_REQUIRED_ERROR.indexOf(';'));
        assertThatThrownBy(() -> tokens(errorTruncated)).isInstanceOf(AssertionError.class);
        String wrongNoDelta = EXPECTED_NO_ERROR_DELTA.replace("\"/api/v1/operations/activations\").equals(request.getAttribute",
                "\"/api/v1/operations/orders\").equals(request.getAttribute");
        assertThatThrownBy(() -> operationsGuard(wrongNoDelta, false)).isInstanceOf(AssertionError.class);
    }
    @Test void ownerOnlySqlNamedInterfacesAndExactProductionInventoryStayBounded() throws Exception {
        try (var files = Files.walk(MAIN)) {
            assertThat(files.filter(path -> path.getFileName().toString().matches(".*OperationsOrder(Read|Cursor|Issuance).*\\.java"))
                    .map(path -> MAIN.relativize(path).toString().replace('\\', '/')).toList()).containsExactlyInAnyOrderElementsOf(PRODUCTION);
        }
        var sources = new HashMap<String, String>();
        for (String path : PRODUCTION) {
            String source = Files.readString(MAIN.resolve(path));
            sources.put(path, source);
            assertThat(source).doesNotContain("INSERT ", "UPDATE ", "DELETE ", "FOR UPDATE", "FOR SHARE", "SELECT *", "OFFSET",
                    "pg_advisory", "audit_fact", "outbox_event", "java.net", "new Hikari", "entitlement_grant", "payment_order", "payment_event");
            if (path.startsWith("operations_iam/"))
                assertThat(source).doesNotContain("FROM catalog_order.", "FROM entitlement_metering.", "com.onecall.core.customer_iam", "com.onecall.core.catalog_order");
        }
        assertThat(methodNames(com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.OrderReader.class))
                .containsExactlyInAnyOrder("fetchOrders", "fetchOrder");
        assertThat(methodNames(com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.IssuanceReader.class)).containsExactly("fetchIssuances");
        String reader = Files.readString(MAIN.resolve(productionFile("JooqOperationsOrderReader.java")));
        assertThat(reader).contains("new DefaultCustomerOrderSnapshotReader()", "context.scoped(Role.ORDER", "snapshot::text")
                .doesNotContain("@Bean", "CustomerOrderQueryConfiguration");
        assertThat(Files.readString(MAIN.resolve(productionFile("JooqOperationsOrderIssuanceReader.java"))))
                .contains("context.scoped(Role.TRIAL", "SELECT order_id, status");
        assertThat(Files.readString(MAIN.resolve(productionFile("DefaultOperationsOrderReadService.java"))))
                .contains("EMPLOYEE_ROLES = Set.of(\"ADMIN\", \"OPS\")");
        paymentStatusSingleTruth(sources);
        String readerPath = productionFile("JooqOperationsOrderReader.java");
        var reduplicated = new HashMap<>(sources);
        reduplicated.put(readerPath, sources.get(readerPath).replace("OperationsOrderReadPorts.PAYMENT_STATUSES.contains",
                "Set.of(\"UNPAID\", \"UNKNOWN\", \"PAID\", \"CLOSED\").contains"));
        assertThatThrownBy(() -> paymentStatusSingleTruth(reduplicated)).as("consumer literal re-duplication is killed").isInstanceOf(AssertionError.class);
        String portsPath = productionFile("OperationsOrderReadPorts.java");
        var narrowed = new HashMap<>(sources);
        narrowed.put(portsPath, sources.get(portsPath).replace("Set.of(\"UNPAID\", \"UNKNOWN\", \"PAID\", \"CLOSED\")", "Set.of(\"UNKNOWN\", \"PAID\", \"CLOSED\")"));
        assertThatThrownBy(() -> paymentStatusSingleTruth(narrowed)).as("contract domain drift is killed").isInstanceOf(AssertionError.class);
        var moved = new HashMap<>(sources);
        moved.put(readerPath, sources.get(readerPath).replace("OperationsOrderReadPorts.PAYMENT_STATUSES", "PAYMENT_STATUSES"));
        assertThatThrownBy(() -> paymentStatusSingleTruth(moved)).as("unqualified consumer reference is killed").isInstanceOf(AssertionError.class);
        instantRangeSingleTruth(sources);
        String codecPath = productionFile("OperationsOrderCursorCodec.java");
        var reinlined = new HashMap<>(sources);
        reinlined.put(readerPath, sources.get(readerPath).replace(
                "OperationsOrderReadPorts.validInstant(anchor.get().createdAt())",
                "anchor.get().createdAt() != null && anchor.get().createdAt().getNano() % 1000 == 0"
                        + " && !anchor.get().createdAt().isBefore(Instant.parse(\"0001-01-01T00:00:00Z\"))"
                        + " && !anchor.get().createdAt().isAfter(Instant.parse(\"9999-12-31T23:59:59.999999Z\"))"));
        assertThatThrownBy(() -> instantRangeSingleTruth(reinlined)).as("owner instant-range re-duplication is killed").isInstanceOf(AssertionError.class);
        var rangeDrift = new HashMap<>(sources);
        rangeDrift.put(portsPath, sources.get(portsPath).replace("\"9999-12-31T23:59:59.999999Z\"", "\"9999-12-31T23:59:59.999998Z\""));
        assertThatThrownBy(() -> instantRangeSingleTruth(rangeDrift)).as("contract instant-range drift is killed").isInstanceOf(AssertionError.class);
        var precisionDrift = new HashMap<>(sources);
        precisionDrift.put(portsPath, sources.get(portsPath).replace("value.getNano() % 1000 == 0", "value.getNano() % 100 == 0"));
        assertThatThrownBy(() -> instantRangeSingleTruth(precisionDrift)).as("contract microsecond-precision drift is killed").isInstanceOf(AssertionError.class);
        var unqualifiedInstant = new HashMap<>(sources);
        unqualifiedInstant.put(codecPath, sources.get(codecPath).replace("OperationsOrderReadPorts.validInstant", "validInstant"));
        assertThatThrownBy(() -> instantRangeSingleTruth(unqualifiedInstant)).as("unqualified instant-range consumer reference is killed").isInstanceOf(AssertionError.class);
    }
    private static void paymentStatusSingleTruth(Map<String, String> sources) {
        var consumers = List.of("operations_iam/internal/orderread/OperationsOrderReadRequest.java",
                "catalog_order/internal/operationsread/JooqOperationsOrderReader.java",
                "operations_iam/internal/orderread/DefaultOperationsOrderReadService.java");
        int unpaidLiterals = 0;
        for (var entry : sources.entrySet()) {
            String path = entry.getKey();
            String source = entry.getValue();
            int references = countPredicate(source, "PAYMENT_STATUSES");
            if (path.equals(productionFile("OperationsOrderReadPorts.java"))) {
                assertThat(countPredicate(source, "Set<String> PAYMENT_STATUSES = Set.of(\"UNPAID\", \"UNKNOWN\", \"PAID\", \"CLOSED\")"))
                        .as("sole PA43 payment-status truth defined in the operations contract").isEqualTo(1);
                assertThat(references).as("contract declares PAYMENT_STATUSES exactly once").isEqualTo(1);
            } else if (consumers.contains(path)) {
                assertThat(references).as("exactly one payment-status reference in intended consumer " + path).isEqualTo(1);
                assertThat(countPredicate(source, "OperationsOrderReadPorts.PAYMENT_STATUSES"))
                        .as("consumer references the contract truth " + path).isEqualTo(1);
            } else {
                assertThat(references).as("no payment-status redefinition outside contract and intended consumers " + path).isZero();
            }
            unpaidLiterals += (int) tokens(source).stream().filter(token -> token.literal() && token.text().equals("\"UNPAID\"")).count();
        }
        assertThat(unpaidLiterals).as("\"UNPAID\" literal appears only in the contract definition across PA43 production sources").isEqualTo(1);
    }
    private static void instantRangeSingleTruth(Map<String, String> sources) {
        var consumers = Map.of("operations_iam/internal/orderread/OperationsOrderCursorCodec.java", 2,
                "catalog_order/internal/operationsread/JooqOperationsOrderReader.java", 1,
                "operations_iam/internal/orderread/DefaultOperationsOrderReadService.java", 4);
        int lowerLiterals = 0, upperLiterals = 0, microsecondRules = 0;
        for (var entry : sources.entrySet()) {
            String path = entry.getKey();
            String source = entry.getValue();
            int references = countPredicate(source, "validInstant");
            if (path.equals(productionFile("OperationsOrderReadPorts.java"))) {
                assertThat(countPredicate(source, "static boolean validInstant(Instant value)"))
                        .as("sole PA43 API instant-range truth defined in the operations contract").isEqualTo(1);
                assertThat(countPredicate(source, "value != null && value.getNano() % 1000 == 0"))
                        .as("contract microsecond-precision rule pinned").isEqualTo(1);
                assertThat(countPredicate(source, "!value.isBefore(Instant.parse(\"0001-01-01T00:00:00Z\"))"))
                        .as("contract lower instant bound pinned").isEqualTo(1);
                assertThat(countPredicate(source, "!value.isAfter(Instant.parse(\"9999-12-31T23:59:59.999999Z\"))"))
                        .as("contract upper instant bound pinned").isEqualTo(1);
                assertThat(references).as("contract declares validInstant exactly once").isEqualTo(1);
            } else if (consumers.containsKey(path)) {
                assertThat(references).as("exact instant-range reference count in intended consumer " + path).isEqualTo(consumers.get(path));
                assertThat(countPredicate(source, "OperationsOrderReadPorts.validInstant"))
                        .as("consumer references the contract instant-range truth " + path).isEqualTo(consumers.get(path));
            } else {
                assertThat(references).as("no instant-range redefinition outside contract and intended consumers " + path).isZero();
            }
            lowerLiterals += (int) tokens(source).stream().filter(token -> token.literal() && token.text().equals("\"0001-01-01T00:00:00Z\"")).count();
            upperLiterals += (int) tokens(source).stream().filter(token -> token.literal() && token.text().equals("\"9999-12-31T23:59:59.999999Z\"")).count();
            microsecondRules += countPredicate(source, "getNano() % 1000 == 0");
        }
        assertThat(lowerLiterals).as("\"0001-01-01T00:00:00Z\" literal appears only in the contract definition across PA43 production sources").isEqualTo(1);
        assertThat(upperLiterals).as("\"9999-12-31T23:59:59.999999Z\" literal appears only in the contract definition across PA43 production sources").isEqualTo(1);
        assertThat(microsecondRules).as("microsecond-precision rule appears only in the contract definition across PA43 production sources").isEqualTo(1);
    }
    private static List<String> methodNames(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods()).map(java.lang.reflect.Method::getName).toList();
    }
    @Test void frozenOpenApiPropertiesRequiredSetsAndMigrationsRemainExact() throws Exception {
        String openapi = Files.readString(BACKEND.getParent().resolve("api/openapi/onecall-v1.openapi.yaml"));
        for (var entry : Map.of("OrderSummary", com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.OrderSummary.class,
                "Order", com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.Order.class,
                "OrderPage", com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.OrderPage.class,
                "OrderSnapshotProjection", com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.Snapshot.class,
                "CatalogBenefitLine", com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.BenefitLine.class).entrySet()) {
            int start = openapi.indexOf("    " + entry.getKey() + ":");
            assertThat(start).isGreaterThan(0);
            var tail = openapi.substring(start).replace("\r\n", "\n");
            var next = Pattern.compile("(?m)^    \\w+:").matcher(tail);
            assertThat(next.find()).isTrue();
            int end = next.find() ? next.start() : tail.length();
            String schema = tail.substring(0, end);
            var properties = Pattern.compile("(?m)^        (\\w+):$").matcher(schema).results().map(match -> match.group(1)).toList();
            var inlineRequired = Pattern.compile("(?m)^      required: \\[([^\\]]+)\\]$").matcher(schema);
            var required = inlineRequired.find()
                    ? java.util.Arrays.stream(inlineRequired.group(1).split(",")).map(String::trim).toList()
                    : Pattern.compile("(?m)^        - (\\w+)$").matcher(schema).results().map(match -> match.group(1)).toList();
            var components = java.util.Arrays.stream(entry.getValue().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList();
            assertThat(components).containsExactlyInAnyOrderElementsOf(properties);
            assertThat(required).doesNotHaveDuplicates().isSubsetOf(components);
            assertThat(required.size()).isEqualTo(entry.getKey().equals("Order") ? 14 : components.size());
        }
        new CustomerCatalogOfferReadAclBoundaryTests().frozenContractsAndV001ThroughV018RemainByteIdentical();
        new CustomerOrderReadBoundaryTests().v020HasOnlyApprovedGrantActionsAndFailureCategories();
    }

    @Test void independentBaselineAndExpectedVariantsHavePinnedIdentities() throws Exception {
        assertThat(hash(EXPECTED_PREFIX + BASELINE_ERROR + EXPECTED_BETWEEN + BASELINE_READ + EXPECTED_SUFFIX))
                .isEqualTo("2eadb3c7bf7118408fb84defba1508b0ef7d10be4e8242c7210e70c01cf8d507");
        assertThat(hash(EXPECTED_NO_ERROR_DELTA)).isEqualTo(EXPECTED_NO_ERROR_DELTA_SHA256);
        assertThat(hash(EXPECTED_PROBE_REQUIRED_ERROR_DELTA)).isEqualTo(EXPECTED_PROBE_REQUIRED_ERROR_DELTA_SHA256);
    }
    private static String hash(String source) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
    }
}
