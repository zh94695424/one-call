# PA23 — Tencent Secret Manager K20 key-source plan

## 1. Status, authority and authoring limitation

- Task: `pa23-plan-fix1-gpt-20261110-01`; work unit: `PA23-sharing-media-secret-manager-key-source`; inspected on **2026-11-10**; gate: **fix only, after independent plan review**; provider/runtime: GPT / Codex CLI (this session; no independent model-version attestation). Prior authoring task: `pa23-secret-manager-key-source-plan-cont-gpt-20261110-01`.
- Binding goal remains exactly the complete active PA23 Goal Confirmation at `docs/gateflow/project-execution-control.md:15`. Design inputs remain `docs/12-technology-stack-and-system-architecture.md`, `docs/17-production-security-design-spec.md`, PA22's plan/final closeout and the current source/POM/CI. Direct code evidence in section 3 establishes goal alignment; no scope creep or changed product decision is introduced.
- **Authoring status: FIX-1 AUTHOR-COMPLETE — PENDING INDEPENDENT RE-REVIEW.** Review A finding `explicit-file-selection-can-be-suppressed-by-foreign-tencent-metadata`, accepted by the Controller, is a **fixed candidate**, not reviewer-accepted closure. Review B reported no material finding; that does not override the Controller's accepted finding. The bounded disposition is recorded in `docs/gateflow/pa23-sharing-media-secret-manager-key-source-plan-fix-1.md`. Controller-supplied official pinned artifacts and scratch manifests still resolve the former B1–B3 gaps. This is not plan acceptance, implementation permission, review dispatch or authorization for any later gate.
- Pin the single direct compile dependency `com.tencentcloudapi:tencentcloud-sdk-java-ssm:3.2.46`. Supplied Central metadata inspected locally records latest=release=**3.2.46**, lastUpdated=`20261025204818`; this is the supplied publication snapshot, not a fresh live registry query. Section 4 records pinned hashes/API; section 11 records the resolved graph.
- Actual local HEAD is `e2815576de43996e9550c84b2f015a9a84d6a86e`; do not confuse it with the PA22 merge commit recorded in Control. Native `.git` points to a Windows path. Read-only Git inspection uses `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call25 --work-tree="$PWD" ...`; do not rewrite metadata. At continuation entry this plan is already untracked; pre-existing private helper state remains untouched and excluded from task reporting/commits.
- Only this plan and `docs/gateflow/pa23-sharing-media-secret-manager-key-source-plan-fix-1.md` are authorized repository writes for this fix. No implementation, test/build/cloud execution or credential acquisition occurs in this fix. Controller scratch evidence is read-only, not a build run by this author. No source/test/POM/CI/Control/resource/README/review-artifact/temp-evidence edit, staging, commit, push, PR action, approval, ready/reviewer request, external comment, re-review entry or later-gate action is authorized.

### Resolved evidence register

All evidence is read-only. `E` denotes `C:\Users\Administrator\AppData\Local\Temp\pa23-sdk` (WSL `/mnt/c/Users/Administrator/AppData/Local/Temp/pa23-sdk`); `T` denotes its parent Temp directory.

| ID | Resolved evidence / decision | Source and disposition |
| --- | --- | --- |
| B1 — resolved | Publication, 3.2.46 SSM/common binary/source/POM and exact API/response binding | `E/metadata.xml`, `E/ssm.jar`, `E/common.jar`, `E/ssm-sources.jar`, `E/common-sources.jar`, `E/ssm.pom`, `E/common.pom`; exact SHA-256/size ledger and source members in section 4 |
| B2 — resolved with explicit limitation | Direct official CvmRoleCredential; no environment/profile/OIDC fallback. Credential refresh is synchronized; metadata has no exposed timeout | `E/common-sources.jar`, SHA-256 `87691D98A96B114F05DEDB24D0A6D8CACD9B753D3CF871EF10DCB5AFB6A5FFB3`; `provider/CvmRoleCredential.java`, `Credential.java`, provider sources. Platform/Security owns runtime validation; no custom metadata client |
| B3 — resolved with explicit limitation | Concrete profile, errors/redaction, bounded connect/read/write, one application invocation (not one socket attempt), no total callTimeout, exact +17 graph/jar delta and 116 probes | Pinned source ledger in section 4; five graph/index paths and hashes in section 11. No exclusions/management changes needed |

These close authoring placeholders without reopening the confirmed goal. Live cloud/CAM provisioning and runtime validation remain Platform/Security deployment obligations, not acceptance performed by this unit.

## 2. Goal, motivation, success and scope

Add one real Tencent Cloud Secret Manager read-only adapter for **K20 `MEDIA_PLAYBACK_SIGNING`**, using the official SSM Java SDK to retrieve **one explicitly configured version** and pass its SecretString into the **unchanged PA22 schema-v1 parser**. The existing provider result and authority remain the only signing/verification path. PA22 supplies file custody only; this work supplies the production read boundary without creating a secret-management platform.

| Goal | Success signal and design alignment |
| --- | --- |
| G1 — real SSM source | Explicit `tencent-ssm` selection, region/name/version and service-role credentials yield one SDK read per key snapshot and the same existing `Result.Available` as PA22 parsing |
| G2 — fail closed | Missing/invalid selectors and Tencent selection with conflicting `bundle-path` create no built-in provider; incomplete/invalid selected Tencent settings and authentication/CAM/resource/state/transport/timeout/throttling/response/parser failures return `Result.Unavailable`; no file, alternate version, default credential or last-good-key fallback. Unselected Tencent metadata does not invalidate explicit file selection |
| G3 — preserve authority | Real authority tests cover PLAYBACK/SEEK/RENEW, ACTIVE-only fresh issuance, VERIFY_ONLY overlap, exact replay, old-key removal and current-state enforcement without PA21/PA22 changes |
| G4 — redaction | No secret, credentials, token, key bytes, SDK response, error message, cause, request identifier or parser detail escapes through logs/output/errors/serialization/string forms |
| G5 — minimal composition/build | Explicit file selection survives; zero/one/many semantics and ambiguity-before-read survive; exact source/CI/XML and dependency-firewall inventories are updated without broad exemptions |

Non-goals: any real Secret Manager/KMS/CAM/COS resource provisioning or mutation; live cloud acceptance or network in automated tests; real credentials/key material; secret creation or rotation scheduler; registry/UI; cache/TTL; Q22 values; public API/OpenAPI/DTO/controller; DB/schema/migration/ACL; deployment manifests/topology; COS/upload/player/CDN/TLS/LB/WAF changes; merge/approval/ready/reviewer requests/external comments/issues. Do not introduce a second authority, generic cloud-secret SPI, resilience framework, background poller or multi-cloud abstraction.

Over-design rationale: two private production classes plus the existing selector are sufficient to separate the SDK boundary from bundle conversion. A tiny nested test seam is for deterministic SDK substitution only, not an exported provider platform. Per-call reads avoid cache invalidation and unauthorized TTL/rotation semantics. No audit writer, new database dependency or public diagnostic carrier is added; existing security audit design remains owned by Platform/Security, not silently implemented in this adapter.

## 3. Direct code evidence and preservation rules

Paths are workspace-relative; abbreviated paths in this table are under `backend/src/main/java/com/onecall/core/sharing/internal/`.

| Inspected evidence | Binding consequence |
| --- | --- |
| `credential/MediaPlaybackSigningKeyProvider.java:7` | `currentKeys()` returns the existing sealed `Available`/`Unavailable`; Entry clones arrays on construction/access and Available copies its list. Existing `@JsonValue` and constant string redaction remain unchanged. |
| `keysource/MediaPlaybackKeyBundleParser.java:26` | Bounded 65,536-byte strict UTF-8, no BOM, depth 8, duplicate detection and trailing-token rejection; exact root `{schemaVersion,keys}`, integer v1, closed entries `{kid,role,keyHex}`; kid `[A-Za-z0-9_-]{1,32}`, lowercase 64-hex/32 bytes; exactly one ACTIVE, sorted zero/many VERIFY_ONLY, whole-bundle duplicate rejection. **Reuse unchanged**, including its Unavailable behavior and material clearing. |
| `keysource/FileMediaPlaybackSigningKeyProvider.java:17` | Explicit absolute normalized path, bounded no-follow regular-file read, component symlink rejection, before/after identity checks, reread each call, no fallback and redaction. Leave this file unchanged. |
| `keysource/MediaPlaybackKeySourceConfiguration.java:10` | Currently one conditional file bean, reading only `bundle-path`; no authority or credential resolution. This is the sole existing production file requiring composition edits. |
| `playback/MediaPlaybackConfiguration.java:21` | Sole unconditional authority consumes `ObjectProvider<MediaPlaybackSigningKeyProvider>` and serves issuer/verifier. Do not edit this file or register another authority. |
| `credential/MediaPlaybackCredentialAuthority.java`, `snapshot`, `issueOrReplay`, `verify` | Enumerates at most two providers and requires exactly one **before calling currentKeys**; snapshot before token access; validates entries/duplicate kids; fresh issuance uses ACTIVE, replay selects recorded kid even when VERIFY_ONLY; missing anchor kid is Unavailable, not replacement issuance. Leave unchanged. |
| `credential/MediaPlaybackCredentialCodec.java` and existing authority/file integration tests | `ocmp1`, MAC/audience/purpose/kid, microsecond anchor and expiry/wire stay fixed. Missing old kid in an otherwise valid set means old verification Invalid and recorded replay Unavailable; source failure means both Unavailable. |
| `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` | Current 13 methods include explicit file/no selection, invalid path, profiles, custom-provider ambiguity and no startup failure for unavailable keys. Existing authority has 24 tests; preserve all. |
| `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java:466` | Current boundary pins exactly 3 keysource files, parser/file/config import sets, no network/cloud and 1 bean. Update only the selector's exact rules; retain parser/file no-network rules and add a separate SDK-only boundary. Existing credential/playback/module restrictions must not be weakened. |
| `backend/pom.xml`, `.github/workflows/ci.yml` | Boot **4.1.2**, Java 21/JDK **21.0.13+9**, Maven **3.9.9**, Modulith **2.1.2**, Testcontainers **2.0.5**, Enforcer **3.6.3** are actual pins. Architecture's Boot family recommendation is not permission to downgrade or change pins. Enforcer has dependency convergence; production jar uses exact coordinates and exact packaged filenames. |
| Security design §§4,6,8,11,12,18 | K20 is Secret Manager/HMAC-SHA-256/256-bit; role-based service identity, disabled/destroyed unavailable, no environment/image fallback; C10 bearer/public/current-state semantics and Q22 remain unchanged. |
| PA22 plan and final closeout | Parser/file behavior and 35 PA22 additions are accepted history, not authority for new cloud APIs or stale inventory claims. This plan freshly measured the current workspace. |

## 4. Inspected official SDK evidence and concrete binding

### Publication and artifact provenance

Use `com.tencentcloudapi:tencentcloud-sdk-java-ssm:3.2.46`, compile scope; its common dependency resolves to **3.2.46**. No floating version, aggregate SDK, COS/KMS/STS service artifact, direct OkHttp dependency, exclusion, dependency-management edit, plugin or repository is needed. The controller-supplied official Central snapshot was inspected read-only on 2026-11-10; local SHA-256 checks match every supplied artifact. No fresh network retrieval or application/build execution is claimed.

| Artifact under `E` | Bytes | SHA-256 |
| --- | ---: | --- |
| `metadata.xml` | Supplied metadata | `DFD56B8BE1E2DD57D9EFEA1EB301219A07DDF18B6F61545737D010881783884B` |
| `ssm.jar` | 82,852 | `93366E9E22772C00B92C7852DEE75FF3E8E9E766485EF8FD562DE3D048E1E14E` |
| `common.jar` | 69,682 | `E1730933E70C107FFEB83BB7DBD077D1DD4A340DDF3E63FF28C7C6F69B8F55E5` |
| `ssm.pom` | 4,146 | `3DF3CFC6918F0C9C01C7EFA909CEB3AD6800A3FF00AE835B8F1F019C7736C73A` |
| `common.pom` | 4,762 | `AF1B43715C67D2B1D6B59DEF8B780BD1686F01305FF33BD77FDEBE6BDE98D7B0` |
| `ssm-sources.jar` | 102,582 | `E2EC0758B918233795B149016C20FEF624DE868CC55C56134646BDDDB55C9C2A` |
| `common-sources.jar` | 42,922 | `87691D98A96B114F05DEDB24D0A6D8CACD9B753D3CF871EF10DCB5AFB6A5FFB3` |

### Exact classes, imports and request/response boundary

Only `TencentSsmSecretStringClient.java` may import Tencent SDK classes in production. Its exact SDK import allowlist is:

```java
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.provider.CvmRoleCredential;
import com.tencentcloudapi.ssm.v20190923.SsmClient;
import com.tencentcloudapi.ssm.v20190923.models.GetSecretValueRequest;
import com.tencentcloudapi.ssm.v20190923.models.GetSecretValueResponse;
```

Source members are these fully qualified paths with dots replaced by slashes and `.java`, plus common `Credential`, `AbstractClient`, provider and HTTP transport sources. Constructor parameter base `com.tencentcloudapi.common.Credential` need not be imported/constructed directly: CvmRoleCredential extends it. No direct OkHttp/Okio imports.

| Inspected API / behavior | Exact implementation decision |
| --- | --- |
| `SsmClient(Credential, String)` and `SsmClient(Credential, String, ClientProfile)` | Use the three-argument constructor, direct `new CvmRoleCredential()`, validated region and explicit profile, lazily inside the client adapter only |
| `GetSecretValue(GetSecretValueRequest)` returns `GetSecretValueResponse`, throws `TencentCloudSDKException` | One application invocation per valid read; action `GetSecretValue`, endpoint `ssm.tencentcloudapi.com`, API version `2019-09-23`; no list/describe/mutation/explicit retry |
| `new GetSecretValueRequest()`; `setSecretName(String)`, `setVersionId(String)` | Set only exact configured name/version. `setEncryptionPublicKey(String)` and `setEncryptionAlgorithm(String)` exist but must not be called; both fields remain null. No moving-stage/latest/current default |
| `getSecretName()`, `getVersionId()`, `getSecretBinary()`, `getSecretString()`, `getRequestId()` all return String | Non-null response; require exact non-null name/version equality, nonblank SecretString, null/empty SecretBinary. Reject binary-only and both-nonempty. API docs say at most one payload field is nonempty. Never decode payloads, return raw response or log/attach RequestId |
| `TencentCloudSDKException` constructors `(String)`, `(String, Throwable)`, `(String, String requestId)`, `(String, String requestId, String errorCode)` | Tests synthesize checked failures with message/cause/code/ID canaries; production maps all codes identically, no lifecycle getter or status field invented |
| `Credential.getSnapshot()` synchronizes refresh and wraps refresh failures in RuntimeException | Catch **TencentCloudSDKException and RuntimeException** at the adapter boundary, including lazy factory/refresh failure. Return only existing empty Unavailable through private seam/provider, with no message/cause/requestId; do not catch JVM Error |

### Concrete lazy production factory and credential route

The package-private nested client factory returns actual SsmClient; tests inject a factory returning a mocked actual SsmClient. Construct lazily only inside the adapter on first valid read, never bean registration or authority ambiguity checks. Serialize lazy publication; failed construction may recover on a later call, with no poisoned client/key cache. Profile tests capture constructor arguments using mocks, not credential access or HTTP construction.

Required production sequence (design contract, not implemented here):

1. `new HttpProfile()`; `setReqMethod(HttpProfile.REQ_POST)`; `setProtocol(HttpProfile.REQ_HTTPS)` (literal `https://`); `setEndpoint("ssm.tencentcloudapi.com")`.
2. `setConnTimeout(validatedConnectSeconds)`; `setReadTimeout(validatedReadSeconds)`; `setWriteTimeout(validatedConnectSeconds)`. Whole seconds; defaults **2/3/2 seconds**. No new write-timeout operator property.
3. `new ClientProfile(ClientProfile.SIGN_TC3_256, httpProfile)` (TC3-HMAC-SHA256); `setDebug(false)`; `setUnsignedPayload(false)`. Leave backup endpoint and language null. No proxy, custom SSL/trust manager/hostname verifier, root-domain/API-gateway override, unsigned payload, custom HTTP client or alternative endpoint. Preserve normal HTTPS certificate/hostname validation.
4. `new SsmClient(new CvmRoleCredential(), region, clientProfile)`. No synthetic/static Credential constructors, SecretId/SecretKey/token fields, default credentials provider or environment/profile/OIDC import in production key-source code.

DefaultCredentialsProvider is **forbidden**: inspected order is **environment → profile → CVM role → OIDC**. EnvironmentVariableCredentialsProvider reads `TENCENTCLOUD_SECRET_ID/KEY`; ProfileCredentialsProvider reads `~/.tencentcloud/credentials` or `/etc/tencentcloud/credentials`. No-arg OIDCRoleArnProvider reads TKE_* environment/token-file state and its STS call lacks an exposed bounded profile; it is not used. Direct CvmRoleCredential is official role-only Credential, with refresh attached to Credential; no custom metadata/token client. Its metadata endpoint is `http://metadata.tencentyun.com/latest/meta-data/cam/security-credentials/`, separate from SSM HTTPS. It exposes **no metadata connect/read timeout or callTimeout**. CvmRoleCredential and AbstractClient are not AutoCloseable; do not invent close/shutdown methods.

### Timeout, retry and logging limitations

HttpProfile defaults: connect=60, read=0, write=0, POST, HTTPS. OkHttp zero means no timeout; override all three as above. Individual connect/read/write are bounded, but **SSM exposes no total callTimeout/DNS deadline and CVM metadata acquisition exposes no timeout**. No total end-to-end deadline is promised. Platform/Security owns deployment/runtime validation of this exact unavoidable SDK limitation; no custom metadata/HTTP client or executor deadline is added.

Generated GetSecretValue makes one SDK action call and does not automatically call AbstractClient.retry; application code must not call it. The shared underlying OkHttpClient retains default retryOnConnectionFailure semantics; HttpProfile exposes no retry toggle. Say **one application GetSecretValue invocation**, never exactly one socket/network attempt or disabled transport retry. Do not directly import/configure OkHttp or add it directly to circumvent this boundary.

AbstractClient installs TCLog as an OkHttp interceptor, but it emits nothing while debug=false. Keep debug false and no logger/request/body/tracing hook; discard exception/response detail. Automated tests only inject/mock actual SDK clients; none contacts CVM metadata, STS or SSM or exercises production refresh.

## 5. Runtime configuration contract (project decisions, not SDK API claims)

Private property prefix `P = onecall.sharing.media-playback.signing-key-source`. No application resource defaults, YAML samples with material, credential fields, actuator exposure or deployment changes. Read only metadata through Spring Environment; normal operator binding of metadata is not secret-value/credential fallback. Never call `System.getenv`, read system credentials, scan home/classpath or accept an inline bundle.

| Exact property | Required / default / validation |
| --- | --- |
| `P.provider` | Optional, no default. Supported literals **exactly** `file` or `tencent-ssm`; case-sensitive, no trimming. Missing/blank/unknown/compound/whitespace/case-variant/unresolved values create no built-in provider. Do not auto-select from other fields or active profile. |
| `P.bundle-path` | Existing file-only metadata; retains PA22 path validation. With `provider=tencent-ssm`, any key presence, even empty, blank, invalid or an unresolved placeholder, suppresses Tencent; no file bean is constructed or read. Presence is Spring Environment key presence (`containsProperty`), not a nonempty/resolved-value check. |
| `P.tencent.region` | Required for selected Tencent adapter; nonempty, no surrounding whitespace, ASCII `[a-z0-9]+(?:-[a-z0-9]+)+`, at most 64 characters. No production region default, inference or region probing. Invalid/missing becomes one configured unavailable provider without network. Region existence is provider/Platform validation, not invented here. |
| `P.tencent.secret-name` | Required; opaque nonempty string, maximum 1,024 UTF-16 code units, no surrounding whitespace/control characters/unpaired surrogates. Send exactly configured value, no trim/URL decoding/ARN synthesis. Application resource bound is not a claimed Tencent name limit. |
| `P.tencent.version-id` | Required; opaque nonempty string, maximum 256 UTF-16 code units, same whitespace/control/surrogate rejection. Exact version identifier only, never a stage/latest/current default. Send via `setVersionId(String)` unchanged; do not guess valid Tencent production values. |
| `P.tencent.connect-timeout-seconds` | Optional: default `2`, integer ASCII `1`–`5`, no sign/units/whitespace/zero/infinite. Whole seconds map to `setConnTimeout` and `setWriteTimeout`. |
| `P.tencent.read-timeout-seconds` | Optional: default `3`, integer ASCII `1`–`10`, same strict parsing. Whole seconds map to `setReadTimeout`; no total callTimeout exposed. These are individual transport bounds, not Q22 credential lifetime. |

`P` is notation only: the literal property prefix must be expanded in code/tests/docs. No endpoint/proxy/debug/retry/credential/cache/TTL property is exposed in this slice. Endpoint policy is explicit HTTPS `ssm.tencentcloudapi.com`, validated region passed to SsmClient, platform-owned DNS/network validation, no arbitrary URL/HTTP/insecure trust/proxy override.

Selection and non-interference are resolved **before any SDK construction**, asymmetrically:

- Exact `provider=file` selects only the existing file provider. All unselected `tencent.*` metadata is irrelevant, specifically `tencent.region`, `tencent.secret-name`, `tencent.version-id`, `tencent.connect-timeout-seconds` and `tencent.read-timeout-seconds`. Do not look up, resolve placeholders in, parse, validate or test presence of those properties on the file branch. Absent, empty, whitespace, valid, invalid and unresolved values, individually or combined, cannot suppress the file provider or trigger its construction/read; only explicit file selection and the unchanged `bundle-path` behavior govern it. Create exactly one file provider even for a missing/invalid path, which remains unavailable on read as in PA22. Construct no Tencent provider, Tencent client adapter, SDK client, role credential or metadata client, and perform no Tencent/metadata read.
- Exact `provider=tencent-ssm` with **any `bundle-path` key presence** suppresses the Tencent provider entirely, before resolving the path value or Tencent settings. Create zero built-in providers and construct/read no file provider: this is never an implicit file fallback. A path-presence inspection failure also suppresses Tencent, without file construction/read. Without `bundle-path`, missing/invalid Tencent-required metadata or timeout values create one permanently unavailable Tencent provider for that configuration, not a startup exception; zero network/client construction on calls. Unresolved selected Tencent metadata is unavailable metadata, not permission to fall back.
- Missing/blank/unknown/invalid/compound/whitespace/case-variant/unresolved selectors, or selector-resolution errors, create neither built-in provider regardless of other metadata. Unselected properties never auto-select a source. Zero/one/many provider and custom-provider ambiguity-before-read semantics remain unchanged. Do not include offending property values in any exception.

Do not expose `secret-id`, `secret-key`, `token` or credential-file properties at all. Direct CvmRoleCredential is the only production route; default environment/profile/OIDC chains are forbidden even though these properties are not exposed.

## 6. Exact desired read lifecycle and data handling

This lifecycle binds directly to the inspected API and limitations in section 4.

1. Validate captured immutable metadata/timeout settings before construction or read. Invalid settings return `new Result.Unavailable()`; do not consult file or any other source.
2. Use a lazily initialized production SDK client inside the small client adapter, never while registering beans. A failed construction is unavailable for that call; do not persist a last-good key snapshot or permanently cache a transient credential failure. Any reusable SDK client/temporary-credential state is distinct from signing-key state. Serialize lazy publication; Credential snapshot refresh is synchronized. AbstractClient/CvmRoleCredential are not AutoCloseable; no close API is invented.
3. Every valid `currentKeys()` invokes the secret-value action **once** with the exact configured name/version. No application retry, alternative version/stage, list/describe call, extra state-discovery call, asynchronous task or background poll. Do not call AbstractClient.retry; underlying default connection retries may still occur. One application invocation does not guarantee one socket attempt.
4. Require non-null successful SDK response; require getSecretName()/getVersionId() to exactly equal the requested non-null values. A null/mismatched required identity is malformed, not a signal to search elsewhere. Never print request/response/request ID.
5. Only SecretString is a valid payload. Null, empty and whitespace-only are unavailable. **Do not base64-decode SecretString**, guess encodings or use binary as a substitute. If the response carries nonempty binary material, reject it even alongside a string; getSecretBinary() is a String and only null/empty represents absence; getSecretString() is also a String.
6. Before allocating UTF-8 bytes reject SecretString character length above 65,536. Encode with a reporting UTF-8 encoder (`CodingErrorAction.REPORT` for malformed/unmappable UTF-16); reject unpaired surrogates instead of silently replacing them. Reject encoded size above 65,536. Do not trim, unescape, normalize, remove BOM or reserialize JSON. Pass the exact UTF-8 content to the existing `MediaPlaybackKeyBundleParser.parse(byte[])` unchanged. Parser enforces all schema/encoding/key rules and returns the existing result.
7. Clear owned byte buffers in `finally`; never clear arrays owned by existing Entries or caller snapshots. Do not store SecretString, response, JsonNode, bytes or parsed Available in adapter fields. Existing immutable/defensive result semantics remain unchanged. Managed SDK/Java Strings cannot be reliably wiped; do not promise heap erasure or HSM isolation.
8. Every failure returns the same empty Unavailable. A subsequent valid call may recover; valid→failure must not serve old keys. An already-running authority operation uses its one snapshot; this does not promise retroactive key recall.

**Timeout boundary:** connect/read/write are nonzero, default 2/3/2 seconds, but SSM exposes no total callTimeout/DNS deadline and CVM metadata exposes no connect/read/call timeout. Connection retries/body progression can extend elapsed duration. No total-deadline claim, custom metadata/HTTP client, uncancellable executor deadline, cache, TTL, backoff or resilience framework. Platform/Security owns deployment/runtime validation of this exact limitation.

**Rotation with an explicit version:** K20 bundle keys can represent ACTIVE/VERIFY_ONLY overlap, but this adapter never follows a moving cloud version. Platform/Security provisions a new immutable secret version and changes the explicit configured version through an approved rollout/restart. Tests model that switch using fresh configured clients/providers, not by pretending cloud versions mutate in place. Same-version response sequencing is allowed only as a no-cache/failure test double, not a claimed production rotation protocol. No config watcher or version scheduler is introduced.

## 7. Failures, status and redaction

All rows below yield **only `MediaPlaybackSigningKeyProvider.Result.Unavailable`**, no partial set, fallback, detail code or exception carrier. Existing authority and gateway map dependency failure as before (including existing 503 behavior); no public status/code is added.

| Boundary failure | Required mapping and deterministic test |
| --- | --- |
| Missing/conflicting/unresolved config | Missing/invalid/unknown/compound/unresolved selector: no built-in provider. Tencent selection with any `bundle-path` presence: no built-in provider or file read. Otherwise incomplete/invalid selected Tencent metadata: one unavailable Tencent provider before client creation. Explicit file selection ignores all unselected Tencent metadata and retains PA22 path behavior; no fallback |
| Credential acquisition/refresh, authentication/signature/expired role credentials | Catch actual SDK/client failure at adapter boundary; no credential-source substitution; inject factory/credential failure without metadata calls |
| CAM permission denial | Unavailable; do not disclose denied resource/action/account or SDK error details; no file read |
| Secret/version not found, disabled, pending destruction/destroyed, inaccessible/deleted | Unavailable; no prior version/stage or state-recovery call. Map every SDK exception identically regardless of vendor code; do not invent status getters or assert the value response includes lifecycle state |
| Throttle, internal/server error, non-success status, unknown vendor code | Unavailable with no application/explicit SDK retry or stale key; underlying connection retry semantics remain as section 4; every unrecognized exception/code remains closed |
| TLS/certificate/hostname failure, DNS/network/connect/reset/proxy error | Unavailable; no trust relaxation, alternate endpoint or logging of endpoint/headers; fake failures only |
| Connect/read/write timeout or SDK/metadata failure surfaced by SDK | Unavailable on TencentCloudSDKException or RuntimeException; no exposed total/metadata timeout claimed; no test sleeps or network |
| Null/empty/mismatched response, missing string, binary response, oversized/invalid Unicode | Unavailable before parser where appropriate; only accepted string reaches parser |
| PA22 parser rejects syntax/schema/BOM/depth/duplicate fields/kids/roles/key bytes | Pass through Unavailable without parser detail, cause, offending kid or raw material |
| More than one provider bean | Existing authority returns Unavailable **before calling either provider**, including both orders and primary decoys; do not implement a priority winner |
| Unexpected runtime exception | Catch at provider/client boundary; Unavailable, no exception attachment. Do not catch fatal JVM `Error` as an availability result |

No logger, `System.out/err`, SDK debug output, request/body interceptor logging, tracing payload capture or exception formatting is allowed in these classes. Pinned TCLog is silent while debug=false; retain profile assertions and synthetic SDK exception/output canaries, not merely our own `toString` checks. New stateful carriers implement constant `@JsonValue`/`toString` redaction, no public getters/records exposing config/client/response, no `Serializable`. Do not serialize raw SDK models to test redaction: never expose them from the seam. Existing result/Entry serialization remains redacted.

Never log, attach, return or use as diagnostic labels: SecretString/binary, SecretId/SecretKey/session token/Authorization/signature, secret name/version/endpoint credential or request identifier, key bytes/hex/base64, playback token/payload/MAC, raw response, parser error or exception message/cause/suppressed exception. Even an SDK request ID is withheld here; no sensitivity judgement at the catch site. Synthetic redaction canaries must not be rendered in failing assertion messages or test output.

## 8. Spring composition and private seams

- Extend only `MediaPlaybackKeySourceConfiguration`; keep the two bean factory return types `MediaPlaybackSigningKeyProvider`, with no extra authority or global SDK credential bean. Production classes are private module internals, not `@NamedInterface` exports.
- Replace permissive conditional matching with nested exact Spring `Condition` checks for the literal selections/conflict rules in section 5. No new source file is needed for conditions/settings. Use `@Conditional` on the two factories; no `@Primary`, `@Order`, `@ConditionalOnMissingBean`, profile shortcut or default bean. Existing PA22 architecture assertions for the old selector annotation must be adjusted narrowly, not removed wholesale.
- Exact `file` retains the original file provider/path behavior regardless of stale `tencent.*` properties; the file branch never inspects them or constructs a Tencent provider/client/SDK/role credential/metadata client. Exact `tencent-ssm` without `bundle-path` creates only the Tencent adapter (unavailable for invalid selected settings); any `bundle-path` presence suppresses Tencent and creates zero built-in providers, with no file construction/read. Unselected properties do not activate either provider or cause file reads. Tencent failure never instantiates or calls the file provider.
- Zero beans: the existing unconditional authority starts and remains unavailable. One provider: use exactly it; preserve manually supplied custom-provider behavior when neither built-in is selected. Multiple beans: preserve existing cardinality-before-read; no currentKeys invocation, SDK construction or metadata acquisition from either contender. Config selection is not a new global registry that overrides custom providers.
- New `TencentSsmMediaPlaybackSigningKeyProvider`: owns metadata, the existing parser and a narrow nested-reader reference; implements only existing currentKeys. New `TencentSsmSecretStringClient`: the only SDK-dependent production class, owns actual SDK request/response validation and service-role client construction, returns only an ephemeral SecretString to the provider. Nested settings/reader/client-factory seams are package-private and have no public operator endpoint.
- Tests instantiate the provider with a same-package fake reader, and instantiate the real client adapter with a same-package fake SDK-client factory returning a mocked **actual pinned SsmClient**. No test selects the production metadata chain or constructs a real HTTP client. The nested client factory returns actual SsmClient; constructor mocks capture profile/region/direct role credentials without HTTP construction or credential refresh. Composition tests mock construction of the new small adapter where needed rather than adding a public production test flag or a new Spring override SPI.

## 9. One behaviorally verifiable slice and exact planned tests

**S1 — explicit Tencent version to PA22 authority, closed failures and file coexistence.** One composed behavior, not layer/file slices: metadata and role-only SDK request → strict response/string conversion → unchanged parser/provider → real authority issuance/verification/replay/current-state tests → exact build/firewall inventory. No other slice is authorized. Exit requires independent plan acceptance and separate implementation authorization first, then all planned/new/existing tests and jar/contract checks green, zero skips/errors/failures, no prohibited surface drift. These are future implementation obligations, not current test results.

Use plain named `@Test void` methods and loops, no parameterized/dynamic/repeated/disabled/conditional tests. All keys/strings/credentials are synthetic in memory (temporary files only for file regression); no committed secret fixtures. Compare secret equality as booleans or constant-time byte comparisons with generic assertion labels, never actual/expected secret renderings.

### A. New `TencentSsmMediaPlaybackSigningKeyProviderTests` — 18 methods

| Exact method | Required assertion matrix |
| --- | --- |
| `validSecretStringMapsExistingBundle` | Real unchanged parser yields exact synthetic ACTIVE entry and immutable result |
| `zeroAndManyVerifyOnlyKeysRemainCanonical` | Zero/one/many, sorted deterministic kid order, no partial filtering |
| `missingRequiredMetadataNeverReadsClient` | Missing/null/blank region/name/version, unresolved metadata; no factory/read invocation |
| `invalidMetadataAndTimeoutsNeverReadClient` | Config bounds/case/whitespace/control/surrogates/invalid integer settings fail closed |
| `eachCurrentKeysReadsExactlyOnce` | One fake invocation per valid call, no startup read or duplicate call |
| `successThenFailureNeverUsesLastGoodKeys` | Success→throw/null/malformed→success has no retained key/fallback |
| `nullEmptyAndWhitespaceStringsAreUnavailable` | All empty forms closed; no normalization into valid JSON |
| `secretStringIsNeverBase64Decoded` | Base64/base64url of valid JSON rejected as JSON, no binary substitute |
| `utf8EncodingRejectsUnpairedSurrogates` | Reporting encoder; malformed UTF-16 never silently replaced |
| `sourceByteAndCharacterBoundsAreEnforced` | Exact byte bound accepts legal bundle, 65,537 fails; multibyte/character prebound tested |
| `parserSyntaxAndSchemaRejectionsStayUnavailable` | Duplicate fields, trailing tokens, wrong v1/root/fields/types/BOM/depth |
| `duplicateKidsAndInvalidRolesStayUnavailable` | ACTIVE/VERIFY_ONLY collision, identical duplicates, DISABLED/DESTROYED/unknown roles |
| `invalidKeyMaterialStaysUnavailable` | Invalid kid/hex/case/length, zero/multiple ACTIVE; unchanged PA22 rules |
| `readerFailuresAreUnavailableWithoutCauses` | Checked/client/runtime failures through nested seam return only empty result |
| `noFileOrAlternativeSourceFallbackExists` | Valid decoy file/reader never touched during Tencent failure; source guard evidence |
| `snapshotsRemainIndependentAndDefensivelyCopied` | Repeated reads and accessor mutations cannot alter earlier snapshots |
| `ownedUtf8BuffersAreCleared` | Owned temporary buffers cleared on parse success/failure without changing Entry copies |
| `providerAndResultsRedactMaterialAndMetadata` | Constant JSON/string forms and captured stdout/stderr/logs exclude synthetic canaries |

### B. New `TencentSsmSecretStringClientTests` — 8 methods

| Exact method | Required assertion matrix against inspected 3.2.46 SDK |
| --- | --- |
| `explicitNameAndVersionReachOneSdkRead` | Mock actual SsmClient.GetSecretValue; exact name/version, encryption fields null, one application invocation, no describe/list/mutation |
| `roleOnlyFactoryIsLazyAndHasNoStaticFallback` | Constructor mocks capture direct CvmRoleCredential and actual SsmClient; no construction before valid read or credential getter/refresh. Source assertions forbid default/static/env/profile/OIDC routes; no metadata calls |
| `clientRegionHttpsTimeoutAndRetrySettingsAreExact` | Capture HttpProfile/ClientProfile and region: POST/HTTPS endpoint, TC3, debug/unsigned false, no overrides, connect/read/write defaults 2/3/2 and write follows validated connect. Assert no AbstractClient.retry call; document default connection retries and absent total/metadata timeout, no fictional toggle; no network |
| `nullMissingBinaryAndMismatchedResponsesFailClosed` | Actual String getters: null response, missing/mismatched name/version, null/empty/blank SecretString, nonempty SecretBinary alone or with string reject; null/empty binary with valid string accepts; no RequestId escapes |
| `authCamAndResourceStateFailuresAreUnavailable` | Synthetic TencentCloudSDKException(message, requestId, errorCode) for auth/CAM/not-found/disabled/destroyed categories; no lifecycle getter or code branch. Include factory/RuntimeException-wrapped refresh failures |
| `throttleUnknownAndServerFailuresAreUnavailable` | Actual checked/runtime errors and synthetic vendor-code canaries; no application/explicit SDK retry, fallback or response leakage; no single-socket assertion |
| `networkTlsAndTimeoutFailuresAreUnavailable` | Fake SDK failure causes for DNS/connect/reset/TLS/timeout; no trust weakening, sleeps or HTTP server |
| `clientFailuresAndStringFormsNeverLeak` | Synthetic SDK message/cause/request-ID/header/response/config canaries absent from outputs and outward results |

### C. New `MediaPlaybackTencentKeySourceIntegrationTests` — 10 methods

Use the real parser, real Tencent provider and real client adapter with mocked actual SsmClient, the existing real authority and fixed synthetic clocks. New provider configurations, rather than mutable real cloud versions, represent version rollouts. All purposes loop over PLAYBACK, SEEK and RENEW where meaningful.

| Exact method | Required behavior |
| --- | --- |
| `tencentBundleIssuesActiveForEveryPurpose` | Full SDK-response-to-authority path, exact claims/anchor, ACTIVE-only fresh issuance |
| `explicitVersionRolloutVerifiesOldAndNewKeys` | Version A then B with new ACTIVE+old VERIFY_ONLY; old/new credentials verify |
| `overlapReplaysExactCredentialForEveryPurpose` | Same recorded anchor/expiry yields byte-identical original credential after switch and clock advance |
| `removedRecordedKidNeverMintsReplacement` | Valid new bundle without old key: old verify Invalid, replay Unavailable, fresh new ACTIVE succeeds |
| `unavailableSourcePrecedesOpaqueTokenAccess` | Source failures before mocked token.value; neither issuance nor replay succeeds |
| `unavailableSourcePreventsStateEvidenceAndOpen` | Real authority/service composition stops before all downstream reads on every source failure category |
| `tencentClaimsStillRequireCurrentState` | Revoked/expired/stale-generation/session/member/risk/object/window denials unchanged, no controlled open |
| `equalMaterialNeverAliasesRecordedKid` | Distinct kids/equal synthetic bytes accepted, signed kid remains bound, no replay substitution |
| `transportAndParserFailuresHaveNoFallback` | SDK rejection and invalid SecretString through real authority, then later recovery; file decoy unused |
| `sdkToAuthorityFlowNeverLeaksCanaries` | Success/issue/verify/replay/failure JSON/string/log/output redaction through the composed path |

### D. Existing configuration tests — 6 additions (13 → 19)

Exactly six additions remain; use loops within these methods, not extra methods:

| Exact method | Required configuration assertions |
| --- | --- |
| `tencentSourceRequiresExactExplicitSelection` | Absent/blank/unknown/invalid/compound/whitespace/case-variant/unresolved selectors and metadata alone create zero built-in providers. Exact Tencent selection with any `bundle-path` presence (valid, empty, whitespace, invalid or unresolved placeholder) creates zero built-in providers, zero Tencent/client/SDK/credential/metadata construction and zero file construction/read; path presence is checked before value resolution. Valid Tencent selection without a path creates only Tencent, without eager SDK/metadata initialization. |
| `tencentMissingMetadataIsUnavailableWithoutNetwork` | Selected Tencent without a path and with each missing/invalid required field, timeout or unresolved placeholder retains exactly one unavailable provider, without startup failure, client/SDK/credential/metadata construction, read or file fallback. |
| `staleTencentMetadataDoesNotSuppressExplicitFileProvider` | Exact file selection plus a valid synthetic file path and each stale `tencent.region`, `tencent.secret-name`, `tencent.version-id`, `tencent.connect-timeout-seconds` and `tencent.read-timeout-seconds` form, individually and combined (absent baseline, empty, whitespace, valid, invalid and unresolved placeholders), yields exactly one provider of the existing file type and a working real-authority issue/verify path. Assert no lookup/placeholder resolution/validation of unselected Tencent fields, zero Tencent provider/client-adapter/SDK-client/role-credential/metadata-client construction and zero Tencent/metadata reads, at registration and after file authority calls. Only explicit file authority use reads the file, never stale metadata. |
| `tencentSharesSoleAuthorityAcrossProfiles` | Default/web/persistence/combined profiles retain one authority shared by issuer/verifier and enabled consumers, without eager SDK/metadata initialization or profile-based source selection. |
| `tencentAndCustomProvidersAreAmbiguousBeforeAnyRead` | Retain two custom providers, Tencent+custom, explicit file+custom (also with stale Tencent metadata), and manually registered Tencent+file; test both registration orders and configuration/provider registration orders, with `@Primary` and `Ordered` decoys. Each pair remains two providers and one unavailable authority before either `currentKeys`, file read, SDK construction or metadata acquisition; do not suppress a selected built-in to favor custom providers. |
| `explicitFileSelectionRemainsIndependentOfTencentClient` | Retain PA22 explicit file/path behavior, including missing/invalid/unresolved paths: exactly one file provider, unavailable on invalid path, unaffected by stale Tencent fields and without any Tencent/client/SDK/credential/metadata construction. Preserve successful file issue/verify and all existing file/profile/custom-provider expectations. |

Retain all existing 13 methods and file tests without changing expectations for previously supported configurations. The stale-metadata method replaces only the former overbroad conflict test; Tencent-plus-path suppression remains covered inside `tencentSourceRequiresExactExplicitSelection`. Neither this rename nor the assertion matrices add methods or weaken missing/unknown/invalid selector failures or multi-provider ambiguity.

### E. Architecture tests — 1 addition (7 → 8)

Exact new method `tencentKeySourceConfinesSdkAndPreservesExistingAuthority`: SDK imports only in the new client adapter; exact two new production paths and the exact SDK import allowlist in section 4; only secret-value read, no write/list/describe APIs, static credentials, direct metadata HTTP, logger, source fallback, scheduler/cache/TTL, DB/public interface or second authority. Retain exact parser/file rules and credential/playback restrictions. Update the old local boundary's exact directory list from 3 to 5 and split its selector checks narrowly to admit only the new exact conditions/two factories. Internal count 79→81; unchanged credential file list remains 3.

Total proposed additions **43 = 18 + 8 + 10 + 6 + 1**. SDK-dependent assertions may not be replaced with fictional API calls or weakened to make this count fit. If inspected evidence needs a different decomposition/count, revise this plan before implementation, preserving full behavior coverage.

Regression obligations: all PA22 parser/file/integration tests (12/10/8), all existing authority/config/service/gateway/admission tests, all 13 existing IT classes and all contract/module/role/ACL/migration checks. No live Tencent/CAM/metadata network or production credentials in automated tests. Existing PostgreSQL 16 container tests remain actual database tests, not cloud acceptance.

## 10. Exact future implementation allowlist and documentation decision

The following **10 paths are future intent only**, contingent on an accepted, SDK-complete plan and later implementation authorization. No glob authorizes additional paths.

| Action | Exact path |
| --- | --- |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmMediaPlaybackSigningKeyProvider.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmSecretStringClient.java` |
| Modify | `backend/src/main/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackKeySourceConfiguration.java` |
| New | `backend/src/test/java/com/onecall/core/sharing/internal/keysource/TencentSsmMediaPlaybackSigningKeyProviderTests.java` |
| New | `backend/src/test/java/com/onecall/core/sharing/internal/keysource/TencentSsmSecretStringClientTests.java` |
| New | `backend/src/test/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackTencentKeySourceIntegrationTests.java` |
| Modify | `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` |
| Modify | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` |
| Modify | `backend/pom.xml` |
| Modify | `.github/workflows/ci.yml` |

POM change: exactly one new direct compile dependency `com.tencentcloudapi:tencentcloud-sdk-java-ssm:3.2.46`. Supplied exact-backend scratch build passed Enforcer convergence and banned-dependency checks without exclusions or dependency-management changes. No direct OkHttp dependency, added plugin/repository or aggregate SDK; preserve all existing pins/scopes. Section 11 fixes the full resolved delta.

Documentation decision: this plan is the internal configuration/behavior contract; existing architecture/security documents already establish custody and role boundaries. No README, resource/YAML/deployment, public contract, model, migration or Control edit is authorized. A later requirement for operator deployment instructions belongs to Platform/Security; no extra report file is authorized for implementation. Parser, file provider, credential authority/codec/provider interface, playback/service/current-state source and all existing IT files remain unchanged.

## 11. Fresh measured inventory and exact target accounting

Measurements were executed against this workspace, not copied from PA22 closeout. A safe read-only scan reconstructed CI's actual expected sets using its inventory-building AST assignments/updates and compared every plain Java test method/class. It also parsed actual POM XML, counted annotation lines/internal files and checked report presence. Source=CI comparison **passes**. There are **zero** local XML reports: current source=CI=XML execution success is **not observed**.

| Inventory | Measured current / CI required | Proposed S1 target / CI/XML required |
| --- | ---: | ---: |
| Surefire classes / methods | 33 / 266 | 36 / 309 |
| Failsafe classes / methods | 13 / 214 | 13 / 214 |
| Total classes / methods | 46 / 480 | 49 / 523 |
| Annotation-line scan `@Test\b` | 481 (one frozen comment) | 524 (same comment only) |
| Configuration / sharing boundary / authority methods | 13 / 7 / 24 | 19 / 8 / 24 |
| Sharing internal regular files | 79 | 81 |
| Keysource / credential files | 3 / 3 | 5 / 3 |
| POM direct dependencies / managed imports | 11 / 1 | 12 / 1, single verified direct dependency |
| POM normal-build / profile plugins | 3 / 1 | 3 / 1 unchanged |
| POM Failsafe includes | 13 | same 13 |
| Local TEST XML files / production jar / runtime manifest | 0 / absent / absent | fresh 49 XML reports / actual jar / actual manifest required |

Actual direct artifact order: `spring-boot-starter-webmvc`, `spring-jdbc`, `jooq`, `HikariCP`, `spring-boot-starter`, `spring-modulith-starter-core`, `spring-boot-starter-test`, `testcontainers`, `postgresql`, `flyway-core`, `flyway-database-postgresql`. One managed import `spring-modulith-bom`; build plugins `spring-boot-maven-plugin`, `maven-surefire-plugin`, `maven-enforcer-plugin`; profile plugin `maven-failsafe-plugin`. Preserve all existing versions/scopes, enforcer convergence/bans and toolchain pins. New SSM dependency is the only intended direct addition.

Failsafe includes stay exactly: `PostgresContainerIT`, `SharingQueryServiceIT`, `CustomerMeIT`, `CustomerSharesIT`, `SharingRoleLeastPrivilegeIT`, `CustomerShareCommandsIT`, `PublicSharesIT`, `PublicShareAdmissionIT`, `SharingAdmissionRoleLeastPrivilegeIT`, `EvidenceMediaReadRoleLeastPrivilegeIT`, `EvidenceMediaReadIT`, `MediaPlaybackIT`, `MediaGatewayTransportIT`, each `**/<class>.java`.

CI inventory updates: introduce `pa23_additions` with precisely section 9's five class/method sets before source-class equality; assert distribution `[18,8,10,6,1]`. Early Surefire aggregate and final exact_inventory argument 266→309, classes 33→36, current methods `[309,214]=523`, annotations 524. Preserve the historical PA18/19/20/21/22 totals and add PA23-start `[266,214]=480`, scan 481; subtract PA23 additions in **every** earlier historical reconstruction, not just current totals. Historical method totals remain `[112,178]`, `[128,192]`, `[162,198]`, `[196,204]`, `[231,214]`; annotation scans 291/321/361/401/446. Preserve sole comment hit `PostgresContainerIT.java` containing `// evidence inside this existing method; no new @Test and no catalog weakening.` No annotation-looking new comments.

### Production jar dependency firewall — independently exercised static literals

Earlier plan authoring recorded that the checked-in firewall literal consistency checks and **82 adversarial probes passed**, without packaged-jar acceptance. This continuation does not rerun those probes or claim application/test execution. Read-only comparison of the supplied baseline/SDK manifests confirms the exact target delta below; 116 probes remain future implementation acceptance, not a present result.

### Supplied exact Maven/Boot graph and firewall target

Read-only controller evidence under `T` (Windows Temp parent defined in section 1); supplied scratch builds are not current worktree test results:

| Evidence path relative to `T` | SHA-256 |
| --- | --- |
| `pa23-sdk-graph-20261110/target/pa23-runtime-dependencies.txt` | `643A060603ADC54E7D4187882D28D42A61102D5A51B971BFDEF26937080A27AF` |
| `pa23-sdk-graph-20261110/target/pa23-dependency-tree.txt` | `7B7D27FCB8CF7B1BF2E6D4A734BF48CBC777BA5E19329E344576B9E7EC293432` |
| `pa23-sdk-graph-20261110/target/pa23-jar-index.txt` | `C2AEE53D14CEAB8A9D0D1346366F81C7B7F7C4FAA618CAEC75B5F8B5351C3060` |
| `pa23-baseline-graph-20261110/target/pa23-baseline-runtime-dependencies.txt` | `74113B6319DC738D72050C2711CE4C58BDF6A3B375F0146FBED662B09145B75D` |
| `pa23-baseline-graph-20261110/target/pa23-baseline-jar-index.txt` | `94BDB63E3BF13087A395F725AD1BBCA4B96CF5158F7B2217E9155159A9D5C4A4` |

| Firewall metric | Checked-in / actual baseline | Exact SDK target |
| --- | ---: | ---: |
| Frozen historical resolved / packaged | 37 / 34 | 37 / 34 unchanged |
| Approved runtime coordinates | 55 | **72** |
| Dependency-only coordinates | 8 | **8** unchanged |
| Coordinates with packaged counterpart | 47 | **64** |
| Packaged literal filenames (sole jarmode included) | 48 | **65** |
| Additions / removals versus current coordinates | — | **+17 / 0**, no replacement |
| Additions / removals versus current packaged filenames | — | **+17 / 0**, no replacement |
| Delta versus historical resolved / packaged | 18 / 14 | **35 / 31**, removals 0 / 0 |
| Approved forbidden-regex collisions | 7 | **7**, same exact positive-first exceptions |
| Packager-only branch | 1 | **1**, same `spring-boot-jarmode-tools-4.1.2.jar` |
| Static adversarial probes | 82 | **116 = 82 + 34**, retain all existing probes |

The exact target tuple-to-jar manifest is the unchanged **55 literal approved_rows** and **48 packaged_literal filenames** in checked-in `.github/workflows/ci.yml`, union these **17 literal rows/filenames**, no removed/replaced row. This finite union is not a namespace wildcard; hashed baseline/SDK manifests establish reproducibility. Preserve all 8 existing dependency-only classifications; all 17 additions have packaged counterparts. Tuple order is `group:artifact:type:version:classifier:scope`, empty classifier, compile scope:

| Added exact coordinate | Added exact packaged filename |
| --- | --- |
| `commons-io:commons-io:jar:2.20.0::compile` | `commons-io-2.20.0.jar` |
| `org.apache.commons:commons-lang3:jar:3.20.0::compile` | `commons-lang3-3.20.0.jar` |
| `org.jetbrains:annotations:jar:17.0.0::compile` | `annotations-17.0.0.jar` |
| `com.tencentcloudapi:tencentcloud-sdk-java-ssm:jar:3.2.46::compile` | `tencentcloud-sdk-java-ssm-3.2.46.jar` |
| `com.tencentcloudapi:tencentcloud-sdk-java-common:jar:3.2.46::compile` | `tencentcloud-sdk-java-common-3.2.46.jar` |
| `com.squareup.okhttp3:okhttp:jar:4.12.0::compile` | `okhttp-4.12.0.jar` |
| `com.squareup.okio:okio:jar:3.6.0::compile` | `okio-3.6.0.jar` |
| `com.squareup.okio:okio-jvm:jar:3.6.0::compile` | `okio-jvm-3.6.0.jar` |
| `org.jetbrains.kotlin:kotlin-stdlib-common:jar:1.9.10::compile` | `kotlin-stdlib-common-1.9.10.jar` |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8:jar:2.3.21::compile` | `kotlin-stdlib-jdk8-2.3.21.jar` |
| `org.jetbrains.kotlin:kotlin-stdlib:jar:2.3.21::compile` | `kotlin-stdlib-2.3.21.jar` |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7:jar:2.3.21::compile` | `kotlin-stdlib-jdk7-2.3.21.jar` |
| `com.google.code.gson:gson:jar:2.13.2::compile` | `gson-2.13.2.jar` |
| `com.google.errorprone:error_prone_annotations:jar:2.41.0::compile` | `error_prone_annotations-2.41.0.jar` |
| `com.squareup.okhttp3:logging-interceptor:jar:4.12.0::compile` | `logging-interceptor-4.12.0.jar` |
| `org.apache.commons:commons-configuration2:jar:2.15.1::compile` | `commons-configuration2-2.15.1.jar` |
| `org.apache.commons:commons-text:jar:1.15.0::compile` | `commons-text-1.15.0.jar` |

Boot management mediates common's requested commons-logging **1.2 → 1.3.6**, Gson **2.8.9 → 2.13.2** and applicable Kotlin additions to **2.3.21** (stdlib-common remains **1.9.10**). It brings already-managed commons-io **2.20.0**, commons-lang3 **3.20.0** and annotations **17.0.0** paths into the runtime graph. No existing coordinate is replaced; tools.jackson core/databind **3.1.7** and jackson-annotations **2.21** remain unchanged, no Jackson 2 core/databind regression. Enforcer convergence/bans passed in the supplied exact-backend scratch build with only one new direct SSM compile dependency; no exclusions/management edits.

Future CI edits update **all** firewall literals/count assertions and diagnostics: approved_rows union these 17 rows, packaged_literal union these 17 filenames; approved=72, dependency_only=8, counterparts=64, packaged=65, historical deltas=35/31, probes=116. Preserve historical 37/34, one jarmode and seven collisions, exact positive-first approved-filename exceptions, coordinate/filename membership, package-entry checks, classifier/version/group forgery, Jackson 2/test/security/ORM/web-adjacent bans and missing/extra/duplicate rejection. No Tencent namespace wildcard or waived check.

Add **one coordinate mutation and one filename mutation for each of the 17 additions**: **17 × 2 = 34**, **82 + 34 = 116** rejection probes. Starting from the complete valid target each time, mutate the coordinate version away from the exact tuple, and independently mutate its paired filename version; each must reject. Retain all 82 existing adversarial probes verbatim. Later implementation must reproduce these actual graph/jar manifests, not treat scratch evidence as test execution.

### Reproduce current safe static measurements

The PA22 plan contains the actual reusable read-only AST/source/POM inventory scanner; execute its code, not its historical expected prose. This command was run here and produced the current table:

```bash
python3 - <<'PY'
from pathlib import Path
text = Path('docs/gateflow/pa22-sharing-media-production-key-custody-and-provider-integration-plan.md').read_text()
scan = text.split('### Reproducible read-only source/CI/POM inventory scan', 1)[1].split("python3 - <<'PY'\n", 1)[1].split('\nPY', 1)[0]
exec(compile(scan, '<readonly-inventory>', 'exec'))
PY
```

For future PA23 source comparison extend that scanner's selected AST roots/loops to include `pa23_additions`, not the old counts. The full checked-in CI heredoc, run after real clean verification, remains the authority for source=CI=XML and historical totals.

## 12. Future validation commands and acceptance evidence

Only after independent plan acceptance and separate implementation authorization, use the pinned working runner/JDK/Maven/Docker. Do not execute these application/build commands during this planning turn:

```bash
cd backend
java -version
./mvnw -B -ntp -v
docker info
./mvnw -B -ntp -Dtest=TencentSsmMediaPlaybackSigningKeyProviderTests,TencentSsmSecretStringClientTests,MediaPlaybackTencentKeySourceIntegrationTests,MediaPlaybackKeyBundleParserTests,FileMediaPlaybackSigningKeyProviderTests,MediaPlaybackFileKeySourceIntegrationTests,MediaPlaybackConfigurationTests,MediaPlaybackCredentialAuthorityTests,SharingQueryBoundaryTests test
./mvnw -B -ntp clean test
./mvnw -B -ntp -Pcontainer-smoke clean verify
./mvnw -B -ntp dependency:tree -Dverbose -DoutputFile=target/pa23-dependency-tree.txt
./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
cd ..
```

Target focused count **117 = 18+8+10+12+10+8+19+24+8**; full Surefire 36/309 plus Failsafe 13/214, total 49/523, all zero skips/errors/failures. Run the exact checked-in inventory validator after clean verify, not against stale XML:

```bash
python3 - <<'PY'
import os, textwrap
from pathlib import Path
workflow = Path('.github/workflows/ci.yml').read_text()
block = textwrap.dedent(workflow.split("python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])
os.chdir('backend')
exec(compile(block, '.github/workflows/ci.yml:container-inventory', 'exec'))
PY
```

Execute the existing `.github/workflows/ci.yml` **default and opt-in web jar smokes**, **production jar dependency firewall** and **contract-regressions** steps verbatim in their pinned environments (Node 22.23.4), preserving their commands/arguments. Updating inventory literals is not permission to replace these tests. The jar smokes must still start with no provider/configured cloud and make no metadata/SSM call. Contract regression includes `cd api/openapi && npm ci && npm run check:s3` and the checked-in protocol/security/capacity/acceptance validator invocations; do not claim those ran because Maven passed. No public/migration/resource/deployment change is expected.

After implementation inspect exact changed paths against section 10, all effective/runtime/jar tuples against the exact section 11 manifest, synthetic-output redaction, and prohibited surfaces. Run `git diff --check`, `git diff --exit-code -- backend/src/main/resources api/openapi deployment`, `git diff --name-only`, `git ls-files --others --exclude-standard` using the explicit gitdir prefix where necessary. Include untracked new files in write-scope review. Do not stage, commit, push or invoke any review gate as part of these instructions.

## 13. Residual risks, owners and binding stop

| Risk / remaining obligation | Owner / boundary |
| --- | --- |
| Unavoidable SDK limitation: CvmRoleCredential exposes no metadata connect/read/call timeout; SSM exposes no total callTimeout/DNS deadline despite bounded connect/read/write defaults 2/3/2 seconds | **Platform/Security** owns deployment/runtime validation and suitability; not an end-to-end deadline. No custom metadata/token/HTTP client, fake timeout, direct OkHttp dependency or fallback |
| Default underlying retryOnConnectionFailure may make multiple connection attempts within one application GetSecretValue invocation | Platform/Security/Operations; no application/AbstractClient.retry, single-socket promise, cache, backoff or SLO claim |
| Real Tencent region/endpoint availability, CAM least privilege, CVM role binding, resource/version provisioning, immutable rollout and live acceptance | **Platform/Security** deployment work; no cloud provisioning/network acceptance here, no environment/profile/OIDC switch for non-CVM deployment |
| Explicit-version overlap and old kid-to-material identity; removing an old kid too soon | Platform/Security + sharing owner; no cross-snapshot registry; Q22 lifetime/skew/revocation remains unselected |
| Java/SDK Strings and cryptographic copies remain in process memory | Security; owned-buffer clearing/redaction is not secure heap erasure or HSM signing |
| Future vulnerability/mediation drift or SDK logging incompatibility | Build/Security; current pinned graph evidence is not all future advisories; stop rather than loosen bans, enable logging or add aggregate SDK |
| Current worktree XML/jar absent; planned implementation/tests not executed | Execution/build owner after independent acceptance and separate authorization; scratch graph is not current source=CI=XML or cloud acceptance |

**Unresolved/blocked authoring items: none.** B1–B3 publication/API/credential/profile/response/error/logging/graph placeholders remain resolved by direct pinned evidence with exact unavoidable SDK limitations disclosed. Initial independent A/B plan reviews occurred; the Controller accepted exactly one material finding. That finding is now a fixed candidate pending independent re-review, not accepted closure. Re-review/plan acceptance and implementation verification remain unperformed gates, not implied approval. Single S1, all 10 implementation paths, 43-method behavior map (including exactly six configuration additions) and 49-class/523-method target remain unchanged; no scope creep, second authority, fallback source, public/database/migration/ACL/deployment/provisioning change, extra slice or production secret.

**Binding stop:** FIX-1 author-complete handoff pending independent re-review. Report fix task label, provider/runtime, both changed document paths, byte sizes/SHA-256 values, bounded selector correction, unchanged 6/43 and 49/523 counts, actual documentation-only validation and prohibitions. No implementation/test/build/cloud execution occurred in this fix. Do not edit source/tests/POM/CI/Control/resources/README/review artifacts/temp evidence, stage, commit, push, open/mutate a PR, dispatch review, approve, mark ready, request reviewers, comment externally, mutate issues, enter re-review or any later gate. Only this plan and its FIX-1 artifact are task-modified repository artifacts; pre-existing Control/review/helper state stays untouched. No runtime or reviewer acceptance is claimed.
