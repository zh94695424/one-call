# A3 S1 implementation — sensitive-data protection and key lifecycle

> Work unit：`A3-production-security-design-spec`
>
> Slice：S1 — sensitive-data protection, key hierarchy, rotation/recovery and historical replay
>
> Task label：`a3-security-s1-impl-ds-20261025-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Gate：`implementation` (S1 author completion only)
>
> Base：accepted plan checkpoint `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c`
>
> Stop condition：S1 author-complete; controller runs hash-bound executable validation before any acceptance or S2 work.

## 1. Gate, scope and binding inputs

- Binding contract: accepted plan `docs/gateflow/a3-production-security-design-spec-plan.md` at checkpoint `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c`, its fix artifact, accepted re-review, and the A3 controller section `A3 accepted-plan checkpoint and S1 implementation entry`.
- S1 freezes only: the AES-256-GCM envelope profile with the A0 text shape; HMAC-SHA-256 lookup shape; exactly 20 key classes; exactly 28 data classes mapped row-for-row to A0 §8.1; exactly 23 deferred inputs partitioned as 18 exact A0 Q rows plus 5 A3-origin CV rows; key states, rotation/recovery/historical decrypt/lookup and replay compatibility; exactly `INV-A3-01..06`; exactly six deterministic S1 fixtures and a zero-dependency Node 22 validator.
- No S2/S3 model section, invariant text or fixture ID was authored. No A0/A1/A2 accepted artifact, OpenAPI source, prototype, screenshot, shared business logic, controller, accepted plan/review/fix or historical untracked review was modified.
- No new table, column, state, endpoint, header, Problem code, employee role, cloud capability claim, production region/SKU/price/capacity/WORM value or runtime implementation was added.

## 2. Exact changed files and SHA-256

Only the following repository paths were created. The first nine are the S1 implementation artifacts; the tenth is this report. Every other repository path is read-only in this task.

| Path | Lines | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| `docs/17-production-security-design-spec.md` | 297 | 33790 | `b252b2b96b4ad50c8acc4aa8d97c6af2c48c91f0a95245feb8aefb1e665a9581` |
| `security/v1/security-design-v1.json` | 915 | 36855 | `13e08cc6a01bee69f2adc6c57842b5a5d1b0d9147fae6771e84e6ee1301ab81d` |
| `security/validate.mjs` | 1616 | 74866 | `0e5471474fbb204c4a897eb3b41f0cee161dc606b69000742f76228478546b6e` |
| `security/v1/fixtures/sec-s1-01-envelope-purpose-roundtrip.json` | 269 | 11650 | `844b67a4f10a8dcbcd690df6093e9de33dc8bc964ac210a4850e89d1f806f576` |
| `security/v1/fixtures/sec-s1-02-hmac-index-equality.json` | 67 | 2827 | `2acec51fd57e468af58546f35a9dd1021de2cbedad75c5610f3963e45155371d` |
| `security/v1/fixtures/sec-s1-03-cross-purpose-denied.json` | 66 | 3051 | `5cbfa710dc2a88b6a44f87590b3ba580370e51b5e528db4f349f1f88282142c5` |
| `security/v1/fixtures/sec-s1-04-rotation-historical-replay.json` | 168 | 7445 | `6a9fbd95d4971a2b551bcda28ca209fc173a9e3a9ca1d4c00841b7acb3737206` |
| `security/v1/fixtures/sec-s1-05-key-unavailable-failclosed.json` | 96 | 4786 | `caa71dffc8c2d015130bf01f0ce95ae69dae7ba963a819275dc157a5eb107c7d` |
| `security/v1/fixtures/sec-s1-06-append-only-rotation-safety.json` | 105 | 4052 | `00d85e4401e6e31f858804ab600a6e3d1e694a4e1c2f938f00588da6c4b2c0e2` |
| `docs/gateflow/a3-production-security-design-spec-s1-implementation.md` | this report | this report | self-hash reported by the author in the final response; re-verifiable by the controller |

The hashes above were measured after the final content decisions in this report, except for the report itself. This report’s own hash is reported by the author in the final response and can be re-verified by the controller.

## 3. Registry, marker and fixture inventory

- Model top-level sections at stage S1: `modelVersion`, `specVersion`, `upstream`, `envelopeProfile`, `hmacProfile`, `keyStates`, `rotationPolicy`, `recoveryPolicy`, `replayCompatibility`, `keyClasses`, `dataClasses`, `deferredInputs`, `invariants`. No other top-level section exists.
- `keyClasses`: 20 (`K01..K20`), unique keyId, keyClass and purposeId; each row carries custody, purpose, algorithm, data mapping, rotation, recovery, allowed states, owner and `failClosedStatus=503`.
- `dataClasses`: 28 (`D01..D28`), each with the exact A0 §8.1 inventory cell, representation, key/HMAC mapping, owner, projection, audit/access and rotation/replay rule.
- `deferredInputs`: 23 = 18 `Q` rows (`Q1,Q3,Q6,Q7,Q8,Q10,Q11,Q12,Q13,Q14,Q17,Q18,Q19,Q22,Q23,Q25,Q27,Q28`) with input/owner/destination copied exactly from A0 §14, plus 5 A3-origin `CV-1..CV-5` rows absent from A0 §14.
- `invariants`: 6 (`INV-A3-01..INV-A3-06`). No `INV-A3-07..21` ID or text appears in the model, document or fixtures.
- Document marker blocks: `security-key-map` 20 rows × 7 columns; `security-data-map` 28 rows × 8 columns; `security-deferred-map` 23 rows × 6 columns. Row order, columns and values equal the model projections.
- Fixtures: exactly six files, all slice `s1`; two explicit negatives (`sec-s1-03`, `sec-s1-05`).

| Fixture | Slice | Negative | Steps |
| --- | --- | --- | ---: |
| `sec-s1-01-envelope-purpose-roundtrip` | s1 | false | 24 |
| `sec-s1-02-hmac-index-equality` | s1 | false | 5 |
| `sec-s1-03-cross-purpose-denied` | s1 | true | 5 |
| `sec-s1-04-rotation-historical-replay` | s1 | false | 15 |
| `sec-s1-05-key-unavailable-failclosed` | s1 | true | 9 |
| `sec-s1-06-append-only-rotation-safety` | s1 | false | 9 |

## 4. Implementation decisions

1. **Envelope parsing.** The validator and document freeze `v1.<key_ref>.<base64url(nonce||ct||tag)>`; the first segment is literal `v1`, the last dot separates the body, and the middle opaque `key_ref` may contain dots and follows the A0 character set. Unknown or malformed key references fail closed.
2. **AEAD/AAD.** AES-256-GCM with a 12-byte nonce, 16-byte tag and AAD `onecall:v1:<purposeId>:<dataClassId>`. Production requires CSPRNG nonces; fixture nonces are fixed `TEST_ONLY` evaluator inputs only.
3. **HMAC.** Independent HMAC-SHA-256 keys, lowercase 64-hex, equality/uniqueness/indexed lookup only, never returned or logged. A malformed stored HMAC is rejected as `HMAC_MALFORMED`.
4. **Key states.** `ACTIVE`, `DECRYPT_ONLY`, `LOOKUP_ONLY`, `VERIFY_ONLY`, `DISABLED`, `DESTROYED`; the state-use matrix is frozen. Old encryption versions become `DECRYPT_ONLY`, old HMAC versions `LOOKUP_ONLY`, and old signature versions `VERIFY_ONLY`.
5. **Rotation/recovery.** Historical decrypt uses the envelope `key_ref`; old keys are retained until retention is approved. Retirement without approved retention is denied. `DESTROYED` is unrecoverable. No plaintext/backup/env fallback exists.
6. **Append-only/frozen rows.** Rewriting `D08`, `D10` or `D27` is denied with `APPEND_ONLY_VIOLATION`; fixtures assert zero row and A0 mutations.
7. **A2 replay.** Replay decrypts the same envelope with its historical key and compares the plaintext hash; `IvrDecision` replay produces no second call, reservation, settlement or event identity. Only transport nonce/timestamp may change across retries.
8. **Deferred provenance.** Q rows are copied exactly from A0 §14 (input/owner/destination); CV rows are A3-origin capability-verification inputs, do not consume Q IDs and are asserted absent from A0 §14. No region/SKU/TTL/WORM/KDF value was guessed.
9. **Stage gating.** The validator accepts only `--stage s1`; it rejects premature S2/S3 section names, future invariant IDs, future fixture IDs, URLs, and any fixture whose slice or ID is not the approved S1 set.
10. **Fixture evaluation.** `SecurityMachine` computes encryption/decryption/HMAC/rotation/recovery/replay/append-only/stage behavior from parsed inputs and fixtures; it never uses `expected` to decide a result. Expected values are compared against computed results.

## 5. Validation commands and observed outcomes

### 5.1 Commands that ran (static/document checks)

| Command | Observed outcome |
| --- | --- |
| `sha256sum` on the nine created artifacts | Exit 0; hashes recorded in §2 |
| `wc -l -c` on the nine created artifacts | Exit 0; sizes recorded in §2 |
| `jq -e . security/v1/security-design-v1.json` and `jq -e . security/v1/fixtures/sec-s1-0{1..6}-*.json` | Exit 0 for the model and all six fixtures (JSON parse valid) |
| `file -bi` on all nine artifacts | `text/plain; charset=utf-8` for the document; `text/plain; charset=us-ascii` / `application/json; charset=us-ascii` for ASCII-only model, validator and fixtures (valid UTF-8 subsets) |
| Grep for `^(<<<<<<<|=======|>>>>>>>)` in `docs/17` and `security/` | No matches |
| Grep for `[[:blank:]]$` in `docs/17` and `security/` | No matches |
| Grep for future invariant IDs, S2/S3 fixture IDs, URLs and premature section tokens in `docs/17` | No matches |
| Grep for URLs in `security/` | No matches |
| `git diff --check -- docs/17-production-security-design-spec.md security` | No output (exit 0) |
| `git status --short` | Only the ten allowed new paths plus the pre-existing dirty/untracked paths listed at task start; no existing path was modified by this task |

### 5.2 Commands attempted but denied by the Agent environment

| Command | Observed outcome |
| --- | --- |
| `node --check security/validate.mjs` | Not executed: environment permission gate required approval and did not grant it. No exit code or compiler output was produced. |
| `node security/validate.mjs --stage s1 ...` (exact accepted arguments) | Not executed: environment permission gate required approval and did not grant it. No PASS/FAIL output, counters or two-run hash were produced. |
| `cd api/openapi && npm run check:s3` | Not executed: environment permission gate required approval and did not grant it. No OpenAPI check result was produced. |
| A2 `protocol/ivr/validate.mjs --stage s3 ...` | Not executed for the same permission reason. |

This is an honest Agent-environment limitation, not a passing executable claim. The controller must run the hash-bound commands from plan §7.1. Static document, JSON, encoding, whitespace and scope checks passed.

### 5.3 Expected executable counters (not observed by the author)

When the controller runs the exact command, the intended success output is:

```text
stage=s1 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=6 fixtures=6 passed=6 failed=0 negative=2 slice_fixtures=6
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s1)
```

The A0 (44/28), A1 (49/6 and five audience values), A2 (12/11/22/15) and A2 event digest were independently observed in the accepted source files (including `jq -cS` recomputation of the A2 digest to `c6b769...f4e9`). The executable assertion remains controller-owned because Node execution was denied.

## 6. Mutation-resistance matrix (not executed; controller-owned)

The following probes are intended to be run on copies outside the repository. They were not executed because Node invocation was denied in this environment. Each row maps to a validator check that fails closed.

| Probe | Intended rejection path |
| --- | --- |
| Remove one `keyClasses` row | Key count ≠ 20; `stage_field_missing` |
| Add an extra `keyClasses` row | Key count ≠ 20 / extra expected row; `stage_field_missing` |
| Duplicate a keyId/keyClass/purpose | Duplicate-key checks; `stage_field_missing` |
| Remove/add/duplicate a `dataClasses` row | Count ≠ 28; A0 inventory mismatch; `stage_field_missing` / `a0_drift` |
| Remove/add/duplicate a deferred row | 23/18/5 partition and Q-ID order checks; `stage_field_missing` |
| Change a Q owner or destination | `deferred` row ≠ A0 §14; `stage_field_missing` |
| Change a CV row to a Q ID | Q-ID list/order mismatch; `stage_field_missing` |
| Add a Q row into the CV group or a CV row into A0 | CV absent-from-A0 / Q-order checks; `stage_field_missing` |
| Use D01 ciphertext with D02 key or AAD | Machine denies with `SECURITY_KEY_PURPOSE_MISMATCH` and zero writes (`sec-s1-03`) |
| Use an HMAC key for AEAD or a DEK for HMAC | Machine purpose/key-class rejection (`PURPOSE_MISMATCH`) |
| Corrupt envelope shape or key_ref | `ENVELOPE_MALFORMED` / `KEY_UNKNOWN` fail closed |
| Supply a malformed stored HMAC | `HMAC_MALFORMED` fail closed |
| Add a future invariant ID or S2/S3 section | `stage_field_premature` |
| Add a future fixture ID or wrong slice | `fixture_id_extra` / slice check / `failed` |
| Rename a fixture ID or mismatch file name | `fixture_id_missing` / `fixture_id_extra` |
| Change A0 catalog/sensitive count | `a0_drift` |
| Change A1 operation/scheme/audience/credential shape | `a1_drift` |
| Change A2 count, identity, event or digest | `a2_drift` |
| Change marker row order/columns/values | `doc_missing` / `doc_extra` / `doc_duplicate` / `doc_invalid` |

## 7. A0/A1/A2 drift evidence

| Fact | Accepted source value | S1 handling |
| --- | --- | --- |
| A0 catalog tables | 44 | Model upstream + validator assert 44 |
| A0 sensitive rows | 28 | Model `dataClasses` 28; each `a0Inventory` compared row-for-row |
| A0 envelope regex | `^v1\.[A-Za-z0-9._:/-]+\..+$` | Model/document/validator preserve exactly |
| A0 HMAC regex | `^[0-9a-f]{64}$` | Model/HMAC profile preserve exactly |
| A0 `auth_identity.key_version` rule | Present in §2.6 | Model `authIdentityVersionRule`; validator source check |
| A1 operations | 49 (18/21/6/3/1) | Validator source assertion |
| A1 security schemes | 6 | Validator exact set |
| A1 audiences | customer/employee/ivr-service/public-capability/provider | Validator set + model upstream |
| A1 credential fields | upload/media credentials 1–2048, readOnly, redaction required, not return-once; share tokens return-once | Validator source assertion |
| A2 messages/eventTypes/identities/invariants | 12/11/22/15 | Validator source assertion |
| A2 event registry digest | `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` | Recomputed with canonical JSON and asserted; no A2 file modified |

No drift was repaired by modifying an upstream file. If the controller’s executable run finds drift, the correct action is to stop and report, not to edit A0/A1/A2.

## 8. Documentation decision

- `docs/17-production-security-design-spec.md` is the only human canonical security document created in this slice. It contains the S1 scope, envelope/HMAC contract, key registry, data registry, rotation/recovery/replay rules, 23 deferred inputs, six invariants, validation/fixture description, residual table and completion template.
- No README, second security document, JSON Schema copy, package dependency, A1 schema copy, A0 table dictionary or A2 message table was created.
- S2/S3 security decisions were not authored; the document states they are deferred to their own gates.

## 9. Residual risks with owner/destination

| Residual | Owner / destination |
| --- | --- |
| `R-A3-HMAC-ROTATION-REPLAY` — HMAC rotation vs A2 `accessNumberHmac`/frozen HMAC needs runtime candidate/retention proof | Security/IVR/Backend; integration + Q3/Q25 |
| `R-A3-KMS-CAPABILITY` — KMS/Secret Manager envelope, rotate, recover and dual-version are unverified | Infrastructure/Security; Q2/CV validation |
| `R-A3-COS-CAPABILITY` — COS region/private endpoint/SSE-KMS/versioning/presign/object-lock/WORM are unverified | Infrastructure/Security; storage/capacity |
| `R-A3-KEY-DESTRUCTION` — a destroyed key is unrecoverable; no plaintext backup/escrow without separate approval | Security/Infrastructure; incident/recovery policy |
| `R-A3-LEGAL-RETENTION` — retention/erasure/backup inputs remain Q3 | Legal/Security; launch review |
| `R-A3-NO-WORM` — no COS WORM/object-lock claim | Infrastructure/Security; storage policy |
| `R-A3-REGION-SKU` — no region/SKU/price/capacity choice | Infrastructure/Security; capacity/deployment |
| Static-validator co-authorship risk — a self-consistent model can still be semantically wrong about a real backend | Adversarial controller/fixture review; later integration/QA |

## 10. No-runtime and no-cloud claim

No real KMS, Secret Manager, CAM, COS, Redis, PostgreSQL, IVR line, payment provider, logging platform, trace collector, analytics or support system was called, configured or observed. No real secret, URL, credential, phone number, identity or cloud identifier is present. The fixtures are model-level deterministic assertions, not production acceptance, penetration testing or capacity evidence. Fixture secrets are derived from `TEST_ONLY_*` labels; fixed nonces are evaluator inputs only.

## 11. Git, PR, review and slice declaration

- No `git add`, commit, push, pull request, merge, branch change or PR mutation was performed.
- No self-review, code review, independent review or gate acceptance was performed.
- No S2 or S3 implementation, model section, invariant text or fixture was authored.
- Only the ten allowed paths listed in §2 were created; every existing path remains read-only and the pre-existing dirty/untracked paths were not touched.

## 12. Stop statement

S1 author completion is declared. The controller must independently run the A1 `check:s3`, accepted A2 stage-S3 validation, `node --check security/validate.mjs`, two byte-identical S1 validator runs and the mutation/scope checks before accepting this slice or opening S2. This task stops here.

## 13. Correction addendum (2026-10-25)

> Correction task label: `a3-security-s1-correction1-ds-20261025-01`
>
> Correction artifact: `docs/gateflow/a3-production-security-design-spec-s1-correction-1.md`

This addendum records a bounded implementation correction after controller execution found a deterministic crash in the validator. The historical record in §5.2 is preserved unchanged: the original author environment denied Node/npm execution, so §5.3 counters were expectations, not observations. The controller then ran both required S1 commands and both crashed before emitting counters with:

```text
TypeError: Cannot read properties of undefined (reading 'filter')
    at machineSummary (security/validate.mjs:810:54)
```

**Confirmed cause.** `collectStrings(value, output)` mutated the supplied accumulator recursively but every path returned `undefined`. `machineSummary()` chained `.filter(...)` on the return value at lines 810-811. Call-site analysis found four categories: the two chaining call sites in `machineSummary`; recursive self-calls that ignore the return; the model scan at former line 1126 and the fixture scan at former line 1273 that pass and retain their own accumulators. Returning the accumulator is the smallest coherent fix: it repairs the two chaining sites and leaves every accumulator-passing caller's behavior equivalent.

**Change.** Only `security/validate.mjs` was modified in this correction: the five return paths in `collectStrings` now return the same `output` accumulator. A reconstructed baseline at SHA-256 `0E5471474FBB204C4A897EB3B41F0CEE161DC606B69000742F76228478546B6E` (identical to §2) was diffed against the corrected file, confirming the five-line delta; corrected SHA-256 is `E710661AC163E7137C48054C528079A83DCAFF02E1A6A72CA8F6D1FA7430F85F` (74,904 bytes, 1,617 lines). No model, fixture, `docs/17`, A0/A1/A2 or controller file was changed.

**Executed validation (this correction).** `node --check security/validate.mjs` exited 0. A1 `npm run check:s3` exited 0 with `PASS check-doc-map (s3)` and 49 operations. The accepted A2 stage-S3 validator exited 0 with `PASS protocol-validation (s3)`, 12 messages, 11 event types, 22 identities, 25/25 fixtures and 3 negatives. Both S1 runs exited 0 with byte-identical stdout (SHA-256 `3E991F1D7A075E826E70E7C2654568019A6A3644383ECECE4699AD82B2CAB0C6`), zero stderr, and exact counters `key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=6 fixtures=6 passed=6 failed=0 negative=2 slice_fixtures=6`, with `drift=0` and all documentation, token, stage and fixture mismatch counters zero. A 32-case temporary-copy mutation matrix rejected every required mutation; details, including one exploratory coverage observation recorded as a residual risk, are in the correction artifact.

No runtime, cloud, commit, push, PR, review or later gate was performed. This correction is bounded and stops here.

## 14. Correction-2 addendum (2026-10-25)

> Correction task label: `a3-security-s1-correction2-ds-20261025-01`
>
> Correction artifact: `docs/gateflow/a3-production-security-design-spec-s1-correction-2.md`

Correction 1 above fixed the crash but did **not** satisfy the accepted S1 purpose-separation requirement. Its exploratory probe showed that an isolated `encrypt` action using HMAC-class `K11` exited 0, because the fixture machine checked purpose/data-class coverage but not the AEAD key class. Static inspection also showed that `decrypt` lacked the same class guard; the correction-1 paired scenario failed only later with `KEY_MISMATCH` from a mismatched `K02` decrypt. Correction 2 addresses `F-S1-C1-01` directly.

**Change.** Only `security/validate.mjs` was modified (correction-1 SHA-256 `E710661AC163E7137C48054C528079A83DCAFF02E1A6A72CA8F6D1FA7430F85F`, 74,904 bytes / 1,617 lines). A single `isAeadKeyClass()` predicate now requires `DEK_*`, and is applied to all three AES-256-GCM call sites: `encrypt` (`createCipheriv`) and `decrypt` (`createDecipheriv`) return the existing `PURPOSE_MISMATCH` / `SECURITY_KEY_PURPOSE_MISMATCH` fail-closed result, and the `request` A2-replay path (`createDecipheriv`) returns the existing `HISTORICAL_KEY_UNAVAILABLE` result. The unchanged `lookup` path still rejects non-`HMAC_` keys, preserving DEK-for-HMAC rejection. Corrected SHA-256 is `BB5BF3B5B797B2A16193AFDE23B973D6B86992D24DF5A46EB09D71BC95D6E025` (75,195 bytes / 1,622 lines). No model, fixture, `docs/17`, plan, controller, correction-1 artifact or A0/A1/A2 source was changed.

**Executed validation (this correction).** `node --check` exited 0. A1 `npm run check:s3` exited 0 with `PASS check-doc-map (s3)` and 49 operations. The accepted A2 stage-S3 validator exited 0 with `PASS protocol-validation (s3)`, 12 messages, 11 event types, 22 identities, 25/25 fixtures and 3 negatives. Both exact S1 runs exited 0 with zero stderr and byte-identical stdout (SHA-256 `3E991F1D7A075E826E70E7C2654568019A6A3644383ECECE4699AD82B2CAB0C6`), preserving counters `20/28/23/18/5/6/6/2`, `passed=6 failed=0`, `drift=0` and all zero doc/token/stage/fixture counters. Focused temporary-copy probes independently rejected isolated HMAC-for-AEAD `encrypt` and a matching-key HMAC envelope fed to `decrypt`, both with computed `PURPOSE_MISMATCH` and without `KEY_MISMATCH`; the correction-1 validator accepted both under the same fixtures. All 32 correction-1 probes remain rejected, DEK-for-HMAC remains rejected, and valid DEK AEAD / HMAC lookup still pass.

No runtime, cloud, commit, push, PR, review or later gate was performed. `F-S1-C1-01` is author-complete pending controller revalidation.
