# A1 production OpenAPI surfaces — S3 fix-3 after independent re-review 1

- Gate label: `a1-openapi-s3-fix3-mimo26flash-20261024-01`
- Work unit: `A1-production-openapi-surfaces-spec`
- Loop: S3 review / bounded fix after independent re-review 1
- Controller decision source: `docs/gateflow/project-execution-control.md` section “A1 S3 independent re-review 1 result and adjudication”
- Re-review source: `docs/reviews/code-review-20261024-201450.md`
- This artifact does **not** claim controller acceptance or re-review acceptance.

## Scope (binding)

Implement only the controller-accepted corrections R-01, R-02, R-03 and the bounded OQ-3/OQ-4 validation coverage. Preserve prior F-01..F-11 corrections, the 49-operation surface set, and S1/S2 behavior. Do not expand C15 into a new conditional union; plan §5.6 keeps `extractionCode` optional on `ShareCreated`. Do not edit A0 `taskId` wording. Do not redesign global T-S/uq/ck ownership (classified aggregate-review residual).

## Write boundary

| Path | Action |
|---|---|
| `api/openapi/onecall-v1.openapi.yaml` | modified |
| `api/openapi/contract-rules.mjs` | modified |
| `api/openapi/client-compat.ts` | modified |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-3.md` | created (this file) |
| `docs/15-production-openapi-surface-spec.md` | **not** modified (no existing statement became false) |
| controller / plan / docs13 / docs14 / existing fix-review artifacts / package-config / screenshots / generated tracked state / git index-history | **not** modified |

## Exact changes

### R-01 — canonical inhabitable generated unions

**YAML (`ShareCodeRotation` C18 wrapper)**

- Wrapper reshaped from `type: object` + `additionalProperties: false` + full property envelope (including optional `extractionCode`) + bare `discriminator` into a **pure oneOf + discriminator carrier**: `description` + `oneOf` (exactly the two branch `$ref`s) + `discriminator.propertyName: result` + explicit canonical `mapping`:
  - `ROTATED → #/components/schemas/ShareCodeRotationRotated`
  - `REPLAYED → #/components/schemas/ShareCodeRotationReplayed`
- No `type`, `properties` or `additionalProperties` on the wrapper, so the closed branches own closure and openapi-typescript 7.8.0 emits the wrapper as exactly `ShareCodeRotationRotated | ShareCodeRotationReplayed` (no duplicate envelope ∩ branch intersection; no component-name literal ∩ canonical enum → `never`).
- Branches remain closed authorities: `type: object`, `additionalProperties: false`; ROTATED requires `extractionCode` with `result.const: ROTATED`; REPLAYED has **no** `extractionCode` property with `result.const: REPLAYED`; neither carries `capabilityToken`.

**YAML (`ShareAdmissionRequest.verification` P2)**

- Envelope preserved (`type: object`, `additionalProperties: false`, properties `mode`/`code`/`verificationProofId`, oneOf four branches) so F5-01 request closure and the existing P2 validator stay satisfied.
- Added explicit canonical `discriminator.mapping` for `NONE` / `EXTRACTION_CODE` / `DESIGNATED_MOBILE` / `ORGANIZATION_MEMBER` so branch `mode` becomes the wire literal and `envelope.mode ∩ "NONE"` is inhabitable.

**Validators (`contract-rules.mjs`)**

- F-03 block rewritten: assert pure-oneOf carrier (`type`/`properties`/`additionalProperties` undefined), exact two-key canonical mapping, exact oneOf branch set, branch closedness, ROTATED requires `extractionCode` + `const: ROTATED`, REPLAYED forbids `extractionCode` + `const: REPLAYED`, no `capabilityToken`, no unowned timestamp projection (R-03).
- P2 validator extended with exact `verification.discriminator.mapping` match against the four canonical mode→branch refs.

**Generated assertions (`client-compat.ts`)**

- Removed the prior comment/strategy that deliberately avoided literal discriminator assertions.
- Added: `AssertShareRotationRotatedResultLiteral` (`'ROTATED'`), `AssertShareRotationReplayedResultLiteral` (`'REPLAYED'`), `AssertShareRotationWrapperIsBranchUnion`, `AssertShareRotationWrapperNotNever`.
- Added inhabitance fixtures for canonical ROTATED (with `extractionCode`) and REPLAYED (without) on the wrapper type; `@ts-expect-error` negatives for REPLAYED+`extractionCode` (branch type) and component-name literal `'ShareCodeRotationRotated'` (branch type).
- Added P2: four branch mode literal asserts + `ShareAdmissionRequest.verification` inhabitance for `{mode:'NONE'}` and `{mode:'EXTRACTION_CODE', code:...}` + `@ts-expect-error` for `mode: 'ShareVerificationNone'`.
- All new assertion type names are exported.

### R-02 — exactly two open opaque payloads

- `IvrEventPayload` and `OpaqueProviderPayload` set `additionalProperties: true` (plus existing `x-onecall-opaque: true` and descriptions). YAML now contains exactly **2** `x-onecall-opaque: true` annotations (both approved components).
- Named-only / node-identity opaque exemptions and fail-closed closure rules unchanged (`OPAQUE_CLOSURE_EXCEPTION_SCHEMAS`, `walkRequestSchema`, `validateSensitiveFields`).
- No other request schema opened.
- Generated assertions: `AssertIvrEventPayloadOpenRecord` / `AssertOpaqueProviderPayloadOpenRecord` equal `Record<string, unknown>`; `AssertIvrEventPayloadNotNeverRecord` / `AssertOpaqueProviderPayloadNotNeverRecord`; concrete inhabitance consts for both payloads and for `IvrCallEventRequest.payload` (I4) and `PaymentProviderCallbackRequest.payload` (K1). Exported.

### R-03 — remove unowned timestamp projection

- `rotatedAt` removed from the C18 wrapper and from both branches (0 occurrences in `onecall-v1.openapi.yaml`).
- Not replaced with another timestamp.
- F-03 shapes validator asserts absence on wrapper/branches (failure messages reference the token only as a regression guard).
- No `rotatedAt` in `client-compat.ts` or `docs/15-production-openapi-surface-spec.md`.

### OQ-3 — schema-driven opaque identity inventory + probes

- Added `EXPECTED_OPAQUE_ID_OCCURRENCE_COUNTS` (exact fail-closed inventory after C18 wrapper reshape):
  - `taskId` 5 (2 schema + 3 params)
  - `callId` 11 (5 schema + 6 params)
  - `shareId` 7 (5 schema + 2 params; wrapper `shareId` removed with the envelope)
  - `recordingId` 4 (3 schema + 1 param)
- `collectOpaqueIdOccurrences(root)`: walks path-level and operation-level parameters (`$ref`-deref’d, any `in`, matched by name in `OPAQUE_ID_BOUNDS`) plus recursive component-schema properties (no `$ref` re-walk) including nested objects, array items, prefixItems, additionalProperties schemas, and composition subschemas.
- `validateOpaqueIdOccurrences(root)`: per-occurrence `assertOpaqueIdField` (type string, no `format: uuid`, `minLength` 1, exact `maxLength` 64/128) + exact per-name count comparison.
- Replaced the old path-param-only walk + hardcoded 11-entry component list.
- `opaqueValueSatisfies(field, value)`: schema-driven value check using the real occurrence’s `minLength`/`maxLength`/`format`.
- Probes in `validateS3FixProbes` are now schema-driven against every discovered occurrence: non-UUID positive within bounds must pass; empty must fail; over-bound (`maxLen+1`) must fail. Literal-length tautologies removed.
- Wire schemas for correct occurrences were not altered.

### OQ-4 — executable negative probes (bounded)

- Replaced comment-only synthetic inline-opaque fixture and tightened the probe suite:
  1. **Unapproved component opaque** — `validateSensitiveFields` on a synthetic root with `UnapprovedOpaquePayload` carrying `x-onecall-opaque: true` must yield the approved-only problem.
  2. **Unapproved inline opaque** — `walkRequestSchema` on an inline node with `x-onecall-opaque: true` must fire `onProblem` with the approved-only message.
  3. **Approved components stay exempt** — both `OpaqueProviderPayload` and `IvrEventPayload` component nodes must not fire that problem under the same walker (node-identity exemption).
  4. **Unapproved section anchor** — `validateOperationMetadata` on a synthetic operation citing `x-onecall-a0-contracts: ['§7.2']` must yield `is not an accepted docs/14 anchor` including `§7.2`.
- Explicit approved section set retained in probes (`§7.1`, `§7.3`, `§7.4`, `§9.18`, `§9.24`, `§9.27`, `§9.28`, `§10.14`, `§10.17`; `§7.2` deliberately absent). Real-document §-anchor membership walk retained. Exact Markdown-heading check remains owned by `a0ContractAnchors`.
- Real-document opaque annotation scan retained.
- T-S/uq/ck global ownership **not** redesigned (remains aggregate-review residual).

## R-01..R-03 / OQ-3 / OQ-4 mapping

| Correction | YAML | contract-rules.mjs | client-compat.ts | docs/15 |
|---|---|---|---|---|
| R-01 | C18 pure carrier + mapping; P2 mapping | F-03 rewrite; P2 mapping assert | canonical literals, union inhabitation, fixtures, negatives, exports | no change needed |
| R-02 | two payloads `additionalProperties: true` | opaque exemption paths unchanged; real-schema scan kept | open-record + not-never + inhabitance + exports | no change needed |
| R-03 | `rotatedAt` removed (0 YAML hits) | F-03 absence asserts | no `rotatedAt` token | no change needed |
| OQ-3 | no wire-schema changes | inventory walk + counts + schema-driven probes | n/a | no change needed |
| OQ-4 | n/a | executable anchor + component/inline opaque negatives; approved set kept | n/a | no change needed |

## Commands / outcomes / hashes

Environment: WSL/linux, workspace `/mnt/d/codex-call`, Node `v22.23.4` (`node --version` executes).

**Blocked by the session approval gate** (every attempt returned `This command requires approval`, including sandbox-disabled and path-alias variants; an isolated subagent reproduced the same block):

| Command | Status |
|---|---|
| `node --check api/openapi/contract-rules.mjs` | **BLOCKED — not executed** |
| `npm --prefix api/openapi run generate` (×2) | **BLOCKED — not executed** |
| `npm --prefix api/openapi run check:s3` | **BLOCKED — not executed** |
| `npm --prefix api/openapi run check:s1` | **BLOCKED — not executed** |
| `npm --prefix api/openapi run check:s2` | **BLOCKED — not executed** |
| `tsc --noEmit -p tsconfig.client-compat.json` | **BLOCKED — not executed** |
| `git diff --check` / `git status` / `git diff --cached` | **BLOCKED — not executed** |

**Executed static checks (available commands only):**

| Check | Outcome |
|---|---|
| `file` on the three API files | UTF-8 text; no CRLF marker on any of the three |
| Grep U+FFFD / conflict markers on the three API files | no matches |
| Grep `rotatedAt` in `onecall-v1.openapi.yaml` | **0** occurrences |
| Grep `rotatedAt` in `client-compat.ts` | **0** occurrences |
| Grep `x-onecall-opaque: true` in YAML | exactly **2** (both approved components) |
| Grep external id properties/params in YAML | taskId 2 schema+3 params; callId 5+6; shareId 5+2; recordingId 3+1 — matches expected inventory 5/11/7/4 |
| `grep -c 'avoid literal const values' client-compat.ts` | **0** (strategy comment removed) |
| New assertion names present and exported | yes (literals, union, P2 modes, open records) |
| `docs/15` rotation/opaque statements | still true post-fix (line 145: ROTATED requires once-only `extractionCode`, REPLAYED forbids it, both branches lack `capabilityToken`; opaque payload statements unchanged); no `rotatedAt` mention |
| Stale generated d.ts (`tmp/openapi-client/onecall-v1.d.ts`) | sha256 `e2c08fb74155979822cb1f7735accf536b26c50e5f56d8488009713ca6fc3ff0`; mtime **before** current YAML edits — reflects pre-fix defective shapes (component-name discriminators, optional `extractionCode` on REPLAYED, `Record<string, never>` payloads, 3× `rotatedAt`). **Not regenerated** because `npm run generate` is blocked. |

Because generate/typecheck/check:s* could not run, the following required validations remain **unverified in this environment**:

- double-generate d.ts hash stability
- generated C18/P2 canonical and inhabitable types
- generated REPLAYED exclusion of `extractionCode`
- generated open records for I4/K1 payloads
- S1/S2 expected negative stage checks
- focused mutation/probe runtime evidence beyond static source review
- exact runtime counts 18/21/6/3/1=49 and 59 Problem codes
- docs/A0 zero-defect confirmation via `check:s3`
- `git diff --check`, empty index, allowed-path audit via git

## Residuals and owners

| Residual | Owner |
|---|---|
| Run `node --check`, `npm --prefix api/openapi run check:s3`, double `generate` + sha256 compare, focused typecheck, and S1/S2 stage checks in an environment where node/npm/git execute | fix author / controller re-entry |
| Confirm generated d.ts shows pure `ShareCodeRotationRotated \| ShareCodeRotationReplayed`, wire-literal branch discriminators, REPLAYED without `extractionCode`, `IvrEventPayload`/`OpaqueProviderPayload` as open unknown-valued records, zero `rotatedAt` | fix author / re-review |
| Global T-S/uq/ck ownership unification | classified aggregate-review residual (out of OQ-4 scope) |
| C15 `ShareCreated.extractionCode` remains optional (plan §5.6); not expanded into a conditional union | plan §5.6 / A0 — intentional non-change |
| Stale tracked screenshot / preview binary noise in the working tree (outside write boundary) | pre-existing; not touched by fix-3 |

## Completion status

- Bounded fix for R-01, R-02, R-03, OQ-3, OQ-4 **implemented** in the three allowed API files.
- `docs/15-production-openapi-surface-spec.md` **not** edited (no statement became false).
- Validation **attempted**; executable steps **blocked** by the approval gate and reported above without claiming success.
- Stopped after bounded fix + validation attempt + this artifact.
- Did **not** self-review, produce a review artifact, stage/commit/push/PR/merge, or enter another gate.
- Does **not** claim controller acceptance or independent re-review acceptance.
