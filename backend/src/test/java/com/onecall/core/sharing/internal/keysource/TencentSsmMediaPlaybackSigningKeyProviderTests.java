package com.onecall.core.sharing.internal.keysource;

import static com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Role;
import com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProvider.Reader;
import com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProvider.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class TencentSsmMediaPlaybackSigningKeyProviderTests {
    @TempDir Path directory;
    private CapturedOutput output;
    @BeforeEach void captureOutput(CapturedOutput output) { this.output = output; }

    @Test void validSecretStringMapsExistingBundle() {
        var result = available(provider(valid()));
        assertThat(result.active().kid()).isEqualTo("K1");
        assertThat(result.active().role()).isEqualTo(Role.ACTIVE);
        assertThat(MessageDigest.isEqual(result.active().material(), material(1))).isTrue();
        assertThat(result.verifyOnly()).isEmpty();
        assertThatThrownBy(() -> result.verifyOnly().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void zeroAndManyVerifyOnlyKeysRemainCanonical() {
        for (List<String> kids : List.of(List.<String>of(), List.of("z"), List.of("z", "a", "A", "_", "0"))) {
            var entries = new java.util.ArrayList<String>();
            entries.add(entry("K1", "ACTIVE", 1));
            for (String kid : kids) entries.add(entry(kid, "VERIFY_ONLY", 2));
            for (int rotation = 0; rotation < entries.size(); rotation++) {
                java.util.Collections.rotate(entries, 1);
                var result = available(provider(bundle(entries.toArray(String[]::new))));
                assertThat(result.verifyOnly()).extracting(value -> value.kid()).containsExactlyElementsOf(kids.stream().sorted().toList());
            }
        }
    }

    @Test void missingRequiredMetadataNeverReadsClient() throws Exception {
        for (int field = 0; field < 3; field++) for (String missing : Arrays.asList(null, "", " ", "${missing}")) {
            String[] metadata = {"test-region", "synthetic-name", "synthetic-version"};
            metadata[field] = missing;
            Reader reader = mock(Reader.class);
            unavailable(new TencentSsmMediaPlaybackSigningKeyProvider(Settings.create(metadata[0], metadata[1], metadata[2], null, null), reader));
            verifyNoInteractions(reader);
        }
    }

    @Test void invalidMetadataAndTimeoutsNeverReadClient() {
        for (int field = 0; field < 5; field++) {
            List<String> invalid = switch (field) {
                case 0 -> List.of("TEST-region", "test", "test-region ", " test-region", "a-" + "b".repeat(63), "test_1-region", "${region}");
                case 1 -> List.of(" x", "x ", "x\n", "x\0y", "x\ud800", "\udc00x", "x".repeat(1_025), "${name}");
                case 2 -> List.of(" x", "x ", "x\t", "x\u007fy", "x\ud800", "\udc00x", "x".repeat(257), "${version}");
                case 3 -> List.of("", "0", "6", "10", "-1", "+1", "01", " 2", "2 ", "2s", "٢", "${connect}");
                default -> List.of("", "0", "11", "-1", "+1", "01", " 3", "3 ", "3s", "٣", "${read}");
            };
            for (String value : invalid) {
                String[] metadata = {"test-region", "synthetic-name", "synthetic-version", null, null};
                metadata[field] = value;
                Reader reader = mock(Reader.class);
                unavailable(new TencentSsmMediaPlaybackSigningKeyProvider(Settings.create(metadata[0], metadata[1], metadata[2], metadata[3], metadata[4]), reader));
                verifyNoInteractions(reader);
            }
        }
        assertThat(Settings.create("a-b", "x".repeat(1_024), "v".repeat(256), "5", "10")).isNotNull();
    }

    @Test void eachCurrentKeysReadsExactlyOnce() {
        AtomicInteger reads = new AtomicInteger();
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings(), () -> { reads.incrementAndGet(); return valid(); });
        assertThat(reads).hasValue(0);
        for (int count = 1; count <= 4; count++) { available(provider); assertThat(reads).hasValue(count); }
    }

    @Test void successThenFailureNeverUsesLastGoodKeys() throws Exception {
        Reader reader = mock(Reader.class);
        when(reader.read()).thenReturn(valid()).thenThrow(new IOException()).thenReturn(null, "{}", valid());
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings(), reader);
        available(provider); unavailable(provider); unavailable(provider); unavailable(provider); available(provider);
        verify(reader, times(5)).read();
    }

    @Test void nullEmptyAndWhitespaceStringsAreUnavailable() {
        for (String value : Arrays.asList(null, "", " ", "\t\r\n", "\u2003")) unavailable(provider(value));
        assertThat(available(provider(" \n" + valid() + "\t")).active().kid()).isEqualTo("K1");
    }

    @Test void secretStringIsNeverBase64Decoded() {
        unavailable(provider(Base64.getEncoder().encodeToString(bytes(valid()))));
        unavailable(provider(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes(valid()))));
    }

    @Test void utf8EncodingRejectsUnpairedSurrogates() {
        for (String surrogate : List.of("\ud800", "\udfff", "\ud800x", "x\udfff"))
            unavailable(provider(valid().replace("K1", surrogate)));
    }

    @Test void sourceByteAndCharacterBoundsAreEnforced() {
        String valid = valid();
        available(provider(valid + " ".repeat(65_536 - bytes(valid).length)));
        unavailable(provider(valid + " ".repeat(65_537 - bytes(valid).length)));
        for (int characters : List.of(65_536, 65_537)) unavailable(provider("é".repeat(characters)));
        String multibyte = valid.replace("K1", "é");
        unavailable(provider(multibyte + " ".repeat(65_536 - multibyte.length())));
    }

    @Test void parserSyntaxAndSchemaRejectionsStayUnavailable() {
        String valid = valid();
        for (String invalid : List.of("null", "[]", "{}", valid + "{}", "\ufeff" + valid,
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":\"1\""),
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"),
                valid.replace("\"keys\":", "\"unknown\":0,\"keys\":"),
                valid.replace("\"kid\":", "\"kid\":\"K0\",\"kid\":"),
                valid.replace("\"K1\"", "1"), "[".repeat(10) + valid + "]".repeat(10))) unavailable(provider(invalid));
    }

    @Test void duplicateKidsAndInvalidRolesStayUnavailable() {
        for (String role : List.of("ACTIVE", "VERIFY_ONLY", "DISABLED", "DESTROYED", "EXPIRED", "UNKNOWN", "active", "")) {
            unavailable(provider(bundle(entry("K1", "ACTIVE", 1), entry("K1", role, 1))));
            if (!role.equals("VERIFY_ONLY")) unavailable(provider(bundle(entry("K1", "ACTIVE", 1), entry("K2", role, 2))));
        }
    }

    @Test void invalidKeyMaterialStaysUnavailable() {
        for (String kid : List.of("", " ", "bad.kid", "é", "x".repeat(33))) unavailable(provider(bundle(entry(kid, "ACTIVE", 1))));
        for (String hex : List.of("", "a".repeat(62), "a".repeat(66), "A".repeat(64), "g".repeat(64)))
            unavailable(provider(valid().replace(hex(1), hex)));
        unavailable(provider(bundle(entry("K0", "VERIFY_ONLY", 1))));
        unavailable(provider(bundle(entry("K1", "ACTIVE", 1), entry("K2", "ACTIVE", 2))));
    }

    @Test void readerFailuresAreUnavailableWithoutCauses() throws Exception {
        String canary = UUID.randomUUID().toString();
        for (Exception failure : List.of(new IOException(canary, new IllegalStateException(canary)), new IllegalStateException(canary))) {
            Reader reader = mock(Reader.class);
            when(reader.read()).thenThrow(failure);
            var result = new TencentSsmMediaPlaybackSigningKeyProvider(settings(), reader).currentKeys();
            assertThat(result).isEqualTo(new Result.Unavailable());
            assertThat((result + JsonMapper.builder().build().writeValueAsString(result) + output.getAll()).contains(canary)).isFalse();
        }
    }

    @Test void noFileOrAlternativeSourceFallbackExists() throws Exception {
        Path decoy = Files.writeString(directory.resolve("decoy"), valid());
        try (var files = mockConstruction(FileMediaPlaybackSigningKeyProvider.class)) {
            unavailable(provider(null)); unavailable(provider("{}"));
            assertThat(files.constructed()).isEmpty();
        }
        available(new TencentSsmMediaPlaybackSigningKeyProvider(settings(), () -> Files.readString(decoy)));
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmMediaPlaybackSigningKeyProvider.java"));
        assertThat(source).doesNotContain("FileMediaPlayback", "Files.", "getenv", "getResource", "lastGood");
    }

    @Test void snapshotsRemainIndependentAndDefensivelyCopied() {
        var provider = provider(bundle(entry("K1", "ACTIVE", 1), entry("K0", "VERIFY_ONLY", 2)));
        var first = available(provider); var second = available(provider);
        assertThat(first).isNotSameAs(second);
        Arrays.fill(first.active().material(), (byte) 0); Arrays.fill(second.verifyOnly().getFirst().material(), (byte) 0);
        assertThat(MessageDigest.isEqual(first.active().material(), material(1))).isTrue();
        assertThat(MessageDigest.isEqual(first.verifyOnly().getFirst().material(), material(2))).isTrue();
        assertThatThrownBy(() -> second.verifyOnly().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void ownedUtf8BuffersAreCleared() {
        var realParser = new MediaPlaybackKeyBundleParser();
        for (boolean failing : List.of(false, true)) {
            AtomicReference<byte[]> observed = new AtomicReference<>();
            try (var parsers = mockConstruction(MediaPlaybackKeyBundleParser.class, (parser, context) ->
                    when(parser.parse(any(byte[].class))).thenAnswer(invocation -> {
                        byte[] source = invocation.getArgument(0); observed.set(source);
                        assertThat(MessageDigest.isEqual(source, bytes(valid()))).isTrue();
                        if (failing) throw new IllegalStateException();
                        return realParser.parse(source);
                    }))) {
                var provider = provider(valid());
                if (failing) unavailable(provider);
                else {
                    var result = provider.currentKeys();
                    assertThat(result).isNotNull();
                }
                assertThat(observed.get()).isNotNull();
                assertThat(Arrays.equals(observed.get(), new byte[observed.get().length])).isTrue();
            }
        }
    }

    @Test void providerAndResultsRedactMaterialAndMetadata() {
        String canary = "synthetic-" + UUID.randomUUID();
        var settings = Settings.create("test-region", canary, canary, null, null);
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings, () -> bundle(entry("K1", "ACTIVE", 171)));
        var mapper = JsonMapper.builder().build();
        var result = available(provider);
        String rendered = output.getAll();
        for (Object carrier : List.of(settings, provider, result, result.active(), provider(null).currentKeys()))
            rendered += carrier + mapper.writeValueAsString(carrier);
        for (String secret : List.of(canary, "test-region", hex(171), Base64.getEncoder().encodeToString(material(171))))
            assertThat(rendered.contains(secret)).isFalse();
        assertThat(provider.toString()).isEqualTo("TencentSsmMediaPlaybackSigningKeyProvider[REDACTED]");
    }

    static Settings settings() { return Settings.create("test-region", "synthetic-name", "synthetic-version", null, null); }
    static String valid() { return bundle(entry("K1", "ACTIVE", 1)); }
    static TencentSsmMediaPlaybackSigningKeyProvider provider(String value) { return new TencentSsmMediaPlaybackSigningKeyProvider(settings(), () -> value); }
    static Result.Available available(TencentSsmMediaPlaybackSigningKeyProvider provider) {
        var result = provider.currentKeys(); assertThat(result).isInstanceOf(Result.Available.class); return (Result.Available) result;
    }
    static void unavailable(TencentSsmMediaPlaybackSigningKeyProvider provider) { assertThat(provider.currentKeys()).isEqualTo(new Result.Unavailable()); }
}
