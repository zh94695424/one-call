# A3 S3 review findings fix 1 — masked-display classification and upload PUT enforcement

## 1. Task, provider and base

- Work unit: `A3-production-security-design-spec`
- Slice: `S3` — media credentials, COS private policy, audit access, high-risk control, deterministic redaction
- Task label: `a3-security-s3-fix1-ds-20261026-01`
- Runtime/provider: Claude Code / `ds` / DeepSeek session model `deepseek-flash[1m]`
- Gate: `fix` only (correction of controller-accepted review findings; no review, checkpoint or gate advance)
- Base commit: `b20be2edfa576e4e27bdf94e067335df25e9cf88` (`gateflow: accept A3-production-security-design-spec S2`), verified by `git rev-parse HEAD` as the current HEAD plus the uncommitted S3 implementation/correction/review
- Accepted review: `docs/reviews/code-review-20261026-010357.md`
- Status: **author-complete within the allowed writes; executable validation is permission-blocked in this session and remains controller-owned.** This is not a checkpoint or review entry.

## 2. Accepted findings and disposition

| Finding | Severity | Status after this fix | Scope |
| --- | --- | --- | --- |
| `F-S3-R1-01` — `/masked$/i` wildcard exemption bypasses deny-list, destination allow-list and credential/URL pattern controls in `redactNode`, and `support-view` uses the same unrestricted suffix | Medium | **Fixed (author)** | `security/validate.mjs`; durable steps in `sec-s3-06` and `sec-s3-07` |
| `F-S3-R1-02` — upload USE treats an omitted `method` as PUT-compatible | Low | **Fixed (author)** | `security/validate.mjs`; durable step in `sec-s3-01` |

## 3. Exact code changes

### 3.1 `F-S3-R1-01` — one centralized masked-display classification

`security/validate.mjs:932-950` adds one classifier used by both call sites:

```js
const MASKED_DISPLAY_SAFE_CLASSES = new Set(['PHONE', 'IDENTITY_NUMBER', 'LEGAL_NAME']);

function maskedDisplayClass(key) {
  const name = String(key);
  if (!/masked$/i.test(name)) {
    return null;
  }
  const base = name.slice(0, -'Masked'.length).toLowerCase();
  const className = REDACTION_CLASS_BY_KEY[base] || null;
  return {
    className,
    safe: Boolean(className) && MASKED_DISPLAY_SAFE_CLASSES.has(className)
  };
}
```

- A `*Masked` key is display-safe only when the base (case-insensitive) is an explicitly recognized deny-list base whose class is in the explicit display-safe set.
- Credential/auth/secret/token/signature/nonce/object-key/bucket/ciphertext/HMAC/key-ref/KMS classes are not in `MASKED_DISPLAY_SAFE_CLASSES`, so their masked variants are never trusted. The same applies to share code, OTP, extraction code, provider identifiers, payload and object version because they are not in the safe set.
- Unknown/arbitrary bases such as `random` resolve to `className: null, safe: false`.

`redactNode` (`security/validate.mjs:1003-1022`) now applies that classification **before** emitting any scalar:

- `!safe` (credential class or unknown base) → `{ blocked: true, field: key }`, i.e. the unknown-field fail-closed path.
- safe key with an object/array value → blocked (previous behavior preserved).
- safe key with a string value → the value must pass `redactStringValue` (`security/validate.mjs:960-982`) before emit. Bearer, absolute URL, `v1.` envelope, named credential and `/api/v1/public/shares/` capability content are tokenized; a benign `138****0000` is unchanged and adds no token.
- safe key with null/number/boolean → emitted unchanged.

`support-view` (`security/validate.mjs:2553-2560`) now calls the same classifier instead of a second suffix test:

```js
const allowed = requested.filter((field) => {
  const maskedDisplay = maskedDisplayClass(field);
  return (maskedDisplay && maskedDisplay.safe)
    || ['id', 'internalId', 'status', 'reasonCode'].includes(field);
});
```

`calleeNumberMasked` stays allowed; `credentialMasked`, `capabilityTokenMasked` and `randomMasked` are denied. There is no remaining `/masked$/i` rule outside the single classifier (grep over `security/validate.mjs` confirms the only suffix test is at line 941).

### 3.2 `F-S3-R1-02` — positive PUT requirement for upload USE

`security/validate.mjs:2098-2099`:

```js
const forbiddenOperation = request.method !== 'PUT'
  || ['READ', 'LIST', 'DELETE'].includes(request.operation);
```

- Missing `method` (`undefined !== 'PUT'`) and every non-PUT method now produce `403 FORBIDDEN / WRITE_SCOPE_VIOLATION` with `acceptedUpload: false`, `acceptedOperations: ['PUT']`, `deniedOperations: ['READ','LIST','DELETE']`, `SECURITY_UPLOAD_CREDENTIAL_DENIED` audit/security events and `sideEffectsZero()`.
- The existing `READ`/`LIST`/`DELETE` operation check is retained as defense in depth. A missing method is no longer treated as PUT-compatible.
- Scope, expiry and oversize checks are unchanged and still run only after the method gate passes.

## 4. Fixture changes (durable, inside existing fixtures)

### 4.1 `sec-s3-01-upload-credential-scope.json`

- Added step `use-missing-method-denied` immediately after `use-put-within-scope`: the request supplies matching scope/expiry/size but omits both `method` and `operation`; expected `DENIED` / `403` / `FORBIDDEN` / `WRITE_SCOPE_VIOLATION`, zero side effects.
- Updated the per-fixture assert `uploadDenied` from `5` to `6`.
- `use-put-within-scope` and all existing PUT/READ/cross-object/expired/oversize steps are unchanged.
- Fixture `negative` remains `false`; fixture ID and file name are unchanged.

### 4.2 `sec-s3-06-redaction-deterministic.json`

- Added `log-safe-masked-credential-tokenized`: safe masked bases (`calleeNumberMasked`, `phoneMasked`, `mobileMasked`, `identityNumberMasked`) carry Bearer, share-capability path, `v1.` envelope and named-credential content. Expected tokens `["[REDACTED:CAPABILITY_TOKEN]", "[REDACTED:CREDENTIAL]"]`, `tokenCount: 2`; the event is emitted only after pattern controls.
- Added `log-credential-masked-key-blocked`: `capabilityTokenMasked: "Bearer TEST_ONLY_BEARER_0009"` is blocked with `blockedField: "capabilityTokenMasked"`, `rawExposed: false`.
- Added `log-unknown-masked-key-blocked`: arbitrary `randomMasked` is blocked with `blockedField: "randomMasked"`, `rawExposed: false`.
- Updated the per-fixture assert: `redactionEmits` `5 → 6`, `redactionBlocks` `2 → 4`.
- Existing benign positives `log-recursive-nested`, `support-masked-allowed`, `public-contract-masked-only`, `log-deterministic-repeat` and the existing unknown/error blocks are unchanged.
- Fixture `negative` remains `false`.

### 4.3 `sec-s3-07-credential-url-leak.json`

- Added `support-view-credential-masked-denied`: requested fields are `["id","calleeNumberMasked","credentialMasked","capabilityTokenMasked","randomMasked"]`; expected `allowedFields: ["id","calleeNumberMasked"]` and `deniedFields: ["credentialMasked","capabilityTokenMasked","randomMasked"]`.
- Existing support steps, playback denial, credential/token/HMAC redaction and `public-contract-masked-only` are unchanged.
- Fixture `negative` remains `true`; `redactionEmits`/`redactionBlocks` remain `3`/`1` because the new support-view step is not an emit/block action.

### 4.4 Preserved totals

- Fixture files: **23** (`6` S1 + `9` S2 + `8` S3); no fixture added, removed or renamed.
- Cumulative explicit negatives: **10**; the three edited fixtures keep their original `negative` flags (`false`, `false`, `true`).
- All fixture IDs, step IDs (new IDs are unique), Problem codes and existing positive/negative steps are preserved; expected data only asserts computed result fields.

## 5. Hashes

### 5.1 Modified paths

| Path | Before this fix (review §0.1) | After this fix |
| --- | --- | --- |
| `security/validate.mjs` | `124e08ab75c3e9471e4b8f0e2ddf3c3641fc20478ed54d8666e46606fe3b45e1` | `bc1ad53c683e41f81f72f3958d1d415702211302eaf679ac4df85ca9d671c498` |
| `security/v1/fixtures/sec-s3-01-upload-credential-scope.json` | `8692b021036344ab38c92c3bfd3918072e5fca1e938564312c116c2bd1a3df52` | `5cd802b6463e013b736d9a3792e62b279c72b1338d0b2e97a357e087e4a2d734` |
| `security/v1/fixtures/sec-s3-06-redaction-deterministic.json` | `d5d4ef5059814ff3af5e538a2303fcf86f5c82382d09a773966251a6121ead50` | `c1d3a3a3e7f1ec0da76cc496d58e48d2e82173d8d579f9964eb876553f773f8c` |
| `security/v1/fixtures/sec-s3-07-credential-url-leak.json` | `cd05587b03875b1ceb78e118e4a61f088fbc4ab5390d894899859ab58a00bfc1` | `6443ad56674824bef35964c2b09340391b0976f61ef6fed480ec6aaecb35005f` |

### 5.2 Verified unchanged paths

| Path | SHA-256 | Comparison |
| --- | --- | --- |
| `docs/17-production-security-design-spec.md` | `f4341a77344183ed571e7a7000899e352918ad8613676d182e1d976773405cfb` | equals review §0.1 / implementation report |
| `security/v1/security-design-v1.json` | `0b8d86fea7e793238cc60413ec2e6c9e509c0f4a0b24034e1300b8184dfaffdf` | equals review §0.1 / implementation report |
| `sec-s3-02-playback-credential-scope.json` | `888515aa3d85116772ac0309a5a02af6cff3fccbb2ab31dd86ef24a643b23a32` | unchanged |
| `sec-s3-03-cos-private-policy.json` | `1500d8e740c21c36d6f7262b9b0fd0d93fc7604c5d9e62f6c28620fdf5300759` | unchanged |
| `sec-s3-04-audit-access-denied.json` | `44d37893d95ac702b997d3b15528e9233a82ad03df73858f3350439080e263dc` | unchanged |
| `sec-s3-05-high-risk-controls.json` | `ea7e1fe7cac5848d16a3357494e82713aef0e59d82485904a9930f5ab34ee2b5` | unchanged |
| `sec-s3-08-end-to-end-security.json` | `6ac683a1541f38869a6338045dbc40d8a69506175e41aa38c9f506180275fa42` | unchanged |
| `docs/gateflow/a3-production-security-design-spec-plan.md` | `12e4136ee557d604ff680f1b162265c68171c5c724ef2888fb69d429007405f1` | unchanged |
| `docs/gateflow/a3-production-security-design-spec-s3-implementation.md` | `671c36864dd67cdcf0284217296d815982a6bbe8be57ad8968417b2acbd479d6` | unchanged |
| `docs/gateflow/a3-production-security-design-spec-s3-correction-1.md` | `f04e4b9b96cba088f86b76fda5ed232a3bea354c154ce9b33f6bc784ddc65ae6` | unchanged |
| `docs/reviews/code-review-20261026-010357.md` | `eaef07007581eb06e63da37d49ab2dc150178bf1d6520f8e13255fa83c6be015` | unchanged |

This artifact's own hash is intentionally omitted; the controller should recompute it.

## 6. Validation ledger

### 6.1 Permission limitation

This session's permission mode accepted file edits but denied command execution with the exact response `This command requires approval`. Confirmed probes:

| Probe | Result |
| --- | --- |
| `node --version` | `v22.23.4` (runtime exists) |
| `node --check security/validate.mjs` | **DENIED** — `This command requires approval` |
| `npm run check:s3` | **DENIED** — `This command requires approval` |
| `node security/validate.mjs --stage s3 …` (accepted arguments) | **DENIED** — `This command requires approval` |
| `python3 --version` | `Python 3.10.12` (runtime exists) |
| `python3 -c "print('ok')"` | **DENIED** — `This command requires approval` |

Per the environment gate, no denied command class was retried or bypassed. Consequently every executable validation below is **NOT RUN** by the author and remains controller-owned. No observed PASS/FAIL output, counter line or stdout hash is claimed.

### 6.2 Required validation status

| # | Required check | Author result |
| --- | --- | --- |
| 1 | `node --check security/validate.mjs` | NOT RUN — permission denied; substitute: full static read of the changed functions and balanced-brace review |
| 2 | A1 `npm run check:s3` | NOT RUN — permission denied; A1 sources were not modified |
| 3 | A2 stage-S3 validator | NOT RUN — permission denied; A2 sources were not modified |
| 4 | Security S1 / S2 / S3 | NOT RUN — permission denied |
| 4a | S3 run twice + byte-identical stdout/hash | NOT RUN — permission denied |
| 5 | Temporary-copy probes | NOT RUN — interpreter denied; static branch traces in §7 |
| 6 | `machine.run(step)` before expected comparison; expected values do not select branches | **PASS (static)** — see §8 |
| 7 | `git diff --check` + allowed-path scope | **PASS (static)** — see §6.3 |

Expected S3 counters after this fix, derived from unchanged model registries and the unchanged fixture inventory (these are **expectations, not observed output**):

```text
stage=s3 ... media_policies=2 audit_events=39 audit_persistence=39 audit_access_events=2
audit_append_facts=37 high_risk_actions=10 audit_coverage=10 invariants=21 fixtures=23
passed=23 failed=0 negative=10 slice_fixtures=6+9+8
drift=0; doc_*/token_*/stage_field_*/fixture_id_* all 0
```

The pre-fix stdout hash is necessarily invalidated because the validator and two fixtures changed; the controller must recompute the post-fix hash and prove two-run byte identity.

### 6.3 Checks that ran (static)

- `jq empty` over the three edited fixtures → exit 0, no output (valid JSON).
- `sha256sum` over the document, model, modified paths, the other S3 fixtures, plan and reports → values in §5.
- `git diff --check -- security/validate.mjs <three edited fixtures>` → exit 0, no output.
- Grep for trailing whitespace in the four allowed paths → 0 matches.
- Grep for conflict markers (`<<<<<<<` / `=======` / `>>>>>>>`) under `security/` → 0 matches.
- `jq` step-ID listing confirms the new durable steps and that all prior step IDs remain:
  - `sec-s3-01`: `use-missing-method-denied` inserted after `use-put-within-scope`.
  - `sec-s3-06`: `log-safe-masked-credential-tokenized`, `log-credential-masked-key-blocked`, `log-unknown-masked-key-blocked` inserted before `assert-redaction`.
  - `sec-s3-07`: `support-view-credential-masked-denied` inserted before `assert-no-leak`.
- `jq` negative flags: `sec-s3-01=false`, `sec-s3-06=false`, `sec-s3-07=true` (unchanged).
- `Glob security/v1/fixtures/*.json` → 23 files (6 S1, 9 S2, 8 S3).
- `git status --short -- security docs/gateflow docs/reviews` → only the pre-existing S3 implementation surface plus this artifact; no stage/commit/PR/merge.

## 7. Focused mutation evidence (static branch traces; executable probes controller-owned)

| Required probe | New branch | Static outcome |
| --- | --- | --- |
| Benign `calleeNumberMasked: "138****0000"` | `maskedDisplayClass` → base `calleenumber` → `PHONE` safe; `redactStringValue` matches no pattern | emitted unchanged, no token; existing positive steps remain valid |
| `capabilityTokenMasked: "Bearer TEST_ONLY_BEARER"` | base `capabilitytoken` → `CAPABILITY_TOKEN` not in safe set → `!safe` | event blocked, `blockedField` = the key, `rawExposed: false`; never raw |
| Safe masked key carrying Bearer / envelope / named credential / share path | safe class → `redactStringValue` before emit | tokenized to `[REDACTED:CREDENTIAL]` / `[REDACTED:CAPABILITY_TOKEN]`; see `log-safe-masked-credential-tokenized` |
| Arbitrary `randomMasked` | base `random` not in `REDACTION_CLASS_BY_KEY` → `safe: false` | event blocked via unknown-field fail-closed path |
| `support-view` with `credentialMasked` / `capabilityTokenMasked` / `randomMasked` | same classifier; not safe and not in the fixed ID/status list | denied; `calleeNumberMasked` remains allowed |
| Upload USE with omitted `method` (and `operation`) | `request.method !== 'PUT'` is true for `undefined` | `403 FORBIDDEN / WRITE_SCOPE_VIOLATION`, denied events, zero side effects |
| Upload USE with non-PUT `method` | same positive requirement | denied even if `operation` says `PUT` |
| Upload USE with explicit `PUT` and matching scope | method gate passes; scope/expiry/size unchanged | existing `UPLOAD_ACCEPTED` path unchanged |

The new durable negative steps encode the first, fourth, fifth and sixth rows so a future regression cannot pass the fixture suite silently. The remaining rows (absolute URL, non-PUT method) are executable-probe rows because the fixture-level URL scanner forbids `http(s)://` literals; the code paths are the same `redactStringValue` URL branch and the same positive method gate.

## 8. `machine.run` ordering and branch independence

- `validateFixtures` still executes `const result = machine.run(step);` at `security/validate.mjs:3709` and only then iterates `Object.entries(step.expected)` for `deepEqual` comparison at `security/validate.mjs:3710-3714`.
- `expected` values are read exclusively in that comparison loop; no evaluator branch consults `step.expected`, `step.checks` outside the `assert` action, or fixture expectations.
- The new fixture expectations assert only computed result fields that exist in the corresponding return objects; they do not add branches or code paths.

## 9. Unchanged paths and scope

Modified only:

1. `security/validate.mjs`
2. `security/v1/fixtures/sec-s3-01-upload-credential-scope.json`
3. `security/v1/fixtures/sec-s3-06-redaction-deterministic.json`
4. `security/v1/fixtures/sec-s3-07-credential-url-leak.json`

Created only this artifact.

Not modified: canonical document `docs/17-production-security-design-spec.md`, machine model `security/v1/security-design-v1.json`, the other five S3 fixtures, the S3 implementation and correction-1 reports, the review, controller, accepted plan, A0/A1/A2 sources, accepted S1/S2 fixtures and artifacts, OpenAPI/protocol assets, prototypes, screenshots and shared business logic. No stage, commit, push, PR creation/modification, merge, review or checkpoint action was performed.

## 10. Residual risks and limitations

- `R-A3-FIX1-VALIDATION-PENDING`: executable S1/S2/S3, A1/A2, two-run hash and probe evidence is not author-observable this session. The controller must run the accepted commands and confirm the exact counters (including `passed=23`) before this fix can be checkpoint-ready.
- The display-safe masked set is deliberately an explicit allowlist (`PHONE`, `IDENTITY_NUMBER`, `LEGAL_NAME`). A future legitimately maskable class requires a deliberate validator change; anything not listed fails closed rather than being auto-trusted by suffix.
- Non-safe masked keys are blocked rather than tokenized. This is the stricter fail-closed interpretation permitted by the finding and the task; callers that want a display value for a credential-class field receive no raw value.
- Pattern controls detect credential/URL/envelope/named-credential/share-capability shapes, not arbitrary unstructured secrets. A caller that labels a raw personal value as a safe masked variant (for example a raw phone number under `phoneMasked`) is not detected by shape; that remains a producer-contract responsibility and an existing `R-A3-REDACTION-RUNTIME` residual.
- No real KMS, Secret Manager, CAM, COS, Redis, PostgreSQL or logging-platform behavior is exercised; all evidence is deterministic model/fixture reasoning.

## 11. Completion statement

- `F-S3-R1-01` is fixed by one centralized masked-display classifier shared by `redactNode` and `support-view`, with safe-value pattern controls and unknown/credential masked keys failing closed.
- `F-S3-R1-02` is fixed by positively requiring `request.method === 'PUT'` while retaining the existing operation checks.
- Durable negative steps are added to the three existing fixtures; fixture count `23`, cumulative negatives `10`, all fixture IDs, Problem codes and registries are preserved; document/model hashes are unchanged.
- This fix is **author-complete** within the allowed writes and **not checkpoint-ready**. Executable validation and mutation probes are **controller-owned and NOT RUN** because this session denied `node`/`npm`/interpreter execution.
- Stop after this fix: no review, no checkpoint, no gate advance.
