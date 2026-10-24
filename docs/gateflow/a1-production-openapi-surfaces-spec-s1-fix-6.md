# A1 S1 bounded-fix-6 artifact

> Task label: `a1-openapi-s1-fix6-mimo26flash-20261024-01`
>
> Provider: Claude Code / `mimo26flash` / `mimo-v2.6-flash`
>
> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: fix — bounded fix round 6 after independent DeepSeek re-review 5
>
> Accepted plan base: `0c2d2ca23d16b8c32999e95a1321184d1c78df24`
>
> Binding re-review: `docs/reviews/code-review-20261024-093027.md` (SHA-256 `861fdbd4148eaaf3d8b4d308dade7be5bb18c6357899261bb110ffd6a37058a0`)
>
> Status: COMPLETED (implementation complete; awaiting independent cross-provider re-review — this provider does not self-accept)

## 1. Scope

Applied only the controller-accepted R6-01 correction from `docs/reviews/code-review-20261024-093027.md` (Finding 01): request `type: array` schemas without `items` — and `prefixItems` schemas without a closing `items` — leave array elements (or the implicit tail) unconstrained, so a request array could accept arbitrary element objects carrying server-derived trusted facts.

Preserved all previously accepted A–G, R2-01/R2-02, R3-01/R3-02, R4-01/R4-02, F5-01/F5-02 fixes, the exact 14-operation S1 contract `C1,C2,C3,C4,C5,C13,C14` + `O1..O7`, OpenAPI semantics, generated types and counts `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`. Static validator hardening only; the canonical 14-operation OpenAPI contract is unchanged (SHA-256 still `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf`).

### Explicitly rejected/deferred (unchanged non-goals)

- No generic JSON Schema engine and no claim of complete meta-schema validation.
- No policy for `patternProperties`, `if`/`then`/`else`, `dependentSchemas`, `not` or `unevaluatedProperties` (controller-deferred).
- Formal empty-`type`-array validation (`type: []`) remains aggregate-review scope, even though the fail-closed admission predicate now prevents its array bypass (an empty type array does not definitively exclude arrays, so `items` is required).
- No exact audience binding; no event-body raw `required` change; no multi-media first-schema semantics; no runtime concerns.
- No OpenAPI operation/schema/route/response/count change, no `docs/15` edit, no `check-doc-map.mjs`/`redocly.yaml`/`client-compat.ts`/package/config change.
- No prior artifact, reviewer evidence, controller, plan, prototype, screenshot or shared-JS edit.
- No stage, commit, push, PR, merge, re-review or S2 entry.

## 2. Changed paths (exactly three)

| # | Path | Role |
| --- | --- | --- |
| 1 | `api/openapi/contract-rules.mjs` | R6-01 strict request-array admission in `walkRequestSchema` |
| 2 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | Cumulative report updated truthfully for fix 6 |
| 3 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-6.md` | This fix artifact (created last) |

Temporary negative/control fixtures live only under `/tmp/onecall-s1-fix6-probes/` (plus the reused `/tmp/onecall-s1-fix-probes/`, `/tmp/onecall-s1-fix2-probes/`, `/tmp/onecall-s1-fix3-probes/`, `/tmp/onecall-s1-fix4-probes/` and `/tmp/onecall-s1-fix5-probes/` suites).

## 3. Controller decision → applied change

### R6-01 — request-array admission closure (parallel to F5-01 object admission)

- `walkRequestSchema` now computes `typeDefinitivelyExcludesArray` exactly parallel to `typeDefinitivelyExcludesObject`: a string type other than `'array'`, or a **non-empty** type array without `'array'`, definitively excludes arrays. Any other traversed non-boolean node (absent type, invalid type, empty type array, type array containing `'array'`, untyped terminals) is array-admitting and must declare the `items` keyword.
- Presence is checked by **own-property** semantics (`Object.prototype.hasOwnProperty.call(node, 'items')`), not truthiness, so `items: false` is an allowed closed/nothing form while an absent `items` fails closed with `request array schema must declare items`.
- `prefixItems` children are still traversed when the parent is array-admitting, but declaring `prefixItems` does not close the remaining implicit `items: true` tail — `items` is still required.
- A schema-valued `items` is traversed (so open `items: {}` fails the object-closure rule on the subschema and trusted facts in `items` are still reported). `items: true` remains rejected by the pre-existing boolean-true invariant; `items: false` remains an allowed closed/nothing schema.
- When the parent type definitively excludes arrays, the walker no longer descends into `items`/`prefixItems` subschemas (dead contract surface for a non-array parent) — the same dead-keyword principle fix-5 applies to `additionalProperties` under a non-object parent. This keeps explicit non-array parents carrying dead array keywords (with trusted facts or open schemas inside) accepted, exactly as required control.
- Cycle-safe pure `$ref` chains, legitimate closed recursion, every-hop `$ref`-sibling rejection (F5-02) and the F5-01 object-closure rule are untouched.
- Canonical impact: none. The three request components (`ClaimRequest`, `ActivationRequest`, `ActivationTransitionRequest`) are flat `type: object` + `additionalProperties: false` with only typed `string`/`integer` leaves — every leaf type definitively excludes arrays, so no `items` declaration is required anywhere in the canonical request surface. `npm run check:s1` stays exit 0 and the OpenAPI hash is unchanged.

### Disclosed consequence: untyped terminals need both closure and `items`

An untyped schema is simultaneously object-admitting (F5-01) and array-admitting (R6-01), so a fully-passing untyped terminal must declare both `additionalProperties: false` and `items` (for example `items: false`). The fix-4 control fixture `c1-bare-additionalProperties-false` (bare `{additionalProperties: false}`) is therefore no longer a fully-passing shape: it still satisfies the object-closure rule (no closure message) but now fails with `request array schema must declare items`. This is an inherent, disclosed consequence of the accepted rule text ("untyped array-admitting terminals must fail closed unless they explicitly declare `items`"), not a rule regression. Supplementary probes in `/tmp/onecall-s1-fix6-probes/run_fix6_c1_scope.py` scope the delta: bare `{additionalProperties: false}` exits 1 with ONLY the array message; `{additionalProperties: false, items: false}` exits 0; typed `type: object` + `additionalProperties: false` still exits 0.

## 4. Accepted finding status (Gateflow terms)

| Finding | Review status (20261024-093027) | Fix-6 status |
| --- | --- | --- |
| 01 — request `type: array` schemas without `items` leave every element value unconstrained (R6-01, 低) | 未修复 | **FIXED** — `type: array` without `items` (including `maxItems`-only and nested variants), `prefixItems` without closing `items` (including nested), a 21-hop `$ref` chain ending in an open array, `type: ['array','null']` without `items`, an untyped terminal `{additionalProperties: false}` without `items`, and open schema-valued `items: {}` all exit 1 with `request array schema must declare items` (or the closure message for the walked `items: {}` subschema); trusted facts inside schema-valued `items` are still reported. `items: false`, typed scalar `items`, closed object `items`, `prefixItems` + `items: false`, dead `items`/`prefixItems` under an explicit non-array parent, pure finite chains ending in a closed array and legitimate closed recursion all stay exit 0. Pending independent re-review disposition. |

This provider does not review or accept its own changes; acceptance is reserved for the mandatory DeepSeek cross-provider re-review.

## 5. Validation commands and results

Run from `api/openapi/` unless noted. Node v22.23.4 / npm 10.9.9. Pre-edit baseline: stock `check:s1` exit 0, `node --check` exit 0, staged empty, HEAD `0c2d2ca23d16b8c32999e95a1321184d1c78df24`.

| Command | Exit | Key output |
| --- | ---: | --- |
| `node --check contract-rules.mjs` | 0 | `NODE_CHECK_OK` |
| `npm run check:s1` (post-edit) | 0 | Redocly valid / 0 problems; bundle/generate/strict `tsc` pass; `operations=14 customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`; `expected=14 expected_counts=7/7/0/0/0`; doc/A0 zeros; `servers=1 relative_root=true`; `PASS check-doc-map (s1)` |
| `python3 /tmp/onecall-s1-fix6-probes/run_fix6_probes.py` | 0 | `ALL FIX6 PROBES PASS` (18 lint cases; detail below) |
| `python3 /tmp/onecall-s1-fix6-probes/run_fix6_c1_scope.py` | 0 | `ALL C1 SCOPE PROBES PASS` (3 cases; c1 delta scoping) |
| `python3 /tmp/onecall-s1-fix-probes/run_probes.py` | 0 | `ALL 14 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix2-probes/run_fix2_probes.py` | 0 | `ALL FIX2 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix3-probes/run_fix3_probes.py` | 0 | `ALL FIX3 PROBES PASS` (incl. `V2-bool-false-prefixItems-tool-constraint`: still exit 1, no listed onecall message, `struct`-only — the new array-admission message is outside its `rule_msgs` allowlist and reflects the fixture's missing `items`, not a boolean-false rejection) |
| `python3 /tmp/onecall-s1-fix4-probes/run_fix4_probes.py` | 1 | 22 of 23 pass; sole delta is control `c1-bare-additionalProperties-false` (see §3): R4-01 negatives s1/s2/s4/s7/s8/s9, r4-21/50-hop chains, x1/x2/x3 and controls c2–c7, nr-s3/s5/s6 all keep their expected outcomes; the c1 object-closure semantics are proven unchanged by the c1-scope probes |
| `python3 /tmp/onecall-s1-fix5-probes/run_fix5_probes.py` | 0 | `ALL FIX5 PROBES PASS` (18 lint cases) |
| `npm run generate` twice + `sha256sum ../../tmp/openapi-client/onecall-v1.d.ts` | 0 | both `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` (identical) |
| `sha256sum onecall-v1.openapi.yaml` | 0 | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` (unchanged) |
| `git diff --check` (in-scope paths) | n/a for untracked fix paths | no output on the authorized paths |
| UTF-8 / U+FFFD / CR / trailing-whitespace / conflict-marker / tab scan (edited paths) | 0 | `issues= NONE` for `contract-rules.mjs` and the implementation report; re-run for this artifact after write (same scan) |
| `git diff --cached --stat` | 0 | empty; no staging |
| `git rev-parse HEAD` | 0 | `0c2d2ca23d16b8c32999e95a1321184d1c78df24` (accepted plan base) |

### fix-6 probe detail (all exit codes are `redocly lint`)

| Probe | Exit | Expectation met |
| --- | ---: | --- |
| `stock-lint` | 0 | canonical contract still green |
| `array-no-items` | 1 | `type: array` + `maxItems` without `items` → `must declare items` (controller probe; was 0) |
| `prefix-items-no-items` | 1 | `prefixItems` without closing `items` → `must declare items` (was 0) |
| `nested-array-no-items` | 1 | closed items object containing an open nested array → `must declare items` (was 0) |
| `nested-prefix-no-items` | 1 | nested `prefixItems` without closing `items` → `must declare items` (was 0) |
| `long-chain-open-array` | 1 | 21-hop pure `$ref` chain → `type: array` without `items` → `must declare items` (was 0) |
| `type-array-contains-array-no-items` | 1 | `type: ['array','null']` without `items` → `must declare items` (was 0) |
| `untyped-closed-no-items` | 1 | untyped `{additionalProperties: false}` terminal via `$ref` → `must declare items` only (object closure satisfied; was 0) |
| `schema-items-open` | 1 | `items: {}` walked → closure message on the items subschema |
| `schema-items-trusted-fact` | 1 | trusted `accountId` inside schema-valued `items` still reported |
| `items-false` | 0 | `items: false` own-property closed/nothing control passes |
| `typed-scalar-items` | 0 | typed scalar `items` control passes |
| `closed-object-items` | 0 | closed object `items` control passes |
| `prefix-items-items-false` | 0 | `prefixItems` + `items: false` tail-closed control passes |
| `dead-items-prefix-nonarray` | 0 | `type: string` parent with dead `items`/`prefixItems` (trusted `accountId` inside) not traversed |
| `pure-chain-closed-array` | 0 | 21-hop pure chain → closed array with `items` passes |
| `legitimate-closed-recursion` | 0 | closed self-recursive object control passes |

### c1-scope probe detail

| Probe | Exit | Expectation met |
| --- | ---: | --- |
| `c1r-bare-ap-false-only-array-message` | 1 | bare `{additionalProperties: false}` fails ONLY with the array-admission message; object-closure rule silent (R4-01 semantics intact) |
| `c1r-untyped-ap-false-items-false` | 0 | fully closed untyped form `{additionalProperties: false, items: false}` passes |
| `c1r-typed-closed-object` | 0 | typed closed object unchanged |

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

## 6. Artifact hashes (post-fix-6)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/contract-rules.mjs` | `41122e31c05193dbb5d7aa7d8fa6a9c16483e9560068bb5596265e36cc2d7fbc` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | `3355840437199ae40ccb94aeb4960ec3cebd634504079d21dbf574ac6fc259bf` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-6.md` | (this file; hash recorded after write) |
| `tmp/openapi-client/onecall-v1.d.ts` (generated, not committed) | `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` |

### Unchanged forbidden targets (re-verified against the fix-5 post-artifact baselines)

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
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-1.md` | `9415859ad62532f1ed3833053a89b420e6528c38af44cf5da79cca6788c66726` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-2.md` | `fd75788803a742704439c61ec99c0e11fc1b9a49884ba36d9144710c159470f1` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-3.md` | `dcda3024ddc01df622d2d9e8d1126192aefe22586f92c1e4da99fb04ab2b4cd2` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-4.md` | `d34ded989ae415917b1de4cc94017c088729958cab829dcf54964a6fb4cb2604` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-5.md` | `01ac68ef8214318ac105713d5f70be5f0f0166f8abe71a1a3abe0f43c43c6207` |

Git HEAD (unchanged): `0c2d2ca23d16b8c32999e95a1321184d1c78df24`. `git diff --cached` empty. The binding re-review `docs/reviews/code-review-20261024-093027.md` (real SHA-256 `861fdbd4148eaaf3d8b4d308dade7be5bb18c6357899261bb110ffd6a37058a0`) was read only, not written by this provider.

## 7. Residual risks

| Residual | Impact | Owner / destination |
| --- | --- | --- |
| Static-only evidence | No runtime/database/auth/payment/IVR proof; unchanged static mandate | Backend/QA runtime slices |
| `x-onecall-audience` not bound to path-derived surface | Governance metadata drift remains an Open Question | API Governance/Security; later A1 aggregate review |
| Non-enumerated JSON Schema keywords (`patternProperties`, `if`/`then`/`else`, `dependentSchemas`, `not`, `unevaluatedProperties`) not traversed or rejected | Outside the lightweight walker by controller deferral | API Validation; later A1 aggregate review |
| R6-01 is a fail-closed contract rule, not full JSON Schema satisfiability | Untyped schemas now must declare both `additionalProperties: false` (F5-01) and `items` (R6-01); the fix-4 bare-`additionalProperties: false` control fixture is superseded as a fully-passing shape (object-closure semantics unchanged, scoped by the c1-scope probes) | A1 validation author; revisit only with an explicit controller policy change |
| Empty `type: []` remains an invalid-schema tooling gap | Not formally validated; the R6-01 predicate treats it as array-admitting (requires `items`) while F5-01 still treats it as object-excluding — no array bypass, formal validation deferred | API Validation; later A1 aggregate review |
| The walker does not descend into dead `items`/`prefixItems` subschemas when the parent type definitively excludes arrays | Trusted facts/boolean-true in such dead subschemas are not reported; semantically consistent with "items/prefixItems ignored for non-array instances"; required to keep explicit non-array parents with dead keyword surface accepted | A1 validation author; revisit if a future contract places meaningful keywords under a non-array-typed `items`/`prefixItems` |
| Wrong-component request property refs (e.g. to `#/components/responses/NotFound`) | Already exit 1 via Redocly `struct`; no custom component-kind machinery added (controller deferred) | API Validation; later A1 aggregate review |
| Generic `deref` retains the 20-resolution guard for non-request-schema consumers | Request-schema walking no longer depends on it; canonical consumers are shallow | A1 validation author if multi-hop chains appear outside request schemas |
| Event-body `required` check still reads raw `operation.requestBody.required` | Fail-closed false rejection for a future `$ref`'d event-body; not a bypass | Later event-body slice |
| Redocly 1.34.3 `struct` rejects boolean schemas inside `prefixItems`; pure `$ref` cycles rejected by Redocly `Self-referencing circular pointer` before the custom cyclic message | Deterministic tooling boundaries; validator outcome remains fail-closed | A1 validation author; revisit on Redocly upgrade |
| Nested arrays without `maxItems`; future 475/512 headers; public-share common-error registration; `R-A0-O20-PUBLISHER`; docs/14 catalog-parser dependency | Prior classified residuals unchanged | As previously assigned |
| No full transitive dependency/vulnerability audit | Out of scope | A1 validation author |

## 8. Scope confirmation

- Only the three paths in §2 were created or modified for this fix; recent-mtime listing and forbidden-path hash re-verification prove no other repository file was written by this task (excluding ignored `node_modules`/`tmp` generate side effects and controller-gate activity on the controller/review/prompt files).
- No forbidden path changed; all listed baseline hashes re-verified.
- No staging, commit, push, PR, merge, controller edit, review edit, re-review or S2 work was performed.
- No S2/S3 operations were added; counts remain `7/7/0/0/0=14`; problem registry remains exactly 15 codes; OpenAPI hash unchanged.
- Temporary probes remain under `/tmp/onecall-s1-fix6-probes/`; no repository fixture files were created.

## 9. Completion

**COMPLETED** — R6-01 applied within the three-path write boundary; mandatory validation green (`node --check` 0, `check:s1` 0, original/fix-2/fix-3/fix-5 suites 0, fix-6 suite 0, c1-scope 0, repeatable generated-type hash, canonical OpenAPI hash unchanged, scope/scans clean, HEAD and staging untouched; the fix-4 suite's single c1 control delta is the disclosed, controller-accepted consequence of R6-01 with its object-closure semantics proven intact). This provider stops here, before re-review; acceptance is reserved for the independent DeepSeek cross-provider review and the controller.
