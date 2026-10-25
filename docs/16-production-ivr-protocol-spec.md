# ONECALL production IVR protocol specification

> Work unit: `A2-production-ivr-protocol-spec`
>
> Slice: S1 (`authorization truth`) and S2 (`execution truth`)
>
> Protocol version: `1.0`
>
> Machine model: `protocol/ivr/v1/ivr-protocol-v1.json`
>
> Deterministic validator: `protocol/ivr/validate.mjs`
>
> Authority: A1 remains authoritative for paths, operations, envelopes, request/response schemas and Problem codes. A0 remains authoritative for tables, columns, states, named CHECKs, identities, transactions and idempotency. This document is authoritative for the S1 authorization semantics and the S2 execution semantics described below and does not restate A1 or A0 wire/schema definitions.

## Status and S2 boundary

S1 freezes authorization truth: the unified `IvrDecision` action-selection contract, the 12-message identity registry, the 22-row identity matrix, protocol version compatibility, decision replay/conflict, A1 error mapping and the S1-approved state transitions.

S2 freezes execution truth on top of S1 without changing A0, A1, the user-facing routing rules or any S1 action/replay/version rule. It promotes the S1 identity-only event block into the full four-column frozen event map, and adds the event envelope, 11 closed payloads, nine `endReason` values, ten quarantine/ignore reasons, the backend execution state machines, the IVR-local B-leg exactly-once operation, the IVR event outbox, heartbeat behavior, terminal/UNKNOWN/reconciliation convergence and reservation settlement/release/reconciling rules. The self-built IVR remains the owner of telephony and media facts; the backend remains the owner of task, entitlement and business state.

S3 remains absent and is not authorized by this document: recording declaration, upload observation, completion, media object lifecycle, evidence creation or solidification and post-terminal media conflict behavior are S3 scope. S2 only carries recording start/end communication facts inside the existing event surface.

The event block below is full-frozen at S2. The S1 `DEFERRED_S2` sentinel form is replaced in place; both forms must never coexist. Any statement that a deferred S2 area is still deferred is a protocol error.

The user-visible rules preserved by this slice are:

- A task-backed call automatically dials the frozen task callee after committed authorization. There is no callee confirmation step in the voice call.
- Only the no-task direct path collects and confirms the callee through DTMF.
- Key `1` activation confirmation may grant exactly one first trial to an unregistered, unclaimed or unverified caller. It does not create a login account or a real-name verification record.
- IVR purchase handling is guidance to the WeChat Mini Program or Web only. The IVR creates no order and takes no payment.
- Operator systems, PBX integration, extensions and seat PINs are not protocol participants.

## 1. Protocol version and compatibility

| Item | S1/S2 contract |
| --- | --- |
| Literal | `1.0` |
| Grammar | `^[0-9]+\.[0-9]+$`, length 1-32 (the A1 field bounds) |
| Transport | Only `protocolVersion` in the `POST /api/v1/ivr/inbound/resolve` request body. Other operations do not carry a version. |
| Freeze | A successful resolve freezes the version on the call/session. Every later message for the same `callId` is interpreted under the frozen version. |
| Supported set | Exactly `["1.0"]`. |
| Unknown or malformed version | `400 IVR_ARGUMENT_INVALID` before any session, call, task or reservation write. |
| Minor compatibility | A future minor may add optional fields or new event types only. A new session is required; a call never upgrades mid-call. |
| Major compatibility | A new major requires a new session. An in-flight v1 call continues under v1; if the server cannot continue it, the call fails closed with `503 SERVICE_UNAVAILABLE` and is not redialed. |
| Unknown enum or unknown event type | Fail closed. Closed request bodies use `422 VALIDATION_FAILED`; A2 event payload or legacy event-type violations use `400 IVR_ARGUMENT_INVALID`. |
| Downgrade | Never fall back to a latest version, ignore unknown fields or perform best-effort parsing. |

## 2. Unified action-selection contract

All three decision purposes use the A1 `IvrDecision` shape and one backend selector. The IVR executes only the returned action; it does not recompute routing, derive caller state, select a different callee or combine conditions locally. The three purposes are `INBOUND_RESOLVE`, `ACTIVATION_NEXT_ACTION` and `DIRECT_AUTHORIZATION`.

| Action / direct success | Required result | IVR behavior | Forbidden |
| --- | --- | --- | --- |
| `DIAL_TASK_CALLEE` | `taskMatch=CLAIMED`; task, reservation, authorized seconds and expiry present; full callee only in the encrypted internal task payload | Persist the unique `callId` B-leg operation before any line request; dial the frozen task callee; bridge and record after answer; never ask for callee confirmation | Calling direct authorization; selecting another callee; recomputing the task |
| `ENTER_CALLEE` | No task; `taskMatch=NONE`; no reservation | Enter DTMF collection and confirmation; then call direct authorization and execute its result | Claiming a task; treating the DTMF callee as a task callee |
| `GUIDE_ACTIVATION` | No reservation; reason `ACTIVATION_REQUIRED` or `CONSENT_REQUIRED` | Play the versioned notice; after explicit key `1`, call activation confirmation at most once; execute the next action | Activating before key `1`; repeating confirmation to obtain a second trial; rewriting the resolve decision |
| `GUIDE_PURCHASE` | No reservation; `NO_AVAILABLE_ENTITLEMENT` | Play purchase guidance for the Mini Program or Web and end | Creating an order; taking payment; outbound dialing |
| `REJECT` | No reservation; matching rejection reason | Play failure notice and end | Outbound dialing; claiming a task; retrying routing |
| `REJECT_SUSPENDED` | No reservation; `ACTIVATION_SUSPENDED` | Play suspension notice and offline service entry, then end | Restoring activation through IVR |
| `REJECT_CALL_IN_PROGRESS` | No reservation; `CALL_IN_PROGRESS` | Play retry-later notice and end | Concurrent outbound dialing |
| Direct success (`purpose=DIRECT_AUTHORIZATION`, `action=null`, `reasonCode=DIRECT_AUTHORIZED`) | `taskMatch=NONE`; task fields null; reservation present; confirmed callee only in the encrypted authorization payload | Persist the unique `callId` B-leg operation before any line request; dial the confirmed direct callee | Using task fields; authorizing direct outbound a second time; recollecting the callee |

Shared S1 rules:

- One decision has exactly one executable intent. Direct success is a purpose-specific execution rule, not an eighth action.
- `availableRecordingSeconds` is a routing snapshot only. `authorizedRecordingSeconds` is the only spend ceiling.
- `authorizationExpiresAt` is the last instant to start a B-leg. A B-leg started before the cutoff continues under its frozen seconds. Replay never renews the authorization.
- The task path never asks the caller to confirm the callee. The direct path never falls back to a task.
- One call supports at most one successful activation confirmation and one successful direct authorization. Replay never rewrites the first frozen result.
- The action selector is the backend service `select_call_action`. S1 fixes its output contract, not its SQL or runtime implementation.

The purpose chains are:

- `INBOUND_RESOLVE` selects the first action for the call.
- `ACTIVATION_NEXT_ACTION` exists only after a successful activation confirmation for a prior `GUIDE_ACTIVATION` resolve. It may return a task or direct action but never rewrites the resolve decision.
- `DIRECT_AUTHORIZATION` exists only on the no-task `ENTER_CALLEE` path. Success has a null action, a committed reservation and `DIRECT_AUTHORIZED`.

## 3. S1 preconditions and state transitions

Only the following transitions are frozen by S1. S2 adds the call, leg, terminal, reconciliation and reservation terminal machines in §6.9; recording media lifecycle remains S3. The S1 precondition IDs are `task-claim`, `session-confirm`, `decision-freeze`, `reservation-held` and `direct-authorization`.

| Aggregate | S1 transition | Trigger | Guard |
| --- | --- | --- | --- |
| `outbound_call_task` | `PENDING` to `CLAIMED` | Task-backed `DIAL_TASK_CALLEE` decision | Atomic conditional claim with one reservation and one frozen decision in the same transaction |
| `inbound_session` | `RECEIVED` to `CONFIRMED` | Successful key `1` activation confirmation | Prior `GUIDE_ACTIVATION` resolve, current policy/prompt/consent, same confirmed subject, at most one first trial |
| `usage_reservation` | new to `HELD` | Task-backed or direct authorization success | One reservation per call |
| `ivr_decision` | new to frozen | Resolve, activation confirmation or direct authorization | Unique `(callId,purpose)` and canonical purpose fingerprint |

Task claim preconditions:

- The task is `PENDING`, unexpired under the database clock and belongs to the current trusted caller, subject, binding and workspace/access state.
- The task is claimed only through the conditional update `PENDING` to `CLAIMED`, and the task, reservation and decision commit atomically. A failed claim or reservation leaves no dial authorization.
- The reciprocal task/call link is preserved. The IVR never switches or replaces a claimed task.

Activation confirmation preconditions:

- The prior `INBOUND_RESOLVE` decision on the same call is `GUIDE_ACTIVATION`.
- The session is `RECEIVED`; the confirmation is explicit key `1` with current policy, prompt and consent content.
- The confirmed subject matches the captured subject. A suspended subject cannot reach `CONFIRMED`.
- The first trial is granted at most once per subject. No login account, real-name record or unapproved identity is created.
- A confirmation without a `GUIDE_ACTIVATION` origin returns a normal `200` `REJECT` with `CONFIRMATION_NOT_ALLOWED` and no activation, trial or reservation.

Direct authorization preconditions:

- The prior decision is `ENTER_CALLEE` and the call has no task.
- The callee is the server-normalized DTMF result. The request carries no task identity and cannot claim a task.
- Exactly one reservation is created. A task-backed call cannot use this path.
- Success has `action=null`, `taskMatch=NONE`, `reasonCode=DIRECT_AUTHORIZED`, a committed reservation and only an internal `authorization.calleeNumber`.

Full decision fields (A0 §10.14 projection):

- Required top-level A1 fields: `decisionId`, `callId`, `purpose`, `action`, `callerState`, `taskMatch`, `reasonCode`, `availableRecordingSeconds`, `balanceAsOf`, `reservationId`, `authorizedRecordingSeconds`, `authorizationExpiresAt`, `decisionAt`.
- Nested/internal fields: `task.taskId`, `task.businessType`, `task.calleeMasked`, `task.calleeNumber`, `authorization.calleeNumber`.
- `authorization.calleeNumber` and `task.calleeNumber` are internal encrypted payload values only. They never appear in a redacted snapshot, log, audit, outbox or customer response.
- A0 §10.14 presents `task.taskId` as `uuid`, while A0 §4.23 defines `task_id` as `text` 1-64 and A1 defines a string 1-64. S1 follows the normative schema/A1 and records residual `R-A2-A0-TASKID-TYPE`; no A0 edit is made.

## 4. Replay, conflict and error mapping

| Scenario | Identity | S1 rule |
| --- | --- | --- |
| Resolve retry | `callId` plus `INBOUND_RESOLVE` fingerprint | Same fingerprint returns the first frozen decision without claiming, reserving or renewing. Changed caller, access or node fingerprint is `409 SESSION_IDENTITY_MISMATCH`. |
| Activation confirmation retry | Body `eventId` plus canonical confirmation fingerprint; event ID excluded from the fingerprint | Same semantics with a new `eventId` returns the first `ACTIVATION_NEXT_ACTION` decision. Changed semantics or prompt is `409 SESSION_IDENTITY_MISMATCH`. The resolve decision is never rewritten. |
| Direct authorization retry | Body `eventId` excluded from the direct fingerprint | Same semantics with a new `eventId` returns the first direct decision. Changed callee, prompt, key or confirmation content is `409 SESSION_IDENTITY_MISMATCH`. No second reservation. |
| Replay after success | Original decision identity | Returns the same frozen decision and never grants a second trial, claim or reservation. |
| Unknown call on confirm/direct | `callId` | `409 SESSION_IDENTITY_MISMATCH`. A1 declares no `CALL_UNKNOWN` for these operations, so S1 does not invent one. |
| Lost response | Original identity | Retry the same identity and payload. Do not mint a new call or decision identity. |
| Unsupported version | Resolve body | `400 IVR_ARGUMENT_INVALID` before any side effect. |

S1 does not define event replay, heartbeat replay, terminal absorption, B-leg recovery or settlement; S2 owns those in §6. Recording media replay remains S3.

The exact A1 error mapping is:

| Condition | Operation | HTTP | Problem code | Retryable | Side effect |
| --- | --- | ---: | --- | --- | --- |
| Closed-schema type or length failure | Any | 422 | `VALIDATION_FAILED` | No | None |
| Version, unknown event type, A2 payload or leg rule | Any | 400 | `IVR_ARGUMENT_INVALID` | No | No session, call or event write |
| Decision, session, access-number or fingerprint conflict | resolve, confirm, direct | 409 | `SESSION_IDENTITY_MISMATCH` | No | None |
| Unknown call on confirm or direct | confirm, direct | 409 | `SESSION_IDENTITY_MISMATCH` | No | None |
| Event identity or semantics conflict | events | 409 | `EVENT_ID_CONFLICT` | No | First outcome unchanged |
| Missing parent call | events, declare, complete | 404 | `CALL_UNKNOWN` | No | No placeholder |
| Recording identity or declaration conflict | declare, complete | 409 | `RECORDING_IDENTITY_CONFLICT` | No | No overwrite |
| Completion before upload observation | complete | 409 | `UPLOAD_OBSERVATION_REQUIRED` | Yes, same identity | None |
| Upload expired | complete | 409 | `UPLOAD_EXPIRED` | No | Recording quarantine; no media fact |
| Storage or media outcome unknown | complete | 503 | `STORAGE_UNKNOWN` | Yes, same identity | No media fact |
| Rate limit or dependency failure | Any | 429 or 503 | `RATE_LIMITED` or `SERVICE_UNAVAILABLE` | Conditional | None or rollback |
| Internal failure | Any | 500/default | `INTERNAL` | Conditional | None or rollback |
| Normal business refusal | resolve, confirm, direct | 200 | `IvrDecision` | No | Exactly the frozen decision |
| Event fact contradiction, ordering or unknown leg | events | 200 | `CallEventReceipt` with `QUARANTINED` | No | Fact not applied |

Per-operation A1 error sets, frozen as the exact machine model values:

| Operation | Exact error codes |
| --- | --- |
| `resolveIvrInbound` | `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `IVR_ARGUMENT_INVALID`, `SESSION_IDENTITY_MISMATCH`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL` |
| `confirmIvrActivation` | `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `IVR_ARGUMENT_INVALID`, `SESSION_IDENTITY_MISMATCH`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL` |
| `authorizeIvrDirectOutbound` | `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `IVR_ARGUMENT_INVALID`, `SESSION_IDENTITY_MISMATCH`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL` |
| `ingestIvrCallEvent` | `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `IVR_ARGUMENT_INVALID`, `CALL_UNKNOWN`, `EVENT_ID_CONFLICT`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL` |
| `declareIvrRecording` | `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `IVR_ARGUMENT_INVALID`, `CALL_UNKNOWN`, `RECORDING_IDENTITY_CONFLICT`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL` |
| `completeIvrRecording` | `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `IVR_ARGUMENT_INVALID`, `CALL_UNKNOWN`, `UPLOAD_OBSERVATION_REQUIRED`, `RECORDING_IDENTITY_CONFLICT`, `UPLOAD_EXPIRED`, `STORAGE_UNKNOWN`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL` |

Decision reason values are the A1/A0 21-value registry:

`ACTIVATION_REQUIRED`, `CONSENT_REQUIRED`, `CALLER_UNTRUSTED`, `SESSION_IDENTITY_MISMATCH`, `CALL_IN_PROGRESS`, `CONFIRMATION_NOT_ALLOWED`, `ACTIVATION_SUSPENDED`, `CALLER_BINDING_REVOKED`, `WORKSPACE_ACCESS_REVOKED`, `TASK_ACCESS_REVOKED`, `ORGANIZATION_INACTIVE`, `ACCOUNT_DISABLED`, `CALL_STATE_UNRESOLVED`, `TASK_CONFLICT`, `TASK_CLAIMED`, `NO_AVAILABLE_ENTITLEMENT`, `NO_VALID_TASK`, `TASK_EXPIRED`, `TASK_UNAVAILABLE`, `DIRECT_AUTHORIZED`, `DATA_INTEGRITY`.

Action values are the A0/A1 seven-value registry:

`GUIDE_ACTIVATION`, `DIAL_TASK_CALLEE`, `ENTER_CALLEE`, `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS`.

`TASK_CONFLICT`, `CALL_IN_PROGRESS` and `CONFIRMATION_NOT_ALLOWED` are decision reasons, never Problem codes. `DIRECT_AUTHORIZED` is the only reason paired with a null action.

## 5. Message registry

The 12 messages are projections of the six accepted A1 IVR operations. `slice` records behavioral ownership only; the S2 and S3 message rows exist in S1 because their wire identity and A1 schema mapping are frozen now. Their payload, execution and media behavior remain deferred.

<!-- protocol-message-map:start -->
| messageId | slice | operationId | methodPath | requestSchema | responseSchema | identity |
| --- | --- | --- | --- | --- | --- | --- |
| resolve.request | S1 | resolveIvrInbound | POST /api/v1/ivr/inbound/resolve | IvrInboundResolveRequest | IvrDecision | callId |
| resolve.decision | S1 | resolveIvrInbound | POST /api/v1/ivr/inbound/resolve | - | IvrDecision | decisionId |
| activation-confirm.request | S1 | confirmIvrActivation | POST /api/v1/ivr/calls/{callId}/activation-confirmations | IvrActivationConfirmRequest | IvrDecision | eventId + callId |
| activation-confirm.decision | S1 | confirmIvrActivation | POST /api/v1/ivr/calls/{callId}/activation-confirmations | - | IvrDecision | decisionId |
| direct-authorization.request | S1 | authorizeIvrDirectOutbound | POST /api/v1/ivr/calls/{callId}/outbound-authorizations | IvrDirectAuthorizationRequest | IvrDecision | eventId + callId |
| direct-authorization.decision | S1 | authorizeIvrDirectOutbound | POST /api/v1/ivr/calls/{callId}/outbound-authorizations | - | IvrDecision | decisionId |
| call-event.request | S2 | ingestIvrCallEvent | POST /api/v1/ivr/calls/{callId}/events | IvrCallEventRequest | CallEventReceipt | eventId |
| call-event.receipt | S2 | ingestIvrCallEvent | POST /api/v1/ivr/calls/{callId}/events | - | CallEventReceipt | eventId |
| recording-declare.request | S3 | declareIvrRecording | POST /api/v1/ivr/calls/{callId}/recordings | RecordingDeclarationRequest | RecordingDeclaration | recordingId |
| recording-declare.result | S3 | declareIvrRecording | POST /api/v1/ivr/calls/{callId}/recordings | - | RecordingDeclaration | recordingId |
| recording-complete.request | S3 | completeIvrRecording | POST /api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete | RecordingCompletionRequest | RecordingCompletion | recordingId |
| recording-complete.result | S3 | completeIvrRecording | POST /api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete | - | RecordingCompletion | recordingId |
<!-- protocol-message-map:end -->

## 6. S2 event registry, envelope and payloads

S2 replaces the S1 identity-only event block in place with the following full-frozen four-column map. The model `eventTypes` array, this table and the payload registry in §6.2 are one projection. Row order is fixed. `envelopeLegId` is `REQUIRED` or `ABSENT`. `payloadFields` is the closed payload key set in dictionary order; a `?` suffix marks an optional or conditional field; `{}` is the empty payload. `normalProcessingOutcome` is the normal accepted outcome; quarantine and ignore reasons are decided by the fact rules in §6.3 and never change this column.

The event registry digest is `eventRegistry: frozenAt=s2 sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`, computed over the 11 `eventTypes` entries in fixed order as canonical JSON (UTF-8, sorted object keys, no insignificant whitespace). S3 may read the registry but must not change it; a changed digest is drift.

<!-- protocol-event-map:start -->
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
<!-- protocol-event-map:end -->

The S2 form is full-frozen, not a deferred identity list. Payload, envelope, processing-state and terminal semantics are implemented by S2; the S1 `DEFERRED_S2` sentinel is removed. The event names are exactly `CALLER_LEG_ANSWERED`, `CALLEE_LEG_RINGING`, `CALLEE_LEG_ANSWERED`, `BRIDGE_STARTED`, `RECORDING_STARTED`, `RECORDING_ENDED`, `CALL_TERMINATED`, `CALL_TERMINATION_UNKNOWN`, `CALL_TERMINAL_RECONCILED`, `OUTBOUND_NOT_STARTED` and `CALL_HEARTBEAT`.

### 6.1 Event envelope

| Envelope field | Contract |
| --- | --- |
| `eventId` | Required. 1-128. Globally unique in the `IVR_EVENT` domain and immutable. |
| `eventType` | Required. Exactly one of the 11 frozen types above. |
| `legId` | Required when `envelopeLegId=REQUIRED`; forbidden when `envelopeLegId=ABSENT`. 1-64, unique per call and immutable. |
| `sequenceNo` | Optional positive integer. The server never requires arrival order; it is used for conflict detection and observation. |
| `occurredAt` | Required UTC `date-time` IVR fact time. It is never used for task TTL or authorization judgement. |
| `payload` | Required closed object defined in §6.2. Unknown, missing required or extra fields fail with `400 IVR_ARGUMENT_INVALID` and write no event. |

`leg_type` and `direction` are never submitted on the wire. They are derived: `callerLeg*` maps to `CALLER`/`INBOUND`; `calleeLeg*` maps to `CALLEE`/`OUTBOUND`. The `IvrEventPayload` schema remains opaque in A1; A2 owns this closed payload vocabulary.

### 6.2 Closed payload definitions

| eventType | Required payload fields | Optional payload fields | Backend effect |
| --- | --- | --- | --- |
| `CALLER_LEG_ANSWERED` | none | none | Create or advance the caller leg to `ANSWERED`; `started_at`/`answered_at` = `occurredAt` |
| `CALLEE_LEG_RINGING` | none | none | Create the callee leg as `RINGING`; `started_at` = `occurredAt` |
| `CALLEE_LEG_ANSWERED` | none | none | Advance the callee leg to `ANSWERED`; `answered_at` = `occurredAt` |
| `BRIDGE_STARTED` | `callerLegId`, `calleeLegId` | none | Both legs must exist and be `ANSWERED`; `call_record.bridged_at` = `occurredAt` |
| `RECORDING_STARTED` | none | none | Require `bridged_at` and `occurredAt >= bridged_at`; set `recording_started_at` |
| `RECORDING_ENDED` | none | none | Require `recording_started_at` and `occurredAt >= recording_started_at`; set `recording_ended_at` |
| `CALL_TERMINATED` | `callerLegId`, `callerLegOutcome`, `endReason`, `authoritativeRecordingSeconds` | `callerLegAnsweredAt`, `calleeLegId`, `calleeLegStartedAt`, `calleeLegAnsweredAt`, `calleeLegOutcome`, `bridgedAt`, `recordingStartedAt`, `recordingEndedAt` | Authoritative terminal: call `ENDED`/`FAILED`, leg ends, bridge/recording times, authoritative seconds, task terminal and settlement handoff |
| `CALL_TERMINATION_UNKNOWN` | `reasonCode` | none | `ACTIVE` to `UNKNOWN`; `end_reason=LINE_OUTCOME_UNKNOWN`; `ended_at` and authoritative seconds stay `NULL`; no release and no redial |
| `CALL_TERMINAL_RECONCILED` | `callerLegId`, `callerLegOutcome`, `endReason`, `authoritativeRecordingSeconds`, `resolvesUnknownEventId` | The same optional terminal times and callee fields as `CALL_TERMINATED` | Only `UNKNOWN` to `ENDED`/`FAILED`; must reference an accepted `CALL_TERMINATION_UNKNOWN` on the same call; performs terminal settlement |
| `OUTBOUND_NOT_STARTED` | `reasonCode` | `reservationId` | Authoritative proof that no line request ever started after the deadline; call/task `FAILED` with 0 seconds and reservation `RELEASED` |
| `CALL_HEARTBEAT` | none | none | No business state change; processing `IGNORED` with reason `HEARTBEAT_IGNORED` |

Payload rules:

- `CALL_TERMINATION_UNKNOWN.reasonCode` is exactly `LINE_OUTCOME_UNKNOWN`. `OUTBOUND_NOT_STARTED.reasonCode` is exactly `AUTHORIZATION_EXPIRED`.
- Terminal `callerLegOutcome` and `calleeLegOutcome` are exactly `ENDED` or `FAILED` and must agree with `endReason`.
- Terminal times are non-decreasing: `callerLegAnsweredAt <= bridgedAt <= recordingStartedAt <= recordingEndedAt <= occurredAt`, and `calleeLegStartedAt <= calleeLegAnsweredAt <= occurredAt`.
- When a leg row does not already exist, the terminal payload must carry that leg's start or answer time; otherwise the event is `QUARANTINED`/`CONTRADICTORY_FACT`.
- `authoritativeRecordingSeconds` is a non-negative integer and must be 0 when `recordingStartedAt` is absent. A value above the authorized seconds does not reject the event, but settlement enters `RECONCILING` and no usage fact or evidence is produced.
- `occurredAt` is the call `ended_at`. The authoritative seconds and task terminal state derive only from accepted terminal communication facts.

### 6.3 Event identity, ordering, terminal absorption and quarantine

- An `eventId` is globally unique in `IVR_EVENT`. The same `eventId` on another call is an identity conflict.
- Same `eventId` and same fingerprint (type, call, leg, sequence, occurred time and payload) returns the first receipt and changes no state. Same `eventId` with a changed fingerprint, type or sequence is `409 EVENT_ID_CONFLICT` and never overwrites the first outcome.
- `sequenceNo` is optional and arrival order is not required. A different `eventId` reusing the same `(callId, sequenceNo)` produces a new `QUARANTINED`/`SEQUENCE_CONFLICT` event; the sequence index stays `NULL` and the original sequence remains in the redacted source.
- An unknown parent call is `404 CALL_UNKNOWN` with no placeholder. An unknown event type, payload field or envelope-leg violation is `400 IVR_ARGUMENT_INVALID` with no event write.
- After a call is terminal, only `CALL_HEARTBEAT` is accepted (`IGNORED`). Any other event whose facts are fully consistent is `IGNORED`/`ALREADY_TERMINAL`; an event that would add or change a fact is `QUARANTINED`/`CALL_TERMINAL`.
- `CALL_TERMINATED` on an `UNKNOWN` call is `QUARANTINED`/`USE_RECONCILIATION_EVENT`. Only `CALL_TERMINAL_RECONCILED` converges `UNKNOWN`.
- Missing or foreign `resolvesUnknownEventId` is `400 IVR_ARGUMENT_INVALID` with no event write. `CALL_TERMINAL_RECONCILED` on a non-`UNKNOWN` call is `QUARANTINED`/`NOT_UNKNOWN`.
- `OUTBOUND_NOT_STARTED` before the deadline is `QUARANTINED`/`AUTHORIZATION_NOT_EXPIRED`; after any outbound fact it is `QUARANTINED`/`OUTBOUND_ALREADY_STARTED`; with no reservation it is `400 IVR_ARGUMENT_INVALID`.
- Event processing status uses only A0 `RECEIVED/APPLIED/IGNORED/QUARANTINED`; the first terminal is absorbed.
- Quarantine and ignore reasons are written to `call_event.reason_code` and are not Problem codes. The exact 10-value registry is `LEG_UNKNOWN`, `SEQUENCE_CONFLICT`, `CONTRADICTORY_FACT`, `CALL_TERMINAL`, `ALREADY_TERMINAL`, `AUTHORIZATION_NOT_EXPIRED`, `OUTBOUND_ALREADY_STARTED`, `USE_RECONCILIATION_EVENT`, `NOT_UNKNOWN` and `HEARTBEAT_IGNORED`.

### 6.4 Heartbeat and liveness

A heartbeat is a `CALL_HEARTBEAT` event, never a lease, a reservation proof or a terminal proof. A known call returns `200 CallEventReceipt{processingStatus:"IGNORED", reasonCode:"HEARTBEAT_IGNORED"}`. An unknown call returns `404 CALL_UNKNOWN`. A repeated `eventId` returns the first receipt. A heartbeat never changes call, task, reservation, recording or evidence state, never releases a reservation, never authorizes a call and never triggers redial. Missing heartbeats may only prompt an operations reconciliation; interval, timeout and alert thresholds remain Q8 deferred. The heartbeat row is a durable `call_event`; its retention and volume remain Q13/Q8 residual.

### 6.5 IVR-local B-leg operation and exactly-once recovery

Before any line request, the IVR persists one local authoritative `BlegOperation` keyed by `callId`. It is not an A0 table and does not require a media-engine rewrite; it fixes logical state and recovery decisions only.

| Field | Rule |
| --- | --- |
| `decisionId` | The frozen decision that authorized the call |
| `reservationId` | The reservation created by that decision; non-null for task and direct paths |
| `authorizationExpiresAt` | Copied from the decision; never renewed |
| `frozenCalleeDigest` | Local digest of the frozen callee; plaintext callee is never stored or logged |
| `lineAttemptCount` | 0 or 1; changes from 0 to 1 only on the actual line request and is never 2 |
| `phase` | One of the local phases below |
| `dispatchMarker` | The persisted unique marker written before entering `DISPATCHING` |
| `terminalEventId` | The local authoritative terminal event identity when one exists |

Local phases are `ABSENT`, `PREPARED`, `DISPATCHING`, `DIALING`, `RINGING`, `ANSWERED`, `BRIDGED`, `RECORDING`, `DISPATCH_UNKNOWN`, `NOT_DISPATCHED`, `AUTHORIZATION_EXPIRED`, `FAILED` and `ENDED`. The allowed transitions are:

| From | Allowed next phases | Guard |
| --- | --- | --- |
| `ABSENT` | `PREPARED` | Decision is `DIAL_TASK_CALLEE` or direct success |
| `PREPARED` | `DISPATCHING` | Marker persisted; `now < authorizationExpiresAt`; `lineAttemptCount = 0` |
| `DISPATCHING` | `DIALING` / `DISPATCH_UNKNOWN` / `FAILED` | The single line request; marker already committed |
| `DIALING` | `RINGING` / `ANSWERED` / `FAILED` / `ENDED` | Line accepted |
| `RINGING` | `ANSWERED` / `FAILED` / `ENDED` | — |
| `ANSWERED` | `BRIDGED` / `FAILED` / `ENDED` | — |
| `BRIDGED` | `RECORDING` / `FAILED` / `ENDED` | — |
| `RECORDING` | `ENDED` / `FAILED` | Accepted terminal |
| `DISPATCH_UNKNOWN` | `DIALING` / `ANSWERED` / `ENDED` / `FAILED` / `NOT_DISPATCHED` | Authoritative line/CDR record only; never a direct redial |
| `NOT_DISPATCHED` | `PREPARED` / `AUTHORIZATION_EXPIRED` | Authoritative proof of no attempt; `lineAttemptCount` stays 0 |
| `AUTHORIZATION_EXPIRED` | outbox report only | Deadline reached and non-dispatch proven |
| `FAILED` / `ENDED` | none | Terminal; never redial |

Recovery decisions:

1. After restart the IVR reads the local operation and event outbox by `callId`. It never mints a new `callId` and never re-resolves to obtain a new authorization.
2. `PREPARED` may dispatch once if the deadline has not passed; if it has passed, report `OUTBOUND_NOT_STARTED`.
3. `DISPATCHING` or `DISPATCH_UNKNOWN` consults authoritative local line/CDR evidence. `ATTEMPTED`, `IN_PROGRESS`, `ANSWERED` or `COMPLETED` restores the facts with `lineAttemptCount=1` and never redials. Authoritative `NO_ATTEMPT` before the deadline returns `NOT_DISPATCHED` and then `PREPARED`, allowing the first actual dispatch. Unprovable outcomes stay unknown and occupied until reconciliation.
4. A deadline reached without proof of non-dispatch is never reported as `OUTBOUND_NOT_STARTED`; it stays unknown/occupied for reconciliation.
5. A lost decision response when the local operation never reached `PREPARED` is resolved by retrying the same `callId`. If the original resolve committed, the retry returns the frozen decision; if it never commits, a later `OUTBOUND_NOT_STARTED` returns `404 CALL_UNKNOWN`, creates no placeholder, releases nothing and produces no usage, and the local outbox records `PERMANENT_FAILURE`.
6. A B-leg that started before the deadline continues under its frozen seconds; the deadline is not an interrupt or redial permission.
7. v1 has exactly one B-leg per call. Retrying a callee requires a new call and reservation and is a product change outside A2.

Evaluator-only counter semantics: `lineAttempts` and the per-operation `lineAttemptCount` are the facts for actual provider-line dial attempts. `lineAttemptCount` stays 0 or 1 per call. The deterministic fixture evaluator's `redialAttempts` counter does not count dials; it counts rejected redial-shaped local operations, namely a second B-leg prepare, a second or invalid dispatch or line outcome, and an illegal reconciliation.

### 6.6 IVR event outbox

| State | Rule |
| --- | --- |
| `PENDING` | The event/declaration/completion intent is persisted before send |
| `SENT` | Sent but no terminal receipt is known |
| `ACKED` | A 2xx terminal receipt (`APPLIED`, `IGNORED` or `QUARANTINED`) was observed |
| `PERMANENT_FAILURE` | Identity, argument or terminal conflict; alert, never rewrite into a new identity |

Retransmission uses the identical `eventId` and payload. A call-event 2xx (including `QUARANTINED`) is terminal for the outbox. `EVENT_ID_CONFLICT`, `SESSION_IDENTITY_MISMATCH`, `CALL_UNKNOWN`, `IVR_ARGUMENT_INVALID` and `VALIDATION_FAILED` are permanent. `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL`, 5xx and network failures are retryable. No retransmission changes identity or redials. Recording declaration and completion outbox classification belongs to S3 and is not implemented here.

### 6.7 Terminal, UNKNOWN and reconciliation

- The only authoritative terminal classes are `CALL_TERMINATED`, `CALL_TERMINAL_RECONCILED` and `OUTBOUND_NOT_STARTED`.
- An unprovable line outcome reports only `CALL_TERMINATION_UNKNOWN`: call `UNKNOWN`, no `ended_at`, no authoritative seconds, task remains `CLAIMED`, reservation remains `HELD` and enters `RECONCILING` after the authorization expiry; it is never released and never redialed.
- `UNKNOWN` converges only through `CALL_TERMINAL_RECONCILED` referencing the accepted unknown event, or backend operations reconciliation. Convergence completes terminal settlement or release and the task terminal in the same transaction.
- Late or contradictory terminal facts are `QUARANTINED`; they never reopen a task, double-charge or create evidence.
- A 30-second TTL, a local lease or a missing heartbeat never makes a `CLAIMED` task or an `UNKNOWN` call claimable again.
- Task terminal state is separate from recording validation and evidence solidification.

### 6.8 End reasons, task terminal and settlement mapping

The nine `endReason` values are `CALLER_HANGUP_AFTER_BRIDGE`, `CALLEE_HANGUP_AFTER_BRIDGE`, `RECORDING_LIMIT_REACHED`, `CALLER_HANGUP_BEFORE_BRIDGE`, `CALLEE_BUSY`, `CALLEE_NO_ANSWER`, `CALLEE_REJECTED`, `B_LEG_FAILED` and `BRIDGE_FAILED`.

| endReason | call_record | task (task-backed) |
| --- | --- | --- |
| `CALLER_HANGUP_AFTER_BRIDGE` | `ENDED` | `COMPLETED` |
| `CALLEE_HANGUP_AFTER_BRIDGE` | `ENDED` | `COMPLETED` |
| `RECORDING_LIMIT_REACHED` | `ENDED` | `COMPLETED` |
| `CALLER_HANGUP_BEFORE_BRIDGE` | `FAILED` | `FAILED` |
| `CALLEE_BUSY` | `FAILED` | `FAILED` |
| `CALLEE_NO_ANSWER` | `FAILED` | `FAILED` |
| `CALLEE_REJECTED` | `FAILED` | `FAILED` |
| `B_LEG_FAILED` | `FAILED` | `FAILED` |
| `BRIDGE_FAILED` | `FAILED` | `FAILED` |

- `OUTBOUND_NOT_STARTED`: call `FAILED`, `end_reason=AUTHORIZATION_EXPIRED`, authoritative seconds 0, task `FAILED`, reservation `RELEASED` and no usage fact. This mapping assumes the original resolve committed and the backend already has the call and reservation.
- When the original resolve never committed, no call or reservation exists. A later `OUTBOUND_NOT_STARTED` is an unknown parent call: `404 CALL_UNKNOWN`, no placeholder, no release and no usage; the IVR outbox records `PERMANENT_FAILURE`.
- `CALL_TERMINATION_UNKNOWN`: call `UNKNOWN`, `end_reason=LINE_OUTCOME_UNKNOWN`, task unchanged and reservation not released.
- Authoritative seconds are independent of task terminal state. A non-zero recording still settles under A0 T-S2-05 even when the call/task failed; 0 seconds is a known-zero release.
- Seconds in `1..authorized_units` settle, release the remainder and produce exactly one `usage_fact`. Zero seconds produce `RELEASED` with no usage fact. Seconds above the authorization do not settle or release, produce no usage fact, keep the reservation in `RECONCILING` and set `last_error_code=DURATION_EXCEEDS_AUTHORIZED`; there is no tolerance.
- Task `COMPLETED`/`FAILED` is triggered only by an accepted terminal or reconciliation, never by heartbeat, declaration, completion or evidence state.
- Leg mapping: `CALLER_HANGUP_BEFORE_BRIDGE` ends the caller leg and fails the callee leg if present; busy/no-answer/rejected/B-leg failure fails the callee leg and ends the caller leg; post-bridge normal or limit cases end both legs; `BRIDGE_FAILED` ends both legs; a missing callee leg stays null. Leg `end_reason` uses the same `endReason` value.

### 6.9 Execution state machines

The model `stateMachines` freezes the S2 backend transitions for `outbound_call_task`, `inbound_session`, `call_record`, `call_leg`, `ivr_decision` and `usage_reservation`, plus the `terminalMapping` above. They never conflict with the S1 preconditions and add no new state or named CHECK.

| Aggregate | Terminal-relevant transitions |
| --- | --- |
| `outbound_call_task` | `PENDING→CLAIMED`; `CLAIMED→COMPLETED/FAILED`; never reopened |
| `inbound_session` | `RECEIVED→CONFIRMED/REJECTED`; `ABANDONED` has no v1 wire trigger |
| `call_record` | new→`ACTIVE`/`REJECTED`; `ACTIVE→ENDED/FAILED/UNKNOWN`; `UNKNOWN→ENDED/FAILED` only by reconciliation |
| `call_leg` | create `RINGING`/`ANSWERED`; `RINGING→ANSWERED`; `ANSWERED→ENDED/FAILED` |
| `ivr_decision` | frozen once per `(callId,purpose)` |
| `usage_reservation` | `HELD→SETTLED/RELEASED/RECONCILING`; `RECONCILING→SETTLED/RELEASED`; at most one terminal |

The model `blegOperation` and `eventOutbox` freeze the local phase machine, recovery rules and outbox states described in §6.5 and §6.6. These are IVR-local; they do not create A0 tables or columns.

## 7. Identity, ownership, precondition and replay matrix

| identity | owner | uniqueness | precondition | replay/conflict | A0/A1 mapping |
| --- | --- | --- | --- | --- | --- |
| callId | IVR gateway | globally unique across nodes; 1-128; immutable; one callId per inbound call | created on first inbound; every later message requires the existing call | same callId and fingerprint replays; changed caller/access/node is a stable conflict | ivr.inbound_session.call_id; ivr.call_record.call_id; resolve path |
| sessionId | backend | UUID; one per callId; immutable | created or reused by resolve | derived from callId; no client input | ivr.inbound_session.id |
| decisionId | backend | UUID; one per (callId,purpose); frozen | first decision for the purpose committed | same fingerprint replays; changed fingerprint is 409 SESSION_IDENTITY_MISMATCH | ivr.ivr_decision.id; uq_ivr_decision_call_purpose |
| decision fingerprint | backend | one canonical SHA-256 per decision purpose; immutable | computed at decision time | same fingerprint replays; changed fingerprint is 409 SESSION_IDENTITY_MISMATCH | ivr.ivr_decision.request_fingerprint; A0 §10.17 |
| taskId | backend | text 1-64; globally unique; immutable after claim | PENDING task claimed for a task-backed decision | same task replays; a different task never replaces a claimed task | ivr.outbound_call_task.task_id; ivr.call_record.task_id |
| reservationId | backend | UUID; one per call; terminal once | committed with the task claim or direct authorization | replay never renews and never creates a second reservation | entitlement_metering.usage_reservation.id; uq_usage_reservation_call |
| action / executable intent | backend | one per decision; frozen | selected only by the backend action selector | replay returns the same action; the IVR never reselects | ivr.ivr_decision.action; ck_ivr_decision_action |
| eventId | IVR | 1-128; globally unique in IVR_EVENT; immutable | parent call exists | same eventId and fingerprint returns the first receipt; changed content is EVENT_ID_CONFLICT | ivr.call_event.event_id; uq_call_event_domain_id |
| sequenceNo | IVR | positive integer; unique per call when present | event creation | a different event reusing it is a sequence conflict | ivr.call_event.sequence_no |
| legId | IVR | 1-64; unique per call; immutable | leg event or terminal fact | same leg facts are idempotent; conflicting facts are quarantined | ivr.call_leg.leg_id |
| B-leg operation | IVR local | key callId; one per call; durable local operation | committed authorization decision and before any line request | restore only; never create a second B-leg operation | A0 has no table; Q28 local operation |
| recordingId | IVR | 1-128; globally unique; immutable | call known and upload not expired | same declaration or observed facts replay; changed identity is RECORDING_IDENTITY_CONFLICT | evidence.recording.recording_id |
| upload credential | backend | short-lived; single object; not an identity; not return-once | declaration succeeded | may be reissued or replayed but never changes recording or object identity | A0 upload session; no column |
| upload expiry | backend | equals recording.upload_expires_at; not renewed | declaration created | replay does not extend it; expiry is UPLOAD_EXPIRED | evidence.recording.upload_expires_at |
| terminal fact | IVR to backend | first accepted terminal per call; eventId identity | call exists and its state permits the fact | duplicate returns the first outcome; a conflicting terminal is quarantined | ivr.call_record.ended_at; ivr.call_record.end_reason; ivr.call_record.authoritative_seconds; ivr.call_event.event_id |
| heartbeat | IVR | eventId; repeatable; no business state change | call known | same eventId replays; never releases, authorizes or redials | ivr.call_event.processing_status |
| bridge fact | IVR | callId plus legIds; no independent backend identity | both legs answered or terminal reconciliation | consistent facts are idempotent; regressing facts are quarantined | ivr.call_record.bridged_at |
| recording start/end | IVR | callId plus eventId | bridge occurred | consistent facts are idempotent; contradictory facts are quarantined | ivr.call_record.recording_started_at; ivr.call_record.recording_ended_at |
| media object | backend | internal UUID/version/hash; immutable | upload observation | same object/version/hash is reused; a different identity conflicts | evidence.media_object.id |
| evidence record | backend | internal UUID; downstream owner | recording VALIDATED and evidence owner flow | A2 defines no evidence wire replay | evidence.evidence_record.id |
| settlement fact | backend | unique reservation/type and source event | accepted terminal fact | duplicates return the first fact; conflicts are quarantined | entitlement_metering.usage_fact.id; uq_usage_fact_reservation_type |
| IVR event outbox | IVR local | (callId,eventId); immutable payload | event generated | retransmit the same identity; never mint a replacement identity | Q28 local durable store |

Machine-readable identity projection (the accepted four-column form):

<!-- protocol-identity-map:start -->
| identity | owner | uniqueness | a0a1Mapping |
| --- | --- | --- | --- |
| callId | IVR gateway | globally unique across nodes; 1-128; immutable; one callId per inbound call | ivr.inbound_session.call_id; ivr.call_record.call_id; resolve path |
| sessionId | backend | UUID; one per callId; immutable | ivr.inbound_session.id |
| decisionId | backend | UUID; one per (callId,purpose); frozen | ivr.ivr_decision.id; uq_ivr_decision_call_purpose |
| decision fingerprint | backend | one canonical SHA-256 per decision purpose; immutable | ivr.ivr_decision.request_fingerprint; A0 §10.17 |
| taskId | backend | text 1-64; globally unique; immutable after claim | ivr.outbound_call_task.task_id; ivr.call_record.task_id |
| reservationId | backend | UUID; one per call; terminal once | entitlement_metering.usage_reservation.id; uq_usage_reservation_call |
| action / executable intent | backend | one per decision; frozen | ivr.ivr_decision.action; ck_ivr_decision_action |
| eventId | IVR | 1-128; globally unique in IVR_EVENT; immutable | ivr.call_event.event_id; uq_call_event_domain_id |
| sequenceNo | IVR | positive integer; unique per call when present | ivr.call_event.sequence_no |
| legId | IVR | 1-64; unique per call; immutable | ivr.call_leg.leg_id |
| B-leg operation | IVR local | key callId; one per call; durable local operation | A0 has no table; Q28 local operation |
| recordingId | IVR | 1-128; globally unique; immutable | evidence.recording.recording_id |
| upload credential | backend | short-lived; single object; not an identity; not return-once | A0 upload session; no column |
| upload expiry | backend | equals recording.upload_expires_at; not renewed | evidence.recording.upload_expires_at |
| terminal fact | IVR to backend | first accepted terminal per call; eventId identity | ivr.call_record.ended_at; ivr.call_record.end_reason; ivr.call_record.authoritative_seconds; ivr.call_event.event_id |
| heartbeat | IVR | eventId; repeatable; no business state change | ivr.call_event.processing_status |
| bridge fact | IVR | callId plus legIds; no independent backend identity | ivr.call_record.bridged_at |
| recording start/end | IVR | callId plus eventId | ivr.call_record.recording_started_at; ivr.call_record.recording_ended_at |
| media object | backend | internal UUID/version/hash; immutable | evidence.media_object.id |
| evidence record | backend | internal UUID; downstream owner | evidence.evidence_record.id |
| settlement fact | backend | unique reservation/type and source event | entitlement_metering.usage_fact.id; uq_usage_fact_reservation_type |
| IVR event outbox | IVR local | (callId,eventId); immutable payload | Q28 local durable store |
<!-- protocol-identity-map:end -->

The identity matrix freezes wire identity, not S2/S3 behavior. Rows for terminal, media and evidence facts exist because the v1 surface has those identities; their state and payload semantics are deferred.

## 8. Invariants

S1 registers the 15 cross-cutting invariant IDs. S2 and S3 may add prose and execution semantics, but must not add or rename an invariant ID.

| ID | Invariant |
| --- | --- |
| INV-A2-01 | One callId maps to one inbound session/call; a second callId is never minted for the same inbound call. |
| INV-A2-02 | One decision has exactly one executable intent; the IVR never recomputes routing or selects another callee. |
| INV-A2-03 | One call has at most one B-leg operation and one actual line request; unknown outcomes or restarts never cause a second dial. |
| INV-A2-04 | One call has at most one reservation, one terminal settlement or release, and one usage fact. |
| INV-A2-05 | The first accepted terminal fact is absorbed; terminal facts never regress; UNKNOWN converges only through reconciliation. |
| INV-A2-06 | The (callId,purpose) decision is immutable; activation confirmation never rewrites the resolve decision. |
| INV-A2-07 | The single-caller single-nonterminal-task rule and reciprocal task/call links remain A0 invariants. |
| INV-A2-08 | Event identity is immutable; the same identity and fingerprint returns the first outcome, while changed content conflicts or quarantines. |
| INV-A2-09 | Recording identity is immutable; declared facts never overwrite server-observed facts; completion never solidifies evidence. |
| INV-A2-10 | The task path never asks for callee confirmation; the direct path never claims a task; there is no latest-callee fallback. |
| INV-A2-11 | Heartbeat never changes business state, never releases a reservation, never authorizes a call and never triggers redial. |
| INV-A2-12 | No protocol output, log, audit or outbox exposes a plaintext callee or an object key, URL or credential. |
| INV-A2-13 | The protocol version is frozen at resolve; an unsupported version fails before side effects; a call never upgrades mid-call. |
| INV-A2-14 | Every unknown outcome fails closed without redial, double reservation, double settlement or contradictory facts. |
| INV-A2-15 | Declaration and completion retries reuse recordingId and never create a second recording or media object. |

S2 execution meaning for the same 15 IDs (no ID is added or renamed):

| ID | S2 execution meaning |
| --- | --- |
| INV-A2-01 | Resolve creates at most one call/session per inbound call; retries and restart reuse the `callId`. |
| INV-A2-02 | The IVR executes the frozen action and one B-leg; event facts never cause local rerouting or another callee. |
| INV-A2-03 | The local operation is keyed by `callId`; `lineAttemptCount` moves only 0 to 1 and unknown/restart recovery never redials. |
| INV-A2-04 | One reservation is terminal once: one settlement or release and at most one usage fact per call. |
| INV-A2-05 | The first accepted terminal is absorbed; `UNKNOWN` is occupied and converges only through `CALL_TERMINAL_RECONCILED`. |
| INV-A2-06 | Terminal events never rewrite the decision; the S1 purposes remain frozen. |
| INV-A2-07 | Task `CLAIMED→COMPLETED/FAILED` uses the same conditional update and preserves A0 reciprocal links. |
| INV-A2-08 | Event replay uses the same `eventId` fingerprint; changed semantics is `EVENT_ID_CONFLICT` or quarantine. |
| INV-A2-09 | S2 only records recording start/end facts; declaration, upload, completion and evidence remain S3. |
| INV-A2-10 | Task terminals follow the frozen task path; direct terminals never claim a task; no latest-callee fallback. |
| INV-A2-11 | `CALL_HEARTBEAT` is always `IGNORED` and never releases, authorizes or redials. |
| INV-A2-12 | The B-leg stores only a callee digest; terminal facts, logs and outbox expose no plaintext callee. |
| INV-A2-13 | The version frozen at S1 resolve governs the event/terminal interpretation. |
| INV-A2-14 | `UNKNOWN` occupies the call and reservation; no redial, second reservation or settlement occurs. |
| INV-A2-15 | S2 carries no declaration or completion execution; the invariant remains the S3 boundary. |

## 9. Deterministic S2 validation and fixtures

`protocol/ivr/validate.mjs --stage s2` performs five independent checks:

1. The machine model uses the exact S2 top-level sections; a missing S2 section or a premature S1/S3 form fails. `payloadState` is forbidden anywhere.
2. The document marker blocks match the model row-for-row and column-for-column. The event block is exactly `eventType,envelopeLegId,payloadFields,normalProcessingOutcome` with 11 rows; `{}` in the document normalizes to the model's empty payload array.
3. The S2 registries are exact: `eventEnvelope` (six envelope fields, 11 closed payloads and payload rules), nine `endReasons`, ten `quarantineReasons`, six backend state machines plus `terminalMapping`, the B-leg fields/phases/transitions/recovery rules and the four outbox states with their call-event classification.
4. `eventRegistry` has exactly `frozenAt=s2` and the canonical SHA-256 of the 11 `eventTypes` entries. A digest change fails. The A1 bundle still has 49 operations, six IVR operations and the exact schema/Problem sets; `IvrEventPayload` remains opaque.
5. A0 still has 44 catalog tables, the exact named CHECK registries and every referenced column. The cumulative fixture set is exactly 17 IDs (six S1 plus 11 S2).

The validator evaluates fixtures with a deterministic in-memory `ProtocolMachine`. For S2 it computes action selection, event identity/replay/conflict, leg/bridge/recording facts, terminal absorption, UNKNOWN occupancy, reconciliation, reservation settlement/release/reconciling, task terminal state, the IVR-local B-leg phase machine and the event outbox. It never reads a fixture's `expected` block to decide a result. The machine has no database, network, clock, random source, SIP/CDR, provider or telephony dependency. Fixture times, identities and line outcomes are fixed inputs. The machine is a fixture evaluator, not a backend, IVR runtime or production acceptance matrix.

The S2 fixtures (cumulative 17) are:

| Fixture | Scenario | Assertions |
| --- | --- | --- |
| `s2-01-task-terminal-completed` | Task resolve, one B-leg, bridge/recording, `CALL_TERMINATED` | call `ENDED`; task `COMPLETED`; reservation `SETTLED`; one usage fact with the authoritative seconds; `lineAttemptCount=1` |
| `s2-02-event-duplicate-replay` | Duplicate terminal and heartbeat with the same `eventId` | first receipt; no second settlement or usage fact; outbox remains `ACKED` |
| `s2-03-event-out-of-order` | Complete terminal first, then consistent late leg facts with a sequence gap | facts applied once; legs/bridge/recording converge; late consistent events `IGNORED`; settlement once |
| `s2-04-terminal-unknown-reconciliation` | `CALL_TERMINATION_UNKNOWN`, redial attempt, then `CALL_TERMINAL_RECONCILED` | UNKNOWN occupies call/reservation; no release/usage; redial blocked; reconciliation converges `ENDED`/`FAILED` and settles once |
| `s2-05-response-loss-retry` | Committed resolve and terminal with lost responses, retried with identical identity | frozen resolve replay; first terminal receipt; no second B-leg/reservation/settlement; outbox `SENT→ACKED` |
| `s2-06-ivr-restart-bleg-recovery` | Restart at `PREPARED` and `DISPATCH_UNKNOWN` | `PREPARED` dispatches once; `DISPATCH_UNKNOWN` reconciles to `DIALING` without redial; `lineAttemptCount<=1` per call |
| `s2-07-authorization-expiry-not-started` | Before-deadline report, then deadline plus proof of no attempt | first report `QUARANTINED`/`AUTHORIZATION_NOT_EXPIRED`; `OUTBOUND_NOT_STARTED` yields `FAILED`, 0 seconds, task `FAILED`, `RELEASED`, no usage |
| `s2-08-redial-second-bleg-rejected` (negative) | Second B-leg, second dispatch/line outcome and duplicate direct authorization | local 409 for the second B-leg and dispatch; `409 SESSION_IDENTITY_MISMATCH` for changed direct semantics; one reservation and `lineAttemptCount<=1` |
| `s2-09-heartbeat-and-terminal` | Heartbeat before and after a terminal | both `IGNORED`/`HEARTBEAT_IGNORED`; no release or settlement from heartbeat; the terminal is still absorbed once |
| `s2-10-terminal-contradiction-quarantine` (negative) | Second terminal with different seconds; `CALL_TERMINATED` after `UNKNOWN` | `QUARANTINED`/`CONTRADICTORY_FACT` and `QUARANTINED`/`USE_RECONCILIATION_EVENT`; no reopen, second settlement or release |
| `s2-11-resolve-never-committed-not-started` (negative) | Resolve never commits; local B-leg/line attempt absent; later `OUTBOUND_NOT_STARTED` | `404 CALL_UNKNOWN`; no call/task/reservation placeholder; no release or usage; outbox `PERMANENT_FAILURE` |

Expected successful stage-s2 output:

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

Running the stage-s1 validator against an S2 tree must fail: the S1 projection requires exactly `eventType,payloadState` with `DEFERRED_S2` and forbids the S2 sections and nested event columns. A stage-s1 pass on an S2 model would be a false claim.

A0 registry names checked directly against `docs/14`:

`ck_outbound_task_status`, `ck_inbound_session_status`, `ck_call_record_status`, `ck_call_leg_state`, `ck_call_event_processing_status`, `ck_ivr_decision_action`, `ck_ivr_decision_purpose`, `ck_ivr_decision_reason`, `ck_usage_reservation_status`, `ck_recording_status`, `ck_evidence_status`, `ck_usage_fact_type`, `ck_usage_fact_event_domain`, `ck_idempotency_scope`.

## 10. Deferred inputs and residual risks

Deferred without invention:

- Q7: line, access number, recording format, buffering and clock provider detail.
- Q8: heartbeat interval, timeout, retry backoff, duration tolerance, retention and SLO values.
- Q10: activation/purchase prompt and consent copy lifecycle.
- Q18: 30-second TTL configuration key, approval and scheduler detail. S1 uses only the already approved 30-second default and frozen per-task expiry.
- Q25: authorization-window and clock-skew values. S1 uses the committed absolute `authorizationExpiresAt`.
- Q27: organization-member task creation/claim role matrix.
- Q13: event/heartbeat/outbox retention.
- Q15/Q26/Q28: event wire values, snapshot/wire mapping and B-leg local recovery. S2 closes the 11 event wire types/payloads, event ordering/quarantine, heartbeat, terminal/UNKNOWN/reconciliation, the B-leg local phase machine keyed by `callId` and the IVR event outbox. Recording media wire remains S3.

Classified residuals:

| ID | Classification | Destination |
| --- | --- | --- |
| `R-A2-A0-TASKID-TYPE` | A0 presentation mismatch; normative A0 §4.23 and A1 text 1-64 are followed | A0 documentation clarification |
| `R-A2-DECLARE-REPLAY` | A1/A0 declaration projection boundary | S3 + API/evidence review |
| `R-A2-SESSION-ABANDONED` | A0 state with no v1 wire trigger | IVR/backend product polish |
| `R-A2-HEARTBEAT-VOLUME` | Durable heartbeat volume and retention | Telephony/operations, Q8/Q13 |
| `R-A2-UPLOAD-OBSERVE` | Object-storage observation trigger | IVR/evidence, Q7/Q8 |
| `R-A2-EVIDENCE-HANDOFF` | Evidence creation/solidification owner | Evidence/backend, A0 §9.28/§10.24 |
| `R-A2-TASK-TERMINAL` | Terminal task mapping is not runtime-proven | IVR/backend integration tests |
| `R-A2-RECONCILIATION-AUTHORITY` | Reconciliation authority depends on IVR CDR/line records | Telephony/IVR, Q28 |
| `R-A2-CALLID-UNIQUENESS` | Cross-node callId uniqueness is runtime-guaranteed | IVR gateway, Q28 |
| `R-A2-MULTI-RECORDING` | A0 may allow multiple recordings per call | Evidence policy |
| `R-A2-NO-RETRY-CALLEE` | One B-leg per call; callee retry is a product change | Product/IVR |
| `R-A2-PROMPT-DTMF` | Prompt copy and DTMF pacing are not frozen | Product/legal, Q10 |
| `R-A2-SIGNATURE` | Signature string, nonce and time window are not frozen | Security/backend, Q7/Q15/Q25 |

S1 and S2 are static contracts. They do not prove PostgreSQL concurrency, actual task claim or reservation behavior, SIP/line/CDR behavior, B-leg dispatch, bridge or recording, COS upload observation, evidence solidification, security signing or production capacity. The fixture evaluator is not a production acceptance matrix.

## 11. Documentation and decision record

- Canonical human specification: this document.
- Canonical machine model: `protocol/ivr/v1/ivr-protocol-v1.json`.
- Deterministic fixtures: `protocol/ivr/v1/fixtures/s1-01-*.json` through `s1-06-*.json` plus `s2-01-*.json` through `s2-11-*.json` (17 cumulative).
- Validator: `protocol/ivr/validate.mjs`, zero third-party dependencies, Node 22.
- S2 promoted the event block from the `DEFERRED_S2` identity form to the full four-column frozen form and owns event payload, execution, recovery, heartbeat, terminal/reconciliation and settlement semantics. The frozen digest is `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`.
- S3 will own recording/media/evidence semantics and must not change the S2 event registry or its digest.
- No README, second protocol document, JSON Schema copy, package file or A0/A1 modification is part of S1 or S2.
- No operator/PBX integration, real credential, provider value, timeout, heartbeat interval, retry backoff, tolerance or cloud resource is introduced.
