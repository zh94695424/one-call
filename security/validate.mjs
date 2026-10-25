#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const HTTP_METHODS = ['get', 'post', 'put', 'patch', 'delete'];
const EXPECTED_EVENT_DIGEST = 'c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9';
const EXPECTED_A0_ENVELOPE_REGEX = '^v1\\.[A-Za-z0-9._:/-]+\\..+$';
const EXPECTED_HMAC_REGEX = '^[0-9a-f]{64}$';

const EXPECTED_TOP_LEVEL = [
  'modelVersion',
  'specVersion',
  'upstream',
  'envelopeProfile',
  'hmacProfile',
  'keyStates',
  'rotationPolicy',
  'recoveryPolicy',
  'replayCompatibility',
  'keyClasses',
  'dataClasses',
  'deferredInputs',
  'invariants'
];

const FORBIDDEN_STAGE_SECTIONS = [
  'trustSurfaces',
  'authDomains',
  'trustBoundaries',
  'serviceIdentities',
  'credentialClasses',
  'signatureProfiles',
  'noncePolicy',
  'audienceRegistry',
  'mediaPolicies',
  'cosPolicy',
  'auditEvents',
  'highRiskActions',
  'auditCoverage',
  'redactionPolicy',
  'failClosedMatrix'
];

const EXPECTED_KEY_ROWS = [
  ['K01', 'KMS_CMK_DATA', 'Tencent KMS (region CV-1)', 'root KEK for DEK', 'ROOT_KEK', 'KMS symmetric CMK', 'K02-K09', 'CMK version rotation CV-1', 're-enable/restore CV-1', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'Security/Infrastructure'],
  ['K02', 'DEK_PROVIDER_SUBJECT', 'KMS-wrapped Secret Manager version + memory', 'provider subject AEAD', 'PROVIDER_SUBJECT', 'AES-256-GCM/12/16/AAD', 'D01', 'new version; old decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'customer_iam'],
  ['K03', 'DEK_IDENTITY_PROFILE', 'KMS-wrapped Secret Manager version + memory', 'legal name/id number AEAD', 'IDENTITY_PROFILE', 'AES-256-GCM/12/16/AAD', 'D02', 'new version; old decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'customer_iam'],
  ['K04', 'DEK_VERIFICATION_REF', 'KMS-wrapped Secret Manager version + memory', 'verification ref AEAD', 'VERIFICATION_REF', 'AES-256-GCM/12/16/AAD', 'D02/D05', 'new version; old decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'customer_iam/subject_number'],
  ['K05', 'DEK_PHONE_NUMBER', 'KMS-wrapped Secret Manager version + memory', 'caller/callee/number AEAD', 'PHONE_NUMBER', 'AES-256-GCM/12/16/AAD', 'D04/D06/D07/D09', 'new version; old decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'subject_number/ivr'],
  ['K06', 'DEK_IVR_DECISION', 'KMS-wrapped Secret Manager version + memory', 'frozen decision payload AEAD', 'IVR_DECISION', 'AES-256-GCM/12/16/AAD', 'D08', 'new writes; append-only old versions decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'ivr'],
  ['K07', 'DEK_CALL_EVENT', 'KMS-wrapped Secret Manager version + memory', 'call event payload AEAD', 'CALL_EVENT', 'AES-256-GCM/12/16/AAD', 'D10', 'new version; old decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'ivr'],
  ['K08', 'DEK_PROVIDER_PAYLOAD', 'KMS-wrapped Secret Manager version + memory', 'provider payload AEAD', 'PROVIDER_PAYLOAD', 'AES-256-GCM/12/16/AAD', 'D11', 'new version; old decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'catalog_order'],
  ['K09', 'DEK_INTERNAL_PAYLOAD', 'KMS-wrapped Secret Manager version + memory', 'audit/outbox/idempotency encrypted snapshot', 'INTERNAL_PAYLOAD', 'AES-256-GCM/12/16/AAD', 'D27/D28', 'new version; old decrypt-only', 'SM version + KMS', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'platform/operations-audit'],
  ['K10', 'COS_CMK_MEDIA', 'Tencent KMS + COS SSE-KMS', 'media object at rest', 'MEDIA_OBJECT', 'COS SSE-KMS CV-4', 'D19', 'COS/KMS rotation CV-4', 'COS/KMS CV-4', 'ACTIVE,DECRYPT_ONLY,DISABLED,DESTROYED', 'evidence'],
  ['K11', 'HMAC_PROVIDER_SUBJECT', 'Secret Manager', 'provider subject lookup', 'PROVIDER_SUBJECT_LOOKUP', 'HMAC-SHA-256/256-bit', 'D01', 'dual-read; old lookup-only', 'SM version', 'ACTIVE,LOOKUP_ONLY,DISABLED,DESTROYED', 'customer_iam'],
  ['K12', 'HMAC_ID_NUMBER', 'Secret Manager', 'id number lookup', 'ID_NUMBER_LOOKUP', 'HMAC-SHA-256/256-bit', 'D02', 'dual-read; old lookup-only', 'SM version', 'ACTIVE,LOOKUP_ONLY,DISABLED,DESTROYED', 'customer_iam'],
  ['K13', 'HMAC_PHONE_NUMBER', 'Secret Manager', 'caller/binding/access number lookup', 'PHONE_NUMBER_LOOKUP', 'HMAC-SHA-256/256-bit', 'D04/D06/D07/D09;accessNumberHmac', 'dual-read; old lookup-only', 'SM version', 'ACTIVE,LOOKUP_ONLY,DISABLED,DESTROYED', 'subject_number/ivr'],
  ['K14', 'HMAC_EMPLOYEE_LOGIN', 'Secret Manager', 'employee login lookup', 'EMPLOYEE_LOGIN_LOOKUP', 'HMAC-SHA-256/256-bit', 'D03', 'dual-read; old lookup-only', 'SM version', 'ACTIVE,LOOKUP_ONLY,DISABLED,DESTROYED', 'operations_iam'],
  ['K15', 'HMAC_EMPLOYEE_AUTH_SUBJECT', 'Secret Manager', 'employee auth subject lookup', 'EMPLOYEE_AUTH_SUBJECT_LOOKUP', 'HMAC-SHA-256/256-bit', 'D03', 'dual-read; old lookup-only', 'SM version', 'ACTIVE,LOOKUP_ONLY,DISABLED,DESTROYED', 'operations_iam'],
  ['K16', 'HMAC_SHARE_DESIGNATED_MOBILE', 'Secret Manager', 'designated mobile proof', 'SHARE_DESIGNATED_MOBILE', 'HMAC-SHA-256/256-bit', 'D24', 'new grant uses new version; old snapshot retains its version', 'SM version', 'ACTIVE,LOOKUP_ONLY,DISABLED,DESTROYED', 'sharing'],
  ['K17', 'IVR_REQUEST_SIGNING', 'Secret Manager', 'IVR request HMAC', 'IVR_REQUEST_SIGNATURE', 'HMAC-SHA-256/256-bit', 'C04', 'active + verify-only overlap', 'SM version', 'ACTIVE,VERIFY_ONLY,DISABLED,DESTROYED', 'IVR/Backend'],
  ['K18', 'PROVIDER_CALLBACK_VERIFICATION', 'Secret Manager/cert store', 'provider signature verify', 'PROVIDER_CALLBACK_VERIFICATION', 'vendor profile Q6', 'C06', 'provider rotation Q6', 'provider/SM', 'ACTIVE,VERIFY_ONLY,DISABLED,DESTROYED', 'Payments/Backend'],
  ['K19', 'MEDIA_UPLOAD_SIGNING', 'Secret Manager/COS signing material', 'upload credential issuance', 'MEDIA_UPLOAD', 'COS profile CV-4', 'C09', 'overlap until uploadExpiresAt', 'SM/COS', 'ACTIVE,VERIFY_ONLY,DISABLED,DESTROYED', 'Security/Backend/Evidence'],
  ['K20', 'MEDIA_PLAYBACK_SIGNING', 'Secret Manager', 'playback token signing', 'MEDIA_PLAYBACK', 'HMAC-SHA-256/256-bit', 'C10', 'overlap until max TTL + Q22', 'SM version', 'ACTIVE,VERIFY_ONLY,DISABLED,DESTROYED', 'Security/Sharing']
];

const EXPECTED_DATA_ROWS = [
  ['D01', 'AEAD+HMAC', 'K02/K11', 'customer_iam', 'never returned; HMAC is not a public key', 'service access redacted; employee read SECURITY_SENSITIVE_READ', 'old key_ref decryptable; HMAC dual-read/rebuild'],
  ['D02', 'AEAD+HMAC', 'K03/K12/K04', 'customer_iam', 'never returned plaintext; status or masked value only', 'same as D01', 'mutable row may re-encrypt; HMAC rebuild'],
  ['D03', 'HMAC+mask', 'K14/K15', 'operations_iam', 'masked login only', 'employee auth access; HMAC never returned', 'dual-read/rebuild'],
  ['D04', 'AEAD+HMAC+mask', 'K05/K13', 'subject_number', 'masked number only', 'service access redacted', 'mutable re-encrypt/rebuild'],
  ['D05', 'AEAD', 'K04', 'subject_number', 'never returned', 'claim/audit redacted', 'append-only; old key retained'],
  ['D06', 'AEAD+HMAC+mask', 'K05/K13', 'ivr', 'masked callee; plaintext only in IVR internal decision payload', 'service access redacted', 'mutable task re-encrypt'],
  ['D07', 'AEAD+HMAC+mask', 'K05/K13', 'ivr', 'masked only', 'service access redacted', 'mutable session re-encrypt'],
  ['D08', 'AEAD+redacted JSON', 'K06', 'ivr', 'redacted snapshot only; replay decrypts the same payload', 'employee/support read audited', 'append-only; old key retained'],
  ['D09', 'AEAD+HMAC+mask', 'K05/K13', 'ivr', 'masked only', 'service access redacted', 'immutable party guard; never rewrite'],
  ['D10', 'AEAD+redacted JSON', 'K07', 'ivr', 'receipt/redacted only', 'service access redacted', 'append-only; never rewrite'],
  ['D11', 'AEAD', 'K08', 'catalog_order', 'never returned or logged', 'restricted read + audit', 'append-only'],
  ['D12', 'internal ID', '-', 'catalog_order', 'not a public key; restricted audit', 'restricted access', 'provider-scoped; never public'],
  ['D13', 'redacted JSON+hash', '-', 'catalog_order', 'commercial fact; no protected value', 'publication audit', 'immutable on publish'],
  ['D14', 'redacted JSON+hash', '-', 'catalog_order', 'frozen commercial fact', 'order access', 'immutable'],
  ['D15', 'redacted JSON+hash', '-', 'entitlement_metering', 'policy facts only', 'publication audit', 'immutable version'],
  ['D16', 'redacted JSON+hash', '-', 'entitlement_metering', 'historical grant facts', 'access/audit', 'immutable'],
  ['D17', 'redacted JSON+hash', '-', 'entitlement_metering', 'usage metadata', 'settlement audit', 'append-only'],
  ['D18', 'redacted JSON+hash', '-', 'activation_consent', 'no protected value', 'consent audit', 'append-only'],
  ['D19', 'COS internal ref + hash', 'K10/CV-4', 'evidence', 'never returns key/bucket/version/URL', 'media credential/object audit', 'COS versioning; old CMK version decryptable'],
  ['D20', 'hash only', '-', 'evidence', 'hash prefix only if approved', 'recording audit', 'immutable validation facts'],
  ['D21', 'reference + redacted JSON+hash', '-', 'evidence', 'no raw PII/object key/URL', 'evidence audit', 'immutable capture'],
  ['D22', 'redacted JSON+hash', '-', 'evidence', 'no media bytes/raw parties', 'append-only audit', 'immutable'],
  ['D23', 'token digest + KDF salt', 'C07/C08', 'sharing', 'plaintext return-once only; never re-displayed', 'share create/rotate/revoke audit', 'capability token not rebuildable; code generation rotation'],
  ['D24', 'redacted JSON+hash+HMAC', 'K16', 'sharing', 'no object key/URL/raw phone/unshared evidence', 'share transition audit', 'immutable snapshot; old HMAC version until grant/retention'],
  ['D25', 'opaque derived key', '-', 'sharing', 'never returned', 'admission access event', 'deterministic by shareGrantId+admissionRequestId'],
  ['D26', 'hash + redacted JSON', '-', 'sharing', 'no raw IP/phone/code/token/full URL', 'SHARE_ACCESS event', 'append-only'],
  ['D27', 'redacted JSON or AEAD', 'K09 when encrypted', 'operations-audit', 'closed redacted projection only', 'audit read access event', 'append-only; never rewrite'],
  ['D28', 'redacted JSON or AEAD', 'K09 when encrypted', 'platform', 'consumers only authorized fields', 'dispatch access redacted', 'immutable payload']
];

const EXPECTED_DEFERRED_Q = [
  ['Q1', 'IDENTITY_PROVIDER/POLICY', 'Freeze independent employee domain/audience/fixed roles only; no MFA/IdP implementation'],
  ['Q3', 'LEGAL_RETENTION', 'Freeze rotation/recovery must satisfy retention first; no retention period selection'],
  ['Q6', 'VENDOR_POLICY', 'Freeze adapter verification order/idempotency/fail-closed only; no vendor field/algorithm'],
  ['Q7', 'VENDOR_POLICY/NUMERIC', 'Freeze mTLS/signature interface and fail-closed only; no line/format/certificate values'],
  ['Q8', 'NUMERIC', 'Do not define tolerance/SLO/timeout'],
  ['Q10', 'POLICY', 'Pass prompt/policy reference only'],
  ['Q11', 'CLOUD_CAPABILITY/LEGAL', 'Fix private bucket/SSE-KMS/versioning requirements; no WORM/region claim'],
  ['Q12', 'NUMERIC/POLICY', 'Freeze salt/generation/KDF interface only; no parameters'],
  ['Q13', 'NUMERIC/POLICY', 'Freeze audit/access classification only; no retention selection'],
  ['Q14', 'VENDOR_POLICY', 'Project status only; no document field/provider'],
  ['Q17', 'PLATFORM', 'Freeze logical service/DB privilege boundary only'],
  ['Q18', 'CONFIG/NUMERIC', 'Use the frozen 30s default; no configuration key definition'],
  ['Q19', 'IDENTITY_PROVIDER', 'Freeze customer domain/audience only; no login/merge definition'],
  ['Q22', 'NUMERIC', 'Freeze credential fail-closed/TTL upper-bound semantics; no values'],
  ['Q23', 'PLATFORM', 'Freeze role/ownership contract only; no deployment identity selection'],
  ['Q25', 'NUMERIC', 'Freeze timestamp/nonce verification order; no window selection'],
  ['Q27', 'ROLE_MATRIX', 'Keep A0/A1 lifecycle/access checks; no role matrix expansion'],
  ['Q28', 'PLATFORM', 'Read existing local A2 recovery only; no new protocol']
];

const EXPECTED_CV_ROWS = [
  ['CV-1', 'KMS region/edition symmetric CMK, envelope/wrap, rotation/disable/recover/pending-delete', 'CLOUD_CAPABILITY', 'Logical K01-K09 profile frozen; capability to verify', 'Infrastructure/Security', 'Q2 closure + COS/KMS validation'],
  ['CV-2', 'Secret Manager region/edition versioning/rotation/dual-version', 'CLOUD_CAPABILITY', 'Logical custody frozen; capability to verify', 'Infrastructure/Security', 'Q2 closure + deployment security'],
  ['CV-3', 'CAM role/policy least privilege and workspace isolation', 'CLOUD_CAPABILITY', 'Six service identities frozen; provisioning to verify', 'Infrastructure/Security', 'Q17/Q23 deployment'],
  ['CV-4', 'COS private bucket/SSE-KMS/versioning/presign (method/object/expiry/size)/private endpoint/object lock', 'CLOUD_CAPABILITY', 'Private bucket requirements fixed; upload primary shape is COS presigned PUT gated on this verification; otherwise fail closed and stop; region/SKU/WORM to verify', 'Infrastructure/Security', 'Q11 storage/capacity'],
  ['CV-5', 'CloudAudit/log service operation evidence and retention', 'CLOUD_CAPABILITY', 'PostgreSQL audit is the source of truth; provider logs supplementary', 'Infrastructure/Security/Observability', 'Q13 observability']
];

const EXPECTED_INVARIANTS = [
  ['INV-A3-01', 'Every protected datum has exactly one dataClass, owner, representation, key/HMAC class and audit/access coverage; no unregistered sensitive field exists.'],
  ['INV-A3-02', 'Encryption and HMAC keys are strictly separated; no key crosses purpose; cross-purpose decrypt or lookup must fail.'],
  ['INV-A3-03', 'Envelope shape, key_ref parsing and AEAD/AAD are fixed; unknown/malformed/retired key_ref, wrong purpose or decryption failure must fail closed with no plaintext fallback.'],
  ['INV-A3-04', 'HMAC is lowercase 64-hex under an independent key; it is used only for equality, uniqueness and indexed lookup and is never returned, logged or public.'],
  ['INV-A3-05', 'Every key and credential has an owner, custody, state, rotation and recovery path; secret material never enters a database, code, image or log.'],
  ['INV-A3-06', 'Rotation never rewrites append-only or frozen rows; historical keys are retained until retention; A2 replay returns the same payload/decision without re-selection or a second reservation.']
];

const EXPECTED_ENVELOPE = {
  version: 'v1',
  textShape: 'v1.<key_ref>.<base64url(nonce || authenticated_ciphertext_and_tag)>',
  regex: EXPECTED_A0_ENVELOPE_REGEX,
  keyRefPattern: '^[A-Za-z0-9._:/-]+$',
  keyRefParse: 'first segment is the literal v1; last dot separates the base64url body; the middle key_ref is opaque and may contain dots',
  bodyPattern: '^[A-Za-z0-9_-]+$',
  bodyEncoding: 'base64url without padding',
  aead: 'AES-256-GCM',
  nonceBytes: 12,
  tagBytes: 16,
  minimumPlaintextBytes: 1,
  aadTemplate: 'onecall:v1:<purposeId>:<dataClassId>',
  aadEncoding: 'UTF-8 ASCII',
  productionNonceSource: 'CSPRNG per encryption; never reused',
  fixtureNoncePolicy: 'fixed TEST_ONLY nonces are deterministic evaluator inputs only',
  unknownKeyRef: 'fail closed; no plaintext or scan fallback',
  authIdentityVersionRule: 'customer_iam.auth_identity.key_version equals the integer version encoded by that row key_ref'
};

const EXPECTED_HMAC = {
  algorithm: 'HMAC-SHA-256',
  keyBits: 256,
  representation: 'lowercase hexadecimal',
  hexPattern: EXPECTED_HMAC_REGEX,
  input: 'UTF-8 bytes of the canonical normalized value',
  usage: ['equality', 'uniqueness', 'indexed lookup'],
  forbiddenUsage: ['encryption', 'signing', 'session', 'public identifier', 'logging', 'audit output'],
  saltOrTruncation: 'none',
  accessNumberHmac: 'same approved phone-number key and contract; ephemeral fingerprint input only'
};

const EXPECTED_KEY_STATES = [
  ['ACTIVE', ['encrypt', 'decrypt', 'lookup', 'sign', 'verify']],
  ['DECRYPT_ONLY', ['decrypt']],
  ['LOOKUP_ONLY', ['lookup']],
  ['VERIFY_ONLY', ['verify']],
  ['DISABLED', []],
  ['DESTROYED', []]
];

const EXPECTED_ROTATION = {
  sequence: ['ACTIVE', 'DECRYPT_ONLY|LOOKUP_ONLY|VERIFY_ONLY', 'DISABLED', 'DESTROYED'],
  newWritesUse: 'ACTIVE',
  oldVersion: 'DECRYPT_ONLY for encryption, LOOKUP_ONLY for HMAC and VERIFY_ONLY for signing',
  mutableRows: 'lazy idempotent re-encrypt or reindex only where A0 permits UPDATE',
  appendOnlyFrozenRows: 'never rewrite; retain the old key version until retention ends',
  hmacDualRead: 'active plus historical keys referenced by retained rows; equality queries only',
  signatureOverlap: 'active plus verify-only overlap; unknown keyId is rejected without default fallback',
  retentionGate: 'retire requires the approved retention input; otherwise the old version stays decrypt-only or lookup-only',
  auditEvent: 'SECURITY_KEY_ROTATE',
  reencryptSelection: 'envelope key_ref selects the historical version; no global key swap',
  failClosed: true
};

const EXPECTED_RECOVERY = {
  disabled: 'authorized KMS or Secret Manager re-enable only; no application bypass',
  pendingDeletion: 'cancel or restore only when CV-1 or CV-2 proves the capability; otherwise unrecoverable',
  destroyed: 'unrecoverable; every read fails closed and the incident process owns the outcome',
  secretVersionUnavailable: 'fail closed; no plaintext environment, file or image fallback',
  backupRebuild: 'no plaintext rebuild of a destroyed key or protected value',
  auditEvent: 'SECURITY_KEY_RECOVER',
  failClosed: true
};

const EXPECTED_REPLAY = {
  transportNonce: 'regenerated on every retry and never part of domain identity',
  domainIdentity: 'callId, eventId, recordingId and decisionId are unchanged across retries',
  historicalDecrypt: 'the envelope key_ref selects the historical key version',
  hmacLookup: 'active plus retained historical keys; equality only, never a plaintext scan',
  a2DecisionReplay: 'the same envelope decrypts the same immutable payload and redacted snapshot',
  a2SideEffects: 'no second call, reservation, settlement or event identity',
  appendOnly: 'frozen rows are never rewritten for rotation',
  rotationWireImpact: 'none on A1 wire, A2 semantic identity, response_sha256 or request_fingerprint inputs'
};

const EXPECTED_FIXTURES = [
  ['sec-s1-01-envelope-purpose-roundtrip', false],
  ['sec-s1-02-hmac-index-equality', false],
  ['sec-s1-03-cross-purpose-denied', true],
  ['sec-s1-04-rotation-historical-replay', false],
  ['sec-s1-05-key-unavailable-failclosed', true],
  ['sec-s1-06-append-only-rotation-safety', false]
];

const MARKER_CONFIG = {
  key: {
    name: 'security-key-map',
    header: ['keyClass', 'custodyStorage', 'purpose', 'algorithm', 'dataClasses', 'rotation', 'recovery']
  },
  data: {
    name: 'security-data-map',
    header: ['dataClass', 'a0Inventory', 'representation', 'keyClass', 'owner', 'projection', 'auditAccess', 'rotationReplay']
  },
  deferred: {
    name: 'security-deferred-map',
    header: ['inputId', 'input', 'valueKind', 'a3Treatment', 'owner', 'destination']
  }
};

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') {
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) {
    return '[' + value.map((item) => canonicalJson(item)).join(',') + ']';
  }
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function sha256(value) {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

function deepEqual(left, right) {
  return canonicalJson(left) === canonicalJson(right);
}

function readUtf8(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

function parseArgs(argv) {
  const allowed = new Set(['--stage', '--model', '--fixtures', '--spec', '--a1-bundle', '--a0', '--a2-model', '--a2-fixtures']);
  const result = {};
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
  for (const key of ['stage', 'model', 'fixtures', 'spec', 'a1Bundle', 'a0', 'a2Model', 'a2Fixtures']) {
    if (!result[key]) {
      throw new Error(`missing --${key.replace(/[A-Z]/g, (letter) => '-' + letter.toLowerCase())}`);
    }
  }
  if (result.stage !== 's1') {
    throw new Error(`stage ${result.stage} is not implemented by the S1 validator`);
  }
  return result;
}

function parseCells(line) {
  const trimmed = line.trim();
  if (!trimmed.startsWith('|') || !trimmed.endsWith('|')) {
    return null;
  }
  return trimmed.slice(1, -1).split('|').map((cell) => cell.trim());
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
    issues.push(`${name}: expected one start and one end marker, found ${startCount}/${endCount}`);
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
  return { header, rows };
}

function extractA0SensitiveRows(doc) {
  const match = doc.match(/<!-- sensitive:start -->([\s\S]*?)<!-- sensitive:end -->/);
  if (!match) {
    return [];
  }
  const rows = [];
  for (const line of match[1].split('\n')) {
    const cells = parseCells(line);
    if (!cells || cells.length !== 3 || isSeparatorRow(cells)) {
      continue;
    }
    if (cells[0] === 'Column(s)') {
      continue;
    }
    rows.push(cells[0]);
  }
  return rows;
}

function extractA0OpenInputs(doc) {
  const section = doc.match(/## 14\. Open Production Inputs([\s\S]*?)(?:\n## 15\.|\n## [1-9][0-9]*\.|$)/);
  const body = section ? section[1] : '';
  const rows = new Map();
  for (const line of body.split('\n')) {
    const cells = parseCells(line);
    if (!cells || cells.length !== 5 || isSeparatorRow(cells)) {
      continue;
    }
    if (!/^Q[0-9]+$/.test(cells[0])) {
      continue;
    }
    rows.set(cells[0], { input: cells[1], why: cells[2], owner: cells[3], destination: cells[4] });
  }
  return rows;
}

function countA0Catalog(doc) {
  const match = doc.match(/<!-- catalog:start -->([\s\S]*?)<!-- catalog:end -->/);
  if (!match) {
    return 0;
  }
  let count = 0;
  for (const line of match[1].split('\n')) {
    if (/^\| `[a-z_]+\.[a-z_]+` \|/.test(line)) {
      count += 1;
    }
  }
  return count;
}

function walkKeys(value, callback, trail = '') {
  if (value === null || typeof value !== 'object') {
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item, index) => walkKeys(item, callback, `${trail}[${index}]`));
    return;
  }
  for (const [key, child] of Object.entries(value)) {
    callback(key, trail ? `${trail}.${key}` : key);
    walkKeys(child, callback, trail ? `${trail}.${key}` : key);
  }
}

function collectStrings(value, output) {
  if (typeof value === 'string') {
    output.push(value);
    return output;
  }
  if (value === null || typeof value !== 'object') {
    return output;
  }
  if (Array.isArray(value)) {
    value.forEach((item) => collectStrings(item, output));
    return output;
  }
  Object.values(value).forEach((child) => collectStrings(child, output));
  return output;
}

function keyTokens(row) {
  return String(row.keyClass || '')
    .split(/[\/;,\s]+/)
    .map((token) => token.trim())
    .filter((token) => /^K[0-9]{2}$/.test(token));
}

function keyCoversDataClass(keyRow, dataClass) {
  return String(keyRow.dataClasses || '')
    .split(/[\/;,]/)
    .map((token) => token.trim())
    .includes(dataClass);
}

function isAeadKeyClass(keyRow) {
  return Boolean(keyRow) && typeof keyRow.keyClass === 'string' && keyRow.keyClass.startsWith('DEK_');
}

function derivePurpose(keyById, dataRow) {
  for (const token of keyTokens(dataRow)) {
    const keyRow = keyById.get(token);
    if (keyRow && (keyRow.keyClass.startsWith('DEK_') || keyRow.keyClass === 'COS_CMK_MEDIA')) {
      return keyRow.purposeId;
    }
  }
  return null;
}

function stateAllows(state, operation) {
  const map = {
    ACTIVE: ['encrypt', 'decrypt', 'lookup', 'sign', 'verify'],
    DECRYPT_ONLY: ['decrypt'],
    LOOKUP_ONLY: ['lookup'],
    VERIFY_ONLY: ['verify'],
    DISABLED: [],
    DESTROYED: []
  };
  return (map[state] || []).includes(operation);
}

function denied(reason, securityEvent, extra = {}) {
  return {
    status: 'DENIED',
    reason,
    problemCode: 'SERVICE_UNAVAILABLE',
    securityEvent,
    auditEvent: securityEvent,
    plaintextExposed: false,
    rowsWritten: 0,
    ...extra
  };
}

function createMachine(model) {
  const keyById = new Map(model.keyClasses.map((row) => [row.keyId, row]));
  const keyClassById = keyById;
  const dataById = new Map(model.dataClasses.map((row) => [row.dataClass, row]));
  const state = {
    materials: new Map(),
    envelopesByStep: new Map(),
    envelopesByDataClass: new Map(),
    hmacs: new Map(),
    plaintextHashes: new Map(),
    auditEvents: []
  };

  function materialFor(keyRef) {
    return state.materials.get(keyRef) || null;
  }

  function parseEnvelope(envelope) {
    if (typeof envelope !== 'string') {
      return null;
    }
    const match = envelope.match(/^v1\.([A-Za-z0-9._:\/-]+)\.([A-Za-z0-9_-]+)$/);
    if (!match) {
      return null;
    }
    const body = Buffer.from(match[2], 'base64url');
    if (body.length < 29) {
      return null;
    }
    return { keyRef: match[1], body };
  }

  function envelopeRecord(step) {
    if (typeof step.envelope === 'string') {
      return { envelope: step.envelope };
    }
    if (step.envelopeFromStep) {
      return state.envelopesByStep.get(step.envelopeFromStep) || null;
    }
    const dataClass = step.envelopeFromDataClass || step.dataClass;
    return state.envelopesByDataClass.get(dataClass) || null;
  }

  function materialSecret(material) {
    return crypto.createHash('sha256').update(material.keyLabel, 'utf8').digest();
  }

  function run(step) {
    if (!step || typeof step !== 'object' || typeof step.action !== 'string') {
      return { status: 'FAILED', reason: 'INVALID_STEP' };
    }
    switch (step.action) {
      case 'encrypt': {
        const material = materialFor(step.keyRef);
        if (!material) {
          return denied('KEY_UNKNOWN', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const keyRow = keyById.get(material.keyId);
        const dataRow = dataById.get(step.dataClass);
        if (!keyRow || !dataRow) {
          return denied('UNREGISTERED_CLASS', 'SECURITY_KEY_PURPOSE_MISMATCH');
        }
        if (step.keyId && step.keyId !== material.keyId) {
          return denied('KEY_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH');
        }
        const expectedPurpose = derivePurpose(keyById, dataRow);
        if (!expectedPurpose || step.purpose !== expectedPurpose || !keyCoversDataClass(keyRow, step.dataClass) || !isAeadKeyClass(keyRow)) {
          return denied('PURPOSE_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH');
        }
        if (!stateAllows(material.state, 'encrypt')) {
          return denied('KEY_STATE_DENIED', 'SECURITY_KEY_ACCESS_DENIED', { keyState: material.state, keyRef: material.keyRef });
        }
        const nonce = Buffer.from(step.nonceHex || '', 'hex');
        if (nonce.length !== 12) {
          return denied('NONCE_INVALID', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const aad = `onecall:v1:${expectedPurpose}:${step.dataClass}`;
        const cipher = crypto.createCipheriv('aes-256-gcm', materialSecret(material), nonce);
        cipher.setAAD(Buffer.from(aad, 'utf8'));
        const ciphertext = Buffer.concat([cipher.update(step.plaintext, 'utf8'), cipher.final()]);
        const tag = cipher.getAuthTag();
        const body = Buffer.concat([nonce, ciphertext, tag]).toString('base64url');
        const envelope = `v1.${material.keyRef}.${body}`;
        const record = { envelope, keyRef: material.keyRef, keyId: material.keyId, dataClass: step.dataClass, purpose: expectedPurpose, plaintextHash: sha256(step.plaintext) };
        state.envelopesByStep.set(step.id, record);
        state.envelopesByDataClass.set(step.dataClass, record);
        state.plaintextHashes.set(step.id, sha256(step.plaintext));
        return {
          status: 'OK',
          aad,
          envelopeVersion: 'v1',
          nonceBytes: 12,
          tagBytes: 16,
          envelopePattern: new RegExp(EXPECTED_A0_ENVELOPE_REGEX).test(envelope),
          plaintextAbsentFromEnvelope: !envelope.includes(step.plaintext),
          keyState: material.state,
          keyRef: material.keyRef,
          keyId: material.keyId,
          rowsWritten: 1
        };
      }
      case 'decrypt': {
        if (step.dependencyAvailable === false) {
          return denied('DEPENDENCY_UNAVAILABLE', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const record = envelopeRecord(step);
        if (!record) {
          return denied('ENVELOPE_MISSING', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const parsed = parseEnvelope(record.envelope);
        if (!parsed) {
          return denied('ENVELOPE_MALFORMED', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const material = materialFor(parsed.keyRef);
        if (!material) {
          return denied('KEY_UNKNOWN', 'SECURITY_KEY_ACCESS_DENIED');
        }
        if (step.keyId && step.keyId !== material.keyId) {
          return denied('KEY_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH', { keyState: material.state, keyRef: material.keyRef });
        }
        const keyRow = keyById.get(material.keyId);
        const dataRow = dataById.get(step.dataClass);
        if (!keyRow || !dataRow || !keyCoversDataClass(keyRow, step.dataClass) || !isAeadKeyClass(keyRow)) {
          return denied('PURPOSE_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH', { keyState: material.state, keyRef: material.keyRef });
        }
        const expectedPurpose = derivePurpose(keyById, dataRow);
        if (!expectedPurpose || step.purpose !== expectedPurpose) {
          return denied('PURPOSE_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH', { keyState: material.state, keyRef: material.keyRef });
        }
        if (!stateAllows(material.state, 'decrypt')) {
          return denied('KEY_STATE_DENIED', 'SECURITY_KEY_ACCESS_DENIED', { keyState: material.state, keyRef: material.keyRef });
        }
        const aad = step.aad || `onecall:v1:${expectedPurpose}:${step.dataClass}`;
        const body = parsed.body;
        const nonce = body.subarray(0, 12);
        const tag = body.subarray(body.length - 16);
        const ciphertext = body.subarray(12, body.length - 16);
        let plaintext;
        try {
          const decipher = crypto.createDecipheriv('aes-256-gcm', materialSecret(material), nonce);
          decipher.setAAD(Buffer.from(aad, 'utf8'));
          decipher.setAuthTag(tag);
          plaintext = Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
        } catch (error) {
          return denied('AAD_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH', { keyState: material.state, keyRef: material.keyRef });
        }
        const expectedHash = record.plaintextHash;
        const roundtripMatches = !expectedHash || sha256(plaintext) === expectedHash;
        if (!roundtripMatches) {
          return denied('ROUNDTRIP_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH', { keyState: material.state, keyRef: material.keyRef });
        }
        return {
          status: 'OK',
          aad,
          roundtripMatches: true,
          plaintextExposed: false,
          keyState: material.state,
          keyRef: material.keyRef,
          keyId: material.keyId,
          rowsWritten: 0
        };
      }
      case 'lookup': {
        const material = materialFor(step.keyRef);
        if (!material) {
          return denied('KEY_UNKNOWN', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const keyRow = keyById.get(material.keyId);
        const dataRow = dataById.get(step.dataClass);
        if (!keyRow || !dataRow || !keyCoversDataClass(keyRow, step.dataClass) || !keyRow.keyClass.startsWith('HMAC_')) {
          return denied('PURPOSE_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH');
        }
        if (!stateAllows(material.state, 'lookup')) {
          return denied('KEY_STATE_DENIED', 'SECURITY_KEY_ACCESS_DENIED', { keyState: material.state, keyRef: material.keyRef });
        }
        if (typeof step.storedHmac === 'string' && !new RegExp(EXPECTED_HMAC_REGEX).test(step.storedHmac)) {
          return denied('HMAC_MALFORMED', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const hmacHex = crypto.createHmac('sha256', materialSecret(material)).update(step.plaintext, 'utf8').digest('hex');
        state.hmacs.set(step.id, hmacHex);
        const result = {
          status: 'OK',
          keyId: material.keyId,
          keyState: material.state,
          hmacPattern: new RegExp(EXPECTED_HMAC_REGEX).test(hmacHex),
          hmacLength: hmacHex.length,
          scanMode: 'INDEXED_EQUALITY',
          plaintextScanned: false
        };
        if (step.equalsStep) {
          result.matched = hmacHex === state.hmacs.get(step.equalsStep);
        }
        if (step.differsFromStep) {
          result.differs = hmacHex !== state.hmacs.get(step.differsFromStep);
        }
        if (step.storedHmacStep) {
          result.matched = hmacHex === state.hmacs.get(step.storedHmacStep);
        }
        if (typeof step.storedHmac === 'string') {
          result.matched = hmacHex === step.storedHmac;
        }
        return result;
      }
      case 'rotate': {
        const material = materialFor(step.fromKeyRef);
        if (!material) {
          return denied('KEY_UNKNOWN', 'SECURITY_KEY_ACCESS_DENIED');
        }
        const keyRow = keyById.get(material.keyId);
        if (!keyRow) {
          return denied('UNREGISTERED_CLASS', 'SECURITY_KEY_PURPOSE_MISMATCH');
        }
        if (step.keyId && step.keyId !== material.keyId) {
          return denied('KEY_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH');
        }
        if (step.retireRequested === true && step.retentionApproved !== true) {
          return denied('RETENTION_UNRESOLVED', 'SECURITY_KEY_ACCESS_DENIED', { fromState: material.state, toState: material.state });
        }
        const oldState = keyRow.keyClass.startsWith('HMAC_')
          ? 'LOOKUP_ONLY'
          : keyRow.keyClass.endsWith('_SIGNING') || keyRow.keyClass.endsWith('_VERIFICATION')
            ? 'VERIFY_ONLY'
            : 'DECRYPT_ONLY';
        material.state = oldState;
        const next = {
          keyId: material.keyId,
          keyRef: step.toKeyRef,
          keyLabel: step.newKeyLabel,
          state: 'ACTIVE'
        };
        state.materials.set(next.keyRef, next);
        state.auditEvents.push('SECURITY_KEY_ROTATE');
        return {
          status: 'OK',
          fromState: oldState,
          toState: 'ACTIVE',
          auditEvent: 'SECURITY_KEY_ROTATE',
          retentionApproved: step.retentionApproved === true
        };
      }
      case 'retire': {
        const material = materialFor(step.keyRef);
        if (!material) {
          return denied('KEY_UNKNOWN', 'SECURITY_KEY_ACCESS_DENIED');
        }
        if (step.retentionApproved !== true) {
          return denied('RETENTION_UNRESOLVED', 'SECURITY_KEY_ACCESS_DENIED', { keyState: material.state });
        }
        material.state = 'DISABLED';
        state.auditEvents.push('SECURITY_KEY_RETIRE');
        return { status: 'OK', keyState: material.state, auditEvent: 'SECURITY_KEY_RETIRE' };
      }
      case 'recover': {
        const material = materialFor(step.keyRef);
        if (!material) {
          return denied('KEY_UNKNOWN', 'SECURITY_KEY_ACCESS_DENIED');
        }
        if (step.keyId && step.keyId !== material.keyId) {
          return denied('KEY_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH');
        }
        if (material.state === 'DESTROYED') {
          return denied('UNRECOVERABLE', 'SECURITY_KEY_ACCESS_DENIED', { keyState: 'DESTROYED' });
        }
        const previousState = material.state;
        material.state = 'ACTIVE';
        state.auditEvents.push('SECURITY_KEY_RECOVER');
        return { status: 'OK', previousState, keyState: 'ACTIVE', auditEvent: 'SECURITY_KEY_RECOVER' };
      }
      case 'persist': {
        if (step.appendOnly === true && step.attempt === 'rewrite') {
          return { status: 'DENIED', reason: 'APPEND_ONLY_VIOLATION', rowsWritten: 0, a0Mutations: 0, dataClass: step.dataClass, appendOnly: true };
        }
        if (step.attempt === 'insert') {
          return { status: 'OK', rowsWritten: 1, a0Mutations: 0, dataClass: step.dataClass, appendOnly: step.appendOnly === true };
        }
        return { status: 'FAILED', reason: 'UNSUPPORTED_PERSIST_ATTEMPT' };
      }
      case 'request': {
        if (step.kind !== 'a2-decision-replay') {
          return { status: 'FAILED', reason: 'UNSUPPORTED_REQUEST_KIND' };
        }
        const record = envelopeRecord(step);
        if (!record) {
          return { status: 'DENIED', reason: 'ENVELOPE_MISSING' };
        }
        const parsed = parseEnvelope(record.envelope);
        if (!parsed) {
          return { status: 'DENIED', reason: 'ENVELOPE_MALFORMED' };
        }
        const material = materialFor(parsed.keyRef);
        const keyRow = material ? keyById.get(material.keyId) : null;
        if (!material || !stateAllows(material.state, 'decrypt') || !isAeadKeyClass(keyRow)) {
          return { status: 'DENIED', reason: 'HISTORICAL_KEY_UNAVAILABLE' };
        }
        const body = parsed.body;
        let replayPlaintext;
        try {
          const decipher = crypto.createDecipheriv('aes-256-gcm', materialSecret(material), body.subarray(0, 12));
          decipher.setAAD(Buffer.from(`onecall:v1:${record.purpose}:${record.dataClass}`, 'utf8'));
          decipher.setAuthTag(body.subarray(body.length - 16));
          replayPlaintext = Buffer.concat([decipher.update(body.subarray(12, body.length - 16)), decipher.final()]).toString('utf8');
        } catch (error) {
          return { status: 'DENIED', reason: 'REPLAY_DECRYPT_FAILED' };
        }
        const payloadEqualsStored = !record.plaintextHash || sha256(replayPlaintext) === record.plaintextHash;
        if (!payloadEqualsStored) {
          return { status: 'DENIED', reason: 'REPLAY_PAYLOAD_MISMATCH' };
        }
        const attempted = Array.isArray(step.attemptedSideEffects) ? step.attemptedSideEffects.length : 0;
        return {
          status: 'OK',
          kind: step.kind,
          payloadEqualsStored: true,
          reservationsCreated: 0,
          settlementsCreated: 0,
          callsCreated: 0,
          eventsCreated: 0,
          deniedSideEffects: attempted
        };
      }
      case 'assert': {
        const actual = machineSummary();
        const mismatches = [];
        for (const [key, value] of Object.entries(step.checks || {})) {
          if (!deepEqual(actual[key], value)) {
            mismatches.push(key);
          }
        }
        return { status: mismatches.length === 0 ? 'OK' : 'FAILED', ...actual, mismatches };
      }
      default:
        return { status: 'FAILED', reason: 'UNKNOWN_ACTION' };
    }
  }

  function machineSummary() {
    const futureInvariant = collectStrings(model, []).filter((text) => /INV-A3-(0[7-9]|1[0-9]|2[01])\b/.test(text)).length;
    const futureFixtures = collectStrings(model, []).filter((text) => /sec-s[23]-/.test(text)).length;
    let forbiddenSections = 0;
    walkKeys(model, (key) => {
      if (FORBIDDEN_STAGE_SECTIONS.includes(key)) {
        forbiddenSections += 1;
      }
    });
    return {
      keyClasses: model.keyClasses.length,
      dataClasses: model.dataClasses.length,
      deferredInputs: model.deferredInputs.length,
      deferredQ: model.deferredInputs.filter((row) => /^Q[0-9]+$/.test(row.inputId)).length,
      deferredCv: model.deferredInputs.filter((row) => /^CV-[0-9]+$/.test(row.inputId)).length,
      invariants: model.invariants.length,
      futureInvariantIds: futureInvariant,
      futureFixtureIds: futureFixtures,
      s2s3Sections: forbiddenSections
    };
  }

  function loadMaterials(materials) {
    for (const material of materials || []) {
      state.materials.set(material.keyRef, {
        keyId: material.keyId,
        keyRef: material.keyRef,
        keyLabel: material.keyLabel,
        state: material.state
      });
    }
  }

  return { run, loadMaterials, state, machineSummary };
}

function checkExact(label, actual, expected, counters, issues) {
  if (!deepEqual(actual, expected)) {
    counters.stage_field_missing += 1;
    issues.push(`${label} does not exactly match the frozen S1 contract`);
    return false;
  }
  return true;
}

function checkA1Credential(schema, label, counters, issues) {
  if (!schema) {
    counters.a1_drift += 1;
    issues.push(`A1 ${label} schema is missing`);
    return;
  }
  const ok = schema.readOnly === true
    && schema.minLength === 1
    && schema.maxLength === 2048
    && schema['x-onecall-log-redaction'] === 'required'
    && schema['x-onecall-return-once'] !== true;
  if (!ok) {
    counters.a1_drift += 1;
    issues.push(`A1 ${label} credential shape drift`);
  }
}

function validateUpstreamA1(bundle, counters, issues) {
  const operationMap = new Map();
  const prefixCounts = new Map();
  const audiences = new Set();
  for (const [operationPath, item] of Object.entries(bundle.paths || {})) {
    for (const method of HTTP_METHODS) {
      const operation = item[method];
      if (!operation || typeof operation !== 'object') {
        continue;
      }
      operationMap.set(operation.operationId, { operation, path: operationPath, method });
      for (const [prefix, expected] of [['/api/v1/customer/', 18], ['/api/v1/operations/', 21], ['/api/v1/ivr/', 6], ['/api/v1/public/shares/', 3], ['/api/v1/callbacks/', 1]]) {
        if (operationPath.startsWith(prefix)) {
          prefixCounts.set(prefix, (prefixCounts.get(prefix) || 0) + 1);
          if (expected !== undefined) {
            break;
          }
        }
      }
      if (operation['x-onecall-audience']) {
        audiences.add(operation['x-onecall-audience']);
      }
    }
  }
  if (operationMap.size !== 49) {
    counters.a1_drift += 1;
    issues.push(`A1 bundle has ${operationMap.size} operations, expected 49`);
  }
  for (const [prefix, expected] of [['/api/v1/customer/', 18], ['/api/v1/operations/', 21], ['/api/v1/ivr/', 6], ['/api/v1/public/shares/', 3], ['/api/v1/callbacks/', 1]]) {
    const actual = prefixCounts.get(prefix) || 0;
    if (actual !== expected) {
      counters.a1_drift += 1;
      issues.push(`A1 prefix ${prefix} has ${actual} operations, expected ${expected}`);
    }
  }
  const expectedAudiences = ['customer', 'employee', 'ivr-service', 'public-capability', 'provider'];
  if (!deepEqual([...audiences].sort(), [...expectedAudiences].sort())) {
    counters.a1_drift += 1;
    issues.push('A1 x-onecall-audience values drift');
  }
  const schemes = Object.keys(bundle.components?.securitySchemes || {});
  const expectedSchemes = ['customerSession', 'employeeSession', 'ivrMtls', 'ivrSignature', 'providerCallbackMtls', 'providerCallbackSignature'];
  if (!deepEqual([...schemes].sort(), [...expectedSchemes].sort())) {
    counters.a1_drift += 1;
    issues.push('A1 security scheme set drift');
  }
  const schemas = bundle.components?.schemas || {};
  checkA1Credential(schemas.RecordingDeclaration?.properties?.upload?.properties?.credential, 'RecordingDeclaration.upload.credential', counters, issues);
  checkA1Credential(schemas.ShareAdmission?.properties?.mediaAuthorization?.properties?.credential, 'ShareAdmission.mediaAuthorization.credential', counters, issues);
  checkA1Credential(schemas.MediaAuthorization?.properties?.credential, 'MediaAuthorization.credential', counters, issues);
  const capabilityToken = schemas.ShareCreated?.properties?.capabilityToken;
  if (!capabilityToken || capabilityToken['x-onecall-return-once'] !== true || capabilityToken['x-onecall-log-redaction'] !== 'required' || capabilityToken.minLength !== 1 || capabilityToken.maxLength !== 512) {
    counters.a1_drift += 1;
    issues.push('A1 ShareCreated.capabilityToken return-once shape drift');
  }
  const extractionCode = schemas.ShareCreated?.properties?.extractionCode;
  if (!extractionCode || extractionCode['x-onecall-return-once'] !== true || extractionCode['x-onecall-log-redaction'] !== 'required' || extractionCode.minLength !== 1 || extractionCode.maxLength !== 128) {
    counters.a1_drift += 1;
    issues.push('A1 ShareCreated.extractionCode return-once shape drift');
  }
  for (const variant of schemas.ShareCodeRotation?.oneOf || []) {
    const name = (variant.$ref || '').split('/').pop();
    if (schemas[name]?.properties?.capabilityToken) {
      counters.a1_drift += 1;
      issues.push(`A1 ${name} must not expose capabilityToken`);
    }
  }
  const problemCodes = schemas.ProblemCode?.enum || [];
  for (const code of ['SERVICE_UNAVAILABLE', 'DEPENDENCY_UNAVAILABLE', 'STORAGE_UNKNOWN', 'UNAUTHENTICATED', 'FORBIDDEN', 'CALLBACK_SIGNATURE_INVALID']) {
    if (!problemCodes.includes(code)) {
      counters.a1_drift += 1;
      issues.push(`A1 ProblemCode ${code} is missing`);
    }
  }
  const parameters = bundle.components?.parameters || {};
  const headerChecks = [
    ['IvrTimestampParam', 'X-ONECALL-TIMESTAMP', 64],
    ['IvrNonceParam', 'X-ONECALL-NONCE', 128],
    ['ProviderCallbackTimestampParam', 'X-ONECALL-TIMESTAMP', 64],
    ['ProviderCallbackNonceParam', 'X-ONECALL-NONCE', 128]
  ];
  for (const [name, header, maxLength] of headerChecks) {
    const parameter = parameters[name];
    if (!parameter || parameter.name !== header || parameter.schema?.minLength !== 1 || parameter.schema?.maxLength !== maxLength) {
      counters.a1_drift += 1;
      issues.push(`A1 ${name} header/length drift`);
    }
  }
  if (bundle.components?.securitySchemes?.ivrSignature?.name !== 'X-ONECALL-SIGNATURE') {
    counters.a1_drift += 1;
    issues.push('A1 ivrSignature header name drift');
  }
  if (bundle.components?.securitySchemes?.providerCallbackSignature?.name !== 'X-ONECALL-CALLBACK-SIGNATURE') {
    counters.a1_drift += 1;
    issues.push('A1 providerCallbackSignature header name drift');
  }
  const ivrOps = [...operationMap.values()].filter((value) => value.path.startsWith('/api/v1/ivr/'));
  for (const value of ivrOps) {
    const security = value.operation.security || [];
    if (!security.some((entry) => entry.ivrMtls && entry.ivrSignature)) {
      counters.a1_drift += 1;
      issues.push(`A1 IVR operation ${value.operation.operationId} lacks mTLS+signature`);
    }
  }
  const providerOps = [...operationMap.values()].filter((value) => value.path.startsWith('/api/v1/callbacks/'));
  for (const value of providerOps) {
    const security = value.operation.security || [];
    if (!security.some((entry) => entry.providerCallbackMtls && entry.providerCallbackSignature)) {
      counters.a1_drift += 1;
      issues.push(`A1 provider operation ${value.operation.operationId} lacks mTLS+signature`);
    }
  }
  return { operationCount: operationMap.size, schemeCount: schemes.length };
}

function validateModel(model, counters, issues) {
  const actualKeys = Object.keys(model);
  for (const key of EXPECTED_TOP_LEVEL) {
    if (!actualKeys.includes(key)) {
      counters.stage_field_missing += 1;
      issues.push(`model: missing required S1 section ${key}`);
    }
  }
  for (const key of actualKeys) {
    if (!EXPECTED_TOP_LEVEL.includes(key)) {
      counters.stage_field_premature += 1;
      issues.push(`model: premature section ${key}`);
    }
  }
  walkKeys(model, (key, trail) => {
    if (FORBIDDEN_STAGE_SECTIONS.includes(key)) {
      counters.stage_field_premature += 1;
      issues.push(`model: premature S2/S3 field ${trail}`);
    }
  });

  checkExact('model.envelopeProfile', model.envelopeProfile, EXPECTED_ENVELOPE, counters, issues);
  checkExact('model.hmacProfile', model.hmacProfile, EXPECTED_HMAC, counters, issues);
  checkExact('model.rotationPolicy', model.rotationPolicy, EXPECTED_ROTATION, counters, issues);
  checkExact('model.recoveryPolicy', model.recoveryPolicy, EXPECTED_RECOVERY, counters, issues);
  checkExact('model.replayCompatibility', model.replayCompatibility, EXPECTED_REPLAY, counters, issues);
  const states = Array.isArray(model.keyStates)
    ? model.keyStates.map((row) => [row.state, row.allowedOperations])
    : null;
  if (!deepEqual(states, EXPECTED_KEY_STATES)) {
    counters.stage_field_missing += 1;
    issues.push('model.keyStates does not match the six frozen states and allowed operations');
  }

  const keyClasses = Array.isArray(model.keyClasses) ? model.keyClasses : [];
  if (keyClasses.length !== 20) {
    counters.stage_field_missing += 1;
    issues.push(`model.keyClasses count is ${keyClasses.length}, expected 20`);
  }
  const keyIds = new Set();
  const keyNames = new Set();
  const keyPurposes = new Set();
  for (const [index, row] of keyClasses.entries()) {
    const expected = EXPECTED_KEY_ROWS[index];
    if (!expected) {
      counters.stage_field_missing += 1;
      issues.push(`model.keyClasses has extra row at index ${index}`);
      continue;
    }
    const actual = [row.keyId, row.keyClass, row.custodyStorage, row.purpose, row.purposeId, row.algorithm, row.dataClasses, row.rotation, row.recovery, (row.allowedStates || []).join(','), row.owner];
    if (!deepEqual(actual, expected)) {
      counters.stage_field_missing += 1;
      issues.push(`model.keyClasses[${index}] ${row.keyId || '?'} drifts from the frozen key registry`);
    }
    if (row.failClosedStatus !== 503) {
      counters.stage_field_missing += 1;
      issues.push(`model.keyClasses[${index}] ${row.keyId || '?'} failClosedStatus must be 503`);
    }
    if (keyIds.has(row.keyId)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate keyId ${row.keyId}`);
    }
    if (keyNames.has(row.keyClass)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate keyClass ${row.keyClass}`);
    }
    if (keyPurposes.has(row.purposeId)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate key purpose ${row.purposeId}`);
    }
    keyIds.add(row.keyId);
    keyNames.add(row.keyClass);
    keyPurposes.add(row.purposeId);
    if (typeof row.owner !== 'string' || row.owner.trim() === '') {
      counters.stage_field_missing += 1;
      issues.push(`model.keyClasses[${index}] has no owner`);
    }
  }

  const dataClasses = Array.isArray(model.dataClasses) ? model.dataClasses : [];
  if (dataClasses.length !== 28) {
    counters.stage_field_missing += 1;
    issues.push(`model.dataClasses count is ${dataClasses.length}, expected 28`);
  }
  const dataIds = new Set();
  for (const [index, row] of dataClasses.entries()) {
    const expected = EXPECTED_DATA_ROWS[index];
    if (!expected) {
      counters.stage_field_missing += 1;
      issues.push(`model.dataClasses has extra row at index ${index}`);
      continue;
    }
    const actual = [row.dataClass, row.representation, row.keyClass, row.owner, row.projection, row.auditAccess, row.rotationReplay];
    if (!deepEqual(actual, expected)) {
      counters.stage_field_missing += 1;
      issues.push(`model.dataClasses[${index}] ${row.dataClass || '?'} drifts from the frozen data registry`);
    }
    if (dataIds.has(row.dataClass)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate dataClass ${row.dataClass}`);
    }
    dataIds.add(row.dataClass);
    for (const token of keyTokens(row)) {
      if (!keyIds.has(token)) {
        counters.stage_field_missing += 1;
        issues.push(`dataClass ${row.dataClass} references unknown key ${token}`);
      }
    }
  }

  const deferred = Array.isArray(model.deferredInputs) ? model.deferredInputs : [];
  if (deferred.length !== 23) {
    counters.stage_field_missing += 1;
    issues.push(`model.deferredInputs count is ${deferred.length}, expected 23`);
  }
  const qRows = deferred.filter((row) => /^Q[0-9]+$/.test(row.inputId));
  const cvRows = deferred.filter((row) => /^CV-[0-9]+$/.test(row.inputId));
  if (qRows.length !== 18 || cvRows.length !== 5) {
    counters.stage_field_missing += 1;
    issues.push(`deferred partition is ${qRows.length} Q + ${cvRows.length} CV, expected 18 + 5`);
  }
  const qIds = qRows.map((row) => row.inputId);
  const expectedQIds = EXPECTED_DEFERRED_Q.map((row) => row[0]);
  if (!deepEqual(qIds, expectedQIds)) {
    counters.stage_field_missing += 1;
    issues.push('deferred Q rows are missing, duplicated or out of order');
  }

  const invariants = Array.isArray(model.invariants) ? model.invariants : [];
  if (invariants.length !== 6) {
    counters.stage_field_missing += 1;
    issues.push(`model.invariants count is ${invariants.length}, expected 6`);
  }
  const invariantPairs = invariants.map((row) => [row.id, row.statement]);
  if (!deepEqual(invariantPairs, EXPECTED_INVARIANTS)) {
    counters.stage_field_missing += 1;
    issues.push('model.invariants do not exactly match INV-A3-01..06');
  }

  const modelStrings = [];
  collectStrings(model, modelStrings);
  for (const text of modelStrings) {
    if (/INV-A3-(0[7-9]|1[0-9]|2[01])\b/.test(text)) {
      counters.stage_field_premature += 1;
      issues.push('model contains a future S2/S3 invariant ID');
    }
    if (/sec-s[23]-/.test(text)) {
      counters.stage_field_premature += 1;
      issues.push('model contains a future S2/S3 fixture ID');
    }
    if (/https?:\/\//.test(text)) {
      counters.stage_field_premature += 1;
      issues.push('model contains a URL');
    }
  }

  return { keyClasses, dataClasses, deferred, invariants, qRows, cvRows, keyIds };
}

function validateDeferredAgainstA0(model, a0Inputs, counters, issues) {
  const deferred = Array.isArray(model.deferredInputs) ? model.deferredInputs : [];
  for (const row of deferred) {
    if (/^Q[0-9]+$/.test(row.inputId)) {
      const a0 = a0Inputs.get(row.inputId);
      if (!a0) {
        counters.stage_field_missing += 1;
        issues.push(`deferred ${row.inputId} is missing from A0 section 14`);
        continue;
      }
      if (row.input !== a0.input || row.owner !== a0.owner || row.destination !== a0.destination) {
        counters.stage_field_missing += 1;
        issues.push(`deferred ${row.inputId} input/owner/destination drift from A0 section 14`);
      }
      const expected = EXPECTED_DEFERRED_Q.find((entry) => entry[0] === row.inputId);
      if (!expected || row.valueKind !== expected[1] || row.a3Treatment !== expected[2]) {
        counters.stage_field_missing += 1;
        issues.push(`deferred ${row.inputId} valueKind/a3Treatment drift`);
      }
    } else if (/^CV-[0-9]+$/.test(row.inputId)) {
      if (a0Inputs.has(row.inputId)) {
        counters.stage_field_missing += 1;
        issues.push(`deferred ${row.inputId} must not appear in A0 section 14`);
      }
      const expected = EXPECTED_CV_ROWS.find((entry) => entry[0] === row.inputId);
      const actual = [row.inputId, row.input, row.valueKind, row.a3Treatment, row.owner, row.destination];
      if (!expected || !deepEqual(actual, expected)) {
        counters.stage_field_missing += 1;
        issues.push(`deferred ${row.inputId} drifts from the A3-origin CV registry`);
      }
      if (!row.owner || !row.destination) {
        counters.stage_field_missing += 1;
        issues.push(`deferred ${row.inputId} has an empty owner or destination`);
      }
    } else {
      counters.stage_field_missing += 1;
      issues.push(`deferred input ${row.inputId} is neither Q nor CV`);
    }
  }
}

function validateFixtures(fixturesDir, model, counters, issues) {
  let files = [];
  try {
    files = fs.readdirSync(fixturesDir).sort();
  } catch (error) {
    counters.failed += 1;
    issues.push(`fixtures directory is unreadable: ${error.message}`);
    return { fixtureCount: 0, negativeCount: 0, sliceCount: 0 };
  }
  const jsonFiles = files.filter((name) => name.endsWith('.json'));
  for (const name of files.filter((entry) => !entry.endsWith('.json'))) {
    counters.fixture_id_extra += 1;
    issues.push(`non-JSON file in fixtures directory: ${name}`);
  }
  const entries = [];
  for (const fileName of jsonFiles) {
    try {
      entries.push({ fileName, fixture: JSON.parse(readUtf8(path.join(fixturesDir, fileName))) });
    } catch (error) {
      counters.failed += 1;
      issues.push(`fixture ${fileName}: ${error.message}`);
    }
  }
  const actualIds = entries.map((entry) => entry.fixture?.id).filter((id) => typeof id === 'string');
  const expectedIds = EXPECTED_FIXTURES.map((entry) => entry[0]);
  for (const id of expectedIds) {
    if (!actualIds.includes(id)) {
      counters.fixture_id_missing += 1;
      issues.push(`missing fixture ID ${id}`);
    }
  }
  for (const id of actualIds) {
    if (!expectedIds.includes(id)) {
      counters.fixture_id_extra += 1;
      issues.push(`extra fixture ID ${id}`);
    }
  }
  if (new Set(actualIds).size !== actualIds.length) {
    counters.fixture_id_extra += 1;
    issues.push('duplicate fixture ID detected');
  }

  for (const { fileName, fixture } of entries) {
    const fixtureIssues = [];
    if (fixture.schemaVersion !== 1) fixtureIssues.push('schemaVersion must be 1');
    if (fixture.slice !== 's1') fixtureIssues.push('slice must be s1');
    if (fileName !== `${fixture.id}.json`) fixtureIssues.push(`file name ${fileName} does not match ID ${fixture.id}`);
    if (!Array.isArray(fixture.steps) || fixture.steps.length === 0) fixtureIssues.push('steps must be a non-empty array');
    const expectedNegative = EXPECTED_FIXTURES.find((entry) => entry[0] === fixture.id)?.[1];
    if (expectedNegative !== undefined && fixture.negative !== expectedNegative) {
      fixtureIssues.push(`negative flag must be ${expectedNegative}`);
    }
    for (const material of fixture.materials || []) {
      if (typeof material.keyLabel !== 'string' || !material.keyLabel.startsWith('TEST_ONLY_')) {
        fixtureIssues.push(`material ${material.keyId || '?'} keyLabel must start with TEST_ONLY_`);
      }
      if (typeof material.keyRef !== 'string' || !material.keyRef.startsWith('TEST_ONLY_')) {
        fixtureIssues.push(`material ${material.keyId || '?'} keyRef must start with TEST_ONLY_`);
      }
      if (!['ACTIVE', 'DECRYPT_ONLY', 'LOOKUP_ONLY', 'VERIFY_ONLY', 'DISABLED', 'DESTROYED'].includes(material.state)) {
        fixtureIssues.push(`material ${material.keyId || '?'} has invalid state`);
      }
    }
    const stepIds = new Set();
    for (const step of fixture.steps || []) {
      if (!step.id || stepIds.has(step.id)) {
        fixtureIssues.push(`step ID ${step.id || '?'} is missing or duplicated`);
      }
      stepIds.add(step.id);
      if (step.action === 'encrypt' && (typeof step.plaintext !== 'string' || !step.plaintext.startsWith('TEST_ONLY_'))) {
        fixtureIssues.push(`encrypt step ${step.id} plaintext must start with TEST_ONLY_`);
      }
      if (step.action === 'encrypt' && !/^[0-9a-f]{24}$/.test(step.nonceHex || '')) {
        fixtureIssues.push(`encrypt step ${step.id} nonceHex must be 24 lowercase hex characters`);
      }
      if (step.action === 'rotate') {
        for (const field of ['fromKeyRef', 'toKeyRef', 'newKeyLabel']) {
          if (typeof step[field] !== 'string' || !step[field].startsWith('TEST_ONLY_')) {
            fixtureIssues.push(`rotate step ${step.id} ${field} must start with TEST_ONLY_`);
          }
        }
      }
      if (!step.expected || typeof step.expected !== 'object') {
        fixtureIssues.push(`step ${step.id} has no expected result`);
      }
    }
    const fixtureStrings = [];
    collectStrings(fixture, fixtureStrings);
    for (const text of fixtureStrings) {
      if (/https?:\/\//.test(text)) {
        fixtureIssues.push('fixture contains a URL');
      }
      if (/INV-A3-(0[7-9]|1[0-9]|2[01])\b/.test(text)) {
        fixtureIssues.push('fixture contains a future S2/S3 invariant ID');
      }
      for (const section of FORBIDDEN_STAGE_SECTIONS) {
        if (text.includes(section)) {
          fixtureIssues.push(`fixture contains premature S2/S3 section token ${section}`);
        }
      }
    }
    if (fixtureIssues.length > 0) {
      counters.failed += 1;
      fixtureIssues.forEach((issue) => issues.push(`fixture ${fixture.id}: ${issue}`));
      continue;
    }
    const machine = createMachine(model);
    machine.loadMaterials(fixture.materials);
    let ok = true;
    for (const step of fixture.steps) {
      const result = machine.run(step);
      for (const [key, value] of Object.entries(step.expected)) {
        if (!(key in result)) {
          ok = false;
          issues.push(`fixture ${fixture.id} step ${step.id}: expected field ${key} is absent from the computed result`);
        } else if (!deepEqual(value, result[key])) {
          ok = false;
          issues.push(`fixture ${fixture.id} step ${step.id}: ${key} expected ${JSON.stringify(value)} but computed ${JSON.stringify(result[key])}`);
        }
      }
    }
    if (ok) {
      counters.passed += 1;
    } else {
      counters.failed += 1;
    }
  }
  const negativeCount = entries.filter((entry) => entry.fixture?.negative === true).length;
  const sliceCount = actualIds.filter((id) => id.startsWith('sec-s1-')).length;
  return { fixtureCount: jsonFiles.length, negativeCount, sliceCount };
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
    a0_drift: 0,
    a1_drift: 0,
    a2_drift: 0,
    passed: 0,
    failed: 0
  };
  const issues = [];
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.stdout.write('FAIL security-validation (invalid-arguments)\n');
    process.exit(1);
  }

  let model;
  let doc;
  let a0Doc;
  let bundle;
  let a2Model;
  try {
    model = JSON.parse(readUtf8(args.model));
    doc = readUtf8(args.spec);
    a0Doc = readUtf8(args.a0);
    bundle = JSON.parse(readUtf8(args.a1Bundle));
    a2Model = JSON.parse(readUtf8(args.a2Model));
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.stdout.write(`FAIL security-validation (${args.stage})\n`);
    process.exit(1);
  }

  const modelInfo = validateModel(model, counters, issues);
  const a1Info = validateUpstreamA1(bundle, counters, issues);

  const a0Tables = countA0Catalog(a0Doc);
  if (a0Tables !== 44) {
    counters.a0_drift += 1;
    issues.push(`A0 catalog has ${a0Tables} tables, expected 44`);
  }
  const a0SensitiveRows = extractA0SensitiveRows(a0Doc);
  if (a0SensitiveRows.length !== 28) {
    counters.a0_drift += 1;
    issues.push(`A0 sensitive inventory has ${a0SensitiveRows.length} rows, expected 28`);
  }
  if (modelInfo.dataClasses.length !== a0SensitiveRows.length) {
    counters.a0_drift += 1;
    issues.push('model dataClasses count does not match the A0 sensitive inventory');
  }
  for (let index = 0; index < Math.min(modelInfo.dataClasses.length, a0SensitiveRows.length); index += 1) {
    const row = modelInfo.dataClasses[index];
    if (row.a0Inventory !== a0SensitiveRows[index]) {
      counters.a0_drift += 1;
      issues.push(`dataClass ${row.dataClass} a0Inventory drifts from A0 sensitive row ${index + 1}`);
    }
  }
  if (!a0Doc.includes(EXPECTED_A0_ENVELOPE_REGEX)) {
    counters.a0_drift += 1;
    issues.push('A0 envelope regex is missing');
  }
  if (!a0Doc.includes(EXPECTED_HMAC_REGEX)) {
    counters.a0_drift += 1;
    issues.push('A0 HMAC regex is missing');
  }
  if (!a0Doc.includes('auth_identity') || !a0Doc.includes('key_version')) {
    counters.a0_drift += 1;
    issues.push('A0 auth_identity.key_version rotation rule is missing');
  }
  const a0Inputs = extractA0OpenInputs(a0Doc);
  if (a0Inputs.size !== 28) {
    counters.a0_drift += 1;
    issues.push(`A0 section 14 has ${a0Inputs.size} open inputs, expected 28`);
  }
  validateDeferredAgainstA0(model, a0Inputs, counters, issues);

  if (a2Model.messages?.length !== 12) {
    counters.a2_drift += 1;
    issues.push(`A2 messages count is ${a2Model.messages?.length}, expected 12`);
  }
  if (a2Model.eventTypes?.length !== 11) {
    counters.a2_drift += 1;
    issues.push(`A2 eventTypes count is ${a2Model.eventTypes?.length}, expected 11`);
  }
  if (a2Model.identities?.length !== 22) {
    counters.a2_drift += 1;
    issues.push(`A2 identities count is ${a2Model.identities?.length}, expected 22`);
  }
  if (a2Model.invariants?.length !== 15) {
    counters.a2_drift += 1;
    issues.push(`A2 invariants count is ${a2Model.invariants?.length}, expected 15`);
  }
  const a2Digest = sha256(canonicalJson(a2Model.eventTypes));
  if (a2Digest !== EXPECTED_EVENT_DIGEST || a2Model.eventRegistry?.sha256 !== EXPECTED_EVENT_DIGEST) {
    counters.a2_drift += 1;
    issues.push('A2 event registry digest drift');
  }
  let a2FixtureCount = 0;
  try {
    a2FixtureCount = fs.readdirSync(args.a2Fixtures).filter((name) => name.endsWith('.json')).length;
  } catch (error) {
    counters.a2_drift += 1;
    issues.push(`A2 fixtures directory is unreadable: ${error.message}`);
  }
  if (a2FixtureCount !== 25) {
    counters.a2_drift += 1;
    issues.push(`A2 fixtures count is ${a2FixtureCount}, expected 25`);
  }

  if (!deepEqual(model.upstream?.a0?.tables, 44) || !deepEqual(model.upstream?.a0?.sensitiveRows, 28)) {
    counters.a0_drift += 1;
    issues.push('model upstream A0 counters drift');
  }
  if (!deepEqual(model.upstream?.a1?.operations, 49) || !deepEqual(model.upstream?.a1?.securitySchemes, 6)) {
    counters.a1_drift += 1;
    issues.push('model upstream A1 counters drift');
  }
  const upstreamAudiences = Array.isArray(model.upstream?.a1?.audiences) ? [...model.upstream.a1.audiences].sort() : [];
  const expectedAudiences = ['customer', 'employee', 'ivr-service', 'public-capability', 'provider'].sort();
  if (!deepEqual(upstreamAudiences, expectedAudiences)) {
    counters.a1_drift += 1;
    issues.push('model upstream A1 audience values drift');
  }
  if (!deepEqual(model.upstream?.a2?.messages, 12)
    || !deepEqual(model.upstream?.a2?.eventTypes, 11)
    || !deepEqual(model.upstream?.a2?.identities, 22)
    || !deepEqual(model.upstream?.a2?.invariants, 15)
    || model.upstream?.a2?.eventRegistrySha256 !== EXPECTED_EVENT_DIGEST) {
    counters.a2_drift += 1;
    issues.push('model upstream A2 counters or digest drift');
  }

  const keyMarker = extractMarker(doc, MARKER_CONFIG.key.name, counters, issues);
  const dataMarker = extractMarker(doc, MARKER_CONFIG.data.name, counters, issues);
  const deferredMarker = extractMarker(doc, MARKER_CONFIG.deferred.name, counters, issues);
  const markerDefinitions = [
    {
      name: MARKER_CONFIG.key.name,
      marker: keyMarker,
      header: MARKER_CONFIG.key.header,
      rows: modelInfo.keyClasses.map((row) => MARKER_CONFIG.key.header.map((column) => String(row[column])))
    },
    {
      name: MARKER_CONFIG.data.name,
      marker: dataMarker,
      header: MARKER_CONFIG.data.header,
      rows: modelInfo.dataClasses.map((row) => MARKER_CONFIG.data.header.map((column) => String(row[column])))
    },
    {
      name: MARKER_CONFIG.deferred.name,
      marker: deferredMarker,
      header: MARKER_CONFIG.deferred.header,
      rows: modelInfo.deferred.map((row) => MARKER_CONFIG.deferred.header.map((column) => String(row[column])))
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
    for (const expectedRow of definition.rows) {
      if (!definition.marker.rows.some((row) => deepEqual(row, expectedRow))) {
        counters.doc_missing += 1;
        issues.push(`${definition.name}: missing row ${expectedRow[0]}`);
      }
    }
    for (const row of definition.marker.rows) {
      if (!definition.rows.some((expectedRow) => deepEqual(row, expectedRow))) {
        counters.doc_extra += 1;
        issues.push(`${definition.name}: extra or drifted row ${row[0]}`);
      }
    }
    if (definition.marker.rows.length !== definition.rows.length) {
      counters.doc_invalid += 1;
      issues.push(`${definition.name}: row count ${definition.marker.rows.length}, expected ${definition.rows.length}`);
    }
    if (definition.marker.rows.length === definition.rows.length && !deepEqual(definition.marker.rows, definition.rows)) {
      counters.doc_invalid += 1;
      issues.push(`${definition.name}: row order drifts from the model`);
    }
  }

  const tokens = new Set();
  for (const row of modelInfo.keyClasses) {
    tokens.add(row.keyId);
    tokens.add(row.keyClass);
  }
  for (const row of modelInfo.dataClasses) {
    tokens.add(row.dataClass);
  }
  for (const row of modelInfo.deferred) {
    tokens.add(row.inputId);
  }
  for (const row of modelInfo.invariants) {
    tokens.add(row.id);
  }
  for (const [state] of EXPECTED_KEY_STATES) {
    tokens.add(state);
  }
  for (const token of [...tokens].sort()) {
    if (!doc.includes(token)) {
      counters.token_missing += 1;
      issues.push(`document does not mention model token ${token}`);
    }
  }
  for (const row of modelInfo.invariants) {
    if (!doc.includes(row.statement)) {
      counters.token_missing += 1;
      issues.push(`document does not contain invariant statement ${row.id}`);
    }
  }

  const futureInvariantMatch = doc.match(/INV-A3-(0[7-9]|1[0-9]|2[01])\b/);
  if (futureInvariantMatch) {
    counters.stage_field_premature += 1;
    issues.push('document contains a future S2/S3 invariant ID');
  }
  if (/sec-s[23]-/.test(doc)) {
    counters.stage_field_premature += 1;
    issues.push('document contains a future S2/S3 fixture ID');
  }
  for (const section of FORBIDDEN_STAGE_SECTIONS) {
    if (doc.includes(section)) {
      counters.stage_field_premature += 1;
      issues.push(`document contains premature S2/S3 section token ${section}`);
    }
  }
  if (/https?:\/\//.test(doc)) {
    counters.stage_field_premature += 1;
    issues.push('document contains a URL');
  }

  const fixtureInfo = validateFixtures(args.fixtures, model, counters, issues);

  const drift = counters.a0_drift + counters.a1_drift + counters.a2_drift;
  const keyCount = modelInfo.keyClasses.length;
  const dataCount = modelInfo.dataClasses.length;
  const deferredCount = modelInfo.deferred.length;
  const deferredQ = modelInfo.deferred.filter((row) => /^Q[0-9]+$/.test(row.inputId)).length;
  const deferredCv = modelInfo.deferred.filter((row) => /^CV-[0-9]+$/.test(row.inputId)).length;
  const invariantCount = modelInfo.invariants.length;

  process.stdout.write(`stage=s1 key_classes=${keyCount} data_classes=${dataCount} deferred_inputs=${deferredCount} deferred_q=${deferredQ} deferred_cv=${deferredCv} invariants=${invariantCount} fixtures=${fixtureInfo.fixtureCount} passed=${counters.passed} failed=${counters.failed} negative=${fixtureInfo.negativeCount} slice_fixtures=${fixtureInfo.sliceCount}\n`);
  process.stdout.write(`a0_tables=${a0Tables} a0_sensitive_rows=${a0SensitiveRows.length} a1_operations=${a1Info.operationCount} a1_schemes=${a1Info.schemeCount} a2_messages=${a2Model.messages?.length || 0} a2_event_types=${a2Model.eventTypes?.length || 0} a2_identities=${a2Model.identities?.length || 0} a2_invariants=${a2Model.invariants?.length || 0} a2_event_registry_sha256=${a2Digest} drift=${drift}\n`);
  process.stdout.write(`doc_missing=${counters.doc_missing} doc_extra=${counters.doc_extra} doc_duplicate=${counters.doc_duplicate} doc_invalid=${counters.doc_invalid} token_missing=${counters.token_missing}\n`);
  process.stdout.write(`stage_field_missing=${counters.stage_field_missing} stage_field_premature=${counters.stage_field_premature} fixture_id_missing=${counters.fixture_id_missing} fixture_id_extra=${counters.fixture_id_extra}\n`);

  const failed = counters.passed !== fixtureInfo.fixtureCount
    || fixtureInfo.fixtureCount !== 6
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
    || drift !== 0
    || issues.length !== 0;
  if (failed) {
    for (const issue of issues) {
      process.stderr.write(`- ${issue}\n`);
    }
    process.stdout.write(`FAIL security-validation (${args.stage})\n`);
    process.exit(1);
  }
  process.stdout.write(`PASS security-validation (${args.stage})\n`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main();
}

export { EXPECTED_FIXTURES, EXPECTED_KEY_ROWS, EXPECTED_DATA_ROWS, validateFixtures };
