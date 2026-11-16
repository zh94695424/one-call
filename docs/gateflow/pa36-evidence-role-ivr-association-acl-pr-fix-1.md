# PA36 PR #46 bounded fix 1

## Identity and gate

- Task: `pa36-pr46-fix1-gpt61sol-20261116-01`; work unit: `PA36-evidence-role-ivr-association-acl`.
- Runtime/provider: Codex CLI / OpenAI GPT; requested routing label `gpt61sol` (the precise serving model is not independently exposed to this session). Environment: WSL/Linux Bash, Temurin Java 21.0.13, Maven 3.9.9, Asia/Shanghai. Started 2026-11-16; command logs carry exact local timestamps.
- Current gate: **PR review -> fix**. Authority: the PA36 plan, unchanged project-execution-control document, and this bounded task. Both submitted peer review artifacts were read; neither reviewer's external runner files were read.
- Entry HEAD: `d5badecf74f458bd54de2987c84069d6b2a4219e`. `author_code_complete=true` means the authorized code/document edits are complete; it does **not** mean validation passed or any gate was accepted.

## Exact changed paths

1. `.github/workflows/ci.yml`
2. `docs/17-production-security-design-spec.md`
3. `docs/operations/pa25-production-database-identity-provisioning-runbook.md`
4. `backend/README.md`
5. `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
6. `backend/src/test/java/com/onecall/archtests/container/EvidenceRoleIvrAssociationAclIT.java`
7. `docs/gateflow/pa36-evidence-role-ivr-association-acl-pr-fix-1.md` (this new artifact)

## Accepted corrections

- `PR46-B-01`: changed only the `java-runtime` timeout from 30 to 45 minutes. Byte comparison against the entry copy proves exactly that one replacement; commands, filters, jobs, permissions and the separate 25-minute timeout are unchanged. Controller-supplied exact-head evidence: run `43894994160` on `d5badec` reached the unchanged 30-minute cap, cancelled `build, module checks, container smoke`, and skipped the three following verification steps. Increasing the budget is not evidence that hosted CI now passes.
- `PR46-B-02`: the three PA36 current sections now identify repository-canonical LF SHA256, standard CRLF-materialized checkout raw SHA256 and the signed Flyway checksum precisely, and explicitly state that checkout bytes depend on representation. Only the current paragraph changed; historical prose was not corrected retroactively. The permitted boundary suite now requires the exact terminology, both hashes, checksum and representation caveat, with the recalculated docs17 full-LF digest. Historical-tail pins remain unchanged.
- `PR46-C-01`: replaced the success-tolerant forbidden-delegation try/catch with existing `denied(connection, sql)`. That helper requires a thrown SQLException with SQLSTATE `42501` and restores its savepoint in `finally`; unexpected successful delegation now fails. The method identity and all test identities/counts are unchanged.
- `PR46-B-03`: **controller bookkeeping, not a code change, closes this finding**. Controller-verified live title: `[PA36] Add evidence-role IVR association ACL prerequisite`. Controller-verified body summary: V018-only scope, frozen predecessors, unchanged application/OpenAPI, exact ACL/topology facts and validation, the docs14 deferred owner, and explicit **draft review artifact / not production readiness**, with hosted CI and PA35 behavior owner-routed. This agent did not fetch, mutate or comment on GitHub.

## Integrity and non-regression

- Independently recalculated V018 repository-canonical LF SHA256: `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`. This local LF-materialized file's raw digest happens to be the same; no universal checkout-byte claim is made.
- Independently materialized standard CRLF bytes in memory: raw SHA256 `ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c`. Signed Flyway CRC32 over UTF-8 lines without line separators remains **1606293398**.
- All 17 predecessor raw SHA256 values match the plan's current-state table; all 17 LF digests match the unchanged `FROZEN` pins; all 17 signed Flyway checksums match unchanged history literals. The audit also compares every tracked file's raw SHA256 to an entry manifest: only the six authorized existing paths change. V001–V018, PA25 SQL, application Java/resources, OpenAPI, POM, schema facts 44/522, controller document, and historical/governance inputs remain byte-identical.
- Independently parsed V018 `$old$`/`$final$`: 16/18 tuples, no removals, exactly `session_id` and `call_id` added as `evidence_role|column|evidence.evidence_record|<column>|SELECT|evidence_owner`; non-grantable semantics and owner-issued executable grant remain untouched.
- Independently parsed PA25 `$frozen$`/`$history$`: ACL facts **53/290/18/410**, **18** history rows. Eleven distinct approved membership pairs remain unchanged, including seven edges for the shared Operations login; success literals remain **7 logins / 9 exact-ACL-monitored roles / 11 edges**. This is static non-regression evidence, not live PostgreSQL proof.
- Source inventory: **77 Surefire classes / 712 ordinary tests**, **32 Failsafe classes / 520 ordinary tests**, **109 classes / 1232 tests**. Both edited suites retain the exact entry method lists (8 boundary / 16 IT). CI inventory assertions, V018 hash/checksum assertions, frozen predecessor pins, historical-tail pins and ACL/topology assertions are unchanged.

## Documentation preservation

Raw bytes after the unique full-line `<!-- PA36 HISTORICAL BODY -->` delimiter, including their original mixed/CRLF representation, were compared to entry copies and are identical. The existing historical-tail canonical pins are unchanged:

| Document | Historical raw SHA256 | Historical LF SHA256 |
| --- | --- | --- |
| docs17 | `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` | `8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f` |
| PA25 runbook | `588e7c1aafc3f2418a8341790eaae0692919b131cf4a76f414d848eb9dc9f63a` | `8b2daec18d9d0ab5498a50d63392f0a253b82eff68822d2a04860981c80c242d` |
| backend README | `0bd1b7b17f388a603a06dc47798748ab8df16199d67d9e243332dc183350a9c6` | `fb6ab282e38c7a308ad963b9481a856682dd488ff30cc8eb412ab80f55fb3beb` |

Updated docs17 full-LF SHA256: **`4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6`**. Runbook full-LF: `792a32f72d6657b59a220a42ec7c11f94361078c8c0d491ff52d777fe22f08ab`; backend README full-LF: `bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177`.

## Commands and observed outcomes

Commands below run from `backend` with `JAVA_HOME=/opt/jdk21`. No wrapper/POM/configuration changes were made.

| Exact command | Outcome |
| --- | --- |
| `./mvnw -B -ntp -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests test` | Exit 126: existing CRLF shebang `/bin/sh^M`, bad interpreter; Maven did not start. |
| `./mvnw -B -ntp -Pcontainer-smoke -Dit.test=EvidenceRoleIvrAssociationAclIT verify` | Exit 126: same wrapper pre-start failure. |
| `./mvnw -B -ntp -Pcontainer-smoke clean verify` | Exit 126: same wrapper pre-start failure. |
| `/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests test` | Exit 0, BUILD SUCCESS; selected XML **1 class / 8 tests / 0 failures / 0 errors / 0 skips**. |
| `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=EvidenceRoleIvrAssociationAclIT verify` | Exit 1, BUILD FAILURE in Surefire: **77 classes / 712 tests / 8 failures / 359 errors / 0 skips**. Failsafe lifecycle was not reached. |
| `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=EvidenceRoleIvrAssociationAclIT failsafe:integration-test failsafe:verify` | Additional diagnostic only, not a replacement/passing lifecycle: exit 1; selected XML **1 class / 16 tests / 0 failures / 16 errors / 0 skips**, all in container setup because Docker socket operations are forbidden. No PostgreSQL IT body executes. |
| `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke clean verify` | Exit 1, BUILD FAILURE; 03:05, finished **2026-11-17 00:01:39 +08:00**. Fresh Surefire XML: **77 classes / 712 tests / 8 failures / 359 errors / 0 skips**. Failsafe not reached: **0 current-run classes / 0 tests**, not a zero-skip 520-test pass. |

Evidence directory: `/tmp/pa36-pr-fix1-evidence/`, including exact command logs/exit files, entry raw copies and tracked manifest, `integrity.json` with all 17 raw/canonical/checksum triples, inventory and selected XML. Pre-existing XML was present before execution: only the explicitly selected class is attributed to the focused 8-test run and 16-test diagnostic. Old XML for other classes is **not** evidence of a current full pass. The full clean run removes stale target reports before measurement.

The requested green **77/712 + 32/520 = 109/1232**, zero failures/errors/skips, is **NOT CONFIRMED** on final bytes. The 109/1232 identity inventory is independently confirmed, not promoted into runtime success. The focused boundary test also passes 8/8 within the final failed clean run. No failing test was skipped or weakened to obtain green output.

Repository-root read-only Git uses `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD" <subcommand>` because plain WSL Git cannot resolve the Windows `D:/...` linked-worktree pointer. With that prefix, `diff --check -- <the seven intended paths>` exits **0**, `diff --cached --quiet` exits **0**, and `status --short` shows exactly the six modified existing paths, this untracked artifact and pre-existing untracked `.codex`. No other tracked bytes change. A strict Python UTF-8 decode, BOM-prefix check and per-line trailing-space/tab scan pass for all seven intended files; entry/final raw-byte comparisons preserve the three historical bodies and existing EOL forms. The Git pointer/configuration were not changed. These outcomes are final artifact checks, not staging or acceptance.

## Validation blockers and owner destinations

- **Controller / bounded-fix scope owner:** correcting the required current docs changes their full digest, but `OperationsActivationReadBoundaryTests.historicalBytes`, `OperationsLifecycleBoundaryTests.historicalBytes` and `OperationsActivationReadsIT.historicalBytes` still require old full-LF `663b6cb4bce60c2937a8d52b4bc788595912fe1908e1847f8d7a7e152ab2eeaf` before extracting the unchanged tail. Those three files are expressly outside the allowed write set. Observed Surefire hash failures include the first two and `OperationsSubjectReadBoundaryTests`, which delegates to `OperationsLifecycleBoundaryTests`; the IT helper is a static blocker for a later Docker-capable run. Controller must authorize the minimal synchronized full-document pin updates in a subsequent bounded action; no historical-tail pin needs changing. They were not edited, bypassed or weakened here.
- **Controller / validation runtime owner:** Mockito cannot self-attach in this sandbox, and localhost/network socket operations fail; this produces numerous unrelated errors and environment-dependent failures. Docker client access is denied at `unix:///var/run/docker.sock`; Testcontainers reports `java.net.SocketException: Operation not permitted` and no valid Docker environment. Repeat the exact required commands in an authorized Java/Docker-capable runtime after resolving the scope-controlled digest coupling. No permissions/configuration/socket workaround was attempted.
- **Controller / hosted CI:** post-push GitHub Actions **NOT EXECUTED**. Controller assesses a new exact-head run after protected push, including whether 45 minutes suffices. No hosted success, production readiness, approval or passed review gate is claimed.
- **Technical Documentation + Backend/DBA / docs14 deferred destination:** preserve `PA36-AGG-B-01`; its PA28/V016 current-heading and evidence-column enumeration need a separately authorized update with all dependent pins. No docs14 edits here.
- **Platform/Security/Operations and PA35 owner / existing production and behavior gates:** credentials/TLS/pg_hba, rotation/old-password refusal, pool drain/stale sessions, marker custody, exclusive/cluster administration and application-side association behavior remain NOT EXECUTED and owner-routed. PA35 remains paused; this fix does not broaden application/production scope.

## Stop boundary

Only the bounded accepted edits and this artifact are delivered. No `git config`, staging, commit, push, branch/PR creation or update, reviewer request, approval, mark-ready, merge, external comment, controller mutation, re-review or other gate was entered. Stop at **PR review -> fix** and return the failed/blocked validation evidence to Controller.
