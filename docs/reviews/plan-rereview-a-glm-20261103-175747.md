# PA13 plan re-review - Reviewer A GLM fallback

## Review identity

- Task label: `pa13-plan-rereview-a-glm53flash-20261103-175747`
- Reviewer: A fallback, GLM Flash
- Input: fixed plan and plan fix 1 on the isolated PA13 snapshot
- Binding scope: PA13 Goal Confirmation in `docs/gateflow/project-execution-control.md:15-35`
- Skill note: the named `planreview` skill was not installed in this session; this review applies the requested Planreview structure directly.

## Conclusion

`pass-with-risks`

No new material plan defect or open blocking question was found. The fixed plan closes every prior accepted finding with executable, bounded details. The risks are the expected deferred runtime proof, deployment migration-identity proof, and the inability to verify a Git commit object from this file-only snapshot.

## Frozen-input identity

| Artifact | Raw snapshot SHA-256 | Canonical-LF SHA-256 | Result |
| --- | --- | --- | --- |
| `docs/gateflow/pa13-sharing-role-least-privilege-plan.md` | `32569007C927009AD094D8A4E560B38A9000B1582FFB8530E7BD6A1AE7E0E51A` | `253953C6A5063B2F147BA01A9AB742A419FCB6DD648C1F6AC19F4D5096B909C5` | Matches supplied plan snapshot; CRLF bytes normalize to the control-record LF hash. |
| `docs/gateflow/pa13-sharing-role-least-privilege-plan-fix-1.md` | `38A4F22900B10E2F2CCA7F5B332555464622DF3034531CA15676FCE68C8D0840` | `5671B82D08A2F71B6B37AAF73E75B34859F0D58BFCAA5BAF4E3CA7C0D081527E` | Matches supplied fix snapshot and control-record LF hash. |
| `docs/gateflow/project-execution-control.md` | `FDCC3E1E71CD47969B5A5D56D0BA118D267F72FB9D5AD238928AEF93B109F2B9` | `565AE074786452FD3DA862400AF074D997BE672E4E74F86FECA6CB3C13C95C27` | Matches supplied control snapshot. |

The fixed plan and fix artifact both retain the original frozen-plan identity at `docs/gateflow/pa13-sharing-role-least-privilege-plan.md:8` and `docs/gateflow/pa13-sharing-role-least-privilege-plan-fix-1.md:8`: raw CRLF `53B9FB2FBB5FD2425C61B95A76BFE41CDF9E8B319545A7D5DA150F6951CC840E` and canonical-LF `B67A19F134A3134BB15CD5A9471DF27ED70E26CB8D2A546189EC8EF40A4B1290`. This is an input identity, not a self-hash of the rewritten fixed plan.

The supplied plan commit `e5a3468da932d506bbea9ff55374d61a739ac071` could not be independently resolved because this isolated workspace is file-only: there is no `.git` repository, and `.codex` is an empty marker. The byte hashes above still establish that the submitted files are the frozen inputs. This is an evidence limitation, not a plan defect.

## Prior-finding closure

| Prior finding | Closure evidence | Status |
| --- | --- | --- |
| Command role full sharing-table reads | The plan grants all-column `SELECT` to all three sharing tables and explains the SECURITY INVOKER admission guard's `SELECT *` plus preserved `to_jsonb(row)` tests. V009 confirms `sharing.guard_share_admission()` reads `SELECT *` at `backend/src/main/resources/db/migration/V009__sharing.sql:256-278`; full-row probes exist in `PostgresContainerIT`. | Fixed |
| No `UPDATE(max_admissions)` | The command UPDATE list is exactly `status`, `admissions_used`, `revocation_generation`, `revision`, `expires_at`, `code_salt`, `code_hash`, and `code_generation`; `max_admissions` is expressly excluded. The plan also requires a negative probe and an otherwise-authorized lifecycle update. | Fixed |
| Owner-role table/column revocation | V010's ordered contract uses `sharing_owner`, `evidence_owner`, `customer_iam_owner`, `organization_access_owner`, `subject_number_owner`, `platform_reliability_owner`, and `audit_owner` blocks. It explicitly revokes relation ACLs, every old column ACL, relevant PUBLIC ACLs, and stale function grants before exact regrants; `REVOKE ALL ON TABLE` alone is prohibited. | Fixed |
| Exact blast radius | The plan names the exact `PostgresContainerIT` ACL/history/role-set/behavior paths, migration-count fixtures, query-service and customer-share test updates, new class, POM include, CI inventory, and README sites. Current source confirms 64 `PostgresContainerIT` methods, 4 current Failsafe classes, and the affected fixtures/sites. | Fixed |
| Real container validation | Both focused and full validation require `-Pcontainer-smoke`; full verification retains XML and requires 8/0/0/0. Plain `verify` is explicitly not Failsafe evidence, and `-Dtest=none` is prohibited. | Fixed |
| Exact cross-module columns | The command grant lists include all 10 frozen `evidence.evidence_record` columns including `identity_at_capture`, all three artifact columns, and membership `role`; every other cross-module column list matches V009 and docs/14 §10.21. | Fixed |
| Exact function signatures | V010 pins `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` and `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)`, both returning `uuid`. These signatures match V001. No platform idempotency function is granted. | Fixed |
| docs/14 reconciliation | The plan names §3.1/§3.2, §4.44–4.46, §10.21, §10.22, rollout lines 4257–4262, current lines 285 and 332–334, and traceability rows `T-S3-27`/`T-S3-28`. Those locations contain the stale sharing-role, trigger, ownership, and V009-only claims. | Fixed |
| Catalog assertions and error mechanics | The plan requires role-explicit `has_*_privilege`, `aclexplode`, grantor/grantee/privilege/grantable, PUBLIC, recursive membership checks, bounded application namespaces, savepoint-isolated writable-transaction `42501` probes, and a genuinely authorized command-role read-only transaction for `25006`. | Fixed |
| Raw/LF hash identity | Original raw-CRLF and canonical-LF identities are retained. The supplied fixed-snapshot raw hashes and recomputed canonical-LF hashes reconcile with the controller record. | Fixed |

## PostgreSQL 16 verification

- Object owners and grantors: V009 grants are made under the corresponding owner block, while the initial `sharing`/`evidence` schema grants precede `SET LOCAL ROLE sharing_owner` and are therefore made by the migration superuser. An owner can revoke grants made by another grantor; V010 nevertheless executes each revoke under the owning role and avoids relying on grantor substitution.
- PUBLIC/default ACLs: every application function created by V001–V009 has an explicit `REVOKE ... FROM PUBLIC`. V010 further revokes relevant PUBLIC grants without a broad default-ACL reset. This is consistent with PostgreSQL default `EXECUTE` semantics.
- Inheritance/membership: `sharing_role` is already `NOINHERIT`; `sharing_command_role` is created `NOINHERIT`. The plan denies any membership path, requires recursive path evidence, and proves `SET ROLE` denial plus `pg_has_role(..., 'MEMBER') = false` rather than relying on a filtered view.
- Column ACLs: PostgreSQL relation-level revocation is not treated as sufficient. V010 requires explicit revokes naming every column, then regrants only exact columns. This also handles stale `pg_attribute.attacl` entries.
- Triggers: `guard_share_grant()`, `guard_share_admission()`, and `reject_row_mutation()` are SECURITY INVOKER trigger functions. Existing trigger invocation does not require the runtime role to have direct `EXECUTE`; V010 removes those obsolete grants while retaining owner-local structural controls.
- Function signatures and observability: the two allowed SECURITY DEFINER signatures and return types match V001. Exact catalog proof includes `pg_class.relacl`, `pg_attribute.attacl`, `pg_namespace.nspacl`, `pg_proc.proacl`, PUBLIC, grantor, grantability, and effective privilege checks.

## Security and behavior boundaries

- Query role: receives `USAGE` only on `sharing`, `customer_iam`, and `organization_access`; 13 exact sharing-column reads; and the exact account/organization/membership columns used by `JooqShareQueryRepository`. It has no sharing table ACL, secret/content access, admission/event access, unrelated schema, sequence, function, write, DDL, or command-role path.
- Command role: receives `USAGE` only on the seven named application schemas, full-row SELECT/INSERT on the three frozen sharing tables, exact eight-column grant UPDATE, exact frozen cross-module reads, and exactly two outbox/audit function EXECUTE grants. It has no DELETE/TRUNCATE/REFERENCES/TRIGGER/CREATE/sequence/platform-table/audit-table privilege and is not wired to any endpoint or login.
- Future behavior: all-column sharing reads/inserts preserve the admission guard, append/event behavior, full-row JSON probes, FK backstops, rotation/extension/lifecycle updates, and outbox/audit calls while `max_admissions` remains immutable at both ACL and trigger levels.

## Inventory and arithmetic

Current source contains exactly 64 `@Test` methods in `PostgresContainerIT`, 10 in `SharingQueryServiceIT`, 10 in `CustomerMeIT`, and 8 in `CustomerSharesIT`. The plan adds exactly 8 methods to a new Failsafe class, yielding `64 + 10 + 10 + 8 + 8 = 100` across five Failsafe classes and `67 + 100 = 167` total. Current POM has four Failsafe includes; the plan adds one. Current CI total is 92; the plan updates it to 100 and the combined total to 167. README sites 40, 239, 255–258, 290, and 297–299 match the plan's declared update surface. Flyway counts change correctly from 9 to 10 in the named fixtures and assertions.

## Single-slice readiness

The slice remains one transactional V010 migration plus one focused security IT and bounded updates to existing assertions/inventory/docs. No production query/API/OpenAPI/default-startup behavior is authorized. All SQL object names, role names, owner roles, column lists, function signatures, privilege ordering, migration/history counts, method names, class counts, and aggregate test totals are present. The package/path for `SharingRoleLeastPrivilegeIT` is strongly implied by the existing container-test package and POM wildcard; this is not a semantic design gap.

## Open questions

None blocking.

## Residual risks

- R1 (Low): No PostgreSQL 16 or Maven/Failsafe execution was performed in this plan-only review. Actual catalog values, trigger behavior, source=CI=XML equality, and 167/0/0/0 remain S1 proof obligations.
- R2 (Low): The file-only snapshot cannot verify plan commit `e5a3468da932d506bbea9ff55374d61a739ac071`. The controller must reconcile the commit object when Git metadata is available.
- R3 (Low): Production migration identity must independently prove superuser-equivalent ability to administer roles and assume every owner. The plan correctly makes this a deployment precondition rather than assuming ordinary object ownership.
- R4 (Low): Exact catalog assertions should preserve the existing `coalesce(acl, acldefault(...))` pattern when checking null/default ACLs. The plan requires PUBLIC inclusion and points to role-effective checks, but does not spell out that helper implementation detail.
- R5 (Low): The new security IT may select test-local synthetic login names and package placement. These are non-production fixture choices and must not leak into V010 or application configuration.

## Artifact-only statement

This review changed only `docs/reviews/plan-rereview-a-glm-20261103-175747.md`. It did not modify the plan, fix artifact, control document, source, tests, migrations, inputs, prior peer artifacts, or any other repository path. It performed no implementation, commit, push, approval, request-for-changes, or external action.
