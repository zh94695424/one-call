package com.onecall.core.operations_iam.internal.web;

import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import java.security.Principal;

public record OperationsRequestPrincipal(Caller caller) implements Principal {
    @Override public String getName() { return "employee"; }
    @Override public String toString() { return "OperationsRequestPrincipal[REDACTED]"; }
}
