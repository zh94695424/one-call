# A3 S1 bounded implementation correction 1 — validator accumulator defect

> Work unit：`A3-production-security-design-spec`
>
> Slice：S1 — sensitive-data protection, key hierarchy, rotation/recovery and historical replay
>
> Gate：`implementation correction` for the approved S1 slice
>
> Task label：`a3-security-s1-correction1-ds-20261025-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Binding plan：`docs/gateflow/a3-production-security-design-spec-plan.md`, accepted SHA-256 `12E4136EE557D604FF680F1B162265C68171C5C724EF2888FB69D429007405F1` (re-verified)
>
> S1 implementation report：`docs/gateflow/a3-production-security-design-spec-s1-implementation.md`, now SHA-256 `2FA64B392554F6B194AE148421508D37B27823F5EFA8FD18ED4020672B1A6DE7` (20,691 bytes after the append-only §13 addendum)
>
> Stop condition：this correction only. No code review, S2, commit, push, PR, merge or branch change.

## 1. Scope and confirmed cause

Controller execution found both required S1 validator invocations crash before emitting counters:

```text
TypeError: Cannot read properties of undefined (reading 'filter')
    at machineSummary (security/validate.mjs:810:54)
    at Object.run (security/validate.mjs:795:24)
    at validateFixtures (security/validate.mjs:1296:30)
    at main (security/validate.mjs:1573:23)
```

The reproduction in this correction exited 1 with the same stack. Confirmed cause: `collectStrings(value, output)` recursively pushed into the supplied accumulator but every return path yielded `undefined`; `machineSummary()` chained `.filter(...)` directly on the return value at lines 810-811.

No S2/S3 model section, invariant text, fixture, behavior or acceptance criterion was added. The approved S1 counters and fail-closed behavior are unchanged.

## 2. Exact changed files and SHA-256

Only the three paths authorized by the correction task changed. All other S1 artifacts are byte-identical to the S1 report inventory.

| Path | Change | Lines | Bytes | SHA-256 |
| --- | --- | ---: | ---: | --- |
| `security/validate.mjs` | modified `collectStrings` return paths | 1,617 | 74,904 | `E710661AC163E7137C48054C528079A83DCAFF02E1A6A72CA8F6D1FA7430F85F` |
| `docs/gateflow/a3-production-security-design-spec-s1-implementation.md` | appended §13 correction addendum | 214 | 20,691 | `2FA64B392554F6B194AE148421508D37B27823F5EFA8FD18ED4020672B1A6DE7` |
| `docs/gateflow/a3-production-security-design-spec-s1-correction-1.md` | this artifact (new) | this artifact | this artifact | reported in the final response; re-verifiable by the controller |

Pre-change validator baseline: `0E5471474FBB204C4A897EB3B41F0CEE161DC606B69000742F76228478546B6E`, 74,866 bytes, 1,616 lines — identical to the hash recorded in S1 report §2. A reconstructed baseline produced that exact hash and the diff below, proving the delta is limited to the five return paths:

```diff
@@ -411,16 +411,17 @@
 function collectStrings(value, output) {
   if (typeof value === 'string') {
     output.push(value);
-    return;
+    return output;
   }
   if (value === null || typeof value !== 'object') {
-    return;
+    return output;
   }
   if (Array.isArray(value)) {
     value.forEach((item) => collectStrings(item, output));
-    return;
+    return output;
   }
   Object.values(value).forEach((child) => collectStrings(child, output));
+  return output;
 }
```

Unchanged S1 hashes re-verified: model `13E08CC6A01BEE69F2ADC6C57842B5A5D1B0D9147FAE6771E84E6EE1301AB81D`; `docs/17` `B252B2B96B4AD50C8ACC4AA8D97C6AF2C48C91F0A95245FEB8AEFB1E665A9581`; all six fixtures match S1 report §2 (`844B67A4…`, `2ACEC51F…`, `5CBFA710…`, `6A9FBD95…`, `CAA71DFF…`, `00D85E44…`).

## 3. Implementation decision and call-site analysis

All `collectStrings` call sites were inspected before choosing the fix:

| Call site | Use of return value | Effect of returning `output` |
| --- | --- | --- |
| `machineSummary` lines 810-811 | chains `.filter(...)` | repairs the crash with no other change |
| recursive self-calls lines 420 and 423 | ignored | no behavior change |
| model scan line 1126 (`modelStrings`) | ignored; accumulator mutated in place | no behavior change |
| fixture scan line 1273 (`fixtureStrings`) | ignored; accumulator mutated in place | no behavior change |

Returning the accumulator is the smallest coherent fix: one function, five return paths, no chaining-site duplication and no change to any accumulator-passing caller. It also makes the function's contract unsurprising for future callers. No validator check, counter, threshold or fail-closed path was weakened or strengthened.

## 4. Exact commands, exit status, counters and two-run equality

Run from `/mnt/d/codex-call` on Node `v22.23.4` / npm `10.9.9`.

| # | Command | Exit | Observed result |
| --- | --- | ---: | --- |
| 1 | `node --check security/validate.mjs` | 0 | no diagnostics |
| 2 | `cd api/openapi && npm run check:s3` | 0 | `PASS check-doc-map (s3)`; operations `49` = `18/21/6/3/1`; all doc/A0/path/source duplicate counters zero |
| 3 | `node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md` | 0 | `PASS protocol-validation (s3)`; `messages=12 event_types=11 identities=22 fixtures=25 passed=25 failed=0 negative=3 slice_fixtures=6+11+8`; all A0/A1 drift counters zero; event digest `c6b769…f4e9` |
| 4 | `node security/validate.mjs --stage s1 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures` (run 1) | 0 | counters below; stderr 0 bytes |
| 5 | same S1 command (run 2) | 0 | counters below; stderr 0 bytes; stdout byte-identical to run 1 |

Exact S1 stdout for both runs:

```text
stage=s1 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=6 fixtures=6 passed=6 failed=0 negative=2 slice_fixtures=6
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s1)
```

Two-run equality: SHA-256 `3E991F1D7A075E826E70E7C2654568019A6A3644383ECECE4699AD82B2CAB0C6` for both stdout captures; `cmp` reported no difference. All counters required by the task match exactly, including `drift=0` and zero documentation, token, stage and fixture counters.

Encoding and scope checks on the three allowed paths:

| Check | Result |
| --- | --- |
| `file -bi` | validator `text/plain; charset=us-ascii` (valid UTF-8 subset); both Markdown files `text/plain; charset=utf-8` |
| `iconv -f UTF-8 -t UTF-8` | exit 0 for all three files |
| `grep -nE '^(<<<<<<<\|=======\|>>>>>>>)'` | no matches (grep exit 1) |
| `grep -n '[[:blank:]]$'` | no matches (grep exit 1) |
| `git diff --check -- <three paths>` | exit 0, no output |
| `git diff --no-index --check /dev/null <each path>` | no whitespace issues |
| `git status --short` | pre-existing dirty/untracked paths preserved; the only new untracked path attributable to this task is this correction artifact (the other two allowed paths already existed as untracked S1 artifacts); no added/deleted/renamed tracked entry |

## 5. Mutation probe matrix

Method: a runner outside the repository (`/tmp/a3-s1-probes/run-probes.mjs`) copied the model, fixtures, `docs/17`, A0, A1 bundle, A2 model and A2 fixtures into a throwaway directory, applied one mutation per probe, and invoked the corrected validator with the same S1 arguments pointed at the copies. No repository model, fixture or specification was modified. Every required probe asserts exit 1 plus the observed rejection message. Result: **32 required probes, 32 rejected, 0 accepted**. Evidence: `/tmp/a3-s1-probes/evidence/<probe>.stderr.txt` and `/tmp/a3-s1-probes/results.json`.

| Probe | Mutation | Exit | Observed rejection |
| --- | --- | ---: | --- |
| `key-missing` | remove `K20` | 1 | `model.keyClasses count is 19, expected 20`; marker row count 20 vs 19 |
| `key-extra` | append duplicate `K20` | 1 | `count is 21, expected 20`; `extra row at index 20` |
| `key-duplicate` | replace `K20` with duplicate `K19` | 1 | `duplicate keyId K19`; registry drift |
| `data-missing` | remove `D28` | 1 | `dataClasses count is 27, expected 28`; A0 inventory mismatch |
| `data-extra` | append duplicate `D28` | 1 | `count is 29, expected 28`; `extra row at index 28` |
| `data-duplicate` | replace `D28` with duplicate `D27` | 1 | `duplicate dataClass D27`; registry drift |
| `deferred-missing` | remove `CV-5` | 1 | `deferredInputs count is 22, expected 23`; `18 Q + 4 CV` |
| `deferred-extra` | append duplicate `Q1` | 1 | `count is 24, expected 23`; Q rows duplicated/out of order |
| `deferred-duplicate` | replace `CV-5` with duplicate `Q1` | 1 | `19 Q + 4 CV, expected 18 + 5`; Q order mismatch |
| `deferred-q-owner-drift` | change `Q1` owner | 1 | `deferred Q1 input/owner/destination drift from A0 section 14` |
| `deferred-q-destination-drift` | change `Q3` destination | 1 | `deferred Q3 … drift from A0 section 14` |
| `deferred-q-input-drift` | change `Q6` input | 1 | `deferred Q6 … drift from A0 section 14` |
| `deferred-cv-provenance-drift` | change `CV-1` input | 1 | `deferred CV-1 drifts from the A3-origin CV registry` |
| `deferred-non-q-cv-id` | rename `CV-5` to `XX-5` | 1 | `deferred input XX-5 is neither Q nor CV`; CV count 4 |
| `future-invariant-model` | add `INV-A3-07` | 1 | `model contains a future S2/S3 invariant ID`; invariants 7 |
| `future-invariant-doc` | append `INV-A3-07` to spec copy | 1 | `document contains a future S2/S3 invariant ID` |
| `future-fixture-file` | add `sec-s2-99-future.json` (slice `s2`) | 1 | `extra fixture ID sec-s2-99-future`; `slice must be s1` |
| `fixture-wrong-slice` | change `sec-s1-01` slice to `s2` | 1 | `sec-s1-01-…: slice must be s1` |
| `fixture-file-id-mismatch` | rename `sec-s1-02` file | 1 | `file name …-wrong.json does not match ID sec-s1-02-…` |
| `fixture-duplicate-id` | copy a fixture under another file name | 1 | `duplicate fixture ID detected`; file/ID mismatch |
| `a0-drift` | alter A0 sensitive row 1 cell | 1 | `dataClass D01 a0Inventory drifts from A0 sensitive row 1` |
| `a1-drift` | delete `customerSession` scheme from bundle copy | 1 | `A1 security scheme set drift` |
| `a2-drift` | remove one A2 message | 1 | `A2 messages count is 11, expected 12` |
| `cross-purpose-aad` | decrypt D01 with AAD for `IDENTITY_PROFILE:D01` | 1 | computed `DENIED` / `AAD_MISMATCH` |
| `cross-purpose-wrong-key` | decrypt D02 envelope with `K02` | 1 | computed `DENIED` / `KEY_MISMATCH` |
| `hmac-dek-purpose-misuse` | lookup D01 with HMAC `K13` (phone-purpose key) | 1 | computed `DENIED` / `PURPOSE_MISMATCH` |
| `dek-for-hmac` | lookup with DEK `K02` | 1 | computed `DENIED` / `PURPOSE_MISMATCH` |
| `envelope-malformed` | malformed envelope text | 1 | computed `DENIED` / `ENVELOPE_MALFORMED` |
| `envelope-key-ref-unknown` | well-formed envelope, unknown key ref | 1 | computed `DENIED` / `KEY_UNKNOWN` |
| `hmac-malformed-stored` | malformed stored HMAC | 1 | computed `DENIED` / `HMAC_MALFORMED` |
| `append-only-rewrite` | rewrite `D08` while expecting success | 1 | computed `DENIED` / `APPEND_ONLY_VIOLATION` |
| `hmac-for-aead-scenario` | encrypt with HMAC `K11`, paired decrypt | 1 | computed `DENIED` / `KEY_MISMATCH` |

One exploratory probe was run outside the required matrix and is reported honestly as a residual risk in §8:

- `exploratory-hmac-for-aead-encrypt-only`: an isolated `encrypt` step using HMAC-class `K11` with the matching purpose returned **exit 0 / `passed=6 failed=0`**, i.e. the encrypt action alone did not reject the HMAC-class key. In the full paired scenario (`hmac-for-aead-scenario`) the subsequent decrypt with `K02` was rejected with `KEY_MISMATCH`, so the fixture as a whole still fails closed.

## 6. A1/A2 regression evidence

| Contract | Command | Exit | Evidence |
| --- | --- | ---: | --- |
| A1 OpenAPI S3 | `cd api/openapi && npm run check:s3` | 0 | `PASS check-doc-map` and `PASS check-doc-map (s3)`; 49 operations `18/21/6/3/1`; A0 catalog 44 with zero missing/extra/duplicate; zero source duplicate counters |
| A2 IVR S3 | accepted stage-S3 validator (exact plan §7.1 arguments) | 0 | `PASS protocol-validation (s3)`; 12 messages, 11 event types, 22 identities, 25/25 fixtures, 3 negatives, `slice_fixtures=6+11+8`; `a1_drift=0`, A0 drift counters 0; event digest `c6b769…f4e9` |

No A0/A1/A2 source was modified.

## 7. Documentation decision

- `docs/17-production-security-design-spec.md` was deliberately **not** changed. The correction fixes a validator implementation defect; the security design, registry rows, invariants and fixture semantics are unchanged, so changing the canonical document would create unsupported drift.
- The existing S1 implementation report received a concise §13 addendum that references this artifact, records the confirmed cause/fix and executed evidence, and explicitly preserves the §5.2 historical truth that the original author environment denied Node/npm execution (§5.3 counters were expectations, not observations).
- This correction artifact is the only new document. No README, model section, fixture, schema or S2/S3 text was added.

## 8. Residual risks and uncovered areas

| Residual / uncovered area | Owner / destination |
| --- | --- |
| `R-A3-S1-HMAC-AEAD-GUARD` — the validator's `encrypt` action does not independently reject HMAC-class key material; the exploratory isolated encrypt probe exited 0. Purpose separation is still enforced end-to-end by the paired decrypt (`KEY_MISMATCH`) and by lookup/decrypt class checks. Not changed here because adding a new encrypt guard is validator behavior beyond this bounded crash correction and would alter S1 acceptance semantics. | Security design owner / controller; decide in S1 re-validation or S2 validator hardening |
| `R-A3-HMAC-ROTATION-REPLAY` — HMAC rotation vs A2 `accessNumberHmac`/frozen HMAC needs runtime candidate/retention proof | Security/IVR/Backend; integration + Q3/Q25 |
| `R-A3-KMS-CAPABILITY` — KMS/Secret Manager envelope, rotate, recover and dual-version are unverified | Infrastructure/Security; Q2/CV validation |
| `R-A3-COS-CAPABILITY` — COS region/private endpoint/SSE-KMS/versioning/presign/object-lock/WORM are unverified | Infrastructure/Security; storage/capacity |
| `R-A3-KEY-DESTRUCTION` — a destroyed key is unrecoverable; no plaintext backup/escrow without separate approval | Security/Infrastructure; incident/recovery policy |
| `R-A3-LEGAL-RETENTION` — retention/erasure/backup inputs remain Q3 | Legal/Security; launch review |
| `R-A3-NO-WORM`, `R-A3-REGION-SKU` — no WORM or region/SKU/price/capacity claim | Infrastructure/Security; storage and capacity/deployment |
| Static-validator co-authorship — a self-consistent model can still be semantically wrong about a real backend | Adversarial controller/fixture review; later integration/QA |
| Mutation probes exercise the validator's static checks and fixture machine on temporary copies; they are not runtime, penetration, concurrency or cloud evidence | Controller/fixture review; integration/QA |

## 9. No-runtime and no-cloud claim

No real KMS, Secret Manager, CAM, COS, Redis, PostgreSQL, IVR line, payment provider, logging platform, trace collector, analytics or support system was called, configured or observed. No real secret, URL, credential, phone number, identity or cloud identifier was used. All fixture material remains derived from `TEST_ONLY_*` labels. Probe copies and evidence live outside the repository under `/tmp/a3-s1-probes`; no temporary file was written into the repository.

## 10. Completion status and artifact path

- Status：**complete** for this bounded correction. The confirmed crash is fixed; both required S1 runs exit 0 with byte-identical output and all required counters; A1/A2 regression, 32/32 required mutation probes, encoding/whitespace and scope checks pass. One exploratory validator-coverage gap is disclosed in §8 with owner/destination.
- Artifact path：`docs/gateflow/a3-production-security-design-spec-s1-correction-1.md`.
- No code review, S2/S3 authoring, staging, commit, push, PR, merge, branch change or controller edit was performed.
