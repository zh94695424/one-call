package com.onecall.core.activation_consent.internal.purchase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.*;
import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseConsentServiceTests {
    static class Harness {
        final JooqPurchaseConsentRepository repository = mock(JooqPurchaseConsentRepository.class);
        final TransactionWitness witness = new TransactionWitness("lo", 1, 2);
        final Connection connection = mock(Connection.class);
        final UUID subject = UUID.randomUUID(), account = UUID.randomUUID();
        final PurchaseConsentInput input = new PurchaseConsentInput(subject, account, "WEB", "purchase-consent:" + "e".repeat(64), "ACCEPT", "SYNTHETIC", "1", "1", "a".repeat(64));
        final LockedActivation locked = new LockedActivation(subject, UUID.randomUUID(), "ACTIVE", 1, witness);
        final ConsentDescriptor descriptor = new ConsentDescriptor(UUID.randomUUID(), subject, "PURCHASE", "WEB", "PURCHASE", input.eventId(), input.policyCode(), input.policyVersion(), input.promptVersion(), "ACCEPT", "ACCOUNT", input.eventId(), account, PurchaseConsentSemantics.fingerprint(input), input.contentSha256(), PurchaseConsentSemantics.sha(PurchaseConsentSemantics.snapshot(input)), Instant.parse("2026-11-24T00:00:00Z"), true);
        final DefaultPurchaseConsentService service;
        Harness() { this(new PurchaseConsentConfiguration.CurrentPolicy("SYNTHETIC", "1", "1", "a".repeat(64))); }
        Harness(PurchaseConsentConfiguration.CurrentPolicy policy) {
            when(repository.enter(witness)).thenReturn(connection); when(repository.lock(subject, witness)).thenReturn(locked); when(repository.insert(input)).thenReturn(descriptor);
            service = new DefaultPurchaseConsentService(repository, policy);
        }
        ConsentDescriptor append() { return service.appendPurchaseConsent(input, locked, witness); }
    }
    static void kind(Runnable action, Kind kind) { assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.kind()).isEqualTo(kind)); }
    @Test void lockOnlyReturnsExistingActivationWithoutCreatingHead() {
        var harness = new Harness(); assertThat(harness.service.lockServiceActivation(harness.subject, harness.witness)).isEqualTo(harness.locked);
        verify(harness.repository).lock(harness.subject, harness.witness); verify(harness.repository, never()).insert(any());
        when(harness.repository.lock(any(), any())).thenReturn(new LockedActivation(harness.subject, null, null, null, harness.witness));
        assertThat(harness.service.lockServiceActivation(harness.subject, harness.witness).activationId()).isNull();
    }
    @Test void appendRequiresPriorLockWitnessAndMandatoryTransaction() throws Exception {
        var harness = new Harness(); doThrow(new Failure(Kind.UNAVAILABLE)).when(harness.repository).verifyLock(any(), any()); kind(harness::append, Kind.UNAVAILABLE); verify(harness.repository, never()).insert(any());
        var annotation = DefaultPurchaseConsentService.class.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(annotation.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.MANDATORY);
        assertThat(annotation.transactionManager()).isEqualTo("operationsSubjectReadCatalogSaleTransactionManager");
    }
    @Test void currentApprovedPolicyAndContentAreRequired() {
        for (var policy : new PurchaseConsentConfiguration.CurrentPolicy[]{null, new PurchaseConsentConfiguration.CurrentPolicy(null, null, null, null), new PurchaseConsentConfiguration.CurrentPolicy("SYNTHETIC", "1", "1", "INVALID")}) {
            var harness = new Harness(policy); kind(harness::append, Kind.REQUIRED); verify(harness.repository, never()).insert(any());
        }
        var harness = new Harness(new PurchaseConsentConfiguration.CurrentPolicy("SYNTHETIC", "2", "1", "a".repeat(64))); kind(harness::append, Kind.STALE);
    }
    @Test void consentSnapshotAndFingerprintMatchFrozenInputs() {
        var harness = new Harness(); var node = PurchaseConsentSemantics.JSON.readTree(PurchaseConsentSemantics.snapshot(harness.input));
        assertThat(node.propertyNames()).containsExactlyInAnyOrder("schemaVersion", "accountId", "subjectId", "purpose", "channel", "eventDomain", "eventId", "sourceType", "sourceRef", "action", "policyCode", "policyVersion", "promptVersion", "contentSha256");
        assertThat(node.get("eventId").asString()).hasSize(81).isEqualTo(node.get("sourceRef").asString());
        assertThat(PurchaseConsentSemantics.sha(PurchaseConsentSemantics.canonical(java.util.Map.of()))).isEqualTo("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");
        assertThat(harness.append()).isEqualTo(harness.descriptor);
    }
    @Test void sameEventReturnsFirstConsentAndChangedSemanticsConflict() {
        var harness = new Harness(); var row = mock(org.jooq.Record.class); when(harness.repository.event(harness.input.eventId())).thenReturn(row); when(harness.repository.describe(row, false)).thenReturn(harness.descriptor);
        assertThat(harness.append()).isEqualTo(harness.descriptor); verify(harness.repository, never()).insert(any());
        var changed = new PurchaseConsentInput(harness.subject, harness.account, "WEB", harness.input.eventId(), "ACCEPT", "SYNTHETIC", "2", "1", harness.input.contentSha256());
        when(harness.repository.consent(harness.descriptor.id())).thenReturn(row);
        kind(() -> harness.service.validateReplay(changed, harness.descriptor.id(), harness.witness), Kind.CONFLICT);
    }
    @Test void declinedMismatchedAndConsumedConsentFailClosed() {
        var harness = new Harness(); var original = harness.descriptor;
        var declined = new ConsentDescriptor(original.id(), original.subjectId(), original.purpose(), original.channel(), original.eventDomain(), original.eventId(), original.policyCode(), original.policyVersion(), original.promptVersion(), "DECLINE", original.sourceType(), original.sourceRef(), original.accountId(), original.requestFingerprint(), original.contentSha256(), original.snapshotSha256(), original.acceptedAt(), false);
        kind(() -> harness.service.validateReplay(harness.input, original.id(), harness.witness), Kind.INTEGRITY);
        var row = mock(org.jooq.Record.class);
        when(harness.repository.consent(original.id())).thenReturn(row);
        when(harness.repository.event(harness.input.eventId())).thenReturn(row);
        when(harness.repository.describe(row, false)).thenReturn(declined);
        kind(() -> harness.service.validateReplay(harness.input, original.id(), harness.witness), Kind.INTEGRITY);
        kind(harness::append, Kind.DECLINED);
        var wrong = new PurchaseConsentInput(UUID.randomUUID(), harness.account, "WEB", harness.input.eventId(), "ACCEPT", "SYNTHETIC", "1", "1", harness.input.contentSha256());
        when(harness.repository.describe(row, false)).thenReturn(original);
        kind(() -> harness.service.validateReplay(wrong, original.id(), harness.witness), Kind.INTEGRITY);
        kind(() -> harness.service.appendPurchaseConsent(wrong, new LockedActivation(wrong.subjectId(), harness.locked.activationId(), "ACTIVE", 1, harness.witness), harness.witness), Kind.REQUIRED);
        for (String field : java.util.List.of("id", "requestFingerprint", "snapshotSha256")) {
            var corrupt = spy(original);
            if (field.equals("id")) when(corrupt.id()).thenReturn(UUID.randomUUID());
            if (field.equals("requestFingerprint")) when(corrupt.requestFingerprint()).thenReturn("b".repeat(64));
            if (field.equals("snapshotSha256")) when(corrupt.snapshotSha256()).thenReturn("b".repeat(64));
            when(harness.repository.describe(row, false)).thenReturn(corrupt);
            kind(() -> harness.service.validateReplay(harness.input, original.id(), harness.witness), Kind.INTEGRITY);
        }
    }
    @Test void factPhasesEmitOnlyOwnedPurchaseOutboxAndAudit() {
        var harness = new Harness(); UUID trace = UUID.randomUUID();
        for (FactPhase phase : FactPhase.values()) { harness.service.appendPurchaseConsentFacts(harness.descriptor, phase, trace, harness.witness); verify(harness.repository).fact(harness.descriptor, phase, trace); }
        var payload = PurchaseConsentSemantics.JSON.readTree(PurchaseConsentSemantics.payload(harness.descriptor)); assertThat(payload.propertyNames()).hasSize(12).doesNotContain("contentSha256", "idempotencyKey");
    }
    @Test void roleRestoresOnSuccessFailureAndAbortedSql() {
        var harness = new Harness(); harness.append(); verify(harness.repository).restore(harness.witness, harness.connection);
        var failure = new Failure(Kind.STALE); doThrow(failure).when(harness.repository).verifyLock(any(), any()); doThrow(new IllegalStateException("PRIVATE cleanup")).when(harness.repository).restore(any(), any());
        assertThatThrownBy(harness::append).isSameAs(failure); assertThat(failure.getSuppressed()).hasSize(1).allSatisfy(suppressed -> assertThat(suppressed.getMessage()).isEqualTo("UNAVAILABLE"));
    }
}
