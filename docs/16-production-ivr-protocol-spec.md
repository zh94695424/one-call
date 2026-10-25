# ONECALL production IVR protocol specification

> Work unit: `A2-production-ivr-protocol-spec`
>
> Slice: S1 only (`authorization truth`)
>
> Protocol version: `1.0`
>
> Machine model: `protocol/ivr/v1/ivr-protocol-v1.json`
>
> Deterministic validator: `protocol/ivr/validate.mjs`
>
> Authority: A1 remains authoritative for paths, operations, envelopes, request/response schemas and Problem codes. A0 remains authoritative for tables, columns, states, named CHECKs, identities, transactions and idempotency. This document is authoritative for the S1 protocol semantics described below and does not restate A1 or A0 wire/schema definitions.

## Status and S1 boundary

S1 freezes authorization truth: the unified `IvrDecision` action-selection contract, the 12-message identity registry, the 22-row identity matrix, protocol version compatibility, decision replay/conflict, A1 error mapping and the S1-approved state transitions. It is a static specification plus a deterministic fixture evaluator. It is not a runtime backend, database, line, media engine, cloud or evidence implementation.

The following areas are deliberately absent from S1 and are not authorized by this slice:

- S2 event envelope, event payload field sets, event processing state, terminal/end reason registries, UNKNOWN reconciliation, B-leg local phase machine, event outbox, settlement execution and task terminal behavior.
- S3 recording upload lifecycle, media object lifecycle, evidence creation or solidification and post-terminal media conflict behavior.

The S1 event block registers event identities only. Payload, envelope, state and terminal semantics are deferred to S2. Any statement that a deferred area is already frozen is a protocol error.

The user-visible rules preserved by this slice are:

- A task-backed call automatically dials the frozen task callee after committed authorization. There is no callee confirmation step in the voice call.
- Only the no-task direct path collects and confirms the callee through DTMF.
- Key `1` activation confirmation may grant exactly one first trial to an unregistered, unclaimed or unverified caller. It does not create a login account or a real-name verification record.
- IVR purchase handling is guidance to the WeChat Mini Program or Web only. The IVR creates no order and takes no payment.
- Operator systems, PBX integration, extensions and seat PINs are not protocol participants.

## 1. Protocol version and compatibility

| Item | S1 contract |
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

Only the following transitions are frozen by S1. Call, leg, terminal, reconciliation, reservation terminal and recording/media transitions remain S2 or S3 scope. The S1 precondition IDs are `task-claim`, `session-confirm`, `decision-freeze`, `reservation-held` and `direct-authorization`.

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

## 4. Replay, conflict and S1 error mapping

| Scenario | Identity | S1 rule |
| --- | --- | --- |
| Resolve retry | `callId` plus `INBOUND_RESOLVE` fingerprint | Same fingerprint returns the first frozen decision without claiming, reserving or renewing. Changed caller, access or node fingerprint is `409 SESSION_IDENTITY_MISMATCH`. |
| Activation confirmation retry | Body `eventId` plus canonical confirmation fingerprint; event ID excluded from the fingerprint | Same semantics with a new `eventId` returns the first `ACTIVATION_NEXT_ACTION` decision. Changed semantics or prompt is `409 SESSION_IDENTITY_MISMATCH`. The resolve decision is never rewritten. |
| Direct authorization retry | Body `eventId` excluded from the direct fingerprint | Same semantics with a new `eventId` returns the first direct decision. Changed callee, prompt, key or confirmation content is `409 SESSION_IDENTITY_MISMATCH`. No second reservation. |
| Replay after success | Original decision identity | Returns the same frozen decision and never grants a second trial, claim or reservation. |
| Unknown call on confirm/direct | `callId` | `409 SESSION_IDENTITY_MISMATCH`. A1 declares no `CALL_UNKNOWN` for these operations, so S1 does not invent one. |
| Lost response | Original identity | Retry the same identity and payload. Do not mint a new call or decision identity. |
| Unsupported version | Resolve body | `400 IVR_ARGUMENT_INVALID` before any side effect. |

S1 does not define event replay, heartbeat replay, terminal absorption, B-leg recovery, settlement or media replay. Those remain S2/S3.

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

## 6. Event identity registry (identity only at S1)

The S1 event block is identity-only. The 11 event names below are frozen wire identities for S2 and S3 work. S1 does not define an event envelope, a payload field set, a processing state, a terminal/end reason, quarantine semantics, reconciliation or an outbox. The S1 validator must reject any event row that adds a payload, envelope, state or terminal column.

Event identities only; payload/state/terminal semantics deferred to S2. `DEFERRED_S2` is a sentinel and does not mean that payload or state behavior is implemented by this slice.

<!-- protocol-event-map:start -->
| eventType | payloadState |
| --- | --- |
| CALLER_LEG_ANSWERED | DEFERRED_S2 |
| CALLEE_LEG_RINGING | DEFERRED_S2 |
| CALLEE_LEG_ANSWERED | DEFERRED_S2 |
| BRIDGE_STARTED | DEFERRED_S2 |
| RECORDING_STARTED | DEFERRED_S2 |
| RECORDING_ENDED | DEFERRED_S2 |
| CALL_TERMINATED | DEFERRED_S2 |
| CALL_TERMINATION_UNKNOWN | DEFERRED_S2 |
| CALL_TERMINAL_RECONCILED | DEFERRED_S2 |
| OUTBOUND_NOT_STARTED | DEFERRED_S2 |
| CALL_HEARTBEAT | DEFERRED_S2 |
<!-- protocol-event-map:end -->

The event names are `CALLER_LEG_ANSWERED`, `CALLEE_LEG_RINGING`, `CALLEE_LEG_ANSWERED`, `BRIDGE_STARTED`, `RECORDING_STARTED`, `RECORDING_ENDED`, `CALL_TERMINATED`, `CALL_TERMINATION_UNKNOWN`, `CALL_TERMINAL_RECONCILED`, `OUTBOUND_NOT_STARTED` and `CALL_HEARTBEAT`.

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

## 9. Deterministic S1 validation and fixtures

`protocol/ivr/validate.mjs --stage s1` performs four independent checks:

1. The machine model uses only the S1 top-level sections; missing current-stage sections and premature S2/S3 sections fail.
2. The document marker blocks (`protocol-message-map`, `protocol-event-map`, `protocol-identity-map`) match the model row-for-row and column-for-column. Event rows at S1 are exactly `eventType` and `payloadState=DEFERRED_S2`.
3. The A1 bundle still has 49 operations, six IVR operations and the exact operation, schema and Problem sets. `IvrEventPayload` remains opaque.
4. A0 still has 44 catalog tables, the exact named CHECK registries and every referenced column. The 22 identity rows and six fixtures are exact.

The validator evaluates fixtures with a deterministic in-memory `ProtocolMachine`. The machine computes action selection, fingerprint replay, conflict detection, task claim, reservation creation and trial counting. It does not read the fixture's expected outcome to decide a result. The machine has no database, network, clock, random source or telephony dependency. Fixture times and identities are fixed inputs.

The S1 fixtures are:

| Fixture | Scenario | Assertions |
| --- | --- | --- |
| `s1-01-task-path-normal` | Resolve to `DIAL_TASK_CALLEE` | 200; task `CLAIMED`; reservation `HELD`; frozen decision; task auto-dial flag; no callee confirmation |
| `s1-02-direct-path-normal` | `ENTER_CALLEE` then direct success | `action=null`; reservation present; no task and no task fields; direct DTMF confirmation counted exactly once |
| `s1-03-activation-redecision` | `GUIDE_ACTIVATION`, key `1`, then `DIAL_TASK_CALLEE` | Session `CONFIRMED`; one trial; resolve decision unchanged; next decision shares the action shape; no login or real-name creation |
| `s1-04-decision-exact-replay` | Resolve, confirm and direct retries with the same fingerprints | First frozen responses; no second claim, reservation or trial; replay ignores new IDs supplied by the caller |
| `s1-05-decision-semantic-conflict` | Changed caller, access, prompt and direct callee | 409 `SESSION_IDENTITY_MISMATCH`; persisted facts and counts unchanged; no second reservation or trial |
| `s1-06-version-unsupported` | `protocolVersion=2.0` and malformed version | 400 `IVR_ARGUMENT_INVALID`; no call or session; all fact counts zero |

Expected successful stage-s1 output:

```text
stage=s1 messages=12 event_types=11 event_map_form=identity-only identities=22 fixtures=6 passed=6 failed=0 negative=0 slice_fixtures=6
a1_operations=49 ivr_operations=6 a1_drift=0
a0_tables=44 a0_status_drift=0 a0_registry_drift=0 a0_column_drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
invariants=15
PASS protocol-validation (s1)
```

A0 registry names checked directly against `docs/14`:

`ck_outbound_task_status`, `ck_inbound_session_status`, `ck_call_record_status`, `ck_call_leg_state`, `ck_call_event_processing_status`, `ck_ivr_decision_action`, `ck_ivr_decision_purpose`, `ck_ivr_decision_reason`, `ck_usage_reservation_status`, `ck_recording_status`, `ck_evidence_status`, `ck_usage_fact_type`, `ck_usage_fact_event_domain`, `ck_idempotency_scope`.

The S2/S3 registries are not authored in S1. The validator recognizes the stage names but the current model and document are stage-s1 projections; `--stage s2` or `--stage s3` must fail until those slices replace the S1 event projection and add the frozen registries.

## 10. Deferred inputs and residual risks

Deferred without invention:

- Q7: line, access number, recording format, buffering and clock provider detail.
- Q8: heartbeat interval, timeout, retry backoff, duration tolerance, retention and SLO values.
- Q10: activation/purchase prompt and consent copy lifecycle.
- Q18: 30-second TTL configuration key, approval and scheduler detail. S1 uses only the already approved 30-second default and frozen per-task expiry.
- Q25: authorization-window and clock-skew values. S1 uses the committed absolute `authorizationExpiresAt`.
- Q27: organization-member task creation/claim role matrix.
- Q13: event/heartbeat/outbox retention.
- Q15/Q26/Q28: event wire values, snapshot/wire mapping and B-leg local recovery. S1 freezes only the authorization-side action contract, identities, version and replay; the B-leg phase/outbox shape is S2.

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

S1 is a static contract. It does not prove PostgreSQL concurrency, actual task claim or reservation behavior, SIP/line/CDR behavior, B-leg dispatch, bridge or recording, COS upload observation, evidence solidification, security signing or production capacity.

## 11. Documentation and decision record

- Canonical human specification: this document.
- Canonical machine model: `protocol/ivr/v1/ivr-protocol-v1.json`.
- Deterministic fixtures: `protocol/ivr/v1/fixtures/s1-01-*.json` through `s1-06-*.json`.
- Validator: `protocol/ivr/validate.mjs`, zero third-party dependencies, Node 22.
- S2 will promote the event block from the `DEFERRED_S2` identity form to the full frozen form and will own event payload, execution, recovery and terminal semantics.
- S3 will own recording/media/evidence semantics and must not change the S2 event registry.
- No README, second protocol document, JSON Schema copy, package file or A0/A1 modification is part of S1.
- No operator/PBX integration, real credential, provider value, timeout, retry backoff, tolerance or cloud resource is introduced.
