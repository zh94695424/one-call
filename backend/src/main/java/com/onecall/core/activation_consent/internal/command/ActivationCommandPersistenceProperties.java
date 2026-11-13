package com.onecall.core.activation_consent.internal.command;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.activation-command.datasource")
public record ActivationCommandPersistenceProperties(String url, String username, String password) {
    public ActivationCommandPersistenceProperties {
        if (url == null || url.isBlank() || username == null || username.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("Activation command credentials required");
    }
    @Override public String toString() { return "ActivationCommandPersistenceProperties[REDACTED]"; }
}
