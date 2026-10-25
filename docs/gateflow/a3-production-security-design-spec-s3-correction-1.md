# A3 S3 implementation correction 1 — upload media policy guard inflection

## 1. Task, provider and base

- Work unit: `A3-production-security-design-spec`
- Slice: S3 — media, COS, audit, high-risk and redaction
- Task label: `a3-security-s3-correction1-ds-20261026-01`
- Runtime/provider: Claude Code / `ds` / DeepSeek session model `deepseek-flash[1m]`
- Gate: `implementation` only (correction of an accepted defect; no review or checkpoint entry)
- Base commit: `b20be2edfa576e4e27bdf94e067335df25e9cf88` (`gateflow: accept A3-production-security-design-spec S2`) plus the uncommitted S3 implementation
- Status: **author-complete within the allowed writes; executable validation is permission-blocked in this session and remains controller-owned.** This is not a checkpoint, review entry or gate advance.

## 2. Accepted defect

`F-S3-C1-01` is a validator false negative, not a model/document defect:

- Controller validation showed `node --check`, A1 `check:s3`, A2 stage-S3, security S1 and security S2 passing, and security S3 computing every exact registry/fixture counter correctly (`media_policies=2`, `audit_events=39`, `37+2` persistence routes, `high_risk_actions=10`, `audit_coverage=10`, `invariants=21`, `fixtures=23 passed=23 failed=0 negative=10 slice_fixtures=6+9+8`).
- S3 still exited `1` with `stage_field_missing=1` and only `upload media policy does not freeze the single COS presigned PUT shape and fail-closed no-fallback rule`.
- `model.mediaPolicies` deep-equals `EXPECTED_MEDIA_POLICIES`. The frozen U1 `fallbackPolicy` is exactly `no custom HMAC token and no signing-gateway fallback; when CV-4 cannot verify the capability, issuance fails closed and implementation stops`.
- The redundant guard in `validateS3Model` used case-sensitive `.includes('fail closed')`; the frozen wording is grammatically inflected as `fails closed`, so the guard rejected the exact frozen policy.

## 3. Exact code change

Only `security/validate.mjs` was modified. Within `validateS3Model`, the upload-policy semantic guard changed by exactly one line:

Before:

```js
    || !String(upload.fallbackPolicy || '').includes('fail closed')) {
```

After:

```js
    || !/fails? closed/.test(String(upload.fallbackPolicy || ''))) {
```

The guard still requires `mediaKind === 'UPLOAD'`, `credentialClass === 'C09 uploadCredential'`, `forbiddenOperations === 'READ/LIST/DELETE'`, `.includes('no custom HMAC token')` and fail-closed behavior (`/fails? closed/` still requires the closed state and rejects `fails open` or removal). `deepEqual(media, EXPECTED_MEDIA_POLICIES)` at `security/validate.mjs:3243` is unchanged and remains the exact full-contract guard covering `primaryShape`, signing-gateway wording, binding, permanence and every other U1/P1 field.

No schema, section, invariant, fixture, Problem code, role, state, policy or production value was added.

## 4. Hashes

| Path | SHA-256 | State |
| --- | --- | --- |
| `security/validate.mjs` before correction (per S3 implementation report) | `d838482cf852e94fa0b534097cfd459fd58b41974623aa814442a9c2cd6743ec` | pre-correction |
| `security/validate.mjs` after correction | `124e08ab75c3e9471e4b8f0e2ddf3c3641fc20478ed54d8666e46606fe3b45e1` | modified |
| `docs/17-production-security-design-spec.md` | `f4341a77344183ed571e7a7000899e352918ad8613676d182e1d976773405cfb` | unchanged |
| `security/v1/security-design-v1.json` | `0b8d86fea7e793238cc60413ec2e6c9e509c0f4a0b24034e1300b8184dfaffdf` | unchanged |
| `sec-s3-01-upload-credential-scope.json` | `8692b021036344ab38c92c3bfd3918072e5fca1e938564312c116c2bd1a3df52` | unchanged |
| `sec-s3-02-playback-credential-scope.json` | `888515aa3d85116772ac0309a5a02af6cff3fccbb2ab31dd86ef24a643b23a32` | unchanged |
| `sec-s3-03-cos-private-policy.json` | `1500d8e740c21c36d6f7262b9b0fd0d93fc7604c5d9e62f6c28620fdf5300759` | unchanged |
| `sec-s3-04-audit-access-denied.json` | `44d37893d95ac702b997d3b15528e9233a82ad03df73858f3350439080e263dc` | unchanged |
| `sec-s3-05-high-risk-controls.json` | `ea7e1fe7cac5848d16a3357494e82713aef0e59d82485904a9930f5ab34ee2b5` | unchanged |
| `sec-s3-06-redaction-deterministic.json` | `d5d4ef5059814ff3af5e538a2303fcf86f5c82382d09a773966251a6121ead50` | unchanged |
| `sec-s3-07-credential-url-leak.json` | `cd05587b03875b1ceb78e118e4a61f088fbc4ab5390d894899859ab58a00bfc1` | unchanged |
| `sec-s3-08-end-to-end-security.json` | `6ac683a1541f38869a6338045dbc40d8a69506175e41aa38c9f506180275fa42` | unchanged |

The document, model and all eight S3 fixtures hash to the same values recorded in the original S3 implementation report, proving the correction did not alter frozen content. This artifact's own hash is intentionally omitted; the controller should recompute it.

## 5. Validation ledger

### 5.1 Permission limitation

This session denied `node`, `npm` and interpreter execution with the exact response `This command requires approval` (probed with `node --check security/validate.mjs`, `npm run check:s3` and `python3 -c`). Per the environment gate the author did not retry or bypass. `node --version` returned `v22.23.4`, confirming the runtime exists but is gated. Consequently items 1–6 below are **NOT RUN** by the author; the controller remains the authoritative executable evidence.

| # | Command | Author result |
| --- | --- | --- |
| 1 | `node --check security/validate.mjs` | NOT RUN — permission denied |
| 2 | `cd api/openapi && npm run check:s3` | NOT RUN — permission denied |
| 3 | A2 stage-S3 with accepted model/fixtures/spec/bundle/A0 paths | NOT RUN — permission denied |
| 4 | Security S1, S2 and S3 with accepted arguments | NOT RUN — permission denied |
| 5 | Two S3 runs + byte-identical output/hash comparison | NOT RUN — permission denied |
| 6 | Temporary-copy mutation probes outside the repository | NOT RUN — permission denied |
| 7 | `git diff --check` and scope inspection | RUN — see 5.2 |

Accepted command templates the controller should run (unchanged path arguments):

```bash
node --check security/validate.mjs
cd api/openapi && npm run check:s3
cd /mnt/d/codex-call
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s1 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures
node security/validate.mjs --stage s2 ...   # same arguments
node security/validate.mjs --stage s3 ...   # same arguments, run twice
```

Expected S3 counters remain as accepted: `stage=s3 ... media_policies=2 audit_events=39 ... invariants=21 fixtures=23 passed=23 failed=0 negative=10 slice_fixtures=6+9+8`, `drift=0`, and all `doc_*/token_*/stage_field_*/fixture_id_*` counters zero.

### 5.2 Checks that ran (static)

- `sha256sum` over the document, model, validator and eight S3 fixtures → values in section 4; document/model/fixtures match the original implementation report.
- Read comparison of `EXPECTED_MEDIA_POLICIES` (`security/validate.mjs:328-367`) against `model.mediaPolicies` (`security/v1/security-design-v1.json:1393-1432`) field-by-field: identical, including U1 `primaryShape`, `forbiddenOperations`, `fallbackPolicy` and `ttlInput` and the whole P1 row. `deepEqual` uses canonical JSON (`security/validate.mjs:658-660`), so key order is irrelevant.
- Grep for `fails? closed` over the model returns `security/v1/security-design-v1.json:1410`, proving the new regex matches the exact frozen `issuance fails closed` wording.
- Grep confirms no remaining `includes('fail closed')` in `security/validate.mjs`; the only fail-closed guard in `validateS3Model` is the corrected regex at line 3256.
- `git diff --check -- security/validate.mjs` → exit `0`, no output.
- `git status --short -- security docs/gateflow` → `security/validate.mjs` modified by this correction; the other security/gateflow entries are the pre-existing S3 implementation, pre-existing controller dirt and this artifact. No A0/A1/A2, accepted S1/S2 fixture, plan or controller content changed by this task.

## 6. Mutation evidence (static reasoning; executable probes controller-owned)

The controller should copy the model to `/tmp` and run S3 mutations there. Expected outcomes derived from the unchanged code:

| Mutation of the U1 policy | Expected result | Mechanism |
| --- | --- | --- |
| Unchanged exact frozen U1 policy | passes | `.includes('no custom HMAC token')` true; `/fails? closed/` matches `fails closed`; `mediaKind`, `credentialClass`, `forbiddenOperations` exact; `deepEqual` true (field-by-field comparison in 5.2) |
| Remove or change `no custom HMAC token` | rejected | explicit `.includes('no custom HMAC token')` guard fails and `deepEqual` fails |
| Remove or change the COS presigned PUT `primaryShape` | rejected | unchanged `deepEqual(media, EXPECTED_MEDIA_POLICIES)` exact-contract guard fails |
| Change `forbiddenOperations` away from `READ/LIST/DELETE` | rejected | explicit equality guard fails and `deepEqual` fails |
| Remove or change the fail-closed wording (e.g. `fails open`, drop `closed`) | rejected | `/fails? closed/` no longer matches and `deepEqual` fails |

Each rejection also raises `stage_field_missing`, so S3 cannot pass. The regex was chosen to accept only the grammatical inflection (`fail closed` / `fails closed`) while still requiring the `closed` state; it is not a truthiness check and the semantic assertion was not removed.

## 7. Unchanged paths and scope

Modified only `security/validate.mjs` (one guard line). Created only this artifact. Not modified by this correction: `docs/17-production-security-design-spec.md`, `security/v1/security-design-v1.json`, all eight `security/v1/fixtures/sec-s3-*.json`, `docs/gateflow/a3-production-security-design-spec-s3-implementation.md`, the plan, the controller, A0/A1/A2 sources, accepted S1/S2 artifacts and fixtures, OpenAPI/protocol assets, prototypes, screenshots and shared business logic. No stage, commit, push, PR, merge or review/checkpoint action was performed.

## 8. Residual risks and limitations

- `R-A3-C1-VALIDATION-PENDING`: executable S3/S1/S2/newline/mutation evidence is not author-observable this session; the controller must run it. Static evidence shows the corrected guard accepts the frozen policy and other guards are intact, but no author run is claimed.
- The semantic guard independently asserts `no custom HMAC token` plus fail-closed behavior; the `no signing-gateway fallback` wording is enforced by the unchanged exact `deepEqual` contract guard rather than a separate substring test. This preserves the accepted contract without broadening the semantic guard.
- No real-cloud, runtime, credential, upload/playback or production-policy validation was performed or claimed; the existing A3 residuals and owners are unchanged.

## 9. Completion statement

- `F-S3-C1-01` is fixed with a one-line, non-weakening guard change that accepts the exact frozen phrase `fails closed` and preserves the C09 / `READ/LIST/DELETE` / no-custom-HMAC-token / fail-closed assertions plus the exact `deepEqual` contract.
- The correction is **author-complete** within the allowed writes and **not checkpoint-ready**; executable validation and mutation confirmation are **controller-owned and NOT RUN** because this session denied `node`/`npm`/interpreter execution.
- Stop after this correction: no review, no checkpoint, no gate advance.
