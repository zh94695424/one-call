package com.onecall.core.sharing.internal.keysource;

import com.fasterxml.jackson.annotation.JsonValue;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Entry;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Role;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class MediaPlaybackKeyBundleParser {
    static final int MAX_SOURCE_BYTES = 65_536;
    private final JsonMapper mapper = JsonMapper.builder(JsonFactory.builder()
                    .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(8).build()).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    public Result parse(byte[] source) {
        try {
            if (source == null || source.length == 0 || source.length > MAX_SOURCE_BYTES) return new Result.Unavailable();
            String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(source)).toString();
            if (json.charAt(0) == '\ufeff') return new Result.Unavailable();
            JsonNode root = mapper.readTree(json);
            closed(root, Set.of("schemaVersion", "keys"));
            JsonNode version = root.path("schemaVersion");
            if (!version.isIntegralNumber() || !version.canConvertToInt() || version.asInt() != 1) return new Result.Unavailable();
            JsonNode keys = root.path("keys");
            if (!keys.isArray() || keys.isEmpty()) return new Result.Unavailable();
            Entry active = null;
            var verifyOnly = new ArrayList<Entry>();
            var kids = new HashSet<String>();
            for (JsonNode key : keys) {
                closed(key, Set.of("kid", "role", "keyHex"));
                String kid = text(key, "kid");
                String role = text(key, "role");
                String hex = text(key, "keyHex");
                if (!kid.matches("[A-Za-z0-9_-]{1,32}") || !kids.add(kid)
                        || !hex.matches("[0-9a-f]{64}")) return new Result.Unavailable();
                Role parsedRole = Role.valueOf(role);
                byte[] material = HexFormat.of().parseHex(hex);
                Entry entry;
                try {
                    if (material.length != 32) return new Result.Unavailable();
                    entry = new Entry(kid, material, parsedRole);
                } finally { Arrays.fill(material, (byte) 0); }
                if (parsedRole == Role.ACTIVE) {
                    if (active != null) return new Result.Unavailable();
                    active = entry;
                } else verifyOnly.add(entry);
            }
            if (active == null) return new Result.Unavailable();
            verifyOnly.sort(Comparator.comparing(Entry::kid));
            return new Result.Available(active, verifyOnly);
        } catch (CharacterCodingException | RuntimeException failure) {
            return new Result.Unavailable();
        }
    }

    private static void closed(JsonNode node, Set<String> fields) {
        if (node == null || !node.isObject() || node.size() != fields.size()
                || node.properties().stream().anyMatch(field -> !fields.contains(field.getKey())))
            throw new IllegalArgumentException();
    }

    private static String text(JsonNode node, String field) {
        if (!node.path(field).isString()) throw new IllegalArgumentException();
        return node.path(field).asString();
    }

    @JsonValue public String redacted() { return "MediaPlaybackKeyBundleParser[REDACTED]"; }
    @Override public String toString() { return redacted(); }
}
