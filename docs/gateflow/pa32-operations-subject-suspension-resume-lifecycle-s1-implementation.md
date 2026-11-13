# PA32 slice 1 implementation — Controller intake

## Identity, authority and status

- Task: `pa32-s1-impl-gpt-20261113-01`; gate: **implementation**; local date: 2026-11-13, Asia/Shanghai.
- Runtime: Codex CLI / requested GPT `gpt-6.1-sol`, reasoning `high`. The runner does not expose independently attestable underlying model identity; the requested routing is provenance, not an independently verified model claim.
- Goal Confirmation and accepted Controller checkpoint were read before implementation. Binding plan: `docs/gateflow/pa32-operations-subject-suspension-resume-lifecycle-plan.md`, SHA256 `9d10a0ca6ecf3ce14cb50b45ea652b3f38ae1455279cadd30c266c0a0969c0de`.
- Inspected product base: `8206f2a8f8a40cd58793efdda55aa41843af5d5f`; accepted plan commit: `da2f743a221c581df80714c5db4b8dd0aa835238`; implementation starting/current HEAD: `d12e53493f12f3a61991c7fe13be77a5323fab86` (`gateflow: record PA32 accepted plan checkpoint`). Accepted-plan commit is an ancestor of HEAD. No commit/index/branch/remote operation was performed.
- Initial explicit-path Git status contained only preexisting untracked `.codex`. Unlike the historical plan-authoring state, Control was not initially modified in this invocation. `.codex` was not inspected, followed, staged, removed or changed.
- **Status: implementation candidate delivered for Controller intake; runtime acceptance BLOCKED, not a focused/full validation pass.** All production and test sources compile; all 54 new unit methods pass. PostgreSQL16 execution, full acceptance XML equality and live web JAR probes are not established. Do not treat this artifact as review, fix, deepreview, PR, final closeout or production readiness.

## Exact change inventory

Exactly **36 implementation paths: 22 new production +9 new test/fixture +5 existing changes**, plus this one report. No additional production/test/configuration path was needed. Source/test/CI membership is checked against accepted plan §4, not inferred from directory counts.

### New production files (under backend/src/main/java/com/onecall/core)

- `operations_iam/subjectlifecycle/package-info.java` — Publishes the exact accepted Spring Modulith named interface.
- `operations_iam/subjectlifecycle/OperationsSubjectLifecycleService.java` — Published two-method inbound port, trusted caller/command/result records and sanitized typed failures.
- `operations_iam/employeelock/package-info.java` — Publishes the exact accepted Spring Modulith named interface.
- `operations_iam/employeelock/EmployeeAdministratorLock.java` — Published lock-only collaborator, current employee facts and UNAVAILABLE/INTEGRITY failure channel.
- `operations_iam/internal/security/EmployeeSessionVerifier.java` — Independent pluggable verifier interface; deliberately no production implementation.
- `operations_iam/internal/security/EmployeeSessionVerificationException.java` — Redacted typed verification outcomes.
- `operations_iam/internal/security/OpaqueEmployeeBearer.java` — Secret wrapper with explicit verifier accessor and redacted representation.
- `operations_iam/internal/security/VerifiedEmployeeSession.java` — Immutable employee identity/audience attestation without stale role authority.
- `operations_iam/internal/security/EmployeeBearerAuthenticator.java` — Strict one-header opaque bearer extraction, independent audience and lazy unique-verifier selection.
- `operations_iam/internal/web/OperationsWebConfiguration.java` — Web-only REQUEST filter registration at HIGHEST_PRECEDENCE and problem writer.
- `operations_iam/internal/web/OperationsBoundaryFilter.java` — Exact two-POST firewall, authentication/input precedence, transport checks, request-local principal and buffered sanitized errors.
- `operations_iam/internal/web/OperationsRequestPrincipal.java` — Request-local trusted employee caller, no retained bearer.
- `operations_iam/internal/web/OperationsProblemWriter.java` — Frozen eight-code status/title mapping, five-field problems and no-store security headers.
- `operations_iam/internal/web/OperationsSubjectsController.java` — Only two frozen POST routes; closed input validation before unique qualified owner resolution.
- `operations_iam/internal/web/OperationsSubjectLifecycleJsonBinder.java` — Strict duplicate/trailing/UTF-8/closed JSON input, code-point bounds and integral BigInteger revision.
- `operations_iam/internal/employeelock/DefaultEmployeeAdministratorLock.java` — Operations-owned named persistence component; MANDATORY same-resource administrator/employee locks and role restoration.
- `activation_consent/internal/operations/OperationsLifecyclePersistenceProperties.java` — Dedicated opt-in datasource properties with nonblank credential checks and redacted representation.
- `activation_consent/internal/operations/OperationsLifecyclePersistenceConfiguration.java` — Dedicated conditional pool/manager/DSL/template/repository/owner wiring; no default credentials or provisioning.
- `activation_consent/internal/operations/OperationsLifecycleRepository.java` — Owner repository seam and internal head/ledger records.
- `activation_consent/internal/operations/JooqOperationsLifecycleRepository.java` — Prepared owner SQL, role assertions, database timestamp, transition/Outbox append and scoped audit capability.
- `activation_consent/internal/operations/DefaultOperationsSubjectLifecycleService.java` — REQUIRES_NEW owner decision, lazy employee collaborator resolution, authorization, replay and conditional transition ordering.
- `activation_consent/internal/operations/OperationsLifecycleSemantics.java` — Frozen eleven-field fingerprint, historical replay comparison, persisted head/transition validation and exact result projection.

### New tests and fixtures (under backend/src/test/java)

- `com/onecall/core/operations_iam/internal/security/EmployeeSessionBoundaryTests.java` — 8 ordinary test methods; names below.
- `com/onecall/archtests/OperationsSubjectsWebTests.java` — 12 ordinary test methods; names below.
- `com/onecall/archtests/activation/OperationsSubjectLifecycleServiceTests.java` — 18 ordinary test methods; names below.
- `com/onecall/archtests/OperationsLifecycleConfigurationTests.java` — 8 ordinary test methods; names below.
- `com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java` — 8 ordinary test methods; names below.
- `com/onecall/archtests/container/OperationsSubjectLifecycleIT.java` — 24 ordinary test methods; names below.
- `com/onecall/archtests/container/OperationsLifecycleRoleLeastPrivilegeIT.java` — 12 ordinary test methods; names below.
- `com/onecall/archtests/container/OperationsSubjectLifecycleFixture.java` — Test-only helper with zero test annotations; not a Failsafe include.
- `com/onecall/archtests/support/SyntheticEmployeeSessions.java` — Test-only helper with zero test annotations; not a Failsafe include.

### Existing paths changed

- `backend/src/main/java/com/onecall/core/activation_consent/package-info.java` — Append only the two accepted Operations named dependencies.
- `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` — Early canonical two-POST Operations pass-through before customer headers/authentication, with no employee imports.
- `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` — Exact activation dependency set/eight-edge assertion only; retain all nine methods and historical negative fixtures.
- `backend/pom.xml` — Exactly two additional Failsafe includes; no dependency/plugin/version changes.
- `.github/workflows/ci.yml` — Exact PA32 method inventories, historical subtraction arithmetic, both endpoint JAR probes and direct-interface employee verifier firewall.

## Key implementation evidence

- HTTP routes are exactly POST `/api/v1/operations/subjects/{subjectId}/suspensions` and `/resumptions`. The employee filter precedes the unchanged customer filter registration; only canonical admitted Operations POSTs bypass customer authentication. Wrong method/route is concealed before authentication; identity verification precedes headers/body/media validation. Strict binder accepts only `reasonCode` and integral positive `expectedActivationRevision`, enforces 8192 bytes and Unicode bounds, and preserves reason/action text.
- Independent verifier selection uses per-authentication `stream().limit(2)`, exact audience `onecall:employee-session:v1` and no production fallback. Missing/malformed credentials are401; unavailable/malformed/ambiguous provider context503; a present wrong audience401. Synthetic provider is only test source and the JAR direct-interface firewall passes.
- Activation imports only published `operations_iam.subjectlifecycle` and `operations_iam.employeelock` types. Operations imports no activation internals. Modulith's nine retained module tests and cycle/negative fixtures pass with the exact eight-target multiset.
- Owner construction does not resolve the unqualified-by-name employee-lock provider. Zero/two/lazy-uncreatable candidates are resolved before transaction/locks into sanitized503. Configuration tests establish startup with zero/two candidates, runtime failure and exact normal component name/qualifiers. Both typed employee-lock kinds map503; unknown programming defects outside that contract map500.
- Authoritative transaction uses the dedicated pool, transaction-aware DSL, REQUIRES_NEW/READ_COMMITTED/writable template and rollback-on-commit-failure. Service asserts a real transaction/bound pool. Order is subject advisory → activation FOR UPDATE → granted nonlocking subject-id read → Operations administrator advisory and employee FOR UPDATE → conditional owner revision update → transition → Outbox → audit. Current matching ACTIVE OPS facts are required before existence/replay/state results; missing/disabled employee401, ACTIVE ADMIN403, absent subject/head404. No tenant/customer ownership filter restricts a valid Operations employee; subject existence remains authoritative.
- Employee collaborator asserts entry activation role, enters Operations IAM role on the same manager/DSL, reads only its own employee row and finally restores/asserts activation role; restoration failure marks rollback-only. Audit independently scopes operations_service_role and calls only existing `audit.append_fact`, finally restoring activation role; outer transaction marks rollback-only on failure. No new grant, production provisioning, foreign base-table mutation or network verification under locks.
- Global `(OPERATIONS, exact actionEventId)` ledger lookup precedes current-state predicate. Stored immutable status identity is validated and used in historical replay fingerprint reconstruction; expected revision is separately compared to stored from_revision. Same action changed subject/actor/route/reason/revision409. Structurally unusable persisted data503. New action requires fixed ACTIVE→SUSPENDED or SUSPENDED→ACTIVE pair, matching revision and valid latest witness; max integer/revision mismatch409. Conditional update count0 causes exactly one bounded reread and conflict; count>1 fails503.
- Fingerprint is canonical lowercase SHA256 over exactly eleven frozen keys, including null accountId/consentRequestFingerprint, with no extra expectedRevision/trace/time. Status identity is `activation-status:<activation UUID>:<nextRevision>`. First success uses one DB decision time for head, ledger, audit and Outbox payload. One explicit eighteen-column immutable transition, thirteen-field activation-status Outbox and one audit append share transaction. Audit uses exact three-key before/after status/revision/reasonCode snapshots, request_id=statusEventId and fresh server trace. No new outbox workflow/idempotency store.
- Resume service has no entitlement/trial/consent collaborator and no `issue_first_trial` path. Fixture snapshots full rows across all existing business schemas except the three authorized fact stores/head; trial-bearing and no-trial activations are represented. These assertions compile but DB proofs remain unexecuted.
- SQL failure translation runs outside transaction.execute after cleanup. The three accepted unique constraint races map409; technical SQL/permission/connection/transaction failures503. No generic retry, savepoint success, raw SQL/cause serialization, production verifier, new policy, schema, migration or dependency was introduced.

## Exact test inventory and arithmetic

The following are exactly accepted §8.1–8.7, ordinary one-annotation methods; no new nested test containers, parameterized/dynamic/repeated tests, disabled methods or annotated helpers. Static comparison confirms ordered method names against the plan and set equality against literal CI inventories.

### EmployeeSessionBoundaryTests — 8

- `missingMalformedAndDuplicateBearerAreUnauthenticated`
- `employeeAudienceIsPassedAndIndependentlyValidated`
- `customerTokensAndClientIdentityNeverAuthenticateEmployee`
- `absentAndAmbiguousVerifierFailClosed`
- `unusableVerifierAndMalformedAttestationFailClosed`
- `invalidExpiredAndRevokedSessionAreUnauthenticated`
- `opaqueValuesAndProviderFailuresHaveRedactedRepresentations`
- `concurrentSessionsNeverShareTrustedCallerState`

### OperationsSubjectsWebTests — 12

- `exactTwoPostsReturnOnlyTenFrozenFields`
- `wrongMethodAndNoncanonicalRoutesAreConcealed`
- `authenticationPrecedesMalformedInputAndMediaNegotiation`
- `closedBodyRejectsUnknownDuplicateMissingAndCoercedFields`
- `reasonAndRevisionBoundsPreserveFrozenInputSemantics`
- `idempotencyHeaderBoundsAndExactOpaqueTextArePreserved`
- `uuidAndContentNegotiationUseFrozenProblemSet`
- `wrongRoleAudienceAndUnavailableDependenciesMapExactly`
- `successReplayAndConflictHaveIdenticalFrozenProjectionRules`
- `problemsAndResponsesNeverLeakSensitiveCanaries`
- `operationsAndCustomerFiltersNeverCrossAuthenticate`
- `concurrentRequestsKeepCallerActionAndReasonIsolated`

### OperationsSubjectLifecycleServiceTests — 18

- `suspendAdvancesOnlyActiveHeadAndAppendsOneFactSet`
- `resumeAdvancesOnlySuspendedHeadAndAppendsOneFactSet`
- `currentActiveOpsIsRequiredBeforeExistenceOrReplay`
- `missingSubjectOrActivationReturnsNotFoundWithoutCreation`
- `wrongStateAndStaleRevisionConflictWithoutWrites`
- `exactActionReplayReturnsOriginalBeforeCurrentPredicate`
- `changedActorSubjectRouteReasonOrExpectedRevisionConflicts`
- `frozenFingerprintHasExactKeysNullsAndCanonicalUtf8`
- `maximumActionIdentityIsStoredWithoutTruncation`
- `statusIdentityUsesActivationAndNextRevisionWithoutOverflow`
- `lockOrderAndOwnerAssertionsPrecedeEveryLocalWrite`
- `unusableHeadTransitionOrEmployeeContextFailsClosed`
- `conditionalUpdateMissRereadsOnceAndConflicts`
- `auditAndOutboxMappingsAreExactAndContainNoSecrets`
- `auditOutboxAndTransitionFailuresRollBackWholeDecision`
- `resumeHasNoTrialConsentOrForeignMutationCollaborator`
- `uniqueAndTechnicalFailuresMapOnlyAfterTransactionCleanup`
- `replayAfterLaterTransitionReturnsFirstImmutableResult`

### OperationsLifecycleConfigurationTests — 8

- `defaultWebAndDisabledProfilesCreateNoLifecyclePool`
- `explicitEnablementRequiresDedicatedNonblankCredentials`
- `ownerBeansUseExactQualifiersAndOneDatasource`
- `writeTemplateUsesRequiresNewReadCommittedAndRollbackOnCommitFailure`
- `employeeCollaboratorIsOperationsOwnedAndUsesExactQualifiedManagerAndDsl`
- `missingAndAmbiguousOwnerCollaboratorsFailClosed`
- `webBoundaryUsesIndependentVerifierAndExactFilterOrder`
- `configurationAndStartupFailuresAreRedacted`

### OperationsLifecycleBoundaryTests — 8

- `operationsPortsAndNamedInterfacesAreExactAndAcyclic`
- `activationImportsOnlyPublishedOperationsInterfaces`
- `employeeSessionIsIndependentAndHasNoProductionVerifierImplementation`
- `employeeOwnerSqlIsLockOnlyAndUsesMandatoryLiteralRoles`
- `lifecycleSqlHasNoForeignBaseWritesOrTrialCalls`
- `routesBinderProblemsAndCustomerPassThroughStayExact`
- `frozenOpenApiMigrationsAndPolicyBytesAreUnchanged`
- `auditOutboxFingerprintAndResponseFieldSetsAreExact`

### OperationsSubjectLifecycleIT — 24

- `suspendActiveSubjectCommitsOneRevisionTransitionOutboxAndAudit`
- `resumeSuspendedSubjectCommitsOneRevisionTransitionOutboxAndAudit`
- `sameSuspendActionReplaysOriginalWithoutAnyWrite`
- `sameResumeActionReplaysOriginalWithoutAnyWrite`
- `historicalReplayAfterOppositeTransitionReturnsOriginalResult`
- `changedReasonAndExpectedRevisionConflictWithoutDuplicateFacts`
- `changedActorSubjectAndRouteUnderSameKeyConflict`
- `missingSubjectAndMissingActivationAreNotFound`
- `inactiveAndWrongCurrentStatesConflict`
- `staleAndUnrepresentableExpectedRevisionsConflict`
- `missingDisabledAndNonOpsEmployeesCannotMutateOrReplay`
- `wrongAudienceAndCustomerCredentialNeverReachOwnerWrites`
- `absentAmbiguousAndUnusableVerifierFailClosed`
- `maximumActionAndReasonRoundTripWithoutTruncation`
- `concurrentSameSuspendActionConvergesOnOneFactSet`
- `concurrentSameResumeActionConvergesOnOneFactSet`
- `concurrentDistinctActionsAtOneRevisionHaveOneWinner`
- `concurrentCrossSubjectSameKeyHasOneWinnerAndOneConflict`
- `employeeRevocationSerializesWithLockedCurrentAuthority`
- `activationAndCustomerCommandRacePreservesOneAuthoritativeHead`
- `resumeAndSuspendNeverChangeTrialConsentOrOtherBusinessFacts`
- `auditFailureRollsBackHeadTransitionAndOutbox`
- `transitionOutboxAndCommitFailuresCannotPartiallySucceed`
- `responsesProblemsAndEventsRespectSensitiveOutputBoundary`

### OperationsLifecycleRoleLeastPrivilegeIT — 12

- `activationRoleCannotReadOrLockEmployeeTable`
- `employeeOwnerLocksOnlyItsOwnRowWithoutWrites`
- `handoffUsesOneBackendPidSessionAndTransactionId`
- `mandatoryCallOutsideTransactionOrWrongEntryRoleIsRejected`
- `activationRoleIsRestoredBeforeEveryOwnerWrite`
- `auditUsesOnlyExistingFunctionCapabilityAndRestoresRole`
- `failedRestorationRollsBackAndPoolReuseCannotLeakRole`
- `ownerAndAuditRolesCannotWriteForeignBaseTables`
- `locksFollowSubjectActivationAdministratorEmployeeOutboxAuditOrder`
- `runtimeLoginHasOnlyThreeFixtureMembershipsAndNoPrivilegeRepair`
- `frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact`
- `failureCleanupLeavesNoTransactionRoleLockOrPartialFact`

| Inventory | Baseline | Added | Final source/CI/POM |
| --- | ---: | ---: | ---: |
| Surefire classes/methods |59/520|5/54|64/574|
| Failsafe classes/methods |25/404|2/36|27/440|
| Total classes/methods |84/924|7/90|91/1014|
| Annotation-token lines |925|90|1015|
| Exact Failsafe includes |25|2|27|

`pa32_unit_additions` ordered counts are8/12/18/8/8; `pa32_container_additions`24/12. Every historical method set, PostgresContainerIT64, the sole preexisting comment-hit tuple and historical totals remain exact. Static CI execution passes PA32-start520/404 and925, PA31-start468/370 and839, PA30-start417/336 and754, and all older assertions. POM includes add only `**/OperationsSubjectLifecycleIT.java` and `**/OperationsLifecycleRoleLeastPrivilegeIT.java`.

**XML is deliberately NOT claimed equal to final source/CI/POM:** the five new unit classes report54/0/0/0 and three retained architecture classes report18/0/0/0, total72/0/0/0 across8 classes. Exact selected class/method XML sets were checked and copied to `/tmp/pa32-s1-validation/surefire-selected/`. Historical CustomerWebConfigurationTests additionally reports6/0/3/0 in the combined focused run; the three errors are forbidden socket binds. Four focused integration classes report one startup error each before their methods execute, not36/34 passing IT methods. Full1014-case XML acceptance is outstanding.

## Validation commands, results and paths

Evidence directory: `/tmp/pa32-s1-validation/` on this machine (temporary, not a committed artifact). Maven outputs/XML are under `backend/target/`; build side outputs are ignored validation outputs, not extra implementation paths.

### Toolchain and exact focused command

`java -version` via initial PATH invokes the Windows shim and fails `WSL ... UtilBindVsockAnyPort ... socket failed 1`. A local Linux `/opt/jdk21/bin/java` is available and reports Temurin21.0.13+9-LTS; Maven wrapper pin is3.9.9 and enforced Java/Maven/dependency rules pass. Docker CLI reports permission denied connecting to `/var/run/docker.sock`.

Accepted command from backend:

```bash
./mvnw -B -ntp -Pcontainer-smoke   -Dtest=EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectLifecycleServiceTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerApiBoundaryTests,CustomerWebConfigurationTests   -Dit.test=OperationsSubjectLifecycleIT,OperationsLifecycleRoleLeastPrivilegeIT,CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT verify
```

- First exact launch with `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH` fails exit126: `./mvnw: /bin/sh^M: bad interpreter: No such file or directory`; log `focused-1.log`. Frozen/nonallowlisted wrapper was not edited.
- Nonmutating wrapper execution is `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH bash -c "$(sed 's/\r$//' ./mvnw)" ./mvnw <same arguments>`. This runs the repository-pinned wrapper contents with LF in memory, not another build tool/version. `focused-2.log`: production239 and test127 sources compile; first tests identify two local assertion defects plus Mockito attachment and socket restrictions. Defects were fixed within allowlist; no historical assertions were weakened.
- Sandbox blocks Mockito self-attachment. Follow-up commands add only command-line `-DargLine=-javaagent:/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar` from existing pinned dependency cache, without POM/library changes. `focused-3.log`, exit1:78 tests,0 failures,3 errors,0 skips; all new54 pass and all non-server retained architecture tests pass; three historical CustomerWebConfigurationTests fail with `java.net.SocketException: Operation not permitted` while starting Tomcat. Combined focused verify cannot reach Failsafe.
- Socket-free focused diagnostic uses same wrapper/agent, `-Dtest=EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectLifecycleServiceTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerApiBoundaryTests test`: `unit-1.log`, exit0,72/0/0/0. Same selection with `package`: `unit-package-final.log`, exit0,72/0/0/0, Spring Boot repackage succeeds. This is a diagnostic subset, NOT substituted focused acceptance.
- Separate same-wrapper/agent `-Pcontainer-smoke -Dit.test=OperationsSubjectLifecycleIT,OperationsLifecycleRoleLeastPrivilegeIT,CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify`: `integration-1.log`, exit1; all four classes fail startup with Testcontainers `Could not find a valid Docker environment`, underlying forbidden Unix socket connection. No tests were disabled/skipped; no live DB/production credential was used.
- After fixture assertions were tightened, `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH bash -c "$(sed 's/\r$//' backend/mvnw)" backend/mvnw -B -ntp -f backend/pom.xml test-compile`: `final-test-compile.log`, exit0, all127 test sources compile. Final changes after packaging are test-only integration assertions, not packaged production changes.
- **Not run:** `./mvnw -B -ntp clean -Pcontainer-smoke verify`, because the required focused-pass prerequisite was not achieved and both socket/Docker blockers are confirmed. Controller must execute the full clean suite after focused PostgreSQL16 pass in an authorized environment. No full-success, zero-skip integration or17-version runtime migration claim.

### Inventory, JAR and contracts

- `/tmp/pa32-static.py` extracts the actual embedded CI Python with AST, runs its source/arithmetic/POM assertions (excluding XML-dependent calls only), then independently checks every source method set against CI. `source-inventory.log`: PASS64/574+27/440=91/1014, annotation1015/POM27, all historical arithmetic. `/tmp/pa32-evidence.py` checks exact accepted plan names, allowlist, hashes, selected XML and whitespace; `evidence.log`: PASS. These scripts are temporary evidence helpers, not extra implementation files or replacement CI.
- CI packaged `production jar dependency firewall` was extracted and executed unchanged except in-memory wrapper CRLF handling: `production-jar-dependency-firewall.log`, exit0. Exact baseline37/34,ssm72/65,counterparts64,dependency-only8,sole packager1;116 adversarial probes pass. Employee verifier direct-interface ban is included. Packaged JAR is `backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`; jar index and exact runtime dependency list are `backend/target/jar-index.txt` and `backend/target/pa11-runtime-dependencies.txt`.
- CI `default and opt-in web jar smokes` was extracted and executed: `default-and-opt-in-web-jar-smokes.log`, exit1, `web JVM exited before startup`. Default nonweb phase succeeded; `backend/target/web-boot.log` establishes `java.net.SocketException: Operation not permitted`. Both endpoint negative probes are checked into CI but could not execute against a listening web JAR here; no probe-success claim.
- `cd api/openapi; npm_config_fetch_retries=0 npm ci && npm run check:s3`, pinned Node22.23.4: `openapi-install.log` and `openapi-check.log`, exit0.49 exact Operations/OpenAPI operations, counts18/21/6/3/1, no mapping drift. Generated outputs are ignored and not in implementation diff.
- Exact CI A2 command: `node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md`; `A2-s3.log`, exit1,25 fixtures pass but CRLF-sensitive A0 headings/check parsing reports8 status/6 registry/40 column drifts. An LF-only **temporary copy** of frozen docs14 at `/tmp/pa32-s1-validation/docs14-lf-diagnostic.md` with only `--a0` redirected produces `A2-lf-diagnostic.log`, exit0, all drift0. Frozen original/validator never changed; exact checked-out command remains failed.
- Exact CI A3 s3 security validation passes (`A3-s3.log`,23 fixtures,drift0); exact A4 s2 capacity passes (`A4-s2.log`,113 fixtures,471 nonfinite probes). Commands are the unchanged corresponding CI step bodies, extracted by `/tmp/pa32-ci-steps.py`.
- Exact CI A5 s1 and s2 fail (`A5-s1.log`, `A5-s2.log`): `A5_DETERMINISM_FAILURE determinism.probe two-probe`. `node-spawn-diagnostic.log` proves even `spawnSync(process.execPath,['--version'])` returns `error.code=EPERM`, syscall `spawnSync /opt/node-v22.23.4-linux-x64/bin/node`; do not interpret child-process sandbox failure as changed acceptance policy or weaken validators. Full hosted A1–A5 remains Controller-owned.

## Frozen files, scope and final hygiene

All22 protected files (OpenAPI, docs12/14/15/17 and V001–V017) match accepted §11 raw-byte SHA256 exactly. Git diff against product base over these files is empty, migration inventory is exactly17/noV018. Plan SHA256 also remains exact. Frozen policy/security validator, Control, fix/review artifacts and `.codex` were not modified. No new dependency, provisioning grant or application YAML was added. No outside-allowlist production/test blocker was discovered.

`/tmp/pa32-s1-validation/frozen-sha256.txt` records every frozen digest; `/tmp/pa32-s1-validation/implementation-sha256.txt` records all36 implementation file digests. Implementation manifest SHA256: `1fdd7acb231f4ae0db4b5e5a3f93bb7b1611b0d8e3c57c702c1af40260085f4c`. `/tmp/pa32-s1-validation/evidence.json` records the exact path/method sets and hashes. This report's own digest must be computed after writing; no self-referential hash claim.

At completion the required read-only Git commands use this worktree's explicit metadata because plain discovery resolves an unusable Windows gitdir:

```bash
export GIT_OPTIONAL_LOCKS=0
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call16
export GIT_WORK_TREE="$PWD"
git -c core.fsmonitor=false status --short
git diff --name-only
git ls-files --others --exclude-standard
git diff --check
```

Expected final membership: five tracked modified allowlisted paths;31 new allowlisted source/test files; this one new report; preserved preexisting untracked `.codex`. No staged changes. Git's LF→CRLF warnings are checkout configuration diagnostics, not permission to normalize protected files. Read-only scope checks enforce membership; no index/stage/commit/fetch/pull/push/branch/PR/issue/review mutation.

## Residual risks and next owner

- **Controller / Backend / Database, blocking acceptance:** Run accepted focused command on actual PostgreSQL16 plus full `clean -Pcontainer-smoke verify`; reconcile exact91/1014 XML including zero failures/errors/skips,27 includes and17 successful Flyway versions. Current DB tests compile but none executed. Do not infer transaction/privilege/rollback correctness from mocks.
- **Backend / independent code review:** Inspect complete36-path candidate, especially role restoration under aborted SQL, current-authority precedence, stored-witness integrity, global action collision, timestamp/hash agreement and no-trial guarantees. No independent review has been performed or claimed. Some tests use source-boundary assertions in addition to behavioral assertions; these do not replace runtime proof.
- **Backend / Database test coverage:** IT fixture reuses the established customer activation fixture's postgres16 container to establish genuine original trial facts and adds its own independent lifecycle pool/login/context. The real PID capture, same-key/cross-subject races, administrator contention, failure-after-stage decorators, aborted employee-read listener and before-commit failure hook are compiled but unexecuted. Planned exhaustive JDBC-wrapper restoration/audit-invocation fault combinations and actual response-delivery-loss transport simulation are not fully represented: lost response is modeled by discarding a successful response then replaying. Controller must decide whether additional coverage within the fixed existing method/path inventory is required before acceptance; no extra methods/files are authorized here.
- **Controller / CI / Platform:** Re-run real listening packaged web probes, source=CI=POM=XML inventory and hosted A1–A5 on one frozen candidate. Current socket/Docker/child-spawn restrictions and checked-out CRLF A2 parsing must be handled by the authorized runner, not by frozen-file/validator edits. Full clean suite intentionally waits for focused pass.
- **Security / Operations IAM deployment:** Production independent employee verifier, issuer/store/session expiry/revocation/MFA/Q1 are external deployment work. No bundled implementation and no production readiness claim. Fake/synthetic verifier remains test-only.
- **Database / deployment:** Supply an already-authorized NOINHERIT runtime identity capable of SET ROLE to the three preexisting roles; no production grant repair is shipped. Independent288 ACL oracle is reused unchanged by reflection in the IT and still needs PostgreSQL proof.
- **Operations:** Global administrator advisory serialization is intentional; throughput tuning, UI secondary confirmation, real provider/live acceptance and production operational readiness are outside this slice.

**Stop: Controller implementation intake and fresh independent A/B code review against the frozen36-path diff plus this report. No later gate is entered.**
