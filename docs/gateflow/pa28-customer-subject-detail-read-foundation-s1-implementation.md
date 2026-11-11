# PA28 Customer Subject-Detail Read Foundation — S1 Implementation

## 1. Authority, status and custody

- Work unit: `PA28-customer-subject-detail-read-foundation`; single composed slice S1; current gate remains `implementation fix 1`.
- Documentation closer: `pa28-customer-subject-detail-implementation-report-gpt-20261112-01`, 2026-11-12, Asia/Shanghai. This task creates only this report and `docs/gateflow/pa28-customer-subject-detail-read-foundation-s1-fix-1.md`; it makes no implementation repair or gate/remote mutation.
- Authority: active PA28 checkpoint in `docs/gateflow/project-execution-control.md`, accepted cumulative plan and plan-fix, Controller-authorized 51→52 path expansion for `PA28-S1-IMPL-01`, and this report/seal follow-up instruction. The accepted plan checkpoint is Control-attested `75e74c1c3d744c9fb833a2a95b4a78d69297b7b5`; no fresh Git verification is claimed in this metadata-free export.
- Completion status: **PASS — bounded local S1 implementation and documentation closure**, because final implementation execution and preservation already passed. This is not Code Review, Controller approval, production acceptance, or authority to advance a later gate.
- Evidence root `E`: `D:\codex-temp\pa28-implementation-evidence-20261111-02`, mounted at `/mnt/d/codex-temp/pa28-implementation-evidence-20261111-02`; final execution root `F = E/fix2`. Relative evidence names below are under `F` unless prefixed `E/`.
- `validation-disposition.json` truthfully records technical PASS but a blocked handoff with absent reports/seal. It remains immutable historical evidence. This report and its companion close that documentation/seal blocker only; they do not relabel failed prior runs. Final closure custody is `implementation-evidence-manifest.sha256` plus `report-closure-audit.json` in `F`.
- Actual outer runner exit, provider/model identity, JSONL validity, completion-event count and stderr remain Controller/runtime attestations, not observable or invented here. Earlier retry/fix metrics are not reused as this task's metrics.

## 2. Confirmed endpoint and exact scope

Only `GET /api/v1/customer/subjects/{subjectId}` / `getCustomerSubject` is implemented. Successful JSON has exactly four fields: `subjectId`, `activationStatus`, `evidenceCount`, `orderCount`. Counts are Java `long` / PostgreSQL bigint. Optional `numberMasked`, `trial`, `consentRequirements` are absent, including null-valued forms; `claimState` is not a schema property and is not added.

The flow reuses verified customer session/workspace, lifecycle repository, existing activation reader and existing problem writer. Three minimal owner seams supply subject ownership/risk, evidence count and workspace-scoped order count. Customer composes their APIs, not foreign SQL/internals. Qualified lazy service/persistence wiring preserves default/non-web inertness and avoids resolving pools for authentication or path-validation failures. Customer named-interface dependencies increase from six to nine; the actual flattened production graph has six edges, including both evidence edges, and remains Modulith-verified and acyclic.

Non-goals: no additional endpoint, frontend, primary-number choice, content/list/media response, risk internals, claim/activation/consent/trial mutation, entitlement/policy read, session issuer/TTL/WeChat mapping, cursor/key dependency, generic query/authorization framework, extra role/pool, cache, index/table/business DDL, OpenAPI change, V001–V015 change, deployment, credentials or live acceptance. No stage/commit/branch/push/PR/review/merge/approval/ready/reviewer request/external comment/issue/Control mutation occurs in this closer.

## 3. One composed transaction and access matrix

The endpoint-named customer transaction template is read-only, `PROPAGATION_REQUIRES_NEW`, `ISOLATION_REPEATABLE_READ`. All owner readers share the transaction-aware `customerQueryDsl` and the same customer connection/snapshot. The service suspends/restores ambient transactions, performs `SET LOCAL ROLE customer_iam_role`, verifies `current_user`, checks lifecycle, resolves ownership, checks all-binding OPEN risk, then reads activation and both counts. It does not alter a shared template at request time, use another pool, acquire request-path locks or perform business writes. Transaction/role state changes are not business writes.

| Condition | Exact behavior |
| --- | --- |
| Personal selected workspace | Subject `owner_account_id` must equal verified active account; organization-owned rows are not personal rows. |
| Organization selected workspace | Subject owner equals selected ACTIVE organization; active account has ACTIVE membership in `ADMINISTRATOR`, `MEMBER`, `EVIDENCE_MANAGER` or `AUDITOR`; all four may read. |
| Missing/inactive account, organization or membership, or invalid membership role | Existing `FORBIDDEN` / 403, before subject lookup. |
| Missing, unowned, foreign-account or foreign-workspace subject | Completed `Optional.empty()` → existing `CustomerProblemWriter.notFound` / uniform 404; no risk, activation or count reads. |
| OPEN risk on any ACTIVE, DISABLED or RETIRED binding of an accessible subject | Existing 403 before projections; no binding/no risk/CLEARED-only risk is not an invented denial. Foreign subjects remain 404. |
| Activation | Existing current singleton status: INACTIVE/ACTIVE/SUSPENDED; only a missing row defaults to INACTIVE, without insertion. Invalid/null projections are 500. |
| Evidence | All evidence statuses for accessible subject, without row contents; preceding subject ownership on the same snapshot supplies workspace authorization. |
| Personal orders | Same subject, matching purchasing `account_id`, and `owner_organization_id IS NULL`; all payment states count. |
| Organization orders | Same subject and selected `owner_organization_id`, including other purchasers in that organization; other workspaces excluded. |

Boundary precedence remains route/method closure 404 → bearer 401/verifier-unavailable 503 → malformed trusted workspace 503 → malformed canonical UUID 422 → persistence/role failures 503 → lifecycle 403 → ownership 404 → OPEN risk 403 → projections. Extra/duplicate query selectors and fixed/chunked GET bodies are ignored, never authority. Dotted/abbreviated UUID values are 422; extra path segments/trailing routes/non-GET remain 404. Null transaction/service/owner results are internal 500, not fabricated absence or zero. Transaction completion precedes both success and empty-result 404; completion failure yields sanitized 503 on either branch. Responses/problems/logs expose no secret or row content. Real HTTP/PostgreSQL tests prove no business writes, snapshot consistency, next-request rechecks and pooled role/read-only/isolation/autocommit/ambient restoration. Repeatable-read is not linearizable revocation or database RLS.

## 4. Exact changed-path accounting

The cumulative authorized implementation set is **52 paths = 31 existing + 21 new**. This closer changes none of them. The cumulative plan amendment and the two separately authorized S1 reports are outside those 52 paths. `path-and-preservation.json` is the exact path-and-before/after-hash index; it records zero unauthorized modified paths and zero unexpected new source paths.

| Category | Existing | New | Total |
| --- | ---: | ---: | ---: |
| Production Java, configuration, migration, ops/build/CI | 7 | 14 | 21 |
| New test classes and fixture | 0 | 7 | 7 |
| Existing test/fixture updates | 20 | 0 | 20 |
| Existing boundary documentation | 4 | 0 | 4 |
| Total | 31 | 21 | 52 |

Production/configuration is exactly 13 new Java files, four existing Java files, new V016, existing PA25 SQL, POM and CI. New Java comprises two customer files; three subject files; four evidence files; four catalog/order files. Existing Java is the customer module descriptor, boundary filter, persistence configuration and subject query configuration. No activation implementation changes. New tests are six classes plus `CustomerSubjectDetailFixture`.

The twenty existing test/fixture paths are the exact plan §6.3 set: four architecture classes (`ModuleBoundaryTests`, `SharingQueryBoundaryTests`, `CustomerApiBoundaryTests`, `CustomerBusinessNumberBoundaryTests`), `CustomerWebConfigurationTests`, fourteen container test/fixture paths (`PostgresContainerIT`, `DatabaseRuntimeAclRbacHardeningIT`, `CustomerBusinessNumberReadPrivilegeIT`, `ProductionDatabaseIdentityProvisioningIT`, `CustomerBusinessNumbersFixture`, `CustomerBusinessNumbersIT`, `CustomerMeFixture`, `SharingQueryFixture`, `SharingQueryServiceIT`, `SharingRoleLeastPrivilegeIT`, `SharingAdmissionRoleLeastPrivilegeIT`, `EvidenceMediaReadIT`, `EvidenceMediaReadRoleLeastPrivilegeIT`, `PublicShareAdmissionIT`), and `CustomerBusinessNumberQueryServiceTests`. Historical migration/ACL assertions remain prefix-pinned before explicit current-chain upgrades; exact graph/history assertions are not replaced by subset checks.

## 5. V016 privilege-only result

`backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` is append-only and privilege-only. The eight added effective tuples below all have grantee `customer_iam_role`, `is_grantable=false`; empty column means schema. Its SQL literal representation has six fields; the non-grantable fact is independently checked, not invented as a seventh stored field.

| Kind | Object | Column | Privilege | Grantor |
| --- | --- | --- | --- | --- |
| column | subject_number.subject_risk_state | caller_number_binding_id | SELECT | subject_number_owner |
| column | subject_number.subject_risk_state | state | SELECT | subject_number_owner |
| schema | evidence | — | USAGE | evidence_owner |
| column | evidence.evidence_record | subject_id | SELECT | evidence_owner |
| schema | catalog_order | — | USAGE | catalog_order_owner |
| column | catalog_order.customer_order | subject_id | SELECT | catalog_order_owner |
| column | catalog_order.customer_order | account_id | SELECT | catalog_order_owner |
| column | catalog_order.customer_order | owner_organization_id | SELECT | catalog_order_owner |

Facts: delta **8 = 2 schema + 6 column**; customer **26→34 = 6 schema + 28 column**; evidence **16** unchanged; other roles **219** unchanged; global **261→269 = 34+16+219**; current history **15→16**. V016 target ACL old/final sets are **42→50** (customer plus evidence), not global totals. Six logins, seven effective roles and four membership edges are preserved. No table-wide SELECT, write/sequence/routine/owner/grant-option capability is added. Count/predicate-column grants permit raw SQL reads of those columns; they are not aggregate-only permissions.

V016 validates complete V015 pre-state or complete V016 final state, rejects partial/widened/misprovenanced/catalog drift atomically, preserves non-target ACLs, roles, credentials, owners, definitions and business rows, and supports stable direct replay. Its migration-only advisory lock is not a request lock. PA25 uses independent literal 269-tuple and exact sixteen-history oracles, with no repair of rejected drift. Actual Flyway checksum is **1669606497**; V016 SHA256 is `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f`. No circular self-checksum is embedded. `v016-inventory.json`, `v016-checksum.log` and real privilege XML establish these results.

## 6. Test design and exact execution inventory

| New class | Ordinary named tests | Coverage |
| --- | ---: | --- |
| CustomerSubjectsWebTests | 12 | Route/auth/validation precedence, ignored inputs, Optional/404/null handling, exact four fields, no cursor/pool activation. |
| CustomerSubjectDetailQueryServiceTests | 12 | Ordered composition, role/lifecycle/ownership/risk/projection failures, long counts, completion errors, immutable transaction and no writes. |
| CustomerSubjectDetailConfigurationTests | 6 | Actual lazy/profile/qualifier graph, independent template, shared customer DSL, missing resources. |
| CustomerSubjectDetailBoundaryTests | 4 | Minimal owner APIs, no foreign SQL, exact V016/PA25 delta, frozen contract/migrations and real module verification. |
| CustomerSubjectsIT | 20 | PostgreSQL16/real HTTP matrix, all roles/statuses, snapshot/concurrency, zero writes, pool/ambient restoration and disclosure sentinels. |
| CustomerSubjectDetailReadPrivilegeIT | 10 | Pre-V016 denial, exact fresh/upgrade/replay/delta, read-write ACL denials, atomic drift rejection, PA25 history and six-login boundaries. |

All **613 baseline identities** are preserved exactly; additions are **64 = 34 Surefire + 30 Failsafe**. No disabled/conditional/parameterized/repeated/dynamic substitution, skip or ignored-failure flag is used. Baseline source identities are independently recovered from `E/retry3/input-corpus.txt`, not fabricated from final XML.

| Inventory | Baseline | Final executed |
| --- | --- | --- |
| Surefire | 40 classes / 341 tests | **44 / 375**, zero failures/errors/skips |
| Failsafe | 17 classes / 272 tests | **19 / 302**, zero failures/errors/skips |
| Combined | 57 / 613 | **63 / 677** |
| Annotation-line scan | 614 | **678**, including the one historical comment hit, not an extra executable test |
| Explicit Failsafe POM includes | 17 | **19**, exact equality to container classes |

`exact-raw-identity.json` enumerates **677 unnormalized (class, method) identities**, with source=CI=raw XML equality. `responsesProblemsAndCapturedLogsNeverDiscloseSecretsOrRowContents` is an ordinary no-argument XML name, not a stripped parameter suffix. Historical CI totals and line-scan checkpoints are retained. Full acceptance XML is only `full-clean-attempt1-surefire-reports/` and `full-clean-attempt1-failsafe-reports/`; inherited/stale/focused XML is not summed into it.

## 7. Executed validation and exits

These executions occurred in fix2 before this documentation closer. No Maven/container/contract run is claimed to have been repeated here. Copied raw XML has been independently recounted for this report. Environment: JDK 21.0.13, Maven 3.9.9, Node 22.23.4, Docker 29.1.3, disposable PostgreSQL16 image recorded in `environment-final.log`. Maven uses `JAVA_HOME=/opt/jdk21` and external `E/retry3/runner/mvnw`; tracked wrapper bytes are unchanged and the external copy equals original bytes with CR removed only.

For each command stem below, exact argument vector is `<stem>.command.json`, durable output `<stem>.log`, and actual exit `<stem>.exit` in `F`. The successful prior commands and retained failed attempts are distinguished, not overwritten.

| Command stem | Executed action / evidence result | Exit |
| --- | --- | ---: |
| environment-final | Date, Java/Maven/Node/Docker and PostgreSQL image preflight. | 0 |
| focused-64-attempt1 | External wrapper `-B -ntp -Pcontainer-smoke`, four new unit classes and two new IT classes, `verify`; 4/34 + 2/30 = 64 passed. | 0 |
| focused-postgres-attempt1 | Same four unit classes plus six selected PostgreSQL/HTTP classes; 4/34 + 6/88 passed. The 88 is the integration subtotal, not 88 including units. | 0 |
| full-clean-attempt1 | External wrapper `-B -ntp clean -Pcontainer-smoke verify`; 44/375 + 19/302, all zero failures/errors/skips. | 0 |
| ci-inventory-final | Exact CI inventory script `E/fix1/ci-inventory-verbatim.py`; source=CI=raw XML, historical checkpoints, 677/678/19. | 0 |
| contract-validators-final | `bash -e E/retry3/contract-commands.sh`: npm ci/check:s3; protocol s3; security s3; capacity s2; acceptance s1 and s2 with doc links. Each passes; structural validators are not production acceptance. | 0 |
| jar-smoke-final | Verbatim CI default/web JAR smokes; bounded unauthenticated subject probes, no DB/verifier adapter; JVM cleanup. `default-jar-final.log`, `web-jar-final.log`. | 0 |
| jar-index-final | `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar`; complete packaged index. | 0 |
| dependency-list-final | Wrapper dependency:list, runtime scope; `runtime-dependencies-final.txt`. | 0 |
| jar-firewall-final | Verbatim dependency/JAR firewall; 116 adversarial probes, exact dependency deltas, no forbidden runtime dependency/JAR expansion. | 0 |
| prior-root-custody | `sha256sum -c evidence-manifest.sha256` at `E`. | 0 |
| prior-retry3-custody | Same verification at `E/retry3`; original failed/stop evidence retained. | 0 |
| prior-fix1-custody | Same verification at `E/fix1`; original failures retained. | 0 |
| preservation-final | Exact paths, baseline identities, original-literal reversal, historical docs, frozen inputs and wrapper equality; `path-and-preservation.json`. | 0 |
| v016-checksum | Actual Flyway ChecksumCalculator on final migration file → 1669606497. | 0 |
| final-jar-sha256 | SHA256 of final packaged JAR; `final-jar.sha256`. | 0 |
| final-jar-equality | `sha256sum -c final-jar.sha256`; same JAR after post-validation. | 0 |
| blocked-custody-check | Original fix2 `blocked-custody.sha256` verification; preserved as historical blocked-handoff custody, not the final seal. | 0 |

`verbatim-ci-script-custody.json` proves the three executed inventory/smoke/firewall script bodies match CI. `postvalidate.py` records the unnormalized identity comparison and rehash of all 1,810 frozen source entries. This closer separately rechecks those entries and permits only the two new reports, with generated `target`, `node_modules`, `tmp` directories excluded exactly as in the source freeze.

Earlier evidence remains non-acceptance: initial Git-pointer/wrapper preflights stopped; retry3 focused 64 passed but full Surefire had the 10-versus-12 configuration failure; fix1 full clean exited 1 with two HTTP assertion failures and exact-XML mismatch. `E/fix1/repair-ledger.md` retains failed ACL-name truncation, no-op INHERIT mutation, invalid order product-code fixture, ineffective bean-definition seam, CRLF A0 parsing and preservation/probe diagnostics. Fix1's supplemental `explain-attempt3.log/.exit` exited 0 for six PostgreSQL16 EXPLAIN ANALYZE/BUFFERS probes over only 32 evidence, 64 orders and 32 cleared-risk rows. These are small-fixture measurements, not representative performance/SLO acceptance. The companion fix artifact records the final bounded repair and final finding states.

## 8. Documentation and frozen inputs

The four already-completed implementation docs are `backend/README.md`, `docs/14-production-data-model-and-flyway-schema-spec.md`, `docs/17-production-security-design-spec.md`, and `docs/operations/pa25-production-database-identity-provisioning-runbook.md`. They add the bounded PA28 V016 current boundary and relabel PA27 V015 historical; historical numbers/catalog material remain preserved. The authorized docs14 LF transport repair has canonical-content equality proof in `E/fix1/a0-newline-repair.json`; it does not alter frozen model tables. This task adds only the two S1 reports.

No OpenAPI change is needed: the existing closed `CustomerSubjectDetail` schema already requires these four fields and permits the confirmed omission of optional fields. No V001–V015 change is needed or permitted: privileges are reconciled by append-only V016, preserving historical migrations/checksums and prefix-pinned security tests. All sixteen raw inputs remain byte-identical, independently of canonical-LF Java guards. `frozen-final.sha256` records each raw hash. Raw OpenAPI is `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`; raw V015 is `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc`. These local transport hashes are not claims about merged LF blobs. Tracked wrapper SHA256 is `825cdc62abd459da5efa6eeb0218f329c0adca3f7d4f7560f6ba2b43b9deb928`.

## 9. Final findings and artifact hashes

| Accepted finding | Final state and proof |
| --- | --- |
| PA28-S1-IMPL-01 | **Fixed and verified**: exact old-test declared-method literal 10→12 only; all eight names/other bytes preserved by reverse hash; full Surefire passes. |
| PA28-S1-IMPL-02 | **Fixed and verified**: test-only actual-controller provider seam reaches completion-failing service; real HTTP present/empty paths remain sanitized 503; pool/transaction assertions and finally restoration unchanged. |
| PA28-S1-IMPL-03 | **Fixed and verified**: lifecycle CapturedOutput injection preserves ordinary no-argument identity and all disclosure assertions; raw 677 equality passes without suffix normalization. |

Fix2 changed only `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java`, SHA256 `2c7d5682335cac8384a71791274fd7ba6f3407202ade0577f73066d048a9d763`. Final JAR SHA256 is `be4e0fee89357bf353b09d55b3f753846d710ac581a821f1375e2a4417854e41`. Current cumulative plan transport SHA256 is `d397e60f4ca67d9004d3f6a4580e1c6d606150015d2a8f76d2b4093cc58342f6`; plan-fix transport SHA256 `c2cceab370c89945ed9861e79ba96e397dcfb4d17b349297b6ea6be9178da275`. The plan includes the separately accepted implementation-time expansion, so these are not substituted for the earlier accepted checkpoint hashes. Control is pinned unchanged at `784e896ec3515dc493f50215c3bd83d43f80a93ae0d0132e67042a7be2e8cdeb`.

| Final evidence artifact | SHA256 |
| --- | --- |
| validation-disposition.json | `1b9bd2e6a4ae5b9338527e4a7365b411e9fe1329b30bc7a6c3f415661d7d45b1` |
| full-clean-inventory.json | `db00505182a91f01084430cc77bc409ae6d400a6b48007bdf2159ddf58359878` |
| exact-raw-identity.json | `75d68c0a5c8c5c608c452464074446a2f90f845a0628e2660985eac49cd34efd` |
| path-and-preservation.json | `ea95b762f7651bafc957129669bba633ff850b5f243ce216297fbd94de887be0` |
| v016-inventory.json | `1f8ab455f3513a00b7977b939ae9cd106954bb3a71e7b6be65d1698c55d5c950` |
| pre-validation-source.json | `253ac99377678a8b80ac944943e6091ea5371c602f44ff48a721dc5eeb91c771` |
| full-clean-attempt1.log | `f72495beaeeacec267bd346200bd4c1e693ee0f833062fcf94285428329c86fd` |
| focused-64-attempt1.log | `5687eb62c176199ecd65423af8514d9fa64b91a3529629aa51f0b466dcf5cf2a` |
| focused-postgres-attempt1.log | `80d24cf54254453a43b6724f000e89df24b300fa98563b139e0dad7e06ece51f` |
| repair-delta.json | `da5c6ba6b2712313cb86e26c1fd671e3cc79b6d5d8a5cc2ae90d65a09e9281eb` |
| test-only-repair.patch | `8d955af48a1cf0c261d7626baef5a928546336bff2e89747c9ecc37285b8c4ac` |

The final manifest seals both reports and all final/historical external evidence, including logs, commands, exit files, copied XML, indexes, checksum files and the closure audit. It excludes itself to avoid a circular digest. Each report's byte size, LF count and SHA256 are in the external closure audit, not self-embedded hashes. Verify from `F` with `sha256sum -c implementation-evidence-manifest.sha256`. No Git metadata is created or altered; local byte preservation is not managed commit custody.

## 10. Classified residuals and production boundary

| Classification | Owner / retained boundary |
| --- | --- |
| Dependency security, out of scope | Dependency/Security: locked npm audit reports 17 vulnerabilities (1 low, 9 moderate, 5 high, 2 critical); validator success is not security acceptance; no dependency/lockfile repair. |
| Authorization architecture, accepted limitation | Security/Database: application-enforced, non-RLS ownership and sensitive predicate-column access; repeatable-read snapshot is not linearizable revocation. |
| Performance, not accepted at scale | Backend/Database: six small-fixture probes are not production-cardinality or SLO evidence; no speculative index/DDL repair. |
| Deferred architecture/features | Backend/Architecture and Product: qualified-DSL inversion, optional number/trial/consent projection and claim-state policy remain separate work. |
| Tooling, non-failing | Backend/CI: Mockito/Byte Buddy dynamic-agent, JVM sharing and compiler/deprecation warnings remain; zero skips/ignored failures. |
| Repository/runner custody | Controller/CI/runtime: metadata-free Git status previously exited 128; byte rehash does not attest a managed commit, outer process/JSONL or provider identity. |
| Operational, not executed | Platform/Database/Security/Operations: production credentials/connections, TLS/pg_hba, deployment/rotation, exclusive administration, session invalidation/pool drain, Q17/Q23 and live acceptance remain outside this task. |

Production execution **NOT EXECUTED**; production readiness **NOT CLAIMED**. Only disposable local validation passed. No remote or later-gate action is performed. The prior editor-availability/report/seal blocker is resolved by the explicit shell `apply_patch` authorization and this bounded closure, not by changing historical dispositions or claiming unobservable runner facts.
