package com.onecall.core.customer_iam.internal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.*;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import java.time.Instant;
import java.util.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerEvidenceQueryServiceTests {
    private final UUID account = UUID.randomUUID();
    private final UUID subject = UUID.randomUUID();
    private final UUID identifier = UUID.randomUUID();
    private final Instant time = Instant.parse("2026-11-12T00:00:00.123456Z");
    private final TrustedCustomerCaller caller = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB,
            new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
    private final TransactionTemplate transaction = mock(TransactionTemplate.class);
    private final DSLContext context = mock(DSLContext.class);
    private final JooqCustomerBusinessNumberAccessRepository lifecycle = mock(JooqCustomerBusinessNumberAccessRepository.class);
    private final CustomerSubjectAccessReader subjects = mock(CustomerSubjectAccessReader.class);
    private final CustomerEvidenceReader evidence = mock(CustomerEvidenceReader.class);
    private CustomerEvidenceQueryService service;
    private Header header(UUID id, String status) {
        return new Header(id, subject, status, "CALL_RECORDING", time.toString(), "v1", time,
                status.equals("SOLIDIFIED") ? time : null);
    }
    private CustomerEvidenceQueryService.Request request() { return new CustomerEvidenceQueryService.Request(Optional.empty(), Optional.empty()); }
    private Optional<CustomerEvidenceQueryService.Detail> detail() { return service.getCustomerEvidence(caller, identifier); }
    private CustomerEvidenceQueryService.Page page() { return service.listCustomerEvidence(caller, request()); }
    private void failure(CustomerContextException.Code code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomerContextException.class,
                problem -> { assertThat(problem.code()).isEqualTo(code); assertThat(problem).hasNoCause(); });
    }
    @BeforeEach void setup() {
        when(transaction.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                .doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
        var role = mock(org.jooq.Record.class); when(role.get(0, String.class)).thenReturn("customer_iam_role");
        when(context.fetchOne("select current_user")).thenReturn(role);
        when(subjects.findOwnedSubjectIds(account, null)).thenReturn(Set.of(subject));
        when(subjects.findOwnedSubject(subject, account, null)).thenReturn(Optional.of(subject));
        when(evidence.findSubjectIdsWithEvidence(Set.of(subject))).thenReturn(Set.of(subject));
        when(evidence.fetchPage(Set.of(subject), Optional.empty(), 21)).thenReturn(List.of(header(identifier, "PENDING")));
        when(evidence.findHeader(identifier)).thenReturn(Optional.of(header(identifier, "PENDING")));
        when(evidence.findArtifactRoles(Set.of(identifier))).thenReturn(Map.of());
        when(evidence.findArtifacts(identifier)).thenReturn(List.of());
        service = new CustomerEvidenceQueryService(transaction, context, lifecycle, subjects, evidence);
    }

    @Test void requestsRequireTrustedScopeAndExactLimitAndAnchorBounds() {
        assertThat(request().effectiveLimit()).isEqualTo(20);
        for (int limit : List.of(1, 20, 100)) assertThat(new CustomerEvidenceQueryService.Request(Optional.empty(), Optional.of(limit)).effectiveLimit()).isEqualTo(limit);
        for (int limit : List.of(-1, 0, 101, Integer.MAX_VALUE)) failure(CustomerContextException.Code.VALIDATION_FAILED,
                () -> new CustomerEvidenceQueryService.Request(Optional.empty(), Optional.of(limit)));
        failure(CustomerContextException.Code.VALIDATION_FAILED, () -> new CustomerEvidenceQueryService.Request(null, Optional.empty()));
        failure(CustomerContextException.Code.VALIDATION_FAILED, () -> new CustomerEvidenceQueryService.Request(Optional.empty(), null));
        for (Anchor anchor : List.of(new Anchor(null, identifier), new Anchor(time, null), new Anchor(time.plusNanos(1), identifier)))
            failure(CustomerContextException.Code.CURSOR_INVALID, () -> new CustomerEvidenceQueryService.Request(Optional.of(anchor), Optional.empty()));
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.listCustomerEvidence(null, request()));
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.getCustomerEvidence(new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB, null), identifier));
        failure(CustomerContextException.Code.VALIDATION_FAILED, () -> service.getCustomerEvidence(caller, null));
        failure(CustomerContextException.Code.VALIDATION_FAILED, () -> service.listCustomerEvidence(caller, null));
    }

    @Test void roleAssumptionAssertionAndLifecyclePrecedeEveryOwnerRead() {
        page(); var ordered = inOrder(context, lifecycle, subjects, evidence);
        ordered.verify(context).execute("SET LOCAL ROLE customer_iam_role"); ordered.verify(context).fetchOne("select current_user");
        ordered.verify(lifecycle).requireActiveContext(any()); ordered.verify(subjects).findOwnedSubjectIds(account, null);
        clearInvocations(subjects, evidence);
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(lifecycle).requireActiveContext(any());
        failure(CustomerContextException.Code.FORBIDDEN, this::page); verifyNoInteractions(subjects, evidence);
        when(context.fetchOne("select current_user")).thenReturn(null);
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::detail); verifyNoInteractions(subjects, evidence);
    }

    @Test void listUsesOwnedEvidenceSubjectsAndChecksAllTheirBindingsBeforePage() {
        UUID offPage = UUID.randomUUID(); UUID empty = UUID.randomUUID();
        when(subjects.findOwnedSubjectIds(account, null)).thenReturn(Set.of(subject, offPage, empty));
        when(evidence.findSubjectIdsWithEvidence(Set.of(subject, offPage, empty))).thenReturn(Set.of(subject, offPage));
        when(subjects.hasOpenRisk(Set.of(subject, offPage))).thenReturn(true);
        failure(CustomerContextException.Code.FORBIDDEN, this::page);
        verify(evidence, never()).fetchPage(anySet(), any(), anyInt());
        verify(subjects, never()).hasOpenRisk(Set.of(empty));
        var ordered = inOrder(subjects, evidence); ordered.verify(subjects).findOwnedSubjectIds(account, null);
        ordered.verify(evidence).findSubjectIdsWithEvidence(Set.of(subject, offPage, empty));
        ordered.verify(subjects).hasOpenRisk(Set.of(subject, offPage));
    }

    @Test void detailMissingUnownedAndForeignEvidenceNeverReadsRiskOrArtifacts() {
        when(evidence.findHeader(identifier)).thenReturn(Optional.empty()); assertThat(detail()).isEmpty();
        verifyNoInteractions(subjects);
        when(evidence.findHeader(identifier)).thenReturn(Optional.of(header(identifier, "PENDING")));
        when(subjects.findOwnedSubject(subject, account, null)).thenReturn(Optional.empty()); assertThat(detail()).isEmpty();
        verify(subjects, never()).hasOpenRisk(org.mockito.ArgumentMatchers.<UUID>any());
        verify(evidence, never()).findArtifacts(any());
    }

    @Test void allFourStatusesAndImmutableCaptureFactsComposeWithoutLiveIdentity() {
        for (String status : List.of("PENDING", "SOLIDIFIED", "QUARANTINED", "FAILED")) {
            when(evidence.findHeader(identifier)).thenReturn(Optional.of(header(identifier, status)));
            assertThat(detail().orElseThrow().status()).isEqualTo(status); assertThat(detail().orElseThrow().capturedAt()).isEqualTo(time);
            when(evidence.fetchPage(Set.of(subject), Optional.empty(), 21)).thenReturn(List.of(header(identifier, status)));
            assertThat(page().items().getFirst().status()).isEqualTo(status);
        }
    }

    @Test void pageLookaheadAndDescendingUuidTiesUseLastReturnedAnchor() {
        UUID high = UUID.fromString("80000000-0000-0000-0000-000000000001");
        UUID low = UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");
        when(evidence.fetchPage(Set.of(subject), Optional.empty(), 2)).thenReturn(List.of(header(high, "PENDING"), header(low, "FAILED")));
        when(evidence.findArtifactRoles(Set.of(high))).thenReturn(Map.of());
        var result = service.listCustomerEvidence(caller, new CustomerEvidenceQueryService.Request(Optional.empty(), Optional.of(1)));
        assertThat(result.items()).extracting(CustomerEvidenceQueryService.Summary::evidenceId).containsExactly(high);
        assertThat(result.nextAnchor()).contains(new Anchor(time, high)); assertThat(result.hasMore()).isTrue();
        verify(evidence).findArtifactRoles(Set.of(high));
    }

    @Test void emptyAndTailPagesHaveNoCursorAndBatchOnlyReturnedArtifactRoles() {
        var tail = page(); assertThat(tail.hasMore()).isFalse(); assertThat(tail.nextAnchor()).isEmpty();
        assertThat(tail.items().getFirst().artifactRoles()).isEmpty(); verify(evidence).findArtifactRoles(Set.of(identifier));
        clearInvocations(evidence); when(subjects.findOwnedSubjectIds(account, null)).thenReturn(Set.of());
        var empty = page(); assertThat(empty.items()).isEmpty(); assertThat(empty.nextAnchor()).isEmpty(); assertThat(empty.hasMore()).isFalse();
        verifyNoInteractions(evidence);
    }

    @Test void artifactRolesAndSafeFactsUseFixedRankAndSourceValidatedDuration() {
        when(evidence.findArtifactRoles(Set.of(identifier))).thenReturn(Map.of(identifier, List.of("DERIVED", "ORIGINAL", "DERIVED")));
        assertThat(page().items().getFirst().artifactRoles()).containsExactly("ORIGINAL", "DERIVED");
        var original = new Artifact("ORIGINAL", "audio/wav", 100L, 37, "a".repeat(64));
        var derived = new Artifact("DERIVED", "audio/wav", 100L, null, "a".repeat(64));
        when(evidence.findArtifacts(identifier)).thenReturn(List.of(derived, original, derived));
        assertThat(detail().orElseThrow().artifacts()).containsExactly(original, derived, derived);
    }

    @Test void unrepresentablePersistedDetailIsUnavailableAndInvalidListFactsAreInternal() {
        for (String capture : Arrays.asList(null, "", "bad", "2026-11-12T00:00:00")) {
            var invalid = new Header(identifier, subject, "PENDING", "CALL_RECORDING", capture, "v1", time, null);
            when(evidence.findHeader(identifier)).thenReturn(Optional.of(invalid)); assertThat(detail()).isEmpty();
            when(evidence.fetchPage(Set.of(subject), Optional.empty(), 21)).thenReturn(List.of(invalid)); failure(CustomerContextException.Code.INTERNAL, this::page);
        }
        when(evidence.findHeader(identifier)).thenReturn(Optional.of(header(identifier, "PENDING")));
        for (Artifact invalid : List.of(new Artifact("ORIGINAL", "x".repeat(129), 1L, null, "a".repeat(64)),
                new Artifact("DERIVED", "audio/wav", 1L, 37, "a".repeat(64)), new Artifact("ORIGINAL", "audio/wav", -1L, null, "a".repeat(64)),
                new Artifact("ORIGINAL", "audio/wav", 1L, null, "A".repeat(64)))) {
            when(evidence.findArtifacts(identifier)).thenReturn(List.of(invalid)); assertThat(detail()).isEmpty();
        }
    }

    @Test void nullExtraDuplicateMisboundAndNonmonotonicOwnerResultsFailClosed() {
        when(evidence.findHeader(identifier)).thenReturn(null); failure(CustomerContextException.Code.INTERNAL, this::detail);
        when(evidence.findHeader(identifier)).thenReturn(Optional.of(header(UUID.randomUUID(), "PENDING"))); failure(CustomerContextException.Code.INTERNAL, this::detail);
        when(evidence.fetchPage(Set.of(subject), Optional.empty(), 21)).thenReturn(List.of(header(identifier, "PENDING"), header(identifier, "PENDING")));
        failure(CustomerContextException.Code.INTERNAL, this::page);
        when(evidence.fetchPage(Set.of(subject), Optional.empty(), 21)).thenReturn(null); failure(CustomerContextException.Code.INTERNAL, this::page);
        when(evidence.fetchPage(Set.of(subject), Optional.empty(), 21)).thenReturn(List.of(header(identifier, "PENDING")));
        when(evidence.findArtifactRoles(Set.of(identifier))).thenReturn(Map.of(UUID.randomUUID(), List.of())); failure(CustomerContextException.Code.INTERNAL, this::page);
        when(evidence.findSubjectIdsWithEvidence(Set.of(subject))).thenReturn(Set.of(UUID.randomUUID())); failure(CustomerContextException.Code.INTERNAL, this::page);
    }

    @Test void sqlBeanTransactionAndCompletionFailuresNeverReturnPartialData() {
        for (RuntimeException exception : List.of(new org.jooq.exception.DataAccessException("SQL_CANARY"),
                new org.springframework.beans.factory.BeanCreationException("SECRET_CANARY"), new org.springframework.transaction.TransactionSystemException("COMMIT_CANARY"),
                new RuntimeException(new java.sql.SQLException("SQL_CANARY")))) {
            doThrow(exception).when(transaction).execute(any()); failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::page);
        }
        var first = new RuntimeException("FIRST"); var second = new RuntimeException("SECOND"); first.initCause(second); second.initCause(first);
        doThrow(first).when(transaction).execute(any()); failure(CustomerContextException.Code.INTERNAL, this::detail);
        doReturn(null).when(transaction).execute(any()); failure(CustomerContextException.Code.INTERNAL, this::page);
    }

    @Test void oneReadOnlyRepeatableNewTransactionContainsCompositionWithoutWrites() {
        detail(); verify(transaction, times(1)).execute(any());
        var ordered = inOrder(context, lifecycle, evidence, subjects);
        ordered.verify(context).execute("SET LOCAL ROLE customer_iam_role"); ordered.verify(context).fetchOne("select current_user");
        ordered.verify(lifecycle).requireActiveContext(any()); ordered.verify(evidence).findHeader(identifier);
        ordered.verify(subjects).findOwnedSubject(subject, account, null); ordered.verify(subjects).hasOpenRisk(subject);
        ordered.verify(evidence).findArtifacts(identifier); verify(context).execute("SET LOCAL ROLE customer_iam_role");
        verifyNoMoreInteractions(context, lifecycle, subjects, evidence);
    }
}
