#!/usr/bin/env node
/**
 * A4 S1 capacity template/fixture validator.
 *
 * Strict JSON only. `expected` blocks are consumed by this validator and are
 * never passed to compute(). The programmatic non-finite probes inject real JS
 * NaN / +Infinity / -Infinity, which strict JSON cannot represent.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { compute, S1_GROUP_IDS, S1_UNITS } from './calc.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const DEFAULT_TEMPLATE = path.join(HERE, 'inputs.template.json');
const DEFAULT_FIXTURES = path.join(HERE, 'fixtures');
const NON_NUMERIC_UNITS = new Set(['enum', 'iso8601', 'boolean']);

function loadJson(filePath) {
  const buffer = fs.readFileSync(filePath);
  const text = buffer.toString('utf8');
  if (text.charCodeAt(0) === 0xfeff) {
    throw new Error(`BOM is not allowed in strict JSON: ${filePath}`);
  }
  return JSON.parse(text);
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function isLeaf(value) {
  return (
    isPlainObject(value) &&
    Object.prototype.hasOwnProperty.call(value, 'value') &&
    Object.prototype.hasOwnProperty.call(value, 'unit') &&
    Object.prototype.hasOwnProperty.call(value, 'provenance')
  );
}

function walkLeaves(value, visitor, currentPath = '', seen = new Set()) {
  if (value === null || typeof value !== 'object') return;
  if (seen.has(value)) return;
  seen.add(value);
  if (isLeaf(value)) {
    visitor(value, currentPath);
  }
  if (Array.isArray(value)) {
    value.forEach((entry, index) => walkLeaves(entry, visitor, `${currentPath}[${index}]`, seen));
    return;
  }
  for (const [key, entry] of Object.entries(value)) {
    walkLeaves(entry, visitor, currentPath ? `${currentPath}.${key}` : key, seen);
  }
}

function tokenizePath(inputPath) {
  return inputPath
    .replace(/\[(\d+)\]/g, '.$1')
    .split('.')
    .filter((token) => token !== '');
}

function getByPath(root, inputPath) {
  return tokenizePath(inputPath).reduce((value, token) => (value == null ? undefined : value[token]), root);
}

function setByPath(root, inputPath, setter) {
  const tokens = tokenizePath(inputPath);
  let cursor = root;
  for (let index = 0; index < tokens.length - 1; index += 1) {
    cursor = cursor[tokens[index]];
  }
  setter(cursor, tokens[tokens.length - 1]);
}

function flattenMetrics(output) {
  const map = new Map();
  for (const group of output.groups ?? []) {
    for (const metric of group.metrics ?? []) {
      if (!map.has(metric.id)) map.set(metric.id, []);
      map.get(metric.id).push(metric);
    }
  }
  return map;
}

function sameValue(actual, expected) {
  if (typeof expected === 'number' && typeof actual === 'number') {
    return Math.abs(actual - expected) <= Math.max(1, Math.abs(expected)) * 1e-9;
  }
  return actual === expected;
}

function requires(value, expected) {
  if (!Array.isArray(value)) return false;
  return expected.every((entry) => value.includes(entry));
}

function checkIncludes(value, expected, label, issues) {
  if (!Array.isArray(value)) {
    issues.push(`${label}: value is not an array`);
    return;
  }
  for (const entry of expected ?? []) {
    if (!value.includes(entry)) issues.push(`${label}: missing ${entry}`);
  }
}

function checkMetric(metric, expected, issues) {
  if (!metric) return;
  if (expected.status !== undefined && metric.status !== expected.status) {
    issues.push(`${metric.id}: status ${metric.status} != ${expected.status}`);
  }
  if (Object.prototype.hasOwnProperty.call(expected, 'value')) {
    if (!sameValue(metric.value, expected.value)) {
      issues.push(`${metric.id}: value ${JSON.stringify(metric.value)} != ${JSON.stringify(expected.value)}`);
    }
  }
  if (expected.unit !== undefined && metric.unit !== expected.unit) {
    issues.push(`${metric.id}: unit ${metric.unit} != ${expected.unit}`);
  }
  if (expected.approvalStatus !== undefined && metric.approvalStatus !== expected.approvalStatus) {
    issues.push(`${metric.id}: approvalStatus ${metric.approvalStatus} != ${expected.approvalStatus}`);
  }
  checkIncludes(metric.reasons, expected.reasonsInclude, `${metric.id}.reasons`, issues);
  checkIncludes(metric.missing, expected.missingInclude, `${metric.id}.missing`, issues);
  checkIncludes(metric.invalid, expected.invalidInclude, `${metric.id}.invalid`, issues);
  checkIncludes(metric.limitations, expected.limitationsInclude, `${metric.id}.limitations`, issues);
  if (expected.valueNull === true && metric.value !== null) {
    issues.push(`${metric.id}: value must be null`);
  }
}

function checkExpected(output, expected, issues) {
  const metrics = flattenMetrics(output);
  const root = expected.root ?? {};
  if (root.status !== undefined && output.status !== root.status) {
    issues.push(`root.status ${output.status} != ${root.status}`);
  }
  if (root.metaStatus !== undefined && output.metaStatus !== root.metaStatus) {
    issues.push(`root.metaStatus ${output.metaStatus} != ${root.metaStatus}`);
  }
  if (root.synthetic !== undefined && output.synthetic !== root.synthetic) {
    issues.push(`root.synthetic ${output.synthetic} != ${root.synthetic}`);
  }
  for (const key of ['productionReady', 'procurementReady', 'cloudVerified', 'a3RiskClosed']) {
    if (output[key] !== false) issues.push(`root.${key} must be false`);
    if (Object.prototype.hasOwnProperty.call(root, key) && root[key] !== false) {
      issues.push(`expected root.${key} must be false in S1`);
    }
  }
  for (const [id, metricExpected] of Object.entries(expected.metrics ?? {})) {
    const candidates = metrics.get(id);
    if (!candidates || candidates.length === 0) {
      issues.push(`metric ${id}: missing from output`);
      continue;
    }
    checkMetric(candidates[0], metricExpected, issues);
  }
  for (const expectation of expected.errorsInclude ?? []) {
    const found = (output.errors ?? []).some(
      (error) =>
        error.code === expectation.code &&
        (expectation.path === undefined || error.path === expectation.path)
    );
    if (!found) {
      issues.push(
        `error missing: ${expectation.code}${expectation.path === undefined ? '' : ` @ ${expectation.path}`}`
      );
    }
  }
  for (const code of expected.errorsAbsent ?? []) {
    if ((output.errors ?? []).some((error) => error.code === code)) {
      issues.push(`error must be absent: ${code}`);
    }
  }
  if (expected.errorsEmpty === true && (output.errors ?? []).length > 0) {
    issues.push(`errors must be empty: ${output.errors.map((error) => error.code).join(',')}`);
  }
  if (expected.noInvalidMetrics === true) {
    for (const group of output.groups ?? []) {
      for (const metric of group.metrics ?? []) {
        if (metric.status === 'INVALID') issues.push(`metric ${metric.id}: must not be INVALID`);
      }
    }
  }
  checkIncludes(output.flags, expected.flagsInclude, 'root.flags', issues);
  for (const [groupId, groupExpected] of Object.entries(expected.groups ?? {})) {
    const group = (output.groups ?? []).find((entry) => entry.id === groupId);
    if (!group) {
      issues.push(`group ${groupId}: missing`);
      continue;
    }
    if (groupExpected.status !== undefined && group.status !== groupExpected.status) {
      issues.push(`group ${groupId}: status ${group.status} != ${groupExpected.status}`);
    }
    if (groupExpected.partial !== undefined && group.partial !== groupExpected.partial) {
      issues.push(`group ${groupId}: partial ${group.partial} != ${groupExpected.partial}`);
    }
  }
  for (const forbidden of expected.forbiddenOutputStrings ?? []) {
    const haystack = JSON.stringify(output).toLowerCase();
    if (haystack.includes(forbidden.toLowerCase())) {
      issues.push(`output must not contain ${forbidden}`);
    }
  }
}

function checkTemplateStructure(template, issues, counters) {
  const topKeys = Object.keys(template).filter((key) => key !== 'meta' && key !== 'asOf');
  if (topKeys.length !== S1_GROUP_IDS.length) {
    issues.push(`template: expected ${S1_GROUP_IDS.length} groups, found ${topKeys.length}`);
  }
  for (const id of S1_GROUP_IDS) {
    if (!Object.prototype.hasOwnProperty.call(template, id)) issues.push(`template: missing group ${id}`);
  }
  for (const key of topKeys) {
    if (!S1_GROUP_IDS.includes(key)) issues.push(`template: unexpected group/key ${key}`);
  }
  let leaves = 0;
  walkLeaves(template, (leaf, leafPath) => {
    leaves += 1;
    if (leaf.value !== null) issues.push(`template ${leafPath}: value must be null`);
    if (!S1_UNITS.has(leaf.unit)) issues.push(`template ${leafPath}: unknown unit ${leaf.unit}`);
    if (leaf.provenance !== 'MISSING') issues.push(`template ${leafPath}: provenance must be MISSING`);
    if (leaf.source !== null) issues.push(`template ${leafPath}: source must be null`);
    if (leaf.owner !== null) issues.push(`template ${leafPath}: owner must be null`);
    if (leaf.approval !== 'MISSING') issues.push(`template ${leafPath}: approval must be MISSING`);
  });
  counters.templateLeaves = leaves;
  if (leaves === 0) issues.push('template: no typed leaves found');
  const roles = template.measuredNodes && template.measuredNodes.MEASURED_ROLE;
  if (!Array.isArray(roles) || roles.length !== 0) {
    issues.push('template measuredNodes.MEASURED_ROLE: must be an empty array');
  }
  const derived = template.mediaIngestStorage && template.mediaIngestStorage.REC_DERIVED_BY_TYPE;
  if (derived) {
    for (const type of ['preview', 'transcript', 'summary', 'export']) {
      if (!derived[type] || derived[type].state !== null) {
        issues.push(`template REC_DERIVED_BY_TYPE.${type}.state: must be null`);
      }
    }
  }
}

function checkTemplateComputation(template, issues, counters) {
  const output = compute(template);
  if (output.groups.length !== 7) issues.push(`computed template: expected 7 groups, found ${output.groups.length}`);
  output.groups.forEach((group, index) => {
    if (group.id !== S1_GROUP_IDS[index]) {
      issues.push(`computed template: group order ${group.id} != ${S1_GROUP_IDS[index]}`);
    }
    if (group.status !== 'INCOMPLETE') {
      issues.push(`computed template group ${group.id}: status ${group.status} != INCOMPLETE`);
    }
    const nullMetrics = group.metrics.filter((metric) => metric.value === null).length;
    const zeroMetrics = group.metrics.filter((metric) => metric.value === 0).length;
    counters.templateNullMetrics += nullMetrics;
    counters.templateZeroMetrics += zeroMetrics;
    if (nullMetrics === 0) issues.push(`computed template group ${group.id}: no null metric (silent zero?)`);
    if (zeroMetrics > 0) issues.push(`computed template group ${group.id}: missing inputs produced ${zeroMetrics} zero value(s)`);
    for (const metric of group.metrics) {
      if (metric.status === 'INVALID') issues.push(`computed template: metric ${metric.id} is INVALID`);
    }
  });
  if (output.status !== 'INCOMPLETE') issues.push(`computed template: root status ${output.status} != INCOMPLETE`);
  if (output.productionReady !== false) issues.push('computed template: productionReady must be false');
  if (output.procurementReady !== false) issues.push('computed template: procurementReady must be false');
  if (output.cloudVerified !== false) issues.push('computed template: cloudVerified must be false');
  if (output.a3RiskClosed !== false) issues.push('computed template: a3RiskClosed must be false');
  if (output.synthetic !== false) issues.push('computed template: synthetic must be false');
  if (output.metaStatus !== 'UNAPPROVED_TEMPLATE') {
    issues.push(`computed template: metaStatus ${output.metaStatus} != UNAPPROVED_TEMPLATE`);
  }
  if ((output.errors ?? []).length !== 0) {
    issues.push(`computed template: expected zero errors, found ${output.errors.map((error) => error.code).join(',')}`);
  }
  counters.templateRootStatus = output.status;
  counters.templateGroupStatuses = output.groups.map((group) => `${group.id}:${group.status}`).join(',');
}

function probeLeaf(value, unit) {
  return {
    value,
    unit,
    provenance: 'SYNTHETIC_TEST_ONLY',
    source: 'nonfinite-probe',
    owner: 'nonfinite-probe',
    approval: 'SYNTHETIC_TEST_ONLY'
  };
}

function probeCommon(role, demandUnit) {
  return {
    role,
    demandUnit,
    headroomRatio: probeLeaf(0.1, 'ratio'),
    measuredCpuUtilization: probeLeaf(0.2, 'ratio'),
    measuredRamUtilization: probeLeaf(0.2, 'ratio'),
    measuredConnectionsUtilization: probeLeaf(0.2, 'ratio'),
    measuredIopsUtilization: probeLeaf(0.2, 'ratio'),
    resourceEvidenceRef: 'nonfinite-probe',
    measuredAt: probeLeaf('2026-10-20T00:00:00Z', 'iso8601')
  };
}

function probeStateless(role, demandUnit) {
  return {
    ...probeCommon(role, demandUnit),
    demandQuantity: probeLeaf(10, demandUnit),
    perNodeRate: probeLeaf(20, demandUnit),
    deployedNodes: probeLeaf(2, 'nodes')
  };
}

function probeRoles() {
  return [
    probeStateless('core-api', 'requestsPerSecond'),
    probeStateless('async-worker', 'tasksPerSecond'),
    probeStateless('media-ai-worker', 'tasksPerSecond'),
    {
      ...probeCommon('postgres', 'transactionsPerSecond'),
      demandQuantity: probeLeaf(10, 'transactionsPerSecond'),
      measuredNormalTopologyCapacity: probeLeaf(20, 'transactionsPerSecond'),
      measuredDegradedTopologyCapacity: probeLeaf(15, 'transactionsPerSecond')
    },
    {
      ...probeCommon('redis', 'opsPerSecond'),
      demandQuantity: probeLeaf(10, 'opsPerSecond'),
      measuredNormalTopologyCapacity: probeLeaf(20, 'opsPerSecond'),
      measuredDegradedTopologyCapacity: probeLeaf(15, 'opsPerSecond')
    },
    {
      ...probeCommon('rabbitmq', 'messagesPerSecond'),
      demandQuantity: probeLeaf(10, 'messagesPerSecond'),
      measuredNormalTopologyCapacity: probeLeaf(20, 'messagesPerSecond'),
      measuredDegradedTopologyCapacity: probeLeaf(15, 'messagesPerSecond')
    },
    {
      ...probeCommon('media-gateway', 'sessions'),
      sharedCoreApiEdgeCapacity: probeLeaf(100, 'sessions')
    }
  ];
}

function runNonFiniteProbes(template, issues, counters) {
  const base = structuredClone(template);
  base.meta = {
    status: 'SYNTHETIC_TEST_ONLY',
    source: 'nonfinite-probe',
    approvedBy: null,
    approvedAt: null
  };
  base.playback.PB_CONCURRENT_SESSIONS = probeLeaf(10, 'count');
  base.measuredNodes.MEASURED_ROLE = probeRoles();
  const paths = [];
  walkLeaves(base, (leaf, leafPath) => {
    if (!NON_NUMERIC_UNITS.has(leaf.unit)) paths.push(leafPath);
  });
  const badValues = [
    ['NaN', Number.NaN],
    ['+Infinity', Number.POSITIVE_INFINITY],
    ['-Infinity', Number.NEGATIVE_INFINITY]
  ];
  for (const leafPath of paths) {
    for (const [label, badValue] of badValues) {
      const input = structuredClone(base);
      setByPath(input, leafPath, (parent, key) => {
        parent[key].value = badValue;
        parent[key].provenance = 'SYNTHETIC_TEST_ONLY';
        parent[key].approval = 'SYNTHETIC_TEST_ONLY';
        parent[key].source = 'nonfinite-probe';
        parent[key].owner = 'nonfinite-probe';
      });
      const output = compute(input);
      counters.probes += 1;
      const found = (output.errors ?? []).some(
        (error) => error.code === 'NUMBER_NONFINITE' && error.path === leafPath
      );
      if (!found) issues.push(`nonfinite probe ${label} @ ${leafPath}: NUMBER_NONFINITE not reported`);
      if (output.status !== 'INVALID') {
        issues.push(`nonfinite probe ${label} @ ${leafPath}: root status ${output.status} != INVALID`);
      }
    }
  }
  counters.probeLeaves = paths.length;
}

function runFixtureFile(filePath, issues, counters) {
  let fixture;
  try {
    fixture = loadJson(filePath);
  } catch (error) {
    issues.push(`${path.basename(filePath)}: ${error.message}`);
    counters.failed += 1;
    return;
  }
  const label = path.basename(filePath);
  if (fixture.stage !== 's1') {
    issues.push(`${label}: stage ${fixture.stage} != s1`);
    counters.failed += 1;
    return;
  }
  if (!Array.isArray(fixture.cases) || fixture.cases.length === 0) {
    issues.push(`${label}: no cases`);
    counters.failed += 1;
    return;
  }
  for (const testCase of fixture.cases) {
    counters.cases += 1;
    const caseIssues = [];
    if (!testCase.id) caseIssues.push('case id missing');
    if (!isPlainObject(testCase.input)) {
      caseIssues.push('case input missing or not an object');
      issues.push(`${label}/${testCase.id}: ${caseIssues.join('; ')}`);
      counters.failed += 1;
      continue;
    }
    if (!isPlainObject(testCase.expect)) caseIssues.push('case expect missing or not an object');
    const output = compute(testCase.input);
    for (const key of ['productionReady', 'procurementReady', 'cloudVerified', 'a3RiskClosed']) {
      if (output[key] !== false) caseIssues.push(`root.${key} must be false`);
    }
    if (isPlainObject(testCase.expect)) checkExpected(output, testCase.expect, caseIssues);
    if (caseIssues.length === 0) {
      counters.passed += 1;
      process.stdout.write(
        `PASS ${label}/${testCase.id}${testCase.behavior ? ` — ${testCase.behavior}` : ''}\n`
      );
    } else {
      counters.failed += 1;
      for (const issue of caseIssues) issues.push(`${label}/${testCase.id}: ${issue}`);
      process.stdout.write(`FAIL ${label}/${testCase.id}\n`);
    }
  }
}

function parseArgs(argv) {
  const result = { stage: null, template: null, fixtures: null, explicitTemplate: false, explicitFixtures: false };
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (!['--stage', '--template', '--fixtures'].includes(token)) {
      throw new Error(`unknown argument: ${token}`);
    }
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) throw new Error(`missing value for ${token}`);
    index += 1;
    if (token === '--stage') result.stage = value;
    if (token === '--template') {
      result.template = value;
      result.explicitTemplate = true;
    }
    if (token === '--fixtures') {
      result.fixtures = value;
      result.explicitFixtures = true;
    }
  }
  if (!result.stage) result.stage = 's1';
  return result;
}

function main() {
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.stdout.write('FAIL capacity-validation (invalid-arguments)\n');
    process.exit(1);
  }
  if (args.stage !== 's1') {
    process.stderr.write(
      `stage ${args.stage} is not implemented in the S1 slice; backupRecovery/capabilityEvidence are deferred to S2\n`
    );
    process.stdout.write(`FAIL capacity-validation (${args.stage})\n`);
    process.exit(1);
  }
  const runTemplate = args.explicitTemplate || !args.explicitFixtures;
  const runFixtures = args.explicitFixtures || !args.explicitTemplate;
  const templatePath = args.template ?? DEFAULT_TEMPLATE;
  const fixturesPath = args.fixtures ?? DEFAULT_FIXTURES;
  const issues = [];
  const counters = {
    templateLeaves: 0,
    templateNullMetrics: 0,
    templateZeroMetrics: 0,
    probes: 0,
    probeLeaves: 0,
    cases: 0,
    passed: 0,
    failed: 0
  };

  let template = null;
  if (runTemplate) {
    try {
      template = loadJson(templatePath);
    } catch (error) {
      issues.push(`template: ${error.message}`);
    }
    if (template) {
      checkTemplateStructure(template, issues, counters);
      checkTemplateComputation(template, issues, counters);
      runNonFiniteProbes(template, issues, counters);
    }
  }

  if (runFixtures) {
    let files = [];
    try {
      files = fs
        .readdirSync(fixturesPath)
        .filter((name) => /^s1-.*\.json$/.test(name))
        .sort();
    } catch (error) {
      issues.push(`fixtures: ${error.message}`);
    }
    if (files.length === 0) issues.push(`fixtures: no s1-*.json files in ${fixturesPath}`);
    for (const file of files) runFixtureFile(path.join(fixturesPath, file), issues, counters);
  }

  if (issues.length > 0) {
    for (const issue of issues) process.stderr.write(`ISSUE ${issue}\n`);
    process.stdout.write(
      `FAIL capacity-validation stage=s1 templateLeaves=${counters.templateLeaves} probes=${counters.probes} cases=${counters.cases} passed=${counters.passed} failed=${counters.failed}\n`
    );
    process.exit(1);
  }
  process.stdout.write(
    `PASS capacity-validation stage=s1 groups=${S1_GROUP_IDS.length} templateLeaves=${counters.templateLeaves} templateStatus=${counters.templateRootStatus} templateGroups=[${counters.templateGroupStatuses}] nullMetrics=${counters.templateNullMetrics} zeroMetrics=${counters.templateZeroMetrics} nonfiniteProbes=${counters.probes} (${counters.probeLeaves} leaves x NaN/+Infinity/-Infinity) fixtureCases=${counters.cases} passed=${counters.passed} failed=${counters.failed} productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false\n`
  );
}

main();
