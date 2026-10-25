# A3 S1 bounded implementation correction 2 — AEAD/DEK class boundary

> Work unit：`A3-production-security-design-spec`
>
> Slice：S1 — sensitive-data protection, key hierarchy, rotation/recovery and historical replay
>
> Gate：`implementation correction` for the approved S1 slice
>
> Task label：`a3-security-s1-correction2-ds-20261025-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Binding plan：`docs/gateflow/a3-production-security-design-spec-plan.md`, accepted SHA-256 `12E4136EE557D604FF680F1B162265C68171C5C724EF2888FB69D429007405F1`
>
> Canonical design：`docs/17-production-security-design-spec.md`, SHA-256 `B252B2B96B4AD50C8ACC4AA8D97C6AF2C48C91F0A95245FEB8AEFB1E665A9581` (unchanged)
>
> Prior correction：`docs/gateflow/a3-production-security-design-spec-s1-correction-1.md`, SHA-256 `F713EAEDBA4462CFDB65FF962ADF00116F66D180FA43542CFD94AA748BCE4B80` (unchanged)
>
> Stop condition：this correction only. No self-review, code review, S2/S3 authoring, staging, commit, push, PR, merge, branch change or controller edit.

## 1. Gate, scope and accepted finding

The controller accepted `F-S1-C1-01` against correction 1: the security fixture machine proved that an isolated `encrypt` action using HMAC-class `K11` exited 0, and static inspection showed that a matching-key `decrypt` had no explicit DEK-class guard. The correction-1 paired scenario failed only because a later decrypt still named DEK `K02`, yielding `KEY_MISMATCH`; that was not evidence that the AEAD boundary rejected HMAC key material. This violates accepted plan §4.3 (HMAC keys are an independent branch and do not participate in the encryption hierarchy), plan §6.9 (an HMAC key used for AEAD must be rejected), and `docs/17` §4 (HMAC keys must not encrypt).

Scope is one validator enforcement change: AEAD `encrypt` and `decrypt` may only accept approved `DEK_*` key classes, while existing dataClass/purpose/AAD/state checks and error conventions are preserved. No model, fixture, design, plan, controller, A1/A2/A0 or correction-1 artifact was changed.

## 2. Exact changed paths and SHA-256

Only the three paths allowed by the correction task changed.

| Path | Change | Lines | Bytes | SHA-256 |
| --- | --- | ---: | ---: | --- |
| `security/validate.mjs` | added AEAD class predicate; applied at three AES-GCM call sites | 1,622 | 75,195 | `BB5BF3B5B797B2A16193AFDE23B973D6B86992D24DF5A46EB09D71BC95D6E025` |
| `docs/gateflow/a3-production-security-design-spec-s1-implementation.md` | appended §14 correction-2 addendum | 228 | 23,395 | `DB806F842314E5E011EEB06B5A0C0114F23E6C1797E637563F74BBEB293992F0` |
| `docs/gateflow/a3-production-security-design-spec-s1-correction-2.md` | this artifact (new) | this artifact | this artifact | reported in the final response; re-verifiable by the controller |

Pre-change baseline (correction 1): `E710661AC163E7137C48054C528079A83DCAFF02E1A6A72CA8F6D1FA7430F85F`, 74,904 bytes, 1,617 lines. The delta is +5 lines / +291 bytes. Exact diff:

```diff
@@ -438,6 +438,10 @@
     .includes(dataClass);
 }

+function isAeadKeyClass(keyRow) {
+  return Boolean(keyRow) && typeof keyRow.keyClass === 'string' && keyRow.keyClass.startsWith('DEK_');
+}
+
 function derivePurpose(keyById, dataRow) {
@@ -539,7 +543,7 @@
         const expectedPurpose = derivePurpose(keyById, dataRow);
-        if (!expectedPurpose || step.purpose !== expectedPurpose || !keyCoversDataClass(keyRow, step.dataClass)) {
+        if (!expectedPurpose || step.purpose !== expectedPurpose || !keyCoversDataClass(keyRow, step.dataClass) || !isAeadKeyClass(keyRow)) {
           return denied('PURPOSE_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH');
         }
@@ -595,7 +599,7 @@
         const keyRow = keyById.get(material.keyId);
         const dataRow = dataById.get(step.dataClass);
-        if (!keyRow || !dataRow || !keyCoversDataClass(keyRow, step.dataClass)) {
+        if (!keyRow || !dataRow || !keyCoversDataClass(keyRow, step.dataClass) || !isAeadKeyClass(keyRow)) {
           return denied('PURPOSE_MISMATCH', 'SECURITY_KEY_PURPOSE_MISMATCH', { keyState: material.state, keyRef: material.keyRef });
         }
@@ -763,7 +767,8 @@
         const material = materialFor(parsed.keyRef);
-        if (!material || !stateAllows(material.state, 'decrypt')) {
+        const keyRow = material ? keyById.get(material.keyId) : null;
+        if (!material || !stateAllows(material.state, 'decrypt') || !isAeadKeyClass(keyRow)) {
           return { status: 'DENIED', reason: 'HISTORICAL_KEY_UNAVAILABLE' };
         }
```

Unchanged S1 hashes re-verified after this task: model `13E08CC6A01BEE69F2ADC6C57842B5A5D1B0D9147FAE6771E84E6EE1301AB81D`; `docs/17` `B252B2B96B4AD50C8ACC4AA8D97C6AF2C48C91F0A95245FEB8AEFB1E665A9581`; all six fixtures `844B67A4…`, `2ACEC51F…`, `5CBFA710…`, `6A9FBD95…`, `CAA71DFF…`, `00D85E44…`; accepted plan `12E4136E…`; correction-1 artifact `F713EAED…`.

## 3. Call-path reasoning and completeness

All cryptographic machine actions were inspected:

| Machine action | Primitive | Boundary decision |
| --- | --- | --- |
| `encrypt` | `crypto.createCipheriv('aes-256-gcm', …)` | **Changed.** `!isAeadKeyClass(keyRow)` added to the existing purpose/data-class condition. A non-`DEK_*` row (HMAC, signing, COS CMK, KMS CMK) now returns the pre-existing `PURPOSE_MISMATCH` / `SECURITY_KEY_PURPOSE_MISMATCH` result before any AES call. |
| `decrypt` | `crypto.createDecipheriv('aes-256-gcm', …)` | **Changed.** Same predicate added to the existing key/data-class coverage condition, returning the same `PURPOSE_MISMATCH` fail-closed result. It executes before expected-purpose, AAD and GCM work. |
| `request` (`a2-decision-replay`) | `crypto.createDecipheriv('aes-256-gcm', …)` | **Changed.** This third GCM decryption path selects material from the envelope `key_ref`; it now also requires a `DEK_*` row and returns the existing `HISTORICAL_KEY_UNAVAILABLE` fail-closed result otherwise. No repository fixture uses a non-DEK replay. |
| `lookup` | `crypto.createHmac('sha256', …)` | **Unchanged.** Already requires `keyClass.startsWith('HMAC_')`; a DEK such as `K02` is rejected with `PURPOSE_MISMATCH`, preserving DEK-for-HMAC rejection. |
| `rotate`, `retire`, `recover` | none | Key lifecycle only; no cipher/HMAC primitive is invoked. Rotation still derives old state from the class (`LOOKUP_ONLY` for `HMAC_`, `VERIFY_ONLY` for signing, otherwise `DECRYPT_ONLY`). |
| `persist`, `assert` | none | Append-only/stage assertions only. |

Ordering preserves existing error conventions. In `encrypt` and `decrypt`, the explicit `step.keyId` mismatch check still runs first, so a caller naming a different key still receives `KEY_MISMATCH`; a caller naming the matching key identity receives the class rejection. Existing wrong-purpose, AAD, key-state, missing-envelope, malformed-envelope and unknown-ref paths are unchanged.

Why `DEK_*` and not `COS_CMK_MEDIA`: `K10` is `COS SSE-KMS CV-4`, not the local AES-256-GCM/12/16/AAD envelope profile. `derivePurpose()` still recognizes `COS_CMK_MEDIA` for purpose derivation (unchanged), but the local GCM machine now rejects `K10` exactly as it rejects HMAC/signing/KEK rows. No fixture encrypts or decrypts `D19` through this machine, so there is no behavior or counter change. `K01` was already excluded because its `dataClasses` value (`K02-K09`) never covers a data class.

Completeness for `F-S1-C1-01`: all three AES-256-GCM call sites in the validator are now behind the same DEK-only predicate; the only HMAC call site retains its HMAC-only predicate. No new model section, invariant, fixture, stage behavior or cloud/runtime claim was added.

## 4. Exact commands, exit status, counters and two-run equality

Run from `/mnt/d/codex-call` on Node `v22.23.4` / npm `10.9.9`.

| # | Command | Exit | Observed result |
| --- | --- | ---: | --- |
| 1 | `node --check security/validate.mjs` | 0 | no diagnostics |
| 2 | `cd api/openapi && npm run check:s3` | 0 | `PASS check-doc-map (s3)`; operations `49` = `18/21/6/3/1`; doc/A0/path/source duplicate counters zero; no lockfile change |
| 3 | `node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md` | 0 | `PASS protocol-validation (s3)`; `messages=12 event_types=11 identities=22 fixtures=25 passed=25 failed=0 negative=3 slice_fixtures=6+11+8`; `a1_drift=0`; A0 drift counters 0; event digest `c6b769…f4e9` |
| 4 | exact S1 command from plan §7.1 (run 1) | 0 | counters below; stderr 0 bytes |
| 5 | same S1 command (run 2) | 0 | counters below; stderr 0 bytes; stdout byte-identical to run 1 |

Exact S1 stdout for both runs:

```text
stage=s1 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=6 fixtures=6 passed=6 failed=0 negative=2 slice_fixtures=6
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s1)
```

Two-run equality: SHA-256 `3E991F1D7A075E826E70E7C2654568019A6A3644383ECECE4699AD82B2CAB0C6` for both stdout captures; `cmp` exit 0 (no difference). Both stderr captures are 0 bytes (`E3B0C442…B855`). All task-required counters match exactly: `20/28/23/18/5/6/6/2`, `passed=6 failed=0`, `drift=0`, and zero doc/token/stage/fixture counters.

## 5. Focused mutation probes — `F-S1-C1-01`

Method: an external runner (`/tmp/a3-s1-correction2/run-probes2.mjs`) reset a throwaway tree per probe, copied the repository model, fixtures, `docs/17`, A0, A1 bundle, A2 model and A2 fixtures, applied one mutation, and ran either the corrected validator (`BB5BF3…`) or the preserved correction-1 validator (`E710661A…`) with the exact S1 arguments pointed at the copies. No repository artifact was mutated. Evidence is under `/tmp/a3-s1-correction2/evidence/`; summary in `/tmp/a3-s1-correction2/results2.json`. Result: **32/32 legacy correction-1 probes rejected, 6/6 focused before/after probes as expected, 1/1 valid control passed.**

Direct `F-S1-C1-01` evidence (sentinel `reason="__PROBE_EXPECT_OK__"` forces the computed value into stderr):

| Probe | Fixture construction | Validator | Exit | Observed computed result |
| --- | --- | ---: | ---: | --- |
| `focus-encrypt-hmac-corrected` | single `encrypt`, `K11` / `TEST_ONLY_K11_V1`, purpose `PROVIDER_SUBJECT`, dataClass `D01` | corrected | 1 | `status … computed "DENIED"`; `reason … computed "PURPOSE_MISMATCH"`; no `KEY_MISMATCH` anywhere |
| `focus-encrypt-hmac-c1-accepts` | same, expected `{status:"OK"}` | correction 1 | 0 | `passed=6 failed=0`; HMAC encrypt accepted |
| `focus-decrypt-hmac-matching-corrected` | single `decrypt`, keyId `K11`, envelope produced externally with the machine's deterministic `sha256(TEST_ONLY_K11_HMAC_V1)` AES key, AAD `onecall:v1:PROVIDER_SUBJECT:D01` | corrected | 1 | `status … computed "DENIED"`; `reason … computed "PURPOSE_MISMATCH"`; no `KEY_MISMATCH` anywhere |
| `focus-decrypt-hmac-matching-c1-accepts` | same matching-key envelope, expected `{status:"OK"}` | correction 1 | 0 | `passed=6 failed=0`; matching-key HMAC decrypt accepted |
| `focus-request-hmac-literal-corrected` | single `request`/`a2-decision-replay` with a literal HMAC-key envelope | corrected | 1 | `reason … computed "HISTORICAL_KEY_UNAVAILABLE"` |
| `focus-request-hmac-literal-c1-accepts` | same, expected `{status:"OK"}` | correction 1 | 0 | `passed=6 failed=0`; HMAC-key replay accepted |
| `control-valid-dek-aead-and-hmac-lookup` | unmutated copies | corrected | 0 | `passed=6 failed=0`, `negative=2`, all counters zero; valid DEK AEAD roundtrips and HMAC lookups pass |

The matching-key probe does not use `KEY_MISMATCH`: the step names `K11`, the envelope `key_ref` is `TEST_ONLY_K11_V1`, and both resolve to the same material. The rejection is therefore the class/purpose boundary itself, not a later identity or AAD mismatch.

Legacy correction-1 matrix re-run against the corrected validator (all exit 1, all expected stderr markers observed):

| Group | Probes | Observed rejection class |
| --- | --- | --- |
| Key registry | `key-missing`, `key-extra`, `key-duplicate` | key count/row drift, duplicate keyId |
| Data registry | `data-missing`, `data-extra`, `data-duplicate` | data count/row drift, A0 inventory mismatch |
| Deferred registry | `deferred-missing`, `deferred-extra`, `deferred-duplicate`, `deferred-q-owner-drift`, `deferred-q-destination-drift`, `deferred-q-input-drift`, `deferred-cv-provenance-drift`, `deferred-non-q-cv-id` | 18 Q + 5 CV partition, A0 §14 drift, CV provenance |
| Future slice | `future-invariant-model`, `future-invariant-doc`, `future-fixture-file` | future invariant/fixture rejection |
| Fixture identity | `fixture-wrong-slice`, `fixture-file-id-mismatch`, `fixture-duplicate-id` | slice/file-name/ID rejection |
| Upstream drift | `a0-drift`, `a1-drift`, `a2-drift` | A0 sensitive row, A1 scheme set, A2 message count |
| Crypto misuse | `cross-purpose-aad`, `cross-purpose-wrong-key`, `hmac-dek-purpose-misuse`, `dek-for-hmac`, `envelope-malformed`, `envelope-key-ref-unknown`, `hmac-malformed-stored`, `append-only-rewrite`, `hmac-for-aead-scenario` | `AAD_MISMATCH`, `KEY_MISMATCH`, `PURPOSE_MISMATCH`, `ENVELOPE_MALFORMED`, `KEY_UNKNOWN`, `HMAC_MALFORMED`, `APPEND_ONLY_VIOLATION` |

`hmac-for-aead-scenario` was updated from the correction-1 false-positive expectation to assert the accepted failure: the first step now computes `DENIED` / `PURPOSE_MISMATCH` directly, and the probe forbids any `KEY_MISMATCH` match. `dek-for-hmac` still computes `PURPOSE_MISMATCH`, preserving DEK-for-HMAC rejection.

## 6. A1/A2 and drift regression

| Contract | Command | Exit | Evidence |
| --- | --- | ---: | --- |
| A1 OpenAPI S3 | `cd api/openapi && npm run check:s3` | 0 | `PASS check-doc-map` and `PASS check-doc-map (s3)`; 49 operations `18/21/6/3/1`; A0 catalog 44 with zero missing/extra/duplicate; zero source duplicate counters |
| A2 IVR S3 | accepted stage-S3 validator with plan §7.1 arguments | 0 | `PASS protocol-validation (s3)`; 12 messages, 11 event types, 22 identities, 25/25 fixtures, 3 negatives, `slice_fixtures=6+11+8`; `a1_drift=0`; A0 drift counters 0; digest `c6b769…f4e9` |
| S1 upstream counters | two exact S1 runs | 0 | `a0_tables=44`, `a0_sensitive_rows=28`, `a1_operations=49`, `a1_schemes=6`, A2 `12/11/22/15`, `drift=0` |

No A0/A1/A2 source, bundle source, lockfile or protocol file was modified. The prescribed A1 command regenerated only the gitignored `tmp/openapi-client` outputs.

## 7. Encoding, whitespace and scope

| Check | Result |
| --- | --- |
| `file -bi` | validator `text/plain; charset=us-ascii` (valid UTF-8 subset); both Markdown files `text/plain; charset=utf-8` |
| `iconv -f UTF-8 -t UTF-8` | exit 0 for all three allowed paths |
| conflict-marker grep | no matches (grep exit 1) |
| trailing-whitespace grep | no matches (grep exit 1) |
| `git diff --check -- security` | exit 0, no output |
| `git diff --no-index --check /dev/null <path>` | exit 1 (files differ from `/dev/null`) with 0 bytes of output, i.e. no whitespace diagnostics |
| `git status --short` | only the three allowed A3 paths are attributable to this task; all pre-existing dirty screenshot/review paths and the earlier S1 artifacts are untouched |
| lockfile check | `git status --short api/openapi` empty; `git diff --stat -- api/openapi` empty |

## 8. Documentation decision

- `docs/17-production-security-design-spec.md` was deliberately **not** changed. The accepted design already states in §4 that HMAC keys do not participate in encryption and that cross-purpose use must fail closed; plan §4.3 and §6.9 already forbid HMAC-for-AEAD. The defect was validator enforcement, so editing the canonical document would create unsupported drift.
- The model and all six fixtures were not changed: the required behavior is a validator boundary, and the repository fixtures already use only `DEK_*` for AEAD. Adding an HMAC-for-AEAD negative fixture would change the frozen six-fixture S1 inventory and counters, which this bounded correction must not do.
- The S1 implementation report received a concise append-only §14 addendum that records correction 1's limitation, this change, hashes and executed evidence. Correction 1 is **not** rewritten as having passed this requirement.
- This correction artifact is the only new document.

## 9. Residual risks and uncovered areas

| Residual / uncovered area | Owner / destination |
| --- | --- |
| `R-A3-S1-HMAC-AEAD-GUARD` (from correction 1) — **closed by this correction** at all three AES-GCM call sites; the controller must independently revalidate the hash-bound evidence | Controller revalidation; no further S1 code change requested |
| `R-A3-S1-AEAD-CLASS-STATIC` — the guard proves the model-level key-class boundary (`DEK_*`) only; it does not prove that a real KMS/Secret Manager key is purpose-bound or that runtime code calls this boundary | Security/Infrastructure; CV-1 runtime capability validation and implementation review |
| `R-A3-S1-REQUEST-CLASS-REASON` — the replay path maps a non-DEK class to the existing `HISTORICAL_KEY_UNAVAILABLE` reason rather than `PURPOSE_MISMATCH`; no repository fixture exercises a non-DEK replay | Controller review; revisit only if an S2/S3 fixture needs a finer replay reason |
| `R-A3-S1-COS-PURPOSE` — `derivePurpose()` still recognizes `COS_CMK_MEDIA` while the local GCM machine rejects `K10`; this matches the design (COS media is SSE-KMS, not the local envelope), and no fixture uses `K10` in the GCM machine | Security/Infrastructure; CV-4 storage validation |
| `R-A3-HMAC-ROTATION-REPLAY` — HMAC rotation vs A2 `accessNumberHmac`/frozen HMAC needs runtime candidate/retention proof | Security/IVR/Backend; integration + Q3/Q25 |
| `R-A3-KMS-CAPABILITY` — KMS/Secret Manager envelope, rotate, recover and dual-version are unverified | Infrastructure/Security; Q2/CV validation |
| `R-A3-COS-CAPABILITY` — COS region/private endpoint/SSE-KMS/versioning/presign/object-lock/WORM are unverified | Infrastructure/Security; storage/capacity |
| `R-A3-KEY-DESTRUCTION` — a destroyed key is unrecoverable; no plaintext backup/escrow without separate approval | Security/Infrastructure; incident/recovery policy |
| `R-A3-LEGAL-RETENTION` — retention/erasure/backup inputs remain Q3 | Legal/Security; launch review |
| `R-A3-NO-WORM`, `R-A3-REGION-SKU` — no WORM or region/SKU/price/capacity claim | Infrastructure/Security; storage and capacity/deployment |
| Static-validator co-authorship — a self-consistent model can still be semantically wrong about a real backend | Adversarial controller/fixture review; later integration/QA |
| Mutation probes exercise static checks and the fixture machine on temporary copies; they are not runtime, penetration, concurrency or cloud evidence | Controller/fixture review; integration/QA |

## 10. No-runtime and no-cloud claim

No real KMS, Secret Manager, CAM, COS, Redis, PostgreSQL, IVR line, payment provider, logging platform, trace collector, analytics or support system was called, configured or observed. No real secret, URL, credential, phone number, identity or cloud identifier was used. The probe envelope was generated locally with Node `crypto` from the `TEST_ONLY_K11_HMAC_V1` label, exactly as the validator derives fixture material; it is not production evidence. Probe copies and evidence live outside the repository under `/tmp/a3-s1-correction2`; no temporary file was written into the repository.

## 11. Completion status and artifact path

- Status：**complete** for this bounded correction. `F-S1-C1-01` is author-complete: isolated HMAC-for-AEAD `encrypt`, matching-key HMAC `decrypt`, and HMAC-key replay are rejected at the class boundary; DEK-for-HMAC remains rejected; valid DEK AEAD and HMAC lookup still pass; both exact S1 runs are green with unchanged counters; A1/A2 regression and the full 32-probe legacy matrix pass.
- Artifact path：`docs/gateflow/a3-production-security-design-spec-s1-correction-2.md`.
- No self-review, code review, staging, commit, push, PR, merge, branch change, controller update, S2 or S3 work was performed.
