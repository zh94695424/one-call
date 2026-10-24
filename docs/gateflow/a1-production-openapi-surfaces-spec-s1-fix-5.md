# A1 S1 bounded-fix-5 artifact

> Task label: `a1-openapi-s1-fix5-mimo26flash-20261024-01`
>
> Provider: Claude Code / `mimo26flash` / `mimo-v2.6-flash`
>
> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: fix — bounded fix round 5 after independent DeepSeek re-review
>
> Accepted plan base: `0c2d2ca23d16b8c32999e95a1321184d1c78df24`
>
> Binding re-review: `docs/reviews/code-review-20261024-061310.md`
>
> Status: COMPLETED (implementation complete; awaiting independent cross-provider re-review — this provider does not self-accept)

## 1. Scope

Applied only the two controller-accepted corrections from `docs/reviews/code-review-20261024-061310.md`:

- **F5-01** permissive/untyped request-schema terminals treated as harmless concrete schemas.
- **F5-02** intermediate-hop `$ref` siblings not inspected by the request-schema resolver.

Preserved all previously accepted A–G, R2-01/R2-02, R3-01/R3-02, R4-01/R4-02 fixes, the exact 14-operation S1 contract `C1,C2,C3,C4,C5,C13,C14` + `O1..O7`, OpenAPI semantics, generated types and counts `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`. Static validator hardening only; the canonical 14-operation OpenAPI contract is unchanged (SHA-256 still `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf`).

### Explicitly rejected/deferred (unchanged non-goals)

- No custom component-kind machinery for a request property referring to an existing `#/components/responses/NotFound` (already exits 1 via Redocly `struct`); no operation-level `requestBody` wrong-component-kind enforcement.
- No exact audience binding (API Governance/Security; later A1 aggregate review).
- No traversal/rejection of non-enumerated JSON Schema keywords (`patternProperties`, `if`/`then`/`else`, `dependentSchemas`, `not`, `unevaluatedProperties`).
- No event-body raw `required` change, no multi-media first-schema semantics, no runtime concerns.
- No generic JSON Schema engine and no claim of exhaustive schema satisfiability (F5-01 is a lightweight fail-closed contract rule).
- No OpenAPI operation/schema/route/response/count change, no `docs/15` edit, no `check-doc-map.mjs` edit, no `redocly.yaml`/`client-compat.ts`/package/config change.
- No prior artifact, reviewer evidence, controller, plan, prototype, screenshot or shared-JS edit.
- No stage, commit, push, PR, merge, re-review or S2 entry.

## 2. Changed paths (exactly three)

| # | Path | Role |
| --- | --- | --- |
| 1 | `api/openapi/contract-rules.mjs` | F5-01 strict request-contract closure; F5-02 every-hop `$ref`-sibling rejection in the resolver |
| 2 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | Cumulative report updated truthfully for fix 5 |
| 3 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-5.md` | This fix artifact (created last) |

Temporary negative/control fixtures live only under `/tmp/onecall-s1-fix5-probes/` (plus the reused `/tmp/onecall-s1-fix-probes/`, `/tmp/onecall-s1-fix2-probes/`, `/tmp/onecall-s1-fix3-probes/` and `/tmp/onecall-s1-fix4-probes/` suites).

## 3. Controller decisions → applied changes

### F5-01 — permissive untyped request-schema terminals

- `walkRequestSchema` object-admission is now strict: every traversed non-boolean request schema that does **not** carry a valid explicit type definitively excluding objects is treated as object-admitting and must set `additionalProperties: false`. The predicate collapses to `!typeDefinitivelyExcludesObject` (a string type other than `object`, or a type array without `object`, suppresses the check); the previous `properties` / `type: object` / object-containing-type-array / `openMap` disjunction is replaced.
- This intentionally requires untyped schemas and composition wrappers (allOf/anyOf/oneOf envelopes with no explicit type, `{}`/annotation-only terminals reached through a `$ref` chain) to declare closure, so a permissive terminal can no longer admit arbitrary JSON (including server-derived trusted facts) while the closure test stays silent. It is a lightweight fail-closed contract rule, not a full JSON Schema satisfiability check.
- Boolean `false` remains an allowed closed/nothing schema (handled before the closure check); boolean `true` remains rejected at every traversed position.
- The walker no longer descends into an `additionalProperties` subschema when the parent's type definitively excludes objects. That keyword is dead contract surface for such a parent (it cannot apply to any non-object instance), and descending into a dead `{}`/schema subschema would raise a false closure/trusted-fact positive on ignored keywords — the same "additionalProperties ignored for non-object instances" principle the R4-01 guard already applies to the parent's own closure check. This keeps the existing fix-4 `c3-type-array-excludes-object` control (`type: [string,'null']` + `additionalProperties: {}`) green.
- Canonical impact: none. The three request components (`ClaimRequest`, `ActivationRequest`, `ActivationTransitionRequest`) are flat `type: object` + `additionalProperties: false` with only typed `string`/`integer` leaves; `npm run check:s1` stays exit 0 and the OpenAPI hash is unchanged.

### F5-02 — intermediate-hop `$ref` siblings

- `resolveRequestSchemaRef` now rejects sibling keywords at **every** `$ref` hop (the entry node and every intermediate alias) under reject-not-merge, returning an explicit custom-rule problem (`request schema $ref must not carry sibling keywords: ...`). Sibling keywords apply under JSON Schema 2020-12, so an impure hop could otherwise smuggle trusted facts one hop away from the entry check.
- The walker's separate entry-node `$ref`-sibling check was removed so the resolver is the single semantic owner of this check; an entry-node sibling is therefore reported exactly once (no duplicate reporting). On a `{ problem }` the walker reports it and stops that branch (reject-not-merge — the target is not merged or walked as a valid schema).
- Pure finite alias chains, the existing cycle/external/missing handling and legitimate closed recursive object schemas are unchanged: a pure `$ref` node has no siblings, so the new check does not fire; recursion still terminates via the resolved-node `seen` set.
- Canonical impact: none. Every canonical request media schema is a single pure `$ref`, so the every-hop check is silent on the stock contract.

## 4. Accepted finding status (Gateflow terms)

| Finding | Review status (20261024-061310) | Fix-5 status |
| --- | --- | --- |
| 01 — request `$ref` chains accept permissive/non-schema local terminals as harmless concrete schemas (F5-01, 低) | 未修复 | **FIXED** — controller probe `empty-schema-terminal` (nested property → `{}`), `annotation-only-untyped-terminal` and `long-chain-permissive-untyped` (21-hop chain → `{}`) now exit 1 with `request object schema must set additionalProperties false`; `untyped-allof-wrapper` also fails; explicit `type: string`, a clean type-array-excluding-object, boolean `false`, closed objects, pure long chains ending at typed non-object/closed object and legitimate closed recursion all stay exit 0. Pending independent re-review disposition. |
| 02 (open question) — intermediate-hop `$ref` siblings not inspected by the resolver (F5-02) | 未修复 (open question) | **FIXED** — controller probe `intermediate-ref-sibling-trusted` (intermediate alias with trusted `properties.accountId` sibling) and `intermediate-ref-sibling-nonschema` (intermediate alias with a non-trusted `description` sibling) now exit 1 with `request schema $ref must not carry sibling keywords`; entry pure refs, multi-hop pure refs, long pure refs and recursive/mutual closed schemas all stay exit 0. Pending independent re-review disposition. |

This provider does not review or accept its own changes; acceptance is reserved for the mandatory DeepSeek cross-provider re-review.

## 5. Validation commands and results

Run from `api/openapi/` unless noted. Node v22.23.4 / npm 10.9.9. Pre-edit baseline: stock `check:s1` exit 0, `node --check` exit 0, staged empty, HEAD `0c2d2ca23d16b8c32999e95a1321184d1c78df24`.

| Command | Exit | Key output |
| --- | ---: | --- |
| `node --check contract-rules.mjs` | 0 | `NODE_CHECK_OK` |
| `npm run check:s1` (post-edit) | 0 | Redocly `Woohoo! Your API description is valid.` / 0 problems; bundle/generate/strict `tsc` pass; `operations=14 customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`; `expected=14 expected_counts=7/7/0/0/0`; doc/A0 zeros; `servers=1 relative_root=true`; `PASS check-doc-map (s1)` |
| `python3 /tmp/onecall-s1-fix5-probes/run_fix5_probes.py` | 0 | `ALL FIX5 PROBES PASS` (18 lint cases) |
| `python3 /tmp/onecall-s1-fix-probes/run_probes.py` | 0 | `ALL 14 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix2-probes/run_fix2_probes.py` | 0 | `ALL FIX2 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix3-probes/run_fix3_probes.py` | 0 | `ALL FIX3 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix4-probes/run_fix4_probes.py` | 0 | `ALL FIX4 PROBES PASS` (including the `c3-type-array-excludes-object` control, kept green by not descending into a dead `additionalProperties` subschema) |
| `npm run generate` twice + `sha256sum ../../tmp/openapi-client/onecall-v1.d.ts` | 0 | both `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` (identical) |
| `sha256sum onecall-v1.openapi.yaml` | 0 | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` (unchanged) |
| `git diff --check` (in-scope paths) | n/a for untracked fix paths | no output on the three fix paths; pre-existing trailing-whitespace noise only in the already-dirty tracked `logo/preview.html` (outside this task); paired with the independent UTF-8 scan below |
| UTF-8 / U+FFFD / CR / trailing-whitespace / conflict-marker / tab scan (edited paths) | 0 | `issues= NONE` for `contract-rules.mjs` and the implementation report |
| `git diff --cached --stat` | 0 | empty; no staging |
| `git rev-parse HEAD` | 0 | `0c2d2ca23d16b8c32999e95a1321184d1c78df24` (accepted plan base) |
| Forbidden-path SHA-256 re-verification (OpenAPI, redocly, check-doc-map, client-compat, package, lock, tsconfig, docs/15, fix-1/2/3/4 artifacts) | 0 | all match the fix-4 post-artifact baselines |

### fix-5 probe detail (all exit codes are `redocly lint`)

| Probe | Exit | Expectation met |
| --- | ---: | --- |
| `stock-lint` | 0 | canonical contract still green |
| `empty-schema-terminal` | 1 | nested property → `{}` → `must set additionalProperties false` (controller probe; was 0) |
| `annotation-only-untyped-terminal` | 1 | `{title, description}` terminal → closure (was 0) |
| `long-chain-permissive-untyped` | 1 | 21-hop chain → `{}` → closure (was 0) |
| `untyped-allof-wrapper` | 1 | untyped `allOf` envelope → closure |
| `intermediate-ref-sibling-trusted` | 1 | intermediate alias with trusted `properties.accountId` sibling → `must not carry sibling keywords` (controller probe; was 0) |
| `intermediate-ref-sibling-nonschema` | 1 | intermediate alias with `description` sibling → sibling message (was 0) |
| `explicit-type-string` | 0 | `type: string` control passes |
| `type-array-excludes-object` | 0 | `type: [string,'null']` control passes (clean; no dead `additionalProperties`) |
| `boolean-false` | 0 | boolean `false` property/items control passes |
| `closed-object` | 0 | closed typed object control passes |
| `long-chain-closed-object` | 0 | 21-hop chain → closed object control passes |
| `long-chain-typed-nonobject` | 0 | 21-hop chain → `type: string` control passes |
| `legitimate-closed-recursion` | 0 | closed self-recursive object control passes |
| `entry-pure-ref` | 0 | entry pure `$ref` control passes |
| `multi-hop-pure-ref` | 0 | multi-hop pure `$ref` chain control passes |
| `long-pure-ref` | 0 | 21-hop pure `$ref` chain control passes |
| `mutual-closed-recursion` | 0 | mutual closed recursion control passes |

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

## 6. Artifact hashes (post-fix-5)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/contract-rules.mjs` | `58b5da7fac70325471512c9244d66a7239de53449dd66a13deb479d9d551b1de` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | `036b6cba1ef39b219678651b9bb51a6bf9e2a6fd0db775962cfd1187d623d1eb` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-5.md` | (this file; hash recorded after write) |
| `tmp/openapi-client/onecall-v1.d.ts` (generated, not committed) | `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` |

### Unchanged forbidden targets (re-verified against the fix-4 post-artifact baselines)

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

Git HEAD (unchanged): `0c2d2ca23d16b8c32999e95a1321184d1c78df24`. `git diff --cached` empty. The binding re-review `docs/reviews/code-review-20261024-061310.md` (real SHA-256 `177da5f72cd3a516049a5d39d2e8baa07ebe70367548571ee73dc80cee38b28e`) was read only, not written by this provider.

## 7. Residual risks

| Residual | Impact | Owner / destination |
| --- | --- | --- |
| Static-only evidence | No runtime/database/auth/payment/IVR proof; unchanged static mandate | Backend/QA runtime slices |
| `x-onecall-audience` not bound to path-derived surface | Governance metadata drift remains an Open Question | API Governance/Security; later A1 aggregate review |
| Non-enumerated JSON Schema keywords (`patternProperties`, `if`/`then`/`else`, `dependentSchemas`, `not`, `unevaluatedProperties`) not traversed or rejected | Outside the lightweight walker by controller deferral | API Validation; later A1 aggregate review |
| F5-01 is a fail-closed contract rule, not full JSON Schema satisfiability | A legitimate composition that JSON Schema would accept but that omits `additionalProperties: false` on an untyped wrapper is now rejected; this is intentional (untyped wrappers must declare closure), not a validator bug | A1 validation author; revisit only with an explicit controller policy change |
| The walker does not descend into a dead `additionalProperties` subschema when the parent type excludes objects | Trusted facts/boolean-true in such a dead subschema are no longer reported; semantically consistent with "additionalProperties ignored for non-object instances" and required to keep the fix-4 `c3` control green; no probe relies on catching facts in dead subschemas | A1 validation author; revisit if a future contract places meaningful keywords under a non-object-typed `additionalProperties` |
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
- Temporary probes remain under `/tmp/onecall-s1-fix5-probes/`; no repository fixture files were created.

## 9. Completion

**COMPLETED** — F5-01 and F5-02 applied within the three-path write boundary; mandatory validation green (`node --check` 0, `check:s1` 0, fix-1/fix-2/fix-3/fix-4/fix-5 probe suites 0, repeatable generated-type hash, canonical OpenAPI hash unchanged, scope/scans clean, HEAD and staging untouched). This provider stops here, before re-review; acceptance is reserved for the independent DeepSeek cross-provider review and the controller.
