package com.onecall.core.sharing.internal.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.sharing-command.datasource")
public record SharingCommandPersistenceProperties(String url, String username, String password) {
    public SharingCommandPersistenceProperties {
        if (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank())
            throw new IllegalArgumentException("Explicit command credentials are required");
    }
}
