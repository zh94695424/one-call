package com.onecall.core.subject_number.internal.claim;

public sealed interface SubjectClaimProviderResult<T> {
    record Allowed<T>(T value) implements SubjectClaimProviderResult<T> {
        public Allowed {
            if (value instanceof byte[] bytes) {
                @SuppressWarnings("unchecked") T copy = (T) bytes.clone();
                value = copy;
            }
        }
        @Override public T value() {
            if (value instanceof byte[] bytes) {
                @SuppressWarnings("unchecked") T copy = (T) bytes.clone();
                return copy;
            }
            return value;
        }
        @Override public String toString() { return "Allowed[REDACTED]"; }
    }
    record Invalid<T>() implements SubjectClaimProviderResult<T> {
        @Override public String toString() { return "Invalid[REDACTED]"; }
    }
    record Unavailable<T>() implements SubjectClaimProviderResult<T> {
        @Override public String toString() { return "Unavailable[REDACTED]"; }
    }
}
