# PA3 V003 Subject Number and Claim Foundation - Implementation Plan

- Task label: `pa3-v003-plan-ds-20261027-01`
- Work unit: `PA3-v003-subject-number-and-claim-foundation`
- Gate: `plan`
- Author/provider: planning Agent (`ds` gate-selected provider)
- Date: 2026-10-27
- Base: merged PR #10 / `main` commit `58c13de895cccf44c0101a865b97e19ae2afef20`; PA2 final head `96a23c4f837afe6784188bec2ee35dd16dfaa937` is an ancestor.
- Binding inputs: `docs/gateflow/project-execution-control.md` current PA3 section; `docs/12-technology-stack-and-system-architecture.md` sections 5, 6, 14 and 15 Phase A; `docs/14-production-data-model-and-flyway-schema-spec.md` sections 2.1-2.8, 3.1-3.2, 4.12-4.15, 5.3, 6.2, 7.1-7.2, 8.2, 9.0-9.0.1, 9.6-9.8, 9.10, 9.23, 10.8, 10.8.1, 11.3, 12.4, 13.5; accepted V001/V002 migrations and `PostgresContainerIT`; `backend/pom.xml`; `.github/workflows/ci.yml`; `backend/README.md`.
- Status: plan artifact only. This gate does not implement code, migration, tests, CI or README changes, does not modify the control document, and does not commit, push, open a PR, merge or enter another gate.

## 1. Goal, motivation, success signals and scope

### 1.1 Goal

Implement exactly `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` so later activation, IVR, order and evidence waves have stable business-subject, caller-number-binding, append-only claim and current risk-state parents, without application services or any V004+ object.

The deliverable is one frozen V003 migration, focused evolution of the existing single PostgreSQL 16 Testcontainers contract suite, and the minimum CI/README/evidence updates required to run and explain that persistence contract.

### 1.2 Motivation

V003 is the smallest useful next migration wave:

- `subject_number.business_subject` gives later activation, IVR, order and entitlement waves a stable subject independent of login identity, account real-name and activation.
- `subject_number.caller_number_binding` gives IVR and claims a canonical-number relation that is not a login identity.
- `subject_number.subject_claim` preserves append-only verified relation attempts so historical claim outcomes are never rewritten.
- `subject_number.subject_risk_state` gives claims and later evidence access a current, revisioned risk fact scoped to a binding.

The four parents unblock later waves without inventing enterprise ownership, role-to-claim permission, outbound-call risk blocking, provider verification or IVR/claim application services.

### 1.3 Success signals (measurable)

| Signal | Required evidence |
| --- | --- |
| Flyway applies V001, V002, V003 to a clean PostgreSQL 16 database | Fresh Testcontainers `postgres:16`; `assertCleanBeforeMigration`; `MigrateResult.migrationsExecuted = 3`; history rows `001`/`002`/`003` with exact scripts/descriptions and `success = true`; `validate()` passes; repeated `migrate()` executes zero; no pending migration. |
| Exactly four V003 tables exist with the frozen catalog | Exactly `subject_number.business_subject`, `caller_number_binding`, `subject_claim` and `subject_risk_state`; 36 columns (9/9/10/8); 25 named checks (6/7/6/6); seven ordinary immediate RESTRICT/RESTRICT FKs; seven PK/ordinary-unique constraints (4 PK + 3 UNIQUE); 18 indexes including exactly two partial unique indexes and one additional partial non-unique index; exact owners. |
| Three local `SECURITY INVOKER` immutability guards plus append-only claim control exist | `subject_number.guard_subject_owner`, `guard_caller_number_binding`, `guard_subject_risk_state`; `subject_number.reject_row_mutation`; four triggers with exact timing/events/functions; all four functions owned by `subject_number_owner`, invoker security, ordered `search_path = subject_number, pg_temp`, no transaction control. |
| Exact owner/PUBLIC/schema/table/column/function ACL and narrow verification-role boundary exist | `subject_number_owner` owns schema/tables/functions; no PUBLIC schema USAGE/CREATE, table privilege or function EXECUTE; `subject_number_role` owns only its exact column DML; `subject_identity_verification_role` has only the narrow real-name `SELECT`/`UPDATE` columns and `guard_subject_owner` EXECUTE; no DELETE/TRUNCATE/REFERENCES/TRIGGER to runtime roles. |
| Fail-closed PostgreSQL 16 evidence covers the required behavior | Exact catalog/predicate/ACL inventory; subject personal owner null-to-account set-once and account/subject real-name separation; binding lifecycle/terminality and active-number uniqueness; append-only claim/check/FK behavior; risk open/clear/re-open revision behavior; runtime/cross-module least privilege; representative active-binding concurrency. |
| V001 and V002 remain byte-for-byte unchanged | HEAD blob SHA-256 values unchanged: V001 `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`; V002 `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5`; CR-normalized diff against `HEAD` exits `0`. |
| Default and container verification pass | Default `./mvnw -B -ntp clean verify` runs 10 surefire tests with no Docker; `-Pcontainer-smoke verify` runs exactly the final 25 named container methods with `skipped=0 failures=0 errors=0`; boot smoke and fat-jar firewall pass. |

### 1.4 Scope boundary

In scope:

- Exactly one new migration: `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql`.
- Exactly four tables in `subject_number` and their columns, named checks, PK/unique constraints, indexes, partial indexes and seven ordinary immediate RESTRICT/RESTRICT FKs.
- Three local `BEFORE UPDATE` `SECURITY INVOKER` guard functions/triggers and one append-only claim function/trigger.
- Exact schema/table/column/function grants from `docs/14` 10.8/10.8.1 and the narrow `subject_identity_verification_role` boundary.
- Focused evolution of `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`.
- Minimum `.github/workflows/ci.yml`, `backend/README.md` and per-slice evidence updates.
- This plan artifact (plan gate only).

### 1.5 Non-goals

- No V004-V009 migration or object; no activation, consent, IVR task/session/call, trial, entitlement, catalog, order, payment, recording, evidence or sharing object.
- No API, controller, JPA aggregate/repository, login/session/credential/OTP, verification-provider integration, ciphertext decryption, application claim/binding/risk service, runtime DataSource/Flyway wiring, cloud resource or production provisioning.
- No enterprise ownership/transfer policy, role-to-claim permission predicate, outbound-call risk blocking rule, Q2 key rotation, Q17/Q23 provisioning or Q24 risk policy.
- No production SQL function beyond the three V003 guards and the append-only claim trigger function; no generic trigger/workflow framework.
- No change to V001, V002, `docs/12`, `docs/14`, the control document, PA1/PA2 artifacts, `backend/pom.xml`, application Java, module boundaries, the V901 broken fixture or contract-regression jobs.
- No production acceptance claim; local/CI success is build/static/container evidence only.

### 1.6 Scope interpretation

- `subject_number_role` and `subject_identity_verification_role` remain separate. The verification role can locate a subject by `id`, read only the real-name/status/revision/time columns and update only the two real-name columns plus revision/time. It cannot write owner, source, binding, claim or risk columns.
- A caller-number binding is not a login identity and creates no `auth_identity`, account, session or credential.
- Subject real-name is independent of account real-name; no V003 path writes `customer_iam.identity_profile`.
- Optional organization context never chooses or transfers enterprise ownership. V003 provides owner FK columns and the single-owner check only; fresh enterprise claims stay fail-closed because the service is out of scope.
- The migration enforces schema, constraints, indexes, FK actions, guards, append-only control and ACL. The documented service protocols in `docs/14` 9.6-9.8 and 9.10 are not implemented; only the database backstops and bounded transient test protocols listed in section 8.3 are evidenced.

## 2. Goal alignment mapping

| Design decision / slice / validation | Binding source | Confirmed goal or success signal |
| --- | --- | --- |
| Only four V003 tables | `docs/14` 4.12-4.15, 11.3 | Exact four frozen parents; no V004+ |
| Creation order subject -> binding -> claim -> risk, then FKs/guards/grants | `docs/14` 11.3 steps 1-8 | Runnable V001+V002+V003 prefix |
| Seven FKs, all ordinary immediate RESTRICT/RESTRICT | `docs/14` 5.3, 2.7 | Seven FK success signal |
| 36 columns, 25 checks, seven PK/unique, 18 indexes | `docs/14` 4.12-4.15 | Exact catalog success signal |
| Three guards and append-only claim trigger | `docs/14` 10.8.1, 11.3 step 5 | Guard/append-only success signal |
| Exact owner/PUBLIC/schema/table/column/function ACL | `docs/14` 2.1, 10.8, 10.8.1, 11.3 steps 6-8 | Least-privilege success signal |
| Owner set-once, real-name separation, binding/risk revision protocols | `docs/14` 4.12-4.15, 6.2 INV-05/06/07/33, 9.6-9.8, 9.10, 9.23 | Behavior success signals |
| V001/V002 hash preservation | Accepted prerequisites / control | No regression |
| PostgreSQL 16 container evidence only | `docs/12` 14, control | Fail-closed build evidence, not production acceptance |

## 3. Design-document alignment and direct code evidence

| Source | Binding requirement | Direct repository evidence | Plan response |
| --- | --- | --- | --- |
| `docs/12` 5-6 (206-246) | One Java codebase; module ownership; no module writes another module's tables | 14 Modulith packages; `subject_number` package exists as a boundary only | Schema-only V003; no application Java change |
| `docs/12` 14 (434-458) | Testcontainers PostgreSQL/Flyway migration and concurrency evidence | `PostgresContainerIT` starts `postgres:16`; Flyway/JDBC are test scope | Extend the same class; keep dependency scopes |
| `docs/12` 15 Phase A (460-466) | PostgreSQL/Flyway, Outbox, audit and identity foundations | V001/V002 supply platform, audit and identity/organization/employee parents | Add only V003 subject/binding/claim/risk tables |
| `docs/14` 2.1 (148-158) | Owner/runtime separation; own-schema USAGE; exact grants; PUBLIC denial | V001 creates all roles/schemas; V002 grants own-schema USAGE and cross-module reads | Grant `subject_number` USAGE to `subject_number_role` and `subject_identity_verification_role`; no extra grants |
| `docs/14` 2.4, 2.7 (175-180, 200-204) | Revision/default rules; immediate RESTRICT FKs; no cascade | V001/V002 use named checks and RESTRICT FKs | Add revision/status/time checks and seven immediate RESTRICT FKs |
| `docs/14` 4.12-4.15 (807-974) | Exact four table contracts and lifecycle rules | None of the four tables exists in V001/V002 | Reproduce the frozen columns/checks/keys/indexes; no invented column/default |
| `docs/14` 5.3 (2519-2537) | Eleven S1 FKs, of which seven belong to V003 | V002 created four FKs; V003 parents absent | Add exactly the seven V003 FKs |
| `docs/14` 6.2 (2674-2695) | INV-05/06/07/33 S1 requirements | V002 tests cover INV-01..04/27-31/40 only | Add executable V003 assertions without claiming full service flows |
| `docs/14` 9.6-9.8 (3071-3128) | Service-owned claim/binding/risk protocols and lock order | No application service or SQL service function exists | Do not implement; test only DB backstops and bounded transient protocols |
| `docs/14` 9.10 (3137-3146) | Lifecycle writer advisory/row lock contract | No runtime service exists | Deferred to later application services; V003 grants support the owned tables only |
| `docs/14` 9.23 (3273-3309) | Deterministic S1 behavior assertions | Current suite proves V001/V002 portions | Add the V003 database-contract subset; explicitly do not claim service protocol codes |
| `docs/14` 10.8 (3515-3564) | Exact V003 schema/table/column grants | V001/V002 grant only their own objects | Implement the V003 subset exactly; no extra grants |
| `docs/14` 10.8.1 (3566-3582) | Guard functions are invoker, owner-local, fixed search_path, local EXECUTE only | V001/V002 use the same pattern | Add three guards and the append-only function; no SECURITY DEFINER |
| `docs/14` 11.3 (4163-4194) | V003 creation order and grants | V001/V002 accepted at HEAD | Follow the exact eight-step V003 order |
| `docs/14` 12.4 (4283-4295) | Immutability controls summary | Audit and V002 guards exist | No DELETE/TRUNCATE runtime grants; UPDATE-only guards; append-only claim |
| `backend/src/main/resources/db/migration/V001...sql` | Frozen schemas, roles, platform/audit | HEAD blob hash `946A...` | Do not edit |
| `backend/src/main/resources/db/migration/V002...sql` | Frozen identity/organization/employee | HEAD blob hash `534A...` | Do not edit |
| `PostgresContainerIT` 280-315, 3982-5330 | Migration/inventory/ACL helpers; current 18 methods | Single container class; exact catalog and grant helpers | Rename two methods, expand helpers, add seven methods total |
| `.github/workflows/ci.yml` 56-104 | Exact container method set, no skips, boot smoke, fat-jar firewall | Expected set is the current 18 methods | Update only the expected method set/count per slice |
| `backend/README.md` 1, 45-68, 100-109 | V001+V002 description, 10/18 counts, evidence boundary | README says V003+ is absent | Update only V003 counts/description |

### 3.1 Baseline findings and disposable probe

- `HEAD` is `58c13de895cccf44c0101a865b97e19ae2afef20`. The only tracked content difference at plan start is the controller's `docs/gateflow/project-execution-control.md` PA3 update, which this gate must not modify.
- V001 HEAD blob SHA-256 is `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`.
- V002 HEAD blob SHA-256 is `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5`.
- The physical worktree files are CRLF in this Windows checkout. Raw on-disk hashes (`96e7...` for V001, `4cf2...` for V002) are not the preservation gate; HEAD blobs plus CR-normalized diffs are.
- Current `PostgresContainerIT` has exactly 18 `@Test` methods, listed in section 8.1. Its helpers already provide two-connection races, `application_name` lock-wait detection, bounded statement/future timeouts, savepoints, expected-failure helpers and FK-safe cleanup. No new test class is needed.
- A disposable PostgreSQL 16 probe (container `v003-probe`, `PostgreSQL 16.15`) applied V001, V002 and a non-repository V003 draft in one transaction per migration. It confirmed exactly 4 tables, 36 columns (9/9/10/8), 25 checks (6/7/6/6), 7 FKs, 7 PK/unique (4/3), 18 indexes (2 partial unique + 1 partial non-unique), 4 functions, 4 triggers, 15 total functions and 11 total triggers across the six schemas, and zero S2/S3 schemas. It also confirmed owner null-to-account set-once, real-name role isolation, append-only claim rejection, active-number uniqueness across subjects, and risk open/clear/re-open conditional revision behavior. The container and all `/tmp` probe files were removed; no repository file was changed.
- The probe resolved one apparent discrepancy: the success signal says "two partial unique indexes". V003 has exactly two partial **unique** indexes (`uq_binding_active_number`, `uq_claim_accepted_subject_account`) and one additional partial **non-unique** index (`ix_risk_open`). The total index count is 18. Tests assert the full 18-index inventory and explicitly distinguish uniqueness.

## 4. First-principles judgment and slice count

V003 is the smallest useful next migration because every later S1/S2 wave that stores a subject, caller binding, claim or risk fact depends on these four parents. Delivering the frozen persistence contract now gives later waves stable IDs, uniqueness and immutable/append-only backstops while avoiding coupling schema work to unresolved enterprise authorization, provider verification and IVR/risk policy.

**Two slices are the minimum that keeps each checkpoint independently falsifiable.**

1. **S1 — migration, exact catalog, ACL and structural guards.** Proves the artifact is complete, runnable, exactly the frozen inventory, and that owners/PUBLIC/column/function privileges are exact. This is one observable contract boundary: "V003 is present and is exactly the frozen persistence contract."
2. **S2 — executable data, lifecycle and concurrency backstops.** Proves the behaviors the DDL claims: owner set-once and real-name separation, binding lifecycle/terminality and active-number uniqueness, append-only claim/check/FK behavior, risk open/clear/re-open revisions, runtime privilege execution, and one representative concurrency race.

Three slices would split behavior into data-lifecycle and concurrency checkpoints, but the confirmed behavior signals are tightly related and the existing suite already provides bounded concurrency helpers; a third split would be checkpoint overhead rather than an independent contract boundary. One slice would combine artifact completeness, catalog exactness, ACL exactness and all behavior into one large review surface. Splitting by table/file/owner would be mechanical and would not establish independent observable behavior. Therefore **2 slices** is compliant with the "normally no more than three" principle.

The migration is authored once in S1. S2 adds tests only; it may not edit V003 unless a controller-approved bounded correction is required by a failing frozen-contract assertion, in which case all prior tests rerun.

## 5. Exact affected files and modules

| Path | Slice | Change | Explicit boundary |
| --- | --- | --- | --- |
| `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | S1 (authored once) | New complete V003 migration | Only four tables, 36 columns, 25 checks, seven PK/unique, 18 indexes, seven FKs, three guards, append-only claim trigger and exact grants; no V004+ |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | S1/S2 | S1: rename/expand migration and inventory methods, extend function/ACL assertions, add two S1 methods and helpers. S2: add five behavior/concurrency methods and bounded helpers. | Same class; no new test class; preserve all V001/V002 methods/behavior |
| `.github/workflows/ci.yml` | S1/S2 | Update only the failsafe method set/count assertion (18 -> 20 -> 25) and label | Docker-first, boot smoke, fat-jar firewall and contract jobs unchanged |
| `backend/README.md` | S1/S2 | Incrementally describe V003 inventory, behavior and final 25-method profile | Keep toolchain pins, module table and evidence boundary; no production claim |
| `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-implementation.md` | S1 | New evidence/completion report | Only S1 evidence |
| `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s2-implementation.md` | S2 | New evidence/completion report | Only S2 evidence |
| `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-plan.md` | plan | This plan artifact | The only path written by this gate |

Explicitly untouched: V001/V002 SQL, `backend/pom.xml`, application Java, module packages, the V901 fixture, `docs/12`, `docs/14`, the control document, PA1/PA2 artifacts, API/UI/protocol/security/deployment/acceptance assets and all migrations other than V003.

## 6. Exact V003 schema and contract

### 6.1 Migration artifact and execution order

- Filename exactly `V003__subject_number_and_claim.sql`; Flyway version `003`; expected description `subject number and claim`.
- The migration runs in Flyway's PostgreSQL transaction. It contains no top-level `COMMIT`, `ROLLBACK` or `BEGIN`, no autonomous transaction and no isolation change. `SET LOCAL ROLE` is used for object ownership; PL/pgSQL function bodies use ordinary block `BEGIN`/`END`.
- Tables, inline constraints, indexes, functions, triggers and grants are created under `subject_number_owner` using `SET LOCAL ROLE`. The migration identity adds the seven FKs after `RESET ROLE`, mirroring V002, so no cross-schema `REFERENCES` grant is invented and the owner remains the object owner.
- Execution order:

1. `SET LOCAL ROLE subject_number_owner`: create `business_subject`, its three indexes.
2. Create `caller_number_binding` with its PK/unique constraint, two indexes and partial unique index.
3. Create `subject_claim` with its PK/unique constraints, two indexes and partial unique index.
4. Create `subject_risk_state` with its PK/unique constraint and two indexes.
5. Create `subject_number.reject_row_mutation()` and `trg_subject_claim_immutable`; create the three guard functions and their triggers.
6. `REVOKE ALL` on the four tables and four functions from `PUBLIC`; grant `USAGE` on schema `subject_number` to `subject_number_role` and `subject_identity_verification_role`; apply the exact table/column/function grants in 6.6.
7. `RESET ROLE`.
8. As the migration identity, add the seven FKs in 6.4 order.
- No other object is created.

- The FKs are added by the migration identity. The disposable probe confirmed the child runtime role can insert without parent `SELECT`, and no §10.8 `REFERENCES` grant exists. If the production migration identity cannot add the FK, Q17 provisioning must resolve it; the migration must not add a speculative grant.

### 6.2 Tables

All `id` columns are `uuid NOT NULL` with no default and no sequence. All text state values are `text` with named checks. All times are `timestamptz`. Every table is owned by `subject_number_owner`. `docs/14` 4.12-4.15 remains the predicate authority; the restatement below is for generation convenience.

#### 6.2.1 `subject_number.business_subject` (9 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_business_subject`; immutable |
| `owner_account_id` | uuid | YES | none | FK `fk_subject_owner_account`; part of `ck_subject_single_owner` |
| `owner_organization_id` | uuid | YES | none | FK `fk_subject_owner_organization`; part of `ck_subject_single_owner` |
| `source` | text | NO | none | `ck_subject_source`; immutable |
| `real_name_status` | text | NO | `'UNVERIFIED'` | `ck_subject_real_name_status` |
| `real_name_verified_at` | timestamptz | YES | none | `ck_subject_real_name_shape` |
| `revision` | integer | NO | `1` | `ck_subject_revision` |
| `created_at` | timestamptz | NO | `now()` | immutable |
| `updated_at` | timestamptz | NO | `now()` | `ck_subject_time_order` |

Checks: `ck_subject_single_owner` (`num_nonnulls(owner_account_id, owner_organization_id) <= 1`), `ck_subject_source`, `ck_subject_real_name_status`, `ck_subject_real_name_shape`, `ck_subject_revision`, `ck_subject_time_order`.

Indexes: `pk_business_subject (id)`, `ix_subject_owner_account (owner_account_id)`, `ix_subject_owner_organization (owner_organization_id)`, `ix_subject_source_status (source, real_name_status)`.

#### 6.2.2 `subject_number.caller_number_binding` (9 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_caller_number_binding`; immutable |
| `subject_id` | uuid | NO | none | FK `fk_binding_subject`; part of `uq_binding_subject_number`; immutable |
| `number_type` | text | NO | none | `ck_binding_number_type`; immutable |
| `number_ciphertext` | text | NO | none | `ck_binding_number_ciphertext`; immutable |
| `number_hmac` | text | NO | none | part of `uq_binding_subject_number` and `uq_binding_active_number`; `ck_binding_number_hmac`; immutable |
| `number_masked` | text | NO | none | `ck_binding_number_masked`; immutable |
| `status` | text | NO | `'ACTIVE'` | `ck_binding_status` |
| `verified_at` | timestamptz | YES | none | `ck_binding_verified_shape` |
| `revision` | integer | NO | `1` | `ck_binding_revision` |

Checks: `ck_binding_number_type`, `ck_binding_number_ciphertext`, `ck_binding_number_hmac`, `ck_binding_number_masked`, `ck_binding_status`, `ck_binding_verified_shape`, `ck_binding_revision`.

Keys/indexes: PK; UNIQUE `uq_binding_subject_number (subject_id, number_hmac)`; partial UNIQUE `uq_binding_active_number (number_hmac) WHERE status = 'ACTIVE'`; `ix_binding_subject_status (subject_id, status)`; `ix_binding_hmac_status (number_hmac, status)`.

#### 6.2.3 `subject_number.subject_claim` (10 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_subject_claim` |
| `subject_id` | uuid | NO | none | FK `fk_claim_subject` |
| `account_id` | uuid | NO | none | FK `fk_claim_account`; part of `uq_claim_accepted_subject_account` |
| `organization_id` | uuid | YES | none | FK `fk_claim_organization` |
| `verification_method` | text | NO | none | `ck_claim_verification_method` |
| `verification_ref_ciphertext` | text | NO | none | `ck_claim_verification_ref` |
| `request_id` | text | NO | none | UNIQUE `uq_claim_request`; `ck_claim_request_id` |
| `status` | text | NO | none | `ck_claim_status`; set at insert |
| `risk_checked_at` | timestamptz | NO | none | claim-decision time |
| `verified_at` | timestamptz | YES | none | `ck_claim_verified_shape`, `ck_claim_verified_after_risk` |

Checks: `ck_claim_verification_method`, `ck_claim_verification_ref`, `ck_claim_request_id`, `ck_claim_status`, `ck_claim_verified_shape`, `ck_claim_verified_after_risk`.

Keys/indexes: PK; UNIQUE `uq_claim_request (request_id)`; partial UNIQUE `uq_claim_accepted_subject_account (subject_id, account_id) WHERE status = 'ACCEPTED'`; `ix_claim_subject_status_time (subject_id, status, risk_checked_at DESC)`; `ix_claim_account_status (account_id, status)`.

#### 6.2.4 `subject_number.subject_risk_state` (8 columns)

| Column | Type | Null | Default | Constraint / notes |
| --- | --- | --- | --- | --- |
| `id` | uuid | NO | none | PK `pk_subject_risk_state`; immutable |
| `caller_number_binding_id` | uuid | NO | none | FK `fk_risk_binding`; part of `uq_risk_binding_type`; immutable |
| `risk_type` | text | NO | none | `ck_risk_type`; part of `uq_risk_binding_type`; immutable |
| `state` | text | NO | `'OPEN'` | `ck_risk_state` |
| `source` | text | NO | none | `ck_risk_source` |
| `observed_at` | timestamptz | NO | `now()` | first observation for current record |
| `cleared_at` | timestamptz | YES | none | `ck_risk_cleared_shape`, `ck_risk_cleared_after_observed` |
| `revision` | integer | NO | `1` | `ck_risk_revision` |

Checks: `ck_risk_type`, `ck_risk_state`, `ck_risk_source`, `ck_risk_cleared_shape`, `ck_risk_cleared_after_observed`, `ck_risk_revision`.

Keys/indexes: PK; UNIQUE `uq_risk_binding_type (caller_number_binding_id, risk_type)`; partial index `ix_risk_open (caller_number_binding_id, observed_at) WHERE state = 'OPEN'`; `ix_risk_binding_state (caller_number_binding_id, state)`.

### 6.3 Exact catalog totals

| Object class | Exact total | Breakdown |
| --- | --- | --- |
| Tables | 4 | business_subject, caller_number_binding, subject_claim, subject_risk_state |
| Columns | 36 | 9 / 9 / 10 / 8 |
| Named checks | 25 | 6 / 7 / 6 / 6 |
| PK/ordinary-unique constraints | 7 | 4 PK + 3 UNIQUE |
| Indexes | 18 | 4 PK + 3 UNIQUE-constraint + 2 partial UNIQUE + 1 partial non-unique + 8 ordinary non-unique |
| Partial unique indexes | 2 | `uq_binding_active_number`, `uq_claim_accepted_subject_account` |
| Partial non-unique indexes | 1 | `ix_risk_open` |
| Foreign keys | 7 | all ordinary immediate RESTRICT/RESTRICT |
| Functions | 4 | 3 guards + 1 append-only trigger function |
| Triggers | 4 | 3 BEFORE UPDATE + 1 BEFORE UPDATE OR DELETE |

Global after V003: exactly 15 functions and 11 non-internal triggers across the six S1 schemas; zero S2/S3 schemas, roles or tables.

### 6.4 Foreign keys

All are ordinary immediate `ON UPDATE RESTRICT ON DELETE RESTRICT`, added by the migration identity after both parents exist:

| Constraint | Child -> parent |
| --- | --- |
| `fk_subject_owner_account` | `subject_number.business_subject(owner_account_id)` -> `customer_iam.customer_account(id)` |
| `fk_subject_owner_organization` | `subject_number.business_subject(owner_organization_id)` -> `organization_access.organization(id)` |
| `fk_binding_subject` | `subject_number.caller_number_binding(subject_id)` -> `subject_number.business_subject(id)` |
| `fk_claim_subject` | `subject_number.subject_claim(subject_id)` -> `subject_number.business_subject(id)` |
| `fk_claim_account` | `subject_number.subject_claim(account_id)` -> `customer_iam.customer_account(id)` |
| `fk_claim_organization` | `subject_number.subject_claim(organization_id)` -> `organization_access.organization(id)` |
| `fk_risk_binding` | `subject_number.subject_risk_state(caller_number_binding_id)` -> `subject_number.caller_number_binding(id)` |

No cascade, no deferrable FK, no `NO_FK`, no cross-schema delete privilege.

### 6.5 Functions and triggers (exact implementation)

All four functions are owned by `subject_number_owner`, `SECURITY INVOKER`, `SET search_path = subject_number, pg_temp`, have no transaction control and are not in the function-wave registry.

**`subject_number.guard_subject_owner()`** — `BEFORE UPDATE` on `business_subject`:
- Reject a change to `id`, `source` or `created_at` with `SUBJECT_OWNER_IMMUTABLE` / SQLSTATE `P0001`.
- Reject clearing or changing `owner_account_id` when `OLD.owner_account_id IS NOT NULL`.
- Reject clearing or changing `owner_organization_id` when `OLD.owner_organization_id IS NOT NULL`.
- Reject a change to `real_name_status` or `real_name_verified_at` when `current_user <> 'subject_identity_verification_role'`.
- Allow a null-to-account personal-owner set; the single-owner check remains the mutual-exclusion backstop.
- It does not add an enterprise-owner prohibition beyond the frozen text; no runtime role has `UPDATE` on `owner_organization_id`.

**`subject_number.guard_caller_number_binding()`** — `BEFORE UPDATE` on `caller_number_binding`: reject changes to `id`, `subject_id`, `number_type`, `number_ciphertext`, `number_hmac` or `number_masked` with `CALLER_NUMBER_BINDING_IMMUTABLE` / `P0001`.

**`subject_number.guard_subject_risk_state()`** — `BEFORE UPDATE` on `subject_risk_state`: reject changes to `id`, `caller_number_binding_id` or `risk_type` with `SUBJECT_RISK_STATE_IMMUTABLE` / `P0001`.

**`subject_number.reject_row_mutation()`** — `BEFORE UPDATE OR DELETE` on `subject_claim`: raise `SUBJECT_CLAIM_APPEND_ONLY` / `P0001` on every row mutation.

Exact trigger names and definitions:

```sql
CREATE TRIGGER trg_business_subject_guard
BEFORE UPDATE ON subject_number.business_subject
FOR EACH ROW EXECUTE FUNCTION subject_number.guard_subject_owner();

CREATE TRIGGER trg_caller_number_binding_guard
BEFORE UPDATE ON subject_number.caller_number_binding
FOR EACH ROW EXECUTE FUNCTION subject_number.guard_caller_number_binding();

CREATE TRIGGER trg_subject_risk_state_guard
BEFORE UPDATE ON subject_number.subject_risk_state
FOR EACH ROW EXECUTE FUNCTION subject_number.guard_subject_risk_state();

CREATE TRIGGER trg_subject_claim_immutable
BEFORE UPDATE OR DELETE ON subject_number.subject_claim
FOR EACH ROW EXECUTE FUNCTION subject_number.reject_row_mutation();
```

The local error strings are structural SQL errors, not service protocol codes and not `CLAIM_*` outcomes.

### 6.6 Grants, ownership and ACL

Schema:

- `subject_number` owner: `subject_number_owner`.
- `GRANT USAGE ON SCHEMA subject_number TO subject_number_role, subject_identity_verification_role;`
- No `CREATE` to either runtime role. No PUBLIC USAGE/CREATE.
- `subject_identity_verification_role` already has `audit` USAGE and `audit.append_fact` EXECUTE from V001; V003 does not widen audit.

Table owners: all four tables owned by `subject_number_owner`. `REVOKE ALL ON TABLE ... FROM PUBLIC` for all four.

| Table | Role | SELECT | INSERT columns | UPDATE columns |
| --- | --- | --- | --- | --- |
| `business_subject` | `subject_number_role` | all (table-level) | `id`, `source`, `revision`, `created_at`, `updated_at` | `owner_account_id`, `revision`, `updated_at` |
| `business_subject` | `subject_identity_verification_role` | `id`, `real_name_status`, `real_name_verified_at`, `revision`, `updated_at` | none | `real_name_status`, `real_name_verified_at`, `revision`, `updated_at` |
| `caller_number_binding` | `subject_number_role` | all (table-level) | all | `status`, `verified_at`, `revision` |
| `subject_claim` | `subject_number_role` | all (table-level) | all | none |
| `subject_risk_state` | `subject_number_role` | all (table-level) | all | `state`, `source`, `observed_at`, `cleared_at`, `revision` |

Critical semantics:

- `GRANT SELECT, INSERT (id, source, revision, created_at, updated_at) ON TABLE subject_number.business_subject TO subject_number_role` yields table-level `INSERT = false` and column-level INSERT true only for those five columns. Tests must assert both.
- `subject_number_role` has no INSERT privilege on either owner column and no UPDATE privilege on `owner_organization_id`.
- `subject_identity_verification_role` has no table-level SELECT or UPDATE and no INSERT; its access is column-level only.
- No runtime role receives DELETE, TRUNCATE, REFERENCES or TRIGGER on any V003 table.
- No new cross-module grant is needed: `subject_number_role` already holds V002 SELECT on `customer_account(id,status)`, `organization(id,status)` and `organization_membership(organization_id,account_id,role,status)`.

Function ACL:

| Function | EXECUTE |
| --- | --- |
| `subject_number.guard_subject_owner()` | `subject_number_role`, `subject_identity_verification_role` |
| `subject_number.guard_caller_number_binding()` | `subject_number_role` |
| `subject_number.guard_subject_risk_state()` | `subject_number_role` |
| `subject_number.reject_row_mutation()` | owner inherent only; no runtime role and no PUBLIC |

`REVOKE ALL ON FUNCTION ... FROM PUBLIC` for all four. The verification role needs `guard_subject_owner` EXECUTE because it is the only role that updates the real-name columns; it cannot update binding/risk tables and therefore needs no other guard EXECUTE.

### 6.7 DB-enforced versus transient executable-test protocol versus service-owned

| Contract | V003 DDL | Transient test protocol | Later owner |
| --- | --- | --- | --- |
| Types/nullability/defaults/status/format/revision/time-order checks | Yes | Exact normalized `pg_get_constraintdef` inventory plus violating-row probes | Backend/Database |
| PK, ordinary unique, partial unique, partial risk index | Yes | Duplicate/active-number/risk-type probes | Backend/Database |
| Seven FK targets/actions/immediacy | Yes | Missing-parent and parent-delete RESTRICT probes | Backend/Database |
| Immutable relation/owner columns and real-name role boundary | Yes (guards + column ACL) | Role- and owner-issued guard probes | Backend/Security |
| Append-only claim | Yes (trigger + no UPDATE/DELETE grants) | Runtime `42501` and owner `SUBJECT_CLAIM_APPEND_ONLY` probes | Backend/Database |
| Owner null-to-account set-once | Partly (single-owner check, guard, column ACL) | Conditional update sequence and stale-revision zero rows | subject-number service |
| Binding create/disable/re-enable/retire/replace lifecycle | Partly (checks/unique/guard) | Conditional status/revision protocol; `RETIRED` terminal is service-owned | subject-number service |
| Risk open/clear/re-open | Partly (checks/unique/revision) | Conditional status/revision protocol | subject-number service |
| Claim decision, risk preemption, replay identity, enterprise fail-closed | No | Not simulated; only append-only/check/FK backstops are tested | subject-number service (`docs/14` 9.6) |
| Canonical-number HMAC derivation, verification-ref decryption | No | Not simulated | application service + Security/Q2 |
| Advisory-lock order, audit facts, Outbox | No | Not simulated | later services |

`RETIRED` binding terminality is **not** DB-enforced: `subject_number_role` has column UPDATE on `status`, so a direct `RETIRED -> ACTIVE` update is technically possible. The service protocol forbids it; the test demonstrates the documented conditional transition (`WHERE status IN (...) AND revision = ...`) and a zero-row second retirement, not a database prohibition. The plan does not add a lifecycle trigger beyond the frozen guard.

## 7. Behavioral implementation slices

Every slice leaves the V001+V002+V003 prefix runnable and fail-closed. No application-service or V004+ work may enter a slice.

### 7.1 S1 — V003 applies after V002 and the exact catalog/ACL is present

Objective: Prove the V003 artifact is complete, runnable, exactly the frozen catalog and ACL, with V001/V002 unchanged.

Prerequisites: Accepted V001/V002 persistence foundation on merged base `58c13de...` (PA2 final head `96a23c4...`); V001/V002 HEAD hashes; Docker-capable PostgreSQL 16; current 18-method suite passing.

Allowed changes:

- `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` (new).
- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`:
  - Rename `v001ThenV002MigrateCleanPostgres16WithoutSkipping` to `v001ThenV002ThenV003MigrateCleanPostgres16WithoutSkipping`; assert three history rows, three executed migrations, V001/V002 assertions preserved, repeated migrate zero, `validate()` pass, V001/V002 smoke preserved, and a rolled-back V003 smoke that creates method-local account/organization prerequisites as the fixture identity, then as `subject_number_role` inserts one unowned subject, one ACTIVE binding, one claim and one risk row, and rolls the whole transaction back.
  - Rename `v001AndV002InventoryMatchesTheFrozenContract` to `v001ThroughV003InventoryMatchesTheFrozenContract`; expand inventory helpers for all four V003 tables, 36 columns, 25 checks, seven PK/unique, 18 indexes, seven FKs, four functions, four triggers, exact owners and no V004+ objects.
  - Add private `V003_CHECK_DEF_MAP` with exactly 25 entries of PostgreSQL 16 canonical `pg_get_constraintdef` literals (section 8.2.1), and whole-definition equality after the existing ASCII-whitespace-only normalizer.
  - Extend `functionsHaveFixedSearchPathAndNoTransactionControl` with the four `subject_number` functions.
  - Extend `assertFunctionsAndTrigger` to the exact 15-function/11-trigger inventory.
  - Extend `assertGrants`/`assertSchemaUsageExactAndNoCreate`/guard-EXECUTE helpers for the V003 matrix.
  - Add exactly two new S1 methods in section 8.1 and bounded helpers. No placeholder/skipped tests.
- `.github/workflows/ci.yml`: expected set/count 20; label updated to V003.
- `backend/README.md`: describe V001+V002+V003 inventory and count 20.
- `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-implementation.md` (new).

Invariants:

- V001/V002 HEAD blobs unchanged.
- V003 creates only the four tables, seven FKs, four functions/triggers and exact grants.
- No V004+ schema/role/table/function/trigger/index/sequence.
- No POM, application Java, runtime DataSource/Flyway or module-boundary change.
- S1 proves exact catalog and ACL; it does not claim service behavior.

Tests/commands: section 8 S1 rows; default verify; full container suite; hashes; scoped whitespace checks.

Expected assertions: 10 surefire tests; 20 container methods; `migrationsExecuted=3`; history `001`,`002`,`003`; exact catalog/ACL counts; 25 check predicates match the explicit map with zero extra/missing; V003 smoke succeeds and rolls back; all prior V001/V002 methods pass.

Completion signal: All S1 commands pass; S1 evidence artifact records exact environment/outputs/hashes; only allowed files changed.

Stop condition: Any need to edit V001/V002, add a V004+ object, change a frozen predicate, add a function/service beyond the four trigger functions, or weaken an assertion. Any unexplained catalog mismatch is fixed only within the allowed V003 migration; otherwise stop and escalate.

### 7.2 S2 — Executable data, lifecycle and concurrency backstops

Objective: Prove the database-visible V003 behavior and the bounded transient protocols required by the confirmed success signals.

Prerequisites: S1 accepted checkpoint; recorded V003 SHA-256; all S1 tests passing.

Allowed changes:

- `PostgresContainerIT.java`: add exactly five methods and bounded helpers in section 8.1/8.3. Reuse `configureBlockedWriter`, `awaitBlockedBackend`, `awaitFuture`, `expectSqlFailure`, `switchLocalRole`, `CheckProbe`, `insertCustomerAccount`, `insertOrganization` and the existing savepoint patterns. Do not change V003 or S1 catalog assertions except a necessary helper generalization.
- `.github/workflows/ci.yml`: expected set/count 25.
- `backend/README.md`: final V003 behavior, 25-method profile, hash preservation and evidence boundary.
- `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s2-implementation.md` (new).

Invariants:

- V003 SHA-256 unchanged from S1.
- No application service, SQL service function, generic trigger framework or V004+ object.
- Test data is method-local. Methods 21-24 run in a transaction that is rolled back, so no append-only claim row or other test row remains. The concurrency method commits only deletable binding/subject rows and cleans them FK-safely.
- Expected constraint failures assert exact SQLSTATE (`23514`, `23505`, `23503`, `42501`, `P0001`) and the exact constraint/error name.
- No `@Disabled`, `Assumptions` or environment skip.

Tests: section 8.3 S2 methods.

Expected assertions:

- `subjectOwnerSetOnceAndRealNameSeparation`: unowned creation; null-to-account owner set with revision+1; different/cleared owner rejected; runtime owner/binding/real-name writes denied with `42501`; verification-role real-name update allowed and owner/source denied; owner-issued real-name change rejected; all six `subject_*` checks have a violating probe.
- `bindingLifecycleTerminalityAndActiveNumberUniqueness`: all seven binding checks; ACTIVE requires `verified_at`; active-number uniqueness across subjects; `(subject_id, number_hmac)` uniqueness; conditional disable/re-enable/retire revisions; stale revision zero rows; a second conditional retire from `RETIRED` affects zero rows; guard immutable columns; FK delete RESTRICT.
- `appendOnlyClaimAndForeignKeyBackstops`: valid accepted row; runtime UPDATE/DELETE `42501`; owner UPDATE/DELETE `SUBJECT_CLAIM_APPEND_ONLY`; all six claim checks; request and accepted-duplicate uniqueness; missing-parent and parent-delete RESTRICT FKs.
- `riskOpenClearReopenRevisionProtocol`: all six risk checks; unique `(binding,type)`; conditional clear/re-open with revision; stale/wrong-state updates zero rows; guard immutable columns; FK delete RESTRICT; `OPEN` creates no dialing/IVR behavior.
- `concurrentActiveBindingUniquenessHasOneWinner`: two connections race ACTIVE bindings for the same HMAC on two subjects; one commits, the loser blocks and receives `23505`, reads the winner; exactly one active row; a rollback variant lets the loser insert; bounded blocking detection; FK-safe cleanup.

Completion signal: Full 25-method container suite passes; focused concurrency method passes in at least three executions; default verify, boot smoke and fat-jar firewall pass; hashes/whitespace clean; S2 evidence artifact complete.

Stop condition: Any needed V003 change (escalate; do not widen), any test that requires an application service or SQL service function, any append-only/cleanup violation, or any assertion weakening.

## 8. Test matrix, commands and expected assertions

### 8.1 Container IT method evolution

Current count: 18. S1 reaches 20; S2 reaches 25. No placeholder or skipped methods. CI asserts exactly the methods present at each checkpoint.

| Slice | Count | Method set |
| --- | --- | --- |
| S1 | 20 | `v001ThenV002ThenV003MigrateCleanPostgres16WithoutSkipping` (renamed), `v001ThroughV003InventoryMatchesTheFrozenContract` (renamed), `brokenMigrationFailsVisibly`, `idempotencyNewReplayConflictAndFinalizeOnce`, `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow`, `concurrentIdempotencyRetryObservesOneWinner`, `outboxReplayConflictRollbackAndDenylist`, `auditIsAppendOnlyAndRuntimeWritesAreDenied`, `auditReasonAndDenylistRulesAreEnforced`, `functionsHaveFixedSearchPathAndNoTransactionControl`, `ownerPublicAndRoleExecutionBoundariesAreExact`, `hostileSearchPathCannotRedirectFunctionEffects`, `mutableHeadsEnforceLifecycleRevisionAndNamedChecks`, `localImmutabilityGuardsRejectRelationAndRotationChanges`, `identityCollisionAndMembershipRevokeRegrantSemantics`, `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner`, `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied`, `lastEnabledAdminTransactionIsSerializedAndFailsClosed`, `v003SubjectNumberCatalogAndGuardsAreExact`, `v003OwnerPublicAndRuntimeAclsAreExact` |
| S2 | 25 | S1 set plus `subjectOwnerSetOnceAndRealNameSeparation`, `bindingLifecycleTerminalityAndActiveNumberUniqueness`, `appendOnlyClaimAndForeignKeyBackstops`, `riskOpenClearReopenRevisionProtocol`, `concurrentActiveBindingUniquenessHasOneWinner` |

S1 method responsibilities:

- `v003SubjectNumberCatalogAndGuardsAreExact`: exact four tables/owners; 36 column rows; 25 check names + normalized whole `pg_get_constraintdef` equality; seven PK/unique defs/columns; 18 whole `indexdef` values including the two partial unique and one partial non-unique index; seven FK defs and `confupdtype/confdeltype = 'r'`, `confmatchtype = 's'`, `condeferrable/condeferred = false`, `convalidated = true`; four functions with signature/owner/`prosecdef=false`/search_path/no `p_id`/no `gen_random_uuid`/no transaction control; four triggers with exact timing/events/function; no V004+ schema/role/function/trigger; no id defaults/sequences.
- `v003OwnerPublicAndRuntimeAclsAreExact`: exact schema USAGE; exact table/column ACL matrix in 6.6; `has_table_privilege` vs `has_column_privilege` distinctions; no DELETE/TRUNCATE/REFERENCES/TRIGGER to runtime roles; guard EXECUTE boundaries; PUBLIC zero on V003 schema/tables/functions.

S2 method responsibilities: section 8.3.

### 8.2 Exact catalog and ACL assertions

S1 inventory must assert at least:

| Area | Exact expectation |
| --- | --- |
| Schemas | Exactly the six existing S1 schemas with owners; no S2/S3 schema; only `public.flyway_schema_history` in `public` |
| Roles | Exactly the 15 existing NOLOGIN/NOSUPERUSER/NOCREATEDB/NOCREATEROLE roles; no new role |
| Tables | Exactly 13 V001+V002+V003 tables; four V003 tables owned by `subject_number_owner` |
| Columns | Exact 36 ordered name/type/nullability/default rows (9/9/10/8); no extra column/default |
| Checks | Exact 25 names (6/7/6/6) and whole-definition normalized equality per 8.2.1 |
| PK/unique | Exact 4 PK + 3 UNIQUE; exact defs and ordered key columns |
| Indexes | Exactly 18; exact whole `indexdef`; 2 partial unique + 1 partial non-unique |
| FKs | Exactly 7; exact names/child/parent; all RESTRICT/RESTRICT, immediate, validated |
| Functions | Exactly 15 total; four new subject_number functions with exact properties |
| Triggers | Exactly 11 total non-internal; four new V003 triggers with exact events/functions |
| Defaults/sequences | No V003 `id` default; zero sequences in the six schemas |
| Schema ACL | Exact V003 USAGE matrix; no CREATE; no PUBLIC |
| Table/column ACL | Exact V003 matrix; no cross-module or undeclared write; no DELETE/TRUNCATE/REFERENCES/TRIGGER |
| Function ACL | Exact V003 EXECUTE matrix; owner inherent; no PUBLIC |
| PUBLIC | Zero `aclexplode(...).grantee = 0` rows on V003 schemas, tables and functions |

### 8.2.1 Frozen V003 check-predicate map

The expected literals below were captured from the disposable PostgreSQL 16.15 probe after V001/V002/V003 application. The implementation must freeze these exact strings in `V003_CHECK_DEF_MAP` and compare whole normalized definitions only; substring/contains/prefix/regex/semantic matching is forbidden. When embedding them in Java, escape backslashes exactly as Java string literals require; do not alter the SQL token content. If a real `postgres:16` container differs in more than ASCII whitespace, stop and escalate; do not broaden the normalizer.

```text
business_subject|ck_subject_single_owner|CHECK ((num_nonnulls(owner_account_id, owner_organization_id) <= 1))
business_subject|ck_subject_source|CHECK ((source = ANY (ARRAY['IVR'::text, 'MINIAPP'::text, 'WEB'::text, 'OPERATIONS'::text])))
business_subject|ck_subject_real_name_status|CHECK ((real_name_status = ANY (ARRAY['UNVERIFIED'::text, 'VERIFIED'::text])))
business_subject|ck_subject_real_name_shape|CHECK (((real_name_status = 'VERIFIED'::text) = (real_name_verified_at IS NOT NULL)))
business_subject|ck_subject_revision|CHECK ((revision >= 1))
business_subject|ck_subject_time_order|CHECK ((updated_at >= created_at))
caller_number_binding|ck_binding_number_type|CHECK ((number_type = ANY (ARRAY['MOBILE'::text, 'LANDLINE'::text])))
caller_number_binding|ck_binding_number_ciphertext|CHECK ((number_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'::text))
caller_number_binding|ck_binding_number_hmac|CHECK ((number_hmac ~ '^[0-9a-f]{64}$'::text))
caller_number_binding|ck_binding_number_masked|CHECK ((((length(number_masked) >= 1) AND (length(number_masked) <= 64)) AND (btrim(number_masked) <> ''::text)))
caller_number_binding|ck_binding_status|CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text, 'RETIRED'::text])))
caller_number_binding|ck_binding_verified_shape|CHECK (((status <> 'ACTIVE'::text) OR (verified_at IS NOT NULL)))
caller_number_binding|ck_binding_revision|CHECK ((revision >= 1))
subject_claim|ck_claim_verification_method|CHECK ((verification_method = ANY (ARRAY['SMS'::text, 'WECHAT_PHONE'::text, 'CALLER_CHALLENGE'::text, 'MANUAL_REVIEW'::text])))
subject_claim|ck_claim_verification_ref|CHECK ((verification_ref_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'::text))
subject_claim|ck_claim_request_id|CHECK ((((length(request_id) >= 1) AND (length(request_id) <= 128)) AND (btrim(request_id) <> ''::text)))
subject_claim|ck_claim_status|CHECK ((status = ANY (ARRAY['ACCEPTED'::text, 'REJECTED'::text, 'CONFLICT'::text])))
subject_claim|ck_claim_verified_shape|CHECK (((status = 'ACCEPTED'::text) = (verified_at IS NOT NULL)))
subject_claim|ck_claim_verified_after_risk|CHECK (((verified_at IS NULL) OR (verified_at >= risk_checked_at)))
subject_risk_state|ck_risk_type|CHECK ((risk_type = ANY (ARRAY['RECYCLED'::text, 'CONFLICT'::text, 'MANUAL_REVIEW'::text])))
subject_risk_state|ck_risk_state|CHECK ((state = ANY (ARRAY['OPEN'::text, 'CLEARED'::text])))
subject_risk_state|ck_risk_source|CHECK ((source = ANY (ARRAY['CLAIM_CHECK'::text, 'OPERATIONS'::text, 'IMPORT'::text])))
subject_risk_state|ck_risk_cleared_shape|CHECK (((state = 'CLEARED'::text) = (cleared_at IS NOT NULL)))
subject_risk_state|ck_risk_cleared_after_observed|CHECK (((cleared_at IS NULL) OR (cleared_at >= observed_at)))
subject_risk_state|ck_risk_revision|CHECK ((revision >= 1))
```

Exact PK/unique definitions:

```text
pk_business_subject|PRIMARY KEY (id)
pk_caller_number_binding|PRIMARY KEY (id)
uq_binding_subject_number|UNIQUE (subject_id, number_hmac)
pk_subject_claim|PRIMARY KEY (id)
uq_claim_request|UNIQUE (request_id)
pk_subject_risk_state|PRIMARY KEY (id)
uq_risk_binding_type|UNIQUE (caller_number_binding_id, risk_type)
```

Exact FK definitions:

```text
fk_subject_owner_account|FOREIGN KEY (owner_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT
fk_subject_owner_organization|FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT
fk_binding_subject|FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT
fk_claim_subject|FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT
fk_claim_account|FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT
fk_claim_organization|FOREIGN KEY (organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT
fk_risk_binding|FOREIGN KEY (caller_number_binding_id) REFERENCES subject_number.caller_number_binding(id) ON UPDATE RESTRICT ON DELETE RESTRICT
```

Exact `pg_indexes.indexdef` values:

```text
CREATE UNIQUE INDEX pk_business_subject ON subject_number.business_subject USING btree (id)
CREATE INDEX ix_subject_owner_account ON subject_number.business_subject USING btree (owner_account_id)
CREATE INDEX ix_subject_owner_organization ON subject_number.business_subject USING btree (owner_organization_id)
CREATE INDEX ix_subject_source_status ON subject_number.business_subject USING btree (source, real_name_status)
CREATE UNIQUE INDEX pk_caller_number_binding ON subject_number.caller_number_binding USING btree (id)
CREATE UNIQUE INDEX uq_binding_subject_number ON subject_number.caller_number_binding USING btree (subject_id, number_hmac)
CREATE UNIQUE INDEX uq_binding_active_number ON subject_number.caller_number_binding USING btree (number_hmac) WHERE (status = 'ACTIVE'::text)
CREATE INDEX ix_binding_subject_status ON subject_number.caller_number_binding USING btree (subject_id, status)
CREATE INDEX ix_binding_hmac_status ON subject_number.caller_number_binding USING btree (number_hmac, status)
CREATE UNIQUE INDEX pk_subject_claim ON subject_number.subject_claim USING btree (id)
CREATE UNIQUE INDEX uq_claim_request ON subject_number.subject_claim USING btree (request_id)
CREATE UNIQUE INDEX uq_claim_accepted_subject_account ON subject_number.subject_claim USING btree (subject_id, account_id) WHERE (status = 'ACCEPTED'::text)
CREATE INDEX ix_claim_subject_status_time ON subject_number.subject_claim USING btree (subject_id, status, risk_checked_at DESC)
CREATE INDEX ix_claim_account_status ON subject_number.subject_claim USING btree (account_id, status)
CREATE UNIQUE INDEX pk_subject_risk_state ON subject_number.subject_risk_state USING btree (id)
CREATE UNIQUE INDEX uq_risk_binding_type ON subject_number.subject_risk_state USING btree (caller_number_binding_id, risk_type)
CREATE INDEX ix_risk_open ON subject_number.subject_risk_state USING btree (caller_number_binding_id, observed_at) WHERE (state = 'OPEN'::text)
CREATE INDEX ix_risk_binding_state ON subject_number.subject_risk_state USING btree (caller_number_binding_id, state)
```

### 8.3 Behavior assertions

S2 probes are complementary behavior evidence; they do not replace the S1 exact inventory.

- Every named check on the method's table has at least one violating-row probe asserting SQLSTATE `23514` and the exact constraint name; valid shapes are covered by successful inserts/updates in the same method.
- Subject owner: null creation, null-to-account set-once, conflict/clear rejection, real-name role isolation, guard immutability, single-owner check.
- Binding: all checks, active-number and subject-number uniqueness, conditional disable/re-enable/retire, stale revision zero rows, terminality protocol, guard/ACL denial, FK RESTRICT.
- Claim: append-only runtime `42501` and owner `P0001`, all checks, request/accepted uniqueness, missing-parent/parent-delete FKs. Claims exist only inside a rolled-back transaction.
- Risk: all checks, unique binding/type, conditional open/clear/re-open, stale zero rows, guard/ACL denial, FK RESTRICT. `OPEN` is not a dialing ban and no IVR table is touched.
- Concurrency: two connections, `application_name`, `pg_stat_activity` lock-wait detection, bounded statement/future timeouts, exact SQLSTATE on the loser, exactly one committed winner, method-local FK-safe cleanup. No sleeps-only timing assertion.

Test implementation rules:

- Use method-local UUIDs and random 64-hex HMACs; no shared fixed rows.
- Methods 21-24 run with `autoCommit=false` and roll back; use savepoints for expected failures. Never delete/truncate a claim row.
- The concurrency method commits only binding/subject rows, then deletes bindings before subjects as the migration identity. No risk/claim child remains.
- Use the migration identity only for setup/cleanup, owner guard probes and FK delete-restrict probes.
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

Expected at S1: `tests=20`; at S2/final: `tests=25`; `skipped=0 failures=0 errors=0`; exact method set from 8.1; no pending migration.

Focused concurrency repetitions (at least three successful executions):

```bash
./mvnw -B -ntp -Pcontainer-smoke verify \
  -Dit.test='PostgresContainerIT#concurrentActiveBindingUniquenessHasOneWinner'
```

Expected: `tests=1`, `skipped=0`, no failure/error in each repetition.

Focused behavior repetitions (at least once each; repeat if a failure is ambiguous):

```bash
./mvnw -B -ntp -Pcontainer-smoke verify \
  -Dit.test='PostgresContainerIT#subjectOwnerSetOnceAndRealNameSeparation'
./mvnw -B -ntp -Pcontainer-smoke verify \
  -Dit.test='PostgresContainerIT#appendOnlyClaimAndForeignKeyBackstops'
```

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
# exactly V001__..., V002__..., V003__subject_number_and_claim.sql
! grep -R -E 'V00[4-9]|V0[1-9][0-9]' backend/src/main/resources/db/migration
# CI expected set has exactly the S1 20 or final 25 names
# one TEST-*PostgresContainerIT.xml; tests == expected set size; cases == expected set;
# skipped=0 failures=0 errors=0; every surefire report skipped=0
```

Catalog no-V004+ checks are executed by the container tests (schemas/roles/functions/triggers); the source check above is only a guard against accidental migration files.

Hashes and whitespace:

```bash
git show HEAD:backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql | sha256sum
# expected: 946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045
git show HEAD:backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql | sha256sum
# expected: 534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5
git diff --ignore-cr-at-eol --exit-code HEAD -- \
  backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql \
  backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql
# expected: exit 0
sha256sum backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql
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
git status --porcelain=v1
# expected tracked/untracked entries only for the allowed slice paths plus the
# controller-owned docs/gateflow/project-execution-control.md modification
docker ps -a --format '{{.Names}}' | grep -E '(^|-)v003-probe($|-)'
# expected: no output; the planning probe was removed and Testcontainers stops its own container
# method-local row assertions: zero remaining test rows for methods 21-24 (rolled back)
# and zero remaining binding/subject rows for the concurrency method after cleanup
```

A global raw `git diff --check` is not authoritative in this Windows-checkout worktree because of the CRLF baseline and pre-existing documentation trailing whitespace. The scoped CR-aware check plus no-index new-file check (run with `-c core.autocrlf=false -c core.safecrlf=false` so the CRLF warning is not mistaken for a whitespace diagnostic) is the equivalent fail-closed gate. New/edited files must be LF; do not normalize line endings repository-wide.

### 8.5 CI assertion evolution

Update only the failsafe report assertion in `.github/workflows/ci.yml`:

- S1: exactly one `TEST-*PostgresContainerIT.xml`; `tests=20`; the S1 set in 8.1; `skipped=0`, `failures=0`, `errors=0`; every surefire report `skipped=0`.
- S2: `tests=25`; the S2 set in 8.1.

Keep unchanged: `docker info` before Maven; `-Pcontainer-smoke verify`; boot smoke; fat-jar firewall; contract regressions; permissions; path filters; toolchain pins. The assertion label changes from V002 to V003; no step is removed or weakened.

### 8.6 README evolution

Update `backend/README.md` only:

- S1: title/intro/evidence paragraphs describe V001+V002+V003; state V003 adds the four tables, 36 columns, 25 checks, seven FKs, 18 indexes, three guards, append-only claim control and exact grants; state no V004+ object; update container count to 20 and describe the renamed/new methods.
- S2: add the five behavior/concurrency methods, count 25, hash preservation, Q2/Q17/Q23/Q24 deferral and the non-production evidence boundary.
- Keep toolchain pins, module boundary table, firewall description and Modulith note unchanged.

### 8.7 V001/V002 hash and line-ending baseline

The contract check is the HEAD blob hash and the CR-normalized diff, not the raw worktree hash. V001 raw on-disk hash in this checkout is `96e7...` and V002 raw on-disk hash is `4cf2...` because of CRLF; those are not the preservation gate. Implementation agents must not normalize line endings repository-wide. New/edited files are LF.

## 9. Documentation decision

- Do not modify `docs/12`, `docs/14`, the control document or PA1/PA2 artifacts. They are binding/accepted inputs.
- Do not add architecture, API, security, deployment or schema documents. This plan is the only new design document for the work unit.
- Update `backend/README.md` as described in 8.6.
- Add one implementation completion/evidence artifact per slice at the exact paths in section 5.
- V003 SQL may carry a short header comment; comments do not change catalog behavior.

## 10. Risks, open questions, owners and destinations

| ID | Risk / open question | Classification | Owner | Destination / action |
| --- | --- | --- | --- | --- |
| R-01 | Cross-schema FK creation by the migration identity depends on Q17 provisioning. | Deferred input | Platform/Database/Security | Q17; test superuser is a fixture; add FKs as migration identity and never invent a `REFERENCES` grant |
| R-02 | Windows CRLF vs LF HEAD blobs makes raw hashes/`git status`/global `git diff --check` misleading. | Environment/tooling | Controller/Platform | Use HEAD blob hashes, CR-normalized diffs and scoped whitespace checks; do not normalize repository-wide |
| R-03 | `RETIRED` binding terminality is not DB-enforced; `status` is a runtime UPDATE column. | Service-owned behavior | subject-number service | Conditional `WHERE status/revision` protocol; a second retirement is zero rows; no new lifecycle trigger |
| R-04 | Enterprise ownership/transfer and role-to-claim permission are unresolved. | Deferred product policy | Product/Subject-number/Organization-access | Do not invent; V003 stores owner FK columns only; fresh enterprise claims stay fail-closed |
| R-05 | Q2 key rotation/KMS and verification-ref decryption are out of scope. | Deferred input | Security/Database | Q2; only envelope/HMAC checks and ciphertext columns exist |
| R-06 | Q17/Q23 production role/provisioning and `SECURITY DEFINER` capability. | Deferred input | Platform/Database/Security | Q17/Q23; V003 adds only invoker trigger functions |
| R-07 | Q24 outbound-call risk blocking is not defined. | Deferred product policy | Product/Risk/IVR | Q24; `OPEN` risk is a claim/evidence predicate only; no dialing ban |
| R-08 | Claim/binding/risk service protocols (9.6-9.8), advisory locks, audit/Outbox are service-owned. | Deferred behavior | subject-number service | Later application service; tests prove only DB backstops and bounded transient protocols |
| R-09 | Exact PG16 check/index deparse forms can vary by minor version. | Test-only fail-closed risk | Backend/QA | Freeze the 16.15-captured literals; normalize ASCII whitespace only; any real difference stops and escalates |
| R-10 | Column-level INSERT/UPDATE can be confused with table-level privileges. | Test-only risk | Backend/QA | Assert both `has_table_privilege` and `has_column_privilege`; business_subject table INSERT is false |
| R-11 | `subject_identity_verification_role` needs guard EXECUTE only for the real-name UPDATE; over-granting would widen the boundary. | Implementation check | Backend/Security | Grant EXECUTE only on `guard_subject_owner`; assert all other V003 guard EXECUTE false |
| R-12 | Append-only claim rows cannot be cleaned up by DELETE. | Test-design risk | Backend/QA | Run claim behavior in rolled-back transactions; concurrency race uses deletable binding rows only |
| R-13 | Concurrency tests can be timing-sensitive. | Bounded test risk | Backend/QA | Two connections, application_name, lock-wait detection, bounded timeouts, three repeated focused runs |
| R-14 | GitHub Actions has not run at plan/implementation gates. | Gate residual | Phaseflow/CI | Exact-head PR gate; local/CI success is not production acceptance |
| R-15 | Test-only protocols could drift into production behavior. | Scope guard | Backend/Controller | Keep them in the IT class; no service/function/trigger beyond the four trigger functions |

No blocking open question remains for planning. Q2/Q17/Q23/Q24 and enterprise ownership/role-to-claim permission are explicitly deferred and must not be guessed.

## 11. No-goal-drift and no-over-design check

| Check | Evidence |
| --- | --- |
| Only V003 exists | One migration; four tables; 36 columns; 25 checks; seven FKs; seven PK/unique; 18 indexes; four trigger functions; no V004+ |
| No later domain object | No activation/consent/IVR/trial/entitlement/catalog/order/payment/evidence/sharing object |
| No application service/API/JPA | No Java main change, no controller/repository/JPA, no runtime DataSource/Flyway |
| No binding-as-login | Binding has no provider/credential/session column and no auth FK |
| Subject real-name separate | `business_subject` real-name columns are independent; no `identity_profile` write |
| No enterprise ownership invention | Owner FKs/single-owner check only; no owner_organization UPDATE for runtime; service fail-closed deferred |
| No role-to-claim permission | No permission predicate or role predicate added |
| No Q24 dialing ban | `OPEN` risk is claim/evidence state only; no IVR object |
| No Q2/Q17/Q23 guess | Only ciphertext/HMAC shape checks; invoker functions; test superuser fixture |
| Exact least privilege | Owned/cross-module/schema/column/function matrix from 10.8; no DELETE/TRUNCATE/REFERENCES/TRIGGER; PUBLIC denied |
| No V001/V002 change | HEAD hash and CR-normalized diff checks |
| Two behavioral slices | Catalog/ACL then data/lifecycle/concurrency; not table/file/owner split |
| Minimum docs/CI/evidence | Only README, CI method set and two per-slice evidence artifacts |
| Test-only protocols are not services | Conditional lifecycle/owner/risk protocols and the binding race live only in the IT class |

## 12. Completion report format

Each implementation Agent writes exactly one new artifact at the slice path in section 5 with:

1. Task label, work unit, gate, slice, date, author/provider.
2. Binding inputs read and confirmation that the control document was not modified.
3. Base commit, accepted prior-slice checkpoint, V001/V002 HEAD hashes, V003 SHA-256 before/after, and the probe record/cleanup.
4. Exact changed paths with SHA-256 and a content-only diff manifest accounting for the CRLF baseline.
5. Scope actually implemented, including the exact four tables/36 columns/25 checks/seven FKs/seven PK-unique/18 indexes/four functions/four triggers/grants for S1, or the exact methods for S2.
6. Exact test methods added/changed and the CI expected set/count (20 or 25).
7. Exact commands, environment (JDK, Maven, Docker, PostgreSQL, Flyway), exits, and surefire/failsafe counts (`10` default; `20/25` container; `skipped=0 failures=0 errors=0`).
8. Negative and concurrency evidence: exact SQLSTATEs, constraint/error names, focused repetition counts, blocking detection and cleanup evidence.
9. V001/V002 hash and whitespace/diff evidence, including the raw-`git diff --check` baseline caveat.
10. README/CI changes and documentation decision.
11. Any deviation, stop condition or unresolved issue, with owner/destination.
12. Residual risks classified as fixed-current, later-approved-slice or later-work-unit, with owners.
13. Completion or blocked statement; explicit statement that no later gate, commit, push, PR, merge or production acceptance was performed.
14. Explicit no-goal-drift statement and exact changed-file list.

The report must not claim GitHub Actions, production provisioning or production acceptance unless those actions actually ran.

## 13. Plan validation performed by the author

- Read the PA3 control contract; `docs/12` sections 5, 6, 14 and 15 Phase A; binding `docs/14` sections 2.1-2.8, 3.1-3.2, 4.12-4.15, 5.3, 6.2, 7.1-7.2, 8.2, 9.0-9.0.1, 9.6-9.8, 9.10, 9.23, 10.8, 10.8.1, 11.3, 12.4 and 13.5; `backend/pom.xml`; `backend/README.md`; V001, V002, the full current `PostgresContainerIT` structure/helpers and `.github/workflows/ci.yml`.
- Independently derived the V003 totals from `docs/14` 4.12-4.15 and 5.3: 4 tables, 36 columns, 25 checks, 7 FKs, 7 PK/ordinary-unique, 18 indexes, 2 partial unique + 1 partial non-unique, 4 functions, 4 triggers. A disposable PostgreSQL 16.15 probe applied V001/V002 and a non-repository V003 draft and returned the same totals, global 15 functions/11 triggers and zero S2/S3 schemas; no discrepancy remains.
- Probed the database behaviors that the plan will test: owner null-to-account set-once and guard rejection; verification-role real-name update and owner denial; append-only runtime `42501` and owner `P0001`; active-number uniqueness across subjects; binding verified-shape; risk clear/re-open revision and stale zero rows. The probe container and all `/tmp` files were removed; no repository source was modified.
- Confirmed the V001/V002 HEAD blob hashes and the CRLF worktree baseline, and prescribed HEAD-hash/CR-normalized checks instead of raw worktree hashes.
- Confirmed the plan prescribes no V004+ migration/object, no application service/API/JPA/runtime wiring, no POM change, no extra grant beyond `docs/14` 2.1/10.8/10.8.1 and no enterprise/Q2/Q17/Q23/Q24 guess.
- Verified the plan names exact allowed files, exact test-method evolution (18 -> 20 -> 25), exact CI assertion evolution and exact validation commands.
- Created only `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-plan.md`.
- Did not run Maven, Flyway or the implementation suite at this plan gate; all implementation expectations are planned evidence, not executed results.
- Validated this artifact with LF/no trailing whitespace, a no-index whitespace check and SHA-256; the completion report records the exact result.
