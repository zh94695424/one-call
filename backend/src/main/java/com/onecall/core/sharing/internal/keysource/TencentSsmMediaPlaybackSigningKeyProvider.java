package com.onecall.core.sharing.internal.keysource;

import com.fasterxml.jackson.annotation.JsonValue;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TencentSsmMediaPlaybackSigningKeyProvider implements MediaPlaybackSigningKeyProvider {
    private final Settings settings;
    private final Reader reader;
    private final MediaPlaybackKeyBundleParser parser = new MediaPlaybackKeyBundleParser();

    TencentSsmMediaPlaybackSigningKeyProvider(Settings settings) {
        this(settings, settings == null ? null : new TencentSsmSecretStringClient(settings));
    }

    TencentSsmMediaPlaybackSigningKeyProvider(Settings settings, Reader reader) {
        this.settings = settings;
        this.reader = reader;
    }

    @Override public Result currentKeys() {
        byte[] buffer = null;
        byte[] source = null;
        try {
            if (settings == null || reader == null) return new Result.Unavailable();
            String secret = reader.read();
            if (secret == null || secret.isBlank() || secret.length() > MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES)
                return new Result.Unavailable();
            buffer = new byte[MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES + 1];
            ByteBuffer target = ByteBuffer.wrap(buffer);
            var encoder = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            var encoded = encoder.encode(CharBuffer.wrap(secret), target, true);
            if (encoded.isOverflow()) return new Result.Unavailable();
            if (encoded.isError()) encoded.throwException();
            var flushed = encoder.flush(target);
            if (flushed.isError()) flushed.throwException();
            if (target.position() > MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES) return new Result.Unavailable();
            source = Arrays.copyOf(buffer, target.position());
            return parser.parse(source);
        } catch (IOException | RuntimeException failure) {
            return new Result.Unavailable();
        } finally {
            if (source != null) Arrays.fill(source, (byte) 0);
            if (buffer != null) Arrays.fill(buffer, (byte) 0);
        }
    }

    @FunctionalInterface
    interface Reader { String read() throws IOException; }

    static final class Settings {
        final String region;
        final String secretName;
        final String versionId;
        final int connectSeconds;
        final int readSeconds;

        private Settings(String region, String secretName, String versionId, int connectSeconds, int readSeconds) {
            this.region = region;
            this.secretName = secretName;
            this.versionId = versionId;
            this.connectSeconds = connectSeconds;
            this.readSeconds = readSeconds;
        }

        static Settings create(String region, String secretName, String versionId, String connect, String read) {
            if (region == null || region.length() > 64 || !region.matches("[a-z0-9]+(?:-[a-z0-9]+)+")
                    || !opaque(secretName, 1_024) || !opaque(versionId, 256)
                    || connect != null && !connect.matches("[1-5]")
                    || read != null && !read.matches("(?:[1-9]|10)")) return null;
            return new Settings(region, secretName, versionId, connect == null ? 2 : Integer.parseInt(connect),
                    read == null ? 3 : Integer.parseInt(read));
        }

        private static boolean opaque(String value, int maximum) {
            if (value == null || value.isEmpty() || value.length() > maximum || value.contains("${")
                    || Character.isWhitespace(value.codePointAt(0)) || Character.isSpaceChar(value.codePointAt(0))
                    || Character.isWhitespace(value.codePointBefore(value.length()))
                    || Character.isSpaceChar(value.codePointBefore(value.length()))
                    || value.codePoints().anyMatch(Character::isISOControl)) return false;
            try {
                StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value));
                return true;
            } catch (CharacterCodingException failure) { return false; }
        }

        @JsonValue public String redacted() { return "Settings[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }

    @JsonValue public String redacted() { return "TencentSsmMediaPlaybackSigningKeyProvider[REDACTED]"; }
    @Override public String toString() { return redacted(); }
}
