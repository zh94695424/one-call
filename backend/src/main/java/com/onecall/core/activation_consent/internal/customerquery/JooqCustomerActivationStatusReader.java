package com.onecall.core.activation_consent.internal.customerquery;

import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;

public final class JooqCustomerActivationStatusReader implements CustomerActivationStatusReader {
    private final DSLContext context;

    public JooqCustomerActivationStatusReader(DSLContext context) { this.context = context; }

    @Override
    public Map<UUID, String> findStatuses(Set<UUID> subjectIds) {
        Objects.requireNonNull(subjectIds);
        if (subjectIds.size() > 101 || subjectIds.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Invalid subjects");
        if (subjectIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(subjectIds.size(), "?"));
        Map<UUID, String> result = new HashMap<>();
        for (var row : context.fetch("SELECT subject_id, status FROM activation_consent.service_activation WHERE subject_id IN ("
                + placeholders + ")", subjectIds.toArray())) {
            UUID subject = row.get("subject_id", UUID.class);
            String status = row.get("status", String.class);
            if (!subjectIds.contains(subject) || !Set.of("INACTIVE", "ACTIVE", "SUSPENDED").contains(status)
                    || result.putIfAbsent(subject, status) != null) throw new IllegalStateException("Invalid projection");
        }
        return Map.copyOf(result);
    }
}
