# PA40A S1 — Implementation Fix 1

- Task label: `pa40a-customer-catalog-offer-read-acl-s1-fix1-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Accepted HEAD: `0064c86d3df704aeb02b9084143049de089606a6`, verified read-only with `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10` and this checkout as `GIT_WORK_TREE`. The inherited Windows `.git` pointer is not edited.
- Gate: bounded PA40A S1 implementation fix 1 only. **Fix authoring complete; Controller Windows/PostgreSQL16 rerun and full gates pending. No runtime acceptance or code review is claimed or entered.**
- Binding inputs read before edits: cumulative accepted plan and fixes 1/2, Control PA40A checkpoint, implementation report, current migration/PA25/tests, and both focused Controller Failsafe reports.

## 1. Controller Failure Evidence And Root Causes

Controller ran, from backend:

```powershell
mvn -B -ntp -Pcontainer-smoke "-Dit.test=CustomerCatalogOfferReadPrivilegesIT" failsafe:integration-test failsafe:verify
```

The retained XML reports **16 tests, 2 failures, 2 errors, 0 skipped, 1009.105 seconds**; the text report rounds elapsed time to 1009 seconds. These are failed Controller results, not successful author validation.

| Retained evidence | Raw SHA256 |
| --- | --- |
| `backend/target/failsafe-reports/com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT.txt` | `14cf208dd6b2e9e9a5f5b47727dcd24df913202735d757f0917e77772db9176a` |
| `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT.xml` | `fb543d22d63e3559226cfa4c8a316fa25e11f902b09567fda09a395e10cb0ce6` |

- Failure: `catalogRoleAndOperationsIdentityRetainExactCapabilitiesAndPool` expected 42 but observed 99. Its query added `catalog_order_role` to the all-schema nine-role ACL query, then subtracted the nine-role set. That leaves all 99 direct catalog-role tuples, not just the frozen 42 in `catalog_order`. Cross-schema grants are valid capabilities and must not be removed.
- Failure: `v019RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically` received `V019_HISTORY_REJECTED` before the intended ACL rejection.
- Errors: `v019ReplaysCleanAndCompleteStatesWithoutFurtherChanges` and `migrationReplayAndRejectedStatesPreserveAllBusinessAndInfrastructureRows` received `V019_HISTORY_REJECTED`.
- History root cause: the optional installed-rank19 validation incorrectly required `V019__evidence_role_ivr_association_read_privileges.sql`. It now requires exactly `V019__customer_catalog_offer_read_privileges.sql`. The V018 entry, all 18 historical basenames/checksums and every other history predicate are unchanged.

## 2. Exact Fix Write Set

Eight implementation paths change within the existing accepted 52-path implementation surface; the two explicitly authorized reports bring this fix to ten paths. No additional implementation path is introduced.

1. `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql` — the single history19 basename correction only.
2. `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOfferReadPrivilegesIT.java` — independent schema-scoped catalog matrix query and explicit complete42 expected-set comparisons before/after V019; one preexisting whitespace-only line cleaned for the full-file whitespace check; no test identity changes.
3. `backend/ops/postgresql/pa25-runtime-identities.sql` — the exact V019 checksum literal only.
4. `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java` — current full320 LF manifest pin only.
5. `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java` — current PA25 raw/LF pins only.
6. `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java` — current PA25 raw/LF pins only.
7. `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` — current PA25/V019 pairs and C27 aggregate pins only; current table/bare representations retain exact27 equality.
8. `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` — the same mechanically necessary current pairs/aggregate updates only.
9. `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation.md` — current hash/CRC/manifest/C27 values and explicit failed-Controller/fix status clarification.
10. `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-1.md` — this new fix artifact.

Global searches of the repository, including hidden files but excluding generated build/dependency outputs and Git metadata, located no additional current-pin consumers. No current checker expectation is replaced with a live-derived expected hash. Existing generated table/bare views still derive from concrete pinned pairs and are checked for exact equality. Original CRLF spans remain byte-preserved.

## 3. Corrected Catalog Matrix

The new private `catalogMatrix(Connection)` query independently enumerates relation, column, routine and schema ACLs strictly inside `catalog_order`, explodes the stored ACLs, and selects only `catalog_order_role`. It retains object kind/name, column, privilege, grantor and grantability. It does not use the nine-role `acls()` helper or subtract unrelated grantees. This is equivalent to the existing catalog product/draft least-privilege matrix queries.

The complete literal expected set has exactly **42** tuples, all granted by `catalog_order_owner`, non-grantable:

- Seven tables × `(SELECT, INSERT)` = 14: `catalog_product`, `catalog_version`, `catalog_offer`, `catalog_benefit_line`, `customer_order`, `payment_order`, `payment_event`.
- Explicit column `UPDATE` = 22: `catalog_version`12, `catalog_offer`3, `customer_order`4, `payment_order`3, with the same exact columns as the preexisting frozen matrices.
- Four guard routines × `EXECUTE` = 4: `guard_catalog_version_published`, `guard_catalog_offer`, `guard_customer_order_snapshot`, `guard_payment_order_identity`.
- `catalog_order` schema `USAGE` = 1.

The predecessor set must equal that complete expected set and have size42; the post-V019 set must equal both the expected set and the predecessor and have size42. No catalog capability or migration grant is removed to satisfy a count. Existing separate cross-schema effective privilege checks are unchanged, as are seven Operations SET edges, 14 Operations ACL entries, 106 IVR ACL entries, and the Operations pool proof. No existing guard is weakened, skipped or deleted.

## 4. Recomputed Current Pins

| Value | Measured result |
| --- | --- |
| V019 raw SHA256 | `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` |
| V019 canonical LF SHA256 | `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` |
| V019 signed Flyway line-based CRC32 | `-1794072022` |
| PA25 raw SHA256 | `6881473933551ff8b4918efcb24351d97827674b4bd193f66d4542e70cd948aa` |
| PA25 canonical LF SHA256 | `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` |
| Current C27 aggregate | `fea4453e24c987221253f659e65a7ed7e270a74a2b81fe087b2b2c895e0c0712` |
| Current full320 LF manifest | `c290aac2bc7662d978e5a516e23d706a1b5195f8601055c3e8a917c21a4065e7` |

CRC32 consumes the UTF-8 bytes of each logical line without line terminators and is rendered as a signed32-bit integer; V019 has no BOM. The existing new boundary test independently compares Java CRC32 to the concrete PA25 history19 checksum. C27 sorts paths by unsigned UTF-8 bytes and hashes each path, NUL, decoded raw32 bytes, decoded LF32 bytes, with no row terminator. The manifest sorts backend-relative `path LF_SHA256` rows, appends one LF per row, then hashes the UTF-8 serialization.

## 5. Historical And Scope Preservation

- Unchanged historical319 LF manifest excluding only V019: `57990aa0bc0886c45a58f4e63698d05f860f94a667bc047b2072b6251f3a66fd`.
- Unchanged historical318 LF manifest excluding V018/V019: `26a10794d635d28bb402a4c88ee27b4c486ac09175b637f90faa122b38114f4a`.
- Recomputed unchanged F26: `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761`; H26 with only the historical README substitution: `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`. Both original literal26 maps are unchanged.
- V001–V018 raw bytes and historical history18 basenames/checksums are unchanged. Historical PA25 raw/LF pins remain `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9` / `8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451`; the existing reconstruction proof passes.
- Same exact22 customer column SELECT grants, customer53→75, PA25 total410→432, history18→19, seven login roles, nine monitored roles, eleven membership edges. These are preserved source/oracle requirements, not newly observed database results.
- All52 accepted implementation paths exist. Same146 ordinary classes,1708 method identities,1709 annotation lines; the identity guard passes. No new test identity or persistent database object, V020, production Java, OpenAPI, docs12/14/15/17, Control, plan/review, POM/CI, private helper, Git metadata or unrelated source edit is made in this fix. Existing implementation changes from before this task are retained.
- Before/after raw-byte snapshots cover 2,070 backend/docs/CI/API/protocol/security/deployment/acceptance files excluding generated outputs/dependencies. All captured files outside these fix paths remain byte-identical. V019 equals its snapshot with exactly the basename substitution; PA25 equals its snapshot with exactly the CRC substitution; all other changed tests except the target IT equal their snapshots with only the enumerated current-pin substitutions.

## 6. Checks Actually Run

WSL-native Java21 `/opt/jdk21` and Maven3.9.9 `/opt/maven-3.9.9`, offline cached dependencies; commands run from backend unless stated otherwise:

```sh
mvn -o -B -ntp -Dtest=CustomerCatalogOfferReadAclBoundaryTests test
mvn -o -B -ntp -Dtest=CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectClaimBoundaryTests,CustomerSubjectActivationBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsActivationReadBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogDraftBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test
mvn -o -B -ntp -Pcontainer-smoke -DskipTests test-compile
```

- New boundary selector: **exit0; 8 tests, 0 failures/errors/skips**.
- Plan focused existing-unit selector: **exit0; 80 tests, 0 failures/errors/skips**. Combined focused evidence: **88 tests**, no failures/errors/skips.
- Container-smoke test compilation: **exit0**; no tests executed by `test-compile`. This compiles the corrected IT, not a PostgreSQL execution.
- Scoped `git diff --check -- <changed intended paths>` using the read-only Git overrides: **exit0**. All ten full files are also checked against `/dev/null` with `git diff --no-index --check`; Git returns1 for content differences, with no final whitespace-error diagnostics. The initial full-file probe found one preexisting trailing-whitespace-only line in the target IT; that line is cleaned before the final successful probes. Existing LF→CRLF advisory warnings do not rewrite checkout bytes.
- Independent hash/CRC/C27/320/319/318 recomputation, F26/H26 recomputation, exact52-path existence, historical arrays, byte scope and current-pin-only checks: pass. A global superseded-current-value search finds no remaining binding source/report consumers.
- Temporary native logs: `/tmp/pa40a-fix1-boundary.log`, `/tmp/pa40a-fix1-focused.log`, `/tmp/pa40a-fix1-compile.log`. Temporary hash/scope evidence stays outside the repository.

No Docker, Windows executable, PostgreSQL/psql command, Failsafe IT, clean full verify, JAR/firewall gate or contract gate is launched by this fix author. The failed Controller Failsafe XML/txt are retained and are not relabeled as passing.

## 7. Controller-Pending Windows/PostgreSQL16 Validation

From the Windows backend worktree with the authorized Java/Maven/Docker/PostgreSQL16 environment:

```powershell
mvn -B -ntp -Dtest=CustomerCatalogOfferReadAclBoundaryTests test
mvn -B -ntp -Pcontainer-smoke -DskipTests test-compile
mvn -B -ntp -Pcontainer-smoke "-Dit.test=CustomerCatalogOfferReadPrivilegesIT" failsafe:integration-test failsafe:verify
mvn -B -ntp -Dtest=CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectClaimBoundaryTests,CustomerSubjectActivationBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsActivationReadBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogDraftBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test
mvn -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT,DatabaseRuntimeAclRbacHardeningIT,ProductionDatabaseIdentityProvisioningIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,CustomerEvidenceReadPrivilegeIT,EvidenceRoleIvrAssociationAclIT failsafe:integration-test failsafe:verify
mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerMeIT,CustomerSubjectsIT,CustomerBusinessNumbersIT,CustomerEvidenceIT,CustomerSubjectClaimsIT,CustomerSubjectActivationsIT,SubjectClaimRoleLeastPrivilegeIT,ActivationCommandRoleLeastPrivilegeIT,OperationsSubjectReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT,OperationsActivationReadsIT,OperationsActivationReadRoleLeastPrivilegeIT,OperationsIvrSessionReadsIT,OperationsCatalogProductReadsIT,OperationsCatalogProductReadRoleLeastPrivilegeIT,OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT,OperationsCatalogDraftLifecycleIT,OperationsCatalogDraftRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
mvn -B -ntp clean verify -Pcontainer-smoke
```

Controller must also execute the original implementation report §7's unchanged exact XML identity/JAR/dependency/firewall/contract gates and disposable-fixture PA25 apply/check commands. Require 1026 Surefire + 682 Failsafe = 1708 exact identities, zero failures/errors/skips, fresh/upgrade/replay/rejection preservation, and exact PA25 topology/history/ACL success evidence; none is claimed here. Recheck the final primary raw/LF pins before execution. Use fresh disposable fixtures or authorized predecessor18 fixtures; do not repair checksums in an existing history19 database to bypass Flyway validation, and do not replay V001–V018 after V019.

## 8. Residual Risks And Blocker Status

- Authoring blocker: **none**. Both confirmed root causes and mechanically cascading current pins are corrected within scope.
- Runtime acceptance blocker: **Controller rerun pending**. Native unit/compile success does not prove the corrected 16 PostgreSQL tests or the full gates succeed; further runtime defects remain possible until those gates execute.
- Migration bytes/checksum have intentionally changed before acceptance. Preserve raw representation and dispose/recreate failed disposable V019 fixtures as appropriate rather than normalizing bytes or rewriting accepted history.
- Column ACLs remain role-wide reads, not row filtering/RLS or endpoint authorization. No production readiness or endpoint implementation is claimed.
- Stop at this fix. No stage, commit, push, PR, merge, approval, mark-ready, reviewer request, external comment/issue mutation, or code-review gate is performed.

## 9. Controller Runtime Continuation — Fix 2

On 2026-11-23, Controller reported that the corrected PostgreSQL16 `CustomerCatalogOfferReadPrivilegesIT` passes **16/16**. This is Controller-reported evidence, not a new integration-test run by the fix-2 author. The plan's 80-test focused selector exposed two pre-existing Windows path-spelling failures: `CustomerSubjectActivationBoundaryTests.openApiDataDocsAndAllSeventeenMigrationsRemainFrozen` (line 56) and `CustomerSubjectClaimBoundaryTests.openApiMigrationsAndProductionProviderAbsenceRemainFrozen` (line 73).

Fix 2 normalizes only the two actual migration-path mappings with `path.toString().replace('\\','/')`, matching `CustomerCatalogOfferReadAclBoundaryTests`. The exact expected V001–V018 basenames plus authorized V019, exact count/order-independent comparison, frozen hashes, and every other historical assertion remain unchanged. No V019, PA25, application source, other test, Control, plan/review, POM/CI, OpenAPI, or private helper is edited.

The bounded change, native validation results, and remaining Controller Windows rerun are recorded in `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-2.md`. Earlier failure evidence and pending full acceptance gates remain historical records; the reported 16/16 does not claim completion of those gates. No staging, commit, push, PR, merge, or review entry is performed.
