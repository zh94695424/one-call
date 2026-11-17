# PA37 Operations Catalog Product Read Foundation — S1 Implementation Evidence

Task: `pa37-operations-catalog-product-read-implementation-s1-gpt-20261117-01`
Date: 2026-11-17 (Asia/Shanghai)

Latest bounded fix: `pa37-operations-catalog-product-read-aggregate-fix2-gpt-20261118-01`, 2026-11-18. Section 11 records the accepted Set-membership optimization, derived production digest and fresh 68/68 result. Sections 1–10 are historical runner evidence; section 11 distinguishes Controller's superseding pre-fix runtime evidence from the post-fix checks actually run here. No later-gate transition is claimed.

Bounded fix follow-ups: `pa37-operations-catalog-product-read-code-review-fix1-gpt-20261117-01` and `pa37-operations-catalog-product-read-code-review-fix2-gpt-20261117-01`, 2026-11-17. Section 8 retains the two accepted Low fixes and their historical stale-guard failure; section 9 records the separately authorized digest/evidence lockstep correction. Section 10 records `pa37-operations-catalog-product-read-aggregate-fix1-gpt-20261117-01`, the canonical-LF assertion and existing-identity test hardening, including the current runner's Docker blocker. Sections 1–9 retain their historical results except for the current source hashes/digest below. No full-repository green validation or later-gate acceptance is claimed.

## 1. Result and stop status

The two authorized GET routes and their eight-file production family are implemented. All 60 PA37 unit/architecture methods pass on the final sources. The accepted 24-path write boundary is retained. **Validation is NOT fully green; no implementation acceptance, runtime PostgreSQL proof, hosted-CI pass, production readiness or later gate is claimed.** Stop at implementation/evidence delivery. No delegation, staging, commit, branch creation, push, PR, review, approval, mark-ready, reviewer request, external comment, merge or close action occurred.

The remaining failures are recorded rather than hidden: PostgreSQL16 cannot start without an accessible Docker socket; this sandbox denies server sockets; an unchanged PA35 negative oracle is CRLF-sensitive; A2's unchanged A0 parser does not accept this checkout's CRLF headings; A5's child-process probe receives EPERM. No forbidden file was edited to work around these conditions.

## 2. Binding inputs and immutable sources

- Accepted plan SHA256: `1fb2216b4046b0ca21ad03643fcc6a4a0d5037e912e7feb60d266df390812486` (33,356 bytes / 435 lines), verified before implementation and again afterward.
- Accepted checkpoint `1f7d745d2ad6739efb27814f56d04aa707af540c` is an ancestor of intake HEAD `b8a2ba7c3d3ea7ec4ffc16053b7cabded288f897`. Read-only Git inspection used an explicit Linux gitdir because `.git` points to `D:/codex-call/.git/worktrees/codex-call24`; `.git` itself is unchanged.
- Control, fixes 1–3, all six effective plan-review artifacts, OpenAPI, V002/V004 and the current Operations/POM/CI sources were inspected. The plan is not redesigned.
- `OperationsReadContext.java` remains byte-identical: SHA256 `75c2e3b4b3cc251e8fe43223512be89e510c711059bf280ffbd466a9848adcdf`. Its existing `Role.ORDER` is reused, with no PA37 branch or token added.
- Baseline raw hashes were captured in `/tmp/pa37-baseline.json`; comparison identifies only the five permitted pre-existing edits. Frozen contracts, migrations, docs12/14/15/17, accepted plan/fixes/reviews, Control and unrelated sources remain unchanged.

### Accepted artifacts: raw SHA256

| Path | SHA256 |
|---|---|
| `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md` | `1fb2216b4046b0ca21ad03643fcc6a4a0d5037e912e7feb60d266df390812486` |
| `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-1.md` | `2f539bcd7e22fac208a2bafe51f29c27499df57c79c34725ae6c08ce1da1ecb4` |
| `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-2.md` | `b0cc4a98dce4b692ba402ea72dc3a10bd0ab1f2c96a6e0f465e5c266543efec7` |
| `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan-fix-3.md` | `25d34ebf31aa30e70e58bb2214452fe40752f4cbb90a3f8bb6ed4454c57ed68f` |
| `docs/reviews/plan-review-20261117-180736.md` | `7882c355a46664db70031954f73a446bec0c5234282998d5b77d5a3975f3fe0e` |
| `docs/reviews/plan-review-20261117-182054.md` | `f04ef05bdbc9f9d5164fa91c03690042ca7c82538379db18c39bbd96f9ae4590` |
| `docs/reviews/plan-review-20261117-193036.md` | `33866cbc034e9490af6317ab9223c679a4820ae51e9ac312125e5fb91045bca4` |
| `docs/reviews/plan-review-20261117-193924.md` | `6ef89a623a50b7998a03f10e3fc2860273da94d1f76040e090aafd868608c19c` |
| `docs/reviews/plan-review-20261117-202154.md` | `08e0eb1a1bd6ccacceb5eb0041e953b2205026f29feca4d97c417faf35aaef70` |
| `docs/reviews/plan-review-20261117-203234.md` | `0fe4ad11beff20eaec024fc3a03f243860d8d83b3f92a978bd8e2f60671660d3` |

## 3. Exact changed-path manifest

Exactly 24 allowlisted paths: five existing files, eight production Java files, ten test/helper Java files and this evidence document. The report is excluded from every code, test, CI, POM and manifest inventory. Hashes below are raw source bytes, not normalized hashes; the evidence document is not self-hashed.

| Changed path | Raw SHA256 |
|---|---|
| `.github/workflows/ci.yml` | `0bdd65d490a562eebedb80cc594d2480bb3314f2e0e14530d79243a3278d8c18` |
| `backend/pom.xml` | `3e843c4104af4ccba76a6f1a2cfa39fcb1ebe9953a4c8e7067edf2e76e250661` |
| `backend/src/main/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsCatalogProductReader.java` | `629276bf5cfd312ca230d0803d8e4f83936610a829a6218b07b8712f22e46699` |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | `470016fda10ae46a8fe2d04e84c57611d7438fa8166c4c3932cb861f3e37ad81` |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/DefaultOperationsCatalogProductReadService.java` | `f7702095e835cbf0ba77754066f2804a7d45d9fbe804c4942eeb68118a0c1895` |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductCursorCodec.java` | `5603beef7008632541e40ea243b1703db1ec8785f20454d5082e2fa8b6cf1746` |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadConfiguration.java` | `8d9884abb85058a64d430a793b3243c3f1e2a5579028d787ffb5a3334beb451d` |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadRequest.java` | `ecc196a8a24e93eb51cc5a93cc8ae1baf38a610ff28910e03d09b8f88c593abb` |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java` | `df487739146661827f8275cde6c4b477068e3615c4c10a425bd7a51fd29d6df8` |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogProductReadController.java` | `dbe57748fad19dfc2b04d5d7add9f7ad613333020bdfe75d84fd3a9a8b388e9f` |
| `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsCatalogProductReadPorts.java` | `ed8da079f7c9e06ba7132fa85bbabc966a69110572a26a6500017cc627348cc7` |
| `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsCatalogProductReadService.java` | `86c5b73e86ae930fe622782caa8d4d55672329a305dc89415e28bbd93cb7bccb` |
| `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadConfigurationTests.java` | `d84e5bf5a4af8585522f0d27eced2b0f833fc498cce662034eb62c25873cc4a0` |
| `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadWebTests.java` | `804d450c6e45c689dce1271d966391f8ca96401e7453526f41644bed90ced1d9` |
| `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | `5bce4981478df1a07cda971dcd3ee6cd5807b3ffcd16a18b0d5d10e90cc265d8` |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogProductReadBoundaryTests.java` | `cabb60e414aabc8915843e9b9353915277abe87b5735ba8e570cb560034a8123` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadFixture.java` | `3f128bdcc9546c7fa0a6176a3c0568e68c082a7c3c433a34e3c3729da54d51a2` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadRoleLeastPrivilegeIT.java` | `39912f6b39a6d0fc695b41c685ea7685b2ec0eb2423224515f66537e4d360971` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadsIT.java` | `2f15ac06a329d3ab3f963a6efed243b6c814f66bc837fa389414169df966a152` |
| `backend/src/test/java/com/onecall/archtests/operations/OperationsCatalogProductReadTestProviders.java` | `a9ede2aa8cfcb07b05b04cb8f41f142e50719b4703ed8c735d60aae0e5c3af98` |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductCursorCodecTests.java` | `6e2c8d040d64788c360cbf2de999e768910f40b88914536c50d1e7f6730c0c76` |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadRequestTests.java` | `116652e06b69635b6f895e1fb6a4e21a4d686bc1ef41f79adf812ea2b1b56e0f` |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadServiceTests.java` | `a0bd7f0bee475675d9f593292eb4b99d8c21a69b3deb95669dab134723c93315` |
| `docs/gateflow/pa37-operations-catalog-product-read-foundation-implementation.md` | Evidence document; excluded from inventories and self-hash. |

The only existing architecture-test changes are manifest count/digest and the three current CI class/include mirrors; its universal production-tree coverage and existing migration exception are preserved. All eight new production paths participate in the manifest; no PA37 exclusion is added.

## 4. Exact inventories

| Inventory | Accepted baseline | Final source/CI/POM expectation |
|---|---:|---:|
| Unit/architecture classes / methods | 83 / 788 | 89 / 848 |
| Container classes / methods | 34 / 556 | 36 / 592 |
| Combined classes / methods | 117 / 1,344 | 125 / 1,440 |
| Annotation line scan | 1,345 | 1,441 |
| Scoped Failsafe / all-POM includes | 34 / 36 | 36 / 38 |
| Guarded production manifest / raw tree | 291 / 292 | 299 / 300 |
| Main Java / test Java files | 271 / 159 | 279 / 169 |

Baseline manifest digest: `95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0`.
Implementation-stage manifest digest: `2b042c0b3356ec7f5c5ef1c94b9675473ac40fc16eebba5734d07c407230f349`.
Current post-fix manifest digest: `12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1` (299 guarded files, unchanged count). The guard now pins this current digest; prior digests are historical only. Section 8 preserves the fix1 failure evidence; section 9 records its bounded lockstep closure; section 11 records the aggregate-fix2 production change and independently derived lockstep digest.
Algorithm is unchanged: every regular file under `src/main/java` and `src/main/resources`, retaining only the existing V018 exception separately guarded by its checksum; each row is relative path, one space, SHA256 of CRLF-to-LF-normalized bytes; sort all rows, join with LF and append final LF, then SHA256. The unchanged context and all eight PA37 production files are included.

### Ten exact test/helper identities

| Exact path | Exact FQCN | Methods |
|---|---|---:|
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadRequestTests.java` | `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests` | unit 8 |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductCursorCodecTests.java` | `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests` | unit 8 |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadServiceTests.java` | `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests` | unit 18 |
| `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadWebTests.java` | `com.onecall.archtests.OperationsCatalogProductReadWebTests` | unit 10 |
| `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadConfigurationTests.java` | `com.onecall.archtests.OperationsCatalogProductReadConfigurationTests` | unit 8 |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogProductReadBoundaryTests.java` | `com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests` | unit 8 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadsIT.java` | `com.onecall.archtests.container.OperationsCatalogProductReadsIT` | container 20 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadRoleLeastPrivilegeIT.java` | `com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT` | container 16 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadFixture.java` | `com.onecall.archtests.container.OperationsCatalogProductReadFixture` | helper 0 |
| `backend/src/test/java/com/onecall/archtests/operations/OperationsCatalogProductReadTestProviders.java` | `com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders` | helper 0 |

The six unit dictionaries have lengths `[8,8,18,10,8,8]`; the two container dictionaries have lengths `[20,16]`. Both helpers have zero test identities, are absent from CI method dictionaries and are not selected by Maven. The exact plan method sets are mirrored in `pa37_unit_additions` and `pa37_container_additions`, merged into the existing expected dictionaries, and checked against source. No conditional, repeated or parameterized test identities are introduced.

Current CI literals are `[848, 592]`, sum1440, line scan1441, unit classes89, container classes36, scoped includes36, XML848/592 and the earlier Surefire aggregate848. The real PA37 historical subtraction is `[788,556]` / sum1344 / scan1345. Existing historical values and mechanisms, including PA34 full-match removal, are unchanged:

```text
baseline 112+178=290 scan291
PA19 128+192=320 scan321; PA20 162+198=360 scan361
PA21 196+204=400 scan401; PA22 231+214=445 scan446
PA23 266+214=480 scan481; PA25 309+214=523 scan524
PA26 309+234=543 scan544; PA27 309+246=555 scan556
PA28 341+272=613 scan614; PA29 375+302=677 scan678
PA30 417+336=753 scan754; PA31 468+370=838 scan839
PA32 520+404=924 scan925; PA33 574+440=1014 scan1015
PA34 644+476=1120 scan1121; PA36 704+504=1208 scan1209
PA35 712+520=1232 scan1233; PA37 788+556=1344 scan1345
current 848+592=1440 scan1441
```

`pa37_names` is subtracted from earlier method totals and source/line scans and included in the baseline/additions union. Current mirror literals89/36/36 replace83/34/34; no fictitious historical class/include assertions are introduced. POM changes are exactly the two specified PA37 Failsafe includes; no dependency block changes.

### Actual XML, distinguished from expected inventory

- Final PA37 focused XML: six classes /60 methods, zero failures/errors/skips; the original CI `exact_inventory` function succeeds for that six-class dictionary.
- Canonical full Surefire XML:89 classes /848 methods, **1 failure,64 errors,0 skips**. Every unit class/method identity matches the full CI unit dictionary, but this is not a green XML gate. All six PA37 classes/60 methods pass in this full run too.
- Targeted Failsafe XML: two classes /two lifecycle-initialization errors, zero test-method execution. These are NOT36 PA37 method passes and do not satisfy the expected36-class/592-method full container inventory.
- The complete CI reconciliation body fails honestly at missing `TEST-*PostgresContainerIT.xml`. Its unchanged static source/history/token/POM portion passes independently. No missing or green report is fabricated, no disabled test is introduced, and failed reports are not relabeled as successful inventory.

## 5. Implemented behavior and evidence boundaries

### Admission, authority, filters and errors

The two boundary filters admit only the exact list/detail GET family; all other methods and URI shapes remain concealed. The web-profile controller uses only the service port. The service enters the existing transaction/context, then executes exactly:

```sql
SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)
```

Only current ACTIVE ADMIN/OPS rows with valid identity/revision are accepted. Wrong audience401, disabled403 and malformed persisted authority503 win before request parsing or cursor-ring resolution. The service-harness parser test does not move authority into the parser. The explicit ring×authority matrix verifies lazy ring suppliers are not invoked on authority failure; detail succeeds with no ring provider.

Only single `cursor`/`limit` list parameters are allowed, default limit20 and bounds1..100; detail rejects query parameters and noncanonical UUIDs. Accept negotiation copies shipped PA35 rules. Blank cursor400, malformed request422, authenticated filter mismatch400, detail absence404, declared429, dependency/configuration/timeout/ACL/transaction/cleanup503 and unexpected500 remain sanitized. Frozen ProblemWriter behavior is reused; no limiter is added.

### DTOs, projection and two-phase paging

List exposes only product identity/code/family and the five approved version-summary fields. Detail adds only the frozen price/currency/billing/installments/benefit/revision/published-at fields. DRAFT publishedAt and QUOTE priceCents are explicitly serialized null; arrays never null; tail nextCursor explicitly null. Non-CNY and inconsistent approved detail facts fail503.

Owner-local SQL reads only approved columns of `catalog_product`, `catalog_version` and `catalog_benefit_line`. Internal grouping keys `product_id`, `catalog_version_id` and benefit `id`, plus `product_family`, are structurally pinned. No SELECT star, offer read/join/EXISTS, snapshot, creator, order/payment/provider, ciphertext or media projection is added.

Root SQL uses `ORDER BY product_code COLLATE "C" ASC, id ASC`, limit+1 and the exact typed exclusive `(product_code COLLATE "C", id) > (CAST(? AS text) COLLATE "C", CAST(? AS uuid))` anchor. The lookahead root is removed before phase two. Only emitted IDs are passed to the summary query, ordered `product_id,version_no,id`; attachment preserves root order and zero-version roots. Java uses UTF-8 unsigned byte comparison plus unsigned native UUID comparison. Detail versions/benefits are deterministically ordered. Unit tests prove phase-two ID selection and filter mismatch before any reader call. PostgreSQL fixtures cover AB/A-1/A_1/A0, multi-version boundaries, empty/exact/tail/1/100 pages, offer-less drafts and multi-channel invariance, but their database assertions remain unexecuted here.

### Cursor envelope and rotation

AES-GCM framing is version1, one-byte key-id length, ASCII key-id1..32, random nonce12, encrypted payload and tag16. Header bytes are authenticated. AAD is length-prefixed purpose `onecall:operations-catalog-product-list:cursor:v1`, exact GET endpoint, exact sort and complete header. Plaintext is purpose/version1, big-endian limit4, digest prefix16, UUID16, code-length1 and ASCII code2..64.

The request digest independently tests the presence-aware LP(fieldName), presence byte, uint32 length and value bytes for purpose/endpoint/sort/normalized limit. Omitted and explicit20 match; transport cursor is excluded. Authentication and limit/digest comparison precede catalog access.

Shared ring validation is reused. Encode chooses the active key; decode uses only the addressed key-id. Retained old keys work across rotation; removed/unknown IDs400 are distinct from malformed/missing/ambiguous ring503. Tests exercise round-trip/outer layout, header/nonce/tag tampering, wrong purpose/endpoint/sort, malformed lengths/code/trailing data, limit and independent digest mismatch, and rotation. Maximum102-byte plaintext +62-byte maximum framing =164 bytes =219 unpadded base64url characters, below512; the max-key/max-code round-trip test proves this bound. Cursors are anchors, not snapshot tokens.

### Transaction, ACL and configuration

The existing qualified datasource, DSL, context and REQUIRES_NEW read-only REPEATABLE_READ transaction template are reused. The owner-local component is named `operationsCatalogProductReader`; both owner reader and Operations configuration use the exact persistence profile/enabled-property/class-level lazy pattern, and the service bean is lazy. Operations imports only its own port, not the catalog concrete reader. Existing module-boundary tests pass; no production datasource/pool/template/property/resource/module edge is added.

Role handoff uses unchanged `context.scoped(Role.ORDER, ...)`, restoring operations_iam_role and then the login. Existing context guards enforce same connection/backend/login, read-only/isolation, statement2s/lock250ms/idle5s timeouts and cleanup. Unit tests prove transaction use/exit on success/failure; database backend/role/snapshot/cleanup assertions are compiled but not observed without PostgreSQL16.

The direct V004 test queries catalog ACLs with aclexplode and expects42 tuples:14 table SELECT/INSERT tuples across seven tables,23 selected-column UPDATE tuples (including sensitive pre-existing version columns), four routine EXECUTE tuples and one schema USAGE tuple, with exact grantor/no grant option. It separately checks cross-schema V004 USAGE, platform/audit EXECUTE and exact cross-module SELECT column sets. DELETE is not granted. The existing410-tuple PA36 oracle is checked separately for its own monitored grantees; it is not misrepresented as the catalog_order_role oracle. Synthetic fixture-only login membership is not a production grant/schema change.

INSERT/UPDATE/DELETE/DDL/FOR UPDATE probes assert read-only SQLSTATE25006, NOT a SELECT-only privilege matrix. PostgreSQL read-only does not deny advisory functions: the test checks backend pg_locks before operations and during success/failure including cleanup, and static production scans forbid advisory use. No advisory-denial claim is made. All these PostgreSQL privilege/lock probes remain unexecuted in this sandbox.

Read tests compare business-table fingerprints across successful nonconcurrent reads; the snapshot test intentionally uses the shared raw GET helper so its independent writer is not mistaken for a read-path mutation. There are no production writes, audit/outbox append, locks, media resolution or snapshot exposure.

## 6. Commands and actual results

Toolchain: `/opt/jdk21` Temurin21.0.13+9, `/opt/maven-3.9.9/bin/mvn`, `/opt/node-v22.23.4-linux-x64/bin/node`22.23.4. Default PATH's Windows java fails under WSL, so the Linux toolchain is explicitly selected. No toolchain or wrapper source is edited.

The sandbox blocks Mockito's dynamic self-attachment. Startup instrumentation uses the already-installed test dependency, not a new dependency or POM change:

```sh
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:/opt/node-v22.23.4-linux-x64/bin:$PATH"
export JAVA_TOOL_OPTIONS=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar
```

### Maven selectors and run ledger

Focused command (run from backend; the standalone passing retry uses this argLine and offline cached artifacts):

```sh
mvn -B -ntp -o -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar -DfailIfNoTests=true -Dtest=com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests,com.onecall.archtests.OperationsCatalogProductReadWebTests,com.onecall.archtests.OperationsCatalogProductReadConfigurationTests,com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests test
```

Result:60/60 pass, zero failures/errors/skips (`/tmp/pa37-focused-agent-2.log`). Final-source repeats also pass60/60 in the isolated targeted verify and final canonical full run. Earlier development retries exposed and fixed import ambiguity, test helper compilation/stubbing and GCM test-IV reuse; pre-agent retry ran60 with36 Mockito initialization errors. They are not represented as passing runs.

Targeted accepted wrapper command:

```sh
./mvnw -B -ntp -Pcontainer-smoke -DfailIfNoTests=true -Dfailsafe.failIfNoSpecifiedTests=true -Dit.test=com.onecall.archtests.container.OperationsCatalogProductReadsIT,com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT verify
```

Result126: unchanged CRLF wrapper shebang resolves `/bin/sh^M`. The equivalent installed Maven3.9.9 command was then actually run:

```sh
mvn -B -ntp -Pcontainer-smoke -DfailIfNoTests=true -Dfailsafe.failIfNoSpecifiedTests=true -Dit.test=com.onecall.archtests.container.OperationsCatalogProductReadsIT,com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT verify
```

Result1: its full848-method Surefire prerequisite stops with the unchanged oracle/socket failures before Failsafe (`/tmp/pa37-targeted-it-mvn.log`). Supplemental isolation, without skipping the selected unit tests:

```sh
mvn -B -ntp -Pcontainer-smoke -DfailIfNoTests=true -Dfailsafe.failIfNoSpecifiedTests=true -Dtest=com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests,com.onecall.archtests.OperationsCatalogProductReadWebTests,com.onecall.archtests.OperationsCatalogProductReadConfigurationTests,com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests -Dit.test=com.onecall.archtests.container.OperationsCatalogProductReadsIT,com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT verify
```

Result1 (`/tmp/pa37-targeted-isolated.log`): final selected units60/60 pass; executable JAR packaging succeeds; both Failsafe classes fail their BeforeAll Docker initialization. Docker itself reports permission denied at `/var/run/docker.sock` (symlink to `/var/run/dify.sock`). No PostgreSQL test method executes; no runtime ACL, snapshot,25006 or role-restoration pass is claimed.

Canonical command:

```sh
mvn clean -Pcontainer-smoke verify
```

Final result1 (`/tmp/pa37-canonical-full-2.log`):89 unit classes /848 methods,1 failure/64 errors/0 skips; all PA37 units pass. An earlier clean attempt caught a redundant local variable in the enhanced codec test; it was fixed and the canonical command rerun. The final failure is not a compilation failure. Failsafe/full-container execution is not reached in the canonical run.

Unchanged failures: `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact:207` expects a negative mutation to throw, but its `originalPlan.replace(bareLine + "\n", "")` is a no-op against1344 CRLF lines in the unchanged PA35 plan. Raw-byte diagnostic confirms bare-LF match=false, bare-CRLF match=true and mutation leaves the string identical. The64 errors arise from server socket `java.net.SocketException: Operation not permitted` and subsequent Spring context failure-threshold errors. Neither test nor frozen plan is edited.

### OpenAPI and repository CI bodies

```sh
cd api/openapi && npm ci && npm run check:s3
```

Result0 with Node22.23.4, `NPM_CONFIG_CACHE=/tmp/pa37-npm-cache` and `NPM_CONFIG_OFFLINE=true` using copied pre-existing cache. Locked install succeeds; no lockfile/dependency changes. OpenAPI s3 succeeds:49 operations, counts18/21/6/3/1, doc/A0 inventory drift0 (`/tmp/pa37-openapi-check.log`). Offline audit output is not a fresh vulnerability-assessment claim.

The exact Python heredoc bodies are extracted from current CI into `/tmp`, not rewritten in repository files:

```sh
cd backend
python3 /tmp/pa37-ci-inventory.py
python3 /tmp/pa37-ci-source-inventory.py
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
mvn -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt
python3 /tmp/pa37-ci-firewall.py
python3 /tmp/pa37-ci-smoke.py
```

- Full inventory body:exit1, missing PostgresContainerIT report; XML gate is not green (`/tmp/pa37-ci-full-inventory.log`).
- Static-only slice of that body:exit0, all source/history/token/POM assertions pass, printing `[848,592] 1441 89 36 36`; additionally the original exact_inventory function passes focused6/60. This is explicitly narrower than the full CI body.
- Runtime dependency list + exact firewall body:exit0, baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1, deltas35/0 and31/0,116 adversarial probes (`/tmp/pa37-firewall-explicit.log`).
- Exact packaged-JAR smoke body:exit1 after default non-web JAR startup succeeds; web JVM exits before startup because socket creation is denied (`/tmp/pa37-smoke-explicit.log`, `backend/target/boot.log`, `backend/target/web-boot.log`). Later web assertions are not reached. JAR was produced by the successful packaging phase of isolated targeted verify, preserved before canonical clean and restored byte-for-byte only for these checks; SHA256 `580c0babb54196626ff4828714eb651ca23ccecd5f812e58706e5d5276e44e1a`,39,886,931 bytes. This is not evidence that canonical verify passed.

The existing contract-regressions CI run bodies are also executed individually, preserving their exact flags even after A2 fails:

**A2 s3** — exit1; log `/tmp/pa37-A2-s3.log`.

```sh
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
```

**A3 s3** — exit0; log `/tmp/pa37-A3-s3.log`.

```sh
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures
```

**A4 s2** — exit0; log `/tmp/pa37-A4-s2.log`.

```sh
node deployment/capacity/validate.mjs --stage s2 \
  --template deployment/capacity/inputs.template.json \
  --fixtures deployment/capacity/fixtures
```

**A5 s1** — exit1; log `/tmp/pa37-A5-s1.log`.

```sh
node acceptance/validate.mjs --stage s1 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures
```

**A5 s2** — exit1; log `/tmp/pa37-A5-s2.log`.

```sh
node acceptance/validate.mjs --stage s2 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures \
  --check-doc-links
```

A2 s3 fails with A0 status drift8, registry drift6 and column drift40; all25 fixture cases pass but the overall gate fails. Its unchanged `parseA0Columns` heading regex is anchored before CR and does not strip CRLF; no validator or docs14 patch is authorized or made. A3 s3 passes23 fixtures with drift0. A4 s2 passes113 fixture cases and471 nonfinite probes, with all deployment-readiness outputs remaining false. A5 s1 and s2 fail `A5_DETERMINISM_FAILURE ... two-probe`: an independent `spawnSync(process.execPath,['--version'])` reproduces `error.code=EPERM` (even while status0/stdout are available), and the existing gate correctly rejects any run.error. No probe bypass or weakened determinism gate is added.

Read-only scope/whitespace checks: `git diff --check` passes on the five existing edits; raw baseline comparison excludes any other existing path change; all18 migrations remain unchanged; index comparison is empty. Git warns about this Windows checkout's line-ending policy but does not modify it. Generated target/node_modules/tmp artifacts and temporary logs/scripts are not added to the source manifest or staged.

Hosted CI, remote status, production credentials/provisioning and any external action are NOT EXECUTED. Logs under `/tmp` are local, ephemeral evidence, not committed artifacts.

## 7. Residual owners and handoff boundary

| Residual | Owner / disposition |
|---|---|
| PostgreSQL16 execution, full XML reconciliation, web packaged-JAR smoke and hosted execution | CI/release + Platform; rerun in an authorized environment with Docker and socket access; no pass claimed here. |
| Existing CRLF-sensitive PA35 oracle/A2 parser and A5 child-process restriction | CI/Backend + Platform; use the canonical LF-capable execution environment or a separately authorized follow-up; frozen inputs/validators/tests are not widened in PA37. |
| Existing broad catalog_order_role grants | Database/Security + Backend; **deferred-with-owner**, unchanged. Read-only transaction is not a SELECT-only grant boundary. |
| Docs15 offer-summary wording without a public field | Contract/Data + Catalog-order owner; no offer query or invented field. |
| Unbounded per-product version arrays, large history and cursor capacity | Capacity/Database + Catalog-order owner; roots are bounded, history remains unbounded as accepted. |
| C collation, timeouts, dependency/key delivery | Platform/Security + Backend/Database; implementation uses frozen shared plumbing; runtime assertions await PostgreSQL16. |
| Authority-first admission and declared429 without an implemented rate limiter | Operations IAM/Backend; precedence tested; limiter remains out of scope. |

No unreported pass, schema/security expansion or later-gate action is authorized by this document. Implementation files and honest non-green validation evidence are delivered; runtime/release acceptance remains blocked on the stated residuals.

## 8. Bounded code-review fix 1 — 2026-11-17

Task label: `pa37-operations-catalog-product-read-code-review-fix1-gpt-20261117-01`. Gate: `fix` only. Before editing, read the PA37 Control checkpoint, accepted plan, both independent code-review artifacts (`code-review-20261117-220345.md` and `code-review-20261117-221604.md`), this implementation report and the shipped IVR service reference. No delegation or external action was performed.

### Accepted findings fixed

- **PA37-CODE-REV-01 — fixed:** retained the existing public constructor, delegating with `System::nanoTime`, and added a package-private `LongSupplier nanoTime` constructor. The five-second deadline is passed into list/detail work. Checks run before context entry, after entry, after authority, before/after each catalog reader phase and before cursor encode when needed. There is no deadline check after the action returns or after the transaction commits. Shared transaction settings, authority precedence, SQL, DTOs, ring configuration and context remain unchanged; unrelated IVR behavior was not copied.
- Extended only the existing service identity `usesOneRepeatableReadTransaction`: deterministic clocks expire at all nine list/lookahead boundaries and all nine detail boundaries, asserting exact reader short-circuiting, authority/context entry boundaries, cleanup, rollback and no commit. Four success regressions (list/detail × expiry during exit/commit) prove completed work is not reclassified; the returned lookahead cursor still authenticates. Existing assertions remain intact; the service class still has exactly 18 methods.
- **PA37-CODE-REV-02 — fixed:** `success` calls `response.setContentLength(body.length)` before writing serialized bytes. Existing web identities `admitsExactListGet` and `admitsExactDetailGet` now assert the actual successful `Content-Length` header equals the emitted byte count, retaining their original status/reader assertions. The web class still has exactly 10 methods.

### Exact fix delta and custody

Only these five existing candidate paths changed relative to fix intake; the four source hashes are also updated in section 3:

| Fix-changed path | Post-fix raw SHA256 |
|---|---|
| `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/DefaultOperationsCatalogProductReadService.java` | `53641a7d90284f4a8ee5160f93852576b140101f8f85fcd69174b92210661602` |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogProductReadController.java` | `dbe57748fad19dfc2b04d5d7add9f7ad613333020bdfe75d84fd3a9a8b388e9f` |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadServiceTests.java` | `a0bd7f0bee475675d9f593292eb4b99d8c21a69b3deb95669dab134723c93315` |
| `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadWebTests.java` | `de43e3374813c6661e1354ff0695730e95632f910af7b570432f6f8f9af4eb65` |
| `docs/gateflow/pa37-operations-catalog-product-read-foundation-implementation.md` | This evidence document; no circular self-hash. Its raw hash is emitted in the local scope-check output. |

Read-only fix-intake snapshot `/tmp/pa37-fix1-baseline.json` captured hashes of 2,226 public files and all 1,440 test identities. Final scope comparison proves the exact same 24 public candidate paths against Git and the accepted plan/report manifest, with only the five authorized fix paths changed. Controller-owned pre-existing Control/review artifacts and private helpers are excluded from the candidate set, not treated as new product files. All snapshotted public files outside the five paths remain byte-identical, including Control, accepted plan/fix/review history, OpenAPI/project contracts, all resources and V001–V018, POM/CI, other tests and `OperationsReadContext`; no V019 or new public path exists. No `.codex` or `.pa37-*` helper was modified.

Every frozen test method identity, not just its count, equals the intake snapshot. PA37 remains six unit/architecture classes with `8+8+18+10+8+8=60` methods, two container classes with `20+16=36` methods and two zero-test helpers. Global inventories remain `89/848 + 36/592 = 125/1440`, annotation line scan `1441`, PA37-start `788+556=1344` / scan `1345`, scoped Failsafe/all-POM includes `36/38`, guarded production/raw tree `299/300` and main/test Java files `279/169`. No test annotation or method was added, removed or renamed; no assertion was weakened or removed. CI/POM inventory bytes are untouched.

### Actual commands and results

All Maven commands use the Linux WSL exports recorded in section 6, including `/opt/jdk21`, `/opt/maven-3.9.9/bin`, `/opt/node-v22.23.4-linux-x64/bin` and the Mockito 5.23.0 `JAVA_TOOL_OPTIONS` agent. Commands run from `backend`:

```sh
mvn -B -ntp -DfailIfNoTests=true -Dtest=com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests,com.onecall.archtests.OperationsCatalogProductReadWebTests,com.onecall.archtests.OperationsCatalogProductReadConfigurationTests,com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests test
```

- Initial focused run: exit 0, 60 tests, zero failures/errors/skips, `/tmp/pa37-fix1-focused.log`. After making the pre-encode check conditional on actually encoding, reran the same six-class command on final source bytes: **exit 0 / BUILD SUCCESS, 60 tests, zero failures/errors/skips**, 01:02 min, finished `2026-11-17T22:27:43+08:00`, `/tmp/pa37-fix1-focused-final.log`. Independently parsed the six generated Surefire XML files: aggregate `60/0/0/0`; these are focused unit results, not PostgreSQL evidence.
- From workspace root, `python3 /tmp/pa37-fix1-scope.py after`: exit 0; exact 24-path candidate/five-path fix boundary, unchanged exact identities, `89/848 + 36/592 = 125/1440` and scan `1441`; `/tmp/pa37-fix1-scope.log`. The initial temporary scope script counted the existing ACL class as a PA37 addition; corrected only that `/tmp` selector and reran successfully, with no product/test change for the check.
- `git --no-optional-locks --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call24 --work-tree="$PWD" diff --check`: exit 0, `/tmp/pa37-fix1-diff-check.log`. Explicit Linux gitdir is needed because the unmodified `.git` points at a Windows path. Existing line-ending warnings are informational. Additional fix-delta whitespace checking covers the untracked candidate files that ordinary `git diff` does not include.

An additional narrowly targeted fix1 check validated the discovered tree-digest consequence, without a repository/container sweep (historical failure, superseded by section 9):

```sh
mvn -B -ntp -DfailIfNoTests=true '-Dtest=com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#v001ThroughV017AndPublicContractsRemainByteFrozen' test
```

**Exit 1 / BUILD FAILURE: 1 test, 1 failure, zero errors/skips**, finished `2026-11-17T22:28:16+08:00`, `/tmp/pa37-fix1-tree-guard.log`. The assertion at line 102 still expects `2b042c0b3356ec7f5c5ef1c94b9675473ac40fc16eebba5734d07c407230f349`; the independently recomputed current 299-file digest is `6d7fa118cbf2580fd492b2446f57f86e5ea808cfe4626ebf015d5c10f334b39e`. Earlier frozen contract/migration assertions in that method pass; the failure is specifically the production-byte digest changed by the two accepted fixes. **This is a newly exposed lockstep blocker, not the PA35 CRLF oracle and not a green repository result.** `EvidenceRoleIvrAssociationAclBoundaryTests.java` is among the original 24 candidate paths but is explicitly outside this fix's five writable paths. It was not edited; a separately authorized exact digest update is required from Controller/Backend before whole-suite green can be claimed. The source count and frozen test identities need no change.

No full repository container sweep, PostgreSQL/Failsafe command, OpenAPI/contract rerun, hosted check, staging, commit, push, PR, merge, approval, mark-ready, reviewer request, external comment or issue action was performed in fix1. Controller's independent rerun of both PA37 PostgreSQL16 classes and resulting XML custody remains pending; no rerun, review acceptance, re-review or later gate is claimed here. Existing capacity, broad-role, contract-wording, rate-limit and environment residuals remain as recorded. The digest blocker was out of scope for fix1 and is resolved only by the separately authorized fix2 in section 9.

## 9. Bounded fix2 — production-tree manifest lockstep

Task: `pa37-operations-catalog-product-read-code-review-fix2-gpt-20261117-01`, 2026-11-17 (Asia/Shanghai). **`PA37-FIX1-INTAKE-01` is closed by this bounded digest/evidence correction.** The latest Control checkpoint, both code-review artifacts, this implementation report and the failing guard source/log were read before editing. No production fix was repeated or redesigned.

Only two repository paths changed in fix2:

| Changed path | Current raw SHA256 |
|---|---|
| `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | `3166b527d255848a908789b1b9412f26293bb1a03c0cb42b3fe7b594d0ba4e5a` |
| `docs/gateflow/pa37-operations-catalog-product-read-foundation-implementation.md` | This evidence document; no circular self-hash. Final raw SHA256 is emitted by the scope command below and returned with the handoff. |

The guard's only byte change is the one digest literal: historical implementation-stage `2b042c0b3356ec7f5c5ef1c94b9675473ac40fc16eebba5734d07c407230f349` becomes current post-fix `6d7fa118cbf2580fd492b2446f57f86e5ea808cfe4626ebf015d5c10f334b39e`. Independent recomputation with the unchanged normalization, path ordering, final LF and existing V018 exception confirms 299 guarded files. Replacing the new literal back in memory yields exactly the intake raw file hash, proving `hasSize(299)` and every other guard byte, migration/contract assertion and method identity unchanged. Section 3 now carries the current guard hash; all 23 non-report changed-path hashes were checked against actual bytes.

### Actual commands and results

Both Maven commands run from `backend` with the Linux toolchain and Mockito agent exports in section 6, without offline/skip flags:

```sh
mvn -B -ntp -DfailIfNoTests=true '-Dtest=com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests' test
mvn -B -ntp -DfailIfNoTests=true -Dtest=com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests,com.onecall.archtests.OperationsCatalogProductReadWebTests,com.onecall.archtests.OperationsCatalogProductReadConfigurationTests,com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests test
```

- Guard class: **exit 0 / BUILD SUCCESS, 8/8, zero failures/errors/skips**, 36.202 s, finished `2026-11-17T22:35:42+08:00`; `/tmp/pa37-fix2-tree-guard.log`. This supersedes only the historical fix1 stale-digest failure, not unrelated repository gates.
- Six PA37 focused unit/architecture classes: **exit 0 / BUILD SUCCESS, 60/60, zero failures/errors/skips**, 18.724 s, finished `2026-11-17T22:36:21+08:00`; `/tmp/pa37-fix2-focused.log`. Generated Surefire XML independently confirms both aggregates and the unchanged exact method identities.

Scope and whitespace commands run from workspace root:

```sh
python3 /tmp/pa37-fix2-scope.py after
git --no-optional-locks --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call24 --work-tree="$PWD" diff --check
git --no-optional-locks --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call24 --work-tree="$PWD" diff --no-index --check -- /dev/null docs/gateflow/pa37-operations-catalog-product-read-foundation-implementation.md
```

Scope check: **exit 0**; `/tmp/pa37-fix2-scope.log`. It reuses the established fix1 exact-scope checker with only the snapshot name and writable-path pair changed, adding literal-only guard verification, independent manifest recomputation, all 23 report hashes and an empty-index check. The before-edit snapshot `/tmp/pa37-fix2-baseline.json` covers 2,226 public files and all 1,440 test identities. The same exact 24-path candidate set matches the accepted plan/report; exactly the two authorized paths changed since fix2 intake, with no added/removed/renamed public path. All other snapshotted files, including Control, plan/review history, CI/POM, OpenAPI, migrations, production sources and other tests, remain byte-identical. All frozen identities remain unchanged: PA37 60 unit/36 container, global `89/848 + 36/592 = 125/1440`, scan `1441`. No new `@Test` or inventory adjustment occurred.

Tracked `git diff --check`: **exit 0**; `/tmp/pa37-fix2-diff-check.log`. The supplemental `--no-index --check` command includes this untracked evidence document, which ordinary `git diff --check` omits; it exits 1 for the expected difference from `/dev/null`, with no whitespace-error diagnostics (`/tmp/pa37-fix2-report-whitespace.log`). Existing checkout line-ending warnings are informational; no line-ending or frozen-input rewrite was made.

### Residuals and stop boundary

No PostgreSQL, Docker or Failsafe command ran in fix2. Controller still owns the post-fix rerun of both PA37 Failsafe classes and resulting 36/36 XML custody. Historical implementation-stage environment failures and the CRLF-sensitive PA35 failure are not relabeled as passes; no full-suite, hosted-CI, runtime-readiness or re-review acceptance is claimed. Existing owner-routed residuals remain unchanged. No staging, commit, push, branch/PR mutation, merge, approval, mark-ready, reviewer request, external comment or issue action occurred. Stop after this bounded lockstep correction.

## 10. Bounded aggregate fix1 — canonical LF and existing-identity boundary evidence

Task: `pa37-operations-catalog-product-read-aggregate-fix1-gpt-20261117-01`, 2026-11-17 (Asia/Shanghai). Scope is only `PA37-AGG-REV-01` and the test portion of `PA37-AGG-REV-03`. The latest Control aggregate intake, aggregate A/B artifacts (`code-review-20261117-231919.md` and `code-review-20261117-232741.md`), accepted plan/review lineage, implementation report and current/sibling tests were inspected. **The portability correction is validated in both materializations; real-container additions compile but remain unexecuted because this runner cannot access Docker. No fresh PostgreSQL16.15 36/36 result is claimed.**

### Bounded changes and unchanged bytes

Exactly the following four test paths and this report change; section 3 records the four current raw SHA256 values. No new repository path or test identity is introduced.

- `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadConfigurationTests.java`: only the hash assertion inside `reusesOrderRoleContext` changes. Read UTF-8, normalize CRLF to LF, hash UTF-8 bytes and pin `734e82e2bfc1f33a1d0c2e3f1cce4cf634bf15796503f82338253e99aff4289d`. The qualifier and `Role.ORDER` assertions remain intact. `OperationsReadContext.java` remains byte-identical: raw mixed-worktree SHA256 `75c2e3b4b3cc251e8fe43223512be89e510c711059bf280ffbd466a9848adcdf`, with 92 CRLF pairs; canonical-LF SHA256 is the new pin.
- `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadWebTests.java`: widen the existing `problem` helper to mirror the shipped sibling: `application/problem+json`, exactly `type|title|status|code|traceId`, matching HTTP/body status and code, UUID trace ID, `Cache-Control: no-store`, existing leak checks plus absence of bearer text in the body, and exact `WWW-Authenticate: Bearer` for 401. Existing identities exercise the helper; none is added or renamed.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadsIT.java`: widen `readsAdminAndOps` to assert ADMIN/OPS 200 on list and detail; absent (`null`), wrong-audience (`wrong`) and missing-employee (`missing`) tokens yield 401 plus the Bearer challenge, and disabled yields 403. Both list and detail shapes are exercised with malformed query/cursor or identifier and invalid Accept so authentication/authority wins before malformed-request validation. The shared, unchanged container problem helper checks the exact safe envelope.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadRoleLeastPrivilegeIT.java`: widen `restoresRoleAfterFailure` with POST and semicolon noncanonical GET variants for list and detail, without credentials, all expecting concealed 404. The original role-restoration, cursor-error and cleanup assertions remain.

Independent before/after byte snapshots cover 2,251 repository/private files, every test identity and the index bytes. All 300 production files remain byte-identical. The unchanged production guard still covers exactly 299 files, retaining the existing V018 exception and LF-normalized digest `6d7fa118cbf2580fd492b2446f57f86e5ea808cfe4626ebf015d5c10f334b39e`. Control, accepted plan/reviews, both helpers, CI/POM, OpenAPI and migrations remain untouched. `PA37-AGG-REV-02` bookkeeping and `PA37-AGG-REV-04` full-sweep custody remain Controller-owned.

### Actual commands and results

Linux toolchain: `/opt/jdk21` Temurin21.0.13+9 and `/opt/maven-3.9.9/bin/mvn`, with the existing Mockito startup agent from section 6. Replayable commands below use variables for the exact literal selectors expanded in the executed invocations; no test skips, dependency changes, offline flag or helper/fixture changes are used.

```sh
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
export JAVA_TOOL_OPTIONS=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar
focused=com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests,com.onecall.archtests.OperationsCatalogProductReadWebTests,com.onecall.archtests.OperationsCatalogProductReadConfigurationTests,com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests,com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests
containers=com.onecall.archtests.container.OperationsCatalogProductReadsIT,com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT
(cd backend && mvn -B -ntp -DfailIfNoTests=true "-Dtest=$focused" test)
python3 /tmp/pa37-agg-fix1-project.py
(cd /tmp/pa37-agg-fix1-lf-repo/backend && mvn -B -ntp -DfailIfNoTests=true -Dtest=com.onecall.archtests.OperationsCatalogProductReadConfigurationTests test)
(cd backend && mvn -B -ntp -Pcontainer-smoke -DfailIfNoTests=true -Dfailsafe.failIfNoSpecifiedTests=true "-Dtest=$focused" "-Dit.test=$containers" verify)
```

| Run | Actual result | Log / custody |
|---|---|---|
| Mixed-worktree focused six PA37 classes plus guard | Exit 0 / BUILD SUCCESS; **68/68**, zero failures/errors/skips; 46.633 s; finished `2026-11-17T23:37:26+08:00` | `/tmp/pa37-agg-fix1-focused.log`; exact seven-class XML set preserved under `/tmp/pa37-agg-fix1-focused-xml/` |
| Whole-tree LF configuration class | Exit 0 / BUILD SUCCESS; **8/8**, zero failures/errors/skips; 17.396 s; finished `2026-11-17T23:38:04+08:00` | `/tmp/pa37-agg-fix1-lf-configuration.log`; XML raw SHA256 `88173a0a1a90a7783c40e569d798aa1b18678b3d85f2e8b5e271de601d34b071` |
| Current-worktree targeted container verify | Exit 1 / BUILD FAILURE; prerequisite units **68/68** and executable-JAR packaging pass; both selected ITs fail in `BeforeAll`; Failsafe reports **2 lifecycle errors, 0 executed test methods**, not 36 passing methods; 32.695 s; finished `2026-11-17T23:38:54+08:00` | `/tmp/pa37-agg-fix1-failsafe.log`; no PostgreSQL engine starts |

The LF proof is a Git-less **whole-repository/candidate** projection, not a one-file substitution: all 2,251 snapshotted paths copied to `/tmp/pa37-agg-fix1-lf-repo`, 1,948 UTF-8 nonbinary files checked, 1,892 source files containing 426,659 CRLF pairs normalized, zero projected text CRLF pairs. Binary bytes are retained. No existing `target` classes/reports are copied; Maven freshly compiles all production/test sources there. `OperationsReadContext.java` has zero CRLF pairs and its raw projected SHA256 equals `734e82e2...4289d`, so the new assertion actually exercises canonical LF. Projection provenance is `/tmp/pa37-agg-fix1-projection.log`.

Docker diagnostics: `docker version` exits 1 with `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`; `/var/run/docker.sock` points to `/var/run/dify.sock`. Testcontainers reports `Could not find a valid Docker environment`, then the cached initialization failure in the second IT. The unchanged `postgres:16` fixture therefore cannot start or freshly verify the Controller's PostgreSQL16.15 image. No permission, daemon, toolchain, fixture, POM or environment-security workaround is attempted.

Current **failed lifecycle** XML hashes, not runtime-pass evidence:

| Failsafe XML filename | Current raw SHA256 |
|---|---|
| `TEST-com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT.xml` | `e625f427e6ab8b4964ad4b52ec0143e796a4a472b2ced2267615b83bf7edca88` |
| `TEST-com.onecall.archtests.container.OperationsCatalogProductReadsIT.xml` | `f2c8d2e208d80ecfbe41e2a319359fcab92207a03b6c1962b7244ec8581778bf` |

Before the attempt, Controller's historical passing 16/20-method XMLs were preserved at `/tmp/pa37-agg-fix1-prior-failsafe/`, with their original hashes `bd5dfe3203086ffcd0c31fded3593db71ef72ceac5d51a23da637edc45f79d4c` and `a9ef9f64d3b84fcc7776f14f0c1e9bcf7a269e64845e3254545644535742c917`. They predate these test additions and are **not** substituted for fresh runtime evidence. `/tmp/pa37-agg-fix1-xml.py` checks focused/LF XML identities against the intake snapshot and distinguishes these historical reports from the new lifecycle errors; output `/tmp/pa37-agg-fix1-xml.log`.

### Inventories, whitespace and stop

```sh
python3 /tmp/pa37-agg-fix1-scope.py after
(cd backend && python3 /tmp/pa37-ci-source-inventory.py)
git --no-optional-locks --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call24 --work-tree="$PWD" diff --check
```

Scope/identity checks pass: exactly the five authorized modified paths, no new/deleted/renamed repository path, all test names unchanged, PA37 **six unit classes/60 methods, two container classes/36 methods, two zero-test helpers**. Global **89/848 + 36/592 = 125/1440**, annotation scan **1441**, accepted candidate path set **24**, all production bytes/digest and index bytes unchanged. Existing CI source/history/token/POM checker exits 0: `PASS source/history/token/POM checks [848, 592] 1441 89 36 36` (`/tmp/pa37-agg-fix1-ci-source-inventory.log`). The supplemental complete-report inventory checker exits 1 for absent `target/failsafe-reports/TEST-*PostgresContainerIT.xml`; it requires the full container sweep, so no full XML-inventory pass is claimed (`/tmp/pa37-agg-fix1-ci-inventory.log`). Tracked `git diff --check` exits 0 with no whitespace-error diagnostics (`/tmp/pa37-agg-fix1-diff-check.log`); existing autocrlf warnings are informational. All 23 non-report manifest hashes in section 3 are independently checked against current bytes; the report has no circular self-hash.

The required fresh PA37 PostgreSQL16.15 **36/36 rerun remains blocked**, including execution of the new 401/403 and concealment assertions. Controller's separate required **full 36-class container sweep is not run or claimed**, nor is hosted CI, production readiness, acceptance or aggregate re-review. Existing residual owners remain unchanged. No staging, commit, push, PR, merge, approval, mark-ready, reviewer request, external comment, issue mutation or later-gate transition occurs. Stop after this bounded fix/report, with runtime validation explicitly incomplete.

## 11. Bounded aggregate fix2 — immutable version membership

Task: `pa37-operations-catalog-product-read-aggregate-fix2-gpt-20261118-01`; date: 2026-11-18 (Asia/Shanghai); gate: `fix`.

### Accepted input and exact delta

Read the latest Control aggregate re-review intake and both artifacts, `docs/reviews/code-review-20261118-002202.md` (A: PASS, zero findings) and `docs/reviews/code-review-20261118-003426.md` (B: Medium custody finding and Low membership finding). Implement only Controller-accepted Low `PA37-AGG-RR2-01`:

- `DefaultOperationsCatalogProductReadService.detail` hoists `var knownVersionIds = Set.copyOf(versionIds);` immediately before the benefit loop and uses that immutable Set for benefit-version membership. The original ordered List and `reader.benefits(versionIds)` call remain unchanged. Validation, ordering, exception mapping, SQL, DTOs and test identities are unchanged; membership work no longer repeatedly scans the version List.
- The existing tree guard changes only its production digest literal. `hasSize(299)`, the V018 exception, LF normalization, sorted path rows and all other assertions remain unchanged.
- This implementation report updates the two current raw source hashes in section 3, the current digest in section 4 and this bounded evidence. Older fix hashes/results remain historical.

Current service raw and canonical-LF SHA256: `f7702095e835cbf0ba77754066f2804a7d45d9fbe804c4942eeb68118a0c1895`. Current guard raw SHA256: `5bce4981478df1a07cda971dcd3ee6cd5807b3ffcd16a18b0d5d10e90cc265d8`.

### Derived production manifest and candidate custody

`/tmp/pa37-agg-fix2-scope.py` independently walks both `backend/src/main/java` and `backend/src/main/resources`, retaining the guard's existing filename exception for V018: **300 raw production files, exactly 299 guarded files**, unchanged path sets. For each included file it decodes UTF-8, replaces CRLF with LF and hashes re-encoded UTF-8 bytes. Rows are `src/main/.../path<single-space><lowercase-SHA256>` (backend-relative, forward slashes), ordinal-sorted, joined with LF and **one final LF**, then SHA-256 hashed. The derived digest, now pinned by the unchanged guard algorithm, is **`12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1`**. It is recomputed from actual production bytes, not assumed or copied from a review suggestion.

Candidate custody uses the same canonical row algorithm with repository-relative paths for the established **24-path** section 3 set:

- Intake independently reproduces **433,334 canonical bytes / 436,643 raw bytes**, SHA256 `519d0f3af899ea31b472a9fce709bae92bceeea792dfb77c8192b0937882f21a`.
- After the service and guard changes, before updating this report, the 24-path checkpoint is **433,397 canonical bytes / 436,706 raw bytes**, SHA256 `5446a3336433b62d038a0fbf97510316db2c179f04db71cef7ca694bd00aaaef`. This is explicitly a pre-report checkpoint, not the final candidate identity.
- The final **23 non-report paths** contain **374,559 canonical bytes**, SHA256 **`95270e83627f4a26d39058089b83f2d53465a694734f2429cc4fc706b47ba43d`**. This stable payload identity is recorded here without a circular report self-hash.
- After finalizing this report, the scope checker writes the complete **24-path final canonical candidate identity**, final canonical/raw byte totals and this report's raw hash to `/tmp/pa37-agg-fix2-candidate-identity.json`; exact canonical path/hash rows are `/tmp/pa37-agg-fix2-candidate-manifest.txt`, and `/tmp/pa37-agg-fix2-scope.log` records them with the scope verdict. The final 24-path digest is also delivered in the runner response; it is not embedded into the report it hashes. Controller can re-attest those same final bytes without changing Control here.

### Actual focused validation

Established Linux toolchain: `/opt/jdk21` Temurin21.0.13+9, Maven3.9.9 and the existing Mockito startup agent; no skips, POM/CI change or dependency override:

```sh
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
export JAVA_TOOL_OPTIONS=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar
focused=com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests,com.onecall.archtests.OperationsCatalogProductReadWebTests,com.onecall.archtests.OperationsCatalogProductReadConfigurationTests,com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests,com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests
(cd backend && mvn -B -ntp -DfailIfNoTests=true "-Dtest=$focused" test)
python3 /tmp/pa37-agg-fix2-xml.py
python3 /tmp/pa37-agg-fix2-scope.py after
git --no-optional-locks --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call24 --work-tree="$PWD" diff --check
```

Maven exits **0 / BUILD SUCCESS**, **7 classes / 68 methods**, zero failures/errors/skips; total time **01:05 min**, finished **2026-11-18T01:12:07+08:00**. Per-class totals are request8, codec8, service18, web10, configuration8, PA37 boundary8 and tree guard8. Log: `/tmp/pa37-agg-fix2-focused.log`. XML verification compares every executed method name against the intake snapshot, including the service's existing negative-benefit assertions. Exact seven-class XML custody: `/tmp/pa37-agg-fix2-focused-xml/`; ordinal filename/raw-hash manifest SHA256 **`7a44de214712f9df2a6a2ff4efe7d2ce11bb7edeebaabeb63b6b08d82cc5a952`**; checker output `/tmp/pa37-agg-fix2-xml.log`.

Scope verification compares a before-edit snapshot of **2,256 repository/private paths and all 1,440 method identities** against final bytes: exactly the three authorized paths changed, no added/deleted/renamed path and no index-byte change. Reverse-delta hashing additionally proves the service changed only by the Set hoist/receiver substitution and the guard only by the digest literal. All other production/test/Control/plan/review/OpenAPI/migration/CI/POM/helper bytes remain unchanged from aggregate-fix intake. All 23 non-report raw hashes in section 3 match current bytes. Preserved identities: PA37 **six unit classes/60 methods, two container classes/36 methods, two zero-test helpers**; global **89/848 + 36/592**, annotation scan **1441**. Final scope output: `/tmp/pa37-agg-fix2-scope.log`. `git diff --check` exits **0**, no whitespace errors; `/tmp/pa37-agg-fix2-diff-check.log` (existing autocrlf warnings are informational).

### Superseding Controller evidence and stop

Section 10's blocked Docker result belongs to the earlier fix runner, not Controller's later execution. The latest Control intake records that the genuine zero-CRLF whole-tree projection `/tmp/pa37-agg-fix1-lf-repo` passed `clean -Pcontainer-smoke verify`: Surefire **89/848**, Failsafe **36/592**, zero failures/errors/skips, BUILD SUCCESS in **28:51**, finished **2026-11-18T01:07:04+08:00**. This supersedes the previously mislabeled CRLF sweep and resolves the reviewers' materialization-custody question; it is **pre-fix Controller evidence**, not a post-Set-hoist run by this agent.

Only the fresh focused **68/68**, XML identity checks, production-manifest derivation, scope check and whitespace check are claimed for this bounded fix. No full container sweep, PA37 PostgreSQL36 rerun or ordinary848-method rerun is performed here; Controller owns the latter two post-fix validations. No staging, commit, push, PR, merge, approval, mark-ready, reviewer request, external comment, issue mutation or re-review entry occurs. Stop after this bounded fix/report; existing residual ownership is unchanged.
