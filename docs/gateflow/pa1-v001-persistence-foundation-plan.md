# PA1 V001 PostgreSQL Persistence Foundation - Implementation Plan

- Task label: `pa1-v001-persistence-foundation-plan`
- Work unit: `PA1-v001-persistence-foundation`
- Gate: `plan`
- Author/provider: DeepSeek-V4.1-Flash (`ds`)
- Date: 2026-10-27
- Base: `main` at `9fe3619af968d896400742d90df819bb66d868bd` (merged PR #8), branch `codex/pa1-v001-persistence-foundation`
- Binding inputs: `docs/gateflow/project-execution-control.md` PA1 binding contract, slice principle and gate record; `docs/12-technology-stack-and-system-architecture.md` sections 5, 6, 14, 15 Phase A and 18 item 1; accepted A0 contract `docs/14-production-data-model-and-flyway-schema-spec.md` sections 3, 4.3-4.5, 5.3, 6.1-6.2, 7.1-7.2, 9.0-9.3, 9.23, 10.1-10.8, 10.10, 11.1, 11.3, 12.1-12.4, 14 and 15; `backend/README.md`; `backend/pom.xml`.
- Status: plan artifact only. No code, migration, source, test, CI, commit, push, PR or implementation action is performed by this gate.

## 1. Goal, motivation and success signals

### 1.1 Goal

Implement exactly the accepted A0 V001 PostgreSQL persistence foundation so later Phase A waves can consume transaction-safe idempotency, transactional Outbox and append-only audit primitives without re-implementing or duplicating reliability behavior.

The deliverable is the V001 migration plus a fail-closed PostgreSQL 16 Testcontainers verification harness. It is not a runtime database integration, API, dispatcher, queue or domain implementation.

### 1.2 Motivation

Every later S1/S2/S3 wave depends on V001:

- `platform.idempotency_record` and its two functions are the only accepted begin/replay/conflict/finalize-once primitive (`docs/14` sections 4.3, 9.1, 10.3-10.4, INV-27).
- `platform.outbox_event` and `platform.outbox_append` are the only accepted in-transaction durable delivery primitive (`docs/14` sections 4.4, 9.2, 10.5, INV-28).
- `audit.audit_fact` and `audit.append_fact` are the only accepted append-only audit primitive (`docs/14` sections 4.5, 9.3, 10.6, INV-29).
- The four functions are the only cross-schema writes permitted in V001, and their owner, fixed `search_path`, grant and rollback boundaries are frozen by `docs/14` sections 2.1-2.8, 10.2, 10.8 and INV-40.

Implementing these frozen primitives first is the smallest useful persistence increment. Implementing identity, organization, subject or later domain tables first would duplicate reliability behavior and violate the frozen nine-wave order.

### 1.3 Success signals (measurable)

| Signal | Required evidence |
| --- | --- |
| Flyway applies V001 to a clean PostgreSQL 16 Testcontainers database without skipping | `MigrateResult.migrationsExecuted = 1`; one applied `V001__platform_schemas_roles_idempotency_outbox_audit.sql` row with `success = true`; no pending migration; a second `migrate()` executes zero migrations; `validate()` passes. |
| Exactly the V001 schemas, roles, three tables, append-only audit guard and four declared functions/grants exist | Exact inventory assertions over `pg_namespace`, `pg_roles`, `pg_class`, `pg_constraint`, `pg_index`, `pg_proc`, `pg_trigger` and `has_*_privilege`; no V002+ schema, role, table, function or trigger. |
| Idempotency `NEW/REPLAY/CONFLICT` and finalize-once behavior | `NEW` at revision 1 with null result fields; `REPLAY` returns the stored first result; `CONFLICT` returns no result and has no side effect; `complete` succeeds once, `ALREADY_COMPLETED` on the same hash, conflict on a different hash/revision; a committed `NEW` raises `IDEMPOTENCY_INCOMPLETE` and is visible through `ix_idempotency_new_alert`. |
| Outbox same-key replay/conflict and transactional rollback | Same `(event_domain, dedupe_key)` and same hash returns the existing id; a different hash raises `OUTBOX_DEDUPE_CONFLICT`; a caller rollback leaves no Outbox row. |
| Audit append-only behavior | Appends through `audit.append_fact`; every update/delete, including by `audit_owner`, raises `AUDIT_APPEND_ONLY`; runtime roles have no insert/update/delete/truncate privilege; a caller rollback leaves no audit row. |
| Fixed `search_path` and function execution boundaries | All four functions are `SECURITY DEFINER`, owned by the correct NOLOGIN/NOSUPERUSER/NOCREATEDB owner, declare the exact ordered `search_path`, contain no transaction control, and are executable only by the declared roles; `PUBLIC` has no schema `USAGE`/`CREATE`, table privilege or function `EXECUTE`; a hostile caller `search_path` cannot redirect effects. |
| Migration failure is visible rather than silently skipped | The container profile is explicit and fail-closed: no `@Disabled`, no `Assumptions`, no `disabledWithoutDocker`, no environment skip; Docker unavailable, image pull failure, container start failure, JDBC failure, Flyway failure or assertion failure fails the Maven build; a test-only broken migration raises `FlywayException` and rolls back its partial DDL. |
| Default verification and container profile both pass | `./mvnw -B -ntp clean verify` passes 10 surefire tests with no container; `./mvnw -B -ntp -Pcontainer-smoke verify` passes exactly one `PostgresContainerIT` suite with 12 tests, `skipped=0`, `failures=0`, `errors=0`. |
| Production runtime firewall preserved | The production fat jar still contains no `spring-security`, `flyway`, `jooq`, `HikariCP`, `postgresql` or `testcontainers`; the application still starts with no DataSource, Flyway bean or scheduler. |

## 2. Scope boundary and explicit non-goals

### 2.1 In scope

- `backend/pom.xml` test-scope Flyway wiring and the minimum firewall adjustment required by it.
- Exactly one new migration: `V001__platform_schemas_roles_idempotency_outbox_audit.sql`.
- Exactly the V001 schemas, roles, tables, checks, indexes, audit trigger and four declared functions from the accepted A0 contract.
- The exact V001 grant/revoke, ownership, fixed `search_path` and security boundaries in `docs/14` sections 2, 3.1, 4.3-4.5, 10.2-10.8 and 11.3.
- PostgreSQL 16 Testcontainers tests proving migration application, no silent skip, exact inventory, idempotency, Outbox, audit append-only, and execution/privilege boundaries.
- A bounded `backend/README.md` update and a bounded `.github/workflows/ci.yml` container-report assertion update.
- This plan artifact.

### 2.2 Strict non-goals

- No V002-V009 migration, table, function, role, schema or behavior. In particular, no `issue_first_trial`, no identity/organization/employee/subject/number/claim/risk tables.
- No customer identity, login, organization membership, business subject, number binding, consent, activation, IVR task/session/call/decision, trial, catalog, order, payment, entitlement, usage, evidence, media, sharing, AI, notary, refund, invoice or support behavior.
- No API, OpenAPI, UI, miniapp, operations console, authentication or authorization service.
- No production database, cloud resource, KMS, COS, Redis, RabbitMQ, secret, credential or deployment role provisioning.
- No queue, dispatcher process, consumer, scheduler, retry worker or DLQ implementation. V001 creates only the Outbox table, the append function and the dispatcher column grants; it does not dispatch.
- No guess of Q17 (Flyway role provisioning/ownership transfer) or Q23 (deployment role `SECURITY DEFINER` capability). The test container's superuser is a test fixture, not a production provisioning claim.
- No guessed production values: no retention period, TTL, capacity, SLO/RPO/RTO, region, SKU, provider identifier or deployment capability.
- No change to `docs/14`, any accepted A0 artifact, the frozen 44-table/40-invariant/9-wave/3-slice baseline, the four V001 function contracts or later-wave behavior.
- No additional declared SQL functions. The only extra database function in V001 is the accepted local trigger function `audit.reject_row_mutation()`; the denylist is validated inline, not through a fifth helper function.
- No `INSERT`, `UPDATE` or `DELETE` path into platform/audit tables other than the four accepted functions and the dispatcher column grant.
- No production runtime Flyway/DataSource/Hikari/jOOQ/Spring Security wiring. Flyway is test scope only.

### 2.3 Scope-boundary interpretation

V001 may create the six empty S1 schemas (`platform`, `audit`, `customer_iam`, `organization_access`, `operations_iam`, `subject_number`) and the 15 S1 NOLOGIN roles required by `docs/14` section 3.1 and section 11.3 step 2. It must not create any S2/S3 schema (`catalog_order`, `ivr`, `entitlement_metering`, `activation_consent`, `evidence`, `sharing`) or any S2/S3 role. The four non-platform/audit S1 schemas remain empty in V001; their tables arrive only in V002/V003, which are out of scope.

## 3. Source alignment and direct code evidence

| Source | Binding requirement | Direct code/repository evidence | Plan response |
| --- | --- | --- | --- |
| `docs/12` section 5 (lines 206-220) | One Java codebase, PostgreSQL transaction boundary; API/worker/scheduler are later deployment units | `backend/src/main/java/com/onecall/core/OneCallApplication.java` is a single bootstrap entry; no worker/scheduler code exists | Keep one runtime artifact; add only test-scope migration verification; no worker/scheduler |
| `docs/12` section 6 (lines 222-246) | Module ownership; no module writes another module's tables; cross-module writes through published interfaces | 14 package boundaries in `backend/src/main/java/com/onecall/core/*`; `ModuleBoundaryTests` enforces allowed dependencies | V001 functions are the only cross-schema write path; no domain module is changed |
| `docs/12` section 14 (lines 434-458) | Testcontainers PostgreSQL/Flyway tests and migration verification | `backend/README.md:48-59`; `PostgresContainerIT` starts `postgres:16`; no Flyway dependency yet | Add Flyway test scope and a fail-closed V001 Testcontainers IT |
| `docs/12` section 15 Phase A (lines 460-466) | Establish PostgreSQL/Flyway, Outbox, audit and security boundaries | `backend/README.md:3` states there is no domain behavior, table or migration | Deliver only the V001 persistence foundation; defer identity/domain and runtime integration |
| `docs/12` section 18 item 1 (lines 520-525) | Data model and initial Flyway schema first | `docs/14` is the accepted A0 data-model/schema contract | Implement only its V001 wave |
| `docs/14` sections 1.3, 4.3-4.5 (lines 71-87, 462-596) | Exact S1 membership and V001 table dictionaries | `docs/14` names the three V001 tables and every column/check/index | Reproduce exactly those three tables; no extras |
| `docs/14` sections 2.1-2.8 (lines 148-212) | Schemas/roles/ownership; NOLOGIN owners; fixed `search_path`; `SECURITY DEFINER`; no transaction control; `PUBLIC` denied | `backend/pom.xml` currently bans Flyway/JDBC/Hikari/jOOQ/security; no DB code exists | Create only the accepted schemas/roles; implement the four functions with the exact security contract |
| `docs/14` sections 3.1-3.2 (lines 264-338) | 15 S1 roles; platform/audit write paths; dispatcher columns; audit read-only | No current role/schema exists | Create the 15 S1 roles and six S1 schemas only |
| `docs/14` section 5.3 (lines 2519-2537) | S1 FKs are ordinary and no V001 table has an FK | V001 tables are platform/audit primitives | No FKs in V001; no FK catalog rows added |
| `docs/14` sections 6.1-6.2 (lines 2629-2695) | INV-27, INV-28, INV-29, INV-30, INV-31 and the S1 portion of INV-40 | Accepted assertions are document-level only | Convert them into executable V001 tests |
| `docs/14` sections 7.1-7.2 (lines 2744-2769) | `NEW -> COMPLETED` once; `PENDING/PUBLISHED/FAILED`; append-only audit | No state code exists | Implement state via table checks/functions only; no stored outcome states |
| `docs/14` sections 9.0-9.3, 9.23 (lines 2949-3051, 3273-3309) | `READ COMMITTED`; same-transaction rollback; unique/row-lock serialization; deterministic S1 assertions | No transaction code exists | Use unique key + `FOR UPDATE`; no advisory locks or inner transaction control |
| `docs/14` sections 10.1-10.8, 10.10 (lines 3414-3608) | Exact four signatures, outcomes, errors, grant matrix, function verification | No function exists | Implement exactly four signatures and the matrix; add executable verification |
| `docs/14` sections 11.1, 11.3 (lines 4092-4194) | V001 creates schemas, roles, three tables, audit trigger and four functions in that order | No migration directory exists | Use the exact V001 filename and intra-wave order |
| `docs/14` sections 12.1-12.4 (lines 4257-4295) | No ordinary delete; append-only audit; idempotency/outbox mutable-head restrictions | No tables exist | Revoke delete/truncate; add the audit-only trigger; no delete path |
| `docs/14` sections 14, 15 (lines 4772-4868) | Q17/Q23 deferred; no over-design; no guessed production values | No deployment/DB provisioning exists | Keep Q17/Q23 open; test with a superuser only; no production provisioning claim |
| `backend/README.md:3` | No domain behavior, table, migration, API, auth, vendor or cloud integration | Confirmed by the file tree | PA1 changes only the V001 persistence foundation and its test evidence |
| `backend/pom.xml:95-106` | Enforcer bans security, JPA, JDBC starter, Flyway, jOOQ, Hikari | Current rule bans Flyway at every scope | Narrow the Flyway ban to compile/runtime; keep all other bans; prove the production jar firewall |
| `backend/src/test/java/com/onecall/archtests/architecture/BootstrapDependencyBoundaryTests.java:11-24` | Flyway must not be on the bootstrap/surefire classpath | Current test asserts `org.flywaydb.core.Flyway` is absent | Preserve this test unchanged by excluding Flyway from the surefire classpath; Flyway is present only in the failsafe container IT |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java:47-82` | PA0 clean-database smoke; no silent skip | Starts `postgres:16`, asserts empty DB and no Flyway history | Retain the clean-before-migration assertion, then extend the same class with V001 migration/behavior tests |
| `backend/src/test/java/com/onecall/archtests/OneCallApplicationTests.java:17-23` | No DataSource or Flyway bean in the bootstrap context | Current assertions | Keep application runtime unchanged; test-scope Flyway does not create a bean without a DataSource |
| `.github/workflows/ci.yml:56-101` | Docker first; container smoke report assertions; fat-jar firewall | Current assertion expects one `PostgresContainerIT` test | Update only the report assertion to the V001 test count/method set; keep Docker, boot smoke and jar firewall unchanged |

### 3.1 The PA0 guardrail tension and resolution

PA0's `BootstrapDependencyBoundaryTests` is a proxy for the production firewall, not a production contract. PA1 needs Flyway to prove the migration. The plan resolves this without weakening the production boundary:

1. Flyway is added only at `test` scope.
2. Maven Enforcer continues to ban Flyway at `compile` and `runtime` scope.
3. The surefire plugin excludes the two Flyway artifacts from the unit-test classpath, so the existing `BootstrapDependencyBoundaryTests` remains byte-for-byte valid and unchanged.
4. The failsafe container IT includes Flyway and runs only under the explicit `container-smoke` profile.
5. The CI production fat-jar check remains unchanged and authoritative.
6. The default `verify` still runs 10 surefire tests, requires no Docker, and proves no Flyway bean/DataSource in the application context.

This is a bounded test-infrastructure adjustment, not a runtime dependency or domain expansion.

## 4. Affected files and modules

| Path | Slice | Change | Explicit boundary |
| --- | --- | --- | --- |
| `backend/pom.xml` | S1 | Add test-scope `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql`; replace the Flyway Enforcer exclude with compile/runtime scope-qualified excludes; add surefire `classpathDependencyExcludes` for the two Flyway artifacts | No new compile/runtime dependency; all other existing Enforcer bans unchanged |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | S1 | New complete V001 migration | Only the accepted V001 objects; no V002+ object |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | S1/S2/S3 | Replace the single PA0 smoke method with the 12 V001 tests and a shared migrated container | Same class name and failsafe include; no Docker skip |
| `backend/src/test/resources/db/migration-broken/V001__broken.sql` | S1 | New test-only invalid migration | Never on the production classpath; used only by the failure-visibility test |
| `backend/README.md` | S1/S3 | Update the PA1 V001 description, commands, test count, Flyway test-scope firewall and evidence boundary | No claim of production acceptance |
| `.github/workflows/ci.yml` | S1/S3 | Update the failsafe report assertion to 12 V001 tests and their exact method names | Keep `docker info` first, boot smoke and fat-jar firewall unchanged |
| `docs/gateflow/pa1-v001-persistence-foundation-plan.md` | plan | This artifact | No implementation |

Explicitly unchanged:

- `backend/src/main/java/**`, including all 14 module packages, `package-info.java`, `ModuleMarker` and `OneCallApplication`.
- `backend/src/main/resources/application.yml`.
- `backend/src/test/java/com/onecall/archtests/OneCallApplicationTests.java`.
- `backend/src/test/java/com/onecall/archtests/architecture/BootstrapDependencyBoundaryTests.java`.
- `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`.
- `docs/14-production-data-model-and-flyway-schema-spec.md`, all A0 artifacts and all review/gate history.
- Any V002+ migration, table, role, schema, function, API, UI, cloud, secret or deployment artifact.

## 5. Exact V001 migration, schema, function, grant and security decisions

### 5.1 Migration artifact, filename and execution order

- Exact file: `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql`.
- Flyway location: `classpath:db/migration`.
- Flyway history table: default `public.flyway_schema_history`. It is migration metadata, not a business table, and is explicitly excluded from the business inventory. The V001 migration neither creates nor grants it.
- The migration uses PostgreSQL transactional DDL. It contains no `BEGIN`, `COMMIT`, `ROLLBACK`, `START TRANSACTION`, `SET TRANSACTION`, autonomous transaction or Flyway callback.
- Ownership is established with `SET LOCAL ROLE <owner>` blocks and `RESET ROLE`; Flyway's default transactional execution for PostgreSQL is required. The tests assert every schema/table/function owner; a wrong owner fails.
- Intra-wave order, matching `docs/14` section 11.3:
  1. `REVOKE ALL ON SCHEMA public FROM PUBLIC;`
  2. Create the 15 V001 NOLOGIN roles.
  3. Create the six S1 schemas with `AUTHORIZATION <owner>`.
  4. `REVOKE ALL ON SCHEMA platform, audit, customer_iam, organization_access, operations_iam, subject_number FROM PUBLIC;`
  5. As `platform_reliability_owner`: create `platform.idempotency_record`, then `platform.outbox_event`, with their named checks and indexes.
  6. As `platform_reliability_owner`: create `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`; revoke all from `PUBLIC`; apply the exact grants.
  7. As `audit_owner`: create `audit.audit_fact` with its named checks and indexes.
  8. As `audit_owner`: create `audit.reject_row_mutation()` and `trg_audit_fact_immutable`; revoke all from `PUBLIC`.
  9. As `audit_owner`: create `audit.append_fact`; revoke all from `PUBLIC`; apply the exact grants.
  10. `RESET ROLE;` at the end. No `entitlement_metering.issue_first_trial` is created.
- No `IF NOT EXISTS` on roles, schemas, tables or functions. A dirty database fails visibly instead of silently accepting an unknown object.

### 5.2 Schemas, roles and ownership

Six schemas, each owned by its NOLOGIN owner:

| Schema | Owner | V001 contents |
| --- | --- | --- |
| `platform` | `platform_reliability_owner` | `idempotency_record`, `outbox_event`, three functions |
| `audit` | `audit_owner` | `audit_fact`, `append_fact`, `reject_row_mutation`, trigger |
| `customer_iam` | `customer_iam_owner` | empty in V001 |
| `organization_access` | `organization_access_owner` | empty in V001 |
| `operations_iam` | `operations_iam_owner` | empty in V001 |
| `subject_number` | `subject_number_owner` | empty in V001 |

15 roles, all `CREATE ROLE <name> NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;`:

1. `platform_reliability_owner`
2. `platform_reliability_role`
3. `platform_dispatcher_role`
4. `audit_owner`
5. `audit_reader_role`
6. `customer_iam_owner`
7. `customer_iam_role`
8. `organization_access_owner`
9. `organization_access_role`
10. `operations_iam_owner`
11. `operations_iam_role`
12. `subject_number_owner`
13. `subject_number_role`
14. `subject_identity_verification_role`
15. `operations_service_role`

`NOLOGIN`, `NOSUPERUSER` and `NOCREATEDB` are required by `docs/14` section 3.1. `NOCREATEROLE` is the explicit least-privilege PostgreSQL default and is asserted. No role membership, LOGIN credential, password or `SET ROLE` grant is created; the Testcontainers superuser can `SET LOCAL ROLE` without membership. Production role provisioning remains Q17/Q23.

`PUBLIC` receives:

- no `USAGE` and no `CREATE` on `public` or any of the six business schemas;
- no table privilege;
- no function `EXECUTE`.

### 5.3 V001 tables, columns, checks and indexes

No table has a foreign key, sequence, `id` default, `ON DELETE` rule, revision on the audit table or any V002+ column.

#### 5.3.1 `platform.idempotency_record`

| Column | Type | Null | Default |
| --- | --- | --- | --- |
| `id` | `uuid` | NO | none; generated by `platform.idempotency_begin` with `pg_catalog.gen_random_uuid()` |
| `scope` | `text` | NO | none |
| `idempotency_key` | `text` | NO | none |
| `request_fingerprint` | `text` | NO | none |
| `status` | `text` | NO | `'NEW'` |
| `result_type` | `text` | YES | none |
| `result_id` | `uuid` | YES | none |
| `result_snapshot` | `jsonb` | YES | none |
| `result_sha256` | `text` | YES | none |
| `created_at` | `timestamptz` | NO | `now()` |
| `completed_at` | `timestamptz` | YES | none |
| `revision` | `integer` | NO | `1` |

Named checks:

- `ck_idempotency_scope`: `scope IN ('IVR_CONFIRM','PURCHASE')`.
- `ck_idempotency_key`: `length(idempotency_key) BETWEEN 1 AND 512 AND btrim(idempotency_key) <> ''`.
- `ck_idempotency_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.
- `ck_idempotency_status`: `status IN ('NEW','COMPLETED')`.
- `ck_idempotency_result_type`: `result_type IS NULL OR (length(result_type) BETWEEN 1 AND 64 AND btrim(result_type) <> '')`.
- `ck_idempotency_result_snapshot_type`: `result_snapshot IS NULL OR jsonb_typeof(result_snapshot) IN ('object','array')`.
- `ck_idempotency_result_sha256`: `result_sha256 IS NULL OR result_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_idempotency_revision`: `revision >= 1`.
- `ck_idempotency_state_shape`: `(status = 'NEW' AND result_type IS NULL AND result_id IS NULL AND result_snapshot IS NULL AND result_sha256 IS NULL AND completed_at IS NULL AND revision = 1) OR (status = 'COMPLETED' AND result_type IS NOT NULL AND result_id IS NOT NULL AND result_snapshot IS NOT NULL AND result_sha256 IS NOT NULL AND completed_at IS NOT NULL AND revision = 2)`.
- `ck_idempotency_completed_after_created`: `completed_at IS NULL OR completed_at >= created_at`.

Keys/indexes:

- `pk_idempotency_record` primary key `(id)`.
- `uq_idempotency_scope_key` unique `(scope, idempotency_key)`.
- `ix_idempotency_new_alert` on `(created_at)` where `status = 'NEW'`.

#### 5.3.2 `platform.outbox_event`

| Column | Type | Null | Default |
| --- | --- | --- | --- |
| `id` | `uuid` | NO | none; generated by `platform.outbox_append` with `pg_catalog.gen_random_uuid()` |
| `event_domain` | `text` | NO | none |
| `dedupe_key` | `text` | NO | none |
| `aggregate_type` | `text` | NO | none |
| `aggregate_id` | `uuid` | NO | none |
| `event_type` | `text` | NO | none |
| `payload_redacted` | `jsonb` | YES | none |
| `payload_ciphertext` | `text` | YES | none |
| `payload_sha256` | `text` | NO | none |
| `status` | `text` | NO | `'PENDING'` |
| `attempt_count` | `integer` | NO | `0` |
| `available_at` | `timestamptz` | NO | `now()` |
| `published_at` | `timestamptz` | YES | none |
| `revision` | `integer` | NO | `1` |

Named checks:

- `ck_outbox_event_domain`: `event_domain IN ('ACTIVATION','IVR_CONFIRM','ACTIVATION_STATUS','OPERATIONS','IVR_EVENT','IVR_DIRECT_AUTH','EVIDENCE','PAYMENT','SHARE_ACCESS','PURCHASE','SUBJECT_CLAIM')`.
- `ck_outbox_dedupe_key`: `length(dedupe_key) BETWEEN 1 AND 512 AND btrim(dedupe_key) <> ''`.
- `ck_outbox_aggregate_type`: `length(aggregate_type) BETWEEN 1 AND 128 AND btrim(aggregate_type) <> ''`.
- `ck_outbox_event_type`: `length(event_type) BETWEEN 1 AND 128 AND btrim(event_type) <> ''`.
- `ck_outbox_payload_present`: `payload_redacted IS NOT NULL OR payload_ciphertext IS NOT NULL`.
- `ck_outbox_ciphertext_shape`: `payload_ciphertext IS NULL OR payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_outbox_payload_sha256`: `payload_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_outbox_status`: `status IN ('PENDING','PUBLISHED','FAILED')`.
- `ck_outbox_attempt_count`: `attempt_count >= 0`.
- `ck_outbox_published_shape`: `(status = 'PUBLISHED') = (published_at IS NOT NULL)`.
- `ck_outbox_published_after_available`: `published_at IS NULL OR published_at >= available_at`.
- `ck_outbox_revision`: `revision >= 1`.

Keys/indexes:

- `pk_outbox_event` primary key `(id)`.
- `uq_outbox_domain_dedupe` unique `(event_domain, dedupe_key)`.
- `ix_outbox_pending_available` on `(available_at, id)` where `status = 'PENDING'`.
- `ix_outbox_failed_available` on `(available_at, id)` where `status = 'FAILED'`.

`aggregate_id` is an intentional polymorphic internal reference and is not a foreign key.

#### 5.3.3 `audit.audit_fact`

| Column | Type | Null | Default |
| --- | --- | --- | --- |
| `id` | `uuid` | NO | none; generated by `audit.append_fact` with `pg_catalog.gen_random_uuid()` |
| `occurred_at` | `timestamptz` | NO | none; caller-supplied |
| `actor_type` | `text` | NO | none |
| `actor_id` | `uuid` | YES | none |
| `action_code` | `text` | NO | none |
| `object_type` | `text` | NO | none |
| `object_id` | `uuid` | YES | none |
| `reason_code` | `text` | YES | none |
| `before_snapshot` | `jsonb` | YES | none |
| `after_snapshot` | `jsonb` | YES | none |
| `request_id` | `text` | YES | none |
| `trace_id` | `text` | YES | none |

Named checks:

- `ck_audit_actor_type`: `actor_type IN ('ACCOUNT','EMPLOYEE','SERVICE','SYSTEM')`.
- `ck_audit_actor_id`: `(actor_type = 'SYSTEM') OR actor_id IS NOT NULL`.
- `ck_audit_action_code`: `action_code ~ '^[A-Z][A-Z0-9_]{1,127}$'`.
- `ck_audit_object_type`: `length(object_type) BETWEEN 1 AND 64 AND btrim(object_type) <> ''`.
- `ck_audit_object_id`: `object_id IS NOT NULL OR object_type = 'SYSTEM'`.
- `ck_audit_reason_code`: `reason_code IS NULL OR (length(reason_code) BETWEEN 1 AND 128 AND btrim(reason_code) <> '')`.
- `ck_audit_before_type`: `before_snapshot IS NULL OR jsonb_typeof(before_snapshot) IN ('object','array')`.
- `ck_audit_after_type`: `after_snapshot IS NULL OR jsonb_typeof(after_snapshot) IN ('object','array')`.
- `ck_audit_request_id`: `request_id IS NULL OR length(request_id) BETWEEN 1 AND 128`.
- `ck_audit_trace_id`: `trace_id IS NULL OR length(trace_id) BETWEEN 1 AND 128`.

Keys/indexes:

- `pk_audit_fact` primary key `(id)`.
- `ix_audit_object_time` on `(object_type, object_id, occurred_at DESC)`.
- `ix_audit_actor_time` on `(actor_type, actor_id, occurred_at DESC)`.
- `ix_audit_action_time` on `(action_code, occurred_at DESC)`.
- `ix_audit_occurred_at` on `(occurred_at DESC)`.

`audit.audit_fact` has no revision column and is append-only.

### 5.4 Append-only audit guard

- `audit.reject_row_mutation()` is `SECURITY INVOKER`, owned by `audit_owner`, contains no transaction control and is not in the four-function registry.
- It raises `RAISE EXCEPTION 'AUDIT_APPEND_ONLY' USING ERRCODE = 'P0001';` for every row.
- `trg_audit_fact_immutable` is `BEFORE UPDATE OR DELETE ON audit.audit_fact FOR EACH ROW EXECUTE FUNCTION audit.reject_row_mutation();`.
- `REVOKE ALL ON FUNCTION audit.reject_row_mutation() FROM PUBLIC;` No runtime `EXECUTE` is granted; the trigger is a structural guard, not a service API.
- No runtime role receives `UPDATE`, `DELETE` or `TRUNCATE`; `audit_reader_role` receives only `SELECT`.
- The trigger blocks `audit_owner` and superuser updates/deletes as well. `TRUNCATE` remains blocked by privilege for every runtime role; the accepted contract does not add a `TRUNCATE` trigger.

### 5.5 The four declared SQL functions

All four are:

- `SECURITY DEFINER`;
- owned by the named NOLOGIN owner;
- declared with `SET search_path = <owning schema>, pg_temp` and schema-qualified object references;
- free of `COMMIT`, `ROLLBACK`, `START TRANSACTION`, `SET TRANSACTION`, autonomous transactions and transaction-level `SET`;
- exception-propagating, never swallowing a database exception;
- revoked from `PUBLIC`;
- executable only by the roles in section 5.6;
- validated against the protected-key denylist and encrypted-envelope shape where a snapshot/payload is accepted.

Error convention: `RAISE EXCEPTION '<STABLE_CODE>' USING ERRCODE = 'P0001';`. The stable code is the contract; tests assert it.

#### 5.5.1 `platform.idempotency_begin`

Signature:

`platform.idempotency_begin(p_scope text, p_key text, p_fingerprint text) RETURNS TABLE (record_id uuid, revision integer, outcome text, result_type text, result_id uuid, result_snapshot jsonb, result_sha256 text)`

- Owner: `platform_reliability_owner`; `SET search_path = platform, pg_temp`.
- Validation: `p_scope IN ('IVR_CONFIRM','PURCHASE')`; `p_key` length 1-512 and nonblank; `p_fingerprint` lowercase 64-hex. Otherwise `IDEMPOTENCY_ARGUMENT_INVALID`.
- Insert: `INSERT INTO platform.idempotency_record (id, scope, idempotency_key, request_fingerprint, status, revision) VALUES (pg_catalog.gen_random_uuid(), p_scope, p_key, p_fingerprint, 'NEW', 1) ON CONFLICT (scope, idempotency_key) DO NOTHING RETURNING id, revision`.
- If inserted: return `(record_id, 1, 'NEW', NULL, NULL, NULL, NULL)`.
- If not inserted: `SELECT ... FOR UPDATE` the existing row by `(scope, idempotency_key)`.
  - `status = 'COMPLETED'` and `request_fingerprint = p_fingerprint`: return `(id, revision, 'REPLAY', result_type, result_id, result_snapshot, result_sha256)`.
  - `status = 'COMPLETED'` and different fingerprint: return `(NULL, NULL, 'CONFLICT', NULL, NULL, NULL, NULL)` with no side effect.
  - `status = 'NEW'`: raise `IDEMPOTENCY_INCOMPLETE` (a committed or same-transaction `NEW` row is not a supported success path).
- No stored result is updated by this function. No `p_id` argument and no table `id` default.
- It takes no advisory lock; the unique key and the `FOR UPDATE` row lock are the V001 serialization. Later callers must acquire their domain locks before calling it per `docs/14` section 9.0/9.0.1.

#### 5.5.2 `platform.idempotency_complete`

Signature:

`platform.idempotency_complete(p_record_id uuid, p_expected_revision integer, p_result_type text, p_result_id uuid, p_result_snapshot jsonb, p_result_sha256 text) RETURNS text`

- Owner: `platform_reliability_owner`; `SET search_path = platform, pg_temp`.
- Validation: record id non-null; expected revision >= 1; result type length 1-64 and nonblank; result id non-null; result snapshot non-null, object/array or the single-key encrypted envelope `{"enc":"v1.<key_ref>.<base64url>"}`; result hash lowercase 64-hex; denylist/envelope valid. Otherwise `IDEMPOTENCY_ARGUMENT_INVALID`.
- One conditional update: `UPDATE platform.idempotency_record SET status = 'COMPLETED', result_type = p_result_type, result_id = p_result_id, result_snapshot = p_result_snapshot, result_sha256 = p_result_sha256, completed_at = pg_catalog.clock_timestamp(), revision = revision + 1 WHERE id = p_record_id AND status = 'NEW' AND revision = p_expected_revision`.
- If one row updates: return `'COMPLETED'`.
- On zero rows: `SELECT ... FOR UPDATE` by id and map deterministically:
  - missing: `IDEMPOTENCY_NOT_FOUND`;
  - `COMPLETED` with the same `result_sha256`: return `'ALREADY_COMPLETED'` without changing anything;
  - `COMPLETED` with a different hash: `IDEMPOTENCY_CONFLICT`;
  - `NEW` with a mismatched revision: `IDEMPOTENCY_CONFLICT`;
  - `NEW` with the expected revision: retry the conditional update once under the row lock; return `'COMPLETED'` if one row updates, otherwise `IDEMPOTENCY_CONFLICT`;
  - any other state: `IDEMPOTENCY_CONFLICT`.
- A completed result is never overwritten. `completed_at` uses `pg_catalog.clock_timestamp()` after lock acquisition, never `now()`.
- Same connection and caller transaction; any exception aborts the caller's transaction.

#### 5.5.3 `platform.outbox_append`

Signature:

`platform.outbox_append(p_event_domain text, p_dedupe_key text, p_aggregate_type text, p_aggregate_id uuid, p_event_type text, p_payload_redacted jsonb, p_payload_ciphertext text, p_payload_sha256 text) RETURNS uuid`

- Owner: `platform_reliability_owner`; `SET search_path = platform, pg_temp`.
- Validation: event domain in the 11 accepted values; dedupe key 1-512 and nonblank; aggregate type 1-128 and nonblank; aggregate id non-null; event type 1-128 and nonblank; at least one of redacted/ciphertext non-null; ciphertext envelope valid when present; redacted payload denylist-valid when present; hash lowercase 64-hex. Otherwise `OUTBOX_PAYLOAD_INVALID`.
- Insert one immutable row with `id = pg_catalog.gen_random_uuid()`, `status = 'PENDING'`, `attempt_count = 0`, `available_at = now()`, `revision = 1` using `ON CONFLICT (event_domain, dedupe_key) DO NOTHING RETURNING id`.
- If inserted, return the new id.
- If the unique key exists: select the existing row; if `payload_sha256` equals the argument, return the existing id; otherwise raise `OUTBOX_DEDUPE_CONFLICT`.
- Never update event identity, payload, ciphertext or hash. The dispatcher changes only `status`, `attempt_count`, `available_at`, `published_at` and `revision`.
- A caller rollback removes the Outbox row. No MQ publish occurs.

#### 5.5.4 `audit.append_fact`

Signature:

`audit.append_fact(p_occurred_at timestamptz, p_actor_type text, p_actor_id uuid, p_action_code text, p_object_type text, p_object_id uuid, p_reason_code text, p_before_snapshot jsonb, p_after_snapshot jsonb, p_request_id text, p_trace_id text) RETURNS uuid`

- Owner: `audit_owner`; `SET search_path = audit, pg_temp`.
- Validation: `occurred_at` non-null; actor type in the four classes; actor id required unless `SYSTEM`; action code matches `^[A-Z][A-Z0-9_]{1,127}$`; object type 1-64 and nonblank; object id required unless `object_type = 'SYSTEM'`; reason code 1-128 and nonblank when present; snapshots null or object/array or the single-key encrypted envelope; denylist valid; request/trace IDs 1-128 when present. Otherwise `AUDIT_ARGUMENT_INVALID`.
- High-risk reason rule: `reason_code` is required when `actor_type = 'EMPLOYEE'` or `action_code IN ('IDENTITY_PROFILE_VERIFY','SUBJECT_OWNER_SET','RISK_STATE_CHANGE','EMPLOYEE_ACCOUNT_CHANGE','ORGANIZATION_STATUS_CHANGE','MEMBERSHIP_CHANGE','IDEMPOTENCY_INCOMPLETE_ALERT')`. Missing/blank reason raises `AUDIT_REASON_REQUIRED`.
- Insert exactly one immutable row with `id = pg_catalog.gen_random_uuid()` and return it.
- No update, delete, truncate or second write function exists. The trigger rejects mutation even if a privilege is misconfigured.
- A caller rollback removes the audit row; an audit exception aborts the caller's business transaction.

#### 5.5.5 Denylist and encrypted-envelope rules (inline, no helper function)

The four functions reject a redacted JSON object containing any of these keys, case-insensitively, at any depth:

`phone`, `mobile`, `callerNumber`, `calleeNumber`, `providerSubject`, `identityNumber`, `idNumber`, `legalName`, `shareCode`, `otp`, `objectKey`, `objectVersionId`, `bucket`, `providerTransactionId`, `providerEventId`, `secret`, `token`, `password`, `hmac`, `ciphertext`, `keyRef`, `kmsKey`.

Masked variants such as `calleeNumberMasked` and explicitly non-sensitive business fields are allowed. An encrypted snapshot/result is the single-key object `{"enc":"v1.<key_ref>.<base64url>"}` matching `^v1\.[A-Za-z0-9._:/-]+\..+$`. Redacted and encrypted representations must not be mixed in one object. The functions validate the shape and do not decrypt.

The denylist walk is implemented inline in `platform.idempotency_complete`, `platform.outbox_append` and `audit.append_fact` (a recursive JSON walk over objects/arrays). No fifth helper function is added.

### 5.6 Exact grant matrix

#### 5.6.1 Schema `USAGE` (without `CREATE`)

| Schema | Grantees |
| --- | --- |
| `platform` | `customer_iam_role`, `organization_access_role`, `operations_iam_role`, `subject_number_role`, `platform_reliability_role`, `platform_dispatcher_role` |
| `audit` | `customer_iam_role`, `organization_access_role`, `operations_iam_role`, `subject_number_role`, `subject_identity_verification_role`, `operations_service_role`, `audit_reader_role` |

The platform owner-module runtime roles receive their own-schema `USAGE` per `docs/14` section 2.1 so the explicitly listed table privileges are usable. No S2/S3 role is granted anything because none exists in V001.

#### 5.6.2 Table and column privileges

| Object | Grantee | Privileges |
| --- | --- | --- |
| `platform.idempotency_record` | `platform_reliability_role` | `SELECT` |
| `platform.outbox_event` | `platform_reliability_role` | `SELECT` |
| `platform.outbox_event` | `platform_dispatcher_role` | `SELECT`; `UPDATE (status, attempt_count, available_at, published_at, revision)` only |
| `audit.audit_fact` | `audit_reader_role` | `SELECT` only |

No domain runtime role receives any platform/audit table privilege. No runtime role receives `INSERT`, `DELETE` or `TRUNCATE` on any V001 table. No role other than the owner has write privilege on `audit.audit_fact`.

#### 5.6.3 Function `EXECUTE`

| Function | Grantees |
| --- | --- |
| `platform.idempotency_begin(text,text,text)` | `customer_iam_role`, `organization_access_role`, `operations_iam_role`, `subject_number_role` |
| `platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)` | same four roles |
| `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` | same four roles |
| `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)` | same four roles plus `subject_identity_verification_role`, `operations_service_role` |

`PUBLIC` has no `EXECUTE` on any function, including the trigger function. `platform_reliability_role`, `platform_dispatcher_role` and `audit_reader_role` receive no `EXECUTE`.

#### 5.6.4 Revokes

- `REVOKE ALL ON SCHEMA public FROM PUBLIC;`
- `REVOKE ALL ON SCHEMA <each of the six> FROM PUBLIC;`
- `REVOKE ALL ON TABLE <each of the three> FROM PUBLIC;`
- `REVOKE ALL ON FUNCTION <each of the four and the trigger function> FROM PUBLIC;`
- No `GRANT` to `PUBLIC` anywhere.

### 5.7 Transaction, lock and security decisions

- Default isolation is `READ COMMITTED`; no `SERIALIZABLE` dependency.
- V001 takes no advisory lock. The idempotency unique key plus the `FOR UPDATE` row lock serialize same-key attempts; the Outbox unique key serializes same-key appends. Later domain callers must follow the global lock order in `docs/14` sections 9.0/9.0.1 before calling these functions.
- Functions execute on the caller's connection and in the caller's transaction. No function commits, rolls back, opens an autonomous transaction or runs a transaction-level `SET`.
- Any exception aborts the caller's transaction; domain writes, function writes, Outbox rows and audit rows roll back together.
- `SECURITY DEFINER` never inherits a caller-controlled `search_path`; every object is schema-qualified and `pg_temp` is last.
- Owners are NOLOGIN/NOSUPERUSER/NOCREATEDB; no owner is a superuser and no owner has a runtime login path.
- No cross-module table grant exists. The four functions are the only cross-schema writes.
- No dispatcher process, queue, consumer or scheduler is implemented. The dispatcher column grant is a capability for later work.
- Audit and Outbox payloads are redacted or encrypted; no plaintext protected value is accepted.
- `platform.idempotency_record` retains completed results; `platform.outbox_event` retains event identity/payload; `audit.audit_fact` is append-only. No ordinary delete path exists.

### 5.8 Flyway and PostgreSQL 16 Testcontainers wiring

- Add to `backend/pom.xml`, test scope only:
  - `org.flywaydb:flyway-core`
  - `org.flywaydb:flyway-database-postgresql`
- Use the Boot `4.1.2` BOM-managed versions. Do not guess a version. If `flyway-database-postgresql` is not BOM-managed, resolve the exact compatible version from Maven Central, record it in the implementation completion report, and pin it; do not float.
- No Spring Boot DataSource, Flyway auto-configuration, HikariCP, jOOQ or runtime JDBC is added. `application.yml` and `OneCallApplication` remain unchanged.
- Enforcer: replace the broad `org.flywaydb:*` exclude with:
  - `org.flywaydb:*:*:*:compile`
  - `org.flywaydb:*:*:*:runtime`
  Keep all other existing bans (`spring-security`, JPA, JDBC starter, jOOQ, HikariCP) unchanged.
- Surefire: add `classpathDependencyExcludes` for `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql` so the existing `BootstrapDependencyBoundaryTests` remains unchanged and the unit-test/bootstrap classpath still has no Flyway. Failsafe does not inherit this exclusion, so the container IT sees Flyway.
- Failsafe/`container-smoke` remains explicit. `failIfNoTests=true` remains. No `@Disabled`, `Assumptions`, `disabledWithoutDocker` or environment-variable skip.
- The IT starts `postgres:16` with a per-JVM random password (existing pattern), waits for readiness, and uses the container superuser `onecall`. The superuser is a test fixture for `CREATE ROLE`/`SET LOCAL ROLE`; it is not a production provisioning claim (Q17/Q23).
- Flyway configuration: `Flyway.configure().dataSource(url, "onecall", password).locations("classpath:db/migration").load()`. The default history schema is `public`; the migration does not treat `public.flyway_schema_history` as a business table.
- The migration file is a production artifact under `src/main/resources/db/migration/`. It is included in the jar as SQL but does not activate any runtime behavior.
- The negative failure test uses a separate Flyway instance with `locations("classpath:db/migration-broken")`, a separate schema, and a test-only broken migration. The broken location is under `src/test/resources` and is never on the production classpath.
- The `postgres:16` image digest remains unpinned (PA0 residual). PA1 does not guess a digest or claim a production PostgreSQL patch version.

## 6. Behavioral implementation slices

Three slices, each behaviorally verifiable. The complete V001 SQL is authored in S1 so Flyway always sees a valid, runnable migration; S2 and S3 are behavior-proof slices over that artifact. V001's checksum is frozen at the final PA1 checkpoint, not at S1. If S2 or S3 reveals a V001 defect, a bounded correction is applied to V001 and every prior slice test is rerun before the V001 checkpoint; no V002 migration is created to patch V001.

### 6.1 S1 - V001 applies cleanly, matches the frozen inventory, and fails visibly

Behavior established:

- Flyway test-scope harness and fail-closed container profile.
- Exact V001 schemas, roles, tables, checks, indexes, trigger, four functions and grants.
- Clean-database pre-migration assertion, one successful migration, repeat migration no-op, `validate()` pass.
- Exact inventory and no V002+ object.
- Happy-path invocation of each function as a granted role (catches syntax, ownership and grant errors).
- Test-only broken migration raises `FlywayException` and rolls back its partial DDL.

Why minimal: a migration must be complete and runnable; splitting a single SQL file across slices would create an invalid intermediate schema. S1 proves the artifact is the accepted V001 shape before deeper semantics.

### 6.2 S2 - Idempotency and Outbox preserve first-result and transaction atomicity

Behavior established:

- `NEW`/`REPLAY`/`CONFLICT`, finalize-once, `ALREADY_COMPLETED`, committed-`NEW` alert and concurrency winner.
- Same-transaction rollback for begin/complete and Outbox append.
- Outbox same-key replay/conflict and protected-payload rejection.

Why minimal: these are the reliability primitives every later wave depends on. No domain table or workflow is added.

### 6.3 S3 - Audit is append-only and execution boundaries are enforced

Behavior established:

- Audit append via the function; high-risk reason rule; denylist.
- Update/delete trigger rejection for every writer, including `audit_owner`.
- Runtime privilege denial for insert/update/delete/truncate.
- Caller rollback removes the audit row.
- Fixed ordered `search_path`, no transaction control, owner/PUBLIC/role execution boundaries, exact schema/table/column grants.
- Hostile caller `search_path` cannot redirect effects.

Why minimal: completes the V001 audit/security contract and proves least privilege. No later domain or dispatcher is added.

## 7. Test matrix, commands and expected assertions

### 7.1 Container IT class

`backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` keeps its class name and failsafe include. `@BeforeAll` starts the container, asserts the pre-migration database is clean (no six business schemas, no business tables, no `public.flyway_schema_history`), then runs Flyway once and stores the migration result. A `@BeforeAll` failure fails every test visibly.

The class has exactly these 12 test methods:

| Method | Assertions |
| --- | --- |
| `v001MigratesCleanPostgres16WithoutSkipping` | One migration executed; applied script is `V001__platform_schemas_roles_idempotency_outbox_audit.sql`; `success = true`; no pending; `validate()` passes; second `migrate()` executes zero; `public.flyway_schema_history` has exactly one successful V001 row. |
| `v001InventoryMatchesTheFrozenContract` | Exactly six schemas and their owners; exactly three business tables and zero tables in the four empty schemas; exact column names/counts (12/14/12); exact named checks (10/12/10); exact PK/unique/partial indexes; exactly four declared functions plus the trigger function; one audit trigger; no V002+ schema/role/table/function. |
| `brokenMigrationFailsVisibly` | Test-only broken migration raises `FlywayException`; its first valid DDL is rolled back; no successful applied row is recorded for it; the broken schema is dropped. |
| `idempotencyNewReplayConflictAndFinalizeOnce` | `NEW` revision 1 with null results; `COMPLETED`; same-key/same-fingerprint `REPLAY` returns the stored fields; same-key/different-fingerprint `CONFLICT` has no side effect; same-hash repeat returns `ALREADY_COMPLETED`; different hash and different revision raise `IDEMPOTENCY_CONFLICT`; stored row unchanged; exactly one row. |
| `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow` | A committed `NEW` row makes the next begin raise `IDEMPOTENCY_INCOMPLETE`; `ix_idempotency_new_alert` finds it; begin-only and begin+complete rollbacks leave no row. |
| `concurrentIdempotencyRetryObservesOneWinner` | Two connections race one key: when the winner commits, the loser returns `REPLAY`; when the winner rolls back, the loser returns `NEW`; exactly one final row and one `NEW` winner. |
| `outboxReplayConflictRollbackAndDenylist` | Same key/hash returns the same id and one row; changed hash raises `OUTBOX_DEDUPE_CONFLICT`; a rollback leaves no row; missing payload, bad envelope and protected plaintext raise `OUTBOX_PAYLOAD_INVALID`. |
| `auditIsAppendOnlyAndRuntimeWritesAreDenied` | Append succeeds as a granted role; direct insert as a domain role is denied; `audit_reader_role` update/delete/truncate is denied; `audit_owner` update/delete raises `AUDIT_APPEND_ONLY`; a rollback leaves no row. |
| `auditReasonAndDenylistRulesAreEnforced` | Employee actor without reason raises `AUDIT_REASON_REQUIRED`; high-risk service action without reason raises it; with reason succeeds; protected key raises `AUDIT_ARGUMENT_INVALID`; masked variant succeeds. |
| `functionsHaveFixedSearchPathAndNoTransactionControl` | All four are `SECURITY DEFINER` with the exact ordered `search_path`; owned by the correct owner; `prosrc` contains no transaction-control keyword; `pg_catalog.gen_random_uuid()` is present in the three insert functions; `issue_first_trial` is absent. |
| `ownerPublicAndRoleExecutionBoundariesAreExact` | Owners are NOLOGIN/NOSUPERUSER/NOCREATEDB/NOCREATEROLE; `PUBLIC` has no schema `USAGE`/`CREATE`, table privilege or function `EXECUTE`; the declared roles have `EXECUTE`/`USAGE` without `CREATE`; the exact table/column grants hold; dispatcher column `UPDATE` succeeds only on the five columns; no `DELETE`/`TRUNCATE`. |
| `hostileSearchPathCannotRedirectFunctionEffects` | A decoy schema/table/function plus a caller `search_path` pointing at it cannot redirect `platform.idempotency_begin`; the real platform table receives the row and the decoy is untouched; the decoy schema is dropped. |

Test implementation rules:

- Use unique UUIDs/keys per test; never truncate append-only tables.
- Use autocommit connections for privilege-denial probes; use explicit transaction/rollback connections for atomicity tests.
- Use `SET ROLE <role>`/`RESET ROLE` from the superuser connection to exercise real grants; roll back an aborted transaction before resetting the role.
- Use `CAST(? AS jsonb)` for JSON parameters; use `PGobject` only if required.
- Concurrency tests use two connections, latches and query timeouts; no unbounded waits.
- No test skips or assumptions.

### 7.2 Commands and expected results

Run from `backend/` with Temurin `21.0.13+9`.

```bash
./mvnw -B -ntp validate
```

Expected: `BUILD SUCCESS`; Enforcer passes with Flyway at test scope only; all other bans intact.

```bash
./mvnw -B -ntp dependency:tree -Dincludes=org.flywaydb:*
```

Expected: `flyway-core` and `flyway-database-postgresql` appear at `:test`; no `compile`/`runtime` occurrence.

Negative firewall probe (temporary, reverted before commit): change `flyway-core` scope from `test` to `compile`, run `./mvnw -B -ntp validate`.

Expected: `BUILD FAILURE` naming the banned Flyway dependency. If it does not fail, stop and fix the Enforcer rule before S1 continues; do not weaken the firewall.

```bash
./mvnw -B -ntp clean verify
```

Expected: `BUILD SUCCESS`; 10 surefire tests; `skipped=0`; no container IT; no Docker required; no DataSource/Flyway bean; adjusted bootstrap classpath test passes.

```bash
docker info
./mvnw -B -ntp -Pcontainer-smoke verify
```

Expected: `BUILD SUCCESS`; exactly one `TEST-*PostgresContainerIT.xml`; `tests=12`, `skipped=0`, `failures=0`, `errors=0`; all 12 method names present; no pending migration; broken migration failure visible; no skipped surefire test.

```bash
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar | grep -E 'BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)'
```

Expected: no output and exit code 1 from grep (firewall passes). The CI step remains authoritative.

```bash
git diff --check
git status --porcelain=v1
```

Expected: no whitespace errors; only the intended PA1 paths are changed; no `target/` artifact is staged.

### 7.3 CI assertion update

In `.github/workflows/ci.yml`, update only the failsafe report assertion:

- exactly one `TEST-*PostgresContainerIT.xml`;
- `tests=12`, `skipped=0`, `failures=0`, `errors=0`;
- the 12 method names from section 7.1 are present.

Keep unchanged: `docker info` before Maven; `-Pcontainer-smoke verify`; boot jar smoke; production fat-jar firewall; contract regressions; permissions; path filters.

## 8. Docs decision

- Update `backend/README.md` to describe PA1 V001:
  - the V001 migration and its exact objects;
  - Flyway test-scope wiring and the production runtime firewall;
  - the 12-test container profile and the 10-test default verify;
  - the Q17/Q23 limitation (test superuser is not production provisioning);
  - the evidence boundary (build/static/container evidence, not production acceptance).
- Do not modify `docs/14` or any A0 artifact. The accepted A0 contract is the binding input and remains unchanged.
- Do not add architecture docs, schema docs, API docs or deployment docs. This plan is the only new gate document.
- The implementation completion report should be `docs/gateflow/pa1-v001-persistence-foundation-implementation.md` (or the controller's chosen path), using section 10.

## 9. Risks, open questions, owners and destinations

| ID | Risk / open question | Classification | Owner | Destination |
| --- | --- | --- | --- | --- |
| R-01 | Maven Enforcer 3.6.3 must honor scope-qualified `bannedDependencies` coordinates. | Blocking for S1; stop condition | Backend/Platform | S1 `validate` plus the compile-scope negative probe. If the probe does not fail, stop and correct the rule; do not remove the Flyway ban. |
| R-02 | Flyway artifacts may not both be Boot 4.1.2 BOM-managed. | Implementation check | Backend | S1 `dependency:resolve`/`dependency:tree`; pin the exact resolved version and record it; never guess. |
| R-03 | The Testcontainers superuser can create roles, while production provisioning is Q17/Q23. | Deferred input | Platform/Database/Security | Deployment/security design; no production provisioning claim. |
| R-04 | Flyway history lives in `public`, while `public` is not a business schema. | Bounded decision | Backend/Database | Treat `public.flyway_schema_history` as migration metadata; exclude it from the business inventory; revisit in deployment design. |
| R-05 | Revoking `PUBLIC` privileges on `public` may affect extensions/tools in a shared database. | Bounded decision | Database/Platform | Verified in the isolated test DB; production behavior is Q17. |
| R-06 | Test-scope Flyway conflicts with the PA0 bootstrap classpath guardrail. | Bounded, disclosed change | Backend/Platform | Surefire classpath exclusion + scope-qualified Enforcer + unchanged fat-jar firewall; preserve `BootstrapDependencyBoundaryTests` unchanged. |
| R-07 | Pre-existing roles in a shared/production database would make plain `CREATE ROLE` fail. | Deferred input | Platform/Database | Q17; fail closed rather than `IF NOT EXISTS`; reconcile provisioning before production. |
| R-08 | The audit trigger message/SQLSTATE is a local choice not frozen by A0. | Bounded decision | Operations-audit/Backend | Use `AUDIT_APPEND_ONLY`/`P0001`; assert it in S3. |
| R-09 | The inline recursive denylist walk is more complex than a helper function. | Bounded implementation risk | Platform/Backend | Inline in the three snapshot-writing functions; no fifth function; unit/IT coverage in S2/S3. |
| R-10 | `postgres:16` digest remains unpinned. | Inherited PA0 residual | Platform/CI | Supply-chain hardening; PA1 does not guess a digest or claim a production version. |
| R-11 | CI's exact test-count/method-name assertion is brittle for future work. | Bounded CI maintenance | Platform/CI | Update the assertion in the same change that adds/removes V001 tests. |
| R-12 | The two-connection concurrency test can be flaky. | Bounded test risk | Backend/QA | Latches plus bounded query timeouts; deterministic commit/rollback scenarios; S2 review. |
| R-13 | V001 must remain a single valid Flyway migration while S2/S3 may reveal defects. | Bounded sequencing | Backend/Controller | V001 checksum finalizes at the PA1 checkpoint; bounded corrections rerun all prior slice tests; no V002 patch. |
| R-14 | V001 grants dispatcher columns but no dispatcher exists. | Explicit non-goal | Platform/Backend | Later async-worker work; no queue/dispatcher in PA1. |
| R-15 | `platform_reliability_role`/`platform_dispatcher_role` own-schema `USAGE` is derived from `docs/14` section 2.1 rather than enumerated in section 10.8. | Plan-review clarification | Platform/Database | Treat section 2.1 as binding so the section 10.8 table grants are usable; if review requires a stricter reading, resolve before implementation. |
| Q17 | Flyway role provisioning, schema ownership transfer and least-privilege grant execution. | Deferred input; not guessed | Platform/Database | Deployment/security design and production readiness. |
| Q23 | Function/role provisioning order and whether the deployment role can create `SECURITY DEFINER` functions. | Deferred input; not guessed | Platform/Database/Security | Deployment/security design and production readiness. |

No blocking open question remains for planning. R-01 is a concrete S1 stop condition; R-02 is resolved by dependency resolution; R-15 is a bounded plan-review clarification.

## 10. Completion report format

The implementation completion report must contain:

- Task label, provider, gate, date and base commit.
- Changed artifact paths and SHA-256 for the V001 SQL, `pom.xml`, `PostgresContainerIT`, broken migration fixture, README and CI workflow.
- Binding inputs: PA1 contract, accepted A0 checkpoints/hashes, and confirmation that `docs/14` is unchanged.
- S1/S2/S3 slice checkpoints and the exact tests/evidence each slice added.
- Flyway version, migration script, applied version, `migrationsExecuted`, repeat-migrate result, `validate()` result and history success.
- Exact inventory evidence: six schemas/owners, 15 roles/attributes, three tables/columns/checks/indexes, four functions/signatures/owners/`search_path`, trigger, and absence of V002+ objects.
- Behavior evidence: idempotency `NEW/REPLAY/CONFLICT`, finalize-once, `ALREADY_COMPLETED`, committed-`NEW` alert, rollback, concurrency; Outbox replay/conflict/rollback; audit append-only/trigger/privilege/rollback.
- Security evidence: fixed `search_path`, no transaction control, `PUBLIC` denial, role `EXECUTE`/`USAGE`, table/column grants, hostile-search-path test.
- Firewall evidence: Enforcer `validate`, dependency-tree scopes, negative compile-scope probe, boot jar smoke and fat-jar absence.
- Exact commands, exits and surefire/failsafe counts (`10` surefire; `12` failsafe, `skipped=0`, `failures=0`, `errors=0`).
- Docs decision.
- Residual risks classified as fixed-current, later-approved-slice or later-work-unit, with owner/destination; Q17/Q23 preserved.
- Explicit limitations: Testcontainers superuser is not production provisioning; no V002-V009, runtime DB, API, UI, cloud, dispatcher, production role/credential or production acceptance.
- Completion or blocked statement.
- Explicit statement that the implementation did not enter another gate beyond the approved slice work and did not claim production acceptance.

## 11. No-goal-drift and no-over-design check

| Check | Evidence |
| --- | --- |
| Only V001 objects exist | Six S1 schemas, 15 S1 roles, three V001 tables, four declared functions, one audit trigger; inventory test asserts no V002+ object. |
| No V002-V009 table/function/behavior | No later migration file, schema, role, table or function is authored; tests assert absence. |
| No identity/organization/subject/IVR/catalog/order/payment/evidence/sharing behavior | Only empty S1 schemas are created; no domain table or service exists. |
| No API/UI/cloud/secret/deployment work | Only backend test-scope migration verification and docs/CI text are touched. |
| No queue/dispatcher | Only the Outbox table, append function and dispatcher column grants exist; no dispatcher process. |
| No new business values | Uses only the accepted `IVR_CONFIRM`/`PURCHASE` scopes, 11 Outbox domains and accepted states; no guessed value. |
| No extra SQL functions | Exactly four declared functions plus the accepted local trigger function; denylist inline. |
| No A0 contract change | `docs/14` and A0 artifacts are untouched; the V001 tables/functions/grants match sections 4.3-4.5 and 10.3-10.8. |
| No production runtime dependency | Flyway is test scope; Enforcer bans compile/runtime; fat-jar check unchanged; no DataSource bean. |
| No Q17/Q23 guess | Superuser is a test fixture; no LOGIN role, role membership, password or deployment provisioning is created or claimed. |
| Three behavioral slices | S1 migration/inventory/failure visibility; S2 idempotency/Outbox; S3 audit/security. Each has independent assertions and no file-split-only slice. |
| Minimality | The migration is the smallest runnable V001 unit; no helper function, no dispatcher, no runtime wiring, no later domain. |

## 12. Plan validation performed by the author

- Read `docs/gateflow/project-execution-control.md` (PA1 binding contract, slice principle, gate record), `docs/12` sections 5, 6, 14, 15 Phase A and 18 item 1, the accepted A0 V001 sections of `docs/14`, `backend/README.md`, `backend/pom.xml`, the existing architecture/container tests and `.github/workflows/ci.yml`.
- Inspected the backend tree to confirm no migration, persistence code, DataSource, Flyway or domain table currently exists.
- Cross-checked the V001 object inventory, checks, indexes, function signatures, grant matrix and error codes against `docs/14` sections 2-4, 5.3, 9.0-9.3, 10.1-10.10 and 11.3.
- Verified that the plan adds no V002+ object and leaves `docs/14`, A0 artifacts, application code and existing guardrail tests untouched.
- Performed document-level checks on this plan: required sections present, exact artifact path, no V002+ implementation content, no trailing whitespace, and no source-file edit by this gate.
- Git status/diff could not be executed in this WSL session because the worktree `.git` pointer resolves to an inaccessible Windows path (`D:/codex-call/.git/worktrees/codex-call1`). The only write performed by this gate is the creation of this plan artifact; no source, test, CI or other document was modified.
- Did not run Maven, Docker, Flyway, PostgreSQL or any implementation command. All command expectations in section 7 are planned evidence for the implementation slices, not executed results.
