package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.OperationsActivationReadFixture.*;
import com.onecall.core.operations_iam.internal.activationread.*;
import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;

class OperationsActivationReadsIT {
    static final OperationsActivationReadFixture fixture = new OperationsActivationReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @Test
    void postgres16UsesExactlyFrozenSeventeenMigrations() throws Exception {
        try (var connection = fixture.admin()) { assertThat(Integer.parseInt(one(connection, "SHOW server_version_num"))).isBetween(160000, 169999); assertThat(one(connection, "SHOW server_encoding")).isEqualTo("UTF8"); assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL")).isEqualTo("17"); }
        var manifest = java.nio.file.Files.readString(java.nio.file.Path.of("../docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md"));
        var entries = java.util.regex.Pattern.compile("(?m)^([0-9a-f]{64})  (.+)$").matcher(manifest).results().toList(); assertThat(entries).hasSize(21);
        var hashes = Map.ofEntries(
            Map.entry("api/openapi/onecall-v1.openapi.yaml", new String[] {"0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68", "ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30"}),
            Map.entry("docs/14-production-data-model-and-flyway-schema-spec.md", new String[] {"0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31", "bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58"}),
            Map.entry("docs/15-production-openapi-surface-spec.md", new String[] {"db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0", "0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02"}),
            Map.entry("docs/17-production-security-design-spec.md", new String[] {"7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e", "8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f"}),
            Map.entry("backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql", new String[] {"96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"}),
            Map.entry("backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql", new String[] {"4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"}),
            Map.entry("backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql", new String[] {"4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"}),
            Map.entry("backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql", new String[] {"8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"}),
            Map.entry("backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql", new String[] {"6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"}),
            Map.entry("backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql", new String[] {"7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"}),
            Map.entry("backend/src/main/resources/db/migration/V007__activation_and_consent.sql", new String[] {"bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"}),
            Map.entry("backend/src/main/resources/db/migration/V008__evidence_media.sql", new String[] {"32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"}),
            Map.entry("backend/src/main/resources/db/migration/V009__sharing.sql", new String[] {"3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"}),
            Map.entry("backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql", new String[] {"d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"}),
            Map.entry("backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql", new String[] {"ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b"}),
            Map.entry("backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql", new String[] {"ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6", "13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828"}),
            Map.entry("backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql", new String[] {"1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e", "691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6"}),
            Map.entry("backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql", new String[] {"bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8", "e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a"}),
            Map.entry("backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql", new String[] {"c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc", "1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3"}),
            Map.entry("backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql", new String[] {"a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c", "988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f"}),
            Map.entry("backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql", new String[] {"74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3", "57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad"}));
        assertThat(hashes).hasSize(21);
        assertThat(entries.stream().map(entry -> entry.group(2))).containsExactlyInAnyOrderElementsOf(hashes.keySet());
        for (var entry : entries) {
            assertThat(entry.group(1)).isEqualTo(hashes.get(entry.group(2))[0]);
            assertThat(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("..").resolve(entry.group(2)))))).as(entry.group(2)).isIn((Object[]) hashes.get(entry.group(2)));
        }
        try (var files = java.nio.file.Files.list(java.nio.file.Path.of("src/main/resources/db/migration"))) {
            assertThat(files.map(path -> path.getFileName().toString()).toList()).hasSize(17).containsExactlyInAnyOrderElementsOf(hashes.keySet().stream().filter(path -> path.contains("/migration/")).map(path -> java.nio.file.Path.of(path).getFileName().toString()).toList());
        }
    }
    @Test
    void realHttpActiveAdminAndOpsReturnFrozenProjection() throws Exception {
        UUID identifier = UUID.fromString("70000000-0000-0000-0000-000000000034");
        fixture.transition(identifier, fixture.time.minusSeconds(34), "INACTIVE", "IVR", "PROJECTION_REASON_34");
        UUID subject;
        try (var connection = fixture.admin(); var statement = connection.prepareStatement("SELECT subject_id FROM activation_consent.service_activation_transition WHERE id=?")) {
            statement.setObject(1, identifier);
            try (var rows = statement.executeQuery()) { assertThat(rows.next()).isTrue(); subject = rows.getObject("subject_id", UUID.class); assertThat(rows.next()).isFalse(); }
        }
        var facts = new TransitionFacts(identifier, subject, "event-" + identifier, "INACTIVE", "ACTIVE", 1, 2, "IVR", "IVR_CONFIRM", "PROJECTION_REASON_34", fixture.time.minusSeconds(34));
        assertThat(facts.subjectId()).isNotEqualTo(facts.transitionId());
        for (String token : List.of("admin", "ops")) {
            var response = fixture.request("GET", BASE, token); assertThat(response.statusCode()).isEqualTo(200); var body = json(response); assertThat(body.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore"); var item = body.path("items").get(0);
            assertThat(item.propertyNames()).containsExactlyInAnyOrder("transitionId", "subjectId", "statusEventId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt");
            assertThat(body.path("items").size()).isEqualTo(1); assertThat(body.path("nextCursor").isNull()).isTrue(); assertThat(body.path("hasMore").asBoolean()).isFalse();
            assertThat(item.path("transitionId").asString()).isEqualTo(facts.transitionId().toString());
            assertThat(item.path("subjectId").asString()).isEqualTo(facts.subjectId().toString());
            assertThat(item.path("statusEventId").asString()).isEqualTo(facts.statusEventId());
            assertThat(item.path("fromStatus").asString()).isEqualTo(facts.fromStatus());
            assertThat(item.path("toStatus").asString()).isEqualTo(facts.toStatus());
            assertThat(item.path("fromRevision").isIntegralNumber()).isTrue(); assertThat(item.path("fromRevision").asInt()).isEqualTo(facts.fromRevision());
            assertThat(item.path("toRevision").isIntegralNumber()).isTrue(); assertThat(item.path("toRevision").asInt()).isEqualTo(facts.toRevision());
            assertThat(item.path("sourceChannel").asString()).isEqualTo(facts.sourceChannel());
            assertThat(item.path("sourceActionDomain").asString()).isEqualTo(facts.sourceActionDomain());
            assertThat(item.path("reasonCode").asString()).isEqualTo(facts.reasonCode());
            assertThat(item.path("occurredAt").asString()).isEqualTo(facts.occurredAt().toString());
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store"); assertThat(response.body()).doesNotContain("PRIVATE", fixture.employee.toString(), fixture.password);
        }
    }
    @Test
    void realHttpAuthLifecycleAndAudiencePrecedenceArePreserved() throws Exception {
        for (String token : Arrays.asList(null, "customer", "wrong", "missing", "disabled")) problem(fixture.request("GET", BASE + "?unknown=x", token, "text/html"), "disabled".equals(token) ? 403 : 401, "disabled".equals(token) ? "FORBIDDEN" : "UNAUTHENTICATED");
        fixture.replaceCursor(null); try { problem(fixture.request("GET", BASE + "?limit=0", "disabled"), 403, "FORBIDDEN"); } finally { fixture.replaceCursor(com.onecall.archtests.operations.OperationsActivationReadTestProviders.properties().cursor()); }
    }
    @Test
    void realHttpMethodsAndCanonicalizationVariantsStayConcealed() throws Exception {
        fixture.transition(1);
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE")) assertThat(fixture.request(method, BASE, "admin").statusCode()).as(method).isEqualTo(404);
        for (String path : List.of(BASE + "/", BASE + ";x", BASE + "/x", "/api//v1/operations/activations", "/api/v1//operations/activations", "/api/v1/operations//activations", "/api/v1/operations/%61ctivations", BASE + "%2f", BASE + "%2F", BASE + "%3b", BASE + "%25", "/%61pi/v1/operations/activations")) {
            var response = fixture.request("GET", path, "admin");
            assertThat(response.statusCode()).as(path).isEqualTo(404); assertThat(response.body()).doesNotContain("transitionId", "items", "event-"); assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty(); problem(response, 404, "NOT_FOUND");
        }
        assertThat(fixture.context.getEnvironment().getProperty("server.error.path")).isEqualTo("/internal/pa34-error");
        assertThat(fixture.context.getBean(org.springframework.boot.autoconfigure.web.WebProperties.class).getError().getPath()).isEqualTo("/internal/pa34-error");
        for (String path : List.of("/api/v1/customer/me", "/api/v1/operations/subjects", BASE + "/")) problem(fixture.request("TRACE", path, "admin"), 500, "INTERNAL");
    }
    @Test
    void realHttpParametersDefaultsDuplicatesAndBoundsAreExact() throws Exception {
        seed(22); assertThat(fixture.get(BASE).path("items").size()).isEqualTo(20);
        for (int limit : List.of(1, 100)) assertThat(fixture.get(BASE + "?limit=" + limit).path("items").size()).isEqualTo(Math.min(limit, 22));
        for (String query : List.of("limit=0", "limit=101", "limit=-1", "limit=%2B1", "limit=1.0", "limit=1&limit=1", "cursor=a&cursor=a", "subjectId=x", "source=WEB", "offset=1", "total=true")) problem(fixture.request("GET", BASE + "?" + query, "admin"), 422, "VALIDATION_FAILED");
        for (String cursor : List.of("", "bad", "x".repeat(513))) problem(fixture.request("GET", BASE + "?cursor=" + cursor, "admin"), 400, "CURSOR_INVALID");
    }
    @Test
    void emptySingleAndExactLimitPagesHaveNullCursor() throws Exception {
        for (int count : List.of(0, 1, 20)) { fixture.reset(); seed(count); var page = fixture.get(BASE); assertThat(page.path("items").size()).isEqualTo(count); assertThat(page.path("nextCursor").isNull()).isTrue(); assertThat(page.path("hasMore").asBoolean()).isFalse(); }
    }
    @Test
    void lookaheadAndTailAnchorOnlyLastReturnedItem() throws Exception {
        seed(3); var first = fixture.get(BASE + "?limit=2"); String token = first.path("nextCursor").asString(); var anchor = new OperationsActivationCursorCodec(fixture.context.getBean(OperationsSubjectReadProperties.class)).decode(token); assertThat(anchor.transitionId()).isEqualTo(new UUID(0, 2));
        var tail = fixture.get(BASE + "?limit=2&cursor=" + token); assertThat(tail.path("items").size()).isEqualTo(1); assertThat(tail.path("items").get(0).path("transitionId").asString()).isEqualTo(new UUID(0, 3).toString()); assertThat(tail.path("nextCursor").isNull()).isTrue();
        String after = new OperationsActivationCursorCodec(fixture.context.getBean(OperationsSubjectReadProperties.class)).encode(new Anchor(fixture.time.minusSeconds(30), UUID.randomUUID())); assertThat(fixture.get(BASE + "?cursor=" + after).path("items").size()).isZero();
    }
    @Test
    void timestampTiesAndHighBitUuidsContinueWithoutDuplicates() throws Exception {
        var identifiers = List.of(new UUID(-1, -1), new UUID(Long.MIN_VALUE, 0), new UUID(Long.MAX_VALUE, -1), new UUID(0, Long.MIN_VALUE), new UUID(0, Long.MAX_VALUE));
        for (UUID identifier : identifiers) fixture.transition(identifier, fixture.time, "INACTIVE", "WEB", "ACTIVATED");
        var actual = new ArrayList<String>(); String path = BASE + "?limit=1";
        while (true) { var page = fixture.get(path); actual.add(page.path("items").get(0).path("transitionId").asString()); if (!page.path("hasMore").asBoolean()) break; path = BASE + "?limit=1&cursor=" + page.path("nextCursor").asString(); }
        assertThat(actual).containsExactlyElementsOf(identifiers.stream().map(UUID::toString).toList());
    }
    @Test
    void changingLimitReusesCursorWithoutRestartOrMismatch() throws Exception {
        seed(23); var first = fixture.get(BASE + "?limit=1"); String cursor = first.path("nextCursor").asString();
        for (int limit : List.of(1, 20, 100)) { var page = fixture.get(BASE + "?limit=" + limit + "&cursor=" + cursor); assertThat(page.path("items").size()).isEqualTo(Math.min(limit, 22)); assertThat(page.path("items").get(0).path("transitionId").asString()).isEqualTo(new UUID(0, 2).toString()); }
    }
    @Test
    void realHttpMalformedTamperedWrongPurposeAndWrongKeyCursorsFail() throws Exception {
        seed(2); String token = fixture.get(BASE + "?limit=1").path("nextCursor").asString(); var properties = fixture.context.getBean(OperationsSubjectReadProperties.class);
        String subject = new OperationsSubjectCursorCodec(properties).encode(new OperationsSubjectReadPorts.Anchor(fixture.time, fixture.employee), new byte[32]);
        byte[] frame = Base64.getUrlDecoder().decode(token); frame[frame.length - 1] ^= 1; String tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(frame);
        byte[] wrongBytes = new byte[32]; Arrays.fill(wrongBytes, (byte) 9); var key = new OperationsSubjectReadProperties.KeyEntry("Test-34", Base64.getUrlEncoder().withoutPadding().encodeToString(wrongBytes)); String wrong = new OperationsActivationCursorCodec(new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(key.id(), List.of(key)))).encode(new Anchor(fixture.time, fixture.employee));
        for (String invalid : List.of("bad", token + "=", tampered, subject, wrong)) problem(fixture.request("GET", BASE + "?cursor=" + invalid, "admin"), 400, "CURSOR_INVALID");
        fixture.replaceCursor(null); try { problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); fixture.reset(); problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { fixture.replaceCursor(properties.cursor()); }
    }
    @Test
    void allNinePersistableTransitionCombinationsReturnAllEnums() throws Exception {
        assertThatThrownBy(() -> fixture.transition(UUID.randomUUID(), fixture.time, "INACTIVE", "WEB", "   "))
                .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("23514"));
        var expected = new LinkedHashMap<UUID, TransitionFacts>();
        for (String shape : List.of("INACTIVE:WEB:ACTIVATION", "INACTIVE:MINIAPP:ACTIVATION", "INACTIVE:IVR:IVR_CONFIRM", "ACTIVE:OPERATIONS:OPERATIONS", "SUSPENDED:OPERATIONS:OPERATIONS")) {
            int sequence = expected.size() + 1; String[] parts = shape.split(":");
            UUID identifier = new UUID(7, sequence), subject = fixture.subject(), activation = UUID.randomUUID();
            String reason = sequence == 1 ? "\t" : sequence == 5 ? "😀".repeat(64) : "REASON_" + sequence;
            var facts = new TransitionFacts(identifier, subject, "status-event-" + sequence, parts[0], "ACTIVE".equals(parts[0]) ? "SUSPENDED" : "ACTIVE", sequence * 10, sequence * 10 + 1, parts[1], parts[2], reason, fixture.time.minusSeconds(sequence));
            UUID consent = "OPERATIONS".equals(facts.sourceChannel()) ? null : UUID.randomUUID(), account = consent == null ? null : UUID.randomUUID();
            try (var connection = fixture.admin()) {
                connection.setAutoCommit(false);
                execute(connection, "INSERT INTO activation_consent.service_activation(id,subject_id,status,revision) VALUES (?,?,?,?)", activation, subject, facts.toStatus(), facts.toRevision());
                if (consent != null) {
                    execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'PRIVATE_ACCOUNT')", account); UUID session = null;
                    if ("IVR".equals(facts.sourceChannel())) { session = UUID.randomUUID(); execute(connection, "INSERT INTO ivr.inbound_session(id,call_id,subject_id,trusted_caller,access_number_masked,node_id,protocol_version,prompt_version) VALUES (?,?,?,false,'PRIVATE_ACCESS','fixture','v1','v1')", session, session.toString(), subject); }
                    execute(connection, "INSERT INTO activation_consent.consent_record(id,subject_id,purpose,channel,event_domain,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,ivr_session_id,snapshot,snapshot_sha256,event_id,request_fingerprint) VALUES (?,?,'SERVICE_ACTIVATION',?,?,'fixture','v1','v1','ACCEPT',?,?,?,?,jsonb_build_object('secret','PRIVATE_CONSENT'),repeat('a',64),?,repeat('b',64))", consent, subject, facts.sourceChannel(), facts.sourceActionDomain(), session == null ? "ACCOUNT" : "IVR_SESSION", consent.toString(), session == null ? account : null, session, consent.toString());
                }
                execute(connection, "INSERT INTO activation_consent.service_activation_transition(id,activation_id,subject_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,source_action_event_id,status_event_domain,status_event_id,request_fingerprint,actor_account_id,actor_employee_id,reason_code,consent_record_id,occurred_at) VALUES (?,?,?,?,?,?,?,?,?,?,'ACTIVATION_STATUS',?,repeat('c',64),?,?,?,?,?::timestamptz)", facts.transitionId(), activation, facts.subjectId(), facts.fromStatus(), facts.toStatus(), facts.fromRevision(), facts.toRevision(), facts.sourceChannel(), facts.sourceActionDomain(), "PRIVATE_ACTION_" + identifier, facts.statusEventId(), account, consent == null ? fixture.employee : null, facts.reasonCode(), consent, facts.occurredAt().toString());
                connection.commit();
            }
            expected.put(identifier, facts);
        }
        var body = fixture.get(BASE); assertThat(body.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(body.path("items").size()).isEqualTo(5); assertThat(body.path("nextCursor").isNull()).isTrue(); assertThat(body.path("hasMore").asBoolean()).isFalse();
        var seen = new HashSet<UUID>();
        for (var item : body.path("items")) {
            assertThat(item.propertyNames()).containsExactlyInAnyOrder("transitionId", "subjectId", "statusEventId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt");
            UUID identifier = UUID.fromString(item.path("transitionId").asString()); assertThat(seen.add(identifier)).isTrue(); assertThat(expected).containsKey(identifier);
            var facts = expected.get(identifier);
            for (String field : List.of("transitionId", "subjectId", "statusEventId", "fromStatus", "toStatus", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt")) assertThat(item.path(field).isString()).as(field).isTrue();
            assertThat(item.path("transitionId").asString()).isEqualTo(facts.transitionId().toString());
            assertThat(item.path("subjectId").asString()).isEqualTo(facts.subjectId().toString());
            assertThat(item.path("statusEventId").asString()).isEqualTo(facts.statusEventId());
            assertThat(item.path("fromStatus").asString()).isEqualTo(facts.fromStatus());
            assertThat(item.path("toStatus").asString()).isEqualTo(facts.toStatus());
            assertThat(item.path("fromRevision").isIntegralNumber()).isTrue(); assertThat(item.path("fromRevision").asInt()).isEqualTo(facts.fromRevision());
            assertThat(item.path("toRevision").isIntegralNumber()).isTrue(); assertThat(item.path("toRevision").asInt()).isEqualTo(facts.toRevision());
            assertThat(item.path("sourceChannel").asString()).isEqualTo(facts.sourceChannel());
            assertThat(item.path("sourceActionDomain").asString()).isEqualTo(facts.sourceActionDomain());
            assertThat(item.path("reasonCode").asString()).isEqualTo(facts.reasonCode());
            assertThat(item.path("occurredAt").asString()).isEqualTo(facts.occurredAt().toString());
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(expected.keySet());
        for (String negative : List.of("INACTIVE:OPERATIONS", "ACTIVE:WEB", "ACTIVE:MINIAPP", "ACTIVE:IVR")) for (boolean lookahead : List.of(false, true)) {
            fixture.reset(); fixture.transition(1); String[] shape = negative.split(":"); fixture.transition(UUID.randomUUID(), lookahead ? fixture.time.minusSeconds(2) : fixture.time, shape[0], shape[1], "INVALID_DOMAIN_SHAPE");
            var response = fixture.request("GET", BASE + "?limit=1", "admin"); problem(response, 503, "SERVICE_UNAVAILABLE"); assertThat(response.body()).doesNotContain("items", "nextCursor", "INVALID_DOMAIN_SHAPE");
        }
    }
    @Test
    void onlyTransitionFactsAreReadAndNoHiddenFieldsLeak() throws Exception {
        seed(2); var queries = new ArrayList<String>(); var providers = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { queries.add(context.sql()); } });
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME); var capture = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>(); capture.start(); logger.addAppender(capture);
        var canaries = new ArrayList<>(List.of("PRIVATE_ACTION_", "PRIVATE_CONSENT", "PRIVATE_ACCOUNT", "PRIVATE_EMPLOYEE", fixture.employee.toString(), fixture.password, fixture.login, fixture.base.url, "SELECT id, subject_id", "jdbc:postgresql", fixture.context.getBean(OperationsSubjectReadProperties.class).cursor().keys().getFirst().material()));
        try {
            logger.info("PA34 harmless capture marker"); assertThat(capture.list).anyMatch(event -> event.getFormattedMessage().contains("PA34 harmless capture marker"));
            var success = fixture.request("GET", BASE + "?limit=1", "admin"); assertThat(success.statusCode()).isEqualTo(200); String token = json(success).path("nextCursor").asString(); canaries.add(token);
            assertThat(queries.stream().filter(query -> query.contains("FROM activation_consent.service_activation_transition"))).hasSize(1);
            assertThat(queries.stream().filter(query -> query.startsWith("SELECT "))).allMatch(query -> query.contains("session_user AS login") || query.contains("FROM operations_iam.employee_account") || query.contains("FROM activation_consent.service_activation_transition"));
            for (String cursor : List.of("malformed", token.substring(0, token.length() - 2) + "XX")) problem(fixture.request("GET", BASE + "?cursor=" + cursor, "admin"), 400, "CURSOR_INVALID");
            fixture.replaceCursor(null); problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); fixture.replaceCursor(com.onecall.archtests.operations.OperationsActivationReadTestProviders.properties().cursor());
            fixture.dsl.configuration().set(providers); logger.detachAppender(capture); fixture.reset(); fixture.transition(UUID.randomUUID(), fixture.time, "INACTIVE", "OPERATIONS", "INVALID"); logger.addAppender(capture); fixture.dsl.configuration().set(new DefaultExecuteListener() { }); problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE");
            logger.detachAppender(capture); fixture.reset(); logger.addAppender(capture); assertThat(fixture.get(BASE).path("items").size()).isZero();
            for (var event : capture.list) { String rendered = event.getFormattedMessage() + Arrays.deepToString(event.getArgumentArray()) + event.getMDCPropertyMap() + (event.getThrowableProxy() == null ? "" : ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(event.getThrowableProxy())); assertThat(rendered).doesNotContain(canaries.toArray(String[]::new)); }
            assertThat(success.body()).doesNotContain(canaries.subList(0, canaries.size() - 1).toArray(String[]::new));
        } finally { fixture.dsl.configuration().set(providers); logger.detachAppender(capture); capture.stop(); fixture.replaceCursor(com.onecall.archtests.operations.OperationsActivationReadTestProviders.properties().cursor()); }
    }
    @Test
    void concurrentAppendIsInvisibleWithinSnapshotAndVisibleNextRequest() throws Exception {
        fixture.transition(1); concurrent(false); assertThat(fixture.get(BASE).path("items").size()).isEqualTo(2);
    }
    @Test
    void concurrentEmployeeDisableUsesSameSnapshotThenDeniesNextRequest() throws Exception {
        fixture.transition(1); concurrent(true); problem(fixture.request("GET", BASE, "admin"), 403, "FORBIDDEN");
    }
    @Test
    void dependencyTimeoutAndUnknownFailuresUseSanitizedProblems() throws Exception {
        fixture.transition(1); var providers = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { if (context.sql().contains("FROM activation_consent.service_activation_transition")) try (var statement = context.connection().createStatement()) { statement.execute("SELECT pg_sleep(3)"); } catch (SQLException failure) { throw new org.jooq.exception.DataAccessException("PRIVATE_TIMEOUT", failure); } } });
        try { problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { fixture.dsl.configuration().set(providers); }
        fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { if (context.sql().contains("FROM activation_consent.service_activation_transition")) throw new IllegalStateException("PRIVATE_UNKNOWN"); } });
        try { problem(fixture.request("GET", BASE, "admin"), 500, "INTERNAL"); } finally { fixture.dsl.configuration().set(providers); }
    }
    @Test
    void readsLeaveBusinessAuditAndOutboxFactsUnchanged() throws Exception {
        seed(2); var before = fixture.fingerprints(); fixture.get(BASE + "?limit=1"); problem(fixture.request("GET", BASE + "?cursor=bad", "admin"), 400, "CURSOR_INVALID"); fixture.get(BASE + "?cursor=" + new OperationsActivationCursorCodec(fixture.context.getBean(OperationsSubjectReadProperties.class)).encode(new Anchor(fixture.time.minusSeconds(50), UUID.randomUUID()))); assertThat(fixture.fingerprints()).isEqualTo(before);
    }
    @Test
    void ownerAndTransactionFailuresNeverProducePartialSuccess() throws Exception {
        for (String point : List.of("FROM activation_consent.service_activation_transition", "SET LOCAL ROLE activation_consent_role", "SET LOCAL ROLE NONE")) {
            var providers = fixture.dsl.configuration().executeListenerProviders(); var armed = new AtomicBoolean(true); fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { if (context.sql().contains(point) && armed.getAndSet(false)) throw new Failure(Code.SERVICE_UNAVAILABLE); } });
            try { var response = fixture.request("GET", BASE, "admin"); problem(response, 503, "SERVICE_UNAVAILABLE"); assertThat(response.body()).doesNotContain("items", "nextCursor"); } finally { fixture.dsl.configuration().set(providers); } assertThat(fixture.get(BASE).path("items").size()).isZero();
        }
        var original = fixture.service; var transaction = new org.springframework.transaction.support.TransactionTemplate() { @Override public <Result> Result execute(org.springframework.transaction.support.TransactionCallback<Result> action) { fixture.transaction.execute(action); throw new org.springframework.transaction.TransactionSystemException("PRIVATE_COMMIT"); } };
        fixture.replaceService(new DefaultOperationsActivationReadService(transaction, fixture.dsl, fixture.scope, fixture.context.getBeanProvider(OperationsSubjectReadProperties.class), fixture.context.getBeanProvider(ActivationHistoryReader.class)));
        try { problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { fixture.replaceService(original); } fixture.get(BASE);
    }
    @Test
    void existingSubjectLifecycleAndCustomerSurfacesRemainIsolated() throws Exception {
        UUID subject = fixture.subject(); for (String path : List.of("/api/v1/operations/subjects", "/api/v1/operations/subjects/" + subject)) assertThat(fixture.request("GET", path, "admin").statusCode()).isEqualTo(200);
        for (String action : List.of("suspensions", "resumptions")) { String path = "/api/v1/operations/subjects/" + subject + "/" + action; problem(fixture.request("POST", path, null), 401, "UNAUTHENTICATED"); problem(fixture.request("POST", path, "admin"), 422, "VALIDATION_FAILED"); }
        assertThat(fixture.request("GET", "/api/v1/customer/me", "admin").statusCode()).isEqualTo(401); assertThat(fixture.request("GET", "/api/v1/public/shares/x", "admin").statusCode()).isNotEqualTo(200);
    }
    private void seed(int count) throws Exception { for (int index = 1; index <= count; index++) fixture.transition(index); }
    private void concurrent(boolean disable) throws Exception {
        var authority = new CountDownLatch(1); var resume = new CountDownLatch(1); var samples = new CopyOnWriteArrayList<String>(); var connections = new CopyOnWriteArrayList<org.postgresql.PGConnection>(); var providers = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext context) { if (context.sql().contains("FROM operations_iam.employee_account") || context.sql().contains("FROM activation_consent.service_activation_transition")) try { samples.add(one(context.connection(), "SELECT txid_current_snapshot()::text||':'||pg_backend_pid()")); connections.add(context.connection().unwrap(org.postgresql.PGConnection.class)); } catch (Exception failure) { throw new IllegalStateException(failure); } }
            @Override public void executeEnd(ExecuteContext context) { if (context.sql().contains("FROM operations_iam.employee_account")) { authority.countDown(); try { if (!resume.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout"); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); } } }
        });
        var response = CompletableFuture.supplyAsync(() -> { try { return fixture.request("GET", BASE, "admin"); } catch (Exception failure) { throw new CompletionException(failure); } });
        try { assertThat(authority.await(10, TimeUnit.SECONDS)).isTrue(); if (disable) try (var connection = fixture.admin()) { execute(connection, "UPDATE operations_iam.employee_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.employee); } else fixture.transition(2); resume.countDown(); var current = response.get(15, TimeUnit.SECONDS); assertThat(current.statusCode()).isEqualTo(200); assertThat(json(current).path("items").size()).isEqualTo(1); assertThat(samples).hasSize(2); assertThat(samples.getFirst()).isEqualTo(samples.getLast()); assertThat(connections.getFirst()).isSameAs(connections.getLast()); }
        finally { resume.countDown(); fixture.dsl.configuration().set(providers); }
    }
}
