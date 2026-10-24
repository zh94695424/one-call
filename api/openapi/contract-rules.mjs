// Redocly custom rules for the ONECALL v1 OpenAPI contract.
//
// The rules are intentionally static: they read the canonical contract and the
// read-only A0 catalog in docs/14. They do not contact a service, database or
// vendor and they never modify repository inputs.

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(HERE, '..', '..');
const A0_DOC = path.join(REPO_ROOT, 'docs', '14-production-data-model-and-flyway-schema-spec.md');

const SURFACES = ['customer', 'operations', 'ivr', 'public-share', 'provider-callback'];
// The approved path namespace is the authoritative owner of surface identity.
// Each prefix maps to exactly one surface; a route that matches exactly one
// prefix must declare that surface in x-onecall-surface/tag/audience/security.
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
function surfaceForRoute(route) {
  let owned = null;
  for (const [prefix, surface] of PREFIX_SURFACE) {
    if (typeof route === 'string' && route.startsWith(prefix)) {
      if (owned !== null) return null;
      owned = surface;
    }
  }
  return owned;
}
const AUDIENCES = ['customer', 'employee', 'ivr-service', 'public-capability', 'provider'];
const IDEMPOTENCY_VALUES = new Set([
  'idempotency-key',
  'event-body',
  'state-conditional',
  'provider-event',
  'not-applicable',
]);
const WRITE_METHODS = new Set(['post', 'put', 'patch', 'delete']);
const METHODS = ['get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'trace'];
const FORBIDDEN_PROBLEM_CODES = new Set([
  'CONFIRMATION_NOT_ALLOWED',
  'TASK_CONFLICT',
  'CALL_IN_PROGRESS',
  'TASK_CLAIMED',
  'TASK_UNAVAILABLE',
]);
const FORBIDDEN_PROPERTY_NAMES = new Set([
  'objectKey',
  'objectVersionId',
  'bucket',
  'cryptoKeyRef',
  'capabilityTokenHash',
  'codeHash',
  'codeSalt',
  'payloadCiphertext',
  'callerNumber',
  'calleeNumber',
  'fullNumber',
  'idNumber',
  'legalName',
  'providerSubject',
]);
const DRAFT_OPERATION_IDS = new Set([
  'createOperationsCatalogVersionDraft',
  'replaceOperationsCatalogVersionDraft',
  'createOperationsTrialPolicyDraft',
  'replaceOperationsTrialPolicyDraft',
]);
// Only O18/O19 catalog draft create/replace may carry client draft commercial
// priceCents and currency. O20/O21 trial drafts reject both (F-05 / docs/15 §6).
const DRAFT_COMMERCIAL_OPERATION_IDS = new Set([
  'createOperationsCatalogVersionDraft',
  'replaceOperationsCatalogVersionDraft',
]);
// Approved open-object exceptions for the response-side closure walker. These
// are the only component schemas allowed to leave additionalProperties open
// (provider opaque payloads / IVR event payloads, plan §4.11-§4.12). Any schema
// may also opt in via the explicit x-onecall-opaque: true annotation.
const OPAQUE_CLOSURE_EXCEPTION_SCHEMAS = new Set([
  'OpaqueProviderPayload',
  'IvrEventPayload',
]);
const IVR_DECISION_OPERATION_IDS = new Set([
  'resolveIvrInbound',
  'confirmIvrActivation',
  'authorizeIvrDirectOutbound',
]);
// Approved common Problem codes every operation must expose. These are excluded
// from the per-operation exact domain-set checks below.
const COMMON_PROBLEM_CODES = new Set([
  'UNAUTHENTICATED',
  'FORBIDDEN',
  'VALIDATION_FAILED',
  'RATE_LIMITED',
  'SERVICE_UNAVAILABLE',
  'INTERNAL',
]);
// Accepted operationId -> Idempotency-Key maxLength contract (plan §4.7/§4.10).
// Fail closed: any idempotency-key operation absent from this map is rejected.
const IDEMPOTENCY_KEY_MAX_LENGTHS = new Map([
  ['createCustomerSubjectClaim', 128],
  ['suspendOperationsSubject', 128],
  ['resumeOperationsSubject', 128],
  ['createCustomerOrder', 475],
  ['createCustomerOutboundCallTask', 512],
  ['cancelCustomerOutboundCallTask', 512],
]);
// Request bodies must never accept server-derived trust facts (docs/15 §6 /
// plan §5 No-expose). Exact property-name match only; legitimate identifiers
// such as subjectId, bindingId, verificationProofId, activationEventId and
// expectedActivationRevision stay allowed.
const TRUSTED_REQUEST_FACT_NAMES = new Set([
  'account',
  'accountId',
  'owner',
  'ownerAccountId',
  'ownerOrganizationId',
  'role',
  'workspaceRole',
  'price',
  'priceCents',
  'amount',
  'amountCents',
  'currency',
  'paidAt',
  'snapshotSha256',
  'versionToken',
  'purchaseConsentId',
  'purchaseConsentEventId',
  'issuanceStatus',
  'paymentSuccess',
  'paymentStatus',
  'trialOutcome',
  'trialResult',
  'entitlementStatus',
  'createdByAccountId',
  'evidenceSolidified',
  'evidenceStatus',
  'recordingOwner',
  'taskClaimed',
  'reservationId',
  'authorizedRecordingSeconds',
  'authorizationExpiresAt',
  'shareAdmissionCount',
  'shareStatus',
  'objectKey',
  'bucket',
  'objectVersionId',
  'capabilityTokenHash',
  'codeHash',
  'codeSalt',
  'cryptoKeyRef',
]);

function readA0() {
  return fs.readFileSync(A0_DOC, 'utf8');
}

let cachedCatalog = null;
function catalogNames() {
  if (cachedCatalog) return cachedCatalog;
  const text = readA0();
  const block = text.match(/<!-- catalog:start -->([\s\S]*?)<!-- catalog:end -->/);
  if (!block) {
    throw new Error('docs/14 catalog block is missing');
  }
  const names = [...block[1].matchAll(/^\|\s*`([a-z_]+\.[a-z_]+)`\s*\|/gm)].map((match) => match[1]);
  cachedCatalog = names;
  return names;
}

let cachedAnchors = null;
function a0ContractAnchors() {
  if (cachedAnchors) return cachedAnchors;
  const text = readA0();
  const anchors = new Set();
  for (const match of text.matchAll(/\bT-S[123]-\d{2}\b/g)) anchors.add(match[0]);
  for (const section of ['§7.1', '§7.2', '§7.3', '§7.4']) anchors.add(section);
  for (const domain of ['ACTIVATION_STATUS', 'EVIDENCE', 'IVR_EVENT', 'SHARE_ACCESS']) anchors.add(domain);
  for (const match of text.matchAll(/\b(?:uq|ck)_[a-z0-9_]+\b/g)) anchors.add(match[0]);
  cachedAnchors = anchors;
  return anchors;
}

function deref(root, node) {
  let current = node;
  let guard = 0;
  while (current && typeof current === 'object' && typeof current.$ref === 'string' && guard < 20) {
    const ref = current.$ref;
    if (!ref.startsWith('#/')) return current;
    let target = root;
    for (const part of ref.slice(2).split('/')) {
      const key = part.replace(/~1/g, '/').replace(/~0/g, '~');
      target = target?.[key];
    }
    current = target;
    guard += 1;
  }
  return current;
}

// Cycle-safe local $ref resolver for request-schema walking. Unlike the
// generic deref guard, it resolves any finite local '#/...' chain without a
// silent depth cutoff (cycles are detected by $ref string) and reports
// external, missing or cyclic refs instead of returning a still-referenced
// node that callers would treat as a concrete schema. Under the reject-not-merge
// policy every $ref object in the chain — the entry node and every intermediate
// alias hop — must be pure: sibling keywords are rejected here (single semantic
// owner of the request-schema $ref-sibling check, so the walker no longer
// re-checks entry siblings and cannot double-report them). Returns `{ node }`
// on success (node may be a boolean schema) or `{ problem }` on failure.
function resolveRequestSchemaRef(root, schema) {
  let current = schema;
  const seenRefs = new Set();
  while (current && typeof current === 'object' && typeof current.$ref === 'string') {
    // F5-02: a $ref object carrying sibling keywords is rejected (not merged).
    // Sibling keywords apply under JSON Schema 2020-12, so an impure hop could
    // otherwise smuggle trusted facts one hop away from the entry check.
    const siblings = Object.keys(current).filter((key) => key !== '$ref');
    if (siblings.length > 0) {
      return { problem: `request schema $ref must not carry sibling keywords: ${siblings.join(', ')}` };
    }
    const ref = current.$ref;
    if (!ref.startsWith('#/')) {
      return { problem: `request schema $ref must resolve to a local schema, found external ref ${JSON.stringify(ref)}` };
    }
    if (seenRefs.has(ref)) {
      return { problem: `request schema $ref chain is cyclic at ${JSON.stringify(ref)}` };
    }
    seenRefs.add(ref);
    let target = root;
    for (const part of ref.slice(2).split('/')) {
      const key = part.replace(/~1/g, '/').replace(/~0/g, '~');
      target = target?.[key];
    }
    if (target === undefined || target === null
      || (typeof target !== 'object' && typeof target !== 'boolean')) {
      return { problem: `request schema $ref does not resolve: ${JSON.stringify(ref)}` };
    }
    current = target;
  }
  return { node: current };
}

function eachOperation(root, callback) {
  const paths = root.paths && typeof root.paths === 'object' ? root.paths : {};
  for (const [route, pathItem] of Object.entries(paths)) {
    if (!pathItem || typeof pathItem !== 'object') continue;
    for (const method of METHODS) {
      const operation = pathItem[method];
      if (operation && typeof operation === 'object') {
        callback(operation, route, method, pathItem);
      }
    }
  }
}

function operationParameters(root, operation, pathItem) {
  const raw = [
    ...(Array.isArray(pathItem?.parameters) ? pathItem.parameters : []),
    ...(Array.isArray(operation?.parameters) ? operation.parameters : []),
  ];
  return raw.map((parameter) => deref(root, parameter)).filter(Boolean);
}

function successSchema(root, operation) {
  const responses = operation?.responses && typeof operation.responses === 'object' ? operation.responses : {};
  for (const [status, raw] of Object.entries(responses)) {
    if (!/^2\d\d$/.test(status)) continue;
    const response = deref(root, raw);
    const content = response?.content;
    if (!content || typeof content !== 'object') continue;
    for (const [mediaType, media] of Object.entries(content)) {
      if (mediaType === 'application/problem+json') continue;
      if (media?.schema) return media.schema;
    }
  }
  return null;
}

function responseStatuses(operation) {
  return new Set(Object.keys(operation?.responses ?? {}));
}

function problemSchema(root) {
  return root?.components?.schemas?.Problem;
}

function problemCodeEnum(root) {
  return root?.components?.schemas?.ProblemCode?.enum ?? [];
}

function registry(root) {
  return root?.['x-onecall-problem-registry'] ?? {};
}

function extensionArray(operation, name) {
  const value = operation?.[name];
  return Array.isArray(value) ? value : null;
}

function validateServerRelative(root) {
  const problems = [];
  const servers = root?.servers;
  if (!Array.isArray(servers) || servers.length === 0) {
    problems.push('servers is required');
    return problems;
  }
  if (servers.length !== 1) {
    problems.push(`servers must contain exactly one relative root, found ${servers.length}`);
  }
  for (const server of servers) {
    const url = server?.url;
    if (url !== '/') {
      problems.push(`server url must be exactly "/", found ${JSON.stringify(url)}`);
    }
    if (typeof url === 'string' && (/^[a-z][a-z0-9+.-]*:/i.test(url) || url.startsWith('//'))) {
      problems.push(`absolute server url is forbidden: ${url}`);
    }
  }
  return problems;
}

function validateTagged(root) {
  const problems = [];
  const tags = Array.isArray(root?.tags) ? root.tags : [];
  const names = tags.map((tag) => tag?.name);
  const expected = [...SURFACES].sort().join(',');
  const actual = [...new Set(names)].sort().join(',');
  if (names.length !== 5 || actual !== expected) {
    problems.push(`exactly five surface tags are required; found ${JSON.stringify(names)}`);
  }
  eachOperation(root, (operation, route) => {
    const operationTags = Array.isArray(operation.tags) ? operation.tags : [];
    if (operationTags.length !== 1) {
      problems.push(`${route}: operation must declare exactly one tag`);
      return;
    }
    if (!SURFACES.includes(operationTags[0])) {
      problems.push(`${route}: tag ${operationTags[0]} is not an approved surface tag`);
    }
    if (operation['x-onecall-surface'] !== operationTags[0]) {
      problems.push(`${route}: x-onecall-surface must match the operation tag`);
    }
  });
  return problems;
}

function validateVersionedPaths(root) {
  const problems = [];
  const seenOperationIds = new Map();
  const seenRoutes = new Set();
  eachOperation(root, (operation, route, method) => {
    const key = `${method.toUpperCase()} ${route}`;
    if (seenRoutes.has(key)) {
      problems.push(`duplicate method+path: ${key}`);
    }
    seenRoutes.add(key);
    if (!route.startsWith('/api/v1/')) {
      problems.push(`${key}: path must start with /api/v1/`);
    }
    if (route.includes('/api/v2')) {
      problems.push(`${key}: /api/v2 is not allowed in the v1 contract`);
    }
    const matches = APPROVED_PREFIXES.filter((prefix) => route.startsWith(prefix));
    if (matches.length !== 1) {
      problems.push(`${key}: path must match exactly one approved surface prefix`);
    } else {
      // Path namespace owns surface identity: once the route maps to exactly
      // one approved prefix, x-onecall-surface must equal that surface so
      // tag/audience/security checks cannot be moved across trust boundaries.
      const ownedSurface = surfaceForRoute(route);
      if (operation['x-onecall-surface'] !== ownedSurface) {
        problems.push(
          `${key}: x-onecall-surface ${JSON.stringify(operation['x-onecall-surface'])}`
          + ` must equal the approved path surface ${JSON.stringify(ownedSurface)}`,
        );
      }
    }
    const operationId = operation.operationId;
    if (typeof operationId !== 'string' || !/^[a-z][A-Za-z0-9]*$/.test(operationId)) {
      problems.push(`${key}: operationId must be stable camelCase`);
    } else if (seenOperationIds.has(operationId)) {
      problems.push(`duplicate operationId ${operationId} at ${key}`);
    } else {
      seenOperationIds.set(operationId, key);
    }
  });
  if (seenRoutes.size === 0) problems.push('no operations found');
  return problems;
}

// Full-document fail-closed scan for unknown x-onecall-*scope* extension
// keys. The controller-accepted clarification is not operation-only: any
// extension key matching x-onecall-*scope* case-insensitively is rejected
// anywhere in the OpenAPI object graph (root, components collections and
// schemas, path and operation objects, request/response and nested schema
// objects). This subsumes the former operation-only scan and preserves its
// operation-level message format. Ordinary schema property names such as a
// business `scope` property and approved extension keys without `scope` (for
// example x-onecall-idempotency) remain allowed. Invented scope values stay
// covered by the idempotency/domain checks; O18-O21 keep state-conditional
// behavior with no Idempotency-Key and no platform.idempotency_record.
const SCOPE_EXTENSION_KEY = /^x-onecall-.*scope/i;

function formatScopeExtensionLocation(segments) {
  if (segments.length === 0) return 'document root';
  // Operation objects keep the historical `METHOD /route` message form.
  if (segments.length === 3 && segments[0] === 'paths' && METHODS.includes(segments[2])) {
    return `${String(segments[2]).toUpperCase()} ${segments[1]}`;
  }
  let location = '';
  for (const segment of segments) {
    if (typeof segment === 'string' && segment.startsWith('[')) {
      location += segment;
    } else {
      location = location === '' ? String(segment) : `${location}.${segment}`;
    }
  }
  return location;
}

function validateNoScopeExtensions(root) {
  const problems = [];
  const seen = new Set();
  const walk = (node, segments) => {
    if (!node || typeof node !== 'object' || seen.has(node)) return;
    seen.add(node);
    if (Array.isArray(node)) {
      node.forEach((child, index) => walk(child, [...segments, `[${index}]`]));
      return;
    }
    const location = formatScopeExtensionLocation(segments);
    for (const [key, value] of Object.entries(node)) {
      if (typeof key === 'string' && SCOPE_EXTENSION_KEY.test(key)) {
        problems.push(`${location}: unapproved x-onecall scope extension ${key}`);
      }
      walk(value, [...segments, key]);
    }
  };
  walk(root, []);
  return problems;
}

function validateOperationMetadata(root) {
  const problems = validateNoScopeExtensions(root);
  const catalog = new Set(catalogNames());
  if (catalog.size !== 44) {
    problems.push(`docs/14 catalog must contain exactly 44 unique qualified names, found ${catalog.size}`);
  }
  const anchors = a0ContractAnchors();
  const codes = new Set(problemCodeEnum(root));
  const registryCodes = new Set(Object.keys(registry(root)));
  const requiredExtensions = [
    'x-onecall-surface',
    'x-onecall-audience',
    'x-onecall-authorization',
    'x-onecall-a0-models',
    'x-onecall-a0-contracts',
    'x-onecall-error-codes',
    'x-onecall-idempotency',
  ];
  eachOperation(root, (operation, route, method) => {
    const key = `${method.toUpperCase()} ${route}`;
    for (const extension of requiredExtensions) {
      if (operation[extension] === undefined) {
        problems.push(`${key}: missing ${extension}`);
      }
    }
    if (!SURFACES.includes(operation['x-onecall-surface'])) {
      problems.push(`${key}: x-onecall-surface is not approved`);
    }
    if (!AUDIENCES.includes(operation['x-onecall-audience'])) {
      problems.push(`${key}: x-onecall-audience is not approved`);
    }
    if (typeof operation['x-onecall-authorization'] !== 'string' || operation['x-onecall-authorization'].length === 0) {
      problems.push(`${key}: x-onecall-authorization must be a non-empty server-derived predicate`);
    }
    const models = extensionArray(operation, 'x-onecall-a0-models');
    if (!models || models.length === 0) {
      problems.push(`${key}: x-onecall-a0-models must contain at least one A0 table`);
    } else {
      const seen = new Set();
      for (const model of models) {
        if (seen.has(model)) problems.push(`${key}: duplicate A0 model ${model}`);
        seen.add(model);
        if (typeof model !== 'string' || !/^[a-z_]+\.[a-z_]+$/.test(model)) {
          problems.push(`${key}: invalid schema-qualified A0 model ${JSON.stringify(model)}`);
        } else if (!catalog.has(model)) {
          problems.push(`${key}: A0 model ${model} is not in the docs/14 44-table catalog`);
        }
      }
    }
    const contracts = extensionArray(operation, 'x-onecall-a0-contracts');
    if (!contracts || contracts.length === 0) {
      problems.push(`${key}: x-onecall-a0-contracts must contain at least one A0 anchor`);
    } else {
      for (const contract of contracts) {
        if (typeof contract !== 'string' || !anchors.has(contract)) {
          problems.push(`${key}: A0 contract anchor ${JSON.stringify(contract)} is not an accepted docs/14 anchor`);
        }
      }
    }
    const errorCodes = extensionArray(operation, 'x-onecall-error-codes');
    if (!errorCodes || errorCodes.length === 0) {
      problems.push(`${key}: x-onecall-error-codes must contain at least one Problem code`);
    } else {
      const seen = new Set();
      for (const code of errorCodes) {
        if (seen.has(code)) problems.push(`${key}: duplicate Problem code ${code}`);
        seen.add(code);
        if (typeof code !== 'string' || code === '*' || code.endsWith('_*')) {
          problems.push(`${key}: wildcard Problem code is forbidden: ${JSON.stringify(code)}`);
          continue;
        }
        if (!codes.has(code)) problems.push(`${key}: Problem code ${code} is not in ProblemCode`);
        if (!registryCodes.has(code)) problems.push(`${key}: Problem code ${code} is not in the problem registry`);
        if (FORBIDDEN_PROBLEM_CODES.has(code)) {
          problems.push(`${key}: IVR decision reason ${code} must not be a Problem code`);
        }
      }
    }
    const idempotency = operation['x-onecall-idempotency'];
    if (!IDEMPOTENCY_VALUES.has(idempotency)) {
      problems.push(`${key}: x-onecall-idempotency ${JSON.stringify(idempotency)} is not approved`);
    }
    if (operation['x-onecall-required-roles'] !== undefined) {
      const roles = operation['x-onecall-required-roles'];
      if (!Array.isArray(roles) || roles.length === 0 || roles.some((role) => role !== 'ADMIN' && role !== 'OPS')) {
        problems.push(`${key}: x-onecall-required-roles must be a non-empty subset of ADMIN/OPS`);
      }
    }
    // Unknown x-onecall-*scope* extension keys are rejected for every
    // operation by validateNoScopeExtensions above (full-document scan);
    // no scope extension is approved on this surface.
  });
  return problems;
}

function validateSecurityBySurface(root) {
  const problems = [];
  const schemes = root?.components?.securitySchemes ?? {};
  const expectedSchemeNames = [
    'customerSession',
    'employeeSession',
    'ivrMtls',
    'ivrSignature',
    'providerCallbackMtls',
    'providerCallbackSignature',
  ];
  for (const name of expectedSchemeNames) {
    if (!schemes[name]) problems.push(`security scheme ${name} is required`);
  }
  eachOperation(root, (operation, route, method) => {
    const key = `${method.toUpperCase()} ${route}`;
    const security = operation.security;
    if (!Array.isArray(security) || security.length === 0) {
      problems.push(`${key}: security is required and must not be empty`);
      return;
    }
    const surface = operation['x-onecall-surface'];
    const requirements = security.map((requirement) => Object.keys(requirement ?? {}).sort());
    if (surface === 'customer') {
      const ok = requirements.length === 1 && requirements[0].length === 1 && requirements[0][0] === 'customerSession';
      if (!ok) problems.push(`${key}: customer operations require customerSession only`);
    } else if (surface === 'operations') {
      const ok = requirements.length === 1 && requirements[0].length === 1 && requirements[0][0] === 'employeeSession';
      if (!ok) problems.push(`${key}: operations require employeeSession only`);
    } else if (surface === 'ivr') {
      const ok = requirements.length === 1
        && requirements[0].length === 2
        && requirements[0].includes('ivrMtls')
        && requirements[0].includes('ivrSignature');
      if (!ok) problems.push(`${key}: IVR operations require mTLS plus a request signature`);
      const parameters = operationParameters(root, operation, root.paths?.[route]);
      const headers = new Set(parameters.filter((parameter) => parameter.in === 'header').map((parameter) => parameter.name));
      if (!headers.has('X-ONECALL-TIMESTAMP') || !headers.has('X-ONECALL-NONCE')) {
        problems.push(`${key}: IVR operations require X-ONECALL-TIMESTAMP and X-ONECALL-NONCE headers`);
      }
    } else if (surface === 'public-share') {
      const allowed = requirements.every((keys) => keys.length === 0
        || (keys.length === 1 && keys[0] === 'customerSession'));
      if (!allowed) problems.push(`${key}: public-share security may only use the optional customerSession`);
      if (!route.includes('{capabilityToken}')) {
        problems.push(`${key}: public-share paths require the capabilityToken path parameter`);
      }
    } else if (surface === 'provider-callback') {
      const ok = requirements.length === 1
        && requirements[0].length === 2
        && requirements[0].includes('providerCallbackMtls')
        && requirements[0].includes('providerCallbackSignature');
      if (!ok) problems.push(`${key}: callback operations require provider mTLS plus a signature`);
    }
    const flat = requirements.flat();
    if (surface === 'operations' && flat.includes('customerSession')) {
      problems.push(`${key}: operations must not accept a customer session`);
    }
    if (surface === 'customer' && flat.includes('employeeSession')) {
      problems.push(`${key}: customer operations must not accept an employee session`);
    }
  });
  return problems;
}

function validateProblemComplete(root) {
  const problems = [];
  const problem = problemSchema(root);
  if (!problem) {
    return ['components.schemas.Problem is required'];
  }
  for (const field of ['type', 'title', 'status', 'code', 'traceId']) {
    if (!Array.isArray(problem.required) || !problem.required.includes(field)) {
      problems.push(`Problem.${field} must be required`);
    }
  }
  if (problem.properties?.code?.$ref !== '#/components/schemas/ProblemCode') {
    problems.push('Problem.code must reference ProblemCode');
  }
  const codes = problemCodeEnum(root);
  const codeSet = new Set(codes);
  const registryEntries = registry(root);
  const registryCodes = Object.keys(registryEntries);
  if (codes.length !== codeSet.size) problems.push('ProblemCode enum contains duplicates');
  if (registryCodes.length !== codeSet.size) {
    problems.push(`ProblemCode enum and registry sets differ: enum=${codes.length}, registry=${registryCodes.length}`);
  }
  for (const code of codes) {
    if (!registryCodes.includes(code)) problems.push(`Problem code ${code} is missing from the registry`);
    if (FORBIDDEN_PROBLEM_CODES.has(code)) problems.push(`IVR decision reason ${code} must not be a Problem code`);
  }
  for (const code of registryCodes) {
    if (!codeSet.has(code)) problems.push(`registry code ${code} is missing from ProblemCode`);
    const entry = registryEntries[code];
    if (!Number.isInteger(entry?.httpStatus) || entry.httpStatus < 400 || entry.httpStatus > 599) {
      problems.push(`registry code ${code}: httpStatus must be 400-599`);
    }
    if (!Array.isArray(entry?.surfaces) || entry.surfaces.some((surface) => !SURFACES.includes(surface))) {
      problems.push(`registry code ${code}: surfaces must be approved surfaces`);
    }
  }
  const referenced = new Set();
  eachOperation(root, (operation, route, method) => {
    const key = `${method.toUpperCase()} ${route}`;
    const errorCodes = extensionArray(operation, 'x-onecall-error-codes') ?? [];
    const statuses = responseStatuses(operation);
    const surface = operation['x-onecall-surface'];
    if (![...statuses].some((status) => /^4|^5|^default$/.test(status))) {
      problems.push(`${key}: at least one 4xx/5xx or default Problem response is required`);
    }
    for (const common of ['VALIDATION_FAILED', 'RATE_LIMITED', 'SERVICE_UNAVAILABLE']) {
      if (!errorCodes.includes(common)) {
        problems.push(`${key}: common Problem code ${common} is required`);
      }
    }
    for (const status of ['422', '429', '503']) {
      if (!statuses.has(status)) {
        problems.push(`${key}: explicit ${status} Problem response is required`);
      }
    }
    for (const code of errorCodes) {
      referenced.add(code);
      const entry = registryEntries[code];
      if (!entry) continue;
      if (!Array.isArray(entry.surfaces) || !entry.surfaces.includes(surface)) {
        problems.push(`${key}: code ${code} is not registered for surface ${surface}`);
      }
      const status = String(entry.httpStatus);
      // `default` satisfies only the INTERNAL 500 fallback. Every other listed
      // code must have an explicit response keyed at its registry httpStatus.
      if (code === 'INTERNAL') {
        if (!statuses.has('500') && !statuses.has('default')) {
          problems.push(`${key}: code INTERNAL requires a 500 or default response`);
        }
      } else if (!statuses.has(status)) {
        problems.push(`${key}: code ${code} requires an explicit ${status} response`);
      }
    }
  });
  for (const code of codes) {
    if (code !== 'INTERNAL' && !referenced.has(code)) {
      problems.push(`Problem code ${code} is not referenced by any operation`);
    }
  }
  const responses = root?.components?.responses ?? {};
  for (const [name, response] of Object.entries(responses)) {
    const content = response?.content?.['application/problem+json'];
    if (!content?.schema) {
      problems.push(`components.responses.${name} must use application/problem+json with a Problem schema`);
    }
  }
  // Exact per-operation domain Problem sets after excluding approved common
  // codes. Extras and omissions both fail: C4 is exactly the three CLAIM
  // conflict codes; O3/O4 are exactly NOT_FOUND plus ACTIVATION_STATE_CONFLICT.
  const exactDomainSets = {
    createCustomerSubjectClaim: ['CLAIM_RISK_BLOCKED', 'CLAIM_REPLAY_CONFLICT', 'CLAIM_CONTEXT_CONFLICT'],
    suspendOperationsSubject: ['NOT_FOUND', 'ACTIVATION_STATE_CONFLICT'],
    resumeOperationsSubject: ['NOT_FOUND', 'ACTIVATION_STATE_CONFLICT'],
    // S2: after excluding COMMON_PROBLEM_CODES, each listed operation's
    // x-onecall-error-codes set must equal exactly these domain codes.
    createCustomerOrder: [
      'IDEMPOTENCY_CONFLICT',
      'ORDER_UNKNOWN_PAYMENT_PENDING',
      'PURCHASE_CONSENT_REQUIRED',
      'PURCHASE_CONSENT_STALE',
      'PURCHASE_CONSENT_DECLINED',
      'ACTIVATION_NOT_ACTIVE',
      'REAL_NAME_REQUIRED',
    ],
    publishOperationsCatalogVersion: [
      'NOT_FOUND',
      'CATALOG_PUBLICATION_CONFLICT',
      'PUBLICATION_STALE_VIEW',
    ],
    offsaleOperationsCatalogVersion: [
      'NOT_FOUND',
      'CATALOG_PUBLICATION_CONFLICT',
      'PUBLICATION_STALE_VIEW',
    ],
    publishOperationsTrialPolicy: [
      'NOT_FOUND',
      'TRIAL_POLICY_PUBLICATION_CONFLICT',
      'TRIAL_POLICY_STALE_VIEW',
    ],
    disableOperationsTrialPolicy: [
      'NOT_FOUND',
      'TRIAL_POLICY_PUBLICATION_CONFLICT',
      'TRIAL_POLICY_STALE_VIEW',
    ],
    createOperationsCatalogVersionDraft: ['NOT_FOUND', 'CATALOG_DRAFT_CONFLICT'],
    replaceOperationsCatalogVersionDraft: [
      'NOT_FOUND',
      'CATALOG_DRAFT_CONFLICT',
      'CATALOG_DRAFT_STALE_VIEW',
    ],
    createOperationsTrialPolicyDraft: ['TRIAL_POLICY_DRAFT_CONFLICT'],
    replaceOperationsTrialPolicyDraft: [
      'NOT_FOUND',
      'TRIAL_POLICY_DRAFT_CONFLICT',
      'TRIAL_POLICY_DRAFT_STALE_VIEW',
    ],
  };
  eachOperation(root, (operation) => {
    const expected = exactDomainSets[operation.operationId];
    if (!expected) return;
    const actual = (extensionArray(operation, 'x-onecall-error-codes') ?? [])
      .filter((code) => !COMMON_PROBLEM_CODES.has(code));
    if (actual.length !== new Set(actual).size) {
      problems.push(`${operation.operationId}: duplicate Problem codes are forbidden`);
    }
    const actualSet = new Set(actual);
    const expectedSet = new Set(expected);
    for (const code of expected) {
      if (!actualSet.has(code)) {
        problems.push(`${operation.operationId}: exact Problem code ${code} is required`);
      }
    }
    for (const code of actualSet) {
      if (!expectedSet.has(code)) {
        problems.push(`${operation.operationId}: exact Problem code set forbids extra code ${code}`);
      }
    }
  });
  return problems;
}

function isPageSchemaName(name) {
  return typeof name === 'string' && /Page$/.test(name);
}

function validateCursorListings(root) {
  const problems = [];
  eachOperation(root, (operation, route, method, pathItem) => {
    const key = `${method.toUpperCase()} ${route}`;
    const schema = successSchema(root, operation);
    const refName = schema?.$ref ? schema.$ref.split('/').pop() : null;
    if (refName && isPageSchemaName(refName)) {
      const page = root?.components?.schemas?.[refName];
      const required = new Set(Array.isArray(page?.required) ? page.required : []);
      for (const field of ['items', 'nextCursor', 'hasMore']) {
        if (!required.has(field)) problems.push(`${key}: ${refName} must require ${field}`);
      }
      if (page?.properties?.items?.type !== 'array') {
        problems.push(`${key}: ${refName}.items must be an array`);
      }
      const parameters = operationParameters(root, operation, pathItem);
      const cursor = parameters.find((parameter) => parameter.in === 'query' && parameter.name === 'cursor');
      const limit = parameters.find((parameter) => parameter.in === 'query' && parameter.name === 'limit');
      if (!cursor) problems.push(`${key}: paginated listings require a cursor query parameter`);
      if (!limit) problems.push(`${key}: paginated listings require a limit query parameter`);
      const cursorSchema = cursor?.schema?.$ref
        ? deref(root, cursor.schema)
        : cursor?.schema;
      if (cursor && !(cursorSchema?.maxLength === 512)) {
        problems.push(`${key}: cursor schema must be the shared 1-512 opaque Cursor`);
      }
      const limitSchema = limit?.schema?.$ref ? deref(root, limit.schema) : limit?.schema;
      if (limit && !(limitSchema?.default === 20 && limitSchema?.maximum === 100)) {
        problems.push(`${key}: limit must use the V1 API-governance default 20 and maximum 100`);
      }
    }
    if (method === 'get' && operation['x-onecall-idempotency'] !== 'not-applicable') {
      problems.push(`${key}: read-only GET must use x-onecall-idempotency not-applicable`);
    }
    if (Array.isArray(operation['x-onecall-filter-digest'])) {
      const parameters = operationParameters(root, operation, pathItem);
      const names = new Set(parameters.filter((parameter) => parameter.in === 'query').map((parameter) => parameter.name));
      for (const filter of operation['x-onecall-filter-digest']) {
        if (!names.has(filter)) problems.push(`${key}: filter digest field ${filter} has no structured query parameter`);
      }
    }
    if (operation.operationId === 'listOperationsSubjects') {
      if (operation['x-onecall-sort'] !== 'createdAt desc, subjectId desc') {
        problems.push('listOperationsSubjects: sort must be exactly createdAt desc, subjectId desc');
      }
      const expected = ['subjectId', 'source', 'claimStatus', 'activationStatus', 'realNameStatus', 'riskState'];
      const actual = operation['x-onecall-filter-digest'];
      if (!Array.isArray(actual) || JSON.stringify(actual) !== JSON.stringify(expected)) {
        problems.push('listOperationsSubjects: filter digest must bind the six approved structured filters');
      }
      const parameters = operationParameters(root, operation, pathItem);
      const freeText = parameters.filter((parameter) => ['q', 'search', 'keyword', 'phone', 'number', 'fullNumber'].includes(parameter.name));
      if (freeText.length > 0) {
        problems.push('listOperationsSubjects: full-number and free-text search are forbidden');
      }
    }
    // S2 stable sort / filter-digest contracts (plan §4.8).
    const s2Sorts = new Map([
      ['listCustomerCatalogOffers', 'productCode asc, versionNo desc, catalogOfferId asc'],
      ['listCustomerOrders', 'createdAt desc, orderId desc'],
      ['listOperationsCatalogProducts', 'productCode asc, productId asc'],
      ['listOperationsTrialPolicies', 'versionNo desc, policyVersionId desc'],
      ['listOperationsOrders', 'createdAt desc, orderId desc'],
      ['listOperationsAuditFacts', 'occurredAt desc, auditId desc'],
    ]);
    const expectedSort = s2Sorts.get(operation.operationId);
    if (expectedSort && operation['x-onecall-sort'] !== expectedSort) {
      problems.push(`${operation.operationId}: sort must be exactly ${expectedSort}`);
    }
    const s2Digests = new Map([
      ['listCustomerCatalogOffers', ['channel', 'audience', 'offerType']],
      ['listOperationsOrders', ['subjectId', 'paymentStatus']],
    ]);
    const expectedDigest = s2Digests.get(operation.operationId);
    if (expectedDigest) {
      const digest = operation['x-onecall-filter-digest'];
      if (!Array.isArray(digest) || JSON.stringify(digest) !== JSON.stringify(expectedDigest)) {
        problems.push(`${operation.operationId}: filter digest must bind ${expectedDigest.join(',')}`);
      }
    }
  });
  const schemas = root?.components?.schemas ?? {};
  for (const [name, schema] of Object.entries(schemas)) {
    if (schema?.properties?.offset || schema?.properties?.total) {
      problems.push(`${name}: offset/total pagination fields are forbidden`);
    }
  }
  return problems;
}

// Resolve an operation-level requestBody through component $ref indirection.
// `declared` is false only when the operation has no requestBody at all.
// `body` is null for a declared body that cannot resolve to a plain object
// (missing target, non-object target, external/cyclic ref): callers must fail
// closed on that shape rather than silently skipping validation.
function resolveRequestBody(root, operation) {
  const raw = operation?.requestBody;
  if (raw === undefined || raw === null) return { declared: false, body: null };
  const body = typeof raw.$ref === 'string' ? deref(root, raw) : raw;
  if (!body || typeof body !== 'object' || typeof body.$ref === 'string') {
    return { declared: true, body: null };
  }
  return { declared: true, body };
}

// Shared resolved request-body schema path used by the trusted-fact walker,
// event-body idempotency checks and future draft forbidden-field checks, so a
// valid requestBody $ref cannot silently skip any of them.
function requestSchema(root, operation) {
  const { body } = resolveRequestBody(root, operation);
  if (!body) return null;
  const content = body.content;
  if (!content || typeof content !== 'object') return null;
  for (const media of Object.values(content)) {
    if (media?.schema) return deref(root, media.schema);
  }
  return null;
}

function validateIdempotencyWrites(root) {
  const problems = [];
  eachOperation(root, (operation, route, method, pathItem) => {
    const key = `${method.toUpperCase()} ${route}`;
    const value = operation['x-onecall-idempotency'];
    if (!IDEMPOTENCY_VALUES.has(value)) return;
    if (!WRITE_METHODS.has(method)) return;
    if (value === 'not-applicable') {
      problems.push(`${key}: write operations cannot use not-applicable idempotency`);
      return;
    }
    const parameters = operationParameters(root, operation, pathItem);
    const idempotencyHeader = parameters.find((parameter) => parameter.in === 'header' && parameter.name === 'Idempotency-Key');
    if (value === 'idempotency-key') {
      if (!idempotencyHeader) {
        problems.push(`${key}: idempotency-key operations require the Idempotency-Key header`);
      } else {
        if (idempotencyHeader.required !== true) problems.push(`${key}: Idempotency-Key must be required`);
        const expectedMax = IDEMPOTENCY_KEY_MAX_LENGTHS.get(operation.operationId);
        if (expectedMax === undefined) {
          problems.push(
            `${operation.operationId}: idempotency-key operation is absent from the Idempotency-Key maxLength contract map`,
          );
        } else if (idempotencyHeader.schema?.maxLength !== expectedMax) {
          problems.push(`${key}: Idempotency-Key must be bounded to ${expectedMax} characters for this action`);
        }
      }
    }
    if (value === 'event-body') {
      const schema = requestSchema(root, operation);
      const properties = schema?.properties ?? {};
      const hasEventIdentity = Object.keys(properties).some((name) => /EventId$/.test(name));
      if (!operation.requestBody?.required) problems.push(`${key}: event-body idempotency requires a required request body`);
      if (!hasEventIdentity) problems.push(`${key}: event-body idempotency requires a stable *EventId request field`);
      if (idempotencyHeader) problems.push(`${key}: event-body operations must not declare Idempotency-Key`);
    }
    if (value === 'state-conditional' && idempotencyHeader) {
      problems.push(`${key}: state-conditional operations must not declare Idempotency-Key`);
    }
    if (DRAFT_OPERATION_IDS.has(operation.operationId)) {
      if (value !== 'state-conditional') problems.push(`${operation.operationId}: must use state-conditional idempotency`);
      if (idempotencyHeader) problems.push(`${operation.operationId}: must not declare Idempotency-Key`);
      const models = extensionArray(operation, 'x-onecall-a0-models') ?? [];
      if (models.includes('platform.idempotency_record')) {
        problems.push(`${operation.operationId}: must not map platform.idempotency_record`);
      }
      const contracts = extensionArray(operation, 'x-onecall-a0-contracts') ?? [];
      if (contracts.some((contract) => /^platform\.idempotency_(begin|record)$/.test(contract))) {
        problems.push(`${operation.operationId}: must not reference platform idempotency functions`);
      }
      const schema = requestSchema(root, operation);
      for (const forbidden of ['status', 'publishedAt', 'publishedByEmployeeId']) {
        if (schema?.properties?.[forbidden]) {
          problems.push(`${operation.operationId}: request must not carry ${forbidden}`);
        }
      }
    }
  });
  return problems;
}

function walkSchema(schema, visitor, seen = new Set()) {
  if (!schema || typeof schema !== 'object') return;
  if (seen.has(schema)) return;
  seen.add(schema);
  visitor(schema);
  if (schema.properties) {
    for (const child of Object.values(schema.properties)) walkSchema(child, visitor, seen);
  }
  if (schema.items) walkSchema(schema.items, visitor, seen);
  for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
    if (Array.isArray(schema[keyword])) {
      for (const child of schema[keyword]) walkSchema(child, visitor, seen);
    }
  }
}

// Ref-resolving walker used for request-body trust-boundary checks. Each node
// is dereferenced before it is visited so a $ref to a nested object, array
// item or composed schema cannot hide a trusted fact. Local '#/...' chains
// are resolved cycle-safely without a silent depth cutoff; external, missing
// or cyclic request-schema refs fail closed with an explicit problem instead
// of being treated as a concrete schema. $ref siblings are rejected (not
// silently dropped) at every hop by resolveRequestSchemaRef (single owner of
// that check, so entry and intermediate aliases cannot double-report). Every
// traversed non-boolean request schema without a valid explicit type that
// definitively excludes objects is treated as object-admitting and must set
// additionalProperties false — a lightweight fail-closed contract rule that
// intentionally requires untyped schemas and composition wrappers to declare
// closure (it is not a full JSON Schema satisfiability check). Symmetrically
// (R6-01), every traversed non-boolean request schema without a valid explicit
// type that definitively excludes arrays is treated as array-admitting and
// must declare the `items` keyword by own-property presence (not truthiness,
// so `items: false` is an allowed closed/nothing form); `prefixItems` does
// not close the remaining implicit tail, so `items` is still required.
// Presence of a schema-valued `items` is traversed (`items: true` remains
// rejected by the boolean-true invariant above); dead `items`/`prefixItems`
// subschemas under a parent type that definitively excludes arrays are not
// traversed (the same dead-keyword principle applied to additionalProperties
// for non-object parents). Traversal covers properties, items, prefixItems,
// allOf/anyOf/oneOf and schema-valued additionalProperties, resolving
// component refs cycle-safely. Boolean schema `true` is rejected at every
// traversed position (it admits arbitrary data); boolean `false` is allowed
// as a closed/nothing schema, including under
// properties/items/prefixItems/additionalProperties. Any type array that
// contains `object` is object-admitting and must set additionalProperties
// false; any type array that contains `array` is array-admitting and must
// declare `items`.
function walkRequestSchema(root, schema, visitor, seen = new Set(), onProblem = null) {
  if (schema === true) {
    if (onProblem) onProblem('request schema boolean true is forbidden: it admits arbitrary data');
    return;
  }
  // Boolean false is a valid closed/nothing JSON Schema: nothing to walk.
  if (!schema || typeof schema !== 'object') return;
  let node = schema;
  if (typeof schema.$ref === 'string') {
    // F5-02: $ref-sibling rejection is owned by resolveRequestSchemaRef at
    // every hop (entry and intermediate alias), so no separate entry check
    // here — keeping one semantic owner avoids duplicate reporting.
    const resolved = resolveRequestSchemaRef(root, schema);
    if (resolved.problem) {
      if (onProblem) onProblem(resolved.problem);
      return;
    }
    node = resolved.node;
    if (node === true) {
      if (onProblem) onProblem('request schema boolean true is forbidden: it admits arbitrary data');
      return;
    }
    if (!node || typeof node !== 'object') return;
  }
  if (seen.has(node)) return;
  seen.add(node);
  visitor(node);
  // F5-01 strict request-contract closure: every traversed non-boolean request
  // schema that does not carry a valid explicit type definitively excluding
  // objects is treated as object-admitting and must set additionalProperties
  // false. This fail-closed rule intentionally requires untyped schemas and
  // composition wrappers (allOf/anyOf/oneOf envelopes with no explicit type,
  // `{}` terminals reached through a $ref chain, annotation-only objects) to
  // declare closure, so a permissive/untyped terminal cannot admit arbitrary
  // JSON (including server-derived trusted facts) while the closure test stays
  // silent. It is a lightweight contract rule, not a full JSON Schema
  // satisfiability check. A string type other than 'object', or a type array
  // without 'object', definitively excludes objects and suppresses the check;
  // a type array containing 'object' (for example ['object', 'null']) admits
  // objects and must set additionalProperties false. Boolean false remains an
  // allowed closed/nothing schema (handled above); boolean true remains
  // rejected above.
  const typeDefinitivelyExcludesObject = node.type !== undefined
    && (typeof node.type === 'string'
      ? node.type !== 'object'
      : (Array.isArray(node.type) ? !node.type.includes('object') : false));
  if (!typeDefinitivelyExcludesObject && node.additionalProperties !== false) {
    if (onProblem) {
      onProblem('request object schema must set additionalProperties false');
    }
  }
  // R6-01 strict request-array admission, parallel to the F5-01 object rule:
  // every traversed non-boolean request schema that does not carry a valid
  // explicit type definitively excluding arrays is treated as array-admitting
  // and must explicitly declare the `items` keyword. Presence uses
  // own-property semantics (not truthiness), so `items: false` is an allowed
  // closed/nothing form while an absent `items` means unconstrained elements
  // (and, under prefixItems, an unconstrained implicit tail) and fails closed.
  // A string type other than 'array', or a non-empty type array without
  // 'array', definitively excludes arrays and suppresses the check and the
  // items/prefixItems descent below (dead keywords for a non-array parent,
  // mirroring the additionalProperties dead-keyword guard). Absent or invalid
  // type declarations, empty type arrays and type arrays containing 'array'
  // are array-admitting and fail closed without an explicit `items` declaration.
  // A schema-valued `items` is still walked (boolean true stays rejected by
  // the invariant above; boolean false stays an allowed closed/nothing schema).
  // This is a lightweight fail-closed contract rule, not a full JSON Schema
  // satisfiability check; formal empty-type-array validation remains
  // aggregate-review scope.
  const typeDefinitivelyExcludesArray = node.type !== undefined
    && (typeof node.type === 'string'
      ? node.type !== 'array'
      : (Array.isArray(node.type)
        ? node.type.length > 0 && !node.type.includes('array')
        : false));
  if (!typeDefinitivelyExcludesArray && !Object.prototype.hasOwnProperty.call(node, 'items')) {
    if (onProblem) {
      onProblem('request array schema must declare items');
    }
  }
  if (node.properties && typeof node.properties === 'object') {
    for (const child of Object.values(node.properties)) {
      walkRequestSchema(root, child, visitor, seen, onProblem);
    }
  }
  if (!typeDefinitivelyExcludesArray && node.items) {
    walkRequestSchema(root, node.items, visitor, seen, onProblem);
  }
  if (!typeDefinitivelyExcludesArray && Array.isArray(node.prefixItems)) {
    for (const child of node.prefixItems) {
      walkRequestSchema(root, child, visitor, seen, onProblem);
    }
  }
  // Truthy walk covers both schema objects and boolean true (rejected above);
  // boolean false is skipped and stays an allowed closed/nothing schema. The
  // walk is skipped when the parent type definitively excludes objects, where
  // additionalProperties is dead contract surface (it cannot apply to any
  // non-object instance) — descending into a dead `{}`/schema subschema there
  // would raise a false closure/trusted-fact positive on ignored keywords.
  if (node.additionalProperties && !typeDefinitivelyExcludesObject) {
    walkRequestSchema(root, node.additionalProperties, visitor, seen, onProblem);
  }
  for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
    if (Array.isArray(node[keyword])) {
      for (const child of node[keyword]) {
        walkRequestSchema(root, child, visitor, seen, onProblem);
      }
    }
  }
}

function validateSensitiveFields(root) {
  const problems = [];
  const schemas = root?.components?.schemas ?? {};
  for (const [name, schema] of Object.entries(schemas)) {
    walkSchema(schema, (node) => {
      // F-04 closure: any type array admitting 'object' is object-admitting
      // and must set additionalProperties false. Only the approved opaque
      // provider/IVR exceptions (by schema name or x-onecall-opaque: true)
      // may remain open.
      const admitsObject = node.type === 'object'
        || (Array.isArray(node.type) && node.type.includes('object'));
      if (admitsObject && node.additionalProperties !== false) {
        const opaqueException = OPAQUE_CLOSURE_EXCEPTION_SCHEMAS.has(name)
          || node['x-onecall-opaque'] === true;
        if (!opaqueException) {
          problems.push(`${name}: object schemas must set additionalProperties false`);
        }
      }
      for (const property of Object.keys(node.properties ?? {})) {
        if (FORBIDDEN_PROPERTY_NAMES.has(property)) {
          problems.push(`${name}: forbidden sensitive property ${property}`);
        }
      }
    });
  }
  const rotation = schemas.ShareCodeRotation;
  if (rotation?.properties?.capabilityToken) {
    problems.push('ShareCodeRotation must never expose capabilityToken');
  }
  const readOnlyRules = {
    capabilityToken: ['x-onecall-return-once'],
    extractionCode: ['x-onecall-return-once'],
    credential: [],
  };
  for (const [name, schema] of Object.entries(schemas)) {
    walkSchema(schema, (node) => {
      for (const [property, requiredFlags] of Object.entries(readOnlyRules)) {
        const field = node.properties?.[property];
        if (!field) continue;
        if (field.readOnly !== true) {
          problems.push(`${name}.${property}: response secret/credential must be readOnly`);
        }
        if (field['x-onecall-log-redaction'] !== 'required') {
          problems.push(`${name}.${property}: response secret/credential must require log redaction`);
        }
        for (const flag of requiredFlags) {
          if (field[flag] !== true) {
            problems.push(`${name}.${property}: response secret must set ${flag}`);
          }
        }
        if (property === 'credential' && field['x-onecall-return-once'] !== undefined) {
          problems.push(`${name}.${property}: media/upload credentials must not claim return-once`);
        }
      }
    });
  }
  return problems;
}

// Request-body-only recursive trust-boundary guard. The operation requestBody
// is resolved through component $ref indirection (fail closed when it cannot
// resolve), must declare a non-empty content map, and every media entry must
// be an object with a schema. Every request media schema must be a pure
// component-schema $ref (no inline open objects, no $ref siblings). The walker
// rejects client-supplied server-derived facts across properties, items,
// prefixItems, compositions, schema-valued additional-properties and
// resolved/cyclic component refs. Response redaction rules above are
// intentionally separate.
function validateTrustedRequestFacts(root) {
  const problems = [];
  eachOperation(root, (operation, route, method) => {
    const key = `${method.toUpperCase()} ${route}`;
    const { declared, body } = resolveRequestBody(root, operation);
    if (!declared) return;
    if (!body) {
      problems.push(`${key}: requestBody must resolve to a request body object`);
      return;
    }
    // Only O18/O19 catalog draft create/replace may carry client draft
    // commercial priceCents and currency (plan §4.7/§5 O18/O19, docs/15 §6).
    // O20/O21 trial drafts and every other request reject both. Every other
    // trusted fact name stays banned on every request body.
    const draftCommercialAllowed = DRAFT_COMMERCIAL_OPERATION_IDS.has(operation.operationId);
    const content = body.content;
    if (!content || typeof content !== 'object' || Object.keys(content).length === 0) {
      problems.push(`${key}: request body must declare a non-empty content map`);
      return;
    }
    for (const [mediaType, media] of Object.entries(content)) {
      if (!media || typeof media !== 'object') {
        problems.push(`${key}: request body media entry ${mediaType} must be an object`);
        continue;
      }
      if (!media.schema) {
        problems.push(`${key}: request body media entry ${mediaType} must declare a schema`);
        continue;
      }
      const schema = media.schema;
      if (typeof schema.$ref !== 'string'
        || !schema.$ref.startsWith('#/components/schemas/')) {
        problems.push(`${key}: request body media schema must be a $ref to a component schema`);
      }
      walkRequestSchema(
        root,
        schema,
        (node) => {
          for (const property of Object.keys(node.properties ?? {})) {
            if (!TRUSTED_REQUEST_FACT_NAMES.has(property)) continue;
            if (draftCommercialAllowed && (property === 'priceCents' || property === 'currency')) continue;
            problems.push(`${key}: request must not accept trusted fact ${property}`);
          }
        },
        new Set(),
        (message) => problems.push(`${key}: ${message}`),
      );
    }
  });
  return problems;
}

function validateNoUnboundedList(root) {
  const problems = [];
  eachOperation(root, (operation, route, method) => {
    const schema = successSchema(root, operation);
    if (schema?.type === 'array') {
      problems.push(`${method.toUpperCase()} ${route}: top-level array responses must be wrapped by a *Page schema`);
    }
  });
  return problems;
}

function validateSecretDirection(root) {
  // The secret-direction contract is implemented through schema annotations.
  // No S1 operation returns a one-time secret or media credential, so this rule
  // currently proves the forbidden combinations are absent.
  const problems = [];
  const schemas = root?.components?.schemas ?? {};
  for (const [name, schema] of Object.entries(schemas)) {
    walkSchema(schema, (node) => {
      for (const [property, field] of Object.entries(node.properties ?? {})) {
        if ((property === 'capabilityToken' || property === 'extractionCode') && field.writeOnly === true) {
          problems.push(`${name}.${property}: response secrets must not be writeOnly`);
        }
      }
    });
  }
  if (schemas.ShareCreated) {
    for (const property of ['capabilityToken', 'extractionCode']) {
      const field = schemas.ShareCreated.properties?.[property];
      if (field && (field.readOnly !== true || field['x-onecall-return-once'] !== true)) {
        problems.push(`ShareCreated.${property}: one-time response secret annotation is required`);
      }
    }
  }
  return problems;
}

function validateIvrDecisionStatus(root) {
  const problems = [];
  eachOperation(root, (operation, route) => {
    if (operation['x-onecall-surface'] !== 'ivr') return;
    if (!IVR_DECISION_OPERATION_IDS.has(operation.operationId)) return;
    const schema = successSchema(root, operation);
    const refName = schema?.$ref ? schema.$ref.split('/').pop() : null;
    if (refName !== 'IvrDecision') {
      problems.push(`${operation.operationId}: normal IVR domain results must be 200 IvrDecision`);
    }
    const errorCodes = extensionArray(operation, 'x-onecall-error-codes') ?? [];
    for (const code of errorCodes) {
      if (FORBIDDEN_PROBLEM_CODES.has(code)) {
        problems.push(`${operation.operationId}: ${code} is an IVR decision reason, not a Problem code`);
      }
    }
  });
  return problems;
}

function validateShareSemantics(root) {
  const problems = [];
  eachOperation(root, (operation, route, method) => {
    if (operation['x-onecall-surface'] !== 'public-share') return;
    const id = operation.operationId;
    if (id === 'getPublicShare') {
      if (method !== 'get') problems.push('getPublicShare must be a GET');
      if (operation['x-onecall-idempotency'] !== 'not-applicable') {
        problems.push('getPublicShare must be observation-free and read-only');
      }
    }
    const codes = new Set(extensionArray(operation, 'x-onecall-error-codes') ?? []);
    if (id === 'createPublicShareAdmission' && !codes.has('SHARE_LOCKED')) {
      problems.push('createPublicShareAdmission must include SHARE_LOCKED');
    }
    if (id === 'authorizePublicShareMedia' && codes.has('SHARE_LOCKED')) {
      problems.push('authorizePublicShareMedia must not include SHARE_LOCKED');
    }
  });
  return problems;
}

// Natural-key / expectedRevision / content-projection shape for the four
// approved DRAFT candidate operations (plan §4.7/§5 O18-O21). Additive only:
// never weakens the existing state-conditional check.
const DRAFT_CREATE_REQUIRED_PROPERTIES = new Map([
  ['createOperationsCatalogVersionDraft', [
    'productId',
    'versionNo',
    'offerType',
    'audience',
    'priceCents',
    'currency',
    'billingCycle',
    'installments',
    'grantIntervalMonths',
    'benefitLines',
    'channels',
  ]],
  ['createOperationsTrialPolicyDraft', ['versionNo', 'grantedSeconds', 'validDays']],
]);
const DRAFT_REPLACE_REQUIRED_PROPERTIES = new Map([
  ['replaceOperationsCatalogVersionDraft', [
    'expectedRevision',
    'offerType',
    'audience',
    'priceCents',
    'currency',
    'billingCycle',
    'installments',
    'grantIntervalMonths',
  ]],
  ['replaceOperationsTrialPolicyDraft', ['expectedRevision', 'grantedSeconds', 'validDays']],
]);
const DRAFT_CREATE_FORBIDDEN_PROPERTIES = new Set([
  'status',
  'publishedAt',
  'publishedByEmployeeId',
  'enabled',
  'saleStatus',
]);
const DRAFT_REPLACE_FORBIDDEN_PROPERTIES = new Set([
  'status',
  'publishedAt',
  'publishedByEmployeeId',
  'enabled',
  'saleStatus',
  'productId',
  'versionNo',
  'campaignKey',
  'benefitLines',
  'channels',
]);

function validateDraftCandidates(root) {
  const problems = [];
  eachOperation(root, (operation) => {
    const id = operation.operationId;
    if (!DRAFT_OPERATION_IDS.has(id)) return;
    if (operation['x-onecall-idempotency'] !== 'state-conditional') {
      problems.push(`${id}: DRAFT candidates must be state-conditional`);
    }
    if (Array.isArray(operation.parameters)
      && operation.parameters.some((parameter) => {
        const resolved = typeof parameter?.$ref === 'string' ? null : parameter;
        return resolved?.in === 'header' && resolved?.name === 'Idempotency-Key'
          || (typeof parameter?.$ref === 'string' && parameter.$ref.endsWith('/IdempotencyKeyParam'));
      })) {
      problems.push(`${id}: DRAFT candidates must not declare Idempotency-Key`);
    }
    const schema = requestSchema(root, operation);
    if (!schema || typeof schema !== 'object') {
      problems.push(`${id}: DRAFT candidate request body must resolve to a component schema`);
      return;
    }
    const properties = schema.properties ?? {};
    const required = new Set(Array.isArray(schema.required) ? schema.required : []);
    const isCreate = id === 'createOperationsCatalogVersionDraft'
      || id === 'createOperationsTrialPolicyDraft';
    const expectedRequired = isCreate
      ? DRAFT_CREATE_REQUIRED_PROPERTIES.get(id)
      : DRAFT_REPLACE_REQUIRED_PROPERTIES.get(id);
    const forbidden = isCreate
      ? DRAFT_CREATE_FORBIDDEN_PROPERTIES
      : DRAFT_REPLACE_FORBIDDEN_PROPERTIES;
    for (const name of expectedRequired ?? []) {
      if (!required.has(name) || properties[name] === undefined) {
        problems.push(`${id}: DRAFT request must require ${name}`);
      }
    }
    if (!isCreate && properties.expectedRevision?.minimum !== 1) {
      problems.push(`${id}: expectedRevision must be an integer >= 1`);
    }
    for (const name of forbidden) {
      if (properties[name] !== undefined) {
        problems.push(`${id}: request must not carry ${name}`);
      }
    }
    if (id === 'createOperationsTrialPolicyDraft'
      && properties.campaignKey !== undefined) {
      problems.push(`${id}: campaignKey is server-fixed FIRST-TRIAL and must not be client-written`);
    }
  });
  return problems;
}

// S2 fix-shape assertions for controller-accepted findings F-01..F-06 plus the
// CNY freeze, full-document scope fail-closed and fix-2 draft-token removal
// clarifications. Additive only: never weakens existing rules.
function validateS2FixShapes(root) {
  const problems = [];
  const schemas = root?.components?.schemas ?? {};

  // F-01: OrderCreateRequest requires explicit ACCEPT consent fields; the
  // purchase consent event ID stays server-derived and must not be requestable.
  const orderCreate = schemas.OrderCreateRequest;
  if (!orderCreate) {
    problems.push('OrderCreateRequest is required');
  } else {
    const required = new Set(Array.isArray(orderCreate.required) ? orderCreate.required : []);
    const properties = orderCreate.properties ?? {};
    for (const field of ['subjectId', 'catalogOfferId', 'channel', 'decision', 'policyCode', 'policyVersion', 'promptVersion', 'contentSha256']) {
      if (!required.has(field) || properties[field] === undefined) {
        problems.push(`OrderCreateRequest: must require ${field}`);
      }
    }
    if (properties.decision?.enum?.length !== 1 || properties.decision?.enum?.[0] !== 'ACCEPT') {
      problems.push('OrderCreateRequest.decision must be fixed to enum [ACCEPT] only');
    }
    if (properties.purchaseConsentEventId !== undefined || properties.purchaseConsentId !== undefined) {
      problems.push('OrderCreateRequest must not accept purchaseConsentEventId/purchaseConsentId');
    }
    for (const field of ['policyCode', 'policyVersion', 'promptVersion']) {
      if (properties[field]?.minLength !== 1 || properties[field]?.maxLength !== 64) {
        problems.push(`OrderCreateRequest.${field} must be bounded 1-64 characters`);
      }
    }
    if (properties.contentSha256?.pattern !== '^[0-9a-f]{64}$') {
      problems.push('OrderCreateRequest.contentSha256 must match ^[0-9a-f]{64}$');
    }
  }

  // F-02: TrialPolicyDraft.enabled is a required boolean fixed to false;
  // publishedAt stays required null.
  const trialDraft = schemas.TrialPolicyDraft;
  if (!trialDraft) {
    problems.push('TrialPolicyDraft is required');
  } else {
    const required = new Set(Array.isArray(trialDraft.required) ? trialDraft.required : []);
    const enabled = trialDraft.properties?.enabled;
    if (!required.has('enabled')) problems.push('TrialPolicyDraft: enabled must be required');
    if (enabled?.type !== 'boolean' || enabled?.const !== false) {
      problems.push('TrialPolicyDraft.enabled must be a boolean fixed to const false');
    }
    if (!required.has('publishedAt') || trialDraft.properties?.publishedAt?.type !== 'null') {
      problems.push('TrialPolicyDraft.publishedAt must remain required type null');
    }
  }

  // F-03: publish/offsale/disable request shapes require the correct nullable
  // expected identity plus expectedRevision.
  const publishOffsaleShapes = [
    ['CatalogVersionPublicationRequest', 'expectedActiveVersionId'],
    ['CatalogVersionOffsaleRequest', null],
    ['TrialPolicyPublicationRequest', 'expectedEnabledPolicyVersionId'],
    ['TrialPolicyDisableRequest', null],
  ];
  for (const [schemaName, identityField] of publishOffsaleShapes) {
    const schema = schemas[schemaName];
    if (!schema) {
      problems.push(`${schemaName} is required`);
      continue;
    }
    const required = new Set(Array.isArray(schema.required) ? schema.required : []);
    const properties = schema.properties ?? {};
    if (identityField) {
      if (!required.has(identityField) || properties[identityField] === undefined) {
        problems.push(`${schemaName}: must require ${identityField}`);
      } else {
        const field = properties[identityField];
        const nullable = field?.type === 'null'
          || (Array.isArray(field?.type) && field.type.includes('null') && field.type.includes('string'));
        if (!nullable) {
          problems.push(`${schemaName}.${identityField} must be nullable (explicit null for none)`);
        }
      }
    } else if (required.has('expectedActiveVersionId') || required.has('expectedEnabledPolicyVersionId')) {
      problems.push(`${schemaName}: must not require an unexpected expected identity field`);
    }
    if (!required.has('expectedRevision') || properties.expectedRevision?.minimum !== 1) {
      problems.push(`${schemaName}: must require expectedRevision integer >= 1`);
    }
  }

  // F-04: closed reusable RedactedAuditSnapshot with only bounded
  // status/revision/reasonCode; AuditFact snapshots ref it (nullable).
  const snapshot = schemas.RedactedAuditSnapshot;
  if (!snapshot) {
    problems.push('RedactedAuditSnapshot is required');
  } else {
    if (snapshot.type !== 'object' || snapshot.additionalProperties !== false) {
      problems.push('RedactedAuditSnapshot must be a closed object');
    }
    if (snapshot.minProperties !== 1) {
      problems.push('RedactedAuditSnapshot must set minProperties 1');
    }
    const keys = new Set(Object.keys(snapshot.properties ?? {}));
    for (const field of ['status', 'revision', 'reasonCode']) {
      if (!keys.has(field)) problems.push(`RedactedAuditSnapshot: must declare ${field}`);
    }
    for (const key of keys) {
      if (!['status', 'revision', 'reasonCode'].includes(key)) {
        problems.push(`RedactedAuditSnapshot: unexpected property ${key}`);
      }
    }
    for (const field of ['status', 'reasonCode']) {
      const node = snapshot.properties?.[field];
      if (node?.type !== 'string' || node?.minLength !== 1 || node?.maxLength !== 64) {
        problems.push(`RedactedAuditSnapshot.${field} must be a bounded string (1-64)`);
      }
    }
    const revision = snapshot.properties?.revision;
    if (revision?.type !== 'integer' || revision?.minimum !== 0) {
      problems.push('RedactedAuditSnapshot.revision must be a nonnegative integer');
    }
    if (Array.isArray(snapshot.required) && snapshot.required.length > 0) {
      problems.push('RedactedAuditSnapshot properties must all be optional');
    }
  }
  const auditFact = schemas.AuditFact;
  if (!auditFact) {
    problems.push('AuditFact is required');
  } else {
    for (const field of ['beforeSnapshot', 'afterSnapshot']) {
      const node = auditFact.properties?.[field];
      const refs = Array.isArray(node?.oneOf)
        ? node.oneOf.map((entry) => entry?.$ref ?? null)
        : [];
      if (!refs.includes('#/components/schemas/RedactedAuditSnapshot')) {
        problems.push(`AuditFact.${field} must $ref RedactedAuditSnapshot`);
      }
      const nullBranch = Array.isArray(node?.oneOf)
        ? node.oneOf.some((entry) => entry?.type === 'null')
        : false;
      if (!nullBranch) {
        problems.push(`AuditFact.${field} must admit an explicit null branch`);
      }
      if (node?.additionalProperties === true) {
        problems.push(`AuditFact.${field} must not set additionalProperties true`);
      }
    }
  }

  // F-06: CatalogProductSummary requires a bounded versions projection.
  const productSummary = schemas.CatalogProductSummary;
  const versionSummary = schemas.CatalogProductVersionSummary;
  if (!versionSummary) {
    problems.push('CatalogProductVersionSummary is required');
  } else {
    if (versionSummary.type !== 'object' || versionSummary.additionalProperties !== false) {
      problems.push('CatalogProductVersionSummary must be a closed object');
    }
    const required = new Set(Array.isArray(versionSummary.required) ? versionSummary.required : []);
    const keys = new Set(Object.keys(versionSummary.properties ?? {}));
    const expectedKeys = ['catalogVersionId', 'versionNo', 'saleStatus', 'offerType', 'audience'];
    for (const field of expectedKeys) {
      if (!required.has(field) || !keys.has(field)) {
        problems.push(`CatalogProductVersionSummary: must require ${field}`);
      }
    }
    for (const key of keys) {
      if (!expectedKeys.includes(key)) {
        problems.push(`CatalogProductVersionSummary: unexpected property ${key}`);
      }
    }
  }
  if (!productSummary) {
    problems.push('CatalogProductSummary is required');
  } else {
    const required = new Set(Array.isArray(productSummary.required) ? productSummary.required : []);
    const versions = productSummary.properties?.versions;
    if (!required.has('versions') || versions?.type !== 'array') {
      problems.push('CatalogProductSummary: must require a versions array');
    } else if (versions?.items?.$ref !== '#/components/schemas/CatalogProductVersionSummary') {
      problems.push('CatalogProductSummary.versions items must $ref CatalogProductVersionSummary');
    }
  }

  // S2 fix 2: the ambiguous singular CatalogVersionDraft.versionToken is
  // removed from the public schema (no A0 version-level owner and no channel
  // association; O19 channel tokens are regenerated server-side and never
  // public input/output). The required CatalogOffer.versionToken stays
  // because C9 offer selection needs it.
  const catalogDraft = schemas.CatalogVersionDraft;
  if (!catalogDraft) {
    problems.push('CatalogVersionDraft is required');
  } else {
    if ((catalogDraft.properties ?? {}).versionToken !== undefined) {
      problems.push('CatalogVersionDraft must not declare versionToken');
    }
    const draftRequired = new Set(Array.isArray(catalogDraft.required) ? catalogDraft.required : []);
    if (draftRequired.has('versionToken')) {
      problems.push('CatalogVersionDraft must not require versionToken');
    }
  }
  const catalogOffer = schemas.CatalogOffer;
  if (!catalogOffer) {
    problems.push('CatalogOffer is required');
  } else {
    const offerToken = (catalogOffer.properties ?? {}).versionToken;
    if (offerToken?.type !== 'string' || offerToken?.minLength !== 1 || offerToken?.maxLength !== 128) {
      problems.push('CatalogOffer.versionToken must remain a bounded string (1-128)');
    }
    const offerRequired = new Set(Array.isArray(catalogOffer.required) ? catalogOffer.required : []);
    if (!offerRequired.has('versionToken')) {
      problems.push('CatalogOffer must keep required versionToken for C9 offer selection');
    }
  }

  // Currency freeze: every public v1 monetary currency property is literal CNY.
  for (const [name, schema] of Object.entries(schemas)) {
    walkSchema(schema, (node) => {
      const currency = node.properties?.currency;
      if (!currency || currency.type !== 'string') return;
      if (!Array.isArray(currency.enum) || currency.enum.length !== 1 || currency.enum[0] !== 'CNY') {
        problems.push(`${name}.currency must freeze to literal enum [CNY]`);
      }
    });
  }

  return problems;
}

function makeRule(validator) {
  return function onecallRule() {
    return {
      Root(root, { report, location }) {
        let messages;
        try {
          messages = validator(root);
        } catch (error) {
          messages = [`rule evaluation failed: ${error.message}`];
        }
        for (const message of messages) {
          report({ message, location });
        }
      },
    };
  };
}

export default function onecallContractRules() {
  return {
    id: 'onecall',
    rules: {
      oas3: {
        'operation-metadata': makeRule(validateOperationMetadata),
        'security-by-surface': makeRule(validateSecurityBySurface),
        'problem-complete': makeRule(validateProblemComplete),
        'cursor-listings': makeRule(validateCursorListings),
        'idempotency-writes': makeRule(validateIdempotencyWrites),
        'sensitive-fields': makeRule((root) => [
          ...validateSensitiveFields(root),
          ...validateTrustedRequestFacts(root),
          ...validateS2FixShapes(root),
        ]),
        'server-relative': makeRule(validateServerRelative),
        'versioned-paths': makeRule(validateVersionedPaths),
        tagged: makeRule(validateTagged),
        'no-unbounded-list': makeRule(validateNoUnboundedList),
        'secret-direction': makeRule(validateSecretDirection),
        'ivr-decision-status': makeRule(validateIvrDecisionStatus),
        'share-semantics': makeRule(validateShareSemantics),
        'draft-candidates': makeRule(validateDraftCandidates),
        's2-fix-shapes': makeRule(validateS2FixShapes),
        'scope-extensions': makeRule(validateNoScopeExtensions),
      },
    },
  };
}
