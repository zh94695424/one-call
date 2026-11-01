package com.onecall.core.sharing.internal.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.persistence")
public final class SharingPersistenceProperties {
    private final String url;
    private final String username;
    private final String password;

    public SharingPersistenceProperties(String url, String username, String password) {
        if (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Explicit query credentials are required");
        }
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
