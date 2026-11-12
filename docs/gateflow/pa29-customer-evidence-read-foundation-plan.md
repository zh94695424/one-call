# PA29 Customer Evidence Read Foundation — Plan

## 1. Authority, goal and acceptance

- Original task: `pa29-evidence-read-plan-gpt-20261112-01`; prior fix task: `pa29-evidence-plan-fix1-gpt-20261112-01`; current bounded fix task: `pa29-evidence-plan-fix2-gpt-20261112-01`; work unit: `PA29-customer-evidence-read-foundation`; date: 2026-11-12; gate: **fix**, not implementation authorization.
- Binding authority: the active PA29 goal-confirmation/adjudication checkpoint in `docs/gateflow/project-execution-control.md:13`. Implementation base is merged main `ef9d0823975ef66d1df3b70e921709d8562b4dcb` (PA28 PR #39), plus Controller goal-confirmation commit `73813534bb3a8ec8f3f3c38db6c16e0d12109b5c`, the locally verified managed HEAD and direct child of that base. Control/goal bookkeeping is not an implementation candidate. No fetch or independent remote-state assertion is made here.
- Fix authority closes `PA29-PLAN-REV-01` through `PA29-PLAN-REV-06` from `docs/reviews/plan-review-20261112-070311.md` and `docs/reviews/plan-review-20261112-071426.md`. Whole-list OPEN-risk 403, always-present `artifactRoles` (empty `[]`), trusted-application JSONB residual, and implementation-derived/sealed V017 checksum are Controller decisions, not unresolved policy questions.
- Fix-2 authority closes exactly the accepted residuals `PA29-PLAN-REV-07` through `PA29-PLAN-REV-12` from `docs/reviews/plan-review-20261112-074018.md` and `docs/reviews/plan-review-20261112-075427.md`, preserving all prior closures. The six corrections are bounded assertion literals, the actual exact dependency-set assertion, explicit fixture key wiring and portable frozen-file verification; no path/test-count expansion or implementation is authorized now.
- Implement exactly `GET /api/v1/customer/evidence` / `listCustomerEvidence` and `GET /api/v1/customer/evidence/{evidenceId}` / `getCustomerEvidence`, against the frozen contract. Do not modify either OpenAPI file or V001–V016, even for formatting/newlines.
- Motivation: customers need their current evidence metadata, including incomplete/failed evidence, without acquiring media access or inheriting sharing's SOLIDIFIED-only eligibility. Subject ownership and current lifecycle/risk policy must govern this read just as they govern the existing customer subject read.
- Success signal: exact JSON key sets, verified-session-only personal/organization isolation, all four customer organization roles and all four evidence states, deterministic keyset pages, uniform detail 404, explicit lifecycle/risk 403, one read-only customer-role snapshot, exact least-privilege delta, unchanged predecessors/contracts, and passing source=CI=XML inventories and full CI.

Goal identifiers used throughout this plan:

| ID | Binding outcome |
| --- | --- |
| G1 | Only the two frozen HTTP reads, with exact shapes and established customer errors/input precedence |
| G2 | Verified account/workspace, current lifecycle, subject ownership and all-binding OPEN-risk isolation |
| G3 | All four evidence states, immutable capture time, current evidence facts and safe ordered artifact facts |
| G4 | Stable descending evidence keyset, authenticated context-bound cursor, default 20 and limits 1–100 |
| G5 | Owner interfaces, one customer-role read-only REQUIRES_NEW/REPEATABLE_READ composition, no N+1 or writes |
| G6 | Frozen inputs, exact privilege/history/inventory proof, redaction and honest delivery limits |

Every change below is justified by one or more of these outcomes. An implementer may not introduce another objective to make the feature more complete.

## 2. Direct-code evidence and first-principles choices

The input audit covers the complete byte contents/structure of Control and architecture/data/OpenAPI specifications, the frozen customer evidence operations/schemas, V008/V014/V016 and PA25 SQL, customer authentication/application/web/configuration, Subject-number/Evidence query owners, share/media projections, directly relevant test fixtures/classes, CI/POM and current README/security/runbook boundaries. Historical acceptance prose is not substituted for current executable source.

| Evidence | Consequence and alignment |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml:918` and `:971` | Only cursor/limit are list inputs; detail has only evidenceId. Do not invent status, subjectId, owner or workspace filters. G1/G4 |
| `api/openapi/onecall-v1.openapi.yaml:4632` through EvidenceDetail | Summary requires createdAt; **EvidenceDetail has no createdAt property**. It is not a summary extension. Artifact requires only artifactRole; optional facts are omitted, never invented. G1/G3 |
| `docs/12-technology-stack-and-system-architecture.md:222` | Owners expose named read interfaces; Customer must not join owner tables itself. Current POM, not the older recommended Spring Boot 3.x text, controls dependencies: Boot 4.1.2, Modulith 2.1.2, Java 21. No dependency upgrade. G5 |
| `docs/14-production-data-model-and-flyway-schema-spec.md`, evidence dictionaries, §7.4, §9.27–9.28, §10.21 | Immutable capture/source facts and mutable evidence status are distinct; lifecycle/media processing are not implemented by reading metadata. G3/G5 |
| `docs/15-production-openapi-surface-spec.md:45` and `:102` | Metadata only; stable createdAt-desc/evidenceId-desc; no media delivery. G1/G3/G4 |
| V008 `evidence_record` and guard | identity_at_capture, consent_version, created_at, source facts are immutable; capturedAt JSON presence is checked but timestamp validity/type is not. Validate the extracted scalar, not snapshot or live identity. G3/G6 |
| V008 `media_object` | MIME permits 255 characters but API permits 128. Reject an unrepresentable detail; never truncate or widen the API. G1/G3 |
| V008 `evidence_artifact` / `recording` | One ORIGINAL maximum, arbitrary DERIVED artifacts; no guarantee every evidence state has an artifact; recording.media_object_id is not unique. Do not multiply artifacts by joining every recording with the same media ID. G3/G5 |
| `CustomerBearerAuthenticator`, `CustomerSessionVerifier`, `VerifiedCustomerSession`, `CustomerBusinessNumberQueryService.requireScope` | Reuse audience `onecall:customer-session:v1` and attested workspace. No new verifier/issuer or client-selected tenant. G2 |
| `JooqCustomerBusinessNumberAccessRepository.requireActiveContext` | Reuse active account/org/membership and exact four-role policy, before resource access. G2/G5 |
| `CustomerSubjectDetailQueryService`, `CustomerPersistenceConfiguration` | Reuse transaction/SET LOCAL ROLE/assertion/failure sanitization and shared qualified customer DSL patterns, not the subject response itself. G2/G5/G6 |
| `CustomerSubjectAccessReader` / `JooqCustomerSubjectAccessReader` | Existing detail ownership and all-binding risk semantics are correct. Add two batch methods to this same named seam, leaving old methods/PA28 behavior intact. G2/G5 |
| `CustomerBoundaryFilter`, `CustomerSubjectsController`, `CustomerBusinessNumbersController`, `CustomerProblemWriter` | Canonical route closure precedes auth; list limit precedes cursor; detail strict UUID validation precedes lazy service; extra selectors/GET bodies are ignored. Preserve this precedent. G1/G2/G4 |
| `CustomerBusinessNumberCursorCodec` | Reuse its small binary HMAC design, **not** its endpoint domain or ascending binding-ID comparison. Evidence requires descending UUID and a separate key. G4 |
| `JooqShareableEvidenceProjectionReader` and `JooqEvidenceMediaObjectReader` | Share projection has full capture JSON and an ORIGINAL requirement; media reader assumes SOLIDIFIED/VALIDATED and resolves private object identity. Neither is the PA29 interface. Only reuse the source-recording join principle internally. G3/G5/G6 |
| V014 hardening; V016 final ACL; PA25 `$frozen$` | Customer has only evidence schema USAGE and evidence_record.subject_id. Metadata cannot run with existing grants. Nineteen exact column SELECT additions are necessary; no new schema/role needed. G5/G6 |
| `CustomerSubjectDetailBoundaryTests`, CI literal method maps and POM includes | Current exact baseline is 44/375 Surefire plus 19/302 Failsafe, 63/677 total; 678 annotation-line hits contain one historical comment. Preserve identities, not just totals. G6 |

The checked-out Windows worktree has a nonportable `.git` pointer: native Linux `git status --short` fails on `D:/codex-call/.git/worktrees/codex-call4`, but installed Windows **`git.exe` works** and verifies HEAD73813534 over main ef9d0823. No repair is needed or authorized. Control and untracked plan/reviews/private prompts were already present; scope proof compares against that initial state, not an invented clean tree. See §11.2 for exact read-only Git commands and bookkeeping exclusions. Remote state is not freshly fetched/attested. No generated XML exists here; historical runtime pass claims are not fresh executions.

## 3. Non-goals and exact scope

No media bytes, file opening, playback, seek, range, streaming, download, media credential, object identity, bucket/key/version/URL, crypto-key reference, share token/code, raw identity JSON, evidence snapshot or evidence_event response. No share projection/reconstruction, share creation/admission/rotation/revocation, evidence creation/solidification/state mutation, consent/claim/activation/trial/order/operations API, live identity enrichment, UI/client generation, issuer/TTL/WeChat mapping, generic repository/authorization/cursor framework, Redis/cache, new pool, advisory/row lock, index/table/view/function migration, cloud integration, provisioning execution or production acceptance.

Approved optional projections are deliberately deterministic: summaries always include distinct rank-ordered `artifactRoles` (including `[]`); detail includes `solidifiedAt` only when present and omits absent duration. No other optional feature is inferred. This is exactly two metadata reads, not a bridge into PA19 media delivery or PA14 sharing commands. G1–G6.

## 4. Literal HTTP, projection and precedence contract

### 4.1 Routes and non-authoritative input

Extend only `CustomerBoundaryFilter.admittedPath` with exact collection equality and detail shape `/api/v1/customer/evidence/[^/;%]+`. Preserve its method check and exact `contextPath + servletPath == requestURI` check. Add only two `@GetMapping` methods, with the frozen operation names, `produces=application/json`, web profile and servlet condition.

| Request | Result / rule |
| --- | --- |
| GET exact collection | 200 EvidencePage after the ordered checks below |
| GET one detail segment containing a canonical full UUID spelling | 200 EvidenceDetail, 404 missing/unavailable, or applicable problem |
| Uppercase hex UUID | Accepted and converted to UUID, as PA28 |
| `bad`, abbreviated UUID, dotted UUID, invalid hex | 422 VALIDATION_FAILED after auth/workspace structure; no service lookup |
| Empty ID / trailing slash on either route / extra path segment / `.json` collection alias | 404 NOT_FOUND before authentication |
| HEAD, POST, PUT, PATCH, DELETE, OPTIONS on either route | 404 NOT_FOUND, not Spring's implicit HEAD or 405 |
| Semicolon/percent alias and servletPath/requestURI disagreement | Existing boundary rejects; direct-filter probes assert 404. Container-rejected malformed HTTP never constitutes another API route |
| Unknown query keys, duplicates of unknown keys, account/subject/owner/workspace/status/sort/role selectors, arbitrary headers/cookies/request attributes | Ignored; cannot select tenant, subject, status or sort |
| GET body, invalid JSON, non-JSON Content-Type, fixed-length/chunked body | Ignored and never parsed; no new body/content-type validation |
| Detail `cursor`/`limit`, including duplicates and invalid values | Ignored, since detail declares neither; detail requires no cursor key |
| List cursor/limit | Only these two names are parsed; duplicate limit -> 422; duplicate cursor -> 400 |

`limit`: absent -> 20; one non-null decimal `[0-9]+` token parsed without overflow and numeric value 1–100 -> accepted. Thus existing precedent accepts leading zero spelling such as `020`; reject empty, whitespace, signs, fractions, zero, 101 and integer overflow. Do not tighten the existing lexical precedent in this feature.

### 4.2 Ordered checks and HTTP errors

| Order | Check | Failure / precedence |
| --- | --- | --- |
| 1 | Canonical path/method/requestURI gate | 404 NOT_FOUND even if bearer missing |
| 2 | Single well-formed Authorization bearer and existing verifier/audience | Missing/malformed/duplicate/rejected/expired/revoked/wrong-audience -> 401 UNAUTHENTICATED; missing/ambiguous/outage/malformed verifier result -> 503 SERVICE_UNAVAILABLE |
| 3 | Trusted caller and selected workspace structure via requireScope | Null/invalid attestation -> 503; precedes UUID/limit/cursor |
| 4a | List limit; then duplicate/structural cursor validation | 422 VALIDATION_FAILED wins over invalid cursor |
| 4b | Detail strict full UUID regex then UUID conversion | 422; no lazy service/DB lookup |
| 5a | List lazy cursor codec/key, cursor MAC + context binding; then lazy service | Structurally bad cursor -> 400 without key lookup; structurally valid cursor with unavailable key -> 503; configured-key tamper/replay -> 400 CURSOR_INVALID; service bean unavailable -> 503 |
| 5b | Detail lazy service, no cursor provider | Missing/failed bean -> 503 |
| 6 | Start new read-only repeatable-read transaction; SET LOCAL ROLE and current_user assertion | Role/start failure -> 503, no owner read |
| 7 | ACTIVE account, then if org ACTIVE org/membership and four allowed roles | 403 FORBIDDEN, before missing evidence/ownership/risk; account absence also 403 under existing resource lifecycle policy |
| 8a | Detail find header, find owned subject | Missing evidence/subject/unowned/foreign -> same 404; **no foreign risk or artifact lookup** |
| 8b | List owned subject IDs, then those with evidence | Foreign/unowned subjects silently excluded; none -> exact empty page |
| 9 | OPEN risk on evidence subject(s), any binding status | 403 FORBIDDEN; no metadata/artifact response, before detail fact validation/list fetch |
| 10 | Validate facts, compose response, finish transaction successfully | Detail persisted unrepresentable/unavailable evidence -> 404; invalid list projection -> 500 INTERNAL; null owner/service container or wrong owner-returned ID -> 500 INTERNAL |
| 11 | Transaction completion and serialization | SQL/jOOQ/Spring data/transaction/bean failures, including commit of empty/not-found reads -> sanitized 503; unexpected programming faults -> sanitized 500; no partial page |

Use existing five-key Problem `{type,title,status,code,traceId}` with existing titles/status mapping and server-generated trace UUID. 401 adds `WWW-Authenticate: Bearer`. Success/problems retain `Cache-Control: no-store`, `Pragma: no-cache`, `X-Content-Type-Options: nosniff`, no Set-Cookie and no exception/cause/SQL/credentials. Existing shared Accept negotiation/error dispatch remains unchanged: unacceptable or malformed negotiation uses the established sanitized INTERNAL boundary, not a new 406 surface. Test it. The filter's Accept check occurs after authentication and before controller input parsing.

429 RATE_LIMITED remains reserved vocabulary, as existing customer reads; do not introduce a rate limiter or fabricate a 429 execution claim. The two operations add no 409/410/share/media errors. Detail never emits CURSOR_INVALID.

### 4.3 Exact success keys

| Object | Exact emitted keys and facts |
| --- | --- |
| EvidencePage | `items`, `nextCursor`, `hasMore`; empty page `{"items":[],"nextCursor":null,"hasMore":false}`; tail cursor null, never omitted |
| EvidenceSummary | `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, `createdAt`, `artifactRoles`; no solidifiedAt/artifacts/owners |
| EvidenceDetail | `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, `artifacts`, plus `solidifiedAt` only when non-null; **never createdAt or artifactRoles** |
| EvidenceArtifact | Always `artifactRole`, `mimeType`, `sizeBytes`, `sha256` for a valid linked media row; `validatedDurationSeconds` only for an ORIGINAL artifact matching the VALIDATED source recording; no null optional key |

Use explicit web records in `CustomerEvidenceDtos`, not serialization of owner records. Local optional-field annotations apply only to optional properties; they must not suppress page.nextCursor. Summary/artifact lists are immutable defensive copies. There are no map-based extras or raw jOOQ/JSONB/owner DTOs on the wire. G1/G3/G6.

## 5. Access, state, artifact and cursor semantics

### 5.1 Ownership and risk

- Personal workspace: `business_subject.owner_account_id = verified account`. Organization workspace: `owner_organization_id = selected verified organization`. Never OR the two predicates together. A member does not need to have created the organization's evidence. G2.
- Reuse `requireActiveContext` with ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR; no narrower sharing/creator policy, real-name/activation/claim gate or active-binding requirement. Subject without bindings remains readable if owned and not OPEN-risk blocked. G2/G3.
- Detail first resolves the evidence's subject, then proves ownership, then checks risk across **all** its binding IDs, including DISABLED/RETIRED and noncaptured bindings. CLEARED/missing risk rows are not OPEN. Never consult capture.bindingId as the customer access boundary. Foreign evidence remains 404 even if its subject has OPEN risk. G2.
- List ownership exclusion precedes pagination. To give OPEN risk deterministic semantics independent of page size/cursor, compute owned subjects that actually have evidence, then check OPEN risk for that entire set, including off-page evidence subjects. Any such OPEN risk -> whole request 403, not omission/partial page; risk on an owned subject with **no evidence** does not block this endpoint. Foreign/unowned risk does not block. This is a conservative direct application of the confirmed evidence-subject risk rule, not a new filter. Explicit tests pin both off-page and no-evidence cases. G2/G4.

### 5.2 Evidence and safe facts

- All PENDING/SOLIDIFIED/QUARANTINED/FAILED rows are eligible. Do not use a SOLIDIFIED WHERE clause. Detail rereads evidence_record on every invocation; no share snapshot, cached result, evidence_event or live identity table. G3/G5.
- SQL extracts only `CASE WHEN jsonb_typeof(identity_at_capture->'capturedAt') = 'string' THEN identity_at_capture->>'capturedAt' END AS captured_at_text`; parse as an ISO offset date-time into Instant. Require a representable valid date-time with offset; never parse current wall clock or snapshot.capture as fallback. Do not select/return JSONB, JSON text, other identity keys or snapshot. Missing/null/wrong-type/invalid capturedAt: persisted detail unavailable -> 404; list selected row invalid -> INTERNAL 500, not silent loss. G3/G6.
- IDs, status, sourceType, consentVersion, createdAt and solidifiedAt come only from evidence_record. Require expected ID/subject binding, exact state enum, sourceType CALL_RECORDING, consentVersion 1–64 Unicode characters, nonblank, no NUL/unencodable UTF-8; timestamps non-null where required; createdAt finite, PostgreSQL-representable and microsecond-aligned for cursor. SOLIDIFIED iff solidifiedAt present; do not fabricate solidifiedAt for any other state. G3/G4.
- Artifact roles are ORIGINAL/DERIVED only. Summary roles are distinct and sorted ORIGINAL=0/DERIVED=1; zero artifacts -> `[]`. Detail artifacts sort by role rank, sha256 ascending, sizeBytes ascending, MIME ascending. Use explicit CASE rank and deterministic `COLLATE "C"` for text ties or an equivalent Java comparator; never lexical role sorting. Equal complete safe facts are indistinguishable and may remain duplicate entries; do not deduplicate real DERIVED artifacts using safe-fact equality. G3.
- For detail, media SHA is lower-case 64 hex; size is a nonnegative long within API bounds (V008 normally enforces >0); MIME is nonblank 1–128 characters, valid UTF-8 without NUL. Missing linked media or invalid required safe facts makes detail unavailable 404, never silently drops an artifact. No ORIGINAL requirement or minimum artifact count is imposed on any evidence state. G3.
- Avoid recording fan-out: duration correlation is exactly `artifact.artifact_role = 'ORIGINAL' AND recording.id = evidence_record.source_ref_id AND recording.media_object_id = artifact.media_object_id AND recording.status = 'VALIDATED'`. Apply this predicate inside the optional recording LEFT JOIN/CASE, never as a WHERE clause that drops artifacts. Source recording ID is unique. Expose its positive validated_duration only on that ORIGINAL artifact. Every DERIVED artifact omits duration, **including a legal DERIVED row sharing the ORIGINAL media_object_id**; absent/unvalidated/misbound source also omits duration. Never copy original duration or read declared_duration. Multiple unrelated recordings referencing the media cannot create duplicate artifacts or supply duration. Invalid non-null duration in an allegedly validated ORIGINAL match -> unavailable 404. G3/G5.
- Header facts may exist without artifacts. PENDING/FAILED/QUARANTINED detail with zero artifacts returns `artifacts: []`; this is not media availability and does not grant playback rights. Unrepresentable stored detail facts are the unavailable 404 case; dependencies failing to execute remain 503, not false 404. G1/G3/G6.

### 5.3 Cursor and seek

New `CustomerEvidenceCursorCodec` follows the established 93-byte/124-character unpadded canonical base64url version-1 envelope: version(1), binding SHA256(32), createdAt epoch seconds(8), nanos(4), evidence UUID(16), HMAC-SHA256(32). HMAC domain is `onecall:customer-evidence:cursor:v1\n`. Binding input is exactly `onecall:customer-evidence:v1\ncreatedAt:desc,evidenceId:desc\n` + canonical account UUID + `\n` + (`PERSONAL` or `ORGANIZATION:` + canonical organization UUID) + `\n`. G4.

Use a separate canonical base64 **32-byte** key property `onecall.customer.evidence.cursor-hmac-key`; no fallback to shares/business-number keys, random defaults or cached configuration repairs. Missing/invalid key -> sanitized 503 when list needs codec; detail never resolves it. No key material in string forms/logs. Limit is deliberately not cursor-bound, matching predecessor behavior; endpoint/account/workspace-kind/org/sort are bound. Different endpoint domains must reject replay even with the same test key. MAC/binding comparisons use `MessageDigest.isEqual`.

Reject wrong alphabet/padding/length/version/re-encoding, invalid nanos, submicrosecond timestamp, PostgreSQL/out-of-range Instant and every MAC/payload mutation with CURSOR_INVALID 400. Structural validation precedes lazy key resolution. Only the anchor time/UUID crosses decode to the query; no client-owned context survives.

Seek predicate: `(e.created_at, e.id) < (CAST(? AS timestamptz), CAST(? AS uuid))`; SQL `ORDER BY e.created_at DESC, e.id DESC LIMIT limit+1`. Validate strict descending sequence against input anchor and within page. Java UUID signed compareTo is not PostgreSQL UUID ordering: compare canonical fixed-width UUID strings, or explicit unsigned UUID bits. Fetch max 101 rows; return first limit; hasMore iff one lookahead exists; next anchor is **last returned**, never lookahead. Empty/tail page has no anchor/null cursor. Subsequent requests are fresh snapshots, not one cross-request snapshot: newer insertions stay before the anchor, no duplicates; new rows behind the anchor may appear. No offset, total count, server-side cursor or snapshot token. G4/G5.

## 6. Exact interfaces, signatures, wiring and data flow

### 6.1 Owner interfaces

Keep `subject_number.customersubjectquery` named `customer-subject-query`; preserve its two methods and add exactly these two batch methods:

```java
Optional<UUID> findOwnedSubject(UUID subjectId, UUID accountId, UUID organizationId);
boolean hasOpenRisk(UUID subjectId);
Set<UUID> findOwnedSubjectIds(UUID accountId, UUID organizationId);
boolean hasOpenRisk(Set<UUID> subjectIds);
```

`organizationId == null` means personal, exclusively; accountId never null. The batch risk query uses EXISTS over bindings JOIN risk_state with subject_id IN supplied IDs and state OPEN, no binding-status predicate. Empty input -> false without SQL. Return distinct non-null subject IDs; never identity/number data. The two old methods retain exact PA28 behavior. G2/G5.

Create `evidence.customerevidencequery`, named **`customer-evidence-query`**, interface `CustomerEvidenceReader`, containing these five methods and nested immutable records:

```java
Set<UUID> findSubjectIdsWithEvidence(Set<UUID> ownedSubjectIds);
List<Header> fetchPage(Set<UUID> subjectIds, Optional<Anchor> anchor, int fetchSize);
Map<UUID, List<String>> findArtifactRoles(Set<UUID> evidenceIds);
Optional<Header> findHeader(UUID evidenceId);
List<Artifact> findArtifacts(UUID evidenceId);

record Anchor(Instant createdAt, UUID evidenceId) { }
record Header(UUID evidenceId, UUID subjectId, String status, String sourceType,
              String capturedAtText, String consentVersion, Instant createdAt,
              Instant solidifiedAt) { }
record Artifact(String artifactRole, String mimeType, Long sizeBytes,
                Integer validatedDurationSeconds, String sha256) { }
```

Header is an internal-to-composition safe scalar carrier, not a serialized DTO; capturedAtText contains only the extracted scalar. Evidence reader never accepts customer internal types, Spring transaction objects or DSLContext in its public signatures. It does not own auth/risk SQL. Its constructor receives qualified `customerQueryDsl`. Header lookup SQL retains source_ref_id only inside the owner when needed for the artifact query; that private join ID never crosses the interface. G3/G5/G6.

`findSubjectIdsWithEvidence` performs DISTINCT subject_id restricted to provided IDs; empty -> empty set. `fetchPage` applies subject set, keyset and limit in SQL; fetchSize must be 2–101. `findArtifactRoles` batches at most 100 returned evidence IDs and groups/distinct-orders roles; missing map key means no artifacts. Empty ID set -> empty map. No media/recording join needed for summary roles. `findHeader` -> Optional.empty only for no row; do not swallow SQL failures. `findArtifacts` uses one evidence/artifact/media/source-recording read with explicit selected safe fields and ordering; use LEFT JOIN media to detect impossible missing media rather than quietly discard it. No wildcard SELECT, snapshot/identity selection or evidence_event access.

### 6.2 Customer service and controller signatures

`CustomerEvidenceQueryService` constructor: `(TransactionTemplate transaction, DSLContext context, JooqCustomerBusinessNumberAccessRepository lifecycle, CustomerSubjectAccessReader subjects, CustomerEvidenceReader evidence)`; the first three are Customer-owned infrastructure, the last two public owner seams.

```java
record Request(Optional<Anchor> anchor, Optional<Integer> limit) {
    int effectiveLimit();
}
record Summary(UUID evidenceId, UUID subjectId, String status, String sourceType,
               Instant capturedAt, String consentVersion, Instant createdAt,
               List<String> artifactRoles) { }
record Page(List<Summary> items, Optional<Anchor> nextAnchor, boolean hasMore) { }
record Detail(UUID evidenceId, UUID subjectId, String status, String sourceType,
              Instant capturedAt, String consentVersion, Instant solidifiedAt,
              List<Artifact> artifacts) { }
Page listCustomerEvidence(TrustedCustomerCaller caller, Request request);
Optional<Detail> getCustomerEvidence(TrustedCustomerCaller caller, UUID evidenceId);
```

Request rejects null Optional containers, invalid optional anchor fields, limit outside 1–100 with existing customer codes. Null caller/workspace -> existing SERVICE_UNAVAILABLE; null external detail ID -> VALIDATION_FAILED. Invalid persisted header/artifact scalars follow §5.2; null method container/row, extra map keys, foreign returned subject, wrong requested ID, duplicate evidence ID, overlarge fetch or nonmonotonic keyset is an owner-contract fault -> INTERNAL, not 404. For detail, an actual missing header/owned subject -> empty Optional; persisted unrepresentable content after ownership/risk -> empty Optional. Never turn SQL exceptions into empty Optional. Unknown exceptions do not expose their messages.

Controller methods:

```java
CustomerEvidenceDtos.EvidencePage listCustomerEvidence(Principal principal,
        HttpServletRequest request);
CustomerEvidenceDtos.EvidenceDetail getCustomerEvidence(Principal principal,
        String evidenceId, HttpServletRequest request, HttpServletResponse response);
```

Use `@PathVariable("evidenceId")` string rather than Spring UUID conversion. Constructor dependencies are qualified ObjectProvider<CustomerEvidenceQueryService>, qualified ObjectProvider<CustomerEvidenceCursorCodec>, CustomerProblemWriter. Do not eagerly resolve the codec on detail. Use existing handlers and problems.notFound after service transaction completes.

### 6.3 Call order and resource ownership

List: filter/auth -> scope -> limit -> cursor structure -> lazy codec/decode -> lazy service -> single transaction -> SET LOCAL ROLE/assert -> lifecycle -> subjects.findOwnedSubjectIds -> evidence.findSubjectIdsWithEvidence -> subjects.hasOpenRisk(evidenceSubjects) -> evidence.fetchPage(limit+1) -> validate header/order -> trim lookahead -> evidence.findArtifactRoles(returned IDs) -> validate/compose -> commit -> encode next anchor -> exact wire mapping.

Detail: filter/auth -> scope -> UUID -> lazy service -> same template transaction -> SET LOCAL ROLE/assert -> lifecycle -> evidence.findHeader -> subjects.findOwnedSubject -> subjects.hasOpenRisk(single ID) -> validate header -> evidence.findArtifacts -> validate/order -> compose -> commit -> wire map or uniform 404. Missing/foreign branch never queries artifacts. No response is written inside the transaction.

Use one new lazy `customerEvidenceTransactionTemplate` bean, same `customerQueryTransactionManager` and `customerQueryDsl`, readOnly=true, propagation REQUIRES_NEW, isolation REPEATABLE_READ. Add lazy `customerEvidenceQueryService` and `customerEvidenceCursorCodec` beans; enable CustomerEvidenceCursorProperties alongside existing property classes. New Evidence configuration contributes only lazy `customerEvidenceReader` qualified to customerQueryDsl, web profile, no pool/manager/template. Existing Subject reader bean is reused. All owner calls execute on the same transaction-aware connection; no owner starts another transaction or switches roles. G5.

Do not refactor existing error/transaction handling into a generic framework. Copy the small established sanitizing boundary, including identity-cycle-safe cause traversal and outer transaction-completion catch. New template keeps PA28/PA27/Me isolation settings unchanged. Runtime default/nonweb stays DB-free; missing web credentials/verifier/key remain lazy failures. No new autoconfiguration or test-profile verifier bypass.

List query count is fixed, not proportional to evidence/subject count: two role commands, one/two lifecycle reads, one ownership query, one DISTINCT evidence-subject query, one risk EXISTS, one page query and at most one returned-ID artifact-role query (at most 9 statements for org; skips for empty sets). Detail max 8 including org lifecycle/role commands, header, ownership, risk and artifact facts. Query counts exclude test-only observer SQL. No per-item read. Avoid speculative query fusion that embeds foreign owner SQL in Customer or Evidence.

The batch subject sets are not page-size bounded: this is the simplest ownership-preserving composition with current owner interfaces, and the off-page risk check necessarily sees all relevant evidence subjects. Use typed bind arrays/IN predicates, never interpolated IDs. Do not claim constant memory or a proven large-tenant SLO. Capacity verification/possible owner-owned relational read model is a residual, not authorization for a cache, new index, function/view or cross-owner join here. G2/G5/G6.

Module change: append only `evidence::customer-evidence-query` to Customer's existing nine dependencies -> **10**. No other production allowed-dependency edge changes. Subject-number/Evidence remain independent of Customer internals, each other and Sharing; named-interface metadata exposes no internal implementation. G5.

## 7. Proven privilege delta, V017 and history

### 7.1 Baseline and exact nineteen additions

V016 customer ACL is **34 = 6 schema USAGE + 28 column SELECT**. Existing subject owner/risk/lifecycle columns cover all new Subject queries. Existing evidence schema USAGE and evidence_record.subject_id SELECT are retained. Every new table reference is under evidence_owner and can use those existing schema privileges. Current independent PA25 oracle: **269 = customer 34 + evidence_role 16 + other five roles 219**.

Authorize one append-only **`backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql`** only. Each row below expands into one tuple per listed column: `(customer_iam_role, column, qualified table, column, SELECT, evidence_owner, false grantable)`.

| Table | New column SELECT tuples | Count | Exact need |
| --- | --- | ---: | --- |
| evidence.evidence_record | id, status, source_type, source_ref_id, identity_at_capture, consent_version, created_at, solidified_at | 8 | ID/seek/current facts, scalar capturedAt extraction and source-recording predicate |
| evidence.evidence_artifact | evidence_id, media_object_id, artifact_role | 3 | Batched roles and evidence-to-artifact/media relation |
| evidence.media_object | id, sha256, size_bytes, mime_type | 4 | Join and four safe immutable facts |
| evidence.recording | id, media_object_id, status, validated_duration | 4 | Unique source-recording identity/media/VALIDATED predicate and optional duration |

Arithmetic: **delta 19 = 0 schema + 19 column**; **customer 53 = 6 schema + 47 column**; target-role snapshot **50 -> 69** if retaining V016's customer/evidence target pair; **global 288 = 53 + 16 + 219 = 269 + 19**. All tuples issued under evidence_owner, no grant options. No evidence_role membership for Customer. The source_ref_id/media IDs are join predicates only, not public projections. Every addition is consumed by the specified SQL; no column is added for hypothetical future filters.

Negative grant boundary: no evidence_record.snapshot/snapshot_sha256/call_id/session_id/reservation_id/source event/revision columns, artifact.id/created_at, media bucket/object_key/object_version_id/storage_provider/crypto_key_ref/worm_status, recording declared facts/call IDs/protocol recording_id, **any evidence_event column**, sharing secrets, live identity fields, other business schemas or routine/sequence expansion. No INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER/table-wide SELECT/schema CREATE/new role/default ACL or GRANT OPTION.

PostgreSQL column ACL cannot grant only a JSON path: SELECT(identity_at_capture) necessarily authorizes the whole column to a raw SQL holder. Likewise approved join columns reveal internal IDs to that trusted DB role. The API/owner SQL extracts only capturedAt and never transports/logs full JSON; this is an explicit trusted-application boundary, **not RLS or JSON-path confinement**. Controller accepted this Security/Database residual. A future requirement for raw-role inability to read the rest of that JSON must stop: a view/function or schema change would exceed this privilege-only plan and needs new goal confirmation. Do not falsely claim the nineteen grants enforce JSON-path security.

### 7.2 Reconciliation and preservation

Carry forward V016's fail-closed catalog reconciliation pattern rather than using unconditional permissive grants. Freeze its complete current target ACL as old (50), append exactly nineteen final tuples (69), preserve evidence_role's sixteen. Preflight accepts only exact clean V016 pre-state or complete V017 final state for replay, with exact predecessor/history/provenance checks; partial/widened/wrong-grantor/grant-option states reject atomically without repair. Validate the sixteen predecessor SQL history rows, exact versions/scripts/descriptions/checksums/ranks/success and permitted 16/17 history cardinality for migration/replay. Do not weaken schema owner, object definition, managed-role/login, settings, provenance, database/public/default/dependency checks. G6.

Before/after invariant: role OIDs/attributes/credential verifiers, comments/provenance, six logins, seven effective roles, **four** membership edges, memberships' ADMIN FALSE/INHERIT FALSE/SET TRUE, role/db settings, owners, object definitions, default ACLs, database ACLs, PUBLIC and unrelated explicit/effective privileges and business rows unchanged. LQ->sharing_role; LC->customer_iam_role; LW->sharing_command_role and evidence_role; public/admission/media-reader retain direct-login boundaries. No new identity or membership. Grants are transactional; use owner SET LOCAL ROLE, restore safely. No repair or dynamic error text.

Keep thirteen failure categories with `V017_` prefix: AUTHORITY, PROVENANCE, HISTORY, SETTINGS, ATTRIBUTES, TOPOLOGY, OWNERSHIP, ACL, DATABASE, DEFAULT_ACL, DEPENDENCY, PRESERVATION, RECONCILIATION, suffix `_REJECTED`. Final-state direct script replay on V017 preserves everything. Do not replay frozen V014/V015/V016 against later enlarged ACLs and make them accept that state.

PA25 script: append the nineteen independently literal expected tuples, update exact resolved cardinality **269 -> 288**, append version `017`, script `V017__customer_evidence_read_privileges.sql`, description `customer evidence read privileges`, SQL type, rank 17, success true and **actual final Flyway checksum**; history **16 -> 17**. Keep V001–V016 literals, especially V016 checksum 1669606497. Compute V017's checksum from final Flyway migration metadata in implementation, seal that exact literal in PA25/tests/report; no fabricated checksum or checksum wildcard. Current PA25 apply/check must reject V016-only and must not repair ACL/history damage. G6.

## 8. Exact implementation file/module allowlist

This section authorizes paths for a later accepted implementation slice, not implementation edits during this fix gate. The **only writes now** are this plan and `docs/gateflow/pa29-customer-evidence-read-foundation-plan-fix-2.md`; fix-1 remains read-only historical input. No wildcard directory authorization is implied. No Control edit by implementer; neither plan/fix/reviews nor Controller goal bookkeeping is an S1 implementation candidate.

### 8.1 Production Java

New, under `backend/src/main/java/com/onecall/core/` (nine files):

1. `evidence/customerevidencequery/CustomerEvidenceReader.java`
2. `evidence/customerevidencequery/package-info.java`
3. `evidence/internal/customerevidencequery/JooqCustomerEvidenceReader.java`
4. `evidence/internal/customerevidencequery/CustomerEvidenceQueryConfiguration.java`
5. `customer_iam/internal/application/CustomerEvidenceQueryService.java`
6. `customer_iam/internal/web/CustomerEvidenceController.java`
7. `customer_iam/internal/web/CustomerEvidenceDtos.java`
8. `customer_iam/internal/web/CustomerEvidenceCursorCodec.java`
9. `customer_iam/internal/web/CustomerEvidenceCursorProperties.java`

Modify only these five existing production Java files under the same root:

1. `subject_number/customersubjectquery/CustomerSubjectAccessReader.java`: two batch signatures only.
2. `subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java`: owner-only batch SQL.
3. `customer_iam/internal/persistence/CustomerPersistenceConfiguration.java`: properties and three lazy beans, no new resources.
4. `customer_iam/internal/web/CustomerBoundaryFilter.java`: two admitted GET shapes only.
5. `customer_iam/package-info.java`: one Evidence named dependency, nine -> ten.

### 8.2 New tests and fixture

Under `backend/src/test/java/`:

1. `com/onecall/archtests/CustomerEvidenceWebTests.java`
2. `com/onecall/core/customer_iam/internal/application/CustomerEvidenceQueryServiceTests.java`
3. `com/onecall/core/customer_iam/internal/web/CustomerEvidenceCursorCodecTests.java`
4. `com/onecall/archtests/CustomerEvidenceConfigurationTests.java`
5. `com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`
6. `com/onecall/archtests/container/CustomerEvidenceIT.java`
7. `com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java`
8. `com/onecall/archtests/container/CustomerEvidenceFixture.java` (support only, no test annotations).

### 8.3 Existing tests/fixture with bounded adjustments

All adjustments preserve every existing test method name, annotation, count, and unrelated assertion. Paths below are relative to `backend/src/test/java/com/onecall/archtests/` except the explicitly qualified core test path. No blanket 16->17 replacement: PostgreSQL major version 16, historical migration targets, predecessor checksums, role ACL counts and pinned replay oracles remain unchanged.

- `architecture/ModuleBoundaryTests.java`: exact Customer allowed map gains only evidence/customer-evidence-query, count 10 in both assertions; preserve nine methods.
- `architecture/CustomerBusinessNumberBoundaryTests.java`: current dependency count 9->10 and current PA25 288/history17 assertions; frozen V015 tuple/byte assertions unchanged; preserve four methods.
- `architecture/CustomerSubjectDetailBoundaryTests.java`: Customer dependencies 9->10; SubjectAccessReader now exactly four signatures, allow Set<UUID> only for two new methods; preserve exact old UUID signatures/count methods. Keep historical V016 delta and normalized predecessor hashes; point **current** PA25 assertions to 288/17/V017. Existing method names containing 269/Sixteen remain historical inventory identities, not current requirements. Preserve four methods.
- `architecture/SharingQueryBoundaryTests.java`: the customer `getAllowedDependencies(modules).stream().map(...)` assertion uses `containsExactlyInAnyOrder`, not a size assertion. Its exact nine-entry set becomes ten by adding only `evidence::customer-evidence-query`, retaining `sharing::customer-query`, `sharing::customer-command`, `sharing::public-query`, `sharing::public-admission`, `subject_number::customer-business-number-query`, `activation_consent::customer-activation-query`, `subject_number::customer-subject-query`, `evidence::customer-subject-count-query`, `catalog_order::customer-subject-count-query` unchanged. Preserve all eight methods and every sharing/admission/gateway/playback assertion.
- `CustomerWebConfigurationTests.java`: in `customerAndSharingGraphsUseExactQualifiers`, CustomerPersistenceConfiguration's exact map grows 12->15 entries: `customerEvidenceTransactionTemplate` -> `[customerQueryTransactionManager]`; `customerEvidenceQueryService` -> `[customerEvidenceTransactionTemplate, customerQueryDsl, customerBusinessNumberAccessRepository, customerSubjectAccessReader, customerEvidenceReader]` in parameter order; `customerEvidenceCursorCodec` -> `[]` (Properties parameter remains skipped). Preserve all six methods, sharing maps, lazy/non-Primary/profile and exact qualifier assertions. Do not add helper declarations to the production configuration.
- `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java`: in `webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact`, `configuration.getDeclaredMethods()` count 12->15 only; preserve all eight methods, existing template isolation checks, lazy/Bean loop, prepared/indexed/no-logging DSL checks and original business-number qualifiers.
- `architecture/CustomerApiBoundaryTests.java`: `Files.walk(customer_iam/internal)` regular-file count 34->39, exactly the five new Customer files in §8.1; preserve all three methods and all per-file foreign-owner/SQL/reflection/profile constraints.
- `container/PostgresContainerIT.java`: update **both independent** `flyway.info().all()` surfaces. At the V013-pinned resolved inventory (source:1396), size16->17; applied remains13, pending-through-target remains empty. Retain exact V014/V015/V016 `ABOVE_TARGET`/not-applied checks; add exactly one V017 entry with version `017`, script `V017__customer_evidence_read_privileges.sql`, state `MigrationState.ABOVE_TARGET`, `isApplied()==false`. Independently, in `assertV009LiveFence` (source:10794), append that V017 script after V016 in the full ordered `all()` script list (V001–V016 entries unchanged). Applied SQL history still ends V013, replay0 and historical ACL/schema/live-fence checks remain unchanged. Preserve all64 methods and the historical comment hit.

For the first surface, each row below must resolve exactly once, with state `ABOVE_TARGET` and `isApplied()==false`; V001–V013 remain `SUCCESS`, not pending:

| Version | Exact resolved script |
| --- | --- |
| 014 | V014__database_runtime_acl_rbac_hardening.sql |
| 015 | V015__customer_business_number_read_privileges.sql |
| 016 | V016__customer_subject_detail_read_privileges.sql |
| 017 | V017__customer_evidence_read_privileges.sql |

The second surface's full `assertV009LiveFence` resolved-script `containsExactly` list is, independently:

```text
V001__platform_schemas_roles_idempotency_outbox_audit.sql
V002__identity_organization_and_employee.sql
V003__subject_number_and_claim.sql
V004__catalog_order_and_payment.sql
V005__ivr_task_session_call.sql
V006__trial_entitlement_issuance_reservation_usage.sql
V007__activation_and_consent.sql
V008__evidence_media.sql
V009__sharing.sql
V010__sharing_role_least_privilege.sql
V011__sharing_public_read_least_privilege.sql
V012__sharing_admission_least_privilege.sql
V013__evidence_media_reader_role.sql
V014__database_runtime_acl_rbac_hardening.sql
V015__customer_business_number_read_privileges.sql
V016__customer_subject_detail_read_privileges.sql
V017__customer_evidence_read_privileges.sql
```

Remaining bounded existing fixture/history adjustments:

- `container/CustomerBusinessNumbersFixture.java`: latest start expected migration count 16->17; explicit numeric target still exact and never provisions automatically. In `web()` only, add one entry to the existing pre-`.run()` `MapPropertySource`: `"onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32])`. Retain the business-number key entry and every other property, pool/session/seeding/provisioning/cleanup helper unchanged. This is explicit independent test configuration for the new evidence codec, not a production key fallback; §9.6 fixes setup order.
- `container/CustomerBusinessNumberReadPrivilegeIT.java`: keep V014->V015 history/delta/replay/least-privilege tests pinned; `latest()` after V015 changes migrate1->2 and applied16->17; current PA25 post-apply ACL-size269->288. In `pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle`, after `latest(); fixture.provision()` and the explicit extra `number_hmac` grant, change only `assertThat(widenedAcls).containsAll(cleanAcls).hasSize(270)` to `hasSize(289)` (clean288 + one forbidden column). Preserve exact tuple membership, rejection/no-repair/restoration checks and every historical V015 oracle/literal unchanged; do not globally replace 269 or 270. Freeze-file loop explicitly excludes **both** V016 and V017 successors rather than asserting successors absent. Preserve eight identities and original 261 checkpoint oracle.
- `container/CustomerSubjectDetailReadPrivilegeIT.java`: make historical current() explicitly target 016; add bounded latest helper only for current PA25/provisioned-login/pool cases; preserve V015->V016 delta 8/global269/history16/replay on V016. Fresh historical case uses explicit 016 and asserts no pending **through target**, not no globally discovered V017. Current PA25 method first proves V015 and V016-only rejected without mutation, then upgrades V017/provisions; removal of V016 tuples and V016 history damage still rejected against current288. Exact V016 oracle remains independent 269; current login/pool cases compare an independently extended 288 oracle. Do not weaken APPROVED columns at historical V016 target. Preserve ten identities.
- `container/ProductionDatabaseIdentityProvisioningIT.java`: current latest17, independent288 literal oracle, final-row mutation rank17/version017/noncanonical17/wrong rank18, cleanup versions ('017','17'), extra-version probes ('017','018'), duplicate rank1017. Independently, in `frozenMigrationsLogicalAclsAndHistoryRemainUnchanged`, change the current `assertThat(flyway.info().applied()).hasSize(16)` to `hasSize(17)` after migrate0/validate; preserve its migrationHashes equality, V001–V013 hash filter, comments/ACL/marker assertions, V016 predecessor-row corruption probes and all older checks. Preserve all twenty methods.

- `container/CustomerMeFixture.java`: `start()` fresh unfiltered `migrationsExecuted` 16->17 and `info().applied().hasSize(16)` ->17; retain validate, role/login grants and all no-write/reset/cleanup behavior.
- `container/SharingQueryFixture.java`: `start()` fresh unfiltered migrate16->17 and applied16->17; no change to role graph, projection grants, reset or shutdown.
- `container/SharingQueryServiceIT.java`: `queryRuntimeStartsAfterFixtureProvisioning` applied-size16->17 only.
- `container/SharingRoleLeastPrivilegeIT.java`: `start()` fresh migrate16->17; `v010MigrationIdentityAndPostgres16` applied-size16->17 and append index `[16]` version `017` / script `V017__customer_evidence_read_privileges.sql`. Preserve indexes0–15 and V010 identity/role assertions. Keep historical V009/V010/V011 upgrade/replay inputs and counts unchanged.
- `container/SharingAdmissionRoleLeastPrivilegeIT.java`: `v012CreatesExactAdmissionRoleAndIsReplaySafe` applied-size16->17 only; repeat migrate0, V012 role/ACL/history/replay assertions unchanged.
- `container/EvidenceMediaReadRoleLeastPrivilegeIT.java`: `roleAttributesPasswordTopologyAndV013HistoryAreExact` applied-size16->17 only; exact V012/V013 history, role topology, no successor-role privilege expansion and replay0 unchanged.
- `container/EvidenceMediaReadIT.java`: nested `Fixture.start()` fresh unfiltered migrate16->17 only; all media projection/privilege facts unchanged.
- `container/PublicShareAdmissionIT.java`: `publicGetAndCreatorCreateRevokeRotateListMeRemainUnchanged` applied-size16->17 only; all admission/creator/public/Me behavior assertions unchanged.
- `container/CustomerBusinessNumbersIT.java`: `postgres16FullChainAndCustomerRuntimeRoleAreUsed` successful-history count string `"16"` -> `"17"`; latest `fixture.start(null)` remains unfiltered, every business-number assertion unchanged.
- `container/CustomerSubjectsIT.java`: `postgres16LatestChainAndCustomerRuntimeRoleAreUsed` successful SQL-history count string `"16"` -> `"17"` only; unchanged subject behavior and role assertions.
- `container/DatabaseRuntimeAclRbacHardeningIT.java`: choose **V017 current expectations**, not historical pinning for its two `current` instances. In `v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle` and `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects`, after pinned V014, unfiltered current migrate2->3 (V015,V016,V017), applied16->17, validate then existing PA25 apply/check sequences at current288/history17. Independently, in `assertFrozen()`, change only the unfiltered migration-resource cardinality `assertThat(resources).hasSize(16)` to `hasSize(17)` because V017 is now resolved on disk. Preserve the filename filter/sort, complete `FROZEN` hash map, `FROZEN.containsKey(version)` guard, normalization/hash comparison and every other frozen block/assertion unchanged; do not append V017 to the historical hash map or stop checking predecessors. Preserve all twelve methods. `prefix` target013 and `latest` target014, V014 fresh14/upgrade1/replay0, `assertOracle()` customer12/evidence16 target28, credentialed/uncredentialed loops, frozen role/ACL graph, all historical snapshot/rejection checks remain exact; never call V014 replay/oracle after current upgrade. PA25 independently enforces288/history17 without repairing drift; do not substitute the V014 oracle for PA25 or relax predecessor history checks.

Directly proven dependent paths, **read/run only, no edits authorized**: `container/CustomerMeIT.java` uses CustomerMeFixture; `container/CustomerSharesFixture.java` constructs CustomerMeFixture and `container/CustomerSharesIT.java` uses CustomerSharesFixture. `container/CustomerShareCommandsIT.java` uses SharingQueryFixture; `container/PublicSharesFixture.java` constructs SharingQueryFixture and `container/PublicSharesIT.java` uses PublicSharesFixture. `container/MediaPlaybackIT.java`'s Fixture constructs SharingQueryFixture; `container/MediaGatewayTransportIT.java`'s Transport constructs MediaPlaybackIT.Fixture. They inherit latest17 through the two fixed fixtures and have no independent latest16 history literal to change. All identities/behavior assertions stay intact. CustomerBusinessNumbersFixture also supplies both CustomerBusinessNumbersIT and CustomerSubjectsIT, whose own string counts are explicitly fixed above. These closure paths explain the review's fourteen affected Failsafe suites; DatabaseRuntimeAclRbacHardeningIT adds a fifteenth, not new suites. Full21-suite validation covers them all. A new proven path outside this exact allowlist requires stop-and-amend, not discretionary edits.

### 8.4 Migration, CI and documentation

- New `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql` only; V001–V016 untouched.
- Modify `backend/ops/postgresql/pa25-runtime-identities.sql` as §7 only.
- Modify `backend/pom.xml` only to add exact Failsafe includes `**/CustomerEvidenceIT.java` and `**/CustomerEvidenceReadPrivilegeIT.java`; no dependency/plugin/version changes.
- Modify `.github/workflows/ci.yml` only for PA29 literal inventory, cumulative inventory/historical subtraction, the early Surefire count and final exact totals/checks; preserve all existing CI jobs/smokes/static gates.
- Modify current-boundary sections only in `backend/README.md`, `docs/17-production-security-design-spec.md`, `docs/operations/pa25-production-database-identity-provisioning-runbook.md`: prepend PA29 current V017 boundary and label PA28 V016 historical, preserving its evidence. State endpoints/shapes/access/risk/cursor/key/config/read-only snapshot, 19/53/288/17 arithmetic, JSON ACL trust residual, test totals and no-media/no-production claim.
- New later execution report `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md` following §12.

Exact S1 path arithmetic: **53 candidates = 19 new + 34 modified**. Production Java14 = new9 + modified5; test Java31 = new8 (seven suites + one fixture) + modified23 (twenty suites + three fixtures). Thus **45 Java paths**, plus migration/PA25 SQL2, CI YAML1, POM XML1 and documentation4 (three modified boundaries + one new report) =53. Existing modified suites split Surefire7 / Failsafe13; the other eight dependent paths above are excluded from the write count. No new test methods are added to existing suites. Plan/fix/Control/reviews/private files are excluded.

No other source/test/migration/doc/config/CI/generated/private-helper path is authorized. In particular `docs/12-technology-stack-and-system-architecture.md`, `docs/14-production-data-model-and-flyway-schema-spec.md`, `docs/15-production-openapi-surface-spec.md`, all API files, security JSON/fixtures, application properties, existing sharing/media services, Control and this accepted plan are not implementation-edit targets.

## 9. Exact tests, methods and inventory

Use ordinary nonparameterized `@Test` methods so source/CI/XML identities remain literal and comparable. Preserve every one of the **677** baseline method identities; bounded adaptations above add no methods to existing classes. New **76 = 42 Surefire + 34 Failsafe** methods are exactly below; each bullet supplies its goal and required assertion family.

### 9.1 CustomerEvidenceWebTests — 12 (G1/G2/G3/G4/G6)

- `exactListRouteReturnsOnlyFrozenPageAndSummaryKeys`: exact three/eight key sets, null tail cursor, roles ordering, defaults.
- `exactDetailRouteReturnsOnlyFrozenDetailAndArtifactKeys`: seven required detail keys, optional solidifiedAt/duration omission and no createdAt.
- `unsupportedMethodsAliasesExtraSegmentsAndTrailingRoutesAreNotFound`: both paths, all non-GET, URI mismatch/semicolon/percent direct probes, auth never reached.
- `missingMalformedDuplicateRejectedAndWrongAudienceBearersFailBeforeInputs`: 401/WWW-Authenticate, no codec/service interaction.
- `unavailableVerifierAndMalformedTrustedWorkspaceFailClosed`: 503 before bad UUID/limit/cursor, ambiguous verifier included.
- `queryHeadersCookiesAttributesAndBodiesCannotSelectOwnersOrFilters`: personal/org attestation only; unknown duplicated selectors/status/subjectId/sort ignored; chunked/invalid GET bodies ignored; detail limit/cursor ignored.
- `limitDefaultBoundsLexicalDuplicatesAndOverflowMatchCustomerPrecedent`: 20,1,100,020 accepted; all rejection forms/duplicate422.
- `malformedDuplicateTamperedAndCrossContextCursorsAreInvalid`: 400, endpoint/account/org/personal replay; input vs dependency ordering.
- `fullUuidValidationAndUppercaseAcceptanceAreExact`: bad/abbreviated/dotted422, uppercase accepted.
- `authenticationScopeLimitCursorLifecycleAndMissingPrecedenceIsExact`: paired faults pin §4.2, lifecycle403 vs missing404, foreign-risk404.
- `missingServicesKeysAndUnexpectedFailuresUseSanitizedProblems`: both routes, missing key not needed by detail, null result/internal, SQL unavailable, no private error text.
- `responsesNegotiationAndExistingRoutesPreserveCustomerBoundary`: no-store headers, five-key problems, Accept behavior, old Me/shares/business-number/subject routes unaffected, secret canaries absent.

### 9.2 CustomerEvidenceQueryServiceTests — 12 (G2/G3/G4/G5/G6)

- `requestsRequireTrustedScopeAndExactLimitAndAnchorBounds`: null/malformed caller/Optional/ID/anchor matrix and defaults.
- `roleAssumptionAssertionAndLifecyclePrecedeEveryOwnerRead`: single transaction, order, all failure cutoffs.
- `listUsesOwnedEvidenceSubjectsAndChecksAllTheirBindingsBeforePage`: set restriction, off-page OPEN403, no-evidence/foreign OPEN ignored.
- `detailMissingUnownedAndForeignEvidenceNeverReadsRiskOrArtifacts`: uniform empty Optional and no follow-on calls.
- `allFourStatusesAndImmutableCaptureFactsComposeWithoutLiveIdentity`: current statuses, JSON scalar, no fallback/creation; detail excludes createdAt at wire mapping.
- `pageLookaheadAndDescendingUuidTiesUseLastReturnedAnchor`: canonical PostgreSQL UUID order across high sign bit; trim/anchor/hasMore.
- `emptyAndTailPagesHaveNoCursorAndBatchOnlyReturnedArtifactRoles`: no per-item fetch; no lookahead ID in roles query.
- `artifactRolesAndSafeFactsUseFixedRankAndSourceValidatedDuration`: role rank, complete tie breaks, omit absent optional duration, no mutable declared facts; same media ID in ORIGINAL/DERIVED owner rows yields duration only on ORIGINAL and preserves both artifacts.
- `unrepresentablePersistedDetailIsUnavailableAndInvalidListFactsAreInternal`: malformed capture/state/time/MIME/hash/size/duration matrix.
- `nullExtraDuplicateMisboundAndNonmonotonicOwnerResultsFailClosed`: null containers/rows/IDs, unexpected map keys/subjects, too many/duplicate/out-of-order headers.
- `sqlBeanTransactionAndCompletionFailuresNeverReturnPartialData`: nested and cyclic causes, commit failures on success/empty/notfound, sanitized503 vs unexpected500.
- `oneReadOnlyRepeatableNewTransactionContainsCompositionWithoutWrites`: read-only/isolation/propagation, same resource, exact call counts, no writes.

### 9.3 CustomerEvidenceCursorCodecTests — 8 (G4/G6)

- `personalAndOrganizationAnchorsRoundTripCanonically`: deterministic exact Instant/UUID.
- `payloadVersionLengthDomainAndMicrosecondPrecisionAreExact`: 93 bytes/124 chars, field layout/domain/finite PG time bounds.
- `invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected`: structural400 before key.
- `truncatedExtendedUnknownVersionAndInvalidTimesAreRejected`: timestamp and canonical re-encoding matrix.
- `everyPayloadBindingAndMacByteMutationIsRejected`: exhaustive mutation per byte, not one example.
- `accountWorkspaceKindOrganizationAndOtherEndpointReplayAreRejected`: same test key and cross-codec rejection, both directions.
- `missingMalformedWrongLengthAndNoncanonicalKeysAreUnavailable`: sanitized503 and no key in toString/errors.
- `keyReplacementInvalidatesOldCursorAndLimitIsNotBound`: rotation consequence, limit changes do not select a different scope.

### 9.4 CustomerEvidenceConfigurationTests — 6 (G1/G5/G6)

- `defaultAndNonWebProfilesRemainDatabaseFree`: controller absent outside servlet web, resources not instantiated.
- `webWiringIsLazyAndInvalidRequestsDoNotCreatePools`: auth/structure/UUID/limit/structure-cursor cutoffs, qualified ObjectProviders.
- `evidenceTemplateIsIndependentReadOnlyRepeatableRequiresNew`: existing templates unchanged; reuse exact manager/DSL/source.
- `ownerReadersUseOnlyTheQualifiedCustomerDsl`: no new transaction manager/pool/Primary/unqualified resource.
- `detailNeverResolvesCursorKeyMediaOrSharingResources`: success and404 without evidence key, no media/command resources.
- `missingCredentialsReadersAndCursorKeysFailClosedWithoutFallback`: sanitized lazy503 and unavailable provider behavior, no verifier bypass.

### 9.5 CustomerEvidenceBoundaryTests — 4 (G1/G3/G5/G6)

- `namedOwnerInterfacesAndTenCustomerDependenciesAreExact`: four Subject signatures/five Evidence signatures, plain records/IDs/sets only, Modulith verify/no cycles/internal edges.
- `customerCompositionAndOwnerSqlRespectMinimumReadBoundaries`: no owner SQL/imports in Customer; owner readers no foreign/internal SQL, SELECT *, writes, locks, transaction starts or media/provider calls; exact DTO components.
- `v017AddsOnlyNineteenColumnsAndPa25Uses288AndSeventeen`: independently literal exact old50/final69/delta19 and current PA25 history/checksum shape, no prohibited statements/roles/grant options.
- `openApiAndV001ThroughV016RemainFrozenWithoutContractExpansion`: existing normalized canonical hashes plus V016 normalized hash; deployment/base comparison additionally verifies raw bytes, not merely normalized equality; rejection canary for changed content.

### 9.6 CustomerEvidenceIT — 24 (G1–G6; PostgreSQL 16)

- `postgres16LatestChainAndVerifiedCustomerRoutesAreWired`: exact17 history, customer login, actual HTTP.
- `personalListAndDetailAreSubjectOwnerScoped`: two accounts, multiple subjects, mixed ownership.
- `organizationListAndDetailUseSelectedOrganizationNotCreator`: cross purchaser/creator/current account, selected org only.
- `allFourActiveOrganizationRolesCanRead`: all four roles, no role selector authority.
- `inactiveMissingAccountsOrganizationsAndMembershipsAreForbidden`: 403, lifecycle before existence, rejected roles.
- `missingUnownedForeignAndOtherWorkspaceDetailsAreUniformNotFound`: same five-key404/headers; no risk/artifact lookup.
- `listExcludesInaccessibleSubjectsBeforeSeekAndLookahead`: foreign/unowned rows cannot shorten/falsify pages or hasMore.
- `allFourEvidenceStatesAreVisibleWithoutArtifactEligibilityGate`: list/detail each state, zero artifacts accepted.
- `capturedAtComesOnlyFromImmutableCaptureScalar`: snapshot/live identity/capture-field canaries; evidence fields vs snapshot conflicting values.
- `summaryAndDetailHaveExactDistinctSchemaKeySets`: page3, summary8, detail7/8, artifact4/5, no null optional keys.
- `artifactRolesAndArtifactsUseRankHashSizeMimeOrder`: ORIGINAL first despite lexical order; tied hashes/size/MIME and duplicate safe DERIVED facts.
- `durationUsesOnlyUniqueMatchingValidatedSourceRecording`: keep this exact identity; seed a VALIDATED source recording with validated_duration=37 and two legal artifacts for the same evidence/media, one ORIGINAL and one DERIVED. Assert exactly two rank-ordered artifacts with identical hash/size/MIME, ORIGINAL `validatedDurationSeconds=37`, DERIVED key absent (not null), and summary `artifactRoles=[ORIGINAL,DERIVED]`. Also cover unrelated same-media recordings, absent/nonvalidated/misbound source, DERIVED-only shared media, and no fan-out. Implement these cases inside this method, no 77th test.
- `openRiskOnAnySubjectBindingIsForbidden`: ACTIVE/DISABLED/RETIRED/noncaptured bindings; clearing risk allows next request.
- `listOffPageRiskBlocksButForeignAndNoEvidenceRiskDoNot`: §5.1 unpaginated risk boundary explicit, cursor/limit variations.
- `absentBindingsAndClearedOrAbsentRiskDoNotInventRestrictions`: no binding/real-name/activation gate, owned data still visible.
- `defaultBoundsEmptySingleAndTailPagesAreExact`: 20/1/100, empty set/no roles query, short/tail pages and maximum lookahead.
- `descendingCreatedAtAndUuidKeysetSurvivesTiesAndNewInserts`: high-bit UUID boundary, page walk, ahead/behind inserts, no duplication.
- `tamperedCrossAccountWorkspaceAndEndpointCursorsNeverWidenScope`: same key across fixture codecs, full HTTP400.
- `uuidRouteMethodAndNonAuthoritativeInputPrecedenceIsExact`: malformed422/non-GET404/extra routes404/body ignored, dependency ordering.
- `unrepresentableCaptureAndArtifactFactsFailClosedWithoutLeakage`: legal persisted bad capture/nonstring/null and oversized MIME;404 detail/500 list as applicable; no fallback snapshot.
- `detailRereadsCurrentStateOnEveryRequest`: PENDING->terminal transitions via fixture revision protocol, current solidifiedAt, no cached share copy.
- `composedReadsShareOneSnapshotAndObserveLaterRevocationNextRequest`: deterministic two-connection barriers after lifecycle/ownership; concurrent risk/status/artifact insertion observed together only next request; no linearizable claim.
- `readsLeaveAllBusinessRowsAndEventsByteEquivalent`: success, empty,404,403,422/400 and forced DB failure; all-module row snapshots, evidence_event/sharing/outbox/audit untouched.
- `roleReadOnlyIsolationRequiresNewQueryCountsAndPoolCleanupAreExact`: same backend PID/current_user/isolation/readOnly across owner calls, suspended ambient transaction restored, 1 vs100 rows constant queries, actual write denied, connection restored after success/throw/completion failure.

Fixture composes `CustomerBusinessNumbersFixture`, uses its verified-session test adapter, isolated pg16/admin seeding and single-connection customer pool. Setup order is `base.start(null)` (latest V017 plus PA25 apply/check and login provisioning), then `base.web()` with both explicit cursor properties installed in its initializer before `.run()`, then seed/reset the case and issue requests. The evidence key source is exactly `Base64.getEncoder().encodeToString(new byte[32])`, canonical base64 `AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=` (32 zero bytes), matching the existing business-number test-key source but bound independently to `onecall.customer.evidence.cursor-hmac-key` by the one additive §8.3 property entry. Do not reuse `base.web()` without that entry, set the property after context creation, use a process-global system-property workaround or modify unrelated fixture helpers. `CustomerEvidenceCursorProperties` binds prefix `onecall.customer.evidence`; §6.3 enables it and wires the lazy `customerEvidenceCursorCodec(CustomerEvidenceCursorProperties)` bean. The controller's qualified `ObjectProvider<CustomerEvidenceCursorCodec>` resolves it only on list after structural validation; detail never resolves it. Missing/invalid evidence key remains a sanitized list503, tested without weakening the fail-closed codec or adding production defaults. For the actual ambient-transaction suspension probe only, temporarily allow two connections in that same existing pool, so REQUIRES_NEW can acquire a different connection without deadlocking behind the suspended outer connection; restore the pool limit afterward. This is not a second pool or production resource change. Copy the minimal evidence-record/event reciprocal creation seed from existing fixtures using valid V008 constraints; use admin transaction for paired creation, never disable triggers/constraints. Evidence event insertion is fixture setup, not production read behavior. Do not mutate immutable fields to produce impossible data; legal weak JSON/MIME constraints suffice, and service unit tests cover otherwise unreachable corruption.

### 9.7 CustomerEvidenceReadPrivilegeIT — 10 (G5/G6; PostgreSQL 16)

- `v016DeniesEveryNineteenMissingColumnCapabilityWith42501`: each new column independently denied, retained approved reads still succeed at269.
- `v017FreshAndUpgradeHaveExactSeventeenHistoryAndFrozenPredecessors`: target016 upgrade1, fresh17, repeat migrate0, exact predecessor bytes/checksums and actual final V017 checksum.
- `v017AddsExactlyNineteenOwnerIssuedTuplesAndMatchesIndependent288Oracle`: difference19, customer53/evidence16/others219, exact grantor/no options.
- `customerRoleReadsOnlyApprovedFactsAndDeniesAllSensitiveCapabilitiesWith42501`: all new SELECTs work; tableSELECT/events/snapshot/objects/declared recording facts/write/routine/owner-role denials; acknowledge full JSON column is readable at raw-role boundary.
- `v017ReplayPreservesRolesCredentialsDefinitionsAclsAndBusinessRows`: exact snapshots before/after final-state replay, stable OIDs/verifiers/graph.
- `v017RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically`: each of19 partial additions, omitted tuple, extra column/table grant, wrong owner, grant option; unchanged catalog after rejection.
- `v017RejectsAuthorityHistoryProvenanceAndCatalogDriftAtomically`: thirteen categories, role/settings/topology/ownership/PUBLIC/default/database/dependency cases and wrong predecessor history; no secret error messages.
- `pa25RequiresExactV017HistoryAndIndependent288OracleWithoutRepair`: V016-only fails, every new tuple removal/history-field mutation fails both modes without repair; final healthy apply/check idempotent.
- `sixLoginsSevenEffectiveRolesAndFourMembershipEdgesRemainExact`: direct login denials, role-switch reachability/effective grant matrix, no inheritance/admin/set drift.
- `realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure`: actual qualified pool, read-only role, permission failure/rollback and next borrower restoration; no lasting role elevation.

### 9.8 Exact cumulative inventory and CI edits

| Suite | Current baseline classes / methods | Additions | Required final classes / methods |
| --- | --- | --- | --- |
| Surefire | 44 / 375 | 5 / 42 (12+12+8+6+4) | **49 / 417** |
| Failsafe | 19 / 302 | 2 / 34 (24+10) | **21 / 336** |
| Total | 63 / 677 | 7 / 76 | **70 / 753** |
| Annotation-line scan | 678 | 76 | **754**, retaining exactly one historical comment |
| POM Failsafe includes | 19 | 2 | **21**, exact class identity set |

Baseline source methods and current CI maps/POM includes were independently compared in memory again during fix1 and agree (44/375,19/302,19 includes; annotation lines678). The 23 existing test/fixture edits add zero identities; §9.6 shared-media duration coverage extends an existing proposed identity. Additions remain76, so source=CI=XML must close at49/417 +21/336 =70/753 and754 annotation lines, POM21. No reports are present, so XML equivalence is an implementation validation requirement, not claimed runtime evidence.

CI must register exact literal `pa29_unit_additions` and `pa29_container_additions` from §§9.1–9.7, merge into unit_expected/container_expected without duplicates, and assert addition sizes `[12,12,8,6,4]`/`[24,10]`. Early Surefire total375->417, final exact_inventory unit417/container336, maps49/21, source methods `[417,336]`, total753, lines754. Require exact report file/class/case identity sets, duplicate rejection and zero skipped/failures/errors. Fixtures must not become suites.

Preserve all historical identities and arithmetic: original112/178=290; PA19-start128/192=320; PA20-start162/198=360; PA21-start196/204=400; PA22-start231/214=445; PA23-start266/214=480; PA25-start309/214=523; PA26-start309/234=543; PA27-start309/246=555; PA28-start341/272=613; new **PA29-start375/302=677**. Historical line scans291,321,361,401,446,481,524,544,556,614,678 remain. Add pa29 names to **every** historical union/subtraction and line-scan removal expression; otherwise old counts silently drift. Add pa29_start_totals and pa29_start_line_scan_total (678). Keep the one historical `PostgresContainerIT.java` comment hit byte-identical. Do not replace exact maps with minimum counts or derive expected method names from discovered source.

## 10. One implementation slice and stop condition

One slice **S1 — authenticated customer evidence metadata composition** owns all code/SQL/tests/docs above. Work order inside that slice is dependency ordering, not extra Gateflow slices:

1. Seal frozen-byte baselines and current exact method/ACL/history identities; write the independent delta tests.
2. Add owner interfaces/adapters and nineteen-column V017/PA25 proof, preserving historical-target tests.
3. Add composed service/transaction, cursor, DTO/controller and canonical filter gate together.
4. Add all literal tests/fixtures, CI/POM exact inventories and current-boundary docs.
5. Run focused then full validation; record actual evidence and residuals; stop.

This is not over-sliced: separate privilege-only, cursor-only, controller-only or one-endpoint slices would not prove the two-operation access/projection/snapshot goal and would duplicate inventory/history work. It is not over-designed: one service/template, one owner Evidence reader and two Subject batch methods, one codec, explicit DTOs and only necessary column grants; no generic framework/resources/locks/cache or future filter. Existing owner counts and sharing/media behaviors are untouched. G1–G6.

Stop when all 76 new identities and 677 preserved identities pass, 19-tuple delta/history17/oracle288 match independently, all frozen raw bytes remain identical, exact report/POM inventories close at70/753/21, current docs/report agree, and the allowlist contains no extra changes. If access semantics, JSON-path security, owner boundary, contract, grants or test inventories cannot be satisfied within this plan, stop with the precise counterexample and request a bounded amendment; never compensate by contract/migration weakening. This gate stops earlier: after writing and statically validating only this plan and its one fix1 artifact.

## 11. Validation commands and expected assertions

### 11.1 Current plan-only validation (no generated files)

- Verify plan/fix artifact are UTF-8, end in newline, have no trailing whitespace/NUL, contain all required sections/closures; plan retains all76 unique proposed method names, 9+5 production paths, 23 exact existing test/fixture edits, 53 S1 candidates and exact inventory/ACL arithmetic. Check every existing method identity against CI and preserve the new method list byte-for-name against the pre-fix plan.
- Parse current CI Python literal maps and source method identities in memory; parse POM XML includes; expected44/375,19/302,19 includes. Do not fabricate reports or execute Maven in this gate.
- Fix1 scope baseline: hash every existing workspace file before/after, excluding only the two authorized plan/fix paths: **1,825 entries**, manifest SHA256 **`c6391b728ea05078820cf1fe26b8b9936ada72ae98f6bb1964914956964a277f`** using sorted `path + NUL + raw-byte SHA256` entries joined by newline (exclude `.git` path components). Require identical paths and raw hashes, with no additional file outside the two allowed paths. This proves no source, test, Control, review or private input mutation independently of pre-existing Git dirt. Private/generated content is not edited or reported.
- Fix2 independently captures the current scope baseline excluding only this plan and fix-2, not fix-1: **1,831 entries**, manifest SHA256 **`af07120b8f4c537ec36de6b5fca34ebe509d9dfce158f7062c6625d8a45165dd`**, with the same algorithm and exact before/after path/hash equality requirement. The earlier fix1 manifest is historical, not the current entry count. Verify the three newly identified literal surfaces and actual dependency-set assertion against source; preserve all76 identities/order and all existing method inventories.
- `sha256sum docs/gateflow/pa29-customer-evidence-read-foundation-plan.md`; report actual digest after final write. No build/test/format command that writes target/reports/cache is authorized now.

### 11.2 Later implementation validation

Actual managed environment: WSL Bash at the user-specified `/mnt/c/Users/Administrator/.codex/worktrees/pa29-next-work-unit/codex-call`; `.git` points to Windows `D:/codex-call/.git/worktrees/codex-call4`. Native Linux `git` cannot resolve that Windows path; **`git.exe` is available and works**, reporting HEAD `73813534` and parent `ef9d0823`. No Git repair is needed or authorized. Initial status already has modified Control and untracked plan/reviews/private prompts; those are Controller inputs, not S1 changes. Repository blobs are LF while this managed checkout has CRLF: compare canonical SHA256 against both accepted Git baselines and independently require unchanged raw checkout SHA256 before/after. Never require raw checkout bytes to equal LF blob bytes. Use Windows Git here (ordinary `git` in a properly mapped independent Linux clone); read-only commands do not authorize pointer changes. Audit both main and goal-confirmation baselines; classify Control/goal bookkeeping separately and compare implementation candidates against the exact53 paths, including new/untracked files via status, not diff alone. Diff names are diagnostic, not a raw frozen-byte gate; classify EOL-only frozen hits through the exact hash/inventory/status checks below:

```sh
git.exe status --short
git.exe rev-parse HEAD
git.exe log -2 --format='%H %s'
git.exe diff --check
git.exe diff --name-only ef9d0823975ef66d1df3b70e921709d8562b4dcb --
git.exe diff --name-only 73813534bb3a8ec8f3f3c38db6c16e0d12109b5c --
```

Run this frozen-path gate from the repository root **before implementation** in `capture` mode, and run the identical here-document afterward with only `PA29_FROZEN_MODE=verify` changed. Keep the snapshot outside the repository, never recapture after edits, and preserve it as execution evidence. No source/helper file is created. In this managed checkout the captured raw hashes must also match all18 entries in §11.3; an independent LF checkout captures its own raw bytes instead. Canonicalization is only CRLF-to-LF, not general whitespace ignoring. All ten existing `api/openapi` files (two specifications plus eight tooling/configuration files) and sixteen predecessor migration paths are the exact26 frozen-path inventory, not wildcard permission; after implementation only the named V017 may be added to the migration directory. Enumerate tracked and nonignored untracked paths, so ignored dependency/build outputs from later CI are not mistaken for frozen inputs. Status entries for frozen paths must be identical to the validated initial status (including any clone-specific EOL-only entries), and the only additional status in these directories must be the untracked V017. No rename, copy, deletion, staging or extra candidate path is accepted:

```sh
PA29_FROZEN_MODE=capture PA29_FROZEN_SNAPSHOT=/tmp/pa29-frozen-before.json python3 - <<'PY'
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

git = shutil.which("git.exe") or shutil.which("git")
assert git, "Git unavailable"
mode = os.environ["PA29_FROZEN_MODE"]
assert mode in ("capture", "verify")
snapshot = Path(os.environ["PA29_FROZEN_SNAPSHOT"])
assert not snapshot.resolve().is_relative_to(Path.cwd().resolve())
bases = ("ef9d0823975ef66d1df3b70e921709d8562b4dcb", "73813534bb3a8ec8f3f3c38db6c16e0d12109b5c")
api = {"api/openapi/" + name for name in (
    "check-doc-map.mjs", "client-compat.ts", "contract-rules.mjs", "media-gateway-v1.openapi.yaml",
    "media-gateway.redocly.yaml", "onecall-v1.openapi.yaml", "package-lock.json", "package.json",
    "redocly.yaml", "tsconfig.client-compat.json")}
migrations = "backend/src/main/resources/db/migration"
successor = migrations + "/V017__customer_evidence_read_privileges.sql"
def output(*args):
    return subprocess.check_output([git, *args])
def sha(data):
    return hashlib.sha256(data).hexdigest()
def canonical(data):
    return data.replace(b"\r\n", b"\n")
frozen = None
canonical_hashes = {}
for base in bases:
    paths = set(output("ls-tree", "-r", "--name-only", "-z", base, "--", "api/openapi", migrations).decode().rstrip("\0").split("\0"))
    predecessors = paths - api
    assert paths & api == api and len(predecessors) == 16
    assert all(re.fullmatch(re.escape(migrations) + r"/V\d{3}__[^/]+\.sql", name) for name in predecessors)
    assert sorted(Path(name).name[1:4] for name in predecessors) == [f"{version:03}" for version in range(1, 17)]
    assert frozen is None or paths == frozen
    frozen = paths
    for name in sorted(frozen):
        digest = sha(canonical(output("show", base + ":" + name)))
        assert name not in canonical_hashes or canonical_hashes[name] == digest
        canonical_hashes[name] = digest
        assert sha(canonical(Path(name).read_bytes())) == digest, name
disk_paths = set(output("ls-files", "--cached", "--others", "--exclude-standard", "-z", "--", "api/openapi", migrations).decode().rstrip("\0").split("\0"))
assert disk_paths == frozen | ({successor} if mode == "verify" else set())
assert all(Path(name).is_file() for name in disk_paths)
entries = output("status", "--porcelain=v1", "-z", "--untracked-files=all", "--", "api/openapi", migrations).split(b"\0")
statuses = {}
for entry in filter(None, entries):
    state, name = entry[:2].decode(), entry[3:].decode()
    assert "R" not in state and "C" not in state and name in disk_paths
    assert state in (" M", "??"), (name, state)
    assert name in frozen or (mode == "verify" and name == successor and state == "??")
    statuses[name] = state
if mode == "verify":
    assert statuses.get(successor) == "??", "V017 must be the sole new untracked migration"
current = {"raw": {name: sha(Path(name).read_bytes()) for name in sorted(frozen)}, "canonical": canonical_hashes,
           "paths": sorted(frozen), "status": {name: state for name, state in statuses.items() if name in frozen}}
if mode == "capture":
    with snapshot.open("x", encoding="utf-8") as stream:
        json.dump(current, stream, sort_keys=True)
else:
    assert current == json.loads(snapshot.read_text(encoding="utf-8")), "Frozen raw/canonical/path/status drift"
print(mode, "PASS: exact26 frozen paths, canonical/base SHA256, raw SHA256 and status inventory")
PY
```

Continue with the existing runtime commands only during the later authorized implementation gate:

```sh
cd backend
grep -Fx '21.0.13+9' .java-version
grep -Fx 'SEMANTIC_VERSION="21.0.13+9"' "$JAVA_HOME/release"
grep -Fx 'IMPLEMENTOR="Eclipse Adoptium"' "$JAVA_HOME/release"
./mvnw -B -ntp -v
docker info
./mvnw -B -ntp -Dtest=CustomerEvidenceWebTests,CustomerEvidenceQueryServiceTests,CustomerEvidenceCursorCodecTests,CustomerEvidenceConfigurationTests,CustomerEvidenceBoundaryTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerEvidenceIT,CustomerEvidenceReadPrivilegeIT verify
./mvnw -B -ntp -Dtest=ModuleBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests,CustomerWebConfigurationTests,CustomerBusinessNumberQueryServiceTests,CustomerApiBoundaryTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT,SharingQueryServiceIT,CustomerMeIT,CustomerSharesIT,SharingRoleLeastPrivilegeIT,PublicSharesIT,PublicShareAdmissionIT,SharingAdmissionRoleLeastPrivilegeIT,EvidenceMediaReadIT,EvidenceMediaReadRoleLeastPrivilegeIT,MediaPlaybackIT,MediaGatewayTransportIT,CustomerShareCommandsIT,ProductionDatabaseIdentityProvisioningIT,DatabaseRuntimeAclRbacHardeningIT,CustomerBusinessNumbersIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectsIT,CustomerSubjectDetailReadPrivilegeIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

Expected toolchain: pinned Temurin21.0.13+9, Maven3.9.9, Docker/PostgreSQL16 available. New-feature focused unit run42 methods; new-feature focused IT34 methods (Surefire also runs normally in verify). Existing adjusted unit closure: seven suites/42 methods (9+4+4+8+6+8+3), existing Failsafe regression command: all19 baseline suites/302 methods, including unchanged dependent suites. Full clean run49/417 +21/336 =70/753, zero skips/errors/failures. Do not present focused report counts as full-run counts; final full clean verify regenerates authoritative XML. Run the **complete updated** `assert container smoke actually ran` Python here-document from `.github/workflows/ci.yml` directly in backend via stdin, not a checked-in helper; require its exact inventory pass. Validate class names, report filenames, suite totals, every testcase identity, zero duplicates/skips and exact POM includes.

Run all other existing CI commands/jobs exactly as currently declared, including default/opt-in web jar smokes and API/protocol/security/deployment/acceptance static gates. Full CI, not just Maven, is a success prerequisite. No dependency/package upgrade or generated client commit is implied. If runtime tools/network/Docker are unavailable, record NOT EXECUTED with reason and owner; do not claim slice completion.

PostgreSQL assertions: clean16 upgrade executes exactly1 migration; fresh17, repeat0; Flyway validates exact17 history; independent ACL288/customer53/evidence16/others219; each19 omitted grant gives42501 at16; all sensitive/read-write/role-switch denials remain42501; partial/widened/wrong owner/grant-option/catalog/history contamination rejects with sanitized category and rollback. Current PA25 apply/check run **only in isolated fixtures**, reject16-only, accept17, repeat without mutation. Six logins/seven effective roles/four edges unchanged.

Runtime assertions: no SQL outside the one composed snapshot; Customer role/current_user/isolation/readOnly and same PID at every owner read; deterministic concurrent barrier observations; no committed business/event/audit/outbox change; all failure/empty/not-found paths release transaction and reset pooled role; invalid HTTP/cursor requests do not instantiate pools. Logs/responses contain no capture-JSON, snapshot, private storage canaries, credentials, keys, raw bearers or share data.

### 11.3 Raw-byte frozen manifest for this checkout

These are raw SHA256 values (this checkout has CRLF inputs). Existing Java normalized hashes remain unchanged for cross-platform source tests; they are not sufficient evidence of this task's byte-identical requirement. The additional V016 LF-normalized SHA256 for the new cross-platform boundary test is `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f`. Canonical OpenAPI SHA256 values against both Git baselines are `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` (onecall) and `7f169b083125b890129e5b640d904034d49bd64ad063300c1459cde596213b65` (media gateway). §11.2 computes/checks every predecessor canonical digest directly from both base blobs and independently seals/checks the implementation host's raw bytes; line-ending conversion is not permission to change a frozen file during implementation.

| Frozen input | Raw SHA256 |
| --- | --- |
| api/openapi/onecall-v1.openapi.yaml | 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68 |
| api/openapi/media-gateway-v1.openapi.yaml | 26434f8bd231fb9ea45724b5887b653e8dc233c42d8b9319f8257f951c16a789 |
| V001 | 96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6 |
| V002 | 4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505 |
| V003 | 4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7 |
| V004 | 8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030 |
| V005 | 6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065 |
| V006 | 7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d |
| V007 | bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81 |
| V008 | 32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde |
| V009 | 3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1 |
| V010 | d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559 |
| V011 | ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e |
| V012 | ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6 |
| V013 | 1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e |
| V014 | bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8 |
| V015 | c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc |
| V016 | a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c |

## 12. Risks, residual owners and completion report

| Risk / open item | Decision and owner |
| --- | --- |
| List risk policy across page boundaries | Controller confirmed whole-list403 for all accessible evidence subjects (§5.1); no page-local risk/exclusion discretion. Backend/Product/Security own any future policy change |
| JSON column-grant sensitivity | Controller accepted trusted-application Security/Database residual; only scalar capturedAt extracted, no JSON emitted/logged, no JSON-path/RLS claim. A stricter future raw-role requirement requires new authority |
| Large subject-set materialization/binds | Bounded query count but tenant-sized memory; Backend/Database own capacity evidence. No invented cache/read model/index/SLO |
| Optional duration completeness | Only ORIGINAL plus unique validated source/media match is authoritative; DERIVED always omits duration, even on shared media. Evidence owner owns any future richer recording association contract |
| Legal persisted malformed capture or oversized MIME | Detail404/list500 without data leak; Evidence/data-quality owner repairs facts through separately approved process, not this read |
| Revocation/risk/status after snapshot starts | REPEATABLE_READ is consistent, not linearizable authorization; later committed changes govern next request. Security/Product own any stricter revocation requirement |
| V017 checksum | Implementation must compute final actual checksum and seal exact PA25/test literal; Database/Backend owner, no plan-time fabricated value |
| Historical tests vs latest provisioning | Explicit target pinning and separate current helpers; CI/Database own failure if a historical assumption was missed; amend allowlist rather than weaken assertions |
| Managed Git and pre-existing dirt | Windows git.exe works at73813534 over main ef9d0823; Linux git alone cannot resolve Windows pointer. Controller-owned Control/untracked inputs excluded from S1; no repair/fetch/stage/commit, remote state not freshly verified |
| Execution environment | No Maven/Docker/XML run in plan gate; implementer/CI owns Java21/Docker/PostgreSQL16 and full CI evidence |
| Production prerequisites | Platform/Database/Security/Operations own production credentials, TLS/pg_hba, migrator custody, deployment/rotation/pool drain/session invalidation and Q17/Q23; NOT EXECUTED, readiness NOT CLAIMED |

No unresolved contract change or policy question remains after Controller adjudication. Whole-list risk403, artifactRoles always present/empty[], and trusted-application JSONB residual are binding; a future request to reverse them returns to bounded plan/goal confirmation, never implicit implementation discretion. The V017 checksum is deliberately deferred until implementation, not an unanswered policy question. No media/playback/share work or lifecycle mutation is a residual deliverable of PA29.

Later implementation completion report must contain:

1. Work unit, accepted goal/plan, S1 outcome and exact changed paths against §8; no drift statement mapped to G1–G6.
2. Exact list/detail success keys, HTTP/error precedence/access/risk/status/artifact/cursor evidence and negative media/share/identity/snapshot/event leakage results.
3. Actual V017 byte hash/Flyway checksum, full exact nineteen delta tuples, old269/new288/customer53/evidence16/others219, history17 and preserved login/role/edge/invariant proof.
4. Commands/toolchain/image, exit codes, source/CI/POM/XML identities and report locations/hashes; focused42/34 vs final417/336/753 and line754/POM21; zero skips/failures/errors. Separate actual executions from static expectations.
5. Transaction/read-only/current role/same-connection/concurrency/query count/no-writes/pool cleanup evidence, including failures and missing cases.
6. Canonical frozen-input/base SHA256 equality plus raw before/after SHA256 equality and exact frozen-path/status inventory, working-tree allowlist/whitespace check, current-boundary docs synchronization and independent CI inventory outcome.
7. Classified risks/open items with named owners, NOT EXECUTED limitations and next Controller gate; no production-readiness assertion.
8. Explicit no stage/commit/push/PR/merge/approval/ready/review-request/external-comment/issue-mutation unless separately authorized later. This planning task performs none of those actions.

Plan-fix-author handoff is only: both artifact paths, final SHA256 values, plan-only static/scope validation results and Git/runtime limitations. Stop there; no re-review dispatch or gate advancement is authorized.
