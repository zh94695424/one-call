# PA27 customer business-number read foundation: S1 implementation

- Task: `pa27-business-number-implementation-gpt-20261111-02`.
- Bounded fix: `pa27-business-number-implementation-fix1-gpt-20261111-01`, 2026-11-11; supersedes the original blocked validation disposition, retaining its report in external `intake/`.
- Work unit: `PA27-customer-business-number-read-foundation`; sole composed S1.
- Author role: implementation only; 2026-11-11, Asia/Shanghai.
- Metadata-free fix snapshot: `/mnt/d/codex-temp/pa27-implementation-fix1-20261111-01`; original implementation snapshot `/mnt/d/codex-temp/pa27-implementation-20261111-02` remains historical provenance.
- Accepted pre-correction cumulative plan SHA256: `cce2dead52bb309d39753cc2e120ac6e9de50a021fc004189fd9135778a627be`; bounded corrected plan SHA256: `58e215b1ce0ce103337ff80d995ab443d6803aa506a4c14775e6273fba8b4a82`.
- Authority: active PA27 Control, accepted cumulative plan, plan-fix-1 and all four plan-review/re-review artifacts; Controller adjudication `PA27-S1-IMPL-01` permits only the bounded cumulative allowlist correction. Control, historical plan-fix and reviews remain byte-identical.
- Validation disposition: final-source clean verification and unchanged exact source=CI=XML inventory pass; focused suites, JAR smokes/firewall and API/A2/A3/A4/A5 checks pass. Initial failed attempts remain recorded below. No deployment or production acceptance is claimed.

## 1. Controller finding disposition

`PA27-S1-IMPL-01` is **fixed** under the accepted Controller adjudication. The original report's `PA27-S1-VAL-01` described this same omitted test-oracle ripple, not a separate production-design defect. Cumulative plan §7 reconciles 49 = 26 M + 23 N, §7.2 adds the one existing path, and §8 specifies the exact assertion update.

`SharingQueryBoundaryTests.sharingQueryApiAndProjectionAreExact` retains the four sharing interfaces and adds exactly `subject_number::customer-business-number-query` and `activation_consent::customer-activation-query` to `containsExactlyInAnyOrder`. All other logical text/assertions/behavior are identical to intake; all eight methods remain. Static exact-replacement proof and the passing focused eight-case suite are durable evidence. No assertion is deleted, renamed, disabled, skipped or weakened. See the separate S1-fix artifact for validation-discovered in-allowlist corrections and final disposition; author completion is not Controller approval or entry into review.

## 2. Evidence custody and prerequisites

Durable evidence is **`D:\codex-temp\pa27-implementation-evidence-20261111-03`**, mounted at `/mnt/d/codex-temp/pa27-implementation-evidence-20261111-03`. This fix runs with authorized danger-full-access; Docker, real HTTP, Node child processes and the requested directory are usable. Each validation stem has `.command`, `.log`, `.exit`, `.started` and `.finished` files. Initial failures remain separately named; no failed attempt is overwritten as success. `reports/` retains focused and final XML/report copies; `intake/` preserves the original blocked report and governing read-only artifacts. The old sandbox's lost `/tmp` evidence is not claimed recovered.

The fix-intake whole-file SHA/mode manifest is `baseline.json`; the corrected 49-path manifest is `allowlist.json`; sixteen raw frozen hashes are in `frozen.sha256`, and historical plan-fix/reviews in `protected.sha256`. Actual fix-intake Control SHA256 is `66a831d997f08a7594ce5f9294ba5a24f27bd2c5c5ded3779f690b85e4763e05`; original S1 intake was `dfd5fda15b82ea70796dbe859727a222e06878c3a54f3b0671442a03dcbced8e`. These collection points are distinct. No Git/remote/PR fact is independently asserted.

Linux uses `/opt/jdk21`: Temurin **21.0.13+9**, vendor Eclipse Adoptium, and the pinned wrapper resolving **Apache Maven 3.9.9**. The checked-in CRLF wrapper remains byte-identical; only an external LF execution copy under evidence `wrapper/` is used, with unchanged wrapper properties and distribution SHA256. No repository wrapper edit or installed-Maven substitute occurs in this fix. Node is **v22.23.4**.

No Mockito preload, test-skipping flag, ignored failure or validator bypass is used. Docker reports server **29.1.3**. The actual `postgres:16` image runs PostgreSQL **16.15 (Debian 16.15-1.pgdg13+2)**, image/repository digest `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`; `postgres-image.log` records inspection and executable version. Tests use disposable synthetic databases, never production.

## 3. Implemented behavior and ownership

Only the exact `GET /api/v1/customer/business-numbers` route and `listCustomerBusinessNumbers(Principal,HttpServletRequest)` are added. The existing boundary admits that one GET; method/path/canonical-URI rejection and established bearer verification remain authoritative. The controller obtains only qualified lazy service/codec providers and uses the existing sanitized Problem writer. Query/body/header/cookie ownership selectors are not read as authority.

`BusinessNumberPage` contains exactly `items`, present nullable `nextCursor`, `hasMore`. Each explicit item contains exactly `bindingId`, `subjectId`, `numberMasked`, `numberType`, `bindingStatus`, `activationStatus`, `realNameStatus`. DTO enums enforce the frozen values. Persistence rows, timestamps, owner IDs, number ciphertext/HMAC and diagnostics are not serialized. Masks are stored projections, not reconstructed secret values. Invalid or inconsistent row/status projections fail the whole page as sanitized INTERNAL; no partial page is returned.

The verified account/selected workspace alone forms scope. The customer access repository rechecks ACTIVE account; selected organization scope additionally rechecks ACTIVE organization and exact account/organization ACTIVE membership with the explicit ADMINISTRATOR/MEMBER/EVIDENCE_MANAGER/AUDITOR allowlist. Missing/inactive context is generic FORBIDDEN, not an empty result. An active context with no owned bindings is the empty successful page. Subject-number owns the account/organization ownership predicate, subject/binding join and sort/seek. No binding-status, activation-status, real-name, risk or verification filter is added. Activation-consent owns the bounded subject-ID query; missing row alone defaults INACTIVE in composition. All three binding statuses, all three present activation statuses, both number types and both real-name statuses remain visible under the literal ownership matrix.

Two public named query seams expose no JDBC/jOOQ/customer-internal types: `CustomerBusinessNumberReader` with immutable Scope/Anchor/Row, and `CustomerActivationStatusReader`. Their lazy web configurations use the same qualified `customerQueryDsl` without a foreign internal Java import. Customer has exactly six allowed named dependencies; 14 modules and four discovered production edges are verified by passing module tests. The historical sharing assertion now enforces that same exact six-name set.

The new service uses a separate read-only REQUIRES_NEW **REPEATABLE_READ** template on the existing customer transaction manager/pool; the old READ_COMMITTED template is unchanged. Inside that transaction it sets/asserts `customer_iam_role`, checks lifecycle, fetches ordered limit+1 rows, and performs one bounded activation batch. Role assumption/assertion failure maps to sanitized 503. No other pool, direct production connection, manual commit, owner role or business write is introduced. PostgreSQL same-connection/snapshot, lifecycle, zero-write and single-connection-pool restoration outcomes are reported from actual IT execution below. Snapshot revocation applies on the next request; no cross-page snapshot or linearizable revocation promise is made.

Pagination defaults to 20 and accepts limits 1–100, including ASCII `001`; duplicates, overflow and non-digit forms fail 422. Keyset order is subject.createdAt DESC / native PostgreSQL binding UUID ASC; the seek excludes the anchor and needs no anchor-row lookup. Lookahead is stripped, continuation anchors the last returned row, and the tail is false/null. Service validates UUID/status/mask/timestamp consistency, duplicate bindings, returned ordering and bounded activation maps. No count/offset/N+1 query is added.

Cursor encoding is endpoint-only HMAC-SHA256, 93 bytes / canonical unpadded base64url 124 characters: version1, 32-byte scope digest, big-endian seconds/nanoseconds, 16 UUID bytes, 32-byte MAC. Scope includes the verified account and personal/organization selection, exact order domain and final LF; the MAC has its separate literal domain separator. Microsecond anchors, bounded UTC years -4712..294276, canonical encoding and constant-time MAC/scope comparisons are enforced. The required external canonical padded standard-base64 32-byte key is wired only through `ONECALL_CUSTOMER_BUSINESS_NUMBER_CURSOR_HMAC_KEY`; no production key/default generator is supplied. Missing/bad key is 503 even on first/empty pages; replacement invalidates old cursors; limit is not scope-bound and no TTL is promised.

Public precedence is boundary404 → bearer401/503 → selected-workspace503 → limit422 → structural-cursor400 → key503 → MAC/scope400 → lifecycle403. Technical dependency/role failures are sanitized503, projection/logic failures sanitized500. Existing no-store/no-cache/nosniff, Bearer challenge and fresh trace behavior are retained. All twelve real-HTTP Web methods pass after correcting repeated Mockito exception stubbing, without changing their assertions or method names.

## 4. V015, PA25 and historical compatibility

V015 is a single transactional DO block adapted from the existing bounded V014 catalog-evidence pattern, without business/schema/role DDL, DML, revokes, default/PUBLIC-ACL mutation or membership changes. It accepts exact V014 state or exact final replay state; it rejects partial/unexpected tuples, invalid authority/ownership/role settings/provenance/topology/default/effective privilege drift. Only the fourteen delta grants are emitted by the two named owners using SET LOCAL ROLE, restoring the invoker. Pre/post invariants preserve unrelated ACLs, owners, definitions, graph and credential/provenance metadata. The focused PostgreSQL rerun passes all eight privilege IT methods, including replay, drift rejection and atomicity checks.

| Owner/grantor | Addition to customer_iam_role | Tuples |
| --- | --- | --- |
| subject_number_owner | USAGE on subject_number | 1 |
| subject_number_owner | SELECT business_subject(id,owner_account_id,owner_organization_id,real_name_status,created_at) | 5 |
| subject_number_owner | SELECT caller_number_binding(id,subject_id,number_masked,number_type,status) | 5 |
| activation_consent_owner | USAGE on activation_consent | 1 |
| activation_consent_owner | SELECT service_activation(subject_id,status) | 2 |

All are owner-issued, no grant option. Retained customer reads are the ten V014 account/organization/membership columns and two schema usages. Static arithmetic is old12 + delta14 = customer26 (4 schema +22 column); evidence16 unchanged; other-role219 unchanged; seven-role global261. No table SELECT, sensitive number material, claims/risk/consent read, write/routine/sequence/schema-CREATE capability is reserved. `static-audit.log` compares independent SQL literal sets; the passing twenty-method PA25 PostgreSQL suite exercises the independently literal Java oracle against actual catalogs. Static arithmetic is not substituted for runtime proof.

Final V015 raw SHA256: `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3`. **Flyway 12.4.0 ChecksumCalculator** over the finalized FileSystemResource yields **`-1316980027`**, not a substituted raw hash or hand-guessed CRC; `flyway-checksum.log` records the fresh invocation. PA25 history and the new history test use that exact checksum. No Flyway repair or ignored migration is introduced.

Current PA25 apply/check requires exact fifteen successful SQL history rows including version015/script/description/checksum, and rejects V014-only. Its independent ACL oracle extends the original 247 by exactly fourteen. The existing twenty provisioning methods remain named and counted unchanged, with terminal corruption probes moved to rank15/015, bad rank16, alias15, cleanup1015 and future016. PA26 retains its twelve historical methods and V014 12/16 oracle, pins historical Flyway to014, uses applied-only historical history checks, and moves current PA25 calls after a separate V015 upgrade. Latest-chain fixtures expect15; the eight-method EvidenceMediaReadIT change is bounded to its migration count. PostgresContainerIT preserves the V001–V013 applied prefix and64 methods, observing both V014 and V015 ABOVE_TARGET. The focused PostgreSQL16 rerun passes PA25 twenty, PA26 twelve and PostgresContainerIT sixty-four methods, plus the new endpoint eighteen and privilege eight: 122 total, zero failures/errors/skips.

## 5. Exact modified/new path inventory

Exactly **49 allowlisted S1 paths = 26 modified +23 new**: production18 (4M/14N), database/tests/CI26 (18M/8N), docs5 (4M/1N). The cumulative plan correction and new S1-fix artifact are separately authorized fix outputs, excluded from those 49. Relative to fix intake, only eight paths change: the sharing boundary test, existing S1 Web test, privilege IT, endpoint IT, subject reader, cumulative plan, this report and S1-fix artifact. The other43 S1 paths remain byte-identical. Build outputs under backend/target are disposable validation artifacts; all logs/XML copies and contract-generation output are external. Final hashes/modes are emitted externally to avoid self-reference.

| Kind | Path | Final raw SHA256 |
| --- | --- | --- |
| M | `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` | `89f47a8f600020aca734fa9550bc28332f3627ef607e21c065be478e86b1b1ad` |
| M | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | `968f2139e6be4453a9b09f72f807989f57c3a5bbef79871947098571321b6891` |
| M | `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java` | `a88b2584deea41c6eaf4988be38a10440efa2e052ff2ead1bfc357da20384da3` |
| M | `backend/src/main/resources/application-web.yml` | `76efd0117d412faec0e55ad6b36671f1f2fc606fbd15f1117f44d40bf7c7558c` |
| N | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumbersController.java` | `4cbc4ec79235dee5064264ff47b054c0368f9104201c729b07af59f910e7ae2d` |
| N | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/BusinessNumberPage.java` | `706a3939275a028552b497ab4256eff7d55d907f419e911fccbf80819382fc2a` |
| N | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorCodec.java` | `5f48be1e66513522605859a9e15bba07ad2086ea9ebd2239dfa3dd86aff65553` |
| N | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorProperties.java` | `d557edc31aa948a5dc76222923370c22c63838d50e5b70ebb5005f306ad93cf0` |
| N | `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryService.java` | `4106e51871ac7904e4dfa7724518be443ada01f597039a6f80fb663b65431a6a` |
| N | `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/JooqCustomerBusinessNumberAccessRepository.java` | `0ebed8ca6b3790ad45a1c60976585aa6fbc2d8f81a4bcb8c000c25bc666ffaa9` |
| N | `backend/src/main/java/com/onecall/core/subject_number/customerquery/package-info.java` | `eb3926dff8bcf6a3c1bd830550a37363911883a02a74767c790503d735cf1e43` |
| N | `backend/src/main/java/com/onecall/core/subject_number/customerquery/CustomerBusinessNumberReader.java` | `691d5855f76762f72fb2f916c5db01243775677557b125b89b21ada9afd2fe97` |
| N | `backend/src/main/java/com/onecall/core/subject_number/internal/customerquery/JooqCustomerBusinessNumberReader.java` | `3336a48affd74e6aacf5597c97e25f1f6bad83ba02b7f3f79cbb56b4837f10a2` |
| N | `backend/src/main/java/com/onecall/core/subject_number/internal/customerquery/CustomerBusinessNumberQueryConfiguration.java` | `2f3904a57c0f1b6cc1149a65f480ddab9c256c6ffb278e35da5c971edd0675e9` |
| N | `backend/src/main/java/com/onecall/core/activation_consent/customerquery/package-info.java` | `ebde4ee2c769829fabdca1d84cb16eff2788cee9e3dcf710b3f5c3e6c1e20d86` |
| N | `backend/src/main/java/com/onecall/core/activation_consent/customerquery/CustomerActivationStatusReader.java` | `2b8c89c425e8fca6e19f12411a6a7835df6a3e5acf82b749aed3b7d651aecbb0` |
| N | `backend/src/main/java/com/onecall/core/activation_consent/internal/customerquery/JooqCustomerActivationStatusReader.java` | `8081a4d502ea6d3d75c154348e91d982efea840e2384e0ede0b8c26b50ed4db8` |
| N | `backend/src/main/java/com/onecall/core/activation_consent/internal/customerquery/CustomerActivationQueryConfiguration.java` | `216d2e49c09977a338c806402a569ba75a81ef801e2ad6aec68d2bc841feecc2` |
| N | `backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql` | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` |
| M | `backend/ops/postgresql/pa25-runtime-identities.sql` | `069ac059069ef24609394ef47e8bf7040a23d0273ea14024b76731ce0dcefa0a` |
| M | `backend/pom.xml` | `bf19e10718a6b618869ae6206a2ce718abd913bf78e7a43905f8525a121e2e72` |
| M | `.github/workflows/ci.yml` | `c031792d9a7e08101c051bc933628aafaf2a27eaa0086713823f1f2273a27143` |
| N | `backend/src/test/java/com/onecall/archtests/CustomerBusinessNumbersWebTests.java` | `9a8b8123edeb327223da18eaba88555648da39c54def6f6fe8cbcde21546cb63` |
| N | `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java` | `2ca65fb5f5240e9a258193165040f6a4b30493f57e305ac838ae8e2771963b3f` |
| N | `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorCodecTests.java` | `5022fd5f5d1aea387b54f555aab80d4d6fc2db417ea2304a2b2174c8f5a77bb0` |
| N | `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java` | `c277dd47dde5998d9e071888b0691a36427c41bfa35cf9221b4612e4802c8e61` |
| N | `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java` | `4b3a305cafa2d9d6443a4854e520a3a8761d138487acec7c290fbb2dc619b5d4` |
| N | `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java` | `0859e11ea913569722afa08c259808206da49e617beaf392fb7f450fe301ede6` |
| N | `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java` | `007713d99e645b9c57d3d97d643ff08d39aba94770416ed8975a047eb32f805a` |
| M | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | `f534ec537ba6f71c2618288823ded96029be7b0f483db3b1e0d6bcfbbce5ef21` |
| M | `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java` | `690f7caf3ad58b3ecf7120509d45f53cb332a3858f3de7dcf83cab8c6f9a7567` |
| M | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | `918173edf0630c14826032f57cc011d818833d7183dda5717732c91a0ca20530` |
| M | `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java` | `08bcab75a0e808dd4f26c8f7afa3a69c840ee46d0659eca859b9b499665ba14f` |
| M | `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java` | `9a25c9f15dbc287f817d026a9f15f2758f41b9b98542fa835a5fbb44af97456c` |
| M | `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java` | `0b2406807f8018719624188fe39fa3b29dc9264a2923cdb6de3f542b76591d68` |
| M | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `013c52ce11e2829dae5e67da5af87b0178976968872494457b390d760c72c82e` |
| M | `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java` | `351e714cea80f6a6008393b7d33c01fd90890999999d5925f2e88095516ecfc0` |
| M | `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java` | `93e61e2eea24f4e61758a8c251c1dce5e484a5f42b7ffe5b6cdfd94b9152ddc5` |
| M | `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java` | `34133eb484a99bbaaed55ed4dd6e25f6a0682acac95b99080bf61f51ca2ba0c3` |
| M | `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java` | `1187de4955ebad3e3426ca9d08dc0403b1d58a0e9300e0e04ca599edfdd6e84b` |
| M | `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java` | `6ef7959bd832772f61d8e31e943c2726359fe0301a773bff877ad3a5e6669a78` |
| M | `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java` | `0619be26be015e43560dc6775f7b61c7abfaae76f65f77ba77bfe4c87a30a814` |
| M | `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java` | `917d9bc454473001d6866e58d2ea2089af2210c10aab30c4895e3f7c6768cc2c` |
| M | `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | `9569cb786a3ebc0fb5c063cc082d0f61d56a3880517f8972ea444b433076d147` |
| M | `backend/README.md` | `8b1ffbdd5901e0919b63d7a206d960f957ae5935480c6dd359d1321f117ad1ff` |
| M | `docs/14-production-data-model-and-flyway-schema-spec.md` | `724773f2b2a7e14587b1bf2a6293055eb4174f05e1bc7dbebdd81257a6cbd7d7` |
| M | `docs/17-production-security-design-spec.md` | `6d0405c55a42e5071ae6f39f196bdd78b7647d3bd2ef8cf9d1143262bb43ebe2` |
| M | `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | `6cec53687760039430865186f54e633e49b9b9800fb2a1e539fd48f1d8a0ab15` |
| N | `docs/gateflow/pa27-customer-business-number-read-foundation-s1-implementation.md` | `self; recorded externally` |

## 6. Exact source, CI, POM and measured XML

The checked-in CI mapping contains four unit additions and two Failsafe additions before source-class equality. Historical subtraction/checkpoint assertions through PA26 remain; PA27-start309/246/scan556 is explicit. Early Surefire aggregate is341, final exact XML calls are341/272, and POM has17 exact includes. The entire unchanged dedented CI Python block passes against final clean outputs, including every historical checkpoint, per-class exact name/duplicate/unsupported-annotation check and zero-failure/error/skip XML requirement. This is **source=CI=XML**, with exact POM includes, not a source-only substitute.

Measured final source/CI/XML: **40/341 +17/272 =57/613**, annotation-line scan614,17 includes. All baseline test method names remain. Final clean Surefire XML contains40 classes/341 passing cases and Failsafe XML17 classes/272 passing cases, with **zero failures, errors and skips in both runners**. No initialization-error placeholder, duplicate, decorated test name or synthetic XML is admitted.

The exact complete CI Python block is extracted verbatim/dedented as `ci-exact-inventory.py`. `exact-ci-inventory-rerun` and `measured-inventory-rerun` both exit0 after `final-clean-verify-rerun` exits0. Its XML requirements are not edited or bypassed. `source-ci-inventory.json` records all613 names; `xml-measured.json` records actual cases/counts. Final XML/text/Failsafe summaries are copied to `reports/final/`; first clean outputs remain separately in `reports/final-clean-first/`. The first clean build passed all613 tests but the unchanged checker rejected one `(CapturedOutput)` XML suffix; lifecycle injection in the already-authorized endpoint IT fixes identity while retaining every test/assertion. Focused repair and final clean outputs both pass the original checker.

### Per-class source counts

| Runner | Exact class | Methods |
| --- | --- | --- |
| Surefire | `com.onecall.archtests.CustomerBusinessNumbersWebTests` | 12 |
| Surefire | `com.onecall.archtests.CustomerMeWebTests` | 8 |
| Surefire | `com.onecall.archtests.CustomerSharesCommandWebTests` | 4 |
| Surefire | `com.onecall.archtests.CustomerSharesWebTests` | 10 |
| Surefire | `com.onecall.archtests.CustomerWebConfigurationTests` | 6 |
| Surefire | `com.onecall.archtests.EvidenceMediaReadPersistenceConfigurationTests` | 4 |
| Surefire | `com.onecall.archtests.MediaGatewayConfigurationTests` | 4 |
| Surefire | `com.onecall.archtests.MediaGatewayWebTests` | 18 |
| Surefire | `com.onecall.archtests.MediaPlaybackConfigurationTests` | 19 |
| Surefire | `com.onecall.archtests.OneCallApplicationTests` | 1 |
| Surefire | `com.onecall.archtests.PublicSharePersistenceConfigurationTests` | 2 |
| Surefire | `com.onecall.archtests.PublicShareWebTests` | 12 |
| Surefire | `com.onecall.archtests.SharingCommandPersistenceConfigurationTests` | 2 |
| Surefire | `com.onecall.archtests.SharingPersistenceConfigurationTests` | 2 |
| Surefire | `com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests` | 1 |
| Surefire | `com.onecall.archtests.architecture.CustomerApiBoundaryTests` | 3 |
| Surefire | `com.onecall.archtests.architecture.CustomerBusinessNumberBoundaryTests` | 4 |
| Surefire | `com.onecall.archtests.architecture.ModuleBoundaryTests` | 9 |
| Surefire | `com.onecall.archtests.architecture.SharingQueryBoundaryTests` | 8 |
| Surefire | `com.onecall.archtests.evidence.ControlledMediaByteSourceTests` | 4 |
| Surefire | `com.onecall.archtests.evidence.EvidenceMediaObjectReaderTests` | 4 |
| Surefire | `com.onecall.archtests.sharing.CustomerShareCommandServiceTests` | 6 |
| Surefire | `com.onecall.archtests.sharing.MediaPlaybackCredentialAuthorityTests` | 24 |
| Surefire | `com.onecall.archtests.sharing.MediaPlaybackServiceTests` | 21 |
| Surefire | `com.onecall.archtests.sharing.PublicShareQueryServiceTests` | 4 |
| Surefire | `com.onecall.archtests.sharing.ShareAdmissionServiceTests` | 26 |
| Surefire | `com.onecall.archtests.sharing.SharingPlaybackStateReaderTests` | 8 |
| Surefire | `com.onecall.archtests.sharing.SingleByteRangeTests` | 6 |
| Surefire | `com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryServiceTests` | 8 |
| Surefire | `com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests` | 6 |
| Surefire | `com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests` | 7 |
| Surefire | `com.onecall.core.customer_iam.internal.web.CustomerBusinessNumberCursorCodecTests` | 8 |
| Surefire | `com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests` | 6 |
| Surefire | `com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests` | 8 |
| Surefire | `com.onecall.core.sharing.internal.keysource.FileMediaPlaybackSigningKeyProviderTests` | 10 |
| Surefire | `com.onecall.core.sharing.internal.keysource.MediaPlaybackFileKeySourceIntegrationTests` | 8 |
| Surefire | `com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests` | 12 |
| Surefire | `com.onecall.core.sharing.internal.keysource.MediaPlaybackTencentKeySourceIntegrationTests` | 10 |
| Surefire | `com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProviderTests` | 18 |
| Surefire | `com.onecall.core.sharing.internal.keysource.TencentSsmSecretStringClientTests` | 8 |
| Failsafe | `com.onecall.archtests.container.CustomerBusinessNumberReadPrivilegeIT` | 8 |
| Failsafe | `com.onecall.archtests.container.CustomerBusinessNumbersIT` | 18 |
| Failsafe | `com.onecall.archtests.container.CustomerMeIT` | 10 |
| Failsafe | `com.onecall.archtests.container.CustomerShareCommandsIT` | 18 |
| Failsafe | `com.onecall.archtests.container.CustomerSharesIT` | 8 |
| Failsafe | `com.onecall.archtests.container.DatabaseRuntimeAclRbacHardeningIT` | 12 |
| Failsafe | `com.onecall.archtests.container.EvidenceMediaReadIT` | 8 |
| Failsafe | `com.onecall.archtests.container.EvidenceMediaReadRoleLeastPrivilegeIT` | 8 |
| Failsafe | `com.onecall.archtests.container.MediaGatewayTransportIT` | 8 |
| Failsafe | `com.onecall.archtests.container.MediaPlaybackIT` | 9 |
| Failsafe | `com.onecall.archtests.container.PostgresContainerIT` | 64 |
| Failsafe | `com.onecall.archtests.container.ProductionDatabaseIdentityProvisioningIT` | 20 |
| Failsafe | `com.onecall.archtests.container.PublicShareAdmissionIT` | 42 |
| Failsafe | `com.onecall.archtests.container.PublicSharesIT` | 8 |
| Failsafe | `com.onecall.archtests.container.SharingAdmissionRoleLeastPrivilegeIT` | 9 |
| Failsafe | `com.onecall.archtests.container.SharingQueryServiceIT` | 10 |
| Failsafe | `com.onecall.archtests.container.SharingRoleLeastPrivilegeIT` | 12 |

### Exact added method names

`com.onecall.archtests.CustomerBusinessNumbersWebTests`: 12 ordinary methods.

```text
exactGetRouteReturnsOnlyFrozenPageAndSevenItemFields
missingInvalidDuplicateAndWrongAudienceBearerAreUnauthenticated
unavailableVerifierAndMissingOrMalformedWorkspaceAreUnavailable
personalAndOrganizationSelectorsComeOnlyFromVerifiedPrincipal
absentAndValidLimitsUseDefaultAndInclusiveBounds
invalidAndDuplicateLimitsAreValidationFailed
malformedDuplicateAndTamperedCursorsAreCursorInvalid
crossAccountPersonalOrganizationAndCrossOrganizationCursorsAreInvalid
authenticationWorkspaceLimitCursorAndLifecycleErrorPrecedenceIsExact
serviceConfigurationAndUnexpectedFailuresUseSanitizedProblems
nonGetAndNonCanonicalRoutesStayNotFoundWithoutNewSurface
successAndProblemsNeverLeakSecretsAndRetainNoStoreHeaders
```

`com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryServiceTests`: 8 ordinary methods.

```text
defaultBoundsAndLookaheadRequestAreExact
trustedWorkspaceAndCurrentLifecycleAreRequiredBeforeReaders
allBindingActivationAndRealNameStatesComposeWithoutFiltering
missingActivationDefaultsToInactiveAndForeignProjectionFailsClosed
lookaheadUsesLastReturnedRowAndNullCursorOnTail
emptySingleAndMultiPageProjectionIsExact
technicalAndProjectionFailuresReturnNoPartialOrSensitivePage
webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact
```

`com.onecall.core.customer_iam.internal.web.CustomerBusinessNumberCursorCodecTests`: 8 ordinary methods.

```text
personalAndOrganizationRoundTripsAreCanonicalAndDeterministic
payloadVersionLengthDomainAndMicrosecondAnchorAreExact
invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected
truncatedExtendedUnknownVersionAndInvalidTimestampAreRejected
everyPayloadOrMacByteMutationIsRejected
accountWorkspaceKindAndOrganizationReplayAreRejected
missingMalformedAndWrongLengthKeysAreUnavailableWithoutDisclosure
keyReplacementInvalidatesOldCursorAndLimitDoesNotBindScope
```

`com.onecall.archtests.architecture.CustomerBusinessNumberBoundaryTests`: 4 ordinary methods.

```text
subjectAndActivationExposeOnlyTheTwoOwnedNamedQueryInterfaces
customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation
controllerSqlAndProjectionStayInsideTheirSemanticOwners
v015AndPa25DeltasAreReadOnlyAndFrozenInputsRemainByteIdentical
```

`com.onecall.archtests.container.CustomerBusinessNumbersIT`: 18 ordinary methods.

```text
postgres16FullChainAndCustomerRuntimeRoleAreUsed
personalScopeReturnsOnlyEveryOwnedBinding
organizationScopeIncludesEveryActiveRoleAndOnlyOrganizationOwnedSubjects
allBindingActivationMissingActivationAndRealNameCombinationsAreVisible
inactiveAndMissingAccountsAreForbiddenWithoutPageReads
inactiveMissingAndForeignOrganizationsOrMembershipsAreForbidden
unownedForeignAndOtherSelectedWorkspaceSubjectsNeverAppear
subjectCreatedAtDescendingAndNativeUuidAscendingTiesAreExact
emptySingleAndExactLimitPagesHaveExactTailSemantics
firstMiddleAndLastPagesEnumerateEveryBindingOnce
defaultOneAndHundredLimitsAndChangedContinuationLimitAreExact
malformedTamperedAndCrossScopeCursorsCannotWidenResults
committedLifecycleChangesAreRecheckedForEveryPage
lifecyclePageAndActivationShareOneRepeatableReadSnapshot
concurrentInsertAndDeletedAnchorFollowDocumentedKeysetSemantics
successDenialAndFailurePerformZeroBusinessWrites
roleIsolationReadOnlyAndAutoCommitRestoreOnPooledConnectionReuse
responsesAndCapturedLogsNeverContainSecretOrOwnershipSentinels
```

`com.onecall.archtests.container.CustomerBusinessNumberReadPrivilegeIT`: 8 ordinary methods.

```text
v014CustomerCannotReadSubjectBindingOrActivationWith42501
v015FreshAndUpgradeHaveExactFifteenHistoryEntriesAndFrozenBytes
v015AddsExactlyFourteenOwnerGrantedTuplesAndNothingElse
customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501
v015ReplayPreservesAclsGraphCredentialsDefinitionsAndBusinessData
v015RejectsPartialUnexpectedGrantorGrantOptionAndAuthorityDriftAtomically
pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle
allSixLoginBoundariesAndRolePoolRestorationRemainUnchanged
```

The new fixture has zero test methods. Assertions cover the literal48-combination matrix for personal/four organization-role fixtures, two accounts/five organizations with unique memberships, foreign/unowned exclusion, native UUID/high-bit ties, page walks, lifecycle/snapshot synchronization, zero writes, restoration and privilege drift. The focused PostgreSQL rerun executes all eighteen endpoint and eight privilege methods successfully. The existing boundary assertions are retained; the one exact four-to-six expectation correction is authorized and closed under `PA27-S1-IMPL-01`.

## 7. Commands and actual results

Every command runs locally, never against production. Maven commands use Linux JDK21.0.13 and the external execution copy of the pinned3.9.9 wrapper. No offline, Mockito-preload, test-skipping, ignored-failure or weakened-assertion flag is used. The original blocked implementation report is preserved in `intake/`; its lost historical logs are not presented as fresh evidence. This table records only the current fix execution.

| Evidence stem / command | Exit | Observed result |
| --- | --- | --- |
| `prerequisites`, `postgres-image` | 0 / 0 | JDK/wrapper/Node/Docker versions and executable PostgreSQL16 image recorded |
| `focused-unit` | 1 | 58 cases, zero failures, one repeated throwing-stub restubbing error; corrected within authorized Web test |
| `focused-unit-rerun` | 0 | Eight classes / 58 cases; zero failures/errors/skips |
| `focused-postgres` | 1 | Surefire341 pass; selected Failsafe122 has six continuation failures and one catalog-helper error; original XML retained |
| `diagnostic-seek`, `diagnostic-seek-rerun`, `diagnostic-seek-fixed` | 1 / 1 / 0 | Missing diagnostic Flyway classpath, then actual bound timestamp/varchar SQL error, then final reader continuation succeeds; external diagnostic only |
| `focused-postgres-rerun` | 0 | Surefire341 and selected Failsafe122 pass; endpoint18, privilege8, PA25 twenty, PA26 twelve, PostgreSQL baseline64; zero failures/errors/skips |
| `final-clean-verify` | 0 | First clean build passes40/341 +17/272, zero failures/errors/skips; XML identity checker below still rejects one decorated method name |
| `exact-ci-inventory`, `measured-inventory` | 1 / 1 | Both unchanged exact checks reject CustomerBusinessNumbersIT XML name ending `(CapturedOutput)`; no XML inventory acceptance claimed for this build |
| `xml-name-diagnostic` | 0 | Proves the one source/XML-name difference; existing lifecycle capture pattern repairs identity without assertion/checker changes |
| `focused-inventory-repair`, `exact-ci-focused-repair`, `final-clean-verify-rerun` | 0 each | Endpoint18 repair passes, unchanged exact checker passes, then final clean build passes Surefire341/Failsafe272 with zero failures/errors/skips |
| `exact-ci-inventory-rerun`, `measured-inventory-rerun` | 0 / 0 | Final-source fresh XML exact40/341 +17/272; scan614/includes17; every historical inventory assertion retained |
| `static-audit`, `static-audit-rerun`, `flyway-checksum` | 0 each | Exact49 path arithmetic, exact sharing replacement, independent SQL literals and actual checksum -1316980027 |
| `jar-smokes-preliminary` | 0 | Checked-in default and opt-in web JAR Python checks pass; preliminary, not substitute for final clean JAR |
| `jar-smokes`, `jar-firewall` | 0 / 0 | First clean JAR passes checked-in default/web boot, dependency/firewall checks and116 adversarial probes; final-source reruns follow |
| `jar-smokes-rerun`, `jar-firewall-rerun` | 0 / 0 | Final clean JAR passes identical default/web smoke and dependency/firewall checks, including116 adversarial probes; boot/dependency/index reports copied externally |
| `api-contract` | 0 | `npm ci && npm run check:s3`; frozen49-operation OpenAPI/generation/types/doc map/media lint pass |
| `contract-a2-s3` | 0 | 25 protocol fixtures pass |
| `contract-a3-s3` | 0 | 23 security fixtures pass |
| `contract-a4-s2` | 0 | 113 capacity cases pass; not production readiness |
| `contract-a5-s1`, `contract-a5-s2` | 0 / 0 | Unchanged validators deterministic; production_accepted=false, runtime evidence NOT_RUN |
| `contract-replica-bytes` | 0 | 1152 contract input files raw-byte equal; only three task-specific plan/report/fix documents excluded |
| `contract-replica-final` | 0 | Final raw comparison again confirms1152 contract input files; hashes retained in contract-replica-final.json |
| `frozen-raw` | 0 | OpenAPI + V001–V014 + actual fix-intake Control and historical protected artifacts unchanged |
| `frozen-raw-final`, `frozen-raw-final-rerun` | 0 / 0 | Both clean build checkpoints preserve all frozen/protected bytes |
| `scope-audit-preliminary`, `scope-audit-preliminary-rerun`, `scope-audit-preliminary-final` | 1 / 1 / 0 | Detected disposable generated metadata, then mixed sharing EOL; cleanup/LF normalization yield exact seven-path scope with preserved modes/method/assertion counts |

The external API/contract replica preserves the checked-in commands and frozen inputs, uses current bounded docs14/docs17, and keeps npm/generated output outside the repository. Ordinary Node child processes are usable in this run; A5 success is not synthesized by disabling determinism. The initial failed test and diagnostic attempts remain separately named with their actual nonzero exits and reports.

Representative final commands (external execution copy only):

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/node-v22.23.4-linux-x64/bin:$PATH
cd backend
/mnt/d/codex-temp/pa27-implementation-evidence-20261111-03/wrapper/mvnw \
  -B -ntp clean -Pcontainer-smoke verify
python3 /mnt/d/codex-temp/pa27-implementation-evidence-20261111-03/ci-exact-inventory.py
```

Full focused/final/contract command lines are recorded in their `.command` files. The focused unit selection is CustomerBusinessNumbersWebTests, CustomerBusinessNumberQueryServiceTests, CustomerBusinessNumberCursorCodecTests, CustomerBusinessNumberBoundaryTests, SharingQueryBoundaryTests, ModuleBoundaryTests, CustomerApiBoundaryTests and CustomerWebConfigurationTests. The focused PostgreSQL selection is CustomerBusinessNumbersIT, CustomerBusinessNumberReadPrivilegeIT, ProductionDatabaseIdentityProvisioningIT, DatabaseRuntimeAclRbacHardeningIT and PostgresContainerIT. Detailed fresh failed logs are retained; they are not overwritten with final success text. A GitHub-hosted CI job is **not executed** or represented as green.

## 8. Frozen raw bytes and evidence hashes

The following intake hashes match post-implementation raw bytes. Existing Java logical-LF predecessor checks remain; these raw checks additionally cover the actual CRLF snapshot bytes.

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6  backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql
4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505  backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql
4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7  backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql
8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030  backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql
6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065  backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql
7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d  backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql
bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81  backend/src/main/resources/db/migration/V007__activation_and_consent.sql
32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde  backend/src/main/resources/db/migration/V008__evidence_media.sql
3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1  backend/src/main/resources/db/migration/V009__sharing.sql
d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559  backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql
ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e  backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql
ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6  backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql
1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e  backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql
bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8  backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql
66a831d997f08a7594ce5f9294ba5a24f27bd2c5c5ded3779f690b85e4763e05  docs/gateflow/project-execution-control.md
```

External `evidence-manifest.sha256` records durable command/log/exit, script, inventory, XML and final document-copy hashes after completion. `baseline.json`, `allowlist.json`, `source-ci-inventory.json`, `xml-measured.json` and `final-scope-audit.json` retain independently inspectable intake, scope and result facts without self-referential document hashes.

Control, historical plan-fix-1 and all review artifacts remain byte-identical to fix intake. The cumulative plan is intentionally corrected only in §§7, 7.2 and 8; the new S1-fix artifact is separate. No protected path is normalized in-place. Modified/new delivery files use strict UTF-8, LF, exactly one terminal LF and no trailing whitespace; existing file modes are preserved. The sharing file's intake CRLF is normalized to LF with only the authorized logical assertion change. A disposable diagnostic generated an unlisted three-byte build metadata file; it was copied externally and removed before the passing scope audit, not delivered as a scope expansion. Final path/frozen/format audit and report hashes are emitted externally after both reports are written.

## 9. Documentation decisions and owner-routed residuals

README, docs14, docs17 and the PA25 runbook all prepend the same bounded **PA27 Current Runtime Boundary (V015)**: customer26/global261/delta14/evidence16/other219, retained V014 reads, fifteen-row current history, unchanged six-login/seven-role/four-edge graph, endpoint/key/visibility/transaction semantics and exact40/341+17/272 inventory. Their PA26 headings are relabeled **PA26 Historical Runtime Boundary (V014; superseded by PA27 V015)** with an explicit historical-only notice. Original capability/history/inventory evidence and model/catalog blocks are preserved; no architecture/OpenAPI/business model redesign occurs. The runbook's current execution order/oracle/evidence inventory now agrees with V015; old12/247/fourteen-row statements remain only as historical PA26 claims. API/A2/A3 validators pass against these bounded additions.

| Item | Owner / required destination | Disposition |
| --- | --- | --- |
| `PA27-S1-IMPL-01` (historical report alias `PA27-S1-VAL-01`) | Accepted Controller adjudication / Backend/CI | Fixed: exact six dependencies, eight unchanged methods, bounded49-path plan correction |
| Docker PostgreSQL16, real HTTP, current PA25/PA26/V015 execution | Authorized local Linux validation environment | Focused122 pass; full clean verification and exact XML completion recorded in §§6–7 |
| Historical A5 child-process EPERM | Authorized local Node execution | Closed locally: unchanged S1/S2 validators exit0, deterministic; no production acceptance |
| Requested durable evidence custody | Authorized external evidence root | Usable; fresh command/exit/log/XML evidence saved to requested D: location; old lost logs not claimed recovered |
| CRLF protected wrapper on Linux | Controller/Platform / authorized normalized checkout or external identical-content execution copy | External copy used, original unchanged; no scope expansion |
| Cursor key custody/distribution, replica consistency, replacement invalidating cursors | Security/Operations / deployment handoff | No live key assigned; missing key remains endpoint503 |
| Qualified customer DSL inversion | Backend/Architecture / accepted post-PA27 follow-up | Focused IT proves current same-connection/snapshot behavior; no extra Java module edge introduced |
| Column grants are not RLS; authorized columns remain cross-row sensitive | Security/Database / least-privilege acceptance | Tenant filtering remains application responsibility; no RLS claim |
| Repeatable-read lifecycle timing and cross-page mutations | Backend/Security / executed latch/keyset IT | Focused endpoint18 pass; no linearizable revocation/cross-page snapshot promise |
| Large owned-set sort/index performance | Backend/Database / representative EXPLAIN; schema/index change returns to Controller | Not measured; no unauthorized index added |
| Q17/Q23, migrator/comment custody, credential/TLS/pg_hba, exclusive administration, pool drain/old sessions | Platform/Database/Security/Operations / existing PA25 runbook | Production NOT EXECUTED / NOT CLAIMED |

## 10. Stop disposition

The sole composed S1 candidate is complete for this bounded author task: corrected49-path implementation allowlist plus two separately authorized plan/fix outputs, closed `PA27-S1-IMPL-01`, real PostgreSQL16/HTTP focused execution, final clean40/341 +17/272 zero-error XML, unchanged exact CI inventory and green JAR/API/A2/A3/A4/A5 checks. Four additional defects discovered by real execution were fixed only within existing authorized paths, preserving every test method and assertion; failed attempts remain evidence, not hidden green runs. No test-budget manipulation, capability widening, extra endpoint, applied production migration, frontend, deployment or production secret assignment is introduced. Controller gate disposition and owner-routed deployment residuals remain separate.

Stop after this bounded fix, validation and report. Control/historical plan-fix/reviews/OpenAPI/V001–V014 are untouched; only the authorized cumulative-plan correction is made. Final static/path/mode/format/frozen/report-inventory checks and document copies are durable under the requested evidence root, covered by `evidence-manifest.sha256`. No stage, commit, branch creation, push, PR creation/mutation, merge, approval, ready action, reviewer request, external comment or code-review entry occurs. Author report is not approval and no live production acceptance is claimed.

## 11. Agreed-fix-1 result: independent code review (2026-11-11)

Task `pa27-business-number-code-review-fix1-gpt-20261111-01` implements only Controller-accepted `PA27-CR-01..05` at gate `fix`. This appended section supplements, rather than rewrites, the historical S1 results and hashes above. The new bounded artifact is `docs/gateflow/pa27-customer-business-number-read-foundation-code-review-fix-1.md`; fresh evidence is `D:\codex-temp\pa27-code-review-fix1-evidence-20261111-01` (`/mnt/d/codex-temp/pa27-code-review-fix1-evidence-20261111-01`). All five fixes, focused PostgreSQL execution, final clean verification and unchanged exact CI inventory pass for this author candidate; no gate advancement or production acceptance is claimed.

| Accepted finding | In-place test-evidence fix |
| --- | --- |
| `PA27-CR-01` | Existing empty/single/exact-tail IT now obtains a signed nonterminal cursor, preserves and independently verifies its anchor row, deletes all remaining bindings and asserts real HTTP 200, empty items, explicitly present null nextCursor and false hasMore; exact shape/no-store checks remain. |
| `PA27-CR-02` | Existing exact configuration test asserts customerQueryDsl PREPARED_STATEMENT, executeLogging=false, INDEXED and the service's exact customerQueryDsl qualifier, retaining all transaction/bean/lazy assertions. |
| `PA27-CR-03` | Existing service methods independently reject null result/row/IDs/time, non-microsecond time, invalid masks/UTF-8/enums, duplicate bindings, over-limit row count and invalid ordering/seek. Activation null/foreign/corrupt projections and real duplicate SQL projections fail as INTERNAL without a cause. Existing defaults/Cartesian/exception cases remain; single-subject deduplication and one bounded 101-subject batch are verified. |
| `PA27-CR-04` | Existing key-replacement method requires exact CURSOR_INVALID, then uses the actual composition service to produce a signed limit=1 cursor and consume it at limit=100, verifying fetch bounds 2/101, exact next binding and terminal metadata. Existing scope roundtrip remains. |
| `PA27-CR-05` | Existing PA25 method injects one forbidden owner-issued number_hmac grant (262 tuples), independently rejects apply/check without ACL/catalog repair, restores in finally, asserts exact original catalog/ACL, and passes check without subsequent changes. All fourteen removed-tuple probes/history mutations/final success assertions remain. |

Exactly six repository paths change; four test paths retain every existing method name/count (8/8/18/8), helpers/setup and original assertions, except strengthening the one wrong-key type check to exact code. No production Java, SQL/V001-V015, OpenAPI, wiring, fixtures/helpers, CI/POM, plans/fix history, reviews, Control or unrelated docs change. The rejected rename and historical EOL rewrites are not implemented.

| Changed path | Current raw SHA256 |
| --- | --- |
| `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java` | `bb5d702b29f02a37d3fb1bb5ec06b55abc71b2c5ad08a7e91123029477dd06aa` |
| `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorCodecTests.java` | `3b5562d71f2a3e2b8ddaf85d12582c78123cb117a23f84c2f4ddda7f2f4f11b1` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java` | `17d3f1bcaec6a1970a78f89512db2fe0aa4321e4b9903101d9e78d94cd5b8a38` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java` | `5aed37155f520653d7f1efc74636df16fd89c4a282ffeba2ef67ef590f5c7552` |
| `docs/gateflow/pa27-customer-business-number-read-foundation-s1-implementation.md` | Final hash is external in delivery.sha256/scope-audit.json to avoid self-reference; original report bytes remain the exact prefix |
| `docs/gateflow/pa27-customer-business-number-read-foundation-code-review-fix-1.md` | Final hash is external in delivery.sha256 and the sealed evidence manifest |

Fresh focused-unit rerun exits 0: two classes/16 cases, zero failures/errors/skips. Focused PostgreSQL exits 0: those 16 Surefire cases plus five Failsafe classes/122 cases (endpoint18/privilege8/PA25 twenty/PA26 twelve/baseline64), all zero failures/errors/skips. The initial focused-unit attempt exits 1 at compilation because the new anchor assertion used an unsupported helper overload; only that newly added assertion is corrected to an inline bound prepared statement. The initial failure remains durable, not relabeled as success. An intervening external audit-script `.git` inclusion mismatch is corrected without any repository metadata edit and its failed log is retained.

| Fresh final validation | Observed result |
| --- | --- |
| `final-clean-verify` | Exit0; exact `-B -ntp clean -Pcontainer-smoke verify`, elapsed15:49, finished2026-11-11 16:30:49 +08:00; Surefire40/341 and Failsafe17/272, zero failures/errors/skips |
| `final-xml`, `exact-ci-inventory`, `measured-inventory` | Exit0 each; exact source=CI=XML **40/341 +17/272 =57/613**, scan **614**, **seventeen includes**, every historical checkpoint/name/duplicate/XML assertion retained; full identities in source-ci-inventory.json and xml-measured.json |
| `frozen-protected`, `frozen-final` | Exit0 each; all fifteen raw plan-frozen OpenAPI/V001-V014 hashes, unchanged V015 SHA2561697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3 and663 protected history/CI/POM/Control entries verified |
| `scope-audit-final`, `prepare-seal` | Exit0 each; exactly six allowed paths, 1,783 other intake files unchanged, original report retained byte-for-byte as prefix, modes/methods/setup/helpers/assertions preserved, UTF-8/LF checks pass; final six-path hashes and copied reports in delivery.sha256, scope-audit.json and sealed delivery/ |

All57 final XML reports and focused execution XML/summaries are retained separately, with no stale full-report substitution. The exact current dedented CI checker SHA256 is `71d575bfdfae93b3000f67d005af3d04a1f801024bf9ddd2ce25e9e6cfd9292f`; no assertion or inventory change occurs. The externally verified evidence-manifest.sha256 seals commands/exits/timestamps/logs/scripts/intake/reports/inventories/final delivery copies. Full commands and per-finding guard matrix are in the new bounded artifact. Ordinary Linux Git cannot resolve the preserved Windows gitdir pointer; independent whole-file manifests prove scope without inventing a clean Git/remote assertion.

Evidence retains exact commands/exits/timestamps/logs, focused/full XML copies and summaries, intake hashes/modes, original reports/test sources, exact current CI checker and final raw frozen/protected/delivery manifests. Maven uses the unchanged pinned 3.9.9 wrapper through an external LF execution copy, `/opt/jdk21` Temurin 21.0.13+9, Docker 29.1.3 and real PostgreSQL 16.15 (`postgres:16` digest `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`). No test-skipping/ignoring flag or checker weakening occurs.

Residual ownership is unchanged: Security/Operations for required cursor-key deployment/custody/rotation; Security/Database for column-grants-not-RLS and tenant filtering; Backend/Architecture for deferred qualified-DSL inversion; Backend/Database for representative large-set ordering EXPLAIN; Platform/Database/Security/Operations for Q17/Q23, migration/comment custody, real credentials/TLS/pg_hba and pool/old-session rotation. Per-request repeatable-read is not a cross-page snapshot or linearizable revocation claim. Hosted CI, deployment and production acceptance are not claimed. No new production risk or capability is introduced by these test-only changes.

Stop at the bounded author report. No stage/commit/push, branch/PR mutation, merge/approval/mark-ready/reviewer request, external comment or re-review entry occurs. Controller owns any next gate decision; production **NOT EXECUTED / NOT CLAIMED**.
