# PA9 V009 S2 implementation evidence

- Task: `pa9-v009-s2-implementation-gpt-20261031-04`; work unit `PA9-v009-sharing-persistence-foundation`; gate **implementation S2 only**; date 2026-10-31, Asia/Shanghai.
- Author status: **S2 implementation-complete, local validation passed; not reviewed or controller-accepted**.
- Provider/runtime: OpenAI coding assistant through Codex CLI, Bash/WSL Linux amd64 in the existing Windows-managed worktree. The controller specifies GPT `gpt-6.1-sol` / high routing; this session does not independently expose/attest an exact provider-side model identifier or runner completion record.
- Branch `codex/pa9-v009-sharing-foundation`; unchanged intake HEAD `a9f9ea3a59edfb6e6ede5a75a0d3e2c1e1ea56df`; accepted S1 `9b5edd68769745de12efda178bbca106f9719328` verified ancestor.
- Accepted plan `docs/gateflow/pa9-v009-sharing-foundation-plan.md`, commit `ee4db90d6d7dd52384e75ccb8e7148cdf1ea08b9`, SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`. Entire plan and active Goal Confirmation/current S2 control checkpoint read before editing; no scope deviation authorized or made.

## Scope and source identity

Only these five repository paths are task writes: the three tested sources below, this implementation record, and `docs/gateflow/pa9-v009-sharing-foundation-s2-completion-1.md`. No Git index/metadata, branch, commit, controller, migration, application, POM, test resource, design or prior record is edited. Intake untracked `.codex` and `build/` are preserved as pre-existing paths; Maven emits generated build/target output, not additional task source artifacts. No manual cleanup/edit of those intake paths is performed. Windows .git indirection is handled read-only with `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`, not repaired.

| Tested source | Raw checkout SHA256 |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `8d11f3f29e63d16d9eb7a23a014bcf4976193aac3363ab62b49eaaa9b55e2d26` |
| `.github/workflows/ci.yml` | `38dad8de228dde846d21f171c15ab81be6790ca6ee65c5c7a925df86610d9f43` |
| `backend/README.md` | `8b1e50d41a13d8e641b97df83bc2ac6635d88302cab07da6dca15c5963b266cc` |

Tested three-source diff against intake HEAD SHA256: `957d6c1e35093e7119f011721356117efcc0fce8efbdc6648c56c6af4a20c684`. This identity excludes the two post-run evidence records. Record self-hashes are intentionally external, avoiding circular embedded hashes; final independent audit/manifest under `/tmp/pa9-s2-04` contains all five path hashes.

Exactly two new @Test methods, no rename/removal/disable/skip: `v009ShapeDefaultsAndAppendOnlyBackstops`, `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops`. Independent extraction verifies all previous 60 @Test method bodies are byte-identical as decoded source; only helpers and class inventory prose accompany the additions. CI and README move to exact cumulative 62, Surefire remains 10. Neither S3 method exists.

## Preflight and actual validation

Read-only branch/status/HEAD/ancestor, accepted-plan/frozen-V009 SHA and V001–V008 canonical blob checks passed. Installed Java Temurin 21.0.13+9; Maven 3.9.9; Git 2.34.1; Docker CLI 29.1.3, Docker Desktop 4.97.0 (244584), Engine 29.9.2. PostgreSQL executable and Flyway JDBC both report **16.15**. Testcontainers 2.0.5, Flyway core 12.4.0, JDBC 42.7.14 are actual XML classpath versions. Local `postgres:16` aliases `postgres:16-alpine`, image ID `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`; this is local test provenance, not a production image pin.

All Maven commands execute from the repository root with `JAVA_HOME=/opt/jdk21`, `PATH=/opt/jdk21/bin:$PATH`, installed `/opt/maven-3.9.9/bin/mvn`, `-f backend/pom.xml -B -ntp`; no wrapper modification or skip flag. Bounds are external `timeout`, never widened. Wall elapsed values include the Maven launcher; Maven-reported times remain in logs.

| Actual command suffix (after the common Maven prefix) | Bound | Exit | Wall elapsed | Reports |
| --- | --- | --- | --- | --- |
| `test-compile` | 600 s | 0 | 10.595 s | compilation, no test execution claim |
| `verify` | 600 s | 0 | 12.940 s | 10 Surefire, 0 failures/errors/skips |
| `-Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009ShapeDefaultsAndAppendOnlyBackstops+v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops' verify`, attempt 1 | 600 s | 1 | 31.946 s | 2 Failsafe, 0 failures / 1 error / 0 skips; 10 Surefire pass |
| Same focused command, attempt 2 | 600 s | 0 | 35.173 s | 2 Failsafe + 10 Surefire, all zero failures/errors/skips |
| Same focused command, final attempt 3 after test-only restoration/coverage hardening | 600 s | 0 | 33.755 s | 2 Failsafe + 10 Surefire, all zero failures/errors/skips |
| `-Pcontainer-smoke '-Dit.test=PostgresContainerIT#v007ForeignKeyAndLateOrderConsentFkBackstops+v008EvidenceInventoryAndGuardShapesAreExact+v008OwnerPublicRuntimeAndCrossModuleAclsAreExact+v008ForeignKeyAndDeferredCreationCycleFkBackstops+v008CreationCycleBothOrdersAndTerminalLifecycleBackstops' verify` | 900 s | 0 | 24.005 s | 5 Failsafe + 10 Surefire, all zero failures/errors/skips |
| `-Pcontainer-smoke verify` (exactly one final cumulative invocation) | 1500 s | 0 | 44.201 s | **62 Failsafe + 10 Surefire, all zero failures/errors/skips** |

Actual failed focused attempt 1 is retained, not counted as a pass: the deferred V008 evidence creation-cycle trigger ran at fixture COMMIT under sharing_role, producing `42501/permission denied for table evidence_event`. The test authoring fix executes `SET CONSTRAINTS ALL IMMEDIATE` while still evidence_owner, before switching to sharing_role. No migration, privilege or assertion weakening. The shape method already passed in that failed attempt. Attempt 2 passed; final focused attempt 3 additionally checks restoration in finally, invalid/non-code status/rotation shapes, and generation deltas from both ACTIVE and LOCKED. No full run was attempted until those changes passed.

An early independent inventory audit also failed on stale README cumulative 60 references; those were corrected to 62 before the final focused/full evidence. No failed audit is presented as a pass. The final audit uses a numeric-count-specific README check rather than an indiscriminate substring search.

Additional actual local checks: `python3 /tmp/pa9-s2-04/audit.py /tmp/pa9-s2-04/full-xml` exits 0; the **actual CI heredoc** extracted without rewriting to `/tmp/pa9-s2-04/ci-validator.py` and executed from backend exits 0 (`container-smoke report assertions passed`). `timeout 60s java -jar backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar` exits 0 in 2.032 s and contains Started OneCallApplication. `jar tf` plus the unchanged production-dependency deny pattern finds no forbidden library, exit 0. Docker final listing has no remaining test container. Unchanged Node/OpenAPI/IVR/security contract-regression jobs are **not run** in this S2 local gate; no remote CI claim.

## CHECK/default/key and append-only matrix

Actual shape test emits **31 distinct exact check names / 123 isolated concrete-false 23514 probes**; the independently extracted frozen V009 name set equals emitted server-verified names. Positive and boundary shapes run under rollback savepoints, not only catalog checks. The retained S1 literal PostgreSQL deparse map remains an independent full-predicate oracle, rechecked with the catalog.

| Exact server CHECK name (SQLSTATE 23514) | Concrete-false shape probes |
| --- | ---: |
| `ck_share_grant_share_id` | 3 |
| `ck_share_grant_token_hash` | 4 |
| `ck_share_grant_content_sha256` | 4 |
| `ck_share_grant_code_hash` | 4 |
| `ck_share_grant_verification_mode` | 1 |
| `ck_share_grant_code_salt` | 3 |
| `ck_share_grant_code_generation` | 1 |
| `ck_share_grant_verification_shape` | 6 |
| `ck_share_grant_org_member` | 1 |
| `ck_share_grant_validity` | 2 |
| `ck_share_grant_admissions` | 4 |
| `ck_share_grant_snapshot_shape` | 36 |
| `ck_share_grant_snapshot_binding` | 1 |
| `ck_share_grant_mobile_binding` | 7 |
| `ck_share_grant_status` | 1 |
| `ck_share_grant_revocation_generation` | 1 |
| `ck_share_grant_revision` | 1 |
| `ck_share_admission_key` | 3 |
| `ck_share_admission_no` | 2 |
| `ck_share_admission_mode` | 1 |
| `ck_share_admission_result` | 1 |
| `ck_share_admission_result_shape` | 4 |
| `ck_share_access_event_domain` | 1 |
| `ck_share_access_event_id` | 3 |
| `ck_share_access_event_type` | 1 |
| `ck_share_access_event_result` | 1 |
| `ck_share_access_event_admission_shape` | 6 |
| `ck_share_access_event_risk_digest` | 4 |
| `ck_share_access_event_payload_sha256` | 4 |
| `ck_share_access_event_payload` | 3 |
| `ck_share_access_event_admission_facts` | 9 |

Boundary/conjunct evidence: ID/key/event-id 1 and 128 valid versus empty/whitespace/129; URL-safe salt 22/256 versus 21/257/invalid character; lower 64-hex versus uppercase/nonhex/63/65 for token/code/content/risk/payload and creation digests; all four verification modes with code/NULL/gen and organization requirements; strict validity versus equality/reversal; 0/unlimited/positive/exact max versus negative count/zero-negative max/over-max. Snapshot object/top-level presence and each object type, each evidence/creator/org binding and each creation digest are probed independently. Mobile HMAC and keyVersion boundaries/absent/null are explicit. All 15 event types/four results, six admission-scoped types, six admission-fact presence keys plus mode/member recipient pair are covered.

CHECK UNKNOWN is preserved: missing/null nested evidence/creator/digest/mode/mobile members can pass; ownerOrganizationId uses IS NOT DISTINCT FROM and must match non-NULL parent, but absent/JSON null pass when the column is SQL NULL. Required top-level object members still fail when absent; JSON null top-level objects fail type checks. ADMISSION_GRANTED JSON null values satisfy key presence without imposing typed generation/time/member semantic validation. These are structural observations, not authorized service payloads.

Nine explicit defaults are exercised by omission: grant code_generation=0, valid_from=transaction_timestamp(), admissions_used=0, download_allowed=false, status=ACTIVE, revocation_generation=0, revision=1; admission admitted_at and event occurred_at equal database transaction_timestamp(). Explicit NULL is not a default: all **32** required columns fail **23502 with exact server table/column**. All **eight** keys fail **23505 with exact server constraint**: pk_evidence_share_grant, uq_share_grant_share_id, uq_share_grant_token_hash, pk_share_admission, uq_share_admission_key, uq_share_admission_no, pk_share_access_event, uq_share_access_event_domain_id. Duplicates isolate non-target identities. No uuid generator/default is invented.

Owner no-op UPDATE and DELETE on both append-only tables produce exact `P0001/SHARING_ROW_APPEND_ONLY`; runtime UPDATE/DELETE instead produce `42501`. All 12 grant frozen columns independently produce owner `P0001/SHARE_GRANT_IMMUTABLE` and runtime `42501`.

Admission BEFORE INSERT guard can mask predicate/key probes; only that one named guard is disabled inside unconditional rollback-only owner savepoints. Invalid admission mode/result isolate result_shape; negative grant code_generation isolates verification_shape; invalid grant mode isolates snapshot_binding. No global trigger bypass or permanent CHECK removal. After rollback-to-savepoint, exact full named catalog and four enabled trigger bindings are reasserted in finally, even on unexpected results. Full shape grant/admission/event fixture row sets are compared after probes; outer rollback plus fresh registered-ID checks proves no fixture residue.

## Lifecycle/action/revision matrix

All 16 status pairs execute with the guard enabled. `S` below means a same-state no-op and revision-only +2 shape, not an owner action. `L` means legal named edge with required action deltas; `T` means exact `P0001/SHARE_GRANT_STATUS_TERMINAL`.

| OLD → NEW | ACTIVE | LOCKED | EXPIRED | REVOKED |
| --- | --- | --- | --- | --- |
| ACTIVE | S; pure future extension/count revision shapes | L lock, generation-neutral | L observed past expiry, revocation+1 | L revoke, revocation+1 |
| LOCKED | L extraction-code rotation only | S | L observed past expiry, revocation+1 | L revoke, revocation+1 |
| EXPIRED | T | T | S | T |
| REVOKED | T | T | T | S |

For each of six legal directed edges and pure extension/count shape, old+1 revision succeeds; old/old−1/old+2 produce exact `P0001/SHARE_GRANT_REVISION_INVALID`. Each action also tests a stale expected-revision conditional WHERE yielding exactly zero, one bounded reread, **explicit transaction rollback** and fresh restoration; zero rows are not a PostgreSQL exception. Count shape proves revision enforcement only: it does not implement/claim S3 admission count-once, event/count transaction, limit or rollback atomicity.

The final focused/full lifecycle output includes **109 labeled mutation probes and eight stale-WHERE probes**. A full per-probe diagnostic matrix is retained separately. Additional exact observations:

| Probe | Result |
| --- | --- |
| LOCKED NONE unlock; code unlock without rotation; unknown status edge | P0001/SHARE_GRANT_STATUS_TRANSITION |
| ACTIVE→EXPIRED and LOCKED→EXPIRED with future old expiry | P0001/SHARE_GRANT_EXPIRY_INVALID, both synthetic direct stored fixtures |
| Pure ACTIVE future extension; lock-only ACTIVE→LOCKED | One-row exact legal shape; generation/count unchanged |
| Mixed ACTIVE→LOCKED + synthetic one-hour extension | One-row structural success; **service-forbidden**, not a named combined action |
| LOCKED/EXPIRED/REVOKED same-state extension with safely future OLD expiry | P0001/SHARE_GRANT_EXTENSION_INVALID; non-ACTIVE predicate isolated |
| ACTIVE already-expired extension | P0001/SHARE_GRANT_EXTENSION_INVALID; clock predicate isolated |
| Same-state EXPIRED/REVOKED revocation_generation+1/revision+1; separately LOCKED same-state | One-row structural success; **service-forbidden**, not expiry/revoke replay or count-once proof |
| Same-state ACTIVE extraction salt+hash replacement, both generations+1, revision+1 | One-row structural success; **service-forbidden**, not unlock/CODE_ROTATED owner success |
| LOCKED→ACTIVE exact fresh salt+hash, both generations+1, revision+1 | One-row legal rotation, sole legal code-rotation action |
| Expiry shrink | P0001/SHARE_GRANT_EXPIRY_DECREASE |
| Code or revocation generation decrease to otherwise valid nonnegative value | P0001/SHARE_GRANT_GENERATION_INVALID |
| All six incomplete salt/hash/code_generation replacement subsets | P0001/SHARE_GRANT_CODE_ROTATION_INVALID |
| Rotation missing/skipped generation delta | P0001/SHARE_GRANT_CODE_ROTATION_INVALID when generation unchanged; otherwise SHARE_GRANT_GENERATION_INVALID |
| Revoke/expiry missing/skipped revocation delta from ACTIVE and LOCKED; non-neutral ACTIVE lock | P0001/SHARE_GRANT_GENERATION_INVALID |
| Non-code rotation with coherent structural deltas | 23514/ck_share_grant_verification_shape |
| Same-state revision=0 | 23514/ck_share_grant_revision, separate from mutable-action revision guard |

Terminal EXPIRED/REVOKED baselines used for same-state generation probes are separately committed fixtures established through named transitions. LOCKED baseline is established through named lock. Future-expiry terminal extension rows and past-expiry active/locked rows are separately valid direct INSERT fixtures, explicitly synthetic rather than a production creation path. Code fixtures have valid synthetic URL-safe salt and lowercase 64-hex placeholders; no KDF computation/policy.

Every lifecycle probe starts a real sharing_role transaction (owner only for owner frozen-column negatives), locks/reads full row/history, uses a savepoint and id/status/expected-revision predicate. For successes the independently evaluated specified field expressions are merged with the complete baseline JSON and compared with the entire resulting row; all other columns/history must match. Savepoint rollback compares full baseline/history/catalog/triggers, then transaction rollback; a new connection compares the complete row and both history arrays again. No successful probe mutation commits. No migration guard is disabled for lifecycle.

Method-local cleanup deletes only registered grant IDs after asserting no admission/event rows, then the account, using owner privileges but no grant-guard bypass (UPDATE-only). Existing v008S2Fixture/v008S2Cleanup performs FK-safe evidence/recording/media/reservation/call/session/subject cleanup, restores its named immutable triggers and validates constraints before commit; errors roll back rather than being swallowed. Fresh checks prove every registered grant/account absent, existing helper proves evidence parents absent, exact V009 catalog/trigger/FK and V008 FK inventories restored. This S2 fixture lifecycle is not an S3 transaction/concurrency witness.

## Exact cumulative method identity

Independent Java @Test extraction, CI Python literal AST extraction (including literal duplicate count) and actual final Failsafe testcase extraction each yield **62 names and total 62, exact set equality**. XML suite/cases contain no failure/error/skip. Ten unit/module tests are parsed separately from three Surefire XML files; they are not mixed into the Failsafe name set. Both new S3 names absent. Previous 60 test bodies unchanged.

```text
appendOnlyClaimAndForeignKeyBackstops
auditIsAppendOnlyAndRuntimeWritesAreDenied
auditReasonAndDenylistRulesAreEnforced
bindingLifecycleTerminalityAndActiveNumberUniqueness
brokenMigrationFailsVisibly
committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow
concurrentActiveBindingUniquenessHasOneWinner
concurrentCatalogPublicationAndCommerceIdentityHasOneWinner
concurrentIdempotencyRetryObservesOneWinner
concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner
concurrentV005TaskClaimAndActiveCallHasOneWinner
concurrentV006FirstTrialAndReservationIdentityHasOneWinner
concurrentV007ActivationAndTransitionIdentityHasOneWinner
concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner
functionsHaveFixedSearchPathAndNoTransactionControl
hostileSearchPathCannotRedirectFunctionEffects
idempotencyNewReplayConflictAndFinalizeOnce
identityCollisionAndMembershipRevokeRegrantSemantics
lastEnabledAdminTransactionIsSerializedAndFailsClosed
localImmutabilityGuardsRejectRelationAndRotationChanges
mutableHeadsEnforceLifecycleRevisionAndNamedChecks
outboxReplayConflictRollbackAndDenylist
ownerPublicAndRoleExecutionBoundariesAreExact
riskOpenClearReopenRevisionProtocol
runtimeAndCrossModulePrivilegesAreExactAndWritesDenied
subjectOwnerSetOnceAndRealNameSeparation
v001ThroughV003InventoryMatchesTheFrozenContract
v001ThroughV009MigrateCleanPostgres16WithoutSkipping
v003OwnerPublicAndRuntimeAclsAreExact
v003SubjectNumberCatalogAndGuardsAreExact
v004CatalogOrderInventoryAndGuardsAreExact
v004CatalogVersionLifecycleAndGuardBackstops
v004ForeignKeyAndStagedConsentFkBackstops
v004OrderPaymentLifecycleAndAppendOnlyBackstops
v004OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005ForeignKeyAndDeferredReservationFkBackstops
v005IvrTaskSessionCallInventoryAndGuardShapesAreExact
v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops
v005OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005SessionCallCanonicalCallerAndReciprocalLinkBackstops
v005TaskTtlLifecycleAndGuardBackstops
v006EntitlementReservationUsageInventoryAndGuardShapesAreExact
v006IssueFirstTrialGrantReplayConflictAndPolicySemantics
v006NestedOutboxAtomicityAndReplayRollbackEvidence
v006OwnerPublicRuntimeAndCrossModuleAclsAreExact
v006ReservationUsageForeignKeyAndLateFkBackstops
v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops
v007ActivationConsentInventoryAndGuardShapesAreExact
v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops
v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops
v007ForeignKeyAndLateOrderConsentFkBackstops
v007OwnerPublicRuntimeAndCrossModuleAclsAreExact
v008CreationCycleBothOrdersAndTerminalLifecycleBackstops
v008EvidenceInventoryAndGuardShapesAreExact
v008ForeignKeyAndDeferredCreationCycleFkBackstops
v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops
v008OwnerPublicRuntimeAndCrossModuleAclsAreExact
v009ForeignKeyAndRestrictDeleteBackstops
v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops
v009OwnerPublicRuntimeAndCrossModuleAclsAreExact
v009ShapeDefaultsAndAppendOnlyBackstops
v009SharingInventoryAndGuardShapesAreExact
```

## Frozen migration evidence

V009 raw SHA256 remains exactly `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`. For every V001–V008 path, `git show 835f2ec:path` canonical bytes equal `git show HEAD:path`; checkout LF-normalized resource bytes also equal those canonical blobs, with no migration path in the diff. These are separate Git-byte and checkout evidence, not an LF-only assertion. Frozen plan SHA unchanged.

| Canonical migration | SHA256, original base = intake HEAD |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `V002__identity_organization_and_employee.sql` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `V003__subject_number_and_claim.sql` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V005__ivr_task_session_call.sql` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `V007__activation_and_consent.sql` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `V008__evidence_media.sql` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |

Cumulative retained migration test executes real staged 004→005→006→007→008 fences, exactly one V009 application, exact nine successful history rows, validate/no-pending evidence and the broken V901 rollback test. Catalog inventory remains 3 tables/38 columns/31 CHECKs/3 PK+5 UNIQUE/13 physical indexes/6 RESTRICT FKs/9 defaults/3 local invoker functions/4 triggers; five SECURITY DEFINER services unchanged, no V010 or extra sharing service.

## Retained artifacts and hashes

Logs and copied XML are outside repository source paths at `/tmp/pa9-s2-04`; each focused attempt is copied before the next run overwrites target. XML includes original per-run provenance/classpath/stdout. Original final target reports remain under backend/target. Logs are not remote CI evidence.

| Artifact | SHA256 |
| --- | --- |
| `/tmp/pa9-s2-04/preflight.log` | `63807faf84b8b7294c06b8eeb8b46bb03b96ae61859b1a151477c461cd983c84` |
| `/tmp/pa9-s2-04/test-compile.log` | `c704d7a80c4aa9d53c99de9769c4839c133ce6799c8c068a31a33bd8092a0ced` |
| `/tmp/pa9-s2-04/default-verify.log` | `f24e65aa8ff56e1d55d91cc89015bd753015a09a7ece72e3666888f7433a8c1f` |
| `/tmp/pa9-s2-04/focused-1.log` | `170c5fbe01dd79c80d6c43a8fb1119d04c828f3cbe7a9a70e8145ed81f123be7` |
| `/tmp/pa9-s2-04/focused-2.log` | `0d4ddc3f8e51b6b09258d7b3dcd1d0dccbed7e9b637f8c2b7ff80cc906eaecad` |
| `/tmp/pa9-s2-04/focused-3.log` | `d086190d5a9ee351b40242239d6756e6f19f9e0c95f499403683c948a950ea35` |
| `/tmp/pa9-s2-04/fence-regression.log` | `cb5bcca592a064905207692f52f915a9aa2b230470bc2a310e124add06845e92` |
| `/tmp/pa9-s2-04/full.log` | `056e0691f58b75a31c4338e337cf13a1452087c68b4e965c05cdb332b2d72c16` |
| `/tmp/pa9-s2-04/boot.log` | `ecf3fad4c6c92bf1e8de87151f84e9d6bb84798f133ca71526c36b5e943625e2` |
| `/tmp/pa9-s2-04/ci-validator.log` | `f590d8167cad47859c57405688c1ea785c67a9972c31ffd9b39fe23a85481fd7` |
| `/tmp/pa9-s2-04/check-matrix.json` | `f849243e8e7c44ca41c4c4801c42336a20b6c60d3e922eb3c94f7d15b70671fe` |
| `/tmp/pa9-s2-04/lifecycle-matrix.txt` | `e8921a07f0577587e194ac951f6ec7e73df1adad3369948e7e190aca31dfc5bc` |
| `/tmp/pa9-s2-04/tested-source.diff` | `957d6c1e35093e7119f011721356117efcc0fce8efbdc6648c56c6af4a20c684` |

| Retained XML (including failed attempts) | SHA256 |
| --- | --- |
| `/tmp/pa9-s2-04/default-surefire/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `a2b2abf89ce124da4e793395cfbd5464908163162f69031c5d495c860d016af1` |
| `/tmp/pa9-s2-04/default-surefire/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `92a060c8ce5a5407779a54bb78532e6acad18a3cb8be628131974ea490b799be` |
| `/tmp/pa9-s2-04/default-surefire/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `bc0ae431346587071d925ab06560808b73925a642c7885746e6b1cf698c1bfa7` |
| `/tmp/pa9-s2-04/fence-regression-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `e56b91162ae7232f168b9f08b377ed4a05c1ed89b119b2073b1167619eb24c3e` |
| `/tmp/pa9-s2-04/fence-regression-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `b0395722966e428e8fc4b72070f8d4596cb06ffae163429398b43508311673c9` |
| `/tmp/pa9-s2-04/fence-regression-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `12fdc7cc98b4a9da95d6552ccf6d8c36cb868e1508eb8a2ff704b119a7bcd514` |
| `/tmp/pa9-s2-04/fence-regression-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `17d06fe5aa237be1c122bfaa615f123b55db3619abe2ce2584bd3d70aba4f837` |
| `/tmp/pa9-s2-04/fence-regression-xml/failsafe-summary.xml` | `a81d75e6f1b44d866827ca77b54e23e883ce942020df3ae3313b9a33a8c77c9e` |
| `/tmp/pa9-s2-04/focused-1-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `2f8c1cd784e10a7efe853aa935ef1ad9723b53224b0a44b0f1ec2d773aac2e1f` |
| `/tmp/pa9-s2-04/focused-1-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `c9b919ba9bd4da5782e65f841c80b33e3729f23b992bc4c6ff48954afb2b2cd7` |
| `/tmp/pa9-s2-04/focused-1-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `128995f70a569ee3732a60425793de78c498c905a6dfa48d2dda3fd28c9cb21a` |
| `/tmp/pa9-s2-04/focused-1-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `d7b7172507813488f1e92ebc7a7ca9c565463b8f43e04b0917e5571504caabfb` |
| `/tmp/pa9-s2-04/focused-1-xml/failsafe-summary.xml` | `0de34c1d7948f987772ffdb98e9912aaf8d67d772f766f1beb01e6c424dd5793` |
| `/tmp/pa9-s2-04/focused-2-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `62f9fffce06d8daec056f275e1ddd02e825fd540f286cb057179be0cefba072e` |
| `/tmp/pa9-s2-04/focused-2-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `1c78c90ef4947150f651570fdadc89bcbd64be3a496c7eaa3a0e4006976705bd` |
| `/tmp/pa9-s2-04/focused-2-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `685553e27c7e0abb6b891be15fd969a62f44630a288e936753b8e703991c7d23` |
| `/tmp/pa9-s2-04/focused-2-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `0aa83fe63a54c25324ed8d58839e6cf66f70a9b87b2b7e307b3139514c1ec837` |
| `/tmp/pa9-s2-04/focused-2-xml/failsafe-summary.xml` | `148c53f8f020ff6213746b781b2f0ee3f842e0f10cef43f51cb392155a44c655` |
| `/tmp/pa9-s2-04/focused-3-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `1e8ce8bf4103770255a47f6675ee29ee58877fcc01a49d9b26f9cfc6ad841b6c` |
| `/tmp/pa9-s2-04/focused-3-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `a6494603859a0192aede918e0dce7ed546a8385885e44f52b1dc52fdfc656588` |
| `/tmp/pa9-s2-04/focused-3-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `ecc9410dbe851f058d19491a061abe74d85bcb4d0588b2bf1ba18dd3b5f97d61` |
| `/tmp/pa9-s2-04/focused-3-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `c844767d8e078f2ab022a5fa1fd3c3045dd4398111e060eb3bdaece0ea02d0a8` |
| `/tmp/pa9-s2-04/focused-3-xml/failsafe-summary.xml` | `148c53f8f020ff6213746b781b2f0ee3f842e0f10cef43f51cb392155a44c655` |
| `/tmp/pa9-s2-04/full-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `816072c81893d2b8f593e783d8341dcb81ba77310347910b9295e9ea297c324d` |
| `/tmp/pa9-s2-04/full-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `9ecbf2ed89431d9008dbb460b0ad2332d0f6193a45bf59cf9ca497692853b5b7` |
| `/tmp/pa9-s2-04/full-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `86ac82d9b5d650a549d354f0f1cd8a7a915084cea4927e7950113093e8d7b0f5` |
| `/tmp/pa9-s2-04/full-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `1ce2de0e3022d93396b4b81feb137e49c53d0782ce3899fdac6e3a4a4c57f116` |
| `/tmp/pa9-s2-04/full-xml/failsafe-summary.xml` | `56ae60aa03aa06ff1b29093ae0853aa36186c093f1c5dd79158af7006a0b220d` |

## Final boundary and residual risks

Final scope/whitespace/newline/hash/inventory audit is run again after writing both records, with outcome in the completion record and external manifest. No unresolved local blocker or frozen-design contradiction remains. These results are author evidence only: independent review and controller acceptance remain later gates. Sharing/Backend/Security owns typed JSON validation, coherent grant/admission binding, expected-revision/action/replay discipline, service-forbidden structural shapes and all S3 count-once/limit/rollback/concurrency work. Security/Product owns unapproved KDF/TTL/limit policy; Platform/Database owns production provisioning/image pinning. No service/API/worker/Redis/COS/KMS integration, production policy or V010 is implemented. Remote CI/PR/review/production state is unproven and unclaimed.

No staging, commit, branch/remote mutation, push, PR, issue, review, approval or external comment occurred. Stop at S2 author completion.

## Final post-record audit

Actual post-write `python3 /tmp/pa9-s2-04/audit.py /tmp/pa9-s2-04/full-xml` exits 0: exact 62/62/62 source/CI/XML equality, 10 Surefire, 31 checks, 32 NOT NULL probes, eight key names, previous 60 method bodies identical, accepted ancestor/HEAD/plan/frozen SQL checks, only the five allowed task paths, no staged delta, and all five files pass newline/trailing-whitespace scans. Ordinary `git diff --check` exits 0. Both new-record `git diff --no-index --check /dev/null <path>` commands exit 1 solely for new-file differences, with no whitespace diagnostics. Pre-existing untracked .codex/build remain separately identified; no remaining test container. Final record bytes/hashes are included in the external manifest after this audit note, not self-embedded.
