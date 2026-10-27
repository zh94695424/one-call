# PA2 V002 Identity, Organization and Employee Foundation — Bounded Plan-Fix 1

- Work unit: `PA2-v002-identity-organization-employee-foundation`
- Gate: `plan fix` (bounded plan-only correction; no implementation, no later gate)
- Task label: `pa2-v002-identity-organization-employee-foundation-plan-fix-1`
- Agent: controller-dispatched plan fix Agent
- Date: 2026-10-27
- Base: `main` at `d93c380b9f0025675931f950d95ca4cfef45647b` (merged PR #9)
- Binding goal: the confirmed PA2 V002-only goal contract in `docs/gateflow/project-execution-control.md` remains binding and unchanged.
- Review artifact: `docs/reviews/plan-review-20261027-150103.md`, SHA-256 `A21A63660BE97C6CF37CEC222A77E4F927D58A58221B1F3E725F25A36A10FF0D` (matches the controller record).
- Reviewed target before fix: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md`, 822 lines / 76279 bytes, SHA-256 `B8FF9FA8F17855D2785AF52ACDC96148ED5F37AAD5EB16A415A78A15FB02A64E` (matches the controller plan-gate record).
- Revised target after fix: same plan path, 846 lines / 83700 bytes, SHA-256 `FF2E9597DC0D9C2AC1772DEAD16726251765CC53FB2E8B5058F6EC5529EA4C0A`.
- Result: revised plan plus this new fix artifact. No other repository path was written by this task. No commit, push, PR, merge, re-review or implementation occurred.

## 1. Gate, scope and binding inputs

- The controller accepted exactly one finding, F-PA2-PR-01, and selected review option (a), not option (b): S1 exact inventory must compare every one of the 34 V002 named check predicates using PostgreSQL 16 canonical/normalized `pg_get_constraintdef` evidence against an explicit expected mapping derived from `docs/14`. S2 keeps the already planned valid-shape and invalid-shape behavioral probes; those do not replace the S1 predicate inventory.
- Binding inputs read for this fix: the active plan; the plan review; the PA2 binding goal contract, slice principle and gate record in `docs/gateflow/project-execution-control.md`; `docs/14-production-data-model-and-flyway-schema-spec.md` 4.6-4.11 (the frozen predicate text); the existing `PostgresContainerIT` catalog/indexdef assertion patterns; and `backend/src/main/resources/db/migration/V001__...sql` (read-only).
- This task is plan-only. It did not implement migration/code/tests/CI/README, and did not modify the control document, the review artifact, `docs/12`, `docs/14`, V001 or any source.

## 2. Accepted finding and selected disposition

| Item | Severity | Controller decision | Disposition |
| --- | --- | --- | --- |
| F-PA2-PR-01 — check names plus a single expected `23514` probe can pass when a same-named check has a subtly wrong predicate | Low (accepted) | Exact normalized `pg_get_constraintdef` evidence for all 34 checks in S1 (option a); retain S2 behavior probes | Fixed in the plan; status `已修复` |

- The correction does not redesign the schema, change any of the 34 source predicates, add a slice, add a migration object, add a test method, add a success signal, or reopen any deferred Q2/Q17/Q23 decision.
- `docs/14` remains the authoritative predicate source. The plan now defines how PostgreSQL 16 canonical deparse evidence is bound to that source; it does not hand-copy an alternative SQL contract.

## 3. Exact plan sections changed

| Plan location | Change |
| --- | --- |
| Header / status | Added the Plan-fix 1 line naming F-PA2-PR-01, the all-34 normalized `pg_get_constraintdef` requirement, this artifact and the plan-only status. |
| §1.3 success signals | Rewrote the "exactly the six frozen tables" row to require the exact 34 check names plus exact normalized `pg_get_constraintdef` equality against an explicit `docs/14`-derived mapping, with whole-definition equality and no substring/contains; updated the fail-closed-evidence row so S2 probes complement and do not replace the S1 inventory. |
| §2 goal alignment | Extended the named-check row with the all-34 normalized predicate inventory. |
| §3 direct evidence | Extended the `docs/14` 4.6-4.11 row and the `PostgresContainerIT` row (with exact line references for the current check-name query and `indexdef` patterns) to name the 34-entry deparse map. |
| §5 affected files | Extended the `PostgresContainerIT` S1 change description to include the exact 34-check normalized predicate map. |
| §6.2 intro | Added that `docs/14` remains authoritative and S1 proves all 34 predicates by normalized `pg_get_constraintdef` equality via §8.2.1; no predicate text changed. |
| §6.6 DB-enforced vs protocol | Changed the checks/time-order evidence cell to "exact normalized `pg_get_constraintdef` predicate inventory (S1) plus negative row probes (S2)". |
| §7.1 S1 allowed changes | Added an explicit bullet naming `v001AndV002InventoryMatchesTheFrozenContract` (via a private helper) as the asserting method for the 34-entry map, with no substring/contains and no substitute by S2 probes. |
| §7.1 invariants | Added the 34-check exact-normalized predicate invariant, failing on name-only, missing/extra or substring/contains matches. |
| §7.1 expected assertions | Added the all-34 normalized equality with zero extra/missing checks. |
| §7.2 S2 expected assertions | Recorded that the `mutableHeadsEnforceLifecycleRevisionAndNamedChecks` valid/invalid-shape probes are behavior evidence and do not replace the S1 inventory. |
| §8.2 exact catalog assertions | Extended the Checks row with the all-34 normalized `pg_get_constraintdef` equality. |
| §8.2.1 (new) | Added the code-generation-ready check-predicate inventory method: location, explicit 34-entry expected map, actual `pg_constraint`/`pg_get_constraintdef` query, narrow normalization, fail-closed condition and non-substitution rule. |
| §8.3 behavior assertions | Added the complementary-evidence note and the positive/valid-shape clause; S2/S3 behavior probes are retained. |
| §10 R-12 | Rewrote the residual to require the all-34 normalized predicate mapping, container-evidenced whitespace normalization only, and no substring/contains. |
| §12 completion report item 5 | Added the S1 34-entry mapping evidence (asserting method, name/set result, per-entry `docs/14` source and raw deparse, normalization samples). |
| §13.1 (new) | Recorded the plan-fix validation and `已修复` status, three-slice/12-16-18 preservation and scope guard. |

No other plan section was changed. The six table definitions, the 34 predicate texts, the four FKs, the six guards, the grants, the three slices and the 12/16/18 method evolution are unchanged.

## 4. Code-generation-ready predicate inventory contract (summary of §8.2.1)

- **Asserting method:** the renamed S1 method `v001AndV002InventoryMatchesTheFrozenContract`, via a private inventory helper. No new test method; the S1 container count stays 12.
- **Expected mapping:** a static, explicit map `schema.table -> {check name -> expected normalized definition}` with exactly 34 entries (4/7/7/4/4/8). Names and predicates derive from `docs/14` 4.6-4.11. Each expected definition is the PostgreSQL 16 canonical `pg_get_constraintdef(oid)` string captured once from the real `postgres:16` container during S1 after V002 applies, then frozen as a test literal.
- **Actual evidence:** `pg_constraint` joined to `pg_class`/`pg_namespace` with `contype = 'c'`, selecting `conname` and `pg_get_constraintdef(oid)` for each of the six tables; compare the name set and every definition.
- **Narrow normalization:** one fixed `normalizeCheckDef` helper that may only trim leading/trailing ASCII whitespace and collapse each run of ASCII whitespace (`[ \t\r\n]+`) to one space, and only when real-container evidence shows a whitespace-only difference. It must not change any other character/token/keyword/identifier/literal/cast/operator/parenthesis, case-fold, reorder, add or remove tokens, or use substring/`contains`/prefix/regex/semantic-equivalence matching. A non-formatting difference is a stop-and-escalate condition.
- **Fail-closed condition:** the method fails the build (uncaught `AssertionError`, no skip/warn fallback) if any of the 34 names is missing or misspelled, any extra `contype = 'c'` row exists on a V002 table, or any normalized definition differs. Constraint names, exact normalized definitions and the no-extra-check count must all agree.
- **Non-substitution:** the S2 positive/negative shape probes remain required and cannot waive the S1 inventory.

## 5. Validation performed

| Check | Method | Outcome |
| --- | --- | --- |
| Before plan hash | `sha256sum` on the plan | `B8FF9FA8F17855D2785AF52ACDC96148ED5F37AAD5EB16A415A78A15FB02A64E`, matching the controller plan record |
| After plan hash | `sha256sum` on the plan | `FF2E9597DC0D9C2AC1772DEAD16726251765CC53FB2E8B5058F6EC5529EA4C0A` |
| Size evolution | `wc -l -c` | 822 lines / 76279 bytes before; 846 lines / 83700 bytes after |
| Encoding / line endings | `file`; `grep -c $'\r'` | UTF-8 text; 0 CR bytes (LF), final newline present |
| Trailing whitespace | `grep -cP '[ \t]+$'` | 0 matches |
| Conflict markers | `grep -nE '^(<<<<<<<|=======|>>>>>>>)'` | No matches |
| Check-name inventory | Extract `- \`ck_...\`` bullets, `sort -u` | 34 unique names: 4/7/7/4/4/8 per table |
| Three slices preserved | Re-read §4/§7/§8.1; grep the S1/S2/S3 rows | Exactly three behavior slices |
| Method evolution preserved | grep §8.1 | S1=12, S2=16, S3=18 unchanged |
| Scope guard | Re-read §1.5/§11; grep for V003/service/POM/provisioning | No V003+ object, application service, extra migration object, POM/runtime or production-provisioning scope added |
| CR-aware whitespace, plan | `git -c core.whitespace=cr-at-eol diff --no-index --check /dev/null <plan>` run from `/tmp` | exit `1` with zero diagnostics (content-only difference; no whitespace errors) |
| Changed-file ownership | Bounded action set plus mtimes | Only the two writable paths were written by this task; controller-owned inputs were not modified (see §6) |

**Windows/CRLF and git limitation (reported accurately).** This WSL shell cannot resolve the worktree object store: the workspace `.git` file contains `gitdir: D:/codex-call/.git/worktrees/codex-call2`, a Windows path that does not exist from Linux. Consequently `git status`, `git show HEAD:<V001>`, and `git diff --check` inside the worktree fail with exit `128` (`fatal: not a git repository`). The scoped `--no-index` check above therefore had to run from `/tmp`, where it is repo-independent. For the same reason the V001 HEAD-blob hash and a repository-wide changed-file enumeration could not be recomputed by git here; the frozen V001 HEAD hash and the control/review artifacts remain controller-recorded inputs. The on-disk V001 is the expected CRLF baseline (536 CR bytes, raw hash `96E72BDACB32610DD03975E3E4FD6B828579B574A375E4241482F19C96B4E1C6`, matching the raw hash documented in the plan review); this task did not write V001.

Mtimes support ownership: the control document (`15:04:30`), the plan review (`15:02:54`), and `docs/14` and V001 (`14:13:42`) all predate this task's writes; only the plan and this artifact were written. No Maven, Flyway, container or test execution was run at this plan-fix gate; all implementation behavior remains planned evidence.

## 6. Final finding status

- **F-PA2-PR-01: `已修复`.**
- Rationale: the plan now consistently requires S1 to assert all 34 named check predicates by exact PostgreSQL 16-normalized `pg_get_constraintdef` equality against an explicit `docs/14`-derived map, with exact names, exact normalized definitions and a no-extra-check count that must all agree and fail closed. The S2 valid/invalid-shape probes are explicitly retained and declared non-substitutable. No predicate, schema, slice, count or scope changed.

## 7. Residual risks

| ID | Residual | Classification | Owner | Destination |
| --- | --- | --- | --- | --- |
| RF-01 | The expected deparse literal is captured from the applied S1 migration, so a wrong migration captured as "expected" could self-agree. | Test-authoring risk | Backend/QA | §8.2.1 requires the S1 evidence to record each entry's `docs/14` source and the S1 review to independently confirm derivation from `docs/14`, not from the implementation's DDL. |
| RF-02 | PostgreSQL 16 deparse formatting may still vary across runs/images. | Test-only fail-closed risk (plan R-12) | Backend/QA | Only container-evidenced whitespace normalization is allowed; any non-formatting difference is stop-and-escalate; never weaken to substring/contains. |
| RF-03 | `postgres:16` remains a floating tag, so a future minor could alter deparse. | Inherited residual (plan R-11) | Platform/CI | Supply-chain hardening; no digest guess. |
| RF-04 | Git/CRLF object-store limitation prevents local git-based hash/diff proof in this shell. | Environment/tooling (plan R-02) | Controller/Platform | Use controller-recorded HEAD hashes and repo-independent scoped checks; do not normalize line endings. |
| RF-05 | Deferred inputs Q2/Q17/Q23 remain unresolved. | Deferred input | Security/Platform/Database | Unchanged; not guessed by this fix. |

## 8. No-goal-drift statement

- The fix stays inside the confirmed V002-only goal. It adds no V003-V009 migration or object, no subject/catalog/IVR/order/evidence/sharing behavior, no API/login/session/credential/OTP/frontend, no runtime DataSource/Flyway wiring, no POM change, no application service, no database service function, no new role/schema, no production provisioning and no Q2/Q17/Q23 guess.
- It preserves the six tables, 34 predicates, four FKs, six guards, exact grants, three behavioral slices, 12/16/18 method evolution, V001 frozen hash and all non-goals.
- `docs/14` remains the authoritative predicate source; the plan adds only the validation method that binds PostgreSQL 16 canonical deparse evidence to it.

## 9. Changed files, hashes and stop statement

- Changed files by this task:
  - `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md` (revised), SHA-256 `FF2E9597DC0D9C2AC1772DEAD16726251765CC53FB2E8B5058F6EC5529EA4C0A`.
  - `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan-fix-1.md` (new), SHA-256 recorded after close-out below.
- Not changed by this task: `docs/gateflow/project-execution-control.md`, `docs/reviews/plan-review-20261027-150103.md`, `docs/12`, `docs/14`, V001, `backend/pom.xml`, application source, tests and CI. These remain controller-owned or binding inputs.
- **Stop statement:** the two bounded writes are complete. This task stops after validation and does not perform re-review or any later gate, commit, push, PR, merge, implementation, CI or README change.

## 10. Artifact hash close-out

- The SHA-256 of this artifact is computed from the final written content and reported in the plan-fix task report. It is intentionally not embedded here, because embedding it would change the hash it describes.
