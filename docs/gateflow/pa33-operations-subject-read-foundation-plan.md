# PA33 Operations subject read foundation — binding implementation plan

## 1. Authority, goal and gate

Task: `pa33-operations-subject-read-plan-gpt-20261114-01`; author date: 2026-11-14; gate: **plan, author complete only**. Required dispatch is Codex runner / GPT `gpt-6.1-sol` / high. This artifact does not attest to runner metadata that the author cannot independently inspect; Controller must verify that dispatch separately. No delegation, implementation, review, repository staging or remote action is authorized by this document.

Cumulative bounded correction: `pa33-operations-subject-read-plan-fix1-gpt-20261114-01`, 2026-11-14, **plan review → bounded fix, author complete only**. Binding Controller dispositions in the PA33 checkpoint and the complete independent reviews `docs/reviews/plan-review-20261114-162057.md` (A) and `docs/reviews/plan-review-20261114-164255.md` (B) are incorporated below as PA33-PLAN-REV-01..06. Historical companion artifact: `docs/gateflow/pa33-operations-subject-read-foundation-plan-fix-1.md`, preserved unchanged. That documentation-only correction authorized only its two plan artifacts and stopped for fresh independent A/B Planreview; it was neither implementation authorization nor a plan-pass claim.

Implementation-discovered bounded correction: `pa33-operations-subject-read-plan-correction1-gpt-20261114-01`, 2026-11-14, under binding Controller decision **PA33-PLAN-CORRECTION-01**, inside approved S1 at HEAD `185a91f3`. Full clean exposed the retained `CustomerEvidenceBoundaryTests.namedOwnerInterfacesAndTenCustomerDependenciesAreExact` oracle's obsolete subject_number/evidence dependency sets. Section 8 now includes that existing test path and the corrected 37-path manifest. Current writes are limited to this cumulative plan, new `docs/gateflow/pa33-operations-subject-read-foundation-plan-fix-2.md`, and `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`; only the two exact dependency assertions change. No new behavior or test identity is authorized. Stop for resumed full validation after the specified focused tests; no review or acceptance claim.

Read order began with `docs/gateflow/project-execution-control.md`, specifically its PA33 goal-confirmation checkpoint. The confirmed goal is exactly two metadata-only authenticated Operations reads: `listOperationsSubjects` and `getOperationsSubject`. Motivation: employees need authoritative, masked subject discovery and diagnosis without gaining customer identity, recording access or lifecycle mutation authority. Success signal: current ACTIVE ADMIN and OPS can obtain the exact frozen page/detail; all other authority, routes and unsafe fields fail closed; read transactions have no domain/audit/Outbox effects; real PostgreSQL16 tests and independent validation prove owner isolation and inventory equality.

Actual inspected HEAD is `6f0b7be863988cfba7f8816a5ea7c7b3b3462699` (PA33 goal-confirmation bookkeeping), whose parent is PA32 merge `699a18a6f218aaef3ab8c9ab164c1b4101c6fadf`. Do not describe the goal-confirmation commit as new product code. Initial untracked `.codex` and `.pa33-plan-prompt.md` are preserved/excluded. The native `.git` pointer names `D:/codex-call/.git/worktrees/codex-call17`; read-only Linux Git uses `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call17 --work-tree="$PWD"`. Do not repair or edit Git metadata.

Non-goals: other Operations endpoints, employee administration or issuance, IdP/Redis/MFA/Q1, production credentials or membership provisioning, UI, full-number/free-text search, playback/media/storage keys, customer policy changes, total/offset pagination, cross-request snapshots, schema/index/grant changes, V018, generic idempotency/cursor/authorization frameworks, writes, locks for business serialization, retry workflows, audit facts, Outbox, trial issuance, consent/grant/balance mutation or production readiness. Default and web-only boot must remain dependency-free and fail closed on authenticated requests lacking dependencies.

**Frozen-contract reconciliation:** The original seven-vs-eight SubjectSummary discrepancy is Controller-accepted: frozen OpenAPI lines 4735–4766 declare **eight** allowed properties, six required plus two optional, as the current Control confirms. Implement those eight, never invent a seven-field projection or make the optionals required; no frozen file edit is proposed. Control's “wrong-filter” under CURSOR_INVALID is read with its immediately following explicit valid-cursor/changed-filter rule: authenticated valid ciphertext + different normalized digest is CURSOR_FILTER_MISMATCH; unauthenticated/corrupt ciphertext is CURSOR_INVALID.

## 2. Direct evidence and design alignment

The following are code/contract evidence, not conclusions borrowed from a historical plan:

| Evidence inspected | Binding consequence |
| --- | --- |
| OpenAPI `GET` paths at 1030 and 1145; schemas at 4471 and 4735–4880; CursorParam/LimitParam at 4022 | Exact employee security, six filters, descending sort, eight summary properties, nullable page cursor, optional activation/trial and the closed safe detail shape below. |
| docs/12 §§6–8, 10–11, 14; docs/15 §§3.1, 4 and pagination rules | Local transactions, published read/inbound interfaces, independent employee identity; no foreign owner SQL merely for page convenience; frozen errors and stable keyset pagination. |
| docs/14 §§2–4, 7.1/7.3, 8–10 and role/lock contracts; V002 employee, V003 subject/binding/claim/risk | Employee role/status is a current DB fact; subject creation time is immutable; no customer owner requirement for these employee reads. Latest claim uses risk_checked_at, not a nonexistent claim created_at. Bindings have no created_at. |
| V006 trial_grant, entitlement_grant, entitlement_balance; V007 service_activation | Unique permanent FIRST-TRIAL per subject, immutable original grant, one balance per grant, one activation per subject; absence can be projected without creating rows. |
| V004 customer_order; V008 evidence_record; V014–V017 ACL assertions | Counts can be performed by their existing owners. Customer-role column grants are not general Operations grants. No foreign table privilege may be inferred from FK or schema ownership. |
| `operations_iam/internal/security/EmployeeBearerAuthenticator.java` and its four neighboring seam/value/error types | Existing verifier is opaque, validates exact employee audience independently, streams at most two providers, sanitizes creation/provider errors, derives only trusted employeeId/audience. Reuse unchanged, including lifecycle Caller, rather than create an employee login implementation. |
| `operations_iam/internal/web/{OperationsBoundaryFilter,OperationsSubjectsController,OperationsProblemWriter,OperationsWebConfiguration}.java` | Current admission is only two POST commands, scoped servlet filter at highest precedence; current Problem writer lacks cursor codes. Keep command behavior and add a dedicated GET controller plus narrow writer overload. |
| `customer_iam/internal/web/CustomerBoundaryFilter.java` | Its early exact Operations POST pass-through must gain the exact same two GET admissions, before customer authentication. No other customer/public behavior changes. No separate public filter currently needs an Operations exception. MediaGatewayFilter is scoped to its internal gateway and does not admit these routes. |
| `activation_consent/internal/operations/OperationsLifecyclePersistenceConfiguration.java`; `operations_iam/internal/employeelock/DefaultEmployeeAdministratorLock.java`; `entitlement_metering/internal/activation/DefaultActivationTrialService.java` | Same-connection literal-role handoff/assert/restore is an existing pattern. Do not call command services or the employee lock collaborator for reads. Their write templates/locks are deliberately unsuitable. |
| `CustomerSubjectDetailQueryService`, `CustomerPersistenceConfiguration`, the three customer owner count/status readers | Existing read-only REQUIRES_NEW/REPEATABLE_READ composition demonstrates snapshot requirements. Existing customer order reader is workspace-filtered, so reusing it for global employee counts would be wrong. Existing customer readers do not perform the proposed owner-role handoff. |
| All 14 module package-info declarations and `ModuleBoundaryTests` | activation_consent already depends on operations_iam. Adding operations_iam → activation_consent creates a cycle. Use consumer-owned inbound ports implemented by owners; no reverse owner imports into Operations. |
| `backend/pom.xml`, `.github/workflows/ci.yml`, all annotated Java test identities, PA32/PA28 fixture and boundary/configuration test patterns | Reconciled baseline is 64/574 Surefire + 27/440 Failsafe, not an older plan's totals. Appendix A enumerates every retained method. |
| docs/17 §§10–12, 17–18 | Independent opaque employee session; fixed ADMIN/OPS; safe allow-listed metadata only. High-risk mutation and audit-read audit rules do not turn these masked subject reads into audit-producing operations. |

**No-overdesign judgment:** one read composition, one cursor codec and five minimal owner adapters are sufficient. The inbound ports are required by the already-existing dependency direction, not speculative microservices. No join across foreign base tables, new reporting schema, read-model sync, cache, search engine, SQL function, cursor table, generic role framework or new library is justified. Bounded candidate batching is the explicit cost of preserving owner privileges while filtering activation status; its fail-closed capacity limit is preferable to silently missing rows or inventing a grant/index. If that bounded capacity is unacceptable in validation or product adjudication, stop and obtain a separately authorized goal change, not a hidden migration.

**Binding capacity adjudication (accepted residual):** Controller accepts whole-response sanitized 503, rollback and no partial response when detail has >100 bindings or >100 claims, or activation filtering reaches 100 batches / 10,100 candidates without sufficient matching lookahead or a proven tail. Product/Backend/Database owns capacity/SLO follow-up. This is not arbitrary-history success, silent truncation, offset pagination, or permission for V018, an index, grant or OpenAPI change. If the frozen contract is later interpreted to require arbitrary-cardinality success, new user authorization is required before proceeding.

## 3. Wire behavior, admission and error order

### 3.1 Routes and authentication

Admit exactly `GET /api/v1/operations/subjects` and `GET /api/v1/operations/subjects/[^/;%]+`, plus the existing two exact POST lifecycle patterns. Raw requestURI must equal contextPath + servletPath; reject encoded spellings, any percent/semicolon, double slash, trailing slash, extra segments, HEAD/OPTIONS/PATCH/PUT/DELETE and wrong case as sanitized `404 NOT_FOUND` before authentication. A syntactically admitted single segment such as `bad` reaches canonical UUID validation after authentication/authority and returns 422. `/employees` and all other Operations routes stay concealed, even if a downstream mapping exists. No redirect, automatic OPTIONS/HEAD or HTML error route may escape the filter. The customer filter uses exactly the same local predicate before customer authentication, not a broad Operations-prefix bypass. Keep registration, REQUEST dispatcher, precedence, response buffering and error concealment intact.

Use the existing Authorization header parser and EmployeeSessionVerifier SPI unchanged: exactly one valid Bearer header; missing/malformed/duplicate/rejected/expired/revoked bearer or wrong audience → 401; zero, two, throwing/uncreatable verifier beans, null/malformed attestation or provider unavailable → 503. Expected and independently checked audience is exactly `onecall:employee-session:v1`. Customer tokens cannot authenticate employees. Only server attestation employee UUID and audience enter the existing OperationsRequestPrincipal/Caller. No header/query/body employee, role, account, organization, source-domain or visibility assertion grants authority. `source` is solely a subject creation-source filter.

Each read starts an authoritative transaction and reads `SELECT id,status,role,revision FROM operations_iam.employee_account WHERE id = ?` under operations_iam_role, with **no FOR UPDATE/advisory lock**. Null/bad trusted caller → 503 (wrong nonmatching audience → 401); absent employee → 401; valid DISABLED employee → 403; only current ACTIVE ADMIN or OPS allowed. Corrupt role/status/revision or mismatching returned identity → 503. The schema fixes roles to ADMIN|OPS; tests must nevertheless inject unknown roles to prove refusal. Authority is current at the request's database snapshot; later concurrent disable is visible to the next request. Unlike mutations, these reads do not promise serialization against employee disable.

For admitted GET, OperationsBoundaryFilter performs only route admission + existing bearer verification/principal setup; it does not parse, negotiate or reject Accept and never executes the existing POST Accept rejection. POST command Accept parsing/rejection and all other POST behavior remain unchanged. The new read controller has no Spring `produces` condition and does not return a negotiable MVC body/ResponseEntity for converter selection: copy all raw Accept header values into immutable RawListRequest/RawDetailRequest system accept data (separate from query keys), without parsing there; call the read service, serialize success to the existing Operations response buffer, then explicitly write `application/json` only after successful service/transaction cleanup. No Spring 406, HTML fallback or implicit media negotiation is allowed. Read failures use the read Code writer and `application/problem+json` irrespective of the rejected Accept.

### 3.2 Deterministic precedence

For admitted reads: (1) bearer verification; (2) select exactly one read-service bean, including safe lazy instantiation; (3) transaction/pool/role and trusted current employee authority; (4) request shape, duplicates/unknown keys and noncursor values; (5) acceptable JSON response media; (6) list cursor configuration and cursor authentication/digest; (7) required owner providers; (8) subject/list reads. Hidden routes are checked before all eight. Missing/ambiguous/uncreatable read-service or required owner dependency is 503, never fallback. Query validation is not allowed to bypass authentication/authority. Detail not-found occurs only after authority and input validation. An empty list is 200, not 404. Cursor configuration is required for every list including the first/empty page; detail never requires cursor keys. Resolve subject/activation ports for list; all five for detail. Required zero/two/creation-failure providers return 503 before any subject projection, including otherwise-empty results.

Frozen mappings: CURSOR_INVALID/CURSOR_FILTER_MISMATCH → 400; UNAUTHENTICATED → 401; FORBIDDEN → 403; detail NOT_FOUND → 404; VALIDATION_FAILED → 422; RATE_LIMITED → 429 only when an existing external admission limit rejects (no new limiter); SERVICE_UNAVAILABLE → 503; unexpected implementation bug → 500 INTERNAL. SQL/connection/transaction/role/timeout/bean failures map to sanitized 503 after cleanup, including 42501, 40001 and 40P01; no privilege repair or retry. Structurally impossible owner projection is 503. Known request errors retain their code only if cleanup succeeds; restoration failure overrides a would-be response with 503. Never return SQLSTATE, message, query string, cursor, bean names, employee facts or credentials.

Reuse the five-property Problem envelope `type,title,status,code,traceId` with `about:blank`, server UUID trace, no-store/no-cache/nosniff, WWW-Authenticate Bearer on 401 only, no Set-Cookie. Add a writer overload for read-service Code; do not expand lifecycle Code or alter command mappings. GET responses are serialized through the Operations buffer with explicit application/json; errors application/problem+json. Prevent Spring-generated 406/405/HTML responses. A supplied Accept that cannot accept application/json (including q=0 exclusion) is 422 after authority; absent or compatible wildcard/JSON Accept is accepted. Combine multiple Accept headers using standard media-range semantics; malformed media syntax is 422. GET bodies and Idempotency-Key/Content-Type are not command inputs and are ignored without being read or logged. No command header is required for either GET.

### 3.3 Exact parsing

Controller copies `getParameterMap()` values without collapsing duplicates; application parser runs only after authority. List key allow-list: `cursor,limit,subjectId,source,claimStatus,activationStatus,realNameStatus,riskState`. Unknown keys, empty noncursor values, key with zero values, or any repeated key (even identical values) → 422. Detail accepts no query keys. Duplicate handling and noncursor validation precede cursor parsing, so duplicate cursor → 422; a single empty/overlong/malformed cursor → 400 after valid configuration. Deterministic field order for validation is unknown/duplicates first, then subjectId, source, claimStatus, activationStatus, realNameStatus, riskState, limit, media, cursor. No reflection binder, trimming, case folding, comma splitting, numeric coercion or client identity binding.

`subjectId`: UUID.fromString plus exact `.toString().equals(input)`; canonical lowercase 36-character UUID only, both path/filter; reject abbreviated, uppercase, braced, padded, encoded/dotted values. Nil UUID is syntactically valid; no version restriction. Enums are exact: source IVR|MINIAPP|WEB|OPERATIONS; claimStatus ACCEPTED|REJECTED|CONFLICT; activationStatus INACTIVE|ACTIVE|SUSPENDED; realNameStatus UNVERIFIED|VERIFIED; riskState OPEN|CLEARED. Omitted limit=20; present limit must match `[0-9]{1,3}` then integer 1–100; leading zero spellings such as `020` normalize to 20; reject signs, whitespace, decimal/exponent, Unicode digits, zero/101/overflow. Limit is not in the filter digest, so changing page size is allowed. Unknown/free-text/full-number filters are rejected, not ignored or translated into search.

## 4. Opaque cursor and key custody

Use only JDK SHA-256, SecureRandom and AES/GCM/NoPadding, independent of all employee/customer/media keys. `OperationsSubjectCursorCodec` gets an immutable validated key-ring snapshot from OperationsSubjectReadProperties. Use an ordered list, never kid-derived property names: `onecall.operations-subject-read.cursor.active-key-id`, `onecall.operations-subject-read.cursor.keys[0].id`, `onecall.operations-subject-read.cursor.keys[0].material`, with contiguous indices 0 through N-1, N=1–3. Each material is canonical unpadded base64url encoding of exactly 32 independent random bytes. IDs are case-sensitive ASCII `[A-Za-z0-9_-]{1,32}`, treated as string values without trimming, case folding or relaxed-name conversion; unique IDs, exactly one matching active ID, remaining keys verify-only. Order conveys no fallback preference. Cursor's nested immutable KeyEntry record belongs inside the already-listed OperationsSubjectReadProperties, not a new top-level type/path. Reject holes, empty/missing/oversized/noncanonical material, duplicate IDs and identical bytes under different IDs as 503. Deployment supplies the complete indexed properties from an explicitly configured secret-mounted `.properties` file through existing Spring external configuration; no dynamic kid map, environment-variable key-name encoding, implicit file discovery or new secret reader. The mount/file and secret distribution are Platform/Security responsibilities, not repository assets. No generated fallback, hard-coded secret, application secret reuse, last-good fallback, KMS/SSM polling or new SDK. Never obtain keys from source/image, HTTP input, or command-line secret values. Lazy validation means malformed/missing keys affect list requests, not startup/default/detail. Properties, KeyEntry, key arrays, request records and failures have redacted toString; defensive copy all byte arrays; no logs of configuration paths or material.

Binary codec v1 is fixed, big-endian: header = version byte `1`, unsigned kid-length byte, ASCII kid (1–32 bytes); then 12 fresh random nonce bytes; then AES-GCM ciphertext of a 60-byte payload and 16-byte tag. Payload = signed epochSecond int64 + nano int32 (0–999999999, PostgreSQL microsecond precision required), UUID most/least-significant int64 pair, and 32 digest bytes. Anchor is exact DB created_at + subject id of **last returned row**, not lookahead or last scanned candidate. AAD = UTF-8 `onecall:operations-subject-list:cursor:v1` + zero byte + exact header bytes. Emit canonical unpadded base64url of the whole envelope; size is 122–163 characters (91–122 binary bytes), within frozen 1–512 bound. Header reveals only version/key identifier, not identity, offset, SQL key or plaintext time. Randomized encryption prevents enumerable identities. Reject noncanonical base64url, padding, invalid version/length/trailing bytes, unknown/retired kid, failed tag, invalid Instant/nanos/UUID structure as CURSOR_INVALID. UUID has no extra business version restriction. Never parse plaintext before successful tag verification; no cross-purpose decoder fallback.

Authenticated payload validation explicitly requires `0 <= nano < 1_000_000_000` **and `nano % 1000 == 0`** before any candidate/owner query. Otherwise return `CURSOR_INVALID`/400; never round/truncate a non-PostgreSQL-representable anchor. Authority still precedes cursor validation under §3.2. In `invalidNanosAndPlaintextShapesNeverReachQueries`, construct correctly authenticated payloads with nano=1, 999 and 999999999, assert CURSOR_INVALID and zero candidate/owner queries; cover negative/overflow nanos and valid 0/1000/999999000 boundaries within that same fixed method.

Digest input is UTF-8 of exactly seven LF-terminated lines: first `onecall:operations-subject-filters:v1`, then `subjectId=<value-or-~>`, `source=<value-or-~>`, `claimStatus=<value-or-~>`, `activationStatus=<value-or-~>`, `realNameStatus=<value-or-~>`, `riskState=<value-or-~>` in that order. Values are already canonical validated UUID/enums; `~` is the absence sentinel and cannot be an accepted value. SHA-256 produces raw 32 bytes; constant-time compare against authenticated payload. Equivalent parameter order/omissions yield identical digest; limit/cursor/Accept are excluded. Only an authenticated valid payload with unequal digest returns CURSOR_FILTER_MISMATCH. Invalid config precedes all cursor errors. No cursor expiry or snapshot claim is invented.

Rotation: provision complete new immutable ring across replicas with old key retained verify-only and new active; all newly issued cursors use active; retained keys decode old cursors without reissuing on decode. Restart/refresh is deployment-atomic per instance; old instances must have new verify key before new issuance. Removal immediately makes old cursors CURSOR_INVALID, never FILTER_MISMATCH; emergency compromise requires ring replacement and documented pagination restart. PA33 does not implement dynamic secret polling, fleet rollout, key generation or session issuance. Security/Platform owns secure generation, distribution, retention/retirement and deployment validation. Valid keys absent from a replica is operational failure risk, not permission to use a fallback.

## 5. Safe projections and cardinality

All response types are explicit nested immutable records on OperationsSubjectReadService, with per-property omission rules (never global NON_EMPTY). SubjectSummary has exactly the eight allowed names from §1: always subjectId/source/activationStatus/realNameStatus/riskState/createdAt; maskedNumber and claimStatus omitted, not null, when no corresponding binding/claim. SubjectPage always has items (possibly []), nextCursor (explicit JSON null at tail/empty), hasMore. `hasMore` iff a matching lookahead exists; return at most limit items. Empty/tail → hasMore=false and nextCursor=null. No total, offset, employee, owner identity or arbitrary map properties.

Detail has exactly **12** allowed properties: nine always present `subjectId,source,realNameStatus,riskState,bindings,claims,evidenceCount,orderCount,createdAt`, plus optional maskedNumber/activation/trial. Bindings and claims are [] when none. For missing activation omit activation entirely; do not invent revision or persist an INACTIVE row. For present activation include exactly status and revision>=1. List activationStatus defaults INACTIVE on no row, otherwise exact current stored status.

BindingSummary: bindingId, numberMasked, numberType, bindingStatus always; verifiedAt only when nonnull. Include all ACTIVE/DISABLED/RETIRED bindings subject to the bounded completeness rule below, order `binding.id ASC`. Display binding for top-level maskedNumber is first by `(status rank ACTIVE=0, DISABLED=1, RETIRED=2, id ASC)`; use the same rule for list/detail, independent of number type. No binding → omit maskedNumber. Read only id/subject_id/number_masked/number_type/status/verified_at. Masked strings must be 1–64 safe PHONE display characters (digits, `+ - ( ) *`, with at least one `*`); otherwise substitute fixed `[REDACTED:PHONE]`, never serialize an unmasked/ciphertext/object-key canary through a mislabeled display column. This is output minimization, not a new search or verification policy.

ClaimSummary: claimId/status/verificationMethod/riskCheckedAt always, verifiedAt only when nonnull. Ordering `risk_checked_at DESC,id DESC`. The latest claim used for list claimStatus/filter is the first row by exactly this order **before** applying the status filter: do not select latest matching historical claim. No claim → omit claimStatus and fail a supplied claimStatus predicate. Read no account_id, organization_id, request_id or verification_ref_ciphertext.

Risk is OPEN if any OPEN risk row exists on any binding for the subject, including disabled/retired bindings; otherwise CLEARED, including no bindings/risk rows. Never return risk records, risk free text or owner identities. Risk filters use this same existential truth; OPEN is not itself an Operations denial.

Trial is omitted only if no trial_grant exists. Otherwise require one consistent FIRST-TRIAL row and its unique TRIAL/RECORDING_SECONDS entitlement_grant and balance; validate subject/grant relationship, original granted units and expiry, positive grant and nonnegative counters with reserved+consumed<=granted. Missing/bad related facts → 503, not omission/repair. Require nonnull `trial_expires_at` and `grant_expires_at` to represent exactly the same stored Instant; mismatch fails closed with sanitized 503, rollback and no partial response. Never pick min/max, current policy or a replacement expiry. After equality validation, project `expiresAt=trial_expires_at` and use that same validated value for status. Exactly grantedSeconds=grant.granted_units; remainingSeconds=granted-reserved-consumed. Status precedence: if request DB transaction_timestamp()>=expiresAt or balance.status=EXPIRED, EXPIRED; else consumed=granted (and balance EXHAUSTED), EXHAUSTED; else GRANTED. Fully reserved but not consumed may have remainingSeconds=0 and status GRANTED. Validate balance status/counter consistency; never issue_first_trial, read live policy or sum paid grants. Capture DB time once for the request, not workstation time.

EvidenceCount is `count(subject_id)` of evidence.evidence_record for that subject, all evidence statuses; no recording/artifact join, media read or risk/status exclusion. OrderCount is `count(subject_id)` of catalog_order.customer_order for that subject, all statuses, all purchasers and personal/organization contexts; no workspace/account predicate and no payment-row multiplication. Both are nonnegative int64 JSON integers, including zero. These endpoints do not grant playback. createdAt and all timestamps are UTC RFC3339 with stored precision, never synthesized from another event.

**Bounded completeness, not silent truncation (Controller-accepted residual):** detail reads at most 101 bindings and 101 claims. Up to 100 each → return all in defined order; the 101st in either means sanitized 503 capacity failure for the entire response, rollback and no partial response. Do not return a partial history as complete, add undocumented truncation flags, or fetch an unbounded array. This is a fail-closed dependency-capacity ceiling within existing SERVICE_UNAVAILABLE, not a public pagination/schema change. Test 0/1/100/101 boundaries explicitly. Product/Backend/Database owns this accepted residual; a later interpretation requiring arbitrary-cardinality success requires new user authorization, not a silent cap, V018, index, grant or OpenAPI change. Count aggregates stay scalar and exact; PostgreSQL may scan matching rows, bounded by request/statement time budgets, not by pretending COUNT is constant-time.

## 6. Ownership, wiring and snapshot/role protocol

### 6.1 Types and dependency direction

Expose one named interface `operations-subject-read` in operations_iam/subjectread. OperationsSubjectReadService is the controller/application contract: `list(Caller,RawListRequest): SubjectPage`, `detail(Caller,RawDetailRequest): OperationsSubjectDetail`; Caller is the existing immutable lifecycle Caller. Nested Raw request types carry copied multi-value query/Accept/path data and redact toString. Code/Failure cover only §3's read codes. OperationsSubjectReadPorts publishes only five small inbound SPIs and safe records: SubjectReader.fetchCandidates(Filters,Optional<Anchor>,int), SubjectReader.detail(UUID,int); ActivationReader.find(Set<UUID>) returning a bounded status/revision map; TrialReader.find(UUID,Instant); EvidenceCounter.count(UUID); OrderCounter.count(UUID). SubjectReader's internal safe subject/detail records are not Jackson wire DTOs. No servlet or client caller data in owner SQL contracts. No generic repository, user-provided SQL, arbitrary role string or writable owner method.

DefaultOperationsSubjectReadService in Operations owns orchestration, employee-local authority SQL, snapshot, parser/cursor invocation, provider cardinality, safe projection validation, status joining, page derivation and response assembly. Each domain implements its inbound SPI under its own internal/operationsread package. OperationsReadContext is the single feature-specific published support type for same-resource assertion and five literal role scopes; its only scope roles are SUBJECT, ACTIVATION, TRIAL, EVIDENCE, ORDER. It does not expose raw SQL execution to the controller. Owners receive exactly `operationsSubjectReadDsl` and `operationsSubjectReadContext` by qualifier; use their scoped literal owner role and restore operations_iam_role. Never import Operations internal implementations from an owner.

Allowed dependency additions only: subject_number, entitlement_metering, evidence and catalog_order each gain `operations_iam::operations-subject-read`; activation_consent gains the same token alongside its existing command/employee-lock dependencies. operations_iam.allowedDependencies stays exactly `{operations_audit}`. No Operations → owner edge, no customer → Operations edge, no new module. ModuleBoundaryTests retains nine method identities; assert 14 modules, revised exact five maps and 12 discovered directed module edges (existing 8 plus four new owner→Operations edges; activation→Operations already exists). Consumer-owned interfaces are intentional dependency inversion, not a claim that Operations owns domain data.

Pin the five replacement EXPECTED_ALLOWED entries exactly (all other entries unchanged):

| Module | Exact allowedDependencies set after S1 |
| --- | --- |
| subject_number | `{operations_iam::operations-subject-read}` |
| activation_consent | `{subject_number, entitlement_metering::activation-trial, operations_iam::subject-lifecycle-command, operations_iam::employee-administrator-lock, operations_iam::operations-subject-read}` |
| entitlement_metering | `{catalog_order, subject_number, operations_iam::operations-subject-read}` |
| evidence | `{entitlement_metering, operations_iam::operations-subject-read}` |
| catalog_order | `{customer_iam, subject_number, operations_iam::operations-subject-read}` |

In retained `discoveredProductionEdgesStayInsideTheConfiguredMap`, keep the subset checks and **containsExactlyInAnyOrder**, not containsAll/set deduplication as a replacement. The exact 12-target multiset is `"sharing", "evidence", "subject_number", "activation_consent", "evidence", "catalog_order", "entitlement_metering", "operations_iam", "operations_iam", "operations_iam", "operations_iam", "operations_iam"` (operations_iam×5, evidence×2, each other target×1). Exact `.as(...)` message: `"exactly twelve production edges exist: customer query owners, sharing -> evidence, activation_consent -> entitlement_metering and operations_iam, and subject_number, entitlement_metering, evidence and catalog_order -> operations_iam"`. Retain all nine Appendix A ModuleBoundaryTests identities, the 14-module set and exact map equality; new activation named-interface allowance does not create another directed edge.

### 6.2 Profiles and providers

Dedicated properties namespace `onecall.operations-subject-read`: explicit `enabled=true`, datasource.url/username/password, and cursor inputs in §4. Production configuration and all owner adapters require profile persistence + enabled=true; adapters are @Lazy with exact bean names operationsSubjectReader/operationsActivationReader/operationsTrialReader/operationsEvidenceCounter/operationsOrderCounter. Every pool, DSL, manager, context and service bean is lazy; no default credentials, provider auto-repair, inheritance from customer/command pools or pool creation at boot. Property binding itself must permit absent values: no eager constructor/@Validated rejection. Validate datasource inputs only when creating the lazy pool; validate cursor inputs only on list codec use, never on detail graph creation. Web GET controller requires web + servlet, and selects one OperationsSubjectReadService by **type**, not an eager required constructor dependency or first-candidate fallback. Bean name is operationsSubjectReadService; duplicate instances of any name are detected via ObjectProvider.stream().limit(2), including instantiation exceptions. Apply the same cardinality rule to owner ports. Required command resources are untouched and neither command enablement nor cursor keys are required for detail.

Default/no-profile/web-only/persistence-disabled: no read pool/service/owner adapters; web endpoint with valid-looking bearer but no verifier returns 503; with synthetic valid verifier but no read service returns 503. Persistence-enabled without web supports direct service invocation but no controller. Web+persistence+enabled makes the graph available lazily. Missing/invalid credentials, connection failure or invalid keys must not crash otherwise inert boot or print their values; first affected authenticated read returns 503. Owner-port resolution may instantiate no network resource other than the already-bound read pool. Production EmployeeSessionVerifier implementations remain absent from packaged code; tests use SyntheticEmployeeSessions without shipping it.

### 6.3 Authoritative transaction and cleanup

One dedicated Hikari datasource (UTC init, max pool size 4, connectionTimeout 3000ms), DataSourceTransactionManager with rollbackOnCommitFailure=true and enforceReadOnly=true, TransactionTemplate REQUIRES_NEW + REPEATABLE_READ + readOnly=true + timeout 5 seconds; DSL uses POSTGRES, prepared indexed parameters, disabled execute logging and TransactionAwareDataSourceProxy over that exact datasource. Enforce database READ ONLY, not just a Java hint. No ambient customer/command transaction can be joined. Statement timeout 2000ms, lock_timeout 250ms and idle_in_transaction_session_timeout 5000ms are SET LOCAL feature settings, never ALTER ROLE/DATABASE; total request deadline also enforced by transaction timeout. Do not acquire write/row/advisory locks; ordinary PostgreSQL SELECT relation locks are transaction-scoped and must be released by commit/rollback.

At entry verify actual transaction active, resource bound to the exact manager datasource, physical connection read-only and isolation REPEATABLE_READ; capture session_user/backend_pid and assert entry current_user=session_user. SET LOCAL ROLE operations_iam_role; SELECT current_user must equal it. Read authority and transaction_timestamp() before any subject existence query. Before each owner handoff, OperationsReadContext requires same bound physical connection/backend, active read-only transaction and entry operations_iam_role; then SET LOCAL ROLE the **literal** subject_number_role / activation_consent_role / entitlement_metering_role / evidence_role / catalog_order_role; assert current_user before owner SELECT. Owner adapter must not open another connection/template, elevate to *_owner, SET SESSION ROLE, RESET outside scope, or query foreign base tables. No network/provider call occurs inside transaction.

In every scope finally restore SET LOCAL ROLE operations_iam_role and assert, including successful empty results and typed refusals. Failure marks rollback-only and returns sanitized 503; do not continue after an aborted transaction or suppress restoration failure into success. At outer finally restore entry login with SET LOCAL ROLE NONE and assert current_user=session_user; if an aborted transaction prevents this, explicitly record restoration as unsuccessful, roll back, and require transaction/pool reset before reuse. Commit/rollback clears all SET LOCAL roles/settings. On returned physical connection assert original session login, autocommit, isolation, readOnly defaults and no active transaction; failure means evict/close connection, never hand contaminated connection to next caller. Retain only safe error classification, not raw causes in HTTP/logs. Test same physical PID reuse after injected restoration failure and verify no lock/role/settings residue. No savepoint continuation, retry, write, audit function, access-event, Outbox or idempotency operation is permitted on success or failure.

Current authority and all candidate/status/detail/trial/count reads use this one snapshot. A change committed after authority read is intentionally not mixed into the result; a new request sees it. Cross-page inserts/updates can change membership and previously visited facts; stable immutable seek keys avoid offset drift but do not promise a frozen multi-page export.

## 7. SQL, privilege sufficiency and bounded work

### 7.1 Owner-local SQL and list algorithm

Subject adapter builds prepared SQL from fixed predicates only (never concatenate values). Logical candidate SQL:

```sql
WITH safe_subjects AS (
SELECT s.id, s.source, s.real_name_status, s.created_at,
       display.number_masked, latest.status AS claim_status,
       EXISTS (SELECT 1 FROM subject_number.caller_number_binding rb
               JOIN subject_number.subject_risk_state r
                 ON r.caller_number_binding_id = rb.id
               WHERE rb.subject_id = s.id AND r.state = 'OPEN') AS risk_open
FROM subject_number.business_subject s
LEFT JOIN LATERAL (
    SELECT b.number_masked FROM subject_number.caller_number_binding b
    WHERE b.subject_id = s.id
    ORDER BY CASE b.status WHEN 'ACTIVE' THEN 0 WHEN 'DISABLED' THEN 1 ELSE 2 END,
             b.id ASC LIMIT 1
) display ON true
LEFT JOIN LATERAL (
    SELECT c.status FROM subject_number.subject_claim c WHERE c.subject_id = s.id
    ORDER BY c.risk_checked_at DESC, c.id DESC LIMIT 1
) latest ON true
)
SELECT id, source, real_name_status, created_at, number_masked, claim_status, risk_open
FROM safe_subjects
WHERE (CAST(? AS uuid) IS NULL OR id = CAST(? AS uuid))
  AND (CAST(? AS text) IS NULL OR source = CAST(? AS text))
  AND (CAST(? AS text) IS NULL OR real_name_status = CAST(? AS text))
  AND (CAST(? AS text) IS NULL OR claim_status = CAST(? AS text))
  AND (CAST(? AS boolean) IS NULL OR risk_open = CAST(? AS boolean))
  AND (CAST(? AS timestamptz) IS NULL
       OR (created_at, id) < (CAST(? AS timestamptz), CAST(? AS uuid)))
ORDER BY created_at DESC, id DESC
LIMIT ?
```

Bind in exact order: subjectId twice, source twice, realNameStatus twice, claimStatus twice, risk boolean twice (OPEN=true/CLEARED=false/absent=null), anchor time twice then anchor UUID, fetch size. All three anchor arguments are null when absent. PostgreSQL can inline this CTE; it is not a materialized reporting table. Never filter claims inside latest's subquery. Tuple comparator uses PostgreSQL UUID ordering, never Java UUID.compareTo signed ordering; parameter anchors retain full stored precision. All schema/table/column identifiers are literals. No SELECT *, ORDER BY client string, OFFSET or total count. A branch-built equivalent with predicates omitted for null filters is allowed only with identical parameterized semantics and test coverage; it is not permission to change the filtering rules.

Without activationStatus filter fetch exactly limit+1 candidate subjects (2–101), then one activation batch (set of at most 101 UUIDs) under activation owner. Missing activation → INACTIVE; drop lookahead only after projecting/validating. With activationStatus filter, activation cannot be joined under subject_number_role: fetch candidate batches of 101 in the same order and snapshot; read their activation map in one bound `WHERE subject_id IN (?,...)` batch; select matching defaulted status in memory and collect only until limit+1 matches. Subsequent candidate batch seeks from the preceding **last scanned** candidate; emitted cursor still seeks from the last **returned** matching item. Never scan later candidates to change the user cursor. Limit to 100 batches/10100 candidate rows per request, discarding processed batches; at most 101 matches retained. If a short batch proves exhaustion return the actual tail. If 100 full batches are consumed without limit+1 matches and exhaustion is unproven, return 503, not a false hasMore=false or empty continuation page. If exactly limit+1 matches are found, no extra exhaustion probe is needed. Nonmatching candidate maps contain no arrays of IDs outside their 101-row batch. This is bounded batched work, not N+1 per subject; at most 200 data SELECTs for filtered list plus fixed authority/settings/assertions. No unbounded activation-ID prefetch or temporary table.

Detail subject adapter uses the same safe CTE restricted by `id = CAST(? AS uuid)` (no list seek/status predicates), then the following exact local queries under one subject-role scope. Activation uses a bounded placeholder list, not a PostgreSQL array containing all subjects. Trial LEFT JOINs ensure missing related facts are visible for integrity failure rather than mistaken for no trial.

```sql
SELECT id, number_masked, number_type, status, verified_at
FROM subject_number.caller_number_binding
WHERE subject_id = CAST(? AS uuid) ORDER BY id ASC LIMIT 101;

SELECT id, status, verification_method, risk_checked_at, verified_at
FROM subject_number.subject_claim
WHERE subject_id = CAST(? AS uuid) ORDER BY risk_checked_at DESC, id DESC LIMIT 101;

SELECT subject_id, status, revision
FROM activation_consent.service_activation
WHERE subject_id IN (CAST(? AS uuid));

SELECT t.id AS trial_id, t.subject_id, t.campaign_key, t.entitlement_grant_id,
       t.expires_at AS trial_expires_at, g.id AS grant_id,
       g.subject_id AS grant_subject_id, g.source_type, g.entitlement_type,
       g.granted_units, g.expires_at AS grant_expires_at,
       b.id AS balance_id, b.grant_id AS balance_grant_id,
       b.granted_units AS balance_granted_units, b.reserved_units,
       b.consumed_units, b.status AS balance_status
FROM entitlement_metering.trial_grant t
LEFT JOIN entitlement_metering.entitlement_grant g ON g.id = t.entitlement_grant_id
LEFT JOIN entitlement_metering.entitlement_balance b ON b.grant_id = g.id
WHERE t.subject_id = CAST(? AS uuid) LIMIT 2;

SELECT count(subject_id) AS evidence_count FROM evidence.evidence_record
WHERE subject_id = CAST(? AS uuid);

SELECT count(subject_id) AS order_count FROM catalog_order.customer_order
WHERE subject_id = CAST(? AS uuid);
```

For a list activation batch, expand the IN list to exactly its 1–101 UUID placeholders and bind each UUID; empty set returns Map.of() without SQL. Validate returned keys are a subset of requested IDs, unique, and every value has valid status/revision. Detail needs at most eight data SELECTs including authority (three subject, activation, trial, evidence, order). No history transition/consent/policy/payment/media query. Timestamp decision is part of authority SELECT or a fixed extra SELECT, not repeated wall-clock reads.

SQL work is bounded in returned rows, retained memory, query count and deadline; there is no false assertion that existing indexes bound all internal examined rows. V003 has no `(created_at,id)` stable-sort index, and count aggregates can scan many matches. Obtain EXPLAIN (ANALYZE, BUFFERS) only against synthetic fixture data; never write an index under this goal. DB timeout or request budget exhaustion → 503 and rollback. Platform/Database owns volume/SLO validation; an index/grant requirement is a stop condition.

### 7.2 Existing capability proof and hard stop

| Executing role | Existing migration capability used | Actual base tables read |
| --- | --- | --- |
| operations_iam_role | V002 lines 345–347 schema USAGE + SELECT | employee_account, only id/status/role/revision |
| subject_number_role | V003 lines 200–217 schema USAGE and SELECT | business_subject, caller_number_binding, subject_claim, subject_risk_state |
| activation_consent_role | V004 lines 485–486 schema USAGE; V007 lines 207–212 table SELECT | service_activation only |
| entitlement_metering_role | V004 lines 492–493 schema USAGE; V006 lines 641–646 table SELECT | trial_grant, entitlement_grant, entitlement_balance only |
| evidence_role | V008 owner grants, as amended/restricted by V014–V017 | evidence_record.subject_id only; V017 retains approved subject counting access |
| catalog_order_role | V004 lines 421–433 schema USAGE and SELECT | customer_order.subject_id only |

This requires **no foreign base-table grant** and no use of table-owner privilege. Owner-local adapters use existing application roles, not *_owner or superuser. Six existing role capabilities suffice; no new object, role, grant, function or migration is in the allowlist. V015–V017 customer grants are not used to make Operations queries work. Runtime credentials must already permit SET ROLE to all six existing roles; production membership setup remains an external prerequisite, as in PA32, not something PA33 creates/repairs. A synthetic non-superuser LOGIN with six NOINHERIT/SET memberships is created only in disposable PostgreSQL tests; that fixture grant is not a production migration or permission to widen real logins. Verify missing each membership separately produces 503/42501 without any corrective SQL. Do not claim the existing three PA25 deployment slots or PA32's three-role login already have these six memberships.

Static privilege sufficiency is proven by the SQL/table/role matrix; live deployed membership/volume cannot be proven here. If exact V017 privilege tests contradict the matrix, or same-login SET ROLE cannot be supplied under already authorized deployment provisioning, or a public/detail-contract delta is actually necessary, **STOP** implementation and return to Controller/user for new authorization. No V018, runtime GRANT, schema helper or public-contract edit is an authorized workaround. Appendix B freezes the exact 22 local files; raw bytes differ from LF Git blobs solely due to pre-existing CRLF checkout. Both local byte freeze and Git-content equality must be checked separately, never normalize files in place.

## 8. Preferred slice and exact allowlist

One preferred implementation slice **S1: complete authoritative Operations subject list/detail**. Its independent gate value is the full employee read behavior with safe pagination/detail and owner/snapshot proof. Splitting routes, owner adapters, codec or CI into module/file slices would leave an unusable or unproved feature; no second slice is justified. S1 author report → Controller validation → independent review/adjudication are gates, not additional behavior slices. This plan task only creates this document; the following is a **future** implementation allowlist subject to plan acceptance, not current write permission. Any extra path/type/test identity requires Controller-authorized plan correction first.

Paths below are relative to repo root; `P = backend/src/main/java/com/onecall/core/`, `T = backend/src/test/java/`. Prefixes expand literally, not globs. One public top-level type per .java filename; package-info is metadata, not a new module. Nested records/enums/failures/interfaces are confined to their listed containing type.

| Class | State | Exact path / type responsibility |
| --- | --- | --- |
| production API metadata | new | P operations_iam/subjectread/package-info.java — named interface |
| production API | new | P operations_iam/subjectread/OperationsSubjectReadService.java — interface; RawListRequest/RawDetailRequest, seven wire record types (SubjectSummary, SubjectPage, OperationsSubjectDetail, BindingSummary, ClaimSummary, ActivationState, TrialSummary), Code/Failure |
| production inbound API | new | P operations_iam/subjectread/OperationsSubjectReadPorts.java — five SPIs, Filters/Anchor/Candidate/SubjectFacts/DetailFacts/BindingFacts/ClaimFacts/ActivationFacts/TrialFacts |
| production scope support | new | P operations_iam/subjectread/OperationsReadContext.java — feature-only role scope, Role enum |
| production application | new | P operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java |
| production validation | new | P operations_iam/internal/subjectread/OperationsSubjectReadRequest.java |
| production crypto | new | P operations_iam/internal/subjectread/OperationsSubjectCursorCodec.java |
| production config | new | P operations_iam/internal/subjectread/OperationsSubjectReadProperties.java — nested Datasource/Cursor, redacted immutable key config |
| production config | new | P operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java |
| production web | new | P operations_iam/internal/web/OperationsSubjectReadController.java |
| production owner adapter | new | P subject_number/internal/operationsread/JooqOperationsSubjectReader.java |
| production owner adapter | new | P activation_consent/internal/operationsread/JooqOperationsActivationReader.java |
| production owner adapter | new | P entitlement_metering/internal/operationsread/JooqOperationsTrialReader.java |
| production owner adapter | new | P evidence/internal/operationsread/JooqOperationsEvidenceCounter.java |
| production owner adapter | new | P catalog_order/internal/operationsread/JooqOperationsOrderCounter.java |
| production filter | existing | P operations_iam/internal/web/OperationsBoundaryFilter.java — exact GET admission + bearer only, bypass Accept for GET; POST Accept/command behavior unchanged |
| production filter | existing | P customer_iam/internal/web/CustomerBoundaryFilter.java — exact early pass-through only |
| production errors | existing | P operations_iam/internal/web/OperationsProblemWriter.java — read Code overload, cursor 400 titles |
| production module metadata | existing | P subject_number/package-info.java |
| production module metadata | existing | P activation_consent/package-info.java |
| production module metadata | existing | P entitlement_metering/package-info.java |
| production module metadata | existing | P evidence/package-info.java |
| production module metadata | existing | P catalog_order/package-info.java |
| unit/web tests | new | T com/onecall/archtests/OperationsSubjectReadWebTests.java |
| unit/parser tests | new | T com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadRequestTests.java |
| unit/crypto tests | new | T com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectCursorCodecTests.java |
| unit/application tests | new | T com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadServiceTests.java |
| unit/config tests | new | T com/onecall/archtests/OperationsSubjectReadConfigurationTests.java |
| unit/architecture tests | new | T com/onecall/archtests/architecture/OperationsSubjectReadBoundaryTests.java |
| container tests | new | T com/onecall/archtests/container/OperationsSubjectReadsIT.java |
| container privilege tests | new | T com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java |
| container fixture, no tests | new | T com/onecall/archtests/container/OperationsSubjectReadFixture.java |
| unit fixture, no tests | new | T com/onecall/archtests/operations/OperationsSubjectReadTestProviders.java |
| architecture test | existing | T com/onecall/archtests/architecture/ModuleBoundaryTests.java — exact maps/edges, retain nine names |
| architecture test | existing | T com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java — retain namedOwnerInterfacesAndTenCustomerDependenciesAreExact and all four identities; synchronize only exact subject_number/evidence target/named-interface dependency assertions under PA33-PLAN-CORRECTION-01 |
| build config | existing | backend/pom.xml — add exactly two Failsafe includes, no dependency/plugin/version delta |
| CI config | existing | .github/workflows/ci.yml — PA33 exact inventories/arithmetic and JAR read-route probes only |

Totals: **37 implementation paths = 25 new (15 production + 8 test classes + 2 fixtures) + 12 existing (8 production + 2 existing tests + POM + CI)**; equivalently **37 = 25 new + 12 existing (8 production + 2 existing tests + POM + CI)**. Production .java files: 15 new including one package-info; 14 new top-level production types, five existing package-info files and three existing classes changed. No existing test method is deleted or renamed and no existing annotated class is added to another suite. One S1 slice, 106 new identities, **70/644 + 29/476 = 99/1120**, token **1121** and POM **29** remain unchanged. The plan/report artifacts themselves are separate gate artifacts, not concealed product allowlist entries. Control/reviews/existing docs/.codex/migrations/OpenAPI and every unlisted path remain excluded except the explicitly authorized cumulative plan correction and new fix-2 artifact.

Retained `OperationsSubjectsWebTests` and `OperationsLifecycleBoundaryTests` are not edited. Preserve their existing POST/customer-pass-through source literals in the filters: `[^/;%]+/(suspensions|resumptions)` remains the two-command POST pattern; customer `operationsPath.matches` remains before `UUID traceId = UUID.randomUUID()`. Add GET branches without replacing those literals or broadening customer pass-through; do not weaken these tests to accommodate refactoring. OperationsWebConfiguration and SyntheticEmployeeSessions also remain unchanged.

## 9. Exact new tests and fixture obligations

Every name below is one ordinary unconditional `@Test void` method. No parameterized/repeated/dynamic/disabled tests and no `@Test` in comments are added. Multiple cases are loops inside these fixed methods. Appendix A supplies the exact retained 1014 method identities, their source and expected XML filenames. New test identity arithmetic must not be adjusted merely to match a failing build.

### 9.1 Surefire additions: six classes / 70 methods

`com.onecall.archtests.OperationsSubjectReadWebTests` — 16:

```text
exactListRouteReturnsOnlyFrozenPageAndSummaryProperties
exactDetailRouteReturnsOnlyFrozenSafeProjection
hiddenMethodsAndNoncanonicalOperationsRoutesRemainNotFound
employeeAuthenticationPrecedesEveryReadInput
zeroTwoAndUncreatableVerifiersAreSanitizedUnavailable
onlyCurrentActiveAdminAndOpsMayRead
missingTwoAndUncreatableReadServicesFailClosed
queryDuplicatesUnknownKeysAndCanonicalUuidUse422
allEnumsAndLimitDefaultsAndBoundsAreExact
cursorInvalidAndFilterMismatchUseFrozen400Problems
optionalFieldsAreOmittedButTailCursorIsExplicitNull
notFoundFollowsAuthorityAndValidation
getBodiesAndCommandHeadersNeverBecomeReadAuthority
customerAndOperationsFiltersPassOnlyExactAdmittedRoutes
jsonMediaAndProblemHeadersNeverExposeSensitiveCanaries
concurrentReadCallersAndFiltersRemainIsolated
```

Within existing `onlyCurrentActiveAdminAndOpsMayRead` and `jsonMediaAndProblemHeadersNeverExposeSensitiveCanaries`, loop over both GET routes: a verified bearer for a currently DISABLED employee plus `Accept: text/html` must return exact 403 FORBIDDEN before media validation; ACTIVE ADMIN and OPS plus `text/html`, `application/json;q=0`, or malformed `application/json;q=broken` must return exact 422 VALIDATION_FAILED after current DB authority. Also cover `application/json;q=0, */*;q=1` (specific JSON exclusion wins), compatible JSON/wildcards, absent Accept, and multiple raw Accept headers. Assert explicit JSON success or five-field application/problem+json error, never 406/HTML/500, no Spring error body and no subject/owner queries on refusals. `detailQueriesAndIncompatibleOrMalformedAcceptAreRejected` covers the same media parser cases for list/detail raw requests; `strictInputsAndDuplicatesHaveFrozenErrorPrecedence` and `disabledMissingAndWrongAudienceEmployeesFailBeforeDisclosure` repeat the 403-vs-422 assertions against current PostgreSQL authority. These are cases inside fixed named methods, not additional tests.

`com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadRequestTests` — 10:

```text
listAllowsExactlyEightQueryKeys
duplicatesIncludingIdenticalCursorValuesAreRejectedFirst
canonicalLowercaseUuidRulesApplyToPathAndFilter
enumValuesAreExactWithoutTrimmingOrCaseFolding
limitDefaultRangeAndDecimalGrammarAreExact
emptyAndUnknownNoncursorInputsAreValidationFailures
singleMalformedCursorIsDeferredUntilAfterNoncursorValidation
filterDigestHasExactOrderedAbsentSentinelEncoding
parameterOrderAndPageSizeNeverChangeNormalizedFilterDigest
detailQueriesAndIncompatibleOrMalformedAcceptAreRejected
```

`com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodecTests` — 12:

```text
encryptedRoundTripPreservesExactTimestampUuidAndDigest
sameAnchorProducesRandomizedOpaqueBoundedTokens
missingMalformedAndAmbiguousKeyMaterialFailsClosed
configurationFailurePrecedesMalformedToken
strictBase64VersionLengthsAndTrailingBytesAreInvalid
tamperedNonceHeaderPayloadAndTagAreInvalid
wrongPurposeKeyAndRetiredKidAreInvalid
onlyAuthenticatedDifferentDigestIsFilterMismatch
activeKeyIssuesAndRetainedKeysVerifyAcrossRotation
retirementNeverFallsBackOrReissuesAReplacement
invalidNanosAndPlaintextShapesNeverReachQueries
keysRequestsAnchorsAndFailuresHaveRedactedRepresentations
```

`com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests` — 16:

```text
trustedCallerAndCurrentEmployeePrecedeInputAndSubjectReads
oneReadOnlyRepeatableNewTransactionOwnsEveryProjection
zeroTwoAndUncreatableOwnerPortsAreUnavailable
ownerHandoffsAssertAndRestoreLiteralRolesOnOneResource
listUsesDescendingSeekAndLimitPlusOneWithoutOffset
activationFilterBatchesCandidatesWithoutSkippingMatchingRows
candidateBudgetExhaustionNeverInventsTailOrContinuation
missingActivationAndAnyBindingRiskProjectExactly
latestClaimSelectionPrecedesClaimStatusFiltering
displayBindingAndOptionalProjectionOrderingAreDeterministic
detailHistoriesAreCompleteWithinCapAndOverflowFailsClosed
trialUsesOriginalGrantBalanceAndDatabaseDecisionTime
countsRemainLongExactAndIndependentOfHistoryJoins
invalidOwnerFactsAndRoleRestorationFailuresAreUnavailable
sqlBeanTransactionAndUnknownFailuresMapWithoutLeakingCauses
allSuccessDenialAndFailurePathsHaveZeroWriteAuditAndOutboxCalls
```

`com.onecall.archtests.OperationsSubjectReadConfigurationTests` — 8:

```text
defaultWebAndDisabledProfilesCreateNoReadPool
persistenceEnabledGraphIsLazyAndUsesDedicatedCredentials
everyReadBeanUsesExactQualifiedDatasourceDslAndContext
templateIsReadOnlyRepeatableRequiresNewWithBoundedTimeouts
ownerAdaptersAreConditionalAndImplementOnlyInboundReadPorts
missingCredentialsAndCursorKeysFailAtAffectedRequestNotBoot
detailDoesNotRequireKeysOrAnyCommandResource
startupPropertiesProvidersAndPoolFailuresAreRedacted
```

`com.onecall.archtests.architecture.OperationsSubjectReadBoundaryTests` — 8:

```text
operationsPublishesOnlyExactReadContractsAndInboundPorts
fiveOwnersImplementInboundPortsWithoutReverseModuleCycles
controllerAndApplicationContainNoForeignBaseTableSql
ownerSqlSelectsOnlyApprovedSafeColumnsAndCounts
readPathHasNoLocksWritesAuditOutboxOrCommandCalls
employeeVerifierIsReusedWithoutPackagedProductionImplementation
v001ThroughV017AndFrozenOpenApiAndDesignDocsRemainUnchanged
allowlistedRoutesProjectionTypesAndDependencyGraphAreExact
```

### 9.2 Failsafe additions: two classes / 36 methods

`com.onecall.archtests.container.OperationsSubjectReadsIT` — 24:

```text
postgres16AppliesExactlyFrozenSeventeenMigrations
activeAdminAndOpsReadWithIndependentEmployeeSessions
disabledMissingAndWrongAudienceEmployeesFailBeforeDisclosure
listDefaultsAndEveryStructuredFilterAreExact
latestClaimFilterDoesNotMatchOlderHistoricalStatus
allBindingRiskStatesAndAbsentActivationHaveExactProjection
stableTimestampUuidTiesSeekWithoutDuplicatesOrOffset
limitPlusOneEmptyAndTailPagesUseLastReturnedAnchor
normalizedFiltersBindCursorButPageSizeDoesNot
tamperedWrongKeyAndRotatedCursorsHaveExactProblems
strictInputsAndDuplicatesHaveFrozenErrorPrecedence
detailReturnsOnlySafeFieldsAndOmissionRules
displayBindingAndClaimOrderingAreDeterministic
detailHistoryZeroOneHundredAndOverflowAreBounded
trialAbsentOriginalExpiredExhaustedAndReservedRulesAreExact
missingOrInconsistentTrialRelationsFailWithoutRepair
evidenceAndOrderCountsIncludeAllSubjectStatusesAndPurchasers
activationFilteredCandidateBatchesRemainCompleteWithinBudget
candidateBudgetAndDatabaseTimeoutsNeverReturnPartialSuccess
authoritySubjectActivationTrialAndCountsShareOneSnapshot
committedEmployeeAndDomainChangesAreVisibleOnNextRequest
successDenialNotFoundAndFaultsPerformZeroBusinessWrites
readEndpointsAndExistingCommandCustomerPublicRoutesStayIsolated
responsesProblemsAndCapturedLogsNeverContainSensitiveCanaries
```

`com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT` — 12:

```text
syntheticLoginHasExactlySixNonInheritedSetMemberships
eachOwnerReadsOnlyItsOwnApprovedProjection
operationsRoleCannotReadForeignBaseTablesWith42501
eachMissingMembershipFailsClosedWithoutPrivilegeRepair
allOwnerReadsUseSameBackendPidTransactionAndSnapshot
wrongResourceEntryRoleAndMissingTransactionAreRejected
readOnlyDatabaseRejectsWritesAndLockingQueries
normalAndExceptionalHandoffsRestoreOperationsRole
failedRestorationRollsBackAndReusesSameCleanPhysicalConnection
poolReadOnlyIsolationAutocommitAndLocalSettingsAreRestored
frozenAclOracleMigrationsRolesAndFunctionsRemainUnchanged
noResidualLocksTransactionsAuditOrOutboxFactsRemain
```

Fixture obligations: new read fixture owns PostgreSQL `postgres:16.15`, UTF8 server encoding, C collation, frozen Flyway V001–V017 chain; non-superuser login with exactly the six SET/non-inherited memberships; all credentials random and disposable. Reuse existing fixture SQL helpers and independent V017 ACL oracle by read-only test imports, not by copying private worktrees or altering production provisioning. Start only owned containers; clean up sessions/pools/containers in finally. SyntheticEmployeeSessions is reused unchanged. Test provider fixture has deterministic fake owner ports, throwing/lazy/duplicate beans, immutable inputs, statement counters and safe failures; no annotation tokens or production-scanned configuration. Capture query/backend/tx/role instrumentation only in tests and never emit PII.

**Distinct-subject fixture matrix:** each label below names a separate deterministic subject UUID (a family expands to distinct UUIDs for each stated combination). Never give EMPTY rows from a populated family. Every setup satisfies frozen constraints/triggers; no disabled constraint, trigger bypass, migration edit or assumed deferral. Unless a row says otherwise, use no activation/trial and zero evidence/orders. All names in the test column are the fixed §9 methods; IT names are in OperationsSubjectReadsIT unless qualified.

| Fixture subject(s) and exact state | Required named test assertions |
| --- | --- |
| EMPTY: zero bindings, zero claims, no activation, no trial/grant/balance | `allBindingRiskStatesAndAbsentActivationHaveExactProjection`, `detailReturnsOnlySafeFieldsAndOmissionRules`, `detailHistoryZeroOneHundredAndOverflowAreBounded`, `trialAbsentOriginalExpiredExhaustedAndReservedRulesAreExact`: [] histories; omitted maskedNumber/claimStatus/activation/trial; list INACTIVE/CLEARED; exact zero counts. |
| HISTORY-1 and HISTORY-100: respectively 1/1 and 100/100 binding/claim rows; HISTORY-B101: 101 bindings/1 claim; HISTORY-C101: 1 binding/101 claims; HISTORY-BOTH101: 101/101 | `detailHistoryZeroOneHundredAndOverflowAreBounded` and service `detailHistoriesAreCompleteWithinCapAndOverflowFailsClosed`: exact 0/1/100 successful lengths using EMPTY/1/100, deterministic full order, each independent 101 overflow and combined overflow → whole-response sanitized 503 + rollback/no partial JSON. |
| BINDING-RISK-{ACTIVE,DISABLED,RETIRED}-{OPEN,CLEARED}: six distinct subjects with one binding and its corresponding risk; BINDING-NORISK: one binding/no risk; MIXED-RISK: active CLEARED plus disabled/retired OPEN bindings | `allBindingRiskStatesAndAbsentActivationHaveExactProjection`, `listDefaultsAndEveryStructuredFilterAreExact`: all three lifecycles, OPEN from any lifecycle, CLEARED with no OPEN/no risk; `displayBindingAndClaimOrderingAreDeterministic`: active-first display rank, ascending binding IDs, MOBILE/LANDLINE and nullable/nonnullable verifiedAt consistent with constraints. |
| CLAIM-LATEST-{ACCEPTED,REJECTED,CONFLICT}: three distinct subjects, each with multiple older claims of the other statuses plus tied latest timestamps and distinct IDs; CLAIM-NONE: no claims | `latestClaimFilterDoesNotMatchOlderHistoricalStatus`, `displayBindingAndClaimOrderingAreDeterministic`, `listDefaultsAndEveryStructuredFilterAreExact`: risk_checked_at DESC/id DESC latest chosen before filtering; older-only matches excluded, no-claim optional omitted; cover SMS/WECHAT_PHONE/CALLER_CHALLENGE/MANUAL_REVIEW and status-correct verifiedAt omission/presence across rows. |
| ACTIVATION-ABSENT: no activation; ACTIVATION-INACTIVE, ACTIVATION-ACTIVE, ACTIVATION-SUSPENDED: three distinct present rows with valid positive revisions and required lifecycle metadata | `listDefaultsAndEveryStructuredFilterAreExact`, `allBindingRiskStatesAndAbsentActivationHaveExactProjection`, `detailReturnsOnlySafeFieldsAndOmissionRules`: absent and present INACTIVE both match list INACTIVE but only present row emits detail activation; exact ACTIVE/SUSPENDED filters, statuses and revisions. |
| FILTER-{IVR,MINIAPP,WEB,OPERATIONS}-{UNVERIFIED,VERIFIED}-{OPEN,CLEARED}: sixteen distinct subjects, correct real-name timestamp shape, one risk-bearing binding each | `listDefaultsAndEveryStructuredFilterAreExact`: all four sources × both real-name × both risk states individually and combined; canonical subjectId and limit 1/20/100; no client-derived authority. |
| SORT-TIES: distinct subjects at equal microsecond created_at, UUIDs straddling Java signed/unsigned boundary, and older/newer timestamps | `stableTimestampUuidTiesSeekWithoutDuplicatesOrOffset`, `limitPlusOneEmptyAndTailPagesUseLastReturnedAnchor`, `normalizedFiltersBindCursorButPageSizeDoesNot`: PostgreSQL descending UUID order, last-returned anchor, empty/single/exact-limit/lookahead/tail pages, no duplicates or offset. |
| TRIAL-AVAILABLE, TRIAL-RESERVED, TRIAL-FULLY-RESERVED: three distinct consistent FIRST-TRIAL graphs, balance AVAILABLE, consumed<granted, respectively zero/partial/all remaining units reserved; TRIAL-EXHAUSTED: consumed=granted/reserved=0, balance EXHAUSTED; TRIAL-TIME-EXPIRED: original expiry <= DB decision time with AVAILABLE balance; TRIAL-BALANCE-EXPIRED: balance EXPIRED with consumed<granted; TRIAL-AT-EXPIRY: original expiry exactly DB decision time | `trialAbsentOriginalExpiredExhaustedAndReservedRulesAreExact` and service `trialUsesOriginalGrantBalanceAndDatabaseDecisionTime`: EMPTY omits trial; original units/expiry, remaining arithmetic, GRANTED including fully reserved zero remaining, EXHAUSTED, both expiry causes and equality boundary EXPIRED. Pin decision time through test-only transaction setup/doubles where a live exact timestamp cannot be scheduled. |
| TRIAL-NO-BALANCE: valid trial→grant, absent balance; TRIAL-EXPIRY-MISMATCH: trial/grant different expiry at initial insertion if frozen checks permit, otherwise test-only TrialFacts double; other impossible relations use doubles only | `missingOrInconsistentTrialRelationsFailWithoutRepair`, service `invalidOwnerFactsAndRoleRestorationFailuresAreUnavailable` and `trialUsesOriginalGrantBalanceAndDatabaseDecisionTime`: missing balance, relation/counter mismatch and either direction of expiry mismatch → sanitized 503 + rollback/no partial response/no repair; success projects validated trial_expires_at (§5). |
| COUNTS: distinct subject with multiple evidence/order/payment statuses, purchasers and personal/organization contexts, plus unrelated subject with nonmatching rows | `evidenceAndOrderCountsIncludeAllSubjectStatusesAndPurchasers`: all matching subject evidence/orders exactly once, unrelated rows excluded, payment/history joins do not multiply counts. |
| SCAN-WITHIN-BUDGET and SCAN-OVER-BUDGET: isolated fixture datasets of distinct candidate subjects, mixed absent/INACTIVE/ACTIVE/SUSPENDED activations; delayed matches across batches, matching lookahead at boundary, short proven tail, and 10,100 scanned candidates with insufficient matches/unproven tail | `activationFilteredCandidateBatchesRemainCompleteWithinBudget`, `candidateBudgetAndDatabaseTimeoutsNeverReturnPartialSuccess` and service `candidateBudgetExhaustionNeverInventsTailOrContinuation`: no skipped matching candidates, exact lookahead/tail, 100-batch limit, sanitized 503 + rollback/no partial response on unproven tail or timeout. |

**Test-only corrupt authority and relation injection:** use the existing Mockito/jOOQ dependency path inside OperationsSubjectReadServiceTests and the listed OperationsSubjectReadTestProviders fixture. Supply a Mockito DSLContext/Result/Record double through the service's already-required qualified DSL constructor dependency; stub the exact employee SELECT to return trusted-id rows with unknown role, unknown status, null/zero/negative revision or mismatching identity. Stub transaction/resource/current_user checks to their valid baseline for these unit cases; do not add an authority-reader port, package-private production hook, factory or new type/path. `trustedCallerAndCurrentEmployeePrecedeInputAndSubjectReads` and `invalidOwnerFactsAndRoleRestorationFailuresAreUnavailable` assert sanitized 503, rollback and zero subject-port calls for each corrupt row. Real DB tests cover actual ACTIVE/DISABLED/missing employees without attempting CHECK-invalid inserts; authority corruption is not claimed as live-DB evidence.

For malformed activation/owner/trial facts use existing listed port doubles in those same fixed methods. In `missingOrInconsistentTrialRelationsFailWithoutRepair`, use controlled setup transactions on the disposable fixture's setup connection for constraint-valid missing-balance/initial expiry-mismatch cases, execute the read and always roll back/clean the setup; if visibility requires a committed setup, isolate that case's data and remove it in finally. Non-deferrable V006 trial→grant/balance FKs cannot produce orphan facts: prove those impossible returned relations through the existing port double, not SET CONSTRAINTS, constraint disabling, a new production seam or wider six-role login. Document which assertions are DB-backed versus doubles. Indexed key IDs containing case, `-` and `_` as values, duplicate IDs/material, gaps, and lazy bad configuration are covered inside `missingMalformedAndAmbiguousKeyMaterialFailsClosed`, `activeKeyIssuesAndRetainedKeysVerifyAcrossRotation`, `missingCredentialsAndCursorKeysFailAtAffectedRequestNotBoot` and `startupPropertiesProvidersAndPoolFailuresAreRedacted`; no new method identities.

Concurrency uses two connections and latches after authority/snapshot establishment; assert concurrent commits are absent from this response and present next request. Restoration-fault test pins a one-connection pool, records failed backend PID, forces rollback and verifies the same clean PID on subsequent request (or explicit eviction when reset cannot be proven), never merely observes a different fresh connection. Capture before/after full row fingerprints/counts for subjects/bindings/claims/risk/activation/transitions/trial/grants/balances/consents/reservations/evidence/orders/audit/Outbox; include failures and 404/403 paths.

## 10. Reconciled inventories and CI arithmetic

Author ran a read-only AST extraction of actual CI expected maps plus all addition/update loops and compared them against every source `@Test void` identity, then compared actual Failsafe includes parsed from POM. **SOURCE=CI=POM passed**: Surefire 64 classes/574; Failsafe 27/440; 91 classes/1014 executed-method identities. Raw `@Test` token occurrences and token-bearing lines both 1015: the sole extra is existing PostgresContainerIT comment `// evidence inside this existing method; no new @Test and no catalog weakening.`. Do not count that comment as a method or delete it to fake arithmetic. No XML files exist in this worktree; XML runtime equality is **UNAVAILABLE, not passed**. Historical Controller reports are not fresh runtime evidence.

After S1: Surefire **70/644**, Failsafe **29/476**, total **99/1120**, annotation/token-line count **1121**, POM Failsafe includes **29**. Add only `**/OperationsSubjectReadsIT.java` and `**/OperationsSubjectReadRoleLeastPrivilegeIT.java` to existing exact 27 includes. Existing excludes and failIfNoTests remain. New report filenames are `target/surefire-reports/TEST-<six new FQCNs>.xml` and `target/failsafe-reports/TEST-<two new FQCNs>.xml`; exact names are expanded by the FQCN lists in §9. Fixtures produce zero reports. Existing report inventory is Appendix A.

CI must add exact `pa33_unit_additions`/`pa33_container_additions` maps using §9, assert disjointness, update new expected maps, preserve all historical source subtraction/oracles, and subtract pa33 names in **every** pre-PA33 historical method/token scan (including PA32-start). Add PA33-start totals [574,440]=1014 and token-line 1015; final [644,476]=1120/token1121/classes70+29/POM29. Preserve baseline chronology: 112+178=290 →128+192=320 →162+198=360 →196+204=400 →231+214=445 →266+214=480 →309+214=523 →309+234=543 →309+246=555 →341+272=613 →375+302=677 →417+336=753 →468+370=838 →520+404=924 →574+440=1014 →644+476=1120. Corresponding comment-inclusive token-line totals are 291,321,361,401,446,481,524,544,556,614,678,754,839,925,1015,1121. Update the early aggregate Surefire assertion from 574 to 644, all final inventory arguments/messages, and source-class coverage; do not change historical PostgresContainerIT64 or any older method identity.

Each full report run must have exactly those filenames/classes/testcases, one occurrence per method, no skipped/failure/error nodes or attributes, no unsupported conditional tests, no extra stale XML. Equality is **source method set = CI literal method set = XML testcase set**, plus POM IT class set = Failsafe XML class set. Totals alone are insufficient. On full run, no -DskipTests, failsafe skip, partial -Dtest or test exclusion may substitute. Report focused XML before clean removes it, and full clean XML separately.

## 11. Validation commands and Controller responsibilities

Commands in this section are for S1/Controller validation after authorization; the plan author ran only read-only inventory/hash/environment probes. Use native Linux Temurin 21.0.13+9 and Maven wrapper 3.9.9; toolchain versions are existing pins, not newly selected recommendations. Do not chmod/edit wrapper, install dependencies or invoke runtime builds during this plan-only task. If a required tool/environment is unavailable, record exact command, exit/error, missing prerequisite and owner; never call a skipped Testcontainers run a pass.

### 11.1 Focused PostgreSQL16 and full clean

From backend with a usable pinned JDK and Docker daemon:

```bash
java -version
./mvnw -B -ntp -v
docker info
./mvnw -B -ntp -Pcontainer-smoke \
  -Dtest=OperationsSubjectReadWebTests,OperationsSubjectReadRequestTests,OperationsSubjectCursorCodecTests,OperationsSubjectReadServiceTests,OperationsSubjectReadConfigurationTests,OperationsSubjectReadBoundaryTests,EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,OperationsSubjectLifecycleServiceTests \
  -Dit.test=OperationsSubjectReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT,OperationsSubjectLifecycleIT,OperationsLifecycleRoleLeastPrivilegeIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

Focused expectation: new 70 + existing (8+12+8+8+9+18)=**133 Surefire** in 12 classes; new 36 + existing24+12=**72 Failsafe** in 4 classes; total16/205; PostgreSQL16, UTF8, exactly 17 successful migration history rows, all zero skips/errors/failures. Full expectation §10:70/644+29/476. If inline mocking requires a Java agent, use only the already-resolved byte-buddy-agent pinned by the existing dependency graph and record its resolved version/path through `-DargLine=-javaagent:<absolute-existing-pinned-agent.jar>`; do not fetch or add a new dependency to fix the environment. No undocumented new Maven profile.

Run the **checked-in CI scripts**, not hand-rewritten approximations, from backend. The following extraction commands execute the exact three named steps without PyYAML or new dependencies (after full clean verify):

```bash
python3 - <<'PY'
from pathlib import Path
import subprocess, textwrap
workflow = Path('../.github/workflows/ci.yml').read_text()
for name in ('assert container smoke actually ran',
             'default and opt-in web jar smokes',
             'production jar dependency firewall'):
    section = workflow.split('      - name: ' + name + '\n', 1)[1]
    script = section.split('        run: |\n', 1)[1].split('\n      - name:', 1)[0]
    script = script.split('\n  contract-regressions:', 1)[0]
    subprocess.run(['bash', '-e', '-c', textwrap.dedent(script)], check=True)
PY
```

Inventory step must prove §10 including exact XML sets. Packaged default executable JAR must exit successfully, start OneCallApplication, and start no web server/Hikari pool. Web-only JAR must start on ephemeral port with no DB/verifier adapter, and test both read paths: no bearer→401, malformed/duplicate/customer bearer→401 where test verifier can distinguish, opaque bearer with no production verifier→503; wrong method/encoded/semicolon/double-slash/extra/trailing paths and `/employees`→404. Keep existing POST/customer/public/internal-gateway probes. Add at least list/detail no-bearer401, opaque-bearer503 and list POST404/detail DELETE404, plus encoded and downstream hidden routes; never use GET detail404 now that it is admitted. Package probes also check exact Problem fields/headers, random distinct server traces, no HTML/credentials/SQL and no cookie. Production JAR must contain no test providers/fixtures, no EmployeeSessionVerifier implementation, no extra SecurityFilterChain implementation, provisioning SQL or duplicate ZIP entries.

Dependency firewall expectations remain unchanged: baseline37/34, SSM72/65, counterparts64, dependency-only8, packager1, deltas35/0 and31/0, **116 adversarial probes**; no new library or production IAM/provider adapter. Inspect exact dependency coordinates and packaged class interfaces, not just artifact names.

### 11.2 A1–A5 and frozen files

At repo root, use existing Node22.23.4 and locked toolchain:

```bash
node --version
(cd api/openapi && npm ci && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
for stage in s1 s2; do
  node acceptance/validate.mjs --stage "$stage" --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures $([ "$stage" != s2 ] || printf '%s' --check-doc-links)
done
sha256sum docs/12-technology-stack-and-system-architecture.md docs/14-production-data-model-and-flyway-schema-spec.md docs/15-production-openapi-surface-spec.md docs/17-production-security-design-spec.md api/openapi/onecall-v1.openapi.yaml backend/src/main/resources/db/migration/V*.sql
git diff --check
```

Expect A1 exact49 operations/no doc-map drift; A2/A3 all fixture checks and zero A0/A1 drift; A3 currently23 fixtures; A4 currently113 fixtures/471 nonfinite probes; A5 s1/s2 matrix drift zero and no runtime-readiness overclaim. Record actual validator output rather than inferring counts from PA32. CRLF may affect A2/A5 document parsing: report raw-run result first; if necessary run only against explicitly logged LF-equivalent **temporary copies** outside repo, preserve both evidence sets, and do not present that as a raw-byte run or edit frozen docs. Hosted clean Linux checkout remains distinct evidence. Hash all22 Appendix B files before/after; enumerate migration names exactly V001–V017, assert zero V018/new files. Inspect no GRANT/ALTER ROLE/CREATE TABLE/function/write SQL in product diff; fixture DDL is confined to disposable test setup. Source+CI+POM equality without XML is not full validation.

Controller-validator owns: independently verify dispatch/model and candidate allowlist; enforce the already-accepted eight-property summary and fail-closed detail/candidate capacity dispositions; freeze reviewed head/diff; run and archive focused/full XML plus logs under separate external evidence directories; independently compare exact source/CI/POM/XML and hashes; run JAR/dependency/A1–A5 checks; inspect role/snapshot/no-effect proof, privacy canaries and restoration fault behavior. An author green report does not self-accept the slice. Independent plan/code review remains a separate fresh gate. Stop for fresh independent A/B Planreview; do not ask reviewers, mutate review state, push or enter review from this author task.

### 11.3 Author environment and honest limits

Read-only probes in this plan task found Node v22.23.4. `java -version` resolves `/mnt/d/tools/jdk21/bin/java` and fails with WSL `UtilBindVsockAnyPort ... socket failed 1`; no working native JDK was demonstrated. Docker CLI exists but `docker info` is permission-denied on `/var/run/docker.sock`. There are **zero** existing TEST-*.xml reports and no built JAR proof. Maven/Testcontainers/full clean/JAR/firewall/A1–A5 were not executed here; this task forbids their generated repository outputs and is a planning gate. Runtime-validation blocker owner is Controller/Platform (native JDK, Docker permissions, dependency cache); do not attempt escalation or change system/docker groups. Static inventory and frozen-byte proofs are available; runtime success is not claimed.

## 12. Residual register and completion contract

| Risk / question | Owner / disposition |
| --- | --- |
| Original seven-vs-eight summary discrepancy | Controller-accepted: exact OpenAPI eight, no frozen source edit; no remaining arithmetic adjudication. |
| Complete detail can exceed100 bindings/claims; activation-filter scan can exceed100 batches/10,100 candidates without sufficient lookahead/proven tail | Controller-accepted residual, destination Product/Backend/Database: sanitized 503 + rollback and no partial response. No silent truncation, offset, arbitrary-history success claim, V018, index, grant or OpenAPI change. A later arbitrary-cardinality-success interpretation requires new user authorization. |
| Missing stable-sort covering index/count volume may exceed time budget | Database/Platform/Backend: synthetic plans and deployment SLO evidence; new index/migration is a hard stop, not PA33 work. |
| Production six-role login and key-ring custody/replica rotation are not provisioned | Platform/Database/Security, Q17/Q23 and deployment readiness; fail closed when absent; do not widen the three PA25 slots or claim an existing real login works. |
| Production employee issuer/session store/revocation/MFA/Q1 unavailable | Identity/Security/Platform external; synthetic seam proves behavior only, no production adapter. |
| Snapshot authority can precede a concurrent disable; pagination has no export snapshot | Backend/Security: accepted read-snapshot semantics, next request rechecks; no row-lock serialization or cross-page promise. |
| Trial projection and mask fallback are implementation choices grounded in existing facts | Entitlement/Security/Controller: verify exact unit/status precedence and output allow-list in review; no live-policy or identity-provider extension. |
| Unusable JDK/Docker and absent runtime XML | Controller/Platform: blocking runtime validation only; no fabricated execution or skipped-as-pass. |
| Dispatch runtime/model cannot be independently attested inside author document | Controller: inspect runner metadata before accepting author completion. |
| Local CRLF/Git metadata mapping | Controller: preserve raw bytes, use read-only absolute Git dir, archive normalized temporary validation separately. |

No static schema/grant/public-contract blocker was discovered under the explicit owner-role and Controller-accepted capacity design above. Deployment, runtime-validation and fresh independent A/B Planreview remain open; accepted capacity is no longer an unanswered adjudication. This bounded fix is not implementation authorization or a plan-pass claim.

Author completion report format: task/gate/actual HEAD; exact one created artifact; size and SHA256 measured **after** final write; current scope diff and preserved untracked paths; direct evidence and static counts (source=CI=POM, annotation, migration/file hashes); proposed S1 allowlist/test/CI deltas; commands actually run versus planned; environment limitations; classified risks/blockers/owners; explicit “author complete — stop for Controller adjudication and independent plan review.” Do not embed this file's own hash inside itself. No stage/commit/push/PR/merge/approval/ready/reviewer request/external comment/plan-review transition.

## Appendix A. Exact retained source and XML method inventory

The following inventory is mechanically derived from the inspected repository and checked equal to CI's accumulated literal method maps and POM's27 IT includes. Each listed class expands to source `backend/src/test/java/<FQCN with dots replaced by slashes>.java`; each unit XML is `backend/target/surefire-reports/TEST-<FQCN>.xml`, each integration XML is `backend/target/failsafe-reports/TEST-<FQCN>.xml`. These are expected XML filenames, not a claim those reports exist. Every listed method is retained unchanged in identity/count by S1.

### A.1. Surefire: 64 classes / 574 retained methods

`com.onecall.archtests.CustomerBusinessNumbersWebTests` — 12:

```text
absentAndValidLimitsUseDefaultAndInclusiveBounds
authenticationWorkspaceLimitCursorAndLifecycleErrorPrecedenceIsExact
crossAccountPersonalOrganizationAndCrossOrganizationCursorsAreInvalid
exactGetRouteReturnsOnlyFrozenPageAndSevenItemFields
invalidAndDuplicateLimitsAreValidationFailed
malformedDuplicateAndTamperedCursorsAreCursorInvalid
missingInvalidDuplicateAndWrongAudienceBearerAreUnauthenticated
nonGetAndNonCanonicalRoutesStayNotFoundWithoutNewSurface
personalAndOrganizationSelectorsComeOnlyFromVerifiedPrincipal
serviceConfigurationAndUnexpectedFailuresUseSanitizedProblems
successAndProblemsNeverLeakSecretsAndRetainNoStoreHeaders
unavailableVerifierAndMissingOrMalformedWorkspaceAreUnavailable
```

`com.onecall.archtests.CustomerEvidenceConfigurationTests` — 6:

```text
defaultAndNonWebProfilesRemainDatabaseFree
detailNeverResolvesCursorKeyMediaOrSharingResources
evidenceTemplateIsIndependentReadOnlyRepeatableRequiresNew
missingCredentialsReadersAndCursorKeysFailClosedWithoutFallback
ownerReadersUseOnlyTheQualifiedCustomerDsl
webWiringIsLazyAndInvalidRequestsDoNotCreatePools
```

`com.onecall.archtests.CustomerEvidenceWebTests` — 12:

```text
authenticationScopeLimitCursorLifecycleAndMissingPrecedenceIsExact
exactDetailRouteReturnsOnlyFrozenDetailAndArtifactKeys
exactListRouteReturnsOnlyFrozenPageAndSummaryKeys
fullUuidValidationAndUppercaseAcceptanceAreExact
limitDefaultBoundsLexicalDuplicatesAndOverflowMatchCustomerPrecedent
malformedDuplicateTamperedAndCrossContextCursorsAreInvalid
missingMalformedDuplicateRejectedAndWrongAudienceBearersFailBeforeInputs
missingServicesKeysAndUnexpectedFailuresUseSanitizedProblems
queryHeadersCookiesAttributesAndBodiesCannotSelectOwnersOrFilters
responsesNegotiationAndExistingRoutesPreserveCustomerBoundary
unavailableVerifierAndMalformedTrustedWorkspaceFailClosed
unsupportedMethodsAliasesExtraSegmentsAndTrailingRoutesAreNotFound
```

`com.onecall.archtests.CustomerMeWebTests` — 8:

```text
applicationNegotiationAndErrorDispatchUseFrozenProblems
authenticatedGetReturnsFrozenCustomerContext
clientSelectorsHeadersCookiesAndBodyCannotOverrideContext
concurrentRequestsKeepAccountAndChannelIsolated
missingMalformedAndDuplicateBearersReturnFrozen401
otherPathsMethodsAndAliasesAreNotCustomerOperations
reachableProblemsAndResponseHeadersAreClosedAndSanitized
reservedProblemVocabularyIsWriterOnly
```

`com.onecall.archtests.CustomerSharesCommandWebTests` — 4:

```text
commandRoutesDoNotChangeShareListRoute
createUsesTrustedCallerAndMapsCreatedAndReplay
invalidCommandAndMissingServiceMapToProblemResponses
revokeAndRotateMapClosedResponseUnions
```

`com.onecall.archtests.CustomerSharesWebTests` — 10:

```text
authenticatedPersonalAndOrganizationGetsUseTrustedCreatorScope
clientSelectorsAndRequestAttributesCannotOverrideTrustedScope
concurrentRequestsKeepAccountAndSelectedWorkspaceIsolated
exactGetAndPostRoutesPassHighestPriorityFilter
limitAndCursorValidationHaveDeterministicPrecedence
meRemainsCompatibleWithLegacyAndSelectedSessions
missingInvalidAndDuplicateBearersFailBeforePageParsing
negotiationAndBufferedFailuresPreservePa11Boundary
queryErrorsUseExactClosedProblems
webWithoutSharingPersistenceFailsClosedAndDefaultStaysDatabaseFree
```

`com.onecall.archtests.CustomerSubjectActivationConfigurationTests` — 8:

```text
activationUsesQualifiedWritableReadCommittedTransaction
ambiguousOrMissingOwnerGraphFailsClosed
disabledActivationCreatesNoPoolOrOwnerGraph
enabledActivationRequiresExplicitDedicatedCredentials
entitlementMandatoryTemplateUsesSameManagerAndDsl
propertiesAndInitializationFailuresRedactCredentials
utcInitializationAndPoolClosePreserveLifecycle
webWithoutPersistenceRemainsAuthenticatedFailClosed
```

`com.onecall.archtests.CustomerSubjectActivationsWebTests` — 12:

```text
acceptUsesExactRouteAndFrozenResultShape
activationUsesBodyEventWithoutIdempotencyHeader
authenticationPrecedesValidationAndPoolResolution
bodyRejectsMalformedUtf8TrailingJsonAndInvalidLengths
closedBodyRejectsUnknownDuplicateMissingAndWrongTypedFields
declineReturnsOnlyCommittedTypedProblem
domainFailuresUseOnlyOperationErrorSet
encodedAndUnrelatedRoutesRemainClosed
malformedTrustedContextFailsClosed
pathContentTypeAndDecisionValidationUseFrozenErrors
responsesProblemsAndLogsRedactInternalActivationFacts
verifiedSessionSuppliesAccountChannelAndWorkspace
```

`com.onecall.archtests.CustomerSubjectClaimsWebTests` — 12:

```text
closedBodyRejectsUnknownDuplicateMissingAndCoercedFields
concurrentSessionsKeepCallerAndCommandContextIsolated
domainProblemsExposeOnlyThreeFrozen409Codes
exactPostReturnsOnlyFrozenAcceptedFields
exactRouteMediaNegotiationAndAuthenticationPrecedenceHold
idempotencyHeaderHasExactBoundsAndOpaqueIdentity
methodUuidAndProofReferenceValidationUseUniform422
missingInvalidDuplicateAndWrongAudienceSessionAreUnauthenticated
missingTrustedWorkspaceAndUnavailableDependenciesFailClosed
onlyTrustedPersonalWorkspaceCanReachOwnerCommand
responsesProblemsAndFailuresNeverExposeSensitiveCanaries
storedRejectedAndConflictReplayOmitVerifiedAt
```

`com.onecall.archtests.CustomerSubjectDetailConfigurationTests` — 6:

```text
controllerUsesOnlyQualifiedSubjectServiceProviderAndProblemWriter
defaultAndNonWebProfilesRemainInert
missingResourcesFailClosedWithoutCursorEvidenceMediaOrCommandResources
ownerReadersShareExactlyTheQualifiedCustomerDsl
subjectTemplateHasIndependentReadOnlyRepeatableNewTransactionSettings
webWiringIsLazyAndSubjectValidationDoesNotCreatePools
```

`com.onecall.archtests.CustomerSubjectsWebTests` — 12:

```text
extraQueryParametersAndGetBodiesAreIgnoredWithoutChangingScope
getUsesExactAuthenticatedSubjectRouteAndFourFieldResponse
invalidTrustedWorkspacePrecedesPathValidation
malformedAbbreviatedAndDottedSubjectValuesReturnValidationFailed
missingMalformedDuplicateAndRejectedBearerNeverResolveService
missingServiceAndPersistenceFailuresUseSanitizedUnavailable
responseNeverAddsOptionalClaimOwnershipOrContentFields
subjectNotFoundForbiddenAndInternalUseExistingProblemEnvelope
subjectReadDoesNotRequireCursorKeyAndExistingRoutesStayUnchanged
trustedSelectedWorkspaceIsTheOnlyAuthority
unavailableVerifierFailsClosedBeforeSubjectResolution
unsupportedMethodsExtraSegmentsAndTrailingRoutesRemainNotFound
```

`com.onecall.archtests.CustomerWebConfigurationTests` — 6:

```text
customerAndSharingGraphsUseExactQualifiers
customerCredentialsAreExplicitAndValidatedBeforeConnection
defaultRuntimeHasNoCustomerOrServletBeans
testProfileAndProductClasspathCannotEnableVerifierBypass
webProfileIsExplicitAndMissingAdapterHasNoDatabaseEffects
webSettingsDisableUnsafeLoggingAndSessionBehavior
```

`com.onecall.archtests.EvidenceMediaReadPersistenceConfigurationTests` — 4:

```text
enabledInvalidCredentialsFailClosedWithSanitizedRootCause
missingDisabledAndNonPersistenceProfilesAreInert
propertiesConfigurationAndDatasourceStringFormsAreRedacted
validExplicitCredentialsWireOnlyQualifiedReadOnlyResources
```

`com.onecall.archtests.MediaGatewayConfigurationTests` — 4:

```text
duplicateByteAuthoritiesFailClosedEvenWithPrimary
nonWebProfilesDoNotRegisterGatewayOrActivateProviders
singleExplicitByteAuthorityIsUsedInEitherRegistrationOrder
webGatewayPrecedesCustomerBoundaryAndDefaultBearerIsUnavailable
```

`com.onecall.archtests.MediaGatewayWebTests` — 18:

```text
abortCloseAndFlushFailuresPreserveCleanupCounts
allPlaybackDenialsHaveOneNonDisclosingEnvelope
ambientTransactionsAndBoundResourcesPreventTransfer
bearerIsPropagatedOpaqueWithoutCustomerIdentity
disconnectAndCommittedFailuresNeverAppendProblems
fullGetReturnsExactAuthorizedBytesAndHeaders
gatewayRouteMethodQueryAndBodyAreClosed
largeObjectTransferUsesFixedBufferAndLongAccounting
malformedAndDuplicateBearerStopBeforeVerification
malformedMultipleAndOverflowRangesStopBeforeAuthorization
misboundSessionAbortsAndClosesBeforeAnyRead
partialTemporalWindowAndUnsafeMimeNeverOpenBytes
rangePresenceSelectsExpectedPurposeWithoutClaimFallback
responsesAndCapturedLogsNeverLeakPrivateCanaries
shortZeroAndImpossibleReadsFailWithoutExtraRead
singleRangeGetReturnsExactIntervalAnd206Headers
unavailableDependenciesAndDefaultByteProviderFailClosed
unsatisfiableRangeDisclosesSizeOnlyAfterAuthorization
```

`com.onecall.archtests.MediaPlaybackConfigurationTests` — 19:

```text
bothEnabledConsumersShareAuthorityInEitherOrder
defaultAndWebProfilesProvideOneUnavailableAuthority
explicitFileSelectionRemainsIndependentOfTencentClient
fileSourceAndCustomProviderAreAmbiguousInEitherOrder
fileSourceRequiresExplicitSelectionAndPath
fileSourceSharesOneAuthorityAcrossProfiles
invalidAndThrowingKeysAreUnavailablePerCall
missingEvidenceStateReaderOrKeysFailClosedWithoutProductionAdapters
persistenceOnlyConsumersShareSingleAuthority
persistenceStateRequiresExplicitAdmissionEnablementAndQualifiedResources
secondVerifierStillFailsClosedWithoutFallback
singleAuthorityEvidenceStateAndReaderAreUsedWithoutReplacement
staleTencentMetadataDoesNotSuppressExplicitFileProvider
tencentAndCustomProvidersAreAmbiguousBeforeAnyRead
tencentMissingMetadataIsUnavailableWithoutNetwork
tencentSharesSoleAuthorityAcrossProfiles
tencentSourceRequiresExactExplicitSelection
unsupportedSourceAndInvalidFileKeepUnavailable
zeroAndAmbiguousKeysDoNotFailStartup
```

`com.onecall.archtests.OneCallApplicationTests` — 1:

```text
contextLoadsWithoutDatabaseOrSecurityAutoConfiguration
```

`com.onecall.archtests.OperationsLifecycleConfigurationTests` — 8:

```text
configurationAndStartupFailuresAreRedacted
defaultWebAndDisabledProfilesCreateNoLifecyclePool
employeeCollaboratorIsOperationsOwnedAndUsesExactQualifiedManagerAndDsl
explicitEnablementRequiresDedicatedNonblankCredentials
missingAndAmbiguousOwnerCollaboratorsFailClosed
ownerBeansUseExactQualifiersAndOneDatasource
webBoundaryUsesIndependentVerifierAndExactFilterOrder
writeTemplateUsesRequiresNewReadCommittedAndRollbackOnCommitFailure
```

`com.onecall.archtests.OperationsSubjectsWebTests` — 12:

```text
authenticationPrecedesMalformedInputAndMediaNegotiation
closedBodyRejectsUnknownDuplicateMissingAndCoercedFields
concurrentRequestsKeepCallerActionAndReasonIsolated
exactTwoPostsReturnOnlyTenFrozenFields
idempotencyHeaderBoundsAndExactOpaqueTextArePreserved
operationsAndCustomerFiltersNeverCrossAuthenticate
problemsAndResponsesNeverLeakSensitiveCanaries
reasonAndRevisionBoundsPreserveFrozenInputSemantics
successReplayAndConflictHaveIdenticalFrozenProjectionRules
uuidAndContentNegotiationUseFrozenProblemSet
wrongMethodAndNoncanonicalRoutesAreConcealed
wrongRoleAudienceAndUnavailableDependenciesMapExactly
```

`com.onecall.archtests.PublicSharePersistenceConfigurationTests` — 2:

```text
defaultWebAndDisabledCommandProfilesStayDatabaseFree
publicReadUsesIsolatedQualifiedManagerAndEnforcedReadOnlyTemplate
```

`com.onecall.archtests.PublicShareWebTests` — 12:

```text
exactPublicGetRoutePrecedesOptionalAuthentication
mediaAuthorizationMapsFrozenDeniedStatusesAndPrecedence
mediaAuthorizationRejectsClosedBodyAndInvalidPurposeWith422
mediaAuthorizationReplayAndConflictUseFrozenProblemSurface
mediaAuthorizationResponseAndProblemNeverLeakInternalMediaFacts
mediaAuthorizationUsesExactRouteAndReturnsOnlyCredentialAndExpiresAt
memberSessionMatchAndNonmatchNeverLeakContent
optionalBearerFailuresKeepFrozenPrecedence
organizationMemberMediaAuthorizationRequiresTrustedCurrentSession
prefetchAndNegotiationHaveNoAuthorizationSideEffects
publicDtoIsClosedAndContentIsOmittedForNonmembers
tokenShapeAndLookupFailuresUseClosedProblems
```

`com.onecall.archtests.SharingCommandPersistenceConfigurationTests` — 2:

```text
commandProfileUsesDistinctPoolAndResetsLocalRole
defaultAndWebProfilesCreateNoCommandDatasource
```

`com.onecall.archtests.SharingPersistenceConfigurationTests` — 2:

```text
defaultProfileCreatesNoPersistenceBeans
persistenceRequiresExplicitQueryCredentials
```

`com.onecall.archtests.SubjectClaimPersistenceConfigurationTests` — 8:

```text
commandBeansUseExactQualifiersAndOneDedicatedDatasource
customerComposerAndControllerUsePublishedOwnerProviderOnly
datasourceAndProviderDiagnosticsNeverExposeCredentialsOrReferences
defaultProvidersAreUnavailableAndControlledReplacementsAreUnique
defaultWebAndDisabledProfilesCreateNoCommandPool
explicitEnablementRequiresAllDedicatedCredentials
roleAssertionFailureNeverFallsBackToAnotherPrincipal
writeTemplateUsesReadCommittedRequiresNewAndRollbackCleanup
```

`com.onecall.archtests.activation.CustomerSubjectActivationServiceTests` — 18:

```text
acceptCreatesOneRevisionTransitionAndIssuesTrialAtomically
canonicalFingerprintsAndSourceIdentitiesMatchFrozenVectors
currentPolicyPromptAndContentMustMatchExactly
declineCommitsRefusalBeforeExceptionMapping
exactAcceptReplayReturnsFirstFrozenResultWithoutWrites
exactDeclineReplayReturnsSameRefusalWithoutWrites
laterActiveEventFreezesLatestAnchorAndOriginalTrial
lifecycleFailurePrecedesNonDisclosingSubjectLookup
missingAnchorTrialOrCorruptReplayDescriptorFailsInternal
missingAndForeignSubjectsReturnNotFoundWithoutEffects
organizationAccessRequiresActiveWorkspaceMembershipAndAllowedRole
personalAccessRequiresActiveAccountAndMatchingOwner
retryableFailuresRestartWholeAttemptAndExhaustAtThree
roleAndLockOrderIsExactBeforeRereadsAndWrites
sameEventSemanticOrContentMismatchReturnsConsentRequired
suspendedRejectsNewAcceptButNeverMutatesOnHistoricalReplay
trialOutboxAndCommitFailuresRespectExactDecisionWriteSets
unknownFailuresAndDomainStringsNeverEscapePublicProblems
```

`com.onecall.archtests.activation.OperationsSubjectLifecycleServiceTests` — 18:

```text
auditAndOutboxMappingsAreExactAndContainNoSecrets
auditOutboxAndTransitionFailuresRollBackWholeDecision
changedActorSubjectRouteReasonOrExpectedRevisionConflicts
conditionalUpdateMissRereadsOnceAndConflicts
currentActiveOpsIsRequiredBeforeExistenceOrReplay
exactActionReplayReturnsOriginalBeforeCurrentPredicate
frozenFingerprintHasExactKeysNullsAndCanonicalUtf8
lockOrderAndOwnerAssertionsPrecedeEveryLocalWrite
maximumActionIdentityIsStoredWithoutTruncation
missingSubjectOrActivationReturnsNotFoundWithoutCreation
replayAfterLaterTransitionReturnsFirstImmutableResult
resumeAdvancesOnlySuspendedHeadAndAppendsOneFactSet
resumeHasNoTrialConsentOrForeignMutationCollaborator
statusIdentityUsesActivationAndNextRevisionWithoutOverflow
suspendAdvancesOnlyActiveHeadAndAppendsOneFactSet
uniqueAndTechnicalFailuresMapOnlyAfterTransactionCleanup
unusableHeadTransitionOrEmployeeContextFailsClosed
wrongStateAndStaleRevisionConflictWithoutWrites
```

`com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests` — 1:

```text
approvedPersistenceFrameworksAndForbiddenStacksAreExact
```

`com.onecall.archtests.architecture.CustomerApiBoundaryTests` — 3:

```text
customerApiSignaturesAndProjectionAreExact
customerDependenciesAndReadOnlySqlStayInsideBoundary
profileAndVerifierArtifactsCannotEscapeIntoProduction
```

`com.onecall.archtests.architecture.CustomerBusinessNumberBoundaryTests` — 4:

```text
controllerSqlAndProjectionStayInsideTheirSemanticOwners
customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation
subjectAndActivationExposeOnlyTheTwoOwnedNamedQueryInterfaces
v015AndPa25DeltasAreReadOnlyAndFrozenInputsRemainByteIdentical
```

`com.onecall.archtests.architecture.CustomerEvidenceBoundaryTests` — 4:

```text
customerCompositionAndOwnerSqlRespectMinimumReadBoundaries
namedOwnerInterfacesAndTenCustomerDependenciesAreExact
openApiAndV001ThroughV016RemainFrozenWithoutContractExpansion
v017AddsOnlyNineteenColumnsAndPa25Uses288AndSeventeen
```

`com.onecall.archtests.architecture.CustomerSubjectActivationBoundaryTests` — 6:

```text
activationSqlNeverReadsEntitlementTablesOrLocksForeignRows
activationUsesOnlyPublishedEntitlementOwnerInterface
commandSurfaceHasNoExtraEndpointPolicySourceOrStateTransition
customerUsesOnlyPublishedActivationCommandInterface
openApiDataDocsAndAllSeventeenMigrationsRemainFrozen
ownerHandoffHasMandatorySameConnectionAndLiteralRoles
```

`com.onecall.archtests.architecture.CustomerSubjectClaimBoundaryTests` — 6:

```text
controllerBinderAndProblemSurfaceMatchFrozenOperation
customerCompositionHasNoSqlOrClientAuthorityEscape
namedCommandExportsOnlyFrozenOwnerTypesAndPersonalCaller
openApiMigrationsAndProductionProviderAbsenceRemainFrozen
ownerSqlUsesOnlyApprovedColumnsLocksAndAuditFunction
singleAuthoritativeTransactionAndLocalSpisPreserveBoundary
```

`com.onecall.archtests.architecture.CustomerSubjectDetailBoundaryTests` — 4:

```text
customerCompositionContainsNoForeignSqlOrInternalImports
openApiAndV001ThroughV015RemainFrozenAcrossNewlineRepresentations
subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces
v016AddsOnlyTheEightRequiredPrivilegeTuplesAndPa25Uses269AndSixteen
```

`com.onecall.archtests.architecture.ModuleBoundaryTests` — 9:

```text
assertsExactAllowedDependencyMapFromPlan62
discoveredProductionEdgesStayInsideTheConfiguredMap
discoversExactlyTheFrozenModuleSet
everyExpectedModuleDeclaresApplicationModule
evidenceMediaReadIsNamedAndInternalImplementationCannotBeImported
rejectsCycleFixture
rejectsForbiddenDependencyFixture
rejectsInternalPackageAccessFixture
verifiesNoCyclesOrBoundaryViolations
```

`com.onecall.archtests.architecture.OperationsLifecycleBoundaryTests` — 8:

```text
activationImportsOnlyPublishedOperationsInterfaces
auditOutboxFingerprintAndResponseFieldSetsAreExact
employeeOwnerSqlIsLockOnlyAndUsesMandatoryLiteralRoles
employeeSessionIsIndependentAndHasNoProductionVerifierImplementation
frozenOpenApiMigrationsAndPolicyBytesAreUnchanged
lifecycleSqlHasNoForeignBaseWritesOrTrialCalls
operationsPortsAndNamedInterfacesAreExactAndAcyclic
routesBinderProblemsAndCustomerPassThroughStayExact
```

`com.onecall.archtests.architecture.SharingQueryBoundaryTests` — 8:

```text
gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges
gatewayUsesExactOwnedFilesAndEvidenceNamedInterface
keySourceUsesOnlyLocalReadAndExistingProviderBoundary
mediaAuthorizationSurfacePreservesNamedInterfaceAndNoForeignWriteBoundary
playbackStateAndOpenPreserveReadOnlyNoProjectionBoundary
playbackUsesOnlyNamedEvidenceReadAndPrivateTypedScope
sharingQueryApiAndProjectionAreExact
tencentKeySourceConfinesSdkAndPreservesExistingAuthority
```

`com.onecall.archtests.entitlement.ActivationTrialServiceTests` — 8:

```text
freshIssueLoadsStoredTrialGrantAndBalanceByReturnedIds
incompleteOrInconsistentTrialFactsFailWithoutSynthesis
mandatoryOwnerScopeRequiresExistingActivationTransaction
missingDisabledUnpublishedAndMalformedPoliciesFailClosed
ownerSwitchesAssertsAndRestoresExactRoles
policyVersionUsesSamePositiveDecimalVersionIdentity
replayAndAlreadyGrantedReturnOriginalImmutableFacts
snapshotRequiresExactRootConsentMetadataWithoutFallback
```

`com.onecall.archtests.evidence.ControlledMediaByteSourceTests` — 4:

```text
byteIntervalsAndOpenRequestsRejectUnboundedOrNullScope
byteRequestsSessionsAndFailuresHaveRedactedRepresentations
deterministicSourceBindsObjectHashSizeAndExactInterval
sessionsCloseAbortAndRejectReadsAfterTerminalState
```

`com.onecall.archtests.evidence.EvidenceMediaObjectReaderTests` — 4:

```text
incompleteInconsistentAndInvalidExistingFactsNeverBecomeMissing
nullInputPrecedesDatabaseAccessAndSqlAndTransactionFailuresAreOpaque
publicApiSerializationValidationAndStringFormsExposeOnlySafeMetadata
validRowsResolveSafelyAndZeroOrMultipleRowsHaveDistinctResults
```

`com.onecall.archtests.sharing.CustomerShareCommandServiceTests` — 6:

```text
createBuildsDeterministicIdentityAndFingerprint
createChecksRiskAndPolicyBeforeReplay
createRejectsChangedFingerprint
createReplaysMetadataWithoutSecrets
revokeReplaysBeforeStatePreconditions
rotateReplayNeverReturnsCode
```

`com.onecall.archtests.sharing.MediaPlaybackCredentialAuthorityTests` — 24:

```text
advancingClockReplayReturnsFirstCredential
authorizedFactsRequireFullWindowAndValidMetadata
carriersResultsKeysAndFailuresAreRedacted
claimShapeAudienceAndTimeBoundsFailClosed
disabledAbsentAndUnknownKeysFailClosed
freshAuthorityReconstructsCommittedAnchor
freshIssuanceUsesOnlyActiveKey
goldenPlaybackRoundTripsExactClaim
goldenRenewRoundTripsExactClaim
goldenSeekRoundTripsExactClaim
invalidKeySnapshotsAndProviderFailuresAreUnavailable
issuanceUsesOnlyWitnessExpiryBound
keyMaterialAndOverlapListsAreDefensivelyCopied
legacyP2IssueRemainsUnavailableWithValidKeys
malformedLegacyAndMissingAnchorsNeverMintReplacement
nonMicrosecondClocksNormalizeDeterministically
purposeEventAndRequestIdentityValidationFailClosed
replayCannotSwitchRecordedKidOrExtendExpiry
tamperAcrossEverySignedFieldIsRejected
truncationTrailingLengthsAndOverflowAreRejected
utf8Base64AndUnknownEnumsAreRejected
verifyOnlyOverlapVerifiesAndReplaysRecordedKey
wireEnvelopeAndDomainSeparationAreExact
zeroAndAmbiguousProvidersAreUnavailableEvenWithPrimary
```

`com.onecall.archtests.sharing.MediaPlaybackServiceTests` — 21:

```text
ambientTransactionsAndPostOpenDependencyIoAreForbidden
audienceIsExactAndCannotBeReplacedByTrustedNetworkContext
credentialsClaimsScopesHandlesOutcomesAndFailuresAreRedacted
crossShareAdmissionAndEvidenceStateCannotReachControlledOpen
evidenceMetadataCannotBeOverriddenByCallerOrClaim
exclusiveExpiryAndNotBeforeBoundariesAreDeterministic
expiryCrossingDuringResolutionPreventsOpen
invalidDurationAndReadWindowsAreNeverClamped
invalidUnavailableNullAndThrowingVerifierFailClosed
malformedOpaqueCredentialStopsBeforeVerifier
malformedVerifiedClaimStopsBeforeCurrentState
missingEvidenceDeniesWithoutControlledOpen
purposeMismatchPrecedesTimeAndStateChecks
readerCannotWidenOrMisbindReturnedHandle
readerUnavailableNullAndThrowingResultsReturnNoHandle
resolvedObjectIdentityAndDurationMustMatchVerifiedScope
seekAndRenewRequireExactPurposeAndNeverIssueOrExtendCredentials
stateDenialsUnavailableAndMalformedResultsPreventEvidenceLookup
unavailableThrowingAndAmbiguousEvidenceNeverFallsBack
validInDurationSubWindowPassesShapeAndTypedOpen
validPlaybackResolvesVerifiedEvidenceAndOpensExactlyBoundedHandle
```

`com.onecall.archtests.sharing.PublicShareQueryServiceTests` — 4:

```text
exactUtf8DigestLookupConcealsUnknownTokens
immutableEvidenceProjectionIsOrderedAndFailClosed
organizationContentRequiresCurrentMatchingMember
stateAndDatabaseTimeChecksNeverMutateGrant
```

`com.onecall.archtests.sharing.ShareAdmissionServiceTests` — 26:

```text
activeAndAdmittedLockedAuthorizeWithoutAdmissionMutation
canonicalAdmissionKeyUsesExactUtf8AndNoRawRequestId
changedMediaFactsConflictBeforeReplayProvider
closedAdmissionBinderRejectsUnknownDuplicateTrailingAndWrongBranchFields
concurrentAnchorsConvergeOnFirstCommittedCredential
concurrentSameKeyCallsConvergeAfterTransactionClosureAndCredentialNeverEntersEvent
evidenceResolutionFailuresNeverInvokeProvider
existingAdmissionReplaysBeforeLimitWithoutVerificationOrRecount
expiredExistingAdmissionNeverRenews
legacyOrUnavailableReplayMetadataNeverIssuesReplacement
mediaAuthorizationBinderAcceptsOnlyFrozenFieldsAndPurpose
mediaAuthorizationIdentityUsesExactUtf8DigestAndPurposeEventMapping
organizationMemberReauthenticatesWitnessBeforeGrantRead
organizationMemberRequiresTrustedCurrentSessionOnly
policyAndProviderAbsenceFailClosedWithoutWrites
problemCodesPreserveFrozenStatusPrecedence
providerFailureAppendsOnlyDeterministicFailureAndRetrySucceeds
resolvedMediaFactsBoundBeforeProviderAndCommittedExactly
responseProjectionContainsOnlyFrozenPublicFields
riskBindingGenerationAndTimeDenialsFailClosedWithoutMutation
sameAuthorizationRequestAcrossPurposesConflictsWithoutSecondCredentialOrEvent
sameAuthorizationRequestAndPurposeReplaysCredentialAndEvent
ticketFailureIsRedactedAndSameKeyReconstructs
ticketMaterialIsValidatedAndCredentialFailureClaimsNoSuccess
verificationModesMapToFrozenResultsAndSecretsNeverEnterViews
wrongTenantAndRiskFailuresCannotProduceSuccess
```

`com.onecall.archtests.sharing.SharingPlaybackStateReaderTests` — 8:

```text
activeAndAdmittedLockedStateAllowWithoutMutation
grantAdmissionAndEvidenceTupleMustMatchVerifiedIdentity
organizationMemberWitnessRequiresCurrentAccountTenantAndMembership
ownershipCapturedBindingAndCurrentRiskFailClosed
readOnlyRepeatableSnapshotClosesBeforeReturningAndOriginalTemplateIsUnchanged
repositoryHashAndTransactionFaultsAreUnavailableAndRedacted
revokedExpiredNotYetValidAndBothStaleGenerationsDeny
witnessShapeHashTypesAndBoundsMustBeCanonical
```

`com.onecall.archtests.sharing.SingleByteRangeTests` — 6:

```text
absentRangeSelectsExactlyFullPositiveObject
closedRangesClipOnlyTheirEndWithoutOverflow
malformedUnitsWhitespaceAndMultipleRangesAreRejected
normalizationMatchesDeterministicSmallObjectOracle
openEndedAndSuffixRangesResolveOneBoundedInterval
reversedAndOverflowDecimalBoundsNeverWrapOrClamp
```

`com.onecall.archtests.subjectnumber.CustomerSubjectClaimServiceTests` — 17:

```text
changedAcceptedContextAppendsExactlyOneConflict
changedRequestIdentityNeverDisclosesStoredOutcome
corruptAcceptedOwnerRelationAndUpdateMissFailClosed
currentPredicateFailuresPersistOnlyResolvableFreshRejections
differentOwnerRefusesWithoutTransferOrRepair
exactAcceptedDuplicateReturnsFirstResultWithoutInsert
exactAuditMappingsAndFailuresNeverLeakOrPartiallyCommit
exactRequestReplayUsesLogicalIdentityAndFirstOutcome
freshAcceptanceSetsOnlyNullOwnerWithExpectedRevision
locksUseFrozenNamespacesAndCurrentRereads
openRiskPreemptsEveryReplayAndDuplicateOutcome
personalCallerAndCurrentActiveAccountAreMandatory
proofApprovalMustBindEveryTrustedDimension
riskTimesAndAcceptedTimesComeFromDatabaseDecisionClock
unavailableDefaultProofAndCipherNeverAccept
uniqueLoserRetriesOnlyAfterWholeTransactionCleanup
zeroInitialBindingStopsBeforeLocksAndWrites
```

`com.onecall.archtests.subjectnumber.SubjectClaimVerificationReferenceTests` — 8:

```text
randomizedCiphertextsCompareByDecodedLogicalIdentity
rowSubjectMethodAndBindingHmacMustMatchPayload
sensitiveValuesAndProviderFailuresHaveRedactedRepresentations
serverResolvedProofReferenceNotClientTextDefinesIdentity
strictPayloadRejectsMissingExtraDuplicateAndMalformedValues
undecryptableAndUnavailableReferencesRemainDistinct
v1PayloadHasExactCanonicalKeysAndNoAccountOrRawProof
wrongVersionAndInvalidEnvelopeFailClosed
```

`com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryServiceTests` — 8:

```text
allBindingActivationAndRealNameStatesComposeWithoutFiltering
defaultBoundsAndLookaheadRequestAreExact
emptySingleAndMultiPageProjectionIsExact
lookaheadUsesLastReturnedRowAndNullCursorOnTail
missingActivationDefaultsToInactiveAndForeignProjectionFailsClosed
technicalAndProjectionFailuresReturnNoPartialOrSensitivePage
trustedWorkspaceAndCurrentLifecycleAreRequiredBeforeReaders
webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact
```

`com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests` — 6:

```text
activeContextHasExactPersonalAndOrganizationProjection
invalidProjectionNeverReturnsPartialContext
missingAndDisabledAccountsAreUnauthenticated
roleAssertionPrecedesRepositoryAndTransactionIsReadOnly
technicalAndProgrammingFailuresAreSanitized
trustedCallerIsTheOnlyAccountAndChannelSource
```

`com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryServiceTests` — 12:

```text
allFourStatusesAndImmutableCaptureFactsComposeWithoutLiveIdentity
artifactRolesAndSafeFactsUseFixedRankAndSourceValidatedDuration
detailMissingUnownedAndForeignEvidenceNeverReadsRiskOrArtifacts
emptyAndTailPagesHaveNoCursorAndBatchOnlyReturnedArtifactRoles
listUsesOwnedEvidenceSubjectsAndChecksAllTheirBindingsBeforePage
nullExtraDuplicateMisboundAndNonmonotonicOwnerResultsFailClosed
oneReadOnlyRepeatableNewTransactionContainsCompositionWithoutWrites
pageLookaheadAndDescendingUuidTiesUseLastReturnedAnchor
requestsRequireTrustedScopeAndExactLimitAndAnchorBounds
roleAssumptionAssertionAndLifecyclePrecedeEveryOwnerRead
sqlBeanTransactionAndCompletionFailuresNeverReturnPartialData
unrepresentablePersistedDetailIsUnavailableAndInvalidListFactsAreInternal
```

`com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryServiceTests` — 12:

```text
inactiveContextPrecedesSubjectAndRiskReads
missingActivationDefaultsInactiveAndPresentStatesAreExact
missingOwnedSubjectIsEmptyAndInvalidProjectionIsInternal
nullCallerWorkspaceAndSubjectFailClosedBeforeQueries
nullExtraUnknownAndMisboundActivationProjectionsAreInternal
oneReadOnlyRepeatableTransactionContainsOrderedOwnerReadsAndNoWrites
openRiskPrecedesActivationAndBothCounts
personalAndOrganizationScopesUseOnlyVerifiedCaller
roleAssumptionOrAssertionFailureIsUnavailableWithoutOwnerReads
roleIsAssumedAndAssertedBeforeLifecycleAndSubjectQueries
sqlTransactionAndBeanFailuresAreRedactedAndUnknownFaultsAreInternal
zeroAndLongCountsAreExactAndNegativeCountsFailClosed
```

`com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests` — 7:

```text
attestedWorkspaceIsCarriedWithoutClientAuthority
invalidRevokedExpiredDisabledAndWrongAudienceSessionsHaveSameDenialShape
malformedVerifierResultsFailClosed
missingAmbiguousAndUnavailableVerifiersFailClosed
secretWrappersAndPrincipalNeverRetainOrExposeBearer
verifierReceivesOnlyOpaqueBearerAndCustomerAudience
workspaceStructureIsCarriedWithoutChangingAuthentication
```

`com.onecall.core.customer_iam.internal.web.CustomerBusinessNumberCursorCodecTests` — 8:

```text
accountWorkspaceKindAndOrganizationReplayAreRejected
everyPayloadOrMacByteMutationIsRejected
invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected
keyReplacementInvalidatesOldCursorAndLimitDoesNotBindScope
missingMalformedAndWrongLengthKeysAreUnavailableWithoutDisclosure
payloadVersionLengthDomainAndMicrosecondAnchorAreExact
personalAndOrganizationRoundTripsAreCanonicalAndDeterministic
truncatedExtendedUnknownVersionAndInvalidTimestampAreRejected
```

`com.onecall.core.customer_iam.internal.web.CustomerEvidenceCursorCodecTests` — 8:

```text
accountWorkspaceKindOrganizationAndOtherEndpointReplayAreRejected
everyPayloadBindingAndMacByteMutationIsRejected
invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected
keyReplacementInvalidatesOldCursorAndLimitIsNotBound
missingMalformedWrongLengthAndNoncanonicalKeysAreUnavailable
payloadVersionLengthDomainAndMicrosecondPrecisionAreExact
personalAndOrganizationAnchorsRoundTripCanonically
truncatedExtendedUnknownVersionAndInvalidTimesAreRejected
```

`com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests` — 6:

```text
anchorBoundsMatchPa10Validation
cursorBindingRejectsOtherAccountsAndWorkspaces
malformedAndNonCanonicalCursorsAreRejected
maximumUnicodeShareIdsFitFrozenWireBound
personalAndOrganizationCursorsRoundTripCanonicalAnchors
wirePageHasOnlyFrozenFieldsAndNullableCursor
```

`com.onecall.core.operations_iam.internal.security.EmployeeSessionBoundaryTests` — 8:

```text
absentAndAmbiguousVerifierFailClosed
concurrentSessionsNeverShareTrustedCallerState
customerTokensAndClientIdentityNeverAuthenticateEmployee
employeeAudienceIsPassedAndIndependentlyValidated
invalidExpiredAndRevokedSessionAreUnauthenticated
missingMalformedAndDuplicateBearerAreUnauthenticated
opaqueValuesAndProviderFailuresHaveRedactedRepresentations
unusableVerifierAndMalformedAttestationFailClosed
```

`com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests` — 8:

```text
creatorContextDenialDoesNotFetchShares
cursorBindingAndAnchorValidationAreDeterministic
defaultsAndBoundsAreDeterministic
emptyAndTailPagesHaveNoCursor
lookaheadUsesLastReturnedRow
projectionComponentsMatchFrozenListing
technicalFailuresReturnNoPartialPageOrSensitiveText
trustedContextCannotBeOverridden
```

`com.onecall.core.sharing.internal.keysource.FileMediaPlaybackSigningKeyProviderTests` — 10:

```text
mapsReadPermissionAndIoFailuresToUnavailable
neverSearchesFallbackLocations
readsOnlyConfiguredAbsoluteRegularFile
redactsPathMaterialAndFailures
rejectsEmptyAndOversizedSourcesBeforeParsing
rejectsMissingBlankRelativeAndInvalidPaths
rejectsMissingDirectoriesAndSymlinkSources
removalAndCorruptionImmediatelyFailClosed
rereadsReplacementWithoutCachedFallback
returnsIndependentImmutableSnapshots
```

`com.onecall.core.sharing.internal.keysource.MediaPlaybackFileKeySourceIntegrationTests` — 8:

```text
badBundlePrecedesTokenParsingAndDownstreamReads
bundleFailuresAndCredentialsNeverLeak
equalMaterialAcrossDistinctKidsDoesNotChangeRecordedKid
fileBackedClaimsStillRequireCurrentState
fileBundleIssuesOnlyActiveForEveryPurpose
overlapReplaysExactPreviouslyIssuedCredential
recordedKeyRemovalNeverMintsReplacement
rotationOverlapVerifiesOldAndNewCredentials
```

`com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests` — 12:

```text
copiesAndRedactsAllSuccessfulAndFailedResults
enforcesByteAndNestingBounds
rejectsInvalidOrDuplicateKids
rejectsInvalidUtf8AndBom
rejectsMissingInvalidAndDeferredRoles
rejectsNonCanonicalHexAndWrongDecodedLength
rejectsTrailingTokensAndNonJsonExtensions
rejectsUnknownMissingAndDuplicateFields
requiresExactSchemaAndRootShape
requiresExactlyOneActive
singleActiveMapsExactKey
zeroAndManyVerifyOnlyKeysHaveCanonicalOrder
```

`com.onecall.core.sharing.internal.keysource.MediaPlaybackTencentKeySourceIntegrationTests` — 10:

```text
equalMaterialNeverAliasesRecordedKid
explicitVersionRolloutVerifiesOldAndNewKeys
overlapReplaysExactCredentialForEveryPurpose
removedRecordedKidNeverMintsReplacement
sdkToAuthorityFlowNeverLeaksCanaries
tencentBundleIssuesActiveForEveryPurpose
tencentClaimsStillRequireCurrentState
transportAndParserFailuresHaveNoFallback
unavailableSourcePrecedesOpaqueTokenAccess
unavailableSourcePreventsStateEvidenceAndOpen
```

`com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProviderTests` — 18:

```text
duplicateKidsAndInvalidRolesStayUnavailable
eachCurrentKeysReadsExactlyOnce
invalidKeyMaterialStaysUnavailable
invalidMetadataAndTimeoutsNeverReadClient
missingRequiredMetadataNeverReadsClient
noFileOrAlternativeSourceFallbackExists
nullEmptyAndWhitespaceStringsAreUnavailable
ownedUtf8BuffersAreCleared
parserSyntaxAndSchemaRejectionsStayUnavailable
providerAndResultsRedactMaterialAndMetadata
readerFailuresAreUnavailableWithoutCauses
secretStringIsNeverBase64Decoded
snapshotsRemainIndependentAndDefensivelyCopied
sourceByteAndCharacterBoundsAreEnforced
successThenFailureNeverUsesLastGoodKeys
utf8EncodingRejectsUnpairedSurrogates
validSecretStringMapsExistingBundle
zeroAndManyVerifyOnlyKeysRemainCanonical
```

`com.onecall.core.sharing.internal.keysource.TencentSsmSecretStringClientTests` — 8:

```text
authCamAndResourceStateFailuresAreUnavailable
clientFailuresAndStringFormsNeverLeak
clientRegionHttpsTimeoutAndRetrySettingsAreExact
explicitNameAndVersionReachOneSdkRead
networkTlsAndTimeoutFailuresAreUnavailable
nullMissingBinaryAndMismatchedResponsesFailClosed
roleOnlyFactoryIsLazyAndHasNoStaticFallback
throttleUnknownAndServerFailuresAreUnavailable
```

### A.2. Failsafe: 27 classes / 440 retained methods

`com.onecall.archtests.container.ActivationCommandRoleLeastPrivilegeIT` — 10:

```text
activationRoleCannotReadAnyEntitlementBaseTable
activationRoleHasExistingSchemaUsageAndIssueExecuteOnly
activationRoleIsRestoredBeforeLocalWritesAndCompletion
entitlementOwnerReadsCompletePolicyAndOriginalTrialFacts
fixtureCleanupAndFrozenAclInventoryRemainExact
handoffUsesOneBackendPidAndOneTransactionId
mandatoryCallsOutsideTransactionAndUnexpectedRoleAreRejected
noForeignWritesRowLocksOrExtraPrivilegesAreNeeded
rollbackAndPoolReuseCannotLeakEntitlementRole
syntheticLoginHasOnlyTwoNonInheritedSetMemberships
```

`com.onecall.archtests.container.CustomerBusinessNumberReadPrivilegeIT` — 8:

```text
allSixLoginBoundariesAndRolePoolRestorationRemainUnchanged
customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501
pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle
v014CustomerCannotReadSubjectBindingOrActivationWith42501
v015AddsExactlyFourteenOwnerGrantedTuplesAndNothingElse
v015FreshAndUpgradeHaveExactFifteenHistoryEntriesAndFrozenBytes
v015RejectsPartialUnexpectedGrantorGrantOptionAndAuthorityDriftAtomically
v015ReplayPreservesAclsGraphCredentialsDefinitionsAndBusinessData
```

`com.onecall.archtests.container.CustomerBusinessNumbersIT` — 18:

```text
allBindingActivationMissingActivationAndRealNameCombinationsAreVisible
committedLifecycleChangesAreRecheckedForEveryPage
concurrentInsertAndDeletedAnchorFollowDocumentedKeysetSemantics
defaultOneAndHundredLimitsAndChangedContinuationLimitAreExact
emptySingleAndExactLimitPagesHaveExactTailSemantics
firstMiddleAndLastPagesEnumerateEveryBindingOnce
inactiveAndMissingAccountsAreForbiddenWithoutPageReads
inactiveMissingAndForeignOrganizationsOrMembershipsAreForbidden
lifecyclePageAndActivationShareOneRepeatableReadSnapshot
malformedTamperedAndCrossScopeCursorsCannotWidenResults
organizationScopeIncludesEveryActiveRoleAndOnlyOrganizationOwnedSubjects
personalScopeReturnsOnlyEveryOwnedBinding
postgres16FullChainAndCustomerRuntimeRoleAreUsed
responsesAndCapturedLogsNeverContainSecretOrOwnershipSentinels
roleIsolationReadOnlyAndAutoCommitRestoreOnPooledConnectionReuse
subjectCreatedAtDescendingAndNativeUuidAscendingTiesAreExact
successDenialAndFailurePerformZeroBusinessWrites
unownedForeignAndOtherSelectedWorkspaceSubjectsNeverAppear
```

`com.onecall.archtests.container.CustomerEvidenceIT` — 24:

```text
absentBindingsAndClearedOrAbsentRiskDoNotInventRestrictions
allFourActiveOrganizationRolesCanRead
allFourEvidenceStatesAreVisibleWithoutArtifactEligibilityGate
artifactRolesAndArtifactsUseRankHashSizeMimeOrder
capturedAtComesOnlyFromImmutableCaptureScalar
composedReadsShareOneSnapshotAndObserveLaterRevocationNextRequest
defaultBoundsEmptySingleAndTailPagesAreExact
descendingCreatedAtAndUuidKeysetSurvivesTiesAndNewInserts
detailRereadsCurrentStateOnEveryRequest
durationUsesOnlyUniqueMatchingValidatedSourceRecording
inactiveMissingAccountsOrganizationsAndMembershipsAreForbidden
listExcludesInaccessibleSubjectsBeforeSeekAndLookahead
listOffPageRiskBlocksButForeignAndNoEvidenceRiskDoNot
missingUnownedForeignAndOtherWorkspaceDetailsAreUniformNotFound
openRiskOnAnySubjectBindingIsForbidden
organizationListAndDetailUseSelectedOrganizationNotCreator
personalListAndDetailAreSubjectOwnerScoped
postgres16LatestChainAndVerifiedCustomerRoutesAreWired
readsLeaveAllBusinessRowsAndEventsByteEquivalent
roleReadOnlyIsolationRequiresNewQueryCountsAndPoolCleanupAreExact
summaryAndDetailHaveExactDistinctSchemaKeySets
tamperedCrossAccountWorkspaceAndEndpointCursorsNeverWidenScope
unrepresentableCaptureAndArtifactFactsFailClosedWithoutLeakage
uuidRouteMethodAndNonAuthoritativeInputPrecedenceIsExact
```

`com.onecall.archtests.container.CustomerEvidenceReadPrivilegeIT` — 10:

```text
customerRoleReadsOnlyApprovedFactsAndDeniesAllSensitiveCapabilitiesWith42501
pa25RequiresExactV017HistoryAndIndependent288OracleWithoutRepair
realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure
sixLoginsSevenEffectiveRolesAndFourMembershipEdgesRemainExact
v016DeniesEveryNineteenMissingColumnCapabilityWith42501
v017AddsExactlyNineteenOwnerIssuedTuplesAndMatchesIndependent288Oracle
v017FreshAndUpgradeHaveExactSeventeenHistoryAndFrozenPredecessors
v017RejectsAuthorityHistoryProvenanceAndCatalogDriftAtomically
v017RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically
v017ReplayPreservesRolesCredentialsDefinitionsAclsAndBusinessRows
```

`com.onecall.archtests.container.CustomerMeIT` — 10:

```text
committedLifecycleChangesAreObservedOnNextRequest
exactGrantsPermitProjectionAndRejectForeignColumnsAndLocks
onlyActiveMembershipsUnderActiveOrganizationsAreProjected
personalContextRequiresCurrentActiveAccount
requestsCannotSelectAnotherAccountOrganizationRoleOrChannel
successAndFailuresDoNotWriteAndRestorePoolState
verifierOutageAndDatabaseFailureExposeNoSecrets
webAndPa10PersistenceCoexistWithoutBeanOrRoleLeakage
webRuntimeReadsMigratedDatabaseUnderCustomerRole
workspaceOrderingIsUuidDeterministicAndPersonalFirst
```

`com.onecall.archtests.container.CustomerShareCommandsIT` — 18:

```text
commandRoleIsAssertedBeforeFirstCommandRead
createAppendFailureRollsBackGrantAuditAndOutbox
createConcurrentSameAndDifferentIdentities
createPersistsGrantAuditAndOutboxAtomicallyWithoutAccessEvent
createReplayReturnsMetadataAndDoesNotPersistOrReturnSecrets
createRiskAndPolicyChecksPrecedeReplay
evidenceProjectionUsesCommandTransactionConnection
queryRoleCannotAssumeCommandRole
revokeChangedReasonTerminalExpiryAndStaleWitnessConflict
revokeConcurrentActionsProduceOneTransition
revokeExactReplayAddsNoFactsOrRevision
revokeFailureAtEachFactAppendRollsBackAllEffects
revokeWritesRegisteredEventAuditAndOutboxAtomically
rotateConcurrentActionsProduceOneTransition
rotateFailureAtEachFactAppendRollsBackAllEffects
rotateReplayReturnsNoCodeOrCapabilityToken
rotateRequiresExactLockedStateAndGenerationWitness
rotateWritesRegisteredEventAuditAndOutboxAtomically
```

`com.onecall.archtests.container.CustomerSharesIT` — 8:

```text
authenticatedHttpListsPersonalAndOrganizationCreatorMetadata
committedAccountMembershipAndOrganizationChangesDenyNextPage
customerAndSharingConnectionsRemainSeparatelyOwned
existingAclsAndReadOnlyTransactionsForbidWritesAndForeignReads
httpCursorsCannotCrossTrustedAccountOrWorkspace
httpPaginationPreservesTiesAndConcurrentInsertOrder
missingRolesVerifierAndDatabaseFailuresStaySanitized
successfulAndFailedHttpPagesNeverWriteOrProjectSecrets
```

`com.onecall.archtests.container.CustomerSubjectActivationsIT` — 24:

```text
acceptReplayReturnsOriginalIdsOutcomeUnitsAndExpiry
changedPolicyNeverChangesPermanentTrialUnitsOrExpiry
concurrentDeclineReplayAndSemanticConflictNeverAccept
concurrentDifferentEventsShareOneTransitionAndPermanentTrial
concurrentSameEventAcceptsConvergeOnOneFrozenResult
declineCommitsOneRefusalAndReplaysSameProblem
existingInactiveHeadAdvancesExactlyOneRevision
foreignPersonalAndOrganizationSubjectsAreNotFound
historicalReplayAfterPolicyChangeOrSuspensionHasNoMutation
inactiveAccountIsForbiddenBeforeMissingSubjectDisclosure
inactiveOrganizationMembershipOrDisallowedRoleIsForbidden
laterActiveEventUsesLatestAnchorWithoutTransitionOrTrialDuplication
malformedContextAndMissingGraphAreServiceUnavailable
missingDisabledUnpublishedOrMalformedPolicyHasNoEffects
organizationAllAllowedRolesCanActivateAccessibleSubject
personalAcceptCommitsExactActivationTrialAndResult
policyDisableAroundFunctionSelectionHasFrozenLinearization
sameEventAcrossAccountsOrSubjectsFailsWithoutDisclosure
sameEventSemanticMismatchHasNoAdditionalEffects
stalePolicyPromptOrContentHasNoConsentOrGrant
storedResultsProblemsAuditOutboxAndLogsAreRedacted
suspendedCannotBeReactivatedByNewCustomerEvent
trialOutboxAndCommitFaultsRespectExactDecisionWriteSets
webAndMiniappUseTrustedChannelAndClosedRequest
```

`com.onecall.archtests.container.CustomerSubjectClaimsIT` — 24:

```text
auditFailureAndConditionalOwnerMissRollBackWithoutSecretLeakage
boundedWholeTransactionRetriesRestorePoolAfterFailure
changedDuplicateContextAppendsOneConflictAndNeverUpdatesOwner
changedReplayIdentityReturnsConflictWithoutStoredDisclosure
clearedRiskReevaluatesAndReplaysFirstStoredOutcome
concurrentExactKeyCallsHaveOneAcceptedWinner
concurrentSameAccountDuplicateAndChangedContextSerialize
corruptAcceptedOwnerRelationsFailWithoutRepair
crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict
exactDuplicateWithNewKeyReturnsFirstAcceptedWithoutInsert
exactReplayUsesFirstIdTimeAndRandomizedReferenceEquality
freshUnownedPersonalClaimCommitsOneOwnerAndAcceptedRow
identicalRejectedAndConflictReplayReturnsFrozenStoredShape
inactiveAccountAndOrganizationWorkspaceAreForbidden
inactiveAndUnverifiedBindingsPersistOnlyRejectedHistory
missingSubjectBindingAndBindingMismatchUseUniformValidation
openRiskPreemptsSameAccountDuplicateAndStoredReferenceErrors
openRiskRejectsFreshRequestAndPreemptsAcceptedReplay
otherAccountAndOrganizationOwnersAreNeverTransferred
postgres16FrozenSeventeenMigrationChainAndDedicatedLoginAreUsed
proofInvalidUnavailableAndUnresolvableReferencesFailClosed
riskBindingAndAccountWritersRespectBothSerialOrders
roleLocksReadsWritesAndAuditShareOneConnection
twoAccountsClaimingUnownedSubjectHaveOneOwnerWinner
```

`com.onecall.archtests.container.CustomerSubjectDetailReadPrivilegeIT` — 10:

```text
allSixLoginsAndSevenEffectiveRolesRetainExactMembershipBoundaries
customerRoleReadsOnlyApprovedColumnsAndDeniesAllOtherCapabilitiesWith42501
pa25RequiresExactV016HistoryAndIndependent269TupleOracleWithoutRepair
realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure
v015DeniesEveryMissingRiskEvidenceAndOrderCapabilityWith42501
v016AddsExactlyEightOwnerIssuedTuplesAndMatchesIndependent269Oracle
v016FreshAndUpgradeHaveExactSixteenHistoryEntriesAndFrozenPredecessors
v016RejectsAuthorityRoleGraphOwnershipSettingsPublicAndDefaultDriftAtomically
v016RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically
v016ReplayPreservesAclsRolesCredentialsDefinitionsAndBusinessRows
```

`com.onecall.archtests.container.CustomerSubjectsIT` — 20:

```text
allFourActiveOrganizationRolesReadOnlySelectedOrganizationSubjects
committedLifecycleAndRiskChangesAreRecheckedOnNextRequest
cursorlessUuidValidationAndIgnoredInputsPreserveExistingCustomerEndpoints
emptyAndMultipleCountsNeverMultiplyAcrossBindingsOrActivation
evidenceCountsEveryStatusForOnlyTheAccessibleSubject
lifecycleRiskActivationAndCountsShareOneRepeatableReadSnapshot
missingActivationDefaultsInactiveAndEveryStoredStatusIsReturned
missingAndInactiveAccountsAreForbiddenBeforeSubjectLookup
missingInactiveAndForeignSelectedOrganizationsAreForbidden
missingInactiveAndRemovedMembershipsAreForbiddenForEveryRole
missingUnownedForeignAccountAndForeignWorkspaceSubjectsAreUniformNotFound
noBindingsNoRiskAndClearedRiskDoNotInventDenial
openRiskOnAnyActiveDisabledOrRetiredBindingIsForbidden
organizationCountsIncludeOtherPurchasersButNotOtherWorkspaces
personalOrderCountsAllPaymentStatesOnlyInPersonalWorkspace
personalOwnerReadsExactlyFourRequiredFields
pooledRoleReadOnlyIsolationAutoCommitAndAmbientTransactionsRestore
postgres16LatestChainAndCustomerRuntimeRoleAreUsed
responsesProblemsAndCapturedLogsNeverDiscloseSecretsOrRowContents
successDenialAndDependencyFailurePerformZeroBusinessWrites
```

`com.onecall.archtests.container.DatabaseRuntimeAclRbacHardeningIT` — 12:

```text
assumedRolesAndPoolRestorationPreserveRuntimeBoundaries
customerContextUsesOnlyApprovedColumnsAcrossAllCurrentStates
customerRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject
evidenceRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject
ownerInvokerGuardsAndSharingWritesRemainOperational
sharingProjectionUsesOnlyApprovedEvidenceColumns
v014FreshChainHasExactHistoryAndImmutablePredecessors
v014IsPrivilegeOnlyAndLeavesUnrelatedBoundariesEquivalent
v014RejectsPublicDefaultAndUnexpectedAclDriftAtomically
v014RejectsUnsafeRolesMembershipsOwnershipAndSettingsAtomically
v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects
v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle
```

`com.onecall.archtests.container.EvidenceMediaReadIT` — 8:

```text
connectionAndPermissionFaultsReturnOnlyUnavailable
corruptedMetadataIsUnavailableAndOptionalDurationIsSupported
dedicatedQualifiedReaderAuthenticatesAndResolvesInOneReadOnlyQuery
duplicateOriginalArtifactsAreUnavailableWithoutLimitOrAggregation
mismatchedRecordingReferenceMediaAndDerivedArtifactFailClosed
missingOriginalMediaAndRecordingRemainUnavailable
onlyAbsentEvidenceReturnsMissing
pendingQuarantinedAndUnvalidatedFactsAreUnavailable
```

`com.onecall.archtests.container.EvidenceMediaReadRoleLeastPrivilegeIT` — 8:

```text
actualLoginDeniesAdjacentReadsWritesDdlSequencesFunctionsAndEscalation
actualLoginSelectsEveryGrantedColumnAndResolvesMatchingObject
exactSixteenDependenciesAndOwnerGrantedColumnSchemaAclsAreEnumerated
failedDuplicateIncompatibleOrMissingRequiredHistoryIsRejected
incompatibleAttributesMembershipsSettingsAndAnyUnexpectedDependencyFailClosed
privilegeOnlyMigrationIsAtomicAndV001ThroughV012RemainByteIdentical
roleAttributesPasswordTopologyAndV013HistoryAreExact
zeroAclCompatibleRoleAndTwiceReplayedGrantedStateAreAccepted
```

`com.onecall.archtests.container.MediaGatewayTransportIT` — 8:

```text
byteOpenReadAbortAndCloseHoldNoDatabaseTransactionOrLock
committedRevocationRotationAndRiskDenyNextByteRequest
realAuthorityIssuedCredentialsReachHttpFullAndRange
realAuthorityTamperScopeAndWindowFailuresNeverReadBytes
realHttpAndLogsRedactSecretsAndLeaveExistingPublicFlowUnchanged
realHttpFullAndRangeUseQualifiedAuthorizationThenExactBytes
realHttpMalformedUnsatisfiableAndDeniedRequestsNeverReadBytes
transferFaultsReleaseOneSessionAndPreserveDurableSnapshots
```

`com.onecall.archtests.container.MediaPlaybackIT` — 9:

```text
committedRevocationRotationAndAdmissionMismatchDenyBeforeEvidence
missingAmbiguousAndInconsistentEvidenceNeverOpensControlledRead
qualifiedAdmissionAndEvidenceRolesResolveOneObjectAndOpenOutsideTransactions
readFailuresPreserveAdmissionRevisionCountsAndAccessEvents
realAuthorityEvidenceMismatchAndUnavailableKeysNeverOpen
realAuthorityRoundTripsAllPurposesThroughCurrentState
realAuthorityStaleAndCrossObjectClaimsNeverOpen
typedOpenHasNoPostOpenDatabaseIoAndNeverDisclosesPrivateFacts
witnessExpiryMembershipBindingAndRiskChangesFailClosedWithoutWrites
```

`com.onecall.archtests.container.OperationsLifecycleRoleLeastPrivilegeIT` — 12:

```text
activationRoleCannotReadOrLockEmployeeTable
activationRoleIsRestoredBeforeEveryOwnerWrite
auditUsesOnlyExistingFunctionCapabilityAndRestoresRole
employeeOwnerLocksOnlyItsOwnRowWithoutWrites
failedRestorationRollsBackAndPoolReuseCannotLeakRole
failureCleanupLeavesNoTransactionRoleLockOrPartialFact
frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact
handoffUsesOneBackendPidSessionAndTransactionId
locksFollowSubjectActivationAdministratorEmployeeOutboxAuditOrder
mandatoryCallOutsideTransactionOrWrongEntryRoleIsRejected
ownerAndAuditRolesCannotWriteForeignBaseTables
runtimeLoginHasOnlyThreeFixtureMembershipsAndNoPrivilegeRepair
```

`com.onecall.archtests.container.OperationsSubjectLifecycleIT` — 24:

```text
absentAmbiguousAndUnusableVerifierFailClosed
activationAndCustomerCommandRacePreservesOneAuthoritativeHead
auditFailureRollsBackHeadTransitionAndOutbox
changedActorSubjectAndRouteUnderSameKeyConflict
changedReasonAndExpectedRevisionConflictWithoutDuplicateFacts
concurrentCrossSubjectSameKeyHasOneWinnerAndOneConflict
concurrentDistinctActionsAtOneRevisionHaveOneWinner
concurrentSameResumeActionConvergesOnOneFactSet
concurrentSameSuspendActionConvergesOnOneFactSet
employeeRevocationSerializesWithLockedCurrentAuthority
historicalReplayAfterOppositeTransitionReturnsOriginalResult
inactiveAndWrongCurrentStatesConflict
maximumActionAndReasonRoundTripWithoutTruncation
missingDisabledAndNonOpsEmployeesCannotMutateOrReplay
missingSubjectAndMissingActivationAreNotFound
responsesProblemsAndEventsRespectSensitiveOutputBoundary
resumeAndSuspendNeverChangeTrialConsentOrOtherBusinessFacts
resumeSuspendedSubjectCommitsOneRevisionTransitionOutboxAndAudit
sameResumeActionReplaysOriginalWithoutAnyWrite
sameSuspendActionReplaysOriginalWithoutAnyWrite
staleAndUnrepresentableExpectedRevisionsConflict
suspendActiveSubjectCommitsOneRevisionTransitionOutboxAndAudit
transitionOutboxAndCommitFailuresCannotPartiallySucceed
wrongAudienceAndCustomerCredentialNeverReachOwnerWrites
```

`com.onecall.archtests.container.PostgresContainerIT` — 64:

```text
appendOnlyClaimAndForeignKeyBackstops
auditIsAppendOnlyAndRuntimeWritesAreDenied
auditReasonAndDenylistRulesAreEnforced
bindingLifecycleTerminalityAndActiveNumberUniqueness
brokenMigrationFailsVisibly
committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow
concurrentActiveBindingUniquenessHasOneWinner
concurrentCatalogPublicationAndCommerceIdentityHasOneWinner
concurrentIdempotencyRetryObservesOneWinner
concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner
concurrentV005TaskClaimAndActiveCallHasOneWinner
concurrentV006FirstTrialAndReservationIdentityHasOneWinner
concurrentV007ActivationAndTransitionIdentityHasOneWinner
concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner
concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner
functionsHaveFixedSearchPathAndNoTransactionControl
hostileSearchPathCannotRedirectFunctionEffects
idempotencyNewReplayConflictAndFinalizeOnce
identityCollisionAndMembershipRevokeRegrantSemantics
lastEnabledAdminTransactionIsSerializedAndFailsClosed
localImmutabilityGuardsRejectRelationAndRotationChanges
mutableHeadsEnforceLifecycleRevisionAndNamedChecks
outboxReplayConflictRollbackAndDenylist
ownerPublicAndRoleExecutionBoundariesAreExact
riskOpenClearReopenRevisionProtocol
runtimeAndCrossModulePrivilegesAreExactAndWritesDenied
subjectOwnerSetOnceAndRealNameSeparation
v001ThroughV003InventoryMatchesTheFrozenContract
v001ThroughV009MigrateCleanPostgres16WithoutSkipping
v003OwnerPublicAndRuntimeAclsAreExact
v003SubjectNumberCatalogAndGuardsAreExact
v004CatalogOrderInventoryAndGuardsAreExact
v004CatalogVersionLifecycleAndGuardBackstops
v004ForeignKeyAndStagedConsentFkBackstops
v004OrderPaymentLifecycleAndAppendOnlyBackstops
v004OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005ForeignKeyAndDeferredReservationFkBackstops
v005IvrTaskSessionCallInventoryAndGuardShapesAreExact
v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops
v005OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005SessionCallCanonicalCallerAndReciprocalLinkBackstops
v005TaskTtlLifecycleAndGuardBackstops
v006EntitlementReservationUsageInventoryAndGuardShapesAreExact
v006IssueFirstTrialGrantReplayConflictAndPolicySemantics
v006NestedOutboxAtomicityAndReplayRollbackEvidence
v006OwnerPublicRuntimeAndCrossModuleAclsAreExact
v006ReservationUsageForeignKeyAndLateFkBackstops
v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops
v007ActivationConsentInventoryAndGuardShapesAreExact
v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops
v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops
v007ForeignKeyAndLateOrderConsentFkBackstops
v007OwnerPublicRuntimeAndCrossModuleAclsAreExact
v008CreationCycleBothOrdersAndTerminalLifecycleBackstops
v008EvidenceInventoryAndGuardShapesAreExact
v008ForeignKeyAndDeferredCreationCycleFkBackstops
v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops
v008OwnerPublicRuntimeAndCrossModuleAclsAreExact
v009AdmissionCountOnceLimitAndAtomicRollbackBackstops
v009ForeignKeyAndRestrictDeleteBackstops
v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops
v009OwnerPublicRuntimeAndCrossModuleAclsAreExact
v009ShapeDefaultsAndAppendOnlyBackstops
v009SharingInventoryAndGuardShapesAreExact
```

`com.onecall.archtests.container.ProductionDatabaseIdentityProvisioningIT` — 20:

```text
adminInheritSetAndGrantorOptionDriftFailClosed
bindsEnvironmentBackedDatasourcesToExactlySixIdentities
commandAndAdmissionWriteOnlyWithinTheirFrozenBoundaries
commandSwitchesToEvidenceAndRestoresOnTheSameConnection
createsExactIdentityGraphAndReplaysWithoutDrift
credentialsEndpointsAndSqlDiagnosticsAreRedacted
customerQueryAssumesOnlyCustomerRoleAndEnforcesReadOnly
deniesEveryUnapprovedRoleTargetIncludingOwnersAndPredefinedRoles
directLoginsRemainDirectAndCannotAssumeOtherIdentities
directPrivilegesMissingGrantsAndGrantOptionsFailClosed
extraReversedTransitiveAndCrossServiceMembershipsFailClosed
frozenMigrationsLogicalAclsAndHistoryRemainUnchanged
invalidInputsHistoryAndAuthorityRejectAtomically
missingAndStaleMembershipsFailCheckAndOnlyAbsentEdgesAreReconciled
ownershipAndUnexpectedSharedDependenciesFailClosed
publicDatabaseAndDefaultAclDriftFailClosed
readGraphsDenyWritesAndDistinguishAclFromReadOnlyFailures
roleAndDatabaseRoleSettingsFailClosed
sharingQueryAssumesOnlySharingRoleAndRestoresPoolIdentity
unsafeAttributesAndLoginShapeMismatchesFailClosed
```

`com.onecall.archtests.container.PublicShareAdmissionIT` — 42:

```text
activeGrantAuthorizesPlaybackSeekAndRenewWithoutAdmissionMutation
admissionRaceWithRevokeOrRotateObservesCommittedWinner
alreadyAdmittedLockedSessionAuthorizesAndShareLockedIsNotReturned
capturedAndCurrentBindingAndOpenOrMissingRiskDenyWithoutEvent
designatedMobileRequiresTrustedProofProvider
differentKeysAtLastSlotHaveOneWinner
duplicateKeysConcurrentlyCreateOneAdmission
extractionCodeSuccessAndFailureAreRedacted
failureAtAdmissionInsertEventOrCountRollsBackAllEffects
invalidTokenOrUnassociatedAdmissionDoesNotDiscloseState
limitReachedLeavesStateAndLastSessionUnchanged
lostResponseRetryReconstructsWithoutSecondAdmission
malformedTicketMaterialFailsClosedWithoutForgedSuccess
mediaAuthorizationDoesNotChangeAdmissionCountRevisionOrSession
mediaAuthorizationUsesExactRouteAndFrozenResponse
noneAdmissionCommitsOneRowEventCountAndRevision
openRiskBlocksWithoutAdmissionOrCount
organizationMemberReauthenticatesOriginalAccountTenantAndMembership
organizationMemberUsesCurrentAccountOrganizationMembership
postAdmissionResponseNeverContainsForbiddenIdentifiersOrSecrets
postAdmissionUsesExactRouteAndClosedBody
providerUnavailableOrMalformedAppendsFailureWithoutSuccessAndRetrySucceeds
publicGetAndCreatorCreateRevokeRotateListMeRemainUnchanged
realAuthorityChangedMediaFactsConflictBeforeProvider
realAuthorityConcurrentIssuanceReturnsFirstCommittedToken
realAuthorityFirstCredentialReplaysAcrossClockAndInstance
realAuthorityRecordedKeyOverlapAndRemovalFailClosed
realAuthoritySuccessStoresOnlyExactNonsecretMetadata
redisUnavailableAfterCommitReturns503AndPersistsAdmission
responseAndAccessPayloadRedactAllForbiddenMediaFacts
revokedExpiredGenerationStaleAndSessionExpiredMapExactly
revokedExpiredLockedAndStaleGenerationMapExactly
sameKeyPlaybackThenSeekConflictsWithoutSecondCredentialOrEvent
sameKeyReplayPrecedesLimitAndDoesNotRecount
sameKeyReplayRequiresMatchingGenerationWitness
sameRequestAndPurposeReplaysOneCredentialAndOneEvent
sessionExpiryReturnsGoneWithoutRenewal
ticketFailureEventIsDeterministicRedactedAndSingle
ticketPolicyUnavailableBeforeAdmissionLeavesNoRows
twoConnectionConcurrentFirstCallsConvergeOnOneCredentialAndOneEvent
unknownTokenIsUniformAndDoesNotLeakDigestOrGrant
wrongTenantAccountOrMembershipFailsClosed
```

`com.onecall.archtests.container.PublicSharesIT` — 8:

```text
organizationPageRechecksCurrentAccountTenantAndMembership
publicGetKeepsRevokedExpiredAndLockedPageSemantics
publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse
publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent
publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection
publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState
publicReadPreservesCreateRevokeRotateListAndMeBehavior
repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged
```

`com.onecall.archtests.container.SharingAdmissionRoleLeastPrivilegeIT` — 9:

```text
admissionPoolAssertsCurrentAndSessionUserBeforeWrite
admissionPoolResetsRoleTransactionAndReadOnlyState
admissionRoleSupportsAdmissionWriteAndRejectsUnrelatedWrites
v001ThroughV011BytesAndAclSnapshotsRemainUnchanged
v012AclSupportsMediaAuthorizationWithoutNewPrivilege
v012CreatesExactAdmissionRoleAndIsReplaySafe
v012DeniesDmlDdlSequencesFunctionsAndGrantOptions
v012DeniesPublicReadRoleEscalationAndCreatorRoleSubstitution
v012GrantsExactSchemasAndColumnsWithoutFunctionExecution
```

`com.onecall.archtests.container.SharingQueryServiceIT` — 10:

```text
committedLifecycleChangesDenyNextQueryAndFailuresRollBack
inactiveAndMissingCreatorContextsFailClosed
invalidAndCrossContextCursorsNeverWidenScope
keysetTiesAndConcurrentInsertRemainStable
listingProjectionNeverReadsOrReturnsSecrets
organizationListingIsCreatorAndWorkspaceScoped
pageLimitsEmptyAndTailStatesAreExact
personalListingIsCreatorScoped
queryRuntimeStartsAfterFixtureProvisioning
runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure
```

`com.onecall.archtests.container.SharingRoleLeastPrivilegeIT` — 12:

```text
v010CommandRoleExactCatalogAcl
v010CommandRolePositiveAndNegativeBehavior
v010LegacyMigrationDigestsRemainCanonical
v010MigrationIdentityAndPostgres16
v010PrivilegeFailuresAre42501AndReadOnlyAuthorizedWriteIs25006
v010QueryRoleExactCatalogAcl
v010QueryRolePositiveAndNegativeBehavior
v010RoleAttributesAndMembershipContainment
v011LegacyMigrationBytesRemainUnchanged
v011PublicReadAllowsOnlyProjectionAndDeniesWritesAndEscalation
v011PublicReadRoleAndAclAllowlistAreExact
v011UpgradeIsPrivilegeOnlyAtomicAndRejectsIncompatibleState
```

`com.onecall.archtests.container.SubjectClaimRoleLeastPrivilegeIT` — 10:

```text
advisoryAndAuditFunctionsNeedNoNewGrantOrMigration
assumedRoleCanReadExactClaimPredicatesAndInsertHistory
claimUpdateDeleteAndTruncateAreDenied
directLoginCannotReadOrWriteBeforeLocalRole
foreignAccountWritesLocksAndAdjacentReadsAreDenied
frozenAclsHistoryAndCustomerQueryPrincipalRemainUnchanged
localRoleAndTransactionLocksClearOnCommitRollbackAndDenial
ownerUpdatePermitsOnlyAccountRevisionAndTimeColumns
syntheticLoginHasOnlyExistingSubjectNumberMembership
unrelatedDomainWritesDdlAndOwnerEscalationAreDenied
```

### A.3. Existing exact Failsafe includes (27)

```text
**/PostgresContainerIT.java
**/SharingQueryServiceIT.java
**/CustomerMeIT.java
**/CustomerSharesIT.java
**/SharingRoleLeastPrivilegeIT.java
**/CustomerShareCommandsIT.java
**/PublicSharesIT.java
**/PublicShareAdmissionIT.java
**/SharingAdmissionRoleLeastPrivilegeIT.java
**/EvidenceMediaReadRoleLeastPrivilegeIT.java
**/EvidenceMediaReadIT.java
**/MediaPlaybackIT.java
**/MediaGatewayTransportIT.java
**/ProductionDatabaseIdentityProvisioningIT.java
**/DatabaseRuntimeAclRbacHardeningIT.java
**/CustomerBusinessNumbersIT.java
**/CustomerBusinessNumberReadPrivilegeIT.java
**/CustomerSubjectsIT.java
**/CustomerSubjectDetailReadPrivilegeIT.java
**/CustomerEvidenceIT.java
**/CustomerEvidenceReadPrivilegeIT.java
**/CustomerSubjectClaimsIT.java
**/SubjectClaimRoleLeastPrivilegeIT.java
**/CustomerSubjectActivationsIT.java
**/ActivationCommandRoleLeastPrivilegeIT.java
**/OperationsSubjectLifecycleIT.java
**/OperationsLifecycleRoleLeastPrivilegeIT.java
```

## Appendix B. Raw-byte frozen baseline

Measured before artifact creation on 2026-11-14. All22 files match current HEAD content after comparison-only CRLF→LF normalization; all22 raw checkout byte streams differ from the LF Git blobs. This is existing checkout representation, not a planned edit. Keep each raw SHA256/size unchanged in this workspace; also compare frozen tracked Git blobs at review base/head. Never rewrite a file simply to make its raw hash match an LF checkout. On a fresh LF checkout, use the matching Git-blob raw baseline rather than falsely expecting these CRLF hashes.

| Exact path | Bytes | Raw SHA256 |
| --- | ---: | --- |
| `docs/12-technology-stack-and-system-architecture.md` | 39507 | `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | 921490 | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` |
| `docs/15-production-openapi-surface-spec.md` | 54892 | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` |
| `docs/17-production-security-design-spec.md` | 108356 | `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` |
| `api/openapi/onecall-v1.openapi.yaml` | 266916 | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | 25597 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | 16799 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | 12873 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | 26195 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | 33724 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | 35016 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` | 14486 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | 22488 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | 19320 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql` | 24954 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |
| `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql` | 14132 | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` |
| `backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql` | 10615 | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` |
| `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | 9861 | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` |
| `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql` | 48188 | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` |
| `backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql` | 46072 | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` |
| `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` | 49529 | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` |
| `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql` | 53773 | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` |
