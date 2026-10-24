# A1 production OpenAPI surfaces — S2 fix 2

Task label: `a1-openapi-s2-fix2-mimo26flash-20261024-01`
Work unit: `A1-production-openapi-surfaces-spec`
Gate: `fix`
Provider: Claude Code / `mimo26flash`
Approved slice: **S2 only** (customers C9–C12, operations O8–O21)
Review source: `docs/reviews/code-review-20261024-141811.md` (pass-with-risks re-review; Open Questions #1 and #3)
Adjudication: `docs/gateflow/project-execution-control.md` §"A1 S2 re-review 1 adjudication and fix 2 entry"
Date: 2026-10-24

## 1. Scope

Implement exactly the two controller-accepted low-risk corrections from re-review 1, then stop. No new operation, no product/security/provider policy change, no DB/backend change, no A0 rewrite, no unapproved audit fields, no invented scope values, no per-channel token exposure. Cumulative operation set remains **32** (`customer=11 operations=21 ivr=0 public-share=0 provider-callback=0`).

**Correction 1** — full-document unknown `x-onecall-*scope*` rejection. The accepted fix-1 clarification was operation-only; re-review 1 showed a root-level mutation linted exit 0. The fail-closed boundary is now the entire OpenAPI object graph: document root, component collections and schemas, path/operation/request/response objects, and nested schema objects. Keys are matched case-insensitively against `/^x-onecall-.*scope/i`. Ordinary schema property names such as a business `scope` property, approved extension keys without `scope` (e.g. `x-onecall-idempotency`), and all values/prose are unaffected. Existing value/domain protections, O18–O21 state-conditional behavior (no `Idempotency-Key`, no `platform.idempotency_record`), and the exact C10 Problem domain set are retained.

**Correction 2** — remove the ambiguous public draft token. `CatalogVersionDraft.versionToken` is removed from the public schema and from all generated/client-compat/docs references. The singular field had no A0 version-level owner and no channel association, while O19/docs already state per-channel version tokens are regenerated server-side and are never public input/output. The required `CatalogOffer.versionToken` is preserved (C9 offer selection requires it). No per-channel tokens, no replacement token field, no token mutation endpoint, no additional projection. O18/O19 operation shapes and counts are unchanged.

## 2. Allowed writes

| Path | Role |
|---|---|
| `api/openapi/onecall-v1.openapi.yaml` | Remove `CatalogVersionDraft.versionToken` |
| `api/openapi/contract-rules.mjs` | Full-document scope walker + registered rule + s2-fix-shapes draft-token block |
| `api/openapi/client-compat.ts` | Absence/retention compile-time asserts + draft-token negative fixture |
| `docs/15-production-openapi-surface-spec.md` | §3.3 draft-projection note + §7 full-document scope negative wording |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-implementation.md` | Cumulative §15 update (history-preserving) |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s2-fix-2.md` | This fix artifact |

Everything else read-only. No stage/commit/push/PR/re-review/S3. The fix-1 artifact was not modified (no supersession marking needed).

## 3. Correction 1 — full-document scope rejection

### 3.1 Contract rule (`api/openapi/contract-rules.mjs`)

New helper block (lines 413–453) introduced before `validateOperationMetadata`:

- `SCOPE_EXTENSION_KEY = /^x-onecall-.*scope/i` — applied **only** to object keys, never to values or prose.
- `formatScopeExtensionLocation(segments)` — preserves the historical message form:
  - root (`segments.length === 0`) → `document root`;
  - operation objects (`paths/<route>/<method>`, i.e. `segments.length === 3 && segments[0] === 'paths' && METHODS.includes(segments[2])`) → `METHOD /route` (e.g. `POST /api/v1/operations/catalog/versions`), matching the fix-1 message format exactly;
  - all other nodes → dot-joined path (array indices appended as `[i]`), e.g. `components.schemas.CatalogVersionDraft`.
- `validateNoScopeExtensions(root)` — full recursive object-graph walk with a `seen` Set (DAG/cycle-safe), descending into every object/array value. Any key matching the regex is reported as `${location}: unapproved x-onecall scope extension ${key}`.

`validateOperationMetadata` (line 455) now seeds its problems from `validateNoScopeExtensions(root)` as the **sole** scope checker; the old operation-level-only loop (fix-1 former lines 490–499) was removed and replaced with a comment noting the full-document scan covers every operation and that no scope extension is approved on this surface. Removing the duplicate loop avoids double-reported operation messages.

Execution guarantee without editing read-only `redocly.yaml`:

- The full-document scan runs under the already-enabled `onecall/operation-metadata` rule via the fold at line 455.
- Additionally registered standalone as `'scope-extensions': makeRule(validateNoScopeExtensions)` at line 1689 (registered-only, mirroring `s2-fix-shapes`; not enabled in redocly.yaml because that file is out of the allowed-write set).

The `segments[0] === 'paths'` guard makes a component/schema named e.g. `get` safe: its segments start with `components`, so it never collides with the operation message form.

### 3.2 Positive controls (must pass, verified statically)

- All 32 existing `x-onecall-idempotency:` operation keys — no `scope` substring — pass.
- Root `x-onecall-contract` / `x-onecall-problem-registry` — pass.
- A business `scope` property (no `x-onecall-` prefix) — pass.
- The 9 value/prose occurrences of "scope" in the canonical YAML — never inspected (keys only).
- The canonical document itself contains **zero** keys matching `/^x-onecall-.*scope/i` (grep-verified).

### 3.3 Required negative probes (source-embedded; controller executes via lint)

| # | Probe mutation | Expected |
|---|---|---|
| N1 | Add `x-onecall-idempotency-scope: CATALOG_DRAFT` at document root | lint exit 1; message contains `document root: unapproved x-onecall scope extension x-onecall-idempotency-scope` |
| N2 | Add the same key under `components.schemas.CatalogVersionDraft` | exit 1; `components.schemas.CatalogVersionDraft: unapproved x-onecall scope extension x-onecall-idempotency-scope` |
| N3 | Add the same key on operation `createOperationsCatalogVersionDraft` | exit 1; `POST /api/v1/operations/catalog/versions: unapproved x-onecall scope extension x-onecall-idempotency-scope` (historical form) |
| N4 | Positive: canonical unmutated document | `npm run check:s2` exit 0 |
| N5 | Positive: add optional `scope: {type: string}` property to `CatalogVersionDraftCreateRequest.properties` | still exit 0 (non-extension property name) |

Probes N1–N3/N5 were **not executed** in this session (redocly/node permission-blocked; see §5). They are deterministic one-line YAML mutations for controller re-run.

## 4. Correction 2 — draft token removal

### 4.1 Canonical schema (`api/openapi/onecall-v1.openapi.yaml`)

Removed the 5-line `versionToken` property block from `CatalogVersionDraft` (formerly between `publishedAt` and `snapshotSha256`). Post-edit, `versionToken` appears only at:

- lines 3636/3679 — `CatalogVersionOffer`-adjacent `CatalogOffer.required` + property (required bounded string 1–128; C9 offer selection);
- lines 3743/3757 — `OrderSnapshotProjection.required` + property (server-returned snapshot projection).

No other draft-token field was added. O18 (`createOperationsCatalogVersionDraft`) and O19 (`replaceOperationsCatalogVersionDraft`) request/response schemas and operation counts are unchanged (32 operationIds, 30 path keys, 10 customer + 20 operations). `ProblemCode` enum remains 30 entries.

### 4.2 `validateS2FixShapes` block (`api/openapi/contract-rules.mjs` lines 1602–1631)

Before the currency-freeze walk:

- `CatalogVersionDraft` must exist, must **not** declare a `versionToken` property, and must **not** list `versionToken` in `required`.
- `CatalogOffer` must exist; its `versionToken` must remain a string with `minLength: 1` / `maxLength: 128`; `versionToken` must remain in `required` (C9).

Rule header comment (line 1421) updated to read "…CNY freeze, full-document scope fail-closed and fix-2 draft-token removal clarifications".

### 4.3 Client-compat assertions (`api/openapi/client-compat.ts`)

Compile-time (lines 347–360), exported at lines 638–639:

- `ForbiddenCatalogDraftTokenKey = Extract<keyof components['schemas']['CatalogVersionDraft'], 'versionToken'>` plus `AssertCatalogVersionDraftVersionTokenAbsent = Expect<Equal<ForbiddenCatalogDraftTokenKey, never>>` — fails to compile if the key reappears in any form (required or optional).
- `AssertCatalogOfferVersionTokenRetained = Expect<Equal<components['schemas']['CatalogOffer']['versionToken'], string>>` — captures both presence and requiredness: an optional offer token would widen the indexed-access type to `string | undefined` and fail.

Negative fixture `catalogDraftWithToken` (lines 411–438): a fully populated valid `CatalogVersionDraft` object literal (all required fields: catalogVersionId/productId uuids, versionNo 2, saleStatus DRAFT, offerType, audience, priceCents 9900, currency CNY, billingCycle MONTHLY, installments/grantIntervalMonths 1, benefitLines entry, channels MINIAPP, revision 1, createdAt, publishedAt null) ending with a property-level `// @ts-expect-error versionToken is not a public CatalogVersionDraft field.` directive immediately above `versionToken: 'removed-draft-token',`. Directive placement follows the fix-1 §7/§8 lesson: property-level for excess-property errors, and no prose line begins with the literal `@ts-expect-error` token (avoids TS2578).

Stale-generated-artifact note: against the pre-fix-2 `tmp/openapi-client/onecall-v1.d.ts` (still contains `readonly versionToken?: string` on `CatalogVersionDraft`), both the absence assert (Extract yields `'versionToken'`, not `never`) and the fixture's `@ts-expect-error` (unused directive) intentionally fail — correctly forcing controller regeneration before typecheck can pass.

### 4.4 Public docs (`docs/15-production-openapi-surface-spec.md`)

- §3.3 (line 120) appended: DRAFT projections do not return `versionToken`; existing channel version tokens are server-regenerated per channel and are never public input/output (no singular draft token, no channel-token field, no mutation endpoint).
- §7 negative-example line (line 210) rewritten: unknown `x-onecall-*scope*` extension keys are rejected at **any** depth (root, components, path/operation, request/response, nested schema) fail-closed; ordinary business property names `scope` and approved extensions without `scope` (e.g. `x-onecall-idempotency`) are unaffected.

No pre-existing draft-token references needed removal beyond the schema itself (the O19 channel-token clarification from fix-1 already said tokens are never public; check-doc-map.mjs contains no `versionToken` references; the O8 `listCustomerCatalogOffers` row at line 99 refers to the legitimate offer-level token and was left unchanged).

## 5. Validation performed / permission limits

| Check | Result |
|---|---|
| `git diff --check` on the four edited contract/doc files | **0** diagnostics |
| `git diff --cached --name-only` | Empty — index untouched; HEAD remains `0ccde10e01159cc633018db4e3d35e828ca8c904` |
| Canonical structure static re-count | 32 operationIds; 30 path keys (10 customer + 20 operations); 32 docs operation-map rows; `ProblemCode` 30 entries — unchanged |
| `versionToken` location grep | Only `CatalogOffer` (3636/3679) + `OrderSnapshotProjection` (3743/3757); zero occurrences under `CatalogVersionDraft` |
| `x-onecall-*scope*` key grep on canonical | Zero matches (document is clean) |
| Business `scope` key grep | Only prose/values; never an object key |
| Hygiene | No `U+FFFD`, no conflict markers; encodings utf-8 / us-ascii |
| `@ts-expect-error` placement audit | New directive at client-compat line 437 only, immediately above the offending property (line 438); no prose line starts with the literal token |
| Post-fix-2 file SHA-256 | See cumulative report §15.4 |
| `node --check api/openapi/contract-rules.mjs` | **Blocked** — `This command requires approval` (re-confirmed post-edit; not executed) |
| `npm run check:s2` | **Blocked** — same |
| `npm run check:s1` / `check:s3` (expected-negative controls) | **Blocked** — same |
| Double `npm run generate` + d.ts hash | **Blocked** — not executed; pre-fix-2 d.ts remains `04ace1bb98ef5a793cebd934035945d1555873754223553fc866d0169ba0c036` (stale, still contains the removed draft token) |
| Bundle hash (stale, unchanged) | `efd7ebb884ce7b4a041daefef55edd48de9066a05b9e11e36372d9b2f9cf3582` |
| Scope mutation probes N1–N3/N5 and positive N4 | **Blocked** for redocly runs; probe specifications recorded in §3.3 for controller re-run |
| `python3 -c` / heredoc static parsers | **Blocked** — approval denied |

Controller must re-run the full S2 harness (`node --check`, `check:s1` expected-negative, `check:s2`, `check:s3` expected-negative, double `generate` + d.ts SHA-256, scope probes N1–N5) for authoritative exits. Honest limitation recorded per the task's permission-block clause.

## 6. Residual risks (owners / destinations)

| Risk | Owner / destination |
|---|---|
| Static assertions prove schema/rule/type shapes only; runtime purchase-consent evaluation, publication locking, audit persistence, and server-side token regeneration remain unproven | Runtime implementation work (post-A1); independent review |
| `R-A0-O20-PUBLISHER` — O20 publisher ownership is an A0 runtime prerequisite, not an S2 static-contract blocker | A0 residual registry; runtime implementation |
| `PURCHASE_CONSENT_DECLINED` Problem producer not implemented (docs-only surface) | Runtime implementation (C10) |
| Nested/`oneOf` capacity of the full-document walker unproven by live lint (permission-blocked); DAG cycle-safety reasoned, not executed | Controller `check:s2` + probes N1–N3 |
| `OpaqueProviderPayload` / `IvrEventPayload` are S3 components; walker exception set is forward-looking and unused by the S2 canonical document | S3 surface work |
| Generated `tmp/openapi-client/onecall-v1.d.ts` is stale (pre-removal draft token); client-compat asserts deliberately fail against it | Controller `npm run generate` (double-run + hash) |
| `onecall/scope-extensions` and `onecall/s2-fix-shapes` are registered but not enabled in read-only `redocly.yaml`; execution rides the enabled `operation-metadata` / `sensitive-fields` folds | redocly.yaml owner (out of allowed-write set) if standalone enablement is later desired |
| Live `node`/`npm`/redocly validation pending controller re-run (session permission denial) | Controller re-review harness |

## 7. Completion / stop

Both controller-accepted corrections are implemented on the four allowed contract/doc files plus this artifact and the cumulative §15 update. Cumulative operation set, A0 mapping, C10 Problem domain set, and all S1/S2 boundaries are unchanged. Writes stayed inside the allowed set; the index was not staged; fix-1 artifact untouched.

Stopped here — before re-review dispatch, commit, stage, push, PR, or S3 entry, per the fix-gate stop condition.
