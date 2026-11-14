# PA34 Operations activation-history read foundation — cumulative plan

Original task: `pa34-operations-activation-history-plan-gpt-20261115-01`; current bounded fix retry: `pa34-operations-activation-history-plan-fix3b-gpt-20261115-01`. Cumulative correction history: `pa34-operations-activation-history-plan-fix-1.md`, `pa34-operations-activation-history-plan-fix-2.md` and `pa34-operations-activation-history-plan-fix-3.md` under `docs/gateflow/`; these are gate artifacts outside the future S1 implementation allowlist.

Work unit: `PA34-operations-activation-history-read-foundation`

Date: 2026-11-15 (Asia/Shanghai)

Gate: **plan fix only; not implementation, re-review, acceptance or production readiness**. Accepted Control findings PA34-PLAN-REV-01..06, PA34-PLAN-FIX-01 and the clarified 23-row future S1 boundary are incorporated below; independent Controller-scheduled final A/B re-review remains required.

Baseline: merged PA33 `e423b4b7e3f0d673bd2ad3fa579c36ca585af13c`; managed PA34 worktree only.

## 1. Binding goal, motivation and traceability

The accepted Goal Confirmation in `docs/gateflow/project-execution-control.md:15`–`:32` is authoritative. Provide authenticated Operations employees a metadata-only history of persisted activation transitions through **only** `GET /api/v1/operations/activations` / `listOperationsActivations`. Success means the exact frozen `ActivationPage`, eleven-field `ActivationSummary`, current ACTIVE ADMIN/OPS authority, stable opaque continuation, and proof of read-only owner-local persistence without changing frozen inputs.

The following labels are traceability shorthand, not additional goals or acceptance criteria:

| Label | Confirmed-goal obligation |
| --- | --- |
| G1 | Only the frozen endpoint, page and eleven safe item properties; no association invention. |
| G2 | Independent employee audience, current lifecycle/fixed authority, exact route concealment and preserved existing surfaces. |
| G3 | Only cursor/limit; stable keyset and integrity-protected endpoint-bound opaque continuation. |
| G4 | One authoritative read-only snapshot, owner-local transition SELECT, asserted/restored roles, no side effects. |
| G5 | Frozen error set/precedence, sanitized failures and fail-closed dependencies/configuration. |
| G6 | Frozen contracts/V001–V017, exact inventories and mandatory implementation validation, no readiness claim. |

Every design row, planned path, test and validation below carries these labels. There is one behaviorally verifiable implementation slice, **S1: authorized activation-history page and continuation** (G1–G6). It includes the whole vertical request path and its evidence. No independent user-visible partial endpoint exists before authority, projection and pagination all work, so splitting by controller/port/database/test ownership would create artificial increments.

### Scope exclusions and minimality

No other Operations reads or commands; no IVR sessions/detail, orders, catalog, trial-policy, audit, customer/public or mutation endpoint; no filter, search, offset, count/total, export, UI, playback, new association field, audit/Outbox fact, cursor table, retry workflow, IdP/session issuance, credential provisioning, live acceptance or production-readiness claim. No OpenAPI/docs/14/docs/15/docs/17/V001–V017 edit and no V018, schema, role, membership or privilege delta. No remote mutation, staging, commit, push, PR, merge, approval, ready action or review initiation.

Minimality decisions: retain existing employee verifier, principal/problem writer and read pool/transaction/context; use one narrow inbound reader and one SELECT rather than five-owner subject enrichment; publish the new read types through the **existing** named read interface; isolate cursor purpose/AAD while reusing the existing subject-read key ring without generalizing subject cursors or introducing a secret; retain existing ACLs and accept bounded query failure rather than adding an index. This is not an association aggregator or new Operations framework.

## 2. Direct inspection evidence

All citations refer to current pre-implementation source, not proposed line numbers. Paths under `backend/src/main/java/com/onecall/core/` are abbreviated as `main/` only in this evidence table; test paths under `backend/src/test/java/` as `test/`.

| Evidence | Current symbol/line and consequence |
| --- | --- |
| Scope | `docs/gateflow/project-execution-control.md:20`–`:31`: exact goal, route, keys, read semantics, errors and freeze. |
| Architecture | `docs/12-technology-stack-and-system-architecture.md:32`, `:55`–`:59`, `:134`, `:243`: one PostgreSQL transaction boundary, Java/jOOQ/Flyway/Hikari, authoritative facts and explicit owner query seams. |
| Data contract | `docs/14-production-data-model-and-flyway-schema-spec.md:366`, `:415`, `:483`, `:2080`–`:2120`: activation owner, append-only transition facts, projected columns and constraints. |
| API contract | `docs/15-production-openapi-surface-spec.md:108`, `:258`–`:270`; `api/openapi/onecall-v1.openapi.yaml:1358`–`:1414`, `:4023`–`:4044`, `:4356`–`:4361`, `:4929`–`:4985`: operation, sort, optional cursor/limit, errors, exact page/item schemas. |
| Security contract | `docs/17-production-security-design-spec.md:113`, `:144`–`:147`, `:596`; OpenAPI `:4000`–`:4004`: independent employee trust surface, fixed roles, metadata redaction/fail-closed. Mutation audit requirements do not authorize read-path writes. |
| Route admission | `main/operations_iam/internal/web/OperationsBoundaryFilter.java:23`–`:69`, especially `:30`–`:37`: GET subject list/detail and POST suspension/resumption templates, raw-URI equality, authentication before MVC; buffering prevents unsanitized partial errors. |
| Registration | `main/operations_iam/internal/web/OperationsWebConfiguration.java:14`–`:26`: web-only, REQUEST dispatcher, Operations wildcard and highest precedence; no new registration needed. |
| Existing controllers | `main/operations_iam/internal/web/OperationsSubjectsController.java:27` onward and `OperationsSubjectReadController.java:23`–`:88`: preserve two POSTs/two GETs; provider cardinality, trusted principal, raw query and explicit JSON/problem writing are reusable patterns. |
| Necessary lockstep | `main/customer_iam/internal/web/CustomerBoundaryFilter.java:48`–`:62`: exact Operations pass-through whitelist precedes customer auth. New GET must be added here as well, otherwise customer filter can conceal/re-authenticate it. No other customer/public change is justified. |
| Employee verifier | `main/operations_iam/internal/security/EmployeeBearerAuthenticator.java:12`–`:37`: single Authorization, exact audience, exactly one verifier, safe verifier errors. `EmployeeSessionVerifier.verify` and `VerifiedEmployeeSession` supply only server-derived employee identity/audience. No packaged production verifier is added. |
| Authority precedence | `main/operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java:60`–`:67`, `:153`–`:194`: transaction/context then employee SELECT and current status/role, then query/codec/owner providers; missing employee 401, disabled 403, invalid authority row 503. |
| Snapshot/role guard | `main/operations_iam/subjectread/OperationsReadContext.java:12`–`:91`: enter, scoped literal owner roles, exit; same connection/backend/session, read-only REPEATABLE_READ, timeout settings, rollback-only on restoration failure, cleared thread-local. |
| Infrastructure | `main/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java:32`–`:151`, `:153` onward: lazy persistence-only graph, dedicated credentials, prepared/no-log jOOQ, REQUIRES_NEW/read-only/RR/5s template and guarded pool cleanup/eviction. Reuse exact qualified beans, not a new pool. |
| Cursor baseline | `main/operations_iam/internal/subjectread/OperationsSubjectCursorCodec.java:19`–`:97`: AES-256-GCM, purpose/header AAD, random nonce, canonical base64url, timestamp+UUID+filter digest and filter-mismatch error. `OperationsSubjectReadProperties.java:13`–`:50` validates key ring and redacts secrets. Copy the small endpoint-specific mechanism, not its subject/filter semantics. |
| Owner pattern | `main/activation_consent/internal/operationsread/JooqOperationsActivationReader.java:21`–`:46`: owner implements Operations inbound port, uses qualified shared DSL/context and `context.scoped(Role.ACTIVATION, ...)`, safe owner-local projection. Existing subject-state reader stays unchanged. |
| Existing privilege | `backend/src/main/resources/db/migration/V007__activation_and_consent.sql:101`–`:162`, `:207`–`:212`: all eleven facts already exist; activation role already has table SELECT. Existing append-only mutation rejection is `:164` onward. No grant/migration is necessary. |
| Owner dependency | `main/activation_consent/package-info.java:1`; `main/operations_iam/subjectread/package-info.java:1`: activation already imports `operations_iam::operations-subject-read`. Reuse that named interface and keep allowedDependencies byte-identical. |
| Boundary guards | `test/com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java:19`–`:82`, `OperationsSubjectReadBoundaryTests.java:17`–`:122`, `ModuleBoundaryTests.java:33`–`:150`: current route/source/frozen assertions and exact allowed graph/12-edge target multiset. New endpoint guard supplements, not weakens, these assertions. |
| Web/fixture patterns | `test/com/onecall/archtests/operations/OperationsSubjectReadTestProviders.java:31`–`:95`; `OperationsSubjectReadWebTests.java:55`–`:140`: explicit MockMvc servlet/raw paths, both filters, real service and controllable providers. |
| PostgreSQL evidence pattern | `test/com/onecall/archtests/container/OperationsSubjectReadFixture.java:36`–`:128`, `:193`–`:204`, `:254`–`:285`; `OperationsSubjectReadsIT.java:36` onward; `OperationsSubjectReadRoleLeastPrivilegeIT.java:50`–`:89`, `:120`–`:242`: synthetic employee sessions, real HTTP/PostgreSQL, fingerprints, role/rollback/cleanup probes. `CustomerBusinessNumbersFixture.java:32`, `:58`–`:79` supplies `postgres:16`, Flyway and test provisioning. |
| Inventory/build | `backend/pom.xml:108`, `:180`–`:211`; `.github/workflows/ci.yml:59`, `:1425`–`:1548`, `:1620`–`:1794`: 29 exact IT includes, PA33 named additions and source/XML/token arithmetic; `:1796`, `:1899`, `:2351`–`:2410`: packaged smokes/firewall/A1–A5. |

The Control evidence sentence says “five PA32/PA33 subject routes”; actual annotations/filter prove **four operation templates** (two POST and two GET). Preserve those four and add one GET = five templates; do not invent a sixth route or edit Control at this gate. The OpenAPI prose mentions consent/trial associations and x-a0 model references, but its closed eleven-field schema plus Goal Confirmation expressly preclude association properties and joins.

## 3. Public contract and error precedence (G1–G5)

### Exact result mapping

`ActivationPage(List<ActivationSummary> items, String nextCursor, boolean hasMore)` has only those three required JSON keys. Copy the items list defensively and force `nextCursor` serialization even when null. Empty/tail: `nextCursor:null`, `hasMore:false`. A continuation exists iff the single fetch produced more than limit rows; only returned item number limit supplies the next anchor.

| JSON property, in record order | SQL column | Java representation and returned-row invariant |
| --- | --- | --- |
| transitionId | id | UUID, non-null |
| subjectId | subject_id | UUID, non-null |
| statusEventId | status_event_id | String, nonblank, 1–128 Unicode code points |
| fromStatus | from_status | String, INACTIVE/ACTIVE/SUSPENDED |
| toStatus | to_status | String, INACTIVE/ACTIVE/SUSPENDED |
| fromRevision | from_revision | public primitive int; nullable Integer in raw port mapping, >=0 after validation |
| toRevision | to_revision | public primitive int; nullable Integer in raw port mapping, >=1 and long arithmetic `to = from + 1` after validation |
| sourceChannel | source_channel | IVR/MINIAPP/WEB/OPERATIONS |
| sourceActionDomain | source_action_domain | ACTIVATION/IVR_CONFIRM/OPERATIONS |
| reasonCode | reason_code | String, nonblank, 1–64 Unicode code points |
| occurredAt | occurred_at | Instant from OffsetDateTime, non-null PostgreSQL-microsecond precision; ISO date-time JSON |

Separate database shape from domain legality. V007's frozen `ck_activation_transition_matrix` plus `ck_activation_transition_channel_shape` admit nine database-persistable shapes: INACTIVE→ACTIVE and ACTIVE→SUSPENDED each with MINIAPP/ACTIVATION, WEB/ACTIVATION, IVR/IVR_CONFIRM or OPERATIONS/OPERATIONS, and SUSPENDED→ACTIVE only OPERATIONS/OPERATIONS. docs/14's sole-owner-service contract permits exactly **five domain-legal** shapes: INACTIVE→ACTIVE via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION; ACTIVE→SUSPENDED via OPERATIONS/OPERATIONS; SUSPENDED→ACTIVE via OPERATIONS/OPERATIONS. The remaining four DB-shape-only combinations are domain-impossible integrity failures: INACTIVE→ACTIVE via OPERATIONS/OPERATIONS, and ACTIVE→SUSPENDED via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION. Do not change V007 or any migration to narrow its checks.

The owner reader maps SQL columns only into raw `TransitionFacts` (section4.1), preserving NULL, without field/domain validation or wire-record construction. The service exclusively validates every raw fact (including lookahead), the five domain-legal shapes, ordering/seek and page bounds, then constructs public `ActivationSummary` with primitive `int` revisions. No raw-fact constructor rejects malformed values: stubbed ports must be able to inject them. Do not require unprojected actor/consent facts or invent reason/event formats. Nonblank and lengths mirror database semantics; do not trim or transform returned text. OpenAPI string `maxLength` bounds are Unicode code-point bounds, not Java UTF-16 unit counts; use `codePointCount` and PostgreSQL-equivalent blank checks, preserving valid astral characters. Specifically, V007's `btrim(text) <> ''` trims U+0020 spaces, not every character Java `isBlank` recognizes. A null/malformed row, overflow, invalid enum/pair/domain shape, duplicated transition, bad sort/seek or excessive result count fails the entire page with sanitized 503, including a malformed lookahead row. No partial success or dropping corrupt rows.

Never SELECT/serialize `activation_id`, source action event identity, status event domain, request fingerprint, actor IDs, consent ID, trial, account/employee payload, raw number, storage/media/credential fields. `subjectId` and `statusEventId` are the schema-authorized identifiers, not new associations.

### Admission and parsing

Add equality with `/api/v1/operations/activations` only to the existing **GET** read disjunction, and the identical GET/equality condition in CustomerBoundaryFilter's pass-through. Preserve contextPath+servletPath==raw requestURI. No prefix/suffix matching or decoding/canonicalizing into admission. No path variables on this endpoint. HEAD/OPTIONS/POST/PUT/PATCH/DELETE/TRACE and every unadmitted path remain concealed 404 at the boundary, not new activation-operation errors. Preserve existing POST header/body rules and the two subject GETs.

Allowed query names exactly `{cursor, limit}`. Map values defensively; unknown names, null values and zero/multiple values are 422. Empty cursor, >512 characters, invalid alphabet/envelope or a supplied malformed cursor are 400 after shape/limit/Accept validation and usable key config. A duplicated cursor is shape-invalid 422, not 400. An omitted cursor means first page. Limit omitted =>20; otherwise existing lexical convention `[0-9]{1,3}` and value 1–100, no signs/decimal/exponent/space. Leading zeros within that lexical bound are accepted as in PA33; there is no new canonical integer requirement. Accept handling copies the existing subject-read JSON-compatible quality/charset validation: unsupported/malformed/effective q=0 =>422; absent Accept accepted. No extra Content-Type requirement for GET and no new query/header API.

### Deterministic order of failure

1. Exact route admission/concealment precedes authentication (G2).
2. Existing single employee Authorization and verifier path: missing/malformed/duplicate/customer/wrong-audience/rejected bearer =>401; absent/ambiguous/uncreatable/unavailable verifier or malformed verification result =>503. Never parse query first (G2/G5).
3. Trusted OperationsRequestPrincipal and exactly one service provider; absent/ambiguous/uncreatable infrastructure =>503. Transaction/authority infrastructure necessarily must be available to inspect lifecycle; no activation reader/cursor dependency is resolved here (G2/G5).
4. One read transaction/context, current employee `id,status,role,revision` under operations role. Missing employee=>401, DISABLED=>403; ACTIVE and exactly ADMIN/OPS accepted. Null/unknown/malformed persisted authority, unsupported role, invalid revision/identity=>503 as PA33's integrity rule, never permission granted. Rejected lifecycle precedes query and activation dependencies (G2/G4/G5).
5. Request shape, limit and Accept=>422. Then resolve/validate the existing shared subject-read cursor key ring, even on first/empty pages=>503 for invalid/missing keys. Then decode supplied cursor=>400 CURSOR_INVALID. Then resolve exactly one owner reader=>503 when missing/ambiguous/uncreatable (G3/G5).
6. Fetch/project/validate/encode in that transaction; all database/connect/timeout/transaction/provider/owner-integrity/cleanup failures=>503. A known allowed RATE_LIMITED failure=>429; do not build a new rate limiter. Preserve allowed typed 400/401/403/422/429/503/500 failures; normalize any shared-code failure outside this operation's exact seven-code set to INTERNAL/500. Unknown application exceptions=>500, without messages/causes (G5).
7. Commit/completion and pool-cleanliness verification must finish successfully **before** response serialization; failures override prospective 200/empty/tail responses with 503. Existing buffered boundary and sanitized controller handlers apply (G4/G5).

Use existing `OperationsSubjectReadService.Code/Failure` and `OperationsProblemWriter.write(readCode,trace)` rather than another public error vocabulary. This endpoint must never emit shared enum's CURSOR_FILTER_MISMATCH or NOT_FOUND. Problem JSON exactly `type,title,status,code,traceId`; fixed title, UUID trace, no-store/no-cache/nosniff, 401 Bearer challenge; no cookies, bearer, employee identity, cursor, SQL, JDBC URL, credentials, exception text or HTML. Concealed 404 remains existing boundary behavior outside the admitted operation's error set.

## 4. Exact design and responsibilities

### 4.1 Names and seams (G1–G6)

All names below are frozen planning identities, not permission to implement now.

| Type/symbol | Allowed responsibility and alignment |
| --- | --- |
| `operations_iam.subjectread.OperationsActivationReadService.list(Caller, RawListRequest)` | Single application use case, no detail/mutation method (G1/G2). Nested records `RawListRequest(Map<String,List<String>> query,List<String> accept)`, `ActivationSummary` in section 3's exact order/types, `ActivationPage` as above. Raw request is immutable with redacted toString (G1/G5). Reuse lifecycle `Caller` and shared read failures. |
| `operations_iam.subjectread.OperationsActivationReadPorts` | Namespace with exactly one nested interface `ActivationHistoryReader.fetch(Optional<Anchor> anchor,int fetchSize)` returning `List<TransitionFacts>`; nested records `Anchor(Instant occurredAt,UUID transitionId)` and `TransitionFacts(UUID transitionId,UUID subjectId,String statusEventId,String fromStatus,String toStatus,Integer fromRevision,Integer toRevision,String sourceChannel,String sourceActionDomain,String reasonCode,Instant occurredAt)`, both with redacted toString. All eleven raw fields are nullable; carrier has no semantic validation or unboxing. No public wire record return, broad repository/entity or arbitrary SQL interface (G1/G3/G4/G5). |
| `operations_iam.internal.activationread.OperationsActivationReadRequest.parse(RawListRequest)` | Immutable record `(int limit,String cursor)`, exact query/Accept rules; private shape/value/accept helpers, redacted toString. No subject filters/digest or dependency access (G3/G5). |
| `OperationsActivationCursorCodec` in same package | Constructor from existing `OperationsSubjectReadProperties`; require its cursor and reuse `validatedKeys`/key decoding/redaction. `encode(Anchor)` and `decode(String)` only; private header/cipher helpers; purpose-isolated AEAD authenticating fixed sort and constant empty-filter digest, never limit (G3/G5). |
| `OperationsActivationReadConfiguration` in same package | Lazy persistence configuration with only bean method `operationsActivationReadService(...)`. Reuse existing `operationsSubjectReadProperties` provider and qualified PA33 transaction/DSL/context. No new property class/binding/bean, DataSource/role/grant creation, datasource credentials or dependency (G4/G6). |
| `DefaultOperationsActivationReadService` in same package | Implements `list`; internal `read`, `authority`, `one`, `checkBudget`, `validate`, `before`, `unavailable` helpers patterned narrowly on subject read. Constructor takes TransactionTemplate, DSLContext, OperationsReadContext, ObjectProvider<OperationsSubjectReadProperties>, ObjectProvider<ActivationHistoryReader>; package-private overload adds LongSupplier for deterministic budget tests. Current employee authority then parse/shared config/cursor/one fetch; exclusively validates raw TransitionFacts before unboxing revisions and constructing wire summaries/page inside transaction. No owner table SQL or subject enrichment (G1–G5). |
| `operations_iam.internal.web.OperationsActivationReadController.listOperationsActivations(Principal,HttpServletRequest,HttpServletResponse)` | Exactly one @GetMapping; ObjectProvider<OperationsActivationReadService>, existing problem writer; private employee/service/query/success helpers and known/unexpected handlers equivalent to PA33. Explicit JsonMapper dates-as-ISO, complete byte serialization before body write, no generic bean/entity serialization (G1/G2/G5). |
| `activation_consent.internal.operationsread.JooqOperationsActivationHistoryReader.fetch(Optional<Anchor>,int)` | Implements only ActivationHistoryReader. Lazy persistence bean `operationsActivationHistoryReader`, exact qualified subject-read DSL/context. Scoped ACTIVATION role and sole transition SELECT. Map eleven SQL columns only to immutable raw TransitionFacts list, `row.get(...,Integer.class)` for revisions (never primitive zero), nullable OffsetDateTime to nullable Instant. No raw-field/domain validation, unboxing or ActivationSummary construction; service owns integrity. JDBC/conversion failures propagate as sanitized dependency503. No other base table or owner service (G1/G4/G5). |

The existing `operations-subject-read` named interface is the inbound Operations **read** seam, not a new endpoint promise. Adding these two public source files there needs **no** package-info or module allowlist change. Existing `OperationsSubjectReadPorts` nested port list stays exactly five, and its service remains list/detail only. Reuse `OperationsReadContext` unchanged, including its shared Failure type. No circular `operations_iam -> activation_consent` dependency is introduced.

### 4.2 Configuration and cursor wire format (G3/G5/G6)

Reuse existing read infrastructure enabled by profile `persistence` and `onecall.operations-subject-read.enabled=true`; web controller remains profile `web`/servlet. New configuration and reader use that same persistence/enabled condition and are lazy. The exact qualified beans are `operationsSubjectReadTransactionTemplate`, `operationsSubjectReadDsl`, `operationsSubjectReadContext`; the existing `operationsSubjectReadDataSource` remains the only read pool. No application YAML changes or new enabled/datasource property are needed. Resolve existing `operationsSubjectReadProperties` for cursor validation only after authority/query validation; this does not instantiate the subject service or subject cursor codec. The activation endpoint requires the already-authorized shared read key ring even on first/empty pages; missing/invalid ring fails closed503. No new operator-visible secret is introduced.

Row08 `JooqOperationsActivationHistoryReader` declares `@Component("operationsActivationHistoryReader")`, `@Profile("persistence")`, `@ConditionalOnProperty(prefix="onecall.operations-subject-read", name="enabled", havingValue="true")` and `@Lazy` directly on that owner class, matching the existing owner-reader registration pattern. It is not declared as an `@Bean` in `OperationsActivationReadConfiguration` or any Operations configuration; Operations depends only on the existing named read interface, not the owner's internal class. No duplicate registration or reverse module edge is permitted.

The subject codec cannot be reused directly: it binds createdAt/subjectId plus six subject filters and emits CURSOR_FILTER_MISMATCH. Reuse its validated key material/configuration, not its codec or subject-filter payload. Distinct purpose/AAD and random nonces separate the activation codec without a generic cursor framework or new production key values. Only the existing ring is read:

```text
onecall.operations-subject-read.cursor.active-key-id
onecall.operations-subject-read.cursor.keys[0].id
onecall.operations-subject-read.cursor.keys[0].material
onecall.operations-subject-read.cursor.keys[1].id
onecall.operations-subject-read.cursor.keys[1].material
onecall.operations-subject-read.cursor.keys[2].id
onecall.operations-subject-read.cursor.keys[2].material
```

Reuse unchanged `OperationsSubjectReadPersistenceConfiguration.operationsSubjectReadProperties` and `OperationsSubjectReadProperties.Cursor.validatedKeys()`/`KeyEntry.bytes()`: 1–3 contiguous entries, unique key IDs `[A-Za-z0-9_-]{1,32}`, unique decoded materials, active ID present; each material canonical unpadded base64url of exactly 32 bytes (43 chars). Existing binding/validation rejects gaps, incomplete entries, duplicate IDs/materials, >3 entries, malformed indexed property names and invalid/missing active ID/material. Keep its enumerable-property checks, precedence and redaction unchanged. There is no activation property class, separate prefix/binder or separate operator secret; activation-style properties cannot configure/fallback the shared ring. No default/fallback key. Construction/property resolution/config validation fails closed503, not an authentication bypass or startup crash of unrelated profiles. All property/key/codec/request/anchor/raw-fact toString outputs redact contents; no secret value is placed in code, docs, logs or test failure messages. Synthetic fixture-only bytes are not production credentials.

Wire format, using the current codec's explicit framing:

* ASCII canonical base64url without padding, 1–512 characters; version byte `1`, one-byte key-ID length, 1–32 ASCII key-ID bytes, random 12-byte nonce, 44 encrypted bytes (28-byte anchor plus 16-byte tag). Total binary size `58 + keyIdLength`; maximum unpadded base64url length120 (90 bytes at ID length32).
* Plain anchor: signed epoch seconds (8 bytes), nanos (4), UUID most/least significant bits (16), all big-endian. No digest in encrypted payload, limit, offset, plaintext timestamp/UUID, SQL key or identity in cleartext. Key identifier is nonsensitive routing metadata, not an anchor.
* `AES/GCM/NoPadding`, 256-bit key, 128-bit tag; authenticated data in exact order: UTF-8 bytes `onecall:operations-activation-list:cursor:v1\0` (terminal NUL), exact version/ID header, UTF-8 bytes `occurredAt desc, transitionId desc\0`, then the 32 raw SHA-256 bytes of UTF-8 `{}` (canonical empty filter object, no BOM/newline). This constant empty-filter digest satisfies docs/15's normalized-filter-digest convention in authenticated input; no request filters or mismatch branch exist. Sort/digest are AAD only, so payload stays28 bytes and framing arithmetic is unchanged. Keep exact sort anchor `occurredAt desc, transitionId desc` fixed. Every encode uses SecureRandom nonce as PA33 does.
* Decode requires canonical round-trip base64url, exact version/header/ID grammar and envelope length, known key, valid tag under that exact purpose/header/sort/empty-digest AAD, exact 28-byte payload, Instant-representable seconds and nanos 0..999999999 divisible by 1000. Invalid anchor on encode is dependency integrity503. Unsupported version, padding, truncation, extensions, wrong key, retired unknown key, altered nonce/ciphertext/tag, subject/other endpoint purpose, wrong sort/digest or malformed decrypted anchor =>400 CURSOR_INVALID. Prove same-ring PA33 tokens fail activation decoding and activation tokens fail PA33 decoding, both CURSOR_INVALID, never CURSOR_FILTER_MISMATCH. Never distinguish cryptographic failure details.
* Active key encodes; all configured keys decode for existing rotation semantics. Limit is deliberately absent: changing 20 to 1 or 100 with the same cursor must continue at the identical anchor. Cursor is not employee-bound because the confirmed ADMIN/OPS visibility is the same unfiltered metadata scope. No expiry, cursor persistence, anchor existence lookup or extra request parameter is invented.

### 4.3 SQL and page algorithm (G1/G3/G4)

Owner reader uses two fixed prepared SQL variants; no dynamic sort/column/name interpolation. Bind UTC OffsetDateTime from Instant and native UUID, not string lexicographic IDs:

```sql
SELECT id, subject_id, status_event_id, from_status, to_status,
       from_revision, to_revision, source_channel, source_action_domain,
       reason_code, occurred_at
FROM activation_consent.service_activation_transition
ORDER BY occurred_at DESC, id DESC
LIMIT ?
```

Continuation variant inserts this predicate before ORDER BY:

```sql
WHERE (occurred_at, id) < (CAST(? AS timestamptz), CAST(? AS uuid))
```

The final bind is exactly validated limit+1 (2–101). No offset, count query, joins, parent lookup, employee/subject/consent/trial facts in this owner SELECT, FOR UPDATE/SHARE, SKIP LOCKED or advisory lock. Reject reader arguments null Optional or out-of-range fetch size as dependency integrity 503. One fetch only; no scan loop, backfill or post-filter pagination. The unfiltered endpoint has no client/resource scope selector. Authorized empty ledger returns 200 empty; do not synthesize transitions for subjects without history.

The service validates every raw TransitionFacts row (including lookahead) against section3 before wire construction: nulls, boxed revisions before unboxing, the five domain-legal shapes, strict descending ordering, exclusive continuation anchor, no duplicate transition IDs and <=fetchSize. The owner only maps columns; it never drops malformed rows or constructs ActivationSummary. For equal Instant, compare UUIDs as **unsigned** most/least significant 64-bit words (`Long.compareUnsigned`), matching PostgreSQL UUID byte ordering; Java UUID.compareTo's signed ordering must not drive validation. Test IDs straddling the high bit. Trim only the lookahead row, set hasMore from actual size>limit, and encode last **returned** item only when hasMore. A tail of exactly limit has no cursor. No cursor is issued for an empty page or from the lookahead. Anchor need not still exist in database. Pages use fresh snapshots across requests; no global/as-of history snapshot, offset semantics or cross-page snapshot retention is promised.

### 4.4 Transaction, authority and cleanup (G2/G4/G5)

Reuse PA33 REQUIRES_NEW, ISOLATION_REPEATABLE_READ, setReadOnly(true), enforced PostgreSQL read-only and five-second transaction/deadline. `OperationsReadContext.enter` requires the transaction-bound shared ConnectionHolder; asserts JDBC readOnly=true, autoCommit=false, RR, session/current login equality and backend; sets local statement timeout 2000ms, lock timeout 250ms, idle-in-transaction timeout 5000ms; sets/asserts operations_iam_role. Do not add an actor lock, retained lock, independent connection or owner transaction.

Under operations_iam_role, issue only the existing authority SELECT:

```sql
SELECT id, status, role, revision
FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)
```

The IAM SELECT and transaction identity probes are the necessary authority/infrastructure exception to “read only transition facts”; they must not be serialized and are not activation enrichment. No clock query is needed by the activation projection/cursor; existing context probes establish the snapshot before authority. Test-only listeners observe `txid_current_snapshot()` using direct JDBC on the same transaction-bound connection at authority and transition SELECT boundaries and assert equal snapshots, backend and connection; guard listener re-entry and remove probes in finally. This adds no production query, wire field or connection. Only after authority and parsing/config/anchor does `one(readers)` resolve the activation port. In reader `context.scoped(Role.ACTIVATION,...)`: assert operations role, SET LOCAL ROLE activation_consent_role, assert role/connection/backend/session/readOnly/RR, SELECT rows, and in finally SET LOCAL ROLE operations_iam_role and assert. Service finally calls context.exit even after enter/owner failure; it restores SET LOCAL ROLE NONE and asserts login then removes thread-local. Failed restoration marks rollback-only and fails 503; do not return a buffered page from a failed completion.

Guarded existing DataSource verifies original autocommit/read-only/isolation/role and local settings on connection return, evicts dirty/broken connections and its template assertClean makes cleanup failure observable. Connection acquisition, SET/assert, query/statement timeout, commit/rollback/close failures must map to sanitized 503. Unknown nondependency application exceptions map to500. Check injected monotonic deadline before/after authority, provider/reader/codec work and before return; no retry. Role changes are trusted literal enum choices, never client data. Transient PostgreSQL read locks end with transaction; no held snapshot/locks across HTTP pages.

One snapshot means concurrent employee disable/transition append committed after snapshot establishment is invisible to the current request and visible on the next request. This is not linearizable revocation. Existing V007 SELECT privilege suffices; readOnly prevents writes even though activation_consent_role also has owner-local command privileges. The trusted login uses existing membership semantics, not new production grants. If actual deployment lacks the existing required read-role capability, return503; do not repair roles or supply credentials here.

## 5. Exact future S1 implementation allowlist

**This section is only the complete future S1 implementation allowlist; it authorizes no current implementation writes.** “New” refers to the merged PA33 baseline. Control, this cumulative plan, all fix reports (fix-1/fix-2/fix-3), reviews and every other gate artifact are outside this numbered list and cannot be counted or used for S1 path-permission decisions. Current fix write authorization is stated separately in section11. No replacement existing path or invented S1 work is authorized.

For exact paths, use these literal prefixes:

```text
M = backend/src/main/java/com/onecall/core/
T = backend/src/test/java/
```

Concatenating the prefix with each suffix is the complete allowlisted path (no globs).

| ID | State | Exact path (prefix as defined) | Responsibility / alignment |
| --- | --- | --- | --- |
| 01 | new | M operations_iam/subjectread/OperationsActivationReadService.java | service/request/eleven-field/page records, G1/G2/G5 |
| 02 | new | M operations_iam/subjectread/OperationsActivationReadPorts.java | one port/anchor/raw TransitionFacts carrier, G3/G4 |
| 03 | new | M operations_iam/internal/activationread/OperationsActivationReadRequest.java | query/Accept validation, G3/G5 |
| 04 | new | M operations_iam/internal/activationread/OperationsActivationCursorCodec.java | shared-ring purpose/anchor/sort/empty-digest AEAD, G3/G5 |
| 05 | new | M operations_iam/internal/activationread/OperationsActivationReadConfiguration.java | lazy existing infrastructure reuse, G4/G6 |
| 06 | new | M operations_iam/internal/activationread/DefaultOperationsActivationReadService.java | authoritative page use case, G1–G5 |
| 07 | new | M operations_iam/internal/web/OperationsActivationReadController.java | only new GET mapping, G1/G2/G5 |
| 08 | new | M activation_consent/internal/operationsread/JooqOperationsActivationHistoryReader.java | sole transition reader, G1/G4 |
| 09 | existing | M operations_iam/internal/web/OperationsBoundaryFilter.java | exactly one GET addition, G2 |
| 10 | existing | M customer_iam/internal/web/CustomerBoundaryFilter.java | proven exact pass-through lockstep only, G2 |
| 11 | new | T com/onecall/archtests/OperationsActivationReadWebTests.java | 12 methods, G1/G2/G5 |
| 12 | new | T com/onecall/archtests/OperationsActivationReadConfigurationTests.java | 8 methods, G3–G6 |
| 13 | new | T com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadRequestTests.java | 8 methods, G3/G5 |
| 14 | new | T com/onecall/core/operations_iam/internal/activationread/OperationsActivationCursorCodecTests.java | 10 methods, G3/G5 |
| 15 | new | T com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadServiceTests.java | 14 methods, G1–G5 |
| 16 | new | T com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java | 8 methods, G1/G2/G4/G6 |
| 17 | new | T com/onecall/archtests/operations/OperationsActivationReadTestProviders.java | MockMvc/transaction/clock/provider helper, zero methods annotated `@Test`; framework setup annotations permitted, G1–G5 |
| 18 | new | T com/onecall/archtests/container/OperationsActivationReadsIT.java | 18 real HTTP/PG16 methods, G1–G6 |
| 19 | new | T com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java | 10 real PG16 methods, G2/G4/G6 |
| 20 | new | T com/onecall/archtests/container/OperationsActivationReadFixture.java | PG16/web seeding/HTTP/role/fingerprint helper, zero methods annotated `@Test`; framework setup annotations permitted, G1–G6 |
| 21 | existing | backend/pom.xml | exactly two Failsafe includes; no dependency/plugin delta, G6 |
| 22 | existing | .github/workflows/ci.yml | exact named additions/inventory and activation JAR probes only, G2/G5/G6 |
| 23 | new | docs/gateflow/pa34-operations-activation-history-read-foundation-implementation.md | later S1 implementation/evidence report, G1–G6 |

Arithmetic: future S1 rows 01–23 = **23 rows = 19 new / 4 existing**. New: eight production Java + ten test/helper Java + one implementation report = 19. Existing: two filter Java + POM + CI = four. Control is Controller-owned bookkeeping outside this allowlist, as are the cumulative plan, fix reports and all review/gate artifacts; there is no combined artifact/S1 total. Existing tests, existing fixtures, POM dependencies, OperationsWebConfiguration, old controllers/service/ports/context/properties/codec/config, package-info files, application YAML, runtime identity scripts, all other gate/review docs and frozen files are read-only. If an unexpected exact-file guard needs adjustment, stop for a narrow allowlist correction rather than silently expanding this list.

### Architecture expected maps (G1/G2/G4/G6)

New `OperationsActivationReadBoundaryTests` checks the exact mapping union across the **three** Operations controllers:

```text
OperationsSubjectsController -> POST /api/v1/operations/subjects/{subjectId}/suspensions
OperationsSubjectsController -> POST /api/v1/operations/subjects/{subjectId}/resumptions
OperationsSubjectReadController -> GET /api/v1/operations/subjects
OperationsSubjectReadController -> GET /api/v1/operations/subjects/{subjectId}
OperationsActivationReadController -> GET /api/v1/operations/activations
```

Its PA34 production-file set is exactly new rows01–08 and existing filter rows09–10, not every file in a preexisting shared package. For `operations_iam/internal/activationread`, the exact four filenames are rows03–06. Its new owner map is exactly `ActivationHistoryReader -> activation_consent.internal.operationsread.JooqOperationsActivationHistoryReader -> activation_consent.service_activation_transition -> Role.ACTIVATION`. Existing five owner adapter maps in OperationsSubjectReadBoundaryTests remain unchanged. Reflection asserts service public method exactly list; the port namespace has exactly one nested interface; wire records exactly section3's properties, primitive int public revisions, nullable boxed port revisions and exact eleven-field TransitionFacts; new controller exactly one GET. Source guard forbids foreign table SQL in controller/service, SELECT * and sensitive columns in reader, writes/audit/Outbox/locks/command calls along new application/owner path, production EmployeeSessionVerifier implementations and permissive route patterns.

`ModuleBoundaryTests.EXPECTED_ALLOWED` is unchanged. Discovered production **target multiset** remains exactly 12: `{sharing:1,evidence:2,subject_number:1,activation_consent:1,catalog_order:1,entitlement_metering:1,operations_iam:5}`. No additional module edge is created by another activation->existing-Operations-read import. Existing owner named interfaces and customer allowed dependencies remain unchanged. Do not alter baseline graph/route test expectations merely to make a failure disappear.

## 6. Exact test identities and assertions

Each listed entry is exactly one ordinary no-argument `@Test` method; use loops inside methods for matrices. Place exact `@Test` immediately before each no-argument method signature, separated only by whitespace, with no interposed `@DisplayName`, other annotation or comment. This matches the frozen CI regex `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`; do not alter that regex. No parameterized/dynamic/repeated tests, overloaded test methods, disabled tests, skips or new `@Test` tokens in comments. Helper/fixture rows17/20 have zero methods annotated `@Test`; framework setup annotations needed for test infrastructure are permitted and are not test identities. Legacy method identities are retained even when their original names refer to nine persistable shapes or endpoint-specific properties: the corrected assertion cells, not those names, define five successful shapes/four integrity failures and reuse of the shared ring. The IDs here are the exact CI addition manifests; assertions are minimum behavioral content, not permission to add scope. Reuse `com.onecall.archtests.support.SyntheticEmployeeSessions` unchanged.

### A. `com.onecall.archtests.OperationsActivationReadWebTests` — 12

| Method | Assertions and alignment |
| --- | --- |
| `exactGetReturnsOnlyFrozenPageAndElevenSummaryProperties` | Exact mapping, JSON types, eleven item keys/three page keys, ISO occurredAt, always-null tail cursor, forbidden-field sentinels absent, G1. |
| `otherMethodsAndNoncanonicalRoutesRemainConcealed` | POST/PUT/PATCH/DELETE/HEAD/OPTIONS/TRACE; trailing slash, double slash at every separator, semicolon/matrix params, %61/%2f/%2F/%3b/%25 encodings, encoded prefix, extra segment and /employees; rawURI/servletPath mismatch even if servletPath looks canonical; no verifier/service invocation, G2. |
| `employeeAuthenticationPrecedesQueryValidation` | Missing/malformed/duplicate/customer/wrong-audience/rejected bearer with bad query/Accept gets401 before service; exact audience passed to verifier, G2/G5. |
| `missingAmbiguousAndBrokenVerifiersFailClosed` | Zero/two/uncreatable/null result/unavailable verifier =>sanitized503, no service/dependency work, G2/G5. |
| `currentEmployeeAuthorityPrecedesActivationDependencies` | Real service with mock transaction/SQL: ADMIN/OPS200, missing401, disabled403 even for bad query/missing keys/reader; malformed/unsupported authority503, no owner/config interaction before accepted authority, G2/G5. |
| `missingAmbiguousAndBrokenServicesFailClosed` | Zero/two/creation failure=>503; no raw bean error, G5. |
| `queryShapeLimitsAndAcceptUseOnlyFrozenErrors` | Defaults/bounds, unknown names (including every former subject filter/offset/total), duplicates, bad Accept, exact422; cursor content400 after shape; no automatic MVC 400/406/415, G3/G5. |
| `cursorFailuresRemainSanitizedAndLimitIsUnbound` | Tamper/wrong-purpose/wrong-key400; missing config503 even first/empty; valid cursor and changed limit admitted, G3/G5. |
| `problemsHaveExactSafeEnvelopeHeadersAndChallenge` | All seven operation error codes/statuses, fixed envelope/titles, traceUUID, no-cache headers, 401 challenge, no cookies/secrets/HTML; shared unallowed code=>500, G5. |
| `customerAndOperationsFiltersAdmitOnlyExactActivationGet` | Both filters in order; exact GET passes without customer auth; aliases don't pass; customer/public behavior unchanged; no cross-authentication, G2. |
| `subjectGetAndLifecyclePostAdmissionRemainUnchanged` | Both subject GETs and both lifecycle POST templates still admitted; original POST header rules kept; forbidden downstream subject variants concealed, G2. |
| `unknownAndPartialResponseFailuresDoNotLeak` | Unknown service/serialization exceptions=>sanitized500; no partial body/cursor/exception marker; dependency typed503 kept, G5. |

### B. `com.onecall.archtests.OperationsActivationReadConfigurationTests` — 8

| Method | Assertions and alignment |
| --- | --- |
| `defaultWebAndDisabledProfilesCreateNoActivationReadResources` | No activation service/reader graph or pool in default/web/disabled persistence; web request without service fails503, G4/G5. |
| `enabledPersistenceGraphIsLazyAndReusesQualifiedReadInfrastructure` | Lazy beans, exact three qualified PA33 resources, no new DataSource/manager/DSL pool; shared enabled condition/profile, G4/G6. |
| `activationKeysUseOnlyExactEndpointSpecificProperties` | Retained method identity: only existing subject-read cursor prefix/bean configures activation; same ring suffices without any activation prefix, activation-style keys alone fail503, no separate property class/binder/operator secret; same-key cross-purpose tokens fail CURSOR_INVALID in both directions, G3/G5. |
| `malformedIndexedKeyPropertiesFailClosedWithoutFallback` | Exercise existing shared binder/validation unchanged: gaps/out-of-range/incomplete/unknown key suffix/property precedence/duplicate IDs or material=>503 even first/empty pages, G3/G5. |
| `missingInvalidAndAmbiguousActivationDependenciesAreUnavailable` | Missing/bad shared property provider, zero/two/uncreatable reader, invalid shared keyring=>503 not startup break of unrelated surfaces, G5. |
| `propertiesKeysRequestsAndCodecsRedactToString` | Include datasource baseline and shared key objects and all new request/raw-fact/cursor objects; sentinels absent from toString, G5. |
| `ownerReaderUsesOnlyExistingActivationReadRoleAndPort` | Reader bean exactly one port, existing qualified context/DSL, lazy conditional graph, no role repair/provisioning, G4/G6. |
| `configurationAddsNoDependencyPoolOrProductionVerifier` | Reflection/source checks for no new resources, library dependency or production verifier; shared transaction settings retained, G2/G4/G6. |

### C. `com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadRequestTests` — 8

| Method | Assertions and alignment |
| --- | --- |
| `absentLimitDefaultsToTwenty` | Empty query/default Accept, limit20/no cursor, G3. |
| `limitsOneThroughOneHundredAreAccepted` | All 1–100 plus PA33-compatible bounded leading-zero spellings, G3. |
| `invalidLimitLexemesAndBoundsAreValidationFailed` | 0/101/negative/plus/blank/decimal/exponent/space/overlong=>422, G3/G5. |
| `onlyCursorAndLimitNamesAreAccepted` | All forbidden query/filter aliases including identity/role/subjectId/source/offset/total rejected422, G1/G3. |
| `duplicateAndNullQueryValuesAreValidationFailed` | Repeated same/different values, null query/key/list/value/empty list/raw request =>422, G3/G5. |
| `cursorContentIsDeferredToCodecAfterShapeValidation` | Single empty/oversize cursor retained for codec400; duplicate cursor422; parser no dependency access, G3/G5. |
| `acceptNegotiationMatchesExistingJsonReadRules` | Absent/wildcard/JSON/+quality/UTF-8 accepted as current compatibility permits; malformed/nonJSON/q0/unsupported parameters rejected422 with specificity precedence, G5. |
| `rawAndParsedRequestsAreImmutableAndRedacted` | Original map/list mutation cannot change request; no secrets in toString; no hidden filters/digest/limit cursor binding, G3/G5. |

### D. `com.onecall.core.operations_iam.internal.activationread.OperationsActivationCursorCodecTests` — 10

| Method | Assertions and alignment |
| --- | --- |
| `roundTripPreservesMicrosecondAnchorAndUuid` | Epoch edge seconds/valid Instant ranges/nanos/UUID high bits round-trip, G3. |
| `encodingIsRandomizedOpaqueCanonicalAndBounded` | Same anchor differs, canonical alphabet/no padding/<512; no plaintext IDs/time/offset, exact framing, G3. |
| `malformedLengthsAlphabetVersionsAndFramesAreCursorInvalid` | Empty/>512/invalid alphabet/padding/noncanonical Base64/truncated/extra bytes/ID/frame/version malformed=>400, G3/G5. |
| `tamperedHeaderNonceCiphertextAndTagAreCursorInvalid` | Each region mutated=>400 without distinguishing reason, G3/G5. |
| `subjectAndOtherPurposeCursorsAreCursorInvalid` | Same shared-ring real PA33 cursor rejected by activation and activation cursor rejected by unchanged PA33 decoder as CURSOR_INVALID; wrong purpose/sort/empty-digest AAD tokens=>400, never mismatch code, G3/G5. |
| `wrongUnknownAndRetiredKeysAreCursorInvalid` | Unknown ID, known ID/wrong material, retired absent key=>400, G3/G5. |
| `activeAndRetainedKeysSupportRotation` | Encode active key; decode retained key; exact existing ring semantics, G3. |
| `missingInvalidAndDuplicateKeyConfigurationIsUnavailable` | Empty/gapped/bad-length/base64/active absent/duplicate ring=>503 including first page setup, G3/G5. |
| `authenticatedMalformedAnchorPayloadsFailClosed` | Construct authenticated wrong length/out-of-range seconds/nanos/nonmicro payloads=>400; invalid encode anchor=>503, G3/G5. |
| `cursorHasNoLimitOrFilterBinding` | Retained method identity means no client-variable filter binding: API/payload solely anchor, fixed canonical empty-filter SHA-256 and exact sort authenticated in AAD; same token with limits1/20/100 decodes identically, no mismatch error, G3. |

### E. `com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadServiceTests` — 14

| Method | Assertions and alignment |
| --- | --- |
| `currentActiveAdminAndOpsAreOnlyAcceptedAuthorities` | Current DB identity/audience/status/role/revision, missing401/disabled403/bad authority503; no client roles, G2/G5. |
| `authorityRunsBeforeParsingKeysAndReaderResolution` | Ordered spy interactions, invalid request with disabled employee=>403 and zero config/reader work, G2/G5. |
| `emptyAndSinglePagesHaveNoCursor` | 0/1 rows; exact three page fields, no synthetic transition, G1/G3. |
| `exactLimitTailAndLookaheadUseLastReturnedAnchor` | n=limit and n=limit+1 with limits1/20/100, exactly one fetch(limit+1), lookahead never anchor, G3/G4. |
| `tiedInstantsUseUnsignedNativeUuidDescendingOrder` | UUIDs across sign boundary, repeated timestamp, native strict descending; duplicates and inversions503, G3/G5. |
| `continuationIsExclusiveStableAndIndependentOfLimit` | Old anchor exclusive, changed limit, missing anchor row allowed, empty continuation no cursor, G3. |
| `allPersistableStatusChannelAndDomainCombinationsProjectExactly` | Classify all nine DB-persistable shapes: five domain-legal shapes project exactly/all enums, four domain-impossible shapes fail503; revisions0 and positive max-safe, Unicode code-point bounds including astral characters; no extra enum/association, G1. |
| `nullMalformedAndInvalidLookaheadRowsFailClosed` | Stub port injects raw TransitionFacts (no validating constructor): null list/row/every nullable field including each boxed revision; invalid enums/pairs/length/revisions/overflow/nonmicro time and each of four DB-shape-only combinations. Mix good rows with invalid returned/lookahead facts; oversized list or outside-anchor row also fails503 with no partial page. Exercise same stub through MockMvc for sanitized503/no item or cursor output, G1/G5. |
| `missingAmbiguousAndUncreatableReadersAreUnavailable` | Port zero/two/constructor failure, delayed until authorized, G4/G5. |
| `cursorConfigurationAndDecodeFailuresPrecedeOwnerRead` | Missing config503, malformed/tamper/wrong-purpose400, shape422 wins before config; zero owner interactions, G3/G5. |
| `singleReadOnlyRepeatableReadTransactionEnclosesAllWork` | One REQUIRES_NEW transaction, same context enter/authority/fetch/encode/exit; mocked probes assert same transaction-bound snapshot observation, with real txid_current_snapshot proof in G/H; no owner transaction/production clock/multi-fetch, G4. |
| `deadlineDependencyAndTransactionFailuresAreUnavailable` | Injected monotonic budget before/after each stage; query/provider/acquire/commit/rollback failures503; no retries, G4/G5. |
| `roleAndContextCleanupRunsOnEveryExit` | Enter failure, auth/parse/decode/fetch/map/encode failure and success all invoke exit; failed restoration fails503 rather than page, G4/G5. |
| `unknownAndOutOfContractTypedFailuresBecomeInternal` | Unknown exceptions and forbidden shared codes=>500; allowed seven typed codes maintained; safe messages/toString and no audit/Outbox/command calls, G5. |

### F. `com.onecall.archtests.architecture.OperationsActivationReadBoundaryTests` — 8

| Method | Assertions and alignment |
| --- | --- |
| `publishesOnlyExactActivationReadServicePortAndRecords` | Reflection on exact API/port/anchor/eleven raw TransitionFacts fields, nullable Integer revisions vs public primitive int and service-only validation/wire construction, existing named interface, G1/G3. |
| `operationsRouteAndControllerFileMapsRemainExact` | Section5's exact five-template/three-controller union and new internal filename set; no other mapping, G1/G2/G6. |
| `ownerReaderSelectsOnlyElevenApprovedTransitionColumns` | Sole FROM table, eleven columns, fixed order/seek/limit binds, no wildcard/association/sensitive columns, G1/G4. |
| `applicationAndControllerHaveNoForeignSqlOrCommandCalls` | No activation base-table SQL in Operations, no hidden module/entity/API invocation; allowed IAM authority only, G1/G4. |
| `readPathHasNoWritesAuditOutboxLocksOrCursorStorage` | New path sources contain no mutation/audit/outbox/locking/DDL/cursor table; infrastructure is read-only/RR, G4. |
| `existingNamedInterfaceAndModuleEdgeMultisetAreUnchanged` | Exact existing allowed map and 12-edge multiset, no cycle/new package-info or named interface, G4/G6. |
| `frozenInputsAndSeventeenMigrationInventoryRemainByteIdentical` | Section8 raw-byte SHA oracle plus exact V001–V017/no V018; existing frozen test also remains, G6. |
| `employeeVerifierAndFilterLockstepStayIndependentAndExact` | No packaged production employee verifier, same audience, precise equality/pass-through in both filters and unchanged POST/subject patterns, G2/G6. |

### G. `com.onecall.archtests.container.OperationsActivationReadsIT` — 18

| Method | Assertions and alignment |
| --- | --- |
| `postgres16UsesExactlyFrozenSeventeenMigrations` | Real server_version_num160000..169999, UTF8, exact successful migrations17/raw-byte oracle, G6. |
| `realHttpActiveAdminAndOpsReturnFrozenProjection` | Boot web+persistence, synthetic independent bearer, real JDBC employee and transition facts; exact safe JSON and headers, G1/G2. |
| `realHttpAuthLifecycleAndAudiencePrecedenceArePreserved` | Missing/customer/wrong/disabled/missing employee with invalid query/key/reader; exact401/403, no disclosure, G2/G5. |
| `realHttpMethodsAndCanonicalizationVariantsStayConcealed` | Real HTTP methods, semicolon/encoded/slash/extra-segment variants; admitted exact path unaffected; server connector rejected encodings must never return data/auth success, and filter-accepted requests must give sanitized404, G2. |
| `realHttpParametersDefaultsDuplicatesAndBoundsAreExact` | Default20 and1/100, duplicate names and all bad/unknown query cases; real container request decoding; exact422/400 split, G3/G5. |
| `emptySingleAndExactLimitPagesHaveNullCursor` | Empty/1/exact limit rows =>correct tail; no anchor lookup or extra facts, G1/G3. |
| `lookaheadAndTailAnchorOnlyLastReturnedItem` | limit+1/multi-page/tail/empty subsequent; decode anchor for assertion, not lookahead, G3. |
| `timestampTiesAndHighBitUuidsContinueWithoutDuplicates` | Native PG sort vs API continuation, microseconds, UUID high-bit boundaries, complete traversal, G3. |
| `changingLimitReusesCursorWithoutRestartOrMismatch` | Change among1/20/100; same exclusive anchor and complete stable history, G3. |
| `realHttpMalformedTamperedWrongPurposeAndWrongKeyCursorsFail` | All cryptographic categories400; malformed config503 even empty; no key/SQL leak, G3/G5. |
| `allNinePersistableTransitionCombinationsReturnAllEnums` | Retained method identity covers all nine V007-persistable shapes, not nine successful outputs: five domain-legal shapes return all enums/exact eleven keys; loop over four DB-shape-only rows seeded under unchanged V007 checks in isolated fixture scopes, each beside good rows and as lookahead, whole HTTP page sanitized503/no partial items/cursor. Revisions/text/code-point bounds; no consent/trial field, G1. |
| `onlyTransitionFactsAreReadAndNoHiddenFieldsLeak` | jOOQ listener observes only role/identity/IAM authority/transition SELECTs, one bounded owner query; seed hidden sentinel columns and association facts; none serialized. Within this same method capture application/server/pool/jOOQ logs across success, malformed and tampered cursor400, dependency503, domain-integrity503 and empty200 paths. Canary cursor ciphertext/material, employee UUID/identity, SQL text/JDBC URL/detail, key material and raw sensitive values must be absent from captured logs and responses/problems. Assert capture is active using a harmless test marker outside request execution; never log sensitive canaries to prove capture. Inspect rendered messages, argument/MDC values and throwable/stack detail (not only formatted messages) using existing logging test infrastructure; no new dependency. Keep capture around request paths, with fixture setup/intentional direct-SQL constraint probes outside it. Use fixture-local logger/appender cleanup in finally; no extra method, G1/G4. |
| `concurrentAppendIsInvisibleWithinSnapshotAndVisibleNextRequest` | Listener+latches after authority/snapshot, separate committed append; test listener samples txid_current_snapshot() on the transaction-bound connection at authority and transition SELECTs and asserts identical snapshots/backend/connection; current page unchanged, next request sees it; no inter-request snapshot guarantee, G3/G4. |
| `concurrentEmployeeDisableUsesSameSnapshotThenDeniesNextRequest` | Separate authority change after snapshot; current authorized read stable, next403; no activation dependency before authority, G2/G4. |
| `dependencyTimeoutAndUnknownFailuresUseSanitizedProblems` | Real query timeout plus controlled reader failure of unknown kind;503 vs500, seven-code envelope, no raw diagnostics, G5. |
| `readsLeaveBusinessAuditAndOutboxFactsUnchanged` | Before/after deterministic all-table fact fingerprints/counts including audit/outbox on success/error/empty/cursor paths; no row mutation, G4. |
| `ownerAndTransactionFailuresNeverProducePartialSuccess` | Fail SQL/role/commit/cleanup with listener/proxy, including prospective empty200; fail503 and follow-up clean200, G4/G5. |
| `existingSubjectLifecycleAndCustomerSurfacesRemainIsolated` | Real HTTP subject GET templates and lifecycle POST admission/auth/header boundary unchanged; customer/public don't accept employee bearer; no new endpoint, G2. |

### H. `com.onecall.archtests.container.OperationsActivationReadRoleLeastPrivilegeIT` — 10

| Method | Assertions and alignment |
| --- | --- |
| `runtimeLoginHasOnlyExistingExplicitRoleMembershipSemantics` | Fixture login NOINHERIT/no superuser/bypass/create privileges; existing role-edge options ADMIN FALSE/INHERIT FALSE/SET TRUE, no production provisioning/grant added, G4/G6. |
| `operationsRoleCannotSelectTransitionsButActivationRoleCan` | Actual42501 under operations role; activation role can SELECT exact projection under read-only transaction; no cross-owner ACL added, G4. |
| `activationRoleCannotReadUnrelatedOwnerPayloads` | Foreign account/employee/subject payload SELECT probes denied42501 except frozen grants; assert exact existing column/table ACLs, not false blanket denial of previously granted columns, G4/G6. |
| `missingRoleCapabilityFailsClosedWithoutRepair` | Test-only revoke membership/capability then request503; no GRANT attempted by application; restore only fixture state, G4/G5. |
| `readOnlyTransactionRejectsWritesAndLockingQueries` | Within exact template roles, actual PG rejects business INSERT/UPDATE/DELETE and FOR UPDATE/SHARE; no request-side audit/outbox, G4. |
| `handoffsAssertAndRestoreExactRolesConnectionAndBackend` | operations->activation->operations->login, same backend/connection/read-only/RR; test-only txid_current_snapshot() observations at authority and transition SELECTs must be identical within the transaction; success and thrown owner exception, G4. |
| `wrongEntryResourceAndTransactionStateFailClosed` | Missing transaction, wrong datasource/connection/backend/entry role, not-read-only or wrong isolation=>503 before transition SELECT, G4/G5. |
| `failedRestorationRollsBackAndEvictsDirtyConnections` | Inject restore/close failures;503/no data; dirty physical connection evicted; known clean return can reuse connection without leaking role/threadlocal, G4/G5. |
| `poolSettingsAndLocksAreCleanAfterSuccessAndFailure` | Restored autoCommit/readOnly/isolation/role/local timeout/timezone settings; no idle-in-transaction/retained locks on observed backends, G4. |
| `frozenAclRolesFunctionsAndAllFactsHaveZeroDelta` | Before/after catalog/role/function and business/audit/outbox fingerprints plus frozen oracle, unchanged V001–V017/ACL independent baseline; no schema/grant delta, G4/G6. |

### Fixture/helper boundaries

`OperationsActivationReadTestProviders` supplies a real DefaultOperationsActivationReadService with controllable transaction/DSL/context/read-port/config providers and synthetic session registry; standalone MVC registers new controller and existing two controllers where testing preserved route admission, both actual filters and existing problem writers. Both helper/fixture rows17/20 have zero methods annotated `@Test`; framework setup annotations needed for infrastructure are permitted and are not test identities. Helpers may not bypass auth or add production verifier implementations. Use a package-private clock constructor from same-package service tests for deterministic deadlines.

`OperationsActivationReadFixture` is a new small fixture patterned on OperationsSubjectReadFixture, **not** a modification of it. Reuse unchanged CustomerBusinessNumbersFixture for PostgreSQL16/Flyway/test provisioning and its `execute/one` helpers; reuse unchanged SyntheticEmployeeSessions. Start web+persistence with existing read datasource/enabled properties and a real HTTP random port; the fixture supplies scoped synthetic cursor key values only to the existing shared `onecall.operations-subject-read.cursor.*` property names. No activation cursor prefix exists, and no separate property class, binder or operator secret is introduced; no new production grants/scripts. Test-only login memberships mirror existing fixture roles and options. Implement `start`, `reset`, `close`, `admin`, `runtime`, `subject`, `transition`, `request`, `json`, `problem`, `fingerprints`, `replaceCursor` helper identities as needed inside this one file. Close application, HTTP client, pool/container on all exits; latches/listeners are removed in finally. Seed five domain-legal V007-valid owner shapes for successful pages (consent for non-Operations, employee actor for Operations, matching revision/head/unique event identities); seed each of four DB-shape-only negative combinations separately beside valid rows/as lookahead, satisfying unchanged FK/actor/consent/unique/check constraints using admin setup outside tested read transactions. Never disable constraints/triggers to create illegal PostgreSQL rows: NULL/constraint-impossible malformed facts use stubbed raw TransitionFacts unit/MockMvc probes; real PostgreSQL proves frozen constraints and allows the four domain-impossible DB shapes whose pages the service must reject503. No wire constructor or owner validation may prevent malformed unit injection. Fixture setup writes are not production request writes and must be excluded from before/after request fingerprints.

## 7. Exact inventory arithmetic and CI/POM coupling (G6)

Merged PA33 baseline is preserved: **70 classes / 644 methods Surefire + 29 / 476 Failsafe = 99 / 1120**; word-boundary token-line scan **1121** (the sole existing PostgresContainerIT comment is retained); POM includes **29**.

| Suite | Class delta | Method/token delta | Required final inventory |
| --- | --- | --- | --- |
| Surefire A–F | +6 | 12+8+8+10+14+8 = **60** | **76 / 704** |
| Failsafe G–H | +2 | 18+10 = **28** | **31 / 504** |
| All | +8 | **88** | **107 / 1208** |
| word-boundary annotation token-line scan | — | +88 | **1209** |
| exact Failsafe POM includes | +2 | no unit-test includes | **31** |

Existing classes/method identities: zero addition/deletion/rename; helpers zero annotations. Do not edit earlier historical addition dictionaries or baseline totals to disguise a mismatch. In `.github/workflows/ci.yml`, add `pa34_unit_additions` and `pa34_container_additions` with **exactly** A–F and G–H class FQCNs and method sets above. Merge them into unit_expected/container_expected after PA33; include their union in per-source additions and subtraction used to recover historical baselines. Every earlier `method_pattern.sub` historical token-scan subtraction must also subtract PA34 names, while a new `pa34_start_totals`/`pa34_start_line_scan_total` excludes only PA34. Add PA34-start aggregate assertion equal PA33's 644+476=1120 and token scan1121, while final totals become704+504=1208/token1209. Keep previous historical arithmetic and sole comment_hits tuple intact. Change final class/method/XML/include assertions/diagnostic consistently to76/31/704/504/1209/31, including the early `unit_suites` aggregate check from644 to704 and both final exact_inventory calls to704/504.

POM additions exactly:

```xml
<include>**/OperationsActivationReadsIT.java</include>
<include>**/OperationsActivationReadRoleLeastPrivilegeIT.java</include>
```

Source A–H identities = CI PA34 dictionaries = actual XML identities. Failsafe source G/H class identities = CI container additions = the two new POM includes, with all 29 existing includes untouched. No attempt to equate 60 unit methods with POM IT includes. Require every suite zero failures/errors/skips and nonempty reports; exact XML Counter/class/method sets, no duplicates, no ignored/missing tests. Preserve anchored `@Test\b` token rule (not substring counting `@TestConfiguration`/`@Testcontainers`), and no multiline/comment trick to evade it. Mandatory source and CI equality runs after focused tests and after full clean run; final inventory acceptance uses full-run reports only.

In existing CI JAR smoke step add only exact activation GET probes: no header=>401, opaque bearer without production verifier=>503, unknown extra query still401 without auth, other methods/encoded/semicolon/double slash/extra segment=>concealed404 where connector admits the request. Use existing bounds/cleanup/no-secret assertions and keep all default/web existing probes. Default JAR remains non-web; web JAR without persistence/verifier never returns data. Firewall inventories/dependency counts remain unchanged; no new dependency or packaged production EmployeeSessionVerifier is permitted.

## 8. Frozen raw-byte inputs (G6)

These SHA-256 values were read directly at this plan gate. They are **raw bytes**, not newline-normalized alternatives. Map key is the exact filename under the indicated directory. Preserve these 21 inputs and the exact migration filename set. Also preserve docs/12 and all other out-of-allowlist files; docs/12's existing frozen architecture test remains intact.

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e  docs/17-production-security-design-spec.md
96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6  backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql
4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505  backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql
4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7  backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql
8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030  backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql
6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065  backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql
7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d  backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql
bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81  backend/src/main/resources/db/migration/V007__activation_and_consent.sql
32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde  backend/src/main/resources/db/migration/V008__evidence_media.sql
3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1  backend/src/main/resources/db/migration/V009__sharing.sql
d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559  backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql
ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e  backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql
ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6  backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql
1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e  backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql
bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8  backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql
c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc  backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql
a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c  backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql
74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3  backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql
```

Mandatory implementation raw-byte check, from repo root; extracts the above exact manifest from this artifact without a temporary helper/manifest:

```bash
python3 - <<'PY'
import hashlib,re
from pathlib import Path
plan=Path('docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md').read_text()
entries=re.findall(r'^([0-9a-f]{64})  (.+)$',plan,re.M)
assert len(entries)==21 and len({p for _,p in entries})==21
for expected,name in entries:
    assert hashlib.sha256(Path(name).read_bytes()).hexdigest()==expected,name
actual={p.name for p in Path('backend/src/main/resources/db/migration').glob('*.sql')}
expected={Path(p).name for _,p in entries if '/migration/' in p}
assert actual==expected and len(actual)==17
print('21 frozen raw-byte hashes; exactly V001-V017; no V018')
PY
```

The new architecture frozen test uses these exact constants, not accepting a later normalized hash or regenerating expectations from changed files. Any frozen mismatch/migration/ACL need blocks implementation for renewed authorization, not a “fix” to the oracle.

## 9. Mandatory implementation validation — NOT RUN at plan gate

All runtime commands below are requirements **after implementation authorization**, not claimed results. They generate targets/dependencies/tmp contract outputs and therefore are deliberately not run in this two-document fix task. Use Java21.0.13, Maven3.9.9, Docker daemon and real postgres:16. Network/toolchain availability is an environment prerequisite, not permission to bypass tests. Do not modify CI/POM inventories to accommodate skips or connector behavior.

### 9.1 Focused then full Java/PostgreSQL16 (G1–G6)

From repository root:

```bash
docker info
cd backend
./mvnw -B -ntp -v
./mvnw -B -ntp -Dtest=OperationsActivationReadWebTests,OperationsActivationReadConfigurationTests,OperationsActivationReadRequestTests,OperationsActivationCursorCodecTests,OperationsActivationReadServiceTests,OperationsActivationReadBoundaryTests,ModuleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadWebTests,OperationsSubjectsWebTests test
./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsActivationReadWebTests,OperationsActivationReadConfigurationTests,OperationsActivationReadRequestTests,OperationsActivationCursorCodecTests,OperationsActivationReadServiceTests,OperationsActivationReadBoundaryTests -Dit.test=OperationsActivationReadsIT,OperationsActivationReadRoleLeastPrivilegeIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
cd ..
```

Verify focused XML exact new method sets and zero skips/failures/errors, but do not use a focused run to claim full inventory. Run section8 frozen checker before and after full validation. Full clean run is authoritative for final 76/704 +31/504 counts and real PG16 container evidence.

### 9.2 Execute exact CI bodies locally for XML inventory, JAR smokes and firewall (G2/G5/G6)

Avoid maintaining a second weaker script. This exact command executes the existing/current CI multiline run bodies for the named steps in the backend working directory; it runs only at implementation validation, after full verify, and does not write a private helper. The first inventory body includes exact source=CI=POM=XML equality; second probes the packaged default/web JAR; third resolves and validates runtime/packaged dependency firewall. A missing/renamed step is a failure, not silently skipped.

```bash
python3 - <<'PY'
from pathlib import Path
import subprocess
lines=Path('.github/workflows/ci.yml').read_text().splitlines()
for name in ('assert container smoke actually ran',
             'default and opt-in web jar smokes',
             'production jar dependency firewall'):
    marker='      - name: '+name
    found=[i for i,line in enumerate(lines) if line==marker]
    assert len(found)==1,name
    start=found[0]+1
    assert lines[start]=='        run: |',(name,lines[start])
    body=[]
    for line in lines[start+1:]:
        if line and not line.startswith('          '): break
        body.append(line[10:] if line else '')
    assert body,name
    subprocess.run(['bash','-e','-o','pipefail','-c','\n'.join(body)],cwd='backend',check=True)
PY
```

The CI firewall's own `dependency:list` output in target and JAR subprocesses are implementation-validation outputs, not new tracked paths. No change to firewall baselines or dependency counts is planned. Retain process timeouts and cleanup on success/failure. Inspect XML with the exact CI body, not only Maven's aggregate line.

### 9.3 Contract and A1–A5 commands (G1/G6)

From repository root, pinned Node22.23.4 as CI; these are exact existing stages, not new acceptance or live integration:

```bash
node --version | grep -Fx v22.23.4
(cd api/openapi && npm ci && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 \
  --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
for stage in s1 s2; do
  extra=()
  if [ "$stage" = s2 ]; then extra+=(--check-doc-links); fi
  node acceptance/validate.mjs --stage "$stage" \
    --matrix acceptance/v1/acceptance-matrix-v1.json \
    --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures \
    --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
    --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
    --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures \
    --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures \
    --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures \
    "${extra[@]}"
done
```

A1 includes frozen contract lint/bundle/client checks as its existing check:s3 package script defines. A2–A5 remain existing fixture validators, not evidence that production IdP, secrets, traffic, capacity or acceptance are ready. Finally run frozen check again, git diff --check, changed-path allowlist review, exact source/CI/POM/XML inventory body, JAR/firewall results review and report all commands/exit codes. Failures remain failures; no acceptance claim from static plan checks.

## 10. Documentation, risks and stop conditions

Documentation decision (G6): only the cumulative plan and fix-3 evidence artifact may change in this retry; fix-1/fix-2 and reviews remain read-only history. Later S1 documentation is exactly the implementation report in section5. Any Controller-owned checkpoint is separate gate bookkeeping outside that allowlist. No frozen architecture/data/API/security document, migration, OpenAPI or additional roadmap/future-slice document is required. This task does not authorize implementation or plan review.

| Residual | Classification / owner | Destination and boundary |
| --- | --- | --- |
| Production independent verifier, session issuance/MFA and current employee provisioning | External prerequisite; Security/Operations IAM | Existing Q1/security/deployment work, outside PA34. Without verifier503; no packaged fake adapter or live credential work. |
| Existing runtime read-login membership/credential configuration | External deployment prerequisite; DBA/Backend | Existing read-pool deployment ownership; tests mirror established NOINHERIT/SET semantics. Application fails503 without repair; no production role/grant delta authorized. |
| Shared read cursor ring custody/rotation and unique AEAD nonces | Existing external deployment prerequisite; Security/Backend | Existing subject-read secret custody/deployment process and implementation report; activation reuses that ring with distinct purpose/AAD, no new operator secret. Missing/bad shared config503 even first/empty pages. No extra endpoint/storage. |
| Global occurredAt/id query may sort many rows because V007 index begins subject_id | Bounded performance residual; Backend/DBA | Carry in implementation report and existing deployment-capacity/production-readiness assessment; prepared limit+1 and timeout/budget may yield503 at scale, no performance/readiness claim. No global index/migration planned. Any required schema/grant change requires renewed authorization; do not create V018. |
| RR revocation is snapshot-relative; continuation spans fresh snapshots | Explicit existing transaction semantics; Backend/Security | Implementation report documents observed tests, not linearizable revocation or pinned cross-page snapshots. No new cursor timestamp/expiry/filter. |
| Malformed transport encodings may be rejected by Tomcat before filters | Test/environment nuance; Backend | Real HTTP tests prove no data exposure; direct filter tests prove sanitized concealment for requests reaching it. Do not relax admission or change server/public behavior merely to force a connector-specific status. |
| Toolchain/Docker/network unavailability | Validation blocker; Controller/implementer | Mandatory implementation validation remains pending until available; no skip/waiver invented in plan. |
| Unsupported field/filter/endpoint, frozen mismatch or required privilege/schema change discovered | **Blocking scope question**; Controller/user | Stop, report exact evidence for renewed Goal Confirmation/authorization; never silently expand scope or lower guards. |

There is no currently demonstrated blocking contract/persistence mismatch: the eleven projected facts and existing SELECT privilege suffice. No residual creates another PA34 goal, acceptance criterion or implementation slice. Independent plan review and all later gates belong to Controller scheduling after this task stops; this agent does not initiate them.

## 11. Plan-fix verification and completion report format

Current static-only verification must: read Control, the complete cumulative plan, reviews `plan-review-20261115-{033925,035649,043352,044112}.md`, fix-1/fix-2 history and relevant current/frozen contracts/code directly; reverify PA34-PLAN-REV-01..06 and PA34-PLAN-FIX-01; check six unit/two IT manifests and all unchanged 60+28=88 unique identities; verify section8's21 raw hashes and17 exact migration filenames; check all planned path ownership and exactly 23 future S1 rows (19 new/4 existing, eight production/ten test-helper Java); verify the reader declaration/module edge and frozen CI regex wording; search stale single-fix and combined-count bookkeeping; verify only the cumulative plan and new fix-3 artifact are changed by this retry; check strict UTF-8, final LF, conflict markers, U+FFFD, trailing whitespace and git diff --check, including untracked artifacts via no-index. Do not build, install, run containers, write target/tmp evidence/private helpers, stage or modify metadata.

This WSL worktree's .git contains a Windows `D:/...` pointer that plain Linux git cannot resolve. Use existing metadata read-only via explicit `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call18`, `GIT_COMMON_DIR=/mnt/d/codex-call/.git`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`; do not repair .git/config or refresh/write the index. Capture this retry's actual read-only baseline and plan-start raw identity in fix-3, not obsolete plan/fix-gate counts or Control hashes. Preexisting Control changes, prompts, helpers, fix-1/fix-2 and all four reviews remain untouched. Only `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md` and new `docs/gateflow/pa34-operations-activation-history-plan-fix-3.md` are authorized writes for this retry; neither belongs to section5. Record preservation of every other worktree path in fix-3.

Completion evidence in fix-3: exact changes; before/after plan raw SHA256/bytes/lines; each accepted finding's plan-fix status (not review pass); 23-row S1 arithmetic and unchanged 88-identity/inventory arithmetic; frozen/hygiene/static checks and exit results; classified owned residuals; runtime NOT RUN; exact agent-changed paths. Report fix-3's own final SHA256/bytes/lines outside artifacts only to avoid a self-referential digest. No source/test/migration/OpenAPI/CI/POM/Control/review/fix-1/fix-2 edit, implementation, re-review/later gate, stage/commit/push/PR or remote mutation. Stop for Controller-scheduled final fresh peer-isolated A/B re-review; the fix does not self-pass.
