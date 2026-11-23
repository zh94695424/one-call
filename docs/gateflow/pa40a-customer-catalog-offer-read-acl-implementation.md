# PA40A S1 — Implementation Authoring Evidence

## Final Controller Validation Status — 2026-11-23

- Task: `pa40a-customer-catalog-offer-read-acl-final-validation-gpt-20261123-01`; work unit: `PA40A-customer-catalog-offer-read-acl`; gate: `implementation` runtime validation evidence.
- Accepted plan commit / implementation HEAD: `0064c86d3df704aeb02b9084143049de089606a6`; base `origin/main`: `ce9f26bd7caa91d69d1cbd7fcb3e058e763341b1`.
- **Controller-completed runtime evidence is recorded, not independently rerun by this documentation task.** The bounded record is `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-final-validation.md`. Pending labels, failures, command recipes, and authoring-only boundaries below describe earlier stages and are superseded only to the extent explicitly recorded here and in that artifact.
- Final XML inventory: **105 Surefire classes / 1026 tests + 41 Failsafe classes / 682 tests = 146 classes / 1708 tests**, with **0 failures, 0 errors, 0 skipped**. The final combined `mvn -B -ntp -Pcontainer-smoke failsafe:verify` exited **0** against all 41 Failsafe XML reports.
- **Split-run limitation:** one uninterrupted `clean verify -Pcontainer-smoke` reached 16/41 green Failsafe classes before the Docker Linux engine disappeared and killed the Maven child. After restarting Docker Desktop, the Controller completed missing/affected classes in bounded batches using the same compiled candidate. This is complete green XML aggregate evidence, **not one uninterrupted all-green clean verify**; the interruption is infrastructure evidence, not a test-failure claim.
- Implementation scope remains exactly **52 paths (49 modified + 3 new)**, excluding private `.pa40a-*`, `.codex`, reports, and generated output. V019 raw/LF SHA256 remains `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e`; migration count is **19**, and V001–V018 have no diff against HEAD.
- All three unchanged CI Python bodies passed in their recorded environments: exact source=CI=POM=XML inventory and historical subtraction checks; default/both web JAR smoke profiles and exact probes/cleanup without a DB/verifier adapter; dependency/firewall inventory and 116 adversarial probes. JAR inspection and runtime dependency inventory succeeded; the JAR contains exactly one V019 migration entry and no V020.
- `npm run check:s3`, Security A3 S3 (23/23, 10 negative), and Capacity A4 S2 (113 fixture cases, 471 non-finite probes) passed. A2 IVR S3 and local A5 acceptance S2 reproduced pre-existing, unrelated source-tree/document validation failures on inputs untouched by PA40A; details and the previously recorded Control/PA22 context are in the bounded record. These are explicit limitations, not passing gates.
- This update changes only this cumulative report and the new bounded validation artifact. It does not alter implementation, tests, migrations, PA25, CI, POM, OpenAPI, plan/review artifacts, or Control, and does not enter review, approval, readiness, or production acceptance.

## Implementation Fix 1 Status — 2026-11-23

Controller Windows/PostgreSQL16 evidence subsequently reports **16 tests, 2 failures, 2 errors, 0 skipped, 1009.105 seconds**, not acceptance. Fix task `pa40a-customer-catalog-offer-read-acl-s1-fix1-gpt-20261123-01` corrects V019's optional history19 basename and the catalog-role test's mistaken all-schema99 versus catalog-schema42 comparison. Current hashes/CRC/C27/full320 below include that fix; historical evidence is unchanged. The independent catalog matrix now compares all42 tuples before and after V019 against an explicit expected set: seven tables × SELECT/INSERT, 22 column UPDATEs, four guard EXECUTEs, one schema USAGE. Cross-schema checks and Operations/IVR/pool proofs remain intact. Fix checks and Controller-pending runtime commands are recorded in `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-fix-1.md`. The original authoring checks below remain historical results, not a rerun or a claim that the Controller failures passed.

- Task: `pa40a-customer-catalog-offer-read-acl-s1-implementation-authoring-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Workdir: `/mnt/c/Users/Administrator/.codex/worktrees/pa40a-customer-catalog-offer-read-acl/codex-call`.
- Accepted HEAD: `0064c86d3df704aeb02b9084143049de089606a6`, verified with the existing read-only `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call10` / `GIT_WORK_TREE` override. The inherited Windows `.git` pointer is unchanged.
- Binding sources: Control PA40A goal/adjudications; the complete final cumulative plan, fixes 1/2, and all six `plan-review-20261123-*` artifacts.
- Status: **AUTHORING COMPLETE; PostgreSQL/Windows runtime validation PENDING**. This is not runtime acceptance, review approval, readiness, or production acceptance.
- The earlier tooling-preflight-only attempts did not author implementation. This dispatch explicitly separates authoring from Controller-owned Windows validation and supersedes their blocked-authoring status.

## 1. Exact Write Accounting

Exactly **52 implementation paths = 49 modified existing + 3 new**, plus this explicitly authorized report. A before/after raw-byte snapshot checks all captured backend/docs/CI/API files; implementation differences equal the plan §6 + Appendix C allowlist, with no missing or extra implementation path. All 44 existing test/fixture paths are edited. The report existed as an untracked earlier-attempt artifact and is replaced by this authoring report.

No production Java, V001–V018, Control, plan/fix/review artifact, OpenAPI/docs12/14/15/17, repository helper/private file, dependency/toolchain/firewall allowlist, or unrelated source is edited. Outside-write-set raw hashes remain unchanged. No Git metadata edit, stage, commit, push, branch/PR/review/approval/comment/issue action, Windows executable launch, Docker launch, package installation, or external network action is performed. Native Maven writes only ordinary local build outputs; an incidental root-relative generated Javadoc file from a preliminary `-f backend/pom.xml` check was removed, leaving no root build artifact in the write set.

### Modified Existing Implementation Paths (49)

- `.github/workflows/ci.yml`
- `backend/README.md`
- `backend/ops/postgresql/pa25-runtime-identities.sql`
- `backend/pom.xml`
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsSubjectReadBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/container/ActivationCommandRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java`
- `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java`
- `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java`
- `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/EvidenceRoleIvrAssociationAclIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftLifecycleIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsLifecycleRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadsIT.java`
- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
- `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java`
- `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java`
- `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java`
- `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/SubjectClaimRoleLeastPrivilegeIT.java`
- `docs/operations/pa25-production-database-identity-provisioning-runbook.md`

### New Implementation Paths (3)

- `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql`
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOfferReadPrivilegesIT.java`

### Separately Authorized Report

- `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation.md`

## 2. V019 Authored State Machine And Exact Delta

One anonymous DO block, existing advisory lock `(725025,1)`, PostgreSQL16 authority checks, transaction-local pg_catalog search path, protected unmanaged superuser runner, and explicit local switch to catalog_order_owner. Its only mutation payload is four explicit-column SELECT grants; the local role is restored. No schema-USAGE duplicate, table-wide SELECT, grant option, REVOKE, business DML, routine/schema/role/member/owner mutation is authored.

The migration independently pins customer old53/final75 and the unchanged exact predecessor18 history. It checks marker provenance for 0/3/4 slots, including an already provisioned four-slot predecessor18; exact topology/options/grantors/attributes/settings/owners/PUBLIC/default/dependency/effective privilege invariants; and two-pass preservation. Complete75 accepts history18 or valid optional19 without a grant. Old53 accepts only history18 and receives the exact22 delta. Every partial state, extra/wrong-grantor/grantable state and history19+old53 rejects rather than repairs. All 13 sanitized `V019_*_REJECTED` categories remain, with P0001 only.

Preservation additionally covers user relation/column/type catalogs and every preexisting ACL entry except the exact22 permitted additions, plus scoped shared dependencies. V019 remains customer-target-only, not an unrelated-role432 repair migration. PA25 and the independent IT own full nine-role exactness.

- Actual raw SHA256: `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e`.
- Canonical-LF SHA256: `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` (new migration is LF).
- Signed Flyway line-based CRC32: **-1794072022**.
- PA25 row19 pins version019, exact basename/description and that concrete checksum.

### Exact 22 Tuples

Tuple format: `grantee|kind|object|column|privilege|grantor|is_grantable`.

```text
customer_iam_role|column|catalog_order.catalog_benefit_line|catalog_version_id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_benefit_line|entitlement_type|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_benefit_line|grant_interval_months|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_benefit_line|quantity|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_benefit_line|validity_days|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_offer|catalog_version_id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_offer|channel|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_offer|id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_offer|offer_status|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_offer|version_token|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_product|id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_product|product_code|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|audience|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|billing_cycle|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|currency|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|installments|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|offer_type|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|price_cents|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|product_id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|sale_status|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.catalog_version|version_no|SELECT|catalog_order_owner|false
```

The sole Java literal delta provider is package-private `CustomerCatalogOfferReadPrivilegesIT.catalogDeltaOracle()`; package-private `current432Oracle()` composes historical410 + delta22 with disjointness and subtraction proofs. Historical V018 `currentOracle()/additionalOracle()/associationDelta()` remain410/122/2. Cross-package units reflectively access the package-private provider. The new IT has no Flyway-typed method signature, so reflective method lookup works with the existing Surefire Flyway classpath exclusion; no POM dependency/exclusion expansion is needed.

The three existing exhaustive customer capability methods retain complete table/column enumeration, savepoint42501 probes and historical targeted phases. Their maps receive exactly four tables/22 columns, compare that projection to the single provider, and current phases cover all69 customer columns. Historical seven-role290 is independently preserved and current312 is projected from the provider where applicable. No V018 replay is moved after019; its isolated historical provisioning call uses the hash-verified18/410 script.

## 3. Frozen And Current Byte Projections

Aggregate fix 1 supersedes only the current pin representation; earlier runtime totals and split-run limitations remain historical evidence, not a new full-suite rerun.

- F26: preserved26 literal pairs, aggregate `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761`.
- H26: F26 with **only** the historical README substitution; aggregate `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`.
- H26 README raw/LF: `fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64` / `bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177`.
- C27: canonical-LF identities for every F26 path, replacing the current PA25/runbook/README content and adding V019; **27 paths, 19 migrations, the same eight non-migration paths**. Each current pair repeats its canonical LF digest in both slots; historical F26/H26 raw+LF pairs remain unchanged.
- C27 aggregate: `58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98`.

All three aggregates sort paths by unsigned UTF-8 bytes and append UTF8(path), one NUL and two decoded 32-byte digests, then SHA256 the concatenation. F26/H26 retain historical raw then LF digests. Current C27 uses canonical LF then canonical LF; neither slot describes host-materialized raw bytes. No hex-text serialization, inter-hash delimiter or row terminator.

Both existing IVR verifier methods retain their original26 literals, historical tableCount/bareCount26, PA35 parser and F26/H26 aggregate constants. H26 expected values come only from those preserved literals and its one README substitution, never live bytes or reconstructed PA25. Historical byte fixtures recover the old PA25 via the independently hash-pinned prefix and remove only the new addendum for prior README/runbook byte probes; they do not supply H26 expected pins. Only test-local PA35 text copies are LF-normalized so existing newline-removal mutation probes actually remove rows in this CRLF checkout; the primary PA35 artifact and parser/counters are unchanged.

C27 has separately named current table/bare representations, exact27 unique-key/canonical-pair/bare equality, a separately pinned aggregate, the same canonical content check for the live root and clean-checkout fixtures, LF/CRLF temporary positive projections, and independent missing/duplicate/fake-entry/hash/byte/BOM/wrong-name/future migration rejection probes. The original historical negative probes still execute and reject. Both PA38/39 guards independently prove historical18/no019 and live exact19 full basenames, old reconstructed and new shipped PA25 pins, and hostile missing/020/extra-wrong019/same-count-wrong019 sets.

### Current Canonical Changed-File Pairs

Current pins describe committed canonical-LF content, not the pre-commit mixed/CRLF worktree representation. Any materialized bytes are accepted only if replacing CRLF with LF produces the pinned canonical digest. The same rule applies to every current path and fixture, without a live-root exception. Historical raw+LF pairs and reconstruction assertions are preserved exactly. See `docs/gateflow/pa40a-customer-catalog-offer-read-acl-aggregate-fix-1.md` for independent committed-blob and clean-projection validation; earlier fix artifacts remain historical evidence, not current pin semantics.

| Path | Canonical LF SHA256 (slot 1) | Canonical LF SHA256 (slot 2) |
| --- | --- | --- |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` | `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` |
| `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | `a74827f87821353365cf48b0f5ebb794a902ca0ee4710229f057078a0f2d7430` | `a74827f87821353365cf48b0f5ebb794a902ca0ee4710229f057078a0f2d7430` |
| `backend/README.md` | `9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b` | `9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b` |
| `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql` | `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` | `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` |

The historical PA25 reconstruction separately verifies canonical LF `8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451` and CRLF raw `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`. It removes only the exact appended22 rows and exact row019 after tagged-array assertions, restoring the four exact guard literals; there is no broad version-sensitive operational fallback.

### Complete C27 Pinned Map

| Path | Canonical LF SHA256 (slot 1) | Canonical LF SHA256 (slot 2) |
| --- | --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| `backend/README.md` | `9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b` | `9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b` |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` | `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `backend/src/main/resources/db/migration/V009__sharing.sql` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |
| `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql` | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |
| `backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql` | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` |
| `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` | `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` | `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` |
| `backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql` | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` |
| `backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql` | `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817` | `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817` |
| `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql` | `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` | `32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e` |
| `docs/12-technology-stack-and-system-architecture.md` | `772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435` | `772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` | `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` |
| `docs/15-production-openapi-surface-spec.md` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` | `0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02` |
| `docs/17-production-security-design-spec.md` | `4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6` | `4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6` |
| `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | `a74827f87821353365cf48b0f5ebb794a902ca0ee4710229f057078a0f2d7430` | `a74827f87821353365cf48b0f5ebb794a902ca0ee4710229f057078a0f2d7430` |

## 4. Final Source Inventories — Not Runtime Acceptance

| Inventory | Final authored/static value |
| --- | ---: |
| Production Java | 298 |
| Production resources / migrations | 22 / 19 |
| Production Java + resource manifest | 320 |
| Test Java / non-ordinary-test fixture files | 192 / 46 |
| Surefire classes / ordinary methods | 105 / 1026 |
| Failsafe classes / ordinary methods | 41 / 682 |
| All ordinary classes / methods | 146 / 1708 |
| Annotation-containing source lines | 1709 (one historical comment-only line) |
| Failsafe POM includes | 41 |
| Existing PA25 provisioning IT methods | 20 |
| Old immutable migration resources | 18 |
| Current customer schema / column / total tuples | 6 / 69 / 75 |
| Seven-role / Operations / IVR / full tuple totals | 312 / 14 / 106 / 432 |
| Noncustomer tuple subtotal | 357 unchanged |
| Catalog runtime role own tuples | 42 unchanged expectation |
| Current logins / monitored roles / membership edges | 7 / 9 / 11 unchanged |
| External slots / managed names / capability boundary / full boundary | 4 / 13 / 13 / 17 unchanged |
| Project role boundary before / after provisioning | 31 / 35 unchanged |

- Current full320 LF manifest: `c290aac2bc7662d978e5a516e23d706a1b5195f8601055c3e8a917c21a4065e7`.
- Excluding exactly V019 reproduces the unchanged319 manifest `57990aa0bc0886c45a58f4e63698d05f860f94a667bc047b2072b6251f3a66fd`.
- Historical318 manifest, excluding exactly V018 and V019, remains `26a10794d635d28bb402a4c88ee27b4c486ac09175b637f90faa122b38114f4a`.
- All1684 existing ordinary identities remain unchanged, digest `6041250383865a75ff49037c1f736bb87db552561206b9990aedaf8604a51770`.
- Final1708 identities: `fe582055c9a28067aa156231f5aabfece21f91510a909a56a4c189c2ba4ca55c`, sorted LF FQCN#method records with terminal LF and duplicate rejection.
- Both exact Appendix D new method sets are authored: eight unit and sixteen IT methods, all ordinary tests. CI maps match those exact identities; all historical start totals/subtractions and annotation formulas remain intact.
- PA25 source has432 distinct normalized tuples and exact19 history rows. Per-role: customer75, evidence18, evidence-media-reader16, sharing23, sharing-command57, sharing-public-read26, sharing-admission97, Operations14, IVR106. These are authored oracle counts, not database observations.

## 5. Authored PostgreSQL Proofs — Execution Pending

All16 new IT identities exist with executable bodies. They cover predecessor LC denial of22 columns; fresh/upgrade/current432 exactness; every approved22/adjacent12 column; explicit stored-value joins and empty-benefit/QUOTE cases; non-read-only writes/DDL/REFERENCES/trigger/index/sequence/routine/unrelated-object denials; all-role SET/CREATE/role-GRANT escalation; no object-GRANT amplification; 0/3/4-slot clean/raw-complete/history19 replay; all21 partial prefixes, extra/grantable/wrong-grantor states; all13 sanitized categories including transaction-rollback fault injection; all432 PA25 missing-tuple cases and history drift; credentials/graph preservation; actual customer and shared Operations physical pool cleanup; and every44 user business table plus infrastructure/definition/ACL/dependency fingerprints.

Each rejection starts with seeded customer, Operations and catalog facts and nonempty audit/outbox/idempotency canaries. Fresh fixture seed is idempotent only per disposable fixture instance, before fingerprints; there is no truncation/reset between compared snapshots. The sequence/REFERENCES objects are test-only and removed before the final invariant comparison. No new production sequence is authored. Object SELECT-GRANT by a role lacking grant options is asserted as PostgreSQL's warning/no-op with no amplification; role-membership GRANT/escalation probes require42501, rather than treating a warning as an error. This preserves the existing customer-test non-amplification convention.

None of these PostgreSQL results, runtime tuple counts, session cleanup results, Flyway executions or PA25 success markers has been observed in this WSL pass. No container test is launched.

## 6. Checks Actually Performed

1. Read binding sources and existing SQL/fixtures/guards/CI/POM; verified accepted HEAD using read-only Git override. Default Git pointer failure was not used to stop authoring.
2. Raw-byte scope snapshots prove exactly the52 implementation paths and unchanged outside bytes; new file identities, all old1684 identities, full1708 digest, all inventory/count formulas,44 existing affected paths, four final byte pairs, V019 CRC and PA25 tagged432/history19 were recomputed locally. Both old literal26 maps/parser counters/aggregate constants are preserved.
3. WSL-native `/opt/jdk21` (21.0.13), `/opt/maven-3.9.9`, offline cached dependencies: final `mvn -o -B -ntp -Pcontainer-smoke -DskipTests test-compile` **exit0**, 298 production/192 test source compilation available; no Docker/IT execution. Earlier compilation errors in newly authored code (Transferable package / qualified TreeSet) were corrected before the final successful compile.
4. Final focused unit selector below: **exit0, 88 tests, 0 failures/errors/skips**, including all eight new units. Earlier native focused runs exposed and corrected Surefire-excluded Flyway reflection, incorrect historical count selection, literal-vs-guard grant-option checking, CRLF row-mutation probes and current numeric guard errors. No historical checker was disabled to obtain this result.
5. All three workflow Python heredocs AST-parse. Executed the actual unchanged CI dictionary/map merges and source/POM/historical-subtraction section in a read-only static extraction: **105/41 classes,1026/682 methods,1709 annotation lines; all historical start formulas pass**. XML/runtime sections were not represented as passing by that static extraction. Final scoped `git diff --check -- .github backend docs/operations` passes after removing five whitespace-only issues; the binding plans/reviews were not modified or reformatted.
6. Additional native default `mvn -o -B -ntp test` **exit1:1026 tests,6 failures,547 errors,0 skips**. The full-suite run encounters unavailable Mockito/Byte Buddy self-attachment and `java.net.SocketException: Operation not permitted` while starting existing web/network fixtures in this sandbox. The inspected failure roots are those WSL restrictions, not a claimed green full suite. No unrelated tests, JVM/POM configuration or socket permissions were changed. Controller must run the required full host gate. A subsequent final focused run and final compile pass as recorded above.

Native logs are temporary session evidence under `/tmp/pa40a-finalcompile.log`, `/tmp/pa40a-finalunits.log`, `/tmp/pa40a-all-units.log`; they are not repository deliverables. No PostgreSQL, PA25, Failsafe, clean full verify, JAR runtime/firewall or contract result is claimed.

## 7. Controller-Pending Windows Validation — Exact Commands

Use Windows-native Java21/Maven3.9.9/Docker with PostgreSQL16 and the already provisioned dependencies. The commands below are instructions for the Controller, not commands launched as Windows executables by this authoring agent. Verify the toolchain and Docker server first; do not install or mutate socket/host configuration as an implicit part of this report.

From the Windows worktree, enter backend:

```powershell
Set-Location 'C:\Users\Administrator\.codex\worktrees\pa40a-customer-catalog-offer-read-acl\codex-call\backend'
java -version
mvn -v
docker version
mvn -B -ntp -Dtest=CustomerCatalogOfferReadAclBoundaryTests test
mvn -B -ntp -Pcontainer-smoke -DskipTests test-compile
mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerCatalogOfferReadPrivilegesIT failsafe:integration-test failsafe:verify
mvn -B -ntp -Dtest=CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectClaimBoundaryTests,CustomerSubjectActivationBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsActivationReadBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogDraftBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test
mvn -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT,DatabaseRuntimeAclRbacHardeningIT,ProductionDatabaseIdentityProvisioningIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,CustomerEvidenceReadPrivilegeIT,EvidenceRoleIvrAssociationAclIT failsafe:integration-test failsafe:verify
mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerMeIT,CustomerSubjectsIT,CustomerBusinessNumbersIT,CustomerEvidenceIT,CustomerSubjectClaimsIT,CustomerSubjectActivationsIT,SubjectClaimRoleLeastPrivilegeIT,ActivationCommandRoleLeastPrivilegeIT,OperationsSubjectReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT,OperationsActivationReadsIT,OperationsActivationReadRoleLeastPrivilegeIT,OperationsIvrSessionReadsIT,OperationsCatalogProductReadsIT,OperationsCatalogProductReadRoleLeastPrivilegeIT,OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT,OperationsCatalogDraftLifecycleIT,OperationsCatalogDraftRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
mvn -B -ntp clean verify -Pcontainer-smoke
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
mvn -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt
```

After the clean full gate, require1026 Surefire+682 Failsafe=1708 exact identities, zero failures/errors/skips; source=CI=POM=XML exactness (not counts alone). Run the workflow's Python bodies unchanged, including JAR/default/web smokes and firewall/adversarial probes. The following PowerShell in-memory extraction executes all three unchanged bodies from backend after the two JAR/dependency manifest commands above; it adds no helper file:

```powershell
@'
from pathlib import Path
import re
workflow = Path('../.github/workflows/ci.yml').read_text().replace('\r\n', '\n')
blocks = list(re.finditer(r"(?m)^          python3 - <<'(PY|PYCODE)'\n(.*?)^          \1$", workflow, re.S))
assert len(blocks) == 3
for block in blocks:
    body = '\n'.join(line[10:] if line.startswith(' ' * 10) else line for line in block.group(2).splitlines())
    exec(compile(body, '<unchanged-ci-body>', 'exec'), {'__name__': '__main__'})
'@ | python -
```

Against disposable/explicitly authorized PostgreSQL fixtures only, with protected connection settings and no password in the command, from repository root (PowerShell external-name inputs already assigned):

```powershell
psql -X -w -v ON_ERROR_STOP=1 -v mode=apply -v sharing_login="$SHARING_LOGIN" -v customer_login="$CUSTOMER_LOGIN" -v command_login="$COMMAND_LOGIN" -v operations_login="$OPERATIONS_LOGIN" -f backend/ops/postgresql/pa25-runtime-identities.sql
psql -X -w -v ON_ERROR_STOP=1 -v mode=check -v sharing_login="$SHARING_LOGIN" -v customer_login="$CUSTOMER_LOGIN" -v command_login="$COMMAND_LOGIN" -v operations_login="$OPERATIONS_LOGIN" -f backend/ops/postgresql/pa25-runtime-identities.sql
```

Require exact `PA25_APPLY_OK logins=7 effective_roles=9 memberships=11` / CHECK equivalent, history19, ACL432/customer75, repeated apply/check preservation and all negative no-repair/atomicity proofs. These are pending expected outcomes, not emitted results in this pass.

From repository root run the plan's unchanged contract gates after authorized bundle generation:

```sh
(cd api/openapi && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s2 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures
```

For Windows PowerShell, use `Push-Location api/openapi; npm run check:s3; Pop-Location` in place of the POSIX subshell line, checking the npm exit code before proceeding. Recheck all final bytes/checksums before runtime gates; do not replay V001–V018 on a V019 database. Capture the actual XML/JAR hashes, zero-skip totals, fresh/upgrade/replay/rejection evidence and private-catalog preservation comparisons under Controller custody.

## 8. Remaining Status

- Authoring blocker: **none**.
- Controller runtime status: **completed evidence recorded** in the final status section and `docs/gateflow/pa40a-customer-catalog-offer-read-acl-implementation-final-validation.md`; earlier pending statements are historical, not the current aggregate status. The final 146-class/1708-test XML inventory is green with zero failures/errors/skips, and combined Failsafe verification exited 0.
- Validation limitations remain explicit: no uninterrupted all-green clean verify; Docker interruption required bounded completion on the same compiled candidate. A2/A5 local source-tree/document checks reproduce pre-existing failures unrelated to PA40A. This record does not infer separate PA25 command outputs beyond the supplied Controller evidence.
- WSL full-suite restrictions are recorded, not bypassed, and are not a reason to abandon this authoring slice.
- Column ACLs remain role-wide scalar reads, not row filtering/RLS/endpoint authority; DRAFT/OFFSALE/other-channel visibility and future trusted application predicates remain explicit. No endpoint or production readiness is claimed.
- No staging, commit, push, PR creation/update, merge, approval, readiness marking, reviewer request, external comment, or code-review gate is entered by this documentation pass.
