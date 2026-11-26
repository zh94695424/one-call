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
