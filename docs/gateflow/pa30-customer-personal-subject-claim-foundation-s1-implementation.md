# PA30 S1 implementation — finalized implementation candidate and Controller validation evidence

## 1. Authority and supplied status

- Current task: `pa30-subject-claim-s1-evidence-finalize-gpt-20261113-01`; supplied runtime/provider: Codex `gpt-6.1-sol`, reasoning `high`; date: 2026-11-13, Asia/Shanghai. Report-only implementation-evidence finalization after Controller validation; no code review or later gate. Section 13 is the authoritative final validation/status record and supersedes earlier pending-validation/current-blocker descriptions only. Sections 2–12 remain unchanged historical implementation and bounded-fix evidence. PostgreSQL16 execution was Controller-owned, not performed by this finalization child.
- Previous bounded task: `pa30-subject-claim-s1-implementation-fix3-gpt-20261113-01`; section 12 preserves its test-orchestration repair and then-pending Controller rerun.
- Task: `pa30-subject-claim-s1-implementation-retry1-gpt-20261113-01`; date: 2026-11-13, Asia/Shanghai; gate: implementation, slice S1 only.
- Supplied runtime/provider: Codex `gpt-6.1-sol`, reasoning `high`. These are dispatch metadata, not independently introspected model identity.
- Status: **CANDIDATE_IMPLEMENTED; IMPLEMENTATION_EVIDENCE_COMPLETE; INDEPENDENT_CODE_REVIEW_NOT_STARTED**. All implementation-gate validation is green on the Controller evidence in section 13: final focused **468 Surefire +34 Failsafe /56 classes**, clean full-container **838 tests /77 classes**, exact source=CI=XML inventory, packaged JAR/firewall, OpenAPI, frozen-file and Git custody checks. This closes the former `BLOCKED_RUNTIME_VALIDATION` status; earlier child socket failures and intermediate Controller failures remain historical, not current blockers. Independent code review has not yet begun. No code-review outcome, CI-hosted execution, live acceptance, PR readiness, approval or production readiness is claimed.
- Supplied base merge: `b48cbb88cec5302b00e3af3be5f621f69838c20f`; goal confirmation: `a4c6abacc8afeec14d0308e854a7daff442a770f`; accepted-plan checkpoint: `d37e8d8e065deda9c54fe1745fc00cd9f987759a`; controller environment checkpoint: `a7cb317c2382a63d5925a4f19b599b791aa8c35f`. Windows Git cannot start in this child, so these remain supplied identities, not locally verified ancestry/HEAD claims.
- Accepted plan raw SHA-256: `71188f38486578979cfcc7a9949900c3b06f90a14ae318002f3a0281409a0191`; fix-1: `0d7c3f182d1df383b7a75751fc029cf2297021a2e6632029c8af5fb972500fd0`; fix-2: `9c5de2b2fc9801ea6119039177c875a8bb2e2124b8e5607d5b0f52c44af00524`. All match before/after implementation. Control was read as mutable authority and not edited or used as a frozen hash oracle.
- Read the original external implementation prompt and accepted plan/fixes, current PA30 Control, frozen claim operation/schemas/T-S1-06/doc mapping, V001/V002/V003 ACLs, and relevant customer/sharing/source/test/CI/POM/fixture patterns. The Maven launcher and controller-deferred runtime overrides supersede the prior preflight blockers only. `backend/mvnw` is unchanged.
- This document replaces the preserved blocked preflight report at the same authorized path. No second repository report path is created; the prior external blocked-report copy is untouched.

## 2. Exact write boundary

Exactly **41 candidates =25 new +16 existing**, plus this already-existing report. The private SHA-256 filesystem baseline contains 1,871 files, excluding Git metadata, generated target trees and node_modules. Candidate scope checks find all 41 and no out-of-allowlist source/config/test/document changes; final report-inclusive scope is 42 changed paths (25 additions, 17 modifications including this report), no deletions. This is filesystem evidence, not successful Git index/diff evidence.

| # | Status | Exact candidate path |
| --- | --- | --- |
| 01 | new | `backend/src/main/java/com/onecall/core/subject_number/customerclaim/package-info.java` |
| 02 | new | `backend/src/main/java/com/onecall/core/subject_number/customerclaim/CustomerSubjectClaimService.java` |
| 03 | new | `backend/src/main/java/com/onecall/core/subject_number/customerclaim/SubjectClaimException.java` |
| 04 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimProofVerifier.java` |
| 05 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimReferenceCipher.java` |
| 06 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimVerificationReference.java` |
| 07 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimProviderResult.java` |
| 08 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/DefaultCustomerSubjectClaimService.java` |
| 09 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimRepository.java` |
| 10 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/JooqSubjectClaimRepository.java` |
| 11 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceConfiguration.java` |
| 12 | new | `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceProperties.java` |
| 13 | new | `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectClaimCommandService.java` |
| 14 | new | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimsController.java` |
| 15 | new | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimJsonBinder.java` |
| 16 | new | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimDtos.java` |
| 17 | new | `backend/src/test/java/com/onecall/archtests/CustomerSubjectClaimsWebTests.java` |
| 18 | new | `backend/src/test/java/com/onecall/archtests/subjectnumber/CustomerSubjectClaimServiceTests.java` |
| 19 | new | `backend/src/test/java/com/onecall/archtests/subjectnumber/SubjectClaimVerificationReferenceTests.java` |
| 20 | new | `backend/src/test/java/com/onecall/archtests/SubjectClaimPersistenceConfigurationTests.java` |
| 21 | new | `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java` |
| 22 | new | `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java` |
| 23 | new | `backend/src/test/java/com/onecall/archtests/container/SubjectClaimRoleLeastPrivilegeIT.java` |
| 24 | new | `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java` |
| 25 | new | `backend/src/test/java/com/onecall/archtests/subjectnumber/ControlledSubjectClaimProviders.java` |
| 26 | existing | `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` |
| 27 | existing | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` |
| 28 | existing | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java` |
| 29 | existing | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java` |
| 30 | existing | `backend/src/main/resources/application-persistence.yml` |
| 31 | existing | `backend/pom.xml` |
| 32 | existing | `.github/workflows/ci.yml` |
| 33 | existing | `backend/README.md` |
| 34 | existing | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` |
| 35 | existing | `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java` |
| 36 | existing | `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java` |
| 37 | existing | `backend/src/test/java/com/onecall/archtests/OneCallApplicationTests.java` |
| 38 | existing | `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java` |
| 39 | existing | `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java` |
| 40 | existing | `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java` |
| 41 | existing | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` |

Existing methods retain their identities/counts. The eight existing test files receive only the bound dependency/controller/default-profile assertion changes; old module edges, read-only checks and inventories remain. No migration, new grant, role graph, production credential, OpenAPI, docs/14, docs/15, plan/fix, Control, wrapper or other repository path is modified. Generated Maven artifacts stay under `backend/target`; private scripts, copied contract validation tree and evidence snapshots stay under `/tmp`.

## 3. Delivered operation and owner boundary

Only `POST /api/v1/customer/subject-claims` is added. Exact route/URI firewall admission precedes session verification; authentication precedes key/media/body processing. Exactly one 1–128-code-point opaque key is retained without trimming/case/comma normalization. Strict UTF-8 JSON accepts exactly the four required non-null strings, canonical UUID shape with server UUID normalization, four exact method enums and bounded proof reference. Duplicate/unknown/missing/coerced/trailing input is rejected. The trusted principal supplies account and selected workspace; organization or inconsistent personal workspace is forbidden, malformed trusted context is unavailable, and current missing/inactive account is uniformly forbidden. No query/body/header selector supplies authority, and the composer does not call getCustomerMe.

The customer composer/controller/binder/DTO/problem surface is SQL-free and imports only the published owner command interface/types. Exactly two top-level owner API types are exported through `customer-subject-claim-command`; PersonalClaimCaller contains only accountId. Output is only claimId/status/subjectId, with verifiedAt omitted except ACCEPTED. Problems have only the existing five safe fields, generated trace UUID and closed titles; the only domain409 codes are CLAIM_RISK_BLOCKED, CLAIM_REPLAY_CONFLICT, CLAIM_CONTEXT_CONFLICT. No-store/no-cache/nosniff and Bearer challenges remain. The endpoint remains authenticated fail-closed503 without a usable owner/proof/cipher collaborator.

Current predicates precede stored results and OPEN risk preempts replay/duplicate/reference errors. Immutable stored row subject/account/null-organization/method identity is checked before any stored open; foreign account/subject/method rows have zero stored opens, including Invalid/Unavailable variants. Logical payload binding/HMAC/method/server proofRef, never randomized ciphertext bytes/digests or client attempt text, defines identity. Exact replay returns the first id/status/time, including permitted stored REJECTED/CONFLICT as200 without verifiedAt. Exact accepted duplicate does not store its fresh key. Changed context appends one CONFLICT; another owner gets403 with resolvable conflict history, never transfer/repair. Accepted-owner integrity scans every accepted relation before success/new writes. Missing parents, invalid/unavailable input proof/reference and infrastructure failures do not fabricate history/audit.

Fresh acceptance inserts one append-only claim and conditionally sets owner only when both owner columns are null, with expected revision, revision+1 and database time, atomically. Same owner causes no UPDATE/revision/time bump. Conditional update miss rereads under the held lock and rolls back, never repairs. Only subject_claim INSERT, permitted owner-account/revision/time UPDATE and existing audit.append_fact are emitted as business writes.

## 4. Transaction, SPI, pool and audit

The owner has exactly the five constructor dependencies specified in the plan. One transaction.execute per whole attempt, maximum three attempts, rejects ambient transactions and uses READ_COMMITTED / REQUIRES_NEW / read-write / rollback-on-commit-failure. Each attempt starts SET LOCAL ROLE subject_number_role and exact current_user assertion; SQL, locks, local SPI work and audit use the transaction-aware DSL over the dedicated pool. No role/credential/connection fallback exists. Driver constraint metadata is inspected through a narrow reflective accessor because the existing PostgreSQL dependency remains runtime-scoped; no dependency/scope change or diagnostic message parsing/retention is added.

Lock sequence is the frozen number advisory key, requested subject advisory key, subject FOR UPDATE, binding FOR SHARE, ascending risk rows FOR SHARE, account lifecycle advisory key and plain id/status account SELECT. Locked current facts and immutable binding identity are checked; zero risk rows are genuinely no OPEN under the number key. No foreign account row, organization/member, outbox, platform-idempotency or earlier-order historical binding locks are added. Only the two named uniqueness constraints and40001/40P01 retry after whole transaction cleanup; exhausted/unknown-commit/infrastructure failures are sanitized503. There are no savepoints, aborted-connection winner reads, generic retries or preparatory/staged plaintext maps.

Explicit opt-in profile/property/env names are `persistence`, `onecall.subject-claim-command.enabled` / `ONECALL_SUBJECT_CLAIM_COMMAND_ENABLED` and dedicated `onecall.subject-claim-command.datasource.url/username/password` / `ONECALL_SUBJECT_CLAIM_COMMAND_DB_URL`, `_DB_USERNAME`, `_DB_PASSWORD`. Defaults are disabled/blank; no query or default credentials are reused. Qualified pool/manager/template/DSL/repository/owner/SPI beans use exactly the bound names. Missing credentials and provider/pool diagnostics are constant/redacted.

Two narrow local SPIs fail closed by default. No network, real verifier, KMS, key-management policy, proof consumption/expiry or production encryption is supplied. Canonical v1 payload has exactly bindingId/numberHmac/proofRef/schemaVersion/subjectId/verificationMethod, sorted keys, strict UTF-8/UUID/HMAC/reference/version/shape and byte-canonical round-trip checks, including lowercase canonical control escapes. Account authority remains in proof approval and immutable claim row, not a new payload field. Envelope framing uses the last dot and frozen key-ref alphabet. Byte-array results are defensively copied and every named sensitive command/input/provider/row/decision representation is redacted. Test-only immutable approvals and randomized authenticated ephemeral encryption are not component-scanned/production adapters and do not decide production Q2.

Audit is on the same connection through existing audit.append_fact, never direct audit DML. Snapshot has exactly reason/outcome/riskEvaluation, all closed enums. Unit scenarios exercise all twelve reason mappings: ACCEPTED, REPLAYED (same safe outcome for all stored statuses), DUPLICATE, RISK_BLOCKED, REPLAY_CONFLICT, CONTEXT_CONFLICT, OWNER_CONFLICT, SUBJECT_BINDING_MISMATCH, BINDING_NOT_ACTIVE, ACCOUNT_INACTIVE, REPLAY_UNRESOLVABLE, DATA_INTEGRITY. Durable denials return transaction decisions and throw only after commit; audit failure and tentative owner/claim integrity failures roll everything back, with no second audit transaction. Excluded/unresolvable-parent/provider/infrastructure paths produce no durable audit. Captured HTTP/log/exception/value canary assertions use safe booleans, not secret-bearing assertion diagnostics. Actual PostgreSQL audit/PII atomicity remains controller-deferred, not proved by mocks.

## 5. Exact test identities and arithmetic

Baseline49/417 +21/336 =70/753. Added5/51 +2/34 =7/85. Final source/CI target is **54/468 +23/370 =77/838**, annotation839 including the unchanged single historical commented annotation, POM23 explicit includes, customer internal43, migrations17, modules14. U06 is absent. All methods are ordinary no-argument named tests; log capture is injected through lifecycle methods so XML retains the exact bare method identities. No parameterized/dynamic/conditional substitute, skip, blanket exclusion, renamed existing test or assertion weakening is used.

### com.onecall.archtests.CustomerSubjectClaimsWebTests (12)

| ID | Exact method | Child evidence |
| --- | --- | --- |
| W01 | `exactPostReturnsOnlyFrozenAcceptedFields` | PASS (focused Surefire) |
| W02 | `missingInvalidDuplicateAndWrongAudienceSessionAreUnauthenticated` | PASS (focused Surefire) |
| W03 | `onlyTrustedPersonalWorkspaceCanReachOwnerCommand` | PASS (focused Surefire) |
| W04 | `missingTrustedWorkspaceAndUnavailableDependenciesFailClosed` | PASS (focused Surefire) |
| W05 | `closedBodyRejectsUnknownDuplicateMissingAndCoercedFields` | PASS (focused Surefire) |
| W06 | `idempotencyHeaderHasExactBoundsAndOpaqueIdentity` | PASS (focused Surefire) |
| W07 | `methodUuidAndProofReferenceValidationUseUniform422` | PASS (focused Surefire) |
| W08 | `domainProblemsExposeOnlyThreeFrozen409Codes` | PASS (focused Surefire) |
| W09 | `storedRejectedAndConflictReplayOmitVerifiedAt` | PASS (focused Surefire) |
| W10 | `exactRouteMediaNegotiationAndAuthenticationPrecedenceHold` | PASS (focused Surefire) |
| W11 | `responsesProblemsAndFailuresNeverExposeSensitiveCanaries` | PASS (focused Surefire) |
| W12 | `concurrentSessionsKeepCallerAndCommandContextIsolated` | PASS (focused Surefire) |
### com.onecall.archtests.subjectnumber.CustomerSubjectClaimServiceTests (17)

| ID | Exact method | Child evidence |
| --- | --- | --- |
| U01 | `personalCallerAndCurrentActiveAccountAreMandatory` | PASS (focused Surefire) |
| U02 | `zeroInitialBindingStopsBeforeLocksAndWrites` | PASS (focused Surefire) |
| U03 | `proofApprovalMustBindEveryTrustedDimension` | PASS (focused Surefire) |
| U04 | `unavailableDefaultProofAndCipherNeverAccept` | PASS (focused Surefire) |
| U05 | `locksUseFrozenNamespacesAndCurrentRereads` | PASS (focused Surefire) |
| U07 | `currentPredicateFailuresPersistOnlyResolvableFreshRejections` | PASS (focused Surefire) |
| U08 | `openRiskPreemptsEveryReplayAndDuplicateOutcome` | PASS (focused Surefire) |
| U09 | `riskTimesAndAcceptedTimesComeFromDatabaseDecisionClock` | PASS (focused Surefire) |
| U10 | `exactRequestReplayUsesLogicalIdentityAndFirstOutcome` | PASS (focused Surefire) |
| U11 | `changedRequestIdentityNeverDisclosesStoredOutcome` | PASS (focused Surefire) |
| U12 | `exactAcceptedDuplicateReturnsFirstResultWithoutInsert` | PASS (focused Surefire) |
| U13 | `changedAcceptedContextAppendsExactlyOneConflict` | PASS (focused Surefire) |
| U14 | `freshAcceptanceSetsOnlyNullOwnerWithExpectedRevision` | PASS (focused Surefire) |
| U15 | `differentOwnerRefusesWithoutTransferOrRepair` | PASS (focused Surefire) |
| U16 | `corruptAcceptedOwnerRelationAndUpdateMissFailClosed` | PASS (focused Surefire) |
| U17 | `uniqueLoserRetriesOnlyAfterWholeTransactionCleanup` | PASS (focused Surefire) |
| U18 | `exactAuditMappingsAndFailuresNeverLeakOrPartiallyCommit` | PASS (focused Surefire) |
### com.onecall.archtests.subjectnumber.SubjectClaimVerificationReferenceTests (8)

| ID | Exact method | Child evidence |
| --- | --- | --- |
| R01 | `v1PayloadHasExactCanonicalKeysAndNoAccountOrRawProof` | PASS (focused Surefire) |
| R02 | `strictPayloadRejectsMissingExtraDuplicateAndMalformedValues` | PASS (focused Surefire) |
| R03 | `wrongVersionAndInvalidEnvelopeFailClosed` | PASS (focused Surefire) |
| R04 | `undecryptableAndUnavailableReferencesRemainDistinct` | PASS (focused Surefire) |
| R05 | `randomizedCiphertextsCompareByDecodedLogicalIdentity` | PASS (focused Surefire) |
| R06 | `rowSubjectMethodAndBindingHmacMustMatchPayload` | PASS (focused Surefire) |
| R07 | `serverResolvedProofReferenceNotClientTextDefinesIdentity` | PASS (focused Surefire) |
| R08 | `sensitiveValuesAndProviderFailuresHaveRedactedRepresentations` | PASS (focused Surefire) |
### com.onecall.archtests.SubjectClaimPersistenceConfigurationTests (8)

| ID | Exact method | Child evidence |
| --- | --- | --- |
| C01 | `defaultWebAndDisabledProfilesCreateNoCommandPool` | PASS (focused Surefire) |
| C02 | `explicitEnablementRequiresAllDedicatedCredentials` | PASS (focused Surefire) |
| C03 | `commandBeansUseExactQualifiersAndOneDedicatedDatasource` | PASS (focused Surefire) |
| C04 | `writeTemplateUsesReadCommittedRequiresNewAndRollbackCleanup` | PASS (focused Surefire) |
| C05 | `defaultProvidersAreUnavailableAndControlledReplacementsAreUnique` | PASS (focused Surefire) |
| C06 | `customerComposerAndControllerUsePublishedOwnerProviderOnly` | PASS (focused Surefire) |
| C07 | `roleAssertionFailureNeverFallsBackToAnotherPrincipal` | PASS (focused Surefire) |
| C08 | `datasourceAndProviderDiagnosticsNeverExposeCredentialsOrReferences` | PASS (focused Surefire) |
### com.onecall.archtests.architecture.CustomerSubjectClaimBoundaryTests (6)

| ID | Exact method | Child evidence |
| --- | --- | --- |
| A01 | `namedCommandExportsOnlyFrozenOwnerTypesAndPersonalCaller` | PASS (focused Surefire) |
| A02 | `controllerBinderAndProblemSurfaceMatchFrozenOperation` | PASS (focused Surefire) |
| A03 | `customerCompositionHasNoSqlOrClientAuthorityEscape` | PASS (focused Surefire) |
| A04 | `ownerSqlUsesOnlyApprovedColumnsLocksAndAuditFunction` | PASS (focused Surefire) |
| A05 | `singleAuthoritativeTransactionAndLocalSpisPreserveBoundary` | PASS (focused Surefire) |
| A06 | `openApiMigrationsAndProductionProviderAbsenceRemainFrozen` | PASS (focused Surefire) |
### com.onecall.archtests.container.CustomerSubjectClaimsIT (24)

| ID | Exact method | Child evidence |
| --- | --- | --- |
| I01 | `postgres16FrozenSeventeenMigrationChainAndDedicatedLoginAreUsed` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I02 | `roleLocksReadsWritesAndAuditShareOneConnection` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I03 | `freshUnownedPersonalClaimCommitsOneOwnerAndAcceptedRow` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I04 | `inactiveAccountAndOrganizationWorkspaceAreForbidden` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I05 | `proofInvalidUnavailableAndUnresolvableReferencesFailClosed` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I06 | `missingSubjectBindingAndBindingMismatchUseUniformValidation` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I07 | `inactiveAndUnverifiedBindingsPersistOnlyRejectedHistory` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I08 | `openRiskRejectsFreshRequestAndPreemptsAcceptedReplay` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I09 | `openRiskPreemptsSameAccountDuplicateAndStoredReferenceErrors` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I10 | `clearedRiskReevaluatesAndReplaysFirstStoredOutcome` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I11 | `exactReplayUsesFirstIdTimeAndRandomizedReferenceEquality` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I12 | `changedReplayIdentityReturnsConflictWithoutStoredDisclosure` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I13 | `exactDuplicateWithNewKeyReturnsFirstAcceptedWithoutInsert` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I14 | `changedDuplicateContextAppendsOneConflictAndNeverUpdatesOwner` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I15 | `identicalRejectedAndConflictReplayReturnsFrozenStoredShape` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I16 | `otherAccountAndOrganizationOwnersAreNeverTransferred` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I17 | `corruptAcceptedOwnerRelationsFailWithoutRepair` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I18 | `concurrentExactKeyCallsHaveOneAcceptedWinner` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I19 | `crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I20 | `concurrentSameAccountDuplicateAndChangedContextSerialize` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I21 | `twoAccountsClaimingUnownedSubjectHaveOneOwnerWinner` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I22 | `riskBindingAndAccountWritersRespectBothSerialOrders` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I23 | `auditFailureAndConditionalOwnerMissRollBackWithoutSecretLeakage` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| I24 | `boundedWholeTransactionRetriesRestorePoolAfterFailure` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
### com.onecall.archtests.container.SubjectClaimRoleLeastPrivilegeIT (10)

| ID | Exact method | Child evidence |
| --- | --- | --- |
| L01 | `syntheticLoginHasOnlyExistingSubjectNumberMembership` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L02 | `directLoginCannotReadOrWriteBeforeLocalRole` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L03 | `assumedRoleCanReadExactClaimPredicatesAndInsertHistory` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L04 | `ownerUpdatePermitsOnlyAccountRevisionAndTimeColumns` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L05 | `claimUpdateDeleteAndTruncateAreDenied` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L06 | `foreignAccountWritesLocksAndAdjacentReadsAreDenied` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L07 | `unrelatedDomainWritesDdlAndOwnerEscalationAreDenied` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L08 | `advisoryAndAuditFunctionsNeedNoNewGrantOrMigration` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L09 | `localRoleAndTransactionLocksClearOnCommitRollbackAndDenial` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |
| L10 | `frozenAclsHistoryAndCustomerQueryPrincipalRemainUnchanged` | NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION |

The new fixture composes CustomerBusinessNumbersFixture.start(null), never base.web(), and starts its own OneCallApplication + SessionBeans + ControlledBeans with web,persistence and explicit customer/query/command credentials, unrelated persistence prerequisites and opt-outs. It creates only a synthetic isolated NOINHERIT login with sole subject_number_role membership, distinct command pool, preflight of profiles/qualified beans/two SPIs and role/PID before any endpoint call. Controlled providers probe the active same-connection transaction; fixtures seed all required authority/history/risk cases. Race methods use bounded futures/latches/barriers and real independent JDBC transactions; no sleeps. Cleanup/fault tests author same-session role/advisory/isolation cleanup, real global uniqueness contention, both named unique-state fault paths,40001/40P01 budgets, audit failure and owner-miss rollback. **None of these PostgreSQL fixture/tests was executed in this child.**

## 6. Commands, actual results and evidence

All Maven commands run from backend using:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin
/opt/maven-3.9.9/bin/mvn -B -ntp ...
```

Installed Java is Temurin21.0.13+9; Maven3.9.9 satisfies the enforcer; Node is22.23.4. For Surefire only, command-line `-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar` loads the already-resolved test dependency at startup. The sandbox denies Mockito's default external self-attach; premain fixes that execution failure without a POM/dependency change, test replacement or weakened assertion.

| Executed command / check | Exit and actual result | Evidence |
| --- | --- | --- |
| `mvn -B -ntp test-compile` | **0**; all main/test sources, including both complete IT classes and fixture, compile. Clean full runs compile200 main +110 test-source files. | `/tmp/pa30-retry1-test-compile-candidate-final.log`; final full log compiler phases |
| `mvn -B -ntp <agent> -Dtest=CustomerSubjectClaimsWebTests,CustomerSubjectClaimServiceTests,SubjectClaimVerificationReferenceTests,SubjectClaimPersistenceConfigurationTests,CustomerSubjectClaimBoundaryTests test` | **0;51 tests /5 classes;0 failures/errors/skips**; exact bare source/CI/XML names independently checked. | `/tmp/pa30-retry1-51-final.log`; `/tmp/pa30-retry1-focused-xml/TEST-*.xml` |
| `mvn -B -ntp <agent> -Dtest=ModuleBoundaryTests,CustomerApiBoundaryTests,CustomerWebConfigurationTests,OneCallApplicationTests,CustomerBusinessNumberBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests test` | **1;39 tests /8 classes;0 failures,3 errors,0 skips**. Three existing embedded-server configuration cases cannot create sockets. All other cases, including all module/dependency/read-only boundary assertions, pass. | `/tmp/pa30-retry1-regression.log`; `/tmp/pa30-retry1-regression-xml/` |
| `mvn -B -ntp <agent> clean verify` | **1;468 tests /54 classes;0 failures,64 errors,0 skips**.404 pass,64 error. No all-green default-suite or package/JAR claim. Repeated final run confirms same bounded socket blocker. | `/tmp/pa30-retry1-468-final.log`; fresh `backend/target/surefire-reports/TEST-*.xml` |
| Extract/compile all three exact CI Python bodies; execute only source/CI/POM/historical-map statements | **0**; exact names/classes,468/370/838, annotation839, POM23 and all historical arithmetic pass. Does not execute runtime bodies or container XML. | `/tmp/pa30-ci-static.py`; emitted source-only PASS |
| OpenAPI offline dependency install in private copied tree, then full `npm run check:s3` | **0** after correcting the private copy to include both frozen docs14/15. Lint/bundle/generate/typecheck/doc-map/media-gateway and s3 pass:49 operations,18/21/6/3/1,44 A0 catalogs, zero doc/source-map discrepancies. | `/tmp/pa30-retry1-openapi2.log`; tree `/tmp/pa30-openapi-check/` |
| Plan/fix hashes and frozen20 raw/LF entry/final comparison | **0; exact matches**, no normalized-away repository edit | `/tmp/pa30-retry1-baseline.json`, `/tmp/pa30-retry1-frozen.json`, `/tmp/pa30-scope-check.py` |
| Source/CI inventory; whitespace, UTF-8, no BOM, final newline; customer/module/resource counts | **0**; no candidate whitespace/encoding issues;43 customer internal files,14 modules,17 migrations | Static command output and scope JSON |
| Windows `git.exe diff --check`, `status --short`, `diff --name-status`, frozen-path `diff --exit-code a4c6ab... -- ...` | **1 each**, WSL UtilBindVsockAnyPort socket failed1 before reading Git metadata. Not passing Git/branch/index evidence. Linux Git not invoked. | Captured terminal diagnostics; Controller must repeat in working Windows shell |

Earlier bounded diagnostics are retained, not hidden: preliminary new-test compilation exposed four effectively-final lambda errors and then two typed-jOOQ scalar probe errors; both were repaired within the allowlist. First focused attempts without premain failed Mockito self-attach and a MockMvc trailing-servlet-path construction. Premain removed the attach error; two subsequent focused attempts found a duplicate-Content-Type test-construction issue, repaired with an explicit duplicate-header servlet wrapper (still requiring422 and no dispatch). Later XML comparison caught CapturedOutput test-parameter suffixes; capture now occurs in BeforeEach, preserving every required no-argument method name. Final compile and51-method focused run pass. Initial private OpenAPI copy lacked docs14 and failed lint; adding the unchanged file to the private copy yielded the complete passing check. No repository contract/toolchain or test assertion was disabled to address these diagnostics.

### Full default runtime blocker

Each affected class's fresh XML contains the causal `java.net.SocketException: Operation not permitted` from `sun.nio.ch.Net.socket0` / embedded Tomcat startup. Subsequent context-threshold errors in web suites follow the same initial denied socket. The original eight-class regression has the same three CustomerWebConfigurationTests errors. No failing new PA30 method or assertion failure remains. This is **not** a passing468 gate and not a reason to exclude those tests or change network/Docker/WSL configuration.

| Existing class with errors | Error count |
| --- | --- |
| `com.onecall.archtests.CustomerBusinessNumbersWebTests` | 12 |
| `com.onecall.archtests.CustomerEvidenceConfigurationTests` | 4 |
| `com.onecall.archtests.CustomerEvidenceWebTests` | 12 |
| `com.onecall.archtests.CustomerMeWebTests` | 8 |
| `com.onecall.archtests.CustomerSharesWebTests` | 10 |
| `com.onecall.archtests.CustomerSubjectDetailConfigurationTests` | 2 |
| `com.onecall.archtests.CustomerSubjectsWebTests` | 12 |
| `com.onecall.archtests.CustomerWebConfigurationTests` | 3 |
| `com.onecall.archtests.MediaGatewayConfigurationTests` | 1 |

The final default XML has exact54-class/468-method source identities, including the five green PA30 classes, but64 error nodes prevent green XML acceptance. The full838 source=CI=XML gate is NOT RUN because Failsafe/container validation is controller-owned and no such XML is fabricated/reused.

## 7. Controller-deferred mandatory validation

Every item below is **NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION** in this child:

- PostgreSQL16/Flyway17 fixture boot, actual synthetic login attributes/membership/ACLs, role/current_user/PID and dedicated pool authority, same-connection local SPI probes, real locking/current-reread/zero-risk phantom behavior, races and append_fact durability, canary logs and reused-session cleanup. All34 exact methods are authored and test-compiled, not skipped/disabled/replaced.
- Focused Failsafe: `mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerSubjectClaimsIT,SubjectClaimRoleLeastPrivilegeIT -DfailIfNoTests=false verify` (expect34 new methods); full `mvn -B -ntp clean -Pcontainer-smoke verify` (expect468+370=838 in54+23=77 classes, zero failures/errors/skips). No Docker command was invoked, no socket/configuration workaround attempted, and no PostgreSQL/Testcontainers/Failsafe execution is claimed.
- Full all-green default468 rerun outside this socket-restricted child is also mandatory; the child attempted it and failed honestly as detailed above. Controller/runtime owner must supply a context that permits existing embedded-server tests, without excluding them. Use the approved installed toolchain; premain is needed only where self-attach remains restricted.
- Packaged default/web JAR probes, including new POST401/no-store/closed-problem probe; JAR route/class/test-provider checks; exact dependency firewall with intermediate82/final116 unchanged; both freshly generated nonempty target/jar-index.txt and target/pa11-runtime-dependencies.txt prerequisites; exact plan10.2 extractor order, full source=CI=XML identity and XML-green checks. No JAR/firewall prerequisites or reports are faked. The default failed before packaging, so there is no completed-package claim.
- Windows Git diff/check/status/base/index custody in the Controller's functioning shell. Filesystem hashes are complementary, not Git evidence.

The plan's privilege stop condition remains: if real frozen-role tests identify a needed grant/schema delta, stop for Controller goal reconfirmation; do not introduce V018, broaden a role or use administrator/query credentials as a substitute. Any bounded implementation-internal runtime failure should be returned for an in-allowlist repair before accepting the candidate.

## 8. Frozen20 byte custody

Exactly20 unique files =OpenAPI +docs14 +docs15 +17 migrations, with V003 counted once and Control excluded. All raw AND LF-canonical hashes match the accepted dispatch values at entry and completion. The migration resource set remains exactly V001–V017, with no V018. A06 embeds both independently pinned representations: it accepts only the known raw-CRLF or canonical-LF whole-file digest for checkout portability, still requires the exact LF digest, exact20 unique identities and exact17 migration resource names, and rejects other bytes/extra resources. The separate local before/after audit requires the original raw hash exactly and does not normalize a forbidden edit away.

| Frozen path | Raw SHA-256 | LF-canonical SHA-256 |
| --- | --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` | `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` |
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

## 9. Documentation, residual ownership and gate boundary

README adds current one-operation truth, unavailable defaults, exact flags/credentials, role/lock/transaction/retry/logical-reference/audit behavior, no transfer, target468/370/838 and54/23/77 counts,839 annotation lines,23 includes,17 unchanged migrations, unchanged OpenAPI, staged82/116 firewall and real validation limits. PA29 and older evidence stays labeled historical; no new runbook, design/doc-map/Control edit or historical artifact rewrite is delivered. CI has explicit pa30_unit_additions / pa30_container_additions literal maps, historical677 and753 baselines preserved, updated early/final468/370 expectations, source/class/POM equality and the packaged POST401 probe. Dependencies/firewall catalog/probe assertions are unchanged.

Residual owners/destinations:

- Controller / Backend QA: complete all deferred container/race/ACL/PID/audit/canary/cleanup/XML/JAR/firewall gates, and rerun the full default suite outside the denied-socket sandbox; no green acceptance yet.
- Controller / Windows environment: repeat Git custody checks; supplied ancestry/HEAD was not independently verified here.
- Subject-number / Security / Platform, under a later newly confirmed goal: real proof verifiers, external I/O transaction design, encryption/KMS/key ownership, production credentials/provisioning, operational observability and replay-audit growth. No current provider/credential/readiness claim.
- Backend/Controller: repair any bounded candidate defect exposed by mandatory runtime validation within the41-path boundary; an actual migration/privilege need requires reconfirmation, never an implicit new grant.

No commit, push, branch creation, PR, merge, approval, mark-ready, reviewer request, external comment, issue mutation, delegation, Control edit, Docker invocation/configuration change, forbidden path write or later gate was performed. Completion is an implementation candidate with explicit validation blockers/obligations, not acceptance or production readiness.

## 10. Bounded implementation fix 1 — provider context selection

### Root cause and scoped repair

The Controller's supplied focused PostgreSQL16 run of `CustomerSubjectClaimsIT` and `SubjectClaimRoleLeastPrivilegeIT` failed during Spring context startup with `BeanDefinitionOverrideException` for `subjectClaimProofVerifier`; no assertion or database test executed. The two `@ConditionalOnMissingBean` defaults were ordinary configuration bean methods, so they could register before the fixture's same-name controlled definitions. These conditions cannot safely rely on regular-configuration discovery order.

This bounded repair changes only these three approved candidate paths and this existing report; no new repository path is introduced:

1. `backend/src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceConfiguration.java`: move the two unchanged, always-`Unavailable` fallback factories into a package-private class in the same source file, imported only through an internal `DeferredImportSelector`. The fallback class has no component/configuration stereotype and therefore cannot be independently component-scanned before the deferred import. Missing-bean checks now see regular user/fixture definitions before fallback registration. Existing persistence profile/enablement/credentials and all pool/transaction/DSL/repository/owner qualifiers remain unchanged. The owner still rejects any provider count other than one; no property escape hatch, override, primary selection or permissive fallback is introduced.
2. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimFixture.java`: the strengthened configuration test exposed a second startup defect after removing the name collision: both fixture factories registered the same dual-interface `ControlledSubjectClaimProviders` object under separate definitions, making the owner's type-provider streams count two candidates of each SPI. Register that object once with the exact proof-verifier name and cipher alias instead. Both required qualified lookups resolve to that same controlled object, and there is exactly one type candidate per SPI. The exact `web,persistence` profiles, dedicated credentials/pools/beans, and complete preflight are unchanged; no endpoint or database assertion is bypassed.
3. `backend/src/test/java/com/onecall/archtests/SubjectClaimPersistenceConfigurationTests.java`: strengthen existing C05 `defaultProvidersAreUnavailableAndControlledReplacementsAreUnique`, without adding or renaming a test method. Use real Spring configuration parsing with overriding explicitly disabled, the exact fixture `ControlledBeans` class, exact `web,persistence` profile pair, and both source-registration orders. Assert one verifier/cipher/owner, exact controlled identity and alias, and default `Unavailable` verify/seal/open behavior. Retain the direct owner wiring/duplicate-verifier rejection assertions and add duplicate-cipher rejection. Only the unit context's Hikari instance supplier is mocked to avoid opening a database; all configuration/provider selection and owner creation execute normally. The real fixture and its PostgreSQL tests retain the real dedicated pool and database path.

### Child validation evidence

All Maven commands run from `backend` with `JAVA_HOME=/opt/jdk21` and `PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin`, using `/opt/maven-3.9.9/bin/mvn`. `<agent>` below is exactly `-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar`.

- `mvn -B -ntp test-compile`: **exit 0**, final post-repair compile; `/tmp/pa30-fix1-test-compile-final.log`. Both ITs and the modified fixture compile.
- `mvn -B -ntp <agent> -Dtest=CustomerSubjectClaimsWebTests,CustomerSubjectClaimServiceTests,SubjectClaimVerificationReferenceTests,SubjectClaimPersistenceConfigurationTests,CustomerSubjectClaimBoundaryTests test`: **exit 0; 51 tests / 5 classes, zero failures/errors/skips**. Configuration class remains **8/8**. Fresh evidence: `/tmp/pa30-fix1-focused-final.log`, `/tmp/pa30-fix1-focused-xml/TEST-*.xml`.
- Required full `mvn -B -ntp <agent> test`: **exit 1; 468 tests /54 classes, 0 failures, 64 errors, 0 skips (404 pass)**. Fresh `/tmp/pa30-fix1-full-test.log` and `/tmp/pa30-fix1-full-test-xml/TEST-*.xml` confirm the same nine pre-existing embedded-server classes listed in section 6; every affected class XML contains causal `java.net.SocketException: Operation not permitted`. All 468 source/XML method identities match exactly; all five PA30 classes, including strengthened C05, remain green. This is an environmental runtime blocker, not an all-green full-suite result. No excluded test, socket workaround or assertion weakening is used.
- Execute the existing private CI source-only extractor `/tmp/pa30-ci-static.py`: **exit 0**; source=CI exact method/class names, **54/468 +23/370 =77/838**, annotation **839**, POM includes **23**. Independently compare all source method lists with `/tmp/pa30-fix1-methods.json`: all **838** identities, including the exact **85** PA30 methods, remain unchanged. Customer files/modules/migrations and frozen definitions are untouched.
- Compare private entry snapshot `/tmp/pa30-fix1-baseline.json` against final source bytes and verify membership in accepted plan section 4: exactly the three listed candidate paths plus this report may differ. Frozen **20 raw/LF pairs** match `/tmp/pa30-retry1-frozen.json`; no OpenAPI, docs14/docs15, V001–V017, plan/fix or Control change. Source whitespace checks pass.
- Windows `git.exe diff --check`, `diff --name-status`, `status --short`: **exit 1 each**, WSL `UtilBindVsockAnyPort` socket failed before Git evidence; still Controller-deferred. An initial Linux `git status --short` was inadvertently attempted before reading the original prompt, failed immediately on the Windows worktree metadata path, and produced no status/diff evidence or repository write. It was not repeated or used as substitute evidence.

Preliminary repair diagnostics are not hidden: first test-compile failed because the assertable context does not expose `getDefaultListableBeanFactory`; use its public `getBeanFactory()` with the known factory type instead. The first focused run compiled but C05 failed with `SERVICE_UNAVAILABLE` because the original fixture registered its dual-interface provider twice. The alias repair above resolves this without weakening the owner's uniqueness check; the final 51-method focused run is green.

### Controller-owned PostgreSQL16 result and remaining gate

**NOT RUN — CONTROLLER POST-CANDIDATE VALIDATION: 34 PostgreSQL16 methods in 2 classes (24 CustomerSubjectClaimsIT +10 SubjectClaimRoleLeastPrivilegeIT). Child executed 0 PG16 methods.** The supplied failed Controller run executed no assertions/database tests and is not a passing result. Controller must rerun its exact focused `-Pcontainer-smoke -Dit.test=CustomerSubjectClaimsIT,SubjectClaimRoleLeastPrivilegeIT -DfailIfNoTests=false verify` command after this repair, then all previously deferred full-container/JAR/firewall/XML and Windows Git custody checks. No Docker command, database bypass, skip, bean-definition overriding, test-count change, commit, remote action, delegation or later gate is authorized or performed. This is a bounded candidate repair, not acceptance or production readiness.

## 11. Bounded implementation fix 2 — PostgreSQL timestamp parameter typing

### Authority, Controller evidence and root cause

Task `pa30-subject-claim-s1-implementation-fix2-gpt-20261113-01`; supplied runtime/provider Codex `gpt-6.1-sol`, reasoning `high` (dispatch metadata, not independently introspected); 2026-11-13, Asia/Shanghai. Implementation repair only, single approved S1 slice; no delegation or code-review/later gate. Accepted plan/fix-1/fix-2 SHA-256 values in section 1 were verified exactly at entry and completion.

The latest supplied Controller run reached PostgreSQL **16.15**, applied all **17** migrations and executed all **34** focused Failsafe methods: **19 failures, 4 errors, 11 passing**. Its JDB evidence on a fresh valid claim identified `risk_checked_at` receiving a character-varying expression rather than `timestamp with time zone` from the plain-SQL jOOQ bind. The rejected INSERT therefore returned sanitized503 before valid/domain outcomes or injected retry-trigger paths could execute. This is an implementation parameter-typing defect, not evidence of a missing grant, migration or changed policy. The supplied retry-count result was one attempt rather than three; the retry algorithm is not changed speculatively.

### Exact repair and semantic preservation

This fix changes exactly two existing repository paths, both already inside the accepted 41-path-plus-report boundary:

1. `backend/src/main/java/com/onecall/core/subject_number/internal/claim/JooqSubjectClaimRepository.java`: exactly two SQL-string line replacements. Cast both trailing INSERT parameters as `?::timestamptz` for `subject_claim.risk_checked_at` and `verified_at`, and cast the conditional owner UPDATE's `business_subject.updated_at` parameter as `?::timestamptz`. The audit call already uses `audit.append_fact(?::timestamptz,...)`; inspection confirms that its time parameter is explicitly typed, so that SQL and its payload remain unchanged.
2. `docs/gateflow/pa30-customer-personal-subject-claim-foundation-s1-implementation.md`: update the current-task heading/summary and append this bounded-fix evidence section. No test, fixture, provider/configuration, wrapper or other candidate path changes in this repair.

The original ten-column/ten-value INSERT still has nine placeholders and literal organization NULL, with identical argument order and values. The existing null-preserving `Instant` to UTC `OffsetDateTime` helper is untouched: SQL NULL remains NULL after the cast, so REJECTED/CONFLICT `verified_at` is not filled or coerced into another time. Database clock selection and accepted-time/risk-time ordering remain unchanged. Owner columns, expected-revision/null-owner predicates and update count, transaction/connection/role, locks/rereads, audit metadata/exact three-key snapshot, error precedence/classification and three-whole-attempt retry budget remain byte-unchanged outside those two SQL strings. No method identity/count changes or new policy are introduced. Existing PostgreSQL fresh-claim, denial-history, owner-update, audit and retry methods already cover the affected paths; none is weakened, skipped, renamed or replaced. This static repair does not establish that every Controller failure is resolved; actual PG16 execution is deferred.

### Child validation actually executed

All commands run from `backend` with `JAVA_HOME=/opt/jdk21`, `PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin` and `/opt/maven-3.9.9/bin/mvn`. `<agent>` is exactly `-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar`, the previously documented test-only premain needed for restricted self-attach, not a test exclusion or assertion override.

| Executed command/check | Actual result | Fresh private evidence |
| --- | --- | --- |
| `mvn -B -ntp test-compile` after the repair | **exit 0**; main and all 110 test-source files compile, including both ITs/fixture | `/tmp/pa30-implementation-fix2-test-compile.log` |
| `mvn -B -ntp <agent> -Dtest=CustomerSubjectClaimsWebTests,CustomerSubjectClaimServiceTests,SubjectClaimVerificationReferenceTests,SubjectClaimPersistenceConfigurationTests,CustomerSubjectClaimBoundaryTests test` | **exit 0; 51 tests /5 classes, 0 failures, 0 errors, 0 skips**; fresh XML names exactly match source | `/tmp/pa30-implementation-fix2-focused.log`; `/tmp/pa30-implementation-fix2-focused-xml/` |
| Default full `mvn -B -ntp <agent> test`, without exclusions | **exit 1; 468 tests /54 classes, 0 failures, 64 errors, 0 skips; 404 passing**. All five PA30 classes remain green; all 468 XML method identities match source | `/tmp/pa30-implementation-fix2-full.log`; `/tmp/pa30-implementation-fix2-full-xml/`; `/tmp/pa30-implementation-fix2-full-summary.json` |
| Existing private source-only CI extractor `python3 /tmp/pa30-ci-static.py` | **exit 0**; exact source=CI method/class identities, **54/468 +23/370 =77/838**, annotation839, POM23, preserved historical arithmetic; no Failsafe XML/runtime execution | Terminal PASS output |
| Frozen20 raw/LF and binding plan/fix hashes; entry/final filesystem and method inventories | **PASS**; exact 41 candidates (25 new/16 existing) plus existing report retained; this task changes only the two listed paths, no additions/deletions; all **838** test identities/counts and all test-source bytes unchanged | `/tmp/pa30-implementation-fix2-entry.json`; `/tmp/pa30-implementation-fix2-final-checks.json`; `/tmp/pa30-implementation-fix2-repository.diff` |
| Windows `git.exe diff --check`, `diff --name-status`, `status --short` | **exit 1 each** before Git metadata access: WSL `UtilBindVsockAnyPort` socket failed1. Git diff/status/branch custody remains unavailable, not passing evidence; local changed-file whitespace checks pass separately | `/tmp/pa30-implementation-fix2-git-diff-check.log`; `/tmp/pa30-implementation-fix2-git-diff.log`; `/tmp/pa30-implementation-fix2-git-status.log` |

Fresh full-suite XML confirms the same nine socket-blocked classes and counts listed in section 6: CustomerBusinessNumbersWebTests12, CustomerEvidenceConfigurationTests4, CustomerEvidenceWebTests12, CustomerMeWebTests8, CustomerSharesWebTests10, CustomerSubjectDetailConfigurationTests2, CustomerSubjectsWebTests12, CustomerWebConfigurationTests3, MediaGatewayConfigurationTests1. Every affected class report contains causal `java.net.SocketException: Operation not permitted`; context-threshold errors follow denied embedded-server startup. The full suite is **not green** and requires a Controller/environment-owner rerun without exclusions. Earlier report directories were moved aside before each suite, so the 51- and 468-method evidence is fresh rather than mixed leftovers.

A preliminary patch invocation used a repository-relative path while already in `backend` and failed before changing any file; its accompanying entry compile passed but is not the post-repair compile evidence above. The patch then applied from repository root and the required post-edit test-compile/focused/full sequence ran as recorded. No path was created by that failed attempt. An initial Linux `git status --short` likewise failed immediately on the Windows gitdir; it supplied no custody evidence, was not repeated and is not a substitute for Controller Git checks.

### Frozen scope and Controller-deferred follow-up

All **20 unique frozen raw/LF pairs** match the accepted fix-2 oracles at entry and completion: OpenAPI, docs/14, docs/15 and exactly V001–V017; V003 is counted once. All three binding plan/fix digests match the dispatch. The private entry/final snapshot proves every other repository input, including Control, `backend/mvnw`, all tests/fixtures and provider configuration, is unchanged by this repair. Comparison with the retained pre-implementation baseline confirms exactly **41 plan paths + this report**, **25 additions/17 modifications including the report**, no out-of-bound path or deletion. This is filesystem evidence, not a successful Git diff/index claim. All runtime method identities/counts are preserved by the exact two-SQL-line-only code delta.

**NOT RUN IN THIS CHILD — 34 PostgreSQL16 Failsafe methods remain for Controller rerun**: 24 `CustomerSubjectClaimsIT` +10 `SubjectClaimRoleLeastPrivilegeIT`. Child executed zero PostgreSQL/Testcontainers/Failsafe methods and invoked no Docker command. Controller must rerun its supplied focused `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerSubjectClaimsIT,SubjectClaimRoleLeastPrivilegeIT -DfailIfNoTests=false verify` command with `/opt/jdk21`, checking fresh acceptance, NULL denial timestamps, conditional owner update, audit append and all three retry attempts on actual PG16. No passing PostgreSQL result is asserted. Prior deferred full-container/JAR/firewall/XML and functioning-Windows-Git custody obligations remain unchanged; neither full838 nor package/firewall acceptance is claimed here.

No commit, push, PR, merge, approval, mark-ready, reviewer request, external comment/issue mutation, delegation, Control/frozen-artifact edit, migration/new grant, bean override/ACL widening, production credential/provider/KMS action, external-system action or later gate occurs. Stop at this bounded implementation repair; Controller owns the next PG16 execution. Candidate status remains **BLOCKED_RUNTIME_VALIDATION**, not acceptance or production readiness.

## 12. Bounded fix 3 — cross-account global-request race orchestration

Task: `pa30-subject-claim-s1-implementation-fix3-gpt-20261113-01`; supplied runtime/provider Codex `gpt-6.1-sol`, reasoning `high` (dispatch metadata, not independently introspected); date 2026-11-13, Asia/Shanghai. Gate remains **implementation**, the single approved PA30 S1 slice only. No delegation or code-review/later-gate action occurs.

### Exact supplied Controller fix-2 PostgreSQL result

The Controller supplied this command, run from `backend`:

```bash
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin \
  /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke \
  -Dit.test=CustomerSubjectClaimsIT,SubjectClaimRoleLeastPrivilegeIT \
  -DfailIfNoTests=false verify
```

- Surefire: **468/468**, zero failures/errors/skips.
- PostgreSQL **16.15**, all **V001–V017** migrations: pass.
- `SubjectClaimRoleLeastPrivilegeIT`: **10/10**, all pass.
- `CustomerSubjectClaimsIT`: **23/24**; the sole remaining error is `crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict`, `java.util.concurrent.TimeoutException` at `CustomerSubjectClaimsIT.race(CustomerSubjectClaimsIT.java:312)`.
- Supplied fresh log: `D:\codex-temp\pa30-s1-implementation-gpt-20261112-01\fix2\controller-focused-fix2.log`. These are Controller-provided results, not a child rerun or independently read log claim.

### Diagnosis and minimal repair

Both original requests used token `personal` and authenticated `fixture.account`, despite targeting independent subjects. The confirmed production order acquires `customer_iam.account:<account>` before request-row lookup and claim insertion. The first transaction therefore waited in the two-party INSERT barrier while the second correctly waited on the same account advisory lock and could not reach that barrier. Its 10-second bound exposed a **test-orchestration deadlock**, not a production locking, ACL, timestamp or migration defect. Global `uq_claim_request` uniqueness spans subjects and accounts; distinct authenticated accounts let both independent transactions reach their inserts without changing production locks.

Only these two existing repository paths change in this bounded task:

1. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java`: within the unchanged method `crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict`, retain two independently seeded subjects and `global-race`; keep the first approval/token as `fixture.account`/`personal`, and use `fixture.other`/`other` for the second. Retain exactly one 200 and one 409, exactly one observed PostgreSQL `23505`, and exactly one claim row. Branch on the actual winning response: first winner owns only first as `fixture.account + "|null|2"`, or second winner owns only second as `fixture.other + "|null|2"`; the other subject remains `null|null|1`. Validate the losing response through existing `fixture.checked(..., 409)` and `code(..., "CLAIM_REPLAY_CONFLICT")`, preserving the canary checks and closed problem shape with no stored outcome or identifiers.
2. This existing implementation report: record the bounded repair and evidence, without another report path.

The real `CyclicBarrier(2)` on the first two claim INSERT attempts, its 10-second wait, `race`'s pool size 2 and bounded futures/cleanup, all 24 IT method identities and all other test methods remain unchanged. No sleeps, skips, assertion reduction, production source/config/lock/transaction/order change, grant/ACL widening, migration or OpenAPI change occurs.

### Child validation and scope custody

All Maven commands use `/opt/maven-3.9.9/bin/mvn`, `JAVA_HOME=/opt/jdk21` and `PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin`. The focused invocation retains the previously documented test-only Mockito premain for restricted self-attach: `-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar`; this is not a test exclusion, socket workaround or behavioral override.

| Executed command/check | Actual result | Fresh private evidence |
| --- | --- | --- |
| From workspace root: `mvn -B -ntp -f backend/pom.xml test-compile` | **exit 0**; main/test compile succeeds, including all 110 test-source files and both ITs | `/tmp/pa30-implementation-fix3-test-compile.log` |
| From `backend`: `mvn -B -ntp <premain above> -Dtest=CustomerSubjectClaimsWebTests,CustomerSubjectClaimServiceTests,SubjectClaimVerificationReferenceTests,SubjectClaimPersistenceConfigurationTests,CustomerSubjectClaimBoundaryTests test` | **exit 0; 51/51 in 5 classes**, zero failures/errors/skips; fresh XML exactly matches source method/class identities | `/tmp/pa30-implementation-fix3-focused.log`; `/tmp/pa30-implementation-fix3-focused-xml/`; `/tmp/pa30-implementation-fix3-focused-summary.json` |
| `python3 /tmp/pa30-ci-static.py` (existing source-only CI extractor) | **exit 0**; source=CI exact method/class identities remain **54/468 +23/370 =77/838**, annotation839, POM23; no full XML/runtime claim | Terminal PASS output |
| Binding hashes, frozen20 raw/LF, entry/final filesystem and method inventories, changed-file whitespace | **PASS**; all three plan/fix digests match dispatch; all20 raw/LF pairs unchanged; all838 method identities preserved, including IT24; only the two paths above change in this task | `/tmp/pa30-implementation-fix3-entry.json`; `/tmp/pa30-implementation-fix3-final-checks.json`; `/tmp/pa30-implementation-fix3-test.diff` |
| Retained pre-implementation baseline versus final filesystem | **PASS**; exact **41 plan paths + existing report**, **25 additions/17 modifications including report**, no deletion or out-of-bound path; Control and all production/config/frozen inputs unchanged by this task | `/tmp/pa30-implementation-fix3-final-checks.json` |
| Windows `git.exe diff --check` and `git.exe status --short` | **exit 1 each**, WSL `UtilBindVsockAnyPort:309: socket failed 1`; not passing Git custody | `/tmp/pa30-implementation-fix3-git-diff-check.log`; `/tmp/pa30-implementation-fix3-git-status.log` |

Previous Surefire reports were moved outside the repository before the focused suite; its five XML reports are fresh. The root-level `test-compile` generated `build/generated-spring-modulith/javadoc.json`; the initial boundary check correctly detected this extra generated path, which was removed along with its empty directories before the final comparison. No pre-existing file was removed. Generated `target` trees remain excluded from source boundary snapshots. Final filesystem checks are complementary evidence, not successful Git index/diff/branch/base verification. Linux Git also failed on the Windows `D:/codex-call/.git/worktrees/codex-call7` gitdir at entry. No Git metadata workaround is attempted.

Known embedded-server/socket restrictions and the historical child full-default failures remain documented in sections 6 and 11; this bounded child does **not** rerun the full suite or claim those restrictions are resolved. The supplied Controller full Surefire result is green as recorded above.

**NOT RUN IN THIS CHILD:** Docker, PostgreSQL, Testcontainers, Failsafe, full838 container execution, packaged JAR/firewall execution and later gates. Child executes zero PostgreSQL/Failsafe methods and invokes no Docker command. Controller owns the next focused **34-method** Failsafe rerun (24 claim +10 role methods), then the full **838-method** container gate and outstanding package/firewall/Git custody. This test repair does not assert a passing PostgreSQL rerun. Candidate remains **BLOCKED_RUNTIME_VALIDATION** pending that evidence.

No commit, push, PR, merge, approval, mark-ready, reviewer request, external comment/issue mutation, delegation, production credential/provider/KMS action or later-gate transition occurs. Stop after this bounded test-orchestration repair and existing-report update.

## 13. Final Controller validation — implementation evidence complete

### 13.1 Authority, provenance and bounded-fix closure

Task `pa30-subject-claim-s1-evidence-finalize-gpt-20261113-01`; supplied runtime/provider Codex `gpt-6.1-sol`, reasoning `high` (dispatch metadata, not independently introspected); 2026-11-13, Asia/Shanghai. This task edits only this existing report. The complete accepted plan, current report and all six Controller logs below were read; the Windows log paths were accessed through their `/mnt/d/codex-temp/pa30-s1-implementation-gpt-20261112-01/` equivalents. Command exit codes and the frozen/Git/prerequisite custody observations are attributed to the Controller; log contents corroborate the runtime summaries. No historical suite, Docker, PostgreSQL, Maven, JAR or OpenAPI command is rerun by this child, and no source/test review is performed.

Binding input raw SHA-256 values were rechecked before this report edit and match the supplied identities: accepted plan `71188F38486578979CFCC7A9949900C3B06F90A14AE318002F3A0281409A0191`; plan fix-1 `0D7C3F182D1DF383B7A75751FC029CF2297021A2E6632029C8AF5FB972500FD0`; plan fix-2 `9C5DE2B2FC9801EA6119039177C875A8BB2E2124B8E5607D5B0F52C44AF00524`. These plan-fix artifacts are distinct from implementation bounded fixes 1–3 below and remain untouched.

| Historical failure / limitation | Bounded closure and final evidence |
| --- | --- |
| Initial Controller fixture startup failed with `BeanDefinitionOverrideException`; controlled dual-interface providers also appeared twice in type lookup. | Implementation fix 1 deferred unavailable fallback registration and registered the controlled object once with a cipher alias, retaining strict uniqueness and disabled overriding. Strengthened existing C05 covered both registration orders. Subsequent Controller PG16 execution reached all 34 methods, and the final focused/full gates pass. Section 10 preserves the original failure and repair. |
| The next Controller PG16 run had 19 failures, 4 errors and 11 passes; plain-SQL timestamp binds rejected `risk_checked_at` as character varying, preempting domain/retry outcomes. | Implementation fix 2 added explicit `timestamptz` casts to the claim timestamp parameters and conditional owner timestamp parameter only. The next Controller focused run passed SQL/domain paths and 33/34 Failsafe methods; its only remaining error was the race barrier timeout below. Section 11 preserves the defect and exact two-SQL-line repair. |
| After fix 2, `crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict` timed out: both requests shared one account advisory lock, preventing the second transaction from reaching the two-party INSERT barrier. | Implementation fix 3 used distinct authenticated accounts for the independent subjects, preserving production locking, the real barrier/23505, winner/loser ownership and replay-conflict assertions. Controller then passed the isolated race 1/1, final focused Failsafe 34/34 and clean full-container gate. Section 12 preserves the orchestration repair; no assertion was skipped or weakened. |
| Historical child full-default runs had 64 embedded-server socket errors, and child Windows Git was unavailable. | These child failures remain recorded, not relabeled as passes. Controller's clean full-container command ran all 468 Surefire methods successfully without exclusions, then all 370 Failsafe methods; Controller also supplied passing Windows Git/frozen/scope custody below. The child's local restrictions are not claimed repaired. |

### 13.2 Focused PostgreSQL16 progression

| Controller log (exact Windows path) | Recorded outcome |
| --- | --- |
| `D:\codex-temp\pa30-s1-implementation-gpt-20261112-01\fix2\controller-focused-fix2.log` | Historical post-timestamp/pre-orchestration result: SQL passed; **468/468 Surefire** passed; **33/34 Failsafe** passed. Sole remaining error: `crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict` barrier timeout (`TimeoutException`); build failed, not a green gate. |
| `D:\codex-temp\pa30-s1-implementation-gpt-20261112-01\fix3\controller-single-race.log` | Controller-observed **exit 0**; repaired race **1/1 Failsafe** passed, zero failures/errors/skips. This isolated result is not substituted for the focused or full gates. |
| `D:\codex-temp\pa30-s1-implementation-gpt-20261112-01\fix3\controller-focused-fix3-final.log` | Controller-observed **exit 0**; **468 Surefire /54 classes +34 Failsafe /2 classes =502 tests /56 classes**, zero failures/errors/skips. All 24 claim IT methods and all 10 least-privilege IT methods pass, including the repaired race. |

These are real Controller PostgreSQL **16.15** container results with **V001–V017** migration evidence, not mock-only or child execution claims. They supersede the historical pending Controller validation labels for I01–I24/L01–L10 in section 5; those child-evidence rows remain unchanged for provenance.

### 13.3 Final clean full-container gate

Exact Controller log: `D:\codex-temp\pa30-s1-implementation-gpt-20261112-01\fix3\controller-full-container-final.log`. Controller observed **exit 0** for:

```bash
cd backend
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin \
  /opt/maven-3.9.9/bin/mvn -B -ntp clean -Pcontainer-smoke verify
```

| Final clean-build inventory | Tests | Classes | Failures / errors / skips |
| --- | --- | --- | --- |
| Surefire | 468 | 54 | 0 /0 /0 |
| Failsafe | 370 | 23 | 0 /0 /0 |
| Total | **838** | **77** | **0 /0 /0** |

The clean build recompiled **200 main** and **110 test source files**, produced `backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar` and repackaged it as the executable JAR. PostgreSQL **16.15** container and **V001–V017** evidence appears across the Failsafe logs; there is **no V018**. The complete 838/77 result is from this final clean Controller run, not focused-report leftovers, skipped-container execution, or a claimed CI-hosted run. Its Surefire phase closes the full-default test obligation; no separate final standalone `clean verify` execution is invented.

### 13.4 Exact inventory, packaged JAR, firewall and OpenAPI

**Inventory/JAR/firewall log:** `D:\codex-temp\pa30-s1-implementation-gpt-20261112-01\evidence\inventory-jar-firewall.log`; Controller observed **exit 0**.

- Controller confirmed `target/jar-index.txt` and `target/pa11-runtime-dependencies.txt` were freshly generated and proven nonempty before extracting/executing the CI bodies; stale or missing prerequisites were not accepted.
- Exact **source=CI=XML: 54/468 +23/370 =77/838**, annotation-line scans **839**, POM explicit includes **23**, zero skips/errors/failures. The historical single commented annotation remains non-executable; 839 is not the test total.
- Default/web JAR smokes passed, including the packaged route boundary; **no DB/verifier adapter** and JVM cleanup checks passed. Test-controlled proof/cipher collaborators are not production adapter evidence.
- Firewall passed: baseline **37/34**, SSM **72/65**, counterparts **64**, dependency-only **8**, packager **1**; deltas **35/0** and **31/0**; final **116 adversarial probes**. This is completed packaged/dependency/firewall evidence, not just a JAR listing or source-only check.

**OpenAPI log:** `D:\codex-temp\pa30-s1-implementation-gpt-20261112-01\evidence\openapi-check-s3.log`; Controller observed **exit 0**. Redocly lint, bundle, generated client, TypeScript compatibility, doc map and media-gateway lint all passed. Stage **s3** has **49 operations** with exact counts **customer=18 /operations=21 /ivr=6 /public-share=3 /provider-callback=1**. Doc missing/extra/duplicate/invalid are all **0**; source duplicate paths/routes/operation IDs are all **0**. No OpenAPI or frozen design document edit is made.

### 13.5 Frozen inputs and Git custody

The following are Controller-independent checks of the candidate, supplied by the Controller rather than represented as successful child Git execution:

- Controller independently parsed the **20 expected raw/LF pairs** from accepted plan fix-2 and recomputed all **20 files**: **PASS**. The exact set is OpenAPI +docs/14 +docs/15 +17 migrations, with V003 counted once and mutable Control excluded; both raw and LF representations were checked.
- `git.exe diff --exit-code a4c6abacc8afeec14d0308e854a7daff442a770f -- <OpenAPI +17 migrations +docs/14 +docs/15>`: **exit 0**. This records the Controller's frozen-path selector, not a literal runnable angle-bracket command.
- `git.exe diff --check`: **exit 0** on the Controller candidate before this report-only finalization. It is not a claim that child Windows Git works or that the Controller has already verified this final report edit.
- Controller exact scope: plan manifest **41 files +existing report =42 relevant paths**. `git status --porcelain=v1 -uall` contains exactly those **42** plus private untracked **`.codex`**; **zero extra, zero missing** relevant paths. The private `.codex` entry is not a 43rd implementation artifact.
- Controller branch: **`codex/pa30-next-work-unit`**; **`origin/main` remains `b48cbb88cec5302b00e3af3be5f621f69838c20f`**. No additional child ancestry/HEAD or remote mutation claim is inferred.

This finalization retains the 41-path candidate and changes only this already-existing report; sections 2–12 are preserved. Controller verification of this report diff and freezing it for independent A/B code review remain Controller-owned next actions, not actions taken here. No source, test, config, CI, POM, OpenAPI, migration, plan/fix artifact, Control or other repository path is edited by this task.

### 13.6 Residual risk classification and hard stop

All **implementation-gate validation is green**; the candidate is **implementation-evidence complete**, not code-reviewed, approved, ready for PR, live-accepted or production-ready. Independent code review **has not yet begun**. Historical runtime blockers are closed by the final Controller evidence above, but these deferred responsibilities remain real and are not waived:

| Residual responsibility | Classification / later owner or work unit |
| --- | --- |
| Real production proof verifier and server-side evidence ownership for supported methods | Deferred production integration; Subject-number / Security and the relevant provider owner must deliver and validate it under a later confirmed work unit. Current defaults remain unavailable; controlled fakes do not prove production verification. |
| Real reference cipher, KMS/key ownership, algorithm and key lifecycle | Deferred production security integration; Subject-number / Security / Platform/KMS owners. Test-only randomized authenticated encryption does not resolve production Q2 or establish a deployable cipher/key source. |
| Production credentials, dedicated-login provisioning, deployment configuration and live acceptance | Deferred operational/live-validation work; Platform / Operations / Controller and acceptance owners. Synthetic container logins and JAR smokes do not prove production credentials, provisioning, deployment or live acceptance. None was performed. |
| Future external provider/KMS transaction and side-effect design | Separate future goal/design work for Subject-number / Security / Platform/provider owners. S1 permits only repeatably resolvable local SPIs and no network I/O; external I/O must not be added under current locks or justified by these green tests. A newly confirmed transaction design is required before real integration. |

Operational observability and replay-audit growth remain later owner considerations as already recorded in section 9. These deferred production/design obligations are not unresolved failures of the completed S1 implementation validation and confer no authority to expand this slice. No CI-hosted, production, independent code-review or later-gate result is fabricated.

**No commit, push, PR creation/update, merge, approval, mark-ready action, reviewer request, external comment, issue mutation, Control update, delegation or later-gate transition occurred.** No production credential/provisioning/provider/KMS action occurred. This task stops after report-only evidence finalization, before independent code review; only the Controller may verify/freeze the report and authorize the next gate.

## 14. Bounded code-review fix 1 — accepted Low findings

Task `pa30-code-review-bounded-fix1-gpt-20261113-01`; date 2026-11-13, Asia/Shanghai; gate remains **code review → fix** for PA30 S1. The complete accepted plan, this report, Review A `docs/reviews/code-review-20261113-024637.md` and Review B retry `docs/reviews/code-review-20261113-040003.md` were read before editing. Review A recorded three accepted Low findings; Review B recorded **PASS / zero findings** against the same 42-path candidate aggregate. This bounded fix does not enter re-review or any later gate.

Evidence-finalization task `pa30-code-review-fix1-evidence-finalize-gpt-20261113-01`; supplied runtime/provider: Codex `gpt-6.1-sol`, reasoning `high`; date 2026-11-13, Asia/Shanghai. Section status: **IMPLEMENTATION/REVIEW-FIX EVIDENCE GREEN; INDEPENDENT RE-REVIEW NOT YET OCCURRED**. The Controller-supplied post-fix results below close the runtime-validation follow-up, not the independent re-review gate. No approval, PR readiness or production readiness is claimed.

### 14.1 Exact bounded changes

- `backend/src/test/java/com/onecall/archtests/subjectnumber/CustomerSubjectClaimServiceTests.java`: existing `changedAcceptedContextAppendsExactlyOneConflict` now covers server proof reference, verification method, binding ID only and number HMAC only changes. Each case asserts context conflict, one appended CONFLICT, unchanged owner state/revision, no accepted-result disclosure, and stored-reference opening only after compatible row identity. Existing `corruptAcceptedOwnerRelationAndUpdateMissFailClosed` now covers a consistent earlier relation followed by an inconsistent later relation, unowned corruption, personal-owner corruption and missing/different organization ownership; each case asserts INTERNAL, safe integrity audit, no owner/repair/new-accepted write and no stored acceptance disclosure. The existing conditional owner-update-miss rollback branch remains.
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java`: existing `changedDuplicateContextAppendsOneConflictAndNeverUpdatesOwner` covers the same four accepted-context dimensions, one conflict row per attempt, unchanged owner/revision/time and no old-result disclosure, with stored opens only after accepted metadata compatibility. Existing `corruptAcceptedOwnerRelationsFailWithoutRepair` covers deterministic earlier/later relation ordering, personal/unowned corruption and organization missing/different corruption, asserting 500 `INTERNAL`, `CLAIM_DATA_INTEGRITY` / `DENIED` / `NO_OPEN`, unchanged owner/history and zero business writes or stored opens.
- `backend/README.md`: current PA30 truth now distinguishes historical child-sandbox NOT RUN labels from Controller-completed focused 34/34, full 838/77, JAR/firewall 116, OpenAPI s3 and frozen/scope evidence. It explicitly keeps production proof/cipher/KMS, credentials/provisioning and live acceptance deferred and makes no code-review, PR-approval or production-readiness claim.
- `docs/gateflow/pa30-customer-personal-subject-claim-foundation-s1-implementation.md`: this bounded evidence section only; no production path, plan/fix artifact, review artifact, Control, OpenAPI, docs/14, docs/15 or migration changed.

### 14.2 Validation and custody

- `/opt/maven-3.9.9/bin/mvn -B -ntp test-compile` with `/opt/jdk21`: **exit 0**.
- Direct modified unit invocation for `changedAcceptedContextAppendsExactlyOneConflict` and `corruptAcceptedOwnerRelationAndUpdateMissFailClosed`: **2/2**, zero failures/errors/skips.
- Focused Surefire invocation for the five approved classes: **51/51**, zero failures/errors/skips; fresh log `/tmp/pa30-code-review-fix1/focused-51.log`.
- Existing CI source-only extractor: **PASS**; exact source=CI arithmetic remains **54/468 + 23/370 = 77/838**, annotation **839**, POM **23**, customer **43**, migrations **17**, modules **14**. No Docker/Testcontainers/Failsafe execution was performed or claimed.
- Plan SHA-256 values remain accepted plan `71188F38486578979CFCC7A9949900C3B06F90A14AE318002F3A0281409A0191`, fix-1 `0D7C3F182D1DF383B7A75751FC029CF2297021A2E6632029C8AF5FB972500FD0`, fix-2 `9C5DE2B2FC9801EA6119039177C875A8BB2E2124B8E5607D5B0F52C44AF00524`; frozen20 raw/LF pairs remain exact. The all-838 test identities/counts remain unchanged.
- Windows `git.exe diff --check` and `status --short` were attempted using the available Windows Git executable and remain **NOT RUN / unavailable in this child** because WSL interop failed with `UtilBindVsockAnyPort:309: socket failed 1`; this is not substituted with Linux Git evidence. The private pre-edit snapshot and exact four-path write boundary are retained under `/tmp/pa30-code-review-fix1/`.

Controller follow-up completed — supplied post-review-fix evidence, not execution by this report-finalization child. The preceding bounded-fix child results remain historical; its lack of Docker/Testcontainers/Failsafe execution is not a current Controller validation blocker.

- Focused PostgreSQL16 command after review fix: `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerSubjectClaimsIT,SubjectClaimRoleLeastPrivilegeIT -DfailIfNoTests=false verify` — **exit 0**, **468 Surefire + 34 Failsafe**, zero failures/errors/skips, including the newly expanded **I14/I17** cases. Log: `D:\codex-temp\pa30-code-review-20261113-01\fix1\controller-focused-fix1.log`.
- Final clean full-container command: `/opt/maven-3.9.9/bin/mvn -B -ntp clean -Pcontainer-smoke verify` — **exit 0**, **468 Surefire /54 classes + 370 Failsafe /23 classes = 838 tests /77 classes**, zero failures/errors/skips. Log: `D:\codex-temp\pa30-code-review-20261113-01\fix1\controller-full-container-final.log`.
- Final exact inventory/JAR/firewall command — **exit 0**: **source=CI=XML 838/77**, annotation **839**, POM **23**; JAR default/web smokes and final firewall **116 passed**. Log: `D:\codex-temp\pa30-code-review-20261113-01\fix1\inventory-jar-firewall-final.log`.
- Final OpenAPI `npm run check:s3` — **exit 0**: stage-s3 **49 operations** and all doc/source uniqueness checks passed. Log: `D:\codex-temp\pa30-code-review-20261113-01\fix1\openapi-check-s3-final.log`.

Implementation/review-fix evidence is green on these supplied Controller results; no hosted CI run is claimed. Independent re-review **has not yet occurred**. Controller verification/freezing of this report and the required independent Windows Git/scope custody checks remain Controller-owned; no new successful Git custody result is inferred here. This finalization edits only this report, preserves sections 1–13 and the exact three finding resolutions in section 14.1, and reruns no tests. Stop at this bounded fix. No commit, push, PR, merge, approval, re-review request, reviewer request, mark-ready action, external mutation, delegation or later-gate transition occurred. No approval, PR readiness, live acceptance or production readiness is claimed.
