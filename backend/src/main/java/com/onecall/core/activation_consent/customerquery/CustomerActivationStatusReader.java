package com.onecall.core.activation_consent.customerquery;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface CustomerActivationStatusReader {
    Map<UUID, String> findStatuses(Set<UUID> subjectIds);
}
