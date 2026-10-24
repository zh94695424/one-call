#!/usr/bin/env node
// Static governance checker for the ONECALL v1 OpenAPI contract.
//
// It performs the assertions Redocly cannot express by itself:
//   * exact stage operationId sets and five-surface counts;
//   * docs/15 operation-map coverage with exact A0 schema-qualified names;
//   * exact-set matching against the read-only docs/14 44-table catalog;
//   * versioned-path, uniqueness, server-root and duplicate-source checks.
//
// Usage:
//   node check-doc-map.mjs <bundle.json> <docs/15.md> [--stage s1|s2|s3] [--source <openapi.yaml>]

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, '..', '..');
const A0_DOC = path.join(REPO_ROOT, 'docs', '14-production-data-model-and-flyway-schema-spec.md');

const METHODS = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'trace'];
const SURFACES = ['customer', 'operations', 'ivr', 'public-share', 'provider-callback'];
// The approved path namespace is the authoritative owner of surface identity.
// Stage counts and docs-map comparison use the path-derived surface rather
// than trusting x-onecall-surface, so a coordinated extension+docs swap of a
// route cannot hide a trust-boundary mismatch.
const PREFIX_SURFACE = new Map([
  ['/api/v1/customer/', 'customer'],
  ['/api/v1/operations/', 'operations'],
  ['/api/v1/ivr/', 'ivr'],
  ['/api/v1/public/shares/', 'public-share'],
  ['/api/v1/callbacks/', 'provider-callback'],
]);
const APPROVED_PREFIXES = [...PREFIX_SURFACE.keys()];
// Returns the single approved surface for a route, or null when the route does
// not match exactly one approved prefix.
function surfaceForPath(route) {
  let owned = null;
  for (const [prefix, surface] of PREFIX_SURFACE) {
    if (typeof route === 'string' && route.startsWith(prefix)) {
      if (owned !== null) return null;
      owned = surface;
    }
  }
  return owned;
}

const S1_IDS = [
  'getCustomerMe',
  'listCustomerBusinessNumbers',
  'getCustomerSubject',
  'createCustomerSubjectClaim',
  'createCustomerSubjectActivation',
  'listCustomerEvidence',
  'getCustomerEvidence',
  'listOperationsSubjects',
  'getOperationsSubject',
  'suspendOperationsSubject',
  'resumeOperationsSubject',
  'listOperationsActivations',
  'listOperationsIvrSessions',
  'getOperationsIvrSession',
];

const S2_ADDED = [
  'listCustomerCatalogOffers',
  'createCustomerOrder',
  'listCustomerOrders',
  'getCustomerOrder',
  'listOperationsCatalogProducts',
  'getOperationsCatalogProduct',
  'publishOperationsCatalogVersion',
  'offsaleOperationsCatalogVersion',
  'listOperationsTrialPolicies',
  'publishOperationsTrialPolicy',
  'disableOperationsTrialPolicy',
  'listOperationsOrders',
  'getOperationsOrder',
  'listOperationsAuditFacts',
  'createOperationsCatalogVersionDraft',
  'replaceOperationsCatalogVersionDraft',
  'createOperationsTrialPolicyDraft',
  'replaceOperationsTrialPolicyDraft',
];

const S3_ADDED = [
  'createCustomerOutboundCallTask',
  'getCustomerOutboundCallTask',
  'cancelCustomerOutboundCallTask',
  'createCustomerShare',
  'listCustomerShares',
  'revokeCustomerShare',
  'rotateCustomerShareCode',
  'resolveIvrInbound',
  'confirmIvrActivation',
  'authorizeIvrDirectOutbound',
  'ingestIvrCallEvent',
  'declareIvrRecording',
  'completeIvrRecording',
  'getPublicShare',
  'createPublicShareAdmission',
  'authorizePublicShareMedia',
  'receivePaymentProviderCallback',
];

const STAGES = {
  s1: { ids: S1_IDS, counts: { customer: 7, operations: 7, 'ivr': 0, 'public-share': 0, 'provider-callback': 0 } },
  s2: { ids: [...S1_IDS, ...S2_ADDED], counts: { customer: 11, operations: 21, 'ivr': 0, 'public-share': 0, 'provider-callback': 0 } },
  s3: { ids: [...S1_IDS, ...S2_ADDED, ...S3_ADDED], counts: { customer: 18, operations: 21, 'ivr': 6, 'public-share': 3, 'provider-callback': 1 } },
};

function parseArgs(argv) {
  const positional = [];
  let stage = null;
  let source = null;
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--stage') {
      stage = argv[index + 1];
      index += 1;
    } else if (arg === '--source') {
      source = argv[index + 1];
      index += 1;
    } else if (arg.startsWith('--')) {
      throw new Error(`unknown option ${arg}`);
    } else {
      positional.push(arg);
    }
  }
  if (positional.length !== 2) {
    throw new Error('usage: check-doc-map.mjs <bundle.json> <docs/15.md> [--stage s1|s2|s3] [--source <openapi.yaml>]');
  }
  if (stage !== null && !STAGES[stage]) {
    throw new Error(`unknown stage ${JSON.stringify(stage)}; expected s1, s2 or s3`);
  }
  return { bundlePath: positional[0], docPath: positional[1], stage, sourcePath: source };
}

function readCatalog() {
  const text = fs.readFileSync(A0_DOC, 'utf8');
  const block = text.match(/<!-- catalog:start -->([\s\S]*?)<!-- catalog:end -->/);
  if (!block) throw new Error('docs/14 catalog block is missing');
  const rows = block[1].split('\n');
  const names = [];
  for (const row of rows) {
    const match = row.match(/^\|\s*`([a-z_]+\.[a-z_]+)`\s*\|/);
    if (match) names.push(match[1]);
  }
  return names;
}

function readOperations(bundle) {
  const operations = [];
  const paths = bundle?.paths ?? {};
  for (const [route, pathItem] of Object.entries(paths)) {
    if (!pathItem || typeof pathItem !== 'object') continue;
    for (const method of METHODS) {
      const operation = pathItem[method];
      if (!operation || typeof operation !== 'object') continue;
      operations.push({
        operationId: operation.operationId,
        method: method.toUpperCase(),
        path: route,
        surface: operation['x-onecall-surface'],
        pathSurface: surfaceForPath(route),
        models: Array.isArray(operation['x-onecall-a0-models']) ? operation['x-onecall-a0-models'] : [],
      });
    }
  }
  return operations;
}

function readDocRows(docText) {
  const block = docText.match(/<!-- operation-map:start -->([\s\S]*?)<!-- operation-map:end -->/);
  if (!block) throw new Error('docs/15 operation-map block is missing');
  const rows = [];
  for (const line of block[1].split('\n')) {
    if (!line.trim().startsWith('|')) continue;
    if (/^\|\s*-+/.test(line)) continue;
    if (/^\|\s*operationId\s*\|/.test(line)) continue;
    const cells = line.split('|').slice(1, -1).map((cell) => cell.trim());
    if (cells.length !== 5) continue;
    const [operationId, method, route, surface, modelsCell] = cells;
    const models = [...modelsCell.matchAll(/`([^`]+)`/g)].map((match) => match[1]);
    rows.push({ operationId, method, route, surface, models });
  }
  return rows;
}

function countSurfaces(operations) {
  // Count by the path-derived surface. A route/extension mismatch is already
  // an explicit error below; stage counts must not be bookkept from the
  // extension alone, or a coordinated extension+docs swap would stay green.
  const counts = Object.fromEntries(SURFACES.map((surface) => [surface, 0]));
  for (const operation of operations) {
    if (operation.pathSurface in counts) counts[operation.pathSurface] += 1;
  }
  return counts;
}

function setOf(values) {
  return new Set(values);
}

function duplicates(values) {
  const seen = new Set();
  const repeated = new Set();
  for (const value of values) {
    if (seen.has(value)) repeated.add(value);
    seen.add(value);
  }
  return [...repeated];
}

function scanSource(sourcePath) {
  const text = fs.readFileSync(sourcePath, 'utf8');
  const lines = text.split('\n');
  const relative = path.relative(REPO_ROOT, sourcePath);
  const metrics = {
    source: relative,
    duplicateRoutes: [],
    duplicateMethodRoutes: [],
    duplicateOperationIds: [],
    unversionedPaths: [],
  };
  const pathIndent = /^( {2})(\/\S*):\s*$/;
  const methodIndent = /^ {4}(get|post|put|patch|delete|head|options|trace):\s*$/;
  const operationIdIndent = /^ {6}operationId:\s*(\S+)\s*$/;
  const pathCounts = new Map();
  const routeCounts = new Map();
  const operationIdCounts = new Map();
  let currentPath = null;
  for (const line of lines) {
    const pathMatch = line.match(pathIndent);
    if (pathMatch) {
      currentPath = pathMatch[2];
      pathCounts.set(currentPath, (pathCounts.get(currentPath) ?? 0) + 1);
      if (!currentPath.startsWith('/api/v1/')) metrics.unversionedPaths.push(currentPath);
      continue;
    }
    const methodMatch = line.match(methodIndent);
    if (methodMatch && currentPath) {
      const key = `${methodMatch[1].toUpperCase()} ${currentPath}`;
      routeCounts.set(key, (routeCounts.get(key) ?? 0) + 1);
      continue;
    }
    const operationMatch = line.match(operationIdIndent);
    if (operationMatch) {
      const id = operationMatch[1];
      operationIdCounts.set(id, (operationIdCounts.get(id) ?? 0) + 1);
    }
  }
  metrics.duplicateRoutes = [...pathCounts.entries()].filter(([, count]) => count > 1).map(([route]) => route);
  metrics.duplicateMethodRoutes = [...routeCounts.entries()].filter(([, count]) => count > 1).map(([route]) => route);
  metrics.duplicateOperationIds = [...operationIdCounts.entries()].filter(([, count]) => count > 1).map(([id]) => id);
  metrics.unversionedPaths = [...new Set(metrics.unversionedPaths)];
  return metrics;
}

function main() {
  let args;
  try {
    args = parseArgs(process.argv.slice(2));
  } catch (error) {
    console.error(`FAIL check-doc-map: ${error.message}`);
    process.exit(2);
  }

  const errors = [];
  const bundle = JSON.parse(fs.readFileSync(args.bundlePath, 'utf8'));
  const docText = fs.readFileSync(args.docPath, 'utf8');
  const catalog = readCatalog();
  const catalogSet = setOf(catalog);
  const operations = readOperations(bundle);
  const docRows = readDocRows(docText);
  const actualIds = operations.map((operation) => operation.operationId);
  const actualIdSet = setOf(actualIds);
  const counts = countSurfaces(operations);

  if (catalog.length !== catalogSet.size) {
    errors.push(`docs/14 catalog contains duplicate names (${catalog.length} rows, ${catalogSet.size} unique)`);
  }
  if (catalogSet.size !== 44) {
    errors.push(`docs/14 catalog must contain exactly 44 unique tables, found ${catalogSet.size}`);
  }
  if (bundle?.openapi !== undefined && !/^3\.1\./.test(String(bundle.openapi))) {
    errors.push(`bundle openapi version must be 3.1.x, found ${bundle.openapi}`);
  }

  // Server root.
  const servers = Array.isArray(bundle?.servers) ? bundle.servers : [];
  const relativeRoot = servers.length === 1 && servers[0]?.url === '/';
  if (!relativeRoot) {
    errors.push(`top-level servers must be exactly one relative url "/", found ${JSON.stringify(servers)}`);
  }

  // Paths and operation identity.
  const duplicateOperationIds = duplicates(actualIds);
  if (duplicateOperationIds.length > 0) errors.push(`duplicate operationIds: ${duplicateOperationIds.join(', ')}`);
  let unversioned = 0;
  let multiSurface = 0;
  for (const operation of operations) {
    if (!operation.path.startsWith('/api/v1/')) {
      errors.push(`${operation.operationId}: path is not versioned: ${operation.path}`);
      unversioned += 1;
    }
    const matches = APPROVED_PREFIXES.filter((prefix) => operation.path.startsWith(prefix));
    if (matches.length !== 1) {
      errors.push(`${operation.operationId}: path must match exactly one approved surface prefix`);
      multiSurface += 1;
    } else if (operation.pathSurface !== null && operation.surface !== operation.pathSurface) {
      // Path namespace owns surface identity: the extension must agree with
      // the single approved prefix so coordinated docs+extension swaps cannot
      // move an operation across the customer/employee trust boundary.
      errors.push(
        `${operation.operationId}: x-onecall-surface ${JSON.stringify(operation.surface)}`
        + ` does not own path surface ${JSON.stringify(operation.pathSurface)}`,
      );
    }
    if (!SURFACES.includes(operation.surface)) {
      errors.push(`${operation.operationId}: invalid x-onecall-surface ${JSON.stringify(operation.surface)}`);
    }
  }

  // A0 model validity and duplicates.
  let a0Invalid = 0;
  let a0Duplicate = 0;
  for (const operation of operations) {
    if (operation.models.length === 0) errors.push(`${operation.operationId}: no A0 models`);
    if (operation.models.length !== setOf(operation.models).size) {
      errors.push(`${operation.operationId}: duplicate A0 model entries`);
      a0Duplicate += 1;
    }
    for (const model of operation.models) {
      if (!/^[a-z_]+\.[a-z_]+$/.test(model) || !catalogSet.has(model)) {
        errors.push(`${operation.operationId}: invalid or unknown A0 model ${model}`);
        a0Invalid += 1;
      }
    }
  }

  // Docs/15 operation-map exact coverage and A0 set equality.
  const docIds = docRows.map((row) => row.operationId);
  const docIdSet = setOf(docIds);
  const duplicateDocIds = duplicates(docIds);
  if (duplicateDocIds.length > 0) errors.push(`docs/15 has duplicate operation rows: ${duplicateDocIds.join(', ')}`);
  let docMissing = 0;
  let docExtra = 0;
  let a0Missing = 0;
  let a0Extra = 0;
  let docInvalid = 0;
  for (const operation of operations) {
    const matches = docRows.filter((row) => row.operationId === operation.operationId);
    if (matches.length === 0) {
      errors.push(`docs/15 is missing operation ${operation.operationId}`);
      docMissing += 1;
      continue;
    }
    const row = matches[0];
    // Compare the docs row against the path-derived surface (the authoritative
    // owner), not the extension alone, so a coordinated extension+docs row
    // swap cannot hide a route/surface ownership mismatch.
    const expectedDocSurface = operation.pathSurface ?? operation.surface;
    if (row.method !== operation.method || row.route !== operation.path || row.surface !== expectedDocSurface) {
      errors.push(`docs/15 row for ${operation.operationId} does not match method/path/surface`);
      docInvalid += 1;
    }
    const rowSet = setOf(row.models);
    if (row.models.length !== rowSet.size) {
      errors.push(`docs/15 row for ${operation.operationId} has duplicate A0 models`);
      docInvalid += 1;
    }
    for (const model of operation.models) {
      if (!rowSet.has(model)) {
        errors.push(`docs/15 row for ${operation.operationId} is missing A0 model ${model}`);
        a0Missing += 1;
      }
    }
    for (const model of row.models) {
      if (!catalogSet.has(model)) {
        errors.push(`docs/15 row for ${operation.operationId} has invalid A0 model ${model}`);
        docInvalid += 1;
      }
      if (!operation.models.includes(model)) {
        errors.push(`docs/15 row for ${operation.operationId} has extra A0 model ${model}`);
        a0Extra += 1;
      }
    }
  }
  for (const row of docRows) {
    if (!actualIdSet.has(row.operationId)) {
      errors.push(`docs/15 has an operation not present in the contract: ${row.operationId}`);
      docExtra += 1;
    }
  }

  // Exact stage set and five-surface counts.
  let stageCounts = null;
  if (args.stage) {
    const stage = STAGES[args.stage];
    const expectedSet = setOf(stage.ids);
    const missing = stage.ids.filter((id) => !actualIdSet.has(id));
    const unexpected = actualIds.filter((id) => !expectedSet.has(id));
    if (missing.length > 0) errors.push(`stage ${args.stage}: missing operationIds ${missing.join(', ')}`);
    if (unexpected.length > 0) errors.push(`stage ${args.stage}: unexpected operationIds ${unexpected.join(', ')}`);
    for (const surface of SURFACES) {
      const expected = stage.counts[surface] ?? 0;
      if ((counts[surface] ?? 0) !== expected) {
        errors.push(`stage ${args.stage}: ${surface} count ${counts[surface] ?? 0} != ${expected}`);
      }
    }
    if (actualIds.length !== stage.ids.length) {
      errors.push(`stage ${args.stage}: total ${actualIds.length} != ${stage.ids.length}`);
    }
    stageCounts = stage.counts;
  }

  // Supplementary raw-source duplicate scan.
  let sourceMetrics = null;
  if (args.sourcePath) {
    sourceMetrics = scanSource(args.sourcePath);
    if (sourceMetrics.duplicateRoutes.length > 0) {
      errors.push(`source has duplicate path keys: ${sourceMetrics.duplicateRoutes.join(', ')}`);
    }
    if (sourceMetrics.duplicateMethodRoutes.length > 0) {
      errors.push(`source has duplicate method+path entries: ${sourceMetrics.duplicateMethodRoutes.join(', ')}`);
    }
    if (sourceMetrics.duplicateOperationIds.length > 0) {
      errors.push(`source has duplicate operationIds: ${sourceMetrics.duplicateOperationIds.join(', ')}`);
    }
    if (sourceMetrics.unversionedPaths.length > 0) {
      errors.push(`source has unversioned paths: ${sourceMetrics.unversionedPaths.join(', ')}`);
    }
  }

  const total = operations.length;
  const countLine = SURFACES.map((surface) => `${surface}=${counts[surface] ?? 0}`).join(' ');
  console.log(`stage=${args.stage ?? 'none'}`);
  console.log(`operations=${total} ${countLine} total=${total}`);
  if (stageCounts) {
    console.log(`expected=${STAGES[args.stage].ids.length} expected_counts=${SURFACES.map((surface) => stageCounts[surface] ?? 0).join('/')}`);
  }
  console.log(`doc_rows=${docRows.length} doc_missing=${docMissing} doc_extra=${docExtra} doc_duplicate=${duplicateDocIds.length} doc_invalid=${docInvalid}`);
  console.log(`a0_catalog=${catalogSet.size} a0_missing=${a0Missing} a0_extra=${a0Extra} a0_duplicate=${a0Duplicate} a0_invalid=${a0Invalid}`);
  console.log(`paths_unversioned=${unversioned} paths_multi_surface=${multiSurface} duplicate_operation_ids=${duplicateOperationIds.length}`);
  console.log(`servers=${servers.length} relative_root=${relativeRoot}`);
  if (sourceMetrics) {
    console.log(`source_duplicate_paths=${sourceMetrics.duplicateRoutes.length} source_duplicate_routes=${sourceMetrics.duplicateMethodRoutes.length} source_duplicate_operation_ids=${sourceMetrics.duplicateOperationIds.length}`);
  }

  if (errors.length > 0) {
    for (const error of errors) console.error(`ERROR ${error}`);
    console.error(`FAIL check-doc-map: ${errors.length} error(s)`);
    process.exit(1);
  }
  console.log(`PASS check-doc-map${args.stage ? ` (${args.stage})` : ''}`);
}

main();
