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

class ProtocolMachine {
  constructor() {
    this.calls = new Map();
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
  }

  countSnapshot() {
    return { ...this.counts };
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
      sessionStatus: 'RECEIVED',
      task: null,
      reservation: null,
      decisions: new Map(),
      decisionOrder: [],
      trialGrants: 0,
      activationConfirmations: 0
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
      call.reservation = { reservationId: server.reservationId, status: 'HELD' };
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
      call.reservation = { reservationId: server.reservationId, status: 'HELD' };
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
    call.reservation = { reservationId: server.reservationId, status: 'HELD' };
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

  handle(operation, callId, body, server) {
    let result;
    if (operation === 'resolveIvrInbound') {
      const allowed = ['callId', 'purpose', 'protocolVersion', 'promptVersion'];
      if (Object.keys(body).some((key) => !allowed.includes(key))) {
        return { status: 422, problem: { code: 'VALIDATION_FAILED' } };
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
    } else {
      throw new Error(`operation ${operation} is not an S1 decision operation`);
    }
    this.assertInvariants();
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
      expectedRows: (model.eventTypes || []).map((row) => config.eventDocColumns.map((column) => String(row[column]))),
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
