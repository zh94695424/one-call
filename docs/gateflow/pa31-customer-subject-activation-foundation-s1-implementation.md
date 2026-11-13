# PA31 Customer Subject Activation Foundation — S1 Implementation Candidate

## 1. Identity, authority and status

- Task: `pa31-subject-activation-implementation-retry1-gpt-20261113-01`; date: 2026-11-13; gate: implementation retry 1; exactly one accepted slice S1.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa31-next-work-unit/codex-call`; branch: `codex/pa31-next-work-unit`; verified entry/current HEAD: `73da123d5ffe0438e12d48f38b4a057203243d95`.
- Runtime: supplied Codex CLI, Linux/WSL2/bash. Requested dispatch is Codex / GPT `gpt-6.1-sol` / high; exact executing model/provider metadata is not independently exposed and is not attested by the requested name. No delegated agents were spawned for this retry.
- Authority: the active PA31 Control checkpoint and accepted cumulative plan/fix-1/fix-2, with all six frozen Planreview artifacts retained. Controller dispositions REV-01..06 are preserved, not redesigned.
- Delivery: all 40 authorized implementation paths are present, plus this report only. The report was created after implementation, successful test compilation and focused feasible validation, not as a substitute for source/test delivery.
- Status after bounded fix-4: **Controller-accepted code-review A findings addressed; compilation and 84 focused unit/architecture methods pass; return the uncommitted candidate for Controller PostgreSQL16 rerun, without entering re-review or another gate**. Controller's clean full-suite/focused PG16 results before fix-4 are supplied baseline evidence, not this child's post-fix execution. See the fix-4 addendum and companion artifact. No commit, push, PR, merge, approval, ready action, reviewer request, external comment or issue mutation occurred.

### Fixed input hashes (raw SHA256, verified unchanged)

| Artifact | SHA256 |
| --- | --- |
| `docs/gateflow/pa31-customer-subject-activation-foundation-plan.md` | `6dde7211bda0c3a0e3af4c6b0d8d468d091787a6fa66c6d480c34e1c654d80b1` |
| `docs/gateflow/pa31-customer-subject-activation-foundation-plan-fix-1.md` | `5419ce0fd5b6373a80b7b0565e6f0cf2d92448c3d953fc161c7141207cf1af83` |
| `docs/gateflow/pa31-customer-subject-activation-foundation-plan-fix-2.md` | `585614481edb2a4ff094e4dca6b341bdad79b2854f2881aaa6e7a495b16f5b9d` |

The Git gitfile still points to its Windows worktree location. Read-only Linux verification used `GIT_OPTIONAL_LOCKS=0 git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call8 --work-tree="$PWD" ...`; Git metadata was not repaired, staged or committed. The exact current Control checkpoint/base above supersedes historical authoring/acceptance commit references embedded in the frozen documents.

## 2. Delivered behavior

| Goal | Candidate implementation and test evidence |
| --- | --- |
| G1 | Exact authenticated POST route; session-derived account/WEB-or-MINIAPP/workspace; structural context validation before input binding; active personal or authorized organization access reread under lifecycle locks. Web authentication/context identities, service access identities and IT lifecycle-wait/revocation identities implement this boundary. |
| G2 | Closed six-string-field binder, strict UTF-8/duplicate/trailing-token/length/UUID checks; one enabled/published FIRST-TRIAL policy read under entitlement role; bounded root-metadata parser and positive decimal version identity; exact prompt/content comparison, no fallback/seed/publisher. Policy parser unit methods and policy IT identities cover refusal cases. |
| G3 | One writable READ_COMMITTED REQUIRES_NEW attempt, MANDATORY local/entitlement scopes on the same manager/DSL/connection; ACCEPT consent/head/transition/permanent trial/Outbox atomicity. Absent head starts INACTIVE revision1 and becomes ACTIVE revision2. Unit atomicity/fault tests pass; PostgreSQL fault/concurrency tests are delivered but NOT RUN. |
| G4 | DECLINE writes only its immutable refusal consent with six-root-key descriptor and JSON-null activationResult, commits a typed outcome, then maps to 409 ACTIVATION_CONSENT_DECLINED. Exact refusal replay has no trial/Outbox/audit effects. Global event conflicts fail closed as CONSENT_REQUIRED after any necessary whole-attempt retry. |
| G5 | Six-field ACCEPT-only result with stored original trial units/expiry. Exact replay validates descriptor/hash/consent/anchor/trial identity and preserves event-time policy, revision, transition and outcome. New valid SUSPENDED ACCEPT is FORBIDDEN403 with zero writes; later ACTIVE event appends consent without another transition/trial. No expired/exhausted balance eligibility synthesis or repair. |
| G6 | Subject advisory → activation row → subject owner read → selected organization advisory → membership advisory → account advisory → locked rereads. Literal SET LOCAL ROLE/current_user assertions hand off to entitlement owner and finally restore activation role before local writes. Three attempts maximum for the exact SQLSTATE/named-constraint allowlist; rollback before reread; technical/commit failure is never success. |
| G7 | Only nine frozen operation errors; fixed safe messages; redacted command/caller/policy/trial/repository diagnostics, prepared SQL with execution logging disabled, no public descriptor/owner/fingerprint/credential fields. Unit redaction tests pass; IT HTTP/log canaries and descriptor/Outbox key allowlists are delivered, not runtime-proven here. |
| G8 | Exactly two named-interface dependency additions, dedicated explicit opt-in pool, exact 86 new test identities, 15 bounded existing updates, exact CI dictionaries/historical subtraction and POM includes. Frozen20, Control/plans/fixes/reviews and all 838 historical test identities remain unchanged. |

There are zero decision-audit calls on both decisions. V006 alone emits the new-trial Outbox. Activation emits only the frozen ACTIVATION_STATUS transition Outbox on a newly created transition; DECLINE, later ACTIVE and exact replay emit no new activation Outbox. No real-name/risk/provider/KMS/catalog/order/purchase/UI/IVR/Operations lifecycle feature, new grant/schema/migration or production provisioning is added.

The fixture uses migrated `postgres:16`, isolated random NOINHERIT login with exactly the two test-only SET membership edges, real owner beans, pool/manager/UTC preflights, observed backend/transaction identities inside both role scopes, finite advisory-lock-observed concurrency, commit/repository/function fault seams, immutable-trigger corruption matrices, log/HTTP canaries, restored guards and isolated role/data cleanup. These are **delivered test code**, not executed PostgreSQL evidence.

## 3. Exact 40-path implementation manifest

Paths below are repository-relative. All 25 additions were absent in the entry snapshot; all 15 existing updates are precisely plan §6 items1–15. The separately authorized report is not one of these40.

| # | Kind | Exact path |
| --- | --- | --- |
| 1 | New production | `backend/src/main/java/com/onecall/core/activation_consent/customeractivation/package-info.java` |
| 2 | New production | `backend/src/main/java/com/onecall/core/activation_consent/customeractivation/CustomerSubjectActivationService.java` |
| 3 | New production | `backend/src/main/java/com/onecall/core/activation_consent/customeractivation/SubjectActivationException.java` |
| 4 | New production | `backend/src/main/java/com/onecall/core/activation_consent/internal/command/DefaultCustomerSubjectActivationService.java` |
| 5 | New production | `backend/src/main/java/com/onecall/core/activation_consent/internal/command/SubjectActivationRepository.java` |
| 6 | New production | `backend/src/main/java/com/onecall/core/activation_consent/internal/command/JooqSubjectActivationRepository.java` |
| 7 | New production | `backend/src/main/java/com/onecall/core/activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java` |
| 8 | New production | `backend/src/main/java/com/onecall/core/activation_consent/internal/command/ActivationCommandPersistenceProperties.java` |
| 9 | New production | `backend/src/main/java/com/onecall/core/activation_consent/internal/command/ActivationSemantics.java` |
| 10 | New production | `backend/src/main/java/com/onecall/core/entitlement_metering/activationtrial/package-info.java` |
| 11 | New production | `backend/src/main/java/com/onecall/core/entitlement_metering/activationtrial/ActivationTrialService.java` |
| 12 | New production | `backend/src/main/java/com/onecall/core/entitlement_metering/internal/activation/DefaultActivationTrialService.java` |
| 13 | New production | `backend/src/main/java/com/onecall/core/entitlement_metering/internal/activation/ActivationTrialPolicySnapshot.java` |
| 14 | New production | `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectActivationCommandService.java` |
| 15 | New production | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationsController.java` |
| 16 | New production | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationJsonBinder.java` |
| 17 | New production | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationDtos.java` |
| 18 | New test/fixture | `backend/src/test/java/com/onecall/archtests/CustomerSubjectActivationsWebTests.java` |
| 19 | New test/fixture | `backend/src/test/java/com/onecall/archtests/activation/CustomerSubjectActivationServiceTests.java` |
| 20 | New test/fixture | `backend/src/test/java/com/onecall/archtests/entitlement/ActivationTrialServiceTests.java` |
| 21 | New test/fixture | `backend/src/test/java/com/onecall/archtests/CustomerSubjectActivationConfigurationTests.java` |
| 22 | New test/fixture | `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java` |
| 23 | New test/fixture | `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java` |
| 24 | New test/fixture | `backend/src/test/java/com/onecall/archtests/container/ActivationCommandRoleLeastPrivilegeIT.java` |
| 25 | New test/fixture | `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationFixture.java` |
| 26 | Existing update | `backend/src/main/java/com/onecall/core/activation_consent/package-info.java` |
| 27 | Existing update | `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` |
| 28 | Existing update | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` |
| 29 | Existing update | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java` |
| 30 | Existing update | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java` |
| 31 | Existing update | `backend/src/main/resources/application-persistence.yml` |
| 32 | Existing update | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` |
| 33 | Existing update | `backend/pom.xml` |
| 34 | Existing update | `.github/workflows/ci.yml` |
| 35 | Existing update | `backend/README.md` |
| 36 | Existing update | `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java` |
| 37 | Existing update | `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java` |
| 38 | Existing update | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` |
| 39 | Existing update | `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java` |
| 40 | Existing update | `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java` |

41st authorized path (this report): `docs/gateflow/pa31-customer-subject-activation-foundation-s1-implementation.md`.

## 4. Test identities and arithmetic

| Inventory | Entry | PA31 additions | Candidate source/CI target |
| --- | --- | --- | --- |
| Surefire | 54 classes / 468 methods | 5 / 52 | 59 / 520 |
| Failsafe | 23 classes / 370 methods | 2 / 34 | 25 / 404 |
| Combined | 77 / 838 | 7 / 86 | 84 / 924 |
| `@Test\b` line scan | 839 | 86 | 925, retaining the one historical PostgresContainerIT comment |
| POM Failsafe includes | 23 | 2 | 25 |

The 52 unit methods are `12+18+8+8+6`; the34 container methods are `24+10`. Fixture adds zero methods. Six existing architecture classes add zero identities and retain32 methods; their assertion-only updates include the exact seven-entry discovered-target multiset, retaining both evidence occurrences. All838 historical method identities/order were compared with the verified base; all86 new identities/order match the accepted plan exactly. CI has exact `pa31_unit_additions` / `pa31_container_additions`, subtracts PA31 from every older checkpoint and retains the dependency/firewall inventory boundaries.

### Focused execution XML (final candidate)

| Class | Tests | Failures | Errors | Skips |
| --- | --- | --- | --- | --- |
| `CustomerSubjectActivationsWebTests` | 12 | 0 | 0 | 0 |
| `CustomerSubjectActivationServiceTests` | 18 | 0 | 0 | 0 |
| `ActivationTrialServiceTests` | 8 | 0 | 0 | 0 |
| `CustomerSubjectActivationConfigurationTests` | 8 | 0 | 0 | 0 |
| `CustomerSubjectActivationBoundaryTests` | 6 | 0 | 0 | 0 |
| `ModuleBoundaryTests` | 9 | 0 | 0 | 0 |
| `CustomerBusinessNumberBoundaryTests` | 4 | 0 | 0 | 0 |
| `CustomerSubjectDetailBoundaryTests` | 4 | 0 | 0 | 0 |
| `SharingQueryBoundaryTests` | 8 | 0 | 0 | 0 |
| `CustomerEvidenceBoundaryTests` | 4 | 0 | 0 | 0 |
| `CustomerApiBoundaryTests` | 3 | 0 | 0 | 0 |
| **Total** | **84** | **0** | **0** | **0** |

This is52 new unit methods plus32 unchanged-identity architecture methods in11 classes. It is not the924-method full acceptance result. The broader default run discovered all59/520 source identities but ended with64 environment errors; it is explicitly a failed run below. No test was disabled, renamed, weakened, skipped, excluded from the configured default suite, or represented as passing because Docker/loopback was unavailable.

### Exact new identity checklist

Each unit identity below is implemented, compiled and passed in the final focused run. Each IT identity is implemented and compiled, but PostgreSQL execution is NOT RUN. The fixture is compiled and has no test identities.

#### `CustomerSubjectActivationsWebTests` (12)

- `acceptUsesExactRouteAndFrozenResultShape` — PASS.
- `activationUsesBodyEventWithoutIdempotencyHeader` — PASS.
- `verifiedSessionSuppliesAccountChannelAndWorkspace` — PASS.
- `authenticationPrecedesValidationAndPoolResolution` — PASS.
- `malformedTrustedContextFailsClosed` — PASS.
- `closedBodyRejectsUnknownDuplicateMissingAndWrongTypedFields` — PASS.
- `bodyRejectsMalformedUtf8TrailingJsonAndInvalidLengths` — PASS.
- `pathContentTypeAndDecisionValidationUseFrozenErrors` — PASS.
- `declineReturnsOnlyCommittedTypedProblem` — PASS.
- `domainFailuresUseOnlyOperationErrorSet` — PASS.
- `encodedAndUnrelatedRoutesRemainClosed` — PASS.
- `responsesProblemsAndLogsRedactInternalActivationFacts` — PASS.

#### `CustomerSubjectActivationServiceTests` (18)

- `personalAccessRequiresActiveAccountAndMatchingOwner` — PASS.
- `organizationAccessRequiresActiveWorkspaceMembershipAndAllowedRole` — PASS.
- `lifecycleFailurePrecedesNonDisclosingSubjectLookup` — PASS.
- `missingAndForeignSubjectsReturnNotFoundWithoutEffects` — PASS.
- `currentPolicyPromptAndContentMustMatchExactly` — PASS.
- `acceptCreatesOneRevisionTransitionAndIssuesTrialAtomically` — PASS.
- `declineCommitsRefusalBeforeExceptionMapping` — PASS.
- `exactAcceptReplayReturnsFirstFrozenResultWithoutWrites` — PASS.
- `exactDeclineReplayReturnsSameRefusalWithoutWrites` — PASS.
- `sameEventSemanticOrContentMismatchReturnsConsentRequired` — PASS.
- `laterActiveEventFreezesLatestAnchorAndOriginalTrial` — PASS.
- `suspendedRejectsNewAcceptButNeverMutatesOnHistoricalReplay` — PASS.
- `missingAnchorTrialOrCorruptReplayDescriptorFailsInternal` — PASS.
- `roleAndLockOrderIsExactBeforeRereadsAndWrites` — PASS.
- `canonicalFingerprintsAndSourceIdentitiesMatchFrozenVectors` — PASS.
- `trialOutboxAndCommitFailuresRespectExactDecisionWriteSets` — PASS.
- `retryableFailuresRestartWholeAttemptAndExhaustAtThree` — PASS.
- `unknownFailuresAndDomainStringsNeverEscapePublicProblems` — PASS.

#### `ActivationTrialServiceTests` (8)

- `snapshotRequiresExactRootConsentMetadataWithoutFallback` — PASS.
- `policyVersionUsesSamePositiveDecimalVersionIdentity` — PASS.
- `missingDisabledUnpublishedAndMalformedPoliciesFailClosed` — PASS.
- `mandatoryOwnerScopeRequiresExistingActivationTransaction` — PASS.
- `ownerSwitchesAssertsAndRestoresExactRoles` — PASS.
- `freshIssueLoadsStoredTrialGrantAndBalanceByReturnedIds` — PASS.
- `replayAndAlreadyGrantedReturnOriginalImmutableFacts` — PASS.
- `incompleteOrInconsistentTrialFactsFailWithoutSynthesis` — PASS.

#### `CustomerSubjectActivationConfigurationTests` (8)

- `disabledActivationCreatesNoPoolOrOwnerGraph` — PASS.
- `enabledActivationRequiresExplicitDedicatedCredentials` — PASS.
- `activationUsesQualifiedWritableReadCommittedTransaction` — PASS.
- `entitlementMandatoryTemplateUsesSameManagerAndDsl` — PASS.
- `ambiguousOrMissingOwnerGraphFailsClosed` — PASS.
- `propertiesAndInitializationFailuresRedactCredentials` — PASS.
- `webWithoutPersistenceRemainsAuthenticatedFailClosed` — PASS.
- `utcInitializationAndPoolClosePreserveLifecycle` — PASS.

#### `CustomerSubjectActivationBoundaryTests` (6)

- `customerUsesOnlyPublishedActivationCommandInterface` — PASS.
- `activationUsesOnlyPublishedEntitlementOwnerInterface` — PASS.
- `activationSqlNeverReadsEntitlementTablesOrLocksForeignRows` — PASS.
- `ownerHandoffHasMandatorySameConnectionAndLiteralRoles` — PASS.
- `openApiDataDocsAndAllSeventeenMigrationsRemainFrozen` — PASS.
- `commandSurfaceHasNoExtraEndpointPolicySourceOrStateTransition` — PASS.

#### `CustomerSubjectActivationsIT` (24)

- `personalAcceptCommitsExactActivationTrialAndResult` — compiled; PostgreSQL NOT RUN.
- `existingInactiveHeadAdvancesExactlyOneRevision` — compiled; PostgreSQL NOT RUN.
- `webAndMiniappUseTrustedChannelAndClosedRequest` — compiled; PostgreSQL NOT RUN.
- `organizationAllAllowedRolesCanActivateAccessibleSubject` — compiled; PostgreSQL NOT RUN.
- `inactiveOrganizationMembershipOrDisallowedRoleIsForbidden` — compiled; PostgreSQL NOT RUN.
- `inactiveAccountIsForbiddenBeforeMissingSubjectDisclosure` — compiled; PostgreSQL NOT RUN.
- `foreignPersonalAndOrganizationSubjectsAreNotFound` — compiled; PostgreSQL NOT RUN.
- `malformedContextAndMissingGraphAreServiceUnavailable` — compiled; PostgreSQL NOT RUN.
- `missingDisabledUnpublishedOrMalformedPolicyHasNoEffects` — compiled; PostgreSQL NOT RUN.
- `stalePolicyPromptOrContentHasNoConsentOrGrant` — compiled; PostgreSQL NOT RUN.
- `declineCommitsOneRefusalAndReplaysSameProblem` — compiled; PostgreSQL NOT RUN.
- `acceptReplayReturnsOriginalIdsOutcomeUnitsAndExpiry` — compiled; PostgreSQL NOT RUN.
- `sameEventSemanticMismatchHasNoAdditionalEffects` — compiled; PostgreSQL NOT RUN.
- `sameEventAcrossAccountsOrSubjectsFailsWithoutDisclosure` — compiled; PostgreSQL NOT RUN.
- `laterActiveEventUsesLatestAnchorWithoutTransitionOrTrialDuplication` — compiled; PostgreSQL NOT RUN.
- `changedPolicyNeverChangesPermanentTrialUnitsOrExpiry` — compiled; PostgreSQL NOT RUN.
- `suspendedCannotBeReactivatedByNewCustomerEvent` — compiled; PostgreSQL NOT RUN.
- `historicalReplayAfterPolicyChangeOrSuspensionHasNoMutation` — compiled; PostgreSQL NOT RUN.
- `concurrentSameEventAcceptsConvergeOnOneFrozenResult` — compiled; PostgreSQL NOT RUN.
- `concurrentDifferentEventsShareOneTransitionAndPermanentTrial` — compiled; PostgreSQL NOT RUN.
- `concurrentDeclineReplayAndSemanticConflictNeverAccept` — compiled; PostgreSQL NOT RUN.
- `trialOutboxAndCommitFaultsRespectExactDecisionWriteSets` — compiled; PostgreSQL NOT RUN.
- `policyDisableAroundFunctionSelectionHasFrozenLinearization` — compiled; PostgreSQL NOT RUN.
- `storedResultsProblemsAuditOutboxAndLogsAreRedacted` — compiled; PostgreSQL NOT RUN.

#### `ActivationCommandRoleLeastPrivilegeIT` (10)

- `syntheticLoginHasOnlyTwoNonInheritedSetMemberships` — compiled; PostgreSQL NOT RUN.
- `activationRoleCannotReadAnyEntitlementBaseTable` — compiled; PostgreSQL NOT RUN.
- `activationRoleHasExistingSchemaUsageAndIssueExecuteOnly` — compiled; PostgreSQL NOT RUN.
- `entitlementOwnerReadsCompletePolicyAndOriginalTrialFacts` — compiled; PostgreSQL NOT RUN.
- `handoffUsesOneBackendPidAndOneTransactionId` — compiled; PostgreSQL NOT RUN.
- `mandatoryCallsOutsideTransactionAndUnexpectedRoleAreRejected` — compiled; PostgreSQL NOT RUN.
- `activationRoleIsRestoredBeforeLocalWritesAndCompletion` — compiled; PostgreSQL NOT RUN.
- `rollbackAndPoolReuseCannotLeakEntitlementRole` — compiled; PostgreSQL NOT RUN.
- `noForeignWritesRowLocksOrExtraPrivilegesAreNeeded` — compiled; PostgreSQL NOT RUN.
- `fixtureCleanupAndFrozenAclInventoryRemainExact` — compiled; PostgreSQL NOT RUN.

#### `CustomerSubjectActivationFixture` (0)

- Fixture only; no test methods.

## 5. Commands, outcomes and corrections

Evidence files are outside the repository under `/tmp/pa31-retry1/`; they are local execution evidence, not committed artifacts or Controller acceptance. All Maven runs use installed JDK21 and Maven3.9.9; `backend/mvnw` CRLF was not modified. Node `v22.23.4` matches actual CI; npm10.9.9; Java Temurin21.0.13+9.

Reproducible final Maven commands, from `backend`:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:$PATH
/opt/maven-3.9.9/bin/mvn -B -ntp test-compile
/opt/maven-3.9.9/bin/mvn -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dtest=CustomerSubjectActivationsWebTests,CustomerSubjectActivationServiceTests,ActivationTrialServiceTests,CustomerSubjectActivationConfigurationTests,CustomerSubjectActivationBoundaryTests,ModuleBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests,CustomerEvidenceBoundaryTests,CustomerApiBoundaryTests test
```

The explicit startup Mockito agent works around sandbox-denied dynamic self-attach without adding a dependency, modifying the POM plugin configuration, changing assertions or enabling a product bypass.

| Command / evidence log | Exit | Actual outcome |
| --- | --- | --- |
| `mvn -f backend/pom.xml ... test-compile` from root; `test-compile-first.log` | 0 | Initial compile. Its generated root build directory was subsequently removed. |
| Maven `test-compile` from backend; `test-compile-units.log`, `test-compile-final.log`, `test-compile-candidate.log` | 0 each | New production and all test/fixture source compile, including both IT classes. Final focused candidate lifecycle recompiled all118 test source files after the last IT edits. |
| Initial focused run; `focused-first.log` | 1 | 84 tests, zero failures,25 errors: Mockito self-attach denial and encoded MockMvc URI setup. Explicit startup agent and URI.create fix address those causes. |
| Intermediate focused run; `focused-second.log` | 1 | Test compilation caught Object-to-String misuse in fixture identity SQL. Corrected typed jOOQ record access throughout role checks/fixture. |
| Intermediate focused run; `focused-third.log` | 1 | 84 tests,1 failure,3 errors. Corrected overlapping Mockito stubbing with doThrow and fake grant records missing nullable order/installment fields. Assertions were not weakened. |
| Focused reruns; `focused-fourth.log`, `focused-final.log`, `focused-candidate.log` | 0 each | Each84/0/0/0. The last recompiles latest source and passes the required52+32 methods; final XML independently summed. |
| Full configured default `mvn ... -DargLine=... test`; `default-unit-final.log` | 1 | 520 tests,0 failures,64 errors,0 skips:456 passed. Embedded Tomcat cannot bind sockets: `java.net.SocketException: Operation not permitted`. This is **not** a full-suite pass. |
| `docker info`; `docker-info.log` | 1 | Client29.1.3 present; server inaccessible: `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`. PostgreSQL IT not launched and no skip/pass invented. |
| `npm --prefix api/openapi ci --offline`; `npm-ci.log` | 0 |265 cached packages installed; zero audit vulnerabilities reported. No package/lock source changes. |
| `npm --prefix api/openapi run check:s3`; `openapi-check.log` | 0 | OpenAPI lint/client generation/type check/doc maps/media-gateway lint pass;49 operations,18/21/6/3/1 surfaces, zero missing/extra/duplicate doc rows. Its two generated tmp client outputs were removed afterward. |
| Extracted checked-in CI source/inventory Python AST, XML-dependent nodes excluded; `ci-source-only-final.log` | 0 | Unchanged source assertions and historical arithmetic pass:59/25 classes,520/404 methods,925 annotation lines,POM25. This is **source/CI static-only**, not source=CI=XML execution. |
| `bash -n /tmp/pa31-retry1/ci-inventory.sh` | 0 | Extracted inventory run-block shell syntax valid. |
| `python3 /tmp/pa31-retry1/audit.py`; `audit-final.log` | 0 | Exact40 path boundary,17/15/7+fixture,86 new identities,838 preserved identities,84/924/925, fixed hashes/frozen20/migration set all pass. |
| Explicit read-only Git `diff --check`; `diff-check.log` | 0 | No whitespace errors. Expected Git line-ending notices do not replace raw custody checks. |

The64 default-suite errors are confined to existing socket-starting tests: CustomerBusinessNumbersWebTests12, CustomerEvidenceConfigurationTests4, CustomerEvidenceWebTests12, CustomerMeWebTests8, CustomerSharesWebTests10, CustomerSubjectDetailConfigurationTests2, CustomerSubjectsWebTests12, CustomerWebConfigurationTests3, MediaGatewayConfigurationTests1. Their XML is preserved in `/tmp/pa31-retry1/default-unit-xml` before focused reruns. Spring's context-failure-threshold errors derive from the same denied socket startup; no product regression is asserted to be resolved by skipping them.

Additional command/setup failures were corrected, not hidden: one Maven command from the repository root without `-f` failed with no POM; native Git's Windows gitfile needed the read-only mapping above; initial full CI extractor failed because PostgresContainerIT XML does not exist; one static-only rerun used root instead of backend and failed FileNotFoundError before succeeding in backend; the first raw-scope audit found the two generated OpenAPI client outputs and passed after their removal. One patch attempt used a redundant backend prefix from backend and made no edit; it was reapplied at the correct path. Final source compile/focused/static results above supersede these setup failures. No test/contract was weakened to obtain them.

## 6. Frozen custody and scope evidence

Entry snapshot:1913 raw files excluding generated target/node_modules/Git internals. `/tmp/pa31-retry1/entry.json` SHA256 `f312cb063c1df885afd322085bc8402df0143ba00b836a6a9100ba61471cbe7b`; original frozen20 manifest `/tmp/pa31-retry1/frozen.json` SHA256 `de3f4a66bfae002efdb179cda62031606db8993f98b2663c48e4fcb49207ac82`. The pre-existing private `.codex` file remains untouched and is not a task addition.

Raw entry/current comparison finds precisely15 modified existing paths and25 new implementation paths; this report is the sole26th new authorized document. Generated Maven target/backend build and npm node_modules are not source changes. Temporary generated root build/OpenAPI client outputs were removed. Control, accepted plan/fix-1/fix-2, all six reviews and all other entry files are byte-identical; all17 migration filenames remain exactly V001–V017; noV018. Architecture tests embed the original frozen raw/LF oracle copied from CustomerSubjectClaimBoundaryTests, not hashes recomputed from edited contract files.

### Frozen20 raw SHA256 (unchanged)

| Path | SHA256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |
| `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql` | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` |
| `backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` |
| `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` |
| `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql` | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` |
| `backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql` | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` |
| `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` |
| `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql` | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` |

## 7. NOT RUN gates and residual ownership

| Gate / residual | Child status and reason | Owner / destination |
| --- | --- | --- |
| Two focused PostgreSQL16 IT classes /34 methods | **NOT RUN**; Docker socket permission denied. Delivered and test-compiled, not passed. No accepted SQL/ACL/PID/transaction/concurrency/rollback/Outbox runtime result is claimed. | Controller: run focused container-smoke verify in an environment with Docker and loopback access. |
| Full25 Failsafe classes/404 methods and full clean container924-method suite | **NOT RUN**; Docker unavailable. Broader child default-only attempt also fails64 loopback-startup errors. | Controller/runtime: independent full clean verify with permitted socket/container startup. |
| Full source=CI=XML including zero-skip Failsafe identities | **NOT RUN**; no real Failsafe XML. Static source/CI arithmetic passed, not an XML replacement. | Controller: run actual unchanged checked-in inventory block after the full suite. |
| Packaged default/web JAR smokes | **NOT RUN**; independent Controller gate; child socket denial already prevents truthful web startup. | Controller: execute actual checked-in JAR run block, including activation probes. |
| Dependency/JAR negative firewall | **NOT RUN**; independent Controller packaged-artifact gate. Existing72 resolved coordinates/65 packaged JARs/116 negative probes remain expected, not observed results. | Controller: execute checked-in firewall block and verify exact inventories. |
| OpenAPI check:s3 | **PASS in child** as recorded above; frozen bytes unchanged. | Controller independently repeats mandatory contract/custody checks. |
| Real FIRST-TRIAL snapshot metadata/publication | No production policy publication, seed or fallback; missing metadata fails CONSENT_REQUIRED. | Operations/product policy owner; external publication prerequisite, not a new PA31 feature. |
| Dedicated dual-role login/credentials/TLS/pg_hba/rotation/pool drain | No production credentials/provisioning supplied or changed; synthetic edges exist only in disposable fixture setup. | Platform/Database/Security/Operations deployment input. |
| Corrupt descriptor/anchor/original trial | Fail INTERNAL; no inferred fields, reconciliation or repair. | Database/Operations incident destination. |
| Production verifier/provider/KMS/UI/IVR/Operations lifecycle/live acceptance | **NOT EXECUTED / NOT DELIVERED / NOT CLAIMED**. | Separately authorized future owners/gates; outside S1. |

## Fix-2 corrected diagnosis and status

Task `pa31-subject-activation-implementation-fix2-gpt-20261113-01` independently rejects fix-1's ACL diagnosis: frozen V004 already grants the activation role platform schema usage and Outbox execution; V014 does not remove that grant. No ACL/schema change is justified or made. The historical fix-1 artifact is preserved unchanged, but its diagnosis and authorization request are superseded by this correction.

The preliminary fresh-GRANTED predicate hypothesis is also rejected: for a valid fresh trial, `sameAction=true` and `ALREADY_GRANTED=false`, so their equality is false and does not reject the result. Existing successful fresh ACCEPT/replay/later-event unit cases and the strengthened six-combination owner outcome/action matrix agree. Same-action GRANTED/REPLAY still require the original policy/fingerprint; ALREADY_GRANTED requires a different action. Both owner predicates and the same-connection role handoff remain unchanged.

A wrapped SQL exception mapping defect is reproduced independently: the strengthened existing failure test returns INTERNAL instead of CONSENT_REQUIRED before repair (one method, one failure, no errors/skips). The implementation now selects the deepest bounded SQL cause and recognizes only exact allowlisted P0001 trial-policy/action diagnostics, including a diagnostic line after a wrapper prefix. Unknown/integrity errors remain INTERNAL and infrastructure SQL states remain SERVICE_UNAVAILABLE. Public errors still have a constant sanitized message and no cause; no diagnostic logging was added.

Final child `test-compile` exits 0; focused unit/architecture validation exits 0 with exactly 84 methods, 0 failures, 0 errors, 0 skips. The exact requested PostgreSQL16 verify command exits 1 in Surefire (520 methods, 5 failures, 273 errors, 0 skips; denied Mockito attachment and socket-dependent startup failures), before Failsafe. A supplementary direct focused Failsafe invocation exits 1 because Docker access is denied: its two XML entries are class-initialization errors (2 entries, 0 failures, 2 errors, 0 skips), not the required 34 executed methods. The earlier Controller 34/5/15/0 result is historical, not a candidate rerun.

Consequently the real underlying shared PostgreSQL ACCEPT exception cannot be independently extracted or reproduced in this sandbox, and no claim is made that the mapping repair resolves all ACCEPT failures or the Controller policy race. No speculative predicate, grant or migration repair is made. Static inventory/path/frozen checks preserve the 40 implementation paths, all test identities, 84 classes/924 methods/925 annotation lines/POM25 and frozen20/no V018. Full command details, boundaries and remaining runtime blocker are in `docs/gateflow/pa31-customer-subject-activation-foundation-s1-fix-2.md`. Only Controller diagnostic/PostgreSQL work remains requested; no review or later gate is entered.

## Bounded fix-3 addendum — 2026-11-13

Task `pa31-subject-activation-implementation-fix3-gpt-20261113-01` receives the Controller's actual PostgreSQL/JFR diagnostic: first ACCEPT fails at `createHead` because a varchar expression targets `created_at timestamptz`. This supersedes fix-2's unknown shared-exception limitation, not its accepted-as-evidence mapping repair. Frozen V007 independently confirms the three target columns are timestamptz. Existing source and compiled bytecode show uncast OffsetDateTime arguments at head insert positions 3/4, head update position 1 and transition insert position 18.

An external compiled probe invokes the real repository through jOOQ's JDBC MockConnection, independently observing String bindings at all four positions before repair. After final compilation it verifies the same UTC instant, complete 4/4/18 argument arrays/order, successful update row-count handling and exactly four explicit casts. This is JDBC-mock evidence, not PostgreSQL execution and not an additional repository test identity. The only production change adds `?::timestamptz` at these four positions in the three existing SQL statements; values, parameter order, role assertions and every other production byte remain unchanged relative to entry.

Final `test-compile` exits 0. The requested explicit-agent 11-class selection exits 0: 84 methods (52 new +32 existing), zero failures/errors/skips. Static CI/source audit exits 0: 59 unit classes/520 methods +25 container classes/404 methods =84/924, annotation925/POM25. Exact entry/path/frozen audit preserves cumulative40 (15 existing +25 new), all test source bytes and 86 new/838 historical identities, frozen20, V001–V017/no V018, accepted plan/fixes/reviews/Control and unrelated/private files. Fix-3 changes only the repository, this report and its single new companion artifact.

Docker CLI is installed but daemon access is denied on `/var/run/docker.sock`. The supplementary direct Failsafe attempt exits 1 with exactly two class-initialization errors, zero failures/skips; **the 34 PostgreSQL16 methods did not execute**. This is a runtime limitation, not a product-test verdict. Controller must execute those exact 34 methods on the final candidate, including `policyDisableAroundFunctionSelectionHasFrozenLinearization`.

Policy disposition: no additional mapping edit is justified by actual post-repair database evidence, which is unavailable here. The service performs head/transition writes before the policy-disable test's issuance hook, so the known timestamp defect can prevent that hook from being reached; this explains a possible shared obstruction, not a measured policy-test exception or pass. Preserve fix-2's bounded P0001 exact allowlist: TRIAL_POLICY_NOT_FOUND/TRIAL_ACTION_CONFLICT → CONSENT_REQUIRED, unrelated integrity/unknown errors → INTERNAL and established infrastructure states → SERVICE_UNAVAILABLE. Its existing nested-exception/negative-control unit matrices pass, but cannot substitute for the real policy-disable PG16 result.

Detailed commands, bytecode/JDBC evidence, path audit and residuals are in `docs/gateflow/pa31-customer-subject-activation-foundation-s1-fix-3.md`; execution evidence is outside the repository at `/tmp/pa31-fix3/evidence/`. Return only this uncommitted fix candidate to Controller; no commit/push/PR/merge/approval/mark-ready/reviewer request/external comment/issue mutation/code review/deepreview or later-gate action occurred. Full container/JAR/firewall/live acceptance is not claimed.

## Bounded fix-4 addendum — 2026-11-13

Task `pa31-subject-activation-implementation-fix4-gpt-20261113-01` closes only Controller-accepted `PA31-CR-A-01..03` from `docs/reviews/code-review-20261113-150800.md`; B's `code-review-20261113-161810.md` records no findings. The supplied Controller baseline is clean PostgreSQL16 59/520 Surefire +25/404 Failsafe =84/924, focused34/0/0/0, source=CI=XML, JAR/web smoke, 116 dependency probes, OpenAPI `check:s3`, frozen20/exact40. Those are prior Controller results, not this child's post-fix runs.

- `PA31-CR-A-01` — **已修复**: all four allowed organization roles now activate through real HTTP/trusted session/controller/command service in the existing IT method. The reset fixture seeds a second organization; the existing foreign-subject method exercises organization X against organization Y's subject directly and over HTTP, checking non-disclosing 404/frozen five-key problem/no-store/nosniff and all eight zero-write counters. The existing verified-session web method also proves selected organization propagation despite spoofed request metadata.
- `PA31-CR-A-02` — **已修复**: existing web and real-HTTP IT success methods assert exact six/two keys, ACTIVE, positive integral revision/seconds, canonical parseable UUID strings, fresh GRANTED and string expiry round-tripping through Instant.parse.
- `PA31-CR-A-03` — **已修复**: only this controller gains an 8192-byte limit using readNBytes(8193), rejecting overflow with existing VALIDATION_FAILED/422 before JSON binding/activate. Existing web validation covers declared and unknown-length overflow, zero owner-command/caller invocations, and the exact accepted boundary. Existing HTTP IT coverage sends both known-length and actual unknown-length/chunked publishers with zero writes and frozen problem headers.

This fix changes exactly four implementation paths (controller, web tests, activation fixture, activation IT), this S1 report and the new fix-4 artifact. Final test-compile exits0; explicit Mockito-agent selection passes11 classes/84 methods, failures0/errors0/skips0. Static CI/source remains59/520 +25/404 =84/924, annotation925/POM25, new52+34=86 and historical838 identities unchanged. Entry/raw-path audit preserves cumulative40, frozen20/V001–V017/noV018, all protected inputs and Git branch/HEAD/index. No migration/grant/dependency/OpenAPI/CI/POM or owner/transaction/lock/replay/DECLINE/SUSPENDED change is made.

Docker version exits1 with daemon socket permission denied; focused PostgreSQL tests are **NOT RUN** here. Controller must rerun focused34 and full PG16/inventory acceptance on this final candidate, particularly the expanded organization, foreign-subject and oversized/chunked HTTP matrices. Full commands, exact paths, custody and residuals are recorded in `docs/gateflow/pa31-customer-subject-activation-foundation-s1-fix-4.md`; local evidence is `/tmp/pa31-fix4/evidence/`. No re-review or later gate is entered.
