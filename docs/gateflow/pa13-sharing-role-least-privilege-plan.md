# PA13 sharing-role least-privilege plan

- Task: `pa13-sharing-role-least-privilege-plan-gpt-20261103-01` (fix 1)
- Work unit: `PA13-sharing-role-least-privilege`
- Gate: fix
- Status: plan-fix-1-complete
- Date: 2026-11-03
- Frozen input identity: raw CRLF SHA-256 `53B9FB2FBB5FD2425C61B95A76BFE41CDF9E8B319545A7D5DA150F6951CC840E`; canonical-LF SHA-256 `B67A19F134A3134BB15CD5A9471DF27ED70E26CB8D2A546189EC8EF40A4B1290`. Both identify the same frozen plan bytes. Do not rewrite history.

## Binding goal and boundaries

Retain V001–V009 byte-for-byte. Add only forward migration `backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql`; no query/API/OpenAPI/default-startup behavior changes. `sharing_role` remains the production query principal, narrowed to the precise `/api/v1/customer/shares` read set below. Create `sharing_command_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT`, unwired from all applications and logins, for the already-frozen future create/admission/revoke/rotate/extend/lock/expiry semantics. No view or SECURITY DEFINER is planned. No implementation of command behavior, production provisioning, API/source edits, PR/commit/review action, or later gate is authorized.

Migration runs under the existing container bootstrap administrator (`onecall` for `PostgresContainerIT`; `fixture_admin` for the sharing/customer fixtures), created as the PostgreSQL container's superuser, and the approved production migration identity. Assert `rolsuper` before applying V010 in each fixture; deployment must prove superuser-equivalent ability to assume every named owner and alter role membership/attributes. Do not assume ordinary object ownership is sufficient to revoke grants made by another grantor. Fail migration if required roles/owners/objects are missing or incompatible; no `DROP ... CASCADE`, role recreation, broad default-ACL reset, or silent partial success. Existing schemas and owner roles from V001–V009 remain unchanged.

## Query-role exact grants

Grant `USAGE` without `CREATE` on `sharing`, `customer_iam`, and `organization_access`. Grant only column `SELECT` as follows:

- `sharing.evidence_share_grant`: `share_id`, `evidence_id`, `verification_mode`, `status`, `valid_from`, `expires_at`, `max_admissions`, `admissions_used`, `download_allowed`, `code_generation`, `revocation_generation`, `created_by_account_id`, `owner_organization_id`.
- `customer_iam.customer_account`: `id`, `status`.
- `organization_access.organization`: `id`, `status`.
- `organization_access.organization_membership`: `organization_id`, `account_id`, `status`.

No other query-role schema/table/column/function/sequence privilege or role membership. In particular no `sharing_command_role` membership path; no `id` or secret/content column of the share grant; no admission/event table access; no `evidence`, `subject_number`, `platform`, or `audit` access. Existing repository SQL and lifecycle checks stay unchanged.

## Command-role exact grants

The command role is not an endpoint/runtime wiring change. It receives `USAGE` without `CREATE` on `sharing`, `evidence`, `customer_iam`, `organization_access`, `subject_number`, `platform`, and `audit`; no `ivr` or `entitlement_metering` privileges. Owned sharing-table privileges are:

- All-column `SELECT` and all-column `INSERT` on each of `sharing.evidence_share_grant`, `sharing.share_admission`, and `sharing.share_access_event`. Full-row SELECT is required by preserved `to_jsonb(row)` behavior tests; the admission guard is SECURITY INVOKER and reads `SELECT *` from the grant row; full-row insertion preserves frozen behavior and schema defaults are not relied upon to narrow privilege.
- `UPDATE` only on `sharing.evidence_share_grant(status, admissions_used, revocation_generation, revision, expires_at, code_salt, code_hash, code_generation)`. `max_admissions` is immutable and expressly excluded. No UPDATE on admission/event; no DELETE, TRUNCATE, REFERENCES, TRIGGER, schema CREATE, DDL, or sequence privileges.

The exact cross-module column SELECT grants, all needed by frozen §10.21 semantics, are:

- `evidence.evidence_record(id, subject_id, status, source_type, source_ref_id, call_id, session_id, reservation_id, identity_at_capture, created_at)`.
- `evidence.evidence_artifact(evidence_id, media_object_id, artifact_role)`.
- `customer_iam.customer_account(id, status)`.
- `organization_access.organization(id, status)` and `organization_access.organization_membership(organization_id, account_id, role, status)`.
- `subject_number.business_subject(id, owner_account_id, owner_organization_id, revision)`; `subject_number.caller_number_binding(id, subject_id, status, verified_at, revision)`; `subject_number.subject_risk_state(caller_number_binding_id, risk_type, state, revision)`.

Grant EXECUTE only on `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` and `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)`, both returning `uuid`. Grant `USAGE` on `platform` and `audit`; no table privilege there. No platform idempotency functions. `sharing_command_role` receives no `ivr` or `entitlement_metering` grants. It does not receive direct EXECUTE on sharing trigger functions: trigger execution is not an invoker's direct function call, and V010 removes the obsolete direct runtime grants. In §10.22 document triggers as owner-local structural controls with no direct runtime EXECUTE grant. No membership grants the command role to `sharing_role` or query login.

## V010 statement order and grantor/owner blocks

V010 is transactional. Exact ordered contract:

1. Under migration superuser, assert preconditions and role attributes; create `sharing_command_role` with the attributes above. Reject an incompatible pre-existing role. Do not change V001–V009.
2. `SET LOCAL ROLE sharing_owner`. Revoke from `sharing_role` and `sharing_command_role` all table-level `SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER` on all three sharing tables. Explicitly revoke each old column ACL as well: from `sharing_role`, grant-table UPDATE on `(status, admissions_used, revocation_generation, revision, expires_at, code_salt, code_hash, code_generation)`; from `sharing_role`, table SELECT/INSERT on each table (the table-level revoke); revoke all column SELECT/INSERT/UPDATE on all three tables from both roles using explicit `REVOKE <privilege> (<every column>)` lists derived from V009's table definitions. This explicit column reset is required because table-level `REVOKE ALL` does not clear `pg_attribute.attacl`. Revoke direct EXECUTE from `sharing_role` and command role on `sharing.guard_share_grant()`, `sharing.guard_share_admission()`, and `sharing.reject_row_mutation()`; revoke relevant PUBLIC table/function privileges; revoke `USAGE,CREATE` on schema `sharing` from PUBLIC and `CREATE` from both roles. Grant command-role schema `USAGE` without CREATE. Grant the three tables' all-column SELECT and INSERT and the exact eight grant UPDATE columns to command role. Grant query-role schema USAGE. Then grant query-role only the 13 listed grant columns. `RESET ROLE`.
3. `SET LOCAL ROLE evidence_owner`. Explicitly revoke every V009 column SELECT from `sharing_role` and `sharing_command_role` on `evidence.evidence_record` and `evidence.evidence_artifact` (the exact lists above); revoke relevant PUBLIC table ACL; revoke schema `USAGE,CREATE` from PUBLIC and `USAGE,CREATE` from both sharing roles. Grant schema USAGE and the exact column SELECT lists to command role only. `RESET ROLE`.
4. `SET LOCAL ROLE customer_iam_owner`. Revoke V009 column SELECT `(id,status)` and relevant table/PUBLIC/schema grants from both sharing roles; then grant schema USAGE and `(id,status)` SELECT to both roles. `RESET ROLE`.
5. `SET LOCAL ROLE organization_access_owner`. Revoke V009 organization `(id,status)` and membership `(organization_id,account_id,role,status)` column SELECT and relevant table/PUBLIC/schema grants from both roles; grant schema USAGE and organization `(id,status)` SELECT to both, membership `(organization_id,account_id,status)` to query role, and membership `(organization_id,account_id,role,status)` to command role. `RESET ROLE`.
6. `SET LOCAL ROLE subject_number_owner`. Revoke the three exact V009 column SELECT lists and relevant table/PUBLIC/schema grants from both roles. Grant schema USAGE and the exact three column SELECT lists above to command role only. `RESET ROLE`.
7. `SET LOCAL ROLE platform_reliability_owner`. Revoke schema USAGE/CREATE and EXECUTE on `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` from PUBLIC and both sharing roles; grant schema USAGE and EXECUTE to command role only. `RESET ROLE`.
8. `SET LOCAL ROLE audit_owner`. Revoke schema USAGE/CREATE and EXECUTE on `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)` from PUBLIC and both sharing roles; grant schema USAGE and EXECUTE to command role only. `RESET ROLE`.
9. Under migration superuser, assert no membership edge/path between query login/`sharing_role` and command role; assert role attributes and no relevant PUBLIC grants. End migration only if all ACL assertions match the intended state.

For all revokes, use explicit `REVOKE ... FROM PUBLIC, sharing_role, sharing_command_role` as applicable while executing in the owning block; include `GRANTED BY` only if PostgreSQL 16 syntax/identity requires it, never as a substitute for the owner block. `REVOKE ALL ON TABLE` alone is insufficient. Do not use `CASCADE`; unrelated grantees/grants must remain unchanged. Column-level revoke lists name all columns for that object so stale attribute ACLs cannot survive. Existing V009 schema/function grants are revoked only for these two grantees and relevant PUBLIC ACLs; owner ACLs remain. Expected end state is exactly the query set and command set stated above, with no table-level sharing privileges for query role and no column ACL residue beyond enumerated sets.

## Exact existing-test delta (implementation contract only)

| File/site | Required post-V010 update | Expected value |
| --- | --- | --- |
| `PostgresContainerIT.v009OwnerPublicRuntimeAndCrossModuleAclsAreExact` / `assertV009AclCatalog` | Preserve the `@Test` method name and count, but remove the V009-final-state ACL expectation that is invalid after V010; do not replace it with competing exact V010 ACL evidence. Keep applicable owner/public/runtime behavioral assertions. Exact V010 ACL proof is only in `SharingRoleLeastPrivilegeIT`. | Remove obsolete expectation of 7 sharing-role schema USAGE rows, 6 table ACL rows, 42 column ACL rows, 5 sharing-role function EXECUTEs, and zero sharing-role memberships. Current post-V010 query schema USAGE set is 3; query sharing-table ACL is zero table-level privileges plus 13 grant-column SELECTs; command sharing tables have table SELECT/INSERT on each of 3 tables plus 8 grant UPDATE columns. Neither role has any role membership edge; query login's only role path is to `sharing_role`. |
| `PostgresContainerIT.assertSchemasAndRoles` (final V009+ assertion sites, including `v009SharingInventoryAndGuardShapesAreExact`) | Add role to exact DB role set; keep staged target-007 absence checks unchanged because V010 has not run in that staged DB. | Final role set = existing exact set + `sharing_command_role`; staged V007 still has neither sharing role. |
| `PostgresContainerIT` migration history assertions (`1285–1369`, `10735–10756`) | Append V010 identity in the existing ordered assertions. Preserve method names and total 64. | 10 history rows/scripts; last row `010 | V010__sharing_role_least_privilege.sql`; description `sharing role least privilege`; applied count 10. V001–V009 identity/checksum history unchanged. |
| `SharingQueryFixture` | Migration assertions only. | `migrationsExecuted == 10`; `applied().hasSize(10)`. |
| `CustomerMeFixture` | Migration assertions only; retain PostgreSQL major-family assertion. | `migrationsExecuted == 10`; `applied().hasSize(10)`; server version remains `160000 <= server_version_num < 170000`. |
| `SharingQueryServiceIT` setup | Applied migrations assertion. | 10. Existing query behavior/current SQL unchanged. |
| `CustomerMeFixture`, `SharingQueryFixture`, `CustomerSharesFixture` role provisioning | Keep endpoint/query logins only as members of `sharing_role`; provision a separate synthetic command probe login only in the new security IT. | No query fixture login is a member of `sharing_command_role`; no production source login change. |
| `SharingQueryServiceIT.listingProjectionNeverReadsOrReturnsSecrets` and `runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure`; `CustomerSharesIT.existingAclsAndReadOnlyTransactionsForbidWritesAndForeignReads` / `CustomerSharesFixture` | Retain query positive projection and pool tests. Replace the claim that the query role has an authorized write blocked by read-only mode with ACL denial; leave the independent `CustomerMeIT` `25006` evidence intact for its actual authorized customer-role write. | Query role unauthorized writes/foreign reads produce `42501`; new IT proves `25006` for authorized command UPDATE in read-only transaction. |
| `PostgresContainerIT` V009 behavior paths using `switchLocalRole(..., "sharing_role")` for frozen create/admission/revoke/rotate/extend/lock/expiry SQL (including `v009OwnerPublicRuntimeAndCrossModuleAclsAreExact` behavior setup and `concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner`) | Change only the explicit future-write test principal to `sharing_command_role`; keep assertion meaning, SQL, data and all 64 test method names. Leave creator-scoped query checks under `sharing_role`. | Frozen command semantics execute with full required SELECT/INSERT and exact UPDATE set; query role remains narrow. |
| `PostgresContainerIT` 64-name CI inventory | Do not add/remove/rename test methods. New tests are not placed here. | Exactly 64 names, unchanged. |
| `backend/pom.xml` Failsafe includes | Add `**/SharingRoleLeastPrivilegeIT.java` exactly once alongside current four IT classes. | 5 class includes. |
| `.github/workflows/ci.yml` inventory | Add exact class and method-name set for `SharingRoleLeastPrivilegeIT`; validate source/CI/XML sets agree. | Existing Failsafe total 92 + N new methods; combined 159 + N. New class method count N is the explicit inventory below, not a guessed value. |

New `SharingRoleLeastPrivilegeIT` contains exactly 8 `@Test` methods (N=8): `v010MigrationIdentityAndPostgres16`, `v010RoleAttributesAndMembershipContainment`, `v010QueryRoleExactCatalogAcl`, `v010CommandRoleExactCatalogAcl`, `v010QueryRolePositiveAndNegativeBehavior`, `v010CommandRolePositiveAndNegativeBehavior`, `v010PrivilegeFailuresAre42501AndReadOnlyAuthorizedWriteIs25006`, `v010LegacyMigrationDigestsRemainCanonical`. Thus source=CI=Failsafe class inventory is 64 + 10 + 10 + 8 + 8 = 100 Failsafe methods across 5 classes; total source/CI/XML counts become 67 Surefire + 100 Failsafe = 167. Keep the 64 `PostgresContainerIT` method names frozen. CI output and README totals use these exact arithmetic values.

## New integration-test mechanics

Run under the real `container-smoke` profile. Use dedicated synthetic non-owner LOGINs: a query login granted only `sharing_role`, and a command probe login granted `sharing_command_role`; do not grant either owner or superuser membership. From the query login, execute `SET ROLE sharing_command_role` and assert SQLSTATE `42501`, plus role-explicit `pg_has_role(query_login,'sharing_command_role','MEMBER') = false`. Membership-path assertion must inspect effective role membership, not a membership-filtered information_schema view.

Use direct `has_schema_privilege`, `has_table_privilege`, `has_column_privilege` with explicit role names and `aclexplode` over `pg_class.relacl`, `pg_attribute.attacl`, `pg_namespace.nspacl`, `pg_proc.proacl`, plus catalog membership. `information_schema` is supplemental only. Compare exact grants including grantor/grantee/privilege/grantable; include PUBLIC. Empty-denial sets are bounded to `sharing`, `evidence`, `customer_iam`, `organization_access`, `subject_number`, `platform`, and `audit` application objects; exclude system schemas and unrelated extensions. Role-effective positive tests execute after `SET LOCAL ROLE` and assert `current_user`.

All expected `42501` attempts occur in writable transactions, each isolated by a savepoint and rolled back to that savepoint before the next probe. Assert SQLSTATE exactly. The `25006` test is a command login that successfully `SET LOCAL ROLE sharing_command_role`, begins an actually read-only transaction, then attempts an otherwise-authorized UPDATE to a grant lifecycle field; assert `25006`. Also prove that the same update is authorized in a writable transaction. No query-role write is used to claim `25006`.

Test allowed query projections/lifecycle rows and denied secret/content (`capability_token_hash`, `code_salt`, `code_hash`, `content_snapshot`, `content_sha256`), `id`, admission/event, cross-module ungranted tables/columns, all writes, DDL, TRUNCATE, references/trigger, functions, and sequences as applicable. Positive command checks cover all-column SELECT/INSERT behavior, the invoker `SELECT *` admission guard, lifecycle updates on exactly eight columns, cross-module exact reads including `identity_at_capture` and membership `role`, and outbox/audit calls. Negative command checks cover UPDATE `max_admissions`, UPDATE admission/event, DELETE/TRUNCATE/REFERENCES/TRIGGER/DDL, unrelated namespaces/functions/tables, and PUBLIC-derived rights.

## Documentation and exact inventory changes

Implementation must update `backend/README.md` at lines/sites 40, 239, 255–258, 290, 297–299: V001–V010; 5 Failsafe classes, 100 Failsafe methods, 167 combined; `64+10+10+8+8`; default `verify` is not Failsafe evidence; CI uses `./mvnw -B -ntp -Pcontainer-smoke verify`; existing floating `postgres:16` family and major-version assertions remain (do not add a second `16.15` image policy). V010 changes only fixture ACL assertions, not production wiring.

Update docs/14 §3.1/§3.2, §4.44–4.46, §10.21, §10.22, rollout statements at current lines 4257–4262, and traceability rows `T-S3-27`/`T-S3-28`: describe V010, exact query and command roles/grants, remove broad `sharing_role` owned-table, evidence/subject/platform/audit and trigger-EXECUTE assertions, add command-role provisioning as deferred/unwired, no PUBLIC access, and preserve frozen semantics. Reconcile current line 285 and 332–334 statements. Clarify that trigger execution does not require direct EXECUTE and V010 revokes direct grants. Keep OpenAPI, current source/query, control document, old reviews, V001–V009 and unrelated docs unchanged.

## Validation and required evidence

From repository root with Docker available:

1. `cd backend && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT verify` — focused profile-scoped security IT; retain `target/failsafe-reports/TEST-...SharingRoleLeastPrivilegeIT.xml` and verify 8 tests, zero failures/errors/skips.
2. `cd backend && ./mvnw -B -ntp -Pcontainer-smoke verify` — full actual container profile, all Surefire/Failsafe suites. Plain `./mvnw verify` is not Failsafe evidence. Do not pass `-Dtest=none`.
3. Test asserts PostgreSQL family using `server_version_num >= 160000 AND < 170000`, matching all existing `postgres:16` fixtures and repository image policy. Assert V010 exact Flyway version/script/description, 10 applied rows/scripts, and `V001`–`V009` canonical-LF digests equal the existing `PostgresContainerIT.assertV001ThroughV009MigrationDigestsUnchanged` pinned values. V001–V009 raw bytes are additionally checked against the pre-edit worktree baseline.
4. Test both catalog and role-effective behavior: exact schema/table/column/function/sequence/PUBLIC ACLs and role membership; positive query/command operations; negative permissions with `42501`; separately authorized read-only mutation with `25006`; query login's explicit `SET ROLE sharing_command_role` fails `42501`.
5. CI check proves exact equality of source `@Test` inventories, POM Failsafe includes, CI expected dictionaries, and emitted Failsafe XML. Expected totals: 64+10+10+8+8=100 Failsafe, 67 Surefire, 167 combined; no duplicate class/methods; zero skips/errors/failures.
6. Confirm V001–V009 canonical hashes and original migration bytes unchanged; run `git diff --check`. Report unavailable Docker/runtime evidence as blocked, never as passed.

## Finding disposition and risk review

All accepted reviewer findings are fixed by this plan: command all-column reads and complete INSERT; exact eight UPDATE columns excluding `max_admissions`; column-ACL revocation and owner execution; complete test/fixture blast radius and counts; real Failsafe profile commands; exact cross-module grants and function signatures; README/docs reconciliation; bounded catalog/SQLSTATE mechanics; canonical and raw plan identity; existing PostgreSQL 16 image policy. No finding is rejected or deferred as a plan correction. The original goal and exact query-role read set are unchanged.

Overdesign remains constrained to one migration + one focused IT + required existing assertions/docs/inventory edits. Hidden coupling is expressly addressed by fixture/history/CI count updates. Residual risks: no runtime proof is produced by this planning artifact; production migration identity and PostgreSQL 16 execution still require implementation-time validation; command role is intentionally unwired and its application-level semantics, auth/admission policy, credential provisioning, concurrency, expiry scheduling, KDF/OTP, media authorization, Redis and deployment containment remain outside this work unit. The exact function signatures must be copied from V009/catalog and asserted in the new IT; no signature may be guessed.

## Completion boundary

This is a plan-only fix. Only the plan and its companion fix record may change. Stop after author validation; do not dispatch re-review, implement V010, commit, push, create/mutate a PR, request reviewers, comment externally, approve findings, or enter a later gate.
