# A1 S1 bounded-fix-3 artifact

> Task label: `a1-openapi-s1-fix3-mimo26flash-20261024-01`
>
> Provider: Claude Code / `mimo26flash` / `mimo-v2.6-flash`
>
> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: fix — bounded fix round 3 after independent DeepSeek re-review
>
> Accepted plan base: `0c2d2ca23d16b8c32999e95a1321184d1c78df24`
>
> Binding re-review: `docs/reviews/code-review-20261024-032551.md`
>
> Status: COMPLETED (implementation complete; awaiting independent cross-provider re-review — this provider does not self-accept)

## 1. Scope

Applied only the two controller-accepted low-severity findings from `docs/reviews/code-review-20261024-032551.md`:

- **R3-01** request-body indirection/media completeness (`requestSchema` + `validateTrustedRequestFacts`).
- **R3-02** JSON Schema boolean/type-array forms in `walkRequestSchema`.

Preserved all previously accepted A–G and R2-01/R2-02 fixes, the exact 14-operation S1 contract `C1,C2,C3,C4,C5,C13,C14` + `O1..O7`, OpenAPI semantics, generated types and counts `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`. Static validator hardening only; the canonical 14-operation OpenAPI contract is unchanged (SHA-256 still `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf`).

### Explicitly rejected/deferred (unchanged non-goals)

- No `x-onecall-audience` surface binding (API Governance/Security; later A1 aggregate review).
- No traversal/rejection of `patternProperties`, `if`/`then`/`else`, `dependentSchemas` or other non-enumerated JSON Schema keywords (API Validation; later A1 aggregate review).
- No generic JSON Schema engine and no claim of exhaustive schema validation.
- No OpenAPI operation/schema/route/response/count change, no `docs/15` edit, no `check-doc-map.mjs` edit, no `redocly.yaml`/`client-compat.ts`/package/config change.
- No prior artifact, reviewer evidence, controller, plan, prototype, screenshot or shared-JS edit.
- No stage, commit, push, PR, merge, re-review or S2 entry.

## 2. Changed paths (exactly three)

| # | Path | Role |
| --- | --- | --- |
| 1 | `api/openapi/contract-rules.mjs` | R3-01 shared requestBody resolution + strict media completeness; R3-02 boolean/type-array handling in the request walker |
| 2 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | Cumulative report updated truthfully for fix 3 |
| 3 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-3.md` | This fix artifact (created last) |

Temporary negative/control fixtures live only under `/tmp/onecall-s1-fix3-probes/` (plus the reused `/tmp/onecall-s1-fix-probes/` and `/tmp/onecall-s1-fix2-probes/` suites).

## 3. Controller decisions → applied changes

### R3-01 — request-body indirection/media completeness

- Added `resolveRequestBody(root, operation)`: resolves an operation-level `requestBody.$ref` through `components.requestBodies` via the existing cycle-guarded `deref`. Distinguishes "no body declared" (skip) from "declared but unresolvable" (missing/non-object target, external ref, cyclic guard exhaustion → fail closed).
- `requestSchema` now consumes that shared resolver, so event-body idempotency checks and future draft forbidden-field checks see through a valid `requestBody` `$ref` instead of silently receiving `null`. (The event-body `required` read stays on the raw `operation.requestBody`; it remains fail-closed for `$ref`'d bodies and was not part of the accepted finding.)
- `validateTrustedRequestFacts` now, for every declared body: fails closed with `requestBody must resolve to a request body object` when resolution fails; requires a non-empty object `content` map (`request body must declare a non-empty content map`); requires every media entry to be an object (`... must be an object`) that declares a schema (`... must declare a schema`); keeps the pure `#/components/schemas/...` `$ref` requirement, `$ref`-sibling rejection, trusted-fact walk and closure checks on every actual media schema. Media-level failures record the problem and skip walking that entry (nothing valid to walk); inline/non-pure schemas still walk after recording the pure-ref problem so nested trusted facts and open objects keep failing as before.
- Passing control preserved: a valid operation-level `requestBody` `$ref` whose media schema points to the closed `ClaimRequest` component stays green (probe L).

### R3-02 — JSON Schema boolean/type-array forms

- `walkRequestSchema` rejects boolean `true` at every traversed position (direct property/items/prefixItems/additionalProperties/composition children, and post-deref when a `$ref` resolves to `true`) with `request schema boolean true is forbidden: it admits arbitrary data`.
- Boolean `false` is allowed as a closed/nothing schema and is simply not walked (including under properties/items/prefixItems/additionalProperties).
- Object-admission for the `additionalProperties: false` closure requirement now includes any `type` array containing `object` (for example `['object', 'null']`, `['object']`), in addition to string `type: object` and presence of `properties`.
- `additionalProperties` traversal widened from object-valued only to any truthy value so boolean `true` at that position is rejected; `false` is skipped (allowed).
- Cycle safety (seen-set on resolved nodes), `$ref`-sibling reject-not-merge policy and the existing traversal set (`properties`, `items`, `prefixItems`, `allOf`/`anyOf`/`oneOf`, schema-valued `additionalProperties`) are unchanged.

## 4. Accepted finding status (Gateflow terms)

| Finding | Review status (20261024-032551) | Fix-3 status |
| --- | --- | --- |
| 01 — request-body guard skips valid `requestBody`/media indirection (R3-01, 低) | 未修复 (residual) | **FIXED** — probes A/B/K (previously exit 0) now exit 1 with the new messages; M/N/O/P/Q negatives fail; L control passes; `requestSchema` resolves indirection for event-body/draft consumers. Pending independent re-review disposition. |
| 02 — closed-object walk misses `type` arrays and boolean `true` (R3-02, 低) | 未修复 (residual) | **FIXED** — probes H/I (previously exit 0) now exit 1; boolean `true` fails under property/items/prefixItems/additionalProperties (R/S/T); open `type: ['object']` fails (U); boolean-false and closed type-array controls pass (V/W). Pending independent re-review disposition. |

This provider does not review or accept its own changes; acceptance is reserved for the mandatory DeepSeek cross-provider re-review.

## 5. Validation commands and results

Run from `api/openapi/` unless noted. Node v22.23.4 / npm 10.9.9. Pre-edit baseline: stock `check:s1` exit 0, `node --check` exit 0, staged empty, HEAD `0c2d2ca23d16b8c32999e95a1321184d1c78df24`.

| Command | Exit | Key output |
| --- | ---: | --- |
| `node --check contract-rules.mjs` | 0 | `NODE_CHECK_OK` |
| `npm run check:s1` (post-edit) | 0 | Redocly `Woohoo! Your API description is valid.` / 0 problems; bundle/generate/strict `tsc` pass; `operations=14 customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`; `expected=14 expected_counts=7/7/0/0/0`; doc/A0 zeros; `servers=1 relative_root=true`; `PASS check-doc-map (s1)` |
| `python3 /tmp/onecall-s1-fix3-probes/run_fix3_probes.py` | 0 | `ALL FIX3 PROBES PASS` (19 lint cases) |
| `python3 /tmp/onecall-s1-fix-probes/run_probes.py` | 0 | `ALL 14 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix2-probes/run_fix2_probes.py` | 0 | `ALL FIX2 PROBES PASS` |
| `npm run generate` twice + `sha256sum ../../tmp/openapi-client/onecall-v1.d.ts` | 0 | both `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` (identical) |
| `sha256sum onecall-v1.openapi.yaml` | 0 | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` (unchanged) |
| `git diff --check` on allowed paths | 0 | no output; both paths are untracked, so paired with the independent UTF-8 scan below |
| UTF-8 / U+FFFD / CR / trailing-whitespace / conflict-marker scan (edited paths) | 0 | `SCANS: CLEAN` |
| `git diff --cached --stat` | 0 | empty; no staging |
| `git rev-parse HEAD` | 0 | `0c2d2ca23d16b8c32999e95a1321184d1c78df24` (accepted plan base) |
| Full-tree sha256 diff vs pre-task baseline (501 files, excluding `.git`/`node_modules`/`tmp`) | 0 | after code+report edits: only `api/openapi/contract-rules.mjs` differed; final post-artifact re-run verified exactly the three §2 paths (protocol per fix-2: this file's own hash recorded after write) |

### fix-3 probe detail (all exit codes are `redocly lint`)

| Probe | Exit | Expectation met |
| --- | ---: | --- |
| `stock-lint` | 0 | canonical contract still green |
| `A-requestBody-ref-inline-open-accountId` | 1 | `must be a $ref to a component schema` + `additionalProperties false` + `trusted fact accountId` (was 0) |
| `B-media-no-schema` | 1 | `request body media entry application/json must declare a schema` (was 0) |
| `H-boolean-schema-property` | 1 | `request schema boolean true is forbidden` (was 0) |
| `I-type-array-open-object` | 1 | `request object schema must set additionalProperties false` for `type: [object, 'null']` (was 0) |
| `K-requestBody-empty-content` | 1 | `request body must declare a non-empty content map` (was 0) |
| `M-requestBody-ref-missing-target` | 1 | `requestBody must resolve to a request body object` |
| `N-requestBody-ref-to-schema` | 1 | non-requestBody `$ref` target → `non-empty content map` |
| `O-media-non-schema-ref` | 1 | `#/components/responses/...` media ref → `must be a $ref to a component schema` |
| `P-multi-media-one-missing-schema` | 1 | `request body media entry text/plain must declare a schema` |
| `Q-media-null-entry` | 1 | `request body media entry application/json must be an object` |
| `R`/`S`/`T` boolean true under items/prefixItems/additionalProperties | 1 each | `boolean true is forbidden` |
| `U-type-array-object-only-open` | 1 | `type: ['object']` without closure fails |
| `L-requestBody-ref-closed-control` | 0 | valid requestBody `$ref` → closed `ClaimRequest` passes |
| `V-boolean-false-positions` | 0 | property/items boolean `false` pass |
| `W-type-array-object-closed` | 0 | `type: [object, 'null']` + `additionalProperties: false` passes |
| `V2-bool-false-prefixItems-tool-constraint` | 1 | only Redocly `struct rule` rejects the boolean; no onecall-rule message (tooling boundary, see §7) |

### check:s1 counts (final run)

```
operations=14 customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14
expected=14 expected_counts=7/7/0/0/0
doc_rows=14 doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0
a0_catalog=44 a0_missing=0 a0_extra=0 a0_duplicate=0 a0_invalid=0
paths_unversioned=0 paths_multi_surface=0 duplicate_operation_ids=0
servers=1 relative_root=true
source_duplicate_paths=0 source_duplicate_routes=0 source_duplicate_operation_ids=0
PASS check-doc-map
PASS check-doc-map (s1)
```

## 6. Artifact hashes (post-fix-3)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/contract-rules.mjs` | `f329be9731970ece338ac1d62d30775a79f25f66e6284114a548a506509f068b` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | `50391bcae56dae933e598cc29485d8c8513036fbdf6da78ba55f2dba4ebc1771` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-3.md` | (this file; hash recorded after write) |
| `tmp/openapi-client/onecall-v1.d.ts` (generated, not committed) | `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` |

### Unchanged forbidden targets (re-verified against the fix-2 post-fix hashes / pre-task baseline)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` |
| `api/openapi/redocly.yaml` | `c76985dcf26c1eba0da024411be0817694859a726787d049e65b9231bc6c781a` |
| `api/openapi/check-doc-map.mjs` | `dfd9d98a52d9666fa4f44fb76556cd964299f4d416d5c899f1fee67eabc83eea` |
| `api/openapi/client-compat.ts` | `414c8062bf52ae6ea383ab73eb2541d567c5d9f65e4282b3e00a64a3ad1f9262` |
| `api/openapi/package.json` | `9fe0622adc7007d22b010254f3ab029d188a0ef5ac6c9bc7718a0196f71ab7f6` |
| `api/openapi/package-lock.json` | `e40eda3a6f969cf2219970a03711147b65803e25ef12ee19dc6264d6e25dfa52` |
| `api/openapi/tsconfig.client-compat.json` | `1b1cea2458ddd4fa069d02f3da9a1cc239ed06db31bcefe2096232e9214f86fe` |
| `docs/15-production-openapi-surface-spec.md` | `34e49f2b612238929f190baa6b0c0e980f69ba51fc33a4d20a9d5c7069aa155f` |
| `docs/09`–`docs/14` | unchanged (full-tree sha256 diff: no delta) |
| `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` | `fe5546533be2f606ecb15d689732d52bed0203392e38fdf4a045b4f21f683a0d` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-1.md` | `9415859ad62532f1ed3833053a89b420e6528c38af44cf5da79cca6788c66726` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-2.md` | `fd75788803a742704439c61ec99c0e11fc1b9a49884ba36d9144710c159470f1` |
| `docs/reviews/code-review-20261023-234505.md` | `db949396c7af2dfefc90837b264d335f25b1ef85fd8d8294f1c51c1cc0780e47` |
| `docs/reviews/code-review-20261024-001741.md` | `6c3282f9466a0bbfdb3f95bcf2450454a5c788bac82d4c94be6921ae619e1d4d` |
| `docs/reviews/code-review-20261024-020734.md` | `5621b2c74959cb774aad01d441ceff1e65026a1e456cfe8575c6b8e47d26bb66` |
| `docs/reviews/code-review-20261024-032551.md` | `048dab56c2da315aa6fc169a4aea8e8015177e670d872bbc515437d84e7712ee` |
| `docs/reviews/plan-review-20261021-205118.md` | `7cef564d0fbde72ac9e44dd95e667fde5988601e714be2dd5a1728d4a9738495` |
| `docs/reviews/plan-review-20261023-230300.md` | `42cf7f9dec9cc4afb8e62fcb4b17eb2b0192b2abccad7e69522ad7a4e2ea639f` |
| `docs/gateflow/project-execution-control.md` | `96b32f9e92ad626a714a02cbb0a87792cceea491b9cb0c2bd3c91158e70e6515` (pre-existing dirty at task start; full-tree before/after diff proves fix 3 did not touch it) |

Git HEAD (unchanged): `0c2d2ca23d16b8c32999e95a1321184d1c78df24`. `git diff --cached` empty.

## 7. Residual risks

| Residual | Impact | Owner / destination |
| --- | --- | --- |
| Static-only evidence | No runtime/database/auth/payment/IVR proof; unchanged static mandate | Backend/QA runtime slices |
| `x-onecall-audience` not bound to path-derived surface | Governance metadata drift remains an Open Question | API Governance/Security; later A1 aggregate review |
| `patternProperties` / `if`/`then`/`else` / `dependentSchemas` / `unevaluatedProperties` not traversed or rejected in request schemas | Non-enumerated JSON Schema keywords remain outside the lightweight walker | API Validation; later A1 aggregate review policy decision |
| Redocly 1.34.3 `struct` rule rejects boolean schemas inside `prefixItems` (JSON Schema 2020-12 allows them; the onecall walker accepts boolean `false` there) | Tooling boundary stricter than contract policy; deterministic | A1 validation author; revisit on Redocly upgrade |
| Event-body `required` check still reads raw `operation.requestBody.required` | Fail-closed false rejection for a future `$ref`'d event-body; not a bypass | Later event-body slice may adopt `resolveRequestBody` |
| `$ref` sibling policy remains reject-not-merge; stage checker (`check-doc-map.mjs`) still does not inspect request bodies | Documented deterministic boundaries, out of fix-3 write scope | Later accepted slices / aggregate review |
| Nested arrays without `maxItems`; Redocly major-upgrade revalidation; future 475/512 headers; future public-share common-error registration; `R-A0-O20-PUBLISHER`; docs/14 catalog-parser dependency | Prior classified residuals unchanged | As previously assigned |
| No full transitive dependency/vulnerability audit | Out of scope | A1 validation author |

## 8. Scope confirmation

- Only the three paths in §2 were created or modified for this fix; full-tree sha256 before/after comparison proves no other repository file changed (excluding ignored `node_modules`/`tmp` side effects).
- No forbidden path changed; all listed baseline hashes re-verified.
- No staging, commit, push, PR, merge, controller edit, review edit, re-review or S2 work was performed.
- No S2/S3 operations were added; counts remain `7/7/0/0/0=14`; problem registry remains exactly 15 codes; OpenAPI hash unchanged.
- Temporary probes remain under `/tmp/onecall-s1-fix3-probes/`; no repository fixture files were created.

## 9. Completion

**COMPLETED** — R3-01 and R3-02 applied within the three-path write boundary; mandatory validation green (`node --check` 0, `check:s1` 0, fix-1/fix-2/fix-3 probe suites 0, repeatable generated-type hash, canonical OpenAPI hash unchanged, scope/scans clean, HEAD and staging untouched). This provider stops here, before re-review; acceptance is reserved for the independent DeepSeek cross-provider review and the controller.
