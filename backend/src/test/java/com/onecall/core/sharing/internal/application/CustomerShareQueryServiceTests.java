package com.onecall.core.sharing.internal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.onecall.core.sharing.internal.persistence.ShareQueryRepository;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerShareQueryServiceTests {
    private final TrustedShareCaller caller = new TrustedShareCaller(UUID.randomUUID(), Optional.empty());
    private final SharePageRequest defaults = new SharePageRequest(Optional.empty(), Optional.empty());
    private ShareQueryRepository repository;
    private PlatformTransactionManager manager;
    private DSLContext context;
    private CustomerShareQueryService service;

    @BeforeEach
    void setup() {
        repository = mock(ShareQueryRepository.class);
        manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        context = mock(DSLContext.class);
        org.jooq.Record role = mock(org.jooq.Record.class);
        when(role.get(0, String.class)).thenReturn("sharing_role");
        when(context.fetchOne("select current_user")).thenReturn(role);
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setReadOnly(true);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        service = new CustomerShareQueryService(template, context, repository);
        when(repository.fetch(any(), any(), anyInt())).thenReturn(List.of());
    }

    @Test
    void defaultsAndBoundsAreDeterministic() {
        service.listCustomerShares(caller, defaults);
        verify(repository).fetch(caller, defaults, 21);
        for (int limit : List.of(1, 100)) {
            SharePageRequest request = new SharePageRequest(Optional.empty(), Optional.of(limit));
            service.listCustomerShares(caller, request);
            verify(repository).fetch(caller, request, limit + 1);
        }
        clearInvocations(repository);
        for (int limit : List.of(0, 101, -1)) {
            expect(ShareQueryException.Code.VALIDATION_FAILED, caller,
                    new SharePageRequest(Optional.empty(), Optional.of(limit)));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void trustedContextCannotBeOverridden() {
        service.listCustomerShares(caller, defaults);
        verify(repository).requireActiveCreatorContext(same(caller));
        verify(repository).fetch(same(caller), same(defaults), eq(21));
        clearInvocations(repository);
        expect(ShareQueryException.Code.INTERNAL, null, defaults);
        expect(ShareQueryException.Code.INTERNAL, new TrustedShareCaller(null, Optional.empty()), defaults);
        expect(ShareQueryException.Code.INTERNAL, new TrustedShareCaller(caller.accountId(), null), defaults);
        expect(ShareQueryException.Code.VALIDATION_FAILED, caller, null);
        expect(ShareQueryException.Code.VALIDATION_FAILED, caller, new SharePageRequest(null, Optional.empty()));
        expect(ShareQueryException.Code.VALIDATION_FAILED, caller, new SharePageRequest(Optional.empty(), null));
        verifyNoInteractions(repository);
    }

    @Test
    void cursorBindingAndAnchorValidationAreDeterministic() {
        for (ShareCursor cursor : List.of(
                new ShareCursor(UUID.randomUUID(), Optional.empty(), Instant.EPOCH, "id"),
                new ShareCursor(caller.accountId(), Optional.of(UUID.randomUUID()), Instant.EPOCH, "id"),
                new ShareCursor(caller.accountId(), null, Instant.EPOCH, "id"),
                new ShareCursor(null, Optional.empty(), Instant.EPOCH, "id"),
                new ShareCursor(caller.accountId(), Optional.empty(), null, "id"),
                new ShareCursor(caller.accountId(), Optional.empty(), Instant.MAX, "id"),
                new ShareCursor(caller.accountId(), Optional.empty(), Instant.ofEpochSecond(0, 1), "id"))) {
            expect(ShareQueryException.Code.CURSOR_INVALID, caller, request(cursor));
        }
        for (String id : Arrays.asList(null, "", "   ", "x".repeat(129), "\0", "\ud800")) {
            expect(ShareQueryException.Code.CURSOR_INVALID, caller,
                    request(new ShareCursor(caller.accountId(), Optional.empty(), Instant.EPOCH, id)));
        }
        verifyNoInteractions(repository);
        ShareCursor valid = new ShareCursor(caller.accountId(), Optional.empty(), Instant.EPOCH, "😀".repeat(128));
        service.listCustomerShares(caller, request(valid));
        verify(repository).fetch(eq(caller), eq(request(valid)), eq(21));
    }

    @Test
    void lookaheadUsesLastReturnedRow() {
        for (int limit : List.of(20, 100)) {
            List<ShareSummary> rows = new ArrayList<>();
            for (int index = 0; index <= limit; index++) { rows.add(summary("id-" + index)); }
            when(repository.fetch(any(), any(), eq(limit + 1))).thenReturn(rows);
            SharePage page = service.listCustomerShares(caller, new SharePageRequest(Optional.empty(), Optional.of(limit)));
            assertThat(page.items()).hasSize(limit);
            assertThat(page.hasMore()).isTrue();
            assertThat(page.nextCursor().orElseThrow().shareId()).isEqualTo("id-" + (limit - 1));
            assertThat(page.nextCursor().orElseThrow().validFrom()).isEqualTo(Instant.EPOCH);
            rows.clear();
            assertThat(page.items()).hasSize(limit);
            assertThatThrownBy(() -> page.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void emptyAndTailPagesHaveNoCursor() {
        for (int count : List.of(0, 1, 20)) {
            when(repository.fetch(any(), any(), anyInt())).thenReturn(Collections.nCopies(count, summary("id")));
            SharePage page = service.listCustomerShares(caller, defaults);
            assertThat(page.items()).hasSize(count);
            assertThat(page.hasMore()).isFalse();
            assertThat(page.nextCursor()).isEmpty();
        }
    }

    @Test
    void projectionComponentsMatchFrozenListing() {
        assertThat(Arrays.stream(ShareSummary.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("shareId", "evidenceId", "verificationMode", "status", "validFrom", "expiresAt",
                        "maxAdmissions", "admissionsUsed", "downloadAllowed", "codeGeneration", "revocationGeneration");
        assertThat(Arrays.stream(ShareSummary.class.getRecordComponents()).map(component -> component.getType()).toList())
                .isEqualTo(List.of(String.class, UUID.class, ShareSummary.VerificationMode.class, ShareSummary.ShareStatus.class,
                        Instant.class, Instant.class, Integer.class, int.class, boolean.class, int.class, int.class));
        assertThat(ShareSummary.VerificationMode.values()).extracting(Enum::name)
                .containsExactly("NONE", "EXTRACTION_CODE", "DESIGNATED_MOBILE", "ORGANIZATION_MEMBER");
        assertThat(ShareSummary.ShareStatus.values()).extracting(Enum::name).containsExactly("ACTIVE", "EXPIRED", "REVOKED", "LOCKED");
        assertThat(summary("id").maxAdmissions()).isNull();
    }

    @Test
    void creatorContextDenialDoesNotFetchShares() {
        doThrow(new ShareQueryException(ShareQueryException.Code.FORBIDDEN)).when(repository).requireActiveCreatorContext(caller);
        expect(ShareQueryException.Code.FORBIDDEN, caller, defaults);
        verify(repository, never()).fetch(any(), any(), anyInt());
        verify(manager).rollback(any());
    }

    @Test
    void technicalFailuresReturnNoPartialPageOrSensitiveText() {
        Logger logger = (Logger) LoggerFactory.getLogger(CustomerShareQueryService.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            when(context.execute("SET LOCAL ROLE sharing_role")).thenThrow(new org.jooq.exception.DataAccessException(
                    "CANARY SQL credentials", new SQLException("CANARY", "42501")));
            expect(ShareQueryException.Code.SERVICE_UNAVAILABLE, caller, defaults);
            reset(context);
            org.jooq.Record role = mock(org.jooq.Record.class);
            when(role.get(0, String.class)).thenReturn("sharing_role");
            when(context.fetchOne("select current_user")).thenReturn(role);
            when(repository.fetch(any(), any(), anyInt())).thenThrow(new org.jooq.exception.DataAccessException(
                    "CANARY", new SQLException("CANARY", "08006")));
            expect(ShareQueryException.Code.SERVICE_UNAVAILABLE, caller, defaults);
            for (RuntimeException poisoned : List.of(new IllegalArgumentException("CANARY unknown mapping"),
                    new NullPointerException("CANARY null mapping"))) {
                doThrow(poisoned).when(repository).fetch(any(), any(), anyInt());
                expect(ShareQueryException.Code.INTERNAL, caller, defaults);
            }
            assertThat(appender.list).hasSize(4);
            assertThat(appender.list.get(0).getFormattedMessage()).contains("sqlState=Optional[42501]", "category=42", "roleAssertionPassed=false");
            assertThat(appender.list.get(1).getFormattedMessage()).contains("sqlState=Optional[08006]", "category=08", "roleAssertionPassed=true");
            for (var event : appender.list) {
                assertThat(event.getThrowableProxy()).isNull();
                assertThat(event.getFormattedMessage()).doesNotContain("CANARY", caller.accountId().toString(), "SELECT");
                assertThat(event.getArgumentArray()).hasSize(1);
                assertThat(event.getArgumentArray()[0]).isInstanceOf(ShareQueryDiagnostic.class);
            }
            verify(manager, times(4)).rollback(any());
            verify(manager, never()).commit(any());
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    private void expect(ShareQueryException.Code code, TrustedShareCaller supplied, SharePageRequest request) {
        assertThatThrownBy(() -> service.listCustomerShares(supplied, request)).isInstanceOfSatisfying(ShareQueryException.class,
                failure -> { assertThat(failure.code()).isEqualTo(code); assertThat(failure.getCause()).isNull();
                    assertThat(failure.getMessage()).isEqualTo(new ShareQueryException(code).getMessage()); });
    }

    private SharePageRequest request(ShareCursor cursor) { return new SharePageRequest(Optional.of(cursor), Optional.empty()); }

    private ShareSummary summary(String id) {
        return new ShareSummary(id, UUID.randomUUID(), ShareSummary.VerificationMode.NONE, ShareSummary.ShareStatus.ACTIVE,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(3600), null, 0, false, 0, 0);
    }
}
