# PA16 Sharing Public-Read Least-Privilege ACL - Final Closeout

## Identity And Status

- Work unit: `PA16-sharing-public-read-least-privilege-acl`.
- Status: `final closeout pass`; one end-to-end database/security slice completed.
- Base: `origin/main` merge `0ccff786a6877229c278d0584423a65014669eed`.
- Accepted plan checkpoint: `685bd50`.
- Accepted slice checkpoint: `1c37c76`.
- Accepted deepreview checkpoint: `21d748c`.
- Accepted PR-review checkpoint: `12e5bc35fd0e4fd823fd9afe6ffdee3f03103430`.
- Draft PR: https://github.com/zh94695424/one-call/pull/26, state OPEN, `isDraft=true`, base `main`, head `codex/pa16-sharing-public-read-least-privilege-acl`, `mergeable=MERGEABLE`.
- No issue was linked because this work unit was a planned backend/database phase, not a GitHub issue.

## What Changed

The slice adds privilege-only Flyway `V011` and the minimum runtime wiring needed by the existing public-share GET:

- creates direct-login `sharing_public_read_role` with no usable migration password, no membership, inheritance, ownership, settings or escalation;
- revokes only current-database `PUBLIC TEMPORARY`;
- grants exactly two schema `USAGE` and 24 column-level `SELECT` privileges across five existing projection tables;
- moves public GET to a dedicated datasource, enforced READ ONLY transaction manager/template and prepared/indexed jOOQ context;
- adds an evidence-owned MANDATORY projection adapter conditionally activated by the dedicated public datasource URL;
- verifies both `current_user` and `session_user` are the public role and never switches to command/evidence roles;
- preserves the PA15 public API/redaction/state/problem behavior and all create/revoke/rotate/list/`me` regressions;
- leaves V001-V010 and all sharing table definitions, constraints and lifecycles byte-for-byte unchanged.

The implementation candidate is exactly 16 paths: 15 modified files and new V011. Implementation, code-review-fix and Gateflow review artifacts are separate reporting authority.

## Verification

- Focused six-class Surefire: 24/24.
- Targeted PostgreSQL 16.15 `SharingRoleLeastPrivilegeIT` + `PublicSharesIT`: 12 + 8 = 20/20.
- Fresh clean `-Pcontainer-smoke verify`: 19 Surefire classes / 91 methods + 7 Failsafe classes / 130 methods = 221, zero failures/errors/skips.
- Exact source=CI=XML inventory: 26 classes / 221 methods, exactly four added and two approved renamed methods.
- V011 PostgreSQL 16.15 evidence covers upgrade/repeat/replay/rollback, exact 2 schema + 24 column ACLs, 70 denied columns, `42501` denials, READ ONLY `25006`, role escalation, DML/DDL/MERGE/COPY, no tuple locks, no durable writes and `P0001`/`22012` incompatible-state handling.
- Concurrency/reset evidence is real: two simultaneous public PIDs 108/111 with narrow identity and READ ONLY; simultaneous public/command PIDs 111/109 with independent identity and reset; original single-PID `25006` reset and positive writable command INSERT retained.
- Packaged-JAR smokes passed; all 82 dependency-firewall probes passed.
- Remote GitHub Actions run `42170352314` on reviewed head `12e5bc3` passed both required jobs: `contract-regressions` and `java-runtime`.

## Documentation And Findings

- Accepted plan/fixes, implementation, code-review fix and all plan/code/aggregate/PR review artifacts are committed.
- Accepted plan findings A-F-01, A-F-02 and A-F-03 are fixed.
- Accepted code-review finding A-F-CONCURRENCY is fixed with bounded two-public and simultaneous public/command witnesses.
- Both aggregate deepreviews and both PR reviews returned `pass`/`pass-with-risks` with zero material findings.
- No accepted finding remains open.

## Remaining Risks And Owners

| Risk | Owner / destination |
| --- | --- |
| Current-database `PUBLIC TEMPORARY` removal may affect external consumers sharing the database. | Database/SRE deployment compatibility review; incompatibility must be escalated, not waived by broadening the role. |
| Production credentials, pg_hba/TLS, password rotation, role-name collision and post-deploy ACL drift are not provisioned by V011. | Database/Security/SRE deployment work. |
| Dedicated pool capacity, abuse/load behavior and production sizing are not validated. | Backend/SRE capacity and abuse review. |
| The public role can read all rows of the 24 allowed columns; row-scoped access depends on the application's token lookup, not PostgreSQL RLS. | Security + Database threat-model decision if row-level isolation is later required. |
| Foreign-server and large-object ACL families are covered indirectly rather than via dedicated ACL-enumeration tests. | Backend/Database test-maintenance. |
| Same-PID reset assertions rely on bounded Hikari thread affinity/soft eviction; HTTP envelope determinism is tested sequentially rather than byte-compared across the six concurrent requests. | Backend test-maintenance. |
| Production customer-session verifier, token format/KDF/TTL, admission/ticket/media authorization and hosted production operation remain later work units. | Customer IAM/Security/Product/Sharing Backend/Platform. |
| Historical `docs/14` prose is not edited by this slice. | Controller/Specification documentation reconciliation. |

## User Actions And Next Entry Point

PR #26 remains an open draft. The user may mark it ready and merge it when satisfied. This work unit did not merge, approve, mark ready, request reviewers, send external comments, delete branches or mutate issues.

After PR #26 is merged, fetch the updated `origin/main`, create a fresh managed worktree and `codex/` branch, and begin a new Phaseflow goal confirmation for the next bounded public-sharing admission/ticket/media slice. Do not reuse PA16 goal confirmation or continue from this unmerged branch.
