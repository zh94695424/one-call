package com.onecall.core.sharing.internal.provider;

public interface Sha256Provider {
    String digest(byte[] exactUtf8Bytes);
}
