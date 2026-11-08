# PA19A plan fix 2

Task: `pa19a-sharing-evidence-media-read-acl-plan-fix2-gpt-20261108`  
Gate: fix round 2  
Base plan SHA256: `5A0AA90D92245479EC351F7707134805C20F3E7841F0E922B3ED8D5E47FE5F3D`

## Applied corrections

- `RR1-01` — Fixed plan §8, V013 structure/replay: requires PostgreSQL 16, superuser, exactly one successful V012 history row, and permits absent or exactly one successful V013 row. Failed/duplicate/incompatible V013 history is rejected; V012 need not remain latest. Replay verification explicitly runs SQL twice successfully on a compatible fully migrated fixture, then requires ordinary `flyway.migrate()` to execute zero migrations.
- `RR1-02` — Fixed plan §V013 exact privilege matrix and assertions: defines exact `pg_shdepend` dependency types, only expected ACL dependencies for the four granted tables, none pre-grant, and rejects membership, ownership, role settings, non-ACL dependencies, and unexpected ACL grants. `aclexplode`/`relacl`/`attacl` predicates and effective-privilege checks are required, with replay assertions.
- `RR1-03` — Fixed plan §8 and role matrix: uses superuser-only `pg_authid.rolpassword IS NULL` before action and after `RESET ROLE`; never reads, echoes, or stores the password value. `pg_roles.rolpassword` is not used as proof.
- `RR1-04` — Fixed plan Commands item 1: replaces `V00*.sql` with `sha256sum src/main/resources/db/migration/V0{01..12}__*.sql`, with exactly 12 ordered rows asserted before digest comparison.
- Database privilege handling — Fixed plan §8 and privilege matrix: direct database CREATE/TEMP incompatibility fails the superuser precondition; no database REVOKE is claimed under `evidence_owner`. V011's PUBLIC TEMP revocation remains baseline evidence. Only evidence schema/table grants execute under `SET LOCAL ROLE evidence_owner`.
- Precision — The exact 22-path allowlist remains unchanged. `EvidenceMediaObjectReader.Result`, `Found`, `Missing`, and `Unavailable` remain nested in the interface file. The `>1` mapping test uses mocked/fake records because V008's unique partial index forbids a real duplicate ORIGINAL row. Configuration is `@Profile("persistence")` plus explicit enablement; absent/false is inert and preserves exact existing datasource/DSL/transaction-template bean counts. One null-UUID `NullPointerException` contract and redaction remain intact.

## Scope and disposition

No role name, column set, query, DTO, allowlist, test-count arithmetic, goal, or non-goal changed. No schema object, storage-location grant, sharing widening, module narrowing, API/OpenAPI change, or future scaffolding was added. The plan retains one coherent implementation slice and its existing residual owners.

PR-01 through PR-06 remain closed. RR1-01 through RR1-04 are repaired as specified. No new blocker identified by this bounded document fix; git metadata inspection is unavailable because the worktree's `.git` pointer resolves to a missing external worktree path.
