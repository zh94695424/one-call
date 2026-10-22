# A0 Production Data Model and Initial Flyway Schema Specification Plan

- Task label: `a0-ivr-contract-fix6-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix` round 6
- Status: revised implementation plan after fix round 6 (accepted F-14 serialization-scenario correction plus the OQ-1 category-1 write-set and OQ-2 consent-content sequencing clarifications); no schema specification or migration is produced here
- Date: 2026-10-22
- Inputs: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`, `docs/reviews/plan-review-20261022-113039.md`, `docs/reviews/plan-review-20261022-124546.md`, `docs/reviews/plan-review-20261022-131145.md`, `docs/reviews/plan-review-20261022-133903.md`, `docs/reviews/plan-review-20261022-140542.md`, `docs/reviews/plan-review-20261022-171403.md`, `docs/reviews/plan-review-20261022-173816.md`, the controller fix-round-6 adjudication and user-approved contract refinement, and the design/business sources named in the goal contract
- Current fix artifact: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md` (this round); the round-5, round-4, round-3, round-2 and round-1 artifacts `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-5.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-4.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-3.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-2.md` and `docs/gateflow/a0-production-data-model-schema-spec-plan-fix.md` are preserved as historical evidence and are not modified
- Final specification path: `docs/14-production-data-model-and-flyway-schema-spec.md`
- Planned implementation slices: 3
- Baseline counts derived from the table and invariant catalogs in this plan: 44 tables, 40 invariants, 9 migration waves, 3 slices
- Derived structural counts after fix round 6: 28 mutable-head + 16 append-only tables; 81 FK decisions = 74 ordinary `FK` + 3 late-added `ALTER_FK` + 2 truly `DEFERRABLE_FK` + 2 `NO_FK`; 4 platform/audit functions created in V001 and 1 first-trial function created in V006

The counts above are not Goal Confirmation terms. They are the result of enumerating every independent fact and correctness condition in the confirmed domains. The 44th table (`entitlement_metering.entitlement_issuance`) is inside the confirmed entitlement/issuance goal and is required by accepted finding PR-004. The five added invariants make accepted correctness conditions explicit rather than leaving them implied in prose.

## 1. Goal

Produce a code-generation-ready PostgreSQL 16 data model and initial Flyway schema specification for the first production baseline. The specification must cover account/login identity, organization membership, employee identity, business subject/number binding, consent/activation, IVR task/session/call linkage, trial/entitlement/reservation/usage, catalog/order/payment, evidence/media metadata, sharing authorization, idempotency, Outbox and append-only audit facts.

The deliverable is a specification, not an executable migration. It must define the intended tables, columns and types, ownership, foreign keys, uniqueness and partial uniqueness, state/revision fields, immutable snapshots, sensitive-field representation, deletion policy, transaction controls and migration order so that a later implementation work unit can generate Flyway SQL and module code without inventing business rules.

## 2. Motivation and Success Signal

### Motivation

The repository has reviewed product rules, architecture and executable browser-prototype behavior, but no production database, Maven project or migration baseline. The architecture explicitly places the data model and initial Flyway schema first in the production-specification sequence. A schema created directly from prototypes would confuse local fixtures, browser state and demo-only identifiers with permanent business truth.

### Success signal

Every table and invariant in the final specification has exactly one owning module; collaborating modules are named separately and receive no write privilege. The specification makes all of the following explicit:

- internal identifiers versus protocol identifiers, action/event-domain identities, and which values are never public keys;
- encrypted fields, HMAC lookup indexes, masking, key-version references and per-column prohibited exposure;
- state, revision and immutable event/snapshot columns, plus the revision or append-only rule for every table;
- foreign keys, uniqueness, partial uniqueness, event/action-domain scoping and concurrency controls;
- a named parent, FK/deferred-FK/no-FK decision and wave for every call, session, task, reservation and evidence link;
- a durable, single-owner entitlement-issuance state for every order, installment and entitlement type, with payment status and issuance status independently observable;
- deletion and append-only policy, including exactly one lifecycle class for every table;
- linear Flyway migration order with no forward dependency, deferred cross-wave foreign keys, same-wave creation order for circular links and intra-wave creation order;
- database-enforced, transaction-enforced and service-enforced controls for the critical cross-channel and IVR rules;
- task creation/replacement/cancellation and direct DTMF authorization idempotency, with the canonical-caller lock and the no-task `ENTER_CALLEE` trust boundary on both paths;
- one IVR-owned shared action-selection/claim application service used by INITIAL_RESOLVE (purpose `INBOUND_RESOLVE`) and POST_ACTIVATION (purpose `ACTIVATION_NEXT_ACTION`), with separate public endpoints, server-derived entry mode/purpose, one ordered predicate set, one atomic claim/reservation/decision write set and no additional SQL function;
- canonical initial-resolve and confirmation semantic fingerprints that exclude transport IDs, timestamps and mutable balances/status, plus event-level and call/purpose-level idempotency precedence, cross-call event reuse conflict and rollback of a new idempotency row on semantic conflict;
- replay before any confirmation side effect and before the `SUSPENDED` check, with authenticated immutable session/caller/call identity validated before disclosure, a `CONFIRMED` session never moved to `REJECTED`, and a fresh confirmation allowed only from the initial `GUIDE_ACTIVATION` path;
- an active/unknown-call guard on both fresh decision paths that excludes the current `callId`, persists stable `REJECT_CALL_IN_PROGRESS` without claim/reservation and gives a rejected competing call a valid non-active `call_record` parent while never touching an unrelated competing call;
- exact task-claim authorization and effective-entitlement predicates (current caller/subject/binding/owner-workspace access including organization, membership and owner/creator account lifecycle status where applicable, `PENDING`/unexpired/revision-bound task, positive available units) repeated in the conditional claim and reservation transaction, without imposing a customer login, membership or real-name check on a direct IVR caller who has no task;
- approved risk semantics: `subject_risk_state` `OPEN` blocks number claims and historical-evidence access, not automatically all future dialing; explicit binding/workspace/subject/activation revocation still yields a stable rejection, and a revoked or unauthorized task never falls through to direct dialing;
- a typed immutable action-specific IVR response snapshot with explicit nullability, purpose-discriminated direct-authorization success (`action` null plus committed reservation fields and an authorization-scoped encrypted callee), stable reason and `taskMatch` values, committed-reservation authorization fields, B-leg start cutoff semantics and encrypted-at-rest callee/redaction boundaries;
- one explicit transaction-outcome contract that separates pre-activation refusal, post-activation domain no-dial results (which commit valid activation/trial with the session `CONFIRMED` and no claim/reservation) and technical failure (which rolls back every write, including the `NEW` idempotency row, and commits no decision), with a same-connection attempt savepoint for an expected claim/reservation denial instead of a swallowed SQL exception;
- an explicit consent-gap route (`GUIDE_ACTIVATION` with stable `CONSENT_REQUIRED` on initial resolve), clarified non-existential `taskMatch = NONE` semantics with a positive no-task check before `ENTER_CALLEE` or direct authorization, current-`UNKNOWN` call preservation, a configurable default 30-second pending-task TTL without duplicate refresh, and replay-before-expiry-housekeeping on initial resolve;
- activation confirmation replay and order creation bound to a canonical semantic fingerprint and first immutable result through `platform.idempotency_begin`/`platform.idempotency_complete`, with mandatory action-event identity for `OPERATIONS` transitions, existing-result handling before the `SUSPENDED` check and before consent or trial, fail-closed rejection only on a fresh path, and a persisted/returned post-activation IVR next action that never overwrites the original resolve decision;
- one permanent `FIRST-TRIAL` per subject regardless of campaign key or policy version, with the granting action domain, action event ID and request fingerprint persisted on the trial row;
- reciprocal exactly-one evidence creation-event ownership enforced by a valid composite FK over `(event_domain, event_id)` and atomic insertion;
- reconstructible Redis share-playback tickets derived from durable admissions, without counting an admission twice;
- reservation reconciliation for missing, late or duplicate call-end facts that never releases unknown use;
- safe, atomic cross-schema idempotency begin/complete, Outbox, audit and first-trial function contracts with explicit privileges, creation waves, finalization-once semantics and redaction;
- subject real-name state independent of login, claim, activation and trial;
- deterministic validation that reports duplicate, missing and extra tables and proves table-to-wave-to-slice, lifecycle, sensitive-field, invariant-enforcement and FK-parent-order coverage;
- unresolved production inputs with an owner and destination, rather than invented defaults.

## 3. Non-Goals and Scope Boundary

### Non-goals

- No executable Flyway `.sql` migration, DDL file, Maven/Spring project, Java code, API implementation or UI change.
- No cloud resource, vendor credential, runtime deployment or production data import.
- No microservice split, service mesh, multi-database sharding, event sourcing, generic rules engine or workflow engine.
- No AI, notary, refund, invoice, work-order, online approval, provider-configuration, capacity-sizing or retention-period tables.
- No speculative account merge, login-identity migration, automatic renewal, stacking, refund eligibility or tax workflow.
- No use of HTML prototypes, screenshots or `shared/onecall-business.js` as a migration starting point.

### Scope boundary

This fix-round-6 gate writes only `docs/gateflow/a0-production-data-model-schema-spec-plan.md` and `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`. The round-1 through round-5 fix artifacts, the review artifacts and the controller are read-only historical evidence. A later implementation gate may add the final specification at `docs/14-production-data-model-and-flyway-schema-spec.md`. It may not modify the controller, architecture source, business-rule sources, source code, prototypes, QA screenshots, Git metadata, or the existing review artifacts.

Provider configuration, capacity numbers, retention periods and future online workflows remain deferred. Offline work orders, refunds and invoices remain out of the data model.

## 4. Design-Document Alignment

| Binding source | Rule that controls this plan | Treatment in the final specification |
| --- | --- | --- |
| `docs/12-technology-stack-and-system-architecture.md` §6 | The source lists 13 modules; 11 are in scope for this baseline (`ai-orchestration` and `notary` are Phase D non-goals). No module writes another module's tables; one shared database in the first release | One PostgreSQL schema and database role for each of the 11 in-scope domain owners, plus a non-domain `platform-reliability` owner; cross-module FKs without cross-schema write grants |
| `docs/12` §6 and §10.2 | Synchronous collaboration uses published application services; a single local transaction commits state, constraints and Outbox | Named published service `entitlement-metering.issueFirstTrial` (created in V006 after its tables) and named cross-schema functions for idempotency begin/complete, Outbox and audit, with same-transaction propagation |
| `docs/12` §7 | Account, login identity, organization membership, business subject/number and employee identity are distinct | Separate tables and constraints; no inference from phone possession or organization role |
| `docs/12` §9.1 | IVR confirmation, activation, trial, reservation, settlement and evidence are separate facts; consent, activation and first trial are one transaction; the transaction checks the subject is not suspended and rechecks activation/consent/entitlement before reserving | Separate activation, consent, trial, grant, balance, reservation, usage and evidence tables; the confirmation transaction handles replay first, rejects a fresh `SUSPENDED` before accepted side effects, calls the first-trial service, then the shared action service rechecks and persists the next action |
| `docs/12` §9.1 item 7 | A reservation with no end event is reconciled against authoritative IVR usage; unknown use is never fully released | `RECONCILING` reservation state, reconciliation fields, owned recovery transaction and failure tests |
| `docs/12` §9.2 and `docs/13` §4-§8 | Task claim, reservation and frozen dial authorization share one transaction; one non-terminal task per canonical caller; direct session establishment takes the same lock | Partial unique constraints, canonical-caller transaction lock, active-call guard and one exposed resolve transaction |
| `docs/12` §9.4 and §10.2 | Payment success and entitlement issuance are separate, idempotent transitions; issuance is unique by order, installment and entitlement type | Separate order, payment, issuance, grant, balance and usage facts with domain-specific uniqueness |
| `docs/12` §9.5 | Share grant, verification, admission and revocation generation are server-side facts; successful admission counts once and repeated submission returns the original session | Share grant, admission and access-event tables; durable admission is the authorization fact and Redis playback tickets are derived |
| `docs/12` §10.1 | PostgreSQL is the persistent business truth; Redis is short-lived or reconstructible | No permanent authorization or entitlement fact depends on Redis; ticketing is reconstructible from admissions |
| `docs/12` §10.3 | Evidence, consent, order snapshots, entitlement and audit cannot be physically deleted through ordinary administration; only never-published, unreferenced drafts are a future deletion candidate | No cascading deletes; restricted permissions; append-only facts and status-based retirement; `catalog_product` identity is immutable and only unreferenced unpublished `catalog_version` drafts are a future deletion candidate |
| `docs/13` §4.3, §5 | `PENDING/CLAIMED` task unique by canonical caller HMAC; TTL uses database time and claim rechecks expiry; create/replace/cancel/claim/direct session take the caller lock | Partial unique index, canonical-caller lock, `clock_timestamp()` check after lock, active-call recheck |
| `docs/13` §4.1, §4.3 | Task creation carries `idempotencyKey`/`requestFingerprint`; same key and input returns the existing task without TTL refresh; replacement is workspace-scoped and only for `PENDING`; claimed tasks cannot be replaced or cancelled | Task idempotency columns and constraints, canonical-caller lock on create/replace/cancel/claim, conditional state updates |
| `docs/13` §6.3, §6.5 | Same `callId` returns the first frozen decision; after activation the backend rechecks the still-valid task and entitlement and returns a next action without rewriting the first decision; each `eventId` is unique in the IVR event domain; direct DTMF authorization uses `eventId + callId` and rejects a task ID | One IVR-owned shared action-selection service, canonical per-purpose fingerprints, replay-before-side-effect precedence, distinct `INBOUND_RESOLVE`/`ACTIVATION_NEXT_ACTION` purposes, the exact claim predicates, suspended rejection and post-activation recheck |
| `docs/13` §6.4, §7 | Direct DTMF authorization is only the `ENTER_CALLEE` no-task path; direct call records carry actual caller, callee, access number, leg IDs and timings | Direct authorization reuses the shared identity/active-call/task/entitlement predicate order, takes the caller lock, rechecks no task and no other active call, creates the active call guard and one reservation; success is a purpose-discriminated decision (`action` null), not a routing action |
| `docs/13` §6.2, §7 | `DIAL_TASK_CALLEE` returns frozen `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt`; available seconds are routing-only; `authorizationExpiresAt` is the last instant to start the B-leg; the IVR persists one B-leg operation and never redials on an unknown outcome | A typed immutable action snapshot, committed-reservation authorization values, a redacted/log-safe snapshot plus encrypted internal payload, IVR-local durable B-leg uniqueness, and B-leg cutoff/replay rules |
| `docs/10` §2-§4; `docs/12` §9.1, §9.2 item 5 | `subject_risk_state` `OPEN` blocks number claims and historical-evidence access; activation, task, workspace permission, entitlement and current status govern an outbound call; unregistered/unverified IVR callers may trial | Risk is not converted into an unapproved blanket dialing ban; explicit binding/workspace/subject/activation/organization/account lifecycle denials reject stably; unregistered/real-name state alone does not block IVR guidance, direct activation or trial |
| `docs/12` §9.1, §10.2; `docs/13` §6.3 | Consent, activation and first trial are one transaction; post-confirmation rechecks may produce a normal no-dial call result; technical failure fails closed | Explicit category-1 refusal, category-2 activation-preserving domain no-dial result and category-3 full technical rollback; pre-confirmation and post-activation predicates are sequential phases of one locked transaction |
| `docs/13` §7 | Task, call, legs, reservation, recording and evidence are explicit links; call completion does not imply evidence completion | Named FK parents for every call/session/task/reservation/evidence link and explicit creation timing |
| `docs/09` §4 | `BusinessSubject` carries a real-name state; `ConsentRecord` and `ServiceActivation` transitions are separate facts; each evidence and activation transition is owned by exactly one event | Subject-level `real_name_status`; immutable consent snapshot; transition ledger; reciprocal evidence creation ownership |
| `docs/09` §4 | Activation transitions use `fromRevision/toRevision`; trial uniqueness and order snapshots are stable | Activation transition ledger, one permanent FIRST-TRIAL key, order and grant snapshots |
| `docs/10` §2-§4 | State dimensions are independent; idempotency is scoped by action domain; payment events may be multiple; event IDs may repeat across domains but not within one | Separate independent state columns; domain-scoped idempotency and event identity; payment-event exception recorded explicitly |
| `docs/10` §4 | Same domain and key with changed input is rejected atomically; identical input returns the first immutable result; entitlement result objects cannot be claimed twice | Request fingerprints, first-result snapshots in `platform.idempotency_record` written once by `platform.idempotency_complete`, immutable issuance and grant facts |
| `docs/11` §1-§2 | Login identity is not a business number; claim does not create login identity or rewrite evidence | Separate `auth_identity`, `caller_number_binding`, `subject_claim` and immutable evidence capture fields |

Supporting references from `docs/03-technical-solution.md` and `docs/07-web-capability-hub-spec.md` inform sharing and evidence vocabulary, but they do not override the current baseline in `docs/09` through `docs/13`.

## 5. First-Principles Judgment and Direct Repository Evidence

| Observation | Direct evidence | Consequence for this plan |
| --- | --- | --- |
| There is no production backend or migration baseline to extend | `rg --files` finds no `pom.xml`, Gradle build, `src/main/java` or `db/migration`; `docs/12` line 458 says prototypes are not a production SDK or migration starting point | The final artifact is a new specification, not an incremental migration of existing production tables |
| The browser logic is in-memory and fixture-driven | `docs/10` lines 7-9; `shared/onecall-business.js` lines 12, 650-717 validate copied state and event domains in memory | Use the JavaScript only as behavior evidence; do not copy demo IDs, field shapes or allocation logic |
| PostgreSQL is the required durable truth | `docs/12` lines 59, 134, 348-360 | Constraints, revisions, idempotency, Outbox and audit live in PostgreSQL; Redis is excluded from permanent facts |
| Module boundaries already exist | `docs/12` lines 222-246 list 13 modules, of which `ai-orchestration` and `notary` are Phase D non-goals | Use schema ownership and no cross-schema writes for the 11 in-scope domain owners plus `platform-reliability`; do not create a new service split |
| Cross-module synchronous collaboration needs a named atomic service | `docs/12` line 242 says modules may not update another module's tables; line 295 requires consent, activation and first trial in one transaction; line 366 requires one local transaction with Outbox | A published `issueFirstTrial` service and controlled cross-schema functions are the only permitted cross-owner write path |
| Identity domains are deliberately separate | `docs/11` lines 9, 21-28, 44-48; `docs/09` line 81 | Do not collapse login, business number, subject real-name, organization membership or employee identity into one identity table |
| Trial and activation uniqueness is already fixed | `docs/10` line 20; `docs/09` lines 64, 82-87 | Enforce a permanent `(subject_id, FIRST-TRIAL)` fact and activation revision transitions as first-class constraints |
| Payment success and entitlement arrival are independent and per line/installment | `docs/09` line 86; `docs/10` lines 22, 46; `docs/12` lines 328, 368 | A durable per-order/per-installment/per-type issuance row is required; an order-level entitlement status cannot represent partial issuance |
| IVR atomicity is the critical concurrency boundary | `docs/13` lines 15, 93-115, 208-210, 240-245, 267-271 | Put claim, active-call guard, reservation and frozen decision in one transaction; use partial uniqueness and a canonical-caller lock |
| Direct DTMF authorization is a distinct trust boundary | `docs/13` lines 93-98, 208, 219-220, 228-232 | Direct authorization must take the caller lock, require the `ENTER_CALLEE` no-task path and reject a task-backed call |
| Task creation and direct authorization have separate idempotency/workspace rules | `docs/13` lines 52-67, 100-107, 226-234, 240-247 | Persist task idempotency key/fingerprint, workspace-scoped replacement, and a direct-authorization decision with one reservation per call |
| Call records carry party and leg facts and are the natural call-scoped parent | `docs/13` lines 253-269; `docs/12` line 356 | `call_record.call_id` is the canonical FK parent; it is created in resolve/direct authorization; never infer evidence completion from call completion |
| Evidence ownership is reciprocal | `docs/09` line 91; `docs/10` line 49 | Exactly one `EVIDENCE` creation event owns each evidence record, and the record's source event must match that event; insertion is atomic |
| Missing end events require authoritative reconciliation | `docs/12` line 298; `docs/13` line 271 | Add `RECONCILING` reservation state and owned recovery; never release unknown use |
| Sharing is authorization plus short-lived media access | `docs/12` lines 335-338, 360, 424 | Persist grant and admission facts; derive Redis playback tickets from durable admissions; Redis loss cannot count a second admission |
| Catalog product identity is stable and only draft versions are a future deletion candidate | `docs/12` line 377; `docs/09` line 130 | `catalog_product` is immutable; only never-published, unreferenced `catalog_version` drafts may be considered for future deletion |
| Database-enforced ownership needs defined function privileges | `docs/12` lines 242, 366-372 | Specify function owner, mode, `search_path`, grants, transaction behavior, first-result semantics and redaction before code generation |
| There are no applicable deeper repository instructions | `rg --files -g AGENTS.md` returned no files | Follow the explicit task scope and existing repository style; do not invent additional governance |

Judgment: the minimum safe design is one PostgreSQL database with module-owned schemas, a small technical `platform-reliability` owner for idempotency/Outbox plus schema-wide privilege and lifecycle policy, an append-only `operations-audit` owner, explicit event/fact tables only where history or uniqueness requires them, and cross-module foreign keys with no cross-module write privileges. More tables are justified only when they carry an independent fact that cannot be represented by a constraint, snapshot or status column. The 44th table carries exactly such an independent fact: the durable issuance state for each order, installment and entitlement type.

## 6. Final Specification Target and Required Structure

The implementation produces only:

`docs/14-production-data-model-and-flyway-schema-spec.md`

That document must contain, in order:

1. Scope, status and relationship to `docs/09` through `docs/13`.
2. Database conventions: schemas, roles, identifiers, protocol IDs, action/event domains, types, timestamps, money, enums, revisions, snapshots, canonical hashing, function contracts and prohibited public keys.
3. Module and schema ownership matrix: exactly one owner per table and invariant, named collaborators, cross-module FK rules, published services and write restrictions.
4. Complete table dictionary: purpose, single owner, wave, slice, columns, nullability, keys, checks, indexes, foreign keys, revision/immutable rule, lifecycle/deletion class and sensitive representation.
5. Foreign-key catalog: child column, exact parent column, parent wave, FK creation wave, FK/deferred-FK/no-FK decision and creation timing.
6. Invariant matrix: ID, single owner, collaborators, primary enforcement class (`DB`, `TX` or `SERVICE`), exact constraint/transaction/service location, verified slice and test evidence.
7. State machines for subject claim, activation, activation confirmation, IVR task creation/replacement/cancellation/claim, inbound session, direct DTMF authorization, call record, trial/entitlement/issuance/reservation/usage, catalog/order/payment, evidence and sharing.
8. Sensitive-field inventory and prohibited exposure rules, covering ciphertext, HMAC, masked, redacted and encrypted snapshot/payload columns.
9. Transaction sequences for activation confirmation with first-trial issuance, task creation/replacement/cancellation, IVR claim/reservation, direct DTMF authorization, usage settlement, reservation reconciliation, payment-to-issuance, evidence solidification and share admission with ticket reconstruction.
10. Cross-schema function contracts for idempotency begin/complete, Outbox append, audit append and first-trial issuance: name, creation wave, arguments, returns, owner, security mode, fixed `search_path`, EXECUTE grants, no transaction control, atomic rollback, first-result/replay/finalize-once semantics and redaction/encryption.
11. Linear Flyway migration order mapped to exact tables and indexes, with deferred and same-wave circular foreign keys and explicit intra-wave creation order.
12. Deletion, retention and retention-hook policy without inventing retention periods; every table has a lifecycle class.
13. Deterministic validation commands and expected assertions, including duplicate/missing/extra table reporting, table-to-wave-to-slice coverage, lifecycle and sensitive coverage, invariant owner/enforcement coverage and FK parent order.
14. Open production inputs with owner and destination.
15. Goal-alignment and no-over-design check.
16. Completion report template.

The final document may contain illustrative DDL fragments, but must not include executable migration files or claim that PostgreSQL constraints were run.

## 7. Implementation Decisions and Goal Alignment

| ID | Decision | Goal alignment |
| --- | --- | --- |
| D1 | Produce one specification document at `docs/14-production-data-model-and-flyway-schema-spec.md`; no executable migration | Delivers the required schema specification without entering a later implementation gate |
| D2 | Use one PostgreSQL database with module schemas and one owning database role per module | Makes table ownership and the no-cross-write rule enforceable rather than documentary only |
| D3 | Add a non-domain `platform-reliability` technical owner for `platform.idempotency_record`, `platform.outbox_event`, cross-schema function contracts and schema-wide privilege/lifecycle policy; `operations-audit` owns `audit.audit_fact` | Gives every goal-critical cross-cutting table and policy one owner without duplicating Outbox/idempotency tables per module |
| D4 | Use application-generated UUID primary keys, separate opaque protocol IDs where externally visible, `timestamptz` in UTC, `bigint` cents, `text` enum codes with named `CHECK` constraints, and no PostgreSQL enum types | Stable identifiers and migration-safe state values without natural keys or provider-specific identifiers |
| D5 | Mutable aggregate heads and mutable lifecycle rows carry `revision` or an equivalent conditional state guard; immutable payloads carry stable event identity and SHA-256 | Satisfies state/revision, concurrent update and replay requirements without adding meaningless revisions to append-only facts |
| D6 | Immutable commercial, consent, authorization, entitlement and evidence snapshots use canonical JSON plus SHA-256; core snapshot fields are not updated | Preserves historical facts across catalog changes, claims, rule changes and sharing |
| D7 | Sensitive values use ciphertext plus key-version reference; lookup uses an independent HMAC-SHA-256 index; display uses masked value; snapshots containing protected values are encrypted or redacted and never emitted as plaintext; no OTP, share code, provider subject or permanent media URL is stored | Satisfies sensitive-field handling and prevents public identifiers from exposing phones, WeChat subjects, IDs, callees or object keys |
| D8 | Business facts use status/terminal timestamps, not deletes; all FKs use `ON DELETE RESTRICT`; append-only tables reject `UPDATE`/`DELETE` by privilege and trigger | Satisfies immutable history and deletion policy without inventing legal retention periods |
| D9 | Cross-module FKs are allowed to stable parent keys; cross-schema `INSERT`, `UPDATE` and `DELETE` are not granted; cross-schema writes occur only through published services or the controlled functions in section 11 | Preserves referential integrity while retaining module ownership |
| D10 | Idempotency is unique by `(scope, idempotency_key)`, stores the semantic request fingerprint and is finalized exactly once by `platform.idempotency_complete`; only a `NEW` begin outcome may execute side effects; activation confirmation (`IVR_CONFIRM`) and order creation (`PURCHASE`) use this platform path in the baseline; the confirmation key is the transport `eventId` while the fingerprint includes `callId`, purpose, immutable caller/session/subject identity, prompt/consent version and confirmation content and excludes timestamps and mutable state, so the same event reused on another call conflicts and a regenerated event ID for the same semantic confirmation matches; call/purpose uniqueness is consulted before side effects and a semantic conflict rolls back any `NEW` row; other domain actions add their own side-effect uniqueness | Prevents replay, duplicate side effects and unfillable first results while keeping transport event IDs out of the semantic identity |
| D11 | Outbox insertion is in the same transaction as the domain mutation through `platform.outbox_append`; consumers are at-least-once and idempotent; audit is append-only through `audit.append_fact`, which appends `audit.audit_fact` in schema `audit` | Matches the architecture transaction and audit requirements without distributed transactions and without an undeclared schema |
| D12 | Use the nine migration waves in section 12, including deferred cross-module FKs after both tables exist and explicit intra-wave creation order | Produces a linear, code-generation-ready migration order with no forward references |
| D13 | Use text state codes with named checks rather than PostgreSQL enums | Allows future expand/contract migrations without unsafe enum removal |
| D14 | Exclude AI, notary, refund, invoice, work-order, provider-configuration, capacity and retention tables from the baseline | Prevents goal drift and speculative over-design |
| D15 | Store subject-level real-name status on `subject_number.business_subject`, separate from account-level `customer_iam.identity_profile`; set it only by a verified subject-identity flow and never write it from claim, activation, IVR or trial flows | Preserves the source rule that IVR/claim facts do not create or rewrite real-name or login identity |
| D16 | Store caller, callee and access-number ciphertext/masked values and explicit leg timings in the IVR call/leg records; lookup uses HMAC; unknown line outcomes remain occupied until reconciliation | Keeps IVR party facts and sensitive-field handling explicit without putting raw numbers in public keys or logs |
| D17 | Every invariant has exactly one accountable owner; collaborating modules are named and may only read or call a published service | Makes the success signal testable and prevents shared-ownership gaps |
| D18 | Event/action identity is unique within its actual domain; a table written by multiple domains stores `(event_domain, event_id)` or a composite with the domain; activation transitions store both the action-domain identity and the unique `ACTIVATION_STATUS` fact identity; the same string may exist in different domains | Matches the cross-channel rule that action domains do not collide while replay is rejected within a domain |
| D19 | Cross-wave FK columns may be created before their parent table, but the FK is added in the later wave that creates the parent; same-wave circular FKs are added by `ALTER TABLE` after both tables exist and are deferred only where insertion order requires it | Removes forward references while preserving the nine-wave migration sequence |
| D20 | Order the three slices by dependency: S1 = V001-V003 (platform/identity/subject), S2 = V004-V007 (catalog/order + IVR + entitlement + activation transaction core), S3 = V008-V009 (evidence + sharing) | Every slice migrates and tests only on its own and earlier waves; no slice names a later slice as a prerequisite |
| D21 | Add `entitlement_metering.entitlement_issuance` as the durable per-order/per-installment/per-type issuance state, and remove the order-level entitlement status column | Makes partial issuance, retries, attempt counts and grant linkage observable without a cross-module write |
| D22 | Expose `entitlement-metering.issueFirstTrial` as a published service invoked with `Propagation.MANDATORY` inside the activation transaction; it alone writes `trial_grant`, `entitlement_grant`, `entitlement_balance` and Outbox | Preserves module ownership while satisfying the source's atomic consent/activation/first-trial rule |
| D23 | Direct DTMF authorization takes the canonical-caller lock, requires the session's `ENTER_CALLEE` no-task path, rechecks no `PENDING/CLAIMED` task and no other `ACTIVE/UNKNOWN` call excluding the current `callId`, reuses the shared identity/activation/consent/entitlement predicate order, creates or binds the active `call_record` in the same transaction, and returns a purpose-discriminated success with `action` null plus the committed reservation fields rather than a routing action | Closes the direct-path concurrency and trust-boundary gap and keeps both IVR entries on one predicate contract without an eighth action |
| D24 | Make `ivr.call_event` IVR-event-domain-specific with unique `(event_domain, event_id)`; make `ivr_decision` source-event uniqueness include the source domain; add the source domain to `usage_fact` and other multi-domain references | Implements the stated event-domain invariant instead of only describing it |
| D25 | Activation confirmation uses `platform.idempotency_record` with scope `IVR_CONFIRM`, an `eventId` key, the canonical confirmation fingerprint and a frozen first result; under the canonical-caller/call locks it authenticates and validates immutable identity, then handles an existing `(call_id, ACTIVATION_NEXT_ACTION)` decision and the event-level `REPLAY`/`CONFLICT` outcome before any `SUSPENDED` check or consent/trial/claim/reservation effect; a matching semantic fingerprint with a new event ID completes that event's `NEW` row with the existing decision ID and frozen result, a changed fingerprint returns a stable conflict with the `NEW` row rolled back, and a `CONFIRMED` session is never changed to `REJECTED`; a fresh confirmation is allowed only when the original `(call_id, INBOUND_RESOLVE)` decision is `GUIDE_ACTIVATION`; `service_activation_transition` requires a non-null action event ID for every domain, including `OPERATIONS`; after a non-suspended confirmation and first-trial resolution the shared IVR service selects and persists exactly one `ACTIVATION_NEXT_ACTION` decision, whose non-null decision ID is the stable first result for every branch including already `ACTIVE` and call-rejected outcomes | Makes activation replay deterministic, preserves the first decision and session state, and returns the required post-activation action without overwriting the original resolve decision |
| D26 | Use `ivr.call_record.call_id` as the canonical call-scoped FK parent; create or bind the current call as `ACTIVE` before a non-terminal decision in resolve and direct authorization, and create a pre-empted competing call as terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS` so its `REJECT_CALL_IN_PROGRESS` decision still has a valid non-active parent; exclude the current `callId` from the guard and never update another call's row; use the partial unique active-caller index only as a backstop | Gives every call link one exact parent, lets a rejected call be represented without violating the one-active-call guard, and fixes creation timing and migration order |
| D27 | Fix the baseline trial campaign identity to `FIRST-TRIAL` on both `trial_policy_version` and `trial_grant`, and enforce unique `subject_id` on `trial_grant` | Prevents a second trial through a new campaign key, a policy version change, a claim or a cross-channel import |
| D28 | Enforce reciprocal evidence creation ownership with a unique creation event per evidence record, a composite FK `(source_event_domain, source_event_id) → evidence_event(event_domain, event_id)`, the reverse `evidence_event.evidence_id → evidence_record.id` FK, both `DEFERRABLE INITIALLY DEFERRED`, and a deferred constraint trigger that validates exactly-one identity at commit | Makes exactly-one creation-event ownership and atomic insertion valid PostgreSQL and database-enforced |
| D29 | Treat the durable PostgreSQL admission as the authorization fact; derive the Redis ticket key from `share_grant_id + admission_id`; same-key retry returns the admission and reissues the ticket without incrementing; record ticket issue/failure as access events | Preserves count-once semantics across Redis failure while keeping Redis reconstructible and non-authoritative |
| D30 | Add `HELD → RECONCILING → SETTLED/RELEASED` reservation handling, reconciliation fields, an owned recovery transaction and tests for missing, late and duplicate call-end facts; never release unknown use | Prevents silent undercharging or permanently stranded entitlement |
| D31 | Classify `catalog_product` as immutable stable identity with no draft-delete exception; only never-published, unreferenced `catalog_version` drafts are a future deletion candidate, with no deletion in the baseline | Removes the lifecycle contradiction and aligns with the source deletion policy |
| D32 | Specify cross-schema function contracts for idempotency begin/complete, Outbox, `audit.append_fact` and first-trial issuance: creation wave, owner, `SECURITY DEFINER`, fixed `search_path`, EXECUTE grants, no transaction control, exception propagation, first-result/replay/finalize-once and redaction/encryption | Makes the DB-enforced ownership model code-generation-ready and privilege-testable |
| D33 | Add a machine-readable baseline matrix, rendered wave and slice membership, a key catalog and FK matrix plus deterministic validation that reports duplicate/missing/extra tables and proves wave/slice/lifecycle/sensitive/invariant/FK coverage | Replaces count-only green checks with evidence for the actual structural claims |
| D34 | Make `platform.idempotency_record` a mutable-head row with `status NEW → COMPLETED`, `completed_at` and `revision`: `begin` inserts `NEW`, and `platform.idempotency_complete` conditionally fills the result exactly once in the same transaction; `NEW` alone may execute side effects; a committed incomplete row is an alertable contract violation | Gives idempotency a realizable first-result finalization path without calling an updated row append-only |
| D35 | Bind order creation to event domain `PURCHASE`: `platform.idempotency_begin('PURCHASE', account_id || ':' || idempotency_key, request_fingerprint)` and `platform.idempotency_complete` with the first order ID and frozen snapshot in the same order transaction; `customer_order.request_fingerprint` is persisted | Makes identical retry return the first order and changed input conflict atomically |
| D36 | Persist `source_action_domain`, `source_action_event_id` and `request_fingerprint` on `entitlement_metering.trial_grant` with unique `(source_action_domain, source_action_event_id)`; distinguish `GRANTED`, `REPLAY` and `ALREADY_GRANTED` without ever creating a second grant or balance | Makes first-trial replay, changed-fingerprint conflict and already-granted-by-another-action behavior implementable and auditable |
| D37 | Treat the evidence record/creation-event link as one bidirectional cycle: both FKs are `DEFERRABLE INITIALLY DEFERRED`, added by exact `ALTER TABLE` statements after both V008 tables exist, with a commit-time constraint trigger validating exactly one `EVIDENCE_CREATED` event whose identity equals the record's source | Removes the invalid single-column FK target and makes the reciprocal rule valid PostgreSQL |
| D38 | Split FK creation modes into ordinary `FK`, late-added `ALTER_FK` and truly `DEFERRABLE_FK`; validate every referenced column set against a machine-readable PK/UNIQUE key catalog and compare the §8 catalog with rendered §12 wave and §13 slice membership | Makes the structural validation prove target validity and rendered membership instead of trusting counts and field shape |
| D39 | Fail closed on a `SUSPENDED` subject only on a fresh confirmation path, after identity validation and after existing-decision/event replay handling: acquire the canonical-caller lock and a subject-scoped lifecycle lock taken by every activation lifecycle writer (IVR confirmation and operations suspend/resume), re-read the activation status, reject with stable `REJECT_SUSPENDED`, mark the inbound session `REJECTED` only when it is not already `CONFIRMED`, and write only one rejected `ivr.ivr_decision` plus an optional audit fact; this is a category-1 pre-activation refusal and writes no accepted consent/transition/trial/grant/balance/reservation/success Outbox; otherwise the IVR activation-confirmation wrapper calls `activation-consent.record_activation_confirmation`, `entitlement-metering.issue_first_trial` and then the IVR-owned shared action-selection service | Closes the suspended-subject entitlement leak, serializes confirmation against operations suspension, never corrupts a confirmed session on replay and keeps `ivr.ivr_decision` ownership inside `ivr` |
| D40 | Define one IVR-owned application service `ivr.select_call_action` shared by INITIAL_RESOLVE (`INBOUND_RESOLVE`) and POST_ACTIVATION (`ACTIVATION_NEXT_ACTION`); public endpoints, purposes and stored decisions stay separate; entry mode and purpose are derived from the authenticated endpoint/session state, never from a caller-supplied field; `ivr.record_activation_next_action` remains a thin IVR entry wrapper that delegates to the shared service and adds no predicate logic | Removes behavioral drift between the two entry paths without adding a fourth slice, a generic workflow engine, a table or a SQL `SECURITY DEFINER` function |
| D41 | Define canonical per-purpose semantic fingerprints and explicit idempotency precedence: the initial-resolve fingerprint covers purpose, protocol version, `callId`, canonical caller HMAC, access-number identity, node identity and immutable session/binding identity; the confirmation fingerprint additionally covers prompt/consent version and confirmation key/content; both exclude transport event IDs, timestamps and mutable balance/status; never compare a confirmation fingerprint with an initial-resolve fingerprint; event-level `REPLAY`/`CONFLICT` and call/purpose existing-decision handling precede side effects and `SUSPENDED`; changed input rolls back any `NEW` row before a stable conflict is returned | Makes event identity and call/purpose identity coexist deterministically and prevents an opaque uniqueness failure or a stale replay |
| D42 | Apply one ordered active/unknown-call predicate on both fresh paths under the canonical-caller lock: reject when another call with the same canonical caller is `ACTIVE`/`UNKNOWN` and its `call_id` differs from the current call, persist `REJECT_CALL_IN_PROGRESS` with no task claim and no reservation, and treat a partial unique violation as a backstop that is converted to the same stable result only after a deterministic reread; never block a call on its own current row | Gives both entry modes one stable concurrency outcome and prevents a raw index error from becoming the protocol response |
| D43 | Ground outbound-call blocking in the approved access/lifecycle policy. For a task-backed call, the task, its subject/binding and its owner workspace must be currently usable: an organization task requires `organization.status = 'ACTIVE'`, the creator `customer_account.status = 'ACTIVE'` and an `ACTIVE` membership whose current role and resource access still grant task access; a personal-owner task requires the applicable owner and creator account rows to be `ACTIVE` (the same row when they coincide); the subject owner, binding and activation/consent must remain valid. For a direct IVR call with no task, apply only the checks that exist for that path — trusted caller/binding when registered, subject lifecycle, activation/consent and entitlement — and do not require a customer login, organization membership, real-name status or owner account; unregistered/unverified IVR callers remain eligible for direct activation/trial/recording. `subject_risk_state` `OPEN` blocks number claims and historical-evidence access and is not an automatic dialing ban; a future call-blocking risk rule is a named deferred policy input, not an invented predicate; a revoked or unauthorized task returns a stable rejection and never falls through to direct dialing | Prevents an unsupported blanket risk ban, closes the organization/account lifecycle gap and prevents authorization bypass through the no-task path |
| D44 | Define a typed immutable action-specific IVR response snapshot with fixed fields, enums and nullability. `DIRECT_AUTHORIZATION` is a decision purpose, not a routing action: a successful direct authorization has `action` null/omitted, the committed `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt`, and the confirmed callee only inside the encrypted internal payload under an authorization-scoped field (never in task fields); a direct refusal uses one of the existing non-dial actions with no reservation; `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION` always carry one of the seven non-null actions. Authorization values come only from the committed reservation, with the baseline recording unit equal to one second; the full callee exists only in `response_payload_ciphertext` for the authenticated IVR internal response, while `response_snapshot`, logs, audit and customer responses are masked/redacted; `authorizationExpiresAt` is the last instant to start the B-leg and replay never renews it | Makes the public IVR contract code-generation-ready without an eighth action and fixes the direct-purpose, callee-placement, redaction, cutoff and lost-response boundaries |
| D45 | Define one explicit transaction-outcome contract for every fresh decision path. (1) A refusal before accepted consent/activation/trial (fresh `SUSPENDED`, invalid confirmation origin/content, identity failure, or the pre-confirmation active/`UNKNOWN`-call guard) commits no accepted consent/transition/trial/grant/balance/reservation and follows the existing replay/idempotency behavior; a current `UNKNOWN` row is never destructively finalized. (2) A normal domain no-dial result after valid consent/activation/trial (revoked task/workspace/binding/owner/creator access, zero entitlement, task expiry/cancellation/replacement, competing active call, expected reservation denial) commits the valid activation/trial, keeps the session `CONFIRMED`, commits one frozen no-dial decision and its completion, and commits no claim/reservation; a tentative claim/reservation is undone through a same-connection savepoint taken after the activation/trial writes and before that claim. (3) A technical failure (SQL/lock timeout/storage/decision/completion/Outbox error) rolls back every write including the `NEW` row, activation/trial and any tentative claim/reservation, commits no decision and returns a bounded retryable error; no database exception is swallowed. Pre-confirmation guards and the post-activation shared fresh selection are explicit sequential phases of the one locked transaction, not contradictory orders | Keeps business activation and call authorization as separate observable facts, removes the dual rollback promise and preserves one local transaction |
| D46 | Adopt the controller's related clarifications inside the shared contract: an ACTIVE activation whose required current consent is missing/expired returns `GUIDE_ACTIVATION` with stable `CONSENT_REQUIRED` on initial resolve so the same confirmation path appends current consent without reissuing the trial, while stale/invalid confirmation content is rejected before accepted side effects; `taskMatch = NONE` means no candidate was selected/evaluated by this decision (early refusals), not proof that no task exists, and `ENTER_CALLEE`/direct authorization require a positive no-valid-task/no-`PENDING`/`CLAIMED` check; the unique durable B-leg operation lives in the existing IVR platform's local authoritative store keyed by `callId` and persisted before the line call, while business `ivr.call_leg` stores callbacks/facts and proves no local outbound uniqueness (implementation deferred to the IVR work unit, no business schema expansion); a fresh attempt on a current `UNKNOWN` call with inconsistent line state returns a stable reconciliation/integrity refusal without destructive state change, new reservation/dial or `UNKNOWN`-to-`REJECTED`/release, and only a never-started/rejectable current `ACTIVE` row may be finalized `REJECTED`; the default pending-task TTL is 30 seconds, configurable server-side, frozen into the task and never refreshed by a duplicate; initial-resolve replay always precedes task-expiry housekeeping or any other mutation | Resolves the consent-gap, early `taskMatch`, B-leg, `UNKNOWN`, TTL and replay-order ambiguities without adding an action, table, function, slice or production configuration key |

## 8. Exact Table Catalog and Ownership

All tables use an application-generated `uuid` primary key named `id`, `timestamptz` times, and named `CHECK` constraints for state codes. The column lists below are the minimum contract; the final specification expands types, nullability, indexes, FK actions and comments without changing the table set. Wave and slice columns are part of the ownership contract and must be repeated by the final specification.

<!-- catalog:start -->
| Table | Owning module | Wave | Slice | Minimum columns and keys | Critical constraints and lifecycle |
| --- | --- | --- | --- | --- | --- |
| `platform.idempotency_record` | `platform-reliability` | V001 | S1 | `id`, `scope`, `idempotency_key`, `request_fingerprint`, `status`, `result_type`, `result_id`, `result_snapshot`, `result_sha256`, `created_at`, `completed_at`, `revision` | Unique `(scope, idempotency_key)`; status `NEW/COMPLETED`; `NEW` requires all result columns and `completed_at` to be NULL, `COMPLETED` requires all result columns and `completed_at` to be non-null; `platform.idempotency_complete` conditionally fills the result exactly once and sets `COMPLETED`, `completed_at`, `revision + 1`; a completed result is never overwritten; the stored fingerprint is the canonical semantic fingerprint for its scope, not a transport timestamp; same key/different fingerprint is a stable conflict; for `IVR_CONFIRM` the key is `eventId` and the fingerprint includes `callId`, so the same event reused on another call conflicts while a regenerated event ID with identical semantic confirmation matches and replays the first result; `NEW` is the only outcome that may execute side effects and a semantic conflict rolls the `NEW` row back; `revision` starts at 1; snapshot is redacted or encrypted; mutable head with a finalize-once result |
| `platform.outbox_event` | `platform-reliability` | V001 | S1 | `id`, `event_domain`, `dedupe_key`, `aggregate_type`, `aggregate_id`, `event_type`, `payload_redacted`, `payload_ciphertext`, `payload_sha256`, `status`, `attempt_count`, `available_at`, `published_at`, `revision` | Unique `(event_domain, dedupe_key)`; inserted in the domain transaction through `platform.outbox_append`; payload is redacted or encrypted and immutable; dispatcher state transitions are conditional and bounded by `revision`; mutable head |
| `audit.audit_fact` | `operations-audit` | V001 | S1 | `id`, `occurred_at`, `actor_type`, `actor_id`, `action_code`, `object_type`, `object_id`, `reason_code`, `before_snapshot`, `after_snapshot`, `request_id`, `trace_id` | Append-only; no update/delete privilege; inserted only through `audit.append_fact` in schema `audit`; actor and object identifiers remain internal; snapshots are redacted or encrypted |
| `customer_iam.customer_account` | `customer-iam` | V002 | S1 | `id`, `display_name`, `status`, `revision`, `created_at`, `updated_at` | Status `ACTIVE/DISABLED`; revision conditional updates; no public natural key; mutable head |
| `customer_iam.auth_identity` | `customer-iam` | V002 | S1 | `id`, `account_id`, `provider`, `provider_scope`, `provider_subject_ciphertext`, `provider_subject_hmac`, `status`, `verified_at`, `key_version`, `revision` | Unique `(provider, provider_scope, provider_subject_hmac)`; no nickname as subject; one identity maps to one account; status changes use conditional revision; mutable head |
| `customer_iam.identity_profile` | `customer-iam` | V002 | S1 | `id`, `account_id`, `real_name_status`, `legal_name_ciphertext`, `id_number_ciphertext`, `id_number_hmac`, `verification_ref_ciphertext`, `verified_at`, `revision` | Unique `account_id`; account-level real-name state is independent of login success and business-number possession; it is not the subject-level real-name fact; mutable head |
| `organization_access.organization` | `organization-access` | V002 | S1 | `id`, `display_name`, `status`, `revision`, `created_at`, `updated_at` | Status `ACTIVE/SUSPENDED/DISABLED`; no login identity; mutable head |
| `organization_access.organization_membership` | `organization-access` | V002 | S1 | `id`, `organization_id`, `account_id`, `role`, `department_label`, `status`, `revision` | Partial unique `(organization_id, account_id)` for non-revoked membership; roles fixed to administrator/member/evidence manager/auditor; mutable head |
| `operations_iam.employee_account` | `operations-iam` | V002 | S1 | `id`, `login_name_hmac`, `login_name_masked`, `auth_issuer`, `auth_subject_hmac`, `display_name`, `role`, `status`, `revision` | Unique `login_name_hmac` and `(auth_issuer, auth_subject_hmac)`; role `ADMIN/OPS`; credentials remain outside business tables; last enabled admin is transaction-protected; mutable head |
| `subject_number.business_subject` | `subject-number` | V003 | S1 | `id`, `owner_account_id`, `owner_organization_id`, `source`, `real_name_status`, `real_name_verified_at`, `revision`, `created_at`, `updated_at` | At most one owner of type personal account or organization (`num_nonnulls(...) <= 1`); an IVR-created subject may be unowned until a verified claim; claim sets the owner only when null and conflicts if a different owner exists; subject real-name is separate from activation and login; claim/IVR never writes it; mutable head |
| `subject_number.caller_number_binding` | `subject-number` | V003 | S1 | `id`, `subject_id`, `number_type`, `number_ciphertext`, `number_hmac`, `number_masked`, `status`, `verified_at`, `revision` | Partial unique `number_hmac` for `ACTIVE`; unique `(subject_id, number_hmac)`; historical retired bindings remain; mutable head |
| `subject_number.subject_claim` | `subject-number` | V003 | S1 | `id`, `subject_id`, `account_id`, `organization_id`, `verification_method`, `verification_ref_ciphertext`, `request_id`, `status`, `risk_checked_at`, `verified_at` | Unique `request_id`; partial unique `(subject_id, account_id)` where `status = ACCEPTED`; status is set at insert and never updated; rejected or conflict attempts remain append-only; append-only |
| `subject_number.subject_risk_state` | `subject-number` | V003 | S1 | `id`, `caller_number_binding_id`, `risk_type`, `state`, `source`, `observed_at`, `cleared_at`, `revision` | Unique `(caller_number_binding_id, risk_type)`; `OPEN` risk blocks number claims and historical-evidence access regardless of replay; it is not by itself a ban on new outbound calling, and any future call-blocking risk predicate requires a separately approved policy; mutable head |
| `catalog_order.catalog_product` | `catalog-order` | V004 | S2 | `id`, `product_code`, `product_family`, `created_at` | Unique `product_code`; immutable stable product identity; no draft status and no product draft-delete exception; append-only |
| `catalog_order.catalog_version` | `catalog-order` | V004 | S2 | `id`, `product_id`, `version_no`, `sale_status`, `offer_type`, `audience`, `price_cents`, `currency`, `billing_cycle`, `installments`, `grant_interval_months`, `snapshot`, `snapshot_sha256`, `created_by_employee_id`, `created_at`, `published_at`, `revision` | Partial unique active version per product; `DRAFT/ACTIVE/OFFSALE`; commercial content immutable after publication; draft content and sale status use conditional revision; type/billing/grant matrix checks; enterprise quote cannot use personal values; only never-published and unreferenced drafts are a future deletion candidate, not implemented here; mutable head |
| `catalog_order.catalog_offer` | `catalog-order` | V004 | S2 | `id`, `catalog_version_id`, `channel`, `offer_status`, `version_token`, `created_at`, `revision` | Unique `(catalog_version_id, channel)`; `version_token` is content/channel-specific; offer status uses conditional revision; withdrawn offers stop new orders only; mutable head |
| `catalog_order.catalog_benefit_line` | `catalog-order` | V004 | S2 | `id`, `catalog_version_id`, `entitlement_type`, `quantity`, `validity_days`, `grant_interval_months` | Unique `(catalog_version_id, entitlement_type)`; positive values only; immutable with the version; append-only |
| `catalog_order.customer_order` | `catalog-order` | V004 | S2 | `id`, `order_no`, `account_id`, `owner_organization_id`, `subject_id`, `channel`, `catalog_version_id`, `catalog_offer_id`, `purchase_consent_id`, `amount_cents`, `currency`, `payment_status`, `snapshot`, `snapshot_sha256`, `version_token`, `idempotency_key`, `request_fingerprint`, `revision`, `created_at`, `paid_at`, `closed_at` | Unique `order_no` and `(account_id, idempotency_key)`; order creation uses platform scope `PURCHASE` with key `account_id || ':' || idempotency_key`, begin/fingerprint/first-order-result completion in the same transaction; identical retry returns the first order and changed input conflicts; channel `MINIAPP/WEB`; amount, snapshot, fingerprint and consent reference immutable; payment status only; entitlement status is not stored and is derived from `entitlement_issuance`; `purchase_consent_id` ordinary FK is added in V007; mutable head |
| `catalog_order.payment_order` | `catalog-order` | V004 | S2 | `id`, `order_id`, `provider`, `merchant_order_no`, `provider_transaction_id`, `status`, `amount_cents`, `currency`, `created_at`, `updated_at`, `revision` | Unique `order_id`, `merchant_order_no` and non-null provider transaction; status changes only from verified provider evidence; mutable head |
| `catalog_order.payment_event` | `catalog-order` | V004 | S2 | `id`, `payment_order_id`, `provider`, `provider_event_id`, `event_type`, `signature_verified`, `payload_ciphertext`, `payload_sha256`, `transaction_id`, `amount_cents`, `occurred_at`, `received_at` | Unique `(provider, provider_event_id)`; append-only; multiple verified events may advance one order; unverified events never advance payment; append-only |
| `ivr.outbound_call_task` | `ivr` | V005 | S2 | `id`, `task_id`, `caller_binding_id`, `subject_id`, `owner_account_id`, `owner_organization_id`, `created_by_account_id`, `caller_number_hmac`, `callee_ciphertext`, `callee_masked`, `business_type`, `status`, `reason_code`, `ttl_seconds`, `expires_at`, `idempotency_key`, `request_fingerprint`, `claimed_call_id`, `claimed_at`, `terminal_at`, `created_at`, `updated_at`, `revision` | Unique `task_id`; unique `(created_by_account_id, idempotency_key)`; same key/input returns the existing task without TTL refresh; same key/different fingerprint is a conflict; an identical caller/callee/business/workspace `PENDING` duplicate may return the existing task without TTL refresh; the default `ttl_seconds` is 30 at creation, server-configurable, frozen into `expires_at`, and never refreshed by a duplicate; exactly one of `owner_account_id`/`owner_organization_id`; partial unique `caller_number_hmac` for `PENDING/CLAIMED`; unique claimed call; a task is claimable only by the shared IVR action service when it is `PENDING`, bound to the current trusted caller/subject/binding and owner workspace, its creator still has current task access, its revision matches, `expires_at > clock_timestamp()` is re-evaluated in the conditional claim update, and a reservation commits in the same transaction; for an organization task, `organization.status = 'ACTIVE'`, the creator account is `ACTIVE` and the creator membership is `ACTIVE` with the current task-access role and resource access; for a personal-owner task, the applicable owner and creator account rows are `ACTIVE`; those lifecycle rows are locked in the shared order before the claim; `claimed_call_id` FK is added in V005 after `call_record`; mutable head |
| `ivr.inbound_session` | `ivr` | V005 | S2 | `id`, `call_id`, `subject_id`, `trusted_caller`, `caller_ciphertext`, `caller_number_hmac`, `caller_masked`, `access_number_masked`, `node_id`, `protocol_version`, `prompt_version`, `status`, `confirmed_subject_id`, `guidance_status`, `created_at`, `confirmed_at`, `revision` | Unique `call_id`; `call_id` is the root IVR protocol identity with no parent FK; created in resolve/direct authorization; a fresh `POST_ACTIVATION` confirmation is allowed only when the original `(call_id, INBOUND_RESOLVE)` decision is `GUIDE_ACTIVATION`; `CONFIRMED` requires trusted caller, key `1`, current consent version, same confirmed subject, a non-suspended subject re-read under the subject lifecycle lock and exactly one `ACTIVATION_NEXT_ACTION` decision committed with it; a `SUSPENDED` subject cannot reach `CONFIRMED`; a confirmed session is never moved to `REJECTED` by replay; mutable head |
| `ivr.call_record` | `ivr` | V005 | S2 | `id`, `call_id`, `session_id`, `task_id`, `subject_id`, `caller_ciphertext`, `caller_number_hmac`, `caller_masked`, `callee_ciphertext`, `callee_masked`, `access_number_masked`, `status`, `node_id`, `protocol_version`, `started_at`, `bridged_at`, `recording_started_at`, `recording_ended_at`, `ended_at`, `authoritative_seconds`, `end_reason`, `revision` | Unique `call_id` and `session_id`; unique non-null `task_id`; partial unique active `caller_number_hmac` for `ACTIVE/UNKNOWN`; canonical call-scoped FK parent; a live call is created or bound as `ACTIVE` before a non-terminal decision/reservation, while a call pre-empted by another active call is created terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS` so its `REJECT_CALL_IN_PROGRESS` decision still has a parent; the other-active-call guard excludes the current `callId`; only a never-started/rejectable current `ACTIVE` row may be finalized `REJECTED`, while a fresh attempt on a current `UNKNOWN` call with inconsistent line state returns a stable reconciliation/integrity refusal and never finalizes `UNKNOWN` to `REJECTED` or releases it; a terminal reject touches only the current call's own row and never an unrelated competing call; the partial unique index is a backstop; party fields are ciphertext/masked and immutable; mutable head |
| `ivr.call_leg` | `ivr` | V005 | S2 | `id`, `call_id`, `leg_id`, `leg_type`, `direction`, `state`, `started_at`, `answered_at`, `ended_at`, `end_reason`, `revision` | Unique `(call_id, leg_id)`; FK to `call_record.call_id`; caller and callee legs belong to one root call; state changes use conditional revision; mutable head |
| `ivr.call_event` | `ivr` | V005 | S2 | `id`, `call_id`, `event_domain`, `event_id`, `sequence_no`, `event_type`, `occurred_at`, `received_at`, `payload_ciphertext`, `payload_redacted`, `payload_sha256`, `processing_status`, `reason_code`, `revision` | `event_domain` check `IVR_EVENT`; unique `(event_domain, event_id)` so an event ID cannot repeat on another call; partial unique `(call_id, sequence_no)` when supplied; FK to `call_record.call_id`; processing status uses conditional revision; mutable head |
| `ivr.ivr_decision` | `ivr` | V005 | S2 | `id`, `session_id`, `call_id`, `purpose`, `source_event_domain`, `source_event_id`, `request_fingerprint`, `action`, `reason_code`, `response_snapshot`, `response_payload_ciphertext`, `response_sha256`, `key_version`, `reservation_id`, `decided_at` | Unique `(call_id, purpose)`; purposes are `INBOUND_RESOLVE`, `DIRECT_AUTHORIZATION` and `ACTIVATION_NEXT_ACTION`, so the post-activation decision never overwrites the original frozen `GUIDE_ACTIVATION` resolve decision; partial unique `(call_id, source_event_domain, source_event_id)` when supplied; same call/purpose with a different canonical fingerprint is a conflict, while the same fingerprint returns the existing decision and frozen result before any side effect; `action` is one of `GUIDE_ACTIVATION`, `DIAL_TASK_CALLEE`, `ENTER_CALLEE`, `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED` and `REJECT_CALL_IN_PROGRESS` and is NOT NULL for `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION`; `DIRECT_AUTHORIZATION` is a purpose, not an eighth action, so a successful direct authorization has `action` NULL with a non-null `reservation_id`, while a direct refusal uses one of the permitted non-dial actions (`GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS`) with a null `reservation_id` and never `DIAL_TASK_CALLEE` or `ENTER_CALLEE`; a named CHECK enforces this purpose/action/nullability shape; `reason_code` uses the stable registry in section 11; `response_snapshot` is the typed redacted action snapshot and `response_payload_ciphertext` is the encrypted internal payload (full task callee only for `DIAL_TASK_CALLEE` under `task.calleeNumber`, and the full confirmed callee only for successful `DIRECT_AUTHORIZATION` under `authorization.calleeNumber`); `ACTIVATION_NEXT_ACTION` and `INBOUND_RESOLVE` are written only through the IVR-owned `ivr.select_call_action` service, with `ivr.record_activation_next_action` as a thin entry wrapper; FK to `call_record.call_id` and `inbound_session`; `reservation_id` FK is added in V006; frozen append-only decision |
| `entitlement_metering.trial_policy_version` | `entitlement-metering` | V006 | S2 | `id`, `campaign_key`, `version_no`, `granted_seconds`, `valid_days`, `enabled`, `snapshot`, `snapshot_sha256`, `published_by_employee_id`, `published_at`, `revision` | `campaign_key` check `FIRST-TRIAL`; unique `(campaign_key, version_no)`; partial unique enabled current campaign; content snapshot is immutable after publication while lifecycle flags use conditional revision; mutable head |
| `entitlement_metering.entitlement_grant` | `entitlement-metering` | V006 | S2 | `id`, `subject_id`, `source_type`, `source_ref_id`, `entitlement_type`, `granted_units`, `valid_from`, `expires_at`, `order_id`, `installment_no`, `snapshot`, `snapshot_sha256`, `granted_at` | Source exclusivity checks; paid partial unique `(order_id, installment_no, entitlement_type)`; trial source uniqueness delegates to `trial_grant`; immutable grant |
| `entitlement_metering.trial_grant` | `entitlement-metering` | V006 | S2 | `id`, `subject_id`, `campaign_key`, `trial_policy_version_id`, `entitlement_grant_id`, `source_action_domain`, `source_action_event_id`, `request_fingerprint`, `granted_at`, `expires_at` | `campaign_key` check `FIRST-TRIAL`; unique `subject_id` permanently; unique `entitlement_grant_id`; unique `(source_action_domain, source_action_event_id)`; source identity and fingerprint are non-null and immutable; same action identity plus same fingerprint replays the original ids, same action identity plus changed fingerprint conflicts, and another action for an already-granted subject returns `ALREADY_GRANTED` original ids without a second grant or balance; grant facts immutable; append-only |
| `entitlement_metering.entitlement_balance` | `entitlement-metering` | V006 | S2 | `id`, `grant_id`, `granted_units`, `reserved_units`, `consumed_units`, `status`, `revision`, `updated_at` | Unique `grant_id`; non-negative units; `reserved_units + consumed_units <= granted_units`; updated only by reservation/settlement transaction; mutable head |
| `entitlement_metering.entitlement_issuance` | `entitlement-metering` | V006 | S2 | `id`, `order_id`, `installment_no`, `entitlement_type`, `status`, `attempt_count`, `revision`, `last_error_code`, `grant_id`, `source_event_domain`, `source_event_id`, `first_attempt_at`, `issued_at`, `updated_at` | Unique `(order_id, installment_no, entitlement_type)`; unique non-null `grant_id`; status `PENDING/ISSUED/FAILED`; `FAILED` requires `last_error_code`; `ISSUED` requires `grant_id` and `issued_at`; `installment_no >= 1` and validated against the frozen order snapshot; mutable head |
| `entitlement_metering.usage_reservation` | `entitlement-metering` | V006 | S2 | `id`, `call_id`, `balance_id`, `subject_id`, `authorized_units`, `status`, `authorization_expires_at`, `settled_units`, `reconciliation_due_at`, `reconciliation_attempt_count`, `last_reconciled_at`, `authority_source`, `last_error_code`, `created_at`, `terminal_at`, `revision` | Unique `call_id`; FK to `call_record.call_id`; status `HELD/RECONCILING/SETTLED/RELEASED`; one terminal transition guarded by `revision`; settlement at most once; `authorized_units` are frozen recording seconds in the baseline (1 unit = 1 second), so `authorizedRecordingSeconds` is an explicit identity conversion of the committed value; `authorization_expires_at` is the last instant to start the B-leg, not the end of a running call, and is never renewed by replay; the reservation commits in the same transaction as the task claim and frozen decision, so no claimed task can commit without it; actual use cannot exceed authorized units plus approved tolerance; unknown use keeps `RECONCILING` and is never released; mutable head |
| `entitlement_metering.usage_fact` | `entitlement-metering` | V006 | S2 | `id`, `reservation_id`, `balance_id`, `call_id`, `usage_type`, `units`, `source_event_domain`, `source_event_id`, `occurred_at`, `created_at`, `snapshot`, `snapshot_sha256` | Unique `(reservation_id, usage_type)` and unique `(source_event_domain, source_event_id)`; FKs to reservation, balance and call record; append-only; positive units only; append-only |
| `activation_consent.consent_record` | `activation-consent` | V007 | S2 | `id`, `subject_id`, `purpose`, `channel`, `event_domain`, `policy_code`, `policy_version`, `prompt_version`, `action`, `source_type`, `source_ref`, `ivr_session_id`, `account_id`, `employee_id`, `snapshot`, `snapshot_sha256`, `event_id`, `request_fingerprint`, `accepted_at` | Unique `(event_domain, event_id)`; explicit accept/decline only; snapshot is redacted or encrypted and immutable; `request_fingerprint` is required for replay comparison; no inferred consent; append-only |
| `activation_consent.service_activation` | `activation-consent` | V007 | S2 | `id`, `subject_id`, `status`, `revision`, `created_at`, `updated_at` | Unique `subject_id`; status `INACTIVE/ACTIVE/SUSPENDED`; current revision must match the transition ledger; confirmation and operations suspend/resume read the status under the subject lifecycle lock and the activation row lock; a `SUSPENDED` row cannot be activated by IVR confirmation and only an `OPERATIONS` transition may append `SUSPENDED → ACTIVE`; resume never calls the first-trial service; mutable head |
| `activation_consent.service_activation_transition` | `activation-consent` | V007 | S2 | `id`, `activation_id`, `subject_id`, `from_status`, `to_status`, `from_revision`, `to_revision`, `source_channel`, `source_action_domain`, `source_action_event_id`, `status_event_domain`, `status_event_id`, `request_fingerprint`, `actor_account_id`, `actor_employee_id`, `reason_code`, `consent_record_id`, `occurred_at` | Unique `(activation_id, to_revision)`, unique `(source_action_domain, source_action_event_id)` and unique `(status_event_domain, status_event_id)`; `to_revision = from_revision + 1`; `source_action_event_id` and `request_fingerprint` are NOT NULL for every domain, including `OPERATIONS`; action domains are `ACTIVATION`, `IVR_CONFIRM` or `OPERATIONS`; `status_event_domain = ACTIVATION_STATUS`; append-only |
| `evidence.media_object` | `evidence` | V008 | S3 | `id`, `storage_provider`, `bucket`, `object_key`, `object_version_id`, `sha256`, `size_bytes`, `mime_type`, `crypto_key_ref`, `worm_status`, `created_at` | Unique `(bucket, object_key, object_version_id)`; immutable identity/version/hash; no object bytes, bucket/key public output or permanent URL; append-only |
| `evidence.recording` | `evidence` | V008 | S3 | `id`, `call_id`, `recording_id`, `media_object_id`, `declared_format`, `declared_size`, `declared_duration`, `declared_sha256`, `validated_duration`, `validated_sha256`, `status`, `upload_expires_at`, `created_at`, `validated_at`, `revision` | Unique `recording_id`; FK to `call_record.call_id` and `media_object`; one recording identity per call/recording pair; status changes use conditional revision; server-computed hash and duration win; mismatch quarantines; mutable head |
| `evidence.evidence_record` | `evidence` | V008 | S3 | `id`, `subject_id`, `source_type`, `source_ref_id`, `source_event_domain`, `source_event_id`, `call_id`, `session_id`, `reservation_id`, `identity_at_capture`, `consent_version`, `status`, `revision`, `snapshot`, `snapshot_sha256`, `created_at`, `solidified_at` | `source_event_domain` check `EVIDENCE`; unique `(source_event_domain, source_event_id)`; composite FK `(source_event_domain, source_event_id)` → `evidence_event(event_domain, event_id)` is `DEFERRABLE INITIALLY DEFERRED`; FKs to subject, call record, inbound session and reservation; exactly one `EVIDENCE_CREATED` event must match at commit through the deferred constraint trigger; captured subject/session/identity/consent are immutable; mutable head |
| `evidence.evidence_artifact` | `evidence` | V008 | S3 | `id`, `evidence_id`, `media_object_id`, `artifact_role`, `created_at` | Unique `(evidence_id, artifact_role, media_object_id)`; immutable; original and derived roles remain distinct; append-only |
| `evidence.evidence_event` | `evidence` | V008 | S3 | `id`, `evidence_id`, `event_domain`, `event_id`, `event_type`, `occurred_at`, `payload_redacted`, `payload_sha256` | `event_domain` check `EVIDENCE`; unique `(event_domain, event_id)` as the composite FK target; `evidence_id` FK to `evidence_record.id` is `DEFERRABLE INITIALLY DEFERRED` (the opposite side of the creation cycle); partial unique `(evidence_id)` where `event_type = EVIDENCE_CREATED`; exactly one creation event owns each evidence record; payload is redacted metadata, not media bytes; append-only |
| `sharing.evidence_share_grant` | `sharing` | V009 | S3 | `id`, `share_id`, `capability_token_hash`, `evidence_id`, `created_by_account_id`, `owner_organization_id`, `verification_mode`, `code_salt`, `code_hash`, `code_generation`, `valid_from`, `expires_at`, `max_admissions`, `admissions_used`, `download_allowed`, `content_snapshot`, `content_sha256`, `status`, `revocation_generation`, `revision` | Unique `share_id` and capability-token hash; `admissions_used <= max_admissions` when a limit is set; content scope immutable; access checks require current status/generation; no plaintext code or permanent URL; mutable head |
| `sharing.share_admission` | `sharing` | V009 | S3 | `id`, `share_grant_id`, `admission_key`, `admission_no`, `verification_mode`, `verification_result`, `admitted_at` | Unique `(share_grant_id, admission_key)` and `(share_grant_id, admission_no)`; successful admission increments count once under row lock; admission row is immutable; same admission key returns this row and reissues the derived ticket without incrementing; append-only |
| `sharing.share_access_event` | `sharing` | V009 | S3 | `id`, `share_grant_id`, `share_admission_id`, `event_domain`, `event_id`, `event_type`, `result`, `occurred_at`, `risk_digest`, `payload_redacted`, `payload_sha256` | `event_domain` check `SHARE_ACCESS`; unique `(event_domain, event_id)`; append-only; records verify, lock, admission, ticket issue, ticket failure, playback, extension, rotation, revoke, expiry and failure outcomes; payload is redacted; append-only |
<!-- catalog:end -->

### 8.1 Sensitive-field inventory

Every protected value has exactly one representation below; the final specification must name the column type, key reference and allowed response field. No full phone number, WeChat subject, identity number, callee number, share code, OTP, object key/version or permanent media URL is a public key or log field.

<!-- sensitive:start -->
| Column(s) | Representation | Prohibited exposure |
| --- | --- | --- |
| `customer_iam.auth_identity.provider_subject_ciphertext` / `provider_subject_hmac` | Ciphertext plus independent HMAC lookup | Never returned; HMAC is not a public identifier |
| `customer_iam.identity_profile.legal_name_ciphertext` / `id_number_ciphertext` / `id_number_hmac` / `verification_ref_ciphertext` | Ciphertext, HMAC, masked display only | No plaintext in API, logs, audit snapshots or prototype fixtures |
| `operations_iam.employee_account.login_name_hmac` / `auth_subject_hmac` | HMAC plus masked login name | Credentials and employee session material stay outside business tables |
| `subject_number.caller_number_binding.number_ciphertext` / `number_hmac` / `number_masked` | Ciphertext, HMAC, masked | HMAC used for uniqueness/lookup, never returned as the number |
| `subject_number.subject_claim.verification_ref_ciphertext` | Ciphertext | Verification evidence is not a public claim identifier |
| `ivr.outbound_call_task.caller_number_hmac` / `callee_ciphertext` / `callee_masked` | HMAC, ciphertext, masked | IVR-only callee plaintext; never in ordinary logs or client responses |
| `ivr.inbound_session.caller_ciphertext` / `caller_number_hmac` / `caller_masked` / `access_number_masked` | Ciphertext, HMAC, masked | Access number and caller are not public keys |
| `ivr.ivr_decision.response_snapshot` / `response_payload_ciphertext` | Typed redacted snapshot plus encrypted internal payload; the full task callee exists only under `task.calleeNumber` for `DIAL_TASK_CALLEE`, and the full confirmed callee only under `authorization.calleeNumber` for a successful `DIRECT_AUTHORIZATION` (`action` null) | Authenticated IVR internal response only; snapshot, logs, audit and customer responses use the masked callee; no permanent media or callee leakage; replay decrypts the same immutable payload; direct success never populates task fields |
| `ivr.call_record.caller_ciphertext` / `caller_masked` / `callee_ciphertext` / `callee_masked` / `access_number_masked` | Ciphertext and masked; caller HMAC for lookup | Call parties are never public identifiers |
| `ivr.call_event.payload_ciphertext` / `payload_redacted` | Encrypted raw payload plus redacted view and SHA-256 | Raw event payload never logged or returned |
| `catalog_order.payment_event.payload_ciphertext` | Ciphertext plus SHA-256 | Provider payload is never returned or logged in plaintext |
| `catalog_order.payment_order.merchant_order_no` / `provider_transaction_id`; `payment_event.provider_event_id` / `transaction_id` | Provider-scoped internal identifiers | Never returned to clients as public keys; logs restricted and access audited |
| `catalog_order.catalog_version.snapshot` / `snapshot_sha256` | Canonical commercial snapshot and hash | No protected value unless encrypted or redacted before hashing |
| `catalog_order.customer_order.snapshot` / `snapshot_sha256` | Canonical order snapshot and hash | Historical commercial facts only; protected values encrypted or redacted |
| `entitlement_metering.trial_policy_version.snapshot` / `snapshot_sha256` | Canonical policy snapshot and hash | Policy facts only; no protected value |
| `entitlement_metering.entitlement_grant.snapshot` / `snapshot_sha256` | Canonical grant snapshot and hash | Historical grant facts only; no protected value |
| `entitlement_metering.usage_fact.snapshot` / `snapshot_sha256` | Canonical usage snapshot and hash | Usage metadata only; no raw media or protected value |
| `activation_consent.consent_record.snapshot` / `snapshot_sha256` | Canonical consent snapshot and hash | No plaintext protected value; prompt and policy references only |
| `evidence.media_object.bucket` / `object_key` / `object_version_id` / `crypto_key_ref` | Internal storage reference | Never exposed in public URLs or client responses |
| `evidence.recording.declared_sha256` / `validated_sha256` | Hash only | Declared hash never overrides server validation |
| `evidence.evidence_record.identity_at_capture` / `consent_version` / `snapshot` | Immutable status/version reference and canonical snapshot; no raw PII | Captured identity is a reference, not a live identity or raw number |
| `evidence.evidence_event.payload_redacted` / `payload_sha256` | Redacted metadata and hash | Never contains media bytes or raw party identifiers |
| `sharing.evidence_share_grant.capability_token_hash` / `code_hash` / `code_salt` | One-way digest and salt | No plaintext capability token or share code stored |
| `sharing.evidence_share_grant.content_snapshot` / `content_sha256` | Internal content-scope snapshot and hash | Never expose object keys, internal evidence IDs or unshared evidence |
| `sharing.share_admission.admission_key` | Opaque idempotency key or digest | Never expose raw verification input or contact data |
| `sharing.share_access_event.risk_digest` / `payload_redacted` | Hash and redacted metadata | No raw IP, phone or full URL in ordinary audit output |
| `audit.audit_fact.before_snapshot` / `after_snapshot` | Redacted or encrypted snapshot | No plaintext secret or raw number |
| `platform.outbox_event.payload_redacted` / `payload_ciphertext` / `platform.idempotency_record.result_snapshot` | Redacted or encrypted domain payload | No secret or raw number; consumers receive only authorized fields |
<!-- sensitive:end -->

### 8.2 Revision, immutability and deletion classification

| Class | Tables | Revision/event rule | Deletion/retirement rule |
| --- | --- | --- | --- |
| Mutable head | `platform.idempotency_record`, `platform.outbox_event`; `customer_iam.customer_account`, `auth_identity`, `identity_profile`; `organization_access.organization`, `organization_membership`; `operations_iam.employee_account`; `subject_number.business_subject`, `caller_number_binding`, `subject_risk_state`; `catalog_order.catalog_version`, `catalog_offer`, `customer_order`, `payment_order`; `ivr.outbound_call_task`, `inbound_session`, `call_record`, `call_leg`, `call_event`; `entitlement_metering.trial_policy_version`, `entitlement_balance`, `entitlement_issuance`, `usage_reservation`; `activation_consent.service_activation`; `evidence.recording`, `evidence_record`; `sharing.evidence_share_grant` | `revision` plus conditional update; outbox dispatcher state changes are conditional and the event payload is immutable; the idempotency row transitions `NEW → COMPLETED` once and its final result is immutable | Status or terminal timestamp retirement; no ordinary hard delete |
| Append-only or frozen | `audit.audit_fact`; `subject_number.subject_claim`; `catalog_order.catalog_product`, `catalog_benefit_line`, `payment_event`; `ivr.ivr_decision`; `entitlement_metering.trial_grant`, `entitlement_grant`, `usage_fact`; `activation_consent.consent_record`, `service_activation_transition`; `evidence.media_object`, `evidence_artifact`, `evidence_event`; `sharing.share_admission`, `share_access_event` | Stable event identity and SHA-256; first decision, grant, snapshot or admission is never updated | No update/delete privilege; append-only trigger or restricted function; retention periods deferred |

Every one of the 44 tables belongs to exactly one row above: 28 mutable-head and 16 append-only or frozen. `platform.idempotency_record` is mutable-head, not append-only, because `platform.idempotency_complete` updates `status`, the result columns, `completed_at` and `revision` exactly once; the completed result is immutable thereafter. Permanent uniqueness overlays (first-trial subject, paid grant, accepted claim, task/call/event, provider payment event, evidence source event and share admission keys) are constraints within these classes, not a third lifecycle class.

`catalog_product` is immutable append-only identity. No product draft-delete exception exists. The only future deletion candidate is an unreferenced `catalog_version` that has never been published; no deletion is implemented in the baseline, and any future capability requires separate design and controller approval. Published, sold, referenced or historical rows are never hard-deleted.

No table uses `ON DELETE CASCADE`. All foreign keys are `ON UPDATE RESTRICT ON DELETE RESTRICT`; no module receives `DELETE` on another module's tables; retention periods, legal erasure and backup deletion are open inputs, not schema defaults.

### 8.3 Machine-readable baseline matrix

The matrix is the authoritative table-to-owner-to-wave-to-slice-to-lifecycle-to-sensitive map. Every table appears exactly once. `MUTABLE_HEAD` and `APPEND_ONLY` correspond to the two classes in section 8.2; `Y` means the table is covered by section 8.1.

<!-- baseline-matrix:start -->
platform.idempotency_record|platform-reliability|V001|S1|MUTABLE_HEAD|Y
platform.outbox_event|platform-reliability|V001|S1|MUTABLE_HEAD|Y
audit.audit_fact|operations-audit|V001|S1|APPEND_ONLY|Y
customer_iam.customer_account|customer-iam|V002|S1|MUTABLE_HEAD|N
customer_iam.auth_identity|customer-iam|V002|S1|MUTABLE_HEAD|Y
customer_iam.identity_profile|customer-iam|V002|S1|MUTABLE_HEAD|Y
organization_access.organization|organization-access|V002|S1|MUTABLE_HEAD|N
organization_access.organization_membership|organization-access|V002|S1|MUTABLE_HEAD|N
operations_iam.employee_account|operations-iam|V002|S1|MUTABLE_HEAD|Y
subject_number.business_subject|subject-number|V003|S1|MUTABLE_HEAD|N
subject_number.caller_number_binding|subject-number|V003|S1|MUTABLE_HEAD|Y
subject_number.subject_claim|subject-number|V003|S1|APPEND_ONLY|Y
subject_number.subject_risk_state|subject-number|V003|S1|MUTABLE_HEAD|N
catalog_order.catalog_product|catalog-order|V004|S2|APPEND_ONLY|N
catalog_order.catalog_version|catalog-order|V004|S2|MUTABLE_HEAD|Y
catalog_order.catalog_offer|catalog-order|V004|S2|MUTABLE_HEAD|N
catalog_order.catalog_benefit_line|catalog-order|V004|S2|APPEND_ONLY|N
catalog_order.customer_order|catalog-order|V004|S2|MUTABLE_HEAD|Y
catalog_order.payment_order|catalog-order|V004|S2|MUTABLE_HEAD|Y
catalog_order.payment_event|catalog-order|V004|S2|APPEND_ONLY|Y
ivr.outbound_call_task|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.inbound_session|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.call_record|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.call_leg|ivr|V005|S2|MUTABLE_HEAD|N
ivr.call_event|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.ivr_decision|ivr|V005|S2|APPEND_ONLY|Y
entitlement_metering.trial_policy_version|entitlement-metering|V006|S2|MUTABLE_HEAD|Y
entitlement_metering.entitlement_grant|entitlement-metering|V006|S2|APPEND_ONLY|Y
entitlement_metering.trial_grant|entitlement-metering|V006|S2|APPEND_ONLY|N
entitlement_metering.entitlement_balance|entitlement-metering|V006|S2|MUTABLE_HEAD|N
entitlement_metering.entitlement_issuance|entitlement-metering|V006|S2|MUTABLE_HEAD|N
entitlement_metering.usage_reservation|entitlement-metering|V006|S2|MUTABLE_HEAD|N
entitlement_metering.usage_fact|entitlement-metering|V006|S2|APPEND_ONLY|Y
activation_consent.consent_record|activation-consent|V007|S2|APPEND_ONLY|Y
activation_consent.service_activation|activation-consent|V007|S2|MUTABLE_HEAD|N
activation_consent.service_activation_transition|activation-consent|V007|S2|APPEND_ONLY|N
evidence.media_object|evidence|V008|S3|APPEND_ONLY|Y
evidence.recording|evidence|V008|S3|MUTABLE_HEAD|Y
evidence.evidence_record|evidence|V008|S3|MUTABLE_HEAD|Y
evidence.evidence_artifact|evidence|V008|S3|APPEND_ONLY|N
evidence.evidence_event|evidence|V008|S3|APPEND_ONLY|Y
sharing.evidence_share_grant|sharing|V009|S3|MUTABLE_HEAD|Y
sharing.share_admission|sharing|V009|S3|APPEND_ONLY|Y
sharing.share_access_event|sharing|V009|S3|APPEND_ONLY|Y
<!-- baseline-matrix:end -->

## 9. Invariant Catalog

Enforcement classes:

- `DB`: a PostgreSQL constraint, index, privilege, trigger or function enforces the rule.
- `TX`: one database transaction with row lock, advisory lock, conditional update or comparable serialization enforces the rule; DB constraints may also support it.
- `SERVICE`: an application service rule spans facts or external state and must have a Testcontainers/domain test; it may be combined with DB or TX controls.

The "Verified in slice" column names the earliest slice whose own or earlier tables make the invariant behaviorally testable. Cross-cutting policy invariants are verified at policy level in S1 and rechecked in the S3 closure.

| ID | Owning module | Collaborators | Invariant | Enforcement class | Enforcement location | Verified in slice | Goal alignment |
| --- | --- | --- | --- | --- | --- | --- | --- |
| INV-01 | `customer-iam` | `organization-access`, `operations-iam`, `subject-number` | Login identity, business-number binding, organization membership and employee identity never substitute for one another | DB+SERVICE | Separate tables and schemas with no cross-assignment FK; service tests reject a phone, membership, claim or employee record used as login identity | S1 | Account/login and business-subject distinction |
| INV-02 | `customer-iam` | none | Effective provider identity is unique within provider scope and maps to one account | DB | Unique `(provider, provider_scope, provider_subject_hmac)`; one-account FK | S1 | Login identity correctness |
| INV-03 | `organization-access` | `customer-iam` | At most one non-revoked membership for an organization and account; membership grants no independent login | DB | Partial unique `(organization_id, account_id)` where non-revoked; membership has no auth credential column | S1 | Organization membership |
| INV-04 | `operations-iam` | none | Employee accounts use a separate issuer and subject; the last enabled `ADMIN` cannot be disabled or demoted | DB+TX | Unique `(auth_issuer, auth_subject_hmac)`; admin-row lock and enabled-admin count check in one transaction | S1 | Employee identity |
| INV-05 | `subject-number` | `customer-iam`, `organization-access` | A subject has at most one personal-account or organization owner; an IVR-created subject may be unowned until a verified claim, and an existing different owner is a conflict | DB+TX | Check `num_nonnulls(owner_account_id, owner_organization_id) <= 1`; owner FKs; claim transaction sets owner only when null | S1 | Business subject ownership |
| INV-06 | `subject-number` | none | One canonical number has at most one active business binding | DB | Partial unique `number_hmac` where `ACTIVE`; unique `(subject_id, number_hmac)` | S1 | Number binding and IVR lookup |
| INV-07 | `subject-number` | none | Claim appends a verified relation; open conflict or recycle risk is checked before replay or same-account duplicate handling | TX+SERVICE | Canonical-number lock; risk-state read; append-only claim; rejected attempts retained; service test | S1 | Cross-channel claim rules |
| INV-08 | `activation-consent` | `ivr`, `entitlement-metering` | Activation requires an explicit, current, immutable consent snapshot and a subject that is not `SUSPENDED`; no key `1` or explicit client confirmation means no activation, trial or reservation; replay/identity handling precedes every gate. A fresh `SUSPENDED` confirmation is a category-1 pre-activation refusal: stable `REJECT_SUSPENDED`, no accepted consent/transition/trial/grant/balance/reservation/success Outbox, and a `CONFIRMED` session is never moved to `REJECTED`. An `ACTIVE` activation with missing/expired current consent returns `GUIDE_ACTIVATION` with stable `CONSENT_REQUIRED` on initial resolve so the same confirmation path appends current consent without reissuing the trial; stale/invalid confirmation content is rejected with `CONSENT_REQUIRED` before accepted side effects. First-trial grant, balance and Outbox are written by `issueFirstTrial` in the activation transaction. After valid activation/trial, a category-2 normal domain no-dial result commits activation/trial with the session `CONFIRMED` and no claim/reservation, while a category-3 technical failure rolls back all writes including the `NEW` row and activation/trial; no failure category writes the other category's facts | DB+TX+SERVICE | Consent FK and snapshot fields; transaction validates consent version, action and current activation status under the subject lifecycle lock; `Propagation.MANDATORY` call to `entitlement-metering.issueFirstTrial`; suspended accept/reject, consent-gap, replay-before-suspend, no-CONFIRMED-to-REJECTED, category-1/2/3 outcome and no-side-effect tests | S2 | Consent/activation, fail-closed suspension, consent-gap recovery, deterministic replay, atomic first trial and unambiguous transaction outcomes |
| INV-09 | `activation-consent` | `ivr` | Activation has one row per subject; each transition owns one unique action event, one unique `ACTIVATION_STATUS` fact event, a request fingerprint and increments revision by exactly one | DB+TX | Unique `subject_id`; unique `(activation_id, to_revision)`, `(source_action_domain, source_action_event_id)` and `(status_event_domain, status_event_id)`; check `to_revision = from_revision + 1`; row lock | S2 | Activation history |
| INV-10 | `activation-consent` | `ivr`, `entitlement-metering` | `SUSPENDED` cannot self-resume through IVR or ordinary client confirmation; a suspended fresh confirmation is rejected with `REJECT_SUSPENDED` and leaves the activation suspended; an existing completed confirmation/decision is replayed before the suspension check and an already `CONFIRMED` session is never changed to `REJECTED`; an operations resume appends only `SUSPENDED → ACTIVE` without reissuing a first trial | TX | Read current state under the subject lifecycle lock and conditionally update only from `INACTIVE/ACTIVE`; reject a fresh suspended path before consent/transition/trial/reservation/Outbox; check existing decision/idempotency first; operations resume path is separate, requires its own event identity and never calls the first-trial service | S2 | Cross-channel lifecycle and stable replay |
| INV-11 | `ivr` | none | One canonical caller has at most one non-terminal task and at most one active or unknown call across all accounts and organizations; both fresh IVR decision paths exclude the current `callId` from the guard, so a call never blocks itself; a competing call receives stable `REJECT_CALL_IN_PROGRESS` with no claim/reservation and a non-active `call_record` parent (terminal `REJECTED` when newly created; an existing current `UNKNOWN` row is referenced unchanged and never finalized); the unique index is a backstop, not the protocol response, and a terminal reject touches only the current call's row | DB+TX | Partial unique `caller_number_hmac` on `outbound_call_task` for `PENDING/CLAIMED` and on `call_record` for `ACTIVE/UNKNOWN`; canonical-caller advisory lock and `call_id <> current_call_id` predicate on both resolve and direct paths; deterministic reread on index conflict | S2 | IVR single-slot task and call |
| INV-12 | `ivr` | none | Only a `PENDING` task whose `expires_at` is ahead of database time after lock acquisition can be claimed; the default pending TTL is 30 seconds, is server-configurable, is frozen into `ttl_seconds`/`expires_at` at creation, and a duplicate or retry never extends or refreshes it | DB+TX | State and timestamp checks; lock then `clock_timestamp()` recheck; conditional update | S2 | Configurable default 30-second TTL without duplicate refresh |
| INV-13 | `ivr` | `entitlement-metering` | On either task-backed entry path, a successful decision claims the task, creates or binds one active call record, freezes the decision, creates one reservation and commits all-or-nothing only after the active-call guard, current caller/subject/binding/owner-workspace access (organization `ACTIVE`, creator/owner account `ACTIVE`, membership `ACTIVE` with current role and resource access for an organization task), active activation/consent, `PENDING`/revision/unexpired task and positive effective entitlement checks pass; a direct IVR path with no task applies only the binding/subject/activation/consent/entitlement checks and no customer login/membership predicate | TX | One shared IVR transaction; the conditional claim includes `expires_at > clock_timestamp()` and the expected revision; a category-3 technical failure rolls back task, call record, reservation, decision and event side effects together, while an expected domain claim/reservation denial rolls back only to the same-connection attempt savepoint and freezes one no-dial decision; concurrency and reservation-competition tests | S2 | Atomic claim/call/reservation/authorization with explicit domain-versus-technical outcomes |
| INV-14 | `ivr` | none | Both public entries share one action-selection service; same `callId` and purpose with the same canonical fingerprint returns the first decision and frozen action; a changed fingerprint or conflicting immutable caller/access/node identity is a stable conflict; the `ACTIVATION_NEXT_ACTION` decision is a distinct purpose and never overwrites the original `INBOUND_RESOLVE` `GUIDE_ACTIVATION` decision, so a repeated resolve returns that original decision; a new confirmation event with an existing call/purpose decision either returns it on a fingerprint match or conflicts with no lingering `NEW` row; each call event is unique in the IVR event domain | DB+TX | Unique `(call_id, purpose)` with purposes `INBOUND_RESOLVE`, `DIRECT_AUTHORIZATION` and `ACTIVATION_NEXT_ACTION`, and `(event_domain, event_id)` on `call_event`; partial unique `(call_id, source_event_domain, source_event_id)`; canonical fingerprint comparison under the canonical-caller/call lock before side effects | S2 | Idempotent IVR protocol, shared selection and preserved first decision |
| INV-15 | `ivr` | `evidence` | Task, session, call, legs, reservation and evidence remain explicitly linked with named FK parents; call completion does not imply evidence solidification | DB | FK catalog in section 12; unique call/session/task and recording identities; separate status columns | S3 | IVR/evidence linkage |
| INV-16 | `entitlement-metering` | `activation-consent`, `platform-reliability` | Exactly one permanent FIRST-TRIAL grant exists per subject; claims, rule changes, re-entry, a new campaign key or a cross-channel import do not reissue it; the granting action domain/event ID/fingerprint are persisted, same action plus same fingerprint replays the original ids, same action plus changed fingerprint conflicts, and another action for an already-granted subject returns `ALREADY_GRANTED` original ids without a second grant or balance | DB+TX | `trial_grant.campaign_key` check `FIRST-TRIAL`; unique `subject_id`; unique `(source_action_domain, source_action_event_id)`; subject-key lock; immutable grant | S2 | Trial uniqueness and provenance |
| INV-17 | `entitlement-metering` | none | Entitlement grants are immutable and unique by trial campaign or `(order, installment, entitlement type)` | DB | Unique constraints and source-exclusivity checks; append-only grant rows | S2 | Entitlement issuance |
| INV-18 | `entitlement-metering` | none | Reserved plus consumed units never exceed granted units; balance updates lock the balance row and use conditional revision | DB+TX | Check; balance-row lock; revision conditional update | S2 | Reservation/usage concurrency |
| INV-19 | `entitlement-metering` | `ivr` | One reservation per call, one terminal transition and one settlement fact; a task claim never commits without its reservation and a reservation never authorizes more than the committed available units; unused units release once; late or duplicate events do not recharge; a normal domain reservation denial after a tentative claim rolls back to the same-connection attempt savepoint and commits one no-dial decision, while a technical failure rolls back the whole transaction | DB+TX+SERVICE | Unique `call_id`; state and unit checks; reservation and claim in one transaction with a same-connection savepoint before the tentative claim; conditional terminal update; unique usage fact; settlement/reconciliation and forced-failure tests | S2 | IVR usage settlement with explicit domain-versus-technical denial |
| INV-20 | `catalog-order` | none | One immutable product identity; at most one active version per product; published version and benefit lines are immutable; type, billing and grant matrix is checked server-side | DB+TX+SERVICE | Unique `product_code`; partial unique active version per product; named checks; immutable published columns; service test | S2 | Catalog versioning |
| INV-21 | `catalog-order` | `activation-consent`, `customer-iam`, `subject-number`, `platform-reliability` | Orders only come from `MINIAPP/WEB`, require account/workspace/subject access, the ordinary-client real-name condition and current active service activation, carry an immutable exact catalog snapshot, purchase consent and server-derived amount, and cannot overwrite history after offsale; creation is bound to platform scope `PURCHASE` with a request fingerprint and the first immutable order result so identical retry returns the first order and changed input conflicts | DB+TX+SERVICE | Unique `(account_id, idempotency_key)`; `request_fingerprint`; platform `PURCHASE` begin/complete in the same transaction; immutable snapshot/fingerprint; service revalidates account/workspace/subject access, ordinary-client real-name, current activation, server amount and version token | S2 | Purchase, idempotent order creation and historical snapshots |
| INV-22 | `catalog-order` | none | Payment events are unique by provider and event; only verified events advance payment; `UNKNOWN` blocks duplicate checkout and is resolved by querying the original order; `PAID` and entitlement issuance remain independently observable | DB+TX | Unique `(provider, provider_event_id)`; signature-verification flag; order row lock and conditional payment update; service blocks a second checkout while `UNKNOWN` | S2 | Payment truth |
| INV-23 | `entitlement-metering` | `catalog-order` | Issuance is idempotent by order, installment and entitlement type, consumes the frozen order snapshot, not current catalog state, and writes grant, balance and Outbox in one transaction | DB+TX+SERVICE | Unique `(order_id, installment_no, entitlement_type)`; snapshot-equality test; same-transaction grant/balance/Outbox | S2 | Entitlement delivery |
| INV-24 | `evidence` | none | Media identity, object version and hash are immutable; bytes stay in object storage; client-declared hash or duration never wins over server validation | DB+SERVICE | Immutable metadata columns; object uniqueness; server rehash and duration validation test | S3 | Evidence/media metadata |
| INV-25 | `evidence` | `subject-number`, `ivr` | Exactly one `EVIDENCE_CREATED` event owns each evidence record; the event's `(event_domain, event_id)` equals the record's `(source_event_domain, source_event_id)`; evidence keeps captured subject, session, identity-at-capture and consent version through later claims or account changes; record and creation event insert atomically | DB+TX | Unique `(source_event_domain, source_event_id)`; unique `(event_domain, event_id)`; partial unique `(evidence_id)` for `EVIDENCE_CREATED`; composite FK with both cycle sides `DEFERRABLE INITIALLY DEFERRED`; deferred constraint trigger validates exactly one identity at commit; immutable capture columns | S3 | Reciprocal evidence ownership |
| INV-26 | `sharing` | none | Share content scope is frozen; successful admissions are counted once; revoke, rotate or expiry changes generation and blocks new or renewed access | DB+TX | Immutable content snapshot; unique admission key/number; grant row lock; status and generation recheck | S3 | Sharing authorization |
| INV-27 | `platform-reliability` | all domain owners | Idempotency is scoped by action domain; `begin` returns `NEW`, `REPLAY` or `CONFLICT`; only `NEW` may execute side effects; `platform.idempotency_complete` finalizes the first result exactly once in the same transaction; same key with changed input is rejected; a replay after committed completion returns the stored first result; an unfinalized committed `NEW` row is an alertable contract violation; for `IVR_CONFIRM`, call/purpose-level existing-decision handling precedes side effects and suspension, a matching semantic fingerprint with a new event ID reuses the existing decision, a changed fingerprint rolls back the `NEW` row and returns a stable conflict, and an event reused on another call conflicts because `callId` is in the fingerprint | DB+TX+SERVICE | Unique `(scope, idempotency_key)`; `status/result/completed_at/revision` checks; conditional write-once completion; canonical semantic fingerprint comparison; event/call-purpose precedence tests; begin/complete function contracts and concurrency tests | S1 | Cross-channel idempotency |
| INV-28 | `platform-reliability` | all domain owners | Outbox insertion shares the domain transaction, has a stable domain-scoped dedupe key, is appended through `platform.outbox_append` and is delivered at least once | DB+TX+SERVICE | Unique `(event_domain, dedupe_key)`; in-transaction function; dispatcher and consumer tests | S1 | Reliable asynchronous facts |
| INV-29 | `operations-audit` | none | Audit facts are append-only and cannot be updated or deleted through ordinary administration; the only write path is `audit.append_fact`, owned by `operations-audit` with `search_path = audit, pg_temp`, appending `audit.audit_fact` | DB | Revoked table privileges; append-only trigger; restricted `audit.append_fact`; privilege test | S1 | Append-only audit |
| INV-30 | `platform-reliability` | all domain owners | Business facts are retired by status or terminal time, not hard delete; FKs restrict deletion; no cross-module cascade; only unreferenced never-published catalog version drafts are a future deletion candidate | DB | `ON DELETE RESTRICT`; no delete grants; lifecycle classification in section 8.2; final closure in S3 | S1 | Deletion policy |
| INV-31 | `platform-reliability` | all domain owners | Full phone, WeChat subject, identity number, callee, share code and object URL are not public keys and are not stored or logged as plaintext secrets; the full IVR callee exists only in the encrypted internal action payload — under `task.calleeNumber` for an authenticated `DIAL_TASK_CALLEE` response or under `authorization.calleeNumber` for a successful `DIRECT_AUTHORIZATION` (`action` null) — while the typed snapshot, audit, logs and customer responses carry only the masked value; a direct success never places the callee in task fields | DB+SERVICE | Column representation contract in section 8.1; action-snapshot nullability/redaction tests; log and response checks; replay-decryption test; final closure in S3 | S1 | Sensitive-field handling and callee boundary |
| INV-32 | `ivr` | none | Task create, replace and cancel is idempotent and workspace/creator-scoped; an identical `PENDING` duplicate returns the existing task without TTL refresh; stale `PENDING` expires under the caller lock; a `CLAIMED` task cannot be replaced or cancelled | DB+TX | Unique `(created_by_account_id, idempotency_key)`; fingerprint and identical-tuple checks; canonical-caller lock; workspace/creator checks; conditional state update | S2 | IVR task lifecycle |
| INV-33 | `subject-number` | `customer-iam` | Subject real-name status is independent of login, claim, activation and trial; claim never writes it | DB+SERVICE | `business_subject.real_name_status`; claim columns cannot update it; service test | S1 | Real-name and subject distinction |
| INV-34 | `ivr` | `entitlement-metering` | Direct DTMF authorization takes the canonical-caller lock, requires the frozen `ENTER_CALLEE` no-task path, rejects a task ID and any `PENDING/CLAIMED` task after a positive no-task check, rechecks no other `ACTIVE/UNKNOWN` call excluding the current `callId`, reuses the shared identity/subject/activation/consent/entitlement predicate order without requiring a customer login, membership or real-name, creates or binds the active call guard, creates one reservation, and freezes a success with `action` null plus the committed reservation fields (never an `ENTER_CALLEE` instruction after the callee was confirmed); a direct refusal uses an existing non-dial action with no reservation; retry returns the first result and changed confirmation content conflicts | DB+TX | Unique `(call_id, purpose)` and partial unique `(call_id, source_event_domain, source_event_id)`; canonical fingerprint comparison; caller lock; active-call partial unique; no-task check; reservation uniqueness; purpose/action/nullability test; concurrency tests | S2 | Direct IVR path within the seven-action contract |
| INV-35 | `platform-reliability` | all domain owners | Event and action identity is unique within its actual domain; domain tables persist the domain and event identity; the same string may exist in different domains, but the same `IVR_CONFIRM` event ID reused for another call conflicts because the semantic fingerprint includes `callId`; changed confirmation content for the same event ID also conflicts | DB+SERVICE | Composite unique constraints on `call_event`, `ivr_decision`, `usage_fact`, `consent_record`, `evidence_event`, `share_access_event`; platform `IVR_CONFIRM` fingerprint/key semantics; event-domain registry and service tests | S2 | Idempotency and event identity |
| INV-36 | `entitlement-metering` | `catalog-order` | Every order, installment and entitlement type has one durable issuance row whose `PENDING/FAILED/ISSUED` state, attempt count, revision, error code and grant link are owned by `entitlement-metering`; partial line failure is observable; no module directly writes another module's issuance or order status | DB+TX+SERVICE | Unique issuance key; state checks; conditional revision; multi-line and partial-failure tests; privilege test | S2 | Durable entitlement issuance state |
| INV-37 | `activation-consent` | `ivr`, `platform-reliability` | Activation confirmation replay is bound to the canonical semantic fingerprint and first immutable result through `platform.idempotency_begin`/`platform.idempotency_complete`; the wrapper authenticates and validates immutable call/session/caller identity, then handles an existing `(call_id, ACTIVATION_NEXT_ACTION)` decision and the event-level `REPLAY`/`CONFLICT` before any `SUSPENDED` check or consent/trial/claim/reservation effect; a new event ID with the same fingerprint returns the existing decision and frozen action and completes that event's row with the existing decision ID, while changed input returns a stable conflict with the `NEW` row rolled back; a `CONFIRMED` session is never moved to `REJECTED`; a fresh confirmation requires the original resolve action `GUIDE_ACTIVATION`; the completed `result_id`/`result_type` is the `ACTIVATION_NEXT_ACTION` decision ID for every accepted branch, including already `ACTIVE` and call-rejected outcomes, and the rejected decision ID for a suspended fresh rejection; a category-1 rejection commits no accepted activation side effect, a category-2 domain no-dial result commits valid activation/trial with its no-dial decision, and a category-3 technical failure commits neither the decision nor any other write; every transition, including `OPERATIONS`, has a non-null action event identity | DB+TX | Platform idempotency scope `IVR_CONFIRM` with event key and canonical semantic fingerprint; write-once completion; `request_fingerprint` NOT NULL; `ivr_decision` unique `(call_id, purpose)`; `source_action_event_id` NOT NULL and unique per domain; replay/conflict/cross-call/first-result identity and category-outcome tests | S2 | Deterministic activation replay, preserved session state and a stable result per branch |
| INV-38 | `entitlement-metering` | `ivr` | A reservation whose end event is missing, late or duplicated is reconciled against authoritative IVR usage; known use is settled first, the remainder is released once; unknown use remains occupied and blocks a new reservation and is never released | DB+TX+SERVICE | `RECONCILING` state; reconciliation fields; caller lock; conditional terminal update; missing/late/duplicate tests | S2 | Safe reservation recovery |
| INV-39 | `sharing` | `platform-reliability` | The durable admission is the authorization fact; the Redis playback ticket is derived and reconstructible; same `admission_key` retry returns the admission and reissues the ticket without incrementing; Redis loss cannot change admission count | DB+TX+SERVICE | Unique admission key; derived ticket key; access events for issue/failure; Redis-failure and lost-cache tests | S3 | Count-once sharing across Redis failure |
| INV-40 | `platform-reliability` | `operations-audit`, `entitlement-metering`, all domain owners | Cross-schema idempotency begin/complete, Outbox, `audit.append_fact` and first-trial functions are `SECURITY DEFINER`, owned by the owning module, have a fixed `search_path`, named EXECUTE grants, no transaction control, exception-propagating rollback, first-result replay/finalize-once and redacted/encrypted payloads; the four platform/audit functions are created and verified in V001/S1 and `issue_first_trial` is created and verified only in V006/S2 after its tables exist; the shared IVR action-selection contract is an IVR-owned application service inside the local transaction, not an additional SQL function | DB+TX+SERVICE | Function signatures, creation waves and grants in sections 11-12; S1 privilege/atomicity/replay/redaction/`search_path` tests for the four platform/audit functions; S2 tests for `issue_first_trial` and for the shared application service | S1 for the four platform/audit functions; S2 for `issue_first_trial` and the shared service | Safe DB-enforced ownership without a new function surface |

## 10. State Machines and Transaction Controls

| Aggregate | State contract | Database control | Transaction control | Owner |
| --- | --- | --- | --- | --- |
| Subject claim | `ACCEPTED / REJECTED / CONFLICT`; `OPEN` risk blocks claim and historical-evidence access | Unique request; partial unique accepted subject/account; append-only outcome; at most one subject owner | Canonical-number lock; risk check before replay; append one claim row; set subject owner only when null, otherwise conflict if different | `subject-number` |
| Service activation | `INACTIVE → ACTIVE`; `ACTIVE → SUSPENDED`; `SUSPENDED → ACTIVE` only through operations; an IVR confirmation of a `SUSPENDED` subject is rejected with `REJECT_SUSPENDED` and changes no state; terminal history retained | Unique subject; unique transition revision and domain/event; revision check; non-null action event | Confirmation and operations suspend/resume acquire the subject lifecycle lock and the activation row lock, re-read status, validate consent or operations reason and event, append one transition and update revision in one transaction; resume never calls the first-trial service | `activation-consent` |
| Idempotency record | `NEW → COMPLETED`; `CONFLICT` is a begin outcome, not a stored state; a committed `NEW` without completion is an alertable contract violation | Unique `(scope, idempotency_key)`; status/result/completed_at checks; conditional write-once completion guarded by status and revision | `begin` inserts `NEW` or returns `REPLAY`/`CONFLICT`; only `NEW` executes side effects; `complete` fills the result once and sets `COMPLETED` in the same transaction; a technical failure rolls back the record, completion and domain facts, while a committed category-1/2 decision finalizes normally | `platform-reliability` |
| Activation confirmation | First confirmation per event ID is frozen; an existing call/purpose decision is handled by canonical fingerprint before any side effect: same fingerprint returns it and completes the new event, changed fingerprint conflicts with no lingering `NEW`; a `SUSPENDED` subject is rejected only on a fresh phase-A path and only before accepted consent/trial; a `CONFIRMED` session is never moved to `REJECTED`; a fresh confirmation requires the original `GUIDE_ACTIVATION` resolve decision; an `ACTIVE` activation with missing/expired current consent yields `GUIDE_ACTIVATION`/`CONSENT_REQUIRED` on initial resolve and rejects stale confirmation content before accepted side effects; a successful confirmation ends with exactly one `ACTIVATION_NEXT_ACTION` decision; phase-A refusal is category 1, a phase-B domain no-dial result is category 2 and commits activation/trial with the session still `CONFIRMED`, and a technical failure is category 3 | Platform idempotency `(IVR_CONFIRM, eventId)` + canonical fingerprint begin/complete; consent unique `(event_domain, event_id)`; transition fingerprint and action event NOT NULL; `ivr_decision` unique `(call_id, purpose)`; unique `usage_reservation.call_id` | IVR-owned confirmation transaction with two explicit sequential phases: authenticate the service and validate immutable call/session/caller identity before disclosure; take the canonical-caller lock and subject lifecycle lock; lock session, current call and activation; handle same-event replay and any existing decision/fingerprint before `SUSPENDED`; only `NEW` proceeds. **Phase A:** apply identity, active/unknown-call (current `callId` excluded) and fresh-confirmation guards; a fresh `SUSPENDED`/invalid-origin/content/competing-call refusal is category 1 — mark a non-confirmed session `REJECTED` only where the guard allows, write one rejected decision, complete with its ID and write no accepted side effect. **Phase B:** after `activation-consent.record_activation_confirmation` and `issueFirstTrial`, call the shared `ivr.select_call_action`, which re-applies the current access/task/entitlement predicates; a normal domain no-dial result is category 2 and commits activation/trial with the one no-dial decision, rolling any tentative claim/reservation back to the attempt savepoint; complete with the next decision ID, enqueue Outbox/audit, commit. A technical error in either phase is category 3 and rolls back every write | `activation-consent` for consent/activation facts; `ivr` owns the confirmation transaction and the shared decision service |
| Outbound task | `PENDING → CLAIMED / REPLACED / CANCELLED / EXPIRED / REJECTED`; `CLAIMED → COMPLETED / FAILED`; all terminal states immutable | Idempotency key/fingerprint; partial unique canonical caller; state checks; unique claimed call; claim-time access/expiry/revision checks | Canonical-caller advisory lock covers create, replace, cancel, claim and direct session establishment; a claim requires `PENDING`, current caller/subject/binding/workspace/creator access including organization/membership/owner/creator account lifecycle status for a task-backed call, `expires_at > clock_timestamp()` in the conditional update, matching revision and a reservation in the same transaction; a duplicate never refreshes the TTL; use a bounded reread instead of a raw constraint error | `ivr` |
| Inbound session | `RECEIVED → CONFIRMED / ABANDONED / REJECTED`; `GUIDE_ACTIVATION` may precede confirmation; a `SUSPENDED` subject cannot become `CONFIRMED`; a fresh confirmation requires the original resolve action `GUIDE_ACTIVATION`; a `CONFIRMED` session is terminal for confirmation-state purposes and is never re-rejected by replay | Unique `call_id`; confirmed-subject and consent-version checks; original `(call_id, INBOUND_RESOLVE)` action check | Confirmation verifies trusted caller, prompt version, key `1`, subject, current consent and non-suspended status under the subject lifecycle lock; existing decision/idempotency replay is handled before this gate | `ivr` |
| IVR decision | First response per `(call_id, purpose)` is frozen; a same-fingerprint retry returns it and a changed fingerprint conflicts; `INBOUND_RESOLVE`/`DIRECT_AUTHORIZATION`/`ACTIVATION_NEXT_ACTION` are distinct purposes and the post-activation decision never rewrites the original resolve decision; the response is the typed snapshot plus the encrypted internal payload; `DIRECT_AUTHORIZATION` success has `action` null plus the committed reservation fields and its confirmed callee only under `authorization.calleeNumber` in the encrypted payload, while a direct refusal carries a permitted non-dial action and no reservation | Unique call/purpose; canonical fingerprint; response hash; partial unique call/source domain/event; purpose/action/nullability CHECK; `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION` written only through the shared IVR action service | Read the existing decision under the canonical-caller/call lock before any side effect; persist one decision after the ordered predicate set; a concurrent insert loser performs a bounded reread and returns the existing decision or a stable conflict | `ivr` |
| Direct authorization | `NONE → AUTHORIZED / REJECTED`; only `ENTER_CALLEE` with no task; one decision per call/event; no task ID accepted; same fingerprint returns the first authorization and changed confirmation content conflicts; success freezes `action` null with the committed reservation/authorization fields (never `ENTER_CALLEE` after callee confirmation), refusal freezes a permitted non-dial action with no reservation | Unique call/purpose; partial unique call/source domain/event; unique reservation per call; active-call partial unique; purpose/action/nullability CHECK | Take canonical-caller lock; recheck no `PENDING/CLAIMED` task after a positive no-task check and no other `ACTIVE/UNKNOWN` call excluding the current `callId`; reuse the shared subject/activation/consent/effective-entitlement checks without a customer login/membership requirement; create or bind the current `call_record ACTIVE`; create or reuse one reservation; freeze the typed decision | `ivr` |
| Call record | Live call `ACTIVE → ENDED / FAILED / UNKNOWN / REJECTED`; a call pre-empted by another active call is created terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS`; `UNKNOWN` remains occupied until reconciliation, then converges to `ENDED` or `FAILED` and is never finalized `REJECTED` or released; `REJECTED` is terminal and never holds the active guard; a replay returns the immutable decision before any state change, and a fresh attempt with inconsistent `UNKNOWN` line state gets a stable reconciliation/integrity refusal with no destructive change or new reservation/dial | Unique call/session/task; active-caller partial unique for `ACTIVE/UNKNOWN`; immutable party ciphertext/masked fields | Create or bind the current live call as `ACTIVE`; create a rejected competing call as terminal `REJECTED` so the decision FK is valid; exclude the current `callId` from the guard; only a never-started/rejectable current `ACTIVE` row may be finalized `REJECTED`; a terminal reject touches only the current call's row and never the competing `UNKNOWN`/`ACTIVE` row; events converge by `(call_id,event_id)`; contradictory terminal facts quarantine | `ivr` |
| Usage reservation | `HELD → SETTLED / RELEASED`; `HELD → RECONCILING`; `RECONCILING → SETTLED / RELEASED`; one terminal transition; frozen `authorized_units` and `authorization_expires_at` | Unique call; state and unit checks; balance check; reconciliation fields; same-transaction claim constraint | Lock reservation and balance; commit with the task claim and frozen decision when the domain predicates succeed; an expected domain denial rolls back only to the attempt savepoint and freezes a no-dial decision, while a technical error rolls back the whole transaction; settle known use first and release the remainder once; unknown use stays `RECONCILING`; caller lock blocks a new reservation; expiry of the start cutoff never renews the authorization | `entitlement-metering` |
| Trial | `GRANTED → EXHAUSTED / EXPIRED` is derived from balance and time | Unique subject; unique source action identity; immutable granted window, source identity and fingerprint; campaign key fixed to `FIRST-TRIAL` | Lock the subject key; `GRANTED` only when no trial exists; same action plus same fingerprint replays original ids; same action plus changed fingerprint conflicts; another action returns `ALREADY_GRANTED`; never mutate or reissue the grant | `entitlement-metering` |
| Catalog | Product identity immutable; version `DRAFT → ACTIVE → OFFSALE`; published content immutable | Unique product code; partial unique active product version; state checks | New publication and old-version offsale share one transaction; product is never in a draft/deletable state | `catalog-order` |
| Order creation | `NONE → CREATED / REJECTED`; created order payment state starts `UNPAID` | Unique `order_no` and `(account_id, idempotency_key)`; `request_fingerprint`; platform `PURCHASE` begin/complete; channel, amount and consent checks | Verify account/workspace/subject access, ordinary-client real-name, current active service activation, explicit consent and ACTIVE offer/version token; compute amount; `begin('PURCHASE', account_id || ':' || idempotency_key, fingerprint)`; only `NEW` inserts the order once with its fingerprint and frozen snapshot; `complete` with the first order ID and snapshot in the same transaction; replay returns the first order, changed input conflicts; no entitlement status column | `catalog-order` |
| Order payment | `UNPAID → UNKNOWN / PAID / CLOSED`; `UNKNOWN → UNPAID / PAID / CLOSED`; `UNKNOWN` blocks duplicate checkout until the original order is queried; paid/closed terminal for ordinary flow | Payment checks; unique order/account idempotency key; payment-event uniqueness | Verified payment event and order row lock; service blocks a second checkout while `UNKNOWN`; multiple verified events may advance one order | `catalog-order` |
| Entitlement issuance | `NOT_STARTED` (no row) → `PENDING → ISSUED`; `PENDING → FAILED`; `FAILED → PENDING` retry; `ISSUED` terminal | Unique `(order_id, installment_no, entitlement_type)`; unique non-null `grant_id`; state and attempt checks | Claim or insert PENDING durably; lock the issuance row; write grant, balance and Outbox and set ISSUED in one transaction; a failed attempt is recorded and retried exactly once per key | `entitlement-metering` |
| Recording | `DECLARED → UPLOADED → VALIDATED / QUARANTINED` | Unique recording ID; server hash/duration fields | Completion validates object identity/hash and never creates a second recording for retries | `evidence` |
| Evidence | `PENDING → SOLIDIFIED / QUARANTINED / FAILED` | Unique source event; unique `(event_domain,event_id)`; partial unique creation event; composite FK and reverse FK both `DEFERRABLE INITIALLY DEFERRED`; deferred trigger; immutable capture fields | Evidence record and exactly one creation event are inserted in one transaction in either order; the deferred trigger validates exactly-one identity at commit; claim never rewrites capture facts | `evidence` |
| Share | `ACTIVE → EXPIRED / REVOKED / LOCKED`; admission + derived playback ticket | Status/generation check; unique admissions; admissions count check; unique access event | Admission locks grant, checks limit/generation, inserts admission and count once in PostgreSQL; after commit the ticket is derived from `share_grant_id + admission_id`; same admission key reissues it; a ticket failure is an access event and never increments admissions | `sharing` |

### 10.1 Shared IVR action-selection/claim contract (normative)

`ivr.select_call_action` is one small IVR-owned application service, not a SQL function and not a new module. The two public endpoints remain separate and keep their own transport and immutable decision purposes:

| Entry | Endpoint | Derived entry mode | Decision purpose | First-result store |
| --- | --- | --- | --- | --- |
| Initial resolve | `POST /api/v1/ivr/inbound/resolve` | `INITIAL_RESOLVE` | `INBOUND_RESOLVE` | Unique `(call_id, purpose)` decision with the initial-resolve fingerprint |
| Post-activation | `POST /api/v1/ivr/calls/{callId}/activation-confirmations` | `POST_ACTIVATION` | `ACTIVATION_NEXT_ACTION` | Platform scope `IVR_CONFIRM` completion plus the unique call/purpose decision |
| Direct authorization | `POST /api/v1/ivr/calls/{callId}/outbound-authorizations` | `DIRECT_AUTHORIZATION` | `DIRECT_AUTHORIZATION` | Unique call/purpose decision with the direct fingerprint; no task ID |

Entry mode and purpose are never taken from a caller-supplied request field. They are selected by the authenticated endpoint and validated against the persisted session state; an unknown or contradictory value fails closed.

Shared input identity (typed; values are server-derived except the protocol identity):

- authenticated IVR service identity, protocol version, `nodeId`, `callId`, `sessionId`;
- canonical caller HMAC, access-number identity and the current immutable caller ciphertext/masked value;
- `subjectId`, `confirmedSubjectId`, `callerBindingId` and owner account/organization when bound;
- for `POST_ACTIVATION`: confirmation `eventId` used only as the idempotency key, plus prompt version, consent policy/version, confirmation key/content and the canonical confirmation fingerprint;
- for `DIRECT_AUTHORIZATION`: confirmation event identity, prompt version, entered callee HMAC/ciphertext and the canonical direct fingerprint;
- expected row revisions are read under the locks, not accepted from the client.

Shared read/lock order (deadlock-avoiding): authenticate service and protocol; normalize the trusted caller; acquire the canonical-caller advisory lock; acquire the subject lifecycle advisory lock when a subject is resolved; lock `inbound_session` and the current `call_record`; lock `service_activation`; read/`FOR SHARE` the current caller binding and the subject owner. For a task-backed call, then read/`FOR SHARE` the applicable lifecycle rows in this fixed order: `organization` (organization task), `organization_membership`, applicable owner `customer_account` (personal task), creator `customer_account`; when two rows coincide or ordering ties occur, lock by ascending UUID. The task-backed predicates require `organization.status = 'ACTIVE'` for an organization task, `organization_membership.status = 'ACTIVE'` with the current task-access role and resource access, and `customer_account.status = 'ACTIVE'` for the creator and the applicable owner. A direct IVR call with no task applies no customer account, membership or owner predicate. Finally lock the candidate task and run the reservation service in its own documented order. A binding disable/revocation, membership revocation, organization suspend/disable, account disable, owner change and activation suspend/resume must take the corresponding row lock (organization, membership, account or binding) or the subject lifecycle lock before commit, so a revocation cannot race the claim. The claim never reuses a stale pre-lock lifecycle or authorization read.

Canonical semantic fingerprints (RFC 8785-compatible canonical JSON and SHA-256):

- Initial-resolve fingerprint fields: purpose `INBOUND_RESOLVE`, protocol version, `callId`, canonical caller HMAC, access-number identity, `nodeId`, session/immutable binding identity. It excludes `receivedAt`, transport request IDs, database timestamps, and current mutable activation/balance/task status.
- Confirmation fingerprint fields: purpose `ACTIVATION_NEXT_ACTION`, protocol version, `callId`, `sessionId`, immutable caller HMAC and access-number identity, subject/confirmed-subject/binding identity, prompt version, consent policy/version, confirmation key/content. It excludes transport `eventId`, `receivedAt`/`occurredAt`, timestamps, current mutable balance/activation status/task revision and the generated decision/action result.
- Direct-authorization fingerprint fields: purpose `DIRECT_AUTHORIZATION`, protocol version, `callId`, `sessionId`, immutable caller identity, prompt version, entered callee HMAC, confirmation key/content; it excludes transport event ID, timestamps and mutable status.
- A confirmation fingerprint is never compared with an initial-resolve fingerprint. Each stored decision is compared only with the fingerprint of its own purpose.

Ordered fresh-decision predicate set (both `INITIAL_RESOLVE` and `POST_ACTIVATION`; direct authorization reuses steps 2, 4, 6 and 7 in the no-task form, with step 5 replaced by the no-task check):

```text
1. Replay and identity (before any side effect or mutation)
   authenticate service; validate callId/session/caller/node against immutable rows
   POST_ACTIVATION: begin('IVR_CONFIRM', eventId, confirmationFingerprint); handle REPLAY/CONFLICT
   read existing (callId, purpose) decision; same fingerprint -> complete new event with
   existing decisionId + frozen result, no consent/trial/claim/reservation/outbox; different
   fingerprint -> rollback any NEW row, stable conflict; CONFIRMED without ACT decision -> fail integrity
   INITIAL_RESOLVE: read (callId, INBOUND_RESOLVE) before task-expiry housekeeping or any other
   mutation; same fingerprint -> return the frozen first result; different fingerprint or
   immutable caller/access/node conflict -> stable conflict
2. Phase A active/unknown-call guard (pre-activation)
   select other call_record for the canonical caller with status ACTIVE/UNKNOWN and
   call_id <> current callId; if found -> REJECT_CALL_IN_PROGRESS, create the current call
   record as terminal REJECTED only if absent, no claim/reservation; category 1
   if the current call_record is UNKNOWN and the fresh attempt is inconsistent with its
   unknown line state -> REJECT/CALL_STATE_UNRESOLVED with no destructive change, no new
   reservation/dial/release, no UNKNOWN-to-REJECTED change; the competing call is untouched
3. Phase A fresh confirmation gate (POST_ACTIVATION only)
   require original (callId, INBOUND_RESOLVE) action = GUIDE_ACTIVATION; otherwise stable
   conflict/reject with no consent, activation or session-state mutation; category 1
   if activation status = SUSPENDED -> REJECT_SUSPENDED, session REJECTED only if not CONFIRMED,
   no accepted consent/trial/grant/balance/reservation/outbox; category 1
4. Phase B activation/consent and current access
   if activation is not ACTIVE: INITIAL_RESOLVE -> GUIDE_ACTIVATION with no task/reservation;
   POST_ACTIVATION must be ACTIVE after its confirmation or it rejects as an integrity failure
   if activation is ACTIVE but required current consent is missing/expired:
     INITIAL_RESOLVE -> GUIDE_ACTIVATION with stable CONSENT_REQUIRED (the same confirmation
     path may append current consent; an existing trial is reused, never reissued)
     POST_ACTIVATION stale/invalid confirmation content -> the confirmation service validates the
     current prompt and consent policy-version/key/content before it appends any accepted
     consent/transition/trial and returns REJECT with CONSENT_REQUIRED; phase B then requires the
     resulting consent to be current
   for a call-capable action, required consent must be current; caller binding must be ACTIVE
   and belong to the subject; the subject lifecycle must permit the action
   task-backed only: organization task -> organization.status = ACTIVE, creator account
     ACTIVE, ACTIVE membership with current task access; personal task -> creator and the
     applicable owner account ACTIVE; those lifecycle rows are locked in the shared order;
     a revoked/unauthorized task -> stable REJECT, never fall through to direct dialing
   direct IVR (no task): no customer login, membership or real-name requirement; apply only
     binding/activation/consent/entitlement/subject-lifecycle checks
5. Task candidate (task path only; bounded, never an arbitrary second task)
   a candidate is first selected/evaluated here; taskMatch = NONE on an earlier refusal means
   "not selected/evaluated", not "no task exists"
   select the caller's PENDING task; require caller/subject/binding/owner-workspace match,
   expected revision and expires_at > clock_timestamp(); expiry/cancel/replacement may fall
   through as no task; access/binding/integrity failure rejects; multiple/inconsistent -> CONFLICT
   ENTER_CALLEE requires a positive no-PENDING/CLAIMED-task check under the caller lock;
   direct authorization repeats that no-task check and rejects a task ID
6. Effective entitlement (routing snapshot, not spend authorization)
   under the balance row locks compute positive available units from a current, nonexpired,
   effective grant; zero -> GUIDE_PURCHASE with no claim/reservation; post-activation rejected
   subject-owner membership/binding/account access is a stable REJECT, not a purchase guide
7. Select and write one action atomically
   establish a same-connection savepoint after any accepted activation/trial writes (when
   present) and before the tentative claim/reservation
   task valid + available > 0 -> conditional claim + current ACTIVE call binding + reservation +
     DIAL_TASK_CALLEE; 0-row claim -> roll back to the savepoint, one bounded reread,
     deterministic non-dial result; expected reservation denial -> roll back to the savepoint
     and freeze one no-dial decision; technical error -> propagate and roll back the whole transaction
   no valid task after a positive no-task check + available > 0 -> ENTER_CALLEE
   active/consented + available = 0 -> GUIDE_PURCHASE
   no activation / missing current consent (INITIAL_RESOLVE only) -> GUIDE_ACTIVATION
   any access/identity/integrity failure -> REJECT / REJECT_SUSPENDED / REJECT_CALL_IN_PROGRESS
   direct success -> purpose DIRECT_AUTHORIZATION with action NULL and the committed
     reservation fields; direct refusal -> an existing non-dial action with no reservation
```

The current call record is created or bound as `ACTIVE` before any non-terminal action so the live call holds the single-call guard. A call pre-empted by another active call is created terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS`, giving its decision a valid non-active parent without violating the partial unique active-caller index; an index violation is caught, the transaction is rolled back and one deterministic reread maps it to `REJECT_CALL_IN_PROGRESS` or a stable conflict, never a raw constraint error. A terminal reject updates only the current call's row; an unrelated competing call is never changed. The current `callId` is always excluded from the guard, so a call never blocks itself. A replay returns the immutable decision for a current `UNKNOWN` call first; a fresh attempt inconsistent with its unknown line state returns `REJECT`/`CALL_STATE_UNRESOLVED` without finalizing the row to `REJECTED`, releasing it or authorizing a new reservation/dial. Only a never-started or otherwise rejectable current `ACTIVE` row may be finalized `REJECTED`.

Idempotency precedence under the canonical-caller/call serialization:

1. Event-level (`IVR_CONFIRM`): same key and same fingerprint -> `REPLAY` the stored first result; same key and changed fingerprint (including a different `callId`) -> stable `CONFLICT`; only `NEW` continues.
2. Call/purpose-level: if `(callId, ACTIVATION_NEXT_ACTION)` exists, the wrapper compares its stored canonical fingerprint. Match with a new event ID -> `platform.idempotency_complete` this event's `NEW` row with the existing decision ID and frozen result, commit, and perform no consent/trial/claim/reservation/Outbox. Different fingerprint -> roll back the transaction (discarding the `NEW` row) and return a stable semantic conflict.
3. Only when no call/purpose decision exists does a fresh confirmation proceed. The unique `(call_id, purpose)` index is a concurrency backstop: a loser rolls back, rereads, and returns the existing decision on a fingerprint match or a stable conflict otherwise.
4. Existing-result handling precedes the `SUSPENDED` check and every confirmation side effect; a completed decision is never overwritten and a `CONFIRMED` session never becomes `REJECTED`.

Stable action/reason/`taskMatch` matrix (reason and `taskMatch` values are part of the frozen snapshot):

| Condition | `action` | `taskMatch` | Stable `reasonCode` | Claim | Reservation |
| --- | --- | --- | --- | --- | --- |
| Caller absent, untrusted or malformed before session identity | `REJECT` | `NONE` | `CALLER_UNTRUSTED` | no | no |
| Immutable call/session/caller/node mismatch | `REJECT` | `NONE` | `SESSION_IDENTITY_MISMATCH` | no | no |
| Another `ACTIVE/UNKNOWN` call, excluding current | `REJECT_CALL_IN_PROGRESS` | `NONE` | `CALL_IN_PROGRESS` | no | no |
| Fresh confirmation not from `GUIDE_ACTIVATION` | `REJECT` | `NONE` | `CONFIRMATION_NOT_ALLOWED` | no | no |
| Fresh activation `SUSPENDED` | `REJECT_SUSPENDED` | `NONE` | `ACTIVATION_SUSPENDED` | no | no |
| No activation, `INITIAL_RESOLVE` | `GUIDE_ACTIVATION` | `NONE` | `ACTIVATION_REQUIRED` | no | no |
| Activation `ACTIVE`, required current consent missing/expired, `INITIAL_RESOLVE` | `GUIDE_ACTIVATION` | `NONE` | `CONSENT_REQUIRED` | no | no |
| `POST_ACTIVATION` confirmation content stale/invalid | `REJECT` | `NONE` | `CONSENT_REQUIRED` | no | no |
| Binding/workspace/creator/task access revoked | `REJECT` | `REJECTED` when a candidate exists, else `NONE` | `CALLER_BINDING_REVOKED`, `WORKSPACE_ACCESS_REVOKED` or `TASK_ACCESS_REVOKED` | no | no |
| Organization suspended/disabled, membership not `ACTIVE`, or owner/creator account not `ACTIVE` (task-backed) | `REJECT` | `REJECTED` when a candidate exists, else `NONE` | `ORGANIZATION_INACTIVE`, `WORKSPACE_ACCESS_REVOKED` or `ACCOUNT_DISABLED` | no | no |
| Fresh attempt on a current `UNKNOWN` call with inconsistent line state | `REJECT` | `NONE` | `CALL_STATE_UNRESOLVED` | no | no |
| Multiple/inconsistent candidate tasks | `REJECT` | `CONFLICT` | `TASK_CONFLICT` | no | no |
| Valid task, active activation/consent, available > 0 | `DIAL_TASK_CALLEE` | `CLAIMED` | `TASK_CLAIMED` | conditional | one `HELD` |
| Pending candidate, available = 0 | `GUIDE_PURCHASE` | `REJECTED` | `NO_AVAILABLE_ENTITLEMENT` | no | no |
| No valid task after a positive no-`PENDING`/`CLAIMED` check, available > 0 | `ENTER_CALLEE` | `NONE` | `NO_VALID_TASK` | no | no |
| Candidate expired at selection/update | `ENTER_CALLEE` or `GUIDE_PURCHASE` by balance | `EXPIRED` | `TASK_EXPIRED` or `NO_AVAILABLE_ENTITLEMENT` | no | no |
| Candidate cancelled/replaced | `ENTER_CALLEE` or `GUIDE_PURCHASE` by balance | `NONE` | `NO_VALID_TASK` or `NO_AVAILABLE_ENTITLEMENT` | no | no |
| Conditional claim lost; reread still shows `PENDING` (revision/race) | `REJECT`; never a new task | `REJECTED` | `TASK_UNAVAILABLE` | no | no |
| Conditional claim lost; reread shows the task claimed by another active call | `REJECT_CALL_IN_PROGRESS`; never a new task | `NONE` | `CALL_IN_PROGRESS` | no | no |
| Reservation competition exhausted balance | `GUIDE_PURCHASE` after rollback to the attempt savepoint | `REJECTED` | `NO_AVAILABLE_ENTITLEMENT` | rolled back to savepoint | rolled back to savepoint; valid activation/trial remain committed (category 2) |
| Ambiguous/inconsistent persisted state | `REJECT` | `CONFLICT` | `DATA_INTEGRITY` | no | no |

`taskMatch` semantics used by the matrix: `CLAIMED` means this decision claimed and bound the task; `NONE` means no candidate task was selected/evaluated by this decision — on an early refusal (identity, active-call, suspended, consent-gap) it is not proof that no task exists; `EXPIRED` means a candidate was already expired; `REJECTED` means a candidate existed but was not claimed for a claim-time condition (zero entitlement, access/binding loss or a lost claim race); `CONFLICT` means multiple/inconsistent candidates or an integrity refusal. `ENTER_CALLEE` and direct authorization require a positive no-valid-task/no-`PENDING`/`CLAIMED` check under the caller lock; they never infer "no task" from `NONE`. Task fields are present only when a candidate was evaluated or claimed; `NONE` with null task fields is valid for early refusals. These values are part of the frozen typed snapshot.

`DIRECT_AUTHORIZATION` is a purpose, not a routing action. A successful direct authorization freezes `action = NULL`, stable `reasonCode = DIRECT_AUTHORIZED`, `taskMatch = NONE`, null task fields, the committed `reservationId`/`authorizedRecordingSeconds`/`authorizationExpiresAt` and the confirmed callee only under `authorization.calleeNumber` in the encrypted internal payload. A direct refusal freezes one of the permitted existing non-dial actions (`GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS`) with its existing stable reason, no reservation and no authorization object. A direct success never returns `ENTER_CALLEE` after the caller entered and confirmed a callee, never accepts a task ID, and never introduces an eighth action, a new table or a new function.

Write set per decision (all writes are in the one IVR-owned local transaction; the return type is the typed frozen snapshot in section 11):

| Action | Current `call_record` | Task | Reservation | Decision / idempotency / Outbox |
| --- | --- | --- | --- | --- |
| `GUIDE_ACTIVATION` | create or bind current call `ACTIVE` | none | none | one decision; no success Outbox |
| `GUIDE_PURCHASE` | create or bind current call `ACTIVE` | none | none | one decision; no success Outbox |
| `ENTER_CALLEE` | create or bind current call `ACTIVE` | none | none | one decision; no success Outbox |
| `DIAL_TASK_CALLEE` | create or bind current call `ACTIVE` | conditional `PENDING → CLAIMED` with `claimed_call_id`/`claimed_at` | one `HELD` | one decision; Outbox/audit; `IVR_CONFIRM` completion on the post-activation entry |
| `REJECT` / `REJECT_SUSPENDED` | create terminal `REJECTED` or finalize only a never-started/rejectable current `ACTIVE` row; never finalize a current `UNKNOWN` row or a competing call | none | none | one decision; on a fresh `SUSPENDED` confirmation the session becomes `REJECTED`; a post-activation call-only reject keeps the session `CONFIRMED`; `IVR_CONFIRM` completion on that entry |
| `REJECT_CALL_IN_PROGRESS` | create or finalize only the current call as terminal `REJECTED` when it is absent/never-started `ACTIVE`; never touch the competing call or a current `UNKNOWN` row | none | none | one decision; a not-yet-confirmed session becomes `REJECTED`, while a post-activation call-only rejection keeps the session `CONFIRMED`; completion on the post-activation entry; no success Outbox |
| Replay (event/call-purpose match) | none | none | none | at most the new event's `idempotency_complete` with the existing decision ID; no consent/trial/claim/reservation/Outbox repeat |
| Direct success (`purpose = DIRECT_AUTHORIZATION`, `action` null, `reasonCode = DIRECT_AUTHORIZED`) | current call `ACTIVE` | no task ID | one reservation | one decision with the committed authorization fields and encrypted `authorization.calleeNumber`; Outbox/audit |
| Direct refusal (`purpose = DIRECT_AUTHORIZATION`, permitted non-dial action) | create or finalize only the current call per the guard rules | none | none | one decision; no authorization object; no success Outbox |

When the current call row already exists as `UNKNOWN`, a refusal decision may reference that row as its parent but must not update, finalize, release or otherwise mutate it; an absent current call is created terminal `REJECTED` only when the guard permits.

Transaction outcome contract (normative; one classification, no blanket rollback rule):

1. **Category 1 — refusal before accepted activation/trial.** A fresh `SUSPENDED` subject, an invalid/stale confirmation origin or content, an identity/session mismatch, or the phase-A active/`UNKNOWN`-call guard rejects before any accepted consent, activation transition, trial/grant/balance or reservation. An authenticated fresh normal rejection persists only its stable rejected decision (with its platform idempotency completion where the entry uses `IVR_CONFIRM`), the required `call_record` parent — creating a terminal `REJECTED` current call when it is absent or finalizing its own never-started/rejectable `ACTIVE` row — and, only where the existing rejection branch already provides for it (the write-set rows for a fresh `SUSPENDED` or `REJECT_CALL_IN_PROGRESS` rejection), marks a not-yet-confirmed session `REJECTED`; it writes no accepted side effect. A current `UNKNOWN` call, an already-`CONFIRMED` session and any unrelated competing call are never destructively changed. An identity/authentication failure or an invalid-origin or semantic/fingerprint conflict produces no accepted effect and no unauthorized persistent write, and discards any `NEW` idempotency row (and uncommitted parent) created for the attempt; the invalid-origin branch keeps its no-session-mutation behavior. Identity, replay and idempotency behavior is exactly the precedence in this section.
2. **Category 2 — normal domain no-dial result after valid activation/trial.** Once accepted consent, activation and any first-trial outcome are validly established, a later shared-selection domain result — revoked task/workspace/binding/owner/creator access, zero available entitlement, task expiry/cancellation/replacement, a competing active/`UNKNOWN` call or an expected reservation denial — commits the accepted activation/trial facts, keeps the session `CONFIRMED`, commits exactly one frozen no-dial decision (with its platform idempotency completion where the entry uses `IVR_CONFIRM`; otherwise the unique decision is the frozen first result), and commits no claim or reservation. A tentative claim/reservation made by this attempt is undone by rolling back to a same-connection savepoint established after the activation/trial writes and before the tentative claim; the no-dial decision is then frozen and committed. There is no autonomous transaction, independent service or independently committed subtransaction. For an initial-resolve or direct-authorization attempt there are no activation/trial facts to preserve, but the same savepoint/no-dial freeze rule applies to an expected claim/reservation denial.
3. **Category 3 — technical transaction failure.** A SQL error, lock/advisory-lock timeout, storage I/O error, decision/completion/Outbox write failure or any other unexpected runtime error rolls back every write in the local transaction, including the `NEW` idempotency row, consent/activation/trial/grant/balance, any tentative claim/reservation and the decision/Outbox. No decision commits, no completion commits, and the caller receives a bounded retryable error. A database exception is never swallowed and a rollback-only transaction is never used to commit.

A non-technical domain refusal never rolls back valid activation/trial, and a technical failure never commits them. Pre-confirmation guards (phase A) run before consent/activation/trial; the shared fresh selection (phase B) runs after successful activation/trial in the same locked transaction. After phase A and before any accepted consent/activation/trial append, `activation-consent.record_activation_confirmation` (the single implementation of the check) validates the current prompt and consent policy-version/key/content; phase B then checks that the resulting consent is current. They are explicit sequential phases of one transaction, not two contradictory orders: category 1 applies to phase A, category 2 applies to phase B. The canonical-caller, subject-lifecycle, access and task locks are held across both phases, so an access revocation either commits before the confirmation acquires those locks (a phase-A/B rejection) or after the confirmation commits (the claim/activation already won and the revocation governs later calls); tests assert these serial orders, never an interleaving while the same locks are already held. The only committed outcomes are: a category-1 rejected decision (plus a permitted terminal current call), a category-2 frozen no-dial decision with valid activation/trial where applicable, or a successful frozen decision with its optional claim/reservation and exact completion; a replay returns an already committed decision, and category 3 commits nothing.

`OPEN` subject risk is not in this matrix: per the approved policy it blocks number claims and historical-evidence access, not a new outbound call by itself. A future approved call-blocking risk rule is a named deferred input with its own owner and destination; until then, only explicit binding/workspace/organization/membership/account/subject/activation lifecycle denials reject on access grounds, and those rejections are stable. Organization, membership and account status are explicit access predicates for a task-backed call, not a risk-derived blanket dialing ban, and they do not apply to a direct IVR caller with no task.

Illustrative scenarios (all implementation assertions are listed in section 11):

- Same current call is `ACTIVE`, another call is absent: the guard excludes the current `callId` and selection continues; the call is never blocked by itself.
- A second call arrives while the first is `ACTIVE` or `UNKNOWN`: the second call's decision is `REJECT_CALL_IN_PROGRESS` and its own terminal `REJECTED` call record; the first call's guard and decision are untouched.
- An active subject has a `PENDING` task but zero available seconds: `GUIDE_PURCHASE`, `taskMatch = REJECTED`, no claim and no reservation; the task remains `PENDING` until expiry/scheduler or an authorized terminal action.
- A membership, organization, owner/creator account or creator authorization is revoked while the activation prompt is open: because the revocation writer and the confirmation serialize on the same lifecycle/access rows, either (a) the revocation commits before the confirmation acquires the locks, so phase B observes the revoked access and freezes a category-2 no-dial `REJECT` with `TASK_ACCESS_REVOKED`/`ORGANIZATION_INACTIVE`/`ACCOUNT_DISABLED`, while the valid activation/trial and `CONFIRMED` session commit with no claim or reservation; or (b) the confirmation acquires the locks first and, while every task/entitlement predicate passes, commits a frozen `DIAL_TASK_CALLEE` with its claim and reservation, after which the revocation governs later calls only and cannot retroactively reject or reverse that committed decision. No post-commit authorization re-read, decision reversal or invented access rejection is permitted, and the task never falls through to direct entry.
- The task expires between the pre-check and the conditional claim: the `UPDATE` affects zero rows, exactly one bounded reread yields `TASK_EXPIRED`, and the result is `ENTER_CALLEE` with remaining balance or `GUIDE_PURCHASE`; no stale claim.
- The reservation service loses a balance race after the claim: the claim and reservation are rolled back to the attempt savepoint; the result is `GUIDE_PURCHASE` when the balance is exhausted, never a committed `CLAIMED` task without a reservation and never a second reservation. Valid activation/trial from the same transaction remain committed (category 2).
- An active activation with missing or expired current consent on initial resolve: the next action is `GUIDE_ACTIVATION` with `CONSENT_REQUIRED`; the confirmation path appends the current consent and returns `ALREADY_GRANTED`/the existing trial rather than issuing a second trial. Stale or invalid confirmation content is rejected before accepted side effects.
- A current call is `UNKNOWN`: a replay returns its immutable decision first; a fresh attempt whose request conflicts with the unknown line state returns `REJECT`/`CALL_STATE_UNRESOLVED` with no new reservation, dial, release or `UNKNOWN`-to-`REJECTED` change, and the competing call is never touched.
- A direct DTMF authorization succeeds: the decision purpose is `DIRECT_AUTHORIZATION`, `action` is null, the reservation/authorized-seconds/cutoff fields are non-null and the confirmed callee is only in the encrypted `authorization.calleeNumber`; no task fields are populated and no `ENTER_CALLEE` action is returned. A direct refusal is a permitted non-dial action with no reservation.
- A task duplicate arrives inside the 30-second window: it returns the existing task and never extends `expires_at`; the production configuration key remains deferred.
- A regenerated IVR event ID repeats the same semantic confirmation: the existing decision is returned and the new event's idempotency row completes with that decision ID; no consent, trial, claim, reservation or success Outbox repeats.
- The same event ID arrives with changed consent/prompt content or for another call: stable conflict, and any `NEW` row inserted for this attempt is rolled back.
- Operations suspends the subject after a confirmation committed: a repeat returns the original decision and the session stays `CONFIRMED`; a fresh confirmation on a not-yet-confirmed session rejects with `REJECT_SUSPENDED`.
- A task-path request never prompts the caller to confirm the callee; the IVR persists one unique `callId` B-leg operation before dialing and never redials on a lost response or unknown line outcome.

Critical transaction sequences:

1. **Activation confirmation with first trial and next action.** The IVR-owned confirmation wrapper runs one transaction and delegates fresh selection to `ivr.select_call_action`. Authenticate the IVR service and validate protocol version, `callId`, `sessionId`, node and immutable caller/subject/access-number identity before reading or disclosing any stored result. Acquire the canonical-caller lock and the subject lifecycle advisory lock on `subject_id` (the same lock taken by every activation lifecycle writer), then lock the `inbound_session` row, the current `call_record`, and, when it exists, the `service_activation` row `FOR UPDATE`; re-read them after the locks and hold the access rows described in section 10.1, including the organization, membership and owner/creator account lifecycle rows for a task-backed confirmation. Call `platform.idempotency_begin('IVR_CONFIRM', eventId, canonicalConfirmationFingerprint)`: `REPLAY` validates the stored caller/call identity and returns the stored first result with no side effect; `CONFLICT` rejects changed input atomically; only `NEW` continues. Before the `SUSPENDED` check and before any consent/trial/claim/reservation effect, read the existing `(callId, ACTIVATION_NEXT_ACTION)` decision. If it exists and its stored canonical fingerprint matches, complete this event's `NEW` row with the existing decision ID and frozen result, enqueue no new side effect and commit; if the fingerprint differs, roll back the transaction (discarding the `NEW` row) and return a stable semantic conflict. If the session is already `CONFIRMED` without such a decision, fail closed as `DATA_INTEGRITY` and change no state. A fresh confirmation requires the original `(callId, INBOUND_RESOLVE)` decision to be `GUIDE_ACTIVATION`; any other initial action (including an already-authorized task dial) returns `REJECT`/`CONFIRMATION_NOT_ALLOWED` with no consent or activation effect. On a fresh path, if the activation status is `SUSPENDED`, fail closed: write no accepted consent, no activation transition, no trial/grant/balance, no usage reservation and no success Outbox; persist exactly one rejected decision (purpose `ACTIVATION_NEXT_ACTION`, action `REJECT_SUSPENDED`, source domain `IVR_CONFIRM`) through the IVR entry wrapper, optionally append an audit fact, call `platform.idempotency_complete(recordId, expectedRevision, 'ACTIVATION_CONFIRMATION', rejectedDecisionId, redactedResultSnapshot, resultSha256)`, and commit; the activation stays `SUSPENDED`, a not-yet-confirmed session becomes `REJECTED`, and a `CONFIRMED` session is never changed to `REJECTED`. Otherwise call the `activation-consent.record_activation_confirmation` published service with `Propagation.MANDATORY`; that service validates the current prompt and consent policy-version/key/content before appending anything and returns the stable `REJECT`/`CONSENT_REQUIRED` rejection with no accepted effect when the content is invalid or stale. The wrapper invokes it only after replay and the phase-A guards and does not reimplement the predicate. On the accepted branch it appends the consent record and either creates the activation `ACTIVE` with its initial transition, appends one `INACTIVE → ACTIVE` transition with `toRevision = fromRevision + 1`, or keeps an existing `ACTIVE` row and appends no duplicate transition; its confirmation anchor is the new transition ID, or the current latest transition ID when the row was already `ACTIVE`. Call `entitlement-metering.issueFirstTrial` with `Propagation.MANDATORY`, which alone writes `trial_grant`, `entitlement_grant`, `entitlement_balance` and Outbox and returns `GRANTED`, `REPLAY` or `ALREADY_GRANTED`. Then call the shared `ivr.select_call_action` with entry mode `POST_ACTIVATION`: it re-applies the ordered active/unknown-call guard (excluding the current `callId`), current activation/consent and caller/subject/binding/workspace/creator access including organization, membership and owner/creator account lifecycle status, the bounded `PENDING` task/revision/`clock_timestamp()` predicate, and positive effective entitlement. It persists exactly one `ACTIVATION_NEXT_ACTION` decision: `DIAL_TASK_CALLEE` only when every task and entitlement predicate passes (conditional claim, current `ACTIVE` call binding, one reservation, frozen typed response), `GUIDE_PURCHASE` when no positive available entitlement (no claim or reservation), `ENTER_CALLEE` only after a positive no-`PENDING`/`CLAIMED`-task check and positive balance, `REJECT` for a revoked access/binding/integrity condition, `REJECT_CALL_IN_PROGRESS` for a competing active call, or `REJECT_SUSPENDED` only if the phase-A gate applies. The transaction has two explicit sequential phases. Phase A applies the identity, active/`UNKNOWN`-call and fresh-confirmation guards before consent/activation/trial; any refusal there is category 1 and writes no accepted side effect (with the `UNKNOWN` preservation rule). Phase B is the shared fresh selection after valid activation/trial. If a phase-B task-access, organization/membership/account, binding, entitlement or active-call condition blocks dialing, the result is category 2: the activation/trial commit, the session remains `CONFIRMED`, the one no-dial decision and its completion commit, and a tentative claim/reservation is rolled back to the attempt savepoint; the transaction never corrupts or re-rejects a confirmed session. The `ACTIVATION_NEXT_ACTION` purpose is distinct from `INBOUND_RESOLVE`, so the frozen `GUIDE_ACTIVATION` decision is never overwritten and a repeated resolve returns it. Call `platform.idempotency_complete(recordId, expectedRevision, 'ACTIVATION_CONFIRMATION', nextActionDecisionId, redactedResultSnapshot, resultSha256)` exactly once using the `record_id` and `revision` returned by `begin`; the decision ID is the stable non-null first result for every branch, including already `ACTIVE` and call-rejected outcomes. Enqueue Outbox/audit for the activation facts and, when claimed, the task/call/reservation facts, then commit. A claim cannot commit without its reservation. A category-3 technical failure (SQL error, lock timeout, storage error, decision/completion/Outbox failure) in either phase rolls back every write in the transaction — the `NEW` row and completion, consent, activation, transition, entitlement, tentative claim/reservation and decision/Outbox — commits no decision and returns a bounded retryable error; a database exception is never swallowed. A duplicate with the same event ID or a new event ID with the same semantic fingerprint returns the stored decision and action without reissuing a trial or creating a second claim or reservation. A concurrent operations suspension is serialized by the subject lifecycle lock: if it commits first, a fresh confirmation rejects with `REJECT_SUSPENDED`; if the confirmation commits first, the later suspension does not undo the granted trial and an operations resume never reissues it; a replay after suspension still returns the original decision and never changes `CONFIRMED` to `REJECTED`.
2. **Task create, replace or cancel.** Resolve the account/workspace and idempotency key; acquire the canonical-caller lock; expire stale `PENDING` tasks using `clock_timestamp()`; verify workspace and creator ownership and that no `ACTIVE/UNKNOWN` call exists; if an identical caller/callee/business/workspace `PENDING` task already exists, return it without refreshing TTL; otherwise insert the new task, or mark the owned `PENDING` task `REPLACED` and insert its replacement in one transaction, and enqueue Outbox; commit. Replacement is allowed only for the same workspace's and current account's `PENDING` task; `CLAIMED` tasks cannot be replaced or cancelled.
3. **IVR initial resolve.** Authenticate the IVR service and protocol, validate the immutable `callId`/node/caller identity, normalize the trusted caller and acquire the canonical-caller lock. Read any existing `(callId, INBOUND_RESOLVE)` decision **before task-expiry housekeeping or any other mutation**: the same initial-resolve fingerprint returns the frozen decision and action unchanged; a changed caller/access/node identity or fingerprint is a stable conflict. Only when no decision exists, create or reuse the inbound session and expire stale `PENDING` tasks using `clock_timestamp()`. Then apply the shared ordered predicate set from section 10.1: the other `ACTIVE/UNKNOWN` call guard excludes the current `callId`; a competing call yields `REJECT_CALL_IN_PROGRESS` with a terminal `REJECTED` current call record (only when absent/never-started `ACTIVE`) and no claim/reservation; a fresh `SUSPENDED` subject yields `REJECT_SUSPENDED`; no activation or missing current consent yields `GUIDE_ACTIVATION` (`ACTIVATION_REQUIRED`/`CONSENT_REQUIRED`); current caller/subject/binding/owner-workspace access including organization/membership/owner/creator lifecycle, activation/consent, the bounded unique task and positive effective entitlement are checked in order. Create or bind the current call record `ACTIVE` for any non-terminal action; for `DIAL_TASK_CALLEE` require the task `PENDING`, bound to the current caller/subject/binding/owner workspace, with current creator/workspace access and expected revision, conditionally update it with `expires_at > clock_timestamp()`, set `claimed_call_id`, call the reservation service in the same transaction and freeze the typed decision; for `ENTER_CALLEE` (only after a positive no-`PENDING`/`CLAIMED`-task check)/`GUIDE_PURCHASE`/`GUIDE_ACTIVATION` write no claim or reservation. Enqueue Outbox/audit and commit before returning an authorization. A 0-row claim or expected reservation denial is handled by rollback to the attempt savepoint, one bounded reread and a stable non-dial decision, never a raw constraint error or an arbitrary second task; a technical failure is category 3 and rolls back task, call record, reservation, decision and event side effects; a current `UNKNOWN` call is never destructively finalized.
4. **Direct DTMF authorization.** Authenticate the IVR service and immutable call/session/caller identity; acquire the canonical-caller lock; read the existing `(callId, DIRECT_AUTHORIZATION)` decision first and return the frozen authorization on the same canonical fingerprint or a stable conflict on changed content. Require the session's frozen `ENTER_CALLEE` decision with no task match; reject any non-empty task ID. Recheck no `PENDING/CLAIMED` task after a positive no-task check and no other `ACTIVE/UNKNOWN` call excluding the current `callId`; a competing call yields `REJECT_CALL_IN_PROGRESS` with no reservation and finalizes only the current call's record when it is absent/never-started `ACTIVE`. Reuse the shared trusted-caller, subject, activation, current consent and positive effective-entitlement checks without a customer login, membership or real-name requirement; create or bind the current `call_record ACTIVE` if it is not already; create or reuse exactly one reservation for the `callId`. Freeze the typed decision: success has purpose `DIRECT_AUTHORIZATION`, `action` null, no task fields, the committed `reservationId`/`authorizedRecordingSeconds`/`authorizationExpiresAt` and the confirmed callee only under `authorization.calleeNumber` in the encrypted payload; it never returns `ENTER_CALLEE` after the callee was confirmed. A direct domain refusal freezes a permitted non-dial action with no reservation. Enqueue Outbox/audit; commit. An expected reservation denial rolls back to the attempt savepoint and freezes the refusal; a technical failure is category 3 and rolls back everything. Retry returns the first decision and never authors a second reservation; a task-backed call that sends an empty task ID is rejected and never falls back to the task path; `authorizationExpiresAt` is copied from the reservation and is the last instant to start the B-leg.
5. **Usage settlement.** Accept the end event by `(call_id, event_domain, event_id)`; lock the reservation and balance; validate authoritative duration, end reason and reservation identity; settle actual units once, release the remainder once and append one usage fact; enqueue Outbox/audit; commit. Late or duplicate events return the first settlement and never recharge.
6. **Reservation reconciliation.** A scheduler scans `HELD` reservations whose `authorization_expires_at` has passed and no terminal call fact exists; acquire the reservation row lock and the canonical-caller lock; move `HELD → RECONCILING` with `reconciliation_due_at`, increment `reconciliation_attempt_count` and enqueue an alert if authority is unavailable. Query the authoritative IVR record by `callId`. If terminal and usage is known, settle used units, release the remainder, append one usage fact, set `SETTLED` and enqueue Outbox. If the call never started and no usage exists, set `RELEASED`. If authority is unknown, keep `RECONCILING`, keep the active-call guard occupied, block a new reservation for that caller and alert. A late authoritative event may converge `RECONCILING` once; duplicate events return the first result.
7. **Order creation.** Authenticate the account and workspace; verify subject access, ordinary-client real-name, current active service activation, explicit purchase consent and the current ACTIVE offer/version token; reject a new checkout while an existing order for the same subject/offer is `UNKNOWN` and require a query of that original order; compute the amount on the server; call `platform.idempotency_begin('PURCHASE', account_id || ':' || idempotency_key, request_fingerprint)`. A `REPLAY` returns the first completed order result; a `CONFLICT` rejects the changed input atomically; only `NEW` inserts the order once with `request_fingerprint`, the exact catalog snapshot and `UNPAID` payment state. Call `platform.idempotency_complete(recordId, expectedRevision, 'ORDER', orderId, redactedOrderSnapshot, snapshotSha256)` in the same transaction, using the returned `record_id` and `revision`, and commit. A forced failure rolls back the order and the idempotency record together; an enterprise quote cannot enter personal self-service payment; no entitlement status is written here.
8. **Payment to entitlement issuance.** Verify the provider event and lock the payment order; update payment status only from verified evidence; enqueue the Outbox event in the same transaction. For each benefit line of the due installment, the consumer inserts or claims one `entitlement_issuance` row by `(order, installment, type)`, locks it, reads the frozen order snapshot, writes the grant, balance and Outbox in one transaction, sets `grant_id` and `ISSUED`, and increments `attempt_count` and `revision` conditionally. A failure records `FAILED` with `last_error_code` and retries from `FAILED → PENDING` without duplicating the grant. Payment status stays independently observable in `catalog-order`; an order-level entitlement view is a computed projection.
9. **Evidence solidification.** Lock the recording and validate the object identity, server-computed hash and duration; insert the immutable evidence record (`source_event_domain = 'EVIDENCE'`, `source_event_id = eventId`) and exactly one `EVIDENCE_CREATED` event with the same `(event_domain, event_id)` in one transaction in either insertion order; the two composite/reverse FKs are `DEFERRABLE INITIALLY DEFERRED`, and the deferred constraint trigger validates exactly-one identity at commit; insert artifacts and enqueue Outbox; commit. Claim, account switch or later catalog change never rewrites captured subject, identity-at-capture or consent version.
10. **Share admission and ticket reconstruction.** Lock the share grant; check status, revocation generation, expiry and admission count; insert one admission once, increment the count and append the access event; commit PostgreSQL. After commit, derive the Redis ticket key from `share_grant_id + admission_id`, issue the short-lived ticket and append a `TICKET_ISSUED` access event. If Redis is unavailable or the ticket is lost, the admission remains durable, a `TICKET_FAILED` event is appended, and the same `admission_key` retry returns the existing admission and reissues the ticket without incrementing the count. A new `admission_key` is a new admission and is checked against the limit and generation; Redis is never the source of the admission fact.

## 11. Contract, Schema, State-Machine and Public-Interface Impact

### Schema contract

- Internal primary keys are UUIDs generated by the application; protocol IDs such as `task_id`, `call_id`, `recording_id`, `order_no` and `share_id` are separate opaque unique values.
- No phone number, WeChat subject, identity number, organization name or employee login name is a primary key.
- Mutable current rows use `revision` for optimistic conditional updates. Revisions are monotonic integers, not timestamp comparisons.
- Immutable snapshots use canonical JSON (RFC 8785-compatible ordering/number rules) and SHA-256. The final specification defines the exact canonicalization procedure.
- Action-domain idempotency uses `(scope, idempotency_key)` in `platform.idempotency_record` with a two-step begin/complete contract. Task creation uses `(created_by_account_id, idempotency_key)` plus a request fingerprint. Activation confirmation uses scope `IVR_CONFIRM` with the event ID as key and the request fingerprint. Order creation uses scope `PURCHASE` with key `account_id || ':' || idempotency_key` and persists the same fingerprint on `customer_order`; its first immutable result is the order ID plus the frozen order snapshot. The activation-confirmation first result is the `ACTIVATION_NEXT_ACTION` decision ID for every accepted branch, including already `ACTIVE`, and the rejected decision ID for a `REJECT_SUSPENDED` outcome. `begin` inserts `NEW`; only `NEW` may execute side effects; `complete` fills the first result exactly once in the same transaction. A `REPLAY` returns the stored completed result, and a changed fingerprint returns a stable `CONFLICT`.
- Event identity is `(event_domain, event_id)` where a table can be written by more than one action domain. The baseline event-domain registry is `ACTIVATION`, `IVR_CONFIRM`, `ACTIVATION_STATUS`, `OPERATIONS`, `IVR_EVENT`, `IVR_DIRECT_AUTH`, `EVIDENCE`, `PAYMENT`, `SHARE_ACCESS`, `PURCHASE` and `SUBJECT_CLAIM`. Single-domain tables store the constant domain with a `CHECK` and unique `(event_domain, event_id)`; multi-domain references store both columns. The same string may exist in different domains.
- State values are `text` plus named `CHECK` constraints. This keeps migrations expand/contract-friendly.
- All time columns are UTC `timestamptz`. IVR TTL checks use database clock time after lock acquisition, not request time or transaction-start `now()`.
- Money is integer cents plus an ISO currency code. No floating-point money.
- Each table has exactly one lifecycle class from section 8.2; every mutable table names its revision rule and every append-only table names its immutable/event identity.
- Every FK is `ON UPDATE RESTRICT ON DELETE RESTRICT`; no cascades.

### Ownership contract

- Each module schema is owned by one module role.
- A module role receives `USAGE` on its schema and write privileges only on its own tables.
- Cross-module roles may receive `SELECT` and `REFERENCES` where required for FKs, but not `INSERT`, `UPDATE` or `DELETE` on another module's tables.
- The only cross-schema write paths are the `platform-reliability` functions, the `operations-audit`-owned `audit.append_fact` function, the named `entitlement-metering.issueFirstTrial` service/function, the `activation-consent.record_activation_confirmation` published service and, for the `DIAL_TASK_CALLEE` branch, the `entitlement-metering.reserve_call_usage` published service; each is specified below and covered by privilege and atomicity tests. The IVR-owned confirmation transaction only writes `ivr` tables directly. `ivr.select_call_action` is the one shared application service for `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION`; the initial-resolve endpoint calls it directly, and `ivr.record_activation_next_action` is a thin IVR entry wrapper that delegates to it. These are application services inside the local transaction, not additional SQL `SECURITY DEFINER` functions; `activation-consent` and `entitlement-metering` never write `ivr.ivr_decision`.
- Every table and invariant has exactly one owner; collaborators in sections 8 and 9 are read-only or call a published service and never become co-owners.

### Cross-schema function contracts

All functions below are specification-level contracts. They are `SECURITY DEFINER`, owned by the module role named in the first column, and declare `SET search_path = <owning schema>, pg_temp` at definition time. `PUBLIC` receives no EXECUTE; each function is granted only to the roles listed. No function contains `COMMIT`, `ROLLBACK` or autonomous transaction control. Any raised exception aborts the caller's transaction, so the domain mutation and the function effect roll back together. The owner role must not be a superuser and must not be able to log in.

Creation waves are part of the contract: `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append` and `audit.append_fact` are created in V001/S1 after the three V001 tables; `entitlement_metering.issue_first_trial` is created in V006/S2 after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance` exist. No function is created before a table it references.

<!-- function-wave:start -->
platform.idempotency_begin|V001|platform.idempotency_record
platform.idempotency_complete|V001|platform.idempotency_record
platform.outbox_append|V001|platform.outbox_event
audit.append_fact|V001|audit.audit_fact
entitlement_metering.issue_first_trial|V006|entitlement_metering.trial_policy_version,entitlement_metering.entitlement_grant,entitlement_metering.trial_grant,entitlement_metering.entitlement_balance
<!-- function-wave:end -->

| Function | Owner and mode | Arguments | Returns | Semantics and grants |
| --- | --- | --- | --- | --- |
| `platform.idempotency_begin` | `platform-reliability`; `SECURITY DEFINER`; `search_path = platform, pg_temp`; created in V001 | `p_scope text`, `p_key text`, `p_fingerprint text` | `record_id uuid`, `revision integer`, `outcome text` (`NEW`, `REPLAY`, `CONFLICT`), `result_type text`, `result_id uuid`, `result_snapshot jsonb`, `result_sha256 text` | Performs `INSERT ... ON CONFLICT (scope, idempotency_key) DO NOTHING` and then selects the row `FOR UPDATE`. If the insert won, returns `NEW` with no result and the initial `revision` (1), which the same transaction must pass to `platform.idempotency_complete`; only `NEW` permits the caller to execute side effects. Concurrent same-key callers resolve through the unique key: a caller that loses the insert waits on the row lock and, after the winner commits, observes `COMPLETED`; if the winner rolls back, the losing insert proceeds and returns `NEW`. A same-fingerprint row with status `COMPLETED` returns `REPLAY` with the stored first result; a different fingerprint returns the stable domain outcome `CONFLICT` with no result and no side effect; a committed `NEW` row (a contract violation because begin and complete must share one transaction) raises stable `IDEMPOTENCY_INCOMPLETE` and emits an alert. Never updates a stored result. Snapshot columns must already be redacted or encrypted; the function rejects a documented denylist of plaintext protected keys. EXECUTE to every in-scope domain role. |
| `platform.idempotency_complete` | `platform-reliability`; `SECURITY DEFINER`; `search_path = platform, pg_temp`; created in V001 | `p_record_id uuid`, `p_expected_revision integer`, `p_result_type text`, `p_result_id uuid`, `p_result_snapshot jsonb`, `p_result_sha256 text` | `outcome text` (`COMPLETED`, `ALREADY_COMPLETED`) | Performs one conditional update: `UPDATE platform.idempotency_record SET status='COMPLETED', result_type=..., result_id=..., result_snapshot=..., result_sha256=..., completed_at=clock_timestamp(), revision=revision+1 WHERE id=p_record_id AND status='NEW' AND revision=p_expected_revision`. If one row is updated, returns `COMPLETED`. If the row is already `COMPLETED` with the same `result_sha256`, returns `ALREADY_COMPLETED` without changing anything; a different result hash on a `COMPLETED` row, or a revision mismatch while the row is still `NEW`, raises stable `IDEMPOTENCY_CONFLICT`. A completed result can never be overwritten. Must be called in the same transaction as its `begin`; no transaction control. Snapshot is redacted or encrypted. EXECUTE to every in-scope domain role. |
| `platform.outbox_append` | `platform-reliability`; `SECURITY DEFINER`; `search_path = platform, pg_temp` | `p_event_domain text`, `p_dedupe_key text`, `p_aggregate_type text`, `p_aggregate_id uuid`, `p_event_type text`, `p_payload_redacted jsonb`, `p_payload_ciphertext text`, `p_payload_sha256 text` | `outbox_id uuid` | Inserts an immutable Outbox row in the caller's transaction. Same `(event_domain, dedupe_key)` with the same payload hash returns the existing id; a different payload hash raises a conflict. Payload is already redacted or encrypted; plaintext protected keys are rejected. EXECUTE to every in-scope domain role. |
| `audit.append_fact` | `operations-audit`; `SECURITY DEFINER`; `search_path = audit, pg_temp`; created in V001 | `p_occurred_at timestamptz`, `p_actor_type text`, `p_actor_id uuid`, `p_action_code text`, `p_object_type text`, `p_object_id uuid`, `p_reason_code text`, `p_before_snapshot jsonb`, `p_after_snapshot jsonb`, `p_request_id text`, `p_trace_id text` | `audit_id uuid` | Appends one immutable row to `audit.audit_fact` in schema `audit`. Requires a reason for high-risk actions and stores only redacted or encrypted snapshots. No update or delete function exists. EXECUTE to domain roles and the operations service role; no table privileges. |
| `entitlement_metering.issue_first_trial` | `entitlement-metering`; `SECURITY DEFINER`; `search_path = entitlement_metering, platform, pg_temp`; created in V006 after its tables | `p_subject_id uuid`, `p_campaign_key text`, `p_trial_policy_version_id uuid`, `p_action_domain text`, `p_action_event_id text`, `p_request_fingerprint text` | `trial_grant_id uuid`, `entitlement_grant_id uuid`, `entitlement_balance_id uuid`, `outcome text` (`GRANTED`, `REPLAY`, `ALREADY_GRANTED`), `first_granted_at timestamptz` | Requires `p_campaign_key = 'FIRST-TRIAL'`. Locks the subject key. If no trial exists, inserts `entitlement_grant`, `trial_grant` (persisting `source_action_domain`, `source_action_event_id`, `request_fingerprint`), `entitlement_balance` and the Outbox event through `platform.outbox_append`, and returns `GRANTED`. If a trial exists with the same `(source_action_domain, source_action_event_id)` and the same fingerprint, returns its original ids and `REPLAY`; the same action identity with a changed fingerprint raises stable `TRIAL_ACTION_CONFLICT`; a trial granted by a different action identity returns the original ids and `ALREADY_GRANTED`. It never creates a second trial, grant or balance. If a trial exists without its grant or balance, raises a conflict and alerts. EXECUTE to `activation_consent_role` and `entitlement_metering_role` only. The application service wrapper uses `Propagation.MANDATORY`; no transaction is created or committed by the function. |

Published application-service boundaries for IVR action selection and activation confirmation (specification-level contracts; no implementation code is produced):

| Published service | Owner | Called by / transaction | Writes | Returns |
| --- | --- | --- | --- | --- |
| `activation-consent.record_activation_confirmation` | `activation-consent` | IVR activation-confirmation transaction with `Propagation.MANDATORY`; called by the wrapper after replay and the phase-A guards | Validates the current prompt and consent policy-version/key/content before any accepted append and returns a stable invalid/stale-content rejection with no accepted write; accepted branch only writes `consent_record`, `service_activation`, `service_activation_transition` | Invalid/stale content: stable `REJECT`/`CONSENT_REQUIRED` with no accepted ids; accepted branch: `consentRecordId`, `confirmationAnchorId` (new transition ID, or current latest transition ID for an already-`ACTIVE` row), `activationStatus` |
| `entitlement-metering.reserve_call_usage` | `entitlement-metering` | IVR activation-confirmation transaction with `Propagation.MANDATORY`; the resolve and direct-authorization sequences use the same reservation service | `usage_reservation` and the conditional `entitlement_balance` update | `reservationId`, `authorizedUnits`, `authorizationExpiresAt`, `balanceSnapshot`; an insufficient-availability/access condition returns a typed domain denial for the attempt savepoint and no-dial decision, while a technical error propagates to the full transaction rollback |
| `ivr.select_call_action` | `ivr` | Called by the initial-resolve endpoint (`entryMode = INITIAL_RESOLVE`) and by the activation-confirmation wrapper (`entryMode = POST_ACTIVATION`) inside the one IVR-owned transaction; direct authorization reuses its ordered predicates. It is an application service, not a SQL function | One `ivr.ivr_decision` for the derived purpose; for `DIAL_TASK_CALLEE` it additionally performs the conditional task claim, creates or binds the current `ACTIVE` `call_record` and calls `entitlement-metering.reserve_call_usage`; for a successful `DIRECT_AUTHORIZATION` it freezes `action` null with the committed reservation fields and no task; for `REJECT_CALL_IN_PROGRESS` it writes the current call's terminal `REJECTED` `call_record` and leaves the competing call untouched | `decisionId`, `action` (null only for successful direct authorization), `reasonCode`, typed snapshot, and the encrypted internal payload for `DIAL_TASK_CALLEE` or a successful `DIRECT_AUTHORIZATION` |
| `ivr.record_activation_next_action` | `ivr` | Thin IVR entry wrapper inside the activation-confirmation transaction; delegates all predicates and writes to `ivr.select_call_action` and persists no decision of its own | Exactly the `ACTIVATION_NEXT_ACTION` decision produced by the shared service; no duplicate consent/trial/claim/reservation logic | `nextActionDecisionId`, `action`, typed snapshot |

The activation-confirmation transaction is one local IVR-owned transaction; the cross-owner services run with `Propagation.MANDATORY` and never commit or roll back independently. `ivr.select_call_action` is the single predicate implementation used by both entry modes; the wrappers add only replay, authentication and the confirmation gate. A repeated confirmation with the same event ID or a new event ID with the same semantic fingerprint returns the stored `nextActionDecisionId`, action and frozen typed result; a changed fingerprint returns a stable conflict and no side effect. The `ivr` owner writes `ivr.ivr_decision`; `activation-consent` and `entitlement-metering` never write it, and the original `INBOUND_RESOLVE` decision row is never updated.

### Typed frozen action response snapshot (F-12)

Every `ivr.ivr_decision` stores one immutable, purpose-discriminated snapshot. `response_snapshot` is the typed redacted JSON used for replay, audit and ordinary inspection; `response_payload_ciphertext` is the encrypted internal payload returned only to the authenticated IVR. `response_sha256` is the SHA-256 of the canonical redacted snapshot; the encrypted payload's redacted projection must equal that snapshot, and replay decrypts the same immutable payload rather than recomputing current state. The platform idempotency `result_snapshot` stores only the redacted typed result (decision ID, purpose, action or null, reason and masked task/authorization fields) and never duplicates the full callee; replay returns the frozen decision and, for the authenticated IVR internal response, decrypts `response_payload_ciphertext`. `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION` are action-bearing; a successful `DIRECT_AUTHORIZATION` is authorization-bearing with a null action.

| Field | Type / enum | Nullability | Source and rule |
| --- | --- | --- | --- |
| `decisionId` | uuid | NOT NULL | The frozen `ivr.ivr_decision.id`; the idempotency first result |
| `callId` | text | NOT NULL | The root protocol identity; included in every fingerprint |
| `purpose` | `INBOUND_RESOLVE`, `ACTIVATION_NEXT_ACTION`, `DIRECT_AUTHORIZATION` | NOT NULL | Derived from the endpoint, never caller-supplied |
| `action` | the seven stable actions, or null | NOT NULL for `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION`; NULL for a successful `DIRECT_AUTHORIZATION`; for a direct refusal one of `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS` | Selected by the shared service; `DIRECT_AUTHORIZATION` is a purpose, not an eighth action |
| `callerState` | `UNREGISTERED`, `INACTIVE`, `ACTIVE`, `SUSPENDED` | NOT NULL | docs/13 stable enum; `UNREGISTERED` does not block IVR guidance/direct activation/trial |
| `taskMatch` | `CLAIMED`, `NONE`, `EXPIRED`, `REJECTED`, `CONFLICT` | NOT NULL | Claim outcome per the section 10.1 matrix; `NONE` means no candidate was selected/evaluated, not proof that none exists |
| `reasonCode` | stable code registry | NOT NULL | The section 10.1 matrix; `DIRECT_AUTHORIZED` for a successful direct authorization with `action` null; no free-text reason |
| `availableRecordingSeconds` | integer >= 0 | NOT NULL | Routing snapshot only, not spend authorization |
| `balanceAsOf` | timestamptz | NOT NULL | Database transaction snapshot time |
| `task.taskId` | uuid | NULL unless a candidate/claimed task was evaluated; always NULL for `DIRECT_AUTHORIZATION` | Frozen task identity; `CLAIMED` requires it |
| `task.businessType` | text | NULL unless a task exists; always NULL for `DIRECT_AUTHORIZATION` | Frozen task business type |
| `task.calleeNumber` | text | NULL in the redacted snapshot; NOT NULL only inside the encrypted internal payload when `action = DIAL_TASK_CALLEE` | Task-path callee only; never used for direct authorization |
| `task.calleeMasked` | text | NULL unless a task exists; always NULL for `DIRECT_AUTHORIZATION` | Safe display/log value |
| `authorization.calleeNumber` | text | NULL except a successful `DIRECT_AUTHORIZATION`, and then only inside the encrypted internal payload | Direct confirmed callee; never in task fields, the redacted snapshot, logs, audit or customer responses |
| `reservationId` | uuid | NOT NULL iff `action = DIAL_TASK_CALLEE` or (`purpose = DIRECT_AUTHORIZATION` and `action` is NULL); otherwise NULL | Committed reservation only; a direct refusal has none |
| `authorizedRecordingSeconds` | integer > 0 | NOT NULL iff a reservation exists | Committed `usage_reservation.authorized_units` under the baseline 1 unit = 1 second conversion |
| `authorizationExpiresAt` | timestamptz | NOT NULL iff a reservation exists | Copied from the committed reservation; last instant to start the B-leg |
| `decisionAt` | timestamptz | NOT NULL | Decision commit time; never part of a fingerprint |

Action-specific consistency rules:

- `DIAL_TASK_CALLEE`: `taskMatch = CLAIMED`, `taskId`, `businessType`, `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt` are non-null; the encrypted internal payload contains the full `calleeNumber`. The response is returned only after the task claim, reservation and decision commit in one transaction; a technical failure rolls all three back, while an expected claim/reservation denial yields a frozen no-dial decision instead (category 2) with no `DIAL_TASK_CALLEE`.
- Successful `DIRECT_AUTHORIZATION` (`purpose = DIRECT_AUTHORIZATION`, `action` is null/omitted): `taskMatch = NONE`, task fields are null, and `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt` are non-null; the encrypted payload contains the confirmed callee only under `authorization.calleeNumber`. It never carries a task ID, never uses task fields for the callee, never returns `ENTER_CALLEE` after the callee was entered/confirmed, and is returned only after the reservation and decision commit.
- Direct refusal (`purpose = DIRECT_AUTHORIZATION`, `action` one of `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS`): `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt` are null; there is no authorization object or direct callee; no claim is made.
- `ENTER_CALLEE`, `GUIDE_PURCHASE`, `GUIDE_ACTIVATION`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS` on `INBOUND_RESOLVE`/`ACTIVATION_NEXT_ACTION`: `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt` are null; no claim is made; `ENTER_CALLEE` is only the pre-input no-task instruction and requires a positive no-`PENDING`/`CLAIMED`-task check.
- `availableRecordingSeconds` may be zero or positive for any action and never overrides the authorized seconds.
- The full callee is never written to `response_snapshot`, logs, audit, Outbox or a customer response. The redacted snapshot and logs use `calleeMasked` and task/decision IDs only; a direct authorization's redacted projection uses a masked callee, and `authorization.calleeNumber` exists only in the encrypted payload.

B-leg and replay rules: after a committed `DIAL_TASK_CALLEE` or successful `DIRECT_AUTHORIZATION`, the IVR must persist a unique `callId` B-leg operation in its existing local authoritative store before issuing any line request; that local durable operation, not the business `ivr.call_leg` callback/fact table, proves outbound uniqueness. The executable local key/protocol and its lost-response recovery are deferred to the IVR protocol work unit, and no business schema expansion is added here. The IVR must not start a new B-leg at or after `authorizationExpiresAt`, and a response lost or an unknown line outcome is reconciled or recovered by `callId` without a second dial. The task path never prompts the caller to confirm the callee. A replay after the cutoff returns the same frozen payload and cutoff, never renews the authorization, and never becomes redial permission; a B-leg that started before the cutoff continues under the frozen authorized seconds.

Privilege/atomicity tests required by the final specification:

- a domain role cannot `INSERT`, `UPDATE` or `DELETE` `platform.*` or `audit.*` tables but can EXECUTE the named functions;
- a domain role cannot write `entitlement_metering.trial_grant`, `entitlement_grant` or `entitlement_balance` directly, but `activation_consent_role` can EXECUTE `issue_first_trial`;
- V001 alone creates and tests the four platform/audit functions; applying only V001-V003 never creates `issue_first_trial`; V006 creates it after its tables and S2 tests it;
- `begin` returns `NEW` once and only `NEW` executes side effects; a concurrent same-key insert waits, then returns `REPLAY` after the winner commits; a lost insert race never produces two `NEW` outcomes;
- `complete` fills the result exactly once; a second `complete` with the same result returns `ALREADY_COMPLETED`, a different result or revision raises conflict, and a completed result is never overwritten;
- a committed `NEW` row without completion raises `IDEMPOTENCY_INCOMPLETE` and alerts;
- a forced failure after `begin`, `complete`, `outbox_append` or `issue_first_trial` rolls back the domain row, the idempotency/outbox row, its completion and the entitlement rows together;
- same-key/same-fingerprint replay returns the first completed result and creates no second side effect; same-key/different-fingerprint returns the stable `CONFLICT` outcome and creates no side effect; the order `PURCHASE` path returns the first order and returns `CONFLICT` on a changed input;
- `issue_first_trial` returns `GRANTED`, then `REPLAY` for the same action and fingerprint, conflicts for the same action and changed fingerprint, and `ALREADY_GRANTED` for a different action on a granted subject without a second grant or balance;
- evidence record and creation event can be inserted in either order and commit under both deferred FKs; duplicate, mismatched or missing `EVIDENCE_CREATED` rows fail at commit;
- a protected plaintext key in a payload is rejected; encrypted or redacted payloads are accepted;
- `search_path` shadowing cannot redirect an unqualified table or function reference;
- a `SUSPENDED` subject confirmation with no prior trial returns `REJECT_SUSPENDED`, writes no accepted consent, transition, trial/grant/balance, reservation or success Outbox, and leaves the activation suspended;
- a `SUSPENDED` subject confirmation with an existing trial makes no state change and creates no second trial, grant, balance or reservation;
- a concurrent operations suspension and IVR confirmation serialized by the subject lifecycle lock produce either a committed suspension followed by `REJECT_SUSPENDED`, or a committed confirmation whose granted trial is not undone; the confirmation never acts on a stale non-suspended status after lock acquisition;
- an operations resume after suspension appends only `SUSPENDED → ACTIVE` and does not call `issueFirstTrial` or create any trial, grant or balance; a later confirmation returns `ALREADY_GRANTED` with the original ids;
- each post-confirmation next action is persisted and returned: `DIAL_TASK_CALLEE`, `ENTER_CALLEE`, `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED` and `REJECT_CALL_IN_PROGRESS`;
- an organization task whose organization is `SUSPENDED`/`DISABLED`, whose creator/owner account is `DISABLED`, or whose creator membership is no longer `ACTIVE` with task access is not claimable; each lifecycle denial returns the stable access rejection and never `ENTER_CALLEE`/direct dialing. The suspension/disable tests cover the prompt window and the interval between the access recheck and the conditional claim; because the claim holds the same lifecycle/access locks continuously, they assert the two serial orders — the status change commits before the confirmation acquires the locks, or the confirmation/claim commits first — and never a mid-lock interleaving;
- an unregistered/unverified/unowned direct IVR caller can still reach direct activation, trial and recording; no test requires a customer login, membership, owner account or real-name status on the direct no-task path;
- an `ACTIVE` activation with missing/expired current consent returns `GUIDE_ACTIVATION`/`CONSENT_REQUIRED` on initial resolve, and the confirmation path appends current consent while returning the existing trial (`ALREADY_GRANTED`/`REPLAY`) rather than issuing a duplicate; stale/invalid confirmation content is rejected before accepted side effects;
- a successful direct authorization stores `action` null, stable `reasonCode = DIRECT_AUTHORIZED`, null task fields, the committed reservation/authorized-seconds/cutoff and the confirmed callee only under `authorization.calleeNumber` in the encrypted payload; a direct refusal stores a permitted non-dial action with its stable reason and no reservation; neither returns `ENTER_CALLEE` after callee confirmation, and the redacted projection contains no full callee;
- category 1 writes only the rejection/completion (and a permitted terminal current call); category 2 commits valid activation/trial with the one no-dial decision and no claim/reservation; category 3 forced after each write rolls back the `NEW` row, activation/trial, tentative claim/reservation and decision/completion together and returns a bounded retryable error; no database exception is swallowed;
- an expected reservation denial after a tentative claim rolls back to the same-connection attempt savepoint, freezes one no-dial decision, and leaves valid activation/trial committed; no autonomous transaction, second service or independent commit exists;
- a fresh attempt on a current `UNKNOWN` call with inconsistent line state returns `REJECT`/`CALL_STATE_UNRESOLVED` with no reservation, dial, release or `UNKNOWN`-to-`REJECTED` change, while replay returns the immutable decision first; the competing call is untouched;
- a duplicate task inside the default 30-second window returns the existing task without refreshing `expires_at`; `resolve` replay returns the frozen first result before task-expiry housekeeping;
- a confirmation on an already-`ACTIVE` activation completes idempotency with the `ACTIVATION_NEXT_ACTION` decision ID, and a repeat returns the same decision and action;
- an identical confirmation replay returns the first decision and action; a repeated `resolve` returns the original frozen `GUIDE_ACTIVATION` decision, and the `ACTIVATION_NEXT_ACTION` row never overwrites it;
- a task that expires before the post-activation recheck is not claimed and yields a non-dial next action (`ENTER_CALLEE` when balance remains and a positive no-task check passes, otherwise `GUIDE_PURCHASE` or `REJECT`);
- confirmation retry creates no duplicate task claim, reservation or usage fact.

Required IVR contract semantic validation assertions (specification-level test requirements, not implemented test code):

- Both `INITIAL_RESOLVE` and `POST_ACTIVATION` execute the same ordered predicate set; a fault injected into any shared predicate changes both entry results consistently, and no predicate exists only in one wrapper.
- A call whose own `call_record` is `ACTIVE`/`UNKNOWN` passes the active-call guard because the current `callId` is excluded; the same call is never rejected as competing with itself.
- Another `ACTIVE` or `UNKNOWN` call for the canonical caller is rejected with `REJECT_CALL_IN_PROGRESS`, `taskMatch = NONE`, no claim and no reservation, while the competing call's row and decision are unchanged; the rejected call has a valid terminal `REJECTED` `call_record`.
- A fresh activation confirmation and a direct DTMF authorization race for the same canonical caller; the caller lock and guard produce one live call and a stable rejection for the other, never two active guards or a raw unique violation.
- Same-event replay returns the first decision; a new event ID with the same semantic fingerprint returns the existing decision and completes the new idempotency row without repeating consent/trial/claim/reservation/Outbox; a new event ID with changed consent, prompt, content or call returns a stable conflict and leaves no `NEW` row; the same event ID reused on another call conflicts.
- A repeat after operations suspension with a `CONFIRMED` session returns the original decision and never changes `CONFIRMED → REJECTED`; a fresh suspended confirmation returns `REJECT_SUSPENDED` with no accepted side effect.
- A valid `PENDING` task with zero available entitlement returns `GUIDE_PURCHASE`, performs no claim and no reservation, and leaves the task `PENDING`; a valid task with positive entitlement claims and reserves atomically.
- Membership, organization, owner/creator account, creator authorization or caller-binding revocation yields a stable `REJECT` for access on the task path, never `ENTER_CALLEE`, and never silently bypasses authorization. The revocation and claim serialize on the same lifecycle/access row and subject locks: the test asserts the two possible serial orders (revocation commits before the confirmation acquires the locks; or the confirmation/claim commits first and the revocation governs later calls), never an interleaving while the same locks are already held.
- An access revocation racing a task-backed activation confirmation is asserted in both serial orders: when the revocation commits before the confirmation's lock acquisition, phase B yields the category-2 no-dial access `REJECT` with valid activation/trial, `CONFIRMED` session, no claim and no reservation; when the confirmation/claim commits first while every task/entitlement predicate passes, the frozen result may be `DIAL_TASK_CALLEE` with its claim and reservation, and the later revocation affects only later calls. The test never rereads authorization after commit and never accepts a reversal or invented rejection of the committed decision.
- An early refusal before task evaluation reports `taskMatch = NONE` without claiming that no task exists; `ENTER_CALLEE` and direct authorization are tested only after a positive no-`PENDING`/`CLAIMED`-task check, and task fields are null for an unevaluated candidate.
- An `ACTIVE` activation with missing/expired current consent yields `GUIDE_ACTIVATION`/`CONSENT_REQUIRED` on initial resolve and no duplicate trial on confirmation; stale/invalid confirmation content is validated once by `activation-consent.record_activation_confirmation` after replay/phase-A guards and before any accepted append, returns `REJECT`/`CONSENT_REQUIRED` with no accepted effect, and phase B then checks the resulting consent is current.
- A successful direct authorization has `action` null, non-null reservation/authorized-seconds/cutoff, null task fields and the callee only in the encrypted `authorization.calleeNumber`; a direct refusal has a permitted non-dial action and no reservation; neither is `ENTER_CALLEE` after callee confirmation, and the redacted snapshot contains no full callee.
- Category 1, category 2 and category 3 outcomes are asserted separately: category 2 commits activation/trial and the no-dial decision without a claim/reservation after a savepoint rollback, while category 3 forced at each write boundary commits no decision and rolls back every write; no contradictory blanket rollback is accepted.
- A current `UNKNOWN` call returns its immutable replay first; a fresh inconsistent attempt returns `REJECT`/`CALL_STATE_UNRESOLVED` without destructive change, reservation or dial, and only a never-started/rejectable current `ACTIVE` row may become `REJECTED`.
- Initial-resolve replay is returned before task-expiry housekeeping; a duplicate task inside the 30-second default window never refreshes `expires_at`.
- An `OPEN` claim-only risk state does not by itself reject a new outbound call; a number claim and historical-evidence access remain blocked. No unsupported blanket call-ban predicate is introduced.
- A task expiring between the pre-check and the conditional `UPDATE` affects zero rows, produces one bounded reread with `taskMatch = EXPIRED`, and yields a non-dial action (`ENTER_CALLEE` with balance, otherwise `GUIDE_PURCHASE`); no expired task is claimed.
- A cancelled or replaced task falls through as `NONE` only when activation/consent/access/entitlement remain valid; a stale task revision fails the claim and yields a stable non-dial result without choosing another task.
- An expected reservation race/denial rolls back the task claim and reservation to the attempt savepoint and freezes one no-dial decision; a forced technical reservation failure is category 3 and rolls back the whole transaction. No `CLAIMED` task commits without a reservation, no second reservation is authored, and the action is `GUIDE_PURCHASE` or a stable rejection.
- Initial resolve and activation decisions are immutable and distinct; repeated resolve returns the original `GUIDE_ACTIVATION`, the activation decision never overwrites it, and the activation decision's ID is the first-result ID.
- A lost `DIAL_TASK_CALLEE` response is recovered by `callId`; no second B-leg is started and the same frozen reservation fields return.
- Replaying an authorization at or after `authorizationExpiresAt` returns the same frozen cutoff and never renews it or authorizes a new B-leg; a B-leg started before the cutoff continues under the frozen seconds.
- The redacted snapshot, logs, audit and customer responses contain no full callee; only the authenticated IVR internal payload decrypts the frozen full callee, and replay returns the same logical payload.
- The `DIAL_TASK_CALLEE` snapshot's `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt` exactly equal the committed reservation's ID, frozen authorized seconds and cutoff; `availableRecordingSeconds` remains routing-only.
- The existing suspended, first-trial, ownership, direct-authorization, task-lifecycle and structural tests remain in force.

### State-machine contract

The final specification must use the states and transition guards in section 10. A state change must be represented by a conditional update and, where history or ownership requires it, a new transition/event fact. Terminal task, activation, reservation, issuance, order, evidence and share states are immutable. Unknown line outcomes remain occupied until reconciliation, are never treated as success, failure or permission to redial, and a current `UNKNOWN` call is never finalized `REJECTED` or released; only a never-started/rejectable `ACTIVE` current row may be finalized `REJECTED`. PostgreSQL owns the transaction; Redis playback tickets and object-storage steps happen after commit with idempotent retry and never create a second domain fact. The final specification must not introduce catch-all statuses such as `PROCESSING`, `SUCCESS` or `FAILED` that combine payment, entitlement, call and evidence dimensions.

### Sensitive-field and migration contract

Sections 8.1 and 8.2 are binding: every protected column uses ciphertext, HMAC, masked or redacted representation, and every table has a revision/immutability and deletion class. Section 12 is binding: a cross-wave FK column may exist before its parent table, but the FK is added only in the later wave; same-wave references are created in dependency order; same-wave circular FKs are added after both tables exist and use deferral only where insertion order requires it. No wave contains a forward reference and no retention period, capacity value or provider credential is invented.

### Public-interface impact

No external API, OpenAPI document, UI or runtime interface is changed in this work unit. The final schema specification is an internal contract for later APIs. It must mark fields that may never be returned directly: ciphertexts, HMACs, key references, object keys/versions, provider payloads, login/auth subjects, full numbers, code hashes and Redis ticket values.

## 12. Migration Order

The final specification uses nine forward-only Flyway migration waves. No wave may reference a table created later. A column that needs a later parent is created without its FK, and the FK is added in the later wave that creates the parent. Same-wave circular references are added after both tables exist. The baseline has no data backfill. Future migrations use expand/contract compatibility.

| Wave | Proposed migration spec name | Tables created | Reason for order |
| --- | --- | --- | --- |
| V001 | `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `platform.idempotency_record`, `platform.outbox_event`, `audit.audit_fact` | Every later module needs idempotency begin/complete, Outbox, audit and the four platform/audit functions; no entitlement table or function is created here |
| V002 | `V002__identity_organization_and_employee.sql` | `customer_iam.customer_account`, `customer_iam.auth_identity`, `customer_iam.identity_profile`, `organization_access.organization`, `organization_access.organization_membership`, `operations_iam.employee_account` | Stable account, organization and employee identities are parents for subject, task, order and evidence facts |
| V003 | `V003__subject_number_and_claim.sql` | `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_claim`, `subject_number.subject_risk_state` | Activation, IVR, orders and evidence require a stable subject and number binding |
| V004 | `V004__catalog_order_and_payment.sql` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `catalog_order.customer_order`, `catalog_order.payment_order`, `catalog_order.payment_event` | Entitlement issuance consumes immutable order snapshots and verified payment events |
| V005 | `V005__ivr_task_session_call.sql` | `ivr.outbound_call_task`, `ivr.inbound_session`, `ivr.call_record`, `ivr.call_leg`, `ivr.call_event`, `ivr.ivr_decision` | Reservation and evidence require call/session/task identities; `call_record` is the canonical call parent |
| V006 | `V006__trial_entitlement_issuance_reservation_usage.sql` | `entitlement_metering.trial_policy_version`, `entitlement_metering.entitlement_grant`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_balance`, `entitlement_metering.entitlement_issuance`, `entitlement_metering.usage_reservation`, `entitlement_metering.usage_fact` | Issuance, reservation and usage have order, call and grant parents from earlier waves or the same wave; `entitlement_metering.issue_first_trial` is created here after its four tables exist |
| V007 | `V007__activation_and_consent.sql` | `activation_consent.consent_record`, `activation_consent.service_activation`, `activation_consent.service_activation_transition` | Consent/activation link to IVR sessions created in V005 and orders created in V004; activation remains separate from trial/reservation tables and calls their published service |
| V008 | `V008__evidence_media.sql` | `evidence.media_object`, `evidence.recording`, `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.evidence_event` | Evidence links call, reservation and server-validated media objects; creation-event ownership is enforced at commit |
| V009 | `V009__sharing.sql` | `sharing.evidence_share_grant`, `sharing.share_admission`, `sharing.share_access_event` | Sharing requires solidified evidence and appends access facts after all parents exist |

FK creation modes are distinct and must not be conflated:

- `FK` is an ordinary, immediate foreign key created in the child's wave (same or later than the parent wave).
- `ALTER_FK` is an ordinary, immediate foreign key added by `ALTER TABLE` because its column was created earlier or the same-wave tables need a defined creation order. It is **not** `DEFERRABLE`.
- `DEFERRABLE_FK` is a PostgreSQL `DEFERRABLE INITIALLY DEFERRED` constraint, used only for the two sides of the evidence record/creation-event cycle.
- `NO_FK` documents an intentional root or polymorphic reference.

| Foreign key | Mode | Added in | Why it is late or deferred |
| --- | --- | --- | --- |
| `ivr.ivr_decision.reservation_id` → `entitlement_metering.usage_reservation.id` | `ALTER_FK` | V006 | The nullable decision column exists in V005; the parent table is created in V006; ordinary immediate FK, not deferrable |
| `catalog_order.customer_order.purchase_consent_id` → `activation_consent.consent_record.id` | `ALTER_FK` | V007 | The order column exists in V004; the consent table is created in V007; ordinary immediate FK, not deferrable |
| `ivr.outbound_call_task.claimed_call_id` → `ivr.call_record.call_id` | `ALTER_FK` | V005 | Same-wave circular link; `outbound_call_task` is created first, `call_record` second, and the FK is added after both; ordinary immediate FK, not deferrable because the task is updated only after the call exists |
| `evidence.evidence_record.(source_event_domain, source_event_id)` → `evidence.evidence_event.(event_domain, event_id)` | `DEFERRABLE_FK` | V008 | Composite circular creation link; `DEFERRABLE INITIALLY DEFERRED` so record and creation event may be inserted in either order in one transaction |
| `evidence.evidence_event.evidence_id` → `evidence.evidence_record.id` | `DEFERRABLE_FK` | V008 | Opposite side of the same cycle; `DEFERRABLE INITIALLY DEFERRED` so either insertion order commits |

No other foreign key in this baseline is deferrable. A deferred constraint trigger on `evidence.evidence_record` (`AFTER INSERT OR UPDATE`, `DEFERRABLE INITIALLY DEFERRED`) validates at commit that exactly one `EVIDENCE_CREATED` event exists for the record and that its `(event_domain, event_id)` equals the record's `(source_event_domain, source_event_id)`.

Complete FK catalog. `NO_FK` rows document the intentional absence of a parent for a root protocol identity or polymorphic internal reference. For every other row, the parent wave is the same as or earlier than the FK wave, and the slice of the child is the same as or later than the slice of the parent. `ALTER_FK` and `DEFERRABLE_FK` are the only names beside ordinary `FK` and `NO_FK`.

<!-- fk-matrix:start -->
customer_iam.auth_identity.account_id|customer_iam.customer_account.id|V002|V002|FK|same wave
customer_iam.identity_profile.account_id|customer_iam.customer_account.id|V002|V002|FK|same wave
organization_access.organization_membership.organization_id|organization_access.organization.id|V002|V002|FK|same wave
organization_access.organization_membership.account_id|customer_iam.customer_account.id|V002|V002|FK|same wave
subject_number.business_subject.owner_account_id|customer_iam.customer_account.id|V002|V003|FK|cross wave
subject_number.business_subject.owner_organization_id|organization_access.organization.id|V002|V003|FK|cross wave
subject_number.caller_number_binding.subject_id|subject_number.business_subject.id|V003|V003|FK|same wave
subject_number.subject_claim.subject_id|subject_number.business_subject.id|V003|V003|FK|same wave
subject_number.subject_claim.account_id|customer_iam.customer_account.id|V002|V003|FK|cross wave
subject_number.subject_claim.organization_id|organization_access.organization.id|V002|V003|FK|cross wave
subject_number.subject_risk_state.caller_number_binding_id|subject_number.caller_number_binding.id|V003|V003|FK|same wave
catalog_order.catalog_version.product_id|catalog_order.catalog_product.id|V004|V004|FK|same wave
catalog_order.catalog_version.created_by_employee_id|operations_iam.employee_account.id|V002|V004|FK|cross wave
catalog_order.catalog_offer.catalog_version_id|catalog_order.catalog_version.id|V004|V004|FK|same wave
catalog_order.catalog_benefit_line.catalog_version_id|catalog_order.catalog_version.id|V004|V004|FK|same wave
catalog_order.customer_order.account_id|customer_iam.customer_account.id|V002|V004|FK|cross wave
catalog_order.customer_order.owner_organization_id|organization_access.organization.id|V002|V004|FK|cross wave
catalog_order.customer_order.subject_id|subject_number.business_subject.id|V003|V004|FK|cross wave
catalog_order.customer_order.catalog_version_id|catalog_order.catalog_version.id|V004|V004|FK|same wave
catalog_order.customer_order.catalog_offer_id|catalog_order.catalog_offer.id|V004|V004|FK|same wave
catalog_order.customer_order.purchase_consent_id|activation_consent.consent_record.id|V007|V007|ALTER_FK|column in V004; ordinary immediate FK added by ALTER in V007, not deferrable
catalog_order.payment_order.order_id|catalog_order.customer_order.id|V004|V004|FK|same wave
catalog_order.payment_event.payment_order_id|catalog_order.payment_order.id|V004|V004|FK|same wave
ivr.outbound_call_task.caller_binding_id|subject_number.caller_number_binding.id|V003|V005|FK|cross wave
ivr.outbound_call_task.subject_id|subject_number.business_subject.id|V003|V005|FK|cross wave
ivr.outbound_call_task.owner_account_id|customer_iam.customer_account.id|V002|V005|FK|cross wave
ivr.outbound_call_task.owner_organization_id|organization_access.organization.id|V002|V005|FK|cross wave
ivr.outbound_call_task.created_by_account_id|customer_iam.customer_account.id|V002|V005|FK|cross wave
ivr.outbound_call_task.claimed_call_id|ivr.call_record.call_id|V005|V005|ALTER_FK|same-wave ALTER after call_record; ordinary immediate FK, not deferrable
ivr.inbound_session.subject_id|subject_number.business_subject.id|V003|V005|FK|cross wave
ivr.inbound_session.confirmed_subject_id|subject_number.business_subject.id|V003|V005|FK|cross wave
ivr.inbound_session.call_id|-|-|-|NO_FK|root IVR protocol identity with no durable parent
ivr.call_record.call_id|-|-|-|NO_FK|canonical call-scoped parent created in resolve or direct authorization
ivr.call_record.session_id|ivr.inbound_session.id|V005|V005|FK|same wave
ivr.call_record.task_id|ivr.outbound_call_task.id|V005|V005|FK|same wave nullable for direct path
ivr.call_leg.call_id|ivr.call_record.call_id|V005|V005|FK|same wave
ivr.call_event.call_id|ivr.call_record.call_id|V005|V005|FK|same wave
ivr.ivr_decision.session_id|ivr.inbound_session.id|V005|V005|FK|same wave
ivr.ivr_decision.call_id|ivr.call_record.call_id|V005|V005|FK|same wave
ivr.ivr_decision.reservation_id|entitlement_metering.usage_reservation.id|V006|V006|ALTER_FK|column in V005; ordinary immediate FK added by ALTER in V006, not deferrable
entitlement_metering.trial_policy_version.published_by_employee_id|operations_iam.employee_account.id|V002|V006|FK|cross wave
entitlement_metering.entitlement_grant.subject_id|subject_number.business_subject.id|V003|V006|FK|cross wave
entitlement_metering.entitlement_grant.order_id|catalog_order.customer_order.id|V004|V006|FK|cross wave
entitlement_metering.trial_grant.subject_id|subject_number.business_subject.id|V003|V006|FK|cross wave
entitlement_metering.trial_grant.trial_policy_version_id|entitlement_metering.trial_policy_version.id|V006|V006|FK|same wave
entitlement_metering.trial_grant.entitlement_grant_id|entitlement_metering.entitlement_grant.id|V006|V006|FK|same wave unique
entitlement_metering.entitlement_balance.grant_id|entitlement_metering.entitlement_grant.id|V006|V006|FK|same wave unique
entitlement_metering.entitlement_issuance.order_id|catalog_order.customer_order.id|V004|V006|FK|cross wave
entitlement_metering.entitlement_issuance.grant_id|entitlement_metering.entitlement_grant.id|V006|V006|FK|same wave nullable unique
entitlement_metering.usage_reservation.call_id|ivr.call_record.call_id|V005|V006|FK|cross wave
entitlement_metering.usage_reservation.balance_id|entitlement_metering.entitlement_balance.id|V006|V006|FK|same wave
entitlement_metering.usage_reservation.subject_id|subject_number.business_subject.id|V003|V006|FK|cross wave
entitlement_metering.usage_fact.reservation_id|entitlement_metering.usage_reservation.id|V006|V006|FK|same wave
entitlement_metering.usage_fact.balance_id|entitlement_metering.entitlement_balance.id|V006|V006|FK|same wave
entitlement_metering.usage_fact.call_id|ivr.call_record.call_id|V005|V006|FK|cross wave
activation_consent.consent_record.subject_id|subject_number.business_subject.id|V003|V007|FK|cross wave
activation_consent.consent_record.ivr_session_id|ivr.inbound_session.id|V005|V007|FK|cross wave nullable for MINIAPP and WEB
activation_consent.consent_record.account_id|customer_iam.customer_account.id|V002|V007|FK|cross wave nullable
activation_consent.consent_record.employee_id|operations_iam.employee_account.id|V002|V007|FK|cross wave nullable
activation_consent.service_activation.subject_id|subject_number.business_subject.id|V003|V007|FK|cross wave unique
activation_consent.service_activation_transition.activation_id|activation_consent.service_activation.id|V007|V007|FK|same wave
activation_consent.service_activation_transition.subject_id|subject_number.business_subject.id|V003|V007|FK|cross wave
activation_consent.service_activation_transition.actor_account_id|customer_iam.customer_account.id|V002|V007|FK|cross wave nullable
activation_consent.service_activation_transition.actor_employee_id|operations_iam.employee_account.id|V002|V007|FK|cross wave required for OPERATIONS
activation_consent.service_activation_transition.consent_record_id|activation_consent.consent_record.id|V007|V007|FK|same wave nullable for OPERATIONS
evidence.recording.call_id|ivr.call_record.call_id|V005|V008|FK|cross wave
evidence.recording.media_object_id|evidence.media_object.id|V008|V008|FK|same wave
evidence.evidence_record.subject_id|subject_number.business_subject.id|V003|V008|FK|cross wave
evidence.evidence_record.call_id|ivr.call_record.call_id|V005|V008|FK|cross wave nullable
evidence.evidence_record.session_id|ivr.inbound_session.id|V005|V008|FK|cross wave nullable
evidence.evidence_record.reservation_id|entitlement_metering.usage_reservation.id|V006|V008|FK|cross wave nullable
evidence.evidence_record.source_event_domain,evidence.evidence_record.source_event_id|evidence.evidence_event.event_domain,evidence.evidence_event.event_id|V008|V008|DEFERRABLE_FK|composite circular creation link; DEFERRABLE INITIALLY DEFERRED
evidence.evidence_artifact.evidence_id|evidence.evidence_record.id|V008|V008|FK|same wave
evidence.evidence_artifact.media_object_id|evidence.media_object.id|V008|V008|FK|same wave
evidence.evidence_event.evidence_id|evidence.evidence_record.id|V008|V008|DEFERRABLE_FK|opposite side of the evidence creation cycle; DEFERRABLE INITIALLY DEFERRED
sharing.evidence_share_grant.evidence_id|evidence.evidence_record.id|V008|V009|FK|cross wave
sharing.evidence_share_grant.created_by_account_id|customer_iam.customer_account.id|V002|V009|FK|cross wave
sharing.evidence_share_grant.owner_organization_id|organization_access.organization.id|V002|V009|FK|cross wave nullable
sharing.share_admission.share_grant_id|sharing.evidence_share_grant.id|V009|V009|FK|same wave
sharing.share_access_event.share_grant_id|sharing.evidence_share_grant.id|V009|V009|FK|same wave
sharing.share_access_event.share_admission_id|sharing.share_admission.id|V009|V009|FK|same wave nullable before admission
<!-- fk-matrix:end -->

Machine-readable candidate-key catalog. Every parent column set referenced by the FK matrix must appear here as `PK` or `UNIQUE`; the structural script rejects any FK target that does not.

<!-- key-catalog:start -->
customer_iam.customer_account|id|PK
organization_access.organization|id|PK
subject_number.business_subject|id|PK
subject_number.caller_number_binding|id|PK
catalog_order.catalog_product|id|PK
catalog_order.catalog_version|id|PK
catalog_order.catalog_offer|id|PK
catalog_order.customer_order|id|PK
catalog_order.payment_order|id|PK
operations_iam.employee_account|id|PK
ivr.inbound_session|id|PK
ivr.outbound_call_task|id|PK
ivr.call_record|call_id|UNIQUE
entitlement_metering.trial_policy_version|id|PK
entitlement_metering.entitlement_grant|id|PK
entitlement_metering.entitlement_balance|id|PK
entitlement_metering.usage_reservation|id|PK
activation_consent.consent_record|id|PK
activation_consent.service_activation|id|PK
evidence.media_object|id|PK
evidence.evidence_record|id|PK
evidence.evidence_event|event_domain,event_id|UNIQUE
sharing.evidence_share_grant|id|PK
sharing.share_admission|id|PK
<!-- key-catalog:end -->

Intra-wave creation order (referenced tables first):

- V001: schemas and roles; `platform.idempotency_record` → `platform.outbox_event` → `audit.audit_fact`; then create `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append` and `audit.append_fact`, plus the append-only triggers. `entitlement_metering.issue_first_trial` is not created here.
- V002: `customer_iam.customer_account` → `auth_identity` → `identity_profile`; `organization_access.organization` → `organization_membership`; `operations_iam.employee_account`.
- V003: `subject_number.business_subject` → `caller_number_binding` → `subject_claim` → `subject_risk_state`.
- V004: `catalog_order.catalog_product` → `catalog_version` → `catalog_offer` and `catalog_benefit_line` → `customer_order` (consent FK deferred to V007) → `payment_order` → `payment_event`.
- V005: `ivr.outbound_call_task` → `inbound_session` → `call_record` → `call_leg` → `call_event` → `ivr_decision`; then `ALTER TABLE ivr.outbound_call_task ADD FOREIGN KEY (claimed_call_id) REFERENCES ivr.call_record(call_id)` (ordinary immediate FK); the `ivr_decision.reservation_id` ordinary FK is added by `ALTER` in V006.
- V006: `entitlement_metering.trial_policy_version` → `entitlement_grant` → `trial_grant` → `entitlement_balance` → `entitlement_issuance` → `usage_reservation` → `usage_fact`; then create `entitlement_metering.issue_first_trial` (all four tables it references now exist); then add the `ivr_decision.reservation_id` ordinary FK.
- V007: `activation_consent.consent_record` → `service_activation` → `service_activation_transition`; then add the `customer_order.purchase_consent_id` ordinary immediate FK by `ALTER`.
- V008: `evidence.media_object` → `recording` → `evidence_record` (columns only; no FK to `evidence_event` yet) → `evidence_event` (columns only; no FK to `evidence_record` yet) → then `ALTER TABLE evidence.evidence_event ADD CONSTRAINT evidence_event_evidence_fk FOREIGN KEY (evidence_id) REFERENCES evidence.evidence_record(id) DEFERRABLE INITIALLY DEFERRED` → then `ALTER TABLE evidence.evidence_record ADD CONSTRAINT evidence_record_creation_event_fk FOREIGN KEY (source_event_domain, source_event_id) REFERENCES evidence.evidence_event(event_domain, event_id) DEFERRABLE INITIALLY DEFERRED` → then create the `DEFERRABLE INITIALLY DEFERRED` constraint trigger that validates exactly one matching `EVIDENCE_CREATED` event at commit → `evidence_artifact`.
- V009: `sharing.evidence_share_grant` → `share_admission` → `share_access_event`.

The final specification must annotate every migration with its exact tables, indexes, FK actions, grant statements and validation queries. It must state that Flyway checksums are immutable and that no destructive rollback is expected.

## 13. Behaviorally Bounded Implementation Slices

The slices are ordered review/write boundaries for the final specification, not separate executable migrations. Each slice can be migrated and tested on only its own and earlier waves. Later slices consume only frozen contracts from earlier slices; no slice names a later slice as a prerequisite. The platform controls are in S1 so every later slice can rely on idempotency, Outbox, audit, deletion and sensitive-field policy.

### Slice S1

Platform, identity and subject foundation.

- Waves: V001-V003.
- Tables: 13 (`platform.*`, `audit.audit_fact`, `customer_iam.*`, `organization_access.*`, `operations_iam.employee_account`, `subject_number.*`).
- Table membership (exact):
<!-- slice-S1-tables:start -->
- `platform.idempotency_record`
- `platform.outbox_event`
- `audit.audit_fact`
- `customer_iam.customer_account`
- `customer_iam.auth_identity`
- `customer_iam.identity_profile`
- `organization_access.organization`
- `organization_access.organization_membership`
- `operations_iam.employee_account`
- `subject_number.business_subject`
- `subject_number.caller_number_binding`
- `subject_number.subject_claim`
- `subject_number.subject_risk_state`
<!-- slice-S1-tables:end -->
- Produces: conventions, schemas/roles/privilege model, idempotency begin/complete and Outbox tables and functions, audit fact/`audit.append_fact`, identity/access tables, subject/number/claim/risk tables, and the sensitive/deletion/revision policy.
- Invariants: INV-01 through INV-07, INV-27 through INV-31, INV-33, and the platform/Outbox/audit portion of INV-40.
- Behavior verified: platform idempotency begin/complete, Outbox and audit controls and their privilege/atomicity contracts are usable by later slices; login identity cannot become a business number or employee; subject ownership is exclusive when set and an unowned IVR-created subject can be claimed without rewriting evidence; subject real-name is independent of login/claim; claim does not create login identity or rewrite evidence; risk preempts replay; sensitive and deletion classes are explicit; every table maps to one wave and slice.
- Goal alignment: account/login identity, organization membership, employee identity, business subject/number binding, idempotency, Outbox, append-only audit, sensitive handling and deletion policy.
- Completion assertion: every table in V001-V003 exists with one owner, one wave and one slice; every S1 invariant portion has DB, TX or SERVICE enforcement; no claim path mutates another module's table; the four platform/audit functions (`platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`, `audit.append_fact`) pass privilege, atomicity, replay, redaction and `search_path` tests; `entitlement_metering.issue_first_trial` is neither created nor required by S1.

### Slice S2

Commerce, IVR, entitlement and activation transaction core.

- Waves: V004-V007.
- Tables: 23 (`catalog_order.*`, `ivr.*`, `entitlement_metering.*`, `activation_consent.*`).
- Table membership (exact):
<!-- slice-S2-tables:start -->
- `catalog_order.catalog_product`
- `catalog_order.catalog_version`
- `catalog_order.catalog_offer`
- `catalog_order.catalog_benefit_line`
- `catalog_order.customer_order`
- `catalog_order.payment_order`
- `catalog_order.payment_event`
- `ivr.outbound_call_task`
- `ivr.inbound_session`
- `ivr.call_record`
- `ivr.call_leg`
- `ivr.call_event`
- `ivr.ivr_decision`
- `entitlement_metering.trial_policy_version`
- `entitlement_metering.entitlement_grant`
- `entitlement_metering.trial_grant`
- `entitlement_metering.entitlement_balance`
- `entitlement_metering.entitlement_issuance`
- `entitlement_metering.usage_reservation`
- `entitlement_metering.usage_fact`
- `activation_consent.consent_record`
- `activation_consent.service_activation`
- `activation_consent.service_activation_transition`
<!-- slice-S2-tables:end -->
- Produces: catalog version/offer/benefit snapshot, order/payment/payment-event model, task/session/decision/call/leg/event model with task creation/replacement/cancellation and direct DTMF authorization, trial policy/grant with persisted action identity, durable issuance state, generic grant/balance/reservation/usage with reconciliation, consent and activation with the published first-trial service.
- Invariants: INV-08 through INV-14, INV-16 through INV-23, INV-32, INV-34 through INV-38, and the `issue_first_trial` portion of INV-40.
- Behavior verified: one non-terminal task per canonical caller; task create/replace/cancel idempotency and workspace rules; TTL uses database time after lock; one shared IVR-owned `ivr.select_call_action` service applies the same ordered identity, active-call (current `callId` excluded), activation/consent, caller/subject/binding/owner-workspace access including organization/membership/owner/creator account lifecycle status (task-backed only), task and effective-entitlement predicates to initial resolve and post-activation, without a customer login/membership/real-name requirement on the direct no-task path; resolve is atomic and creates the active call guard while a competing call gets `REJECT_CALL_IN_PROGRESS` with a terminal rejected parent and no claim/reservation; same `callId`/purpose with the same canonical fingerprint replays the first decision, a new confirmation event with the same fingerprint reuses the existing decision without side effects, changed input conflicts with the `NEW` row rolled back, and an event reused on another call conflicts; direct authorization takes the lock, requires `ENTER_CALLEE` with no task, rejects task-backed calls, creates one reservation and freezes a purpose-discriminated success with `action` null plus the committed authorization fields and an authorization-scoped encrypted callee (a refusal uses a permitted non-dial action with no reservation); trial is permanently unique, records its source action identity/fingerprint and returns `GRANTED`/`REPLAY`/`ALREADY_GRANTED` without a second grant; entitlement issuance is durable and observable per line and installment; reservation settles once and reconciles missing, late or duplicate end facts without releasing unknown use; activation confirmation handles replay before the `SUSPENDED` check, a fresh `SUSPENDED` subject is rejected with `REJECT_SUSPENDED` as a category-1 refusal before any accepted consent or trial issuance, an `ACTIVE` activation with missing/expired consent routes to `GUIDE_ACTIVATION`/`CONSENT_REQUIRED` on initial resolve, stale confirmation content is rejected before accepted side effects, and a `CONFIRMED` session is never moved to `REJECTED`; after activation/trial the shared service returns `DIAL_TASK_CALLEE`, `ENTER_CALLEE` (only after a positive no-task check), `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED` or `REJECT_CALL_IN_PROGRESS` as an `ACTIVATION_NEXT_ACTION` decision that never overwrites the original resolve decision; a category-2 later call-only rejection commits valid activation/trial while the session remains `CONFIRMED`, whereas a category-3 technical failure rolls back every write; claim-only risk does not become an unsupported dialing ban; operations transitions have an event identity and resume never reissues a trial; order creation is `PURCHASE`-bound with a first-order result and changed-input conflict; verified payment advances independently; first-trial issuance and Outbox are atomic through the published service.
- Goal alignment: catalog/order/payment, IVR task/session/call linkage, trial/entitlement/issuance/reservation/usage, consent/activation.
- Cross-slice handoff: S2 consumes S1 platform functions and identity/subject/activation contracts. It does not consume S3. The paid entitlement path is fully testable in S2 because `catalog_order`, `entitlement_metering` and `activation_consent` are all in V004-V007. Evidence and sharing are not required to verify issuance, reservation or activation.
- Completion assertion: both IVR entries run one shared `ivr.select_call_action` predicate implementation with separate endpoints, purposes and decisions; the IVR claim, current active-call binding, reservation and frozen typed decision cannot be separated across transactions and no task commits `CLAIMED` without its reservation; a category-3 technical failure rolls back the activation, idempotency completion and first-trial write together, while a category-2 domain no-dial result commits valid activation/trial with the one no-dial decision and no claim/reservation; a fresh `SUSPENDED` confirmation with and without an existing trial is rejected with `REJECT_SUSPENDED` and writes no accepted consent, transition, trial, grant, balance, reservation or success Outbox; an organization/creator/owner lifecycle denial on the task path returns a stable access rejection and never falls through to direct dialing, while the direct no-task path imposes no login/membership/real-name predicate; an existing decision/event replay is returned before the suspension check, a `CONFIRMED` session is never moved to `REJECTED`, and a confirmation/suspension race is serialized by the subject lifecycle lock; resume after suspension never reissues a trial; every confirmation branch, including already `ACTIVE` and a later call-only rejection, completes idempotency with a stable non-null `ACTIVATION_NEXT_ACTION` decision ID; the post-activation action is selected only after the shared active-call guard (current `callId` excluded), current caller/subject/binding/owner-workspace access including organization/membership/owner/creator account lifecycle status, `PENDING`/revision/`expires_at > clock_timestamp()` task predicate and positive effective entitlement, returning `DIAL_TASK_CALLEE` only when all pass, `GUIDE_PURCHASE` with no claim/reservation when available entitlement is zero, `ENTER_CALLEE` only after a positive no-task check when no valid task remains and balance is positive, and stable `REJECT`/`REJECT_CALL_IN_PROGRESS`/`REJECT_SUSPENDED` for the corresponding access, concurrency or lifecycle conditions; a second `ACTIVE/UNKNOWN` call is rejected with a terminal `REJECTED` parent and no claim/reservation while the unrelated competing call is untouched; a new event ID with the same semantic fingerprint reuses the existing decision without side effects, changed input conflicts with the `NEW` row rolled back, and the same event reused on another call conflicts; repeated confirmation and repeated resolve return their original decisions and a retry creates no second claim or reservation; the typed snapshot exposes the exact committed reservation ID, authorized seconds and start cutoff, keeps the full callee only in the encrypted internal payload, and never renews authorization on replay; a successful direct authorization has `action` null with null task fields and an `authorization.calleeNumber` only in the encrypted payload, while a direct refusal uses a permitted non-dial action with no reservation; the consent-gap route, `taskMatch = NONE` early-refusal semantics, current-`UNKNOWN` preservation, default 30-second non-refreshing TTL and replay-before-expiry-housekeeping rules are all asserted; `entitlement_metering.issue_first_trial` is created in V006 after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance` and passes privilege, atomicity, replay, `ALREADY_GRANTED`, redaction and `search_path` tests in S2; order creation returns the first order and conflicts on changed input; no module writes another module's issuance, order status or `ivr.ivr_decision` directly; every S2 table maps to one wave and slice; all S2 invariants name DB, TX or SERVICE enforcement.

### Slice S3

Evidence and sharing closure.

- Waves: V008-V009.
- Tables: 8 (`evidence.*`, `sharing.*`).
- Table membership (exact):
<!-- slice-S3-tables:start -->
- `evidence.media_object`
- `evidence.recording`
- `evidence.evidence_record`
- `evidence.evidence_artifact`
- `evidence.evidence_event`
- `sharing.evidence_share_grant`
- `sharing.share_admission`
- `sharing.share_access_event`
<!-- slice-S3-tables:end -->
- Produces: media object, recording, evidence, artifact and evidence event with reciprocal creation ownership through a valid composite FK cycle; share grant/admission/access events with reconstructible playback tickets; the final cross-slice consistency matrix.
- Invariants: INV-15, INV-24, INV-25, INV-26, INV-39.
- Behavior verified: task/session/call/reservation/evidence links each name an exact FK parent; media identity and server validation win over declarations; each evidence record is owned by exactly one matching creation event through a composite `(event_domain, event_id)` FK, both cycle constraints are `DEFERRABLE INITIALLY DEFERRED`, and insertion is atomic; share admissions count once; revoke/rotate/expiry blocks new or renewed access; Redis failure or loss does not change admission count and the ticket is reissued from the durable admission; global ownership/enforcement closure.
- Goal alignment: evidence/media metadata, sharing authorization and necessary cross-slice correctness.
- Cross-slice handoff: S3 consumes S1 platform controls and S2 call/reservation/evidence-parent contracts. It has no forward dependency.
- Completion assertion: every cross-cutting invariant has one owner and an enforcement mechanism; every table appears in exactly one wave and one slice; no out-of-scope table exists; the FK catalog has no parent later than its FK wave; every FK target matches the key catalog; the rendered wave and slice membership equals the table catalog exactly; the sensitive and lifecycle matrices cover every applicable table.

Cross-slice dependency summary:

| Slice | Depends on | Frozen handoff | Later-slice prerequisites |
| --- | --- | --- | --- |
| S1 | none | Platform functions/policy, identity, subject and binding contracts | none |
| S2 | S1 only | Catalog/order/payment, IVR call guard, issuance/grant/balance/reservation, consent/activation and first-trial service contracts | none |
| S3 | S1, S2 only | Evidence creation ownership, share admission and ticket contracts; final structural closure | none |

The dependency graph is acyclic: S1 → S2 → S3. No slice references a table created by a later slice. S1 is the platform/identity/subject foundation; S2 is the atomic commerce/IVR/entitlement/activation transaction core because those contracts are mutually dependent; S3 is evidence and sharing closure. Splitting S2 further would mechanically divide transaction boundaries and make the atomic first-trial and call/reservation behavior untestable. No fourth slice is authorized.

## 14. Validation Plan

### 14.1 Plan and fix-artifact validation (this gate)

Run from the repository root. The required toolchain is POSIX `grep`, `awk`, `sed`, `sort`, `comm`, `uniq`, `cut`, `diff`, `wc` and `bash`; the checks do not depend on `rg`. The structural script is the authoritative check for PR-012/F-07. It is extracted verbatim from this section and reused unchanged for the final specification by setting `FILE` to the final-spec path.

Gate preconditions:

```bash
FILE=docs/gateflow/a0-production-data-model-schema-spec-plan.md
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md
test -s "$FILE"
test -s "$FIX2"
for section in "Goal" "Motivation and Success Signal" "Non-Goals and Scope Boundary" "Design-Document Alignment" "First-Principles Judgment and Direct Repository Evidence" "Final Specification Target and Required Structure" "Implementation Decisions and Goal Alignment" "Exact Table Catalog and Ownership" "Invariant Catalog" "State Machines and Transaction Controls" "Contract, Schema, State-Machine and Public-Interface Impact" "Migration Order" "Behaviorally Bounded Implementation Slices" "Validation Plan" "Docs Decision and Affected Files" "Risks, Open Questions and Residual-Risk Classification" "Why This Plan Avoids Over-Design and Goal Drift" "Completion Report Format"; do
  grep -q -E "^## [0-9]+\. $section$" "$FILE" || { echo "MISSING SECTION: $section"; exit 1; }
done
test "$(grep -c -E '^### Slice S[1-3]$' "$FILE")" -eq 3
test "$(grep -c -E '^\| V00[1-9] \|' "$FILE")" -eq 9
s1=$(sed -n '/^- Completion assertion: every table in V001-V003/p' "$FILE")
printf '%s\n' "$s1" | grep -q 'four platform/audit functions'
printf '%s\n' "$s1" | grep -q 'issue_first_trial.*pass' && exit 1
# PASS: both artifacts exist; 18 required sections; 3 slice headings; 9 wave rows;
# S1 completion names only the four platform/audit functions and does not require issue_first_trial
```

Extract and run the structural script:

```bash
FILE=docs/gateflow/a0-production-data-model-schema-spec-plan.md
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md
export FILE FIX2
awk '/^# structural-script-v2$/{f=1} f{print} /^# end structural-script-v2$/{exit}' "$FILE" > /tmp/a0-structural-v2.sh
bash /tmp/a0-structural-v2.sh
# PASS: exit 0 and the summary line
```

The embedded script's fallback default still names the preserved fix-3 artifact; this gate always exports `FILE` and `FIX2` explicitly so the current plan and fix-6 artifact are validated. The structural block itself is unchanged from fix round 3 and no check was deleted or weakened. Section 11's per-record split, dependency and function-wave logic is enclosed in one `{ ... }` action block, so the program parses as a valid `awk` program and the existing `END` checks still run.

Structural script v2 (reused unchanged by section 14.2):

```bash
# structural-script-v2
FILE="${FILE:-docs/gateflow/a0-production-data-model-schema-spec-plan.md}"
FIX2="${FIX2:-docs/gateflow/a0-production-data-model-schema-spec-plan-fix-3.md}"
fail=0
say() { printf '%s\n' "$*"; }
bad() { say "$*"; fail=1; }

test -s "$FILE" || bad "MISSING/EMPTY FILE: $FILE"
test -s "$FIX2" || bad "MISSING/EMPTY FIX ARTIFACT: $FIX2"

# Extract every machine-readable block by its anchored marker only.
awk '/^<!-- catalog:start -->$/{f=1;next} /^<!-- catalog:end -->$/{f=0} f && /^\| `[a-z_]+\.[a-z_]+` \|/ {line=$0; sub(/^\| `/, "", line); sub(/` \|.*/, "", line); print line}' "$FILE" | sort > /tmp/a0-catalog
awk '/^<!-- baseline-matrix:start -->$/{f=1;next} /^<!-- baseline-matrix:end -->$/{f=0} f && NF' "$FILE" > /tmp/a0-matrix
awk '/^<!-- sensitive:start -->$/{f=1;next} /^<!-- sensitive:end -->$/{f=0} f && NF' "$FILE" > /tmp/a0-sensitive
awk '/^<!-- fk-matrix:start -->$/{f=1;next} /^<!-- fk-matrix:end -->$/{f=0} f && NF' "$FILE" > /tmp/a0-fk
awk '/^<!-- key-catalog:start -->$/{f=1;next} /^<!-- key-catalog:end -->$/{f=0} f && NF' "$FILE" > /tmp/a0-keys
cut -d'|' -f1 /tmp/a0-matrix | sort > /tmp/a0-matrix-tables

# 1. Catalog, matrix, duplicates and exact set equality.
test "$(wc -l < /tmp/a0-catalog)" -eq 44 || bad "CATALOG COUNT != 44"
test "$(wc -l < /tmp/a0-matrix-tables)" -eq 44 || bad "MATRIX COUNT != 44"
test -z "$(uniq -d /tmp/a0-catalog)" || bad "DUPLICATE CATALOG TABLES"
test -z "$(uniq -d /tmp/a0-matrix-tables)" || bad "DUPLICATE MATRIX TABLES"
if ! diff -q /tmp/a0-catalog /tmp/a0-matrix-tables >/dev/null; then
  say "TABLE SET DIFF (left catalog, right matrix):"
  comm -3 /tmp/a0-catalog /tmp/a0-matrix-tables
  fail=1
fi

# 2. Rendered §12 wave membership equals the catalog and the matrix wave column.
awk -F'|' '/^\| V00[1-9] \|/ {
  w=$2; gsub(/[ \t]/,"",w);
  t=$4;
  while (match(t, /`[a-z_]+\.[a-z_]+`/)) {
    x=substr(t,RSTART+1,RLENGTH-2);
    print x"|"w;
    t=substr(t,RSTART+RLENGTH);
  }
}' "$FILE" | sort > /tmp/a0-wave-pairs
cut -d'|' -f1 /tmp/a0-wave-pairs | sort -u > /tmp/a0-wave-tables
test "$(wc -l < /tmp/a0-wave-pairs)" -eq 44 || bad "WAVE TABLE MEMBERSHIP COUNT != 44"
test -z "$(cut -d'|' -f1 /tmp/a0-wave-pairs | sort | uniq -d)" || bad "DUPLICATE TABLE IN WAVE MEMBERSHIP"
if ! diff -q /tmp/a0-catalog /tmp/a0-wave-tables >/dev/null; then
  say "TABLE SET DIFF (left catalog, right §12 wave membership):"
  comm -3 /tmp/a0-catalog /tmp/a0-wave-tables
  fail=1
fi
for pair in V001:3 V002:6 V003:4 V004:7 V005:6 V006:7 V007:3 V008:5 V009:3; do
  w=${pair%%:*}; want=${pair##*:}; got=$(awk -F'|' -v w="$w" '$2==w' /tmp/a0-wave-pairs | wc -l)
  test "$got" -eq "$want" || bad "WAVE $w COUNT $got != $want"
done
cut -d'|' -f1,3 /tmp/a0-matrix | sort > /tmp/a0-matrix-wave
if ! diff -q /tmp/a0-matrix-wave /tmp/a0-wave-pairs >/dev/null; then
  say "MATRIX vs §12 WAVE MEMBERSHIP DIFF:"
  comm -3 /tmp/a0-matrix-wave /tmp/a0-wave-pairs
  fail=1
fi

# 3. Rendered §13 slice membership equals the catalog and the matrix slice column.
awk '
/^<!-- slice-S1-tables:start -->$/ {s="S1"; f=1; next}
/^<!-- slice-S2-tables:start -->$/ {s="S2"; f=1; next}
/^<!-- slice-S3-tables:start -->$/ {s="S3"; f=1; next}
/^<!-- slice-S[1-3]-tables:end -->$/ {f=0; next}
f && /^- `[a-z_]+\.[a-z_]+`$/ {line=$0; sub(/^- `/,"",line); sub(/`$/,"",line); print line"|"s}
' "$FILE" | sort > /tmp/a0-slice-pairs
cut -d'|' -f1 /tmp/a0-slice-pairs | sort -u > /tmp/a0-slice-tables
test "$(wc -l < /tmp/a0-slice-pairs)" -eq 44 || bad "SLICE TABLE MEMBERSHIP COUNT != 44"
test -z "$(cut -d'|' -f1 /tmp/a0-slice-pairs | sort | uniq -d)" || bad "DUPLICATE TABLE IN SLICE MEMBERSHIP"
if ! diff -q /tmp/a0-catalog /tmp/a0-slice-tables >/dev/null; then
  say "TABLE SET DIFF (left catalog, right §13 slice membership):"
  comm -3 /tmp/a0-catalog /tmp/a0-slice-tables
  fail=1
fi
for pair in S1:13 S2:23 S3:8; do
  s=${pair%%:*}; want=${pair##*:}; got=$(awk -F'|' -v s="$s" '$2==s' /tmp/a0-slice-pairs | wc -l)
  test "$got" -eq "$want" || bad "SLICE $s COUNT $got != $want"
done
cut -d'|' -f1,4 /tmp/a0-matrix | sort > /tmp/a0-matrix-slice
if ! diff -q /tmp/a0-matrix-slice /tmp/a0-slice-pairs >/dev/null; then
  say "MATRIX vs §13 SLICE MEMBERSHIP DIFF:"
  comm -3 /tmp/a0-matrix-slice /tmp/a0-slice-pairs
  fail=1
fi

# 4. One wave, slice, lifecycle and sensitive flag per table; slice binds its waves.
awk -F'|' '
  NF!=6 {print "BAD MATRIX FIELD COUNT: " $0; bad=1; next}
  $3 !~ /^V00[1-9]$/ {print "BAD WAVE: " $0; bad=1}
  $4 !~ /^S[1-3]$/ {print "BAD SLICE: " $0; bad=1}
  $5 !~ /^(MUTABLE_HEAD|APPEND_ONLY)$/ {print "BAD LIFECYCLE: " $0; bad=1}
  $6 !~ /^[YN]$/ {print "BAD SENSITIVE FLAG: " $0; bad=1}
  $4=="S1" && $3 !~ /^V00[1-3]$/ {print "S1 HAS LATER WAVE: " $0; bad=1}
  $4=="S2" && $3 !~ /^V00[4-7]$/ {print "S2 HAS OUT-OF-SLICE WAVE: " $0; bad=1}
  $4=="S3" && $3 !~ /^V00[8-9]$/ {print "S3 HAS OUT-OF-SLICE WAVE: " $0; bad=1}
  END {exit bad}
' /tmp/a0-matrix || fail=1
test "$(awk -F'|' '$5=="MUTABLE_HEAD"' /tmp/a0-matrix | wc -l)" -eq 28 || bad "MUTABLE HEAD COUNT != 28"
test "$(awk -F'|' '$5=="APPEND_ONLY"' /tmp/a0-matrix | wc -l)" -eq 16 || bad "APPEND-ONLY COUNT != 16"

# 5. Sensitive coverage.
test "$(awk -F'|' '$6=="Y"' /tmp/a0-matrix | wc -l)" -eq 28 || bad "SENSITIVE Y COUNT != 28"
for t in $(awk -F'|' '$6=="Y"{print $1}' /tmp/a0-matrix); do
  grep -Fq "$t" /tmp/a0-sensitive || bad "SENSITIVE TABLE NOT INVENTORIED: $t"
done

# 6. Invariant count, uniqueness, single owner and enforcement class.
awk -F'|' '/^\| INV-[0-9][0-9] \|/ {id=$2; gsub(/[ \t]/,"",id); print id}' "$FILE" | sort > /tmp/a0-inv
test "$(wc -l < /tmp/a0-inv)" -eq 40 || bad "INVARIANT COUNT != 40"
test -z "$(uniq -d /tmp/a0-inv)" || bad "DUPLICATE INVARIANTS"
awk -F'|' '/^\| INV-[0-9][0-9] \|/ {
  o=$3; gsub(/^[ \t]+|[ \t]+$/,"",o);
  if (o=="" || o ~ /,/) {print "BAD INV OWNER: " $0; bad=1}
  if ($0 !~ /(DB|TX|SERVICE)/) {print "NO ENFORCEMENT CLASS: " $0; bad=1}
} END {exit bad}' "$FILE" || fail=1

# 7. Candidate-key catalog shape and catalog membership.
awk -F'|' '
  FILENAME ~ /a0-catalog$/ {cat[$1]=1; next}
  $3!="PK" && $3!="UNIQUE" {print "BAD KEY TYPE: " $0; bad=1}
  !($1 in cat) {print "KEY TABLE NOT IN CATALOG: " $1; bad=1}
  END {exit bad}
' /tmp/a0-catalog /tmp/a0-keys || fail=1

# 8. FK matrix: modes, parent order, catalog membership, composite cardinality,
#    referenced-key existence and declared mode shape.
awk -F'|' '
function trim(x){gsub(/^[ \t]+|[ \t]+$/,"",x); return x}
function parts(x, arr,   n,i,k,c,dot,dots,t,col,tab,cols) {
  n=split(x, arr, ","); tab=""; cols="";
  for(i=1;i<=n;i++){
    c=trim(arr[i]);
    if (c=="-") continue;
    dot=0;
    for(k=1;k<=length(c);k++) if (substr(c,k,1)==".") dot=k;
    if (dot==0) { print "BAD COLUMN REFERENCE: " c; bad=1; return "" }
    col=substr(c,dot+1); t=substr(c,1,dot-1);
    dots=0;
    for(k=1;k<=length(t);k++) if (substr(t,k,1)==".") dots++;
    if (dots!=1) { print "BAD TABLE REFERENCE: " c; bad=1; return "" }
    if (tab=="") tab=t; else if (tab!=t) { print "MIXED TABLE IN COMPOSITE KEY: " x; bad=1 }
    cols=(cols=="" ? col : cols","col);
  }
  return tab "|" cols;
}
FILENAME ~ /a0-keys$/ { key[$1"|"$2]=$3; next }
FILENAME ~ /a0-catalog$/ { cat[$1]=1; next }
NF!=6 { print "BAD FK FIELD COUNT: " $0; bad=1; next }
{
  c=trim($1); p=trim($2); pw=trim($3); fw=trim($4); mode=trim($5); note=tolower(trim($6));
  cp=parts(c);
  if (cp=="") { bad=1; next }
  cbar=index(cp,"|"); ct=substr(cp,1,cbar-1); ccols=substr(cp,cbar+1);
  if (!(ct in cat)) { print "FK CHILD NOT IN CATALOG: " ct; bad=1 }
  if (mode !~ /^(FK|ALTER_FK|DEFERRABLE_FK|NO_FK)$/) { print "BAD FK MODE: " $0; bad=1; next }
  total++;
  if (mode=="NO_FK") {
    nofk++;
    if (pw!="-" || fw!="-") { print "NO_FK MUST HAVE - WAVES: " $0; bad=1 }
    next;
  }
  pp=parts(p);
  if (pp=="") { bad=1; next }
  pbar=index(pp,"|"); pt=substr(pp,1,pbar-1); pcols=substr(pp,pbar+1);
  if (!(pt in cat)) { print "FK PARENT NOT IN CATALOG: " pt; bad=1 }
  nc=split(ccols, cx, ","); np=split(pcols, px, ",");
  if (nc != np) { print "COMPOSITE CARDINALITY MISMATCH: " $0; bad=1 }
  if (!((pt"|"pcols) in key)) { print "FK TARGET NOT A DECLARED PK/UNIQUE KEY: " pt"("pcols")"; bad=1 }
  if (pw !~ /^V00[1-9]$/ || fw !~ /^V00[1-9]$/) { print "BAD FK WAVE: " $0; bad=1; next }
  pwn=pw; gsub(/[^0-9]/,"",pwn); fwn=fw; gsub(/[^0-9]/,"",fwn);
  if (pwn+0 > fwn+0) { print "PARENT AFTER FK: " $0; bad=1 }
  if (mode=="FK") {
    plain++;
    if (note ~ /deferrable/ || note ~ /alter/) { print "ORDINARY FK MISLABELED: " $0; bad=1 }
  } else if (mode=="ALTER_FK") {
    alt++;
    if (note !~ /alter/) { print "ALTER_FK NOTE MUST SAY ALTER: " $0; bad=1 }
    alter_row[ct"|"ccols]=1;
  } else if (mode=="DEFERRABLE_FK") {
    def++;
    if (note !~ /deferrable initially deferred/) { print "DEFERRABLE_FK NOTE MISSING DEFERRABLE INITIALLY DEFERRED: " $0; bad=1 }
    if (pwn+0 != fwn+0) { print "DEFERRABLE_FK MUST BE SAME WAVE: " $0; bad=1 }
    def_row[ct"|"ccols]=1;
  }
}
END {
  if (total != 81) { print "FK DECISION COUNT " total " != 81"; bad=1 }
  if (nofk != 2) { print "NO_FK COUNT " nofk " != 2"; bad=1 }
  if (def != 2) { print "DEFERRABLE_FK COUNT " def " != 2"; bad=1 }
  if (alt != 3) { print "ALTER_FK COUNT " alt " != 3"; bad=1 }
  expected_def["evidence.evidence_record|source_event_domain,source_event_id"]=1
  expected_def["evidence.evidence_event|evidence_id"]=1
  expected_alt["ivr.outbound_call_task|claimed_call_id"]=1
  expected_alt["catalog_order.customer_order|purchase_consent_id"]=1
  expected_alt["ivr.ivr_decision|reservation_id"]=1
  for (k in def_row) if (!(k in expected_def)) { print "UNDECLARED DEFERRABLE_FK: " k; bad=1 }
  for (k in expected_def) if (!(k in def_row)) { print "MISSING DEFERRABLE_FK: " k; bad=1 }
  for (k in alter_row) if (!(k in expected_alt)) { print "UNDECLARED ALTER_FK: " k; bad=1 }
  for (k in expected_alt) if (!(k in alter_row)) { print "MISSING ALTER_FK: " k; bad=1 }
  if (plain != total - def - alt - nofk) { print "PLAIN FK COUNT INCONSISTENT"; bad=1 }
  printf "fk_modes=FK:%d,ALTER_FK:%d,DEFERRABLE_FK:%d,NO_FK:%d total=%d\n", plain, alt, def, nofk, total
  exit bad
}
' /tmp/a0-keys /tmp/a0-catalog /tmp/a0-fk || fail=1

# 9. Required call/session/task/reservation/evidence links and the composite evidence cycle.
for link in ivr.inbound_session.call_id ivr.call_record.call_id ivr.call_record.session_id ivr.call_record.task_id ivr.outbound_call_task.claimed_call_id ivr.call_leg.call_id ivr.call_event.call_id ivr.ivr_decision.call_id ivr.ivr_decision.session_id ivr.ivr_decision.reservation_id entitlement_metering.usage_reservation.call_id entitlement_metering.usage_fact.call_id evidence.recording.call_id evidence.evidence_record.call_id evidence.evidence_record.session_id evidence.evidence_record.reservation_id evidence.evidence_record.source_event_domain evidence.evidence_event.evidence_id activation_consent.consent_record.ivr_session_id; do
  grep -q -E "^${link}[,|]" /tmp/a0-fk || bad "MISSING REQUIRED LINK DECISION: $link"
done

# 10. Contract terms and removed tokens.
grep -q -E 'platform\.idempotency_complet[e]' "$FILE" || bad "MISSING idempotency_complete CONTRACT"
grep -q -E 'PURCHAS[E]' "$FILE" || bad "MISSING PURCHASE SCOPE"
grep -q -E 'ALREADY_GRANTE[D]' "$FILE" || bad "MISSING ALREADY_GRANTED OUTCOME"
grep -q -E 'source_action_domai[n]' "$FILE" || bad "MISSING source_action_domain"
grep -q -E 'source_action_event_i[d]' "$FILE" || bad "MISSING source_action_event_id"
grep -q -E 'request_fingerprin[t]' "$FILE" || bad "MISSING request_fingerprint"
grep -q 'REJECT_SUSPENDED' "$FILE" || bad "MISSING REJECT_SUSPENDED"
grep -q 'ACTIVATION_NEXT_ACTION' "$FILE" || bad "MISSING ACTIVATION_NEXT_ACTION"
grep -q 'subject lifecycle' "$FILE" || bad "MISSING SUBJECT LIFECYCLE SERIALIZATION"
grep -q 'non-suspended' "$FILE" || bad "MISSING NON-SUSPENDED GATE"
grep -q -E 'audit\.append_fac[t]' "$FILE" || bad "MISSING audit.append_fact"
grep -q -E 'operations_?audit' "$FILE" && bad "UNDECLARED AUDIT SCHEMA TOKEN REMAINS"
grep -q -E 'DEFERRED[_]FK' "$FILE" && bad "STALE LEGACY FK MODE REMAINS"
grep -q -E 'evidence\.evidence_record\.source_event_id[|]evidence\.evidence_event\.event_id' "$FILE" && bad "STALE SINGLE-COLUMN EVIDENCE FK REMAINS"
grep -q -E '4[3] tables|3[5] invariants' "$FILE" && bad "STALE BASELINE COUNT REMAINS"

# 11. Function creation waves and table dependencies from the function-wave catalog.
awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && NF' "$FILE" > /tmp/a0-functions
test "$(wc -l < /tmp/a0-functions)" -eq 5 || bad "FUNCTION WAVE COUNT != 5"
awk -F'|' '
  FILENAME ~ /a0-matrix$/ { mw[$1]=$3; next }
  FILENAME ~ /a0-catalog$/ { cat[$1]=1; next }
  NF!=3 { print "BAD FUNCTION WAVE FIELD COUNT: " $0; bad=1; next }
  $1 !~ /^(platform\.idempotency_begin|platform\.idempotency_complete|platform\.outbox_append|audit\.append_fact|entitlement_metering\.issue_first_trial)$/ { print "UNDECLARED FUNCTION: " $1; bad=1 }
  $2 !~ /^V00[1-9]$/ { print "BAD FUNCTION WAVE: " $0; bad=1 }
  {
    n=split($3, deps, ",");
    for(i=1;i<=n;i++){
      if (!(deps[i] in cat)) { print "FUNCTION DEPENDENCY NOT IN CATALOG: " deps[i]; bad=1 }
      fw=$2; gsub(/[^0-9]/,"",fw); dw=mw[deps[i]]; gsub(/[^0-9]/,"",dw);
      if (dw+0 > fw+0) { print "FUNCTION CREATED BEFORE ITS TABLE: " $1 " -> " deps[i]; bad=1 }
    }
    if ($2=="V001") v1f[$1]=1;
    if ($2=="V006") v6f[$1]=1;
  }
  END {
    split("platform.idempotency_begin platform.idempotency_complete platform.outbox_append audit.append_fact", a, " ");
    for (i in a) if (!(a[i] in v1f)) { print "V001 MISSING FUNCTION: " a[i]; bad=1 }
    if (!("entitlement_metering.issue_first_trial" in v6f)) { print "V006 MISSING issue_first_trial"; bad=1 }
    exit bad
  }
' /tmp/a0-matrix /tmp/a0-catalog /tmp/a0-functions || fail=1

# 12. No later-slice prerequisite and no trailing whitespace.
if grep -q -E 'S2 dep[ei]nds on S3|S2 requ[i]res S3|S1 dep[ei]nds on S[23]|S1 requ[i]res S[23]' "$FILE"; then
  bad "LATER-SLICE PREREQUISITE PRESENT"
fi
if grep -q -E '[[:blank:]]+$' "$FILE"; then
  grep -n -E '[[:blank:]]+$' "$FILE"
  bad "TRAILING WHITESPACE"
fi

say "catalog=$(wc -l < /tmp/a0-catalog) matrix=$(wc -l < /tmp/a0-matrix) waves=$(grep -c -E '^\| V00[1-9] \|' "$FILE") wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=$(wc -l < /tmp/a0-inv) lifecycle=28/16 sensitive=28 fk_rows=$(wc -l < /tmp/a0-fk) key_sets=$(wc -l < /tmp/a0-keys)"
exit $fail
# end structural-script-v2
```

### 14.2 Final-spec validation (future implementation gate)

The final specification must preserve the machine-readable blocks and row formats used by the structural script: the `catalog`, `sensitive`, `baseline-matrix`, `fk-matrix`, `key-catalog` and `function-wave` blocks, the nine `| V00N |` wave rows with backticked table names in the "Tables created" column, and the exact `slice-S1-tables`/`slice-S2-tables`/`slice-S3-tables` membership blocks. It must use the same FK mode names and the same key-catalog columns.

Run the script extracted unchanged from section 14.1:

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md bash /tmp/a0-structural-v2.sh
# PASS: exit 0 and the same summary line (44 catalog, 40 invariants, 9 waves, 13/23/8 slices, 28/16 lifecycle, 81 FK decisions)
```

Do not replace it with a global table-count regex. The script scopes the table count to the catalog block, compares catalog names exactly with rendered wave and slice membership, validates every FK target against the key catalog, and distinguishes ordinary `FK`, late-added `ALTER_FK` and truly `DEFERRABLE_FK` modes.

Additional final-spec checks:

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
test -s "$FILE"
grep -q 'Sensitive-field inventory' "$FILE"
grep -q 'Revision, immutability and deletion classification' "$FILE"
grep -q 'Cross-schema function contracts' "$FILE"
grep -q 'issueFirstTrial' "$FILE"
grep -q 'ENTER_CALLEE' "$FILE"
grep -q 'RECONCILING' "$FILE"
grep -q 'REJECT_SUSPENDED' "$FILE"
grep -q 'ACTIVATION_NEXT_ACTION' "$FILE"
grep -q 'record_activation_next_action' "$FILE"
grep -q 'select_call_action' "$FILE"
grep -q 'REJECT_CALL_IN_PROGRESS' "$FILE"
grep -q 'NO_AVAILABLE_ENTITLEMENT' "$FILE"
grep -q 'CONSENT_REQUIRED' "$FILE"
grep -q 'CALL_STATE_UNRESOLVED' "$FILE"
grep -q 'DIRECT_AUTHORIZED' "$FILE"
grep -q 'authorization.calleeNumber' "$FILE"
grep -q 'authorizedRecordingSeconds' "$FILE"
grep -q 'authorizationExpiresAt' "$FILE"
grep -q 'subject lifecycle' "$FILE"
grep -q 'non-suspended' "$FILE"
grep -q 'savepoint' "$FILE"
# PASS: required contracts and the F-08 through F-13 suspension, consent-gap, shared-selection, active-call, UNKNOWN, outcome and typed-authorization terms are present
! grep -q -E '^\| `[^`]+\.(ai_|notary_|refund|invoice|work_order|provider_config)' "$FILE"
# PASS: no out-of-scope table row
test -z "$(git status --porcelain=v1 | grep -E '\.sql$|db/migration/')"
# PASS: no executable migration was added, modified or left untracked
```

Structural assertions for the final specification:

1. Every one of the 44 table rows has one owning module, one migration wave, one slice, one lifecycle class and one sensitive representation where applicable.
2. Every one of the 40 invariants has exactly one owner, named collaborators, a DB/TX/SERVICE enforcement class, an exact enforcement location, a verified slice and a goal clause.
3. Every mutable table identifies its revision/conditional-update rule; every append-only table identifies its event identity and no-update/no-delete control; `platform.idempotency_record` is mutable-head and finalizes its result once.
4. Every sensitive column identifies ciphertext, HMAC, masked, redacted or encrypted representation and a prohibited exposure.
5. Every FK identifies its parent, action, creation wave, mode (`FK`, `ALTER_FK`, `DEFERRABLE_FK` or `NO_FK`) and whether it crosses a module or slice boundary; deferred and no-FK decisions are explicit.
6. Every state machine has allowed states, terminal states, transition guard and retry/reconciliation behavior.
7. Every critical activation, task lifecycle, IVR resolve, direct authorization, usage settlement, reservation reconciliation, order creation, payment-to-issuance, evidence and sharing race has an explicit transaction sequence.
8. The migration list has no forward reference and covers all 44 tables exactly once; function creation follows the section 11 creation waves.
9. Every unresolved production input is marked "not guessed" and assigned an owner/destination.
10. The final goal-alignment review maps each of the three slices and every validation to the confirmed contract.

## 15. Docs Decision and Affected Files

### Docs decision

- Add only `docs/14-production-data-model-and-flyway-schema-spec.md` in the future implementation gate.
- Do not modify `docs/09` through `docs/13`, `docs/README.md`, the controller, existing reviews or source/prototype files.
- This fix-round-6 gate writes only the revised plan and `a0-production-data-model-schema-spec-plan-fix-6.md`. The baseline is 44 tables, 40 invariants, 9 migration waves, 3 dependency-ordered slices (13/23/8), 28 mutable-head and 16 append-only tables, and 81 FK decisions (74 `FK`, 3 `ALTER_FK`, 2 `DEFERRABLE_FK`, 2 `NO_FK`); the final specification must preserve those counts or explicitly justify a controller-approved change.
- The controller remains the authority for gate state and acceptance. A future documentation-index update, if required by the controller, is a separate controlled change.
- No retention period, capacity, credential or provider configuration is recorded as approved in the schema specification.

### Affected files

| Path | Change |
| --- | --- |
| `docs/gateflow/a0-production-data-model-schema-spec-plan.md` | Revised in place by task `a0-ivr-contract-fix6-ds-20261022-01`; F-14 and the OQ-1/OQ-2 clarifications addressed |
| `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md` | New round-6 fix artifact with F-14 status, direct evidence, changed locations and exact validation outcomes |
| `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-5.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-4.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-3.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-2.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix.md` | Preserved historical fix artifacts; not modified by this task |
| `docs/reviews/plan-review-20261022-113039.md`, `docs/reviews/plan-review-20261022-124546.md`, `docs/reviews/plan-review-20261022-131145.md`, `docs/reviews/plan-review-20261022-133903.md`, `docs/reviews/plan-review-20261022-140542.md`, `docs/reviews/plan-review-20261022-171403.md`, `docs/reviews/plan-review-20261022-173816.md` | Preserved review artifacts; not modified by this task |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | To be added only by the future implementation gate |
| `docs/gateflow/project-execution-control.md` | Not modified by this task |
| `docs/12-technology-stack-and-system-architecture.md` and `docs/09`-`docs/13` | Not modified by this task |
| Source, prototype, QA screenshot and review paths | Not modified by this task |
| `.codex`, Git metadata and external systems | Not modified by this task |

## 16. Risks, Open Questions and Residual-Risk Classification

### Open questions and unresolved production inputs

| ID | Unresolved input | Why it is not guessed | Owner | Destination |
| --- | --- | --- | --- | --- |
| Q1 | Employee authentication source, MFA policy and credential rotation | Security architecture is a later design gate; business tables only store issuer/subject references | Security/Backend | Security design and operations IAM implementation |
| Q2 | Envelope-encryption/KMS products, key hierarchy and rotation | Provider selection is deferred; schema stores ciphertext, key reference and HMAC index only | Security/Infrastructure | Security design and COS/KMS validation |
| Q3 | Legal retention, erasure rights, backup deletion and audit retention | Legal approval is not available | Legal/Security | Retention and deletion policy; production launch review |
| Q4 | Production prices, entitlement quantities, seat counts, trial wording and catalog approval | Demo values are explicitly not approved | Product/Operations/Finance | Catalog approval and Phase C configuration |
| Q5 | Renewal, stacking, offset order, auto-renew, refund and tax/invoice behavior | Current baseline keeps these offline or future | Product/Finance | Phase C business rules; offline support process |
| Q6 | WeChat payment merchant/app identifiers, payload fields and callback retention | Vendor integration is not yet contracted into the schema | Payments/Backend | Payment adapter and OpenAPI/security design |
| Q7 | SIP line/access number, callee display, recording format/size, buffer and time synchronization | Real IVR platform and line integration are pending | Telephony/IVR | IVR protocol and line integration |
| Q8 | Authorized-versus-reported duration tolerance and IVR SLO/timeouts | Requires real-line measurement and acceptance policy | IVR/Backend/QA | IVR protocol and capacity/acceptance work |
| Q9 | Number recycling and conflict-resolution rules | Current design only detects risk; automatic transfer is forbidden | Product/Risk/Operations | Claim-risk policy and operations runbook |
| Q10 | Recording notice/consent wording and prompt-version lifecycle | Legal/product approval is pending | Product/Legal | Consent policy and IVR prompt approval |
| Q11 | COS object-lock/WORM capability, storage class and media retention | Cloud capability and legal retention are unapproved | Infrastructure/Security | Storage/security/capacity work |
| Q12 | Share-code KDF parameters, admission limits, expiry defaults and rate-limit thresholds | No approved production policy; schema stores the facts but no defaults | Security/Product | Security design and sharing policy |
| Q13 | Audit/Outbox retention, DLQ thresholds and replay operations | Operational policy is deferred | Operations/Backend | Operations runbook and observability design |
| Q14 | Subject real-name verification provider, evidence and re-verification policy | Real-name source is not contracted; schema only stores subject-level status and protected references | Security/Product | Verification adapter and identity design |
| Q15 | Exact event-domain wire values and formats for `ACTIVATION`, `IVR_CONFIRM`, `IVR_EVENT`, `IVR_DIRECT_AUTH`, `EVIDENCE`, `PAYMENT`, `SHARE_ACCESS` and other domains | Domain names are fixed by business rules, but wire format and validation rules are API/security decisions | Backend/Security | IVR protocol and OpenAPI/security design |
| Q16 | Task idempotency-key retention, replacement audit detail and abuse limits | No approved production policy; baseline stores the fact and unique key but no retention default | Product/Backend | IVR protocol and operations policy |
| Q17 | Flyway role provisioning, schema ownership transfer and least-privilege grant execution | Deployment identity and database ownership are environment-specific | Platform/Database | Deployment/security design and production readiness |
| Q18 | Production configuration key/approval for the configurable pending-task TTL, config versioning/audit and scheduler cadence | The plan states the source default of 30 seconds as the baseline default and freezes it per task; the production configuration key, approval and scheduler SLO are pending and are not invented here | Product/Backend | IVR protocol and operations configuration |
| Q19 | WeChat appid/openid/unionid mapping and multi-app identity association | The source requires clarification, but account merge and login-identity migration are explicit non-goals | Identity/Backend | Authentication design and security review |
| Q20 | Installment due-date trigger, grace/retry policy and scheduler cadence for future `entitlement_issuance` rows | The baseline creates the durable slot and enforces uniqueness, but the source does not approve a due-date or grace rule | Product/Finance/Backend | Entitlement issuance design and Phase C configuration |
| Q21 | Reservation reconciliation scan cadence, alert thresholds and manual adjustment approval | Operational policy is deferred; the baseline defines states and never releases unknown use | Operations/Backend/IVR | Operations runbook and reconciliation design |
| Q22 | Redis playback-ticket TTL, clock-skew allowance and revocation propagation window | Security/product policy is not approved; the baseline derives the key and never makes Redis authoritative | Security/Sharing | Security design and sharing policy |
| Q23 | Function/role provisioning order and whether the deployment role can create SECURITY DEFINER functions | Environment-specific database ownership; the baseline specifies the contract but not the deployment identity | Platform/Database/Security | Deployment/security design and production readiness |
| Q24 | Any call-blocking subject-risk rule beyond the approved `OPEN` blocks on number claims and historical-evidence access | The approved policy does not ban future dialing for claim-only risk; inventing a blanket ban would contradict `docs/10` | Product/Risk/IVR | Claim-risk and IVR policy; launch-readiness review |
| Q25 | Exact `authorizationExpiresAt` window, clock-skew allowance and configurable value | The source requires a monitored clock and a window covering the call plus skew, but no production value is approved | IVR/Entitlement/Backend | IVR protocol, configuration and capacity/acceptance work |
| Q26 | Typed snapshot JSON-schema version and IVR wire mapping for the stable reason/taskMatch enums | The field contract is fixed here, but wire encoding and version negotiation are API/security decisions | IVR/Backend/Security | IVR protocol and OpenAPI/security design |
| Q27 | Exact organization role(s) that hold the task-create/claim-revalidation permission | The plan makes organization/creator/owner account and membership lifecycle status mandatory predicates; only the approved role-to-permission matrix remains a security/authorization decision | Organization-access/Security | Authorization design and S2 policy tests |
| Q28 | Durable IVR-local B-leg operation key, persistence protocol and lost-response recovery | The business `ivr.call_leg` table records callbacks/facts and does not prove local outbound uniqueness; the authoritative unique operation belongs to the existing IVR platform, whose executable key/protocol is a later IVR implementation decision | Telephony/IVR | IVR protocol and line integration; no business schema expansion |

No unresolved item may be filled with a demo value, a guessed retention period or a provider-specific identifier in the final specification. Fields may exist as configuration references or nullable production inputs only where the state machine explicitly permits them.

### Residual-risk classification

| Risk | Classification | Owner and destination |
| --- | --- | --- |
| Caller-only matching can associate a delayed old inbound with the current replacement task | launch-blocking residual risk, not solved by the schema | Product/Telephony/Backend; IVR integration and launch-readiness review |
| Number recycling may make historical evidence access unsafe | launch-blocking policy risk | Product/Risk/Legal; claim/evidence access policy |
| No executable migration or PostgreSQL concurrent test exists at this gate | residual verification risk | Backend; implementation gate and Testcontainers concurrency suite |
| Task creation/replacement, direct DTMF authorization and reconciliation rely on canonical-caller and reservation locks that have no executable concurrency test at this gate | launch-blocking concurrency verification risk | Backend/IVR; implementation Testcontainers suite and IVR integration |
| First-trial issuance, entitlement issuance, evidence creation ownership and share-ticket reconstruction require cross-module failure-injection tests that do not yet exist | residual verification risk | Backend/Entitlement/Evidence/Sharing; implementation integration suite |
| Suspended-confirmation rejection, confirmation/suspension serialization and every post-activation next action require executable transaction tests that do not yet exist | residual verification risk | Activation/IVR/Entitlement/Backend; implementation Testcontainers suite |
| Shared action-selection parity, current-call exclusion, competing-call rejection, semantic-fingerprint replay/conflict, claim/reservation rollback, task-expiry-at-update, revocation races and typed-snapshot exactness require executable concurrency and integration tests that do not yet exist | residual verification risk | IVR/Activation/Entitlement/Backend; implementation Testcontainers and integration suite |
| Organization/owner/creator account and membership lifecycle predicates, their deterministic lock order and writer-side locking require executable authorization/concurrency tests | residual verification risk | Organization-access/Security/IVR; S2 authorization and Testcontainers suite |
| Purpose-discriminated direct authorization (`action` null plus `authorization.calleeNumber`), direct-refusal nullability and callee redaction require executable typed-contract tests | residual verification risk | IVR/Backend; implementation contract and redaction tests |
| Category-1/2/3 outcome classification, the attempt savepoint and full technical rollback require forced-failure and savepoint tests | residual verification risk | Activation/IVR/Entitlement/Backend; implementation Testcontainers failure-injection suite |
| Durable IVR-local B-leg uniqueness is not implemented in this business schema | deferred implementation risk | Telephony/IVR; IVR protocol and line integration |
| The exact role matrix for task-access revalidation and any future call-blocking risk rule are not approved policy inputs | policy/integration risk | Product/Risk/Organization-access/Security; authorization and claim-risk design |
| Idempotency begin/complete concurrency, committed-`NEW` alerting and write-once result tests do not yet exist; a bug that commits `begin` without `complete` is detectable and alertable but not preventable by constraints alone | residual verification/operations risk | Platform/Backend; implementation concurrency suite and operations runbook |
| Order `PURCHASE` replay, changed-input conflict and first-order result identity require integration tests that do not yet exist | residual verification risk | Catalog-order/Backend; implementation integration suite |
| The evidence composite FK cycle and deferred constraint trigger require a real PostgreSQL migration test for both insertion orders and commit-time rejection | residual verification risk | Evidence/Database/Backend; implementation migration suite |
| Cross-schema SECURITY DEFINER functions depend on environment-specific role provisioning and audit review | deployment/security verification risk | Platform/Database/Security; security design and production readiness |
| Subject real-name verification source and re-verification policy are not contracted | launch-blocking policy/integration risk | Product/Security; verification adapter and identity design |
| No real IVR line, payment provider or COS object-lock validation exists | launch-blocking integration risk | IVR/Payments/Infrastructure; protocol and production-readiness gates |
| AI, notary, refunds, invoices and online workflows are absent | deferred follow-up, intentionally out of scope | Later phase owners |
| Capacity, SLO, RPO/RTO, secrets rotation and alert thresholds are absent | deferred follow-up | Capacity/security/operations work units |

## 17. Why This Plan Avoids Over-Design and Goal Drift

- Every one of the 44 tables and 40 invariants maps to a confirmed goal domain or a necessary correctness/safety condition. There is no AI, notary, refund, invoice, work-order, provider-configuration, capacity or speculative customer-workflow table.
- The 44th table is not scope expansion: `entitlement_metering.entitlement_issuance` owns the per-order/per-installment/per-type issuance fact required by the confirmed entitlement goal and accepted finding PR-004. It replaces a cross-owned order status with one owning module.
- The five added invariants are not new product scope: they make the accepted issuance, activation-replay, reconciliation, share-ticket and cross-schema-function conditions explicit and testable.
- The added task-idempotency, direct-authorization, subject-real-name, call-party, event-domain, FK-mode, reconciliation, idempotency-completion and function-contract decisions are source-required corrections (`docs/13` §4.1/§4.3/§6.4/§7, `docs/09` §4, `docs/10` §4, `docs/12` §10), not speculative scope.
- The idempotency completion path adds no table and no domain scope: it finalizes the existing platform record exactly once, and order/trial replay reuse the same owned facts. The evidence correction changes only the FK shape and cycle deferrability; it adds no evidence fact.
- Separate tables are used only where an independent fact, uniqueness rule, immutable history or transaction owner exists. JSON is not used as a substitute for core relations or state.
- External-call attempt/result metadata is carried by audit facts, Outbox dispatcher fields and provider-event rows; a separate provider-call log table is deferred rather than adding a speculative table. The technical `platform-reliability` owner is the minimum way to prevent per-module Outbox/idempotency duplication and cross-module writes; it owns no business truth.
- The design keeps the approved modular-monolith and PostgreSQL boundary. It does not introduce microservices, separate databases, CQRS, event sourcing or a generic rules engine.
- The shared IVR action-selection contract is an application service inside the existing IVR-owned local transaction. It adds no table, module, slice, workflow engine or SQL `SECURITY DEFINER` function; it removes duplicated predicate logic between initial resolve and post-activation. The fix-round-5 corrections stay inside the seven-action contract: `DIRECT_AUTHORIZATION` remains a purpose with a null-action success, the transaction-outcome contract uses an in-transaction savepoint rather than a new service, and the organization/account lifecycle predicates reuse existing rows and locks. The fix-round-6 correction is limited to the revocation serialization scenario and the category-1 write-set and consent-content sequencing clarifications; it adds no service, action, state or lock.
- Redis remains limited to short-lived sessions, OTP, rate limits and derived playback tickets; no permanent authorization or entitlement fact depends on it, and ticket loss is reconstructible.
- The plan does not copy prototype IDs, local sequence allocation, fixture values or in-memory event shapes.
- The three slices follow dependency order and behavioral transaction boundaries: S1 is the platform/identity/subject foundation, S2 is the atomic commerce/IVR/entitlement/activation core, and S3 is evidence/sharing closure. No slice depends on a later slice; no fourth slice is invented.
- Unresolved inputs are assigned, not guessed. No demo price, trial duration, retention period, encryption product, line capability, installment due-date rule or vendor credential becomes a schema default. The unapproved call-blocking risk rule and the task-access role matrix are named deferred inputs rather than invented predicates.
- This gate writes only the plan and the round-6 fix artifact; it does not write the final schema specification, executable DDL or any migration.

## 18. Completion Report Format

The fix completion report must use exactly these headings:

- Task label and gate
- Artifact paths
- Concise fixed-plan summary and final counts/slices
- F-09, F-12, F-13 and F-14 status summary, the related OQ-1/OQ-2, consent-gap/`taskMatch`/B-leg/`UNKNOWN`/TTL/replay-order clarifications, the preserved F-10/F-11 states, and the PR-001 through PR-014 and F-01 through F-08 status summary
- Files changed
- Exact validations and outcomes
- Residual risks or blocking questions
- Explicit statement that no re-review or later gate was entered

The report must state that only the two allowed artifacts were written, list the 44-table/40-invariant/9-wave/3-slice (13/23/8) baseline and the derived 28/16 lifecycle and 74/3/2/2 FK-mode counts, include the validation command results, give every accepted finding and every partial original finding one final status with evidence, name the residual-risk owners/destinations and uncovered areas, and stop before re-review, commit, push, PR, merge or any later gate.
