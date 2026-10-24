# A1 S1 bounded-fix artifact

> Task label: `a1-openapi-s1-fix-mimo26flash-20261024-01`
>
> Provider: Claude Code / `mimo26flash` / `mimo-v2.6-flash`
>
> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: fix — S1 code-review findings only
>
> Accepted plan base: `0c2d2ca23d16b8c32999e95a1321184d1c78df24`
>
> Status: COMPLETED

## 1. Scope

Applied only the controller-adjudicated bounded fixes A–G against the accepted plan base. Preserved exactly the S1 operation set `C1,C2,C3,C4,C5,C13,C14` and `O1..O7` with counts `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`. Static OpenAPI/validation/doc change only; no S2/S3 operations, no runtime/database/UI work.

### Explicitly rejected (unchanged)

- No extra `docs/14` `CLAIM_*` outcome codes added to `ProblemCode`.
- No C5 DECLINE 200 redesign; no DECLINE 200 schema.
- No O5 sort change.
- No removal of legitimate `NOT_FOUND` / `VALIDATION_FAILED`.
- No operation count, route, security or A0-mapping changes.

## 2. Changed paths (exactly six)

| # | Path | Role |
| --- | --- | --- |
| 1 | `api/openapi/onecall-v1.openapi.yaml` | Canonical contract (registry, common matrix, C2 field, C5 wording) |
| 2 | `api/openapi/contract-rules.mjs` | Rule engine (problem-complete, idempotency map, trusted-fact guard) |
| 3 | `api/openapi/client-compat.ts` | Compile-time type assertions (15 codes, `activationStatus` exactness) |
| 4 | `docs/15-production-openapi-surface-spec.md` | Human governance doc (registry 15, exact sets, C5 DECLINE) |
| 5 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | Implementation report (Fix A rewrite) |
| 6 | `docs/gateflow/a1-production-openapi-surfaces-spec-s1-fix-1.md` | This fix artifact (created last) |

Temporary negative fixtures live only under `/tmp/onecall-s1-fix-probes/`. Nothing in the repository outside the six paths was modified for this fix.

## 3. Controller decisions → applied changes

### A. Implementation report SHA correction

`docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` accepted-plan line corrected to `0c2d2ca23d16b8c32999e95a1321184d1c78df24`. Report fully rewritten to record the post-review bounded-fix pass, post-fix contract decisions, post-fix validation evidence and post-fix artifact hashes (§9 table below).

### B. Common Problem matrix on all 14 operations

- Registry added `RATE_LIMITED` (429) and `SERVICE_UNAVAILABLE` (503) before `INTERNAL`, both surfaces = five surfaces; `INTERNAL` remains 500/`default`.
- `ProblemCode` enum extended in the same order; registry set-and-order equals enum (15 codes).
- `components.responses` gained `RateLimited` and `ServiceUnavailable` (Problem schema refs).
- Every one of the 14 operations now lists `VALIDATION_FAILED`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE` in `x-onecall-error-codes` and exposes explicit `'422'` / `'429'` / `'503'` response refs; `default` remains `INTERNAL`.
- No numeric rate/retry/timeout policy; no vendor details.

### C. Response-only `BusinessNumber.activationStatus`

- Property added with `type: string`, `readOnly: true`, `enum: [INACTIVE, ACTIVE, SUSPENDED]`, description marking it server-derived and never client-writable.
- Added to `BusinessNumber.required`.
- `client-compat.ts` asserts `Equal<components['schemas']['BusinessNumber']['activationStatus'], 'INACTIVE' | 'ACTIVE' | 'SUSPENDED'>`; removing, making optional or changing the enum fails typecheck.

### D. `onecall/problem-complete` strengthened

- `default` satisfies only the `INTERNAL` 500 fallback (special-cased); every other listed code requires an explicit response at the registry `httpStatus`.
- Each operation must list the common codes `UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL` and expose `VALIDATION_FAILED`/`422`, `RATE_LIMITED`/`429`, `SERVICE_UNAVAILABLE`/`503` explicitly.
- After excluding `COMMON_PROBLEM_CODES`, exact domain sets enforced: `createCustomerSubjectClaim` = `CLAIM_RISK_BLOCKED` + `CLAIM_REPLAY_CONFLICT` + `CLAIM_CONTEXT_CONFLICT`; `suspendOperationsSubject`/`resumeOperationsSubject` = `NOT_FOUND` + `ACTIVATION_STATE_CONFLICT`. Missing, extra or duplicate codes fail (C4 + `CURSOR_INVALID` fails).

### E. OperationId-to-maxLength Idempotency-Key map

- `IDEMPOTENCY_KEY_MAX_LENGTHS`: `createCustomerSubjectClaim`/`suspendOperationsSubject`/`resumeOperationsSubject` = 128; future `createCustomerOrder` = 475; future `createCustomerOutboundCallTask`/`cancelCustomerOutboundCallTask` = 512.
- Unmapped operationIds carrying `x-onecall-idempotency: idempotency-key` fail closed (`absent from the Idempotency-Key maxLength contract map`).
- Future operations were not added to S1; class behaviour proven only by `/tmp` synthetic probes.

### F. Request-body-only recursive trusted-fact guard

- `walkRequestSchema` dereferences and recurses `properties` / `items` / `allOf` / `anyOf` / `oneOf`.
- Exact-name `TRUSTED_REQUEST_FACT_NAMES`: `account`, `accountId`, `owner`, `ownerAccountId`, `ownerOrganizationId`, `role`, `workspaceRole`, `price`, `priceCents`, `amount`, `amountCents`, `paymentSuccess`, `paymentStatus`, `trialOutcome`, `trialResult`, `entitlementStatus`.
- Legitimate identifiers (`subjectId`, `bindingId`, `verificationProofId`, `activationEventId`, `expectedActivationRevision`, `entitlementId`, …) remain allowed (exact-name match, not substring).
- Folded into the already-enabled `sensitive-fields` rule (`redocly.yaml` enumerates rule names; no new rule name introduced).

### G. C5 DECLINE preserved as `409 ACTIVATION_CONSENT_DECLINED`

- Registry `a0Origin` for `ACTIVATION_CONSENT_DECLINED` now states: explicit DECLINE committed typed refusal; no accepted consent/activation/trial; same-event replay returns the same refusal; T-S2-13.
- Operation description and 409 description rewritten: a DECLINE is a committed typed refusal carried by `409 ACTIVATION_CONSENT_DECLINED`; it appends the refusal, creates no accepted consent, activation or trial; replaying the same `activationEventId` returns the same refusal; the `200 ActivationResult` schema is ACCEPT-only success with no DECLINE 200 schema.

## 4. Validation commands and results

Run from `api/openapi/` unless noted. Node v22.23.4 / npm 10.9.9.

| Command | Exit | Key output |
| --- | ---: | --- |
| `node --check contract-rules.mjs` | 0 | syntax OK |
| `npm run lint` (via `check:s1`) | 0 | `Woohoo! Your API description is valid. 🎉` — Redocly 0 problems |
| `npm run bundle` | 0 | bundle written to `tmp/openapi-client/onecall-v1.bundle.json` |
| `npm run generate` | 0 | `openapi-typescript 7.8.0` → `onecall-v1.d.ts` |
| `npm run typecheck` | 0 | `tsc --noEmit -p tsconfig.client-compat.json` strict pass |
| `npm run doc-map` | 0 | `PASS check-doc-map` |
| `npm run check:s1` | 0 | full chain PASS (see counts below) |
| `node check-doc-map.mjs … --stage s2` | 1 | expected negative: 4 errors (`7 != 21`, `7 != 32`, `14 != 32` + missing list) |
| `node check-doc-map.mjs … --stage s3` | 1 | expected negative: 7 errors (`0 != 1`, `14 != 49` + per-surface mismatches) |
| `python3 /tmp/onecall-s1-fix-probes/run_probes.py` | 0 | `ALL 14 PROBES PASS` |
| ad-hoc `p14-nested-role` probe | 0 | exit=1, `request must not accept trusted fact role` FOUND |
| double `npm run generate` SHA-256 compare | 0 | identical (below) |
| forbidden-path hash re-verification (17 paths) | 0 | `forbidden_verified=17 all_unchanged=True` |
| UTF-8 / U+FFFD / CR / trailing-ws / conflict-marker scan (6 allowed paths) | 0 | `SCANS: CLEAN` |
| `git diff --check -- <six allowed paths>` | 0 | no output (paths untracked; paired with independent scan above) |

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

### Negative probe results

| Probe | Exit (want) | Assertion snippet |
| --- | ---: | --- |
| p0-stock-copy | 0 (0) | canonical spec unchanged, clean pass |
| p1-c4-delete-422 | 1 (1) | `explicit 422 Problem response is required` + `requires an explicit 422 response` |
| p2-c4-extra-cursor | 1 (1) | `exact Problem code set forbids extra code CURSOR_INVALID` |
| p3-shared-idem-200 | 1 (1) | `bounded to 128 characters for this action` |
| p4-order-475-pass | 0 (0) | synthetic `createCustomerOrder` @475 accepted |
| p5-order-512-fail | 1 (1) | `bounded to 475 characters for this action` |
| p6-task-512-pass | 0 (0) | synthetic `createCustomerOutboundCallTask` @512 accepted |
| p7-task-475-fail | 1 (1) | `bounded to 512 characters for this action` |
| p8-cancel-512-pass | 0 (0) | synthetic `cancelCustomerOutboundCallTask` @512 accepted |
| p9-unmapped-op-fail-closed | 1 (1) | `absent from the Idempotency-Key maxLength contract map` |
| p10-nested-accountId | 1 (1) | `request must not accept trusted fact accountId` |
| p11-allOf-priceCents | 1 (1) | `request must not accept trusted fact priceCents` |
| p12-array-paymentStatus | 1 (1) | `request must not accept trusted fact paymentStatus` |
| p13-oneOf-trialOutcome | 1 (1) | `request must not accept trusted fact trialOutcome` |
| p14-nested-role (ad-hoc) | 1 (1) | `request must not accept trusted fact role` |

### Common-matrix exposure proof

- Registry count = 15; `ProblemCode` enum count = 15; order equal (`registry_count=15 enum_count=15 order_equal=True`).
- Codes in order: `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `VALIDATION_FAILED`, `CURSOR_INVALID`, `CURSOR_FILTER_MISMATCH`, `CLAIM_RISK_BLOCKED`, `CLAIM_REPLAY_CONFLICT`, `CLAIM_CONTEXT_CONFLICT`, `ACTIVATION_STATE_CONFLICT`, `CONSENT_REQUIRED`, `ACTIVATION_CONSENT_DECLINED`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL`.
- All 14 operations list `VALIDATION_FAILED`/`RATE_LIMITED`/`SERVICE_UNAVAILABLE` in `x-onecall-error-codes`, expose explicit `'422'`/`'429'`/`'503'` refs to the shared Problem responses, and keep `default` → `INTERNAL`. `exposure_ok=14/14`.

### Generated hash repeatability

```
gen1=1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521
gen2=1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521
DOUBLE_GENERATE_IDENTICAL=yes
```

`tmp/openapi-client/onecall-v1.d.ts` is ignored build output and is not committed.

## 5. Artifact hashes (post-fix)

| Artifact | SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `3337fab17565ebc22bf2b3764fd27568ef576900c8acc1fbb971e716808d88bf` |
| `api/openapi/contract-rules.mjs` | `098459752159fc8b5983a804211ba600e776fa5d3362857f4bd9fd17ad80ba35` |
| `api/openapi/client-compat.ts` | `414c8062bf52ae6ea383ab73eb2541d567c5d9f65e4282b3e00a64a3ad1f9262` |
| `docs/15-production-openapi-surface-spec.md` | `421ebc50b3faea79dff5aeef6591acd1fe1993c32fcc8cd022d3e2cf1fc20303` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md` | `988423f34ddfe75f39fa7c85b743eb63a2ba4af3dd75db382e846448c23b84ca` |
| `api/openapi/redocly.yaml` (unchanged) | `c76985dcf26c1eba0da024411be0817694859a726787d049e65b9231bc6c781a` |
| `api/openapi/check-doc-map.mjs` (unchanged) | `7a926c736d733e053a3704759f16757de808fb9382df2f9009771e2ac6a0e87a` |
| `api/openapi/tsconfig.client-compat.json` (unchanged) | `1b1cea2458ddd4fa069d02f3da9a1cc239ed06db31bcefe2096232e9214f86fe` |
| `api/openapi/package.json` (unchanged) | `9fe0622adc7007d22b010254f3ab029d188a0ef5ac6c9bc7718a0196f71ab7f6` |
| `api/openapi/package-lock.json` (unchanged) | `e40eda3a6f969cf2219970a03711147b65803e25ef12ee19dc6264d6e25dfa52` |
| `tmp/openapi-client/onecall-v1.d.ts` (generated, not committed) | `1792e9c9e49df204f982c9a6fb1f18f8c251820560eaf23c5b1a02904e749521` |

Git HEAD (unchanged): `0c2d2ca23d16b8c32999e95a1321184d1c78df24`.

## 6. Forbidden-path re-verification

Baseline recorded in `/tmp/onecall-s1-fix-baseline.sha256` before edits. Re-hashed after all fixes:

```
forbidden_verified=17 all_unchanged=True
```

Covered paths: `api/openapi/redocly.yaml`, `api/openapi/check-doc-map.mjs`, `api/openapi/package.json`, `api/openapi/package-lock.json`, `api/openapi/tsconfig.client-compat.json`, `docs/09`–`docs/14`, `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md`, `docs/gateflow/project-execution-control.md`, `docs/reviews/code-review-20261023-234505.md`, `docs/reviews/code-review-20261024-001741.md`, `docs/reviews/plan-review-20261021-205118.md`, `docs/reviews/plan-review-20261023-230300.md`.

Pre-existing dirty controller/logo/screenshot paths were left untouched.

## 7. Residuals

| Residual | Impact |
| --- | --- |
| Nested arrays inside request bodies do not enforce `maxItems` | Trusted-fact guard walks `items` but does not bound array size; out of scope for A–G |
| Redocly major-version drift | Rules pin behaviour to installed Redocly 1.34.3; a major upgrade could change custom-rule semantics |
| Static-only evidence | No runtime/database/auth/payment/IVR proof; unchanged from S1 static mandate |
| Stage s2/s3 counts | Deliberately fail until later slices land; must not be widened to fake S1 passage |
| Shared `IdempotencyKeyParam` (128) | C4/O3/O4 reuse the shared param; future 475/512 operations must declare their own header params (proven by synthetic probes only) |

## 8. Scope confirmation

- Only the six paths in §2 were created or modified for this fix.
- No forbidden path changed (17 baseline hashes verified).
- No staging, commit, push, PR, merge, or re-review was performed.
- No S2/S3 operations were added; counts remain `7/7/0/0/0=14`.
- Problem registry remains exactly 15 codes with registry/enum order equality.

## 9. Completion

**COMPLETED** — bounded fixes A–G applied; mandatory validation green; fix artifact created last.
