package com.onecall.core.operations_iam.subjectread;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class OperationsSubjectReadPorts {
    private OperationsSubjectReadPorts() { }
    public interface SubjectReader {
        List<Candidate> fetchCandidates(Filters filters, Optional<Anchor> anchor, int fetchSize);
        Optional<DetailFacts> detail(UUID subjectId, int historyLimit);
    }
    public interface ActivationReader { Map<UUID, ActivationFacts> find(Set<UUID> subjectIds); }
    public interface TrialReader { Optional<TrialFacts> find(UUID subjectId, Instant now); }
    public interface EvidenceCounter { long count(UUID subjectId); }
    public interface OrderCounter { long count(UUID subjectId); }

    public record Filters(UUID subjectId, String source, String claimStatus, String activationStatus,
            String realNameStatus, String riskState) { }
    public record Anchor(Instant createdAt, UUID subjectId) {
        @Override
        public String toString() { return "Anchor[REDACTED]"; }
    }
    public record Candidate(SubjectFacts subject) { }
    public record SubjectFacts(UUID subjectId, String source, String maskedNumber, String claimStatus,
            String realNameStatus, boolean riskOpen, Instant createdAt) { }
    public record DetailFacts(SubjectFacts subject, List<BindingFacts> bindings, List<ClaimFacts> claims) {
        public DetailFacts { bindings = List.copyOf(bindings); claims = List.copyOf(claims); }
    }
    public record BindingFacts(UUID bindingId, String numberMasked, String numberType, String status, Instant verifiedAt) { }
    public record ClaimFacts(UUID claimId, String status, String verificationMethod, Instant riskCheckedAt, Instant verifiedAt) { }
    public record ActivationFacts(String status, int revision) { }
    public record TrialFacts(UUID trialId, UUID subjectId, String campaignKey, UUID entitlementGrantId,
            Instant trialExpiresAt, UUID grantId, UUID grantSubjectId, String sourceType, String entitlementType,
            Long grantedUnits, Instant grantExpiresAt, UUID balanceId, UUID balanceGrantId,
            Long balanceGrantedUnits, Long reservedUnits, Long consumedUnits, String balanceStatus) { }
}
