package com.onecall.core.operations_iam.internal.ivrread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders.*;
import com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;


class OperationsIvrSessionReadServiceTests {
    private void failure(Runnable operation, Code code) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, failure -> {
            assertThat(failure.code()).isEqualTo(code); assertThat(failure.getMessage()).doesNotContain("PRIVATE", "jdbc:");
        });
    }
    private RawDetailRequest detail() { return new RawDetailRequest(fact(1).sessionId().toString(), Map.of(), List.of()); }
    private org.jooq.Result<org.jooq.Record> rows(String[] names, Class<?>[] types) {
        var fields = new org.jooq.Field<?>[names.length];
        for (int index = 0; index < names.length; index++) fields[index] = org.jooq.impl.DSL.field(names[index], types[index]);
        return org.jooq.impl.DSL.using(org.jooq.SQLDialect.POSTGRES).newResult(fields);
    }
    private void row(org.jooq.Result<org.jooq.Record> result, Object... values) {
        var record = org.jooq.impl.DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(result.fields());
        record.fromArray(values); result.add(record);
    }
    private void batchProjection(int count) {
        var dsl = mock(org.jooq.DSLContext.class); var context = mock(OperationsReadContext.class);
        when(context.scoped(eq(OperationsReadContext.Role.EVIDENCE), any())).thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(1)).get());
        var records = rows(new String[] {"id", "status", "session_id", "call_id"}, new Class<?>[] {UUID.class, String.class, UUID.class, String.class});
        for (int index = 0; index < count; index++) row(records, new UUID(0, index + 1), "PENDING", fact(1).sessionId(), fact(1).callId());
        var chunks = new ArrayList<List<UUID>>();
        when(dsl.fetch(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM evidence.evidence_record")) return records;
            Object[] binds = (Object[]) invocation.getRawArguments()[1]; var ids = Arrays.stream(binds).map(UUID.class::cast).toList(); chunks.add(ids);
            assertThat(ids.size()).isBetween(1, 1000); assertThat(sql).contains("GROUP BY evidence_id, artifact_role", "CASE artifact_role WHEN 'ORIGINAL' THEN 0 WHEN 'DERIVED' THEN 1 ELSE 2 END");
            var artifacts = rows(new String[] {"evidence_id", "artifact_role"}, new Class<?>[] {UUID.class, String.class});
            for (UUID id : ids) if ((id.getLeastSignificantBits() & 1) == 0) { row(artifacts, id, "DERIVED"); row(artifacts, id, "ORIGINAL"); row(artifacts, id, "DERIVED"); }
            return artifacts;
        });
        var reader = new com.onecall.core.evidence.internal.operationsread.JooqOperationsIvrEvidenceReader(dsl, context);
        var result = reader.find(fact(1).sessionId(), fact(1).callId()); assertThat(result).hasSize(count);
        assertThat(chunks).hasSize((count + 999) / 1000);
        assertThat(chunks.stream().flatMap(List::stream).toList()).containsExactlyElementsOf(result.stream().map(EvidenceFacts::evidenceId).toList());
        for (var fact : result) assertThat(fact.artifactRoles()).isEqualTo((fact.evidenceId().getLeastSignificantBits() & 1) == 0 ? List.of("ORIGINAL", "DERIVED") : List.of());
        verify(context).scoped(eq(OperationsReadContext.Role.EVIDENCE), any());
    }

    @Test
    void activeAdminAndOpsAreOnlyCurrentAuthorities() throws Exception {
        for (String role : List.of("ADMIN", "OPS")) { var fixture = new OperationsIvrSessionReadTestProviders(); when(fixture.employee.get("role", String.class)).thenReturn(role);
        assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items()).containsExactly(fact(1)); verify(fixture.dsl).fetchOne(anyString(), any(Object[].class)); }
    }
    @Test
    void wrongAudienceAndMissingEmployeeAreUnauthenticated() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); failure(() -> fixture.service.list(new com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller(EMPLOYEE, "wrong"), null), Code.UNAUTHENTICATED);
        when(fixture.dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(null); failure(() -> fixture.service.list(fixture.caller, null), Code.UNAUTHENTICATED);
    }
    @Test
    void disabledAuthorityIsForbiddenAndCorruptEmployeeFactsAreUnavailable() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); when(fixture.employee.get("status", String.class)).thenReturn("DISABLED"); failure(() -> fixture.service.list(fixture.caller, null), Code.FORBIDDEN);
        for (String role : Arrays.asList("VIEWER", null)) { when(fixture.employee.get("role", String.class)).thenReturn(role); failure(() -> fixture.service.list(fixture.caller, null), Code.SERVICE_UNAVAILABLE); }
        when(fixture.employee.get("role", String.class)).thenReturn("ADMIN"); when(fixture.employee.get("revision", Integer.class)).thenReturn(0); failure(() -> fixture.service.list(fixture.caller, null), Code.SERVICE_UNAVAILABLE);
        when(fixture.employee.get("revision", Integer.class)).thenReturn(1); when(fixture.employee.get("id", UUID.class)).thenReturn(UUID.randomUUID()); failure(() -> fixture.service.list(fixture.caller, null), Code.SERVICE_UNAVAILABLE);
    }
    @Test
    void authorityPrecedesParsingAndOwnerDependencyResolution() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); fixture.beans.destroySingleton("reader");
        failure(() -> fixture.service.list(null, null), Code.SERVICE_UNAVAILABLE);
        for (String audience : Arrays.asList(null, "", " ")) failure(() -> fixture.service.list(new com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller(EMPLOYEE, audience), null), Code.SERVICE_UNAVAILABLE);
        failure(() -> fixture.service.list(new com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller(null, "valid"), null), Code.SERVICE_UNAVAILABLE);
        when(fixture.employee.get("status", String.class)).thenReturn("DISABLED"); failure(() -> fixture.service.list(fixture.caller, null), Code.FORBIDDEN); verifyNoInteractions(fixture.reader, fixture.evidence);
    }
    @Test
    void listUsesLookaheadAndOnlyLastReturnedAnchor() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); when(fixture.reader.fetch(any(), any(), anyInt())).thenReturn(List.of(fact(1), fact(2), fact(3)));
        var result = fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("2")))); assertThat(result.items()).containsExactly(fact(1), fact(2)); assertThat(result.hasMore()).isTrue();
        var parsed = OperationsIvrSessionReadRequest.parse(raw(Map.of())); assertThat(new OperationsIvrSessionCursorCodec(properties()).decode(result.nextCursor(), parsed.digest())).isEqualTo(new Anchor(fact(2).createdAt(), fact(2).sessionId())); verify(fixture.reader).fetch(any(), eq(Optional.empty()), eq(3));
    }
    @Test
    void emptyAndTailPagesHaveExplicitNullCursor() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); for (var rows : List.of(List.<IvrSessionSummary>of(), List.of(fact(1)))) { when(fixture.reader.fetch(any(), any(), anyInt())).thenReturn(rows); var page = fixture.service.list(fixture.caller, raw(Map.of())); assertThat(page.nextCursor()).isNull(); assertThat(page.hasMore()).isFalse(); }
    }
    @Test
    void tiedInstantsUseUnsignedUuidDescendingOrder() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); var high = new IvrSessionSummary(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "high", null, "RECEIVED", "node", "v1", "v1", null, null, "***", TIME, null);
        var low = new IvrSessionSummary(new UUID(0, 1), "low", null, "RECEIVED", "node", "v1", "v1", null, null, "***", TIME, null);
        when(fixture.reader.fetch(any(), any(), anyInt())).thenReturn(List.of(high, low)); assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items()).containsExactly(high, low);
        when(fixture.reader.fetch(any(), any(), anyInt())).thenReturn(List.of(low, high)); failure(() -> fixture.service.list(fixture.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
    }
    @Test
    void continuationIsExclusiveAndLimitIsNotBound() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); byte[] digest = OperationsIvrSessionReadRequest.parse(raw(Map.of())).digest(); String cursor = new OperationsIvrSessionCursorCodec(properties()).encode(new Anchor(fact(1).createdAt(), fact(1).sessionId()), digest);
        when(fixture.reader.fetch(any(), any(), anyInt())).thenReturn(List.of(fact(2))); assertThat(fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of(cursor), "limit", List.of("100")))).items()).containsExactly(fact(2));
        when(fixture.reader.fetch(any(), any(), anyInt())).thenReturn(List.of(fact(1))); failure(() -> fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of(cursor)))), Code.SERVICE_UNAVAILABLE);
    }
    @Test
    void allFourFilterValuesReachOnlyExactReaderFilters() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); var query = Map.of("subjectId", List.of(EMPLOYEE.toString()), "callId", List.of(" exact "), "taskId", List.of("task"), "status", List.of("RECEIVED")); fixture.service.list(fixture.caller, raw(query));
        verify(fixture.reader).fetch(eq(new Filters(EMPLOYEE, " exact ", "task", "RECEIVED")), eq(Optional.empty()), eq(21)); verifyNoInteractions(fixture.evidence);
        var token = new OperationsIvrSessionCursorCodec(properties()).encode(new Anchor(TIME, EMPLOYEE), OperationsIvrSessionReadRequest.parse(raw(Map.of())).digest()); failure(() -> fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of(token), "taskId", List.of("changed")))), Code.CURSOR_FILTER_MISMATCH);
    }
    @Test
    void sessionProjectionHasExactRequiredAndOmittedOptionalProperties() throws Exception {
        var mapper = tools.jackson.databind.json.JsonMapper.builder().disable(tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build(); var json = mapper.valueToTree(fact(1)); assertThat(json.propertyNames()).containsExactlyInAnyOrder("sessionId", "callId", "status", "nodeId", "protocolVersion", "promptVersion", "accessNumberMasked", "createdAt"); assertThat(IvrSessionSummary.class.getRecordComponents()).hasSize(12);
    }
    @Test
    void detailRequiresExactlyOneMatchingCall() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); var root = fact(1);
        for (var calls : List.of(List.<CallFacts>of(), List.of(new CallFacts(root.sessionId(), "wrong", new IvrCallSummary("ACTIVE", null, null, null))), List.of(detailFact(root).calls().getFirst(), detailFact(root).calls().getFirst()))) { when(fixture.reader.detail(any())).thenReturn(Optional.of(new DetailFacts(root, calls, List.of(), List.of(), List.of()))); failure(() -> fixture.service.detail(fixture.caller, detail()), Code.SERVICE_UNAVAILABLE); }
    }
    @Test
    void nullableDecisionActionRemainsPresent() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); var root = fact(1); var decision = new IvrDecisionSummary(EMPLOYEE, "DIRECT_AUTHORIZATION", null, "DIRECT_AUTHORIZED", EMPLOYEE, TIME);
        when(fixture.reader.detail(any())).thenReturn(Optional.of(new DetailFacts(root, detailFact(root).calls(), List.of(new DecisionFacts(root.sessionId(), root.callId(), decision)), List.of(), List.of())));
        var result = fixture.service.detail(fixture.caller, detail()); var node = tools.jackson.databind.json.JsonMapper.builder().build().valueToTree(result); assertThat(node.path("decisions").get(0).has("action")).isTrue(); assertThat(node.path("decisions").get(0).path("action").isNull()).isTrue();
    }
    @Test
    void nestedArraysAndArtifactRolesAreDeterministic() throws Exception {
        for (int count : List.of(0, 1, 1000, 1001, 2000, 3000)) batchProjection(count);
        var fixture = new OperationsIvrSessionReadTestProviders(); var root = fact(1); when(fixture.evidence.find(any(), anyString())).thenReturn(List.of(new EvidenceFacts(new UUID(0, 1), "SOLIDIFIED", root.sessionId(), root.callId(), List.of("ORIGINAL", "DERIVED")), new EvidenceFacts(new UUID(0, 2), "PENDING", null, root.callId(), List.of())));
        assertThat(fixture.service.detail(fixture.caller, detail()).evidence()).extracting(IvrEvidenceSummary::artifactRoles).containsExactly(List.of("ORIGINAL", "DERIVED"), List.of());
        when(fixture.evidence.find(any(), anyString())).thenReturn(List.of(new EvidenceFacts(EMPLOYEE, "PENDING", root.sessionId(), root.callId(), List.of("DERIVED", "ORIGINAL")))); failure(() -> fixture.service.detail(fixture.caller, detail()), Code.SERVICE_UNAVAILABLE);
    }
    @Test
    void missingRootIsNotFoundAndEmptyArraysAreValid() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); var result = fixture.service.detail(fixture.caller, detail()); assertThat(result.decisions()).isEmpty(); assertThat(result.legs()).isEmpty(); assertThat(result.callEvents()).isEmpty(); assertThat(result.evidence()).isEmpty();
        when(fixture.reader.detail(any())).thenReturn(Optional.empty()); failure(() -> fixture.service.detail(fixture.caller, detail()), Code.NOT_FOUND);
    }
    @Test
    void inconsistentPersistedFactsAndLookaheadFailWholeRequest() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); var bad = new IvrSessionSummary(EMPLOYEE, "call", null, "CONFIRMED", "node", "v1", "v1", null, null, "***", TIME, null);
        when(fixture.reader.fetch(any(), any(), anyInt())).thenReturn(List.of(fact(1), bad)); failure(() -> fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("1")))), Code.SERVICE_UNAVAILABLE);
        when(fixture.evidence.find(any(), anyString())).thenReturn(List.of(new EvidenceFacts(EMPLOYEE, "PENDING", UUID.randomUUID(), "wrong", List.of()))); failure(() -> fixture.service.detail(fixture.caller, detail()), Code.SERVICE_UNAVAILABLE);
    }
    @Test
    void dependencyConfigurationAndDeadlineFailuresAreUnavailable() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); fixture.beans.destroySingleton("properties"); failure(() -> fixture.service.list(fixture.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
        doThrow(new org.jooq.exception.DataAccessException("PRIVATE jdbc:")).when(fixture.reader).detail(any()); failure(() -> fixture.service.detail(fixture.caller, detail()), Code.SERVICE_UNAVAILABLE);
        var service = new DefaultOperationsIvrSessionReadService(fixture.transaction, fixture.dsl, fixture.scope, fixture.beans.getBeanProvider(OperationsSubjectReadProperties.class), fixture.beans.getBeanProvider(IvrSessionReader.class), fixture.beans.getBeanProvider(IvrEvidenceReader.class), new java.util.function.LongSupplier() { long now; public long getAsLong() { return now += 6_000_000_000L; } }); failure(() -> service.list(fixture.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
    }
    @Test
    void unknownProgrammingFailuresAreInternalAndSanitized() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); for (Code code : Code.values()) { doThrow(new Failure(code)).when(fixture.reader).fetch(any(), any(), anyInt()); failure(() -> fixture.service.list(fixture.caller, raw(Map.of())), code); }
        doThrow(new IllegalStateException("PRIVATE")).when(fixture.reader).fetch(any(), any(), anyInt()); failure(() -> fixture.service.list(fixture.caller, raw(Map.of())), Code.INTERNAL);
    }
    @Test
    void singleSnapshotRoleCleanupAndZeroSideEffectsCoverEveryExit() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); fixture.service.list(fixture.caller, raw(Map.of())); var order = inOrder(fixture.scope, fixture.dsl, fixture.reader); order.verify(fixture.scope).enter(); order.verify(fixture.dsl).fetchOne(anyString(), any(Object[].class)); order.verify(fixture.reader).fetch(any(), any(), anyInt()); order.verify(fixture.scope).exit();
        doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(fixture.scope).exit(); failure(() -> fixture.service.list(fixture.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE); verify(fixture.connection).rollback();
    }
}
