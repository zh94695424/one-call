# A2 production IVR protocol spec — S2 implementation report

- Task label: `a2-ivr-s2-impl-ds-20261025-01`
- Work unit: `A2-production-ivr-protocol-spec`
- Gate: `implementation`, slice S2 only
- Accepted S1 checkpoint: `cf97bdefe9860e1341caf72f8f70516018a248f6`
- Binding plan: `docs/gateflow/a2-production-ivr-protocol-spec-plan.md` §§4.5-4.13, 5, 6, 7, 8.3
- Event registry digest: `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`
- Stop state: S2 implementation complete; not staged, not committed, not pushed, no PR/issue, no merge, no review, S3 not started.

## 1. Exact changed files

Modified:

- `docs/16-production-ivr-protocol-spec.md`
- `protocol/ivr/v1/ivr-protocol-v1.json`
- `protocol/ivr/validate.mjs`

Added (exactly the 11 required S2 fixtures):

- `protocol/ivr/v1/fixtures/s2-01-task-terminal-completed.json`
- `protocol/ivr/v1/fixtures/s2-02-event-duplicate-replay.json`
- `protocol/ivr/v1/fixtures/s2-03-event-out-of-order.json`
- `protocol/ivr/v1/fixtures/s2-04-terminal-unknown-reconciliation.json`
- `protocol/ivr/v1/fixtures/s2-05-response-loss-retry.json`
- `protocol/ivr/v1/fixtures/s2-06-ivr-restart-bleg-recovery.json`
- `protocol/ivr/v1/fixtures/s2-07-authorization-expiry-not-started.json`
- `protocol/ivr/v1/fixtures/s2-08-redial-second-bleg-rejected.json`
- `protocol/ivr/v1/fixtures/s2-09-heartbeat-and-terminal.json`
- `protocol/ivr/v1/fixtures/s2-10-terminal-contradiction-quarantine.json`
- `protocol/ivr/v1/fixtures/s2-11-resolve-never-committed-not-started.json`

Added report:

- `docs/gateflow/a2-production-ivr-protocol-spec-s2-implementation.md`

Temporary (ignored `tmp/`, not deliverables):

- `tmp/a2_event_canonical.json` (canonical eventTypes serialization used for the digest)
- `tmp/a2_compute.py` (unused due to execution denial)
- `tmp/a2-s2-probe-manifest.md` (static mutation-probe manifest)

No other path was modified. S1 fixtures, A0/A1 sources, OpenAPI, prototypes, screenshots, shared business logic and the control document were not touched.

## 2. Full-frozen event registry

`eventTypes` is exactly the following 11 rows in fixed order. The S1 `payloadState=DEFERRED_S2` sentinel is removed. The doc marker `protocol-event-map` has the same header and rows; `{}` is the doc notation for the model's empty `payloadFields` array.

| eventType | envelopeLegId | payloadFields | normalProcessingOutcome |
| --- | --- | --- | --- |
| CALLER_LEG_ANSWERED | REQUIRED | {} | APPLIED |
| CALLEE_LEG_RINGING | REQUIRED | {} | APPLIED |
| CALLEE_LEG_ANSWERED | REQUIRED | {} | APPLIED |
| BRIDGE_STARTED | ABSENT | calleeLegId,callerLegId | APPLIED |
| RECORDING_STARTED | ABSENT | {} | APPLIED |
| RECORDING_ENDED | ABSENT | {} | APPLIED |
| CALL_TERMINATED | ABSENT | authoritativeRecordingSeconds,bridgedAt?,calleeLegAnsweredAt?,calleeLegId?,calleeLegOutcome?,calleeLegStartedAt?,callerLegAnsweredAt?,callerLegId,callerLegOutcome,endReason,recordingEndedAt?,recordingStartedAt? | APPLIED |
| CALL_TERMINATION_UNKNOWN | ABSENT | reasonCode | APPLIED |
| CALL_TERMINAL_RECONCILED | ABSENT | authoritativeRecordingSeconds,bridgedAt?,calleeLegAnsweredAt?,calleeLegId?,calleeLegOutcome?,calleeLegStartedAt?,callerLegAnsweredAt?,callerLegId,callerLegOutcome,endReason,recordingEndedAt?,recordingStartedAt?,resolvesUnknownEventId | APPLIED |
| OUTBOUND_NOT_STARTED | ABSENT | reasonCode,reservationId? | APPLIED |
| CALL_HEARTBEAT | ABSENT | {} | IGNORED |

`eventRegistry` is exactly `{frozenAt:"s2", sha256:"c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9"}`. The digest is SHA-256 over the canonical JSON of the 11 `eventTypes` entries (UTF-8, sorted object keys, no insignificant whitespace), computed in fixed row order.

## 3. Envelope and payload registries

`eventEnvelope` freezes:

- Wire fields: `eventId`, `eventType`, `legId?`, `sequenceNo?`, `occurredAt`, `payload`.
- Required: `eventId`, `eventType`, `occurredAt`, `payload`.
- `legId` is required for the three leg events and forbidden for the other eight.
- `callerLeg*` derives `CALLER`/`INBOUND`; `calleeLeg*` derives `CALLEE`/`OUTBOUND`; no wire `leg_type`/`direction`.
- `occurredAt` is IVR fact time and is never used for task TTL or authorization.
- Payloads are closed per event; unknown, missing required or extra fields are `400 IVR_ARGUMENT_INVALID` with no event write.

Closed payload definitions (required / optional):

| eventType | Required | Optional |
| --- | --- | --- |
| CALLER_LEG_ANSWERED | none | none |
| CALLEE_LEG_RINGING | none | none |
| CALLEE_LEG_ANSWERED | none | none |
| BRIDGE_STARTED | callerLegId, calleeLegId | none |
| RECORDING_STARTED | none | none |
| RECORDING_ENDED | none | none |
| CALL_TERMINATED | callerLegId, callerLegOutcome, endReason, authoritativeRecordingSeconds | callerLegAnsweredAt, calleeLegId, calleeLegStartedAt, calleeLegAnsweredAt, calleeLegOutcome, bridgedAt, recordingStartedAt, recordingEndedAt |
| CALL_TERMINATION_UNKNOWN | reasonCode (`LINE_OUTCOME_UNKNOWN`) | none |
| CALL_TERMINAL_RECONCILED | callerLegId, callerLegOutcome, endReason, authoritativeRecordingSeconds, resolvesUnknownEventId | callerLegAnsweredAt, calleeLegId, calleeLegStartedAt, calleeLegAnsweredAt, calleeLegOutcome, bridgedAt, recordingStartedAt, recordingEndedAt |
| OUTBOUND_NOT_STARTED | reasonCode (`AUTHORIZATION_EXPIRED`) | reservationId |
| CALL_HEARTBEAT | none | none |

Terminal rules: outcomes are `ENDED`/`FAILED`; `occurredAt` is the call `ended_at`; times are non-decreasing; a missing leg row requires its start/answer time in the terminal; `authoritativeRecordingSeconds` is a non-negative integer and must be 0 without a recording start; over-authorized seconds enter `RECONCILING` with no usage fact.

## 4. End reasons, task/settlement mapping and quarantine registry

Nine `endReason` values:

`CALLER_HANGUP_AFTER_BRIDGE`, `CALLEE_HANGUP_AFTER_BRIDGE`, `RECORDING_LIMIT_REACHED`, `CALLER_HANGUP_BEFORE_BRIDGE`, `CALLEE_BUSY`, `CALLEE_NO_ANSWER`, `CALLEE_REJECTED`, `B_LEG_FAILED`, `BRIDGE_FAILED`.

| endReason class | call | task |
| --- | --- | --- |
| Three post-bridge/limit values | `ENDED` | `COMPLETED` |
| Six pre-bridge failure values | `FAILED` | `FAILED` |

Special terminal mappings: `LINE_OUTCOME_UNKNOWN` → call `UNKNOWN`, task unchanged; `AUTHORIZATION_EXPIRED` → call/task `FAILED`, reservation `RELEASED`, 0 seconds, no usage. The latter applies only when the original resolve committed; an uncommitted resolve gets `404 CALL_UNKNOWN` and no placeholder.

Ten quarantine/ignore reasons (written to `call_event.reason_code`, never Problem codes):

`LEG_UNKNOWN`, `SEQUENCE_CONFLICT`, `CONTRADICTORY_FACT`, `CALL_TERMINAL`, `ALREADY_TERMINAL`, `AUTHORIZATION_NOT_EXPIRED`, `OUTBOUND_ALREADY_STARTED`, `USE_RECONCILIATION_EVENT`, `NOT_UNKNOWN`, `HEARTBEAT_IGNORED`.

Settlement: 0 seconds → `RELEASED`, no usage; `1..authorized` → `SETTLED`, one usage fact, remainder released; `> authorized` → `RECONCILING`, no settlement/release/usage and no tolerance. Settlement is at most once and is driven only by accepted terminal/reconciliation facts; heartbeat never releases.

## 5. State transitions

Backend state machines in `stateMachines` (six aggregates plus `terminalMapping`):

- `outbound_call_task`: `PENDING→CLAIMED`; `CLAIMED→COMPLETED/FAILED`; never reopened.
- `inbound_session`: `RECEIVED→CONFIRMED/REJECTED`; `ABANDONED` has no v1 wire trigger.
- `call_record`: new→`ACTIVE`/`REJECTED`; `ACTIVE→ENDED/FAILED/UNKNOWN`; `UNKNOWN→ENDED/FAILED` only by reconciliation.
- `call_leg`: create `RINGING`/`ANSWERED`; `RINGING→ANSWERED`; `ANSWERED→ENDED/FAILED`.
- `ivr_decision`: frozen once per `(callId,purpose)`.
- `usage_reservation`: `HELD→SETTLED/RELEASED/RECONCILING`; `RECONCILING→SETTLED/RELEASED`; one terminal transition.

IVR-local B-leg phases (keyed by `callId`, exactly one operation per call):

`ABSENT`, `PREPARED`, `DISPATCHING`, `DIALING`, `RINGING`, `ANSWERED`, `BRIDGED`, `RECORDING`, `DISPATCH_UNKNOWN`, `NOT_DISPATCHED`, `AUTHORIZATION_EXPIRED`, `FAILED`, `ENDED`.

Local rules:

- `lineAttemptCount` is 0 or 1, moves to 1 only on the actual line request and never to 2.
- `PREPARED` may enter `DISPATCHING` once before `authorizationExpiresAt` with `lineAttemptCount=0`.
- `DISPATCHING`/`DISPATCH_UNKNOWN` consults authoritative line/CDR evidence; it never redials.
- Authoritative `NO_ATTEMPT` returns `NOT_DISPATCHED`; if the deadline has not passed it becomes `PREPARED` for the first actual dispatch.
- A deadline without proof of non-dispatch stays unknown/occupied for reconciliation, never `OUTBOUND_NOT_STARTED`.
- A B-leg started before the deadline continues under its frozen seconds.

Event outbox states: `PENDING`, `SENT`, `ACKED`, `PERMANENT_FAILURE`. The identity is `(callId,eventId)` with an immutable payload; retransmission uses the identical identity. Call-event 2xx (including `QUARANTINED`) is ACKED; `EVENT_ID_CONFLICT`, `SESSION_IDENTITY_MISMATCH`, `CALL_UNKNOWN`, `IVR_ARGUMENT_INVALID` and `VALIDATION_FAILED` are permanent; `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL`, 5xx and network are retryable. Recording declaration/completion outbox classification is explicitly deferred to S3.

Recovery decisions implemented in the fixture evaluator:

1. Restart reads the local operation/outbox by `callId`; it never mints a new `callId` or re-resolves for new authorization.
2. `PREPARED` dispatches once after restart if the deadline has not passed.
3. `DISPATCH_UNKNOWN` + authoritative `ATTEMPTED` resumes `DIALING` with `lineAttemptCount=1` and never redials.
4. `UNKNOWN` keeps the call occupied, the task `CLAIMED` and the reservation `HELD`; only `CALL_TERMINAL_RECONCILED` converges.
5. Resolve response loss uses the same `callId` and returns the frozen decision when committed.
6. Resolve never committed: later `OUTBOUND_NOT_STARTED` is `404 CALL_UNKNOWN`, no placeholder/release/usage, outbox `PERMANENT_FAILURE`.

## 6. Fixtures and counters

Cumulative fixture set: 17 (`6+11`); marked negatives: 3 (`s2-08`, `s2-10`, `s2-11`). S2 fixtures use the deterministic `ProtocolMachine` and assert computed projection/counter subsets (S1 fixtures remain exact and unchanged). The machine never reads `expected` to decide an outcome.

| Fixture | Scenario | Key computed assertions |
| --- | --- | --- |
| `s2-01-task-terminal-completed` | Task resolve → one B-leg → leg/bridge/recording → `CALL_TERMINATED` | call `ENDED`; task `COMPLETED`; reservation `SETTLED`; `lineAttempts=1`; `eventsApplied=7`; `settlements=1`; `usageFacts=1`; `terminalFactsAccepted=1` |
| `s2-02-event-duplicate-replay` | Duplicate heartbeat and terminal with the same `eventId` | first receipts; `eventsReplayed=2`; `releases=1`; `usageFacts=0`; `outboxRetried=2`; outbox both `ACKED` |
| `s2-03-event-out-of-order` | Terminal first (sequence 10), late consistent leg/recording facts | `eventsApplied=1`; `eventsIgnored=4`; `eventsQuarantined=0`; legs/bridge/recording converge; `settlements=1`; `usageFacts=1` |
| `s2-04-terminal-unknown-reconciliation` | Media facts → `CALL_TERMINATION_UNKNOWN` → redial attempt → `CALL_TERMINATED` quarantine → reconciliation | UNKNOWN has null `endedAt`/seconds and `HELD` reservation; `redialAttempts=1`; `eventsQuarantined=1`; reconciliation `reconciliations=1`, `settlements=1`, `usageFacts=1`; `releases=0` |
| `s2-05-response-loss-retry` | Lost resolve and terminal responses, identical retries | frozen resolve replay; `eventsReplayed=1`; one B-leg/reservation; outbox `SENT→ACKED`; `releases=1`; `usageFacts=0`; `outboxRetried=1` |
| `s2-06-ivr-restart-bleg-recovery` | Restart at `PREPARED` and `DISPATCH_UNKNOWN` | `blegOperationsCreated=2`; `lineAttempts=2` (one per call); `redialAttempts=2`; each `lineAttemptCount=1`; no second dial |
| `s2-07-authorization-expiry-not-started` | Early report, expiry proof, `OUTBOUND_NOT_STARTED` | early `QUARANTINED`/`AUTHORIZATION_NOT_EXPIRED`; final `FAILED`, 0 seconds, task `FAILED`, `RELEASED`; `usageFacts=0`; `releases=1` |
| `s2-08-redial-second-bleg-rejected` (negative) | Second prepare, second dispatch, changed direct authorization | 409 `BLEG_OPERATION_EXISTS`; 409 `REDIAL_FORBIDDEN`; 409 `SESSION_IDENTITY_MISMATCH`; `reservationsCreated=1`; `lineAttempts=1`; `redialAttempts=2` |
| `s2-09-heartbeat-and-terminal` | Heartbeats before and after terminal | both `IGNORED`/`HEARTBEAT_IGNORED`; `eventsIgnored=2`; `releases=1`; `settlements=0`; terminal absorbed once |
| `s2-10-terminal-contradiction-quarantine` (negative) | Contradictory second terminal; changed event fingerprint; `CALL_TERMINATED` after `UNKNOWN` | `CONTRADICTORY_FACT`; `EVENT_ID_CONFLICT`; `USE_RECONCILIATION_EVENT`; `eventsConflicted=1`; `eventsQuarantined=2`; `releases=1`; `usageFacts=0`; outbox permanent=1 |
| `s2-11-resolve-never-committed-not-started` (negative) | Resolve never commits, no local B-leg, later `OUTBOUND_NOT_STARTED` | 503 before side effects; local prepare 409 `CALL_UNKNOWN`; event 404 `CALL_UNKNOWN`; `callsCreated=0`, `reservationsCreated=0`, releases/usage=0; outbox `PERMANENT_FAILURE` |

## 7. Validator changes

`protocol/ivr/validate.mjs` now:

- Keeps the S1 stage projection exactly (12 messages, 22 identities, 15 invariant IDs, six S1 fixtures, identity-only event map) and keeps S1 fixture evaluation exact.
- For S2, validates the exact four-column event rows, the absence of any nested `payloadState`, all S2 sections, the 11 payload definitions, the nine end reasons, the ten quarantine reasons, the state machines and terminal mapping, the B-leg machine and the outbox registry.
- Recomputes the event registry digest and requires `{frozenAt:"s2", sha256:<digest>}`.
- Normalizes doc `{}` to the model's empty payload array in the event marker comparison.
- Executes wire requests and IVR-local `bleg.prepare` / `bleg.dispatch` / `bleg.reconcile` / `bleg.expire` / restart / line-outcome steps with a deterministic machine that computes event replay/conflict, legs/bridge/recording, terminal absorption, UNKNOWN occupancy, reconciliation, settlement/release/reconciling, task terminal state, B-leg phases, line attempts, redial rejection and outbox state.
- Adds S2 token coverage (event types, payloads, end/quarantine registries, B-leg phases, outbox states, state machines, terminal mapping, digest).
- Emits `event_registry_frozen=s2 event_registry_sha256=<digest>` for S2/S3.
- Uses cumulative fixture IDs and the marked-negative count.

`executionCounts.redialAttempts` is an evaluator-only diagnostic, not a dial count. It counts rejected redial-shaped local operations: a second B-leg prepare, a second or invalid dispatch or line outcome, and an illegal reconciliation. `lineAttempts` and the per-operation `lineAttemptCount` are the facts for actual provider-line dial attempts; `lineAttemptCount` remains 0 or 1 per call. Fixture keys and values are unchanged by this clarification.

## 8. Commands and observed results

Executed static checks (observations):

| Command | Observed |
| --- | --- |
| `file -bi <all changed files>` | doc/model/validator `charset=utf-8`; fixtures `us-ascii` (valid UTF-8 subset) |
| `git diff --check -- docs/16-production-ivr-protocol-spec.md protocol/ivr` | no output (clean) |
| Conflict-marker grep on `docs/16` and `protocol/ivr` | no matches |
| Trailing-whitespace grep on `docs/16` and `protocol/ivr` | 0 occurrences |
| Multiline `,\s*[}\]]` grep on model and fixtures | no trailing commas |
| Brace/bracket occurrence counts on model and validator | balanced (model `{}` 216/216, `[]` 149/149; validator `{}` 1207/1207, `()` 1611/1611, `[]` 443/443) |
| `sha256sum tmp/a2_event_canonical.json` | `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` |
| `git status --porcelain` scoped to changed paths | exactly the allowed modified/added paths |

Blocked executable validation (runner permission mode denied the commands; not claimed as executed):

- `cd api/openapi && npm run check:s3` → `This command requires approval`.
- `node --check protocol/ivr/validate.mjs` → `This command requires approval`.
- `node protocol/ivr/validate.mjs --stage s2 ...` (twice) → `This command requires approval`.
- `node protocol/ivr/validate.mjs --stage s1 ...` → `This command requires approval`.

Expected stage-s2 output when the controller reruns the exact plan command:

```text
stage=s2 messages=12 event_types=11 event_map_form=full-frozen identities=22 fixtures=17 passed=17 failed=0 negative=3 slice_fixtures=6+11
a1_operations=49 ivr_operations=6 a1_drift=0
a0_tables=44 a0_status_drift=0 a0_registry_drift=0 a0_column_drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
invariants=15
event_registry_frozen=s2 event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9
PASS protocol-validation (s2)
```

Stage S1 on this S2 tree is expected to fail specifically because:

- the S1 event projection requires exactly `eventType,payloadState` with `DEFERRED_S2`, while the tree has the four S2 columns and no `payloadState`;
- the S2 top-level sections and nested event columns are premature for S1;
- the doc event marker header is the S2 four-column header and the S1 adjacent `DEFERRED_S2` statement is intentionally absent.

A stage-s1 pass on this S2 model would be a false claim; it was not run and is not claimed.

## 9. A1/A0 drift evidence

The A1 and A0 sources were not modified. The validator retains the accepted checks; expected (pending executable rerun) values are:

- `a1_operations=49`, `ivr_operations=6`, `a1_drift=0`.
- `a0_tables=44`, `a0_status_drift=0`, `a0_registry_drift=0`, `a0_column_drift=0`.
- `doc_missing=doc_extra=doc_duplicate=doc_invalid=token_missing=0`.
- `stage_field_missing=stage_field_premature=fixture_id_missing=fixture_id_extra=0`.
- `invariants=15`; messages=12; event_types=11; identities=22.
- A1 `IvrEventPayload` remains opaque; no new endpoint, Problem code, A0 table/column/state or transaction was introduced.

## 10. Mutation probes

Executable probes were blocked with the Node/npm denial, so the probes are documented as static, reproducible mutations in `tmp/a2-s2-probe-manifest.md`. The production checks that reject each probe are:

- missing/extra/reordered event row → exact 11-row/order/deep-equal checks and doc marker row counters;
- stale `payloadState` → S2 nested-key prohibition and exact four-column row check;
- digest change → recomputed canonical SHA-256 equality;
- unknown event → 400 `IVR_ARGUMENT_INVALID` and no state write;
- duplicate same event → first receipt, no second settlement, replay counter;
- same ID changed fingerprint → 409 `EVENT_ID_CONFLICT` and outbox permanent;
- second B-leg/redial → local 409 and `lineAttemptCount<=1` invariant;
- out-of-order contradiction → quarantine, not partial application;
- UNKNOWN + redial/release → `REDIAL_FORBIDDEN`, `USE_RECONCILIATION_EVENT`, occupied reservation invariant;
- over-authorized settlement → `RECONCILING`, no usage/release;
- heartbeat release → `IGNORED` and no business change;
- resolve never committed → 503 before side effects, event 404, no placeholder/release/usage, outbox permanent.

## 11. Prototype-rule alignment

- Task path: the frozen task callee is dialed automatically after committed claim; no callee confirmation. B-leg preparation is local and one-per-call.
- Direct path: DTMF collection/confirmation remains only on the no-task direct path; direct success is the purpose-specific null-action rule.
- Activation: key `1` grants at most one first trial; no login/real-name creation; resolve is never rewritten.
- Purchase: guidance-only; no order or payment.
- Operator/PBX/provider: not participants; no provider/security/numeric policy values were invented.
- User-facing routing behavior is unchanged; S2 adds execution facts and failure handling only.

## 12. Residual risks and owners

| ID | Residual | Owner/destination |
| --- | --- | --- |
| `R-A2-A0-TASKID-TYPE` | A0 presentation mismatch; normative A1/A0 §4.23 text is followed | A0 documentation clarification |
| `R-A2-HEARTBEAT-VOLUME` | Durable heartbeat volume/retention | Telephony/operations, Q8/Q13 |
| `R-A2-TASK-TERMINAL` | Task terminal mapping is not runtime-proven | IVR/backend integration tests |
| `R-A2-RECONCILIATION-AUTHORITY` | Reconciliation authority depends on IVR line/CDR records | Telephony/IVR, Q28 |
| `R-A2-CALLID-UNIQUENESS` | Cross-node `callId` uniqueness is runtime-guaranteed | IVR gateway, Q28 |
| `R-A2-NO-RETRY-CALLEE` | One B-leg per call; callee retry is a product change | Product/IVR |
| `R-A2-PROMPT-DTMF` | Prompt copy and DTMF pacing are not frozen | Product/legal, Q10 |
| `R-A2-SIGNATURE` | Signature/nonce/time-window values are not frozen | Security/backend, Q7/Q15/Q25 |
| `R-A2-DECLARE-REPLAY`, `R-A2-UPLOAD-OBSERVE`, `R-A2-EVIDENCE-HANDOFF`, `R-A2-MULTI-RECORDING` | S3 media/evidence boundaries | S3/evidence owners |

Deferred inputs remain Q7, Q8, Q10, Q18, Q25, Q27 and Q13; Q15/Q26/Q28 are closed for the S2 event/B-leg shapes, with recording media wire still S3.

## 13. Runtime limitations and stop state

- No backend, database, Flyway, SIP/CDR, provider, COS, Redis or evidence runtime was run.
- The fixture evaluator is an in-memory protocol model, not a production acceptance matrix; it does not prove concurrency, real line behavior, media bytes, signatures or capacity.
- Node/npm execution was denied by the runner permission mode; the executable validation and probes must be rerun by the controller.
- S3 was not started; no declaration/upload/complete/evidence behavior was implemented beyond recording start/end communication facts.
- No staging, commit, push, PR/issue creation, merge, code review or next-gate action was performed.
- Stop state: S2 complete pending controller executable validation.
