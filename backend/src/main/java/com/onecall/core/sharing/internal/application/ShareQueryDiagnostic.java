package com.onecall.core.sharing.internal.application;

import java.util.Optional;

public record ShareQueryDiagnostic(String operation, ShareQueryException.Code errorCode,
        String exceptionClass, Optional<String> sqlState, String category, boolean roleAssertionPassed) {}
