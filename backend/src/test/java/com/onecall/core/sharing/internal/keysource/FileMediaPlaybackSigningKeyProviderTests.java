package com.onecall.core.sharing.internal.keysource;

import static com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class FileMediaPlaybackSigningKeyProviderTests {
    @TempDir Path directory;
    private CapturedOutput output;

    @BeforeEach void captureOutput(CapturedOutput output) { this.output = output; }

    @Test void readsOnlyConfiguredAbsoluteRegularFile() throws Exception {
        Path path = write("source", bundle(entry("K1", "ACTIVE", 1)));
        byte[] before = Files.readAllBytes(path);
        var result = available(provider(path));
        assertThat(result.active().kid()).isEqualTo("K1");
        assertThat(MessageDigest.isEqual(result.active().material(), material(1))).isTrue();
        assertThat(MessageDigest.isEqual(before, Files.readAllBytes(path))).isTrue();
        try (var files = Files.list(directory)) { assertThat(files.count()).isEqualTo(1); }
    }

    @Test void rejectsMissingBlankRelativeAndInvalidPaths() {
        for (String path : java.util.Arrays.asList(null, "", " ", "source", "./source", "../source", "\0", "file:///source",
                directory + "/./source", directory + "/other/../source"))
            unavailable(new FileMediaPlaybackSigningKeyProvider(path));
    }

    @Test void rejectsMissingDirectoriesAndSymlinkSources() throws Exception {
        unavailable(provider(directory.resolve("missing")));
        unavailable(provider(directory));
        Path path = write("source", bundle(entry("K1", "ACTIVE", 1)));
        assertSymlinkRejected(directory.resolve("linked"), path, directory.resolve("linked"));
        Path parent = directory.resolve("linked-directory");
        assertSymlinkRejected(parent, directory, parent.resolve("source"));
        BasicFileAttributes nonregular = mock(BasicFileAttributes.class);
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenReturn(nonregular);
            unavailable(provider(path));
        }
    }

    @Test void mapsReadPermissionAndIoFailuresToUnavailable() throws Exception {
        Path path = write("source", bundle(entry("K1", "ACTIVE", 1)));
        String canary = "read-failure-" + UUID.randomUUID();
        for (Exception failure : List.of(new AccessDeniedException(canary), new IOException(canary),
                new SecurityException(canary), new UnsupportedOperationException(canary))) {
            var provider = spy(provider(path));
            doThrow(failure).when(provider).readBoundedSource(path);
            unavailable(provider);
            verify(provider).readBoundedSource(path);
        }
        assertThat(output.getAll().contains(canary)).isFalse();
    }

    @Test void rejectsEmptyAndOversizedSourcesBeforeParsing() throws Exception {
        Path path = write("source", "");
        unavailable(provider(path));
        Files.write(path, new byte[65_537]);
        unavailable(provider(path));
        String valid = bundle(entry("K1", "ACTIVE", 1));
        Files.writeString(path, valid + " ".repeat(65_536 - bytes(valid).length));
        assertThat(available(provider(path)).active().kid()).isEqualTo("K1");
        for (byte[] replacement : List.of(new byte[65_537], bytes(valid.substring(0, valid.length() / 2)), bytes(valid))) {
            Files.writeString(path, valid + " ");
            BasicFileAttributes original = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            AtomicInteger reads = new AtomicInteger();
            try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
                files.when(() -> Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenAnswer(invocation -> {
                    if (reads.getAndIncrement() == 0) {
                        Files.write(path, replacement);
                        return original;
                    }
                    return invocation.callRealMethod();
                });
                unavailable(provider(path));
                assertThat(reads.get()).isEqualTo(2);
            }
        }
    }

    @Test void neverSearchesFallbackLocations() throws Exception {
        Path decoy = write("decoy", bundle(entry("K1", "ACTIVE", 1)));
        unavailable(provider(directory.resolve("missing")));
        unavailable(new FileMediaPlaybackSigningKeyProvider("decoy"));
        assertThat(available(provider(decoy)).active().kid()).isEqualTo("K1");
        Files.writeString(directory.resolve("invalid"), "{}");
        unavailable(provider(directory.resolve("invalid")));
    }

    @Test void rereadsReplacementWithoutCachedFallback() throws Exception {
        Path path = write("source", bundle(entry("K1", "ACTIVE", 1)));
        var provider = provider(path);
        assertThat(available(provider).active().kid()).isEqualTo("K1");
        Files.writeString(path, bundle(entry("K3", "VERIFY_ONLY", 3), entry("K2", "ACTIVE", 2), entry("K1", "VERIFY_ONLY", 1)));
        var rotated = available(provider);
        assertThat(rotated.active().kid()).isEqualTo("K2");
        assertThat(rotated.verifyOnly()).extracting(value -> value.kid()).containsExactly("K1", "K3");
        assertThat(MessageDigest.isEqual(rotated.active().material(), material(2))).isTrue();
    }

    @Test void removalAndCorruptionImmediatelyFailClosed() throws Exception {
        String valid = bundle(entry("K1", "ACTIVE", 1));
        Path path = write("source", valid);
        var provider = provider(path);
        available(provider);
        Files.delete(path); unavailable(provider);
        Files.writeString(path, valid); available(provider);
        for (String invalid : List.of("{", "{}", bundle(entry("K1", "DISABLED", 1)), bundle(entry("K1", "DESTROYED", 1)),
                bundle(entry("K1", "EXPIRED", 1)))) {
            Files.writeString(path, invalid); unavailable(provider);
            Files.writeString(path, valid); available(provider);
        }
    }

    @Test void returnsIndependentImmutableSnapshots() throws Exception {
        Path path = write("source", bundle(entry("K1", "ACTIVE", 1), entry("K0", "VERIFY_ONLY", 2)));
        var provider = provider(path);
        var first = available(provider);
        var second = available(provider);
        assertThat(second).isNotSameAs(first);
        java.util.Arrays.fill(second.active().material(), (byte) 0);
        java.util.Arrays.fill(first.verifyOnly().getFirst().material(), (byte) 0);
        assertThat(MessageDigest.isEqual(first.active().material(), material(1))).isTrue();
        assertThat(MessageDigest.isEqual(first.verifyOnly().getFirst().material(), material(2))).isTrue();
        assertThatThrownBy(() -> first.verifyOnly().clear()).isInstanceOf(UnsupportedOperationException.class);
        Files.writeString(path, bundle(entry("K2", "ACTIVE", 3)));
        assertThat(available(provider).active().kid()).isEqualTo("K2");
        Files.delete(path); unavailable(provider);
        assertThat(first.active().kid()).isEqualTo("K1");
    }

    @Test void redactsPathMaterialAndFailures() throws Exception {
        String canary = "path-failure-" + UUID.randomUUID();
        Path path = write(canary, bundle(entry("K1", "ACTIVE", 171)));
        var provider = provider(path);
        var mapper = JsonMapper.builder().build();
        assertThat(provider.toString()).isEqualTo("FileMediaPlaybackSigningKeyProvider[REDACTED]");
        assertThat(mapper.writeValueAsString(provider)).isEqualTo("\"FileMediaPlaybackSigningKeyProvider[REDACTED]\"");
        available(provider);
        Files.writeString(path, canary); unavailable(provider);
        unavailable(new FileMediaPlaybackSigningKeyProvider(canary + "\0"));
        var failing = spy(provider);
        doThrow(new IOException(canary)).when(failing).readBoundedSource(path);
        unavailable(failing);
        String rendered = output.getAll() + mapper.writeValueAsString(failing) + provider;
        assertThat(rendered.contains(canary) || rendered.contains(path.toString()) || rendered.contains(hex(171))).isFalse();
        assertThat(rendered.contains(java.util.Base64.getEncoder().encodeToString(material(171)))).isFalse();
    }

    private void assertSymlinkRejected(Path link, Path target, Path configured) throws Exception {
        try {
            Files.createSymbolicLink(link, target);
            unavailable(provider(configured));
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            BasicFileAttributes symbolic = mock(BasicFileAttributes.class);
            when(symbolic.isSymbolicLink()).thenReturn(true);
            try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
                files.when(() -> Files.readAttributes(link, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenReturn(symbolic);
                unavailable(provider(configured));
            }
        }
    }

    private Path write(String name, String json) throws IOException { return Files.writeString(directory.resolve(name), json); }
    private static FileMediaPlaybackSigningKeyProvider provider(Path path) { return new FileMediaPlaybackSigningKeyProvider(path.toString()); }
    private static Result.Available available(FileMediaPlaybackSigningKeyProvider provider) {
        Result result = provider.currentKeys();
        assertThat(result).isInstanceOf(Result.Available.class);
        return (Result.Available) result;
    }
    private static void unavailable(FileMediaPlaybackSigningKeyProvider provider) {
        assertThat(provider.currentKeys()).isInstanceOf(Result.Unavailable.class);
    }
}
