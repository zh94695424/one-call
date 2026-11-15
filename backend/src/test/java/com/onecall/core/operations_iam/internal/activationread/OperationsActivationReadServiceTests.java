package com.onecall.core.operations_iam.internal.activationread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsActivationReadTestProviders.*;
import com.onecall.archtests.operations.OperationsActivationReadTestProviders;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class OperationsActivationReadServiceTests {
    private void failure(Code code, Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, problem -> assertThat(problem.code()).isEqualTo(code));
    }
    @Test
    void currentActiveAdminAndOpsAreOnlyAcceptedAuthorities() throws Exception {
        for (String role : List.of("ADMIN", "OPS", "OTHER")) {
            var fixture = new OperationsActivationReadTestProviders(); when(fixture.employee.get("role", String.class)).thenReturn(role);
            if ("OTHER".equals(role)) failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of())));
            else assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items()).hasSize(1);
        }
        for (String corrupt : List.of("missing", "disabled", "status", "revision", "identity", "audience")) {
            var fixture = new OperationsActivationReadTestProviders();
            switch (corrupt) {
                case "missing" -> when(fixture.dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(null);
                case "disabled" -> when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
                case "status" -> when(fixture.employee.get("status", String.class)).thenReturn(null);
                case "revision" -> when(fixture.employee.get("revision", Integer.class)).thenReturn(0);
                case "identity" -> when(fixture.employee.get("id", UUID.class)).thenReturn(UUID.randomUUID());
                default -> { }
            }
            Code expected = "missing".equals(corrupt) || "audience".equals(corrupt) ? Code.UNAUTHENTICATED : "disabled".equals(corrupt) ? Code.FORBIDDEN : Code.SERVICE_UNAVAILABLE;
            var caller = "audience".equals(corrupt) ? new com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller(EMPLOYEE, "customer") : fixture.caller;
            failure(expected, () -> fixture.service.list(caller, raw(Map.of()))); verifyNoInteractions(fixture.reader);
        }
    }
    @Test
    void authorityRunsBeforeParsingKeysAndReaderResolution() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); fixture.beans.destroySingleton("properties"); fixture.beans.destroySingleton("reader");
        when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
        failure(Code.FORBIDDEN, () -> fixture.service.list(fixture.caller, null));
        var order = inOrder(fixture.scope, fixture.dsl); order.verify(fixture.scope).enter(); order.verify(fixture.dsl).fetchOne(anyString(), any(Object[].class)); order.verify(fixture.scope).exit();
        verifyNoInteractions(fixture.reader);
    }
    @Test
    void emptyAndSinglePagesHaveNoCursor() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        for (var rows : List.of(List.<TransitionFacts>of(), List.of(fact(1)))) {
            when(fixture.reader.fetch(any(), anyInt())).thenReturn(rows); var page = fixture.service.list(fixture.caller, raw(Map.of()));
            assertThat(page.items()).hasSize(rows.size()); assertThat(page.hasMore()).isFalse(); assertThat(page.nextCursor()).isNull();
            assertThatThrownBy(() -> page.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        }
    }
    @Test
    void exactLimitTailAndLookaheadUseLastReturnedAnchor() throws Exception {
        for (int limit : List.of(1, 20, 100)) for (boolean extra : List.of(false, true)) {
            var fixture = new OperationsActivationReadTestProviders(); var rows = java.util.stream.IntStream.rangeClosed(1, limit + (extra ? 1 : 0)).mapToObj(OperationsActivationReadTestProviders::fact).toList();
            when(fixture.reader.fetch(any(), anyInt())).thenReturn(rows); var page = fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("" + limit))));
            assertThat(page.items()).hasSize(limit); assertThat(page.hasMore()).isEqualTo(extra); verify(fixture.reader).fetch(Optional.empty(), limit + 1);
            if (extra) assertThat(new OperationsActivationCursorCodec(properties()).decode(page.nextCursor())).isEqualTo(new Anchor(fact(limit).occurredAt(), fact(limit).transitionId())); else assertThat(page.nextCursor()).isNull();
        }
    }
    @Test
    void tiedInstantsUseUnsignedNativeUuidDescendingOrder() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        var rows = List.of(with(fact(1), "transitionId", new UUID(Long.MIN_VALUE, 0)), with(fact(1), "transitionId", new UUID(Long.MAX_VALUE, -1)));
        when(fixture.reader.fetch(any(), anyInt())).thenReturn(rows); assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items()).hasSize(2);
        for (var invalid : List.of(List.of(rows.getLast(), rows.getFirst()), List.of(rows.getFirst(), rows.getFirst()))) {
            when(fixture.reader.fetch(any(), anyInt())).thenReturn(invalid); failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of())));
        }
    }
    @Test
    void continuationIsExclusiveStableAndIndependentOfLimit() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); var anchor = new Anchor(TIME, UUID.randomUUID()); String cursor = new OperationsActivationCursorCodec(properties()).encode(anchor);
        for (int limit : List.of(1, 20, 100)) {
            fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("" + limit), "cursor", List.of(cursor)))); verify(fixture.reader).fetch(Optional.of(anchor), limit + 1);
        }
        when(fixture.reader.fetch(any(), anyInt())).thenReturn(List.of()); assertThat(fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of(cursor)))).nextCursor()).isNull();
        when(fixture.reader.fetch(any(), anyInt())).thenReturn(List.of(with(fact(1), "occurredAt", TIME.plusSeconds(1)))); failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of(cursor)))));
    }
    @Test
    void allPersistableStatusChannelAndDomainCombinationsProjectExactly() throws Exception {
        for (String from : List.of("INACTIVE", "ACTIVE", "SUSPENDED")) for (String channel : List.of("WEB", "MINIAPP", "IVR", "OPERATIONS")) {
            if ("SUSPENDED".equals(from) && !"OPERATIONS".equals(channel)) continue;
            var fixture = new OperationsActivationReadTestProviders(); var value = shape(from, channel);
            when(fixture.reader.fetch(any(), anyInt())).thenReturn(List.of(value));
            boolean legal = "INACTIVE".equals(from) != "OPERATIONS".equals(channel);
            if (legal) {
                var page = fixture.service.list(fixture.caller, raw(Map.of())); assertThat(page.items()).hasSize(1);
                var item = page.items().getFirst();
                assertThat(Arrays.stream(item.getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getType)).containsExactly(UUID.class, UUID.class, String.class, String.class, String.class, int.class, int.class, String.class, String.class, String.class, Instant.class);
                assertThat(item.transitionId()).isEqualTo(value.transitionId());
                assertThat(item.subjectId()).isEqualTo(value.subjectId());
                assertThat(item.statusEventId()).isEqualTo(value.statusEventId());
                assertThat(item.fromStatus()).isEqualTo(value.fromStatus());
                assertThat(item.toStatus()).isEqualTo(value.toStatus());
                assertThat(item.fromRevision()).isEqualTo(value.fromRevision().intValue());
                assertThat(item.toRevision()).isEqualTo(value.toRevision().intValue());
                assertThat(item.sourceChannel()).isEqualTo(value.sourceChannel());
                assertThat(item.sourceActionDomain()).isEqualTo(value.sourceActionDomain());
                assertThat(item.reasonCode()).isEqualTo(value.reasonCode());
                assertThat(item.occurredAt()).isEqualTo(value.occurredAt());
            } else failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of())));
        }
        for (String text : List.of("\t", "😀".repeat(64))) {
            var fixture = new OperationsActivationReadTestProviders(); var value = with(with(with(fact(1), "reasonCode", text), "statusEventId", "😀".repeat(128)), "fromRevision", Integer.MAX_VALUE - 1); value = with(value, "toRevision", Integer.MAX_VALUE);
            when(fixture.reader.fetch(any(), anyInt())).thenReturn(List.of(value)); assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items().getFirst().reasonCode()).isEqualTo(text);
        }
    }
    @Test
    void nullMalformedAndInvalidLookaheadRowsFailClosed() throws Exception {
        var malformed = new ArrayList<TransitionFacts>(); malformed.add(null);
        for (var field : TransitionFacts.class.getRecordComponents()) malformed.add(with(fact(2), field.getName(), null));
        for (var entry : Map.<String, List<Object>>of("fromRevision", List.of(-1, Integer.MAX_VALUE), "toRevision", List.of(0, 3), "fromStatus", List.of("OTHER", "ACTIVE"), "toStatus", List.of("INACTIVE"), "reasonCode", List.of("", " ", "😀".repeat(65)), "statusEventId", List.of("", " ", "😀".repeat(129)), "sourceChannel", List.of("OTHER"), "sourceActionDomain", List.of("OTHER"), "occurredAt", List.of(Instant.ofEpochSecond(0, 1))).entrySet()) for (Object value : entry.getValue()) malformed.add(with(fact(2), entry.getKey(), value));
        malformed.add(shape("INACTIVE", "OPERATIONS")); for (String channel : List.of("WEB", "MINIAPP", "IVR")) malformed.add(shape("ACTIVE", channel));
        for (TransitionFacts invalid : malformed) {
            var fixture = new OperationsActivationReadTestProviders(); when(fixture.reader.fetch(any(), anyInt())).thenReturn(Arrays.asList(fact(1), invalid));
            failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("1")))));
            var response = fixture.mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(BASE).servletPath(BASE).header("Authorization", "Bearer valid").param("limit", "1")).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(503); assertThat(response.getContentAsString()).doesNotContain("items", "nextCursor", "event-");
        }
        var fixture = new OperationsActivationReadTestProviders(); when(fixture.reader.fetch(any(), anyInt())).thenReturn(null); failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of())));
        when(fixture.reader.fetch(any(), anyInt())).thenReturn(List.of(fact(1), fact(2), fact(3))); failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("1")))));
    }
    @Test
    void missingAmbiguousAndUncreatableReadersAreUnavailable() throws Exception {
        for (int count : List.of(0, 2, 3)) {
            var fixture = new OperationsActivationReadTestProviders(); fixture.beans.destroySingleton("reader");
            if (count == 2) { fixture.beans.registerSingleton("first", fixture.reader); fixture.beans.registerSingleton("second", mock(ActivationHistoryReader.class)); }
            if (count == 3) { var definition = new RootBeanDefinition(ActivationHistoryReader.class); definition.setInstanceSupplier(() -> { throw new IllegalStateException("PRIVATE"); }); fixture.beans.registerBeanDefinition("broken", definition); }
            failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of()))); verifyNoInteractions(fixture.reader);
        }
    }
    @Test
    void cursorConfigurationAndDecodeFailuresPrecedeOwnerRead() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); fixture.beans.destroySingleton("properties");
        failure(Code.VALIDATION_FAILED, () -> fixture.service.list(fixture.caller, raw(Map.of("unknown", List.of("x")))));
        failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of("")))));
        fixture.beans.registerSingleton("properties", properties());
        for (String cursor : List.of("", "x".repeat(513), "tampered")) failure(Code.CURSOR_INVALID, () -> fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of(cursor)))));
        verifyNoInteractions(fixture.reader);
    }
    @Test
    void singleReadOnlyRepeatableReadTransactionEnclosesAllWork() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        when(fixture.reader.fetch(any(), anyInt())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue(); assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue(); assertThat(TransactionSynchronizationManager.hasResource(fixture.source)).isTrue(); return List.of(fact(1));
        }); fixture.service.list(fixture.caller, raw(Map.of()));
        var order = inOrder(fixture.scope, fixture.dsl, fixture.reader); order.verify(fixture.scope).enter(); order.verify(fixture.dsl).fetchOne(anyString(), any(Object[].class)); order.verify(fixture.reader).fetch(Optional.empty(), 21); order.verify(fixture.scope).exit();
        verify(fixture.connection).setReadOnly(true); verify(fixture.connection).setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ); verify(fixture.connection).commit(); verify(fixture.connection).close();
    }
    @Test
    void deadlineDependencyAndTransactionFailuresAreUnavailable() throws Exception {
        for (int tick = 0; tick < 10; tick++) {
            var fixture = new OperationsActivationReadTestProviders(); var calls = new AtomicLong(); int expire = tick;
            var service = new DefaultOperationsActivationReadService(fixture.transaction, fixture.dsl, fixture.scope, fixture.beans.getBeanProvider(OperationsSubjectReadProperties.class), fixture.beans.getBeanProvider(ActivationHistoryReader.class), () -> calls.getAndIncrement() <= expire ? 0 : 6000000000L);
            failure(Code.SERVICE_UNAVAILABLE, () -> service.list(fixture.caller, raw(Map.of())));
        }
        for (String stage : List.of("query", "acquire", "commit", "rollback")) {
            var fixture = new OperationsActivationReadTestProviders();
            switch (stage) {
                case "query" -> when(fixture.dsl.fetchOne(anyString(), any(Object[].class))).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE"));
                case "acquire" -> when(fixture.source.getConnection()).thenThrow(new java.sql.SQLException("PRIVATE"));
                case "commit" -> doThrow(new java.sql.SQLException("PRIVATE")).when(fixture.connection).commit();
                case "rollback" -> { when(fixture.reader.fetch(any(), anyInt())).thenThrow(new Failure(Code.SERVICE_UNAVAILABLE)); doThrow(new java.sql.SQLException("PRIVATE")).when(fixture.connection).rollback(); }
            }
            failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of())));
        }
    }
    @Test
    void roleAndContextCleanupRunsOnEveryExit() throws Exception {
        for (String stage : List.of("enter", "authority", "parse", "cursor", "reader", "map", "success", "exit")) {
            var fixture = new OperationsActivationReadTestProviders(); var query = new HashMap<String, List<String>>();
            switch (stage) {
                case "enter" -> doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(fixture.scope).enter();
                case "authority" -> when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
                case "parse" -> query.put("unknown", List.of("x"));
                case "cursor" -> query.put("cursor", List.of("bad"));
                case "reader" -> when(fixture.reader.fetch(any(), anyInt())).thenThrow(new Failure(Code.SERVICE_UNAVAILABLE));
                case "map" -> when(fixture.reader.fetch(any(), anyInt())).thenReturn(Arrays.asList((TransitionFacts) null));
                case "exit" -> doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(fixture.scope).exit();
            }
            Code expected = switch (stage) { case "authority" -> Code.FORBIDDEN; case "parse" -> Code.VALIDATION_FAILED; case "cursor" -> Code.CURSOR_INVALID; default -> Code.SERVICE_UNAVAILABLE; };
            if ("success".equals(stage)) fixture.service.list(fixture.caller, raw(query)); else failure(expected, () -> fixture.service.list(fixture.caller, raw(query)));
            verify(fixture.scope).exit(); verify(fixture.connection).close();
        }
    }
    @Test
    void unknownAndOutOfContractTypedFailuresBecomeInternal() throws Exception {
        for (Code code : Code.values()) {
            var fixture = new OperationsActivationReadTestProviders(); when(fixture.reader.fetch(any(), anyInt())).thenThrow(new Failure(code));
            var allowed = Set.of(Code.CURSOR_INVALID, Code.UNAUTHENTICATED, Code.FORBIDDEN, Code.VALIDATION_FAILED, Code.RATE_LIMITED, Code.SERVICE_UNAVAILABLE, Code.INTERNAL);
            failure(allowed.contains(code) ? code : Code.INTERNAL, () -> fixture.service.list(fixture.caller, raw(Map.of())));
        }
        var fixture = new OperationsActivationReadTestProviders(); when(fixture.reader.fetch(any(), anyInt())).thenThrow(new IllegalStateException("PRIVATE")); failure(Code.INTERNAL, () -> fixture.service.list(fixture.caller, raw(Map.of())));
    }
    private TransitionFacts shape(String from, String channel) throws Exception {
        var value = with(with(fact(2), "fromStatus", from), "toStatus", "ACTIVE".equals(from) ? "SUSPENDED" : "ACTIVE");
        return with(with(value, "sourceChannel", channel), "sourceActionDomain", "OPERATIONS".equals(channel) ? "OPERATIONS" : "IVR".equals(channel) ? "IVR_CONFIRM" : "ACTIVATION");
    }
    private TransitionFacts with(TransitionFacts original, String name, Object value) throws Exception {
        var fields = TransitionFacts.class.getRecordComponents(); var arguments = new Object[fields.length];
        for (int index = 0; index < fields.length; index++) arguments[index] = fields[index].getName().equals(name) ? value : fields[index].getAccessor().invoke(original);
        return TransitionFacts.class.getDeclaredConstructor(Arrays.stream(fields).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new)).newInstance(arguments);
    }
}
