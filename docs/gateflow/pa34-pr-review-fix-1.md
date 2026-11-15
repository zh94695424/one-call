# PA34 PR-review fix 1

Task: `pa34-operations-activation-history-pr-fix1-gpt-20261115-01`.

Work unit: `PA34-operations-activation-history-read-foundation`.

Date: 2026-11-15 (Asia/Shanghai).

Provider/task metadata: fresh Codex / GPT author-fix dispatch; requested model/profile `gpt-6.1-sol` / `high`. This records dispatch metadata, not an independently measured provider model identity. No subagent or peer review is used.

Gate: bounded author fix after PR #45 review, not independent re-review, finding acceptance, approval, live acceptance or production readiness. Stop for Controller.

## 1. Authority and checkpoint

Authority is the active PA34 Control, accepted cumulative plan, PR review A `docs/reviews/pr-45-review-20261115-142238.md`, PR review B2 `docs/reviews/pr-45-review-20261115-152516.md`, existing implementation/fix1/fix2/readiness records and this bounded dispatch. Local checkpoint HEAD is verified as `2e8ad0cde168d7c7d74c2963e02b58d19d010b6e`; reviewed remote head is Controller-supplied `a12d9bae70a864af8a72b2cda657a33c4674c069`. These are distinct checkpoints, not interchangeable validation subjects.

Managed worktree: `C:\Users\Administrator\.codex\worktrees\pa34-next-work-unit\codex-call` (WSL `/mnt/c/Users/Administrator/.codex/worktrees/pa34-next-work-unit/codex-call`). The Windows `.git` pointer is preserved. Read-only Git uses the existing `/mnt/d/codex-call/.git/worktrees/codex-call18` metadata, explicit `GIT_DIR`, `GIT_COMMON_DIR`, `GIT_WORK_TREE` and `GIT_OPTIONAL_LOCKS=0`; no Git-state repair is performed.

New evidence root E: `D:\codex-temp\pa34-pr-fix1-evidence-20261115-01` (WSL `/mnt/d/codex-temp/pa34-pr-fix1-evidence-20261115-01`). All generated scripts, logs, XML and the authorized LF projection are external under E. Maven uses only the unchanged `/mnt/d/codex-temp/pa34-implementation-evidence-20261115-01/mvnw-shim.sh`; `backend/mvnw` is unchanged. Prior evidence is used only for immutable identity comparison and read-only validation-script reuse, not as a substitute for current runtime runs.

## 2. Exact finding dispositions

| Finding | Controller disposition | Author action / boundary |
| --- | --- | --- |
| `PA34-PR-REV-01` (Low) | accepted for bounded fix | Decode catches `AEADBadTagException` before remaining `GeneralSecurityException`: authentication/tag and existing malformed/runtime-anchor failures remain `CURSOR_INVALID`/400; JCE algorithm/provider checked failures become sanitized `SERVICE_UNAVAILABLE`/503, matching encode. No exception cause/message is exposed. |
| `PA34-PR-REV-02` (Low) | accepted, documentation only | `PA34-PLAN-CORRECTION-02` reconciles mutable permission prose/table with accepted Control `PA34-IMPL-CORRECTION-01`: explicitly authorize `OperationsWebConfiguration.java`, exactly24 rows=19 new/5 existing, coherent row numbers and helper-row references. No implementation permission is newly invented. Section8 and its21 path/hash rows remain byte-for-byte unchanged. |
| `PA34-PR-REV-03` (Low) | rejected-with-reason; do not repair | Moving the plan-based21-row oracle to a test-owned resource contradicts accepted `PA34-CODE-REV-01`, which deliberately retains exact21 plan rows/constants while removing only the plan's own byte self-pin. Permission-only edits do not affect the retained manifest. Neither frozen test nor its21-row expectation changes; oracle relocation is later maintainability work, not this fix. |
| `PA34-PR-REV-04` (Low) | accepted, documentation only | Readiness is explicitly a dated 2026-11-15 pre-push record; a separate dated annotation cites Controller's later push and OPEN Draft PR #45 from `codex/pa34-next-work-unit` against `main`. Historical verification is not relabeled as knowledge of future remote state or hosted validation. |

The existing `OperationsActivationCursorCodecTests.missingInvalidAndDuplicateKeyConfigurationIsUnavailable` identity gains scoped static Mockito seams for both `Cipher.getInstance("AES/GCM/NoPadding")` and `MessageDigest.getInstance("SHA-256")`, each throwing `NoSuchAlgorithmException` with a detail canary. Both decode and encode must return the exact sanitized503 failure. A malformed token remains400 even while the Cipher seam is unavailable. Existing failure assertions now require the fixed safe message and null cause, and a post-scope round trip proves mocks are closed. No provider registry is mutated; no new test method/class, helper, dependency, fallback, endpoint, field, migration or grant is added. Existing tampered-tag, wrong-key/purpose and authenticated-malformed-anchor matrices are retained.

## 3. PA34-PLAN-CORRECTION-02 identity and immutable-manifest proof

This is a permission-only reconciliation under accepted `PA34-IMPL-CORRECTION-01`, not a new plan goal or a change to test design.

| Measurement | Before | After |
| --- | --- | --- |
| Raw SHA256 | `6afe1d7a6f14f69d628694b3bd9ef9f088f08a1e32a0378161a8449e0922ee69` | `bfd33265dfd0df5f4df4e72017266a9ce6a28055877449d5372cfea6f7ae4c62` |
| Bytes / lines | 84,995 /573 | 85,784 /574 |
| S1 permission rows | 23=19 new/4 existing | 24=19 new/5 existing |

Current plan references: section5 line197, `OperationsWebConfiguration.java` row11 at line222, arithmetic line237, section8 line418. The implementation report's PR-fix section carries this new plan identity for Controller handoff; Control itself is not edited. Earlier hashes/line references remain historical, not current pins.

The section8 body excluding the '## 8.' heading marker is exactly4,419 bytes before and after, SHA256 `BAB64BF545FA209C705056BE2E896C88E21170D6CB90ED0F12A6ECDF0878E135`; the complete section8 block including the '## 8.' heading marker is exactly4,424 bytes before and after, SHA256 `D114E71C79BA44F56F76E0DF9767D926E5E9A250E71F92B6C0EBBC396BA23E86`. Both definitions contain exactly the same21 `^<64hex>  path$` rows retaining all original paths/hashes. `permission-proof.py` compares all before/after diff opcodes against only mutable permission prose/table/reference locations, checks contiguous01-24 numbering and19/5 arithmetic, confirms both frozen test files and the PA33 subject codec unchanged, and writes `permission-only-plan.diff`. All other plan bytes, including sections3/4's runtime design and all eight test manifests, are unchanged apart from the two helper-row number references.

`PA34-PR-FIX-EVIDENCE-01`: `section8-before.bin` and `scope.py` use `split(b'## 8.')[1].split(b'## 9.')[0]` for the body; `permission-proof.py` measures from `## 8.` through just before `## 9.` for the complete block. The five-byte heading marker alone explains the different lengths/hashes; both the body and complete block remain byte-for-byte unchanged before/after. Clarification audit evidence: `D:\codex-temp\pa34-pr-fix1-docclarify-evidence-20261115-01`.

## 4. Validation results

All required validation completed successfully under E. The first full clean attempt ended with eight environment `Cannot allocate memory` errors during Spring classpath scanning; it is retained as failed-attempt evidence and is not counted. The unchanged command was rerun cleanly and is the authoritative full result.

| Validation | Result | Evidence |
| --- | --- | --- |
| Focused six unit classes | Exactly60/60 Surefire; zero failures/errors/skips; exit0 | `focused-unit.log`, `focused-unit.exit`, `focused-surefire-xml/` |
| Focused two PostgreSQL16 IT classes | Exactly28/28 Failsafe; zero failures/errors/skips; exit0 | `focused-container.log`, `focused-container.exit`, `focused-failsafe-xml/` |
| Full clean PostgreSQL16 container smoke | Exactly704/704 Surefire +504/504 Failsafe; zero failures/errors/skips; BUILD SUCCESS; exit0 | `full-clean-verify-rerun.log`, `full-clean-verify-rerun.exit`, `full-surefire-xml/`, `full-failsafe-xml/` |
| Exact source=CI=POM=XML inventory | 88 PA34 identities; 60+28; full1208; token1209; POM31; exact focused/full XML sets | `final-inventory.log`, `ci-validation-run.log`, `accepted-identities.json`, `full-xml-summary.json` |
| Frozen CRLF worktree and authorized LF projection | All21 raw/LF hashes; exact V001-V017; no V018; selected guards exit0 | `scope.py`, `lf-validation.py`, `lf-validation-timeline.log`, `lf-frozen/`, `linux-lf/` |
| Exact packaged JAR, firewall and CI bodies | Three exact CI bodies exit0; packaged smokes and accepted firewall pass; 116 adversarial probes | `ci-validation-run.log`, `boot.log`, `web-boot.log`, `web-boot-custom-error.log`, `jar-index.txt`, `pa11-runtime-dependencies.txt` |
| Accepted A1-A5 commands | Every command exit0 in authorized LF projection; runtime evidence remains NOT_RUN and production accepted=false | `contracts-a1-a5.log`, `accepted-a1-a5-command.sh` |
| Permission/scope/hygiene | Plan-only mutable permission delta; section8 exactly21 unchanged rows; exact six allowed paths; HEAD/pointer preserved; no staged path; `git diff --check` clean | `permission-proof.log`, `permission-only-plan.diff`, `pre-final-scope.log` |

The exact CI body inventory was rerun after the successful full clean result. The first failed full attempt remains explicitly classified as an environment resource failure, not a test failure or skip.

## 5. Residuals and owner classification

- Deliberate PA33 divergence: `OperationsSubjectCursorCodec` retains its prior checked-JCE-to400 decode behavior. Backend/Security owns a separately authorized PA33 correction and regression review; this fix must not edit PA33 or generalize cursor infrastructure.
- Plan-oracle coupling: rejected as a repair requirement in this gate; Backend test/gate maintainers own any later separately authorized manifest relocation. The accepted21-row design and raw/LF constants remain authoritative.
- Platform/Backend retains non-default `server.error.path` / `spring.web.error.path` coordination and root/trailing-slash policy; B-F04 broader TRACE concealment remains deferred to later Operations route-boundary/contract work.
- Backend/DBA + Operations retains global-order capacity/timeouts and corrupt-fact remediation; no V018, index, row dropping, write or grant is authorized. Backend/Security retains snapshot-relative revocation and fresh continuation snapshots.
- Security/Operations IAM/DBA retains production verifier/session/MFA/provisioning, runtime read credentials and shared-ring custody/rotation. Production readiness is NOT CLAIMED.
- Controller owns gate intake and any subsequent independent A/B re-review. Controller records both hosted checks passing at the reviewed remote head; this author task neither queries nor mutates remote state and does not relabel older pending-check review snapshots.

## 6. Scope and Controller stop

Exactly five existing paths plus this one new artifact are authorized: activation codec, its existing codec test class, mutable plan permission prose/table, readiness, implementation report and `docs/gateflow/pa34-pr-review-fix-1.md`. No Control/review/frozen-contract/migration/POM/CI/other production or test/config/package-info/private-helper edit is authorized. Final content snapshot/hygiene and empty-staging results will be recorded after validation and documentation.

No stage, commit, branch creation, push, PR update, reviewer request, approval, mark-ready action, merge, external comment or re-review entry is performed. This is author remediation only. Stop for Controller.
