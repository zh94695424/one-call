package com.onecall.core.evidence.internal.mediaread;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.evidence-media-read.datasource")
public record EvidenceMediaReadPersistenceProperties(Boolean enabled, String url, String username, String password) {
    public EvidenceMediaReadPersistenceProperties {
        if (Boolean.TRUE.equals(enabled) && (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank() || !username.equals("evidence_media_reader_role")
                || !validUrl(url)))
            throw new IllegalArgumentException("Valid explicit evidence media reader credentials are required");
    }

    private static boolean validUrl(String url) {
        try {
            var parsed = java.net.URI.create(url.substring("jdbc:".length()));
            return url.startsWith("jdbc:postgresql://") && "postgresql".equals(parsed.getScheme())
                    && parsed.getHost() != null && parsed.getUserInfo() == null
                    && parsed.getPath() != null && parsed.getPath().length() > 1
                    && parsed.getFragment() == null && parsed.getQuery() == null
                    && parsed.getPort() != 0 && parsed.getPort() <= 65535;
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "EvidenceMediaReadPersistenceProperties[REDACTED]";
    }
}
