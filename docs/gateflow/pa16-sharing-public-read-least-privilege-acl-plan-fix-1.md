# PA16 Public-Read Least-Privilege ACL — Plan Fix 1

## 1. Identity, Authority And Status

- Work unit: `PA16-sharing-public-read-least-privilege-acl`.
- Task label: `pa16-public-read-acl-plan-fix1-gpt-20261106`; date: 2026-11-06, Asia/Shanghai.
- Gate: `fix` following `plan review`; status: **author-side plan repair complete; pending Controller validation**. No implementation or re-review is authorized or started here.
- Sole fix author; no delegation or parallel agents were used.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa16-sharing-public-read-least-privilege-acl/codex-call`.
- Branch: `codex/pa16-sharing-public-read-least-privilege-acl`; base HEAD remains `0ccff786a6877229c278d0584423a65014669eed`.
- Binding authority: current goal confirmation and accepted review adjudication in `docs/gateflow/project-execution-control.md`; the findings were implemented as adjudicated, not re-litigated.

## 2. Exact Write Scope And Artifact Identities

Exactly two author-written repository paths:

1. Modified `docs/gateflow/pa16-sharing-public-read-least-privilege-acl-plan.md`.
2. Created `docs/gateflow/pa16-sharing-public-read-least-privilege-acl-plan-fix-1.md` (this record).

| Plan identity | LF line count (`wc -l`) | SHA-256 of actual bytes |
| --- | --- | --- |
| Before fix, verified against binding input | 734 | `E8732EDC7D5159B5047BF178B66B24E3B42BDABFE78E4052F47279A2CBC62725` |
| After fix | 765 | `400F7E69ECD55480C0D05DC7DFDA29DB1BF4C0EA011B8E680F1F352B00D2BE21` |

This record's exact final SHA-256 and line count are computed after writing and recorded in the completion report, detached from this file to avoid a circular self-hash. No third manifest/report path is created.

Re-read and verified review inputs (actual LF counts; hashes are the binding identities):

- Reviewer A: `docs/reviews/plan-review-20261106-095836.md`, 91 lines, SHA-256 `84ED67CA0611F08DDAA22EEFF7A8195AD92F89C51DE8D4973028AAE3564D9A75`.
- Reviewer B: `docs/reviews/plan-review-20261106-102208.md`, 97 lines, SHA-256 `A6C71330C5B2642136EB402AE3B0D61D23A92CB84488D927E1CC78A347CFFB76`.
- Current read-only Control: 3,796 lines, SHA-256 `3582098DC74DF689BA145D9CB788E73332E6253D94DA882F7A289C25AC9CE245`. Its already-modified state predates this task and is preserved.

## 3. Finding And Question Dispositions

| Controller item | Disposition and exact plan repair |
| --- | --- |
| A-F-01 / B-Finding-1 | **Resolved in plan**, §§1, 6–8. Remove the separate properties production path. Define the public static record inside allowlisted `PublicSharePersistenceConfiguration`; pin binding prefix, enclosing registration, public compact constructor, null/blank/exact-username checks and redacted `toString`. Tests import the nested type. Future implementation inventory is exactly 16 = 15 modified + one new V011. Preserve the protected architecture file's exact 8-public/13-query/19-command file sets without adding it to the allowlist. |
| A-F-02 | **Resolved in plan**, §§6, 8–9. Public reader method has exactly `@ConditionalOnProperty(prefix="onecall.sharing-public-read.datasource", name="url", matchIfMissing=false)`, in addition to the existing class profile/command conditions. Command-only URL-absent contexts have exactly the original reader and retain by-type lookup compatibility. Valid public contexts have exactly two named readers. Both `PublicSharesIT` lookups select `publicShareableEvidenceProjectionReader` by name. Existing configuration methods assert bean sets/counts, distinct managers, proxies, manager-specific annotations and missing-manager failure. No `@Primary`; no edit to protected `CustomerShareCommandsIT`. |
| A-F-03 | **Resolved in plan**, §§8–9. Public DSL retains `executions`; separate `sharingCommandDsl` listener records `commandExecutions`. Pin the expanded observation record, JDBC observation query, per-stream isolation and clearing. After public snapshots, clear the command stream, execute a command create, require a positive INSERT count before every-INSERT `readOnly=off` and command/session identity assertions. Require the public stream remains unchanged during the command. No vacuous `allMatch` evidence. |
| A-OQ-1 | **Deferred-with-owner**, unchanged. Current-database PUBLIC TEMP revoke is global and deployment-owned. Database/SRE must assess external consumers and escalate incompatibility to Controller; no restoration/waiver or production provisioning is authorized. |
| A-OQ-2 | **Resolved in plan**, §§9, 11. Complete role/ownership/membership/effective-ACL catalogs are authoritative for capabilities not individually probed. Add exact CREATE ROLE, CREATE DATABASE, CREATE VIEW, CREATE INDEX, MERGE and COPY FROM probes, each expecting 42501. CREATE DATABASE runs on a fresh autocommit connection outside a transaction to avoid 25001. COPY must be rejected before input mode, not accepted as a driver limitation. No helper role or persistent helper function is added. |
| A-OQ-3 | **Resolved in plan**, §8. Pin properties-only `ApplicationContextRunner` to nested `PublicReadPropertiesBindingOnly`, with only `@EnableConfigurationProperties` for the production nested record. Explicitly exclude full public configuration, readers/services/managers/datasources/auto-configuration. Invalid binding cannot fail for an unrelated reader. Require deepest `IllegalArgumentException`, exact sanitized credential message and no canaries; do not dump the outer binding exception. Direct constructor and valid-binding/redaction checks remain inside existing methods; full valid graph is exercised in IT. |
| A-OQ-4 | **Resolved in plan**, §§3, 5. Existing-role `rolpassword` content and nullness are excluded from compatibility rejection and snapshots. Only fresh creation before credential assignment may query the boolean `rolpassword IS NULL`. No existing password hash may be selected/logged. Replay after disposable password assignment must succeed without reading the hash. |
| A-OQ-5 | **Retained and made explicit**, §§8–9. Existing renamed public-projection IT invokes the actual proxy inside the actual public template/manager, requiring Found, narrow identities, READ ONLY and same public PID for repository/evidence SQL. No transaction or only an ambient command transaction fails MANDATORY before reader SQL. A reader-only configuration context missing the named public manager fails invocation with `NoSuchBeanDefinitionException`, never falls back to command. No extra JUnit method is added. |

These are author-side plan dispositions, not a replacement review verdict or a runtime acceptance claim. Both original review verdicts remain untouched.

## 4. Preserved Scope And Arithmetic

- **One end-to-end ACL/isolation behavior slice**: privileges + isolated public pool/manager/DSL + evidence-owned MANDATORY adapter + database/API regression evidence.
- **16 future implementation paths**: 15 existing modified paths and only new `V011__sharing_public_read_least_privilege.sql`. The two documentation paths at this fix gate are separate from that future implementation allowlist.
- No new production Java file. `SharingQueryBoundaryTests` and `CustomerShareCommandsIT` stay outside the implementation allowlist and unchanged. The production nested record and nested test helper are not new test classes/methods.
- Current **19 Surefire classes / 91 methods + 7 Failsafe classes / 126 methods = 217**, unchanged source/CI identities. Future **91 + 130 = 221 in the same 26 classes**, with exactly four new V011 Failsafe methods and the same two method renames. ACL class 8→12; public IT 8→8; command IT 18→18; configuration tests 2→2. Focused Surefire 24 and targeted Failsafe 20 overlap the full total.
- Exact **24 allowed columns / five tables**, complements **44 + 26 = 70** individual denied columns. Preserve two schema USAGE grants, no table-level SELECT, memberships, ownership, function/sequence/write/row-lock/DDL/escalation or admission/access-event/audit/Outbox access.
- V001–V010 byte preservation and privilege-only V011 remain binding. Latest history 10→11 is distinct from nine structural waves. PA15 API/redaction/problem/no-write/member behavior and all create/revoke/rotate/list/me regressions remain required.
- CI retains the same classes, replaces only the two method names, adds only the four V011 names and updates 126→130 / 217→221. README anchors distinguish current PA16 totals/topology/history from historical statements and document only the dedicated property names and deployment-owned authentication, never credentials or policy values.
- Control's original 17-path intake/adjudication wording is not edited here. Controller owns reconciling it to this expressly authorized nested-record correction and exactly 16 future paths before any implementation authorization.

## 5. Read-Only Validation Actually Performed

1. Read both complete review artifacts, current Control goal/adjudication, and applicable AGENTS instructions. Recomputed all binding input SHA-256 identities; the before-plan hash and 734 lines matched exactly.
2. Re-read cited source: public configuration/service/repository, command configuration, evidence reader/configuration, protected architecture inventory, configuration tests, public fixture/IT and protected command IT lookup sites. Confirmed command-only configuration registration and all three by-type command-reader lookups; confirmed both public lookup sites and the original public-fixture listener/INSERT assertion failure mechanism.
3. Re-read CI literal inventories/checker and README count/history/property anchors; migration-history/role anchors in PostgresContainerIT and the V010 ACL test were inspected without edits. Read V008/V009 DDL and V010 privilege source, and rechecked the fixture migration-count anchors.
4. Python 3 read-only text/inventory checks evaluated only CI literal inventory assignments/updates and extracted actual `@Test void` identities. Verified exact source=CI=Appendix A, 19/91 + 7/126 = 217 across 26 classes, no duplicate/conditional/parameterized substitution, exactly four proposed new methods and two proposed renames, and 91+130=221. No XML/runtime result is claimed.
5. Parsed the future implementation allowlist: exactly 16 unique paths, 15 currently existing and one absent V011 resource, with both protected tests excluded and no standalone properties Java path anywhere in the revised plan. Reconciled the protected architecture sets: 8 public + 13 query + 19 command = 40 existing files.
6. Recomputed V008/V009 column inventories against the permitted projection: table counts 20=9+11, 17=6+11, 5=3+2, 11=4+7, 15=2+13; fully denied tables have 7+11+8 columns. Confirmed 24 allowed and 44+26=70 denied. Verified all six additional negative probe rows explicitly require 42501 and CREATE DATABASE excludes transaction/savepoint context.
7. Rehashed every V001–V010 file: all ten raw-byte and all ten canonical-LF hashes match the frozen plan tables. Confirmed V011 remains absent. Text search found no temporary-table dependency in `backend/src`, `.github`, `acceptance`, `security` or `protocol`; this does not inventory external deployment consumers.
8. Used only read-only Git inspection with `GIT_OPTIONAL_LOCKS=0`, `GIT_NO_LAZY_FETCH=1`, explicit `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call16` and `GIT_WORK_TREE` to bypass the existing Windows pointer without repairing it. HEAD remains the base. Tracked diff names contain only the pre-existing Controller change; source/tests/migrations/CI/README have no tracked diff. `git diff --check` passes for tracked changes; separate text checks cover the untracked plan/fix Markdown files, since Git's tracked diff does not include them.
9. Checked balanced Markdown fences, no trailing whitespace, exact final document hashes/line counts, protected review/Control hashes and final status. Validation-harness retries were text-only: `python` was unavailable, so `python3` was used; CI extraction was corrected for a truncated block and its referenced literal inventories, and column/name extraction was corrected for digits in `sha256` and method-only rename destinations. Corrected checks passed; no application test/build was run during those retries.

Implementation commands in plan §9 are future obligations only: focused six-class Surefire (24), targeted ACL/public Failsafe (20), full clean container verify (221), source=CI=XML checker, existing packaged-JAR smokes and 82 dependency-firewall probes. They were **not run** in this gate. No Maven, Docker, PostgreSQL, production connection, hosted CI or web/network action occurred.

## 6. Residual Owners And Next Boundary

- **Controller**: validate this fix and its detached identities, reconcile the 16-path intake, then own any freeze/re-review/implementation authorization. No gate-pass/review approval is asserted by the author.
- **Database/SRE**: deployment compatibility and consumer inventory for global PUBLIC TEMP removal; no automatic restore or waiver, including rollback after a successful forward-only migration. Escalate incompatibility to Controller.
- **Database/Security/SRE**: fixed cluster role-name collision, authentication/pg_hba/TLS, credential distribution/rotation, cross-database ACLs and drift. No production credentials, provisioning or policy selection is delivered.
- **Backend implementation owner**: execute real PG16 SQLSTATE, migration replay/refusal/rollback, public-manager MANDATORY/proxy, command-only bean compatibility, dual-listener positive INSERT, same-PID/isolation/concurrency and all regression tests. Static validation cannot replace that evidence or authorize broader privileges.
- **Backend/SRE and Controller/Platform**: pool capacity/abuse review and later independent acceptance/hosted evidence. **Controller/Specification** retains historical docs/14 reconciliation. Existing IAM/Security/Product/admission/media work remains outside this slice.

## 7. Action-Boundary Confirmation

Only the two scoped Markdown paths were written. No source/test/migration/CI/README/Control/review/private-scaffolding file was changed. No credentials or secrets are included. No Git state mutation occurred: no stage, commit, fetch, push, branch/worktree creation or configuration repair. No remote/PR/issue/comment/approval/ready/reviewer action, implementation, deployment or re-review occurred. Existing Controller/private/untracked state is preserved, not cleaned up or claimed as this author's change.
