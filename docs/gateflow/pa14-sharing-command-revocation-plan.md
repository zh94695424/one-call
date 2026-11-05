# PA14 Sharing Command Revocation — Bounded Implementation Plan

- Task: `pa14-sharing-command-revocation-plan-gpt-20261105`.
- Work unit/gate: `PA14-sharing-command-revocation` / `plan`, 2026-11-05.
- Assigned runtime/provider/model: Codex runner / `gpt` / `gpt-6.1-sol`, reasoning `high`.
- Baseline: merged PR #22, `71e91f0e0fac5b2ea14976f390c1924f7825f3a5`, branch `codex/pa14-sharing-command-revocation`.
- Deliverable: this plan only. Exactly one future implementation slice: revoke, slice 2 of the accepted full PA14 plan. Planning completion is not implementation, gate acceptance or production readiness.

## 1. Goal, motivation and binding boundary

Implement only `POST /api/v1/customer/shares/{shareId}/revocations`, through the existing trusted customer web boundary, command service and command-role transaction. An authorized creator in the selected workspace must be able to revoke future share access exactly once, replay the original action without writes, and receive frozen, sanitized conflicts for incompatible actions. Persist the transition and its three durable facts atomically. A database-only lifecycle guard is not a substitute for service-level authorization, replay or error precedence.

Success means the exact response/error/state matrix below is executable; PostgreSQL 16 evidence proves role assertion, lock order, current context, ownership concealment, deterministic identity, immutable replay, conditional update, exact facts, rollback and bounded concurrency; create, list and `/me` remain green. Expected inventory is **78 Surefire methods / 16 classes + 113 Failsafe methods / 6 classes = 191**, zero failures/errors/skips. The increment is exactly one Surefire and five Failsafe methods, not the full plan's later 79/118/197 endpoint.

Non-goals: code-rotation success or its command/result/provider implementation; expiry observation/extension/lock commands; public admission/ticket/media authorization; recall of issued short-lived credentials or remote buffered media; Redis, COS/KMS, SMS/OTP delivery, frontend; production policy, TTL, KDF, HMAC or provisioning values. No V001–V010, schema, OpenAPI, product-contract or accepted-foundation changes. No new datasource, transaction manager, export, dependency, migration, platform idempotency scope, policy provider or service dependency. No merge, approval, ready action, reviewer request, external comment or issue mutation. Do not stage, commit, push, change branches or delegate.

## 2. Direct evidence and first-principles decisions

All authorities are local frozen repository sources; no external contract is needed. References identify sections or source symbols rather than assuming old full-plan line numbers still describe the merged code.

| Evidence inspected | Established fact and goal-aligned decision |
| --- | --- |
| `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`, Identity, Durable facts, Transactions, Repository, Routes and named-test sections | Derive revoke from the accepted plan; reuse its exact records, event/action identities, redacted payloads and event → update → Outbox → audit order. Do not redesign shared foundation or implement its third slice. |
| `docs/gateflow/project-execution-control.md`, 2026-11-05 goal checkpoint; prior slice/code/aggregate/PR review intake | Revoke-only goal is accepted. Prior findings are closed; preserve fixes for same-connection role control, timestamp typing, canonicalization and create scope/material/replay. Deferred evidence-owner lifecycle/snapshot questions and environment limitations do not authorize changes here. Baseline and 77/108/185 are explicit. |
| `docs/14-production-data-model-and-flyway-schema-spec.md` §§2.4/2.5/2.12, 4.44–4.46, 7.4, 9.0/9.0.1/9.0.2, 10.21/10.23/10.24; §9.29 corroborates owner transitions | Conditional revision, canonical hashes, deterministic request transitions, absorbing terminal states, command ACL, ranks and owner-local append order already exist. §10.24 is an evidence-lifecycle registry, not authority to rename sharing facts; sharing literals come from the accepted PA14 clarification/plan. No documentation/schema amendment is necessary. |
| `docs/15-production-openapi-surface-spec.md` §§3.3/4; `api/openapi/onecall-v1.openapi.yaml` `revokeCustomerShare`, `ShareRevokeRequest`, `ShareState`, `IdempotencyKeyParam`, referenced responses | Required header and JSON body, opaque share ID, optional non-null witness/reason, 200 state projection, revoke-specific 409/410 codes, and non-recall promise are frozen. Do not introduce a reason enum, code witness body field, discriminator or secrets. |
| `backend/src/main/resources/db/migration/V009__sharing.sql`, grant/access-event definitions, lifecycle and append-only guards; V010 privilege migration | Twenty grant columns; eleven event columns; ACTIVE/LOCKED → REVOKED requires revocation generation +1, revision +1 and unchanged code material. EXPIRED/REVOKED are terminal. Existing command role can select/insert and update the eight authorized grant columns; revoke uses only three. No migration or ACL widening. |
| `sharing/internal/command/{CustomerShareCommand,CustomerShareCommandResult,ShareCommandException}.java` | Sealed command admits only Create; results only Created/CreationReplayed. Add only nested Revoke and Revoked. The exact error enum already contains revoke codes; leave it unchanged. |
| `sharing/internal/commandservice/DefaultCustomerShareCommandService.java` `execute`, `create`, hash/canonical/state helpers | Currently dispatches only Create and owns one `TransactionTemplate`; extend dispatch without changing create ordering, exception sanitization, constructor or providers. Reuse canonical SHA-256 machinery. Revoke calls no evidence reader or create-material/policy provider. |
| `sharing/internal/persistence/{SharingCommandRepository,JooqSharingCommandRepository}.java` | Existing explicit `COLUMNS`/grant decoder, role assertion, rank locks, active-context reader, database clock, Outbox/audit functions suffice as foundation; grant lock, transition lookup, event append and conditional transition are missing and belong here, not SQL in the service. |
| `customer_iam/internal/web/{CustomerBoundaryFilter,CustomerSharesController,CustomerShareCommandJsonBinder,CustomerShareCommandDtos,CustomerProblemWriter}.java` | Filter/controller admit only create/list/me. Binder is local, duplicate- and trailing-token-strict; DTOs have no sharing imports. Writer already maps all frozen revoke errors and query CURSOR_INVALID. Extend four web files only; no writer edit is needed. |
| `backend/src/test/java/com/onecall/archtests/{CustomerSharesCommandWebTests,CustomerSharesWebTests}.java` | Four/ten existing methods. `revokeAndRotateMapClosedResponseUnions` currently checks writer rows and rejects both future routes; expand revoke assertions while retaining rotate 404. `exactGetAndPostRoutesPassHighestPriorityFilter` currently rejects all transition methods; replace only exact revoke POST expectation and retain alias/encoding/method/context checks. |
| Existing service tests and `container/CustomerShareCommandsIT.java` | Four/eight current methods. IT owns a PostgreSQL 16 fixture, synthetic NOINHERIT login, distinct command pool, SQL/role/PID/bind recorder, deterministic test providers and append/commit fault patterns. Extend inside these classes, not a new fixture/test class. |
| `architecture/{SharingQueryBoundaryTests,CustomerApiBoundaryTests,ModuleBoundaryTests}.java`, command configuration tests, POM, CI and README | Existing boundaries already export five command top-level types, protect query files and isolate pools. Nested additions need no export/inventory widening. POM already explicitly includes CustomerShareCommandsIT. CI and README are the only required inventory edits. |

First-principles judgments: authorize current caller and lock owned grant before revealing historical replay; compare action fingerprint before terminal/current witnesses so successful revoke retries work; enforce terminal/expiry before a new action's witness so a stale input cannot hide terminal status; row serialization plus a witnessed UPDATE gives one state winner; appending event before UPDATE requires throwing on every failed update so no orphan success fact can commit. These judgments specialize the frozen order, not new policy. DB-legal same-state terminal generation bumps remain service-forbidden.

Planning inspection confirmed HEAD with a read-only explicit Git-directory override because `.git` contains a Windows `D:/...` worktree pointer. The pre-existing modified `docs/gateflow/project-execution-control.md` and untracked `.codex` are not this task's changes; preserve them. No AGENTS.md was found in the worktree/parent inspection. Do not repair repository metadata.

## 3. Exact transport and response contract

### 3.1 Admission, binding and trusted caller

- Admit only the exact POST route with one nonempty opaque path segment, alongside unchanged create POST and existing GET routes. Keep raw request URI equal to context path plus servlet path. Reject alternate methods, trailing slash, extra segments, matrix parameters, encoded route/ID aliases and route-name encodings with the existing sanitized 404 boundary. IDs are not parsed as UUIDs. Syntactically admitted IDs are validated nonblank, 1–128 Unicode code points before command-provider resolution/DB access; invalid IDs yield 422, valid absent/non-owned IDs yield 404. A colon in a valid `share:` identifier is ordinary data.
- Preserve filter authentication and trusted session/workspace selection. No body/query/request-attribute account, organization, role or channel selector is accepted as authority. Controller validates selected workspace shape exactly as create/list already do: personal with absent organization, or organization with present organization; invalid trusted shape is controlled 503.
- For revoke only, after authentication and before Content-Type/body parsing/provider resolution/DB, enumerate `request.getHeaders("Idempotency-Key")`. Require exactly one value, nonblank, 1–128 Unicode code points. Reject missing, blank/whitespace-only, overlong and duplicate values, including identical duplicates, with 422 VALIDATION_FAILED. No first-value or joined-value fallback, trim, normalization, case folding or truncation. Header name is case-insensitive; accepted value and its UTF-8 bytes are preserved. Preserve valid comma-containing single values as a single value, not an invented comma-splitting policy. Keep existing lossless UTF-8/NUL safety guards consistent with create; do not invent normalization. Following the existing `CustomerBoundaryFilter.trace(request)` pattern, the Filter stores the exact accepted string in a filter-owned request attribute and exposes package-private `static String idempotencyKey(HttpServletRequest request)` to retrieve that stored string unchanged. The Controller calls only `CustomerBoundaryFilter.idempotencyKey(request)` for the key; it must not contain the literal `getAttribute(` (including `request.getAttribute(`), re-enumerate the header or split its value. The accessor performs no trim, normalization, case folding, truncation or comma splitting; never derive identity from JSON.
- On create, continue ignoring unrelated Idempotency-Key, including invalid/duplicate values. For revoke, invalid header beats malformed body, Content-Type and invalid trusted workspace shape. Authentication failure precedes header validation. Workspace shape remains controller-side after valid header/Content-Type.
- Require exactly one syntactically valid `application/json` Content-Type with valid parameters; missing, malformed, wildcard, other media types and `application/*+json` return 422 before body binding/provider/DB, not 415. Apply the existing check to create and revoke without changing existing Accept, buffering, trace, sanitization or security-header behavior.
- Body is required; `{}` is valid. Add nested web `RevokeRequest(Integer expectedRevocationGeneration, String reason)`. Allowed members are exactly `expectedRevocationGeneration` and `reason`. Omitted members map to Java null; explicit JSON null for either is invalid. Witness must be an integral int >=0 (including zero); reject negative, fractional, string, boolean and int overflow. Optional reason, when supplied, must be a nonblank string of 1–128 Unicode code points; preserve the exact accepted string, including surrounding whitespace. No compulsory reason when omitted, enum, trim or reason translation. Validate malformed/duplicate/trailing JSON, arrays/scalars, missing body, unknown fields (including actionRequestId, traceId, expectedCodeGeneration or workspace selectors), invalid types and explicit nulls as 422 with no invocation/effects. Reuse the strict local mapper; no global mapper change.
- Add `public CustomerShareCommandDtos.RevokeRequest revoke(byte[] body)` to the binder. The controller's new exact `@PostMapping(path="/api/v1/customer/shares/{shareId}/revocations", produces="application/json")` handler is `public Object revokeCustomerShare(String shareId, Principal principal, HttpServletRequest request)` with explicit `@PathVariable("shareId")` on the string and IOException as needed. Validate path, trusted principal/workspace and body, construct Revoke, then resolve only the qualified command provider. A missing bean/BeansException is 503. Do not resolve or fall back to the query service.
- Pass `new ShareCommandCaller(accountId, Optional.ofNullable(selected.organizationId()))` and `Optional.of(CustomerBoundaryFilter.trace(request))`. No body trace/header trace override, ThreadLocal or new dependency. Direct non-web calls may carry `Optional.empty()` trace. Validate direct service inputs too, before opening a transaction.

### 3.2 Success and Problem responses

Both new success and exact replay are HTTP **200 application/json**, a flat closed ShareState using the existing DTO/UTC serializer. Exact members: `shareId`, `evidenceId`, `verificationMode`, `status`, `validFrom`, `expiresAt`, `maxAdmissions`, `admissionsUsed`, `downloadAllowed`, `codeGeneration`, `revocationGeneration`. `maxAdmissions` is explicitly null when absent; other members are required/non-null. Revoke status is REVOKED. No nested `state`, `result`, revision, traceId, capabilityToken, extractionCode, token/code hashes, salt, object keys/URLs or raw action key. Replay projects the first event's status/generations plus unchanged grant metadata, not arbitrary current-state counters/generations; the terminal revoke cannot subsequently mutate through this service.

Use existing `CustomerProblemWriter` and `commandFailure` handler unchanged. Problem is exactly `{type:"about:blank", title, status, code, traceId}`, with server-generated trace UUID, application/problem+json, HTTP/body status equality, Cache-Control no-store, Pragma no-cache and X-Content-Type-Options nosniff. 401 additionally preserves WWW-Authenticate Bearer. No SQL/provider message, stack, credential, raw key or ownership hint appears.

| Code | HTTP | Exact title / use |
| --- | --- | --- |
| UNAUTHENTICATED | 401 | Authentication required; missing/invalid/duplicate bearer or untrusted principal |
| FORBIDDEN | 403 | Access denied; current caller account/org/membership absent, inactive or ineligible |
| NOT_FOUND | 404 | Not found; valid missing/non-owned grant, or rejected route/method/alias |
| VALIDATION_FAILED | 422 | Request validation failed; header/path/media/body/direct-command structural errors |
| SHARE_TRANSITION_CONFLICT | 409 | Share transition conflict; changed semantics under same identity, incompatible/expired new transition, ordinary conditional loser |
| SHARE_GENERATION_STALE | 409 | Share generation stale; present revocation witness mismatches a compatible unexpired new revoke |
| SHARE_REVOKED | 410 | Share revoked; new action against terminal REVOKED, before stale/expiry checks |
| RATE_LIMITED | 429 | Too many requests; preserve mapping only, enforcement is gateway-owned |
| SERVICE_UNAVAILABLE | 503 | Service unavailable; missing command graph, transaction acquisition, role/lock/clock/context SQL/decode failures |
| INTERNAL | 500 | Internal error; unexpected append/commit/internal failures, sanitized by existing handling |

Revoke's exact operation union is those ten codes. Never return SHARE_EXPIRED, SHARE_LOCKED, create risk/policy/creation-conflict codes or a rotation-specific code. Retain all existing writer rows, especially CURSOR_INVALID → 400 Invalid cursor. Known database guard/material failures retain the existing sanitized classifier; an intentionally injected generic failure is INTERNAL, not success. Do not remap every SQL error indiscriminately to a transition conflict or change create's classifier.

## 4. Identity, replay and state/error matrix

Use the existing canonical serializer (RFC 8785-compatible, UTF-16 sorted object keys, UTF-8) and SHA-256 provider; every digest is lowercase 64hex. Fixed `actionType = "REVOKE"`.

```text
actionRequestDigest = sha256Hex(UTF8(exact accepted Idempotency-Key))
sourceIdentity = {kind:"REQUEST", actionRequestId:exact accepted key}
eventId = "share-transition:" + sha256Hex(canonicalJson({shareGrantId,actionType,sourceIdentity}))
actionFingerprint = sha256Hex(canonicalJson({shareGrantId,actionType,
    reasonCode:validatedReasonOrNull,newExpiresAt:null,actionRequestDigest}))
transitionOutboxDedupe = "share-transition:" + eventId
```

Here shareGrantId is the locked database grant UUID in canonical lowercase text, not public shareId. Identity becomes computable only after owned grant lock. Neither expected witness, trace, generated row UUID, current revision/status/generation nor sampled time enters either identity/fingerprint. Raw key occurs only in ephemeral identity computation, never persisted/logged. Different keys/actions derive different identities; same key with changed exact reason conflicts. Null/absent reason is canonically JSON null. Whitespace variants are distinct accepted semantic strings.

Apply the following order to structurally valid commands after role, locks, current context and owned grant are established. No facts/state writes on any conflict:

| Persisted action/current state | Witness combinations | Outcome |
| --- | --- | --- |
| Same event identity and same action fingerprint | Omitted, equal, stale, or changed since first call | 200 original recorded revoke projection; ignore current-state/witness/time preconditions; no UPDATE/appends/material/provider calls |
| Same event identity but changed valid reason/fingerprint | Any valid witness, including terminal current state | 409 SHARE_TRANSITION_CONFLICT, before terminal check |
| No matching event; REVOKED | Omitted/equal/stale; time before/equal/after expiry; code generation zero or positive | 410 SHARE_REVOKED; terminal precedence over stale/expiry |
| No matching event; EXPIRED | Omitted/equal/stale; any mode/code generation | 409 SHARE_TRANSITION_CONFLICT; never terminal generation bump |
| No matching event; ACTIVE or LOCKED; sampled time >= expiresAt | Omitted/equal/stale | 409 SHARE_TRANSITION_CONFLICT; equality is expired; do not persist EXPIRED or append an expiry fact in this work unit |
| No matching event; ACTIVE or LOCKED; sampled time < expiresAt | Witness absent or equal to locked revocation generation | 200 after exactly one REVOKED transition; revocation generation +1, revision +1; code generation unchanged |
| No matching event; ACTIVE or LOCKED; sampled time < expiresAt | Witness present and unequal (less or greater, including zero vs nonzero) | 409 SHARE_GENERATION_STALE; no writes |

There is no new lower `valid_from` gate or maximum-admission check for revoke. Revoke applies to all four verification modes for ACTIVE and LOCKED, including non-code LOCKED grants: this is not an unlock or code rotation. NONE/DESIGNATED_MOBILE/ORGANIZATION_MEMBER have codeGeneration 0; EXTRACTION_CODE has codeGeneration >=1 and non-null salt/hash. A source revocation generation may be zero or positive. Neither changes the decision except the optional revocation witness. No expected code-generation input is added; code generation and salt/hash are preserved. State/time compatibility precedes the witness after replay misses. Negative/overflow/explicit-null witness and invalid reason remain 422 before this table, not replay bypasses of structural validation.

The broad schema allows several same-state shapes that the service must not use. In particular do not implement REVOKED → REVOKED with another generation increment, EXPIRED → REVOKED, ACTIVE code rotation or non-code unlock. No production sharing policy is consulted for revoke, and no token, code, mobile binding, evidence projection, risk read or external I/O is needed.

## 5. Code-generation-ready interfaces and persistence

Keep all additions nested/non-exported except the existing named command interface members; package layout, five top-level command exports and query interface remain unchanged.

```java
record Revoke(String shareId, String actionRequestId, Optional<UUID> traceId,
        Integer expectedRevocationGeneration, String reason) implements CustomerShareCommand
record Revoked(CustomerShareCommandResult.ShareState state) implements CustomerShareCommandResult
```

Add Revoke to the sealed permits list. Required strings/state/Optional container are non-null; command witness/reason remain nullable for absence. `execute(CustomerShareCommand, ShareCommandCaller)` retains its signature, validates and dispatches Create or Revoke inside exactly one existing command TransactionTemplate execution, preserving current transaction-acquisition and commit-error handling. Use a private `revoke(CustomerShareCommand.Revoke command, ShareCommandCaller caller)` path and helpers in the same service; no constructor change or generic redesign.

Add exactly these repository API members and nested records, as frozen by the full plan:

```java
Optional<ShareGrant> lockGrant(DSLContext transactionDsl, String shareId, ShareCommandCaller caller);
Optional<PersistedTransition> findTransition(DSLContext transactionDsl, UUID grantId, String eventId);
void appendAccessEvent(DSLContext transactionDsl, AccessEvent event);
TransitionResult transitionGrant(DSLContext transactionDsl, UUID grantId,
        int expectedRevision, ShareStatus expectedStatus,
        Optional<Integer> expectedRevocationGeneration, Optional<Integer> expectedCodeGeneration,
        ShareStatus targetStatus, int targetRevision, int targetCodeGeneration,
        int targetRevocationGeneration, Optional<ExtractionCodeMaterial> replacement);

record PersistedTransition(UUID shareGrantId, String eventId, String actionType,
        String actionRequestDigest, String actionFingerprint, ShareStatus status,
        int codeGeneration, int revocationGeneration, int revision, Instant occurredAt) { }
record AccessEvent(UUID id, UUID shareGrantId, Optional<UUID> shareAdmissionId,
        String eventDomain, String eventId, String eventType, String result,
        Instant occurredAt, Optional<String> riskDigest, JSONB payloadRedacted, String payloadSha256) { }
sealed interface TransitionResult {
    record Updated(ShareGrant current) implements TransitionResult { }
    record NotUpdated() implements TransitionResult { }
}
```

All reference/Optional containers and returned containers/results are non-null; validate record invariants consistently with existing repository records. Reuse existing `ShareGrant`'s exact twenty fields, `ActiveCallerContext`, `AuditFact`, `OutboxFact`, `ExtractionCodeMaterial`, enums and methods. No grant updatedAt/createdAt/ownerAccountId/scopeDigest/creationFingerprint columns exist; do not add them. Frozen content_snapshot members remain `content`, `verification`, `creation` JSONB object nodes; no revoke edit to the snapshot or creation fingerprint.

Jooq implementation requirements:

1. `lockGrant`: bound SELECT of the existing explicit twenty `COLUMNS` from sharing.evidence_share_grant, `WHERE share_id = ? AND created_by_account_id = ? AND owner_organization_id IS NOT DISTINCT FROM ?::uuid FOR UPDATE`. Bind selected organization or SQL null; account match is always required, including organization shares. Reuse typed decoder; no wildcard, unfiltered ID-first probe, foreign lock or alternate-owner lookup. Valid absent/wrong account/wrong workspace maps to identical 404 through empty Optional. SQL/decode failure is not a fake 404; fail closed as 503.
2. `findTransition`: read only sharing.share_access_event by share_grant_id, event_domain SHARE_ACCESS and event_id. Explicitly select event identity/type/result/time and payload/hash needed to decode the exact frozen PersistedTransition; it is not a new table/column. Verify type REVOKED corresponds to action REVOKE and valid SUCCESS/redacted fields; malformed/unreadable facts fail closed, not a replay miss. Do not store raw reason or key in access payload, or use an Outbox row as replay authority. No row UPDATE/DELETE to event ledger.
3. `appendAccessEvent`: plain bound INSERT of all eleven event columns, with application-generated row UUID. Explicitly bind sampled timestamp (typed UTC OffsetDateTime with timestamptz cast, as established by prior fixes), JSONB payload and nullable admission/risk columns. Domain/event identity unique constraint is a backstop, not an ON CONFLICT path that swallows changed semantics or an aborted transaction.
4. `transitionGrant`: retain the accepted full-plan signature so witness absence has unambiguous semantics, but implement only the revoke call shape here: target REVOKED, replacement empty, expectedCodeGeneration empty, targetCodeGeneration equal to old, targetRevision/targetRevocationGeneration old+1. No rotate command or replacement-material success branch is authorized. UPDATE changes only status, revocation_generation and revision; WHERE compares id, expected revision, expected source status and optional present generation witnesses. Empty Optional adds no generation predicate. Return explicit twenty-column RETURNING decoded as Updated; zero rows returns NotUpdated. Do not update expires_at, admissions_used, code_salt, code_hash, code_generation or immutable fields, even to their old values. Do not invent targetExpiresAt. Existing role/lock/clock/Outbox/audit methods are reused, not duplicated.

Illustrative revoke SQL shape (add the revocation predicate only when witness is present; bind everything):

```sql
UPDATE sharing.evidence_share_grant
SET status = ?, revocation_generation = ?, revision = ?
WHERE id = ? AND revision = ? AND status = ?
  /* optional predicate shape: AND revocation_generation = ? */
RETURNING <the existing explicit twenty-column projection>
```

The placeholder comment is explanatory plan notation, not an instruction to add inline implementation comments. Witness values come from `Optional.ofNullable(command.expectedRevocationGeneration())`; do not silently replace absence with current generation. The shared expected-code slot remains empty for revoke; no request or policy is added to exercise a future rotation path.

## 6. Exact transaction, lock, update and fact order

One READ COMMITTED transaction/physical command connection, no nested/new transaction, savepoint, retry loop or query datasource fallback:

1. `assertCommandRole(transactionDsl)` performs SET LOCAL ROLE sharing_command_role and SELECT current_user exact assertion before the first command read. Reuse the distinct existing command pool/template/qualifiers and role reset semantics. A revoke never switches to evidence_role.
2. `acquireCreatorWorkspaceLocks(transactionDsl, caller)`: organization ranks 7 → 8 → 9 with bound exact keys `organization_access.organization:<orgUUID>`, `organization_access.membership:<orgUUID>:<accountUUID>`, `customer_iam.account:<accountUUID>`; personal rank 9 only. UUIDs are canonical lowercase 36-character strings, never membership row IDs. SQL is existing pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?, 0)). No foreign row locks, rank 10 idempotency record or later acquisition of an earlier key.
3. `readActiveCallerContext` under those locks, before grant lock. Existing exact customer account/org/membership reads require ACTIVE predicates and existing eligible role registry. Missing/inactive/ineligible is 403 before grant disclosure; SQL/decode failure is 503. Do not infer ownership from subject/evidence or let historical replay bypass current caller checks.
4. `lockGrant` by opaque shareId and caller/workspace; empty is sanitized 404. Then exactly one `sampleDatabaseTime` SELECT clock_timestamp(), non-null and microsecond precision as current foundation. Reuse this sample for expiry and event/audit/Outbox payload timestamps. Never use JVM time, transaction-start now(), request time or a second clock sample as application authority.
5. Derive deterministic eventId/request digest/fingerprint from the locked grant and validated action. `findTransition` first. Matching fingerprint returns Revoked with persisted outcome, immediately, before current terminal/expiry/witness predicates and with no write. Changed fingerprint throws 409 transition conflict, even if grant is now REVOKED or witness changed.
6. On replay miss: REVOKED → 410; incompatible EXPIRED or elapsed ACTIVE/LOCKED → 409 transition conflict; compatible ACTIVE/LOCKED with mismatching present witness → 409 generation stale. Otherwise calculate exactly old revision +1 and old revocation generation +1; preserve code generation/material and all other grant fields. Do not call policies/material providers or make external I/O.
7. Build the exact post-transition access payload/hash below; append one REVOKED access event (owner-local rank after locked grant).
8. Execute one `transitionGrant` conditional UPDATE. Require Updated and verify returned state matches intended exact changes. On NotUpdated, perform **one** bounded same-transaction `lockGrant` reread, then throw the rollback-only stable failure; never return a successful current state, consult the just-inserted event as successful replay, append another event or retry UPDATE. Classify reread terminal REVOKED as 410, incompatible/expired as 409 transition conflict, otherwise mismatching present witness as 409 generation stale, otherwise 409 transition conflict (including absent reread). SQL/decode failure remains controlled unavailable. Every such throw rolls back the already-appended event. Test both empty and present witness zero-row shapes.
9. Append one Outbox through platform.outbox_append, global rank 14.
10. Append one audit through audit.append_fact, global rank 15. Return Revoked only after the outer template commits. Any event/UPDATE/Outbox/audit/commit failure rolls back all four effects, including generations and revision. Do not catch-and-continue after SQL errors or invent a denial/failure access event for these errors.

### 6.1 Exact durable facts

Let `r` be old revision, `g` old revocation generation, `c` unchanged code generation, and `t` the one database-time sample. Canonicalize and hash each exact payload independently. No extra payload fields; JSON null only where explicitly specified.

| Fact | Exact envelope and contents |
| --- | --- |
| Access event | New application row UUID; shareGrantId locked grant UUID; shareAdmissionId absent/SQL null; eventDomain SHARE_ACCESS; eventId as §4; eventType REVOKED (not SHARE_REVOKED); result SUCCESS; occurredAt t; riskDigest absent/SQL null. Exact payload `{actionType:"REVOKE",actionRequestDigest,actionFingerprint,status:"REVOKED",codeGeneration:c,revocationGeneration:g+1,revision:r+1}`. payloadSha256 hashes precisely canonical payload; no timestamp/reason in payload. |
| Conditional grant head | status REVOKED, revocation_generation g+1, revision r+1; every other one of twenty columns unchanged. No admission insert/count change. |
| Outbox | eventDomain SHARE_ACCESS; aggregateType EVIDENCE_SHARE_GRANT; aggregateId locked grant UUID; eventType SHARE_REVOKED; dedupeKey `share-transition:share-transition:<64hex>` literally. payloadRedacted present; payloadCiphertext absent. Exact payload `{shareId,grantId,actionType:"REVOKE",actionRequestDigest,actionFingerprint,status:"REVOKED",codeGeneration:c,revocationGeneration:g+1,revision:r+1,occurredAt:utc(t)}`. Hash only this canonical payload. Existing eight-argument append function has no timestamp argument; do not add one or modify its defaults. |
| Audit | occurredAt t; actorType ACCOUNT; actorId caller UUID; actionCode SHARE_GRANT_REVOKE; objectType EVIDENCE_SHARE_GRANT; objectId locked grant UUID; reasonCode exact validated reason or SQL null. beforeSnapshot exactly `{status:oldStatus,revision:r}`; afterSnapshot exactly `{status:"REVOKED",revision:r+1,reasonCode:validatedReasonOrJsonNull}`. requestId actionRequestDigest; traceId `command.traceId().map(UUID::toString)`, SQL null on direct Optional.empty. Existing eleven-argument audit function. |

Exact replay performs role/locks/current context/grant/time/action reads but adds no access event, UPDATE, Outbox or audit, allocates no event row UUID, increments no revision/generation, and issues no secret. Fingerprint comparison depends only on frozen semantic inputs; generated timestamps/UUIDs cannot make retries conflict.

## 7. Allowed future implementation files and alignment

This section defines the future slice's exact implementation allowlist, **not permission to edit them in this planning task**. No new production/test file is needed. Paths under the indicated existing roots are fully specified; no wildcard ownership.

| Exact file (relative to repository root) | Required scoped change / goal alignment |
| --- | --- |
| `backend/src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommand.java` | Nested Revoke/permits only; typed validated revoke intent. |
| `backend/src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommandResult.java` | Nested Revoked only; reuse ShareState without secrets/discriminator. |
| `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java` | Revoke dispatch/validation/transaction flow/facts/replay/conflicts; preserve Create and constructor. |
| `backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandRepository.java` | Four missing operations and three nested transition/event/result types; keep existing context/creation contracts. |
| `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqSharingCommandRepository.java` | Owner-filtered explicit lock, persisted-event decode, event INSERT, one conditional UPDATE; reuse existing transaction controls and append functions. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | Exact revoke POST, strict transition header then media gate; no broadened route or create-header behavior. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSharesController.java` | Trusted Revoke construction, lazy command execution and flat 200 mapping; leave list/create behavior intact. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandDtos.java` | Nested sharing-import-free RevokeRequest only; reuse state DTO/enums/UTC serializer. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandJsonBinder.java` | Strict closed revoke reader only, shared existing local mapper; preserve create. |
| `backend/src/test/java/com/onecall/archtests/sharing/CustomerShareCommandServiceTests.java` | One accepted new method and private fixture helpers for replay/state ordering. |
| `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java` | Five accepted methods and in-class helpers; real command-role SQL/fact/rollback/concurrency evidence. |
| `backend/src/test/java/com/onecall/archtests/CustomerSharesCommandWebTests.java` | Expand existing four methods in place for revoke HTTP and header/body/error precedence, keeping create/rotate guards. |
| `backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java` | Expand existing exact-route method in place for admitted revoke with context prefix; retain all negative and GET regression assertions. |
| `.github/workflows/ci.yml` | Add six exact names to existing dictionaries and change aggregate source/CI/XML totals/messages only. Preserve build/smoke/firewall/security checks. |
| `backend/README.md` | Current totals/breakdowns/history and route implementation status only, as §9 specifies. |

Read/verify but do not edit: CustomerProblemWriter, ShareCommandException, command service/caller/package-info exports, command/query configuration and properties, all query/evidence/provider files, deterministic test provider, architecture/configuration test sources, backend POM, application YAML, migrations, OpenAPI and docs/12/14/15. No top-level type/file or provider inventory change; the merged command tree has 18 files, not the full-plan future rotation tree's 19. Boundary tests should pass unchanged; do not weaken protected scans, allowed imports, exact query membership, command-file inventory, module count 14 or two discovered dependency edges. Any genuinely necessary additional file requires bounded plan correction with direct evidence before editing, not silent expansion.

Future completion reporting is terminal text; no additional implementation artifact path is authorized by this plan. Any later Controller request for a gate artifact must grant that path explicitly. For the current task the only allowed write remains this new plan file; do not edit Control, README or CI now.

## 8. Exactly one implementation slice

**S2 — Revoke end to end.** Prerequisites: reviewed/accepted bounded plan; PR #22 baseline and existing foundation retained; Java 21/Maven 3.9.9 and disposable PostgreSQL 16/Testcontainers/loopback-capable environment available for future runtime proof. Synthetic fixture roles/credentials/material remain test-only. Missing production providers stay fail closed; no production enablement is a prerequisite for revoke tests.

Implement in this single slice: nested command/result and binder/DTO; exact boundary/controller path; transaction repository operations and service order/state table/facts; six accepted test additions plus in-place web assertions; atomic CI/README inventory update. These are implementation tasks inside one behavioral slice, not separately shippable foundation/web/database slices. Non-goals remain §1; especially no Rotate command/result/provider, code-rotation route admission, expiry persistence or schema/doc contract work.

### 8.1 Exact added test ownership

Use plain `@Test` methods only; loops/table assertions belong inside these methods, not parameterized/repeated/dynamic/conditional tests. No extra method names, skipped tests or removed assertions. Existing source method map expresses this behavior adequately, so **no deviation from the accepted increment is justified**.

| Added method | Required assertions tied to goal |
| --- | --- |
| Surefire `CustomerShareCommandServiceTests.revokeReplaysBeforeStatePreconditions` | Same eventId/fingerprint succeeds before current REVOKED/EXPIRED/time/witness checks; changed witness still replays; changed exact reason conflicts before terminal check. Assert deterministic canonical identity/digest/fingerprint (witness/trace excluded; exact raw-key bytes preserved), use the exact comma-containing key `revoke,raw-key` in a replay/identity assertion that independently recomputes the request digest and transition event identity from that exact raw string and proves replay without writes; current context/ownership before replay, role/locks/clock order, no writes/UUID/material/evidence/policy interactions for replay. Cover direct invalid ID/key/witness/reason before transaction and ordinary new-action REVOKED/expiry/stale order via table loops. Use existing mocked template/repository fixture, without treating mock rollback as PostgreSQL proof. |
| Failsafe `CustomerShareCommandsIT.revokeWritesRegisteredEventAuditAndOutboxAtomically` | Table over ACTIVE/LOCKED and four modes with valid shapes, zero/nonzero source generations and absent/equal witness; exactly one REVOKED event, three-column head change, one exact SHARE_REVOKED Outbox and SHARE_GRANT_REVOKE audit in order. Independently recompute identity/fingerprint/payload hashes and exact key sets from read-back facts; all twenty grant fields compared for immutability. Prove one database-time sample, command role/connection, exact old/new counters/revision, reason omitted/present preserved, trusted trace present and direct Optional.empty audit SQL null. No admission/count/material/evidence-role work. |
| Failsafe `CustomerShareCommandsIT.revokeExactReplayAddsNoFactsOrRevision` | Execute same key/reason again, including changed/absent stale valid witness and later sampled time past expiry. Return original projection and assert byte/semantic durable facts/counts, revision/generations and all grant fields unchanged; no INSERT/UPDATE/Outbox/audit or generated secrets. Exact replay still requires active context/owned workspace, not an authorization bypass. |
| Failsafe `CustomerShareCommandsIT.revokeChangedReasonTerminalExpiryAndStaleWitnessConflict` | Exhaust §4 matrix: changed reason same identity →409; distinct key against REVOKED →410 even with stale witness/elapsed time; EXPIRED and elapsed ACTIVE/LOCKED →409 transition; before expiry both source statuses absent/equal witnesses succeed and lower/higher mismatch →409 stale, including zero. Equality-to-sampled-time expiry boundary must be deterministic (controlled repository clock seam returning a previously read database timestamp or exact fixture equality, no sleeps). Valid missing/wrong account/personal-vs-org/wrong-org grant →404; missing/inactive account/org/membership →403 before grant read; SQL/decode context/lock failure →503. Verify zero-row UPDATE invokes one reread then stable error and event rollback, never successful current state; no second UPDATE. No-effects snapshots for every denial. |
| Failsafe `CustomerShareCommandsIT.revokeConcurrentActionsProduceOneTransition` | Bounded two-connection races for identical key (one transition, both 200 with same projection) and distinct keys (one 200, loser 410 after lock/re-read); retry changed semantics same key yields409, no second append. Separate personal/org cases prove rank9-only vs exact ranks7→8→9, active context then grant lock, one physical command connection per operation and no foreign locks/query-role fallback. Use barriers/latches before lock acquisition, finite Future.get/await timeouts and executor cleanup; never place a rendezvous after a serialized lock. Count deltas exactly one event/update/Outbox/audit and +1/+1 generations/revision. |
| Failsafe `CustomerShareCommandsIT.revokeFailureAtEachFactAppendRollsBackAllEffects` | Faults before/after event INSERT, conditional update (including NotUpdated), Outbox append, audit append and transaction commit after callback; ensure each injected later failure follows real prior writes. Fresh admin connection proves original grant (all twenty fields), no revoke event, no revoke Outbox/audit and no revision/generation drift relative to pre-existing create facts. Clear fault and retry same key proves no poisoned replay residue. Generic failures expose INTERNAL/500 mapping via web method; declared unavailable controls retain503. Reuse existing spy/listener/commit-failure patterns; no rollback by test transaction masquerading as service rollback. |

Nonzero source revocation generations, LOCKED or EXPIRED fixtures may be prepared through valid synthetic owner lifecycle SQL in IT only; preserve V009 guards/ACLs and revisions, do not disable triggers or change migration checks. Invalid server material/guard injection retains the existing classifier; no production policy parameters are chosen.

### 8.2 Existing test bodies to expand, no method additions

- `CustomerSharesCommandWebTests.revokeAndRotateMapClosedResponseUnions`: replace only revoke's old all-404 branch with first/replay 200 closed ShareState, selected personal/org caller and trusted trace assertions. Send one `Idempotency-Key` value `revoke,raw-key` through the actual filter/controller path and assert the captured `CustomerShareCommand.Revoke.actionRequestId()` equals that exact string. Retain all six exact shared writer domain rows/common status/header tests; keep code-rotations unadmitted 404. After mapping revoke to its command path, scope `verifyNoInteractions(fixture.commands)` only to the retained rotate-only rejected 404 branch, using an isolated rotate fixture; it must not cover or mask revoke command invocation. Drive actual command exception mapping through controller for revoke's exact union and prove no code/token/hash/salt/key leakage.
- `CustomerSharesCommandWebTests.invalidCommandAndMissingServiceMapToProblemResponses`: preserve every create case; add header missing/blank/128-vs129 code points/identical and distinct duplicates/case-insensitive header-name/raw whitespace/Unicode identity, body and null/witness/reason matrix, media cases, invalid path, missing command bean/BeansException and trusted workspace shape. Invalid header beats malformed JSON/media/workspace; no auth beats header; invalid binding never resolves service/provider or reaches DB. Test invalid UTF-8/NUL safety consistently with the existing binder. Header/body/provider precedence is tested using invocation/side-effect counters, not status alone.
- `CustomerSharesCommandWebTests.createUsesTrustedCallerAndMapsCreatedAndReplay` and `commandRoutesDoNotChangeShareListRoute`: retain create header-ignore, microsecond expiry, all-mode create/replay and list provider/cursor behavior. Expand only as needed to assert revoke uses command provider and list remains query-only.
- `CustomerSharesWebTests.exactGetAndPostRoutesPassHighestPriorityFilter`: separate exact revoke POST from the existing rejected-method loop; authenticated JSON request with valid key must reach the revoke controller (controlled 503 is correct when command bean is deliberately absent), and an available test command bean path proves 200 where configured. Preserve /pa12 context prefix, exact GET/create cases, filter priority and no verifier calls for rejected aliases. Preserve HEAD/OPTIONS/GET/PUT/PATCH/DELETE transition 404, code-rotations POST404, raw URI/encoding/matrix/extra-segment/trailing-slash defenses, sanitized HEAD body and security headers. Keep the method name unchanged in source and CI.

Run unchanged architecture, module, query, customer-context, session, configuration, `/me`, sharing-list, role ACL and migration regression suites. Their existing methods prove no export/file-count/pool/role/query-transaction drift. No new web, boundary, configuration or fixture methods/classes are needed.

## 9. Inventory, CI, README and future verification

Read-only source enumeration during planning found exactly the stipulated 77/108 baseline. Exact post-slice class counts:

| Surefire class | Methods |
| --- | ---: |
| CustomerMeWebTests | 8 |
| CustomerSharesCommandWebTests | 4 |
| CustomerSharesWebTests | 10 |
| CustomerWebConfigurationTests | 6 |
| OneCallApplicationTests | 1 |
| SharingCommandPersistenceConfigurationTests | 2 |
| SharingPersistenceConfigurationTests | 2 |
| BootstrapDependencyBoundaryTests | 1 |
| CustomerApiBoundaryTests | 3 |
| ModuleBoundaryTests | 8 |
| SharingQueryBoundaryTests | 1 |
| CustomerShareCommandServiceTests | **5** |
| CustomerContextServiceTests | 6 |
| CustomerSessionBoundaryTests | 7 |
| CustomerShareCursorCodecTests | 6 |
| CustomerShareQueryServiceTests | 8 |
| **16 classes** | **78** |

| Failsafe class | Methods |
| --- | ---: |
| PostgresContainerIT | 64 |
| SharingQueryServiceIT | 10 |
| CustomerMeIT | 10 |
| CustomerSharesIT | 8 |
| SharingRoleLeastPrivilegeIT | 8 |
| CustomerShareCommandsIT | **13** |
| **6 classes** | **113** |

CI changes must be atomic with source: add only `revokeReplaysBeforeStatePreconditions` to the existing fully qualified service-test dictionary and the five exact names in §8.1 to the CustomerShareCommandsIT dictionary. Preserve all current method identities/duplicate-case/conditional-test/source-class checks. Update the initial Surefire total assertion/message 77→78, exact_inventory Surefire77→78 and Failsafe108→113, and final printed text to `PA14 revoke slice exact source/CI/XML inventory passed: Surefire 78; Failsafe 64+10+10+8+8+13=113; combined 191; zero skips/errors/failures`. Preserve all 64 PostgresContainerIT names and the six-class Failsafe configuration; POM needs **no change** because it already includes this IT class.

Require source=CI=XML equality of exact fully qualified class/method pairs, not merely aggregate totals. No extra/missing classes, duplicate test cases, parameterized/repeated/dynamic/conditional tests, skipped cases, failures or errors. Surefire service class becomes 5 and IT becomes13; every other class/method set remains equal to merged baseline. Explicitly reject accidentally importing rotation's sixth service test/five ITs or full-plan 79/118/197 totals.

README ownership: current inventory/run-breakdown clauses at current lines40/291/300/322 become 78/113/191 with `64+10+10+8+8+13`; present-tense run description names PA14's cumulative13 command ITs (8 create+5 revoke), not eight or18. Preserve historical slice1 77/108/185 and its ten/eight additions as explicitly historical, and append the revoke increment +1/+5; update cumulative history clause near305 through revoke. Change current route statement to create+revoke implemented/admitted, code rotation deferred. Keep historical PA12 159, PA9 S3=64 near313 and all other frozen migration/ACL history unchanged. No docs/12/14/15/OpenAPI edit or new standalone docs is required for implementation.

Future implementation validation, **not run by this planning task**, from `backend` in the established Java21/Maven3.9.9/Docker environment:

```sh
./mvnw -B -ntp -Dtest=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify
./mvnw -B -ntp -Pcontainer-smoke verify
```

Expect respectively 5+4+10=19 focused Surefire methods, 13 targeted command ITs, and full78/113/191 with zero skips/errors/failures. `verify` also executes its configured Surefire set; use fresh report directories/full final run so targeted reports cannot fake the final XML inventory. Run the existing CI exact-inventory Python block against resulting XML/source/dictionaries, then the existing default/web packaged-JAR smokes and production-JAR firewall82 probes without weakening them. Preserve the existing CI runtime prerequisites and timeout. If loopback/Docker permissions prevent execution, report exact environment failure and unexecuted proof; do not claim a green/accepted checkpoint, remove tests or change expectations.

Static completion checks: `git diff --check`; exact implementation allowlist; no protected migration/OpenAPI/doc/config/query/provider changes; exactly six added @Test methods; names source=CI; 78+113=191; no new provider/export/class; rotate still404; no extra header/body fields, SQL wildcard, foreign lock, second transaction, platform idempotency, production value or secret leakage. For Windows worktree-pointer issues use read-only `GIT_DIR`/`GIT_WORK_TREE` overrides rather than rewriting `.git` or configuration.

## 10. Docs decision, residual risks and stop rules

- Documentation decision: this new bounded plan records the implementation contract; future README inventory/status edits are sufficient. Accepted full-plan sharing identities/facts and frozen docs/OpenAPI/V009/V010 already support revoke. Do not amend authoritative product docs, accepted full plan or execution Control as incidental cleanup.
- Backend/Database test owner: role reset, typed SQL/event decoding, locks, witnessed UPDATE, concurrent winner and append/commit rollback need actual future PostgreSQL16 evidence. Static planning does not prove runtime behavior. Existing command-only pool/ACL and query read-only separation must remain intact.
- Security/Product/Sharing owners retain unapproved production policy/KDF/mobile normalization/HMAC/OTP/TTL/provider/provisioning questions. Revoke requires no new policy/material choice and must not turn fail-closed creation into production enablement. Gateway rate limiting and short-lived credential revocation window remain out of scope.
- Evidence-owner lifecycle/snapshot-naming questions classified in prior reviews stay deferred; revoke only reuses immutable grant metadata. No evidence projection or recording-lifecycle redesign is necessary.
- Environment owner retains loopback/Docker and Windows Git-pointer concerns. They limit evidence, not product scope. Pre-existing Control modification and `.codex` stay untouched. No current product/schema/policy blocker or test-map deviation was found by direct inspection.
- Stop `blocked` if implementation would require a product/schema/policy decision or contradiction with frozen action/fact/status rules. Stop `needs-more-evidence` if exact contract, ownership or test evidence cannot be safely established. Do not reinterpret SQL permissiveness as product authorization, widen files or add future slices to hide a gap.

This is minimal because existing trusted session, query/command pools, role controls, locks/context readers, canonical serializer, error vocabulary, state DTO and append functions are retained. Only the missing revoke path and its persisted transition collaborators are added. One new service method and five database methods plus existing web assertions cover the complete behavior; no foundation rework, independent web/database slice or rotation scaffolding is needed. All files/tests map to that single endpoint and no other goal.

## 11. Completion reporting and current planning boundary

Future implementation completion report: provider/model; slice S2 and exact changed paths; command/status/response mapping summary; six added method names and full source/CI/XML class/method totals; each executed command and result; fact/rollback/concurrency evidence; static protected-file check; regression/smoke/firewall results; environment limitations; residual owners and blockers; explicit no remote/gate action. Do not claim acceptance from author completion.

Current planning completion report: provider/model; artifact path; slice count1; expected78 Surefire+113 Failsafe=191; blocker none (or exact bounded stop status); `status: plan-complete`. Planning author writes only `docs/gateflow/pa14-sharing-command-revocation-plan.md`, performs read-only/static inspection, runs no Maven/Docker/package build, creates no implementation artifact, and finishes without starting implementation or mutating any external state.
