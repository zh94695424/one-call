# PA13 sharing-role least-privilege plan

- Task: `pa13-sharing-role-least-privilege-plan-gpt-20261103-01`
- Work unit: `PA13-sharing-role-least-privilege`
- Gate: plan
- Status: plan-complete
- Date: 2026-11-03

## Goal and implementation decision

Retain `V001`–`V009` byte-for-byte. Add only `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql`. V010 converts `sharing_role` to the query-only principal and creates `sharing_command_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT`. The query login remains a separate LOGIN role with only membership in `sharing_role`; it must not have (direct or transitive) membership in `sharing_command_role`. Do not add a view or SECURITY DEFINER function: the current list and lifecycle queries are straightforward direct reads and column-level SELECT privileges are sufficient and narrower.

V010 must execute transactionally under Flyway's normal PostgreSQL transaction. Use explicit `SET LOCAL ROLE` only if needed to own role/ACL statements; the migration identity must already have role administration rights. Create the command role with `CREATE ROLE ... NOLOGIN ... NOINHERIT`; do not grant it to the login or to `sharing_role`. Revoke inherited broad V009 ACLs rather than attempting to layer column grants over table-wide SELECT. Revoke ALL on both roles for all V009 table grants, schema/function ACLs currently given to `sharing_role`, and any PUBLIC privileges relevant to these objects; then restore only the following exact grants. Explicitly revoke table-level SELECT before granting query columns. Revoke INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER and all other table privileges from `sharing_role`; it receives no sequence, schema CREATE, ownership, role membership, or DDL capability. V010 does not change any table, column, index, constraint, trigger, or function definition.

Repeated execution is not a deployment contract (Flyway applies once); nevertheless make role creation/revocation safely guarded where PostgreSQL syntax permits. Do not use broad `DROP ... CASCADE`, remove/recreate roles, or make the migration silently tolerate an incompatible pre-existing command role. Verify ACL effects from PostgreSQL catalogs after migration. Since `NOINHERIT` is not a substitute for membership control, explicitly prove no membership path lets the query login assume the command role. V010 must also explicitly account for existing PUBLIC grants and default ACLs: revoke relevant PUBLIC schema/table/function privileges and assert none remain. Do not presume a database-wide default ACL can be reset safely; inspect and document catalog results for the objects created by V001–V009 and V010.

## Exact query-role read set

The `JooqShareQueryRepository` list SELECT of `sharing.evidence_share_grant` needs exactly these columns (projection plus scope, keyset, and ordering):

`share_id`, `evidence_id`, `verification_mode`, `status`, `valid_from`, `expires_at`, `max_admissions`, `admissions_used`, `download_allowed`, `code_generation`, `revocation_generation`, `created_by_account_id`, and `owner_organization_id`.

The customer context checks need `customer_iam.customer_account(id,status)`; organization context checks need `organization_access.organization(id,status)` and `organization_access.organization_membership(organization_id,account_id,status)`. The role needs `USAGE` on schemas `sharing`, `customer_iam`, and `organization_access`, and column-only SELECT on those columns. These are the full current query and ACTIVE lifecycle-check privileges. No SELECT on `evidence`, `subject_number`, `platform`, or `audit`; no grants on `share_admission` or `share_access_event`; no sequence or function execution; no SELECT on grant `id`, token/hash/salt/code content, snapshots, digest, or revision. In particular deny `capability_token_hash`, `code_salt`, `code_hash`, `content_snapshot`, `content_sha256` and all other non-listed grant columns. Keep query SQL unchanged.

## Command-role grant boundary

`sharing_command_role` is an inert future principal only; no current datasource, service, login, endpoint, or controller is wired to it. It gets `USAGE` only on the schemas needed below and no schema `CREATE`. Preserve only the frozen future write/admission/lifecycle/test semantics in docs/14 and V009:

- `sharing.evidence_share_grant`: SELECT the command workflow's lookup/guard/read columns; INSERT only `id`, `share_id`, `capability_token_hash`, `evidence_id`, `created_by_account_id`, `owner_organization_id`, `verification_mode`, `code_salt`, `code_hash`, `code_generation`, `valid_from`, `expires_at`, `max_admissions`, `admissions_used`, `download_allowed`, `content_snapshot`, `content_sha256`, `status`, `revocation_generation`, `revision`; UPDATE only mutable lifecycle columns `expires_at`, `max_admissions`, `admissions_used`, `status`, `code_salt`, `code_hash`, `code_generation`, `revocation_generation`, `revision` (reconcile exact columns against frozen lifecycle SQL/test statements before implementation; never grant DELETE/TRUNCATE/REFERENCES/TRIGGER/DDL).
- `sharing.share_admission`: SELECT and INSERT its declared columns for idempotency lookup, admission number/count and immutable admission insertion. No UPDATE/DELETE/TRUNCATE.
- `sharing.share_access_event`: INSERT only the declared event columns needed for exact redacted event append; do not grant SELECT absent a frozen workflow read. No UPDATE/DELETE/TRUNCATE.
- Cross-module read columns only for existing frozen semantics: `evidence.evidence_record` and `evidence.evidence_artifact` fields used for creation-time eligibility/snapshot; `customer_iam.customer_account(id,status)`; `organization_access.organization(id,status)` and `organization_access.organization_membership(organization_id,account_id,status)`; `subject_number.business_subject`, `caller_number_binding`, and `subject_risk_state` only the columns used by frozen caller/recipient admission predicates. Derive the final cross-module column lists directly from docs/14 §10.21 and the V009-behavior fixture SQL, not whole-table grants.
- Grant EXECUTE only on the exact existing `platform.outbox_append` and `audit.append_fact` signatures when the frozen create/lifecycle contract calls them. Grant no direct EXECUTE on trigger guard/reject functions: triggers invoke them as part of table operations. Grant no other functions.

V009 behavior tests currently use `sharing_role` as an effective synthetic write principal for future create/admission/lock/revoke/rotate/extend/expiry behavior. Change only those test connections/`SET LOCAL ROLE` statements to `sharing_command_role`; keep SQL assertions, atomicity/idempotency/immutability assertions, denied writes and role-effective checks intact. The production query pool continues to set and assert `sharing_role` only. Tests must distinguish authorization failure (`42501`) from read-only transaction failure (`25006`): use writable transactions under command role to test ACL denials; retain a separate read-only proof that an otherwise authorized write is blocked by transaction mode.

## Consumer and test inventory

Consumers and assertions that reference `sharing_role` and must remain query-role consumers:

- `SharingPersistenceConfiguration` fixed local role and effective-role assertion.
- `CustomerShareQueryService` query transaction contract and tests.
- `SharingQueryFixture`, `CustomerSharesFixture`, `SharingQueryServiceIT`, and `CustomerSharesIT` PA10 query/list, ACTIVE account/organization/membership, pagination, pool-reset, and PA11 coexistence coverage.
- `PostgresContainerIT` startup/migration/database behavior setup where the sharing query login is provisioned; retain login configuration and query assertions, changing only explicit future-write behavior principals.
- Any role/credential assertions in `SharingPersistenceConfigurationTests`, `CustomerSharesWebTests`, architecture tests, and fixture SQL. Inventory with `rg -n 'sharing_role|share_admission|share_access_event|SET LOCAL ROLE|SET ROLE' backend/src/test backend/src/main` before edit; classify each occurrence as query-role, command-role behavior fixture, or unrelated text and assert none are missed.

V009 ACL statements granting broad read/write/function/cross-module privileges to `sharing_role` are superseded—not rewritten. Existing PA10 `/shares` and PA11 `/me` coexistence behavior and V009 database invariants remain covered unchanged.

## Test changes

Add `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java` and add it to the existing Maven Failsafe IT inclusion and CI inventory only if those inventories are explicit. Follow `PostgresContainerIT` fixture/startup conventions and run against PostgreSQL `16.15` (pin the test image for this security test; assert `SHOW server_version` is `16.15`). Include catalog checks using `pg_roles`, `pg_auth_members`, `has_schema_privilege`, `has_table_privilege`, `has_column_privilege`, `has_function_privilege`, `information_schema.role_table_grants`/`column_privileges`, and ACL catalogs as appropriate:

1. Exact role attributes: `sharing_role` and `sharing_command_role` NOLOGIN, NOSUPERUSER, NOCREATEDB, NOCREATEROLE; command role NOINHERIT. Exact membership path: query login can assume `sharing_role`, cannot assume command role, and command role is not a member of query role. Include effective `pg_has_role` assertions, not only a direct membership row check.
2. Exact query schema and column grants match the query read-set above, with no table-wide SELECT; no grant on admission/event, secret columns, evidence/subject/platform/audit, functions, sequences, DDL, writes or PUBLIC. Assert exact allowed function set is empty.
3. `SET LOCAL ROLE sharing_role`; prove `current_user='sharing_role'`, allowed projected list SELECT succeeds and ACTIVE account/organization/membership reads succeed. Prove `CustomerShareQueryService` path continues working through its configured pool.
4. Under that same effective role, direct SELECT of each of `capability_token_hash`, `code_salt`, `code_hash`, `content_snapshot`, `content_sha256` fails with SQLSTATE `42501`; attempt INSERT/UPDATE/DELETE/TRUNCATE, DDL, admission/event SELECT, foreign-table SELECT, and unauthorized evidence/subject/platform/audit access and assert `42501`. Where SQL syntax/runtime policy can produce another failure first, isolate ACL checks with a minimal statement whose only failing condition is privilege. Distinctly assert read-only authorized mutation still fails `25006` in the existing read-only test.
5. Under `SET LOCAL ROLE sharing_command_role` in a writable test transaction, run and preserve the existing future behavior probes, plus verify denied operations/columns still fail `42501`; prove it is absent from production pool configuration and `/shares` continues to record `sharing_role` as `current_user`.

Move only V009-behavior fixture paths that intentionally write/admit/lifecycle-mutate to command role. Do not weaken assertions by changing exception types, removing expected operations, or relying on the direct login's lack of grants. Existing `CustomerSharesIT` direct-login checks remain supplemental only; new ACL conclusions must use role-effective execution.

## Durable documentation and inventories

Update `backend/README.md`'s current V009 warning to state the V010 exact read-only query role and separate unwired command role; update credential guidance to clarify `/shares` login may assume only `sharing_role`. Update docs/14 §3.1/§3.2/§4.44–4.46/§10.21 and the durable grant matrix so it records V010, both role attributes, exact query grant list, exact command grant list, no PUBLIC access, and the fact that command privileges do not imply an implemented service. Keep frozen behavior descriptions intact. Do not edit the control record, prior review history, OpenAPI, or unrelated docs.

Update the explicit CI source inventory and Maven Failsafe XML inventory in the same commit if present. Expected source=CI/XML inventory equality must include the new `SharingRoleLeastPrivilegeIT` exactly once and retain all existing ITs. No POM or CI edit is needed if inventory is convention-based; document/verify this from actual configuration rather than inventing an inventory. Do not modify acceptance inventories or unrelated snapshots.

## Minimal implementation slice and ownership

Use one end-to-end, independently reviewable slice: V010 ACL conversion + focused role-effective PostgreSQL IT + migrations of only future-write test principals + README/docs grant matrix/inventory update. Ownership is intentionally confined to V010; sharing container IT/fixtures and the new IT; `backend/README.md`; docs/14; and only confirmed explicit POM/CI inventory entries. No source query/API behavior change, no V001–V009 edits, no changes outside this list.

## Validation contract

Run from repository root, with Docker available:

1. `docker run --rm postgres:16.15 postgres --version` — reports PostgreSQL `16.15`.
2. `cd backend && ./mvnw -DskipTests=false -Dtest=none -Dit.test=SharingRoleLeastPrivilegeIT,PostgresContainerIT,CustomerSharesIT verify` — focused Flyway/catalog/role-effective checks pass. If wrapper does not support this selector, use the existing documented Failsafe selector without changing its configuration.
3. `cd backend && ./mvnw verify` — full Maven unit, architecture, and integration suite passes, including `/me`, `/shares`, and V009 behavior coverage.
4. In the integration fixture, assert Flyway reports exactly 10 applied migrations, V010 identity/description is `V010`/`sharing role least privilege`, and checksums for V001–V009 match the pre-change baseline (capture baseline before editing; never rewrite old migrations).
5. Query catalogs and assert exact table/column/function/schema/role/membership/PUBLIC grant sets as enumerated above. Positive and negative probes must run after `SET LOCAL ROLE` and assert `current_user`; secret, foreign-table, write, and DDL ACL denials are `42501`; read-only transaction denial is separately `25006`.
6. Compare SHA-256 of every `V001__*.sql` through `V009__*.sql` against the baseline captured before implementation; all match byte-for-byte. `git diff --check` passes.
7. Compare source ITs, Maven Failsafe includes, and CI IT inventory as sets; exact equality, no duplicate entries. If CI uses a different explicit inventory root, inspect its actual checker and update only the required inventory.

Tests involving PostgreSQL and Docker are not replaced by static SQL inspection. Record any unavailable environment as a validation blocker, not as a pass.

## Assumptions, security review, and deferred proof

- Goal alignment: the command role prevents least-privilege pressure from broadening the endpoint role; direct column grants preserve current SQL and avoid an unnecessary view or SECURITY DEFINER escalation.
- Avoid overdesign: one migration, one focused IT, narrowly changed behavior-test role setup, durable matrix updates; no runtime command service, compatibility abstraction, generic ACL framework, or schema redesign.
- PostgreSQL assumptions to verify against 16.15: column SELECT grants coexist with table-level REVOKE; role membership and `NOINHERIT` behavior match effective `SET ROLE` probes; transactional DDL/role ACL changes are supported by the actual Flyway execution identity; catalog ACL expansion correctly accounts for owner privileges and PUBLIC. Test schemas/tables are owner-managed; the query login must not own objects, be superuser, or inherit an owner role.
- `USAGE` is required per accessed schema, but is not table/data access. `sharing_role` must not receive schema CREATE. `PUBLIC` must have no applicable access. Role-effective tests must account for inherited memberships, object ownership, PUBLIC and function EXECUTE defaults; a list of explicit GRANT statements alone is not proof.
- Function execution must be empty for query role; trigger functions execute through table DML and are not granted to query role. Command function privileges are exact signature-specific `platform.outbox_append`/`audit.append_fact` only when required by frozen operations; SECURITY DEFINER is not introduced.
- Residuals deliberately not proved here: no command application/service correctness, authentication/admission decision, token derivation or secrecy beyond DB column ACL, KDF/OTP, expiry worker, idempotency at service boundary, concurrency/deadlock policy outside frozen DB probes, session/ticket/media authorization, Redis, provisioning/rotation of production login credentials, or runtime command-role containment. Assign command semantics to the later sharing-command/admission/lifecycle implementation work unit; assign production provisioning/login/session management to its designated security/deployment work unit; assign playback/media authorization to its own later authorization work unit. This plan does not authorize entering any of those gates.

## Completion boundary

Implementation may proceed only after this plan is accepted. This artifact is the sole permitted file in the present planning task. No commit, push, PR mutation, external communication, approval/reviewer action, or later gate is part of this work unit.
