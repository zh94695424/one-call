package com.onecall.core.sharing.internal.playback;

public interface MediaCredentialVerifier {
    Result verify(OpaquePlaybackCredential credential);

    sealed interface Result permits Result.Verified, Result.Invalid, Result.Unavailable {
        record Verified(VerifiedMediaClaim claim) implements Result { }
        record Invalid() implements Result { }
        record Unavailable() implements Result { }
    }
}
