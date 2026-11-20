# PA38-S1 code-review fix1

## Binding and status

- Task `pa38-operations-catalog-sale-lifecycle-code-review-fix1-gpt-20261119-02`; November 19, 2026, Asia/Shanghai; bounded author only.
- Inputs: accepted Controller intake/corrected profile matrix in Control; unchanged accepted PA38 plan; implementation evidence; reviews A `code-review-20261118-225316.md` and B `code-review-20261118-225837.md` under `/tmp/pa38-code-review.72bxM1/{a,b}/docs/reviews/`. No whole-unit re-review.
- Primary root `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`; unchanged HEAD `ca90b0793f615a370ec66334fd196653beb358f4`, tree `f716066ac00f19bf6874060be343a99b2be55aa3`, branch `codex/pa38-next-work-unit`.
- **Status bounded-code-fix-complete**. All twelve accepted corrections, all focused gates, clean full verify and its unchanged shipped CI/XML oracle pass, with zero failures/errors/skips. Next gate **re-review**, not entered or requested by this author.
- Two routes, schema, OpenAPI, migrations, public DTO/error vocabulary, POM includes and CI/test identities remain unchanged. No method is added, deleted, renamed, disabled or skipped; no new test/production Java class or source file is added (the explicit non-test harness lives in an existing file).

## Finding closures

### PA38-CODE-REV-01 — closed in code and focused evidence

`backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java:184` uses only semantics-owned dedupe; audit shares that owner. `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java:31` / `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java:37` pin both full five-bind arrays, fixed SQL argument order, literal NULL ciphertext, literal six-field payload and independent SHA-256. Distinct retirement version number/hash and pre-row timestamp distinguish it from the candidate decision-time payload. `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:31` applies the same exact PostgreSQL evidence to candidate, retirement and real offsale; offsale publication time is captured before mutation, with audit/Outbox identity equality.

### PA38-CODE-REV-02 — closed in code and focused evidence

`backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java:79` runs nine real contexts registering both owning configurations. Property absent/false under default/web/persistence: no read or write definitions; true under default/web: none; true under persistence: both read plus all three write definitions. Lazy/non-singleton assertions avoid external datasource instantiation. Exact qualifiers remain; frozen subject-read tests pass 8/8.

### PA38-CODE-REV-03 — closed in code and focused evidence

`backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java:113` first pins literal UTF-8 JSON bytes/field order and publishedAt `2026-11-18T00:00:00Z`, not mapper-derived expected JSON. Existing status/headers/Content-Length and nullable/other-active cases remain.

### PA38-CODE-REV-04 — closed in code and focused evidence

`backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:17` asserts candidate currentActiveVersionId in both zero-benefit and existing-benefit publication branches, retaining immutable content and exact facts.

### PA38-CODE-REV-05 — closed in code and focused evidence

`backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:75` starts a persistent prepared admin observer and completes its first query before submitting the blocked command. It observes the actual wait; unchanged 250 ms lock timeout produces real 55P03/409. Same-pool concurrent read, unchanged lifecycle, zero facts and session cleanup remain mandatory.

### PA38-CODE-REV-06 — closed in code and focused evidence

`backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java:110` binds the canonical full key into explicit CAST(? AS text). `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java:30` verifies actual repository bind and rendered PostgreSQL SQL, not a helper tautology; `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:26` pins the actual successful prepared PostgreSQL statement/bind with effective key catalog_order.publication:<UUID>.

### PA38-CODE-REV-07 — closed in code and focused evidence

`backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java:128` uses the command-interface constant in the real named-interface assertion. The dead migration sentinel is removed from EvidenceRoleIvrAssociationAclBoundaryTests; existing migration/no-V019 tests remain. Guard identities stay 9+8.

### PA38-CODE-REV-08 — closed in code and focused evidence

`backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java:11` has no ineffective Profile/Lazy annotations or unused component import. `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java:53` registers real configuration/repository with exact qualified template/datasource/DSL definitions, resolves exactly one command, then registers a second provider and asserts closed ambiguity failure. No production visibility or qualifier widening.

### PA38-CODE-REV-09 — closed in code and focused evidence

`backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java:9` removes unreachable FORBIDDEN; `backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java:12` retains only a validated five-character SQLSTATE witness, never the original cause/message. `backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java:39` traverses caught SQLException/jOOQ causes. `backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java:57` proves 42501 diagnostics, sanitized toString/no cause and rollback ownership. `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java:58` proves public command 503 does not leak the witness/details.

### PA38-CODE-REV-10 — closed in code and focused evidence

`backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java:22` now checks executed SQL: no employee-account/IAM-table SQL or FOR UPDATE. `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java:43` preserves ordered authority delegation before catalog role/mutation/restoration. `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java:40` and `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java:46` assert exact ACTION_DERIVED audit SQL/eight binds and exact Outbox payloads. `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java:36` is an explicit non-test harness used directly with cleanup, never another test instance/JUnit setup lifecycle.

### PA38-CODE-REV-11 — closed in code and focused evidence

`backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java:58` and the following injected-40001 subcase each commit one bypass competitor, then call `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java:125`. Assertions: exactly two distinct transaction IDs; recovery login → exact catalog_order_role → NONE/login; no recovery authority/mutation/lock/fact SQL; one authority read, one activation attempt and one lock overall; zero failed-command facts; exactly one committed ACTIVE row with competitor ID; pool/session cleanup.

### PA38-CODE-REV-CTRL-01 — closed in code and focused evidence

`backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java:53` catches construction/provider failures only within provider resolution and maps to sanitized SERVICE_UNAVAILABLE. `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java:71` exercises real lazy Spring construction failure; `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java:97` proves both routes: malformed 422, missing/ambiguous/construction 503 with no leaks, and unrelated command processing exception still 500 INTERNAL.

## Changed paths

This author changes 15 allowed existing source/test paths, the implementation report and this new artifact (17 public paths). Complete public work-unit diff must be original 32 candidate paths + this artifact + Controller-owned Control = **34**, zero tracked deletions. No staging. Private exclusions apply only to untracked paths, never tracked paths.

```text
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java
backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java
backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md
docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-1.md
```

Control remains author-untouched, SHA256 `cd56fbb8c9c484307cd6015413d5fb4a1c00d2df4c07f4e764edf7bbc49c5516`. Final public changed-path/deletion audit: **PASS, exit 0: exact 34 public paths, zero tracked deletions, zero staged paths**.

## Environment and projection custody

- Maven `/opt/maven-3.9.9/bin/mvn -B -ntp`, **3.9.9**; `JAVA_HOME=/opt/jdk21`, PATH prefixed `/opt/jdk21/bin`, Temurin **21.0.13**. No wrapper/fallback or weakened test.
- Final standalone preflight: `mvn -v`; `docker version --format '{{.Client.Version}}'`; `docker version --format '{{.Server.Version}}'`; `docker info --format '{{.ServerVersion}}|{{.OperatingSystem}}|{{.KernelVersion}}'`. Each actual exit **0**; Client **29.1.3**, explicitly **Server field 29.1.3**, server-backed info `29.1.3|Ubuntu 22.04.5 LTS|6.18.33.2-microsoft-standard-WSL2`. `/tmp/pa38-code-fix1-final-preflight.log` contains all outputs/exits. Flyway observes actual **PostgreSQL 16.15** in live `postgres:16` tests.
- Accepted plan §11.0 setup/preflight Bash blocks execute verbatim after extraction. Independent Git-less projection excludes `.git`, `.codex`, `.pa38-*`, `.pa*-*`, `target`, `tmp`, `node_modules`; verifies exact initial byte inventories; normalizes only the single PA35 plan's 1,344 CRLF lines in the projection, never primary inputs.
- Retired setup-only root `/tmp/pa38-lf-validation.QZulK9/candidate`, inventory `440f7098561db3b2827c89379884e8a8cf1a79382e5d9790dc48184b5a2ce193`; setup exit 0, `/tmp/pa38-code-fix1-setup.log`. Retired because exact PostgreSQL argument assertions changed the two allowed LifecycleIT/fixture files. No canonical acceptance from that root.
- **Single final root** `/tmp/pa38-lf-validation.G2fHfN/candidate`, inventory `d536aa3fd7fd1d511c81d833194c21bcbfcff87d825d0ecb58723fe9bd2a95fe`; setup exit 0, `/tmp/pa38-code-fix1-final-setup.log`. All final preflight/smoke/focused/full/oracle/XML evidence comes from it. Only report/fix-artifact bookkeeping follows in primary; final audit compares all 31 implementation files byte-for-byte against this root.
- Frozen primary PA35 SHA256 `d5ba84e34124e0cb44fd5a6c8fdc008a8894c3dee472268997d6a4f02500de55`; projection LF SHA256 `58ec889fe812cd1e0c439a7d3c1054aa41bfe8bd44bcd27183512fbe3ffd85d3`. All other projection inputs unchanged.

## Commands, exits and totals

Commands run from final projection `backend`, with the launcher/JDK above. `M` means exactly `/opt/maven-3.9.9/bin/mvn -B -ntp`. Every final completed test gate has zero failures/errors/skips.

| Gate | Exact arguments after M | Exit | Classes/methods | Complete stdout/stderr |
| --- | --- | --- | --- | --- |
| Smoke | `-Dtest=OperationsSubjectReadConfigurationTests test` | 0 | 1/8 | `/tmp/pa38-code-fix1-final-smoke.log` |
| PA38 units | `-Dtest=EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests test` | 0 | 8/86 | `/tmp/pa38-code-fix1-final-focused86.log` |
| Existing guards | `-Dtest=ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test` | 0 | 2/17 = 9+8 | `/tmp/pa38-code-fix1-final-guards17.log` |
| Frozen regressions | `-Dtest=OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectReadBoundaryTests test` | 0 | 6/52 | `/tmp/pa38-code-fix1-final-regression52.log` |
| Live focused PostgreSQL | `-Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify` | 0 | Surefire 97/934; Failsafe 2/38 = 24+14 | `/tmp/pa38-code-fix1-final-failsafe38.log` |
| Clean full + same-shell CI/XML custody | `clean -Pcontainer-smoke verify && python3 - <<'PY'` (body below) | 0 | Surefire 97/934; Failsafe 38/630; oracle 0; zero F/E/S | `/tmp/pa38-code-fix1-final-clean-full-oracle.log` |
| Static/frozen/path/hash audit | exact process-local Git commands + Python assertions below | 0 | 34 paths; zero deletions; 310 manifest; all frozen | `/tmp/pa38-code-fix1-final-static.log` |
| Whitespace | process-local `git diff --check` | 0 | zero whitespace errors | `/tmp/pa38-code-fix1-final-static.log` |

### Preserved diagnostics, not acceptance

- Initial plain `git status --short` exits **128** because the unchanged Windows `.git` pointer is not POSIX-resolvable. All acceptance Git uses process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, primary `GIT_WORK_TREE`, `GIT_OPTIONAL_LOCKS=0`, `git -c core.safecrlf=false`. No gitdir/config/index repair.
- Primary initial `M -f backend/pom.xml` plus the same focused eight-class selector and `test`, exit **1**, compile-only private-repository import failure; `/tmp/pa38-code-fix1-focused-initial.log`. Corrected in the allowed configuration test with real context `Class.forName` registration, no production visibility widening. Primary diagnostic rerun exits **0**, 86/86, `/tmp/pa38-code-fix1-focused-diagnostic2.log`, before final added subcases; not canonical acceptance.
- First local static helper assertion exits **1** because its untracked-only private predicate omitted plain `.codex` (it handled `.codex/`); subsequent display-only tail returned shell 0. `/tmp/pa38-code-fix1-static-before-report.log`. Corrected helper predicate includes that existing untracked marker, never filters tracked paths or weakens public expected paths. `/tmp/pa38-code-fix1-static-current.log` then passes exact original 32 + Control, zero tracked deletions, exit 0 before the artifact is added. Final audit includes the artifact too.
- Final preflight/smoke are honest current post-edit measurements, not claimed to precede source edits. Earlier implementation preflight was inspected first. Documentation generation had two tool-only exit-1 attempts: a wrong lookup prefix before editing, and an unsupported generated patch hunk header rejected without a write; corrected lookup/hunk formatting resolved both, without changing source/test acceptance. No failed diagnostic is counted as acceptance or resolved by weaker tests.

### Full clean-sweep CI/XML custody

Maven and the following Python body execute in one shell, chained by `&&`, without intervening Maven selectors or copied XML. The shipped CI heredoc is executed unmodified:

```python
from pathlib import Path
from textwrap import dedent
from hashlib import sha256
workflow = Path('../.github/workflows/ci.yml').read_text()
marker = "          python3 - <<'PY'\n"
start = workflow.index(marker) + len(marker)
end = workflow.index('\n          PY', start)
exec(compile(dedent(workflow[start:end]), 'ci-container-inventory', 'exec'))
reports = sorted([*Path('target/surefire-reports').glob('TEST-*.xml'),
                  *Path('target/failsafe-reports').glob('TEST-*.xml')],
                 key=lambda path: path.as_posix())
assert sum(path.parent.name == 'surefire-reports' for path in reports) == 97
assert sum(path.parent.name == 'failsafe-reports' for path in reports) == 38
inventory = {path.as_posix(): sha256(path.read_bytes()).hexdigest() for path in reports}
binding = ''.join(name + ' ' + digest + '\n' for name, digest in inventory.items())
print(binding, end='')
print('FINAL_XML_COUNT', len(inventory))
print('FINAL_XML_AGGREGATE', sha256(binding.encode()).hexdigest())
for name in ['OperationsCatalogVersionLifecycleIT', 'OperationsCatalogVersionRoleLeastPrivilegeIT']:
    filename = 'target/failsafe-reports/TEST-com.onecall.archtests.container.' + name + '.xml'
    print('PA38_XML', filename, inventory[filename])
```

Final XML custody: **135 XML files**, deterministic filename/raw-SHA256/LF aggregate **`fb061f40eafd4e13a22e426fba4e38ca046acadbb562ef8b00bd3bf3a56748ef`**. Clean verify exit **0**, runtime **27:57**, finish **2026-11-19T09:57:57+08:00**; same-shell shipped oracle exits **0**, exact `97/934 + 38/630 = 135/1564`, scan 1,565, POM includes 38, zero failures/errors/skips.

```text
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml 6b0d06d2581d0c8d03eba439243cbdf63378973b0475d11bbc4be0944351a06f
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml dd55e0332072873f548aaaa71b1d6bc7904e0cdefa92c221b3d8401b139ec9e8
```. XML stays generated build output, not a public changed path.

## Exact test identities

These source method-name sequences equal review A's frozen candidate exactly. Final shipped oracle checks source, unchanged CI maps, unchanged POM set and clean-sweep XML. Complete confirmed inventory **97/934 Surefire + 38/630 Failsafe = 135/1,564**, source scan **1,565** (only the historical comment hit), Java source files **290 production / 180 test**. PA38 remains **8/86 + 2/38**.

### OperationsCatalogVersionConfigurationTests — 8

`backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java`

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

### OperationsCatalogVersionWebTests — 14

`backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java`

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

### OperationsCatalogVersionBoundaryTests — 8

`backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java`

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

`backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java`

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

`backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java`

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

### CatalogSaleLifecycleRepositoryTests — 12

`backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java`

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

### CatalogSaleLifecycleSemanticsTests — 10

`backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java`

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

### DefaultOperationsCatalogSaleCommandTests — 16

`backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java`

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

### EmployeeAuthorityReadPortTests — 8

`backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java`

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

### CatalogVersionRequestBinderTests — 10

`backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java`

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

## Frozen-input and public-path audit

Process-local Git prefix: `env GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25 GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call GIT_OPTIONAL_LOCKS=0 git -c core.safecrlf=false`.

Commands after that prefix: `rev-parse HEAD`; `rev-parse HEAD^{tree}`; `branch --show-current`; `diff --check`; `diff --diff-filter=D HEAD --name-only`; `diff --name-only HEAD -z`; `ls-files --others --exclude-standard -z`; `diff --exit-code HEAD --` the six frozen paths and migration directory. Python asserts exact public tracked/untracked union = original 32 + fix artifact + Control, compares all test identities to review A, all 31 implementation bytes to final projection, Control pin, and no tracked deletion independently. Private exclusions are applied to untracked inputs only.

Six raw pins, unchanged before/after:

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db  docs/12-technology-stack-and-system-architecture.md
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205  docs/17-production-security-design-spec.md
b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9  backend/ops/postgresql/pa25-runtime-identities.sql
```

Exactly 18 migration files V001–V018 in numeric order, no V019; raw path/hash/LF aggregate **`53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`**. Real PostgreSQL validates the unchanged 18-row history too.

Production manifest: all regular backend `src/main/java`/`src/main/resources` files except V018's filename, paths relative to backend, CRLF→LF content hashing, sorted path/hash rows with trailing LF. Recomputed count **310**, digest **`e9de000fabce960d271e819e870390bcb151e8b03b1d7292c43b5c0b391e65d8`**, pinned by the existing guard, actually passed. No nonexistent manifest byte-count claim.

## Post-fix 32-path candidate inventory

Algorithm: ordinal path order, UTF-8 `relative/path<single-space><lowercase raw SHA256>` rows, LF joining and one final LF. Includes the current report, excludes this artifact and Control; no self-referential artifact hash. **32 paths / 533,173 raw bytes / aggregate `457182309ba3eca967fb7c5dd836f0defa15b363282599378e203fd88aaea340`**.

```text
.github/workflows/ci.yml 11dfd54c9c0c797ea30142d2a0a614df6385c724f54a343f6bfc5a2b8677209a
backend/pom.xml f8fc1389d46a0af34ebb8b538947c6571211b44a0e19be7875ab3923dfee0fb8
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java 5ae7933e32a200e1efc6c66c0d79c1568e382f8767eef9f3eebaff24f8b2dc40
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java 360290131c6d32200bb725627fb3268222ec755012b8dd8b8ffef44c60ae9f29
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java fb4b3f0834bb55f15f230b916aaea0a40b34943de71040d0ce0af2cceb001640
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java 0939a2ce0ffdcef1947794dcf9a5c5fcc324e83cb9119354148a0ded9592032e
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
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java 631a13be9a2ba6fd9f2fb79eb9b240190cadf83bb6dff0bd4970fa693a5329cc
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java bc3c6ccbd7d0426690b78e9ec1fe775687a9fd4562324ba941e3bb4ef0cd614f
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java 4646b6de23b81353a65feff6fed469486b4ee6c245eec2246b7f79e9da71096a
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java 2177e6913a5b1308b72457edfe242788dd353f240d7dcb96d54087071b4d859e
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java 9d32f9b2a2a90f2101273936536c6760d10579fd1b7c8844ecd5310c2ec1ea86
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java c83930bcfda5728defc18cb08b437a48ed274d0fc6dee1bf54e98ec32ce8ddc4
backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java 0c53ae76efda5b34dda851bd93a88e720644e767d5270758df327dace6a4f4a9
backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java 06519b1ac4a2fa46ffb37585990e4c3aa11cf35f6be393344bea17ad47be5c21
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md c8a34be41a7aa84642d2ef7460c8a3a25bb990265b24504a234691e4476b6686
```

## Final log hashes

```text
/tmp/pa38-code-fix1-final-clean-full-oracle.log 8d0d44a73c343cd047d468ea71688b8cb45e57ef140d538d7ffa7f722b25ef76
/tmp/pa38-code-fix1-final-failsafe38.log 85237ddae09742d30460c89c10c1a0dd1041b145b15c9f69d3e73f1c70d68412
/tmp/pa38-code-fix1-final-focused86.log 3840ad7ac3de3db00d449c5c43dd744d7316c407814ca7ecf68ec02ded683351
/tmp/pa38-code-fix1-final-guards17.log e5c17f108a04e3eb9ff69c5a396771e4bef3a167c19309db761ddccb33e12781
/tmp/pa38-code-fix1-final-preflight.log 5b53c2352a64e158fe6a948d8c538c5d2b8e76fe3261ed632c4f636feb285482
/tmp/pa38-code-fix1-final-regression52.log f4265b29e46d6008df0241f827b9a4d48776260f6e331c5ad8e4a15e69d9a1b3
/tmp/pa38-code-fix1-final-setup.log 6d684cfc7f841b52fdfea908d04aa819c255ed6a6d5769bf3c671f7a5710e65e
/tmp/pa38-code-fix1-final-smoke.log 1ab7c2efb9a3b59174ee095c075c73e7f15cd70e0a1cc0583e9d8f25f9970230
/tmp/pa38-code-fix1-final-static.log 486ba9ec786e78f9a6174e707721c919484316f73ba5a4c26f7c6d9b77e3e1db
```

## Residual risks and owner/destination

| Owner | Destination |
| --- | --- |
| Backend + Platform | Deployment readiness: runtime enablement/credentials and shared four-slot pool saturation; no production enablement claim or new pool. |
| Database/security owner | Existing frozen broad catalog grants; retain V004 42-tuple matrix/negative probes, not a newly hardened least-privilege claim. |
| Audit/Outbox owner | Existing retention/dispatch/observability lifecycle; atomic exact facts do not claim delivery completion. |
| Catalog/UI owners | Draft editing, confirmation UX and other mutations remain outside this two-route slice. |
| Backend test-boundary + Controller | Primary PA35 CRLF remains frozen; accepted LF projection alone establishes complete runtime acceptance. |
| Phaseflow/Gateflow tooling | Deferred CTRL-02 is a future gate-script hardening unit. This fix's independent tracked-deletion assertion and untracked-only private filter show zero current deletion; accepted plan/helper untouched. |

No correctness residual substitutes for a required assertion. Any unexpected final gate failure blocks completion. **Final status bounded-code-fix-complete; next gate re-review, not entered.** No staging, commit, push, branch, PR, merge, approval, mark-ready, reviewer request, external comment or issue mutation.
