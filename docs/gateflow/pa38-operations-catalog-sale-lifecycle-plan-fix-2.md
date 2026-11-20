# PA38 Operations Catalog Sale Lifecycle — Plan Fix 2

**Task label:** `pa38-operations-catalog-sale-lifecycle-plan-fix2-gpt-20261118-01`
**Date:** November 18, 2026 (Asia/Shanghai)
**Binding checkpoint:** `docs/gateflow/project-execution-control.md`, PA38 Goal Confirmation and “PA38 plan re-review intake and accepted fix2”.
**Cumulative plan:** `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan.md`.
**Disposition:** author-side closure of Controller-accepted `PA38-PLAN-RR-01..10`; fresh independent A/B plan re-review remains required. No implementation or gate acceptance is claimed.

## 1. Scope and inputs

Only the cumulative plan and this new fix2 artifact are written. Exactly two frozen POST routes, one PA38-S1 slice, no public contract/schema/ACL/credential/pool expansion. The historical fix1 artifact is unchanged; this cumulative fix2 specification supersedes its incorrect datasource, file-count, propagation, CI and manifest statements.

Inputs: the full fixed plan, `pa38-operations-catalog-sale-lifecycle-plan-fix-1.md`, Goal Confirmation/accepted fix2, and all four plan-review artifacts: `docs/reviews/plan-review-20261118-064733.md`, `docs/reviews/plan-review-20261118-082009.md`, `docs/reviews/plan-review-20261118-085416.md`, `docs/reviews/plan-review-20261118-092912.md`. Findings were checked against the actual operations-read and operations-lifecycle persistence configurations, OperationsReadContext, boundary filter/controller/problem writer/principal, PA25 identity SQL, V001/V002/V004/V014/V018, frozen OpenAPI and docs 12/14/15/17, existing read/lifecycle fixtures and ACL tests, both guard classes, POM and the complete CI source/method/XML oracle. Controller adjudication takes precedence over alternative suggestions in the individual reviews.

## 2. Finding-by-finding closure

| Accepted finding | Author-side closure | Exact plan/test verification anchors |
| --- | --- | --- |
| RR-01 — runtime identity/pool | Bind to LO `operationsSubjectReadDataSource` / `onecall.operations-subject-read.datasource.*`; extend the exact operations-read configuration with writable `operationsCatalogSaleTransactionManager` / `operationsCatalogSaleTransactionTemplate`, reusing the existing DSL/guard and physical pool. Explicitly reject operationsLifecycle-family reuse and borrowed membership evidence. | Plan §2.3, §4.2, §6; configuration tests assert raw datasource identity, new writable template and unchanged read-only template; PA25 seven roles enumerated. |
| RR-02 — authority propagation | Sole outer command REQUIRES_NEW/READ_COMMITTED; authority uses mandatory transaction-bound DSL, no suspension/new transaction, no employee/admin lock. Scalar witness and internal ConnectionHolder assertions prove same connection/PID/transaction. Admitted role order is exactly IAM -> catalog -> login. | Plan §3, §6.1, §9.3; authority mandatory/connection tests and `setLocalRoleIsExactAndRestored`. |
| RR-03 — 409 writer path | Authorize one minimal existing-writer overload for the closed catalog command enum; exactly PUBLICATION_STALE_VIEW / CATALOG_PUBLICATION_CONFLICT as 409 codes through the existing envelope/header/byte-length policy. Two explicit HTTP test identities replace two planned methods, preserving 86 additions. | Plan §4.2, §5.2, §9.1/§9.3; `staleViewHasExact409ProblemHeadersAndLength`, `publicationConflictHasExact409ProblemHeadersAndLength`, each covers both routes. Modified production 2 -> 4. |
| RR-04 — module edges | Keep 14 modules and discovered multiset count 13, including six IAM targets. Only catalog_order allowed dependencies grow 3 -> 5; assert all three IAM named-interface tuples and preserve IAM -> operations_audit declaration. No new discovered edge or weakened negative fixture. | Plan §3, §10.2; existing ModuleBoundaryTests remains 9 tests and exact multiset assertion. |
| RR-05 — CI lockstep | Correct fix1 926 -> 934 and 1557 -> 1565; early CI XML literal/diagnostic 848 -> 934; PA38 names in every method-total and source-line subtraction/union family; preserve exact historical comment and all historical baselines. Enumerate each changed literal/diagnostic, maps, POM and Java guard tokens. | Plan §10.1–§10.2; exact ledger, full historical chain, 97/934 +38/630, scan1565 vs methods1564, POM38. |
| RR-06 — exact facts | Freeze 11 audit arguments, deterministic request dedupe identity, carried filter trace UUID, one post-lock decision_time, exact publish status/revision/snapshotSha256 and offsale status/revision snapshots. Freeze all eight Outbox arguments including domain, canonical six-field payload, SQL NULL ciphertext and lowercase canonical SHA256. Exact audit/Outbox order/cardinality; zero replay facts. | Plan §6.2, §8, §9.3; audit/Outbox argument, timestamp and snapshot identities. |
| RR-07 — rollback/recovery/fixture | One fresh bounded recovery transaction on the same LO command pool, exact catalog role, restoration, zero writes/facts and no retry. Add a no-test fixture owning deterministic after-audit/after-Outbox/restore/termination/real-23505/injected-40001 fault stages. Existing IT methods own all subcases. No invented tests or search_path restoration claim. | Plan §4.3, §6.1, §9.2–§9.3; direct unique-race/rollback/eviction subcases; test Java169 ->180 but report classes/methods unchanged by fixture. |
| RR-08 — manifest carrier | Name EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen as carrier, 299 ->310. Exactly sorted backend-relative path + space + LF-normalized file SHA256 lines, one terminal LF, UTF-8 outer SHA256. No nonexistent byte aggregate/field or CI carrier claim. | Plan §10.2–§10.3; exact runnable recomputation and independently reproduced current digest. Final implementation digest must be computed from real implementation bytes. |
| RR-09 — state precedence | Both five-column tables are aligned and top-down first-match. Missing/invariant/non-matchable/stale/replay/transition are partitioned. Explicit ACTIVE candidate +matching current+wrong revision is stale. DRAFT+published_at is schema-unreachable defense-in-depth, not normal behavior. | Plan §7; `mapsStaleExpectation`, `mapsUnmatchableStateToConflict`, stale/replay container tests own all table subcases. |
| RR-10 — accurate ACL claim | Replace planned only-lifecycle privilege identity with `catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes`; assert exact 42 catalog-schema tuples plus frozen cross-schema/function checks and negative probes. Existing broad grants are not called least privilege. | Plan §9.2–§9.3, §12; residual owned by Database/Security + Catalog-order, no ACL migration. |

All ten accepted findings are closed **in the plan**, not certified by implemented/runtime evidence. Prior accepted REV scope/access/parser/replay/ownership corrections remain cumulative.

## 3. Independently recomputed inventory

| Inventory | Actual task-entry/current | Planned PA38 final | Derivation |
| --- | ---: | ---: | --- |
| production Java | 279 | 290 | +11 new; four modified do not add files |
| production Java touched | 0 in this task | 15 | 11 new +4 modified |
| test Java | 169 | 180 | +11 new: ten test classes +one no-test fixture |
| test Java touched | 0 in this task | 13 | 11 new +2 modified guards |
| production/test resources | 21 /1 | 21 /1 | no change |
| migrations | 18 | 18 | V001–V018; no V019 |
| Surefire classes/methods | 89 /848 | 97 /934 | +8 classes /86 methods; lengths8,10,12,16,10,14,8,8 |
| Failsafe classes/methods | 36 /592 | 38 /630 | +2 classes /38 methods; lengths24,14 |
| total report classes/methods | 125 /1440 | 135 /1564 | +10 classes /124 methods; fixture yields no report |
| CI source annotation-line hits | 1441 | 1565 | +124; one preserved historical comment hit |
| Surefire/Failsafe XML test totals | 848 /592 oracle | 934 /630 expected | source=CI=POM=XML; no XML runtime execution here |
| Failsafe POM includes | 36 | 38 | exact two IT includes, no fixture |
| production modules/discovered edges | 14 /13 oracle | 14 /13 | named interfaces add no unique module pair |
| catalog_order allowed dependencies | 3 | 5 | two additional IAM named interfaces |
| modified guard test identities | 9 /8 | 9 /8 | all17 shipped identities retained |
| guarded production manifest | 299 | 310 | main Java+resources minus excluded V018:290+21-1 |
| complete implementation allowlist | no implementation | 30 paths | 22 new (11 production+11 test), 8 modified (4 production+2 test+CI+POM) |

Current Java/resource counts were enumerated from the filesystem. Current test counts were independently scanned with the shipped `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(` method regex and classified by the parsed Failsafe POM include set: 89/848 and36/592. Source-line regex `@Test\b` gives1441; the only comment is the exact PostgresContainerIT comment preserved in plan §10.1. These are static source/oracle counts, not executed XML results. Planned methods were parsed from the cumulative plan, checked for exact per-class cardinality/uniqueness, and summed to86/38. Neither writer/config changes nor the no-test fixture alter these method sums.

Additional executed static proof: parsed the shipped embedded CI Python AST, evaluated only its literal expected-method maps and duplicate-checked union loops (no XML/container execution), and compared **all125 current FQCN/method sets** with source: exact equality. Projecting the ten planned class/method sets gives exactly97/934 and38/630. Re-evaluated all **18 historical method-total subtraction families** and all **18 historical source-line substitution families**, adding PA38 to each removal set while preserving its existing replacement form; every historical result stays identical. The projected final scan is1565 vs1564 actual methods. Both state tables pass an exact five-column structure check. Temporary static-harness setup errors were corrected before the successful complete run; no product source or oracle was altered to obtain these results.

The current Java manifest algorithm independently yields `299 / 12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1`. The final310-file digest is intentionally **not guessed** before code generation. Plan §10.3 gives the exact non-mutating computation and §10.2 the actual Java constant to update after implementation. There is no aggregate byte field to remove from code or to report; only the erroneous plan claim was removed. The CI literal/diagnostic replacement ledger and full historical method/line chains are authoritative in plan §10.2, not implicit global replacements.


## 4. Exact implementation path allowlist

These are **future implementation** paths, not files edited by this planning task. N=new; M=modified.

- N: `backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java`
- N: `backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java`
- N: `backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java`
- N: `backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/package-info.java`
- N: `backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java`
- N: `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java`
- N: `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java`
- N: `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java`
- N: `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java`
- N: `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java`
- N: `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java`
- M: `backend/src/main/java/com/onecall/core/catalog_order/package-info.java`
- M: `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java`
- M: `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java`
- M: `backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java`
- N: `backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java`
- N: `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java`
- N: `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java`
- N: `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java`
- N: `backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java`
- N: `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java`
- N: `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java`
- N: `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java`
- N: `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java`
- N: `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java`
- N: `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java`
- M: `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`
- M: `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
- M: `.github/workflows/ci.yml`
- M: `backend/pom.xml`

## 5. Exact test identity ledger

The ten new report classes and all124 planned methods (exactly the cumulative-plan §9 sets) are:

`com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPortTests` (8): `readsOnlyStatusRoleRevision`, `isTransactionMandatory`, `usesOperationsIamRole`, `doesNotUseForUpdateOrAdvisoryLock`, `preservesCommandConnectionOnSuccess`, `marksOuterTransactionRollbackOnlyOnFailure`, `mapsMissingEmployeeToUnavailable`, `returnsImmutableSnapshot`.

`com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemanticsTests` (10): `canonicalLockKeyUsesUtf8ProductId`, `dedupeKeysUseCanonicalVersionId`, `publicationReasonIsActionDerived`, `offsaleReasonIsActionDerived`, `publicationSnapshotsContainOnlyFrozenFields`, `offsaleSnapshotsContainOnlyFrozenFields`, `rejectsMalformedPersistedState`, `rejectsRevisionOverflow`, `nullActiveExpectationIsSyntaxOnlyForActiveIdentity`, `preTransitionExpectationIsNotReplay`.

`com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryTests` (12): `readsAuthorityWithoutLocking`, `entersExactOperationsIamRole`, `entersExactCatalogOrderRoleForMutation`, `acquiresProductLockBeforeDecisionTime`, `publicationReadsCandidateAndCurrentActiveUnderLock`, `retiresOldBeforeActivatingCandidate`, `offsaleUsesConditionalRevisionUpdate`, `updatesOnlyLifecycleColumns`, `usesOnePublishAudit`, `usesTwoPublishOutboxesWhenOldActiveExists`, `usesOneOffsaleAuditAndOutbox`, `restoresRoleOnSuccessAndFailure`.

`com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommandTests` (16): `requiresActiveAdminWitness`, `opsMapsToForbidden`, `disabledMapsToForbidden`, `missingAuthorityMapsToServiceUnavailable`, `mapsStaleExpectation`, `mapsUnmatchableStateToConflict`, `replayPerformsZeroWritesAndFacts`, `rollsBackAllFactsOnRepositoryFailure`, `publishCommandHasFrozenFields`, `offsaleCommandHasFrozenFields`, `returnsExactCatalogVersionState`, `usesClosedFailureCodes`, `doesNotExposeRequestReason`, `doesNotExposeDatabaseTypes`, `acceptsNullableExpectedActiveOnly`, `rejectsNonPositiveExpectedRevision`.

`com.onecall.core.operations_iam.internal.web.CatalogVersionRequestBinderTests` (10): `publicationRequiresExactlyTwoFields`, `publicationAcceptsExplicitNullActive`, `offsaleRequiresExactlyOneField`, `rejectsUnknownFields`, `rejectsDuplicateFields`, `rejectsTrailingTokens`, `rejectsQuotedFractionalOverflowAndZeroRevision`, `rejectsNonCanonicalUuid`, `rejectsInvalidUtf8AndOversizedBody`, `rejectsReasonAndIdempotencyFields`.

`com.onecall.archtests.OperationsCatalogVersionWebTests` (14): `admitsExactPublicationPost`, `admitsExactOffsalePost`, `classifiesReadsSubjectsAndCatalogCommandsSeparately`, `catalogCommandsDoNotRequireIdempotencyKey`, `subjectCommandsRetainExactIdempotencyRule`, `authenticationPrecedesStructuralParsing`, `structuralValidationPrecedesAuthorityWitness`, `disabledEmployeeReturnsForbiddenAfterValidParsing`, `opsReturnsForbidden`, `rejectsInvalidContentTypeAndShippedAcceptOutcome`, `successHasExactStateAndHeaders`, `staleViewHasExact409ProblemHeadersAndLength`, `concealsOtherCatalogMethodsAndUris`, `publicationConflictHasExact409ProblemHeadersAndLength`.

`com.onecall.archtests.OperationsCatalogVersionConfigurationTests` (8): `reusesExistingOperationsDatasource`, `usesNewWritableRequiresNewReadCommittedTemplate`, `reusesSamePhysicalDslBinding`, `doesNotCreateSecondPoolOrCredentialFamily`, `resolvesExactlyOneCommandProvider`, `missingProviderFailsClosed`, `ambiguousProviderFailsClosed`, `defaultRuntimeHasNoWriteBeans`.

`com.onecall.archtests.architecture.OperationsCatalogVersionBoundaryTests` (8): `scansExactProductionFiles`, `commandPortIsOperationsIamOwned`, `catalogOrderImplementsNamedPortWithoutReverseEdge`, `employeeAuthorityPortIsOperationsIamOwned`, `catalogSqlNeverReadsEmployeeAccount`, `rejectsSelectStarForbiddenTablesAndContentWrites`, `requiresOneTransactionAndPublicationOrder`, `preservesFrozenInputsAndNoV019`.

`com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT` (24): `adminPublishesWithoutPreviousActive`, `adminPublishesAndRetiresPreviousActive`, `retirementStatementPrecedesActivation`, `publicationIncrementsBothRevisions`, `publicationUsesOneDecisionTimestamp`, `publicationAuditArgumentsAreExact`, `publicationOutboxArgumentsAreExact`, `publicationWithOldActiveHasOneAuditAndTwoOutboxes`, `offsaleTransitionsActive`, `offsaleAuditAndOutboxArgumentsAreExact`, `publicationCurrentStateReplayIsZeroWrite`, `offsaleCurrentStateReplayIsZeroWrite`, `preTransitionPublicationRetryIsStale`, `preTransitionOffsaleRetryIsStale`, `stalePublicationMakesNoChange`, `staleOffsaleMakesNoChange`, `unexpectedStatesMapToConflict`, `publishedContentAndHistoryRemainByteIdentical`, `auditFailureRollsBackLifecycle`, `outboxFailureRollsBackLifecycle`, `concurrentPublishersYieldOneActiveWinner`, `concurrentOffsalesYieldOneWinner`, `oneActiveInvariantHoldsAfterEveryCommit`, `routeMutationOutsideTwoTemplatesIsConcealed`.

`com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT` (14): `disposableLoginHasAdminFalseInheritFalseSetTrueRoles`, `loginCanSetOperationsIamRole`, `loginCanSetCatalogOrderRole`, `operationsIamRoleReadsOnlyAuthorityColumns`, `catalogRoleCannotReadEmployeeAccount`, `catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes`, `v001ThroughV018ProvideAllRequiredObjects`, `noV019IsNeededOrPresent`, `setLocalRoleIsExactAndRestored`, `restorationFailureEvictsDirtyConnection`, `readOnlyOrWrongRoleCannotMutateCatalog`, `publishedTriggerRejectsContentMutation`, `activePartialUniqueIndexHolds`, `historicalRowsRemainUnchangedAfterRepeatedCommands`.

The no-test carrier is `com.onecall.archtests.container.OperationsCatalogVersionLifecycleFixture`: zero annotations/method identities, no CI expected class and no POM include. Planned-name replacements and subcase ownership are enumerated in plan §9.3; no hidden/helper test identities enter totals.

Modified shipped guards retain these exact methods:

- `com.onecall.archtests.architecture.ModuleBoundaryTests` (9): `discoversExactlyTheFrozenModuleSet`, `everyExpectedModuleDeclaresApplicationModule`, `assertsExactAllowedDependencyMapFromPlan62`, `discoveredProductionEdgesStayInsideTheConfiguredMap`, `verifiesNoCyclesOrBoundaryViolations`, `rejectsCycleFixture`, `evidenceMediaReadIsNamedAndInternalImplementationCannotBeImported`, `rejectsForbiddenDependencyFixture`, `rejectsInternalPackageAccessFixture`.
- `com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests` (8): `v018NamesOnlyTheTwoOwnerIssuedAssociationColumns`, `v018GuardsAuthorityHistoryProvenanceAndExactReplayStates`, `v001ThroughV017AndPublicContractsRemainByteFrozen`, `pa25UsesExactEighteenHistoryAndIndependent410TupleOracle`, `pa25DefinesOnlyTheElevenApprovedNonInheritingEdges`, `currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory`, `pa36HasNoApplicationApiPoolOrDependencyExpansion`, `ciPomAndOrdinaryMethodInventoryMatchExactly`.

## 6. Validation commands and evidence status

Executed here: non-mutating filesystem/source/POM inventory scans, planned method/path-count checks, exact current Java manifest recomputation, frozen task-entry file SHA256 comparison, UTF-8/LF/replacement-character/trailing-whitespace/conflict-marker checks, read-only Git identity/diff checks, and all-review/checkpoint hashes. No Maven, Docker, PostgreSQL, Testcontainers, CI job, install or re-review was run. Runtime assertions below remain planned.

Focused implementation Surefire (8 classes/86 tests, both exact409 writer paths, mandatory authority/LO wiring):

```bash
(cd backend && ./mvnw -B -ntp -Dtest='EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests' test)
```

Existing guards (2 classes/17 unchanged methods, manifest310,14 modules/13 edges/5 catalog dependencies):

```bash
(cd backend && ./mvnw -B -ntp -Dtest='ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests' test)
```

Focused Failsafe (2 PA38 IT reports/38 tests, plus full Surefire97/934; fixture no report):

```bash
(cd backend && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify)
```

Full gate (97/934 Surefire,38/630 Failsafe,135/1564 total methods,1565 scan hits,38 exact includes; zero skips/failures/errors):

```bash
(cd backend && ./mvnw -B -ntp -Pcontainer-smoke verify)
```

Plan §11.3 gives the exact follow-up command extracting/executing the complete existing CI Python oracle after full verify; §10.3 gives exact manifest recomputation; §11.4 gives static/frozen verification commands. Focused Failsafe must assert all eleven audit/eight Outbox arguments, fault-stage rollback atomicity, same-PID/transaction role chain, direct23505 recovery/fresh transaction/zero facts, restoration/eviction,42-tuple V004 ACL plus negatives, immutable history, both state/replay matrices and no V019. Do not report a focused XML directory as a full gate.

## 7. Frozen-file proof and hashes

The read-only Git workaround uses `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call25 --work-tree="$PWD"` because the checkout's existing `.git` pointer is Windows-style. No Git metadata was repaired or written. HEAD remains `c9f91337f8bba8e9340ef6239912b44f26373c25`; tree `d225e4de493ad078b12fb3abad8699f557718d8c`. The pre-existing Control edit and private/untracked runner files belong to task-entry state; they are not this task's changes.

Before editing, a read-only SHA256 snapshot covered **1830 protected files** under backend/docs/api/.github (excluding allowed plan paths and generated target/node_modules/.git). After editing all protected path/hash pairs are unchanged, including Control, fix1, all four reviews, source/tests/CI/POM/OpenAPI/migrations and PA25. The sorted `path + space + raw SHA256 + LF` snapshot aggregate remains `b5d207c16e8f1cfac2b2fd43dfde4258a7638c495a129009f5c79bee3599cab5`. This is a task-entry protection snapshot, **not** the product manifest. No other protected path is added/deleted.

Raw frozen/checkpoint hashes (unchanged from task entry):

| Path | SHA256 |
| --- | --- |
| `docs/gateflow/project-execution-control.md` | `c936ecc912f68eaa35d2a1e895912721d7e6d3273fce82b79c71b084a27a1b94` |
| `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan-fix-1.md` | `3527b912f45e0a89bebea97f114eb8e928c39e104d316930a12b3752a78d2d2a` |
| `docs/reviews/plan-review-20261118-064733.md` | `5a3467ba0eb78068f61cbeffbc5bb66fbfa6790be86cab9d821a4b94e21e3d6b` |
| `docs/reviews/plan-review-20261118-082009.md` | `f13e9f79432ad4373024c1b29fb33facc0f005915033dfbc2becf635c476bd0f` |
| `docs/reviews/plan-review-20261118-085416.md` | `d0876168ef7e197088b93e5ff931057e536a43bbe3847de972fc78914fc7735e` |
| `docs/reviews/plan-review-20261118-092912.md` | `797d5433b2d3b3375c0e7f28cb721e6065df41f8e84285344316147847d10303` |
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` |
| `docs/12-technology-stack-and-system-architecture.md` | `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` |
| `docs/17-production-security-design-spec.md` | `b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205` |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9` |
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
| `backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql` | `ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c` |

Artifact SHA256s are reported in the completion response after both files are finalized; no self-referential hash or fabricated future implementation digest is stored here. This artifact adds no product-manifest size field.

## 8. Blocking questions and next gate

**Blocking open questions: none** within the Controller-confirmed scope. LO enablement/production secret delivery and pool health remain Platform/Security/Operations deployment residuals; broad frozen catalog ACL remains Database/Security +Catalog-order residual, not least privilege; lock-free authority timing remains Security-owned; audit/Outbox dispatch/retention remains Platform/Operations; future drafts/customer filtering/UI stay outside this slice. Any newly required public/schema/ACL change stops for Controller goal confirmation.

Next gate: **Controller intake, then fresh independent A/B plan re-review**. This author stops after the two artifacts and static checks; does not dispatch re-review, implement, stage, commit, push, mutate PR/issues, approve, mark ready, request reviewers, comment externally or install dependencies.
