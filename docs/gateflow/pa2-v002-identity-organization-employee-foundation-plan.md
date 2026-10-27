# PA2 V002 Identity, Organization and Employee Foundation - Implementation Plan

- Task label: `pa2-v002-identity-organization-employee-foundation-plan`
- Work unit: `PA2-v002-identity-organization-employee-foundation`
- Gate: `plan`
- Author/provider: planning Agent selected by the controller (`ds` gate-selected provider)
- Date: 2026-10-27
- Base: `main` at `d93c380b9f0025675931f950d95ca4cfef45647b` (merged PR #9), tree `b475570bc4452b3d802cc2f919cb2a4f5fc7a033`
- Binding inputs: `docs/gateflow/project-execution-control.md` PA2 binding goal contract, slice principle and gate record; `docs/12-technology-stack-and-system-architecture.md` sections 5, 6, 14, 15 Phase A and 18 item 1; `docs/14-production-data-model-and-flyway-schema-spec.md` sections 2.1, 2.4, 2.7, 3.1-3.3, 4.6-4.11, 5.3, 6.2, 9.4, 9.5, 9.9, 9.10, 10.8, 10.8.1, 10.10, 11.3, 12.1-12.4, 13.5, 15.1-15.2; `backend/pom.xml`; `backend/README.md`; the accepted V001 migration and PostgreSQL evidence suite; `.github/workflows/ci.yml`.
- Status: plan artifact only. This gate does not implement code, migration, tests, CI or README changes, does not modify the control document, and does not commit, push, open a PR, merge or enter another gate.
- Plan-fix 1 (2026-10-27): resolves controller-accepted review finding F-PA2-PR-01 by requiring exact PostgreSQL 16-normalized `pg_get_constraintdef` inventory evidence for all 34 V002 named checks (section 8.2.1), while retaining the S2 positive/negative behavior probes. Artifact: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan-fix-1.md`. This remains a plan artifact only.

## 1. Goal, motivation, success signals and scope

### 1.1 Goal

Implement exactly `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` so later subject, IVR, order and evidence waves have stable customer-account, authentication-identity, account-real-name, organization-membership and operations-employee parents. The work unit must not implement application authentication or later domain behavior.

The deliverable is one frozen migration plus focused additions to the existing PostgreSQL 16 Testcontainers contract suite and the minimum CI/README/evidence updates required to run and explain that contract.

### 1.2 Motivation

Every later S1 table that carries ownership or actor identity depends on V002 parents:

- `subject_number.business_subject.owner_account_id` and `subject_number.subject_claim.account_id` reference `customer_iam.customer_account`.
- `subject_number.business_subject.owner_organization_id`, `subject_number.subject_claim.organization_id`, IVR task ownership and order ownership reference `organization_access.organization`.
- Catalog/trial-policy publication and operations lifecycle facts reference `operations_iam.employee_account`.
- Customer login identity, account-level real-name and organization membership need their frozen tables before any later flow can be designed without forward references.

Implementing only the frozen V002 persistence contract is the smallest useful next wave. It avoids coupling schema delivery to still-deferred customer/employee authentication, session storage, operations services, Q2 provider-subject rotation and Q17/Q23 production provisioning.

### 1.3 Success signals (measurable)

| Signal | Required evidence |
| --- | --- |
| Flyway applies V001 then V002 to a clean PostgreSQL 16 database | Fresh Testcontainers `postgres:16`; `assertCleanBeforeMigration`; `MigrateResult.migrationsExecuted = 2`; history rows `001` then `002` with the exact scripts/descriptions and `success = true`; `validate()` passes; repeated `migrate()` executes zero; no pending migration. |
| Exactly the six frozen tables exist with every named check, key, index and four same-wave immediate FKs, and every check predicate is proven exact | Exact PostgreSQL catalog assertions over `pg_class`, `pg_namespace`, `pg_attribute`, `pg_attrdef`, `pg_constraint`, `pg_index`/`pg_indexes`, `pg_proc` and `pg_trigger`; exactly the 34 V002 check names **and** exact PostgreSQL 16-normalized `pg_get_constraintdef` equality for all 34 check predicates against an explicit expected mapping derived from `docs/14` 4.6-4.11 (whole-definition equality, never substring/contains); table list exactly the nine V001+V002 tables; no V003+ table/schema/function/trigger; no `subject_number` table. |
| Six local immutability guards and exact narrow owned/cross-module grants exist | Six `BEFORE UPDATE` `SECURITY INVOKER` trigger functions with exact owners and ordered `search_path`; six enabled guards; exact table/column `SELECT`/`INSERT`/`UPDATE` grants; exact schema `USAGE`; no `DELETE`/`TRUNCATE` to any runtime role; no cross-module write or row-lock privilege; `PUBLIC` denied on every schema/table/function. |
| Fail-closed PostgreSQL 16 evidence covers inventory, lifecycle/revision, immutable relations, identity collision, membership revoke/regrant, least privilege and last-enabled-admin transaction protection | Executable tests listed in section 8; S1 inventory asserts the 34-check normalized predicate mapping (section 8.2.1); every expected constraint failure asserts the exact SQLSTATE and constraint/error name; S2 positive/negative shape probes complement and do not replace that S1 predicate inventory; concurrency scenarios use bounded blocking detection, not sleeps; a failed assertion fails the build; there is no skip path. |
| V001 remains byte-for-byte unchanged at SHA-256 `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` | `git show HEAD:backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql \| sha256sum` equals the frozen hash; `git diff --ignore-cr-at-eol --exit-code HEAD -- <V001>` exits `0`; V001 is never edited. |
| Default and container-profile verification pass | Default `./mvnw -B -ntp clean verify` runs 10 surefire tests, no Docker, no DataSource/Flyway bean; `-Pcontainer-smoke verify` runs exactly the final 18 named container methods with `skipped=0 failures=0 errors=0`; boot smoke and fat-jar firewall pass. |

### 1.4 Scope boundary

In scope:

- Exactly one new migration: `V002__identity_organization_and_employee.sql`.
- Exactly six tables: `customer_iam.customer_account`, `customer_iam.auth_identity`, `customer_iam.identity_profile`, `organization_access.organization`, `organization_access.organization_membership`, `operations_iam.employee_account`.
- Their named checks, primary keys, unique constraints, indexes, partial indexes and four same-wave immediate foreign keys.
- Six local `BEFORE UPDATE` guard trigger functions and triggers.
- Exact schema/table/column/function grants and `PUBLIC` denial from `docs/14` sections 2.1, 10.8 and 10.8.1.
- Focused additions to `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`.
- Minimum `.github/workflows/ci.yml`, `backend/README.md` and per-slice evidence updates.
- This plan artifact.

### 1.5 Non-goals

- No V003-V009 migration or object; no `subject_number`, `catalog_order`, `ivr`, `entitlement_metering`, `activation_consent`, `evidence` or `sharing` table, role, schema or behavior.
- No subject, phone-number, claim, risk, activation, consent, IVR, trial, catalog, order, payment, evidence or sharing behavior.
- No API, login/session/credential/OTP implementation, frontend, runtime DataSource/Flyway wiring, cloud resource, production provisioning or domain workflow.
- No JPA aggregates, repositories, service endpoints, database service functions, generic trigger frameworks, credential/session tables, speculative key rotation or later-wave compatibility objects.
- No change to V001, `docs/12`, `docs/14`, the control document, the PA1 evidence, `backend/pom.xml`, application source, the broken V901 fixture, contract-regression jobs or module boundaries.
- No guess of Q2 provider-subject rotation behavior or Q17/Q23 production role/provisioning inputs.
- No production acceptance claim; local/CI success is build/static/container evidence only.

### 1.6 Scope interpretation

- Customer authentication and employee authentication stay separate: `customer_iam.auth_identity` supports only `WECHAT`/`PHONE`; `operations_iam.employee_account` has its own `auth_issuer`/`auth_subject_hmac` and no FK to any customer table.
- Organization membership is authorization after customer login, never a login identity: it has no provider, credential, OTP, session or HMAC column and no FK to `auth_identity`.
- Account-level real-name is separate from subject-level real-name: `customer_iam.identity_profile` is keyed only to `customer_account`; no subject table or subject-owned column exists in V002.
- The migration can enforce constraints, indexes, FK actions, guard triggers and ACLs. Transaction protocols owned by later application services are demonstrated only by executable test scenarios; no service, SQL function or trigger beyond the frozen six guards is added.

## 2. Goal alignment mapping

| Design decision / slice / validation | Binding source | Confirmed goal or success signal |
| --- | --- | --- |
| V002 contains only the six named tables | `docs/14` 4.6-4.11, 11.3 | Exact six frozen tables; no V003+ |
| Creation order customer_account -> auth_identity -> identity_profile, then organization -> membership, then employee, then FKs | `docs/14` 11.3 steps 1-4 | Flyway V001+V002 prefix and stable parents |
| Four same-wave immediate FKs | `docs/14` 5.3 | Four frozen FKs |
| Named checks, PKs, unique constraints, indexes and partial indexes | `docs/14` 4.6-4.11, 5.3 | Every named check/key/index; all 34 check predicates proven by exact normalized `pg_get_constraintdef` inventory |
| Six `BEFORE UPDATE` guard functions/triggers | `docs/14` 10.8.1, 11.3 step 5 | Six local immutability guards |
| Owned-table SELECT/INSERT and column UPDATE grants | `docs/14` 10.8 owned-table table | Exact narrow owned-table grants |
| Cross-module SELECT columns and schema USAGE | `docs/14` 2.1, 10.8, 11.3 step 6 | Exact narrow cross-module grants |
| `PUBLIC` denial, no runtime DELETE/TRUNCATE, no cross-module write/row lock | `docs/14` 2.1, 10.8, 12.2 | Least privilege |
| Lifecycle defaults, status domains, revision and time order | `docs/14` 2.4, 4.6-4.11, 9.10 | Lifecycle/revision evidence |
| Identity unique tuple and immutable relation guard | `docs/14` 4.7, 9.5, INV-02 | Identity collision semantics |
| Membership partial unique and immutable relation guard | `docs/14` 4.10, 9.10, INV-03 | Revoke-and-regrant semantics |
| Last-enabled-admin partial index and transaction protocol | `docs/14` 4.11, 9.9, INV-04 | Last-enabled-admin transaction protection |
| V001 untouched and hash-checked | Control PA2 contract | V001 byte-for-byte unchanged |
| Renamed migration/inventory tests plus focused additions | `docs/12` 14, PA1 evidence conventions | Default and container verification |
| S1 migration/catalog, S2 row semantics, S3 authorization/transaction protocol | PA2 slice principle | Three behaviorally distinct, runnable slices |
| Test-only collision/last-admin protocols | `docs/14` 9.5, 9.9, 9.10 | Evidence without application services |
| README/CI/evidence updates only | Control PA2 scope boundary | Minimum explain/run updates |

## 3. Design-document alignment and direct code evidence

| Source | Binding requirement | Direct code/repository evidence | Plan response |
| --- | --- | --- | --- |
| `docs/12` 5 (206-220), 6 (222-246) | One Java codebase; module ownership; no module writes another module's tables | 14 Modulith packages; `ModuleBoundaryTests`; no domain persistence code | No production Java change; V002 is schema-only |
| `docs/12` 14 (434-458) | Testcontainers PostgreSQL/Flyway migration and concurrency evidence | `PostgresContainerIT` starts `postgres:16`; Flyway/JDBC are test scope | Extend the same class; keep dependency scopes |
| `docs/12` 15 Phase A (460-466) | PostgreSQL/Flyway, Outbox, audit and identity foundations | V001 supplies platform/audit; four S1 schemas are empty | Add only V002 identity/organization/employee tables |
| `docs/12` 18 item 1 (520-525) | Data model and initial Flyway schema first | `docs/14` is the accepted A0 contract | Implement only the frozen V002 wave |
| `docs/14` 2.1 (148-158) | Owner/runtime separation; own-schema USAGE; exact cross-module reads; PUBLIC denial; migration identity | V001 creates owners/runtime roles and revokes PUBLIC | Grant own-schema USAGE without CREATE; no runtime grants beyond 10.8 |
| `docs/14` 2.4 (175-180), 2.7 (200-204) | Revision/default rules; immediate FK actions | V001 uses named checks and no cascades | Add revision/status/time checks and four immediate RESTRICT FKs |
| `docs/14` 3.1 (264-281), 3.2 (287-299) | Six S1 roles/runtimes; exact V002 table write paths | V001 creates the 15 S1 roles; V002 tables absent | Tables owned by the three V002 owners; only three runtime roles write |
| `docs/14` 3.3 (340-348) | Login identity, workspace access, business/login separation | No V002 table exists | No membership/employee login path; schema-only separation |
| `docs/14` 4.6-4.11 (598-805) | Exact six table contracts and lifecycle rules | V001 contains none of these tables | Reproduce exactly; no extra column/default/index; S1 proves all 34 frozen check predicates by normalized `pg_get_constraintdef` deparse |
| `docs/14` 5.3 (2519-2537) | Four V002 FKs; ordinary immediate; parent wave V002 | No V002 FK exists | Add exactly four FKs in V002 |
| `docs/14` 6.2 (2674-2695) | INV-01..INV-04 check/key/guard requirements | V001 tests cover only INV-27..INV-31/40 portions | Convert V002 portions into executable assertions |
| `docs/14` 9.4 (3053-3059) | Login projection reads exact organization/membership columns | No cross-module grant exists yet | Grant `customer_iam_role` exact SELECT columns |
| `docs/14` 9.5 (3061-3069) | Identity bind collision mapping and immutable relation | No identity table exists | Unique tuple + guard; test-only collision classification |
| `docs/14` 9.9 (3130-3135) | Last-admin advisory lock/count/conditional update | No employee table or function exists | Partial index/guard/grants; test-only protocol |
| `docs/14` 9.10 (3137-3146) | Membership pair lock, revoke/regrant, terminal row | No membership table exists | Partial unique/guard; test-only revoke/regrant |
| `docs/14` 10.8 (3515-3564) | Exact V002 schema/table/column grants | V001 grants only platform/audit | Implement the V002 subset exactly; no extra grants |
| `docs/14` 10.8.1 (3566-3582) | Six guard functions, SECURITY INVOKER, owner, fixed search_path, local EXECUTE | V001 has only `audit.reject_row_mutation` | Add six narrow guards; no generic framework |
| `docs/14` 10.10 (3605-3607) | Owner/PUBLIC/search_path/transaction-control/guard verification | V001 suite already verifies those patterns | Extend to the six guards |
| `docs/14` 11.3 (4163-4194) | V002 creation order and grants | V001 created schemas/roles only | Follow the exact six-step order |
| `docs/14` 12.1-12.4 (4257-4295) | All six tables are mutable heads; no ordinary delete; status retirement | V001 has no V002 tables | No DELETE/TRUNCATE runtime grants; guards are UPDATE-only |
| `backend/pom.xml` 41-77, 79-133, 136-162 | Flyway/JDBC/Testcontainers test scope; surefire exclusion; Enforcer firewall; container profile | Current POM is correct for PA1 | Do not change POM or dependency scopes |
| `backend/README.md` 3, 26-66, 95-106 | V001-only description, 10/12 counts, firewall/evidence boundary | README says V002+ is absent | Update only V002 counts/description |
| V001 SQL 1-537 | Frozen schemas/roles/platform/audit/function/grants | V001 HEAD blob hash matches the frozen hash | Do not edit; V002 assumes its objects |
| `PostgresContainerIT` 130-245, 1746-1866, 1617-2118 | Migration/inventory/failure and exact catalog helpers; check-name-only `pg_constraint` queries and exact `indexdef` assertions | 12 methods; exact 3-table/5-function inventory; no check-predicate deparse assertion yet | Rename two methods and expand inventory; add an explicit 34-entry check-name-to-normalized-`pg_get_constraintdef` map with exact equality; preserve V001 assertions |
| `PostgresContainerIT` 1082-1122 | Bounded concurrency blocking helpers | `configureBlockedLoser` hardcodes `subject_number_role`; `awaitFuture` is `BeginResult`-specific | Add a role-parameterized helper and genericize `awaitFuture` without changing behavior |
| `BootstrapDependencyBoundaryTests` 11-24 | Flyway absent from the bootstrap/surefire classpath | Test is unchanged and passes | Keep unchanged |
| `.github/workflows/ci.yml` 56-104, 105-115 | Exact container method set, no skips, boot smoke, fat-jar firewall | Expected set is the PA1 12 methods | Update only the expected set/count per slice; keep all other steps |
| PA1 plan/evidence | Behavior slices, fail-closed commands, per-slice completion reports | PA1 delivered 3 slices and 12 methods | Use the same evidence discipline, not stale PA1 assumptions |

### 3.1 Baseline environment findings

- `HEAD` is `d93c380b9f0025675931f950d95ca4cfef45647b`.
- The physical worktree was checked out on Windows with CRLF line endings, while every HEAD blob is LF. Consequently `git status` reports hundreds of line-ending-only modifications and raw `git diff --check` exits `2` with trailing-whitespace diagnostics for CRLF lines. This is an environment artifact, not a work-unit content change.
- `git show HEAD:<V001> | sha256sum` is exactly `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`. The raw on-disk V001 hash differs only because of CRLF; it must not be used as the V001 contract check.
- `git diff --ignore-cr-at-eol --exit-code HEAD -- <V001>` exits `0`.
- The only genuine tracked-worktree content difference at plan start is `docs/gateflow/project-execution-control.md` (the controller's expected PA2 update). The plan gate must not modify it.
- All repository files are LF in their HEAD blobs. Implementation agents must write new/edited files with LF and must not perform a repository-wide line-ending normalization.
- Validation must use `git -c core.whitespace=cr-at-eol diff --check` scoped to allowed paths and `git diff --no-index --check /dev/null <new-file>` for untracked files (exit `1` with no diagnostics means content-only difference).
- A disposable PostgreSQL 16 probe confirmed the cross-schema FK design: a child runtime role with INSERT on the child table and no SELECT on the parent table successfully inserted a row referencing an existing parent when the FK had been added by the migration identity. The probe returned `FK_INSERT_OK` and both the child runtime role and the child table owner reported no parent SELECT privilege. V002 therefore needs no cross-schema `REFERENCES` or parent `SELECT` grant for the membership FKs.
- A second disposable PostgreSQL 16 probe confirmed that a role with table `SELECT` and only column-level `UPDATE` can run `SELECT ... FOR UPDATE`; it returned `FOR_UPDATE_OK`. The documented last-admin row lock is therefore feasible under the frozen `operations_iam_role` grants, and the container test must reassert it.

## 4. First-principles judgment

V002 is the smallest useful next migration because every later S1 entity that needs an owner, actor or workspace depends on these six parents, while the design deliberately defers authentication, session storage, operations services and key management. Delivering the frozen persistence contract now gives later waves stable IDs and constraints without inventing service behavior.

Three implementation slices are the minimum that keeps each checkpoint independently falsifiable:

1. A migration/catalog slice proves the artifact is complete, runnable and exactly the frozen inventory.
2. A row-semantics slice proves defaults, checks, revisions, guards, identity collision and membership revoke/regrant behavior.
3. An authorization/transaction slice proves exact least privilege and the last-enabled-admin serialization that the schema alone cannot enforce.

Merging S2 and S3 would combine data-shape failures, privilege failures and concurrency protocol failures into one large method set and blur review boundaries. Splitting by table or file would be mechanical and would not establish independent behavior. The cross-schema FK creation as the migration identity and the owner-issued grants are deliberate least-privilege decisions: they avoid inventing `REFERENCES` grants or requiring the migration identity to hold grant options, while preserving the frozen ACL outcome. The test-only SQL protocols for identity collision and last-admin are evidence of documented transaction semantics, not new production services.

## 5. Exact affected files and modules

| Path | Slice | Change | Explicit boundary |
| --- | --- | --- | --- |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | S1 (authored once) | New complete V002 migration | Only the six tables, four FKs, six guards and exact grants; no V003+ |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | S1/S2/S3 | Rename/expand migration and inventory methods in S1 (including the exact 34-check normalized predicate map); add four S2 methods; add two S3 methods; add bounded helpers | Same class; no new test class; preserve all V001 methods/behavior |
| `.github/workflows/ci.yml` | S1/S2/S3 | Update only the failsafe method set/count assertion (12 -> 16 -> 18) and its label | Docker-first, boot smoke, fat-jar and contract jobs unchanged |
| `backend/README.md` | S1/S2/S3 | Incrementally describe V002 inventory, behavior and final 18-method profile | Keep toolchain pins, module table and evidence boundary; no production claim |
| `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md` | S1 | New evidence/completion report | Only this slice's evidence |
| `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s2-implementation.md` | S2 | New evidence/completion report | Only this slice's evidence |
| `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md` | S3 | New evidence/completion report | Only this slice's evidence |
| `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md` | plan | This plan artifact | The only path written by this gate |

Explicitly untouched: V001 SQL, `backend/pom.xml`, application Java, module packages, the V901 fixture, `docs/12`, `docs/14`, the control document, PA1 artifacts, API/UI/protocol/security/deployment/acceptance assets and all migrations other than V002.

## 6. Exact V002 schema and contract

### 6.1 Migration artifact and execution order

- Filename exactly `V002__identity_organization_and_employee.sql`; Flyway version `002`; expected description `identity organization and employee`.
- The migration runs in Flyway's PostgreSQL transaction. It contains no top-level `COMMIT`, `ROLLBACK` or `BEGIN`, no autonomous transaction, and no `SET TRANSACTION`/isolation change. `SET LOCAL ROLE` is used for object ownership; PL/pgSQL function bodies use ordinary block `BEGIN`/`END`.
- Tables, inline constraints, indexes, guard functions and triggers are created under the V002 schema owners using `SET LOCAL ROLE`; the migration identity adds the four cross-schema FKs in the same wave. Grants are issued by the owning schema role so the owner remains the grantor and the migration identity needs no ad-hoc grant options.
- Execution order:

1. `SET LOCAL ROLE customer_iam_owner`: create `customer_account`, its index, `auth_identity`, its unique constraint/index, and `identity_profile`, its unique constraint. `RESET ROLE`.
2. `SET LOCAL ROLE organization_access_owner`: create `organization`, its index, `organization_membership`, its partial unique index and two indexes. `RESET ROLE`.
3. `SET LOCAL ROLE operations_iam_owner`: create `employee_account`, its unique constraints and indexes. `RESET ROLE`.
4. As the migration identity, add the four ordinary immediate FKs with `ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY ... ON UPDATE RESTRICT ON DELETE RESTRICT` in section 6.3 order.
5. For each owner in turn, create its guard functions and triggers (section 6.4), then issue that owner's `REVOKE`/`GRANT` statements (section 6.5).
6. No other object is created.

- The cross-schema FKs are added by the migration identity so the child table owner does not need a cross-schema `REFERENCES` grant that is absent from §10.8. The resulting constraints belong to the child tables. The disposable PG16 probe in section 3.1 confirmed the runtime child insert does not require parent SELECT. If the production migration identity cannot add the FK, Q17 provisioning must resolve it; the migration must not add a speculative grant.
- Indexes are created in each table's owner block so index/table ownership stays with the schema owner. The FK step is the only cross-owner DDL.

### 6.2 Tables

All `id` columns are `uuid NOT NULL` with no default and no sequence. All text state values are `text` with named checks. All times are `timestamptz`. All tables are mutable heads with `revision integer NOT NULL DEFAULT 1 CHECK (revision >= 1)` and no ordinary delete. No table has `created_at` or `updated_at` unless the dictionary lists it.

The check predicates below restate the frozen `docs/14` 4.6-4.11 text only for readability; `docs/14` remains the authority and no alternative SQL contract is introduced. S1 asserts every one of the 34 predicates by PostgreSQL 16-normalized `pg_get_constraintdef` equality using the method in section 8.2.1.

#### 6.2.1 `customer_iam.customer_account`

Owner: `customer_iam_owner`. Wave V002. Six columns, in order.

| Column | Type | Null | Default | Constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_customer_account` | Stable internal identity |
| `display_name` | text | NO | none | check `ck_customer_account_display_name` | Length 1-200, nonblank |
| `status` | text | NO | `'ACTIVE'` | check `ck_customer_account_status` | `ACTIVE` or `DISABLED` |
| `revision` | integer | NO | `1` | check `ck_customer_account_revision` | Conditional-update revision |
| `created_at` | timestamptz | NO | `now()` | none | Immutable creation time |
| `updated_at` | timestamptz | NO | `now()` | check `ck_customer_account_time_order` | Last successful state update |

Named checks, exactly:

- `ck_customer_account_display_name`: `length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''`
- `ck_customer_account_status`: `status IN ('ACTIVE','DISABLED')`
- `ck_customer_account_revision`: `revision >= 1`
- `ck_customer_account_time_order`: `updated_at >= created_at`

Keys and indexes, exactly:

- Primary key `pk_customer_account (id)`.
- Index `ix_customer_account_status (status)`.
- No natural-key unique index, no foreign key.

#### 6.2.2 `customer_iam.auth_identity`

Owner: `customer_iam_owner`. Wave V002. Ten columns, in order.

| Column | Type | Null | Default | Constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_auth_identity` | Internal identity |
| `account_id` | uuid | NO | none | FK `fk_auth_identity_account` | Exactly one parent account |
| `provider` | text | NO | none | check `ck_auth_identity_provider` | `WECHAT` or `PHONE` |
| `provider_scope` | text | NO | none | check `ck_auth_identity_scope` | Length 1-128, nonblank |
| `provider_subject_ciphertext` | text | NO | none | check `ck_auth_identity_ciphertext` | Versioned envelope |
| `provider_subject_hmac` | text | NO | none | check `ck_auth_identity_hmac`; part of unique tuple | 64 lowercase hex |
| `status` | text | NO | `'ACTIVE'` | check `ck_auth_identity_status` | `ACTIVE` or `DISABLED` |
| `verified_at` | timestamptz | NO | none | none | Verification time |
| `key_version` | integer | NO | none | check `ck_auth_identity_key_version` | Positive generation reference |
| `revision` | integer | NO | `1` | check `ck_auth_identity_revision` | Conditional-update revision |

Named checks, exactly:

- `ck_auth_identity_provider`: `provider IN ('WECHAT','PHONE')`
- `ck_auth_identity_scope`: `length(provider_scope) BETWEEN 1 AND 128 AND btrim(provider_scope) <> ''`
- `ck_auth_identity_ciphertext`: `provider_subject_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`
- `ck_auth_identity_hmac`: `provider_subject_hmac ~ '^[0-9a-f]{64}$'`
- `ck_auth_identity_status`: `status IN ('ACTIVE','DISABLED')`
- `ck_auth_identity_key_version`: `key_version >= 1`
- `ck_auth_identity_revision`: `revision >= 1`

Keys and indexes, exactly:

- Primary key `pk_auth_identity (id)`.
- Unique constraint `uq_auth_identity_subject (provider, provider_scope, provider_subject_hmac)`.
- Index `ix_auth_identity_account_status (account_id, status)`.
- FK `fk_auth_identity_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

#### 6.2.3 `customer_iam.identity_profile`

Owner: `customer_iam_owner`. Wave V002. Nine columns, in order.

| Column | Type | Null | Default | Constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_identity_profile` | Internal identity |
| `account_id` | uuid | NO | none | unique `uq_identity_profile_account`; FK `fk_identity_profile_account` | One profile per account |
| `real_name_status` | text | NO | `'UNVERIFIED'` | check `ck_identity_profile_status` | `UNVERIFIED` or `VERIFIED` |
| `legal_name_ciphertext` | text | YES | none | check `ck_identity_profile_legal_ciphertext` | Envelope or null |
| `id_number_ciphertext` | text | YES | none | check `ck_identity_profile_id_ciphertext` | Envelope or null |
| `id_number_hmac` | text | YES | none | check `ck_identity_profile_id_hmac` | 64 hex or null |
| `verification_ref_ciphertext` | text | YES | none | check `ck_identity_profile_verification_ref` | Envelope or null |
| `verified_at` | timestamptz | YES | none | part of `ck_identity_profile_state_shape` | Non-null exactly when verified |
| `revision` | integer | NO | `1` | check `ck_identity_profile_revision` | Conditional-update revision |

Named checks, exactly:

- `ck_identity_profile_status`: `real_name_status IN ('UNVERIFIED','VERIFIED')`
- `ck_identity_profile_legal_ciphertext`: `legal_name_ciphertext IS NULL OR legal_name_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`
- `ck_identity_profile_id_ciphertext`: `id_number_ciphertext IS NULL OR id_number_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`
- `ck_identity_profile_id_hmac`: `id_number_hmac IS NULL OR id_number_hmac ~ '^[0-9a-f]{64}$'`
- `ck_identity_profile_verification_ref`: `verification_ref_ciphertext IS NULL OR verification_ref_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`
- `ck_identity_profile_state_shape`: `(real_name_status = 'UNVERIFIED' AND legal_name_ciphertext IS NULL AND id_number_ciphertext IS NULL AND id_number_hmac IS NULL AND verification_ref_ciphertext IS NULL AND verified_at IS NULL) OR (real_name_status = 'VERIFIED' AND legal_name_ciphertext IS NOT NULL AND id_number_ciphertext IS NOT NULL AND id_number_hmac IS NOT NULL AND verification_ref_ciphertext IS NOT NULL AND verified_at IS NOT NULL)`
- `ck_identity_profile_revision`: `revision >= 1`

Resolution of the specification alias: `docs/14` 4.8 lists the canonical named check as `ck_identity_profile_state_shape` and defines it as the full state shape. The `verified_at` table row abbreviates it as `ck_identity_profile_verified_shape`. V002 uses the canonical named-check list name `ck_identity_profile_state_shape` and creates exactly seven checks. It must not create a duplicate eighth check, which would violate exact inventory. The migration carries a short SQL comment mapping the row alias to the canonical name. This is a spec-editorial residual owned by the A0 spec/documentation owner, not a code divergence.

Keys and indexes, exactly:

- Primary key `pk_identity_profile (id)`.
- Unique constraint `uq_identity_profile_account (account_id)`.
- FK `fk_identity_profile_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- No index on `id_number_hmac`.

#### 6.2.4 `organization_access.organization`

Owner: `organization_access_owner`. Wave V002. Six columns, in order.

| Column | Type | Null | Default | Constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_organization` | Workspace identity; not a login |
| `display_name` | text | NO | none | check `ck_organization_display_name` | Length 1-200, nonblank |
| `status` | text | NO | `'ACTIVE'` | check `ck_organization_status` | `ACTIVE`, `SUSPENDED` or `DISABLED` |
| `revision` | integer | NO | `1` | check `ck_organization_revision` | Conditional-update revision |
| `created_at` | timestamptz | NO | `now()` | none | Immutable creation time |
| `updated_at` | timestamptz | NO | `now()` | check `ck_organization_time_order` | Last state update |

Named checks, exactly:

- `ck_organization_display_name`: `length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''`
- `ck_organization_status`: `status IN ('ACTIVE','SUSPENDED','DISABLED')`
- `ck_organization_revision`: `revision >= 1`
- `ck_organization_time_order`: `updated_at >= created_at`

Keys and indexes, exactly:

- Primary key `pk_organization (id)`.
- Index `ix_organization_status (status)`.
- No credential/login column, no foreign key.

#### 6.2.5 `organization_access.organization_membership`

Owner: `organization_access_owner`. Wave V002. Seven columns, in order.

| Column | Type | Null | Default | Constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_organization_membership` | Internal membership identity |
| `organization_id` | uuid | NO | none | FK `fk_membership_organization`; part of `uq_membership_live` | Workspace |
| `account_id` | uuid | NO | none | FK `fk_membership_account`; part of `uq_membership_live` | Natural-person account |
| `role` | text | NO | none | check `ck_membership_role` | `ADMINISTRATOR`, `MEMBER`, `EVIDENCE_MANAGER`, `AUDITOR` |
| `department_label` | text | YES | none | check `ck_membership_department` | Display-only, 1-200 when present |
| `status` | text | NO | `'ACTIVE'` | check `ck_membership_status` | `ACTIVE`, `SUSPENDED`, `REVOKED` |
| `revision` | integer | NO | `1` | check `ck_membership_revision` | Conditional-update revision |

Named checks, exactly:

- `ck_membership_role`: `role IN ('ADMINISTRATOR','MEMBER','EVIDENCE_MANAGER','AUDITOR')`
- `ck_membership_department`: `department_label IS NULL OR (length(department_label) BETWEEN 1 AND 200 AND btrim(department_label) <> '')`
- `ck_membership_status`: `status IN ('ACTIVE','SUSPENDED','REVOKED')`
- `ck_membership_revision`: `revision >= 1`

Keys and indexes, exactly:

- Primary key `pk_organization_membership (id)`.
- Partial unique index `uq_membership_live ON organization_access.organization_membership (organization_id, account_id) WHERE status <> 'REVOKED'`. It is an index, not a `pg_constraint` row.
- Index `ix_membership_account_status (account_id, status)`.
- Index `ix_membership_organization_status_role (organization_id, status, role)`.
- FK `fk_membership_organization (organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_membership_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

#### 6.2.6 `operations_iam.employee_account`

Owner: `operations_iam_owner`. Wave V002. Nine columns, in order.

| Column | Type | Null | Default | Constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_employee_account` | Internal employee identity |
| `login_name_hmac` | text | NO | none | unique `uq_employee_login_hmac`; check `ck_employee_login_hmac` | 64 lowercase hex |
| `login_name_masked` | text | NO | none | check `ck_employee_login_masked` | Display-only, 1-128 |
| `auth_issuer` | text | NO | none | part of `uq_employee_issuer_subject`; check `ck_employee_issuer` | Separate operations issuer, 1-128 |
| `auth_subject_hmac` | text | NO | none | part of `uq_employee_issuer_subject`; check `ck_employee_subject_hmac` | 64 lowercase hex |
| `display_name` | text | NO | none | check `ck_employee_display_name` | Length 1-200 |
| `role` | text | NO | none | check `ck_employee_role` | `ADMIN` or `OPS` |
| `status` | text | NO | `'ACTIVE'` | check `ck_employee_status` | `ACTIVE` or `DISABLED` |
| `revision` | integer | NO | `1` | check `ck_employee_revision` | Conditional-update revision |

Named checks, exactly:

- `ck_employee_login_hmac`: `login_name_hmac ~ '^[0-9a-f]{64}$'`
- `ck_employee_login_masked`: `length(login_name_masked) BETWEEN 1 AND 128 AND btrim(login_name_masked) <> ''`
- `ck_employee_issuer`: `length(auth_issuer) BETWEEN 1 AND 128 AND btrim(auth_issuer) <> ''`
- `ck_employee_subject_hmac`: `auth_subject_hmac ~ '^[0-9a-f]{64}$'`
- `ck_employee_display_name`: `length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''`
- `ck_employee_role`: `role IN ('ADMIN','OPS')`
- `ck_employee_status`: `status IN ('ACTIVE','DISABLED')`
- `ck_employee_revision`: `revision >= 1`

Keys and indexes, exactly:

- Primary key `pk_employee_account (id)`.
- Unique constraint `uq_employee_login_hmac (login_name_hmac)`.
- Unique constraint `uq_employee_issuer_subject (auth_issuer, auth_subject_hmac)`.
- Partial index `ix_employee_enabled_admin ON operations_iam.employee_account (id) WHERE role = 'ADMIN' AND status = 'ACTIVE'`.
- Index `ix_employee_auth_issuer_status (auth_issuer, status)`.
- No foreign key; employee identity is independent of customers and business subjects.

### 6.3 Foreign keys

All four are ordinary immediate, validated, `MATCH SIMPLE`, `ON UPDATE RESTRICT ON DELETE RESTRICT`, added in V002 after both tables exist.

| Constraint | Child column | Parent | Actions | Deferrable |
| --- | --- | --- | --- | --- |
| `fk_auth_identity_account` | `customer_iam.auth_identity.account_id` | `customer_iam.customer_account(id)` | RESTRICT/RESTRICT | No |
| `fk_identity_profile_account` | `customer_iam.identity_profile.account_id` | `customer_iam.customer_account(id)` | RESTRICT/RESTRICT | No |
| `fk_membership_organization` | `organization_access.organization_membership.organization_id` | `organization_access.organization(id)` | RESTRICT/RESTRICT | No |
| `fk_membership_account` | `organization_access.organization_membership.account_id` | `customer_iam.customer_account(id)` | RESTRICT/RESTRICT | No |

No V003+ FK is created. `operations_iam.employee_account` has no FK. `customer_account` and `organization` have no FK.

### 6.4 Local immutability guard triggers

All six functions and triggers are created under their owning schema role. Each function is `LANGUAGE plpgsql`, `SECURITY INVOKER`, `SET search_path = <owning schema>, pg_temp`, returns `trigger`, has no arguments, no transaction control, no `gen_random_uuid`, and ends with `RETURN NEW` after its checks. Each trigger is `BEFORE UPDATE ON <table> FOR EACH ROW`, fires on every update (not `UPDATE OF`), and fires only on `UPDATE`. Each function raises SQLSTATE `P0001` with the stable code shown. No generic trigger framework or extra function is added.

| Function | Trigger | Table | Reject predicate | Stable error |
| --- | --- | --- | --- | --- |
| `customer_iam.guard_customer_account` | `trg_customer_account_guard` | `customer_iam.customer_account` | `NEW.id` or `NEW.created_at` distinct from OLD | `CUSTOMER_ACCOUNT_IMMUTABLE` |
| `customer_iam.guard_auth_identity` | `trg_auth_identity_guard` | `customer_iam.auth_identity` | `id`, `account_id`, `provider`, `provider_scope` changes; `provider_subject_ciphertext`/`provider_subject_hmac` changes unless both change together and `NEW.key_version = OLD.key_version + 1` | `AUTH_IDENTITY_IMMUTABLE` |
| `customer_iam.guard_identity_profile` | `trg_identity_profile_guard` | `customer_iam.identity_profile` | `id` or `account_id` distinct from OLD | `IDENTITY_PROFILE_IMMUTABLE` |
| `organization_access.guard_organization` | `trg_organization_guard` | `organization_access.organization` | `id` or `created_at` distinct from OLD | `ORGANIZATION_IMMUTABLE` |
| `organization_access.guard_membership` | `trg_membership_guard` | `organization_access.organization_membership` | `id`, `organization_id` or `account_id` distinct from OLD | `MEMBERSHIP_IMMUTABLE` |
| `operations_iam.guard_employee_account` | `trg_employee_account_guard` | `operations_iam.employee_account` | `id`, `login_name_hmac`, `auth_issuer` or `auth_subject_hmac` distinct from OLD | `EMPLOYEE_ACCOUNT_IMMUTABLE` |

The auth guard implements the exact frozen rotation-shape predicate. It does not define KMS behavior, logical provider-subject preservation, envelope algorithms or a rotation service; those remain Q2. A key-version-only change is not forbidden by the frozen predicate and is not separately invented. The six guards are local structural controls, not entry-wave registry functions or published cross-schema APIs.

### 6.5 Grants, ownership and ACL

All `REVOKE`/`GRANT` statements are executed under the owning schema role, so the owner is the grantor and the migration identity does not need ad hoc grant options. No `ALTER DEFAULT PRIVILEGES` or implicit PUBLIC reliance is used. All tables/functions are explicitly revoked from `PUBLIC` and explicitly granted; the inventory asserts exact `aclexplode` grantees so a leaked default privilege fails. Runtime roles receive no `DELETE`, `TRUNCATE`, `REFERENCES` or `TRIGGER` privilege. No runtime role receives schema `CREATE`. No cross-module write or row-lock privilege is granted.

Schema `USAGE` (without `CREATE`):

| Schema | Runtime `USAGE` grantees |
| --- | --- |
| `customer_iam` | `customer_iam_role`, `subject_number_role` |
| `organization_access` | `organization_access_role`, `customer_iam_role`, `subject_number_role` |
| `operations_iam` | `operations_iam_role` |
| `subject_number` | none in V002 (V003 later; owner-only) |
| `platform`, `audit` | unchanged from V001 |

Owned-table grants:

| Table | Role | `SELECT` | `INSERT` | `UPDATE` columns |
| --- | --- | --- | --- | --- |
| `customer_iam.customer_account` | `customer_iam_role` | all (table-level) | all (table-level) | `display_name`, `status`, `revision`, `updated_at` |
| `customer_iam.auth_identity` | `customer_iam_role` | all | all | `status`, `verified_at`, `key_version`, `revision` |
| `customer_iam.identity_profile` | `customer_iam_role` | all | all | `real_name_status`, `legal_name_ciphertext`, `id_number_ciphertext`, `id_number_hmac`, `verification_ref_ciphertext`, `verified_at`, `revision` |
| `organization_access.organization` | `organization_access_role` | all | all | `display_name`, `status`, `revision`, `updated_at` |
| `organization_access.organization_membership` | `organization_access_role` | all | all | `role`, `department_label`, `status`, `revision` |
| `operations_iam.employee_account` | `operations_iam_role` | all | all | `display_name`, `role`, `status`, `revision` |

Cross-module `SELECT` grants (column-level only; no table-level `SELECT`, no writes, no row lock):

| Grantee | Table | Exact columns |
| --- | --- | --- |
| `customer_iam_role` | `organization_access.organization` | `id`, `display_name`, `status` |
| `customer_iam_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` |
| `subject_number_role` | `organization_access.organization` | `id`, `status` |
| `subject_number_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` |
| `subject_number_role` | `customer_iam.customer_account` | `id`, `status` |

Guard function grants:

| Function | Explicit `EXECUTE` grantee |
| --- | --- |
| `customer_iam.guard_customer_account()` | `customer_iam_role` |
| `customer_iam.guard_auth_identity()` | `customer_iam_role` |
| `customer_iam.guard_identity_profile()` | `customer_iam_role` |
| `organization_access.guard_organization()` | `organization_access_role` |
| `organization_access.guard_membership()` | `organization_access_role` |
| `operations_iam.guard_employee_account()` | `operations_iam_role` |

No other grantee; `PUBLIC` has no `EXECUTE`. Owner roles retain their inherent function/table/schema rights; the tests distinguish inherent ownership from explicit ACL grants.

Ownership:

- Table owners: `customer_iam_owner` for the three customer tables; `organization_access_owner` for the two organization tables; `operations_iam_owner` for the employee table.
- Guard function owners: the corresponding schema owner.
- Triggers are attached to the owning table.
- No role, schema or sequence is created by V002. No `id` default is added.

### 6.6 DB-enforced versus transaction-protocol behavior

| Contract | Enforced by V002 DDL | Demonstrated only by executable test protocol | Later owner |
| --- | --- | --- | --- |
| Column types, nullability, defaults, status/format checks, revision >= 1, time order | Yes | Exact normalized `pg_get_constraintdef` predicate inventory (S1) plus negative row probes (S2) | Backend/Database |
| PK, unique constraints, partial unique membership index, partial enabled-admin index | Yes | Duplicate/collision probes | Backend/Database |
| Four FK targets/actions/immediacy | Yes | Parent-delete RESTRICT probes; catalog for update RESTRICT | Backend/Database |
| Immutable relation columns and auth rotation shape | Yes (guards) | Owner-role guard probes | Backend/Security |
| Conditional revision update and stale-write zero rows | No (columns/check only) | `WHERE revision = expected` protocol test | Application services |
| Login status gating, identity bind same-account/different-account mapping | No | Conflict-then-read classification test | customer-iam service |
| Membership pair advisory lock and terminal REVOKED transition | No | Partial-unique/revoke/regrant collision test | organization-access service |
| Last-enabled-admin advisory lock and count/update | No | Exact lock/count/conditional-update test | operations-iam service |
| Audit append on lifecycle changes | V001 primitive exists | Not re-tested by V002 | Application services |

No application service, database service function, generic transaction helper or last-admin SQL function is added. The test protocols are transient SQL in the IT class and are not production objects. The classification strings `IDENTITY_CONFLICT` and `LAST_ENABLED_ADMIN` are test-protocol assertions, not new database objects, frozen error codes or published API contracts.

## 7. Behavioral implementation slices

Every slice leaves the V001+V002 migration prefix runnable and fail-closed. The full V002 migration is authored in S1; S2 and S3 add tests only and must not edit V002 unless a controller-approved bounded correction is required by a failing frozen-contract assertion. Any such correction reruns all prior slice tests.

### 7.1 S1 - V002 applies after V001 and the exact catalog is present

Objective: Prove the V002 artifact is complete, runnable, exactly the frozen catalog, and leaves V001 unchanged.

Prerequisites: Accepted PA1 V001 at `d93c380b9f0025675931f950d95ca4cfef45647b`; V001 HEAD blob hash; Docker-capable PostgreSQL 16 environment for the container checkpoint; existing Flyway/JDBC/Testcontainers test boundaries.

Allowed changes:

- `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` (new).
- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`:
  - Rename `v001MigratesCleanPostgres16WithoutSkipping` to `v001ThenV002MigrateCleanPostgres16WithoutSkipping`; assert two history rows, two executed migrations, V001 assertions preserved, repeated migrate zero, `validate()` pass, V001 function smoke, and a rolled-back V002 insert smoke.
  - Rename `v001InventoryMatchesTheFrozenContract` to `v001AndV002InventoryMatchesTheFrozenContract`; expand the inventory helpers for the six tables, four FKs, six guards, exact grants and no V003+ objects.
  - In that renamed method (via a private inventory helper), assert the 34-entry check-predicate mapping defined in section 8.2.1: the exact 4/7/7/4/4/8 check-name set per table plus exact normalized `pg_get_constraintdef` equality for every one of the 34 checks against an explicit expected map. No substring/contains comparison is permitted, and the S2 positive/negative probes are not a substitute.
  - Extend `functionsHaveFixedSearchPathAndNoTransactionControl` with the six guard-function properties (SECURITY INVOKER, owner, ordered search_path, no transaction control, no `gen_random_uuid`); keep its V001 assertions.
  - Expand the inventory helper `assertFunctionsAndTrigger` to the exact 11-function/7-trigger inventory.
  - Extend or leave `ownerPublicAndRoleExecutionBoundariesAreExact` unchanged where its platform/audit-only queries remain valid; the inventory helper is the authoritative V002 grant catalog, and S3 adds the V002 behavioral privilege probes.
  - Add private helpers for the V002 smoke and catalog assertions; no new test methods in S1.
- `.github/workflows/ci.yml`: update only the expected 12-method set (two renamed) and label.
- `backend/README.md`: describe V002 inventory/migration; keep the container count at 12 for S1.
- `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md` (new).

Invariants:

- V001 is byte-for-byte unchanged.
- V002 creates only the six tables, four FKs, six guard functions/triggers and exact grants.
- S1 asserts exact normalized `pg_get_constraintdef` equality for all 34 named checks against the explicit expected mapping (section 8.2.1); a name-only, missing/extra, or substring/contains match is a failure.
- No V003+ schema, role, table, function, trigger, index or sequence.
- No POM, application Java, runtime DataSource/Flyway or module-boundary change.
- The V002 insert smoke uses the owning runtime roles, inserts one valid row per table (membership referencing the account/organization created in the same transaction), and rolls back.

Tests/commands: Section 8 S1 rows; default verify; full container suite; V001/V002 hashes; scoped whitespace checks.

Expected assertions: 10 surefire tests; 12 container methods; `migrationsExecuted=2`; history `001`,`002`; exact catalog counts; all 34 check predicates match the explicit expected mapping by exact normalized `pg_get_constraintdef` equality with zero extra/missing checks (section 8.2.1); V002 smoke succeeds and rolls back; V001 functions still succeed; all V001 tests still pass.

Completion signal: All S1 commands pass; evidence artifact records exact environment/outputs/hashes; only allowed files changed.

Stop condition: Any need to edit V001, add a V003+ object, add a role/schema, add a POM/runtime dependency, add a database service function beyond the six guards, or weaken a V001 assertion. Any unexplained catalog mismatch is fixed only within the allowed V002 migration; otherwise stop and escalate.

### 7.2 S2 - Row lifecycle, guards, identity collision and membership replacement

Objective: Prove the DB-visible row semantics and the documented collision/revoke-regrant outcomes.

Prerequisites: S1 accepted checkpoint; recorded V002 SHA-256; all S1 tests passing.

Allowed changes:

- `PostgresContainerIT.java`: add exactly four methods and bounded helpers; genericize `awaitFuture` to `<T> T` without changing behavior; add a role-parameterized blocked-session helper; add savepoint/expected-failure helpers; add method-local cleanup helpers. Do not change V002 or the S1 catalog assertions except a necessary helper generalization.
- `.github/workflows/ci.yml`: expected set/count 16.
- `backend/README.md`: S2 behavior description and count 16.
- `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s2-implementation.md` (new).

Invariants:

- V002 hash unchanged from S1.
- No application service, SQL function, generic trigger framework or V003+ object.
- Test data is method-local; committed races are cleaned up as the migration identity in FK-safe order; no truncate; no append-only table mutation.
- Expected constraint failures assert exact SQLSTATE and constraint/error name.

Tests: Section 8 S2 methods.

Expected assertions:

- `mutableHeadsEnforceLifecycleRevisionAndNamedChecks`: defaults (`ACTIVE`/`UNVERIFIED`, revision 1, timestamps); valid conditional updates increment revision once; stale revision affects zero rows; every named check has a violating probe with SQLSTATE `23514` and its exact constraint name; NOT NULL probes are covered by catalog nullability; FK parent-delete RESTRICT probes raise `23503`; no id defaults/sequences. These valid/invalid-shape probes are behavior evidence and do not replace the S1 exact 34-check normalized `pg_get_constraintdef` inventory.
- `localImmutabilityGuardsRejectRelationAndRotationChanges`: owner-role probes for every immutable column raise the exact stable error; runtime column privileges deny immutable changes with `42501`; the auth rotation branch rejects one-sided changes and accepts both-changed-plus-key-version-increment under a rolled-back owner probe; allowed status/verification/key-version/revision updates succeed.
- `identityCollisionAndMembershipRevokeRegrantSemantics`: identity tuple uniqueness; same-account idempotent classification and different-account `IDENTITY_CONFLICT`; unique profile per account; employee login/issuer uniqueness; membership live uniqueness; revoke then new-row regrant; revoked row cannot be revived while a live row exists; multiple revoked rows allowed; guard rejects repointing.
- `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner`: two connections race the same identity tuple and the same live membership pair; one commits, the loser blocks on the unique index, receives `23505`, reads the winner and classifies; a rollback variant lets the loser insert; exactly one committed winner row per scenario; bounded blocking detection and cleanup.

Completion signal: S1+S2 tests pass (16 total); focused concurrency method passes in at least three executions; V001/V002 hashes unchanged; evidence artifact complete.

Stop condition: Any needed V002 change, any guard/constraint ambiguity beyond the frozen text, any test that requires an application service or function, or any cleanup/append-only violation. Escalate instead of weakening.

### 7.3 S3 - Exact least privilege and last-enabled-admin transaction protection

Objective: Prove runtime/cross-module authorization behavior and the last-enabled-admin serialization protocol.

Prerequisites: S1/S2 accepted checkpoints; recorded V002 SHA-256; all prior tests passing.

Allowed changes:

- `PostgresContainerIT.java`: add exactly two methods and bounded helpers; no V002 or prior-behavior changes except a necessary helper generalization.
- `.github/workflows/ci.yml`: expected set/count 18.
- `backend/README.md`: final V002 behavior, exact counts and evidence boundary.
- `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md` (new).

Invariants:

- V002 hash unchanged from S1.
- No new grants, roles, functions or services.
- All probes run under the actual runtime roles; the migration identity is used only for setup/cleanup and owner-role guard probes.
- No DELETE/TRUNCATE to runtime roles; no cross-owner row lock; no PUBLIC privilege.
- The last-admin test uses the exact advisory key `pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation', 0))`, row lock, enabled-admin count and conditional update; it is transient SQL, not a production function. The disposable PG16 probe in section 3.1 confirmed the required `FOR UPDATE` works with column-level `UPDATE`; the container test must reassert it.

Tests: Section 8 S3 methods.

Expected assertions:

- `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied`: owned-table SELECT/INSERT succeed; exact UPDATE columns succeed; undeclared UPDATE columns, DELETE and TRUNCATE fail with `42501`; cross-module exact columns are readable; undeclared columns and `FOR UPDATE` fail with `42501`; cross-module writes fail; employee/customer/organization domains remain separate; guard function EXECUTE is granted only to the owning runtime role and not PUBLIC.
- `lastEnabledAdminTransactionIsSerializedAndFailsClosed`: with two enabled admins, one disable/demote commits with revision+1; the final enabled admin cannot be disabled or demoted (`LAST_ENABLED_ADMIN`, changes nothing); two concurrent admin mutations serialize on the advisory lock, the second observes the new count and fails closed; final state has at least one enabled admin; cleanup removes only method-local employees.

Completion signal: Full 18-method container suite passes; focused last-admin method passes in at least three executions; default verify, boot smoke and fat-jar firewall pass; hashes/diffs clean; evidence artifact complete.

Stop condition: Any grant defect that requires a V002 change (escalate; do not widen), any last-admin need for a database function/service, any V003+ object, or any test weakening. Stop and report.

## 8. Test matrix, commands and expected assertions

### 8.1 Container IT method evolution

S1 keeps 12 methods; S2 reaches 16; S3 reaches 18. No placeholder or skipped methods are added. CI asserts exactly the methods present at each checkpoint.

| Slice | Count | Method set |
| --- | --- | --- |
| S1 | 12 | `v001ThenV002MigrateCleanPostgres16WithoutSkipping`, `v001AndV002InventoryMatchesTheFrozenContract`, `brokenMigrationFailsVisibly`, `idempotencyNewReplayConflictAndFinalizeOnce`, `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow`, `concurrentIdempotencyRetryObservesOneWinner`, `outboxReplayConflictRollbackAndDenylist`, `auditIsAppendOnlyAndRuntimeWritesAreDenied`, `auditReasonAndDenylistRulesAreEnforced`, `functionsHaveFixedSearchPathAndNoTransactionControl`, `ownerPublicAndRoleExecutionBoundariesAreExact`, `hostileSearchPathCannotRedirectFunctionEffects` |
| S2 | 16 | S1 set plus `mutableHeadsEnforceLifecycleRevisionAndNamedChecks`, `localImmutabilityGuardsRejectRelationAndRotationChanges`, `identityCollisionAndMembershipRevokeRegrantSemantics`, `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner` |
| S3 | 18 | S2 set plus `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied`, `lastEnabledAdminTransactionIsSerializedAndFailsClosed` |

### 8.2 Exact catalog assertions

S1 inventory must assert at least:

| Area | Exact expectation |
| --- | --- |
| Schemas | Exactly the six S1 schemas with existing owners plus `public`; no S2/S3 or extra schema |
| Roles | Exactly the 15 existing NOLOGIN/NOSUPERUSER/NOCREATEDB/NOCREATEROLE S1 roles; no new role |
| Tables | Exactly the nine V001+V002 tables; six V002 tables have the exact owners; zero tables in `subject_number`; only `public.flyway_schema_history` in `public` |
| Columns | Exact ordered column names, types, nullability and defaults for all six V002 tables; 6/10/9/6/7/9 columns; no extra column |
| Checks | Exact 4/7/7/4/4/8 named checks; no extra check; `ck_identity_profile_state_shape` present and `ck_identity_profile_verified_shape` absent; all 34 check predicates match the explicit expected mapping by exact normalized `pg_get_constraintdef` equality (section 8.2.1) |
| PK/unique constraints | Exact six PKs and four unique constraints; exact columns; no extra unique constraint |
| Partial unique/index | `uq_membership_live` is a unique partial index, not a constraint; exact predicate and columns |
| Indexes | Exactly 18 V002 indexes (6 PK, 4 unique-constraint, 6 named non-unique, `uq_membership_live`, `ix_employee_enabled_admin`); exact names/columns/uniqueness; exact `indexdef` for both partial indexes |
| FKs | Exactly four FKs; exact names/child/parent columns; `confupdtype='r'`, `confdeltype='r'`, `confmatchtype='s'`, `condeferrable=false`, `condeferred=false`, `convalidated=true`; no extra FK |
| Functions | Exactly 11 functions in the six schemas (5 V001 + 6 guards); guard signature `()`, return `trigger`, owner, `prosecdef=false`, ordered `search_path`, no p_id, no `gen_random_uuid`, no transaction control; `issue_first_trial` absent |
| Triggers | Exactly seven non-internal triggers (audit + six guards); each guard is BEFORE/ROW/UPDATE-only on its table and calls its exact function; audit trigger unchanged |
| Defaults/sequences | No `id` default on any of the nine tables; zero sequences in the six schemas |
| Schema ACL | Exact V002 USAGE matrix in 6.5; no CREATE for runtime roles; no PUBLIC USAGE/CREATE |
| Table/column ACL | Exact owned/cross-module grants in 6.5; no DELETE/TRUNCATE to runtime roles; no cross-module write; no REFERENCES/TRIGGER runtime grants |
| Function ACL | Guard EXECUTE only to the owning runtime role; no PUBLIC; owner inherent rights distinguished |
| PUBLIC | Zero `aclexplode(...).grantee = 0` rows on V002 schemas, tables and functions |

Exact column/default summary:

| Table | Ordered columns | Defaults |
| --- | --- | --- |
| `customer_iam.customer_account` | `id, display_name, status, revision, created_at, updated_at` | `status='ACTIVE'`, `revision=1`, `created_at=now()`, `updated_at=now()` |
| `customer_iam.auth_identity` | `id, account_id, provider, provider_scope, provider_subject_ciphertext, provider_subject_hmac, status, verified_at, key_version, revision` | `status='ACTIVE'`, `revision=1`; no default for the rest |
| `customer_iam.identity_profile` | `id, account_id, real_name_status, legal_name_ciphertext, id_number_ciphertext, id_number_hmac, verification_ref_ciphertext, verified_at, revision` | `real_name_status='UNVERIFIED'`, `revision=1`; no default for the rest |
| `organization_access.organization` | `id, display_name, status, revision, created_at, updated_at` | `status='ACTIVE'`, `revision=1`, `created_at=now()`, `updated_at=now()` |
| `organization_access.organization_membership` | `id, organization_id, account_id, role, department_label, status, revision` | `status='ACTIVE'`, `revision=1`; no default for the rest |
| `operations_iam.employee_account` | `id, login_name_hmac, login_name_masked, auth_issuer, auth_subject_hmac, display_name, role, status, revision` | `status='ACTIVE'`, `revision=1`; no default for the rest |

### 8.2.1 Check-predicate inventory method (F-PA2-PR-01)

`docs/14` 4.6-4.11 keeps the frozen predicate text; this section defines how S1 proves it and does not restate an alternative SQL contract. The 34 checks are the exact 4/7/7/4/4/8 named checks in section 6.2.

- **Authority and location.** The assertion runs inside the renamed S1 method `v001AndV002InventoryMatchesTheFrozenContract` via a private inventory helper. No new test method is added; the S1 container count stays 12.
- **Explicit expected mapping.** The test holds a static, explicit map `schema.table -> {check name -> expected normalized definition}` with exactly 34 entries whose names and predicates derive from `docs/14` 4.6-4.11. Each expected definition is the PostgreSQL 16 canonical `pg_get_constraintdef(oid)` string captured once from the real `postgres:16` container during S1 after V002 applies, then frozen in the test as a literal. The implementation must not hand-author an "equivalent" SQL predicate and must not change any source predicate. The S1 evidence artifact records, for each entry, the `docs/14` section/subsection and the raw captured deparse, so the S1 review can independently confirm derivation from the frozen predicates rather than from the implementation's own DDL; capturing a deparse from a migration whose predicate disagrees with `docs/14` is not valid evidence.
- **Actual evidence.** For each of the six V002 tables the helper queries `pg_constraint` joined to `pg_class`/`pg_namespace` with `contype = 'c'`, selecting `conname` and `pg_get_constraintdef(oid)`, then compares the name set and each definition to the expected map.
- **Narrow normalization.** Both sides pass through one fixed `normalizeCheckDef` helper. That helper may only trim leading/trailing ASCII whitespace and collapse each run of ASCII whitespace (`[ \t\r\n]+`) to a single space, and it is used only if real-container evidence shows the expected literal and the actual deparse differ solely in such whitespace. It must not change any other character, token, keyword, identifier, literal, cast, operator or parenthesis; must not case-fold, reorder, add or remove tokens; and must not perform substring, `contains`, prefix, regex or semantic-equivalence matching. If a real container shows a difference that is not formatting-only, stop and escalate; do not broaden the normalizer. The S1 evidence records the raw and normalized values that justify any normalization.
- **Fail-closed condition.** The assertion fails the build (uncaught `AssertionError`, no skip/warn fallback) if any of the 34 names is missing or misspelled, any extra `contype = 'c'` row exists on a V002 table, or any normalized `pg_get_constraintdef` differs from its expected entry. The failure names the table, constraint and actual-vs-expected definitions. Constraint names, exact normalized definitions and the no-extra-check count must all agree; any single mismatch fails the method.
- **Non-substitution.** The S2 positive/negative shape probes remain required behavior evidence, but they do not replace this exact S1 predicate inventory and cannot waive it.

### 8.3 Behavior assertions

The S2/S3 probes below are complementary behavior evidence. They do not replace the S1 exact 34-check normalized `pg_get_constraintdef` inventory (section 8.2.1), and that inventory does not replace these probes.

S2 covers:

- Defaults and valid conditional updates for all six tables.
- Stale-revision update affects zero rows and changes nothing.
- Every named check has a violating row probe asserting `23514` and the exact constraint name; positive/valid-shape acceptance remains covered by the defaults and valid-update assertions in the same method.
- FK child inserts with missing parents raise `23503`; parent delete with children raises `23503`; FK catalog covers on-update RESTRICT because guards make parent id changes unreachable.
- Guard probes as owner roles for all immutable columns and the auth rotation shape.
- Runtime column-privilege probes for immutable columns (`42501`).
- Identity tuple collisions, same-account classification, different-account `IDENTITY_CONFLICT`, profile/employee uniqueness.
- Membership live uniqueness, revoke, new-row regrant, revoked-row revival conflict, multiple revoked rows, immutable pair.
- Concurrent identity and membership unique collisions with one committed winner and a rollback variant.

S3 covers:

- Owned-table SELECT/INSERT success and exact UPDATE success.
- Undeclared UPDATE, DELETE and TRUNCATE denied with `42501`.
- Cross-module exact-column reads; undeclared columns, writes and `FOR UPDATE` denied.
- Schema USAGE exactness and no CREATE.
- Guard EXECUTE/PUBLIC boundaries.
- Last-admin sequential disable/demote failure with no change; two-admin success; concurrent serialization; final enabled-admin invariant.

Test implementation rules:

- Use method-local UUIDs and random 64-hex HMACs; no shared fixed rows.
- JUnit executes the class sequentially; `brokenMigrationFailsVisibly` keeps its `finally` cleanup of the isolated history/marker schemas so the inventory is not polluted.
- Use savepoints for expected failures inside multi-step transactions; roll back aborted transactions before switching roles.
- Use the migration identity only for setup/cleanup, owner guard probes and FK delete-restrict probes.
- Concurrency uses two connections, `application_name`, `pg_stat_activity` lock-wait detection and bounded timeouts; no unbounded sleeps.
- Cleanup is FK-safe and exact: delete membership rows first; then auth_identity and identity_profile rows; then organization rows; then customer_account rows; employees are independent and may be deleted separately. Use only method-local IDs. No truncate; no append-only mutation.
- No `@Disabled`, `Assumptions`, environment skip or weakened assertion.

### 8.4 Commands and expected results

Run from `backend/` with Temurin `21.0.13+9` and Maven `3.9.9`.

```bash
./mvnw -B -ntp validate
./mvnw -B -ntp clean verify
```

Expected: `BUILD SUCCESS`; Enforcer passes; 10 surefire tests, `skipped=0`; no container; no DataSource/Flyway bean.

```bash
docker info
./mvnw -B -ntp -Pcontainer-smoke verify
```

Expected at S1: `tests=12`; at S2: `tests=16`; at S3/final: `tests=18`; `skipped=0 failures=0 errors=0`; exact method set from 8.1; no pending migration.

Focused concurrency repetitions (at least three successful executions each):

```bash
./mvnw -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner'
./mvnw -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#lastEnabledAdminTransactionIsSerializedAndFailsClosed'
```

Expected: `tests=1`, `skipped=0`, no failure/error, in each repetition.

Firewalls and smoke:

```bash
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
! grep -Eq 'BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)' target/jar-index.txt
```

Expected: boot starts and exits; no forbidden library in the fat jar; POM/dependency scopes unchanged.

Hashes and whitespace:

```bash
git show HEAD:backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql | sha256sum
# expected (case-insensitive hex): 946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045
git diff --ignore-cr-at-eol --exit-code HEAD -- backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql
# expected: exit 0
sha256sum backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql
# record at S1; assert unchanged at S2/S3
git -c core.whitespace=cr-at-eol diff --check -- <exact changed tracked paths>
# expected: exit 0
git -c core.whitespace=cr-at-eol diff --no-index --check /dev/null <new untracked file>
# expected: exit 1 with zero diagnostic output
```

A global raw `git diff --check` is not authoritative in this Windows-checkout worktree because of the baseline CRLF lines and pre-existing documentation trailing whitespace. The scoped CR-aware check plus no-index new-file check is the equivalent fail-closed gate.

### 8.5 CI assertion evolution

Update only the failsafe report assertion in `.github/workflows/ci.yml`:

- S1: exactly one `TEST-*PostgresContainerIT.xml`; `tests=12`; the S1 set in 8.1; `skipped=0`, `failures=0`, `errors=0`.
- S2: `tests=16`; the S2 set.
- S3: `tests=18`; the S3 set.

Keep unchanged: `docker info` before Maven; `-Pcontainer-smoke verify`; surefire skip check; boot smoke; fat-jar firewall; contract regressions; permissions; path filters; toolchain pins. The assertion label may change from "S3" to "V002" but no step is removed or weakened.

### 8.6 README evolution

Update `backend/README.md` only:

- S1: title/intro/evidence paragraphs describe V001+V002; state V002 adds the six tables, four FKs, six guards and exact grants; remove the "V002+ absent" claim; keep the 10-test default and 12-test container count with the two renamed methods.
- S2: add the four behavior methods and count 16.
- S3: final 18 methods, exact least-privilege/last-admin evidence, V001 hash preservation, Q2/Q17/Q23 deferral and the non-production evidence boundary.
- Keep toolchain pins, module boundary table, firewall description and Modulith note unchanged.

### 8.7 V001 hash and line-ending baseline

The V001 contract check is the HEAD blob hash and the CR-normalized diff, not the raw worktree hash. Implementation agents must not normalize line endings repository-wide. New/edited files are LF. `git status` line-ending-only noise is expected and must not be staged or reported as V002 scope.

## 9. Documentation decision

- Do not modify `docs/12`, `docs/14`, the control document or PA1 artifacts. They are binding/accepted inputs.
- Do not add architecture, API, security, deployment or schema documents. This plan is the only new design document for the work unit.
- Update `backend/README.md` as described in 8.6.
- Add one implementation completion/evidence artifact per slice at the exact paths in section 5.
- The V002 SQL may carry a short header comment and the §4.8 alias comment; comments do not change catalog behavior.

## 10. Risks, open questions, owners and destinations

| ID | Risk / open question | Classification | Owner | Destination / action |
| --- | --- | --- | --- | --- |
| R-01 | `docs/14` 4.8 row alias `ck_identity_profile_verified_shape` differs from the named-check list `ck_identity_profile_state_shape`. | Resolved in plan; spec-editorial residual | A0 spec/documentation owner | Use `ck_identity_profile_state_shape`; no duplicate check; reconcile the alias in a future spec correction, not in V002 |
| R-02 | Windows checkout CRLF vs LF HEAD blobs makes raw hashes/`git status`/`git diff --check` misleading. | Environment/tooling | Controller/Platform | Use HEAD blob hash, CR-normalized diffs and scoped whitespace checks; do not normalize repository-wide |
| R-03 | Cross-schema FK creation by the migration identity depends on Q17 provisioning. | Deferred input | Platform/Database/Security | Q17; test superuser is a fixture; fail closed rather than inventing a `REFERENCES` grant |
| R-04 | Production migration identity/role membership and grantor capability. | Deferred input | Platform/Database/Security | Q17; owner-issued grants minimize the requirement; no production claim |
| R-05 | Q2 provider-subject re-encryption/key rotation semantics. | Deferred input | Security/Database | Q2; only the frozen guard shape is tested; no rotation service/KMS guess |
| R-06 | Q23 deployment-role `SECURITY DEFINER` capability. | Deferred input | Platform/Database/Security | Q23; no SECURITY DEFINER function is added by V002 |
| R-07 | Account-status login gating and identity bind serial order are service-owned. | Deferred behavior | customer-iam service | Application service; V002 enforces unique/immutable schema only |
| R-08 | Membership pair advisory lock and terminal REVOKED transition are service-owned. | Deferred behavior | organization-access service | Application service; V002 enforces partial unique/immutable relation |
| R-09 | Last-enabled-admin advisory/count/update is a test protocol, not a database function. | Bounded evidence decision | operations-iam service | Application service; V002 provides index/guard/grants; test proves the protocol |
| R-10 | Audit append on lifecycle changes is not exercised by V002 tests. | Deferred service behavior | Application services | PA1 audit primitive already accepted; no committed audit residue added |
| R-11 | `postgres:16` remains a floating test image tag. | Inherited residual | Platform/CI | Supply-chain hardening; no digest guess |
| R-12 | Exact PG16 check/index deparse forms can vary. | Test-only fail-closed risk | Backend/QA | S1 asserts exact check names plus normalized `pg_get_constraintdef` equality for all 34 checks against an explicit frozen mapping (section 8.2.1), and exact partial `indexdef`. Only container-evidenced whitespace normalization is allowed; expected deparse is corrected only with recorded container evidence; never substring/contains, never weaken semantics. |
| R-13 | Column-level privilege semantics can be confused with table-level privileges. | Test-only risk | Backend/QA | Assert both `has_table_privilege` and `has_column_privilege` with explicit expectations |
| R-14 | Concurrency tests can be timing-sensitive. | Bounded test risk | Backend/QA | Two connections, application_name, lock-wait detection, bounded timeouts, focused repetitions |
| R-15 | Runtime `SELECT ... FOR UPDATE` with column-level UPDATE must be allowed for the documented last-admin service. | Probe-confirmed; implementation check | Backend/Database | Disposable PG16 probe returned `FOR_UPDATE_OK`; the container test must reassert it. If a target PG16 denies it, stop and escalate the frozen grant-matrix conflict; do not widen grants |
| R-16 | GitHub Actions has not run at plan/implementation gates. | Gate residual | Phaseflow/CI | Exact-head PR gate; local/CI success is not production acceptance |
| R-17 | The plan's test-only protocols could drift into production behavior. | Scope guard | Backend/Controller | Keep them in the IT class; no function/service/trigger beyond the six guards |

No blocking open question remains for planning. Q2/Q17/Q23 remain deferred and cannot be guessed. R-01 is resolved by the named-check authority. R-15 is a concrete implementation stop condition.

## 11. No-goal-drift and no-over-design check

| Check | Evidence |
| --- | --- |
| Only V002 exists | One migration file; six tables; four FKs; six guards; no V003-V009 |
| No later domain behavior | No subject/number/claim/risk/activation/consent/IVR/trial/catalog/order/payment/evidence/sharing object |
| No application authentication | No credential/OTP/session/API/runtime DataSource; customer and employee domains remain separate |
| No membership login | Membership has no provider/credential/session column and no auth FK; exact inventory |
| No account/subject real-name conflation | `identity_profile` is keyed only to `customer_account`; no subject table |
| No extra table/column/default/index | Exact 4.6-4.11 catalog; no timestamps on tables that do not list them; no id default/sequence |
| No extra function/service | Only six local trigger functions; no published SQL function; `issue_first_trial` absent |
| No generic trigger framework | Six explicit narrow guards; no dispatcher/mutation framework |
| No speculative key rotation | Only the frozen auth guard predicate is tested; Q2 untouched |
| No Q17/Q23 guess | Test superuser is a fixture; no LOGIN role/provisioning claim |
| Exact least privilege | Owned/cross-module grants from 10.8; own-schema USAGE from 2.1; no DELETE/TRUNCATE/write/row lock; PUBLIC denied |
| No V001 change | V001 hash/diff checks; V001 assertions preserved |
| Three behavioral slices | Migration/catalog; row semantics; authorization/transaction protocol; no table/file split |
| Minimum docs/CI/evidence | Only README, CI method set and per-slice evidence artifacts |
| Test-only protocols are not services | Identity collision and last-admin live only in the IT class; no production object |

## 12. Completion report format

Each implementation Agent writes exactly one new artifact at the slice path in section 5 with:

1. Task label, work unit, gate, slice, date, author/provider.
2. Binding inputs read and confirmation that the control document was not modified.
3. Base commit, accepted prior-slice checkpoint, V001 HEAD hash, and V002 SHA-256 before/after.
4. Exact changed paths with SHA-256 and a content-only diff manifest accounting for the CRLF baseline.
5. Scope actually implemented, including the exact six tables/four FKs/six guards/grants for S1 or the exact methods for S2/S3. For S1, the 34-entry check-predicate mapping evidence: the exact asserting method, the name/set equality result, the per-entry `docs/14` source and raw captured deparse, and any container-evidenced normalization with raw/normalized samples.
6. Exact test methods added/changed and the CI expected set/count.
7. Exact commands, environment (JDK, Maven, Docker, PostgreSQL, Flyway), exits, and surefire/failsafe counts (`10` default; `12/16/18` container; `skipped=0 failures=0 errors=0`).
8. Negative and concurrency evidence: exact SQLSTATEs, constraint/error names, focused repetition counts, blocking detection, and cleanup evidence.
9. V001/V002 hash and whitespace/diff evidence, including the raw-`git diff --check` baseline caveat.
10. README/CI changes and documentation decision.
11. Any deviation, stop condition or unresolved issue, with owner/destination.
12. Residual risks classified as fixed-current, later-approved-slice or later-work-unit, with owners.
13. Completion or blocked statement; explicit statement that no later gate, commit, push, PR, merge or production acceptance was performed.
14. Explicit no-goal-drift statement and exact changed-file list.

The report must not claim GitHub Actions, production provisioning or production acceptance unless those actions actually ran.

## 13. Plan validation performed by the author

- Read the PA2 control contract, `docs/12` sections 5, 6, 14, 15 Phase A and 18 item 1, the binding `docs/14` sections, `backend/pom.xml`, `backend/README.md`, V001, the existing `PostgresContainerIT`, `BootstrapDependencyBoundaryTests`, `.github/workflows/ci.yml`, the PA1 plan/evidence conventions and the A0 completion-report convention.
- Cross-checked the six table definitions, 34 named checks, 10 PK/unique constraints, 18 indexes, four FKs, six guards, exact grants, creation order and lifecycle rules against `docs/14` 4.6-4.11, 5.3, 9.4, 9.5, 9.9, 9.10, 10.8, 10.8.1 and 11.3.
- Confirmed V001's HEAD blob hash is the frozen value and that no V001 edit is planned.
- Confirmed the plan prescribes no V003+ migration/object, no application authentication/service/runtime wiring, no POM change and no extra grant beyond §2.1/§10.8/§10.8.1.
- Verified the plan names exact allowed files, exact test-method evolution, exact CI assertion evolution and exact validation commands.
- Created only `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md`.
- Ran read-only baseline checks: V001 HEAD hash, CR-normalized V001 diff, global `git diff --check` (exit `2` due to the CRLF baseline and pre-existing documentation trailing whitespace), per-file content-diff identification, and untracked-file inventory.
- Ran one disposable `postgres:16` cross-schema FK probe (no repository change): FK created by the migration identity; child runtime role with INSERT on the child and no parent table SELECT; valid parent insert; child insert returned `FK_INSERT_OK`; the container was removed. This validates the no-extra-grant FK decision.
- Ran one disposable `postgres:16` row-lock probe (no repository change): table SELECT plus column-level UPDATE; `SELECT ... FOR UPDATE` returned `FOR_UPDATE_OK`; the container was removed. This validates the last-admin protocol's row-lock assumption.
- Ran the plan-file whitespace/no-EOL-whitespace check with `git diff --no-index --check /dev/null <plan>` and a trailing-whitespace scan.
- Did not run Maven, the Flyway migration, the implementation suite or the full container profile at this plan gate; all implementation expectations are planned evidence, not executed results.

### 13.1 Plan-fix 1 validation (F-PA2-PR-01)

- F-PA2-PR-01 final status `已修复` in this plan: added the S1 34-check exact normalized `pg_get_constraintdef` inventory contract in §1.3, §2, §3, §5, §6.2, §6.6, §7.1, §8.2/§8.2.1, §8.3, R-12 and §12, and retained the S2 positive/negative probes as non-substitutable.
- Re-read the edited sections for internal consistency; confirmed the three slices and the 12/16/18 method evolution are unchanged; confirmed no V003+ object, application service, extra migration object, POM/runtime or production-provisioning scope entered.
- Recomputed the plan SHA-256 and ran scoped CR-aware and trailing-whitespace checks. This note was written at the `plan fix` gate and is not an implementation claim.
