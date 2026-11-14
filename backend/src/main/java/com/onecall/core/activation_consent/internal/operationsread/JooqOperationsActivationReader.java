package com.onecall.core.activation_consent.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("operationsActivationReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsActivationReader implements ActivationReader {
    private final DSLContext dsl;
    private final OperationsReadContext context;
    public JooqOperationsActivationReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }

    @Override public Map<UUID, ActivationFacts> find(Set<UUID> subjectIds) {
        if (subjectIds == null || subjectIds.size() > 101 || subjectIds.stream().anyMatch(Objects::isNull)) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return context.scoped(Role.ACTIVATION, () -> {
            if (subjectIds.isEmpty()) return Map.of();
            String placeholders = String.join(",", Collections.nCopies(subjectIds.size(), "CAST(? AS uuid)"));
            var result = new HashMap<UUID, ActivationFacts>();
            for (var row : dsl.fetch("SELECT subject_id, status, revision FROM activation_consent.service_activation WHERE subject_id IN (" + placeholders + ")", subjectIds.toArray())) {
                UUID subject = row.get("subject_id", UUID.class);
                String status = row.get("status", String.class);
                Integer revision = row.get("revision", Integer.class);
                if (!subjectIds.contains(subject) || status == null || !Set.of("INACTIVE", "ACTIVE", "SUSPENDED").contains(status)
                        || revision == null || revision < 1 || result.putIfAbsent(subject, new ActivationFacts(status, revision)) != null)
                    throw new Failure(Code.SERVICE_UNAVAILABLE);
            }
            return Map.copyOf(result);
        });
    }
}
