# PA13 sharing-role least-privilege plan fix 1

- Task label: `pa13-sharing-role-least-privilege-plan-fix1-gpt-20261103-01`
- Work unit: `PA13-sharing-role-least-privilege`
- Gate: `fix`
- Status: `plan-fix-1-complete`
- Date: 2026-11-03
- Frozen plan input: raw CRLF SHA-256 `53B9FB2FBB5FD2425C61B95A76BFE41CDF9E8B319545A7D5DA150F6951CC840E`; canonical-LF SHA-256 `B67A19F134A3134BB15CD5A9471DF27ED70E26CB8D2A546189EC8EF40A4B1290`. Both retained; neither frozen source nor prior history was rewritten.
- Reviewer inputs: A `docs/reviews/plan-review-20261103-161145.md`, SHA-256 `10FA1BECF1CEDE67023602D0AC427E58D0ECB358B6D4A55A36F9C0CFB5541A6D`; B `docs/reviews/plan-review-20261103-161146.md`, SHA-256 `1444CB7A0FF84C6600029C5889FEFCD54BF97D4512436DE733CA2C1ACECF1B93`.

## Dispositions

Every accepted finding is fixed in the revised executable contract; none is rejected or left as an implementation-time derivation. No new scope or change to the confirmed goal/query-role read set is introduced.

| Input finding | Disposition | Plan correction |
| --- | --- | --- |
| A F-01 / B B-03 | Fixed | Command role has all-column SELECT and INSERT on all three sharing tables; rationale pins invoker `SELECT *` and full-row frozen tests. |
| A F-02 | Fixed | Exact eight grant UPDATE columns; `max_admissions` excluded. |
| A F-03 / B B-07 | Fixed | V010 order pins superuser migration identity, every owner-role block, explicit table and column ACL revokes, exact re-grants, no CASCADE, and catalog end state. |
| A F-04 / B B-01 | Fixed | Exact existing-test/fixture/role-set/history/CI/POM/README sites and expected deltas enumerated; 64 `PostgresContainerIT` method names remain unchanged. |
| A F-05 / B B-02 | Fixed | Both focused and full commands activate `-Pcontainer-smoke`; explicitly disallow plain `verify` as Failsafe evidence and omit `-Dtest=none`. |
| A F-06 / B B-04 | Fixed | Full cross-module columns include `identity_at_capture` and membership `role`; exact outbox/audit signatures and role grants stated; no “derive before implementation” remains. |
| A F-07 / B B-09 | Fixed | README count sites and docs/14 §3, §4, §10.21/10.22, rollout, traceability, trigger wording and command-role rollout are listed. Retain repository `postgres:16` family and major assertion. |
| A F-08 / B B-05 / B B-08 | Fixed | Explicit role-targeted privilege functions/`aclexplode`, app-schema bounds, explicit query-login SET ROLE failure, savepoint-isolated `42501`, and authorized command mutation `25006`. |
| B B-06 | Fixed | Both raw-CRLF and canonical-LF frozen-plan hashes are recorded and retained. |

## Files changed

- `docs/gateflow/pa13-sharing-role-least-privilege-plan.md` — rewritten as a code-generation-ready, plan-only contract.
- `docs/gateflow/pa13-sharing-role-least-privilege-plan-fix-1.md` — this disposition and author-validation record.

No source, migration, test, README, CI, design/control document, prior review, Git metadata, or other repository file is changed. No temporary PostgreSQL experiment or external persistence was used.

## Author validation

- Compared both review findings against the revised plan and checked the exact query set against the accepted goal and reviewer-confirmed repository projection.
- Read V009 ownership blocks, frozen §10.21 grant matrix, function definitions/signatures, migration-count sites, POM Failsafe profile, CI inventories and README count sites to cross-check the plan.
- Confirmed intended counts: 64 existing PostgreSQL methods unchanged; source/CI/Failsafe inventory adds 8 new IT methods; Failsafe `92 + 8 = 100`; combined `159 + 8 = 167`; migration history 9 → 10; query-role exact schema USAGE 3; query sharing privileges comprise 13 column SELECTs and zero table-level privileges; command sharing ACL comprises table SELECT/INSERT on each of 3 tables and 8 grant UPDATE columns.
- Checked both authorized artifacts for trailing whitespace. `git diff --check` was attempted but Git could not resolve this worktree's `.git` indirection (`D:/codex-call/.git/worktrees/codex-call11`); therefore Git diff/scope validation is unavailable here. Unchanged V001–V009 hashes, Maven, PostgreSQL 16, catalog behavior, and source=CI=Failsafe XML runtime equality were not run in this plan-only gate. They are explicit implementation validation requirements, not claimed as passed.
- Scope review: only the two authorized documentation artifacts are intended to differ. No re-review dispatched; no implementation, approval, commit, push, PR mutation, reviewer request/comment, or later-gate action performed.

## Residual risks and open questions

- No runtime evidence exists yet. Implementation must run the real container profile and produce PostgreSQL catalog/behavior and Failsafe XML evidence.
- The production migration identity must be proven to meet the superuser/role-administration precondition and owner-role assumptions before deployment; production provisioning remains outside this work unit.
- Catalog assertions must account for `PUBLIC`, ownership, grantor, role inheritance/membership and PostgreSQL 16 effective privileges. The prescribed integration test is the proof point.
- The command role remains deliberately unwired. Application correctness, admission policy, credentials, concurrency, expiry scheduling, KDF/OTP, Redis and media authorization remain out of scope.
- No unresolved design question blocks author validation. Exact function signatures are pinned from V001 definitions and must be asserted in the new IT.

Artifact-only statement: this fix changes planning artifacts only and authorizes no implementation or external action.
