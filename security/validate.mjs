#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const HTTP_METHODS = ['get', 'post', 'put', 'patch', 'delete'];
const EXPECTED_EVENT_DIGEST = 'c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9';
const EXPECTED_A0_ENVELOPE_REGEX = '^v1\\.[A-Za-z0-9._:/-]+\\..+$';
const EXPECTED_HMAC_REGEX = '^[0-9a-f]{64}$';

const S1_MODEL_SECTIONS = [
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

const S2_MODEL_SECTIONS = [
  'trustSurfaces',
  'authDomains',
  'trustBoundaries',
  'serviceIdentities',
  'credentialClasses',
  'signatureProfiles',
  'noncePolicy',
  'audienceRegistry'
];

const S3_MODEL_SECTIONS = [
  'mediaPolicies',
  'cosPolicy',
  'auditEvents',
  'highRiskActions',
  'auditCoverage',
  'redactionPolicy',
  'failClosedMatrix'
];

const STAGE_CONFIG = {
  s1: {
    required: S1_MODEL_SECTIONS,
    s2Sections: 'optional',
    forbidden: S3_MODEL_SECTIONS,
    invariantCounts: [6, 11],
    futureInvariantPattern: /INV-A3-(1[2-9]|2[01])\b/,
    fixtureSlices: ['s1'],
    allowS2Fixtures: true
  },
  s2: {
    required: [...S1_MODEL_SECTIONS, ...S2_MODEL_SECTIONS],
    s2Sections: 'required',
    forbidden: S3_MODEL_SECTIONS,
    invariantCounts: [11],
    futureInvariantPattern: /INV-A3-(1[2-9]|2[01])\b/,
    fixtureSlices: ['s1', 's2'],
    allowS2Fixtures: true
  }
};

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

const EXPECTED_INVARIANTS_S2 = [
  ['INV-A3-07', 'The five authentication domains never share a token, credential, audience or verifier; a request that crosses domains is rejected.'],
  ['INV-A3-08', 'The six service identities use least privilege, independent CAM roles and rotation; no frontend holds a service secret and no policy grants a wildcard action or resource.'],
  ['INV-A3-09', 'Every IVR request must pass mTLS, signature, timestamp and nonce verification; the verification order and canonical string are fixed.'],
  ['INV-A3-10', 'Replay rejection occurs before any domain side effect; transport nonce and timestamp are refreshed on every retry while A2 eventId and payload stay unchanged.'],
  ['INV-A3-11', 'Provider callbacks require mTLS, vendor signature, timestamp and nonce; the same event and fingerprint is idempotent while a changed payload is a conflict.']
];

const EXPECTED_INVARIANTS_CUMULATIVE = [...EXPECTED_INVARIANTS, ...EXPECTED_INVARIANTS_S2];

const EXPECTED_TRUST_SURFACES = [
  { surfaceId: 'TS1', domain: 'customer', prefix: '/api/v1/customer/', operations: 18, boundaries: ['TB1', 'TB2'], authentication: 'customerSession audience onecall:customer-session:v1' },
  { surfaceId: 'TS2', domain: 'employee', prefix: '/api/v1/operations/', operations: 21, boundaries: ['TB1', 'TB2'], authentication: 'employeeSession audience onecall:employee-session:v1 with fixed ADMIN/OPS' },
  { surfaceId: 'TS3', domain: 'ivr-service', prefix: '/api/v1/ivr/', operations: 6, boundaries: ['TB3'], authentication: 'ivrMtls plus ivrSignature plus timestamp plus nonce' },
  { surfaceId: 'TS4', domain: 'public-capability', prefix: '/api/v1/public/shares/', operations: 3, boundaries: ['TB1', 'TB2'], authentication: 'capability credential C07 or C08; optional C01 only for ORGANIZATION_MEMBER; C10 media presentation' },
  { surfaceId: 'TS5', domain: 'provider', prefix: '/api/v1/callbacks/', operations: 1, boundaries: ['TB4'], authentication: 'providerCallbackMtls plus providerCallbackSignature plus timestamp plus nonce' }
];

const EXPECTED_AUTH_DOMAINS = [
  { domain: 'customer', prefix: '/api/v1/customer/', operations: 18, credentialClasses: ['C01 customerSession'], storage: 'Redis session record + account state', tokenAudience: 'onecall:customer-session:v1', crossDomainRule: 'reject any other domain token; gateway trust never bypasses the audience check', trustSurfaceId: 'TS1' },
  { domain: 'employee', prefix: '/api/v1/operations/', operations: 21, credentialClasses: ['C02 employeeSession'], storage: 'Redis session record + employee state', tokenAudience: 'onecall:employee-session:v1', crossDomainRule: 'independent issuer; fixed ADMIN/OPS; never reused with the customer domain', trustSurfaceId: 'TS2' },
  { domain: 'ivr-service', prefix: '/api/v1/ivr/', operations: 6, credentialClasses: ['C03 ivrMtls', 'C04 ivrSignature'], storage: 'mTLS certificate store + Secret Manager HMAC key', tokenAudience: 'onecall:ivr-request:v1', crossDomainRule: 'accept mTLS plus signature only; never a session or capability token', trustSurfaceId: 'TS3' },
  { domain: 'public-capability', prefix: '/api/v1/public/shares/', operations: 3, credentialClasses: ['C07 publicShareCapabilityToken', 'C08 extractionCode', 'C01 customerSession (optional ORGANIZATION_MEMBER only)', 'C10 playbackCredential'], storage: 'DB SHA-256 digest + admission state + Redis ticket Q22', tokenAudience: 'onecall:share-capability:v1; onecall:media-playback:v1', crossDomainRule: 'capability token cannot be used on another surface; customer session cannot replace capability; C10 is presented to media-gateway only', trustSurfaceId: 'TS4' },
  { domain: 'provider', prefix: '/api/v1/callbacks/', operations: 1, credentialClasses: ['C05 providerCallbackMtls', 'C06 providerCallbackSignature'], storage: 'mTLS certificate store + Secret Manager vendor verification material', tokenAudience: 'onecall:provider-callback:v1', crossDomainRule: 'accept provider mTLS plus vendor signature only; never disclose an internal result', trustSurfaceId: 'TS5' }
];

const EXPECTED_TRUST_BOUNDARIES = [
  { boundaryId: 'TB1', boundary: 'Internet and edge to customer, public-share and operations frontends', allowed: 'TLS and WAF; customer, employee and capability domains', forbidden: 'direct database, Redis, message queue or COS access' },
  { boundaryId: 'TB2', boundary: 'Edge to core-api customer, operations and public-share APIs', allowed: 'matching session or capability credential with path and audience checks', forbidden: 'cross-domain token and permanent object URL' },
  { boundaryId: 'TB3', boundary: 'Carrier premises and IVR network to core-api IVR API', allowed: 'mTLS plus IVR signature plus timestamp plus nonce', forbidden: 'direct database or message queue access; unsigned request' },
  { boundaryId: 'TB4', boundary: 'Internet and provider to callback API', allowed: 'provider mTLS plus vendor signature plus timestamp plus nonce', forbidden: 'internal result disclosure and order amount or payment success claims' },
  { boundaryId: 'TB5', boundary: 'core-api and workers to PostgreSQL, Redis and RabbitMQ', allowed: 'NOLOGIN database role, Outbox and least privilege', forbidden: 'cross-module writes, client direct access and bypassing the Outbox' },
  { boundaryId: 'TB6', boundary: 'core-api, workers and ivr-gateway to Tencent KMS, Secret Manager and COS', allowed: 'CAM service role, short-lived upload credential and private bucket', forbidden: 'static AccessKey, public object and permanent URL' },
  { boundaryId: 'TB7', boundary: 'Applications to logs, traces, metrics, analytics, support and public export', allowed: 'redaction pipeline, masked value, internal ID, status and audit access', forbidden: 'plaintext, secret, credential URL, HMAC or ciphertext' }
];

const EXPECTED_SERVICE_IDENTITIES = [
  { serviceIdentity: 'core-api', mechanism: 'Tencent CAM service role + DB NOLOGIN role', audience: 'onecall:service:core:v1', allowedCalls: ['DB', 'Redis', 'Outbox', 'KMS', 'Secret Manager', 'COS evidence metadata'], secretCustody: 'Secret Manager or instance role; no static key', failClosedStatus: 503 },
  { serviceIdentity: 'async-worker', mechanism: 'Tencent CAM service role + DB role', audience: 'onecall:service:worker:v1', allowedCalls: ['DB', 'MQ consumer', 'KMS', 'Secret Manager', 'COS', 'notary adapter', 'AI adapter'], secretCustody: 'Secret Manager or instance role', failClosedStatus: 503 },
  { serviceIdentity: 'scheduler', mechanism: 'Tencent CAM service role + DB role', audience: 'onecall:service:scheduler:v1', allowedCalls: ['DB lease', 'expiry scan'], secretCustody: 'minimal least privilege; no external provider key', failClosedStatus: 503 },
  { serviceIdentity: 'media-ai-worker', mechanism: 'Tencent CAM service role', audience: 'onecall:service:media-ai:v1', allowedCalls: ['MQ', 'COS read', 'COS write', 'AI provider Secret Manager'], secretCustody: 'Secret Manager; no business DB write', failClosedStatus: 503 },
  { serviceIdentity: 'ivr-gateway', mechanism: 'mTLS client + Secret Manager', audience: 'onecall:ivr-request:v1', allowedCalls: ['A1 IVR API', 'controlled COS upload'], secretCustody: 'C03 mTLS credential + C04 signing key', failClosedStatus: 503 },
  { serviceIdentity: 'media-gateway', mechanism: 'mTLS or internal audience + Secret Manager', audience: 'onecall:media-playback:v1', allowedCalls: ['C10 validation', 'COS media read'], secretCustody: 'C10 signing key; never issues upload credential', failClosedStatus: 503 }
];

const EXPECTED_CREDENTIAL_CLASSES = [
  { credentialClass: 'C01 customerSession', credentialId: 'C01', domain: 'customer', audience: 'onecall:customer-session:v1', storage: 'Redis session record + account state', ttlInput: 'Q1/Q19', scope: 'customer API only', rotationRevocation: 'logout, account disable or expiry revokes the session', redaction: 'bearer and Authorization header always removed' },
  { credentialClass: 'C02 employeeSession', credentialId: 'C02', domain: 'employee', audience: 'onecall:employee-session:v1', storage: 'Redis session record + employee state', ttlInput: 'Q1', scope: 'operations API only', rotationRevocation: 'role change, disable or credential reset revokes the session', redaction: 'bearer and Authorization header always removed' },
  { credentialClass: 'C03 ivrMtls', credentialId: 'C03', domain: 'ivr-service', audience: 'onecall:ivr-request:v1', storage: 'mTLS certificate store', ttlInput: 'Q7/Q15', scope: 'IVR mTLS identity only', rotationRevocation: 'certificate overlap and rotation per Q7', redaction: 'certificate and private key never logged' },
  { credentialClass: 'C04 ivrSignature', credentialId: 'C04', domain: 'ivr-service', audience: 'onecall:ivr-request:v1', storage: 'Secret Manager K17', ttlInput: 'Q25 window', scope: 'per-request signature', rotationRevocation: 'keyId overlap; old key VERIFY_ONLY until retirement', redaction: 'signature and nonce always removed' },
  { credentialClass: 'C05 providerCallbackMtls', credentialId: 'C05', domain: 'provider', audience: 'onecall:provider-callback:v1', storage: 'mTLS certificate store', ttlInput: 'Q6/Q15', scope: 'provider callback only', rotationRevocation: 'provider certificate rotation per Q6', redaction: 'certificate never logged' },
  { credentialClass: 'C06 providerCallbackSignature', credentialId: 'C06', domain: 'provider', audience: 'onecall:provider-callback:v1', storage: 'Secret Manager or certificate store K18', ttlInput: 'Q6', scope: 'per-callback signature', rotationRevocation: 'provider rotation per Q6', redaction: 'signature and nonce always removed' },
  { credentialClass: 'C07 publicShareCapabilityToken', credentialId: 'C07', domain: 'public-capability', audience: 'onecall:share-capability:v1', storage: 'DB SHA-256 digest only', ttlInput: 'share expiresAt / Q12', scope: 'path capability; created once', rotationRevocation: 'revoke or generation change; not rebuildable', redaction: 'capability token always removed and return-once' },
  { credentialClass: 'C08 extractionCode', credentialId: 'C08', domain: 'public-capability', audience: 'onecall:share-capability:v1', storage: 'per-grant salt + slow KDF digest', ttlInput: 'Q12', scope: 'EXTRACTION_CODE', rotationRevocation: 'code_generation rotation', redaction: 'code and OTP always removed and return-once' },
  { credentialClass: 'C09 uploadCredential', credentialId: 'C09', domain: 'ivr-service', audience: 'onecall:evidence-upload:v1 (issuance-side policy classification only; not a COS embedded audience claim)', storage: 'COS presign CV-4 verified or K19', ttlInput: 'uploadExpiresAt', scope: 'single-object COS presigned PUT only; fail closed and stop if CV-4 capability is unverified', rotationRevocation: 'no renewal or re-sign; expiry only', redaction: 'credential URL always removed' },
  { credentialClass: 'C10 playbackCredential', credentialId: 'C10', domain: 'public-capability', audience: 'onecall:media-playback:v1', storage: 'media gateway K20', ttlInput: 'Q22', scope: 'Authorization Bearer to media-gateway; share, admission, generation, object and PLAYBACK/SEEK/RENEW', rotationRevocation: 'issued credential is not recalled; expiry only', redaction: 'credential URL and bearer value always removed' }
];

const EXPECTED_SIGNATURE_PROFILES = [
  {
    profileId: 'ivr-request-v1',
    domain: 'ivr-service',
    operationPrefix: '/api/v1/ivr/',
    operations: 6,
    securitySchemes: ['ivrMtls', 'ivrSignature'],
    headers: { signature: 'X-ONECALL-SIGNATURE', timestamp: 'X-ONECALL-TIMESTAMP', nonce: 'X-ONECALL-NONCE' },
    headerLengths: { timestampMin: 1, timestampMax: 64, nonceMin: 1, nonceMax: 128, signatureLengthFrozen: false },
    signatureValue: 'v1.<keyId>.<base64url(HMAC-SHA-256(K17, canonicalString))>',
    signatureValuePattern: '^v1\\.[A-Za-z0-9_-]{1,64}\\.[A-Za-z0-9_-]+$',
    keyIdPattern: '^[A-Za-z0-9_-]{1,64}$',
    canonicalStringLines: ['ONECALL-IVR-V1', 'uppercase HTTP method', 'raw request path without query, not decoded and not normalized', 'X-ONECALL-TIMESTAMP', 'X-ONECALL-NONCE', 'lowercase hex SHA-256 of the exact request body bytes; an empty body uses the empty-bytes hash'],
    canonicalStringSeparator: 'LF with no trailing newline',
    algorithm: 'HMAC-SHA-256',
    timestampFormat: 'ASCII Unix epoch seconds matching ^[0-9]{1,12}$; backend receive time is authoritative',
    acceptanceWindow: 'plus or minus W; W is deferred Q25',
    nonceFormat: '^[A-Za-z0-9_-]{1,128}$',
    nonceUniqueness: '(serviceIdentity, nonce)',
    nonceStore: 'Redis SET NX',
    nonceTtl: 'W plus skew; numeric value deferred Q25',
    verificationOrder: ['mTLS client identity', 'required headers and lengths', 'timestamp parse and window', 'signature verify with active or verify-only keyId', 'nonce SET NX', 'A1 and A2 processing'],
    failureMapping: { missingOrBadMtls: '401 UNAUTHENTICATED', badSignature: '401 UNAUTHENTICATED', timestampOutOfWindow: '401 UNAUTHENTICATED', nonceReplay: '401 UNAUTHENTICATED', serviceIdentityNotAllowed: '403 FORBIDDEN', nonceOrKeyOrSecretManagerUnavailable: '503 SERVICE_UNAVAILABLE' },
    zeroSideEffects: ['no session', 'no call', 'no decision', 'no task claim', 'no reservation', 'no settlement', 'no business event', 'no redial'],
    retryTransport: 'fresh timestamp and nonce; the same callId, eventId and immutable payload',
    rotation: 'active plus verify-only overlap; retired key rejected; unknown keyId rejected without default fallback',
    numericInputsDeferred: ['Q25 acceptance window', 'Q25 nonce TTL and clock skew']
  },
  {
    profileId: 'provider-callback-v1',
    domain: 'provider',
    operation: 'POST /api/v1/callbacks/payment-events',
    securitySchemes: ['providerCallbackMtls', 'providerCallbackSignature'],
    headers: { signature: 'X-ONECALL-CALLBACK-SIGNATURE', timestamp: 'X-ONECALL-TIMESTAMP', nonce: 'X-ONECALL-NONCE' },
    headerLengths: { timestampMin: 1, timestampMax: 64, nonceMin: 1, nonceMax: 128, signatureLengthFrozen: false },
    vendorAlgorithm: 'deferred Q6/Q15',
    vendorCanonicalization: 'deferred Q6/Q15',
    vendorPayload: 'deferred Q6/Q15',
    vendorWindow: 'deferred Q6/Q15',
    verificationOrder: ['provider mTLS identity', 'headers', 'provider profile Q6', 'timestamp window', 'constant-time signature verify', 'event identity and payload fingerprint', 'nonce SET NX for a new event or changed payload', 'payment processing'],
    duplicate: 'same event and same fingerprint returns 200 accepted true duplicate true with no side effect and without requiring nonce consumption',
    conflict: 'same event with a different payload returns 409 PAYMENT_EVENT_CONFLICT after the nonce check',
    failureMapping: { signatureCertificateWindowOrNonceReplay: '401 CALLBACK_SIGNATURE_INVALID', changedPayload: '409 PAYMENT_EVENT_CONFLICT', dependencyUnavailable: '503 CALLBACK_RETRY_LATER', exactDuplicate: '200 accepted true duplicate true' },
    payloadHandling: 'the adapter encrypts the raw provider payload at rest; plaintext is never logged and internal results are never returned',
    a2Impact: 'none; provider callbacks are outside the IVR call and event state machine',
    vendorRetryPolicy: 'deferred Q6; A3 freezes fail-closed and event idempotency only'
  }
];

const EXPECTED_NONCE_POLICY = {
  transportScope: 'transport replay protection only; never part of A2 domain identity',
  uniqueness: '(serviceIdentity, nonce)',
  store: 'Redis SET NX',
  ttl: 'acceptance window W plus skew; numeric values are deferred Q25',
  ivrNonceFormat: '^[A-Za-z0-9_-]{1,128}$',
  providerNonceFormat: '^[A-Za-z0-9_-]{1,128}$',
  a1Lengths: { timestampMin: 1, timestampMax: 64, nonceMin: 1, nonceMax: 128 },
  dependencyFailure: '503 fail closed before A1 or A2 processing',
  retry: 'fresh timestamp and nonce with the same domain identity and immutable payload',
  numericInputsDeferred: ['Q25 window', 'Q25 TTL', 'Q25 clock skew']
};

const EXPECTED_AUDIENCE_REGISTRY = [
  { audience: 'onecall:customer-session:v1', ownerDomain: 'customer', credentialClasses: ['C01 customerSession'], verifier: 'core-api' },
  { audience: 'onecall:employee-session:v1', ownerDomain: 'employee', credentialClasses: ['C02 employeeSession'], verifier: 'core-api' },
  { audience: 'onecall:ivr-request:v1', ownerDomain: 'ivr-service', credentialClasses: ['C03 ivrMtls', 'C04 ivrSignature'], verifier: 'core-api', serviceIdentity: 'ivr-gateway' },
  { audience: 'onecall:share-capability:v1', ownerDomain: 'public-capability', credentialClasses: ['C07 publicShareCapabilityToken', 'C08 extractionCode'], verifier: 'core-api' },
  { audience: 'onecall:media-playback:v1', ownerDomain: 'public-capability', credentialClasses: ['C10 playbackCredential'], verifier: 'media-gateway', serviceIdentity: 'media-gateway' },
  { audience: 'onecall:provider-callback:v1', ownerDomain: 'provider', credentialClasses: ['C05 providerCallbackMtls', 'C06 providerCallbackSignature'], verifier: 'core-api' },
  { audience: 'onecall:evidence-upload:v1', ownerDomain: 'ivr-service', credentialClasses: ['C09 uploadCredential'], verifier: 'issuance-side policy classification only', serviceIdentity: 'ivr-gateway' },
  { audience: 'onecall:service:core:v1', ownerDomain: 'internal', credentialClasses: [], verifier: 'Tencent CAM service role', serviceIdentity: 'core-api' },
  { audience: 'onecall:service:worker:v1', ownerDomain: 'internal', credentialClasses: [], verifier: 'Tencent CAM service role', serviceIdentity: 'async-worker' },
  { audience: 'onecall:service:scheduler:v1', ownerDomain: 'internal', credentialClasses: [], verifier: 'Tencent CAM service role', serviceIdentity: 'scheduler' },
  { audience: 'onecall:service:media-ai:v1', ownerDomain: 'internal', credentialClasses: [], verifier: 'Tencent CAM service role', serviceIdentity: 'media-ai-worker' }
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

const EXPECTED_FIXTURES_S1 = [
  ['sec-s1-01-envelope-purpose-roundtrip', false],
  ['sec-s1-02-hmac-index-equality', false],
  ['sec-s1-03-cross-purpose-denied', true],
  ['sec-s1-04-rotation-historical-replay', false],
  ['sec-s1-05-key-unavailable-failclosed', true],
  ['sec-s1-06-append-only-rotation-safety', false]
];

const EXPECTED_FIXTURES_S2 = [
  ['sec-s2-01-five-domains-accepted', false],
  ['sec-s2-02-cross-domain-rejected', true],
  ['sec-s2-03-service-identity-least-privilege', false],
  ['sec-s2-04-ivr-signature-invalid', true],
  ['sec-s2-05-ivr-replay-rejected', true],
  ['sec-s2-06-provider-signature-replay', true],
  ['sec-s2-07-signing-key-rotation', false],
  ['sec-s2-08-failclosed-dependency-preserves-a2', false],
  ['sec-s2-09-ivr-outbox-auth-blocked', true]
];

const EXPECTED_FIXTURES = [...EXPECTED_FIXTURES_S1, ...EXPECTED_FIXTURES_S2];

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
  },
  domain: {
    name: 'security-domain-map',
    header: ['domain', 'prefix', 'operations', 'credentialClasses', 'storage', 'tokenAudience', 'crossDomainRule']
  },
  service: {
    name: 'security-service-map',
    header: ['serviceIdentity', 'mechanism', 'audience', 'allowedCalls', 'secretCustody']
  },
  credential: {
    name: 'security-credential-map',
    header: ['credentialClass', 'domain', 'audience', 'storage', 'ttlInput', 'scope', 'rotationRevocation', 'redaction']
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
  if (!STAGE_CONFIG[result.stage]) {
    throw new Error(`stage ${result.stage} is not implemented by the S2 validator`);
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

function createMachine(model, stage = 's1') {
  const keyById = new Map(model.keyClasses.map((row) => [row.keyId, row]));
  const keyClassById = keyById;
  const dataById = new Map(model.dataClasses.map((row) => [row.dataClass, row]));
  const domainByPrefix = [...(model.authDomains || [])].sort((left, right) => right.prefix.length - left.prefix.length);
  const credentialById = new Map((model.credentialClasses || []).map((row) => [row.credentialId, row]));
  const serviceById = new Map((model.serviceIdentities || []).map((row) => [row.serviceIdentity, row]));
  const state = {
    materials: new Map(),
    envelopesByStep: new Map(),
    envelopesByDataClass: new Map(),
    hmacs: new Map(),
    plaintextHashes: new Map(),
    auditEvents: [],
    nonceSeen: new Set(),
    a2Events: new Map(),
    providerEvents: new Map(),
    providerNonces: new Set(),
    outbox: new Map(),
    outboxIdentityChanges: 0,
    outboxPayloadChanges: 0,
    payments: 0,
    entitlements: 0,
    a2Calls: 0,
    a2Reservations: 0,
    a2Settlements: 0,
    a2Redials: 0,
    a2ReResolves: 0
  };

  function domainForPath(requestPath) {
    for (const row of domainByPrefix) {
      if (typeof requestPath === 'string' && requestPath.startsWith(row.prefix)) {
        return row;
      }
    }
    return null;
  }

  function sideEffectsZero(extra = {}) {
    return {
      sessions: 0,
      calls: 0,
      decisions: 0,
      tasks: 0,
      reservations: 0,
      settlements: 0,
      events: 0,
      redials: 0,
      reResolves: 0,
      ...extra
    };
  }

  function headerShape(profile) {
    return {
      headerNames: {
        signature: profile?.headers?.signature || null,
        timestamp: profile?.headers?.timestamp || null,
        nonce: profile?.headers?.nonce || null
      },
      timestampMinLength: profile?.headerLengths?.timestampMin ?? 1,
      timestampMaxLength: profile?.headerLengths?.timestampMax ?? 64,
      nonceMinLength: profile?.headerLengths?.nonceMin ?? 1,
      nonceMaxLength: profile?.headerLengths?.nonceMax ?? 128,
      signatureLengthConstrained: false,
      signatureValueFormat: profile?.signatureValue || 'provider vendor profile Q6'
    };
  }

  function canonicalIvrString(step) {
    const bodyHash = crypto.createHash('sha256').update(step.body || '', 'utf8').digest('hex');
    return [
      'ONECALL-IVR-V1',
      String(step.method || '').toUpperCase(),
      step.path || '',
      String(step.timestamp ?? ''),
      String(step.nonce ?? ''),
      bodyHash
    ].join('\n');
  }

  function signatureFor(step, material) {
    const canonical = canonicalIvrString(step);
    const signature = crypto.createHmac('sha256', materialSecret(material)).update(canonical, 'utf8').digest('base64url');
    return { canonical, signature: `v1.${material.keyRef}.${signature}` };
  }

  function materialSecret(material) {
    return crypto.createHash('sha256').update(material.keyLabel, 'utf8').digest();
  }

  function timingSafeEqualStrings(left, right) {
    const leftBuffer = Buffer.from(String(left), 'utf8');
    const rightBuffer = Buffer.from(String(right), 'utf8');
    if (leftBuffer.length !== rightBuffer.length) {
      return false;
    }
    return crypto.timingSafeEqual(leftBuffer, rightBuffer);
  }

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

  function isValidDomainIdentity(identity) {
    if (!identity || typeof identity !== 'object' || Array.isArray(identity)) {
      return false;
    }
    const values = Object.values(identity);
    return values.length > 0 && values.every((value) => typeof value === 'string' && value.length > 0);
  }

  function isValidIvrTimestamp(value) {
    return typeof value === 'string' && /^[0-9]{1,12}$/.test(value) && value.length <= 64;
  }

  function isValidIvrNonce(value) {
    return typeof value === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(value);
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
      case 'domain-request': {
        const domainRow = domainForPath(step.path);
        const credential = credentialById.get(step.credentialId);
        if (!domainRow) {
          return {
            status: 'DENIED', statusCode: 404, problemCode: 'NOT_FOUND', reason: 'UNKNOWN_DOMAIN_PATH',
            accepted: false, crossDomainRejected: false, sideEffects: sideEffectsZero()
          };
        }
        const optionalPublic = domainRow.domain === 'public-capability'
          && credential?.credentialId === 'C01'
          && step.organizationMember === true;
        const crossDomain = !credential || (credential.domain !== domainRow.domain && !optionalPublic);
        let statusCode = 200;
        let problemCode = null;
        let reason = 'ACCEPTED';
        if (crossDomain) {
          statusCode = 401;
          problemCode = domainRow.domain === 'provider' ? 'CALLBACK_SIGNATURE_INVALID' : 'UNAUTHENTICATED';
          reason = 'CROSS_DOMAIN_TOKEN';
        } else if (step.credentialStatus && step.credentialStatus !== 'ACTIVE') {
          statusCode = 401;
          problemCode = 'UNAUTHENTICATED';
          reason = 'CREDENTIAL_EXPIRED_OR_REVOKED';
        } else {
          const domainAudiences = String(domainRow.tokenAudience || '').split(';').map((entry) => entry.trim());
          const audienceOk = step.presentedAudience === credential.audience
            && (optionalPublic || domainAudiences.includes(step.presentedAudience));
          if (!audienceOk) {
            statusCode = 401;
            problemCode = domainRow.domain === 'provider' ? 'CALLBACK_SIGNATURE_INVALID' : 'UNAUTHENTICATED';
            reason = 'AUDIENCE_MISMATCH';
          } else if ((domainRow.domain === 'ivr-service' || domainRow.domain === 'provider')
            && (step.mtls !== true || step.signatureValid !== true)) {
            statusCode = 401;
            problemCode = domainRow.domain === 'provider' ? 'CALLBACK_SIGNATURE_INVALID' : 'UNAUTHENTICATED';
            reason = 'REQUIRED_MECHANISM_MISSING';
          } else if (domainRow.domain === 'public-capability' && step.capabilityKnown === false) {
            statusCode = 404;
            problemCode = 'SHARE_TOKEN_INVALID';
            reason = 'CAPABILITY_UNKNOWN';
          } else if (step.roleAllowed === false) {
            statusCode = 403;
            problemCode = 'FORBIDDEN';
            reason = 'ROLE_OR_ACCESS_DENIED';
          }
        }
        return {
          status: statusCode === 200 ? 'OK' : 'DENIED',
          statusCode,
          problemCode,
          reason,
          accepted: statusCode === 200,
          domain: domainRow.domain,
          requiredAudience: domainRow.tokenAudience,
          credentialDomain: credential?.domain || null,
          crossDomainRejected: crossDomain,
          domainOperations: domainRow.operations,
          sideEffects: sideEffectsZero()
        };
      }
      case 'service-call': {
        const identity = serviceById.get(step.serviceIdentity);
        const wildcard = typeof step.call === 'string' && step.call.includes('*');
        if (step.frontend === true || !identity) {
          return {
            status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN',
            reason: step.frontend === true ? 'FRONTEND_SERVICE_SECRET_FORBIDDEN' : 'UNKNOWN_SERVICE_IDENTITY',
            callAllowed: false, wildcardAllowed: false, frontendSecretGranted: false,
            frontendSecretRejected: step.holdsSecret === true, auditReadDenied: true,
            sideEffects: sideEffectsZero()
          };
        }
        if (wildcard) {
          return {
            status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'WILDCARD_FORBIDDEN',
            callAllowed: false, wildcardAllowed: false, frontendSecretGranted: false,
            frontendSecretRejected: false, auditReadDenied: false, sideEffects: sideEffectsZero()
          };
        }
        if (step.call === 'AUDIT_READ') {
          return {
            status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'AUDIT_READ_FORBIDDEN',
            callAllowed: false, wildcardAllowed: false, frontendSecretGranted: false,
            frontendSecretRejected: false, auditReadDenied: true, sideEffects: sideEffectsZero()
          };
        }
        const allowed = (identity.allowedCalls || []).includes(step.call);
        return {
          status: allowed ? 'OK' : 'DENIED',
          statusCode: allowed ? 200 : 403,
          problemCode: allowed ? null : 'FORBIDDEN',
          reason: allowed ? 'CALL_ALLOWED' : 'CALL_NOT_ALLOWED',
          callAllowed: allowed,
          wildcardAllowed: false,
          frontendSecretGranted: false,
          frontendSecretRejected: false,
          auditReadDenied: false,
          auditReadAllowed: false,
          sideEffects: sideEffectsZero()
        };
      }
      case 'ivr-request': {
        const profile = (model.signatureProfiles || []).find((row) => row.profileId === 'ivr-request-v1');
        const shape = headerShape(profile);
        const base = {
          ...shape,
          verificationOrder: profile?.verificationOrder || [],
          a2Processed: false,
          a2FirstResultReused: false,
          signatureVerified: false,
          timestampAccepted: false,
          nonceAccepted: false,
          nonceConsumed: false,
          a2EventId: step.a2?.eventId || null,
          sideEffects: sideEffectsZero()
        };
        const mtls = step.mtls || {};
        if (mtls.present !== true) {
          return { ...base, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'MTLS_MISSING' };
        }
        if (mtls.serviceIdentity !== 'ivr-gateway') {
          return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'SERVICE_IDENTITY_NOT_ALLOWED' };
        }
        if (mtls.certificateState && !['ACTIVE', 'VERIFY_ONLY'].includes(mtls.certificateState)) {
          return { ...base, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'MTLS_INVALID' };
        }
        const signingMaterial = materialFor(step.keyRef) || null;
        const headers = {};
        if (signingMaterial) {
          const built = signatureFor(step, signingMaterial);
          headers['X-ONECALL-SIGNATURE'] = built.signature;
        }
        headers['X-ONECALL-TIMESTAMP'] = String(step.timestamp ?? '');
        headers['X-ONECALL-NONCE'] = String(step.nonce ?? '');
        if (step.signatureMode === 'missing') {
          delete headers['X-ONECALL-SIGNATURE'];
        } else if (step.signatureMode === 'invalid' && headers['X-ONECALL-SIGNATURE']) {
          const validSignature = headers['X-ONECALL-SIGNATURE'];
          const replacement = validSignature.endsWith('A') ? 'B' : 'A';
          headers['X-ONECALL-SIGNATURE'] = `${validSignature.slice(0, -1)}${replacement}`;
        }
        for (const [header, value] of Object.entries(step.headersOverride || {})) {
          if (value === null) {
            delete headers[header];
          } else {
            headers[header] = value;
          }
        }
        const signatureValue = headers['X-ONECALL-SIGNATURE'];
        const timestampValue = headers['X-ONECALL-TIMESTAMP'];
        const nonceValue = headers['X-ONECALL-NONCE'];
        const signatureFormatValid = typeof signatureValue === 'string' && /^v1\.[A-Za-z0-9_-]{1,64}\.[A-Za-z0-9_-]+$/.test(signatureValue);
        const timestampFormatValid = typeof timestampValue === 'string' && /^[0-9]{1,12}$/.test(timestampValue) && timestampValue.length >= 1 && timestampValue.length <= 64;
        const nonceFormatValid = typeof nonceValue === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(nonceValue);
        const headerResult = {
          ...base,
          headerValuesPresent: {
            signature: typeof signatureValue === 'string' && signatureValue.length > 0,
            timestamp: typeof timestampValue === 'string' && timestampValue.length > 0,
            nonce: typeof nonceValue === 'string' && nonceValue.length > 0
          },
          signatureFormatValid,
          timestampFormatValid,
          nonceFormatValid
        };
        if (!signatureFormatValid || !timestampFormatValid || !nonceFormatValid) {
          return { ...headerResult, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'HEADER_INVALID' };
        }
        const timestampNumber = Number(timestampValue);
        const receivedAt = Number(step.receivedAt);
        const windowSeconds = Number(step.windowSeconds);
        if (!Number.isFinite(timestampNumber) || !Number.isFinite(receivedAt) || !Number.isFinite(windowSeconds)) {
          return { ...headerResult, status: 'FAILED', reason: 'FIXTURE_INPUT_INVALID' };
        }
        if (Math.abs(receivedAt - timestampNumber) > windowSeconds) {
          return { ...headerResult, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'TIMESTAMP_OUT_OF_WINDOW' };
        }
        if (step.secretManagerAvailable === false) {
          return { ...headerResult, status: 'DENIED', statusCode: 503, problemCode: 'SERVICE_UNAVAILABLE', reason: 'SECRET_MANAGER_UNAVAILABLE' };
        }
        const headerKeyId = signatureValue.match(/^v1\.([A-Za-z0-9_-]{1,64})\./)?.[1] || null;
        const material = headerKeyId
          ? [...state.materials.values()].find((entry) => entry.keyRef === headerKeyId) || null
          : null;
        if (!material) {
          return { ...headerResult, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'SIGNATURE_KEY_UNKNOWN' };
        }
        if (!stateAllows(material.state, 'verify')) {
          return { ...headerResult, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'SIGNATURE_KEY_DISABLED', keyState: material.state };
        }
        const expectedSignature = signatureFor(step, material).signature;
        if (!timingSafeEqualStrings(signatureValue, expectedSignature)) {
          return { ...headerResult, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'SIGNATURE_INVALID' };
        }
        if (step.nonceStore !== 'AVAILABLE') {
          return { ...headerResult, signatureVerified: true, status: 'DENIED', statusCode: 503, problemCode: 'SERVICE_UNAVAILABLE', reason: 'NONCE_STORE_UNAVAILABLE' };
        }
        const nonceKey = `${mtls.serviceIdentity}:${nonceValue}`;
        if (state.nonceSeen.has(nonceKey)) {
          return { ...headerResult, signatureVerified: true, status: 'DENIED', statusCode: 401, problemCode: 'UNAUTHENTICATED', reason: 'NONCE_REPLAY' };
        }
        state.nonceSeen.add(nonceKey);
        let eventsCreated = 0;
        let firstResultReused = false;
        if (step.a2 && step.a2.eventId) {
          const existing = state.a2Events.get(step.a2.eventId);
          if (existing && existing.fingerprint !== step.a2.payloadFingerprint) {
            return { ...headerResult, signatureVerified: true, timestampAccepted: true, nonceAccepted: true, nonceConsumed: true, status: 'DENIED', statusCode: 409, problemCode: 'EVENT_ID_CONFLICT', reason: 'A2_IDENTITY_CONFLICT' };
          }
          if (existing) {
            firstResultReused = true;
          } else {
            state.a2Events.set(step.a2.eventId, { fingerprint: step.a2.payloadFingerprint });
            eventsCreated = 1;
          }
        }
        return {
          ...headerResult,
          status: 'OK',
          statusCode: 200,
          problemCode: null,
          reason: 'ACCEPTED',
          signatureVerified: true,
          timestampAccepted: true,
          nonceAccepted: true,
          nonceConsumed: true,
          keyState: material.state,
          canonicalString: canonicalIvrString(step),
          a2Processed: Boolean(step.a2 && step.a2.eventId),
          a2FirstResultReused: firstResultReused,
          sideEffects: sideEffectsZero({ events: eventsCreated })
        };
      }
      case 'provider-callback': {
        const profile = (model.signatureProfiles || []).find((row) => row.profileId === 'provider-callback-v1');
        const shape = headerShape(profile);
        const base = {
          ...shape,
          verificationOrder: profile?.verificationOrder || [],
          signatureVerified: false,
          nonceConsumed: false,
          duplicate: false,
          accepted: false,
          paymentsCreated: 0,
          entitlementsCreated: 0,
          payments: state.payments,
          entitlements: state.entitlements,
          providerEvents: state.providerEvents.size,
          sideEffects: sideEffectsZero()
        };
        const mtls = step.mtls || {};
        if (mtls.present !== true || mtls.serviceIdentity !== 'provider' || (mtls.certificateState && !['ACTIVE', 'VERIFY_ONLY'].includes(mtls.certificateState))) {
          return { ...base, status: 'DENIED', statusCode: 401, problemCode: 'CALLBACK_SIGNATURE_INVALID', reason: 'MTLS_INVALID' };
        }
        const signatureValue = step.signature ?? (step.signatureMode === 'missing' ? null : 'TEST_ONLY_VENDOR_SIGNATURE');
        const timestampValue = step.timestamp === undefined ? null : String(step.timestamp);
        const nonceValue = step.nonce === undefined ? null : String(step.nonce);
        const signatureOk = typeof signatureValue === 'string' && signatureValue.length > 0;
        const timestampOk = typeof timestampValue === 'string' && /^[0-9]{1,12}$/.test(timestampValue) && timestampValue.length <= 64;
        const nonceOk = typeof nonceValue === 'string' && /^[A-Za-z0-9_-]{1,128}$/.test(nonceValue);
        if (!signatureOk || !timestampOk || !nonceOk) {
          return { ...base, status: 'DENIED', statusCode: 401, problemCode: 'CALLBACK_SIGNATURE_INVALID', reason: 'HEADER_INVALID' };
        }
        if (step.profileAvailable === false) {
          return { ...base, status: 'DENIED', statusCode: 503, problemCode: 'CALLBACK_RETRY_LATER', reason: 'PROVIDER_PROFILE_UNAVAILABLE' };
        }
        const timestampNumber = Number(timestampValue);
        const receivedAt = Number(step.receivedAt);
        const windowSeconds = Number(step.windowSeconds);
        if (!Number.isFinite(timestampNumber) || !Number.isFinite(receivedAt) || !Number.isFinite(windowSeconds)) {
          return { ...base, status: 'FAILED', reason: 'FIXTURE_INPUT_INVALID' };
        }
        if (Math.abs(receivedAt - timestampNumber) > windowSeconds) {
          return { ...base, status: 'DENIED', statusCode: 401, problemCode: 'CALLBACK_SIGNATURE_INVALID', reason: 'TIMESTAMP_OUT_OF_WINDOW' };
        }
        if (step.keyRef) {
          const verifyMaterial = materialFor(step.keyRef);
          if (!verifyMaterial || !stateAllows(verifyMaterial.state, 'verify')) {
            return { ...base, status: 'DENIED', statusCode: 401, problemCode: 'CALLBACK_SIGNATURE_INVALID', reason: 'SIGNATURE_KEY_DISABLED' };
          }
        }
        if (step.signatureMode === 'invalid') {
          return { ...base, status: 'DENIED', statusCode: 401, problemCode: 'CALLBACK_SIGNATURE_INVALID', reason: 'SIGNATURE_INVALID' };
        }
        const existing = state.providerEvents.get(step.eventId);
        if (existing === step.payloadFingerprint) {
          return {
            ...base,
            status: 'OK', statusCode: 200, problemCode: null, reason: 'EXACT_DUPLICATE',
            signatureVerified: true, duplicate: true, accepted: true, nonceConsumed: false,
            payments: state.payments, entitlements: state.entitlements
          };
        }
        if (step.nonceStore !== 'AVAILABLE') {
          return { ...base, signatureVerified: true, status: 'DENIED', statusCode: 503, problemCode: 'CALLBACK_RETRY_LATER', reason: 'NONCE_STORE_UNAVAILABLE' };
        }
        if (state.providerNonces.has(nonceValue)) {
          return { ...base, signatureVerified: true, status: 'DENIED', statusCode: 401, problemCode: 'CALLBACK_SIGNATURE_INVALID', reason: 'NONCE_REPLAY' };
        }
        state.providerNonces.add(nonceValue);
        if (existing !== undefined && existing !== step.payloadFingerprint) {
          return {
            ...base, status: 'DENIED', statusCode: 409, problemCode: 'PAYMENT_EVENT_CONFLICT',
            reason: 'CHANGED_PAYLOAD', signatureVerified: true, nonceConsumed: true,
            payments: state.payments, entitlements: state.entitlements
          };
        }
        state.providerEvents.set(step.eventId, step.payloadFingerprint);
        state.payments += 1;
        state.entitlements += 1;
        return {
          ...base,
          status: 'OK', statusCode: 200, problemCode: null, reason: 'ACCEPTED',
          signatureVerified: true, nonceConsumed: true, duplicate: false, accepted: true,
          paymentsCreated: 1, entitlementsCreated: 1,
          payments: state.payments, entitlements: state.entitlements,
          providerEvents: state.providerEvents.size
        };
      }
      case 'ivr-outbox': {
        const eventId = step.eventId;
        let entry = state.outbox.get(eventId);
        if (!entry && step.create !== false && !isValidDomainIdentity(step.domainIdentity)) {
          return {
            status: 'DENIED',
            statusCode: 403,
            problemCode: 'FORBIDDEN',
            reason: 'IDENTITY_MUTATION_FORBIDDEN',
            eventId,
            outboxState: 'PENDING',
            securityBlocked: false,
            identityChanges: 0,
            payloadChanges: 0,
            a2BusinessSideEffects: 0,
            sideEffects: sideEffectsZero()
          };
        }
        if (!entry && step.create !== false) {
          const initialObservation = step.observedStatus === 401 || step.observedStatus === 403;
          if (initialObservation
            && (!isValidIvrTimestamp(step.transportTimestamp) || !isValidIvrNonce(step.transportNonce))) {
            return {
              status: 'DENIED',
              statusCode: 403,
              problemCode: 'FORBIDDEN',
              reason: 'STALE_TRANSPORT',
              eventId,
              outboxState: 'PENDING',
              securityBlocked: false,
              identityChanges: 0,
              payloadChanges: 0,
              a2BusinessSideEffects: 0,
              sideEffects: sideEffectsZero()
            };
          }
          entry = {
            eventId,
            payloadFingerprint: step.payloadFingerprint,
            state: 'PENDING',
            securityBlocked: false,
            identity: step.domainIdentity,
            previousNonce: null,
            previousTimestamp: null
          };
          state.outbox.set(eventId, entry);
        }
        if (!entry) {
          return { status: 'FAILED', reason: 'OUTBOX_ENTRY_MISSING' };
        }
        const base = {
          eventId,
          outboxState: entry.state,
          sameEventId: entry.eventId === eventId,
          samePayload: entry.payloadFingerprint === step.payloadFingerprint,
          securityBlocked: entry.securityBlocked,
          hotRetryPerformed: false,
          retried: false,
          retryAllowed: false,
          freshTransport: false,
          alertSignal: null,
          alertRedacted: true,
          credentialExposed: false,
          signatureLogged: false,
          nonceLogged: false,
          identityChanges: 0,
          payloadChanges: 0,
          a2BusinessSideEffects: 0,
          sideEffects: sideEffectsZero()
        };
        if (!base.samePayload || step.mutatePayload === true) {
          return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'PAYLOAD_MUTATION_FORBIDDEN' };
        }
        if (step.mutateEventId === true) {
          return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'IDENTITY_MUTATION_FORBIDDEN' };
        }
        const identitySupplied = step.domainIdentity !== undefined && step.domainIdentity !== null;
        if (identitySupplied && (!isValidDomainIdentity(step.domainIdentity) || !deepEqual(entry.identity, step.domainIdentity))) {
          return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'IDENTITY_MUTATION_FORBIDDEN' };
        }
        const attempts = [
          ['hotRetryRequested', 'HOT_RETRY_FORBIDDEN'],
          ['redial', 'REDIAL_FORBIDDEN'],
          ['reResolve', 'RERESOLVE_FORBIDDEN'],
          ['reservation', 'RESERVATION_FORBIDDEN'],
          ['settlement', 'SETTLEMENT_FORBIDDEN']
        ];
        for (const [flag, reason] of attempts) {
          if (step[flag] === true) {
            return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason };
          }
        }
        if (step.observedStatus === 401 || step.observedStatus === 403) {
          entry.securityBlocked = true;
          if (!identitySupplied || !isValidDomainIdentity(step.domainIdentity) || !deepEqual(entry.identity, step.domainIdentity)) {
            return {
              ...base,
              status: 'DENIED',
              statusCode: 403,
              problemCode: 'FORBIDDEN',
              reason: 'IDENTITY_MUTATION_FORBIDDEN',
              outboxState: entry.state,
              securityBlocked: true,
              alertSignal: 'IVR_OUTBOX_SECURITY_BLOCKED'
            };
          }
          const transportSupplied = step.transportNonce !== undefined || step.transportTimestamp !== undefined;
          if (transportSupplied) {
            if (!isValidIvrTimestamp(step.transportTimestamp) || !isValidIvrNonce(step.transportNonce)) {
              return {
                ...base,
                status: 'DENIED',
                statusCode: 403,
                problemCode: 'FORBIDDEN',
                reason: 'STALE_TRANSPORT',
                outboxState: entry.state,
                securityBlocked: true,
                alertSignal: 'IVR_OUTBOX_SECURITY_BLOCKED'
              };
            }
            entry.previousNonce = step.transportNonce;
            entry.previousTimestamp = step.transportTimestamp;
          } else if (!isValidIvrTimestamp(entry.previousTimestamp) || !isValidIvrNonce(entry.previousNonce)) {
            return {
              ...base,
              status: 'DENIED',
              statusCode: 403,
              problemCode: 'FORBIDDEN',
              reason: 'STALE_TRANSPORT',
              outboxState: entry.state,
              securityBlocked: true,
              alertSignal: 'IVR_OUTBOX_SECURITY_BLOCKED'
            };
          }
          return {
            ...base,
            status: 'DENIED',
            statusCode: step.observedStatus,
            problemCode: step.observedStatus === 401 ? 'UNAUTHENTICATED' : 'FORBIDDEN',
            reason: step.observedStatus === 401 ? 'OBSERVED_UNAUTHENTICATED' : 'OBSERVED_FORBIDDEN',
            outboxState: entry.state,
            securityBlocked: true,
            alertSignal: 'IVR_OUTBOX_SECURITY_BLOCKED'
          };
        }
        const recoverySignals = ['CREDENTIAL_ROTATED', 'KEY_ROTATED', 'CERTIFICATE_ROTATED', 'SERVICE_AUTHORIZATION_RESTORED'];
        if (step.recoverySignal && entry.securityBlocked) {
          if (!recoverySignals.includes(step.recoverySignal)) {
            return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'RECOVERY_SIGNAL_INVALID' };
          }
          if (!identitySupplied || !isValidDomainIdentity(step.domainIdentity) || !deepEqual(entry.identity, step.domainIdentity)) {
            return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'IDENTITY_MUTATION_FORBIDDEN' };
          }
          if (!isValidIvrTimestamp(step.transportTimestamp) || !isValidIvrNonce(step.transportNonce)) {
            return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'STALE_TRANSPORT' };
          }
          const freshTransport = step.transportNonce !== entry.previousNonce
            && step.transportTimestamp !== entry.previousTimestamp;
          if (!freshTransport) {
            return { ...base, status: 'DENIED', statusCode: 403, problemCode: 'FORBIDDEN', reason: 'STALE_TRANSPORT' };
          }
          entry.securityBlocked = false;
          entry.previousNonce = step.transportNonce;
          entry.previousTimestamp = step.transportTimestamp;
          return {
            ...base,
            status: 'OK',
            statusCode: 200,
            problemCode: null,
            reason: 'RETRY_AFTER_RECOVERY',
            outboxState: entry.state,
            securityBlocked: false,
            retryAllowed: true,
            retried: true,
            freshTransport: true,
            alertSignal: null
          };
        }
        return { ...base, status: 'OK', statusCode: 200, problemCode: null, reason: 'OUTBOX_UNCHANGED', outboxState: entry.state, securityBlocked: entry.securityBlocked };
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

  function projectedModel() {
    const projected = {};
    for (const key of S1_MODEL_SECTIONS) {
      if (key in model) {
        projected[key] = model[key];
      }
    }
    projected.invariants = (model.invariants || []).slice(0, 6);
    if (stage === 's2') {
      for (const key of S2_MODEL_SECTIONS) {
        if (key in model) {
          projected[key] = model[key];
        }
      }
      projected.invariants = model.invariants || [];
    }
    return projected;
  }

  function machineSummary() {
    const projected = projectedModel();
    const projectedStrings = collectStrings(projected, []);
    const futurePattern = stage === 's2'
      ? /INV-A3-(1[2-9]|2[01])\b/
      : /INV-A3-(0[7-9]|1[0-9]|2[01])\b/;
    const futureInvariant = projectedStrings.filter((text) => futurePattern.test(text)).length;
    const futureFixtures = projectedStrings.filter((text) => /sec-s3-/.test(text)).length;
    let forbiddenSections = 0;
    walkKeys(projected, (key) => {
      if (S3_MODEL_SECTIONS.includes(key)) {
        forbiddenSections += 1;
      }
    });
    return {
      keyClasses: projected.keyClasses.length,
      dataClasses: projected.dataClasses.length,
      deferredInputs: projected.deferredInputs.length,
      deferredQ: projected.deferredInputs.filter((row) => /^Q[0-9]+$/.test(row.inputId)).length,
      deferredCv: projected.deferredInputs.filter((row) => /^CV-[0-9]+$/.test(row.inputId)).length,
      invariants: projected.invariants.length,
      futureInvariantIds: futureInvariant,
      futureFixtureIds: futureFixtures,
      s2s3Sections: forbiddenSections,
      trustSurfaces: (projected.trustSurfaces || []).length,
      authDomains: (projected.authDomains || []).length,
      trustBoundaries: (projected.trustBoundaries || []).length,
      serviceIdentities: (projected.serviceIdentities || []).length,
      credentialClasses: (projected.credentialClasses || []).length,
      signatureProfiles: (projected.signatureProfiles || []).length,
      a2Messages: model.upstream?.a2?.messages || 0,
      a2EventTypes: model.upstream?.a2?.eventTypes || 0,
      a2Identities: model.upstream?.a2?.identities || 0,
      a2Invariants: model.upstream?.a2?.invariants || 0,
      a2EventRegistrySha256: model.upstream?.a2?.eventRegistrySha256 || null,
      a2Events: state.a2Events.size,
      a2Calls: state.a2Calls,
      a2Reservations: state.a2Reservations,
      a2Settlements: state.a2Settlements,
      a2Redials: state.a2Redials,
      a2ReResolves: state.a2ReResolves,
      outboxEntries: state.outbox.size,
      securityBlockedEntries: [...state.outbox.values()].filter((entry) => entry.securityBlocked).length,
      outboxIdentityChanges: state.outboxIdentityChanges,
      outboxPayloadChanges: state.outboxPayloadChanges,
      payments: state.payments,
      entitlements: state.entitlements,
      providerEvents: state.providerEvents.size,
      nonceSeen: state.nonceSeen.size
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

function validateModel(model, stage, counters, issues) {
  const config = STAGE_CONFIG[stage];
  const actualKeys = Object.keys(model);
  for (const key of config.required) {
    if (!actualKeys.includes(key)) {
      counters.stage_field_missing += 1;
      issues.push(`model: missing required ${stage} section ${key}`);
    }
  }
  const allowedTopLevel = new Set([...S1_MODEL_SECTIONS, ...S2_MODEL_SECTIONS]);
  for (const key of actualKeys) {
    if (!allowedTopLevel.has(key)) {
      counters.stage_field_premature += 1;
      issues.push(`model: premature section ${key}`);
    }
  }
  if (config.s2Sections === 'optional') {
    const presentS2 = S2_MODEL_SECTIONS.filter((key) => actualKeys.includes(key));
    if (presentS2.length !== 0 && presentS2.length !== S2_MODEL_SECTIONS.length) {
      counters.stage_field_missing += 1;
      issues.push('model: partially present S2 section set');
    }
  }
  walkKeys(model, (key, trail) => {
    if (S3_MODEL_SECTIONS.includes(key)) {
      counters.stage_field_premature += 1;
      issues.push(`model: premature S3 field ${trail}`);
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
  if (!config.invariantCounts.includes(invariants.length)) {
    counters.stage_field_missing += 1;
    issues.push(`model.invariants count is ${invariants.length}, expected ${config.invariantCounts.join(' or ')}`);
  }
  const invariantPairs = invariants.map((row) => [row.id, row.statement]);
  let expectedInvariants = EXPECTED_INVARIANTS;
  if (invariants.length === 11) {
    expectedInvariants = EXPECTED_INVARIANTS_CUMULATIVE;
  } else if (stage === 's2' || invariants.length !== 6) {
    expectedInvariants = EXPECTED_INVARIANTS_CUMULATIVE;
  }
  if (!deepEqual(invariantPairs, expectedInvariants)) {
    counters.stage_field_missing += 1;
    issues.push(`model.invariants do not exactly match the ${stage} invariant registry`);
  }

  const modelStrings = [];
  collectStrings(model, modelStrings);
  for (const text of modelStrings) {
    if (config.futureInvariantPattern.test(text)) {
      counters.stage_field_premature += 1;
      issues.push('model contains a future invariant ID');
    }
    if (/sec-s3-/.test(text)) {
      counters.stage_field_premature += 1;
      issues.push('model contains a future fixture ID');
    }
    if (/https?:\/\//.test(text)) {
      counters.stage_field_premature += 1;
      issues.push('model contains a URL');
    }
  }

  if (config.s2Sections === 'required' || S2_MODEL_SECTIONS.every((key) => actualKeys.includes(key))) {
    validateS2Model(model, counters, issues);
  }

  return { keyClasses, dataClasses, deferred, invariants, qRows, cvRows, keyIds };
}

function validateS2Model(model, counters, issues) {
  const checks = [
    ['trustSurfaces', EXPECTED_TRUST_SURFACES],
    ['authDomains', EXPECTED_AUTH_DOMAINS],
    ['trustBoundaries', EXPECTED_TRUST_BOUNDARIES],
    ['serviceIdentities', EXPECTED_SERVICE_IDENTITIES],
    ['credentialClasses', EXPECTED_CREDENTIAL_CLASSES],
    ['signatureProfiles', EXPECTED_SIGNATURE_PROFILES],
    ['audienceRegistry', EXPECTED_AUDIENCE_REGISTRY]
  ];
  for (const [section, expected] of checks) {
    const actual = model[section];
    if (!Array.isArray(actual) || !deepEqual(actual, expected)) {
      counters.stage_field_missing += 1;
      issues.push(`model.${section} does not exactly match the frozen S2 registry`);
    }
    if (Array.isArray(actual) && actual.length !== expected.length) {
      counters.stage_field_missing += 1;
      issues.push(`model.${section} count is ${actual.length}, expected ${expected.length}`);
    }
  }
  if (!deepEqual(model.noncePolicy, EXPECTED_NONCE_POLICY)) {
    counters.stage_field_missing += 1;
    issues.push('model.noncePolicy does not exactly match the frozen S2 nonce policy');
  }

  const surfaces = Array.isArray(model.trustSurfaces) ? model.trustSurfaces : [];
  if (surfaces.length !== 5) {
    counters.stage_field_missing += 1;
    issues.push(`model.trustSurfaces count is ${surfaces.length}, expected 5`);
  }
  const domains = Array.isArray(model.authDomains) ? model.authDomains : [];
  if (domains.length !== 5) {
    counters.stage_field_missing += 1;
    issues.push(`model.authDomains count is ${domains.length}, expected 5`);
  }
  const boundaries = Array.isArray(model.trustBoundaries) ? model.trustBoundaries : [];
  if (boundaries.length !== 7) {
    counters.stage_field_missing += 1;
    issues.push(`model.trustBoundaries count is ${boundaries.length}, expected 7`);
  }
  const services = Array.isArray(model.serviceIdentities) ? model.serviceIdentities : [];
  if (services.length !== 6) {
    counters.stage_field_missing += 1;
    issues.push(`model.serviceIdentities count is ${services.length}, expected 6`);
  }
  const credentials = Array.isArray(model.credentialClasses) ? model.credentialClasses : [];
  if (credentials.length !== 10) {
    counters.stage_field_missing += 1;
    issues.push(`model.credentialClasses count is ${credentials.length}, expected 10`);
  }
  const profiles = Array.isArray(model.signatureProfiles) ? model.signatureProfiles : [];
  if (profiles.length !== 2) {
    counters.stage_field_missing += 1;
    issues.push(`model.signatureProfiles count is ${profiles.length}, expected 2`);
  }

  const seenDomains = new Set();
  const domainOperations = new Set();
  for (const row of domains) {
    if (seenDomains.has(row.domain)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate auth domain ${row.domain}`);
    }
    seenDomains.add(row.domain);
    domainOperations.add(String(row.operations));
    if (!(row.operations > 0)) {
      counters.stage_field_missing += 1;
      issues.push(`auth domain ${row.domain} has an invalid operation count`);
    }
  }
  if (!deepEqual([...domainOperations].sort(), ['1', '18', '21', '3', '6'])) {
    counters.stage_field_missing += 1;
    issues.push('auth domain operation counts drift from 18/21/6/3/1');
  }

  const seenServices = new Set();
  for (const row of services) {
    if (seenServices.has(row.serviceIdentity)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate service identity ${row.serviceIdentity}`);
    }
    seenServices.add(row.serviceIdentity);
    if (!Array.isArray(row.allowedCalls) || row.allowedCalls.length === 0) {
      counters.stage_field_missing += 1;
      issues.push(`service identity ${row.serviceIdentity} has no allowed calls`);
    }
    for (const call of row.allowedCalls || []) {
      if (typeof call !== 'string' || call.includes('*')) {
        counters.stage_field_missing += 1;
        issues.push(`service identity ${row.serviceIdentity} has a wildcard or invalid allowed call`);
      }
    }
    for (const field of ['mechanism', 'audience', 'secretCustody']) {
      if (typeof row[field] !== 'string' || row[field].trim() === '' || row[field].includes('*')) {
        counters.stage_field_missing += 1;
        issues.push(`service identity ${row.serviceIdentity} has an invalid ${field}`);
      }
    }
    if (row.failClosedStatus !== 503) {
      counters.stage_field_missing += 1;
      issues.push(`service identity ${row.serviceIdentity} failClosedStatus must be 503`);
    }
  }
  for (const frontend of ['customer-web', 'public-share-web', 'operations-web', 'miniapp']) {
    if (seenServices.has(frontend)) {
      counters.stage_field_missing += 1;
      issues.push(`frontend ${frontend} must not hold a service identity`);
    }
  }

  const seenCredentials = new Set();
  for (const row of credentials) {
    if (seenCredentials.has(row.credentialId) || seenCredentials.has(row.credentialClass)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate credential ${row.credentialId || row.credentialClass}`);
    }
    seenCredentials.add(row.credentialId);
    seenCredentials.add(row.credentialClass);
    if (!/Q[0-9]+/.test(row.ttlInput || '') && !/[Ee]xpiresAt/.test(row.ttlInput || '')) {
      counters.stage_field_missing += 1;
      issues.push(`credential ${row.credentialId} ttlInput must remain deferred or expiry-derived, got ${row.ttlInput}`);
    }
    for (const field of ['domain', 'audience', 'storage', 'scope', 'rotationRevocation', 'redaction']) {
      if (typeof row[field] !== 'string' || row[field].trim() === '') {
        counters.stage_field_missing += 1;
        issues.push(`credential ${row.credentialId} has an invalid ${field}`);
      }
    }
    if (/\*/.test(row.scope || '') || /\*/.test(row.audience || '')) {
      counters.stage_field_missing += 1;
      issues.push(`credential ${row.credentialId} must not use a wildcard scope or audience`);
    }
  }

  const seenAudiences = new Set();
  for (const row of model.audienceRegistry || []) {
    if (seenAudiences.has(row.audience)) {
      counters.stage_field_missing += 1;
      issues.push(`duplicate audience ${row.audience}`);
    }
    seenAudiences.add(row.audience);
    if (typeof row.ownerDomain !== 'string' || typeof row.verifier !== 'string') {
      counters.stage_field_missing += 1;
      issues.push(`audience ${row.audience} has an invalid owner or verifier`);
    }
  }

  const ivrProfile = (model.signatureProfiles || []).find((row) => row.profileId === 'ivr-request-v1');
  const providerProfile = (model.signatureProfiles || []).find((row) => row.profileId === 'provider-callback-v1');
  if (!ivrProfile || !providerProfile) {
    counters.stage_field_missing += 1;
    issues.push('model.signatureProfiles must contain exactly the IVR and provider profiles');
    return;
  }
  const headerChecks = [
    ['signature', 'X-ONECALL-SIGNATURE'],
    ['timestamp', 'X-ONECALL-TIMESTAMP'],
    ['nonce', 'X-ONECALL-NONCE']
  ];
  for (const [field, header] of headerChecks) {
    if (ivrProfile.headers?.[field] !== header) {
      counters.stage_field_missing += 1;
      issues.push(`IVR signature profile ${field} header drift`);
    }
  }
  if (providerProfile.headers?.signature !== 'X-ONECALL-CALLBACK-SIGNATURE' || providerProfile.headers?.timestamp !== 'X-ONECALL-TIMESTAMP' || providerProfile.headers?.nonce !== 'X-ONECALL-NONCE') {
    counters.stage_field_missing += 1;
    issues.push('provider signature profile header drift');
  }
  if (ivrProfile.headerLengths?.signatureLengthFrozen !== false || providerProfile.headerLengths?.signatureLengthFrozen !== false) {
    counters.stage_field_missing += 1;
    issues.push('A1 does not freeze a signature header length');
  }
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

function fixtureSlice(id) {
  if (/^sec-s1-/.test(id || '')) return 's1';
  if (/^sec-s2-/.test(id || '')) return 's2';
  if (/^sec-s3-/.test(id || '')) return 's3';
  return 'unknown';
}

function validateFixtures(fixturesDir, model, stage, counters, issues) {
  let files = [];
  try {
    files = fs.readdirSync(fixturesDir).sort();
  } catch (error) {
    counters.failed += 1;
    issues.push(`fixtures directory is unreadable: ${error.message}`);
    return { fixtureCount: 0, negativeCount: 0, s1Count: 0, s2Count: 0 };
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

  const s1Ids = EXPECTED_FIXTURES_S1.map((entry) => entry[0]);
  const s2Ids = EXPECTED_FIXTURES_S2.map((entry) => entry[0]);
  const expectedIds = stage === 's2' ? [...s1Ids, ...s2Ids] : s1Ids;
  const considered = stage === 's2'
    ? entries
    : entries.filter((entry) => fixtureSlice(entry.fixture?.id) === 's1');

  const actualIds = considered.map((entry) => entry.fixture?.id).filter((id) => typeof id === 'string');
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
  if (stage === 's1') {
    for (const entry of entries) {
      const slice = fixtureSlice(entry.fixture?.id);
      if (slice === 's3' || slice === 'unknown') {
        counters.fixture_id_extra += 1;
        issues.push(`premature fixture ID outside the S1 projection: ${entry.fixture?.id || entry.fileName}`);
        continue;
      }
      if (slice === 's2') {
        const expected = EXPECTED_FIXTURES_S2.find((row) => row[0] === entry.fixture?.id);
        if (!expected
          || entry.fixture?.slice !== 's2'
          || entry.fixture?.negative !== expected[1]
          || entry.fileName !== `${entry.fixture?.id}.json`) {
          counters.fixture_id_extra += 1;
          issues.push(`S2 fixture ${entry.fileName} is not a valid later-stage artifact`);
        }
      }
    }
  }

  for (const { fileName, fixture } of considered) {
    const fixtureIssues = [];
    const slice = fixtureSlice(fixture.id);
    if (fixture.schemaVersion !== 1) fixtureIssues.push('schemaVersion must be 1');
    if (fixture.slice !== slice) fixtureIssues.push(`slice must be ${slice}`);
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
      if (slice === 's1' && /INV-A3-(0[7-9]|1[0-9]|2[01])\b/.test(text)) {
        fixtureIssues.push('S1 fixture contains a future invariant ID');
      }
      if (slice === 's2' && /INV-A3-(1[2-9]|2[01])\b/.test(text)) {
        fixtureIssues.push('S2 fixture contains a future invariant ID');
      }
      const forbidden = slice === 's1' ? [...S2_MODEL_SECTIONS, ...S3_MODEL_SECTIONS] : S3_MODEL_SECTIONS;
      for (const section of forbidden) {
        if (text.includes(section)) {
          fixtureIssues.push(`fixture contains premature section token ${section}`);
        }
      }
    }
    if (fixtureIssues.length > 0) {
      counters.failed += 1;
      fixtureIssues.forEach((issue) => issues.push(`fixture ${fixture.id}: ${issue}`));
      continue;
    }
    const machine = createMachine(model, slice);
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
  const negativeCount = considered.filter((entry) => entry.fixture?.negative === true).length;
  const s1Count = considered.filter((entry) => fixtureSlice(entry.fixture?.id) === 's1').length;
  const s2Count = considered.filter((entry) => fixtureSlice(entry.fixture?.id) === 's2').length;
  return { fixtureCount: considered.length, negativeCount, s1Count, s2Count };
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

  const modelInfo = validateModel(model, args.stage, counters, issues);
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
  const s2MarkerRequested = args.stage === 's2'
    || ['security-domain-map', 'security-service-map', 'security-credential-map'].some((name) => doc.includes(`<!-- ${name}:start -->`));
  if (s2MarkerRequested) {
    const domainMarker = extractMarker(doc, MARKER_CONFIG.domain.name, counters, issues);
    const serviceMarker = extractMarker(doc, MARKER_CONFIG.service.name, counters, issues);
    const credentialMarker = extractMarker(doc, MARKER_CONFIG.credential.name, counters, issues);
    markerDefinitions.push(
      {
        name: MARKER_CONFIG.domain.name,
        marker: domainMarker,
        header: MARKER_CONFIG.domain.header,
        rows: (model.authDomains || []).map((row) => [
          row.domain,
          row.prefix,
          String(row.operations),
          (row.credentialClasses || []).join(', '),
          row.storage,
          row.tokenAudience,
          row.crossDomainRule
        ])
      },
      {
        name: MARKER_CONFIG.service.name,
        marker: serviceMarker,
        header: MARKER_CONFIG.service.header,
        rows: (model.serviceIdentities || []).map((row) => [
          row.serviceIdentity,
          row.mechanism,
          row.audience,
          (row.allowedCalls || []).join(', '),
          row.secretCustody
        ])
      },
      {
        name: MARKER_CONFIG.credential.name,
        marker: credentialMarker,
        header: MARKER_CONFIG.credential.header,
        rows: (model.credentialClasses || []).map((row) => MARKER_CONFIG.credential.header.map((column) => String(row[column])))
      }
    );
  }
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
  const tokenInvariants = args.stage === 's2' ? modelInfo.invariants : modelInfo.invariants.slice(0, 6);
  for (const row of tokenInvariants) {
    tokens.add(row.id);
  }
  for (const [state] of EXPECTED_KEY_STATES) {
    tokens.add(state);
  }
  if (args.stage === 's2') {
    for (const row of model.trustSurfaces || []) {
      tokens.add(row.surfaceId);
      tokens.add(row.domain);
    }
    for (const row of model.trustBoundaries || []) {
      tokens.add(row.boundaryId);
    }
    for (const row of model.serviceIdentities || []) {
      tokens.add(row.serviceIdentity);
      tokens.add(row.audience);
    }
    for (const row of model.credentialClasses || []) {
      tokens.add(row.credentialId);
      tokens.add(row.credentialClass);
      tokens.add(row.audience);
    }
    for (const row of model.signatureProfiles || []) {
      tokens.add(row.profileId);
    }
    for (const row of model.audienceRegistry || []) {
      tokens.add(row.audience);
    }
  }
  for (const token of [...tokens].sort()) {
    if (!doc.includes(token)) {
      counters.token_missing += 1;
      issues.push(`document does not mention model token ${token}`);
    }
  }
  for (const row of tokenInvariants) {
    if (!doc.includes(row.statement)) {
      counters.token_missing += 1;
      issues.push(`document does not contain invariant statement ${row.id}`);
    }
  }

  const futureInvariantMatch = doc.match(/INV-A3-(1[2-9]|2[01])\b/);
  if (futureInvariantMatch) {
    counters.stage_field_premature += 1;
    issues.push('document contains a future invariant ID');
  }
  if (/sec-s3-/.test(doc)) {
    counters.stage_field_premature += 1;
    issues.push('document contains a future fixture ID');
  }
  for (const section of S3_MODEL_SECTIONS) {
    if (doc.includes(section)) {
      counters.stage_field_premature += 1;
      issues.push(`document contains premature S3 section token ${section}`);
    }
  }
  if (/https?:\/\//.test(doc)) {
    counters.stage_field_premature += 1;
    issues.push('document contains a URL');
  }

  const fixtureInfo = validateFixtures(args.fixtures, model, args.stage, counters, issues);

  const drift = counters.a0_drift + counters.a1_drift + counters.a2_drift;
  const keyCount = modelInfo.keyClasses.length;
  const dataCount = modelInfo.dataClasses.length;
  const deferredCount = modelInfo.deferred.length;
  const deferredQ = modelInfo.deferred.filter((row) => /^Q[0-9]+$/.test(row.inputId)).length;
  const deferredCv = modelInfo.deferred.filter((row) => /^CV-[0-9]+$/.test(row.inputId)).length;
  const projectedInvariantCount = args.stage === 's2' ? modelInfo.invariants.length : Math.min(modelInfo.invariants.length, 6);
  const expectedFixtureCount = args.stage === 's2' ? 15 : 6;
  const sliceLabel = args.stage === 's2'
    ? `${fixtureInfo.s1Count}+${fixtureInfo.s2Count}`
    : String(fixtureInfo.s1Count);

  if (args.stage === 's2') {
    process.stdout.write(`stage=s2 trust_surfaces=${(model.trustSurfaces || []).length} auth_domains=${(model.authDomains || []).length} trust_boundaries=${(model.trustBoundaries || []).length} service_identities=${(model.serviceIdentities || []).length} credential_classes=${(model.credentialClasses || []).length} signature_profiles=${(model.signatureProfiles || []).length} key_classes=${keyCount} data_classes=${dataCount} deferred_inputs=${deferredCount} deferred_q=${deferredQ} deferred_cv=${deferredCv} invariants=${projectedInvariantCount} fixtures=${fixtureInfo.fixtureCount} passed=${counters.passed} failed=${counters.failed} negative=${fixtureInfo.negativeCount} slice_fixtures=${sliceLabel}\n`);
  } else {
    process.stdout.write(`stage=s1 key_classes=${keyCount} data_classes=${dataCount} deferred_inputs=${deferredCount} deferred_q=${deferredQ} deferred_cv=${deferredCv} invariants=${projectedInvariantCount} fixtures=${fixtureInfo.fixtureCount} passed=${counters.passed} failed=${counters.failed} negative=${fixtureInfo.negativeCount} slice_fixtures=${sliceLabel}\n`);
  }
  process.stdout.write(`a0_tables=${a0Tables} a0_sensitive_rows=${a0SensitiveRows.length} a1_operations=${a1Info.operationCount} a1_schemes=${a1Info.schemeCount} a2_messages=${a2Model.messages?.length || 0} a2_event_types=${a2Model.eventTypes?.length || 0} a2_identities=${a2Model.identities?.length || 0} a2_invariants=${a2Model.invariants?.length || 0} a2_event_registry_sha256=${a2Digest} drift=${drift}\n`);
  process.stdout.write(`doc_missing=${counters.doc_missing} doc_extra=${counters.doc_extra} doc_duplicate=${counters.doc_duplicate} doc_invalid=${counters.doc_invalid} token_missing=${counters.token_missing}\n`);
  process.stdout.write(`stage_field_missing=${counters.stage_field_missing} stage_field_premature=${counters.stage_field_premature} fixture_id_missing=${counters.fixture_id_missing} fixture_id_extra=${counters.fixture_id_extra}\n`);

  const stageCountsOk = args.stage === 's2'
    ? (model.trustSurfaces || []).length === 5
      && (model.authDomains || []).length === 5
      && (model.trustBoundaries || []).length === 7
      && (model.serviceIdentities || []).length === 6
      && (model.credentialClasses || []).length === 10
      && (model.signatureProfiles || []).length === 2
      && projectedInvariantCount === 11
      && fixtureInfo.s1Count === 6
      && fixtureInfo.s2Count === 9
      && fixtureInfo.negativeCount === 7
    : fixtureInfo.s1Count === 6
      && fixtureInfo.negativeCount === 2;
  const failed = counters.passed !== fixtureInfo.fixtureCount
    || fixtureInfo.fixtureCount !== expectedFixtureCount
    || !stageCountsOk
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
