package com.onecall.archtests.container;

import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.CustomerEvidenceFixture.keys;
import static org.assertj.core.api.Assertions.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import com.zaxxer.hikari.HikariDataSource;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

@ExtendWith(OutputCaptureExtension.class)
class CustomerEvidenceIT {
    private static CustomerEvidenceFixture fixture;
    private CapturedOutput output;

    @BeforeAll static void start() throws Exception { fixture = new CustomerEvidenceFixture(); fixture.start(); }
    @AfterAll static void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void seed(CapturedOutput output) throws Exception { this.output = output; fixture.seed(); }

    private JsonNode list(String query) throws Exception { return fixture.list("personal", query, 200); }
    private JsonNode detail(UUID id) throws Exception { return fixture.detail(id, "personal", 200); }
    private void account(String status) throws Exception {
        fixture.mutate("UPDATE customer_iam.customer_account SET status=?,revision=revision+1,updated_at=now() WHERE id=?", status, fixture.base.account);
    }
    private void organization(UUID id, String status) throws Exception {
        fixture.mutate("UPDATE organization_access.organization SET status=?,revision=revision+1,updated_at=now() WHERE id=?", status, id);
    }
    private void membership(UUID id, String status) throws Exception {
        fixture.mutate("UPDATE organization_access.organization_membership SET status=?,revision=revision+1 WHERE account_id=? AND organization_id=?", status, fixture.base.account, id);
    }
    private UUID row(UUID subject, String state, UUID id, Instant time) throws Exception {
        return fixture.evidence(subject, state, id, time, UUID.randomUUID(), "\"" + fixture.capturedAt + "\"");
    }
    private void empty(JsonNode page) {
        assertThat(fixture.ids(page)).isEmpty(); assertThat(page.path("nextCursor").isNull()).isTrue(); assertThat(page.path("hasMore").asBoolean()).isFalse();
    }
    private List<String> roles(JsonNode value) {
        List<String> result = new ArrayList<>(); for (var role : value) result.add(role.asString()); return result;
    }
    private void replaceService(CustomerEvidenceQueryService service) {
        var controller = fixture.base.context.getBean(CustomerEvidenceController.class);
        var factory = new org.springframework.beans.factory.support.StaticListableBeanFactory(Map.of("customerEvidenceQueryService", service));
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "service", factory.getBeanProvider(CustomerEvidenceQueryService.class));
    }
    private CustomerEvidenceQueryService completionFailureService() {
        var manager = fixture.base.context.getBean("customerQueryTransactionManager", DataSourceTransactionManager.class);
        var failing = new PlatformTransactionManager() {
            @Override public TransactionStatus getTransaction(TransactionDefinition definition) { return manager.getTransaction(definition); }
            @Override public void commit(TransactionStatus status) { manager.commit(status); throw new TransactionSystemException("COMPLETION_CANARY"); }
            @Override public void rollback(TransactionStatus status) { manager.rollback(status); }
        };
        var transaction = new TransactionTemplate(failing);
        transaction.setReadOnly(true); transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new CustomerEvidenceQueryService(transaction, fixture.dsl(),
                fixture.base.context.getBean("customerBusinessNumberAccessRepository", JooqCustomerBusinessNumberAccessRepository.class),
                fixture.base.context.getBean("customerSubjectAccessReader", CustomerSubjectAccessReader.class),
                fixture.base.context.getBean("customerEvidenceReader", CustomerEvidenceReader.class));
    }

    @Test
    void postgres16LatestChainAndVerifiedCustomerRoutesAreWired() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(Integer.parseInt(one(connection, "SHOW server_version_num"))).isBetween(160000, 169999);
            assertThat(one(connection, "SELECT count(*) FROM public.flyway_schema_history WHERE success AND type='SQL'")).isEqualTo("19");
        }
        try (var connection = fixture.base.runtime()) { assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa27_customer|pa27_customer"); }
        assertThat(fixture.ids(list(""))).containsExactly(fixture.evidence);
        assertThat(detail(fixture.evidence).path("evidenceId").asString()).isEqualTo(fixture.evidence.toString());
        for (String suffix : List.of("", "/" + fixture.evidence)) {
            var response = fixture.request("GET", suffix, null, HttpRequest.BodyPublishers.noBody());
            fixture.check(response, 401); assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
            fixture.list("unverified", suffix, 401);
        }
        assertThat(fixture.states).isNotEmpty().allMatch(state -> state.startsWith("customer_iam_role|pa27_customer|on|repeatable read|"));
    }

    @Test
    void personalListAndDetailAreSubjectOwnerScoped() throws Exception {
        UUID secondSubject = fixture.subject(fixture.base.account, null); UUID second = fixture.evidence(secondSubject, "FAILED");
        UUID other = fixture.evidence(fixture.foreign, "SOLIDIFIED"); UUID unowned = fixture.evidence(fixture.unowned, "PENDING");
        assertThat(fixture.ids(list(""))).containsExactlyInAnyOrder(fixture.evidence, second);
        detail(fixture.evidence); detail(second); fixture.detail(other, "personal", 404); fixture.detail(unowned, "personal", 404);
        assertThat(fixture.ids(fixture.list("other", "", 200))).containsExactly(other);
        fixture.detail(fixture.evidence, "other", 404); fixture.detail(other, "other", 200);
    }

    @Test
    void organizationListAndDetailUseSelectedOrganizationNotCreator() throws Exception {
        UUID organization = fixture.base.organizations.getFirst();
        UUID subject = fixture.subject(null, organization); UUID id = fixture.evidence(subject, "SOLIDIFIED");
        fixture.order(subject, fixture.base.other, organization);
        UUID different = fixture.evidence(fixture.subject(null, fixture.base.organizations.get(1)), "FAILED");
        fixture.base.session("otherOrg", fixture.base.other, organization);
        assertThat(fixture.ids(fixture.list("org0", "?accountId=" + fixture.base.other + "&organizationId=" + fixture.base.organizations.get(1), 200))).containsExactly(id);
        fixture.detail(id, "org0", 200); fixture.detail(id, "otherOrg", 200);
        fixture.detail(id, "personal", 404); fixture.detail(different, "org0", 404); fixture.detail(fixture.evidence, "org0", 404);
        assertThat(fixture.ids(fixture.list("org1", "", 200))).containsExactly(different);
    }

    @Test
    void allFourActiveOrganizationRolesCanRead() throws Exception {
        for (int index = 0; index < 4; index++) {
            UUID id = fixture.evidence(fixture.subject(null, fixture.base.organizations.get(index)), "PENDING");
            assertThat(fixture.ids(fixture.list("org" + index, "?role=FORBIDDEN&status=SOLIDIFIED", 200))).containsExactly(id);
            fixture.detail(id, "org" + index, 200);
        }
    }

    @Test
    void inactiveMissingAccountsOrganizationsAndMembershipsAreForbidden() throws Exception {
        UUID missing = UUID.randomUUID();
        for (String state : List.of("DISABLED")) {
            account(state); fixture.list("personal", "", 403); fixture.detail(missing, "personal", 403); account("ACTIVE");
        }
        fixture.base.session("missingAccount", UUID.randomUUID(), null);
        fixture.list("missingAccount", "", 403); fixture.detail(missing, "missingAccount", 403);
        UUID organization = fixture.base.organizations.getFirst();
        for (String state : List.of("SUSPENDED", "DISABLED")) {
            organization(organization, state); fixture.list("org0", "", 403); fixture.detail(missing, "org0", 403); organization(organization, "ACTIVE");
        }
        for (String state : List.of("SUSPENDED", "REVOKED")) {
            membership(organization, state); fixture.list("org0", "", 403); fixture.detail(missing, "org0", 403); membership(organization, "ACTIVE");
        }
        fixture.list("org4", "?role=ADMINISTRATOR", 403); fixture.detail(missing, "org4", 403);
        fixture.base.session("missingOrg", fixture.base.account, UUID.randomUUID());
        fixture.list("missingOrg", "", 403); fixture.detail(missing, "missingOrg", 403);
    }

    @Test
    void missingUnownedForeignAndOtherWorkspaceDetailsAreUniformNotFound() throws Exception {
        UUID other = fixture.evidence(fixture.foreign, "FAILED");
        UUID unowned = fixture.evidence(fixture.unowned, "PENDING");
        UUID organization = fixture.evidence(fixture.subject(null, fixture.base.organizations.getFirst()), "PENDING");
        fixture.risk(fixture.binding(fixture.foreign, "RETIRED"), "OPEN");
        JsonNode reference = null;
        for (UUID id : List.of(UUID.randomUUID(), other, unowned, organization)) {
            fixture.queries.clear(); var problem = fixture.detail(id, "personal", 404);
            assertThat(fixture.queries).noneMatch(sql -> sql.contains("subject_risk_state") || sql.contains("evidence_artifact") || sql.contains("media_object"));
            if (reference != null) for (String key : List.of("type", "title", "status", "code")) assertThat(problem.path(key)).isEqualTo(reference.path(key));
            reference = problem;
        }
        fixture.detail(fixture.evidence, "org0", 404);
    }

    @Test
    void listExcludesInaccessibleSubjectsBeforeSeekAndLookahead() throws Exception {
        UUID older = row(fixture.subject, "FAILED", UUID.randomUUID(), fixture.base.time.minusSeconds(1));
        for (int index = 0; index < 5; index++) {
            row(fixture.foreign, "PENDING", UUID.randomUUID(), fixture.base.time.plusSeconds(index + 1));
            row(fixture.unowned, "PENDING", UUID.randomUUID(), fixture.base.time.minusNanos(1000));
        }
        var first = list("?limit=1"); assertThat(fixture.ids(first)).containsExactly(fixture.evidence); assertThat(first.path("hasMore").asBoolean()).isTrue();
        var tail = list("?limit=1&cursor=" + first.path("nextCursor").asString());
        assertThat(fixture.ids(tail)).containsExactly(older); assertThat(tail.path("hasMore").asBoolean()).isFalse(); assertThat(tail.path("nextCursor").isNull()).isTrue();
    }

    @Test
    void allFourEvidenceStatesAreVisibleWithoutArtifactEligibilityGate() throws Exception {
        Map<UUID, String> states = new HashMap<>(); states.put(fixture.evidence, "PENDING");
        for (String state : List.of("SOLIDIFIED", "QUARANTINED", "FAILED")) states.put(fixture.evidence(fixture.subject, state), state);
        assertThat(fixture.ids(list("?status=SOLIDIFIED&sourceType=OTHER&subjectId=" + fixture.foreign))).containsExactlyInAnyOrderElementsOf(states.keySet());
        for (var entry : states.entrySet()) {
            var value = detail(entry.getKey()); assertThat(value.path("status").asString()).isEqualTo(entry.getValue()); assertThat(value.path("artifacts").size()).isZero();
            assertThat(value.has("solidifiedAt")).isEqualTo(entry.getValue().equals("SOLIDIFIED"));
        }
        for (var item : list("").path("items")) assertThat(item.path("artifactRoles").size()).isZero();
    }

    @Test
    void capturedAtComesOnlyFromImmutableCaptureScalar() throws Exception {
        fixture.mutate("UPDATE customer_iam.customer_account SET display_name='LIVE_IDENTITY_CANARY',revision=revision+1,updated_at=now() WHERE id=?", fixture.base.account);
        fixture.mutate("INSERT INTO customer_iam.identity_profile(id,account_id,real_name_status,legal_name_ciphertext,id_number_ciphertext,id_number_hmac,verification_ref_ciphertext,verified_at) VALUES (?,?,'VERIFIED','v1.fixture.LEGAL_NAME_CANARY','v1.fixture.IDENTITY_CANARY',repeat('a',64),'v1.fixture.VERIFICATION_CANARY',now())", UUID.randomUUID(), fixture.base.account);
        try (var connection = fixture.admin()) {
            execute(connection, "SET ROLE subject_identity_verification_role");
            execute(connection, "UPDATE subject_number.business_subject SET real_name_status='VERIFIED',real_name_verified_at=now(),revision=revision+1,updated_at=now() WHERE id=?", fixture.subject);
        }
        for (JsonNode value : List.of(detail(fixture.evidence), list("").path("items").get(0))) {
            assertThat(Instant.parse(value.path("capturedAt").asString())).isEqualTo(fixture.capturedAt);
            assertThat(value.path("status").asString()).isEqualTo("PENDING"); assertThat(value.path("consentVersion").asString()).isEqualTo("consent-v1");
            assertThat(value.path("sourceType").asString()).isEqualTo("CALL_RECORDING");
        }
        assertThat(fixture.queries).noneMatch(sql -> sql.contains("snapshot") || sql.contains("evidence_event") || sql.contains("display_name") || sql.contains("identity_at_capture::"));
        assertThat(fixture.queries).anyMatch(sql -> sql.contains("jsonb_typeof") && sql.contains("->>'capturedAt'"));
        assertThatThrownBy(() -> fixture.mutate("UPDATE evidence.evidence_record SET identity_at_capture=jsonb_set(identity_at_capture,'{capturedAt}','\"2099-01-01T00:00:00Z\"') WHERE id=?", fixture.evidence))
                .isInstanceOf(java.sql.SQLException.class).hasMessageContaining("EVIDENCE_RECORD_HEAD_IMMUTABLE");
    }

    @Test
    void summaryAndDetailHaveExactDistinctSchemaKeySets() throws Exception {
        UUID source = UUID.randomUUID(); UUID id = fixture.evidence(fixture.subject, "SOLIDIFIED", UUID.randomUUID(), fixture.base.time, source, "\"" + fixture.capturedAt + "\"");
        UUID media = fixture.media("a".repeat(64), 12, "audio/wav"); fixture.recording(source, media, "VALIDATED", 37); fixture.artifact(id, media, "ORIGINAL");
        UUID derived = fixture.media("b".repeat(64), 13, "audio/mpeg"); fixture.artifact(id, derived, "DERIVED");
        var page = list(""); fixture.ids(page);
        keys(detail(fixture.evidence), "evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "artifacts");
        var value = detail(id); keys(value, "evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "solidifiedAt", "artifacts");
        keys(value.path("artifacts").get(0), "artifactRole", "mimeType", "sizeBytes", "sha256", "validatedDurationSeconds");
        keys(value.path("artifacts").get(1), "artifactRole", "mimeType", "sizeBytes", "sha256");
        assertThat(value.toString()).doesNotContain(":null");
    }

    @Test
    void artifactRolesAndArtifactsUseRankHashSizeMimeOrder() throws Exception {
        fixture.artifact(fixture.evidence, fixture.media("f".repeat(64), 100, "audio/wav"), "ORIGINAL");
        for (Object[] facts : List.of(new Object[]{"b", 1L, "audio/wav"}, new Object[]{"a", 2L, "audio/wav"},
                new Object[]{"a", 1L, "audio/wav"}, new Object[]{"a", 1L, "audio/aac"}, new Object[]{"a", 1L, "audio/aac"}))
            fixture.artifact(fixture.evidence, fixture.media(((String) facts[0]).repeat(64), (Long) facts[1], (String) facts[2]), "DERIVED");
        var artifacts = detail(fixture.evidence).path("artifacts"); assertThat(artifacts.size()).isEqualTo(6);
        assertThat(artifacts.get(0).path("artifactRole").asString()).isEqualTo("ORIGINAL");
        assertThat(artifacts.get(1)).isEqualTo(artifacts.get(2));
        assertThat(artifacts.get(1).path("mimeType").asString()).isEqualTo("audio/aac");
        assertThat(artifacts.get(3).path("mimeType").asString()).isEqualTo("audio/wav");
        assertThat(artifacts.get(4).path("sizeBytes").asLong()).isEqualTo(2);
        assertThat(artifacts.get(5).path("sha256").asString()).isEqualTo("b".repeat(64));
        assertThat(roles(list("").path("items").get(0).path("artifactRoles"))).containsExactly("ORIGINAL", "DERIVED");
    }

    @Test
    void durationUsesOnlyUniqueMatchingValidatedSourceRecording() throws Exception {
        UUID media = fixture.media("a".repeat(64), 17, "audio/wav"); UUID source = UUID.randomUUID();
        fixture.recording(source, media, "VALIDATED", 37);
        fixture.recording(UUID.randomUUID(), media, "VALIDATED", 88); fixture.recording(UUID.randomUUID(), media, "VALIDATED", 99);
        UUID id = fixture.evidence(fixture.subject, "PENDING", UUID.randomUUID(), fixture.base.time.plusSeconds(1), source, "\"" + fixture.capturedAt + "\"");
        fixture.artifact(id, media, "ORIGINAL"); fixture.artifact(id, media, "DERIVED");
        var artifacts = detail(id).path("artifacts"); assertThat(artifacts.size()).isEqualTo(2);
        assertThat(artifacts.get(0).path("artifactRole").asString()).isEqualTo("ORIGINAL");
        assertThat(artifacts.get(0).path("validatedDurationSeconds").asInt()).isEqualTo(37);
        assertThat(artifacts.get(1).path("artifactRole").asString()).isEqualTo("DERIVED"); assertThat(artifacts.get(1).has("validatedDurationSeconds")).isFalse();
        for (String key : List.of("sha256", "sizeBytes", "mimeType")) assertThat(artifacts.get(0).path(key)).isEqualTo(artifacts.get(1).path(key));
        assertThat(roles(list("").path("items").get(0).path("artifactRoles"))).containsExactly("ORIGINAL", "DERIVED");
        for (String scenario : List.of("absent", "DECLARED", "UPLOADED", "QUARANTINED", "misbound", "derivedOnly")) {
            UUID recording = UUID.randomUUID();
            if (!scenario.equals("absent")) fixture.recording(recording,
                    scenario.equals("DECLARED") ? null : scenario.equals("misbound") ? fixture.media("b".repeat(64), 18, "audio/wav") : media,
                    scenario.equals("misbound") || scenario.equals("derivedOnly") ? "VALIDATED" : scenario, 41);
            UUID item = fixture.evidence(fixture.subject, "FAILED", UUID.randomUUID(), fixture.base.time, recording, "\"" + fixture.capturedAt + "\"");
            fixture.artifact(item, media, scenario.equals("derivedOnly") ? "DERIVED" : "ORIGINAL");
            var result = detail(item).path("artifacts"); assertThat(result.size()).isEqualTo(1); assertThat(result.get(0).has("validatedDurationSeconds")).isFalse();
        }
    }

    @Test
    void openRiskOnAnySubjectBindingIsForbidden() throws Exception {
        for (String status : List.of("ACTIVE", "DISABLED", "RETIRED")) {
            fixture.risk(fixture.binding(fixture.subject, status), "OPEN");
            fixture.queries.clear(); fixture.detail(fixture.evidence, "personal", 403);
            assertThat(fixture.queries).noneMatch(sql -> sql.contains("evidence_artifact")); fixture.list("personal", "", 403);
            fixture.clearRisk(); detail(fixture.evidence); list("");
        }
    }

    @Test
    void listOffPageRiskBlocksButForeignAndNoEvidenceRiskDoNot() throws Exception {
        UUID offPage = fixture.subject(fixture.base.account, null); row(offPage, "FAILED", UUID.randomUUID(), fixture.base.time.minusSeconds(1));
        var first = list("?limit=1"); String cursor = first.path("nextCursor").asString();
        fixture.risk(fixture.binding(offPage, "RETIRED"), "OPEN");
        for (String query : List.of("?limit=1", "?limit=100", "?limit=1&cursor=" + cursor)) fixture.list("personal", query, 403);
        fixture.clearRisk();
        fixture.risk(fixture.binding(fixture.foreign, "DISABLED"), "OPEN"); fixture.evidence(fixture.foreign, "PENDING");
        fixture.risk(fixture.binding(fixture.unowned, "ACTIVE"), "OPEN"); fixture.evidence(fixture.unowned, "PENDING");
        fixture.risk(fixture.binding(fixture.subject(fixture.base.account, null), "ACTIVE"), "OPEN");
        assertThat(fixture.ids(list("?limit=100"))).hasSize(2); list("?limit=1&cursor=" + cursor);
    }

    @Test
    void absentBindingsAndClearedOrAbsentRiskDoNotInventRestrictions() throws Exception {
        detail(fixture.evidence); assertThat(fixture.ids(list(""))).containsExactly(fixture.evidence);
        fixture.binding(fixture.subject, "RETIRED"); detail(fixture.evidence);
        fixture.risk(fixture.binding(fixture.subject, "DISABLED"), "CLEARED"); detail(fixture.evidence); list("");
        fixture.mutate("INSERT INTO activation_consent.service_activation(id,subject_id,status) VALUES (?,?,'SUSPENDED')", UUID.randomUUID(), fixture.subject);
        detail(fixture.evidence); list("");
    }

    @Test
    void defaultBoundsEmptySingleAndTailPagesAreExact() throws Exception {
        fixture.queries.clear(); empty(fixture.list("other", "", 200));
        assertThat(fixture.queries).noneMatch(sql -> sql.contains("evidence_artifact"));
        var single = list("?limit=1"); assertThat(fixture.ids(single)).containsExactly(fixture.evidence); assertThat(single.path("nextCursor").isNull()).isTrue();
        for (int index = 1; index <= 100; index++) row(fixture.subject, "PENDING", UUID.randomUUID(), fixture.base.time.minusSeconds(index));
        var defaultPage = list(""); assertThat(fixture.ids(defaultPage)).hasSize(20); assertThat(defaultPage.path("hasMore").asBoolean()).isTrue();
        assertThat(fixture.ids(list("?limit=020"))).isEqualTo(fixture.ids(defaultPage));
        var maximum = list("?limit=100"); assertThat(fixture.ids(maximum)).hasSize(100); assertThat(maximum.path("hasMore").asBoolean()).isTrue();
        var tail = list("?limit=100&cursor=" + maximum.path("nextCursor").asString()); assertThat(fixture.ids(tail)).hasSize(1);
        assertThat(tail.path("hasMore").asBoolean()).isFalse(); assertThat(tail.path("nextCursor").isNull()).isTrue();
        for (String invalid : List.of("", "0", "101", "-1", "%20", "+1", "1.5", "2147483648")) fixture.list("personal", "?limit=" + invalid + "&cursor=bad", 422);
    }

    @Test
    void descendingCreatedAtAndUuidKeysetSurvivesTiesAndNewInserts() throws Exception {
        fixture.base.session("walk", fixture.base.other, null);
        var ids = List.of(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), UUID.fromString("80000000-0000-0000-0000-000000000000"),
                UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        for (UUID id : ids) row(fixture.foreign, "PENDING", id, fixture.base.time);
        var first = fixture.list("walk", "?limit=2", 200); assertThat(fixture.ids(first)).containsExactlyElementsOf(ids.subList(0, 2));
        UUID ahead = row(fixture.foreign, "PENDING", UUID.randomUUID(), fixture.base.time.plusSeconds(1));
        UUID behind = row(fixture.foreign, "PENDING", UUID.randomUUID(), fixture.base.time.minusSeconds(1));
        List<UUID> walked = new ArrayList<>(fixture.ids(first)); String cursor = first.path("nextCursor").asString();
        while (cursor != null) {
            var page = fixture.list("walk", "?limit=2&cursor=" + cursor, 200); walked.addAll(fixture.ids(page));
            cursor = page.path("nextCursor").isNull() ? null : page.path("nextCursor").asString();
        }
        assertThat(walked).containsExactly(ids.get(0), ids.get(1), ids.get(2), ids.get(3), behind).doesNotHaveDuplicates().doesNotContain(ahead);
        assertThat(fixture.ids(fixture.list("walk", "?limit=1", 200))).containsExactly(ahead);
    }

    @Test
    void tamperedCrossAccountWorkspaceAndEndpointCursorsNeverWidenScope() throws Exception {
        fixture.evidence(fixture.subject, "FAILED"); String cursor = list("?limit=1").path("nextCursor").asString();
        byte[] bytes = Base64.getUrlDecoder().decode(cursor); bytes[92] ^= 1;
        fixture.list("personal", "?cursor=" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), 400);
        for (String token : List.of("other", "org0", "org1")) fixture.list(token, "?cursor=" + cursor, 400);
        var properties = new CustomerEvidenceCursorProperties(); properties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        var codec = new CustomerEvidenceCursorCodec(properties);
        var anchor = new CustomerEvidenceReader.Anchor(fixture.base.time, fixture.evidence);
        String foreign = codec.encode(new Scope(fixture.base.other, null), anchor); fixture.list("personal", "?cursor=" + foreign, 400);
        var businessProperties = new CustomerBusinessNumberCursorProperties(); businessProperties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        String business = new CustomerBusinessNumberCursorCodec(businessProperties).encode(new Scope(fixture.base.account, null),
                new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Anchor(fixture.base.time, fixture.evidence));
        fixture.list("personal", "?cursor=" + business, 400);
        assertThat(fixture.base.get("personal", "?cursor=" + cursor).statusCode()).isEqualTo(400);
        for (String invalid : List.of("bad", "", cursor + "=", cursor + "&cursor=" + cursor)) fixture.list("personal", "?cursor=" + invalid, 400);
    }

    @Test
    void uuidRouteMethodAndNonAuthoritativeInputPrecedenceIsExact() throws Exception {
        for (String suffix : List.of("/bad", "/1-1-1-1-1", "/" + fixture.evidence + ".json")) {
            fixture.queries.clear(); fixture.list("personal", suffix, 422); assertThat(fixture.queries).isEmpty(); fixture.list("invalid", suffix, 401);
        }
        for (String suffix : List.of("/", ".json", "/" + fixture.evidence + "/", "/" + fixture.evidence + "/extra")) fixture.list(null, suffix, 404);
        for (String method : List.of("HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"))
            for (String suffix : List.of("", "/" + fixture.evidence))
                assertThat(fixture.request(method, suffix, null, HttpRequest.BodyPublishers.noBody()).statusCode()).isEqualTo(404);
        String query = "?status=SOLIDIFIED&subjectId=" + fixture.foreign + "&ownerAccountId=" + fixture.base.other + "&workspace=foreign&role=ADMINISTRATOR&sort=asc&unknown=a&unknown=b";
        for (boolean chunked : List.of(false, true)) {
            var body = chunked ? HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream("BODY_CANARY".getBytes(StandardCharsets.UTF_8))) : HttpRequest.BodyPublishers.ofString("{invalid BODY_CANARY");
            var page = fixture.check(fixture.request("GET", query, "personal", body), 200); assertThat(fixture.ids(page)).containsExactly(fixture.evidence);
            fixture.check(fixture.request("GET", "/" + fixture.evidence.toString().toUpperCase(Locale.ROOT) + query + "&limit=bad&limit=bad&cursor=bad&cursor=bad", "personal", body), 200);
        }
        fixture.queries.clear(); fixture.list("personal", "?limit=0&cursor=bad", 422); fixture.list("personal", "?cursor=bad", 400);
        fixture.list("personal", "?limit=1&limit=2", 422); assertThat(fixture.queries).isEmpty();
        fixture.failOn = "SET LOCAL ROLE";
        try { fixture.list("personal", "?limit=0", 422); fixture.list("personal", "?cursor=bad", 400); fixture.list("personal", "", 503); }
        finally { fixture.failOn = null; }
    }

    @Test
    void unrepresentableCaptureAndArtifactFactsFailClosedWithoutLeakage() throws Exception {
        for (String capture : List.of("null", "42", "{}", "\"not-a-time\"", "\"2026-01-01T00:00:00\"")) {
            UUID account = UUID.randomUUID(); fixture.mutate("INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'reader')", account);
            fixture.base.session("malformed", account, null); UUID subject = fixture.subject(account, null);
            UUID id = fixture.evidence(subject, "PENDING", UUID.randomUUID(), fixture.base.time, UUID.randomUUID(), capture);
            fixture.detail(id, "malformed", 404); fixture.list("malformed", "", 500);
        }
        fixture.artifact(fixture.evidence, fixture.media("a".repeat(64), 1, "x".repeat(129)), "DERIVED");
        fixture.detail(fixture.evidence, "personal", 404); assertThat(fixture.ids(list(""))).containsExactly(fixture.evidence);
        assertThat(output.getAll()).doesNotContain("IDENTITY_CANARY", "SNAPSHOT_CANARY", "BUCKET_CANARY", "EVENT_CANARY");
    }

    @Test
    void detailRereadsCurrentStateOnEveryRequest() throws Exception {
        for (String terminal : List.of("SOLIDIFIED", "FAILED", "QUARANTINED")) {
            UUID id = fixture.evidence(fixture.subject, "PENDING"); assertThat(detail(id).path("status").asString()).isEqualTo("PENDING");
            fixture.transition(id, terminal); var current = detail(id); assertThat(current.path("status").asString()).isEqualTo(terminal);
            assertThat(current.has("solidifiedAt")).isEqualTo(terminal.equals("SOLIDIFIED"));
            if (terminal.equals("SOLIDIFIED")) assertThat(Instant.parse(current.path("solidifiedAt").asString())).isEqualTo(fixture.base.time.plusSeconds(60));
            assertThat(Instant.parse(current.path("capturedAt").asString())).isEqualTo(fixture.capturedAt);
        }
        assertThat(fixture.queries).noneMatch(sql -> sql.contains("sharing.") || sql.contains("snapshot") || sql.contains("evidence_event"));
    }

    @Test
    void composedReadsShareOneSnapshotAndObserveLaterRevocationNextRequest() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            for (boolean lifecycle : List.of(false, true)) {
                UUID id = fixture.evidence(fixture.subject, "PENDING");
                CountDownLatch entered = new CountDownLatch(1); CountDownLatch committed = new CountDownLatch(1);
                Runnable barrier = () -> {
                    entered.countDown();
                    try { assertThat(committed.await(30, TimeUnit.SECONDS)).isTrue(); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                };
                if (lifecycle) fixture.afterLifecycle = barrier; else fixture.afterOwnership = barrier;
                var response = executor.submit(() -> fixture.detail(id, "personal", 200));
                try {
                    assertThat(entered.await(30, TimeUnit.SECONDS)).isTrue();
                    try (var connection = fixture.admin()) {
                        connection.setAutoCommit(false);
                        execute(connection, "UPDATE evidence.evidence_record SET status='SOLIDIFIED',solidified_at=now(),revision=revision+1 WHERE id=?", id);
                        UUID media = UUID.randomUUID(); UUID binding = UUID.randomUUID();
                        execute(connection, "INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES (?,'COS','BUCKET_CANARY',?,'v1',repeat('a',64),1,'audio/wav','KEY_CANARY')", media, media.toString());
                        execute(connection, "INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,'DERIVED')", UUID.randomUUID(), id, media);
                        execute(connection, "INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status) VALUES (?,?,'MOBILE','v1.fixture.CANARY',?,'CANARY','RETIRED')", binding, fixture.subject, UUID.randomUUID().toString().replace("-", "").repeat(2));
                        execute(connection, "INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source) VALUES (?,?,'CONFLICT','OPEN','IMPORT')", UUID.randomUUID(), binding);
                        connection.commit();
                    }
                } finally { committed.countDown(); }
                var old = response.get(30, TimeUnit.SECONDS); assertThat(old.path("status").asString()).isEqualTo("PENDING"); assertThat(old.path("artifacts").size()).isZero();
                fixture.detail(id, "personal", 403); fixture.clearRisk(); var current = detail(id);
                assertThat(current.path("status").asString()).isEqualTo("SOLIDIFIED"); assertThat(current.path("artifacts").size()).isEqualTo(1);
            }
            CountDownLatch entered = new CountDownLatch(1); CountDownLatch revoked = new CountDownLatch(1);
            fixture.afterLifecycle = () -> { entered.countDown(); try { assertThat(revoked.await(30, TimeUnit.SECONDS)).isTrue(); } catch (InterruptedException failure) { throw new IllegalStateException(failure); } };
            var response = executor.submit(() -> fixture.detail(fixture.evidence, "personal", 200));
            try { assertThat(entered.await(30, TimeUnit.SECONDS)).isTrue(); account("DISABLED"); } finally { revoked.countDown(); }
            response.get(30, TimeUnit.SECONDS); fixture.detail(fixture.evidence, "personal", 403); account("ACTIVE");
        }
    }

    @Test
    void readsLeaveAllBusinessRowsAndEventsByteEquivalent() throws Exception {
        UUID riskySubject = fixture.subject(fixture.base.account, null); UUID risky = fixture.evidence(riskySubject, "FAILED");
        fixture.order(fixture.subject, fixture.base.other, null);
        UUID organizationEvidence = fixture.evidence(fixture.subject(null, fixture.base.organizations.getFirst()), "PENDING");
        fixture.risk(fixture.binding(riskySubject, "RETIRED"), "OPEN");
        String before = fixture.snapshot(); int logOffset = output.getAll().length();
        detail(fixture.evidence); fixture.detail(risky, "personal", 403); fixture.detail(UUID.randomUUID(), "personal", 404);
        assertThat(fixture.ids(fixture.list("org0", "", 200))).containsExactly(organizationEvidence);
        fixture.list("personal", "", 403); empty(fixture.list("other", "", 200)); fixture.list("personal", "?limit=0", 422); fixture.list("personal", "?cursor=bad", 400);
        fixture.failOn = "evidence_record";
        try { fixture.detail(fixture.evidence, "personal", 503); fixture.list("personal", "", 503); } finally { fixture.failOn = null; }
        assertThat(fixture.snapshot()).isEqualTo(before);
        assertThat(fixture.queries).allMatch(sql -> sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT") || sql.equals("SET LOCAL ROLE customer_iam_role"));
        assertThat(output.getAll().substring(logOffset)).doesNotContain("CANARY", fixture.base.password, "Bearer ", "SCRAM-SHA-256", "jdbc:", fixture.subject.toString(), fixture.evidence.toString());
    }

    @Test
    void roleReadOnlyIsolationRequiresNewQueryCountsAndPoolCleanupAreExact() throws Exception {
        fixture.resetObservations(); list("?limit=1"); int count = fixture.queries.size();
        assertThat(count).isEqualTo(8); assertThat(new HashSet<>(fixture.states)).hasSize(1); fixture.cleanPool();
        for (int index = 1; index < 100; index++) fixture.evidence(fixture.subject, "FAILED");
        fixture.resetObservations(); assertThat(fixture.ids(list("?limit=100"))).hasSize(100); assertThat(fixture.queries).hasSize(count);
        assertThat(new HashSet<>(fixture.states)).hasSize(1); fixture.cleanPool();
        fixture.resetObservations(); detail(fixture.evidence); assertThat(fixture.queries).hasSize(7); assertThat(new HashSet<>(fixture.states)).hasSize(1);
        assertThat(fixture.states).allMatch(state -> state.startsWith("customer_iam_role|pa27_customer|on|repeatable read|"));
        fixture.detail(UUID.randomUUID(), "personal", 404); fixture.cleanPool(); empty(fixture.list("other", "", 200)); fixture.cleanPool();
        account("DISABLED"); fixture.detail(fixture.evidence, "personal", 403); fixture.cleanPool(); account("ACTIVE");
        for (String failure : List.of("SET LOCAL ROLE", "select current_user", "evidence_record", "evidence_artifact")) {
            fixture.failOn = failure;
            try { fixture.detail(fixture.evidence, "personal", 503); } finally { fixture.failOn = null; }
            fixture.cleanPool();
        }
        fixture.probeWrite = true; fixture.detail(fixture.evidence, "personal", 503);
        assertThat(fixture.writeSqlState).isIn("25006", "42501"); fixture.cleanPool();
        var original = fixture.service(); replaceService(completionFailureService());
        try { fixture.detail(fixture.evidence, "personal", 503); fixture.detail(UUID.randomUUID(), "personal", 503); fixture.list("other", "", 503); }
        finally { replaceService(original); }
        fixture.cleanPool();
        var pool = fixture.base.context.getBean("customerQueryDataSource", HikariDataSource.class); pool.setMaximumPoolSize(2);
        var manager = fixture.base.context.getBean("customerQueryTransactionManager", DataSourceTransactionManager.class);
        try {
            new TransactionTemplate(manager).execute(status -> {
                String before = fixture.dsl().fetchOne("SELECT current_user||'|'||pg_backend_pid()").get(0, String.class);
                fixture.resetObservations(); assertThat(original.getCustomerEvidence(fixture.caller(), fixture.evidence)).isPresent();
                String outerPid = before.substring(before.indexOf('|') + 1);
                assertThat(fixture.states).isNotEmpty().allMatch(state -> state.startsWith("customer_iam_role|pa27_customer|on|repeatable read|") && !state.endsWith("|" + outerPid));
                assertThat(new HashSet<>(fixture.states)).hasSize(1);
                assertThat(fixture.dsl().fetchOne("SELECT current_user||'|'||pg_backend_pid()").get(0, String.class)).isEqualTo(before);
                return null;
            });
        } finally { pool.setMaximumPoolSize(1); }
        fixture.cleanPool();
    }
}
