# PA32 Operations Subject Suspension/Resume Lifecycle — Aggregate Fix 1

- Task label: `pa32-aggregate-fix1-gpt-20261114-01`
- Gate: aggregate deepreview → fix
- Scope: the three Controller-adjudicated Low test-evidence gaps and residual-bookkeeping correction only.
- Production code, policies, package-info, plan, Control, CI, POM, OpenAPI, migrations, and frozen docs are unchanged.
- Test identities and arithmetic are preserved: `OperationsLifecycleConfigurationTests` remains 8 methods, `OperationsSubjectLifecycleServiceTests` remains 18 methods, PA32 remains 90 methods, and the full target remains `574/440 = 91/1014`.

## Final Fix Evidence

### PA32-AGG-L-01 — accepted Low gap — fixed

Extended `defaultWebAndDisabledProfilesCreateNoLifecyclePool` in `backend/src/test/java/com/onecall/archtests/OperationsLifecycleConfigurationTests.java` with an enabled `true` plus `web`-only `ApplicationContextRunner` case. The context must start successfully while none of `operationsLifecycleDataSource`, `operationsLifecycleTransactionManager`, `operationsLifecycleDsl`, `operationsSubjectLifecycleService`, or `employeeAdministratorLock` exists. No test method was added.

### PA32-AGG-L-02 — accepted Low gap — fixed

Extended `missingAndAmbiguousOwnerCollaboratorsFailClosed` in `backend/src/test/java/com/onecall/archtests/OperationsLifecycleConfigurationTests.java`. For zero and two `EmployeeAdministratorLock` candidates, the test now refreshes a valid `persistence`/enabled context and obtains the service from the real `OperationsLifecyclePersistenceConfiguration.operationsSubjectLifecycleService` bean path, with the infrastructure collaborators supplied as valid test beans. Each per-call invocation returns redacted `SERVICE_UNAVAILABLE` before transaction/repository interaction. The lazy-resolution failure case remains covered by the existing `RootBeanDefinition` case. No test method was added.

### PA32-AGG-L-03 — accepted Low gap — fixed

Extended `unusableHeadTransitionOrEmployeeContextFailsClosed` in `backend/src/test/java/com/onecall/archtests/activation/OperationsSubjectLifecycleServiceTests.java` with otherwise correctly shaped persisted witnesses covering wrong status-event identity, wrong fingerprint, channel/action-domain mismatch variants, and `head.updatedAt` versus witness `occurredAt` mismatch. Every variant asserts sanitized `SERVICE_UNAVAILABLE` and no writes; the existing unusable-head, missing-witness, employee-failure, and caller-context cases remain. No test method was added.

## Reviewer B F-1 Disposition

**Deferred-with-owner; not repaired in production code and not silently accepted as code fixed.** The proposed customer-path DB-clock change is outside the confirmed PA32 goal and the accepted 36-path implementation allowlist, and would alter PA31 behavior. The cross-path clock-domain risk remains a follow-up activation-clock-hardening item with the ownership and destination recorded below.

The reachable failure is positive JVM-ahead/DB-behind skew: a customer activation head written with JVM `Instant.now()` can be ahead of DB `clock_timestamp()`. The first Operations command then rejects the regressing DB timestamp and returns sanitized `SERVICE_UNAVAILABLE`/503 with rollback and no partial transition, audit, or Outbox writes.

Negative JVM-behind/DB-ahead skew is not a reachable cross-path failure under the current production write topology. Customer code does not write `service_activation` after the initial INACTIVE create+advance transaction, which uses the same JVM timestamp for `created_at` and `updated_at`: ACCEPT on ACTIVE writes no head, ACCEPT on SUSPENDED returns FORBIDDEN, and DECLINE writes no head. Operations only writes ACTIVE/SUSPENDED. `ck_service_activation_time_order` checks `updated_at >= created_at`; it is not a monotonicity constraint across updates.

### Re-review bookkeeping correction

- Correction task: `pa32-aggregate-fix3-gpt-20261114-03`, November 14, 2026; gate: aggregate deepreview → re-review → bounded fix. Controller accepted the sole Low documentation-accuracy finding, `PA32-AGG-RR-L-01`.
- Independent re-review: `docs/reviews/code-review-20261114-073936.md`; SHA256: `35c21e050c218d619b648ec725ef2f11328d603f0433596c6c44ca9a5c6aab62`.
- Independent re-review: `docs/reviews/code-review-20261114-080727.md`; SHA256: `93fe1c8ed417f9d11df8cf166775a47b1365a7f68ba654ef448260ef34d1747c`.
- Corrected F-1 reachability to the positive-only JVM-ahead/DB-behind failure described above; negative skew is not reachable as a cross-path failure under the current production write topology. **F-1 deferred-with-owner** is unchanged, with Backend / Database / Platform / Operations retaining ownership of the follow-up activation-clock-hardening work unit before production readiness. Both re-reviews accepted the test fix and F-2 closure; neither is reopened.
- This correction changes documentation only. Control and gate-state bookkeeping remain for Controller; no production-readiness or additional runtime-validation claim is made.

## Consolidated Residual Owner/Destination Matrix

| Residual | Owner | Destination / disposition |
|---|---|---|
| Production employee verifier and Q1 acceptance | Backend / Security / Operations | Follow-up production readiness and security acceptance; not implemented in PA32. |
| `SET ROLE` and role provisioning | Database / Platform | PostgreSQL deployment and provisioning runbook; live acceptance remains outstanding. |
| PostgreSQL 16 deployment | Platform / Database | PostgreSQL 16 deployment validation and production readiness gate. |
| UTF-8 behavior and database/client encoding | Backend / Database / Platform | UTF-8 deployment and compatibility acceptance; not reopened in this fix. |
| Cross-path activation clock discipline: positive JVM-ahead/DB-behind skew can make the first Operations command reject a customer-written head timestamp; negative JVM-behind/DB-ahead skew is not a reachable cross-path failure under the current production write topology | Backend / Database / Platform / Operations | **F-1 deferred-with-owner.** Follow-up activation-clock-hardening work unit before production readiness. Preserve sanitized `503 SERVICE_UNAVAILABLE`, rollback, and no-partial-write behavior while deciding whether to anchor both writers to the database clock. |
| Audit diagnostic suppression | Backend / Security | Security acceptance and diagnostic-suppression runbook; primary failure and rollback semantics remain the contract. |
| Non-ASCII `Idempotency-Key` and header policy | Backend / API | API/security contract decision and acceptance artifact; policy remains unresolved outside this bounded fix. |
| Response-delivery-loss semantics | Backend / Security / Operations | Production runbook and security acceptance; define delivery-loss handling and operational reconciliation. |
| Hosted CI and live acceptance | Controller / Platform / Operations | Hosted CI run and live acceptance gate; not executed or claimed by this fix. |
| Global `operations_iam.admin_mutation` advisory throughput | Operations / Backend | Follow-up capacity/tuning work; deliberate global serialization is accepted for PA32. |
| Future Operations route allowlist | Backend | Future route-change checklist requiring the Operations two-POST filter allowlist and customer pass-through to change in lockstep. |
| PostgreSQL 16 / Hikari test brittleness and upgrade-sensitive assertions | Backend / Test / Platform | Upgrade-validation work; fail-closed test mechanics remain residual risk. |
| `RATE_LIMITED` / 429 with no producing throttler | Backend / API | Accepted PA32 boundary; future throttler/API contract work, not a defect in this slice. |

## Validation Boundary

This artifact records the bounded test and bookkeeping fix only. It makes no production-readiness, hosted-CI, live-acceptance, Docker, PostgreSQL, or external employee-verifier claim. The final completion report records the exact commands and outcomes available in the local backend shell.

### Corrected Local Validation — Aggregate Fix 2

- Validation task: `pa32-aggregate-fix2-gpt-20261114-02`, November 14, 2026; gate remains aggregate deepreview → fix.
- Corrected the Spring declared-API compilation error with an explicit `DefaultListableBeanFactory` cast. The real configuration bean path, zero/two-candidate cases, sanitized per-call failure, and lazy-resolution case remain intact. The test verifies Spring's `afterPropertiesSet` callback and clears that initialization interaction before retaining the per-call transaction/repository no-interaction assertion.
- The executable run also exposed a valid customer witness incorrectly listed as malformed; that case now supplies an invalid fingerprint. Operations-domain fixtures now have otherwise valid Operations actor/status fields so the wrong-fingerprint case reaches fingerprint validation. All existing failure/no-write assertions and the 8/18 test-method counts remain.
- The requested command without a startup agent compiled successfully but exited 1: 26 tests, 0 failures, 21 errors, 0 skipped, because Mockito could not self-attach in this Linux environment. An initial startup-agent retry exposed the two fixture issues above (26 tests, 2 failures, 0 errors, 0 skipped); neither failed run is green evidence.

Successful command, run from `backend` with no POM or dependency changes:

```sh
env JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/usr/lib/wsl/lib /mnt/d/tools/maven/bin/mvn -B -ntp '-Dmaven.repo.local=/mnt/d/tools/m2/repository' '-Dtest=OperationsLifecycleConfigurationTests,OperationsSubjectLifecycleServiceTests' '-DargLine=-javaagent:/mnt/d/tools/m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar' test
```

Final Maven exit: **0**, `BUILD SUCCESS`, finished `2026-11-14T07:02:19+08:00` (44.640 seconds).

| Surefire class | Methods executed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `com.onecall.archtests.OperationsLifecycleConfigurationTests` | 8 | 0 | 0 | 0 |
| `com.onecall.archtests.activation.OperationsSubjectLifecycleServiceTests` | 18 | 0 | 0 | 0 |
| Total | 26 | 0 | 0 | 0 |

The three allowed files pass conflict-marker, U+FFFD, and trailing-whitespace scans; `git diff --check` passes. Only the two targeted classes were executed: no Docker/container, full-suite, hosted-CI, or live-acceptance validation is claimed. Finding dispositions and residual owner/destination semantics are unchanged.
