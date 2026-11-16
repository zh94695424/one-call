# PA35 Operations IVR Session Read Foundation — Bounded Plan Fix 5

Task label: `pa35-operations-ivr-session-read-plan-fix5-gpt-20261117-01`
Work unit: `PA35-operations-ivr-session-read-foundation`
Date: 2026-11-17 (America/Los_Angeles)
Gate: bounded `plan fix5`; author corrections, not independent acceptance.

**Status:** complete, non-conditional, directly implementable single-slice plan;
no blocking open question. FIX-01 and the accepted decisions of FIX-02 remain;
FIX-03..05 and accepted prior A3/B findings remain corrected; Controller-accepted
PA35-PLAN-REREVIEW2-01..07 and clarifications remain corrected.
PA35-PLAN-REREVIEW3-01..02 are author-corrected against merged HEAD
`9a4fbaddb0fb8f619559734cc4a5e048a44096c1`; independent acceptance is pending.
Merged PA36 V018 now supplies the previously missing Evidence association grants;
existing NOLOGIN roles and the current grants suffice;
the exact V005 task association supports the confirmed filter. SQL resides in
owner modules; existing role/module assertions change in lockstep. Production
login provisioning remains external/fail-closed, not a plan blocker. Stop for
Controller intake and fresh independent A/B Planreview; no acceptance or runtime
execution is claimed, and implementation is not authorized at this gate.

## 1. Binding goal, evidence and one slice

Implement only `GET /api/v1/operations/ivr-sessions`
(`listOperationsIvrSessions`) and
`GET /api/v1/operations/ivr-sessions/{sessionId}`
(`getOperationsIvrSession`). Reuse opaque `employeeSession`, exact audience
`onecall:employee-session:v1`, current server-derived ACTIVE employee and fixed
ADMIN/OPS authority. No caller role, employee identity, workspace or visibility
input is accepted. No customer visibility, third endpoint, export, mutation,
media playback/upload, recording access, audit/outbox fact, retained lock,
retry workflow, generic query layer or new persistence is introduced.

Direct sources: active PA35 checkpoint and accepted findings in
`docs/gateflow/project-execution-control.md`; OpenAPI routes at 1415–1577 and
schemas at 4986–5265; docs14 §4.24–4.28 and evidence tables; docs15 Operations
inventory/model ownership; docs17 opaque employee security and minimization;
V002 employee constraints; V005 IVR columns/checks/FKs/grants; V008 evidence
columns/checks/FKs/grants. Fresh round-2 reviews are A3
`docs/reviews/plan-review-20261116-024007.md` and B
`docs/reviews/plan-review-20261116-013622.md`; prior effective reviews are A3
`docs/reviews/plan-review-20261116-004459.md` and B
`docs/reviews/plan-review-20261116-000416.md` in their isolated review snapshots;
failed A/A2 runs are not acceptance evidence. Cumulative fix1/fix2/fix3 remain
historical evidence, superseded by this plan where corrected. Current Control
hash drift after fix3 is Controller-owned bookkeeping, not a
change to plan inputs or permission to rewrite Control.
PA33/PA34 filters, controllers, requests, codecs, service, configuration,
`OperationsReadContext`, owner readers, provider fixtures, web/configuration/
boundary tests and both Operations read IT families are the direct reuse
baseline, not a presumed new architecture. Current CI and POM are inspected
separately from historical implementation reports.

One implementation slice S1 covers both endpoints: one authority boundary,
one safe session chain, one qualified read transaction and one verification
closure. Splitting by controller, adapter or owner would not independently
prove usable behavior. There is no demonstrated independent behavioral gate
requiring another slice. Reuse beats a second pool, alternate ring, generic
query abstraction or extra named interface. The one required new owner edge is
IVR → Operations' existing named read interface (§7); no reverse edge is added.

Frozen OpenAPI, docs12/14/15, current post-PA36 docs17 and V001–V018
remain unchanged. V018 is an existing frozen input, not a PA35 deliverable.
No V019, new schema role, public grant or privilege delta is planned. Disposable-login
memberships are fixture/deployment configuration, not schema evolution.


### 1.1 Resumed goal, motivation and direct merged-code evidence

The user's 2026-11-17 confirmation is the binding scope contract. Motivation:
give currently ACTIVE ADMIN/OPS employees a minimized, coherent IVR session
list and detail chain without granting media access or bypassing owner modules.
Success signal is the exact two frozen HTTP reads, twelve-field summary and
safe detail, stable filter-bound continuation, one snapshot and restored role,
proved by the named tests and exact inventories below. This gate supplies a
plan only; no runtime, production or independent acceptance is asserted.

Verified branch `codex/pa35-operations-ivr-session-read-foundation` and HEAD
`9a4fbaddb0fb8f619559734cc4a5e048a44096c1`. Root `.git` has a Windows
pointer; read-only `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call23
--work-tree="$PWD" ...` accesses the same existing metadata without editing it.
Control and imported planning/review artifacts were already dirty/untracked.
The six effective prior reviews are 000416,004459,013622,024007,032650,050138
under `docs/reviews/plan-review-20261116-*.md`; fixes1–4 remain historical.

First principles: authentication does not imply current employee authority;
caller filters do not imply authorization; a cross-owner association requires
an actual granted scalar column; independent reads do not form a snapshot;
raw-role capabilities are not response exposure. Therefore reuse the existing
authority/context and owner-local projections rather than widening contracts.
Direct evidence in merged main:
- V014 restricts evidence_role to column grants; V008 alone is NOT the current
  ACL. V018 adds exactly `evidence_record(session_id,call_id)` SELECT from
  evidence_owner, non-grantable. Evidence ACL16→18; original seven monitored
  roles288→290 = customer53 + evidence18 + others219. PA25's expanded nine-role
  oracle is410 =290 + Operations14 + IVR106; those120 tuples are newly monitored,
  not newly granted. V018 itself uses the evidence-only16/18 oracle, not410.
- `backend/ops/postgresql/pa25-runtime-identities.sql`, current docs17 and the
  PA25 runbook already support `operations_login`/`operations_ivr_read`, the
  existing shared read pool and all seven SET edges. Current history18 and
  topology7 logins/9 ACL-monitored roles/11 membership edges supersede the old
  six/seven/four topology. Four shared-context targets are separate from the
  nine-role ACL inventory; no context-target ACL tuples are added to410.
- Current OperationsReadContext still has five nested owner roles ending ORDER;
  adding IVR is the sole context production change. PA36's LO already permits
  IVR. Existing PA33/PA34 disposable fixtures stay scoped to their old memberships.
- `EvidenceRoleIvrAssociationAclBoundaryTests` (8 ordinary methods) and
  `EvidenceRoleIvrAssociationAclIT` (16) prove the new prerequisite and current
  provisioning boundary. Their historical V017 denial and predecessor checks
  remain intact. Only the former's current CI-total assertion needs a PA35
  lockstep (§7 row30); PA36's existing real shared-pool test already covers all
  Role.values() and explicitly IVR, with exactly seven SET targets.

No goal drift or overdesign: only the confirmed routes/filters/fields are
implemented; no client source change (the frozen OpenAPI-generated client
already declares both operations; contract validation regenerates only the
existing disposable output), migration/grant/role/pool or generic query layer.
One slice S1 remains the smallest complete authority-to-safe-chain behavior;
the extra current-inventory assertion is verification, not a second slice.

## 2. Exact supported task filter — no schema or contract mismatch

The former incompatibility was a self-imposed restriction, not a frozen-schema
defect. OpenAPI's list A0 models explicitly include inbound_session,
call_record and outbound_call_task; docs15 lists the same owner-local facts.
The list projection is inbound-session-only, not its filtering table set.
The frozen list A0 metadata is an allowed superset: its ivr.ivr_decision entry
does not require decision reads for the exact twelve-field inbound summary.
V005 provides the exact association:

1. `ivr.inbound_session.id = ivr.call_record.session_id` (UUID/UUID;
   uq_call_record_session and fk_call_record_session).
2. `ivr.call_record.task_id = ivr.outbound_call_task.id` (nullable UUID/UUID;
   existing foreign key).
3. `ivr.outbound_call_task.task_id = :taskId` (public text1–64/text).

Use the owner-local EXISTS semijoin in §5.1 exactly when taskId is present.
Absent taskId emits no call/task predicate or join: sessions without calls or
tasks remain representable. Never compare the internal task UUID with public
task text, invent inbound_session.task_id, omit a supplied filter, infer an
association from callId, select snapshots or introduce a migration. No column,
grant, role or public-contract change is required. No blocking question remains.

## 3. Exact authority, role capability and resource reuse

The new disposable PA35 read login has exactly three existing memberships:
`operations_iam_role`, `ivr_role`, `evidence_role`, each granted by fixture
administrator with `WITH ADMIN FALSE, INHERIT FALSE, SET TRUE`. The login is
NOINHERIT, NOSUPERUSER, NOCREATEDB, NOCREATEROLE, NOREPLICATION, NOBYPASSRLS.
It has no direct table grants. PA34 already uses this grant pattern for its
six existing roles including Operations and Evidence; PA35 adds existing IVR
membership to its own fixture, not to the PA34 fixture or production schema.
V005 grants IVR SELECT to ivr_role. The current Evidence boundary is V014
plus frozen V018, not the original V008 grants: evidence_role has exactly18
explicit ACL tuples and only eight readable evidence_record columns
`id,subject_id,status,source_type,source_ref_id,identity_at_capture,session_id,call_id`.
It has no broad evidence_record table SELECT, no grant option and no reads of
other record columns. PA35 uses four of those eight columns plus the already
authorized evidence_artifact(evidence_id,artifact_role). IVR's broader grants
do not make protected IVR columns part of the application projection. Existing
Evidence media capabilities are never exercised by these adapters.

The external production identity prerequisite is now specified and executable
in merged PA25 SQL, docs17 and the current PA36 section of
`docs/operations/pa25-production-database-identity-provisioning-runbook.md`.
The fourth external input operations_login binds the existing
onecall.operations-subject-read.datasource properties. LO has exactly seven
non-inheriting SET edges: operations_iam_role, subject_number_role,
activation_consent_role, entitlement_metering_role, evidence_role,
catalog_order_role, ivr_role; ADMIN FALSE, INHERIT FALSE, SET TRUE.
PA35's three-role disposable fixture is not the full shared production login.
Platform/Database/Security still own actual credential delivery, PA25 apply/check
and production verification using the existing four-input runbook. No runbook
update or new identity topology is a PA35 implementation deliverable. Missing
membership fails sanitized503; never self-grant, substitute an owner/superuser,
or add a migration, schema role, pool or credential. Existing roles retain their
actual migration attributes; SET and read-only transactions are not a raw-role
privilege sandbox. All existing Operations reads remain supported.

Exactly one new authority lookup per request occurs in Operations role before
IVR/evidence reads and before query/path parsing. Existing boundary bearer
verification remains first. Inside the existing transaction:

1. `operationsSubjectReadContext.enter()` asserts the login/connection,
   read-only and REPEATABLE_READ state and enters/asserts operations_iam_role.
2. Query `operations_iam.employee_account` by trusted employeeId for only
   `id,status,role,revision`. A null trusted caller, null employeeId or
   null/blank audience is503; only a nonblank wrong audience is401, as is
   a missing employee row. Check trusted caller facts before that query;
   validate persisted id/status/role/revision before checking DISABLED.
   Valid status DISABLED with role ADMIN/OPS and revision>=1 is403.
   Role outside ADMIN/OPS, status outside ACTIVE/DISABLED, revision<1,
   null required facts or id unequal to the trusted employeeId is corrupt
   persistence and sanitized503, even if another field says DISABLED.
   This matches V002 and PA33/PA34; never accept authority from inputs.
3. Parse request and cursor, then invoke the narrow IVR reader port. The IVR
   owner adapter wraps each public read operation in `scoped(Role.IVR)`;
   the service does not double-enter that scope. The same connection/backend
   and transaction are used; finally restores/asserts Operations role.
4. Detail alone invokes the Evidence port whose owner adapter uses
   `scoped(Role.EVIDENCE)` with identical restoration. IVR scope has exited
   before Evidence entry. List resolves no Evidence reader.
5. Always exit context in `finally`; assert original login after ROLE NONE.
   Cleanup failure marks rollback-only and returns sanitized503. Existing
   guarded pool asserts cleanliness and evicts contaminated connections.

Only existing production context change: add `IVR("ivr_role")` to nested
`OperationsReadContext.Role`, appended after ORDER; retain
`EVIDENCE("evidence_role")`, every other role and all context mechanics unchanged.
Update the two existing role-sensitive tests in §7 in lockstep; no identity delta.

Reuse exact qualified `operationsSubjectReadDataSource`,
`operationsSubjectReadDsl`, `operationsSubjectReadTransactionTemplate`,
`operationsSubjectReadContext`, `OperationsSubjectReadProperties` and shared
cursor key ring. Keep pool `operations-subject-read` maximum4,
connectionTimeout3000ms; TransactionTemplate REQUIRES_NEW/REPEATABLE_READ/
readOnly timeout5s; statement_timeout2000ms; lock_timeout250ms;
idle_in_transaction_session_timeout5000ms. The five-second timeout is a
TransactionTemplate property, not a pool property. The existing persistence
datasource configuration file stays unchanged and outside the manifest. One
transaction covers authority plus page/anchor or complete detail chain.
No second datasource/pool, transaction manager, DSL context, key ring, nested
independent read transaction or activation-style property prefix is allowed.

New small `OperationsIvrSessionReadConfiguration` composes only the service,
with @Configuration(proxyBeanMethods=false), @Profile("persistence"),
@ConditionalOnProperty(prefix="onecall.operations-subject-read",name="enabled",
havingValue="true") and @Lazy. Qualify every reused resource; use existing
properties provider. Both small query adapters use those same conditions,
@Component with unique names `operationsIvrSessionReader` and
`operationsIvrEvidenceReader`, @Lazy, and the qualified existing DSL/context;
no duplicate @Bean registration. Put contracts in the existing public
`subjectread` package's existing `operations-subject-read` named interface.
The service injects only distinct narrow IvrSessionReader/IvrEvidenceReader
ports (providers qualified by the above bean names), never owner classes.
Place the two adapters in IVR/Evidence internals exactly as §7 specifies.
Each adapter queries only its own schema; no foreign-schema SQL resides in
Operations. IVR's existing package-info gains precisely
`operations_iam::operations-subject-read`; Evidence already permits that edge.
Operations' descriptor remains unchanged (`operations_audit` only): no reverse
Operations→IVR/Evidence compile dependency or cycle. Existing module-boundary
assertions must reflect exactly13 discovered edges, including six Operations
target occurrences, up from12/five; other occurrences remain unchanged (§7).
Missing, ambiguous, uncreatable dependencies fail closed with sanitized503;
retain lazy request-time behavior. Detail does not resolve cursor key material.
No production employee verifier implementation is added.

## 4. Request, route and failure precedence

Add exactly these GET paths to Operations admission AND customer-filter bypass:
`/api/v1/operations/ivr-sessions` and
`/api/v1/operations/ivr-sessions/[^/;%]+`. Preserve exact
`contextPath + servletPath == requestURI`. Both filters currently enumerate
Operations routes; the customer change is required, not speculative. Reject
all other REQUEST-dispatch methods/routes, encoded variants, semicolons, double slashes and
extra segments with concealed404 before authentication. Do not widen prefixes.
Preserve PA31–PA34 subject, lifecycle and activation behavior/filter order.
Real-HTTP TRACE on both new routes follows the pinned subject-read sibling:
Tomcat rejects it before REQUEST admission and the existing shared ERROR
dispatcher returns sanitized500 INTERNAL. Preserve the Operations filter's
ERROR branch verbatim (its TRACE404 exception is activations-only); no ERROR
branch widening or shared dispatcher edit is authorized. MockMvc REQUEST TRACE
remains concealed404. Broader TRACE concealment stays with the existing B-F04
deferred Operations route-boundary owner/work, not S1.

After bearer verification and current authority, list permits only
`cursor|limit|subjectId|callId|taskId|status`, at most one value each. Null
maps/keys/lists/values, duplicates (including identical values) and unknown
keys are422 VALIDATION_FAILED. Limit absent=20, ASCII `[0-9]{1,3}` parsed
integer1–100; signs, decimals, exponent, whitespace and overflow are422.
The `[0-9]{1,3}` grammar already matches PA33/PA34 exactly; retain it unchanged,
including accepted leading-zero forms within the parsed1–100 bounds.
SubjectId and detail sessionId must round-trip UUID.toString exactly lowercase.
CallId1–128 and taskId1–64 Unicode code points, nonblank; preserve exact
accepted text without trimming, case folding or Unicode normalization.
Status is exactly RECEIVED|CONFIRMED|ABANDONED|REJECTED. Detail allows no query
parameters. Reuse existing JSON Accept negotiation rules without changing
existing parser helpers. Capture raw query/path in controller but do no
semantic parsing there before authority.

Shape/noncursor validation is422; a single cursor's contents are decoded
after that validation and valid key configuration. Empty, >512, malformed,
tampered, wrong-purpose or cross-endpoint cursor is400 CURSOR_INVALID.
Only authenticated valid-envelope changed filter digest is400
CURSOR_FILTER_MISMATCH. Missing/invalid configured ring is503, never400.
No input role/workspace, GET body or command header changes read authority.

Failure classification, in order:
- REQUEST route concealment404; real-HTTP TRACE500 as above;
  missing/invalid opaque bearer401 UNAUTHENTICATED.
- Null trusted caller, null employeeId or null/blank audience503
  SERVICE_UNAVAILABLE; nonblank wrong audience or missing employee401
  UNAUTHENTICATED.
- Validate persisted employee facts first: impossible role/status/revision,
  null required fields or mismatched id503 SERVICE_UNAVAILABLE; only a valid
  DISABLED employee403 FORBIDDEN. ACTIVE ADMIN/OPS proceeds.
- Dependency/context corruption503 SERVICE_UNAVAILABLE.
- Authorized query/path/Accept shape422 VALIDATION_FAILED; cursor400 as above.
- Valid detail request whose root is absent404 NOT_FOUND.
- Missing required call, duplicate/inconsistent facts, invalid required fields,
  bad enum/length/time/association, SQL/dependency/timeout/transaction/cleanup
  failure: whole-request sanitized503; no partial200 or partial arrays.
- Unknown programming/runtime failure: sanitized500 INTERNAL, not503.
- Preserve existing429 RATE_LIMITED mapping; do not invent a limiter/retry.

Return existing closed Problem envelope, trace headers, cache/no-store and
401 challenge behavior via OperationsProblemWriter. Never emit or log SQL,
credential/key/token, persistence exception, raw query or protected canary.
Serialize to a complete response buffer before writing any200 body.
Reuse `OperationsSubjectReadService.Failure/Code` throughout; do not add a
parallel error vocabulary in the new service, controller, ports or codec.
At BOTH the new service's transaction boundary and controller exception
handler, pass through every existing Failure/Code unchanged: CURSOR_INVALID
and CURSOR_FILTER_MISMATCH400, UNAUTHENTICATED401, FORBIDDEN403, NOT_FOUND404,
VALIDATION_FAILED422, RATE_LIMITED429, SERVICE_UNAVAILABLE503 and INTERNAL500.
Catch Failure before dependency/runtime catches in the service; the controller
passes failure.code() directly to OperationsProblemWriter, with no PA34
subset switch/default that collapses NOT_FOUND or CURSOR_FILTER_MISMATCH to
INTERNAL. Map actual SQL/JDBC/jOOQ/Spring data-access, bean-resolution,
transaction, deadline and context/role/cleanup failures to503; unknown
programming/runtime failures become500. A real cleanup/context failure may
override the preceding result with503; an otherwise known failure must not be
reclassified merely because it crosses a service/controller boundary.

## 5. Exact projections, SQL, omissions and deterministic arrays

Every DTO has additionalProperties:false. Required fields stay present.
Optional fields below are omitted when SQL NULL, not serialized as null or
empty placeholders. Only decision.action and page.nextCursor are explicitly
required-and-nullable. Instants serialize UTC RFC3339 with DB microsecond
precision. Validate bounds, enums, UUIDs, chronology and relation consistency
before output; no hidden-field repair or alternate-table guidance inference.

### 5.1 Session/page: literal twelve-column mapping

| Source on ivr.inbound_session | DTO field | Presence |
|---|---|---|
| id | sessionId | required UUID |
| call_id | callId | required text1–128 |
| subject_id | subjectId | optional UUID |
| status | status | required RECEIVED/CONFIRMED/ABANDONED/REJECTED |
| node_id | nodeId | required text1–64 |
| protocol_version | protocolVersion | required text1–32 |
| prompt_version | promptVersion | required text1–64 |
| guidance_status | guidanceStatus | optional ACTIVATION_REQUIRED/CONSENT_REQUIRED |
| caller_masked | callerMasked | optional text1–64 |
| access_number_masked | accessNumberMasked | required text1–64 |
| created_at | createdAt | required instant |
| confirmed_at | confirmedAt | optional instant |

No trusted_caller, confirmed_subject_id, revision, ciphertext or HMAC is selected.
Validate visible confirmedAt/status/time shape without fetching hidden facts.
Same literal mapping supplies detail root; guidanceStatus comes ONLY from
inbound_session.guidance_status. Never infer it from decisions/activation/task.
Page has exactly required items (array), nextCursor (string or explicit null),
hasMore (boolean). Fetch limit+1; validate lookahead as well as returned rows.
Order exactly `created_at DESC, id DESC`, exclusive keyset
`(created_at,id) < (anchorOccurredAt,anchorSessionId)`. UUID comparison must
match PostgreSQL unsigned native UUID ordering. Anchor only last returned row;
nextCursor is null on empty/tail page, never omitted. No offset or total.

Exact base SELECT (always the same twelve safe summary columns) is:

```sql
SELECT s.id, s.call_id, s.subject_id, s.status, s.node_id,
       s.protocol_version, s.prompt_version, s.guidance_status,
       s.caller_masked, s.access_number_masked, s.created_at, s.confirmed_at
FROM ivr.inbound_session s
WHERE TRUE
```

Build the predicate list deterministically in the following order, appending
each fixed fragment exactly when its corresponding parsed value is present.
This is one fixed query-construction algorithm, not an unresolved design choice:

| Presence | Exact appended SQL | Positional bind values, in order |
|---|---|---|
| subjectId | `AND s.subject_id = CAST(? AS uuid)` | subjectId (UUID) |
| callId | `AND s.call_id = ?` | callId (text1–128) |
| taskId | `AND EXISTS (SELECT 1 FROM ivr.call_record c JOIN ivr.outbound_call_task t ON t.id = c.task_id WHERE c.session_id = s.id AND t.task_id = ?)` | taskId (public text1–64) |
| status | `AND s.status = ?` | status (exact enum text) |
| decoded anchor | `AND (s.created_at, s.id) < (CAST(? AS timestamptz), CAST(? AS uuid))` | anchorOccurredAt (UTC instant), anchorSessionId (UUID) |

Always append this exact suffix and its final bind `fetchLimit = limit + 1`:

```sql
ORDER BY s.created_at DESC, s.id DESC
LIMIT ?
```

Aliases are exactly s=inbound_session, c=call_record, t=outbound_call_task.
The semijoin SELECT 1 projects no call/task fields and cannot multiply session
rows. Task identity is used only in that semijoin; callId/subjectId/status
compare literally s.call_id/s.subject_id/s.status, never c/t equivalents.
Without taskId, no call/task table appears in emitted SQL, so a session without
a call, or a call without a task, remains eligible. With taskId, absence of
either association correctly does not match. All present predicates intersect.
No UUID/text comparison, wildcard projection, decision lookup or task summary
field is introduced. Only fixed developer-owned fragments are assembled;
all user values and anchor/limit are positional JDBC binds in table order.

Index expectations from V005: uq_inbound_session_call supports call equality;
ix_inbound_session_subject_time supports subject/time candidate access;
uq_call_record_session supports c.session_id correlation;
pk_outbound_call_task supports t.id lookup and uq_outbound_call_task_task_id
supports public text equality; ix_call_record_task and partial
uq_call_record_task permit task-driven access. PostgreSQL may decorrelate the
EXISTS or choose a scan. No frozen index guarantees a global
(created_at DESC,id DESC) seek or status-only lookup;
ix_inbound_session_hmac_status cannot be assumed usable without its HMAC
prefix (which is never selected or filtered here). Sorting/tie refinement and
bounded scans are expected, especially without subjectId. Record representative
EXPLAIN evidence in G's existing paging/filter tests without asserting a
planner-specific index choice. Existing two-second statement/five-second
transaction budgets fail sanitized503 on exhaustion; no new index or migration.

### 5.2 Detail call and exact child DTOs

Root query uses those same twelve columns, `WHERE s.id=CAST(? AS uuid)`;
zero root rows =>404 before child queries. No fabricated root.
This SELECT uses alias s and the sole bind sessionId (UUID). All IVR queries
below run inside the IVR owner adapter's scoped(Role.IVR), never evidence_role
or an Operations-owned foreign SQL class. One detail read operation encloses
the complete IVR chain; it restores Operations before the separate Evidence
port call. The transaction/snapshot remains the same across both owner calls.
Required call is exactly one `ivr.call_record` row `WHERE session_id=CAST(? AS
uuid)`: select `session_id,call_id,status,started_at,ended_at,
authoritative_seconds`. First two fields are internal association checks and
are not serialized. Require root sessionId and callId equality. Map status
required ACTIVE|ENDED|FAILED|UNKNOWN|REJECTED; status alone is required in
IvrCallSummary. started_at→startedAt, ended_at→endedAt and
authoritative_seconds→authoritativeSeconds are each optional and omitted on
SQL NULL, never emitted as null. Present authoritativeSeconds is integer>=0.
Apply V005 terminal shape exactly: ACTIVE/UNKNOWN iff ended_at IS NULL;
ENDED/FAILED/REJECTED require ended_at. Validate ended_at>=started_at only when
both are present. V005 permits present authoritative_seconds only when>=0 and
status is ENDED or FAILED; NULL remains valid in every status.
An ACTIVE call with NULL ended_at is valid and omits endedAt; optional NULL
started_at or authoritative_seconds alone is not corruption. A root without
the contract-required call is explicitly treated as a corruption assumption
and fails closed503, not404 or a fabricated call; that same session stays
listable without a task filter. This does not claim session-without-call is
reachable-valid domain behavior. IVR/A2 plus Contract/Data own documentation
clarification of the invariant at docs14 §9.13/§7.3 and the A2 protocol
documentation; those frozen inputs are not edited in PA35.
No caller/callee numbers, media timing or task facts are serialized.

| Array | Exact selected source → public field | Presence/order/filter |
|---|---|---|
| decisions | ivr.ivr_decision.id→decisionId, purpose→purpose, action→action, reason_code→reasonCode, reservation_id→reservationId, decided_at→decidedAt; session_id/call_id only for internal checks | decisionId/purpose/action/reasonCode/decidedAt required; action explicitly null allowed; reservationId omitted if null; WHERE session_id=? OR call_id=? with both associations required to match root; ORDER BY decided_at ASC,id ASC |
| legs | ivr.call_leg.leg_id→legId, leg_type→legType, direction→direction, state→state, started_at→startedAt, answered_at→answeredAt, ended_at→endedAt, end_reason→endReason | first five required; remaining three optional; WHERE call_id=?; ORDER BY started_at ASC,leg_id COLLATE "C" ASC; uniqueness (call_id,leg_id) closes ties |
| callEvents | ivr.call_event.event_id→eventId, event_type→eventType, sequence_no→sequenceNo, occurred_at→occurredAt, processing_status→processingStatus, reason_code→reasonCode | eventId/eventType/occurredAt/processingStatus required; sequenceNo>=1 and reasonCode optional; WHERE call_id=?; ORDER BY sequence_no ASC NULLS LAST,occurred_at ASC,event_id COLLATE "C" ASC; frozen event_domain=IVR_EVENT and unique domain/event_id close ties |
| evidence | evidence.evidence_record.id→evidenceId,status→status; session_id/call_id only for checks | required evidenceId/status/artifactRoles; WHERE session_id=? OR call_id=?; require nonnull call_id=root.callId and session_id either NULL or root.sessionId; any conflicting associated row fails503; ORDER BY id ASC |
| artifactRoles | evidence.evidence_artifact.evidence_id→internal grouping key, artifact_role→artifactRoles entry | One grouped batch for all collected evidence IDs: SELECT evidence_id, artifact_role FROM evidence.evidence_artifact WHERE evidence_id IN (CAST(? AS uuid),...) GROUP BY evidence_id, artifact_role ORDER BY evidence_id ASC, CASE artifact_role WHEN 'ORIGINAL' THEN 0 WHEN 'DERIVED' THEN 1 ELSE 2 END; validate enum before output; duplicates collapse to role membership, [] valid |

Detail SQL uses the literal unaliased columns/tables and exact SELECT lists
in the preceding paragraphs/table (root alone uses s); no implicit aliases,
SELECT *, dynamic identifiers or unbound caller text. Bind order is:
call=(sessionId UUID); decisions=(sessionId UUID,callId text), with UUID CAST
on the session predicate; legs/events=(callId text);
evidence_record=(sessionId UUID,callId text), with UUID CAST on its session
predicate; artifactRoles=(all returned evidenceId UUIDs sorted in ascending
native UUID order, partitioned consecutively into fixed-size1000-ID IN chunks,
with only the final chunk possibly smaller; one positional UUID bind per ID).
Execute ceil(evidenceCount/1000) artifact queries sequentially inside the SAME
Evidence scope/connection/REPEATABLE_READ snapshot; never one query per evidence
row. Empty evidence issues zero artifact queries. Initialize every evidence
row's artifactRoles to [], merge all chunks by evidence_id, reject unexpected
IDs (including IDs outside the current chunk), deduplicate roles and order
ORIGINAL then DERIVED; preserve the final evidence ID order from the table.
No evidence ID is dropped, capped or interpolated. Fixed placeholder repetition
only, no dynamic identifiers or per-evidence queries. Every table reference
is schema-qualified. Evidence reader selects only id,status,session_id,call_id
from evidence.evidence_record and only evidence_id,artifact_role from
evidence.evidence_artifact,
in its own scoped(Role.EVIDENCE); no IVR or other foreign owner table is joined.
The exact record query is:

```sql
SELECT id, status, session_id, call_id
FROM evidence.evidence_record
WHERE session_id = CAST(? AS uuid) OR call_id = ?
ORDER BY id ASC
```

The binds are root sessionId then validated canonical callId. V018 authorizes
both selected/predicate association columns; H3/H10 must prove real-role success
for this exact four-column SELECT and SQLSTATE42501 for a protected record
column and broad SELECT, not hide the former missing grant. This consumes the
existing owner-issued association capability without another grant or alternate
association. Artifact SQL remains the exact two-column grouped batch above.
Correlation uses only evidence_record.call_id/session_id, not source_ref_id,
recording IDs, snapshots or inferred links. The OR deliberately detects
conflicting associated rows before accepting any evidence entry.

Frozen provenance resolution: IvrEvidenceSummary.artifactRoles is required by
the response schema, which is authoritative. The frozen detail
`x-onecall-a0-models` and docs15 metadata omit `evidence.evidence_artifact`.
This is an A1/A0 documentation residual owned by Contract/Data, destined for
their metadata reconciliation backlog, not an open mapping question. The
existing V008 table, V014 artifact grants and V018 association grants support
exactly the safe projections above;
no public field, contract, table, privilege or frozen-document change occurs.
F4/Boundary explicitly permits this safe table/projection and evidence_record's
four safe columns while still forbidding media/recording tables, hashes,
snapshots and protected columns. ORIGINAL then DERIVED order and evidence ID
order remain deterministic, including empty arrays and duplicate role rows.

Existing index expectations: inbound/call session unique indexes support root
and required-call lookup; uq_call_leg_id and ix_call_leg_call_type support
call-local legs; ix_call_event_call_time and uq_call_event_sequence support
call-local events; ix_ivr_decision_call_time supports call-side decisions;
ix_evidence_call supports call-side evidence and uq_evidence_artifact's leading
evidence_id supports artifact membership. There is no dedicated decision or
evidence session_id index; the OR predicates can scan. Explicit array sorts
remain required and deadlines still apply; no new index/privilege is assumed.

The event SQL selects only listed metadata; SQL ORDER BY may reference its
selected sequence_no. Internal call/session link fields are never serialized.
No decision source_event_domain/source_event_id/request_fingerprint,
response_snapshot/response_payload_ciphertext/response_sha256/key_version
is selected or serialized. No event payload_redacted/payload_ciphertext/hash/
fingerprint is selected. Evidence queries do not select source_ref_id,
snapshot, hashes, media_object_id, recording identifiers, bucket/object keys,
URLs, bytes or credentials, and do not query evidence.media_object or recording.
No joins to media or speculative lineage.

Decision purpose exactly INBOUND_RESOLVE|ACTIVATION_NEXT_ACTION|
DIRECT_AUTHORIZATION. Action exactly GUIDE_ACTIVATION|DIAL_TASK_CALLEE|
ENTER_CALLEE|GUIDE_PURCHASE|REJECT|REJECT_SUSPENDED|REJECT_CALL_IN_PROGRESS|null;
validate the actual V005 purpose/action/reservation shape: INBOUND_RESOLVE and
ACTIVATION_NEXT_ACTION require nonnull action, with reservationId present iff
action is DIAL_TASK_CALLEE. DIRECT_AUTHORIZATION permits null action with
reservationId, or GUIDE_PURCHASE/REJECT/REJECT_SUSPENDED/
REJECT_CALL_IN_PROGRESS with no reservationId. Null action is equivalent to
reasonCode DIRECT_AUTHORIZED; do not reject valid denied direct decisions by
assuming their action must be null. reasonCode exactly the OpenAPI registry:
ACTIVATION_REQUIRED, CONSENT_REQUIRED, CALLER_UNTRUSTED,
SESSION_IDENTITY_MISMATCH, CALL_IN_PROGRESS, CONFIRMATION_NOT_ALLOWED,
ACTIVATION_SUSPENDED, CALLER_BINDING_REVOKED, WORKSPACE_ACCESS_REVOKED,
TASK_ACCESS_REVOKED, ORGANIZATION_INACTIVE, ACCOUNT_DISABLED,
CALL_STATE_UNRESOLVED, TASK_CONFLICT, TASK_CLAIMED, NO_AVAILABLE_ENTITLEMENT,
NO_VALID_TASK, TASK_EXPIRED, TASK_UNAVAILABLE, DIRECT_AUTHORIZED, DATA_INTEGRITY.
Leg type CALLER|CALLEE, direction INBOUND|OUTBOUND with frozen matching pair,
state RINGING|ANSWERED|ENDED|FAILED; legId/endReason1–64.
Event eventId1–128, eventType/reasonCode1–64, processingStatus
RECEIVED|APPLIED|IGNORED|QUARANTINED.
Evidence status PENDING|SOLIDIFIED|QUARANTINED|FAILED; artifactRoles ORIGINAL|
DERIVED. Enforce required values and exact enums rather than unknown-value
omission. Detail always includes call and arrays decisions, legs, callEvents,
evidence, with each evidence artifactRoles array present; empty arrays valid.
No arbitrary truncation, hidden pagination or invented per-child cap: bounded
transaction/dependency failure yields503 rather than partial data.

## 6. One endpoint-specific cursor, shared ring

Use AES/GCM/NoPadding, 256-bit existing validated key, random12-byte nonce,
128-bit tag and canonical unpadded base64url1–512. No plaintext timestamp/UUID,
SQL key, enumerable identity, offset, limit or server-side cursor table.

Fixed purpose `onecall:operations-ivr-session-list:cursor:v1`;
exact endpoint `GET /api/v1/operations/ivr-sessions`;
exact sort `createdAt desc, sessionId desc`.
Canonical filter digest is SHA-256 over UTF-8 prefix
`onecall:operations-ivr-session-filters:v1\n`, followed in exact order
subjectId, callId, taskId, status. For each field append ASCII field name,
a one-byte presence flag (0 absent,1 present), a four-byte big-endian UTF-8
byte length (zero if absent), and exact normalized bytes. SubjectId is canonical
UUID; accepted text is exact as §4; absent differs from empty (empty invalid).
No delimiter collision; query order and limit do not bind the digest.

Envelope binary grammar: version byte=1, key-id length byte k (1–32), ASCII
key-id matching [A-Za-z0-9_-]{1,32},12-byte nonce,60-byte ciphertext,16-byte GCM
tag; no trailing bytes. Header is exactly version/length/key-id (2+k bytes).
Encrypted plaintext is the28-byte anchor (epoch-second int64,nanos int32,
UUID128) immediately followed by the32-byte canonical filter digest, matching
the PA33 subject baseline. Numeric fields are big-endian. Ciphertext+tag=76
bytes; frame=90+k=91–122 bytes; unpadded base64url=122–163 characters, still
within the frozen1–512 bound. No plaintext anchor or filter digest in envelope.
AAD is four concatenated four-byte big-endian length-prefixed byte strings:
UTF-8 purpose, endpoint, sort, then the exact binary header. AAD does not bind
the current request digest. Reject malformed/canonical-alphabet/frame failures
as CURSOR_INVALID; authenticate/decrypt before validating60-byte payload and
anchor nanos exactly as the subject baseline: 0<=nanos<1_000_000_000 and
nanos%1000==0, then Instant.ofEpochSecond(seconds,nanos); construction failure
is CURSOR_INVALID. No additional epoch-seconds window is introduced.
Tampered header/nonce/ciphertext/tag or wrong
purpose/endpoint/sort is400 CURSOR_INVALID. Only after successful decryption
and payload validation compare the decrypted digest constant-time with the
current request digest: mismatch is400 CURSOR_FILTER_MISMATCH, not tamper.
Shared active key issues; retained keys verify; unknown/retired keys invalid,
no fallback or replacement cursor. Missing/invalid ring503 before decode.
Codec/request/properties/failures have redacted representations.

## 7. Exact future S1 path manifest

This is the complete intended implementation allowlist, not permission to edit
it at this gate. Gates: R routes/authority/requests; D DTO/SQL/consistency;
C cursor; T snapshot/roles/resources; F frozen/security boundary; V inventory/
packaged/contract validation. Every row belongs to the single slice S1.
Query adapters live in their owner modules and implement the two distinct
Operations-owned ports; exactly two new production adapters remain, not four
or forwarding wrappers. No new module or named interface is introduced.

| # | State | Exact path | Responsibility | Gates |
|---|---|---|---|---|
| 01 | existing | `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java` | Admit exactly two GET templates; preserve URI equality and concealment. | R |
| 02 | existing | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | Add exact two GET bypasses in lockstep; no broad Operations bypass. | R |
| 03 | existing | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsReadContext.java` | Only add nested enum IVR("ivr_role"); retain EVIDENCE("evidence_role") and all context logic. | T |
| 04 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsIvrSessionReadController.java` | Two mappings, raw request capture, exact DTO serialization and sanitized problems. | R/D |
| 05 | new | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsIvrSessionReadService.java` | Only list/detail; immutable raw requests and exact frozen DTO records. | R/D |
| 06 | new | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsIvrSessionReadPorts.java` | Narrow IvrSessionReader/IvrEvidenceReader and immutable facts/filters/anchor; no generic query port. | D/T |
| 07 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadRequest.java` | Strict parsing and presence-aware filter digest. | R/C |
| 08 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionCursorCodec.java` | One endpoint-specific AES-GCM envelope using shared ring. | C |
| 09 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/DefaultOperationsIvrSessionReadService.java` | Single authority check, transaction, mapping/consistency and failure classification. | R/D/T |
| 10 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java` | Compose service only over existing qualified resources; no datasource/key bean. | T |
| 11 | new | `backend/src/main/java/com/onecall/core/ivr/internal/operationsread/JooqOperationsIvrSessionReader.java` | IVR-owned safe SQL/EXISTS task filter and detail chain inside Role.IVR; no cross-owner join. | D/T |
| 12 | new | `backend/src/main/java/com/onecall/core/evidence/internal/operationsread/JooqOperationsIvrEvidenceReader.java` | Evidence-owned safe SQL inside Role.EVIDENCE; exact call/session correlation, no foreign join or media read. | D/T |
| 13 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadRequestTests.java` | Exact ordinary test identities A below. | R/C |
| 14 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionCursorCodecTests.java` | Exact ordinary test identities B below. | C |
| 15 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadServiceTests.java` | Exact ordinary test identities C below. | R/D/T |
| 16 | new | `backend/src/test/java/com/onecall/archtests/OperationsIvrSessionReadWebTests.java` | Exact ordinary test identities D below. | R/D |
| 17 | new | `backend/src/test/java/com/onecall/archtests/OperationsIvrSessionReadConfigurationTests.java` | Exact ordinary test identities E below. | T |
| 18 | new | `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` | Exact ordinary test identities F below. | F |
| 19 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` | Exact ordinary test identities G below; PostgreSQL16. | D/T |
| 20 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadRoleLeastPrivilegeIT.java` | Exact ordinary test identities H below; PostgreSQL16. | T/F |
| 21 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadFixture.java` | Disposable login, frozen synthetic facts and catalog/data fingerprints; zero tests. | T |
| 22 | new | `backend/src/test/java/com/onecall/archtests/operations/OperationsIvrSessionReadTestProviders.java` | Synthetic properties/facts/providers only; zero tests; no production verifier. | R/C/T |
| 23 | existing | `.github/workflows/ci.yml` | Exact named PA35 additions, source/history/token/POM/XML reconciliation; retain firewall/A1-A5. | V |
| 24 | existing | `backend/pom.xml` | Only two Failsafe includes, no dependency/plugin/profile change. | V |
| 25 | new | `docs/gateflow/pa35-operations-ivr-session-read-foundation-implementation.md` | Actual authorized scope, source/CI/POM/XML, commands/hashes, residuals; not a plan-gate runtime claim. | V |
| 26 | existing | `backend/src/main/java/com/onecall/core/ivr/package-info.java` | Retain four dependencies; add exactly operations_iam::operations-subject-read. | F/T |
| 27 | existing | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | Add exact IVR allowed edge and change discovered target multiset12→13/five→six Operations targets; no identity delta. | F/V |
| 28 | existing | `backend/src/test/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadServiceTests.java` | Include IVR after ORDER in existing exact nested Role.values assertion; no identity delta. | T/V |
| 29 | existing | `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java` | Replace generic Role.values loop with PA33 fixture's exact five-role list, excluding IVR; no identity delta. | T/V |
| 30 | existing | `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | In ciPomAndOrdinaryMethodInventoryMatchExactly update only current CI total literals to [788, 556]/1345/83/34; preserve PA36-start/history/ACL/topology/method checks. | F/V |

Counts: **30 S1 paths =20 new +10 existing**. Production:13=9 new+4
existing (including the one existing module descriptor). Tests/helpers:14=
10 new (8 test classes+2 zero-test helpers)+4 existing test classes. CI/POM:2
existing. Implementation report:1 new. Thus13+14+2+1=30;
9+10+0+1=20 new,4+4+2+0=10 existing. Exactly two new owner SQL adapters
replace the former two Operations-internal paths; no stale adapter path is
allowed. No other production/test/module/resource/dependency/private path is
included. Context row03 changes only the enum, not its mechanics.

Exact lockstep changes, all inside existing test identities:
- `ownerHandoffsAssertAndRestoreLiteralRolesOnOneResource` in row28 retains
  its assertions and exact enum order SUBJECT,ACTIVATION,TRIAL,EVIDENCE,ORDER,
  then IVR (matching the appended production enum). No test is added/renamed.
- `normalAndExceptionalHandoffsRestoreOperationsRole` in row29 iterates
  `List.of(Role.SUBJECT,Role.ACTIVATION,Role.TRIAL,Role.EVIDENCE,Role.ORDER)`
  rather than Role.values(); retain success/exception restoration checks for
  exactly the PA33 fixture's configured roles. Do not grant that fixture IVR.
  PA35 H separately proves IVR scope/membership/restoration on its own login.
- Row27's `assertsExactAllowedDependencyMapFromPlan62` continues checking
  EXPECTED_ALLOWED, whose ivr set gains only the named Operations edge.
  `discoveredProductionEdgesStayInsideTheConfiguredMap` changes its description
  from twelve to thirteen and appends one operations_iam target to its exact
  multiset: sharing×1,evidence×2,subject_number×1,activation_consent×1,
  catalog_order×1,entitlement_metering×1,operations_iam×6 =13. Current map has
  five Operations target occurrences; future has six. This is discovered-edge
  cardinality, not the total count of every descriptor permission. Preserve
  all other assertions, notably the unrelated customer allowed size12.
  Existing nine test identities remain unchanged; no new module or reverse
  Operations→IVR/Evidence edge is permitted.
- Row30's `ciPomAndOrdinaryMethodInventoryMatchExactly` retains all eight
  PA36 method identities and all16 container identities. Replace only current
  `[712, 520]`, `== 1233`, `== 77`, `== 32` literals with `[788, 556]`,
  `== 1345`, `== 83`, `== 34`; retain `pa36_start_totals == [704, 504]` and
  `pa36_start_line_scan_total == 1209`. Preserve every other assertion in this
  class: frozen predecessor/public-contract pairs, current docs17 canonical
  pin, V018 checksum, history18,410 ACL,11 edges, dependency hash, no expansion.
Gate bookkeeping artifacts are separate from these S1 rows; the active
Controller dispatch alone determines the author document allowlist. Prior fix
artifacts remain immutable history. Control and independent review artifacts
remain Controller-owned, outside this agent's write scope.

## 8. Exact ordinary test identity manifest

All identities below are ordinary no-argument `@Test void name()` (an optional
throws clause is permitted). Place @Test immediately before signature with
whitespace only. No intervening annotations/comments, parameterized, repeated,
dynamic, inherited tests or tests in helpers. Setup/teardown annotations are
permitted but are not test identities. Multiple assertions/variants run inside
the one named method and do not change counts. All method names are unique
within their class. Each class's file path is exactly §7's matching simple name.

### A. `com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests` — Surefire, 12 methods

1. `listAllowsExactlySixQueryKeys()`
2. `duplicateUnknownAndNullParametersAreValidationFailed()`
3. `absentLimitDefaultsToTwenty()`
4. `limitGrammarAndOneThroughOneHundredBoundsAreExact()`
5. `subjectAndPathUuidsAreCanonicalLowercase()`
6. `callAndTaskIdsUseExactCodePointBoundsWithoutTrimming()`
7. `sessionStatusesAreExactAndCaseSensitive()`
8. `detailRejectsEveryQueryParameter()`
9. `singleMalformedCursorIsDeferredToCodec()`
10. `acceptNegotiationUsesExistingJsonReadRules()`
11. `presenceAwareDigestHasFixedFieldOrderAndUnambiguousLengths()`
12. `queryOrderAndLimitDoNotChangeDigest()`

### B. `com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests` — Surefire, 12 methods

1. `roundTripPreservesMicrosecondAnchorAndUuid()`
2. `encodingIsRandomizedOpaqueCanonicalAndBounded()`
3. `lengthAlphabetVersionAndFrameFailuresAreCursorInvalid()`
4. `tamperedHeaderNonceCiphertextAndTagAreCursorInvalid()`
5. `wrongPurposeEndpointAndSortAreCursorInvalid()`
6. `subjectAndActivationCursorsAreCursorInvalid()`
7. `unknownAndRetiredKeysAreCursorInvalid()`
8. `authenticatedChangedFilterDigestIsFilterMismatch()`
9. `digestTamperingIsInvalidNotFilterMismatch()`
10. `activeAndRetainedSharedKeysSupportRotation()`
11. `missingInvalidAndAmbiguousKeyMaterialIsUnavailable()`
12. `invalidAnchorPrecisionAndRedactedRepresentationsFailClosed()`

### C. `com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests` — Surefire, 18 methods

1. `activeAdminAndOpsAreOnlyCurrentAuthorities()`
2. `wrongAudienceAndMissingEmployeeAreUnauthenticated()`
3. `disabledAuthorityIsForbiddenAndCorruptEmployeeFactsAreUnavailable()`
4. `authorityPrecedesParsingAndOwnerDependencyResolution()`
5. `listUsesLookaheadAndOnlyLastReturnedAnchor()`
6. `emptyAndTailPagesHaveExplicitNullCursor()`
7. `tiedInstantsUseUnsignedUuidDescendingOrder()`
8. `continuationIsExclusiveAndLimitIsNotBound()`
9. `allFourFilterValuesReachOnlyExactReaderFilters()`
10. `sessionProjectionHasExactRequiredAndOmittedOptionalProperties()`
11. `detailRequiresExactlyOneMatchingCall()`
12. `nullableDecisionActionRemainsPresent()`
13. `nestedArraysAndArtifactRolesAreDeterministic()`
14. `missingRootIsNotFoundAndEmptyArraysAreValid()`
15. `inconsistentPersistedFactsAndLookaheadFailWholeRequest()`
16. `dependencyConfigurationAndDeadlineFailuresAreUnavailable()`
17. `unknownProgrammingFailuresAreInternalAndSanitized()`
18. `singleSnapshotRoleCleanupAndZeroSideEffectsCoverEveryExit()`

### D. `com.onecall.archtests.OperationsIvrSessionReadWebTests` — Surefire, 16 methods

1. `exactListGetReturnsOnlyFrozenPageAndSummary()`
2. `exactDetailGetReturnsOnlyFrozenSafeChain()`
3. `wrongMethodsAndUnknownOperationsPathsStayConcealed()`
4. `encodedSemicolonDoubleSlashAndExtraSegmentsStayConcealed()`
5. `authenticationPrecedesMalformedQueryAndPath()`
6. `currentAuthorityPrecedesOwnerDependencies()`
7. `missingAmbiguousAndBrokenVerifiersFailClosed()`
8. `missingAmbiguousAndBrokenServicesFailClosed()`
9. `duplicateUnknownMalformedParametersAndPathAre422()`
10. `malformedTamperedAndFilterMismatchCursorsAre400()`
11. `problemsHaveExactSafeEnvelopeHeadersAndChallenge()`
12. `optionalFieldsAreOmittedAndRequiredNullsRemainPresent()`
13. `noCallerRoleWorkspaceBodyOrHeaderBecomesAuthority()`
14. `customerAndOperationsFiltersAdmitOnlyExactTwoGets()`
15. `existingSubjectActivationAndLifecycleRoutesRemainUnchanged()`
16. `partialAndUnknownFailuresNeverLeakSensitiveCanaries()`

### E. `com.onecall.archtests.OperationsIvrSessionReadConfigurationTests` — Surefire, 10 methods

1. `defaultWebAndDisabledPersistenceCreateNoIvrReadResources()`
2. `enabledGraphLazilyReusesAllQualifiedSubjectReadBeans()`
3. `serviceConfigurationCreatesNoPoolDslContextOrKeyRing()`
4. `transactionRetainsFiveSecondReadOnlyRepeatableReadSettings()`
5. `onlyExistingSubjectReadPrefixConfiguresKeysAndCredentials()`
6. `missingInvalidAmbiguousAndUncreatableResourcesFailClosed()`
7. `detailDoesNotResolveCursorKeyMaterial()`
8. `ownerReadersRegisterExactlyOnceWithoutReverseModuleEdges()`
9. `requestsPropertiesKeysAndFailuresHaveRedactedRepresentations()`
10. `noProductionEmployeeVerifierOrNewDependencyIsRegistered()`

### F. `com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests` — Surefire, 8 methods

1. `publishesOnlyTwoReadMethodsAndExactFrozenRecords()`
2. `operationsRoutesAndControllerMappingsAreExact()`
3. `ivrSqlUsesOnlyExplicitApprovedColumns()`
4. `evidenceSqlNeverReadsMediaRecordingOrProtectedColumns()`
5. `applicationAndControllerContainNoForeignSql()`
6. `readPathHasNoWritesAuditOutboxLocksOrCursorStorage()`
7. `namedInterfaceAndExactThirteenModuleEdgesArePreserved()`
8. `frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact()`

### G. `com.onecall.archtests.container.OperationsIvrSessionReadsIT` — Failsafe, 24 methods

1. `postgres16UsesExactlyFrozenEighteenMigrations()`
2. `realHttpActiveAdminAndOpsReturnExactList()`
3. `realHttpDetailReturnsExactSafeChain()`
4. `realHttpAuthenticationAuthorityAndAudiencePrecedenceAreExact()`
5. `realHttpMethodsAndNoncanonicalVariantsStayConcealed()`
6. `realHttpParameterDefaultsDuplicatesAndBoundsAreExact()`
7. `subjectFilterMatchesOnlyExactSessionSubject()`
8. `callFilterMatchesOnlyExactCallIdentity()`
9. `taskFilterRequiresPersistedAssociationAndNeverSilentlyDropsPredicate()`
10. `statusFilterMatchesAllFourFrozenStatuses()`
11. `combinedFiltersAndPresenceAwareDigestRemainExact()`
12. `emptySingleAndExactLimitPagesHaveNullCursor()`
13. `lookaheadAndTailUseOnlyLastReturnedAnchor()`
14. `timestampTiesAndHighBitUuidsContinueWithoutDuplicates()`
15. `changingLimitReusesCursorWithoutMismatch()`
16. `realHttpMalformedTamperedWrongPurposeAndFilterMismatchAre400()`
17. `sessionOptionalNullsAreOmittedAndGuidanceIsNotInferred()`
18. `callAndDecisionRequiredNullableAndOptionalFieldsAreExact()`
19. `legsEventsAndEvidenceHaveDeterministicOrdering()`
20. `missingRootIs404ButMissingCallAndInconsistentLinksAre503()`
21. `concurrentDomainChangesRemainInvisibleWithinSnapshot()`
22. `concurrentEmployeeDisableUsesSnapshotThenDeniesNextRequest()`
23. `dependencyTimeoutCleanupAndUnknownFailuresAreSanitized()`
24. `allResponsesLogsAndBusinessAuditOutboxFingerprintsStaySafe()`

### H. `com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT` — Failsafe, 12 methods

1. `disposableLoginHasExactlyThreeExistingNonInheritedSetMemberships()`
2. `operationsRoleCannotReadIvrButScopedIvrRoleCan()`
3. `evidenceScopeReadsOnlySafeAssociationAndArtifactProjection()`
4. `missingIvrMembershipFailsClosedWithoutRepair()`
5. `missingEvidenceMembershipFailsDetailWithoutRepair()`
6. `missingOperationsMembershipFailsClosedBeforeParsing()`
7. `handoffsAssertAndRestoreSameConnectionBackendAndSnapshot()`
8. `wrongEntryResourceAndTransactionStateFailClosed()`
9. `readOnlyTransactionRejectsAllDmlAndLockingQueries()`
10. `forbiddenQueriesStayOutsideProductionSqlWithoutPretendingOwnerColumnAcls()`
11. `failedRestorationRollsBackAndEvictsDirtyConnections()`
12. `poolRolesSettingsLocksCatalogsAndFactsAreCleanAfterEveryExit()`

C3 tests valid DISABLED403 separately from impossible role/status, null fields,
revision<1 and id mismatch503, including corrupt DISABLED facts; C15 retains
whole-request corruption/lookahead503 and C17 retains unknown-failure500.
G4 and D6 mirror the same precedence; no new identity is needed.
C2/C3/G4/D6 also distinguish null trusted caller/employeeId and null/blank
audience503 from nonblank wrong audience401. C14/D11/G20 prove NOT_FOUND404;
B8/C9/D10/G16 prove CURSOR_FILTER_MISMATCH400. C17/D11/D16/G23 inject every
existing Failure/Code and assert unchanged code/status through both new
service and controller layers, separately from dependency503 and unknown500.
G5 retains its identity: POST/PUT/PATCH/DELETE/HEAD/OPTIONS on both new routes
are concealed404, as are REQUEST noncanonical/unknown routes; real-HTTP TRACE
on both new routes is sanitized500 INTERNAL via the unchanged shared ERROR
dispatcher. D3 retains REQUEST-dispatch TRACE404; D15 and existing PA34 web/IT
identities/ERROR-branch expectations remain unchanged.
B2/B3 assert the exact91–122-byte/122–163-character framing,60-byte plaintext
and76-byte ciphertext+tag. B2 asserts neither the28-byte anchor nor32-byte
filter digest occurs as clear envelope metadata (inspect/decrypt with fixture
key, not text-only checks). B4/B9 flip header/encrypted digest bytes without
retagging =>CURSOR_INVALID; B8 uses an authenticated different decrypted digest
=>CURSOR_FILTER_MISMATCH. B5 tests each AAD component and B12 authenticated
malformed payloads; D10/G16 exercise invalid-versus-mismatch at HTTP boundary.
G18/D12 cover an ACTIVE call with NULL ended_at, optional NULL started_at and
authoritative_seconds omitted, plus valid terminal shapes and invalid V005
terminal/time shapes503. C13/G19/H3/F4 prove the §5.2 fixed-size1000-ID batch
algorithm, no N+1, deterministic merge/evidence/role ordering and [] for no
artifacts. Cover zero, one,1000,1001 and multiple full chunks inside these
existing identities: query count ceil(evidenceCount/1000), zero for no evidence,
no omitted ID/partial200 and one snapshot across all chunks; deadline/fault503
fails the whole response. F4 explicitly allows only
evidence_record(id,status,session_id,call_id) and
evidence_artifact(evidence_id,artifact_role), still forbidding media/recording/
hashes. E4/H12 assert the exact pool/template/session timeout labels in §3.

G's task-filter test exercises the exact §2 association: public non-UUID task
text, different internal UUID, matching/nonmatching tasks, call with NULL task,
and session without call. Assert no-task list includes those last two; supplied
task filters exclude them, combine with the other three literal root filters,
and never duplicate roots. Keep all variants in the existing G9/G11/G12
identities, not new methods. F3 asserts the twelve-column list projection and
permitted owner-local semijoin; F5 forbids Operations-owned foreign SQL; F7
asserts the one new named owner edge and no reverse compile dependency. E8
proves both owner adapters implement only their distinct qualified ports.
H proves existing role grants accurately:
foreign tables are denied under operations_iam_role; read-only rejects DML
under owner roles. IVR's broad reads require static SQL and wire/log canaries
to prove unused columns are never selected/serialized. Evidence H3/H10 additionally
assert the actual column ACL: association reads succeed, protected record columns
and broad record SELECT fail42501. Never claim all Evidence owner reads are broad,
or that unused but already granted media columns are SQL-denied. Missing role
memberships never trigger self-repair grants. Capture session_user,
current_user, backend PID, isolation, read-only state and restored connection
settings on success and every fault path; fingerprint business/audit/outbox
facts, roles, grants and function definitions after fixture setup.

## 9. Exact source = CI = POM = XML reconciliation

Current source was directly scanned with the existing CI regex
`@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`:77 Surefire classes/712 methods,
32 Failsafe classes/520 methods =109 classes/1232 methods. Every baseline
identity remains unchanged. Current CI final maps and assertions specify the
same77/712 and32/520. Current POM has32 explicit Failsafe includes matching
those IT classes; Surefire uses its existing default names. Current
`@Test` token-line count is **1233**, not1232: one preserved comment hit in
PostgresContainerIT contains the token. Do not remove it or weaken the regex.
No local XML reports are supplied as current execution evidence. Historical
PA34 reports107/1208; they do not describe the merged PA36 baseline. The
current CI's expected XML arithmetic is verified statically, not newly run.

Planned additions (all exact identities §8):

| Runner | Class deltas | Method deltas | Baseline → expected full |
|---|---|---|---|
| Surefire | A–F:6 |12+12+18+16+10+8=76 |77/712 →83/788 |
| Failsafe | G–H:2 |24+12=36 |32/520 →34/556 |
| Combined |8 |112 |109/1232 →117/1344 |
| Token-lines |8 classes, no helper test tokens |112 |1233 →1345 |

Four changed existing test classes add **zero** classes/methods/token lines:
OperationsSubjectReadServiceTests16 + ModuleBoundaryTests9 +
EvidenceRoleIvrAssociationAclBoundaryTests8 =33 Surefire methods;
OperationsSubjectReadRoleLeastPrivilegeIT12 Failsafe methods. All45 existing
identities remain exactly unchanged; only assertions/loop scope change (§7).
PA35-new-only set is6/76+2/36=8/112. Required focused command in §11 includes
these four lockstep classes: **9/109 Surefire +3/48 Failsafe =12/157**,
zero skips/errors/failures. Full expected XML:83/788 +34/556 =117/1344,
zero skips/errors/failures. These existing45 are already in the baseline and
must not be counted as additions, subtracted from history or duplicated in CI.
These are fixed verification expectations, not claims of executed tests; XML must prove
the exact names, not just totals. Duplicate/missing/unexpected/skipped suite
or method fails the inventory gate. The focused method set must equal the
full run's subset for exactly those twelve selected classes.

CI named additions: add `pa35_unit_additions` dictionary keyed by each full A–F
class with exactly its §8 method-name set, and `pa35_container_additions` for
G–H. Merge into unit_expected/container_expected respectively. Require
per-class lengths [12,12,18,16,10,8] and [24,12]. In per-file scans subtract
PA35 identities before *every* pre-PA35 historical method/token calculation,
including the existing PA34-start and PA36-start calculations; preserve historical invariants:
112+178=290 →128+192=320 →162+198=360 →196+204=400 →231+214=445 →266+214=480
→309+214=523 →309+234=543 →309+246=555 →341+272=613 →375+302=677 →417+336=753
→468+370=838 →520+404=924 →574+440=1014 →644+476=1120 →704+504=1208
→712+520=1232 (PA35-start, after the historical PA36-start).
Historical token-line totals remain291,321,361,401,446,481,524,544,556,614,
678,754,839,925,1015,1121,1209,1233; final1345. New pa35_start_totals must equal
[712,520] and pa35_start_line_scan_total1233; final method_totals[788,556].
Require final maps83/34 and POM includes34, exact set equality, exact_inventory
XML expectations788/556. Preserve pa36_unit_additions8 and
pa36_container_additions16 and every PA36 static prerequisite assertion. Preserve every older named identity/addition, comment
exception, regex and historical subtraction assertion; do not merely replace
the totals or weaken the checks.

Also update the early aggregate Surefire assertion inside the existing CI
report body from712 to788, not just the final exact_inventory call. Preserve
the PostgresContainerIT64-method assertion unchanged. Every current total-bearing
diagnostic must reflect the final inventory without changing historical totals.

POM delta is exactly:
`<include>**/OperationsIvrSessionReadsIT.java</include>` and
`<include>**/OperationsIvrSessionReadRoleLeastPrivilegeIT.java</include>`
in existing container-smoke Failsafe includes;32→34. No Surefire include or
exclusion changes, dependency changes, version changes or new profile.
Throughout this plan, “POM34” means this container-smoke Failsafe include scope,
not all global POM include elements (two unrelated includes remain unchanged;
the all-POM include count is34→36).

### 9.1 Exhaustive post-PA36 test-assumption lockstep

The merged predecessor already changed the following current-state assumptions.
These are preservation requirements, NOT additional PA35 write paths. Paths in
this ledger are relative to `backend/src/test/java/com/onecall/archtests/`.
All historical method names, V015/V016/V017 target fixtures, prefix checksums,
old customer53/288 or migration-era denial oracles remain historical; never
globally replace seventeen/288/four-edge text. Current PA25 is history18,
customer53/original-seven290/evidence18/expanded-nine410, topology7/9/11.

| Existing merged paths | Current assertion to preserve |
|---|---|
| `architecture/CustomerBusinessNumberBoundaryTests.java`, `architecture/CustomerSubjectDetailBoundaryTests.java` | PA25 history18/oracle410, old V015/V017 schema facts unchanged |
| `architecture/CustomerEvidenceBoundaryTests.java` | 410 monitored ACLs, evidence18, others219 excluding customer/evidence/Operations/IVR; history18 with version018/checksum1606293398; customer53 unchanged |
| `architecture/CustomerSubjectActivationBoundaryTests.java`, `architecture/CustomerSubjectClaimBoundaryTests.java` | Exact migration tail adds V018 while preserving historical frozen input maps |
| `architecture/OperationsSubjectReadBoundaryTests.java` | Current migration inventory V001–V018 |
| `architecture/OperationsActivationReadBoundaryTests.java`, `architecture/OperationsLifecycleBoundaryTests.java` | Current migration count18; verify whole current docs17 canonical hash before checking the uniquely marked historical body against old pins |
| `container/OperationsActivationReadsIT.java` | History18, migration tail V018 and the same current-docs17/historical-body separation; PostgreSQL16/UTF8 retained |
| `container/PostgresContainerIT.java` | Exact eighteen migration filenames; historical64 methods/comment token retained |
| `container/DatabaseRuntimeAclRbacHardeningIT.java` | Current history/resources18; existing historical hardening prefix unchanged; current PA25 marker7/9/11 |
| `container/ProductionDatabaseIdentityProvisioningIT.java` | History18 and final-rank18 corruption checks; exact410 expanded ACL oracle, four external inputs,7/9/11 markers and11 edges;31→35 project-role inventory; old credential/provenance preservation remains |
| `container/ActivationCommandRoleLeastPrivilegeIT.java`, `container/OperationsLifecycleRoleLeastPrivilegeIT.java` | Original-seven oracle290 = historical288 + the two association tuples; history18 (lifecycle retains first17 plus one018) |
| `container/CustomerBusinessNumberReadPrivilegeIT.java` | V015 prefix remains; latest adds V016–V018 (3), history18, excludes V018 from predecessor freeze; current290 / injected-extra291; marker7/9/11 |
| `container/CustomerEvidenceReadPrivilegeIT.java` | Preserve V017/288 historical oracle; explicit latest018 adds two association tuples to290, requires PA25 only after latest, current history18 and ranks16/17/18 corruption cases |
| `container/CustomerSubjectDetailReadPrivilegeIT.java` | Preserve V016/269 prefix oracle; latest target018 adds frozen customer V01719 tuples plus association2 =290 |
| `container/CustomerBusinessNumbersFixture.java` | Default latest18; explicit historical targets retained; fourth operations_login provisioning input |
| `container/CustomerMeFixture.java`, `container/SharingQueryFixture.java` | Latest migration execution/history18 |
| `container/CustomerBusinessNumbersIT.java`, `container/CustomerEvidenceIT.java`, `container/CustomerSubjectClaimsIT.java`, `container/CustomerSubjectsIT.java` | Current history18, unchanged historical method identities |
| `container/EvidenceMediaReadIT.java`, `container/EvidenceMediaReadRoleLeastPrivilegeIT.java` | Latest execution/applied history18, no new media behavior |
| `container/OperationsActivationReadRoleLeastPrivilegeIT.java`, `container/OperationsSubjectReadRoleLeastPrivilegeIT.java`, `container/OperationsSubjectReadsIT.java` | Current history18; existing fixtures/memberships unchanged; only row29's role loop changes for appended IVR |
| `container/PublicShareAdmissionIT.java`, `container/SharingAdmissionRoleLeastPrivilegeIT.java`, `container/SharingQueryServiceIT.java`, `container/SharingRoleLeastPrivilegeIT.java`, `container/SubjectClaimRoleLeastPrivilegeIT.java` | Latest execution/applied history18; historical privilege boundaries unchanged |
| `architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`, `container/EvidenceRoleIvrAssociationAclIT.java` | 8+16 ordinary methods; V01742501 denial, evidence16→18, full408→410, V018 checksum/history18, seven-role LO/shared-pool handoffs,11-edge topology and provenance; only row30's current CI numbers update |

This ledger covers all36 test/helper Java paths changed by the PA36 merge.
Only row30 newly pins current CI totals; no other merged test needs a new PA35
current-state rewrite. Current docs17 canonical hash, V018 and PA25 pins in §10
replace old assumptions; historical artifacts and test prefixes are untouched.
Preserve CI's V018 packaged-entry check exactly once and PA25 probes/markers.
Current XML expectations are77/712 +32/520, with32 scoped Failsafe includes;
future XML must prove83/788 +34/556 and exact method sets, not merely counts.

## 10. Frozen raw/LF SHA-256 manifest (measured current bytes)

Raw is measured checked-out bytes; canonical LF replaces CRLF with LF only,
without trimming, BOM removal, text re-encoding or other normalization. The
26 protected inputs are OpenAPI + docs12/14/15/current17 +18 migrations +
PA25 SQL/current runbook/backend README. The last three are merged-current
preservation inputs, not permission to alter them. PA36 changed docs17, PA25,
runbook and README; every pin here is remeasured, not inherited from fix4.
No frozen file is rewritten for verification or to manufacture a passing hash.

| Exact frozen path | Current raw SHA-256 | Current LF SHA-256 |
|---|---|---|
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| `docs/12-technology-stack-and-system-architecture.md` | `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db` | `772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` | `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` |
| `docs/17-production-security-design-spec.md` | `b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205` | `4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6` |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql` | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |
| `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql` | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |
| `backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql` | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` |
| `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` | `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` |
| `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql` | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` |
| `backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql` | `ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c` | `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817` |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9` | `8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451` |
| `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | `44ced2245fc1c89175eeb386bc6cc6fa036302e368829cdd163f3da022425423` | `792a32f72d6657b59a220a42ec7c11f94361078c8c0d491ff52d777fe22f08ab` |
| `backend/README.md` | `fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64` | `bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177` |

Pair-map digest: `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409` over path-sorted UTF-8 path,NUL,raw digest bytes,
canonical digest bytes per row (no row terminator). It locks all26 pairs.

F8/G1 retain the inherited plan-file mechanism. From backend cwd read
`../docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md` as UTF-8
and match `(?m)^([0-9a-f]{64})  (.+)$`; exactly26 unique bare entries below.
In BOTH identities independently hard-code the26-path raw/LF map above.
Require table-path, bare-path and independent-map exact set equality; require
table pair equality and bare hash == map RAW member. A changed plan line cannot
redefine the independent oracle. Recompute the pair-map digest independently.

For each on-disk file accept its raw digest in {pinned RAW,pinned LF} OR its
CRLF→LF digest == pinned LF; independently ALWAYS require canonical digest ==
pinned LF. Thus LF/CRLF checkout representation is accepted, never substantive
drift. Table/map/bare equality is mandatory even when canonical disk bytes pass.
Within F8 and G1 (no extra test methods), materialize each canonical input and
its all-CRLF equivalent in test-managed temporary directories outside source,
exercise the same verifier and require both pass. Mutate one non-newline byte,
add a BOM, alter a table pair/bare hash, add/remove/duplicate a path and require
failure; never rewrite repository inputs. Canonical equality catches real
drift independently of accepting either recorded representation.
Require exactly the18 listed migration names V001–V018 and no V019. G1 also
requires PostgreSQL16/UTF8 and18 successful Flyway versions, exact V018 signed
checksum1606293398 and unchanged predecessor history. F8 retains the independent
employee-verifier boundary assertion. No PA35 migration/grant is introduced.

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db  docs/12-technology-stack-and-system-architecture.md
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205  docs/17-production-security-design-spec.md
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
ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c  backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql
b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9  backend/ops/postgresql/pa25-runtime-identities.sql
44ced2245fc1c89175eeb386bc6cc6fa036302e368829cdd163f3da022425423  docs/operations/pa25-production-database-identity-provisioning-runbook.md
fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64  backend/README.md
```

Current mutable implementation inputs (baseline-only pins; future S1 changes
are allowed only by §7, not treated as frozen outputs):
- `.github/workflows/ci.yml`: raw `e0c9e36f4fc3d2453d74cbddea8055ef81c78785c6d1526b166c9956bc3156bc`; LF `0a70feb1604328c9d90ebe6738b243060336a6976e0c8490d7a3de57969337a2`.
- `backend/pom.xml`: raw `5305544be26437deb8750d3d5e298fca7cfe383ea9adf971ef1e9d739bf7b7d3`; LF `35279bdad18d71dd47a0a92becf8678560802bac748ba1f0dbf8a310d525ef9e`.

## 11. Validation sequence and exact commands (future implementation only)

At this gate: static inspection/hash/source inventory only. Do not build,
run Maven, launch containers, install dependencies or claim runtime validation.
At implementation authorization, execute the complete four-filter design in
§2/§5, not a reduced endpoint. Static checks: exact30-path S1 allowlist,
module graph changes only IVR→Operations named read edge,12→13 discovered
edges/five→six Operations target occurrences; literal SELECT lists/no wildcard,
no DML/lock/audit/
outbox/media APIs, current ring/resources only, exact two routes in both
filters, exact DTO required/null/omitted sets, §8 source identities, §9 CI/POM
arithmetic and §10 raw/LF hashes. V018 remains frozen; exactly18 migration filenames V001–V018 and no V019.

Focused PostgreSQL16 verification (all eight exact new classes plus all four
changed existing test classes, not a wildcard that omits CursorCodec):

```bash
cd backend
./mvnw -Pcontainer-smoke -Dtest=OperationsIvrSessionReadRequestTests,OperationsIvrSessionCursorCodecTests,OperationsIvrSessionReadServiceTests,OperationsIvrSessionReadWebTests,OperationsIvrSessionReadConfigurationTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectReadServiceTests,ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests -Dit.test=OperationsIvrSessionReadsIT,OperationsIvrSessionReadRoleLeastPrivilegeIT,OperationsSubjectReadRoleLeastPrivilegeIT -DfailIfNoTests=true -Dfailsafe.failIfNoSpecifiedTests=true -DskipTests=false -DskipITs=false verify
```

Full PostgreSQL16 verification, authoritative for full XML reconciliation:

```bash
cd backend
./mvnw clean -Pcontainer-smoke verify
```

Require actual PostgreSQL16, successful all18 Flyway migrations and no disabled
container/skipped test. Preserve focused reports outside source before clean;
report exact names and statuses. Never claim inferred totals in place of XML.

After full verify, run the exact existing CI bodies from repository root,
with no weakened copy of their assertions:

```bash
python3 - <<'VALIDATE'
from pathlib import Path
import subprocess
lines = Path('.github/workflows/ci.yml').read_text().splitlines()
for name in ('assert container smoke actually ran',
             'default and opt-in web jar smokes',
             'production jar dependency firewall'):
    matches = [index for index, line in enumerate(lines)
               if line == '      - name: ' + name]
    assert len(matches) == 1, name
    start = matches[0] + 1
    assert lines[start] == '        run: |'
    body = []
    for line in lines[start + 1:]:
        if line and not line.startswith('          '):
            break
        body.append(line[10:] if line else '')
    assert body
    subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', '\n'.join(body)],
                   cwd='backend', check=True)
VALIDATE
```

This asserts full source=CI=POM=XML and default/opt-in packaged web JAR smokes
plus resolved-runtime/JAR dependency firewall. Preserve no production verifier,
no test fixture/class/private identity file in JAR; no new libraries, bypass or
relaxed dependency rule. Capture outputs and JAR SHA-256 in implementation
report; no current JAR result is claimed.

A1–A5 exact CI stages, from repository root with Node22.23.4 and locked A1
installation (`cd api/openapi && npm ci && npm run check:s3`):

```bash
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s1 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s2 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures --check-doc-links
```

A2 is IVR protocol, not merely a frozen-document checker; hashes separately
prove frozen documents. In CRLF checkout use an external disposable LF-equivalent
projection for existing checker conventions, with source raw bytes unchanged;
record that limitation and every exact command/cwd/result. Do not patch frozen
files or validators to manufacture a pass. A5 s1 and s2 both required.

## 12. Residuals, owners and completion/stop

| Residual | Owner | Destination / required evidence |
|---|---|---|
| External Operations read production identity delivery | Platform/Database/Security | Use the merged current PA36 section of docs/operations/pa25-production-database-identity-provisioning-runbook.md and existing four-input PA25 SQL; no PA35 documentation/topology update. Deliver actual credentials, non-superuser login memberships and production proof of all required SET handoffs/restoration, including PA35 Operations/IVR/Evidence; preserve existing reads, missing capability sanitized503, never self-grant; no migration/role/credential added by PA35 |
| Frozen detail artifact provenance metadata omission (A1/A0) | Contract/Data | Contract/Data metadata reconciliation backlog: frozen detail x-onecall-a0-models/docs15 omit evidence.evidence_artifact; required response artifactRoles is authoritative; safe two-column batch only, no public contract or frozen-input edit in PA35 |
| External opaque-session issuer/IdP/MFA/issuance | Identity/Security | Production identity acceptance, outside PA35; no packaged synthetic verifier |
| Key lifecycle/retained keys/production secret delivery | Security/Platform | Existing Operations subject-read deployment secret-custody process (Q17/Q23/deployment readiness); no in-repo runbook currently covers this AES ring. Required external deliverable: new/extended deployment secret runbook naming onecall.operations-subject-read.cursor.keys[i].id/material and cursor.active-key-id, 1–3 keys, unique IDs and exactly one active member, canonical base64url32-byte AES keys, retained-key distribution/rotation across replicas and per-key96-bit random-nonce discipline across all three read endpoints; reuse one ring, never a new PA35 ring; missing/invalid material sanitized503. PA25's customer HMAC-key instructions do not cover it |
| Session-without-required-call corruption assumption | IVR/A2 plus Contract/Data | Documentation clarification of the named invariant at docs14 §9.13/§7.3 and A2 protocol documentation; do not claim reachable-valid behavior; missing call remains whole-detail sanitized503 and frozen inputs unchanged |
| Real-HTTP TRACE concealment beyond activations | Existing B-F04 Operations route-boundary owner | Later Operations route-boundary work; PA35 pins sanitized500 INTERNAL on both new routes via existing ERROR dispatch, no ERROR-branch widening |
| Snapshot/role/pool cleanup and dependency deadlines | Backend/Database | G/H PostgreSQL16 evidence including fault cleanup and backend PID; never partial success |
| Large child chains / five-second read budget | Backend/Operations | Timeout sanitized503, capacity follow-up; no unreviewed truncation/pagination or new indexes |
| Shared-pool and index/OR-scan capacity | Backend/Database/Capacity | docs/18-production-capacity-and-deployment-spec.md volume model plus implementation EXPLAIN evidence for unfiltered/status/subject/task pages and session-side OR reads on ivr_decision/evidence_record: no global (created_at DESC,id DESC) index or session-side indexes; reused Operations pool maximum4/connectionTimeout3000ms, TransactionTemplate REQUIRES_NEW/REPEATABLE_READ/readOnly timeout5s, statement_timeout2000ms, lock_timeout250ms, idle_in_transaction_session_timeout5000ms; contention/deadline exhaustion sanitized503; no index/new migration/pool change |
| PostgreSQL16/container/hosted CI availability | Test/Platform | Authorized focused/full verification and hosted CI artifacts, not plan-gate claims |
| Live/cloud/media capabilities and production readiness | Operations/Infrastructure | Separate acceptance; explicitly not claimed here |

Implementation report must give exact changed paths and counts; route/filter/
authority evidence; column/projection/ordering/omission checks; cursor AAD/digest
and shared-ring proof; transaction/role restore evidence; source=CI=POM=XML
names and totals; focused/full commands and zero-skips outputs; raw/LF frozen
hashes; JAR/firewall/A1–A5 results; and owner-routed residuals. No live,
production-readiness or hosted-CI claim follows from local validation.

No blocking schema/contract/ACL question remains. The owner-routed external
deployment and future validation residuals above are not conditional plan
branches. If implementation discovers a true frozen schema/contract mismatch
or needs any new privilege/migration, stop for new authorization rather than
fabricating a mapping or expanding this slice.

Current stop: write only this cumulative plan and fix-5 evidence, then stop
for Controller intake and fresh independent A/B Planreview. No Control, source,
test, migration, OpenAPI/docs12/14/15/17, CI, POM, existing artifact or private
helper edit; no build/container, stage/commit/push, PR action, merge, approval,
mark-ready, reviewer request or external comment. This author cannot grant
acceptance, authorize implementation or claim runtime/production readiness.
