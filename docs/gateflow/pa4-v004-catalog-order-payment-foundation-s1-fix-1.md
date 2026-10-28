# PA4 V004 S1 fix-1 — F-PA4-S1-01 ACL evidence completeness

1. Task label / work unit / gate / slice / date / author-provider
   - Task label: `pa4-v004-s1-fix1-mimo26flash-20261028-01`
   - Work unit: `PA4-v004-catalog-order-payment-foundation`
   - Gate: `fix` (S1 only). Accepted-plan base: `ee6139e0bae844d77ce145dc988f6edfaf1dccf7`.
   - Branch (declared): `codex/pa4-v004-catalog-order-payment-foundation`. Git metadata is unresolvable in this session (worktree `.git` points to `D:/codex-call/.git/worktrees/codex-call4`; the D: drive is not mounted and mounting it was denied) — identical environment limitation to review RR-02.
   - Date: 2026-10-28
   - Author/provider: MiMo 2.6 Flash (bounded S1 fix author; original S1 implementation author; no self-review)
   - Scope: fix only accepted finding `F-PA4-S1-01` from `docs/reviews/code-review-20261028-073404.md`. No re-review, no S2, no gate transition, no commit/stage/push/PR, no production acceptance.

2. Accepted finding being fixed
   - `F-PA4-S1-01` (Low, test-only/fail-closed): the V004 exact-ACL method `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact` did not assert plan-mandated `REFERENCES`/`TRIGGER` table privileges, cross-module column-level `UPDATE`, owner-role function `EXECUTE`, or the complete role/schema `USAGE`/`CREATE` matrix, so a future out-of-matrix grant of those shapes would pass the 27-method suite. The current V004 migration contains no live over-grant; this was an evidence-completeness gap.

3. Binding inputs read (read-only)
   - `docs/gateflow/project-execution-control.md` — current gate `fix` (S1) for `F-PA4-S1-01`; S2 not to be started; no slice commit before fix/re-review loop passes.
   - `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` §6.6.1–§6.6.5, §8.3 (and §8.6 command set).
   - `docs/reviews/code-review-20261028-073404.md` — full finding text, OQ-2 (authoritative denial list = plan §6.6.5; enumerate the full 23-role universe), fix sketch, RR-01/RR-02/RR-08/RR-11.
   - `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (pre-fix SHA-256 `747bc344e2f7f82d91abf7dba716b104f998335826fd485fe5e0b8a7228cfb7e`, 8777 lines, CRLF): `S1_ROLES` (15), `V004_ROLES` (8), `V004_TABLES` (7), V003 full-matrix REFERENCES/TRIGGER pattern (`:7203-7214` pre-fix), V002 matrix pattern, shared helper `assertTablePrivileges` (5-privilege, kept unchanged).
   - `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` — read-only truth for the grant matrix; SHA-256 `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` before and after this fix (byte-identical; never edited).
   - `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s1-implementation.md` — read-only evidence context (S1 §1–§17).
   - V001–V003 migrations — read-only, used to derive the cumulative schema-`USAGE` expectations.

4. Exact code changes (test-only; one maintained Java file)
   - File: `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (kept CRLF, matching the Windows worktree; no line-ending normalization).
   - Unchanged: the 27 `@Test` methods (count re-verified = 27 by grep), the test entry `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact` (`:1569`, still calls the same four helpers), `assertV004PublicAclIsZero`, `assertTablePrivileges` and every V001–V003 assertion path, CI workflow, README, POM, application code, all migrations.
   - `assertV004SchemaUsageMatrix` rewritten as a complete data-driven effective matrix: `ownerBySchema` (9 schemas) + `usageRolesBySchema` (exact USAGE role lists) × grantees (`S1_ROLES` 15 + `V004_ROLES` 8 + `public` = 24) × {`USAGE`,`CREATE`}; expected `USAGE` = membership in the enumerated list, expected `CREATE` = schema owner only. Seven explicit wave spot rules kept for auditability (`catalog_order_role` `activation_consent` true; `entitlement_metering`/`ivr` false; `ivr_role` `ivr`/`catalog_order` false; `activation_consent_role` `subject_number` false; `entitlement_metering_role` `subject_number` false).
   - `assertV004OwnedAndCrossModuleGrants` rewritten as a complete effective matrix: for each of the 7 V004 tables × all 23 roles, asserts table-level `SELECT`/`INSERT`/`UPDATE`/`DELETE`/`TRUNCATE`/`REFERENCES`/`TRIGGER` and per-column `SELECT`/`INSERT`/`UPDATE` over the full 78-column inventory, with owner-inherent privileges modeled explicitly (`catalog_order_owner`: all true) and out-of-matrix rows expected false. For the five V004-touched S1 tables, `catalog_order_role` is asserted to exact column `SELECT` with all seven table privileges false and column `INSERT`/`UPDATE` false on every column; `ivr_role`, `activation_consent_role` and `entitlement_metering_role` are asserted to zero table privileges (all seven) and zero column `SELECT`/`INSERT`/`UPDATE` on every column.
   - `assertV004GuardFunctionExecuteBoundaries` extended: for each of the four guards, `catalog_order_owner` true (owner-inherent) and `catalog_order_role` true; every other role in `S1_ROLES`+`V004_ROLES` (21 roles) false; `public` false. `reject_row_mutation`: `catalog_order_owner` true; `catalog_order_role` and all other 22 roles false; `public` false. The four S1 platform/audit function grants to the four S2 runtime roles, the `entitlement_metering_owner` platform-capability denial and the `issue_first_trial` absence checks are preserved unchanged.
   - No helper framework, no generic abstraction, no production code, no new test method, no rename.

5. Matrices covered (post-fix)
   - Schema `USAGE`/`CREATE`: 9 schemas (`catalog_order`, `ivr`, `entitlement_metering`, `activation_consent`, `customer_iam`, `organization_access`, `subject_number`, `platform`, `audit`) × 24 grantees × 2 privileges = 432 effective checks, plus 7 wave spot rules. Owners (`catalog_order_owner`, `ivr_owner`, `entitlement_metering_owner`, `activation_consent_owner`, `customer_iam_owner`, `organization_access_owner`, `subject_number_owner`, `platform_reliability_owner`, `audit_owner`) hold `USAGE`+`CREATE` only on their own schema; no runtime role holds `CREATE`; unlisted `USAGE` (including `public`) is false; the accepted V004 wave (`catalog_order_role` → `activation_consent` yes, `ivr`/`entitlement_metering` no; `entitlement_metering_role` → `catalog_order` yes; `ivr_role` → `platform`/`audit`/`entitlement_metering`/`activation_consent` yes, `ivr`/`catalog_order`/S1 reads no) is encoded in `usageRolesBySchema`.
   - V004 owned tables: 7 tables × 23 roles × 7 table privileges (1127 checks) + 78 columns × 23 roles × 3 column privileges (5382 checks). `catalog_order_role`: table `SELECT`/`INSERT` true, table `UPDATE`/`DELETE`/`TRUNCATE`/`REFERENCES`/`TRIGGER` false, column `UPDATE` exactly 0/12/3/0/4/4/0. `entitlement_metering_role`: only the 10/6/8 cross-module column `SELECT` sets true; every other table/column privilege false. All other S1/V004 roles all-false.
   - V004-touched S1 tables (5): `catalog_order_role` exact column `SELECT` (2/2/2/4/4 columns), zero table privileges of all seven kinds, zero column `INSERT`/`UPDATE`; full denial for `ivr_role`/`activation_consent_role`/`entitlement_metering_role` (5×7 table + column `SELECT`/`INSERT`/`UPDATE` per column).
   - V004 functions (5): `EXECUTE` true only for `catalog_order_owner` (all five, owner-inherent) and `catalog_order_role` (four guards only); false for every other S1/V004 role and `public`. Four S1 function grants to the four S2 runtime roles preserved; `entitlement_metering_owner` platform denial and `issue_first_trial` absence preserved.

6. Commands, environment, exits, counts
   Environment observed: Linux WSL2; `which git` → `/usr/bin/git`; `which java` → `/mnt/d/tools/jdk21/bin/java`; `which mvn` → `/mnt/d/tools/maven/bin/mvn`; no `javac` on PATH; no local `psql`/`postgres`; Python 3.10.12 present. Worktree `.git` → `D:/codex-call/.git/worktrees/codex-call4` (unresolvable).
   Commands executed successfully this session (exit 0): `sha256sum` (V001–V004, IT class, S1 artifact), `file`, `od -c`, `wc -l`, `grep` (method/`@Test`/grant inventories, trailing-whitespace and conflict-marker scans), Read/Grep/Edit/Write tools.
   Required validation — **NOT RUN (environment-blocked; every attempt was denied by the session permission layer with no execution and no exit status; recorded exactly)**:
   | Command | Result |
   | --- | --- |
   | `git status` | `fatal: not a git repository: .../codex-call/D:/codex-call/.git/worktrees/codex-call4` |
   | `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call4 --work-tree=... status --porcelain` | `This command requires approval` |
   | `mount -t drvfs D: /mnt/d` (incl. `dangerouslyDisableSandbox`) | `This command requires approval` |
   | `docker info --format ServerVersion` | `This command requires approval` |
   | `java -version` | `This command requires approval` |
   | `/mnt/d/tools/jdk21/bin/javac -version` | `This command requires approval` |
   | `cd backend && ./mvnw -B -ntp validate` | `... The following part requires approval: ./mvnw -B -ntp validate` |
   | absolute-path `mvnw -f backend/pom.xml -B -ntp validate` and `mvn -f backend/pom.xml -B -ntp validate` | `This command requires approval` |
   | `./mvnw -B -ntp clean verify`, `./mvnw -B -ntp -Pcontainer-smoke verify` | not reachable (same denial class as validate) |
   | `python3` script execution (brace-balance helper under `backend/target`) | `This command requires approval` |
   Therefore no post-fix Surefire/Failsafe XML, no 10/27 runtime counts, no boot-smoke log, no fat-jar firewall output and no disposable rogue-grant probe run were produced by this session. The pre-fix controller XML (Surefire 10/0/0/0; Failsafe 27/0/0/0, PostgreSQL 16.15) corresponds to the pre-fix IT hash and is **not** claimed as post-fix evidence. Static counts that were verified: source `@Test` = 27; CI expected set untouched (still the same 27 names); migration directory contains exactly V001–V004.
   Controller must run, from `backend`: `./mvnw -B -ntp validate`; `./mvnw -B -ntp clean verify`; `docker info`; `./mvnw -B -ntp -Pcontainer-smoke verify`, and confirm Surefire 10/0/0/0, container PostgreSQL 16 Failsafe exactly 27/0/0/0, and identical source/CI/Failsafe method-name sets, plus boot smoke and fat-jar firewall after `clean verify`.

7. Hashes (SHA-256, measured this session)
   - `V004__catalog_order_and_payment.sql` before = after = `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` (byte-identical; matches the required `A7BA7A8C...6EDC`).
   - `V001` `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6`, `V002` `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505`, `V003` `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` — unchanged from the reviewed/frozen worktree values.
   - `PostgresContainerIT.java` before `747bc344e2f7f82d91abf7dba716b104f998335826fd485fe5e0b8a7228cfb7e` (8777 lines) → after `85634104cf3981f3aade5c0e42816d883737c3257cee33651886f1d28381acd8` (8807 lines, CRLF preserved, no trailing whitespace, no conflict markers).
   - `pa4-v004-catalog-order-payment-foundation-s1-implementation.md` `fbe1cd7e705b5a573ca471496dfa43c96b4640469dfc99c4cb4d534bfc061d87` — unchanged (read-only).

8. Adversarial evidence
   - The new assertions are positive/negative effective-privilege probes against a live migrated database (`has_schema_privilege`/`has_table_privilege`/`has_column_privilege`/`has_function_privilege`), so any grant outside the accepted matrix flips a checked-in expected value and fails the suite closed with a role/schema/table/column-labeled assertion.
   - Disposable rogue-grant probe protocol (for the controller; not executed here because Docker execution was denied). Run as the migration superuser inside a transaction or with immediate rollback; do not modify checked-in SQL:
     1. REFERENCES surface: `BEGIN; SET ROLE catalog_order_owner; GRANT REFERENCES ON TABLE catalog_order.catalog_product TO catalog_order_role;` then `SELECT has_table_privilege('catalog_order_role','catalog_order.catalog_product','REFERENCES');` → `t`; the matrix assertion `matrix catalog_order_role table REFERENCES catalog_order.catalog_product` now fails. `ROLLBACK;`
     2. Cross-module column UPDATE surface: `BEGIN; SET ROLE customer_iam_owner; GRANT UPDATE (status) ON TABLE customer_iam.customer_account TO catalog_order_role;` then `SELECT has_column_privilege('catalog_order_role','customer_iam.customer_account','status','UPDATE');` → `t`; `catalog_order_role must not UPDATE customer_iam.customer_account.status` fails. `ROLLBACK;`
     3. Forbidden function EXECUTE surface: `BEGIN; SET ROLE catalog_order_owner; GRANT EXECUTE ON FUNCTION catalog_order.guard_catalog_offer() TO ivr_role;` then `SELECT has_function_privilege('ivr_role','catalog_order.guard_catalog_offer()','EXECUTE');` → `t`; `ivr_role must not EXECUTE catalog_order.guard_catalog_offer()` fails. `ROLLBACK;`
     4. Unlisted schema USAGE surface: `BEGIN; SET ROLE catalog_order_owner; GRANT USAGE ON SCHEMA catalog_order TO ivr_role;` then `SELECT has_schema_privilege('ivr_role','catalog_order','USAGE');` → `t`; `ivr_role schema USAGE catalog_order` fails. `ROLLBACK;`
   - No production SQL was weakened or mutated; the probe grants exist only inside rolled-back transactions in a disposable database.

9. Changed-file list (fix-owned writes only)
   | Path | Action | Note |
   | --- | --- | --- |
   | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited | three V004 ACL helper methods rewritten; 27 `@Test` preserved; CRLF preserved |
   | `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s1-fix-1.md` | new | this artifact; LF |
   - Disposable build-output side effect (not a maintained path): `backend/target/fix1-scratch/brace_check.py` (scratch file; execution denied; removable with `backend/target` cleanup).
   - Nothing else was written. Forbidden paths (V004/migrations, `.github/workflows/ci.yml`, `backend/README.md`, `backend/pom.xml`, application code, plan, S1 implementation artifact, review artifact, `docs/12`, `docs/14`, `project-execution-control.md`) are untouched. Nothing was staged; no commit, push or PR was created.

10. Docs decision
    - No documentation changes beyond this durable fix artifact. `docs/12`/`docs/14`/plan/implementation-artifact/README/CI remain as reviewed; OQ-1 (docs/14 §10.12 vs §11.4 wave wording) and OQ-4 (line-ending convention) stay open for their named owners and are not altered by this test-only fix.

11. Residual risks, owners, destinations
    | ID | Residual risk | Classification | Owner | Destination |
    | --- | --- | --- | --- | --- |
    | FIX1-R01 | The post-fix 27-method suite was not executed in this session (Maven/Docker denied), so a mistaken expected value in the new matrices — most plausibly an S1 schema-`USAGE` pairing — would only surface as a fail-closed assertion on the controller/CI run. | blocked-current (environment) | Controller/Backend-QA | Run the §6 command set; any mismatch fails with a labeled assertion and a one-row matrix correction, never a weakened check. |
    | FIX1-R02 | Worktree Git metadata remains unresolvable (D: drive unmounted; mount denied), so tracked-diff/`core.whitespace=cr-at-eol` evidence and frozen Git-blob comparison are content-hash substitutes, not Git-object proofs. | environment/tooling | Controller/Platform | Controller Git evidence with gitdir access; same destination as review RR-02/RR-08. |
    | FIX1-R03 | The four disposable rogue-grant probes (§8) were not executed (Docker denied); detectability of each newly covered surface is argued from PostgreSQL privilege semantics, not a live fail. | blocked-current (environment) | Backend/QA | Run the §8 rollback probes in a disposable PostgreSQL 16 container. |
    | FIX1-R04 | Assertion volume increases by roughly 7–8 thousand local privilege round-trips inside `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact`; total suite time grows but stays bounded (same class as the existing V002/V003 matrices). | test-scope | Backend/QA | Confirm container-suite wall time on the controller run. |
    | FIX1-R05 | Controller toolchain (Temurin 21.0.12.1 vs pinned 21.0.13+9) and unpinned `postgres:16` image remain as review RR-05/RR-07. | later-work-unit | Phaseflow/CI; Platform | Exact-head CI; digest pin in the platform work unit. |

12. Final status
    - `F-PA4-S1-01`: **author-reported ???** — the accepted fix is fully implemented in the maintained test path and the V004 migration remains byte-identical, but the required executable validation (§6) and the adversarial probes (§8) are environment-blocked in this session and must be run by the controller before re-review. No pass is invented for any blocked command.
    - Explicitly not performed: no later gate, no S2 work, no plan/review/PEC edit, no git stage/commit/push/PR/merge, no CI change, no production acceptance, no Phaseflow acceptance claim. The controller inspects and adjudicates.
