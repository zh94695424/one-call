# PA19A sharing evidence media read ACL plan — fix 3

Task: `pa19a-sharing-evidence-media-read-acl-plan-fix3-gpt-20261108`
Gate: `fix` round 3

## Accepted correction

Applied accepted finding `RR2-01` to the exact `pg_shdepend` precondition/postcondition/replay predicate in the plan. The correct post-grant ACL dependency set contains exactly:

- 15 `deptype='a'`, `classid='pg_class'` column ACL rows on the four listed tables. Each expanded `attacl` item must match grantor `evidence_owner`, grantee `evidence_media_reader_role`, privilege `SELECT`, and `is_grantable=false`.
- One `deptype='a'`, `classid='pg_namespace'` row for schema `evidence`. Its expanded `nspacl` item must match grantor `evidence_owner`, grantee `evidence_media_reader_role`, privilege `USAGE`, and `is_grantable=false`.
- Zero ACL rows before grants. Every other classid, objid, deptype, ACL item, membership, role setting, ownership/non-ACL dependency, or unexpected grant is rejected.

The role IT must enumerate all 16 expected rows after initial grants and on replay; the pre-grant role must have zero rows. The plan retains the requirement that both V013 executions succeed and ordinary Flyway migration then executes zero migrations.

## Scope preserved

No changes to role attributes, granted columns, query, DTO/API, test counts, the 22-path implementation allowlist, source=CI=XML arithmetic, goals, non-goals, or residual owners. V001–V012 and source files remain untouched. This is a plan-only fix; it does not authorize implementation, review dispatch, approval, or any external action. No commit, push, PR, or review action was performed.

## Validation

The fixed plan’s exact predicate was checked against the accepted `15 pg_class + 1 pg_namespace` requirement. Final plan and fix-artifact bytes, lines, and SHA-256 are reported by the authoring task’s completion record.
