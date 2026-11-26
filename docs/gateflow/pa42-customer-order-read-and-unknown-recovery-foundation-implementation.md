# PA42 customer order reads and UNKNOWN visibility — implementation evidence

## 1. Identity, authority and disposition

- Task: `pa42-customer-order-read-implementation-fix3-gpt-20261125-230500`.
- Execution began 2026-11-25 and continued into 2026-11-26; timezone: Asia/Shanghai.
- Authorized worktree: `C:\Users\Administrator\.codex\worktrees\pa42-customer-order-read\codex-call`.
- Accepted plan: `docs/gateflow/pa42-customer-order-read-and-unknown-recovery-foundation-plan.md`, 428 lines / 74,189 bytes; raw SHA256 `e39f31a8bf071d1a13b753cd54cc636f7c4aebd4f0e5ee7f39308c36707a940f`.
- Authority: accepted plan, active PA42 checkpoint in `docs/gateflow/project-execution-control.md`, and the user's explicit one-path controller exception.
- Local HEAD: `b4073ae6db79cb3c935485aa4d586696dc93a4a1`.
- Disposition: implementation and all five required local validation phases complete; final source/CI/POM/XML identities match. Local validation is not deployment acceptance or authorization to advance a gate.
- All accumulated implementation changes were preserved. No restart, revert, staging, commit, branch creation or remote action was performed.

## 2. Scope and continuation fixes

| Classification | Accumulated delta relative to HEAD |
| --- | ---: |
| Approved new implementation/test/migration paths | 16 |
| Approved modified implementation paths | 10 |
| Original approved modified bookkeeping paths | 58 |
| Explicitly authorized additional bookkeeping guard | 1 |
| Product/bookkeeping scope | **85: 16 new + 69 modified** |
| Additional implementation report | 1 |
| Owned product/bookkeeping/report paths | **86** |
| Inherited Control modification, not edited here | 1 |
| Owned paths plus inherited Control | **87** |

The accepted plan itself remains unchanged; the controller exception increases bookkeeping from 58 to 59 and the planned product/bookkeeping scope from 84 to 85. All 85 paths have textual changes. The report is additional, not silently counted as one of the sixteen new product paths. Inherited `.codex` and private `.pa42-*` prompts remain untouched and excluded.

Continuation changes:

1. Changed only `getDeclaredMethods()` expected size **18→21** in the authorized `CustomerBusinessNumberQueryServiceTests` inventory guard. Every lazy/bean, transaction, DSL and qualifier assertion remains; equality was not weakened.
2. Corrected the new positional PA25 probe to `ops/postgresql/pa25-runtime-identities.sql`, relative to `backend/`. Parsed its actual `$frozen$` JSON instead of nonexistent SQL tuple literals; retained exact 449/432 sizes and exact equality of the first 432 positions.
3. Reconciled final CI method identities with actual source, including the accepted POST-route test identity replacement, all required PA42 names, retained expanded probes, exact current counts and identity hash. Historical PA41 and earlier ledgers remain independently reconstructed.
4. Corrected current inventory expectations in approved catalog-offer/evidence bookkeeping guards without changing historical equality assertions.
5. Removed a new test fixture's illegal attempt to delete a subject already referenced by an order: workspace-only fixtures now insert organization/membership rows legally without rewriting order subjects.
6. Completed PostgreSQL probes for missing account/organization, inactive organization/membership, same-account/different-organization exclusion, legal homogeneous issuance states, absent/invalid shared keys, no-query precedence, detail independence from keys, role cleanup and actual bound EXPLAIN shapes.
7. Added exact packaged-migration bytes and full compiled-resource/JAR entry-byte equality to CI, plus packaged GET route/authentication/alias probes.
8. Historical regression execution exposed stale current-state fixtures, not a reason to change V020: changed the widened seven-role ACL inventory 313→330; pinned the historical catalog-offer fresh fixture to V019; replaced an obsolete customer-role `SELECT id` denial with that approved positive read plus exact SQLSTATE42501 hidden-column/locking/write/role-escalation denials; moved a hostile history row from colliding rank20 to1020; reconciled the V014 guard's current migration inventory to exact001–020; and extended the IVR current frozen-byte/history oracle to V020 while retaining its independent V018/V019 historical digests and mutation/rejection probes.

No assertion was disabled, excluded, weakened to containment instead of equality, or made conditional on stale reports. No migration before V020, frozen contract, production command graph, lifecycle SQL, property family, role, pool or schema object was changed.

## 3. Implemented feature boundary

Only the frozen list/detail GET operations are added; existing POST behavior remains covered by its original tests.

- Owner module `catalog_order` owns the public read port, prepared SQL and validating historical snapshot seam. Existing subject-count named interface and callers remain.
- Customer IAM orchestrates trusted account plus selected PERSONAL/ORGANIZATION workspace, existing ACTIVE account/organization/membership/allowed-role checks and exactly the customer read role.
- Both reads use the existing customer datasource/manager in one read-only REQUIRES_NEW / REPEATABLE_READ transaction. Role verification and lifecycle checks precede data reads.
- Keyset pages use PostgreSQL `(created_at,id)` descending order, limit+1, a nonprojected sentinel and one batched raw-status query only for returned IDs. Empty pages skip issuance SQL.
- Issuance status is the complete validated raw status-set projection: NONE, PENDING, ISSUED, FAILED or MIXED. No grant/expiry/schedule join and no write is introduced.
- UNKNOWN remains visible. Detail reads stored order facts and a semantically/hash-validated immutable snapshot, never current catalog reconstruction.
- Corrupt list facts map to sanitized 500; corrupt/missing/foreign detail to indistinguishable 404; dependency/role/completion failure to sanitized 503; lifecycle denial remains 403.
- Cursor uses the existing catalog-offers key with a separate orders HMAC domain, exact canonical scope/filter binding and key-free structural validation. Limit/structure/key/MAC/lifecycle precedence and detail key independence are tested.

No payment reconciliation, provider call, repair, issuance action, cancellation, enterprise checkout, frontend, read model, new secret or index is added.

## 4. Environment and exact validation commands

Executed from `backend/` with:

```sh
export JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH
java -version
./mvnw -version
```

Observed: Temurin OpenJDK **21.0.13+9-LTS**, checked-in Maven wrapper **3.9.9**, Docker server **29.1.3**, and actual container **PostgreSQL 16.16** from `postgres:16`.

Accepted-plan commands, without test exclusions:

```sh
./mvnw -B -ntp -Dtest=JooqCustomerOrderReaderTests,CustomerOrderSnapshotReaderTests,CustomerOrderQueryServiceTests,CustomerOrderCursorCodecTests,CustomerOrderReadConfigurationTests,CustomerOrdersWebTests,CustomerOrderConfigurationTests,CustomerWebConfigurationTests test
./mvnw -B -ntp -Dtest=CustomerOrderReadBoundaryTests,ModuleBoundaryTests,CustomerApiBoundaryTests,CustomerOrderCreationBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests,CustomerCatalogOfferBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsCatalogDraftBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsActivationReadBoundaryTests,SharingQueryBoundaryTests,OperationsLifecycleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerSubjectClaimBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsSubjectsWebTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerOrdersReadIT,CustomerOrderReadPrivilegesIT verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=ProductionDatabaseIdentityProvisioningIT,DatabaseRuntimeAclRbacHardeningIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,CustomerEvidenceReadPrivilegeIT,EvidenceRoleIvrAssociationAclIT,CustomerCatalogOfferReadPrivilegesIT,CustomerOrderRoleLeastPrivilegeIT,OperationsIvrSessionReadsIT verify
./mvnw -B -ntp -Pcontainer-smoke verify
```

| Phase | Tests / failures / errors / skips | Evidence |
| --- | --- | --- |
| Exact focused unit/web/configuration | **56 / 0 / 0 / 0** | `/tmp/pa42-fix3-focused.log`; BUILD SUCCESS, 21:46:06 +08:00 |
| Exact architecture/module/PA25 guard set | **138 / 0 / 0 / 0** | `/tmp/pa42-fix3-guards-green.log`; BUILD SUCCESS, 21:52:19 +08:00 |
| PA42 verify final unit phase | **1,164 / 0 / 0 / 0** | `/tmp/pa42-fix3-pa42-it-final.log` |
| PA42 PostgreSQL16 Failsafe | **26 / 0 / 0 / 0**: reads16, privileges10 | `/tmp/pa42-fix3-pa42-it-final.log`; BUILD SUCCESS, 22:13:28 +08:00 |
| Historical ACL/provisioning/nonregression verify final unit phase | **1,164 / 0 / 0 / 0** | `/tmp/pa42-fix3-historical-it-final.log` |
| Historical ACL/provisioning/nonregression Failsafe | **128 / 0 / 0 / 0**, nine classes | `/tmp/pa42-fix3-historical-it-final.log`; BUILD SUCCESS, 2026-11-26 01:03:55 +08:00 |
| Mandatory clean full container-smoke verify, units | **1,164 / 0 / 0 / 0**, 122 classes | `/tmp/pa42-fix3-full.log` |
| Mandatory clean full container-smoke verify, integration | **764 / 0 / 0 / 0**, 46 classes | `/tmp/pa42-fix3-full.log`; BUILD SUCCESS, 2026-11-26 03:55:07 +08:00 |
| Exact CI source/POM/XML identity checker | **168 classes / 1,928 methods**, exact identities, no failures/errors/skips | `/tmp/pa42-fix3-ci.log`; exact current workflow Python executed after full verify |
| Packaged default/web/error-path smokes | **212 HTTP probes**, default lifecycle smoke and both error-path configurations pass | `/tmp/pa42-fix3-jar-smoke.log`; JVMs cleaned up |
| Production dependency/JAR firewall | **116 adversarial probes** pass; baseline37/34, SSM72/65, counterparts64, dependency-only8, packager1 | `/tmp/pa42-fix3-firewall.log`; exact current workflow Python |

Overlapping commands are not summed into a fictitious unique suite. Initial guard runs exposed stale current count/hash assertions in approved paths; the final 138-test command passes after direct-source reconciliation. The first expanded PA42 verify had **1,164 green unit tests**, then **26 ITs / 1 failure / 1 error / 0 skips**: the positional parser and illegal workspace fixture described in §2. Those results are superseded only by an actual successful rerun, not by editing the report.

The first exact historical command finished at 23:07:51 +08:00 with **1,164 green units**, then **128 ITs / 6 failures / 1 error / 0 skips**. Its seven unsuccessful methods were the business-number widened ACL count, catalog-offer historical fresh fixture, obsolete PA41 read denial, subject-detail hostile rank collision, two V014 current migration-inventory guards, and the IVR historical/current frozen oracle. Corrections stay within approved paths and retain exact historical boundaries; `/tmp/pa42-fix3-historical-first-totals.json` records method identities and diagnostic messages.

The second exact historical command finished on 2026-11-26 at 00:02:41 +08:00 with **1,164 green units**, then **128 ITs / 1 failure / 0 errors / 0 skips**: the IVR current table/bare representation counts still expected27 instead of28. Both were corrected without relaxing equality. A targeted `-Dit.test=OperationsIvrSessionReadsIT#postgres16UsesExactlyFrozenEighteenMigrations verify` then passed **1,164 units + 1 IT**, all zero failures/errors/skips, at 00:09:43 +08:00. `/tmp/pa42-fix3-historical-it-second.log` and `/tmp/pa42-fix3-ivr-oracle.log` retain these distinct runs.

The third exact historical command passes all **128 ITs**: provisioning20, runtime ACL12, business-number8, subject-detail10, evidence10, evidence/IVR16, catalog-offer16, PA41 command-role12 and IVR24. XML identities/checksums were preserved in `/tmp/pa42-fix3-historical-xml-summary.json` before `mvn clean`; the mandatory full verify starts from a freshly removed `target/`, not accumulated reports/classes.

The first clean mandatory full verify finished on 2026-11-26 at 02:25:58 +08:00: **122 classes / 1,164 units / 0 failures / 0 errors / 0 skips**, and **46 classes / 764 ITs / 4 failures / 0 errors / 0 skips**. All four failures share `PostgresContainerIT.assertV009LiveFence`: its exact resolved migration list omitted V020 although its historical applied V001–V013 equality remains correct. Added only V020 to the approved resolved list, preserving historical applied history, ACL and guard assertions. `/tmp/pa42-fix3-full-first.log` and `/tmp/pa42-fix3-full-first-totals.json` retain the unsuccessful full run; it is not presented as passing.

Targeted `-Dit.test=PostgresContainerIT verify` passed **1,164 units + 64 ITs**, zero failures/errors/skips, on 2026-11-26 at 02:33:11 +08:00 (`/tmp/pa42-fix3-postgres-fence.log`). Another `mvn clean` preceded the final exact full verify: **1,928 unique methods passed**. All 168 XML files have mtimes at or after its recorded start epoch; equality is not based on leftover focused reports. Full-run `OperationsCatalogDraftLifecycleIT` passes **24/24**, and its historically named `postgres16RunsExactlyV001ThroughV018` passes in **14.906 seconds**, executing the current exact V001–V020 history assertion, not an obsolete eighteen-migration target.

The first packaged smoke incorrectly treated `/api/v1/customer/orders/extra` as an unmatched alias; it is a single detail path segment, so authentication correctly precedes ID validation (401 without authentication; unavailable verifier503 in the bounded packaged fixture). The CI-only probe now asserts that precedence and separately rejects extra-depth aliases with exact404; no production path matcher changed. `/tmp/pa42-fix3-jar-smoke-first.log` preserves the failed expectation. All three Python programs extracted directly from the final workflow were then executed successfully: source/CI/POM/fresh XML identities, default plus both web/error-path JAR smokes, and dependency/packaged-entry firewall. The final workflow-only correction does not alter compiled source/resources or test identities.

## 5. Exact source, CI, POM, XML and packaging inventories

Direct-source preflight passes with **332 production Java files, 216 test Java files, 23 production resources and 20 SQL migrations**.

| Inventory | Exact current size |
| --- | ---: |
| Unit test classes / ordinary methods | 122 / 1,164 |
| Integration test classes / ordinary methods | 46 / 764 |
| All classes / ordinary methods | 168 / 1,928 |
| Annotation-line scan | 1,929 |
| Exact Failsafe POM includes | 46 |
| Production Java/resource manifest members | 355 |

- Current sorted `class#method` identity manifest SHA256: `0af27da2f85f6239d0e04a3a694fd4bbde7b325208a7771fbf359db80c056ae8`.
- Production/resource LF-normalized path/hash manifest SHA256: `f851cbd4039f24d9fb96792684c317e04e047ba59bd7a6ef0f95a0c94b40328c`.
- The extra annotation-line hit is the inherited `PostgresContainerIT` comment, not a test or a skip.
- PA41 historical identity totals remain **116/1,127 + 44/738 = 160/1,865**, annotation lines 1,866. All earlier historical ledgers remain exact.
- PA42 current additions are **37 unit methods in seven class entries** and **26 integration methods in two new classes**. The existing route test identity replacement changes no total and is normalized back only for historical reconstruction.
- Current source/CI method maps, POM include identities and fresh full-run Surefire/Failsafe method identities pass exact equality through the actual workflow checker, including every historical reconstruction.

Historical executable JAR (fix3-era; superseded by §11 and the final continuation evidence in §12): SHA256 `b9efc511b653ee16832c5751a8634ba39dd5d46c12529142b72443588abc2016`, **40,192,156 bytes**. Its **811** non-directory `BOOT-INF/classes/` entries equal the complete `target/classes` relative-path set, with every entry byte-equal and no duplicate entries. Packaged migration paths equal the exact twenty source V001–V020 paths; each is byte-identical to source and V020 occurs exactly once. `/tmp/pa42-fix3-packaged-final.json` records that historical inventory and draft-lifecycle XML result. Section 11 records the later successful retry JAR (`c903d78d38c9fd82a37abb31502ef372dc9d46e201068227c637d7ba71483bbe`, **40,192,267 bytes**); §12 identifies the final continuation build by its own hash and content-parity checks. Whole-JAR reproducibility across clean repackages is not claimed.

## 6. Frozen bytes and migration/provisioning identities

Against `/tmp/pa42-fix3-before.sha256`, all **33** frozen files remain raw-byte identical: V001–V019, every file under `api/openapi/`, and docs12/14/15/17. This includes both OpenAPI specifications and the associated contract tooling/configuration.

Detailed raw/LF manifest: `/tmp/pa42-fix3-frozen-final.json`, SHA256 `2b84bce38e2a91ca366b20f5f7c101922d970f9876408ea4d00eaa51ff28cf80`. Git HEAD stores normalized LF while inherited working files use CRLF; normalized HEAD comparison is separate and is not misrepresented as raw HEAD-byte equality.

| Artifact | Identity |
| --- | --- |
| V020 raw/LF SHA256 | `804c46dda94cdfdf609b16057200fe5a9a526f288ef291ad82fc71edeeaf19dd` |
| V020 resolved Flyway checksum | `207560051` |
| Current PA25 raw SHA256 | `2558af4013d8568fa04271cf402487b1cb66de050bea7ee237623d9059a0e12e` |
| Current PA25 LF SHA256 | `b1cd2e464810e00d69482be2dff26d5cf5c9dc23dd098fbc1cf2ec26417e0853` |
| Reconstructed historical V019 PA25 LF SHA256 | `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` |
| Reconstructed historical V018 PA25 LF SHA256 | `8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451` |
| Reconstructed historical V018 PA25 CRLF SHA256 | `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9` |

V020 is unchanged in this continuation. It retains one guarded transactional DO block and exactly three owner-issued GRANT statements: fourteen order column SELECTs; entitlement schema USAGE; two issuance column SELECTs. The seven-field ACL delta is exactly **17**, global current inventory **449**, customer inventory **92 = seven schema USAGE + 85 column SELECT**; the other roles retain exactly 357 tuples. No grant option, table SELECT, write, ownership or topology expansion is authorized.

Focused and final full-run PostgreSQL evidence proves fresh and V019 upgrade to exactly twenty successful migrations/one V020 row; zero second Flyway migrations; raw replay in old-grant and completed states; exact owner grantors; exact 432-prefix positions; historical 432/410 reconstruction; unchanged seven logins/eleven membership edges; current PA25 apply/check; and rejection rather than repair of mismatched ACL/history states.

All thirteen failure categories passed the final focused PA42 PostgreSQL run: **AUTHORITY, PROVENANCE, HISTORY, SETTINGS, ATTRIBUTES, TOPOLOGY, OWNERSHIP, ACL, DATABASE, DEFAULT_ACL, DEPENDENCY, PRESERVATION, RECONCILIATION**. Tests preserve SQLSTATE `P0001`, safe category messages and before/after snapshots. Rejection probes include partial/extra/wrong-grantor/grant-option grants; completed-history missing grants; invalid self-row identity/rank/checksum-null/success; role settings/attributes/membership/ownership/default privileges; forged provenance/database/dependency state; and injected preservation/reconciliation failures. Raw self-row checks retain the family's nonnull checksum rule; Flyway validation independently checks the resolved migration checksum.

## 7. Read-only, projection, role and EXPLAIN evidence

The expanded PostgreSQL suite checks real HTTP PERSONAL/ORGANIZATION ownership, all four allowed membership roles, same-organization/different-account and same-account/different-organization isolation, ACTIVE/missing/inactive lifecycle, stable unsigned PostgreSQL UUID/timestamp paging, exact response keys and nullable timestamps, UNKNOWN/all payment states, NONE/homogeneous/mixed issuance, catalog mutation versus stored snapshot, safe-integer corruption, SQL constraints, key/error precedence and dependency rollback.

Real customer login `pa27_customer` SETs `customer_iam_role`; tests assert current/session identity, transaction isolation/read-only state and pool restoration on success/failure/reuse. Direct role ACL tests deny hidden columns, SELECT *, writes, owner/service role switches, DDL/default-ACL escalation and unrelated entitlement data.

Database before/after equality inventories cover business rows, idempotency/audit/Outbox, object/column definitions and ACLs, schemas/relations/functions/default privileges, dependencies, databases/settings, roles/memberships, sequences and exact Flyway rows. Concurrent committed issuance changes are invisible inside an existing repeatable-read request and visible to the next request.

Representative EXPLAIN fixture: **10,001 orders, ten accounts, two organizations, dense account 8,201 orders, 1,000 repeated timestamp values and 10,000 issuance rows**, including zero/one/many fan-out. Fixture-owner ANALYZE precedes actual prepared owner-port SQL under customer_iam_role.

Eleven `EXPLAIN (ANALYZE, BUFFERS)` statements cover PERSONAL/ORGANIZATION first/deep lists at API limit20/100 (SQL limits21/101), both details and a 100-order issuance batch. Successful final full-run output records PostgreSQL version, actual prepared SQL and bound values/counts, nodes/indexes, actual rows/loops/rows removed, sort memory, planning/execution times and buffers. All plan nodes have one loop; data buffers are warm shared hits, with no reported shared reads or spills.

| Statement / shape | Actual rows / filtered rows | Data shared hits | Planning / execution ms | Measured nodes / memory |
| --- | --- | ---: | --- | --- |
| 0 PERSONAL first20 | 21 returned; 2,734 scanned matches / 7,267 removed | 1,673 | 0.299 / 2.636 | Sequential scan + top-N, 30kB |
| 1 PERSONAL deep20 | 21 returned; 820 matches / 183 heap-filter removals | 511 | 0.266 / 0.930 | Bitmap index/heap `ix_customer_order_organization`, top-N30kB; 1,003 index rows / 507 exact heap blocks |
| 2 PERSONAL first100 | 101 returned; 2,734 matches / 7,267 removed | 1,667 | 0.047 / 1.913 | Sequential scan + top-N52kB |
| 3 PERSONAL deep100 | 101 returned; 820 matches / 183 removed | 511 | 0.065 / 0.764 | Same bitmap path, top-N51kB |
| 4 PERSONAL detail | 1 / 0 | 3 | 0.074 / 0.036 | `pk_customer_order` index scan |
| 5 ORGANIZATION first20 | 21 returned; index22 / 5 removed | 32 | 0.074 / 0.047 | Organization index + incremental sort, one full-sort group, 27kB |
| 6 ORGANIZATION deep20 | 21 returned; index24 / 7 removed | 33 | 0.063 / 0.053 | Same index/sort, one group, 28kB |
| 7 ORGANIZATION first100 | 101 returned; index102 / 22 removed | 127 | 0.049 / 0.105 | Same index/sort, four groups, 29kB |
| 8 ORGANIZATION deep100 | 101 returned; index103 / 24 removed | 130 | 0.061 / 0.144 | Same index/sort, four groups, 29kB |
| 9 ORGANIZATION detail | 1 / 0 | 3 | 0.035 / 0.020 | `pk_customer_order` index scan |
| 10 issuance batch100 | 100 / 9,900 | 164 | 0.287 / 0.679 | Sequential issuance scan; only order_id/status projection |

These are local fixture measurements, not production SLOs. Personal first-page scans and issuance fan-out remain capacity/index-review inputs; no planner forcing or index DDL was added. Raw statements/plans are in `/tmp/pa42-fix3-full.log`; extracted successful final measurements are in `/tmp/pa42-fix3-explain-final.json`.

## 8. Residual ownership and no-remote-action boundary

- Customer Data/Database owner: measured scans/sorts and fan-out are input to the separate docs18 capacity/index-DDL work unit. No universal latency guarantee or new index is claimed.
- Security/Operations: shared catalog-offers/orders cursor-key rotation invalidates both cursor families; independent credential policy remains a separate work unit.
- Customer Orders/Data Integrity owner: corrupt historical facts remain fail-closed; no repair/backfill is performed.
- Product/Design owner: organization visibility is current account plus selected workspace, not organization-wide commerce sharing or enterprise creation.
- Design/Contract/Security owner: cursor envelopes are authenticated and opaque, not confidential ciphertext; family-wide confidentiality migration is out of scope.
- Production deployment acceptance is not executed; local fixture validation does not change that runbook status.

No staging, commit, push, PR, merge, approval, ready marking, reviewer request, external comment or other remote action occurred. All five mandated local Maven phases pass; production deployment acceptance remains NOT EXECUTED and no gate advancement is claimed.

## 9. Final local evidence ledger

All paths below are local runner evidence, not newly authorized repository artifacts. SHA256 values identify the actual successful final outputs; earlier failed attempts remain separately named and are not substituted for green evidence.

| Evidence | Bytes | SHA256 |
| --- | ---: | --- |
| `/tmp/pa42-fix3-focused.log` | 36,096 | `a6849be3dc4685f5828aff8057537677035f56b0e472dd76e7627fe9ade2723d` |
| `/tmp/pa42-fix3-guards-green.log` | 12,701 | `9ee63a8e4123dd45c6edad7be253d8881228d242cfc9326e896a34672d26ce4d` |
| `/tmp/pa42-fix3-pa42-it-final.log` | 513,617 | `3c97ea8beaee289ae73158cd3a53ad21274c7fe4c8515465d651198585b94737` |
| `/tmp/pa42-fix3-historical-it-final.log` | 1,176,286 | `0de5b2bb238deb6a20e42b2899110a642ffb77dfcb527ba2f801e10f45a87ecb` |
| `/tmp/pa42-fix3-full.log` | 1,897,821 | `27c2930745a851b5f39dc823fee3f422af9522aa6531eb76db440db296f18d67` |
| `/tmp/pa42-fix3-ci.log` | 1,357 | `0611616cc7979ee3787c03bc64ca660f9ccda9068909d59278a2dedafc84673e` |
| `/tmp/pa42-fix3-jar-smoke.log` | 18,477 | `5589ef3dec5dc78eef3de5617939b59a38ba2cbbb2205a3e1a5c685bfa508394` |
| `/tmp/pa42-fix3-firewall.log` | 534 | `981d7f7af1b485a8f7906a8177949d6bd87bc0bee66eb0724cf072593704c150` |
| `/tmp/pa42-fix3-historical-xml-summary.json` | 31,843 | `b32252ba79e043d1d157c9b0b16c03a87a67e449ce458e1156782f8e578c4bb1` |
| `/tmp/pa42-fix3-final-xml-totals.json` | 226 | `6dc9829133c089b31fc2d85aa872793f69412b502ecf3e1feff78005deefd9fb` |
| `/tmp/pa42-fix3-packaged-final.json` | 1,772 | `ed4fdf7c7256b5bf6fa45dfaa6668b4d6d57ec3adcf5ac769a663ee14283b8fa` |
| `/tmp/pa42-fix3-explain-final.json` | 8,065 | `f6030c312cad33b338d82d40410c924d2999e977f088e17ac43041fd250c43c7` |
| `/tmp/pa42-fix3-scope.json` | 19,947 | `2bf09b3d1c92d491e79d6845067a5806ed6bdc714170039b37584b29ef82ab73` |

Final scope re-enumeration confirms exactly **85 product/bookkeeping paths = 16 new + 69 modified, plus this report**. The inherited Control remains raw-byte unchanged from the start of this continuation. Accepted plan and all 33 frozen contract/migration files remain unchanged. The additional configuration guard differs from HEAD only by the authorized 18→21 assertion. `git diff --check` passes using the repository's configured line-ending normalization; all sixteen new files also pass a direct trailing-whitespace scan.

**Blockers: none for the authorized local implementation/validation slice.** No tooling blocker, unlisted-path expansion or unresolved user-visible/security decision remains. Production deployment acceptance is **NOT EXECUTED**; all residuals retain the explicit owners and separate destinations in §8.

## 10. Code-review fix1

- `CustomerOrderReadPrivilegesIT.customerRoleCanReadOnlyApprovedColumns` now executes the PUBLIC grant inside a savepoint, rolls it back, and verifies through `pg_attribute.attacl`/`aclexplode` that PUBLIC has no `id` column SELECT privilege while the admin-side ACL set remains exactly `current449Oracle()`. Existing denial, owner, ACL and role assertions are unchanged.
- `CustomerOrdersReadIT.catalogMutationCannotChangeStoredSnapshotProjection` now requires a non-empty captured catalog-order SELECT collection before applying the independence assertion.
- `DefaultCustomerOrderSnapshotReader` translates only `OrderCreationException` semantic validation/integrity failures to the fixed-message `InvalidSnapshotException`, preserving the cause without snapshot data; unexpected runtime/encoding failures remain uncaught for sanitized 500 handling.
- Unsafe-integer reader negatives now pass SHA-256 of each actual mutated payload, so the safe-integer rejection is the only possible failure reason.
- No public operation contract, V020, frozen input, test identity, method name or accepted 85-path scope changed. Final identities remain **122 unit classes / 1,164 ordinary methods** and **46 integration classes / 764 ordinary methods**; CI annotation inventory remains **1,929 methods** with **1,928 unique XML identities**.
- The five changed source/test paths have a sorted LF-normalized manifest of **5 lines / 824 bytes**, SHA256 `bb9181c930e5a929abacf2c19b342b883f49375c689687445e5b7a43acc66ffa`.

### Fix validation

| Validation | Result |
| --- | --- |
| Focused snapshot/query/web/configuration | **56 / 0 / 0 / 0**, BUILD SUCCESS |
| CustomerOrdersReadIT PostgreSQL 16 Failsafe | **16 / 0 / 0 / 0**, BUILD SUCCESS |
| CustomerOrderReadPrivilegesIT PostgreSQL 16 Failsafe | **10 / 0 / 0 / 0**, BUILD SUCCESS |
| Focused PA42 architecture/module/PA25 guards | **134 / 4 / 0 / 0**; four unrelated frozen-byte/resource inventory assertions remain blockers |

The four guard failures are unchanged frozen-input checks: `CustomerCatalogOfferReadAclBoundaryTests` reports the pre-existing V001–V018 and packaged-resource hashes, `CustomerOrderCreationBoundaryTests` reports the same frozen hash, and `EvidenceRoleIvrAssociationAclBoundaryTests` reports its frozen hash. No frozen input, guard assertion, source/CI identity or POM inventory was changed to mask them.

Section 10 is the preserved fix1 evidence snapshot: its **134 / 4 / 0 / 0** guard attempt was blocked by stale deterministic pins and is superseded by the successful pin-only retry recorded in §11. Its failure history is retained.

## 11. Code-review fix2 retry1

The fix1 implementation and accepted **85-path product/bookkeeping scope** were preserved. Before changing deterministic pins, each manifest was reconstructed from the final working-tree bytes using the corresponding test's exact path selection, LF normalization, sorted path/content framing and SHA-256 operation; each candidate was independently recomputed twice with identical output. The five approved changed source/test paths remained exactly the sorted **5-line / 824-byte** manifest with SHA256 `bb9181c930e5a929abacf2c19b342b883f49375c689687445e5b7a43acc66ffa`. No additional code-review-fix source/test delta was found.

| Deterministic manifest assertion | Entries / LF-normalized bytes | Independently recomputed SHA256 |
| --- | ---: | --- |
| Catalog V001–V018 frozen contracts and migrations | 353 / 55,041 | `93764e87e85a318753f98a20dfaaa46bde76a0e6e4113cd856ca1172b03cdbae` |
| Catalog migration/resource inventory | 355 / 55,325 | `7d7346e43c1c6d744ec803e4746d9b33b42ca33aa7e89c8beff317cc0cf3910d` |
| Evidence-role V001–V017 and public-contract frozen manifest | 352 / 54,888 | `9bb26c371439c984615b885ef1f854e7a86cc561176b693646ba6108c2568d79` |

Only the three stale expected literals were updated. `CustomerOrderCreationBoundaryTests.frozenContractsMigrationsAndPa25RemainByteIdentical` delegates to the catalog assertion and contains no separate expected hash. No assertion was weakened or removed.

### Retry validation

| Validation | Result |
| --- | --- |
| Exact architecture/module/PA25 guard set | **138 / 0 / 0 / 0**, BUILD SUCCESS |
| Clean `-Pcontainer-smoke clean verify` | Surefire **1,164 / 0 / 0 / 0**; Failsafe **764 / 0 / 0 / 0**; BUILD SUCCESS |
| Current workflow source=CI=POM=fresh-XML identity | **122 / 1,164 unit; 46 / 764 integration; 1,929 annotation methods; 1,928 unique XML identities**, exact parity |
| Packaged artifact parity | **811 / 811** compiled class entries byte-identical; exact V001–V020 package set and bytes, V020 once; JAR SHA256 `c903d78d38c9fd82a37abb31502ef372dc9d46e201068227c637d7ba71483bbe` |
| Frozen input byte check | **33 files** byte-identical: V001–V019, all 10 OpenAPI files, and docs12/14/15/17; V020 remains the sole approved additional migration |
| Current workflow HTTP smoke | **212 / 212 probes pass**, including both web/error-path configurations and default startup smoke |
| Current workflow firewall | **116 / 116 adversarial probes pass** |
| `git diff --check` | Pass; configured line-ending conversion warnings only |

Successful evidence is retained in local `/tmp` logs: `/tmp/pa42-code-review-fix2-retry1-guards.log` (12,701 bytes; SHA256 `f0c0071bd09c7eddc1b8c56c6bbc3416e5f5a0b2ac390a0ad11a1f1c4373a6e5`), `/tmp/pa42-code-review-fix2-retry1-full.log` (1,897,993 bytes; SHA256 `b1505b95a6b2949a259c820bcf59b95f2cad0413acbfaa27bdc5c0e388def6d0`), `/tmp/pa42-code-review-fix2-retry1-workflow-identity.log` (1,155 bytes; SHA256 `f415e075942cf47d10b332eee6e5585f7d7b121404bbb20a3aa8e4320cd0b0b5`), `/tmp/pa42-code-review-fix2-retry1-packaged-parity.log` (292 bytes; SHA256 `8343eeee46fcb9358d5efd2db6b4ae710d7311d1ffd7376d811258502db06abc`), `/tmp/pa42-code-review-fix2-retry1-frozen-bytes.log` (108 bytes; SHA256 `15ffda347f0b4ae5cf9ce598bfacb4a5b84bb94322fc5ecd8bbf5a49724163ec`), `/tmp/pa42-code-review-fix2-retry1-jar-smoke.log` (18,477 bytes; SHA256 `5589ef3dec5dc78eef3de5617939b59a38ba2cbbb2205a3e1a5c685bfa508394`), and `/tmp/pa42-code-review-fix2-retry1-firewall.log` (534 bytes; SHA256 `981d7f7af1b485a8f7906a8177949d6bd87bc0bee66eb0724cf072593704c150`).

The §10 **134 / 4 / 0 / 0** run and its four stale-pin failures remain a truthful historical record; the run is superseded by this independently reconciled **138 / 0 / 0 / 0** result, not erased or misrepresented. No production code, migration, frozen input, CI/POM inventory, identity, plan/control or private helper was changed for this retry. **No remote action occurred.**

## 12. Final code-review re-review fix3 continuation

Task: `pa42-code-review-fix3-gpt-20261126-085212-continuation`, 2026-11-26 (Asia/Shanghai). The retained fix3 retry's F-01 tests and F-03 historical-artifact clarification are preserved. This continuation corrects the rejected F-02 pre-rollback expectation and refreshes final evidence only.

### Review artifacts and authority

| Review | Task ID | Artifact / raw SHA256 |
| --- | --- | --- |
| Independent A | `pa42-code-review-rereview-a-ds-20261126-070607` | `docs/reviews/code-review-20261126-070959.md`; `e67f133186703fb99637dc3cd7e32160f80bc29bea597171743d024844258e29` |
| Independent B | `pa42-code-review-rereview-b-qwen38flash-20261126-070607` | `docs/reviews/code-review-20261126-072114.md`; `aab9c183319457618a430df114ad644b1856bf5849fb95531a5daa8f49be72a8` |

The authoritative Control remains raw-byte SHA256 `fe133f7cb269df4f25ad7c202d087043f5beedc7904eb5c5a68609333f54c81d`; the accepted plan remains `e39f31a8bf071d1a13b753cd54cc636f7c4aebd4f0e5ee7f39308c36707a940f`. Neither is edited.

### Finding disposition and mutation-sensitive evidence

- **F-01 — fixed, retained.** Existing `validatesStoredSemanticsHashThenProjection` now injects a specific `OrderCreationException` through the private semantics seam and requires `InvalidSnapshotException.getCause()` to be the same object. A separate non-semantic `IllegalStateException` canary must escape unchanged and must not become `InvalidSnapshotException`. Existing `corruptSnapshotAndRowFactsMapDetail404` separately requires an unexpected snapshot-reader runtime failure to map to `CustomerContextException.Code.INTERNAL`, while the known invalid-snapshot path still returns empty. The two unchanged method inventories total **13 / 0 / 0 / 0** in the fresh focused run. An isolated JShell JVM additionally redefined classes **in memory only** and executed the same 13 JUnit identities: broadening the snapshot catch to `RuntimeException`, discarding the semantic cause, and swallowing service runtime failures as empty/404 each produced **13 run / 1 intended assertion failure / 0 skipped**. The first two failed `validatesStoredSemanticsHashThenProjection`; the third failed `corruptSnapshotAndRowFactsMapDetail404`. Unaltered and restored baselines each passed **13 / 0 / 0 / 0**. No repository source, target class, report XML, packaged byte or the separate full-suite JVM was modified by this diagnostic. `/tmp/pa42-code-review-fix3-continuation-in-memory-mutations.log`: **3,488 bytes**, SHA256 `d4cd1bd751504bb09b6f4835b207feb27edb75e6130cfd4824d70baf8e4e8806`.
- **F-02 — fixed under Controller adjudication.** The same runtime connection attempts `GRANT SELECT(id) ON catalog_order.customer_order TO PUBLIC` as a non-owner without grant option. PostgreSQL **16.16** returns the non-erroring warning/no-op result: `has_column_privilege(0, ..., 'id', 'SELECT') = 'f'` **before** rollback, not `'t'`. The assertion now fixes that actual result with explanatory wording. It fails if PUBLIC ever receives the privilege or the attempted operation instead errors. The separate `'f'` assertion **after** `connection.rollback(publicGrant)`, admin-side `pg_attribute.attacl`/`aclexplode` PUBLIC-absence assertion, and exact `current449Oracle()` equality all remain intact. The final privileges run passes **10 / 0 / 0 / 0**.
- **F-03 — fixed, retained and clarified.** Section 5 explicitly labels its original JAR historical/superseded; §11 is the prior successful retry artifact, and the final continuation artifact is identified below by this build's own SHA256. Byte parity of all compiled entries/resources is the invariant; deterministic whole-JAR reproduction is not asserted.

The previous retry's pre-rollback **`'t'` attempt really failed**: privileges **10 / 1 / 0 / 0**, expected `"t"` but actual `"f"` at `CustomerOrderReadPrivilegesIT:160`, ending at **2026-11-26 08:49:49 +08:00**. That is evidence against the positive-grant premise, not a product failure. Its original command output is extracted without rerunning or relabeling it in `/tmp/pa42-code-review-fix3-continuation-rejected-positive-oracle-1.log` (**463,831 bytes**, SHA256 `7515dca54e683ed0ef6c646aff91eb58f21f3e99872029dd47f4682245c359ba`). The original prior-run transcript is **2,329,711 bytes**, SHA256 `d435f98303f7f24609279d4fe4b13d7540854ac2d2ed9df70f2009ba56156e1e`. The previously interrupted full attempt is not presented as completed or green.

### Final validation, in required order

All Maven commands use `/opt/maven-3.9.9/bin/mvn -B -ntp` from `backend/` with `JAVA_HOME=/opt/jdk21`, the supplied `GIT_DIR` and `GIT_WORK_TREE`. Totals are tests / failures / errors / skips; overlapping phases are not summed as unique tests.

| Validation | Final result | Local evidence (`/tmp/pa42-code-review-fix3-continuation-` prefix) |
| --- | --- | --- |
| Two focused unit classes, `-Dtest=CustomerOrderSnapshotReaderTests,CustomerOrderQueryServiceTests test` | **13 / 0 / 0 / 0**, BUILD SUCCESS, 08:54:11 +08:00 | `focused.log` |
| `-Pcontainer-smoke -Dit.test=CustomerOrderReadPrivilegesIT verify` | Surefire **1,164 / 0 / 0 / 0**; privileges **10 / 0 / 0 / 0**, BUILD SUCCESS, 09:02:30 +08:00 | `privileges-it.log` |
| Exact 20-class architecture/module/PA25 guard command in §4 | **138 / 0 / 0 / 0**, BUILD SUCCESS, 09:03:47 +08:00 | `guards.log` |
| Mandatory `-Pcontainer-smoke clean verify 2>&1 \| tee /tmp/pa42-code-review-fix3-continuation-full.log` | Surefire **1,164 / 0 / 0 / 0**, 122 classes; Failsafe **764 / 0 / 0 / 0**, 46 classes; BUILD SUCCESS, **10:27:55 +08:00** | `full.log`, `full-totals.json`, `fresh-xml.log` |
| Actual current-workflow source=CI=POM=fresh-XML checker | **122 / 1,164 + 46 / 764**, exact identities; **1,929** annotation hits, **1,928** unique XML identities; 46 exact POM includes; historical reconstructions retained | `workflow-identity.log` |
| Final clean-build packaged parity | **811 / 811** complete compiled entries byte-identical, no duplicates; exact **20** packaged migration paths/bytes, V020 once; **23 / 23** source resource bytes exact | `packaged-parity.log`, `packaged-parity.json`, `openapi-parity.log` |
| Actual current-workflow default + both web/error-path smokes | **212 / 212 HTTP probes pass**; default startup passes; child JVMs cleaned up | `jar-smoke.log` |
| Actual current-workflow production dependency/JAR firewall | **116 / 116 adversarial probes pass**; baseline37/34, SSM72/65, counterparts64, dependency-only8, packager1 | `firewall.log`, `runtime-dependencies.log` |
| Frozen-byte and scope checks | **33 / 33** raw-byte equal; all 85 product-scope paths plus plan/Control unchanged since continuation preflight | `frozen-scope-manifest.log` |
| `git diff --check` | Exit **0**; configured LF→CRLF conversion warnings only | `diff-check.log` |

The mandatory clean full command began at **09:03:47 +08:00** and exited naturally after approximately **84 minutes**. No shell timeout, kill or interrupt was applied. Both long ACL scans were left in flight; their silence was not treated as a failure. All **168** XML files have mtimes at or after the saved clean-run start epoch, and every test count, method identity, failure/error/skip total was independently checked. The final full-run PA42 reads and privileges classes pass **16 / 0 / 0 / 0** and **10 / 0 / 0 / 0** respectively.

Final executable artifact: `backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`, **40,192,267 bytes**, raw SHA256 **`90317944dbb8f47fb3dbf131ad91362a530fa04bf240fbe7421761d6a2aafde8`**. This supersedes §5/§11 for the final continuation build; its different whole-JAR hash is not evidence of changed production classes. The full `BOOT-INF/classes/` path set equals the complete 811-entry compiled set, and every entry is byte-identical. All V001–V020 migrations and all three application YAML resources equal source bytes. The ten `api/openapi/` contract/tooling files remain frozen raw-byte exact; this application's 23-resource inventory does **not** package OpenAPI files, and the exact packaged set proves no unapproved OpenAPI/resource entries were added. Whole-JAR reproducibility is **not claimed**.

All three checking programs were freshly extracted from the current `.github/workflows/ci.yml`, without editing CI or the programs: identity **176,063 bytes / `f4376c86ee73b19550f3b51e68cb561932a0eb14b4932ecede53dabce6163e46`**; JAR smoke **12,261 bytes / `1ec05cf38bbdf61b59463cd720ecda83d00f088785765e7d60dd267c1a8eed42`**; firewall **24,364 bytes / `d850e23541b1032dc2ecb4b331c68dfad95676fa3d9cdc47e215d66e092cc886`**. The actual extracted workflow programs, not substitutes, produced the results above.

### Final evidence hashes

All following files use the local `/tmp/pa42-code-review-fix3-continuation-` prefix. They are runner evidence, not new repository artifacts.

| File suffix | Bytes | Raw SHA256 |
| --- | ---: | --- |
| `focused.log` | 5,083 | `4dbd892cc986400f08756e6fe6f88d1f1d34cb0b7381941c1a808a335786c11d` |
| `privileges-it.log` | 462,514 | `60f654564690e2c93d0f73dc04d337454bcf7b4a794d7a35d7e6328d1bc99723` |
| `guards.log` | 11,266 | `b7a65550d0dd5f87e2f94c8810b3771e64db2835bbbe6f9523ed38d596b5530e` |
| `full.log` | 1,889,085 | `c286712cdaecf7e4efbbef33ed36954d0d96f083c1f8e60eb7b4375d7b7f17da` |
| `full-totals.json` | 257 | `6c51742f77badc108add6766dde60d2c40f463a24890c88f8049bac1a51db56f` |
| `fresh-xml.log` | 159 | `70e9ce829c2bfce6301be72018003a9f0d733fb9a761d48a14f8d3192d971ffc` |
| `workflow-identity.log` | 1,155 | `f415e075942cf47d10b332eee6e5585f7d7b121404bbb20a3aa8e4320cd0b0b5` |
| `packaged-parity.log` | 381 | `259d86bc55388e159bfecd7fa38e46e9730970ccb0a646134a3b8e7b909e133b` |
| `packaged-parity.json` | 249 | `d6c6e87952ce379ffd74a7d81e5d4ec4f3fdf86e0df12d866731fac9dd77cc19` |
| `jar-smoke.log` | 18,477 | `5589ef3dec5dc78eef3de5617939b59a38ba2cbbb2205a3e1a5c685bfa508394` |
| `firewall.log` | 534 | `981d7f7af1b485a8f7906a8177949d6bd87bc0bee66eb0724cf072593704c150` |
| `frozen-scope-manifest.log` | 335 | `b0fdb84117df14aaf7b32af649522cd57e43d3e986603e431f0815a05f1d24cd` |
| `openapi-parity.log` | 407 | `b8b3518dddbfb9acda144e6d1c49a852080e3539e41865dbabb1f8d8068b4e3a` |
| `diff-check.log` | 13,777 | `43257941ba57ac7bbb8d7e882c572ad9b11501eca3c26831cee819bc41cc49ed` |

### Revised frozen-membership manifest and handoff

Recomputed from **final bytes**, with the existing frozen membership, sorted LF-normalized `path hash\n` lines: `/tmp/pa42-code-review-fix3-continuation-final-manifest.txt` = **85 lines / 13,160 bytes**, SHA256 **`7fe9146dd238fb4fb970fe666103292a005d89e8f93cab4e67a48d66222627b5`**. Git membership remains exactly **16 new + 69 modified** relative to `origin/main`; no member was added or dropped. The report and Control are excluded from that pre-existing membership. Exactly the three authorized test paths differ from the re-review's frozen manifest; all other **82** members, including all production source/resources, V020, PA25, CI/POM and identity inventories, remain LF-byte-identical.

Authorized change set across the retained retry and continuation: `CustomerOrderSnapshotReaderTests.java`, `CustomerOrderQueryServiceTests.java`, `CustomerOrderReadPrivilegesIT.java`, and this implementation report. This continuation writes only the privileges test and report; retained F-01 test changes are not undone. No test method is added, removed or renamed. No production/main code, migration, frozen input, CI/POM identity, accepted plan or Control is edited.

**Blockers: none.** Final raw report line count, byte count and SHA256 are computed **after the last report edit** in `/tmp/pa42-code-review-fix3-continuation-report-integrity.log` and the final handoff response; the full-file checksum is deliberately external to avoid a self-referential checksum claim. This final section supersedes earlier artifact evidence without erasing any failed attempt.

**No staging, commit, push, PR, merge, approval, ready marking, reviewer request, external comment, gate advancement or other remote action occurred.** Production deployment acceptance remains NOT EXECUTED.

## 13. Aggregate review fix 1

Task: `pa42-aggregate-fix1-qwen38flash-20261126-114255`, 2026-11-26 (Asia/Shanghai). Accepted slice `7cd6fd2cb9ba04d548d92d5be5aaacd17febe2a6`; accepted-slice checkpoint HEAD `57ab763a005f1050904c15e44b5582f1dc7ac15c`; Control `docs/gateflow/project-execution-control.md` remains the authoritative **be097e7fa64b02094c9096c559e6c8ae378f581f014059c8ce0b98e693d3bd4d** and was not edited by this task.

### Provider and reviewer history

The routed GPT provider exhausted its prepaid quota immediately after completing §12, so this aggregate fix was executed by the Qwen3.8 Flash fallback implementation agent. An initial fallback pass applied the five authorized edits, recompiled the module and produced the focused **13 / 0 / 0 / 0** result plus an aborted full-surefire run; a second fallback pass continued the same task, found and corrected a defective §13 IT anchor (the positive control inherited a deliberately corrupted `repeat('b',64)` snapshot hash and therefore returned 404 instead of 200), and completed every remaining validation in required order. Both passes are recorded here; the intermediate failing anchor is not erased.

Aggregate review evidence, both re-hashed on disk before use:

| Review | Bytes | SHA256 | Acceptance status |
| --- | ---: | --- | --- |
| A `docs/reviews/code-review-20261126-105905.md` | 17,193 | `b256623cd8bb33d9a5fc75bcab88f7ef81d0518c56201cc2b64cfb98ffc94026` | authoritative for AGG-01/AGG-02 acceptance |
| B `docs/reviews/code-review-20261126-113238.md` | 29,564 | `d4f018571b17beb53bc12da0962de0022b84730699b6b34d21f0ee2af881e49b` | quarantined from independent acceptance because its glob printed peer lines; used only as corroborating technical evidence |

### Accepted finding mapping

| Finding | Accepted fix | Exact edits | Anchors |
| --- | --- | --- | --- |
| AGG-02 (review A F-01, Low) | fail-closed historical contract validation inside the stored-snapshot seam | `DefaultCustomerOrderSnapshotReader.java` only: frozen `Set.of("RECORDING_SECONDS","AI_SECONDS","EVIDENCE_COUNT","STORAGE_BYTES")` and `PRODUCT_CODE = "[A-Z][A-Z0-9_-]{1,63}"`; after semantic parse/hash validation and `CustomerOrderSemantics.projection`, every projected benefit `entitlementType` must be in the set and `productCode` must match the pattern, else the existing `InvalidSnapshotException` | `CustomerOrderSnapshotReaderTests.missingExtraMalformedAndMismatchedFactsAreRejected` (existing identity) |
| AGG-01 (review A F-02, Low) | direct test coverage for the row↔snapshot comparison chain and for plan §10 scenario 3/6 PostgreSQL anchors | `CustomerOrderQueryServiceTests.corruptSnapshotAndRowFactsMapDetail404`, `CustomerOrdersReadIT.stablePostgresUuidOrderAndPagingBoundariesAreExact`, `CustomerOrdersReadIT.corruptSummaryIs500AndCorruptDetailIs404WithNoWrites` (all existing identities) | four in-memory mutants below |

No test method was added, removed or renamed; no assertion was weakened and no skip was introduced. `CustomerOrderSemantics`, V004, V020, OpenAPI, CI/POM and every frozen input are unmodified: **33 / 33** frozen files remain raw-byte identical and **81 / 85** product-scope manifest members are LF-byte-identical.

### AGG-02 exact validation

The seam now rejects, with the pre-existing `InvalidSnapshotException` (no new exception type, no public-contract change), any projected historical snapshot that carries an `entitlementType` outside the frozen OpenAPI enum (`api/openapi/onecall-v1.openapi.yaml:5281,6038`) or a `productCode` outside the frozen 2..64-character `^[A-Z][A-Z0-9_-]{1,63}$` contract (the same regex V004 enforces in `ck_catalog_product_code`, and the same 2..64 length OpenAPI declares). Because the service maps `InvalidSnapshotException` to `Optional.empty()`, both rejections surface as customer-visible 404 rather than leaking an out-of-contract projection.

In `missingExtraMalformedAndMismatchedFactsAreRejected` the added cases recompute each snapshot's own SHA-256 so the new checks are reached instead of the hash guard: all four frozen entitlement types are accepted as positives, `AI_SECONDS`→`EVIL` is rejected, a one-character `productCode` `"X"` is rejected, and the two-character `"XY"` lower boundary is accepted.

### AGG-01 anchors

- Unit (`corruptSnapshotAndRowFactsMapDetail404`): from one fully agreeing row/snapshot pair, six **one-at-a-time** snapshot mismatches (`catalogVersionId`, `catalogOfferId`, `channel` `WEB`→`MINIAPP`, `amountCents` `123`→`124`, `currency` `CNY`→`USD`, `snapshotSha256` `a×64`→`b×64`) each map detail to empty/404. each iteration clears prior invocations and then `verify(snapshots).read("stored", "a".repeat(64))` proves the seam was consulted with the row's own stored snapshot and hash, so every case had already passed the purchase-consent, payment-shape and issuance gates and the row↔snapshot comparison - not an earlier short-circuit - produced the 404; a fully agreeing control then asserts presence, and the pre-existing `InvalidSnapshotException`/`IllegalStateException` canaries still map to 404/500.
- IT detail-vs-list anchor (`corruptSummaryIs500AndCorruptDetailIs404WithNoWrites`): a **legal** DB row whose stored snapshot is hash-consistent (detail 200 positive control proves the seam re-validation passes; `md5(snapshot::text)||'|'||snapshot_sha256` is asserted invariant across the mutation) is rewritten so only `amount_cents` disagrees (`124|123` in DB facts). Detail becomes 404 while list stays 200 and still projects the row amount `124`; the personal row count is asserted `1` and metadata inventory is unchanged.
- IT cursor anchor (`stablePostgresUuidOrderAndPagingBoundariesAreExact`): the fixed five-order fixture traversal is derived from PostgreSQL itself (`string_agg(id ORDER BY created_at DESC, id DESC)`). Traversal A pages `limit=2`, then continues the same cursor at `limit=4`: first page SQL has no seek clause and exactly `ORDER BY o.created_at DESC, o.id DESC LIMIT ?`; second page SQL has exactly one keyset seek `(o.created_at, o.id) < (CAST(? AS timestamptz), CAST(? AS uuid))`, no `OFFSET`, exactly one order SELECT and one issuance SELECT; `hasMore` goes `true`→`false` and `nextCursor` non-null→null; the union is `doesNotHaveDuplicates().containsExactlyElementsOf(expected)`. Traversal B then deletes the page-1 anchor row and inserts an older row between requests, asserting the fixed traversal still yields exactly six unique members (`expected[0]`, anchor, `expected[2]`, `expected[3]`, `expected[4]`, inserted) with `hasMore` `true`→`false` and a null tail cursor - no duplicate and no skip.

### In-memory mutation evidence

Mutants were compiled into `/tmp/pa42-aggregate-fix1-mut/<id>/out`, applied with `ByteBuddyAgent` + `Instrumentation.redefineClasses` inside a disposable JShell JVM and restored from `target/classes`; repository source, `target/` bytes and surefire/failsafe artifacts were never edited. Each mutant was first verified to keep an identical JVMTI member set (`javap -p -s` diff empty), so redefinition is legal and no member was added or removed.

| Mutant | Single behavioural change | Result |
| --- | --- | --- |
| M1 seam domain checks deleted | AGG-02 entitlement/product-code guards replaced by a never-true condition | `13 / 12 / 1 / 0`; the one failure is exactly `missingExtraMalformedAndMismatchedFactsAreRejected` ("Expecting code to raise a throwable") |
| M2 row↔snapshot comparisons deleted | six field comparisons removed, `snapshot == null` guard retained | `13 / 12 / 1 / 0`; the one failure is exactly `corruptSnapshotAndRowFactsMapDetail404` on the first mismatched snapshot, proving the comparison chain - not a gate - is load-bearing |
| M3 keyset seek neutralised | seek clause kept at identical bind arity but `OR 1=1` appended | `16 / 14 / 2 / 0`; failures are `stablePostgresUuidOrderAndPagingBoundariesAreExact` and the pre-existing `tiedTimestampsUseUnsignedPostgresUuidOrderAcrossPages`, both 500/INTERNAL because ignoring the cursor re-selects rows the monotonicity guard rejects |
| M4 `hasMore` boundary weakened | `rows.size() > effectiveLimit()` replaced by `>=` | `16 / 15 / 1 / 0`; the single failure is exactly `stablePostgresUuidOrderAndPagingBoundariesAreExact` "Expecting value to be false but was true", i.e. the added tail `hasMore`/null-cursor assertions |

Baselines in the same harness: the two focused classes `13 / 13 / 0` before the unit mutants and `13 / 13 / 0` again after restoring them, and unmutated `CustomerOrdersReadIT` `16 / 16 / 0` in each probe JVM; the later clean full builds re-ran the same 16 container methods green, so no mutant byte survived its JVM. Raw logs: `/tmp/pa42-aggregate-fix1-in-memory-mutations-unit.log` (1,483 bytes; SHA256 `02acaf590900b6c6598eda2e1807740d938740ce334a40eafe08256c1907e65e`) and `/tmp/pa42-aggregate-fix1-in-memory-mutations-it.log` (230,225 bytes; SHA256 `9b7b5af3af931ce9d76674e481a01a0e199a6e0686753d4388bc32336f21498a`).

### Aggregate deterministic-pin drift caused by the authorized AGG-02 edit (blocker)

Three guard literals hash the **entire** `src/main/java` + `src/main/resources` tree, so the authorized production edit necessarily invalidates them; those three guard files are not in this task's allowed-write list and were not touched. `/tmp/pa42-aggregate-fix1-pin-diagnosis2.py` reconstructs each manifest exactly as the guard computes it (per-path `path + ' ' + sha256(LF-normalized bytes)`, sorted, joined with a trailing LF) and proves three facts: at checkpoint HEAD the reconstruction reproduces all three pinned literals, with final bytes it reproduces the three observed failures, and the **sole** differing entry in every manifest is `backend/src/main/java/com/onecall/core/catalog_order/internal/customerorder/DefaultCustomerOrderSnapshotReader.java` (seam `33633c742a99021c…` → `96ed7333d8bb50ce…`).

| Manifest (entries) | Guard identities | Pinned / HEAD-reproduced | Final bytes |
| --- | --- | --- | --- |
| `src/main/java` + `src/main/resources` excluding V019/V020 (353) | `CustomerCatalogOfferReadAclBoundaryTests.frozenContractsAndV001ThroughV018RemainByteIdentical:95` and delegating `CustomerOrderCreationBoundaryTests.frozenContractsMigrationsAndPa25RemainByteIdentical:87` | `93764e87e85a318753f98a20dfaaa46bde76a0e6e4113cd856ca1172b03cdbae` | `ad4662b82014ee9a79c9342be68102c8a26f3041e82c46620c17addf702b7898` |
| same tree with no exclusions (355) | `CustomerCatalogOfferReadAclBoundaryTests.migrationOnlyAddsOnePackagedResource:130` | `7d7346e43c1c6d744ec803e4746d9b33b42ca33aa7e89c8beff317cc0cf3910d` | `d722d31a2c0a35e26243ab40380464c94d15edbfc769c72567fa1e07ad844433` |
| excluding V018/V019/V020 (352) | `EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen:102` | `9bb26c371439c984615b885ef1f854e7a86cc561176b693646ba6108c2568d79` | `8f4ca11a2270169fda716622238e18ea0f4af64e943c7c1e2cf672bb39951818` |

This is exactly the §10 → §11 precedent: the only admissible remedy is a Controller-authorized **pin-only** update of the three literals above in the three guard files (no assertion weakened, no other byte changed). Until that authorization exists, required validations 3 and 4 cannot reach zero failures, and the green-only workflow identity script cannot certify the run. All other **134** guards and all **1,160** remaining units pass with the AGG-02 edit in place.

### Final validation, in required order

| Validation | Result | Evidence |
| --- | --- | --- |
| 1. Focused `CustomerOrderSnapshotReaderTests,CustomerOrderQueryServiceTests` | **13 / 0 / 0 / 0**, BUILD SUCCESS | `it-targeted.log`, `focused.log` |
| 2. PostgreSQL `CustomerOrdersReadIT,CustomerOrderReadPrivilegesIT` (`-Pcontainer-smoke`) | reads **16 / 0 / 0 / 0**, privileges **10 / 0 / 0 / 0**, zero failures/errors/skips | `it-targeted.log`, `read-it-rerun.log` |
| 3. Exact 138-test architecture/module/PA25 guard command (§4 line 74 class list, sanctioned Maven) | **138 / 4 / 0 / 0**, BUILD FAILURE - the four failures are only the stale aggregate pins above; no other guard regressed | `guards.log` |
| 4. Mandated `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke clean verify` | Surefire **1,164 / 4 / 0 / 0** (122 classes), Failsafe **not reached**, BUILD FAILURE in 06:31 min; required **1,164 / 0 / 0 / 0** + BUILD SUCCESS is unattainable without the pin-only authorization | `full.log`, 410,417/`eddc64992179da51b048969fb7f4fa0d4e7bdf17ec17d778ae8e72f196a44d49` (2,328 lines) |
| 4b. Diagnostic `-Pcontainer-smoke -Dmaven.test.failure.ignore=true clean verify` | Surefire **1,164 / 4 / 0 / 0**; Failsafe **764 / 0 / 0 / 0** across 46 classes; BUILD SUCCESS in 01:22 h | `full-diagnostic.log`, `full-totals.json` |
| 5. Workflow identity | verbatim `workflow-identity.py` exits non-green on the four documented pin identities; the byte-identical adjacent copy proves **122 / 1,164** + **46 / 764** = **168 / 1,928**, 1,929 annotation lines, 1,928 unique XML identities, **46** POM includes, zero skips/errors | `workflow-identity.log`, `workflow-identity-adjacent.log` |
| 5. Packaged parity | **811 / 811** complete compiled class entries byte-identical with no duplicates; exact **20** packaged migration paths/bytes with V020 once; **23 / 23** source resource bytes exact; final build's own JAR is 40,192,842 bytes with SHA256 `15bf8f05d7cf97e605338f1d13e8d0154c13d0c978e3611581828e7a5abcf58a`, which is the artifact the HTTP and firewall probes exercised | `packaged-parity.log` |
| 5. HTTP smoke | **212 / 212** probes pass (default startup plus both web/error-path configurations), all problem bodies shaped `type/title/status/code/traceId` with unique trace IDs and no leaked material | `jar-smoke.log` |
| 5. Firewall | **116 / 116** adversarial dependency/packaging probes pass (`baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1; deltas35/0 and 31/0`) | `firewall.log` |
| 5. Frozen bytes/scope | **33 / 33** raw-byte equal; 85-path membership has exactly the 4 authorized differing members | `frozen-scope-manifest.log` |
| 5. `git diff --check` | pass; configured line-ending conversion warnings only | `diff-check.log` |

### Revised manifest and report integrity

The revised **85-path** product/bookkeeping manifest, recomputed from final LF-normalized bytes with the unchanged frozen membership, is `/tmp/pa42-aggregate-fix1-final-manifest.txt` = **85 lines / 13,160 bytes**, SHA256 **`5904446ad87ae9731e0db9baaaad3e75fbdd58f88427db9e559131e6de7be689`**. No member was added or dropped; exactly the **4** authorized aggregate-fix members differ from the §12 manifest (`DefaultCustomerOrderSnapshotReader.java`, `CustomerOrderSnapshotReaderTests.java`, `CustomerOrderQueryServiceTests.java`, `CustomerOrdersReadIT.java`) and the other **81** are LF-byte-identical. Scoped to those same 85 paths `git diff --name-status origin/main` still reports exactly **16 added + 69 modified = 85**, so no member entered or left the product scope; this report and Control stay outside that membership, and `git diff --name-status` from the accepted-slice checkpoint `7cd6fd2cb9ba04d548d92d5be5aaacd17febe2a6` shows only the six locally modified files (the five allowed paths plus the pre-existing Control edit). Recomputation is deterministic from final LF-normalized bytes with the existing frozen membership; whole-JAR reproducibility is **not** claimed. Report line/byte/SHA256 after the last edit are recorded externally in `/tmp/pa42-aggregate-fix1-report-integrity.log` and in the handoff response, because a file cannot honestly contain its own final checksum.

**Blockers:** the three stale aggregate pin literals described above (353/355/352-entry manifests) require a Controller-authorized pin-only update in three guard files outside this task's allowed writes; until then required validations 3 and 4 and the green-only identity script cannot pass. No scope drift, no identity/count change, no frozen-byte change and no other failing required test was found.

**No staging, commit, push, PR, merge, approval, ready marking, reviewer request, external comment, gate advancement or other remote action occurred.**

### Final local evidence ledger

| Evidence file (`/tmp/pa42-aggregate-fix1-` prefix) | Bytes | Raw SHA256 |
| --- | ---: | --- |
| `focused.log` (initial fallback pass) | 3,648 | `44c6855e7c715d1542c4190d107e7fd09fbda6d6bad186fbd3e985faa62b0c7a` |
| `read-it.log` (initial fallback pass, full surefire abort) | 405,162 | `5c3af9450497f6962964f34423fd06dd0c382764025ffecc4e71bd882141a556` |
| `it-targeted.log` (13 units + privileges 10 + reads 16/1 defective anchor) | 114,179 | `05d6345ec4254e1eec25cf1393fdb249acc4190e045b9da3dd0b83db9ee9bab1` |
| `read-it-rerun.log` (corrected anchor, reads 16 / 0 / 0 / 0) | 55,899 | `c13d25704eccd8c7d2f00a483d99e28e0c80992952fe41bc9cbcdf4ebf708d4f` |
| `pin-drift.log` (three-manifest HEAD reproduction and sole-delta proof) | 1,587 | `7d58b2296afa62b82da1412900d193d705eda3f3539050b9f0d9e7215203a421` |
| `guards.log` | 15,508 | `bea805cff98ebef7df7170d2295e0152a153016a793bf9ef75616433bf9d040a` |
| `full.log` (mandated exact command) | 410,417 | `eddc64992179da51b048969fb7f4fa0d4e7bdf17ec17d778ae8e72f196a44d49` |
| `full-diagnostic.log` | 1,901,729 | `e960a89988155a5aa4af58449de2bdbf48b248fb62d72661421edb988818474d` |
| `full-totals.json`, `fresh-xml.log` | 852 / 717 | `7b9e376a3db3f685f30653704f6b4869287c92a51d2fa2d7f9cfe83b3dea5488` / `06e1b1657ee88f604ceaabb773d55dcb403c3c2679310b5d8267364737aa7e3f` |
| `workflow-identity.log` (verbatim, non-green) | 66 | `9cbd5d51d4d659a73d9a301ae01cb20a92b3ec443109808b522e02e52ccc1a8c` |
| `workflow-identity-adjacent.log` | 1,226 | `94739f0781e6f28fa549a5abf208549ed15a852d8dbde0194f5e0599f788e998` |
| `packaged-parity.log`, `packaged-parity.json` | 381 / 249 | `cceec2ad8b98605b5d9f30225aa42d77817c67cf9e254715fc6ab689b41e0389` / `c449e5e1bd3856ba2d339fcc9b779bebf1ace894ba88447813960e7707232ec9` |
| `jar-smoke.log` | 18,494 | `956b89f7e846dab69f57b57322411af23f44f96838a01d9001f7941909920dff` |
| `firewall.log`, `runtime-dependencies.log` | 550 / 705 | `36da837b113f9d6020881386c75bf754261089b8931cce61f3e513e736b9676f` / `c9bb695d4eddef8f1f4538c1f5ae32ce38b0be15a8650441b9a7ad738385fab4` |
| `in-memory-mutations-unit.log`, `in-memory-mutations-it.log` | 1,483 / 230,225 | `02acaf590900b6c6598eda2e1807740d938740ce334a40eafe08256c1907e65e` / `9b7b5af3af931ce9d76674e481a01a0e199a6e0686753d4388bc32336f21498a` |
| `frozen-scope-manifest.log`, `diff-check.log`, `final-manifest.txt` | 444 / 0 / 13,160 | `c19897157ca0fb352269af86ba67a2261af05776c3ffce9350b416e27d78b263` / `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` / `5904446ad87ae9731e0db9baaaad3e75fbdd58f88427db9e559131e6de7be689` |

Only the five allowed paths changed in this task: the seam production file, the three test classes above and this report. No other production, test, bookkeeping, control or review path was written; the three guard files holding the stale aggregate pins were deliberately left untouched.

### Controller-authorized pin finalization — resolved November 26, 2026

Task **`pa42-aggregate-fix1-pin-gpt-20261126-140825`** supersedes only the unresolved pin-blocker disposition above. The earlier failed guard/full runs, diagnostic failure-ignore run, adjacent identity diagnosis, prior artifact hash and prior manifest remain historical evidence, not the final acceptance run. Controller explicitly authorized the three hash-literal replacements; no list, exclusion, count, identity, delegation or assertion strength changed.

The only repository paths written during this pin-finalization task are:

- `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java` — two hash literals.
- `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` — one hash literal.
- `docs/gateflow/pa42-customer-order-read-and-unknown-recovery-foundation-implementation.md` — this §13 continuation.

`CustomerOrderCreationBoundaryTests.java` has no duplicate old literal and was not edited; its existing delegation passes with the updated catalog assertion. The aggregate implementation and its tests were left untouched. Production, V020, PA25, CI/POM, plan, Control, review artifacts and private prompts were not written. Control remains raw SHA256 **`a354a7daf210ee3b04f724b352961789a067bd6e7c881cb3d0d0b885e62c919b`**.

#### Independently reconstructed pin evidence

Before applying replacements, every file in `backend/src/main/java` and `backend/src/main/resources` was compared against its actual checkpoint HEAD bytes, not merely an assumed historical seam substitution. After the replacements and final build, the same manifests were recomputed from final bytes. Each row uses the guard's exact existing path representation, LF normalization, exclusions, sorted `path + ' ' + SHA256` lines and trailing LF. Before/after final manifest bytes are identical; guard edits cannot enter these production/resource manifests.

| Exact existing manifest | Entries / manifest bytes | Independently reproduced checkpoint HEAD SHA256 | Final SHA256, identical before/after literal edits |
| --- | --- | --- | --- |
| Exclude V019/V020 | 353 / 55,041 | `93764e87e85a318753f98a20dfaaa46bde76a0e6e4113cd856ca1172b03cdbae` | `ad4662b82014ee9a79c9342be68102c8a26f3041e82c46620c17addf702b7898` |
| No exclusions | 355 / 55,325 | `7d7346e43c1c6d744ec803e4746d9b33b42ca33aa7e89c8beff317cc0cf3910d` | `d722d31a2c0a35e26243ab40380464c94d15edbfc769c72567fa1e07ad844433` |
| Exclude V018/V019/V020 | 352 / 54,888 | `9bb26c371439c984615b885ef1f854e7a86cc561176b693646ba6108c2568d79` | `8f4ca11a2270169fda716622238e18ea0f4af64e943c7c1e2cf672bb39951818` |

The **sole differing production entry** against HEAD in all three manifests is `backend/src/main/java/com/onecall/core/catalog_order/internal/customerorder/DefaultCustomerOrderSnapshotReader.java`; the other production/resource entries match. Evidence: `/tmp/pa42-aggregate-fix1-pin-before.log`, plus `/tmp/pa42-aggregate-fix1-pin-{353,355,352}-{before,after}.manifest`. Independently reversing exactly the three approved literal replacements restores both guard files' original **raw-byte** SHA256 values (`7a7d6d4dde967ec45593e5b21f67c5dcac3e7dad32483fdcd6fda9476c4c5a66` and `98340d9be68683498a12cfa53b2ef10faf34712e31053bde961ce79532dd2ab0`); even their pre-existing mixed CRLF/LF endings are preserved. Evidence: `/tmp/pa42-aggregate-fix1-pin-raw-literal-only.log`.

#### Final acceptance validations

All Maven commands ran from `backend` using `JAVA_HOME=/opt/jdk21`, `/opt/jdk21/bin` first on `PATH`, and `/opt/maven-3.9.9/bin/mvn`. The exact unchanged 20-class guard selection in §4 ran first. The mandatory full command was then `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke clean verify 2>&1 | tee /tmp/pa42-aggregate-fix1-pin-final-full.log`, under `pipefail`, with **no timeout, test filter, test-failure-ignore or kill**. It completed naturally from **2026-11-26 14:16:27 +08:00** to **15:41:21 +08:00** (Maven reports **01:24 h**); XML totals were independently checked after completion.

| Validation | Final result | Evidence (`/tmp/pa42-aggregate-fix1-pin-` prefix) |
| --- | --- | --- |
| Exact architecture/module/PA25 guards | **138 / 0 / 0 / 0**, 20 classes, BUILD SUCCESS | `final-guards.log`, `final-guard-totals.log` |
| Unfiltered clean full Surefire | **1,164 / 0 / 0 / 0**, 122 classes | `final-full.log`, `final-full-totals.json` |
| Unfiltered clean full Failsafe | **764 / 0 / 0 / 0**, 46 classes, **BUILD SUCCESS** | `final-full.log`, `final-full-totals.json` |
| Actual workflow identity | **122 / 1,164 + 46 / 764 = 168 / 1,928**; 1,929 annotation lines, 1,928 exact XML identities, 46 POM includes; zero failures/errors/skips | `final-workflow-identity.log` |
| Packaged/resource parity | **811 / 811** complete compiled entries byte-identical, no duplicate entries; **23 / 23** source resources exact; **20 / 20** migration paths and bytes exact, V020 once | `final-packaged-parity.json` |
| Final-build JAR HTTP smoke | **212 / 212**, actual default startup and both web/error-path configurations pass | `final-jar-smoke.log` |
| Final-build dependency/packaging firewall | **116 / 116** adversarial probes; baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1; deltas35/0 and31/0 | `final-firewall.log`, `final-runtime-dependencies.log` |
| Frozen inputs / scope | **33 / 33** raw-byte equal; unchanged 85-path membership; exactly two authorized pin guard members differ from the aggregate-fix manifest | `final-frozen-scope-manifest.log` |
| `git diff --check` | **pass**, exit 0; only configured LF-to-CRLF conversion warnings | `final-diff-check.log` |

Identity, HTTP and firewall Python were extracted **verbatim from the actual current `.github/workflows/ci.yml`**, not the diagnostic adjacent copy: extracted script SHA256 values are respectively `f4376c86ee73b19550f3b51e68cb561932a0eb14b4932ecede53dabce6163e46`, `1ec05cf38bbdf61b59463cd720ecda83d00f088785765e7d60dd267c1a8eed42` and `d850e23541b1032dc2ecb4b331c68dfad95676fa3d9cdc47e215d66e092cc886`. The clean build's own final JAR, used by all subsequent package/HTTP/firewall checks, is **40,192,842 bytes**, SHA256 **`20a3d3e57ca290180ab86181ec78e5bc0ce57cf279ae7b5e832f9fe463890b1e`**. No whole-JAR reproducibility claim is made.

#### Final manifest and evidence integrity

The final manifest is `/tmp/pa42-aggregate-fix1-pin-final-manifest.txt`: **85 lines / 13,160 bytes / SHA256 `623e04f24d0fdedde5b87140caf7603e51f0d1949b184d5f90d89993b713bfba`**, recomputed from final LF-normalized bytes using the unchanged frozen 85-path membership. Relative to the aggregate-fix manifest `5904446ad87ae9731e0db9baaaad3e75fbdd58f88427db9e559131e6de7be689`, only the two authorized pin guard members differ; **83** members are unchanged. Relative to §12, the four aggregate implementation/test members plus those two guards differ; **79** members are unchanged. Scoped `git diff --name-status origin/main` remains **16 added + 69 modified = 85**; this report and Control remain outside the manifest.

| Final evidence (`/tmp/pa42-aggregate-fix1-pin-` prefix) | Bytes | Raw SHA256 |
| --- | --- | --- |
| `before.log` | 980 | `a37097297afb088949d1efeac698d93fb2573fa3761e60b42099bb80203e9540` |
| `raw-literal-only.log` | 496 | `352e6d95a62608b9da155cd386881233f3312f26ac11c363a1a4a657349818d7` |
| `final-guards.log` | 12,701 | `a9aa03b45bf9ca310962bf48ffea2ed8675d2154d37ca4e60663a031cefe2ca1` |
| `final-full.log` | 1,898,013 (10,353 lines) | `e74f30056f3b80915f6c70b4ce339eb2a67d2e8121806961cba0dfa070c470dc` |
| `final-full-totals.json` | 203 | `e3159d55531d8dbc1c945247a9327db0eafddbe523650466ede7f6f44413b6da` |
| `final-workflow-identity.log` | 1,155 | `f415e075942cf47d10b332eee6e5585f7d7b121404bbb20a3aa8e4320cd0b0b5` |
| `final-packaged-parity.json` | 249 | `a51df64a059bd2d74048fbe7bc9073056f3dbca99eee7764d8c5a0a481862fd0` |
| `final-jar-smoke.log` | 18,477 | `5589ef3dec5dc78eef3de5617939b59a38ba2cbbb2205a3e1a5c685bfa508394` |
| `final-firewall.log` | 534 | `981d7f7af1b485a8f7906a8177949d6bd87bc0bee66eb0724cf072593704c150` |
| `final-runtime-dependencies.log` | 682 | `fcba228231c32ec4ab000c1abe727fbbe877b1d2fee954db1b2f95c4f333a2de` |
| `final-frozen-scope-manifest.log` | 1,554 | `158b955e7d38d30732080bec71bcba61237c6829ed1b4332278003d6ed8094cb` |
| `final-diff-check.log` | 1,500 | `cfe2480d1a1fb027b19e11b615ab6d6323641d3318b5c23f2e632e66aefb4474` |
| `final-manifest.txt` | 13,160 | `623e04f24d0fdedde5b87140caf7603e51f0d1949b184d5f90d89993b713bfba` |

Final report line/byte/SHA256 is measured **after the last report edit**, recorded externally in `/tmp/pa42-aggregate-fix1-pin-final-report-integrity.log` and the final handoff; embedding its own final checksum in this file would be self-referential. The final post-report scope, manifest, artifact and whitespace recheck is recorded in `/tmp/pa42-aggregate-fix1-pin-final-post-report.log`.

**Final blockers: none.** The four stale-pin assertion failures are resolved without weakening any guard. Historical failures above are preserved. No additional path drift, identity/count change, frozen-input change or validation failure occurred. A pre-build local scope helper initially used `backend` as `GIT_WORK_TREE`, causing only its origin/main path-count assertion to fail; correcting the helper to the explicitly required repository-root worktree proved 16 + 69 = 85 before the full run and again afterward, with no repository change for that diagnostic. No commit, stage, push, PR, merge, approval, ready marking, reviewer request, external comment or other remote action was performed.
