package com.onecall.core.activation_consent.purchaseconsent;

import java.time.Instant;
import java.util.UUID;

public interface PurchaseConsentService {
    LockedActivation lockServiceActivation(UUID subjectId, TransactionWitness witness);
    ConsentDescriptor validateReplay(PurchaseConsentInput input, UUID consentId, TransactionWitness witness);
    ConsentDescriptor appendPurchaseConsent(PurchaseConsentInput input, LockedActivation locked, TransactionWitness witness);
    void appendPurchaseConsentFacts(ConsentDescriptor consent, FactPhase phase, UUID traceId, TransactionWitness witness);
    record TransactionWitness(String login, int backendPid, long transactionId) {}
    record LockedActivation(UUID subjectId, UUID activationId, String status, Integer revision, TransactionWitness witness) {}
    record PurchaseConsentInput(UUID subjectId, UUID accountId, String channel, String eventId,
            String action, String policyCode, String policyVersion, String promptVersion, String contentSha256) {}
    record ConsentDescriptor(UUID id, UUID subjectId, String purpose, String channel, String eventDomain,
            String eventId, String policyCode, String policyVersion, String promptVersion, String action,
            String sourceType, String sourceRef, UUID accountId, String requestFingerprint,
            String contentSha256, String snapshotSha256, Instant acceptedAt, boolean newlyAppended) {}
    enum FactPhase { OUTBOX, AUDIT }
    enum Kind { REQUIRED, STALE, DECLINED, CONFLICT, UNAVAILABLE, INTEGRITY }
    final class Failure extends RuntimeException {
        private final Kind kind;
        public Failure(Kind kind) { super(kind.name()); this.kind = kind; }
        public Kind kind() { return kind; }
    }
}
