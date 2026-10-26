#!/usr/bin/env node
// A5 production acceptance matrix validator (S1 only).
// Zero third-party dependencies. Node 22 ESM.
//
// This validator proves specification coverage, source resolution and
// expectation consistency against accepted A0-A4 contracts. It never
// executes a runtime system and never emits production acceptance.

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const AXES = ['NORMAL', 'DUPLICATE', 'TIMEOUT', 'OUT_OF_ORDER', 'RECOVERY', 'PERMISSION_DENIED'];
const FAMILIES = [
  'FAM-IDENTITY',
  'FAM-SUBJECT',
  'FAM-ACTIVATION',
  'FAM-COMMERCE',
  'FAM-IVR',
  'FAM-MEDIA-EVIDENCE',
  'FAM-SHARING',
  'FAM-SECURITY',
  'FAM-DEPLOYMENT'
];
const PATHS = [
  'PATH-ACTIVATION-TRIAL',
  'PATH-COMMERCE-ENTITLEMENT',
  'PATH-IVR-SETTLEMENT',
  'PATH-RECORDING-EVIDENCE',
  'PATH-SHARE-ACCESS',
  'PATH-AUTH-AUDIT',
  'PATH-DEPLOY-ACCEPTANCE'
];
const UNITS = [...FAMILIES, ...PATHS];
const PATH_PAIR = {
  'PATH-ACTIVATION-TRIAL': 'FAM-ACTIVATION',
  'PATH-COMMERCE-ENTITLEMENT': 'FAM-COMMERCE',
  'PATH-IVR-SETTLEMENT': 'FAM-IVR',
  'PATH-RECORDING-EVIDENCE': 'FAM-MEDIA-EVIDENCE',
  'PATH-SHARE-ACCESS': 'FAM-SHARING',
  'PATH-AUTH-AUDIT': 'FAM-SECURITY',
  'PATH-DEPLOY-ACCEPTANCE': 'FAM-DEPLOYMENT'
};
const EXECUTION_TIERS = ['STATIC_SPEC', 'MODEL_FIXTURE', 'INTEGRATION', 'LINE', 'CLOUD', 'MANUAL'];
const ACCEPTANCE_RULES = new Set(['A5_ACCEPT_RUNTIME_TIER_AND_AUTHORITY', 'A5_ACCEPT_NOT_APPLICABLE']);
const PROCESSING = new Set(['APPLIED', 'IGNORED', 'QUARANTINED', 'RECEIVED']);
const METRIC_CATEGORIES = new Set([
  'DOC12:S12#IVR',
  'DOC12:S12#TRANSACTION',
  'DOC12:S12#EVIDENCE',
  'DOC12:S12#SHARING',
  'DOC12:S12#ASYNC',
  'DOC12:S12#EXTERNAL_DEPENDENCY'
]);
const ORDERING_BY_AXIS = {
  NORMAL: new Set(['NORMAL']),
  DUPLICATE: new Set(['IDENTICAL_RETRY', 'CONFLICTING_RETRY']),
  TIMEOUT: new Set(['DEADLINE_CROSSING', 'DEPENDENCY_FAILURE']),
  OUT_OF_ORDER: new Set(['OUT_OF_ORDER', 'POST_TERMINAL']),
  RECOVERY: new Set(['RESTART', 'FAILOVER']),
  PERMISSION_DENIED: new Set(['PERMISSION_DENIED'])
};
const EMPTY_EDGE_CLASSES = new Set([
  'INSERT_ONLY',
  'APPEND_ONLY',
  'FROZEN_RESULT',
  'DERIVED_IMMUTABLE',
  'DERIVED_FACT',
  'APPEND_ONLY_HISTORY',
  'APPEND_ONLY_OUTCOMES',
  'PROSE_PARENTHESIZED'
]);
const NA_REGISTRY = {
  'NA-NO-CONTRACT-DEADLINE': {
    allowed: new Set([
      'FAM-SUBJECT:TIMEOUT',
      'FAM-DEPLOYMENT:TIMEOUT',
      'PATH-DEPLOY-ACCEPTANCE:TIMEOUT'
    ]),
    reason:
      'A0 subject claim/binding/risk rows carry no deadline or expiry semantics; A4 explicitly leaves SLO/timeout/RPO/RTO unapproved and its backup/capability gates are not request deadlines.',
    anchor: new Set([
      'A0:SM:Subject claim',
      'A0:SM:Subject risk state',
      'A0:SM:Caller-number binding',
      'A4:SECTION:8.4',
      'A4:SECTION:10.4',
      'DOC12:S17.1'
    ])
  },
  'NA-NO-ORDERED-FACT-STREAM': {
    allowed: new Set([
      'FAM-DEPLOYMENT:OUT_OF_ORDER',
      'PATH-DEPLOY-ACCEPTANCE:OUT_OF_ORDER'
    ]),
    reason:
      'A4 BACKUP_DEPENDENCY and CAPABILITY inputs are exact-set typed inputs; duplicates are INVALID and there is no event sequence or arrival-order semantic.',
    anchor: new Set([
      'A4:SECTION:9.1',
      'A4:SECTION:10.1',
      'A4:FIX:s2-01-backup-recovery',
      'A4:FIX:s2-02-capability-assertion'
    ])
  },
  'NA-NO-AUTHZ-BOUNDARY': {
    allowed: new Set(['FAM-DEPLOYMENT:PERMISSION_DENIED']),
    reason:
      'The A4 calculator has no runtime caller identity, role or tenant; CALLER_CONTROL_FORBIDDEN is input integrity, not permission. A3 auth domains cover API surfaces only.',
    anchor: new Set([
      'A4:SECTION:1.2',
      'A4:SECTION:8.3',
      'A4:SECTION:10.4',
      'A3:TS:TS1',
      'A3:TS:TS5'
    ])
  }
};
const REASON_CODES = [
  'A5_MODEL_MISSING', 'A5_MODEL_INVALID', 'A5_SECTION_MISSING', 'A5_SECTION_EXTRA',
  'A5_DUPLICATE_ID', 'A5_UNKNOWN_AXIS', 'A5_UNKNOWN_UNIT', 'A5_ORPHAN_SCENARIO',
  'A5_COVERAGE_CELL_MISSING', 'A5_COVERAGE_CELL_DUPLICATE', 'A5_COVERAGE_CELL_ILLEGAL',
  'A5_APPLICABILITY_ILLEGAL', 'A5_NA_REASON_UNKNOWN', 'A5_NA_REASON_UNBACKED',
  'A5_SCENARIO_FIELD_MISSING', 'A5_SCENARIO_FIELD_INVALID',
  'A5_SOURCE_REF_INVALID', 'A5_SOURCE_REF_UNRESOLVED', 'A5_SOURCE_REF_AMBIGUOUS',
  'A5_SOURCE_REF_DUPLICATE', 'A5_SOURCE_DRIFT', 'A5_SOURCE_BASELINE_MISMATCH',
  'A5_A0_STATE_SHAPE_UNRECOGNIZED', 'A5_TRANSITION_REF_INVALID',
  'A5_OWNER_UPSTREAM_UNRESOLVED', 'A5_RESIDUAL_OWNER_UNKNOWN',
  'A5_PATH_FAMILY_DUPLICATE', 'A5_EXTERNAL_PROJECTION_CONTRADICTION',
  'A5_EVIDENCE_OWNER_MISSING', 'A5_EVIDENCE_OWNER_UNKNOWN',
  'A5_EVIDENCE_OWNER_MISMATCH', 'A5_EVIDENCE_DELEGATION_UNRESOLVED',
  'A5_EFFECT_VALUE_INVALID', 'A5_EFFECT_BOUND_UNBACKED', 'A5_EFFECT_LEDGER_MISMATCH',
  'A5_AXIS_STIMULUS_MISMATCH', 'A5_STATE_SEQUENCE_MISMATCH', 'A5_TERMINAL_MUTATION',
  'A5_DUPLICATE_SIDE_EFFECT', 'A5_ORDERING_REGRESSION', 'A5_DENIED_WITH_BUSINESS_EFFECT',
  'A5_CLEANUP_ILLEGAL',
  'A5_EVIDENCE_MANIFEST_INVALID', 'A5_EVIDENCE_TIER_INSUFFICIENT',
  'A5_EVIDENCE_ARTIFACT_MISSING', 'A5_EVIDENCE_REF_INVALID',
  'A5_EVIDENCE_DIGEST_MISMATCH', 'A5_EVIDENCE_TIME_INVALID',
  'A5_EVIDENCE_STATE_CONTRADICTION', 'A5_SYNTHETIC_EVIDENCE_REJECTED',
  'A5_CALLER_CONTROL_FORBIDDEN', 'A5_CALLER_ASSERTED_PASS',
  'A5_ACCEPTANCE_AUTHORITY_MISSING', 'A5_ACCEPTANCE_AUTHORITY_INVALID',
  'A5_ACCEPTANCE_EVIDENCE_MISSING', 'A5_ACCEPTANCE_IMMUTABILITY_VIOLATION',
  'A5_RESIDUAL_OWNER_MISSING', 'A5_RESIDUAL_DESTINATION_MISSING',
  'A5_DOC_MODEL_DRIFT', 'A5_DOC_MARKER_MISSING', 'A5_DOC_MARKER_DUPLICATE',
  'A5_PROHIBITED_READINESS_FLAG', 'A5_COUNT_ONLY_COVERAGE',
  'A5_DETERMINISM_FAILURE', 'A5_LEGACY_REFERENCE_FORBIDDEN'
];
const DENIAL_ALLOWLIST = new Set([
  'audit.audit_fact',
  'sharing.share_access_event',
  'ivr.call_event',
  'ivr.ivr_decision',
  'ivr.call_record',
  'platform.idempotency_record'
]);
const SUCCESS_TABLES = new Set([
  'entitlement_metering.usage_fact',
  'entitlement_metering.usage_reservation',
  'entitlement_metering.entitlement_grant',
  'entitlement_metering.trial_grant',
  'catalog_order.customer_order',
  'catalog_order.payment_event',
  'evidence.evidence_record',
  'evidence.media_object',
  'sharing.share_admission',
  'activation_consent.service_activation',
  'subject_number.subject_claim',
  'customer_iam.auth_identity'
]);
const DUPLICATE_EXCEPTIONS = new Set(['platform.idempotency_record', 'audit.audit_fact']);
const A0_MODULES = new Set([
  'customer-iam', 'organization-access', 'operations-iam', 'subject-number',
  'catalog-order', 'entitlement-metering', 'activation-consent', 'ivr',
  'evidence', 'sharing', 'platform-reliability', 'operations-audit'
]);
const LEGACY_MARKERS = [
  'docs/04', 'prototype', 'operations-prototype', 'web-prototype',
  'screenshot', 'shared/onecall-business.js'
];
const A0_TX_RE = /^T-S[123]-\d\d$/;
const SCENARIO_ID_RE = /^A5-(IDENT|SUBJ|ACTIV|COMM|IVR|MEDIA|SHARE|SEC|DEPLOY|X-(ACTIV|COMM|IVR|MEDIA|SHARE|AUTH|DEPLOY))-(NORMAL|DUPLICATE|TIMEOUT|OUT_OF_ORDER|RECOVERY|PERMISSION_DENIED)-[0-9]{2}$/;
const EXTERNAL_FIELDS = new Set([
  'status', 'action', 'reasonCode', 'problemCode', 'processingStatus', 'terminalStatus'
]);
const EXTERNAL_OPS = new Set(['EQUALS', 'UNCHANGED', 'NULL', 'NOT_NULL']);
// basisRefs are source references, so every entry must carry a recognized
// source-ref prefix. Empty/whitespace strings, non-strings and free text are
// rejected here; resolution (unresolved/ambiguous/legacy) stays in the refs phase.
const BASIS_REF_SHAPE = /^(A0|A1|A2|A3|A4|DOC12|DOC14):/;
// A timingRef may only point at a contract/policy input; no bare seconds/SLO.
const TIMING_REF_PREFIXES = ['A0:Q:', 'A0:TABLE:', 'A2:IDENTITY:', 'A3:DEFERRED:', 'A3:CV:'];
// Closed timing policy registry from the frozen plan/upstream sources. A
// resolvable ref outside this set is not a contract/policy timing input.
const TIMING_POLICY_REFS = new Set([
  'A0:TABLE:entitlement_metering.trial_policy_version',
  'A2:IDENTITY:upload expiry',
  'A3:DEFERRED:Q1',
  'A3:DEFERRED:Q6',
  'A3:DEFERRED:Q22',
  'A3:DEFERRED:Q25'
]);
// Validator-owned minimum runtime evidence derived from accepted plan §4.2.
// Inclusion is exact-set, never a linear rank: every listed tier is required.
const UNIT_MINIMUM_TIERS = {
  'FAM-IDENTITY': ['INTEGRATION'],
  'FAM-SUBJECT': ['INTEGRATION'],
  'FAM-ACTIVATION': ['INTEGRATION'],
  'FAM-COMMERCE': ['INTEGRATION'],
  'FAM-IVR': ['INTEGRATION', 'LINE'],
  'FAM-MEDIA-EVIDENCE': ['INTEGRATION', 'CLOUD'],
  'FAM-SHARING': ['INTEGRATION', 'CLOUD'],
  'FAM-SECURITY': ['INTEGRATION'],
  'FAM-DEPLOYMENT': ['INTEGRATION', 'CLOUD'],
  'PATH-ACTIVATION-TRIAL': ['INTEGRATION', 'LINE'],
  'PATH-COMMERCE-ENTITLEMENT': ['INTEGRATION'],
  'PATH-IVR-SETTLEMENT': ['INTEGRATION', 'LINE'],
  'PATH-RECORDING-EVIDENCE': ['INTEGRATION', 'CLOUD'],
  'PATH-SHARE-ACCESS': ['INTEGRATION', 'CLOUD'],
  'PATH-AUTH-AUDIT': ['INTEGRATION'],
  'PATH-DEPLOY-ACCEPTANCE': ['INTEGRATION', 'CLOUD', 'MANUAL']
};

// S2 evidence admission contract. These are validator-owned constants so a
// coordinated model/doc edit cannot weaken caller/synthetic/tier rejection.
const EVIDENCE_POLICY_ID = 'A5-EVIDENCE-POLICY-V1';
const EVIDENCE_CLASSES = new Set(['STATIC_SPEC', 'MODEL_FIXTURE', 'SYNTHETIC', 'RUNTIME']);
const SPEC_ONLY_TIERS = ['STATIC_SPEC', 'MODEL_FIXTURE'];
const RUNTIME_TIERS = ['INTEGRATION', 'LINE', 'CLOUD', 'MANUAL'];
const ARTIFACT_ROLES = [
  'AUDIT_EXPORT', 'OUTBOX_EXPORT', 'DB_STATE_EXPORT', 'METRIC_SNAPSHOT',
  'LOG_EXPORT', 'LINE_CDR', 'CLOUD_EVIDENCE', 'MANUAL_RECORD', 'TEST_REPORT'
];
const ARTIFACT_FIELDS = [
  'artifactId', 'role', 'locatorRef', 'sha256', 'capturedAt', 'producerRef', 'redactionRef'
];
const MANIFEST_FIELDS = [
  'manifestId', 'scenarioId', 'evidenceOwner', 'evidenceClass', 'evidenceTiers',
  'executedAt', 'recordedAt', 'artifacts', 'observations', 'manifestDigest'
];
const ACCEPTANCE_RECORD_FIELDS = [
  'recordId', 'scenarioId', 'manifestId', 'manifestDigest', 'authorityRef', 'acceptedAt', 'result'
];
const AUTHORITY_FIELDS = [
  'authorityId', 'status', 'owner', 'approvedAt', 'authorizationBasisRef'
];
const AUTHORITY_STATUSES = ['APPROVED', 'SYNTHETIC_TEST_ONLY'];
const ACCEPTANCE_RECORD_RESULTS = ['PASS', 'FAIL'];
const MANIFEST_CALLER_FIELDS = ['result', 'pass', 'productionAccepted', 'execution'];
const MANIFEST_CALLER_ASSERTED_FIELDS = ['accepted'];
const EVIDENCE_STATES = [
  'NOT_RUN', 'EVIDENCE_SUBMITTED', 'EVIDENCE_REJECTED', 'EVIDENCE_VALID',
  'BLOCKED', 'PASS', 'FAIL', 'NOT_APPLICABLE'
];
const ISO_Z_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$/;
const SHA256_RE = /^[0-9a-f]{64}$/;
const LOCATOR_FORBIDDEN = [
  '?', 'http://', 'https://', '://', 'authorization', 'bearer', 'credential',
  'token', 'bucket', 'version', 'access_key', 'secret', 'x-cos-', 'cos.ap-',
  '.cos.'
];
const EVIDENCE_POLICY_CONTRACT = {
  policyId: EVIDENCE_POLICY_ID,
  evidenceClasses: [...EVIDENCE_CLASSES],
  specOnlyTiers: [...SPEC_ONLY_TIERS],
  runtimeTiers: [...RUNTIME_TIERS],
  artifactRoles: [...ARTIFACT_ROLES],
  artifactFields: [...ARTIFACT_FIELDS],
  manifestFields: [...MANIFEST_FIELDS],
  acceptanceRecordFields: [...ACCEPTANCE_RECORD_FIELDS],
  acceptanceRecordResults: [...ACCEPTANCE_RECORD_RESULTS],
  authorityFields: [...AUTHORITY_FIELDS],
  authorityStatuses: [...AUTHORITY_STATUSES],
  callerForbiddenFields: [...MANIFEST_CALLER_FIELDS],
  callerAssertedFields: [...MANIFEST_CALLER_ASSERTED_FIELDS],
  requiredStateMachine: [...EVIDENCE_STATES],
  acceptanceRules: [...ACCEPTANCE_RULES],
  maximumAdmittedState: 'EVIDENCE_VALID',
  unreachableResidual: 'A5-RES-02'
};

const root = (issues, code, id = '', p = '') => issues.push({ code, path: p, id: String(id) });

function isObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}
function isString(value) {
  return typeof value === 'string' && value.length > 0;
}
function isStringArray(value) {
  return Array.isArray(value) && value.every((item) => typeof item === 'string');
}
function canonicalOwnerText(value) {
  return String(value)
    .trim()
    .replace(/\s+/g, ' ')
    .replace(/\s*\/\s*/g, '/');
}
// Deterministic canonical JSON: recursively sorted object keys, array order
// preserved. Used only to derive/compare evidence manifest digests.
function canonicalStringify(value) {
  if (Array.isArray(value)) return `[${value.map((item) => canonicalStringify(item)).join(',')}]`;
  if (value !== null && typeof value === 'object') {
    return `{${Object.keys(value).sort()
      .map((key) => `${JSON.stringify(key)}:${canonicalStringify(value[key])}`)
      .join(',')}}`;
  }
  return JSON.stringify(value);
}
function sha256Hex(text) {
  return crypto.createHash('sha256').update(text, 'utf8').digest('hex');
}
function manifestDigestOf(manifest) {
  const body = { ...manifest };
  delete body.manifestDigest;
  return sha256Hex(canonicalStringify(body));
}
function parseIsoZ(value) {
  if (typeof value !== 'string' || !ISO_Z_RE.test(value)) return null;
  const millis = Date.parse(value);
  return Number.isFinite(millis) ? millis : null;
}
function sameStringSet(left, right) {
  if (!Array.isArray(left) || !Array.isArray(right)) return false;
  if (left.length !== right.length) return false;
  const set = new Set(left);
  return set.size === right.length && right.every((item) => set.has(item));
}
function sortEdges(edges) {
  return [...edges]
    .map(([from, to]) => [from, to])
    .sort((a, b) => (a[0] === b[0] ? a[1].localeCompare(b[1]) : a[0].localeCompare(b[0])));
}
function splitTopLevelCommas(cell) {
  const parts = [];
  let depth = 0;
  let start = 0;
  for (let i = 0; i < cell.length; i += 1) {
    const ch = cell[i];
    if (ch === '(') depth += 1;
    else if (ch === ')') depth -= 1;
    else if (ch === ',' && depth === 0) {
      parts.push(cell.slice(start, i));
      start = i + 1;
    }
  }
  parts.push(cell.slice(start));
  return parts;
}
function cleanStateToken(text) {
  return text.replace(/`/g, '').replace(/[;,\]]+$/g, '').trim();
}
function tokenizeStates(cell) {
  const states = [];
  for (const raw of splitTopLevelCommas(cell)) {
    const part = raw.trim();
    if (!part) continue;
    if (part.startsWith('(')) {
      const close = part.indexOf(')');
      if (close === -1) throw new ShapeError('unbalanced parentheses');
      states.push(part.slice(0, close + 1));
      continue;
    }
    const tick = part.match(/`([^`]+)`/);
    if (tick) {
      const literal = tick[1].trim();
      if (literal.startsWith('(')) {
        if (!literal.endsWith(')')) throw new ShapeError('unbalanced tuple literal');
        states.push(literal);
      } else if (/^[A-Z][A-Z0-9_]*$/.test(literal)) {
        states.push(literal);
      } else {
        throw new ShapeError(`unrecognized state token: ${literal}`);
      }
      continue;
    }
    if (/^[A-Z][A-Z0-9_]*$/.test(part)) {
      states.push(part);
    } else {
      throw new ShapeError(`unrecognized state token: ${part}`);
    }
  }
  if (states.length === 0) throw new ShapeError('empty state set');
  return [...new Set(states)];
}
function slashComponents(token) {
  return token
    .split('/')
    .map((part) => cleanStateToken(part))
    .filter(Boolean);
}
function stateTokensIn(text) {
  return [...text.matchAll(/[A-Z][A-Z0-9_]*/g)].map((match) => match[0]);
}
function extractEdges(cell, states) {
  const tokens = cell.split(/\s+/).filter(Boolean);
  const edges = [];
  const stateSet = new Set(states);
  for (let i = 0; i < tokens.length; i += 1) {
    const token = tokens[i];
    const hasRight = token.includes('→');
    const hasBoth = token.includes('↔');
    if (!hasRight && !hasBoth) continue;
    const arrow = hasBoth ? '↔' : '→';
    const pieces = token.split(arrow);
    let leftText;
    let rightText;
    if (pieces.length >= 2) {
      leftText = pieces[0];
      rightText = pieces.slice(1).join(arrow);
      if (!leftText && i > 0) leftText = tokens[i - 1];
      if (!rightText && i + 1 < tokens.length) rightText = tokens[i + 1];
    } else {
      leftText = i > 0 ? tokens[i - 1] : '';
      rightText = i + 1 < tokens.length ? tokens[i + 1] : '';
    }
    let left = slashComponents(leftText).flatMap(stateTokensIn);
    let right = slashComponents(rightText).flatMap(stateTokensIn);
    if (left.length > 1 && i >= 2 && ['→', '↔'].includes(tokens[i - 2])) {
      left = [stateTokensIn(slashComponents(leftText).slice(-1)[0] || '')].flat().slice(-1);
    }
    if (right.length > 1 && i + 2 < tokens.length && ['→', '↔'].includes(tokens[i + 2])) {
      right = [stateTokensIn(slashComponents(rightText)[0] || '')].flat().slice(0, 1);
    }
    left = [...new Set(left)];
    right = [...new Set(right)];
    if (left.length === 0 && right.length === 0) continue;
    if (left.length === 0 || right.length === 0) {
      throw new ShapeError(`half-state arrow at ${token}`);
    }
    for (const from of left) {
      for (const to of right) {
        if (!stateSet.has(from) || !stateSet.has(to)) {
          throw new ShapeError(`arrow endpoint not an allowed state: ${from}->${to}`);
        }
        edges.push([from, to]);
        if (hasBoth) edges.push([to, from]);
      }
    }
  }
  const seen = new Set();
  return edges.filter(([from, to]) => {
    const key = `${from}\u0000${to}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

class ShapeError extends Error {
  constructor(message) {
    super(message);
    this.code = 'A5_A0_STATE_SHAPE_UNRECOGNIZED';
  }
}

function classifyShape(aggregate, allowed, transitions) {
  if (
    allowed.includes('(owner null, owner null)') &&
    allowed.includes('(account, null)') &&
    transitions.includes('Only null → personal account')
  ) return 'PROSE_PARENTHESIZED';
  if (transitions.includes('Insert-only outcome; no transition')) return 'INSERT_ONLY';
  if (transitions.includes('Same verified-evidence transitions as the order')) return 'SHARED_EDGE_SET';
  if (transitions.includes('No transition; append-only')) return 'APPEND_ONLY';
  if (allowed.includes('GRANTED') && allowed.includes('derived') && transitions.includes('No row transition')) {
    return 'DERIVED_IMMUTABLE';
  }
  if (allowed.includes('Frozen purpose-specific result (no status transition)')) return 'FROZEN_RESULT';
  if (allowed.includes('other evidence events are append-only history')) return 'APPEND_ONLY_HISTORY';
  if (
    allowed.includes('Admission is an immutable one-time fact') &&
    allowed.includes('derived, not a PostgreSQL state')
  ) return 'DERIVED_FACT';
  if (allowed.includes('Append-only success/denied/failed/ignored outcomes')) return 'APPEND_ONLY_OUTCOMES';
  if (allowed.includes('→') || allowed.includes('↔')) return 'STATE_CELL_ARROW';
  return 'STANDARD';
}

function parseStateRow(raw) {
  const aggregate = String(raw.aggregate || '').trim();
  const allowed = String(raw.allowedStates || '').trim();
  const transitions = String(raw.transitions || '').trim();
  const owner = String(raw.owner || '').trim();
  if (!aggregate || !owner) throw new ShapeError('missing aggregate or owner');
  const classification = classifyShape(aggregate, allowed, transitions);
  let states = [];
  let derivedStates = [];
  let edges = [];
  if (classification === 'PROSE_PARENTHESIZED') {
    for (const rawPart of splitTopLevelCommas(allowed)) {
      const part = cleanStateToken(rawPart);
      if (!part.startsWith('(')) throw new ShapeError('non-tuple subject ownership state');
      states.push(part);
    }
  } else if (classification === 'DERIVED_IMMUTABLE') {
    states = ['GRANTED'];
    derivedStates = [...allowed.matchAll(/`([A-Z][A-Z0-9_]*)`/g)]
      .map((match) => match[1])
      .filter((name) => name !== 'GRANTED');
  } else if (classification === 'SHARED_EDGE_SET') {
    states = tokenizeStates(allowed);
  } else if (classification === 'STATE_CELL_ARROW') {
    const tick = allowed.match(/`([^`]*(?:→|↔)[^`]*)`/);
    if (!tick) throw new ShapeError('state-cell arrow missing');
    const operator = tick[1].includes('↔') ? '↔' : '→';
    const parts = tick[1].split(/[→↔]/).map((part) => cleanStateToken(part));
    if (parts.length !== 2) throw new ShapeError('state-cell arrow arity invalid');
    if (parts.some((part) => !/^[A-Z][A-Z0-9_]*$/.test(part))) {
      throw new ShapeError('state-cell arrow endpoint invalid');
    }
    states = [...new Set(parts)];
    edges = [[parts[0], parts[1]]];
    if (operator === '↔') edges.push([parts[1], parts[0]]);
  } else if (classification === 'INSERT_ONLY' || classification === 'APPEND_ONLY') {
    states = tokenizeStates(allowed);
  } else if (
    classification === 'FROZEN_RESULT' ||
    classification === 'APPEND_ONLY_HISTORY' ||
    classification === 'DERIVED_FACT' ||
    classification === 'APPEND_ONLY_OUTCOMES'
  ) {
    states = [];
  } else {
    states = tokenizeStates(allowed);
    edges = extractEdges(transitions, states);
    if (edges.length === 0) throw new ShapeError('arrowless standard row');
  }
  return {
    aggregate,
    allowedStates: allowed,
    transitions,
    owner,
    attachedNote: raw.attachedNote ? String(raw.attachedNote).trim() : null,
    classification,
    states,
    derivedStates: [...new Set(derivedStates)],
    edges: sortEdges(edges)
  };
}

function parseA0StateSections(doc) {
  const lines = doc.split('\n');
  const sections = [];
  let current = null;
  for (const line of lines) {
    const heading = line.match(/^### (7\.[134]) /);
    if (heading) {
      current = { id: heading[1], rows: [] };
      sections.push(current);
      continue;
    }
    if (/^### /.test(line) || /^## /.test(line)) {
      current = null;
      continue;
    }
    if (!current || !line.startsWith('|')) continue;
    // Accepted A0 rows may or may not end with a trailing pipe (the Call record
    // row carries its attached note without one); strip only a present wrapper.
    const cells = line.replace(/^\|/, '').replace(/\|$/, '').split('|').map((cell) => cell.trim());
    if (cells.length === 0) continue;
    if (cells[0] === 'Aggregate') {
      if (cells.length !== 4 || cells[1] !== 'Allowed states' ||
          cells[2] !== 'Allowed transitions and guards' || cells[3] !== 'Owner') {
        throw new ShapeError(`state table header changed in section ${current.id}`);
      }
      continue;
    }
    if (/^-+$/.test(cells[0].replace(/[: ]/g, ''))) continue;
    if (cells.length > 4 && !(cells[0] === 'Call record' && cells.length === 5)) {
      throw new ShapeError(`unexpected state row width in ${current.id}: ${cells[0]}`);
    }
    const row = {
      aggregate: cells[0],
      allowedStates: cells[1],
      transitions: cells[2],
      owner: cells[3],
      attachedNote: cells.length === 5 ? cells[4] : null,
      section: current.id
    };
    current.rows.push(row);
  }
  const bySection = Object.fromEntries(sections.map((section) => [section.id, section.rows]));
  const all = [
    ...(bySection['7.1'] || []),
    ...(bySection['7.3'] || []),
    ...(bySection['7.4'] || [])
  ];
  return { bySection, all };
}

function parseA0(doc) {
  const catalogMatch = doc.match(/<!-- catalog:start -->([\s\S]*?)<!-- catalog:end -->/);
  if (!catalogMatch) throw new Error('A0 catalog block missing');
  const tables = new Map();
  for (const line of catalogMatch[1].split('\n')) {
    if (!line.startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells[0] === 'Table') continue;
    if (/^-+$/.test(cells[0].replace(/[: ]/g, ''))) continue;
    const name = cells[0].replace(/`/g, '');
    const columns = [...cells[4].matchAll(/`([a-z_][a-z0-9_]*)`/g)].map((match) => match[1]);
    tables.set(name, { owner: cells[1].replace(/`/g, ''), columns: new Set(columns), slice: cells[3] });
  }
  const invariantBlock = doc.match(/### 6\.1[\s\S]*?(?=### 6\.2)/);
  if (!invariantBlock) throw new Error('A0 invariant section missing');
  const invariants = new Set();
  for (const line of invariantBlock[0].split('\n')) {
    const match = line.match(/^\| (INV-\d\d) \|/);
    if (match) invariants.add(match[1]);
  }
  const { bySection, all: rawRows } = parseA0StateSections(doc);
  const stateRows = new Map();
  for (const raw of rawRows) {
    if (stateRows.has(raw.aggregate)) throw new ShapeError(`duplicate aggregate ${raw.aggregate}`);
    stateRows.set(raw.aggregate, parseStateRow(raw));
  }
  const customerOrder = stateRows.get('Customer order');
  const paymentOrder = stateRows.get('Payment order');
  if (paymentOrder && paymentOrder.classification === 'SHARED_EDGE_SET') {
    if (!customerOrder) throw new ShapeError('shared edge set without Customer order');
    const stateSetsEqual =
      paymentOrder.states.length === customerOrder.states.length &&
      paymentOrder.states.every((state) => customerOrder.states.includes(state));
    if (!stateSetsEqual) throw new ShapeError('Payment order states differ from Customer order');
    paymentOrder.edges = customerOrder.edges.map(([from, to]) => [from, to]);
  }
  const transactions = new Set();
  for (const line of doc.split('\n')) {
    const match = line.match(/^### 9\.\d+ (T-S[123]-\d\d) /);
    if (match) transactions.add(match[1]);
  }
  const qRows = new Map();
  const qBlock = doc.match(/## 14\. Open Production Inputs([\s\S]*?)(?=\n## |$)/);
  if (!qBlock) throw new Error('A0 section 14 missing');
  for (const line of qBlock[1].split('\n')) {
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length < 5) continue;
    const match = cells[0].match(/^(Q\d+)$/);
    if (!match) continue;
    qRows.set(match[1], { input: cells[1], owner: cells[3], destination: cells[4] });
  }
  const outbox = new Set();
  const evidenceEvents = new Set();
  const s1017 = doc.match(/### 10\.17[\s\S]*?(?=### 10\.18)/);
  if (!s1017) throw new Error('A0 section 10.17 missing');
  for (const line of s1017[0].split('\n')) {
    if (!line.startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length < 4) continue;
    if (cells[0] === 'Event domain') continue;
    for (const match of cells[3].matchAll(/`([A-Z][A-Z0-9_]+)`/g)) outbox.add(match[1]);
  }
  const s1024 = doc.match(/### 10\.24[\s\S]*?(?=## 11\.)/);
  if (!s1024) throw new Error('A0 section 10.24 missing');
  for (const line of s1024[0].split('\n')) {
    if (!line.startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length === 0) continue;
    for (const match of cells[0].matchAll(/`([A-Z][A-Z0-9_]+)`/g)) evidenceEvents.add(match[1]);
    if (cells.length >= 3) {
      for (const match of cells[1].matchAll(/`([A-Z][A-Z0-9_]+)`/g)) outbox.add(match[1]);
    }
  }
  return {
    tables,
    invariants,
    stateRows,
    rawRows,
    bySection,
    transactions,
    qRows,
    outbox,
    evidenceEvents,
    parseStateRow
  };
}

function parseA1(bundle) {
  const operations = new Set();
  for (const pathValue of Object.values(bundle.paths || {})) {
    for (const method of ['get', 'post', 'put', 'patch', 'delete', 'head', 'options']) {
      const operation = pathValue?.[method];
      if (operation?.operationId) operations.add(operation.operationId);
    }
  }
  const problemCodes = new Set(bundle.components?.schemas?.ProblemCode?.enum || []);
  return { operations, problemCodes };
}

function parseA2(model, fixtureDir) {
  const messages = new Set((model.messages || []).map((item) => item.messageId));
  const eventTypes = new Set((model.eventTypes || []).map((item) => item.eventType));
  const identities = new Set((model.identities || []).map((item) => item.identity));
  const invariants = new Set((model.invariants || []).map((item) => item.id));
  const actions = new Set(model.actions || []);
  const reasons = new Set(model.decisionReasons || []);
  const machines = new Map();
  for (const [name, machine] of Object.entries(model.stateMachines || {})) {
    const states = [...(machine.states || [])];
    const edges = [];
    for (const transition of machine.transitions || []) {
      if (transition.from && transition.to) edges.push([transition.from, transition.to]);
    }
    machines.set(name, { states, edges: sortEdges(edges), derivedStates: [] });
  }
  if (model.recordingLifecycle) {
    const states = [...(model.recordingLifecycle.states || [])];
    const edges = [];
    for (const transition of model.recordingLifecycle.transitions || []) {
      if (transition.from && transition.to) edges.push([transition.from, transition.to]);
    }
    machines.set('recording', { states, edges: sortEdges(edges), derivedStates: [] });
  }
  const fixtures = new Set();
  for (const file of fs.readdirSync(fixtureDir)) {
    if (!file.endsWith('.json')) continue;
    const value = JSON.parse(fs.readFileSync(path.join(fixtureDir, file), 'utf8'));
    if (value.id) fixtures.add(value.id);
  }
  return {
    messages,
    eventTypes,
    identities,
    invariants,
    actions,
    reasons,
    machines,
    fixtures,
    eventRegistrySha256: model.eventRegistry?.sha256 || null
  };
}

function parseA3(model, docs17, fixtureDir) {
  const trustSurfaces = new Set((model.trustSurfaces || []).map((item) => item.surfaceId));
  const authDomains = new Set((model.authDomains || []).map((item) => item.domain));
  const trustBoundaries = new Set((model.trustBoundaries || []).map((item) => item.boundaryId));
  const serviceIdentities = new Set((model.serviceIdentities || []).map((item) => item.serviceIdentity));
  const credentialClasses = new Set((model.credentialClasses || []).map((item) => item.credentialId));
  const auditEvents = new Map((model.auditEvents || []).map((item) => [item.actionCode, item]));
  const highRisk = new Map((model.highRiskActions || []).map((item) => [item.operationId, item]));
  const callClasses = new Set((model.auditCoverage || []).map((item) => item.callClass));
  const deferredInputs = new Set((model.deferredInputs || []).map((item) => item.inputId));
  const mediaPolicies = new Set((model.mediaPolicies || []).map((item) => item.policyId));
  const invariants = new Set((model.invariants || []).map((item) => item.id));
  const keys = new Set((model.keyClasses || []).map((item) => item.keyId));
  const dataClasses = new Set((model.dataClasses || []).map((item) => item.dataClass));
  const residuals = new Map();
  const block = docs17.match(/## 20\. [^\n]*([\s\S]*?)(?=\n## |\n### |$)/);
  if (!block) throw new Error('docs/17 section 20 missing');
  for (const line of block[1].split('\n')) {
    if (!line.startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length < 3) continue;
    const match = cells[0].match(/^`(R-A3-[A-Z0-9-]+)`$/);
    if (!match) continue;
    const ownerDestination = cells[2];
    const split = ownerDestination.split('；');
    const owner = (split[0] || '').trim();
    const destination = (split.slice(1).join('；') || '').trim();
    if (residuals.has(match[1])) {
      residuals.get(match[1]).duplicate = true;
    } else {
      residuals.set(match[1], { id: match[1], content: cells[1], owner, destination });
    }
  }
  const fixtures = new Set();
  for (const file of fs.readdirSync(fixtureDir)) {
    if (!file.endsWith('.json')) continue;
    const value = JSON.parse(fs.readFileSync(path.join(fixtureDir, file), 'utf8'));
    if (value.id) fixtures.add(value.id);
  }
  return {
    trustSurfaces,
    authDomains,
    trustBoundaries,
    serviceIdentities,
    credentialClasses,
    auditEvents,
    highRisk,
    callClasses,
    deferredInputs,
    mediaPolicies,
    invariants,
    keys,
    dataClasses,
    residuals,
    fixtures,
    counts: {
      trustSurfaces: trustSurfaces.size,
      authDomains: authDomains.size,
      trustBoundaries: trustBoundaries.size,
      serviceIdentities: serviceIdentities.size,
      credentialClasses: credentialClasses.size,
      auditEvents: auditEvents.size,
      highRiskActions: highRisk.size,
      invariants: invariants.size,
      deferredInputs: deferredInputs.size,
      mediaPolicies: mediaPolicies.size,
      fixtures: fixtures.size,
      residuals: residuals.size
    }
  };
}

async function parseA4(calcPath, templatePath, fixtureDir, docs18, closeoutDoc) {
  const calc = await import(pathToFileURL(calcPath).href);
  const template = JSON.parse(fs.readFileSync(templatePath, 'utf8'));
  const result = calc.compute(template);
  const formulas = new Set();
  const strings = new Set();
  const visit = (value) => {
    if (Array.isArray(value)) {
      for (const item of value) visit(item);
      return;
    }
    if (isObject(value)) {
      if (typeof value.formulaId === 'string') formulas.add(value.formulaId);
      if (Array.isArray(value.formulaIds)) {
        for (const formulaId of value.formulaIds) {
          if (typeof formulaId === 'string') formulas.add(formulaId);
        }
      }
      for (const item of Object.values(value)) visit(item);
    } else if (typeof value === 'string') {
      strings.add(value);
    }
  };
  visit(result);
  const flags = new Set(Array.isArray(result.flags) ? result.flags : []);
  const constants = {};
  for (const name of ['productionReady', 'procurementReady', 'cloudVerified', 'a3RiskClosed']) {
    if (Object.prototype.hasOwnProperty.call(result, name)) constants[name] = result[name];
  }
  const sections = new Set();
  for (const line of docs18.split('\n')) {
    const match = line.match(/^#{2,4} (\d+(?:\.\d+)*)\.?(?:\s|$)/);
    if (match) sections.add(match[1]);
  }
  const capabilityBlock = docs18.match(/### 10\.1[\s\S]*?(?=### 10\.2)/);
  if (!capabilityBlock) throw new Error('A4 section 10.1 missing');
  const capabilities = new Set();
  for (const match of capabilityBlock[0].matchAll(/`(CV-[1-5]|regionAzSku)`/g)) capabilities.add(match[1]);
  const residualBlock = docs18.match(/### 10\.3[\s\S]*?(?=### 10\.4)/);
  if (!residualBlock) throw new Error('A4 section 10.3 missing');
  const residualRows = [];
  for (const line of residualBlock[0].split('\n')) {
    if (!line.startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length < 2) continue;
    if (cells[0].startsWith('---') || cells[0] === '残留/输入') continue;
    residualRows.push({ index: residualRows.length + 1, label: cells[0], ownerDestination: cells[1] });
  }
  const closeoutBlock = closeoutDoc.match(/## Residual risks and destinations([\s\S]*?)(?=\n## |$)/);
  if (!closeoutBlock) throw new Error('A4 closeout residual table missing');
  const closeoutRows = [];
  for (const line of closeoutBlock[1].split('\n')) {
    if (!line.startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length < 3) continue;
    if (cells[0].startsWith('---') || cells[0] === 'Residual') continue;
    closeoutRows.push({ index: closeoutRows.length + 1, residual: cells[0], owner: cells[1], destination: cells[2] });
  }
  const fixtures = new Set();
  for (const file of fs.readdirSync(fixtureDir)) {
    if (!file.endsWith('.json')) continue;
    const value = JSON.parse(fs.readFileSync(path.join(fixtureDir, file), 'utf8'));
    // A4 stage-1 fixtures identify themselves with `id`; S2 fixtures use `fixture`.
    const id = typeof value.fixture === 'string' ? value.fixture : value.id;
    if (typeof id === 'string') fixtures.add(id);
  }
  return {
    groups: new Set(calc.S2_GROUP_IDS || []),
    formulas,
    flags,
    strings,
    constants,
    capabilities,
    sections,
    residualRows,
    closeoutRows,
    fixtures,
    result,
    counts: {
      groups: (calc.S2_GROUP_IDS || []).length,
      fixtures: fixtures.size,
      capabilities: capabilities.size,
      residualRows: residualRows.length,
      closeoutRows: closeoutRows.length
    }
  };
}

function parseDoc12(doc) {
  const sections = new Set();
  for (const line of doc.split('\n')) {
    const match = line.match(/^#{2,4} (\d+(?:\.\d+)*)\.?(?:\s|$)/);
    if (match) sections.add(match[1]);
  }
  return {
    sections,
    categories: new Set(['IVR', 'TRANSACTION', 'EVIDENCE', 'SHARING', 'ASYNC', 'EXTERNAL_DEPENDENCY'])
  };
}

function buildContext(sources) {
  return sources;
}

function machineInfo(ctx, targetRef, cache) {
  if (cache.has(targetRef)) return cache.get(targetRef);
  let info = null;
  if (targetRef.startsWith('A0:SM:')) {
    const aggregate = targetRef.slice('A0:SM:'.length);
    const row = ctx.a0.stateRows.get(aggregate);
    if (row) {
      info = {
        kind: 'machine',
        name: aggregate,
        states: new Set(row.states),
        derivedStates: new Set(row.derivedStates),
        edges: new Set(row.edges.map(([from, to]) => `${from}\u0000${to}`)),
        edgeList: row.edges,
        emptyEdge: row.edges.length === 0,
        classification: row.classification
      };
    }
  } else if (targetRef.startsWith('A2:STATE:')) {
    const name = targetRef.slice('A2:STATE:'.length);
    const machine = ctx.a2.machines.get(name);
    if (machine) {
      info = {
        kind: 'machine',
        name,
        states: new Set(machine.states),
        derivedStates: new Set(machine.derivedStates || []),
        edges: new Set(machine.edges.map(([from, to]) => `${from}\u0000${to}`)),
        edgeList: machine.edges,
        emptyEdge: machine.edges.length === 0,
        classification: machine.edges.length === 0 ? 'EMPTY_EDGE' : 'STANDARD'
      };
    }
  } else if (targetRef.startsWith('A0:TABLE:')) {
    const table = targetRef.slice('A0:TABLE:'.length);
    const row = ctx.a0.tables.get(table);
    if (row) info = { kind: 'table', name: table, columns: row.columns };
  }
  cache.set(targetRef, info);
  return info;
}

function machineTerminals(info) {
  if (!info || info.kind !== 'machine' || info.states.size === 0) return [];
  const outgoing = new Set(info.edgeList.map(([from]) => from));
  return [...info.states].filter((state) => !outgoing.has(state)).sort();
}

function resolveRef(ctx, ref) {
  const legacy = LEGACY_MARKERS.some((marker) => ref.includes(marker));
  if (legacy) return { status: 'legacy' };
  const prefixMatch = ref.match(/^(A0|A1|A2|A3|A4|DOC12|DOC14):/);
  if (!prefixMatch) return { status: 'invalid' };
  const parts = ref.split('#')[0].split(':');
  const prefix = `${parts[0]}:${parts[1]}`;
  try {
    switch (prefix) {
      case 'A0:TABLE': {
        const name = ref.slice('A0:TABLE:'.length);
        return ctx.a0.tables.has(name) ? { status: 'ok', value: ctx.a0.tables.get(name) } : { status: 'unresolved' };
      }
      case 'A0:INV': {
        const id = ref.slice('A0:INV:'.length);
        return ctx.a0.invariants.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A0:SM': {
        const rest = ref.slice('A0:SM:'.length);
        if (rest.includes('->')) {
          const arrow = rest.indexOf('->');
          const before = rest.slice(0, arrow);
          const to = rest.slice(arrow + 2);
          const split = before.lastIndexOf(':');
          if (split <= 0) return { status: 'invalid' };
          const aggregate = before.slice(0, split);
          const from = before.slice(split + 1);
          const row = ctx.a0.stateRows.get(aggregate);
          if (!row) return { status: 'unresolved' };
          if (!from || !to) return { status: 'invalid' };
          const present = row.edges.some(([a, b]) => a === from && b === to);
          return present ? { status: 'ok', value: { aggregate, from, to } } : { status: 'unresolved' };
        }
        const row = ctx.a0.stateRows.get(rest);
        return row ? { status: 'ok', value: row } : { status: 'unresolved' };
      }
      case 'A0:TX': {
        const id = ref.slice('A0:TX:'.length);
        return ctx.a0.transactions.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A0:OUTBOX': {
        const id = ref.slice('A0:OUTBOX:'.length);
        return ctx.a0.outbox.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A0:EVIDENCE-EVENT': {
        const id = ref.slice('A0:EVIDENCE-EVENT:'.length);
        return ctx.a0.evidenceEvents.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A0:Q': {
        const id = ref.slice('A0:Q:'.length);
        return ctx.a0.qRows.has(id) ? { status: 'ok', value: ctx.a0.qRows.get(id) } : { status: 'unresolved' };
      }
      case 'A1:OP': {
        const id = ref.slice('A1:OP:'.length);
        return ctx.a1.operations.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A1:PROBLEM': {
        const id = ref.slice('A1:PROBLEM:'.length);
        return ctx.a1.problemCodes.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:MSG': {
        const id = ref.slice('A2:MSG:'.length);
        return ctx.a2.messages.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:EVENT': {
        const id = ref.slice('A2:EVENT:'.length);
        return ctx.a2.eventTypes.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:IDENTITY': {
        const id = ref.slice('A2:IDENTITY:'.length);
        return ctx.a2.identities.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:INV': {
        const id = ref.slice('A2:INV:'.length);
        return ctx.a2.invariants.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:ACTION': {
        const id = ref.slice('A2:ACTION:'.length);
        return ctx.a2.actions.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:REASON': {
        const id = ref.slice('A2:REASON:'.length);
        return ctx.a2.reasons.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:PROCESSING': {
        const id = ref.slice('A2:PROCESSING:'.length);
        return PROCESSING.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A2:STATE': {
        const rest = ref.slice('A2:STATE:'.length);
        if (rest.includes('->')) {
          const arrow = rest.indexOf('->');
          const before = rest.slice(0, arrow);
          const to = rest.slice(arrow + 2);
          const split = before.lastIndexOf(':');
          if (split <= 0) return { status: 'invalid' };
          const name = before.slice(0, split);
          const from = before.slice(split + 1);
          const machine = ctx.a2.machines.get(name);
          if (!machine) return { status: 'unresolved' };
          const present = machine.edges.some(([a, b]) => a === from && b === to);
          return present ? { status: 'ok', value: { machine: name, from, to } } : { status: 'unresolved' };
        }
        return ctx.a2.machines.has(rest)
          ? { status: 'ok', value: ctx.a2.machines.get(rest) }
          : { status: 'unresolved' };
      }
      case 'A2:FIX': {
        const id = ref.slice('A2:FIX:'.length);
        return ctx.a2.fixtures.has(id) ? { status: 'ok', value: id } : { status: 'unresolved' };
      }
      case 'A3:TS': return ctx.a3.trustSurfaces.has(ref.slice(6)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:TB': return ctx.a3.trustBoundaries.has(ref.slice(6)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:AUTH': return ctx.a3.authDomains.has(ref.slice(8)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:SERVICE': return ctx.a3.serviceIdentities.has(ref.slice(11)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:CRED': return ctx.a3.credentialClasses.has(ref.slice(8)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:INV': return ctx.a3.invariants.has(ref.slice(7)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:KEY': return ctx.a3.keys.has(ref.slice(7)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:DATA': return ctx.a3.dataClasses.has(ref.slice(8)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:CV': return ctx.a3.deferredInputs.has(ref.slice(6)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:MEDIA': return ctx.a3.mediaPolicies.has(ref.slice(9)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:AUDIT': return ctx.a3.auditEvents.has(ref.slice(9)) ? { status: 'ok', value: ctx.a3.auditEvents.get(ref.slice(9)) } : { status: 'unresolved' };
      case 'A3:HIGH-RISK': return ctx.a3.highRisk.has(ref.slice(13)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:COVERAGE': return ctx.a3.callClasses.has(ref.slice(12)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:DEFERRED': return ctx.a3.deferredInputs.has(ref.slice(12)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A3:RESIDUAL': {
        const id = ref.slice('A3:RESIDUAL:'.length);
        const residual = ctx.a3.residuals.get(id);
        if (!residual) return { status: 'unresolved' };
        return residual.duplicate ? { status: 'ambiguous' } : { status: 'ok', value: residual };
      }
      case 'A3:FIX': return ctx.a3.fixtures.has(ref.slice(7)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A4:GROUP': return ctx.a4.groups.has(ref.slice(9)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A4:FORMULA': return ctx.a4.formulas.has(ref.slice(11)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A4:FIX': return ctx.a4.fixtures.has(ref.slice(7)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A4:CONSTANT': return Object.prototype.hasOwnProperty.call(ctx.a4.constants, ref.slice(12))
        ? { status: 'ok', value: ctx.a4.constants[ref.slice(12)] } : { status: 'unresolved' };
      case 'A4:FLAG': return ctx.a4.flags.has(ref.slice(9)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A4:CAPABILITY': return ctx.a4.capabilities.has(ref.slice(14)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A4:SECTION': return ctx.a4.sections.has(ref.slice(11)) ? { status: 'ok' } : { status: 'unresolved' };
      case 'A4:RESIDUAL': {
        const index = Number(ref.slice(12));
        return ctx.a4.residualRows.some((row) => row.index === index) ? { status: 'ok' } : { status: 'unresolved' };
      }
      case 'A4:CLOSEOUT': {
        const index = Number(ref.slice(12));
        return ctx.a4.closeoutRows.some((row) => row.index === index) ? { status: 'ok' } : { status: 'unresolved' };
      }
      case 'DOC12:S12': {
        const category = ref.split('#')[1];
        return category && ctx.doc12.categories.has(category) && ctx.doc12.sections.has('12')
          ? { status: 'ok' } : { status: 'unresolved' };
      }
      case 'DOC12:S13': return ctx.doc12.sections.has('13') ? { status: 'ok' } : { status: 'unresolved' };
      case 'DOC12:S17.1': return ctx.doc12.sections.has('17.1') ? { status: 'ok' } : { status: 'unresolved' };
      case 'DOC12:S18': return ctx.doc12.sections.has('18') ? { status: 'ok' } : { status: 'unresolved' };
      case 'DOC14:S14': return ctx.a0.qRows.size > 0 ? { status: 'ok' } : { status: 'unresolved' };
      default:
        return { status: 'invalid' };
    }
  } catch {
    return { status: 'invalid' };
  }
}

function externalFieldAllowed(field) {
  if (!isString(field)) return false;
  if (EXTERNAL_FIELDS.has(field)) return true;
  return field.startsWith('counts.') && field.length > 'counts.'.length;
}
function invalidBasisRefs(basisRefs) {
  if (!Array.isArray(basisRefs)) return [];
  return basisRefs.filter((ref) =>
    !isString(ref) || ref.trim().length === 0 || !BASIS_REF_SHAPE.test(ref));
}
// Closed ExternalAssertion contract: fixed field set, fixed op set, valueRef must
// be a resolvable source ref, and a compared value is required for EQUALS/UNCHANGED.
function validateExternalAssertion(ctx, assertion, id, where, issues) {
  const report = (code, value, path) => root(issues, code, value, `${where}.${path}`);
  if (!isObject(assertion)) {
    report('A5_SCENARIO_FIELD_INVALID', id, '');
    return false;
  }
  let ok = true;
  if (!isString(assertion.operationRef)) {
    report('A5_SCENARIO_FIELD_MISSING', id, 'operationRef');
    ok = false;
  }
  if (!Object.prototype.hasOwnProperty.call(assertion, 'field')) {
    report('A5_SCENARIO_FIELD_MISSING', id, 'field');
    ok = false;
  } else if (assertion.field === null) {
    report('A5_SCENARIO_FIELD_INVALID', id, 'field');
    ok = false;
  } else if (!externalFieldAllowed(assertion.field)) {
    report('A5_SCENARIO_FIELD_INVALID', assertion.field, 'field');
    ok = false;
  }
  if (!Object.prototype.hasOwnProperty.call(assertion, 'op')) {
    report('A5_SCENARIO_FIELD_MISSING', id, 'op');
    ok = false;
  } else if (!EXTERNAL_OPS.has(assertion.op)) {
    report('A5_SCENARIO_FIELD_INVALID', assertion.op, 'op');
    ok = false;
  }
  if (!Array.isArray(assertion.basisRefs) || assertion.basisRefs.length === 0) {
    report('A5_SCENARIO_FIELD_MISSING', id, 'basisRefs');
    ok = false;
  } else {
    for (const ref of invalidBasisRefs(assertion.basisRefs)) {
      report('A5_SOURCE_REF_INVALID', isString(ref) ? ref : '?', 'basisRefs');
      ok = false;
    }
  }
  const needsValue = assertion.op === 'EQUALS' || assertion.op === 'UNCHANGED';
  if (needsValue && !Object.prototype.hasOwnProperty.call(assertion, 'valueRef')) {
    report('A5_SCENARIO_FIELD_MISSING', id, 'valueRef');
    ok = false;
  } else if (Object.prototype.hasOwnProperty.call(assertion, 'valueRef')) {
    const valueRef = assertion.valueRef;
    if (!isString(valueRef) || !/^(A0|A1|A2|A3|A4|DOC12|DOC14):/.test(valueRef)) {
      report('A5_SOURCE_REF_INVALID', valueRef === undefined || valueRef === null ? '?' : valueRef, 'valueRef');
      ok = false;
    } else {
      const resolved = resolveRef(ctx, valueRef);
      if (resolved.status === 'legacy') {
        report('A5_LEGACY_REFERENCE_FORBIDDEN', valueRef, 'valueRef');
        ok = false;
      } else if (resolved.status === 'invalid') {
        report('A5_SOURCE_REF_INVALID', valueRef, 'valueRef');
        ok = false;
      } else if (resolved.status === 'unresolved') {
        report('A5_SOURCE_REF_UNRESOLVED', valueRef, 'valueRef');
        ok = false;
      } else if (resolved.status === 'ambiguous') {
        report('A5_SOURCE_REF_AMBIGUOUS', valueRef, 'valueRef');
        ok = false;
      } else if (resolved.status !== 'ok') {
        report('A5_SCENARIO_FIELD_INVALID', valueRef, 'valueRef');
        ok = false;
      }
    }
  }
  return ok;
}

function collectRefs(value, out = [], key = '') {
  if (typeof value === 'string') {
    // transitionRef is validated by the dedicated state-machine phase so that
    // unknown-edge/derived-endpoint forms surface as A5_TRANSITION_REF_INVALID.
    if (key !== 'transitionRef' &&
        (/^(A0|A1|A2|A3|A4|DOC12|DOC14):/.test(value) || LEGACY_MARKERS.some((marker) => value.includes(marker)))) {
      out.push(value);
    }
  } else if (Array.isArray(value)) {
    for (const item of value) collectRefs(item, out);
  } else if (isObject(value)) {
    for (const [childKey, item] of Object.entries(value)) collectRefs(item, out, childKey);
  }
  return out;
}

function collectArraysForDuplicates(value, out = [], keyPath = '') {
  if (Array.isArray(value)) {
    const strings = value.filter((item) => typeof item === 'string');
    if (strings.length > 0) out.push({ path: keyPath, strings });
    for (let i = 0; i < value.length; i += 1) collectArraysForDuplicates(value[i], out, `${keyPath}[${i}]`);
  } else if (isObject(value)) {
    for (const [key, item] of Object.entries(value)) {
      collectArraysForDuplicates(item, out, keyPath ? `${keyPath}.${key}` : key);
    }
  }
  return out;
}

function validateRoot(ctx, model, p) {
  const allowedRoot = new Set([
    'modelVersion', 'matrixId', 'specAsOf', 'axes', 'coverageUnits',
    'notApplicableReasons', 'owners', 'executionTiers', 'coverage', 'scenarios',
    'residuals', 'sourceBaseline', 'reasonRegistry', 'acceptanceStateMachine',
    'evidencePolicy', 'authorityRegistry'
  ]);
  if (!isObject(model)) {
    root(p, 'A5_MODEL_INVALID', '', 'model');
    return;
  }
  const prohibitedCaller = ['productionAccepted', 'accepted', 'result', 'pass', 'execution'];
  for (const key of prohibitedCaller) {
    if (Object.prototype.hasOwnProperty.call(model, key)) {
      root(p, 'A5_CALLER_CONTROL_FORBIDDEN', key, `model.${key}`);
    }
  }
  for (const key of ['productionReady', 'procurementReady', 'cloudVerified', 'a3RiskClosed']) {
    if (Object.prototype.hasOwnProperty.call(model, key)) {
      root(p, 'A5_PROHIBITED_READINESS_FLAG', key, `model.${key}`);
    }
  }
  for (const key of Object.keys(model)) {
    if (!allowedRoot.has(key) && !prohibitedCaller.includes(key) &&
        !['productionReady', 'procurementReady', 'cloudVerified', 'a3RiskClosed'].includes(key)) {
      root(p, 'A5_SECTION_EXTRA', key, `model.${key}`);
    }
  }
  if (model.modelVersion !== 1) root(p, 'A5_MODEL_INVALID', 'modelVersion', 'model.modelVersion');
  if (!isString(model.matrixId)) root(p, 'A5_MODEL_MISSING', 'matrixId', 'model.matrixId');
  if (!isString(model.specAsOf)) root(p, 'A5_MODEL_MISSING', 'specAsOf', 'model.specAsOf');
  for (const key of ['axes', 'coverageUnits', 'notApplicableReasons', 'owners', 'executionTiers',
    'coverage', 'scenarios', 'residuals', 'reasonRegistry']) {
    if (!Array.isArray(model[key])) {
      if (model[key] === undefined) root(p, 'A5_SECTION_MISSING', key, `model.${key}`);
      else root(p, 'A5_MODEL_INVALID', key, `model.${key}`);
    }
  }
  if (!isObject(model.sourceBaseline)) root(p, 'A5_SECTION_MISSING', 'sourceBaseline', 'model.sourceBaseline');
  if (!isObject(model.acceptanceStateMachine)) root(p, 'A5_SECTION_MISSING', 'acceptanceStateMachine', 'model.acceptanceStateMachine');
  if (Array.isArray(model.axes)) {
    const axes = model.axes.map((item) => item.axis);
    if (axes.length !== AXES.length || AXES.some((axis) => !axes.includes(axis))) {
      root(p, 'A5_UNKNOWN_AXIS', 'axes', 'model.axes');
    }
  }
}

function validateIds(model, p) {
  const seen = new Map();
  const dup = (value, label) => {
    if (seen.has(`${label}:${value}`)) root(p, 'A5_DUPLICATE_ID', value, label);
    else seen.set(`${label}:${value}`, true);
  };
  for (const axis of model.axes || []) dup(axis.axis, 'axis');
  for (const unit of model.coverageUnits || []) dup(unit.unitId, 'unit');
  for (const scenario of model.scenarios || []) dup(scenario.scenarioId, 'scenario');
  for (const residual of model.residuals || []) dup(residual.residualId, 'residual');
  for (const owner of model.owners || []) dup(owner.ownerId, 'owner');
  for (const row of model.coverage || []) dup(`${row.unitId}|${row.axis}`, 'coverage');
  const baselineKeys = Object.keys(model.sourceBaseline || {});
  const baselineSeen = new Set();
  for (const key of baselineKeys) {
    if (baselineSeen.has(key)) root(p, 'A5_DUPLICATE_ID', key, 'sourceBaseline');
    baselineSeen.add(key);
  }
}

function validateRequired(model, p) {
  const scenarios = Array.isArray(model.scenarios) ? model.scenarios : [];
  const units = new Set(UNITS);
  const axes = new Set(AXES);
  const required = (obj, key, code, id, base) => {
    if (!Object.prototype.hasOwnProperty.call(obj, key)) root(p, code, id, `${base}.${key}`);
    else if (obj[key] === null) root(p, 'A5_SCENARIO_FIELD_INVALID', id, `${base}.${key}`);
  };
  for (const unit of model.coverageUnits || []) {
    if (!units.has(unit.unitId)) root(p, 'A5_UNKNOWN_UNIT', unit.unitId, 'coverageUnits');
    if (!isString(unit.leadOwner) || !isString(unit.leadService) || !isString(unit.kind)) {
      root(p, 'A5_SECTION_MISSING', unit.unitId, 'coverageUnits');
    }
  }
  for (const row of model.coverage || []) {
    if (!units.has(row.unitId)) root(p, 'A5_UNKNOWN_UNIT', row.unitId, 'coverage');
    if (!axes.has(row.axis)) root(p, 'A5_UNKNOWN_AXIS', row.axis, 'coverage');
    if (!['REPRESENTED', 'NOT_APPLICABLE', 'DEFERRED'].includes(row.state)) {
      root(p, 'A5_COVERAGE_CELL_ILLEGAL', `${row.unitId}/${row.axis}`, 'coverage');
    }
  }
  for (const scenario of scenarios) {
    const base = `scenarios.${scenario.scenarioId || '?'}`;
    if (isString(scenario.scenarioId) && !SCENARIO_ID_RE.test(scenario.scenarioId)) {
      root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.scenarioId`);
    }
    for (const key of [
      'scenarioId', 'coverageUnitId', 'axis', 'title', 'owner', 'aggregateRef', 'service',
      'requiredEvidenceTiers', 'sourceRefs', 'outcomeRefs', 'preconditions', 'stimulus',
      'expected', 'evidenceOwner', 'resultSemantics', 'residualIds'
    ]) {
      required(scenario, key, key === 'evidenceOwner' ? 'A5_EVIDENCE_OWNER_MISSING' : 'A5_SCENARIO_FIELD_MISSING',
        scenario.scenarioId, base);
    }
    if (scenario.evidenceOwner === null) root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.evidenceOwner`);
    if (!axes.has(scenario.axis)) root(p, 'A5_UNKNOWN_AXIS', scenario.axis, `${base}.axis`);
    if (!units.has(scenario.coverageUnitId)) root(p, 'A5_UNKNOWN_UNIT', scenario.coverageUnitId, `${base}.coverageUnitId`);
    if (typeof scenario.title !== 'string' || scenario.title.length === 0) {
      root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.title`);
    }
    if (!Array.isArray(scenario.requiredEvidenceTiers) || scenario.requiredEvidenceTiers.length === 0) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.requiredEvidenceTiers`);
    } else {
      for (const tier of scenario.requiredEvidenceTiers) {
        if (!EXECUTION_TIERS.includes(tier)) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', tier, `${base}.requiredEvidenceTiers`);
        }
      }
      if (new Set(scenario.requiredEvidenceTiers).size !== scenario.requiredEvidenceTiers.length) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.requiredEvidenceTiers`);
      }
    }
    if (!Array.isArray(scenario.sourceRefs) || scenario.sourceRefs.length < 2) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.sourceRefs`);
    }
    if (!Array.isArray(scenario.outcomeRefs) || scenario.outcomeRefs.length < 1) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.outcomeRefs`);
    }
    if (!Array.isArray(scenario.residualIds)) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.residualIds`);
    }
    if (!isObject(scenario.preconditions)) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.preconditions`);
    } else {
      for (const key of ['startingState', 'fixtureRefs', 'environmentRefs', 'assumptionRefs']) {
        if (!Array.isArray(scenario.preconditions[key])) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.preconditions.${key}`);
        }
      }
    }
    if (!isObject(scenario.stimulus)) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.stimulus`);
    } else {
      if (!Object.values(ORDERING_BY_AXIS).some((allowed) => allowed.has(scenario.stimulus.orderingClass))) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.stimulus.orderingClass`);
      }
      if (!Array.isArray(scenario.stimulus.steps) || scenario.stimulus.steps.length === 0) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.stimulus.steps`);
      } else {
        const stepIds = new Set();
        for (let index = 0; index < scenario.stimulus.steps.length; index += 1) {
          const step = scenario.stimulus.steps[index];
          const stepBase = `${base}.stimulus.steps[${index}]`;
          if (!isObject(step)) {
            root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, stepBase);
            continue;
          }
          for (const key of ['stepId', 'actorRef', 'identityRefs']) {
            if (!Object.prototype.hasOwnProperty.call(step, key)) {
              root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${stepBase}.${key}`);
            }
          }
          if (!isString(step.stepId)) root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${stepBase}.stepId`);
          else if (stepIds.has(step.stepId)) root(p, 'A5_DUPLICATE_ID', step.stepId, `${stepBase}.stepId`);
          else stepIds.add(step.stepId);
          if (!Array.isArray(step.identityRefs)) {
            root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${stepBase}.identityRefs`);
          }
          for (const key of ['expectedState', 'expectedDelta']) {
            if (step[key] !== undefined && typeof step[key] !== (key === 'expectedState' ? 'object' : 'object')) {
              root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${stepBase}.${key}`);
            }
          }
          if (step.expectedState !== undefined && !Array.isArray(step.expectedState)) {
            root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${stepBase}.expectedState`);
          }
          if (step.expectedResponse !== undefined && !isObject(step.expectedResponse)) {
            root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${stepBase}.expectedResponse`);
          }
        }
      }
      if (!isObject(scenario.stimulus.faultInjection) || !isString(scenario.stimulus.faultInjection.kind)) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.stimulus.faultInjection`);
      }
    }
    if (!isObject(scenario.expected)) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected`);
    } else {
      for (const key of ['persisted', 'external', 'terminalAbsorption', 'idempotency', 'effects', 'evidence', 'cleanup']) {
        if (!Object.prototype.hasOwnProperty.call(scenario.expected, key)) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.${key}`);
        }
      }
      if (Array.isArray(scenario.expected.persisted) && scenario.expected.persisted.length === 0) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.persisted`);
      }
      if (Array.isArray(scenario.expected.external) && scenario.expected.external.length === 0) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.external`);
      }
      if (isObject(scenario.expected.terminalAbsorption)) {
        if (!Array.isArray(scenario.expected.terminalAbsorption.terminalStates)) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.terminalAbsorption.terminalStates`);
        }
        if (!['ABSORB', 'IGNORE', 'QUARANTINE', 'REJECT'].includes(scenario.expected.terminalAbsorption.postTerminalOutcome)) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.expected.terminalAbsorption.postTerminalOutcome`);
        }
        if (!Array.isArray(scenario.expected.terminalAbsorption.sourceRefs)) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.terminalAbsorption.sourceRefs`);
        }
      }
      if (isObject(scenario.expected.idempotency)) {
        for (const key of ['identityRef', 'sameFingerprintOutcome', 'changedFingerprintOutcome', 'sourceRefs']) {
          if (!Object.prototype.hasOwnProperty.call(scenario.expected.idempotency, key)) {
            root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.idempotency.${key}`);
          }
        }
      }
      if (isObject(scenario.expected.effects)) {
        for (const key of ['baseline', 'steps', 'overall', 'zeroOrBoundedBasisRefs']) {
          if (!Object.prototype.hasOwnProperty.call(scenario.expected.effects, key)) {
            root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.effects.${key}`);
          }
        }
      }
      if (isObject(scenario.expected.evidence)) {
        for (const key of ['audit', 'outbox', 'observability', 'logs', 'accessEvents', 'zeroBasisRefs']) {
          if (!Array.isArray(scenario.expected.evidence[key])) {
            root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.evidence.${key}`);
          }
        }
      }
      if (isObject(scenario.expected.cleanup)) {
        for (const key of ['mode', 'steps', 'sourceRefs']) {
          if (!Object.prototype.hasOwnProperty.call(scenario.expected.cleanup, key)) {
            root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.expected.cleanup.${key}`);
          }
        }
        if (!['NONE', 'IDEMPOTENT_REPLAY', 'AUTHORITATIVE_RECONCILIATION', 'NEW_IDENTITY_REQUIRES_REF', 'MANUAL_RESTORE']
          .includes(scenario.expected.cleanup.mode)) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.expected.cleanup.mode`);
        }
      }
    }
    if (scenario.axis === 'DUPLICATE') {
      if (!isObject(scenario.duplicateReplay)) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.duplicateReplay`);
      }
    }
    if (isObject(scenario.duplicateReplay)) {
      if (!Object.prototype.hasOwnProperty.call(scenario.duplicateReplay, 'responseOutcome')) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.duplicateReplay.responseOutcome`);
      } else if (!['FIRST_RESULT', 'CONFLICT'].includes(scenario.duplicateReplay.responseOutcome)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.duplicateReplay.responseOutcome, `${base}.duplicateReplay.responseOutcome`);
      }
    }
    if (!isObject(scenario.resultSemantics)) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.resultSemantics`);
    } else {
      const allowed = new Set(['specResult', 'runtimeResult', 'acceptanceRuleRef', 'failConditionRefs']);
      for (const key of Object.keys(scenario.resultSemantics)) {
        if (!allowed.has(key)) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.resultSemantics.${key}`);
        }
      }
      for (const key of allowed) {
        if (!Object.prototype.hasOwnProperty.call(scenario.resultSemantics, key)) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.resultSemantics.${key}`);
        }
      }
      if (scenario.resultSemantics.specResult !== 'SPEC_DEFINED') {
        root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.resultSemantics.specResult`);
      }
      if (scenario.resultSemantics.runtimeResult !== 'NOT_RUN') {
        root(p, 'A5_CALLER_CONTROL_FORBIDDEN', scenario.scenarioId, `${base}.resultSemantics.runtimeResult`);
      }
      if (!ACCEPTANCE_RULES.has(scenario.resultSemantics.acceptanceRuleRef)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.resultSemantics.acceptanceRuleRef`);
      }
      if (!Array.isArray(scenario.resultSemantics.failConditionRefs) ||
          scenario.resultSemantics.failConditionRefs.length === 0) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', scenario.scenarioId, `${base}.resultSemantics.failConditionRefs`);
      }
    }
    const allowedScenarioKeys = new Set([
      'scenarioId', 'coverageUnitId', 'axis', 'title', 'owner', 'aggregateRef', 'service',
      'requiredEvidenceTiers', 'sourceRefs', 'outcomeRefs', 'preconditions', 'stimulus',
      'expected', 'duplicateReplay', 'evidenceOwner', 'acceptanceAuthorityRef',
      'resultSemantics', 'residualIds'
    ]);
    for (const key of Object.keys(scenario)) {
      if (!allowedScenarioKeys.has(key)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.scenarioId, `${base}.${key}`);
      }
    }
  }
  for (const owner of model.owners || []) {
    if (!isString(owner.ownerId) || !isString(owner.kind) || !Array.isArray(owner.upstream)) {
      root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', owner.ownerId || '?', 'owners');
    }
    if (owner.acceptanceOnly !== true) {
      root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', owner.ownerId || '?', 'owners.acceptanceOnly');
    }
    if (!['MODULE', 'RESPONSIBILITY', 'DELEGATED_SET'].includes(owner.kind)) {
      root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', owner.ownerId || '?', 'owners.kind');
    }
  }
  for (const residual of model.residuals || []) {
    const id = residual.residualId || '?';
    if (!isString(residual.residualId)) root(p, 'A5_MODEL_INVALID', id, 'residuals');
    if (!isString(residual.owner)) root(p, 'A5_RESIDUAL_OWNER_MISSING', id, 'residuals.owner');
    if (!isString(residual.destination)) root(p, 'A5_RESIDUAL_DESTINATION_MISSING', id, 'residuals.destination');
    if (!Array.isArray(residual.upstreamRefs) || residual.upstreamRefs.length === 0) {
      root(p, 'A5_SOURCE_REF_UNRESOLVED', id, 'residuals.upstreamRefs');
    }
  }
}

function refsFromCoverage(row) {
  return Array.isArray(row.sourceRefs) ? row.sourceRefs : [];
}

function validateRefs(ctx, model, p) {
  const resolveRefs = (refs, where, id) => {
    for (const ref of refs) {
      const result = resolveRef(ctx, ref);
      if (result.status === 'legacy') root(p, 'A5_LEGACY_REFERENCE_FORBIDDEN', ref, `${where} (${id})`);
      else if (result.status === 'invalid') root(p, 'A5_SOURCE_REF_INVALID', ref, `${where} (${id})`);
      else if (result.status === 'unresolved') root(p, 'A5_SOURCE_REF_UNRESOLVED', ref, `${where} (${id})`);
      else if (result.status === 'ambiguous') root(p, 'A5_SOURCE_REF_AMBIGUOUS', ref, `${where} (${id})`);
    }
  };
  const duplicates = (obj, where, id) => {
    for (const entry of collectArraysForDuplicates(obj)) {
      const seen = new Set();
      for (const ref of entry.strings) {
        if (!/^(A0|A1|A2|A3|A4|DOC12|DOC14):/.test(ref) && !LEGACY_MARKERS.some((marker) => ref.includes(marker))) continue;
        if (seen.has(ref)) root(p, 'A5_SOURCE_REF_DUPLICATE', ref, `${where}.${entry.path} (${id})`);
        seen.add(ref);
      }
    }
  };
  for (const row of model.coverage || []) {
    if (row.state === 'NOT_APPLICABLE') resolveRefs(refsFromCoverage(row), 'coverage.sourceRefs', `${row.unitId}/${row.axis}`);
  }
  for (const residual of model.residuals || []) {
    resolveRefs(Array.isArray(residual.upstreamRefs) ? residual.upstreamRefs : [], 'residuals.upstreamRefs', residual.residualId);
  }
  for (const owner of model.owners || []) {
    for (const upstream of owner.upstream || []) {
      resolveRefs([upstream.ref], 'owners.upstream', owner.ownerId);
    }
  }
  for (const scenario of model.scenarios || []) {
    const id = scenario.scenarioId;
    duplicates(scenario, `scenarios.${id}`, id);
    const refs = collectRefs(scenario);
    resolveRefs(refs, 'scenario', id);
    const outcome = new Set(Array.isArray(scenario.outcomeRefs) ? scenario.outcomeRefs : []);
    for (const ref of outcome) {
      if (!Array.isArray(scenario.sourceRefs) || !scenario.sourceRefs.includes(ref)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', ref, `scenarios.${id}.outcomeRefs`);
      }
    }
    const sourceRefs = Array.isArray(scenario.sourceRefs) ? scenario.sourceRefs : [];
    if (scenario.coverageUnitId === 'FAM-DEPLOYMENT' || scenario.coverageUnitId === 'PATH-DEPLOY-ACCEPTANCE') {
      if (!sourceRefs.some((ref) => ref.startsWith('A4:')) || !sourceRefs.includes('DOC12:S13')) {
        root(p, 'A5_SOURCE_REF_UNRESOLVED', id, `scenarios.${id}.sourceRefs`);
      }
    } else {
      const hasState = sourceRefs.some((ref) =>
        ref.startsWith('A0:SM:') || ref.startsWith('A0:INV:') || ref.startsWith('A2:INV:') ||
        ref.startsWith('A2:STATE:') || ref.startsWith('A3:INV:'));
      const hasAnchor = sourceRefs.some((ref) =>
        ref.startsWith('A1:OP:') || ref.startsWith('A1:PROBLEM:') || ref.startsWith('A2:EVENT:') ||
        ref.startsWith('A2:FIX:') || ref.startsWith('A3:') || ref.startsWith('A4:'));
      if (!hasState || !hasAnchor) root(p, 'A5_SOURCE_REF_UNRESOLVED', id, `scenarios.${id}.sourceRefs`);
    }
  }
}

function validateOwners(ctx, model, p) {
  const ownerIds = new Set((model.owners || []).map((owner) => owner.ownerId));
  for (const owner of model.owners || []) {
    const id = owner.ownerId;
    if (owner.kind === 'MODULE') {
      if (!A0_MODULES.has(id)) {
        root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', id, 'owners.kind');
        continue;
      }
      const upstream = owner.upstream || [];
      let matched = false;
      for (const entry of upstream) {
        if (!entry.ref || !['owner', 'ownerDestination', 'rowSet', 'section'].includes(entry.field)) continue;
        const result = resolveRef(ctx, entry.ref);
        if (result.status !== 'ok') continue;
        const ownerText = upstreamOwnerText(ctx, entry.ref, entry.field);
        if (ownerText !== null && canonicalOwnerText(ownerText) === canonicalOwnerText(id)) matched = true;
      }
      if (!matched) root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', id, 'owners.upstream');
    } else if (owner.kind === 'RESPONSIBILITY') {
      const upstream = owner.upstream || [];
      let matched = false;
      for (const entry of upstream) {
        if (entry.match !== 'WHOLE_CELL' || !entry.exactText) continue;
        const result = resolveRef(ctx, entry.ref);
        if (result.status !== 'ok') continue;
        const ownerText = upstreamOwnerText(ctx, entry.ref, entry.field);
        if (ownerText === null) continue;
        if (canonicalOwnerText(entry.exactText) === canonicalOwnerText(id) &&
            canonicalOwnerText(ownerText) === canonicalOwnerText(id)) {
          matched = true;
        }
      }
      if (!matched) root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', id, 'owners.upstream');
    } else if (owner.kind === 'DELEGATED_SET') {
      if (id !== 'A5-UPSTREAM-DELEGATED') root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', id, 'owners.kind');
      const rowSet = (owner.upstream || []).find((entry) => entry.match === 'ROW_SET');
      if (!rowSet || rowSet.ref !== 'DOC14:S14') {
        root(p, 'A5_OWNER_UPSTREAM_UNRESOLVED', id, 'owners.upstream');
      } else if (ctx.a0.qRows.size !== 28) {
        root(p, 'A5_SOURCE_DRIFT', '28', 'owners.upstream');
      }
    }
  }
  const services = new Set([...A0_MODULES, ...ctx.a3.serviceIdentities]);
  for (const scenario of model.scenarios || []) {
    const id = scenario.scenarioId;
    if (scenario.owner === undefined) root(p, 'A5_EVIDENCE_OWNER_MISSING', id, `scenarios.${id}.owner`);
    else if (!ownerIds.has(scenario.owner)) root(p, 'A5_EVIDENCE_OWNER_UNKNOWN', scenario.owner, `scenarios.${id}.owner`);
    if (scenario.evidenceOwner === undefined) root(p, 'A5_EVIDENCE_OWNER_MISSING', id, `scenarios.${id}.evidenceOwner`);
    else if (!ownerIds.has(scenario.evidenceOwner)) root(p, 'A5_EVIDENCE_OWNER_UNKNOWN', scenario.evidenceOwner, `scenarios.${id}.evidenceOwner`);
    else if (scenario.evidenceOwner !== scenario.owner) root(p, 'A5_EVIDENCE_OWNER_MISMATCH', id, `scenarios.${id}.evidenceOwner`);
    if (scenario.owner === 'A5-UPSTREAM-DELEGATED' || scenario.evidenceOwner === 'A5-UPSTREAM-DELEGATED') {
      root(p, 'A5_EVIDENCE_DELEGATION_UNRESOLVED', id, `scenarios.${id}.owner`);
    }
    // deployment/capacity is plan-authorized tooling only for the two deployment units.
    const deploymentUnit = scenario.coverageUnitId === 'FAM-DEPLOYMENT' ||
      scenario.coverageUnitId === 'PATH-DEPLOY-ACCEPTANCE';
    const serviceAllowed = services.has(scenario.service) ||
      (deploymentUnit && scenario.service === 'deployment/capacity');
    if (!serviceAllowed) root(p, 'A5_SCENARIO_FIELD_INVALID', scenario.service, `scenarios.${id}.service`);
  }
  for (const residual of model.residuals || []) {
    if (!ownerIds.has(residual.owner)) root(p, 'A5_RESIDUAL_OWNER_UNKNOWN', residual.owner, `residuals.${residual.residualId}`);
  }
}

function upstreamOwnerText(ctx, ref, field) {
  if (ref.startsWith('A0:SM:')) {
    const row = ctx.a0.stateRows.get(ref.slice('A0:SM:'.length));
    return row ? row.owner : null;
  }
  if (ref.startsWith('A0:TABLE:')) {
    const table = ctx.a0.tables.get(ref.slice('A0:TABLE:'.length));
    return table ? table.owner : null;
  }
  if (ref.startsWith('A0:Q:')) {
    const row = ctx.a0.qRows.get(ref.slice('A0:Q:'.length));
    return row ? (field === 'ownerDestination' ? row.destination : row.owner) : null;
  }
  if (ref.startsWith('A3:RESIDUAL:')) {
    const row = ctx.a3.residuals.get(ref.slice('A3:RESIDUAL:'.length));
    return row ? (field === 'ownerDestination' ? `${row.owner}；${row.destination}` : row.owner) : null;
  }
  if (ref.startsWith('A4:CLOSEOUT:')) {
    const index = Number(ref.slice('A4:CLOSEOUT:'.length));
    const row = ctx.a4.closeoutRows.find((item) => item.index === index);
    return row ? (field === 'ownerDestination' ? `${row.owner} ${row.destination}` : row.owner) : null;
  }
  return null;
}

function validateCoverage(model, p) {
  const rows = Array.isArray(model.coverage) ? model.coverage : [];
  const unitIds = new Set((model.coverageUnits || []).map((unit) => unit.unitId));
  for (const unit of UNITS) if (!unitIds.has(unit)) root(p, 'A5_UNKNOWN_UNIT', unit, 'coverageUnits');
  const cells = new Map();
  for (const row of rows) {
    const key = `${row.unitId}/${row.axis}`;
    if (cells.has(key)) {
      root(p, 'A5_COVERAGE_CELL_DUPLICATE', key, 'coverage');
      return;
    }
    cells.set(key, row);
  }
  for (const unit of UNITS) {
    for (const axis of AXES) {
      const key = `${unit}/${axis}`;
      if (!cells.has(key)) {
        root(p, 'A5_COVERAGE_CELL_MISSING', key, 'coverage');
        return;
      }
    }
  }
  for (const [key, row] of cells) {
    if (!UNITS.includes(row.unitId) || !AXES.includes(row.axis)) {
      root(p, 'A5_COVERAGE_CELL_ILLEGAL', key, 'coverage');
      return;
    }
    if (row.state === 'NOT_APPLICABLE') {
      const reason = NA_REGISTRY[row.reasonCode];
      if (!reason) {
        root(p, 'A5_NA_REASON_UNKNOWN', row.reasonCode || '?', `coverage.${key}`);
        return;
      }
      if (!reason.allowed.has(`${row.unitId}:${row.axis}`)) {
        root(p, 'A5_APPLICABILITY_ILLEGAL', row.reasonCode, `coverage.${key}`);
        return;
      }
      const refs = Array.isArray(row.sourceRefs) ? row.sourceRefs : [];
      if (refs.length < 2 || !refs.every((ref) => reason.anchor.has(ref))) {
        root(p, 'A5_NA_REASON_UNBACKED', row.reasonCode, `coverage.${key}`);
        return;
      }
      if (row.scenarioId !== undefined) {
        root(p, 'A5_APPLICABILITY_ILLEGAL', row.scenarioId, `coverage.${key}`);
        return;
      }
    } else if (row.state === 'REPRESENTED') {
      if (!isString(row.scenarioId)) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', key, `coverage.${key}.scenarioId`);
        return;
      }
    } else if (row.state === 'DEFERRED') {
      root(p, 'A5_APPLICABILITY_ILLEGAL', key, `coverage.${key}`);
      return;
    }
  }
  const scenariosById = new Map((model.scenarios || []).map((scenario) => [scenario.scenarioId, scenario]));
  for (const row of cells.values()) {
    if (row.state !== 'REPRESENTED') continue;
    const scenario = scenariosById.get(row.scenarioId);
    if (!scenario) {
      root(p, 'A5_ORPHAN_SCENARIO', row.scenarioId, `coverage.${row.unitId}/${row.axis}`);
      return;
    }
    if (scenario.coverageUnitId !== row.unitId || scenario.axis !== row.axis) {
      root(p, 'A5_ORPHAN_SCENARIO', row.scenarioId, `coverage.${row.unitId}/${row.axis}`);
      return;
    }
  }
  for (const scenario of model.scenarios || []) {
    const key = `${scenario.coverageUnitId}/${scenario.axis}`;
    const row = cells.get(key);
    if (!row || row.state !== 'REPRESENTED' || row.scenarioId !== scenario.scenarioId) {
      root(p, 'A5_ORPHAN_SCENARIO', scenario.scenarioId, `coverage.${key}`);
      return;
    }
  }
}

function parseTransitionRef(ref) {
  if (ref.startsWith('A0:SM:')) {
    const rest = ref.slice('A0:SM:'.length);
    if (!rest.includes('->')) return null;
    const arrow = rest.indexOf('->');
    const before = rest.slice(0, arrow);
    const to = rest.slice(arrow + 2);
    const split = before.lastIndexOf(':');
    if (split <= 0) return null;
    return { kind: 'A0', machine: before.slice(0, split), from: before.slice(split + 1), to };
  }
  if (ref.startsWith('A2:STATE:')) {
    const rest = ref.slice('A2:STATE:'.length);
    if (!rest.includes('->')) return null;
    const arrow = rest.indexOf('->');
    const before = rest.slice(0, arrow);
    const to = rest.slice(arrow + 2);
    const split = before.lastIndexOf(':');
    if (split <= 0) return null;
    return { kind: 'A2', machine: before.slice(0, split), from: before.slice(split + 1), to };
  }
  return null;
}

function stateAssertions(scenario) {
  const list = [];
  const pre = scenario.preconditions?.startingState;
  if (Array.isArray(pre)) pre.forEach((assertion, index) => list.push({ assertion, where: 'pre', index }));
  const steps = scenario.stimulus?.steps;
  if (Array.isArray(steps)) {
    steps.forEach((step, stepIndex) => {
      if (!Array.isArray(step.expectedState)) return;
      step.expectedState.forEach((assertion, index) => list.push({ assertion, where: 'step', stepIndex, index, stepId: step.stepId }));
    });
  }
  const final = scenario.expected?.persisted;
  if (Array.isArray(final)) final.forEach((assertion, index) => list.push({ assertion, where: 'final', index }));
  return list;
}

function effectVector(entry) {
  return {
    domainWrites: Array.isArray(entry?.domainWrites) ? entry.domainWrites : [],
    audit: Array.isArray(entry?.audit) ? entry.audit : [],
    outbox: Array.isArray(entry?.outbox) ? entry.outbox : [],
    accessEvents: Array.isArray(entry?.accessEvents) ? entry.accessEvents : [],
    externalCalls: Array.isArray(entry?.externalCalls) ? entry.externalCalls : []
  };
}
// Effect table refs are A0:TABLE:<table>; the plan allowlists are bare table names.
function tableKey(ref) {
  return typeof ref === 'string' && ref.startsWith('A0:TABLE:') ? ref.slice('A0:TABLE:'.length) : ref;
}
// One quantitative interpretation: EXACT is a fixed value, BOUNDED is [min,max].
function effectQuantity(item) {
  if (item.mode === 'EXACT') {
    const value = Number(item.value || 0);
    return { min: value, max: value };
  }
  return { min: Number(item.min || 0), max: Number(item.max || 0) };
}
// A hard zero-write rule only treats BOUNDED as zero when max === 0.
function effectMax(item) {
  return effectQuantity(item).max;
}
function sumVectorEntries(vectors) {
  const maps = { domainWrites: new Map(), audit: new Map(), outbox: new Map(), accessEvents: new Map(), externalCalls: new Map() };
  const refKey = { domainWrites: 'tableRef', audit: 'auditRef', outbox: 'outboxRef', accessEvents: 'accessEventRef', externalCalls: 'operationRef' };
  for (const vector of vectors) {
    for (const category of Object.keys(maps)) {
      for (const item of effectVector(vector)[category]) {
        const ref = item[refKey[category]];
        if (!maps[category].has(ref)) maps[category].set(ref, { min: 0, max: 0 });
        const current = maps[category].get(ref);
        const quantity = effectQuantity(item);
        current.min += quantity.min;
        current.max += quantity.max;
      }
    }
  }
  return maps;
}

function validateSemantics(ctx, model, p) {
  const machineCache = new Map();
  const scenariosById = new Map((model.scenarios || []).map((scenario) => [scenario.scenarioId, scenario]));
  const coverageByCell = new Map((model.coverage || []).map((row) => [`${row.unitId}/${row.axis}`, row]));
  for (const scenario of model.scenarios || []) {
    const id = scenario.scenarioId;
    const base = `scenarios.${id}`;
    const ordering = scenario.stimulus?.orderingClass;
    if (!ORDERING_BY_AXIS[scenario.axis]?.has(ordering)) {
      root(p, 'A5_AXIS_STIMULUS_MISMATCH', id, `${base}.stimulus.orderingClass`);
      continue;
    }
    let primaryInfo = machineInfo(ctx, scenario.aggregateRef, machineCache);
    if (!primaryInfo &&
        (scenario.coverageUnitId === 'FAM-DEPLOYMENT' || scenario.coverageUnitId === 'PATH-DEPLOY-ACCEPTANCE') &&
        scenario.aggregateRef.startsWith('A4:')) {
      primaryInfo = { kind: 'external' };
    }
    if (!primaryInfo) {
      root(p, 'A5_SOURCE_REF_UNRESOLVED', scenario.aggregateRef, `${base}.aggregateRef`);
      continue;
    }
    if (primaryInfo.kind === 'machine') {
      const terminals = machineTerminals(primaryInfo);
      const declared = Array.isArray(scenario.expected.terminalAbsorption.terminalStates)
        ? [...scenario.expected.terminalAbsorption.terminalStates].sort() : null;
      if (!declared || declared.length !== terminals.length || declared.some((value, index) => value !== terminals[index])) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', id, `${base}.expected.terminalAbsorption.terminalStates`);
      }
    }
    const states = new Map();
    let aborted = false;
    const apply = (assertion, where, step, stepId) => {
      const target = assertion.targetRef;
      const info = machineInfo(ctx, target, machineCache);
      if (!info) {
        root(p, 'A5_SOURCE_REF_UNRESOLVED', target, `${base}.${where}`);
        aborted = true;
        return;
      }
      if (!Array.isArray(assertion.basisRefs) || assertion.basisRefs.length === 0) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', assertion.field, `${base}.${where}.basisRefs`);
        aborted = true;
        return;
      }
      const badBasisRefs = invalidBasisRefs(assertion.basisRefs);
      if (badBasisRefs.length > 0) {
        for (const ref of badBasisRefs) {
          root(p, 'A5_SOURCE_REF_INVALID', isString(ref) ? ref : '?', `${base}.${where}.basisRefs`);
        }
        aborted = true;
        return;
      }
      const op = assertion.op;
      if (!['EQUALS', 'UNCHANGED', 'NULL', 'NOT_NULL'].includes(op)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', op, `${base}.${where}.op`);
        aborted = true;
        return;
      }
      if (where === 'pre' && assertion.transitionRef !== undefined) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', assertion.transitionRef, `${base}.preconditions.startingState`);
        aborted = true;
        return;
      }
      if (where === 'final' && assertion.transitionRef !== undefined) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', assertion.transitionRef, `${base}.expected.persisted`);
        aborted = true;
        return;
      }
      if (op === 'UNCHANGED' && assertion.transitionRef !== undefined) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', assertion.transitionRef, `${base}.${where}`);
        aborted = true;
        return;
      }
      if (info.kind === 'table') {
        if (!info.columns.has(assertion.field)) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', assertion.field, `${base}.${where}.field`);
          aborted = true;
        }
        if (op !== 'NULL' && op !== 'NOT_NULL') {
          if (op === 'UNCHANGED' || op === 'EQUALS') {
            const key = target;
            const current = states.get(key);
            if (where === 'step') {
              if (op === 'EQUALS' && current && current.value === assertion.value) {
                root(p, 'A5_SCENARIO_FIELD_INVALID', assertion.value, `${base}.${where}`);
                aborted = true;
              }
              states.set(key, { value: assertion.value });
            } else if (where === 'final') {
              if (current && current.value !== assertion.value) {
                root(p, 'A5_STATE_SEQUENCE_MISMATCH', target, `${base}.expected.persisted`);
                aborted = true;
              }
            } else {
              states.set(key, { value: assertion.value });
            }
          }
        }
        return;
      }
      const value = assertion.value;
      if (op !== 'NULL' && op !== 'NOT_NULL') {
        if (value === undefined || value === null) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', assertion.field, `${base}.${where}.value`);
          aborted = true;
          return;
        }
        if (info.derivedStates.has(value)) {
          root(p, 'A5_TRANSITION_REF_INVALID', value, `${base}.${where}.value`);
          aborted = true;
          return;
        }
        if (!info.states.has(value)) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', value, `${base}.${where}.value`);
          aborted = true;
          return;
        }
      }
      const current = states.get(target);
      const pre = current ? current.value : null;
      const terminals = machineTerminals(info);
      // Terminal absorption (§10 rule 7): a POST_TERMINAL delivery may not add a
      // domain write beyond a contract-defined duplicate reconciliation.
      if (!info.emptyEdge && where === 'step' && pre !== null && terminals.includes(pre) &&
          scenario.stimulus?.orderingClass === 'POST_TERMINAL' &&
          scenario.axis !== 'DUPLICATE' && stepDomainSum(step) > 0) {
        root(p, 'A5_TERMINAL_MUTATION', target, `${base}.${where} (${stepId || ''})`);
        aborted = true;
        return;
      }
      if (op === 'UNCHANGED') {
        if (pre !== null && value !== pre) {
          root(p, 'A5_STATE_SEQUENCE_MISMATCH', target, `${base}.${where}`);
          aborted = true;
        }
        if (where === 'final') {
          root(p, 'A5_SCENARIO_FIELD_INVALID', 'UNCHANGED', `${base}.expected.persisted`);
          aborted = true;
        }
        return;
      }
      if (op !== 'EQUALS') {
        if (where === 'step') states.set(target, current || { value: null });
        return;
      }
      if (!info.emptyEdge && where === 'step' && pre !== null && terminals.includes(pre)) {
        if (value !== pre) {
          root(p, 'A5_TERMINAL_MUTATION', target, `${base}.${where} (${stepId || ''})`);
          aborted = true;
          return;
        }
      }
      if (pre !== null && value === pre && where === 'step') {
        root(p, 'A5_SCENARIO_FIELD_INVALID', value, `${base}.${where}.value`);
        aborted = true;
        return;
      }
      if (where === 'step' && pre !== null && value !== pre) {
        if (!info.emptyEdge) {
          if (!assertion.transitionRef) {
            root(p, 'A5_SCENARIO_FIELD_MISSING', target, `${base}.${where}.transitionRef`);
            aborted = true;
            return;
          }
          const parsed = parseTransitionRef(assertion.transitionRef);
          if (!parsed) {
            root(p, 'A5_TRANSITION_REF_INVALID', assertion.transitionRef, `${base}.${where}.transitionRef`);
            aborted = true;
            return;
          }
          const expectedKind = info.name && ctx.a0.stateRows.has(info.name) ? 'A0' : 'A2';
          if (parsed.kind !== expectedKind || parsed.machine !== info.name) {
            root(p, 'A5_TRANSITION_REF_INVALID', assertion.transitionRef, `${base}.${where}.transitionRef`);
            aborted = true;
            return;
          }
          if (info.derivedStates.has(parsed.from) || info.derivedStates.has(parsed.to)) {
            root(p, 'A5_TRANSITION_REF_INVALID', assertion.transitionRef, `${base}.${where}.transitionRef`);
            aborted = true;
            return;
          }
          if (!info.edges.has(`${parsed.from}\u0000${parsed.to}`)) {
            root(p, 'A5_TRANSITION_REF_INVALID', assertion.transitionRef, `${base}.${where}.transitionRef`);
            aborted = true;
            return;
          }
          if (parsed.from !== pre || parsed.to !== value) {
            root(p, 'A5_STATE_SEQUENCE_MISMATCH', target, `${base}.${where}.transitionRef`);
            aborted = true;
            return;
          }
        } else if (assertion.transitionRef) {
          root(p, 'A5_TRANSITION_REF_INVALID', assertion.transitionRef, `${base}.${where}.transitionRef`);
          aborted = true;
          return;
        }
      }
      if (where === 'step') states.set(target, { value });
      else if (where === 'final' && current && current.value !== value) {
        const sequence = sequenceValues(scenario, target);
        if (scenario.axis === 'OUT_OF_ORDER' && sequence.includes(value)) {
          root(p, 'A5_ORDERING_REGRESSION', target, `${base}.expected.persisted`);
        } else {
          root(p, 'A5_STATE_SEQUENCE_MISMATCH', target, `${base}.expected.persisted`);
        }
        aborted = true;
      } else if (where === 'pre') {
        states.set(target, { value });
      }
    };
    for (const item of stateAssertions(scenario)) {
      if (aborted) break;
      apply(item.assertion, item.where, item.where === 'step' ? scenario.stimulus.steps[item.stepIndex] : null, item.stepId);
    }
    if (aborted) continue;

    // Final persisted must exist for every machine target used earlier.
    for (const [target, current] of states) {
      const info = machineInfo(ctx, target, machineCache);
      if (info?.kind === 'machine') {
        if (!scenario.expected.persisted.some((assertion) => assertion.targetRef === target)) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', target, `${base}.expected.persisted`);
          aborted = true;
          break;
        }
      }
    }
    if (aborted) continue;

    // Closed ExternalAssertion shapes for both the final projection and step-local responses.
    const external = Array.isArray(scenario.expected.external) ? scenario.expected.external : [];
    let externalFailure = false;
    for (const rootAssertion of external) {
      if (!validateExternalAssertion(ctx, rootAssertion, id, `${base}.expected.external`, p)) {
        externalFailure = true;
      }
    }
    for (let index = 0; index < scenario.stimulus.steps.length; index += 1) {
      const response = scenario.stimulus.steps[index].expectedResponse;
      if (response !== undefined &&
          !validateExternalAssertion(ctx, response, id, `${base}.stimulus.steps[${index}].expectedResponse`, p)) {
        externalFailure = true;
      }
    }
    if (externalFailure) continue;

    // External projection reconciliation.
    let contradiction = false;
    for (const rootAssertion of external) {
      let last = null;
      for (const step of scenario.stimulus.steps) {
        const response = step.expectedResponse;
        if (response && response.operationRef === rootAssertion.operationRef && response.field === rootAssertion.field) {
          last = response;
        }
      }
      if (last && (last.op !== rootAssertion.op || last.valueRef !== rootAssertion.valueRef)) {
        root(p, 'A5_EXTERNAL_PROJECTION_CONTRADICTION', `${rootAssertion.operationRef}/${rootAssertion.field}`, `${base}.expected.external`);
        contradiction = true;
        break;
      }
    }
    if (contradiction) continue;

    // Effect structure and arithmetic.
    const effects = scenario.expected.effects;
    const vectors = [effects.baseline, ...(effects.steps || []).map((step) => step.delta), effects.overall];
    let effectFailure = false;
    const checkVector = (vector, where) => {
      for (const category of ['domainWrites', 'audit', 'outbox', 'accessEvents', 'externalCalls']) {
        for (const item of effectVector(vector)[category]) {
          if (item.mode === 'EXACT') {
            if (!Number.isInteger(item.value) || item.value < 0) {
              root(p, 'A5_EFFECT_VALUE_INVALID', item.value, `${base}.${where}.${category}`);
              effectFailure = true;
            }
          } else if (item.mode === 'BOUNDED') {
            if (!Number.isInteger(item.min) || !Number.isInteger(item.max) ||
                item.min < 0 || item.max < item.min) {
              root(p, 'A5_EFFECT_VALUE_INVALID', `${item.min}-${item.max}`, `${base}.${where}.${category}`);
              effectFailure = true;
            }
            if (!item.basisRef) {
              root(p, 'A5_EFFECT_BOUND_UNBACKED', item[category === 'domainWrites' ? 'tableRef' : category === 'audit' ? 'auditRef' : category === 'outbox' ? 'outboxRef' : category === 'accessEvents' ? 'accessEventRef' : 'operationRef'], `${base}.${where}.${category}`);
              effectFailure = true;
            }
          } else {
            root(p, 'A5_EFFECT_VALUE_INVALID', item.mode, `${base}.${where}.${category}`);
            effectFailure = true;
          }
          if (!item.basisRef) {
            if (item.mode === 'BOUNDED') {
              // already reported
            } else {
              root(p, 'A5_EFFECT_VALUE_INVALID', 'basisRef', `${base}.${where}.${category}`);
              effectFailure = true;
            }
          }
        }
      }
    };
    checkVector(effects.baseline, 'effects.baseline');
    (effects.steps || []).forEach((step, index) => checkVector(step.delta, `effects.steps[${index}]`));
    checkVector(effects.overall, 'effects.overall');
    if (effectFailure) continue;
    if (!Array.isArray(effects.zeroOrBoundedBasisRefs) || effects.zeroOrBoundedBasisRefs.length === 0) {
      root(p, 'A5_SCENARIO_FIELD_MISSING', id, `${base}.expected.effects.zeroOrBoundedBasisRefs`);
      continue;
    }
    const stepDeltas = (effects.steps || []).map((step) => step.delta);
    const sums = sumVectorEntries([effects.baseline, ...stepDeltas]);
    const refKey = { domainWrites: 'tableRef', audit: 'auditRef', outbox: 'outboxRef', accessEvents: 'accessEventRef', externalCalls: 'operationRef' };
    let ledgerFailure = false;
    const overallVector = effectVector(effects.overall);
    for (const category of Object.keys(sums)) {
      for (const [ref, sum] of sums[category]) {
        const overallItems = overallVector[category]
          .filter((item) => item[refKey[category]] === ref);
        if (overallItems.length === 0) {
          root(p, 'A5_EFFECT_LEDGER_MISMATCH', ref, `${base}.expected.effects.overall`);
          ledgerFailure = true;
          continue;
        }
        const overallMin = overallItems.reduce((total, item) => total + effectQuantity(item).min, 0);
        const overallMax = overallItems.reduce((total, item) => total + effectQuantity(item).max, 0);
        if (overallItems.some((item) => item.mode === 'BOUNDED')) {
          // BOUNDED overall must contain the baseline + step-delta interval.
          if (!(overallMin <= sum.min && sum.max <= overallMax)) {
            root(p, 'A5_EFFECT_LEDGER_MISMATCH', ref, `${base}.expected.effects.overall`);
            ledgerFailure = true;
          }
        } else if (overallMin !== sum.min || overallMax !== sum.max) {
          // EXACT overall keeps the existing strict equality.
          root(p, 'A5_EFFECT_LEDGER_MISMATCH', ref, `${base}.expected.effects.overall`);
          ledgerFailure = true;
        }
      }
      // Reconcile the union: an overall-only ref has an expected baseline+delta
      // sum of zero, so only a zero-containing overall interval is legal.
      const overallRefs = new Set(overallVector[category].map((item) => item[refKey[category]]));
      for (const ref of overallRefs) {
        if (sums[category].has(ref)) continue;
        const overallItems = overallVector[category].filter((item) => item[refKey[category]] === ref);
        const overallMin = overallItems.reduce((total, item) => total + effectQuantity(item).min, 0);
        const overallMax = overallItems.reduce((total, item) => total + effectQuantity(item).max, 0);
        if (!(overallMin <= 0 && 0 <= overallMax)) {
          root(p, 'A5_EFFECT_LEDGER_MISMATCH', ref, `${base}.expected.effects.overall`);
          ledgerFailure = true;
        }
      }
    }
    if (ledgerFailure) continue;

    // Duplicate rules.
    if (scenario.axis === 'DUPLICATE') {
      const replay = scenario.duplicateReplay;
      const stepIds = scenario.stimulus.steps.map((step) => step.stepId);
      if (!replay || !stepIds.includes(replay.ofStepId) || replay.businessEffect !== 'ZERO') {
        root(p, 'A5_DUPLICATE_SIDE_EFFECT', id, `${base}.duplicateReplay`);
        continue;
      }
      const ofIndex = stepIds.indexOf(replay.ofStepId);
      let duplicateFailure = false;
      for (let index = ofIndex + 1; index < stepDeltas.length; index += 1) {
        for (const item of effectVector(stepDeltas[index]).domainWrites) {
          if (effectMax(item) <= 0) continue;
          if (DUPLICATE_EXCEPTIONS.has(tableKey(item.tableRef)) && item.basisRef) continue;
          root(p, 'A5_DUPLICATE_SIDE_EFFECT', item.tableRef, `${base}.effects.steps[${index}]`);
          duplicateFailure = true;
        }
      }
      if (duplicateFailure) continue;
    }

    // Permission-denied rules.
    if (scenario.axis === 'PERMISSION_DENIED') {
      let deniedFailure = false;
      for (const item of effectVector(effects.overall).domainWrites) {
        if (effectMax(item) <= 0) continue;
        if (DENIAL_ALLOWLIST.has(tableKey(item.tableRef)) && item.basisRef) continue;
        root(p, 'A5_DENIED_WITH_BUSINESS_EFFECT', item.tableRef, `${base}.expected.effects.overall`);
        deniedFailure = true;
      }
      if (deniedFailure) continue;
    }

    // Out-of-order rules.
    if (scenario.axis === 'OUT_OF_ORDER') {
      // Plan §8.3: no second settlement/release/usage fact for OUT_OF_ORDER.
      // service_activation is excluded: two distinct transitions (activate then
      // suspend) are legitimate ordered writes, not a repeated settlement fact.
      const protectedRefs = new Set([
        'entitlement_metering.usage_fact', 'entitlement_metering.usage_reservation',
        'entitlement_metering.entitlement_grant', 'evidence.evidence_record',
        'sharing.share_admission'
      ]);
      // Aggregate by normalized table ref so split entries cannot evade the
      // threshold; BOUNDED quantities contribute their upper bound.
      const totals = new Map();
      for (const item of effectVector(effects.overall).domainWrites) {
        const key = tableKey(item.tableRef);
        if (!totals.has(key)) totals.set(key, { max: 0, ref: item.tableRef });
        const total = totals.get(key);
        total.max += effectQuantity(item).max;
      }
      let orderingFailure = false;
      for (const [key, total] of totals) {
        if (total.max > 1 && protectedRefs.has(key)) {
          root(p, 'A5_ORDERING_REGRESSION', total.ref, `${base}.expected.effects.overall`);
          orderingFailure = true;
        }
      }
      if (orderingFailure) continue;
    }

    // Timeout rules: every declared timingRef must be a resolved contract/policy input.
    if (scenario.axis === 'TIMEOUT') {
      const timingRefs = scenario.stimulus.steps
        .filter((step) => step.timingRef !== undefined)
        .map((step) => step.timingRef);
      let timingFailure = false;
      if (timingRefs.length === 0) {
        root(p, 'A5_SCENARIO_FIELD_MISSING', id, `${base}.stimulus.steps.timingRef`);
        timingFailure = true;
      }
      for (const timingRef of timingRefs) {
        if (!isString(timingRef) || !TIMING_REF_PREFIXES.some((prefix) => timingRef.startsWith(prefix))) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', String(timingRef), `${base}.stimulus.steps.timingRef`);
          timingFailure = true;
          continue;
        }
        const resolved = resolveRef(ctx, timingRef);
        if (resolved.status === 'legacy') {
          root(p, 'A5_LEGACY_REFERENCE_FORBIDDEN', timingRef, `${base}.stimulus.steps.timingRef`);
          timingFailure = true;
        } else if (resolved.status === 'invalid') {
          root(p, 'A5_SOURCE_REF_INVALID', timingRef, `${base}.stimulus.steps.timingRef`);
          timingFailure = true;
        } else if (resolved.status === 'unresolved') {
          root(p, 'A5_SOURCE_REF_UNRESOLVED', timingRef, `${base}.stimulus.steps.timingRef`);
          timingFailure = true;
        } else if (resolved.status === 'ambiguous') {
          root(p, 'A5_SOURCE_REF_AMBIGUOUS', timingRef, `${base}.stimulus.steps.timingRef`);
          timingFailure = true;
        } else if (resolved.status === 'ok' && !TIMING_POLICY_REFS.has(timingRef)) {
          // A resolvable but non-policy ref (e.g. an arbitrary A0 table) is not
          // a contract/policy timing input.
          root(p, 'A5_SCENARIO_FIELD_INVALID', timingRef, `${base}.stimulus.steps.timingRef`);
          timingFailure = true;
        }
      }
      if (timingFailure) continue;
    }

    // Cleanup rules.
    const cleanup = scenario.expected.cleanup;
    const illegal = (cleanup.steps || []).some((step) => /\b(delete|truncate|drop|erase|purge)\b/i.test(String(step)));
    if (illegal) {
      root(p, 'A5_CLEANUP_ILLEGAL', id, `${base}.expected.cleanup.steps`);
      continue;
    }
    if (scenario.axis === 'RECOVERY') {
      if (cleanup.mode === 'NONE' || !Array.isArray(cleanup.sourceRefs) || cleanup.sourceRefs.length === 0) {
        root(p, 'A5_CLEANUP_ILLEGAL', id, `${base}.expected.cleanup`);
        continue;
      }
      if (cleanup.mode === 'MANUAL_RESTORE' &&
          !cleanup.sourceRefs.some((ref) => ref.startsWith('A4:') || ref.startsWith('A2:FIX:'))) {
        root(p, 'A5_CLEANUP_ILLEGAL', id, `${base}.expected.cleanup.sourceRefs`);
        continue;
      }
    }

    // Evidence expectations.
    const evidence = scenario.expected.evidence;
    let evidenceFailure = false;
    const checkEvidenceEntries = (entries, category) => {
      for (const item of Array.isArray(entries) ? entries : []) {
        if (!['EXACT', 'ZERO', 'BOUNDED'].includes(item.expectation)) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', item.expectation, `${base}.expected.evidence.${category}`);
          evidenceFailure = true;
        }
        if (!item.basisRef) {
          root(p, 'A5_SCENARIO_FIELD_MISSING', 'basisRef', `${base}.expected.evidence.${category}`);
          evidenceFailure = true;
        }
      }
    };
    checkEvidenceEntries(evidence.audit, 'audit');
    checkEvidenceEntries(evidence.outbox, 'outbox');
    checkEvidenceEntries(evidence.accessEvents, 'accessEvents');
    if (evidenceFailure) continue;
    if ((evidence.audit.length === 0 || evidence.outbox.length === 0 || evidence.accessEvents.length === 0) &&
        (!Array.isArray(evidence.zeroBasisRefs) || evidence.zeroBasisRefs.length === 0)) {
      root(p, 'A5_SCENARIO_FIELD_INVALID', id, `${base}.expected.evidence.zeroBasisRefs`);
      continue;
    }
    for (const observability of evidence.observability || []) {
      if (!METRIC_CATEGORIES.has(observability.metricCategoryRef)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', observability.metricCategoryRef, `${base}.expected.evidence.observability`);
        evidenceFailure = true;
      }
      if (!['CORRELATED_BY_TRACE_ID', 'NO_FORBIDDEN_PAYLOAD'].includes(observability.assertion)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', observability.assertion, `${base}.expected.evidence.observability`);
        evidenceFailure = true;
      }
    }
    for (const log of evidence.logs || []) {
      if (log.boundaryRef !== 'A3:TB:TB7' || log.redactionRef !== 'A3:INV:INV-A3-15' ||
          log.assertion !== 'NO_DENYLISTED_FIELD') {
        root(p, 'A5_SCENARIO_FIELD_INVALID', id, `${base}.expected.evidence.logs`);
        evidenceFailure = true;
      }
    }
    if (evidenceFailure) continue;
    const compatibility = auditCompatibleUnits(scenario.coverageUnitId);
    const auditRefs = [
      ...effectVector(effects.overall).audit.map((item) => item.auditRef),
      ...(evidence.audit || []).map((item) => item.auditRef)
    ];
    for (const auditRef of auditRefs) {
      const code = auditRef.replace(/^A3:AUDIT:/, '');
      if (!ctx.a3.auditEvents.has(code) || !compatibility.has(code)) {
        root(p, 'A5_SCENARIO_FIELD_INVALID', auditRef, `${base}.expected.evidence.audit`);
        evidenceFailure = true;
      }
    }
    if (evidenceFailure) continue;

    const overall = effectVector(effects.overall);
    const mirror = (category, keyName, evidenceList) => {
      for (const item of overall[category]) {
        const value = Number(item.value || 0);
        if (value <= 0) continue;
        const match = evidenceList.find((entry) => entry[keyName] === item[keyName]);
        if (!match || match.expectation !== 'EXACT' || Number(match.value) !== value) {
          root(p, 'A5_SCENARIO_FIELD_INVALID', item[keyName], `${base}.expected.evidence.${category}`);
          evidenceFailure = true;
        }
      }
    };
    mirror('audit', 'auditRef', evidence.audit);
    mirror('outbox', 'outboxRef', evidence.outbox);
    mirror('accessEvents', 'auditRef', evidence.accessEvents);
    if (evidenceFailure) continue;

    // Plan §4.2 runtime floor, owned by the validator independently of model/doc parity.
    // Exact-set inclusion: every minimum tier must be present; stronger extras stay legal.
    const minimumTiers = UNIT_MINIMUM_TIERS[scenario.coverageUnitId] || [];
    if (minimumTiers.some((tier) => !scenario.requiredEvidenceTiers.includes(tier))) {
      root(p, 'A5_EVIDENCE_TIER_INSUFFICIENT', id, `${base}.requiredEvidenceTiers`);
      continue;
    }
  }

  // Path/family uniqueness.
  for (const scenario of model.scenarios || []) {
    const familyUnit = PATH_PAIR[scenario.coverageUnitId];
    if (!familyUnit) continue;
    const familyRow = coverageByCell.get(`${familyUnit}/${scenario.axis}`);
    if (!familyRow || familyRow.state !== 'REPRESENTED') continue;
    const familyScenario = scenariosById.get(familyRow.scenarioId);
    if (!familyScenario) continue;
    const pathSources = new Set(scenario.sourceRefs);
    const familySources = new Set(familyScenario.sourceRefs);
    const pathOutcomes = new Set(scenario.outcomeRefs);
    const familyOutcomes = new Set(familyScenario.outcomeRefs);
    const sourceDiff = [...pathSources].some((ref) => !familySources.has(ref));
    const outcomeDiff = [...pathOutcomes].some((ref) => !familyOutcomes.has(ref));
    if (!sourceDiff || !outcomeDiff) {
      root(p, 'A5_PATH_FAMILY_DUPLICATE', scenario.scenarioId, `scenarios.${scenario.scenarioId}.path`);
    }
    if (scenario.coverageUnitId === 'PATH-DEPLOY-ACCEPTANCE') {
      if (!scenario.sourceRefs.some((ref) => ref.startsWith('A4:')) ||
          !scenario.outcomeRefs.some((ref) => ref.startsWith('A4:'))) {
        root(p, 'A5_PATH_FAMILY_DUPLICATE', scenario.scenarioId, `scenarios.${scenario.scenarioId}.path-deploy`);
      }
    }
  }
}

function stepDomainSum(step) {
  if (!step || !step.expectedDelta) return 0;
  return effectVector(step.expectedDelta).domainWrites
    .reduce((sum, item) => sum + effectMax(item), 0);
}
function sequenceValues(scenario, target) {
  const values = [];
  const pre = scenario.preconditions?.startingState || [];
  for (const assertion of pre) if (assertion.targetRef === target && assertion.value !== undefined) values.push(assertion.value);
  for (const step of scenario.stimulus?.steps || []) {
    for (const assertion of step.expectedState || []) {
      if (assertion.targetRef === target && assertion.value !== undefined) values.push(assertion.value);
    }
  }
  return values;
}
function auditCompatibleUnits(unit) {
  const all = new Set(ctxAuditAll());
  if (unit === 'FAM-SECURITY' || unit === 'PATH-AUTH-AUDIT') return all;
  if (unit === 'FAM-ACTIVATION' || unit === 'PATH-ACTIVATION-TRIAL') {
    return new Set(['SUBJECT_SUSPEND', 'SUBJECT_RESUME', 'SECURITY_AUTH_DENIED', 'SECURITY_AUDIENCE_MISMATCH']);
  }
  if (unit === 'FAM-COMMERCE' || unit === 'PATH-COMMERCE-ENTITLEMENT') {
    return new Set(['CATALOG_VERSION_PUBLISH', 'CATALOG_VERSION_OFFSALE', 'TRIAL_POLICY_CREATE',
      'TRIAL_POLICY_REPLACE', 'TRIAL_POLICY_PUBLISH', 'TRIAL_POLICY_DISABLE', 'PAYMENT_CALLBACK_ACCEPTED']);
  }
  if (unit === 'FAM-SHARING' || unit === 'PATH-SHARE-ACCESS') {
    return new Set(['SHARE_REVOKE', 'SHARE_CODE_ROTATE']);
  }
  if (unit === 'FAM-MEDIA-EVIDENCE' || unit === 'PATH-RECORDING-EVIDENCE') {
    return new Set(['SECURITY_UPLOAD_CREDENTIAL_ISSUED', 'SECURITY_UPLOAD_CREDENTIAL_DENIED',
      'SECURITY_PLAYBACK_CREDENTIAL_ISSUED', 'SECURITY_PLAYBACK_CREDENTIAL_DENIED',
      'SECURITY_COS_POLICY_DENIED', 'SECURITY_REDACTION_BLOCKED', 'SECURITY_REDACTION_VIOLATION']);
  }
  if (unit === 'FAM-IDENTITY') return new Set(['SECURITY_AUTH_DENIED', 'SECURITY_AUDIENCE_MISMATCH']);
  if (unit === 'FAM-IVR' || unit === 'PATH-IVR-SETTLEMENT') {
    return new Set(['SECURITY_SIGNATURE_INVALID', 'SECURITY_REPLAY_REJECTED',
      'SECURITY_TIMESTAMP_REJECTED', 'SECURITY_AUTH_DENIED']);
  }
  return new Set();
}
function ctxAuditAll() {
  return ['SECURITY_KEY_ACTIVATE', 'SECURITY_KEY_ROTATE', 'SECURITY_KEY_RETIRE', 'SECURITY_KEY_DISABLE',
    'SECURITY_KEY_RECOVER', 'SECURITY_KEY_DESTROY', 'SECURITY_KEY_PURPOSE_MISMATCH',
    'SECURITY_KEY_ACCESS_DENIED', 'SECURITY_AUTH_DENIED', 'SECURITY_AUDIENCE_MISMATCH',
    'SECURITY_SERVICE_IDENTITY_DENIED', 'SECURITY_SIGNATURE_INVALID', 'SECURITY_REPLAY_REJECTED',
    'SECURITY_TIMESTAMP_REJECTED', 'SECURITY_PROVIDER_CALLBACK_REJECTED',
    'SECURITY_UPLOAD_CREDENTIAL_ISSUED', 'SECURITY_UPLOAD_CREDENTIAL_DENIED',
    'SECURITY_PLAYBACK_CREDENTIAL_ISSUED', 'SECURITY_PLAYBACK_CREDENTIAL_DENIED',
    'SECURITY_COS_POLICY_DENIED', 'AUDIT_READ', 'AUDIT_READ_DENIED', 'OPERATIONS_HIGH_RISK_DENIED',
    'SECURITY_SENSITIVE_READ', 'SECURITY_REDACTION_BLOCKED', 'SECURITY_REDACTION_VIOLATION',
    'SUBJECT_SUSPEND', 'SUBJECT_RESUME', 'CATALOG_VERSION_PUBLISH', 'CATALOG_VERSION_OFFSALE',
    'TRIAL_POLICY_CREATE', 'TRIAL_POLICY_REPLACE', 'TRIAL_POLICY_PUBLISH', 'TRIAL_POLICY_DISABLE',
    'CATALOG_DRAFT_CREATE', 'CATALOG_DRAFT_REPLACE', 'SHARE_REVOKE', 'SHARE_CODE_ROTATE',
    'PAYMENT_CALLBACK_ACCEPTED'];
}

function deriveDocTables(model) {
  const familyRows = [['Unit', 'Kind', 'Lead owner', 'Lead service', 'Paired unit']];
  for (const unit of model.coverageUnits || []) {
    familyRows.push([unit.unitId, unit.kind, unit.leadOwner, unit.leadService, unit.pairedUnit || '-']);
  }
  const axisRows = [['Axis', 'Meaning']];
  for (const axis of model.axes || []) axisRows.push([axis.axis, axis.meaning]);
  const coverageRows = [['Unit', 'Axis', 'State', 'Scenario/Reason']];
  for (const unit of model.coverageUnits || []) {
    for (const axis of AXES) {
      const row = (model.coverage || []).find((item) => item.unitId === unit.unitId && item.axis === axis);
      coverageRows.push([unit.unitId, axis, row?.state || 'MISSING',
        row?.state === 'REPRESENTED' ? row.scenarioId : row?.reasonCode || '-']);
    }
  }
  const scenarioRows = [['Scenario', 'Unit', 'Axis', 'Title', 'Owner', 'Evidence owner', 'Aggregate ref', 'Tiers']];
  for (const scenario of model.scenarios || []) {
    scenarioRows.push([scenario.scenarioId, scenario.coverageUnitId, scenario.axis, scenario.title,
      scenario.owner, scenario.evidenceOwner, scenario.aggregateRef, scenario.requiredEvidenceTiers.join('+')]);
  }
  const stateRows = [['From', 'To', 'Condition']];
  for (const row of model.acceptanceStateMachine?.transitions || []) {
    stateRows.push([row.from, row.to, row.condition]);
  }
  const reasonRows = [['Reason', 'Allowed cells']];
  for (const reason of model.notApplicableReasons || []) {
    reasonRows.push([reason.reasonId, [...reason.allowedCells].sort().join(', ')]);
  }
  const residualRows = [['Residual', 'Owner', 'Destination', 'Upstream refs']];
  for (const residual of model.residuals || []) {
    residualRows.push([residual.residualId, residual.owner, residual.destination, residual.upstreamRefs.join(', ')]);
  }
  const gateRows = [['Gate', 'Expression']];
  for (const gate of model.acceptanceStateMachine?.gates || []) gateRows.push([gate.gate, gate.expression]);
  return {
    'acceptance-family-map': familyRows,
    'acceptance-axis-map': axisRows,
    'acceptance-coverage-map': coverageRows,
    'acceptance-scenario-index': scenarioRows,
    'acceptance-evidence-state-map': stateRows,
    'acceptance-reason-map': reasonRows,
    'acceptance-residual-map': residualRows,
    'acceptance-gate-map': gateRows
  };
}

function parseDocBlock(doc, name) {
  const start = `<!-- ${name}:start -->`;
  const end = `<!-- ${name}:end -->`;
  const starts = doc.split(start).length - 1;
  const ends = doc.split(end).length - 1;
  if (starts === 0 || ends === 0) return { status: 'missing' };
  if (starts > 1 || ends > 1) return { status: 'duplicate' };
  const startIndex = doc.indexOf(start) + start.length;
  const endIndex = doc.indexOf(end, startIndex);
  const block = doc.slice(startIndex, endIndex);
  const rows = [];
  for (const line of block.split('\n')) {
    if (!line.trim().startsWith('|')) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length === 0) continue;
    if (cells.every((cell) => /^:?-+:?$/.test(cell))) continue;
    rows.push(cells);
  }
  return { status: 'ok', rows };
}

function validateDoc(model, doc, p) {
  const derived = deriveDocTables(model);
  for (const [name, expectedRows] of Object.entries(derived)) {
    const block = parseDocBlock(doc, name);
    if (block.status === 'missing') {
      root(p, 'A5_DOC_MARKER_MISSING', name, 'docs/19');
      continue;
    }
    if (block.status === 'duplicate') {
      root(p, 'A5_DOC_MARKER_DUPLICATE', name, 'docs/19');
      continue;
    }
    if (JSON.stringify(block.rows) !== JSON.stringify(expectedRows)) {
      root(p, 'A5_DOC_MODEL_DRIFT', name, 'docs/19');
    }
  }
}

function computeBaseline(model) {
  return {
    a0: {
      tables: 44,
      invariants: 40,
      stateRows: 31,
      stateRowsBySlice: { S1: 12, S2: 13, S3: 6 },
      transactions: 29,
      qRows: 28
    },
    a1: { operations: 49, problemCodes: 59 },
    a2: {
      messages: 12, eventTypes: 11, identities: 22, invariants: 15, fixtures: 25,
      eventRegistrySha256: 'c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9'
    },
    a3: {
      trustSurfaces: 5, authDomains: 5, trustBoundaries: 7, serviceIdentities: 6,
      credentialClasses: 10, auditEvents: 39, highRiskActions: 10, invariants: 21,
      deferredInputs: 23, mediaPolicies: 2, fixtures: 23, residuals: 18
    },
    a4: {
      groups: 9, fixtures: 12, capabilities: 6, residualRows: 16, closeoutRows: 10,
      constants: { productionReady: false, procurementReady: false, cloudVerified: false, a3RiskClosed: false }
    }
  };
}

function validateBaseline(ctx, model, p) {
  const expected = computeBaseline(model);
  const actual = {
    a0: {
      tables: ctx.a0.tables.size,
      invariants: ctx.a0.invariants.size,
      stateRows: ctx.a0.stateRows.size,
      stateRowsBySlice: {
        S1: ctx.a0.bySection['7.1']?.length || 0,
        S2: ctx.a0.bySection['7.3']?.length || 0,
        S3: ctx.a0.bySection['7.4']?.length || 0
      },
      transactions: ctx.a0.transactions.size,
      qRows: ctx.a0.qRows.size
    },
    a1: { operations: ctx.a1.operations.size, problemCodes: ctx.a1.problemCodes.size },
    a2: {
      messages: ctx.a2.messages.size, eventTypes: ctx.a2.eventTypes.size,
      identities: ctx.a2.identities.size, invariants: ctx.a2.invariants.size,
      fixtures: ctx.a2.fixtures.size, eventRegistrySha256: ctx.a2.eventRegistrySha256
    },
    a3: {
      trustSurfaces: ctx.a3.counts.trustSurfaces, authDomains: ctx.a3.counts.authDomains,
      trustBoundaries: ctx.a3.counts.trustBoundaries, serviceIdentities: ctx.a3.counts.serviceIdentities,
      credentialClasses: ctx.a3.counts.credentialClasses, auditEvents: ctx.a3.counts.auditEvents,
      highRiskActions: ctx.a3.counts.highRiskActions, invariants: ctx.a3.counts.invariants,
      deferredInputs: ctx.a3.counts.deferredInputs, mediaPolicies: ctx.a3.counts.mediaPolicies,
      fixtures: ctx.a3.counts.fixtures, residuals: ctx.a3.counts.residuals
    },
    a4: {
      groups: ctx.a4.counts.groups, fixtures: ctx.a4.counts.fixtures,
      capabilities: ctx.a4.counts.capabilities, residualRows: ctx.a4.counts.residualRows,
      closeoutRows: ctx.a4.counts.closeoutRows, constants: ctx.a4.constants
    }
  };
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    root(p, 'A5_SOURCE_DRIFT', 'upstream-baseline', 'sources');
  }
  if (JSON.stringify(model.sourceBaseline) !== JSON.stringify(expected)) {
    root(p, 'A5_SOURCE_BASELINE_MISMATCH', 'sourceBaseline', 'model.sourceBaseline');
  }
  // A3 model may not carry a residual registry.
  if (JSON.stringify(ctx.a3Model).includes('R-A3') ||
      Object.prototype.hasOwnProperty.call(ctx.a3Model, 'residuals')) {
    root(p, 'A5_SOURCE_DRIFT', 'a3-residuals', 'a3-model');
  }
}

// ---------------------------------------------------------------------------
// S2: acceptance evidence contract, admission and aggregate derivation.
// Static-only. The authority registry is deliberately empty (OQ-03), so a
// manifest can at most reach EVIDENCE_VALID; PASS/production acceptance remain
// unreachable without a future approved external authority.
// ---------------------------------------------------------------------------

function scanForbiddenControlKeys(value, base, issues) {
  if (Array.isArray(value)) {
    value.forEach((item, index) => scanForbiddenControlKeys(item, `${base}[${index}]`, issues));
    return;
  }
  if (!isObject(value)) return;
  for (const [key, item] of Object.entries(value)) {
    if (MANIFEST_CALLER_FIELDS.includes(key)) {
      root(issues, 'A5_CALLER_CONTROL_FORBIDDEN', key, `${base}.${key}`);
    } else if (MANIFEST_CALLER_ASSERTED_FIELDS.includes(key)) {
      root(issues, 'A5_CALLER_ASSERTED_PASS', key, `${base}.${key}`);
    } else if (['productionReady', 'procurementReady', 'cloudVerified', 'a3RiskClosed'].includes(key)) {
      root(issues, 'A5_PROHIBITED_READINESS_FLAG', key, `${base}.${key}`);
    }
    scanForbiddenControlKeys(item, `${base}.${key}`, issues);
  }
}

function validateS2Contract(model, p) {
  const policy = model.evidencePolicy;
  scanForbiddenControlKeys(policy, 'model.evidencePolicy', p);
  scanForbiddenControlKeys(model.authorityRegistry, 'model.authorityRegistry', p);
  if (policy === undefined) {
    root(p, 'A5_SECTION_MISSING', 'evidencePolicy', 'model.evidencePolicy');
  } else if (!isObject(policy)) {
    root(p, 'A5_MODEL_INVALID', 'evidencePolicy', 'model.evidencePolicy');
  } else {
    for (const [key, expected] of Object.entries(EVIDENCE_POLICY_CONTRACT)) {
      const actual = policy[key];
      if (Array.isArray(expected)) {
        if (!sameStringSet(actual, expected)) {
          root(p, 'A5_MODEL_INVALID', key, `model.evidencePolicy.${key}`);
        }
      } else if (actual !== expected) {
        root(p, 'A5_MODEL_INVALID', key, `model.evidencePolicy.${key}`);
      }
    }
    if (policy.requiredStateMachine !== undefined && model.acceptanceStateMachine?.states &&
        !sameStringSet(policy.requiredStateMachine, model.acceptanceStateMachine.states)) {
      root(p, 'A5_MODEL_INVALID', 'requiredStateMachine', 'model.evidencePolicy.requiredStateMachine');
    }
  }
  if (!Array.isArray(model.authorityRegistry)) {
    if (model.authorityRegistry === undefined) {
      root(p, 'A5_SECTION_MISSING', 'authorityRegistry', 'model.authorityRegistry');
    } else {
      root(p, 'A5_MODEL_INVALID', 'authorityRegistry', 'model.authorityRegistry');
    }
  } else if (model.authorityRegistry.length > 0) {
    // OQ-03: the static registry must be empty. No invented authority row is
    // accepted, with or without a basis ref.
    for (const row of model.authorityRegistry) {
      const id = isObject(row) && isString(row.authorityId) ? row.authorityId : 'authorityRegistry';
      root(p, 'A5_ACCEPTANCE_AUTHORITY_INVALID', id, 'model.authorityRegistry');
    }
  }
}

function requiredArtifactRoles(scenario, requiredTiers) {
  const roles = new Set();
  const runtime = requiredTiers.some((tier) => RUNTIME_TIERS.includes(tier));
  if (!runtime) {
    roles.add('TEST_REPORT');
    return roles;
  }
  roles.add('TEST_REPORT');
  const evidence = scenario.expected?.evidence || {};
  if ((scenario.expected?.persisted || []).length > 0) roles.add('DB_STATE_EXPORT');
  if ((evidence.audit || []).length > 0 || (evidence.accessEvents || []).length > 0) {
    roles.add('AUDIT_EXPORT');
  }
  if ((evidence.outbox || []).length > 0) roles.add('OUTBOX_EXPORT');
  if ((evidence.observability || []).length > 0) roles.add('METRIC_SNAPSHOT');
  if ((evidence.logs || []).length > 0) roles.add('LOG_EXPORT');
  for (const tier of requiredTiers) {
    if (tier === 'LINE') roles.add('LINE_CDR');
    else if (tier === 'CLOUD') roles.add('CLOUD_EVIDENCE');
    else if (tier === 'MANUAL') roles.add('MANUAL_RECORD');
  }
  return roles;
}

function locatorAllowed(locatorRef) {
  if (!isString(locatorRef)) return false;
  const lower = locatorRef.toLowerCase();
  return !LOCATOR_FORBIDDEN.some((needle) => lower.includes(needle));
}

function validateArtifact(ctx, artifact, manifest, id, issues) {
  const where = `manifest.${id}.artifacts`;
  if (!isObject(artifact)) {
    root(issues, 'A5_EVIDENCE_REF_INVALID', id, where);
    return;
  }
  const keys = Object.keys(artifact);
  if (keys.some((key) => !ARTIFACT_FIELDS.includes(key)) ||
      ARTIFACT_FIELDS.some((key) => !Object.prototype.hasOwnProperty.call(artifact, key))) {
    root(issues, 'A5_EVIDENCE_REF_INVALID', id, where);
    return;
  }
  if (!isString(artifact.artifactId)) root(issues, 'A5_EVIDENCE_REF_INVALID', id, `${where}.artifactId`);
  if (!ARTIFACT_ROLES.includes(artifact.role)) root(issues, 'A5_EVIDENCE_REF_INVALID', id, `${where}.role`);
  if (!locatorAllowed(artifact.locatorRef)) root(issues, 'A5_EVIDENCE_REF_INVALID', id, `${where}.locatorRef`);
  if (!isString(artifact.sha256) || !SHA256_RE.test(artifact.sha256)) {
    root(issues, 'A5_EVIDENCE_REF_INVALID', id, `${where}.sha256`);
  }
  const captured = parseIsoZ(artifact.capturedAt);
  const executed = parseIsoZ(manifest.executedAt);
  const recorded = parseIsoZ(manifest.recordedAt);
  if (captured === null) {
    root(issues, 'A5_EVIDENCE_TIME_INVALID', id, `${where}.capturedAt`);
  } else if (executed !== null && recorded !== null && (captured < executed || captured > recorded)) {
    root(issues, 'A5_EVIDENCE_TIME_INVALID', id, `${where}.capturedAt`);
  }
  for (const key of ['producerRef', 'redactionRef']) {
    if (!isString(artifact[key]) || !BASIS_REF_SHAPE.test(artifact[key])) {
      root(issues, 'A5_EVIDENCE_REF_INVALID', id, `${where}.${key}`);
      continue;
    }
    // The human canonical contract requires every provenance ref to be a
    // resolvable source ref. Free text / unknown prefixes are invalid here;
    // a shaped ref that resolves to nothing is equally fail-closed.
    const result = resolveRef(ctx, artifact[key]);
    if (result.status !== 'ok') {
      root(issues, 'A5_EVIDENCE_REF_INVALID', artifact[key], `${where}.${key}`);
    }
  }
}

function validateObservation(ctx, observation, scenario, id, issues) {
  const where = `manifest.${id}.observations`;
  if (!isObject(observation)) {
    root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, where);
    return;
  }
  if (!isString(observation.targetRef) || !isString(observation.field) ||
      !Object.prototype.hasOwnProperty.call(observation, 'value')) {
    root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, where);
    return;
  }
  // An observation may only speak about business state the scenario itself
  // asserts: step expected-state rows plus the final persisted projection.
  // Claims about unrelated aggregates are out of scope and contradict the
  // evidence contract even when the value is legal for that aggregate.
  const assertedPairs = new Set();
  for (const step of scenario.stimulus?.steps || []) {
    for (const row of step.expectedState || []) {
      if (isObject(row) && isString(row.targetRef) && isString(row.field)) {
        assertedPairs.add(`${row.targetRef}\u0000${row.field}`);
      }
    }
  }
  for (const row of scenario.expected?.persisted || []) {
    if (isObject(row) && isString(row.targetRef) && isString(row.field)) {
      assertedPairs.add(`${row.targetRef}\u0000${row.field}`);
    }
  }
  if (!assertedPairs.has(`${observation.targetRef}\u0000${observation.field}`)) {
    root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:scope`);
    return;
  }
  const info = machineInfo(ctx, observation.targetRef, new Map());
  if (info && info.kind === 'machine') {
    if (info.derivedStates.has(observation.value) || !info.states.has(observation.value)) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:value`);
    }
  } else if (observation.targetRef.startsWith('A0:SM:') ||
             observation.targetRef.startsWith('A2:STATE:')) {
    root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:targetRef`);
  }
  const expected = (scenario.expected?.persisted || [])
    .find((row) => row.targetRef === observation.targetRef && row.field === observation.field);
  if (expected) {
    if (expected.op === 'EQUALS' && observation.value !== expected.value) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:final`);
    }
    if (expected.op === 'NULL' && observation.value !== null) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:final`);
    }
  }
  if (observation.transitionRef !== undefined) {
    if (!isString(observation.transitionRef)) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:transitionRef`);
      return;
    }
    const parsed = parseTransitionRef(observation.transitionRef);
    if (!parsed) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:transitionRef`);
      return;
    }
    const target = parsed.kind === 'A0'
      ? `A0:SM:${parsed.machine}`
      : `A2:STATE:${parsed.machine}`;
    const info2 = machineInfo(ctx, target, new Map());
    if (!info2 || info2.kind !== 'machine' ||
        !info2.edges.has(`${parsed.from}\u0000${parsed.to}`)) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:transitionRef`);
      return;
    }
    if (machineTerminals(info2).includes(parsed.from) && parsed.from !== parsed.to) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:terminal`);
    }
    const terminalStates = scenario.expected?.terminalAbsorption?.terminalStates || [];
    if (terminalStates.includes(parsed.from) && parsed.from !== parsed.to) {
      root(issues, 'A5_EVIDENCE_STATE_CONTRADICTION', id, `${where}:terminal`);
    }
  }
}

function validateEvidence(ctx, model, ledger, options = {}, issues = []) {
  const stats = {
    manifests: 0,
    admitted: 0,
    records: 0,
    blocked: 0,
    rejected: 0,
    admittedState: null,
    runtimeEvidence: 'NOT_RUN',
    productionAccepted: false
  };
  if (ledger === null || ledger === undefined) return stats;
  if (!isObject(ledger)) {
    root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', 'ledger', 'evidence');
    stats.runtimeEvidence = 'EVIDENCE_REJECTED';
    return stats;
  }
  const ledgerAllowed = new Set(['schemaVersion', 'asOf', 'manifests', 'acceptanceRecords']);
  if (Object.keys(ledger).some((key) => !ledgerAllowed.has(key))) {
    root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', 'ledger', 'evidence');
  }
  const asOf = parseIsoZ(ledger.asOf);
  if (ledger.schemaVersion !== 1 || asOf === null) {
    root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', 'ledger', 'evidence');
    stats.runtimeEvidence = 'EVIDENCE_REJECTED';
    return stats;
  }
  const manifests = Array.isArray(ledger.manifests) ? ledger.manifests : null;
  const records = Array.isArray(ledger.acceptanceRecords) ? ledger.acceptanceRecords : null;
  if (!manifests || !records) {
    root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', 'ledger', 'evidence');
    stats.runtimeEvidence = 'EVIDENCE_REJECTED';
    return stats;
  }
  stats.manifests = manifests.length;
  stats.records = records.length;

  const scenarios = new Map((model.scenarios || []).map((scenario) => [scenario.scenarioId, scenario]));
  const profiles = options.scenarioProfiles || {};
  const manifestById = new Map();
  const manifestState = new Map();
  const callerControlled = [];

  // Manifest identity conflicts are resolved first so a replay/immutability
  // failure is reported once instead of cascading into every other check.
  const identityGroups = new Map();
  for (const manifest of manifests) {
    if (!isObject(manifest) || !isString(manifest.manifestId)) continue;
    if (!identityGroups.has(manifest.manifestId)) identityGroups.set(manifest.manifestId, []);
    identityGroups.get(manifest.manifestId).push(manifest);
  }
  const conflicted = new Set();
  for (const [manifestId, group] of identityGroups) {
    if (group.length < 2) continue;
    const digests = new Set(group.map((item) => String(item.manifestDigest)));
    const bodies = new Set(group.map((item) => canonicalStringify({ ...item, manifestDigest: undefined })));
    if (digests.size > 1 || bodies.size > 1) {
      root(issues, 'A5_ACCEPTANCE_IMMUTABILITY_VIOLATION', manifestId, 'evidence.manifests');
      conflicted.add(manifestId);
    }
  }

  for (const manifest of manifests) {
    if (!isObject(manifest)) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', 'manifest', 'evidence.manifests');
      stats.rejected += 1;
      continue;
    }
    const id = isString(manifest.manifestId) ? manifest.manifestId : 'manifest';
    if (conflicted.has(id)) {
      stats.rejected += 1;
      continue;
    }
    if (manifestById.has(id)) continue; // identical replay: admit once
    manifestById.set(id, manifest);
    const before = issues.length;

    for (const key of MANIFEST_CALLER_FIELDS) {
      if (Object.prototype.hasOwnProperty.call(manifest, key)) {
        root(issues, 'A5_CALLER_CONTROL_FORBIDDEN', key, `manifest.${id}.${key}`);
        callerControlled.push(id);
      }
    }
    for (const key of MANIFEST_CALLER_ASSERTED_FIELDS) {
      if (Object.prototype.hasOwnProperty.call(manifest, key)) {
        root(issues, 'A5_CALLER_ASSERTED_PASS', key, `manifest.${id}.${key}`);
        callerControlled.push(id);
      }
    }
    for (const key of Object.keys(manifest)) {
      if (!MANIFEST_FIELDS.includes(key) &&
          !MANIFEST_CALLER_FIELDS.includes(key) &&
          !MANIFEST_CALLER_ASSERTED_FIELDS.includes(key)) {
        root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', key, `manifest.${id}.${key}`);
      }
    }
    const coreFields = ['manifestId', 'scenarioId', 'evidenceClass', 'evidenceTiers',
      'executedAt', 'recordedAt', 'artifacts', 'observations', 'manifestDigest'];
    const coreMissing = coreFields.some((key) =>
      !Object.prototype.hasOwnProperty.call(manifest, key) || manifest[key] === null);
    if (coreMissing) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', id, `manifest.${id}`);
      stats.rejected += 1;
      continue;
    }
    // Identity fields are closed non-empty strings: a non-string manifestId
    // or scenarioId can never be matched to a scenario/record identity.
    if (!isString(manifest.manifestId) || !isString(manifest.scenarioId)) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', id, `manifest.${id}`);
      stats.rejected += 1;
      continue;
    }
    const ownerPresent = Object.prototype.hasOwnProperty.call(manifest, 'evidenceOwner') &&
      manifest.evidenceOwner !== null;
    if (!ownerPresent) {
      root(issues, 'A5_EVIDENCE_OWNER_MISSING', id, `manifest.${id}.evidenceOwner`);
    }
    const scenario = scenarios.get(manifest.scenarioId);
    if (!scenario) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', manifest.scenarioId, `manifest.${id}.scenarioId`);
      stats.rejected += 1;
      continue;
    }
    if (ownerPresent &&
        (!isString(manifest.evidenceOwner) || manifest.evidenceOwner !== scenario.evidenceOwner)) {
      root(issues, 'A5_EVIDENCE_OWNER_MISMATCH', id, `manifest.${id}.evidenceOwner`);
    }
    if (!EVIDENCE_CLASSES.has(manifest.evidenceClass)) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', manifest.evidenceClass, `manifest.${id}.evidenceClass`);
    }
    if (!Array.isArray(manifest.evidenceTiers) || manifest.evidenceTiers.length === 0 ||
        new Set(manifest.evidenceTiers).size !== manifest.evidenceTiers.length ||
        manifest.evidenceTiers.some((tier) => !EXECUTION_TIERS.includes(tier))) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', id, `manifest.${id}.evidenceTiers`);
    }
    const executedAt = parseIsoZ(manifest.executedAt);
    const recordedAt = parseIsoZ(manifest.recordedAt);
    if (executedAt === null) root(issues, 'A5_EVIDENCE_TIME_INVALID', id, `manifest.${id}.executedAt`);
    if (recordedAt === null) root(issues, 'A5_EVIDENCE_TIME_INVALID', id, `manifest.${id}.recordedAt`);
    if (executedAt !== null && recordedAt !== null && executedAt > recordedAt) {
      root(issues, 'A5_EVIDENCE_TIME_INVALID', id, `manifest.${id}.recordedAt`);
    }
    if ((executedAt !== null && executedAt > asOf) || (recordedAt !== null && recordedAt > asOf)) {
      root(issues, 'A5_EVIDENCE_TIME_INVALID', id, `manifest.${id}`);
    }
    if (!isString(manifest.manifestDigest) || !SHA256_RE.test(manifest.manifestDigest)) {
      root(issues, 'A5_EVIDENCE_DIGEST_MISMATCH', id, `manifest.${id}.manifestDigest`);
    } else if (manifestDigestOf(manifest) !== manifest.manifestDigest) {
      root(issues, 'A5_EVIDENCE_DIGEST_MISMATCH', id, `manifest.${id}.manifestDigest`);
    }
    if (!Array.isArray(manifest.artifacts)) {
      root(issues, 'A5_EVIDENCE_ARTIFACT_MISSING', id, `manifest.${id}.artifacts`);
    } else {
      if (manifest.artifacts.length === 0) {
        root(issues, 'A5_EVIDENCE_ARTIFACT_MISSING', id, `manifest.${id}.artifacts`);
      }
      const artifactIds = new Set();
      for (const artifact of manifest.artifacts) {
        if (isObject(artifact) && isString(artifact.artifactId)) {
          if (artifactIds.has(artifact.artifactId)) {
            root(issues, 'A5_EVIDENCE_REF_INVALID', artifact.artifactId, `manifest.${id}.artifacts`);
          }
          artifactIds.add(artifact.artifactId);
        }
        validateArtifact(ctx, artifact, manifest, id, issues);
      }
    }
    if (!Array.isArray(manifest.observations)) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', id, `manifest.${id}.observations`);
    } else {
      for (const observation of manifest.observations) {
        validateObservation(ctx, observation, scenario, id, issues);
      }
    }

    const requiredTiers = profiles[scenario.scenarioId]?.requiredEvidenceTiers ||
      scenario.requiredEvidenceTiers || [];
    let tierInsufficient = false;
    let syntheticRejected = false;
    const providedTiers = Array.isArray(manifest.evidenceTiers) ? manifest.evidenceTiers : [];
    if (manifest.evidenceClass === 'SYNTHETIC' &&
        (providedTiers.some((tier) => RUNTIME_TIERS.includes(tier)) ||
         requiredTiers.some((tier) => RUNTIME_TIERS.includes(tier)))) {
      root(issues, 'A5_SYNTHETIC_EVIDENCE_REJECTED', id, `manifest.${id}.evidenceClass`);
      tierInsufficient = true;
      syntheticRejected = true;
    } else if (manifest.evidenceClass === 'STATIC_SPEC' || manifest.evidenceClass === 'MODEL_FIXTURE') {
      if (providedTiers.some((tier) => RUNTIME_TIERS.includes(tier)) ||
          requiredTiers.some((tier) => RUNTIME_TIERS.includes(tier))) {
        root(issues, 'A5_EVIDENCE_TIER_INSUFFICIENT', id, `manifest.${id}.evidenceTiers`);
        tierInsufficient = true;
      }
    } else if (manifest.evidenceClass === 'RUNTIME') {
      const provided = new Set(providedTiers);
      if (requiredTiers.some((tier) => !provided.has(tier))) {
        root(issues, 'A5_EVIDENCE_TIER_INSUFFICIENT', id, `manifest.${id}.evidenceTiers`);
        tierInsufficient = true;
      }
    }
    if (!tierInsufficient && Array.isArray(manifest.artifacts)) {
      const roles = new Set(manifest.artifacts
        .filter((artifact) => isObject(artifact) && isString(artifact.role))
        .map((artifact) => artifact.role));
      for (const role of requiredArtifactRoles(scenario, requiredTiers)) {
        if (!roles.has(role)) {
          root(issues, 'A5_EVIDENCE_ARTIFACT_MISSING', role, `manifest.${id}.artifacts`);
        }
      }
    }

    const manifestIssues = issues.slice(before);
    if (manifestIssues.length > 0) {
      const codes = new Set(manifestIssues.map((issue) => issue.code));
      const onlyTier = tierInsufficient && !syntheticRejected &&
        [...codes].every((code) => code === 'A5_EVIDENCE_TIER_INSUFFICIENT');
      if (onlyTier) stats.blocked += 1;
      else stats.rejected += 1;
    } else {
      stats.admitted += 1;
      manifestState.set(id, 'EVIDENCE_VALID');
    }
  }

  // Acceptance records must be independent of the manifest and reference an
  // approved authority. Record identity conflicts fail closed first.
  const recordGroups = new Map();
  for (const record of records) {
    if (!isObject(record) || !isString(record.scenarioId)) continue;
    if (!recordGroups.has(record.scenarioId)) recordGroups.set(record.scenarioId, []);
    recordGroups.get(record.scenarioId).push(record);
  }
  const conflictedScenarios = new Set();
  for (const [scenarioId, group] of recordGroups) {
    if (group.length < 2) continue;
    const bodies = new Set(group.map((record) => canonicalStringify(record)));
    if (bodies.size > 1) {
      root(issues, 'A5_ACCEPTANCE_IMMUTABILITY_VIOLATION', scenarioId, 'evidence.acceptanceRecords');
      conflictedScenarios.add(scenarioId);
    }
  }
  // recordId is a terminal identity: reusing it for a different body is an
  // immutability violation even when the scenario binding is otherwise valid.
  const recordIdGroups = new Map();
  for (const record of records) {
    if (!isObject(record) || !isString(record.recordId)) continue;
    if (!recordIdGroups.has(record.recordId)) recordIdGroups.set(record.recordId, []);
    recordIdGroups.get(record.recordId).push(record);
  }
  const conflictedRecordIds = new Set();
  for (const [recordId, group] of recordIdGroups) {
    if (group.length < 2) continue;
    const bodies = new Set(group.map((record) => canonicalStringify(record)));
    if (bodies.size > 1) {
      root(issues, 'A5_ACCEPTANCE_IMMUTABILITY_VIOLATION', recordId, 'evidence.acceptanceRecords');
      conflictedRecordIds.add(recordId);
    }
  }

  let passScenarios = 0;
  const uniqueRecords = [];
  const seenRecords = new Set();
  for (const record of records) {
    if (!isObject(record)) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', 'record', 'evidence.acceptanceRecords');
      continue;
    }
    if (conflictedScenarios.has(record.scenarioId)) continue;
    if (isString(record.recordId) && conflictedRecordIds.has(record.recordId)) continue;
    const body = canonicalStringify(record);
    if (seenRecords.has(body)) continue;
    seenRecords.add(body);
    uniqueRecords.push(record);
  }
  for (const record of uniqueRecords) {
    const id = isString(record.recordId) ? record.recordId : 'record';
    const keys = Object.keys(record);
    const shapeFields = ACCEPTANCE_RECORD_FIELDS.filter((key) => key !== 'authorityRef');
    if (keys.some((key) => !ACCEPTANCE_RECORD_FIELDS.includes(key)) ||
        shapeFields.some((key) => !Object.prototype.hasOwnProperty.call(record, key))) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', id, 'evidence.acceptanceRecords');
      continue;
    }
    // Closed string identity fields: a record can only bind to an admitted
    // manifest through non-empty string identities.
    if (!isString(record.recordId) || !isString(record.scenarioId) ||
        !isString(record.manifestId)) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', id, 'evidence.acceptanceRecords');
      continue;
    }
    if (!ACCEPTANCE_RECORD_RESULTS.includes(record.result)) {
      root(issues, 'A5_EVIDENCE_MANIFEST_INVALID', record.result, `record.${id}.result`);
      continue;
    }
    if (conflicted.has(record.manifestId)) continue;
    const manifest = manifestById.get(record.manifestId);
    if (!manifest) {
      root(issues, 'A5_ACCEPTANCE_EVIDENCE_MISSING', id, `record.${id}.manifestId`);
      continue;
    }
    // A record's scenarioId is part of its terminal identity: it must bind to
    // the very scenario of the manifest it references.
    if (record.scenarioId !== manifest.scenarioId) {
      root(issues, 'A5_ACCEPTANCE_IMMUTABILITY_VIOLATION', id, `record.${id}.scenarioId`);
      continue;
    }
    if (record.manifestDigest !== manifest.manifestDigest) {
      root(issues, 'A5_ACCEPTANCE_IMMUTABILITY_VIOLATION', id, `record.${id}.manifestDigest`);
      continue;
    }
    if (manifestState.get(record.manifestId) !== 'EVIDENCE_VALID') {
      root(issues, 'A5_ACCEPTANCE_EVIDENCE_MISSING', id, `record.${id}.manifestId`);
      continue;
    }
    const acceptedAt = parseIsoZ(record.acceptedAt);
    const manifestRecorded = parseIsoZ(manifest.recordedAt);
    if (acceptedAt === null || acceptedAt > asOf ||
        (manifestRecorded !== null && acceptedAt < manifestRecorded)) {
      root(issues, 'A5_EVIDENCE_TIME_INVALID', id, `record.${id}.acceptedAt`);
      continue;
    }
    // A structurally valid FAIL record drives the admitted manifest/scenario
    // to terminal FAIL; it grants nothing but must never be masked by an
    // EVIDENCE_VALID roll-up. Only PASS requires an approved authority.
    if (record.result === 'FAIL') {
      manifestState.set(record.manifestId, 'FAIL');
      continue;
    }
    if (Array.isArray(model.authorityRegistry) && model.authorityRegistry.length > 0) {
      root(issues, 'A5_ACCEPTANCE_AUTHORITY_INVALID', id, `record.${id}.authorityRef`);
      continue;
    }
    if (!isString(record.authorityRef)) {
      root(issues, 'A5_ACCEPTANCE_AUTHORITY_MISSING', id, `record.${id}.authorityRef`);
      continue;
    }
    // Empty registry (or no APPROVED row) means no authority can approve.
    root(issues, 'A5_ACCEPTANCE_AUTHORITY_MISSING', record.authorityRef, `record.${id}.authorityRef`);
  }

  // Aggregate derivation. productionAccepted is an output; a caller field can
  // never substitute. With the deliberately empty registry it stays false.
  const represented = (model.coverage || []).filter((row) => row.state === 'REPRESENTED');
  const forbidden = callerControlled.length > 0 ||
    ['productionAccepted', 'accepted', 'result', 'pass', 'execution']
      .some((key) => Object.prototype.hasOwnProperty.call(model, key));
  if (!forbidden && represented.length > 0) {
    const approved = new Map((model.authorityRegistry || [])
      .filter((row) => isObject(row) && row.status === 'APPROVED' &&
        isString(row.authorizationBasisRef))
      .map((row) => [row.authorityId, row]));
    const admittedScenarios = new Set();
    for (const [manifestId, state] of manifestState) {
      if (state !== 'EVIDENCE_VALID') continue;
      const manifest = manifestById.get(manifestId);
      if (manifest && scenarios.has(manifest.scenarioId)) admittedScenarios.add(manifest.scenarioId);
    }
    const passed = new Set();
    for (const record of uniqueRecords) {
      if (record.result !== 'PASS') continue;
      if (!approved.has(record.authorityRef)) continue;
      const manifest = manifestById.get(record.manifestId);
      if (!manifest || manifestState.get(record.manifestId) !== 'EVIDENCE_VALID') continue;
      if (record.manifestDigest !== manifest.manifestDigest) continue;
      passed.add(record.scenarioId);
    }
    const everyRepresented = represented.every((row) =>
      admittedScenarios.has(row.scenarioId) || passed.has(row.scenarioId));
    stats.productionAccepted = everyRepresented && represented.every((row) => passed.has(row.scenarioId));
    passScenarios = passed.size;
  }
  stats.passScenarios = passScenarios;
  if (stats.admitted > 0) stats.admittedState = 'EVIDENCE_VALID';
  if (stats.rejected > 0) stats.runtimeEvidence = 'EVIDENCE_REJECTED';
  else if (stats.blocked > 0) stats.runtimeEvidence = 'BLOCKED';
  else if (stats.manifests === 0) stats.runtimeEvidence = 'NOT_RUN';
  else if ([...manifestState.values()].includes('FAIL')) stats.runtimeEvidence = 'FAIL';
  else if (represented.length > 0 && (() => {
    const admittedScenarios = new Set();
    for (const [manifestId, state] of manifestState) {
      if (state !== 'EVIDENCE_VALID') continue;
      const manifest = manifestById.get(manifestId);
      if (manifest) admittedScenarios.add(manifest.scenarioId);
    }
    return represented.every((row) => admittedScenarios.has(row.scenarioId));
  })()) stats.runtimeEvidence = 'EVIDENCE_VALID';
  else stats.runtimeEvidence = 'EVIDENCE_SUBMITTED';
  return stats;
}

function checkDocLinks(docPath, readmePath, issues) {
  const doc = fs.readFileSync(docPath, 'utf8');
  const prose = doc.replace(/```[\s\S]*?```/g, '');
  const base = path.dirname(docPath);
  const linkRe = /\[[^\]]*\]\(([^)]+)\)/g;
  let match;
  while ((match = linkRe.exec(prose)) !== null) {
    const target = match[1].trim();
    if (!target || target.startsWith('#') || /^[a-z][a-z0-9+.-]*:/i.test(target)) continue;
    const filePart = target.split('#')[0];
    if (!filePart) continue;
    if (!fs.existsSync(path.resolve(base, filePart))) {
      root(issues, 'A5_SOURCE_REF_UNRESOLVED', target, 'docs/19.link');
    }
  }
  const readme = fs.readFileSync(readmePath, 'utf8');
  const navigation = '19-production-acceptance-matrix.md';
  const expectedRow =
    '| [19-production-acceptance-matrix.md](19-production-acceptance-matrix.md) | 生产验收矩阵、六轴覆盖与证据状态 | 架构、QA、安全、运维、产品 |';
  const rows = readme.split('\n').filter((line) => line.includes(`](${navigation})`));
  if (rows.length !== 1) {
    root(issues, 'A5_DOC_MODEL_DRIFT', 'docs19-row-count', 'docs/README.md');
  } else if (rows[0].trim() !== expectedRow) {
    root(issues, 'A5_DOC_MODEL_DRIFT', 'docs19-row-text', 'docs/README.md');
  }
  if (!fs.existsSync(path.resolve(path.dirname(readmePath), navigation))) {
    root(issues, 'A5_SOURCE_REF_UNRESOLVED', navigation, 'docs/README.md');
  }
}

async function evaluate(ctx, model, doc) {
  const phases = [];
  const rootPhase = [];
  validateRoot(ctx, model, rootPhase);
  phases.push(['root', rootPhase]);
  if (rootPhase.length) return finish(phases);
  const idPhase = [];
  validateIds(model, idPhase);
  phases.push(['ids', idPhase]);
  if (idPhase.length) return finish(phases);
  const requiredPhase = [];
  validateRequired(model, requiredPhase);
  phases.push(['required', requiredPhase]);
  if (requiredPhase.length) return finish(phases);
  const refPhase = [];
  validateRefs(ctx, model, refPhase);
  phases.push(['refs', refPhase]);
  if (refPhase.length) return finish(phases);
  const ownerPhase = [];
  validateOwners(ctx, model, ownerPhase);
  phases.push(['owners', ownerPhase]);
  if (ownerPhase.length) return finish(phases);
  const coveragePhase = [];
  validateCoverage(model, coveragePhase);
  phases.push(['coverage', coveragePhase]);
  if (coveragePhase.length) return finish(phases);
  const semanticPhase = [];
  validateSemantics(ctx, model, semanticPhase);
  phases.push(['semantics', semanticPhase]);
  if (semanticPhase.length) return finish(phases);
  const docPhase = [];
  validateDoc(model, doc, docPhase);
  phases.push(['doc', docPhase]);
  if (docPhase.length) return finish(phases);
  const baselinePhase = [];
  validateBaseline(ctx, model, baselinePhase);
  phases.push(['baseline', baselinePhase]);
  return finish(phases);
}

function finish(phases) {
  for (const [name, issues] of phases) {
    if (issues.length) return { phase: name, issues: sortIssues(issues) };
  }
  return { phase: null, issues: [] };
}
function sortIssues(issues) {
  const seen = new Set();
  return issues
    .filter((issue) => {
      const key = `${issue.code}\u0000${issue.path}\u0000${issue.id}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .sort((a, b) => {
      if (a.code !== b.code) return a.code.localeCompare(b.code);
      if (a.path !== b.path) return a.path.localeCompare(b.path);
      return a.id.localeCompare(b.id);
    });
}

function applyMutation(model, mutation) {
  const clone = structuredClone(model);
  switch (mutation.op) {
    case 'removeCoverageCell':
      clone.coverage = clone.coverage.filter((row) => !(row.unitId === mutation.unitId && row.axis === mutation.axis));
      break;
    case 'setCoverageCell': {
      const row = clone.coverage.find((item) => item.unitId === mutation.unitId && item.axis === mutation.axis);
      Object.assign(row, structuredClone(mutation.value));
      break;
    }
    case 'duplicateScenario': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      clone.scenarios.push(structuredClone(scenario));
      break;
    }
    case 'appendScenario':
      clone.scenarios.push(structuredClone(mutation.value));
      break;
    case 'setPath': {
      let target = clone;
      for (let i = 0; i < mutation.path.length - 1; i += 1) target = target[mutation.path[i]];
      target[mutation.path[mutation.path.length - 1]] = structuredClone(mutation.value);
      break;
    }
    case 'deletePath': {
      let target = clone;
      for (let i = 0; i < mutation.path.length - 1; i += 1) target = target[mutation.path[i]];
      delete target[mutation.path[mutation.path.length - 1]];
      break;
    }
    case 'pushPath': {
      let target = clone;
      for (const part of mutation.path) target = target[part];
      target.push(structuredClone(mutation.value));
      break;
    }
    case 'setScenario': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      let target = scenario;
      for (let i = 0; i < mutation.path.length - 1; i += 1) target = target[mutation.path[i]];
      if (mutation.value === undefined) delete target[mutation.path[mutation.path.length - 1]];
      else target[mutation.path[mutation.path.length - 1]] = structuredClone(mutation.value);
      break;
    }
    case 'pushScenarioField': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      let target = scenario;
      for (const part of mutation.path) target = target[part];
      target.push(structuredClone(mutation.value));
      break;
    }
    case 'setOwnerUpstream': {
      const owner = clone.owners.find((item) => item.ownerId === mutation.ownerId);
      owner.upstream = structuredClone(mutation.value);
      break;
    }
    case 'setAuthorityRegistry':
      clone.authorityRegistry = structuredClone(mutation.value);
      break;
    case 'setEvidencePolicy':
      clone.evidencePolicy = structuredClone(mutation.value);
      break;
    case 'pushResidual':
      clone.residuals.push(structuredClone(mutation.value));
      break;
    case 'setScenarioState': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      const step = scenario.stimulus.steps[mutation.stepIndex || 0];
      step.expectedState = structuredClone(mutation.expectedState);
      if (mutation.finalState) scenario.expected.persisted = structuredClone(mutation.finalState);
      break;
    }
    case 'setScenarioFinal': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      scenario.expected.persisted[mutation.index || 0] = structuredClone(mutation.value);
      break;
    }
    case 'setStepDelta': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      const delta = structuredClone(mutation.value);
      scenario.expected.effects.steps[mutation.stepIndex].delta = delta;
      const stimulusStep = scenario.stimulus.steps[mutation.stepIndex];
      if (stimulusStep) stimulusStep.expectedDelta = structuredClone(delta);
      break;
    }
    case 'setOverall': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      scenario.expected.effects.overall = structuredClone(mutation.value);
      break;
    }
    case 'setStepResponse': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      scenario.stimulus.steps[mutation.stepIndex].expectedResponse = structuredClone(mutation.value);
      break;
    }
    case 'appendBasisRef': {
      const scenario = clone.scenarios.find((item) => item.scenarioId === mutation.scenarioId);
      const assertion = scenario.stimulus.steps[mutation.stepIndex || 0].expectedState[mutation.stateIndex || 0];
      assertion.basisRefs.push(mutation.value);
      break;
    }
    default:
      throw new Error(`unknown fixture mutation: ${mutation.op}`);
  }
  return clone;
}

async function runFixtures(ctx, model, doc, fixtureDir, stage = 's1') {
  const specs = [
    ['s1-01-matrix-integrity.json', 'MODEL_MUTATION'],
    ['s1-02-expectation-safety.json', 'MODEL_MUTATION'],
    ['s1-03-a0-state-shapes.json', 'A0_STATE_SHAPE']
  ];
  if (stage === 's2') specs.push(['s2-01-evidence-admission.json', 'EVIDENCE_MANIFEST']);
  const results = [];
  let passed = 0;
  let failed = 0;
  let cases = 0;
  let negativeFiles = 0;
  const failures = [];
  for (const [file, kind] of specs) {
    const fixture = JSON.parse(fs.readFileSync(path.join(fixtureDir, file), 'utf8'));
    if (fixture.kind !== kind) {
      failures.push(`${file}: expected kind ${kind}, got ${fixture.kind}`);
      failed += 1;
      continue;
    }
    let fileFailed = 0;
    if (kind === 'MODEL_MUTATION') {
      for (const testCase of fixture.cases || []) {
        cases += 1;
        let mutated = model;
        for (const mutation of testCase.mutations || []) mutated = applyMutation(mutated, mutation);
        const result = await evaluate(ctx, mutated, doc);
        const codes = [...new Set(result.issues.map((issue) => issue.code))].sort();
        const expected = [...(testCase.expectedPrimaryCodes || [])].sort();
        const expectedExit = expected.length === 0 ? 0 : 1;
        const actualExit = codes.length === 0 ? 0 : 1;
        if (JSON.stringify(codes) === JSON.stringify(expected) && actualExit === expectedExit) {
          passed += 1;
        } else {
          failed += 1;
          fileFailed += 1;
          failures.push(`${testCase.caseId}: expected [${expected.join(',')}] exit=${expectedExit}, got [${codes.join(',')}] exit=${actualExit}`);
        }
      }
    } else if (kind === 'EVIDENCE_MANIFEST') {
      for (const testCase of fixture.cases || []) {
        cases += 1;
        let mutated = model;
        for (const mutation of testCase.mutations || []) mutated = applyMutation(mutated, mutation);
        const allIssues = [];
        if (!testCase.admissionOnly) {
          const s1 = await evaluate(ctx, mutated, doc);
          allIssues.push(...s1.issues);
        }
        validateS2Contract(mutated, allIssues);
        const stats = validateEvidence(ctx, mutated, testCase.evidence ?? null,
          { scenarioProfiles: testCase.scenarioProfiles }, allIssues);
        const codes = [...new Set(allIssues.map((issue) => issue.code))].sort();
        const expected = [...(testCase.expectedPrimaryCodes || [])].sort();
        const expectedExit = expected.length === 0 ? 0 : 1;
        const actualExit = codes.length === 0 ? 0 : 1;
        let ok = JSON.stringify(codes) === JSON.stringify(expected) && actualExit === expectedExit;
        const expectations = [
          ['expectAdmitted', 'admitted'],
          ['expectBlocked', 'blocked'],
          ['expectRuntimeEvidence', 'runtimeEvidence'],
          ['expectProductionAccepted', 'productionAccepted'],
          ['expectAdmittedState', 'admittedState']
        ];
        for (const [caseKey, statKey] of expectations) {
          if (testCase[caseKey] !== undefined && stats[statKey] !== testCase[caseKey]) ok = false;
        }
        if (ok) {
          passed += 1;
        } else {
          failed += 1;
          fileFailed += 1;
          failures.push(`${testCase.caseId}: expected [${expected.join(',')}] exit=${expectedExit}, got [${codes.join(',')}] exit=${actualExit} stats=${JSON.stringify(stats)}`);
        }
      }
    } else {
      for (const expectation of fixture.expectationsList || []) {
        cases += 1;
        const row = ctx.a0.stateRows.get(expectation.aggregate);
        let codes = [];
        if (expectation.negative) {
          try {
            const raw = ctx.a0.rawRows.find((item) => item.aggregate === expectation.aggregate);
            ctx.a0.parseStateRow({ ...raw, ...expectation.mutation });
          } catch (error) {
            codes = [error.code || 'A5_A0_STATE_SHAPE_UNRECOGNIZED'];
          }
          const expected = [...(expectation.expectedPrimaryCodes || [])].sort();
          if (JSON.stringify(codes) === JSON.stringify(expected)) passed += 1;
          else {
            failed += 1;
            fileFailed += 1;
            failures.push(`${expectation.caseId}: expected [${expected.join(',')}], got [${codes.join(',')}]`);
          }
          continue;
        }
        const expected = expectation.expected;
        const actual = {
          states: row.states,
          edges: row.edges,
          derivedStates: row.derivedStates,
          classification: row.classification,
          ...(expected.attachedNote !== undefined ? { attachedNote: row.attachedNote } : {})
        };
        const expectedComparable = {
          states: expected.states,
          edges: expected.edges,
          derivedStates: expected.derivedStates,
          classification: expected.classification,
          ...(expected.attachedNote !== undefined ? { attachedNote: expected.attachedNote } : {})
        };
        if (JSON.stringify(actual) === JSON.stringify(expectedComparable)) passed += 1;
        else {
          failed += 1;
          fileFailed += 1;
          failures.push(`${expectation.caseId}: A0 shape mismatch`);
        }
      }
    }
    const hasNegative = (fixture.cases || []).some((testCase) => (testCase.expectedPrimaryCodes || []).length > 0) ||
      (fixture.expectationsList || []).some((expectation) => expectation.negative);
    if (hasNegative) negativeFiles += 1;
    results.push({ file, cases: fixture.cases?.length || fixture.expectationsList?.length || 0, failed: fileFailed });
  }
  return { files: results.length, passed, failed, cases, negativeFiles, failures, results };
}

function parseArgs(argv) {
  const args = {};
  const knownValues = new Set([
    '--stage', '--matrix', '--doc', '--fixtures', '--a0', '--a1-bundle',
    '--a2-model', '--a2-fixtures', '--a3-model', '--a3-fixtures',
    '--a4-calc', '--a4-fixtures', '--evidence'
  ]);
  const knownFlags = new Set(['--check-doc-links']);
  for (let i = 0; i < argv.length; i += 1) {
    const key = argv[i];
    if (knownFlags.has(key)) {
      args[key.slice(2)] = true;
      continue;
    }
    if (!knownValues.has(key)) throw new Error(`unknown argument: ${key}`);
    const value = argv[i + 1];
    if (value === undefined || value.startsWith('--')) throw new Error(`missing value for ${key}`);
    args[key.slice(2)] = value;
    i += 1;
  }
  return args;
}

function failInvalid(message) {
  process.stdout.write('FAIL acceptance-validation (invalid-arguments)\n');
  process.stderr.write(`${message}\n`);
  process.exitCode = 2;
}

function failIssues(stage, issues, label = '') {
  process.stdout.write(`FAIL acceptance-validation (${stage}${label})\n`);
  for (const issue of sortIssues(issues)) {
    process.stderr.write(`ISSUE ${issue.code} ${issue.path} ${issue.id}\n`);
  }
  process.exitCode = 1;
}

async function main() {
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    failInvalid(error.message);
    return;
  }
  const required = ['stage', 'matrix', 'doc', 'fixtures', 'a0', 'a1-bundle', 'a2-model',
    'a2-fixtures', 'a3-model', 'a3-fixtures', 'a4-calc', 'a4-fixtures'];
  for (const key of required) {
    if (!args[key]) {
      failInvalid(`missing required argument: --${key}`);
      return;
    }
  }
  if (!['s1', 's2'].includes(args.stage)) {
    failInvalid(`unsupported stage: ${args.stage}`);
    return;
  }
  if (args.stage === 's1' && args.evidence) {
    failInvalid('--evidence is only valid with --stage s2');
    return;
  }
  if (args['check-doc-links'] && args.stage !== 's2') {
    failInvalid('--check-doc-links is only valid with --stage s2');
    return;
  }
  try {
    const inputFiles = [args['a1-bundle'], args.a0, args['a2-model'], args['a3-model'],
      args['a4-calc'], args.matrix, args.doc];
    if (args.evidence) inputFiles.push(args.evidence);
    for (const file of inputFiles) {
      if (!fs.existsSync(file)) throw new Error(`missing input file: ${file}`);
    }
    const a0Doc = fs.readFileSync(args.a0, 'utf8');
    const a1Bundle = JSON.parse(fs.readFileSync(args['a1-bundle'], 'utf8'));
    const a2Model = JSON.parse(fs.readFileSync(args['a2-model'], 'utf8'));
    const a3Model = JSON.parse(fs.readFileSync(args['a3-model'], 'utf8'));
    const docs17Path = path.join(path.dirname(args['a3-model']), '..', '..', 'docs', '17-production-security-design-spec.md');
    const docs18Path = path.join(path.dirname(args['a4-calc']), '..', '..', 'docs', '18-production-capacity-and-deployment-spec.md');
    const docs12Path = path.join(path.dirname(args['a4-calc']), '..', '..', 'docs', '12-technology-stack-and-system-architecture.md');
    const closeoutPath = path.join(path.dirname(args['a4-calc']), '..', '..', 'docs', 'gateflow', 'a4-production-capacity-deployment-spec-final-closeout.md');
    const templatePath = path.join(path.dirname(args['a4-calc']), 'inputs.template.json');
    const docs17 = fs.readFileSync(docs17Path, 'utf8');
    const docs18 = fs.readFileSync(docs18Path, 'utf8');
    const docs12 = fs.readFileSync(docs12Path, 'utf8');
    const closeoutDoc = fs.readFileSync(closeoutPath, 'utf8');
    const ctx = buildContext({
      a0: parseA0(a0Doc),
      a1: parseA1(a1Bundle),
      a2: parseA2(a2Model, args['a2-fixtures']),
      a3: parseA3(a3Model, docs17, args['a3-fixtures']),
      a3Model,
      a4: await parseA4(args['a4-calc'], templatePath, args['a4-fixtures'], docs18, closeoutDoc),
      doc12: parseDoc12(docs12)
    });
    const model = JSON.parse(fs.readFileSync(args.matrix, 'utf8'));
    const doc = fs.readFileSync(args.doc, 'utf8');
    const result = await evaluate(ctx, model, doc);
    const contractIssues = [];
    if (args.stage === 's2') validateS2Contract(model, contractIssues);
    const preIssues = [...result.issues, ...contractIssues];
    if (preIssues.length > 0) {
      failIssues(args.stage, preIssues);
      return;
    }
    const fixtureResult = await runFixtures(ctx, model, doc, args.fixtures, args.stage);
    if (fixtureResult.failed > 0) {
      process.stdout.write('FAIL acceptance-validation (fixture-failure)\n');
      for (const failure of fixtureResult.failures) process.stderr.write(`${failure}\n`);
      process.exitCode = 1;
      return;
    }
    let evidenceStats = null;
    if (args.stage === 's2') {
      let ledger = null;
      if (args.evidence) {
        try {
          ledger = JSON.parse(fs.readFileSync(args.evidence, 'utf8'));
        } catch (error) {
          failIssues('s2', [{ code: 'A5_EVIDENCE_MANIFEST_INVALID', path: 'evidence', id: error.message }]);
          return;
        }
      }
      const evidenceIssues = [];
      evidenceStats = validateEvidence(ctx, model, ledger, {}, evidenceIssues);
      if (evidenceIssues.length > 0) {
        failIssues('s2', evidenceIssues);
        return;
      }
      if (evidenceStats.productionAccepted) {
        failIssues('s2', [{ code: 'A5_ACCEPTANCE_AUTHORITY_INVALID', path: 'aggregate', id: 'productionAccepted' }]);
        return;
      }
      if (args['check-doc-links']) {
        const linkIssues = [];
        checkDocLinks(args.doc, path.join(path.dirname(args.doc), 'README.md'), linkIssues);
        if (linkIssues.length > 0) {
          failIssues('s2', linkIssues);
          return;
        }
      }
    }
    const counters = summarize(ctx, model, fixtureResult, args.stage, evidenceStats);
    process.stdout.write(`${counters}\nPASS acceptance-validation (${args.stage})\n`);
  } catch (error) {
    failInvalid(error.message);
  }
}

function summarize(ctx, model, fixtureResult, stage = 's1', evidenceStats = null) {
  const covered = (model.coverage || []).filter((row) => row.state === 'REPRESENTED').length;
  const na = (model.coverage || []).filter((row) => row.state === 'NOT_APPLICABLE').length;
  const deferred = (model.coverage || []).filter((row) => row.state === 'DEFERRED').length;
  const sourceRefs = (model.scenarios || []).reduce((sum, scenario) => sum + (scenario.sourceRefs?.length || 0), 0);
  const runtimeNotRun = (model.scenarios || []).every((scenario) => scenario.resultSemantics?.runtimeResult === 'NOT_RUN');
  const parts = [
    `stage=${stage}`,
    `matrix=${model.matrixId}`,
    'axes=6',
    'units=16',
    `cells=${model.coverage.length}`,
    `represented=${covered}`,
    `not_applicable=${na}`,
    `deferred=${deferred}`,
    `scenarios=${model.scenarios.length}`,
    `source_refs=${sourceRefs}`,
    'unresolved=0',
    'ambiguous=0',
    'drift=0',
    `a3_residuals=${ctx.a3.residuals.size}`,
    `a0_state_rows=${ctx.a0.stateRows.size}`,
    'a0_shape_unrecognized=0',
    'owner_upstream_unresolved=0',
    'residual_owner_unknown=0',
    'transition_ref_invalid=0',
    'path_family_duplicate=0',
    'external_projection_contradiction=0',
    'evidence_owner_missing=0',
    'coverage_missing=0',
    'coverage_duplicate=0',
    'coverage_illegal=0',
    'na_unbacked=0',
    'terminal_mutation=0',
    'duplicate_side_effect=0',
    'ordering_regression=0',
    'denied_with_business_effect=0',
    'caller_control=0',
    'synthetic_acceptance=0'
  ];
  if (stage === 's2' && evidenceStats) {
    parts.push(
      `evidence_manifests=${evidenceStats.manifests}`,
      `evidence_admitted=${evidenceStats.admitted}`,
      `acceptance_records=${evidenceStats.records}`,
      `blocked=${evidenceStats.blocked}`
    );
  }
  parts.push(
    `runtime_evidence=${stage === 's2' && evidenceStats
      ? evidenceStats.runtimeEvidence
      : (runtimeNotRun ? 'NOT_RUN' : 'INVALID')}`,
    `production_accepted=${stage === 's2' && evidenceStats
      ? (evidenceStats.productionAccepted ? 'true' : 'false')
      : 'false'}`,
    'spec_coverage=COMPLETE',
    `fixtures=${fixtureResult.files}`,
    `passed=${fixtureResult.files}`,
    'failed=0',
    `negative=${fixtureResult.negativeFiles}`,
    'deterministic=true'
  );
  return parts.join(' ');
}

main();
