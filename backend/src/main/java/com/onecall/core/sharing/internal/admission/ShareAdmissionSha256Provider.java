package com.onecall.core.sharing.internal.admission;

@FunctionalInterface
public interface ShareAdmissionSha256Provider {
    String digest(byte[] exactUtf8Bytes);
}
