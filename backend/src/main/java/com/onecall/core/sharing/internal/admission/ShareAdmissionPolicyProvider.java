package com.onecall.core.sharing.internal.admission;

import java.time.Duration;
import java.util.UUID;

@FunctionalInterface
public interface ShareAdmissionPolicyProvider {
    Result ticketPolicy(SharePolicyRequest request);

    record SharePolicyRequest(UUID shareGrantId, String verificationMode) { }
    sealed interface Result permits ApprovedTicketPolicy, Unavailable { }
    record ApprovedTicketPolicy(Duration sessionTtl, Duration ticketTtl) implements Result { }
    record Unavailable() implements Result { }
}
