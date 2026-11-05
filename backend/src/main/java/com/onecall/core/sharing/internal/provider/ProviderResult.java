package com.onecall.core.sharing.internal.provider;

import com.onecall.core.sharing.internal.command.ShareCommandException;
import java.util.Objects;

public sealed interface ProviderResult<T> {
    record Allowed<T>(T value) implements ProviderResult<T> {
        public Allowed { Objects.requireNonNull(value); }
    }
    record Denied<T>(ShareCommandException.Code code) implements ProviderResult<T> {
        public Denied { Objects.requireNonNull(code); }
    }
    record Unavailable<T>(ShareCommandException.Code code) implements ProviderResult<T> {
        public Unavailable { Objects.requireNonNull(code); }
    }
}
