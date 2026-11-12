package com.onecall.core.subject_number.customersubjectquery;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CustomerSubjectAccessReader {
    Optional<UUID> findOwnedSubject(UUID subjectId, UUID accountId, UUID organizationId);
    boolean hasOpenRisk(UUID subjectId);
    Set<UUID> findOwnedSubjectIds(UUID accountId, UUID organizationId);
    boolean hasOpenRisk(Set<UUID> subjectIds);
}
