# PA32 bounded plan fix 1

- Task label: `pa32-operations-lifecycle-plan-fix1-gpt-20261113-01`.
- Runtime/provider: Codex / GPT `gpt-6.1-sol`, reasoning `high`.
- Gate: **fix**; status: **complete, stop for Controller adjudication**.
- Reviewed base: `8206f2a8f8a40cd58793efdda55aa41843af5d5f`.
- Prior cumulative-plan SHA256: `972569faf95aa43f390cbfc37a8723528d94d8437847ae26fe720ef8798d8a68`.
- Historical plan-authoring Control snapshot: `79b305fe7ca474a40823eef309f5ef855abfddb950fbbc1164f09630c2ae8f9`.
- Review-freeze Control provenance: `5ee4dfd6e3b17c5b6400045fdb397d8baf050aab3ce1fc29a8a2b75f146c9039`.
- Binding authority: the PA32 Goal Confirmation text in `docs/gateflow/project-execution-control.md`; the mutable Control file hash is not treated as the authority.

## 1. Disposition and exact plan changes

| Finding | Disposition | Cumulative-plan sections changed |
| --- | --- | --- |
| PA32-PLAN-REV-01 | **Closed.** §3 now requires the already-allowlisted `ModuleBoundaryTests` edit to preserve `containsExactlyInAnyOrder`, add `"operations_iam"` exactly once, change the description from exactly seven to exactly eight, and preserve the method plus all nine test methods. §4 identifies the exact assertion edit and all other exact dependency assertions remain unchanged. | §§3, 4 |
| PA32-PLAN-REV-02 | **Closed.** `DefaultEmployeeAdministratorLock` is pinned as an Operations-IAM-owned `@Component` with `@Profile("persistence")` and the exact `onecall.operations-lifecycle.enabled=true` condition. Its constructor is qualified with `operationsLifecycleTransactionManager` and `operationsLifecycleDsl`; it exists in persistence-only mode, never web-only mode, and activation-consent imports only published interfaces. Configuration tests now cover persistence-only construction, web-only absence, qualifiers, ambiguity and unavailable behavior. | §§4, 6, 8.4 |
| PA32-PLAN-REV-03 | **Closed.** `Failure(Code)` is replaced with a nested redacted `RuntimeException` for the lifecycle port. The employee-lock collaborator has a nested redacted `RuntimeException` with exactly `Kind { UNAVAILABLE, INTEGRITY }`. The plan pins `UNAVAILABLE → SERVICE_UNAVAILABLE`, `INTEGRITY → INTERNAL`, rollback-before-mapping, controller translation and no throwable serialization; existing named tests cover these rules without adding methods. | §5.1, §§8.4–8.5 |
| PA32-PLAN-REV-04 | **Closed.** The contradictory invariant now reads “no undeclared/new foreign read or row lock.” It expressly retains the V007-granted `subject_number.business_subject.id` read and continues to prohibit reading or locking `operations_iam` under `activation_consent_role`. | §7.1 |
| PA32-PLAN-REV-05 | **Closed.** §11 labels `79B305FE...` as the historical plan-authoring snapshot, records `5EE4DFD6...` as review-freeze provenance, disclaims byte-identity for mutable Controller bookkeeping, and states that the PA32 Goal Confirmation text is binding. | §11 |

## 2. Scope and arithmetic proof

No confirmed goal, non-goal, implementation slice, schema, OpenAPI contract, migration, privilege, authorization boundary, lock order, state transition, replay rule, error set, audit/Outbox mapping, or validation contract changed. The edits only make already accepted architecture-test, bean-registration, failure-channel, invariant, and provenance requirements code-generation-ready.

| Measure | Before | After | Reason |
| --- | ---: | ---: | --- |
| Implementation allowlist paths | 36 | 36 | No new source or test path; existing allowlisted files receive more exact instructions. |
| New production files | 22 | 22 | `DefaultEmployeeAdministratorLock` remains an existing planned file. |
| Changed existing files | 5 | 5 | `ModuleBoundaryTests` remains one changed file; only its assertion literals are clarified. |
| New test/fixture files | 9 | 9 | No fixture or test class is added. |
| Surefire classes/methods | 59 / 520 | 59 / 520 | No test method changes. |
| Failsafe classes/methods | 25 / 404 | 25 / 404 | No integration-test method changes. |
| PA32 added classes/methods | 7 / 90 | 7 / 90 | Existing planned inventory is preserved. |
| Final classes/methods | 91 / 1,014 | 91 / 1,014 | `ModuleBoundaryTests` keeps nine methods; all other exact inventories remain fixed. |
| Annotation-token lines | 925 + 90 = 1,015 | 925 + 90 = 1,015 | No test source is added or annotated by this fix. |
| Failsafe includes | 25 | 27 | This is the existing PA32 implementation target, not a fix change. |

The new fix artifact is a gate artifact and is not an implementation path. The cumulative plan’s completion report now records one modified plan plus this one new fix artifact; no source, test, migration, OpenAPI, CI, POM, existing documentation, or Control file was edited.

## 3. Validations performed

- Read the binding PA32 Goal Confirmation and Controller adjudication in `project-execution-control.md` before editing.
- Read the cumulative plan and both required review artifacts, including all accepted findings and explicit dispositions.
- Performed read-only source inspection of `ModuleBoundaryTests`, existing Spring profile/property configuration patterns, existing redacted `RuntimeException` failure patterns, the employee/session boundary, lifecycle handoff patterns, OpenAPI operations routes, docs/14, docs/15, docs/17, and V007.
- Checked that the plan’s named configuration tests and boundary tests retain their existing method counts and now state the required assertions.
- Ran `git diff --check` after the documentation edits.
- No Maven, Docker, database, Flyway, package-install, network, remote-state, staging, commit, branch, or tracked-file-mutating command was run.

## 4. Limitations and residual risks

- XML equality, Maven execution, PostgreSQL16 locking/rollback, Testcontainers, packaged-JAR probes, CI execution, and runtime Spring bean creation remain future implementation-gate evidence; they are not claimed here.
- The environment-specific review gitdir command remains review evidence only; native git equivalents remain valid and were not broadened by this fix.
- The separately enabled lifecycle datasource remains an intentional fail-closed isolation choice and is not replaced by the customer activation pool.
- The external employee-session verifier, production role memberships, and deployment credentials remain external prerequisites; absent, ambiguous, or unusable dependencies remain `SERVICE_UNAVAILABLE`.
- Controller must adjudicate this corrected plan before implementation or re-review. This artifact does not authorize implementation, source changes, schema/API/privilege expansion, or any remote action.

## 5. Completion

All Controller-accepted findings PA32-PLAN-REV-01 through PA32-PLAN-REV-05 are addressed in the cumulative plan. The goal and all frozen boundaries remain unchanged. Both allowed artifacts are present; stop for Controller adjudication.
