# PA40A Customer Catalog Offer Read ACL — Implementation And Review Plan

- Task label: `pa40a-customer-catalog-offer-read-acl-plan-gpt-20261123-01`.
- Date: 2026-11-23; timezone Asia/Shanghai.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa40a-customer-catalog-offer-read-acl/codex-call`.
- Read-only worktree HEAD: `2398a4d71eee2973b684f6a404d3c65766bf7230`; product baseline PA39 merge `ce9f26bd7caa91d69d1cbd7fcb3e058e763341b1`. Use the per-worktree Git directory, not the common repository HEAD/index.
- Deliverable at bounded plan-fix2: this cumulative plan and `docs/gateflow/pa40a-customer-catalog-offer-read-acl-plan-fix-2.md` only. Fix task: `pa40a-customer-catalog-offer-read-acl-plan-fix2-gpt-20261123-01`. Fix1 remains immutable historical evidence; all five prior accepted fixes remain binding. Implementation, runtime validation and acceptance are **NOT EXECUTED**. Next gate: **final bounded independent peer-isolated A/B plan re-review**.
- Planned implementation: **one atomic slice**, three new implementation files and 49 modified implementation/support files (**52 paths total**; full allowlist below). The two planning documents are outside that future implementation count.

## 1. Scope, Authority And Evidence

Control's PA40A Goal Confirmation was read first and controls every decision. This is an ACL prerequisite, not the blocked PA40 endpoint work. Preserve V001–V018 bytes, all production Java, OpenAPI, docs12/14/15/17, existing owners, memberships, runtime identities, credentials and pools. Add only V019 plus its independent oracle/proofs and necessary existing guard/inventory/documentation maintenance. No V020; no endpoint, Java application logic, cursor key, catalog mutation or order/payment work.

Read sources: docs12 architecture (modular monolith, owner boundaries, PostgreSQL/Flyway/jOOQ), docs14 §§4.16–4.19, §7.3 and §9.26, docs15 operation map/projection/keyset rules, docs17 authentication/role separation/least privilege/secrets/cursors; frozen OpenAPI `listCustomerCatalogOffers` and `CatalogOfferPage`, `CatalogOffer`, `CatalogBenefitLine`; all 18 migration resources, their role/grant definitions and V014–V018 fail-closed guards; PA25 SQL/runbook, customer PostgreSQL16 fixtures and independent ACL tests; global source/CI/POM/README/freeze-guard searches including PA33–PA39. Appendix A records independent current byte/checksum evidence, not runtime results.

External dependency evidence only: read `codex/pa40-next-work-unit:docs/gateflow/pa40-customer-catalog-offers-read-foundation-plan.md` with read-only `git show`. It stops at BQ-PA40-01: LC cannot select these catalog facts, while substituting Operations LO would change the authority topology. PA40A's confirmed additive customer ACL resolves that dependency without adopting LO or implementing `listCustomerCatalogOffers`. No branch files were copied.

### 1.1 Numeric wording discrepancy: preserve the real graph

The request/Control use “six logins/identities” in places. That is the historical three-external-slot graph, not the current source. Current PA25 unequivocally has **four external logins plus three direct login roles = seven logins**, nine ACL-monitored roles and eleven membership edges. Current success strings literally say `logins=7 effective_roles=9 memberships=11`; the runbook explains the graph. Four external identities, not six, are supplied to psql. Existing fixtures can still intentionally exercise the historical six-login/four-edge graph before current provisioning.

Implement **zero identity/topology delta**, seven current logins before/after, while preserving historical six-login fixtures at their original targeted boundaries. Do not delete a login or change a marker to satisfy stale prose. This is a source-evidenced numerical correction, not authorization for new identities. Reviewers must explicitly adjudicate it; Controller owns correcting its Control wording outside this write gate. No technical design question remains under the binding “existing graph unchanged” rule. If Controller instead intends exactly six current logins, stop for renewed goal confirmation: that contradicts preservation and cannot be implemented in PA40A.

## 2. Exact Minimum Grant Matrix

All four objects and schema are owned by **`catalog_order_owner`**; every new tuple has grantee **`customer_iam_role`**, privilege **SELECT**, grantor **`catalog_order_owner`**, `is_grantable=false`, kind `column`. No new schema tuple: V016 already supplied catalog schema USAGE. Exactly **22** new tuples, not table SELECT.

| Object in `catalog_order` | Exact columns | Count | Necessity |
| --- | --- | ---: | --- |
| `catalog_product` | `id, product_code` | 2 | `id` proves the version/product join; `product_code` is response and first sort/seek key |
| `catalog_version` | `id, product_id, version_no, sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments` | 10 | `id` is response/child join identity; `product_id` joins the selected product; `version_no` is response/descending seek key; `sale_status` proves ACTIVE; remaining six columns are exact stored response/commercial/filter facts |
| `catalog_offer` | `id, catalog_version_id, channel, offer_status, version_token` | 5 | `id` is response/final UUID seek key; parent ID proves association; channel is response/authorized filter; status excludes WITHDRAWN; token is the stored response token |
| `catalog_benefit_line` | `catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months` | 5 | parent ID prevents cross-version benefit attachment; the other four are precisely the four response fields; entitlement type also proves uniqueness/deterministic ordering |

### 2.1 Deliberately excluded: every other column of the four tables

| Object | Denied columns | Why no capability is required |
| --- | --- | --- |
| product | `product_family, created_at` | neither projection, sort/filter nor join/integrity witness |
| version | `grant_interval_months, snapshot, snapshot_sha256, created_by_employee_id, created_at, published_at, revision` | version interval is not a CatalogOffer response field; line interval is independent stored benefit content. The version billing matrix is already DB-constrained; reading another scalar to re-prove it is unnecessary. Snapshot/hash and staff/time metadata belong to publication/commands, not this projection. ACTIVE plus the validated existing publication-shape constraint proves published state. Revision is not versionToken, sort or cursor data |
| offer | `created_at, revision` | not projected or used for ordering; token is read directly, never fabricated from revision |
| benefit | `id` | immutable surrogate not projected; `(catalog_version_id, entitlement_type)` is the required unique association identity |

V004 defines 34 columns across these objects; 22 are allowed and **12 adjacent columns remain denied**. No owner/account/organization column is needed on these tables. No employee-account access, snapshot parsing/hash recomputation, publication timestamp or extra future convenience grant is justified. In particular **do not invent equality between version interval and every benefit interval**: V004 and current draft semantics constrain them independently. That would both over-grant and invent a future endpoint rule.

### 2.2 Integrity basis, not future endpoint implementation

Capabilities must permit a later owner projection to use `version.product_id = product.id`, `offer.catalog_version_id = version.id`, `benefit.catalog_version_id = version.id`, ACTIVE version and ACTIVE offer predicates, and authorized MINIAPP/WEB channel plus audience/offerType filtering. Frozen ordering is `productCode asc, versionNo desc, catalogOfferId asc`; no timestamps or benefit surrogate ID are sort keys. Filter digest is channel/audience/offerType. Cursor encoding/authentication is explicitly deferred; it does not require another DB column.

Integrity required for this capability: match exact parent IDs; use stored price/token/benefits, not caller input or revision-derived values; validate representable product code, positive version/installments/quantities/validity, benefit registry and nonnegative line interval, CNY (V004 allows other uppercase currencies but the response does not), quote-null-price and audience/offer/billing shape; detect duplicate/mismatched associations. PKs/FKs/unique keys and named checks already provide structural integrity; publication owner semantics provide approved immutable commercial facts. No extra permission can prove that a hostile database owner has not rewritten all facts. Snapshot/hash access would widen disclosure without establishing an independent trust root. Do not port write-side snapshot verification into this read ACL prerequisite.

The contract permits empty `benefitLines` (no minItems); do not create a mandatory nonempty-benefit rule. The SQL proof may use a LEFT JOIN or separate parent-keyed benefit query without inventing a business restriction. Shape/association validation implementation remains PA40 work, not application code in this slice. Frozen errors are UNAUTHENTICATED/FORBIDDEN/VALIDATION_FAILED/CURSOR_INVALID/CURSOR_FILTER_MISMATCH/RATE_LIMITED/SERVICE_UNAVAILABLE/INTERNAL with existing operation response metadata; PA40A adds none.

**Security limit:** column ACLs are not row visibility, RLS, JSON-path security or endpoint authority. The raw customer role can read permitted columns of DRAFT/OFFSALE and other-channel rows. ACTIVE/channel/customer-scope predicates remain trusted-application work in PA40. Do not claim this migration itself filters rows or enables an HTTP operation.

## 3. Current Source State And Arithmetic

V004 lines 54–153 create these exact columns, FK identities and indexes; lines 421–435 grant catalog schema USAGE and whole-table SELECT/INSERT to `catalog_order_role`, with column UPDATE rights on mutable catalog heads. None grants these four tables to customer role. V016's owner-issued delta grants catalog schema USAGE plus only `customer_order(subject_id, account_id, owner_organization_id)` for the customer subject-detail count. V017 adds nineteen evidence columns; V018 adds only `evidence_role` association columns, not customer catalog reads.

PA25 `$frozen$` JSON was parsed independently: **410 rows, 410 distinct tuples**. Its mixed representation has 391 six-field rows and 19 seven-field rows with explicit false; canonicalize each to the first six identity fields plus false for the seven-field effective oracle. Preserve those historical row representations; do not double-append false. Independent `EvidenceRoleIvrAssociationAclIT.currentOracle()` is the frozen V018 410-tuple oracle; keep it historically exact, do not silently redefine it.

| Monitored role | Current | Final | Delta |
| --- | ---: | ---: | ---: |
| customer_iam_role | 53 | 75 | +22 |
| evidence_role | 18 | 18 | 0 |
| evidence_media_reader_role | 16 | 16 | 0 |
| sharing_role | 23 | 23 | 0 |
| sharing_command_role | 57 | 57 | 0 |
| sharing_public_read_role | 26 | 26 | 0 |
| sharing_admission_role | 97 | 97 | 0 |
| operations_iam_role | 14 | 14 | 0 |
| ivr_role | 106 | 106 | 0 |
| Total | **410** | **432** | **+22** |

Customer53 = six schema USAGE + 47 column SELECT; final75 = six schema USAGE + 69 column SELECT. Seven-role subtotal290→312; Operations14+IVR106=120 unchanged; 312+120=432. The non-customer subtotal357 is unchanged. V019 uses a **customer-only 53→75 oracle**, not the cross-module 410/432 collector. This avoids coupling migration authority to unrelated role ACLs. Original V014/V015/V016/V017/V018 arrays remain 81→28, 28→42, 42→50, 50→69 and evidence16→18 at their historical target scopes.

**Controller-adjudicated scope clarification (`PA40A-PLAN-REV-A-02`):** Control's “exact ACL graph”, “fail closed” and fresh/upgrade convergence wording means V019 checks exactly customer53→75/delta22 plus its ownership, provenance, PUBLIC/inherited amplification, topology, defaults, dependency and preservation/invariant envelope. It does not make V019 an all-role410→432 reconciliation or repair migration. PA25 and the independent IT own full nine-role410→432 bidirectional exactness, including unrelated-role drift rejection. V019 must preserve unrelated ACL bytes and reject violations of its invariant envelope, but must never grant, revoke, repair or broaden another role to make a full-oracle comparison pass. The later adjudication resolves the earlier Control wording tension without editing Control or reducing end-to-end exactness.

### 3.1 Single shared Java delta oracle

The sole Java delta22 tuple-literal provider is the already planned new `com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT`, with **package-private `static Set<String> catalogDeltaOracle()`** returning an immutable set equal to Appendix B.2, and **package-private `static Set<String> current432Oracle()`** composing a fresh set from historical `EvidenceRoleIvrAssociationAclIT.currentOracle()` plus that accessor. Assert baseline410, delta22, disjointness, union432, exact customer75/noncustomer357 and subtraction recovering410. Neither accessor is an @Test; neither adds a file, method identity, fixture dependency or public API. Keep `EvidenceRoleIvrAssociationAclIT.currentOracle()` exactly at its existing historical410 definition; its122/2 accessors also stay historical.

All same-package current consumers use these accessors, never independent delta tuple literals: new IT's ACL and PA25 proofs; `ProductionDatabaseIdentityProvisioningIT.assertExactFrozenAcls`; `OperationsCatalogProductReadRoleLeastPrivilegeIT.assertsNoNewGrant`; V018 IT's current PA25/login/fresh legs; V015 current PA25 and catalog-capability legs; V016 `currentOracle`; V017 `latestOracle`; `ActivationCommandRoleLeastPrivilegeIT.fixtureCleanupAndFrozenAclInventoryRemainExact`; and `OperationsLifecycleRoleLeastPrivilegeIT.frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact`. Seven-role consumers project `current432Oracle()` by excluding exactly operations_iam_role and ivr_role, yielding312; independently retain their old288+association2=290 comparisons against the corresponding historical410 projection. Do not append22 twice to a current helper.

Cross-package architecture tests that need the current432/delta22 expected sets use read-only reflective access to these two package-private accessors (`Class.forName`, `getDeclaredMethod`, `setAccessible(true)`), not a public accessor or another literal. PA25 SQL necessarily has its explicit reviewed22 literal rows; V019 necessarily has its customer target literals; these are independently compared against the provider, never parsed to manufacture the provider's expected tuples. `APPROVED` maps are column-authorization expectations, not additional tuple-oracle providers; their catalog projection must equal this accessor's four-table22-column projection. No runtime catalog is a source of expected values.

Required owner/grantor identities already exist: catalog_order_owner (all new grants); customer_iam_owner, organization_access_owner, subject_number_owner, activation_consent_owner, entitlement_metering_owner, evidence_owner, sharing_owner, platform_reliability_owner, audit_owner, operations_iam_owner and ivr_owner remain the twelve PA25 oracle grantors. All existing namespace/relation/type/routine owners and existing grantors remain unchanged. Migration runner is the existing protected unmanaged superuser, not a new login or runtime owner; the explicit local owner switch ensures new ACL grantor provenance.

## 4. Exact V019 SQL Construction Contract

Path: `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql`; Flyway description `customer catalog offer read privileges`. One transaction, one anonymous `DO $migration$` block following V018's fail-closed pattern; no persistent helper/collector object. Use V018 as a structural reference, **not a blind version-number substitution**.

The complete mutation payload is exactly these four grants inside the guarded clean-prestate branch:

```sql
SET LOCAL ROLE catalog_order_owner;
GRANT SELECT (id, product_code)
    ON TABLE catalog_order.catalog_product TO customer_iam_role;
GRANT SELECT (id, product_id, version_no, sale_status, offer_type, audience,
    price_cents, currency, billing_cycle, installments)
    ON TABLE catalog_order.catalog_version TO customer_iam_role;
GRANT SELECT (id, catalog_version_id, channel, offer_status, version_token)
    ON TABLE catalog_order.catalog_offer TO customer_iam_role;
GRANT SELECT (catalog_version_id, entitlement_type, quantity, validity_days,
    grant_interval_months)
    ON TABLE catalog_order.catalog_benefit_line TO customer_iam_role;
EXECUTE format('SET LOCAL ROLE %I', session_user);
```

No `WITH GRANT OPTION`, USAGE regrant, REVOKE, ALTER DEFAULT PRIVILEGES, table privilege, business statement, function creation/execution grant, role/member mutation or owner change. GRANT's `ON TABLE` syntactic form with explicit columns is column-level SELECT, not table-wide SELECT. Granting under the owner role is mandatory even if the migration session itself is superuser.

### 4.1 Deterministic guard and assertion specification

1. Declarations: `target_names = ARRAY['customer_iam_role']`; literal `$old$` consists of exactly the 53 customer tuples in the current independently verified V018 baseline (Appendix B); `$final$` consists of those unchanged entries plus the literal 22 entries generated by the matrix in Appendix B, total75. Do not dynamically learn the accepted ACL state from the live database. `$history$` is the exact current PA25 18-row array, unmodified, including V018 checksum1606293398. Keep expected owners/routines and current topology invariants from V018/PA25. Use separately named old/final arrays; fail if lengths/distinct lengths are not 53/75 or delta is not precisely the matrix.
2. Authority: PostgreSQL `server_version_num BETWEEN 160000 AND 169999`; `current_user=session_user`; that role is superuser and outside the managed identity graph. Acquire `pg_advisory_xact_lock(725025,1)`; transaction-local `search_path=pg_catalog`. No caller-controlled object names in unquoted SQL; relation/schema/role resolution must fail if missing or ambiguous. Read actual catalogs; never trust has_column_privilege alone to establish tuple provenance.
3. Provenance: retain current UTF-8 byte-exact PA25 v1 marker algorithm, OID/database/slot/name binding; accept only 0, 3 or 4 correctly marked external slots, in their exact known order. Zero is the fresh unprovisioned path; three is approved historical provisioning; four is current provisioning. Reject collisions, duplicate/forged/missing-slot markers and unmanaged dependencies. **Unlike V018, four markers are allowed with predecessor history18 and old customer53**: deployed V018→V019 must not be rejected for already having LO. Do not carry V018's `marked_count=4 requires self18` rule forward as “requires self19”.
4. History: exact successful SQL rows1–18 match version (zero-padded), rank, script, description and signed checksums in Appendix A/current `$history$`; total history row count IN(18,19), no extra/baseline/failed/repeatable/null-checksum/rank0/duplicate/unexpected row. Optional row19 must be rank19/version019/typeSQL/successtrue/exact script/exact description/non-null checksum. There is no circular self-checksum literal inside V019; Flyway validate, independent test and PA25 pin concrete V019 CRC after bytes are final. Missing/altered V018 or any V020 is rejected.
5. Replay state machine: run two verification passes. On pass1 compare complete collected customer ACLs with **exact old53 OR exact final75**, never a subset. Old53 is accepted only with history18. Complete75 is accepted with history18 (raw application/replay before Flyway registration) or valid history19. History19+old53 is drift, not a repair invitation. Any 1..21-of22 partial state, extra column/table/sequence/routine/schema right, wrong grantor, grantable tuple, PUBLIC/inherited amplification or touching unknown ACL rejects. Pass2 must equal exact final75. GRANT only when pass1 is old53; full state executes no grants.
6. Collector: resolve literal schema/relation/column/routine identifiers to exact OIDs/attnums and reject NULL resolutions/duplicates; use V018's union across schema, table, sequence, column and routine ACLs with acldefault for implicit owner defaults. Boundary includes customer role plus applicable external logins and the unchanged full PA25 role graph. Inspect both grantee/grantor directions where the existing guard does; inspect all user namespaces, including unexpected objects, rather than just four allowed tables. Compare sorted tuples including grantor and grantability. Restrict the **expected exact privilege oracle to customer role**, while separately checking PUBLIC, external-login direct privileges, dependencies, ownership, topology, defaults and role-effective amplification. Never add owners to the exact target oracle (their inherent privileges are not grants).
7. Role/ownership/topology safety: preserve existing safe LOGIN/NOLOGIN/INHERIT flags, role attributes, marked identity settings, exact approved edges/options/superuser grantors, database CONNECT versus CREATE/TEMP rules and project owner map. Zero/four-edge/eleven-edge states depend on approved provenance, not a request to add edges. Verify catalog schema and four relations/types are catalog_order_owner-owned. Preserve known routine SECURITY DEFINER/config/search_path checks. No new schema/object/column/index/trigger/function/type/sequence/role/member/settings/comment is created or changed.
8. PUBLIC/default/dependency safety: retain existing user-object PUBLIC ACL rejection and target-effective privilege checks; reject unexpected default ACLs involving managed roles as creator/grantee/grantor, role/database settings, extension membership or shared-dependency drift. Do not issue blanket PUBLIC revokes or “repair” default grants. System-catalog built-in visibility is not a new project privilege. Existing non-target ACLs are invariant snapshots, not forced to432 inside the migration.
9. Preservation: use the two-pass V018 invariant snapshot of role catalogs/verifiers/comments, memberships, role settings, default ACLs, databases, routine definitions/configs, constraints, indexes, triggers, owners and other ACLs. Add exact user-relation/column/type/definition inventory comparison where needed to prove the declared no-schema delta. Account for only the 22 new customer column ACL entries and corresponding ACL dependencies. Compare all other attacl entries including owner/non-target grantees; do not drop whole catalog rows or entire ACL arrays to hide drift. Do not include expected Flyway self insertion inside the DO snapshot; Flyway appends it afterward. Business row fingerprints are executable test obligations, not migration scans/DML.
10. Sanitization: categories `V019_AUTHORITY_REJECTED`, `PROVENANCE`, `HISTORY`, `SETTINGS`, `ATTRIBUTES`, `TOPOLOGY`, `OWNERSHIP`, `ACL`, `DATABASE`, `DEFAULT_ACL`, `DEPENDENCY`, `PRESERVATION`, `RECONCILIATION` with the common `V019_..._REJECTED` form. Existing exception handler emits only SQLSTATE P0001 and category; no detail/internal SQL, credentials or private row values. Test each category. All failures roll back the DO transaction, including failed post-grant assertions.

Fresh chain V001→V019 and upgrade18→19 converge modulo environment-specific OIDs/history execution timestamps: compare normalized named ACL graph and exact logical schema definitions, not cross-container OID identity. Flyway migrate again executes zero resources. Raw V019 replay under valid history18/final75 or history19/final75 is a no-op. Do **not** replay V001–V018 after V019; their historic guards intentionally recognize their own prefix states only. No down migration or production REVOKE rollback is authorized: operational rollback means transaction abort/restore known prior DB backup under Controller/Database ownership, not weakening the final oracle.

## 5. PA25 And Legacy-Proof Integration

Modify only current expectation literals/matrix/history in `backend/ops/postgresql/pa25-runtime-identities.sql`:

- `$frozen$` (declaration near line50): retain all410 current mixed six/seven-field tuples byte-semantically and append22 owner-issued column tuples. Require cardinality432 and432 distinct. No schema-USAGE duplicate. Every grantable flag remains false when resolving.
- `$history$` (ends near line3475): retain all18 entries/checksums, append `["019", "V019__customer_catalog_offer_read_privileges.sql", <computed signed CRC>, "customer catalog offer read privileges"]`. Concrete CRC/SHA are measured after migration text finalizes, never guessed here.
- `$pa25$` history guard near3818: exact18→19, not minimum-version acceptance. Require V019 success; reject V018-only and V020/duplicate/failed metadata.
- Expected-resolved length/distinct checks near3913–3914 and actual ACL length check near3944: all410→432; maintain bidirectional set equality, NULL-resolution/grantor/option checks. Counts alone do not prove correctness.
- `external_names`, `slots`, `effective_names`, `shared_context_targets`, marker algorithm, grantor checks, approved11 edges, seven LOGIN identities, monitored9 roles and all success markers remain exact. PA25 does not issue catalog grants. `apply` creates only approved external identities/edges; `check` performs no mutation; neither repairs missing V019 privileges.
- Current graph: LQ→sharing_role; LC→customer_iam_role; LW→sharing_command_role/evidence_role; LO→operations_iam_role/subject_number_role/activation_consent_role/entitlement_metering_role/evidence_role/catalog_order_role/ivr_role. All11 ADMIN FALSE/INHERIT FALSE/SET TRUE, unmanaged superuser grantor. Three existing direct login roles have no touching membership. LC has exactly one direct SET edge and no pre-SET object capability; “direct customer login” is not a new inheriting role/login topology.

Current apply/check requires19, so upgrade tests must not invoke updated PA25 on a DB stopped at18. For an already provisioned18 DB, preserve a V018-compatible PA25 prefix as **test-local frozen text reconstructed from the new script** (remove only the exact22 tuples and exact row19, restore only432→410/19→18 guard literals after asserting old/new tagged arrays). Verify the reconstructed prefix hash against the current pre-change PA25 raw/LF pins (raw `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`, LF `8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451`), then run it for the old four-slot graph. It is not a shipped second operational script and must not be reconstructed by broad string replacement. Alternatively use the existing explicit historical graph fixture for the three-slot path. Use normal new PA25 only after applying19. Capture private verifier/OID/comment/grantor snapshots internally, compare without printing.

Unchanged source-expected topology quantities: four external slots plus three direct logins; `managed_names`13 (external4 + monitored9); capability boundary13 (monitored9 + context4); full boundary17 (external4 + monitored9 + context4). The current runbook's project-role expectation remains31 before external provisioning and35 afterward. These are project-boundary quantities, not invented total `pg_roles` row counts, which also include PostgreSQL built-ins and the disposable administrator. Preserve these formulas and all membership grantor/option identities in apply/check/fresh/upgrade proofs.

### 5.1 Maintain historical tests without erasing evidence

Keep all **1684 existing method identities unchanged**, including historically named `noV019IsNeededOrPresent`, `preservesFrozenInputsAndNoV019`, `postgres16RunsExactlyV001ThroughV018` and `pa25RequiresV018AndExact410OracleInBothModes`. Their historical clauses still prove no PA38/PA39 migration or grant was needed; append an explicit PA40A current-chain assertion allowing exactly V019 and forbidding V020. Do not rename methods in old CI addition maps or rewrite historic expected18/410 arrays into19/432. Live-chain assertions advance separately. New tests have accurate current names.

- V014 tests keep target014 replay/oracle81→28 and frozen13 history. Their `current` tail migrate014→latest changes4→5 and live history18→19; `assertFrozen` admits19 exact files while retaining old pins.
- V015 `latest()` tail015→latest changes3→4; current seven-role290→312, through §3.1's provider. `v015FreshAndUpgradeHaveExactFifteenHistoryEntriesAndFrozenBytes` retains target15 and historic hash map; its predecessor capture excludes V019 along with V016–V018 rather than reclassifying V019 as a predecessor.
- V016 `latest()` currently targets018: target019 for PA25/current-role paths. Old016 replay tests remain target016; tail015→latest3→4 and016→latest2→3; `currentHistory()` explicitly asserts19 and row019, retaining prior row018 assertion as a prefix. Extend its seven-role `currentOracle()` with22, not its historic269 oracle.
- V017 `latest()` targets018 and expects1: current PA25 paths advance to019 and expect2; `latestOracle()` gains22 on top of historical288+association2=290→312. Original17/288/53/69 expectations stay targeted. History-negative loops extend to19; add duplicate019/future020, retain original malformed18 probes.
- V018 `latest()/upgrade()` and `currentOracle()/additionalOracle()/associationDelta()` remain **historical target018 / 410 /122 /2**. Their replay/drift tests run at018 and continue proving exact evidence16→18 and no customer delta. For methods calling `provision()` or fresh `start(null)`, advance to019 **after** all V018 assertions/replays, compare full current432, then require `current432 minus catalogDelta22 == historical410`. Never replay V018 after019. `freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors` uses a separately targeted fresh018 leg, then explicit latest019 leg; before PA25 tests pre018/018 histories reject, only019 accepts. Missing-tuple iteration must cover all432 current tuples, not only410. Attribute/provenance/atomic-failure helpers must snapshot at the correct phase rather than call PA25 prematurely.
- `ProductionDatabaseIdentityProvisioningIT.assertExactFrozenAcls()` currently composes historical288 plus `EvidenceRoleIvrAssociationAclIT.additionalOracle()`122=410. Retain that assertion and compare current results to §3.1's `current432Oracle()`; explicitly prove subtraction recovers410. `start()`/current history18→19. Retain adversarial tests for rank18 and add rank19, duplicate019 and future020; use isolated restoration/sentinel rank1019 with original1018 probes preserved, not accidental collision with real rank19.
- Other historical seven-role reflection oracles currently expect290: `ActivationCommandRoleLeastPrivilegeIT` and `OperationsLifecycleRoleLeastPrivilegeIT` use §3.1's projection and assert312. Operations nine-role oracle in `OperationsCatalogProductReadRoleLeastPrivilegeIT.assertsNoNewGrant` uses `current432Oracle()`, then explicitly checks **catalog role's own42 tuples unchanged**. `OperationsCatalogDraftFixture.provisioningOracle()` does **not** delegate to this IT: it creates an independent `CustomerBusinessNumbersFixture`, calls `start(null)`, then checks PA25 exit0 and unchanged seven-login/eleven-edge success text. Its own body needs no edit; its two callers' history pins do (§5.4).

### 5.3 Explicit exhaustive customer capability methods

Authorize these existing methods and their class-level `APPROVED` maps (no renames or new @Test identities):

| Exact method | Required historical/current phase correction |
| --- | --- |
| `CustomerBusinessNumberReadPrivilegeIT.customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501` | Existing body is target015 (`upgrade(); frozenGraph()`), not latest. Preserve that historical261/customer26 proof and its complete capability loop. Then advance015→019 through `latest()` (4 migrations), provision current PA25 and run the same exhaustive capability assertions at current19. Its original six-table `APPROVED` map has22 columns (customer26 includes four schema tuples): append exactly catalog22 (44 columns across10 tables). For the current loop compose the historical410 customer column projection47 plus these catalog22, yielding69; for the historical loop use the original22-column projection (remove only the new catalog entries). The additional25 intervening V016/V017 columns belong to the explicit historical410 overlay, not new V019 grants or silent additions to the old map. |
| `CustomerSubjectDetailReadPrivilegeIT.customerRoleReadsOnlyApprovedColumnsAndDeniesAllOtherCapabilitiesWith42501` | Existing body already advances015→018 and provisions. Advance to019 instead; exact `migrationsExecuted`3→4, `currentHistory()`19. Its original12-table47-column `APPROVED` map gains exactly four catalog entries22, total16 tables/69 columns. Existing historical016/269 assertions in other methods stay targeted. |
| `CustomerEvidenceReadPrivilegeIT.customerRoleReadsOnlyApprovedFactsAndDeniesAllSensitiveCapabilitiesWith42501` | Existing body is target017 (`upgrade(); frozenGraph()`), not latest. Preserve historical288/customer53, evidence canary/seed and exhaustive assertions with original12-table47-column projection. Then advance017→019 through `latest()` (2 migrations), provision and repeat capability assertions using the updated16-table69-column `APPROVED` map. Compare the historical phase to existing `currentOracle()`288 and the final phase to `latestOracle()`312 (seven monitored roles); do not redefine historical288. |

Each map receives exactly product `(id, product_code)`; version `(id, product_id, version_no, sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments)`; offer `(id, catalog_version_id, channel, offer_status, version_token)`; benefit `(catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months)`. Preserve every old map entry. Compare the catalog projection against `catalogDeltaOracle()` rather than introduce another delta tuple literal. Separate historical and current loop expectations explicitly; adding allowed columns to a map while still running only at015/017 would itself fail and is not a valid fix.

Preserve the existing complete `pg_class`/`pg_namespace` table enumeration, `pg_attribute` non-dropped column enumeration and their existing public/system exclusions. Do not exclude catalog tables, narrow the loop to an allowlist, skip unapproved columns, weaken `denied`/savepoint helpers, or use broad has-privilege shortcuts. Keep every adjacent12 catalog-column denial and every other unapproved-column denial, whole-row/`SELECT *`, per-column INSERT/UPDATE, DELETE/TRUNCATE, REFERENCES/trigger/DDL, row-lock UPDATE/SHARE, routine, sequence, role/privilege-escalation and grant-non-amplification probe, transaction settings, seed/canary and before/after ACL-preservation assertion. Historical grant/replay tests remain unchanged at their original version. The new IT adds focused catalog22/12 proofs, not a fourth replacement for these full-catalog loops.

Two further exact V018 method bodies must change within its already allowed path: `evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn` calls the changed current `provision()` and ends with historical410 equality, so preserve all evidence probes but make its final equality current432 with explicit delta subtraction; `v018ReplaysOnlyExactCleanOrCompleteStatesWithoutDrift` must keep all replay phases at018/410 and route its topology==2 PA25 apply through the hash-verified historical18/410 prefix from §5, not updated current PA25 and not `provision()` that advances019. Only this isolated historical call changes script selection; current PA25 entry points must still reject18. No V018 replay after019 and no blanket version-sensitive fallback in operational PA25.

### 5.4 Complete null-start call graph and affected pins

All Java paths in the following call-graph tables are under `backend/src/test/java/com/onecall/archtests/container/`; suffix `.java` is explicit. `start(null)` means `CustomerBusinessNumbersFixture.start(null)`, not arbitrary fixtures with a `start` method. There are **11 direct call sites in11 paths**, and the composed null-start graph has **34 paths:12 fixtures (root included) +22 terminal IT classes**. The three additional direct users of the root fixture target only014/015/016 in the current source (`CustomerBusinessNumberReadPrivilegeIT.start`, `CustomerSubjectDetailReadPrivilegeIT.start`, `CustomerEvidenceReadPrivilegeIT.start`); they are the historical branches covered in §5.3, not omitted null callers. Including them gives37 root-fixture-related paths. Static imports of `execute`/`one`/session helpers alone are not startup edges.

| Direct call path and method | Exact edge / disposition |
| --- | --- |
| `CustomerBusinessNumbersIT.java: start` | `fixture.start(null)`; terminal's history pin below changes. |
| `EvidenceRoleIvrAssociationAclIT.java: freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors` | fresh `fixture.start(null)`; separate historical018 from current019/432 legs. |
| `OperationsActivationReadRoleLeastPrivilegeIT.java: frozenAclRolesFunctionsAndAllFactsHaveZeroDelta` | `baseline.start(null)` independent PA25 check; same method's history pin changes. |
| `OperationsCatalogDraftFixture.java: provisioningOracle` | `independent.start(null)`; no local changing assertion, read-only fixture. |
| `CustomerSubjectDetailFixture.java: start` | `base.start(null)` → `CustomerSubjectsIT.start`. |
| `CustomerEvidenceFixture.java: start` | `base.start(null)` → `CustomerEvidenceIT.start`. |
| `CustomerSubjectClaimFixture.java: start` | `base.start(null)` → `CustomerSubjectClaimsIT.start`, `SubjectClaimRoleLeastPrivilegeIT.start`. |
| `CustomerSubjectActivationFixture.java: start` | `base.start(null)` → `CustomerSubjectActivationsIT.start`, `ActivationCommandRoleLeastPrivilegeIT.start`, and lifecycle composition below. |
| `OperationsSubjectReadFixture.java: start` | `base.start(null)` → `OperationsSubjectReadsIT.start`, `OperationsSubjectReadRoleLeastPrivilegeIT.start`. |
| `OperationsActivationReadFixture.java: start` | `base.start(null)` → `OperationsActivationReadsIT.start`, `OperationsActivationReadRoleLeastPrivilegeIT.start` (in addition to that IT's separate direct baseline). |
| `OperationsIvrSessionReadFixture.java: start` | `base.start(null)` → `OperationsIvrSessionReadsIT.start`, `OperationsIvrSessionReadRoleLeastPrivilegeIT.start`, and catalog composition below. |

The four composed fixture startup edges are exact: `OperationsSubjectLifecycleFixture.java: start → customer.start()` (CustomerSubjectActivationFixture), with terminal `OperationsSubjectLifecycleIT.start` and `OperationsLifecycleRoleLeastPrivilegeIT.start`; `OperationsCatalogProductReadFixture.java: start → shared.start()` (OperationsIvrSessionReadFixture), with terminal `OperationsCatalogProductReadsIT.start` and `OperationsCatalogProductReadRoleLeastPrivilegeIT.start`; `OperationsCatalogVersionLifecycleFixture.java: start → catalog.start()` (OperationsCatalogProductReadFixture), with terminal `OperationsCatalogVersionLifecycleIT.start` and `OperationsCatalogVersionRoleLeastPrivilegeIT.start`; `OperationsCatalogDraftFixture.java: start → base.start()` (OperationsCatalogVersionLifecycleFixture), with terminal `OperationsCatalogDraftLifecycleIT.start` and `OperationsCatalogDraftRoleLeastPrivilegeIT.start`. Both draft terminals also call `provisioningOracle()` from the exact methods below. All eleven non-root fixtures are read-only: none has a changing history/ACL/count assertion. Their pool wiring, synthetic role topology and startup methods need no edit.

| Exact terminal path | Exact changing method / pin, or explicit read-only result |
| --- | --- |
| `CustomerBusinessNumbersIT.java` | `postgres16FullChainAndCustomerRuntimeRoleAreUsed`: live history18→19. |
| `CustomerSubjectsIT.java` | `postgres16LatestChainAndCustomerRuntimeRoleAreUsed`: live history18→19. |
| `CustomerEvidenceIT.java` | `postgres16LatestChainAndVerifiedCustomerRoutesAreWired`: live history18→19. |
| `CustomerSubjectClaimsIT.java` | `postgres16FrozenSeventeenMigrationChainAndDedicatedLoginAreUsed`: live history18→19; historical prefix17 unchanged. |
| `SubjectClaimRoleLeastPrivilegeIT.java` | `frozenAclsHistoryAndCustomerQueryPrincipalRemainUnchanged`: applied history18→19; existing ACL preservation snapshot unchanged. |
| `CustomerSubjectActivationsIT.java` | Read-only: `start` reaches null through CustomerSubjectActivationFixture; no changing history/ACL oracle or migration-resource pin in any method. |
| `ActivationCommandRoleLeastPrivilegeIT.java` | `fixtureCleanupAndFrozenAclInventoryRemainExact`: historical seven-role290→current312 provider projection. |
| `OperationsSubjectLifecycleIT.java` | Read-only: `start` reaches null through lifecycle/customer activation; no changing history/ACL oracle or migration-resource pin in any method. |
| `OperationsLifecycleRoleLeastPrivilegeIT.java` | `frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact`: current seven-role290→312; preserve prefix17 and row018, add exact row019/current overlay. |
| `OperationsSubjectReadsIT.java` | `postgres16AppliesExactlyFrozenSeventeenMigrations`: live history18→19. |
| `OperationsSubjectReadRoleLeastPrivilegeIT.java` | `frozenAclOracleMigrationsRolesAndFunctionsRemainUnchanged`: live history18→19. |
| `OperationsActivationReadsIT.java` | `postgres16UsesExactlyFrozenSeventeenMigrations`: exact packaged/source migration set18→19 and history18→19; frozen hash map retained. |
| `OperationsActivationReadRoleLeastPrivilegeIT.java` | `frozenAclRolesFunctionsAndAllFactsHaveZeroDelta`: live history18→19; independent baseline PA25 check keeps its unchanged success marker. |
| `OperationsIvrSessionReadsIT.java` | `postgres16UsesExactlyFrozenEighteenMigrations`: resource size/range18→19, exact current27 overlay substituting PA25 SQL/runbook/README raw+LF pins plus V019, and live history19; §5.2's independent historical26 evidence remains. |
| `OperationsIvrSessionReadRoleLeastPrivilegeIT.java` | Read-only: `start` reaches null through shared IVR fixture; no changing history/ACL oracle or migration-resource pin in any method. Its role-scoped capability/pool assertions remain exact. |
| `OperationsCatalogProductReadsIT.java` | Read-only: `start` reaches null through product/shared IVR; no changing history/ACL oracle or migration-resource pin in any method. Projection/order/pool assertions remain exact. |
| `OperationsCatalogProductReadRoleLeastPrivilegeIT.java` | `assertsNoNewGrant`: full410→432 through provider; catalog role42 unchanged. |
| `OperationsCatalogVersionLifecycleIT.java` | Read-only: `start` reaches null through version/product/shared IVR; no changing history/ACL oracle or migration-resource pin in any method. `v001ThroughV018ProvideAllRequiredObjects` belongs to the role IT in the next row, not this lifecycle IT. |
| `OperationsCatalogVersionRoleLeastPrivilegeIT.java` | `v001ThroughV018ProvideAllRequiredObjects`: live history18→19; `noV019IsNeededOrPresent`: preserve PA38 historical18 projection, add current exact19 and row019=1, reject020/extra names. |
| `OperationsCatalogDraftLifecycleIT.java` | `postgres16RunsExactlyV001ThroughV018`: exact history string range1..18→1..19; `provisioningOracle()` call unchanged. |
| `OperationsCatalogDraftRoleLeastPrivilegeIT.java` | `laterMigrationsAndProvisioningRetainExactEffectiveAclOracle`: live count18→19 and019 absent0→present1, plus no020; independent PA25 call unchanged. |
| `EvidenceRoleIvrAssociationAclIT.java` | `freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors`: direct null-start current leg19/432, explicit targeted fresh018/410 leg retained; other exact affected current/replay bodies are in Appendix C and §5.3. |

The read-only catalog-version lifecycle terminal makes **five** terminal IT classes outside the write set; **17** terminal IT paths plus root fixture `CustomerBusinessNumbersFixture.start` are modified. That root's null `migrationsExecuted`18→19 changes; explicit non-null targets still use `Integer.parseInt(target)`, and `provision()` still runs only on the final null path. No file is added merely because its startup transitively changes. The complete Appendix C additionally covers all independent/non-null fixtures, SQL/source/manifest guards and PA25 consumers outside this null-start graph.

### 5.5 Global exact-pin audit and missing architecture guard

The bounded fix searched the entire `backend/src/test` tree, not only container IT, for `start(null)`, `history18`, `applied18`, range1..18 (including `rangeClosed`, `<18`, SQL history predicates and string literals), `hasSize(18)`, numeric/string `isEqualTo(18)`, V018/V019/no-V019 guards, exact410/current ACL sets,290 projections, PA25 history/ACL arrays and source tokens, migration/resource lists, manifest318/319, testJava190, ordinary-test/annotation counts and identity inventories. It separately inspected PA25 SQL, README/runbook, `.github/workflows/ci.yml` and POM includes. The result is exactly Appendix C's44 existing test/fixture paths; no further affected path was found. This is a source audit, not a green runtime claim.

Explicitly authorize `OperationsSubjectReadBoundaryTests.v001ThroughV017AndFrozenOpenApiAndDesignDocsRemainUnchanged`: retain all V001–V017 and frozen OpenAPI/design-document byte pins; preserve historical18 evidence separately; advance its current resource expectation from `rangeClosed(1,18)` to exact V001–V019. Validate **full expected basenames**, not just the existing four-character version-prefix projection (which could accept a wrong-name replacement). Within this same existing method, use a test-local non-@Test `assertCurrentMigrationNames` helper for the positive set and negative in-memory sets containing `V020__unexpected.sql`, an extra wrong-name `V019__unexpected.sql`, and replacement of the approved V019 basename by that wrong name. Each must reject, including the replacement case whose count is still19; retain duplicate/missing-resource rejection. Do not create V020, touch migrations for a unit probe, or weaken exact equality to containsAll/startsWith. The helper must be exercised by the live resource list and all hostile lists, not a detached arithmetic demonstration. This guard is added to the focused unit selector in §9.

Outside the null graph, exact affected independent startup/history paths are `CustomerMeFixture.start`, `SharingQueryFixture.start`, `SharingRoleLeastPrivilegeIT.start`/`v010MigrationIdentityAndPostgres16`, `EvidenceMediaReadIT.Fixture.start` (not the outer delegating @BeforeAll method), `EvidenceMediaReadRoleLeastPrivilegeIT.roleAttributesPasswordTopologyAndV013HistoryAreExact`, `ProductionDatabaseIdentityProvisioningIT.start`/`frozenMigrationsLogicalAclsAndHistoryRemainUnchanged`, `SharingQueryServiceIT.queryRuntimeStartsAfterFixtureProvisioning`, `PublicShareAdmissionIT.publicGetAndCreatorCreateRevokeRotateListMeRemainUnchanged` and `SharingAdmissionRoleLeastPrivilegeIT.v012CreatesExactAdmissionRoleAndIsReplaySafe`: advance only their full-chain18 assertions to19. V014 live tails, V015–V018 historical/current distinctions, source-only PA25 pins and all other architecture freeze guards are explicitly covered in §5.1–§5.3 and Appendix C.

Transitive independent consumers were also traced without adding unjustified paths: `CustomerMeIT.start → CustomerMeFixture.start`; `CustomerSharesIT.start → CustomerSharesFixture.start → CustomerMeFixture.start`; `CustomerShareCommandsIT.startDatabase`, `SharingQueryServiceIT.start`, `SharingAdmissionRoleLeastPrivilegeIT.start`, `PublicShareAdmissionIT.start → SharingQueryFixture.start`; `PublicSharesIT.startDatabase → PublicSharesFixture.start → SharingQueryFixture.start`; `MediaPlaybackIT.start → Fixture()` constructor → SharingQueryFixture.start; `EvidenceMediaReadIT.start` and `EvidenceMediaReadRoleLeastPrivilegeIT.start → EvidenceMediaReadIT.Fixture.start`. Unchanged consumer methods retain behavior/identities; only the explicit current pins in Appendix C change. Static helper imports, Bootstrap dependency-token checks, HTTP410, enum/problem-code18, catalog commercial quantities, index18, evidence_role18, frozen26 and historical checksums containing those digits are not current migration/ACL counts.

PA25-negative history loops in the existing authorized customer/V018/PA25 methods retain original rank16/17/18 corruptions and add rank19/duplicate019/future020. Any existing mutation `installed_rank=19` collides with the real new row: keep its original meaning in the historical phase, then use an explicitly unoccupied sentinel (1019) for the current phase; restore the exact captured row and preserve the older1018 probes. Current PA25 missing-tuple iterations use `current432Oracle()` without the V018 method's old subset-skipping `continue`, so all432 tuples are exercised; historical410 assertions remain separate. Do not achieve compatibility by running current PA25 at18 or skipping corruptions.

The shared hash-verified historical script reconstruction in §5 is owned by the new IT as package-private non-@Test `static String historicalV018Pa25Script()`. Both its provisioned-upgrade proof and V018's isolated replay/topology method use that accessor and a separate test-local container script path. Normal current `psql`/`success` calls always use shipped19/432 PA25. No blanket fallback, broad replacement or second shipped SQL resource is authorized.

Audit counts: **67 existing @Test entry points +16 existing non-@Test entry points +3 APPROVED fields** are named in Appendix C (previously61 +16 and no explicitly named map fields); six newly authorized existing @Test bodies, zero renamed/deleted/extra ordinary identities. The new IT's two oracle accessors and historical-script accessor and the architecture guard's hostile-list helper are non-@Test. New ordinary identities remain exactly Appendix D's8+16. Implementation-path baseline49 existing →final52 (+3 new); modified49 =test44 +support5. No fixture-only dependency inflates that set.

### 5.2 Frozen-input verifier maintenance, especially PA33–PA39

Do not bulk-regenerate old arrays or historical artifacts. All existing OpenAPI/docs12/14/15/17 and V001–V018 digest rows retain original values. Existing prefix arrays of17 or18 rows remain those sizes; construct current19-file expectations by exact prefix plus V019, and hash V019 separately. No prefix filtering of arbitrary V019 files; only the exact expected basename may be excluded for a historical projection.

`OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact` and `OperationsIvrSessionReadsIT.postgres16UsesExactlyFrozenEighteenMigrations` each contain a 26-row FROZEN map, historical PA35 plan table/bare-line parser, aggregate digests and hostile verifier probes. **PA40A-PLAN-RR-B-01 closure:** preserve each verifier's entire original literal26 byte-for-byte, including all old PA25 SQL/runbook/README pairs; do not regenerate it or replace any old row. It comprises18 migration rows +5 OpenAPI/docs12/14/15/17 rows +PA25 SQL +PA25 runbook +README =26. The following are the sole projection rules, implemented independently in each existing method without renaming it:

1. **Pre-PA40A evidence F26:** parse the unchanged FROZEN literal with unique paths and exactly26 pairs; independently recompute aggregate `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761`. This is immutable pre-change evidence, not a second live/current mode; do not compare changed live PA25/runbook/README bytes to F26.
2. **Historical evidence H26:** copy F26 and substitute only `backend/README.md` with historical raw `fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64` / LF `bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177`. Every other pair, especially PA25 SQL and runbook, comes directly from the preserved FROZEN literal, **not** the §5 historical PA25 prefix reconstruction or today's live files. Keep the original PA35 plan and parser unchanged: original `tableCount == 26`, `bareCount == 26`, exact table/bare/F26 path-set equality, exact table-pair equality to H26 and bare raw-hash equality. Recompute historical aggregate `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`. H26 has exactly18 migrations; V019 is absent from every historical map/table/bare fixture and historical resource projection. Historical byte probes use preserved historical fixtures where available, never relabel live changed bytes as historical evidence; the old map/parser/aggregate evidence does not require fabricating historical runbook bytes.
3. **Only live/current projection C27:** copy F26, substitute exactly the three existing paths `backend/ops/postgresql/pa25-runtime-identities.sql`, `docs/operations/pa25-production-database-identity-provisioning-runbook.md`, `backend/README.md` with their newly measured final raw+canonical-LF pairs, then add exactly `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql` with its measured pair. Thus27 =26−3+3+1, with19 migrations and the same8 non-migration paths; the other23 original pairs remain unchanged. Implementation measures these four final files and pins the current pairs and separately recomputed current aggregate in each verifier before validation; this plan invents no future hash. Runtime assertions compare actual live bytes to those pinned expectations, never derive expected hashes from the same bytes being checked. Within each existing method, add separately named current table/bare representations of C27 (test-local; no new artifact and no edit to the PA35 plan). Require `currentTableCount == 27`, `currentBareCount == 27`, unique paths, exact current table/bare/C27 key equality, exact table-pair equality and bare raw-hash equality. Current parsers/probes are separate from the original historical parser/counters; no `containsAll`, cardinality-only check or optional old/live fallback.
4. **One aggregate formula for all three evidence states:** sort paths by unsigned UTF-8 bytes; for each append UTF8(path), one `0x00`,32 decoded binary raw-hash bytes,32 decoded binary LF-hash bytes, with no delimiter between hashes and no row terminator; SHA-256 the concatenation. F26 and H26 retain their exact existing aggregates above; C27 gets its own measured/pinned aggregate, not either historical constant and not a checksum of hex-text serialization. Keep all original hex-text/extra-delimiter/row-terminator negative interpretations and mutation/negative probes executable.

Both verifiers must actually invoke the independent historical26 evidence assertions and the current27 live-byte verification; a dormant overlay is insufficient. Current migration scanners compare the complete basename set to the18 frozen basenames plus the exact authorized V019, count19; current history is19. Temporary positive current projections copy actual expected V019 and all three changed non-migration files before checking C27, and exercise both LF/CRLF representations without rewriting primary checkout files. Historical projections exclude only that exact V019 basename, not arbitrary prefix matches, and contain no V019 row. Preserve original one-byte/BOM corruption, raw/LF pin mutation, missing/duplicate/fake-plan-entry/corrupt-input probes against their proper independent projection; PA35 table/bare mutations still fail H26. Add current PA25/runbook/README pin and byte drift rejection, V019 hash/missing-file rejection, extra `V020__unexpected.sql`, extra `V019__unexpected.sql`, and same-count wrong-name V019 replacement rejection within the existing methods. Current table/bare missing/duplicate/fake-entry/hash probes must fail C27 independently; original `tableCount/bareCount == 26` never becomes27.

`EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen` currently excludes V018 from a production manifest318. Exclude exactly V018 **and V019** for that historical318 assertion so its digest `26a10794d635d28bb402a4c88ee27b4c486ac09175b637f90faa122b38114f4a` stays unchanged; additionally freeze the complete current319-file pre-PA40A manifest in the new test. Never change its old expected count/hash to include new files. PA36 doc-prefix checks remain on the unchanged PA36 prefix; PA40A updates are appended before the existing historical marker, not inserted ahead of that prefix. **Documentation-writing constraint:** README/runbook addenda must not add any backticked Java consumer path matching `` `backend/src/main/java/…java` `` before `<!-- PA36 HISTORICAL BODY -->`. `EvidenceRoleIvrAssociationAclBoundaryTests.currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory` is read-only, outside Appendix C's authorized methods; its current-prefix `consumers == 4`, existing PA36 text, security pin and all historical-tail pins stay unchanged. Use prose without such path references; do not move or edit historical tails or authorize that method merely to accommodate addenda.

PA38/39 guards pin current Java298/testJava190, migration18/noV019 and PA25 raw hash `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`. ProductionJava298 stays; testJava becomes192. Preserve an explicit old PA25 hash check against reconstructed frozen prefix and add exact new current hash. Do not allow an arbitrary PA25 change simply because its hash differs. All modified test class/method surfaces are enumerated in Appendix C, including PA33 lifecycle, PA34 Operations subject read, PA35 IVR, PA37 activation reads, PA38 catalog sale and PA39 drafts.

Exact PA38/39 target shapes (assertion-body edits only; **no method renames**):

| Existing method | Required historical and current assertions in that same method |
| --- | --- |
| `OperationsCatalogVersionBoundaryTests.preservesFrozenInputsAndNoV019` | Retain original OpenAPI raw pin and historical PA25 raw pin above. Verify the §5 hash-verified V018 PA25 reconstruction against that old pin (representation-aware raw/LF pair, not arbitrary acceptance); separately hash shipped PA25 against the newly measured/pinned current pair. Preserve an explicit historical exact18/full-basename projection with no V019; derive it by excluding only the authorized V019 from an already validated current set. Independently require live migration count19 and exact basename equality to Appendix A's18 names plus `V019__customer_catalog_offer_read_privileges.sql`; positive current list with exact V019 succeeds. Reject missing V019, extra V020, extra wrong-name V019 and same-count wrong-name V019 replacement. No live `noneMatch(startsWith("V019"))`; that assertion belongs only to the historical18 projection. |
| `OperationsCatalogDraftBoundaryTests.preservesFrozenContractsEighteenMigrationsAndProvisioning` | Retain original OpenAPI raw pin and historical PA25 raw pin; verify the same §5 reconstruction against the old pair and independently pin/check shipped PA25's measured new pair. Preserve historical exact18/full-basename/no-V019 evidence independently of live exact19/full-basename equality to the same18 names plus the exact authorized V019. Positive current19 succeeds; missing V019, extra V020, extra wrong-name V019 and same-count wrong-name V019 replacement all fail. The old18/no-V019 clause remains historical only, never a live predicate. |

These are dual old/new PA25 pins, not a live `isIn(oldHash,newHash)` escape. Reuse the already authorized test-local reconstruction/reflection access pattern; no extra fixture/provider/path or ordinary @Test identity. This reconstruction is specific to these PA25 byte proofs and provisioned-upgrade tests: it never supplies H26's PA25/runbook pairs. Both methods must execute historical and current assertions independently, preserve exact equality and frozen V001–V018/OpenAPI digests, and keep their original names despite their historical wording.

### 5.2.1 Re-review question resolutions (binding Control adjudication)

1. **Cardinality/aggregate:** C27 alone is live, with its own recomputed aggregate/current table27/bare27; H26 retains historical table26/bare26 and `ed03b90f…4409`; F26 retains pre-PA40A `afda2837…c761`. No alternative projection formula is delegated to implementation.
2. **Historical hash source:** H26 uses original FROZEN pairs for all paths except the one known historical README substitution. PA25-prefix reconstruction is not a historical-map hash source; no runbook reconstruction is assumed.
3. **Addendum constraint:** accept the read-only consumer-count4 constraint above. No new backticked Java consumer path before the marker in README/runbook; preserve historical tails and do not edit the guard method.
4. **Fail-closed scope:** reaffirm §3/§4.1/§6.1 and accepted fix1 A-02: V019 rejects extra/wrong-grantor/partial/grantable states within the customer53→75/delta22 plus invariant/preservation envelope. It is not a full432 reconciliation/repair. PA25 + independent IT enforce full nine-role exact410→432; no unrelated-role ACL grant/revoke/repair/broadening. No scope or inventory change.

## 6. One Minimal Atomic Implementation Slice

**S1 — customer catalog read ACL capability plus exact executable proof.** Deliver V019, PA25 exact oracle/history integration, focused PostgreSQL16 proof, historical/current guard maintenance, current README/runbook addendum and CI/POM identities together. Migration without its oracle breaks provisioning; oracle without migration rejects the DB; privilege assertions without actual-login tests do not prove capability. These are one behavior, not three artificial slices. No dependency requires another implementation slice. Historical regression maintenance is part of S1, not independent product scope.

Allowed future implementation paths are exactly Appendix C plus the following eight support/new paths. Current gate authorizes **none of these implementation writes**:

1. NEW `backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql`.
2. NEW `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java` (eight ordinary unit methods, Appendix D).
3. NEW `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOfferReadPrivilegesIT.java` (sixteen ordinary integration methods, Appendix D; test-local helpers only, no third fixture).
4. MODIFY `backend/ops/postgresql/pa25-runtime-identities.sql` (only current oracle/history expectations).
5. MODIFY `backend/README.md` (append PA40A addendum before existing `<!-- PA36 HISTORICAL BODY -->`, preserving PA36 prefix and historical tail).
6. MODIFY `docs/operations/pa25-production-database-identity-provisioning-runbook.md` (same addendum placement; current19/432 commands/preconditions/security limits, no credentials).
7. MODIFY `.github/workflows/ci.yml` (new maps, exact inventory, live PA25/JAR assertions; preserve historical identities/formulas).
8. MODIFY `backend/pom.xml` (one Failsafe include only).

The **44** existing test/fixture paths in Appendix C plus these8 paths = **52 implementation paths: 3 new, 49 modified** (44 existing test/fixture files +5 existing support files). The global audit adds `OperationsSubjectReadBoundaryTests.java`, not an extra fixture/provider file. Twelve existing architecture tests +32 existing container test/fixture paths =44; two new test files make46 test-side implementation paths; one new migration plus five support files make the other6. All52 paths exist currently except the three explicitly NEW paths. Earlier Control/review references to the proposed count are historical dispatch evidence superseded by this audited allowlist, not permission to edit Control. No implementation report/review/Control path is silently authorized. A later Controller dispatch may separately authorize evidence artifacts outside this code allowlist. New unit/IT Java files are test code, not prohibited production Java application changes.

### 6.1 PostgreSQL16 executable proof design

Use existing `CustomerBusinessNumbersFixture.start("018")` for predecessor and `start(null)` for final fresh. Reuse psql/provision and customer web pool only; do not configure any catalog endpoint, cursor key or new bean. Generate temporary passwords inside the container/test memory; never print them or connection/environment values. Real TCP customer LC connects without object privilege, then `SET LOCAL ROLE customer_iam_role` exactly as the shipped pool. Do not prove capability only with a superuser's SET ROLE. Exercise the actual `customerQueryDataSource` initialized by existing customer fixture for clean/restoration proof; no production configuration changes.

Seed synthetic catalog rows using existing owners/fixture patterns with valid parent employee/account/FK facts before fingerprints: product, version, offers and benefits in representative SUBSCRIPTION/ONE_TIME/ADD_ON/QUOTE states, both channels and ACTIVE/DRAFT/OFFSALE/WITHDRAWN lifecycle. Read all22 columns explicitly, including otherwise join-only columns, as the real customer role; run one explicit joined/parent-keyed safe-shape proof with stored values. This is a SQL capability test, not a new production read repository, response mapper, cursor or HTTP route.

Enumerate every actual column of each table from pg_attribute and assert exactly22 allowed/12 denied. For each denied column, execute real SELECT and require SQLSTATE **42501**; also deny SELECT *, whole-row/composite selection and RETURNING paths that could expose excluded columns. Confirm whole-table has_table_privilege(SELECT)=false. Use independent transactions or savepoints so a previous error cannot turn the next proof into25P02. For write tests do **not** set read-only first: deny by ACL42501, not25006. Test INSERT/default-values, UPDATE approved and excluded fields, DELETE, TRUNCATE, REFERENCES/TRIGGER, schema CREATE, table/index/trigger/ALTER/DROP attempts with the expected permission-first SQLSTATE. Use extant synthetic objects and valid SQL/types so syntax, nonexistent-object or check errors cannot masquerade as ACL denial.

For sequence privileges use an existing sequence found in fixture/catalog if one exists; if V001–V019 has none, create a test-only catalog-owner sequence **after** baseline inventory/preservation capture is scoped to the seeded fixture, assert LC nextval/currval/setval denied42501 and remove by container disposal. Do not add a production sequence. For unrelated schemas use known existing protected scalar columns/routines (`payment_order`/`payment_event`, employee private fields, platform idempotency/outbox and audit append routines), not an already approved customer projection. Trigger functions cannot be called as ordinary functions; verify has_function_privilege false and use meaningful permission-denied probes, not unsupported trigger-call errors.

Test GRANT to another test principal and SET ROLE catalog_order_owner/catalog_order_role/operations_iam_role/other runtimes, CREATE ROLE and privileged builtins/pg_read_file with permission-first probes: require42501. Creating a test grantee is superuser fixture setup only and cannot alter the approved application role graph. Inspect no grant options; graph and direct/inherited/effective rights must agree. Public/default/wrong-grantor contaminations are inserted only in isolated fixture transactions, never in shipped SQL, then V019 and PA25 must reject without repair or side effects.

Before/after V019 and each replay/rejection fingerprint **every user business table** (all44, including nonempty `audit.audit_fact`, `platform.outbox_event`/`platform.idempotency_record` and seeded catalog/customer/Operations facts), sorted row JSON/counts; no truncation/reset between snapshots. Snapshot definitions/owners/role attrs/password verifier hashes/comments/settings/default ACLs/membership options/grantors and non-target ACLs. Compare private data only internally; emit pass/fail not contents. Flyway history19 is the sole expected new row, excluded separately rather than dropping all metadata from comparison. Verify expected22 ACL/dependency tuples only, same owners and object/column/index/function/trigger inventories. Empty-table-only invariance is insufficient.

Catalog role/pool proof: capture complete existing catalog_order_role42-tuples, retain its SELECT/INSERT/update columns and approved cross-owner facts; LO keeps exactly seven SET targets, operations role14 and IVR106 unchanged, shared `operationsSubjectReadDataSource` and transaction bean names unchanged. Exercise existing catalog/Operations read/lifecycle regressions and physical-connection role/readOnly/isolation/autoCommit/timeouts restoration after normal/fault paths. A read-only template is not a least-privilege sandbox for catalog/Operations roles; do not broaden LC to achieve those probes.

## 7. Exact Source, Resource, Test And CI Inventories

The following are independently measured source values, not recovered or invented runtime totals. Final column means **future expected after S1**. Existing method identities do not change; only the two new classes add24 ordinary methods.

| Inventory | Current measured | Final expected | Delta |
| --- | ---: | ---: | ---: |
| Production Java files | 298 | 298 | 0 |
| Production resource files | 21 | 22 | +1 V019 |
| Migration resources | 18 | 19 | +1 |
| Production Java + resource manifest | 319 | 320 | +1 |
| Test Java files (including fixtures) | 190 | 192 | +2 |
| Surefire classes / ordinary methods | 104 / 1018 | 105 / 1026 | +1 / +8 |
| Failsafe classes / ordinary methods | 40 / 666 | 41 / 682 | +1 / +16 |
| All ordinary test classes / methods | 144 / 1684 | 146 / 1708 | +2 / +24 |
| Non-test-method Java test/fixture files | 46 | 46 | 0 |
| Annotation-containing source lines | 1685 | 1709 | +24 |
| Historical comment-only annotation line | 1 | 1 | 0 |
| Exact Failsafe POM includes | 40 | 41 | +1 |
| PA25 ordinary test methods | 20 | 20 | 0 |
| Frozen old migration files / hashes | 18 | 18 | 0 |

Current full production319-file LF manifest SHA256 is `57990aa0bc0886c45a58f4e63698d05f860f94a667bc047b2072b6251f3a66fd` (sorted backend-relative `path SHA256(LF bytes)` lines, terminal LF). This is **not** the older historical318-file manifest in PA36/PA39 evidence. Final complete320-file digest and V019 raw/LF/checksum depend on unwritten SQL; compute during implementation, then pin exactly. The unchanged319 projection excluding precisely V019 must equal this digest, so every production Java/resource byte outside V019 is protected. Do not fabricate an unwritten-resource hash.

Current identity digest (sorted FQCN#method, one terminal LF): `6041250383865a75ff49037c1f736bb87db552561206b9990aedaf8604a51770`. Appendix D gives the **recomputed future identity digest** from exactly the planned24 additions, with no renamed old tests.

### 7.1 CI/POM implementation instructions

Add `**/CustomerCatalogOfferReadPrivilegesIT.java` exactly once to `maven-failsafe-plugin` includes in container-smoke; default unit discovery picks up the new boundary Tests. Preserve old40 includes verbatim and require new41 exact set equality against IT source classes.

In `.github/workflows/ci.yml`:

- Surefire XML count near165 becomes1026; retain all named unit checks and exact failures/errors/skips0.
- Add `pa40a_unit_additions` and `pa40a_container_additions` dictionaries with Appendix D's exact FQCN/method sets (8/16), update unit_expected/container_expected by these sets, and include PA40A in the per-class additions union and in every historical-subtraction union. Add `pa40a_start_totals == [1018,666]`, start_line_scan1685, final[1026,682]/1708, final annotation1709, classes105/41/includes41. For every baseline/PA19...PA39 start count and annotation scan, subtract PA40A additions **in addition to** all existing subtractions. Do not change any historical total to absorb24 extra tests. Preserve PA39-start934+630=1564/line1565 and PA36/35/37/38 historical formulas.
- New class assertions and source identities require exact sets, not count-only/containsAll. Extend XML exact_inventory to Surefire1026 and Failsafe682, zero failures/errors/skips, no duplicate/missing XML cases or stale reports. CI source=CI=POM=XML must hold; inherited ordinary-method regex remains `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`. No parameterized/dynamic/repeated tests to distort identities.
- Update current printed inventory and old boundary tests' current CI string pins to105/41,1026/682,1708/1709 without altering historic addition-map names. Keep existing PA36 source-token checks on frozen V018 (`NOT IN (16,18)` etc). Near1968 change only the PA25 **current** expected_resolved410/history18 tokens to432/19; retain graph11 and logins7 markers. Do not change historical V01816/18 or410 oracle tokens in the V018-specific fixture checks.
- JAR section near3143–3146: retain V018 count1, add exact V019 count1, exact packaged migration set equals source19, reject V020/unexpected names/duplicates. Keep PA25 SQL and ops scripts out of the artifact. Preserve credential/firewall/adversarial manifest probes and unchanged Java interface constraints.
- No source application class, dependency, JVM/toolchain or firewall allowlist expansion. Exact final production source/resource count320 is packaging input, not a claim about JAR entry/class totals (compilation creates variable nested entries).

## 8. Documentation Decision And Alignment

Do not edit frozen OpenAPI/docs12/14/15/17 or any historical gate/review artifact. README and PA25 runbook get a scoped PA40A addendum **after the existing PA36 current prefix but before its historical-body marker**; explicitly identify PA40A19/432 as the latest numeric provisioning boundary, with existing seven-login/eleven-edge topology unchanged. Retain the old PA36 prefix as a labelled older boundary so its prefix-only guards remain meaningful, not contradictory operational instructions. The addendum states role-wide scalar read amplification, no RLS/endpoint authority, four required external-name inputs, migration-before-provisioning ordering and NOT EXECUTED production acceptance. No production secret or cursor configuration.

Alignment: matrix22 closes only the confirmed customer capability gap; target-only migration preserves unrelated owner domains; PA25432 makes actual privileges deployable without repair; real-login42501 and byte/history guards prove least privilege/upgrade safety; CI/POM/XML pins make proof discoverable; README/runbook prevent operators using stale18/410. No extra architecture layer, endpoint design, generic ACL engine or extra slices are warranted. Controller-owned Control is read-only. **No overdesign or goal drift is justified.**

## 9. Validation Commands And Future Assertions

These are future implementation-stage commands, **not results of this planning run**. Use Java21/Maven3.9.9/PostgreSQL16/Docker per existing CI, dependencies already provisioned. Do not install packages or normalize this worktree at this gate. If managed CRLF materialization prevents inherited PA35 byte tests, Controller supplies a separately authorized LF validation projection; preserve primary frozen bytes and record both representation/projection provenance. Do not change expected canonical hashes to hide an environment failure.

From `backend` (commands write target only in a later authorized implementation/validation workspace):

```sh
mvn -B -ntp -Dtest=CustomerCatalogOfferReadAclBoundaryTests test
mvn -B -ntp -Pcontainer-smoke -DskipTests test-compile
mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerCatalogOfferReadPrivilegesIT failsafe:integration-test failsafe:verify
mvn -B -ntp -Dtest=CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectClaimBoundaryTests,CustomerSubjectActivationBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsActivationReadBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogDraftBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests test
mvn -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT,DatabaseRuntimeAclRbacHardeningIT,ProductionDatabaseIdentityProvisioningIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,CustomerEvidenceReadPrivilegeIT,EvidenceRoleIvrAssociationAclIT failsafe:integration-test failsafe:verify
mvn -B -ntp -Pcontainer-smoke -Dit.test=CustomerMeIT,CustomerSubjectsIT,CustomerBusinessNumbersIT,CustomerEvidenceIT,CustomerSubjectClaimsIT,CustomerSubjectActivationsIT,SubjectClaimRoleLeastPrivilegeIT,ActivationCommandRoleLeastPrivilegeIT,OperationsSubjectReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT,OperationsActivationReadsIT,OperationsActivationReadRoleLeastPrivilegeIT,OperationsIvrSessionReadsIT,OperationsCatalogProductReadsIT,OperationsCatalogProductReadRoleLeastPrivilegeIT,OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT,OperationsCatalogDraftLifecycleIT,OperationsCatalogDraftRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
mvn -B -ntp clean verify -Pcontainer-smoke
```

Before running a selector verify exact class basenames from POM/source; absent selector is a validation failure, not a silent success. Focused expected new tests8 unit +16 IT=24, all0 failure/error/skip. Existing regression methods keep their identities and expected semantics; no unmeasured focused runtime total is claimed. The clean full future expectation is **1026 Surefire +682 Failsafe=1708**, source annotations1709 with the one known comment, exact classes105+41 and POM41. Run the CI inventory Python heredoc unchanged from the updated workflow, not a new count-only substitute; XML must match the complete exact source identity sets/digest in Appendix D.

PA25 apply/check from repository root, only against disposable fixtures or separately authorized target DB using protected psql connection settings (no password in command):

```sh
psql -X -w -v ON_ERROR_STOP=1 -v mode=apply -v sharing_login="$SHARING_LOGIN" -v customer_login="$CUSTOMER_LOGIN" -v command_login="$COMMAND_LOGIN" -v operations_login="$OPERATIONS_LOGIN" -f backend/ops/postgresql/pa25-runtime-identities.sql
psql -X -w -v ON_ERROR_STOP=1 -v mode=check -v sharing_login="$SHARING_LOGIN" -v customer_login="$CUSTOMER_LOGIN" -v command_login="$COMMAND_LOGIN" -v operations_login="$OPERATIONS_LOGIN" -f backend/ops/postgresql/pa25-runtime-identities.sql
```

Require exact current success markers `PA25_APPLY_OK logins=7 effective_roles=9 memberships=11` and CHECK equivalent; exact history19/ACL432; twice-run apply/check no verifier/OID/marker/edge/grant changes. Negative target18/history mutation/missing tuple/unexpected grant states reject atomically with sanitized categories and no provisioning “repair”. Explicitly test fresh0-slot, old3-slot and existing4-slot upgrade paths and compare final named graph. Separate real-login and existing pool tests prove role/isolation/readOnly/timeout cleanup; never infer credential readiness from successful local provisioning.

After clean verify run the workflow's existing default/opt-in web JAR smokes and packaged-class dependency-firewall/adversarial probes, with new migration-set checks. Inspect JAR contains V001–V019 once each, no V020, no PA25 ops SQL, no test adapters/verifiers. Run unchanged contract scripts where tools are already available:

```sh
(cd api/openapi && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s2 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures
```

These exact contract commands are taken from the inspected workflow/package scripts (A1 s3, A2 s3, A3 s3, A4 s2 and A5 s2); generated bundle outputs belong only in a later authorized validation projection, never this planning worktree. Missing tools/Docker/daemon/network are **NOT EXECUTED/BLOCKED**, never pass or permission to install at this gate. Capture actual executed counts/XML/JAR hashes in the later authorized evidence artifact; repeat focused fresh/upgrade/replay after final SQL bytes and checksum pins settle. No production acceptance result is implied.

## 10. Independent Plan Review And Residuals

Peer reviewer independently derives the22-column matrix (especially exclusion of version interval/snapshot/hash/timestamps/benefit surrogate), verifies432=410+22 and old53→75, checks historical410 literal is not overwritten, enumerates all affected PA33–PA39 guards, reproduces current/future identity counts/digests, and challenges the seven-versus-six discrepancy using source. Review must verify the upgrade path with already provisioned four slots is accepted at predecessor18; blindly inheriting V018's self-history provenance condition would break deployment. Review the old-current PA25 fixture reconstruction byte pin and both PA35 verifier overlays; preserving historical plans must not disable current verification.

| Residual / destination | Owner | Treatment |
| --- | --- | --- |
| Actual-login row visibility and customer authority are not ACL predicates; catalog approved fields readable role-wide | Backend/Security, future PA40 | Explicit accepted boundary; implement endpoint later under fresh scope, not here |
| Version snapshot/publication correctness is owner-trusted; no cryptographic proof against hostile DB owner | Catalog owner/Database/Security, existing publication controls | Preserve existing constraints/triggers; do not grant snapshots |
| “Six” versus actual seven current logins | Controller, independent plan review | Source correction required in Controller-owned wording; no role deletion/new grant authorized |
| OID-bound markers and cooperative advisory lock do not defeat privileged concurrent admin | Database/Platform, release procedure | Exclusive migration/provisioning administration; no generic locking framework |
| V019 checksum, raw/LF hashes and current320 manifest digest not knowable before SQL finalization | Implementer + independent reviewer | Compute then pin before runtime/full/JAR acceptance; never placeholders at implementation completion |
| TLS/pg_hba, production credentials/rotation/session invalidation/pool drain, cross-database shared dependencies, secret custody | Platform/Security/Operations, existing PA25 runbook Q17/Q23 | External production prerequisites, NOT EXECUTED; no secrets generated/published here |
| Windows Git/CRLF and Docker/tool availability | Controller/validation owner | Read-only verified Git override; authorized validation projection; report blocked validation truthfully |

Blocking technical open question: **none**, under unchanged-current-graph interpretation in §1.1. The numeric wording discrepancy is explicitly escalated for review/Controller reconciliation, not silently implemented as six. If that interpretation is rejected, renewed scope confirmation is required before implementation. Gate completion is a plan, not authorization to merge, approve or implement. Exact next gate: **independent peer-isolated plan review**.

## Appendix A — Frozen Migration Checksums And Byte Pins

Measured current raw and LF SHA256. No old file may be rewritten or line-ending-normalized in the primary worktree. Future tests append V019 pins separately; old expected arrays remain intact.

| Resource | Signed Flyway CRC32 | Raw SHA256 | LF SHA256 |
| --- | ---: | --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | -709123726 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `V002__identity_organization_and_employee.sql` | -1229975166 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `V003__subject_number_and_claim.sql` | -1355776370 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `V004__catalog_order_and_payment.sql` | -665258487 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V005__ivr_task_session_call.sql` | -1005314773 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | 366834714 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `V007__activation_and_consent.sql` | -1281725572 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `V008__evidence_media.sql` | -245169007 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `V009__sharing.sql` | -59961153 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `V010__sharing_role_least_privilege.sql` | 247431460 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `V011__sharing_public_read_least_privilege.sql` | 492686454 | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `V012__sharing_admission_least_privilege.sql` | -291628548 | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `V013__evidence_media_reader_role.sql` | -611381791 | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |
| `V014__database_runtime_acl_rbac_hardening.sql` | -340359494 | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |
| `V015__customer_business_number_read_privileges.sql` | -1316980027 | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` | `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3` |
| `V016__customer_subject_detail_read_privileges.sql` | 1669606497 | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` | `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` |
| `V017__customer_evidence_read_privileges.sql` | -41822162 | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` | `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` |
| `V018__evidence_role_ivr_association_read_privileges.sql` | 1606293398 | `ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c` | `892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817` |

## Appendix B — Literal Customer Baseline And Delta Identity

Tuple format `grantee|kind|object|column|privilege|grantor|is_grantable`. These53 current tuples are source-extracted, reviewed expected constants; test/migration expected tuples must not be learned from the runtime DB. Copy them as a literal baseline and append only the22 literal delta lines. PA25 JSON represents the same identities with either six fields (implicit false) or seven fields (explicit false); canonical comparison is seven fields in both cases.

### B.1 Frozen V018 customer53

```text
customer_iam_role|column|activation_consent.service_activation|status|SELECT|activation_consent_owner|false
customer_iam_role|column|activation_consent.service_activation|subject_id|SELECT|activation_consent_owner|false
customer_iam_role|column|catalog_order.customer_order|account_id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.customer_order|owner_organization_id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.customer_order|subject_id|SELECT|catalog_order_owner|false
customer_iam_role|column|customer_iam.customer_account|display_name|SELECT|customer_iam_owner|false
customer_iam_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false
customer_iam_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false
customer_iam_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|consent_version|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|created_at|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|solidified_at|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false
customer_iam_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|status|SELECT|evidence_owner|false
customer_iam_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false
customer_iam_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false
customer_iam_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false
customer_iam_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false
customer_iam_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false
customer_iam_role|column|organization_access.organization|display_name|SELECT|organization_access_owner|false
customer_iam_role|column|organization_access.organization|id|SELECT|organization_access_owner|false
customer_iam_role|column|organization_access.organization|status|SELECT|organization_access_owner|false
customer_iam_role|column|subject_number.business_subject|created_at|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.business_subject|real_name_status|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.caller_number_binding|number_masked|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.caller_number_binding|number_type|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false
customer_iam_role|schema|activation_consent||USAGE|activation_consent_owner|false
customer_iam_role|schema|catalog_order||USAGE|catalog_order_owner|false
customer_iam_role|schema|customer_iam||USAGE|customer_iam_owner|false
customer_iam_role|schema|evidence||USAGE|evidence_owner|false
customer_iam_role|schema|organization_access||USAGE|organization_access_owner|false
customer_iam_role|schema|subject_number||USAGE|subject_number_owner|false
```

### B.2 Exact V019 delta22

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

## Appendix C — Exhaustive Existing Test/Fixture Write Allowlist

All44 paths below are MODIFY, not new. Existing @Test method names remain unchanged. The method column explicitly authorizes assertion-body changes at the named current test/fixture/guard entry points, plus the named `APPROVED` fields and §3.1/§5.3 non-@Test composition helpers. Unlisted ordinary methods remain regression coverage with unchanged identities and assertions; a changed fixture alone is not authorization to edit them. Historical-only array sizes (for example benefit/index18 or evidence_role18) do not advance. Helper changes apply transitively to existing callers without adding test identities; §5.4 enumerates the complete null-start graph rather than relying on this general rule to establish coverage.

| Existing path | Exact affected method/guard entry points |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java` | `v015AndPa25DeltasAreReadOnlyAndFrozenInputsRemainByteIdentical` |
| `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java` | `v017AddsOnlyNineteenColumnsAndPa25Uses288AndSeventeen`, `openApiAndV001ThroughV016RemainFrozenWithoutContractExpansion` |
| `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java` | `openApiDataDocsAndAllSeventeenMigrationsRemainFrozen` |
| `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java` | `openApiMigrationsAndProductionProviderAbsenceRemainFrozen` |
| `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java` | `v016AddsOnlyTheEightRequiredPrivilegeTuplesAndPa25Uses269AndSixteen` |
| `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | `v001ThroughV017AndPublicContractsRemainByteFrozen`, `pa25UsesExactEighteenHistoryAndIndependent410TupleOracle`, `pa36HasNoApplicationApiPoolOrDependencyExpansion`, `ciPomAndOrdinaryMethodInventoryMatchExactly` |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java` | `frozenInputsAndSeventeenMigrationInventoryRemainByteIdentical` |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java` | `scansExactPlannedProductionAndTestFiles`, `preservesFrozenContractsEighteenMigrationsAndProvisioning` (exact §5.2 historical18/current19 full-basename and dual PA25 pin shape; no rename) |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java` | `scansExactProductionFiles`, `preservesFrozenInputsAndNoV019` (exact §5.2 historical18/current19 full-basename and dual PA25 pin shape; no rename) |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` | `frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact` (§5.2 unchanged literal26/parser26 plus independent live27 PA25 SQL/runbook/README substitutions and V019; preserve old aggregates/probes) |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java` | `frozenOpenApiMigrationsAndPolicyBytesAreUnchanged` |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsSubjectReadBoundaryTests.java` | `v001ThroughV017AndFrozenOpenApiAndDesignDocsRemainUnchanged` |
| `backend/src/test/java/com/onecall/archtests/container/ActivationCommandRoleLeastPrivilegeIT.java` | `fixtureCleanupAndFrozenAclInventoryRemainExact` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java` | `APPROVED`, `latest`, `v015FreshAndUpgradeHaveExactFifteenHistoryEntriesAndFrozenBytes`, `customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501`, `pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle`, `allSixLoginBoundariesAndRolePoolRestorationRemainUnchanged` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java` | `start` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java` | `postgres16FullChainAndCustomerRuntimeRoleAreUsed` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceIT.java` | `postgres16LatestChainAndVerifiedCustomerRoutesAreWired` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java` | `APPROVED`, `latest`, `latestOracle`, `customerRoleReadsOnlyApprovedFactsAndDeniesAllSensitiveCapabilitiesWith42501`, `pa25RequiresExactV017HistoryAndIndependent288OracleWithoutRepair`, `sixLoginsSevenEffectiveRolesAndFourMembershipEdgesRemainExact`, `realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java` | `start` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java` | `postgres16FrozenSeventeenMigrationChainAndDedicatedLoginAreUsed` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java` | `APPROVED`, `latest`, `currentOracle`, `currentHistory`, `customerRoleReadsOnlyApprovedColumnsAndDeniesAllOtherCapabilitiesWith42501`, `pa25RequiresExactV016HistoryAndIndependent269TupleOracleWithoutRepair`, `allSixLoginsAndSevenEffectiveRolesRetainExactMembershipBoundaries`, `realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java` | `postgres16LatestChainAndCustomerRuntimeRoleAreUsed` |
| `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java` | `v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle`, `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects`, `assertFrozen` |
| `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java` | `Fixture.start` |
| `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java` | `roleAttributesPasswordTopologyAndV013HistoryAreExact` |
| `backend/src/test/java/com/onecall/archtests/container/EvidenceRoleIvrAssociationAclIT.java` | `provision`, `freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors`, `evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn`, `v018ReplaysOnlyExactCleanOrCompleteStatesWithoutDrift`, `pa25RequiresV018AndExact410OracleInBothModes`, `pa25UpgradesThreeSlotsToFourAndPreservesExistingCredentialBytes`, `pa25RejectsInvalidOperationsInputAndCollidingProvenance`, `pa25ApplyAndCheckEnforceExactElevenEdgeTopology`, `operationsLoginSetsExactlySevenRolesAndRestoresSessionContext`, `allOtherLoginAndDirectRoleBoundariesRemainExact`, `pa25FailuresAreAtomicSanitizedAndNeverAdoptOrRestamp` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java` | `frozenAclRolesFunctionsAndAllFactsHaveZeroDelta` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java` | `postgres16UsesExactlyFrozenSeventeenMigrations` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftLifecycleIT.java` | `postgres16RunsExactlyV001ThroughV018` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftRoleLeastPrivilegeIT.java` | `laterMigrationsAndProvisioningRetainExactEffectiveAclOracle` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadRoleLeastPrivilegeIT.java` | `assertsNoNewGrant` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java` | `v001ThroughV018ProvideAllRequiredObjects`, `noV019IsNeededOrPresent` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` | `postgres16UsesExactlyFrozenEighteenMigrations` (§5.2 unchanged literal26/parser26 plus independent live27 PA25 SQL/runbook/README substitutions and V019; preserve old aggregates/probes) |
| `backend/src/test/java/com/onecall/archtests/container/OperationsLifecycleRoleLeastPrivilegeIT.java` | `frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java` | `frozenAclOracleMigrationsRolesAndFunctionsRemainUnchanged` |
| `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadsIT.java` | `postgres16AppliesExactlyFrozenSeventeenMigrations` |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`, `assertV009LiveFence` |
| `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java` | `start`, `invalidInputsHistoryAndAuthorityRejectAtomically`, `frozenMigrationsLogicalAclsAndHistoryRemainUnchanged`, `assertExactFrozenAcls` |
| `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | `publicGetAndCreatorCreateRevokeRotateListMeRemainUnchanged` |
| `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java` | `v012CreatesExactAdmissionRoleAndIsReplaySafe` |
| `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java` | `start` |
| `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java` | `queryRuntimeStartsAfterFixtureProvisioning` |
| `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java` | `start`, `v010MigrationIdentityAndPostgres16` |
| `backend/src/test/java/com/onecall/archtests/container/SubjectClaimRoleLeastPrivilegeIT.java` | `frozenAclsHistoryAndCustomerQueryPrincipalRemainUnchanged` |

Read-only affected historical/frozen surfaces (not additional writes): docs12/14/15/17, OpenAPI, old migration resources, PA35/PA36/PA38/PA39 plans and review evidence, `OperationsCatalogDraftFixture.provisioningOracle()` caller, and existing runtime pool classes. Global numeric matches for HTTP410, commercial quantities, registry/index cardinality18 and historical inventories are **not** migration counters and must not be changed. Appendices and §5 identify transitive verifier/README hash pins rather than allowing broad repinning.

## Appendix D — Exact New Method Identities And Final Digest

### Unit: `com.onecall.archtests.architecture.CustomerCatalogOfferReadAclBoundaryTests` (8)

```text
v019GrantsExactlyTwentyTwoOwnerIssuedColumns
v019RejectsDriftAndAcceptsOnlyExactReplayStates
frozenContractsAndV001ThroughV018RemainByteIdentical
pa25PinsNineteenHistoryAndIndependent432TupleOracle
existingSevenLoginElevenEdgeGraphRemainsUnchanged
currentDocsPreserveHistoricalBoundaries
migrationOnlyAddsOnePackagedResource
sourceCiPomAndXmlIdentitiesMatchExactly
```

### Integration: `com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT` (16)

```text
v018CustomerLoginDeniesAllTwentyTwoCatalogColumns
freshAndUpgradeReachExactlyNineteenSuccessfulMigrations
v019AddsOnlyTwentyTwoOwnerIssuedTuplesAndMatches432Oracle
customerLoginReadsEveryApprovedColumnAndExactStoredProjection
customerLoginDeniesEveryAdjacentColumnWith42501
customerLoginDeniesWritesDdlSequencesRoutinesAndUnrelatedObjects
customerLoginCannotEscalateOrGrantPrivileges
v019ReplaysCleanAndCompleteStatesWithoutFurtherChanges
v019RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically
v019RejectsAuthorityHistoryProvenanceAndTopologyDrift
v019RejectsOwnerSettingsPublicDefaultsAndDependencyDrift
pa25ApplyAndCheckRequireNineteenHistoryAndExact432Oracle
allSevenLoginsElevenMembershipEdgesAndCredentialsRemainUnchanged
customerPoolRestoresRoleTransactionAndSettingsOnSuccessAndFailure
catalogRoleAndOperationsIdentityRetainExactCapabilitiesAndPool
migrationReplayAndRejectedStatesPreserveAllBusinessAndInfrastructureRows
```

Each IT method is an ordinary @Test; loops may cover negative matrices without changing method identity. A method name is not a substitute for implementing its assertions in §6.1. Final identity digest from current1684 plus exactly these24: **`fe582055c9a28067aa156231f5aabfece21f91510a909a56a4c189c2ba4ca55c`**. Current digest independently recomputed **`6041250383865a75ff49037c1f736bb87db552561206b9990aedaf8604a51770`**. Sorted LF FQCN#method records with terminal LF, duplicate identity rejection. New set24, final1708, annotation1709, source=CI=XML exact identity comparison; no claimed runtime XML exists at this planning gate.

## Appendix E — Planning Validation Actually Performed

The following original planning-run evidence is retained as historical provenance; it is not a claim that Git or runtime checks succeeded in the bounded fix workspace.

- Read-only file/SQL/contract/fixture/CI/POM inspection, global hidden-path-aware `rg` count/history/ACL/freeze scans and migration-grant/guard-array parsing; no Maven, psql, Docker, application, contracts or JAR tests executed.
- Verified per-worktree Git override resolves HEAD2398a4d71eee2973b684f6a404d3c65766bf7230; read-only `git show` inspected only external blocked PA40 dependency. Initial status showed only pre-existing `.codex`, `.pa40a-plan-prompt.md`, `.pa40a-plan-run.sh` untracked; never edited them or Git metadata.
- Parsed current PA25 JSON:410 unique tuples, customer53 and exact per-role totals; source-scanned104/1018+40/666, annotation1685; XML-parsed POM40 distinct includes; counted productionJava298/resources21/testJava190. Recomputed identity digests, all18 migration raw/LF SHA256 and signed line-based CRC32; retained all original pins.
- Verified future command selectors against source and contract command paths/options against CI and package.json; no dependency installation or generated output.
- Final planning-only checks: sole artifact diff, frozen inputs checked against initial raw pins and representation-aware HEAD bytes, artifact line/byte count and SHA256. A direct raw checkout-versus-HEAD comparison first found the expected V001 CRLF-versus-LF representation difference; no file was normalized or rewritten. Canonical-LF comparison and the independently recorded raw pins distinguish representation from content drift. The delivery message records the resulting hash rather than attempting a self-referential hash inside this file.

### E.1 Bounded plan-fix1 validation

- Read Controller's current accepted adjudication and both timestamped plan reviews; source-audited all affected assertion/helper entries, migration/PA25 literals, relevant direct/composed fixtures and current CI/POM/README/runbook pins. No implementation or dependency installation.
- Recomputed44 existing allowlist paths (12 architecture/32 container),49 existing implementation paths plus3 new=52, the11 null-start sites/11 paths and34-path composed graph, all Appendix C method/field identities and the inventory values/digests in §7/Appendix D. Failsafe-only POM includes40 (not42 across all plugins); annotation scan uses `@Test\b`1685 (not substring matches including @TestConfiguration).
- Read all18 migration resources and PA25 arrays; verified historical customer53 and delta22, PA25410/history18, full production319 LF manifest and both ordinary-method identity digests. Exact V019 bytes/CRC/full320 manifest remain future measurements, not invented values.
- The default `git status` command failed because this checkout's worktree pointer names an unavailable Windows Git path. No Git metadata was modified; fix validation instead uses before/after byte-hash and path-set comparison of repository files, excluding Git/helper/generated directories, and an exact two-document write allowlist. No Maven, Docker, psql, CI, JAR or runtime test was run at this plan-only gate. Final line/byte/SHA256 measurements are reported externally to avoid self-reference.

### E.2 Bounded plan-fix2 validation

- Read Control's accepted re-review adjudication, cumulative plan/fix1, all four binding reviews, both exact frozen verifier bodies, relevant PA35 table/bare/aggregate rules, PA38/39 guard bodies and the read-only PA36 current-prefix consumer-count guard. Closed `PA40A-PLAN-RR-B-01` and four nonblocking questions in the proposed plan; final bounded A/B plan re-review remains required.
- Independently reparse each original literal26 and PA35 table/bare26, verify exact historical README-only substitution, recompute both historical/pre-PA40A aggregate formulas and confirm no V019 historical row. Current27 digests are future implementation measurements; no current hash or runtime result is fabricated.
- Byte-hash/path-set comparison against the pre-edit workspace snapshot is the scope check because the inherited Windows Git pointer is unavailable. Only this cumulative plan and new fix2 artifact may differ; fix1, Control, all four reviews, source/tests, PA35 plan and every other captured file remain unchanged. No Git metadata/helper/generated path writes, dependency installation, implementation or runtime execution.
- Appendix C's same44 paths/67 existing @Test entry points/16 existing non-@Test entries/3 fields and Appendix D's same24 new identities remain binding. Implementation inventory stays52=3 new+49 modified, classes146/methods1708/annotation1709; no deterministic correction is needed. Final document hashes are reported outside the artifacts to avoid self-reference.
