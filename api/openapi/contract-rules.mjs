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
// (provider opaque payloads / IVR event payloads, plan §4.11-§4.12). Opaque
// opt-in via x-onecall-opaque: true is fail-closed: only these two named
// components may carry the annotation; any other occurrence is rejected.
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
  ['revokeCustomerShare', 128],
  ['rotateCustomerShareCode', 128],
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
  // F-10: section anchors are an explicit approved ownership set, not every
  // §N.N token in docs/14. Each approved section is verified to exist in the
  // A0 text so a renamed/removed section fails closed instead of silently
  // validating. §7.2 is deliberately absent (not a cited A1 contract anchor).
  const APPROVED_A0_SECTION_ANCHORS = [
    '§7.1',
    '§7.3',
    '§7.4',
    '§9.18',
    '§9.24',
    '§9.27',
    '§9.28',
    '§10.14',
    '§10.17',
  ];
  for (const section of APPROVED_A0_SECTION_ANCHORS) {
    // A0 owns a section through Markdown headings such as "### 7.1 S1 state
    // machines", not necessarily a literal "§7.1" token. Validate each approved
    // anchor against an exact heading number so a renamed/removed section fails
    // closed, while a same-numbered heading anywhere still satisfies the check.
    const sectionNumber = section.slice(1);
    const escapedNumber = sectionNumber.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const headingPattern = new RegExp(`^#{1,6}\\s+${escapedNumber}(?:\\s|$)`, 'm');
    if (!headingPattern.test(text)) {
      throw new Error(`docs/14 is missing approved A0 section anchor ${section}`);
    }
    anchors.add(section);
  }
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
    // S3: customer outbound-call tasks (C6-C8), shares (C15-C18),
    // IVR (I1-I6), public-share (P1-P3) and payment callback (K1).
    createCustomerOutboundCallTask: [
      'ACTIVATION_NOT_ACTIVE',
      'OUTBOUND_CALL_IN_PROGRESS',
      'OUTBOUND_TASK_CONFLICT',
    ],
    getCustomerOutboundCallTask: ['NOT_FOUND'],
    cancelCustomerOutboundCallTask: [
      'NOT_FOUND',
      'OUTBOUND_TASK_NOT_CANCELLABLE',
      'OUTBOUND_TASK_CONFLICT',
    ],
    createCustomerShare: [
      'SHARE_CREATION_CONFLICT',
      'SHARE_ACCESS_POLICY_UNAVAILABLE',
      'SHARE_RISK_BLOCKED',
    ],
    listCustomerShares: ['CURSOR_INVALID'],
    revokeCustomerShare: [
      'NOT_FOUND',
      'SHARE_TRANSITION_CONFLICT',
      'SHARE_GENERATION_STALE',
      'SHARE_REVOKED',
    ],
    rotateCustomerShareCode: ['NOT_FOUND', 'SHARE_TRANSITION_CONFLICT'],
    resolveIvrInbound: ['IVR_ARGUMENT_INVALID', 'SESSION_IDENTITY_MISMATCH'],
    confirmIvrActivation: ['IVR_ARGUMENT_INVALID', 'SESSION_IDENTITY_MISMATCH'],
    authorizeIvrDirectOutbound: ['IVR_ARGUMENT_INVALID', 'SESSION_IDENTITY_MISMATCH'],
    ingestIvrCallEvent: ['IVR_ARGUMENT_INVALID', 'CALL_UNKNOWN', 'EVENT_ID_CONFLICT'],
    declareIvrRecording: ['IVR_ARGUMENT_INVALID', 'CALL_UNKNOWN', 'RECORDING_IDENTITY_CONFLICT'],
    completeIvrRecording: [
      'IVR_ARGUMENT_INVALID',
      'CALL_UNKNOWN',
      'UPLOAD_OBSERVATION_REQUIRED',
      'RECORDING_IDENTITY_CONFLICT',
      'UPLOAD_EXPIRED',
      'STORAGE_UNKNOWN',
    ],
    getPublicShare: ['SHARE_TOKEN_INVALID', 'SHARE_REVOKED', 'SHARE_EXPIRED'],
    createPublicShareAdmission: [
      'SHARE_VERIFICATION_FAILED',
      'SHARE_RISK_BLOCKED',
      'SHARE_WRONG_TENANT',
      'SHARE_TOKEN_INVALID',
      'SHARE_REVOKED',
      'SHARE_EXPIRED',
      'SHARE_LOCKED',
      'SHARE_ADMISSION_LIMIT_REACHED',
      'SHARE_GENERATION_STALE',
      'SHARE_SESSION_EXPIRED',
      'SHARE_SESSION_POLICY_UNAVAILABLE',
      'DEPENDENCY_UNAVAILABLE',
    ],
    authorizePublicShareMedia: [
      'SHARE_RISK_BLOCKED',
      'SHARE_WRONG_TENANT',
      'SHARE_TOKEN_INVALID',
      'SHARE_REVOKED',
      'SHARE_EXPIRED',
      'SHARE_GENERATION_STALE',
      'SHARE_SESSION_EXPIRED',
      'SHARE_SESSION_POLICY_UNAVAILABLE',
      'DEPENDENCY_UNAVAILABLE',
    ],
    receivePaymentProviderCallback: [
      'CALLBACK_SIGNATURE_INVALID',
      'PAYMENT_EVENT_CONFLICT',
      'CALLBACK_RETRY_LATER',
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
    // S2/S3 stable sort / filter-digest contracts (plan §4.8 / S3).
    const s2Sorts = new Map([
      ['listCustomerCatalogOffers', 'productCode asc, versionNo desc, catalogOfferId asc'],
      ['listCustomerOrders', 'createdAt desc, orderId desc'],
      ['listOperationsCatalogProducts', 'productCode asc, productId asc'],
      ['listOperationsTrialPolicies', 'versionNo desc, policyVersionId desc'],
      ['listOperationsOrders', 'createdAt desc, orderId desc'],
      ['listOperationsAuditFacts', 'occurredAt desc, auditId desc'],
      ['listCustomerShares', 'validFrom desc, shareId desc'],
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
      // Plan mandates the body field named `eventId` (camelCase) for I2/I3/I4;
      // keep matching PascalCase *EventId aliases as well.
      const hasEventIdentity = Object.keys(properties).some((name) => /[eE]ventId$/.test(name));
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
  // F-10: opaque closure exemptions are restricted to the two named component
  // schemas (OpaqueProviderPayload / IvrEventPayload). The exemption is by
  // node identity with the approved component, not by annotation alone. Any
  // other node carrying x-onecall-opaque: true — including inline schemas —
  // fails closed with an explicit problem.
  const opaqueComponentSchemas = root?.components?.schemas ?? {};
  const opaqueRequestException = node === opaqueComponentSchemas.OpaqueProviderPayload
    || node === opaqueComponentSchemas.IvrEventPayload;
  if (node['x-onecall-opaque'] === true && !opaqueRequestException) {
    if (onProblem) {
      onProblem('x-onecall-opaque is approved only on OpaqueProviderPayload and IvrEventPayload components');
    }
  }
  const typeDefinitivelyExcludesObject = node.type !== undefined
    && (typeof node.type === 'string'
      ? node.type !== 'object'
      : (Array.isArray(node.type) ? !node.type.includes('object') : false));
  if (!opaqueRequestException && !typeDefinitivelyExcludesObject && node.additionalProperties !== false) {
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
  if (!opaqueRequestException && !typeDefinitivelyExcludesArray && !Object.prototype.hasOwnProperty.call(node, 'items')) {
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
  // The approved opaque component nodes deliberately declare
  // `additionalProperties: true` as their approval contract; that exact boolean
  // declaration is not an ordinary schema-valued traversal position, so it is
  // not re-walked into the generic boolean-true rejection. Direct boolean true
  // schemas and every non-approved additionalProperties position still fail.
  const approvedOpaqueOpen = opaqueRequestException && node.additionalProperties === true;
  if (node.additionalProperties && !typeDefinitivelyExcludesObject && !approvedOpaqueOpen) {
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
      // F-04/F-10 closure: any type array admitting 'object' is object-admitting
      // and must set additionalProperties false. Only the two named opaque
      // provider/IVR component schemas may remain open; the exemption is
      // name-based (not annotation-based). Any other schema carrying
      // x-onecall-opaque: true fails closed with an explicit problem so an
      // unapproved annotation cannot bypass closure.
      const admitsObject = node.type === 'object'
        || (Array.isArray(node.type) && node.type.includes('object'));
      if (node['x-onecall-opaque'] === true && !OPAQUE_CLOSURE_EXCEPTION_SCHEMAS.has(name)) {
        problems.push(`${name}: x-onecall-opaque is approved only on OpaqueProviderPayload and IvrEventPayload`);
      }
      if (admitsObject && node.additionalProperties !== false) {
        const opaqueException = OPAQUE_CLOSURE_EXCEPTION_SCHEMAS.has(name);
        if (!opaqueException) {
          problems.push(`${name}: object schemas must set additionalProperties false`);
        }
      }
      for (const [property, field] of Object.entries(node.properties ?? {})) {
        if (!FORBIDDEN_PROPERTY_NAMES.has(property)) continue;
        // IvrDecision.task.calleeNumber / authorization.calleeNumber are the
        // only approved internal-payload exceptions (plan §6.1): response-only
        // readOnly fields marked x-onecall-internal-payload with mandatory log
        // redaction. Requests still reject these names via trusted-fact rules.
        const internalPayloadException = field?.readOnly === true
          && field?.['x-onecall-internal-payload'] === true
          && field?.['x-onecall-log-redaction'] === 'required';
        if (!internalPayloadException) {
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
  // F-02: ShareCreated secret-direction validation must actually inspect
  // capabilityToken/extractionCode after ref/composition shape. A resolving
  // property collector walks $ref chains and allOf/oneOf/anyOf compositions so
  // a composed ShareCreated cannot hide its secrets from this check.
  if (schemas.ShareCreated) {
    const collected = collectResolvedProperties(root, schemas.ShareCreated, new Set());
    for (const property of ['capabilityToken', 'extractionCode']) {
      const field = collected.get(property);
      if (field && (field.readOnly !== true || field['x-onecall-return-once'] !== true)) {
        problems.push(`ShareCreated.${property}: one-time response secret annotation is required`);
      }
    }
  }
  return problems;
}

// F-02 helper: collect object properties from a schema, resolving $ref chains
// and walking allOf/anyOf/oneOf compositions. Later duplicate property names
// overwrite earlier ones (branch-local secrets win). Returns Map<name, field>.
function collectResolvedProperties(root, schema, seen = new Set()) {
  const collected = new Map();
  if (!schema || typeof schema !== 'object') return collected;
  if (typeof schema.$ref === 'string') {
    if (seen.has(schema.$ref)) return collected;
    seen.add(schema.$ref);
    let target = root;
    for (const part of schema.$ref.slice(2).split('/')) {
      const key = part.replace(/~1/g, '/').replace(/~0/g, '~');
      target = target?.[key];
    }
    if (target && typeof target === 'object') {
      for (const [name, field] of collectResolvedProperties(root, target, seen)) {
        collected.set(name, field);
      }
    }
    return collected;
  }
  for (const [name, field] of Object.entries(schema.properties ?? {})) {
    collected.set(name, field);
  }
  for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
    if (Array.isArray(schema[keyword])) {
      for (const branch of schema[keyword]) {
        for (const [name, field] of collectResolvedProperties(root, branch, seen)) {
          collected.set(name, field);
        }
      }
    }
  }
  return collected;
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
  // P1: PublicShareView.content and verificationChallenge stay optional so an
  // unauthenticated ORGANIZATION_MEMBER page can omit content (plan §5.4).
  const view = root?.components?.schemas?.PublicShareView;
  if (view) {
    const required = new Set(Array.isArray(view.required) ? view.required : []);
    if (required.has('content')) {
      problems.push('PublicShareView.content must remain optional');
    }
    if (required.has('verificationChallenge')) {
      problems.push('PublicShareView.verificationChallenge must remain optional');
    }
  }
  // ShareCodeRotation must never carry capabilityToken (schema-level, in
  // addition to the response walker assertion above).
  const rotation = root?.components?.schemas?.ShareCodeRotation;
  if (rotation?.properties?.capabilityToken) {
    problems.push('ShareCodeRotation must never expose capabilityToken');
  }
  // P2 additive S3 assertion (never a relaxation of F5-01/R6-01):
  // ShareAdmissionRequest.verification must stay an object-only closed
  // discriminated union over exactly the four approved verification modes.
  // The wrapper must declare `type: object` (definitively non-array) and
  // `additionalProperties: false` (closed to unbounded objects); the oneOf
  // must keep exactly the four ShareVerification* branches with a `mode`
  // discriminator so branch-specific secrets (the writeOnly extraction
  // `code`) stay confined to their own mode.
  const admissionRequest = root?.components?.schemas?.ShareAdmissionRequest;
  const verification = admissionRequest?.properties?.verification;
  if (!verification) {
    problems.push('ShareAdmissionRequest.verification is required');
  } else {
    if (verification.type !== 'object') {
      problems.push('ShareAdmissionRequest.verification must be object-only');
    }
    if (verification.additionalProperties !== false) {
      problems.push('ShareAdmissionRequest.verification must set additionalProperties false');
    }
    const expectedVerificationBranches = [
      '#/components/schemas/ShareVerificationNone',
      '#/components/schemas/ShareVerificationExtractionCode',
      '#/components/schemas/ShareVerificationDesignatedMobile',
      '#/components/schemas/ShareVerificationOrganizationMember',
    ];
    const verificationRefs = Array.isArray(verification.oneOf)
      ? verification.oneOf.map((entry) => (entry && entry.$ref) || null)
      : [];
    if (verificationRefs.length !== expectedVerificationBranches.length
      || expectedVerificationBranches.some((branch) => !verificationRefs.includes(branch))) {
      problems.push('ShareAdmissionRequest.verification must keep exactly the four approved mode branches');
    }
    if (verification.discriminator?.propertyName !== 'mode') {
      problems.push('ShareAdmissionRequest.verification must use a mode discriminator');
    }
    // R-01: the discriminator must carry an explicit canonical mapping so the
    // generated union uses wire literals (NONE/EXTRACTION_CODE/
    // DESIGNATED_MOBILE/ORGANIZATION_MEMBER), not component names.
    const verificationMapping = verification.discriminator?.mapping;
    const expectedVerificationMapping = {
      NONE: '#/components/schemas/ShareVerificationNone',
      EXTRACTION_CODE: '#/components/schemas/ShareVerificationExtractionCode',
      DESIGNATED_MOBILE: '#/components/schemas/ShareVerificationDesignatedMobile',
      ORGANIZATION_MEMBER: '#/components/schemas/ShareVerificationOrganizationMember',
    };
    if (!verificationMapping
      || typeof verificationMapping !== 'object'
      || Object.keys(verificationMapping).length !== Object.keys(expectedVerificationMapping).length
      || Object.entries(expectedVerificationMapping).some(([value, ref]) => verificationMapping[value] !== ref)) {
      problems.push('ShareAdmissionRequest.verification discriminator.mapping must map the four canonical verification modes');
    }
  }
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

// ---------------------------------------------------------------------------
// S3 fix validators (F-01..F-11). Chained into the enabled sensitive-fields
// rule so they run under redocly.yaml without adding a new named rule.
// ---------------------------------------------------------------------------

// F-01: opaque A0 protocol identities (taskId/callId/recordingId/shareId) map
// non-UUID bounded strings, not internal UUIDs. Internal decisionId/sessionId/
// reservationId/evidenceId/admissionId and true internal resources stay UUID.
const OPAQUE_ID_BOUNDS = new Map([
  ['taskId', 64],
  ['callId', 128],
  ['recordingId', 128],
  ['shareId', 128],
]);

function assertOpaqueIdField(problems, label, field, maxLen) {
  if (!field || field.type !== 'string') {
    problems.push(`${label}: must be type string`);
    return;
  }
  if (field.format === 'uuid') {
    problems.push(`${label}: must not use format uuid (opaque A0 protocol identity)`);
  }
  if (field.minLength !== 1) {
    problems.push(`${label}: must set minLength 1`);
  }
  if (field.maxLength !== maxLen) {
    problems.push(`${label}: must set maxLength ${maxLen}`);
  }
}

// OQ-3 (F-01 regression): a deterministic occurrence inventory of every
// external taskId/callId/recordingId/shareId occurrence. The walk covers
// operation path AND query parameters plus every component-schema property at
// any nesting depth (inline objects, array items, composition subschemas);
// $ref targets are not re-walked here so a shared component is counted once.
// The exact per-name counts below fail closed when an expected occurrence
// disappears, changes name or a new unreviewed occurrence appears.
const EXPECTED_OPAQUE_ID_OCCURRENCE_COUNTS = new Map([
  // 2 schema (OutboundCallTask.taskId, IvrDecision.task.taskId) + 3 params
  // (C7 path, C8 path, listOperationsIvrSessions query).
  ['taskId', 5],
  // 5 schema (IvrSessionSummary, IvrSessionDetail, IvrDecision,
  // IvrInboundResolveRequest, CallEventReceipt) + 6 params (I2-I6 path,
  // listOperationsIvrSessions query).
  ['callId', 11],
  // 5 schema (ShareState, ShareSummary, ShareCreated, ShareCodeRotationRotated,
  // ShareCodeRotationReplayed) + 2 params (C17 path, C18 path).
  ['shareId', 7],
  // 3 schema (RecordingDeclarationRequest, RecordingDeclaration,
  // RecordingCompletion) + 1 param (I6 path).
  ['recordingId', 4],
]);

function collectOpaqueIdOccurrences(root) {
  const occurrences = [];
  const paths = root?.paths ?? {};
  for (const [, pathItem] of Object.entries(paths)) {
    if (!pathItem || typeof pathItem !== 'object') continue;
    for (const [method, operation] of Object.entries(pathItem)) {
      if (!operation || typeof operation !== 'object' || !operation.operationId) continue;
      const rawParams = [
        ...(Array.isArray(pathItem.parameters) ? pathItem.parameters : []),
        ...(Array.isArray(operation.parameters) ? operation.parameters : []),
      ];
      for (const rawParam of rawParams) {
        const param = deref(root, rawParam);
        if (!param || typeof param.name !== 'string') continue;
        const maxLen = OPAQUE_ID_BOUNDS.get(param.name);
        if (maxLen === undefined) continue;
        occurrences.push({
          name: param.name,
          label: `${method.toUpperCase()} ${operation.operationId} ${param.in ?? 'unknown'} param ${param.name}`,
          field: param.schema,
          maxLen,
        });
      }
    }
  }
  const schemas = root?.components?.schemas ?? {};
  const walk = (node, pathLabel) => {
    if (!node || typeof node !== 'object') return;
    if (node.$ref) return; // counted at the target component, not through the alias
    if (node.properties && typeof node.properties === 'object') {
      for (const [propName, propSchema] of Object.entries(node.properties)) {
        const childLabel = pathLabel ? `${pathLabel}.${propName}` : propName;
        const maxLen = OPAQUE_ID_BOUNDS.get(propName);
        if (maxLen !== undefined && propSchema && typeof propSchema === 'object') {
          occurrences.push({ name: propName, label: childLabel, field: propSchema, maxLen });
        }
        walk(propSchema, childLabel);
      }
    }
    if (node.items) walk(node.items, `${pathLabel}[]`);
    if (Array.isArray(node.prefixItems)) {
      node.prefixItems.forEach((child, index) => walk(child, `${pathLabel}[${index}]`));
    }
    if (node.additionalProperties && typeof node.additionalProperties === 'object') {
      walk(node.additionalProperties, `${pathLabel}.*`);
    }
    for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
      if (Array.isArray(node[keyword])) {
        node[keyword].forEach((child, index) => walk(child, `${pathLabel}.${keyword}[${index}]`));
      }
    }
  };
  for (const [schemaName, schema] of Object.entries(schemas)) {
    walk(schema, schemaName);
  }
  return occurrences;
}

function validateOpaqueIdOccurrences(root) {
  const problems = [];
  const occurrences = collectOpaqueIdOccurrences(root);
  const counts = new Map();
  for (const occurrence of occurrences) {
    counts.set(occurrence.name, (counts.get(occurrence.name) ?? 0) + 1);
    assertOpaqueIdField(problems, occurrence.label, occurrence.field, occurrence.maxLen);
  }
  for (const [name, expected] of EXPECTED_OPAQUE_ID_OCCURRENCE_COUNTS) {
    const found = counts.get(name) ?? 0;
    if (found !== expected) {
      problems.push(`opaque identity ${name}: expected exactly ${expected} occurrences, found ${found} (occurrence inventory changed)`);
    }
  }
  return problems;
}

// Schema-driven opaque value probe: check one sample value against the real
// discovered schema bounds (minLength/maxLength/format), so a bounds regression
// on any occurrence fails the probe rather than a hardcoded literal length.
function opaqueValueSatisfies(field, value) {
  if (!field || typeof value !== 'string') return false;
  if (field.format === 'uuid') return false;
  if (typeof field.minLength === 'number' && value.length < field.minLength) return false;
  if (typeof field.maxLength === 'number' && value.length > field.maxLength) return false;
  return true;
}

function validateS3FixShapes(root) {
  const problems = [];
  const schemas = root?.components?.schemas ?? {};

  // OQ-3/F-01: every external taskId/callId/recordingId/shareId occurrence —
  // operation path AND query parameters plus every component-schema property
  // at any nesting depth — is validated against its exact bounds and pinned by
  // an explicit occurrence inventory, so a disappeared/renamed/added
  // occurrence or a bounds/format regression fails closed.
  problems.push(...validateOpaqueIdOccurrences(root));

  // Internal UUID resources must stay uuid (regression guard).
  const internalUuidProps = [
    ['IvrDecision', 'decisionId'],
    ['IvrDecision', 'reservationId'],
    ['ShareState', 'evidenceId'],
    ['ShareSummary', 'evidenceId'],
    ['ShareCreated', 'evidenceId'],
    ['ShareAdmission', 'admissionId'],
  ];
  for (const [schemaName, propName] of internalUuidProps) {
    const field = schemas[schemaName]?.properties?.[propName];
    if (field && field.format !== 'uuid') {
      problems.push(`${schemaName}.${propName}: internal resource must keep format uuid`);
    }
  }

  // F-02: ShareCreated must be a satisfiable closed object (no allOf+AP:false
  // unsatisfiability), with capabilityToken required, extractionCode present
  // but optional, and no createdAt.
  const shareCreated = schemas.ShareCreated;
  if (!shareCreated) {
    problems.push('ShareCreated is required');
  } else {
    if (shareCreated.allOf) {
      problems.push('ShareCreated must be a flattened closed object, not an allOf composition');
    }
    if (shareCreated.additionalProperties !== false) {
      problems.push('ShareCreated must set additionalProperties false');
    }
    const required = new Set(Array.isArray(shareCreated.required) ? shareCreated.required : []);
    const props = shareCreated.properties ?? {};
    if (!required.has('capabilityToken')) {
      problems.push('ShareCreated.capabilityToken must be required');
    }
    if (props.capabilityToken?.readOnly !== true || props.capabilityToken?.['x-onecall-return-once'] !== true) {
      problems.push('ShareCreated.capabilityToken must be readOnly return-once');
    }
    if (props.extractionCode === undefined) {
      problems.push('ShareCreated.extractionCode must be present (optional, mode-conditional)');
    } else if (required.has('extractionCode')) {
      problems.push('ShareCreated.extractionCode must stay optional (only EXTRACTION_CODE mode)');
    }
    if (props.extractionCode && (props.extractionCode.readOnly !== true || props.extractionCode?.['x-onecall-return-once'] !== true)) {
      problems.push('ShareCreated.extractionCode must be readOnly return-once');
    }
    if ('createdAt' in props) {
      problems.push('ShareCreated must not declare createdAt (validFrom is authoritative)');
    }
  }

  // F-04: ShareSummary must not declare createdAt.
  const shareSummary = schemas.ShareSummary;
  if (shareSummary && 'createdAt' in (shareSummary.properties ?? {})) {
    problems.push('ShareSummary must not declare createdAt (validFrom is authoritative)');
  }

  // F-03/R-01: ShareCodeRotation is one operation/status contract delivered as
  // a closed discriminated result. The wrapper is a pure oneOf + discriminator
  // carrier (no duplicate property/discriminator envelope) with an explicit
  // canonical ROTATED/REPLAYED mapping so the generated union is inhabitable;
  // the branches stay the closed authorities. ROTATED requires extractionCode;
  // REPLAYED forbids it; neither branch has capabilityToken; no rotatedAt
  // projection is exposed (R-03).
  const rotation = schemas.ShareCodeRotation;
  if (!rotation) {
    problems.push('ShareCodeRotation is required');
  } else {
    if (rotation.type !== undefined) {
      problems.push('ShareCodeRotation wrapper must stay a pure oneOf carrier (no duplicate type/envelope)');
    }
    if (rotation.properties !== undefined) {
      problems.push('ShareCodeRotation wrapper must not re-declare a property envelope (R-01)');
    }
    if (rotation.additionalProperties !== undefined) {
      problems.push('ShareCodeRotation wrapper must not declare additionalProperties (closed by branches)');
    }
    if (rotation.discriminator?.propertyName !== 'result') {
      problems.push('ShareCodeRotation must declare discriminator propertyName result');
    }
    const rotationMapping = rotation.discriminator?.mapping;
    if (!rotationMapping
      || rotationMapping.ROTATED !== '#/components/schemas/ShareCodeRotationRotated'
      || rotationMapping.REPLAYED !== '#/components/schemas/ShareCodeRotationReplayed'
      || Object.keys(rotationMapping).length !== 2) {
      problems.push('ShareCodeRotation discriminator.mapping must map ROTATED/REPLAYED to the canonical branches');
    }
    const oneOfRefs = Array.isArray(rotation.oneOf) ? rotation.oneOf.map((b) => b?.$ref) : [];
    if (!oneOfRefs.includes('#/components/schemas/ShareCodeRotationRotated')
      || !oneOfRefs.includes('#/components/schemas/ShareCodeRotationReplayed')
      || oneOfRefs.length !== 2) {
      problems.push('ShareCodeRotation oneOf must be exactly the Rotated and Replayed branches');
    }
    if ('rotatedAt' in (rotation.properties ?? {})) {
      problems.push('ShareCodeRotation wrapper must not declare rotatedAt (R-03: unowned projection)');
    }
    // ROTATED branch: closed, extractionCode required, canonical const, no rotatedAt.
    const rotated = schemas.ShareCodeRotationRotated;
    if (!rotated || rotated.additionalProperties !== false) {
      problems.push('ShareCodeRotationRotated must be a closed object branch');
    }
    const rotatedRequired = new Set(Array.isArray(rotated?.required) ? rotated.required : []);
    if (!rotatedRequired.has('extractionCode')) {
      problems.push('ShareCodeRotationRotated must require extractionCode');
    }
    if (rotated?.properties?.result?.const !== 'ROTATED') {
      problems.push('ShareCodeRotationRotated.result must be the canonical const ROTATED');
    }
    if (rotated?.properties?.capabilityToken) {
      problems.push('ShareCodeRotationRotated must not expose capabilityToken');
    }
    if ('rotatedAt' in (rotated?.properties ?? {})) {
      problems.push('ShareCodeRotationRotated must not declare rotatedAt (R-03: unowned projection)');
    }
    // REPLAYED branch: closed, extractionCode absent (forbidden), canonical const, no rotatedAt.
    const replayed = schemas.ShareCodeRotationReplayed;
    if (!replayed || replayed.additionalProperties !== false) {
      problems.push('ShareCodeRotationReplayed must be a closed object branch');
    }
    if (replayed?.properties?.result?.const !== 'REPLAYED') {
      problems.push('ShareCodeRotationReplayed.result must be the canonical const REPLAYED');
    }
    if (replayed?.properties && 'extractionCode' in replayed.properties) {
      problems.push('ShareCodeRotationReplayed must not declare extractionCode');
    }
    if (replayed?.properties?.capabilityToken) {
      problems.push('ShareCodeRotationReplayed must not expose capabilityToken');
    }
    if ('rotatedAt' in (replayed?.properties ?? {})) {
      problems.push('ShareCodeRotationReplayed must not declare rotatedAt (R-03: unowned projection)');
    }
  }

  // F-06: protocolVersion required IVR input 1-32, not server-derived.
  const inboundResolve = schemas.IvrInboundResolveRequest;
  if (!inboundResolve) {
    problems.push('IvrInboundResolveRequest is required');
  } else {
    const required = new Set(Array.isArray(inboundResolve.required) ? inboundResolve.required : []);
    const pv = inboundResolve.properties?.protocolVersion;
    if (!required.has('protocolVersion')) {
      problems.push('IvrInboundResolveRequest.protocolVersion must be required');
    }
    if (pv?.minLength !== 1 || pv?.maxLength !== 32) {
      problems.push('IvrInboundResolveRequest.protocolVersion must be bounded 1-32');
    }
  }
  // F-06: I4 sequenceNo optional minimum 1, payload required, legId max 64.
  const callEvent = schemas.IvrCallEventRequest;
  if (!callEvent) {
    problems.push('IvrCallEventRequest is required');
  } else {
    const required = new Set(Array.isArray(callEvent.required) ? callEvent.required : []);
    if (required.has('sequenceNo')) {
      problems.push('IvrCallEventRequest.sequenceNo must stay optional');
    }
    if (callEvent.properties?.sequenceNo?.minimum !== 1) {
      problems.push('IvrCallEventRequest.sequenceNo must set minimum 1');
    }
    if (!required.has('payload')) {
      problems.push('IvrCallEventRequest.payload must be required');
    }
    if (callEvent.properties?.legId?.maxLength !== 64) {
      problems.push('IvrCallEventRequest.legId must set maxLength 64');
    }
  }
  // F-06: OutboundCallTask.expiresAt required non-null date-time.
  const outboundTask = schemas.OutboundCallTask;
  if (outboundTask) {
    const required = new Set(Array.isArray(outboundTask.required) ? outboundTask.required : []);
    const expiresAt = outboundTask.properties?.expiresAt;
    if (!required.has('expiresAt')) {
      problems.push('OutboundCallTask.expiresAt must be required');
    }
    if (expiresAt && Array.isArray(expiresAt.type) && expiresAt.type.includes('null')) {
      problems.push('OutboundCallTask.expiresAt must be non-nullable');
    }
  }

  // F-07: RecordingDeclarationRequest sha256 required, durationSeconds min 1,
  // mimeType max 64; RecordingCompletion.validatedDurationSeconds min 1.
  const declRequest = schemas.RecordingDeclarationRequest;
  if (!declRequest) {
    problems.push('RecordingDeclarationRequest is required');
  } else {
    const required = new Set(Array.isArray(declRequest.required) ? declRequest.required : []);
    if (!required.has('sha256')) {
      problems.push('RecordingDeclarationRequest.sha256 must be required');
    }
    if (declRequest.properties?.durationSeconds?.minimum !== 1) {
      problems.push('RecordingDeclarationRequest.durationSeconds must set minimum 1');
    }
    if (declRequest.properties?.mimeType?.maxLength !== 64) {
      problems.push('RecordingDeclarationRequest.mimeType must set maxLength 64');
    }
  }
  const completion = schemas.RecordingCompletion;
  if (completion?.properties?.validatedDurationSeconds?.minimum !== 1) {
    problems.push('RecordingCompletion.validatedDurationSeconds must set minimum 1');
  }

  // F-08/R-01: IvrDecision required includes the five frozen domain fields.
  // A0 §10.14: the two routing snapshot fields are NOT NULL; only the three
  // reservation-conditional fields are explicitly nullable.
  const ivrDecision = schemas.IvrDecision;
  if (!ivrDecision) {
    problems.push('IvrDecision is required');
  } else {
    const required = new Set(Array.isArray(ivrDecision.required) ? ivrDecision.required : []);
    const snapshotFields = ['availableRecordingSeconds', 'balanceAsOf'];
    const nullableReservationFields = ['reservationId', 'authorizedRecordingSeconds', 'authorizationExpiresAt'];
    for (const field of [...snapshotFields, ...nullableReservationFields]) {
      if (!required.has(field)) {
        problems.push(`IvrDecision must require ${field}`);
      }
    }
    for (const field of snapshotFields) {
      const prop = ivrDecision.properties?.[field];
      if (!prop || Array.isArray(prop.type) || prop.type === 'null') {
        problems.push(`IvrDecision.${field} must be non-nullable per A0 §10.14 NOT NULL`);
      }
    }
    for (const field of nullableReservationFields) {
      const prop = ivrDecision.properties?.[field];
      if (prop && !(Array.isArray(prop.type) && prop.type.includes('null'))) {
        problems.push(`IvrDecision.${field} must remain explicitly nullable`);
      }
    }
  }

  // F-09: I2 decision required ACCEPT; I3 promptVersion/confirmationKey/confirmationContent required.
  const activationConfirm = schemas.IvrActivationConfirmRequest;
  if (activationConfirm) {
    const required = new Set(Array.isArray(activationConfirm.required) ? activationConfirm.required : []);
    if (!required.has('decision')) {
      problems.push('IvrActivationConfirmRequest.decision must be required');
    }
    if (activationConfirm.properties?.decision?.enum?.[0] !== 'ACCEPT') {
      problems.push('IvrActivationConfirmRequest.decision must fix to ACCEPT');
    }
  } else {
    problems.push('IvrActivationConfirmRequest is required');
  }
  const directAuth = schemas.IvrDirectAuthorizationRequest;
  if (!directAuth) {
    problems.push('IvrDirectAuthorizationRequest is required');
  } else {
    const required = new Set(Array.isArray(directAuth.required) ? directAuth.required : []);
    for (const field of ['promptVersion', 'confirmationKey', 'confirmationContent']) {
      if (!required.has(field)) {
        problems.push(`IvrDirectAuthorizationRequest must require ${field}`);
      }
    }
    for (const [field, maxLen] of [['promptVersion', 64], ['confirmationKey', 64], ['confirmationContent', 128]]) {
      const prop = directAuth.properties?.[field];
      if (!prop || prop.minLength !== 1 || prop.maxLength !== maxLen) {
        problems.push(`IvrDirectAuthorizationRequest.${field} must be bounded 1-${maxLen}`);
      }
    }
  }

  return problems;
}

// F-10/F-11/OQ-3/OQ-4 focused probes: positive and negative fixtures run
// in-process so a regression in the validators or the contract fails closed
// at lint time.
function validateS3FixProbes(root) {
  const problems = [];
  const schemas = root?.components?.schemas ?? {};

  // OQ-3: schema-driven value probes against every discovered external
  // taskId/callId/recordingId/shareId occurrence. A non-UUID value within the
  // real occurrence's bounds must pass; empty and over-bound values must fail;
  // the checks read min/maxLength/format from the discovered field schema so a
  // bounds regression fails the probe instead of a hardcoded literal length.
  const opaqueOccurrences = collectOpaqueIdOccurrences(root);
  if (opaqueOccurrences.length === 0) {
    problems.push('S3 probe: no external opaque identity occurrences discovered (inventory regression)');
  }
  const positiveProbeValue = 'PROBE-VALUE-0001';
  for (const occurrence of opaqueOccurrences) {
    const label = `S3 probe (${occurrence.label})`;
    if (!opaqueValueSatisfies(occurrence.field, positiveProbeValue)) {
      problems.push(`${label}: non-UUID positive value ${JSON.stringify(positiveProbeValue)} must satisfy the occurrence schema`);
    }
    if (opaqueValueSatisfies(occurrence.field, '')) {
      problems.push(`${label}: empty value must fail the occurrence schema bounds`);
    }
    if (opaqueValueSatisfies(occurrence.field, 'x'.repeat(occurrence.maxLen + 1))) {
      problems.push(`${label}: over-bound value of length ${occurrence.maxLen + 1} must fail the occurrence schema bounds`);
    }
  }

  // F-02: the real ShareCreated must NOT be allOf (satisfiable flattened shape).
  if (schemas.ShareCreated?.allOf) {
    problems.push('S3 probe: ShareCreated must not use allOf (F-02 satisfiability)');
  }

  // F-02: secret-direction collector must resolve the flattened ShareCreated
  // and find both secrets with return-once annotations.
  const collected = collectResolvedProperties(root, schemas.ShareCreated, new Set());
  for (const property of ['capabilityToken', 'extractionCode']) {
    const field = collected.get(property);
    if (!field) {
      problems.push(`S3 probe: ShareCreated must expose ${property} after ref/composition resolution`);
    } else if (field.readOnly !== true || field['x-onecall-return-once'] !== true) {
      problems.push(`S3 probe: ShareCreated.${property} must be readOnly return-once`);
    }
  }

  // F-10: the only schemas carrying x-onecall-opaque: true must be the two
  // approved component schemas; any other annotation fails closed.
  for (const [name, schema] of Object.entries(schemas)) {
    if (schema && schema['x-onecall-opaque'] === true && !OPAQUE_CLOSURE_EXCEPTION_SCHEMAS.has(name)) {
      problems.push(`S3 probe: unapproved x-onecall-opaque on ${name}`);
    }
  }

  // OQ-4 executable opaque negatives: the real validators must reject an
  // unapproved component annotation and an unapproved inline annotation.
  const unapprovedComponentOpaqueRoot = {
    components: {
      schemas: {
        UnapprovedOpaquePayload: {
          type: 'object',
          additionalProperties: false,
          'x-onecall-opaque': true,
        },
      },
    },
  };
  const componentOpaqueProblems = validateSensitiveFields(unapprovedComponentOpaqueRoot);
  if (!componentOpaqueProblems.some((problem) => problem.includes('x-onecall-opaque is approved only on OpaqueProviderPayload and IvrEventPayload'))) {
    problems.push('S3 probe: validateSensitiveFields must reject an unapproved component x-onecall-opaque: true schema');
  }
  const inlineOpaqueProblems = [];
  walkRequestSchema(
    root,
    { type: 'object', additionalProperties: true, 'x-onecall-opaque': true },
    () => {},
    new Set(),
    (problem) => inlineOpaqueProblems.push(problem),
  );
  if (!inlineOpaqueProblems.some((problem) => problem.includes('x-onecall-opaque is approved only on OpaqueProviderPayload and IvrEventPayload'))) {
    problems.push('S3 probe: walkRequestSchema must reject an unapproved inline x-onecall-opaque: true schema');
  }
  if (!inlineOpaqueProblems.some((problem) => problem.includes('request object schema must set additionalProperties false'))) {
    problems.push('S3 probe: an unapproved inline x-onecall-opaque: true object must not gain the additionalProperties exemption');
  }
  // OQ-4 executable negative: an ordinary (unannotated) open request object
  // must still be rejected; the approved-node openness skip is identity-based.
  const ordinaryOpenProblems = [];
  walkRequestSchema(
    root,
    { type: 'object', additionalProperties: true },
    () => {},
    new Set(),
    (problem) => ordinaryOpenProblems.push(problem),
  );
  if (!ordinaryOpenProblems.some((problem) => problem.includes('request object schema must set additionalProperties false'))) {
    problems.push('S3 probe: walkRequestSchema must reject an ordinary request object with additionalProperties: true');
  }
  // OQ-4 executable negative: a directly traversed boolean true schema remains
  // rejected; the approved additionalProperties skip must not weaken this.
  const directBooleanProblems = [];
  walkRequestSchema(root, true, () => {}, new Set(), (problem) => directBooleanProblems.push(problem));
  if (!directBooleanProblems.some((problem) => problem.includes('request schema boolean true is forbidden'))) {
    problems.push('S3 probe: walkRequestSchema must reject a directly traversed boolean true schema');
  }
  // The two approved component nodes must stay fully walker-clean under the
  // same walker (node-identity exemption, not annotation-only). Requiring zero
  // problems catches the fix-3 regression where the approved node's own
  // boolean additionalProperties: true was re-walked as an ordinary schema.
  for (const approvedName of OPAQUE_CLOSURE_EXCEPTION_SCHEMAS) {
    const approvedSchema = schemas[approvedName];
    if (!approvedSchema) {
      problems.push(`S3 probe: approved opaque component ${approvedName} is missing`);
      continue;
    }
    const approvedProblems = [];
    walkRequestSchema(root, approvedSchema, () => {}, new Set(), (problem) => approvedProblems.push(problem));
    if (approvedProblems.length > 0) {
      problems.push(`S3 probe: approved opaque component ${approvedName} must produce zero walker problems, got: ${approvedProblems.join('; ')}`);
    }
  }

  // F-10/OQ-4: every x-onecall-a0-contracts entry referencing a § anchor must
  // be in the approved set (validateOperationMetadata already enforces
  // membership; this probe asserts the approved set contains no unapproved
  // extras and that the real validator rejects an unapproved anchor).
  const APPROVED_A0_SECTION_ANCHORS = new Set([
    '§7.1', '§7.3', '§7.4', '§9.18', '§9.24', '§9.27', '§9.28', '§10.14', '§10.17',
  ]);
  const citedSections = new Set();
  for (const [, pathItem] of Object.entries(root?.paths ?? {})) {
    if (!pathItem || typeof pathItem !== 'object') continue;
    for (const [, operation] of Object.entries(pathItem)) {
      if (!operation || typeof operation !== 'object') continue;
      for (const contract of extensionArray(operation, 'x-onecall-a0-contracts') ?? []) {
        if (typeof contract === 'string' && contract.startsWith('§')) {
          citedSections.add(contract);
        }
      }
    }
  }
  for (const section of citedSections) {
    if (!APPROVED_A0_SECTION_ANCHORS.has(section)) {
      problems.push(`S3 probe: cited A0 section anchor ${section} is not in the approved ownership set`);
    }
  }
  // OQ-4 executable anchor negative: a synthetic operation citing the
  // unapproved §7.2 anchor must be rejected by validateOperationMetadata with
  // the explicit not-an-accepted-anchor problem (the approved set keeps §7.2
  // deliberately absent; the exact Markdown-heading check is owned by
  // a0ContractAnchors).
  const unapprovedAnchorRoot = {
    paths: {
      '/probe': {
        post: {
          operationId: 'probeUnapprovedAnchor',
          'x-onecall-a0-contracts': ['§7.2'],
        },
      },
    },
  };
  const anchorProblems = validateOperationMetadata(unapprovedAnchorRoot);
  if (!anchorProblems.some((problem) => problem.includes('is not an accepted docs/14 anchor') && problem.includes('§7.2'))) {
    problems.push('S3 probe: validateOperationMetadata must reject the unapproved §7.2 A0 contract anchor');
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
          ...validateS3FixShapes(root),
          ...validateS3FixProbes(root),
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
