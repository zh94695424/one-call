package com.onecall.core.activation_consent.internal.purchase;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(transactionManager = "operationsSubjectReadCatalogSaleTransactionManager", propagation = Propagation.MANDATORY)
public class DefaultPurchaseConsentService implements PurchaseConsentService {
    private final JooqPurchaseConsentRepository repository;
    private final PurchaseConsentConfiguration.CurrentPolicy policy;
    DefaultPurchaseConsentService(JooqPurchaseConsentRepository repository, PurchaseConsentConfiguration.CurrentPolicy policy) { this.repository = repository; this.policy = policy; }
    private <Value> Value owned(TransactionWitness witness, Supplier<Value> action) {
        java.sql.Connection connection = repository.enter(witness);
        RuntimeException primary = null;
        try { return action.get(); }
        catch (RuntimeException failure) { primary = failure; throw failure; }
        finally {
            try { repository.restore(witness, connection); }
            catch (RuntimeException cleanup) {
                Failure sanitized = new Failure(Kind.UNAVAILABLE);
                if (primary != null) primary.addSuppressed(sanitized); else throw sanitized;
            }
        }
    }
    @Override public LockedActivation lockServiceActivation(UUID subjectId, TransactionWitness witness) { return owned(witness, () -> repository.lock(subjectId, witness)); }
    @Override public ConsentDescriptor validateReplay(PurchaseConsentInput input, UUID consentId, TransactionWitness witness) {
        return owned(witness, () -> {
            requireInput(input);
            var row = repository.consent(consentId);
            if (row == null) throw new Failure(Kind.INTEGRITY);
            var consent = repository.describe(row, false);
            if (!Objects.equals(consentId, consent.id())) throw new Failure(Kind.INTEGRITY);
            return validate(input, consent, true);
        });
    }
    @Override public ConsentDescriptor appendPurchaseConsent(PurchaseConsentInput input, LockedActivation locked, TransactionWitness witness) {
        return owned(witness, () -> {
            requireInput(input);
            repository.verifyLock(locked, witness);
            if (!Objects.equals(input.subjectId(), locked.subjectId())) throw new Failure(Kind.REQUIRED);
            PurchaseConsentConfiguration.CurrentPolicy current = policy;
            if (current == null || !current.valid()) throw new Failure(Kind.REQUIRED);
            if (!current.policyCode().equals(input.policyCode()) || !current.policyVersion().equals(input.policyVersion())
                    || !current.promptVersion().equals(input.promptVersion()) || !current.contentSha256().equals(input.contentSha256())) throw new Failure(Kind.STALE);
            var existing = repository.event(input.eventId());
            return existing == null ? repository.insert(input) : validate(input, repository.describe(existing, false), false);
        });
    }
    private void requireInput(PurchaseConsentInput input) {
        if (input == null || input.subjectId() == null || input.accountId() == null || !"ACCEPT".equals(input.action())
                || !java.util.Set.of("WEB", "MINIAPP").contains(Objects.toString(input.channel(), ""))
                || input.eventId() == null || !input.eventId().matches("purchase-consent:[0-9a-f]{64}")
                || !PurchaseConsentSemantics.validText(input.policyCode()) || !PurchaseConsentSemantics.validText(input.policyVersion())
                || !PurchaseConsentSemantics.validText(input.promptVersion()) || input.contentSha256() == null || !input.contentSha256().matches("[0-9a-f]{64}")) throw new Failure(Kind.REQUIRED);
    }
    private ConsentDescriptor validate(PurchaseConsentInput input, ConsentDescriptor consent, boolean replay) {
        if (!"ACCEPT".equals(consent.action())) throw new Failure(replay ? Kind.INTEGRITY : Kind.DECLINED);
        if (!input.subjectId().equals(consent.subjectId()) || !input.accountId().equals(consent.accountId()) || !input.channel().equals(consent.channel())
                || !input.eventId().equals(consent.eventId()) || !"PURCHASE".equals(consent.eventDomain()) || !"PURCHASE".equals(consent.purpose())
                || !"ACCOUNT".equals(consent.sourceType()) || !input.eventId().equals(consent.sourceRef())) throw new Failure(replay ? Kind.INTEGRITY : Kind.REQUIRED);
        if (replay) {
            var original = new PurchaseConsentInput(consent.subjectId(), consent.accountId(), consent.channel(), consent.eventId(), consent.action(),
                    consent.policyCode(), consent.policyVersion(), consent.promptVersion(), consent.contentSha256());
            if (!PurchaseConsentSemantics.fingerprint(original).equals(consent.requestFingerprint())
                    || !PurchaseConsentSemantics.sha(PurchaseConsentSemantics.snapshot(original)).equals(consent.snapshotSha256())) throw new Failure(Kind.INTEGRITY);
        }
        if (!PurchaseConsentSemantics.fingerprint(input).equals(consent.requestFingerprint()) || !input.contentSha256().equals(consent.contentSha256())) throw new Failure(Kind.CONFLICT);
        String full = PurchaseConsentSemantics.snapshot(input);
        if (!PurchaseConsentSemantics.sha(full).equals(consent.snapshotSha256())) throw new Failure(Kind.INTEGRITY);
        return consent;
    }
    @Override public void appendPurchaseConsentFacts(ConsentDescriptor consent, FactPhase phase, UUID traceId, TransactionWitness witness) {
        owned(witness, () -> { repository.fact(consent, phase, traceId); return null; });
    }
}
