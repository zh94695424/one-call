# PA34 PR re-review author fix 2

Task: `pa34-operations-activation-history-pr-fix2-gpt-20261115-01`.

Work unit: `PA34-operations-activation-history-read-foundation`. Date: 2026-11-15 (Asia/Shanghai).

This is bounded author remediation at `fix2`, not independent re-review, finding acceptance, approval, live acceptance or production readiness. Stop for Controller and fresh independent A/B PR re-review.

## 1. Authority and checkpoint

Read authority: active PA34 Control; independent A/B re-review artifacts `docs/reviews/pr-45-rereview-20261115-170900.md` and `docs/reviews/pr-45-rereview-20261115-172042.md`; prior `docs/gateflow/pa34-pr-review-fix-1.md`; and all six current candidate files. Verified local HEAD: `5934d6665757f791e725d7c89df5982ec056d47a`. Controller-supplied remote reviewed head remains `a12d9bae70a864af8a72b2cda657a33c4674c069`; no remote query or mutation is performed.

Managed worktree: `C:\Users\Administrator\.codex\worktrees\pa34-next-work-unit\codex-call`. Evidence root E: `D:\codex-temp\pa34-pr-fix2-evidence-20261115-01` (WSL `/mnt/d/codex-temp/pa34-pr-fix2-evidence-20261115-01`). Scripts, generated projections, XML and logs are external under E. Existing dirty candidate/readiness/prior-fix/private files are baseline inputs, not changes authored by this round. The Windows `.git` pointer is preserved; Linux read-only Git uses explicit existing metadata paths and `GIT_OPTIONAL_LOCKS=0`. No Git repair, staging or index write is performed.

Provider/task metadata: fresh Codex / GPT dispatch requests `gpt-6.1-sol` / `high`; this is supplied dispatch metadata, not independently measured model identity. No subagent, delegation or peer review is used. Runtime: WSL2 Linux amd64 `6.18.33.2-microsoft-standard-WSL2`, Eclipse Adoptium Java21.0.13, Maven3.9.9, Docker29.1.3, Node22.23.4; exact emitted values are in `versions.log`. The unchanged Maven shim is `/mnt/d/codex-temp/pa34-implementation-evidence-20261115-01/mvnw-shim.sh`, SHA256 `a69c64cb2c46e36bc7abb9a7e4138260bbf7ef74f8c1073ca95549574800283c`; unchanged `backend/mvnw` SHA256 `825cdc62abd459da5efa6eeb0218f329c0adca3f7d4f7560f6ba2b43b9deb928`.

## 2. Finding dispositions and exact changes

| Finding | Controller disposition | Author action |
| --- | --- | --- |
| `PA34-PR-REREVIEW-01` (Low, docs) | accepted for bounded fix | Change only the production-file-set sentence at plan line251 from `new rows01–08 and existing filter rows09–10` to `new rows01–08 and existing production rows09–11`. No other plan byte changes; section5 remains exactly24 rows=19 new/5 existing. |
| `PA34-PR-REREVIEW-02` (Low, codec) | accepted for bounded fix | Decode catches `BadPaddingException` (including its `AEADBadTagException` subclass) with existing runtime/caller failures as sanitized `CURSOR_INVALID`/400. Remaining checked JCE failures remain sanitized `SERVICE_UNAVAILABLE`/503. Encode's checked-security catch is unchanged and remains503, including the simulated padding failure. |
| `PA34-PR-REREVIEW-03` (Low, test) | accepted for bounded fix | Both scoped JDK static mocks now use `CALLS_REAL_METHODS`; targeted Cipher/SHA-256 `NoSuchAlgorithmException` stubs and the post-scope round trip remain. A mocked Cipher's `doFinal(byte[])` throws plain `BadPaddingException` with a detail canary inside the existing Cipher scope; decode must return400 and encode503. |
| `PA34-PR-REREVIEW-04` (Low, line endings) | rejected-with-reason; no repair | Actual formal candidate bytes are LF; normalized Git diff is surgical and hosted Linux uses LF. Rewriting to CRLF would create representation churn, not a correctness fix. Raw hashes describe actual worktree bytes. Do not relabel all historical evidence or modify the frozen dual-EOL guards/constants. |

The existing identity `OperationsActivationCursorCodecTests.missingInvalidAndDuplicateKeyConfigurationIsUnavailable` is extended, not replaced or supplemented with a new identity. Prior configuration, malformed-token400, Cipher/SHA-256 provider503, encode503, fixed safe-message/null-cause, tamper/wrong-key/purpose/authenticated-invalid-anchor and restoration checks remain. No production helper or provider registry is changed; no new method, class, dependency, endpoint, field, grant or fallback is added.

## 3. Exact plan and frozen identities

| Measurement | Before fix2 | After fix2 |
| --- | --- | --- |
| Raw SHA256 | `bfd33265dfd0df5f4df4e72017266a9ce6a28055877449d5372cfea6f7ae4c62` | `1d99885939bcc0d6536334366ee6108b43b0b4506c724cbba9ce68f17647b4d4` |
| Bytes / lines | 85,784 /574 | 85,788 /574 |
| Section5 rows | 24=19 new/5 existing | 24=19 new/5 existing |

Current plan locations: section5 line197, row11 line222, arithmetic line237, changed permission sentence line251, section8 line418. `permission-proof.py` proves exact byte equality after replacing that single old sentence with the new sentence, contiguous01-24 rows, 19/5 arithmetic, unchanged codec test identities and unchanged frozen boundary/IT tests and PA33 codec. `permission-only-plan.diff` is the exact one-line diff. Older plan hashes in unchanged prior artifacts and earlier report sections remain historical.

Both section8 definitions remain identical before/after: body excluding the five-byte `## 8.` marker is4,419 bytes, SHA256 `bab64bf545fa209c705056be2e896c88e21170d6cb90ed0f12a6ecdf0878e135`; complete block including that marker and ending before `## 9.` is4,424 bytes, SHA256 `d114e71c79ba44f56f76e0df9767d926e5e9a250e71f92b6c0ebbc396ba23e86`. Both contain exactly the same21 unchanged hash/path rows. No frozen source expectation, OpenAPI/docs14/15/17, migration, POM or CI change is authorized.

## 4. Validation evidence

Validation completed serially under E; no LF/container job ran concurrently with the full clean verification. New runtime evidence, rather than prior-round passes, is authoritative for this candidate.

| Validation | Result | Evidence |
| --- | --- | --- |
| Focused six unit classes | Surefire exactly `60/60`, failures/errors/skips `0/0/0`, exit0 | `focused-unit.log`, `focused-unit.exit`, `focused-surefire-xml/` |
| Focused two PostgreSQL16 IT classes | Failsafe exactly `28/28`, failures/errors/skips `0/0/0`, exit0; PostgreSQL `16.15` | `focused-container.log`, `focused-container.exit`, `focused-failsafe-xml/` |
| Serial full clean PostgreSQL16 verify | Surefire exactly `704/704` across76 suites and Failsafe exactly `504/504` across31 suites; failures/errors/skips `0/0/0`; BUILD SUCCESS, exit0; total31:17 | `full-clean-verify.log`, `full-clean-verify.exit`, `full-surefire-xml/`, `full-failsafe-xml/` |
| Exact source=CI=POM=XML | 88 accepted identities (`60+28`), source1208, `@Test` token1209, POM includes31; focused/full XML exact and full1208 names unchanged | `exact-inventory.log`, `exact-ci-bodies.log`, `accepted-identities.json`, `full-xml-summary.json` |
| Frozen contracts and EOL guard | All21 raw/LF hashes pass; exact V001–V017 and no V018; frozen focused probe exit0; section8 exactly21 unchanged rows | `lf-validation-timeline.log`, `lf-validation-final.exit`, `linux-lf/`, `lf-surefire-xml/`, `lf-failsafe-xml/` |
| CI/JAR/firewall | Exact three CI bodies exit0; packaged JAR and web/custom-error probes pass; PA11 firewall passes `116` adversarial probes, baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1 | `exact-ci-bodies.log`, `boot.log`, `web-boot.log`, `web-boot-custom-error.log`, `jar-index.txt`, `pa11-runtime-dependencies.txt` |
| Accepted A1–A5 | Exact accepted command block exit0; OpenAPI/IVR/security/capacity/acceptance checks pass; runtime evidence remains `NOT_RUN`, production accepted=false | `contracts-a1-a5.log`, `accepted-a1-a5-command.sh` |
| Scope/hygiene | Exact five-path fix2 scope; no staged path; HEAD/pointer preserved; `git diff --check` clean; section8 and permission proof pass | `final-scope.log`, `permission-proof.log`, `permission-only-plan.diff`, `git-diff-check.log`, `index-observation.log` |

Retained non-authoritative attempts are explicitly separated: the manually selected early unit command passed `26/26` but omitted accepted classes and is not the required focused result; the derived exact six-class rerun is authoritative. The prior fix1 concurrent full run's eight environment `Cannot allocate memory` errors remain in its prior evidence root and were not relabeled; fix2 full validation was serial. The adapted permission script initially rejected the authorized line251 replacement due its old allowed-region boundary; the corrected proof passed and the initial assertion is retained in `retained-attempt-notes.log`.

## 5. Residual ownership and stop

- Backend/Security owns separately authorized PA33 subject-codec checked-JCE attribution work; this fix does not modify PA33 or generalize cursor infrastructure.
- Backend test/gate maintainers own any later relocation of the plan-based frozen manifest; the accepted21-row oracle and dual-EOL constants remain unchanged.
- Platform/Backend owns non-default error-path coordination and root/trailing-slash policy; deferred B-F04 TRACE concealment remains later route-boundary/contract work.
- Backend/DBA and Operations own global-order capacity/timeouts and corrupt-fact remediation; Backend/Security owns snapshot-relative revocation and fresh continuation snapshots. No new migration/index/write/grant is authorized.
- Security/Operations IAM/DBA owns production verifier/session/MFA/provisioning, runtime read credentials and shared-ring custody/rotation. Existing development-tool vulnerability reports do not authorize dependency changes here.
- Controller owns intake, independent A/B PR re-review and all remote/gate decisions. Local evidence is not hosted-CI or live acceptance; production readiness is NOT CLAIMED.

Exactly five paths may differ from the current candidate baseline: codec, existing codec test, one plan sentence, implementation report and this new artifact. Prior PR-fix artifact, readiness, Control, prior reviews, POM/CI/migrations, other code/tests and private helpers are preserved. No stage, commit, push, PR update, reviewer request, approval, mark-ready action, merge, external comment or next-gate entry is performed.
