# PA4 V004 aggregate fix-1 — F-PA4-AGG-01 complete S1-table/S2-role denial matrix and owner S1-function EXECUTE evidence

1. Task label / work unit / gate / date / author-provider
   - Task label: `pa4-v004-aggregate-fix1-mimo26flash-20261028-01`
   - Work unit: `PA4-v004-catalog-order-payment-foundation`
   - Gate: `fix` (aggregate finding `F-PA4-AGG-01` only). Accepted aggregate-review base: `95669d48fafed343af21cc988d46f8db856c3246` (accepted S2 slice commit); immutable base `81022037dc5dced625788477835472a84a16d52d`.
   - Branch: `codex/pa4-v004-catalog-order-payment-foundation` (verified with `git branch --show-current`; HEAD verified with `git rev-parse HEAD` = `95669d48fafed343af21cc988d46f8db856c3246`).
   - Date: 2026-10-28
   - Author/provider: MiMo 2.6 Flash (bounded aggregate fix author; no author self-review; no other gate)
   - Scope: fix ONLY the controller-accepted `F-PA4-AGG-01` from `docs/reviews/code-review-20261028-114712.md`, test evidence only. No re-review, no gate transition, no commit/stage/push/PR/merge, no edit of control, plan, prior artifacts, README, CI, POM, migrations or any other tracked file.

2. Accepted finding being fixed
   - `F-PA4-AGG-01` (low, test-only fail-closed evidence gap): the V004 exact-ACL proof in `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact` (test entry `PostgresContainerIT.java:1628`) → `assertV004OwnedAndCrossModuleGrants` (`:6897`) and `assertV004GuardFunctionExecuteBoundaries` (`:7129`) did not assert plan-mandated denials for (a) the eight S1 tables outside the V004-touched set across the eight S2 roles, (b) `catalog_order_role` denials on S1 tables outside the touched set, and (c) the four S2 owner roles' `EXECUTE=false` on the four S1 functions. Pre-fix, the table/column denial loops iterated only the local `s1AllColumns` map (five V004-touched S1 tables) for three S2 runtime roles plus the RR-09 four-owner loop, and the S1-function section asserted only positive `EXECUTE` for the four S2 runtime roles. A future out-of-matrix grant of those shapes would leave the 31/0/0/0 suite green. The reviewed V004 migration has no live over-grant, so this is an evidence gap only; the migration was not and must not be edited.

3. Binding inputs read (read-only)
   - `docs/gateflow/project-execution-control.md` — current gate `fix` (`F-PA4-AGG-01`); aggregate deepreview row; PA4 goal contract/slice principle.
   - `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` §6.6.4 (function `EXECUTE` table; "No `catalog_order_owner`, `ivr_owner`, `entitlement_metering_owner` or `activation_consent_owner` receives a runtime `EXECUTE` grant"), §6.6.5 (denials to assert, including "no S1 table privilege" for `ivr_role`/`activation_consent_role`/`entitlement_metering_role` and "No other S2 role receives a table or column privilege on a V004 or S1 table"), §8.3 (exact ACL assertions, including `entitlement_metering_role` cannot read S1 tables).
   - `docs/reviews/code-review-20261028-114712.md` — full finding text, fix sketch (13 tables = `S1_TABLES + V002_TABLES + V003_TABLES`, eight S2 roles, seven table privileges, column `SELECT`/`INSERT`/`UPDATE` with the five-table `catalog_order_role` `SELECT` exception, four owner × four S1-function `EXECUTE=false`), and residual-risk table RR-01..RR-11.
   - Binding design sources as directed: `docs/12-technology-stack-and-system-architecture.md` §§5/6/14/15 and `docs/14-production-data-model-and-flyway-schema-spec.md` V004 sections (2.9, 4.16-4.22, 5.3, 9.0.1, 9.17, 9.18, 9.22, 9.26, 10.12, 10.16, 11.4).
   - `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (pre-fix SHA-256 `3579b376d90f1c10804e47a673606813b9e875c4b845ab247cb18ccdfc5f084b`, 10,797 lines, CRLF): constants `PLATFORM_FUNCTIONS`, `AUDIT_FUNCTION`, `S1_TABLES` (3), `V002_TABLES` (6), `V003_TABLES` (4), `ALL_V001_V003_TABLES` (13), `V004_ROLES` (exactly the eight S2 roles); helpers `hasTable`/`hasColumn`/`hasFunction`/`hasSchema`/`queryStrings` (`:8861-8920` region).
   - `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` — read-only grant truth (only the five declared S1 column reads; no grant on the eight other S1 tables; no owner `EXECUTE` on S1 functions); never edited.

4. Exact code changes (test-only; one maintained Java file)
   - File: `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` — CRLF preserved (`file(1)` still reports CRLF after the fix); 10,797 → 10,780 lines; scoped diff `+61 / −78`, hunks confined to the two allowed helpers.
   - `assertV004OwnedAndCrossModuleGrants`:
     - Header comment records the `F-PA4-AGG-01` addition (`:6898-6907`).
     - Removed the now-superseded local `s1AllColumns` map (its five-table expectations are fully subsumed by live column enumeration plus the `catalogCrossSelect` exception; exact S1 column inventories remain asserted by `assertTablesColumnsChecksAndIndexes`).
     - Replaced the three overlapping loops (the `catalogCrossSelect` exact-read loop, the three-runtime-role denial loop, and the RR-09 four-owner denial loop) with ONE exact bounded loop (`:7028-7074`):
       - `s1UniverseTables` = `S1_TABLES + V002_TABLES + V003_TABLES`, asserted `containsExactlyInAnyOrderElementsOf(ALL_V001_V003_TABLES)` and `hasSize(13)` (exact expected table set; equality with the existing constant checked at runtime, not assumed);
       - per table, actual non-dropped columns are enumerated live from `pg_class`/`pg_namespace`/`pg_attribute` and asserted non-empty — a missing table or column fails closed instead of being silently skipped (no `getOrDefault`-style column skip; `catalogCrossSelect.getOrDefault` is only the expected-grant lookup, where an absent key means "no expected grant");
       - crossed with the eight S2 roles (`V004_ROLES`): all seven table privileges (`SELECT`/`INSERT`/`UPDATE`/`DELETE`/`TRUNCATE`/`REFERENCES`/`TRIGGER`) asserted false for every one of the 13×8 pairs; column `SELECT`/`INSERT`/`UPDATE` asserted with expected value `true` only for `catalog_order_role` column `SELECT` on columns listed in `catalogCrossSelect` (the five V004-touched S1 tables), and `false` for every other pair — including `catalog_order_role` on the eight untouched S1 tables and all eight roles' column `INSERT`/`UPDATE` everywhere.
   - `assertV004GuardFunctionExecuteBoundaries`:
     - Added after the unchanged positive four-runtime-role loop (`:7186-7199`): the four S2 owner roles (`catalog_order_owner`, `ivr_owner`, `entitlement_metering_owner`, `activation_consent_owner`) asserted `EXECUTE=false` on all four S1 functions — `PLATFORM_FUNCTIONS` (3) plus `AUDIT_FUNCTION`.
   - Unchanged: all 31 `@Test` methods (count and names), the test entry `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact` (`:1628`, still calls the same four helpers), all positive S2 runtime grants on the four S1 functions, the V004 own-function/guard/`reject_row_mutation` matrix, `assertV004SchemaUsageMatrix`, `assertV004PublicAclIsZero`, the `entitlement_metering_owner` platform-capability block and `issue_first_trial` absence, every V001-V003 helper (including `assertGrants`), CI workflow, README, POM, all migrations and all application code. No method additions, no renames, no new abstraction.

5. Fix mapping (finding → evidence)
   | Accepted defect element | Post-fix evidence |
   | --- | --- |
   | Eight S1 tables outside the touched set (`platform.idempotency_record`, `platform.outbox_event`, `audit.audit_fact`, `customer_iam.auth_identity`, `operations_iam.employee_account`, `subject_number.caller_number_binding`, `subject_number.subject_claim`, `subject_number.subject_risk_state`) × eight S2 roles × seven table privileges | Unified loop `:7028-7074`: 13×8 table-privilege denials (all false), table set equality-asserted against `ALL_V001_V003_TABLES` |
   | `catalog_order_role` denials outside the touched set | Same loop: `expectGrant` is false unless `role == catalog_order_role && privilege == SELECT && catalogCrossSelect` lists the column, and `catalogCrossSelect` contains only the five touched tables |
   | Column `SELECT`/`INSERT`/`UPDATE` for all 13×8, with the existing exact `catalog_order_role` column `SELECT` on the five touched tables as the only exception | Same loop: actual enumerated columns per table; `expectGrant` encodes exactly the `catalogCrossSelect` exception (granted columns true, all other columns false); `INSERT`/`UPDATE` expected false everywhere |
   | No silent skipping of missing tables/columns | Live column enumeration per table with `isNotEmpty()` fail-closed assertion; union size 13 and set equality asserted |
   | Four S2 OWNER roles `EXECUTE=false` on all four S1 functions (`platform.idempotency_begin(text,text,text)`, `platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)`, `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)`, `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)`) | New loop `:7186-7199`; signatures confirmed: `PLATFORM_FUNCTIONS` matches the first three verbatim; `AUDIT_FUNCTION` uses the textual form `timestamp with time zone` ≡ `timestamptz` for the fourth and is already proven by the passing positive runtime loop |
   | Keep unrelated S1 helpers unchanged; no method additions; 31 names exact; CI expected set unchanged | Diff confined to the two helpers; source `@Test` = 31 = CI `expected` = Failsafe `testcase` names (sort-diff empty); `git diff --name-only HEAD` shows only this test file (plus the controller's own pre-existing control-doc edit) |
   | Preserve positive S2 runtime grants, V004 own-function matrix and schema checks | Those sections are byte-untouched in the diff; full container run green |
   - Final status: author-reported **已修复**.

6. Commands, environment, exits, counts (executed in this session; no permission denial occurred)
   - Environment observed: Linux WSL2; `git` functional in this session (worktree `.git` resolves); Windows `cmd.exe mvnw.cmd` invoked with cwd `backend` because the WSL `mvnw` has a CRLF shebang; Java 21.0.12.1 (Temurin; Enforcer `RequireJavaVersion`/`RequireMavenVersion` passed), Maven 3.9.9, Docker Desktop 29.7.2 (npipe), PostgreSQL 16.15, Testcontainers 2.0.5 / Ryuk 0.14.0.
   - Executed checks:

   | Check | Command / method | Result |
   | --- | --- | --- |
   | Branch / HEAD | `git branch --show-current`; `git rev-parse HEAD` | `codex/pa4-v004-catalog-order-payment-foundation`; `95669d48fafed343af21cc988d46f8db856c3246` |
   | Status | `git status --short` | modified: `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (this fix) and `docs/gateflow/project-execution-control.md` (controller's pre-existing gate-record edit, not touched by this task); untracked: `docs/reviews/code-review-20261028-114712.md` and controller launcher/prompt files (pre-existing bookkeeping). No other implementation path changed |
   | Exact diff | `git diff --numstat` / full `git diff` on the test file | `61 78`; every hunk inside `assertV004OwnedAndCrossModuleGrants` / `assertV004GuardFunctionExecuteBoundaries` only |
   | Maven validate | `cmd.exe /c "mvnw.cmd -B -ntp validate"` (cwd `backend`) | `BUILD SUCCESS` |
   | Default verification | `cmd.exe /c "mvnw.cmd -B -ntp clean verify"` | `BUILD SUCCESS`; Surefire `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` |
   | Container verification | `cmd.exe /c "mvnw.cmd -B -ntp -Pcontainer-smoke verify"` | `BUILD SUCCESS`; Docker 29.7.2; `PostgreSQL 16.15`; Flyway `Successfully applied 4 migrations to schema "public", now at version v004`; Failsafe `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0` (23.33 s); regenerated XML `tests="31" errors="0" skipped="0" failures="0"` |
   | Method-name equality | source `@Test` names vs `.github/workflows/ci.yml` `expected` set vs Failsafe XML `testcase` names (sorted `diff`) | exact 31/31/31, empty diffs; zero `@Disabled`/`Assumptions` occurrences |
   | Frozen Git blobs | `git show HEAD:<migration> \| sha256sum` | V001 `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`; V002 `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`; V003 `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` — all match the frozen contract |
   | V004 hashes | `sha256sum` worktree = `git show HEAD:` blob = `backend/target/classes` packaged | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` everywhere (matches required `A7BA7A8C…6EDC`); byte-identical before/after this fix |
   | CR-aware scoped whitespace | `git diff --check -- <test file>`; `file(1)`; hunk-scoped diff | exit 0, no whitespace errors; file remains `ASCII text … with CRLF line terminators`; no EOL churn (diff is hunk-scoped, not whole-file) |
   - Not run, and not required by this gate: the concurrency-only profile repetitions, boot smoke and fat-jar firewall (unchanged code paths, inherited green from the accepted S2/aggregate evidence), and exact-head GitHub Actions (later PR gate, RR-01). No validation was claimed on inherited XML — the Failsafe XML above was regenerated by this session's post-fix container run.

7. Hashes (SHA-256, measured this session)
   - `V004__catalog_order_and_payment.sql` before = after = `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` (byte-identical; matches required `A7BA7A8CBAF5EECAF04F117B53C1B4BFD31C31B04E9432984D6C108B6E496EDC`).
   - V001/V002/V003 frozen Git blobs unchanged (values above; worktree CRLF byte hashes are not the frozen contract — see aggregate RR-08).
   - `PostgresContainerIT.java`: before `3579b376d90f1c10804e47a673606813b9e875c4b845ab247cb18ccdfc5f084b` (10,797 lines) → after `12c79f5f42fdb19c4f8ac09e2ecf01a99c073b76e6e7631d0be37d9d828480fc` (10,780 lines, CRLF preserved, no trailing-whitespace errors, no conflict markers).

8. Docs decision
   - Changed docs: exactly one NEW artifact — this file (`docs/gateflow/pa4-v004-catalog-order-payment-foundation-aggregate-fix-1.md`, LF).
   - Not edited: `docs/gateflow/project-execution-control.md` (its unstaged modification predates this task and is controller-owned gate bookkeeping), the accepted plan, all prior gateflow/review artifacts, `backend/README.md`, `.github/workflows/ci.yml`, `pom.xml`, all migrations and every other tracked file. Controller prompt/launcher files are not implementation and were not touched.

9. Residual risks (inherited from the aggregate review; this fix adds none)
   | ID | Risk | Classification | Owner | Destination |
   | --- | --- | --- | --- | --- |
   | RR-01 | Exact-head GitHub Actions has not run; local Maven/Docker runs are not CI evidence. | later-work-unit | Phaseflow/CI | Exact-head PR CI (`java-runtime` + `contract-regressions`) |
   | RR-02 | Temurin 21.0.12.1 used locally vs project/CI pin 21.0.13+9 (Enforcer `[21,22)` accepted). | later-work-unit | Phaseflow/CI | Exact-head CI on the pinned toolchain |
   | RR-03 | `postgres:16` floating fixture (resolved 16.15); Q17/Q23 provisioning and digest pinning not proven. | later-work-unit | Platform/Database/Security | Q17/Q23; production provisioning and digest pin |
   | RR-04 | Seven-table `TRUNCATE` cleanup safe only while the class runs serially. | test-only/fail-closed | Backend/QA | Keep serial or scope cleanup by UUID if parallel execution is introduced |
   | RR-05 | Failure-path cleanup after timeout/aborted race not injected. | test-only/robustness | Backend/QA | Failure-injection evidence if required |
   | RR-06 | Three concurrency-only repetitions remain controller evidence; this session ran the full 31-method suite once. | evidence | Controller/QA | Preserve per-repetition XML or accept the controller log; exact-head CI repeats the full suite |
   | RR-07 | README lines 58/68/102 and the class Javadoc carry stale “S1+S2+S3”/PA1–PA3 labels. | documentation-only | Backend/QA | Later documentation pass; no code change required |
   | RR-08 | Prior S2 review recorded worktree CRLF hashes as the frozen V001–V003 values. | existing-issue (prior evidence) | Controller/QA | Correct the prior evidence record; do not normalize line endings repository-wide |
   | RR-09 | `V004_CHECK_DEF_MAP`/`V004_INDEX_DEFS` are PG16.15 deparse literals. | test-only/fail-closed | Backend/QA | Keep fixture pinned to 16.15; any real difference stops and escalates |
   | RR-10 | `F-PA4-AGG-01` test-only ACL evidence gap. | **addressed by this fix** (was: later-approved-slice if deferred) | Backend/QA (fix) → fresh DeepSeek re-review | Closed pending independent re-review; exact-head CI still required |
   | RR-11 | Cumulative 10.8k-line suite and later production provisioning dependence. | existing-issue / later-work-unit | Backend/QA / Platform | Ongoing test maintenance; Q17/Q23 |
   - No unclassified or blocking residual was introduced by this fix.

10. Completion
    - `F-PA4-AGG-01`: author-reported **已修复** — the denial matrix now covers all 13 S1 tables × eight S2 roles (seven table privileges, column `SELECT`/`INSERT`/`UPDATE` with the exact five-table `catalog_order_role` `SELECT` exception) and the four S2 owner roles × four S1 functions `EXECUTE=false`, with fail-closed live column enumeration and exact table-set equality; positive grants, the 31-method set, V001–V003 frozen blobs and the byte-identical V004 are preserved.
    - Exactly one source file changed (`backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`) plus this one new artifact; every validation in §6 was executed green in this session.
    - Ready for a fresh DeepSeek re-review of `F-PA4-AGG-01` closure. No commit, stage, push, PR, merge, comment, gate transition or self-review was performed.
