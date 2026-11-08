# PA19 sharing media gateway playback foundation — resumed plan

## 1. Gate, authority and goal

- Task: `pa19-sharing-media-gateway-playback-foundation-plan-resume1-gpt-20261108`; date: 2026-11-08; gate: **plan only**. This document is an implementation specification, not implementation authorization or a review decision.
- Author fix: `pa19-sharing-media-gateway-playback-foundation-plan-fix1-gpt-20261108`; date: 2026-11-08; gate: **fix**; requested runtime: GPT `gpt-6.1-sol` / high. This bounded revision implements only the Control's accepted findings/precision; it is not plan acceptance, readiness, implementation or A/B re-review.
- Inspected HEAD: `fc3bd7639b97aa1301184a9aa8b4e25676766e99`, branch `codex/pa19-sharing-media-gateway-playback-delivery-resume`. PA19A merge prerequisite: `3afc3e9a68900ea718b534650ca661861f036d4e`, parents `db8a8e9bb3c70376a7c258e357c3f5b2c38e8609` and `e8e64a97ddc0828e09adeaf84a8c7f43440cb491`.
- Binding authority: `docs/gateflow/project-execution-control.md`, “Active resume checkpoint - 2026-11-08 / PA19 sharing media gateway playback foundation”. The already-confirmed goal is unchanged: consume an already-issued opaque `onecall:media-playback:v1` credential through trusted verification, immutable claim validation, current sharing authorization, evidence-owned single-object resolution, and one scope-bounded internal controlled-read seam.
- Read-only superseded input: `/mnt/c/Users/Administrator/.codex/worktrees/pa19-sharing-media-playback-resume/controller-runtime/pa19-old-blocked-plan.md`; SHA256 `bc2587b4da0af962ce3bd7f6a5c9677abec748191996437f3cd688223bab7b16`. Retain its contract/non-goals/residual analysis; replace its evidence-runtime blocker with the merged PA19A API/configuration, and replace its obsolete counts with the measured inventory below. Do not edit that input.
- Applicable instruction discovery: the ancestor `.codex/AGENTS.md` exists and is empty; no repository `AGENTS.md` or `CLAUDE.md`, including nested source/docs instructions, was found. No external research or runtime test execution is claimed.

### Motivation and binding success signals

PA18 issues/replays a credential and records authorization events; it does not consume that credential or open media. PA19A now supplies the missing least-privilege evidence reader. The minimum remaining path must demonstrate that verified authority, not client-selected storage facts or a sharing projection, determines the single object and read scope.

Success is exactly the Control signal: deterministic tests prove a valid verified claim reaches one controlled-read open for exactly its resolved private object; invalid, malformed, expired, not-yet-valid, wrong-audience/purpose, cross-share/admission/object, stale-generation and inconsistent/ambiguous/unavailable cases fail closed without a usable handle. Credential/object/location redaction and existing PA18/PA19A behavior remain intact. Existing PostgreSQL 16 evidence-reader and least-privilege tests remain in the full gate. No new product acceptance criterion is introduced.

### Non-goals and immutable boundary

No public data-plane route, controller/filter, OpenAPI change, gateway origin/path, CDN/LB/ingress, HTTP range/streaming contract, storage bytes, byte-array cache, buffered-byte recall, upload, download permission, transcoding, generic storage framework, schema, role, migration or privilege change. No COS/KMS/Secret Manager client, credentials or network call. No production signature/key/wire format, token parsing, Redis redesign, Q22 numeric TTL/skew/revocation window, or policy defaults. No PA15–PA18 issuance/replay/event/count semantics change. V001–V013 stay byte-identical. Tests use fixture values, never turn them into production policy. Residual destinations below are ownership records, not future slices or additional goals.

## 2. Direct evidence and first-principles alignment

All paths in this section are existing inputs, not permission to edit them.

| Evidence | Observed source fact | Design consequence |
| --- | --- | --- |
| `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareMediaCredentialProvider.java` | `issue`, `issueOrReplay`, sealed issuance results; no verification method. Authorization request has grant/admission/generations/time bounds, not a client object location. | Add a separate injected verifier; do not change issuance, invent a payload decoder, or require PA18 to emit a new format. A trusted provider can return server-enriched verified facts; this plan does not implement that provider. |
| `.../sharing/internal/admission/DefaultShareMediaAuthorizationService.java`, `authorize`, `authoritative`, `bound`, `outside` | Locks/checks current grant and admission witness; permits ACTIVE and already-admitted LOCKED; checks ownership/binding/risk and member lifecycle; calls issuer outside the state transaction; then writes deterministic events. | Consumption must independently recheck relevant current facts, but must not call `authorize`, replay issuance, acquire admission counters, append events, or mutate expiry. |
| `.../sharing/internal/admission/ShareAdmissionRepository.java`, `JooqShareAdmissionRepository.java` | Existing grant-by-token lookup and grant lock, admission-by-ID, canonical witness fields and current lifecycle/risk reads. Only the Jooq implementation implements the interface in the source tree. | Add one non-locking grant-by-ID read; reuse the existing sharing-owned repository under a separate read-only repeatable-read transaction. No implementation ripple to a hand-written fake repository is necessary; tests currently mock it. |
| `.../sharing/internal/persistence/ShareAdmissionPersistenceConfiguration.java` | `persistence` plus explicit `onecall.sharing-admission.datasource.enabled=true`; qualified admission resources; explicit `sharing_admission_role`; existing template is read/write READ_COMMITTED. | Do not reuse its mutable transaction settings or change them. Copy its template settings into a new read-only REPEATABLE_READ/REQUIRES_NEW template in the new state reader. |
| `.../evidence/mediaread/EvidenceMediaObjectReader.java`, `ResolvedMediaObject.java`, `package-info.java` | `Result resolve(UUID evidenceId)`; sealed `Found(media)`, `Missing`, `Unavailable`; safe media UUID/hash/size/MIME/optional duration; named interface `media-read`. | The verified evidence UUID is the sole resolver key; compare the returned media UUID and duration with the verified scope. Use only this named API; do not import its internal implementation. |
| `.../evidence/internal/mediaread/JooqEvidenceMediaObjectReader.java`, `read` | One 15-column query binds the supplied evidence ID, requires one ORIGINAL artifact and matching SOLIDIFIED CALL_RECORDING/VALIDATED recording/media; zero evidence is Missing, multiple/inconsistent facts are Unavailable; no LIMIT/aggregation or bucket/key/version selection. | Honor the existing three outcomes; do not re-resolve, select an arbitrary object, query artifacts from sharing, or pretend Unavailable identifies a specific corruption. |
| `.../evidence/internal/mediaread/EvidenceMediaReadPersistenceConfiguration.java`, `EvidenceMediaReadPersistenceProperties.java` | Dedicated qualified read-only resources under `persistence` and explicit `onecall.evidence-media-read.datasource.enabled=true`; explicit reader role; transaction closes before `resolve` returns. | Consume the existing bean. Missing configuration is a safe unavailable dependency, not authority to use the sharing/default datasource. |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql`, V012 and V013 | V008 owns immutable object identity and validated media facts. V012 permits admission/state reads but not private media/artifact/recording reads. V013 permits exactly 15 evidence columns through `evidence_media_reader_role`, without object location or general table SELECT. | The old blocker is closed by PA19A, not by granting sharing more access. Neither SQL nor credential/role changes are needed. |
| `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`, `SharingQueryBoundaryTests.java` | Exact 14-module map; sharing currently permits only `evidence::share-projection`; exact internal source partitions and four existing sharing named interfaces. | Explicitly add only `evidence::media-read` to the sharing row and matching test map. Keep playback internal and all four existing interfaces exact. Update the source partition, not the non-disclosure prohibitions. |
| `docs/gateflow/pa0-production-runtime-bootstrap-plan.md` §6.2 and `docs/12-technology-stack-and-system-architecture.md` §6 | Historical Java upper-bound map permits sharing → evidence, rejects foreign internals/cycles; sharing owns authorization/playback, evidence owns objects. | This document records the precise named-interface refinement; no fifteenth module or cross-cutting module edge is justified. Historical plans stay unchanged. |
| `api/openapi/onecall-v1.openapi.yaml`, `authorizePublicShareMedia`, `MediaAuthorizationRequest`, `MediaAuthorization` | Existing authorization operation returns opaque credential/expiry; purpose is PLAYBACK/SEEK/RENEW; no playback data-plane operation. | Reuse the existing purpose enum, not a new public protocol. The internal consumer takes no client object identifier and exposes no web endpoint. |
| `docs/14-production-data-model-and-flyway-schema-spec.md` §§4.39–4.45; `docs/17-production-security-design-spec.md` C10/P1, §media authority and residual table | Private append-only media identity, current sharing generations, bearer-only object resolution; LOCKED does not invalidate an admitted session; no issued-byte recall; Q22 and transport remain deferred. | Validate identity, current facts and exclusive expiry without asserting continuous revocation or timing policy. A typed bounded handle is sufficient here; HTTP/storage behavior is not. |
| `ShareAdmissionServiceTests`, `PublicShareAdmissionIT`, `EvidenceMediaObjectReaderTests`, `EvidenceMediaReadIT`, `EvidenceMediaReadRoleLeastPrivilegeIT`, configuration/architecture tests | Existing exact issuance/replay/redaction and least-privilege coverage. | Keep every existing method and assertion strength; add consumer tests without replacing PA18 or PA19A tests. |

First-principles judgment: authority has three different owners. The verifier attests immutable credential facts; sharing checks present authorization; evidence resolves immutable private media facts. A storage/transport provider is not entitled to decide any of those. Keeping orchestration in sharing's private playback package avoids a gratuitous module and lets sharing use its own state, while the only new foreign Java access is the exact existing evidence named interface. A verified claim alone is insufficient for revocation; a current sharing row alone is insufficient for credential scope; a projection is insufficient for private object resolution. No layer may substitute for another.

## 3. Exact architecture and type contract

New main sources live in `com.onecall.core.sharing.internal.playback`. They are not a named interface and cannot be imported by other modules. Public Java visibility for construction/testing is not a public transport/API. Nested types below belong in the named enclosing file; no additional source files are implicit. `UUID`, `Instant`, `Duration`, `Clock` are JDK types. `Purpose` below is exactly `MediaAuthorizationRequest.Purpose` from the existing sharing-owned `internal.publicadmission` package.

```java
final class OpaquePlaybackCredential {
    OpaquePlaybackCredential(String value); // public constructor for internal callers/tests
    String value();                        // public; verifier input only
    String toString();                     // constant redacted label
}

record VerifiedMediaClaim(
    String audience, UUID shareGrantId, UUID shareAdmissionId,
    UUID evidenceId, UUID mediaObjectId,
    int revocationGeneration, int codeGeneration, Purpose purpose,
    Instant issuedAt, Instant notBefore, Instant expiresAt,
    int durationSeconds, PlaybackWindow window) {
    record PlaybackWindow(Duration startInclusive, Duration endExclusive) {}
}

interface MediaCredentialVerifier {
    Result verify(OpaquePlaybackCredential credential);
    sealed interface Result { // permits exactly these nested records
        record Verified(VerifiedMediaClaim claim) implements Result {}
        record Invalid() implements Result {}
        record Unavailable() implements Result {}
    }
}

interface PlaybackStateReader {
    Result check(VerifiedMediaClaim claim);
    sealed interface Result {
        record Allowed(Instant checkedAt) implements Result {}
        record Denied(MediaPlaybackService.Reason reason) implements Result {}
        record Unavailable() implements Result {}
    }
}

final class SharingPlaybackStateReader implements PlaybackStateReader {
    SharingPlaybackStateReader(TransactionTemplate admissionTransactions,
        ShareAdmissionRepository repository, ShareAdmissionSha256Provider sha256);
    Result check(VerifiedMediaClaim claim);
}

interface ControlledMediaReader {
    Result open(ReadScope scope);
    record ReadScope(ResolvedMediaObject media, Purpose purpose,
        VerifiedMediaClaim.PlaybackWindow window, Instant expiresAt) {}
    record ControlledReadHandle(ReadScope scope) {}
    sealed interface Result {
        record Opened(ControlledReadHandle handle) implements Result {}
        record Unavailable() implements Result {}
    }
}

final class MediaPlaybackService {
    MediaPlaybackService(MediaCredentialVerifier verifier, PlaybackStateReader state,
        EvidenceMediaObjectReader evidence, ControlledMediaReader reader, Clock clock);
    Result authorizeAndOpen(OpaquePlaybackCredential credential, Purpose expectedPurpose);
    enum Reason {
        MALFORMED_CREDENTIAL, INVALID_CREDENTIAL, MALFORMED_CLAIM,
        WRONG_AUDIENCE, WRONG_PURPOSE, NOT_YET_VALID, EXPIRED,
        SCOPE_MISMATCH, REVOKED, STALE_GENERATION, ADMISSION_MISMATCH,
        CURRENT_STATE_DENIED, EVIDENCE_MISSING
    }
    sealed interface Result {
        record Opened(ControlledMediaReader.ControlledReadHandle handle) implements Result {}
        record Denied(Reason reason) implements Result {}
        record Unavailable() implements Result {}
    }
}
```

Make constructors/methods shown for the final classes public, overriding interface methods public, and sealed `permits` lists explicit. The snippet describes signatures, not complete Java compilation units. The opaque wrapper deliberately permits malformed input to reach the service's typed validation. `VerifiedMediaClaim` and its window are immutable values, not a trusted-client DTO; allow construction of malformed values for boundary tests and validate them in the service. All valid paths must obey the invariants below. `ReadScope`, `ControlledReadHandle` and both `Opened` records reject null components with a fixed safe argument message; their structural construction also checks window positivity and any available duration bound. Other result records carrying a reason require a non-null enum. No mutable collection, byte array or JsonNode is retained in a claim or handle.

The handle is an immutable **typed bounded internal read handle**, containing only the exact read scope delivered to the injected provider. It is not a live InputStream, byte cache, URL, bearer, generic provider token or independently redeemable broader credential. `open` is the single controlled-read seam; the unavailable default opens nothing, deterministic test providers return this handle, and no production storage resource is allocated in this slice. Do not add speculative `read`, `seek`, `close`, HTTP, resource-lifecycle or byte-transfer APIs. The representation makes the accepted foundation testable without pretending to implement delivery.

### Wiring

`MediaPlaybackConfiguration` is a non-proxied configuration in the new private package. It creates the service with service-local fail-closed verifier/reader defaults, each returning its sealed Unavailable; these defaults are not beans and do not use `@ConditionalOnMissingBean`. The explicit bean-type replacement contract is deterministic: after bean definitions are registered, the service factory resolves each of the four dependency types through a typed `ObjectProvider`; zero candidates uses a local Unavailable fallback, exactly one uses that explicit bean, and more than one fails configuration with a fixed safe message, even if one is `@Primary`. Count candidates with a bounded `stream().limit(2)` check in a private helper before `getIfAvailable`; never select a first/primary candidate among multiple authorities. This requires no bean overriding, registration-order assumption, auto-configuration imports resource or additional file. A nested configuration guarded by `@Profile("persistence")` and `@ConditionalOnProperty(prefix="onecall.sharing-admission.datasource", name="enabled", havingValue="true")` creates `PlaybackStateReader` as `SharingPlaybackStateReader` from the existing qualified `sharingAdmissionTransactionTemplate`, repository and qualified `sharingAdmissionSha256Provider`. Do not change the existing admission configuration.

All four dependencies use the same zero/one/multiple-candidate rule above; missing state/evidence remains Unavailable, not authority to use another datasource. Production defaults use `Clock.systemUTC()` locally; the constructor accepts a deterministic clock for tests. No new enable flag, configuration properties, key, datasource, DSLContext, transaction manager or dependency is introduced. No fake signer/verifier/reader implementation is scanned into the product. Existing evidence enablement/credentials and admission enablement/credentials remain explicit and unchanged.

The exact service bean method is `MediaPlaybackService mediaPlaybackService(ObjectProvider<MediaCredentialVerifier> verifier, ObjectProvider<PlaybackStateReader> state, ObjectProvider<EvidenceMediaObjectReader> evidence, ObjectProvider<ControlledMediaReader> reader)`. `MediaCredentialVerifier unavailableMediaCredentialVerifier()` and `ControlledMediaReader unavailableControlledMediaReader()` are private local fallback factories, not `@Bean` methods. Candidate-resolution helpers stay private inside this already-authorized configuration file. The nested `PersistenceStateConfiguration` declares `PlaybackStateReader playbackStateReader(@Qualifier("sharingAdmissionTransactionTemplate") TransactionTemplate transactions, ShareAdmissionRepository repository, @Qualifier("sharingAdmissionSha256Provider") ShareAdmissionSha256Provider sha256)`. It does not declare a fallback state bean that could shadow the authoritative one. The service bean supplies its clock directly, avoiding an unrelated global Clock bean contract.

## 4. Ordered behavior and invariants

Use the following precedence as an ordered decision procedure; the first failure wins. All outcomes contain only the typed result/constant reason. No stage after a failed stage runs. “No read” means zero controlled-read open calls on pre-open denial; for reader unavailability there is one open attempt and no successful handle or object-byte read.

1. **Ambient transaction guard and input.** If `TransactionSynchronizationManager.isActualTransactionActive()` is true, return Unavailable before verifier/provider/database I/O. No ambient transaction may be suspended and resumed around an opened read. Null/blank credential, NUL, non-UTF-8-encodable string, or more than the existing C10 2048 code points returns Denied(MALFORMED_CREDENTIAL). Do not trim, normalize or parse it. Null intended purpose returns Denied(WRONG_PURPOSE). The service accepts no share/admission/evidence/media identifier, duration/window, bucket/key/version/URL, request DSL or metadata from a caller.
2. **Trusted verification.** Call `verify` once with the unchanged opaque credential. Invalid returns Denied(INVALID_CREDENTIAL). Unavailable, null result, or a thrown RuntimeException returns Unavailable; discard exception/cause text. A Verified containing a null claim is Denied(MALFORMED_CLAIM). No issuer invocation or credential format inference occurs.
3. **Immutable shape.** Require non-null audience, all four UUIDs, purpose, all three instants and both window durations; nonnegative generations; positive duration; `issuedAt <= notBefore < expiresAt`; `0 <= startInclusive < endExclusive <= Duration.ofSeconds(durationSeconds)`. Null/invalid/overflowing facts return Denied(MALFORMED_CLAIM). Do not clamp a window or manufacture missing bounds. UUID identities are exact, not strings to normalize. Empty audience proceeds to exact audience denial; null audience is malformed.
4. **Audience then purpose.** Audience must be the exact case-sensitive string `onecall:media-playback:v1`, with no suffix, trim or alternate audience. Otherwise Denied(WRONG_AUDIENCE). `expectedPurpose` is trusted internal invocation intent, never a client wire value; claim purpose must exactly equal that non-null intended Purpose, otherwise Denied(WRONG_PURPOSE). Each of PLAYBACK, SEEK and RENEW uses the same verification/read bounds. RENEW is only a verified consumption purpose: it issues/extends nothing, opens no broader read and grants no extension of authority beyond the verified scope.
5. **Credential time.** Read the injected clock. `now < issuedAt` or `now < notBefore` returns Denied(NOT_YET_VALID). `now >= expiresAt` returns Denied(EXPIRED); equality is excluded and `now == notBefore` is valid. No tolerance/skew, minimum/maximum TTL, relative duration or grace period is invented.
6. **Current sharing state.** Call `PlaybackStateReader.check(claim)` once. Its Denied reason is propagated, Unavailable/null/exception maps to Unavailable. Allowed requires non-null `checkedAt` with `issuedAt <= checkedAt`, `notBefore <= checkedAt < expiresAt`. Null or non-null out-of-interval `Allowed(checkedAt)` is an inconsistent adapter result and maps only to Unavailable, never to a time denial. At this boundary only explicit state-reader Denied outcomes carry NOT_YET_VALID/EXPIRED reasons; the local clock fences in steps 5/8 retain their typed time denials. The adapter's exact checks and precedence are in §5. No evidence call occurs on state failure.
7. **Evidence resolution.** Reassert no active transaction, then call only `EvidenceMediaObjectReader.resolve(claim.evidenceId())`, once. Missing returns Denied(EVIDENCE_MISSING). Unavailable/null/exception returns Unavailable. Found must supply valid immutable metadata; compare `mediaObjectId` exactly to the claim, else Denied(SCOPE_MISMATCH). Require non-null durationSeconds and exact equality to the verified duration; mismatch is Denied(SCOPE_MISMATCH), absent required duration is Unavailable. Revalidate positive size, lowercase 64-hex hash, nonblank MIME, positive duration and the window against that duration before constructing a ReadScope; impossible/malformed metadata is Unavailable. The production PA19A record already validates these shape facts. Do not fetch a second object or fall back to a sharing projection.
8. **Final local fence and open.** Recheck the local clock against claim not-before/expiry, and no ambient transaction, immediately before open. Time failure returns the same typed denial and zero opens. Construct a scope using the Found metadata, exact verified purpose/window/expiry, without expansion. Call `reader.open(scope)` once. Unavailable/null/exception is Unavailable. Opened must have a non-null handle whose complete scope equals the requested scope, including object ID/hash/size/MIME/duration, purpose/window and expiry; a mismatch is Unavailable, never accepted or retried. A successful result returns exactly that bounded handle. No second open, fallback or byte transfer is allowed.

The already immutable evidence object's size/hash/MIME are evidence facts, not caller facts. Claim duration is a constraint that must agree, not an override of the reader. PA19A allows an optional duration generally; this duration-bounded consumer must fail closed when it lacks that fact, without changing the evidence API or weakening its optional-duration tests.

### Outcome matrix and exception/redaction rules

| Case | Exact outcome / call boundary |
| --- | --- |
| Valid claim, ACTIVE or admitted LOCKED grant, matching state/media | Opened with exactly the verified scope; verifier=1, state=1, evidence=1, controlled open=1. |
| Malformed input / Invalid verifier result | Denied(MALFORMED_CREDENTIAL / INVALID_CREDENTIAL); no later calls. |
| Verifier absent/unavailable/null/throws | Unavailable; state/evidence/open=0. |
| Malformed verified facts, audience/purpose mismatch | Denied(MALFORMED_CLAIM / WRONG_AUDIENCE / WRONG_PURPOSE), in §4 precedence; no state/evidence/open. |
| Not-yet-valid, future issuedAt, exclusive expiry at either clock fence | Denied(NOT_YET_VALID / EXPIRED); no open. |
| State reader returns Allowed with null checkedAt, checkedAt before issuedAt/notBefore, or checkedAt at/after expiresAt | Unavailable as an inconsistent adapter result; evidence/open=0, not a synthesized time Denied. |
| Cross-share/admission, malformed witness, stale generation, revoked/expired/current-fact denial | §5's typed result; no evidence/open. There is no client identity channel with which to redirect lookup. |
| Evidence genuinely absent | Denied(EVIDENCE_MISSING); open=0. |
| Duplicate ORIGINAL artifacts, missing relationships, inconsistent recording/media/status, invalid metadata | PA19A returns Unavailable, propagated as Unavailable; open=0. The consumer must not recategorize this as Missing or invent a diagnostic subtype. |
| Found media UUID or duration disagrees with verified object scope | Denied(SCOPE_MISMATCH); open=0. |
| Evidence bean absent, resolver null/throws/unavailable, required duration absent | Unavailable; open=0. |
| Reader absent/unavailable/null/throws, or widens/misbinds returned handle | Unavailable; at most one open attempt, no successful handle/byte read. |

Every new sensitive wrapper/claim/window/scope/handle/Opened value has a constant redacted `toString`. Use `@JsonValue` on a constant redacted string method for these concrete sensitive values so root/nested Jackson serialization cannot expose fields; do not rely on `@JsonIgnoreType` alone or record defaults. The internal success handle intentionally retains the exact bounded scope, including media object UUID/hash/size/MIME/duration, purpose/window and expiry, for internal scope equality; redaction must not strip or replace these authority facts. Sensitive values expose only constant redacted labels through serialization/`toString`, never identifiers or metadata through logs or exceptions. No equals/hashCode/input value is logged. Denied may serialize only its fixed enum; Unavailable has no dependency details. No Throwable, SQL state/text, raw JSON, credentials, bucket/key/version, URL or provider message is stored in an outcome or thrown to a caller; identifiers retained internally must never appear in such exception text. Constructor failure messages are fixed safe literals; never chain external exceptions. Do not catch fatal JVM Errors. New production code performs no logging, stdout/stderr or event/audit writes. Tests capture logs and serialization using canaries, including exception messages resembling private locations, to prove absence. Existing immutable private UUID accessors remain internal authority facts, not public projections.

### Transaction ordering and no-I/O-after-open guarantee

Sequence: ambient guard → verifier → local claim validation → one sharing-owned read transaction → transaction closure → evidence-owned `resolve` transaction → transaction closure → local clock/scope checks → one controlled open → local result/handle equality check → return. New orchestration has no `@Transactional` annotation. Neither verifier nor evidence nor reader runs while sharing's transaction is active; no transaction or lock spans provider I/O.

After `reader.open` is invoked, orchestration performs only in-memory result/scope validation and returns. It performs **no** verifier, state, evidence, database, logging/event, policy or other provider I/O, no clock-based renewal and no second read/open. The handle contains no deferred loader/callback, connection, supplier, DSL, bytes or resource-owning stream. Thus returning it cannot lazily re-enter authorization/state lookup. Actual transport/byte delivery is outside this slice; do not claim a stream was delivered or buffered bytes revoked. Tests mark the open boundary and fail if any dependency/transaction acquisition is subsequently touched.

## 5. Authoritative sharing check without PA18 mutation

Add exactly `Optional<GrantSnapshot> findGrantById(UUID grantId)` to `ShareAdmissionRepository` and implement it in `JooqShareAdmissionRepository` by reusing `GRANT_COLUMNS`, the existing grant mapper and evidence-record join, with `WHERE grant_row.id=?`. Assert the existing dedicated current/session-user principal before this authoritative lookup. No token lookup, `FOR UPDATE`, `FOR SHARE`, advisory lock, write or additional evidence columns are permitted. Keep the existing `findGrantByTokenDigest`/`lockGrant` methods and their behavior unchanged. V012 already permits this SELECT. This is a sharing authorization read of its already-approved context, not media resolution or broadened evidence access.

`SharingPlaybackStateReader` makes a defensive copy of the injected TransactionTemplate configuration, setting readOnly=true, isolation=REPEATABLE_READ, propagation=REQUIRES_NEW. Never mutate the original template. One snapshot is needed because several existing repository methods read grant/witness/lifecycle/binding/risk facts. Use existing reads only; do not call lifecycle lock methods. A completed snapshot validates current committed facts without incrementing/replaying anything.

Within `check`, first validate the claim shape defensively (null/malformed => Denied(MALFORMED_CLAIM)), then execute these checks in order; unexpected repository/mapper/hash/transaction RuntimeException or null transaction return => Unavailable:

1. `findGrantById(claim.shareGrantId())`; absent or returned grant ID mismatch => Denied(ADMISSION_MISMATCH). `findAdmissionById(grant.id(), claim.shareAdmissionId())`; absent or returned admission ID/grant ID mismatch => Denied(ADMISSION_MISMATCH). Compare `grant.evidenceId()` exactly to verified `evidenceId`; mismatch => Denied(SCOPE_MISMATCH). No reader lookup by the grant or admission ID is substituted for the claim evidence ID.
2. Obtain `repository.now()` for authoritative current time. REVOKED => Denied(REVOKED); explicit EXPIRED status => Denied(EXPIRED). Otherwise, missing/inconsistent grant bounds means null `validFrom`, null `expiresAt`, or invalid ordering `validFrom >= expiresAt`, all Unavailable, including null bounds on accepted ACTIVE/LOCKED statuses; check these before dereferencing/comparing instants. With valid bounds, `now >= grant.expiresAt` => Denied(EXPIRED); `now < grant.validFrom` => Denied(NOT_YET_VALID). Only ACTIVE or LOCKED are accepted; any other status => Denied(CURRENT_STATE_DENIED). Do not apply maxAdmissions/admissionsUsed as a new admission test.
3. Load `findAdmissionWitness(grant.id(), admission.id())`. Absent => Denied(ADMISSION_MISMATCH). Require the exact PA18 `admission-granted:<grant UUID>:<admission UUID>` successful ADMISSION_GRANTED witness and exact verification mode across grant/admission/witness. Modes are exactly NONE, EXTRACTION_CODE, DESIGNATED_MOBILE and ORGANIZATION_MEMBER; NONE requires admission verificationResult NOT_REQUIRED, the other three require VERIFIED, as written by `DefaultShareAdmissionService`. Missing/unknown/malformed facts => Unavailable. `findAdmissionWitness` already filters event type/result; do not read or append authorization events to prove issuance again.
4. Copy PA18's canonical JSON hash algorithm locally into private helpers of the new adapter (sorted object keys recursively; array order retained; JsonMapper serialization; existing SHA-256 provider returning lowercase 64-hex). The exact six payload keys are `verificationMode`, `codeGeneration`, `revocationGeneration`, `grantExpiresAt`, `admissionExpiresAt`, `sessionExpiresAt`; ORGANIZATION_MEMBER adds exactly `recipientAccountId`, `recipientOrganizationId`. Require a JSON object, textual mode/time/recipient fields, integral int-convertible nonnegative generations, and hash equality. Parse time with `Instant.parse`, recipients with `UUID.fromString`; require recipient strings equal the parsed UUID's canonical `toString`, and semantic equality of every payload value to its corresponding typed witness field. Time text uses the existing PA18 `utc` representation (zero fractional digits for whole seconds, otherwise six microsecond digits), not a new time format or precision policy. Reject wrong/null types, invalid encodings, missing/nonpositive expiries or extra keys as Unavailable. Do not refactor/move PA18 helpers or accept a similar-looking hash. Keep JsonNode inside the transaction and out of returned results.
5. Require `admittedAt < sessionExpiresAt <= admissionExpiresAt <= witness.grantExpiresAt`, monotonic `witness.grantExpiresAt <= current grant.expiresAt`, `claim.issuedAt >= admittedAt`, `claim.notBefore >= grant.validFrom`, and `claim.expiresAt <= min(current grant expiry, witness.grantExpiresAt, witness.admissionExpiresAt, witness.sessionExpiresAt)`. Current grant expiry may be extended, never shortened: V009 permits an ACTIVE, not-yet-expired grant extension with `revision+1`. Extension never updates the frozen witness and never lengthens the existing admission/session or this claim/read cap. Broken witness order/expiry consistency (including witness grant expiry greater than current grant expiry) => Unavailable; credential exceeding otherwise valid frozen/current bounds => Denied(SCOPE_MISMATCH), even when the extended current grant would cover it. At `now >= witness.sessionExpiresAt` or `now >= witness.admissionExpiresAt` return Denied(EXPIRED). Check claim not-before/expiry against DB now as in §4. This consumer performs no extension, tolerance or policy lookup.
6. Compare all three sources of revocation/code generations: verified claim, current grant, admission witness. Any inequality => Denied(STALE_GENERATION). Both dimensions are mandatory; do not infer one from the other.
7. For ORGANIZATION_MEMBER, obtain recipient account/organization from the validated durable witness, require organization equals grant owner, and reread existing `readCurrentOrganization`, `readCurrentMembership`, `readCurrentAccount`; each exact ID relationship and ACTIVE status must hold. Missing/inactive/wrong-tenant facts => Denied(CURRENT_STATE_DENIED). A credential is the trusted playback authority here: no new customer bearer/caller resolver is accepted. This does not replace PA18's current-session reauthentication during issuance. For non-member modes require no recipient fields, per the frozen witness shape.
8. Reproduce PA18's current ownership/binding/risk predicates using `readSubjectOwnership`, `readCurrentBindings`, optional `readCapturedBinding`, and `readSubjectRisk`. Malformed identity-at-capture, absent/contradictory subject ownership => Unavailable. Require exact subject ID, at least one owner, organization owner equal to grant ownerOrganizationId, and for a personal grant ownerAccountId equal to createdByAccountId. Require exactly one selected binding (captured binding if present, otherwise the current selection), same subject, ACTIVE and verified, and membership in current active/verified bindings. Require a present nonempty risk result for that subject/selected binding with no OPEN rows. Also check returned RiskState subject/binding identities and supported row states (OPEN or CLEARED) rather than trusting a contradictory mock. Binding/risk denial => Denied(CURRENT_STATE_DENIED); malformed result/unknown state => Unavailable. No foreign Java module imports; existing V012-authorized repository reads only.
9. Take a final `repository.now()` and recheck grant/witness/claim exclusive expiry and not-before, returning Allowed(finalNow) only while valid. Commit/close the read transaction before returning. Commit/rollback failure => Unavailable. Never expire a grant, update revision/count, insert admission/event, read authorization replay keys, call issuance/ticket/policy/code/mobile providers, or lock lifecycle rows.

Read-only snapshot semantics are intentional: committed revoke/rotation/expiry/lifecycle/risk changes visible before the state snapshot deny the next attempt. There is no atomic DB-to-external-open transaction; a concurrent change after that snapshot may race this attempt. The final local expiry fence limits time crossing during evidence lookup, not revocation propagation. No zero-race/continuous recall guarantee or Q22 revocation window is asserted. Evidence identity/media facts are immutable; the handle cannot expand scope. This is the retained foundation, not a new revocation system.

## 6. One implementation slice and unconditional file allowlist

**Disposition: one behaviorally verifiable slice**, “verified credential → current authority → exact evidence object → bounded controlled open”. Splitting verification/resolution/open would create intermediate scaffolding with no complete accepted success signal; there is no unavoidable separation. Its only completion signal is §1's binding signal, exercised by §7. No future slice is added.

The following is the complete implementation write set, not permission to write it during this plan gate. Paths are exact and repo-relative. NEW means absent in the measured tree; MODIFY means present. No wildcard expansion or conditional follow-up file is authorized. Nested helpers/fixture classes stay in their listed owner file. The plan artifact itself is frozen input to implementation, not a moving scope escape hatch.

| Action | Exact path | Required change |
| --- | --- | --- |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/OpaquePlaybackCredential.java` | Redacted opaque value. |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/VerifiedMediaClaim.java` | Claim and nested duration window. |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaCredentialVerifier.java` | Trusted injected verifier and sealed outcomes. |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/PlaybackStateReader.java` | Typed state check and sealed outcomes. |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/SharingPlaybackStateReader.java` | Read-only authority adapter and private witness helpers. |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/ControlledMediaReader.java` | Typed scope, bounded handle and open outcomes; no bytes/location. |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackService.java` | Ordered orchestration and typed safe outcomes/reasons. |
| NEW | `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackConfiguration.java` | Fail-closed defaults and existing dependency wiring. |
| MODIFY | `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionRepository.java` | Exactly one read method. |
| MODIFY | `backend/src/main/java/com/onecall/core/sharing/internal/admission/JooqShareAdmissionRepository.java` | Non-locking principal-checked lookup. |
| MODIFY | `backend/src/main/java/com/onecall/core/sharing/package-info.java` | Exact allowedDependencies becomes `{"evidence::share-projection", "evidence::media-read"}`; no bare evidence/internal access. |
| NEW | `backend/src/test/java/com/onecall/archtests/sharing/MediaPlaybackServiceTests.java` | 20 ordinary test methods below. |
| NEW | `backend/src/test/java/com/onecall/archtests/sharing/SharingPlaybackStateReaderTests.java` | 8 ordinary test methods below. |
| NEW | `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` | 4 ordinary test methods below. |
| MODIFY | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | Update only sharing's expected named dependency map; retain 14 modules, two module-level edges and all nine existing methods/negative fixtures. |
| MODIFY | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | Add exact eight-file PLAYBACK_FILES partition, exclude it from commandPaths; preserve all old checks and add two methods below. |
| NEW | `backend/src/test/java/com/onecall/archtests/container/MediaPlaybackIT.java` | Six methods; self-contained nested fixture and qualified pool/query tracing. |
| MODIFY | `backend/pom.xml` | Add only `**/MediaPlaybackIT.java` to container-smoke Failsafe includes (11 → 12). No dependencies/plugins/version changes. |
| MODIFY | `.github/workflows/ci.yml` | Exact new class/method sets, counts and arithmetic in §8; preserve unrelated dependency/runtime/contract gates. |
| NEW | `docs/gateflow/pa19-sharing-media-gateway-playback-foundation-implementation-report.md` | Completion report specified in §9. |
| MODIFY | `docs/gateflow/project-execution-control.md` | Controller-owned implementation checkpoint/evidence update only, after authorized implementation; no unearned review/merge/ready status. |

Total implementation allowlist: **21 files = 8 new main + 3 existing main + 4 new tests + 2 existing tests + POM + CI + report + Control**. The original plan gate authorized only this plan; the current fix gate authorizes only this plan and `docs/gateflow/pa19-sharing-media-gateway-playback-foundation-plan-fix-1.md`. All 21 implementation writes remain deferred. No codegen, private helper file, internal GPT writer or generated migration/source is required.

Inventory verification: measured tree has 127 main Java files and 60 test Java files (including non-test fixtures/package-info). The eight new main paths, four new test paths and report path are absent. All MODIFY paths exist. The only repository implementation is Jooq; source partitions specifically explain the otherwise-easy-to-miss SharingQueryBoundaryTests ripple. Playback's new package is excluded from its legacy command partition and tested in its own exact partition, not globally exempted from safety checks. Named sharing interfaces remain exactly customer-query/customer-command/public-query/public-admission. Module-level discovered edges remain customer_iam → sharing → evidence. There is no PA19A API/configuration/test edit or additional sharing projection access.

## 7. Exact new test methods and behavior

Use ordinary `@Test void` methods, no parameterized/repeated/dynamic/disabled/conditional tests. Loop case matrices inside these methods. Keep all 320 existing test methods, including all 21 ShareAdmissionServiceTests, 37 PublicShareAdmissionIT, eight EvidenceMediaReadIT and eight EvidenceMediaReadRoleLeastPrivilegeIT methods, unchanged in behavior. Existing architecture methods may receive the narrowly specified map/partition updates, not weakened constraints.

### MediaPlaybackServiceTests — 20 methods

1. `validPlaybackResolvesVerifiedEvidenceAndOpensExactlyBoundedHandle` — exact ordering, sole evidence key and all scope fields.
2. `seekAndRenewRequireExactPurposeAndNeverIssueOrExtendCredentials` — both valid purposes and all mismatches; trusted internal expectedPurpose is not a wire input; RENEW retains the exact verified scope and never broadens/extends authority.
3. `malformedOpaqueCredentialStopsBeforeVerifier` — null wrapper/value, blank, NUL, UTF-8 error, C10 length; no normalization.
4. `invalidUnavailableNullAndThrowingVerifierFailClosed` — exact typed outcomes, canary exception discarded.
5. `malformedVerifiedClaimStopsBeforeCurrentState` — null claim/IDs/purpose/time/window, negative generations, nonpositive duration, contradictory time/window shapes.
6. `audienceIsExactAndCannotBeReplacedByTrustedNetworkContext` — case/space/suffix/other-domain/empty audience; null shape precedence.
7. `purposeMismatchPrecedesTimeAndStateChecks` — intended null/enum mismatch and conflicting failures.
8. `exclusiveExpiryAndNotBeforeBoundariesAreDeterministic` — exact equality boundaries, future issuedAt, no skew.
9. `invalidDurationAndReadWindowsAreNeverClamped` — negative/empty/inverted/overflow-related bounds, end beyond duration.
10. `stateDenialsUnavailableAndMalformedResultsPreventEvidenceLookup` — every explicit state Denied reason is propagated; missing/throwing/null state result and Allowed with null checkedAt, checkedAt before issuedAt/notBefore, or checkedAt at/after expiresAt are exactly Unavailable, with evidence/open=0; valid interval endpoints remain pinned.
11. `crossShareAdmissionAndEvidenceStateCannotReachControlledOpen` — wire real state adapter with mocked repository to prove tuple mismatches, not merely mocked Denied.
12. `missingEvidenceDeniesWithoutControlledOpen` — Missing is distinct from Unavailable.
13. `unavailableThrowingAndAmbiguousEvidenceNeverFallsBack` — Unavailable/null/throw, no second resolver/projection call; ambiguity is tested by actual reader in integration.
14. `resolvedObjectIdentityAndDurationMustMatchVerifiedScope` — media UUID/duration disagreement and absent required duration.
15. `evidenceMetadataCannotBeOverriddenByCallerOrClaim` — method/type reflection excludes client object inputs; Found hash/size/MIME retained and window bounded; defensive constructor failures are safe.
16. `readerUnavailableNullAndThrowingResultsReturnNoHandle` — at most one open; safe default/error results.
17. `readerCannotWidenOrMisbindReturnedHandle` — wrong object/hash/size/MIME/duration/purpose/window/expiry/null result; no returned success/retry.
18. `expiryCrossingDuringResolutionPreventsOpen` — advancing deterministic clock and no grace period.
19. `ambientTransactionsAndPostOpenDependencyIoAreForbidden` — no calls on ambient transaction; transaction closure assertions at verifier/evidence/open; open marks dependency spies sealed against subsequent access.
20. `credentialsClaimsScopesHandlesOutcomesAndFailuresAreRedacted` — internal successful handle retains exact requested object UUID/hash/size/MIME/duration and purpose/window/expiry; independently assert constant-redacted toString/root/nested Jackson and absence from log/exception capture with credential/UUID/location/URL/exception canaries; fixed safe outcomes, not structural removal of scope.

### SharingPlaybackStateReaderTests — 8 methods

1. `activeAndAdmittedLockedStateAllowWithoutMutation` — both statuses, no count/max-admission reenforcement, valid modes/results; verify zero write/lock/issuer/event methods.
2. `grantAdmissionAndEvidenceTupleMustMatchVerifiedIdentity` — absent/cross IDs and lookup argument exactness.
3. `witnessShapeHashTypesAndBoundsMustBeCanonical` — all witness keys/types/hash/recipients/event/mode/order/claim upper bounds and §5's null/invalid grant bounds => Unavailable; match PA18 canonicalization without changing PA18. Inside this method, model an ACTIVE post-admission grant extension with `revision+1`: unchanged witness and admission/session bounds permit Allowed only while checkedAt/claim expiry remain within all frozen/current bounds; expiry equal to the frozen cap is valid only before that exclusive time, while a claim expiring beyond any frozen bound is Denied(SCOPE_MISMATCH), even within the extended grant. At the frozen session/admission expiry deny EXPIRED; shortening/inconsistent witness-current order is Unavailable. Assert no new admission/event/count or reader-side revision mutation.
4. `revokedExpiredNotYetValidAndBothStaleGenerationsDeny` — deterministic precedence, DB-now equality, all three generation sources, final time fence.
5. `organizationMemberWitnessRequiresCurrentAccountTenantAndMembership` — exact IDs/ACTIVE states; no new customer session input; non-member recipient prohibition.
6. `ownershipCapturedBindingAndCurrentRiskFailClosed` — absent/ambiguous/contradictory ownership/bindings, current selection, empty/OPEN/unknown risk and identity mismatch.
7. `readOnlyRepeatableSnapshotClosesBeforeReturningAndOriginalTemplateIsUnchanged` — original settings untouched, transaction flags/isolation/propagation, no lifecycle locks or mutations, deterministic transaction tracing.
8. `repositoryHashAndTransactionFaultsAreUnavailableAndRedacted` — exceptions/nulls/commit failure/bad hash provider, no partial Allowed or sensitive cause.

### MediaPlaybackConfigurationTests — 4 methods

1. `defaultAndWebProfilesProvideOnlyUnavailablePlaybackProviders` — no database/network access; no state bean without opt-in; service-local unavailable verifier/reader defaults, no fallback provider beans registered; service fails closed.
2. `persistenceStateRequiresExplicitAdmissionEnablementAndQualifiedResources` — missing/false/true admission flag, correct qualifiers/template copy; no default datasource.
3. `explicitVerifierEvidenceStateAndReaderAreUsedWithoutReplacement` — loop both explicit-provider/configuration registration orders with bean overriding disabled; one trusted bean of each dependency type is used exactly, no fallback bean/shadowing or default invocation. Loop duplicate candidates for each of the four types, including a primary-marked candidate: configuration fails with a fixed safe message, never arbitrary selection. All fixture configurations/helpers remain nested in this test file; no new test method or imports resource.
4. `missingEvidenceOrProvidersFailClosedWithoutProductionAdapters` — missing evidence/state yields Unavailable, no fake provider auto-registration or location/config disclosure.

### SharingQueryBoundaryTests — two additional methods

- `playbackUsesOnlyNamedEvidenceReadAndPrivateTypedScope` — exact eight playback files/signatures/nested sealed variants; no new named interface; allowed evidence API imports only, no evidence internals, caller DSL, bytes, raw location, HTTP, foreign Java state access, provider framework or logging. Reflection tests the sole public service method shape and complete handle/scope components.
- `playbackStateAndOpenPreserveReadOnlyNoProjectionBoundary` — enforce no DML/locking/provider issuance/event code in the new path, no sharing projection reuse or broadened SQL; exact grant-by-ID bound query/principal check and no legacy preliminary-lookup change; preserve all legacy source deny-lists. Real transaction behavior remains integration-tested, not claimed from string checks.

### MediaPlaybackIT — six methods, PostgreSQL 16, container-smoke only

1. `qualifiedAdmissionAndEvidenceRolesResolveOneObjectAndOpenOutsideTransactions` — full real state repository + real evidence reader with fake verifier/controlled reader; current_user=session_user for each dedicated login, REPEATABLE_READ/read_only sharing transaction, evidence read_only transaction; both complete before open; sole bound evidence key and 15-column query; ACTIVE and LOCKED admitted cases.
2. `committedRevocationRotationAndAdmissionMismatchDenyBeforeEvidence` — separate admin/owner connection commits lifecycle changes before next attempt; prove both generation dimensions, cross-share/admission and no read or writes.
3. `witnessExpiryMembershipBindingAndRiskChangesFailClosedWithoutWrites` — expiry equality with fixture times, malformed witness fixture facts where schema permits, membership/account/organization changes, ownership/binding/risk denials; immutable DB guards are not disabled. Inside this method, create a real admission/witness, then commit a legitimate ACTIVE/unexpired grant `expires_at` increase with `revision=revision+1` via the owner/admin fixture connection (as in PublicShareAdmissionIT); keep generations unchanged. Real state reader returns Allowed and orchestration opens once only within frozen witness grant/admission/session bounds; a trusted test claim beyond a frozen bound but inside current extended expiry yields Denied(SCOPE_MISMATCH), evidence/open=0. Exercise the frozen exclusive expiry denial with fixture time as supported, without mutating immutable admissions/witnesses. Snapshot witness/payload/hash, admission/session values, admission/event rows and counts before extension and after both attempts: all unchanged; only the fixture extension increments grant revision once, and playback adds no revision/count/admission/event mutation. No sleeps, extra admission/issuance or new test method.
4. `missingAmbiguousAndInconsistentEvidenceNeverOpensControlledRead` — real Missing, duplicate ORIGINAL, wrong recording/media relationship/status, required-duration failure where supported; PA19A Unavailable honored. Capture zero open calls.
5. `readFailuresPreserveAdmissionRevisionCountsAndAccessEvents` — broken actual admission/evidence login or connection, fake reader failure; state table/event snapshots before/after all outcomes, no incidental expiry mutation; safe pool/transaction cleanup.
6. `typedOpenHasNoPostOpenDatabaseIoAndNeverDisclosesPrivateFacts` — mark open then forbid further pool checkouts/SQL/verifier calls; JSON/string/log canaries; admission role still denied media/artifact/recording columns and evidence role denied location columns and sharing tables, using existing privileges, no SET ROLE escalation.

Integration fixture is nested in MediaPlaybackIT: reuse the existing package-visible `SharingQueryFixture` start/reset/admin/execute methods for PostgreSQL 16 and V001–V013 migration setup, without modifying it. Recreate only necessary seed/pool helpers locally from PublicShareAdmissionIT's private fixture and EvidenceMediaReadIT's nested fixture patterns; do not change their access modifiers. Seed legitimate subject/account/binding/risk/evidence/media/recording/artifact/grant facts in allowed dependency order, then use the unchanged DefaultShareAdmissionService with deterministic test providers to establish the real admission and canonical witness. Build a trusted test claim from those server-owned facts, not from a new wire format. Configure passwords only in test PostgreSQL for the two existing dedicated login roles, use their actual logins/qualified pools, and close/reset fixture resources. Do not weaken SQL guards, mutate immutable admissions, grant privileges, share a DSL across owners, or introduce a test role/schema/migration. Existing fixtures already migrate exactly 13 versions. All additional helper classes remain nested; no test resource or existing fixture file edit is needed.

## 8. Measured baseline, exact arithmetic and validation

Plan-time measurement was source inspection, not test execution. Regex `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(` over all test Java files gives **22 Surefire classes / 128 methods + 11 Failsafe classes / 192 methods = 33 classes / 320 methods**. AST-reading the literal `unit_expected`/`container_expected` assignments, subscript additions and `.update(...)` in `.github/workflows/ci.yml` confirms exact source=CI method sets for every class. POM has exactly 11 Failsafe includes. No local Surefire/Failsafe XML reports exist here. PA19A final closeout records the same `22/128 + 11/192 = 33/320` with zero failures/errors/skips under PostgreSQL 16.15; that is inherited recorded XML/run evidence, **not a new run or fresh source=CI=XML assertion**. The superseded plan's 119/176 totals are not the current baseline.

| Existing Surefire class (simple name) | Methods |
| --- | ---: |
| CustomerMeWebTests | 8 |
| CustomerSharesCommandWebTests | 4 |
| CustomerSharesWebTests | 10 |
| CustomerWebConfigurationTests | 6 |
| EvidenceMediaReadPersistenceConfigurationTests | 4 |
| OneCallApplicationTests | 1 |
| PublicSharePersistenceConfigurationTests | 2 |
| PublicShareWebTests | 12 |
| SharingCommandPersistenceConfigurationTests | 2 |
| SharingPersistenceConfigurationTests | 2 |
| BootstrapDependencyBoundaryTests | 1 |
| CustomerApiBoundaryTests | 3 |
| ModuleBoundaryTests | 9 |
| SharingQueryBoundaryTests | 2 |
| EvidenceMediaObjectReaderTests | 4 |
| CustomerShareCommandServiceTests | 6 |
| PublicShareQueryServiceTests | 4 |
| ShareAdmissionServiceTests | 21 |
| CustomerContextServiceTests | 6 |
| CustomerSessionBoundaryTests | 7 |
| CustomerShareCursorCodecTests | 6 |
| CustomerShareQueryServiceTests | 8 |

Failsafe baseline class arithmetic is PostgresContainerIT 64 + SharingQueryServiceIT 10 + CustomerMeIT 10 + CustomerSharesIT 8 + SharingRoleLeastPrivilegeIT 12 + CustomerShareCommandsIT 18 + PublicSharesIT 8 + PublicShareAdmissionIT 37 + SharingAdmissionRoleLeastPrivilegeIT 9 + EvidenceMediaReadRoleLeastPrivilegeIT 8 + EvidenceMediaReadIT 8 = **192**.

Exact final target:

- Surefire: `128 + 20 + 8 + 4 + 2 = 162`; classes `22 + 3 = 25`. SharingQueryBoundaryTests becomes 4, ModuleBoundaryTests remains 9, every other existing class count stays fixed.
- Failsafe: `192 + 6 = 198`; classes/includes `11 + 1 = 12`.
- Combined: `320 + 34 + 6 = 360`; classes `33 + 4 = 37`; target fresh **source=CI=XML is 25/162 + 12/198 = 37/360**, with zero failures/errors/skips.
- Word-boundary comment-inclusive line scan currently 321, target 361; the sole non-method hit stays the existing PostgresContainerIT comment. Add no literal annotation-like comments. Actual methods are 320 → 360, not the line-scan total.
- Existing CI's historical PA18 subtraction has additions `[9,6,1,13,1] = 16 unit + 14 integration`. Its current synthetic baseline is `112+178=290` (including PA19A), line scan 291. Preserve these historical facts: add a separate exact `pa19_additions` dictionary for the three new unit classes, two SharingQueryBoundaryTests methods and six MediaPlaybackIT methods; subtract **both** PA18 and PA19 additions for the old 112/178 and 291 assertions. Add a separate PA19-start subtraction of only PA19 additions yielding 128/192=320 and 321 lines. Final raw totals are 162/198=360 and 361 lines. Do not mistakenly change the old 112/178 to 146/184 and relabel it historical evidence.

CI changes are unconditional and exact: add the three fully-qualified unit classes and all 32 method names from §7; union the two new names into `com.onecall.archtests.architecture.SharingQueryBoundaryTests`; add `com.onecall.archtests.container.MediaPlaybackIT` with its six names. Update the early Surefire 128 aggregate check/error to 162; `method_totals` to `[162,198]`, combined 360; class counts to 25/12; include count to 12; both `exact_inventory` calls to 162/198 and the final print text to PA19/current/historical arithmetic above. Maintain exact source-class equality, no duplicate XML method pairs, zero skips/errors/failures, rejection of unsupported test annotations, all old expected sets and the 64-method PostgresContainerIT checks. Do not touch unrelated workflow jobs, dependency firewalls or contract validators.

### Commands for the subsequently authorized implementation gate

Prerequisites: existing project Java/Maven toolchain and Docker-accessible PostgreSQL 16 container execution as used by CI; no production cloud or signer credentials. Do not run these in the present plan-only task. Use the platform's working git executable/path mapping rather than modifying `.git` metadata.

```bash
cd backend
mvn -q -Dtest=MediaPlaybackServiceTests,SharingPlaybackStateReaderTests,MediaPlaybackConfigurationTests,SharingQueryBoundaryTests,ModuleBoundaryTests test
mvn -q -Pcontainer-smoke -Dit.test=MediaPlaybackIT verify
mvn -q clean test
mvn -q -Pcontainer-smoke verify
cd ..
git diff --check
git diff fc3bd7639b97aa1301184a9aa8b4e25676766e99 -- api/openapi/onecall-v1.openapi.yaml backend/src/main/resources/db/migration
```

Focused unit command: 20+8+4+4+9 = 45 methods. Focused container invocation must use `-Pcontainer-smoke`; it runs the normal unit lifecycle plus six selected Failsafe methods, not the full 198-method claim. Run the final clean unit/full container pair before parsing reports, to remove stale focused XML. Full targets are 162 Surefire and 198 Failsafe, not inferred from exit code alone.

Execute the existing CI exact-inventory Python heredoc (the block containing `exact_inventory`, beginning with `import glob`) unchanged from the updated workflow against those fresh reports from `backend`, as CI does. This is a verification command, not code generation: extract that literal block and pass it directly to Python, or use the platform CI job. It must prove full class-name/method-pair source=CI=XML equality, counts, no unsupported tests and zero skipped/failed/error reports. A full normal CI run additionally preserves its existing boot/package/dependency/contract gates. Capture actual command output/environment and report paths. Missing Docker/toolchain/XML is a reported validation limitation, never a claimed pass or test weakening.

Exact local invocation of that inventory block, from the repository root:

```bash
cd backend
python3 - <<'PY'
from pathlib import Path
import textwrap
lines = Path('../.github/workflows/ci.yml').read_text().splitlines()
start = next(index for index, line in enumerate(lines) if line.strip() == 'import glob')
end = next(index for index in range(start, len(lines)) if lines[index].strip() == 'PY')
exec(compile(textwrap.dedent('\n'.join(lines[start:end])), 'ci-exact-inventory', 'exec'))
PY
cd ..
```

Prove immutable bytes with the clean diff command above and compare every V001–V013 working-file SHA256 to this measured starting-worktree manifest; ensure no V014/new schema/role exists. This Windows checkout has inherited CRLF working files while repository blobs use LF: raw `git show HEAD:<path>` is not the raw working-byte baseline. Do not normalize migration files to make those different representations match. Independently preserve the canonical repository bytes with the empty migration diff and, when comparing blobs, the existing Git clean filter. No migration file is in the write set.

| Version (existing filename from inventory) | Starting raw working-file SHA256 |
| --- | --- |
| V001 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| V002 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| V003 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| V004 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| V005 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| V006 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| V007 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| V008 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| V009 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| V010 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |
| V011 | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` |
| V012 | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` |
| V013 | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` |

On a different clean checkout, preserve that checkout's own before/after raw manifest and the same canonical repository blobs, rather than rewriting its line endings to this worktree's representation. Verify the exact implementation file allowlist separately; the POM diff must contain only the new Failsafe include, OpenAPI/frontend/PA19A sources and tests must remain unchanged.

## 9. Docs decision, completion report and residual ownership

No design-spec/OpenAPI/README/historical review/plan edit is needed: docs/12 and docs/14/17 already state the retained ownership and security boundaries. This plan is the precise named-interface-map refinement; the implementation report and Control checkpoint record actual execution. The only implementation docs writes are the two exact paths in §6. No completion report is created at this plan gate.

Implementation report format: (1) task/branch/base and one-slice disposition; (2) 21-file allowlist actual added/modified paths and no-drift comparison; (3) contract/outcome/ordering evidence mapped to the named tests, distinguishing injected seam success from real byte delivery; (4) commands/toolchain/PostgreSQL version/report paths and actual fresh 25/162+12/198=37/360 source=CI=XML arithmetic with failures/errors/skips, or explicit limitations; (5) V001–V013/OpenAPI byte identity and POM-only include change; (6) default-unavailable configuration/redaction results; (7) residual owner/destination table and no blocking unresolved question, or an explicit stop if evidence contradicts this plan. Control updates only verifiable checkpoint facts and the proper next Controller gate; do not mark review/PR/merge approval or readiness on implementation evidence alone.

| Risk / residual | Disposition, owner and destination |
| --- | --- |
| Old evidence-owned runtime ACL blocker | Closed as a planning prerequisite by merged PA19A `evidence::media-read` and dedicated configuration/V013. Evidence + DB/Security own the existing seam; retain its tests, do not reopen by broadening sharing ACLs. Runtime deployment must still supply its explicit approved credentials. |
| Real credential provider, signatures, key rotation, wire/enrichment and issued-claim provenance | Security + Platform + Sharing provider integration; existing C10 provider integration ownership. Production mediaObjectId/duration/window must come from the authenticated issuance-time claim, independently of consumption-time evidence resolution, so SCOPE_MISMATCH is not a self-referential/vacuous comparison; defining that provenance belongs to the future credential-provider plan, not a wire design here. A cross-module verifier provider must first receive an approved allowedDependencies/named-interface revision under PA0-R-09, with exact ModuleBoundaryTests/SharingQueryBoundaryTests map review, and resolve docs/12 adapter placement (also required for an in-sharing provider). An evidence→sharing bean would additionally create a cycle against sharing→evidence and is not pre-authorized. This slice adds no provider named interface/module edge and does not widen the already-pinned evidence::media-read refinement. Default verifier remains Unavailable. No PA18 issuance format or event requirement is changed to simulate this. |
| Q22 TTL, skew, revocation propagation and snapshot-to-open race | Security + Sharing + Platform; Q22 security policy ownership, explicitly including local service Clock versus database repository.now()/clock_timestamp() skew and provider-claim/admission-time comparisons. Exact internal bounds/exclusive expiry are implemented; no numeric skew/tolerance/TTL policy, continuous read recall, locks around external I/O or zero-race guarantee. |
| Gateway deployment, player contract, origin/path, CDN/LB, HTTP range/streaming and actual byte delivery | Infrastructure + Security; `R-A3-MEDIA-GATEWAY-TRANSPORT`. Typed handle/open representation is fully decided for this slice; no transport decision blocks it. |
| Actual COS/KMS/Secret Manager access and private location resolution inside a production provider | Platform + Security + Evidence provider integration; destination is the future controlled-reader provider plan. A cross-module reader provider must first receive an approved allowedDependencies/named-interface revision under PA0-R-09, with exact ModuleBoundaryTests/SharingQueryBoundaryTests map review, and resolve docs/12 adapter placement (also required for an in-sharing provider). An evidence→sharing bean would additionally create a cycle against sharing→evidence and is not pre-authorized. This slice adds no provider named interface/module edge and does not widen the already-pinned evidence::media-read refinement. Existing evidence reader intentionally does not expose bucket/key/version. Default controlled reader remains Unavailable; no broader DB access or cloud capability is inferred. |
| End-to-end edge/collector/analytics redaction | Platform + Security + Observability; `R-A3-REDACTION-RUNTIME`. Local values/serialization/log absence are tested; no claim about deployed external systems. |
| Optional duration and ambiguous/inconsistent evidence | Evidence owns fact integrity through existing reader/API and PA19A tests; this consumer fails closed as specified, without remediation/migration work or guessed metadata. |
| Missing execution prerequisites or unexpected inventory/architecture contradiction | Backend implementer reports to the Controller in the exact implementation report/Control checkpoint. Stop and return for scope correction if a new file/privilege/API is actually required; never silently expand the allowlist or weaken a test. This is a contingency, not a present unresolved design question. |

**No blocking open question remains for the retained internal goal.** Trusted providers and real deployment credentials are deliberately absent implementations, not hidden readiness claims. Their safe default is specified and testable. No residual authorizes additional work here.

## 10. No-over-design/no-drift and plan artifact validation

Eight small private source files, one additional existing-repository SELECT and one existing named-interface permission are sufficient. No new module, schema, role, wire contract, framework, numeric policy, generic storage abstraction, byte cache, cache invalidation mechanism, provider registry or future transport scaffold is justified. One integrated slice exercises the complete accepted path. Existing projection non-disclosure and PA18/PA19A tests remain authoritative; the two architecture inventory edits make the precise addition visible rather than bypassing those boundaries.

Plan-time validation is limited to this document and read-only evidence/inventory measurement. Initial mapped Git status contained an existing untracked `.codex` entry; preserve it without opening/staging/cleaning it. The checkout's `.git` points to a Windows `D:/...` path, so Linux inspection uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call21` and `GIT_WORK_TREE` set to this checkout, without editing metadata. Verify final tracked diff is empty and the only newly created task artifact is this plan (in addition to the preserved pre-existing `.codex` entry). Run `git diff --no-index --check /dev/null docs/gateflow/pa19-sharing-media-gateway-playback-foundation-plan.md` for the untracked plan, plus path-scoped `git diff --check -- <plan-path>`; record `wc -l -c` and `sha256sum` of the final artifact outside the artifact itself to avoid a self-hash. Do not stage, commit, push, start review/implementation or take external actions.

The preceding paragraph records the original plan gate. For fix 1, the pre-existing modified Control, both imported reviews and `.codex` marker are preserved unchanged. The only permitted delta is this plan plus the new fix-1 artifact named in §6. Validate whitespace only on those two paths, compare a before/after repository file-hash inventory and the exact §6 allowlist/method-name sets against the input plan, and record both final line/byte/SHA256 identities outside their own artifacts (the fix artifact may record the final plan identity). No runtime tests, implementation or A/B re-review is performed at this fix gate.
