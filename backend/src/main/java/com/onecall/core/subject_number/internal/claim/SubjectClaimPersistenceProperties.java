package com.onecall.core.subject_number.internal.claim;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.subject-claim-command.datasource")
public record SubjectClaimPersistenceProperties(String url, String username, String password) {
    public SubjectClaimPersistenceProperties {
        if (url == null || url.isBlank() || username == null || username.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("Subject claim command credentials required");
    }
    @Override public String toString() { return "SubjectClaimPersistenceProperties[REDACTED]"; }
}
