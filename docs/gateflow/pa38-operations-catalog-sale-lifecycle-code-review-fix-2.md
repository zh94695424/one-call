# PA38-S1 post-fix re-review fix2

## Binding and disposition

- Task: `pa38-operations-catalog-sale-lifecycle-code-review-fix2-gpt-20261119-01`; November 19, 2026, Asia/Shanghai; bounded fix author only.
- Primary workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`; HEAD `ca90b0793f615a370ec66334fd196653beb358f4`, tree `f716066ac00f19bf6874060be343a99b2be55aa3`, branch `codex/pa38-next-work-unit`, unchanged.
- Implemented only accepted `PA38-RR2-02`, `PA38-RR2-03`, `PA38-RR2-04`, in the five allowed existing Java paths, the implementation report and this new artifact. No Java source/test file or test method is added, deleted, renamed, disabled or skipped.
- **Status: accepted bounded code corrections implemented; validation-blocked.** Focused 86, PostgreSQL 38 and three sequential exact-observer executions pass. Guards and canonical verify are not green: required removal of production metadata changes the manifest hash, while its explicitly forbidden carrier retains its old literal. No failure is waived or concealed; there is no implementation-pass or readiness claim.
- **Next gate: re-review**, after Controller disposition of the manifest-carrier scope conflict and completion of required green validation. No re-review is requested or entered by this author.
- `PA38-RR2-01` remains deferred-with-owner to the PA35 guard owner. Neither PA35 guard nor its primary frozen CRLF plan is edited. Raw CRLF reproduction of the known two failures is not treated as a PA38 blocker. The new failure described here is unrelated to that accepted deferral.

### Input custody

Read Control's PA38 section, the accepted cumulative plan, both original reviews, both post-fix re-reviews, fix1 artifact, implementation report and exact five source/test files, with unchanged configuration/repository/CI/POM/guard context. Starting raw SHA256 bindings:

```text
docs/gateflow/project-execution-control.md 58c1fe4e21ca15101e651a5db2c0f0c977f8fb1d23dd25aa2137150107e66486
docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan.md 337e27f7cbb7a1f31c0acba474fbe375ad1e587b88e34a3b6089c04c137fd8de
docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-1.md 356852b572409570a06c9701b23e4ff559e3bc38830c3224dc601ea61030dcff
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md c8a34be41a7aa84642d2ef7460c8a3a25bb990265b24504a234691e4476b6686
docs/reviews/code-review-20261118-225316.md 779237fa91d9e8311f6e3bbd06c89dfd2ac06f4b68b8291edbe7e241c4cfd0b4
docs/reviews/code-review-20261118-225837.md 12ee53228bc60db934d41e0aacae9991851ad47fc8fc0f09e8e040c746f0c8e0
docs/reviews/code-review-20261119-101103.md 9b81661833c2309fe25e5c0764e39b2bf33f43a6a162825b028864e9d086f9ab
docs/reviews/code-review-20261119-110803.md cc1d58f457364e4c8f2190dd714b98a89dc8a54788eea4d938aefc00def9ddde
```

Control, plan, reviews and fix1 artifact remain unchanged. The report's current hash is in the final 32-path inventory below, not its starting binding above. Controller's later Control bookkeeping, if any, supersedes this author-time pin without implying an author edit.

## Finding-by-finding closure

### PA38-RR2-02 — code corrected, real PostgreSQL evidence green

- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java:28` exposes only two package-private synchronization fields needed by the existing IT: a lock-attempt latch and volatile backend PID. `OperationsCatalogVersionLifecycleFixture.java:42` resets both on every test/subcase reset.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java:47` identifies product advisory-lock execution in jOOQ `executeStart`, reads `pg_backend_pid()` using **that creating command's `context.connection()`**, assigns the PID, and counts down immediately before the actual product-lock SQL executes. Direct JDBC avoids listener recursion; no admin/observer/other login PID is substituted.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:75` uses distinct `observerStarted` and `waitObserved` latches. After opening its admin connection and preparing its statement, the observer unconditionally signals started, then waits for the fixture's command lock attempt and asserts a positive PID. The main thread proves observer startup **before** submitting the command.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:78` polls exactly `SELECT count(*) FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock'`, binding that command PID with `setInt`. A different same-login waiter cannot satisfy this predicate. Observation releases `waitObserved`; both its bounded await and the observer future must succeed.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:94` retains actual SQLSTATE `55P03`, exact sanitized 409, zero audit/Outbox facts and byte-equal lifecycle state; the unchanged product `lock_timeout=250ms`, product lock holder, same-pool concurrent product read, rollback and clean session-restoration assertions all remain. Earlier one-winner, real 23505, 40001/recovery/no-retry and replay backstops in the same frozen method are unchanged.
- The complete Lifecycle class passes 24/24 within the 38-test PostgreSQL run. The unchanged method `concurrentPublishersYieldOneActiveWinner` also passes three separate sequential Maven invocations against fresh live PostgreSQL containers, 1/1 each. No polling sleep, raised product timeout, synthetic SQLSTATE substitute, external same-login waiter, bypassed assertion or new test identity is introduced.

### PA38-RR2-03 — dead metadata removed, real qualification preserved

`backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java:14` has an ordinary `TransactionTemplate` constructor parameter; its inert `@Qualifier` and import are removed. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java:18` retains the effective qualified bean-method parameter, and its manual construction at line 22 is unchanged. Focused real-context/provider coverage remains green. There is no product behavior or transaction-setting change. The resulting production-byte digest change is independently measured below; its forbidden guard carrier is deliberately not edited.

### PA38-RR2-04 — one-way support ownership and nonvacuous assertion

- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java:39` keeps the existing non-test harness in the same compilation unit, now owning VERSION/PRODUCT/HASH/NOW, its valid-row data factory at line 92 and mocked-record factory at line 100. All harness and repository-test references back to `CatalogSaleLifecycleSemanticsTests` are removed.
- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java:5` imports only the support type's static data/factories, uses its `record(...)` helper, and continues constructing that support harness directly. There is no reference to the repository **test class**. Dependency direction is repository test → harness and semantics test → harness, with no harness → test edge and no test-class cycle.
- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java:22` preserves `readsAuthorityWithoutLocking`; line 23 contains the requested concise owner comment naming `DefaultOperationsCatalogSaleCommandTests.requiresActiveAdminWitness`. Line 24 asserts `assertThat(statements).isNotEmpty()` **before** the negative `allSatisfy` assertion. Empty observations can no longer pass vacuously. The frozen identity is not renamed.
- Focused Repository 12/12 and Semantics 10/10 pass; all 124 PA38 test identities remain set-equal to frozen CI and independently preserved fix1 source. Java file counts remain 290 production / 180 test. No additional Java file is created.

## Environment and validation custody

- Launcher: `/opt/maven-3.9.9/bin/mvn -B -ntp` (M below); `JAVA_HOME=/opt/jdk21`, PATH prefixed `/opt/jdk21/bin`; Maven 3.9.9, Temurin Java 21.0.13.
- Individually recorded preflight commands: `M -v`; `docker version --format '{{.Client.Version}}'`; `docker version --format '{{.Server.Version}}'`; `docker info --format '{{.ServerVersion}}|{{.OperatingSystem}}|{{.KernelVersion}}'`. Each actual exit is 0. Client 29.1.3; **Server field 29.1.3**; server-backed info `29.1.3|Ubuntu 22.04.5 LTS|6.18.33.2-microsoft-standard-WSL2`. Live Flyway/container logs report PostgreSQL 16.15.
- One Git-less projection: `/tmp/pa38-lf-validation.y9rnvg1o/candidate`, initial materialization inventory `0c4fc052a0d3adf376114595dcbdfbbd435a6047d44c16ca4551bc39528805d5`. Creation refuses existing destination and verifies complete initial byte equality while excluding `.git`, `.codex`, `.pa38-*`, `.pa*-*`, `target`, `tmp`, `node_modules`.
- Only the prescribed PA35 plan's 1,344 CRLF lines become LF in the projection: primary raw SHA256 `d5ba84e34124e0cb44fd5a6c8fdc008a8894c3dee472268997d6a4f02500de55`, projected LF SHA256 `58ec889fe812cd1e0c439a7d3c1054aa41bfe8bd44bcd27183512fbe3ffd85d3`. All other projected bytes and primary frozen bytes remain unchanged. Final static audit verifies all five changed Java files still equal the tested projection byte-for-byte. Documentation bookkeeping follows tests only.
- Evidence directory `/tmp/pa38-fix2-evidence/` contains raw combined stdout/stderr logs, expanded per-gate commands, actual exit records and copied per-run XML. These are local evidence, not staged repository inputs. XML snapshots are captured immediately after each invocation, before the next method run or `clean` can overwrite them. Only the selected class XML is counted for each targeted gate; retained unrelated XML in those directories is not counted as freshly executed evidence.

### Exact commands, exits and totals

Run from the projection's `backend`, with M and JDK above:

| Gate | Arguments after M | Exit | Executed totals |
| --- | --- | --- | --- |
| Focused eight classes | `-Dtest=EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests test` | 0 | 8 classes / 86 tests; F/E/S 0/0/0 |
| Guards | `-Dtest=ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test` | **1** | 2 classes / 17 tests; **F/E/S 1/0/0** |
| Canonical focused verify | `-Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify` | **1** | Surefire 97/934; **F/E/S 1/0/0**; Failsafe not reached |
| Direct Failsafe, same profile/classes, no skipped tests | `-Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify` | 0 | 2 classes / 38 = 24+14; F/E/S 0/0/0 |
| Observer repeat 1 | `-Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify` | 0 | 1/1; F/E/S 0/0/0 |
| Observer repeat 2 | same exact arguments, separate subsequent invocation | 0 | 1/1; F/E/S 0/0/0 |
| Observer repeat 3 | same exact arguments, separate subsequent invocation | 0 | 1/1; F/E/S 0/0/0 |
| Canonical clean full attempt | `clean -Pcontainer-smoke verify` | **1** | Surefire 97/934; **F/E/S 1/0/0**; Failsafe not reached |
| Static audit | `python3 /tmp/pa38-fix2-evidence/static-audit.py` | 0 | source/CI/POM identities, frozen bytes, scope, manifest, candidate aggregate |
| Whitespace | process-local `git -c core.safecrlf=false diff --check` | 0 | no whitespace errors |

The three sequential observer-only Maven executions finish at `2026-11-19T11:19:03+08:00`, `2026-11-19T11:19:19+08:00` and `2026-11-19T11:19:36+08:00`, respectively. A successful single 38-test run is not substituted for these required repetitions.

### Single validation blocker — out-of-allowlist manifest carrier

All three nonzero Maven invocations have exactly one failure, the unchanged `EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen`, at the manifest assertion in `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java:102`:

```text
expected: e9de000fabce960d271e819e870390bcb151e8b03b1d7292c43b5c0b391e65d8
actual:   3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0
```

The count remains 310. Replacing only the command entry's hash in an **in-memory** counterfactual manifest with the preserved fix1 command hash reproduces the old pin exactly; no file is modified for that check. This isolates the digest change to the required dead-metadata removal, not unrelated production drift. The guard file is explicitly outside the allowed write paths. It is neither edited nor patched in the LF projection. CI and POM are also unchanged.

Accordingly, guards are **16 passing / 17 run**, not 17/17 green. Both canonical verifies stop at the Surefire failure; **the full 38-class / 630-test Failsafe sweep and same-shell clean-full CI/XML oracle do not run**. A green complete sweep is not claimed. Direct Failsafe goals establish only the specifically requested two PA38 classes and repeated observer, not canonical full acceptance. No `skipTests`, `maven.test.failure.ignore`, disabled test, altered digest expectation, modified projection guard or synthetic XML is used. Controller must authorize/dispose of manifest-carrier synchronization before required green global gates can be completed; no forbidden repair is made here.

### Preserved diagnostic failures

- Initial plain `git status --short`: exit 128 because the preserved Windows `.git` pointer targets `D:/codex-call/.git/worktrees/codex-call25`. Read-only Git evidence uses process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, `GIT_WORK_TREE` set to primary, `GIT_OPTIONAL_LOCKS=0` and `-c core.safecrlf=false`; no repository/config/index repair.
- `static-initial.log`: exit 1 because a first scope assertion did not separately classify the four pre-existing untracked original/re-review artifacts. They were present in the starting inventory and remain unchanged; the corrected audit explicitly lists them rather than silently hiding them.
- `static-second.log`: exit 1 because an exploratory regex stopped at an inner set's closing brace in the CI Python map. Replaced only the temporary audit parser with AST extraction of the unchanged shipped heredoc; no CI edit.
- `static-third.log`: exit 1 because an exploratory POM scan counted two unrelated dependency includes as well as the 38 Failsafe includes. The corrected temporary audit scopes to `maven-failsafe-plugin`; no POM edit. `static-fourth.log` and subsequent audits exit 0.
- `guards17.log`, `canonical-focused-verify.log`, `clean-full.log`: actual Maven exits 1, exactly the manifest mismatch above, preserved without rerun-based concealment. This is the current validation blocker, not the deferred PA35 failure pair.
- A read-only lookup for nonexistent `/tmp/pa38-tools` produced a discovery diagnostic; no such directory or helper was required or fabricated. No candidate compilation failure, PostgreSQL failure, observer-repeat failure or skipped test occurred.

## Test identities and counts

All names below are set-equal to the unchanged `pa38_unit_additions` / `pa38_container_additions` CI literals and the corresponding preserved fix1 source. Failsafe includes remain exactly 38 and contain both PA38 classes. Guard identities remain 9+8. Java source files remain **290/180**. The unchanged frozen test names are:

### EmployeeAuthorityReadPortTests — 8

```text
readsOnlyStatusRoleRevision
isTransactionMandatory
usesOperationsIamRole
doesNotUseForUpdateOrAdvisoryLock
preservesCommandConnectionOnSuccess
marksOuterTransactionRollbackOnlyOnFailure
mapsMissingEmployeeToUnavailable
returnsImmutableSnapshot
```

### CatalogSaleLifecycleSemanticsTests — 10

```text
canonicalLockKeyUsesUtf8ProductId
dedupeKeysUseCanonicalVersionId
publicationReasonIsActionDerived
offsaleReasonIsActionDerived
publicationSnapshotsContainOnlyFrozenFields
offsaleSnapshotsContainOnlyFrozenFields
rejectsMalformedPersistedState
rejectsRevisionOverflow
nullActiveExpectationIsSyntaxOnlyForActiveIdentity
preTransitionExpectationIsNotReplay
```

### CatalogSaleLifecycleRepositoryTests — 12

```text
readsAuthorityWithoutLocking
entersExactOperationsIamRole
entersExactCatalogOrderRoleForMutation
acquiresProductLockBeforeDecisionTime
publicationReadsCandidateAndCurrentActiveUnderLock
retiresOldBeforeActivatingCandidate
offsaleUsesConditionalRevisionUpdate
updatesOnlyLifecycleColumns
usesOnePublishAudit
usesTwoPublishOutboxesWhenOldActiveExists
usesOneOffsaleAuditAndOutbox
restoresRoleOnSuccessAndFailure
```

### DefaultOperationsCatalogSaleCommandTests — 16

```text
requiresActiveAdminWitness
opsMapsToForbidden
disabledMapsToForbidden
missingAuthorityMapsToServiceUnavailable
mapsStaleExpectation
mapsUnmatchableStateToConflict
replayPerformsZeroWritesAndFacts
rollsBackAllFactsOnRepositoryFailure
publishCommandHasFrozenFields
offsaleCommandHasFrozenFields
returnsExactCatalogVersionState
usesClosedFailureCodes
doesNotExposeRequestReason
doesNotExposeDatabaseTypes
acceptsNullableExpectedActiveOnly
rejectsNonPositiveExpectedRevision
```

### CatalogVersionRequestBinderTests — 10

```text
publicationRequiresExactlyTwoFields
publicationAcceptsExplicitNullActive
offsaleRequiresExactlyOneField
rejectsUnknownFields
rejectsDuplicateFields
rejectsTrailingTokens
rejectsQuotedFractionalOverflowAndZeroRevision
rejectsNonCanonicalUuid
rejectsInvalidUtf8AndOversizedBody
rejectsReasonAndIdempotencyFields
```

### OperationsCatalogVersionWebTests — 14

```text
admitsExactPublicationPost
admitsExactOffsalePost
classifiesReadsSubjectsAndCatalogCommandsSeparately
catalogCommandsDoNotRequireIdempotencyKey
subjectCommandsRetainExactIdempotencyRule
authenticationPrecedesStructuralParsing
structuralValidationPrecedesAuthorityWitness
disabledEmployeeReturnsForbiddenAfterValidParsing
opsReturnsForbidden
rejectsInvalidContentTypeAndShippedAcceptOutcome
successHasExactStateAndHeaders
staleViewHasExact409ProblemHeadersAndLength
concealsOtherCatalogMethodsAndUris
publicationConflictHasExact409ProblemHeadersAndLength
```

### OperationsCatalogVersionConfigurationTests — 8

```text
reusesExistingOperationsDatasource
usesNewWritableRequiresNewReadCommittedTemplate
reusesSamePhysicalDslBinding
doesNotCreateSecondPoolOrCredentialFamily
resolvesExactlyOneCommandProvider
missingProviderFailsClosed
ambiguousProviderFailsClosed
defaultRuntimeHasNoWriteBeans
```

### OperationsCatalogVersionBoundaryTests — 8

```text
scansExactProductionFiles
commandPortIsOperationsIamOwned
catalogOrderImplementsNamedPortWithoutReverseEdge
employeeAuthorityPortIsOperationsIamOwned
catalogSqlNeverReadsEmployeeAccount
rejectsSelectStarForbiddenTablesAndContentWrites
requiresOneTransactionAndPublicationOrder
preservesFrozenInputsAndNoV019
```

### OperationsCatalogVersionLifecycleIT — 24

```text
adminPublishesWithoutPreviousActive
adminPublishesAndRetiresPreviousActive
retirementStatementPrecedesActivation
publicationIncrementsBothRevisions
publicationUsesOneDecisionTimestamp
publicationAuditArgumentsAreExact
publicationOutboxArgumentsAreExact
publicationWithOldActiveHasOneAuditAndTwoOutboxes
offsaleTransitionsActive
offsaleAuditAndOutboxArgumentsAreExact
publicationCurrentStateReplayIsZeroWrite
offsaleCurrentStateReplayIsZeroWrite
preTransitionPublicationRetryIsStale
preTransitionOffsaleRetryIsStale
stalePublicationMakesNoChange
staleOffsaleMakesNoChange
unexpectedStatesMapToConflict
publishedContentAndHistoryRemainByteIdentical
auditFailureRollsBackLifecycle
outboxFailureRollsBackLifecycle
concurrentPublishersYieldOneActiveWinner
concurrentOffsalesYieldOneWinner
oneActiveInvariantHoldsAfterEveryCommit
routeMutationOutsideTwoTemplatesIsConcealed
```

### OperationsCatalogVersionRoleLeastPrivilegeIT — 14

```text
disposableLoginHasAdminFalseInheritFalseSetTrueRoles
loginCanSetOperationsIamRole
loginCanSetCatalogOrderRole
operationsIamRoleReadsOnlyAuthorityColumns
catalogRoleCannotReadEmployeeAccount
catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes
v001ThroughV018ProvideAllRequiredObjects
noV019IsNeededOrPresent
setLocalRoleIsExactAndRestored
restorationFailureEvictsDirtyConnection
readOnlyOrWrongRoleCannotMutateCatalog
publishedTriggerRejectsContentMutation
activePartialUniqueIndexHolds
historicalRowsRemainUnchangedAfterRepeatedCommands
```

## Frozen boundaries and scope

- Final candidate: **32 original implementation/evidence paths + fix1 artifact + this fix2 artifact + Controller-owned Control = 35**. Existing raw Git public status also contains four untracked independent review artifacts (`code-review-20261118-225316.md`, `code-review-20261118-225837.md`, `code-review-20261119-101103.md`, `code-review-20261119-110803.md`) already present at dispatch. They are separately hash-bound above and unchanged; raw public status is therefore 39, not silently reported as 35. They are not new fix2 writes.
- The final scope audit compares all starting public input hashes and requires no changes outside the seven allowed paths, except generated build output in the independent projection. Among the original 32 paths only the five authorized Java paths and report differ from fix1. No Java file is added; no source/test, test identity, route, DTO, schema, ACL, migration, OpenAPI, CI or POM is otherwise edited.
- Zero tracked deletions, zero staged paths, zero staged `.pa38-*`/`.codex` entries; no staging command is run. Private pre-existing helpers stay untouched and untracked. `git diff --check` passes.
- V001–V018 remain exactly 18 raw-byte-unchanged migration files; no V019. Raw migration aggregate remains `53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70` (ordinal repository-relative path/raw-SHA256 rows, LF-joined with final LF). OpenAPI/docs12/14/15/17/PA25 raw pins are unchanged:

```text
api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68
docs/12-technology-stack-and-system-architecture.md 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db
docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31
docs/15-production-openapi-surface-spec.md db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0
docs/17-production-security-design-spec.md b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205
backend/ops/postgresql/pa25-runtime-identities.sql b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9
```

### Recomputed production manifest

All regular backend `src/main/java` and `src/main/resources` files, excluding only filename `V018__evidence_role_ivr_association_read_privileges.sql`; normalize each file's CRLF to LF, hash, form `backend-relative/path<space>lowercase-sha256`, sort ordinally, LF-join with a final LF, SHA256 UTF-8 bytes. **310 entries / `3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0`**. Complete computed rows: `/tmp/pa38-fix2-evidence/manifest.txt`. The unchanged guard still pins the prior digest, as disclosed above; there is no manifest byte-count field and no green-guard claim.

### Original 32-path candidate inventory

Algorithm: sort the original 32 repository-relative paths ordinally; hash each **raw** file; form UTF-8 `relative/path<single-space><lowercase-raw-SHA256>` rows, LF-join with one final LF, SHA256 that serialization. Includes the updated implementation report; excludes both fix artifacts, review artifacts and Control. No self-referential report/artifact hash is required.

**32 paths / 540,342 raw bytes / ordinal aggregate `cb10ab61785a1f27c0589c978d4f8149f7144dec46139e2d74f09186ab8eb406`**.

```text
.github/workflows/ci.yml 11dfd54c9c0c797ea30142d2a0a614df6385c724f54a343f6bfc5a2b8677209a
backend/pom.xml f8fc1389d46a0af34ebb8b538947c6571211b44a0e19be7875ab3923dfee0fb8
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java 5ae7933e32a200e1efc6c66c0d79c1568e382f8767eef9f3eebaff24f8b2dc40
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java 360290131c6d32200bb725627fb3268222ec755012b8dd8b8ffef44c60ae9f29
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java fb4b3f0834bb55f15f230b916aaea0a40b34943de71040d0ce0af2cceb001640
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java c8815c97a2185e2c92153def48f58b8790a4fe728d18132643fe9c18655f8cee
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java a6cb5b1a1afe682b3b539db04a922f6213c95992291e3319e04448c950be3141
backend/src/main/java/com/onecall/core/catalog_order/package-info.java 26aade7834fac4dcc4c0cb8986b0cf73baf79eec92feb7d62583807738f50f6a
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java 8478a8ce9f605422d81521aec94625a035641dbe4d3fe8b36eaa82486dfb34d4
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java b47cba36fb7caf349fb93b6b934be69f2f842624d88976f94377835173995042
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/package-info.java 0f660b9b5b2bb67aabcd493175cdb04fc34496655886750f8ef122e28334ed03
backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java f76b0db87646b0a8fad62256164e26d0805c454a07b2ec88e9990c220904bd61
backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java 01ff82d510de08bf02da5d9737ad75710f998973faacbc883cc31cbfe95cf0fb
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java e32deefb17b395682188612c60fffb4da95b690e7e9b676176a063c1bdd78e1d
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java ef26754fbea7b7b24a313ff138c176f508ed3c189b0f4e758c69d2aa337adf18
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java d46fb4056ae3edc4489e8b71b0787b8ae6adf0c7710ad9771e1def99ed5cdb2b
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java a99c6c711644fe4e9dcdae32b77e256108ec86a66d11dc47a73f36c5b8985b91
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java fbfa75f83eef307f9458a2202b80bbf2cebd91335f34f4ed7d512ac18c762c6d
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java 4f204661ca4512d7112dca3d1043249114b1ce84c3b6876591b91cb16e0acb72
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java 4584f7aba6a8c11cff0aa446b78a1a96c839cfceb99ddc102cc9e91e3990de62
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java f2fcebd2dc1f38096ed1b60969b6b1fe29fdcc975540659204ffa289d8265a60
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java 9accf48cef6775f83a0c2f4d808eeb58c8ad534c44e393fa010fb28167967bdc
backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java 811cf9aecd74f4f3b6180faba42dcc66c4e29fe596d3919d6df5218579786d31
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java 7c5c4a8a654cc187eeee6f3f3bdfa047b46056de2b605680891c6e615a5c4f40
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java 10d01e19451caa78479791f357c6c7e0d04314420a843f02c0f14d839f14b432
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java 4646b6de23b81353a65feff6fed469486b4ee6c245eec2246b7f79e9da71096a
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java acfc4ae711725606643f98ac544918871c4330abd056ae92e1bc7def24418eb1
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java d3a355aa6218620d1ec00d145fbe5567ec04a4aee595d50173a77f0e0b91ac0e
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java c83930bcfda5728defc18cb08b437a48ed274d0fc6dee1bf54e98ec32ce8ddc4
backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java 0c53ae76efda5b34dda851bd93a88e720644e767d5270758df327dace6a4f4a9
backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java 06519b1ac4a2fa46ffb37585990e4c3aa11cf35f6be393344bea17ad47be5c21
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md 03ccd7221c1c00d26ad5667132a7e85c0732eb4050040a8a47ad6c93bf8d31aa
```

## XML and raw log hashes

Per-gate selected XML snapshots and filename/raw-SHA256 rows are retained in `/tmp/pa38-fix2-evidence/test-summary.json`. The two full PA38 IT class hashes below refer to the successful **38-test invocation**, not later method-only XML. Observer repeats have separate immutable copies:

```text
failsafe38-xml/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml de1ac515bc34ac8b772fac10aa1310a1424ad9776f8295eb8542b668c36a055f
failsafe38-xml/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml 8eaace08d280206ddcabdd4716a8a7e18858a46dc6d612b3729ffc51b5965598
observer-repeat-1-xml/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml ab80bc624aefb43ce61bb3bd9b73e25c962cee774adc33cdc632f4d913e27f71
observer-repeat-2-xml/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml 6e4a3cbbc2782b56c52ba4420512f5160737320450a2b4236e75a5577fff6427
observer-repeat-3-xml/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml 17147510f6a325a9cc88558afdb99ed9b93339fb85ef6c9cef16a42c4fa154a3
```

Raw combined logs, all under `/tmp/pa38-fix2-evidence/`, with SHA256:

```text
canonical-focused-verify.log 4ef47af258f16df0dc5d57bf63b01f30deac8655b877e9723c56af2bb369b054
clean-full.log e29b0763726e8088b58da1e443ee29a0645570efc51bf54b55b5abbcf2f17da2
failsafe38.log 1a5b0c67a90554ff2eabf7a978387fb85c7f8daf370d741587e3f3102f9317c8
focused86.log f4809712545c6eb58eab5f213166110c61046fa88c9018a30dd6619353b1b836
guards17.log 8bf0546a79722d8dba07535f477aaf8e0a5544de1747c06d201d93ce7788d884
observer-repeat-1.log b224748efd3d5dd827ddfe569ca3209bd5b954a7724727bfc8bd2ed6ea2f7255
observer-repeat-2.log 1ecc702518192b584a75db428ad054485aa1445e9b9dfdb057f67944fe2fc047
observer-repeat-3.log 32af6d5dbe2152b52bd3b4bb8146637d8eb43fd29c7e8c97fffb1519ccc1536d
preflight-exits.log 6c9004942e97207eaa59645f323a7b43a30139dd2ff284a60cd0e699e157a71d
preflight.log c40828833a41f1a8ff6959fa90eb278b67202f63c9e99784e981e8016a70bfd6
static-fourth.log 24f19a799d312e2e5a8411bd8136941aea30ec70ce24f2e015286285c57cdd28
static-initial.log c71b94087ad68c607cb0f85282573bc9b937f6e70fa9d386111ecb0347aa5243
static-pre-artifact.log 857ca04fd21589e8d89d4d94742ed4d624e258e46d8c35ee7f6f968488b62477
static-second.log 9f6753e343b78d8d98894f7c15376322a7600a6a39e912e8d0a33f8e147f7f71
static-third.log 6762e2151312ff6e893b235938915de60acabe90ad03a8aa84da9b8ac66da747
```

The final static/path audit is rerun after this artifact is created; its transcript is `/tmp/pa38-fix2-evidence/static-final.log`. Final file/log custody hashes are also recorded separately in `/tmp/pa38-fix2-evidence/custody.sha256`, avoiding a self-hash dependency for this artifact.

## Residual risks and stop boundary

- **Controller / manifest-carrier scope:** the required production metadata removal necessarily changes the frozen guard's product-manifest digest. Its carrier is outside this task's allowed paths. Green guards and canonical full acceptance remain blocked until scope is explicitly resolved and the unchanged guard algorithm's correct literal is synchronized by an authorized owner, followed by reruns. This is not an accepted risk waiver or PA35 deferral.
- **Backend test-boundary / PA35 owner:** raw CRLF guard hardening remains `PA38-RR2-01`, deferred with owner. The prescribed LF projection is used without changing primary bytes. The fix2 canonical attempt still fails for the distinct manifest mismatch; the accepted earlier LF green result is historical, not a current-green claim.
- **Backend test robustness:** exact-PID attribution and observer-start synchronization remove the accepted false-witness/startup defects. A severely starved host could still fail to schedule observation within the unchanged real 250ms lock wait; the test fails closed, never counts another backend, and retains all deterministic SQLSTATE/state/fact/session backstops. Three repeated live runs are evidence of stability, not a guarantee under arbitrary scheduler starvation.
- **Platform/Security/Operations:** prior production enablement/secret provisioning, shared four-slot LO pool saturation/capacity, existing broad grants, audit/Outbox dispatch/retention and future draft/UI slices remain unchanged deployment/future-slice ownership; no new product capability, pool, credentials, schema/ACL or operational acceptance is claimed.
- **Controller / next gate re-review:** code corrections are implemented, but required global validation is not complete. This author stops after the implementation report and this fix artifact. No stage, commit, push, branch creation, PR mutation, merge, approval, mark-ready, reviewer request, external comment or issue mutation occurs.
