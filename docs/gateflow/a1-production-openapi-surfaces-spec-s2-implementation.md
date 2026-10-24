# A1 production OpenAPI surfaces — S2 implementation report

Task label: `a1-openapi-s2-impl-mimo26flash-20261024-01`
Work unit: `A1-production-openapi-surfaces-spec`
Gate: `implementation`
Approved slice: **S2 only** (customers C9–C12, operations O8–O21)
Accepted S1 base: `0ccde10e01159cc633018db4e3d35e828ca8c904`
Branch: `codex/a1-production-openapi-surfaces-spec`
Date: 2026-10-24
Report-sync task: `a1-openapi-s2-report-sync-mimo26flash-20261024-01` (§13 — controller evidence only; no code changes)

## 1. Outcome

S2 static contract is implemented on the four allowed existing files. Cumulative operation set is **32** (`customer=11 operations=21 ivr=0 public-share=0 provider-callback=0`). S1 rules were preserved and extended only additively.

**Controller validation is green (authoritative).** After the bounded same-provider 401 correction (§12), the controller ran the full harness: `node --check contract-rules.mjs` exit 0; `npm run check:s2` exit 0 with Redocly valid/0 problems, bundle/generate/strict TypeScript/doc-map passing, and exact stage counts `operations=32 customer=11 operations=21 ivr=0 public-share=0 provider-callback=0` (expected `32` and `11/21/0/0/0`); doc and A0 missing/extra/duplicate/invalid all zero; path/server/duplicate checks pass. The required exact-stage negative `check:s1` and incomplete-future-stage negative `check:s3` both exited 1 exactly as specified (§13). Two independent `npm run generate` runs produced an identical client `d.ts` SHA-256. **No S2 implementation blocker remains.** The child-session approval-gate denials recorded in §8/§12.4 remain historical execution evidence only and are superseded by this controller validation.

## 2. Changed files (allowed writes only)

| Path | Change |
|---|---|
| `api/openapi/onecall-v1.openapi.yaml` | +18 operations (C9–C12, O8–O21); +15 Problem codes (enum now 30); +30 schemas (Money, catalog, order, trial, audit, draft); +`OrderIdempotencyKeyParam` (max 475); new conflict responses |
| `api/openapi/contract-rules.mjs` | Additive only: `exactDomainSets` +10 ops; `priceCents` draft-only trusted-fact exception; `validateDraftCandidates` required/forbidden sets; S2 sort/digest maps |
| `api/openapi/client-compat.ts` | 32-op union; 30-code union; S2 path entries; new field-absence and enum assertions; value fixtures |
| `docs/15-production-openapi-surface-spec.md` | S1+S2 cumulative doc; 32-row `operation-map`; S2 behavior table; 30-code Problem table; O20/O21 R-A0-O20-PUBLISHER row |

Not written (prohibited): docs/09–14, controller, commit/stage/push/PR, S3 surfaces.

## 3. Operation inventory (32)

**S1 (14)** — unchanged:
`getCustomerMe`, `listCustomerBusinessNumbers`, `getCustomerSubject`, `createCustomerSubjectClaim`, `createCustomerSubjectActivation`, `listCustomerEvidence`, `getCustomerEvidence`, `listOperationsSubjects`, `getOperationsSubject`, `suspendOperationsSubject`, `resumeOperationsSubject`, `listOperationsActivations`, `listOperationsIvrSessions`, `getOperationsIvrSession`

**S2 customer C9–C12 (4)**
| opId | method | path |
|---|---|---|
| `listCustomerCatalogOffers` | GET | `/api/v1/customer/catalog/offers` |
| `createCustomerOrder` | POST | `/api/v1/customer/orders` |
| `listCustomerOrders` | GET | `/api/v1/customer/orders` |
| `getCustomerOrder` | GET | `/api/v1/customer/orders/{orderId}` |

**S2 operations O8–O21 (14)**
| opId | method | path | roles |
|---|---|---|---|
| `listOperationsCatalogProducts` | GET | `/api/v1/operations/catalog/products` | ADMIN, OPS |
| `getOperationsCatalogProduct` | GET | `/api/v1/operations/catalog/products/{productId}` | ADMIN, OPS |
| `publishOperationsCatalogVersion` | POST | `/api/v1/operations/catalog/versions/{versionId}/publications` | ADMIN |
| `offsaleOperationsCatalogVersion` | POST | `/api/v1/operations/catalog/versions/{versionId}/offsales` | ADMIN |
| `listOperationsTrialPolicies` | GET | `/api/v1/operations/trial-policies` | ADMIN, OPS |
| `createOperationsTrialPolicyDraft` | POST | `/api/v1/operations/trial-policies` | ADMIN |
| `publishOperationsTrialPolicy` | POST | `/api/v1/operations/trial-policies/{policyVersionId}/publications` | ADMIN |
| `disableOperationsTrialPolicy` | POST | `/api/v1/operations/trial-policies/{policyVersionId}/disables` | ADMIN |
| `listOperationsOrders` | GET | `/api/v1/operations/orders` | ADMIN, OPS |
| `getOperationsOrder` | GET | `/api/v1/operations/orders/{orderId}` | ADMIN, OPS |
| `listOperationsAuditFacts` | GET | `/api/v1/operations/audit` | ADMIN, OPS |
| `createOperationsCatalogVersionDraft` | POST | `/api/v1/operations/catalog/versions` | ADMIN |
| `replaceOperationsCatalogVersionDraft` | PUT | `/api/v1/operations/catalog/versions/{versionId}/draft` | ADMIN |
| `replaceOperationsTrialPolicyDraft` | PUT | `/api/v1/operations/trial-policies/{policyVersionId}/draft` | ADMIN |

## 4. Frozen S2 behavior implemented

- **C9** ACTIVE catalog visibility; server-derived pricing/currency; sort `productCode asc, versionNo desc, catalogOfferId asc`; digest `channel,audience,offerType`.
- **C10** client selects `subjectId`/`catalogOfferId`/`channel` (+ optional `expectedVersionToken`) only; amount, consent, activation, payment, real-name are server-derived. Codes: `IDEMPOTENCY_CONFLICT`, `ORDER_UNKNOWN_PAYMENT_PENDING`, `PURCHASE_CONSENT_REQUIRED/STALE/DECLINED`, `ACTIVATION_NOT_ACTIVE`, `REAL_NAME_REQUIRED`. Idempotency class `idempotency-key` max **475**.
- **C11/C12** immutable order snapshot + `paymentStatus` (`UNPAID|UNKNOWN|PAID|CLOSED`) + computed `issuanceStatus` (`NONE|PENDING|ISSUED|FAILED|MIXED`); sort `createdAt desc, orderId desc`.
- **O8–O17** operations catalog/trial/order/audit reads; O10/O11 publication/offsale with `expectedActiveVersionId` + `expectedRevision`; O13/O14 publish/disable with `expectedEnabledPolicyVersionId` + `expectedRevision`.
- **O18–O21** DRAFT create/replace: `state-conditional`; **no** `Idempotency-Key`; **no** `platform.idempotency_record` mapping; natural keys `(productId,versionNo)` and `(FIRST-TRIAL,versionNo)`; `expectedRevision` minimum 1; create 201 first / 200 same-projection replay / 409 `*_DRAFT_CONFLICT`; replace 200 applied-or-replay / 409 `*_DRAFT_STALE_VIEW` or `*_DRAFT_CONFLICT`.
- **Canonical content projections** (plan §4.7) documented in docs/15 §3.3.
- **Trusted-fact guard** still bans client `priceCents` on `OrderCreateRequest`; draft O18/O19 may carry draft commercial `priceCents` only (scoped exception in `contract-rules.mjs`).
- **No invented scopes**: `CATALOG_DRAFT` / `TRIAL_POLICY_DRAFT` do not appear as scopes or idempotency domains.

## 5. R-A0-O20-PUBLISHER (unchanged)

Carried verbatim as a runtime prerequisite on O20/O21 via `x-onecall-r-a0-o20-publisher: R-A0-O20-PUBLISHER`. S2 freezes only the static public contract. `published_by_employee_id` is not exposed, renamed, fabricated, or solved; no runtime insertability claim. Docs/15 §8 row updated to that wording.

## 6. Problem codes (30)

S1 15 + S2 15:
`CATALOG_DRAFT_CONFLICT`, `CATALOG_DRAFT_STALE_VIEW`, `TRIAL_POLICY_DRAFT_CONFLICT`, `TRIAL_POLICY_DRAFT_STALE_VIEW`, `CATALOG_PUBLICATION_CONFLICT`, `PUBLICATION_STALE_VIEW`, `TRIAL_POLICY_PUBLICATION_CONFLICT`, `TRIAL_POLICY_STALE_VIEW`, `PURCHASE_CONSENT_REQUIRED`, `PURCHASE_CONSENT_STALE`, `PURCHASE_CONSENT_DECLINED`, `REAL_NAME_REQUIRED`, `ACTIVATION_NOT_ACTIVE`, `ORDER_UNKNOWN_PAYMENT_PENDING`, `IDEMPOTENCY_CONFLICT`.
Registry ↔ enum set equality enforced by `contract-rules.mjs`; exact domain sets for the 13 exact-set operations (incl. O20 without `NOT_FOUND`).

## 7. A0 mappings

Every S2 operation carries `x-onecall-a0-models` restricted to the docs/14 44-table catalog (schema-qualified). Draft writes also map `operations_iam.employee_account` + `audit.audit_fact`. Publication/offsale/disable map `catalog_order.catalog_version` or `entitlement_metering.trial_policy_version`, `audit.audit_fact`, `platform.outbox_event`.

## 8. Validation

> **Superseded by §13 (controller validation).** The tables in §8.0–§8.4 record the original child-session attempts and are retained purely as historical execution evidence of the approval-gate denials. The authoritative validation outcome is §13.

### 8.0 Commands attempted in the child session (historical; from WSL `/mnt/d/codex-call`)

| Command | Exit | Result |
|---|---|---|
| `npm run check:s2` (cwd, `--prefix`, absolute) | — | **BLOCKED** `This command requires approval` |
| `node --check contract-rules.mjs` | — | **BLOCKED** |
| `npm run lint` / `bundle` / `generate` / `typecheck` / `doc-map` / `check` / `check:s1` / `check:s3` | — | **BLOCKED** |
| direct `node_modules/.bin/{redocly,tsc,openapi-typescript}` | — | **BLOCKED** |
| `bash -lc 'npm run check:s2'` | — | **BLOCKED** |
| `git diff --check --` (4 allowed paths) | **0** | clean (no whitespace/conflict markers) |
| `file -bi` (4 allowed paths) | **0** | utf-8 / us-ascii; no BOM/CRLF markers via tool |
| `sha256sum` sources | **0** | see §8.1 |

Two agents independently confirmed the approval gate blocks essentially all code execution (`node -p "1+1"` blocked).

### 8.1 Source hashes (implementation session, pre-correction; see §12.5 / §13 for the authoritative post-correction hash)

```
1ff9b1fda4f960fdd61a2d7c6bbd1cf5364205a1d0594857385e233f4bc7f41f  api/openapi/onecall-v1.openapi.yaml
4d501e032e52af324815520db7ac09994b8ab36ec5120791f94f0a8ea150e771  api/openapi/contract-rules.mjs
6eacccaf732d858e0c100e5a12c7e900db9921a218167093d577b2e7d580deeb  api/openapi/client-compat.ts
20284993e52cea04e4f5004550a2fa71485efd0b19d4d53ea017f24bcf1b4412  docs/15-production-openapi-surface-spec.md
```

### 8.2 Pre-existing generated artifacts at implementation time (historical; superseded by the controller `generate` runs in §13)

```
1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521  tmp/openapi-client/onecall-v1.d.ts   (1409 lines; paths end at S1 getOperationsIvrSession — S1-era)
3adff4abe9ad583da234881bf1b0170c13de2bb567fb524cfead11e4981cc85b  tmp/openapi-client/onecall-v1.bundle.json
```

At implementation time double-`generate` hash equality was **not verified** (generate had not run). The controller subsequently ran `generate` twice independently with identical output — see §13.

### 8.3 Static structure checks (read-only greps)

| Check | Result |
|---|---|
| Path keys in YAML | 30 unique `/api/v1/...` keys |
| Methods | 32 operations |
| operationId unique set | 32; matches ExpectedOperationId / STAGES.s2 |
| Surface counts | customer 11 / operations 21 / ivr 0 / public-share 0 / provider-callback 0 |
| docs/15 operation-map rows | 32 |
| ProblemCode enum | 30 entries |
| Registry entries | 30 (one per code) |
| `x-onecall-idempotency` | O18–O21 = `state-conditional`; C10 = `idempotency-key` |
| `OrderIdempotencyKeyParam` | referenced by C10; max 475 |
| O20/O21 publisher extension | present on both |
| Trusted fields on OrderCreateRequest | absent (price/amount/payment/trial/activation) |
| Trial draft publisher fields | absent |
| Catalog draft replace immutables | productId/versionNo/benefitLines/channels absent |
| Sensitivity names in schemas | not present as properties |
| `CATALOG_DRAFT`/`TRIAL_POLICY_DRAFT` as scopes | not present |
| `git diff --check` | exit 0 |

### 8.4 Negative probes (historical)

Deterministic `/tmp` fixtures for O18–O21 (Idempotency-Key, platform.idempotency_record, invented scopes, wrong sets/counts, client priceCents on OrderCreateRequest) were **not executed** in the child session (approval-gated). Client-compat type-level negative assertions are present in source; the controller's strict TypeScript step subsequently passed (§13). The harness-stage negatives `check:s1` and `check:s3` were run by the controller with the required exit-1 outcomes (§13).

### 8.5 Required harness commands (historical checklist; all executed by the controller — see §13)

```bash
cd /mnt/d/codex-call/api/openapi
npm run check:s2   # expected PASS: operations=32 customer=11 operations=21 ivr=0 public-share=0 provider-callback=0
npm run check:s1   # required exact-stage negative: exit 1 reporting exactly the 18 S2 operations as unexpected
npm run check:s3   # required incomplete-future-stage negative: exit 1 reporting exactly 17 missing S3 operations
npm run generate && sha256sum ../../tmp/openapi-client/onecall-v1.d.ts
npm run generate && sha256sum ../../tmp/openapi-client/onecall-v1.d.ts  # hashes must match
node --check contract-rules.mjs
```

## 9. Residual risks (classified, post-controller-validation)

| Class | Risk |
|---|---|
| ~~BLOCKER (env)~~ **SUPERSEDED** | The former "check:s2 never executed / gate not green" blocker is obsolete: the controller ran the full harness green (§13). **No S2 implementation blocker remains.** |
| ~~WARN~~ **SUPERSEDED** | Stale S1-era d.ts: resolved — two independent controller `generate` runs produced the identical current `d.ts` hash `e6fc258f431bbe5ffa3eb4e9e23bd2e7994cb9deaf33d1e71ff14166a1653444` (§13). |
| **INFO** | Child-session `/tmp` fixture probes were never run; not an S2 static-contract blocker — controller evidence covers `check:s2`, strict TypeScript, doc-map, and both required stage negatives. |
| **INFO** | `R-A0-O20-PUBLISHER` remains a classified runtime prerequisite (A0 precondition), **not** an S2 static-contract blocker (§5). |
| **INFO** | Static contract evidence does not prove runtime/database/auth/payment behavior; those remain runtime concerns outside the S2 static gate. |
| **INFO** | Product seed / offline support contact / V1 pagination defaults remain open questions (docs/15 §7). |

## 10. Docs decision

docs/15 was updated in place (allowed) as the cumulative S1+S2 machine-read contract. No docs/09–14 edits. Plan and controller were not modified.

## 11. Stop point

**S2 implementation complete — ready for independent code review.** Stopped after S2 implementation, the bounded §12 correction, controller validation recorded in §13, and this report synchronization. Not acceptance: no commit, stage, push, PR, self-review, review-acceptance, or S3 entry.

## 12. S2 implementation correction (task `a1-openapi-s2-impl-correction-mimo26flash-20261024-01`, 2026-10-24)

Base checkpoint: `0ccde10e01159cc633018db4e3d35e828ca8c904`; working files were the S2 implementation of task `a1-openapi-s2-impl-mimo26flash-20261024-01`.

### 12.1 Controller dynamic evidence (failure that motivated this correction)

The controller ran `node --check contract-rules.mjs` (passed before npm) and then `npm run check:s2` (exit 1):

```text
POST /api/v1/customer/orders: code UNAUTHENTICATED requires an explicit 401 response
onecall/problem-complete
```

Root cause: `createCustomerOrder` declared `UNAUTHENTICATED` in `x-onecall-error-codes` but its `responses` map lacked a `'401'` entry. The `problem-complete` rule requires an explicit response keyed at each declared code's registry `httpStatus` (only `INTERNAL` may fall back to `500`/`default`).

### 12.2 Full 18-operation response/declared-code consistency audit

To prevent a sibling omission in the next controller run, every S2 operation (C9–C12, O8–O21) was audited read-only against the registry `httpStatus` map: every code in each operation's `x-onecall-error-codes` must have an explicit response keyed at that status (shared status keys legitimately cover multiple same-status codes, e.g. the single `'400'` for `CURSOR_INVALID`+`CURSOR_FILTER_MISMATCH`, the single `'409'` for publication/draft conflict pairs).

| opId | declared codes needing explicit status | explicit responses present | result |
|---|---|---|---|
| `listCustomerCatalogOffers` | 401, 403, 422, 400, 429, 503, default | 400/401/403/422/429/503/default | OK |
| `createCustomerOrder` | 401, 403, 422, 409 (IDEMPOTENCY_CONFLICT + ORDER_UNKNOWN_PAYMENT_PENDING + PURCHASE_CONSENT_*), 403 (ACTIVATION_NOT_ACTIVE), 422 (REAL_NAME_REQUIRED), 429, 503, default | 403/409/422/429/503/default — **missing 401** | **DEFECT → corrected** |
| `listCustomerOrders` | 401, 403, 422, 400, 429, 503, default | 400/401/403/422/429/503/default | OK |
| `getCustomerOrder` | 401, 403, 404, 422, 429, 503, default | 401/403/404/422/429/503/default | OK |
| `listOperationsCatalogProducts` | 401, 403, 422, 400, 429, 503, default | 400/401/403/422/429/503/default | OK |
| `getOperationsCatalogProduct` | 401, 403, 404, 422, 429, 503, default | 401/403/404/422/429/503/default | OK |
| `publishOperationsCatalogVersion` | 401, 403, 404, 422, 409 (CATALOG_PUBLICATION_CONFLICT + PUBLICATION_STALE_VIEW), 429, 503, default | 401/403/404/409/422/429/503/default | OK |
| `offsaleOperationsCatalogVersion` | same as publish | 401/403/404/409/422/429/503/default | OK |
| `listOperationsTrialPolicies` | 401, 403, 422, 400, 429, 503, default | 400/401/403/422/429/503/default | OK |
| `createOperationsTrialPolicyDraft` | 401, 403, 422, 409 (TRIAL_POLICY_DRAFT_CONFLICT), 429, 503, default | 401/403/409/422/429/503/default | OK |
| `publishOperationsTrialPolicy` | 401, 403, 404, 422, 409 (TRIAL_POLICY_PUBLICATION_CONFLICT + TRIAL_POLICY_STALE_VIEW), 429, 503, default | 401/403/404/409/422/429/503/default | OK |
| `disableOperationsTrialPolicy` | same as publish | 401/403/404/409/422/429/503/default | OK |
| `listOperationsOrders` | 401, 403, 422, 400, 429, 503, default | 400/401/403/422/429/503/default | OK |
| `getOperationsOrder` | 401, 403, 404, 422, 429, 503, default | 401/403/404/422/429/503/default | OK |
| `listOperationsAuditFacts` | 401, 403, 422, 400, 429, 503, default | 400/401/403/422/429/503/default | OK |
| `createOperationsCatalogVersionDraft` | 401, 403, 404, 422, 409 (CATALOG_DRAFT_CONFLICT), 429, 503, default | 401/403/404/409/422/429/503/default | OK |
| `replaceOperationsCatalogVersionDraft` | 401, 403, 404, 422, 409 (CATALOG_DRAFT_CONFLICT + CATALOG_DRAFT_STALE_VIEW), 429, 503, default | 401/403/404/409/422/429/503/default | OK |
| `replaceOperationsTrialPolicyDraft` | 401, 403, 404, 422, 409 (TRIAL_POLICY_DRAFT_CONFLICT + TRIAL_POLICY_DRAFT_STALE_VIEW), 429, 503, default | 401/403/404/409/422/429/503/default | OK |

Exactly one defect existed across all 18 operations: `createCustomerOrder` lacked `'401'`. No other declared-code/status omission was found.

### 12.3 Correction applied

`api/openapi/onecall-v1.openapi.yaml` — added to `createCustomerOrder` `responses` (between `'200'` and `'403'`):

```yaml
        '401':
          $ref: '#/components/responses/Unauthenticated'
```

This is the same shared `components.responses.Unauthenticated` `$ref` used by every other operation that declares `UNAUTHENTICATED`. No error codes, registry entries, Problem matrix rows, semantics, or other files were changed. `contract-rules.mjs`, `client-compat.ts` and `docs/15` were audited and required no change for this correction (docs/15 documents the common 401/403 Problem rows and the exact C10 domain set, neither of which altered).

### 12.4 Validation in this correction session (read-only harness attempts; historical — superseded by §13)

| Command | Result |
|---|---|
| `node --check contract-rules.mjs` | **BLOCKED** `This command requires approval` (contract-rules.mjs was not edited this session; the controller itself reported it passed) |
| `npm run check:s2` | **BLOCKED** `This command requires approval`; not runnable in that child session — subsequently executed green by the controller (§13) |
| `sha256sum` (4 allowed paths) | exit 0; see §12.5 |
| read-only 18-op response/code audit (this report §12.2) | exactly one defect, corrected in §12.3 |

The controller subsequently re-ran the harness with exit 0 after this correction — recorded in §13.

### 12.5 Source hashes after the correction (this session)

```
b92da3273d6e5a27fa58786435a5060561ee6098ea33a05ac1a00fbc587d9c5f  api/openapi/onecall-v1.openapi.yaml
4d501e032e52af324815520db7ac09994b8ab36ec5120791f94f0a8ea150e771  api/openapi/contract-rules.mjs
6eacccaf732d858e0c100e5a12c7e900db9921a218167093d577b2e7d580deeb  api/openapi/client-compat.ts
20284993e52cea04e4f5004550a2fa71485efd0b19d4d53ea017f24bcf1b4412  docs/15-production-openapi-surface-spec.md
```

Only `onecall-v1.openapi.yaml` differs from §8.1 (the added `'401'` response). The other three allowed files are byte-identical to the S2 implementation session.

### 12.6 Correction stop point

Stopped after the bounded YAML correction and this report update. No commit, stage, push, PR, self-review, review artifact, S3 entry or controller edit.

## 13. Controller validation evidence (task `a1-openapi-s2-report-sync-mimo26flash-20261024-01`, 2026-10-24)

Authoritative dynamic validation performed by the controller after the §12 bounded 401 correction. This section supersedes the child-session approval-gate blocker language in §1/§8/§9/§12.4, which is retained above only as historical execution evidence.

### 13.1 Syntax check

| Command | Exit |
|---|---|
| `node --check contract-rules.mjs` | **0** |

### 13.2 First `npm run check:s2` (exit 1 — motivated the bounded §12 correction)

Exit **1** solely because `createCustomerOrder` declared `UNAUTHENTICATED` in `x-onecall-error-codes` but lacked an explicit `401` response:

```text
POST /api/v1/customer/orders: code UNAUTHENTICATED requires an explicit 401 response
```

The bounded same-provider correction added `$ref: '#/components/responses/Unauthenticated'` to `createCustomerOrder` responses (§12.3), audited all **18** S2 operations (C9–C12, O8–O21) against declared-code/status consistency (§12.2), and found **no sibling omission** — exactly one defect existed.

### 13.3 Second `npm run check:s2` (exit 0 — S2 gate green)

Exit **0**:

- Redocly: valid, **0** problems.
- bundle / generate / strict TypeScript / doc-map: all passed.
- Exact stage counts: `operations=32 customer=11 operations=21 ivr=0 public-share=0 provider-callback=0` — matches expected `32` and `11/21/0/0/0`.
- Doc and A0 checks: missing / extra / duplicate / invalid all **zero**.
- Path / server / duplicate checks: pass.

### 13.4 Required stage negatives (expected exit 1 — not regressions)

| Command | Exit | Meaning |
|---|---|---|
| `npm run check:s1` | **1** | Required **exact-stage negative**: reports exactly the **18** S2 operations as unexpected, counts `11/21/32` rather than `7/7/14`. Correct S2-preserves-S1-exactness signal — **not** a regression. |
| `npm run check:s3` | **1** | Required **incomplete-future-stage negative**: reports exactly **17** missing S3 operations and absent IVR/public-share/callback counts. Correct S3-not-yet-implemented signal — **not** an S2 failure. |

### 13.5 Idempotent client generation

Two independent `npm run generate` executions produced byte-identical output:

```
e6fc258f431bbe5ffa3eb4e9e23bd2e7994cb9deaf33d1e71ff14166a1653444  tmp/openapi-client/onecall-v1.d.ts
```

### 13.6 Canonical artifact hash (post-correction)

```
b92da3273d6e5a27fa58786435a5060561ee6098ea33a05ac1a00fbc587d9c5f  api/openapi/onecall-v1.openapi.yaml
```

Matches §12.5 (the canonical OpenAPI after the bounded 401 correction).

### 13.7 Hygiene checks (allowed paths)

- `git diff --check`: no diagnostics.
- Strict UTF-8 decode: passes; `U+FFFD` = 0, `CR` = 0.
- Conflict-marker scan: no match.

### 13.8 Classification after controller validation

- **No S2 implementation blocker remains.**
- `R-A0-O20-PUBLISHER` remains a classified **runtime prerequisite**, not an S2 static-contract blocker.
- Static evidence does **not** prove runtime/database/auth/payment behavior.

### 13.9 Controller-validation stop point

Implementation complete — ready for independent code review. No acceptance, commit, stage, push, PR, or S3 entry; controller not edited; no new tests run or claimed by this report-sync task.

## 14. S2 fix 1 — new final state (task `a1-openapi-s2-fix1-mimo26flash-20261024-01`, 2026-10-24)

This section supersedes the pre-fix hashes in §13.5/§13.6 for the four implementation files. It records the cumulative state after the controller-accepted fix for findings F-01..F-06 plus the CNY freeze, scope fail-closed, and O19 channel-token documentation clarifications, and after the same-provider bounded TypeScript-only correction of the fix-authored `client-compat.ts` assertions (task `a1-openapi-s2-fix1-correction-mimo26flash-20261024-01`; see fix-1 §7). Full change detail lives in `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-1.md`. §1–§13 remain historical implementation/validation evidence.

### 14.1 Outcome

S2 fix 1 is implemented on the same four allowed existing files plus this report and the fix artifact. Cumulative operation set remains **32** (`customer=11 operations=21 ivr=0 public-share=0 provider-callback=0`). All accepted S1/S2 boundaries, the exact C10 Problem domain set (incl. `PURCHASE_CONSENT_REQUIRED/STALE/DECLINED`), and `R-A0-O20-PUBLISHER` are retained. No new operation, A0 table, Problem code, surface, or product/security/provider policy.

### 14.2 Fixes applied

| Finding | Change |
|---|---|
| F-01 | `OrderCreateRequest` requires explicit `decision=ACCEPT` + `policyCode`/`policyVersion`/`promptVersion`/`contentSha256`; consent event ID stays server-derived; C10 description updated |
| F-02 | `TrialPolicyDraft.enabled` is required boolean `const: false`; `publishedAt` stays `null` |
| F-03 | O10/O13 publication requests require nullable expected identity + `expectedRevision`; O11/O14 retain required `expectedRevision` only |
| F-04 | Closed `RedactedAuditSnapshot` (`status`/`revision`/`reasonCode` only, `additionalProperties: false`, `minProperties: 1`); AuditFact snapshots are nullable refs; response closure walker treats type-arrays admitting `object` as object-admitting with opaque exceptions preserved |
| F-05 | Trusted-fact set extended (`currency`, `paidAt`, `snapshotSha256`, `versionToken`, `purchaseConsentId`/`purchaseConsentEventId`, `issuanceStatus`, …); draft commercial exception scoped to O18/O19 only |
| F-06 | `CatalogProductSummary.versions` requires closed `CatalogProductVersionSummary` (`catalogVersionId`/`versionNo`/`saleStatus`/`offerType`/`audience`) |
| Currency | All public v1 monetary `currency` fields frozen to literal `enum: [CNY]` (public surface only; A0 ISO storage domain unchanged) |
| Scope | Fail closed on invented idempotency scope values and unknown `x-onecall-*scope*` extensions |
| O19 docs | Channel version tokens regenerated server-side only; not public input/output; no channel-token field or mutation endpoint |

### 14.3 Changed files (allowed writes only)

| Path | Fix change |
|---|---|
| `api/openapi/onecall-v1.openapi.yaml` | F-01..F-06 schemas + C10/O8/O19/Money descriptions + currency `enum: [CNY]` |
| `api/openapi/contract-rules.mjs` | `DRAFT_COMMERCIAL_OPERATION_IDS`, extended `TRUSTED_REQUEST_FACT_NAMES`, opaque-closure exceptions, strengthened `validateSensitiveFields`, scoped draft-commercial exception, scope fail-closed, new `validateS2FixShapes` rule |
| `api/openapi/client-compat.ts` | Extended forbidden keys, currency asserts, `AssertTrialDraftEnabledFalse`, F-01/F-03/F-04/F-06 asserts, `@ts-expect-error` adversarial probes, updated fixture + exports |
| `docs/15-production-openapi-surface-spec.md` | §3.2 C10/O8/O10/O13/O18/O19/O20/O21/O17 rows; §5 CNY freeze; §6 consent + O18/O19-only commercial exception; §7 scope negatives |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-1.md` | New fix artifact |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-implementation.md` | This §14 cumulative update |

### 14.4 Post-fix hashes

```
d492a20ea6d32c77788c34c68ec3c031ad5d49a8662417f177b9e13aaa076fdc  api/openapi/onecall-v1.openapi.yaml
0576fe8d9f65d9c9de87bcc2bf8ffbf6add17f68f2a00192c3ba5c34f4290223  api/openapi/contract-rules.mjs
51d626cb597c278dbf642848c39d2737c36f01ed2534dfe1575897065e8b9a5b  api/openapi/client-compat.ts
6f7a6914073f3ad89afc75cf4b357dd8c9f06472bfad062744cbdbfb215a5927  docs/15-production-openapi-surface-spec.md
```

The YAML / contract-rules / docs/15 hashes match the original fix-1 §14.4; only `client-compat.ts` changed under bounded correction `a1-openapi-s2-fix1-correction-mimo26flash-20261024-01` (supersedes fix-1 `0c106cab…`). These supersede the pre-fix §13.5/§13.6 hashes (`e6fc258f…` d.ts, `b92da327…` YAML). Generated `tmp/openapi-client/onecall-v1.d.ts` was not regenerated in the fix or correction sessions because `npm run generate` / `node --check` / redocly require elevated command approval that was unavailable to the authors; controller re-run is required for the post-fix d.ts SHA-256.

### 14.5 Validation

See `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-1.md` §5 (original fix) and §7 (bounded TypeScript correction) plus the corresponding task final responses for exact command exits, permission-limited commands, and hygiene evidence. The authoritative WSL `check:s2` after fix 1 failed strict TypeScript only in `client-compat.ts` (`AuditFact.beforeSnapshot` optional-type assert; `@ts-expect-error` placement on declaration lines for excess-property / property-type negatives). The correction moved directives onto the offending property lines and widened the snapshot assert with `| undefined` without changing OpenAPI requiredness. Controller re-validation of the full S2 harness is required before any acceptance decision.

### 14.6 Fix-1 stop point

Stopped after the six-file bounded fix plus the same-provider compatibility-test correction. No commit, stage, push, PR, self-review acceptance, re-review dispatch, S3 entry, or controller edit.

### 14.7 S2 fix 1 bounded correction 2 (task `a1-openapi-s2-fix1-correction2-mimo26flash-20261024-01`, 2026-10-24)

Controller WSL `npm run check:s2` had exactly one failure after the §7 bounded correction: `api/openapi/client-compat.ts:349:1 TS2578 Unused '@ts-expect-error' directive`. Root cause was explanatory prose (lines 347–349) whose line-349 comment began with the literal `@ts-expect-error` token, which TypeScript parsed as a directive attached to the error-free statement at line 350. Correction: the prose was reworded to `// TypeScript suppress directive must sit immediately above that property.`, removing the literal marker from prose only. All actual property-level negative probes (F-05 lines 354/361/368/375, F-04 line 392, F-03/C10 probes) and all `Assert*` types were left in place, unmoved and unweakened. Writes: `api/openapi/client-compat.ts` plus `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-1.md` §8 and this subsection — no contract file (YAML / contract-rules / docs/15) changed, no operation/schema/rule semantic change.

Validation limitation/result: post-correction-2 `client-compat.ts` SHA-256 `ddc03357c887d2817d227e73d04844394938a4f88bb8d8d708ab6ab105276c59` (supersedes §14.4 `51d626cb…`); `git diff --check` on the edited file exits 0 with no diagnostics; grep confirms the literal directive token remains only on genuine probe lines. `npm run check:s2` was **not run** in this session — command approval was denied (`This command requires approval`) for cwd, `--prefix`, and direct `tsc` invocations — so **controller rerun is required** for the authoritative post-correction-2 exit. §14.4/§14.5 hash and validation statements are superseded for `client-compat.ts` by this subsection. No stage/commit/push/PR/re-review/S3.

## 15. S2 fix 2 — new final state (task `a1-openapi-s2-fix2-mimo26flash-20261024-01`, 2026-10-24)

This section supersedes the §14.4 hashes for the four implementation files and records the cumulative state after the two controller-accepted re-review-1 corrections (full-document unknown `x-onecall-*scope*` rejection; removal of the ambiguous public `CatalogVersionDraft.versionToken`). Full change detail lives in `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-2.md`. §1–§14.7 remain historical implementation/validation evidence and were not erased.

### 15.1 Outcome

S2 fix 2 is implemented on the same four allowed existing files plus this report and the new fix-2 artifact. Cumulative operation set remains **32** (`customer=11 operations=21 ivr=0 public-share=0 provider-callback=0`); 30 path keys, 32 `ProblemCode` entries, C10 Problem domain set, O18/O19 shapes, and all S1/S2 boundaries unchanged. No new operation, A0 table, Problem code, surface, per-channel token field, token mutation endpoint, or product/security/provider policy.

### 15.2 Corrections applied

| Correction | Change |
|---|---|
| Full-document scope rejection | `SCOPE_EXTENSION_KEY = /^x-onecall-.*scope/i` keys-only match over the entire object graph via `validateNoScopeExtensions` (DAG-safe walk); folded as the sole scope checker into enabled `onecall/operation-metadata` (line 455) and registered standalone as `onecall/scope-extensions` (line 1689); historical `METHOD /route` message form preserved via `formatScopeExtensionLocation`; old operation-only loop removed; ordinary `scope` property names and approved extensions without `scope` (e.g. `x-onecall-idempotency`) pass |
| Draft token removal | `CatalogVersionDraft.versionToken` property deleted from the canonical YAML; `validateS2FixShapes` (lines 1602–1631) forbids its reintroduction and freezes required `CatalogOffer.versionToken` (bounded 1–128); client-compat `AssertCatalogVersionDraftVersionTokenAbsent` (`Extract<keyof …, 'versionToken'>` → `never`) + `AssertCatalogOfferVersionTokenRetained` (`Equal<…['versionToken'], string>`) + `catalogDraftWithToken` property-level `@ts-expect-error` negative fixture; docs/15 §3.3 states DRAFT projections never return `versionToken` and §7 states the full-document scope boundary |

### 15.3 Changed files (allowed writes only)

| Path | Fix-2 change |
|---|---|
| `api/openapi/onecall-v1.openapi.yaml` | Removed `CatalogVersionDraft.versionToken` (5-line property block) |
| `api/openapi/contract-rules.mjs` | Full-document scope helpers (lines 413–453), sole-checker fold (455), operation-only loop removed (comment at 543), s2-fix-shapes draft-token block (1602–1631), header comment (1421), `scope-extensions` registration (1689) |
| `api/openapi/client-compat.ts` | Absence/retention asserts (347–360), `catalogDraftWithToken` fixture with property-level `@ts-expect-error` (411–438), exports (638–639) |
| `docs/15-production-openapi-surface-spec.md` | §3.3 draft-projection note (line 120); §7 full-document scope negative wording (line 210) |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-2.md` | New fix artifact |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-implementation.md` | This §15 cumulative update |

### 15.4 Post-fix-2 hashes

```
2b004341d7ead66e0e3ba2a2c4a29e73477d98189b83237b9379061361acc79f  api/openapi/onecall-v1.openapi.yaml
024476601e4f2e2f5d8edfc654ce115b77a267526a4864236f936237c0ae4cbf  api/openapi/contract-rules.mjs
36f723c570cf2c4e72433eea9b7eda706058fd7ee6e1f5c608b7c492f1e4590a  api/openapi/client-compat.ts
52b36da7e6bdd8caa2997b73453c76060d6c60fd426298ffd7de5a495e194333  docs/15-production-openapi-surface-spec.md
```

These supersede the §14.4 / §14.7 hashes (YAML `d492a20e…`, contract-rules `0576fe8d…`, client-compat `ddc03357…`, docs/15 `6f7a6914…`) for the four implementation files. The stale generated `tmp/openapi-client/onecall-v1.d.ts` remains `04ace1bb98ef5a793cebd934035945d1555873754223553fc866d0169ba0c036` (pre-removal draft token still present) and the stale bundle `efd7ebb884ce7b4a041daefef55edd48de9066a05b9e11e36372d9b2f9cf3582`; `npm run generate` was permission-blocked in this session, so **controller double-generate + d.ts SHA-256 is required**. Client-compat asserts intentionally fail against the stale d.ts to force that regeneration.

### 15.5 Validation

Static battery completed: `git diff --check` clean on the four files; index untouched (`git diff --cached --name-only` empty, HEAD `0ccde10…`); 32 operationIds / 30 path keys / 30 ProblemCode entries unchanged; zero `x-onecall-*scope*` keys in canonical; `versionToken` only under `CatalogOffer` (3636/3679) and `OrderSnapshotProjection` (3743/3757); no U+FFFD, no conflict markers; the new `@ts-expect-error` sits property-level above the offending property and no prose line begins with the literal token. Dynamic validation (`node --check`, `check:s1`/`check:s2`/`check:s3`, double generate, scope probes N1–N5) was **permission-blocked** (`This command requires approval`), demonstrated post-edit; probe specifications and residual risks with owners are recorded in fix-2 §3.3 and §6. Controller re-run of the full S2 harness is required for authoritative exits.

### 15.6 Fix-2 stop point

Stopped after the six-file bounded fix. No commit, stage, push, PR, self-review acceptance, re-review dispatch, S3 entry, or controller edit. Fix-1 artifact left unmodified (no supersession marking needed).
