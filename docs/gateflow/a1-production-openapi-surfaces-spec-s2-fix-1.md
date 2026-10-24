# A1 production OpenAPI surfaces — S2 fix 1

Task label: `a1-openapi-s2-fix1-mimo26flash-20261024-01`
Correction task: `a1-openapi-s2-fix1-correction-mimo26flash-20261024-01` (same-provider bounded TypeScript-only correction; see §7)
Work unit: `A1-production-openapi-surfaces-spec`
Gate: `fix`
Provider: Claude Code / `mimo26flash`
Approved slice: **S2 only** (customers C9–C12, operations O8–O21)
Review source: `docs/reviews/code-review-20261024-131209.md`
Adjudication: `docs/gateflow/project-execution-control.md` §"A1 S2 code-review adjudication and fix entry"
Date: 2026-10-24

## 1. Scope

Implement only the controller-accepted S2 review findings **F-01..F-06** plus the two bounded clarifications (CNY currency freeze; invented-scope / unknown `x-onecall-*scope*` fail-closed) and the documentation-only O19 channel-token clarification. Keep all **32** cumulative operations (`customer=11 operations=21 ivr=0 public-share=0 provider-callback=0`) and all accepted S1/S2 boundaries intact. No new operation, no product/security/provider policy change, no DB/backend change, no A0 rewrite, no unapproved audit fields.

## 2. Allowed writes

| Path | Role |
|---|---|
| `api/openapi/onecall-v1.openapi.yaml` | Schema + description fixes |
| `api/openapi/contract-rules.mjs` | Walker / trusted-fact / scope / s2-fix-shapes validators |
| `api/openapi/client-compat.ts` | Generated-type + `@ts-expect-error` assertions |
| `docs/15-production-openapi-surface-spec.md` | Public surface doc updates |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-implementation.md` | Cumulative §14 update |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-1.md` | This fix artifact |

Everything else read-only. No stage/commit/push/PR/re-review/S3.

## 3. Accepted findings → concrete changes

### F-01 C10 explicit purchase consent

`OrderCreateRequest` now requires `decision` fixed to `enum: [ACCEPT]` (DECLINE is not order creation) plus required `policyCode`, `policyVersion`, `promptVersion`, `contentSha256` (pattern `^[0-9a-f]{64}$`). `purchaseConsentEventId` / `purchaseConsentId` remain server-derived from the authenticated account + `Idempotency-Key` and are not requestable. C10 operation description updated. The exact C10 Problem domain set (including `PURCHASE_CONSENT_REQUIRED`/`STALE`/`DECLINED`) is unchanged.

Static + generated-type assertions:
- `validateS2FixShapes` F-01 block (required fields, ACCEPT-only enum, no consent-ID request fields, bounds).
- `ForbiddenOrderCreateKey` extended with `currency`, `paidAt`, `snapshotSha256`, `versionToken`, `purchaseConsentId`, `issuanceStatus`.
- `AssertOrderCreateDecision` = `'ACCEPT'`.
- `@ts-expect-error` probes: DECLINE decision, missing decision, missing contentSha256, currency on request, purchaseConsentEventId on request.

### F-02 O20 trial draft `enabled`

`TrialPolicyDraft.enabled` is now `type: boolean` with `const: false` (required; never nullable/default true). `publishedAt` remains `type: 'null'`. Description updated.

Assertions:
- `validateS2FixShapes` F-02 block.
- `AssertTrialDraftEnabledFalse` = `false` (renamed from `AssertTrialDraftEnabledNull`).

### F-03 state-conditional identity

- `CatalogVersionPublicationRequest.required` = `[expectedActiveVersionId, expectedRevision]` (identity remains nullable for explicit null / none).
- `TrialPolicyPublicationRequest.required` = `[expectedEnabledPolicyVersionId, expectedRevision]` (nullable).
- O11 `CatalogVersionOffsaleRequest` and O14 `TrialPolicyDisableRequest` keep required `expectedRevision` only (no unexpected identity field).

Assertions:
- `validateS2FixShapes` F-03 block over all four publish/offsale/disable request shapes.
- `AssertCatalogVersionPublicationRequiredIdentity` / `AssertTrialPolicyPublicationRequiredIdentity`.
- `@ts-expect-error` probes: missing expected identity on both publish requests.

### F-04 audit closure

New closed component `RedactedAuditSnapshot`:
- `type: object`, `additionalProperties: false`, `minProperties: 1`
- only optional `status` (string 1-64), `revision` (integer ≥ 0), `reasonCode` (string 1-64)
- no maps / identifiers / PII / payload / provider fields

`AuditFact.beforeSnapshot` / `afterSnapshot` are now `oneOf: [$ref RedactedAuditSnapshot, {type: 'null'}]` (replacing open `type: [object, 'null']` + `additionalProperties: true`).

Response-side closure walker (`validateSensitiveFields`) now treats any type array admitting `object` as object-admitting and requires `additionalProperties: false`, preserving only the approved opaque exceptions (`OpaqueProviderPayload`, `IvrEventPayload` by schema name, or `x-onecall-opaque: true`).

Assertions:
- `validateS2FixShapes` F-04 block (closed / minProperties / exact keys / nullable refs).
- `AssertRedactedAuditSnapshotKeys`, `AssertAuditFactSnapshotNullable`.
- `@ts-expect-error` probe: extra key on `RedactedAuditSnapshot`.

### F-05 trusted facts

`TRUSTED_REQUEST_FACT_NAMES` extended with (at least) `currency`, `paidAt`, `snapshotSha256`, `versionToken`, `purchaseConsentId`, `purchaseConsentEventId`, `issuanceStatus`, plus additional server-derived fact names used by later surfaces (`createdByAccountId`, evidence/recording/share/storage facts). Client draft `priceCents`/`currency` remain permitted **only** on O18/O19 via `DRAFT_COMMERCIAL_OPERATION_IDS`; O20/O21 reject both.

Assertions:
- `validateTrustedRequestFacts` draft-commercial exception scoped to O18/O19.
- `@ts-expect-error` probes: priceCents/currency on `TrialPolicyDraftCreateRequest` and `TrialPolicyDraftReplaceRequest`.
- `ForbiddenOrderCreateKey` absence checks.

### F-06 O8 projection

New closed `CatalogProductVersionSummary` with exactly `catalogVersionId`, `versionNo`, `saleStatus`, `offerType`, `audience` (all required). `CatalogProductSummary` now requires a `versions` array of that component. No benefits, payments, snapshots, new endpoint or N+1. O8 description updated.

Assertions:
- `validateS2FixShapes` F-06 block.
- `AssertCatalogProductVersionSummaryKeys`, `AssertCatalogProductSummaryVersions`.

### Currency clarification (public v1 only)

All 8 former `pattern: '^[A-Z]{3}$'` currency properties plus the pre-existing `Money` schema are frozen to `enum: [CNY]`. Public API narrowing only; A0’s wider internal ISO storage domain is not modified.

Assertions:
- `validateS2FixShapes` currency-freeze walk (every string `currency` property must be exactly `enum: [CNY]`).
- `AssertOrderCurrency` and sibling currency asserts for all currency-bearing schemas.

### Scope clarification

`validateOperationMetadata` now fails closed on any operation-level `x-onecall-*` key matching `/scope/i`. No `CATALOG_DRAFT` / `TRIAL_POLICY_DRAFT` scope, no `platform.idempotency_record` on O18–O21, no `Idempotency-Key` on O18–O21 (existing draft-candidates / idempotency-writes checks retained).

### O19 channel-token clarification (documentation-only)

O19 (`replaceOperationsCatalogVersionDraft`) description and docs/15 §3.2 now state that existing-channel version tokens are regenerated server-side only and are never public input/output; no channel-token field or mutation endpoint is exposed.

## 4. Non-goals / stop conditions honored

- No new operation, path, Problem code, surface, or A0 table.
- No DB/backend/auth/payment runtime claim.
- No unapproved audit snapshot fields.
- No writes outside the six allowed paths.
- No stage/commit/push/PR/S3/re-review dispatch.

## 5. Validation performed / permission limits

| Check | Result |
|---|---|
| `git diff --check` on allowed paths | **0** diagnostics (no whitespace/conflict issues) |
| `node --check contract-rules.mjs` | **Blocked** — `This command requires approval` (not executed) |
| `npm run check:s2` | **Blocked** — same |
| `npm run check:s1` | **Blocked** — same |
| `npm run check:s3` | **Blocked** — same |
| Double `npm run generate` + d.ts SHA-256 | **Blocked** — both generates not executed; pre-existing d.ts still `e6fc258f431bbe5ffa3eb4e9e23bd2e7994cb9deaf33d1e71ff14166a1653444` (stale, pre-fix) |
| Live mutation probes | **Blocked** for redocly/tsc runs; adversarial `@ts-expect-error` probes + `validateS2FixShapes` assertions are embedded for controller re-run |
| Static structural review | Done via Read/Grep |
| Post-fix file SHA-256 | See cumulative report §14.4 |

Controller must re-run the full harness for authoritative exits.

## 6. Residual risks

- Static contract assertions prove schema/rule shapes only; they do not prove runtime purchase-consent evaluation, publication locking, or audit persistence.
- `OpaqueProviderPayload` / `IvrEventPayload` are S3 components; the walker exception set is forward-looking and currently unused by the S2 canonical document.
- Live `node`/`npm` validation (syntax check, check:s1/s2/s3, generate idempotency, mutation probes) is **pending controller re-run** due to session permission denial of all `node --check` / `npm run *` / `node -e` commands.
- `validateS2FixShapes` is registered as `onecall/s2-fix-shapes` and also folded into the already-enabled `onecall/sensitive-fields` rule so it runs under the existing redocly.yaml enablement without editing that non-allowed file.

## 7. Bounded correction (task `a1-openapi-s2-fix1-correction-mimo26flash-20261024-01`, 2026-10-24)

Controller evidence (`docs/gateflow/project-execution-control.md` §"A1 S2 fix 1 return and bounded correction entry") recorded that the authoritative WSL `npm run check:s2` failed strict TypeScript only in this fix's compatibility tests. Canonical schema/lint/bundle/generation did not fail. Correction is limited to `api/openapi/client-compat.ts` plus this artifact and the cumulative report; contract semantics are unchanged.

### 7.1 Failures corrected

| Failure | Root cause | Correction |
|---|---|---|
| `AssertAuditFactSnapshotNullable` (was ~line 325) | `AuditFact.beforeSnapshot` is intentionally optional, so the generated indexed access type is `RedactedAuditSnapshot \| null \| undefined`, not the asserted required-nullable form | Expected type extended with `\| undefined`; OpenAPI field remains optional (not made required) |
| Trial draft create/replace `@ts-expect-error` probes (was ~348–374) | Directive sat on the `const` declaration while TypeScript reports excess-property errors on the offending property line, yielding unused-directive + expected property errors | Each directive moved immediately above the excess `priceCents` / `currency` property; negatives still reject both fields on O20/O21 request shapes |
| `RedactedAuditSnapshot` extra-key probe (was ~388–391) | Same declaration-vs-property placement mismatch | Directive moved above the excess `providerPayload` property; closed-component negative preserved |
| C10 DECLINE / `currency` / `purchaseConsentEventId` probes (was ~461–464, 486–494) | Same placement mismatch (type error on `decision: 'DECLINE'`; excess-property errors on `currency` / `purchaseConsentEventId`) | Each directive moved above the actual offending property; ACCEPT-only + no-client-consent-ID negatives preserved |

`AssertTrialDraftEnabledFalse` remains `Equal<…['enabled'], false>`; generated `TrialPolicyDraft.enabled` is already literal `false` and was not loosened. Missing-required F-03 and C10 completeness probes keep their declaration-level `@ts-expect-error` (TypeScript reports those on the object literal).

### 7.2 Correction scope and validation

| Check | Result |
|---|---|
| Writes | `api/openapi/client-compat.ts`, this artifact, `docs/gateflow/a1-production-openapi-surfaces-spec-s2-implementation.md` only |
| Contract files (YAML / contract-rules / docs/15) | Untouched — SHA-256 unchanged from §14.4 / cumulative §14.4 |
| Post-correction `client-compat.ts` SHA-256 | `51d626cb597c278dbf642848c39d2737c36f01ed2534dfe1575897065e8b9a5b` (supersedes fix-1 `0c106cab…`) |
| `git diff --check` on allowed paths | **0** diagnostics |
| `node --check contract-rules.mjs` | **Blocked** — command approval denied |
| `npm run check:s2` | **Blocked** — same |
| Operation count / semantics / A0 mapping / runtime claims | Unchanged (still 32 cumulative ops; no schema, rule or docs change) |

Controller must re-run the full S2 harness for authoritative exits. No stage/commit/push/PR/re-review/S3.

## 8. Bounded correction 2 (task `a1-openapi-s2-fix1-correction2-mimo26flash-20261024-01`, 2026-10-24)

Controller WSL `npm run check:s2` had exactly one remaining failure after §7:

```text
api/openapi/client-compat.ts:349:1 TS2578 Unused '@ts-expect-error' directive
```

### 8.1 Root cause

Lines 347–349 of `client-compat.ts` were explanatory prose describing directive placement. The line-349 comment began with the literal `@ts-expect-error` token (`// @ts-expect-error directive must sit immediately above that property.`), so TypeScript parsed the prose itself as a suppress directive attached to the following statement (line 350), which had no error — yielding TS2578. The actual property-level negative probes were correctly placed and consumed.

### 8.2 Correction applied

Single mechanical reword of the prose in `api/openapi/client-compat.ts` (line 349): the comment now reads `// TypeScript suppress directive must sit immediately above that property.`, removing the literal directive marker from prose. No probe was moved, weakened, added, or removed: the four F-05 property-level probes (lines 354, 361, 368, 375), the F-04 extra-key probe (line 392), the F-03/C10 declaration- and property-level probes, and all `Assert*` types are byte-identical to §7 state. No contract semantic change; no operation/schema/rule/docs-15 change.

### 8.3 Validation limitation / result

| Check | Result |
|---|---|
| Writes | `api/openapi/client-compat.ts`, this artifact, `docs/gateflow/a1-production-openapi-surfaces-spec-s2-implementation.md` only |
| Contract files (YAML / contract-rules / docs/15) | Untouched |
| Post-correction-2 `client-compat.ts` SHA-256 | `ddc03357c887d2817d227e73d04844394938a4f88bb8d8d708ab6ab105276c59` (supersedes §7 `51d626cb…`) |
| `git diff --check` on `client-compat.ts` | **0** diagnostics |
| Grep: prose containing literal directive token | Only actual property/declaration-level probes remain (lines 354, 361, 368, 375, 380, 384, 392, 465, 468, 478, 490, 495) |
| `npm run check:s2` | **Not run** — command approval denied in this session (`This command requires approval`, attempted via cwd/`--prefix`/direct tsc); **controller rerun required** for authoritative exit |

Controller must re-run `npm run check:s2` for the authoritative post-correction-2 exit. No stage/commit/push/PR/re-review/S3.
