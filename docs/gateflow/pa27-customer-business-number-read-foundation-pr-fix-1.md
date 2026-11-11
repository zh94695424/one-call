# PA27 customer business-number read foundation: PR fix 1

- Task: `pa27-pr38-fix1-gpt-20261111-01`.
- Work unit: `PA27-customer-business-number-read-foundation`; Gateflow gate: `fix`.
- Date: 2026-11-11, Asia/Shanghai.
- Authority: Controller-accepted High finding `PA27-PR38-CI-01` only; active PA27 Control, cumulative plan sections 8 and 10, S1 implementation, code-review-fix-1 and both PR #38 reviews are read-only context.
- Durable evidence: `D:\codex-temp\pa27-pr38-fix1-evidence-20261111-01`, mounted at `/mnt/d/codex-temp/pa27-pr38-fix1-evidence-20261111-01`.
- Disposition: accepted finding fixed in this bounded author candidate; focused tests, fresh full clean verification, exact source=CI=XML inventory, frozen-byte checks and final scope/evidence checks pass. This artifact is not Controller acceptance, hosted-CI success, re-review entry or production acceptance.

## 1. Hosted failure and non-product root cause

The accepted hosted failure is GitHub Actions run `43011410306`, job `130491648149`, `java-runtime`, at PR #38 head `dc8668ef699b87ce33d9a27cc10e6e4bb68f0e4a`. These identifiers and failure details are Controller-supplied intake, not a new remote inspection by this author. The failing method is `CustomerBusinessNumberBoundaryTests.v015AndPa25DeltasAreReadOnlyAndFrozenInputsRemainByteIdentical`; its first reported failure is V006: expected Windows/CRLF raw SHA256 `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d`, but Linux/LF checkout raw SHA256 is `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475`.

The test hashed `Files.readAllBytes` directly against plan-era Windows raw values. Git checkout newline conversion changes those bytes without changing the frozen content. This is an environment-dependent test oracle, not a migration, OpenAPI, Flyway, ACL or product-behavior defect. Plan section 10's raw values remain historical CRLF provenance and remain the local pre/post byte-preservation baseline; they are not rewritten or silently relabeled as canonical values.

## 2. Bounded implementation and changed paths

Exactly one existing repository file is modified and exactly one artifact is added:

| Change | Exact path | Raw SHA256 |
| --- | --- | --- |
| Modified | `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java` | Before: `c277dd47dde5998d9e071888b0691a36427c41bfa35cf9221b4612e4802c8e61`; after: `3f45ccc86556b8bb65d5d90746789313799ff2d992ceefc5e40e6e8aa0e98c29` |
| New | `docs/gateflow/pa27-customer-business-number-read-foundation-pr-fix-1.md` | Final raw SHA256 is recorded externally in `delivery.sha256` and `evidence-manifest.sha256`, avoiding a self-referential document hash |

The existing frozen map now pins exactly the fifteen accepted canonical-LF SHA256 values. A private byte-oriented helper maps CRLF (`0D 0A`) to one LF (`0A`) and lone CR (`0D`) to LF, then SHA256 hashes the resulting bytes. No decoding/re-encoding, trimming, whitespace folding, BOM removal or final-newline change occurs; every non-newline byte is retained.

The same existing boundary method retains all V015 read-only/grant-shape, PA25 history/oracle and frozen-content assertions. Inside its existing fifteen-input loop it additionally synthesizes LF, CRLF and lone-CR copies in memory, asserts each canonical digest equals the literal, and flips one non-newline byte in each copy to prove content mutation differs. No frozen file is written. The four ordinary test names/counts are unchanged; no test is added, renamed, disabled, skipped or parameterized. All other assertions and the three other methods remain intact. Helpers are private to this one test class; no fixture/shared-helper change is made.

## 3. Windows raw and Linux canonical provenance

The following exact pairs are independently checked against the current input content. Windows raw means the historical CRLF form, reconstructed only in memory; canonical-LF means CRLF and lone CR normalized to LF. For a Linux LF checkout, that canonical value is also its raw SHA256. Every historical Windows value is independently reproduced from the CRLF copy. OpenAPI is `api/openapi/onecall-v1.openapi.yaml`; V001-V014 are the exact versioned SQL files under `backend/src/main/resources/db/migration/`, with full filenames and all variant/mutation digests in `frozen-canonical-proof.json`.

| Input | Windows CRLF raw SHA256 | Linux canonical-LF SHA256 |
| --- | --- | --- |
| OpenAPI | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| V001 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| V002 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| V003 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| V004 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| V005 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| V006 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| V007 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| V008 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| V009 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| V010 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| V011 | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| V012 | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| V013 | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |
| V014 | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |

V015 is deliberately outside this canonical frozen map and remains byte-identical in its existing real LF form: raw SHA256 `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3`. Fresh Flyway 12.4.0 `ChecksumCalculator` execution against the actual unchanged file returns `-1316980027` (`flyway-checksum`, exit 0). No V015 hash convention, SQL, history checksum or migration byte is changed.

## 4. Validation and durable evidence

Execution uses Temurin 21.0.13+9, the pinned wrapper distribution Maven 3.9.9 and Docker 29.1.3 with the existing `postgres:16` image. The checked-in CRLF wrapper is untouched; an external LF execution copy and byte-identical wrapper properties live in evidence `wrapper/`. No test-ignore/skip flag, dependency, POM, CI or validator change is introduced. `.command`, `.log`, `.exit`, `.started` and `.finished` files retain validation execution details.

| Validation stem | Actual result |
| --- | --- |
| `frozen-preliminary` | Exit 0: independent Python byte normalization reproduces all fifteen literals from LF, CRLF and lone-CR copies, all 45 one-byte mutations differ, all historical CRLF raw values reproduce, V015 raw is exact and all four method identities are unchanged |
| `focused-boundary-module` | Exit 1 before Maven/test execution: the new external wrapper copy initially lacked its adjacent `.mvn/wrapper/maven-wrapper.properties`; retained failure, no repository change |
| `focused-boundary-module-rerun` | Exit 0 after copying the unchanged properties externally: six classes / 44 cases, zero failures/errors/skips; XML copied to `reports/focused/` before clean and identities recorded in `focused-xml-summary.json` |
| `flyway-checksum` | Exit 0: actual Flyway 12.4.0 checksum remains `-1316980027` |
| `final-clean-verify` | Exit 0: fresh `clean -Pcontainer-smoke verify`, elapsed 15:22, finished 2026-11-11 19:45:43 +08:00; Surefire 40 classes / 341 cases and Failsafe 17 classes / 272 cases, zero failures/errors/skips |
| `final-xml`, `exact-ci-inventory`, `measured-inventory` | Exit 0 each: exact source=CI=XML **40/341 + 17/272 = 57/613**, annotation-line scan **614**, exactly **seventeen includes**; all 57 fresh XML reports copied to `reports/final/`, full identities in `source-ci-inventory.json` and `xml-measured.json` |
| `exact-ci-extraction` | Exit 0: freshly extracted CI body is exact; prior-copy terminal-LF-only comparison explained below |
| `frozen-raw-preliminary`, `frozen-raw-final` | Exit 0 each: `sha256sum -c frozen.sha256` verifies unchanged raw OpenAPI, V001-V015 and Control bytes against intake |
| `frozen-post-build`, `frozen-final`, `delivery-audit` | Exit 0 each: exactly one modified test and this one new artifact, 1,802 other intake files/modes unchanged, four existing test identities and original assertions preserved; all 45 newline variants and 45 mutations pass; report provenance/hash/format checks pass |
| `manifest-check` | Exit 0: final delivered copies, both exact output hashes, validation commands/logs/exits/timestamps, scripts and XML are sealed in `evidence-manifest.sha256`; manifest excludes itself and its later verification records to avoid self-reference |

The focused selection is `CustomerBusinessNumbersWebTests,CustomerBusinessNumberQueryServiceTests,CustomerBusinessNumberCursorCodecTests,CustomerBusinessNumberBoundaryTests,ModuleBoundaryTests,CustomerApiBoundaryTests`; all four boundary cases and all nine module cases pass. The exact acceptance command is the following, executed from `backend` using only the external newline-compatible wrapper:

```bash
/mnt/d/codex-temp/pa27-pr38-fix1-evidence-20261111-01/wrapper/mvnw -B -ntp clean -Pcontainer-smoke verify
```

The unchanged `assert container smoke actually ran` Python body is extracted/dedented directly from `.github/workflows/ci.yml` as `ci-exact-inventory.py`; its exact extracted SHA256 is `4d7aa4121b10b64537f68ad913a09c0c796dc5c19f16e4442f32c1ea6fea8bef` (no terminal LF, exactly as the plan section 10 extraction). The prior evidence copy differs only by one appended terminal LF (`71d575bfdfae93b3000f67d005af3d04a1f801024bf9ddd2ce25e9e6cfd9292f`); the current copy is the exact freshly extracted body, not an edited/reduced checker. Source/CI/XML names, historical checkpoints, duplicates, zero failures/errors/skips and the seventeen POM includes are checked by that body, not merely by aggregating counts.

`baseline.json` captures all 1,803 intake non-build files and modes, including the unchanged `.git` pointer; `check-frozen.py` independently checks all non-build paths, frozen bytes, test identities and newline/mutation proofs. `frozen-canonical-proof.json` records full input filenames, raw/canonical hashes and per-variant mutation hashes. `scope-audit.json`, `delivery.sha256`, final delivery copies and the external evidence manifest retain exact final scope and hashes without self-reference. Generated `backend/target` output is disposable validation output, not delivery. No frozen checkout file is converted or rewritten for validation.

The Java test source is finalized before the focused rerun and full clean execution; its delivered hash remains `3f45ccc86556b8bb65d5d90746789313799ff2d992ceefc5e40e6e8aa0e98c29`. Only this report is completed after execution. Final raw freeze, full path/mode audit, report consistency/format checks, delivery copying and manifest verification run after report completion. No stale XML, post-run Java edit, ignored failure or hosted-rerun substitution is used.

## 5. Preserved boundaries and residuals

No production Java, SQL/V001-V015, OpenAPI, CI, POM, module wiring, fixtures/shared helpers, plan, other reports, prior reviews, Control or Git metadata/history is modified. Ordinary test inventory remains unchanged. The original raw-byte freeze policy remains enforced by independent intake-to-delivery comparison; the test's portable identity intentionally ignores only newline representation, not content changes.

The ordinary Linux `git status` cannot resolve this worktree's existing Windows `D:/...` gitdir pointer. No pointer/config/history is rewritten; scope is established by the independent full intake manifest and exact path/mode comparison, not an invented Git-clean or remote claim. Controller owns Git/commit attribution and final evidence custody.

Hosted `java-runtime` at the supplied old head remains failed; a fresh hosted run against a Controller-committed fix is not executed or claimed. Controller owns commit, fresh PR re-review and subsequent gate decisions. This author does not enter re-review, stage, commit, push, create a branch, mutate a PR, merge, approve, mark ready, request reviewers or comment externally.

Accepted plan residuals and owners are unchanged: cursor-key custody/distribution/rotation (Security/Operations); column grants rather than RLS and application tenant filtering (Security/Database); deferred qualified-DSL inversion (Backend/Architecture); representative large-owned-set sort/index EXPLAIN (Backend/Database); per-request repeatable-read is not a cross-page snapshot or linearizable revocation; production credentials, TLS, `pg_hba`, pool drain and old-session rotation (Platform/Database/Security/Operations). The opportunistic reverse-scope exact-code test note remains Backend/Test-owned and out of scope. Production/live deployment is **NOT EXECUTED / NOT CLAIMED**.
