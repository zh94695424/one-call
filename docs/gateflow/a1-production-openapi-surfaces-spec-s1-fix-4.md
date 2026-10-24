# A1 S1 bounded-fix-4 artifact

> Task label: `a1-openapi-s1-fix4-mimo26flash-20261024-01`
>
> Provider: Claude Code / `mimo26flash` / `mimo-v2.6-flash`
>
> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: fix — bounded fix round 4 after independent DeepSeek re-review
>
> Accepted plan base: `0c2d2ca23d16b8c32999e95a1321184d1c78df24`
>
> Binding re-review: `docs/reviews/code-review-20261024-045057.md`
>
> Status: COMPLETED (implementation complete; awaiting independent cross-provider re-review — this provider does not self-accept)

## 1. Scope

Applied only the two controller-accepted low-severity R2-02 closure bypasses from `docs/reviews/code-review-20261024-045057.md`:

- **R4-01** untyped schema-valued `additionalProperties` open maps.
- **R4-02** silent dereference cutoff for long local `$ref` chains in the request walker.

Preserved all previously accepted A–G, R2-01/R2-02, R3-01/R3-02 fixes, the exact 14-operation S1 contract `C1,C2,C3,C4,C5,C13,C14` + `O1..O7`, OpenAPI semantics, generated types and counts `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`. Static validator hardening only; the canonical 14-operation OpenAPI contract is unchanged (SHA-256 still `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf`).

### Explicitly rejected/deferred (unchanged non-goals)

- No `requestBody` refs-to-`components.requestBodies` kind enforcement (wrong-component refs with response-like content remain API Validation / later A1 aggregate review).
- No change to the raw event-body `operation.requestBody.required` read (fail-closed false rejection belongs to the later event-body slice).
- No multi-media event/draft semantics.
- No `x-onecall-audience` surface binding (API Governance/Security; later A1 aggregate review).
- No traversal/rejection of `patternProperties`, `if`/`then`/`else`, `dependentSchemas`, `not`, `unevaluatedProperties` or other non-enumerated JSON Schema keywords (API Validation; later A1 aggregate review).
- No generic JSON Schema engine and no claim of exhaustive schema validation.
- No OpenAPI operation/schema/route/response/count change, no `docs/15` edit, no `check-doc-map.mjs` edit, no `redocly.yaml`/`client-compat.ts`/package/config change.
- No prior artifact, reviewer evidence, controller, plan, prototype, screenshot or shared-JS edit.
- No stage, commit, push, PR, merge, re-review or S2 entry.

## 2. Changed paths (exactly three)

| # | Path | Role |
| --- | --- | --- |
| 1 | `api/openapi/contract-rules.mjs` | R4-01 untyped open-map object admission; R4-02 cycle-safe request-schema `$ref` resolver |
| 2 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | Cumulative report updated truthfully for fix 4 |
| 3 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-4.md` | This fix artifact (created last) |

Temporary negative/control fixtures live only under `/tmp/onecall-s1-fix4-probes/` (plus the reused `/tmp/onecall-s1-fix-probes/`, `/tmp/onecall-s1-fix2-probes/` and `/tmp/onecall-s1-fix3-probes/` suites).

## 3. Controller decisions → applied changes

### R4-01 — untyped schema-valued `additionalProperties` open maps

- `walkRequestSchema` object-admission now treats a non-`false` `additionalProperties` as an open map that admits arbitrary keys, even when the node has no `properties` and no explicit `type: object`/object-containing `type` array.
- False-positive guard: when an explicit string `type` or `type` array definitively excludes `object`, `additionalProperties` is ignored for non-object instances and the open-map term is suppressed. Existing `properties` / `type: object` / object-containing type-array admission terms are unchanged.
- `{additionalProperties: false}` remains a valid closed form (no problem when it is the only admission signal).
- The `additionalProperties` subschema is still walked after the closure check, so a schema-valued map whose subschema declares trusted facts (for example `accountId`) still emits the existing trusted-fact message in addition to the closure message.
- Boolean `additionalProperties: true` fails both the closed-object policy and the boolean-`true` rule.

### R4-02 — silent dereference cutoff

- Added `resolveRequestSchemaRef(root, schema)`: resolves any finite local `#/...` `$ref` chain cycle-safely (seen-set on `$ref` strings) with **no** silent depth cutoff. Returns `{ node }` on success (node may be a boolean schema) or `{ problem }` when the chain is external, missing/null/non-schema, or cyclic.
- `walkRequestSchema` now uses that resolver instead of the generic 20-guard `deref` for request-schema nodes. On `{ problem }` it reports an explicit custom-rule message (`request schema $ref must resolve to a local schema…` / `request schema $ref does not resolve…` / `request schema $ref chain is cyclic…`) and stops that branch; an unresolved ref is never treated as a concrete schema.
- Legitimate recursive object schemas remain cycle-safe via the existing resolved-node `seen` set: a closed component object whose child `$ref` points back to itself (or mutual closed recursion) passes without hang.
- Request-schema `$ref` sibling rejection is preserved (siblings are still reported before resolution).
- The generic `deref` function is intentionally unchanged for existing consumers (`operationParameters`, `successSchema`, `resolveRequestBody` body resolution, cursor/limit schemas). Request-body indirection fail-closed behavior from fix 3 is unchanged.

## 4. Accepted finding status (Gateflow terms)

| Finding | Review status (20261024-045057) | Fix-4 status |
| --- | --- | --- |
| 01 — request closure misses schema-valued `additionalProperties` on untyped map nodes (R4-01, 低) | 未修复 | **FIXED** — review probes `s1`/`s2`/`s4` (previously exit 0) now exit 1 with `request object schema must set additionalProperties false`; `additionalProperties: true`, nested open maps and trusted-fact subschemas under open maps fail; type-exclusion, bare-`false` and long clean controls pass. Pending independent re-review disposition. |
| 02 — silent `deref` depth cutoff lets >20-resolution chains bypass the trusted-fact walker (R4-02, 低, review residual) | 未修复 (residual) | **FIXED** — 21-hop and 50-hop finite local chains ending in a closed `accountId` object now reach the terminal and fail with `trusted fact accountId`; closed trusted-fact-free long chains and closed self/mutual recursion controls pass; nested external and missing request-schema refs fail closed with explicit custom-rule messages. Pending independent re-review disposition. |

This provider does not review or accept its own changes; acceptance is reserved for the mandatory DeepSeek cross-provider re-review.

## 5. Validation commands and results

Run from `api/openapi/` unless noted. Node v22.23.4 / npm 10.9.9. Pre-edit baseline: stock `check:s1` exit 0, `node --check` exit 0, staged empty, HEAD `0c2d2ca23d16b8c32999e95a1321184d1c78df24`.

| Command | Exit | Key output |
| --- | ---: | --- |
| `node --check contract-rules.mjs` | 0 | `NODE_CHECK_OK` |
| `npm run check:s1` (post-edit) | 0 | Redocly `Woohoo! Your API description is valid.` / 0 problems; bundle/generate/strict `tsc` pass; `operations=14 customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`; `expected=14 expected_counts=7/7/0/0/0`; doc/A0 zeros; `servers=1 relative_root=true`; `PASS check-doc-map (s1)` |
| `python3 /tmp/onecall-s1-fix4-probes/run_fix4_probes.py` | 0 | `ALL FIX4 PROBES PASS` (23 lint cases) |
| `python3 /tmp/onecall-s1-fix-probes/run_probes.py` | 0 | `ALL 14 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix2-probes/run_fix2_probes.py` | 0 | `ALL FIX2 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix3-probes/run_fix3_probes.py` | 0 | `ALL FIX3 PROBES PASS` |
| `npm run generate` twice + `sha256sum ../../tmp/openapi-client/onecall-v1.d.ts` | 0 | both `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` (identical) |
| `sha256sum onecall-v1.openapi.yaml` | 0 | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` (unchanged) |
| `git diff --check` | n/a for untracked fix paths | no output on the three fix paths; pre-existing trailing-whitespace noise only in the already-dirty tracked `logo/preview.html` (outside this task); paired with the independent UTF-8 scan below |
| UTF-8 / U+FFFD / CR / trailing-whitespace / conflict-marker / tab scan (edited paths) | 0 | `SCANS: CLEAN` |
| `git diff --cached --stat` | 0 | empty; no staging |
| `git rev-parse HEAD` | 0 | `0c2d2ca23d16b8c32999e95a1321184d1c78df24` (accepted plan base) |
| Forbidden-path SHA-256 re-verification (OpenAPI, redocly, check-doc-map, client-compat, package, lock, tsconfig, docs/15, fix-1/2/3 artifacts, prior reviews, plan) | 0 | all match the fix-3 post-artifact baselines |
| Recent-mtime scope listing under the repo (excluding `.git`/`node_modules`/`tmp`) | 0 | only `api/openapi/contract-rules.mjs` attributable to this task among implementation paths; controller/review/prompt mtimes are controller-gate activity, not this task's writes |

### fix-4 probe detail (all exit codes are `redocly lint`)

| Probe | Exit | Expectation met |
| --- | ---: | --- |
| `stock-lint` | 0 | canonical contract still green |
| `s1-bare-additionalProperties-empty-schema` | 1 | `{additionalProperties: {}}` → `must set additionalProperties false` (was 0) |
| `s2-bare-additionalProperties-string` | 1 | `{additionalProperties: {type: string}}` → closure (was 0) |
| `s4-items-bare-additionalProperties` | 1 | bare map under `items` → closure (was 0) |
| `s7-bare-additionalProperties-true` | 1 | `additionalProperties: true` → closure + `boolean true is forbidden` |
| `s8-nested-items-open-map` | 1 | deep open map under array items → closure |
| `s9-open-map-with-trusted-fact-subschema` | 1 | open map subschema with `accountId` → closure + `trusted fact accountId` |
| `r4-21hop-closed-accountId` | 1 | 21-hop chain reaches terminal → `trusted fact accountId` (was 0) |
| `r4-50hop-closed-accountId` | 1 | 50-hop chain reaches terminal → `trusted fact accountId` |
| `r4-21hop-open-accountId` | 1 | 21-hop open terminal → trusted fact + closure |
| `x1-nested-external-ref` | 1 | nested external `$ref` → `must resolve to a local schema` |
| `x2-nested-missing-ref` | 1 | nested missing `$ref` → `does not resolve` |
| `x3-nested-pure-ref-cycle-tool-constraint` | 1 | pure `$ref` cycle rejected by Redocly `Self-referencing circular pointer` before the custom rule (documented tooling boundary; custom cyclic message not independently observable) |
| `c1-bare-additionalProperties-false` | 0 | untyped `additionalProperties: false` remains accepted |
| `c2-type-string-additionalProperties-object` | 0 | explicit `type: string` + `additionalProperties` → no closed-object false positive |
| `c3-type-array-excludes-object` | 0 | `type: [string, 'null']` + `additionalProperties: {}` → no false positive |
| `c4-21hop-closed-clean` | 0 | 21-hop closed trusted-fact-free control passes |
| `c5-50hop-closed-clean` | 0 | 50-hop closed trusted-fact-free control passes |
| `c6-legit-closed-recursive-object` | 0 | closed self-recursive object schema passes without hang |
| `c7-legit-mutual-closed-recursive` | 0 | closed mutual recursion passes without hang |
| `nr-s3-open-map-trusted-fact` | 1 | nonregression: open map declaring `accountId` still fails trusted fact |
| `nr-s5-bare-properties-empty` | 1 | nonregression: bare `properties: {}` still fails closure |
| `nr-s6-type-object-additionalProperties-empty` | 1 | nonregression: `type: object` + `additionalProperties: {}` still fails closure |

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

## 6. Artifact hashes (post-fix-4)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/contract-rules.mjs` | `bc4c1650390f8fe019ad33ebaaad237d839c768c140b18184ae7dda68e87d9a9` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | `620756245e3fc7c83f2e398c4f4bfc63b746e7248e7c32c04ae87b5ed5f2cd4f` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-4.md` | (this file; hash recorded after write) |
| `tmp/openapi-client/onecall-v1.d.ts` (generated, not committed) | `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` |

### Unchanged forbidden targets (re-verified against the fix-3 post-fix hashes / pre-task baseline)

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
| `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` | `fe5546533be2f606ecb15d689732d52bed0203392e38fdf4a045b4f21f683a0d` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-1.md` | `9415859ad62532f1ed3833053a89b420e6528c38af44cf5da79cca6788c66726` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-2.md` | `fd75788803a742704439c61ec99c0e11fc1b9a49884ba36d9144710c159470f1` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-3.md` | `dcda3024ddc01df622d2d9e8d1126192aefe22586f92c1e4da99fb04ab2b4cd2` |
| `docs/reviews/code-review-20261023-234505.md` | `db949396c7af2dfefc90837b264d335f25b1ef85fd8d8294f1c51c1cc0780e47` |
| `docs/reviews/code-review-20261024-001741.md` | `6c3282f9466a0bbfdb3f95bcf2450454a5c788bac82d4c94be6921ae619e1d4d` |
| `docs/reviews/code-review-20261024-020734.md` | `5621b2c74959cb774aad01d441ceff1e65026a1e456cfe8575c6b8e47d26bb66` |
| `docs/reviews/code-review-20261024-032551.md` | `048dab56c2da315aa6fc169a4aea8e8015177e670d872bbc515437d84e7712ee` |
| `docs/reviews/code-review-20261024-045057.md` | `8b719cab9f10d14f2bbffde031eeb44b558764693c520d24adce502fa09057f3` |
| `docs/reviews/plan-review-20261021-205118.md` | `7cef564d0fbde72ac9e44dd95e667fde5988601e714be2dd5a1728d4a9738495` |
| `docs/reviews/plan-review-20261023-230300.md` | `42cf7f9dec9cc4afb8e62fcb4b17eb2b0192b2abccad7e69522ad7a4e2ea639f` |

Git HEAD (unchanged): `0c2d2ca23d16b8c32999e95a1321184d1c78df24`. `git diff --cached` empty. Controller document was updated by the controller gate (fix-4 adjudication) before this task and was not written by this provider.

## 7. Residual risks

| Residual | Impact | Owner / destination |
| --- | --- | --- |
| Static-only evidence | No runtime/database/auth/payment/IVR proof; unchanged static mandate | Backend/QA runtime slices |
| `x-onecall-audience` not bound to path-derived surface | Governance metadata drift remains an Open Question | API Governance/Security; later A1 aggregate review |
| `patternProperties` / `if`/`then`/`else` / `dependentSchemas` / `unevaluatedProperties` not traversed or rejected in request schemas | Non-enumerated JSON Schema keywords remain outside the lightweight walker | API Validation; later A1 aggregate review policy decision |
| Redocly 1.34.3 `struct` rule rejects boolean schemas inside `prefixItems` | Tooling boundary stricter than contract policy; deterministic | A1 validation author; revisit on Redocly upgrade |
| Redocly 1.34.3 rejects pure `$ref` cycles with `Self-referencing circular pointer` before the custom rule can report the cyclic message | Validator outcome is still fail-closed rejection; legitimate closed recursive *object* schemas remain accepted by both toolchain and custom rule | A1 validation author; revisit on Redocly upgrade |
| Generic `deref` retains the 20-resolution guard for non-request-schema consumers | Request-schema walking no longer depends on it; canonical consumers are shallow | A1 validation author if multi-hop chains appear outside request schemas |
| Event-body `required` check still reads raw `operation.requestBody.required` | Fail-closed false rejection for a future `$ref`'d event-body; not a bypass | Later event-body slice may adopt `resolveRequestBody` |
| Wrong-component `requestBody` refs with a `content` map | Deferred by controller; pure closed media and trusted-fact invariants still hold | API Validation; later A1 aggregate review |
| `$ref` sibling policy remains reject-not-merge; stage checker (`check-doc-map.mjs`) still does not inspect request bodies | Documented deterministic boundaries, out of fix-4 write scope | Later accepted slices / aggregate review |
| Nested arrays without `maxItems`; Redocly major-upgrade revalidation; future 475/512 headers; future public-share common-error registration; `R-A0-O20-PUBLISHER`; docs/14 catalog-parser dependency | Prior classified residuals unchanged | As previously assigned |
| No full transitive dependency/vulnerability audit | Out of scope | A1 validation author |

## 8. Scope confirmation

- Only the three paths in §2 were created or modified for this fix; recent-mtime listing and forbidden-path hash re-verification prove no other repository file was written by this task (excluding ignored `node_modules`/`tmp` generate side effects and controller-gate activity on the controller/review/prompt files).
- No forbidden path changed; all listed baseline hashes re-verified.
- No staging, commit, push, PR, merge, controller edit, review edit, re-review or S2 work was performed.
- No S2/S3 operations were added; counts remain `7/7/0/0/0=14`; problem registry remains exactly 15 codes; OpenAPI hash unchanged.
- Temporary probes remain under `/tmp/onecall-s1-fix4-probes/`; no repository fixture files were created.

## 9. Completion

**COMPLETED** — R4-01 and R4-02 applied within the three-path write boundary; mandatory validation green (`node --check` 0, `check:s1` 0, fix-1/fix-2/fix-3/fix-4 probe suites 0, repeatable generated-type hash, canonical OpenAPI hash unchanged, scope/scans clean, HEAD and staging untouched). This provider stops here, before re-review; acceptance is reserved for the independent DeepSeek cross-provider review and the controller.
