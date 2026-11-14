package com.onecall.core.activation_consent.internal.operations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.operations-lifecycle.datasource")
public record OperationsLifecyclePersistenceProperties(String url, String username, String password) {
    public OperationsLifecyclePersistenceProperties {
        if (url == null || url.isBlank() || username == null || username.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("Operations lifecycle credentials required");
    }
    @Override public String toString() { return "OperationsLifecyclePersistenceProperties[REDACTED]"; }
}
