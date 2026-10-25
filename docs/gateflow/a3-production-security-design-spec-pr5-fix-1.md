# A3 production security design — Draft PR #5 review fix 1

- Gate: `fix`
- Work unit: `A3-production-security-design-spec`
- Task label: `a3-security-pr5-fix1-ds-20261026-01-launch2`
- Author model: DeepSeek (`deepseek-flash`)
- Binding head: `codex/a3-production-security-design-spec` @ `875e4c5c1d72d6bc474dc7e5c29a7802bb023037`
- Binding review: `docs/reviews/pr-5-review-20261026-024410.md`
- Status: **author-complete; controller validation and a fresh MiMo PR re-review are required**. This artifact does not claim acceptance, CI green, runtime readiness or production readiness.

---

## 1. Accepted finding matrix

| Finding | Severity accepted | Required bounded fix | Author status |
| --- | --- | --- | --- |
| `F-PR5-01` | Minor | Remove construction-guaranteed `futureInvariantIds`/`s2s3Sections` from `machineSummary` and only their fixture assertion keys; keep live `futureFixtureIds`, invariant count/text checks, section checks and string guards | 已修复 (author-complete) |
| `F-PR5-02` | Minor | Key provider nonce replay state by exact composite `${mtls.serviceIdentity}:${nonceValue}`, matching the frozen `(serviceIdentity, nonce)` policy and IVR path; preserve order and duplicate/conflict semantics | 已修复 (author-complete) |
| `F-PR5-03` | Minor | Remove incorrect `Q15` references; C03/certificate = `Q7` only, C05/provider certificate and vendor algorithm/canonicalization/payload/window = `Q6` only; add no deferred row or invented value | 已修复 (author-complete) |
| `F-PR5-04` | Minor | Freeze exact masked-display safe-class list `PHONE`, `IDENTITY_NUMBER`, `LEGAL_NAME` in doc, model and validator expected projection; all other/unknown `*Masked` keys fail closed | 已修复 (author-complete) |

All four fixes stay inside the static-spec boundary. No A0/A1/A2 file, runtime/cloud/UI path, prototype, screenshot, other fixture, review artifact or controller path was touched.

---

## 2. Decisions

### F-PR5-01 — remove vacuous summary fields
- Deleted `futureInvariantIds` and `s2s3Sections` from `machineSummary()` and removed their now-unused `futureInvariantPattern` / `forbidden` stage-config entries.
- Kept `futureFixtureIds` live (it uses the discriminative `sec-s3-`/`sec-s4-` patterns).
- Removed only the two keys from every fixture `checks`/`expected` object that contained them (12 existing fixtures). Retained fixture IDs, counts, negative set, `futureFixtureIds`, invariant counts, section checks, string guards and every state/side-effect assertion.
- Did not replace the removed fields with another construction-guaranteed field.

### F-PR5-02 — provider nonce scope
- Provider callback replay lookup/insert now uses `const providerNonceKey = \`${mtls.serviceIdentity}:${nonceValue}\``, identical to the IVR path composite.
- Verification order is unchanged: mTLS → headers → profile availability → timestamp window → signature → exact-duplicate early `200` with no nonce consumption → nonce store availability → composite nonce replay `401` → changed-payload `409` after nonce consumption.
- No second provider identity, numeric policy, TTL, window or response/status semantic was introduced.

### F-PR5-03 — Q15 removal
- Doc/model/validator now cite:
  - C03 IVR mTLS certificate TTL/lifecycle: `Q7` only.
  - C05 provider mTLS certificate and provider signature algorithm/canonicalization/payload/window: `Q6` only.
- Removed `Q15` from `docs/17` (credential table, signature paragraph, two residual rows), the model (`ttlInput` ×2, four vendor fields) and the validator's frozen expected credential/signature projections.
- The 23-row deferred registry, owners, destinations and all deferred counts are unchanged; no replacement row/value was invented.

### F-PR5-04 — exact masked-display safe classes
- Doc §18 now states masked display is allowed only for safe classes `PHONE`, `IDENTITY_NUMBER`, `LEGAL_NAME`; every other or unknown `*Masked` key follows unknown-field fail-closed (block/drop).
- Model `redactionPolicy.maskedDisplaySafeClasses` is exactly `["PHONE","IDENTITY_NUMBER","LEGAL_NAME"]` beside the retained `maskedVariantsAllowed: true`.
- Validator freezes the same list in `EXPECTED_REDACTION_POLICY`, reuses that single list for the executable classifier, and adds an exact projection check. The whole-object `deepEqual` projection remains, so deleting, reordering or broadening the model list fails validation.
- Executable classifier behavior is unchanged: credential/auth/secret/unknown bases are never display-safe.

---

## 3. Changed paths and SHA-256

| Path | SHA-256 |
| --- | --- |
| `docs/17-production-security-design-spec.md` | `4E7AC923556C5AC1B8EC4103E7A85F02E79597E852AB4A4A51C84DB54B6CD447` |
| `security/v1/security-design-v1.json` | `E1F46CB2D27142C90DE5125222C3068AEE067E28A6ACF24B269FB693B9023629` |
| `security/validate.mjs` | `2CCBF75CC162A85943FEE75D79E28FC92C3096F855880620A3CE9BAEFF574B6F` |
| `security/v1/fixtures/sec-s1-06-append-only-rotation-safety.json` | `7B073D4B5326686065931B55BA03CB74368AE9CB5F6F235FEFBF5DB5941701A8` |
| `security/v1/fixtures/sec-s2-01-five-domains-accepted.json` | `639AD736080BF0977846E1F6141823D03EAF51A16BA7CDE55762EA2B24AFE130` |
| `security/v1/fixtures/sec-s2-02-cross-domain-rejected.json` | `FCD1815BCA3D46D2E4909F0849A964C6E36BEAEC53C8867698833686E66DF8FD` |
| `security/v1/fixtures/sec-s2-03-service-identity-least-privilege.json` | `1D1AD1F9A8FF2282D9FB10A73DD163987A1791C912E63E0A4664C27FBE9F69F0` |
| `security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json` | `209A0BDEA490BA8455D3B500818945E3EC20F2577F6F1C76FD1B5C9CF50172A6` |
| `security/v1/fixtures/sec-s2-05-ivr-replay-rejected.json` | `7177200CB48C0E50F55C07AAFF80E550F359A8DB5BEFB1E7B7EB3DF2194C3772` |
| `security/v1/fixtures/sec-s2-06-provider-signature-replay.json` | `9724C2B5F78D459DFED3F95FBCAA85C31048CDE496EC02C43676D4D30A76F530` |
| `security/v1/fixtures/sec-s2-07-signing-key-rotation.json` | `A2CAA759439B5E1B9385FFA0C901BFEAB4BA9A3621A7525EECC22D4F1C83F971` |
| `security/v1/fixtures/sec-s2-08-failclosed-dependency-preserves-a2.json` | `4DC74866007013B2FFCF3F957E94A7750FF78956C590E636792A865CE05EDA97` |
| `security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` | `CCFC4401DF443E14BABD42523C74C3A143CF0B0053D5FD5F5DF41D354B81853D` |
| `security/v1/fixtures/sec-s3-01-upload-credential-scope.json` | `0D8D28D59278740DBF791F75F2B2A1CF21CE2F73B6BDC9B4D612E4EEB0D98551` |
| `security/v1/fixtures/sec-s3-08-end-to-end-security.json` | `77DD9F16C0F5F585C549705001A61AFF65AADD6B9A10115B89690FC14AFEE3CA` |

Created: `docs/gateflow/a3-production-security-design-spec-pr5-fix-1.md` (this artifact).

---

## 4. Validation

### Executed locally

| Check | Result |
| --- | --- |
| `jq empty` on the model and all 12 edited fixture JSON files | exit `0`, no output (all parse as JSON) |
| `git diff --check` scoped to the 15 changed paths | exit `0`, no output (no whitespace errors) |
| Conflict-marker scan over `docs/17` + `security/**` | no matches |
| Trailing-whitespace scan over `docs/17` + `security/**` | no files matched |
| `grep` for `futureInvariantIds` / `s2s3Sections` over `security/**` | no matches |
| `grep` for `Q15` over `docs/17`, model and validator | no matches |
| `grep` for `providerNonces` | only the composite-key lookup/insert sites |
| Model counters via `jq` | 20 key / 28 data / 23 deferred (18 `Q*` + 5 `CV-*`) / 21 invariants / 2 media / 39 audit / 2 `access_event` / 37 `append_fact` / 10 high-risk / 10 coverage |
| A2 digest via `jq` | `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` |
| Fixture inventory via `git ls-files` | 23 fixtures (`6+9+8` slices) |
| Negative set via grep | exactly 10: `sec-s1-03`, `sec-s1-05`, `sec-s2-02`, `sec-s2-04`, `sec-s2-05`, `sec-s2-06`, `sec-s2-09`, `sec-s3-02`, `sec-s3-04`, `sec-s3-07` |

### Not executed — permission-gated in this session

The required author-side executable commands were attempted and each returned the harness message `This command requires approval` (no execution, no output):

- `node --check security/validate.mjs`
- `npm --prefix api/openapi run check:s3` (equivalently `cd api/openapi && npm run check:s3`)
- `node protocol/ivr/validate.mjs --stage s3 ...`
- `node security/validate.mjs --stage s1|s2|s3 ...`
- The second S3 run for byte-identical stdout comparison

`python3` was also permission-gated; `jq` was available and was used for JSON parsing and static counter checks. **No exit code or PASS line is claimed for any node/npm command.** The controller must run `node --check`, `check:s3`, the A2 S3 validator, all three security stages and the duplicate S3 byte-identity check on this tree. Static counts above show the frozen inventory was not altered, but they are not a substitute for executable validation.

---

## 5. Docs decision

- Canonical document was updated rather than deferred: `Q15` mis-citations were corrected in place to `Q7`/`Q6`, and the masked-display rule now names the exact safe classes with explicit fail-closed behavior for all other/unknown `*Masked` keys.
- No new document section, deferred row, numeric TTL/window/algorithm, provider policy or public API was added.
- Doc credential-table rows remain exactly equal to the model rows (the validator derives its row comparison from the model; both sides now carry `Q7`/`Q6`).

---

## 6. Scope proof

- `git rev-parse HEAD` = `875e4c5c1d72d6bc474dc7e5c29a7802bb023037` (unchanged).
- `git diff --cached --name-only` = empty; nothing staged or committed.
- `git diff --name-only -- docs/17-production-security-design-spec.md security/` lists exactly the 15 files in §3.
- No write occurred outside the allowed doc/model/validator/listed-fixture set; the only new path is this artifact.
- Pre-existing unrelated dirty files (prototypes, screenshots, other docs/reviews, shared paths) were not touched.
- No branch switch/reset/restore, stage, commit, push, PR/issue action, merge, re-review or extra gate was performed.

---

## 7. Gateflow-classified residuals / uncovered areas

| Item | Classification | Owner / handling |
| --- | --- | --- |
| Executable validation (node/npm, all stages, duplicate S3 byte-identity) unavailable in the author session | `evidence-gap` (environmental) | Controller must execute the required command set on this tree before any acceptance |
| No multi-identity provider nonce fixture was added | `uncovered-area` (bounded by instruction) | Policy and both transport paths now share the `(serviceIdentity, nonce)` composite; add a discriminator fixture only if a second provider identity is ever introduced |
| Doc/model safe-class divergence from the executable classifier | `closed` | All three now carry the same three-class list; validator enforces exact projection |
| Q15 mis-attribution | `closed` | `Q15` removed from all A3 cert/vendor sites; existing `Q7`/`Q6` owners/destinations retained; 23-row registry unchanged |
| Runtime KMS/Redis/COS/PostgreSQL/mTLS/provider behavior | `out-of-scope static spec` | Remains owned by the 17-row `R-A3-*` residual table; no runtime claim made |

---

## 8. Completion

- Accepted findings `F-PR5-01..04`: all author-complete within the bounded scope.
- Author artifact path: `docs/gateflow/a3-production-security-design-spec-pr5-fix-1.md`.
- Next gate (controller-owned): run the required executable validation, then dispatch a fresh MiMo 2.6 Flash PR re-review. No self-acceptance is claimed.
