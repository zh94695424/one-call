package com.onecall.core.sharing.internal.keysource;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Role;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class MediaPlaybackKeyBundleParserTests {
    private final MediaPlaybackKeyBundleParser parser = new MediaPlaybackKeyBundleParser();
    private CapturedOutput output;

    @BeforeEach void captureOutput(CapturedOutput output) { this.output = output; }

    @Test void singleActiveMapsExactKey() {
        var result = available(bundle(entry("K1", "ACTIVE", 1)));
        assertThat(result.active().kid()).isEqualTo("K1");
        assertThat(result.active().role()).isEqualTo(Role.ACTIVE);
        assertThat(MessageDigest.isEqual(result.active().material(), material(1))).isTrue();
        assertThat(result.verifyOnly()).isEmpty();
    }

    @Test void zeroAndManyVerifyOnlyKeysHaveCanonicalOrder() {
        for (List<String> kids : List.of(List.<String>of(), List.of("z"), List.of("z", "a", "A", "_", "0"))) {
            var entries = new java.util.ArrayList<String>();
            entries.add(entry("K1", "ACTIVE", 1));
            for (String kid : kids) entries.add(entry(kid, "VERIFY_ONLY", 2));
            var expected = kids.stream().sorted().toList();
            for (int rotation = 0; rotation < entries.size(); rotation++) {
                java.util.Collections.rotate(entries, 1);
                var result = available(bundle(entries.toArray(String[]::new)));
                assertThat(result.active().kid()).isEqualTo("K1");
                assertThat(result.verifyOnly()).extracting(value -> value.kid()).containsExactlyElementsOf(expected);
            }
        }
        String reordered = "{\"keys\":[{\"keyHex\":\"" + hex(1) + "\",\"role\":\"ACTIVE\",\"kid\":\"K1\"}],\"schemaVersion\":1}";
        assertThat(available(reordered).active().kid()).isEqualTo("K1");
    }

    @Test void requiresExactSchemaAndRootShape() {
        String valid = bundle(entry("K1", "ACTIVE", 1));
        for (String version : List.of("null", "\"1\"", "1.0", "1e0", "true", "0", "2", "-1", "999999999999999999999"))
            unavailable(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":" + version));
        for (String root : List.of("null", "1", "true", "\"x\"", "[]", "{}", "{\"keys\":[]}")) unavailable(root);
        for (String keys : List.of("null", "{}", "1", "\"x\"", "[]", "[null]", "[1]", "[[]]"))
            unavailable("{\"schemaVersion\":1,\"keys\":" + keys + "}");
        unavailable("{\"schemaVersion\":1}");
    }

    @Test void rejectsUnknownMissingAndDuplicateFields() {
        String key = entry("K1", "ACTIVE", 1);
        String valid = bundle(key);
        for (String field : List.of("state", "status", "expiresAt", "notBefore", "algorithm", "provider", "extension")) {
            unavailable(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"" + field + "\":{}"));
            unavailable(bundle(key.replace("\"kid\":", "\"" + field + "\":null,\"kid\":")));
        }
        for (String field : List.of("kid", "role", "keyHex")) {
            var node = JsonMapper.builder().build().readTree(key);
            String value = JsonMapper.builder().build().writeValueAsString(node.path(field));
            unavailable(bundle(key.replace("\"" + field + "\":" + value, "\"" + field + "\":null")));
            unavailable(bundle(key.replace("\"" + field + "\":" + value, "\"" + field + "\":1")));
            unavailable(bundle(key.replace("\"" + field + "\":" + value, "\"" + field + "\":" + value + ",\"" + field + "\":" + value)));
            unavailable(bundle(key.replace("\"" + field + "\":" + value, "\"unknown\":" + value)));
        }
        unavailable(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"));
        unavailable(valid.replace("\"keys\":", "\"keys\":[],\"keys\":"));
        unavailable(bundle(key.replace("\"kid\":", "\"k\\u0069d\":\"other\",\"kid\":")));
        unavailable(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schema\\u0056ersion\":1"));
    }

    @Test void rejectsTrailingTokensAndNonJsonExtensions() {
        String valid = bundle(entry("K1", "ACTIVE", 1));
        assertThat(available(" \r\n\t" + valid + " \n").active().kid()).isEqualTo("K1");
        for (String suffix : List.of("{}", "[]", "null", "true", "garbage")) unavailable(valid + suffix);
        for (String invalid : List.of(valid.replace("{", "{/*comment*/"), valid.replace('"', '\''),
                valid.replace("\"schemaVersion\"", "schemaVersion"), valid.replace("1,", "1,,"),
                valid.replace("]}", "],}"), valid.replace("K1", "\\x41"), valid.replace("K1", "\\uZZZZ"),
                valid.replace(":1,", ":NaN,"), valid.replace(":1,", ":Infinity,"), valid + "//comment")) unavailable(invalid);
    }

    @Test void rejectsInvalidUtf8AndBom() {
        byte[] valid = bytes(bundle(entry("K1", "ACTIVE", 1)));
        for (byte[] prefix : List.of(new byte[]{(byte) 0xc0, (byte) 0xaf}, new byte[]{(byte) 0xc3},
                new byte[]{(byte) 0xff}, new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf})) {
            byte[] input = new byte[prefix.length + valid.length];
            System.arraycopy(prefix, 0, input, 0, prefix.length);
            System.arraycopy(valid, 0, input, prefix.length, valid.length);
            assertThat(parser.parse(input)).isInstanceOf(Result.Unavailable.class);
        }
        for (var charset : List.of(StandardCharsets.UTF_16, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE,
                java.nio.charset.Charset.forName("UTF-32")))
            assertThat(parser.parse(new String(valid, StandardCharsets.UTF_8).getBytes(charset))).isInstanceOf(Result.Unavailable.class);
        assertThat(available(bundle(entry("\\u004b1", "ACTIVE", 1))).active().kid()).isEqualTo("K1");
    }

    @Test void rejectsInvalidOrDuplicateKids() {
        for (String kid : List.of("", " ", "a b", ".", "/", "é", "\\u0000", "\\n", "\\ud800", "a".repeat(33)))
            unavailable(bundle(entry(kid, "ACTIVE", 1)));
        for (String kid : List.of("a", "a".repeat(32), "_-09AZaz")) assertThat(available(bundle(entry(kid, "ACTIVE", 1))).active().kid()).isEqualTo(kid);
        assertThat(available(bundle(entry("K1", "ACTIVE", 1), entry("k1", "VERIFY_ONLY", 1))).verifyOnly()).hasSize(1);
        for (String equivalent : List.of("K1", "\\u004b1", "K\\u0031")) {
            unavailable(bundle(entry("K1", "ACTIVE", 1), entry(equivalent, "ACTIVE", 1)));
            unavailable(bundle(entry("K1", "ACTIVE", 1), entry(equivalent, "VERIFY_ONLY", 1)));
            unavailable(bundle(entry("K2", "ACTIVE", 2), entry("K1", "VERIFY_ONLY", 1), entry(equivalent, "VERIFY_ONLY", 1)));
        }
    }

    @Test void requiresExactlyOneActive() {
        unavailable(bundle());
        unavailable(bundle(entry("K1", "VERIFY_ONLY", 1), entry("K2", "VERIFY_ONLY", 2)));
        unavailable(bundle(entry("K1", "ACTIVE", 1), entry("K2", "ACTIVE", 2)));
        unavailable(bundle(entry("K1", "ACTIVE", 1), entry("K1", "ACTIVE", 1)));
        assertThat(available(bundle(entry("K1", "ACTIVE", 1), entry("K2", "VERIFY_ONLY", 2))).verifyOnly()).hasSize(1);
    }

    @Test void rejectsMissingInvalidAndDeferredRoles() {
        for (String role : List.of("", "active", "ACTIVE ", " ACTIVE", "DISABLED", "DESTROYED", "EXPIRED", "PENDING", "DEFERRED", "OTHER"))
            unavailable(bundle(entry("K1", "ACTIVE", 1), entry("K2", role, 2)));
        for (String role : List.of("null", "1", "{}", "[]"))
            unavailable(bundle(entry("K1", "ACTIVE", 1).replace("\"ACTIVE\"", role)));
        unavailable(bundle("{\"kid\":\"K1\",\"keyHex\":\"" + hex(1) + "\"}"));
    }

    @Test void rejectsNonCanonicalHexAndWrongDecodedLength() {
        String hex = hex(171);
        var encodings = new java.util.ArrayList<>(List.of("", "00", "00".repeat(31), "00".repeat(33), hex.substring(1),
                hex.toUpperCase(java.util.Locale.ROOT), "A" + hex.substring(1), "g".repeat(64), "0x" + hex, " " + hex,
                hex + " ", String.join(":", java.util.Collections.nCopies(32, "ab"))));
        for (var encoder : List.of(Base64.getEncoder(), Base64.getEncoder().withoutPadding(), Base64.getUrlEncoder(),
                Base64.getUrlEncoder().withoutPadding(), Base64.getMimeEncoder(16, bytes("\r\n"))))
            encodings.add(encoder.encodeToString(material(171)).replace("\r", "\\r").replace("\n", "\\n"));
        for (String encoding : encodings) unavailable(bundle(entry("K1", "ACTIVE", 171).replace(hex, encoding)));
        assertThat(available(bundle(entry("K1", "ACTIVE", 171))).active().material().length).isEqualTo(32);
    }

    @Test void enforcesByteAndNestingBounds() {
        String valid = bundle(entry("K1", "ACTIVE", 1));
        String boundary = valid + " ".repeat(65_536 - bytes(valid).length);
        assertThat(available(boundary).active().kid()).isEqualTo("K1");
        unavailable(boundary + " "); unavailable("");
        assertThat(parser.parse(null)).isInstanceOf(Result.Unavailable.class);
        for (int depth : List.of(1, 7, 8, 9, 100)) unavailable("{\"schemaVersion\":1,\"keys\":" + "[".repeat(depth) + "0" + "]".repeat(depth) + "}");
        assertThat(available(bundle(entry("K1", "ACTIVE", 1), entry("K2", "VERIFY_ONLY", 2))).verifyOnly()).hasSize(1);
    }

    @Test void copiesAndRedactsAllSuccessfulAndFailedResults() {
        byte[] input = bytes(bundle(entry("K1", "ACTIVE", 171), entry("K2", "VERIFY_ONLY", 2)));
        var result = (Result.Available) parser.parse(input);
        Arrays.fill(input, (byte) 0);
        Arrays.fill(result.active().material(), (byte) 0);
        Arrays.fill(result.verifyOnly().getFirst().material(), (byte) 0);
        assertThat(MessageDigest.isEqual(result.active().material(), material(171))).isTrue();
        assertThat(MessageDigest.isEqual(result.verifyOnly().getFirst().material(), material(2))).isTrue();
        assertThatThrownBy(() -> result.verifyOnly().clear()).isInstanceOf(UnsupportedOperationException.class);
        String canary = "parser-failure-" + java.util.UUID.randomUUID();
        var failed = parser.parse(bytes(canary));
        var mapper = JsonMapper.builder().build();
        for (Object carrier : List.of(parser, result, result.active(), result.verifyOnly().getFirst(), failed)) {
            String rendered = carrier.toString() + mapper.writeValueAsString(carrier);
            assertThat(rendered.contains(hex(171)) || rendered.contains(canary)).isFalse();
            assertThat(rendered.contains(Base64.getEncoder().encodeToString(material(171)))).isFalse();
        }
        assertThat(mapper.writeValueAsString(parser)).isEqualTo("\"MediaPlaybackKeyBundleParser[REDACTED]\"");
        assertThat(result.toString()).isEqualTo("Available[REDACTED]");
        assertThat(output.getAll().contains(canary) || output.getAll().contains(hex(171))).isFalse();
    }

    private Result.Available available(String json) {
        Result result = parser.parse(bytes(json));
        assertThat(result).isInstanceOf(Result.Available.class);
        return (Result.Available) result;
    }

    private void unavailable(String json) { assertThat(parser.parse(bytes(json))).isInstanceOf(Result.Unavailable.class); }
    static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    static byte[] material(int seed) {
        byte[] material = new byte[32];
        for (int index = 0; index < material.length; index++) material[index] = (byte) (seed + index);
        return material;
    }
    static String hex(int seed) { return HexFormat.of().formatHex(material(seed)); }
    static String entry(String kid, String role, int seed) {
        return "{\"kid\":\"" + kid + "\",\"role\":\"" + role + "\",\"keyHex\":\"" + hex(seed) + "\"}";
    }
    static String bundle(String... entries) { return "{\"schemaVersion\":1,\"keys\":[" + String.join(",", entries) + "]}"; }
}
