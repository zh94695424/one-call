# A1 S1 bounded-fix-2 artifact

> Task label: `a1-openapi-s1-fix2-mimo26flash-20261024-01`
>
> Provider: Claude Code / `mimo26flash` / `mimo-v2.6-flash`
>
> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: fix — bounded fix round 2 after independent DeepSeek re-review
>
> Accepted plan base: `0c2d2ca23d16b8c32999e95a1321184d1c78df24`
>
> Binding re-review: `docs/reviews/code-review-20261024-020734.md`
>
> Status: COMPLETED

## 1. Scope

Applied only the controller-accepted R2-01 (surface path ownership), R2-02 (closed recursive request schemas) and the durable docs/15 wording clarification. Preserved all previously accepted A–G fixes, the exact 14-operation S1 contract `C1,C2,C3,C4,C5,C13,C14` + `O1..O7`, OpenAPI semantics, generated types and counts `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`. Static validator/document hardening only.

### Explicitly rejected/deferred (unchanged)

- No OpenAPI contract, problem registry, response status, operation inventory, route, schema or security declaration change.
- No public-share error-surface addition in S1 (S3 coordination).
- No duplication of all canonical registry statuses as validator constants.
- No nested `maxItems`, runtime behavior, Redocly upgrade, future concrete 475/512 headers or `R-A0-O20-PUBLISHER` work.
- No prior artifact, reviewer evidence, controller, plan, docs/09–14, prototype, screenshot or shared-JS edit.
- No stage, commit, push, PR, merge, re-review or S2 entry.

## 2. Changed paths (exactly five)

| # | Path | Role |
| --- | --- | --- |
| 1 | `api/openapi/contract-rules.mjs` | R2-01 path→surface ownership; R2-02 closed recursive request schemas |
| 2 | `api/openapi/check-doc-map.mjs` | R2-01 independent path-derived surface counts/docs comparison |
| 3 | `docs/15-production-openapi-surface-spec.md` | Durable static-scope wording (line 19) |
| 4 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | Post-fix-2 report update |
| 5 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-2.md` | This fix artifact (created last) |

Temporary negative fixtures live only under `/tmp/onecall-s1-fix2-probes/` and `/tmp/onecall-s1-fix-probes/`.

## 3. Controller decisions → applied changes

### R2-01 — surface path ownership

- Added an explicit stable prefix-to-surface map in both validators covering all five surfaces: `/api/v1/customer/`→`customer`, `/api/v1/operations/`→`operations`, `/api/v1/ivr/`→`ivr`, `/api/v1/public/shares/`→`public-share`, `/api/v1/callbacks/`→`provider-callback`.
- `contract-rules.mjs` `onecall/versioned-paths`: once a route matches exactly one approved prefix, `x-onecall-surface` must equal the mapped surface. Existing tag/audience/security checks remain tied to that validated surface. Rule covers all five surfaces and later slices; no hard-coded S1 operation pairs.
- `check-doc-map.mjs`: independently derives each operation's path surface; a route/extension mismatch is an explicit error (`does not own path surface`); stage surface counts use the path-derived surface; docs/15 rows are compared against the path-derived surface so a coordinated extension+docs row swap cannot hide the route mismatch.

### R2-02 — closed recursive request schemas

- Every request-body media schema must be a pure component-schema `$ref` (`#/components/schemas/...`). Inline/open request objects fail even if no forbidden property is declared.
- Any request-schema `$ref` node carrying sibling keywords is rejected (not silently dropped).
- Every traversed request object node must set `additionalProperties: false`.
- Walker now recursively traverses `properties`, `items`, `prefixItems`, `allOf`, `anyOf`, `oneOf` and schema-valued `additionalProperties`, resolving component refs cycle-safely (seen-set on resolved nodes).
- Exact-name `TRUSTED_REQUEST_FACT_NAMES` matching and legitimate identifiers preserved. Request validation remains separate from response secrecy/redaction ownership.
- No generic policy engine and no new rule name: still folded into enabled `onecall/sensitive-fields`; `redocly.yaml` untouched.

### Docs clarification

- `docs/15-production-openapi-surface-spec.md` line 19 replaced the volatile historical gate statement with the durable statement: 本文与配套静态校验不构成暂存、提交、推送、PR 或运行时生产就绪声明。

## 4. Validation commands and results

Run from `api/openapi/` unless noted. Node v22.23.4 / npm 10.9.9.

| Command | Exit | Key output |
| --- | ---: | --- |
| `node --check contract-rules.mjs` | 0 | `NODE_CHECK_RULES_OK` |
| `node --check check-doc-map.mjs` | 0 | `NODE_CHECK_MAP_OK` |
| `npm run check:s1` | 0 | Redocly 0 problems; bundle/generate/strict `tsc` pass; `operations=14 customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`; `expected=14 expected_counts=7/7/0/0/0`; doc/A0 zeros; `servers=1 relative_root=true` |
| `sha256sum tmp/openapi-client/onecall-v1.d.ts` twice | 0 | both `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` |
| `python3 /tmp/onecall-s1-fix-probes/run_probes.py` | 0 | `ALL 14 PROBES PASS` |
| `python3 /tmp/onecall-s1-fix2-probes/run_fix2_probes.py` | 0 | `ALL FIX2 PROBES PASS` |
| `node check-doc-map.mjs … --stage s2` | 1 | expected negative (missing S2 ids / count mismatches) |
| `node check-doc-map.mjs … --stage s3` | 1 | expected negative (missing S3 ids / count mismatches) |
| p9 surface swap lint (`/tmp/onecall-s1-fix2-probes/p9-surface-swap.yaml`) | 1 | `GET /api/v1/customer/me: x-onecall-surface "operations" must equal the approved path surface "customer"` and the inverse operations mismatch |
| p9 surface swap stage (`--stage s1` + swapped docs rows) | 1 | `does not own path surface` ×2 + `docs/15 row … does not match method/path/surface` ×2; `FAIL check-doc-map: 4 error(s)` even though counts still print 7/7 |
| p26 inline open request body | 1 | `request body media schema must be a $ref to a component schema` + `request object schema must set additionalProperties false` |
| p27 `prefixItems` `amountCents` | 1 | `request must not accept trusted fact amountCents` |
| p28 schema-valued `additionalProperties` / open map | 1 | `request object schema must set additionalProperties false` + `request must not accept trusted fact entitlementStatus` |
| p29 `$ref` sibling `accountId` | 1 | `request schema $ref must not carry sibling keywords: properties` |
| p30 nested `accountId` | 1 | trusted-fact rejection (preserved) |
| p31 array `items` `priceCents` | 1 | trusted-fact rejection (preserved) |
| p32 `allOf` `role` | 1 | trusted-fact rejection (preserved) |
| p33 `oneOf` `trialOutcome` | 1 | trusted-fact rejection (preserved) |
| p34 resolved `$ref` `accountId` | 1 | trusted-fact rejection (preserved) |
| p35 self-referential `$ref` `ownerAccountId` | 1 | trusted-fact rejection, no hang (preserved) |
| p36 legitimate identifiers (`subjectId`/`bindingId`/`activationEventId`/`expectedActivationRevision`/`entitlementId`) | 0 | still allowed |
| stock lint + stage s1 | 0 | green |
| five prefix mappings present in both `.mjs` files | 0 | `FIVE_PREFIX_MAPPINGS_OK` |
| OpenAPI SHA-256 re-verify | 0 | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` (unchanged) |
| UTF-8 / U+FFFD / CR / trailing-ws / conflict-marker scan (4 edited paths) | 0 | `SCANS: CLEAN` |
| `git diff --cached --stat` | 0 | empty; no staging |
| `git rev-parse HEAD` | 0 | `0c2d2ca23d16b8c32999e95a1321184d1c78df24` |

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

### Generated hash repeatability

```
gen1=1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521
gen2=1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521
DOUBLE_GENERATE_IDENTICAL=yes
```

## 5. Artifact hashes (post-fix-2)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/contract-rules.mjs` | `251f2c6aaea467400c4995f1f789e834f7a28554320ca340906bcc145075276f` |
| `api/openapi/check-doc-map.mjs` | `dfd9d98a52d9666fa4f44fb76556cd964299f4d416d5c899f1fee67eabc83eea` |
| `docs/15-production-openapi-surface-spec.md` | `34e49f2b612238929f190baa6b0c0e980f69ba51fc33a4d20a9d5c7069aa155f` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | `0ee72060ae60fd23ddd0bf4087822780ecec96507a3660720ed1b5a191e6d0ef` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-2.md` | (this file; hash recorded after write) |
| `tmp/openapi-client/onecall-v1.d.ts` (generated, not committed) | `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` |

### Unchanged forbidden targets (re-verified against pre-edit baseline)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` |
| `api/openapi/redocly.yaml` | `c76985dcf26c1eba0da024411be0817694859a726787d049e65b9231bc6c781a` |
| `api/openapi/client-compat.ts` | `414c8062bf52ae6ea383ab73eb2541d567c5d9f65e4282b3e00a64a3ad1f9262` |
| `api/openapi/package.json` | `9fe0622adc7007d22b010254f3ab029d188a0ef5ac6c9bc7718a0196f71ab7f6` |
| `api/openapi/package-lock.json` | `e40eda3a6f969cf2219970a03711147b65803e25ef12ee19dc6264d6e25dfa52` |
| `api/openapi/tsconfig.client-compat.json` | `1b1cea2458ddd4fa069d02f3da9a1cc239ed06db31bcefe2096232e9214f86fe` |
| `docs/09`–`docs/14` | unchanged (baseline `sha256sum -c` OK) |
| `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` | `fe5546533be2f606ecb15d689732d52bed0203392e38fdf4a045b4f21f683a0d` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-1.md` | `9415859ad62532f1ed3833053a89b420e6528c38af44cf5da79cca6788c66726` |
| `docs/reviews/code-review-20261023-234505.md` | `db949396c7af2dfefc90837b264d335f25b1ef85fd8d8294f1c51c1cc0780e47` |
| `docs/reviews/code-review-20261024-001741.md` | `6c3282f9466a0bbfdb3f95bcf2450454a5c788bac82d4c94be6921ae619e1d4d` |
| `docs/reviews/code-review-20261024-020734.md` | `5621b2c74959cb774aad01d441ceff1e65026a1e456cfe8575c6b8e47d26bb66` |
| `docs/reviews/plan-review-20261021-205118.md` | `7cef564d0fbde72ac9e44dd95e667fde5988601e714be2dd5a1728d4a9738495` |
| `docs/reviews/plan-review-20261023-230300.md` | `42cf7f9dec9cc4afb8e62fcb4b17eb2b0192b2abccad7e69522ad7a4e2ea639f` |
| `docs/gateflow/project-execution-control.md` | `42964172b681b6c4f527f5df6787b1a3473b3eb01f50856600e2410dc3934f66` (pre-existing dirty; untouched) |

Git HEAD (unchanged): `0c2d2ca23d16b8c32999e95a1321184d1c78df24`. `git diff --cached` empty.

## 6. Residuals

| Residual | Impact |
| --- | --- |
| Static-only evidence | No runtime/database/auth/payment/IVR proof; unchanged from S1 static mandate |
| Nested arrays do not enforce `maxItems` | Out of R2 scope; previously classified runtime/capacity concern |
| Redocly 1.34.3 custom-rule API | Future major upgrade requires revalidation |
| Stage s2/s3 counts | Deliberately fail until later slices land |
| Synthetic 475/512 idempotency probes | Still `/tmp`-only; later slices must declare their own header params |
| `$ref` sibling policy is reject-not-merge | Deterministic and safe; valid cyclic pure refs remain accepted |
| Future public-share common-error registration | Deferred to S3 when public-share operations land |

## 7. Scope confirmation

- Only the five paths in §2 were created or modified for this fix.
- No forbidden path changed (baseline hashes re-verified).
- No staging, commit, push, PR, merge, re-review or S2 work was performed.
- No S2/S3 operations were added; counts remain `7/7/0/0/0=14`.
- Problem registry remains exactly 15 codes; OpenAPI hash unchanged.

## 8. Completion

**COMPLETED** — R2-01, R2-02 and the durable docs/15 clarification applied; mandatory validation green; fix artifact created last.
