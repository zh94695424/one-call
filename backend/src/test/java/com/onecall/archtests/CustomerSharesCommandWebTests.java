package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.security.CustomerBearerAuthenticator;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.sharing.internal.application.*;
import com.onecall.core.sharing.internal.command.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class CustomerSharesCommandWebTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EVIDENCE = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final String PATH = "/api/v1/customer/shares";
    private static final String BODY = "{\"creationRequestId\":\" raw 😀 \",\"evidenceId\":\"" + EVIDENCE
            + "\",\"verificationMode\":\"NONE\",\"expiresAt\":\"2026-11-05T12:00:00.123456+02:00\",\"downloadAllowed\":false}";
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void createUsesTrustedCallerAndMapsCreatedAndReplay() throws Exception {
        Fixture fixture = new Fixture(true);
        for (String mode : List.of("NONE", "EXTRACTION_CODE", "DESIGNATED_MOBILE", "ORGANIZATION_MEMBER")) {
            fixture.organization = mode.equals("ORGANIZATION_MEMBER") ? Optional.of(UUID.randomUUID()) : Optional.empty();
            var state = state(mode);
            when(fixture.commands.execute(any(), any())).thenReturn(new CustomerShareCommandResult.Created(state, "one-time-token",
                    mode.equals("EXTRACTION_CODE") ? Optional.of("one-time-code") : Optional.empty()));
            String body = BODY.replace("\"NONE\"", "\"" + mode + "\"");
            if (mode.equals("DESIGNATED_MOBILE")) body = body.substring(0, body.length() - 1) + ",\"designatedMobile\":\"13800138000\"}";
            var created = fixture.mvc.perform(post(PATH).servletPath(PATH).header("Authorization", "Bearer test")
                    .header("Idempotency-Key", " ", "x".repeat(200)).contentType("application/json; charset=UTF-8").content(body)).andReturn().getResponse();
            assertThat(created.getStatus()).isEqualTo(201);
            var json = mapper.readTree(created.getContentAsString());
            assertThat(json.path("capabilityToken").asString()).isEqualTo("one-time-token");
            assertThat(json.has("extractionCode")).isEqualTo(mode.equals("EXTRACTION_CODE"));
            assertThat(json.path("expiresAt").asString()).isEqualTo("2026-11-05T10:00:00.123456Z");
            assertThat(json.has("maxAdmissions") && json.path("maxAdmissions").isNull()).isTrue();
            assertThat(json.has("state") || json.has("result") || json.has("traceId")).isFalse();
            var fields = new java.util.ArrayList<>(List.of("shareId", "evidenceId", "verificationMode", "status", "validFrom", "expiresAt", "maxAdmissions",
                    "admissionsUsed", "downloadAllowed", "codeGeneration", "revocationGeneration", "capabilityToken"));
            if (mode.equals("EXTRACTION_CODE")) fields.add("extractionCode");
            assertThat(json.properties().stream().map(java.util.Map.Entry::getKey)).containsExactlyInAnyOrderElementsOf(fields);
            assertThat(created.getHeader("Cache-Control")).isEqualTo("no-store");
            var invocation = mockingDetails(fixture.commands).getInvocations().stream().reduce((first, last) -> last).orElseThrow();
            var command = (CustomerShareCommand.Create) invocation.getArgument(0);
            assertThat(command.creationRequestId()).isEqualTo(" raw 😀 ");
            assertThat(command.traceId()).isEqualTo(Optional.of(fixture.trace.get()));
            assertThat(command.expiresAt()).isEqualTo(Instant.parse("2026-11-05T10:00:00.123456Z"));
            assertThat(invocation.getArgument(1, ShareCommandCaller.class)).isEqualTo(new ShareCommandCaller(ACCOUNT, fixture.organization));
            when(fixture.commands.execute(any(), any())).thenReturn(new CustomerShareCommandResult.CreationReplayed(state));
            var replay = fixture.mvc.perform(post(PATH).servletPath(PATH).header("Authorization", "Bearer test")
                    .contentType("application/json").content(body)).andReturn().getResponse();
            assertThat(replay.getStatus()).isEqualTo(200);
            assertThat(replay.getContentAsString()).doesNotContain("capabilityToken", "extractionCode", "result", "state", "one-time");
        }
    }

    @Test
    void revokeAndRotateMapClosedResponseUnions() throws Exception {
        CustomerProblemWriter writer = new CustomerProblemWriter();
        Map<ShareCommandException.Code, List<Object>> rows = Map.of(
                ShareCommandException.Code.SHARE_RISK_BLOCKED, List.of(403, "Share risk blocked"),
                ShareCommandException.Code.SHARE_ACCESS_POLICY_UNAVAILABLE, List.of(403, "Sharing policy unavailable"),
                ShareCommandException.Code.SHARE_CREATION_CONFLICT, List.of(409, "Share creation conflict"),
                ShareCommandException.Code.SHARE_TRANSITION_CONFLICT, List.of(409, "Share transition conflict"),
                ShareCommandException.Code.SHARE_GENERATION_STALE, List.of(409, "Share generation stale"),
                ShareCommandException.Code.SHARE_REVOKED, List.of(410, "Share revoked"));
        UUID trace = UUID.randomUUID();
        for (var row : rows.entrySet()) {
            var response = new MockHttpServletResponse();
            writer.write(response, row.getKey(), trace);
            var json = mapper.readTree(response.getContentAsString());
            assertThat(response.getStatus()).isEqualTo(row.getValue().getFirst());
            assertThat(json.path("title").asString()).isEqualTo(row.getValue().get(1));
            assertThat(json.path("traceId").asString()).isEqualTo(trace.toString());
            assertThat(json.path("code").asString()).isEqualTo(row.getKey().name());
            assertThat(json.size()).isEqualTo(5);
        }
        Fixture fixture = new Fixture(true);
        for (boolean organization : List.of(false, true)) {
            fixture.organization = organization ? Optional.of(UUID.randomUUID()) : Optional.empty();
            var active = state("EXTRACTION_CODE");
            var revoked = new CustomerShareCommandResult.ShareState(active.shareId(), active.evidenceId(), active.verificationMode(),
                    ShareSummary.ShareStatus.REVOKED, active.validFrom(), active.expiresAt(), active.maxAdmissions(),
                    active.admissionsUsed(), active.downloadAllowed(), active.codeGeneration(), 1);
            when(fixture.commands.execute(any(), any())).thenReturn(new CustomerShareCommandResult.Revoked(revoked));
            String first = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                var response = fixture.mvc.perform(fixture.revoke().header("Idempotency-Key", "revoke,raw-key")
                        .contentType("application/json").content("{\"expectedRevocationGeneration\":0,\"reason\":\" raw reason \"}"))
                        .andReturn().getResponse();
                assertThat(response.getStatus()).isEqualTo(200);
                var json = mapper.readTree(response.getContentAsString());
                assertThat(json.properties().stream().map(Map.Entry::getKey)).containsExactlyInAnyOrder("shareId", "evidenceId",
                        "verificationMode", "status", "validFrom", "expiresAt", "maxAdmissions", "admissionsUsed", "downloadAllowed",
                        "codeGeneration", "revocationGeneration");
                assertThat(json.path("status").asString()).isEqualTo("REVOKED");
                assertThat(json.path("maxAdmissions").isNull()).isTrue();
                assertThat(json.path("expiresAt").asString()).isEqualTo("2026-11-05T10:00:00.123456Z");
                assertThat(response.getContentAsString()).doesNotContain("revoke,raw-key", "raw reason", "token", "hash", "salt");
                assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
                if (first == null) first = response.getContentAsString();
                else assertThat(response.getContentAsString()).isEqualTo(first);
                var invocation = mockingDetails(fixture.commands).getInvocations().stream().reduce((left, right) -> right).orElseThrow();
                var command = invocation.getArgument(0, CustomerShareCommand.Revoke.class);
                assertThat(command.actionRequestId()).isEqualTo("revoke,raw-key");
                assertThat(command.shareId()).isEqualTo("share:fixture");
                assertThat(command.reason()).isEqualTo(" raw reason ");
                assertThat(command.expectedRevocationGeneration()).isZero();
                assertThat(command.traceId()).contains(fixture.trace.get());
                assertThat(invocation.getArgument(1, ShareCommandCaller.class)).isEqualTo(new ShareCommandCaller(ACCOUNT, fixture.organization));
            }
        }
        for (var row : Map.of(ShareCommandException.Code.UNAUTHENTICATED, 401, ShareCommandException.Code.FORBIDDEN, 403,
                ShareCommandException.Code.NOT_FOUND, 404, ShareCommandException.Code.VALIDATION_FAILED, 422,
                ShareCommandException.Code.SHARE_TRANSITION_CONFLICT, 409, ShareCommandException.Code.SHARE_GENERATION_STALE, 409,
                ShareCommandException.Code.SHARE_REVOKED, 410, ShareCommandException.Code.RATE_LIMITED, 429,
                ShareCommandException.Code.SERVICE_UNAVAILABLE, 503, ShareCommandException.Code.INTERNAL, 500).entrySet()) {
            doThrow(new ShareCommandException(row.getKey())).when(fixture.commands).execute(any(), any());
            problem(fixture.mvc.perform(fixture.revoke().header("Idempotency-Key", "revoke,raw-key").contentType("application/json")
                    .content("{}")).andReturn().getResponse(), row.getValue(), row.getKey().name());
        }
        verifyNoInteractions(fixture.query);
        Fixture rotate = new Fixture(true);
        for (String suffix : List.of("code-rotations")) {
            String path = PATH + "/share:fixture/" + suffix;
            var response = rotate.mvc.perform(post(path).servletPath(path).contentType("application/json").content("{}"))
                    .andReturn().getResponse();
            problem(response, 404, "NOT_FOUND");
        }
        verifyNoInteractions(rotate.commands);
    }

    @Test
    void invalidCommandAndMissingServiceMapToProblemResponses() throws Exception {
        Fixture fixture = new Fixture(true);
        for (String body : List.of("", "null", "[]", "true", "{", BODY + " {}", BODY.replace("\"downloadAllowed\":false", "\"downloadAllowed\":0"),
                BODY.replace("\"downloadAllowed\":false", "\"downloadAllowed\":null"), BODY.replace(",\"downloadAllowed\":false", ""),
                BODY.replace(" raw 😀 ", " "), BODY.replace(" raw 😀 ", "😀".repeat(129)), BODY.replace(EVIDENCE.toString(), "1-1-1-1-1"),
                BODY.replace("\"NONE\"", "null"), BODY.replace("\"NONE\"", "\"unknown\""), BODY.replace(".123456+02:00", ".1234560Z"),
                BODY.replace(".123456+02:00", ".123456000Z"), BODY.replace(".123456+02:00", ""),
                BODY.replace("false}", "false,\"maxAdmissions\":0}"), BODY.replace("false}", "false,\"maxAdmissions\":2147483648}"),
                BODY.replace("false}", "false,\"maxAdmissions\":1.0}"), BODY.replace("false}", "false,\"traceId\":\"client\"}"),
                BODY.replace("false}", "false,\"creationRequestId\":\"duplicate\"}"), BODY.replace("false}", "false,\"designatedMobile\":null}"),
                BODY.replace("\"NONE\"", "\"DESIGNATED_MOBILE\""))) {
            problem(fixture.mvc.perform(fixture.post().contentType("application/json").content(body)).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        }
        for (String type : List.of("ABSENT", "text/plain", "application/*", "application/*+json", "application/problem+json", "broken;")) {
            var request = fixture.post().content(BODY);
            if (!type.equals("ABSENT")) request.header("Content-Type", type);
            problem(fixture.mvc.perform(request).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        }
        verifyNoInteractions(fixture.commands);
        for (String[] keys : List.of(new String[] {}, new String[] {""}, new String[] {" "}, new String[] {"😀".repeat(129)},
                new String[] {"same", "same"}, new String[] {"first", "second"}, new String[] {"bad\0"})) {
            var request = fixture.revoke().header("Content-Type", "text/plain").content("{");
            if (keys.length > 0) request.header("Idempotency-Key", (Object[]) keys);
            problem(fixture.mvc.perform(request).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        }
        for (String body : List.of("", "null", "[]", "true", "{", "{} {}", "{\"reason\":null}", "{\"reason\":\" \"}",
                "{\"reason\":1}", "{\"reason\":\"" + "😀".repeat(129) + "\"}", "{\"reason\":\"x\",\"reason\":\"x\"}",
                "{\"reason\":\"\\u0000\"}", "{\"reason\":\"\\uD800\"}", "{\"expectedRevocationGeneration\":null}",
                "{\"expectedRevocationGeneration\":-1}", "{\"expectedRevocationGeneration\":1.0}", "{\"expectedRevocationGeneration\":2147483648}",
                "{\"expectedRevocationGeneration\":\"0\"}", "{\"expectedRevocationGeneration\":true}", "{\"expectedCodeGeneration\":0}",
                "{\"actionRequestId\":\"client\"}", "{\"traceId\":\"client\"}", "{\"organizationId\":\"client\"}")) {
            problem(fixture.mvc.perform(fixture.revoke().header("Idempotency-Key", "valid").contentType("application/json").content(body))
                    .andReturn().getResponse(), 422, "VALIDATION_FAILED");
        }
        problem(fixture.mvc.perform(fixture.revoke().header("Idempotency-Key", "valid").contentType("application/json")
                .content(new byte[] {(byte) 0xff})).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        for (String type : List.of("ABSENT", "text/plain", "application/*", "application/*+json", "application/problem+json", "broken;")) {
            var request = fixture.revoke().header("Idempotency-Key", "valid").content("{}");
            if (!type.equals("ABSENT")) request.header("Content-Type", type);
            problem(fixture.mvc.perform(request).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        }
        String overlong = PATH + "/" + "x".repeat(129) + "/revocations";
        problem(fixture.mvc.perform(post(overlong).servletPath(overlong).header("Authorization", "Bearer test")
                .header("Idempotency-Key", "valid").contentType("application/json").content("{}"))
                .andReturn().getResponse(), 422, "VALIDATION_FAILED");
        problem(fixture.mvc.perform(post(PATH + "/share:fixture/revocations").servletPath(PATH + "/share:fixture/revocations")
                .content("{")).andReturn().getResponse(), 401, "UNAUTHENTICATED");
        verifyNoInteractions(fixture.commands, fixture.query);
        assertThat(fixture.commandResolutions).hasValue(0);
        Fixture malformedWorkspace = new Fixture(true);
        for (var selected : List.of(new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, UUID.randomUUID()),
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, null))) {
            malformedWorkspace.workspace = selected;
            problem(malformedWorkspace.mvc.perform(malformedWorkspace.revoke().contentType("text/plain").content("{"))
                    .andReturn().getResponse(), 422, "VALIDATION_FAILED");
            problem(malformedWorkspace.mvc.perform(malformedWorkspace.revoke().header("Idempotency-Key", "valid")
                    .contentType("text/plain").content("{")).andReturn().getResponse(), 422, "VALIDATION_FAILED");
            problem(malformedWorkspace.mvc.perform(malformedWorkspace.revoke().header("Idempotency-Key", "valid")
                    .contentType("application/json").content("{")).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        }
        assertThat(malformedWorkspace.commandResolutions).hasValue(0);
        verifyNoInteractions(malformedWorkspace.commands, malformedWorkspace.query);
        Fixture missing = new Fixture(false);
        problem(missing.mvc.perform(missing.revoke().header("Idempotency-Key", "valid").contentType("application/json").content("{}"))
                .andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        for (String key : List.of("😀".repeat(128), " raw ключ 😀 ", "revoke,raw-key")) {
            when(fixture.commands.execute(any(), any())).thenReturn(new CustomerShareCommandResult.Revoked(state("NONE")));
            assertThat(fixture.mvc.perform(fixture.revoke().header("iDeMpOtEnCy-KeY", key).contentType("application/json")
                    .content("{\"expectedRevocationGeneration\":0,\"reason\":\"" + "😀".repeat(128) + "\"}"))
                    .andReturn().getResponse().getStatus()).isEqualTo(200);
            var invocation = mockingDetails(fixture.commands).getInvocations().stream().reduce((left, right) -> right).orElseThrow();
            assertThat(invocation.getArgument(0, CustomerShareCommand.Revoke.class).actionRequestId()).isEqualTo(key);
        }
        problem(missing.mvc.perform(missing.post().contentType("application/json").content(BODY)).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        for (var code : List.of(ShareCommandException.Code.INTERNAL, ShareCommandException.Code.RATE_LIMITED,
                ShareCommandException.Code.VALIDATION_FAILED, ShareCommandException.Code.SHARE_RISK_BLOCKED,
                ShareCommandException.Code.SHARE_ACCESS_POLICY_UNAVAILABLE, ShareCommandException.Code.SHARE_CREATION_CONFLICT)) {
            doThrow(new ShareCommandException(code)).when(fixture.commands).execute(any(), any());
            int status = switch (code) { case INTERNAL -> 500; case RATE_LIMITED -> 429; case VALIDATION_FAILED -> 422;
                case SHARE_CREATION_CONFLICT -> 409; default -> 403; };
            problem(fixture.mvc.perform(fixture.post().contentType("application/json").content(BODY)).andReturn().getResponse(), status, code.name());
        }
    }

    @Test
    void commandRoutesDoNotChangeShareListRoute() throws Exception {
        Fixture fixture = new Fixture(true);
        when(fixture.query.listCustomerShares(any(), any())).thenReturn(new SharePage(List.of(), Optional.empty(), false));
        var response = fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(fixture.commands);
        when(fixture.query.listCustomerShares(any(), any())).thenThrow(new ShareQueryException(ShareQueryException.Code.CURSOR_INVALID));
        problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(), 400, "CURSOR_INVALID");
        for (String method : List.of("HEAD", "OPTIONS", "PUT", "PATCH", "DELETE"))
            problem(fixture.mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), PATH).servletPath(PATH)).andReturn().getResponse(), 404, "NOT_FOUND");
    }

    private void problem(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        var json = mapper.readTree(response.getContentAsString());
        assertThat(json.path("code").asString()).isEqualTo(code);
        assertThat(json.path("type").asString()).isEqualTo("about:blank");
        assertThat(json.path("status").asInt()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        if (status == 401) assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(json.size()).isEqualTo(5);
        assertThatCode(() -> UUID.fromString(json.path("traceId").asString())).doesNotThrowAnyException();
        assertThat(response.getContentAsString()).doesNotContain("Exception", "jdbc:", "one-time", "raw 😀", "revoke,raw-key", "salt", "codeHash", "capabilityToken");
    }

    private static CustomerShareCommandResult.ShareState state(String mode) {
        return new CustomerShareCommandResult.ShareState("share:fixture", EVIDENCE, ShareSummary.VerificationMode.valueOf(mode),
                ShareSummary.ShareStatus.ACTIVE, Instant.parse("2026-11-04T10:00:00Z"), Instant.parse("2026-11-05T10:00:00.123456Z"),
                null, 0, false, mode.equals("EXTRACTION_CODE") ? 1 : 0, 0);
    }

    static final class Fixture {
        final CustomerShareCommandService commands = mock(CustomerShareCommandService.class);
        final CustomerShareQueryService query = mock(CustomerShareQueryService.class);
        final java.util.concurrent.atomic.AtomicReference<UUID> trace = new java.util.concurrent.atomic.AtomicReference<>();
        Optional<UUID> organization = Optional.empty();
        SelectedCustomerWorkspace workspace;
        final java.util.concurrent.atomic.AtomicInteger commandResolutions = new java.util.concurrent.atomic.AtomicInteger();
        final MockMvc mvc;

        Fixture(boolean commandPresent) {
            var beans = new DefaultListableBeanFactory();
            beans.registerSingleton("customerShareQueryService", query);
            if (commandPresent) beans.registerSingleton("customerShareCommandService", commands);
            beans.registerSingleton("verifier", (CustomerSessionVerifier) (bearer, audience) -> new VerifiedCustomerSession(ACCOUNT,
                    TrustedCustomerCaller.Channel.WEB, audience, workspace != null ? workspace : new SelectedCustomerWorkspace(organization.isPresent()
                            ? CustomerContext.WorkspaceType.ORGANIZATION : CustomerContext.WorkspaceType.PERSONAL, organization.orElse(null))));
            CustomerProblemWriter problems = new CustomerProblemWriter();
            var commandProvider = spy(beans.getBeanProvider(CustomerShareCommandService.class));
            doAnswer(invocation -> {
                commandResolutions.incrementAndGet();
                return invocation.callRealMethod();
            }).when(commandProvider).getObject();
            var controller = new CustomerSharesController(beans.getBeanProvider(CustomerShareQueryService.class),
                    commandProvider, problems);
            mvc = MockMvcBuilders.standaloneSetup(controller).addFilters(new CustomerBoundaryFilter(
                    new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), problems),
                    (jakarta.servlet.Filter) (request, response, chain) -> {
                        trace.set((UUID) request.getAttribute(CustomerBoundaryFilter.class.getName() + ".traceId"));
                        chain.doFilter(request, response);
                    }).build();
        }

        MockHttpServletRequestBuilder post() { return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(PATH)
                .servletPath(PATH).header("Authorization", "Bearer test"); }

        MockHttpServletRequestBuilder revoke() {
            String path = PATH + "/share:fixture/revocations";
            return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).servletPath(path).header("Authorization", "Bearer test");
        }
    }
}
