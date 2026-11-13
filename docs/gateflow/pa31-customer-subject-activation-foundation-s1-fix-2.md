# PA31 Customer Subject Activation Foundation — Implementation Fix-2

## 1. Identity, authority and outcome

- Task: `pa31-subject-activation-implementation-fix2-gpt-20261113-01`; date: 2026-11-13.
- Runtime: supplied Codex CLI, Linux/WSL/bash; requested GPT `gpt-6.1-sol` / high. Exact executing model/provider metadata is not independently exposed; the requested routing label is not runtime attestation. No subagents were used.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa31-next-work-unit/codex-call`; branch remains `codex/pa31-next-work-unit`; HEAD remains `297e7c2e349d5cd6f0bb45dd94309a4ec0311137`.
- Authority: active PA31 Control, accepted cumulative plan and both plan fixes, accepted plan-review inputs, S1 implementation report, historical S1 fix-1 and this bounded dispatch. No implementation review or later gate was entered.
- Outcome: **partial source repair with passing focused units; blocked on independent reproduction of the shared PostgreSQL ACCEPT failure and required PostgreSQL16 validation. NOT a completed fix or passing Controller intake.** No ACL/schema contradiction or new authorization request is asserted.

## 2. Independently established diagnosis

### Fix-1 privilege claim rejected

Frozen V004 explicitly grants platform schema usage and Outbox execution to `activation_consent_role`; checking only V001/V007 omitted the intervening grant. V014 does not revoke this activation-role grant. The accepted plan's sufficient-privileges boundary therefore remains binding. No migration, grant, V018 or owner redesign was made. Historical S1 fix-1 is unchanged but its missing-grant diagnosis and proposed authorization escalation are withdrawn by the corrected S1 report.

### Preliminary hypothesis 1 rejected: the predicate is not inverted

Both activation and entitlement validators reject when `isAlreadyGranted == sameAction`. The relevant truth table is:

| Outcome | Same granting action | Equality / result |
| --- | --- | --- |
| GRANTED | true | false: valid, subject to all integrity checks |
| GRANTED | false | true: reject |
| REPLAY | true | false: valid, subject to original policy/fingerprint |
| REPLAY | false | true: reject |
| ALREADY_GRANTED | true | true: reject |
| ALREADY_GRANTED | false | false: valid, retaining original trial |

Existing fresh ACCEPT, frozen ACCEPT replay and later ACTIVE event tests pass with the unchanged activation predicate. The existing owner replay test now exercises all six combinations plus mismatched same-action policy/fingerprint rejection for GRANTED and REPLAY, and asserts role restoration throughout. Unknown outcomes remain rejected by the existing owner/descriptor allowlists. No result assertion was weakened and neither predicate was changed.

This disproves the proposed boolean explanation for a **valid** fresh GRANTED result. It does not prove that the unavailable PostgreSQL run supplies valid facts or identify its real shared ACCEPT exception.

### Preliminary hypothesis 2 confirmed as a reproducible mapping defect

The existing `trialOutboxAndCommitFailuresRespectExactDecisionWriteSets` method was strengthened without adding a test identity. Before the product repair, a jOOQ exception containing an outer JDBC wrapper around a P0001 trial-policy exception produced INTERNAL instead of CONSENT_REQUIRED. Its red regression ran one method: 1 failure, 0 errors, 0 skips, exit 1. This identifies a real exception-selection defect independently of the sanitized public exception.

The original helper selected the first SQLException in the cause chain, potentially a wrapper without the meaningful database SQLSTATE. The mapper also inspected only the first message line. The repair follows at most 20 causes to the deepest SQLException, then accepts only SQLSTATE P0001 and the exact two allowed trial diagnostics. Bare exact messages remain supported; exact `ERROR:` diagnostic lines are recognized within at most 64 lines/8192 characters, not by substring search. A wrapper prefix no longer prevents recognition. Integrity/argument/campaign/unknown P0001 errors, suffix matches, context-only mentions and wrong SQLSTATEs are not promoted to CONSENT_REQUIRED. Existing infrastructure-state mapping is retained.

Final regressions exercise direct and nested jOOQ/JDBC exceptions, bare/multiline/wrapper-prefixed diagnostics for both TRIAL_POLICY_NOT_FOUND and TRIAL_ACTION_CONFLICT, unknown diagnostics and infrastructure states. They verify rollback/no commit/no activation Outbox on the mapped policy/action rejection. Public exceptions retain their constant message and no cause; existing redaction tests pass. No diagnostic output was inserted into production or test code.

**Causal limitation:** this is a reproduced implementation defect consistent with the Controller's policy-race symptom, not an independently observed PostgreSQL root exception. The child cannot establish that it explains the Controller race, ordinary ACCEPT failures, HTTP 500s, or every other shared failure. Those remain unresolved until actual PostgreSQL execution permits bounded internal diagnosis. No invented ACL/predicate cause replaces that missing evidence.

## 3. Exact fix-2 writes and preserved boundaries

Only these five repository paths differ from the fix-2 entry snapshot:

1. `backend/src/main/java/com/onecall/core/activation_consent/internal/command/DefaultCustomerSubjectActivationService.java` — bounded SQL cause/diagnostic mapping only.
2. `backend/src/test/java/com/onecall/archtests/activation/CustomerSubjectActivationServiceTests.java` — strengthen one existing failure method with wrapped-exception regressions.
3. `backend/src/test/java/com/onecall/archtests/entitlement/ActivationTrialServiceTests.java` — strengthen one existing replay method with outcome/action and fingerprint/policy matrices.
4. `docs/gateflow/pa31-customer-subject-activation-foundation-s1-implementation.md` — corrected fix-2 diagnosis/status only.
5. `docs/gateflow/pa31-customer-subject-activation-foundation-s1-fix-2.md` — this sole new fix artifact.

All three implementation/test files are in the accepted 40-path allowlist; the other two are explicitly authorized reports. The pre-existing 15 modified existing implementation paths and 25 new implementation paths remain the cumulative implementation inventory. S1 fix-1 is preserved unchanged, as are unrelated/private files. No file is deleted. No dependency, fixture, transaction, lock order, role assertion/restoration, pool or owner change is made.

DECLINE still commits its single refusal consent before the public exception, with zero activation/trial/Outbox/audit writes. Its focused unit and web assertions pass; new PostgreSQL proof is unavailable. Fresh ACCEPT, later ACTIVE and exact replay unit assertions pass without changing their identities or state semantics. Database atomicity/ACL/concurrency/rollback/HTTP behavior is not claimed from mock-based unit results.

## 4. Actual executed commands and exit codes

Evidence is outside the repository at `/tmp/pa31-implementation-fix2-gpt-20261113-01/evidence/`. This permitted `/tmp` destination is used instead of the non-writable Windows evidence drive. No repository cache or wrong-drive directory was created. All Maven commands run from `backend` with JDK21/Maven3.9.9 and the requested restricted PATH.

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin
/opt/maven-3.9.9/bin/mvn -B -ntp test-compile
/opt/maven-3.9.9/bin/mvn -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dtest=CustomerSubjectActivationsWebTests,CustomerSubjectActivationServiceTests,ActivationTrialServiceTests,CustomerSubjectActivationConfigurationTests,CustomerSubjectActivationBoundaryTests,ModuleBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests,CustomerEvidenceBoundaryTests,CustomerApiBoundaryTests test
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin \
  /opt/maven-3.9.9/bin/mvn -B -ntp \
  -Pcontainer-smoke \
  -Dit.test=CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT \
  -DfailIfNoTests=false verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke \
  -Dit.test=CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT \
  -DfailIfNoTests=false failsafe:integration-test failsafe:verify
```

The exact requested verify command was executed before the final focused run. It failed in default Surefire, so the supplementary direct Failsafe goals attempt the same two IT classes without modifying any test, disabling any assertion, or passing any skip/ignore flag. The explicit startup Mockito agent is used only in the focused unit command to avoid denied dynamic attachment; no dependency/POM change is involved.

| Command / evidence | Exit | Actual result |
| --- | --- | --- |
| Baseline `test-compile`; `test-compile-baseline.log` | 0 | Existing candidate compiles. |
| Additional offline `-o test-compile`; `test-compile-offline.log` | 0 | Cached toolchain/dependencies are usable. |
| Pre-repair single failure method with explicit agent; `mapping-regression-before.log` | 1 | 1 method, 1 failure, 0 errors, 0 skips: expected CONSENT_REQUIRED, actual INTERNAL. |
| Exact requested focused-PG16 verify; `focused-pg16-exact.log` | 1 | Surefire 520 methods, 5 failures, 273 errors, 0 skips; denied dynamic Mockito attachment and socket-dependent context failures; Failsafe never reached. Not a PostgreSQL result. |
| Final `test-compile`; `test-compile-final.log` | 0 | Final source and all test/fixture source compile. |
| Final selected 11 unit/architecture classes; `focused84-final.log` and `focused84-xml/` | 0 | Exactly 84 methods = 52 new +32 named existing; 0 failures, 0 errors, 0 skips. |
| Supplementary direct focused Failsafe; `focused34-direct.log` and `focused34-xml/` | 1 | 2 class-initialization XML entries, 0 failures, 2 errors, 0 skips; Docker unavailable. **Not 34 executed methods.** |
| `docker version`; `docker-version.log` | 1 | Permission denied accessing `/var/run/docker.sock`; Testcontainers likewise cannot establish a Docker environment. |
| `python3 /tmp/pa31-retry1/ci-source-only.py` from backend; `static-ci.log` | 0 | Static CI/source identities and arithmetic: 59 unit classes/520 methods +25 IT classes/404 methods =84/924; annotation925, POM includes25. |
| `python3 /tmp/pa31-implementation-fix2-gpt-20261113-01/evidence/audit.py` from root; `audit.log` | 0 | Exact five-path fix delta, cumulative40, identity preservation, frozen20 raw bytes, no V018, unchanged branch/HEAD/index and protected inputs. |

The original Controller XML was copied to `controller-prior-xml/` before the disposable attempts overwrote generated reports: exactly 34 methods, 5 failures, 15 errors, 0 skips. It is explicitly historical evidence. `exact-verify-surefire/` preserves the failed exact command's reports separately. `focused84-xml/` contains only the 11 final selected classes, not stale reports counted as passes. `xml-summary.json` separates all four sets.

A first XML-checking command exited 1 when it required the direct run to contain the 34 source method identities; the real XML contains only two class-initialization error entries. The corrected summarizer records 2/0/2/0 explicitly rather than inventing 34 results. No test setup was weakened to force a different count. Initial default Git commands also fail on the Windows gitfile; all subsequent Git reads use explicit `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call8 --work-tree="$PWD"`, without metadata repair.

## 5. Static custody and inventory

- The entry snapshot covers 1941 non-generated files. Completion permits exactly the five paths listed in section 3, no deletion and no unrelated addition; generated Maven reports are excluded from product-path accounting and retained only as execution outputs.
- Cumulative product scope remains exactly 15 modified existing paths +25 new implementation/test/fixture paths =40. This task adds only its single authorized fix artifact; the original S1 report and S1 fix-1 are accounted separately, and pre-existing `.codex` is not a task addition.
- All 86 new identities remain exactly the accepted plan lists (52 unit +34 container); all 838 historical identities remain unchanged. Source total84 classes/924 methods; annotation line scan925; POM Failsafe includes25. No new test method/class, rename, deletion, ignore or skip was introduced.
- All20 frozen raw SHA256 values remain the S1 baseline: OpenAPI, docs14, docs15 and exactly V001–V017. No V018. Accepted plan, both plan fixes, Control, reviews, historical S1 fix-1, Git gitfile and private/unrelated files remain byte-identical to entry. The complete frozen20 values and exact fix delta are in `audit-result.json`.
- No `backend/D?:` cache is present. No dependency/schema/grant/OpenAPI/frozen-hash change was made. No production policy/content/credentials, SQL diagnostic payload or sensitive value is exposed by the mapper or added logging.

## 6. Blocker, residual risk and stop

**Required 34-method PostgreSQL16 pass is NOT obtained.** Actual shared ACCEPT exception extraction/reproduction is NOT obtained. Both are blocked by the child sandbox's denied Docker/socket access; approval mode is `never`, so no escalation is attempted. Source inspection plus a mock-based mapping regression cannot establish or repair an unknown database failure. No further speculative source/ACL/schema edit is justified.

The uncommitted partial candidate and evidence are handed back for Controller diagnostic/PostgreSQL work in an authorized environment. It is **not ready for a passing Controller PostgreSQL16 intake or code review**; no all-ACCEPT fix, 34/0/0/0 pass, full-container pass, production readiness or live acceptance is claimed. Full container, code/deep review, packaged JAR/firewall and later gates were not begun.

No commit, push, branch creation/change, PR creation/update, merge, approval, mark-ready action, reviewer request, external comment, issue mutation or later-gate action occurred. Read-only Git confirms the same branch/HEAD and empty index. No remote command was used.
