# PA13 sharing-role least-privilege final closeout

- Work unit: `PA13-sharing-role-least-privilege`
- Branch: `codex/pa13-sharing-role-least-privilege`
- Draft PR: https://github.com/zh94695424/one-call/pull/21
- Base: `a7438dfc96f6a7845787ab3ce4007436cf796607`
- Reviewed implementation + PR-review head: `4daada7e4f54b4eabd27fbe477d8f615b33a2a02`
- Status: `final closeout pass`
- Issue association: not applicable

## What changed

- Added the forward-only migration `V010__sharing_role_least_privilege.sql`.
- Narrowed `sharing_role` to the exact query-only projection required by `GET /api/v1/customer/shares`: 13 share columns plus exact account, organization and membership context reads.
- Added the separate, unwired `sharing_command_role` with exact three-table SELECT/INSERT, eight grant UPDATE columns, 34 cross-module SELECT columns, and only `platform.outbox_append` / `audit.append_fact` EXECUTE.
- Added fail-closed reset and postcondition coverage across the nine bounded schemas for relation kinds, all live columns, mixed routines/procedures, sequences, schema/object PUBLIC ACLs, grantors, grantability, role attributes and membership containment.
- Added PostgreSQL 16 tests for exact query/command matrices, role-effective denials, `42501`/`25006` SQLSTATEs, stale role/membership/attribute/column/sequence/procedure state, deterministic post-ACL rollback, positive outbox/audit calls, and current `/shares` compatibility.
- Synchronized POM Failsafe wiring, CI inventory, README and the production data-model specification.

No production Java, API/OpenAPI, frontend, command-role wiring, view or new service behavior was introduced. V001-V009 remain byte-identical.

## Verification

- Focused `SharingRoleLeastPrivilegeIT`: 8/8 passed.
- Full PostgreSQL 16.15 suite: 67 Surefire + 100 Failsafe = 167 passed, zero failures/errors/skips.
- Flyway history/current inventory: 10 migrations, ending at V010.
- Exact source/CI/XML inventory: 5 Failsafe classes / 100 methods and 13 Surefire classes / 67 methods matched.
- V001-V009 raw bytes, sizes, CRLF counts and canonical-LF hashes match the frozen pre-edit manifest.
- Aggregate deepreview A/B passed after fixes; final PR review passed after direct GitHub blob verification rejected the sole snapshot-only CRLF finding.
- GitHub Actions run `41721587351` on final reviewed head `4daada7e4f54b4eabd27fbe477d8f615b33a2a02`: `java-runtime` and `contract-regressions` both `success`.
- PR #21 was OPEN/Draft/MERGEABLE/CLEAN when `draft-PR-pass` was accepted.

## Documentation

- `docs/14-production-data-model-and-flyway-schema-spec.md` now distinguishes frozen V009 history from post-V010 privileges, documents the unwired command role and tenth Flyway/non-tenth-structural-wave status, and no longer assigns current sharing-table writes to `sharing_role`.
- `backend/README.md`, CI inventory and the PA13 implementation record match final executable evidence.

## Finding status

- All accepted plan-review, code-review, aggregate-review and PR-review findings are `fixed`.
- PR-review finding A F-01 is `rejected-with-reason`: its CRLF observation came from tar materialization. GitHub's PR tree resolves V010 to blob `bc4303fbbdb68b510a987983e7472105a712216a`; direct API bytes are 24,760 bytes, zero CRLF, SHA256 `FADD30A3C058C8847D734F83B22AF3A93B2C79AB92CBA1B0D1A8A547E5F4A343`, matching the local worktree and Git blob.
- No accepted finding remains open and no blocking question remains.

## Remaining risks and owners

| Risk | Owner | Destination |
| --- | --- | --- |
| `pg_default_acl` can reintroduce future-object grants | Platform/Database/Security | production role/default-ACL provisioning and rehearsal |
| Unexpected third-party routine grantors make V010 fail closed | Platform/Database/Security | production migration preflight and cleanup |
| Production login/provisioning and application wiring for `sharing_command_role` are not implemented | Backend/Sharing + Platform/Security | later sharing command-service work unit and production provisioning |
| Production-cluster rehearsal, backup/restore and emergency-revocation behavior are unverified | Platform/Database/Security | pre-production rollout |
| PostgreSQL evidence is bounded to the tested 16.15 environment | Platform/Database | supported-version matrix or production compatibility check |

## Next entry point

After the user merges PR #21, fetch the merged `main`, create a fresh managed worktree and `codex/` branch from the merge commit, then run fresh Phaseflow goal confirmation for the next bounded sharing work unit. The tentative candidate is `PA14-sharing-command-service-foundation`, built on the now-reviewed `sharing_command_role`; its exact service/API, policy, concurrency and production-wiring scope must be confirmed rather than inferred from PA13.
