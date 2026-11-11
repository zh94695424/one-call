package com.onecall.core.subject_number.customerquery;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface CustomerBusinessNumberReader {
    List<Row> fetch(Scope scope, Optional<Anchor> anchor, int fetchSize);

    record Scope(UUID accountId, UUID organizationId) {
        public Scope { Objects.requireNonNull(accountId); }
    }

    record Anchor(Instant createdAt, UUID bindingId) {
        public Anchor {
            Objects.requireNonNull(createdAt);
            Objects.requireNonNull(bindingId);
            int year = createdAt.atOffset(java.time.ZoneOffset.UTC).getYear();
            if (createdAt.getNano() % 1000 != 0 || year < -4712 || year > 294276)
                throw new IllegalArgumentException("Invalid anchor");
        }
    }

    record Row(UUID bindingId, UUID subjectId, String numberMasked, String numberType,
            String bindingStatus, String realNameStatus, Instant subjectCreatedAt) { }
}
