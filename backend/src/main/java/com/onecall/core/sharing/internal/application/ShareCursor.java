package com.onecall.core.sharing.internal.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record ShareCursor(UUID accountId, Optional<UUID> organizationId, Instant validFrom, String shareId) {}
