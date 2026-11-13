package com.onecall.core.activation_consent.internal.operations;

import com.onecall.core.activation_consent.internal.command.ActivationSemantics;
import com.onecall.core.activation_consent.internal.operations.OperationsLifecycleRepository.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class OperationsLifecycleSemantics {
    private OperationsLifecycleSemantics() { }
    public static boolean text(String value, int bound) {
        return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= bound && value.indexOf('\0') < 0
                && StandardCharsets.UTF_8.newEncoder().canEncode(value);
    }
    public static Map<String, Object> fingerprintFields(Command command, Caller caller, String target, String statusEvent) {
        return ActivationSemantics.fields("accountId", null, "actorEmployeeId", caller.employeeId(), "channel", "OPERATIONS", "consentRequestFingerprint", null,
                "reasonCode", command.reasonCode(), "sourceActionDomain", "OPERATIONS", "sourceActionEventId", command.actionEventId(),
                "statusEventDomain", "ACTIVATION_STATUS", "statusEventId", statusEvent, "subjectId", command.subjectId(), "toStatus", target);
    }
    public static String fingerprint(Command command, Caller caller, String target, String statusEvent) {
        return ActivationSemantics.sha(ActivationSemantics.canonical(fingerprintFields(command, caller, target, statusEvent)));
    }
    public static void validateHead(Head head, UUID subjectId) {
        if (head == null || head.id() == null || !subjectId.equals(head.subjectId()) || head.status() == null
                || !Set.of("INACTIVE", "ACTIVE", "SUSPENDED").contains(head.status()) || head.revision() < 1 || head.createdAt() == null
                || head.updatedAt() == null || head.updatedAt().isBefore(head.createdAt())) throw unavailable();
    }
    public static void validateTransition(Transition row) {
        if (row == null || row.id() == null || row.activationId() == null || row.subjectId() == null || row.fromStatus() == null || row.toStatus() == null
                || !Set.of("INACTIVE", "ACTIVE", "SUSPENDED").contains(row.fromStatus()) || !Set.of("ACTIVE", "SUSPENDED").contains(row.toStatus())
                || row.fromStatus().equals(row.toStatus()) || row.fromRevision() < 0 || row.fromRevision() == Integer.MAX_VALUE || row.toRevision() != row.fromRevision() + 1
                || row.channel() == null || !Set.of("MINIAPP", "WEB", "IVR", "OPERATIONS").contains(row.channel())
                || row.actionDomain() == null || !Set.of("ACTIVATION", "IVR_CONFIRM", "OPERATIONS").contains(row.actionDomain())
                || !text(row.actionEvent(), 128) || !"ACTIVATION_STATUS".equals(row.statusDomain())
                || !ActivationSemantics.statusEvent(row.activationId(), row.toRevision()).equals(row.statusEvent())
                || row.fingerprint() == null || !row.fingerprint().matches("[0-9a-f]{64}") || !text(row.reason(), 64) || row.occurredAt() == null) throw unavailable();
        if ("OPERATIONS".equals(row.actionDomain())) {
            if (!"OPERATIONS".equals(row.channel()) || row.employeeId() == null || row.accountId() != null || row.consentId() != null
                    || row.fromRevision() < 1 || "INACTIVE".equals(row.fromStatus())) throw unavailable();
            var stored = new Command(row.subjectId(), row.actionEvent(), row.reason(), java.math.BigInteger.valueOf(row.fromRevision()));
            if (!fingerprint(stored, new Caller(row.employeeId(), "onecall:employee-session:v1"), row.toStatus(), row.statusEvent()).equals(row.fingerprint())) throw unavailable();
        } else {
            if (row.consentId() == null || "SUSPENDED".equals(row.fromStatus()) || "OPERATIONS".equals(row.channel())) throw unavailable();
            if ("ACTIVATION".equals(row.actionDomain()) && (!Set.of("WEB", "MINIAPP").contains(row.channel()) || row.accountId() == null || row.employeeId() != null)) throw unavailable();
            if ("IVR_CONFIRM".equals(row.actionDomain()) && !"IVR".equals(row.channel())) throw unavailable();
        }
    }
    public static ActivationTransition replay(Transition row, Head head, Command command, Caller caller, String from, String target) {
        validateTransition(row);
        if (!"OPERATIONS".equals(row.actionDomain()) || !head.id().equals(row.activationId()) || !command.subjectId().equals(row.subjectId())
                || !Objects.equals(caller.employeeId(), row.employeeId()) || !from.equals(row.fromStatus()) || !target.equals(row.toStatus())
                || !command.actionEventId().equals(row.actionEvent()) || !command.reasonCode().equals(row.reason())
                || !command.expectedActivationRevision().equals(java.math.BigInteger.valueOf(row.fromRevision()))
                || !fingerprint(command, caller, target, row.statusEvent()).equals(row.fingerprint())) throw new Failure(Code.ACTIVATION_STATE_CONFLICT);
        return result(row);
    }
    public static void witness(Transition row, Head head) {
        validateTransition(row);
        if (!head.id().equals(row.activationId()) || !head.subjectId().equals(row.subjectId()) || !head.status().equals(row.toStatus())
                || head.revision() != row.toRevision() || !head.updatedAt().equals(row.occurredAt())) throw unavailable();
    }
    public static ActivationTransition result(Transition row) {
        return new ActivationTransition(row.id(), row.statusEvent(), row.fromStatus(), row.toStatus(), row.fromRevision(), row.toRevision(), row.channel(), row.actionDomain(), row.reason(), row.occurredAt());
    }
    static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
