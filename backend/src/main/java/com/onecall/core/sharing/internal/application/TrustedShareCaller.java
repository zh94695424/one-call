package com.onecall.core.sharing.internal.application;

import java.util.Optional;
import java.util.UUID;

public record TrustedShareCaller(UUID accountId, Optional<UUID> organizationId) {}
