# PA9 V009 S1 implementation — resumed implementation complete

- Current task: `pa9-v009-s1-implementation-gpt-20261031-02`; work unit: `PA9-v009-sharing-persistence-foundation`; gate: implementation **S1 only**.
- Date: October 31, 2026, Asia/Shanghai. Author status: **S1 IMPLEMENTED / LOCALLY VERIFIED**, subject to controller acceptance; not approval, remote CI success or production readiness.
- Accepted plan: `docs/gateflow/pa9-v009-sharing-foundation-plan.md`, SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`, accepted commit `ee4db90d6d7dd52384e75ccb8e7148cdf1ea08b9`.
- Observed intake/final HEAD: `de74c57617a6aa377523e0717d6511457d5682cf` (committed historical environment-blocked attempt), whose parent is the accepted-plan commit. No Git metadata repair, staging, commit, branch, remote mutation, push, PR, approval, review request or external comment.
- Intake `git status --short` contains only pre-existing untracked `.codex`; no controller dirt exists at this resumed HEAD. `.codex` and controller content are untouched. The earlier record's dirty-controller observation remains historical, not a claim about this resumed intake.
- Read-only Git uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7 GIT_WORK_TREE=$PWD GIT_OPTIONAL_LOCKS=0`; the Windows worktree pointer is not rewritten.

## S1 code-review fix 1 — October 31, 2026

Task `pa9-v009-s1-codefix-gpt-20261031-01` applies only controller-accepted A P1, B OBS-3 and the two B OBS-4 inventory javadocs: README current total 60 with the three PA9 S1 additions explicit; `assertV001ThroughV009MigrationDigestsUnchanged` at both existing inventory call sites, preserving V001–V008 pins and the classpath LF-normalization path while pinning V009 to `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`; accurate final V009/no-V010 live-fence comments. No migration bytes or test methods change. B OBS-2/OBS-5 are rejected non-actions; OBS-1/OBS-6–8 remain residual only.

The implementation scope, runtime results and tested source hashes below describe the pre-fix implementation dispatch, not execution of this code fix. Maven/Docker are not run for the fix; controller owns focused/full reruns, fresh A/B re-review and acceptance. Current changed paths, updated hashes and read-only checks are recorded in `docs/gateflow/pa9-v009-sharing-foundation-s1-code-fix-1.md`. No S2/S3 work is authorized or performed.

## Current implementation and exact scope

Exactly these six allowed paths are the task delta:

- `backend/src/main/resources/db/migration/V009__sharing.sql`.
- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`.
- `.github/workflows/ci.yml`.
- `backend/README.md`.
- `docs/gateflow/pa9-v009-sharing-foundation-s1-implementation.md`.
- `docs/gateflow/pa9-v009-sharing-foundation-s1-completion-1.md`.

The two record paths were already committed; this update retains their entire earlier blocked record below. The blocker is resolved by the authorized Docker-capable environment, not erased. Accepted plan/design, V001–V008, POM, controller, test resources and application sources remain unchanged.

V009 is one complete atomic Flyway migration: sharing_owner/sharing_role before any grant, owned sharing schema, grant→admission→event tables, 38 ordered columns (32 NOT NULL / 6 nullable), 31 named literal checks (17/5/9), 3 PK + 5 UNIQUE, all 13 physical indexes (5/4/4; five named secondary btree indexes), six immediate validated nondeferrable MATCH SIMPLE RESTRICT/RESTRICT FKs, nine defaults, three zero-argument owner-local PL/pgSQL SECURITY INVOKER trigger functions with fixed `sharing, pg_temp` search_path, and four enabled ordinary row triggers. Foreign FKs use migration-identity ALTERs following the V008 pattern; ownership remains sharing_owner. No extra table, sequence, identity/generated field, SQL service or V010 object.

Grant guard freezes the 12 named fields, applies literal OLD/NEW expiry/generation/rotation/revision/status rules, terminality and LOCKED extraction-code-only unlock. Same-state terminal generation increases and ACTIVE code rotation remain structurally possible as frozen; mixed otherwise-valid lock+extension is not given a new DDL ban. Admission re-reads the qualified parent without row/advisory lock and checks actual clock/window, status, mode, limit and next number. Append-only helper always raises. No service, policy constant, count/event write or transaction control is added. These are implemented structural controls; their exhaustive behavior remains S2/S3 scope.

ACL delivery/evidence: PUBLIC zero (including default function ACL expansion); owner-local rights; six owned table SELECT/INSERT entries, exactly eight column UPDATEs, exactly 34 cross-module column SELECTs on eight tables, seven schema USAGEs and no CREATE, exactly five effective executable signatures (three trigger helpers plus only outbox/audit services), no membership/grant option, no foreign write/row lock, no evidence_role/S1/S2 sharing expansion. Evidence's seven signatures and five global SECURITY DEFINER services are unchanged. Positive/negative probes run under real SET LOCAL ROLE; foreign projections have synthetic known rows, not empty-table masking.

## Migration fence and catalog evidence

The chain remains staged 004→005→006→007→explicit 008. Both genuine no-sharing helpers execute against V008 before unrestricted V009, with V008 catalog/functions/triggers/PUBLIC+owned ACL evidence and evidence_owner/evidence_role-only evidence USAGE. Staged V008 applies one migration; unrestricted V009 applies one; exact nine ordered SQL history rows/resources, validate(), empty pending set and repeated zero migration pass. Existing earlier staged orphan-consent and late-FK witnesses remain.

Three final V006/V007/V008 inventory call sites invoke assertV009LiveFence. An independent source audit compares its entire initial executed body to assertNoV008PlusObjects: identical non-sharing assertions; only sharing schema/role/table absence flips to presence, followed by exact global schema/role/function/trigger inventories, exact sharing tables/indexes/types/functions/triggers and nine resources/history rows. Retains IVR six-table, entitlement seven-table, activation three-table, evidence five-table/two-role inventories; five named global services; no future service names or legacy IVR names; exact consent FK and all misplaced sharing-name exclusions. assertV006PresenceAndLateFk has no sharing-specific change; the risk helper changes only its direct sharing absence to exact sharing ownership.

The literal V009_CHECK_DEF_MAP contains all 31 PG16 deparse strings from rollback-only temporary reference relations authored from accepted-plan predicates and column types, **before** the migration was executed. `reference.sql` SHA256 `81fc346296e31759332dd2da72698f64eeaa2860731d83182b1988f49e2339b0`; `check-oracle.tsv` SHA256 `8383ef01003c8fd67f1b8d1b7f7fe8cf3803ef7a1787745199b67742b9fe8dce`. Reference output was inspected for casts, ANY/ALL arrays, BETWEEN expansion, IS DISTINCT FROM and original JSON UNKNOWN semantics; no predicate is derived from actual V009 to assert itself. Exact column type/null/default/identity/generated details, CHECK bodies, PK/UNIQUE definitions, FK parent PK/actions, and full named pg_get_indexdef plus ordered key/DESC/uniqueness/validity/ownership/constraint links pass. Only occurred_at in ix_share_access_event_grant is DESC.

## S1 SQLSTATE and restoration evidence

| Frozen FK | Missing non-NULL parent | Parent DELETE | Parent key UPDATE |
| --- | --- | --- | --- |
| fk_share_grant_evidence | 23503 + exact server constraint | 23503 + same exact name | 23503 + same exact name |
| fk_share_grant_creator_account | 23503 + exact server constraint | 23503 + same exact name | 23503 + same exact name |
| fk_share_grant_owner_organization | 23503 + exact server constraint | 23503 + same exact name | 23503 + same exact name |
| fk_share_admission_grant | 23503 under savepoint-only admission-guard disable; normal enabled guard first raises P0001/SHARE_ADMISSION_REFUSED | 23503 + exact server constraint | 23503 + exact server constraint |
| fk_share_access_event_grant | 23503 + exact server constraint | 23503 + same exact name | 23503 + same exact name |
| fk_share_access_event_admission | 23503 + exact server constraint | 23503 + same exact name | 23503 + same exact name |

Valid parents and NULL organization/admission references succeed. Eighteen FK failures inspect PSQLException server SQLSTATE **and exact constraint name**, not merely message substring. Parent mutations isolate only the masking named owner trigger in rollback-only savepoints; the evidence parent additionally defers only the two named existing V008 cycle FKs inside that savepoint to prevent their earlier failure masking sharing RESTRICT. Each probe rolls back unconditionally and asserts trigger O; catalog flags remain unchanged. No DISABLE TRIGGER ALL, session_replication_role, permanent check/FK drop, committed fixture or broad cleanup.

ACL denials give 42501 for undeclared foreign columns and SELECT *, every foreign INSERT/UPDATE/DELETE, FOR UPDATE and FOR SHARE, direct platform/audit writes, owned immutable-column UPDATEs, DELETE/TRUNCATE, schema CREATE and unapproved privileges. Sharing grant FOR UPDATE and real owned INSERT/allowed UPDATE succeed. Every registered synthetic fixture ID is absent on a fresh connection after whole-transaction rollback, with exact sharing catalog/trigger controls and V008 FK/deferred flags restored. No leaked disabled trigger or row.

31-check concrete-false/boundary/UNKNOWN, runtime default behavior, PK/UNIQUE behavior, lifecycle/append-only transition matrix and atomic admission/event/race evidence are **not S1 execution claims**; those remain S2/S3. No S2/S3 @Test method is added.

## Actual commands, failures and successful validation

All Maven invocations use `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH`, Java Temurin `21.0.13+9-LTS`, Maven `3.9.9`. The installed Maven executable is `/opt/maven-3.9.9/bin/mvn` (abbreviated MVN below). No skip flags were used. Commands with `-f backend/pom.xml` ran at repository root; default verify and focused run 1 ran from backend without `-f`.

| Actual command / retained log under `/tmp/pa9-v009-s1-resume/` | Exit / result |
| --- | --- |
| `sha256sum docs/gateflow/pa9-v009-sharing-foundation-plan.md`; explicit-metadata `git rev-parse HEAD`, `git log -3 --oneline`, `git merge-base --is-ancestor ee4db90d6d7dd52384e75ccb8e7148cdf1ea08b9 HEAD` | 0; accepted digest exact; HEAD is blocked-record successor, accepted plan is ancestor |
| `docker version`; `docker run -d --rm --name pa9-v009-reference -e POSTGRES_HOST_AUTH_METHOD=trust postgres:16-alpine`; `docker exec ... pg_isready -U postgres` | 0; Docker client 29.1.3, server 29.9.2, Desktop 4.97.0; disposable test database ready |
| `docker exec -i pa9-v009-reference psql -X -v ON_ERROR_STOP=1 -U postgres -Atq < /tmp/pa9-v009-s1-resume/reference.sql` | 0; 31 independent literal-predicate definitions, temporary reference tables, transaction ROLLBACK; `check-oracle.tsv` |
| `docker exec pa9-v009-reference psql -U postgres -Atc 'select version()'` | 0; PostgreSQL 16.15, Alpine x86_64 |
| V001–V009 streamed in individual BEGIN/COMMIT transactions to reference `psql -v ON_ERROR_STOP=1` | 0; `ddl.log`; all final DDL installs without prior migration changes |
| `timeout 600s MVN -B -ntp -f backend/pom.xml test-compile` | 0; 10.307 s, `compile.log` |
| `timeout 600s MVN -B -ntp verify` (backend) | 0; 11.134 s; 10 Surefire, zero failures/errors/skips; `default-verify.log` |
| `timeout 600s MVN -B -ntp [-f backend/pom.xml] -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v001ThroughV009MigrateCleanPostgres16WithoutSkipping+v009SharingInventoryAndGuardShapesAreExact+v009OwnerPublicRuntimeAndCrossModuleAclsAreExact+v009ForeignKeyAndRestrictDeleteBackstops' verify` — run 1 | 1; 44.528 s, 4 tests / 3 errors, `focused-s1-1.log`; fixture membership used invalid OWNER and new catalog concatenations needed internal-char casts; migration installed successfully |
| Same four-method command — run 2 | 1; 28.554 s, 4 tests / 2 assertion failures, `focused-s1-2.log`; non-CHECK `connoinherit` must not be mistaken for CHECK NO INHERIT; V008 evidence-cycle FK needed named savepoint-only deferral to expose sharing RESTRICT first |
| Same four-method command — run 3 | 0; 30.019 s; 4 Failsafe + 10 Surefire, all zero failures/errors/skips; `focused-s1-3.log`, `focused-3-reports/`; only test authoring fixes between focused runs, V009 unchanged |
| `timeout 900s MVN -B -ntp -f backend/pom.xml -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v007ForeignKeyAndLateOrderConsentFkBackstops+v008EvidenceInventoryAndGuardShapesAreExact+v008OwnerPublicRuntimeAndCrossModuleAclsAreExact+v008ForeignKeyAndDeferredCreationCycleFkBackstops+v008CreationCycleBothOrdersAndTerminalLifecycleBackstops' verify` | 0; 24.475 s; 5 Failsafe + 10 Surefire, all zero failures/errors/skips; `cumulative-fences.log`, `cumulative-fence-reports/` |
| `timeout 1500s MVN -B -ntp -f backend/pom.xml -Pcontainer-smoke verify` — exactly one full S1 invocation | 0; 36.559 s, container suite 24.31 s; 60 Failsafe + 10 Surefire, all zero failures/errors/skips; `full-s1.log`, `full-reports/` |
| `python3 /tmp/pa9-v009-s1-resume/inventory-audit.py` | 0; exact 60 source=CI methods, no duplicates or S2/S3 additions; all three final call sites; full staged-vs-live fence text equivalence except only sharing absence→presence; `inventory-audit.log` |
| `(cd backend && python3 /tmp/pa9-v009-s1-resume/ci-report-validator.py)` — script extracted verbatim/dedented from edited CI | 0; exact source=CI=XML 60, 10 Surefire, no duplicates/failures/errors/skips; `ci-report-validation.log` |
| `timeout 60s /opt/jdk21/bin/java -jar backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`; `jar tf ...` + CI forbidden-library expression | 0 / 0; application started and exited; runtime jar firewall passes; `boot.log`, `jar-index.txt` |
| Supplemental reference catalog totals query | First read-only query exit 1 (internal-char concatenation), corrected `contype::text` query exit 0: 20/11/7 columns and c=31,f=6,p=3,u=5; `reference-catalog-totals.txt` |
| `docker stop pa9-v009-reference` | 0; disposable reference container removed; Testcontainers/Ryuk containers also gone |

Environment adaptations, not repository changes: a first Maven version probe omitted JAVA_HOME and failed with ClassNotFoundException (exit 1); with the pinned JDK it succeeds. Direct `./backend/mvnw -v` fails 126 because the protected wrapper checkout has CRLF `/bin/sh^M`; no wrapper/POM edit. Installed pinned Maven executes the same build. `docker pull postgres:16` downloaded layers but stalled and was terminated by targeted `pkill` (terminated pull, not a successful pull). `docker tag postgres:16-alpine postgres:16` (exit 0) makes the existing test source's `postgres:16` resolve locally to the controller-verified PG16.15 alpine image, without source/POM/test-resource changes. `container-image.txt` records image `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`. This local alias is not a production or CI digest pin. Maven root invocations generated only the incidental untracked `build/generated-spring-modulith/javadoc.json`; that author-generated file and empty parent directories were removed after validation, leaving only authorized task paths. Maven target outputs remain generated build evidence, not source artifacts.

## Source, logs and XML identity

Final full run source hashes (unchanged by subsequent documentation-only record updates):

- `backend/src/main/resources/db/migration/V009__sharing.sql`: `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`.
- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`: `b8fa61fd3f3b0eb180ebc52e6a4e67aa392854231a9bf16c2e62d9925bc1f55b`.
- `.github/workflows/ci.yml`: `6603d01f8d9f024fba378173194b172d6e482e261f63db61552690e2ca5800a0`.
- `backend/README.md`: `47f77226ebbb3f6be1fe991428391e81b02156d3e57c03103c8d6b485bd5ad3c`.

V009 S1 candidate bytes are frozen for controller acceptance at `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`; no S2/S3 migration revision is authorized. V001–V008 canonical Git blob hashes from accepted commit match all eight plan §7.2 pins (`canonical-migration-hashes.txt`); `git diff --exit-code` against that commit is 0 for all eight files and protected plan/design/POM/test resources. After code-review fix 1, runtime digest helper `assertV001ThroughV009MigrationDigestsUnchanged` pins V001–V009 and normalizes checkout CRLF only in memory. It is invoked from both retained V008 and new V009 inventory tests; no migration bytes are rewritten. Execution of the updated helper awaits controller reruns.

Retained evidence hashes (all under `/tmp/pa9-v009-s1-resume/`, temporary local retention, not a committed CI archive):

- `focused-s1-3.log`: `379c98fc611c582a4d2a02da970f1024a126b827dc7e72dbeea99a6796fa340e`.
- `focused-3-reports/TEST-com.onecall.archtests.container.PostgresContainerIT.xml`: `8908f92af43f78acd05ba27ea2750a00b26bcda49850fc342467e332f4fe8a55`.
- `cumulative-fences.log`: `42cfe7a90efe926febcb3c0bfc56726a44259f6ceec045bbf2eafd33ebd2a412`.
- `cumulative-fence-reports/TEST-com.onecall.archtests.container.PostgresContainerIT.xml`: `d65b132eb99f7aaa938767eb018ba2419583778fffe30fcc74187c6a3aad64d4`.
- `full-s1.log`: `d4f3769f8d4106c3644bdd4dcf450f1d2bc6e8454df018b7dad272f9d16acb4f`.
- `full-reports/failsafe/TEST-com.onecall.archtests.container.PostgresContainerIT.xml`: `181be5cf3f7773627edb231ee4b1fbe0222cb8bc2d04a9a8913077047f7611a0`.
- `full-reports/surefire/TEST-com.onecall.archtests.OneCallApplicationTests.xml`: `2adb9e2fa0a463d360935b542a3359b32ecfd2e70e732ce5eb25578a1cfa611c`.
- `full-reports/surefire/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml`: `93484468eb6bb2f00c7895e0afb6e5b49baabe5c6ce6aa485ef522d9014152b8`.
- `full-reports/surefire/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml`: `2746def43ac6d6b8af61d73aca2be867137103ea98949db070809b9194d41ad1`.
- `expected-methods.json`: `4e87a37bdff69787da1e9a38a073a2202f8000d3cfa998ac1993690ce6260b19`.

Renamed only `v001ThroughV008MigrateCleanPostgres16WithoutSkipping` → `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`; added exactly the three S1 methods. The full exact source=CI=Failsafe XML set is 60 (one testcase per distinct name), with all previous other 56 names retained:

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
v009OwnerPublicRuntimeAndCrossModuleAclsAreExact
v009SharingInventoryAndGuardShapesAreExact
```

## Residual ownership and stop

- Backend/Database/QA/controller: review this S1 patch and decide acceptance; later slices require their own authorization. No frozen contradiction or unresolved local runtime blocker remains.
- S2/S3 owners: exhaustive shape/default/lifecycle/append-only and caller-atomicity/count-once/bounded-race witnesses remain not implemented. Database structural acceptance is not owner-service authorization; JSON UNKNOWN/presence-only limitations are retained, not strengthened.
- Platform/CI: wrapper CRLF and local image alias are environment facts, not changes to the frozen test/toolchain contract. Remote CI, independent review and contract-regression job were not run by this author; local Maven/boot/firewall is not a claim of remote CI green.
- Security/Product/Sharing: production KDF/code, expiry/limits, risk and ticket policy/service/provisioning remain outside this persistence gate; no production values guessed.

Final scoped whitespace/path/protected-byte checks are recorded below after artifact writing. Stop at S1 author completion; no S2/S3 or Git/remote action.

---

## Final resumed validation

- Final `git diff --check` scoped to all six allowed paths: exit 0; only configured LF→CRLF informational warnings, no whitespace errors. New-file `git diff --no-index --check /dev/null backend/src/main/resources/db/migration/V009__sharing.sql` returns 1 (new-file difference) with no whitespace diagnostics; repeating with read-only `-c core.autocrlf=false` also returns 1 and empty diagnostic output. This is not described as an exit-0 command. An independent six-file trailing-whitespace/final-newline scan passes.
- Protected V001–V008/accepted-plan/design/POM/test-resource diff against the accepted commit: exit 0; controller diff against resumed HEAD: exit 0. Canonical old migration blob hashes match every plan pin. Final tested source hash verification: all four migration/test/CI/README entries OK; no source changed after the passing full run.
- Scope: five modified tracked allowed files plus the new allowed V009 migration; pre-existing `.codex` remains excluded and untouched. No extra task artifact or generated root build directory remains. Both historical attempt records were programmatically compared to their HEAD versions and remain verbatim suffixes.
- Source=CI=60 and complete staged/live-fence equivalence audit rerun: exit 0. The exact extracted CI validator executed on the full XML: exit 0, 60 Failsafe and 10 Surefire, no duplicate names/failures/errors/skips. No containers remain from this task.
- Tested tracked source diff (`full-tested-source.diff`, test/CI/README only) SHA256: `1a773c0b4bbb53b50432d01521c4bfcc93cc6da1792218bbff0314d7e1c27285`; new V009 digest is listed separately above. These identify the tested uncommitted source delta without staging or manufacturing a commit. Record-only updates follow test execution.

## Historical attempt 1 — retained verbatim

The following is the committed record from task `pa9-v009-s1-implementation-gpt-20261031-01`. Its blocker and NOT RUN statements describe that earlier attempt only; they are superseded for current status by the actual resumed results above, not deleted or reclassified as a pass.

# PA9 V009 S1 implementation attempt — blocked

- Task: `pa9-v009-s1-implementation-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`; gate: implementation S1 only.
- Date: 2026-10-31, Asia/Shanghai.
- Accepted plan: `docs/gateflow/pa9-v009-sharing-foundation-plan.md`.
- Verified SHA256: `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`.
- Accepted commit and observed HEAD: `ee4db90d6d7dd52384e75ccb8e7148cdf1ea08b9`.
- Status: **BLOCKED / NOT IMPLEMENTED**, not S1 completion or acceptance.

## Exact blocking prerequisite

Plan §6 requires a Docker-capable PostgreSQL 16 environment for S1 and identifies missing runtime evidence as a stop condition. Plan §7.3 requires generating the independent CHECK deparse oracle using literal plan predicates on PostgreSQL 16, freezing the inspected results as Java literals, and explicitly disallows guessing PostgreSQL deparse strings. That authoring prerequisite cannot currently be met:

- `docker info` returned exit 1: `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`.
- The client output identifies Docker 29.1.3; no server or PostgreSQL version was obtained.
- `command -v psql postgres initdb pg_ctl` found none; the ordinary local PostgreSQL installation paths were absent.
- This session cannot request elevated permissions. No socket permissions, daemon configuration, repository metadata, dependencies, or external infrastructure were changed.

This is an environment blocker, not a discovered contradiction in the frozen DDL. No weaker oracle, guessed catalog definitions, partial migration, fabricated runtime evidence, or self-approved contract change is substituted. Resume S1 in a permitted Docker/PostgreSQL 16 environment.

## Scope and preserved baseline

Only this record and `docs/gateflow/pa9-v009-sharing-foundation-s1-completion-1.md` are created. The other four allowed implementation paths are unchanged; V009 is not created. No methods are added or renamed, and CI/README retain their existing V008 inventory rather than claiming unimplemented S1=60. Source/CI/Failsafe XML equality at 60, nine-row migration history, live fences, catalog/ACL/FK assertions and SQLSTATE witnesses are **NOT RUN / NOT IMPLEMENTED**. S2/S3 are not entered.

Intake already contained a modified `docs/gateflow/project-execution-control.md` and untracked `.codex`; neither is edited. The Windows Git pointer is unresolved by default; read-only Git inspection uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`. No staging, commit, push, PR, review request, or external comment occurs.

## Actual validation

- Accepted-plan `sha256sum`: exit 0, exact supplied digest.
- Explicit-metadata `git rev-parse HEAD` and `git status --short`: exit 0; accepted commit and intake dirt confirmed.
- Scoped `git diff --check` over all six allowed paths: exit 0 before records were written; record checks are described in the completion record.
- `git show ee4db90d6d7dd52384e75ccb8e7148cdf1ea08b9:<migration-path> | sha256sum` for each V001–V008: all eight digests match plan §7.2; retained at `/tmp/pa9-v009-s1-20261031/canonical-migration-hashes.txt`.
- `git diff --exit-code ee4db90d6d7dd52384e75ccb8e7148cdf1ea08b9 -- backend/src/main/resources/db/migration`: exit 0; old migration paths have no worktree delta. No V009 digest exists.
- `/opt/jdk21/bin/java -version`: exit 0, Temurin `21.0.13+9-LTS`.
- `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH /opt/maven-3.9.9/bin/mvn -version`: exit 0, Maven 3.9.9 / Java 21.0.13.
- `docker info > /tmp/pa9-docker-info.log 2>&1`: exit 1, daemon access denied. This is not a skipped or passing container run.
- Default Maven verify, focused S1 verify, full container-smoke verify, CI execution and database probes: **NOT RUN** after the prerequisite stop; no Surefire/Failsafe XML or passing totals claimed.

Some supplementary file-hash reads returned `Cannot allocate memory`, including `.codex` and the large test source; no successful private-file checksum is claimed. Subsequent memory inspection reported 57 MiB free RAM and exhausted 2048 MiB swap. The protected files were not written.

## Residual ownership

Controller/Infrastructure must provide authorized Docker access and PostgreSQL 16 execution before this S1 attempt can resume. Backend/Database/QA must then generate the independent oracle, implement all six authorized paths, and execute the focused and bounded cumulative validation. This record does not assert the requested 3-table/38-column/31-check schema, any ACL or guard correctness, implementation completion, review approval, or production readiness.
