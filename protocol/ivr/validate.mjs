#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { isUtf8 } from 'node:buffer';
import { fileURLToPath } from 'node:url';

const HTTP_METHODS = ['get', 'post', 'put', 'patch', 'delete'];
const S1_OPERATIONS = [
  'resolveIvrInbound',
  'confirmIvrActivation',
  'authorizeIvrDirectOutbound',
  'ingestIvrCallEvent',
  'declareIvrRecording',
  'completeIvrRecording'
];

const EXPECTED_MESSAGES = [
  ['resolve.request', 'S1', 'resolveIvrInbound', 'POST /api/v1/ivr/inbound/resolve', 'IvrInboundResolveRequest', 'IvrDecision', 'callId'],
  ['resolve.decision', 'S1', 'resolveIvrInbound', 'POST /api/v1/ivr/inbound/resolve', '-', 'IvrDecision', 'decisionId'],
  ['activation-confirm.request', 'S1', 'confirmIvrActivation', 'POST /api/v1/ivr/calls/{callId}/activation-confirmations', 'IvrActivationConfirmRequest', 'IvrDecision', 'eventId + callId'],
  ['activation-confirm.decision', 'S1', 'confirmIvrActivation', 'POST /api/v1/ivr/calls/{callId}/activation-confirmations', '-', 'IvrDecision', 'decisionId'],
  ['direct-authorization.request', 'S1', 'authorizeIvrDirectOutbound', 'POST /api/v1/ivr/calls/{callId}/outbound-authorizations', 'IvrDirectAuthorizationRequest', 'IvrDecision', 'eventId + callId'],
  ['direct-authorization.decision', 'S1', 'authorizeIvrDirectOutbound', 'POST /api/v1/ivr/calls/{callId}/outbound-authorizations', '-', 'IvrDecision', 'decisionId'],
  ['call-event.request', 'S2', 'ingestIvrCallEvent', 'POST /api/v1/ivr/calls/{callId}/events', 'IvrCallEventRequest', 'CallEventReceipt', 'eventId'],
  ['call-event.receipt', 'S2', 'ingestIvrCallEvent', 'POST /api/v1/ivr/calls/{callId}/events', '-', 'CallEventReceipt', 'eventId'],
  ['recording-declare.request', 'S3', 'declareIvrRecording', 'POST /api/v1/ivr/calls/{callId}/recordings', 'RecordingDeclarationRequest', 'RecordingDeclaration', 'recordingId'],
  ['recording-declare.result', 'S3', 'declareIvrRecording', 'POST /api/v1/ivr/calls/{callId}/recordings', '-', 'RecordingDeclaration', 'recordingId'],
  ['recording-complete.request', 'S3', 'completeIvrRecording', 'POST /api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete', 'RecordingCompletionRequest', 'RecordingCompletion', 'recordingId'],
  ['recording-complete.result', 'S3', 'completeIvrRecording', 'POST /api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete', '-', 'RecordingCompletion', 'recordingId']
].map((row) => ({
  messageId: row[0],
  slice: row[1],
  operationId: row[2],
  methodPath: row[3],
  requestSchema: row[4],
  responseSchema: row[5],
  identity: row[6]
}));

const EXPECTED_EVENT_TYPES = [
  'CALLER_LEG_ANSWERED',
  'CALLEE_LEG_RINGING',
  'CALLEE_LEG_ANSWERED',
  'BRIDGE_STARTED',
  'RECORDING_STARTED',
  'RECORDING_ENDED',
  'CALL_TERMINATED',
  'CALL_TERMINATION_UNKNOWN',
  'CALL_TERMINAL_RECONCILED',
  'OUTBOUND_NOT_STARTED',
  'CALL_HEARTBEAT'
];

const EVENT_PAYLOAD_DEFINITIONS = {
  CALLER_LEG_ANSWERED: { required: [], optional: [] },
  CALLEE_LEG_RINGING: { required: [], optional: [] },
  CALLEE_LEG_ANSWERED: { required: [], optional: [] },
  BRIDGE_STARTED: { required: ['callerLegId', 'calleeLegId'], optional: [] },
  RECORDING_STARTED: { required: [], optional: [] },
  RECORDING_ENDED: { required: [], optional: [] },
  CALL_TERMINATED: {
    required: ['callerLegId', 'callerLegOutcome', 'endReason', 'authoritativeRecordingSeconds'],
    optional: ['callerLegAnsweredAt', 'calleeLegId', 'calleeLegStartedAt', 'calleeLegAnsweredAt', 'calleeLegOutcome', 'bridgedAt', 'recordingStartedAt', 'recordingEndedAt']
  },
  CALL_TERMINATION_UNKNOWN: { required: ['reasonCode'], optional: [] },
  CALL_TERMINAL_RECONCILED: {
    required: ['callerLegId', 'callerLegOutcome', 'endReason', 'authoritativeRecordingSeconds', 'resolvesUnknownEventId'],
    optional: ['callerLegAnsweredAt', 'calleeLegId', 'calleeLegStartedAt', 'calleeLegAnsweredAt', 'calleeLegOutcome', 'bridgedAt', 'recordingStartedAt', 'recordingEndedAt']
  },
  OUTBOUND_NOT_STARTED: { required: ['reasonCode'], optional: ['reservationId'] },
  CALL_HEARTBEAT: { required: [], optional: [] }
};

const REQUIRED_LEG_EVENTS = new Set(['CALLER_LEG_ANSWERED', 'CALLEE_LEG_RINGING', 'CALLEE_LEG_ANSWERED']);

function payloadFieldsFor(definition) {
  return [...definition.required, ...definition.optional.map((field) => `${field}?`)].sort();
}

const EXPECTED_EVENT_ROWS = EXPECTED_EVENT_TYPES.map((eventType) => ({
  eventType,
  envelopeLegId: REQUIRED_LEG_EVENTS.has(eventType) ? 'REQUIRED' : 'ABSENT',
  payloadFields: payloadFieldsFor(EVENT_PAYLOAD_DEFINITIONS[eventType]),
  normalProcessingOutcome: eventType === 'CALL_HEARTBEAT' ? 'IGNORED' : 'APPLIED'
}));

const EXPECTED_END_REASONS = [
  'CALLER_HANGUP_AFTER_BRIDGE',
  'CALLEE_HANGUP_AFTER_BRIDGE',
  'RECORDING_LIMIT_REACHED',
  'CALLER_HANGUP_BEFORE_BRIDGE',
  'CALLEE_BUSY',
  'CALLEE_NO_ANSWER',
  'CALLEE_REJECTED',
  'B_LEG_FAILED',
  'BRIDGE_FAILED'
];

const EXPECTED_QUARANTINE_REASONS = [
  'LEG_UNKNOWN',
  'SEQUENCE_CONFLICT',
  'CONTRADICTORY_FACT',
  'CALL_TERMINAL',
  'ALREADY_TERMINAL',
  'AUTHORIZATION_NOT_EXPIRED',
  'OUTBOUND_ALREADY_STARTED',
  'USE_RECONCILIATION_EVENT',
  'NOT_UNKNOWN',
  'HEARTBEAT_IGNORED'
];

const EXPECTED_EVENT_ENVELOPE = {
  fields: ['eventId', 'eventType', 'legId?', 'sequenceNo?', 'occurredAt', 'payload'],
  required: ['eventId', 'eventType', 'occurredAt', 'payload'],
  legIdRule: 'required when envelopeLegId is REQUIRED; absent when envelopeLegId is ABSENT',
  legTypeDerivation: 'callerLeg* maps to CALLER/INBOUND; calleeLeg* maps to CALLEE/OUTBOUND; leg_type and direction are never submitted',
  occurredAtSemantics: 'UTC date-time IVR fact time; never used for task TTL or authorization judgement',
  payloadRule: 'closed per eventType; unknown, missing required or extra payload fields fail with 400 IVR_ARGUMENT_INVALID',
  payloads: EVENT_PAYLOAD_DEFINITIONS,
  payloadRules: {
    'CALL_TERMINATION_UNKNOWN.reasonCode': 'LINE_OUTCOME_UNKNOWN',
    'OUTBOUND_NOT_STARTED.reasonCode': 'AUTHORIZATION_EXPIRED',
    legOutcomes: ['ENDED', 'FAILED'],
    timeOrder: 'callerLegAnsweredAt <= bridgedAt <= recordingStartedAt <= recordingEndedAt <= occurredAt; calleeLegStartedAt <= calleeLegAnsweredAt <= occurredAt',
    missingLegTimes: "when a leg row is absent the terminal payload must carry that leg's start or answer time",
    secondsWithoutRecordingStart: 'authoritativeRecordingSeconds must be 0 when recordingStartedAt is absent'
  }
};

const EXPECTED_STATE_MACHINE_STATES = {
  outbound_call_task: ['PENDING', 'CLAIMED', 'REPLACED', 'CANCELLED', 'EXPIRED', 'REJECTED', 'COMPLETED', 'FAILED'],
  inbound_session: ['RECEIVED', 'CONFIRMED', 'ABANDONED', 'REJECTED'],
  call_record: ['ACTIVE', 'ENDED', 'FAILED', 'UNKNOWN', 'REJECTED'],
  call_leg: ['RINGING', 'ANSWERED', 'ENDED', 'FAILED'],
  ivr_decision: ['frozen'],
  usage_reservation: ['HELD', 'RECONCILING', 'SETTLED', 'RELEASED']
};

const EXPECTED_STATE_MACHINE_TRANSITIONS = {
  outbound_call_task: [['PENDING', 'CLAIMED'], ['CLAIMED', 'COMPLETED'], ['CLAIMED', 'FAILED']],
  inbound_session: [['RECEIVED', 'CONFIRMED'], ['RECEIVED', 'REJECTED'], [null, 'ABANDONED']],
  call_record: [[null, 'ACTIVE'], [null, 'REJECTED'], ['ACTIVE', 'ENDED'], ['ACTIVE', 'FAILED'], ['ACTIVE', 'UNKNOWN'], ['UNKNOWN', 'ENDED'], ['UNKNOWN', 'FAILED']],
  call_leg: [[null, 'RINGING'], [null, 'ANSWERED'], ['RINGING', 'ANSWERED'], ['ANSWERED', 'ENDED'], ['ANSWERED', 'FAILED']],
  ivr_decision: [[null, 'frozen']],
  usage_reservation: [['HELD', 'SETTLED'], ['HELD', 'RELEASED'], ['HELD', 'RECONCILING'], ['RECONCILING', 'SETTLED'], ['RECONCILING', 'RELEASED']]
};

const EXPECTED_TERMINAL_MAPPING = {
  CALLER_HANGUP_AFTER_BRIDGE: { callStatus: 'ENDED', taskStatus: 'COMPLETED' },
  CALLEE_HANGUP_AFTER_BRIDGE: { callStatus: 'ENDED', taskStatus: 'COMPLETED' },
  RECORDING_LIMIT_REACHED: { callStatus: 'ENDED', taskStatus: 'COMPLETED' },
  CALLER_HANGUP_BEFORE_BRIDGE: { callStatus: 'FAILED', taskStatus: 'FAILED' },
  CALLEE_BUSY: { callStatus: 'FAILED', taskStatus: 'FAILED' },
  CALLEE_NO_ANSWER: { callStatus: 'FAILED', taskStatus: 'FAILED' },
  CALLEE_REJECTED: { callStatus: 'FAILED', taskStatus: 'FAILED' },
  B_LEG_FAILED: { callStatus: 'FAILED', taskStatus: 'FAILED' },
  BRIDGE_FAILED: { callStatus: 'FAILED', taskStatus: 'FAILED' },
  LINE_OUTCOME_UNKNOWN: { callStatus: 'UNKNOWN', taskStatus: null },
  AUTHORIZATION_EXPIRED: { callStatus: 'FAILED', taskStatus: 'FAILED' }
};

const EXPECTED_BLEG_FIELDS = [
  'decisionId',
  'reservationId',
  'authorizationExpiresAt',
  'frozenCalleeDigest',
  'lineAttemptCount',
  'phase',
  'dispatchMarker',
  'terminalEventId'
];

const EXPECTED_BLEG_PHASES = [
  'ABSENT',
  'PREPARED',
  'DISPATCHING',
  'DIALING',
  'RINGING',
  'ANSWERED',
  'BRIDGED',
  'RECORDING',
  'DISPATCH_UNKNOWN',
  'NOT_DISPATCHED',
  'AUTHORIZATION_EXPIRED',
  'FAILED',
  'ENDED'
];

const EXPECTED_BLEG_TRANSITIONS = [
  ['ABSENT', 'PREPARED'],
  ['PREPARED', 'DISPATCHING'],
  ['DISPATCHING', 'DIALING'],
  ['DISPATCHING', 'DISPATCH_UNKNOWN'],
  ['DISPATCHING', 'FAILED'],
  ['DIALING', 'RINGING'],
  ['DIALING', 'ANSWERED'],
  ['DIALING', 'FAILED'],
  ['RINGING', 'ANSWERED'],
  ['RINGING', 'FAILED'],
  ['ANSWERED', 'BRIDGED'],
  ['ANSWERED', 'FAILED'],
  ['BRIDGED', 'RECORDING'],
  ['BRIDGED', 'ENDED'],
  ['RECORDING', 'ENDED'],
  ['RECORDING', 'FAILED'],
  ['DISPATCH_UNKNOWN', 'DIALING'],
  ['DISPATCH_UNKNOWN', 'ANSWERED'],
  ['DISPATCH_UNKNOWN', 'ENDED'],
  ['DISPATCH_UNKNOWN', 'FAILED'],
  ['DISPATCH_UNKNOWN', 'NOT_DISPATCHED'],
  ['NOT_DISPATCHED', 'PREPARED'],
  ['NOT_DISPATCHED', 'AUTHORIZATION_EXPIRED'],
  ['AUTHORIZATION_EXPIRED', 'ENDED'],
  ['FAILED', 'FAILED'],
  ['ENDED', 'ENDED']
];

const EXPECTED_EVENT_OUTBOX = {
  states: ['PENDING', 'SENT', 'ACKED', 'PERMANENT_FAILURE'],
  identity: '(callId,eventId); immutable payload',
  retransmission: 'resend the identical eventId and payload; never mint a replacement identity',
  callEvent: {
    ackStatuses: ['APPLIED', 'IGNORED', 'QUARANTINED'],
    permanentCodes: ['EVENT_ID_CONFLICT', 'SESSION_IDENTITY_MISMATCH', 'CALL_UNKNOWN', 'IVR_ARGUMENT_INVALID', 'VALIDATION_FAILED'],
    retryableCodes: ['RATE_LIMITED', 'SERVICE_UNAVAILABLE', 'INTERNAL'],
    retryableTransport: true
  },
  recordingClassification: 'deferred to S3'
};

const EXPECTED_RECORDING_LIFECYCLE = {
  states: ['DECLARED', 'UPLOADED', 'VALIDATED', 'QUARANTINED'],
  transitions: [
    { from: null, to: 'DECLARED', trigger: 'first declaration for a known parent call', guard: 'unique recordingId; declaration facts only' },
    { from: 'DECLARED', to: 'UPLOADED', trigger: 'server-local upload observation', guard: 'one immutable media object; replay requires identical observation identity and facts' },
    { from: 'UPLOADED', to: 'VALIDATED', trigger: 'completion with matching server-observed facts', guard: 'observed hash, size, MIME and duration match the declaration; server facts win' },
    { from: 'UPLOADED', to: 'QUARANTINED', trigger: 'completion with different or mismatched server-observed facts', guard: 'quarantine once; first media facts are preserved' },
    { from: 'DECLARED', to: 'QUARANTINED', trigger: 'completion or upload observation at or after uploadExpiresAt', guard: 'no successful media or evidence fact' }
  ],
  declaration: {
    requestFields: ['recordingId', 'mimeType', 'sizeBytes', 'durationSeconds', 'sha256'],
    responseFields: ['recordingId', 'status', 'uploadExpiresAt', 'upload.credential', 'upload.expiresAt', 'declaredAt'],
    status: 'DECLARED',
    replay: 'same recordingId with identical declaration facts while DECLARED returns the first 201 snapshot with no second recording, credential or expiry renewal',
    conflict: 'changed declaration facts or any re-declare after UPLOADED, VALIDATED or QUARANTINED returns 409 RECORDING_IDENTITY_CONFLICT',
    clientReference: 'not accepted by declaration and never part of declaration identity'
  },
  uploadObservation: {
    owner: 'IVR-local/server-observed; not a public IVR endpoint',
    fields: ['objectIdentityDigest', 'sha256', 'sizeBytes', 'mimeType', 'durationSeconds'],
    replay: 'same observation identity and facts returns the first UPLOADED result without a second media object, including replays at or after uploadExpiresAt',
    conflict: 'changed observation identity or facts fails closed with RECORDING_IDENTITY_CONFLICT and mutates nothing, including after uploadExpiresAt',
    expiry: 'a first observation while DECLARED at or after uploadExpiresAt is refused and quarantines once with UPLOAD_EXPIRED; an already UPLOADED row resolves immutable replay or conflict first',
    redaction: 'raw object key, bucket, object version and credential material never leave the observation boundary'
  },
  completion: {
    requestFields: ['clientReference'],
    serverObservedFields: ['objectIdentityDigest', 'sha256', 'sizeBytes', 'mimeType', 'durationSeconds'],
    clientStorageFactsForbidden: ['bucket', 'objectKey', 'objectVersionId', 'sha256', 'sizeBytes', 'mimeType', 'durationSeconds'],
    order: ['known call and recording', 'terminal replay or conflict', 'upload expiry for a still-DECLARED recording', 'upload observation', 'storage unknown', 'validation'],
    expired: '409 UPLOAD_EXPIRED; only a still-DECLARED recording is QUARANTINED; an already UPLOADED recording is not expired by completion time; no successful media fact for the DECLARED row',
    observationRequired: 'a DECLARED recording returns 409 UPLOAD_OBSERVATION_REQUIRED with no mutation; the same identity retry may converge after observation',
    storageUnknown: '503 STORAGE_UNKNOWN with no media fact, terminal mutation or revision change; the same identity retry may converge',
    validated: '200 VALIDATED with server-recomputed duration and hash only',
    quarantined: '200 QUARANTINED for a pre-terminal differing or mismatched observation without overwriting the first media facts',
    replay: 'same recordingId and identical server-observed facts returns the first VALIDATED or QUARANTINED result without a second recording or media object',
    conflict: 'different server-observed facts after VALIDATED or QUARANTINED returns 409 RECORDING_IDENTITY_CONFLICT with unchanged status, revision and facts'
  },
  conflictOutbox: {
    eventDomain: 'EVIDENCE',
    aggregateType: 'RECORDING',
    eventType: 'RECORDING_CONFLICT',
    dedupeKey: 'recording-conflict:<conflictDigest>',
    count: 'exactly one deduplicated redacted fact per identical conflict payload',
    payloadFields: ['recordingId', 'originalStatus', 'conflictDigest', 'presentedSha256', 'presentedSizeBytes', 'presentedMime', 'presentedDuration', 'reasonCode'],
    redaction: 'object version and key material is hashed and never stored or logged'
  },
  outbox: {
    identity: '(callId,recordingId); immutable payload; retries never mint a new identity',
    terminalAbsorption: 'ACKED and PERMANENT_FAILURE are absorbing for one (callId,recordingId) entry; later responses, response loss or post-terminal conflicts never change a terminal entry or increment another terminal counter',
    declaration: {
      ackedHttp: [201],
      permanentCodes: ['RECORDING_IDENTITY_CONFLICT', 'IVR_ARGUMENT_INVALID', 'VALIDATION_FAILED', 'CALL_UNKNOWN'],
      retryableCodes: ['RATE_LIMITED', 'SERVICE_UNAVAILABLE', 'INTERNAL']
    },
    completion: {
      ackedHttp: [200],
      permanentCodes: ['UPLOAD_EXPIRED', 'RECORDING_IDENTITY_CONFLICT', 'IVR_ARGUMENT_INVALID', 'VALIDATION_FAILED', 'CALL_UNKNOWN'],
      retryableCodes: ['UPLOAD_OBSERVATION_REQUIRED', 'STORAGE_UNKNOWN', 'RATE_LIMITED', 'SERVICE_UNAVAILABLE', 'INTERNAL']
    }
  },
  evidenceBoundary: 'recording completion never creates or solidifies evidence; evidence creation identity and policy remain with the evidence owner'
};

const EXPECTED_MEDIA_EVIDENCE_BOUNDARY = {
  owner: 'evidence',
  prerequisite: 'recording.status is VALIDATED',
  handoffFields: ['callId', 'recordingId', 'recordingStatus', 'validatedDurationSeconds', 'validatedSha256', 'mediaObjectDigest'],
  completionCreatesEvidence: false,
  completionSolidifiesEvidence: false,
  downstreamOwnerFlow: 'evidence owner creates the evidence record, creation event and original artifact from its own validated source facts',
  wire: 'no IVR evidence endpoint; the A2 boundary is typed and handoff-ready only',
  redaction: 'no object key, bucket, version, permanent URL, credential, media bytes or raw party identifier crosses the boundary',
  evidenceStatusValues: ['PENDING', 'SOLIDIFIED', 'QUARANTINED', 'FAILED'],
  residual: 'R-A2-EVIDENCE-HANDOFF'
};

const RECORDING_OBSERVATION_FIELDS = ['objectIdentityDigest', 'sha256', 'sizeBytes', 'mimeType', 'durationSeconds'];
const RECORDING_DECLARATION_FIELDS = ['recordingId', 'mimeType', 'sizeBytes', 'durationSeconds', 'sha256'];
const RECORDING_FORBIDDEN_RESPONSE_FIELDS = new Set(['bucket', 'objectKey', 'objectVersionId', 'objectVersion', 'storageProvider', 'cryptoKeyRef', 'permanentUrl', 'url']);

const EXPECTED_IDENTITIES = [
  ['callId', 'IVR gateway', 'globally unique across nodes; 1-128; immutable; one callId per inbound call', 'created on first inbound; every later message requires the existing call', 'same callId and fingerprint replays; changed caller/access/node is a stable conflict', 'ivr.inbound_session.call_id; ivr.call_record.call_id; resolve path'],
  ['sessionId', 'backend', 'UUID; one per callId; immutable', 'created or reused by resolve', 'derived from callId; no client input', 'ivr.inbound_session.id'],
  ['decisionId', 'backend', 'UUID; one per (callId,purpose); frozen', 'first decision for the purpose committed', 'same fingerprint replays; changed fingerprint is 409 SESSION_IDENTITY_MISMATCH', 'ivr.ivr_decision.id; uq_ivr_decision_call_purpose'],
  ['decision fingerprint', 'backend', 'one canonical SHA-256 per decision purpose; immutable', 'computed at decision time', 'same fingerprint replays; changed fingerprint is 409 SESSION_IDENTITY_MISMATCH', 'ivr.ivr_decision.request_fingerprint; A0 §10.17'],
  ['taskId', 'backend', 'text 1-64; globally unique; immutable after claim', 'PENDING task claimed for a task-backed decision', 'same task replays; a different task never replaces a claimed task', 'ivr.outbound_call_task.task_id; ivr.call_record.task_id'],
  ['reservationId', 'backend', 'UUID; one per call; terminal once', 'committed with the task claim or direct authorization', 'replay never renews and never creates a second reservation', 'entitlement_metering.usage_reservation.id; uq_usage_reservation_call'],
  ['action / executable intent', 'backend', 'one per decision; frozen', 'selected only by the backend action selector', 'replay returns the same action; the IVR never reselects', 'ivr.ivr_decision.action; ck_ivr_decision_action'],
  ['eventId', 'IVR', '1-128; globally unique in IVR_EVENT; immutable', 'parent call exists', 'same eventId and fingerprint returns the first receipt; changed content is EVENT_ID_CONFLICT', 'ivr.call_event.event_id; uq_call_event_domain_id'],
  ['sequenceNo', 'IVR', 'positive integer; unique per call when present', 'event creation', 'a different event reusing it is a sequence conflict', 'ivr.call_event.sequence_no'],
  ['legId', 'IVR', '1-64; unique per call; immutable', 'leg event or terminal fact', 'same leg facts are idempotent; conflicting facts are quarantined', 'ivr.call_leg.leg_id'],
  ['B-leg operation', 'IVR local', 'key callId; one per call; durable local operation', 'committed authorization decision and before any line request', 'restore only; never create a second B-leg operation', 'A0 has no table; Q28 local operation'],
  ['recordingId', 'IVR', '1-128; globally unique; immutable', 'call known and upload not expired', 'same declaration or observed facts replay; changed identity is RECORDING_IDENTITY_CONFLICT', 'evidence.recording.recording_id'],
  ['upload credential', 'backend', 'short-lived; single object; not an identity; not return-once', 'declaration succeeded', 'may be reissued or replayed but never changes recording or object identity', 'A0 upload session; no column'],
  ['upload expiry', 'backend', 'equals recording.upload_expires_at; not renewed', 'declaration created', 'replay does not extend it; expiry is UPLOAD_EXPIRED', 'evidence.recording.upload_expires_at'],
  ['terminal fact', 'IVR to backend', 'first accepted terminal per call; eventId identity', 'call exists and its state permits the fact', 'duplicate returns the first outcome; a conflicting terminal is quarantined', 'ivr.call_record.ended_at; ivr.call_record.end_reason; ivr.call_record.authoritative_seconds; ivr.call_event.event_id'],
  ['heartbeat', 'IVR', 'eventId; repeatable; no business state change', 'call known', 'same eventId replays; never releases, authorizes or redials', 'ivr.call_event.processing_status'],
  ['bridge fact', 'IVR', 'callId plus legIds; no independent backend identity', 'both legs answered or terminal reconciliation', 'consistent facts are idempotent; regressing facts are quarantined', 'ivr.call_record.bridged_at'],
  ['recording start/end', 'IVR', 'callId plus eventId', 'bridge occurred', 'consistent facts are idempotent; contradictory facts are quarantined', 'ivr.call_record.recording_started_at; ivr.call_record.recording_ended_at'],
  ['media object', 'backend', 'internal UUID/version/hash; immutable', 'upload observation', 'same object/version/hash is reused; a different identity conflicts', 'evidence.media_object.id'],
  ['evidence record', 'backend', 'internal UUID; downstream owner', 'recording VALIDATED and evidence owner flow', 'A2 defines no evidence wire replay', 'evidence.evidence_record.id'],
  ['settlement fact', 'backend', 'unique reservation/type and source event', 'accepted terminal fact', 'duplicates return the first fact; conflicts are quarantined', 'entitlement_metering.usage_fact.id; uq_usage_fact_reservation_type'],
  ['IVR event outbox', 'IVR local', '(callId,eventId); immutable payload', 'event generated', 'retransmit the same identity; never mint a replacement identity', 'Q28 local durable store']
].map((row) => ({
  identity: row[0],
  owner: row[1],
  uniqueness: row[2],
  precondition: row[3],
  replayConflict: row[4],
  a0a1Mapping: row[5]
}));

const EXPECTED_INVARIANTS = [
  'INV-A2-01',
  'INV-A2-02',
  'INV-A2-03',
  'INV-A2-04',
  'INV-A2-05',
  'INV-A2-06',
  'INV-A2-07',
  'INV-A2-08',
  'INV-A2-09',
  'INV-A2-10',
  'INV-A2-11',
  'INV-A2-12',
  'INV-A2-13',
  'INV-A2-14',
  'INV-A2-15'
];

const EXPECTED_PRECONDITION_IDS = [
  'task-claim',
  'session-confirm',
  'decision-freeze',
  'reservation-held',
  'direct-authorization'
];

const A0_STATUS_CHECKS = [
  'ck_outbound_task_status',
  'ck_inbound_session_status',
  'ck_call_record_status',
  'ck_call_leg_state',
  'ck_call_event_processing_status',
  'ck_usage_reservation_status',
  'ck_recording_status',
  'ck_evidence_status'
];

const A0_REGISTRY_CHECKS = [
  'ck_ivr_decision_action',
  'ck_ivr_decision_purpose',
  'ck_ivr_decision_reason',
  'ck_usage_fact_type',
  'ck_usage_fact_event_domain',
  'ck_idempotency_scope'
];

const A0_ALL_CHECKS = [...A0_STATUS_CHECKS, ...A0_REGISTRY_CHECKS];
const A1_REQUIRED_SCHEMAS = [
  'IvrDecision',
  'IvrInboundResolveRequest',
  'IvrActivationConfirmRequest',
  'IvrDirectAuthorizationRequest',
  'IvrCallEventRequest',
  'CallEventReceipt',
  'RecordingDeclarationRequest',
  'RecordingDeclaration',
  'RecordingCompletionRequest',
  'RecordingCompletion',
  'Problem',
  'ProblemCode'
];
const A1_REQUIRED_FIELDS = {
  IvrInboundResolveRequest: ['callId', 'purpose', 'protocolVersion'],
  IvrActivationConfirmRequest: ['eventId', 'policyCode', 'policyVersion', 'promptVersion', 'contentSha256', 'decision'],
  IvrDirectAuthorizationRequest: ['eventId', 'callee', 'promptVersion', 'confirmationKey', 'confirmationContent'],
  IvrCallEventRequest: ['eventId', 'eventType', 'occurredAt', 'payload'],
  CallEventReceipt: ['callId', 'eventId', 'processingStatus', 'receivedAt'],
  RecordingDeclarationRequest: ['recordingId', 'mimeType', 'sizeBytes', 'durationSeconds', 'sha256'],
  RecordingDeclaration: ['recordingId', 'status', 'uploadExpiresAt', 'upload'],
  RecordingCompletionRequest: [],
  RecordingCompletion: ['recordingId', 'status'],
  Problem: ['type', 'title', 'status', 'code', 'traceId']
};
const A1_OPERATION_CONTRACT = {
  resolveIvrInbound: {
    method: 'post',
    path: '/api/v1/ivr/inbound/resolve',
    requestSchema: 'IvrInboundResolveRequest',
    responseSchema: 'IvrDecision'
  },
  confirmIvrActivation: {
    method: 'post',
    path: '/api/v1/ivr/calls/{callId}/activation-confirmations',
    requestSchema: 'IvrActivationConfirmRequest',
    responseSchema: 'IvrDecision'
  },
  authorizeIvrDirectOutbound: {
    method: 'post',
    path: '/api/v1/ivr/calls/{callId}/outbound-authorizations',
    requestSchema: 'IvrDirectAuthorizationRequest',
    responseSchema: 'IvrDecision'
  },
  ingestIvrCallEvent: {
    method: 'post',
    path: '/api/v1/ivr/calls/{callId}/events',
    requestSchema: 'IvrCallEventRequest',
    responseSchema: 'CallEventReceipt'
  },
  declareIvrRecording: {
    method: 'post',
    path: '/api/v1/ivr/calls/{callId}/recordings',
    requestSchema: 'RecordingDeclarationRequest',
    responseSchema: 'RecordingDeclaration'
  },
  completeIvrRecording: {
    method: 'post',
    path: '/api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete',
    requestSchema: 'RecordingCompletionRequest',
    responseSchema: 'RecordingCompletion'
  }
};

const STAGE_CONFIG = {
  s1: {
    topLevelRequired: [
      'modelVersion',
      'protocolVersion',
      'supportedVersions',
      'messages',
      'eventTypes',
      'identities',
      'actionSelection',
      'decisionFields',
      'actions',
      'purposes',
      'decisionReasons',
      'errors',
      'preconditions',
      'invariants'
    ],
    topLevelForbidden: [
      'eventRegistry',
      'eventEnvelope',
      'endReasons',
      'quarantineReasons',
      'stateMachines',
      'blegOperation',
      'eventOutbox',
      'recordingLifecycle',
      'mediaEvidenceBoundary'
    ],
    forbiddenNestedKeys: [
      'eventRegistry',
      'eventEnvelope',
      'endReasons',
      'quarantineReasons',
      'stateMachines',
      'blegOperation',
      'eventOutbox',
      'recordingLifecycle',
      'mediaEvidenceBoundary',
      'envelopeLegId',
      'payloadFields',
      'normalProcessingOutcome'
    ],
    eventColumnKeys: ['eventType', 'payloadState'],
    eventDocColumns: ['eventType', 'payloadState'],
    messageDocColumns: ['messageId', 'slice', 'operationId', 'methodPath', 'requestSchema', 'responseSchema', 'identity'],
    identityDocColumns: ['identity', 'owner', 'uniqueness', 'a0a1Mapping'],
    eventMapForm: 'identity-only',
    fixtureIds: [
      's1-01-task-path-normal',
      's1-02-direct-path-normal',
      's1-03-activation-redecision',
      's1-04-decision-exact-replay',
      's1-05-decision-semantic-conflict',
      's1-06-version-unsupported'
    ],
    sliceOrder: ['s1']
  },
  s2: {
    topLevelRequired: [
      'modelVersion',
      'protocolVersion',
      'supportedVersions',
      'messages',
      'eventTypes',
      'identities',
      'actionSelection',
      'decisionFields',
      'actions',
      'purposes',
      'decisionReasons',
      'errors',
      'preconditions',
      'invariants',
      'eventRegistry',
      'eventEnvelope',
      'endReasons',
      'quarantineReasons',
      'stateMachines',
      'blegOperation',
      'eventOutbox'
    ],
    topLevelForbidden: ['recordingLifecycle', 'mediaEvidenceBoundary'],
    forbiddenNestedKeys: ['recordingLifecycle', 'mediaEvidenceBoundary', 'payloadState'],
    eventColumnKeys: ['eventType', 'envelopeLegId', 'payloadFields', 'normalProcessingOutcome'],
    eventDocColumns: ['eventType', 'envelopeLegId', 'payloadFields', 'normalProcessingOutcome'],
    messageDocColumns: ['messageId', 'slice', 'operationId', 'methodPath', 'requestSchema', 'responseSchema', 'identity'],
    identityDocColumns: ['identity', 'owner', 'uniqueness', 'a0a1Mapping'],
    eventMapForm: 'full-frozen',
    fixtureIds: [
      's1-01-task-path-normal',
      's1-02-direct-path-normal',
      's1-03-activation-redecision',
      's1-04-decision-exact-replay',
      's1-05-decision-semantic-conflict',
      's1-06-version-unsupported',
      's2-01-task-terminal-completed',
      's2-02-event-duplicate-replay',
      's2-03-event-out-of-order',
      's2-04-terminal-unknown-reconciliation',
      's2-05-response-loss-retry',
      's2-06-ivr-restart-bleg-recovery',
      's2-07-authorization-expiry-not-started',
      's2-08-redial-second-bleg-rejected',
      's2-09-heartbeat-and-terminal',
      's2-10-terminal-contradiction-quarantine',
      's2-11-resolve-never-committed-not-started'
    ],
    sliceOrder: ['s1', 's2']
  },
  s3: {
    topLevelRequired: [
      'modelVersion',
      'protocolVersion',
      'supportedVersions',
      'messages',
      'eventTypes',
      'identities',
      'actionSelection',
      'decisionFields',
      'actions',
      'purposes',
      'decisionReasons',
      'errors',
      'preconditions',
      'invariants',
      'eventRegistry',
      'eventEnvelope',
      'endReasons',
      'quarantineReasons',
      'stateMachines',
      'blegOperation',
      'eventOutbox',
      'recordingLifecycle',
      'mediaEvidenceBoundary'
    ],
    topLevelForbidden: [],
    forbiddenNestedKeys: ['payloadState'],
    eventColumnKeys: ['eventType', 'envelopeLegId', 'payloadFields', 'normalProcessingOutcome'],
    eventDocColumns: ['eventType', 'envelopeLegId', 'payloadFields', 'normalProcessingOutcome'],
    messageDocColumns: ['messageId', 'slice', 'operationId', 'methodPath', 'requestSchema', 'responseSchema', 'identity'],
    identityDocColumns: ['identity', 'owner', 'uniqueness', 'a0a1Mapping'],
    eventMapForm: 'full-frozen',
    fixtureIds: [
      's1-01-task-path-normal',
      's1-02-direct-path-normal',
      's1-03-activation-redecision',
      's1-04-decision-exact-replay',
      's1-05-decision-semantic-conflict',
      's1-06-version-unsupported',
      's2-01-task-terminal-completed',
      's2-02-event-duplicate-replay',
      's2-03-event-out-of-order',
      's2-04-terminal-unknown-reconciliation',
      's2-05-response-loss-retry',
      's2-06-ivr-restart-bleg-recovery',
      's2-07-authorization-expiry-not-started',
      's2-08-redial-second-bleg-rejected',
      's2-09-heartbeat-and-terminal',
      's2-10-terminal-contradiction-quarantine',
      's2-11-resolve-never-committed-not-started',
      's3-01-recording-validated',
      's3-02-declaration-replay',
      's3-03-upload-observation-required',
      's3-04-upload-expired-quarantine',
      's3-05-storage-unknown-retry',
      's3-06-recording-identity-conflict',
      's3-07-post-terminal-conflict-fact',
      's3-08-end-to-end-task-media'
    ],
    sliceOrder: ['s1', 's2', 's3']
  }
};

const NEGATIVE_FIXTURE_IDS = new Set(['s2-08-redial-second-bleg-rejected', 's2-10-terminal-contradiction-quarantine', 's2-11-resolve-never-committed-not-started']);

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') {
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) {
    return '[' + value.map((item) => canonicalJson(item)).join(',') + ']';
  }
  const keys = Object.keys(value).sort();
  return '{' + keys.map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function sha256(value) {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

function stableUuid(seed) {
  const hash = sha256(seed);
  const variant = ((parseInt(hash[16], 16) & 0x3) | 0x8).toString(16);
  return `${hash.slice(0, 8)}-${hash.slice(8, 12)}-5${hash.slice(13, 16)}-${variant}${hash.slice(17, 20)}-${hash.slice(20, 32)}`;
}

function parseArgs(argv) {
  const result = {};
  const allowed = new Set(['--stage', '--model', '--fixtures', '--spec', '--a1-bundle', '--a0']);
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (!allowed.has(token)) {
      throw new Error(`unknown argument: ${token}`);
    }
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) {
      throw new Error(`missing value for ${token}`);
    }
    result[token.slice(2).replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())] = value;
    index += 1;
  }
  for (const key of ['stage', 'model', 'fixtures', 'spec', 'a1Bundle', 'a0']) {
    if (!result[key]) {
      throw new Error(`missing --${key.replace(/[A-Z]/g, (letter) => '-' + letter.toLowerCase())}`);
    }
  }
  if (!STAGE_CONFIG[result.stage]) {
    throw new Error(`unknown stage: ${result.stage}`);
  }
  return result;
}

function readUtf8(filePath) {
  const buffer = fs.readFileSync(filePath);
  if (!isUtf8(buffer)) {
    throw new Error(`file is not valid UTF-8: ${filePath}`);
  }
  return buffer.toString('utf8');
}

function parseCells(line) {
  const trimmed = line.trim();
  if (!trimmed.startsWith('|') || !trimmed.endsWith('|')) {
    return null;
  }
  return trimmed.slice(1, -1).split('|').map((cell) => cell.trim().replace(/^`(.*)`$/, '$1').trim());
}

function isSeparatorRow(cells) {
  return cells.length > 0 && cells.every((cell) => /^:?-{3,}:?$/.test(cell));
}

function extractMarker(doc, name, counters, issues) {
  const startToken = `<!-- ${name}:start -->`;
  const endToken = `<!-- ${name}:end -->`;
  const startCount = doc.split(startToken).length - 1;
  const endCount = doc.split(endToken).length - 1;
  if (startCount !== 1 || endCount !== 1) {
    counters.doc_missing += 1;
    issues.push(`${name}: expected exactly one start and end marker, found ${startCount}/${endCount}`);
    return null;
  }
  const startIndex = doc.indexOf(startToken) + startToken.length;
  const endIndex = doc.indexOf(endToken, startIndex);
  if (endIndex < startIndex) {
    counters.doc_invalid += 1;
    issues.push(`${name}: end marker precedes start marker`);
    return null;
  }
  const raw = doc.slice(startIndex, endIndex);
  const lines = raw.split('\n').map((line) => line.trim()).filter((line) => line !== '');
  if (lines.length < 2) {
    counters.doc_missing += 1;
    issues.push(`${name}: marker block has no table`);
    return null;
  }
  const header = parseCells(lines[0]);
  const separator = parseCells(lines[1]);
  if (!header || !separator || !isSeparatorRow(separator)) {
    counters.doc_invalid += 1;
    issues.push(`${name}: malformed header or separator`);
    return null;
  }
  const rows = [];
  for (const line of lines.slice(2)) {
    const cells = parseCells(line);
    if (!cells) {
      counters.doc_invalid += 1;
      issues.push(`${name}: non-table row inside marker block`);
      continue;
    }
    if (cells.length !== header.length) {
      counters.doc_invalid += 1;
      issues.push(`${name}: row has ${cells.length} cells, expected ${header.length}`);
      continue;
    }
    rows.push(cells);
  }
  return { header, rows, raw };
}

function requireExactKeys(object, keys, label, counters, issues) {
  if (object === null || typeof object !== 'object' || Array.isArray(object)) {
    counters.stage_field_missing += 1;
    issues.push(`${label}: expected an object`);
    return false;
  }
  const actual = Object.keys(object);
  let ok = true;
  for (const key of keys) {
    if (!actual.includes(key)) {
      counters.stage_field_missing += 1;
      issues.push(`${label}: missing field ${key}`);
      ok = false;
    }
  }
  for (const key of actual) {
    if (!keys.includes(key)) {
      counters.stage_field_premature += 1;
      issues.push(`${label}: premature field ${key}`);
      ok = false;
    }
  }
  return ok;
}

function deepEqual(left, right) {
  return canonicalJson(left) === canonicalJson(right);
}

function checkExact(label, actual, expected, counters, issues) {
  if (!deepEqual(actual, expected)) {
    counters.stage_field_missing += 1;
    issues.push(`${label} does not exactly match the frozen S2 registry`);
    return false;
  }
  return true;
}

function sortedTransitionPairs(pairs) {
  return pairs.map(([from, to]) => canonicalJson([from, to])).sort();
}

function validateS2Registries(model, counters, issues) {
  checkExact('model.eventEnvelope', model.eventEnvelope, EXPECTED_EVENT_ENVELOPE, counters, issues);
  checkExact('model.endReasons', model.endReasons, EXPECTED_END_REASONS, counters, issues);
  checkExact('model.quarantineReasons', model.quarantineReasons, EXPECTED_QUARANTINE_REASONS, counters, issues);

  const stateMachines = model.stateMachines;
  if (stateMachines === null || typeof stateMachines !== 'object' || Array.isArray(stateMachines)) {
    counters.stage_field_missing += 1;
    issues.push('model.stateMachines must be an object');
  } else {
    const expectedAggregates = Object.keys(EXPECTED_STATE_MACHINE_STATES);
    const actualAggregates = Object.keys(stateMachines);
    if (!deepEqual([...actualAggregates].sort(), [...expectedAggregates, 'terminalMapping'].sort())) {
      counters.stage_field_missing += 1;
      issues.push('model.stateMachines aggregate set does not match the S2 registry');
    }
    for (const aggregate of expectedAggregates) {
      const machine = stateMachines[aggregate];
      if (machine === null || typeof machine !== 'object' || Array.isArray(machine)) {
        counters.stage_field_missing += 1;
        issues.push(`model.stateMachines.${aggregate} must be an object`);
        continue;
      }
      requireExactKeys(machine, ['states', 'transitions'], `model.stateMachines.${aggregate}`, counters, issues);
      if (!deepEqual([...(machine.states || [])].sort(), [...EXPECTED_STATE_MACHINE_STATES[aggregate]].sort())) {
        counters.stage_field_missing += 1;
        issues.push(`model.stateMachines.${aggregate}.states does not match the A0 state registry`);
      }
      if (!Array.isArray(machine.transitions) || machine.transitions.length === 0) {
        counters.stage_field_missing += 1;
        issues.push(`model.stateMachines.${aggregate}.transitions is empty or not an array`);
        continue;
      }
      for (const [index, transition] of machine.transitions.entries()) {
        requireExactKeys(transition, ['from', 'to', 'trigger', 'guard'], `model.stateMachines.${aggregate}.transitions[${index}]`, counters, issues);
      }
      for (const [from, to] of EXPECTED_STATE_MACHINE_TRANSITIONS[aggregate]) {
        if (!machine.transitions.some((transition) => transition.from === from && transition.to === to)) {
          counters.stage_field_missing += 1;
          issues.push(`model.stateMachines.${aggregate} is missing transition ${from}->${to}`);
        }
      }
      if (!deepEqual(
        sortedTransitionPairs(machine.transitions.map((transition) => [transition.from, transition.to])),
        sortedTransitionPairs(EXPECTED_STATE_MACHINE_TRANSITIONS[aggregate])
      )) {
        counters.stage_field_missing += 1;
        issues.push(`model.stateMachines.${aggregate}.transitions does not exactly match the frozen transition registry`);
      }
    }
    checkExact('model.stateMachines.terminalMapping', stateMachines.terminalMapping, EXPECTED_TERMINAL_MAPPING, counters, issues);
  }

  const bleg = model.blegOperation;
  if (bleg === null || typeof bleg !== 'object' || Array.isArray(bleg)) {
    counters.stage_field_missing += 1;
    issues.push('model.blegOperation must be an object');
  } else {
    requireExactKeys(bleg, ['key', 'fields', 'lineAttemptCountValues', 'phases', 'transitions', 'recoveryRules'], 'model.blegOperation', counters, issues);
    if (bleg.key !== 'callId') {
      counters.stage_field_missing += 1;
      issues.push('model.blegOperation.key must be callId');
    }
    checkExact('model.blegOperation.fields', bleg.fields, EXPECTED_BLEG_FIELDS, counters, issues);
    checkExact('model.blegOperation.lineAttemptCountValues', bleg.lineAttemptCountValues, [0, 1], counters, issues);
    checkExact('model.blegOperation.phases', bleg.phases, EXPECTED_BLEG_PHASES, counters, issues);
    if (!Array.isArray(bleg.transitions) || bleg.transitions.length === 0) {
      counters.stage_field_missing += 1;
      issues.push('model.blegOperation.transitions is empty or not an array');
    } else {
      for (const [index, transition] of bleg.transitions.entries()) {
        requireExactKeys(transition, ['from', 'to', 'guard'], `model.blegOperation.transitions[${index}]`, counters, issues);
      }
      for (const [from, to] of EXPECTED_BLEG_TRANSITIONS) {
        if (!bleg.transitions.some((transition) => transition.from === from && transition.to === to)) {
          counters.stage_field_missing += 1;
          issues.push(`model.blegOperation is missing transition ${from}->${to}`);
        }
      }
      if (!deepEqual(
        sortedTransitionPairs(bleg.transitions.map((transition) => [transition.from, transition.to])),
        sortedTransitionPairs(EXPECTED_BLEG_TRANSITIONS)
      )) {
        counters.stage_field_missing += 1;
        issues.push('model.blegOperation.transitions does not exactly match the frozen transition registry');
      }
    }
    if (!Array.isArray(bleg.recoveryRules) || bleg.recoveryRules.length !== 7) {
      counters.stage_field_missing += 1;
      issues.push('model.blegOperation.recoveryRules must contain the seven recovery decisions');
    }
  }

  checkExact('model.eventOutbox', model.eventOutbox, EXPECTED_EVENT_OUTBOX, counters, issues);

  const computedDigest = sha256(canonicalJson(model.eventTypes));
  if (!deepEqual(model.eventRegistry, { frozenAt: 's2', sha256: computedDigest })) {
    counters.stage_field_missing += 1;
    issues.push('model.eventRegistry must be exactly {frozenAt:s2, sha256:<canonical eventTypes digest>}');
  }
}

function validateS3Registries(model, a0Checks, counters, issues) {
  checkExact('model.recordingLifecycle', model.recordingLifecycle, EXPECTED_RECORDING_LIFECYCLE, counters, issues);
  checkExact('model.mediaEvidenceBoundary', model.mediaEvidenceBoundary, EXPECTED_MEDIA_EVIDENCE_BOUNDARY, counters, issues);

  const lifecycle = model.recordingLifecycle;
  if (lifecycle && typeof lifecycle === 'object' && !Array.isArray(lifecycle)) {
    if (!Array.isArray(lifecycle.states) || !setEqual(lifecycle.states, EXPECTED_RECORDING_LIFECYCLE.states)) {
      counters.stage_field_missing += 1;
      issues.push('model.recordingLifecycle.states does not exactly match the four approved recording states');
    }
    if (a0Checks.has('ck_recording_status') && !setEqual(lifecycle.states || [], a0Checks.get('ck_recording_status'))) {
      counters.a0_status_drift += 1;
      issues.push('model.recordingLifecycle.states does not equal A0 ck_recording_status');
    }
    const actualPairs = Array.isArray(lifecycle.transitions)
      ? lifecycle.transitions.map((transition) => [transition?.from ?? null, transition?.to ?? null])
      : [];
    const expectedPairs = EXPECTED_RECORDING_LIFECYCLE.transitions.map((transition) => [transition.from, transition.to]);
    if (!deepEqual(sortedTransitionPairs(actualPairs), sortedTransitionPairs(expectedPairs))) {
      counters.stage_field_missing += 1;
      issues.push('model.recordingLifecycle.transitions does not exactly match the approved recording edges');
    }
    for (const [index, transition] of (Array.isArray(lifecycle.transitions) ? lifecycle.transitions : []).entries()) {
      requireExactKeys(transition, ['from', 'to', 'trigger', 'guard'], `model.recordingLifecycle.transitions[${index}]`, counters, issues);
    }
  }
  if (model.mediaEvidenceBoundary && typeof model.mediaEvidenceBoundary === 'object') {
    if (model.mediaEvidenceBoundary.completionCreatesEvidence !== false) {
      counters.stage_field_missing += 1;
      issues.push('model.mediaEvidenceBoundary.completionCreatesEvidence must be false');
    }
    if (model.mediaEvidenceBoundary.completionSolidifiesEvidence !== false) {
      counters.stage_field_missing += 1;
      issues.push('model.mediaEvidenceBoundary.completionSolidifiesEvidence must be false');
    }
  }
}

function subsetEqual(expected, actual) {
  if (expected === null || typeof expected !== 'object') {
    return expected === actual;
  }
  if (Array.isArray(expected)) {
    if (!Array.isArray(actual) || actual.length !== expected.length) {
      return false;
    }
    return expected.every((item, index) => subsetEqual(item, actual[index]));
  }
  if (actual === null || typeof actual !== 'object' || Array.isArray(actual)) {
    return false;
  }
  return Object.keys(expected).every((key) => Object.prototype.hasOwnProperty.call(actual, key) && subsetEqual(expected[key], actual[key]));
}

function setEqual(left, right) {
  const a = [...new Set(left)].sort();
  const b = [...new Set(right)].sort();
  return deepEqual(a, b);
}

function walkKeys(value, callback) {
  if (value === null || typeof value !== 'object') {
    return;
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      walkKeys(item, callback);
    }
    return;
  }
  for (const [key, child] of Object.entries(value)) {
    callback(key);
    walkKeys(child, callback);
  }
}

function collectStrings(value, output) {
  if (typeof value === 'string') {
    output.push(value);
    return;
  }
  if (value === null || typeof value !== 'object') {
    return;
  }
  if (Array.isArray(value)) {
    for (const item of value) {
      collectStrings(item, output);
    }
    return;
  }
  for (const child of Object.values(value)) {
    collectStrings(child, output);
  }
}

function schemaName(schema) {
  if (!schema || typeof schema !== 'object') {
    return null;
  }
  if (typeof schema.$ref === 'string' && schema.$ref.startsWith('#/components/schemas/')) {
    return schema.$ref.slice('#/components/schemas/'.length);
  }
  return null;
}

function requestSchemaName(operation) {
  return schemaName(operation?.requestBody?.content?.['application/json']?.schema);
}

function successResponse(operation) {
  for (const status of ['200', '201']) {
    if (operation?.responses?.[status]) {
      return { status, response: operation.responses[status] };
    }
  }
  return null;
}

function responseSchemaName(operation) {
  const success = successResponse(operation);
  if (!success) {
    return null;
  }
  const content = success.response?.content || {};
  if (content['application/json']) {
    return schemaName(content['application/json'].schema);
  }
  return null;
}

function parseA0Catalog(doc) {
  const match = doc.match(/<!-- catalog:start -->([\s\S]*?)<!-- catalog:end -->/);
  if (!match) {
    return [];
  }
  const tables = [];
  for (const line of match[1].split('\n')) {
    const row = line.match(/^\| `([a-z_]+\.[a-z_]+)` \|/);
    if (row) {
      tables.push(row[1]);
    }
  }
  return tables;
}

function parseA0Columns(doc) {
  const lines = doc.split('\n');
  const result = new Map();
  let current = null;
  let collecting = false;
  for (const line of lines) {
    const heading = line.match(/^### 4\.\d+(?:\.\d+)? ([a-z_]+\.[a-z_]+)$/);
    if (heading) {
      current = heading[1];
      result.set(current, new Set());
      collecting = false;
      continue;
    }
    if (/^### /.test(line)) {
      current = null;
      collecting = false;
      continue;
    }
    if (!current) {
      continue;
    }
    if (/^\| Column \|/.test(line)) {
      collecting = true;
      continue;
    }
    if (!collecting) {
      continue;
    }
    if (line.startsWith('|')) {
      const cells = parseCells(line);
      if (cells && cells.length >= 1 && !isSeparatorRow(cells)) {
        const column = cells[0].replace(/`/g, '').trim();
        if (column !== 'Column') {
          result.get(current).add(column);
        }
      }
    } else {
      collecting = false;
    }
  }
  return result;
}

function parseA0Checks(doc) {
  const checks = new Map();
  for (const line of doc.split('\n')) {
    const match = line.match(/^- `([^`]+)`: `(.+)`\.$/);
    if (!match) {
      continue;
    }
    const values = [...match[2].matchAll(/'([A-Za-z0-9_]+)'/g)].map((item) => item[1]);
    checks.set(match[1], values);
  }
  return checks;
}

function parseA0DecisionFields(doc) {
  const match = doc.match(/### 10\.14 [^\n]*\n([\s\S]*?)### 10\.15 /);
  if (!match) {
    return [];
  }
  const fields = [];
  for (const line of match[1].split('\n')) {
    const row = line.match(/^\| `([^`]+)` \|/);
    if (row) {
      fields.push(row[1]);
    }
  }
  return fields;
}

const TERMINAL_CALL_STATUSES = new Set(['ENDED', 'FAILED', 'REJECTED', 'UNKNOWN']);
const ENDED_CLASS_REASONS = new Set(['CALLER_HANGUP_AFTER_BRIDGE', 'CALLEE_HANGUP_AFTER_BRIDGE', 'RECORDING_LIMIT_REACHED']);
const OUTBOX_PERMANENT_HTTP = new Set([400, 404, 409, 422]);

function validateEventPayload(eventType, payload) {
  const definition = EVENT_PAYLOAD_DEFINITIONS[eventType];
  if (!definition) {
    return 'unknown event type';
  }
  if (payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
    return 'payload must be an object';
  }
  const allowed = new Set([...definition.required, ...definition.optional]);
  for (const key of Object.keys(payload)) {
    if (!allowed.has(key)) {
      return `unknown payload field ${key}`;
    }
  }
  for (const key of definition.required) {
    if (!Object.prototype.hasOwnProperty.call(payload, key)) {
      return `missing payload field ${key}`;
    }
  }
  if (eventType === 'CALL_TERMINATION_UNKNOWN' && payload.reasonCode !== 'LINE_OUTCOME_UNKNOWN') {
    return 'CALL_TERMINATION_UNKNOWN reasonCode must be LINE_OUTCOME_UNKNOWN';
  }
  if (eventType === 'OUTBOUND_NOT_STARTED' && payload.reasonCode !== 'AUTHORIZATION_EXPIRED') {
    return 'OUTBOUND_NOT_STARTED reasonCode must be AUTHORIZATION_EXPIRED';
  }
  if (eventType === 'CALL_TERMINATED' || eventType === 'CALL_TERMINAL_RECONCILED') {
    if (!EXPECTED_END_REASONS.includes(payload.endReason)) {
      return 'endReason is not registered';
    }
    if (!['ENDED', 'FAILED'].includes(payload.callerLegOutcome)) {
      return 'callerLegOutcome must be ENDED or FAILED';
    }
    if (payload.calleeLegOutcome !== undefined && !['ENDED', 'FAILED'].includes(payload.calleeLegOutcome)) {
      return 'calleeLegOutcome must be ENDED or FAILED';
    }
    if (!Number.isInteger(payload.authoritativeRecordingSeconds) || payload.authoritativeRecordingSeconds < 0) {
      return 'authoritativeRecordingSeconds must be a non-negative integer';
    }
    for (const key of ['callerLegAnsweredAt', 'calleeLegStartedAt', 'calleeLegAnsweredAt', 'bridgedAt', 'recordingStartedAt', 'recordingEndedAt']) {
      if (payload[key] !== undefined && typeof payload[key] !== 'string') {
        return `${key} must be a date-time string`;
      }
    }
    if (eventType === 'CALL_TERMINAL_RECONCILED' && typeof payload.resolvesUnknownEventId !== 'string') {
      return 'resolvesUnknownEventId is required';
    }
  }
  if (eventType === 'BRIDGE_STARTED' && (typeof payload.callerLegId !== 'string' || typeof payload.calleeLegId !== 'string')) {
    return 'bridge leg ids must be strings';
  }
  return null;
}

function terminalLegOutcomes(endReason) {
  if (ENDED_CLASS_REASONS.has(endReason)) {
    return { caller: 'ENDED', callee: 'ENDED' };
  }
  if (endReason === 'BRIDGE_FAILED') {
    return { caller: 'ENDED', callee: 'ENDED' };
  }
  if (endReason === 'CALLER_HANGUP_BEFORE_BRIDGE') {
    return { caller: 'ENDED', callee: 'FAILED' };
  }
  return { caller: 'ENDED', callee: 'FAILED' };
}

class ProtocolMachine {
  constructor() {
    this.calls = new Map();
    this.eventReceipts = new Map();
    this.outbox = new Map();
    this.sequenceIndex = new Map();
    this.recordings = new Map();
    this.recordingOutbox = new Map();
    this.recordingConflicts = new Map();
    this.activeDelivery = {};
    this.counts = {
      callsCreated: 0,
      sessionsCreated: 0,
      tasksClaimed: 0,
      reservationsCreated: 0,
      decisionsFrozen: 0,
      trialsGranted: 0,
      activationConfirmations: 0,
      directConfirmations: 0,
      loginAccountsCreated: 0,
      realNameProfilesCreated: 0,
      purchaseOrdersCreated: 0,
      operatorIntegrations: 0
    };
    this.executionCounts = {
      eventsReceived: 0,
      eventsApplied: 0,
      eventsIgnored: 0,
      eventsQuarantined: 0,
      eventsConflicted: 0,
      eventsReplayed: 0,
      blegOperationsCreated: 0,
      lineAttempts: 0,
      redialAttempts: 0,
      terminalFactsAccepted: 0,
      unknownFacts: 0,
      settlements: 0,
      releases: 0,
      reconciling: 0,
      reconciliations: 0,
      usageFacts: 0,
      outboxEntries: 0,
      outboxAcked: 0,
      outboxPermanent: 0,
      outboxRetried: 0,
      recordingsCreated: 0,
      recordingDeclarations: 0,
      recordingDeclarationReplays: 0,
      uploadObservations: 0,
      uploadObservationReplays: 0,
      mediaObjectsCreated: 0,
      recordingValidations: 0,
      recordingQuarantines: 0,
      recordingCompletionReplays: 0,
      recordingConflictFacts: 0,
      recordingOutboxEntries: 0,
      recordingOutboxAcked: 0,
      recordingOutboxPermanent: 0,
      recordingOutboxRetried: 0,
      evidenceRecordsCreated: 0,
      evidenceSolidified: 0
    };
  }

  countSnapshot() {
    return { ...this.counts };
  }

  executionCountSnapshot() {
    return { ...this.counts, ...this.executionCounts };
  }

  projectExecution(callId) {
    const call = this.calls.get(callId);
    const outbox = [...this.outbox.values()]
      .filter((entry) => entry.callId === callId)
      .sort((left, right) => (left.eventId < right.eventId ? -1 : left.eventId > right.eventId ? 1 : 0))
      .map((entry) => ({ eventId: entry.eventId, eventType: entry.eventType, status: entry.status }));
    if (!call) {
      return { call: null, bleg: null, outbox };
    }
    const legs = [...call.legs.values()]
      .sort((left, right) => (left.legId < right.legId ? -1 : left.legId > right.legId ? 1 : 0))
      .map((leg) => ({
        legId: leg.legId,
        role: leg.role,
        state: leg.state,
        answeredAt: leg.answeredAt,
        endedAt: leg.endedAt,
        endReason: leg.endReason
      }));
    return {
      call: {
        exists: true,
        status: call.status,
        sessionStatus: call.sessionStatus,
        task: call.task ? { taskId: call.task.taskId, status: call.task.status } : null,
        reservation: call.reservation ? { reservationId: call.reservation.reservationId, status: call.reservation.status } : null,
        endedAt: call.endedAt,
        endReason: call.endReason,
        authoritativeSeconds: call.authoritativeSeconds,
        bridgedAt: call.bridgedAt,
        recordingStartedAt: call.recordingStartedAt,
        recordingEndedAt: call.recordingEndedAt,
        unknownEventId: call.unknownEventId,
        reconciliationEventId: call.reconciliationEventId,
        terminalEventId: call.terminalEventId,
        legs
      },
      bleg: call.bleg ? {
        decisionId: call.bleg.decisionId,
        reservationId: call.bleg.reservationId,
        authorizationExpiresAt: call.bleg.authorizationExpiresAt,
        lineAttemptCount: call.bleg.lineAttemptCount,
        phase: call.bleg.phase,
        dispatchMarker: call.bleg.dispatchMarker,
        terminalEventId: call.bleg.terminalEventId
      } : null,
      outbox,
      recordings: this.projectRecordings(callId),
      recordingOutbox: [...this.recordingOutbox.values()]
        .filter((entry) => entry.callId === callId)
        .sort((left, right) => (left.key < right.key ? -1 : left.key > right.key ? 1 : 0))
        .map((entry) => ({ kind: entry.kind, recordingId: entry.recordingId, status: entry.status })),
      recordingConflictOutbox: [...this.recordingConflicts.values()].filter((fact) => fact.callId === callId).map((fact) => ({
        outboxId: fact.outboxId,
        dedupeKey: fact.dedupeKey,
        eventDomain: 'EVIDENCE',
        aggregateType: 'RECORDING',
        eventType: 'RECORDING_CONFLICT',
        payload: fact.payload
      }))
    };
  }

  projectRecordings(callId) {
    return [...this.recordings.values()]
      .filter((recording) => recording.callId === callId)
      .sort((left, right) => (left.recordingId < right.recordingId ? -1 : left.recordingId > right.recordingId ? 1 : 0))
      .map((recording) => ({
        recordingId: recording.recordingId,
        status: recording.status,
        revision: recording.revision,
        declared: { ...recording.declared },
        uploadExpiresAt: recording.uploadExpiresAt,
        mediaObject: recording.mediaObject ? { ...recording.mediaObject } : null,
        validated: recording.validated ? { ...recording.validated } : null,
        terminalFacts: recording.terminalFacts ? { ...recording.terminalFacts } : null,
        completedAt: recording.terminalResponse?.completedAt ?? null,
        evidenceCreated: false,
        evidenceSolidified: false
      }));
  }

  eventFingerprint(callId, body) {
    return sha256(canonicalJson({
      callId,
      eventId: body.eventId,
      eventType: body.eventType,
      legId: body.legId ?? null,
      sequenceNo: body.sequenceNo ?? null,
      occurredAt: body.occurredAt,
      payload: body.payload
    }));
  }

  touchOutbox(callId, body) {
    const eventId = body.eventId;
    let entry = this.outbox.get(eventId);
    if (!entry) {
      entry = {
        eventId,
        callId,
        eventType: body.eventType,
        status: 'PENDING',
        fingerprint: this.eventFingerprint(callId, body)
      };
      this.outbox.set(eventId, entry);
      this.executionCounts.outboxEntries += 1;
    } else {
      this.executionCounts.outboxRetried += 1;
    }
    return entry;
  }

  finishOutbox(entry, httpStatus) {
    if (this.activeDelivery?.responseLost === true) {
      entry.status = 'SENT';
      return;
    }
    if (httpStatus >= 200 && httpStatus < 300) {
      if (entry.status !== 'ACKED') {
        entry.status = 'ACKED';
        this.executionCounts.outboxAcked += 1;
      }
      return;
    }
    if (OUTBOX_PERMANENT_HTTP.has(httpStatus)) {
      if (entry.status !== 'PERMANENT_FAILURE') {
        entry.status = 'PERMANENT_FAILURE';
        this.executionCounts.outboxPermanent += 1;
      }
      return;
    }
    entry.status = 'SENT';
  }

  advanceBleg(call, phases, nextPhase) {
    if (call.bleg && phases.includes(call.bleg.phase)) {
      call.bleg.phase = nextPhase;
    }
  }

  handleEvent(callId, body, server = {}) {
    const outboxEntry = this.touchOutbox(callId, body);
    if (typeof body.eventId !== 'string' || body.eventId.length < 1 || body.eventId.length > 128) {
      this.finishOutbox(outboxEntry, 422);
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (typeof body.eventType !== 'string' || body.eventType.length < 1 || body.eventType.length > 64) {
      this.finishOutbox(outboxEntry, 422);
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (typeof body.occurredAt !== 'string') {
      this.finishOutbox(outboxEntry, 422);
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (body.sequenceNo !== undefined && (!Number.isInteger(body.sequenceNo) || body.sequenceNo < 1)) {
      this.finishOutbox(outboxEntry, 422);
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    const row = EXPECTED_EVENT_ROWS.find((item) => item.eventType === body.eventType);
    if (!row) {
      this.finishOutbox(outboxEntry, 400);
      return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
    }
    if (row.envelopeLegId === 'REQUIRED') {
      if (typeof body.legId !== 'string' || body.legId.length < 1 || body.legId.length > 64) {
        this.finishOutbox(outboxEntry, 400);
        return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
      }
    } else if (body.legId !== undefined) {
      this.finishOutbox(outboxEntry, 400);
      return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
    }
    if (validateEventPayload(body.eventType, body.payload)) {
      this.finishOutbox(outboxEntry, 400);
      return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
    }
    const fingerprint = this.eventFingerprint(callId, body);
    const existing = this.eventReceipts.get(body.eventId);
    if (existing) {
      if (existing.callId !== callId || existing.fingerprint !== fingerprint) {
        this.executionCounts.eventsConflicted += 1;
        this.finishOutbox(outboxEntry, 409);
        return { status: 409, problem: { code: 'EVENT_ID_CONFLICT' } };
      }
      this.executionCounts.eventsReplayed += 1;
      this.finishOutbox(outboxEntry, 200);
      return { status: 200, response: existing.response };
    }
    const call = this.calls.get(callId);
    if (!call) {
      this.finishOutbox(outboxEntry, 404);
      return { status: 404, problem: { code: 'CALL_UNKNOWN' } };
    }
    let sequenceConflict = false;
    if (body.sequenceNo !== undefined) {
      const holder = this.sequenceIndex.get(`${callId}:${body.sequenceNo}`);
      if (holder !== undefined && holder !== body.eventId) {
        sequenceConflict = true;
      }
    }
    const outcome = this.processEvent(call, body, sequenceConflict);
    if (outcome.argument === true) {
      this.finishOutbox(outboxEntry, 400);
      return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
    }
    const receipt = {
      callId,
      eventId: body.eventId,
      processingStatus: outcome.status,
      receivedAt: server.receivedAt || body.occurredAt
    };
    if (outcome.reasonCode) {
      receipt.reasonCode = outcome.reasonCode;
    }
    this.eventReceipts.set(body.eventId, { callId, fingerprint, response: receipt });
    if (body.sequenceNo !== undefined && outcome.status !== 'QUARANTINED') {
      this.sequenceIndex.set(`${callId}:${body.sequenceNo}`, body.eventId);
    }
    this.executionCounts.eventsReceived += 1;
    if (outcome.status === 'APPLIED') {
      this.executionCounts.eventsApplied += 1;
    } else if (outcome.status === 'IGNORED') {
      this.executionCounts.eventsIgnored += 1;
    } else {
      this.executionCounts.eventsQuarantined += 1;
    }
    this.finishOutbox(outboxEntry, 200);
    this.assertExecutionInvariants();
    return { status: 200, response: receipt };
  }

  processEvent(call, body, sequenceConflict) {
    if (sequenceConflict) {
      return { status: 'QUARANTINED', reasonCode: 'SEQUENCE_CONFLICT' };
    }
    switch (body.eventType) {
      case 'CALLER_LEG_ANSWERED':
        return this.applyLegEvent(call, body, 'CALLER');
      case 'CALLEE_LEG_RINGING':
        return this.applyLegEvent(call, body, 'CALLEE_RINGING');
      case 'CALLEE_LEG_ANSWERED':
        return this.applyLegEvent(call, body, 'CALLEE_ANSWERED');
      case 'BRIDGE_STARTED':
        return this.applyBridgeEvent(call, body);
      case 'RECORDING_STARTED':
        return this.applyRecordingStarted(call, body);
      case 'RECORDING_ENDED':
        return this.applyRecordingEnded(call, body);
      case 'CALL_TERMINATED':
        return this.applyTerminalEvent(call, body, false);
      case 'CALL_TERMINATION_UNKNOWN':
        return this.applyTerminationUnknown(call, body);
      case 'CALL_TERMINAL_RECONCILED':
        return this.applyTerminalEvent(call, body, true);
      case 'OUTBOUND_NOT_STARTED':
        return this.applyOutboundNotStarted(call, body);
      case 'CALL_HEARTBEAT':
        return { status: 'IGNORED', reasonCode: 'HEARTBEAT_IGNORED' };
      default:
        return { argument: true };
    }
  }

  applyLegEvent(call, body, kind) {
    const role = kind === 'CALLER' ? 'CALLER' : 'CALLEE';
    const legId = body.legId;
    const occurredAt = body.occurredAt;
    const existing = call.legs.get(legId);
    if (TERMINAL_CALL_STATUSES.has(call.status)) {
      const consistent =
        (kind === 'CALLER' && existing && existing.answeredAt === occurredAt) ||
        (kind === 'CALLEE_RINGING' && existing && existing.startedAt === occurredAt) ||
        (kind === 'CALLEE_ANSWERED' && existing && existing.answeredAt === occurredAt);
      return consistent
        ? { status: 'IGNORED', reasonCode: 'ALREADY_TERMINAL' }
        : { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    const sameRole = [...call.legs.values()].find((leg) => leg.role === role && leg.legId !== legId);
    if (sameRole) {
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (kind === 'CALLER') {
      if (!existing) {
        call.legs.set(legId, {
          legId, role, state: 'ANSWERED', startedAt: occurredAt, answeredAt: occurredAt, endedAt: null, endReason: null
        });
        return { status: 'APPLIED' };
      }
      if (existing.role !== 'CALLER' || existing.answeredAt !== occurredAt) {
        return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
      }
      return { status: 'IGNORED' };
    }
    if (kind === 'CALLEE_RINGING') {
      if (!existing) {
        call.legs.set(legId, {
          legId, role, state: 'RINGING', startedAt: occurredAt, answeredAt: null, endedAt: null, endReason: null
        });
        this.advanceBleg(call, ['DIALING'], 'RINGING');
        return { status: 'APPLIED' };
      }
      if (existing.role !== 'CALLEE') {
        return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
      }
      if (existing.state === 'RINGING' && existing.startedAt === occurredAt) {
        return { status: 'IGNORED' };
      }
      if (existing.state === 'ANSWERED' && occurredAt <= existing.answeredAt) {
        return { status: 'IGNORED' };
      }
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (!existing) {
      call.legs.set(legId, {
        legId, role, state: 'ANSWERED', startedAt: occurredAt, answeredAt: occurredAt, endedAt: null, endReason: null
      });
      this.advanceBleg(call, ['DIALING', 'RINGING'], 'ANSWERED');
      return { status: 'APPLIED' };
    }
    if (existing.role !== 'CALLEE') {
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (existing.state === 'RINGING') {
      if (occurredAt < existing.startedAt) {
        return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
      }
      existing.state = 'ANSWERED';
      existing.answeredAt = occurredAt;
      this.advanceBleg(call, ['DIALING', 'RINGING'], 'ANSWERED');
      return { status: 'APPLIED' };
    }
    if (existing.state === 'ANSWERED' && existing.answeredAt === occurredAt) {
      return { status: 'IGNORED' };
    }
    return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
  }

  applyBridgeEvent(call, body) {
    const { callerLegId, calleeLegId } = body.payload;
    const occurredAt = body.occurredAt;
    const callerLeg = call.legs.get(callerLegId);
    const calleeLeg = call.legs.get(calleeLegId);
    if (TERMINAL_CALL_STATUSES.has(call.status)) {
      return call.bridgedAt === occurredAt
        ? { status: 'IGNORED', reasonCode: 'ALREADY_TERMINAL' }
        : { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    if (!callerLeg || !calleeLeg) {
      return { status: 'QUARANTINED', reasonCode: 'LEG_UNKNOWN' };
    }
    if (callerLeg.role !== 'CALLER' || calleeLeg.role !== 'CALLEE') {
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (callerLeg.state !== 'ANSWERED' || calleeLeg.state !== 'ANSWERED') {
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (occurredAt < callerLeg.answeredAt || occurredAt < calleeLeg.answeredAt) {
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (call.bridgedAt !== null) {
      return call.bridgedAt === occurredAt
        ? { status: 'IGNORED' }
        : { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    call.bridgedAt = occurredAt;
    this.advanceBleg(call, ['ANSWERED'], 'BRIDGED');
    return { status: 'APPLIED' };
  }

  applyRecordingStarted(call, body) {
    const occurredAt = body.occurredAt;
    if (TERMINAL_CALL_STATUSES.has(call.status)) {
      return call.recordingStartedAt === occurredAt
        ? { status: 'IGNORED', reasonCode: 'ALREADY_TERMINAL' }
        : { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    if (call.bridgedAt === null || occurredAt < call.bridgedAt) {
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (call.recordingStartedAt !== null) {
      return call.recordingStartedAt === occurredAt
        ? { status: 'IGNORED' }
        : { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    call.recordingStartedAt = occurredAt;
    this.advanceBleg(call, ['BRIDGED'], 'RECORDING');
    return { status: 'APPLIED' };
  }

  applyRecordingEnded(call, body) {
    const occurredAt = body.occurredAt;
    if (TERMINAL_CALL_STATUSES.has(call.status)) {
      return call.recordingEndedAt === occurredAt
        ? { status: 'IGNORED', reasonCode: 'ALREADY_TERMINAL' }
        : { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    if (call.recordingStartedAt === null || occurredAt < call.recordingStartedAt) {
      return { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (call.recordingEndedAt !== null) {
      return call.recordingEndedAt === occurredAt
        ? { status: 'IGNORED' }
        : { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    call.recordingEndedAt = occurredAt;
    return { status: 'APPLIED' };
  }

  terminalFactObject(payload, occurredAt) {
    return {
      occurredAt,
      callerLegId: payload.callerLegId,
      callerLegOutcome: payload.callerLegOutcome,
      calleeLegId: payload.calleeLegId ?? null,
      calleeLegOutcome: payload.calleeLegOutcome ?? null,
      endReason: payload.endReason,
      authoritativeRecordingSeconds: payload.authoritativeRecordingSeconds,
      callerLegAnsweredAt: payload.callerLegAnsweredAt ?? null,
      calleeLegStartedAt: payload.calleeLegStartedAt ?? null,
      calleeLegAnsweredAt: payload.calleeLegAnsweredAt ?? null,
      bridgedAt: payload.bridgedAt ?? null,
      recordingStartedAt: payload.recordingStartedAt ?? null,
      recordingEndedAt: payload.recordingEndedAt ?? null
    };
  }

  validateTerminalFacts(call, payload, occurredAt) {
    const outcomes = terminalLegOutcomes(payload.endReason);
    if (payload.callerLegOutcome !== outcomes.caller) {
      return 'CONTRADICTORY_FACT';
    }
    if (payload.calleeLegOutcome !== undefined && payload.calleeLegOutcome !== outcomes.callee) {
      return 'CONTRADICTORY_FACT';
    }
    const callerExisting = call.legs.get(payload.callerLegId);
    if (callerExisting) {
      if (callerExisting.role !== 'CALLER') {
        return 'CONTRADICTORY_FACT';
      }
      if (payload.callerLegAnsweredAt !== undefined && callerExisting.answeredAt !== payload.callerLegAnsweredAt) {
        return 'CONTRADICTORY_FACT';
      }
      if (!callerExisting.answeredAt) {
        return 'CONTRADICTORY_FACT';
      }
    } else if (payload.callerLegAnsweredAt === undefined) {
      return 'CONTRADICTORY_FACT';
    }
    const otherCaller = [...call.legs.values()].find((leg) => leg.role === 'CALLER' && leg.legId !== payload.callerLegId);
    if (otherCaller) {
      return 'CONTRADICTORY_FACT';
    }
    const callerAnsweredAt = callerExisting ? callerExisting.answeredAt : payload.callerLegAnsweredAt;
    const existingCallee = [...call.legs.values()].find((leg) => leg.role === 'CALLEE');
    if (existingCallee && payload.calleeLegId !== existingCallee.legId) {
      return 'CONTRADICTORY_FACT';
    }
    let calleeStartedAt = null;
    let calleeAnsweredAt = null;
    if (payload.calleeLegId !== undefined) {
      const calleeExisting = call.legs.get(payload.calleeLegId);
      if (calleeExisting) {
        if (calleeExisting.role !== 'CALLEE') {
          return 'CONTRADICTORY_FACT';
        }
        if (payload.calleeLegStartedAt !== undefined && calleeExisting.startedAt !== payload.calleeLegStartedAt) {
          return 'CONTRADICTORY_FACT';
        }
        if (payload.calleeLegAnsweredAt !== undefined && calleeExisting.answeredAt !== payload.calleeLegAnsweredAt) {
          return 'CONTRADICTORY_FACT';
        }
        calleeStartedAt = calleeExisting.startedAt;
        calleeAnsweredAt = calleeExisting.answeredAt;
      } else {
        if (payload.calleeLegStartedAt === undefined || payload.calleeLegAnsweredAt === undefined) {
          return 'CONTRADICTORY_FACT';
        }
        calleeStartedAt = payload.calleeLegStartedAt;
        calleeAnsweredAt = payload.calleeLegAnsweredAt;
      }
      if (payload.calleeLegOutcome === undefined) {
        return 'CONTRADICTORY_FACT';
      }
      if (calleeAnsweredAt === null) {
        const unansweredFailure = payload.calleeLegOutcome === 'FAILED' && payload.calleeLegAnsweredAt === undefined;
        if (!unansweredFailure) {
          return 'CONTRADICTORY_FACT';
        }
      } else if (calleeStartedAt > calleeAnsweredAt) {
        return 'CONTRADICTORY_FACT';
      }
    } else if (payload.calleeLegStartedAt !== undefined || payload.calleeLegAnsweredAt !== undefined || payload.calleeLegOutcome !== undefined) {
      return 'CONTRADICTORY_FACT';
    }
    let bridgedAt = call.bridgedAt;
    if (payload.bridgedAt !== undefined) {
      if (call.bridgedAt !== null && call.bridgedAt !== payload.bridgedAt) {
        return 'CONTRADICTORY_FACT';
      }
      if (payload.bridgedAt < callerAnsweredAt || payload.bridgedAt > occurredAt) {
        return 'CONTRADICTORY_FACT';
      }
      bridgedAt = payload.bridgedAt;
    }
    let recordingStartedAt = call.recordingStartedAt;
    if (payload.recordingStartedAt !== undefined) {
      if (call.recordingStartedAt !== null && call.recordingStartedAt !== payload.recordingStartedAt) {
        return 'CONTRADICTORY_FACT';
      }
      if (bridgedAt === null || payload.recordingStartedAt < bridgedAt || payload.recordingStartedAt > occurredAt) {
        return 'CONTRADICTORY_FACT';
      }
      recordingStartedAt = payload.recordingStartedAt;
    }
    let recordingEndedAt = call.recordingEndedAt;
    if (payload.recordingEndedAt !== undefined) {
      if (call.recordingEndedAt !== null && call.recordingEndedAt !== payload.recordingEndedAt) {
        return 'CONTRADICTORY_FACT';
      }
      if (recordingStartedAt === null || payload.recordingEndedAt < recordingStartedAt || payload.recordingEndedAt > occurredAt) {
        return 'CONTRADICTORY_FACT';
      }
      recordingEndedAt = payload.recordingEndedAt;
    }
    if (recordingEndedAt !== null && recordingStartedAt === null) {
      return 'CONTRADICTORY_FACT';
    }
    if (payload.authoritativeRecordingSeconds > 0 && recordingStartedAt === null) {
      return 'CONTRADICTORY_FACT';
    }
    if (bridgedAt !== null && callerAnsweredAt > bridgedAt) {
      return 'CONTRADICTORY_FACT';
    }
    return null;
  }

  settleReservation(call, seconds) {
    if (!call.reservation) {
      return;
    }
    const authorized = call.reservation.authorizedRecordingSeconds;
    if (seconds === 0) {
      if (call.reservation.status !== 'RELEASED') {
        call.reservation.status = 'RELEASED';
        call.reservationTerminalCount += 1;
        this.executionCounts.releases += 1;
      }
    } else if (authorized !== null && seconds <= authorized) {
      if (call.reservation.status !== 'SETTLED') {
        call.reservation.status = 'SETTLED';
        call.reservationTerminalCount += 1;
        this.executionCounts.settlements += 1;
        call.usageFactCount += 1;
        if (call.usageFactCount > 1) {
          throw new Error('more than one usage fact for a call');
        }
        this.executionCounts.usageFacts += 1;
      }
    } else if (call.reservation.status !== 'RECONCILING') {
      call.reservation.status = 'RECONCILING';
      this.executionCounts.reconciling += 1;
    }
    if (call.reservationTerminalCount > 1) {
      throw new Error('more than one reservation terminal transition');
    }
  }

  applyTerminalFacts(call, body, reconciled) {
    const payload = body.payload;
    const occurredAt = body.occurredAt;
    const mapping = EXPECTED_TERMINAL_MAPPING[payload.endReason];
    call.status = mapping.callStatus;
    call.endedAt = occurredAt;
    call.endReason = payload.endReason;
    call.authoritativeSeconds = payload.authoritativeRecordingSeconds;
    call.terminalEventId = body.eventId;
    call.terminalAccepted = true;
    call.terminalFacts = this.terminalFactObject(payload, occurredAt);
    if (reconciled) {
      call.reconciliationEventId = body.eventId;
      this.executionCounts.reconciliations += 1;
    }
    const callerExisting = call.legs.get(payload.callerLegId);
    if (callerExisting) {
      callerExisting.state = payload.callerLegOutcome;
      callerExisting.endedAt = occurredAt;
      callerExisting.endReason = payload.endReason;
    } else {
      call.legs.set(payload.callerLegId, {
        legId: payload.callerLegId,
        role: 'CALLER',
        state: payload.callerLegOutcome,
        startedAt: payload.callerLegAnsweredAt,
        answeredAt: payload.callerLegAnsweredAt,
        endedAt: occurredAt,
        endReason: payload.endReason
      });
    }
    if (payload.calleeLegId !== undefined) {
      const calleeExisting = call.legs.get(payload.calleeLegId);
      if (calleeExisting) {
        calleeExisting.state = payload.calleeLegOutcome;
        calleeExisting.endedAt = occurredAt;
        calleeExisting.endReason = payload.endReason;
        if (payload.calleeLegStartedAt !== undefined) {
          calleeExisting.startedAt = payload.calleeLegStartedAt;
        }
        if (payload.calleeLegAnsweredAt !== undefined) {
          calleeExisting.answeredAt = payload.calleeLegAnsweredAt;
        }
      } else {
        call.legs.set(payload.calleeLegId, {
          legId: payload.calleeLegId,
          role: 'CALLEE',
          state: payload.calleeLegOutcome,
          startedAt: payload.calleeLegStartedAt,
          answeredAt: payload.calleeLegAnsweredAt,
          endedAt: occurredAt,
          endReason: payload.endReason
        });
      }
    }
    if (payload.bridgedAt !== undefined && call.bridgedAt === null) {
      call.bridgedAt = payload.bridgedAt;
    }
    if (payload.recordingStartedAt !== undefined && call.recordingStartedAt === null) {
      call.recordingStartedAt = payload.recordingStartedAt;
    }
    if (payload.recordingEndedAt !== undefined && call.recordingEndedAt === null) {
      call.recordingEndedAt = payload.recordingEndedAt;
    }
    this.executionCounts.terminalFactsAccepted += 1;
    if (call.bleg) {
      call.bleg.phase = call.status === 'ENDED' ? 'ENDED' : 'FAILED';
      call.bleg.terminalEventId = body.eventId;
    }
    this.settleReservation(call, payload.authoritativeRecordingSeconds);
    if (call.task) {
      call.task.status = mapping.taskStatus;
    }
  }

  applyTerminalEvent(call, body, reconciled) {
    const payload = body.payload;
    if (reconciled) {
      if (typeof payload.resolvesUnknownEventId !== 'string') {
        return { argument: true };
      }
      if (call.status !== 'UNKNOWN') {
        return { status: 'QUARANTINED', reasonCode: 'NOT_UNKNOWN' };
      }
      if (call.unknownEventId === null || payload.resolvesUnknownEventId !== call.unknownEventId) {
        return { argument: true };
      }
    }
    if (call.terminalAccepted) {
      return this.terminalFactsMatch(call, payload, body.occurredAt)
        ? { status: 'IGNORED', reasonCode: 'ALREADY_TERMINAL' }
        : { status: 'QUARANTINED', reasonCode: 'CONTRADICTORY_FACT' };
    }
    if (call.status === 'UNKNOWN' && !reconciled) {
      return { status: 'QUARANTINED', reasonCode: 'USE_RECONCILIATION_EVENT' };
    }
    if (call.status !== 'ACTIVE' && call.status !== 'UNKNOWN') {
      return { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    const contradiction = this.validateTerminalFacts(call, payload, body.occurredAt);
    if (contradiction) {
      return { status: 'QUARANTINED', reasonCode: contradiction };
    }
    this.applyTerminalFacts(call, body, reconciled);
    return { status: 'APPLIED' };
  }

  terminalFactsMatch(call, payload, occurredAt) {
    return deepEqual(call.terminalFacts, this.terminalFactObject(payload, occurredAt));
  }

  applyTerminationUnknown(call, body) {
    if (call.terminalAccepted) {
      return { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    if (call.status === 'UNKNOWN') {
      return { status: 'IGNORED', reasonCode: 'ALREADY_TERMINAL' };
    }
    if (call.status !== 'ACTIVE') {
      return { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    call.status = 'UNKNOWN';
    call.endedAt = null;
    call.authoritativeSeconds = null;
    call.endReason = 'LINE_OUTCOME_UNKNOWN';
    call.unknownEventId = body.eventId;
    this.executionCounts.unknownFacts += 1;
    if (call.bleg && !['DISPATCH_UNKNOWN', 'FAILED', 'ENDED', 'AUTHORIZATION_EXPIRED'].includes(call.bleg.phase)) {
      call.bleg.phase = 'DISPATCH_UNKNOWN';
    }
    return { status: 'APPLIED' };
  }

  applyOutboundNotStarted(call, body) {
    if (call.terminalAccepted) {
      return { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    if (call.status === 'UNKNOWN') {
      return { status: 'QUARANTINED', reasonCode: 'USE_RECONCILIATION_EVENT' };
    }
    if (call.status !== 'ACTIVE') {
      return { status: 'QUARANTINED', reasonCode: 'CALL_TERMINAL' };
    }
    if (!call.reservation) {
      return { argument: true };
    }
    if (body.payload.reservationId !== undefined && body.payload.reservationId !== call.reservation.reservationId) {
      return { argument: true };
    }
    const calleeLeg = [...call.legs.values()].find((leg) => leg.role === 'CALLEE');
    const blegActive = call.bleg && !['PREPARED', 'NOT_DISPATCHED', 'AUTHORIZATION_EXPIRED'].includes(call.bleg.phase);
    if (calleeLeg || call.bridgedAt !== null || call.recordingStartedAt !== null || (call.bleg && call.bleg.lineAttemptCount > 0) || blegActive) {
      return { status: 'QUARANTINED', reasonCode: 'OUTBOUND_ALREADY_STARTED' };
    }
    if (body.occurredAt < call.reservation.authorizationExpiresAt) {
      return { status: 'QUARANTINED', reasonCode: 'AUTHORIZATION_NOT_EXPIRED' };
    }
    call.status = 'FAILED';
    call.endedAt = body.occurredAt;
    call.endReason = 'AUTHORIZATION_EXPIRED';
    call.authoritativeSeconds = 0;
    call.terminalEventId = body.eventId;
    call.terminalAccepted = true;
    this.executionCounts.terminalFactsAccepted += 1;
    if (call.task) {
      call.task.status = 'FAILED';
    }
    call.reservation.status = 'RELEASED';
    call.reservationTerminalCount += 1;
    this.executionCounts.releases += 1;
    if (call.bleg) {
      call.bleg.phase = 'AUTHORIZATION_EXPIRED';
      call.bleg.terminalEventId = body.eventId;
    }
    return { status: 'APPLIED' };
  }

  touchRecordingOutbox(kind, callId, recordingId) {
    const key = `${kind}:${callId}:${recordingId}`;
    let entry = this.recordingOutbox.get(key);
    if (!entry) {
      entry = { key, kind, callId, recordingId, status: 'PENDING' };
      this.recordingOutbox.set(key, entry);
      this.executionCounts.recordingOutboxEntries += 1;
    } else {
      this.executionCounts.recordingOutboxRetried += 1;
    }
    return entry;
  }

  finishRecordingOutbox(entry, httpStatus, code) {
    if (entry.status === 'ACKED' || entry.status === 'PERMANENT_FAILURE') {
      return;
    }
    if (this.activeDelivery?.responseLost === true) {
      entry.status = 'SENT';
      return;
    }
    const definition = EXPECTED_RECORDING_LIFECYCLE.outbox[entry.kind];
    if (definition.ackedHttp.includes(httpStatus)) {
      if (entry.status !== 'ACKED') {
        entry.status = 'ACKED';
        this.executionCounts.recordingOutboxAcked += 1;
      }
      return;
    }
    if (definition.permanentCodes.includes(code) || ([400, 404, 422].includes(httpStatus) && !definition.retryableCodes.includes(code))) {
      if (entry.status !== 'PERMANENT_FAILURE') {
        entry.status = 'PERMANENT_FAILURE';
        this.executionCounts.recordingOutboxPermanent += 1;
      }
      return;
    }
    entry.status = 'SENT';
  }

  normalizeObservation(observed) {
    if (observed === null || typeof observed !== 'object' || Array.isArray(observed)) {
      return 'observation must be an object';
    }
    const keys = Object.keys(observed);
    if (keys.length !== RECORDING_OBSERVATION_FIELDS.length || RECORDING_OBSERVATION_FIELDS.some((key) => !keys.includes(key))) {
      return 'observation fields must be exactly the server-observed storage/media facts';
    }
    if (typeof observed.objectIdentityDigest !== 'string' || observed.objectIdentityDigest.length < 1 || observed.objectIdentityDigest.length > 256) {
      return 'objectIdentityDigest must be a 1-256 string';
    }
    if (typeof observed.sha256 !== 'string' || !/^[0-9a-f]{64}$/.test(observed.sha256)) {
      return 'sha256 must be a lowercase SHA-256';
    }
    if (!Number.isInteger(observed.sizeBytes) || observed.sizeBytes < 1) {
      return 'sizeBytes must be a positive integer';
    }
    if (typeof observed.mimeType !== 'string' || observed.mimeType.length < 1 || observed.mimeType.length > 255) {
      return 'mimeType must be a 1-255 string';
    }
    if (!Number.isInteger(observed.durationSeconds) || observed.durationSeconds < 1) {
      return 'durationSeconds must be a positive integer';
    }
    return null;
  }

  observationOf(recording) {
    if (!recording.mediaObject) {
      return null;
    }
    return {
      objectIdentityDigest: recording.mediaObject.objectIdentityDigest,
      sha256: recording.mediaObject.sha256,
      sizeBytes: recording.mediaObject.sizeBytes,
      mimeType: recording.mediaObject.mimeType,
      durationSeconds: recording.mediaObject.durationSeconds
    };
  }

  completionObservation(recording, server) {
    if (server.observed !== undefined) {
      if (this.normalizeObservation(server.observed)) {
        throw new Error('server.observed must be a complete server-observed fact set');
      }
      return {
        objectIdentityDigest: server.observed.objectIdentityDigest,
        sha256: server.observed.sha256,
        sizeBytes: server.observed.sizeBytes,
        mimeType: server.observed.mimeType,
        durationSeconds: server.observed.durationSeconds
      };
    }
    if (recording.terminalPresentation) {
      return { ...recording.terminalPresentation };
    }
    return this.observationOf(recording);
  }

  quarantineRecording(recording, now, reasonCode, presented = null) {
    recording.status = 'QUARANTINED';
    recording.revision += 1;
    recording.validatedAt = now;
    recording.terminalOutcome = { status: 'QUARANTINED', code: reasonCode };
    if (presented) {
      recording.terminalFacts = { ...presented };
      recording.terminalPresentation = { ...presented };
    }
    recording.terminalResponse = {
      recordingId: recording.recordingId,
      status: 'QUARANTINED',
      reasonCode,
      completedAt: now
    };
    this.executionCounts.recordingQuarantines += 1;
  }

  appendRecordingConflict(callId, recording, presented, reasonCode) {
    const conflictInput = {
      recordingId: recording.recordingId,
      originalStatus: recording.status,
      originalValidatedSha256: recording.validated?.sha256 ?? null,
      presentedObjectVersionDigest: presented.objectIdentityDigest,
      presentedSha256: presented.sha256,
      presentedSizeBytes: presented.sizeBytes,
      presentedMime: presented.mimeType,
      presentedDuration: presented.durationSeconds,
      reasonCode
    };
    const conflictDigest = sha256(canonicalJson(conflictInput));
    const dedupeKey = `recording-conflict:${conflictDigest}`;
    let fact = this.recordingConflicts.get(dedupeKey);
    if (!fact) {
      fact = {
        callId,
        outboxId: `outbox-recording-conflict:${conflictDigest.slice(0, 32)}`,
        dedupeKey,
        payload: {
          recordingId: recording.recordingId,
          originalStatus: recording.status,
          conflictDigest,
          presentedSha256: presented.sha256,
          presentedSizeBytes: presented.sizeBytes,
          presentedMime: presented.mimeType,
          presentedDuration: presented.durationSeconds,
          reasonCode
        }
      };
      this.recordingConflicts.set(dedupeKey, fact);
      this.executionCounts.recordingConflictFacts += 1;
    }
    return fact;
  }

  handleDeclare(callId, body, server) {
    const allowed = ['callId', ...RECORDING_DECLARATION_FIELDS];
    if (Object.keys(body).some((key) => !allowed.includes(key))) {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (typeof body.recordingId !== 'string' || body.recordingId.length < 1 || body.recordingId.length > 128 || body.recordingId.trim() === '') {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (typeof body.mimeType !== 'string' || body.mimeType.length < 1 || body.mimeType.length > 64 || body.mimeType.trim() === '') {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (!Number.isInteger(body.sizeBytes) || body.sizeBytes < 1) {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (!Number.isInteger(body.durationSeconds) || body.durationSeconds < 1) {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (typeof body.sha256 !== 'string' || !/^[0-9a-f]{64}$/.test(body.sha256)) {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (!this.calls.has(callId)) {
      return { status: 404, problem: { code: 'CALL_UNKNOWN' } };
    }
    const outboxEntry = this.touchRecordingOutbox('declaration', callId, body.recordingId);
    const declaration = {
      mimeType: body.mimeType,
      sizeBytes: body.sizeBytes,
      durationSeconds: body.durationSeconds,
      sha256: body.sha256
    };
    const existing = this.recordings.get(body.recordingId);
    if (existing) {
      if (existing.callId === callId && existing.status === 'DECLARED' && deepEqual(existing.declared, declaration)) {
        this.executionCounts.recordingDeclarationReplays += 1;
        this.finishRecordingOutbox(outboxEntry, 201, null);
        return { status: 201, response: existing.declarationResponse };
      }
      this.finishRecordingOutbox(outboxEntry, 409, 'RECORDING_IDENTITY_CONFLICT');
      return { status: 409, problem: { code: 'RECORDING_IDENTITY_CONFLICT' } };
    }
    const now = server.now;
    const uploadExpiresAt = server.uploadExpiresAt;
    if (typeof now !== 'string' || typeof uploadExpiresAt !== 'string' || !(now < uploadExpiresAt)) {
      throw new Error('declaration server context must provide now before uploadExpiresAt');
    }
    const credential = `upload-credential:${sha256(`ivr-upload:${body.recordingId}:${uploadExpiresAt}`).slice(0, 32)}`;
    const declarationResponse = {
      recordingId: body.recordingId,
      status: 'DECLARED',
      uploadExpiresAt,
      upload: { credential, expiresAt: uploadExpiresAt },
      declaredAt: now
    };
    this.recordings.set(body.recordingId, {
      callId,
      recordingId: body.recordingId,
      status: 'DECLARED',
      declared: declaration,
      declarationFingerprint: sha256(canonicalJson({ callId, recordingId: body.recordingId, ...declaration })),
      declaredAt: now,
      uploadExpiresAt,
      upload: { credential, expiresAt: uploadExpiresAt },
      revision: 1,
      mediaObject: null,
      observationFingerprint: null,
      validated: null,
      validatedAt: null,
      terminalFacts: null,
      terminalPresentation: null,
      terminalOutcome: null,
      terminalResponse: null,
      declarationResponse
    });
    this.executionCounts.recordingsCreated += 1;
    this.executionCounts.recordingDeclarations += 1;
    this.finishRecordingOutbox(outboxEntry, 201, null);
    return { status: 201, response: declarationResponse };
  }

  handleObserveUpload(callId, body) {
    const recording = this.recordings.get(body.recordingId);
    if (!this.calls.has(callId) || !recording || recording.callId !== callId) {
      return { status: 409, code: 'CALL_UNKNOWN' };
    }
    const observed = body.observed;
    const observationError = this.normalizeObservation(observed);
    if (observationError) {
      return { status: 400, code: 'IVR_ARGUMENT_INVALID' };
    }
    const normalized = {
      objectIdentityDigest: observed.objectIdentityDigest,
      sha256: observed.sha256,
      sizeBytes: observed.sizeBytes,
      mimeType: observed.mimeType,
      durationSeconds: observed.durationSeconds
    };
    const fingerprint = sha256(canonicalJson(normalized));
    if (recording.status === 'VALIDATED' || recording.status === 'QUARANTINED') {
      if (recording.observationFingerprint === fingerprint) {
        this.executionCounts.uploadObservationReplays += 1;
        return { status: 200, response: { recordingId: recording.recordingId, status: 'UPLOADED' } };
      }
      return { status: 409, code: 'RECORDING_IDENTITY_CONFLICT' };
    }
    if (recording.status === 'UPLOADED' || recording.observationFingerprint !== null) {
      if (recording.observationFingerprint === fingerprint) {
        this.executionCounts.uploadObservationReplays += 1;
        return { status: 200, response: { recordingId: recording.recordingId, status: 'UPLOADED' } };
      }
      return { status: 409, code: 'RECORDING_IDENTITY_CONFLICT' };
    }
    if (typeof body.now !== 'string') {
      throw new Error('upload observation requires a fixed now');
    }
    if (body.now >= recording.uploadExpiresAt) {
      this.quarantineRecording(recording, body.now, 'UPLOAD_EXPIRED');
      return { status: 409, code: 'UPLOAD_EXPIRED' };
    }
    recording.mediaObject = normalized;
    recording.observationFingerprint = fingerprint;
    recording.status = 'UPLOADED';
    recording.revision += 1;
    this.executionCounts.uploadObservations += 1;
    this.executionCounts.mediaObjectsCreated += 1;
    return { status: 200, response: { recordingId: recording.recordingId, status: 'UPLOADED' } };
  }

  handleComplete(callId, recordingId, body, server) {
    const allowed = ['callId', 'recordingId', 'clientReference'];
    if (Object.keys(body).some((key) => !allowed.includes(key))) {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (body.clientReference !== undefined && (typeof body.clientReference !== 'string' || body.clientReference.length < 1 || body.clientReference.length > 128)) {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (typeof recordingId !== 'string' || recordingId.length < 1 || recordingId.length > 128) {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    if (!this.calls.has(callId)) {
      return { status: 404, problem: { code: 'CALL_UNKNOWN' } };
    }
    const recording = this.recordings.get(recordingId);
    if (!recording || recording.callId !== callId) {
      return { status: 404, problem: { code: 'CALL_UNKNOWN' } };
    }
    const outboxEntry = this.touchRecordingOutbox('completion', callId, recordingId);
    const now = server.now;
    if (typeof now !== 'string') {
      throw new Error('completion requires a fixed now');
    }
    if (recording.status === 'VALIDATED' || recording.status === 'QUARANTINED') {
      if (recording.terminalOutcome?.code === 'UPLOAD_EXPIRED' && recording.terminalFacts === null) {
        this.finishRecordingOutbox(outboxEntry, 409, 'UPLOAD_EXPIRED');
        return { status: 409, problem: { code: 'UPLOAD_EXPIRED' } };
      }
      const presented = this.completionObservation(recording, server);
      if (recording.terminalFacts && presented && deepEqual(recording.terminalFacts, presented)) {
        this.executionCounts.recordingCompletionReplays += 1;
        this.finishRecordingOutbox(outboxEntry, 200, null);
        return { status: 200, response: recording.terminalResponse };
      }
      if (presented) {
        this.appendRecordingConflict(callId, recording, presented, 'RECORDING_IDENTITY_CONFLICT');
      }
      this.finishRecordingOutbox(outboxEntry, 409, 'RECORDING_IDENTITY_CONFLICT');
      return { status: 409, problem: { code: 'RECORDING_IDENTITY_CONFLICT' } };
    }
    if (recording.status === 'DECLARED' && now >= recording.uploadExpiresAt) {
      this.quarantineRecording(recording, now, 'UPLOAD_EXPIRED');
      this.finishRecordingOutbox(outboxEntry, 409, 'UPLOAD_EXPIRED');
      return { status: 409, problem: { code: 'UPLOAD_EXPIRED' } };
    }
    if (recording.status === 'DECLARED') {
      this.finishRecordingOutbox(outboxEntry, 409, 'UPLOAD_OBSERVATION_REQUIRED');
      return { status: 409, problem: { code: 'UPLOAD_OBSERVATION_REQUIRED' } };
    }
    if (server.storageUnknown === true) {
      this.finishRecordingOutbox(outboxEntry, 503, 'STORAGE_UNKNOWN');
      return { status: 503, problem: { code: 'STORAGE_UNKNOWN' } };
    }
    const presented = this.completionObservation(recording, server);
    if (!presented) {
      throw new Error('completion requires a server observation');
    }
    if (!deepEqual(presented, this.observationOf(recording))) {
      this.quarantineRecording(recording, now, 'RECORDING_IDENTITY_CONFLICT', presented);
      this.finishRecordingOutbox(outboxEntry, 200, null);
      return { status: 200, response: recording.terminalResponse };
    }
    if (!deepEqual({ mimeType: presented.mimeType, sizeBytes: presented.sizeBytes, durationSeconds: presented.durationSeconds, sha256: presented.sha256 }, recording.declared)) {
      this.quarantineRecording(recording, now, 'MEDIA_VALIDATION_FAILED', presented);
      this.finishRecordingOutbox(outboxEntry, 200, null);
      return { status: 200, response: recording.terminalResponse };
    }
    recording.status = 'VALIDATED';
    recording.revision += 1;
    recording.validatedAt = now;
    recording.validated = { durationSeconds: presented.durationSeconds, sha256: presented.sha256 };
    recording.terminalFacts = { ...presented };
    recording.terminalPresentation = { ...presented };
    recording.terminalOutcome = { status: 'VALIDATED', code: null };
    recording.terminalResponse = {
      recordingId: recording.recordingId,
      status: 'VALIDATED',
      validatedDurationSeconds: presented.durationSeconds,
      validatedSha256: presented.sha256,
      completedAt: now
    };
    this.executionCounts.recordingValidations += 1;
    this.finishRecordingOutbox(outboxEntry, 200, null);
    return { status: 200, response: recording.terminalResponse };
  }

  localBlegPrepare(callId) {
    const call = this.calls.get(callId);
    if (!call) {
      return { status: 409, code: 'CALL_UNKNOWN' };
    }
    if (call.bleg) {
      this.executionCounts.redialAttempts += 1;
      return { status: 409, code: 'BLEG_OPERATION_EXISTS' };
    }
    if (!call.reservation) {
      return { status: 409, code: 'NO_AUTHORIZATION' };
    }
    let decision = null;
    for (const purpose of ['DIRECT_AUTHORIZATION', 'ACTIVATION_NEXT_ACTION', 'INBOUND_RESOLVE']) {
      const frozen = call.decisions.get(purpose);
      if (!frozen) {
        continue;
      }
      if (frozen.response.action === 'DIAL_TASK_CALLEE' || (frozen.response.purpose === 'DIRECT_AUTHORIZATION' && frozen.response.action === null)) {
        decision = frozen.response;
        break;
      }
    }
    if (!decision) {
      return { status: 409, code: 'NO_AUTHORIZATION' };
    }
    const callee = decision.task?.calleeNumber || decision.authorization?.calleeNumber;
    if (!callee) {
      return { status: 409, code: 'NO_AUTHORIZATION' };
    }
    call.bleg = {
      decisionId: decision.decisionId,
      reservationId: call.reservation.reservationId,
      authorizationExpiresAt: decision.authorizationExpiresAt,
      frozenCalleeDigest: sha256(callee),
      lineAttemptCount: 0,
      phase: 'PREPARED',
      dispatchMarker: null,
      terminalEventId: null
    };
    this.executionCounts.blegOperationsCreated += 1;
    return { status: 200 };
  }

  localBlegDispatch(callId, body) {
    const call = this.calls.get(callId);
    if (!call?.bleg) {
      return { status: 409, code: 'BLEG_MISSING' };
    }
    if (call.bleg.lineAttemptCount > 0 || call.bleg.phase !== 'PREPARED') {
      this.executionCounts.redialAttempts += 1;
      return { status: 409, code: 'REDIAL_FORBIDDEN' };
    }
    if (typeof body.now !== 'string' || body.now >= call.bleg.authorizationExpiresAt) {
      return { status: 409, code: 'AUTHORIZATION_EXPIRED' };
    }
    call.bleg.phase = 'DISPATCHING';
    call.bleg.dispatchMarker = `dispatch:${callId}`;
    return { status: 200 };
  }

  localLineOutcome(callId, body) {
    const call = this.calls.get(callId);
    if (!call?.bleg) {
      return { status: 409, code: 'BLEG_MISSING' };
    }
    if (call.bleg.phase !== 'DISPATCHING') {
      this.executionCounts.redialAttempts += 1;
      return { status: 409, code: call.bleg.lineAttemptCount > 0 ? 'REDIAL_FORBIDDEN' : 'BLEG_STATE_INVALID' };
    }
    if (!['ACCEPTED', 'UNKNOWN', 'FAILED'].includes(body.outcome)) {
      return { status: 409, code: 'INVALID_LINE_OUTCOME' };
    }
    call.bleg.lineAttemptCount = 1;
    this.executionCounts.lineAttempts += 1;
    if (body.outcome === 'ACCEPTED') {
      call.bleg.phase = 'DIALING';
    } else if (body.outcome === 'UNKNOWN') {
      call.bleg.phase = 'DISPATCH_UNKNOWN';
    } else {
      call.bleg.phase = 'FAILED';
    }
    return { status: 200 };
  }

  localBlegReconcile(callId, body) {
    const call = this.calls.get(callId);
    if (!call?.bleg) {
      return { status: 409, code: 'BLEG_MISSING' };
    }
    if (!['DISPATCHING', 'DISPATCH_UNKNOWN'].includes(call.bleg.phase)) {
      this.executionCounts.redialAttempts += 1;
      return { status: 409, code: 'REDIAL_FORBIDDEN' };
    }
    if (body.authoritative === 'NO_ATTEMPT') {
      call.bleg.lineAttemptCount = 0;
      call.bleg.phase = 'NOT_DISPATCHED';
      if (typeof body.now === 'string' && body.now < call.bleg.authorizationExpiresAt) {
        call.bleg.phase = 'PREPARED';
      } else {
        call.bleg.phase = 'AUTHORIZATION_EXPIRED';
      }
      return { status: 200 };
    }
    if (!['ATTEMPTED', 'IN_PROGRESS', 'ANSWERED', 'COMPLETED'].includes(body.authoritative)) {
      return { status: 409, code: 'INVALID_LINE_EVIDENCE' };
    }
    if (call.bleg.lineAttemptCount === 0) {
      call.bleg.lineAttemptCount = 1;
      this.executionCounts.lineAttempts += 1;
    }
    if (body.authoritative === 'ATTEMPTED' || body.authoritative === 'IN_PROGRESS') {
      call.bleg.phase = 'DIALING';
    } else if (body.authoritative === 'ANSWERED') {
      call.bleg.phase = 'ANSWERED';
    } else {
      call.bleg.phase = 'ENDED';
    }
    return { status: 200 };
  }

  localBlegExpire(callId, body) {
    const call = this.calls.get(callId);
    if (!call?.bleg) {
      return { status: 409, code: 'BLEG_MISSING' };
    }
    if (call.bleg.lineAttemptCount !== 0 || !['PREPARED', 'NOT_DISPATCHED'].includes(call.bleg.phase)) {
      return { status: 409, code: 'REDIAL_FORBIDDEN' };
    }
    if (typeof body.now !== 'string' || body.now < call.bleg.authorizationExpiresAt) {
      return { status: 409, code: 'AUTHORIZATION_NOT_EXPIRED' };
    }
    call.bleg.phase = 'AUTHORIZATION_EXPIRED';
    return { status: 200 };
  }

  handleLocal(operation, callId, body) {
    let result;
    if (operation === 'bleg.prepare') {
      result = this.localBlegPrepare(callId);
    } else if (operation === 'bleg.dispatch') {
      result = this.localBlegDispatch(callId, body);
    } else if (operation === 'bleg.reconcile') {
      result = this.localBlegReconcile(callId, body);
    } else if (operation === 'bleg.expire') {
      result = this.localBlegExpire(callId, body);
    } else if (operation === 'recording.observeUpload') {
      result = this.handleObserveUpload(callId, body);
    } else if (operation === 'ivr.restart') {
      result = { status: 200 };
    } else {
      throw new Error(`unknown local operation ${operation}`);
    }
    this.assertExecutionInvariants();
    return result;
  }

  handleLineOutcome(callId, body) {
    const result = this.localLineOutcome(callId, body);
    this.assertExecutionInvariants();
    return result;
  }

  assertExecutionInvariants() {
    if (this.executionCounts.evidenceRecordsCreated !== 0 || this.executionCounts.evidenceSolidified !== 0) {
      throw new Error('recording completion must not create or solidify evidence');
    }
    const allowedRecordingStates = new Set(EXPECTED_RECORDING_LIFECYCLE.states);
    for (const recording of this.recordings.values()) {
      if (!allowedRecordingStates.has(recording.status)) {
        throw new Error(`unexpected recording status ${recording.status}`);
      }
      if (!Number.isInteger(recording.revision) || recording.revision < 1) {
        throw new Error('recording revision must be a positive integer');
      }
      if (recording.status === 'VALIDATED' && (!recording.validated || !recording.mediaObject)) {
        throw new Error('VALIDATED recording must keep one media object and validated facts');
      }
      if (recording.status === 'QUARANTINED' && recording.validated !== null) {
        throw new Error('QUARANTINED recording must not carry successful validated facts');
      }
    }
    for (const call of this.calls.values()) {
      if (call.bleg && call.bleg.lineAttemptCount !== 0 && call.bleg.lineAttemptCount !== 1) {
        throw new Error('lineAttemptCount must be 0 or 1');
      }
      if (call.status === 'UNKNOWN') {
        if (call.endedAt !== null || call.authoritativeSeconds !== null) {
          throw new Error('UNKNOWN call must keep ended_at and authoritative seconds null');
        }
        if (call.reservation && ['SETTLED', 'RELEASED'].includes(call.reservation.status)) {
          throw new Error('UNKNOWN call must not release or settle its reservation');
        }
      }
      if (call.reservationTerminalCount > 1) {
        throw new Error('reservation terminal transition happened more than once');
      }
      if (call.usageFactCount > 1) {
        throw new Error('more than one usage fact for a call');
      }
    }
  }

  projectCall(callId) {
    const call = this.calls.get(callId);
    if (!call) {
      return { call: null };
    }
    const decisions = call.decisionOrder.map((purpose) => {
      const frozen = call.decisions.get(purpose);
      return {
        purpose,
        action: frozen.response.action,
        reasonCode: frozen.response.reasonCode
      };
    });
    const latest = call.decisionOrder.length > 0 ? call.decisions.get(call.decisionOrder[call.decisionOrder.length - 1]).response : null;
    return {
      call: {
        exists: true,
        status: 'ACTIVE',
        protocolVersion: call.protocolVersion,
        sessionStatus: call.sessionStatus,
        task: call.task ? { taskId: call.task.taskId, status: call.task.status } : null,
        reservation: call.reservation ? { reservationId: call.reservation.reservationId, status: call.reservation.status } : null,
        decisions,
        trialGrants: call.trialGrants,
        activationConfirmations: call.activationConfirmations,
        behavior: this.behaviorFor(latest)
      }
    };
  }

  behaviorFor(decision) {
    const action = decision ? decision.action : null;
    return {
      taskAutoDialFrozenCallee: action === 'DIAL_TASK_CALLEE' || (decision?.purpose === 'DIRECT_AUTHORIZATION' && decision?.action === null),
      calleeConfirmationRequired: action === 'ENTER_CALLEE',
      directDtmfRequired: action === 'ENTER_CALLEE',
      purchaseGuidanceOnly: action === 'GUIDE_PURCHASE',
      activationKeyOneRequired: action === 'GUIDE_ACTIVATION'
    };
  }

  assertInvariants() {
    for (const call of this.calls.values()) {
      if (call.decisionOrder.length > 3) {
        throw new Error(`more than three purpose decisions for ${call.callId}`);
      }
      const unique = new Set(call.decisionOrder);
      if (unique.size !== call.decisionOrder.length) {
        throw new Error(`duplicate decision purpose for ${call.callId}`);
      }
      if (call.trialGrants > 1) {
        throw new Error(`more than one trial for ${call.callId}`);
      }
      if (call.activationConfirmations > 1) {
        throw new Error(`more than one activation confirmation for ${call.callId}`);
      }
    }
    if (this.counts.loginAccountsCreated !== 0 || this.counts.realNameProfilesCreated !== 0) {
      throw new Error('IVR created a login or real-name record');
    }
    if (this.counts.purchaseOrdersCreated !== 0 || this.counts.operatorIntegrations !== 0) {
      throw new Error('IVR crossed the order or operator boundary');
    }
  }

  resolveSessionId(callId) {
    return stableUuid(`ivr-session:${callId}`);
  }

  resolveFingerprint(callId, body, server) {
    return sha256(canonicalJson({
      purpose: 'INBOUND_RESOLVE',
      protocolVersion: body.protocolVersion,
      callId,
      sessionId: this.resolveSessionId(callId),
      callerNumberHmac: server.callerNumberHmac,
      accessNumberHmac: server.accessNumberHmac,
      subjectId: server.subjectId ?? null,
      callerBindingId: server.callerBindingId ?? null,
      entryMode: server.entryMode,
      nodeId: server.nodeId
    }));
  }

  confirmFingerprint(callId, call, body, server) {
    return sha256(canonicalJson({
      purpose: 'ACTIVATION_NEXT_ACTION',
      protocolVersion: call.protocolVersion,
      callId,
      sessionId: this.resolveSessionId(callId),
      callerNumberHmac: call.callerNumberHmac,
      accessNumberHmac: call.accessNumberHmac,
      subjectId: call.subjectId ?? null,
      confirmedSubjectId: server.confirmedSubjectId ?? null,
      callerBindingId: call.callerBindingId ?? null,
      nodeId: call.nodeId,
      promptVersion: body.promptVersion,
      policyCode: body.policyCode,
      policyVersion: body.policyVersion,
      confirmationKey: '1',
      confirmationContent: body.contentSha256
    }));
  }

  directFingerprint(callId, call, body, server) {
    return sha256(canonicalJson({
      purpose: 'DIRECT_AUTHORIZATION',
      protocolVersion: call.protocolVersion,
      callId,
      sessionId: this.resolveSessionId(callId),
      callerNumberHmac: call.callerNumberHmac,
      accessNumberHmac: call.accessNumberHmac,
      promptVersion: body.promptVersion,
      enteredCalleeHmac: server.enteredCalleeHmac,
      confirmationKey: body.confirmationKey,
      confirmationContent: body.confirmationContent
    }));
  }

  selectionAction(server, effectiveCallerState) {
    if (server.callerTrusted === false) {
      return { action: 'REJECT', reason: 'CALLER_UNTRUSTED' };
    }
    if (server.activeCall === true) {
      return { action: 'REJECT_CALL_IN_PROGRESS', reason: 'CALL_IN_PROGRESS' };
    }
    if (effectiveCallerState === 'SUSPENDED') {
      return { action: 'REJECT_SUSPENDED', reason: 'ACTIVATION_SUSPENDED' };
    }
    if (effectiveCallerState === 'UNREGISTERED' || effectiveCallerState === 'INACTIVE') {
      return { action: 'GUIDE_ACTIVATION', reason: server.activation?.consentRequired ? 'CONSENT_REQUIRED' : 'ACTIVATION_REQUIRED' };
    }
    const available = server.entitlement?.availableRecordingSeconds ?? 0;
    if (available <= 0) {
      return { action: 'GUIDE_PURCHASE', reason: 'NO_AVAILABLE_ENTITLEMENT' };
    }
    if (server.task && server.task.expired !== true) {
      return { action: 'DIAL_TASK_CALLEE', reason: 'TASK_CLAIMED' };
    }
    return { action: 'ENTER_CALLEE', reason: 'NO_VALID_TASK' };
  }

  buildDecision({ callId, purpose, action, reason, taskMatch, callerState, server, task, reservationId = null }) {
    if (!server.decisionId || !server.decisionAt || !server.balanceAsOf) {
      throw new Error('fixture server context must provide decisionId, decisionAt and balanceAsOf');
    }
    const decision = {
      decisionId: server.decisionId,
      callId,
      purpose,
      action,
      callerState,
      taskMatch,
      reasonCode: reason,
      availableRecordingSeconds: server.entitlement?.availableRecordingSeconds ?? 0,
      balanceAsOf: server.balanceAsOf,
      task: task || null,
      reservationId,
      authorizedRecordingSeconds: reservationId ? (server.entitlement?.authorizedRecordingSeconds ?? null) : null,
      authorizationExpiresAt: reservationId ? (server.entitlement?.authorizationExpiresAt ?? null) : null,
      decisionAt: server.decisionAt
    };
    if (purpose === 'DIRECT_AUTHORIZATION' && action === null) {
      decision.authorization = { calleeNumber: server.confirmedDirectCallee };
    }
    return decision;
  }

  storeDecision(call, purpose, fingerprint, response) {
    if (call.decisions.has(purpose)) {
      throw new Error(`duplicate decision purpose ${purpose}`);
    }
    call.decisions.set(purpose, { fingerprint, response });
    call.decisionOrder.push(purpose);
    this.counts.decisionsFrozen += 1;
  }

  handleResolve(body, server) {
    const versionCheck = this.checkVersion(body.protocolVersion);
    if (versionCheck) {
      return versionCheck;
    }
    if (!body.callId || body.purpose !== 'INBOUND_RESOLVE') {
      return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
    }
    const callId = body.callId;
    const fingerprint = this.resolveFingerprint(callId, body, server);
    const existingCall = this.calls.get(callId);
    if (existingCall) {
      const frozen = existingCall.decisions.get('INBOUND_RESOLVE');
      if (!frozen) {
        return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
      }
      if (frozen.fingerprint !== fingerprint) {
        return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
      }
      return { status: 200, response: frozen.response };
    }

    const call = {
      callId,
      protocolVersion: body.protocolVersion,
      callerState: server.callerState,
      callerNumberHmac: server.callerNumberHmac,
      accessNumberHmac: server.accessNumberHmac,
      subjectId: server.subjectId ?? null,
      callerBindingId: server.callerBindingId ?? null,
      nodeId: server.nodeId,
      promptVersion: body.promptVersion ?? null,
      status: 'ACTIVE',
      sessionStatus: 'RECEIVED',
      task: null,
      reservation: null,
      decisions: new Map(),
      decisionOrder: [],
      trialGrants: 0,
      activationConfirmations: 0,
      legs: new Map(),
      bridgedAt: null,
      recordingStartedAt: null,
      recordingEndedAt: null,
      endedAt: null,
      endReason: null,
      authoritativeSeconds: null,
      unknownEventId: null,
      reconciliationEventId: null,
      terminalEventId: null,
      terminalAccepted: false,
      terminalFacts: null,
      bleg: null,
      reservationTerminalCount: 0,
      usageFactCount: 0
    };
    this.calls.set(callId, call);
    this.counts.callsCreated += 1;
    this.counts.sessionsCreated += 1;

    const selection = this.selectionAction(server, server.callerState);
    let task = null;
    let reservationId = null;
    if (selection.action === 'DIAL_TASK_CALLEE') {
      if (!server.task || !server.task.taskId || !server.reservationId) {
        throw new Error('task-backed selection requires task and reservationId');
      }
      task = {
        taskId: server.task.taskId,
        businessType: server.task.businessType,
        calleeMasked: server.task.calleeMasked,
        calleeNumber: server.task.calleeNumber
      };
      call.task = { taskId: server.task.taskId, status: 'CLAIMED' };
      call.reservation = {
        reservationId: server.reservationId,
        status: 'HELD',
        authorizedRecordingSeconds: server.entitlement?.authorizedRecordingSeconds ?? null,
        authorizationExpiresAt: server.entitlement?.authorizationExpiresAt ?? null
      };
      reservationId = server.reservationId;
      this.counts.tasksClaimed += 1;
      this.counts.reservationsCreated += 1;
    }
    const response = this.buildDecision({
      callId,
      purpose: 'INBOUND_RESOLVE',
      action: selection.action,
      reason: selection.reason,
      taskMatch: selection.action === 'DIAL_TASK_CALLEE' ? 'CLAIMED' : 'NONE',
      callerState: server.callerState,
      server,
      task,
      reservationId
    });
    this.storeDecision(call, 'INBOUND_RESOLVE', fingerprint, response);
    return { status: 200, response };
  }

  handleConfirm(callId, body, server) {
    const call = this.calls.get(callId);
    if (!call) {
      return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
    }
    if (body.decision !== 'ACCEPT' || !/^[0-9a-f]{64}$/.test(body.contentSha256 || '')) {
      return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
    }
    const fingerprint = this.confirmFingerprint(callId, call, body, server);
    const existing = call.decisions.get('ACTIVATION_NEXT_ACTION');
    if (existing) {
      if (existing.fingerprint !== fingerprint) {
        return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
      }
      return { status: 200, response: existing.response };
    }
    const resolveDecision = call.decisions.get('INBOUND_RESOLVE');
    if (!resolveDecision) {
      return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
    }
    if (resolveDecision.response.action !== 'GUIDE_ACTIVATION') {
      const rejection = this.buildDecision({
        callId,
        purpose: 'ACTIVATION_NEXT_ACTION',
        action: 'REJECT',
        reason: 'CONFIRMATION_NOT_ALLOWED',
        taskMatch: 'NONE',
        callerState: call.callerState,
        server,
        task: null
      });
      this.storeDecision(call, 'ACTIVATION_NEXT_ACTION', fingerprint, rejection);
      return { status: 200, response: rejection };
    }
    if (call.sessionStatus !== 'RECEIVED') {
      return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
    }
    if (server.confirmedSubjectId !== call.subjectId) {
      return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
    }

    call.sessionStatus = 'CONFIRMED';
    call.activationConfirmations += 1;
    this.counts.activationConfirmations += 1;
    if (server.activation?.trialEligible === true) {
      if (call.trialGrants === 0) {
        call.trialGrants = 1;
        this.counts.trialsGranted += 1;
      }
    }
    const effectiveCallerState = server.activation?.resultState || server.callerState;
    const selection = this.selectionAction(server, effectiveCallerState);
    let task = null;
    let reservationId = null;
    if (selection.action === 'DIAL_TASK_CALLEE') {
      if (!server.task || !server.task.taskId || !server.reservationId) {
        throw new Error('task-backed selection requires task and reservationId');
      }
      task = {
        taskId: server.task.taskId,
        businessType: server.task.businessType,
        calleeMasked: server.task.calleeMasked,
        calleeNumber: server.task.calleeNumber
      };
      call.task = { taskId: server.task.taskId, status: 'CLAIMED' };
      call.reservation = {
        reservationId: server.reservationId,
        status: 'HELD',
        authorizedRecordingSeconds: server.entitlement?.authorizedRecordingSeconds ?? null,
        authorizationExpiresAt: server.entitlement?.authorizationExpiresAt ?? null
      };
      reservationId = server.reservationId;
      this.counts.tasksClaimed += 1;
      this.counts.reservationsCreated += 1;
    }
    const response = this.buildDecision({
      callId,
      purpose: 'ACTIVATION_NEXT_ACTION',
      action: selection.action,
      reason: selection.reason,
      taskMatch: selection.action === 'DIAL_TASK_CALLEE' ? 'CLAIMED' : 'NONE',
      callerState: effectiveCallerState,
      server,
      task,
      reservationId
    });
    this.storeDecision(call, 'ACTIVATION_NEXT_ACTION', fingerprint, response);
    return { status: 200, response };
  }

  handleDirect(callId, body, server) {
    const call = this.calls.get(callId);
    if (!call) {
      return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
    }
    const fingerprint = this.directFingerprint(callId, call, body, server);
    const existing = call.decisions.get('DIRECT_AUTHORIZATION');
    if (existing) {
      if (existing.fingerprint !== fingerprint) {
        return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
      }
      return { status: 200, response: existing.response };
    }
    const resolveDecision = call.decisions.get('INBOUND_RESOLVE');
    if (!resolveDecision) {
      return { status: 409, problem: { code: 'SESSION_IDENTITY_MISMATCH' } };
    }
    if (resolveDecision.response.action !== 'ENTER_CALLEE' || server.task || call.task) {
      const rejection = this.buildDecision({
        callId,
        purpose: 'DIRECT_AUTHORIZATION',
        action: 'REJECT',
        reason: server.task || call.task || resolveDecision.response.action === 'DIAL_TASK_CALLEE' ? 'TASK_CLAIMED' : 'CONFIRMATION_NOT_ALLOWED',
        taskMatch: 'NONE',
        callerState: server.callerState || call.callerState,
        server,
        task: null
      });
      this.storeDecision(call, 'DIRECT_AUTHORIZATION', fingerprint, rejection);
      return { status: 200, response: rejection };
    }
    const available = server.entitlement?.availableRecordingSeconds ?? 0;
    if (available <= 0) {
      const guidance = this.buildDecision({
        callId,
        purpose: 'DIRECT_AUTHORIZATION',
        action: 'GUIDE_PURCHASE',
        reason: 'NO_AVAILABLE_ENTITLEMENT',
        taskMatch: 'NONE',
        callerState: server.callerState || call.callerState,
        server,
        task: null
      });
      this.storeDecision(call, 'DIRECT_AUTHORIZATION', fingerprint, guidance);
      return { status: 200, response: guidance };
    }
    if (!server.reservationId || !server.decisionId) {
      throw new Error('direct success requires reservationId and decisionId');
    }
    server.confirmedDirectCallee = body.callee;
    call.reservation = {
      reservationId: server.reservationId,
      status: 'HELD',
      authorizedRecordingSeconds: server.entitlement?.authorizedRecordingSeconds ?? null,
      authorizationExpiresAt: server.entitlement?.authorizationExpiresAt ?? null
    };
    this.counts.reservationsCreated += 1;
    this.counts.directConfirmations += 1;
    const response = this.buildDecision({
      callId,
      purpose: 'DIRECT_AUTHORIZATION',
      action: null,
      reason: 'DIRECT_AUTHORIZED',
      taskMatch: 'NONE',
      callerState: server.callerState || call.callerState,
      server,
      task: null,
      reservationId: server.reservationId
    });
    this.storeDecision(call, 'DIRECT_AUTHORIZATION', fingerprint, response);
    return { status: 200, response };
  }

  checkVersion(version) {
    if (typeof version !== 'string' || version.length < 1 || version.length > 32 || !/^[0-9]+\.[0-9]+$/.test(version)) {
      return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
    }
    if (version !== '1.0') {
      return { status: 400, problem: { code: 'IVR_ARGUMENT_INVALID' } };
    }
    return null;
  }

  handle(operation, callId, body, server, delivery = {}) {
    this.activeDelivery = delivery || {};
    let result;
    if (operation === 'resolveIvrInbound') {
      const allowed = ['callId', 'purpose', 'protocolVersion', 'promptVersion'];
      if (Object.keys(body).some((key) => !allowed.includes(key))) {
        return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
      }
      if (this.activeDelivery.commitFailure === true) {
        return { status: 503, problem: { code: 'SERVICE_UNAVAILABLE' } };
      }
      result = this.handleResolve(body, server);
    } else if (operation === 'confirmIvrActivation') {
      const allowed = ['eventId', 'policyCode', 'policyVersion', 'promptVersion', 'contentSha256', 'decision'];
      if (Object.keys(body).some((key) => !allowed.includes(key))) {
        return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
      }
      result = this.handleConfirm(callId, body, server);
    } else if (operation === 'authorizeIvrDirectOutbound') {
      const allowed = ['eventId', 'callee', 'promptVersion', 'confirmationKey', 'confirmationContent'];
      if (Object.keys(body).some((key) => !allowed.includes(key))) {
        return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
      }
      result = this.handleDirect(callId, body, server);
    } else if (operation === 'ingestIvrCallEvent') {
      const allowed = ['eventId', 'eventType', 'legId', 'sequenceNo', 'occurredAt', 'payload'];
      if (Object.keys(body).some((key) => !allowed.includes(key))) {
        return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
      }
      result = this.handleEvent(callId, body, server);
    } else if (operation === 'declareIvrRecording') {
      result = this.handleDeclare(callId, body, server);
    } else if (operation === 'completeIvrRecording') {
      result = this.handleComplete(callId, body.recordingId, body, server);
    } else {
      throw new Error(`operation ${operation} is not a fixture operation`);
    }
    this.assertInvariants();
    this.assertExecutionInvariants();
    return result;
  }
}

function validateDecisionShape(response, expectedPurpose) {
  const required = ['decisionId', 'callId', 'purpose', 'action', 'callerState', 'taskMatch', 'reasonCode', 'availableRecordingSeconds', 'balanceAsOf', 'reservationId', 'authorizedRecordingSeconds', 'authorizationExpiresAt', 'decisionAt'];
  for (const key of required) {
    if (!Object.prototype.hasOwnProperty.call(response, key)) {
      throw new Error(`decision response missing ${key}`);
    }
  }
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(response.decisionId)) {
    throw new Error('decisionId is not a UUID-shaped value');
  }
  if (response.purpose !== expectedPurpose) {
    throw new Error(`expected purpose ${expectedPurpose}, got ${response.purpose}`);
  }
  if (!['GUIDE_ACTIVATION', 'DIAL_TASK_CALLEE', 'ENTER_CALLEE', 'GUIDE_PURCHASE', 'REJECT', 'REJECT_SUSPENDED', 'REJECT_CALL_IN_PROGRESS', null].includes(response.action)) {
    throw new Error(`unexpected action ${response.action}`);
  }
  if (response.action === 'DIAL_TASK_CALLEE') {
    if (response.taskMatch !== 'CLAIMED' || !response.task || !response.task.taskId || response.reservationId === null) {
      throw new Error('DIAL_TASK_CALLEE must carry task, CLAIMED match and reservation');
    }
  }
  if (response.reservationId !== null && !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(response.reservationId)) {
    throw new Error('reservationId is not a UUID-shaped value');
  }
  if (response.purpose === 'DIRECT_AUTHORIZATION' && response.action === null) {
    if (response.reasonCode !== 'DIRECT_AUTHORIZED' || response.taskMatch !== 'NONE' || response.task !== null || response.reservationId === null) {
      throw new Error('direct success shape is inconsistent');
    }
    if (!response.authorization || typeof response.authorization.calleeNumber !== 'string') {
      throw new Error('direct success must carry the internal authorization callee');
    }
  }
}

function expectedSliceForFixture(fixtureId, config) {
  if (typeof fixtureId !== 'string') {
    return null;
  }
  const separator = fixtureId.indexOf('-');
  if (separator < 1) {
    return null;
  }
  const prefix = fixtureId.slice(0, separator);
  if (!config.sliceOrder.includes(prefix) || !config.fixtureIds.includes(fixtureId)) {
    return null;
  }
  return prefix;
}

function validateFixture(fixture, expectedSlice, counters, issues) {
  const fixtureLabel = fixture?.id || '<unknown fixture>';
  if (fixture === null || typeof fixture !== 'object' || Array.isArray(fixture)) {
    counters.failed += 1;
    issues.push(`${fixtureLabel}: fixture is not an object`);
    return false;
  }
  const topKeys = ['schemaVersion', 'id', 'slice', 'description', 'steps'];
  if (!deepEqual(Object.keys(fixture).sort(), [...topKeys].sort())) {
    counters.failed += 1;
    issues.push(`${fixtureLabel}: top-level keys must be ${topKeys.join(',')}`);
    return false;
  }
  if (typeof expectedSlice !== 'string' || expectedSlice.length === 0 || fixture.schemaVersion !== 1 || fixture.slice !== expectedSlice || typeof fixture.description !== 'string' || fixture.description.length === 0 || !Array.isArray(fixture.steps) || fixture.steps.length === 0) {
    counters.failed += 1;
    issues.push(`${fixtureLabel}: invalid schemaVersion, slice, description or steps`);
    return false;
  }
  if (fixture.slice === 's2') {
    return validateS2Fixture(fixture, counters, issues);
  }
  if (fixture.slice === 's3') {
    return validateS3Fixture(fixture, counters, issues);
  }
  const machine = new ProtocolMachine();
  const records = new Map();
  let fixtureOk = true;
  for (const [index, step] of fixture.steps.entries()) {
    const stepLabel = `${fixtureLabel}#${index + 1}`;
    try {
      if (step === null || typeof step !== 'object' || Array.isArray(step)) {
        throw new Error('step is not an object');
      }
      const stepKeys = ['id', 'action', 'operation', 'body', 'server', 'expected'];
      if (!deepEqual(Object.keys(step).sort(), [...stepKeys].sort())) {
        throw new Error(`step keys must be ${stepKeys.join(',')}`);
      }
      if (typeof step.id !== 'string' || step.id.length === 0 || records.has(step.id)) {
        throw new Error('step id must be unique and non-empty');
      }
      if (step.action !== 'request') {
        throw new Error(`unsupported step action ${step.action}`);
      }
      if (!S1_OPERATIONS.includes(step.operation)) {
        throw new Error(`unknown operation ${step.operation}`);
      }
      if (step.operation !== 'resolveIvrInbound' && step.operation !== 'confirmIvrActivation' && step.operation !== 'authorizeIvrDirectOutbound') {
        throw new Error(`S1 fixtures may call only decision operations, got ${step.operation}`);
      }
      if (step.body === null || typeof step.body !== 'object' || Array.isArray(step.body)) {
        throw new Error('body must be an object');
      }
      if (step.server === null || typeof step.server !== 'object' || Array.isArray(step.server)) {
        throw new Error('server context must be an object');
      }
      const bodyRequirements = {
        resolveIvrInbound: ['callId', 'purpose', 'protocolVersion'],
        confirmIvrActivation: ['eventId', 'policyCode', 'policyVersion', 'promptVersion', 'contentSha256', 'decision'],
        authorizeIvrDirectOutbound: ['eventId', 'callee', 'promptVersion', 'confirmationKey', 'confirmationContent']
      };
      for (const key of bodyRequirements[step.operation]) {
        if (!(key in step.body)) {
          throw new Error(`body is missing ${key}`);
        }
      }
      if (step.operation !== 'resolveIvrInbound' && (typeof step.body.eventId !== 'string' || step.body.eventId.length < 1 || step.body.eventId.length > 128)) {
        throw new Error('eventId must be a 1-128 string');
      }
      if (step.expected === null || typeof step.expected !== 'object' || Array.isArray(step.expected)) {
        throw new Error('expected must be an object');
      }
      const expectedKeys = ['status', 'response', 'code', 'responseEqualsStep', 'persisted', 'counts'];
      for (const key of Object.keys(step.expected)) {
        if (!expectedKeys.includes(key)) {
          throw new Error(`unknown expected key ${key}`);
        }
      }
      if (typeof step.expected.status !== 'number' || !step.expected.persisted || !step.expected.counts) {
        throw new Error('expected must include status, persisted and counts');
      }
      const targetCallId = step.server.callId || step.body.callId;
      if (!targetCallId) {
        throw new Error('step must identify the target call through body.callId or server.callId');
      }
      const result = machine.handle(step.operation, targetCallId, step.body, step.server);
      if (result.status !== step.expected.status) {
        throw new Error(`status ${result.status} != expected ${step.expected.status}`);
      }
      if (step.expected.code !== undefined) {
        if (result.problem?.code !== step.expected.code) {
          throw new Error(`problem ${result.problem?.code} != expected ${step.expected.code}`);
        }
      } else if (result.problem) {
        throw new Error(`unexpected problem ${result.problem.code}`);
      }
      if (result.status === 200) {
        const purpose = step.operation === 'resolveIvrInbound' ? 'INBOUND_RESOLVE' : step.operation === 'confirmIvrActivation' ? 'ACTIVATION_NEXT_ACTION' : 'DIRECT_AUTHORIZATION';
        validateDecisionShape(result.response, purpose);
        if (step.expected.responseEqualsStep !== undefined) {
          const prior = records.get(step.expected.responseEqualsStep);
          if (!prior || !prior.response) {
            throw new Error(`responseEqualsStep ${step.expected.responseEqualsStep} has no prior response`);
          }
          if (!deepEqual(result.response, prior.response)) {
            throw new Error('replayed response does not equal the first frozen response');
          }
        } else if (step.expected.response !== undefined) {
          if (!subsetEqual(step.expected.response, result.response)) {
            throw new Error('response subset mismatch');
          }
        } else {
          throw new Error('200 step must assert a response or responseEqualsStep');
        }
      } else if (step.expected.response !== undefined || step.expected.responseEqualsStep !== undefined) {
        throw new Error('non-200 step must not assert a response');
      }
      const persisted = machine.projectCall(targetCallId);
      if (!deepEqual(step.expected.persisted, persisted)) {
        throw new Error('persisted snapshot mismatch');
      }
      const counts = machine.countSnapshot();
      if (!deepEqual(step.expected.counts, counts)) {
        throw new Error('fact counts mismatch');
      }
      records.set(step.id, { response: result.response });
    } catch (error) {
      fixtureOk = false;
      issues.push(`${stepLabel}: ${error.message}`);
    }
  }
  if (fixtureOk) {
    counters.passed += 1;
  } else {
    counters.failed += 1;
  }
  return fixtureOk;
}

function validateReceiptShape(receipt) {
  if (receipt === null || typeof receipt !== 'object' || Array.isArray(receipt)) {
    throw new Error('event receipt must be an object');
  }
  for (const key of ['callId', 'eventId', 'processingStatus', 'receivedAt']) {
    if (!Object.prototype.hasOwnProperty.call(receipt, key)) {
      throw new Error(`event receipt missing ${key}`);
    }
  }
  if (!['APPLIED', 'QUARANTINED', 'IGNORED'].includes(receipt.processingStatus)) {
    throw new Error(`unexpected processingStatus ${receipt.processingStatus}`);
  }
  if (typeof receipt.receivedAt !== 'string') {
    throw new Error('event receipt receivedAt must be a string');
  }
}

function validateS2Fixture(fixture, counters, issues) {
  const fixtureLabel = fixture.id;
  const machine = new ProtocolMachine();
  const records = new Map();
  const requestOperations = new Set(['resolveIvrInbound', 'confirmIvrActivation', 'authorizeIvrDirectOutbound', 'ingestIvrCallEvent']);
  const localOperations = new Set(['bleg.prepare', 'bleg.dispatch', 'bleg.reconcile', 'bleg.expire', 'ivr.restart']);
  let fixtureOk = true;
  for (const [index, step] of fixture.steps.entries()) {
    const stepLabel = `${fixtureLabel}#${index + 1}`;
    try {
      if (step === null || typeof step !== 'object' || Array.isArray(step)) {
        throw new Error('step is not an object');
      }
      if (typeof step.id !== 'string' || step.id.length === 0 || records.has(step.id)) {
        throw new Error('step id must be unique and non-empty');
      }
      const action = step.action;
      if (!['request', 'local', 'lineOutcome', 'restart', 'assert'].includes(action)) {
        throw new Error(`unsupported step action ${action}`);
      }
      const expectedKeys = ['status', 'response', 'code', 'responseEqualsStep', 'persisted', 'counts'];
      if (step.expected === null || typeof step.expected !== 'object' || Array.isArray(step.expected)) {
        throw new Error('expected must be an object');
      }
      for (const key of Object.keys(step.expected)) {
        if (!expectedKeys.includes(key)) {
          throw new Error(`unknown expected key ${key}`);
        }
      }
      if (typeof step.expected.status !== 'number' || !step.expected.persisted || !step.expected.counts) {
        throw new Error('expected must include status, persisted and counts');
      }
      const allowedKeys = action === 'request' && step.delivery !== undefined
        ? ['id', 'action', 'operation', 'body', 'server', 'expected', 'delivery']
        : action === 'request'
          ? ['id', 'action', 'operation', 'body', 'server', 'expected']
          : action === 'local'
            ? ['id', 'action', 'operation', 'body', 'expected']
            : ['id', 'action', 'body', 'expected'];
      if (!deepEqual(Object.keys(step).sort(), [...allowedKeys].sort())) {
        throw new Error(`step keys must be ${allowedKeys.join(',')}`);
      }
      if (step.body === null || typeof step.body !== 'object' || Array.isArray(step.body)) {
        throw new Error('body must be an object');
      }
      const targetCallId = step.body.callId || step.server?.callId;
      if (!targetCallId) {
        throw new Error('step must identify the target call through body.callId');
      }
      let result;
      if (action === 'request') {
        if (!requestOperations.has(step.operation)) {
          throw new Error(`S2 fixtures may call only decision and call-event operations, got ${step.operation}`);
        }
        if (step.server === null || typeof step.server !== 'object' || Array.isArray(step.server)) {
          throw new Error('server context must be an object');
        }
        const bodyRequirements = {
          resolveIvrInbound: ['callId', 'purpose', 'protocolVersion'],
          confirmIvrActivation: ['eventId', 'policyCode', 'policyVersion', 'promptVersion', 'contentSha256', 'decision'],
          authorizeIvrDirectOutbound: ['eventId', 'callee', 'promptVersion', 'confirmationKey', 'confirmationContent'],
          ingestIvrCallEvent: ['eventId', 'eventType', 'occurredAt', 'payload']
        };
        for (const key of bodyRequirements[step.operation]) {
          if (!(key in step.body)) {
            throw new Error(`body is missing ${key}`);
          }
        }
        let delivery = {};
        if (step.delivery !== undefined) {
          if (step.delivery === null || typeof step.delivery !== 'object' || Array.isArray(step.delivery)) {
            throw new Error('delivery must be an object');
          }
          for (const key of Object.keys(step.delivery)) {
            if (!['responseLost', 'commitFailure'].includes(key)) {
              throw new Error(`unknown delivery key ${key}`);
            }
            if (typeof step.delivery[key] !== 'boolean') {
              throw new Error(`delivery.${key} must be boolean`);
            }
          }
          delivery = step.delivery;
        }
        result = machine.handle(step.operation, targetCallId, step.body, step.server, delivery);
      } else if (action === 'local') {
        if (!localOperations.has(step.operation)) {
          throw new Error(`unknown local operation ${step.operation}`);
        }
        result = machine.handleLocal(step.operation, targetCallId, step.body);
      } else if (action === 'lineOutcome') {
        result = machine.handleLineOutcome(targetCallId, step.body);
      } else {
        result = { status: 200 };
      }
      if (result.status !== step.expected.status) {
        throw new Error(`status ${result.status} != expected ${step.expected.status}`);
      }
      const actualCode = result.problem?.code ?? result.code;
      if (step.expected.code !== undefined) {
        if (actualCode !== step.expected.code) {
          throw new Error(`code ${actualCode} != expected ${step.expected.code}`);
        }
      } else if (actualCode !== undefined) {
        throw new Error(`unexpected code ${actualCode}`);
      }
      if (action === 'request' && result.status === 200) {
        if (step.operation === 'ingestIvrCallEvent') {
          validateReceiptShape(result.response);
        } else {
          const purpose = step.operation === 'resolveIvrInbound' ? 'INBOUND_RESOLVE' : step.operation === 'confirmIvrActivation' ? 'ACTIVATION_NEXT_ACTION' : 'DIRECT_AUTHORIZATION';
          validateDecisionShape(result.response, purpose);
        }
        if (step.expected.responseEqualsStep !== undefined) {
          const prior = records.get(step.expected.responseEqualsStep);
          if (!prior || !prior.response) {
            throw new Error(`responseEqualsStep ${step.expected.responseEqualsStep} has no prior response`);
          }
          if (!deepEqual(result.response, prior.response)) {
            throw new Error('replayed response does not equal the first frozen response');
          }
        } else if (step.expected.response !== undefined) {
          if (!subsetEqual(step.expected.response, result.response)) {
            throw new Error('response subset mismatch');
          }
        } else {
          throw new Error('200 request step must assert a response or responseEqualsStep');
        }
      } else if (step.expected.response !== undefined || step.expected.responseEqualsStep !== undefined) {
        throw new Error('non-200 or non-request step must not assert a response');
      }
      const persisted = machine.projectExecution(targetCallId);
      if (!subsetEqual(step.expected.persisted, persisted)) {
        throw new Error('persisted projection assertion mismatch');
      }
      const counts = machine.executionCountSnapshot();
      if (!subsetEqual(step.expected.counts, counts)) {
        throw new Error('fact counter assertion mismatch');
      }
      records.set(step.id, { response: result.response });
    } catch (error) {
      fixtureOk = false;
      issues.push(`${stepLabel}: ${error.message}`);
    }
  }
  if (fixtureOk) {
    counters.passed += 1;
  } else {
    counters.failed += 1;
  }
  return fixtureOk;
}

function assertNoForbiddenMediaFields(value, location = 'response') {
  if (value === null || typeof value !== 'object') {
    return;
  }
  if (Array.isArray(value)) {
    for (const [index, item] of value.entries()) {
      assertNoForbiddenMediaFields(item, `${location}[${index}]`);
    }
    return;
  }
  for (const [key, child] of Object.entries(value)) {
    if (RECORDING_FORBIDDEN_RESPONSE_FIELDS.has(key)) {
      throw new Error(`${location} exposes forbidden media field ${key}`);
    }
    assertNoForbiddenMediaFields(child, `${location}.${key}`);
  }
}

function validateDeclarationShape(response) {
  if (response === null || typeof response !== 'object' || Array.isArray(response)) {
    throw new Error('recording declaration must be an object');
  }
  for (const key of ['recordingId', 'status', 'uploadExpiresAt', 'upload']) {
    if (!Object.prototype.hasOwnProperty.call(response, key)) {
      throw new Error(`recording declaration missing ${key}`);
    }
  }
  if (response.status !== 'DECLARED') {
    throw new Error('recording declaration status must be DECLARED');
  }
  if (typeof response.uploadExpiresAt !== 'string') {
    throw new Error('recording declaration uploadExpiresAt must be a string');
  }
  if (response.upload === null || typeof response.upload !== 'object' || Array.isArray(response.upload)) {
    throw new Error('recording declaration upload must be an object');
  }
  if (typeof response.upload.credential !== 'string' || response.upload.credential.length < 1 || response.upload.credential.length > 2048) {
    throw new Error('recording declaration upload.credential must be a 1-2048 string');
  }
  if (typeof response.upload.expiresAt !== 'string' || response.upload.expiresAt !== response.uploadExpiresAt) {
    throw new Error('recording declaration upload.expiresAt must equal uploadExpiresAt');
  }
  assertNoForbiddenMediaFields(response);
}

function validateCompletionShape(response) {
  if (response === null || typeof response !== 'object' || Array.isArray(response)) {
    throw new Error('recording completion must be an object');
  }
  for (const key of ['recordingId', 'status']) {
    if (!Object.prototype.hasOwnProperty.call(response, key)) {
      throw new Error(`recording completion missing ${key}`);
    }
  }
  if (!['VALIDATED', 'QUARANTINED'].includes(response.status)) {
    throw new Error(`unexpected recording completion status ${response.status}`);
  }
  if (response.status === 'VALIDATED') {
    if (!Number.isInteger(response.validatedDurationSeconds) || response.validatedDurationSeconds < 1) {
      throw new Error('validated completion requires a positive validatedDurationSeconds');
    }
    if (typeof response.validatedSha256 !== 'string' || !/^[0-9a-f]{64}$/.test(response.validatedSha256)) {
      throw new Error('validated completion requires validatedSha256');
    }
  }
  if (typeof response.completedAt !== 'string') {
    throw new Error('recording completion requires completedAt');
  }
  assertNoForbiddenMediaFields(response);
}

function validateS3Fixture(fixture, counters, issues) {
  const fixtureLabel = fixture.id;
  const machine = new ProtocolMachine();
  const records = new Map();
  const requestOperations = new Set(['resolveIvrInbound', 'confirmIvrActivation', 'authorizeIvrDirectOutbound', 'ingestIvrCallEvent', 'declareIvrRecording', 'completeIvrRecording']);
  const localOperations = new Set(['bleg.prepare', 'bleg.dispatch', 'bleg.reconcile', 'bleg.expire', 'recording.observeUpload', 'ivr.restart']);
  let fixtureOk = true;
  for (const [index, step] of fixture.steps.entries()) {
    const stepLabel = `${fixtureLabel}#${index + 1}`;
    try {
      if (step === null || typeof step !== 'object' || Array.isArray(step)) {
        throw new Error('step is not an object');
      }
      if (typeof step.id !== 'string' || step.id.length === 0 || records.has(step.id)) {
        throw new Error('step id must be unique and non-empty');
      }
      const action = step.action;
      if (!['request', 'local', 'lineOutcome', 'restart', 'assert'].includes(action)) {
        throw new Error(`unsupported step action ${action}`);
      }
      const expectedKeys = ['status', 'response', 'code', 'responseEqualsStep', 'persisted', 'counts'];
      if (step.expected === null || typeof step.expected !== 'object' || Array.isArray(step.expected)) {
        throw new Error('expected must be an object');
      }
      for (const key of Object.keys(step.expected)) {
        if (!expectedKeys.includes(key)) {
          throw new Error(`unknown expected key ${key}`);
        }
      }
      if (typeof step.expected.status !== 'number' || !step.expected.persisted || !step.expected.counts) {
        throw new Error('expected must include status, persisted and counts');
      }
      const allowedKeys = action === 'request' && step.delivery !== undefined
        ? ['id', 'action', 'operation', 'body', 'server', 'expected', 'delivery']
        : action === 'request'
          ? ['id', 'action', 'operation', 'body', 'server', 'expected']
          : action === 'local'
            ? ['id', 'action', 'operation', 'body', 'expected']
            : ['id', 'action', 'body', 'expected'];
      if (!deepEqual(Object.keys(step).sort(), [...allowedKeys].sort())) {
        throw new Error(`step keys must be ${allowedKeys.join(',')}`);
      }
      if (step.body === null || typeof step.body !== 'object' || Array.isArray(step.body)) {
        throw new Error('body must be an object');
      }
      const targetCallId = step.body.callId || step.server?.callId;
      if (!targetCallId) {
        throw new Error('step must identify the target call through body.callId');
      }
      let result;
      if (action === 'request') {
        if (!requestOperations.has(step.operation)) {
          throw new Error(`unknown request operation ${step.operation}`);
        }
        if (step.server === null || typeof step.server !== 'object' || Array.isArray(step.server)) {
          throw new Error('server context must be an object');
        }
        const bodyRequirements = {
          resolveIvrInbound: ['callId', 'purpose', 'protocolVersion'],
          confirmIvrActivation: ['eventId', 'policyCode', 'policyVersion', 'promptVersion', 'contentSha256', 'decision'],
          authorizeIvrDirectOutbound: ['eventId', 'callee', 'promptVersion', 'confirmationKey', 'confirmationContent'],
          ingestIvrCallEvent: ['eventId', 'eventType', 'occurredAt', 'payload'],
          declareIvrRecording: ['callId', 'recordingId', 'mimeType', 'sizeBytes', 'durationSeconds', 'sha256'],
          completeIvrRecording: ['callId', 'recordingId']
        };
        for (const key of bodyRequirements[step.operation]) {
          if (!(key in step.body)) {
            throw new Error(`body is missing ${key}`);
          }
        }
        let delivery = {};
        if (step.delivery !== undefined) {
          if (step.delivery === null || typeof step.delivery !== 'object' || Array.isArray(step.delivery)) {
            throw new Error('delivery must be an object');
          }
          for (const key of Object.keys(step.delivery)) {
            if (!['responseLost', 'commitFailure'].includes(key)) {
              throw new Error(`unknown delivery key ${key}`);
            }
            if (typeof step.delivery[key] !== 'boolean') {
              throw new Error(`delivery.${key} must be boolean`);
            }
          }
          delivery = step.delivery;
        }
        result = machine.handle(step.operation, targetCallId, step.body, step.server, delivery);
      } else if (action === 'local') {
        if (!localOperations.has(step.operation)) {
          throw new Error(`unknown local operation ${step.operation}`);
        }
        if (step.operation === 'recording.observeUpload') {
          for (const key of ['callId', 'recordingId', 'observed', 'now']) {
            if (!(key in step.body)) {
              throw new Error(`observation body is missing ${key}`);
            }
          }
        }
        result = machine.handleLocal(step.operation, targetCallId, step.body);
      } else if (action === 'lineOutcome') {
        result = machine.handleLineOutcome(targetCallId, step.body);
      } else {
        result = { status: 200 };
      }
      if (result.status !== step.expected.status) {
        throw new Error(`status ${result.status} != expected ${step.expected.status}`);
      }
      const actualCode = result.problem?.code ?? result.code;
      if (step.expected.code !== undefined) {
        if (actualCode !== step.expected.code) {
          throw new Error(`code ${actualCode} != expected ${step.expected.code}`);
        }
      } else if (actualCode !== undefined) {
        throw new Error(`unexpected code ${actualCode}`);
      }
      if (action === 'request' && (result.status === 200 || result.status === 201)) {
        if (step.operation === 'declareIvrRecording') {
          validateDeclarationShape(result.response);
        } else if (step.operation === 'completeIvrRecording') {
          validateCompletionShape(result.response);
        } else if (step.operation === 'ingestIvrCallEvent') {
          validateReceiptShape(result.response);
        } else {
          const purpose = step.operation === 'resolveIvrInbound' ? 'INBOUND_RESOLVE' : step.operation === 'confirmIvrActivation' ? 'ACTIVATION_NEXT_ACTION' : 'DIRECT_AUTHORIZATION';
          validateDecisionShape(result.response, purpose);
        }
        if (step.expected.responseEqualsStep !== undefined) {
          const prior = records.get(step.expected.responseEqualsStep);
          if (!prior || !prior.response) {
            throw new Error(`responseEqualsStep ${step.expected.responseEqualsStep} has no prior response`);
          }
          if (!deepEqual(result.response, prior.response)) {
            throw new Error('replayed response does not equal the first frozen response');
          }
        } else if (step.expected.response !== undefined) {
          if (!subsetEqual(step.expected.response, result.response)) {
            throw new Error('response subset mismatch');
          }
        } else {
          throw new Error('successful request step must assert a response or responseEqualsStep');
        }
      } else if (step.expected.response !== undefined || step.expected.responseEqualsStep !== undefined) {
        throw new Error('non-success or non-request step must not assert a response');
      }
      const persisted = machine.projectExecution(targetCallId);
      assertNoForbiddenMediaFields(persisted, 'persisted');
      if (!subsetEqual(step.expected.persisted, persisted)) {
        throw new Error('persisted projection assertion mismatch');
      }
      const counts = machine.executionCountSnapshot();
      if (!subsetEqual(step.expected.counts, counts)) {
        throw new Error('fact counter assertion mismatch');
      }
      records.set(step.id, { response: result.response });
    } catch (error) {
      fixtureOk = false;
      issues.push(`${stepLabel}: ${error.message}`);
    }
  }
  if (fixtureOk) {
    counters.passed += 1;
  } else {
    counters.failed += 1;
  }
  return fixtureOk;
}

function main() {
  const counters = {
    doc_missing: 0,
    doc_extra: 0,
    doc_duplicate: 0,
    doc_invalid: 0,
    token_missing: 0,
    stage_field_missing: 0,
    stage_field_premature: 0,
    fixture_id_missing: 0,
    fixture_id_extra: 0,
    a1_drift: 0,
    a0_status_drift: 0,
    a0_registry_drift: 0,
    a0_column_drift: 0,
    passed: 0,
    failed: 0
  };
  const issues = [];
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.stdout.write('FAIL protocol-validation (invalid-arguments)\n');
    process.exit(1);
  }
  const config = STAGE_CONFIG[args.stage];

  let model;
  let bundle;
  let doc;
  try {
    model = JSON.parse(readUtf8(args.model));
    bundle = JSON.parse(readUtf8(args.a1Bundle));
    doc = readUtf8(args.spec);
    readUtf8(args.a0);
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.stdout.write(`FAIL protocol-validation (${args.stage})\n`);
    process.exit(1);
  }
  const a0Doc = readUtf8(args.a0);

  const modelKeys = Object.keys(model);
  for (const key of config.topLevelRequired) {
    if (!modelKeys.includes(key)) {
      counters.stage_field_missing += 1;
      issues.push(`model: missing required section ${key}`);
    }
  }
  for (const key of modelKeys) {
    if (!config.topLevelRequired.includes(key)) {
      counters.stage_field_premature += 1;
      issues.push(`model: premature section ${key}`);
    }
  }
  for (const key of config.topLevelForbidden) {
    if (modelKeys.includes(key)) {
      counters.stage_field_premature += 1;
      issues.push(`model: forbidden section ${key}`);
    }
  }
  walkKeys(model, (key) => {
    if (config.forbiddenNestedKeys.includes(key)) {
      counters.stage_field_premature += 1;
      issues.push(`model: premature nested field ${key}`);
    }
  });

  if (!deepEqual(model.messages, EXPECTED_MESSAGES)) {
    counters.stage_field_missing += 1;
    issues.push('model.messages does not exactly match the accepted 12-message registry');
  }
  if (!Array.isArray(model.messages) || model.messages.length !== 12) {
    counters.stage_field_missing += 1;
    issues.push(`model.messages count is ${Array.isArray(model.messages) ? model.messages.length : 'not an array'}, expected 12`);
  }
  for (const [index, message] of (Array.isArray(model.messages) ? model.messages : []).entries()) {
    requireExactKeys(message, ['messageId', 'slice', 'operationId', 'methodPath', 'requestSchema', 'responseSchema', 'identity'], `model.messages[${index}]`, counters, issues);
  }

  if (!deepEqual(model.identities, EXPECTED_IDENTITIES)) {
    counters.stage_field_missing += 1;
    issues.push('model.identities does not exactly match the accepted 22-identity registry');
  }
  if (!Array.isArray(model.identities) || model.identities.length !== 22) {
    counters.stage_field_missing += 1;
    issues.push(`model.identities count is ${Array.isArray(model.identities) ? model.identities.length : 'not an array'}, expected 22`);
  }
  for (const [index, identity] of (Array.isArray(model.identities) ? model.identities : []).entries()) {
    requireExactKeys(identity, ['identity', 'owner', 'uniqueness', 'precondition', 'replayConflict', 'a0a1Mapping'], `model.identities[${index}]`, counters, issues);
  }

  const eventTypes = Array.isArray(model.eventTypes) ? model.eventTypes : [];
  if (eventTypes.length !== 11) {
    counters.stage_field_missing += 1;
    issues.push(`model.eventTypes count is ${eventTypes.length}, expected 11`);
  }
  const eventNames = eventTypes.map((row) => row.eventType);
  if (!deepEqual(eventNames, EXPECTED_EVENT_TYPES)) {
    counters.stage_field_missing += 1;
    issues.push('model.eventTypes names or order do not match the accepted 11-event registry');
  }
  for (const [index, eventType] of eventTypes.entries()) {
    if (!requireExactKeys(eventType, config.eventColumnKeys, `model.eventTypes[${index}]`, counters, issues)) {
      continue;
    }
    if (args.stage === 's1' && eventType.payloadState !== 'DEFERRED_S2') {
      counters.stage_field_premature += 1;
      issues.push(`model.eventTypes[${index}]: payloadState must be DEFERRED_S2`);
    }
  }
  if (args.stage !== 's1') {
    if (!deepEqual(eventTypes, EXPECTED_EVENT_ROWS)) {
      counters.stage_field_missing += 1;
      issues.push('model.eventTypes does not exactly match the frozen S2 four-column event registry');
    }
    validateS2Registries(model, counters, issues);
  }

  if (!Array.isArray(model.invariants) || model.invariants.length !== 15) {
    counters.stage_field_missing += 1;
    issues.push(`model.invariants count is ${Array.isArray(model.invariants) ? model.invariants.length : 'not an array'}, expected 15`);
  } else if (!deepEqual(model.invariants.map((item) => item.id), EXPECTED_INVARIANTS)) {
    counters.stage_field_missing += 1;
    issues.push('model.invariants IDs or order do not match INV-A2-01..15');
  }

  if (!Array.isArray(model.actions) || !Array.isArray(model.purposes) || !Array.isArray(model.decisionReasons)) {
    counters.stage_field_missing += 1;
    issues.push('model action/purpose/reason registries are not arrays');
  }
  const preconditions = model.preconditions || {};
  const pathIds = Array.isArray(preconditions.decisionPaths) ? preconditions.decisionPaths.map((entry) => entry.id) : [];
  if (!deepEqual(pathIds, EXPECTED_PRECONDITION_IDS)) {
    counters.stage_field_missing += 1;
    issues.push('model.preconditions.decisionPaths IDs or order do not match the S1 decision paths');
  }
  if (!preconditions.protocolVersion || typeof preconditions.protocolVersion !== 'object') {
    counters.stage_field_missing += 1;
    issues.push('model.preconditions.protocolVersion is missing');
  }
  if (!model.decisionFields || !Array.isArray(model.decisionFields.required) || !Array.isArray(model.decisionFields.fields) || !model.decisionFields.enums) {
    counters.stage_field_missing += 1;
    issues.push('model.decisionFields is incomplete');
  }

  const operationMap = new Map();
  for (const [operationPath, item] of Object.entries(bundle.paths || {})) {
    for (const method of HTTP_METHODS) {
      const operation = item[method];
      if (operation && typeof operation === 'object') {
        operationMap.set(operation.operationId, { method, path: operationPath, operation });
      }
    }
  }
  const a1OperationCount = operationMap.size;
  const ivrOperations = [...operationMap.entries()].filter(([, value]) => Array.isArray(value.operation.tags) && value.operation.tags.includes('ivr'));
  if (a1OperationCount !== 49) {
    counters.a1_drift += 1;
    issues.push(`A1 bundle has ${a1OperationCount} operations, expected 49`);
  }
  if (ivrOperations.length !== 6) {
    counters.a1_drift += 1;
    issues.push(`A1 bundle has ${ivrOperations.length} IVR operations, expected 6`);
  }
  for (const [operationId, contract] of Object.entries(A1_OPERATION_CONTRACT)) {
    const found = operationMap.get(operationId);
    if (!found) {
      counters.a1_drift += 1;
      issues.push(`A1 operation ${operationId} is missing`);
      continue;
    }
    if (found.method !== contract.method || found.path !== contract.path) {
      counters.a1_drift += 1;
      issues.push(`A1 operation ${operationId} method/path drift`);
    }
    if (requestSchemaName(found.operation) !== contract.requestSchema) {
      counters.a1_drift += 1;
      issues.push(`A1 operation ${operationId} request schema drift`);
    }
    const actualResponse = successResponse(found.operation);
    const actualResponseSchema = responseSchemaName(found.operation);
    if (!actualResponse || actualResponseSchema !== contract.responseSchema) {
      counters.a1_drift += 1;
      issues.push(`A1 operation ${operationId} success response schema drift`);
    }
  }
  const ivrPaths = [...operationMap.values()].filter((value) => value.path.startsWith('/api/v1/ivr/'));
  const expectedIvrPaths = new Set(Object.values(A1_OPERATION_CONTRACT).map((contract) => contract.path));
  for (const value of ivrPaths) {
    if (!expectedIvrPaths.has(value.path)) {
      counters.a1_drift += 1;
      issues.push(`A1 has an unregistered IVR path ${value.path}`);
    }
  }
  const schemaMap = bundle.components?.schemas || {};
  for (const schema of A1_REQUIRED_SCHEMAS) {
    if (!schemaMap[schema]) {
      counters.a1_drift += 1;
      issues.push(`A1 schema ${schema} is missing`);
    }
  }
  for (const [schemaNameValue, requiredFields] of Object.entries(A1_REQUIRED_FIELDS)) {
    const schema = schemaMap[schemaNameValue];
    if (!schema) {
      continue;
    }
    if (!deepEqual([...(schema.required || [])].sort(), [...requiredFields].sort())) {
      counters.a1_drift += 1;
      issues.push(`A1 schema ${schemaNameValue} required fields drift`);
    }
  }
  const ivrDecision = schemaMap.IvrDecision;
  if (ivrDecision) {
    if (!deepEqual([...(ivrDecision.required || [])], model.decisionFields?.required)) {
      counters.a1_drift += 1;
      issues.push('IvrDecision required fields do not match model.decisionFields.required');
    }
    const properties = ivrDecision.properties || {};
    for (const field of model.decisionFields?.fields || []) {
      const [root, child] = field.split('.');
      if (!properties[root] || (child && !properties[root].properties?.[child])) {
        counters.a1_drift += 1;
        issues.push(`IvrDecision is missing model decision field ${field}`);
      }
    }
    const actionValues = properties.action?.enum || [];
    const nonNullActions = actionValues.filter((value) => value !== null);
    if (!deepEqual(nonNullActions, model.actions)) {
      counters.a1_drift += 1;
      issues.push('IvrDecision action enum does not match model.actions');
    }
    if (!actionValues.includes(null)) {
      counters.a1_drift += 1;
      issues.push('IvrDecision action enum does not include null');
    }
    if (!deepEqual(properties.purpose?.enum || [], model.purposes)) {
      counters.a1_drift += 1;
      issues.push('IvrDecision purpose enum does not match model.purposes');
    }
    if (!deepEqual(properties.reasonCode?.enum || [], model.decisionReasons)) {
      counters.a1_drift += 1;
      issues.push('IvrDecision reasonCode enum does not match model.decisionReasons');
    }
    if (!deepEqual(properties.callerState?.enum || [], model.decisionFields?.enums?.callerState)) {
      counters.a1_drift += 1;
      issues.push('IvrDecision callerState enum drift');
    }
    if (!deepEqual(properties.taskMatch?.enum || [], model.decisionFields?.enums?.taskMatch)) {
      counters.a1_drift += 1;
      issues.push('IvrDecision taskMatch enum drift');
    }
    if (properties.task?.properties?.calleeNumber?.['x-onecall-internal-payload'] !== true) {
      counters.a1_drift += 1;
      issues.push('IvrDecision task.calleeNumber is not marked internal');
    }
    if (properties.authorization?.properties?.calleeNumber?.['x-onecall-internal-payload'] !== true) {
      counters.a1_drift += 1;
      issues.push('IvrDecision authorization.calleeNumber is not marked internal');
    }
    if (properties.protocolVersion !== undefined) {
      counters.a1_drift += 1;
      issues.push('IvrDecision must not expose protocolVersion');
    }
  }
  const resolveRequest = schemaMap.IvrInboundResolveRequest;
  if (resolveRequest?.properties?.protocolVersion) {
    if (resolveRequest.properties.protocolVersion.minLength !== 1 || resolveRequest.properties.protocolVersion.maxLength !== 32) {
      counters.a1_drift += 1;
      issues.push('IvrInboundResolveRequest protocolVersion length bounds drift');
    }
  }
  if (schemaMap.IvrActivationConfirmRequest?.properties?.decision?.enum?.join(',') !== 'ACCEPT') {
    counters.a1_drift += 1;
    issues.push('IvrActivationConfirmRequest decision enum must be ACCEPT');
  }
  if (!schemaMap.IvrCallEventRequest?.properties?.payload || schemaMap.IvrEventPayload?.['x-onecall-opaque'] !== true) {
    counters.a1_drift += 1;
    issues.push('IvrEventPayload is not opaque');
  }
  const problemCodeEnum = schemaMap.ProblemCode?.enum || [];
  const reasonTokens = new Set(model.decisionReasons || []);
  for (const forbidden of ['TASK_CONFLICT', 'CALL_IN_PROGRESS', 'CONFIRMATION_NOT_ALLOWED']) {
    if (reasonTokens.has(forbidden) && problemCodeEnum.includes(forbidden)) {
      counters.a1_drift += 1;
      issues.push(`domain decision reason ${forbidden} must not be a Problem code`);
    }
  }
  for (const [operationId, codes] of Object.entries(model.errors?.operations || {})) {
    const found = operationMap.get(operationId);
    if (!found) {
      counters.a1_drift += 1;
      issues.push(`model errors reference unknown A1 operation ${operationId}`);
      continue;
    }
    const actualCodes = found.operation['x-onecall-error-codes'] || [];
    if (!deepEqual(codes, actualCodes)) {
      counters.a1_drift += 1;
      issues.push(`model error set drift for ${operationId}`);
    }
    for (const code of codes) {
      if (!problemCodeEnum.includes(code)) {
        counters.a1_drift += 1;
        issues.push(`model error code ${code} is not in A1 ProblemCode`);
      }
    }
  }
  for (const [code, status] of Object.entries(model.errors?.problemCodes || {})) {
    if (!problemCodeEnum.includes(code)) {
      counters.a1_drift += 1;
      issues.push(`model problem code ${code} is not in A1 ProblemCode`);
    }
    if (typeof status !== 'number' || status < 400 || status > 599) {
      counters.a1_drift += 1;
      issues.push(`model problem code ${code} has invalid status ${status}`);
    }
  }
  for (const operationId of S1_OPERATIONS) {
    if (!model.errors?.operations?.[operationId]) {
      counters.a1_drift += 1;
      issues.push(`model errors omit ${operationId}`);
    }
  }

  const catalogTables = parseA0Catalog(a0Doc);
  const a0Tables = catalogTables.length;
  if (a0Tables !== 44) {
    counters.a0_status_drift += 1;
    issues.push(`A0 catalog has ${a0Tables} tables, expected 44`);
  }
  const a0Checks = parseA0Checks(a0Doc);
  const a0Columns = parseA0Columns(a0Doc);
  const a0DecisionFields = parseA0DecisionFields(a0Doc);
  if (args.stage === 's3') {
    validateS3Registries(model, a0Checks, counters, issues);
  }
  for (const checkName of A0_ALL_CHECKS) {
    if (!a0Checks.has(checkName)) {
      if (A0_STATUS_CHECKS.includes(checkName)) {
        counters.a0_status_drift += 1;
      } else {
        counters.a0_registry_drift += 1;
      }
      issues.push(`A0 named CHECK ${checkName} is missing`);
      continue;
    }
    const modelValues = preconditions.a0Registries?.[checkName];
    if (!Array.isArray(modelValues) || !setEqual(modelValues, a0Checks.get(checkName))) {
      if (A0_STATUS_CHECKS.includes(checkName)) {
        counters.a0_status_drift += 1;
      } else {
        counters.a0_registry_drift += 1;
      }
      issues.push(`model registry ${checkName} does not equal A0`);
    }
  }
  for (const checkName of Object.keys(preconditions.a0Registries || {})) {
    if (!A0_ALL_CHECKS.includes(checkName)) {
      counters.a0_registry_drift += 1;
      issues.push(`model has unexpected A0 registry ${checkName}`);
    }
  }
  if (a0Checks.has('ck_ivr_decision_action') && !setEqual(a0Checks.get('ck_ivr_decision_action'), model.actions || [])) {
    counters.a0_registry_drift += 1;
    issues.push('model.actions does not equal ck_ivr_decision_action');
  }
  if (a0Checks.has('ck_ivr_decision_purpose') && !setEqual(a0Checks.get('ck_ivr_decision_purpose'), model.purposes || [])) {
    counters.a0_registry_drift += 1;
    issues.push('model.purposes does not equal ck_ivr_decision_purpose');
  }
  if (a0Checks.has('ck_ivr_decision_reason') && !setEqual(a0Checks.get('ck_ivr_decision_reason'), model.decisionReasons || [])) {
    counters.a0_registry_drift += 1;
    issues.push('model.decisionReasons does not equal ck_ivr_decision_reason');
  }
  if (preconditions.eventDomains?.usageFactSource !== 'IVR_EVENT') {
    counters.a0_registry_drift += 1;
    issues.push('model usage-fact source domain must be IVR_EVENT');
  }
  for (const value of Object.values(preconditions.eventDomains?.decisionSources || {})) {
    if (value !== null && !a0Doc.includes(value)) {
      counters.a0_registry_drift += 1;
      issues.push(`A0 does not contain decision source event domain ${value}`);
    }
  }
  for (const ref of preconditions.columnRefs || []) {
    if (!catalogTables.includes(ref.table)) {
      counters.a0_column_drift += 1;
      issues.push(`model column ref uses non-catalog A0 table ${ref.table}`);
      continue;
    }
    const columns = a0Columns.get(ref.table);
    if (!columns) {
      counters.a0_column_drift += 1;
      issues.push(`A0 has no column dictionary for ${ref.table}`);
      continue;
    }
    for (const column of ref.columns || []) {
      if (!columns.has(column)) {
        counters.a0_column_drift += 1;
        issues.push(`model references missing A0 column ${ref.table}.${column}`);
      }
    }
  }
  const allModelStrings = [];
  collectStrings(model, allModelStrings);
  for (const text of allModelStrings) {
    for (const match of text.matchAll(/\b([a-z_]+\.[a-z_]+\.[a-z_]+)\b/g)) {
      const token = match[1];
      const parts = token.split('.');
      const table = `${parts[0]}.${parts[1]}`;
      const column = parts[2];
      if (!catalogTables.includes(table)) {
        counters.a0_column_drift += 1;
        issues.push(`model string references non-catalog A0 table ${table}`);
        continue;
      }
      const columns = a0Columns.get(table);
      if (!columns || !columns.has(column)) {
        counters.a0_column_drift += 1;
        issues.push(`model string references missing A0 column ${token}`);
      }
    }
  }
  for (const field of model.decisionFields?.fields || []) {
    if (!a0DecisionFields.includes(field)) {
      counters.a0_column_drift += 1;
      issues.push(`A0 §10.14 does not define decision field ${field}`);
    }
  }

  const messageMarker = extractMarker(doc, 'protocol-message-map', counters, issues);
  const eventMarker = extractMarker(doc, 'protocol-event-map', counters, issues);
  const identityMarker = extractMarker(doc, 'protocol-identity-map', counters, issues);

  const markerDefinitions = [
    {
      name: 'protocol-message-map',
      marker: messageMarker,
      header: config.messageDocColumns,
      expectedRows: (model.messages || []).map((row) => config.messageDocColumns.map((column) => String(row[column]))),
      label: 'message'
    },
    {
      name: 'protocol-event-map',
      marker: eventMarker,
      header: config.eventDocColumns,
      expectedRows: (model.eventTypes || []).map((row) => config.eventDocColumns.map((column) => {
        if (column === 'payloadFields' && Array.isArray(row.payloadFields)) {
          return row.payloadFields.length === 0 ? '{}' : row.payloadFields.join(',');
        }
        return String(row[column]);
      })),
      label: 'event'
    },
    {
      name: 'protocol-identity-map',
      marker: identityMarker,
      header: config.identityDocColumns,
      expectedRows: (model.identities || []).map((row) => config.identityDocColumns.map((column) => String(row[column]))),
      label: 'identity'
    }
  ];
  for (const definition of markerDefinitions) {
    if (!definition.marker) {
      continue;
    }
    if (!deepEqual(definition.marker.header, definition.header)) {
      counters.doc_invalid += 1;
      issues.push(`${definition.name}: header must be ${definition.header.join(',')}`);
      continue;
    }
    const seen = new Set();
    for (const row of definition.marker.rows) {
      if (seen.has(row[0])) {
        counters.doc_duplicate += 1;
        issues.push(`${definition.name}: duplicate row ${row[0]}`);
      }
      seen.add(row[0]);
    }
    for (const expectedRow of definition.expectedRows) {
      if (!definition.marker.rows.some((row) => deepEqual(row, expectedRow))) {
        counters.doc_missing += 1;
        issues.push(`${definition.name}: missing row ${expectedRow[0]}`);
      }
    }
    for (const row of definition.marker.rows) {
      if (!definition.expectedRows.some((expectedRow) => deepEqual(row, expectedRow))) {
        counters.doc_extra += 1;
        issues.push(`${definition.name}: extra or drifted row ${row[0]}`);
      }
    }
    if (definition.marker.rows.length !== definition.expectedRows.length) {
      counters.doc_invalid += 1;
      issues.push(`${definition.name}: row count ${definition.marker.rows.length}, expected ${definition.expectedRows.length}`);
    }
    if (definition.marker.rows.length === definition.expectedRows.length && !deepEqual(definition.marker.rows, definition.expectedRows)) {
      counters.doc_invalid += 1;
      issues.push(`${definition.name}: row order or values drift from the model`);
    }
    if (definition.name === 'protocol-event-map' && definition.marker.header.length !== 2 && args.stage === 's1') {
      counters.stage_field_premature += 1;
      issues.push('S1 event marker block must contain only eventType and payloadState');
    }
  }
  if (args.stage === 's1') {
    const eventStart = doc.indexOf('<!-- protocol-event-map:start -->');
    const eventEnd = doc.indexOf('<!-- protocol-event-map:end -->');
    if (eventStart >= 0 && eventEnd > eventStart) {
      const adjacent = doc.slice(Math.max(0, eventStart - 2200), Math.min(doc.length, eventEnd + 2200));
      if (!/event identities only; payload\/state\/terminal semantics deferred to S2/i.test(adjacent)) {
        counters.doc_invalid += 1;
        issues.push('S1 event marker block lacks the required adjacent DEFERRED_S2 non-confusion statement');
      }
    }
  }

  const tokens = new Set();
  tokens.add(model.protocolVersion);
  for (const version of model.supportedVersions || []) {
    tokens.add(version);
  }
  for (const message of model.messages || []) {
    for (const key of ['messageId', 'operationId', 'requestSchema', 'responseSchema']) {
      if (message[key] && message[key] !== '-') {
        tokens.add(message[key]);
      }
    }
  }
  for (const eventType of model.eventTypes || []) {
    tokens.add(eventType.eventType);
    if (eventType.payloadState) {
      tokens.add(eventType.payloadState);
    }
  }
  for (const identity of model.identities || []) {
    tokens.add(identity.identity);
  }
  for (const value of model.actions || []) {
    tokens.add(value);
  }
  for (const value of model.purposes || []) {
    tokens.add(value);
  }
  for (const value of model.decisionReasons || []) {
    tokens.add(value);
  }
  for (const invariant of model.invariants || []) {
    tokens.add(invariant.id);
  }
  for (const checkName of A0_ALL_CHECKS) {
    tokens.add(checkName);
  }
  for (const code of Object.keys(model.errors?.problemCodes || {})) {
    tokens.add(code);
  }
  for (const pathId of pathIds) {
    tokens.add(pathId);
  }
  tokens.add('R-A2-A0-TASKID-TYPE');
  tokens.add('task.taskId');
  if (args.stage !== 's1') {
    for (const reason of model.endReasons || []) {
      tokens.add(reason);
    }
    for (const reason of model.quarantineReasons || []) {
      tokens.add(reason);
    }
    for (const phase of model.blegOperation?.phases || []) {
      tokens.add(phase);
    }
    for (const state of model.eventOutbox?.states || []) {
      tokens.add(state);
    }
    for (const aggregate of Object.keys(model.stateMachines || {})) {
      tokens.add(aggregate);
    }
    for (const reason of Object.keys(model.stateMachines?.terminalMapping || {})) {
      tokens.add(reason);
    }
    tokens.add('eventRegistry');
    tokens.add('frozenAt');
    tokens.add(sha256(canonicalJson(model.eventTypes)));
  }
  if (args.stage === 's3') {
    tokens.add('recordingLifecycle');
    tokens.add('mediaEvidenceBoundary');
    for (const state of model.recordingLifecycle?.states || []) {
      tokens.add(state);
    }
    for (const key of ['eventDomain', 'aggregateType', 'eventType', 'dedupeKey']) {
      const value = model.recordingLifecycle?.conflictOutbox?.[key];
      if (typeof value === 'string' && !value.includes('<')) {
        tokens.add(value);
      }
    }
    tokens.add('recording-conflict:');
    for (const field of model.mediaEvidenceBoundary?.handoffFields || []) {
      tokens.add(field);
    }
    for (const value of model.mediaEvidenceBoundary?.evidenceStatusValues || []) {
      tokens.add(value);
    }
    for (const value of model.recordingLifecycle?.outbox?.declaration?.permanentCodes || []) {
      tokens.add(value);
    }
    for (const value of model.recordingLifecycle?.outbox?.completion?.permanentCodes || []) {
      tokens.add(value);
    }
    for (const value of model.recordingLifecycle?.outbox?.completion?.retryableCodes || []) {
      tokens.add(value);
    }
  }
  for (const token of [...tokens].sort()) {
    if (!doc.includes(token)) {
      counters.token_missing += 1;
      issues.push(`document does not mention model token ${token}`);
    }
  }

  let fixtureFiles = [];
  try {
    fixtureFiles = fs.readdirSync(args.fixtures).sort();
  } catch (error) {
    counters.failed += 1;
    issues.push(`fixtures directory is unreadable: ${error.message}`);
  }
  const jsonFiles = fixtureFiles.filter((name) => name.endsWith('.json'));
  const fixtureEntries = [];
  for (const fileName of jsonFiles) {
    try {
      const fixture = JSON.parse(readUtf8(path.join(args.fixtures, fileName)));
      fixtureEntries.push({ fileName, fixture });
    } catch (error) {
      counters.failed += 1;
      issues.push(`fixture ${fileName}: ${error.message}`);
    }
  }
  const actualIds = fixtureEntries.map((entry) => entry.fixture?.id).filter((id) => typeof id === 'string');
  for (const expectedId of config.fixtureIds) {
    if (!actualIds.includes(expectedId)) {
      counters.fixture_id_missing += 1;
      issues.push(`missing fixture ID ${expectedId}`);
    }
  }
  for (const actualId of actualIds) {
    if (!config.fixtureIds.includes(actualId)) {
      counters.fixture_id_extra += 1;
      issues.push(`extra fixture ID ${actualId}`);
    }
  }
  if (new Set(actualIds).size !== actualIds.length) {
    counters.fixture_id_extra += 1;
    issues.push('duplicate fixture ID detected');
  }
  const extraFiles = fixtureFiles.filter((name) => !name.endsWith('.json'));
  for (const fileName of extraFiles) {
    counters.fixture_id_extra += 1;
    issues.push(`non-JSON file in fixtures directory: ${fileName}`);
  }
  for (const entry of fixtureEntries) {
    validateFixture(entry.fixture, expectedSliceForFixture(entry.fixture?.id, config), counters, issues);
  }
  const negativeCount = fixtureEntries.filter((entry) => NEGATIVE_FIXTURE_IDS.has(entry.fixture?.id)).length;

  const slices = config.sliceOrder.map((sliceName) => {
    const prefix = sliceName + '-';
    return actualIds.filter((id) => id.startsWith(prefix)).length;
  });
  const fixtureCount = jsonFiles.length;
  const messages = Array.isArray(model.messages) ? model.messages.length : 0;
  const eventCount = eventTypes.length;
  const identities = Array.isArray(model.identities) ? model.identities.length : 0;
  const invariantCount = Array.isArray(model.invariants) ? model.invariants.length : 0;

  process.stdout.write(`stage=${args.stage} messages=${messages} event_types=${eventCount} event_map_form=${config.eventMapForm} identities=${identities} fixtures=${fixtureCount} passed=${counters.passed} failed=${counters.failed} negative=${negativeCount} slice_fixtures=${slices.join('+')}\n`);
  process.stdout.write(`a1_operations=${a1OperationCount} ivr_operations=${ivrOperations.length} a1_drift=${counters.a1_drift}\n`);
  process.stdout.write(`a0_tables=${a0Tables} a0_status_drift=${counters.a0_status_drift} a0_registry_drift=${counters.a0_registry_drift} a0_column_drift=${counters.a0_column_drift}\n`);
  process.stdout.write(`doc_missing=${counters.doc_missing} doc_extra=${counters.doc_extra} doc_duplicate=${counters.doc_duplicate} doc_invalid=${counters.doc_invalid} token_missing=${counters.token_missing}\n`);
  process.stdout.write(`stage_field_missing=${counters.stage_field_missing} stage_field_premature=${counters.stage_field_premature} fixture_id_missing=${counters.fixture_id_missing} fixture_id_extra=${counters.fixture_id_extra}\n`);
  process.stdout.write(`invariants=${invariantCount}\n`);
  if (args.stage !== 's1') {
    process.stdout.write(`event_registry_frozen=s2 event_registry_sha256=${sha256(canonicalJson(eventTypes))}\n`);
  }
  const failed = counters.passed !== fixtureCount
    || counters.failed !== 0
    || counters.doc_missing !== 0
    || counters.doc_extra !== 0
    || counters.doc_duplicate !== 0
    || counters.doc_invalid !== 0
    || counters.token_missing !== 0
    || counters.stage_field_missing !== 0
    || counters.stage_field_premature !== 0
    || counters.fixture_id_missing !== 0
    || counters.fixture_id_extra !== 0
    || counters.a1_drift !== 0
    || counters.a0_status_drift !== 0
    || counters.a0_registry_drift !== 0
    || counters.a0_column_drift !== 0
    || issues.length !== 0;
  if (failed) {
    for (const issue of issues) {
      process.stderr.write(`- ${issue}\n`);
    }
    process.stdout.write(`FAIL protocol-validation (${args.stage})\n`);
    process.exit(1);
  }
  process.stdout.write(`PASS protocol-validation (${args.stage})\n`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main();
}

export { STAGE_CONFIG, expectedSliceForFixture, validateFixture };
