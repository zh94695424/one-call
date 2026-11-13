# PA31 Customer Subject Activation Foundation — Implementation Fix-3

## 1. Task, runtime and boundary

- Task: `pa31-subject-activation-implementation-fix3-gpt-20261113-01`; date: 2026-11-13; work unit: `PA31-customer-personal-subject-activation-foundation`, existing S1 only.
- Runtime/provider: supplied Codex CLI, Linux/WSL2/bash, OpenAI/GPT routing; requested routing in Control is `gpt-6.1-sol` / high. Exact executing model/provider backend metadata is not independently exposed; routing is not runtime attestation. No subagents/provider switch was used.
- Toolchain: `/opt/jdk21` Temurin21.0.13+9, `/opt/maven-3.9.9/bin/mvn` Maven3.9.9. Approval policy `never`; no escalation attempted.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa31-next-work-unit/codex-call`; branch `codex/pa31-next-work-unit`; HEAD `297e7c2e349d5cd6f0bb45dd94309a4ec0311137` unchanged.
- Inputs: Active PA31 checkpoint, accepted cumulative plan/fixes, both accepted final plan reviews (`plan-review-20261113-113756.md`, `plan-review-20261113-120136.md`), S1 report and S1 fix-1/fix-2. No implementation peer-review conclusion was consulted or review gate entered.
- Outcome: **bounded timestamp-binding fix candidate for Controller's exact 34-method PostgreSQL16 rerun; not a PostgreSQL pass, acceptance or later-gate authorization**.

## 2. Root cause and independent evidence

The Controller supplies actual JFR-captured PostgreSQL evidence: valid first ACCEPT reaches the head insert with OffsetDateTime arguments and fails because `created_at` is timestamptz but its expression is character varying. That is database evidence supplied by Controller, not a PostgreSQL run independently repeated in this child. It is not an ACL, migration, grant or Outbox-privilege defect. Historical fix-1's diagnosis remains rejected; its artifact is unchanged.

Independent source checks confirm V007 declares `service_activation.created_at`, `service_activation.updated_at` and `service_activation_transition.occurred_at` as timestamptz. Before repair the repository passed UTC OffsetDateTime values through raw-SQL Object-varargs overloads without casts at all affected positions. Existing compiled bytecode independently confirms the same SQL and atOffset conversions (`repository-before.javap`).

An external Java probe (`BindingProbe.java`, outside the repository) invokes all three real compiled repository methods through actual jOOQ3.21.9 and its JDBC MockConnection. Before repair, the mock JDBC endpoint records **java.lang.String** timestamp bindings, not JDBC typed timestamptz values. This independently verifies the binding mechanism behind the supplied PostgreSQL error, including the other two statements. It does not pretend to be a PostgreSQL engine.

| Existing method | Parameter count / timestamp positions, 1-based | Exact repair |
| --- | --- | --- |
| `createHead` | 4 / 3=created_at, 4=updated_at | `VALUES (?,?,'INACTIVE',1,?::timestamptz,?::timestamptz)` |
| `advance` | 4 / 1=updated_at | `updated_at=?::timestamptz`; remaining WHERE parameters unchanged |
| `appendTransition` | 18 / 18=occurred_at | Last VALUES placeholder only becomes `?::timestamptz` |

No argument or time conversion changes. The four explicit casts make PostgreSQL parse the existing UTC textual parameters as the known target timestamptz type. Final compiled probe verifies complete 4/4/18 argument arrays/order, identical UTC instant (including microseconds), update success and exactly 2/1/1 casts. Its post-repair JDBC bindings remain Strings by design; the SQL casts, not a claim of a changed JDBC setter, provide the server type. `repository-after.javap` preserves final compiled evidence. Removing just the four cast suffixes reproduces the entry repository's raw SHA256 exactly.

## 3. Policy-disable disposition

`policyDisableAroundFunctionSelectionHasFrozenLinearization` remains unchanged. Its before/after policy toggle wraps `issueFirstTrial`; the service executes createHead/advance/appendTransition before calling that hook. The verified timestamp failure can therefore obstruct reaching either toggle branch. This is a source-derived explanation of an obstruction, **not an independently captured exception from that policy method**.

After repairing timestamps, a real policy-disable database result could not be obtained: Docker daemon access is denied. The child has no actual post-repair P0001/other SQL exception from that method and does not invent a diagnosis or declare it fixed/passing. Controller must rerun it and inspect the primary database exception if it still fails; restore failure after an aborted transaction must not replace that primary evidence.

Fix-2's mapper is preserved byte-for-byte: bounded deepest JDBC cause selection, P0001 plus only exact TRIAL_POLICY_NOT_FOUND/TRIAL_ACTION_CONFLICT diagnostics → CONSENT_REQUIRED; unrelated integrity/unknown failures remain INTERNAL and the existing connection/permission/resource infrastructure allowlist remains SERVICE_UNAVAILABLE. No broad message matching, new public code, changed predicate or speculative privilege repair is added. Existing direct/nested JDBC diagnostic, wrapper, negative-control and rollback matrices pass in the focused 18-method service class; that is unit evidence only, not satisfaction of the PG16 policy test.

## 4. Exact writes and preserved invariants

Only these three repository paths differ from this task's entry snapshot:

1. `backend/src/main/java/com/onecall/core/activation_consent/internal/command/JooqSubjectActivationRepository.java` — four explicit casts across three existing statements only; accepted implementation path 6.
2. `docs/gateflow/pa31-customer-subject-activation-foundation-s1-implementation.md` — current status and bounded fix-3 evidence addendum.
3. `docs/gateflow/pa31-customer-subject-activation-foundation-s1-fix-3.md` — this sole new artifact.

No test/fixture source changed at all. Existing S1 fix-1/fix-2, accepted plan/fixes/reviews/Control, OpenAPI/docs14/docs15/V001–V017 and unrelated/private files remain raw-byte-identical to entry. Cumulative40 implementation paths remain 15 existing updates +25 additions; reports are accounted separately. No migration/grant/dependency/ACL/pool/logging change was made.

One slice, same-connection two-role handoff/assertions/restoration, lock order, conditional revision semantics, DECLINE one-consent behavior, zero decision audit, immutable descriptors/replay and no duplicate trial/transition remain unchanged. All test source bytes, identities and assertions are preserved; no addition/deletion/rename/skip/ignore/weakening is introduced.

## 5. Executed commands and results

Evidence root: `/tmp/pa31-fix3/evidence/`. All Maven commands run from `backend` with `JAVA_HOME=/opt/jdk21` and `PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin`.

```bash
/opt/maven-3.9.9/bin/mvn -B -ntp test-compile
/opt/maven-3.9.9/bin/mvn -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dtest=CustomerSubjectActivationsWebTests,CustomerSubjectActivationServiceTests,ActivationTrialServiceTests,CustomerSubjectActivationConfigurationTests,CustomerSubjectActivationBoundaryTests,ModuleBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests,CustomerEvidenceBoundaryTests,CustomerApiBoundaryTests test
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke \
  -Dit.test=CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT \
  -DfailIfNoTests=false failsafe:integration-test failsafe:verify
```

| Execution / evidence | Exit | Actual result |
| --- | --- | --- |
| Final `test-compile`; `test-compile.log` | 0 | Final production and all118 test/fixture sources compile; source recompilation occurred. |
| Exact requested explicit-agent focused selection; `focused84.log`, `focused84-xml/` | 0 | 11 classes, **84 methods =52 new +32 existing**, zero failures/errors/skips. |
| External `javac -proc:none` and compiled BindingProbe before/after; `binding-before.log`, `binding-after.log` | 0 / 0 | All three real compiled repository methods, four JDBC timestamp positions and exact full parameter order verified. Not repository test identities or PostgreSQL execution. |
| `docker version`; `docker-version.log` | 1 | Docker CLI present; `/var/run/docker.sock` permission denied. |
| Supplementary direct focused Failsafe; `focused34-direct.log`, `focused34-direct-xml/` | 1 | Exactly **2 class-initialization errors, 0 failures, 0 skips** due to missing accessible Docker. **Not 34 executed methods; not a product-test failure verdict.** |
| `python3 /tmp/pa31-retry1/ci-source-only.py` from backend; `static-ci.log` | 0 | Checked-in static CI/source identities:59/520 +25/404 =84/924, annotation925, POM includes25. |
| `python3 /tmp/pa31-fix3/evidence/audit.py` from repository root; `audit.log` | 0 | Exact three-path delta, pure four-cast production edit, unchanged tests, cumulative40, frozen20/no V018 and Git/protected-input custody. |

Focused XML is copied by exact class names, not counted from stale directories. `xml-summary.json` separates 84/0/0/0 unit results from 2/0/2/0 container setup entries. No exact/full verify, full container suite, packaged JAR/firewall or OpenAPI generation was run in fix-3; prior historical evidence is not relabeled as this task's execution.

Setup diagnostics are not hidden: default Git fails the existing Windows gitfile and is replaced only for reads by the explicit Linux git-dir mapping. One patch attempt used a redundant backend prefix and failed without modification; the following compile was pre-repair/up-to-date (`test-compile-initial.log`), not the final compilation. The external probe's first timestamp parser expected ISO `T` but jOOQ emits a SQL timestamp space; after normalizing only that separator, both compiled probes pass. Initial javac annotation processing created a generated root build/javadoc file, which was removed; subsequent probe compilation uses `-proc:none`. The first static-CI invocation used root and failed before checks; rerunning from backend passes (`static-ci-initial.log` retained). No such setup correction alters product/test behavior or excuses a product failure.

## 6. Static custody and handoff

The outside-repository entry snapshot covers1942 non-generated files. Final whole-tree raw comparison permits only section4's three paths, with no deletion or unrelated addition. `audit-result.json` records the exact delta and all20 frozen raw hashes. All86 new identities (52 unit +34 container) equal the accepted plan lists; all838 historical identities are unchanged. Full static source inventory remains84 classes/924 methods, annotation925/POM25. This is **not** full source=CI=executed-container-XML evidence.

Read-only Git uses `GIT_OPTIONAL_LOCKS=0 git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call8 --work-tree="$PWD" ...`; no metadata repair. Branch/HEAD remain unchanged; staged diff is empty. Final hashes, sizes and LF line counts of all three changed files are emitted in the completion report and external `artifact-manifest.json` rather than circular self-hashing this artifact.

Residual requirement: Controller must run **exactly34 PostgreSQL16 methods**, with no failures/errors/skips, on this final candidate, explicitly reassessing the policy-disable case. Docker setup failure here is a child runtime limitation. No all-ACCEPT, policy-disable, full-container, production-readiness or live-acceptance pass is claimed. Do not advance to review or another gate on the strength of JDBC mock/unit/static evidence alone.

No commit, push, branch creation/change, PR creation/update, merge, approval, mark-ready action, reviewer request, external comment, issue mutation, code review/deepreview or later-gate entry occurred. No remote command was used. Return this uncommitted bounded fix candidate and stop for Controller validation.
