# PA36 PR #46 bounded fix 2

## Identity and scope

- Task: `pa36-pr46-fix2-gpt61sol-20261117-01`; work unit: `PA36-evidence-role-ivr-association-acl`.
- Date: 2026-11-17, Asia/Shanghai. Gate remains **PR review -> fix**; this artifact does not advance or accept a gate.
- Entry HEAD, verified with the explicit read-only linked-worktree Git command: `d5badecf74f458bd54de2987c84069d6b2a4219e`.
- This round is only the minimal synchronized full-document pin update authorized after fix 1. The six previously modified files and the new fix-1 artifact were already present on entry and remain byte-identical to round entry.

## Exact changed paths this round

1. `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`
2. `backend/src/test/java/com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java`
3. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
4. `docs/gateflow/pa36-evidence-role-ivr-association-acl-pr-fix-2.md` (this sole new artifact)

Each Java file changes exactly one hash literal, on one assertion line. No method, test identity, annotation, execution profile, marker check, historical-tail pin or current-contract assertion is changed.

## Hash rationale and preservation

Fix 1 changed only the PA36 current section of docs17, so the full-document LF digest changed from `663b6cb4bce60c2937a8d52b4bc788595912fe1908e1847f8d7a7e152ab2eeaf` to `4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6`. These three helpers assert the full digest before extracting the historical body. Synchronizing that full-document assertion restores consistency without relaxing the independent historical-body boundary. This round does not edit docs17.

Independent byte checks confirm each entry file becomes its final file by exactly one old-to-new literal replacement, with all other bytes preserved. The old literal is absent and the new literal occurs exactly once in each of the three asserting files. The docs17 full-LF hash independently recalculates to the new value.

The unique `<!-- PA36 HISTORICAL BODY -->` delimiters and historical-tail pins remain unchanged; independently extracted LF tails recalculate to:

| Document | Historical-tail LF SHA256 |
| --- | --- |
| `docs/17-production-security-design-spec.md` | `8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f` |
| `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | `8b2daec18d9d0ab5498a50d63392f0a253b82eff68822d2a04860981c80c242d` |
| `backend/README.md` | `fb6ab282e38c7a308ad963b9481a856682dd488ff30cc8eb412ab80f55fb3beb` |

V018 canonical LF SHA256 independently remains `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817`. Independent parsing of PA25's frozen ACL/history arrays confirms **53/290/18/410** (customer role / seven-role union / evidence role / complete oracle), **18** history rows, **9** exact-ACL-monitored roles and **11** distinct approved membership edges, including **7** shared Operations edges. Both success literals remain **7 logins / 9 exact-ACL-monitored roles / 11 edges**. This is static integrity evidence, not live PostgreSQL execution.

An entry raw-SHA256 manifest covering 2,134 tracked/previous-artifact files confirms only the three authorized existing test files changed this round. V001-V018, PA25 SQL, ACL/topology definitions, OpenAPI, application source/resources, POM, CI, the three public docs, the prior PA36 boundary/IT, fix-1 artifact and Control remain byte-identical to entry. No staging, configuration change or Git mutation was performed.

## Validation evidence

Commands run from `backend` using `JAVA_HOME=/opt/jdk21` and `/opt/maven-3.9.9/bin/mvn`; no test, profile, dependency or runtime setting was weakened or bypassed.

| Command | Observed result |
| --- | --- |
| `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=OperationsActivationReadBoundaryTests,OperationsLifecycleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test` | Exit 0, BUILD SUCCESS: 3 classes / 24 tests / 0 failures / 0 errors / 0 skips; each class passes 8 tests. Finished 2026-11-17 00:05:51 +08:00. |
| `docker info` (read-only capability check) | Exit 1: permission denied connecting to `unix:///var/run/docker.sock`; no sandbox or socket workaround attempted. |
| `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke clean verify` | Exit 1, BUILD FAILURE in Surefire: 77 classes / 712 tests / 5 failures / 359 errors / 0 skips. Elapsed 03:07; finished 2026-11-17 00:09:31 +08:00. Failsafe was not reached; no current-run Failsafe XML exists. |

### Environment blockers, separate from the corrected coupling

The full clean lifecycle is not green. Its five assertion failures are in `MediaPlaybackConfigurationTests` (four) and `SubjectClaimPersistenceConfigurationTests` (one); their exception chains show Mockito/Byte Buddy cannot self-attach to the sandbox JVM. The error reports likewise include widespread Mockito mock-maker initialization failures. Docker access is independently denied, so no live PostgreSQL IT success is claimed. These runtime restrictions are not evidence of a defect in the three synchronized assertions; repeat the unchanged full command in an authorized JVM/Docker-capable runtime. No workaround, test skip or unrelated fix was attempted.

Fresh full-run XML confirms the three focused boundary classes and the delegating `OperationsSubjectReadBoundaryTests` each pass 8/8 with zero failures/errors/skips. Thus the previously reported Surefire full-document hash failures are absent. `OperationsActivationReadsIT` was compiled and independently byte-audited, but its container tests were not executed because Surefire stopped the lifecycle. Zero Failsafe reports is not a passing container suite.

Local evidence directory: `/tmp/pa36-pr-fix2-evidence/`. It contains Maven logs and exit files, copied focused XML reports preserved before the clean lifecycle, Docker capability output and independent integrity audit output. The initial ad-hoc ACL audit incorrectly interpreted 53 as a schema-tuple count; the corrected independent audit verifies the actual customer-role count of 53 and passes, with no repository edits caused by that diagnostic error.

Final Git checks use `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"` from the repository root. `diff --check -- <ten intended paths>` exits 0; it covers the six pre-existing modified paths, the three synchronized test files and this artifact. `diff --cached --quiet` exits 0: the index is clean. Because Git's ordinary diff does not inspect untracked content, both gateflow artifacts are independently checked for UTF-8, BOM absence and trailing whitespace, and pass. Final status excluding untracked `.codex` shows only the six pre-existing modifications, three new test modifications and the two gateflow artifacts (fix 1 pre-existing, fix 2 new). Entry-manifest comparison confirms no other tracked bytes changed. Git emits existing LF-to-CRLF conversion warnings, not whitespace-check failures; no configuration or line-ending normalization is performed.

## Gate disposition

Only the synchronized full-document hash coupling is corrected. No claim of hosted CI success, live PostgreSQL success, production readiness, approval or re-review is made. No commit, stage, push, PR mutation, external comment, reviewer request, merge, approval or mark-ready action is performed. Stop at bounded-fix completion; do not enter re-review.
