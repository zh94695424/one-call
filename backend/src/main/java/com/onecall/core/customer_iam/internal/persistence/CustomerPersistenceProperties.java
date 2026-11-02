package com.onecall.core.customer_iam.internal.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.customer.persistence")
public class CustomerPersistenceProperties {
    private String url;
    private String username;
    private String password;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public void validate() {
        if (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Explicit customer query credentials are required");
        }
    }
}
