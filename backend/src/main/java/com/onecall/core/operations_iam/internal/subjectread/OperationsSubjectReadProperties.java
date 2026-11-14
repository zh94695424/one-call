package com.onecall.core.operations_iam.internal.subjectread;

import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("onecall.operations-subject-read")
public record OperationsSubjectReadProperties(boolean enabled, Datasource datasource, Cursor cursor) {
    public record Datasource(String url, String username, String password) {
        public void validate() {
            if (url == null || url.isBlank() || username == null || username.isBlank() || password == null || password.isBlank())
                throw new Failure(Code.SERVICE_UNAVAILABLE);
        }
        @Override public String toString() { return "Datasource[REDACTED]"; }
    }
    public record Cursor(String activeKeyId, List<KeyEntry> keys) {
        public Cursor { if (keys != null) keys = Collections.unmodifiableList(new ArrayList<>(keys)); }
        public List<KeyEntry> validatedKeys() {
            try {
                if (activeKeyId == null || keys == null || keys.isEmpty() || keys.size() > 3) throw new IllegalArgumentException();
                var identifiers = new HashSet<String>();
                var materials = new ArrayList<byte[]>();
                for (var key : keys) {
                    if (key == null || key.id() == null || !key.id().matches("[A-Za-z0-9_-]{1,32}")
                            || !identifiers.add(key.id())) throw new IllegalArgumentException();
                    byte[] material = key.bytes();
                    if (materials.stream().anyMatch(previous -> MessageDigest.isEqual(previous, material))) throw new IllegalArgumentException();
                    materials.add(material);
                }
                if (!identifiers.contains(activeKeyId)) throw new IllegalArgumentException();
                return List.copyOf(keys);
            } catch (RuntimeException invalid) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        }
        @Override public String toString() { return "Cursor[REDACTED]"; }
    }
    public record KeyEntry(String id, String material) {
        public byte[] bytes() {
            if (material == null || material.length() != 43 || !material.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException();
            byte[] decoded = Base64.getUrlDecoder().decode(material);
            if (decoded.length != 32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(material)) throw new IllegalArgumentException();
            return decoded.clone();
        }
        @Override public String toString() { return "KeyEntry[REDACTED]"; }
    }
    @Override public String toString() { return "OperationsSubjectReadProperties[REDACTED]"; }
}
