# PA30 plan fix 1 — adjudicated finding closure

## 1. Task, authority and result

- Task: `pa30-plan-fix1-gpt-20261112-01`; date: 2026-11-12 (Asia/Shanghai).
- Supplied runtime/provider: Codex `gpt-6.1-sol`, reasoning `high`; this is dispatch metadata, not independent model introspection.
- Gate: fix after independent plan review. Plan-only work completed; exit status `0` for the fix task, with Windows Git validation unavailable as explicitly recorded below. No implementation or later gate is performed or requested.
- Supplied base: `a4c6abacc8afeec14d0308e854a7daff442a770f`; not locally Git-verified because Windows Git interop fails.
- Authorized writes: modify the plan and add this fix artifact, exactly two documents. No delegation.
- Goal unchanged: only `POST /api/v1/customer/subject-claims`, personal selected workspace, verified-session account authority, local fail-closed proof/reference SPI and controlled fakes. No enterprise ownership, history API, login/session issuance, adjacent business writes, production provisioning, provider/KMS, OpenAPI edit or new migration.

## 2. Binding inputs and changed outputs

| Input | Exact raw SHA-256 |
| --- | --- |
| Reviewed plan, before this fix | `20ec9f9245ac1407fa63913db053bb2bbb91a34e7b293b49d1b9f82ca1877455` |
| Reviewer A: `docs/reviews/plan-review-20261112-225639.md` | `f853d2f3bcbafe48ecb3ee11787e5949f8522be69830752695b29ca7622f2deb` |
| Reviewer B: `docs/reviews/plan-review-20261112-213245.md` | `90661f87f37e54eee98474755d8f62cf2273de94088fab30c7a2bfa1b9e80e7e` |

All three exact input hashes matched dispatch before editing. Both accepted reviews and Control's PA30 goal confirmation/review adjudication were read. Failed A drafts were neither accessed nor treated as evidence.

Changed plan: `docs/gateflow/pa30-customer-personal-subject-claim-foundation-plan.md`, final raw = LF SHA-256 `9cf8d1328b896686b9a297f355f0504ead323ffaabecc0f1ba9b6893ab135f75` (91114 bytes; 677 LF lines). This artifact is UTF-8/LF too; its final raw/LF hash is emitted by final validation and the task completion response, not embedded recursively in itself.

Control legitimately advanced after the original plan freeze to record accepted review artifacts/adjudication. Original plan-time Control raw `d29f8785dbf955c411594fca8df76d358ed05c1578c2536c6e5e8246e521152a`, LF `911808c0dd5e040160495fecc5f1b5c85e5bb0aaa4a845ef62b27d58d4a41d1b` are historical, not the current implementation oracle. Plan §2 now records the current adjudicated Control pair below. This task does not edit Control or reviews; they are pre-existing Controller inputs, preserved byte-for-byte throughout this fix.

## 3. Accepted findings and verification

“Closed” means the plan defect is corrected, not that future code/tests have run. Section references are to the revised plan.

| Finding | Closure / exact changed sections | Required verification | Residual risk |
| --- | --- | --- | --- |
| `PA30-PLAN-REV-01` High | Closed: §3 G8, §4 rows08/09/18, §5.1, §6.2–6.3, §7.1–7.4, §9–§11 remove two-phase preparation, decoded map and PreparationChanged. One authoritative TransactionTemplate execute per attempt; local SPIs only, three whole attempts. | U05/U17, A05, I02/I18–I24; ambient tx rejection, same-PID/cleanup, unique/40001/40P01 retry, no external I/O. Obsolete U06 deleted. | Runtime serialization/cleanup evidence deferred to implementation; no design question. |
| `PA30-PLAN-REV-02` High | Closed: §5.1/§5.3, §6.3, §7.3, §9.1 U08/U11 and §9.2 I09/I12/provider fixture pin risk then immutable row subject/account/null-org/method before any stored open. | Counting fake zero opens for each foreign-account/different-subject/different-method row, even Invalid/Unavailable; OPEN risk blocks before replay/reference errors and zero stored opens. | Implementation must not accidentally open on lookup; tested with per-envelope counters, not payload-valued diagnostics. |
| `PA30-PLAN-REV-03` Medium | Closed: §2, §9.1 A06, §10.3 add docs/15 raw/LF and exact21-path oracle/baseline; V003 counted once. §12 reflects current Control. | Independent raw/LF recomputation, exact21 unique paths, four non-migration plus17 migrations; artifact table §6 below. | Hash capture alone is not verification; compare pinned pairs at implementation entry/completion. |
| `PA30-PLAN-REV-04` Medium | Closed: §7.1, §9.1 C02/C03, §9.2 I01/I02 and fixture contract explicitly use web,persistence, customer-query and dedicated command credentials, controlled beans and preflight. | Assert qualified owner/repository/pool/manager/template/DSL and both unique SPIs before first HTTP; command role/PID and distinct query pool; missing dedicated credentials cannot fall back. | Docker/context startup unrun here. Unrelated eager sharing-query pool receives explicit pa27_query credentials only, never command authority. |
| `PA30-PLAN-REV-05` Medium | Closed: §4 rows34/35 and §9.3 name exact existing test methods and both10→11 lists/checks; customer39→43 and fixed controller/profile list addition. | Existing ModuleBoundaryTests and CustomerApiBoundaryTests identities preserved; both exact tuple lists gain named command edge, no existing edge removed. | Architecture tests will run only at implementation; no method count padding. |
| `PA30-PLAN-REV-06` Medium | Closed: §5.3, §7.2–7.4 and §9.1 U07–U18/§9.2 I23 define closed reason/outcome/risk table and all no-audit/rollback cases. | U18 enumerates all exact snapshots/function reasons/exclusions; I23 transactional durability, audit/owner-miss failure; no old status/result or sensitive facts. | Audit growth from repeated replay remains bounded by existing operational controls, not a new limiter in this goal. |
| `PA30-PLAN-REV-07` Low | Closed: §6.2/§7.2, §9.1 R08/W11/U18, §9.2 I23/provider fixture explicitly constant-redact Payload, ProofRequest, ApprovedProof, SealedReference, all rows/provider results/command state. | Boolean canary absence across representations, HTTP/logs/causes/audit; no actual sensitive value in assertion failure text. | Dynamic leakage checks deferred; no production provider or key material is introduced. |

### Bound open-question resolutions

| Bound resolution | Exact changed sections / verification | Remaining question |
| --- | --- | --- |
| Verified-session account unexpectedly absent => uniform FORBIDDEN, no claim/owner write | §5.3 missing-account row retained; §6.3/§7.3 no-audit exception; §9.1 U01 and §9.2 I04/fixture seed explicitly assert403/no claim/owner/audit. | None. |
| docs/15 hash required | §2 exact raw/LF pair; §9.1 A06; §10.3 four non-migration +17 baseline; §6 below independently records pair. | None. |
| S1 chooses single authoritative transaction; future external proof/KMS I/O under locks forbidden | §6.3/§7.3–7.4, U05/A05/I02. No future-KMS preparation seam; new external integration requires new goal/design confirmation. | None. |
| Exact audit mapping part of fix | §7.3 table and exclusions, §9 U18/I23; safe three-key snapshots. | None. |
| Sensitive type redaction part of fix | §6.2/§7.2, §9 R08/W11/U18/I23; constant representation plus boolean-only canary assertions. | None. |

Reviewer open questions about preparation retry budget disappear with that state machine; the remaining budget is three complete authoritative attempts for only named uniqueness/serialization/deadlock failures. Local proof/input validation can precede observing inactive account status per T-S1-06; a current account once read uses the bound403 mapping. L03 now explicitly probes actual binding/risk FOR SHARE and subject FOR UPDATE. Synthetic login creation stays isolated in this fixture's own container, not the existing catalog-oracle fixtures; no oracle relaxation is authorized. These are verification obligations, not new product-policy questions.

## 4. Transaction, preorder and retry contract

1. HTTP route/session/media/header/personal workspace/strict body checks run before owner invocation. Only verified session supplies account; body supplies only frozen four fields. Missing owner or ambiguous dependencies fails closed. Reject an ambient active transaction.
2. Start one dedicated READ COMMITTED, REQUIRES_NEW, read-write TransactionTemplate execute. SET LOCAL ROLE subject_number_role and exact current_user assertion; use its single connection/PID throughout. Read immutable binding identity; zero gives uniform422 without lock/write. Local proof approval binds account/subject/binding/HMAC/method/attempt; canonical input seal/open round-trip validates server reference. No external call.
3. Exact T-S1-06 lock order: `subject_number.number:<immutable HMAC>` advisory; `subject_number.subject:<requested UUID>` advisory; subject FOR UPDATE; binding FOR SHARE; all risk rows ORDER BY id FOR SHARE; `customer_iam.account:<authenticated UUID>` advisory; plain account id/status SELECT. No organization/membership or foreign account row locks. Reread all predicates under those locks. No stale pre-lock state authorizes a write.
4. Missing account gives403/no claim/owner/audit. Other current predicate failures precede replay and can persist only resolvable fresh REJECTED rows; NOT_REQUIRED audit. For passing predicates evaluate all current risk rows with database time; no rows means no OPEN, not fabricated CLEAR. OPEN wins over replay, duplicate and stored-reference error, producing fresh REJECTED or audit-only denial; never open stored references for it.
5. Read globally unique request row metadata; compare requested subject, authenticated account, null organization and method first. Any mismatch => non-disclosing409 CLAIM_REPLAY_CONFLICT, audit-only, zero opens of that row, regardless of cipher health. Only a compatible candidate is opened and checked against its own row and current binding/HMAC/server proof identity. Valid historical context mismatch is409, not invalid-reference422. No foreign plaintext map/staging/cache.
6. Reread request/accepted rows on this transaction before final decision; accepted-owner integrity precedes stored acceptance/duplicate or new owner/claim writes. Exact replay returns original stored shape; no new claim. Without replay compare same-account accepted metadata before opening that candidate, then logical payload context. Exact duplicate returns first accepted result without fresh-key row; changed context appends one CONFLICT with409 CLAIM_CONTEXT_CONFLICT. Foreign/org owner403 with one resolvable CONFLICT, no transfer. Pre-write accepted-owner corruption500 with safe audit only and no repair.
7. Fresh acceptance inserts one append-only claim; conditionally update owner only if both owner columns null, exact revision+1/time and one affected row. Same-account existing owner has zero UPDATE. Add safe audit on same connection. Return decision, commit, then translate a durable denial to sanitized HTTP exception. Owner-update miss or SQL/audit failure rolls back all tentative claim/owner/audit; never write a second audit transaction.
8. Catch retryable errors only after execute cleanup. SQLSTATE23505 retries only `uq_claim_request`/`uq_claim_accepted_subject_account`; 40001/40P01 share the maximum three total attempts. Restart at step2, including local proof validation, locks/current risk. No savepoint, nested transaction, aborted read, provider preparation or PreparationChanged. Other integrity500/infrastructure503 and unknown commit503 are not silently retried; budget exhaustion503. No tentative owner survives rollback.

All S1 SPIs are local fail-closed/controlled-fake operations; no network I/O at all, especially after any PostgreSQL/advisory/row lock. Real external proof/KMS/consuming side effects are explicitly later-goal work requiring renewed confirmation.

### Counting-cipher oracle

U11 `changedRequestIdentityNeverDisclosesStoredOutcome` and I12 `changedReplayIdentityReturnsConflictWithoutStoredDisclosure` use synthetic stored-envelope counters. For each foreign-account, different-subject and different-method request row, assert counter exactly0 for that row, no appended claim/owner change, closed409 replay conflict and no old result. Include invalid and unavailable foreign-reference variants; their result must never be consulted. Input-reference round-trip opens have their own counter, never reset to hide a stored open. Add OPEN risk for foreign and metadata-compatible candidates with Invalid/Unavailable references: risk409 wins and every stored-open counter remains0. Counter/assertion diagnostics use safe integers/booleans and synthetic labels, never envelope/secret actual values.

### Exact audit oracle

Plan §7.3 is authoritative: snapshot keys exactly reason/outcome/riskEvaluation, reason identical to append_fact argument, before_snapshot NULL. Accepted→CLAIM_ACCEPTED/ACCEPTED/NO_OPEN; replay of any stored status→CLAIM_REPLAYED/REPLAYED/NO_OPEN; duplicate→CLAIM_DUPLICATE/DUPLICATE/NO_OPEN; OPEN→CLAIM_RISK_BLOCKED/DENIED/OPEN; replay/context/owner conflict→their exact internal reason/CONFLICT/NO_OPEN. Current mismatch/inactive-binding/inactive-account reasons→DENIED/NOT_REQUIRED. Stored Invalid→CLAIM_REPLAY_UNRESOLVABLE/DENIED/NO_OPEN; pre-write integrity→CLAIM_DATA_INTEGRITY/DENIED/NO_OPEN, audit-only500. Missing-parent/account, proof/input/provider/SQL/pool/retry-exhaustion failures have no durable audit; post-write integrity/owner-miss/audit failures fully roll back. No proof/reference/number/HMAC/ciphertext/old-outcome facts in snapshots. U18 asserts every exact mapping/exclusion, I23 asserts durability/rollback; replay audit never reveals stored status.

## 5. Slice, manifest and exact test inventory

One implementation slice; manifest **37 =25 new +12 existing**, unchanged in path membership. Two current plan/fix artifacts and pre-existing review/Control inputs are not implementation candidates. Row membership in plan §4 is the exact path oracle.

Existing12 =4 main-source files (customer package-info/filter/problem-writer/web configuration) +1 persistence YAML +1 POM +1 CI +1 README +4 existing test files. New25 =16 main-source +7 test classes +2 annotation-free helpers. Customer internal39+4=43. No source path is added merely to replace deleted preparation machinery.

| Inventory | Current observed | Additions | Final implementation requirement |
| --- | --- | --- | --- |
| Surefire classes/methods |49/417|5/51|54/468|
| Failsafe classes/methods |21/336|2/34|23/370|
| Combined classes/methods |70/753|7/85|77/838|
| Annotation lines |754|85|839|
| POM Failsafe includes |21|2|23|
| Production modules |14|0|14|
| Customer internal source files |39|4|43|
| Migration resources/history |17|0|17|

The one existing commented historical @Test line explains annotation839 versus executable838; preserve its exact whitelist. Exact source=CI=XML names/classes must match54/468 +23/370 after full implementation builds; XML is not run here. POM exact23 includes must equal final container-class names. Current source/CI maps and POM text include list were independently checked at fix time (49/417 +21/336,754 lines,21 includes). Both early Surefire total417→468 and final CI XML/inventory assertions must change later. Historical PA29-start375+302=677 and PA30-start417+336=753 stay historical; staged firewall82/116 remains unchanged, not executed here.

Deleted proposed U06 `allProviderCallsRunOutsideTransactionsAndLocks`; no replacement padding. U05/A05/I02 absorb the actual single-transaction/local-SPI invariant. Rename proposed U18 to `exactAuditMappingsAndFailuresNeverLeakOrPartiallyCommit`, A05 to `singleAuthoritativeTransactionAndLocalSpisPreserveBoundary`, I24 to `boundedWholeTransactionRetriesRestorePoolAfterFailure`; remove preparation race/restart expectations. Additions therefore85, not86: W12 +U17 +R8 +C8 +A6 =51 Surefire; I24 +L10 =34 Failsafe. Existing test method identities/counts remain unchanged.

### Exact Surefire identities (51 /5 classes)

`com.onecall.archtests.CustomerSubjectClaimsWebTests` (12):

| ID | Exact method |
| --- | --- |
| W01 | `exactPostReturnsOnlyFrozenAcceptedFields` |
| W02 | `missingInvalidDuplicateAndWrongAudienceSessionAreUnauthenticated` |
| W03 | `onlyTrustedPersonalWorkspaceCanReachOwnerCommand` |
| W04 | `missingTrustedWorkspaceAndUnavailableDependenciesFailClosed` |
| W05 | `closedBodyRejectsUnknownDuplicateMissingAndCoercedFields` |
| W06 | `idempotencyHeaderHasExactBoundsAndOpaqueIdentity` |
| W07 | `methodUuidAndProofReferenceValidationUseUniform422` |
| W08 | `domainProblemsExposeOnlyThreeFrozen409Codes` |
| W09 | `storedRejectedAndConflictReplayOmitVerifiedAt` |
| W10 | `exactRouteMediaNegotiationAndAuthenticationPrecedenceHold` |
| W11 | `responsesProblemsAndFailuresNeverExposeSensitiveCanaries` |
| W12 | `concurrentSessionsKeepCallerAndCommandContextIsolated` |

`com.onecall.archtests.subjectnumber.CustomerSubjectClaimServiceTests` (17):

| ID | Exact method |
| --- | --- |
| U01 | `personalCallerAndCurrentActiveAccountAreMandatory` |
| U02 | `zeroInitialBindingStopsBeforeLocksAndWrites` |
| U03 | `proofApprovalMustBindEveryTrustedDimension` |
| U04 | `unavailableDefaultProofAndCipherNeverAccept` |
| U05 | `locksUseFrozenNamespacesAndCurrentRereads` |
| U07 | `currentPredicateFailuresPersistOnlyResolvableFreshRejections` |
| U08 | `openRiskPreemptsEveryReplayAndDuplicateOutcome` |
| U09 | `riskTimesAndAcceptedTimesComeFromDatabaseDecisionClock` |
| U10 | `exactRequestReplayUsesLogicalIdentityAndFirstOutcome` |
| U11 | `changedRequestIdentityNeverDisclosesStoredOutcome` |
| U12 | `exactAcceptedDuplicateReturnsFirstResultWithoutInsert` |
| U13 | `changedAcceptedContextAppendsExactlyOneConflict` |
| U14 | `freshAcceptanceSetsOnlyNullOwnerWithExpectedRevision` |
| U15 | `differentOwnerRefusesWithoutTransferOrRepair` |
| U16 | `corruptAcceptedOwnerRelationAndUpdateMissFailClosed` |
| U17 | `uniqueLoserRetriesOnlyAfterWholeTransactionCleanup` |
| U18 | `exactAuditMappingsAndFailuresNeverLeakOrPartiallyCommit` |

`com.onecall.archtests.subjectnumber.SubjectClaimVerificationReferenceTests` (8):

| ID | Exact method |
| --- | --- |
| R01 | `v1PayloadHasExactCanonicalKeysAndNoAccountOrRawProof` |
| R02 | `strictPayloadRejectsMissingExtraDuplicateAndMalformedValues` |
| R03 | `wrongVersionAndInvalidEnvelopeFailClosed` |
| R04 | `undecryptableAndUnavailableReferencesRemainDistinct` |
| R05 | `randomizedCiphertextsCompareByDecodedLogicalIdentity` |
| R06 | `rowSubjectMethodAndBindingHmacMustMatchPayload` |
| R07 | `serverResolvedProofReferenceNotClientTextDefinesIdentity` |
| R08 | `sensitiveValuesAndProviderFailuresHaveRedactedRepresentations` |

`com.onecall.archtests.SubjectClaimPersistenceConfigurationTests` (8):

| ID | Exact method |
| --- | --- |
| C01 | `defaultWebAndDisabledProfilesCreateNoCommandPool` |
| C02 | `explicitEnablementRequiresAllDedicatedCredentials` |
| C03 | `commandBeansUseExactQualifiersAndOneDedicatedDatasource` |
| C04 | `writeTemplateUsesReadCommittedRequiresNewAndRollbackCleanup` |
| C05 | `defaultProvidersAreUnavailableAndControlledReplacementsAreUnique` |
| C06 | `customerComposerAndControllerUsePublishedOwnerProviderOnly` |
| C07 | `roleAssertionFailureNeverFallsBackToAnotherPrincipal` |
| C08 | `datasourceAndProviderDiagnosticsNeverExposeCredentialsOrReferences` |

`com.onecall.archtests.architecture.CustomerSubjectClaimBoundaryTests` (6):

| ID | Exact method |
| --- | --- |
| A01 | `namedCommandExportsOnlyFrozenOwnerTypesAndPersonalCaller` |
| A02 | `controllerBinderAndProblemSurfaceMatchFrozenOperation` |
| A03 | `customerCompositionHasNoSqlOrClientAuthorityEscape` |
| A04 | `ownerSqlUsesOnlyApprovedColumnsLocksAndAuditFunction` |
| A05 | `singleAuthoritativeTransactionAndLocalSpisPreserveBoundary` |
| A06 | `openApiMigrationsAndProductionProviderAbsenceRemainFrozen` |

### Exact Failsafe identities (34 /2 classes)

`com.onecall.archtests.container.CustomerSubjectClaimsIT` (24):

| ID | Exact method |
| --- | --- |
| I01 | `postgres16FrozenSeventeenMigrationChainAndDedicatedLoginAreUsed` |
| I02 | `roleLocksReadsWritesAndAuditShareOneConnection` |
| I03 | `freshUnownedPersonalClaimCommitsOneOwnerAndAcceptedRow` |
| I04 | `inactiveAccountAndOrganizationWorkspaceAreForbidden` |
| I05 | `proofInvalidUnavailableAndUnresolvableReferencesFailClosed` |
| I06 | `missingSubjectBindingAndBindingMismatchUseUniformValidation` |
| I07 | `inactiveAndUnverifiedBindingsPersistOnlyRejectedHistory` |
| I08 | `openRiskRejectsFreshRequestAndPreemptsAcceptedReplay` |
| I09 | `openRiskPreemptsSameAccountDuplicateAndStoredReferenceErrors` |
| I10 | `clearedRiskReevaluatesAndReplaysFirstStoredOutcome` |
| I11 | `exactReplayUsesFirstIdTimeAndRandomizedReferenceEquality` |
| I12 | `changedReplayIdentityReturnsConflictWithoutStoredDisclosure` |
| I13 | `exactDuplicateWithNewKeyReturnsFirstAcceptedWithoutInsert` |
| I14 | `changedDuplicateContextAppendsOneConflictAndNeverUpdatesOwner` |
| I15 | `identicalRejectedAndConflictReplayReturnsFrozenStoredShape` |
| I16 | `otherAccountAndOrganizationOwnersAreNeverTransferred` |
| I17 | `corruptAcceptedOwnerRelationsFailWithoutRepair` |
| I18 | `concurrentExactKeyCallsHaveOneAcceptedWinner` |
| I19 | `crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict` |
| I20 | `concurrentSameAccountDuplicateAndChangedContextSerialize` |
| I21 | `twoAccountsClaimingUnownedSubjectHaveOneOwnerWinner` |
| I22 | `riskBindingAndAccountWritersRespectBothSerialOrders` |
| I23 | `auditFailureAndConditionalOwnerMissRollBackWithoutSecretLeakage` |
| I24 | `boundedWholeTransactionRetriesRestorePoolAfterFailure` |

`com.onecall.archtests.container.SubjectClaimRoleLeastPrivilegeIT` (10):

| ID | Exact method |
| --- | --- |
| L01 | `syntheticLoginHasOnlyExistingSubjectNumberMembership` |
| L02 | `directLoginCannotReadOrWriteBeforeLocalRole` |
| L03 | `assumedRoleCanReadExactClaimPredicatesAndInsertHistory` |
| L04 | `ownerUpdatePermitsOnlyAccountRevisionAndTimeColumns` |
| L05 | `claimUpdateDeleteAndTruncateAreDenied` |
| L06 | `foreignAccountWritesLocksAndAdjacentReadsAreDenied` |
| L07 | `unrelatedDomainWritesDdlAndOwnerEscalationAreDenied` |
| L08 | `advisoryAndAuditFunctionsNeedNoNewGrantOrMigration` |
| L09 | `localRoleAndTransactionLocksClearOnCommitRollbackAndDenial` |
| L10 | `frozenAclsHistoryAndCustomerQueryPrincipalRemainUnchanged` |

### Exact existing edits (zero method delta)

- `ModuleBoundaryTests.EXPECTED_ALLOWED` customer list10→11; preserve all old dependencies and add `subject_number::customer-subject-claim-command`. In both `verifiesNoCyclesOrBoundaryViolations` and `rejectsInternalPackageAccessFixture`, change hasSize(10)→hasSize(11) and add exact tuple (subject_number,customer-subject-claim-command) to each precise edge list. Keep `assertsExactAllowedDependencyMapFromPlan62` and `discoveredProductionEdgesStayInsideTheConfiguredMap` identities, so both map and actual edges remain exact.
- `CustomerApiBoundaryTests.customerDependenciesAndReadOnlySqlStayInsideBoundary`: internal count39→43, narrow command named-interface import allowance only. `profileAndVerifierArtifactsCannotEscapeIntoProduction`: append `web/CustomerSubjectClaimsController.java` to its fixed controller/profile list, retaining every old entry and web/servlet assertions. `customerApiSignaturesAndProjectionAreExact` identity remains unchanged.
- Existing CustomerWebConfigurationTests/OneCallApplicationTests adjust only profile/default/qualified assertions; no new methods, no weakening of existing no-SQL/internal/no-query-write checks. No preparation-specific allowlist/test remains required.

## 6. Final frozen-input hashes and no-migration decision

**21 unique paths = four non-migration frozen source/contract/control files +17 migration files.** V003 is one migration, counted once; no 22nd path and no “four plus17 less overlap” ambiguity. Raw and LF hashes are both mandatory even when equal. The following table is the final static oracle for plan §2, implementation baseline and A06; every pair was recomputed and compared against the before-fix snapshot. docs/15 is included. Control is the current adjudicated input, not edited here.

| Frozen path | Raw SHA-256 | LF-canonical SHA-256 |
| --- | --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` | `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` |
| `docs/gateflow/project-execution-control.md` | `e64fa076f0316f99ecbd235d37c59c0a6764c2e49c774917bcbb22c8fd5c196f` | `9d776f2e66a40c51fd48ebc00e0b594b5d021c2e07cd7c8eea05841502a6d2f9` |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql` | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |
| `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql` | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |
| `backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql` | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` |
| `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` | `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` |
| `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql` | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` |

Existing V003 claim SELECT/INSERT, binding/risk SELECT plus current row-lock-capable owner grants, subject permitted owner UPDATE, V002 account id/status SELECT and V001 append_fact EXECUTE suffice by static evidence. Lock namespaces/order and no foreign account row lock preserve that privilege set. No new grant/function/migration, no V018 and no edit to V001–V017 or OpenAPI is authorized. L03/L08 validate actual runtime FOR SHARE/FOR UPDATE/advisory/audit capabilities later; if any privilege is genuinely missing, stop BLOCKED_NEEDS_GOAL_RECONFIRMATION, never escalate credentials or add a migration.

## 7. Fixture context contract

The new helper composes CustomerBusinessNumbersFixture.start(null) but starts its own OneCallApplication + existing SessionBeans + controlled proof/cipher configuration with profiles **web,persistence**, server.port0. It never calls the base's web-only web(). Explicit high-priority properties:

- Customer query: `onecall.customer.persistence.url/username/password` use container URL, existing pa27_customer and synthetic fixture password; both `onecall.customer.business-numbers.cursor-hmac-key` and `onecall.customer.evidence.cursor-hmac-key` are Base64 synthetic32-byte keys.
- Command: `onecall.subject-claim-command.enabled=true`; `onecall.subject-claim-command.datasource.url/username/password` use container URL, distinct pa30_subject_claim_<suffix> NOINHERIT synthetic login and its password, sole membership subject_number_role.
- Unrelated eager sharing-query pool: `onecall.persistence.url/username/password` use container URL, existing pa27_query and synthetic fixture password. They are startup dependencies, never command fallback. Explicit false for `onecall.sharing-command.enabled`, `onecall.sharing-admission.datasource.enabled`, `onecall.evidence-media-read.datasource.enabled`; public-read is gated by sharing-command.enabled too.
- Exactly one controlled SubjectClaimProofVerifier and one SubjectClaimReferenceCipher bean; missing-bean defaults suppressed, no bean overriding or ambiguous primary selection. Fakes stay in test sources and never packaged.

Before any HTTP endpoint call, preflight/I01 asserts web+persistence profiles, qualified customerSubjectClaimService owner implementation, subjectClaimRepository, subjectClaimCommandDataSource, command manager/template/DSL, both unique controlled SPI beans, explicit command login and distinct command/query pools. Command template role/PID probe establishes real authority; I02 proves same-connection work. C02/C03 remove individual command credentials while other pools are configured to prove fail-closed/no implicit fallback. Isolated test role creation does not mutate existing provisioning/ACL oracle fixtures. Missing current account seed maps403/no claim/owner/audit; historical corrupt accepted rows are never repaired.

## 8. Validation actually performed and limitations

| Plan-only check | Outcome |
| --- | --- |
| Exact original-plan and both accepted-review raw SHA-256 inputs | PASS; matched dispatch before edits. |
| Plan/fix UTF-8, no BOM, LF-only, final newline, no trailing whitespace; final raw/LF hashes | PASS; final independent validator emits both artifact hashes. |
| Manifest row ordinals/uniqueness/existence and new/existing split | PASS;37 unique =25 absent/new +12 present/existing. |
| Exact proposed test-name uniqueness/totals and matching artifact tables | PASS;85 unique identities =51 Surefire +34 Failsafe across7 classes, no U06. |
| Existing source=CI map method/class equality, annotation scan and POM text include names/counts | PASS;49/417 +21/336,754 annotation lines,21 exact includes. Map-building statements only; no XML/report/build/smoke execution. |
| Future source=CI=XML/POM/annotation arithmetic | PASS statically:54/468 +23/370 =77/838;839 annotation lines;23 includes; dynamic equality NOT RUN. |
| Frozen OpenAPI/docs14/docs15/Control and V001–V017 raw/LF comparisons | PASS;21 unique pairs unchanged during fix, final table §6. |
| `git.exe diff --check` and `git.exe status --short` | ATTEMPTED, unavailable: both exit1 with WSL UtilBindVsockAnyPort socket failed1; not passing Git evidence. Linux Git cannot resolve Windows worktree .git path and was not substituted for Windows checks. |
| Independent before/after SHA-256 filesystem status | PASS; initial1864 files =1 plan modified +1863 unchanged;1 fix artifact added, final1865 files. Controller reviews/Control unchanged; no other additions/deletions. This is not Git tracked-status proof. |
| Maven/Docker/XML/JAR/firewall/OpenAPI toolchain/endpoint behavior | NOT RUN at plan fix. No runtime/production acceptance claim. |

A first read-only CI map probe lacked the early expected/expected_query seed sets and stopped with NameError; the corrected isolated map extraction included those literals and passed. An initial generated patch with numbered unified hunk headers was rejected without writing; rerun with accepted apply_patch hunk syntax succeeded. Neither diagnostic failure is reported as a failed test or hidden as runtime validation. Private check scripts/snapshots are under /tmp, not repository deliverables.

Blocking design questions: **none**. Nonblocking residual verification risks: Windows Git diff/status/base confirmation needs a functioning Controller Windows environment; actual fixture boot/privilege/locking/serialization/rollback and exact audit/secret-leak tests need implementation-time Maven/PostgreSQL/Docker runs; future full source=CI=XML/JAR/firewall gates remain unexecuted. Future provider/KMS, production credentials and audit-volume operations belong to later confirmed goals, not this slice.

No source/test/config/migration/OpenAPI/CI/POM/README/existing other document/Control edit; no delegation, commit, push, PR, reviewer request, approval, ready action, external comment/issue mutation or later gate. Stop at fix1 completion; this document does not approve or trigger implementation/review.
