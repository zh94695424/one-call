package com.onecall.core.sharing.internal.application;

import java.util.Optional;

public record SharePageRequest(Optional<ShareCursor> cursor, Optional<Integer> limit) {}
