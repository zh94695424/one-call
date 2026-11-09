# PA22 — sharing media production key custody and provider integration plan

## 1. Status, authority and goal

- Task: `pa22-key-custody-plan-gpt-20261110-01`; work unit: `PA22-sharing-media-production-key-custody-and-provider-integration`; gate: **plan authoring only**; inspected on 2026-11-10. This document is an implementation specification, not implementation, review, acceptance or production-readiness evidence.
- Binding controller: `docs/gateflow/project-execution-control.md:13`, especially “PA22 sharing media production key custody and provider integration - goal confirmation accepted”. Design sources: `docs/12-technology-stack-and-system-architecture.md` and `docs/17-production-security-design-spec.md`. The controller's accepted scope takes precedence over speculative improvements.
- Motivation: PA21 already implements C10 issuance/verification/replay but intentionally has no production key source. Supply one bounded, provider-neutral structured-bundle boundary and one read-only local/mounted-file adapter behind the existing `MediaPlaybackSigningKeyProvider`; do not introduce another authority or a key-management platform.
- Success: an explicitly configured external file yields exactly one ACTIVE and zero or more VERIFY_ONLY entries with valid kids and exactly 32 decoded bytes each. All source/schema/state/provider failures fail closed. Deterministic tests prove overlap, old credential verification and exact replay, ACTIVE-only fresh issuance, removal behavior, redaction, default unavailability and unchanged PA21 wire/current-state behavior.
- Workspace evidence: native `.git` contains a Windows `gitdir` and plain Linux `git status` fails. Read-only inspection works with `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call24 --work-tree="$PWD" ...`; do not rewrite Git metadata. Actual HEAD is `dce09ae516856a36e7725e58261b2b7c7c5249b3` (`gateflow: confirm PA22 goal`), immediately above PA21 merge `8452296bb0b2cc79968801cde0a40eef2ab20dd8`. Initial status also includes an untracked `.codex`; it is pre-existing, not this task's output, and must remain untouched.

### Goal alignment vocabulary

Every decision and test below implements at least one of these accepted goals; these labels are not additional requirements:

| ID | Confirmed goal / success signal |
| --- | --- |
| G1 | One provider-neutral external bundle maps to one ACTIVE plus zero/many VERIFY_ONLY keys. |
| G2 | Strict validation and closed failures for source, structure, encoding, state and ambiguity. |
| G3 | Real authority overlap, ACTIVE-only fresh issuance, exact recorded-key replay and retirement failure. |
| G4 | Secret/token redaction, copying and immutable results. |
| G5 | Preserve PA21/default 503, wire, current-state, architecture, contracts, schema and dependency pins. |

### Scope and non-goals

The sole implementation slice is a parser, a file provider, its explicit Spring composition and focused tests/inventory maintenance (G1–G5). No Tencent Secret Manager/KMS/COS call, SDK, credentials, provisioning, live cloud verification, concurrency work, production material, COS byte read/upload, player, CDN/TLS/LB/WAF, Q22 duration/skew/revocation-window value, cache, scheduled/background reloader, registry UI/admin, new authorization source, public API, database/ACL/migration, Docker topology or deployment-manifest change. Preserve all migrations, including V001–V008 and the existing V009–V013; preserve OpenAPI/DTO/controllers and every current POM version. No merge, approval, ready state, reviewer request, external comment or issue mutation. Future concerns are residuals, not extra slices.

## 2. Direct code evidence and constraints

Paths and method names are the inspected source anchors; no conclusion below depends solely on the task prompt.

| Evidence inspected | Consequence / alignment |
| --- | --- |
| `backend/src/main/java/com/onecall/core/sharing/internal/credential/MediaPlaybackSigningKeyProvider.java`: `Role`, `Entry`, `Result.Available` | Reuse ACTIVE/VERIFY_ONLY and the existing sealed result. Entry construction/access clone byte arrays; Available copies its list; both redact `toString`/Jackson via `@JsonValue`. Do not replace this contract (G1/G4/G5). |
| `.../credential/MediaPlaybackCredentialAuthority.java`: `snapshot`, `validEntry`, `issueOrReplay`, `verify` | Existing snapshot enumerates at most two beans and requires exactly one; validates kid `[A-Za-z0-9_-]{1,32}`, 32 bytes and roles; rejects duplicate kids; catches provider runtime failures. Snapshot validation precedes reading token value. Fresh issuance chooses ACTIVE; recorded replay selects anchor kid even if VERIFY_ONLY; missing recorded kid yields Unavailable. Keep this implementation and constructor unchanged (G2/G3/G5). |
| `.../credential/MediaPlaybackCredentialCodec.java`: `encode`, `decode`, `mac` | Existing `ocmp1` envelope, audience/domain separation, canonical unpadded base64url credential encoding, signed kid and 32-byte MAC stay untouched. External key encoding below is not a new credential wire format (G3/G5). |
| `.../playback/MediaPlaybackConfiguration.java`: `mediaPlaybackCredentialAuthority` | One unconditional authority bean already consumes `ObjectProvider<MediaPlaybackSigningKeyProvider>` and supplies both existing interfaces. Keep this bean unconditional and do not alter its configuration file (G5). |
| `backend/src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionJsonBinder.java`: mapper setup and `bindMediaAuthorization` | Existing project uses Jackson 3 `tools.jackson.*`, strict duplicate detection, trailing-token rejection, explicit field closure and reporting UTF-8 decoding. Reuse that bounded pattern, not an injected globally lenient mapper or a new JSON dependency (G2/G5). |
| `backend/src/test/java/com/onecall/archtests/sharing/MediaPlaybackCredentialAuthorityTests.java` (24 tests) | Existing golden PLAYBACK/SEEK/RENEW, malformed wire, only-active issuance, verify-only replay, missing key, defensive copying, microsecond replay and redaction remain unchanged. New tests must exercise those behaviors through an actual file adapter, not merely another lambda (G3/G5). |
| `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` (9 tests) | Default/web, persistence consumers, bean registration orders, zero/ambiguous keys and throwing providers already have coverage; extend without changing existing constructors or test semantics (G2/G5). |
| `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`: `sharingQueryApiAndProjectionAreExact`, `playbackUsesOnlyNamedEvidenceReadAndPrivateTypedScope` | Exact credential file list has three files; sharing internal file count is 76. Credential import whitelist deliberately excludes file/JSON/config plumbing; playback bans `byte[]`/`InputStream`. Put new plumbing in private sibling `internal/keysource`, preserve existing credential/playback restrictions and add a separate exact keysource boundary (G1/G5). |
| `MediaPlaybackServiceTests`, `MediaGatewayWebTests`, `PublicShareAdmissionIT`, `MediaPlaybackIT`, `MediaGatewayTransportIT` | Existing service denial order, 503 envelope, committed replay/event behavior, current-state rechecks, evidence identity/window and transfer boundaries remain regression obligations; no test fixture/constructor rewiring is needed (G3/G5). |
| `backend/pom.xml`, `.github/workflows/ci.yml`, `.java-version`, `.mvn/wrapper/maven-wrapper.properties` under `backend/` | Actual pins are Java 21 / JDK 21.0.13+9, Maven 3.9.9, Boot 4.1.2, Modulith 2.1.2, Testcontainers 2.0.5, Enforcer 3.6.3; do not infer Boot 3 from architectural recommendations. Existing Jackson/Mockito/JUnit suffice; no POM change (G5). |
| Security design sections containing K20 and C10 (`docs/17-production-security-design-spec.md:141`, `:191`, `:196`, `:198`, `:334`, `:360`) | K20 custody is Secret Manager; HMAC-SHA-256/256-bit; new ACTIVE/old VERIFY_ONLY overlap; disabled/destroyed fail closed; Q22 remains deferred. File adapter models the handoff, not proof of Tencent custody/provisioning (G1–G5). |

Inspect all direct users with `rg -n 'MediaPlaybackSigningKeyProvider|MediaPlaybackCredentialAuthority|MediaPlaybackConfiguration' backend/src/main backend/src/test`. Existing users include deterministic command providers, admission fixtures and transport/current-state ITs; all retain their signatures. Architecture doc sections 6, 8 and 13 retain module ownership, adapter normalization and existing validation approach.

## 3. Exact external bundle contract — version 1 (G1/G2/G4)

Logical shape only (metavariables are deliberately not a valid deployable bundle; no key bytes are embedded here):

```text
{
  "schemaVersion": 1,
  "keys": [
    {"kid": <valid-kid>, "role": "ACTIVE", "keyHex": <64-lowercase-hex-characters>},
    {"kid": <another-valid-kid>, "role": "VERIFY_ONLY", "keyHex": <64-lowercase-hex-characters>}
  ]
}
```

1. Input is one JSON object encoded in strict UTF-8, without a UTF-8 BOM. Invalid/truncated/overlong UTF-8, unmappable input, UTF-16/32 and BOMs fail. Decode with reporting errors before parsing; do not allow Jackson byte-based encoding auto-detection. Standard JSON whitespace is permitted before/after the object; byte size includes all whitespace. Empty input is invalid.
2. Maximum source/parser input size is **65,536 bytes inclusive**. Read at most 65,537 bytes to detect overflow; never unbounded `readAllBytes`. Enforce the same limit in parser entry as well as the file reader. Use a parser nesting-depth ceiling of 8; accepted schema itself only needs root/array/entry containers. This is a resource bound, not a Q22/time value.
3. Root field set is exactly `{schemaVersion, keys}`, each once. `schemaVersion` must be a JSON integer token with value 1; reject null, strings, booleans, 1.0/1e0, other integers and absent values. `keys` must be a nonempty array of objects. No wrapper, bundle list, second document or multiple-source merge is supported.
4. Each entry field set is exactly `{kid, role, keyHex}`, each once and with textual values. Unknown fields at every object level fail, including `state`, `status`, `expiresAt`, `notBefore`, provider/algorithm identifiers and nested extensions. Missing/null/nontext fields fail. No defaulting, scalar coercion or ignored fields. Object field order is insignificant. Standard JSON escapes are allowed only when the decoded value satisfies its field constraints; no lone surrogate or control-character value can satisfy kid/role/hex validation.
5. Standard strict JSON only: duplicate object properties (including escaped-equivalent names), comments, single quotes, unquoted names, trailing commas, NaN/Infinity, malformed escapes and trailing tokens all fail. Use a dedicated local strict mapper with `StreamReadFeature.STRICT_DUPLICATE_DETECTION` and `DeserializationFeature.FAIL_ON_TRAILING_TOKENS`; validate exact shape explicitly rather than relying on permissive POJO binding. Do not retain or expose its JsonNode tree.
6. `kid` is case-sensitive ASCII `[A-Za-z0-9_-]{1,32}`, exactly matching PA21. No trimming, Unicode normalization or case folding. Empty/blank, dot, slash, spaces, non-ASCII, length 33 and control characters fail. `K1` and `k1` are distinct.
7. `role` is exactly `ACTIVE` or `VERIFY_ONLY`, case-sensitive with no trimming. Exactly one ACTIVE is required; zero or two/more ACTIVE entries fail. VERIFY_ONLY multiplicity is zero through the number fitting the byte bound; no extra arbitrary count limit. Missing/invalid roles fail the entire bundle. `DISABLED`, `DESTROYED`, `EXPIRED`, `PENDING`, `DEFERRED`, unknown values and attempted separate lifecycle/expiry fields all fail the entire bundle, never get filtered out. This seam does not infer expiry or implement a clock/scheduler/state conversion.
8. `keyHex` is exactly 64 ASCII lowercase characters matching `[0-9a-f]{64}` and must decode to exactly 32 bytes. Reject uppercase/mixed-case hex, `0x`, separators, spaces, odd lengths, invalid digits and shorter/longer decoded keys (including 0/1/31/33 bytes). **No base64 variant is accepted**: standard/base64url, padded/unpadded, MIME, alternate alphabets and encoding-selector fields are unsupported; no decoder guessing. The credential codec's base64url rules remain unchanged.
9. Any duplicate kid, including identical duplicate entries, active/verify-only overlap on the same kid, or verify-only/verify-only duplicates, rejects the whole bundle before Available construction. Kid equality is exact decoded-string equality. Different kids with equal key bytes are permitted, matching the existing provider contract; kid is still signed and is the only replay lookup identity. Never deduplicate, alias, merge or select by equal bytes. Platform/Security owns generation of distinct production key versions; adding a cross-snapshot/material registry is outside scope.
10. Input array order is not authoritative. Extract the single ACTIVE separately and sort VERIFY_ONLY entries by ascending ASCII kid (Java natural string order is equivalent for the allowed alphabet); return every accepted verify-only entry exactly once. All input permutations of the same bundle produce the same ordered snapshot. Map only via existing `new Entry(kid, decoded, Role.ACTIVE/VERIFY_ONLY)` and `new Result.Available(active, sortedVerifyOnly)`; no new state enum, key ring or domain DTO.
11. Any failure returns `new Result.Unavailable()` for the whole snapshot. No partial keyset, last-good key, skipped bad entry, default kid or fallback key. Parser catches expected decode/validation/runtime parser failures internally without logging or propagating parser exception messages/causes; do not catch JVM fatal `Error` as recoverable availability.

Rationale: a closed small schema, one encoding and existing roles are sufficient for G1/G2. Adding a generic secret envelope, algorithm negotiation, timestamps, cloud version registry, JWT or crypto library would broaden the accepted seam and obscure PA21 authority ownership.

## 4. Local/mounted file source and lifecycle (G1/G2/G4/G5)

### Configuration and visibility

- Exact properties: `onecall.sharing.media-playback.signing-key-source.provider=file` and `onecall.sharing.media-playback.signing-key-source.bundle-path=<absolute-external-file-path>`.
- `provider` has no default; `file` is the sole supported adapter selection. Absent, blank, `false`, unknown or compound selections create no local provider. Path alone must not activate it. `file` with missing/blank/invalid path creates one configured but unavailable provider, not a startup exception or an accidental fallback.
- Both properties are private runtime operator configuration, never public DTO/OpenAPI/event fields; only the selector and filesystem location are configuration, never key bytes. No profile is required or auto-enabling: explicit opt-in works in default/web/persistence combinations. Do not add `test`/`dev` bypasses or `application*.yml` defaults, examples containing secrets, logging or actuator exposure.
- Resolve only the explicit path property through Spring's existing configuration system. Normal Spring resolution of these two metadata properties is not a key-material environment fallback. Never read raw key material from environment/system properties, classpath resources, inline configuration, a home directory, a default filename or alternate secret locations. No URI, glob, directory scan or path-list syntax.
- Use a small public `MediaPlaybackKeySourceConfiguration` in `sharing.internal.keysource` with `@Configuration(proxyBeanMethods=false)` and a single `@Bean` conditioned by `@ConditionalOnProperty(prefix="onecall.sharing.media-playback.signing-key-source", name="provider", havingValue="file", matchIfMissing=false)`. It returns `MediaPlaybackSigningKeyProvider`, instantiated as `FileMediaPlaybackSigningKeyProvider`. Read path metadata without throwing on malformed/missing property resolution; convert such resolution failure into an adapter with an unavailable path. No secret-bearing properties record is needed.

### File boundary and failure behavior

- Store only configured path metadata, not bytes or a last-good result. Validate within `currentKeys()`: path must be nonblank, absolute, already normalized (reject `.`/`..` components rather than silently rewriting), and identify exactly one regular file. The explicitly configured absolute filename and its normalized parent form the allowed boundary; no caller/request controls the path and there is no second root-search mechanism.
- Reject symlinks in the configured file or its path components; use no-follow attribute checks and a read-only byte channel opened with READ and NOFOLLOW_LINKS. Do not follow a link outside the explicit boundary. Do not traverse directories looking for a bundle. Reject directories, devices/FIFOs/nonregular files and unsupported no-follow operations. This deliberately excludes symlink-based secret mounts; Platform must supply a direct regular-file mount for this adapter.
- The application opens **only for reading**: no CREATE/WRITE/APPEND, chmod, copied/persisted secret, temp copy, rename or deletion. Deployment owns read-only mount and filesystem ACLs. Do not reject a file merely because a test filesystem reports write permission; claiming deployment read-only enforcement from that check would be false.
- Check regular-file attributes and reported size, then enforce the byte bound while actually reading with a closed-on-all-paths channel. An advertised small size is not sufficient. Missing paths, disappearing files, access denied, permission/security errors, invalid paths, unreadable files, short/inconsistent read failures, unsupported filesystem options and oversize input map to Result.Unavailable. Do not log the path, content, parsed values or caught exception. Do not emit or attach an IOException/Jackson exception as a cause.
- Use a package-private `readBoundedSource(Path)` helper in the adapter for the actual bounded file I/O; it is not another provider or a public loader SPI. It allows a same-package test spy to deterministically throw `AccessDeniedException`, `IOException` and `SecurityException` even when the runner is root. Also exercise real temporary-file reads, absence, nonregular files, size and removal. No production test flags or permission assumptions/skips.

### Read timing

Read and parse the file **on every `currentKeys()` call**; the authority already takes one key snapshot per issuance/replay/verification operation. No initialization snapshot, cache, scheduler, watch service, mtime shortcut, last-good fallback or asynchronous reload. A replaced valid file takes effect on the next call. Removal, corruption, unsupported state or unreadability makes the next call Unavailable; restoring a valid file allows a later call to recover. Returned earlier snapshots remain immutable and independent.

This bounded seam deliberately trades one bounded local read/parse per authority call for simple, testable retirement/failure semantics. No throughput/SLO claim is made. Operators should replace the whole file coherently; an observed incomplete update fails closed. Concurrent rotation guarantees and hostile filesystem race elimination are not claimed or added here. An already-running operation uses its one validated snapshot; there is no retroactive recall promise. Platform/Security decides overlap/retirement timing later, without filling in Q22 here.

## 5. Exact Spring composition and authority semantics (G2/G3/G5)

| Configured provider beans visible to the existing authority | Required behavior |
| --- | --- |
| Zero | Existing unconditional authority still starts; issueOrReplay and verify are Unavailable. Existing gateway no-key path remains 503/MEDIA_UNAVAILABLE. No replacement authority or fixture is created. |
| One local file provider, valid source | Existing authority validates and uses its current snapshot; one authority instance supplies issuer and verifier interfaces to existing consumers. |
| One local file provider, invalid source/path | Context still starts; each call is Unavailable. Malformed token parsing and state/evidence/byte access must not run. |
| One explicitly supplied existing custom provider, local adapter not selected | Preserve PA21's single-provider behavior; this property selects only the local adapter, not a new global registry. |
| File adapter plus a custom provider, or two custom providers | Context retains exactly one authority but operations fail Unavailable. Existing `keys.stream().limit(2)` cardinality check must run before calling either provider. No arbitrary order, `getIfAvailable`, `@Primary`, `@Order`, missing-bean suppression or preferred-bean winner. Test both registration orders and a primary decoy. |

Do not add a competing provider in the default configuration or a second authority in the new configuration. Do not use `@ConditionalOnMissingBean` to hide ambiguity. Do not modify `MediaPlaybackConfiguration`, `MediaPlaybackCredentialAuthority` or their constructor wiring. Existing discovery of the new configuration by component scanning is sufficient; focused context tests explicitly import it alongside the existing configuration.

Preserve key-validation-before-token-parse ordering by leaving `snapshot()` unchanged. With a bad/missing bundle or ambiguous providers, verify must return Unavailable without invoking mocked `OpaquePlaybackCredential.value()`. Replay must never mint a substitute ACTIVE credential if its recorded kid is missing. Distinguish valid-bundle retirement from bundle unavailability: after removing an old kid while retaining a valid ACTIVE, old-token verify is Invalid, old-anchor replay is Unavailable, and fresh issuance uses the new ACTIVE. If any entry instead carries DISABLED/DESTROYED/EXPIRED, the entire bundle is Unavailable. VERIFY_ONLY is allowed for deterministic reconstruction of a previously recorded credential, not selection for fresh issuance; this preserves PA21's overlap semantics.

## 6. Ownership, copying and redaction (G1/G4/G5)

- Parser holds JSON/decoded material only in local variables; no public bundle DTO, record, event, exception carrier or additional key registry. Provider retains only source location and parser, and reads fresh bytes each call. Parser and provider override `toString` and expose `@JsonValue` constant redacted strings; serializing either cannot expose path, fields, JsonNode, bytes or derived material.
- Construct Entry using decoded temporary arrays; existing constructor clones, and accessor clones remain unchanged. Clear owned decoded temporary arrays after Entry construction and owned file buffers in finally where practical. Do not mutate caller-owned parser input. Available returns the existing immutable defensive list, sorted before construction. No source array or mutable collection is exposed.
- No Java `Serializable`, Jackson secret DTO, global serializer customization, new logger, exception formatting with user/source values, suppressed/cause attachment, metrics labels containing key metadata or source dumps. Parser/provider failure is a result, not a thrown secret-bearing exception. No guarantee of complete JVM heap erasure: Jackson strings/trees and HMAC copies have ordinary managed-memory lifetimes.
- Tests generate deterministic **synthetic** 32-byte arrays in memory, encode only when writing under JUnit `@TempDir`, and clean them automatically. Never commit bundle fixtures with material or use real secrets. Tests/report artifacts must not print secret/token hex/base64, byte-array assertion diffs, bundle JSON, source paths containing canaries or credential substrings. Compare sensitive values via boolean equality/constant-time byte equality with generic assertion labels instead of rendering actual/expected values.
- Redaction tests capture stdout/stderr and application logs using existing OutputCaptureExtension and/or a detached-after-test logback appender. Check positive/negative source, parser, provider, available/entry, authority request/result and opaque token paths with generated canaries. Assert JSON and toString are constant/redacted and contain neither raw/hex/base64 synthetic key material, token payload/MAC/full value nor failure/path canaries. Do not add public serialization of secrets to prove decoding.

## 7. One behaviorally verifiable implementation slice (G1–G5)

**Slice S1: explicitly mounted K20 bundle drives the existing PA21 authority, with closed rotation/replay and failure semantics.** Implement parser + bounded file adapter + one conditional provider bean; prove it with file-backed authority/composition tests; maintain exact architecture/CI inventory. This is one security behavior, not parser/config/test slices. No separately security-critical boundary requires a second slice; maximum remains one, not the default ceiling of three.

Acceptance exit for S1: all matrix tests below and all existing regressions are green with source=CI=XML exactness, zero skipped/failed/errored tests, unchanged POM and contracts/migrations. A temporary-file rotation must show old ACTIVE → VERIFY_ONLY overlap → removed, preserving exact previously issued credential during overlap and refusing replacement after removal. Invalid source must not parse tokens or read downstream state. Implementation reports limitations instead of claiming cloud/deployment readiness.

## 8. Exhaustive concrete test matrix and exact additions

Use plain `@Test void` methods with loops for matrix cases, not parameterized/repeated/dynamic/disabled/conditional tests (the existing CI inventory forbids those). No new tests are counted inside helpers. Every new method below is mandatory; names are the CI inventory contract. Test source placement in the same private keysource package enables the bounded I/O failure spy without expanding the production API.

### A. New `com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests` — 12 methods

| Method | Concrete assertions / alignment |
| --- | --- |
| `singleActiveMapsExactKey` | Valid minimal v1 maps the exact synthetic decoded bytes/kid/ACTIVE; verify-only list empty; no issuance responsibility in parser (G1/G4). |
| `zeroAndManyVerifyOnlyKeysHaveCanonicalOrder` | Zero, one, several VERIFY_ONLY entries; permute input and object-field order; ACTIVE separate, verify-only ASCII-sorted, none lost (G1). |
| `requiresExactSchemaAndRootShape` | Missing/null/string/float/exponent/boolean/unsupported schema, null/scalar/array root, absent/null/nonarray/empty keys and nonobject entries all Unavailable; integer 1 only accepted (G2). |
| `rejectsUnknownMissingAndDuplicateFields` | Root and entry unknown/missing/null/wrong-type fields, duplicate root/entry properties, escaped-equivalent duplicate names and attempted algorithm/state/expiry extensions reject whole bundle (G2). |
| `rejectsTrailingTokensAndNonJsonExtensions` | Second root/garbage, comments, single quotes, unquoted fields, trailing comma, malformed escape and non-JSON numbers rejected; standard surrounding whitespace accepted (G2). |
| `rejectsInvalidUtf8AndBom` | Invalid/overlong/truncated byte sequences, UTF-8 BOM, UTF-16/32 input reject without exception text; valid UTF-8 JSON escapes decode before field validation (G2/G4). |
| `rejectsInvalidOrDuplicateKids` | Null/blank/space/dot/slash/non-ASCII/control/33-character kids reject; 1/32-character valid boundaries and case distinctions accepted; repeated ACTIVE/VERIFY_ONLY and verify-only duplicates reject even for identical material (G1/G2). |
| `requiresExactlyOneActive` | No ACTIVE (all verify-only), multiple ACTIVE (distinct or repeated kids), empty keys fail; one ACTIVE plus zero/many verify-only succeeds (G1/G2). |
| `rejectsMissingInvalidAndDeferredRoles` | Missing/null/nontext/blank/lowercase/whitespace/unknown role; DISABLED, DESTROYED, EXPIRED, PENDING and DEFERRED reject entire set, including when another valid ACTIVE exists (G2). |
| `rejectsNonCanonicalHexAndWrongDecodedLength` | 0/1/31/33-byte encodings, odd/invalid/mixed/uppercase/prefixed/separated hex and all base64 variants fail; only 64 lowercase hex yielding 32 bytes passes (G2). |
| `enforcesByteAndNestingBounds` | Valid exact-65,536-byte input via legal whitespace accepted; 65,537 bytes, empty input and nesting beyond ceiling fail; wrong nested shapes fail even below ceiling (G2). |
| `copiesAndRedactsAllSuccessfulAndFailedResults` | Mutate parser input and returned material accessor copies after parsing; snapshot unchanged; verify-only list unmodifiable; mapper/toString/log output contains no key/canary on success or malformed bundle (G4). |

### B. New `com.onecall.core.sharing.internal.keysource.FileMediaPlaybackSigningKeyProviderTests` — 10 methods

| Method | Concrete assertions / alignment |
| --- | --- |
| `readsOnlyConfiguredAbsoluteRegularFile` | Real @TempDir regular file with valid bundle becomes Available; no writes/copies; decoded content matches without printing it; provider maps through parser (G1/G2). |
| `rejectsMissingBlankRelativeAndInvalidPaths` | Null/empty/blank/relative/non-normalized/NUL/URI-style configuration yields Unavailable, not construction/startup exceptions or fallback (G2/G5). |
| `rejectsMissingDirectoriesAndSymlinkSources` | Missing file, directory, final-file symlink and parent-component symlink fail; simulate nonregular attributes deterministically if platform cannot create a special file/link; no disabled/skipped tests (G2). |
| `mapsReadPermissionAndIoFailuresToUnavailable` | Package-private bounded-read spy throws AccessDeniedException, IOException and SecurityException with generated canaries; all return Unavailable, none leak or escape. Exercise unsupported filesystem operation failure too (G2/G4). |
| `rejectsEmptyAndOversizedSourcesBeforeParsing` | Real zero-byte and 65,537-byte files fail; 65,536 legal-byte bundle succeeds; bounded-read overflow still fails when advertised size is smaller; exercise failure after initial attributes (G2). |
| `neverSearchesFallbackLocations` | Explicit target missing/invalid while a valid decoy sits in another @TempDir location still fails; supplying the decoy's explicit absolute path succeeds. Architecture assertions prohibit classpath/env/directory/default search; tests never write outside temp storage (G2/G5). |
| `rereadsReplacementWithoutCachedFallback` | Same provider observes valid K1 file then replacement K2+K1-overlap file on next call, yielding new ACTIVE and sorted verify-only; not startup-only state (G1/G3). |
| `removalAndCorruptionImmediatelyFailClosed` | Valid→deleted→restored→malformed→restored source returns Available/Unavailable accordingly, with no last-good fallback; DISABLED/DESTROYED/EXPIRED updates fail whole snapshot (G2/G3). |
| `returnsIndependentImmutableSnapshots` | Two reads, input replacement and accessor mutation cannot alter earlier snapshot; verify-only list immutable; all channels close so temp files can be replaced/deleted (G4). |
| `redactsPathMaterialAndFailures` | Provider/parser JSON and toString constant; success, corrupt input, invalid paths and injected I/O failures emit no path/key/token/failure canary to captured logs/stdout/stderr and do not propagate causes (G4). |

For synthetic nonregular/link/read-overflow branches, use narrowly scoped Mockito mocking or spies with existing dependencies; actual temp-file happy/read/remove cases remain real I/O. Do not add a filesystem abstraction library, OS-specific subprocess, mock server or permissive production hook.

### C. New `com.onecall.core.sharing.internal.keysource.MediaPlaybackFileKeySourceIntegrationTests` — 8 methods

Use real parser, real temp file provider, existing authority and fixed Clock. Build ordinary valid PA21 requests/anchors in local helpers; do not alter the existing deterministic-provider fixture or codec. All purpose loops cover PLAYBACK, SEEK and RENEW where meaningful.

| Method | Concrete assertions / alignment |
| --- | --- |
| `fileBundleIssuesOnlyActiveForEveryPurpose` | File has ACTIVE K2 and several VERIFY_ONLY kids with distinct synthetic material; fresh requests select K2 in token and anchor for every purpose, round-trip exact claims; no verify-only selection (G1/G3/G5). |
| `rotationOverlapVerifiesOldAndNewCredentials` | Issue under K1-only; replace file with ACTIVE K2 + VERIFY_ONLY K1 using distinct material; old token and newly issued K2 token both verify; new token/anchor uses K2 (G3). |
| `overlapReplaysExactPreviouslyIssuedCredential` | Preserve first request's expiry/recorded anchor, advance clock within synthetic witness bounds, rotate to overlap; byte-for-byte first credential, expiry and anchor reconstructed, not newly issued/rebound to K2. Compare secrets via boolean equality (G3/G4/G5). |
| `recordedKeyRemovalNeverMintsReplacement` | Remove K1 from otherwise valid K2 bundle: old token Invalid, K1 replay Unavailable, fresh K2 issuance succeeds; no replacement credential or mutated recorded anchor (G2/G3). |
| `badBundlePrecedesTokenParsingAndDownstreamReads` | Missing/corrupt/disabled/destroyed/expired file: verify(mock opaque) is Unavailable and never calls value(); issueOrReplay unavailable; composed service has zero state/evidence/reader interactions (G2/G5). |
| `fileBackedClaimsStillRequireCurrentState` | File-issued valid claim does not bypass mocked revoked/stale-generation/session-expired/wrong-object/risk/current-state denials; state denial prevents evidence/controlled read, evidence mismatch prevents byte open; no writes/events introduced (G3/G5). |
| `bundleFailuresAndCredentialsNeverLeak` | Capture all logs/output around real file issuance/verification/replay and schema/I/O failures; JSON/toString of existing carriers and new parser/provider do not expose full token/payload/MAC/raw/hex/base64 key bytes or generated error canaries (G4). |
| `equalMaterialAcrossDistinctKidsDoesNotChangeRecordedKid` | Different kids with identical synthetic bytes accepted; token kid remains MAC-bound; changed kid without recomputing MAC fails; replay remains recorded K1 during overlap and fails after its removal, never aliases K2 (G2/G3/G5). |

### D. Extend existing configuration tests — exactly 4 new methods (9 → 13)

In `com.onecall.archtests.MediaPlaybackConfigurationTests` import the new configuration only in the new composed runners; retain existing runner/default tests unchanged.

| Method | Concrete assertions / alignment |
| --- | --- |
| `fileSourceRequiresExplicitSelectionAndPath` | No properties or path-only gives zero key providers and one unavailable authority; provider=file + valid temp file gives exactly one local provider and one authority; missing/blank/invalid path retains one unavailable provider without startup failure (G1/G2/G5). |
| `fileSourceSharesOneAuthorityAcrossProfiles` | Default/web/persistence and existing consumer combinations use exactly the same sole authority as issuer+verifier when opted in; no production reader or database auto-enable, no constructor changes (G5). |
| `fileSourceAndCustomProviderAreAmbiguousInEitherOrder` | File+custom and two custom beans, both registration orders, including @Primary/priority decoy: one authority starts, results Unavailable and neither provider's currentKeys is called; no arbitrary choice (G2/G5). |
| `unsupportedSourceAndInvalidFileKeepUnavailable` | Unselected/unknown/compound selector yields no local provider; invalid/missing/unreadable/malformed file with selector=file gives unavailable operations; default malformed opaque token cannot escape as Invalid instead of Unavailable. Preserve existing 503 mapper tests unchanged (G2/G5). |

### E. Extend architecture boundary — exactly 1 new method (6 → 7)

`com.onecall.archtests.architecture.SharingQueryBoundaryTests.keySourceUsesOnlyLocalReadAndExistingProviderBoundary`: exact three keysource production files; parser only JSON/encoding/collection/credential contract dependencies, adapter only bounded local READ/no-follow filesystem and parser, configuration only exact property opt-in/provider construction. Assert no writes, network/cloud SDK, environment/classpath key lookup, secret logging, scheduler/cache, public NamedInterface/DTO, DB/evidence/authorization dependencies or second authority. Assert redaction methods and schema closure/cardinality/size checks without broad source-string exemptions (G1/G2/G4/G5).

Modify the existing `sharingQueryApiAndProjectionAreExact` command-path listing by exactly the three `keysource/...` entries and change internal count 76 → 79. Preserve its existing command-source bans and the exact three-file credential listing/import restrictions. Preserve playback file list/import rules. No weakening of current module/named-interface checks to admit new plumbing into credential/playback.

### F. Existing tests to rerun, not rewire or recount as new

- All 24 `MediaPlaybackCredentialAuthorityTests`, including all three golden claims, `freshIssuanceUsesOnlyActiveKey`, `verifyOnlyOverlapVerifiesAndReplaysRecordedKey`, `disabledAbsentAndUnknownKeysFailClosed`, `zeroAndAmbiguousProvidersAreUnavailableEvenWithPrimary`, replay/shape/redaction cases (G3–G5).
- All existing `MediaPlaybackConfigurationTests`, `MediaPlaybackServiceTests`, `ShareAdmissionServiceTests`, `MediaGatewayWebTests` (especially `unavailableDependenciesAndDefaultByteProviderFailClosed` and log redaction), and `PublicShareWebTests` (G2–G5).
- All 42 `PublicShareAdmissionIT` methods, including real authority replay/overlap/recorded-key-removal and pre-existing concurrency tests; rerun them unchanged, do not add concurrency work. All 9 `MediaPlaybackIT`, 8 `MediaGatewayTransportIT`, role/ACL/migration checks and all other container tests (G3/G5).
- Entire architecture/module inventory and both existing jar startup/dependency-firewall CI smokes (G5). New local-secret tests use no cloud/network and do not claim to prove real Tencent custody.

## 9. Exact file allowlist and public impact

### This planning turn

Only **new** `docs/gateflow/pa22-sharing-media-production-key-custody-and-provider-integration-plan.md` may be written. No other plan/report artifact or source/test/config edit. The implementation allowlist below is future intent, not permission to edit now.

### Intended S1 implementation allowlist — 9 paths

| Action | Exact workspace-relative path | Reason |
| --- | --- | --- |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackKeyBundleParser.java` | Closed bundle parser (G1/G2/G4). |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/keysource/FileMediaPlaybackSigningKeyProvider.java` | Bounded read-only adapter behind existing contract (G1/G2/G4). |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackKeySourceConfiguration.java` | Explicitly conditional single provider bean, no authority (G1/G5). |
| New | `backend/src/test/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackKeyBundleParserTests.java` | Matrix A (G1/G2/G4). |
| New | `backend/src/test/java/com/onecall/core/sharing/internal/keysource/FileMediaPlaybackSigningKeyProviderTests.java` | Matrix B (G1–G5). |
| New | `backend/src/test/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackFileKeySourceIntegrationTests.java` | Matrix C (G1–G5). |
| Modified | `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` | Four explicit-composition cases (G1/G2/G5). |
| Modified | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | Exact file/count pins and one dedicated boundary method (G1/G2/G4/G5). |
| Modified | `.github/workflows/ci.yml` | Source=CI=XML inventory additions only; preserve jobs, toolchain, jar/firewall/contract checks (G5). |

No main-file modification, public surface, constructor or serialization-wire change. No migration/OpenAPI/POM/dependency/ACL/config-resource/deployment change. Architecture/schema prose sources remain unchanged: they already assign K20 custody and C10 roles; this plan is the precise seam/config operator contract and documentation decision. No README/config examples or extra implementation report files are authorized by this plan. If implementation discovers an additional required path/boundary, stop and request re-confirmation rather than silently expanding this list.

## 10. Measured inventories and exact CI maintenance (G5)

Read-only measurement at plan time parsed actual Java test source and reconstructed CI's Python inventory assignments/updates (not copied from an old plan). Both agree on the following current numbers. There are **no** local `backend/target/*reports/TEST-*.xml` reports, so current XML pass counts have not been observed; the current CI code requires them, and implementation must produce new reports before claiming source=CI=XML green.

| Inventory | Measured current | Exact target after S1 |
| --- | ---: | ---: |
| Surefire classes / methods | 30 / 231 | 33 / 266 |
| Failsafe classes / methods | 13 / 214 | 13 / 214 |
| Total classes / methods | 43 / 445 | 46 / 480 |
| Regex annotation-line scan (`@Test\b`, includes one frozen comment) | 446 | 481 |
| New unit methods | 0 | 35 = 12 + 10 + 8 + 4 + 1 |
| Existing configuration methods | 9 | 13 |
| Existing sharing architecture methods | 6 | 7 |
| Existing authority methods | 24 | 24 |
| Sharing internal regular source files | 76 | 79 |
| Private credential / new keysource source files | 3 / 0 | 3 / 3 |
| POM Failsafe includes | 13 | 13, exactly same IT classes |
| Direct dependencies / dependency-management imports | 11 / 1 | 11 / 1 unchanged |
| Normal-build plugins / profile plugins | 3 / 1 | 3 / 1 unchanged |

Current direct dependency artifactIds, in order: `spring-boot-starter-webmvc`, `spring-jdbc`, `jooq`, `HikariCP`, `spring-boot-starter`, `spring-modulith-starter-core`, `spring-boot-starter-test`, `testcontainers`, `postgresql`, `flyway-core`, `flyway-database-postgresql`. One managed import: `spring-modulith-bom`. Build plugins: `spring-boot-maven-plugin`, `maven-surefire-plugin`, `maven-enforcer-plugin`; profile plugin: `maven-failsafe-plugin`. No Tencent/cloud SDK. Preserve all scopes/enforcer bans and POM bytes, not merely versions. Project version remains `0.0.1-SNAPSHOT`.

Failsafe includes, all unchanged: `PostgresContainerIT`, `SharingQueryServiceIT`, `CustomerMeIT`, `CustomerSharesIT`, `SharingRoleLeastPrivilegeIT`, `CustomerShareCommandsIT`, `PublicSharesIT`, `PublicShareAdmissionIT`, `SharingAdmissionRoleLeastPrivilegeIT`, `EvidenceMediaReadRoleLeastPrivilegeIT`, `EvidenceMediaReadIT`, `MediaPlaybackIT`, `MediaGatewayTransportIT` (each `**/<class>.java`).

CI update must retain the existing exact_inventory report-pair and unsupported-test checks. Add a `pa22_additions` dictionary containing exactly sections A–E's five class/method sets, route all to `unit_expected`, and verify its method cardinalities `[12, 10, 8, 4, 1]`. Add it before the source-class equality scan. Update the early Surefire XML aggregate guard 231 → 266, final class counts 30 → 33 and aggregate/current totals to `[266,214] = 480`; update config count 9 → 13, add new architecture count 7, and keep authority count 24 and all IT counts unchanged. Update final exact_inventory unit argument to 266, container argument stays 214, and final success text accurately labels PA22.

Preserve historical assertions by including PA22 names in the subtraction sets used to reconstruct pre-PA18/PA19/PA20/PA21 baselines; otherwise merely adding new tests corrupts those checks. Historical method totals remain `[112,178]=290`, `[128,192]=320`, `[162,198]=360`, `[196,204]=400`; historical annotation scans remain 291/321/361/401. Add an explicit PA22-start assertion `[231,214]=445`, annotation 446, subtracting only PA22 additions. Current annotations become 481. Preserve the sole existing comment hit exactly: `PostgresContainerIT.java` containing `// evidence inside this existing method; no new @Test and no catalog weakening.` No new annotation-looking comments.

### Reproducible read-only source/CI/POM inventory scan

Run from workspace root. This is the scan used for measurement, generalized to include the future additions dictionary; it executes only inventory-building AST statements from the existing CI heredoc, not CI jobs or report-dependent code.

```bash
python3 - <<'PY'
import ast, re, textwrap, xml.etree.ElementTree as ET
from pathlib import Path
workflow = Path('.github/workflows/ci.yml').read_text()
block = textwrap.dedent(workflow.split("python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])
roots = {'expected', 'expected_query', 'unit_expected', 'container_expected',
         'pa19_additions', 'pa20_additions', 'pa21_additions', 'pa22_additions'}
environment = {}
for statement in ast.parse(block).body:
    selected = isinstance(statement, ast.Assign) and any(
        isinstance(target, ast.Name) and target.id in roots or
        isinstance(target, ast.Subscript) and isinstance(target.value, ast.Name)
        and target.value.id in roots for target in statement.targets)
    selected |= isinstance(statement, ast.Expr) and isinstance(statement.value, ast.Call) and isinstance(statement.value.func, ast.Attribute) and isinstance(statement.value.func.value, ast.Name) and statement.value.func.value.id in {'unit_expected', 'container_expected'}
    selected |= isinstance(statement, ast.For) and ast.unparse(statement.iter) in {
        'pa19_additions.items()', 'pa20_additions.items()',
        'pa21_additions.items()', 'pa22_additions.items()'}
    if selected:
        exec(compile(ast.Module(body=[statement], type_ignores=[]), '<ci-inventory>', 'exec'), environment)
root = Path('backend/src/test/java')
pattern = re.compile(r'@Test\s+(?:public\s+)?void\s+(\w+)\s*\(')
actual, annotation_lines, comment_hits = {}, 0, []
for path in root.rglob('*.java'):
    source = path.read_text()
    names = pattern.findall(source)
    if names:
        assert len(names) == len(set(names))
        actual[str(path.relative_to(root)).removesuffix('.java').replace('/', '.')] = set(names)
        assert not re.search(r'@(ParameterizedTest|RepeatedTest|TestFactory|Disabled|Enabled\w*|Disabled\w*)\b', source)
    annotation_lines += sum(bool(re.search(r'@Test\b', line)) for line in source.splitlines())
    comment_hits += [(path.name, line.strip()) for line in source.splitlines()
                     if '@Test' in line and line.strip().startswith('//')]
assert actual == environment['unit_expected'] | environment['container_expected']
for inventory in ('unit_expected', 'container_expected'):
    print(inventory, len(environment[inventory]), sum(map(len, environment[inventory].values())))
print('classes/methods/annotation-lines', len(actual), sum(map(len, actual.values())), annotation_lines)
print('comment hits', comment_hits)
namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
pom = ET.parse('backend/pom.xml').getroot()
queries = {
    'direct_dependencies': 'm:dependencies/m:dependency',
    'managed_dependencies': 'm:dependencyManagement/m:dependencies/m:dependency',
    'build_plugins': 'm:build/m:plugins/m:plugin',
    'profile_plugins': 'm:profiles/m:profile/m:build/m:plugins/m:plugin',
    'failsafe_includes': ".//m:plugin[m:artifactId='maven-failsafe-plugin']/m:configuration/m:includes/m:include",
}
for label, query in queries.items():
    entries = pom.findall(query, namespace)
    print(label, len(entries), [entry.findtext('m:artifactId', default=entry.text, namespaces=namespace) for entry in entries])
includes = pom.findall(queries['failsafe_includes'], namespace)
assert {entry.text for entry in includes} == {'**/' + name.rsplit('.', 1)[1] + '.java' for name in environment['container_expected']}
print('versions', [(entry.tag.rsplit('}', 1)[-1], entry.text) for entry in pom.iter()
                   if entry.tag.endswith('version') or entry.tag.endswith('release')])
print('local XML report files', len(list(Path('backend/target').glob('*reports/TEST-*.xml'))))
print('sharing internal files', sum(path.is_file() for path in Path('backend/src/main/java/com/onecall/core/sharing/internal').rglob('*')))
PY
```

Expected current/target output is exactly the table above; POM artifactIds/order/version nodes remain unchanged. This scan proves source=CI agreement, not XML execution. After full verification, run the actual CI heredoc below for exact source=CI=XML and all historical assertions.

## 11. Precise validation commands and reporting (G2–G5)

### Plan-author validation actually available

Read-only code/config/test/design/controller inspection; source/CI/POM scan; Git status/diff through the explicit gitdir; artifact-only diff/format/hash/line/byte checks. Initial inventory extraction needed one corrected rerun to include CI's `expected` and `expected_query` prerequisites; the corrected scan passes. No application tests have run and no XML green is claimed. `java -version` currently fails with WSL `UtilBindVsockAnyPort` socket error; the workspace has no existing target reports. Do not change Java/POM/toolchain, add a dependency or invoke a later gate to work around this environment limitation.

### Implementation validation, when that gate is authorized

Run in an environment with the pinned JDK/Maven and Docker daemon, without changing pins. These commands are requirements for that later gate, not commands executed by the planning agent:

```bash
cd backend
java -version
./mvnw -B -ntp -v
docker info
./mvnw -B -ntp -Dtest=MediaPlaybackKeyBundleParserTests,FileMediaPlaybackSigningKeyProviderTests,MediaPlaybackFileKeySourceIntegrationTests,MediaPlaybackConfigurationTests,MediaPlaybackCredentialAuthorityTests,SharingQueryBoundaryTests test
./mvnw -B -ntp clean test
./mvnw -B -ntp -Pcontainer-smoke clean verify
cd ..
```

Expected: pinned JDK 21.0.13 and Maven 3.9.9; focused 74 methods (12+10+8+13+24+7), full Surefire 33/266 and Failsafe 13/214, zero skips/errors/failures. The final clean verify prevents stale XML from satisfying exactness. Container database remains PostgreSQL 16 as existing tests specify. Then rerun section 10's source/POM scan and execute the exact checked-in inventory validator from workspace root:

```bash
python3 - <<'PY'
import os, textwrap
from pathlib import Path
workflow = Path('.github/workflows/ci.yml').read_text()
block = textwrap.dedent(workflow.split("python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])
os.chdir('backend')
exec(compile(block, '.github/workflows/ci.yml:container-inventory', 'exec'))
PY
git diff --check
git diff --exit-code -- backend/pom.xml backend/src/main/resources api/openapi deployment
git diff --name-only
git ls-files --others --exclude-standard
```

Use the explicit read-only gitdir/work-tree prefix noted in section 1 if the local Windows pointer still requires it. Assert only implementation allowlist paths changed, pre-existing untracked files remain untouched, and no generated secret fixture/report enters Git. Review the diff itself for accidental material without printing secret canaries into a report. Existing migrations/resources/OpenAPI/POM/deployment must have zero diff; the narrower allowlist also forbids unrelated source changes.

CI obligations remain: run the unchanged “default and opt-in web jar smokes”, “production jar dependency firewall”, and `contract-regressions` steps from `.github/workflows/ci.yml` in their existing pinned environments (Node 22.23.4). Exact existing contract commands are `npm ci && npm run check:s3` in `api/openapi`, then the checked-in A2/A3/A4/A5 validator commands/arguments. Do not hand-edit these commands, install cloud SDKs or loosen the dependency firewall. Report these as not-run until their real outputs exist; Maven verification alone does not establish jar-smoke/contract-regression success.

### Required implementation completion report (no extra report file authorized)

Report exact changed paths, behavior/config/schema decisions, tests and exact source/CI/XML/POM numbers, no-key 503/current-state/wire preservation, zero secret-bearing artifacts, unchanged migration/OpenAPI/POM/deployment surfaces, environment failures/not-run validation and residual owners. Do not claim Tencent integration, a deployment guarantee, Q22 approval or production readiness. Any goal that cannot be satisfied within the allowlist must be explicitly unresolved and returned for re-confirmation, not silently implemented as another slice.

## 12. Risks and residual ownership

| Risk / deferred concern | Owner / boundary |
| --- | --- |
| Real Tencent Secret Manager identity, SDK/API choice, access policy, provisioning, key version lifecycle and custody evidence | Later **Platform/Security** work unit; this adapter only consumes an external bundle. No network or cloud provisioning work here. |
| Read-only mount/ACL, supported regular-file layout, coherent replacement and stable kid→version/material assignment | Platform supplies and secures the explicit path. Symlink mounts are intentionally unsupported; a direct regular-file mount is required. Hostile path-race hardening/concurrency protocols need separate confirmation. |
| File-read latency/availability and temporary malformed replacement | Bounded synchronous read fails closed without cache. Platform measures later; no performance target or optimization work is implied. |
| Rotation overlap/retirement, credential maximum lifetime, skew and revocation window | Security + sharing contract owners; Q22 remains deferred, no values inferred from test clocks. |
| Process-memory secret residence and HMAC replay capability while old key remains available | Existing HMAC authority semantics; best-effort buffer clearing is not heap erasure or HSM isolation. Security owns future stronger custody/isolation requirements. |
| Cross-call same-kid material substitution or removing keys while credentials remain live | Platform/Security must preserve key-version identity/overlap. No historical registry is added; this seam rejects intra-bundle kid collisions and fails recorded-key lookup after removal only. |
| Existing no-key 503 might be mistaken for configured-service readiness | Operations must explicitly configure a valid source; provider availability is not evidence of byte-provider, network or deployment readiness. |
| Local WSL Java launcher failure and absent XML reports | Execution-environment owner; later implementation must run pinned validation in a working runner and report missing evidence, not change versions. |

No residual authorizes extra implementation, extra artifacts or a later gate. No confirmed design goal is left undecided by this plan; execution and production/cloud/deployment evidence remain deliberately unperformed/deferred.

## 13. Binding stop condition

**Stop at plan-author completion.** Write and validate only this plan artifact; report its path, line/byte count, SHA-256, exact task-created changed paths, validation performed/not run, key decisions, risks and unresolved goals. Do not edit source/tests/config/CI or any other artifact; do not commit, push, open/mutate a PR, approve, mark ready, request reviewers, post external comments, mutate issues, invoke plan review or enter any later gate. The pre-existing `.codex` remains untouched. This document's future implementation instructions are not permission for the planning agent to execute them.
