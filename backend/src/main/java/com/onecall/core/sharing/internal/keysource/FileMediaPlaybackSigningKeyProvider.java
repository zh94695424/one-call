package com.onecall.core.sharing.internal.keysource;

import com.fasterxml.jackson.annotation.JsonValue;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public final class FileMediaPlaybackSigningKeyProvider implements MediaPlaybackSigningKeyProvider {
    private final String configuredPath;
    private final MediaPlaybackKeyBundleParser parser = new MediaPlaybackKeyBundleParser();

    public FileMediaPlaybackSigningKeyProvider(String configuredPath) { this.configuredPath = configuredPath; }

    @Override public Result currentKeys() {
        byte[] source = null;
        try {
            if (configuredPath == null || configuredPath.isBlank()) return new Result.Unavailable();
            Path path = Path.of(configuredPath);
            if (!path.isAbsolute() || !path.equals(path.normalize())) return new Result.Unavailable();
            source = readBoundedSource(path);
            return parser.parse(source);
        } catch (IOException | RuntimeException failure) {
            return new Result.Unavailable();
        } finally { if (source != null) Arrays.fill(source, (byte) 0); }
    }

    byte[] readBoundedSource(Path path) throws IOException {
        BasicFileAttributes before = checkedAttributes(path);
        if (!before.isRegularFile() || before.size() <= 0 || before.size() > MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES)
            throw new IOException();
        byte[] buffer = new byte[MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES + 1];
        try {
            ByteBuffer target = ByteBuffer.wrap(buffer);
            try (var channel = Files.newByteChannel(path, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
                while (target.hasRemaining()) {
                    int count = channel.read(target);
                    if (count < 0) break;
                    if (count == 0) throw new IOException();
                }
            }
            BasicFileAttributes after = checkedAttributes(path);
            if (target.position() > MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES || target.position() != before.size()
                    || !after.isRegularFile() || before.size() != after.size()
                    || !Objects.equals(before.fileKey(), after.fileKey())
                    || !before.lastModifiedTime().equals(after.lastModifiedTime())) throw new IOException();
            return Arrays.copyOf(buffer, target.position());
        } finally { Arrays.fill(buffer, (byte) 0); }
    }

    private static BasicFileAttributes checkedAttributes(Path path) throws IOException {
        Path component = path.getRoot();
        if (Files.readAttributes(component, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink())
            throw new IOException();
        for (Path name : path) {
            component = component.resolve(name);
            BasicFileAttributes attributes = Files.readAttributes(component, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink()) throw new IOException();
            if (component.equals(path)) return attributes;
            if (!attributes.isDirectory()) throw new IOException();
        }
        throw new IOException();
    }

    @JsonValue public String redacted() { return "FileMediaPlaybackSigningKeyProvider[REDACTED]"; }
    @Override public String toString() { return redacted(); }
}
