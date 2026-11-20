# PA38 aggregate deepreview fix2 — final evidence

## Documentation corrections and current custody chain

### Current Controller correction chain — RR3 through RR8

Aggregate re-review3 findings `PA38-AGG-RR3-01..04` and aggregate re-review4 findings `PA38-AGG-RR4-01..06` were resolved by Controller/agent documentation-only edits to the implementation report, this artifact, the aggregate re-review fix1 and re-review2 fix1 artifacts, Control, the new `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview3-intake.md`, and new review artifacts. Product, test, migration, OpenAPI, POM, CI and evidence bytes remain unchanged. This artifact carries the final report hash and current 32-path custody after those edits; the current final aggregate re-review gate is the one explicitly named in Control.

### Aggregate re-review2 fix1 documentation correction — superseded by the RR3/Controller section above

Task `pa38-operations-catalog-sale-lifecycle-aggregate-rereview2-fix1-qwen38flash-20261119-01` closes accepted `PA38-AGG-RR2-02` (Low: the RR-01 correction pointer was narrower than the mis-attribution it replaced) and `PA38-AGG-RR2-03` (Low: gate-superseded values carried nested `Current` labels). It changes labels and pointers only: the implementation report now has exactly one top-level H1 with a current aggregate re-review2 fix1 section, the nested `Current validation` / `Current … preflight` / `Current full-sweep XML custody` / `Current implementation path hashes` labels are renamed to gate-anchored `Fix1-gate historical …`, `Fix2-gate …`, `Fix3-gate …` and `Aggregate-fix1-gate …` forms, the duplicate historical fix3 H1 is demoted inside `## Historical fix3 implementation evidence`, explicit currency pointers name this artifact's final 32-path custody as the authority for future state, and every PostgreSQL 16.15 statement is now an unmistakably gate-labeled historical measurement while current executed evidence remains PostgreSQL 16.16 from the floating `postgres:16` image. No historical measurement value is rewritten, and no historical aggregate fix1 byte changes.

This historical gate's write set was the implementation report, this supplemental artifact, `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview-fix-1.md` and new `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview2-fix-1.md`. No product/test/migration/OpenAPI/POM/CI/review/evidence or Control byte changed and no test reran. `PA38-AGG-RR2-01` was Controller custody bookkeeping completed after that return; `PA38-AGG-RR2-04/05` remain deferred-with-owner. Its then-current custody was superseded by later accepted gates. Its next step was targeted aggregate re-review3, not entered at that gate. The current gate is the one explicitly named in Control.

### Aggregate re-review fix1 documentation correction — superseded by the section above

Task `pa38-operations-catalog-sale-lifecycle-aggregate-rereview-fix1-gpt-20261119-01` corrected accepted `PA38-AGG-RR-01/02` only. The original fix2 scope and execution narrative below are historical; that gate's write set was the implementation report, this supplemental artifact and new `docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-rereview-fix-1.md`. No product/test/migration/OpenAPI/POM/CI/evidence or Control byte changed and no tests were rerun. Its report hash and 32-path custody were superseded by later gates; historical aggregate fix1 remains unchanged. Its next step was a fresh independent A/B aggregate re-review and is historical. The current gate is the one explicitly named in Control.

## Gate, work unit and disposition

- Gate: existing bounded PA38 aggregate fix2; final evidence, not a new implementation or review gate.
- Work unit: `pa38-operations-catalog-sale-lifecycle-aggregate-fix2-final-gpt-20261119-01`, November 19, 2026 (Asia/Shanghai). Continues `pa38-operations-catalog-sale-lifecycle-aggregate-fix2-gpt-20261119-01`.
- Primary: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`.
- Branch `codex/pa38-next-work-unit`; base/HEAD `e3e49c4cf20c80078aa833a69efd3a9a2a832bfa`; HEAD tree `bdfe4736b08ff425747b4fb1e9a4c6cc56adb850`; accepted slice checkpoint `f612c99589ca59124e341f984fc17f2209598167`.
- **`PA38-AGG-FIX1-PIN-01`: closed for this bounded fix2.** The already-authored literal-only test-carrier synchronization restores manifest lockstep. Controller has completed canonical validation. Existing aggregate remediations `PA38-AGG-REV-01/02/03` remain implemented, with residual classifications preserved; this author neither reimplements them nor grants independent aggregate acceptance.
- **That fix2 gate's next step was a fresh independent A/B aggregate re-review, subsequently delivered through the current gate. This author did not enter any review gate.** The current gate is explicitly named in Control and in the current Controller correction-chain section at the top of this artifact. No staging, commit, push, PR mutation, merge, approval, mark-ready, reviewer request, external comment or issue mutation occurs. No production-readiness claim.

## Exact changed paths and scope

Only these two paths are edited by this final evidence work unit:

```text
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md
docs/gateflow/pa38-operations-catalog-sale-lifecycle-aggregate-deepreview-fix-2.md
```

The prior product/controller/Web changes and manifest test-carrier correction are inputs, not writes in this finalization. Every Java/test, fixture, migration, evidence file and Controller-owned `docs/gateflow/project-execution-control.md` remains untouched. The old three-path fix2 authorization does not expand this final two-path scope. Pre-existing uncommitted and untracked files are retained. Git access uses process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, `GIT_WORK_TREE` equal to primary and `GIT_OPTIONAL_LOCKS=0` because the Windows `.git` pointer is not directly usable from WSL; no pointer/configuration repair is performed.

## Manifest before / after: test-only lockstep

| Binding | Before fix2 | Accepted after fix2 / final |
| --- | --- | --- |
| Production manifest count | 310 | 310 |
| Actual production digest after aggregate fix1 | `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045` | `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045` |
| Test-carrier pinned digest | `3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0` | `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045` |
| Carrier raw SHA256 | `02931eff38612241830a3e4d99f936c42e2c4d673ae865d7a9bd8325104b3520` | `373afbc1d54d9e3b7b7ea88050b9b39ac625fb9a1424d3c9aecb7660ae5d2f62` |
| Carrier raw bytes | 22,599 | 22,599 |

The historical `3fb47bccfb82a9c4a36e4fe5f1abd778f0cb46e04d797955521e6660d03cf2d0` manifest describes the pre-aggregate-fix1 production state; it is not today's production digest. Aggregate fix1's transport fix changed production bytes but its bounded scope excluded the carrier, leaving the old pin. Fix2 changes exactly one 64-byte digest literal in `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`. A read-only byte comparison against the evidence snapshot proves that replacing this one literal yields the current carrier exactly: all 131 CRLF sequences, other assertions, raw/LF frozen hashes, count and `@Test` identities are preserved. No assertion is weakened, no test added/removed/renamed/disabled and no production behavior changes because of this carrier edit.

Recomputed unchanged algorithm: enumerate every regular file beneath backend `src/main/java` and `src/main/resources`, excluding only the existing `V018__evidence_role_ivr_association_read_privileges.sql` filename; normalize each file's CRLF to LF, hash SHA256; sort backend-relative POSIX `path + single space + lowercase digest` rows ordinally; UTF-8 encode with LF after every row, including the last; hash the binding. Result **310 / `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`**, exactly matching `manifest.txt` and the guard literal. This normalized production inventory is distinct from the raw original-candidate inventory below.

## Preserved semantic remediation

Both exact publication and offsale POST handlers obtain the authenticated caller before accessing the body. After authentication the local reader calls `request.getInputStream().readNBytes(8193)`, followed by strict parsing and only then command-provider resolution. The bound is preventive: the application consumes at most 8193 bytes regardless of declared or unknown length; it does not buffer a 2 MiB entity and reject afterward. Existing canonical UUID, strict UTF-8, duplicate/trailing/unknown-field, JSON object and positive integral revision validation remains.

| Both POST routes: input | Preserved result | Body / downstream witness |
| --- | --- | --- |
| Valid JSON padded to exactly 8192 bytes, declared or unknown length | 200 | Exactly 8192 bytes; normal success path |
| Valid prefix padded to 8193 bytes, declared or unknown length | 422 VALIDATION_FAILED | Exactly 8193 bytes; sentinel rejected |
| Valid prefix padded to 2 MiB, declared or unknown length | 422 VALIDATION_FAILED | Exactly 8193 bytes, not the complete entity |
| Missing body | 422 VALIDATION_FAILED | Empty input reaches structural parsing, not framework-required-body failure |
| Explicit zero-byte body | 422 VALIDATION_FAILED | One stream acquisition, zero bytes |
| Truncated stream throwing IOException after seven bytes | 422 VALIDATION_FAILED | One acquisition, seven bytes; no partial-body parse |
| Missing, invalid or wrong-audience credential with fail-if-read stream | 401 UNAUTHENTICATED | Zero body acquisitions/bytes |
| Authentication verifier exception with fail-if-read stream | 503 SERVICE_UNAVAILABLE | Zero body acquisitions/bytes |
| Authenticated valid 8192-byte body, lazy command provider throws | 503 SERVICE_UNAVAILABLE | 8192 bytes read; one provider attempt per route; no command call |

For every invalid-body case, the lazy-provider construction counter remains zero and `verifyNoInteractions(command)` holds: no provider construction, command invocation, command-side authority/database/advisory locks or facts. Sanitized five-field problems preserve status/code, trace, content type, content length, no-store/no-cache/nosniff and challenge behavior; no raw exception/body/SQL/credential details leak. These subcases are within the existing 14 Web identities, not newly invented test identities.

`PA38-AGG-REV-03` audit truth remains: `audit.audit_fact.request_id` correlates identity but is not a database-enforced uniqueness key. Corrected by `PA38-AGG-RR-01`: PA38 product replay safety comes from the advisory transaction lock + post-lock state short-circuit + revision/current-state CAS. Frozen `platform.outbox_append` uses `ON CONFLICT (event_domain, dedupe_key) DO NOTHING`; identical `payload_sha256` returns the existing event id idempotently with no error or rollback, while same-key/different-payload raises `OUTBOX_DEDUPE_CONFLICT` / `P0001` and rolls back the mutation transaction including its audit append. The Outbox dedupe index cannot produce an uncaught `23505` through this function; Java's `23505` recovery is driven by the catalog active unique index. Under a hypothetical bypass, a silent identical-payload skip could leave an unpaired audit fact with no Java-visible signal: owner Platform/Operations + Backend, destination later pairing/detection hardening. Exact SQL/source lines and the correction to historical aggregate fix1's overclaim are in the new fix artifact; historical fix1 bytes are not rewritten.

## Controller validation and independent evidence check

Evidence root: `/tmp/pa38-aggregate-fix2-evidence/`. Accepted LF projection: `/tmp/pa38-lf-validation.hgmbb4/candidate`. Recorded environment: `/opt/maven-3.9.9/bin/mvn -B -ntp`, `JAVA_HOME=/opt/jdk21`, Java 21.0.13, Docker **Server** 29.1.3 and PostgreSQL 16.16 from the floating `postgres:16` image, not a pinned digest. `failsafe38.log:33-36` and `:79-82` bind that image tag to the live 16.16 observations; `PA38-AGG-RR-02` corrects the current report's LifecycleIT statement accordingly, without relabeling explicitly historical 16.15 runs. Preflight and live PostgreSQL transcripts corroborate these versions; no environment command is rerun here. Setup records projection inventory `cd6fa883a65e4be547d9598b3ac81a7c5503c8c4ffdb651467a1138c12064aa2`; the authorized PA35 LF projection has raw primary hash `d5ba84e34124e0cb44fd5a6c8fdc008a8894c3dee472268997d6a4f02500de55` and canonical LF hash `58ec889fe812cd1e0c439a7d3c1054aa41bfe8bd44bcd27183512fbe3ffd85d3`. Primary PA35 bytes are not changed.

| Existing Controller evidence | Reports / tests | Result |
| --- | --- | --- |
| Smoke class OperationsSubjectReadConfigurationTests in canonical Surefire XML | 1 / 8 | 8/8, zero failures/errors/skips |
| Focused PA38 Surefire | 8 / 86 | Exit 0, zero failures/errors/skips |
| Existing guards | 2 / 17 | Exit 0, zero failures/errors/skips |
| Direct PA38 PostgreSQL Failsafe | 2 / 38 (24 + 14) | Exit 0, zero failures/errors/skips |
| Exact observer method, three sequential processes | 1 / 1 each | Exits 0, 0, 0; zero failures/errors/skips |
| Canonical clean full Surefire | 97 / 934 | Zero failures/errors/skips |
| Canonical clean full Failsafe | 38 / 630 | Zero failures/errors/skips |
| Unchanged shipped CI/XML oracle and custody | 135 / 1564 | PASS; clean-full-oracle EXIT=0 |

Controller reports the smoke and clean sweep passed. The root does not contain a separate smoke command/exit/log or the complete clean Maven transcript: the 8/8 smoke-class result is independently corroborated in final XML, not claimed here as a separately audited smoke invocation. `clean-full-oracle.log` contains the shipped oracle's successful source=CI=POM=XML inventory and 135-file custody, not the Maven build transcript; `clean-full-oracle.sh` retains the canonical `clean -Pcontainer-smoke verify && python3` chain. Controller's completed same-shell validation is the supplied authority; this finalizer does not infer unrecorded invocation details or replay the oracle.

The independent `xml-audit.py` remains the method-identity and totals check. Its existing assertions were evaluated read-only in memory with only the two evidence-output writes suppressed; its logic was not edited. Its computed summary and binding equal `test-summary.json` and `full-xml135.sha256` exactly. It verifies focused/live method sets against `pa38-test-identities.json`, the exact observer name `concurrentPublishersYieldOneActiveWinner`, both full-suite totals, all 135 XML hashes against the oracle log and every PA38 method identity in final XML. The shipped CI oracle is **not** rerun; Maven, Docker, PostgreSQL and tests are **not** rerun. Evidence and fixtures are not modified.

Final XML custody (ordinal backend-relative XML path + space + raw SHA256 + LF):

```text
files 135
aggregate 316abf43769a81c897ab2c96733af20f3e35acf7e22983bf188ad7c3e505f422
PA38 Lifecycle ebe22d6c4eb79b0073b1d0137f0b0b1b743d8083d7086f13ceb6ccc64606b908
PA38 Role b8f5805eb55c709d3124ed21526410bd2941e6dc344e78f7b90cf71f263ceb6c
smoke-class XML 7ff17eacf506123f3e7e12af069f5e2e52d323f652c58923f9c5a437a7ce8f23
```

### Recorded canonical commands

Run by Controller from the accepted projection's `backend`, with the recorded JDK/launcher; reproduced as evidence, not instructions executed by this author:

```bash
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=EmployeeAuthorityReadPortTests\,CatalogSaleLifecycleSemanticsTests\,CatalogSaleLifecycleRepositoryTests\,DefaultOperationsCatalogSaleCommandTests\,CatalogVersionRequestBinderTests\,OperationsCatalogVersionWebTests\,OperationsCatalogVersionConfigurationTests\,OperationsCatalogVersionBoundaryTests test
/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=ModuleBoundaryTests\,EvidenceRoleIvrAssociationAclBoundaryTests test
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT\,OperationsCatalogVersionRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT#concurrentPublishersYieldOneActiveWinner failsafe:integration-test failsafe:verify
/opt/maven-3.9.9/bin/mvn -B -ntp clean -Pcontainer-smoke verify
```

The final command is the canonical chain in `clean-full-oracle.sh`; the following success-gated Python block extracts and executes the unchanged `.github/workflows/ci.yml` oracle, then records XML custody. Separate gate `.exit` files all contain their named `EXIT=0`. Historical fix1 guard/full failures remain in the prior artifact and are not relabeled, waived or used as final custody.

## Frozen inputs and source custody

- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java`: `5e04236630271a447783ea294252bc063cb146f7c434211113533d5ad120b365`; current primary bytes equal tested projection bytes.
- `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java`: `07608529343964ed58da9f608cb68847a64bff0bc31e920d91f2ea2455768093`; current primary bytes equal tested projection bytes.
- `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`: `373afbc1d54d9e3b7b7ea88050b9b39ac625fb9a1424d3c9aecb7660ae5d2f62`; current primary bytes equal tested projection bytes.

Java source/test inventory recomputes to **290/180**. V001–V018 remain exactly 18 unchanged migrations; no V019 exists. Both OpenAPI files, docs12/14/15/17 and PA25 remain unchanged against HEAD. Migration ordinal path/raw-hash/LF aggregate: `53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70`. Frozen raw hashes:

```text
api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68
docs/12-technology-stack-and-system-architecture.md 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db
docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31
docs/15-production-openapi-surface-spec.md db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0
docs/17-production-security-design-spec.md b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205
backend/ops/postgresql/pa25-runtime-identities.sql b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9
```

Source/CI maps retain eight PA38 unit classes / 86 methods and two container classes / 38 methods; the unchanged oracle checks POM and historical totals. Exact identities below are corroborated by the independent XML audit, not substituted for it.

### `com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPortTests` (8)

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

### `com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemanticsTests` (10)

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

### `com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryTests` (12)

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

### `com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommandTests` (16)

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

### `com.onecall.core.operations_iam.internal.web.CatalogVersionRequestBinderTests` (10)

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

### `com.onecall.archtests.OperationsCatalogVersionWebTests` (14)

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

### `com.onecall.archtests.OperationsCatalogVersionConfigurationTests` (8)

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

### `com.onecall.archtests.architecture.OperationsCatalogVersionBoundaryTests` (8)

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

### `com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT` (24)

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

### `com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT` (14)

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

## Final original 32-path candidate custody — current

The original candidate set is exactly the 32 paths from `candidate32.txt`, not a new all-files worktree inventory. After the implementation report reached its final bytes, each original file was hashed **raw**, without newline normalization. Ordinally sort POSIX paths, serialize `path + single space + lowercase raw SHA256 + LF` including final LF, then SHA256 the UTF-8 binding. Raw byte count sums the same 32 files.

- Current final original candidate after the final RR9 wording correction: **32 paths / 572,661 raw bytes / `8b6ac652ecfab06f85d5166d71a8a275f824527057352fb4931c3bd31624e5f2`**.
- Current final implementation report: **109,847 raw bytes / `4988956569b2376009cc517e6c412b56fae5d285f1f61ac9327e41c30dbaba2d`**. Only this report changed among the 32 candidate paths; the other 31 rows remain byte-identical to entry, and `572,661 = 564,632 - 101,818 + 109,847` reconciles exactly. The earlier `32 / 572,726 / 20fa30f3...`, `32 / 572,759 / 3e2e99f7...`, `32 / 572,762 / d5520ddd...`, `32 / 572,633 / 02da23e6...`, `32 / 572,633 / d091b8e9...`, `32 / 572,649 / ac6c90e4...`, `32 / 572,489 / d29c3465...`, `32 / 571,838 / 94a362d5...` and `32 / 571,762 / 235b0703...` bindings remain historical.
- One intra-gate recomputation was superseded before close by later label polish inside the same report: 32 / 571,432 / `8f3468e7e41c89553955ec46dd9cc7a3207c8b582c5c668bb91476ccc783e30f` with report 108,618 / `8957f41355dd993b8354c23048860d269ea46f04031bb518a5c2242310a25d11`. That is a working value of this gate, not an accepted binding; only the task-entry binding and the final binding above are pinned here.
- Superseded gate bindings retained as the record of their own gates, not candidate drift: aggregate re-review fix1 **32 / 564,632 / `fb658be3b405c3af567be4174caa09428e07c8e533cf6f346927e47c88e7c8a2`** with report 101,818 / `c0d98b3e30a7e903a7bb335a76a01d8f4ab3dbea3c29523ab33ead3fb4cf6585`, and aggregate re-review1 **32 / 560,721 / `e7ab60382d33c307e363f3d7f9bee48e919559eeabcbe6763ede0eb74c63dbec`** with report 97,907 / `016ebf1e9bffcb6dbcb19760c01987f288560f68b8c27899869557fce3935fc2`.
- The earlier static summary/candidate32-current snapshot is historical pre-final-report evidence, not this final binding. Neither evidence file is overwritten.
- This supplemental fix2 artifact and the prior aggregate fix1 artifact are outside the original 32 paths. The final report is inside; holding its final aggregate here avoids self-reference. This artifact's own size/hash is returned externally, not embedded in itself. The report is not edited after this binding.

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
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java 5e04236630271a447783ea294252bc063cb146f7c434211113533d5ad120b365
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java d46fb4056ae3edc4489e8b71b0787b8ae6adf0c7710ad9771e1def99ed5cdb2b
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java a99c6c711644fe4e9dcdae32b77e256108ec86a66d11dc47a73f36c5b8985b91
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java fbfa75f83eef307f9458a2202b80bbf2cebd91335f34f4ed7d512ac18c762c6d
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java 4f204661ca4512d7112dca3d1043249114b1ce84c3b6876591b91cb16e0acb72
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java 07608529343964ed58da9f608cb68847a64bff0bc31e920d91f2ea2455768093
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java 373afbc1d54d9e3b7b7ea88050b9b39ac625fb9a1424d3c9aecb7660ae5d2f62
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
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md 4988956569b2376009cc517e6c412b56fae5d285f1f61ac9327e41c30dbaba2d
```

## Residual owners and destinations

| Owner | Retained residual / destination; not closed by fix2 |
| --- | --- |
| Backend + Platform | Commit-then-cleanup 503 and stale retry semantics; later cleanup/recovery hardening. |
| Security | Lock-free authority/admission races and existing verifier availability/policy. |
| Platform / Security / Operations | Deployment enablement, secrets, shared four-slot pool capacity and sustained load; operational rollout gate. |
| Platform / Operations | No independent retirement audit fact; audit/Outbox retention, dispatch/retries and unpaired audit-writer dedupe; future platform hardening. |
| Platform / Operations + Backend | Hypothetical identical-payload Outbox skip after bypass of product protections can leave an unpaired audit fact without a Java-visible signal; later pairing/detection hardening. |
| Platform / Operations | Delivery ordering is not guaranteed by `(available_at,id)` despite atomic commit of both events; later dispatcher/consumer contract (`PA38-AGG-RR-03`). Slow-client time/unread-body behavior remains container/operational hardening. |
| Controller / Backend evidence maintenance | Stale foreign per-gate XML is filtered by `xml-audit.py`; preserve exact 135-report projection custody and isolate/preserve future bundles (`PA38-AGG-RR-05`). |
| Backend / Operations IAM | Unused authority-port `sqlState()` is deferred interface cleanup (`PA38-AGG-RR-07`); transport-fault observability remains classified under the accepted IOException-to-422 contract (`PA38-AGG-RR-06`). |
| Operations IAM / Backend | No reachable 429 implementation; preserve frozen enum and defer limiter implementation. |
| Backend | Recovery structural validation; later recovery slice. |
| Backend test-hardening / test-boundary / CI / Controller | Filtered nonempty allSatisfy witnesses, observer starvation/scheduling and primary PA35 CRLF; authorized LF projection only, no frozen primary edit. |
| Operations IAM / Security | Latent forward/include filter behavior; future filter work. |
| Database / Security / Catalog-order | Existing broad ACLs; no schema/grant/migration hardening in this gate. |
| API governance / Catalog-order | UI second confirmation, draft authoring, trial/customer filtering and future API/business slices. |
| Controller | Fresh independent A/B aggregate re-review, later acceptance and Control bookkeeping. |

Residuals remain classified, with no expansion into production readiness, new migration or later slice. Supplemental plan/fix artifacts, aggregate A/B review inputs and Control remain at their existing bytes; none is a 33rd original implementation path.

## Evidence transcript bindings and stop

The following existing files were read and remain unchanged; raw SHA256 anchors their contents. These hashes do not turn historical static snapshots into the final candidate binding.

```text
baseline.json 7d9f42a6cfd38aea0512632446c630c9164ebd12ce9021f246d6b227374c49d5
setup.sh c8b785c587732086df0c063c489e3e3cc4ebeb97a1a228ec577da64cd189659d
setup.log a6081fd4593bb49a8fffdcb5611f7c1f5c7c0af0b9c9e568ca305ed9ab3706f1
preflight.sh 3dd1784b3a9a5b1b7560a449f6428c20836f92e987bef5fa9c18b3bc7c236088
preflight.log 8c012b773f51ba36fa4a7d98545a71292152f6999c30fa0281b4af7ed999c952
run-gates.sh 98a3e390b5d34e3f38a99bdf6aefc0f4e427e8411052171f6a19d6db9013202e
clean-full-oracle.sh 1c51157a7dfd5825e955b449293fc1d55396c2611e3f31b6128c2217146dd71e
clean-full-oracle.log e5666ba208aeecee637ccd94168a7755145b898113ff1fd8cb6cfa9837e31dbe
clean-full-oracle.exit ef33d9dd1941ec45471c2c392536b8f0f2506281011b1ea5396077e41480be53
xml-audit.py 945ff153080f9e06b4e714d75101f5dc715ab69ad1fdfdeb2ab8b4f3cb42c4e4
test-summary.json d99ffac1c67a38e03aeee0b662d7e6dc6a15e042b2fd18db5376959cb573061e
full-xml135.sha256 316abf43769a81c897ab2c96733af20f3e35acf7e22983bf188ad7c3e505f422
pa38-test-identities.json 852784fe1715f6ec3956cbd5a2ee873d341facdc101ede47dcd7c492794acff3
manifest.txt 1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045
migration-inventory.txt 53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70
candidate32.txt e68d4006ac0cfc070882f12ca0086042b9664e25ee6b8818cba7db3c6e400d39
candidate32-current.txt 3886d106943e49944cd7fadcbadbafe6869768a23b20993eaece96a4f68043ed
static-summary.json 9a276814ab887bffcef582c57f47162df8d26fb9d428c1c9c22d1996320f3b6f
focused86.command 1af6d79e8e4f7e3aa5b46176dc89ce7f687e7957448a510690126c1ab246bfd7
focused86.exit f14c02bd33e64588a0e8585f26b9e637aac58e5e5c57dbdb4e507bbb16d540de
focused86.log 0f3260a24fda9a7ad4e1dcc7ba5cee4315462d796ecbd1f18d64514856144c34
guards17.command 14461568f244757d96e80b50faeff316845a022873753184c8e1210c914bb149
guards17.exit 6bb476041965972b1a10c8a3720799b266905be8af421aefdfc39ff1af03961b
guards17.log 01d0f6cf563bca530e06e0b6649d3de130a14cde20819981466b2e31c47cf696
failsafe38.command 5e10f401c9a228fbd06df1d254f2327ea26632daaada45cf421c734d983c81e2
failsafe38.exit b00cfeda13bec9314fdade598f54f0f07a47f364fa5874c283d66315d6a1effc
failsafe38.log 2e425244d8a0784e92b97d7541e13b10000201cecf4d39c775efa56741ebc8df
observer-repeat-1.command 9a4e5909ace95676f40101ad4a4e20c03e22543f289207dea921945f991af77f
observer-repeat-1.exit 21f3d2580a55f188b02c5c3315d1526f0465d9211011d03d9e770c14a6994077
observer-repeat-1.log 0d4e806410372aa68fd6e2f6b45b86b3b113c3fd710d53b6f2f7a878e8542f47
observer-repeat-2.command 9a4e5909ace95676f40101ad4a4e20c03e22543f289207dea921945f991af77f
observer-repeat-2.exit c674324933bc73cfe84e3e1c048261eddb0cda30392041d53ce9990a1f0d1bb4
observer-repeat-2.log 15df08b08723f2d7553dd147ae389c71d91e18ad5b1377b44917640f7427aaca
observer-repeat-3.command 9a4e5909ace95676f40101ad4a4e20c03e22543f289207dea921945f991af77f
observer-repeat-3.exit b4977664c10ee0957c22cb686972367670525fe0dabb85305481b1263060f06d
observer-repeat-3.log 3945ac4ff968b0d42d4d8edf4a382a00363f5b299d16438d8eceb1d4176d9909
```

Read-only final checks: unchanged manifest algorithm/pin 310; Java 290/180; frozen diff/no V019; original 32-path final raw custody; independent XML identities/totals/custody; `git diff --check`. Zero tracked deletions and no staged paths. No Maven, Docker, PostgreSQL, full tests or shipped oracle execution, evidence rewrite or fixture mutation in this finalization.

**Completion at this historical fix2 gate: bounded aggregate fix2 final evidence complete; PA38-AGG-FIX1-PIN-01 closed.** The current candidate binding and current stop point are the post-RR8 values in the current Controller correction-chain section near the top of this artifact and the current custody listing below; the current gate is the one explicitly named in Control, not any historical re-review stop point.
