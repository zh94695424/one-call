#!/usr/bin/env node
/**
 * A4 S1 capacity arithmetic and unknown propagation.
 *
 * Zero third-party dependencies, Node 22 ESM. `compute(input)` is deterministic
 * and never reads the wall clock, network, files or environment. S1 implements
 * docs/18 sections 1-6; S2 appends the backup/recovery and capability formulas
 * in sections 9-10. Caller capability assertions never change the fixed root
 * `cloudVerified=false` / `a3RiskClosed=false` constants.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SCHEMA_VERSION = 1;
const S1_GROUP_ID_LIST = [
  'ivrLines',
  'apiControlPlane',
  'measuredNodes',
  'playback',
  'mediaIngestStorage',
  'growthAndAsync',
  'spool'
];
const S2_GROUP_ID_LIST = [...S1_GROUP_ID_LIST, 'backupRecovery', 'capabilityEvidence'];
const GROUP_IDS = S2_GROUP_ID_LIST;

const STATUS_RANK = Object.freeze({
  COMPLETE: 0,
  NOT_VERIFIED: 1,
  INCOMPLETE: 2,
  NOT_MET: 3,
  INVALID: 4
});

const UNITS = new Set([
  'callsPerSecond',
  'seconds',
  'channels',
  'bridgePorts',
  'ratio',
  'count',
  'countPerDay',
  'requestsPerSecond',
  'requests',
  'transactionsPerSecond',
  'opsPerSecond',
  'messagesPerSecond',
  'connections',
  'sessions',
  'bitsPerSecond',
  'bytes',
  'bytesPerSecond',
  'bytesPerDay',
  'tasksPerSecond',
  'tasks',
  'transactions',
  'bytesPerTask',
  'nodes',
  'days',
  'hertz',
  'bit',
  'boolean',
  'enum',
  'iso8601'
]);

const PROVENANCES = new Set([
  'MISSING',
  'SYNTHETIC_TEST_ONLY',
  'MEASURED',
  'ESTIMATE',
  'APPROVED_POLICY'
]);

const APPROVALS = new Set(['MISSING', 'UNAPPROVED', 'APPROVED', 'SYNTHETIC_TEST_ONLY']);

const META_STATUSES = new Set([
  'UNAPPROVED_TEMPLATE',
  'SYNTHETIC_TEST_ONLY',
  'APPROVED_SOURCE_INPUTS'
]);

const CALLER_CONTROL_KEYS = [
  'productionReady',
  'procurementReady',
  'approved',
  'cloudVerified',
  'a3RiskClosed'
];

const ROLE_DEFINITIONS = Object.freeze({
  'core-api': {
    kind: 'stateless',
    demandUnit: 'requestsPerSecond',
    demandLabel: 'API/控制面请求率'
  },
  'async-worker': {
    kind: 'stateless',
    demandUnit: 'tasksPerSecond',
    demandLabel: '异步任务到达率'
  },
  'media-ai-worker': {
    kind: 'stateless',
    demandUnit: 'tasksPerSecond',
    demandLabel: '媒体/AI 任务到达率'
  },
  postgres: {
    kind: 'stateful',
    demandUnit: 'transactionsPerSecond',
    demandLabel: '数据库事务率'
  },
  redis: {
    kind: 'stateful',
    demandUnit: 'opsPerSecond',
    demandLabel: '缓存操作率'
  },
  rabbitmq: {
    kind: 'stateful',
    demandUnit: 'messagesPerSecond',
    demandLabel: '消息吞吐'
  },
  'media-gateway': {
    kind: 'gateway',
    demandUnit: 'sessions',
    demandLabel: '并发播放会话'
  }
});

const REQUIRED_ROLES = ['core-api', 'async-worker', 'postgres', 'redis', 'rabbitmq', 'media-gateway'];

const IVR_MIX_KEYS = ['task', 'noTaskDtmf', 'activation', 'rejected', 'failed'];
const API_RATE_KEYS = ['customer', 'operations', 'ivrControl', 'publicShare', 'providerCallback'];
const PEAK_KINDS = ['inbound', 'outbound', 'shared', 'bridge'];
const DERIVED_TYPES = ['preview', 'transcript', 'summary', 'export'];
const META_TYPES = ['audit', 'outbox', 'callEvent', 'evidenceEvent', 'shareAccess'];
const DB_AUX_KINDS = ['index', 'wal', 'replication', 'backup'];
const BACKUP_DEPENDENCY_IDS = [
  'postgresPitr',
  'cosVersions',
  'kmsKeyVersions',
  'secretVersions',
  'reconciliationOutbox',
  'ivrLocalFacts'
];
const CAPABILITY_IDS = ['CV-1', 'CV-2', 'CV-3', 'CV-4', 'CV-5', 'regionAzSku'];
const BACKUP_DEPENDENCY_FIELDS = new Set([
  'dependencyId',
  'included',
  'approvedRpoSeconds',
  'approvedRtoSeconds',
  'restoreTestedAt',
  'observedRpoSeconds',
  'observedRtoSeconds',
  'owner',
  'evidenceRef'
]);
const CAPABILITY_FIELDS = new Set([
  'capabilityId',
  'status',
  'claimedBy',
  'owner',
  'source',
  'evidenceRef',
  'verifiedAt'
]);
const CAPABILITY_LIMITATIONS = [
  'DEPLOYMENT_EVIDENCE_INCOMPLETE',
  'NO_CLOUD_VERIFICATION',
  'A3_RISK_NOT_CLOSED'
];

const ROLE_COMMON_FIELDS = new Set([
  'role',
  'demandUnit',
  'headroomRatio',
  'measuredCpuUtilization',
  'measuredRamUtilization',
  'measuredConnectionsUtilization',
  'measuredIopsUtilization',
  'resourceEvidenceRef',
  'measuredAt'
]);
const ROLE_FIELDS = Object.freeze({
  stateless: new Set([...ROLE_COMMON_FIELDS, 'demandQuantity', 'perNodeRate', 'deployedNodes']),
  stateful: new Set([...ROLE_COMMON_FIELDS, 'demandQuantity', 'measuredNormalTopologyCapacity', 'measuredDegradedTopologyCapacity']),
  gateway: new Set([...ROLE_COMMON_FIELDS, 'demandQuantity', 'sharedCoreApiEdgeCapacity'])
});

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function unique(values) {
  return [...new Set(values)];
}

function hasOwn(object, key) {
  return Object.prototype.hasOwnProperty.call(object, key);
}

function rank(status) {
  return STATUS_RANK[status] ?? STATUS_RANK.INVALID;
}

function worst(statuses) {
  if (statuses.length === 0) return 'INCOMPLETE';
  return statuses.reduce((acc, status) => (rank(status) > rank(acc) ? status : acc), 'COMPLETE');
}

function addError(ctx, code, errorPath) {
  const key = `${code}|${errorPath}`;
  if (ctx.errorKeys.has(key)) return;
  ctx.errorKeys.add(key);
  ctx.errors.push({ code, path: errorPath });
}

function dateValid(value) {
  return typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Date.parse(value));
}

function scanSynthetic(value, seen = new Set()) {
  if (value === null || typeof value !== 'object') return false;
  if (seen.has(value)) return false;
  seen.add(value);
  if (isPlainObject(value) && value.provenance === 'SYNTHETIC_TEST_ONLY') return true;
  return Object.values(value).some((entry) => scanSynthetic(entry, seen));
}

function scanLeaves(value, visitor, currentPath = '', seen = new Set()) {
  if (value === null || typeof value !== 'object') return;
  if (seen.has(value)) return;
  seen.add(value);
  if (isPlainObject(value) && hasOwn(value, 'value') && hasOwn(value, 'unit') && hasOwn(value, 'provenance')) {
    visitor(value, currentPath);
  }
  if (Array.isArray(value)) {
    value.forEach((entry, index) => scanLeaves(entry, visitor, `${currentPath}[${index}]`, seen));
    return;
  }
  for (const [key, entry] of Object.entries(value)) {
    scanLeaves(entry, visitor, currentPath ? `${currentPath}.${key}` : key, seen);
  }
}

function readMeta(ctx, input) {
  const meta = input.meta;
  if (meta === undefined || meta === null) {
    return { status: 'UNAPPROVED_TEMPLATE' };
  }
  if (!isPlainObject(meta)) {
    addError(ctx, 'TYPE_INVALID', 'meta');
    return { status: 'UNAPPROVED_TEMPLATE' };
  }
  if (hasOwn(meta, 'status')) {
    if (typeof meta.status !== 'string' || !META_STATUSES.has(meta.status)) {
      addError(ctx, 'TYPE_INVALID', 'meta.status');
    }
  }
  const status = META_STATUSES.has(meta.status) ? meta.status : 'UNAPPROVED_TEMPLATE';
  if (status === 'APPROVED_SOURCE_INPUTS') {
    const sourceOk = typeof meta.source === 'string' && meta.source.trim() !== '';
    const approverOk = typeof meta.approvedBy === 'string' && meta.approvedBy.trim() !== '';
    const dateOk = dateValid(meta.approvedAt);
    if (!sourceOk || !approverOk || !dateOk) {
      addError(ctx, 'APPROVAL_INVALID', 'meta');
    }
  }
  return { status };
}

function readGroup(ctx, input, groupId) {
  const raw = input[groupId];
  if (raw === undefined || raw === null) return { value: {}, invalidPath: null };
  if (!isPlainObject(raw)) {
    addError(ctx, 'TYPE_INVALID', groupId);
    return { value: {}, invalidPath: groupId };
  }
  return { value: raw, invalidPath: null };
}

function checkUnknownKeys(ctx, object, allowed, basePath, invalidOut) {
  if (!isPlainObject(object)) return;
  for (const key of Object.keys(object)) {
    if (!allowed.has(key)) {
      addError(ctx, 'UNKNOWN_KEY', `${basePath}.${key}`);
      invalidOut.push(`${basePath}.${key}`);
    }
  }
}

function invalidate(rec, ctx, code) {
  rec.invalid = true;
  rec.reasons.push(code);
  addError(ctx, code, rec.path);
}

function validateKind(rec, ctx, value, kind, spec) {
  if (kind === null || kind === undefined) return;
  const fail = (code) => invalidate(rec, ctx, code);
  if (kind === 'DiscreteCount') {
    if (value < 0) fail('NEGATIVE_VALUE');
    else if (!Number.isInteger(value)) fail('COUNT_NOT_INTEGER');
    else if (!Number.isSafeInteger(value)) fail('OVERFLOW_UNSAFE_INTEGER');
    return;
  }
  if (kind === 'Ratio01') {
    if (value < 0) fail('NEGATIVE_VALUE');
    else if (value > 1) fail('RATIO_OUT_OF_RANGE');
    return;
  }
  if (kind === 'WireOverheadRatio') {
    if (value < 0) fail('NEGATIVE_VALUE');
    else if (value >= 1) fail('RATIO_OUT_OF_RANGE');
    return;
  }
  if (kind === 'VersionFactor') {
    if (value < 0) fail('NEGATIVE_VALUE');
    else if (value < 1) fail('RATIO_OUT_OF_RANGE');
    return;
  }
  if (kind === 'CopyFactor' || kind === 'DerivedByteRatio') {
    if (value < 0) fail('NEGATIVE_VALUE');
    return;
  }
  // UtilizationRatio (derived only), NonNegativeRate, NonNegativeMeasure,
  // DailyVolume and DurationSeconds all require x >= 0.
  if (value < 0) fail('NEGATIVE_VALUE');
  if (spec && spec.positive && value === 0) fail('DIVISION_BY_ZERO');
}

function readLeaf(ctx, parent, key, leafPath, spec) {
  const rec = {
    path: leafPath,
    present: false,
    leaf: null,
    value: null,
    missing: false,
    invalid: false,
    usable: false,
    provenance: 'MISSING',
    approval: 'MISSING',
    reasons: [],
    spec
  };
  if (!isPlainObject(parent) || !hasOwn(parent, key)) {
    rec.missing = true;
    return rec;
  }
  rec.present = true;
  const leaf = parent[key];
  rec.leaf = leaf;
  if (!isPlainObject(leaf)) {
    invalidate(rec, ctx, 'TYPE_INVALID');
    rec.missing = true;
    return rec;
  }
  const provenance = leaf.provenance === undefined ? 'MISSING' : leaf.provenance;
  const approval = leaf.approval === undefined ? 'MISSING' : leaf.approval;
  if (typeof provenance !== 'string' || !PROVENANCES.has(provenance)) {
    invalidate(rec, ctx, 'PROVENANCE_INVALID');
  }
  if (typeof approval !== 'string' || !APPROVALS.has(approval)) {
    invalidate(rec, ctx, 'APPROVAL_INVALID');
  }
  rec.provenance = provenance;
  rec.approval = approval;
  if (spec.unit !== undefined && leaf.unit !== spec.unit) {
    invalidate(rec, ctx, 'UNIT_MISMATCH');
  }
  const hasValue = leaf.value !== undefined && leaf.value !== null;
  if (hasValue) {
    if (spec.type === 'number') {
      if (typeof leaf.value !== 'number') {
        invalidate(rec, ctx, 'TYPE_INVALID');
      } else if (!Number.isFinite(leaf.value)) {
        invalidate(rec, ctx, 'NUMBER_NONFINITE');
      } else {
        if (Math.abs(leaf.value) > Number.MAX_SAFE_INTEGER) {
          invalidate(rec, ctx, 'OVERFLOW_UNSAFE_INTEGER');
        }
        validateKind(rec, ctx, leaf.value, spec.kind, spec);
        rec.value = leaf.value;
      }
    } else if (spec.type === 'string') {
      if (typeof leaf.value !== 'string') {
        invalidate(rec, ctx, 'TYPE_INVALID');
      } else {
        if (spec.enumValues && !spec.enumValues.includes(leaf.value)) {
          invalidate(rec, ctx, 'TYPE_INVALID');
        }
        rec.value = leaf.value;
      }
    } else if (spec.type === 'boolean') {
      if (typeof leaf.value !== 'boolean') {
        invalidate(rec, ctx, 'TYPE_INVALID');
      } else {
        rec.value = leaf.value;
      }
    } else {
      invalidate(rec, ctx, 'TYPE_INVALID');
    }
  }
  validateLeafMetadata(rec, ctx, leaf, provenance, approval, hasValue);
  if (!hasValue || provenance === 'MISSING') {
    rec.missing = true;
  }
  rec.usable = !rec.invalid && !rec.missing;
  return rec;
}

function validateLeafMetadata(rec, ctx, leaf, provenance, approval, hasValue) {
  const sourceOk = typeof leaf.source === 'string' && leaf.source.trim() !== '';
  const ownerOk = typeof leaf.owner === 'string' && leaf.owner.trim() !== '';
  if (provenance === 'MEASURED' && (!sourceOk || !ownerOk)) {
    invalidate(rec, ctx, 'PROVENANCE_INVALID');
  }
  if (provenance === 'APPROVED_POLICY' && (!sourceOk || !ownerOk || approval !== 'APPROVED')) {
    invalidate(rec, ctx, 'APPROVAL_INVALID');
  }
  if (provenance === 'SYNTHETIC_TEST_ONLY' && approval !== 'SYNTHETIC_TEST_ONLY') {
    invalidate(rec, ctx, 'APPROVAL_INVALID');
  }
  if (approval === 'SYNTHETIC_TEST_ONLY' && provenance !== 'SYNTHETIC_TEST_ONLY') {
    invalidate(rec, ctx, 'APPROVAL_INVALID');
  }
  if (approval === 'APPROVED') {
    const provenanceOk = provenance === 'MEASURED' || provenance === 'APPROVED_POLICY';
    if (!provenanceOk || !sourceOk || !ownerOk || !hasValue) {
      invalidate(rec, ctx, 'APPROVAL_INVALID');
    }
  }
  if (provenance === 'MISSING' && approval === 'APPROVED') {
    invalidate(rec, ctx, 'APPROVAL_INVALID');
  }
}

function approvalStatusFor(used) {
  const present = used.filter((rec) => rec && rec.present);
  if (used.some((rec) => rec && rec.invalid)) return 'UNAPPROVED';
  if (present.length === 0) return 'MISSING';
  if (used.some((rec) => rec && rec.missing)) {
    if (present.every((rec) => rec.provenance === 'SYNTHETIC_TEST_ONLY')) return 'SYNTHETIC_TEST_ONLY';
    return 'UNAPPROVED';
  }
  if (present.every((rec) => rec.provenance === 'SYNTHETIC_TEST_ONLY')) {
    return 'SYNTHETIC_TEST_ONLY';
  }
  if (
    present.every(
      (rec) =>
        rec.approval === 'APPROVED' &&
        (rec.provenance === 'MEASURED' || rec.provenance === 'APPROVED_POLICY')
    )
  ) {
    return 'APPROVED';
  }
  return 'UNAPPROVED';
}

function makeMetric(ctx, options) {
  const missing = unique(options.missing ?? []);
  const invalid = unique(options.invalid ?? []);
  const reasons = unique(options.reasons ?? []);
  const limitations = unique(options.limitations ?? []);
  const used = options.used ?? [];
  let status = options.status;
  if (!status) {
    if (invalid.length > 0) status = 'INVALID';
    else if (missing.length > 0) status = 'INCOMPLETE';
    else status = 'COMPLETE';
  }
  const approvalStatus = options.approvalStatus ?? approvalStatusFor(used);
  if (approvalStatus !== 'APPROVED') {
    limitations.push('NOT_APPROVED_PRODUCTION_VALUE');
  }
  const value = options.value === undefined ? null : options.value;
  const metric = {
    id: options.id,
    formulaId: options.formulaId,
    status,
    unit: options.unit ?? null,
    value,
    missing,
    invalid,
    reasons,
    limitations: unique(limitations),
    approvalStatus
  };
  Object.defineProperty(metric, 'used', { value: used, enumerable: false });
  return metric;
}

function mergeRecs(metrics) {
  return metrics.flatMap((metric) => metric.used ?? []);
}

function collectMissing(records) {
  return unique(records.filter((rec) => rec && rec.missing).map((rec) => rec.path));
}

function collectInvalid(records) {
  return unique(records.filter((rec) => rec && rec.invalid).map((rec) => rec.path));
}

function invalidReasons(records) {
  return unique(records.flatMap((rec) => (rec ? rec.reasons : [])));
}

function multiply(records, ctx, metricPath, options = {}) {
  const invalid = collectInvalid(records);
  const missing = collectMissing(records);
  const reasons = invalidReasons(records);
  const used = records.filter(Boolean);
  if (invalid.length > 0) {
    return { status: 'INVALID', value: null, missing, invalid, reasons, used };
  }
  if (options.allowZeroShortCircuit !== false) {
    const zero = used.find((rec) => !rec.missing && !rec.invalid && rec.value === 0);
    if (zero) {
      return {
        status: 'COMPLETE',
        value: 0,
        missing,
        invalid: [],
        reasons,
        used,
        limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
      };
    }
  }
  if (missing.length > 0) {
    return { status: 'INCOMPLETE', value: null, missing, invalid: [], reasons, used };
  }
  let value = 1;
  for (const rec of used) value *= rec.value;
  if (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER) {
    addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', metricPath);
    return {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: [metricPath],
      reasons: ['OVERFLOW_UNSAFE_INTEGER'],
      used
    };
  }
  return { status: 'COMPLETE', value, missing: [], invalid: [], reasons, used };
}

function addRecords(left, right) {
  return [...left, ...right];
}

function derivedOperand(metric) {
  if (!metric) return { status: 'INCOMPLETE', value: null, missing: ['<derived>'], used: [] };
  return {
    status: metric.status,
    value: metric.status === 'COMPLETE' ? metric.value : null,
    missing: metric.missing,
    invalid: metric.invalid,
    used: metric.used ?? []
  };
}

function multiplyOperands(operands, ctx, metricPath) {
  const invalid = unique(operands.flatMap((op) => op.invalid ?? []));
  const missing = unique(operands.flatMap((op) => op.missing ?? []));
  const used = operands.flatMap((op) => op.used ?? []);
  const reasons = unique(
    operands.flatMap((op) => (op.status === 'INVALID' ? op.reasons ?? [] : []))
  );
  if (invalid.length > 0) {
    return { status: 'INVALID', value: null, missing, invalid, reasons, used };
  }
  const zero = operands.find((op) => op.status === 'COMPLETE' && op.value === 0);
  if (zero) {
    return {
      status: 'COMPLETE',
      value: 0,
      missing,
      invalid: [],
      reasons,
      used,
      limitations: ['ZERO_FACTOR_SHORT_CIRCUIT'],
      shortCircuited: true
    };
  }
  if (operands.some((op) => op.status !== 'COMPLETE')) {
    return { status: 'INCOMPLETE', value: null, missing, invalid: [], reasons, used };
  }
  let value = 1;
  for (const op of operands) value *= op.value;
  if (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER) {
    addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', metricPath);
    return {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: [metricPath],
      reasons: ['OVERFLOW_UNSAFE_INTEGER'],
      used
    };
  }
  return { status: 'COMPLETE', value, missing: [], invalid: [], reasons, used };
}

function sumOperands(operands, ctx, metricPath) {
  const invalid = unique(operands.flatMap((op) => op.invalid ?? []));
  const missing = unique(operands.flatMap((op) => op.missing ?? []));
  const used = operands.flatMap((op) => op.used ?? []);
  const reasons = unique(
    operands.flatMap((op) => (op.status === 'INVALID' ? op.reasons ?? [] : []))
  );
  if (invalid.length > 0) {
    return { status: 'INVALID', value: null, missing, invalid, reasons, used };
  }
  if (missing.length > 0 || operands.some((op) => op.status !== 'COMPLETE')) {
    return { status: 'INCOMPLETE', value: null, missing, invalid: [], reasons, used };
  }
  const value = operands.reduce((acc, op) => acc + op.value, 0);
  if (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER) {
    addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', metricPath);
    return {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: [metricPath],
      reasons: ['OVERFLOW_UNSAFE_INTEGER'],
      used
    };
  }
  return { status: 'COMPLETE', value, missing: [], invalid: [], reasons, used };
}

function groupResult(id, formulaIds, unit, metrics, extraMissing = [], extraInvalid = [], extraLimitations = []) {
  const missing = unique([...metrics.flatMap((metric) => metric.missing), ...extraMissing]);
  const invalid = unique([...metrics.flatMap((metric) => metric.invalid), ...extraInvalid]);
  const limitations = unique([...metrics.flatMap((metric) => metric.limitations), ...extraLimitations]);
  let status = worst(metrics.map((metric) => metric.status));
  if (invalid.length > 0) status = 'INVALID';
  const partial =
    metrics.some((metric) => metric.status === 'COMPLETE') &&
    metrics.some((metric) => metric.status !== 'COMPLETE');
  return {
    id,
    status,
    partial,
    formulaIds,
    unit: unit ?? null,
    metrics,
    missing,
    invalid,
    limitations
  };
}

function metricById(metrics, id) {
  return metrics.find((metric) => metric.id === id) ?? null;
}

function readPeak(ctx, peaks, kind, basePath) {
  const result = {
    kind,
    present: false,
    valid: false,
    metric: null,
    count: null,
    window: null,
    scope: null
  };
  if (!isPlainObject(peaks) || !hasOwn(peaks, kind)) return result;
  result.present = true;
  const entry = peaks[kind];
  const entryPath = `${basePath}.${kind}`;
  if (!isPlainObject(entry)) {
    addError(ctx, 'TYPE_INVALID', entryPath);
    result.metric = makeMetric(ctx, {
      id: `ivrLines.observedPeak.${kind}`,
      formulaId: 'F-IVR-05a',
      unit: 'count',
      status: 'INVALID',
      invalid: [entryPath],
      reasons: ['TYPE_INVALID'],
      used: []
    });
    return result;
  }
  const count = readLeaf(ctx, entry, 'count', `${entryPath}.count`, {
    type: 'number',
    unit: 'count',
    kind: 'DiscreteCount'
  });
  const window = readLeaf(ctx, entry, 'windowSeconds', `${entryPath}.windowSeconds`, {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  let scopeOk = typeof entry.scope === 'string' && entry.scope.trim() !== '';
  const scopeMalformed =
    hasOwn(entry, 'scope') && entry.scope !== null && entry.scope !== undefined && typeof entry.scope !== 'string';
  if (scopeMalformed) {
    addError(ctx, 'TYPE_INVALID', `${entryPath}.scope`);
    scopeOk = false;
  }
  const missing = [
    ...collectMissing([count, window]),
    ...(scopeOk ? [] : [`${entryPath}.scope`])
  ];
  const invalid = [
    ...collectInvalid([count, window]),
    ...(scopeMalformed ? [`${entryPath}.scope`] : [])
  ];
  const reasons = invalidReasons([count, window]);
  if (scopeMalformed) reasons.push('TYPE_INVALID');
  const complete = count.usable && window.usable && scopeOk;
  result.count = count;
  result.window = window;
  result.scope = scopeOk ? entry.scope : null;
  result.valid = complete;
  result.metric = makeMetric(ctx, {
    id: `ivrLines.observedPeak.${kind}`,
    formulaId: 'F-IVR-05a',
    unit: 'count',
    status: invalid.length > 0 ? 'INVALID' : missing.length > 0 ? 'INCOMPLETE' : 'COMPLETE',
    value: complete ? count.value : null,
    missing,
    invalid,
    reasons,
    used: [count, window]
  });
  return result;
}

function ivrProvenanceQualified(rec) {
  return rec && !rec.missing && !rec.invalid && rec.provenance === 'MEASURED';
}

function policyOrMeasured(rec) {
  return (
    rec &&
    !rec.missing &&
    !rec.invalid &&
    (rec.provenance === 'MEASURED' || rec.provenance === 'APPROVED_POLICY')
  );
}

function computeIvrLines(ctx, input) {
  const read = readGroup(ctx, input, 'ivrLines');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  checkUnknownKeys(
    ctx,
    group,
    new Set([
      'IVR_IN_RATE_PS',
      'IVR_IN_HOLD_S',
      'IVR_OUT_RATE_PS',
      'IVR_OUT_OCC_S',
      'IVR_OUT_RING_S',
      'IVR_BRIDGE_RATE_PS',
      'IVR_BRIDGE_MEAN_S',
      'IVR_TRUNK_MODE',
      'IVR_IN_LIMIT_CH',
      'IVR_OUT_LIMIT_CH',
      'IVR_SHARED_LIMIT_CH',
      'IVR_FAILOVER_HEADROOM_RATIO',
      'IVR_MIX_RATE_PS',
      'IVR_OBSERVED_PEAK'
    ]),
    'ivrLines',
    extraInvalid
  );
  const inRate = readLeaf(ctx, group, 'IVR_IN_RATE_PS', 'ivrLines.IVR_IN_RATE_PS', {
    type: 'number',
    unit: 'callsPerSecond',
    kind: 'NonNegativeRate'
  });
  const inHold = readLeaf(ctx, group, 'IVR_IN_HOLD_S', 'ivrLines.IVR_IN_HOLD_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const outRate = readLeaf(ctx, group, 'IVR_OUT_RATE_PS', 'ivrLines.IVR_OUT_RATE_PS', {
    type: 'number',
    unit: 'callsPerSecond',
    kind: 'NonNegativeRate'
  });
  const outOcc = readLeaf(ctx, group, 'IVR_OUT_OCC_S', 'ivrLines.IVR_OUT_OCC_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const outRing = readLeaf(ctx, group, 'IVR_OUT_RING_S', 'ivrLines.IVR_OUT_RING_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const bridgeRate = readLeaf(ctx, group, 'IVR_BRIDGE_RATE_PS', 'ivrLines.IVR_BRIDGE_RATE_PS', {
    type: 'number',
    unit: 'callsPerSecond',
    kind: 'NonNegativeRate'
  });
  const bridgeMean = readLeaf(ctx, group, 'IVR_BRIDGE_MEAN_S', 'ivrLines.IVR_BRIDGE_MEAN_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const mode = readLeaf(ctx, group, 'IVR_TRUNK_MODE', 'ivrLines.IVR_TRUNK_MODE', {
    type: 'string',
    unit: 'enum',
    enumValues: ['SEPARATE_INBOUND_OUTBOUND', 'SHARED']
  });
  const inLimit = readLeaf(ctx, group, 'IVR_IN_LIMIT_CH', 'ivrLines.IVR_IN_LIMIT_CH', {
    type: 'number',
    unit: 'channels',
    kind: 'DiscreteCount'
  });
  const outLimit = readLeaf(ctx, group, 'IVR_OUT_LIMIT_CH', 'ivrLines.IVR_OUT_LIMIT_CH', {
    type: 'number',
    unit: 'channels',
    kind: 'DiscreteCount'
  });
  const sharedLimit = readLeaf(ctx, group, 'IVR_SHARED_LIMIT_CH', 'ivrLines.IVR_SHARED_LIMIT_CH', {
    type: 'number',
    unit: 'channels',
    kind: 'DiscreteCount'
  });
  const headroom = readLeaf(
    ctx,
    group,
    'IVR_FAILOVER_HEADROOM_RATIO',
    'ivrLines.IVR_FAILOVER_HEADROOM_RATIO',
    { type: 'number', unit: 'ratio', kind: 'Ratio01' }
  );
  const mix = {};
  for (const key of IVR_MIX_KEYS) {
    const mixObject = isPlainObject(group.IVR_MIX_RATE_PS) ? group.IVR_MIX_RATE_PS : null;
    mix[key] = readLeaf(ctx, mixObject, key, `ivrLines.IVR_MIX_RATE_PS.${key}`, {
      type: 'number',
      unit: 'callsPerSecond',
      kind: 'NonNegativeRate'
    });
  }
  if (group.IVR_MIX_RATE_PS !== undefined && !isPlainObject(group.IVR_MIX_RATE_PS)) {
    addError(ctx, 'TYPE_INVALID', 'ivrLines.IVR_MIX_RATE_PS');
    extraInvalid.push('ivrLines.IVR_MIX_RATE_PS');
  } else if (isPlainObject(group.IVR_MIX_RATE_PS)) {
    checkUnknownKeys(ctx, group.IVR_MIX_RATE_PS, new Set(IVR_MIX_KEYS), 'ivrLines.IVR_MIX_RATE_PS', extraInvalid);
  }
  const peaks = {};
  if (group.IVR_OBSERVED_PEAK !== undefined && !isPlainObject(group.IVR_OBSERVED_PEAK)) {
    addError(ctx, 'TYPE_INVALID', 'ivrLines.IVR_OBSERVED_PEAK');
    extraInvalid.push('ivrLines.IVR_OBSERVED_PEAK');
  } else if (isPlainObject(group.IVR_OBSERVED_PEAK)) {
    checkUnknownKeys(ctx, group.IVR_OBSERVED_PEAK, new Set(PEAK_KINDS), 'ivrLines.IVR_OBSERVED_PEAK', extraInvalid);
  }
  for (const kind of PEAK_KINDS) {
    peaks[kind] = readPeak(ctx, group.IVR_OBSERVED_PEAK, kind, 'ivrLines.IVR_OBSERVED_PEAK');
  }

  const metrics = [];

  const a = multiply([inRate, inHold], ctx, 'ivrLines.a');
  metrics.push(
    makeMetric(ctx, {
      id: 'ivrLines.a',
      formulaId: 'F-IVR-01',
      unit: 'channels',
      ...a,
      limitations: [...(a.limitations ?? []), 'AVERAGE_NOT_PEAK']
    })
  );
  const b = multiply([outRate, outOcc], ctx, 'ivrLines.b');
  metrics.push(
    makeMetric(ctx, {
      id: 'ivrLines.b',
      formulaId: 'F-IVR-02',
      unit: 'channels',
      ...b,
      limitations: [...(b.limitations ?? []), 'AVERAGE_NOT_PEAK']
    })
  );
  const bridge = multiply([bridgeRate, bridgeMean], ctx, 'ivrLines.bridgePorts');
  metrics.push(
    makeMetric(ctx, {
      id: 'ivrLines.bridgePorts',
      formulaId: 'F-IVR-03',
      unit: 'bridgePorts',
      ...bridge,
      limitations: [...(bridge.limitations ?? []), 'AVERAGE_NOT_PEAK']
    })
  );

  const modePathIssues = mode.reasons;
  if (mode.missing) {
    metrics.push(
      makeMetric(ctx, {
        id: 'ivrLines.mode',
        formulaId: 'F-IVR-04',
        unit: 'enum',
        status: 'INCOMPLETE',
        missing: [mode.path],
        used: [mode]
      })
    );
  } else if (mode.invalid) {
    metrics.push(
      makeMetric(ctx, {
        id: 'ivrLines.mode',
        formulaId: 'F-IVR-04',
        unit: 'enum',
        status: 'INVALID',
        invalid: [mode.path],
        reasons: modePathIssues,
        used: [mode]
      })
    );
  } else if (mode.value === 'SEPARATE_INBOUND_OUTBOUND' && !sharedLimit.missing) {
    addError(ctx, 'MODE_LIMIT_MISMATCH', 'ivrLines.IVR_SHARED_LIMIT_CH');
    metrics.push(
      makeMetric(ctx, {
        id: 'ivrLines.mode',
        formulaId: 'F-IVR-04',
        unit: 'enum',
        status: 'INVALID',
        value: mode.value,
        invalid: ['ivrLines.IVR_SHARED_LIMIT_CH'],
        reasons: ['MODE_LIMIT_MISMATCH'],
        used: [mode, sharedLimit]
      })
    );
  } else if (mode.value === 'SHARED' && (!inLimit.missing || !outLimit.missing)) {
    addError(ctx, 'MODE_LIMIT_MISMATCH', 'ivrLines.IVR_IN_LIMIT_CH');
    metrics.push(
      makeMetric(ctx, {
        id: 'ivrLines.mode',
        formulaId: 'F-IVR-04',
        unit: 'enum',
        status: 'INVALID',
        value: mode.value,
        invalid: ['ivrLines.IVR_IN_LIMIT_CH'],
        reasons: ['MODE_LIMIT_MISMATCH'],
        used: [mode, inLimit, outLimit]
      })
    );
  } else {
    metrics.push(
      makeMetric(ctx, {
        id: 'ivrLines.mode',
        formulaId: 'F-IVR-04',
        unit: 'enum',
        status: 'COMPLETE',
        value: mode.value,
        used: [mode]
      })
    );
  }

  const aOperand = derivedOperand(metricById(metrics, 'ivrLines.a'));
  const bOperand = derivedOperand(metricById(metrics, 'ivrLines.b'));
  const lineDemands = [];
  if (mode.value === 'SEPARATE_INBOUND_OUTBOUND' && !mode.missing && !mode.invalid) {
    lineDemands.push({
      kind: 'inbound',
      limit: inLimit,
      peak: peaks.inbound,
      demand: { ...aOperand, id: 'ivrLines.lineDemand.inbound' }
    });
    lineDemands.push({
      kind: 'outbound',
      limit: outLimit,
      peak: peaks.outbound,
      demand: { ...bOperand, id: 'ivrLines.lineDemand.outbound' }
    });
  } else if (mode.value === 'SHARED' && !mode.missing && !mode.invalid) {
    const shared = sumOperands([aOperand, bOperand], ctx, 'ivrLines.lineDemand.shared');
    lineDemands.push({ kind: 'shared', limit: sharedLimit, peak: peaks.shared, demand: shared });
  }
  for (const item of lineDemands) {
    metrics.push(
      makeMetric(ctx, {
        id: `ivrLines.lineDemand.${item.kind}`,
        formulaId: 'F-IVR-04',
        unit: 'channels',
        status: item.demand.status,
        value: item.demand.status === 'COMPLETE' ? item.demand.value : null,
        missing: item.demand.missing ?? [],
        invalid: item.demand.invalid ?? [],
        reasons: item.demand.reasons ?? [],
        used: item.demand.used ?? [],
        limitations: ['AVERAGE_NOT_PEAK']
      })
    );
  }

  for (const item of lineDemands) {
    const demand = item.demand;
    const demandValue = demand.status === 'COMPLETE' ? demand.value : null;
    const usableOperands = [
      { status: item.limit.missing ? 'INCOMPLETE' : item.limit.invalid ? 'INVALID' : 'COMPLETE', value: item.limit.value, missing: item.limit.missing ? [item.limit.path] : [], invalid: item.limit.invalid ? [item.limit.path] : [], used: [item.limit] },
      { status: headroom.missing ? 'INCOMPLETE' : headroom.invalid ? 'INVALID' : 'COMPLETE', value: headroom.missing || headroom.invalid ? null : 1 - headroom.value, missing: headroom.missing ? [headroom.path] : [], invalid: headroom.invalid ? [headroom.path] : [], used: [headroom] }
    ];
    const usable = multiplyOperands(usableOperands, ctx, `ivrLines.usable.${item.kind}`);
    metrics.push(
      makeMetric(ctx, {
        id: `ivrLines.usable.${item.kind}`,
        formulaId: 'F-IVR-05',
        unit: 'channels',
        status: usable.status,
        value: usable.status === 'COMPLETE' ? usable.value : null,
        missing: usable.missing,
        invalid: usable.invalid,
        reasons: usable.reasons,
        used: usable.used,
        limitations: usable.limitations ?? []
      })
    );
    const usableValue = usable.status === 'COMPLETE' ? usable.value : null;
    let utilization;
    if (demand.status === 'INVALID' || usable.status === 'INVALID') {
      utilization = {
        status: 'INVALID',
        value: null,
        missing: [],
        invalid: unique([...(demand.invalid ?? []), ...(usable.invalid ?? [])]),
        reasons: unique([...(demand.reasons ?? []), ...(usable.reasons ?? [])])
      };
    } else if (demand.status !== 'COMPLETE' || usable.status !== 'COMPLETE') {
      utilization = {
        status: 'INCOMPLETE',
        value: null,
        missing: unique([...(demand.missing ?? []), ...(usable.missing ?? [])]),
        invalid: [],
        reasons: []
      };
    } else if (usableValue === 0 && demandValue === 0) {
      utilization = {
        status: 'COMPLETE',
        value: 0,
        missing: [],
        invalid: [],
        reasons: ['IDLE_ZERO_DENOMINATOR']
      };
    } else if (usableValue === 0) {
      utilization = {
        status: 'NOT_MET',
        value: null,
        missing: [],
        invalid: [],
        reasons: ['DIVISION_BY_ZERO', 'TARGET_NOT_MET']
      };
    } else {
      const ratio = demandValue / usableValue;
      if (!Number.isFinite(ratio)) {
        utilization = { status: 'INVALID', value: null, missing: [], invalid: [`ivrLines.utilization.${item.kind}`], reasons: ['NUMBER_NONFINITE'] };
        addError(ctx, 'NUMBER_NONFINITE', `ivrLines.utilization.${item.kind}`);
      } else {
        utilization = {
          status: ratio > 1 ? 'NOT_MET' : 'COMPLETE',
          value: ratio,
          missing: [],
          invalid: [],
          reasons: ratio > 1 ? ['TARGET_NOT_MET'] : []
        };
      }
    }
    metrics.push(
      makeMetric(ctx, {
        id: `ivrLines.utilization.${item.kind}`,
        formulaId: 'F-IVR-05',
        unit: 'ratio',
        ...utilization,
        used: [...(demand.used ?? []), ...(usable.used ?? [])]
      })
    );
    let shortfall;
    if (demand.status === 'INVALID' || usable.status === 'INVALID') {
      shortfall = { status: 'INVALID', value: null, missing: [], invalid: unique([...(demand.invalid ?? []), ...(usable.invalid ?? [])]), reasons: unique([...(demand.reasons ?? []), ...(usable.reasons ?? [])]) };
    } else if (demand.status !== 'COMPLETE' || usable.status !== 'COMPLETE') {
      shortfall = { status: 'INCOMPLETE', value: null, missing: unique([...(demand.missing ?? []), ...(usable.missing ?? [])]), invalid: [], reasons: [] };
    } else {
      const value = Math.max(0, demandValue - usableValue);
      shortfall = { status: value > 0 ? 'NOT_MET' : 'COMPLETE', value, missing: [], invalid: [], reasons: value > 0 ? ['TARGET_NOT_MET'] : [] };
    }
    metrics.push(
      makeMetric(ctx, {
        id: `ivrLines.shortfall.${item.kind}`,
        formulaId: 'F-IVR-05',
        unit: 'channels',
        ...shortfall,
        used: [...(demand.used ?? []), ...(usable.used ?? [])]
      })
    );

    const peak = item.peak;
    let verdict;
    if (
      mode.missing ||
      demand.status === 'INCOMPLETE' ||
      usable.status === 'INCOMPLETE'
    ) {
      verdict = {
        status: 'INCOMPLETE',
        value: null,
        missing: unique([...(demand.missing ?? []), ...(usable.missing ?? [])]),
        invalid: [],
        reasons: []
      };
    } else if (
      mode.invalid ||
      demand.status === 'INVALID' ||
      usable.status === 'INVALID' ||
      (peak && peak.metric && peak.metric.status === 'INVALID')
    ) {
      verdict = {
        status: 'INVALID',
        value: null,
        missing: [],
        invalid: unique([
          ...(demand.invalid ?? []),
          ...(usable.invalid ?? []),
          ...(peak && peak.metric ? peak.metric.invalid : [])
        ]),
        reasons: unique([
          ...(demand.reasons ?? []),
          ...(usable.reasons ?? []),
          ...(peak && peak.metric ? peak.metric.reasons : [])
        ])
      };
    } else if (shortfall.value > 0) {
      verdict = {
        status: 'NOT_MET',
        value: 'NOT_MET',
        missing: [],
        invalid: [],
        reasons: ['TARGET_NOT_MET'],
        limitations: ['LINE_RECOMMENDATION_UNVERIFIED']
      };
    } else if (!peak || !peak.valid) {
      verdict = {
        status: 'NOT_VERIFIED',
        value: 'ARITHMETIC_ONLY',
        missing: peak && peak.metric ? peak.metric.missing : [`ivrLines.IVR_OBSERVED_PEAK.${item.kind}`],
        invalid: [],
        reasons: ['PEAK_EVIDENCE_MISSING'],
        limitations: ['PEAK_EVIDENCE_MISSING', 'LINE_RECOMMENDATION_UNVERIFIED']
      };
    } else if (peak.count.value > usableValue) {
      verdict = {
        status: 'NOT_MET',
        value: 'NOT_MET',
        missing: [],
        invalid: [],
        reasons: ['TARGET_NOT_MET'],
        limitations: ['LINE_RECOMMENDATION_UNVERIFIED']
      };
    } else {
      const zeroDemand = demandValue === 0 || usableValue === 0;
      const estimateLike =
        !ivrProvenanceQualified(demand.used?.[0]) ||
        !policyOrMeasured(item.limit) ||
        !policyOrMeasured(headroom) ||
        !ivrProvenanceQualified(peak.count) ||
        !ivrProvenanceQualified(peak.window);
      // demand provenance is checked from the leaves that produced it.
      const demandLeaves = demand.used ?? [];
      const demandMeasured = demandLeaves.length > 0 && demandLeaves.every(ivrProvenanceQualified);
      const estimateInputs = !demandMeasured || estimateLike;
      if (zeroDemand) {
        verdict = {
          status: 'NOT_VERIFIED',
          value: 'ARITHMETIC_ONLY',
          missing: [],
          invalid: [],
          reasons: ['ZERO_DEMAND_NOT_A_FIT'],
          limitations: ['ZERO_DEMAND_NOT_A_FIT', 'LINE_RECOMMENDATION_UNVERIFIED']
        };
      } else if (estimateInputs) {
        verdict = {
          status: 'NOT_VERIFIED',
          value: 'ARITHMETIC_ONLY',
          missing: [],
          invalid: [],
          reasons: [],
          limitations: ['LINE_RECOMMENDATION_UNVERIFIED']
        };
      } else {
        verdict = {
          status: 'COMPLETE',
          value: 'LINE_FIT_EVIDENCE_COMPLETE',
          missing: [],
          invalid: [],
          reasons: []
        };
      }
    }
    metrics.push(
      makeMetric(ctx, {
        id: `ivrLines.verdict.${item.kind}`,
        formulaId: 'F-IVR-05a',
        unit: 'enum',
        ...verdict,
        used: [...(demand.used ?? []), ...(usable.used ?? []), ...(peak && peak.count ? [peak.count] : [])]
      })
    );
  }

  let ringMetric;
  if (outRing.invalid || outOcc.invalid) {
    ringMetric = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(outRing.invalid ? [outRing.path] : []), ...(outOcc.invalid ? [outOcc.path] : [])]),
      reasons: unique([...outRing.reasons, ...outOcc.reasons])
    };
  } else if (outRing.missing || outOcc.missing) {
    ringMetric = { status: 'INCOMPLETE', value: null, missing: collectMissing([outRing, outOcc]), invalid: [], reasons: [] };
  } else if (outOcc.value === 0 && outRing.value === 0) {
    ringMetric = { status: 'COMPLETE', value: 0, missing: [], invalid: [], reasons: ['IDLE_ZERO_DENOMINATOR'] };
  } else if (outOcc.value === 0 && outRing.value > 0) {
    addError(ctx, 'RING_EXCEEDS_OCCUPANCY', 'ivrLines.IVR_OUT_RING_S');
    ringMetric = { status: 'INVALID', value: null, missing: [], invalid: [outRing.path], reasons: ['RING_EXCEEDS_OCCUPANCY'] };
  } else if (outRing.value > outOcc.value) {
    addError(ctx, 'RING_EXCEEDS_OCCUPANCY', 'ivrLines.IVR_OUT_RING_S');
    ringMetric = { status: 'INVALID', value: null, missing: [], invalid: [outRing.path], reasons: ['RING_EXCEEDS_OCCUPANCY'] };
  } else {
    ringMetric = { status: 'COMPLETE', value: outRing.value / outOcc.value, missing: [], invalid: [], reasons: [] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'ivrLines.ringRatio',
      formulaId: 'F-IVR-06',
      unit: 'ratio',
      ...ringMetric,
      used: [outRing, outOcc]
    })
  );

  let mixMetric;
  const mixRecords = IVR_MIX_KEYS.map((key) => mix[key]);
  const mixInvalid = collectInvalid(mixRecords);
  const mixMissing = collectMissing(mixRecords);
  if (mixInvalid.length > 0) {
    mixMetric = { status: 'INVALID', value: null, missing: mixMissing, invalid: mixInvalid, reasons: invalidReasons(mixRecords) };
  } else if (mixMissing.length > 0 || inRate.missing || inRate.invalid) {
    mixMetric = {
      status: inRate.invalid ? 'INVALID' : 'INCOMPLETE',
      value: null,
      missing: unique([...mixMissing, ...(inRate.missing ? [inRate.path] : [])]),
      invalid: inRate.invalid ? [inRate.path] : [],
      reasons: inRate.invalid ? inRate.reasons : []
    };
  } else {
    const sum = mixRecords.reduce((acc, rec) => acc + rec.value, 0);
    if (!Number.isFinite(sum) || Math.abs(sum) > Number.MAX_SAFE_INTEGER) {
      addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', 'ivrLines.mixSum');
      mixMetric = { status: 'INVALID', value: null, missing: [], invalid: ['ivrLines.mixSum'], reasons: ['OVERFLOW_UNSAFE_INTEGER'] };
    } else if (Math.abs(sum - inRate.value) > 1e-9 * Math.max(1, Math.abs(inRate.value))) {
      addError(ctx, 'MIX_SUM_MISMATCH', 'ivrLines.IVR_MIX_RATE_PS');
      mixMetric = { status: 'INVALID', value: sum, missing: [], invalid: ['ivrLines.IVR_MIX_RATE_PS'], reasons: ['MIX_SUM_MISMATCH'] };
    } else {
      mixMetric = { status: 'COMPLETE', value: sum, missing: [], invalid: [], reasons: [] };
    }
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'ivrLines.mixSum',
      formulaId: 'F-IVR-07',
      unit: 'callsPerSecond',
      ...mixMetric,
      used: [...mixRecords, inRate]
    })
  );

  for (const kind of PEAK_KINDS) {
    if (peaks[kind].metric) metrics.push(peaks[kind].metric);
  }

  return groupResult(
    'ivrLines',
    ['F-IVR-01', 'F-IVR-02', 'F-IVR-03', 'F-IVR-04', 'F-IVR-05', 'F-IVR-05a', 'F-IVR-06', 'F-IVR-07'],
    null,
    metrics,
    [],
    extraInvalid
  );
}

function computeApiControlPlane(ctx, input) {
  const read = readGroup(ctx, input, 'apiControlPlane');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  checkUnknownKeys(
    ctx,
    group,
    new Set(['API_RATE_PS', 'API_MEAN_SERVICE_S', 'API_DB_TXN_MEAN_S']),
    'apiControlPlane',
    extraInvalid
  );
  const rates = {};
  if (group.API_RATE_PS !== undefined && !isPlainObject(group.API_RATE_PS)) {
    addError(ctx, 'TYPE_INVALID', 'apiControlPlane.API_RATE_PS');
    extraInvalid.push('apiControlPlane.API_RATE_PS');
  } else if (isPlainObject(group.API_RATE_PS)) {
    checkUnknownKeys(ctx, group.API_RATE_PS, new Set(API_RATE_KEYS), 'apiControlPlane.API_RATE_PS', extraInvalid);
  }
  const rateObject = isPlainObject(group.API_RATE_PS) ? group.API_RATE_PS : null;
  for (const key of API_RATE_KEYS) {
    rates[key] = readLeaf(ctx, rateObject, key, `apiControlPlane.API_RATE_PS.${key}`, {
      type: 'number',
      unit: 'requestsPerSecond',
      kind: 'NonNegativeRate'
    });
  }
  const meanService = readLeaf(ctx, group, 'API_MEAN_SERVICE_S', 'apiControlPlane.API_MEAN_SERVICE_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const dbTxnMean = readLeaf(ctx, group, 'API_DB_TXN_MEAN_S', 'apiControlPlane.API_DB_TXN_MEAN_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const metrics = [];
  const rateRecords = API_RATE_KEYS.map((key) => rates[key]);
  const invalid = collectInvalid(rateRecords);
  const missing = collectMissing(rateRecords);
  let total;
  if (invalid.length > 0) {
    total = { status: 'INVALID', value: null, missing, invalid, reasons: invalidReasons(rateRecords) };
  } else if (missing.length > 0) {
    total = { status: 'INCOMPLETE', value: null, missing, invalid: [], reasons: [] };
  } else {
    const value = rateRecords.reduce((acc, rec) => acc + rec.value, 0);
    if (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER) {
      addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', 'apiControlPlane.totalRate');
      total = { status: 'INVALID', value: null, missing: [], invalid: ['apiControlPlane.totalRate'], reasons: ['OVERFLOW_UNSAFE_INTEGER'] };
    } else {
      total = { status: 'COMPLETE', value, missing: [], invalid: [], reasons: [] };
    }
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'apiControlPlane.totalRate',
      formulaId: 'F-API-01',
      unit: 'requestsPerSecond',
      ...total,
      used: rateRecords
    })
  );
  const totalOperand = derivedOperand(metricById(metrics, 'apiControlPlane.totalRate'));
  const inFlight = multiplyOperands([totalOperand, { status: meanService.missing ? 'INCOMPLETE' : meanService.invalid ? 'INVALID' : 'COMPLETE', value: meanService.value, missing: meanService.missing ? [meanService.path] : [], invalid: meanService.invalid ? [meanService.path] : [], used: [meanService] }], ctx, 'apiControlPlane.inFlight');
  metrics.push(
    makeMetric(ctx, {
      id: 'apiControlPlane.inFlight',
      formulaId: 'F-API-02',
      unit: 'requests',
      status: inFlight.status,
      value: inFlight.status === 'COMPLETE' ? inFlight.value : null,
      missing: inFlight.missing,
      invalid: inFlight.invalid,
      reasons: inFlight.reasons,
      used: inFlight.used,
      limitations: inFlight.limitations ?? []
    })
  );
  const dbTxn = multiplyOperands([totalOperand, { status: dbTxnMean.missing ? 'INCOMPLETE' : dbTxnMean.invalid ? 'INVALID' : 'COMPLETE', value: dbTxnMean.value, missing: dbTxnMean.missing ? [dbTxnMean.path] : [], invalid: dbTxnMean.invalid ? [dbTxnMean.path] : [], used: [dbTxnMean] }], ctx, 'apiControlPlane.dbTxnInFlightEstimate');
  metrics.push(
    makeMetric(ctx, {
      id: 'apiControlPlane.dbTxnInFlightEstimate',
      formulaId: 'F-API-02',
      unit: 'transactions',
      status: dbTxn.status,
      value: dbTxn.status === 'COMPLETE' ? dbTxn.value : null,
      missing: dbTxn.missing,
      invalid: dbTxn.invalid,
      reasons: dbTxn.reasons,
      used: dbTxn.used,
      limitations: [...(dbTxn.limitations ?? []), 'TXN_PER_REQUEST_NOT_MEASURED']
    })
  );
  return groupResult('apiControlPlane', ['F-API-01', 'F-API-02'], null, metrics, [], extraInvalid);
}

function computePlayback(ctx, input) {
  const read = readGroup(ctx, input, 'playback');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  checkUnknownKeys(
    ctx,
    group,
    new Set([
      'PB_CONCURRENT_SESSIONS',
      'PB_BITRATE_BPS',
      'PB_SESSIONS_PER_DAY',
      'PB_MEAN_SECONDS',
      'PB_CDN_CACHE_HIT_RATIO'
    ]),
    'playback',
    extraInvalid
  );
  const concurrent = readLeaf(ctx, group, 'PB_CONCURRENT_SESSIONS', 'playback.PB_CONCURRENT_SESSIONS', {
    type: 'number',
    unit: 'count',
    kind: 'DiscreteCount'
  });
  const bitrate = readLeaf(ctx, group, 'PB_BITRATE_BPS', 'playback.PB_BITRATE_BPS', {
    type: 'number',
    unit: 'bitsPerSecond',
    kind: 'NonNegativeRate'
  });
  const sessionsPerDay = readLeaf(ctx, group, 'PB_SESSIONS_PER_DAY', 'playback.PB_SESSIONS_PER_DAY', {
    type: 'number',
    unit: 'countPerDay',
    kind: 'DiscreteCount'
  });
  const meanSeconds = readLeaf(ctx, group, 'PB_MEAN_SECONDS', 'playback.PB_MEAN_SECONDS', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const cacheRatio = readLeaf(ctx, group, 'PB_CDN_CACHE_HIT_RATIO', 'playback.PB_CDN_CACHE_HIT_RATIO', {
    type: 'number',
    unit: 'ratio',
    kind: 'Ratio01'
  });
  const metrics = [];
  const peak = multiply([concurrent, bitrate], ctx, 'playback.peakBitsPerSecond');
  metrics.push(
    makeMetric(ctx, {
      id: 'playback.peakBitsPerSecond',
      formulaId: 'F-PB-01',
      unit: 'bitsPerSecond',
      ...peak,
      limitations: [...(peak.limitations ?? []), 'SHARED_MEDIA_GATEWAY', 'BEARER_CHECKS_UNCHANGED']
    })
  );
  const daily = multiply([sessionsPerDay, meanSeconds, bitrate], ctx, 'playback.dailyPayloadBytes');
  let dailyMetric;
  if (daily.status === 'COMPLETE') {
    dailyMetric = { ...daily, value: daily.value / 8 };
  } else {
    dailyMetric = daily;
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'playback.dailyPayloadBytes',
      formulaId: 'F-PB-02',
      unit: 'bytesPerDay',
      ...dailyMetric,
      limitations: [...(dailyMetric.limitations ?? []), 'AVERAGE_NOT_PEAK', 'CONDITIONAL_CACHE_SCENARIO']
    })
  );
  let origin;
  if (dailyMetric.status === 'INVALID' || cacheRatio.invalid) {
    origin = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(dailyMetric.invalid ?? []), ...(cacheRatio.invalid ? [cacheRatio.path] : [])]),
      reasons: unique([...(dailyMetric.reasons ?? []), ...cacheRatio.reasons])
    };
  } else if (dailyMetric.status === 'COMPLETE' && dailyMetric.value === 0) {
    origin = { status: 'COMPLETE', value: 0, missing: cacheRatio.missing ? [cacheRatio.path] : [], invalid: [], reasons: [], limitations: ['ZERO_FACTOR_SHORT_CIRCUIT'] };
  } else if (dailyMetric.status !== 'COMPLETE' || cacheRatio.missing) {
    origin = {
      status: 'INCOMPLETE',
      value: null,
      missing: unique([...(dailyMetric.missing ?? []), ...(cacheRatio.missing ? [cacheRatio.path] : [])]),
      invalid: [],
      reasons: []
    };
  } else {
    origin = { status: 'COMPLETE', value: dailyMetric.value * (1 - cacheRatio.value), missing: [], invalid: [], reasons: [] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'playback.originBytesPerDay',
      formulaId: 'F-PB-03',
      unit: 'bytesPerDay',
      ...origin,
      used: [...(dailyMetric.used ?? []), cacheRatio],
      limitations: [...(origin.limitations ?? []), 'CONDITIONAL_CACHE_SCENARIO', 'NO_CDN_TOPOLOGY_APPROVAL', 'BEARER_CHECKS_UNCHANGED']
    })
  );
  return groupResult('playback', ['F-PB-01', 'F-PB-02', 'F-PB-03'], null, metrics, [], extraInvalid);
}

function readDerivedType(ctx, container, type, basePath, extraInvalid) {
  const typePath = `${basePath}.${type}`;
  const entry = isPlainObject(container) && hasOwn(container, type) ? container[type] : undefined;
  const record = {
    type,
    present: entry !== undefined,
    state: null,
    stateInvalid: false,
    stateMissing: entry === undefined,
    bytes: null,
    retention: null,
    version: null,
    entry: isPlainObject(entry) ? entry : null,
    path: typePath
  };
  if (entry === undefined) return record;
  if (!isPlainObject(entry)) {
    addError(ctx, 'TYPE_INVALID', typePath);
    extraInvalid.push(typePath);
    record.stateInvalid = true;
    return record;
  }
  checkUnknownKeys(ctx, entry, new Set(['state', 'bytesPerOriginalByte', 'retentionDays', 'versionFactor']), typePath, extraInvalid);
  if (!hasOwn(entry, 'state') || entry.state === null) {
    record.stateMissing = true;
  } else if (typeof entry.state !== 'string' || !['PRODUCED', 'DISABLED'].includes(entry.state)) {
    addError(ctx, 'TYPE_INVALID', `${typePath}.state`);
    extraInvalid.push(`${typePath}.state`);
    record.stateInvalid = true;
  } else {
    record.state = entry.state;
    record.stateMissing = false;
  }
  record.bytes = readLeaf(ctx, entry, 'bytesPerOriginalByte', `${typePath}.bytesPerOriginalByte`, {
    type: 'number',
    unit: 'ratio',
    kind: 'DerivedByteRatio'
  });
  record.retention = readLeaf(ctx, entry, 'retentionDays', `${typePath}.retentionDays`, {
    type: 'number',
    unit: 'days',
    kind: 'NonNegativeMeasure'
  });
  record.version = readLeaf(ctx, entry, 'versionFactor', `${typePath}.versionFactor`, {
    type: 'number',
    unit: 'ratio',
    kind: 'VersionFactor'
  });
  return record;
}

function computeMediaIngestStorage(ctx, input, spoolMetrics) {
  const read = readGroup(ctx, input, 'mediaIngestStorage');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  const allowed = new Set([
    'REC_BITRATE_BPS',
    'REC_CODEC',
    'REC_SAMPLE_RATE_HZ',
    'REC_BIT_DEPTH',
    'REC_CHANNELS',
    'REC_MEAN_VALIDATED_S',
    'REC_PER_DAY',
    'REC_ORIGINAL_VERSION_FACTOR',
    'REC_RETENTION_DAYS',
    'REC_DERIVED_BY_TYPE',
    'REC_LEGAL_HOLD_EXTRA_BYTES',
    'REC_BACKUP_COPY_FACTOR',
    'UPLOAD_CONCURRENT_PEAK',
    'UPLOAD_PER_STREAM_WIRE_BPS',
    'UPLOAD_MEASURED_PEAK_WIRE_BPS',
    'UPLOAD_WIRE_OVERHEAD_RATIO',
    'UPLOAD_COMBINED_MEASURED_WIRE_BPS'
  ]);
  checkUnknownKeys(ctx, group, allowed, 'mediaIngestStorage', extraInvalid);
  const bitrate = readLeaf(ctx, group, 'REC_BITRATE_BPS', 'mediaIngestStorage.REC_BITRATE_BPS', {
    type: 'number',
    unit: 'bitsPerSecond',
    kind: 'NonNegativeRate'
  });
  const codec = readLeaf(ctx, group, 'REC_CODEC', 'mediaIngestStorage.REC_CODEC', {
    type: 'string',
    unit: 'enum'
  });
  const sampleRate = readLeaf(ctx, group, 'REC_SAMPLE_RATE_HZ', 'mediaIngestStorage.REC_SAMPLE_RATE_HZ', {
    type: 'number',
    unit: 'hertz',
    kind: 'NonNegativeRate'
  });
  const bitDepth = readLeaf(ctx, group, 'REC_BIT_DEPTH', 'mediaIngestStorage.REC_BIT_DEPTH', {
    type: 'number',
    unit: 'bit',
    kind: 'DiscreteCount'
  });
  const channels = readLeaf(ctx, group, 'REC_CHANNELS', 'mediaIngestStorage.REC_CHANNELS', {
    type: 'number',
    unit: 'channels',
    kind: 'DiscreteCount'
  });
  const validatedSeconds = readLeaf(ctx, group, 'REC_MEAN_VALIDATED_S', 'mediaIngestStorage.REC_MEAN_VALIDATED_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const recPerDay = readLeaf(ctx, group, 'REC_PER_DAY', 'mediaIngestStorage.REC_PER_DAY', {
    type: 'number',
    unit: 'countPerDay',
    kind: 'DiscreteCount'
  });
  const originalVersion = readLeaf(ctx, group, 'REC_ORIGINAL_VERSION_FACTOR', 'mediaIngestStorage.REC_ORIGINAL_VERSION_FACTOR', {
    type: 'number',
    unit: 'ratio',
    kind: 'VersionFactor'
  });
  const retention = readLeaf(ctx, group, 'REC_RETENTION_DAYS', 'mediaIngestStorage.REC_RETENTION_DAYS', {
    type: 'number',
    unit: 'days',
    kind: 'NonNegativeMeasure'
  });
  const legalHold = readLeaf(ctx, group, 'REC_LEGAL_HOLD_EXTRA_BYTES', 'mediaIngestStorage.REC_LEGAL_HOLD_EXTRA_BYTES', {
    type: 'number',
    unit: 'bytes',
    kind: 'NonNegativeMeasure'
  });
  const backupFactor = readLeaf(ctx, group, 'REC_BACKUP_COPY_FACTOR', 'mediaIngestStorage.REC_BACKUP_COPY_FACTOR', {
    type: 'number',
    unit: 'ratio',
    kind: 'CopyFactor'
  });
  const uploadConcurrent = readLeaf(ctx, group, 'UPLOAD_CONCURRENT_PEAK', 'mediaIngestStorage.UPLOAD_CONCURRENT_PEAK', {
    type: 'number',
    unit: 'count',
    kind: 'DiscreteCount'
  });
  const uploadPerStream = readLeaf(ctx, group, 'UPLOAD_PER_STREAM_WIRE_BPS', 'mediaIngestStorage.UPLOAD_PER_STREAM_WIRE_BPS', {
    type: 'number',
    unit: 'bitsPerSecond',
    kind: 'NonNegativeRate'
  });
  const measuredPeakWire = readLeaf(ctx, group, 'UPLOAD_MEASURED_PEAK_WIRE_BPS', 'mediaIngestStorage.UPLOAD_MEASURED_PEAK_WIRE_BPS', {
    type: 'number',
    unit: 'bitsPerSecond',
    kind: 'NonNegativeRate'
  });
  const overhead = readLeaf(ctx, group, 'UPLOAD_WIRE_OVERHEAD_RATIO', 'mediaIngestStorage.UPLOAD_WIRE_OVERHEAD_RATIO', {
    type: 'number',
    unit: 'ratio',
    kind: 'WireOverheadRatio'
  });
  const combined = readLeaf(ctx, group, 'UPLOAD_COMBINED_MEASURED_WIRE_BPS', 'mediaIngestStorage.UPLOAD_COMBINED_MEASURED_WIRE_BPS', {
    type: 'number',
    unit: 'bitsPerSecond',
    kind: 'NonNegativeRate'
  });
  const derivedContainer = group.REC_DERIVED_BY_TYPE;
  if (derivedContainer !== undefined && !isPlainObject(derivedContainer)) {
    addError(ctx, 'TYPE_INVALID', 'mediaIngestStorage.REC_DERIVED_BY_TYPE');
    extraInvalid.push('mediaIngestStorage.REC_DERIVED_BY_TYPE');
  } else if (isPlainObject(derivedContainer)) {
    checkUnknownKeys(ctx, derivedContainer, new Set(DERIVED_TYPES), 'mediaIngestStorage.REC_DERIVED_BY_TYPE', extraInvalid);
  }
  const derived = {};
  for (const type of DERIVED_TYPES) {
    derived[type] = readDerivedType(
      ctx,
      isPlainObject(derivedContainer) ? derivedContainer : null,
      type,
      'mediaIngestStorage.REC_DERIVED_BY_TYPE',
      extraInvalid
    );
  }

  const metrics = [];
  const shadow = [];
  let rec01;
  if (bitrate.invalid) {
    rec01 = { status: 'INVALID', value: null, missing: [], invalid: [bitrate.path], reasons: bitrate.reasons, used: [bitrate] };
    for (const rec of [sampleRate, bitDepth, channels]) {
      if (rec.invalid) shadow.push(rec.path);
    }
  } else if (!bitrate.missing) {
    rec01 = { status: 'COMPLETE', value: bitrate.value, missing: [], invalid: [], reasons: [], used: [bitrate] };
    for (const rec of [sampleRate, bitDepth, channels]) {
      if (rec.invalid) {
        rec01.invalid = [...(rec01.invalid ?? []), rec.path];
        rec01.reasons = [...(rec01.reasons ?? []), ...rec.reasons];
      }
    }
    if (rec01.invalid.length > 0) rec01.status = 'INVALID';
  } else if (codec.invalid) {
    rec01 = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: [codec.path, ...collectInvalid([sampleRate, bitDepth, channels])],
      reasons: unique([...codec.reasons, ...invalidReasons([sampleRate, bitDepth, channels])]),
      used: [codec, sampleRate, bitDepth, channels]
    };
  } else if (codec.missing) {
    rec01 = {
      status: 'INCOMPLETE',
      value: null,
      missing: unique([bitrate.path, codec.path, ...collectMissing([sampleRate, bitDepth, channels])]),
      invalid: collectInvalid([sampleRate, bitDepth, channels]),
      reasons: invalidReasons([sampleRate, bitDepth, channels]),
      used: [bitrate, codec, sampleRate, bitDepth, channels]
    };
  } else if (codec.value === 'PCM_LINEAR') {
    const pcm = multiply([sampleRate, bitDepth, channels], ctx, 'mediaIngestStorage.recordingBitsPerSecond');
    rec01 = { ...pcm, used: [bitrate, codec, sampleRate, bitDepth, channels] };
  } else {
    rec01 = {
      status: 'INCOMPLETE',
      value: null,
      missing: [bitrate.path],
      invalid: collectInvalid([sampleRate, bitDepth, channels]),
      reasons: invalidReasons([sampleRate, bitDepth, channels]),
      used: [bitrate, codec, sampleRate, bitDepth, channels]
    };
  }
  if (shadow.length > 0) {
    rec01 = {
      ...rec01,
      status: 'INVALID',
      invalid: unique([...(rec01.invalid ?? []), ...shadow]),
      reasons: unique([...(rec01.reasons ?? []), ...invalidReasons([sampleRate, bitDepth, channels])])
    };
  }
  if ((rec01.invalid ?? []).length > 0) rec01.status = 'INVALID';
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.recordingBitsPerSecond',
      formulaId: 'F-REC-01',
      unit: 'bitsPerSecond',
      status: rec01.status,
      value: rec01.status === 'COMPLETE' ? rec01.value : null,
      missing: rec01.missing,
      invalid: rec01.invalid ?? [],
      reasons: rec01.reasons ?? [],
      used: rec01.used,
      limitations: [...(rec01.limitations ?? []), 'NOT_APPROVED_PRODUCTION_VALUE']
    })
  );

  const rec01Operand = derivedOperand(metricById(metrics, 'mediaIngestStorage.recordingBitsPerSecond'));
  const rec02 = multiplyOperands(
    [
      rec01Operand,
      {
        status: validatedSeconds.missing ? 'INCOMPLETE' : validatedSeconds.invalid ? 'INVALID' : 'COMPLETE',
        value: validatedSeconds.value,
        missing: validatedSeconds.missing ? [validatedSeconds.path] : [],
        invalid: validatedSeconds.invalid ? [validatedSeconds.path] : [],
        used: [validatedSeconds]
      }
    ],
    ctx,
    'mediaIngestStorage.bytesPerRecording'
  );
  let rec02Metric;
  if (rec02.status === 'COMPLETE') {
    rec02Metric = { ...rec02, value: rec02.value / 8 };
  } else {
    rec02Metric = rec02;
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.bytesPerRecording',
      formulaId: 'F-REC-02',
      unit: 'bytes',
      status: rec02Metric.status,
      value: rec02Metric.status === 'COMPLETE' ? rec02Metric.value : null,
      missing: rec02Metric.missing,
      invalid: rec02Metric.invalid,
      reasons: rec02Metric.reasons,
      used: rec02Metric.used,
      limitations: rec02Metric.limitations ?? []
    })
  );
  const rec02Operand = derivedOperand(metricById(metrics, 'mediaIngestStorage.bytesPerRecording'));
  const rec03 = multiplyOperands(
    [
      rec02Operand,
      {
        status: recPerDay.missing ? 'INCOMPLETE' : recPerDay.invalid ? 'INVALID' : 'COMPLETE',
        value: recPerDay.value,
        missing: recPerDay.missing ? [recPerDay.path] : [],
        invalid: recPerDay.invalid ? [recPerDay.path] : [],
        used: [recPerDay]
      }
    ],
    ctx,
    'mediaIngestStorage.dailyOriginalBytes'
  );
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.dailyOriginalBytes',
      formulaId: 'F-REC-03',
      unit: 'bytesPerDay',
      status: rec03.status,
      value: rec03.status === 'COMPLETE' ? rec03.value : null,
      missing: rec03.missing,
      invalid: rec03.invalid,
      reasons: rec03.reasons,
      used: rec03.used,
      limitations: rec03.limitations ?? []
    })
  );
  const rec03Operand = derivedOperand(metricById(metrics, 'mediaIngestStorage.dailyOriginalBytes'));

  const rec04 = multiplyOperands(
    [
      rec03Operand,
      {
        status: retention.missing ? 'INCOMPLETE' : retention.invalid ? 'INVALID' : 'COMPLETE',
        value: retention.value,
        missing: retention.missing ? [retention.path] : [],
        invalid: retention.invalid ? [retention.path] : [],
        used: [retention]
      },
      {
        status: originalVersion.missing ? 'INCOMPLETE' : originalVersion.invalid ? 'INVALID' : 'COMPLETE',
        value: originalVersion.value,
        missing: originalVersion.missing ? [originalVersion.path] : [],
        invalid: originalVersion.invalid ? [originalVersion.path] : [],
        used: [originalVersion]
      }
    ],
    ctx,
    'mediaIngestStorage.retainedOriginalBytes'
  );
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.retainedOriginalBytes',
      formulaId: 'F-REC-04',
      unit: 'bytes',
      status: rec04.status,
      value: rec04.status === 'COMPLETE' ? rec04.value : null,
      missing: rec04.missing,
      invalid: rec04.invalid,
      reasons: rec04.reasons,
      used: rec04.used,
      limitations: rec04.limitations ?? []
    })
  );

  const derivedRetained = {};
  for (const type of DERIVED_TYPES) {
    const info = derived[type];
    const typeId = `mediaIngestStorage.derived.${type}`;
    if (!info.present) {
      derivedRetained[type] = { status: 'INCOMPLETE', value: 0 };
      metrics.push(
        makeMetric(ctx, {
          id: typeId,
          formulaId: 'F-REC-05',
          unit: 'bytes',
          status: 'INCOMPLETE',
          missing: [`${info.path}.state`, `${info.path}.bytesPerOriginalByte`, `${info.path}.retentionDays`, `${info.path}.versionFactor`],
          used: []
        })
      );
      metrics.push(
        makeMetric(ctx, {
          id: `mediaIngestStorage.derivedDaily.${type}`,
          formulaId: 'F-REC-05',
          unit: 'bytesPerDay',
          status: 'INCOMPLETE',
          missing: [`${info.path}.state`, `${info.path}.bytesPerOriginalByte`],
          used: []
        })
      );
      continue;
    }
    if (info.stateInvalid || info.stateMissing) {
      const leafInvalid = collectInvalid([info.bytes, info.retention, info.version]);
      const invalidLeaves = unique([
        ...(info.stateInvalid ? [`${info.path}.state`] : []),
        ...leafInvalid
      ]);
      const status = invalidLeaves.length > 0 ? 'INVALID' : 'INCOMPLETE';
      const reasons = unique([
        ...(info.stateInvalid ? ['TYPE_INVALID'] : []),
        ...invalidReasons([info.bytes, info.retention, info.version])
      ]);
      metrics.push(
        makeMetric(ctx, {
          id: typeId,
          formulaId: 'F-REC-05',
          unit: 'bytes',
          status,
          missing: info.stateMissing ? [`${info.path}.state`] : [],
          invalid: invalidLeaves,
          reasons,
          used: [info.bytes, info.retention, info.version]
        })
      );
      metrics.push(
        makeMetric(ctx, {
          id: `mediaIngestStorage.derivedDaily.${type}`,
          formulaId: 'F-REC-05',
          unit: 'bytesPerDay',
          status,
          missing: info.stateMissing ? [`${info.path}.state`] : [],
          invalid: invalidLeaves,
          reasons,
          used: [info.bytes, info.retention, info.version]
        })
      );
      derivedRetained[type] = { status, value: 0, used: [info.bytes, info.retention, info.version], missing: info.stateMissing ? [`${info.path}.state`] : [], invalid: invalidLeaves };
      continue;
    }
    if (info.state === 'DISABLED') {
      const explicitZero =
        info.bytes.present && !info.bytes.missing && !info.bytes.invalid && info.bytes.value === 0;
      const invalidLeaves = unique([
        ...(explicitZero ? [] : [`${info.path}.bytesPerOriginalByte`]),
        ...collectInvalid([info.retention, info.version])
      ]);
      const reasons = explicitZero
        ? invalidReasons([info.retention, info.version])
        : ['PROVENANCE_INVALID', ...invalidReasons([info.retention, info.version])];
      if (!explicitZero) {
        addError(ctx, 'PROVENANCE_INVALID', `${info.path}.bytesPerOriginalByte`);
      }
      const status = invalidLeaves.length > 0 || !explicitZero ? 'INVALID' : 'COMPLETE';
      const daily = makeMetric(ctx, {
        id: `mediaIngestStorage.derivedDaily.${type}`,
        formulaId: 'F-REC-05',
        unit: 'bytesPerDay',
        status,
        value: status === 'COMPLETE' ? 0 : null,
        invalid: invalidLeaves,
        reasons,
        used: [info.bytes, info.retention, info.version],
        limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
      });
      metrics.push(daily);
      metrics.push(
        makeMetric(ctx, {
          id: typeId,
          formulaId: 'F-REC-05',
          unit: 'bytes',
          status: daily.status,
          value: daily.status === 'COMPLETE' ? 0 : null,
          invalid: daily.invalid,
          reasons: daily.reasons,
          used: daily.used,
          limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
        })
      );
      derivedRetained[type] = {
        status: daily.status,
        value: 0,
        used: daily.used,
        missing: [],
        invalid: daily.invalid
      };
      continue;
    }
    // PRODUCED
    const dailyOperands = [
      rec03Operand,
      {
        status: info.bytes.missing ? 'INCOMPLETE' : info.bytes.invalid ? 'INVALID' : 'COMPLETE',
        value: info.bytes.value,
        missing: info.bytes.missing ? [info.bytes.path] : [],
        invalid: info.bytes.invalid ? [info.bytes.path] : [],
        used: [info.bytes]
      }
    ];
    const daily = multiplyOperands(dailyOperands, ctx, `mediaIngestStorage.derivedDaily.${type}`);
    metrics.push(
      makeMetric(ctx, {
        id: `mediaIngestStorage.derivedDaily.${type}`,
        formulaId: 'F-REC-05',
        unit: 'bytesPerDay',
        status: daily.status,
        value: daily.status === 'COMPLETE' ? daily.value : null,
        missing: daily.missing,
        invalid: daily.invalid,
        reasons: daily.reasons,
        used: daily.used,
        limitations: daily.limitations ?? []
      })
    );
    const dailyOperand = derivedOperand(metricById(metrics, `mediaIngestStorage.derivedDaily.${type}`));
    const retained = multiplyOperands(
      [
        dailyOperand,
        {
          status: info.retention.missing ? 'INCOMPLETE' : info.retention.invalid ? 'INVALID' : 'COMPLETE',
          value: info.retention.value,
          missing: info.retention.missing ? [info.retention.path] : [],
          invalid: info.retention.invalid ? [info.retention.path] : [],
          used: [info.retention]
        },
        {
          status: info.version.missing ? 'INCOMPLETE' : info.version.invalid ? 'INVALID' : 'COMPLETE',
          value: info.version.value,
          missing: info.version.missing ? [info.version.path] : [],
          invalid: info.version.invalid ? [info.version.path] : [],
          used: [info.version]
        }
      ],
      ctx,
      typeId
    );
    metrics.push(
      makeMetric(ctx, {
        id: typeId,
        formulaId: 'F-REC-05',
        unit: 'bytes',
        status: retained.status,
        value: retained.status === 'COMPLETE' ? retained.value : null,
        missing: retained.missing,
        invalid: retained.invalid,
        reasons: retained.reasons,
        used: retained.used,
        limitations: retained.limitations ?? []
      })
    );
    derivedRetained[type] = {
      status: retained.status,
      value: retained.status === 'COMPLETE' ? retained.value : null,
      used: retained.used,
      missing: retained.missing,
      invalid: retained.invalid
    };
  }

  const rec04Operand = derivedOperand(metricById(metrics, 'mediaIngestStorage.retainedOriginalBytes'));
  const logicalOperands = [
    rec04Operand,
    ...DERIVED_TYPES.map((type) => ({
      status: derivedRetained[type].status,
      value: derivedRetained[type].value,
      missing: derivedRetained[type].missing ?? [],
      invalid: derivedRetained[type].invalid ?? [],
      used: derivedRetained[type].used ?? []
    })),
    {
      status: legalHold.missing ? 'INCOMPLETE' : legalHold.invalid ? 'INVALID' : 'COMPLETE',
      value: legalHold.value,
      missing: legalHold.missing ? [legalHold.path] : [],
      invalid: legalHold.invalid ? [legalHold.path] : [],
      used: [legalHold]
    }
  ];
  const logical = sumOperands(logicalOperands, ctx, 'mediaIngestStorage.logicalMediaBytes');
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.logicalMediaBytes',
      formulaId: 'F-REC-06',
      unit: 'bytes',
      status: logical.status,
      value: logical.status === 'COMPLETE' ? logical.value : null,
      missing: logical.missing,
      invalid: logical.invalid,
      reasons: logical.reasons,
      used: logical.used,
      limitations: logical.limitations ?? []
    })
  );
  const logicalOperand = derivedOperand(metricById(metrics, 'mediaIngestStorage.logicalMediaBytes'));
  const backup = multiplyOperands(
    [
      logicalOperand,
      {
        status: backupFactor.missing ? 'INCOMPLETE' : backupFactor.invalid ? 'INVALID' : 'COMPLETE',
        value: backupFactor.value,
        missing: backupFactor.missing ? [backupFactor.path] : [],
        invalid: backupFactor.invalid ? [backupFactor.path] : [],
        used: [backupFactor]
      }
    ],
    ctx,
    'mediaIngestStorage.backupMediaBytes'
  );
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.backupMediaBytes',
      formulaId: 'F-REC-06',
      unit: 'bytes',
      status: backup.status,
      value: backup.status === 'COMPLETE' ? backup.value : null,
      missing: backup.missing,
      invalid: backup.invalid,
      reasons: backup.reasons,
      used: backup.used,
      limitations: backup.limitations ?? []
    })
  );

  const rec03DailyOperand = derivedOperand(metricById(metrics, 'mediaIngestStorage.dailyOriginalBytes'));
  const up01 = multiplyOperands(
    [rec03DailyOperand, { status: 'COMPLETE', value: 8, missing: [], invalid: [], used: [] }],
    ctx,
    'mediaIngestStorage.upload.dailyAveragePayload'
  );
  let up01Metric;
  if (up01.status === 'COMPLETE') {
    up01Metric = { ...up01, value: up01.value / 86400 };
  } else {
    up01Metric = up01;
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.upload.dailyAveragePayload',
      formulaId: 'F-UP-01',
      unit: 'bitsPerSecond',
      status: up01Metric.status,
      value: up01Metric.status === 'COMPLETE' ? up01Metric.value : null,
      missing: up01Metric.missing,
      invalid: up01Metric.invalid,
      reasons: up01Metric.reasons,
      used: up01Metric.used,
      limitations: [...(up01Metric.limitations ?? []), 'AVERAGE_NOT_PEAK']
    })
  );

  let peakWire;
  if (measuredPeakWire.invalid) {
    peakWire = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: [measuredPeakWire.path],
      reasons: measuredPeakWire.reasons,
      used: [measuredPeakWire]
    };
  } else if (!measuredPeakWire.missing) {
    peakWire = { status: 'COMPLETE', value: measuredPeakWire.value, missing: [], invalid: [], reasons: [], used: [measuredPeakWire] };
  } else {
    const factors = [uploadConcurrent, uploadPerStream];
    const invalid = collectInvalid(factors);
    const missing = collectMissing(factors);
    if (invalid.length > 0) {
      peakWire = { status: 'INVALID', value: null, missing, invalid, reasons: invalidReasons(factors), used: factors };
    } else if (missing.length > 0) {
      peakWire = {
        status: 'INCOMPLETE',
        value: null,
        missing,
        invalid: [],
        reasons: ['PEAK_INPUTS_MISSING'],
        used: factors,
        limitations: ['PEAK_INPUTS_MISSING']
      };
    } else {
      const value = uploadConcurrent.value * uploadPerStream.value;
      if (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER) {
        addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', 'mediaIngestStorage.upload.peakWire');
        peakWire = {
          status: 'INVALID',
          value: null,
          missing: [],
          invalid: ['mediaIngestStorage.upload.peakWire'],
          reasons: ['OVERFLOW_UNSAFE_INTEGER'],
          used: factors
        };
      } else {
        peakWire = { status: 'COMPLETE', value, missing: [], invalid: [], reasons: [], used: factors };
      }
    }
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.upload.peakWire',
      formulaId: 'F-UP-02',
      unit: 'bitsPerSecond',
      ...peakWire
    })
  );

  let peakPayload;
  if (peakWire.status === 'INVALID' || overhead.invalid) {
    peakPayload = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(peakWire.invalid ?? []), ...(overhead.invalid ? [overhead.path] : [])]),
      reasons: unique([...(peakWire.reasons ?? []), ...overhead.reasons]),
      used: [...(peakWire.used ?? []), overhead]
    };
  } else if (peakWire.status === 'COMPLETE' && peakWire.value === 0) {
    peakPayload = {
      status: 'COMPLETE',
      value: 0,
      missing: overhead.missing ? [overhead.path] : [],
      invalid: [],
      reasons: [],
      used: [...(peakWire.used ?? []), overhead],
      limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
    };
  } else if (peakWire.status !== 'COMPLETE' || overhead.missing) {
    peakPayload = {
      status: 'INCOMPLETE',
      value: null,
      missing: unique([...(peakWire.missing ?? []), ...(overhead.missing ? [overhead.path] : [])]),
      invalid: [],
      reasons: [],
      used: [...(peakWire.used ?? []), overhead]
    };
  } else {
    peakPayload = {
      status: 'COMPLETE',
      value: peakWire.value * (1 - overhead.value),
      missing: [],
      invalid: [],
      reasons: [],
      used: [...(peakWire.used ?? []), overhead],
      limitations: ['ESTIMATE_ONLY']
    };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.upload.peakPayloadEstimate',
      formulaId: 'F-UP-03',
      unit: 'bitsPerSecond',
      ...peakPayload
    })
  );

  const ongoingWire = makeMetric(ctx, {
    id: 'mediaIngestStorage.upload.ongoingWire',
    formulaId: 'F-UP-04',
    unit: 'bitsPerSecond',
    status: peakWire.status,
    value: peakWire.status === 'COMPLETE' ? peakWire.value : null,
    missing: peakWire.missing,
    invalid: peakWire.invalid,
    reasons: peakWire.reasons,
    used: peakWire.used,
    limitations: peakWire.limitations ?? []
  });
  metrics.push(ongoingWire);

  const backlogMetric = metricById(spoolMetrics, 'spool.recoveryBacklog');
  const netMetric = metricById(spoolMetrics, 'spool.recoveryNetPayloadBytesPerSecond');
  let catchUp;
  if (!backlogMetric || backlogMetric.status === 'INVALID') {
    catchUp = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: backlogMetric ? backlogMetric.invalid : ['spool.recoveryBacklog'],
      reasons: backlogMetric ? backlogMetric.reasons : [],
      used: backlogMetric ? backlogMetric.used : []
    };
  } else if (backlogMetric.status !== 'COMPLETE') {
    catchUp = {
      status: 'INCOMPLETE',
      value: null,
      missing: backlogMetric.missing,
      invalid: [],
      reasons: [],
      used: backlogMetric.used
    };
  } else if (backlogMetric.value === 0) {
    catchUp = {
      status: 'COMPLETE',
      value: 0,
      missing: netMetric && netMetric.missing ? netMetric.missing : [],
      invalid: [],
      reasons: [],
      used: backlogMetric.used,
      limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
    };
  } else if (!netMetric || netMetric.status === 'INVALID') {
    catchUp = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: netMetric ? netMetric.invalid : ['spool.recoveryNetPayloadBytesPerSecond'],
      reasons: netMetric ? netMetric.reasons : [],
      used: netMetric ? netMetric.used : []
    };
  } else if (netMetric.status !== 'COMPLETE') {
    catchUp = {
      status: 'INCOMPLETE',
      value: null,
      missing: netMetric.missing,
      invalid: [],
      reasons: [],
      used: netMetric.used
    };
  } else if (netMetric.value <= 0) {
    catchUp = {
      status: 'NOT_MET',
      value: 0,
      missing: [],
      invalid: [],
      reasons: ['DRAIN_IMPOSSIBLE'],
      used: [...(backlogMetric.used ?? []), ...(netMetric.used ?? [])]
    };
  } else {
    catchUp = {
      status: 'COMPLETE',
      value: netMetric.value * 8,
      missing: [],
      invalid: [],
      reasons: [],
      used: [...(backlogMetric.used ?? []), ...(netMetric.used ?? [])]
    };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.upload.backlogCatchUpPayloadBits',
      formulaId: 'F-UP-04',
      unit: 'bitsPerSecond',
      ...catchUp
    })
  );

  let catchUpWire;
  if (catchUp.status === 'INVALID' || overhead.invalid) {
    catchUpWire = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(catchUp.invalid ?? []), ...(overhead.invalid ? [overhead.path] : [])]),
      reasons: unique([...(catchUp.reasons ?? []), ...overhead.reasons]),
      used: [...(catchUp.used ?? []), overhead]
    };
  } else if (catchUp.status === 'NOT_MET') {
    catchUpWire = {
      status: 'NOT_MET',
      value: 0,
      missing: [],
      invalid: [],
      reasons: catchUp.reasons,
      used: [...(catchUp.used ?? []), overhead]
    };
  } else if (catchUp.status !== 'COMPLETE' || overhead.missing) {
    catchUpWire = {
      status: 'INCOMPLETE',
      value: null,
      missing: unique([...(catchUp.missing ?? []), ...(overhead.missing ? [overhead.path] : [])]),
      invalid: [],
      reasons: [],
      used: [...(catchUp.used ?? []), overhead]
    };
  } else if (catchUp.value === 0) {
    catchUpWire = {
      status: 'COMPLETE',
      value: 0,
      missing: [],
      invalid: [],
      reasons: [],
      used: [...(catchUp.used ?? []), overhead],
      limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
    };
  } else {
    catchUpWire = {
      status: 'COMPLETE',
      value: catchUp.value / (1 - overhead.value),
      missing: [],
      invalid: [],
      reasons: [],
      used: [...(catchUp.used ?? []), overhead],
      limitations: ['ESTIMATE_ONLY']
    };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'mediaIngestStorage.upload.backlogCatchUpWireEstimate',
      formulaId: 'F-UP-04',
      unit: 'bitsPerSecond',
      ...catchUpWire
    })
  );

  if (combined.invalid || (combined.present && !combined.missing)) {
    let combinedMetric;
    if (combined.invalid) {
      combinedMetric = {
        status: 'INVALID',
        value: null,
        missing: [],
        invalid: [combined.path],
        reasons: combined.reasons,
        used: [combined]
      };
    } else if (combined.missing) {
      combinedMetric = {
        status: 'INCOMPLETE',
        value: null,
        missing: [combined.path],
        invalid: [],
        reasons: [],
        used: [combined]
      };
    } else {
      combinedMetric = { status: 'COMPLETE', value: combined.value, missing: [], invalid: [], reasons: [], used: [combined] };
    }
    metrics.push(
      makeMetric(ctx, {
        id: 'mediaIngestStorage.upload.combinedMeasuredWire',
        formulaId: 'F-UP-04',
        unit: 'bitsPerSecond',
        ...combinedMetric
      })
    );
  }

  return groupResult('mediaIngestStorage', ['F-REC-01', 'F-REC-02', 'F-REC-03', 'F-REC-04', 'F-REC-05', 'F-REC-06', 'F-UP-01', 'F-UP-02', 'F-UP-03', 'F-UP-04'], null, metrics, [], extraInvalid);
}

function computeGrowthAndAsync(ctx, input) {
  const read = readGroup(ctx, input, 'growthAndAsync');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  const allowed = new Set([
    'META_BY_TYPE',
    'DB_AUX_BY_KIND',
    'QUEUE_ARRIVAL_TASKS_PS',
    'QUEUE_SERVICE_TASKS_PS',
    'QUEUE_BYTES_PER_TASK',
    'QUEUE_INITIAL_BACKLOG_TASKS',
    'AI_TASK_ARRIVALS_PS',
    'AI_TASK_SERVICE_RATE_PS',
    'AI_MEAN_SERVICE_S',
    'AI_TASK_BYTES'
  ]);
  checkUnknownKeys(ctx, group, allowed, 'growthAndAsync', extraInvalid);
  const metrics = [];

  const metaContainer = group.META_BY_TYPE;
  if (metaContainer !== undefined && !isPlainObject(metaContainer)) {
    addError(ctx, 'TYPE_INVALID', 'growthAndAsync.META_BY_TYPE');
    extraInvalid.push('growthAndAsync.META_BY_TYPE');
  } else if (isPlainObject(metaContainer)) {
    checkUnknownKeys(ctx, metaContainer, new Set(META_TYPES), 'growthAndAsync.META_BY_TYPE', extraInvalid);
  }
  for (const type of META_TYPES) {
    const entry = isPlainObject(metaContainer) && hasOwn(metaContainer, type) ? metaContainer[type] : undefined;
    const base = `growthAndAsync.META_BY_TYPE.${type}`;
    if (entry === undefined) {
      metrics.push(
        makeMetric(ctx, {
          id: `growthAndAsync.metadata.${type}`,
          formulaId: 'F-REC-07',
          unit: 'bytes',
          status: 'INCOMPLETE',
          missing: [`${base}.rowsPerDay`, `${base}.bytesPerRow`, `${base}.retentionDays`],
          used: []
        })
      );
      continue;
    }
    if (!isPlainObject(entry)) {
      addError(ctx, 'TYPE_INVALID', base);
      extraInvalid.push(base);
      metrics.push(
        makeMetric(ctx, {
          id: `growthAndAsync.metadata.${type}`,
          formulaId: 'F-REC-07',
          unit: 'bytes',
          status: 'INVALID',
          invalid: [base],
          reasons: ['TYPE_INVALID'],
          used: []
        })
      );
      continue;
    }
    checkUnknownKeys(ctx, entry, new Set(['rowsPerDay', 'bytesPerRow', 'retentionDays']), base, extraInvalid);
    const rows = readLeaf(ctx, entry, 'rowsPerDay', `${base}.rowsPerDay`, {
      type: 'number',
      unit: 'countPerDay',
      kind: 'DailyVolume'
    });
    const bytes = readLeaf(ctx, entry, 'bytesPerRow', `${base}.bytesPerRow`, {
      type: 'number',
      unit: 'bytes',
      kind: 'NonNegativeMeasure'
    });
    const retention = readLeaf(ctx, entry, 'retentionDays', `${base}.retentionDays`, {
      type: 'number',
      unit: 'days',
      kind: 'NonNegativeMeasure'
    });
    const product = multiply([rows, bytes, retention], ctx, `growthAndAsync.metadata.${type}`);
    metrics.push(
      makeMetric(ctx, {
        id: `growthAndAsync.metadata.${type}`,
        formulaId: 'F-REC-07',
        unit: 'bytes',
        ...product
      })
    );
  }

  const auxContainer = group.DB_AUX_BY_KIND;
  if (auxContainer !== undefined && !isPlainObject(auxContainer)) {
    addError(ctx, 'TYPE_INVALID', 'growthAndAsync.DB_AUX_BY_KIND');
    extraInvalid.push('growthAndAsync.DB_AUX_BY_KIND');
  } else if (isPlainObject(auxContainer)) {
    checkUnknownKeys(ctx, auxContainer, new Set(DB_AUX_KINDS), 'growthAndAsync.DB_AUX_BY_KIND', extraInvalid);
  }
  for (const kind of DB_AUX_KINDS) {
    const leaf = readLeaf(
      ctx,
      isPlainObject(auxContainer) ? auxContainer : null,
      kind,
      `growthAndAsync.DB_AUX_BY_KIND.${kind}`,
      { type: 'number', unit: 'bytes', kind: 'NonNegativeMeasure' }
    );
    metrics.push(
      makeMetric(ctx, {
        id: `growthAndAsync.dbAux.${kind}`,
        formulaId: 'F-DB-01',
        unit: 'bytes',
        status: leaf.invalid ? 'INVALID' : leaf.missing ? 'INCOMPLETE' : 'COMPLETE',
        value: leaf.usable ? leaf.value : null,
        missing: leaf.missing ? [leaf.path] : [],
        invalid: leaf.invalid ? [leaf.path] : [],
        reasons: leaf.reasons,
        used: [leaf]
      })
    );
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'growthAndAsync.dbSizingComplete',
      formulaId: 'F-DB-01',
      unit: 'boolean',
      status: 'NOT_VERIFIED',
      value: false,
      reasons: ['DB_SIZING_INCOMPLETE'],
      limitations: ['DB_SIZING_INCOMPLETE'],
      approvalStatus: 'UNAPPROVED'
    })
  );

  const queueArrival = readLeaf(ctx, group, 'QUEUE_ARRIVAL_TASKS_PS', 'growthAndAsync.QUEUE_ARRIVAL_TASKS_PS', {
    type: 'number',
    unit: 'tasksPerSecond',
    kind: 'NonNegativeRate'
  });
  const queueService = readLeaf(ctx, group, 'QUEUE_SERVICE_TASKS_PS', 'growthAndAsync.QUEUE_SERVICE_TASKS_PS', {
    type: 'number',
    unit: 'tasksPerSecond',
    kind: 'NonNegativeRate'
  });
  const queueBytes = readLeaf(ctx, group, 'QUEUE_BYTES_PER_TASK', 'growthAndAsync.QUEUE_BYTES_PER_TASK', {
    type: 'number',
    unit: 'bytesPerTask',
    kind: 'NonNegativeMeasure'
  });
  const queueBacklog = readLeaf(ctx, group, 'QUEUE_INITIAL_BACKLOG_TASKS', 'growthAndAsync.QUEUE_INITIAL_BACKLOG_TASKS', {
    type: 'number',
    unit: 'tasks',
    kind: 'DiscreteCount'
  });
  let net;
  if (queueArrival.invalid || queueService.invalid) {
    net = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(queueArrival.invalid ? [queueArrival.path] : []), ...(queueService.invalid ? [queueService.path] : [])]),
      reasons: unique([...queueArrival.reasons, ...queueService.reasons]),
      used: [queueArrival, queueService]
    };
  } else if (queueArrival.missing || queueService.missing) {
    net = {
      status: 'INCOMPLETE',
      value: null,
      missing: collectMissing([queueArrival, queueService]),
      invalid: [],
      reasons: [],
      used: [queueArrival, queueService]
    };
  } else {
    net = { status: 'COMPLETE', value: queueService.value - queueArrival.value, missing: [], invalid: [], reasons: [], used: [queueArrival, queueService] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'growthAndAsync.queue.netTasksPerSecond',
      formulaId: 'F-QUEUE-01',
      unit: 'tasksPerSecond',
      ...net
    })
  );
  let drain;
  if (queueBacklog.invalid) {
    drain = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: [queueBacklog.path],
      reasons: queueBacklog.reasons,
      used: [queueBacklog, queueArrival, queueService]
    };
  } else if (queueBacklog.missing) {
    drain = { status: 'INCOMPLETE', value: null, missing: [queueBacklog.path], invalid: [], reasons: [], used: [queueBacklog, queueArrival, queueService] };
  } else if (net.status === 'INVALID') {
    drain = { status: 'INVALID', value: null, missing: [], invalid: net.invalid, reasons: net.reasons, used: [...net.used, queueBacklog] };
  } else if (queueBacklog.value === 0) {
    const reasons = net.status === 'COMPLETE' && net.value === 0 ? ['IDLE_ZERO_DENOMINATOR'] : [];
    drain = {
      status: 'COMPLETE',
      value: 0,
      missing: net.status === 'COMPLETE' ? [] : net.missing ?? [],
      invalid: [],
      reasons,
      used: [queueBacklog, queueArrival, queueService]
    };
  } else if (net.status === 'INVALID') {
    drain = { status: 'INVALID', value: null, missing: [], invalid: net.invalid, reasons: net.reasons, used: [...net.used, queueBacklog] };
  } else if (net.status !== 'COMPLETE') {
    drain = { status: 'INCOMPLETE', value: null, missing: net.missing, invalid: [], reasons: [], used: [...net.used, queueBacklog] };
  } else if (net.value === 0) {
    drain = { status: 'NOT_MET', value: null, missing: [], invalid: [], reasons: ['BACKLOG_NOT_DRAINING'], used: [...net.used, queueBacklog] };
  } else if (net.value < 0) {
    drain = { status: 'NOT_MET', value: null, missing: [], invalid: [], reasons: ['BACKLOG_GROWING'], used: [...net.used, queueBacklog] };
  } else {
    drain = { status: 'COMPLETE', value: queueBacklog.value / net.value, missing: [], invalid: [], reasons: [], used: [...net.used, queueBacklog] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'growthAndAsync.queue.drainSeconds',
      formulaId: 'F-QUEUE-01',
      unit: 'seconds',
      ...drain
    })
  );
  let bytesChange;
  if ([queueArrival, queueService, queueBytes].some((rec) => rec.invalid)) {
    bytesChange = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: collectInvalid([queueArrival, queueService, queueBytes]),
      reasons: invalidReasons([queueArrival, queueService, queueBytes]),
      used: [queueArrival, queueService, queueBytes]
    };
  } else if ([queueArrival, queueService, queueBytes].some((rec) => rec.missing)) {
    bytesChange = {
      status: 'INCOMPLETE',
      value: null,
      missing: collectMissing([queueArrival, queueService, queueBytes]),
      invalid: [],
      reasons: [],
      used: [queueArrival, queueService, queueBytes]
    };
  } else {
    const value = (queueArrival.value - queueService.value) * queueBytes.value;
    if (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER) {
      addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', 'growthAndAsync.queue.backlogBytesChangePerSecond');
      bytesChange = { status: 'INVALID', value: null, missing: [], invalid: ['growthAndAsync.queue.backlogBytesChangePerSecond'], reasons: ['OVERFLOW_UNSAFE_INTEGER'], used: [queueArrival, queueService, queueBytes] };
    } else {
      bytesChange = { status: 'COMPLETE', value, missing: [], invalid: [], reasons: [], used: [queueArrival, queueService, queueBytes] };
    }
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'growthAndAsync.queue.backlogBytesChangePerSecond',
      formulaId: 'F-QUEUE-01',
      unit: 'bytesPerSecond',
      ...bytesChange
    })
  );

  const aiArrivals = readLeaf(ctx, group, 'AI_TASK_ARRIVALS_PS', 'growthAndAsync.AI_TASK_ARRIVALS_PS', {
    type: 'number',
    unit: 'tasksPerSecond',
    kind: 'NonNegativeRate'
  });
  const aiService = readLeaf(ctx, group, 'AI_TASK_SERVICE_RATE_PS', 'growthAndAsync.AI_TASK_SERVICE_RATE_PS', {
    type: 'number',
    unit: 'tasksPerSecond',
    kind: 'NonNegativeRate'
  });
  const aiMean = readLeaf(ctx, group, 'AI_MEAN_SERVICE_S', 'growthAndAsync.AI_MEAN_SERVICE_S', {
    type: 'number',
    unit: 'seconds',
    kind: 'DurationSeconds'
  });
  const aiBytes = readLeaf(ctx, group, 'AI_TASK_BYTES', 'growthAndAsync.AI_TASK_BYTES', {
    type: 'number',
    unit: 'bytesPerTask',
    kind: 'NonNegativeMeasure'
  });
  let loadRatio;
  if (aiArrivals.invalid || aiService.invalid) {
    loadRatio = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(aiArrivals.invalid ? [aiArrivals.path] : []), ...(aiService.invalid ? [aiService.path] : [])]),
      reasons: unique([...aiArrivals.reasons, ...aiService.reasons]),
      used: [aiArrivals, aiService]
    };
  } else if (aiArrivals.missing) {
    loadRatio = { status: 'INCOMPLETE', value: null, missing: [aiArrivals.path], invalid: [], reasons: [], used: [aiArrivals, aiService] };
  } else if (aiArrivals.value === 0) {
    const reasons = aiService.missing ? [] : aiService.value === 0 ? ['IDLE_ZERO_DENOMINATOR'] : [];
    loadRatio = {
      status: 'COMPLETE',
      value: 0,
      missing: aiService.missing ? [aiService.path] : [],
      invalid: [],
      reasons,
      used: [aiArrivals, aiService],
      limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
    };
  } else if (aiService.missing) {
    loadRatio = { status: 'INCOMPLETE', value: null, missing: [aiService.path], invalid: [], reasons: [], used: [aiArrivals, aiService] };
  } else if (aiService.value === 0) {
    loadRatio = { status: 'NOT_MET', value: null, missing: [], invalid: [], reasons: ['DIVISION_BY_ZERO'], used: [aiArrivals, aiService] };
  } else {
    loadRatio = { status: 'COMPLETE', value: aiArrivals.value / aiService.value, missing: [], invalid: [], reasons: [], used: [aiArrivals, aiService] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'growthAndAsync.ai.loadRatio',
      formulaId: 'F-AI-01',
      unit: 'ratio',
      ...loadRatio,
      limitations: [...(loadRatio.limitations ?? []), 'LOAD_RATIO_NOT_OCCUPANCY', 'AVERAGE_NOT_PEAK']
    })
  );
  let avgInService;
  if (aiArrivals.invalid || aiMean.invalid) {
    avgInService = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(aiArrivals.invalid ? [aiArrivals.path] : []), ...(aiMean.invalid ? [aiMean.path] : [])]),
      reasons: unique([...aiArrivals.reasons, ...aiMean.reasons]),
      used: [aiArrivals, aiMean]
    };
  } else if (aiArrivals.missing) {
    avgInService = { status: 'INCOMPLETE', value: null, missing: [aiArrivals.path], invalid: [], reasons: [], used: [aiArrivals, aiMean] };
  } else if (aiArrivals.value === 0) {
    avgInService = {
      status: 'COMPLETE',
      value: 0,
      missing: aiMean.missing ? [aiMean.path] : [],
      invalid: [],
      reasons: [],
      used: [aiArrivals, aiMean],
      limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
    };
  } else if (aiMean.missing) {
    avgInService = { status: 'INCOMPLETE', value: null, missing: [aiMean.path], invalid: [], reasons: [], used: [aiArrivals, aiMean] };
  } else {
    const value = aiArrivals.value * aiMean.value;
    avgInService = Number.isFinite(value) && Math.abs(value) <= Number.MAX_SAFE_INTEGER
      ? { status: 'COMPLETE', value, missing: [], invalid: [], reasons: [], used: [aiArrivals, aiMean] }
      : { status: 'INVALID', value: null, missing: [], invalid: ['growthAndAsync.ai.avgInServiceTasks'], reasons: ['OVERFLOW_UNSAFE_INTEGER'], used: [aiArrivals, aiMean] };
    if (avgInService.status === 'INVALID') addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', 'growthAndAsync.ai.avgInServiceTasks');
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'growthAndAsync.ai.avgInServiceTasks',
      formulaId: 'F-AI-01',
      unit: 'tasks',
      ...avgInService,
      limitations: [...(avgInService.limitations ?? []), 'AVERAGE_NOT_PEAK']
    })
  );
  let aiBytesPerS;
  if (aiArrivals.invalid || aiBytes.invalid) {
    aiBytesPerS = {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...(aiArrivals.invalid ? [aiArrivals.path] : []), ...(aiBytes.invalid ? [aiBytes.path] : [])]),
      reasons: unique([...aiArrivals.reasons, ...aiBytes.reasons]),
      used: [aiArrivals, aiBytes]
    };
  } else if (aiArrivals.missing) {
    aiBytesPerS = { status: 'INCOMPLETE', value: null, missing: [aiArrivals.path], invalid: [], reasons: [], used: [aiArrivals, aiBytes] };
  } else if (aiArrivals.value === 0) {
    aiBytesPerS = {
      status: 'COMPLETE',
      value: 0,
      missing: aiBytes.missing ? [aiBytes.path] : [],
      invalid: [],
      reasons: [],
      used: [aiArrivals, aiBytes],
      limitations: ['ZERO_FACTOR_SHORT_CIRCUIT']
    };
  } else if (aiBytes.missing) {
    aiBytesPerS = { status: 'INCOMPLETE', value: null, missing: [aiBytes.path], invalid: [], reasons: [], used: [aiArrivals, aiBytes] };
  } else {
    const value = aiArrivals.value * aiBytes.value;
    aiBytesPerS = Number.isFinite(value) && Math.abs(value) <= Number.MAX_SAFE_INTEGER
      ? { status: 'COMPLETE', value, missing: [], invalid: [], reasons: [], used: [aiArrivals, aiBytes] }
      : { status: 'INVALID', value: null, missing: [], invalid: ['growthAndAsync.ai.bytesPerSecond'], reasons: ['OVERFLOW_UNSAFE_INTEGER'], used: [aiArrivals, aiBytes] };
    if (aiBytesPerS.status === 'INVALID') addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', 'growthAndAsync.ai.bytesPerSecond');
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'growthAndAsync.ai.bytesPerSecond',
      formulaId: 'F-AI-01',
      unit: 'bytesPerSecond',
      ...aiBytesPerS
    })
  );

  return groupResult('growthAndAsync', ['F-REC-07', 'F-DB-01', 'F-QUEUE-01', 'F-AI-01'], null, metrics, [], extraInvalid);
}

function readPlainRoleField(entry, key, rolePath, ctx, options = {}) {
  const result = { present: false, missing: true, invalid: false, value: null, reasons: [] };
  if (!isPlainObject(entry) || !hasOwn(entry, key)) return result;
  result.present = true;
  const raw = entry[key];
  if (typeof raw !== 'string' || raw.trim() === '') {
    result.invalid = true;
    result.reasons.push('TYPE_INVALID');
    addError(ctx, 'TYPE_INVALID', `${rolePath}.${key}`);
    return result;
  }
  if (options.allowed && !options.allowed.includes(raw)) {
    result.invalid = true;
    result.reasons.push(options.code ?? 'TYPE_INVALID');
    addError(ctx, options.code ?? 'TYPE_INVALID', `${rolePath}.${key}`);
    return result;
  }
  result.missing = false;
  result.value = raw;
  return result;
}

function roleLeaf(ctx, entry, key, rolePath, spec) {
  return readLeaf(ctx, entry, key, `${rolePath}.${key}`, spec);
}

function evidenceComplete(role, entry, rolePath) {
  const refOk = isPlainObject(entry) && typeof entry.resourceEvidenceRef === 'string' && entry.resourceEvidenceRef.trim() !== '';
  const measuredAtOk = role.measuredAt && role.measuredAt.usable && dateValid(role.measuredAt.value);
  return { refOk, measuredAtOk };
}

function utilizationChecks(role) {
  const labels = [
    ['measuredCpuUtilization', 'CPU'],
    ['measuredRamUtilization', 'RAM'],
    ['measuredConnectionsUtilization', 'connections'],
    ['measuredIopsUtilization', 'IOPS']
  ];
  const missing = [];
  const invalid = [];
  const reasons = [];
  let exceeds = false;
  for (const [key] of labels) {
    const rec = role[key];
    if (!rec || rec.missing) missing.push(rec ? rec.path : key);
    else if (rec.invalid) {
      invalid.push(rec.path);
      reasons.push(...rec.reasons);
    } else if (role.headroom && role.headroom.usable && rec.value + role.headroom.value > 1) {
      exceeds = true;
    }
  }
  if (role.headroom && role.headroom.missing) missing.push(role.headroom.path);
  if (role.headroom && role.headroom.invalid) {
    invalid.push(role.headroom.path);
    reasons.push(...role.headroom.reasons);
  }
  return { missing: unique(missing), invalid: unique(invalid), reasons: unique(reasons), exceeds };
}

function computeMeasuredNodes(ctx, input) {
  const read = readGroup(ctx, input, 'measuredNodes');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  checkUnknownKeys(ctx, group, new Set(['MEASURED_ROLE']), 'measuredNodes', extraInvalid);
  const roleEntries = group.MEASURED_ROLE;
  if (roleEntries !== undefined && !Array.isArray(roleEntries)) {
    addError(ctx, 'TYPE_INVALID', 'measuredNodes.MEASURED_ROLE');
    extraInvalid.push('measuredNodes.MEASURED_ROLE');
  }
  const entries = Array.isArray(roleEntries) ? roleEntries : [];
  const metrics = [];
  const seen = new Set();
  const playbackConcurrent = readLeaf(
    ctx,
    isPlainObject(input.playback) ? input.playback : null,
    'PB_CONCURRENT_SESSIONS',
    'playback.PB_CONCURRENT_SESSIONS',
    { type: 'number', unit: 'count', kind: 'DiscreteCount' }
  );
  const aiInputPresent =
    isPlainObject(input.growthAndAsync) &&
    isPlainObject(input.growthAndAsync.AI_TASK_ARRIVALS_PS) &&
    input.growthAndAsync.AI_TASK_ARRIVALS_PS.value !== null &&
    input.growthAndAsync.AI_TASK_ARRIVALS_PS.value !== undefined;
  const required = [...REQUIRED_ROLES, ...(aiInputPresent ? ['media-ai-worker'] : [])];
  const declared = new Set();

  entries.forEach((entry, index) => {
    const rolePath = `measuredNodes.MEASURED_ROLE[${index}]`;
    if (!isPlainObject(entry)) {
      addError(ctx, 'TYPE_INVALID', rolePath);
      extraInvalid.push(rolePath);
      metrics.push(
        makeMetric(ctx, {
          id: `measuredNodes.unknown[${index}]`,
          formulaId: 'F-NODE-06',
          unit: 'enum',
          status: 'INVALID',
          invalid: [rolePath],
          reasons: ['TYPE_INVALID'],
          used: []
        })
      );
      return;
    }
    const roleName = readPlainRoleField(entry, 'role', rolePath, ctx, { code: 'UNKNOWN_ROLE', allowed: Object.keys(ROLE_DEFINITIONS) });
    if (roleName.invalid || roleName.missing) {
      metrics.push(
        makeMetric(ctx, {
          id: `measuredNodes.unknown[${index}]`,
          formulaId: 'F-NODE-06',
          unit: 'enum',
          status: roleName.invalid ? 'INVALID' : 'INCOMPLETE',
          value: roleName.missing ? null : entry.role,
          missing: roleName.missing ? [`${rolePath}.role`] : [],
          invalid: roleName.invalid ? [`${rolePath}.role`] : [],
          reasons: roleName.reasons,
          used: []
        })
      );
      return;
    }
    const roleKey = roleName.value;
    declared.add(roleKey);
    if (seen.has(roleKey)) {
      addError(ctx, 'CAPACITY_INCONSISTENT', rolePath);
      metrics.push(
        makeMetric(ctx, {
          id: `measuredNodes.${roleKey}`,
          formulaId: 'F-NODE-06',
          unit: 'enum',
          status: 'INVALID',
          invalid: [rolePath],
          reasons: ['CAPACITY_INCONSISTENT'],
          used: []
        })
      );
      return;
    }
    seen.add(roleKey);
    const definition = ROLE_DEFINITIONS[roleKey];
    const roleMetrics = computeRole(ctx, entry, roleKey, definition, rolePath, index, playbackConcurrent, extraInvalid);
    metrics.push(...roleMetrics);
  });

  for (const roleKey of required) {
    if (!declared.has(roleKey)) {
      metrics.push(
        makeMetric(ctx, {
          id: `measuredNodes.${roleKey}`,
          formulaId: 'F-NODE-06',
          unit: 'enum',
          status: 'INCOMPLETE',
          missing: [`measuredNodes.MEASURED_ROLE.${roleKey}`],
          reasons: ['ROLE_NOT_DECLARED'],
          limitations: ['ROLE_NOT_DECLARED'],
          used: []
        })
      );
    }
  }

  return groupResult(
    'measuredNodes',
    ['F-NODE-01', 'F-NODE-02', 'F-NODE-03', 'F-NODE-04', 'F-NODE-05', 'F-NODE-06'],
    null,
    metrics,
    [],
    extraInvalid
  );
}

function computeRole(ctx, entry, roleKey, definition, rolePath, index, playbackConcurrent, extraInvalid) {
  const metrics = [];
  const structuralInvalid = [];
  const structuralMissing = [];
  const structuralReasons = [];
  const demandUnit = readPlainRoleField(entry, 'demandUnit', rolePath, ctx, { code: 'ROLE_FIELD_MISMATCH' });
  if (demandUnit.missing) {
    structuralMissing.push(`${rolePath}.demandUnit`);
  } else if (demandUnit.invalid) {
    structuralInvalid.push(`${rolePath}.demandUnit`);
    structuralReasons.push(...demandUnit.reasons);
  } else if (demandUnit.value !== definition.demandUnit) {
    addError(ctx, 'ROLE_FIELD_MISMATCH', `${rolePath}.demandUnit`);
    structuralInvalid.push(`${rolePath}.demandUnit`);
    structuralReasons.push('ROLE_FIELD_MISMATCH');
  }
  const allowed = ROLE_FIELDS[definition.kind];
  for (const key of Object.keys(entry)) {
    if (!allowed.has(key)) {
      const code = definition.kind === 'gateway' && (key === 'perNodeRate' || key === 'deployedNodes') ? 'SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN' : 'ROLE_FIELD_MISMATCH';
      addError(ctx, code, `${rolePath}.${key}`);
      structuralInvalid.push(`${rolePath}.${key}`);
      structuralReasons.push(code);
      if (code === 'SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN') extraInvalid.push(`${rolePath}.${key}`);
    }
  }
  // Known fields that belong to another role kind still produce ROLE_FIELD_MISMATCH
  // (the loop above catches them because they are absent from the role kind's set).
  const headroom = roleLeaf(ctx, entry, 'headroomRatio', rolePath, { type: 'number', unit: 'ratio', kind: 'Ratio01' });
  const cpu = roleLeaf(ctx, entry, 'measuredCpuUtilization', rolePath, { type: 'number', unit: 'ratio', kind: 'Ratio01' });
  const ram = roleLeaf(ctx, entry, 'measuredRamUtilization', rolePath, { type: 'number', unit: 'ratio', kind: 'Ratio01' });
  const conn = roleLeaf(ctx, entry, 'measuredConnectionsUtilization', rolePath, { type: 'number', unit: 'ratio', kind: 'Ratio01' });
  const iops = roleLeaf(ctx, entry, 'measuredIopsUtilization', rolePath, { type: 'number', unit: 'ratio', kind: 'Ratio01' });
  const measuredAt = roleLeaf(ctx, entry, 'measuredAt', rolePath, {
    type: 'string',
    unit: 'iso8601'
  });
  if (measuredAt.present && !measuredAt.missing && !measuredAt.invalid && !dateValid(measuredAt.value)) {
    addError(ctx, 'DATE_INVALID', measuredAt.path);
    measuredAt.invalid = true;
    measuredAt.reasons.push('DATE_INVALID');
  }
  const role = {
    headroom,
    measuredCpuUtilization: cpu,
    measuredRamUtilization: ram,
    measuredConnectionsUtilization: conn,
    measuredIopsUtilization: iops,
    measuredAt
  };
  const evidence = evidenceComplete(role, entry, rolePath);
  const resourceMissing = [];
  if (!evidence.refOk) resourceMissing.push(`${rolePath}.resourceEvidenceRef`);
  if (!evidence.measuredAtOk) resourceMissing.push(`${rolePath}.measuredAt`);
  for (const rec of [cpu, ram, conn, iops]) {
    if (rec.missing) resourceMissing.push(rec.path);
  }
  const resourceInvalid = collectInvalid([cpu, ram, conn, iops, headroom, measuredAt]);
  if (headroom.missing) resourceMissing.push(headroom.path);
  const resourceLimitations = ['MEASUREMENT_REQUIRED'];
  const checks = utilizationChecks(role);
  const missingResource = unique([...resourceMissing, ...checks.missing]);
  const invalidResource = unique([...resourceInvalid, ...checks.invalid]);
  const resourceReasons = unique([...invalidReasons([cpu, ram, conn, iops, headroom, measuredAt]), ...checks.reasons]);
  let resourceMetric;
  if (invalidResource.length > 0) {
    resourceMetric = {
      status: 'INVALID',
      value: null,
      missing: missingResource,
      invalid: invalidResource,
      reasons: resourceReasons,
      used: [cpu, ram, conn, iops, headroom, measuredAt]
    };
  } else if (missingResource.length > 0) {
    resourceMetric = {
      status: 'NOT_VERIFIED',
      value: null,
      missing: missingResource,
      invalid: [],
      reasons: ['RESOURCE_EVIDENCE_MISSING'],
      used: [cpu, ram, conn, iops, headroom, measuredAt],
      limitations: ['RESOURCE_EVIDENCE_MISSING']
    };
  } else if (checks.exceeds) {
    resourceMetric = {
      status: 'NOT_MET',
      value: false,
      missing: [],
      invalid: [],
      reasons: ['RESOURCE_HEADROOM_INSUFFICIENT'],
      used: [cpu, ram, conn, iops, headroom],
      limitations: ['RESOURCE_HEADROOM_INSUFFICIENT']
    };
  } else {
    resourceMetric = {
      status: 'COMPLETE',
      value: true,
      missing: [],
      invalid: [],
      reasons: [],
      used: [cpu, ram, conn, iops, headroom, measuredAt]
    };
  }
  metrics.push(
    makeMetric(ctx, {
      id: `measuredNodes.${roleKey}.resourceEvidence`,
      formulaId: 'F-NODE-03',
      unit: 'boolean',
      ...resourceMetric
    })
  );

  let verdictInputs = {
    invalid: structuralInvalid,
    reasons: structuralReasons,
    resource: resourceMetric
  };

  if (definition.kind === 'stateless') {
    const demand = roleLeaf(ctx, entry, 'demandQuantity', rolePath, { type: 'number', unit: definition.demandUnit, kind: 'NonNegativeMeasure' });
    const perNode = roleLeaf(ctx, entry, 'perNodeRate', rolePath, { type: 'number', unit: definition.demandUnit, kind: 'NonNegativeRate' });
    const deployed = roleLeaf(ctx, entry, 'deployedNodes', rolePath, { type: 'number', unit: 'nodes', kind: 'DiscreteCount' });
    let requiredMetric;
    if (demand.invalid || perNode.invalid || deployed.invalid) {
      requiredMetric = {
        status: 'INVALID',
        value: null,
        missing: collectMissing([demand, perNode, deployed]),
        invalid: collectInvalid([demand, perNode, deployed]),
        reasons: invalidReasons([demand, perNode, deployed]),
        used: [demand, perNode, deployed]
      };
    } else if (demand.missing) {
      requiredMetric = { status: 'INCOMPLETE', value: null, missing: [demand.path], invalid: [], reasons: [], used: [demand, perNode, deployed] };
    } else if (demand.value === 0) {
      requiredMetric = {
        status: 'COMPLETE',
        value: 0,
        missing: collectMissing([perNode, deployed, headroom]),
        invalid: [],
        reasons: ['ZERO_DEMAND_NOT_A_FIT'],
        used: [demand, perNode, deployed, headroom],
        limitations: ['ZERO_DEMAND_NOT_A_FIT']
      };
    } else if (perNode.missing || headroom.missing) {
      requiredMetric = {
        status: 'INCOMPLETE',
        value: null,
        missing: collectMissing([perNode, headroom]),
        invalid: [],
        reasons: ['MEASUREMENT_REQUIRED'],
        used: [demand, perNode, deployed, headroom],
        limitations: ['MEASUREMENT_REQUIRED']
      };
    } else {
      const denominator = perNode.value * (1 - headroom.value);
      if (!(denominator > 0)) {
        requiredMetric = {
          status: 'INCOMPLETE',
          value: null,
          missing: [],
          invalid: [],
          reasons: ['MEASUREMENT_REQUIRED'],
          used: [demand, perNode, deployed, headroom],
          limitations: ['MEASUREMENT_REQUIRED']
        };
      } else {
        const value = Math.ceil(demand.value / denominator);
        if (!Number.isFinite(value) || !Number.isSafeInteger(value)) {
          addError(ctx, 'OVERFLOW_UNSAFE_INTEGER', `measuredNodes.${roleKey}.requiredNodes`);
          requiredMetric = {
            status: 'INVALID',
            value: null,
            missing: [],
            invalid: [`measuredNodes.${roleKey}.requiredNodes`],
            reasons: ['OVERFLOW_UNSAFE_INTEGER'],
            used: [demand, perNode, deployed, headroom]
          };
        } else {
          requiredMetric = { status: 'COMPLETE', value, missing: [], invalid: [], reasons: [], used: [demand, perNode, deployed, headroom] };
        }
      }
    }
    metrics.push(
      makeMetric(ctx, {
        id: `measuredNodes.${roleKey}.requiredNodes`,
        formulaId: 'F-NODE-01',
        unit: 'nodes',
        ...requiredMetric
      })
    );
    let nMinusOne;
    if (demand.invalid || perNode.invalid || deployed.invalid || headroom.invalid) {
      nMinusOne = {
        status: 'INVALID',
        value: null,
        missing: collectMissing([demand, perNode, deployed, headroom]),
        invalid: collectInvalid([demand, perNode, deployed, headroom]),
        reasons: invalidReasons([demand, perNode, deployed, headroom]),
        used: [demand, perNode, deployed, headroom]
      };
    } else if (demand.missing || perNode.missing || deployed.missing || headroom.missing) {
      nMinusOne = {
        status: 'NOT_VERIFIED',
        value: null,
        missing: collectMissing([demand, perNode, deployed, headroom]),
        invalid: [],
        reasons: ['MEASUREMENT_REQUIRED'],
        used: [demand, perNode, deployed, headroom],
        limitations: ['MEASUREMENT_REQUIRED']
      };
    } else if (deployed.value < 2) {
      nMinusOne = {
        status: 'NOT_MET',
        value: false,
        missing: [],
        invalid: [],
        reasons: ['N_MINUS_ONE_IMPOSSIBLE'],
        used: [demand, perNode, deployed, headroom],
        limitations: ['N_MINUS_ONE_IMPOSSIBLE']
      };
    } else {
      const requiredMetric = metricById(metrics, `measuredNodes.${roleKey}.requiredNodes`);
      if (!requiredMetric || requiredMetric.status !== 'COMPLETE') {
        nMinusOne = {
          status: 'NOT_VERIFIED',
          value: null,
          missing: requiredMetric ? requiredMetric.missing : [`measuredNodes.${roleKey}.requiredNodes`],
          invalid: [],
          reasons: ['MEASUREMENT_REQUIRED'],
          used: [demand, perNode, deployed, headroom],
          limitations: ['MEASUREMENT_REQUIRED']
        };
      } else {
        const capacity = perNode.value * (1 - headroom.value) * (deployed.value - 1);
        const requiredValue = requiredMetric.value;
        const belowRequired = deployed.value < requiredValue;
        const fit = !belowRequired && demand.value <= capacity;
        nMinusOne = {
          status: fit ? 'COMPLETE' : 'NOT_MET',
          value: fit,
          missing: [],
          invalid: [],
          reasons: belowRequired ? ['DEPLOYED_BELOW_REQUIRED'] : fit ? [] : ['TARGET_NOT_MET'],
          used: [demand, perNode, deployed, headroom],
          limitations: belowRequired ? ['DEPLOYED_BELOW_REQUIRED'] : []
        };
      }
    }
    metrics.push(
      makeMetric(ctx, {
        id: `measuredNodes.${roleKey}.nMinusOne`,
        formulaId: 'F-NODE-02',
        unit: 'boolean',
        ...nMinusOne
      })
    );
    const requiredMetricRef = metricById(metrics, `measuredNodes.${roleKey}.requiredNodes`);
    const fitMetrics = [requiredMetricRef, metricById(metrics, `measuredNodes.${roleKey}.nMinusOne`)];
    const demandLeaves = [demand];
    const capacityLeaves = [perNode];
    verdictInputs = { ...verdictInputs, fitMetrics, demandLeaves, capacityLeaves, headroom, demand };
  } else if (definition.kind === 'stateful') {
    const demand = roleLeaf(ctx, entry, 'demandQuantity', rolePath, { type: 'number', unit: definition.demandUnit, kind: 'NonNegativeMeasure' });
    const normal = roleLeaf(ctx, entry, 'measuredNormalTopologyCapacity', rolePath, { type: 'number', unit: definition.demandUnit, kind: 'NonNegativeMeasure' });
    const degraded = roleLeaf(ctx, entry, 'measuredDegradedTopologyCapacity', rolePath, { type: 'number', unit: definition.demandUnit, kind: 'NonNegativeMeasure' });
    const makeFit = (capacityRec, label) => {
      if (demand.invalid || capacityRec.invalid || headroom.invalid) {
        return {
          status: 'INVALID',
          value: null,
          missing: collectMissing([demand, capacityRec, headroom]),
          invalid: collectInvalid([demand, capacityRec, headroom]),
          reasons: invalidReasons([demand, capacityRec, headroom]),
          used: [demand, capacityRec, headroom]
        };
      }
      if (demand.missing || capacityRec.missing || headroom.missing) {
        return {
          status: 'NOT_VERIFIED',
          value: null,
          missing: collectMissing([demand, capacityRec, headroom]),
          invalid: [],
          reasons: ['MEASUREMENT_REQUIRED'],
          used: [demand, capacityRec, headroom],
          limitations: ['MEASUREMENT_REQUIRED']
        };
      }
      const fit = demand.value <= capacityRec.value * (1 - headroom.value);
      return {
        status: fit ? 'COMPLETE' : 'NOT_MET',
        value: fit,
        missing: [],
        invalid: [],
        reasons: fit ? [] : ['TARGET_NOT_MET'],
        used: [demand, capacityRec, headroom]
      };
    };
    const normalFit = makeFit(normal, 'normal');
    const degradedFit = makeFit(degraded, 'degraded');
    metrics.push(
      makeMetric(ctx, {
        id: `measuredNodes.${roleKey}.normalFit`,
        formulaId: 'F-NODE-04',
        unit: 'boolean',
        ...normalFit
      })
    );
    metrics.push(
      makeMetric(ctx, {
        id: `measuredNodes.${roleKey}.degradedFit`,
        formulaId: 'F-NODE-04',
        unit: 'boolean',
        ...degradedFit
      })
    );
    verdictInputs = {
      ...verdictInputs,
      fitMetrics: [
        metricById(metrics, `measuredNodes.${roleKey}.normalFit`),
        metricById(metrics, `measuredNodes.${roleKey}.degradedFit`)
      ],
      demandLeaves: [demand],
      capacityLeaves: [normal, degraded],
      headroom,
      demand
    };
  } else {
    const roleDemand = entry.demandQuantity === undefined ? null : roleLeaf(ctx, entry, 'demandQuantity', rolePath, { type: 'number', unit: 'sessions', kind: 'NonNegativeMeasure' });
    const shared = roleLeaf(ctx, entry, 'sharedCoreApiEdgeCapacity', rolePath, { type: 'number', unit: 'sessions', kind: 'NonNegativeMeasure' });
    let demandMismatch = false;
    if (roleDemand) {
      if (roleDemand.invalid) {
        demandMismatch = true;
      } else if (!roleDemand.missing) {
        if (
          playbackConcurrent.missing ||
          playbackConcurrent.invalid ||
          roleDemand.value !== playbackConcurrent.value
        ) {
          addError(ctx, 'ROLE_FIELD_MISMATCH', roleDemand.path);
          demandMismatch = true;
        }
      }
    }
    let sharedFit;
    if (demandMismatch) {
      sharedFit = {
        status: 'INVALID',
        value: null,
        missing: [],
        invalid: [roleDemand.path],
        reasons: ['ROLE_FIELD_MISMATCH'],
        used: [roleDemand]
      };
    } else if (playbackConcurrent.invalid || shared.invalid || headroom.invalid) {
      sharedFit = {
        status: 'INVALID',
        value: null,
        missing: collectMissing([playbackConcurrent, shared, headroom]),
        invalid: collectInvalid([playbackConcurrent, shared, headroom]),
        reasons: invalidReasons([playbackConcurrent, shared, headroom]),
        used: [playbackConcurrent, shared, headroom]
      };
    } else if (playbackConcurrent.missing || shared.missing || headroom.missing) {
      sharedFit = {
        status: 'NOT_VERIFIED',
        value: null,
        missing: collectMissing([playbackConcurrent, shared, headroom]),
        invalid: [],
        reasons: ['MEASUREMENT_REQUIRED'],
        used: [playbackConcurrent, shared, headroom],
        limitations: ['MEASUREMENT_REQUIRED', 'SHARED_MEDIA_GATEWAY']
      };
    } else {
      const fit = playbackConcurrent.value <= shared.value * (1 - headroom.value);
      sharedFit = {
        status: fit ? 'COMPLETE' : 'NOT_MET',
        value: fit,
        missing: [],
        invalid: [],
        reasons: fit ? [] : ['TARGET_NOT_MET'],
        used: [playbackConcurrent, shared, headroom],
        limitations: ['SHARED_MEDIA_GATEWAY']
      };
    }
    metrics.push(
      makeMetric(ctx, {
        id: `measuredNodes.${roleKey}.sharedFit`,
        formulaId: 'F-NODE-05',
        unit: 'boolean',
        ...sharedFit
      })
    );
    verdictInputs = {
      ...verdictInputs,
      fitMetrics: [metricById(metrics, `measuredNodes.${roleKey}.sharedFit`)],
      demandLeaves: [playbackConcurrent],
      capacityLeaves: [shared],
      headroom,
      demand: playbackConcurrent
    };
  }

  const verdict = roleVerdict(ctx, roleKey, verdictInputs, structuralInvalid, structuralMissing, structuralReasons);
  metrics.push(
    makeMetric(ctx, {
      id: `measuredNodes.${roleKey}`,
      formulaId: 'F-NODE-06',
      unit: 'enum',
      ...verdict
    })
  );
  return metrics;
}

function roleVerdict(ctx, roleKey, inputs, structuralInvalid, structuralMissing, structuralReasons) {
  const resource = inputs.resource;
  const fitMetrics = inputs.fitMetrics ?? [];
  const allUsed = [
    ...(inputs.demandLeaves ?? []),
    ...(inputs.capacityLeaves ?? []),
    ...(inputs.headroom ? [inputs.headroom] : []),
    ...(resource.used ?? [])
  ];
  if (structuralInvalid.length > 0) {
    return {
      status: 'INVALID',
      value: null,
      missing: structuralMissing,
      invalid: structuralInvalid,
      reasons: structuralReasons,
      used: allUsed
    };
  }
  if (structuralMissing.length > 0) {
    return {
      status: 'INCOMPLETE',
      value: null,
      missing: structuralMissing,
      invalid: [],
      reasons: structuralReasons,
      used: allUsed
    };
  }
  const invalid = unique([
    ...fitMetrics.flatMap((metric) => (metric ? metric.invalid : [])),
    ...(resource.invalid ?? [])
  ]);
  if (invalid.length > 0) {
    return {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid,
      reasons: unique([
        ...fitMetrics.flatMap((metric) => (metric ? metric.reasons : [])),
        ...(resource.reasons ?? [])
      ]),
      used: allUsed
    };
  }
  const fitIncomplete = fitMetrics.some((metric) => !metric || metric.status === 'INCOMPLETE' || metric.status === 'NOT_VERIFIED');
  if (fitIncomplete) {
    return {
      status: 'INCOMPLETE',
      value: null,
      missing: unique(fitMetrics.flatMap((metric) => (metric ? metric.missing : []))),
      invalid: [],
      reasons: [],
      used: allUsed
    };
  }
  const anyNotMet = fitMetrics.some((metric) => metric && metric.status === 'NOT_MET') || resource.status === 'NOT_MET';
  if (anyNotMet) {
    return {
      status: 'NOT_MET',
      value: 'NOT_MET',
      missing: [],
      invalid: [],
      reasons: unique([
        ...fitMetrics.flatMap((metric) => (metric ? metric.reasons : [])),
        ...(resource.reasons ?? [])
      ]),
      used: allUsed
    };
  }
  const resourceVerified = resource.status === 'COMPLETE';
  if (!resourceVerified) {
    return {
      status: 'NOT_VERIFIED',
      value: 'NOT_VERIFIED',
      missing: resource.missing,
      invalid: [],
      reasons: ['RESOURCE_EVIDENCE_MISSING'],
      used: allUsed,
      limitations: ['RESOURCE_EVIDENCE_MISSING']
    };
  }
  const demandLeaves = inputs.demandLeaves ?? [];
  const capacityLeaves = inputs.capacityLeaves ?? [];
  const demandZero = inputs.demand && inputs.demand.usable && inputs.demand.value === 0;
  if (demandZero) {
    return {
      status: 'NOT_VERIFIED',
      value: 'ARITHMETIC_ONLY',
      missing: [],
      invalid: [],
      reasons: ['ZERO_DEMAND_NOT_A_FIT'],
      used: allUsed,
      limitations: ['ZERO_DEMAND_NOT_A_FIT']
    };
  }
  const measuredDemand = demandLeaves.every((rec) => rec && rec.usable && rec.provenance === 'MEASURED');
  const measuredCapacity = capacityLeaves.every((rec) => rec && rec.usable && rec.provenance === 'MEASURED');
  const headroomOk = inputs.headroom && inputs.headroom.usable && (inputs.headroom.provenance === 'MEASURED' || inputs.headroom.provenance === 'APPROVED_POLICY');
  const qualified = measuredDemand && measuredCapacity && headroomOk;
  if (!qualified) {
    return {
      status: 'NOT_VERIFIED',
      value: 'ARITHMETIC_ONLY',
      missing: [],
      invalid: [],
      reasons: [],
      used: allUsed,
      limitations: ['ESTIMATE_ONLY']
    };
  }
  return {
    status: 'COMPLETE',
    value: 'FIT_EVIDENCE_COMPLETE',
    missing: [],
    invalid: [],
    reasons: [],
    used: allUsed
  };
}

function computeSpool(ctx, input) {
  const read = readGroup(ctx, input, 'spool');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  checkUnknownKeys(
    ctx,
    group,
    new Set([
      'SPOOL_DISK_BYTES',
      'SPOOL_FREE_BYTES',
      'SPOOL_EXISTING_BYTES',
      'SPOOL_NEW_PEAK_BYTES_PS',
      'SPOOL_NEW_AVG_BYTES_PS',
      'SPOOL_OUTAGE_S',
      'SPOOL_HEADROOM_RATIO',
      'SPOOL_RECOVERY_TOTAL_BYTES_PS',
      'SPOOL_CONTINUING_BYTES_PS'
    ]),
    'spool',
    extraInvalid
  );
  const disk = readLeaf(ctx, group, 'SPOOL_DISK_BYTES', 'spool.SPOOL_DISK_BYTES', { type: 'number', unit: 'bytes', kind: 'NonNegativeMeasure' });
  const free = readLeaf(ctx, group, 'SPOOL_FREE_BYTES', 'spool.SPOOL_FREE_BYTES', { type: 'number', unit: 'bytes', kind: 'NonNegativeMeasure' });
  const existing = readLeaf(ctx, group, 'SPOOL_EXISTING_BYTES', 'spool.SPOOL_EXISTING_BYTES', { type: 'number', unit: 'bytes', kind: 'NonNegativeMeasure' });
  const newPeak = readLeaf(ctx, group, 'SPOOL_NEW_PEAK_BYTES_PS', 'spool.SPOOL_NEW_PEAK_BYTES_PS', { type: 'number', unit: 'bytesPerSecond', kind: 'NonNegativeRate' });
  const newAvg = readLeaf(ctx, group, 'SPOOL_NEW_AVG_BYTES_PS', 'spool.SPOOL_NEW_AVG_BYTES_PS', { type: 'number', unit: 'bytesPerSecond', kind: 'NonNegativeRate' });
  const outage = readLeaf(ctx, group, 'SPOOL_OUTAGE_S', 'spool.SPOOL_OUTAGE_S', { type: 'number', unit: 'seconds', kind: 'DurationSeconds', positive: true });
  const headroom = readLeaf(ctx, group, 'SPOOL_HEADROOM_RATIO', 'spool.SPOOL_HEADROOM_RATIO', { type: 'number', unit: 'ratio', kind: 'Ratio01' });
  const recoveryTotal = readLeaf(ctx, group, 'SPOOL_RECOVERY_TOTAL_BYTES_PS', 'spool.SPOOL_RECOVERY_TOTAL_BYTES_PS', { type: 'number', unit: 'bytesPerSecond', kind: 'NonNegativeRate' });
  const continuing = readLeaf(ctx, group, 'SPOOL_CONTINUING_BYTES_PS', 'spool.SPOOL_CONTINUING_BYTES_PS', { type: 'number', unit: 'bytesPerSecond', kind: 'NonNegativeRate' });
  const metrics = [];

  let used;
  if (disk.invalid || free.invalid) {
    used = { status: 'INVALID', value: null, missing: [], invalid: collectInvalid([disk, free]), reasons: invalidReasons([disk, free]), used: [disk, free] };
  } else if (disk.missing || free.missing) {
    used = { status: 'INCOMPLETE', value: null, missing: collectMissing([disk, free]), invalid: [], reasons: [], used: [disk, free] };
  } else if (free.value > disk.value) {
    addError(ctx, 'CAPACITY_INCONSISTENT', 'spool.SPOOL_FREE_BYTES');
    used = { status: 'INVALID', value: null, missing: [], invalid: ['spool.SPOOL_FREE_BYTES'], reasons: ['CAPACITY_INCONSISTENT'], used: [disk, free] };
  } else {
    const value = disk.value - free.value;
    if (existing.usable && existing.value > value) {
      addError(ctx, 'CAPACITY_INCONSISTENT', 'spool.SPOOL_EXISTING_BYTES');
      used = { status: 'INVALID', value: null, missing: [], invalid: ['spool.SPOOL_EXISTING_BYTES'], reasons: ['CAPACITY_INCONSISTENT'], used: [disk, free, existing] };
    } else {
      used = { status: 'COMPLETE', value, missing: [], invalid: [], reasons: [], used: [disk, free, existing] };
    }
  }
  metrics.push(makeMetric(ctx, { id: 'spool.used', formulaId: 'F-SPOOL-01', unit: 'bytes', ...used }));

  const producedPeak = multiply([newPeak, outage], ctx, 'spool.outageProducedPeak');
  const producedAvg = multiply([newAvg, outage], ctx, 'spool.outageProducedAvg');

  const sameWindowSource =
    newPeak.usable && newAvg.usable && typeof newPeak.leaf.source === 'string' && newPeak.leaf.source !== '' && newPeak.leaf.source === newAvg.leaf.source;
  const peakWindowLimitations = sameWindowSource ? [] : ['PEAK_AVG_WINDOW_UNQUALIFIED'];
  if (sameWindowSource && newPeak.value < newAvg.value) {
    addError(ctx, 'CAPACITY_INCONSISTENT', 'spool.SPOOL_NEW_PEAK_BYTES_PS');
    producedPeak.status = 'INVALID';
    producedPeak.value = null;
    producedPeak.invalid = ['spool.SPOOL_NEW_PEAK_BYTES_PS'];
    producedPeak.reasons = ['CAPACITY_INCONSISTENT'];
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'spool.outageProducedPeak',
      formulaId: 'F-SPOOL-01',
      unit: 'bytes',
      ...producedPeak,
      limitations: [...(producedPeak.limitations ?? []), ...peakWindowLimitations]
    })
  );
  metrics.push(makeMetric(ctx, { id: 'spool.outageProducedAvg', formulaId: 'F-SPOOL-01', unit: 'bytes', ...producedAvg }));

  const usedOperand = derivedOperand(metricById(metrics, 'spool.used'));
  let peakOccupancy;
  if (producedPeak.status === 'INVALID' || usedOperand.status === 'INVALID') {
    peakOccupancy = { status: 'INVALID', value: null, missing: [], invalid: unique([...(producedPeak.invalid ?? []), ...(usedOperand.invalid ?? [])]), reasons: unique([...(producedPeak.reasons ?? []), ...(usedOperand.reasons ?? [])]), used: [...(usedOperand.used ?? []), ...(producedPeak.used ?? [])] };
  } else if (producedPeak.status !== 'COMPLETE' || usedOperand.status !== 'COMPLETE') {
    peakOccupancy = { status: 'INCOMPLETE', value: null, missing: unique([...(producedPeak.missing ?? []), ...(usedOperand.missing ?? [])]), invalid: [], reasons: [], used: [...(usedOperand.used ?? []), ...(producedPeak.used ?? [])] };
  } else {
    peakOccupancy = { status: 'COMPLETE', value: usedOperand.value + producedPeak.value, missing: [], invalid: [], reasons: [], used: [...(usedOperand.used ?? []), ...(producedPeak.used ?? [])] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'spool.peakOccupancy',
      formulaId: 'F-SPOOL-01',
      unit: 'bytes',
      ...peakOccupancy,
      limitations: [...(peakOccupancy.limitations ?? []), ...peakWindowLimitations]
    })
  );
  const avgOperand = derivedOperand(metricById(metrics, 'spool.outageProducedAvg'));
  let avgOccupancy;
  if (avgOperand.status === 'INVALID' || usedOperand.status === 'INVALID') {
    avgOccupancy = { status: 'INVALID', value: null, missing: [], invalid: unique([...(avgOperand.invalid ?? []), ...(usedOperand.invalid ?? [])]), reasons: unique([...(avgOperand.reasons ?? []), ...(usedOperand.reasons ?? [])]), used: [...(usedOperand.used ?? []), ...(avgOperand.used ?? [])] };
  } else if (avgOperand.status !== 'COMPLETE' || usedOperand.status !== 'COMPLETE') {
    avgOccupancy = { status: 'INCOMPLETE', value: null, missing: unique([...(avgOperand.missing ?? []), ...(usedOperand.missing ?? [])]), invalid: [], reasons: [], used: [...(usedOperand.used ?? []), ...(avgOperand.used ?? [])] };
  } else {
    avgOccupancy = { status: 'COMPLETE', value: usedOperand.value + avgOperand.value, missing: [], invalid: [], reasons: [], used: [...(usedOperand.used ?? []), ...(avgOperand.used ?? [])] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'spool.avgOccupancy',
      formulaId: 'F-SPOOL-01',
      unit: 'bytes',
      ...avgOccupancy,
      limitations: [...(avgOccupancy.limitations ?? []), 'AVERAGE_NOT_PEAK', ...peakWindowLimitations]
    })
  );
  let backlog;
  if (existing.invalid || avgOperand.status === 'INVALID') {
    backlog = { status: 'INVALID', value: null, missing: [], invalid: unique([...(existing.invalid ? [existing.path] : []), ...(avgOperand.invalid ?? [])]), reasons: unique([...existing.reasons, ...(avgOperand.reasons ?? [])]), used: [existing, ...(avgOperand.used ?? [])] };
  } else if (existing.missing || avgOperand.status !== 'COMPLETE') {
    backlog = { status: 'INCOMPLETE', value: null, missing: unique([...(existing.missing ? [existing.path] : []), ...(avgOperand.missing ?? [])]), invalid: [], reasons: [], used: [existing, ...(avgOperand.used ?? [])] };
  } else {
    backlog = { status: 'COMPLETE', value: existing.value + avgOperand.value, missing: [], invalid: [], reasons: [], used: [existing, ...(avgOperand.used ?? [])] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'spool.recoveryBacklog',
      formulaId: 'F-SPOOL-01',
      unit: 'bytes',
      ...backlog
    })
  );

  const usable = multiplyOperands(
    [
      { status: disk.missing ? 'INCOMPLETE' : disk.invalid ? 'INVALID' : 'COMPLETE', value: disk.value, missing: disk.missing ? [disk.path] : [], invalid: disk.invalid ? [disk.path] : [], used: [disk] },
      { status: headroom.missing ? 'INCOMPLETE' : headroom.invalid ? 'INVALID' : 'COMPLETE', value: headroom.missing || headroom.invalid ? null : 1 - headroom.value, missing: headroom.missing ? [headroom.path] : [], invalid: headroom.invalid ? [headroom.path] : [], used: [headroom] }
    ],
    ctx,
    'spool.usable'
  );
  metrics.push(
    makeMetric(ctx, {
      id: 'spool.usable',
      formulaId: 'F-SPOOL-02',
      unit: 'bytes',
      status: usable.status,
      value: usable.status === 'COMPLETE' ? usable.value : null,
      missing: usable.missing,
      invalid: usable.invalid,
      reasons: usable.reasons,
      used: usable.used,
      limitations: usable.limitations ?? []
    })
  );
  const peakOccupancyMetric = metricById(metrics, 'spool.peakOccupancy');
  const usableOperand = derivedOperand(metricById(metrics, 'spool.usable'));
  let fitPeak;
  if (peakOccupancyMetric.status === 'INVALID' || usableOperand.status === 'INVALID') {
    fitPeak = { status: 'INVALID', value: null, missing: [], invalid: unique([...peakOccupancyMetric.invalid, ...(usableOperand.invalid ?? [])]), reasons: unique([...peakOccupancyMetric.reasons, ...(usableOperand.reasons ?? [])]), used: [...(peakOccupancyMetric.used ?? []), ...(usableOperand.used ?? [])] };
  } else if (peakOccupancyMetric.status !== 'COMPLETE' || usableOperand.status !== 'COMPLETE') {
    fitPeak = { status: 'INCOMPLETE', value: null, missing: unique([...peakOccupancyMetric.missing, ...(usableOperand.missing ?? [])]), invalid: [], reasons: [], used: [...(peakOccupancyMetric.used ?? []), ...(usableOperand.used ?? [])] };
  } else {
    const fit = peakOccupancyMetric.value <= usableOperand.value;
    fitPeak = { status: fit ? 'COMPLETE' : 'NOT_MET', value: fit, missing: [], invalid: [], reasons: fit ? [] : ['TARGET_NOT_MET'], used: [...(peakOccupancyMetric.used ?? []), ...(usableOperand.used ?? [])] };
  }
  metrics.push(makeMetric(ctx, { id: 'spool.fitPeak', formulaId: 'F-SPOOL-02', unit: 'boolean', ...fitPeak }));
  const freeAfter = subtractFloor(peakOccupancyMetric, usableOperand, false, ctx, 'spool.freeAfterPeak', 'bytes');
  metrics.push(makeMetric(ctx, { id: 'spool.freeAfterPeak', formulaId: 'F-SPOOL-02', unit: 'bytes', ...freeAfter }));
  const overflow = subtractFloor(peakOccupancyMetric, usableOperand, true, ctx, 'spool.overflowBytes', 'bytes');
  metrics.push(makeMetric(ctx, { id: 'spool.overflowBytes', formulaId: 'F-SPOOL-02', unit: 'bytes', ...overflow }));

  let net;
  if (recoveryTotal.invalid || continuing.invalid) {
    net = { status: 'INVALID', value: null, missing: [], invalid: collectInvalid([recoveryTotal, continuing]), reasons: invalidReasons([recoveryTotal, continuing]), used: [recoveryTotal, continuing] };
  } else if (recoveryTotal.missing || continuing.missing) {
    net = { status: 'INCOMPLETE', value: null, missing: collectMissing([recoveryTotal, continuing]), invalid: [], reasons: [], used: [recoveryTotal, continuing] };
  } else {
    net = { status: 'COMPLETE', value: recoveryTotal.value - continuing.value, missing: [], invalid: [], reasons: [], used: [recoveryTotal, continuing] };
  }
  metrics.push(
    makeMetric(ctx, {
      id: 'spool.recoveryNetPayloadBytesPerSecond',
      formulaId: 'F-SPOOL-03',
      unit: 'bytesPerSecond',
      ...net
    })
  );
  const backlogMetric = metricById(metrics, 'spool.recoveryBacklog');
  let drain;
  if (backlogMetric.status === 'INVALID' || net.status === 'INVALID') {
    drain = { status: 'INVALID', value: null, missing: [], invalid: unique([...backlogMetric.invalid, ...(net.invalid ?? [])]), reasons: unique([...backlogMetric.reasons, ...(net.reasons ?? [])]), used: [...(backlogMetric.used ?? []), ...(net.used ?? [])] };
  } else if (backlogMetric.status !== 'COMPLETE') {
    drain = { status: 'INCOMPLETE', value: null, missing: backlogMetric.missing, invalid: [], reasons: [], used: [...(backlogMetric.used ?? []), ...(net.used ?? [])] };
  } else if (backlogMetric.value === 0) {
    drain = {
      status: 'COMPLETE',
      value: 0,
      missing: net.status === 'COMPLETE' ? [] : net.missing ?? [],
      invalid: [],
      reasons: net.status === 'COMPLETE' && net.value === 0 ? ['IDLE_ZERO_DENOMINATOR'] : [],
      used: [...(backlogMetric.used ?? []), ...(net.used ?? [])]
    };
  } else if (net.status !== 'COMPLETE') {
    drain = { status: 'INCOMPLETE', value: null, missing: net.missing, invalid: [], reasons: [], used: [...(backlogMetric.used ?? []), ...(net.used ?? [])] };
  } else if (net.value <= 0) {
    drain = { status: 'NOT_MET', value: null, missing: [], invalid: [], reasons: ['DRAIN_IMPOSSIBLE'], used: [...(backlogMetric.used ?? []), ...(net.used ?? [])] };
  } else {
    drain = { status: 'COMPLETE', value: backlogMetric.value / net.value, missing: [], invalid: [], reasons: [], used: [...(backlogMetric.used ?? []), ...(net.used ?? [])] };
  }
  metrics.push(makeMetric(ctx, { id: 'spool.drainSeconds', formulaId: 'F-SPOOL-03', unit: 'seconds', ...drain }));

  return groupResult('spool', ['F-SPOOL-01', 'F-SPOOL-02', 'F-SPOOL-03'], null, metrics, [], extraInvalid);
}

function subtractFloor(leftMetric, rightOperand, overflow, ctx, metricPath, unit) {
  if (leftMetric.status === 'INVALID' || rightOperand.status === 'INVALID') {
    return {
      status: 'INVALID',
      value: null,
      missing: [],
      invalid: unique([...leftMetric.invalid, ...(rightOperand.invalid ?? [])]),
      reasons: unique([...leftMetric.reasons, ...(rightOperand.reasons ?? [])]),
      used: [...(leftMetric.used ?? []), ...(rightOperand.used ?? [])]
    };
  }
  if (leftMetric.status !== 'COMPLETE' || rightOperand.status !== 'COMPLETE') {
    return {
      status: 'INCOMPLETE',
      value: null,
      missing: unique([...leftMetric.missing, ...(rightOperand.missing ?? [])]),
      invalid: [],
      reasons: [],
      used: [...(leftMetric.used ?? []), ...(rightOperand.used ?? [])]
    };
  }
  const value = overflow
    ? Math.max(0, leftMetric.value - rightOperand.value)
    : Math.max(0, rightOperand.value - leftMetric.value);
  return {
    status: 'COMPLETE',
    value,
    missing: [],
    invalid: [],
    reasons: [],
    used: [...(leftMetric.used ?? []), ...(rightOperand.used ?? [])]
  };
}

function readAsOf(ctx, input) {
  const rec = readLeaf(ctx, input, 'asOf', 'asOf', { type: 'string', unit: 'iso8601' });
  if (rec.usable && !dateValid(rec.value)) {
    invalidate(rec, ctx, 'DATE_INVALID');
    rec.usable = false;
    rec.value = null;
  }
  return { rec, value: rec.usable ? rec.value : null };
}

function readPlainString(ctx, entry, key, fieldPath) {
  const rec = {
    path: fieldPath,
    present: false,
    missing: false,
    invalid: false,
    usable: false,
    value: null,
    reasons: []
  };
  if (!isPlainObject(entry) || !hasOwn(entry, key) || entry[key] === null || entry[key] === undefined) {
    rec.missing = true;
    return rec;
  }
  rec.present = true;
  if (typeof entry[key] !== 'string') {
    rec.invalid = true;
    rec.reasons.push('TYPE_INVALID');
    addError(ctx, 'TYPE_INVALID', fieldPath);
    return rec;
  }
  const trimmed = entry[key].trim();
  if (trimmed === '') {
    rec.missing = true;
    return rec;
  }
  rec.value = trimmed;
  rec.usable = true;
  return rec;
}

function readPlainBoolean(ctx, entry, key, fieldPath) {
  const rec = {
    path: fieldPath,
    present: false,
    missing: false,
    invalid: false,
    usable: false,
    value: null,
    reasons: []
  };
  if (!isPlainObject(entry) || !hasOwn(entry, key) || entry[key] === null || entry[key] === undefined) {
    rec.missing = true;
    return rec;
  }
  rec.present = true;
  if (typeof entry[key] !== 'boolean') {
    rec.invalid = true;
    rec.reasons.push('TYPE_INVALID');
    addError(ctx, 'TYPE_INVALID', fieldPath);
    return rec;
  }
  rec.value = entry[key];
  rec.usable = true;
  return rec;
}

function readDateLeaf(ctx, entry, key, fieldPath) {
  const rec = readLeaf(ctx, entry, key, fieldPath, { type: 'string', unit: 'iso8601' });
  if (rec.usable && !dateValid(rec.value)) {
    invalidate(rec, ctx, 'DATE_INVALID');
    rec.usable = false;
  }
  return rec;
}

function applyFutureDateCheck(rec, ctx, asOf) {
  if (asOf.value && rec.usable && Date.parse(rec.value) > Date.parse(asOf.value)) {
    invalidate(rec, ctx, 'DATE_IN_FUTURE');
    rec.usable = false;
  }
}

function computeBackupDependency(ctx, entry, index, basePath, asOf, duplicateId) {
  const entryPath = `${basePath}[${index}]`;
  const idFallback = `backupRecovery.item${index}`;
  if (!isPlainObject(entry)) {
    addError(ctx, 'TYPE_INVALID', entryPath);
    return {
      metric: makeMetric(ctx, {
        id: idFallback,
        formulaId: 'F-BACKUP-01',
        unit: null,
        status: 'INVALID',
        value: null,
        invalid: [entryPath],
        reasons: ['TYPE_INVALID'],
        used: [],
        limitations: ['EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY']
      }),
      usablyIncluded: false
    };
  }
  const unknownPaths = [];
  checkUnknownKeys(ctx, entry, BACKUP_DEPENDENCY_FIELDS, entryPath, unknownPaths);

  const depIdRec = readPlainString(ctx, entry, 'dependencyId', `${entryPath}.dependencyId`);
  if (depIdRec.usable && !BACKUP_DEPENDENCY_IDS.includes(depIdRec.value)) {
    invalidate(depIdRec, ctx, 'TYPE_INVALID');
  }
  if (duplicateId === true && depIdRec.usable && BACKUP_DEPENDENCY_IDS.includes(depIdRec.value)) {
    invalidate(depIdRec, ctx, 'DEPENDENCY_DUPLICATE');
  }
  const included = readPlainBoolean(ctx, entry, 'included', `${entryPath}.included`);
  const approvedRpo = readLeaf(ctx, entry, 'approvedRpoSeconds', `${entryPath}.approvedRpoSeconds`, {
    type: 'number',
    unit: 'seconds',
    kind: 'NonNegativeMeasure'
  });
  const approvedRto = readLeaf(ctx, entry, 'approvedRtoSeconds', `${entryPath}.approvedRtoSeconds`, {
    type: 'number',
    unit: 'seconds',
    kind: 'NonNegativeMeasure'
  });
  const restoreTestedAt = readDateLeaf(ctx, entry, 'restoreTestedAt', `${entryPath}.restoreTestedAt`);
  const observedRpo = readLeaf(ctx, entry, 'observedRpoSeconds', `${entryPath}.observedRpoSeconds`, {
    type: 'number',
    unit: 'seconds',
    kind: 'NonNegativeMeasure'
  });
  const observedRto = readLeaf(ctx, entry, 'observedRtoSeconds', `${entryPath}.observedRtoSeconds`, {
    type: 'number',
    unit: 'seconds',
    kind: 'NonNegativeMeasure'
  });
  const owner = readPlainString(ctx, entry, 'owner', `${entryPath}.owner`);
  const evidenceRef = readPlainString(ctx, entry, 'evidenceRef', `${entryPath}.evidenceRef`);

  applyFutureDateCheck(restoreTestedAt, ctx, asOf);

  const requiredRecs = [depIdRec, included, approvedRpo, approvedRto, restoreTestedAt, owner, evidenceRef];
  const observationRecs = [observedRpo, observedRto];
  const leafRecs = [approvedRpo, approvedRto, restoreTestedAt, observedRpo, observedRto];
  const excluded = included.usable && included.value === false;
  const asOfMissing = asOf.value === null && !asOf.rec.invalid;
  const asOfInvalid = asOf.rec.invalid === true;

  let invalidPaths = unique([
    ...collectInvalid([...requiredRecs, ...observationRecs]),
    ...unknownPaths,
    ...(asOfInvalid ? ['asOf'] : [])
  ]);
  const invalidCodes = unique([
    ...invalidReasons([...requiredRecs, ...observationRecs]),
    ...(unknownPaths.length > 0 ? ['UNKNOWN_KEY'] : []),
    ...(asOfInvalid ? asOf.rec.reasons : [])
  ]);
  let missingPaths = collectMissing(requiredRecs);
  if (excluded) {
    missingPaths = missingPaths.filter(
      (missingPath) =>
        missingPath === `${entryPath}.dependencyId` || missingPath === `${entryPath}.included`
    );
  }
  const observationMissing = collectMissing(observationRecs);

  let status;
  let value = null;
  let reasons = [];
  if (invalidPaths.length > 0) {
    status = 'INVALID';
    reasons = invalidCodes;
  } else if (excluded) {
    status = 'INCOMPLETE';
    reasons = ['DEPENDENCY_EXCLUDED'];
  } else if (missingPaths.length > 0) {
    status = 'INCOMPLETE';
    reasons = ['DEPENDENCY_MISSING'];
  } else if (asOfMissing) {
    status = 'INCOMPLETE';
    reasons = ['ASOF_REQUIRED'];
  } else if (observationMissing.length > 0) {
    status = 'INCOMPLETE';
    reasons = ['OBSERVATION_MISSING'];
  } else if (observedRpo.value > approvedRpo.value || observedRto.value > approvedRto.value) {
    status = 'NOT_MET';
    value = 'NOT_MET';
    reasons = ['TARGET_NOT_MET'];
  } else {
    status = 'COMPLETE';
    value = 'MET';
  }

  const metricMissing = unique([
    ...missingPaths,
    ...(asOfMissing && !excluded ? ['asOf'] : []),
    ...(!excluded ? observationMissing : [])
  ]);
  const limitations = [
    'EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY',
    ...(asOfMissing ? ['ASOF_ABSENT_NO_FUTURE_CHECK'] : [])
  ];
  const metricId =
    depIdRec.usable &&
    BACKUP_DEPENDENCY_IDS.includes(depIdRec.value) &&
    duplicateId !== true
      ? `backupRecovery.${depIdRec.value}`
      : idFallback;
  // F-BACKUP-02 owns the observed-vs-approved target comparison verdict
  // (MET/NOT_MET); every typed-completeness/excluded/missing/invalid verdict
  // stays attributed to F-BACKUP-01.
  const formulaId =
    (status === 'COMPLETE' && value === 'MET') || status === 'NOT_MET'
      ? 'F-BACKUP-02'
      : 'F-BACKUP-01';
  const usablyIncluded =
    !excluded &&
    included.usable &&
    included.value === true &&
    missingPaths.length === 0 &&
    invalidPaths.length === 0;

  return {
    metric: makeMetric(ctx, {
      id: metricId,
      formulaId,
      unit: null,
      status,
      value,
      missing: metricMissing,
      invalid: invalidPaths,
      reasons,
      used: leafRecs,
      limitations
    }),
    usablyIncluded
  };
}

function computeBackupRecovery(ctx, input, asOf) {
  const read = readGroup(ctx, input, 'backupRecovery');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  checkUnknownKeys(ctx, group, new Set(['BACKUP_DEPENDENCY']), 'backupRecovery', extraInvalid);

  const collectionPath = 'backupRecovery.BACKUP_DEPENDENCY';
  const raw = group.BACKUP_DEPENDENCY;
  const collectionMissing = [];
  const collectionInvalid = [];
  let entries = [];
  if (Array.isArray(raw)) {
    entries = raw;
  } else if (raw === undefined || raw === null) {
    collectionMissing.push(collectionPath);
  } else {
    addError(ctx, 'TYPE_INVALID', collectionPath);
    collectionInvalid.push(collectionPath);
  }

  const metrics = [];
  const states = [];
  const idCounts = new Map();
  for (const entry of entries) {
    if (!isPlainObject(entry) || typeof entry.dependencyId !== 'string') continue;
    const dependencyId = entry.dependencyId.trim();
    if (!BACKUP_DEPENDENCY_IDS.includes(dependencyId)) continue;
    idCounts.set(dependencyId, (idCounts.get(dependencyId) ?? 0) + 1);
  }
  const presentIds = new Set();
  entries.forEach((entry, index) => {
    let duplicateId = false;
    if (isPlainObject(entry) && typeof entry.dependencyId === 'string') {
      const dependencyId = entry.dependencyId.trim();
      if (BACKUP_DEPENDENCY_IDS.includes(dependencyId)) {
        presentIds.add(dependencyId);
        duplicateId = (idCounts.get(dependencyId) ?? 0) > 1;
      }
    }
    const result = computeBackupDependency(ctx, entry, index, collectionPath, asOf, duplicateId);
    metrics.push(result.metric);
    states.push({ usablyIncluded: result.usablyIncluded });
  });

  const entriesEmpty = entries.length === 0;
  const copiesOnly = entriesEmpty || states.every((state) => !state.usablyIncluded);
  const asOfMissing = asOf.value === null && !asOf.rec.invalid;
  if (entriesEmpty) {
    metrics.push(
      makeMetric(ctx, {
        id: 'backupRecovery.dependencies',
        formulaId: 'F-BACKUP-01',
        unit: null,
        status: collectionInvalid.length > 0 ? 'INVALID' : 'INCOMPLETE',
        value: null,
        missing: collectionMissing.length > 0 ? collectionMissing : [collectionPath],
        invalid: collectionInvalid,
        reasons: collectionInvalid.length > 0 ? ['TYPE_INVALID'] : [],
        used: [],
        limitations: ['EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY']
      })
    );
  } else {
    // Exact-set closure: each fixed dependencyId must be present exactly once.
    // Absent IDs stay individually visible as INCOMPLETE + DEPENDENCY_ABSENT and
    // prevent the aggregate from ever reaching BACKUP_CLOSURE_COMPLETE.
    for (const dependencyId of BACKUP_DEPENDENCY_IDS) {
      if (presentIds.has(dependencyId)) continue;
      metrics.push(
        makeMetric(ctx, {
          id: `backupRecovery.${dependencyId}`,
          formulaId: 'F-BACKUP-01',
          unit: null,
          status: 'INCOMPLETE',
          value: null,
          missing: [`${collectionPath}.${dependencyId}`],
          reasons: ['DEPENDENCY_ABSENT'],
          used: [],
          limitations: [
            'EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY',
            ...(asOfMissing ? ['ASOF_ABSENT_NO_FUTURE_CHECK'] : [])
          ]
        })
      );
    }
  }

  let closureStatus;
  if (collectionInvalid.length > 0 || metrics.some((metric) => metric.status === 'INVALID')) {
    closureStatus = 'INVALID';
  } else if (collectionMissing.length > 0 || entriesEmpty || copiesOnly) {
    closureStatus = 'INCOMPLETE';
  } else {
    closureStatus = worst(metrics.map((metric) => metric.status));
  }
  let closureValue = null;
  let closureReasons = [];
  if (closureStatus === 'INVALID') {
    closureValue = null;
  } else if (copiesOnly) {
    closureValue = 'COPIES_ONLY';
    closureReasons = ['COPIES_ONLY_INCOMPLETE'];
  } else if (closureStatus === 'NOT_MET') {
    closureValue = 'NOT_MET';
    closureReasons = ['TARGET_NOT_MET'];
  } else if (closureStatus === 'COMPLETE') {
    closureValue = 'BACKUP_CLOSURE_COMPLETE';
  } else {
    closureValue = 'DEPENDENCY_CLOSURE_INCOMPLETE';
    if (!entriesEmpty && presentIds.size < BACKUP_DEPENDENCY_IDS.length) {
      closureReasons = ['DEPENDENCY_ABSENT'];
    }
  }
  const closureMissing = unique([...collectionMissing, ...metrics.flatMap((metric) => metric.missing)]);
  const closureInvalid = unique([...collectionInvalid, ...metrics.flatMap((metric) => metric.invalid)]);
  const closureLimitations = [
    'EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY',
    ...(asOfMissing && !entriesEmpty ? ['ASOF_ABSENT_NO_FUTURE_CHECK'] : [])
  ];
  metrics.push(
    makeMetric(ctx, {
      id: 'backupRecovery.closure',
      formulaId: 'F-BACKUP-01',
      unit: null,
      status: closureStatus,
      value: closureValue,
      missing: closureMissing,
      invalid: closureInvalid,
      reasons: closureReasons,
      used: mergeRecs(metrics),
      limitations: closureLimitations
    })
  );

  return groupResult(
    'backupRecovery',
    ['F-BACKUP-01', 'F-BACKUP-02'],
    null,
    metrics,
    [],
    extraInvalid,
    ['EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY']
  );
}

function computeCapabilityItem(ctx, entry, index, basePath, asOf, duplicateId) {
  const entryPath = `${basePath}[${index}]`;
  const idFallback = `capabilityEvidence.item${index}`;
  if (!isPlainObject(entry)) {
    addError(ctx, 'TYPE_INVALID', entryPath);
    return makeMetric(ctx, {
      id: idFallback,
      formulaId: 'F-CAP-01',
      unit: null,
      status: 'INVALID',
      value: null,
      invalid: [entryPath],
      reasons: ['TYPE_INVALID'],
      used: [],
      limitations: [...CAPABILITY_LIMITATIONS]
    });
  }
  const unknownPaths = [];
  checkUnknownKeys(ctx, entry, CAPABILITY_FIELDS, entryPath, unknownPaths);

  const idRec = readPlainString(ctx, entry, 'capabilityId', `${entryPath}.capabilityId`);
  if (idRec.usable && !CAPABILITY_IDS.includes(idRec.value)) {
    invalidate(idRec, ctx, 'TYPE_INVALID');
  }
  if (duplicateId === true && idRec.usable && CAPABILITY_IDS.includes(idRec.value)) {
    invalidate(idRec, ctx, 'CAPABILITY_DUPLICATE');
  }
  const statusRec = readPlainString(ctx, entry, 'status', `${entryPath}.status`);
  let claimed = false;
  let unverified = false;
  if (statusRec.usable) {
    if (statusRec.value === 'CLAIMED_VERIFIED') claimed = true;
    else if (statusRec.value === 'UNVERIFIED') unverified = true;
    else invalidate(statusRec, ctx, 'TYPE_INVALID');
  }
  const claimedBy = readPlainString(ctx, entry, 'claimedBy', `${entryPath}.claimedBy`);
  const owner = readPlainString(ctx, entry, 'owner', `${entryPath}.owner`);
  const source = readPlainString(ctx, entry, 'source', `${entryPath}.source`);
  const evidenceRef = readPlainString(ctx, entry, 'evidenceRef', `${entryPath}.evidenceRef`);
  const verifiedAt = readDateLeaf(ctx, entry, 'verifiedAt', `${entryPath}.verifiedAt`);
  applyFutureDateCheck(verifiedAt, ctx, asOf);

  const evidenceRecs = [claimedBy, owner, source, evidenceRef, verifiedAt];
  const evidenceMissing = collectMissing(evidenceRecs);
  const asOfMissing = asOf.value === null && !asOf.rec.invalid;
  const asOfInvalid = asOf.rec.invalid === true;

  let invalidPaths = unique([
    ...collectInvalid([idRec, statusRec, ...evidenceRecs]),
    ...unknownPaths,
    ...(asOfInvalid ? ['asOf'] : [])
  ]);
  let invalidCodes = unique([
    ...invalidReasons([idRec, statusRec, ...evidenceRecs]),
    ...(unknownPaths.length > 0 ? ['UNKNOWN_KEY'] : []),
    ...(asOfInvalid ? asOf.rec.reasons : [])
  ]);

  let status;
  let value = null;
  let reasons = [];
  let missingPaths = [];
  if (invalidPaths.length > 0) {
    status = 'INVALID';
    reasons = invalidCodes;
  } else if (!idRec.usable || !statusRec.usable) {
    status = 'INCOMPLETE';
    missingPaths = collectMissing([idRec, statusRec]);
  } else if (claimed && evidenceMissing.length > 0) {
    status = 'INVALID';
    reasons = ['EVIDENCE_REQUIRED'];
    invalidPaths = unique([...invalidPaths, ...evidenceMissing]);
    addError(ctx, 'EVIDENCE_REQUIRED', entryPath);
  } else if (unverified) {
    status = 'NOT_VERIFIED';
    value = 'UNVERIFIED';
  } else if (claimed) {
    status = 'NOT_VERIFIED';
    value = 'CLAIMED_VERIFIED';
  } else {
    status = 'INVALID';
    reasons = ['TYPE_INVALID'];
  }

  const limitations = [
    ...CAPABILITY_LIMITATIONS,
    ...(asOfMissing ? ['ASOF_ABSENT_NO_FUTURE_CHECK'] : [])
  ];
  const metricId =
    idRec.usable && CAPABILITY_IDS.includes(idRec.value) && duplicateId !== true
      ? `capabilityEvidence.${idRec.value}`
      : idFallback;
  return makeMetric(ctx, {
    id: metricId,
    formulaId: 'F-CAP-01',
    unit: null,
    status,
    value,
    missing: missingPaths,
    invalid: invalidPaths,
    reasons,
    used: [verifiedAt],
    limitations
  });
}

function computeCapabilityEvidence(ctx, input, asOf) {
  const read = readGroup(ctx, input, 'capabilityEvidence');
  const group = read.value;
  const extraInvalid = read.invalidPath ? [read.invalidPath] : [];
  checkUnknownKeys(ctx, group, new Set(['CAPABILITY']), 'capabilityEvidence', extraInvalid);

  const collectionPath = 'capabilityEvidence.CAPABILITY';
  const raw = group.CAPABILITY;
  const collectionMissing = [];
  const collectionInvalid = [];
  let entries = [];
  if (Array.isArray(raw)) {
    entries = raw;
  } else if (raw === undefined || raw === null) {
    collectionMissing.push(collectionPath);
  } else {
    addError(ctx, 'TYPE_INVALID', collectionPath);
    collectionInvalid.push(collectionPath);
  }

  const idCounts = new Map();
  for (const entry of entries) {
    if (!isPlainObject(entry) || typeof entry.capabilityId !== 'string') continue;
    const capabilityId = entry.capabilityId.trim();
    if (!CAPABILITY_IDS.includes(capabilityId)) continue;
    idCounts.set(capabilityId, (idCounts.get(capabilityId) ?? 0) + 1);
  }
  const presentIds = new Set();
  const metrics = entries.map((entry, index) => {
    let duplicateId = false;
    if (isPlainObject(entry) && typeof entry.capabilityId === 'string') {
      const capabilityId = entry.capabilityId.trim();
      if (CAPABILITY_IDS.includes(capabilityId)) {
        presentIds.add(capabilityId);
        duplicateId = (idCounts.get(capabilityId) ?? 0) > 1;
      }
    }
    return computeCapabilityItem(ctx, entry, index, collectionPath, asOf, duplicateId);
  });
  const entriesEmpty = entries.length === 0;
  if (entriesEmpty) {
    const summaryMissing = collectionMissing.length > 0 ? collectionMissing : [collectionPath];
    metrics.push(
      makeMetric(ctx, {
        id: 'capabilityEvidence.summary',
        formulaId: 'F-CAP-01',
        unit: null,
        status: collectionInvalid.length > 0 ? 'INVALID' : 'INCOMPLETE',
        value: null,
        missing: summaryMissing,
        invalid: collectionInvalid,
        reasons: collectionInvalid.length > 0 ? ['TYPE_INVALID'] : [],
        used: [],
        limitations: [...CAPABILITY_LIMITATIONS]
      })
    );
  } else {
    // Exact-set checklist: every fixed capabilityId must be present exactly once.
    // Omitted capabilities stay individually visible as INCOMPLETE +
    // CAPABILITY_ABSENT instead of disappearing from the verdict.
    for (const capabilityId of CAPABILITY_IDS) {
      if (presentIds.has(capabilityId)) continue;
      metrics.push(
        makeMetric(ctx, {
          id: `capabilityEvidence.${capabilityId}`,
          formulaId: 'F-CAP-01',
          unit: null,
          status: 'INCOMPLETE',
          value: null,
          missing: [`${collectionPath}.${capabilityId}`],
          reasons: ['CAPABILITY_ABSENT'],
          used: [],
          limitations: [...CAPABILITY_LIMITATIONS]
        })
      );
    }
  }

  return groupResult(
    'capabilityEvidence',
    ['F-CAP-01'],
    null,
    metrics,
    [],
    extraInvalid,
    [...CAPABILITY_LIMITATIONS]
  );
}

export function compute(input = {}) {
  if (!isPlainObject(input)) input = {};
  const ctx = { errors: [], errorKeys: new Set(), input };
  for (const key of CALLER_CONTROL_KEYS) {
    if (hasOwn(input, key)) addError(ctx, 'CALLER_CONTROL_FORBIDDEN', key);
  }
  if (isPlainObject(input)) {
    const allowedTop = new Set(['meta', 'asOf', ...GROUP_IDS]);
    for (const key of Object.keys(input)) {
      if (!allowedTop.has(key) && !CALLER_CONTROL_KEYS.includes(key)) {
        addError(ctx, 'UNKNOWN_KEY', key);
      }
    }
  }
  const meta = readMeta(ctx, input);
  const synthetic = meta.status === 'SYNTHETIC_TEST_ONLY' || scanSynthetic(input);
  const asOf = readAsOf(ctx, input);

  // Compute in dependency order, then expose the canonical nine-group order.
  const ivrLines = computeIvrLines(ctx, input);
  const apiControlPlane = computeApiControlPlane(ctx, input);
  const playback = computePlayback(ctx, input);
  const spool = computeSpool(ctx, input);
  const mediaIngestStorage = computeMediaIngestStorage(ctx, input, spool.metrics);
  const growthAndAsync = computeGrowthAndAsync(ctx, input);
  const measuredNodes = computeMeasuredNodes(ctx, input);
  const backupRecovery = computeBackupRecovery(ctx, input, asOf);
  const capabilityEvidence = computeCapabilityEvidence(ctx, input, asOf);
  const byId = {
    ivrLines,
    apiControlPlane,
    measuredNodes,
    playback,
    mediaIngestStorage,
    growthAndAsync,
    spool,
    backupRecovery,
    capabilityEvidence
  };
  const groups = GROUP_IDS.map((id) => byId[id]);
  for (const group of groups) {
    const groupErrors = ctx.errors.filter(
      (error) => error.path === group.id || error.path.startsWith(`${group.id}.`)
    );
    if (groupErrors.length > 0) {
      group.status = 'INVALID';
      group.invalid = unique([...group.invalid, ...groupErrors.map((error) => error.path)]);
    }
  }

  const rootStatus = ctx.errors.length > 0 ? 'INVALID' : worst(groups.map((group) => group.status));
  const flags = unique([
    ...groups.flatMap((group) => group.limitations),
    'NO_CLOUD_VERIFICATION',
    'A3_RISK_NOT_CLOSED'
  ]);
  return {
    schemaVersion: SCHEMA_VERSION,
    status: rootStatus,
    productionReady: false,
    procurementReady: false,
    cloudVerified: false,
    a3RiskClosed: false,
    synthetic,
    metaStatus: meta.status,
    asOf: asOf.value,
    groups,
    flags,
    errors: ctx.errors
  };
}

function readJsonInput(filePath) {
  const text = fs.readFileSync(filePath, 'utf8');
  return JSON.parse(text);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const args = process.argv.slice(2);
  let inputPath = null;
  for (let index = 0; index < args.length; index += 1) {
    if (args[index] === '--input') {
      inputPath = args[index + 1];
      index += 1;
    } else {
      process.stderr.write(`unknown argument: ${args[index]}\n`);
      process.exit(1);
    }
  }
  if (!inputPath) {
    process.stderr.write('usage: node deployment/capacity/calc.mjs --input <input.json>\n');
    process.exit(1);
  }
  try {
    const input = readJsonInput(inputPath);
    process.stdout.write(`${JSON.stringify(compute(input), null, 2)}\n`);
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exit(1);
  }
}

export const S1_GROUP_IDS = S1_GROUP_ID_LIST;
export const S2_GROUP_IDS = S2_GROUP_ID_LIST;
export const S1_UNITS = UNITS;
