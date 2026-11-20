# PA38-S1 manifest lockstep fix3

## Binding and final disposition

- Task: `pa38-operations-catalog-sale-lifecycle-code-review-fix3-gpt-20261119-01`; November 19, 2026, Asia/Shanghai; bounded author, not acceptance authority.
- Scope: close only Controller finding **`PA38-FIX2-PIN-01`**. Only the existing manifest carrier, implementation report and this new artifact change in this task.
- Primary root: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`. HEAD `ca90b0793f615a370ec66334fd196653beb358f4`, tree `f716066ac00f19bf6874060be343a99b2be55aa3`, branch `codex/pa38-next-work-unit`, all unchanged. The Windows `.git` pointer is resolved process-locally with `wslpath -u`; no repository/configuration repair or index mutation occurs.
- **Final status: bounded fix3 complete; `PA38-FIX2-PIN-01` closed in the candidate with all requested validation green. Next gate: `re-review`.** No independent acceptance, production readiness, approval or entry/request of that gate is claimed.

## Exact reason and closure

Accepted fix2 removed only the manually constructed command's dead constructor `@Qualifier` and import; the effective configuration bean-method qualifier remained. Those authorized production bytes correctly changed the production manifest, but the then-out-of-allowlist carrier retained the prior digest. Controller explicitly authorized lockstep synchronization in Control's PA38 intake. This was an evidence-pin mismatch, not a PostgreSQL/product regression or the deferred PA35 CRLF issue.

`backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java:102` changes exactly the current manifest digest literal:

```text
before e9de000fabce960d271e819e870390bcb151e8b03b1d7292c43b5c0b391e65d8
after  3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0
count  310 -> 310
```

The pre-edit carrier is retained outside the candidate at `guard-before.java` in the evidence directory. A byte-for-byte assertion proves `before.replace(old_digest, new_digest) == current`; the old literal occurs exactly once. Every other byte, mixed existing line ending, assertion/hash, guard and `@Test` identity remains intact. No guard is removed or weakened. The implementation report now leads with fix3 evidence/current pins and explicitly labels fix1/fix2 results and prior hashes as historical provenance.

### 310-file digest algorithm

From `backend`, recursively enumerate regular files beneath `src/main/java` and `src/main/resources`, excluding only files whose filename is `V018__evidence_role_ivr_association_read_privileges.sql`. For each file normalize **CRLF to LF only**, SHA256 those bytes to lowercase hexadecimal, then form `backend-relative POSIX path + one ASCII space + hash`. Sort rows in ordinal order, join with LF and append exactly one final LF; SHA256 the UTF-8 serialization. Result: **310 / `3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0`**. Rows are retained in `manifest.txt`. Replacing only the command's row hash with preserved fix1 bytes reproduces the old `e9de...` digest without modifying any file, independently attributing the change to the accepted metadata removal. Neither count nor manifest algorithm changes.

## Validation root and execution

- One fresh accepted LF projection: `/tmp/pa38-lf-validation.SzsubC/candidate`; setup inventory SHA256 `ad528f468803c5c4c24e836679bb7adc3ff81639f175e021aecbd86ce70d3b14`. It supersedes the fix2 projection because authorized carrier bytes changed. All fix3 gates use this one root, launcher and environment; no acceptance evidence is spliced from fix1/fix2.
- Setup copies the stable candidate excluding `.git`, `.codex`, `.pa38-*`, general `.pa*-*`, `target`, `tmp`, `node_modules` at every depth. It asserts initial byte equality and unchanged primary inventory. Only the PA35 plan's 1,344 CRLF lines are normalized in the projection: raw `d5ba84e34124e0cb44fd5a6c8fdc008a8894c3dee472268997d6a4f02500de55`, LF `58ec889fe812cd1e0c439a7d3c1054aa41bfe8bd44bcd27183512fbe3ffd85d3`. Primary frozen bytes remain untouched.
- Every primary Java file is byte-identical to the tested projection, including the changed carrier. Final report/artifact documentation is completed after execution, so the setup inventory is an execution-input binding, not a self-referential final documentation hash. Current final raw candidate custody is below.
- `JAVA_HOME=/opt/jdk21`, `/opt/jdk21/bin` first on PATH; all Maven commands use `/opt/maven-3.9.9/bin/mvn -B -ntp`. Preflight `/opt/maven-3.9.9/bin/mvn -B -ntp -v`, `docker version --format '{{.Client.Version}}'`, `docker version --format '{{.Server.Version}}'`, `docker info --format '{{.ServerVersion}}|{{.OperatingSystem}}|{{.KernelVersion}}'` each exit **0**. Maven 3.9.9, Eclipse Adoptium Java 21.0.13 at `/opt/jdk21`; Docker Client 29.1.3 and separately observed **Server field 29.1.3**, server-backed info reachable. Tests use real Testcontainers PostgreSQL **16.15**.
- Raw commands, exit sidecars, logs, per-run immutable XML snapshots and audits live under `/tmp/pa38-fix3-evidence/`. `run-gates.sh` uses fail-fast `set -euo pipefail`; there is no Maven failure retry, waived failure or skipped test.

| Gate | Exit | Actual classes / tests; failures/errors/skips |
| --- | --- | --- |
| Existing configuration smoke | 0 | 1/8; 0/0/0 |
| Existing guards | 0 | 2/17 = 9+8; 0/0/0 |
| Focused PA38 unit suite | 0 | 8/86; 0/0/0 |
| Canonical two-class PostgreSQL verify | 0 | Surefire 97/934 + Failsafe 2/38 = 24+14; 0/0/0 |
| Exact observer method, three sequential separate executions | 0 / 0 / 0 | 1/1 each; 0/0/0 |
| Canonical clean full verify | 0 | Surefire 97/934 + Failsafe 38/630 = 135/1564; 0/0/0 |
| Same-shell unchanged shipped CI/XML oracle and custody | 0 | 135 XML files; source=CI=POM=XML; line scan 1565; POM 38 includes |
| Final static/frozen/path audit and XML audit | 0 / 0 | Java 290/180; identities 8/86 + 2/38; guards 9+8 |

Exact executed Maven selectors, from `/tmp/pa38-lf-validation.SzsubC/candidate/backend`:

```text
smoke: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=OperationsSubjectReadConfigurationTests test
guards17: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=ModuleBoundaryTests\,EvidenceRoleIvrAssociationAclBoundaryTests test
focused86: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=EmployeeAuthorityReadPortTests\,CatalogSaleLifecycleSemanticsTests\,CatalogSaleLifecycleRepositoryTests\,DefaultOperationsCatalogSaleCommandTests\,CatalogVersionRequestBinderTests\,OperationsCatalogVersionWebTests\,OperationsCatalogVersionConfigurationTests\,OperationsCatalogVersionBoundaryTests test
failsafe38: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT\,OperationsCatalogVersionRoleLeastPrivilegeIT verify
observer-repeat-1: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
observer-repeat-2: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
observer-repeat-3: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
clean-full: exit 0
/opt/maven-3.9.9/bin/mvn -B -ntp clean -Pcontainer-smoke verify
```

After that last command in the **same shell and same `&&` chain**, the accepted plan §11.3 block extracts the first unchanged `python3 - <<'PY'` heredoc from `.github/workflows/ci.yml` and executes its dedented contents directly. Exact script: `/tmp/pa38-fix3-evidence/clean-full-oracle.sh` (extracted unchanged from the accepted plan); the combined script exits 0. It immediately computes XML custody against that clean sweep's own `target`, not the focused/repeated snapshots. No CI, POM, test include or expected total changes. The log explicitly prints the shipped `PA38 exact source=CI=POM=XML inventory passed: 97/934 + 38/630 = 135/1564` summary.

The extra audits execute `python3 /tmp/pa38-fix3-evidence/static-audit.py` and `python3 /tmp/pa38-fix3-evidence/xml-audit.py`, both exit 0. Source identities match the unchanged literal CI PA38 maps and preserved fix1 source; source files remain 290 production / 180 test Java files. All source/CI/POM/XML global identities are checked by the shipped oracle. The repeated exact-PID observer retains real 250ms lock timeout, real 55P03/409, unchanged state, zero facts/writes, concurrent same-pool read and session-restoration backstops; no observer source change is made in fix3.

### Supplemental audit diagnostic

The first stricter temporary static audit exited **1** because `git ls-files --others` (deliberately including ignored files) additionally reported pre-existing ignored `build/generated-spring-modulith/javadoc.json`. Prior fix2's `--exclude-standard` had omitted it. The temporary audit was corrected to inventory it separately, require its starting raw hash `1ecdbab69d44df83b514765cf309e503971b296026a45b5ae636fa497914f817` unchanged and require `git check-ignore` confirmation (`.gitignore:2:build/`). It is not silently excluded as a candidate change, deleted or edited. Corrected and final audits exit 0. This is a custody-classification diagnostic, not a failed Maven/product gate; both transcripts remain. No shipped guard/CI/POM was changed to accommodate it.

## Clean-sweep XML custody

Exactly **97 Surefire + 38 Failsafe = 135 XML files**, all zero failures/errors/skips. Enumerate `target/surefire-reports/TEST-*.xml` and `target/failsafe-reports/TEST-*.xml`, sort backend-relative POSIX filenames ordinally, SHA256 each file's raw bytes, serialize `filename + ASCII space + lowercase hash + LF` including final LF, UTF-8 encode and SHA256. Inventory: `xml135.sha256`; aggregate **`f187461a6e0d3d1ac4e29be8fc2e0cff45b9e738e317012ceb6192d4a0549cb0`**. The identical aggregate is printed by the same-shell plan block and independently checked by `xml-audit.py`.

```text
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml 7983ca4907e4296107d8a376c8ffa629ce42f843262f3935bdc5135d6da62dee
target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml 1a2711e76231b0c147521b34139821d5e7ea7c7d00f17ad5d1b5d77361822af1
```

Per-gate snapshots and XML testcase identities/hashes are retained in `test-summary.json`. Full-class PA38 XML is separate from all three method-only observer snapshots; no method-only XML substitutes for the clean full totals. The method-only Failsafe invocations do not rerun Surefire: their snapshot directories also retain the preceding 97 Surefire XML files, which are not counted as repeat executions or used for final acceptance; each repeat executes exactly the selected one Failsafe method.

## Frozen inputs and no V019

Before/after raw-byte inventory and resolved `git diff --exit-code HEAD --` confirm unchanged OpenAPI, docs12/14/15/17, PA25 and migration paths. All FROZEN guard rows remain raw/LF-valid. Exactly 18 migrations V001–V018, no V019 anywhere. Migration raw path/hash/LF ordinal aggregate remains **`53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`**. No primary PA35 plan, migration, schema/ACL, credentials, pool, public contract, CI or POM changes.

```text
api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68
docs/12-technology-stack-and-system-architecture.md 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db
docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31
docs/15-production-openapi-surface-spec.md db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0
docs/17-production-security-design-spec.md b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205
backend/ops/postgresql/pa25-runtime-identities.sql b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9
```

## Current original 32-path candidate custody

Exactly the original **32** candidate paths, including the final updated implementation report: **544,750 raw bytes**, ordinal path/raw-SHA256/LF aggregate **`e68d4006ac0cfc070882f12ca0086042b9664e25ee6b8818cba7db3c6e400d39`**. This is distinct from the production manifest: it includes tests, CI/POM and report and does **not** normalize file bytes. Algorithm: sort candidate-relative POSIX paths ordinally, SHA256 each raw file to lowercase hex, serialize `path + ASCII space + hash + LF` with a final LF, UTF-8 encode and SHA256. The byte total is the sum of the 32 raw file sizes. This report is in the original 32; this new fix3 artifact is not. `candidate32.txt` is regenerated by the final static audit.

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
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java 02931eff38612241830a3e4d99f936c42e2c4d673ae865d7a9bd8325104b3520
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
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md cca3435d98a5a3ac8ddcbb020eb223017badc3dc7d76f15ceb7c5956d9f7c0ee
```

### Public work-unit audit

Original 32 + fix1/fix2/fix3 artifacts + Controller-owned Control = **36** candidate/bookkeeping paths. The exact resolved Git tracked-against-HEAD plus all-untracked set is accounted for, with **zero tracked deletions**, **zero staged paths**, unchanged HEAD/tree/branch and `git diff --check` exit 0. No tracked private `.pa38-*`/`.codex` path is included. Private/generated exclusions follow the accepted setup; the four pre-existing independent review artifacts are retained, byte-unchanged and separately reported rather than silently counted as new candidate edits. Including those four reviews and the pre-existing ignored generated build JSON gives **41 raw non-private status paths**, explicitly reconciled to the **36** work-unit paths. Full 36-path listing: `public-paths.txt`.

Only the three authorized paths change against this task's starting inventory. All production, other tests/docs/plans, CI/POM, reviews, previous fix artifacts and Control are unchanged. Additional current bindings:

```text
docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-1.md 356852b572409570a06c9701b23e4ff559e3bc38830c3224dc601ea61030dcff
docs/gateflow/pa38-operations-catalog-sale-lifecycle-code-review-fix-2.md 530946083685fad6c9448d49e7f4f487fffae0d96b49c4b80adda7d737dd110e
docs/gateflow/project-execution-control.md 9a360fbfdc9d21fc22e03a3b6d3acf79fcecc5018f5624e6d8c39050a94b19f4
```

The new fix3 artifact's own final raw SHA256 and byte size are written to the separate local `custody.sha256` / `final-custody.json` after creation, avoiding a self-hash cycle. The implementation report's final raw hash is already in the 32-path inventory above; the author-time Control pin can only be superseded by later Controller bookkeeping, not an author edit.

## Preserved test identities

Guard 9+8, PA38 unit 8 classes/86 methods, PA38 PostgreSQL 2 classes/38 methods: no addition, deletion, rename, disablement, skip or weakened assertion. Names below are source-extracted; all PA38 names are set-equal to the unchanged CI maps and the executed class XML, and guards retain the byte-proven identity set.

### ModuleBoundaryTests — 9

```text
discoversExactlyTheFrozenModuleSet
everyExpectedModuleDeclaresApplicationModule
assertsExactAllowedDependencyMapFromPlan62
discoveredProductionEdgesStayInsideTheConfiguredMap
verifiesNoCyclesOrBoundaryViolations
rejectsCycleFixture
evidenceMediaReadIsNamedAndInternalImplementationCannotBeImported
rejectsForbiddenDependencyFixture
rejectsInternalPackageAccessFixture
```

### EvidenceRoleIvrAssociationAclBoundaryTests — 8

```text
v018NamesOnlyTheTwoOwnerIssuedAssociationColumns
v018GuardsAuthorityHistoryProvenanceAndExactReplayStates
v001ThroughV017AndPublicContractsRemainByteFrozen
pa25UsesExactEighteenHistoryAndIndependent410TupleOracle
pa25DefinesOnlyTheElevenApprovedNonInheritingEdges
currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory
pa36HasNoApplicationApiPoolOrDependencyExpansion
ciPomAndOrdinaryMethodInventoryMatchExactly
```

### com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPortTests — 8

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

### com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemanticsTests — 10

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

### com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryTests — 12

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

### com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommandTests — 16

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

### com.onecall.core.operations_iam.internal.web.CatalogVersionRequestBinderTests — 10

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

### com.onecall.archtests.OperationsCatalogVersionWebTests — 14

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

### com.onecall.archtests.OperationsCatalogVersionConfigurationTests — 8

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

### com.onecall.archtests.architecture.OperationsCatalogVersionBoundaryTests — 8

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

### com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT — 24

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

### com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT — 14

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

## Raw evidence log hashes

All paths below are relative to `/tmp/pa38-fix3-evidence/`; final static audit and complete file custody are retained separately after this artifact is finalized.

```text
clean-full-oracle.log c4641601106a26a487b3806cc3851ea4c200b09d2ac3dbe4985dd660bf78c3d6
failsafe38.log b0d3a503a1a326ce9af836e71a2736e0d546dd37577ac78e771982b70557b8dd
focused86.log ad17279416cc7edd48a0d138ad7b1db4267a10c3e2653406cfe26bc77dd48de7
guards17.log 96a697657ece1fc621553fcdd76e15ae0e309494c66e1a3629d28f1d28e00f97
observer-repeat-1.log a35704d629d68e5595a9cd926eb21bd437b79f3826b806687f1e837539226464
observer-repeat-2.log 28eb566ba8b967401973706386b469709034fc43d714524ec8405e285b16d19b
observer-repeat-3.log 7222994a4f29c5d0dde7389087066c76030b44abe492ddccfaae4aa93ce5b75e
preflight.log 8bbe7b2963e3d5dbe18a328671ff75a1604d4fd302503c44ef0f3696ce71a006
setup.log c09bf9f77af79df90d3a53d767349fc0b9ffadae5270231ebd26e5d6a6becdb4
smoke.log 86d60b0bb42f6e0072809a183078cdcce3f7a9a87e35d88da76184488fd7face
static-pre-report-corrected.log 2183a8a6db73c02136ca0341c9cf01d0745a2bd55d56e6a169f7fb04736b18fc
static-pre-report.log c5c4a7c3dcd23475c16be49d7bebcf7c84925ad2e20028c429a0c27c0442aa55
write-artifact.log 2f91624cb3f39af95ed151ffe97e99989622d714a2ff9f9ca29d40f3b7ab4592
xml-audit.log 5fcef2ceb3b8304ae958bba08e2d217e35ee3961cbca545d1b9984b5d94c7bb9
```

## Residual risks and owners

- **Controller / re-review owners:** fix3 closes only the manifest lockstep finding; fresh independent re-review and later gate/deployment decisions are not performed or pre-approved here.
- **Backend test-boundary / PA35 guard owner:** `PA38-RR2-01` raw-CRLF hardening remains deferred-with-owner. Canonical accepted LF validation is green; no raw-checkout all-green claim or primary frozen-byte edit is made.
- **Backend test robustness / validation host owner:** three separate live observer passes and full-class/full-sweep passes preserve the exact-PID fix2 witness. Severe scheduler starvation can still fail closed within the unchanged 250ms real timeout; repeats do not guarantee arbitrary-host scheduling.
- **Platform / Security / Operations:** production enablement/secret delivery and seven existing PA25 SET edges, unchanged four-slot/3000ms shared LO pool saturation/capacity/read-503 recovery, existing broad catalog grants, audit/Outbox dispatch/retention and future draft/UI slices remain their prior deployment/future-slice responsibilities. No new pool, credential, ACL/schema or production-readiness acceptance is claimed.

**Stop boundary:** fix3 artifact and completion report only; next gate **`re-review`**. No stage, commit, push, branch creation, PR creation/update, merge, approve, mark-ready, reviewer request, external comment or issue mutation occurs.
