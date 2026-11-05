# PA14 Customer Extraction-Code Rotation — Bounded Plan

- Task label: `pa14-sharing-code-rotation-plan-gpt-20261105-174254`.
- Work unit: `PA14-sharing-command-code-rotation`; Planflow/Gateflow gate: `plan` only; date: 2026-11-05 (Asia/Shanghai).
- Slice count: **one**, the accepted PA14 full plan's final end-to-end rotation slice. This document is not implementation authorization or gate acceptance.
- Implementation baseline/merge base: `d9cc5bd22f6eda0523f989348f66b09137087fc0` (merged revoke PR #23). Inspected HEAD: `27db41aee78d16b45ef26992b1fbf8afd3a5eec6`; its only committed difference from that merge is `docs/gateflow/project-execution-control.md`. Backend, API and CI implementation content is identical to the merge.
- This gate's sole writable artifact is `docs/gateflow/pa14-sharing-command-code-rotation-plan.md`. All implementation edits described below are future work after the appropriate gate decision.

## 1. Authority, Goal And Non-Goals

Read authorities: `docs/12-technology-stack-and-system-architecture.md`; the active code-rotation goal confirmation in `docs/gateflow/project-execution-control.md`; accepted `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`; `api/openapi/onecall-v1.openapi.yaml` operation `rotateCustomerShareCode` and its request/closed response schemas; `docs/14-production-data-model-and-flyway-schema-spec.md` §§2.4, 2.5, 2.12, 4.44–4.46, 7.4, 9.0–9.0.2, 9.29, 10.21, 10.23, 10.24; V009/V010; current command, web, persistence, provider, tests, POM, CI and README. The accepted full plan supplies the implementation boundary and exact identities/facts; the active goal narrows it to rotation only. No authority is edited here.

Implement only `POST /api/v1/customer/shares/{shareId}/code-rotations`. Only a new `EXTRACTION_CODE` grant action with source `LOCKED` may become `ACTIVE`. Preserve create, revoke, share-list, `/me`, context-path/firewall protections, the query service and read-only `sharing_role` transaction. New success returns the generated extraction code exactly once, never a capability token; exact replay returns only persisted result metadata.

No capability-token regeneration, non-code unlock, ACTIVE-to-ACTIVE rotation, expiry extension, other transition, public admission/ticket/media, Redis/COS/KMS/SMS, frontend, schema, V001–V010, OpenAPI or design-document changes. No production policy, KDF, code-generation parameter, TTL, key, provider, provisioning, deployment or enablement value is selected. Do not implement authentication, create or revoke again. No merge, approval, ready action, reviewer request, external comment or issue mutation.

## 2. Merged-Code Findings And Exact Future Allowlist

Current `CustomerShareCommand` permits Create/Revoke only; result variants are Created/CreationReplayed/Revoked. Filter/controller/binder/DTOs admit create/revoke but not rotation. `SharingCommandRepository` already declares the full accepted transition signature and nested persisted-transition/fact/result records. `JooqSharingCommandRepository.findTransition` decodes only REVOKE/REVOKED; `transitionGrant` rejects replacement/code-witness shapes. Role, lock, active-context, owner-filtered grant lock, clock, append, rollback and error-classifier seams already exist. Seven provider files exist; `ShareRotationPolicyProvider` is the sole missing eighth file. Existing deterministic codes use an invariant salt, which must become generation-distinct for rotation without changing generation-one create behavior. POM already includes `CustomerShareCommandsIT`; YAML already has disabled-by-default command configuration. Neither needs another edit.

Future implementation allowlist is exactly the following **20 paths**. All exist except the explicitly NEW provider. Directory abbreviations are literal repository-relative prefixes, not permission to edit other files:

- `M = backend/src/main/java/com/onecall/core/`
- `T = backend/src/test/java/com/onecall/archtests/`

| # | Path, relative to prefix where shown | Bounded change |
| --- | --- | --- |
| 1 | M `customer_iam/internal/web/CustomerBoundaryFilter.java` | Admit the one rotation POST and reuse transition header/media checks. |
| 2 | M `customer_iam/internal/web/CustomerSharesController.java` | Add `rotateCustomerShareCode`, trusted caller/trace conversion and closed response mapping. |
| 3 | M `customer_iam/internal/web/CustomerShareCommandDtos.java` | Three nested rotation DTOs only. |
| 4 | M `customer_iam/internal/web/CustomerShareCommandJsonBinder.java` | Strict `rotate(byte[] body)` binding only. |
| 5 | M `sharing/internal/command/CustomerShareCommand.java` | Nested Rotate record and sealed permits entry. |
| 6 | M `sharing/internal/command/CustomerShareCommandResult.java` | Nested Rotated/RotationReplayed records and permits entries. |
| 7 | M `sharing/internal/commandservice/DefaultCustomerShareCommandService.java` | Rotation dispatch/validation, policy dependency and the ordered command path; reuse existing helpers. |
| 8 | M `sharing/internal/persistence/JooqSharingCommandRepository.java` | Strict rotation replay decoding and one rotation UPDATE branch; preserve revoke branch. |
| 9 | M `sharing/internal/persistence/SharingCommandPersistenceConfiguration.java` | Qualified rotation policy injection plus missing-bean fail-closed lambda. |
| 10 | M `sharing/internal/provider/ShareRotationPolicyProvider.java` **NEW** | Accepted internal interface, nested request and decision only. |
| 11 | T `sharing/CustomerShareCommandServiceTests.java` | One new named Surefire method; adjust existing fixture constructor wiring. |
| 12 | T `sharing/DeterministicShareCommandProviders.java` | Test-only rotation ALLOW provider and generation-distinct replacement material. |
| 13 | T `container/CustomerShareCommandsIT.java` | Five new named IT methods, helper/fixture wiring and instrumentation. |
| 14 | T `CustomerSharesCommandWebTests.java` | Extend existing four methods, no new methods. |
| 15 | T `CustomerSharesWebTests.java` | Extend existing route/filter method, no new methods. |
| 16 | T `SharingCommandPersistenceConfigurationTests.java` | Extend existing two methods for policy default/override and no-bean conditions. |
| 17 | T `CustomerWebConfigurationTests.java` | Extend existing qualifier-graph method for the added policy dependency. |
| 18 | T `architecture/SharingQueryBoundaryTests.java` | Add the eighth provider to the exact command-tree inventory and assert its internal contract. |
| 19 | `.github/workflows/ci.yml` | Exact source/CI/XML inventory expectations, totals and inventory message only. |
| 20 | `backend/README.md` | Bounded current totals, admitted behavior and history clauses described in §9. |

The full plan permits these paths; this narrower slice does not use its other permissions. In particular, leave `SharingCommandRepository.java`, `CustomerShareCommandService.java`, `ShareCommandCaller.java`, `ShareCommandException.java`, `CustomerProblemWriter.java`, all existing provider contracts, package-info files, evidence/query code, properties/YAML, POM and other architecture tests unchanged. Their required APIs/mappings/boundaries already exist. No new adapter class, configuration file, test class or exported top-level command type. Sharing command-tree inventory goes 18→19 files (6 command, 1 commandservice, 4 persistence, 8 provider); customer-internal Java inventory stays 25.

## 3. Exact Transport And Error Contract

### 3.1 Boundary And Precedence

Add only POST with servlet-path shape `/api/v1/customer/shares/[^/;%]+/code-rotations`, retaining raw URI equality with context path plus servlet path. Preserve rejection of GET/other methods, aliases, encoded/matrix segments, duplicate/extra/trailing slash, extra segments and `/error` as sanitized 404. Keep highest-priority filtering and `/pa12` context-prefix fixtures. A route-shaped but blank, invalid UTF-8/NUL or over-128-code-point shareId fails 422 in controller validation before provider resolution/service/DB; syntactically valid missing/non-owned IDs instead reach owner-filtered lookup and conceal as 404. Treat shareId as opaque; do not parse it as UUID or impose the generated `share:` digest format.

Preserve existing precedence exactly: route rejection → trusted authentication → transition header → Content-Type → existing Accept negotiation → controller shareId validation → trusted selected-workspace shape → body binding → command-provider resolution → command execution. As in the existing revoke controller, blank/over-128 shareId validation precedes trusted selected-workspace shape validation; the mixed case of an invalid route-shaped shareId plus malformed trusted workspace returns HTTP 422 / VALIDATION_FAILED, before body binding or provider resolution. The authenticator supplies account/workspace; no client account, organization, role or trace selector is accepted. Missing/inactive DB caller context is a later command 403, not a web workspace-shape error. Invalid transition header plus malformed trusted selected-workspace shape stays 422; valid header/media and valid shareId plus malformed workspace shape stays 503. Do not move workspace validation ahead of the existing filter.

Enumerate `request.getHeaders("Idempotency-Key")` case-insensitively by header name: exactly one nonblank value, 1–128 Unicode code points, no NUL/unencodable UTF-8. Reject missing, empty/whitespace-only, >128, malformed or multiple values (including equal duplicates) as 422 before body/provider/DB. Preserve exact accepted string, including surrounding whitespace, Unicode, case and comma; no trim, normalize, case-fold, first-value fallback, comma-join or split. Hash exact UTF-8. The existing request attribute carries the accepted key. Create continues ignoring unrelated Idempotency-Key; revoke behavior is unchanged.

Require exactly one syntactically valid `application/json` Content-Type; valid parameters are accepted. Missing/malformed/non-JSON/wildcard/`application/*+json` returns 422, not 415, before raw-body binding. Preserve existing negotiation/buffered-error handling rather than adding new media behavior. Read body once in controller, after the filter.

### 3.2 Closed Binding And DTOs

Add nested `CodeRotationRequest(Integer expectedCodeGeneration)` and binder `rotate(byte[] body)`. Required body is an object: `{}` or exactly one optional `expectedCodeGeneration`. Omission is Java null; a supplied value must be an integral JSON int in [0, 2147483647]. Explicit null, negative, fractional, overflow, string, boolean, missing/empty/malformed/trailing JSON, scalar/array, duplicate member and any unknown member fail 422. Unknown includes `actionRequestId`, `reason`, `expectedRevocationGeneration`, client code/salt/hash/token, account/workspace/trace selectors. Reuse local strict-duplicate/trailing-token mapper; do not alter global JSON configuration.

Add the exact flat nested DTOs, using the existing DTO-local enums and no sharing imports:

| DTO | Exact required JSON/record fields |
| --- | --- |
| `ShareCodeRotationRotated` | `String result, String shareId, VerificationMode verificationMode, ShareStatus status, int codeGeneration, int revocationGeneration, String extractionCode` |
| `ShareCodeRotationReplayed` | `String result, String shareId, VerificationMode verificationMode, ShareStatus status, int codeGeneration, int revocationGeneration` |

Both return HTTP 200/application-json. First has `result="ROTATED"`, mode EXTRACTION_CODE, target ACTIVE, new counters, and the one-time non-null code. Replay has `result="REPLAYED"`, immutable grant shareId/mode plus persisted transition status/counters, never current status/counters. Frozen wire status enum permits ACTIVE|LOCKED; this operation's successful persisted target is ACTIVE, including replay after later locking/revoking/expiry. No capabilityToken, hash/salt, old code, evidence/timestamp/revision/state wrapper or nullable secret placeholder in either response; replay has no extractionCode property at all.

Controller supplies `Optional.of(CustomerBoundaryFilter.trace(request))`, converts trusted caller to existing `ShareCommandCaller(accountId, selected organization Optional)`, resolves existing qualified `ObjectProvider<CustomerShareCommandService>` only after binding, and maps BeansException/missing provider to 503. Retain constructor `(query ObjectProvider, command ObjectProvider, CustomerProblemWriter)`, qualifiers/order, existing exception handler and query mapping. Unexpected command result variant fails sanitized INTERNAL rather than serializing an internal record.

### 3.3 Closed Operation Errors

Reuse existing `CustomerProblemWriter` command adapter unchanged: `{type:"about:blank",title,status,code,traceId}`, existing no-store/security headers and trusted generated trace, no SQL/exception/credential/material echo.

| Condition | HTTP / code / title |
| --- | --- |
| Missing/invalid authentication | 401 / UNAUTHENTICATED / Authentication required |
| Ineligible active caller context, policy Denied or Allowed(DENY) | 403 / FORBIDDEN / Access denied |
| Valid opaque ID absent or outside account/selected workspace | 404 / NOT_FOUND / Not found |
| Changed action fingerprint; wrong status/mode, elapsed/equal expiry, stale code witness, bounded conditional miss | 409 / SHARE_TRANSITION_CONFLICT / Share transition conflict |
| Invalid header/path/body/media/direct input | 422 / VALIDATION_FAILED / Request validation failed |
| Existing gateway-owned mapping only; no rate limiter added | 429 / RATE_LIMITED / Too many requests |
| Missing/unavailable policy/material/service; role/lock/context-read/clock/replay-decode unavailability | 503 / SERVICE_UNAVAILABLE / Service unavailable |
| Unexpected append/commit/internal failure under existing classifier | 500 / INTERNAL / Internal error |

Rotation never emits SHARE_GENERATION_STALE, SHARE_REVOKED/410, SHARE_CREATION_CONFLICT, SHARE_RISK_BLOCKED or SHARE_ACCESS_POLICY_UNAVAILABLE. Revoke keeps its own stale/410 mappings and create its existing policy/risk mappings.

## 4. Command And Provider Types

Add nested `CustomerShareCommand.Rotate(String shareId, String actionRequestId, Optional<UUID> traceId, Integer expectedCodeGeneration)`. Non-null required strings/Optional container; direct structural validation before entering transaction enforces the same opaque ID/key/witness constraints. Null trace containers are invalid, `Optional.empty()` is legitimate non-web audit metadata. No reason/new expiry field or body key; witnesses are not identity. Preserve all existing records/execute service signature and reuse ShareSummary enums.

Add exactly `CustomerShareCommandResult.Rotated(String shareId, ShareSummary.VerificationMode verificationMode, ShareSummary.ShareStatus status, int codeGeneration, int revocationGeneration, String extractionCode)` and `.RotationReplayed(String shareId, ShareSummary.VerificationMode verificationMode, ShareSummary.ShareStatus status, int codeGeneration, int revocationGeneration)` with non-null references. Five result variants total; discriminator belongs to web DTO mapping. Five top-level command exports remain unchanged.

NEW internal `ShareRotationPolicyProvider` has `ProviderResult<RotationPolicyDecision> evaluate(RotationPolicyRequest request)`; nested `RotationPolicyDecision {ALLOW,DENY}` and nested `RotationPolicyRequest(ShareCommandCaller caller, UUID grantId, ShareSummary.VerificationMode verificationMode, ShareSummary.ShareStatus status, int codeGeneration, int revocationGeneration)`. All references non-null. Request uses locked source state and trusted caller, no raw key/secret/new timestamp or speculative policy inputs.

Add the `shareRotationPolicyProvider` bean inside existing command configuration with `@ConditionalOnMissingBean(ShareRotationPolicyProvider.class)`; its lambda returns `new ProviderResult.Unavailable<>(Code.SERVICE_UNAVAILABLE)`. The fail-closed adapter is this lambda, not a new adapter file or production decision. Inject it with `@Qualifier("shareRotationPolicyProvider")` into the service factory and constructor immediately after creation policy, before token/code/mobile/digest dependencies. The complete service collaborator order becomes template, DSL, repository, mandatory evidence reader, creation policy, rotation policy, tokens, codes, mobiles, sha256. Do not add a default ALLOW, choose policy parameters, add another feature flag or change enablement/credentials.

Only Allowed(ALLOW) continues; Denied (regardless of its supplied code) and Allowed(DENY) normalize to FORBIDDEN/403; Unavailable normalizes to SERVICE_UNAVAILABLE/503. Null/malformed outcome or thrown adapter exception fails closed as SERVICE_UNAVAILABLE/503. Exact replay never invokes policy. Production absence therefore fails closed for new rotations while not suppressing valid persisted replay. Default/web-only/disabled persistence contexts still create no command/provider/pool graph. A deterministic `rotationPolicy()` factory in the existing test adapter returns ALLOW only for tests and can be replaced with spies/denials/unavailability in test fixtures.

## 5. One Transaction, State Matrix And Ordering

Use existing writable READ COMMITTED, REQUIRES_NEW command template, rollback-on-commit-failure manager and transaction-aware command DSL, never query pool/transaction. All repository operations below take the same DSL/physical connection; no nested transaction, savepoint, connection, ThreadLocal or service SQL. Rotation does not invoke evidence projection, evidence-role switching, creation/risk/mobile/token providers, create identity/insert or allocate another grant UUID.

Exact sequence:

1. Structural command/caller validation, then enter one command transaction; `assertCommandRole`: SET LOCAL ROLE sharing_command_role followed by exact current_user assertion, before command reads.
2. `acquireCreatorWorkspaceLocks`: org ranks 7→8→9 with exact keys `organization_access.organization:<organization UUID>`, `organization_access.membership:<organization UUID>:<account UUID>`, `customer_iam.account:<account UUID>`; personal rank 9 only. Reuse `SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?, 0))`; canonical lowercase UUID text, not membership-row ID.
3. `readActiveCallerContext`, enforcing active account and selected org/membership; missing/inactive 403, SQL/decode failure 503. `lockGrant` by opaque shareId AND created_by_account_id AND owner_organization_id IS NOT DISTINCT FROM selected organization, explicit 20-column FOR UPDATE. Valid absent/non-owned 404. No grant disclosure or replay before these checks.
4. `sampleDatabaseTime`: one SELECT clock_timestamp(), after grant lock; require non-null microsecond-aligned Instant, reuse for expiry and operation timestamps. No JVM time/now()/second service clock. The existing V009 trigger is unchanged; this is one application clock sample, not a claim to remove trigger clock expressions.
5. Compute exact action identity/fingerprint (§6); `findTransition` before all current-state/status/mode/expiry/witness/policy/material checks. Exact fingerprint returns RotationReplayed from persisted snapshot and writes nothing. Changed fingerprint returns 409 without append. Corrupt/SQL/undecodable persisted fact is 503, not an identity miss.
6. Only after replay misses, enforce the matrix below in order: source status and mode, expiry, optional witness. Do not introduce lower-valid_from/admission-count/creation eligibility gates. No status repair or automatic expiry transition.
7. Evaluate rotation policy once with source snapshot. 403/503 stops before code generation or any event/update/Outbox/audit. Same key may succeed later after policy becomes ALLOW because refusal persists no transition.
8. Compute exact old+1 revision and both generations using checked arithmetic; unrepresentable server target fails closed 503 without writes. `codes.generate(grant.id(), nextCodeGeneration)` once. Validate replacement (§7) before appending facts. No token issuance, new grant identity or client-selected code.
9. Append CODE_ROTATED access event → execute one conditional UPDATE → verify returned intended full 20-field grant → append SHARE_CODE_ROTATED Outbox → append SHARE_GRANT_CODE_ROTATE audit. Only after transaction/commit succeeds return Rotated with in-memory code.
10. NotUpdated allows exactly one bounded same-transaction owner-filtered `lockGrant` reread, then always SHARE_TRANSITION_CONFLICT and rollback. No update retry, current-state success or lookup that could replay the uncommitted access event. Roll back access event and every later fact/state change. Existing unrelated SQL unique failures remain INTERNAL/500; known material CHECK/lifecycle-guard failures retain declared SERVICE_UNAVAILABLE/503, never raw DB text.

| Replay / current state | New-action decision after authorized grant lock |
| --- | --- |
| Matching persisted action/fingerprint; any later current status/time/witness | REPLAYED metadata before current checks; no policy/material/writes. |
| Persisted action, different fingerprint | 409, before current checks. |
| Any mode with ACTIVE, EXPIRED or REVOKED source | 409; no policy/material/writes, including REVOKED (not revoke's 410). |
| LOCKED with NONE, DESIGNATED_MOBILE or ORGANIZATION_MEMBER | 409; no unlock. |
| LOCKED EXTRACTION_CODE, sampled time >= expires_at | 409, including exact equality. |
| Eligible LOCKED EXTRACTION_CODE, witness present but != code_generation | 409 SHARE_TRANSITION_CONFLICT, not SHARE_GENERATION_STALE. |
| Eligible LOCKED EXTRACTION_CODE, witness absent or equal | Policy then material then one ACTIVE transition. |

## 6. Exact Identity, Replay And Durable Facts

Reuse existing RFC 8785-compatible canonical JSON and SHA-256 helpers, including UTF-16 key ordering and exact UTF-8 string encoding. All hashes below are lowercase 64hex. UUIDs are canonical string values. No new canonicalizer, digest algorithm or persistence table.

- `actionType = "CODE_ROTATE"`.
- `actionRequestDigest = hex(SHA-256(UTF-8(exact actionRequestId)))`.
- `sourceIdentity = {kind:"REQUEST",actionRequestId:<exact raw header value>}`.
- `eventId = "share-transition:" + hex(SHA-256(canonicalJson({shareGrantId:<locked UUID>,actionType:"CODE_ROTATE",sourceIdentity})))`.
- `actionFingerprint = hex(SHA-256(canonicalJson({shareGrantId:<locked UUID>,actionType:"CODE_ROTATE",reasonCode:null,newExpiresAt:null,actionRequestDigest})))`.
- Exclude witness, trace, observed time, status, current counters, new material and other transport fields. Same key with a changed witness/trace replays. Raw key exists only transiently in identity calculation, not persisted payload/audit. Revoke's different actionType remains a separate identity even if its raw key matches; do not invent cross-action key conflicts.

Extend existing `findTransition(DSLContext transactionDsl, UUID grantId, String eventId) -> Optional<PersistedTransition>` to decode the two exact registered successful combinations: REVOKED/REVOKE/REVOKED and CODE_ROTATED/CODE_ROTATE/ACTIVE. Retain grant/domain/event binding, SUCCESS, exact seven payload members, lowercase digest strings, integer/range checks, non-null timestamp and canonical payload hash verification. Rotation codeGeneration >=1; revocationGeneration/revision >=1. Do not broaden statuses/types or accept partial/malformed/hash-mismatched data. Existing nested PersistedTransition already holds grantId, eventId, actionType, actionRequestDigest, actionFingerprint, status, codeGeneration, revocationGeneration, revision and occurredAt; no new replay store/record type. Service compares fingerprint; replay status/counters are from this durable record, not the later head. shareId/mode remain immutable grant metadata.

| Fact | Exact values, payload, hashing and null behavior |
| --- | --- |
| Access event | Existing AccessEvent, new row UUID; share_grant_id=locked grant, share_admission_id SQL null; event_domain=SHARE_ACCESS; event_id as above; event_type=CODE_ROTATED, result=SUCCESS; occurred_at=single sampled Instant; risk_digest SQL null. payload_redacted has exactly `{actionType,actionRequestDigest,actionFingerprint,status,codeGeneration,revocationGeneration,revision}`, with CODE_ROTATE, ACTIVE and target counters/revision. payload_sha256 hashes canonical JSON of exactly this object; timestamp is not a payload key. |
| Outbox | Existing OutboxFact via platform.outbox_append; eventDomain=SHARE_ACCESS; aggregateType=EVIDENCE_SHARE_GRANT; aggregateId=grant UUID; eventType=SHARE_CODE_ROTATED; dedupeKey=`"share-transition:" + eventId`, literally `share-transition:share-transition:<64hex>`. payloadRedacted has exactly `{shareId,grantId,actionType,actionRequestDigest,actionFingerprint,status,codeGeneration,revocationGeneration,revision,occurredAt}`. payloadCiphertext absent/SQL null. payloadSha256 hashes this exact canonical object independently of the access payload. occurredAt is sampled time in canonical RFC 3339 UTC Z text, whole seconds if nanos=0, otherwise exactly six fractional digits; no timestamp argument is added to outbox_append. |
| Audit | Existing AuditFact via audit.append_fact; occurredAt=sampled Instant; actorType=ACCOUNT, actorId=trusted account UUID; actionCode=SHARE_GRANT_CODE_ROTATE; objectType=EVIDENCE_SHARE_GRANT, objectId=grant UUID. beforeSnapshot exactly `{status:"LOCKED",revision:<old>}`, afterSnapshot exactly `{status:"ACTIVE",revision:<old+1>}`; reasonCode absent/SQL null. requestId=actionRequestDigest; traceId=`command.traceId().map(UUID::toString)`: web trace present, direct Optional.empty gives SQL null. No trace is added to fingerprint or other facts. |

No raw code/token/key, salt/code hash, mobile, reason or policy details enter either payload, audit snapshots, logs or Problems. Stored code_salt/code_hash are permitted only on the grant. No SHARE_* type is inserted into V009's access-event CHECK. First response alone returns plaintext code; an exact replay generates/returns none and appends no event/Outbox/audit or revision even after relock, later rotate/revoke, elapsed expiry or policy change.

## 7. Repository Shape And Conditional Update

Retain the existing accepted interface unchanged:

```java
TransitionResult transitionGrant(DSLContext transactionDsl, UUID grantId,
        int expectedRevision, ShareStatus expectedStatus,
        Optional<Integer> expectedRevocationGeneration,
        Optional<Integer> expectedCodeGeneration, ShareStatus targetStatus,
        int targetRevision, int targetCodeGeneration,
        int targetRevocationGeneration, Optional<ExtractionCodeMaterial> replacement);
```

All references/Optional containers are non-null; optional client witness converts with Optional.ofNullable. Rotate passes expectedRevision=old revision, expectedStatus=LOCKED, expectedRevocationGeneration=empty, expectedCodeGeneration=nullable client witness Optional, targetStatus=ACTIVE, all three target integers old+1, replacement=present. Revoke retains target REVOKED and its existing empty code witness/replacement branch. Reject unsupported call shapes; this is not a generalized transition framework.

Rotation UPDATE shape, using bound parameters and the existing explicit COLUMNS constant:

```sql
UPDATE sharing.evidence_share_grant
SET status = ?, code_salt = ?, code_hash = ?, code_generation = ?,
    revocation_generation = ?, revision = ?
WHERE id = ? AND revision = ? AND status = ?
  /* append AND code_generation = ? only when code witness is present */
  /* append AND revocation_generation = ? only when its witness is present */
RETURNING <existing explicit twenty columns>
```

The comments above describe SQL assembly, not inline production comments to add. For this rotation call only the optional code witness can be present. Empty Optional adds no predicate; zero is a real witness. Source mode/ownership were established by immutable-mode owner-filtered locked read; retain that boundary, not a new unowned lookup. Targets are checked old+1 values bound once; no second UPDATE/touch of unrelated columns.

The explicit twenty-column projection, all existing in V009, is `id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id, verification_mode, code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, download_allowed, content_snapshot, content_sha256, status, revocation_generation, revision`. Return Updated(decoded grant) or NotUpdated, never wildcard projection. Compare the returned row with the full intended ShareGrant; mismatch aborts INTERNAL and rolls back.

Only six grant columns change: status LOCKED→ACTIVE, new code_salt, new code_hash, code_generation+1, revocation_generation+1, revision+1. All other fourteen stay exactly equal, including capability_token_hash, expiry/valid_from, admissions, snapshot/hash, owner and identifiers. V010 already grants eight mutable columns (these six plus expires_at and admissions_used); this slice must not write those extra two or modify grants/DDL. Preserve Java/business grantOwnerOrganizationId versus SQL owner_organization_id mapping; no snapshot regeneration or frozen creation JSON changes.

Replacement uses existing ShareExtractionCodeProvider and ExtractionCodeMaterial. Validate non-null code/salt/hash, code 1–128 code points with existing encoding/NUL guards, salt `[A-Za-z0-9_-]{22,256}`, lowercase hash `[0-9a-f]{64}`, and salt AND hash each different from the locked old value. Next code and revocation generations and revision must each equal old+1, source LOCKED+EXTRACTION_CODE, target ACTIVE. Adapter Denied/Unavailable, thrown/null/malformed result, null fields, invalid shapes or reused salt/hash fail SERVICE_UNAVAILABLE/503 before any event/update. This specifies only frozen storage/transport acceptance, not a production code length or KDF. Never recompute an invented KDF; codeHash is the provider's approved output. A V009 material/guard rejection still rolls back as declared 503 through the existing classifier.

## 8. Exact Test Delta And Proof Ownership

No methods are removed/renamed/disabled, no dynamic/parameterized count expansion, no new test classes. Use ordinary named @Test methods. Existing helper methods do not count as test inventory. Preserve all existing create/revoke/list/me assertions. Update constructor call sites in the already-owned service and IT fixtures only.

### 8.1 Exactly One New Surefire Method

`CustomerShareCommandServiceTests.rotateReplayNeverReturnsCode` (required new method, accepted full-plan name): independently recompute request digest/event identity/fingerprint for exact Unicode/whitespace/comma key; verify changed witness/trace excluded. Prove role→locks→active context→owned grant→one clock→findTransition order, replay before all current status/time/witness/policy/material checks, persisted status/counters and zero secrets/writes. Test differing fingerprint 409 and no append; context/ownership still precede replay. For new actions prove source/mode/expiry/witness→policy→material→event→update→Outbox→audit order, Denied/DENY 403 and unavailable/malformed/throwing 503 before material/writes, invalid replacement/reused salt/hash 503, direct invalid inputs rejected before transaction, null versus empty trace-container semantics, and no token/evidence/creation provider calls. Add table-driven loops within this one method, not new test methods. Reuse fixture/helpers rather than refactoring create/revoke tests.

### 8.2 Exactly Five New Failsafe Methods

All in existing `CustomerShareCommandsIT`, bringing its 13 methods to the accepted 18:

1. `rotateWritesRegisteredEventAuditAndOutboxAtomically` — real PostgreSQL LOCKED EXTRACTION_CODE success with absent/equal witnesses and nontrivial old counters; assert code once, no token/stored material in response, exact six changed/fourteen unchanged columns, all three increments exactly one, salt/hash independently distinct, no plaintext storage. Assert all three facts/closed payload sets/canonical hashes/double-prefix dedupe, access/audit nulls, trusted present trace and direct Optional.empty SQL-null trace, one sampled microsecond timestamp and normalized Outbox JSON read-back. Record actual SQL roles/PIDs/clock/order and policy/material invocation order. Denied, Allowed(DENY), Unavailable, missing production policy, thrown/malformed policy and material failures leave row/facts/counters unchanged; no code call on policy refusal. Retry identical key after ALLOW succeeds; replay bypasses newly denied/unavailable policy. Prove malformed/reused code material gives 503 before append, not guard 500. Test deterministic provider returns generation-distinct salt/hash/code while generation-one create behavior stays intact.
2. `rotateReplayReturnsNoCodeOrCapabilityToken` — independently inspect persisted successful action; repeat with absent/equal/stale/changed witness and changed trace; advance controlled DB-derived time beyond expiry and perform legitimate later lock/rotate/revoke lifecycle steps. Original action still returns original ACTIVE result/counters, no extractionCode/capabilityToken and no provider calls/new facts/head/revision changes. Compare fresh durable snapshots before/after each replay. Retain context/ownership concealment. Corrupt/mismatched persisted payload/hash yields 503 rather than regeneration; seed separate malformed append-only facts for repository decoding in rollback-isolated fixtures, never UPDATE existing facts or disable guards. Fingerprint conflict under the same identity rolls back without appending.
3. `rotateRequiresExactLockedStateAndGenerationWitness` — four modes × four statuses, before/equal/after expiry, absent/exact/stale code witnesses; only unexpired LOCKED EXTRACTION_CODE succeeds. Stale is 409 transition conflict; terminal REVOKED is not 410. Invalid source/status/mode/expiry/witness yields zero policy/material/writes. Real absent/non-owned/personal-vs-org/workspace mismatch hides as 404; inactive/missing account/org/member gives 403; context SQL/decode/role/lock/clock faults give 503. Exercise actual conditional UPDATE with present/absent witness and forced zero-row result, proving exactly one bounded owned reread, no retry/update success, access-event rollback and 409 even when reread is absent or changed. Revision/status/code witness predicates are observable, not mocked away; bound old+1 target values and full-row comparison remain enforced.
4. `rotateConcurrentActionsProduceOneTransition` — bounded two-connection identical-key race: one ROTATED, one REPLAYED, one returned secret and one event/update/Outbox/audit set; distinct-key race: one ROTATED, one 409 after source becomes ACTIVE, one transition. Cover personal rank 9-only and org 7→8→9 exact advisory keys, active-context-before-grant and effective role/connection/clock isolation for each operation. Rendezvous before serialized locks, never inside held advisory/grant locks; finite Future/latch/database lock/statement timeouts and finally executor/connection cleanup. No sleep-only race assertion, hanging barrier or broad retry loop. No double generation/revision increment.
5. `rotateFailureAtEachFactAppendRollsBackAllEffects` — use existing transaction-bound repository/SQL probes and commit-failure seam to inject before/after event insert, conditional update, Outbox append, audit append and after callback before commit. Verify every injected failure against fresh admin connections: all twenty grant fields unchanged, no durable event/Outbox/audit, no generation/revision increment, no success result/secret. Assert sanitized existing 500 for unexpected append/commit faults versus declared 503 for known availability/material failures; no raw canary/SQL. Remove fault and retry same key successfully, then exact replay returns no code. Do not treat uncommitted event as replay.

Seed LOCKED states with existing valid insert/lifecycle SQL and owner fixture authority, preserving V009 checks/triggers, V010 command-role execution and NOINHERIT probe isolation. Existing test fixture/DB may manage test-only policy beans and wrappers; production service remains command-role only. Reuse generation-one extraction-code creation, not another grant-construction implementation. Reuse existing role/catalog tests, failure-injection instrumentation and bounded race pattern; extend local helpers only. Test provider `codes()` must keep the existing generation-one outputs and derive stable, distinct later-generation test salt/hash/code from grant ID/generation; this is not a production algorithm.

### 8.3 Assertions In Existing Methods Only

| Existing exact owner | Added/retained assertions |
| --- | --- |
| `CustomerSharesCommandWebTests.revokeAndRotateMapClosedResponseUnions` | Replace rotation-deferred 404 assertions with real Filter/Controller first/replay 200, closed exact DTO key sets, trusted personal/org caller/key/trace, no replay secret; all eight rotation error-code mappings, no revoke-only stale/410 leakage; retain revoke and shared writer/query mappings. |
| `CustomerSharesCommandWebTests.invalidCommandAndMissingServiceMapToProblemResponses` | Rotation empty/closed body success, all header/media/path/body invalidity classes and 128/129 code-point boundaries, duplicate headers/member keys, exact accepted key bytes; provider-resolution counters prove validation precedence, malformed workspace+header precedence, and blank/over-128 route-shaped shareId plus malformed trusted workspace returns 422 / VALIDATION_FAILED with valid header/media and zero provider resolution/service calls (§3.1); retain valid-shareId/malformed-workspace 503, missing/BeansException 503, sanitization and no service invocation on invalid binding. |
| `CustomerSharesCommandWebTests.createUsesTrustedCallerAndMapsCreatedAndReplay` | Preserve create/header-ignore/one-time secret/trace coverage. |
| `CustomerSharesCommandWebTests.commandRoutesDoNotChangeShareListRoute` | Preserve GET query-only route/cursor mapping and no command resolution. |
| `CustomerSharesWebTests.exactGetAndPostRoutesPassHighestPriorityFilter` | Admit only rotation POST; retain all exact route/context/encoding/matrix/method rejection cases, path 422 and header-before-body/provider precedence; web-only missing command 503 and registered command 200. Preserve list/me/create/revoke probes. |
| `SharingCommandPersistenceConfigurationTests.defaultAndWebProfilesCreateNoCommandDatasource` | Default/web/disabled graph also has no rotation provider; retain absent pools/credentials failures. |
| `SharingCommandPersistenceConfigurationTests.commandProfileUsesDistinctPoolAndResetsLocalRole` | Enabled graph has qualified fail-closed default policy returning Unavailable; explicit test policy override wins with no ambiguous/default duplicate; existing distinct pool/read-committed/requires-new/close/mandatory-reader checks unchanged. |
| `CustomerWebConfigurationTests.customerAndSharingGraphsUseExactQualifiers` | Exact added shareRotationPolicyProvider service parameter in position stated in §4; retain query/command ObjectProvider qualifiers/order, flags, bean graphs, no eager dependency. |
| `SharingQueryBoundaryTests.sharingQueryApiAndProjectionAreExact` | Exact 19 command files/eight providers including NEW path, nested policy request/decision components and non-export; still exactly five command exports and unchanged query interface, denylist, allowed module graph. |

Unchanged `ModuleBoundaryTests`, `CustomerApiBoundaryTests`, query/configuration suites, role IT and all other suites run as regressions. No weakening of their exact imports, 14 modules, two discovered customer/sharing/evidence edges, named interfaces, SQL/source firewall or frozen migrations. Do not edit those tests merely to make inventory green.

## 9. Source = CI = XML And README

Baseline is 16 Surefire classes/78 methods plus 6 Failsafe classes/113 methods =191. Add exactly 1+5: **16/79 Surefire; 6/118 Failsafe; 197 combined**, failures=errors=skips=0. Service class becomes 6 methods (four create, one revoke, one rotate); CustomerShareCommandsIT becomes 18 (8 foundation/create +5 revoke +5 rotate). Other class/method inventories unchanged. Failsafe breakdown `64+10+10+8+8+18=118`. No POM include or dependency change; retain failIfNoTests and Docker requirement.

In `.github/workflows/ci.yml` add only §8.1's method to the service class set in `unit_expected` and §8.2's five to the existing IT class in `container_expected`; retain every other name and source equality/duplicate/XML checks. Change explicit Surefire count/error 78→79 and final exact_inventory arguments to 79/118. Inventory print becomes `PA14 exact source/CI/XML inventory passed: Surefire 79; Failsafe 64+10+10+8+8+18=118; combined 197; zero skips/errors/failures`. Keep 16/6 classes and source=CI=XML equality; do not insert DTO/provider production filenames into test dictionaries. JDK/Maven pins, build command, smokes/firewall and job ceiling are unchanged.

README edits stay in existing current-total/admitted-behavior/history clauses around baseline lines 40, 291, 300, 305, 322: current totals 79/118/197; explicit six Failsafe classes, 18 command IT methods and `64+10+10+8+8+18`; distinguish historical create 77/108/185 and revoke 78/113/191 from current rotation. Replace line 40's deferred-rotation 404 statement with admitted rotation under the trusted boundary and fail-closed missing policy, one-time code/no replay secret, not production enablement. Preserve historical PA13-addition wording as historical, or append the new PA14 delta; extend cumulative history through final PA14 slice. Preserve historical 159 (line 300), 64 (line 322), historical S3=64 (line 313) and nine structural waves/ten migration entries. No unrelated README rewrite or production-ready claim.

## 10. Future Implementation Validation Commands And Evidence

These commands are a **future implementation checklist**, not executed at this plan gate. Run in established Docker/loopback-capable Java 21 environment. Canonical CI is Temurin `21.0.13+9`, Maven `3.9.9`, PostgreSQL major 16; PostgreSQL **16.15** is the established prior reference environment, not a hard pass/fail condition. Existing fixture image `postgres:16` remains test-only/unpinned: require PostgreSQL major 16 and record the actual `SELECT version()` / `server_version`. If the image resolves to another 16.x patch, a successful behaviorally valid run remains admissible with that actual version recorded; do not silently claim a version or edit fixture/schema. Do not disable Ryuk, skip tests, loosen assertions or choose a new production version.

From repository root, established local invocation adaptations (not repository edits):

```sh
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
MVN=/opt/maven-3.9.9/bin/mvn
AGENT='-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar'
export DOCKER_HOST=unix:///mnt/wsl/docker-desktop/shared-sockets/guest-services/docker.proxy.sock
java -version
"$MVN" -v
docker info
"$MVN" -B -ntp -f backend/pom.xml "$AGENT" \
  -Dtest=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests,SharingCommandPersistenceConfigurationTests,CustomerWebConfigurationTests,SharingQueryBoundaryTests test
"$MVN" -B -ntp -f backend/pom.xml "$AGENT" \
  -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify
"$MVN" -B -ntp -f backend/pom.xml "$AGENT" -Pcontainer-smoke clean verify
```

Use the guest Docker socket only when present/authorized; ordinary CI uses its configured daemon and `./mvnw -B -ntp -Pcontainer-smoke verify` from backend. Existing Windows wrapper CRLF and Mockito self-attach limitations were documented in the revoke implementation evidence; installed exact Maven plus explicit agent are invocation-only adaptations, not wrapper/POM/config changes. If sockets/daemon are denied, record NOT RUN/failed environment and send runtime validation to Controller/Platform; compilation or partial XML is not 197-test evidence. No escalation or environment mutation is authorized by this plan gate.

After the full clean verification, run the **existing CI bodies**, not a replacement reduced validator. The following actual extraction/execution command (from backend) reuses the inventory, packaged-JAR and firewall steps exactly, with only installed-Maven invocation adaptation locally; it generates runtime evidence and therefore belongs only to the future implementation validation gate:

```sh
cd backend
python3 - <<'PY'
from pathlib import Path
import os
import subprocess
import textwrap

workflow = Path('../.github/workflows/ci.yml').read_text()
steps = ('assert container smoke actually ran',
         'default and opt-in web jar smokes',
         'production jar dependency firewall')
for name in steps:
    block = workflow.split('      - name: ' + name + '\n', 1)[1]
    block = block.split('\n      - name:', 1)[0]
    script = textwrap.dedent(block.split('        run: |\n', 1)[1])
    script = script.replace('./mvnw', '/opt/maven-3.9.9/bin/mvn')
    subprocess.run(['bash', '-e', '-c', script], check=True, env=os.environ.copy())
PY
```

Expected evidence: exact source/CI/XML name sets for 16/79 and 6/118, no duplicate/missing/extra cases, 197 tests with zero failures/errors/skips; 18 command IT methods on PostgreSQL major 16, with the actual `SELECT version()` / `server_version` recorded, and with roles, SQL/bind ordering, one physical command connection/clock, full grant snapshots, exact fact/hash/null/trace assertions, policy/material ordering, rollback and bounded race results. Targeted verify alone supplies 18 command IT results, not all 118. Default non-web/database-free and opt-in web missing-adapter JAR smokes pass; production-JAR firewall retains all 82 adversarial probes and no deterministic providers/verifiers leak into the production artifact. Existing V001–V010/OpenAPI hashes and module/ACL boundaries stay unchanged. Keep logs/XML as runtime evidence in the implementation gate's authorized destination, not a new artifact in this plan gate.

Static implementation closeout must also run git diff --check, compare changed paths to §2, verify exactly six new @Test methods/source=CI, no baseline deletions/renames/skips, unchanged migration/API files and POM/profile flags. Any extra file/method/schema/policy requirement is a stop-and-return-to-plan condition, not implicit permission.

## 11. Minimality, Residual Risks And Blocking Questions

One externally verifiable behavior slice spans web→command→policy/material→conditional SQL→facts→tests. It adds only the missing internal policy contract and rotation variants/path to existing machinery. No separate infrastructure/provider slice, generic transition engine, new replay table, schema migration, cross-module dependency, create/revoke rewrite, speculative policy/KDF or future admission feature. Existing shared signatures, transaction/locks/facts, writer, profiles and tests are reused; only one missing production file and six test methods are added. Exact replay never regenerates lost one-time material; recovery is not an unauthorized retrieval endpoint. This bounded loss characteristic is tested/documented, not “fixed” by storing plaintext.

| Residual risk | Owner and destination / treatment |
| --- | --- |
| Production rotation allow/deny policy and approved code KDF/material custody are intentionally unset. | Security/Product/Sharing Backend → approved security/policy/provider design and later provisioning gate. Current missing adapter fails closed 503; deterministic ALLOW/material remains test-only. Not a blocker to this bounded implementation plan. |
| Role/connection reset, conditional predicates, append/commit rollback and race behavior require execution, not prose. | Backend/Database → five new PostgreSQL IT methods and full 197-case implementation evidence; Controller/Platform → Docker/loopback-capable runner. Required before implementation acceptance. |
| Existing postgres:16 image is unpinned and can resolve to another 16.x patch than the established prior 16.15 reference environment; local wrapper/agent/socket limitations can prevent proof. | Platform/Controller → record actual toolchain/image and `SELECT version()` / `server_version`; require PostgreSQL major 16 and the full behavioral evidence in §10, not patch 16.15. Do not silently claim versions or call partial runs acceptance. |
| Frozen DB lifecycle guards structurally admit some service-forbidden shapes; this slice must never expose them (especially ACTIVE-to-ACTIVE or non-code unlock). | Sharing/Backend → state/mode and exact-field PostgreSQL regression assertions; Database retains existing structural constraints. No DDL strengthening in this slice. |
| One-time response loss and request abuse/rate limiting remain outside code retrieval semantics. | Sharing/Product → frozen replay contract/response secrecy tests; Security/Platform → existing gateway rate-limit ownership. No secret recovery, new TTL or gateway implementation. |

**Blocking open questions: none for the confirmed goal and this plan gate.** Unapproved production values remain explicit fail-closed residuals, not questions answered by invented defaults. Runtime capability/version deficiencies, if encountered later, block runtime acceptance and go to the named owners; they do not authorize extra source edits or claim success.

## 12. Plan-Gate Review And Completion Boundary

Read the complete written artifact. Verify every existing path/type/test/column/fact/digest above against the accepted plan and merged source; NEW items are explicitly the one provider file, nested command/result/DTO records, binder/controller methods, deterministic rotation-policy factory and six named tests. Future full-row UPDATE projection and payload key sets use only existing schema/fact columns; no migration/API changes are required.

For this worktree's Windows `.git` pointer, ordinary Git fails on Linux. Use read-only process-local overrides, never rewrite metadata:

```sh
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call14
export GIT_WORK_TREE="$PWD"
git diff --check
git status --short
git diff --name-only
git ls-files --others --exclude-standard --directory
sha256sum docs/gateflow/pa14-sharing-command-code-rotation-plan.md
```

At intake, a pre-existing untracked `.codex` entry was present. Do not read, modify, delete or stage it. The final path check must prove no tracked change and exactly this plan as the new task-created file, with the pre-existing private entry unchanged/excluded from task delta. New-file whitespace must also be checked explicitly because ordinary git diff --check omits untracked files (use git diff --no-index --check /dev/null against this artifact). Record observed results and SHA256 in the completion report rather than claiming implementation tests ran.

Completion report must state runtime/provider/model honestly (Control's requested routing is not independent proof of the actual model), task label, gate, exit result, artifact path/SHA256, one slice, expected 79/118/197 inventory, read-only validation, owned residuals/no blocking questions, and no implementation/remote/external action. This task neither stages/commits/pushes nor creates/updates a PR, requests review, approves, marks ready, merges, comments externally or changes an issue. No delegation. No implementation artifact is created.
