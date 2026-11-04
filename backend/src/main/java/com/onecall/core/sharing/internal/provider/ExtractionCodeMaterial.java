package com.onecall.core.sharing.internal.provider;

import java.util.Objects;

public record ExtractionCodeMaterial(String code, String salt, String codeHash) {
    public ExtractionCodeMaterial {
        Objects.requireNonNull(code);
        Objects.requireNonNull(salt);
        Objects.requireNonNull(codeHash);
    }
}
