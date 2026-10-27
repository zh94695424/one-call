# PA4 V004 Catalog Order and Payment Foundation - Implementation Plan

- Task label: `pa4-v004-plan-ds-20261028-01`
- Work unit: `PA4-v004-catalog-order-payment-foundation`
- Gate: `plan`
- Author: DeepSeek (plan only; no implementation, no Git action)
- Branch: `codex/pa4-v004-catalog-order-payment-foundation`
- Base: merged PR #11 / `origin/main` commit `81022037dc5dced625788477835472a84a16d52d`
- Binding inputs: `docs/gateflow/project-execution-control.md` PA4 goal/slice/gate sections; `docs/12-technology-stack-and-system-architecture.md` sections 5, 6, 14, 15; `docs/14-production-data-model-and-flyway-schema-spec.md` sections 2.9, 4.16-4.22, 5.4, 9.0.1, 9.17, 9.18, 9.22, 9.26, 10.12, 10.13, 10.16, 10.17, 10.19, 11.4, 13.2.1, 13.6, 13.7, 13.10, 13.11, 15 and 16; accepted V001/V002/V003 migrations; `PostgresContainerIT`; `.github/workflows/ci.yml`; `backend/README.md`; `backend/pom.xml`.

This plan implements exactly `V004__catalog_order_and_payment.sql`. It does not implement catalog publication, order creation, purchase consent, payment callback processing, entitlement issuance, provider integration, or any V005+ object. The controller contract is binding; this document does not add goals, acceptance criteria, hardening waves, application services or future-slice work.

## 1. Goal, success signals, scope and independent inventory

### 1.1 Goal

Implement exactly one forward-only Flyway migration, `V004__catalog_order_and_payment.sql`, so later consent, entitlement, customer-purchase and payment-processing work has stable catalog product/version/offer/benefit, immutable order snapshot, payment-order and append-only provider-event parents. V004 creates the four frozen S2 schemas and eight NOLOGIN owner/runtime roles, but only the seven `catalog_order` tables. The `customer_order.purchase_consent_id` column is `NOT NULL` and has no FK until V007.

### 1.2 Frozen success signals (must be proven, not reinterpreted)

- Flyway applies V001 through V004 to a clean PostgreSQL 16 database.
- Exactly four S2 schemas: `catalog_order`, `ivr`, `entitlement_metering`, `activation_consent`; exactly eight NOLOGIN, NOSUPERUSER, NOCREATEDB, NOCREATEROLE roles: `catalog_order_owner`, `catalog_order_role`, `ivr_owner`, `ivr_role`, `entitlement_metering_owner`, `entitlement_metering_role`, `activation_consent_owner`, `activation_consent_role`.
- Exactly seven `catalog_order` tables; only those seven tables exist in the four S2 schemas after V004. `catalog_order` owns all seven.
- Exact counts: 78 columns; 51 named checks; seven PKs plus 11 ordinary/partial unique keys; 28 total indexes including PK/unique indexes; 11 ordinary immediate `RESTRICT` FKs; five `SECURITY INVOKER` trigger functions; seven triggers.
- `customer_order.purchase_consent_id` is `NOT NULL` with no default and no FK; `fk_customer_order_consent` is absent until V007.
- Exact owner/runtime/cross-module grants, column-level ACLs, function `EXECUTE` matrix and zero-PUBLIC schema/table/function/column privileges.
- V005+ tables, `issue_first_trial`, future owner capability and forward grants are absent.
- Fail-closed PostgreSQL 16 evidence covers inventory, lifecycle/revision and immutable-snapshot guards, append-only rows, active-version and identity uniqueness, V004 FK backstops, exact least privilege and bounded publication/order/payment concurrency backstops.
- Default verification, the PostgreSQL 16 container profile and boot/firewall checks pass with no skip, failure or error; source/CI/Failsafe XML method sets are exact.
- V001/V002/V003 Git blobs remain byte-identical at the frozen SHA-256 values.

### 1.3 Scope boundary

Allowed changes: the new V004 migration; focused cumulative additions to `PostgresContainerIT`; the minimum CI expected-set update; `backend/README.md` evidence; the required PA4 Gateflow artifacts/controller bookkeeping (controller-owned, not written by the implementation author). Database tests may use bounded transient SQL to prove documented transaction/concurrency backstops, but must label all service orchestration as deferred.

### 1.4 Non-goals

No V005-V009 migration or object; no IVR, entitlement, activation-consent, evidence or sharing table/function; no catalog-publication, order-creation, purchase-consent, payment-callback, entitlement-issuance or provider application service; no API, controller, JPA aggregate/repository, frontend, runtime DataSource/Flyway wiring, cloud/provisioning resource; no refund/invoice/work-order object; no early V007 consent FK; no invented offer deletion/withdrawal endpoint or entitlement status column; no guessed Q2/Q17/Q20/Q23, OQ-4 or Q4/Q6 values.

### 1.5 Independent inventory recount (verified against docs/14 and a disposable PG16.15 probe)

| Item | Frozen | Independent derivation | Evidence |
| --- | --- | --- | --- |
| S2 schemas | 4 | `catalog_order`, `ivr`, `entitlement_metering`, `activation_consent` | docs/14 2.9, 11.4 V004 step 1 |
| NOLOGIN roles | 8 | one owner + one runtime per schema | docs/14 2.9 |
| V004 tables | 7 | exact V004 membership | docs/14 1.5, 11.1 |
| Columns | 78 | 4 + 17 + 7 + 6 + 21 + 11 + 12 | docs/14 4.16-4.22, 13.2.1 |
| Named checks | 51 | 2 + 12 + 4 + 4 + 13 + 9 + 7 | docs/14 4.16-4.22 |
| PKs | 7 | one per table | docs/14 4.16-4.22 |
| Ordinary/partial unique keys | 11 | 9 ordinary unique constraints + 2 partial unique indexes | docs/14 4.16-4.22 |
| Total indexes | 28 | 7 PK indexes + 9 unique-constraint indexes + 2 partial unique indexes + 10 `ix_*` indexes | docs/14 4.16-4.22 |
| V004 FKs | 11 | version 2, offer 1, benefit 1, customer_order 5, payment_order 1, payment_event 1 | docs/14 4.16-4.22, 5.4 |
| Deferred FK | 1 | `fk_customer_order_consent` added by `ALTER` in V007; column exists in V004 | docs/14 4.20, 5.4, 11.4 V007 step 3 |
| Trigger functions | 5 | `reject_row_mutation`, `guard_catalog_version_published`, `guard_catalog_offer`, `guard_customer_order_snapshot`, `guard_payment_order_identity` | docs/14 10.16 |
| Triggers | 7 | 3 append-only + 4 guard triggers | docs/14 10.16 |
| Defaulted columns | 16 | product 1, version 3, offer 3, benefit 0, order 3, payment_order 4, payment_event 2 | docs/14 4.16-4.22; probe |
| V005+ tables | 0 | `ivr`, `entitlement_metering`, `activation_consent` have zero tables; `evidence`/`sharing` absent | docs/14 11.4 V004 step 5; probe |
| `issue_first_trial` | 0 | not created before V006 | docs/14 10.9, 11.4 V006 step 3 |

The disposable probe applied V001-V004 in one transaction per file and returned exactly: 7 tables, 78 columns, 51 checks, 7 PK constraints, 9 unique constraints, 11 FKs, 28 indexes, 5 functions, 7 triggers, 16 defaulted columns, 0 sequences, 0 V005+ tables, 0 `issue_first_trial`, and global totals 20 functions / 18 triggers. The probe container and temporary files were removed; no repository file was changed by the probe.

### 1.6 Resolved interpretations (no blocking question remains)

The following points are resolved from explicit binding text or accepted evidence; they are not guesses. If the plan reviewer rejects an interpretation, it is a bounded plan fix before implementation, not an implementation-time redesign.

1. **`ivr` schema `USAGE` to `ivr_role` is deferred to V005.** docs/14 11.4 V004 step 1 grants `USAGE` on `catalog_order`, `activation_consent` and `entitlement_metering`, but not on `ivr`; V005 step 4 explicitly grants `ivr_role` `USAGE` on its own `ivr` schema. V004 therefore asserts `ivr_role` has no `ivr` `USAGE` and V004 grants none. This is the F-01 creation/first-read-wave pairing applied by the accepted wave order.
2. **`entitlement_metering_role` reads on V004 `catalog_order` tables are granted in V004.** docs/14 11.4 V004 step 4 says to apply the V004 owned-table grants and the §10.12 cross-module `SELECT` grants; the target tables and the grantee role both exist after V004. §10.12 lists exactly `customer_order`, `payment_order` and `payment_event`. V006 step 5 applies the V006-only grants (including `subject_number` and `catalog_order_role` reads of V006 tables); it does not need to re-grant V004 tables.
3. **`reject_row_mutation` has no runtime `EXECUTE` grant.** docs/14 10.16 says to grant `EXECUTE` on each guard function only to the owning runtime role. The accepted V003 implementation and its suite granted `EXECUTE` only on `guard_*` functions and deliberately did not grant `subject_number.reject_row_mutation` to any runtime or verification role (backend/README.md, PA3 evidence). V004 follows that accepted precedent: four `guard_*` functions are granted to `catalog_order_role`; `reject_row_mutation` is owner-inherent only. The append-only trigger still fires for all writers.
4. **Trigger names for the four non-frozen guards.** docs/14 freezes the five function names and only three trigger names (`trg_catalog_product_immutable`, `trg_catalog_benefit_line_immutable`, `trg_payment_event_immutable`). This plan fixes the four other trigger names as `trg_catalog_version_guard`, `trg_catalog_offer_guard`, `trg_customer_order_guard`, `trg_payment_order_guard`. These are implementation identifiers, not new schema objects.
5. **Stable guard error codes.** docs/14 requires a stable SQL error but does not freeze message text. This plan fixes `P0001` codes: `CATALOG_ROW_APPEND_ONLY`, `CATALOG_VERSION_PUBLISHED_IMMUTABLE`, `CATALOG_OFFER_RELATION_IMMUTABLE`, `CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE`, `PAYMENT_ORDER_IDENTITY_IMMUTABLE`. Tests assert the exact code and message.
6. **`catalog_version` UPDATE column set.** §10.12 enumerates the draft-content columns after the phrase "draft content columns": `offer_type`, `audience`, `price_cents`, `currency`, `billing_cycle`, `installments`, `grant_interval_months`, `snapshot`, `snapshot_sha256`, plus `sale_status`, `published_at` and `revision`. `product_id`, `version_no`, `created_by_employee_id` and `created_at` are not granted for `UPDATE`; the guard also rejects them after publication.
7. **`catalog_offer` withdrawal is deferred.** docs/14 4.18 and 11.4 V004/OQ-4 explicitly do not implement an independent withdrawal endpoint or transaction. V004 creates the `ACTIVE`/`WITHDRAWN` state and guard only; version `OFFSALE` is the frozen new-order control.
8. **No CASCADE, no deferral.** Every V004 FK is ordinary immediate `ON UPDATE RESTRICT ON DELETE RESTRICT`; no S2 deferrable FK exists. The V007 consent FK is ordinary immediate when added later.

No blocking open question remains for this persistence-only plan. Q2/Q4/Q6/Q17/Q20/Q23/OQ-4 and the service-owned publication/order/payment/issuance behaviors retain their named owners and must not be guessed.

## 2. Goal alignment

| Confirmed goal area | V004 evidence | Deferred behavior (not implemented) |
| --- | --- | --- |
| Catalog product/version/offer/benefit | Seven `catalog_order` tables with immutable product/benefit identity, version snapshot/hash, offer channel/token and active-version uniqueness | Publication/offsale service, offer withdrawal endpoint (OQ-4) |
| Immutable order snapshot | `customer_order` immutable commercial/consent/fingerprint columns, payment-only lifecycle, `UNPAID`/`UNKNOWN`/`PAID`/`CLOSED` shape | PURCHASE idempotency, consent append, order creation transaction (§9.17) |
| Payment order and provider event | `payment_order` identity/amount/status shape; `payment_event` append-only encrypted payload and provider-event uniqueness | Payment callback verification, provider payload mapping (Q6), issuance (§9.18) |
| Least privilege | Four S2 schemas/roles; owned/cross-module column ACLs; platform/audit function EXECUTE; PUBLIC zero | Production role provisioning (Q17/Q23) |
| Audit/Outbox/idempotency reuse | No new platform/audit table or function; S2 roles receive the four accepted S1 function `EXECUTE` grants | Service calls to those functions |
| Deletion/retention | Append-only product/benefit/payment-event; status retirement only; no delete/cascade grants | Retention/legal policy (Q3) |
| Migration order | V004 after V003, before V005; no forward object | V005+ |

## 3. Design-document alignment and direct-code evidence

### 3.1 Binding design sections

- docs/12 sections 5 (catalog-order module), 6 (module ownership: `catalog-order` owns product/version/catalog/order/payment; `entitlement-metering` depends on catalog/order snapshots), 14 (Testcontainers/Flyway/unique-constraint testing), 15 Phase A/C (platform foundation, purchase/payment).
- docs/14 2.9 (S2 schemas/roles), 4.16-4.22 (complete dictionaries/lifecycle/immutability), 5.4 (S2 FK decisions; `purchase_consent_id` is an `ALTER_FK` in V007), 9.0.1 (S2 lock order; owner publication advisory key), 9.17 (order creation service), 9.18 (payment to issuance service), 9.22 (S2 transaction assertions), 9.26 (serialized catalog publication), 10.12 (S2 grant matrix), 10.13 (services are application services, not SQL functions), 10.16 (S2 guard triggers), 10.17 (fingerprints/Outbox identities), 10.19 (typed commerce/schedule snapshots), 11.4 V004 (exact creation order), 13.2.1 (S2 column counts), 13.6/13.7 (S2 control/behavior matrices), 13.10/13.11 (closure/limitations), 15 (goal alignment/no-over-design), 16 (completion template).
- Accepted V001/V002/V003 migrations and PA1-PA3 evidence are immutable prerequisites. Their execution pattern (owner-issued DDL, migration-identity FKs after `RESET ROLE`, `SECURITY INVOKER` guard functions with `SET search_path = <schema>, pg_temp`, exact ACL introspection) is reused, not redesigned.

### 3.2 Direct repository facts

- `backend/src/main/resources/db/migration/` contains only V001, V002 and V003.
- `PostgresContainerIT` has exactly 25 `@Test` methods. It asserts absence of `catalog_order`, the other S2 schemas/roles and every V004+ object in `assertNoV004PlusObjects` (line 5724), calls that helper from `v003SubjectNumberCatalogAndGuardsAreExact` (line 1255), asserts a global exact V001-V003 table set (line 4219) and repeats a no-S2-schema assertion inside `riskOpenClearReopenRevisionProtocol` (lines 6782-6789). All four must evolve; leaving any one creates a post-V004 contradiction.
- The migration method asserts `migrationsExecuted = 3` and three history rows (lines 364-411); it must become four.
- `.github/workflows/ci.yml` asserts the exact 25-name Failsafe set and `tests == len(expected)`, `skipped=0`, `failures=0`, `errors=0`, `cases == expected`.
- `backend/README.md` states 25 methods and "no V004+" repeatedly.
- The Windows worktree `.git` pointer uses a `D:/` path. Read-only Git checks must use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call4` and `GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`; no Git metadata or config may be changed.
- Verified HEAD blob SHA-256: V001 `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`; V002 `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`; V003 `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1`. These are the frozen values.
- The only pre-existing worktree modification is the controller-owned `docs/gateflow/project-execution-control.md`; the implementation author must not touch it.

## 4. First-principles judgment and slice count

V004 is the smallest useful next migration because entitlement issuance needs an immutable paid-order snapshot and verified payment-event parent, while Phase C purchase/activation needs a stable catalog. Creating the four S2 schemas and eight roles now is required by §2.9 and §11.4; creating only the seven `catalog_order` tables avoids implementing publication, consent, IVR, entitlement, payment orchestration or provider integration before their waves and policies are accepted.

**Two slices are the minimum that keeps each checkpoint independently falsifiable.**

1. **S1 - migration, exact catalog and ACL.** Proves the artifact is complete, runnable, exactly the frozen inventory, and that owner/PUBLIC/runtime/cross-module/column/function privileges are exact, with no V005+ object. This is one observable contract boundary: "V004 is present and is exactly the frozen persistence/security contract."
2. **S2 - executable lifecycle, guard, FK and bounded concurrency backstops.** Proves the behaviors the DDL claims: revision/publication lifecycle, immutable snapshots, append-only rows, active-version and identity uniqueness, all V004 FK backstops including the absent consent FK, and bounded publication/order/payment/event races using transient test protocols.

Three slices would split behavior into lifecycle and concurrency checkpoints, but the existing suite already has bounded concurrency helpers (`application_name`, `pg_stat_activity`, latch/blocking detection, timeouts, savepoints) and the behavior fixtures are shared; a third split would add checkpoint overhead without an independent contract boundary. One slice would combine artifact completeness, exact catalog, ACL and all behavior into one large review surface. Splitting by table/file/owner/layer would be mechanical. Therefore **2 slices** comply with the "normally no more than three" principle.

V004 is authored once in S1. S2 adds tests and documentation only; it may not edit V004 unless a controller-approved bounded correction is required by a failing frozen-contract assertion, in which case all prior tests rerun.

## 5. Exact affected paths and allowed changes

| Path | Slice | Change | Explicit boundary |
| --- | --- | --- | --- |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | S1 (authored once) | New complete V004 migration | Only four schemas, eight roles, seven tables, 78 columns, 51 checks, 28 indexes, 11 FKs, five trigger functions, seven triggers and exact grants; no V005+ |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | S1/S2 | S1: rename/evolve migration and absence assertions, extend function test, add two catalog/ACL methods and helpers/constants. S2: add four behavior/concurrency methods and bounded helpers/probe tables. | Same class; no new test class; preserve all 25 prior methods except the explicitly renamed migration method and the explicitly evolved assertions |
| `.github/workflows/ci.yml` | S1/S2 | Update only the Failsafe expected method set/count (25 -> 27 -> 31) and its label/error text | Docker-first, boot smoke, fat-jar firewall, contract jobs, permissions and filters unchanged |
| `backend/README.md` | S1/S2 | Describe V004 inventory/behavior and the 27/31 method profiles | Keep toolchain pins, module table, firewall description and evidence boundary; no production claim |
| `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s1-implementation.md` | S1 | New evidence/completion report | Only S1 evidence |
| `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s2-implementation.md` | S2 | New evidence/completion report | Only S2 evidence |
| `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` | plan | This plan artifact | The only path written by this gate |

Explicitly untouched: V001/V002/V003 SQL, `backend/pom.xml`, application Java (including package/module markers), `docs/12`, `docs/14`, `docs/gateflow/project-execution-control.md`, PA1/PA2/PA3 artifacts, API/UI/protocol/security/deployment/acceptance assets, and every migration other than V004.

## 6. Exact V004 schema and contract

### 6.1 Migration artifact and creation order

- Filename exactly `V004__catalog_order_and_payment.sql`; Flyway version `004`; description `catalog order and payment`.
- The migration runs in Flyway's PostgreSQL transaction. It contains no top-level `COMMIT`, `ROLLBACK`, `BEGIN`, autonomous transaction or isolation change. `SET LOCAL ROLE` is used for object ownership and owner-issued grants. PL/pgSQL bodies use ordinary block `BEGIN`/`END`.
- Execution order (mirrors docs/14 11.4 V004 and the accepted V001-V003 pattern):

1. As the migration identity: `CREATE ROLE` the eight NOLOGIN roles; `CREATE SCHEMA ... AUTHORIZATION <owner>` the four schemas; `REVOKE ALL ON SCHEMA catalog_order, ivr, entitlement_metering, activation_consent FROM PUBLIC`.
2. `SET LOCAL ROLE platform_reliability_owner`: grant `USAGE` on `platform` and `EXECUTE` on the three platform functions to the four S2 runtime roles. `RESET ROLE`.
3. `SET LOCAL ROLE audit_owner`: grant `USAGE` on `audit` and `EXECUTE` on `audit.append_fact` to the four S2 runtime roles. `RESET ROLE`.
4. `SET LOCAL ROLE catalog_order_owner`: create `catalog_product` -> `catalog_version` -> `catalog_offer` -> `catalog_benefit_line` -> `customer_order` -> `payment_order` -> `payment_event`, with their inline PK/unique/check constraints and explicit indexes. Create the five trigger functions and seven triggers. Apply the exact table/column/function grants and schema `USAGE`. `RESET ROLE`.
5. Owner-issued cross-module grants: `SET LOCAL ROLE customer_iam_owner`, `organization_access_owner`, `subject_number_owner` for the exact `catalog_order_role` reads and schema `USAGE`; `SET LOCAL ROLE activation_consent_owner` and `entitlement_metering_owner` for the exact S2 schema `USAGE` matrix. `RESET ROLE` after each.
6. As the migration identity: add the 11 FKs with exact `ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY ... ON UPDATE RESTRICT ON DELETE RESTRICT`, in an order where every parent exists (all parents precede the ALTER phase).
- No other object is created. No `issue_first_trial`, no V005+ table, no future owner capability.
- Tables, functions, triggers and indexes are owned by `catalog_order_owner`; constraints added by the migration identity remain constraints of the table owner. This mirrors V003; Q17/Q23 production provisioning is not claimed.
- `SET LOCAL ROLE` is owner-issued only within the migration transaction; no role membership or `LOGIN` attribute is granted.

### 6.2 Tables

All `id` columns are `uuid NOT NULL` with no default and no sequence. Text state values are `text` with named checks. Times are `timestamptz`. Money is `bigint` cents; currency is three uppercase ISO-4217 characters. `jsonb` is used only for canonical snapshots. `docs/14` 4.16-4.22 is the predicate authority; the restatement below is for generation convenience and must match exactly.

#### 6.2.1 `catalog_order.catalog_product` (4 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_catalog_product` |
| `product_code` | text | NO | none | UNIQUE `uq_catalog_product_code`; `ck_catalog_product_code` |
| `product_family` | text | NO | none | `ck_catalog_product_family` |
| `created_at` | timestamptz | NO | `now()` | creation time |

Checks: `ck_catalog_product_code` (`product_code ~ '^[A-Z][A-Z0-9_-]{1,63}$'`), `ck_catalog_product_family` (`product_family ~ '^[A-Z][A-Z0-9_]{0,63}$'`).

Keys/indexes: PK; UNIQUE `uq_catalog_product_code (product_code)`; `ix_catalog_product_family (product_family, created_at)`.

Append-only: `trg_catalog_product_immutable` uses `catalog_order.reject_row_mutation()` on `BEFORE UPDATE OR DELETE`. Runtime `SELECT`/`INSERT` only.

#### 6.2.2 `catalog_order.catalog_version` (17 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_catalog_version` |
| `product_id` | uuid | NO | none | FK `fk_catalog_version_product`; part of `uq_catalog_version_no`; part of partial `uq_catalog_version_active` |
| `version_no` | integer | NO | none | `ck_catalog_version_no`; part of `uq_catalog_version_no` |
| `sale_status` | text | NO | `'DRAFT'` | `ck_catalog_version_sale_status` |
| `offer_type` | text | NO | none | `ck_catalog_version_offer_type` |
| `audience` | text | NO | none | `ck_catalog_version_audience`; `ck_catalog_version_audience_shape` |
| `price_cents` | bigint | YES | none | `ck_catalog_version_price` |
| `currency` | text | NO | none | `ck_catalog_version_currency` |
| `billing_cycle` | text | NO | none | `ck_catalog_version_billing_matrix` |
| `installments` | integer | NO | none | `ck_catalog_version_billing_matrix` |
| `grant_interval_months` | integer | NO | none | `ck_catalog_version_billing_matrix` |
| `snapshot` | jsonb | NO | none | `ck_catalog_version_snapshot_type` |
| `snapshot_sha256` | text | NO | none | `ck_catalog_version_snapshot_sha` |
| `created_by_employee_id` | uuid | NO | none | FK `fk_catalog_version_employee` |
| `created_at` | timestamptz | NO | `now()` | draft creation time |
| `published_at` | timestamptz | YES | none | `ck_catalog_version_published_shape` |
| `revision` | integer | NO | `1` | `ck_catalog_version_revision` |

Keys/indexes: PK; UNIQUE `uq_catalog_version_no (product_id, version_no)`; partial UNIQUE `uq_catalog_version_active (product_id) WHERE sale_status = 'ACTIVE'`; `ix_catalog_version_product_status (product_id, sale_status, version_no DESC)`.

Guard: `trg_catalog_version_guard` (`BEFORE UPDATE`) uses `catalog_order.guard_catalog_version_published()`: once `OLD.published_at IS NOT NULL`, reject any change to `product_id`, `version_no`, `offer_type`, `audience`, `price_cents`, `currency`, `billing_cycle`, `installments`, `grant_interval_months`, `snapshot`, `snapshot_sha256`, `created_by_employee_id`, `created_at` or `published_at`; allow `sale_status` and `revision`. Before publication, draft content and `sale_status` use conditional revision updates.

#### 6.2.3 `catalog_order.catalog_offer` (7 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_catalog_offer` |
| `catalog_version_id` | uuid | NO | none | FK `fk_catalog_offer_version`; part of `uq_catalog_offer_version_channel` |
| `channel` | text | NO | none | `ck_catalog_offer_channel`; part of `uq_catalog_offer_version_channel` |
| `offer_status` | text | NO | `'ACTIVE'` | `ck_catalog_offer_status` |
| `version_token` | text | NO | none | `ck_catalog_offer_token` |
| `created_at` | timestamptz | NO | `now()` | creation time |
| `revision` | integer | NO | `1` | `ck_catalog_offer_revision` |

Keys/indexes: PK; UNIQUE `uq_catalog_offer_version_channel (catalog_version_id, channel)`; `ix_catalog_offer_channel_status (channel, offer_status)`.

Guard: `trg_catalog_offer_guard` (`BEFORE UPDATE`) uses `catalog_order.guard_catalog_offer()`: reject changes to `catalog_version_id` or `channel`; allow `offer_status`, `version_token`, `revision`.

#### 6.2.4 `catalog_order.catalog_benefit_line` (6 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_catalog_benefit_line` |
| `catalog_version_id` | uuid | NO | none | FK `fk_catalog_benefit_version`; part of `uq_catalog_benefit_version_type` |
| `entitlement_type` | text | NO | none | `ck_catalog_benefit_type`; part of `uq_catalog_benefit_version_type` |
| `quantity` | bigint | NO | none | `ck_catalog_benefit_quantity` |
| `validity_days` | integer | NO | none | `ck_catalog_benefit_validity` |
| `grant_interval_months` | integer | NO | none | `ck_catalog_benefit_interval` |

Keys/indexes: PK; UNIQUE `uq_catalog_benefit_version_type (catalog_version_id, entitlement_type)`; `ix_catalog_benefit_version (catalog_version_id)`.

Append-only: `trg_catalog_benefit_line_immutable` uses `catalog_order.reject_row_mutation()` on `BEFORE UPDATE OR DELETE`. Runtime `SELECT`/`INSERT` only.

#### 6.2.5 `catalog_order.customer_order` (21 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_customer_order` |
| `order_no` | text | NO | none | UNIQUE `uq_customer_order_no`; `ck_customer_order_no` |
| `account_id` | uuid | NO | none | FK `fk_customer_order_account`; part of `uq_customer_order_idempotency` |
| `owner_organization_id` | uuid | YES | none | FK `fk_customer_order_organization` |
| `subject_id` | uuid | NO | none | FK `fk_customer_order_subject` |
| `channel` | text | NO | none | `ck_customer_order_channel` |
| `catalog_version_id` | uuid | NO | none | FK `fk_customer_order_version` |
| `catalog_offer_id` | uuid | NO | none | FK `fk_customer_order_offer` |
| `purchase_consent_id` | uuid | NO | none | NOT NULL; **no FK in V004**; `fk_customer_order_consent` added by `ALTER` in V007 |
| `amount_cents` | bigint | NO | none | `ck_customer_order_amount` |
| `currency` | text | NO | none | `ck_customer_order_currency` |
| `payment_status` | text | NO | `'UNPAID'` | `ck_customer_order_payment_status` |
| `snapshot` | jsonb | NO | none | `ck_customer_order_snapshot_type` |
| `snapshot_sha256` | text | NO | none | `ck_customer_order_snapshot_sha` |
| `version_token` | text | NO | none | `ck_customer_order_version_token` |
| `idempotency_key` | text | NO | none | `ck_customer_order_idempotency`; part of `uq_customer_order_idempotency` |
| `request_fingerprint` | text | NO | none | `ck_customer_order_fingerprint` |
| `revision` | integer | NO | `1` | `ck_customer_order_revision` |
| `created_at` | timestamptz | NO | `now()` | creation time |
| `paid_at` | timestamptz | YES | none | `ck_customer_order_payment_shape`; `ck_customer_order_time_order` |
| `closed_at` | timestamptz | YES | none | `ck_customer_order_payment_shape`; `ck_customer_order_time_order` |

Keys/indexes: PK; UNIQUE `uq_customer_order_no (order_no)`; UNIQUE `uq_customer_order_idempotency (account_id, idempotency_key)`; `ix_customer_order_subject_status (subject_id, payment_status, created_at DESC)`; `ix_customer_order_organization (owner_organization_id, created_at DESC)`; `ix_customer_order_version (catalog_version_id)`.

Guard: `trg_customer_order_guard` (`BEFORE UPDATE`) uses `catalog_order.guard_customer_order_snapshot()`: reject changes to `id`, `order_no`, `account_id`, `owner_organization_id`, `subject_id`, `channel`, `catalog_version_id`, `catalog_offer_id`, `purchase_consent_id`, `amount_cents`, `currency`, `snapshot`, `snapshot_sha256`, `version_token`, `idempotency_key`, `request_fingerprint`, `created_at`; allow only `payment_status`, `paid_at`, `closed_at`, `revision`. No entitlement-status column exists.

#### 6.2.6 `catalog_order.payment_order` (11 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_payment_order` |
| `order_id` | uuid | NO | none | UNIQUE `uq_payment_order_order`; FK `fk_payment_order_order` |
| `provider` | text | NO | none | `ck_payment_order_provider` |
| `merchant_order_no` | text | NO | none | UNIQUE `uq_payment_order_merchant`; `ck_payment_order_merchant_no` |
| `provider_transaction_id` | text | YES | none | partial UNIQUE `uq_payment_order_transaction`; `ck_payment_order_transaction` |
| `status` | text | NO | `'UNPAID'` | `ck_payment_order_status`; `ck_payment_order_paid_shape` |
| `amount_cents` | bigint | NO | none | `ck_payment_order_amount` |
| `currency` | text | NO | none | `ck_payment_order_currency` |
| `created_at` | timestamptz | NO | `now()` | creation time |
| `updated_at` | timestamptz | NO | `now()` | `ck_payment_order_time_order` |
| `revision` | integer | NO | `1` | `ck_payment_order_revision` |

Keys/indexes: PK; UNIQUE `uq_payment_order_order (order_id)`; UNIQUE `uq_payment_order_merchant (merchant_order_no)`; partial UNIQUE `uq_payment_order_transaction (provider_transaction_id) WHERE provider_transaction_id IS NOT NULL`; `ix_payment_order_status_time (status, updated_at)`.

Guard: `trg_payment_order_guard` (`BEFORE UPDATE`) uses `catalog_order.guard_payment_order_identity()`: reject changes to `order_id`, `provider`, `merchant_order_no`, `amount_cents`, `currency`, `created_at`; allow `status`, `provider_transaction_id`, `updated_at`, `revision`.

#### 6.2.7 `catalog_order.payment_event` (12 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_payment_event` |
| `payment_order_id` | uuid | NO | none | FK `fk_payment_event_order` |
| `provider` | text | NO | none | `ck_payment_event_provider`; part of `uq_payment_event_provider_event` |
| `provider_event_id` | text | NO | none | `ck_payment_event_event_id`; part of `uq_payment_event_provider_event` |
| `event_type` | text | NO | none | `ck_payment_event_type` |
| `signature_verified` | boolean | NO | `false` | only verified events may advance payment (service rule) |
| `payload_ciphertext` | text | NO | none | `ck_payment_event_payload_ciphertext` |
| `payload_sha256` | text | NO | none | `ck_payment_event_payload_sha` |
| `transaction_id` | text | YES | none | `ck_payment_event_transaction` |
| `amount_cents` | bigint | NO | none | `ck_payment_event_amount` |
| `occurred_at` | timestamptz | NO | none | provider event time; audit only |
| `received_at` | timestamptz | NO | `now()` | server receipt time |

Keys/indexes: PK; UNIQUE `uq_payment_event_provider_event (provider, provider_event_id)`; `ix_payment_event_order_time (payment_order_id, received_at DESC)`; partial `ix_payment_event_verified (payment_order_id, received_at) WHERE signature_verified`.

Append-only: `trg_payment_event_immutable` uses `catalog_order.reject_row_mutation()` on `BEFORE UPDATE OR DELETE`. Runtime `SELECT`/`INSERT` only. Duplicate `(provider, provider_event_id)` is a DB unique backstop; first-result replay is service-owned.

### 6.3 Catalog totals reconciliation

- Column order per table (exact): product `id,product_code,product_family,created_at`; version `id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id,created_at,published_at,revision`; offer `id,catalog_version_id,channel,offer_status,version_token,created_at,revision`; benefit `id,catalog_version_id,entitlement_type,quantity,validity_days,grant_interval_months`; customer_order `id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint,revision,created_at,paid_at,closed_at`; payment_order `id,order_id,provider,merchant_order_no,provider_transaction_id,status,amount_cents,currency,created_at,updated_at,revision`; payment_event `id,payment_order_id,provider,provider_event_id,event_type,signature_verified,payload_ciphertext,payload_sha256,transaction_id,amount_cents,occurred_at,received_at`.
- Defaulted columns (16): product `created_at`; version `sale_status='DRAFT'`, `created_at=now()`, `revision=1`; offer `offer_status='ACTIVE'`, `created_at=now()`, `revision=1`; benefit none; customer_order `payment_status='UNPAID'`, `created_at=now()`, `revision=1`; payment_order `status='UNPAID'`, `created_at=now()`, `updated_at=now()`, `revision=1`; payment_event `signature_verified=false`, `received_at=now()`.
- No `id` default and no sequence in any S2 schema.

### 6.4 Foreign keys (11 V004 + 1 deferred)

All 11 are ordinary immediate constraints, `ON UPDATE RESTRICT ON DELETE RESTRICT`, `condeferrable=false`, `condeferred=false`, `convalidated=true`.

| # | Constraint | Child column | Parent |
| --- | --- | --- | --- |
| 1 | `fk_catalog_version_product` | `catalog_version.product_id` | `catalog_order.catalog_product(id)` |
| 2 | `fk_catalog_version_employee` | `catalog_version.created_by_employee_id` | `operations_iam.employee_account(id)` |
| 3 | `fk_catalog_offer_version` | `catalog_offer.catalog_version_id` | `catalog_order.catalog_version(id)` |
| 4 | `fk_catalog_benefit_version` | `catalog_benefit_line.catalog_version_id` | `catalog_order.catalog_version(id)` |
| 5 | `fk_customer_order_account` | `customer_order.account_id` | `customer_iam.customer_account(id)` |
| 6 | `fk_customer_order_organization` | `customer_order.owner_organization_id` | `organization_access.organization(id)` |
| 7 | `fk_customer_order_subject` | `customer_order.subject_id` | `subject_number.business_subject(id)` |
| 8 | `fk_customer_order_version` | `customer_order.catalog_version_id` | `catalog_order.catalog_version(id)` |
| 9 | `fk_customer_order_offer` | `customer_order.catalog_offer_id` | `catalog_order.catalog_offer(id)` |
| 10 | `fk_payment_order_order` | `payment_order.order_id` | `catalog_order.customer_order(id)` |
| 11 | `fk_payment_event_order` | `payment_event.payment_order_id` | `catalog_order.payment_order(id)` |
| deferred | `fk_customer_order_consent` | `customer_order.purchase_consent_id` | `activation_consent.consent_record(id)`; added by `ALTER` in V007 |

No V004 FK references a V005+ table. `purchase_consent_id` is NOT NULL but has no FK, no default and no check beyond NOT NULL in V004.

### 6.5 Functions and triggers (exact names, security and bodies)

All five functions are `LANGUAGE plpgsql`, `SECURITY INVOKER`, owned by `catalog_order_owner`, with `SET search_path = catalog_order, pg_temp`. They contain no transaction control, no `gen_random_uuid`, no `p_id` argument and are not in the function-wave registry.

| Function | Trigger(s) | Behavior |
| --- | --- | --- |
| `catalog_order.reject_row_mutation()` | `trg_catalog_product_immutable`, `trg_catalog_benefit_line_immutable`, `trg_payment_event_immutable` | Always `RAISE EXCEPTION 'CATALOG_ROW_APPEND_ONLY' USING ERRCODE='P0001'`. Trigger events: `BEFORE UPDATE OR DELETE`, `FOR EACH ROW`. |
| `catalog_order.guard_catalog_version_published()` | `trg_catalog_version_guard` | If `OLD.published_at IS NOT NULL`, reject changes to the 14 published/immutable columns listed in 6.2.2 with `CATALOG_VERSION_PUBLISHED_IMMUTABLE`/`P0001`; allow `sale_status`/`revision`. Trigger: `BEFORE UPDATE`, `FOR EACH ROW`. |
| `catalog_order.guard_catalog_offer()` | `trg_catalog_offer_guard` | Reject changes to `catalog_version_id` or `channel` with `CATALOG_OFFER_RELATION_IMMUTABLE`/`P0001`. Trigger: `BEFORE UPDATE`, `FOR EACH ROW`. |
| `catalog_order.guard_customer_order_snapshot()` | `trg_customer_order_guard` | Reject changes to the 17 immutable columns listed in 6.2.5 with `CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE`/`P0001`; allow only payment status/terminal time/revision. Trigger: `BEFORE UPDATE`, `FOR EACH ROW`. |
| `catalog_order.guard_payment_order_identity()` | `trg_payment_order_guard` | Reject changes to `order_id`, `provider`, `merchant_order_no`, `amount_cents`, `currency` or `created_at` with `PAYMENT_ORDER_IDENTITY_IMMUTABLE`/`P0001`; allow status/transaction/updated_at/revision. Trigger: `BEFORE UPDATE`, `FOR EACH ROW`. |

Exact trigger set and flags (`tgtype &` checks; `1` = true):

| Trigger | Table | Function | BEFORE | ROW | UPDATE | DELETE | INSERT | TRUNCATE |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `trg_catalog_product_immutable` | `catalog_product` | `reject_row_mutation()` | 1 | 1 | 1 | 1 | 0 | 0 |
| `trg_catalog_version_guard` | `catalog_version` | `guard_catalog_version_published()` | 1 | 1 | 1 | 0 | 0 | 0 |
| `trg_catalog_offer_guard` | `catalog_offer` | `guard_catalog_offer()` | 1 | 1 | 1 | 0 | 0 | 0 |
| `trg_catalog_benefit_line_immutable` | `catalog_benefit_line` | `reject_row_mutation()` | 1 | 1 | 1 | 1 | 0 | 0 |
| `trg_customer_order_guard` | `customer_order` | `guard_customer_order_snapshot()` | 1 | 1 | 1 | 0 | 0 | 0 |
| `trg_payment_order_guard` | `payment_order` | `guard_payment_order_identity()` | 1 | 1 | 1 | 0 | 0 | 0 |
| `trg_payment_event_immutable` | `payment_event` | `reject_row_mutation()` | 1 | 1 | 1 | 1 | 0 | 0 |

No other non-internal trigger exists in `catalog_order`.

### 6.6 Ownership, grants and ACL matrix

#### 6.6.1 Schema owners and `USAGE`/`CREATE`

| Schema | Owner | `USAGE` at V004 | `CREATE` |
| --- | --- | --- | --- |
| `catalog_order` | `catalog_order_owner` | `catalog_order_owner`, `catalog_order_role`, `entitlement_metering_role` | owner only |
| `ivr` | `ivr_owner` | `ivr_owner` only; **`ivr_role` is deferred to V005** | owner only |
| `entitlement_metering` | `entitlement_metering_owner` | owner, `entitlement_metering_role`, `activation_consent_role`, `ivr_role` (**`catalog_order_role` is deferred to V006**) | owner only |
| `activation_consent` | `activation_consent_owner` | owner, `activation_consent_role`, `ivr_role`, `catalog_order_role` | owner only |
| `customer_iam` | `customer_iam_owner` | existing S1 roles plus `catalog_order_role` | owner only |
| `organization_access` | `organization_access_owner` | existing S1 roles plus `catalog_order_role` | owner only |
| `subject_number` | `subject_number_owner` | existing S1 roles plus `catalog_order_role` | owner only |
| `platform` | `platform_reliability_owner` | existing S1 roles plus the four S2 runtime roles | owner only |
| `audit` | `audit_owner` | existing S1 roles plus the four S2 runtime roles | owner only |

No S2 runtime role receives `CREATE` on any schema. `ivr_role` has no `USAGE` on `catalog_order`, `customer_iam`, `organization_access`, `subject_number` or `ivr` at V004; those first-read grants are V005. `activation_consent_role` has no `USAGE` on `subject_number`, `customer_iam` or `organization_access` at V004; those are V007. `catalog_order_role` has no `USAGE` on `ivr` or `entitlement_metering` at V004 (its first `entitlement_metering` `USAGE` is V006); its `activation_consent` `USAGE` is granted at V004. `entitlement_metering_role` has no `USAGE` on `customer_iam`, `organization_access` or `subject_number` at V004; the subject read is V006.

#### 6.6.2 Owned-table column grants (`catalog_order_role`)

`REVOKE ALL` on all seven tables and five functions from `PUBLIC`. Table-level `SELECT` and `INSERT` for `catalog_order_role` on all seven; `UPDATE` is column-level only:

| Table | `SELECT` | `INSERT` | `UPDATE` columns |
| --- | --- | --- | --- |
| `catalog_product` | table-level | table-level | none |
| `catalog_version` | table-level | table-level | `sale_status`, `offer_type`, `audience`, `price_cents`, `currency`, `billing_cycle`, `installments`, `grant_interval_months`, `snapshot`, `snapshot_sha256`, `published_at`, `revision` |
| `catalog_offer` | table-level | table-level | `offer_status`, `version_token`, `revision` |
| `catalog_benefit_line` | table-level | table-level | none |
| `customer_order` | table-level | table-level | `payment_status`, `paid_at`, `closed_at`, `revision` |
| `payment_order` | table-level | table-level | `status`, `provider_transaction_id`, `updated_at`, `revision` |
| `payment_event` | table-level | table-level | none |

Critical ACL semantics: `has_table_privilege(..., 'UPDATE')` must be false for every table; `has_column_privilege(..., 'UPDATE')` must be true only for the listed columns and false for every other column. `has_table_privilege(..., 'DELETE'|'TRUNCATE'|'REFERENCES'|'TRIGGER')` must be false for every runtime role.

#### 6.6.3 Cross-module reads (exact columns; read-only, no row-lock privilege)

| Grantee | Table | Exact `SELECT` columns | Wave |
| --- | --- | --- | --- |
| `catalog_order_role` | `customer_iam.customer_account` | `id`, `status` | V004 |
| `catalog_order_role` | `customer_iam.identity_profile` | `account_id`, `real_name_status` | V004 |
| `catalog_order_role` | `organization_access.organization` | `id`, `status` | V004 |
| `catalog_order_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` | V004 |
| `catalog_order_role` | `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` | V004 |
| `entitlement_metering_role` | `catalog_order.customer_order` | `id`, `account_id`, `owner_organization_id`, `subject_id`, `catalog_version_id`, `snapshot`, `snapshot_sha256`, `payment_status`, `currency`, `created_at` | V004 |
| `entitlement_metering_role` | `catalog_order.payment_order` | `id`, `order_id`, `status`, `amount_cents`, `currency`, `updated_at` | V004 |
| `entitlement_metering_role` | `catalog_order.payment_event` | `id`, `payment_order_id`, `provider`, `provider_event_id`, `event_type`, `signature_verified`, `amount_cents`, `occurred_at` | V004 |

No other S2 role receives a table or column privilege on a V004 or S1 table. `ivr_role` receives no `catalog_order` or S1 table privilege at V004. `catalog_order_role` receives no `activation_consent` or `entitlement_metering` table privilege at V004 (those tables do not exist); its future reads are V006/V007. No cross-module `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, `REFERENCES` or row-lock privilege is granted.

#### 6.6.4 Function `EXECUTE`

| Function | `EXECUTE` |
| --- | --- |
| `platform.idempotency_begin(text,text,text)` | `catalog_order_role`, `ivr_role`, `entitlement_metering_role`, `activation_consent_role` |
| `platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)` | same four |
| `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` | same four |
| `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)` | same four |
| `catalog_order.guard_catalog_version_published()` | `catalog_order_role` only |
| `catalog_order.guard_catalog_offer()` | `catalog_order_role` only |
| `catalog_order.guard_customer_order_snapshot()` | `catalog_order_role` only |
| `catalog_order.guard_payment_order_identity()` | `catalog_order_role` only |
| `catalog_order.reject_row_mutation()` | owner-inherent only; no runtime role; no PUBLIC |
| `entitlement_metering.issue_first_trial(...)` | absent at V004; created in V006 with `EXECUTE` only to `activation_consent_role`/`entitlement_metering_role` |

`REVOKE ALL ON FUNCTION ... FROM PUBLIC` for all five V004 functions. `PUBLIC` receives no schema, table, column or function privilege. The four S1 functions and the four guard functions are the only function grants introduced in V004. No `catalog_order_owner`, `ivr_owner`, `entitlement_metering_owner` or `activation_consent_owner` receives a runtime `EXECUTE` grant; `entitlement_metering_owner` receives **no** `platform` `USAGE` and **no** `platform.outbox_append` `EXECUTE` at V004 (that internal owner capability is V006).

#### 6.6.5 Denials to assert

- `PUBLIC` zero on the four S2 schemas; zero `pg_class` table ACL; zero `pg_proc` function ACL; zero `pg_attribute.attacl` on every non-dropped user column of the seven V004 tables and the five S1 tables touched by V004 grants.
- No runtime role has `CREATE` on any schema; no `DELETE`/`TRUNCATE`/`REFERENCES`/`TRIGGER` on any table; no cross-module write; no `FOR UPDATE`/`FOR SHARE` path (no `UPDATE` privilege on a foreign table).
- `ivr_role` has no `ivr`/`catalog_order`/S1 table privilege and no schema `USAGE` on `catalog_order`, `customer_iam`, `organization_access`, `subject_number` or `ivr` at V004.
- `activation_consent_role` has no S1 table privilege or schema `USAGE` on `subject_number`, `customer_iam` or `organization_access` at V004.
- `entitlement_metering_role` has no S1 table privilege or schema `USAGE` on `customer_iam`, `organization_access` or `subject_number` at V004.
- `catalog_order_role` has no `USAGE` on `ivr` and no `USAGE` on `entitlement_metering` at V004 (its first `USAGE` on that schema is V006); it has no table privilege on any `activation_consent` or `entitlement_metering` table (those tables do not exist). Its `activation_consent` schema `USAGE` is granted at V004 and is not a denial.
- `entitlement_metering_owner` has no `platform` `USAGE`/`outbox_append` `EXECUTE`; no `issue_first_trial`; no V005+ role (`evidence_owner`, `evidence_role`, `sharing_owner`, `sharing_role`).

### 6.7 Database guarantees vs transient test protocol vs deferred services

| Contract | V004 DDL | Transient test protocol | Deferred owner |
| --- | --- | --- | --- |
| Types/nullability/defaults/status/format/revision/time/shape checks | Yes | Exact normalized `pg_get_constraintdef` inventory plus one `23514`+name probe per check | Backend/Database |
| PK, ordinary unique, partial unique, partial indexes | Yes | Duplicate/active-version/provider-event/idempotency probes | Backend/Database |
| 11 FK targets/actions/immediacy; absent V007 FK | Yes | Missing-parent `23503` and parent-delete RESTRICT probes; `fk_customer_order_consent` absent; NOT NULL consent column | Backend/Database; V007 |
| Immutable snapshots and immutable relations | Yes (guards + column ACL) | Guard `P0001` probes and allowed-update probes | catalog-order service |
| Append-only product/benefit/payment-event | Yes (triggers + no update/delete ACL) | Runtime `42501`, owner `P0001`, and append-only visibility probes | catalog-order service |
| Catalog version sale lifecycle and publication serialization | Partly (checks/partial unique/guard) | Conditional revision updates; retire-then-activate protocol under `pg_advisory_xact_lock(hashtextextended('catalog_order.publication:<product_id>', 0))`; immediate-partial-unique loser `23505` | `catalog-order.publish_catalog_version`/`offsale_catalog_version` (application service, §9.26) |
| Order creation and PURCHASE idempotency | No (only `uq_customer_order_idempotency`, snapshot columns) | Same-key insert race → one `23505` winner; no order/consent/idempotency transaction simulated | Order service + `platform.idempotency_*` + `activation-consent.append_purchase_consent` (§9.17) |
| Purchase consent | No | Not simulated; only the NOT NULL column and absent FK are asserted | `activation_consent.append_purchase_consent`; FK in V007 |
| Payment callback/status advancement | No (only status shapes and identity uniqueness) | Status/transaction shape probes; duplicate-event unique probe | Payment adapter + order service (§9.18); Q6 provider payload |
| Entitlement issuance and provider orchestration | No | Not simulated | V006 issuance service; Q20; provider integration |
| Catalog offer withdrawal | No | Not simulated | Product/Operations/catalog-order; OQ-4 |
| Retention/deletion policy | No hard delete; append-only/status only | No retention behavior claimed | Legal/Security (Q3) |

The test suite must never present a transient SQL protocol as an application service. No SQL service function is created in V004; the only functions are the five local `SECURITY INVOKER` trigger functions.

### 6.8 Absence boundaries

- No V005+ table in `ivr`, `entitlement_metering` or `activation_consent`; `evidence` and `sharing` schemas do not exist.
- No `issue_first_trial` anywhere.
- No `activation_consent.consent_record`, `service_activation` or `service_activation_transition`; no V006 entitlement tables.
- No `fk_customer_order_consent`; no V007 `ALTER`.
- No V005+ role; no role membership; no `LOGIN`/superuser/createdb/createrole.
- No forward grant at V004: `ivr_role` S1/ivr grants, `activation_consent_role` S1 grants, `entitlement_metering_role` subject grants, and `catalog_order_role` `entitlement_metering` schema `USAGE` plus all `catalog_order_role` entitlement/activation table and column reads are absent. `catalog_order_role` `activation_consent` schema `USAGE` is granted at V004; its `entitlement_metering` schema `USAGE` and reads are V006.
- No owner platform capability for `entitlement_metering_owner`.
- No id default, sequence or business sequence.
- No extra table, column, check, index, function, trigger, sequence, schema, role or grant beyond this plan.

## 7. Behavioral implementation slices

Every slice leaves the V001+V002+V003+V004 prefix runnable and fail-closed. No application-service or V005+ work may enter a slice.

### 7.1 S1 - V004 applies after V003; exact catalog and ACL

**Objective/outcome.** Prove the V004 artifact is complete, runnable, exactly the frozen inventory and ACL, with V001-V003 byte-identical and no V005+ object.

**Prerequisites.** Accepted PA1-PA3 persistence foundation on merged base `81022037dc5dced625788477835472a84a16d52d`; V001/V002/V003 HEAD hashes as recorded in 3.2; Docker-capable PostgreSQL 16; current 25-method suite green; clean worktree except the controller edit.

**Allowed files.**

- `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` (new; authored once).
- `PostgresContainerIT.java` (S1 changes below).
- `.github/workflows/ci.yml` (expected set/count 27 and V004 label).
- `backend/README.md` (S1 description and count 27).
- `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s1-implementation.md` (new).

**Exact DDL changes.** Implement section 6 in one migration:

- Roles/schemas/PUBLIC revoke in 6.1 step 1.
- Platform/audit `USAGE` + four S1 function `EXECUTE` to the four S2 runtime roles (6.6.1, 6.6.4).
- Seven tables in 6.1 step 4 order, with the exact columns/checks/keys/indexes in 6.2 and 6.3.
- Five `SECURITY INVOKER` functions and seven triggers in 6.5, with the exact search path and error codes.
- `REVOKE ALL ... FROM PUBLIC` on all seven tables and five functions; owned-table ACL in 6.6.2; cross-module reads in 6.6.3; guard `EXECUTE` in 6.6.4; schema `USAGE` in 6.6.1.
- Owner-issued cross-module grants from `customer_iam_owner`, `organization_access_owner`, `subject_number_owner`, `activation_consent_owner`, `entitlement_metering_owner`; `RESET ROLE` after each owner block.
- 11 exact `ALTER TABLE ... ADD CONSTRAINT` FKs after `RESET ROLE`.
- No other statement.

**Exact test/document changes.**

1. Rename `v001ThenV002ThenV003MigrateCleanPostgres16WithoutSkipping` to `v001ThroughV004MigrateCleanPostgres16WithoutSkipping`; assert `migrationsExecuted=4`; add the V004 history row (`version=004`, description `catalog order and payment`, script `V004__catalog_order_and_payment.sql`, success true); preserve `validate()`/repeat-migrate-zero and the V001/V002/V003 smoke calls.
2. In `assertTablesColumnsChecksAndIndexes` (line 4219), restrict the exact global table-set query to the six S1 schemas and keep `ALL_V001_V003_TABLES`; leave the `public` schema assertion unchanged. The V004 method separately asserts the four S2 schemas.
3. Replace `assertNoV004PlusObjects` (line 5724) with `assertNoV005PlusObjects`: `evidence`/`sharing` schemas absent; `ivr`, `entitlement_metering`, `activation_consent` contain zero tables; `issue_first_trial` count zero; `evidence_owner`, `evidence_role`, `sharing_owner`, `sharing_role` absent; legacy `ivr.call_task`/`ivr.call_session` and V005 `ivr.*` core tables absent.
4. Remove the `assertNoV004PlusObjects(connection)` call from `v003SubjectNumberCatalogAndGuardsAreExact` (line 1255).
5. Evolve the no-S2-schema block in `riskOpenClearReopenRevisionProtocol` (lines 6782-6789): assert `ivr`, `entitlement_metering` and `activation_consent` exist with zero tables; assert `evidence`/`sharing` absent; assert `to_regclass('ivr.call_task')`, `ivr.call_session`, `ivr.outbound_call_task`, `ivr.inbound_session`, `ivr.call_record`, `ivr.call_leg`, `ivr.call_event` and `ivr.ivr_decision` are null. Change the label from "no dialing/IVR schema" to "no IVR table".
6. Extend `functionsHaveFixedSearchPathAndNoTransactionControl`: add the exact five `catalog_order` functions with `prosecdef=false`, owner `catalog_order_owner`, search_path `catalog_order, pg_temp`; include them in the transaction-control query; assert no `gen_random_uuid` in them; update the `issue_first_trial` label to "absent from V001-V005".
7. Add `V004_TABLES`, `V004_CHECK_DEF_MAP`, `V004_PK_UNIQUE_DEFS`, `V004_FK_DEFS`, `V004_INDEX_DEFS` constants and helpers `assertV004TablesColumnsChecksIndexesFks`, `assertV004CheckPredicateInventory`, `assertV004FunctionsAndTriggersExact`, `assertV004NoIdDefaultsOrSequences`, `assertV004SchemaUsageMatrix`, `assertV004OwnedAndCrossModuleGrants`, `assertV004PublicAclIsZero`, `assertV004GuardFunctionExecuteBoundaries`.
8. Add exactly two new methods:
   - `v004CatalogOrderInventoryAndGuardsAreExact`: exact seven tables/owners; 78 ordered column rows with types/nullability/defaults; 51 checks (exact names and whole normalized `pg_get_constraintdef` equality); seven PKs, nine ordinary uniques and two partial unique indexes (11 keys); 28 exact `indexdef` values; 11 FK defs/properties and `fk_customer_order_consent` absence; `purchase_consent_id` NOT NULL/no default/no FK; five functions; seven triggers; zero id defaults/sequences; eight roles/schema owners; `assertNoV005PlusObjects`.
   - `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact`: exact schema `USAGE`/`CREATE`; exact owned-table `SELECT`/`INSERT`/column `UPDATE`; exact cross-module column `SELECT`; exact function `EXECUTE`; PUBLIC zero on schema/table/function/column; all denials in 6.6.5.
9. Update the CI expected set to exactly 27 names and the label/error text to V004.
10. Update README to V004 S1: four schemas/eight roles/seven tables/78 columns/51 checks/28 indexes/11 FKs/5 functions/7 triggers; no V005+; 27 methods; V001-V003 hashes preserved.

**Invariants/state transitions/error handling.**

- V001/V002/V003 HEAD blobs and CR-normalized diffs unchanged.
- V004 is the only new object source; no V005+ object, no `issue_first_trial`, no V007 FK.
- No application Java/POM/runtime DataSource/Flyway; no new test class.
- All prior 25 methods remain behaviorally identical except the explicitly renamed migration method and the evolved absence/global-table/function assertions.
- Any unexpected catalog mismatch is fixed only inside V004; any need to change V001-V003, a frozen predicate or a grant not in §10.12 stops the slice.

**Non-goals.** No data/lifecycle/concurrency behavior method in S1; no publication/order/payment service; no V005+ object.

**Validation commands/assertions.** Section 8.6:
- `./mvnw -B -ntp validate` and `./mvnw -B -ntp clean verify` → 10 surefire tests, no skip.
- `docker info`; `./mvnw -B -ntp -Pcontainer-smoke verify` → exactly 27 methods, `tests=27`, `skipped=0 failures=0 errors=0`, exact set.
- Exact catalog/ACL assertions in 8.2/8.3; V001-V003 hashes; V004 SHA-256 recorded; whitespace and status checks.

**Completion signal.** All S1 commands pass; the S1 evidence artifact records exact environment/outputs/hashes and the exact changed paths; only allowed files changed.

**Stop condition.** Any need to edit V001/V002/V003 or the controller, add a V005+ object or `issue_first_trial`, add the V007 FK, change a frozen predicate, add a grant outside §10.12/§11.4, modify application code/POM, or weaken an assertion.

### 7.2 S2 - Executable lifecycle, guard, FK and bounded concurrency backstops

**Objective/outcome.** Prove the database-visible V004 behavior and the bounded transient protocols required by the frozen success signals, without claiming any application service exists.

**Prerequisites.** Accepted S1 checkpoint; recorded V004 SHA-256; all S1 tests passing; V004 frozen.

**Allowed files.**

- `PostgresContainerIT.java`: add exactly four methods and bounded helpers/probe tables.
- `.github/workflows/ci.yml`: expected set/count 31.
- `backend/README.md`: final V004 behavior, 31-method profile, hashes and evidence boundary.
- `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s2-implementation.md` (new).

**Exact test changes.**

1. `v004CatalogVersionLifecycleAndGuardBackstops`:
   - Valid draft product/version/offer/benefit inserts; conditional revision updates; stale-revision update affects zero rows.
   - Publication allows `sale_status='ACTIVE'`, `published_at` set, `revision+1`; published content update raises `CATALOG_VERSION_PUBLISHED_IMMUTABLE`/`P0001`; `ACTIVE -> OFFSALE` succeeds with revision+1.
   - Offer mutable updates succeed; `catalog_version_id`/`channel` changes raise `CATALOG_OFFER_RELATION_IMMUTABLE`/`P0001`.
   - Immediate partial unique `uq_catalog_version_active` rejects a second `ACTIVE` version with `23505`; the retire-then-activate protocol leaves exactly one `ACTIVE`.
   - Product/benefit-line append-only: runtime `UPDATE`/`DELETE` `42501`; owner `UPDATE`/`DELETE` `CATALOG_ROW_APPEND_ONLY`/`P0001`.
   - One `23514`+exact-name probe for each of the 22 product/version/offer/benefit checks (8.4).
   - All fixtures rolled back; no committed row.
2. `v004OrderPaymentLifecycleAndAppendOnlyBackstops`:
   - Valid order insert with a random `purchase_consent_id` (no parent) succeeds; `purchase_consent_id=NULL` fails with SQLSTATE `23502`.
   - Order immutable-column updates raise `CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE`/`P0001`; valid `payment_status/paid_at/closed_at/revision` transitions succeed; invalid payment shapes fail with `23514` and the exact check name.
   - Payment-order identity guard rejects `order_id`/`provider`/`merchant_order_no`/`amount_cents`/`currency`/`created_at` changes; status/transaction/updated_at/revision updates succeed; `PAID` without `provider_transaction_id` fails `ck_payment_order_paid_shape`.
   - Payment-event duplicate `(provider, provider_event_id)` fails `23505`; runtime `UPDATE`/`DELETE` `42501`; owner `UPDATE`/`DELETE` `CATALOG_ROW_APPEND_ONLY`/`P0001`; duplicate/verified/unverified rows are retained.
   - One `23514`+exact-name probe for each of the 29 customer_order/payment_order/payment_event checks (8.4).
   - All fixtures rolled back; no committed append-only row.
3. `v004ForeignKeyAndAbsentConsentFkBackstops`:
   - Missing-parent `23503` + exact constraint name for all 11 FKs.
   - Parent-delete RESTRICT `23503` probes where the parent has no append-only delete trigger: `catalog_version` (offer/benefit child), `customer_order` (payment_order child), `payment_order` (payment_event child), `customer_account`/`organization`/`subject` (order child), `employee_account` (version child). The `catalog_product` delete is intentionally shadowed by `trg_catalog_product_immutable` (`P0001`); assert that trigger and the FK metadata `confdeltype='r'` instead of a `23503`.
   - Exact 11-row FK name/definition/property map; `fk_customer_order_consent` count zero; `purchase_consent_id` `attnotnull=true`, no default, no FK; `activation_consent` has zero tables.
   - All fixtures rolled back.
4. `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner`:
   - **Publication race without application lock:** two connections activate two different DRAFT versions for one product with no current `ACTIVE`; one commits, the loser receives exact `23505` `uq_catalog_version_active`, rolls back and reads the winner; final exactly one `ACTIVE`.
   - **Publication protocol with advisory lock:** two connections run `pg_advisory_xact_lock(hashtextextended('catalog_order.publication:<product_id>', 0))`, re-read the expected current active version, retire it to `OFFSALE` with revision+1, then activate the candidate with `published_at` and revision+1; one commits, the loser observes a stale expected current version and changes nothing. This is transient SQL protocol evidence, not a service.
   - **Order idempotency race:** two connections insert the same `(account_id, idempotency_key)`; one winner, loser `23505` `uq_customer_order_idempotency`.
   - **Payment-order identity race:** two connections insert `payment_order` for the same `order_id`; one winner, loser `23505` `uq_payment_order_order`; a second scenario races the same non-null `provider_transaction_id` on two orders and expects `23505` `uq_payment_order_transaction`.
   - **Payment-event identity race:** two connections insert the same `(provider, provider_event_id)`; one winner, loser `23505` `uq_payment_event_provider_event`.
   - Each scenario uses two connections with distinct `application_name`, `pg_stat_activity` lock-wait detection, bounded `statement_timeout` and future timeout; exactly one committed winner; the loser is rolled back before reading the winner; no sleep-only timing assertion. Append-only rows are never removed by runtime SQL.
   - Method-local cleanup: in `finally`, close both connections, reconnect as the migration identity, and run one `TRUNCATE` covering all seven `catalog_order` tables in a single statement (this is the only way to remove committed append-only fixtures and is test-fixture hygiene, not a granted runtime path; the TRUNCATE privilege denial to runtime roles is separately asserted). Then delete the method-local V002/V003 parent fixtures in FK-safe order (`business_subject`, `organization`, `employee_account`, `customer_account`). Assert zero remaining method-local rows by UUID.
   - No `@Disabled`, `Assumptions`, environment skip or weakened assertion.

**Invariants/state transitions/error handling.**

- V004 is unchanged; only the test class, CI set/count, README and S2 artifact change.
- Every expected failure uses a savepoint or a separate connection and asserts the exact SQLSTATE and constraint/exception name.
- Valid revision/lifecycle paths are demonstrated in the same method as the rejection paths.
- Publication/order/payment/event service orchestration remains deferred; the tests label the transient protocols.
- No append-only row survives cleanup; no runtime role receives `DELETE`/`TRUNCATE`.

**Non-goals.** No V005+ object; no application service/API/JPA; no provider integration; no entitlement/consent object; no new schema/function/trigger/grant; no claim that publication/order/payment/issuance services exist.

**Validation commands/assertions.** Section 8.6:
- Full container profile → exactly 31 methods, `skipped=0 failures=0 errors=0`, exact set.
- Focused behavior methods once each (repeat if ambiguous); focused concurrency method at least three independent runs, each `tests=1`, `skipped=0`, no failure/error.
- Default verify 10/0/0/0; boot smoke; fat-jar firewall; V001-V003 hashes; V004 hash unchanged; whitespace/status/cleanup checks.

**Completion signal.** All S2 commands pass; the S2 artifact records exact methods/outputs/SQLSTATEs/constraint names/repetition counts/hashes/cleanup; only allowed files changed.

**Stop condition.** Any need to edit V004, V001-V003, the controller, application code or POM; any new schema/function/trigger/grant/service; any append-only cleanup that weakens the runtime denial; any unexplained mismatch that would require relaxing an assertion.

## 8. Test matrix, commands and expected assertions

### 8.1 Container IT method evolution

Current count: 25. S1 reaches 27; S2 reaches 31. No placeholder or skipped method. CI asserts exactly the method set present at each checkpoint.

| Slice | Count | Exact method set |
| --- | --- | --- |
| S1 | 27 | `v001ThroughV004MigrateCleanPostgres16WithoutSkipping` (renamed), `v001ThroughV003InventoryMatchesTheFrozenContract`, `brokenMigrationFailsVisibly`, `idempotencyNewReplayConflictAndFinalizeOnce`, `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow`, `concurrentIdempotencyRetryObservesOneWinner`, `outboxReplayConflictRollbackAndDenylist`, `auditIsAppendOnlyAndRuntimeWritesAreDenied`, `auditReasonAndDenylistRulesAreEnforced`, `functionsHaveFixedSearchPathAndNoTransactionControl`, `ownerPublicAndRoleExecutionBoundariesAreExact`, `hostileSearchPathCannotRedirectFunctionEffects`, `mutableHeadsEnforceLifecycleRevisionAndNamedChecks`, `localImmutabilityGuardsRejectRelationAndRotationChanges`, `identityCollisionAndMembershipRevokeRegrantSemantics`, `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner`, `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied`, `lastEnabledAdminTransactionIsSerializedAndFailsClosed`, `v003SubjectNumberCatalogAndGuardsAreExact`, `v003OwnerPublicAndRuntimeAclsAreExact`, `subjectOwnerSetOnceAndRealNameSeparation`, `bindingLifecycleTerminalityAndActiveNumberUniqueness`, `appendOnlyClaimAndForeignKeyBackstops`, `riskOpenClearReopenRevisionProtocol`, `concurrentActiveBindingUniquenessHasOneWinner`, `v004CatalogOrderInventoryAndGuardsAreExact`, `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact` |
| S2 | 31 | S1 set plus `v004CatalogVersionLifecycleAndGuardBackstops`, `v004OrderPaymentLifecycleAndAppendOnlyBackstops`, `v004ForeignKeyAndAbsentConsentFkBackstops`, `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner` |

S1 method responsibilities:

- `v004CatalogOrderInventoryAndGuardsAreExact`: exactly seven tables/owners; 78 column rows; 51 check names plus whole normalized `pg_get_constraintdef` equality; seven PKs, nine ordinary uniques, two partial uniques; 28 exact `indexdef`s; 11 FK defs/properties; `purchase_consent_id` NOT NULL/no default/no FK and `fk_customer_order_consent` absent; five functions with signature/owner/`prosecdef=false`/search_path/no `p_id`/no `gen_random_uuid`/no transaction control; seven triggers with exact timing/events/function; no extra function/trigger; no id default/sequence; eight role attributes; four schema owners; `assertNoV005PlusObjects`.
- `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact`: exact schema `USAGE`/`CREATE`; exact owned table/column ACL matrix; exact cross-module column `SELECT`; exact function `EXECUTE`; PUBLIC zero on V004 schemas/tables/functions/columns and the touched S1 tables; all denials in 6.6.5.

S2 method responsibilities: 8.4 and 8.5.

### 8.2 Exact catalog and ACL maps

`V004_CHECK_DEF_MAP` must contain exactly these 51 literals (PostgreSQL 16.15 deparse; whole-definition equality after the existing ASCII-whitespace-only normalizer; no substring/contains/prefix/regex/semantic matching). Java escaping must preserve the SQL token content.

```text
catalog_benefit_line|ck_catalog_benefit_interval|CHECK ((grant_interval_months >= 0))
catalog_benefit_line|ck_catalog_benefit_quantity|CHECK ((quantity >= 1))
catalog_benefit_line|ck_catalog_benefit_type|CHECK ((entitlement_type = ANY (ARRAY['RECORDING_SECONDS'::text, 'AI_SECONDS'::text, 'EVIDENCE_COUNT'::text, 'STORAGE_BYTES'::text])))
catalog_benefit_line|ck_catalog_benefit_validity|CHECK ((validity_days >= 1))
catalog_offer|ck_catalog_offer_channel|CHECK ((channel = ANY (ARRAY['MINIAPP'::text, 'WEB'::text])))
catalog_offer|ck_catalog_offer_revision|CHECK ((revision >= 1))
catalog_offer|ck_catalog_offer_status|CHECK ((offer_status = ANY (ARRAY['ACTIVE'::text, 'WITHDRAWN'::text])))
catalog_offer|ck_catalog_offer_token|CHECK ((((length(version_token) >= 1) AND (length(version_token) <= 128)) AND (btrim(version_token) <> ''::text)))
catalog_product|ck_catalog_product_code|CHECK ((product_code ~ '^[A-Z][A-Z0-9_-]{1,63}$'::text))
catalog_product|ck_catalog_product_family|CHECK ((product_family ~ '^[A-Z][A-Z0-9_]{0,63}$'::text))
catalog_version|ck_catalog_version_audience|CHECK ((audience = ANY (ARRAY['PERSONAL'::text, 'ENTERPRISE'::text])))
catalog_version|ck_catalog_version_audience_shape|CHECK (((audience = 'ENTERPRISE'::text) = (offer_type = 'QUOTE'::text)))
catalog_version|ck_catalog_version_billing_matrix|CHECK ((((offer_type = 'SUBSCRIPTION'::text) AND (billing_cycle = ANY (ARRAY['MONTHLY'::text, 'QUARTERLY'::text, 'ANNUAL'::text])) AND (installments >= 1) AND (grant_interval_months >= 1)) OR ((offer_type = ANY (ARRAY['ONE_TIME'::text, 'ADD_ON'::text])) AND (billing_cycle = 'ONCE'::text) AND (installments = 1) AND (grant_interval_months = 0)) OR ((offer_type = 'QUOTE'::text) AND (billing_cycle = 'QUOTE'::text) AND (installments = 1) AND (grant_interval_months = 0) AND (price_cents IS NULL))))
catalog_version|ck_catalog_version_currency|CHECK ((currency ~ '^[A-Z]{3}$'::text))
catalog_version|ck_catalog_version_no|CHECK ((version_no >= 1))
catalog_version|ck_catalog_version_offer_type|CHECK ((offer_type = ANY (ARRAY['SUBSCRIPTION'::text, 'ONE_TIME'::text, 'ADD_ON'::text, 'QUOTE'::text])))
catalog_version|ck_catalog_version_price|CHECK ((((price_cents IS NULL) = (offer_type = 'QUOTE'::text)) AND ((price_cents IS NULL) OR (price_cents >= 0))))
catalog_version|ck_catalog_version_published_shape|CHECK (((sale_status = 'DRAFT'::text) = (published_at IS NULL)))
catalog_version|ck_catalog_version_revision|CHECK ((revision >= 1))
catalog_version|ck_catalog_version_sale_status|CHECK ((sale_status = ANY (ARRAY['DRAFT'::text, 'ACTIVE'::text, 'OFFSALE'::text])))
catalog_version|ck_catalog_version_snapshot_sha|CHECK ((snapshot_sha256 ~ '^[0-9a-f]{64}$'::text))
catalog_version|ck_catalog_version_snapshot_type|CHECK ((jsonb_typeof(snapshot) = 'object'::text))
customer_order|ck_customer_order_amount|CHECK ((amount_cents >= 0))
customer_order|ck_customer_order_channel|CHECK ((channel = ANY (ARRAY['MINIAPP'::text, 'WEB'::text])))
customer_order|ck_customer_order_currency|CHECK ((currency ~ '^[A-Z]{3}$'::text))
customer_order|ck_customer_order_fingerprint|CHECK ((request_fingerprint ~ '^[0-9a-f]{64}$'::text))
customer_order|ck_customer_order_idempotency|CHECK ((((length(idempotency_key) >= 1) AND (length(idempotency_key) <= 475)) AND (btrim(idempotency_key) <> ''::text)))
customer_order|ck_customer_order_no|CHECK ((((length(order_no) >= 1) AND (length(order_no) <= 64)) AND (btrim(order_no) <> ''::text)))
customer_order|ck_customer_order_payment_shape|CHECK ((((payment_status = 'PAID'::text) = (paid_at IS NOT NULL)) AND ((payment_status = 'CLOSED'::text) = (closed_at IS NOT NULL))))
customer_order|ck_customer_order_payment_status|CHECK ((payment_status = ANY (ARRAY['UNPAID'::text, 'UNKNOWN'::text, 'PAID'::text, 'CLOSED'::text])))
customer_order|ck_customer_order_revision|CHECK ((revision >= 1))
customer_order|ck_customer_order_snapshot_sha|CHECK ((snapshot_sha256 ~ '^[0-9a-f]{64}$'::text))
customer_order|ck_customer_order_snapshot_type|CHECK ((jsonb_typeof(snapshot) = 'object'::text))
customer_order|ck_customer_order_time_order|CHECK ((((paid_at IS NULL) OR (paid_at >= created_at)) AND ((closed_at IS NULL) OR (closed_at >= created_at))))
customer_order|ck_customer_order_version_token|CHECK ((((length(version_token) >= 1) AND (length(version_token) <= 128)) AND (btrim(version_token) <> ''::text)))
payment_event|ck_payment_event_amount|CHECK ((amount_cents >= 0))
payment_event|ck_payment_event_event_id|CHECK ((((length(provider_event_id) >= 1) AND (length(provider_event_id) <= 128)) AND (btrim(provider_event_id) <> ''::text)))
payment_event|ck_payment_event_payload_ciphertext|CHECK ((payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'::text))
payment_event|ck_payment_event_payload_sha|CHECK ((payload_sha256 ~ '^[0-9a-f]{64}$'::text))
payment_event|ck_payment_event_provider|CHECK ((provider = 'WECHAT'::text))
payment_event|ck_payment_event_transaction|CHECK (((transaction_id IS NULL) OR (((length(transaction_id) >= 1) AND (length(transaction_id) <= 128)) AND (btrim(transaction_id) <> ''::text))))
payment_event|ck_payment_event_type|CHECK ((((length(event_type) >= 1) AND (length(event_type) <= 64)) AND (btrim(event_type) <> ''::text)))
payment_order|ck_payment_order_amount|CHECK ((amount_cents >= 0))
payment_order|ck_payment_order_currency|CHECK ((currency ~ '^[A-Z]{3}$'::text))
payment_order|ck_payment_order_merchant_no|CHECK ((((length(merchant_order_no) >= 1) AND (length(merchant_order_no) <= 64)) AND (btrim(merchant_order_no) <> ''::text)))
payment_order|ck_payment_order_paid_shape|CHECK (((status <> 'PAID'::text) OR (provider_transaction_id IS NOT NULL)))
payment_order|ck_payment_order_provider|CHECK ((provider = 'WECHAT'::text))
payment_order|ck_payment_order_revision|CHECK ((revision >= 1))
payment_order|ck_payment_order_status|CHECK ((status = ANY (ARRAY['UNPAID'::text, 'UNKNOWN'::text, 'PAID'::text, 'CLOSED'::text])))
payment_order|ck_payment_order_time_order|CHECK ((updated_at >= created_at))
payment_order|ck_payment_order_transaction|CHECK (((provider_transaction_id IS NULL) OR (((length(provider_transaction_id) >= 1) AND (length(provider_transaction_id) <= 128)) AND (btrim(provider_transaction_id) <> ''::text))))
```

Exact PK/unique constraint definitions (7 PK + 9 unique; the two partial uniques are indexes, not constraints):

```text
catalog_benefit_line|pk_catalog_benefit_line|p|PRIMARY KEY (id)|id
catalog_benefit_line|uq_catalog_benefit_version_type|u|UNIQUE (catalog_version_id, entitlement_type)|catalog_version_id,entitlement_type
catalog_offer|pk_catalog_offer|p|PRIMARY KEY (id)|id
catalog_offer|uq_catalog_offer_version_channel|u|UNIQUE (catalog_version_id, channel)|catalog_version_id,channel
catalog_product|pk_catalog_product|p|PRIMARY KEY (id)|id
catalog_product|uq_catalog_product_code|u|UNIQUE (product_code)|product_code
catalog_version|pk_catalog_version|p|PRIMARY KEY (id)|id
catalog_version|uq_catalog_version_no|u|UNIQUE (product_id, version_no)|product_id,version_no
customer_order|pk_customer_order|p|PRIMARY KEY (id)|id
customer_order|uq_customer_order_idempotency|u|UNIQUE (account_id, idempotency_key)|account_id,idempotency_key
customer_order|uq_customer_order_no|u|UNIQUE (order_no)|order_no
payment_event|pk_payment_event|p|PRIMARY KEY (id)|id
payment_event|uq_payment_event_provider_event|u|UNIQUE (provider, provider_event_id)|provider,provider_event_id
payment_order|pk_payment_order|p|PRIMARY KEY (id)|id
payment_order|uq_payment_order_merchant|u|UNIQUE (merchant_order_no)|merchant_order_no
payment_order|uq_payment_order_order|u|UNIQUE (order_id)|order_id
```

Exact FK definitions (11; all `rrs|false|false|true`):

```text
fk_catalog_benefit_version|FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_catalog_offer_version|FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_catalog_version_employee|FOREIGN KEY (created_by_employee_id) REFERENCES operations_iam.employee_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_catalog_version_product|FOREIGN KEY (product_id) REFERENCES catalog_order.catalog_product(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_customer_order_account|FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_customer_order_offer|FOREIGN KEY (catalog_offer_id) REFERENCES catalog_order.catalog_offer(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_customer_order_organization|FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_customer_order_subject|FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_customer_order_version|FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_payment_event_order|FOREIGN KEY (payment_order_id) REFERENCES catalog_order.payment_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
fk_payment_order_order|FOREIGN KEY (order_id) REFERENCES catalog_order.customer_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true
```

Exact `pg_indexes.indexdef` values (28, sorted by table then index name; the implementation may assert as a set):

```text
CREATE INDEX ix_catalog_benefit_version ON catalog_order.catalog_benefit_line USING btree (catalog_version_id)
CREATE UNIQUE INDEX pk_catalog_benefit_line ON catalog_order.catalog_benefit_line USING btree (id)
CREATE UNIQUE INDEX uq_catalog_benefit_version_type ON catalog_order.catalog_benefit_line USING btree (catalog_version_id, entitlement_type)
CREATE INDEX ix_catalog_offer_channel_status ON catalog_order.catalog_offer USING btree (channel, offer_status)
CREATE UNIQUE INDEX pk_catalog_offer ON catalog_order.catalog_offer USING btree (id)
CREATE UNIQUE INDEX uq_catalog_offer_version_channel ON catalog_order.catalog_offer USING btree (catalog_version_id, channel)
CREATE INDEX ix_catalog_product_family ON catalog_order.catalog_product USING btree (product_family, created_at)
CREATE UNIQUE INDEX pk_catalog_product ON catalog_order.catalog_product USING btree (id)
CREATE UNIQUE INDEX uq_catalog_product_code ON catalog_order.catalog_product USING btree (product_code)
CREATE INDEX ix_catalog_version_product_status ON catalog_order.catalog_version USING btree (product_id, sale_status, version_no DESC)
CREATE UNIQUE INDEX pk_catalog_version ON catalog_order.catalog_version USING btree (id)
CREATE UNIQUE INDEX uq_catalog_version_active ON catalog_order.catalog_version USING btree (product_id) WHERE (sale_status = 'ACTIVE'::text)
CREATE UNIQUE INDEX uq_catalog_version_no ON catalog_order.catalog_version USING btree (product_id, version_no)
CREATE INDEX ix_customer_order_organization ON catalog_order.customer_order USING btree (owner_organization_id, created_at DESC)
CREATE INDEX ix_customer_order_subject_status ON catalog_order.customer_order USING btree (subject_id, payment_status, created_at DESC)
CREATE INDEX ix_customer_order_version ON catalog_order.customer_order USING btree (catalog_version_id)
CREATE UNIQUE INDEX pk_customer_order ON catalog_order.customer_order USING btree (id)
CREATE UNIQUE INDEX uq_customer_order_idempotency ON catalog_order.customer_order USING btree (account_id, idempotency_key)
CREATE UNIQUE INDEX uq_customer_order_no ON catalog_order.customer_order USING btree (order_no)
CREATE INDEX ix_payment_event_order_time ON catalog_order.payment_event USING btree (payment_order_id, received_at DESC)
CREATE INDEX ix_payment_event_verified ON catalog_order.payment_event USING btree (payment_order_id, received_at) WHERE signature_verified
CREATE UNIQUE INDEX pk_payment_event ON catalog_order.payment_event USING btree (id)
CREATE UNIQUE INDEX uq_payment_event_provider_event ON catalog_order.payment_event USING btree (provider, provider_event_id)
CREATE INDEX ix_payment_order_status_time ON catalog_order.payment_order USING btree (status, updated_at)
CREATE UNIQUE INDEX pk_payment_order ON catalog_order.payment_order USING btree (id)
CREATE UNIQUE INDEX uq_payment_order_merchant ON catalog_order.payment_order USING btree (merchant_order_no)
CREATE UNIQUE INDEX uq_payment_order_order ON catalog_order.payment_order USING btree (order_id)
CREATE UNIQUE INDEX uq_payment_order_transaction ON catalog_order.payment_order USING btree (provider_transaction_id) WHERE (provider_transaction_id IS NOT NULL)
```

Exact function rows (signature|prosecdef|owner|config):

```text
catalog_order.guard_catalog_offer()|false|catalog_order_owner|search_path=catalog_order, pg_temp
catalog_order.guard_catalog_version_published()|false|catalog_order_owner|search_path=catalog_order, pg_temp
catalog_order.guard_customer_order_snapshot()|false|catalog_order_owner|search_path=catalog_order, pg_temp
catalog_order.guard_payment_order_identity()|false|catalog_order_owner|search_path=catalog_order, pg_temp
catalog_order.reject_row_mutation()|false|catalog_order_owner|search_path=catalog_order, pg_temp
```

If the real PostgreSQL 16 container differs in more than ASCII whitespace, stop and escalate; do not broaden the normalizer or weaken the map.

### 8.3 ACL assertions (exact)

- Schema `USAGE`/`CREATE`: assert every row in 6.6.1 for the 12 V004/S1/production roles and `PUBLIC`; assert no `CREATE` for any runtime role; assert `ivr_role` no `ivr`/`catalog_order`/S1 `USAGE`; `catalog_order_role` no `ivr` and no `entitlement_metering` `USAGE` at V004 (its first `entitlement_metering` `USAGE` is V006), while `has_schema_privilege('catalog_order_role','activation_consent','USAGE')` is true; `activation_consent_role` no S1 `USAGE`; `entitlement_metering_role` no S1 `USAGE`.
- Owned-table: `assertTablePrivileges` for `SELECT=true`, `INSERT=true`, `UPDATE=false`, `DELETE=false`, `TRUNCATE=false` on all seven for `catalog_order_role`; then `has_column_privilege` must be true exactly for the 12/3/4/4 UPDATE columns and false for every other column, including `id`, `product_id`, `version_no`, `created_by_employee_id`, `created_at`, `merchant_order_no`, `amount_cents`.
- Cross-module: for each row in 6.6.3, assert the exact columns are `SELECT`-readable and every unlisted column is denied; assert table-level `SELECT`/`UPDATE`/`INSERT`/`DELETE` are false for the grantee on the foreign table; assert `catalog_order_role` cannot read `activation_consent`/`entitlement_metering` tables (absent) and `entitlement_metering_role` cannot read S1 tables.
- Function: assert `EXECUTE` exactly as 6.6.4; assert no runtime `EXECUTE` on `reject_row_mutation`; assert no `PUBLIC`; assert no `issue_first_trial` and no `entitlement_metering_owner` platform capability.
- PUBLIC: query `aclexplode(coalesce(nspacl, acldefault('n', nspowner)))`, `acldefault('r', relowner)`, `acldefault('f', proowner)` and `acldefault('c', relowner)` with `grantee=0` for the four S2 schemas, seven tables, five functions and all non-dropped columns; also assert the five touched S1 tables have zero PUBLIC column ACL. Include the PA3 F-S1-01 column-ACL pattern.
- Role attributes: all eight `rolcanlogin=false`, `rolsuper=false`, `rolcreatedb=false`, `rolcreaterole=false`; no `pg_auth_members` membership among the eight; owners have `CREATE` only on their own schema.

### 8.4 Named-check probes (`23514` + exact constraint name)

The S2 suite must define a `V004_CHECK_PROBES` table with exactly 51 entries and assert the covered check-name set equals `V004_CHECK_DEF_MAP`'s name set. Each probe uses a savepoint, asserts SQLSTATE `23514` and the exact constraint name, and rolls back. Suggested violating values (must be adapted only to satisfy the exact predicate, never to weaken it):

| Table | Check | Violating probe |
| --- | --- | --- |
| `catalog_product` | `ck_catalog_product_code` | `product_code='bad'` |
| `catalog_product` | `ck_catalog_product_family` | `product_family='bad'` |
| `catalog_version` | `ck_catalog_version_no` | `version_no=0` |
| `catalog_version` | `ck_catalog_version_sale_status` | `sale_status='INVALID'` |
| `catalog_version` | `ck_catalog_version_offer_type` | `offer_type='INVALID'` |
| `catalog_version` | `ck_catalog_version_audience` | `audience='INVALID'` |
| `catalog_version` | `ck_catalog_version_audience_shape` | `audience='ENTERPRISE', offer_type='ONE_TIME'` |
| `catalog_version` | `ck_catalog_version_price` | `offer_type='ONE_TIME', price_cents=NULL` |
| `catalog_version` | `ck_catalog_version_currency` | `currency='CN'` |
| `catalog_version` | `ck_catalog_version_billing_matrix` | `offer_type='SUBSCRIPTION', billing_cycle='ONCE'` |
| `catalog_version` | `ck_catalog_version_snapshot_type` | `snapshot='[]'::jsonb` |
| `catalog_version` | `ck_catalog_version_snapshot_sha` | `snapshot_sha256='abc'` |
| `catalog_version` | `ck_catalog_version_published_shape` | `sale_status='ACTIVE', published_at=NULL` |
| `catalog_version` | `ck_catalog_version_revision` | `revision=0` |
| `catalog_offer` | `ck_catalog_offer_channel` | `channel='IVR'` |
| `catalog_offer` | `ck_catalog_offer_status` | `offer_status='RETIRED'` |
| `catalog_offer` | `ck_catalog_offer_token` | `version_token='   '` |
| `catalog_offer` | `ck_catalog_offer_revision` | `revision=0` |
| `catalog_benefit_line` | `ck_catalog_benefit_type` | `entitlement_type='INVALID'` |
| `catalog_benefit_line` | `ck_catalog_benefit_quantity` | `quantity=0` |
| `catalog_benefit_line` | `ck_catalog_benefit_validity` | `validity_days=0` |
| `catalog_benefit_line` | `ck_catalog_benefit_interval` | `grant_interval_months=-1` |
| `customer_order` | `ck_customer_order_no` | `order_no='   '` |
| `customer_order` | `ck_customer_order_channel` | `channel='IVR'` |
| `customer_order` | `ck_customer_order_amount` | `amount_cents=-1` |
| `customer_order` | `ck_customer_order_currency` | `currency='CN'` |
| `customer_order` | `ck_customer_order_payment_status` | `payment_status='REFUNDED'` |
| `customer_order` | `ck_customer_order_payment_shape` | `payment_status='PAID', paid_at=NULL` |
| `customer_order` | `ck_customer_order_time_order` | `created_at='2026-01-02', paid_at='2026-01-01', payment_status='PAID'` |
| `customer_order` | `ck_customer_order_snapshot_type` | `snapshot='[]'::jsonb` |
| `customer_order` | `ck_customer_order_snapshot_sha` | `snapshot_sha256='abc'` |
| `customer_order` | `ck_customer_order_version_token` | `version_token='   '` |
| `customer_order` | `ck_customer_order_idempotency` | 476-character `idempotency_key` (or blank) |
| `customer_order` | `ck_customer_order_fingerprint` | `request_fingerprint='abc'` |
| `customer_order` | `ck_customer_order_revision` | `revision=0` |
| `payment_order` | `ck_payment_order_provider` | `provider='ALIPAY'` |
| `payment_order` | `ck_payment_order_merchant_no` | `merchant_order_no='   '` |
| `payment_order` | `ck_payment_order_transaction` | `provider_transaction_id='   '` |
| `payment_order` | `ck_payment_order_status` | `status='REFUNDED'` |
| `payment_order` | `ck_payment_order_paid_shape` | `status='PAID', provider_transaction_id=NULL` |
| `payment_order` | `ck_payment_order_amount` | `amount_cents=-1` |
| `payment_order` | `ck_payment_order_currency` | `currency='CN'` |
| `payment_order` | `ck_payment_order_time_order` | `created_at=now(), updated_at=created_at - interval '1 second'` |
| `payment_order` | `ck_payment_order_revision` | `revision=0` |
| `payment_event` | `ck_payment_event_provider` | `provider='ALIPAY'` |
| `payment_event` | `ck_payment_event_event_id` | `provider_event_id='   '` |
| `payment_event` | `ck_payment_event_type` | `event_type='   '` |
| `payment_event` | `ck_payment_event_payload_ciphertext` | `payload_ciphertext='plaintext'` |
| `payment_event` | `ck_payment_event_payload_sha` | `payload_sha256='abc'` |
| `payment_event` | `ck_payment_event_transaction` | `transaction_id='   '` |
| `payment_event` | `ck_payment_event_amount` | `amount_cents=-1` |

### 8.5 Concurrency and transient-protocol assertions

- Two real connections per scenario, distinct `application_name`, `statement_timeout`, future timeout, `pg_stat_activity` blocking detection; no sleep-only timing assertion.
- Publication without application lock: exact loser SQLSTATE `23505`, constraint `uq_catalog_version_active`; winner commit; loser rollback; final one `ACTIVE`.
- Publication with `pg_advisory_xact_lock(hashtextextended('catalog_order.publication:<product_id>', 0))`: retire-first then activate; one winner; stale loser changes nothing; existing orders/snapshots (if any fixture) are not rewritten; this is protocol evidence only.
- Order idempotency: exact loser `23505`/`uq_customer_order_idempotency`; one order.
- Payment order: exact loser `23505`/`uq_payment_order_order`; separate `provider_transaction_id` race → `23505`/`uq_payment_order_transaction`.
- Payment event: exact loser `23505`/`uq_payment_event_provider_event`; one event.
- Cleanup: one migration-identity `TRUNCATE` of all seven `catalog_order` tables in a single statement, then FK-safe deletion of method-local V002/V003 parents; assert zero remaining rows by the method's UUIDs. The TRUNCATE is test-fixture hygiene only; runtime `TRUNCATE` denial remains asserted in S1/S2 ACL checks.

### 8.6 Commands and expected results

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

Expected at S1: `tests=27`; at S2/final: `tests=31`; `skipped=0 failures=0 errors=0`; exact method set from 8.1; no pending migration.

Focused behavior methods (at least once each; repeat if ambiguous):

```bash
./mvnw -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#v004CatalogVersionLifecycleAndGuardBackstops'
./mvnw -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#v004OrderPaymentLifecycleAndAppendOnlyBackstops'
./mvnw -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#v004ForeignKeyAndAbsentConsentFkBackstops'
```

Focused concurrency method (at least three independent successful executions):

```bash
./mvnw -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#concurrentCatalogPublicationAndCommerceIdentityHasOneWinner'
```

Expected: `tests=1`, `skipped=0`, no failure/error in each repetition.

Boot smoke and fat-jar firewall:

```bash
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
! grep -Eq 'BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)' target/jar-index.txt
```

Exact source/CI/XML checks:

```bash
ls backend/src/main/resources/db/migration
# exactly V001__..., V002__..., V003__..., V004__catalog_order_and_payment.sql
! grep -R -E 'V00[5-9]|V0[1-9][0-9]' backend/src/main/resources/db/migration
# one TEST-*PostgresContainerIT.xml; tests == expected set size; cases == expected set;
# skipped=0 failures=0 errors=0; every surefire report skipped=0
# CI expected set has exactly the S1 27 or final 31 names
```

Hashes and whitespace (use the Windows worktree Git env):

```bash
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call4
export GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call
git show HEAD:backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql | sha256sum
# expected: 946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045
git show HEAD:backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql | sha256sum
# expected: 534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5
git show HEAD:backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql | sha256sum
# expected: 1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1
git diff --ignore-cr-at-eol --exit-code HEAD -- \
  backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql \
  backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql \
  backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql
# expected: exit 0
sha256sum backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql
# record at S1; assert unchanged at S2
git -c core.whitespace=cr-at-eol diff --check -- <exact changed tracked paths>
# expected: exit 0
git diff --no-index --check /dev/null <each new untracked file>
# expected: exit 1 with zero diagnostic output
grep -n -E '[[:blank:]]+$' <each changed/new text file>
# expected: no output
```

Exact-path and cleanup checks:

```bash
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call4 GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call git status --porcelain=v1
# expected: allowed slice paths plus the controller-owned project-execution-control.md modification
docker ps -a --format '{{.Names}}' | grep -E '^pa4-probe$' || true
# expected: no output; any planning probe is removed and Testcontainers stops its own container
# method-local row assertions: zero remaining method-local catalog_order rows and zero remaining
# V002/V003 parent fixtures after the concurrency cleanup; rolled-back methods leave no row
```

A global raw `git diff --check` is not authoritative in this Windows-checkout worktree because of the CRLF baseline and pre-existing documentation whitespace. The scoped CR-aware check plus the no-index new-file check is the equivalent fail-closed gate. New/edited files must be LF; do not normalize line endings repository-wide.

### 8.7 CI assertion evolution

Update only the Failsafe report assertion in `.github/workflows/ci.yml`:

- S1: exactly one `TEST-*PostgresContainerIT.xml`; `tests=27`; the S1 set in 8.1; `skipped=0`, `failures=0`, `errors=0`; every surefire report `skipped=0`.
- S2: `tests=31`; the S2 set in 8.1.

Keep unchanged: `docker info` before Maven; `-Pcontainer-smoke verify`; boot smoke; fat-jar firewall; contract regressions; permissions; path filters; toolchain pins. The assertion label changes from V003 to V004; no step is removed or weakened.

### 8.8 README evolution

- S1: update the title/intro/evidence paragraphs to PA0/PA1/PA2/PA3/PA4; state V004 adds four schemas, eight roles, seven tables, 78 columns, 51 checks, 28 indexes, 11 FKs, five trigger functions, seven triggers and exact grants; state no V005+ object/`issue_first_trial`; update the container count to 27 and describe the renamed/new methods.
- S2: add the four behavior/concurrency methods, count 31, V004 hash preservation, Q4/Q6/Q17/Q20/Q23/OQ-4 deferral and the non-production evidence boundary.
- Keep toolchain pins, module boundary table, firewall description and Modulith note unchanged.

## 9. Documentation decision

- Do not modify `docs/12`, `docs/14`, the controller, V001-V003, `backend/pom.xml` or application code. They are binding/accepted inputs.
- Do not add architecture, API, security, deployment or schema documents. This plan is the only new design document for the work unit.
- Update `backend/README.md` as described in 8.8.
- Add one implementation completion/evidence artifact per slice at the exact paths in section 5.
- The V004 SQL may carry a short header comment; comments do not change catalog behavior. No documentation file may claim a service, provider or production acceptance that was not executed.

## 10. Risks, open questions, owners and destinations

| ID | Risk / open question | Classification | Owner | Destination / action |
| --- | --- | --- | --- | --- |
| R-01 | Migration-identity FK/grant execution depends on Q17/Q23 provisioning. | Deferred input | Platform/Database/Security | Q17/Q23; test superuser is a fixture; add FKs after `RESET ROLE` and never invent a `REFERENCES` grant |
| R-02 | Windows CRLF and the `D:/` `.git` pointer make raw hashes/global diff misleading. | Environment/tooling | Controller/Platform | Use the exact `GIT_DIR`/`GIT_WORK_TREE`, HEAD blob hashes and CR-normalized/scoped checks; never normalize repository-wide |
| R-03 | Catalog publication/offsale, order creation, payment callback, consent and issuance are service-owned. | Deferred behavior | catalog-order / activation-consent / entitlement-metering services | Implement in their later work units (§9.17/§9.18/§9.26); tests prove DB backstops and bounded transient protocols only |
| R-04 | OQ-4 offer withdrawal endpoint is not implemented. | Deferred product/operations policy | Product/Operations/catalog-order | Catalog operations OpenAPI work; V004 relies on version `OFFSALE` for new-order control |
| R-05 | V007 consent FK is absent; the NOT NULL column has no parent in V004. | Deferred accepted slice | Activation-consent/catalog-order | Add `fk_customer_order_consent` by ordinary immediate `ALTER` in V007; do not add early |
| R-06 | Q2 envelope/KMS, Q6 provider payload/merchant identifiers, Q20 installment timing/grace. | Deferred input | Security/Payments/Finance | Later adapter/security/issuance work; store envelope/hash/cents only; no guessed values |
| R-07 | PG16 deparse/index text can vary by minor version. | Test-only fail-closed risk | Backend/QA | Freeze the 16.15 literals; normalize ASCII whitespace only; any real difference stops and escalates |
| R-08 | Committed append-only fixtures cannot be deleted by runtime SQL or owner DELETE. | Test-design risk | Backend/QA | Behavior tests roll back; concurrency cleanup uses one migration-identity `TRUNCATE` of all seven tables and FK-safe parent deletes; runtime denial remains asserted |
| R-09 | Column-level INSERT/UPDATE can be confused with table-level privileges. | Test-only risk | Backend/QA | Assert both `has_table_privilege` and `has_column_privilege`; table UPDATE false; exact column sets |
| R-10 | `reject_row_mutation` EXECUTE interpretation. | Resolved by accepted precedent | Backend/Security | No runtime grant; four guard functions only; append-only triggers still fire |
| R-11 | Wave assignment of `ivr` schema USAGE and `entitlement_metering_role` catalog reads. | Resolved interpretation; reviewer may confirm | Controller/Plan review | Documented in 1.6 with §11.4/§10.12 citations; a rejection is a bounded plan fix, not implementation redesign |
| R-12 | Transient publication/order/payment protocols could drift into production behavior. | Scope guard | Backend/Controller | Keep them in the IT class; no service/function/trigger beyond the five trigger functions |
| R-13 | Concurrency tests can be timing-sensitive. | Bounded test risk | Backend/QA | Two connections, `application_name`, lock-wait detection, bounded timeouts, three focused runs |
| R-14 | GitHub Actions has not run at plan/implementation gates. | Gate residual | Phaseflow/CI | Exact-head PR gate; local/CI success is not production acceptance |
| R-15 | Catalog entitlement projection, issuance status and payment provider orchestration are not V004. | Deferred behavior | entitlement-metering/payments | Later services; V004 stores no order-level entitlement status and no provider integration |

No blocking open question remains for planning. Q2/Q4/Q6/Q17/Q20/Q23, OQ-4 and the service-owned publication/order/payment/issuance behaviors are explicitly deferred and must not be guessed. The two resolved interpretations in 1.6 are the only plan-level judgment calls; if the controller rejects either, the plan is corrected before implementation.

## 11. No-goal-drift and no-over-design check

| Check | Evidence |
| --- | --- |
| Only V004 exists | One new migration; four schemas; eight roles; seven tables; 78 columns; 51 checks; seven PKs; 11 unique keys; 28 indexes; 11 FKs; five trigger functions; seven triggers; no V005+ |
| No later domain object | No IVR, entitlement, activation-consent, evidence, sharing, refund, invoice, work-order or provider object |
| No application service/API/JPA | No Java main change, no controller/repository/JPA, no runtime DataSource/Flyway, no API |
| No early V007 FK | `purchase_consent_id` NOT NULL; `fk_customer_order_consent` absent; V007 owns it |
| No entitlement status on the order | `payment_status` is payment-only; no issuance/entitlement column |
| No offer withdrawal service | `ACTIVE`/`WITHDRAWN` state and guard only; OQ-4 deferred |
| No Q2/Q4/Q6/Q17/Q20/Q23 guess | Envelope/HMAC/sha checks only; no price/quantity/date/provider/KMS value; invoker trigger functions; test superuser fixture |
| Exact least privilege | 6.6 matrix; no DELETE/TRUNCATE/REFERENCES/TRIGGER; no cross-module write/row lock; PUBLIC denied |
| No V001-V003 change | HEAD blob hashes and CR-normalized diff checks |
| Two behavioral slices | Catalog/ACL then lifecycle/guard/FK/concurrency; not table/file/owner/layer split |
| Minimum docs/CI/evidence | Only README, CI method set and two per-slice evidence artifacts |
| Test-only protocols are not services | Publication/order/payment/event protocols live only in the IT class; no SQL service function |
| No production claim | README and completion reports state the evidence boundary |

## 12. Completion report template

Each implementation Agent writes exactly one new artifact at the slice path in section 5 with:

1. Task label, work unit, gate, slice, date, author/provider.
2. Binding inputs read and confirmation that the control document was not modified.
3. Base commit, accepted prior-slice checkpoint, V001/V002/V003 HEAD hashes, V004 SHA-256 before/after, and the probe record/cleanup.
4. Exact changed paths with SHA-256 and a content-only diff manifest accounting for the CRLF baseline.
5. Scope actually implemented, including the exact four schemas/eight roles/seven tables/78 columns/51 checks/28 indexes/11 FKs/five functions/seven triggers/grants for S1, or the exact methods for S2.
6. Exact test methods added/changed and the CI expected set/count (27 or 31).
7. Exact commands, environment (JDK, Maven, Docker, PostgreSQL, Flyway), exits, and surefire/failsafe counts (`10` default; `27/31` container; `skipped=0 failures=0 errors=0`).
8. Negative and concurrency evidence: exact SQLSTATEs, constraint/guard names, focused repetition counts, blocking detection and cleanup evidence (including the append-only TRUNCATE and row-count assertions).
9. V001/V002/V003 hash and whitespace/diff evidence, including the raw-`git diff --check` baseline caveat.
10. README/CI changes and documentation decision.
11. Any deviation, stop condition or unresolved issue, with owner/destination.
12. Residual risks classified as fixed-current, later-approved-slice or later-work-unit, with owners.
13. Completion or blocked statement; explicit statement that no later gate, commit, push, PR, merge or production acceptance was performed.
14. Explicit no-goal-drift statement and exact changed-file list.

The report must not claim GitHub Actions, production provisioning or production acceptance unless those actions actually ran.

## 13. Plan validation performed by the author

- Read the PA4 control contract; `docs/12` sections 5, 6, 14 and 15; binding `docs/14` sections 2.9, 4.16-4.22, 5.4, 9.0.1, 9.17, 9.18, 9.22, 9.26, 10.12, 10.13, 10.16, 10.17, 10.19, 11.4, 13.2.1, 13.6, 13.7, 13.10, 13.11, 15 and 16; V001/V002/V003; the full current `PostgresContainerIT`; `.github/workflows/ci.yml`; `backend/README.md`; `backend/pom.xml`.
- Independently derived the V004 totals from docs/14 and confirmed them with a disposable `postgres:16` (PostgreSQL 16.15) probe: seven tables, 78 columns, 51 checks, seven PKs, nine ordinary uniques plus two partial uniques, 11 FKs, 28 indexes, five functions, seven triggers, 16 defaulted columns, zero id defaults/sequences, zero V005+ tables, zero `issue_first_trial`, global 20 functions/18 triggers.
- Probed the exact PostgreSQL 16.15 `pg_get_constraintdef`, PK/unique, FK, `indexdef`, function and trigger forms embedded in 8.2; no discrepancy remains.
- Probed the behavior the plan will test: published-content guard, second-`ACTIVE` immediate partial unique `23505`, retire-then-activate one-active protocol, offer relation guard, order insert with a random consent ID and no V007 FK, order idempotency `23505`, order snapshot guard, payment-shape `23514`, payment-order order/transaction uniqueness, payment paid-shape `23514`, payment-event provider-event `23505`, append-only `CATALOG_ROW_APPEND_ONLY`, missing-parent `23503`, parent-delete RESTRICT `23503`, and `purchase_consent_id` NOT NULL `23502`.
- Confirmed the V001/V002/V003 HEAD blob hashes exactly match the frozen values using the Windows-worktree `GIT_DIR`/`GIT_WORK_TREE`; did not modify Git metadata.
- Confirmed the plan prescribes no V005+ migration/object, no application service/API/JPA/runtime wiring, no POM change, no grant outside docs/14 10.12/11.4, and no Q2/Q4/Q6/Q17/Q20/Q23/OQ-4 guess.
- Verified the plan names exact allowed files, exact method evolution (25 -> 27 -> 31), exact CI assertion evolution and exact validation commands.
- Created only `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md`.
- Did not run Maven, Flyway or the implementation suite at this plan gate; all implementation expectations are planned evidence, not executed results. The disposable probe container and temporary files were removed.
- Validated this artifact for UTF-8, LF, no conflict markers, no trailing whitespace and SHA-256; the final report records the exact result.
