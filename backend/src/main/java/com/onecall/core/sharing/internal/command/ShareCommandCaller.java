package com.onecall.core.sharing.internal.command;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ShareCommandCaller(UUID accountId, Optional<UUID> organizationId) {
    public ShareCommandCaller {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(organizationId);
    }
}
