# PA34 PR re-review2 author fix 3

Task: `pa34-operations-activation-history-pr-rereview2-fix3-gpt-20261115-01`.

Work unit: `PA34-operations-activation-history-read-foundation`. Date: 2026-11-15 (Asia/Shanghai). Gate: bounded PR re-review2 `fix3`.

This is author remediation, not independent review, finding acceptance, approval, live acceptance or production readiness. Fresh independent A/B re-review is mandatory; stop after author validation for Controller intake.

## 1. Authority and scope

Read authority: active PA34 Control checkpoint; `docs/reviews/pr-45-rereview2-20261115-182500-a4.md` and `docs/reviews/pr-45-rereview2-20261115-182500-b.md`; cumulative plan and implementation report; prior PR-fix artifacts; directly relevant source/tests and retained validation scripts. Verified HEAD is `8380f8523f87bb287bca01fbcc7a65a96ca64c25`.

Managed worktree: `C:\Users\Administrator\.codex\worktrees\pa34-next-work-unit\codex-call`. External evidence E: `/mnt/d/codex-temp/pa34-pr-fix3-evidence-20261115-01`. Existing dirty codec/test/readiness and prior fix artifacts are preserved baseline inputs, not permission to modify them. Existing private files are preserved and not copied into the validation projection. Linux Git uses the existing metadata paths read-only with `GIT_OPTIONAL_LOCKS=0`; the Windows `.git` pointer is not repaired.

Exactly six existing paths and this one new artifact are task-owned: `OperationsWebConfiguration.java`, `OperationsActivationReadConfigurationTests.java`, `OperationsActivationReadFixture.java`, `OperationsActivationReadsIT.java`, the cumulative plan, implementation report and `docs/gateflow/pa34-pr-rereview-fix-2.md`. Full paths and hashes are recorded in the final candidate table and external scope audit. No other repository write, test identity, dependency, POM/CI/manifest, migration, OpenAPI/docs14/15/17, Control/review, codec or private-helper change is authorized.

## 2. Finding dispositions

| Finding | Author disposition | Bounded repair |
| --- | --- | --- |
| `PA34-PR-REREVIEW2-A-M01` (Medium, Controller accepted) | Author-fixed; independent verification pending | ERROR registration reads only supported `spring.web.error.path`, default `/error`. Removed legacy key is never a fallback. Existing path-shape checks precede registration; fixed message is `Invalid error dispatch path`; ERROR-only/highest precedence and all REQUEST wiring are retained. |
| `PA34-PR-REREVIEW2-A-L02` (Low, Controller accepted) | Author-fixed; independent verification pending | Section5's generic old-config read-only catch-all explicitly excepts authorized row11 `OperationsWebConfiguration.java`. |
| `PA34-PR-REREVIEW2-B-L01` (Low, Controller accepted) | Author-fixed; independent verification pending | Only five mutable plan lines change: row11 permission, config exception, row11 assertion ownership, and the two named existing-test assertion cells. They record actual ERROR/REQUEST registration, effective-key/default/legacy non-effect, provider/padding classification, fixed-message/null-cause and post-static-scope restoration assertions. |

Prior `PA34-PR-REREVIEW-01..03` remain fixed and `04` remains rejected-with-reason. Earlier PR decisions are not reopened; neither codec nor codec tests are modified.

The existing configuration identity now exercises default, Spring-only custom, different Spring+legacy values, valid legacy-only and malformed legacy-only settings, then all twelve malformed effective values with a valid legacy value that must not rescue them. Actual servlet mappings prove ERROR versus unchanged REQUEST dispatchers and precedence. Each malformed effective path produces the exact neutral root-cause message and no ERROR-registration singleton. No new test method/class, argument, parameterized/dynamic identity or CI identity is introduced.

The container fixture sets only `spring.web.error.path`; the existing HTTP identity compares the environment's effective value to `WebProperties.Error#getPath()` and proves exact activation TRACE404 with that custom path. The same identity starts and closes a separate default-path web context, requiring activation TRACE404 and the original three sibling TRACE500/INTERNAL responses there. No removed-key setting is used in either context, and sibling concealment is not generalized. The default context is deliberate: unchanged customer-controller mapping cannot provide the old dual-key custom-path sibling500 with only the supported key.

Boot4.1.2 dependency metadata is independently inspected under E in `boot-error-path-metadata.log`: the Spring key has default `/error` and no deprecation; the removed legacy key has deprecation level `error` and replacement `spring.web.error.path`.

## 3. Candidate and frozen identities

Baseline plan: 85,788 bytes /574 lines, SHA256 `1D99885939BCC0D6536334366EE6108B43B0B4506C724CBBA9CE68F17647B4D4`. `permission-proof.py` verifies that only original plan lines222/237/251/280/313 change and all other plan bytes remain unchanged. Section5 remains contiguous01–24 =19 new/5 existing. All88 existing method identities remain authoritative.

Section8's body remains4,419 bytes /SHA256 `BAB64BF545FA209C705056BE2E896C88E21170D6CB90ED0F12A6ECDF0878E135`; the complete block including `## 8.` remains4,424 bytes /SHA256 `D114E71C79BA44F56F76E0DF9767D926E5E9A250E71F92B6C0EBBC396BA23E86`. Both are byte-identical to baseline; exactly21 hash/path rows exist in the entire plan. All21 frozen raw/LF inputs and exact V001–V017/no V018 remain required checks.

The six existing final candidate paths are pinned below. This artifact's own final hash is recorded externally in `final-candidate-manifest.json` and `final-scope.log`, avoiding a self-referential digest.

| Path | SHA256 (raw worktree bytes) | Bytes / lines |
| --- | --- | --- |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsWebConfiguration.java` | `86A268CE54557683635009CE43B4229328BC2E5449AA6708F0018A06990BD201` | 2,513 /42 |
| `backend/src/test/java/com/onecall/archtests/OperationsActivationReadConfigurationTests.java` | `BD79649FC35B4411A05B44E09016B8FBCFD1706586154594E653037C6EABCD4D` | 16,423 /155 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadFixture.java` | `D14B5DCE392390FDD4CDC8393584BF588BD43373E05C33D959D8FDC82FA0A793` | 13,952 /121 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java` | `9D99B7EE4FBBB35E1401D02F416080CEDCEEED28D05F44502D46F325CD4CD098` | 36,664 /274 |
| `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md` | `1548929EA7305D14ADE65D9CD3DD4416F4DFE970B69DAC75560C3E61746AA0B7` | 87,010 /574 |
| `docs/gateflow/pa34-operations-activation-history-read-foundation-implementation.md` | `BD66CB792EF5DC63E4D7FA652D97604ADEA9E83A0CEC9F88D2D330E287DFCEAB` | 70,505 /511 |

The final four production/test source hashes exactly equal `validated-source-hashes.json`, captured before full validation and rechecked afterward. The plan is unchanged throughout validation; only this artifact and the report are finalized after results. `source-proof-before.log`, `source-proof-after.log`, `validation-timeline.log` and the final scope manifest establish that provenance.

## 4. Commands and validation

All Maven runs are serial. The inherited `BASH_ENV` shim maps the CRLF checkout's `./mvnw` command to native Maven3.9.9/JDK21 without editing the wrapper or POM. Evidence scripts and all generated logs/projections/XML stay under E or existing ignored Maven `target` output. Attempts are retained; environment-only failures are excluded from accepted counts, never relabeled as passing tests.

Retained non-authoritative attempt: initial focused units passed60/60; initial container run failed one of28 tests because the Spring-only custom error path leaves the unchanged legacy customer controller unmapped and the sibling response became404 instead of the former dual-key500. This is a real fixture-assumption/residual discovery, not an environment failure. `initial-attempt/` retains the complete logs, exit codes and Failsafe XML. Only the existing HTTP test identity is corrected to locate its sibling500 observation in an explicit default-path context; custom-path activation404 still runs against the PostgreSQL fixture. All final validation is rerun serially on the corrected candidate.

E/`retained-attempt-notes.log` also preserves the initial raw Linux Git pointer diagnostic, the external source-proof script's newline-marker adaptation and the informational version query's unconfigured Maven launcher failure. These are not passing tests or omitted failures. The configured toolchain query and corrected LF-projected source comparison pass without changing repository metadata, candidate source or the required Maven commands.

The validation driver runs the exact six unit classes and two PostgreSQL16 IT classes, then `./mvnw -B -ntp -Pcontainer-smoke clean verify`, followed by the three exact checked-in CI bodies and exact source=CI=POM=XML inventory. The separate LF projection uses HEAD plus the current authorized cumulative candidate overlays, not private files, and runs the two existing frozen guard identities followed by the exact accepted section9.3 A1–A5 command block.

All required author validation completes successfully on the final source candidate. The first failing container attempt is retained separately and is not included in any passing total.

| Validation | Authoritative result | Evidence under E |
| --- | --- | --- |
| Focused six no-argument unit classes | 60/60, zero failures/errors/skips, exit0 | `focused-unit.log`, `.exit`, `focused-surefire-xml/` |
| Focused two PostgreSQL16 Failsafe classes | 28/28, zero failures/errors/skips, exit0 | `focused-container.log`, `.exit`, `focused-failsafe-xml/` |
| Full clean PostgreSQL16 verify | 76/704 Surefire +31/504 Failsafe =107/1208, zero failures/errors/skips, BUILD SUCCESS, exit0; Maven31:44 | `full-clean-verify.log`, `.exit`, `full-surefire-xml/`, `full-failsafe-xml/`, `full-xml-summary.json` |
| Exact plan=source=CI=POM=XML | Exactly88 PA34 identities=60+28; source1208, token1209, POM31; full XML names equal all previous accepted1208 | `exact-inventory.log`, `.exit`, `accepted-identities.json`, `exact-ci-bodies.log` |
| Exact three CI bodies | All exit0; default startup,120 web/default+custom-error probes and PA11 firewall116 adversarial probes pass | `exact-ci-bodies.log`, `.exit`, `boot.log`, `web-boot.log`, `web-boot-custom-error.log`, `jar-index.txt`, `pa11-runtime-dependencies.txt` |
| Additional supported-key-only packaged probes | Six activation GET/TRACE/POST/HEAD probes pass with only the Spring error key; JVM closed; no sibling-concealment claim | `supported-key-jar-probes.log`, `.exit`, `spring-only-web-boot.log` |
| Frozen raw/LF and migration inventory | All21 raw/LF hashes; exact V001–V017/no V018; selected LF architecture1 +PostgreSQL1, zero failures/errors/skips, exit0 | `lf-validation-timeline.log`, `lf-frozen.log`, `.exit`, `lf-surefire-xml/`, `lf-failsafe-xml/` |
| Exact accepted A1–A5 command in LF projection | Every stage passes, exit0; runtime evidence remains NOT_RUN and production accepted=false | `accepted-a1-a5-command.sh`, `contracts-a1-a5.log`, `.exit`, `linux-lf/` |
| Mutable plan permission/assertion proof | Only five permitted lines change; contiguous24=19 new/5 existing; all21 section8 rows and both raw block hashes byte-identical | `permission-proof.log`, `permission-only-plan.diff` |
| Source, final scope and hygiene | Exact seven-path fix3 delta; unchanged no-argument identities/codec/prior artifacts/private files; HEAD/pointer/index preserved; no staged path; strict UTF-8/final-LF/conflict/trailing-whitespace and diff checks pass | `source-proof-before.log`, `source-proof-after.log`, `final-scope.log`, `final-candidate-manifest.json` |

Full clean verify runs from 2026-11-15T19:52:27+08:00 to20:24:13+08:00. Exact CI bodies/inventory complete before the separate LF JVM/container work starts at20:24:44+08:00; LF/A1–A5 completes before the additional packaged probes begin at20:29:00+08:00. All serial validations finish at20:29:05+08:00 (`validation-timeline.log`, `finish-validation-timeline.log`).

Packaged JAR SHA256: `F9B46B3330BA3D87197A00FEE125120A1968AE1810AD8A6B212A2112CDD3159A`. Firewall counts remain baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1, deltas35/0 and31/0. No dependency or CI body is changed.

Exact commands (backend working directory unless stated otherwise):

```bash
./mvnw -B -ntp -Dtest=OperationsActivationReadConfigurationTests,OperationsActivationReadWebTests,OperationsActivationReadBoundaryTests,OperationsActivationCursorCodecTests,OperationsActivationReadRequestTests,OperationsActivationReadServiceTests test
./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsActivationReadConfigurationTests,OperationsActivationReadWebTests,OperationsActivationReadBoundaryTests,OperationsActivationCursorCodecTests,OperationsActivationReadRequestTests,OperationsActivationReadServiceTests -Dit.test=OperationsActivationReadRoleLeastPrivilegeIT,OperationsActivationReadsIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

`validate.py` then extracts and executes the unchanged workflow bodies `assert container smoke actually ran`, `default and opt-in web jar smokes` and `production jar dependency firewall` from the current `.github/workflows/ci.yml` using the retained `ci-validation.py` extractor; each body's SHA256 and exit result are logged in `exact-ci-bodies.log`. The inherited inventory script checks exact plan/source/CI/POM/XML sets, every historical subtraction and equality with the previous accepted full1208 names. Both scripts execute against the current candidate; prior run results are not substituted.

`finish-validation.sh` waits for all main validation and inventory to complete successfully before starting any further JVM/container work. In the external LF projection it runs:

```bash
./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsActivationReadBoundaryTests#frozenInputsAndSeventeenMigrationInventoryRemainByteIdentical -Dit.test=OperationsActivationReadsIT#postgres16UsesExactlyFrozenSeventeenMigrations verify
```

The exact accepted section9.3 A1–A5 shell block is copied unchanged to E/`accepted-a1-a5-command.sh` and executed in that LF projection; its SHA256 is `E3FBB73E2F1419738069028F1BBBCC0DEC63BAB1347B8EFEF50C2E0352E39BE0`. An additional external `supported-key-jar-probes.py` starts the final packaged JAR with only `--spring.web.error.path=/internal/pa34-spring-only-error`, verifies six bounded activation GET/TRACE/POST/HEAD probes and closes the JVM. It makes no sibling-concealment claim and does not alter CI. Static `permission-proof.py`, `source-proof.py` and `scope.py` record byte preservation, unchanged no-argument identities, candidate provenance and exact seven-path scope/hygiene.

## 5. Residuals and stop boundary

- `CustomerApiErrorController` remains unchanged and still maps `${server.error.path:/error}`. Broader customer-controller/legacy-key/deployment coordination remains Platform/Backend-owned outside this work unit. The observed Spring-only custom-path sibling404 is not generalized TRACE concealment by the Operations filter and is not claimed fixed; sibling500 evidence is explicitly default-path only. Passing historical dual-key packaged probes does not make the removed key effective for Boot or for the corrected filter.
- Security/Backend owns the previously recorded PA33 cross-codec divergence and production JCE-provider semantics; no codec work is reopened here. Security/Operations IAM/DBA retains production verifier/session/MFA/provisioning, runtime credentials and shared-ring custody/rotation.
- Backend/DBA/Operations retains global-order capacity/timeouts and corrupt-fact remediation; Backend/Security retains snapshot-relative revocation and fresh continuation snapshots. No schema/grant/index/V018 or production-readiness claim is introduced.
- Hosted passing checks belong to the earlier reviewed remote head, not these local fixed bytes. This task makes no remote query or mutation and claims no hosted validation of this candidate.
- No stage, commit, push, PR update, merge, approval, mark-ready, reviewer request, external comment or later-gate entry is performed. Controller owns intake and fresh independent A/B re-review. Production readiness is NOT CLAIMED.
