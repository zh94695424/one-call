# PA3 V003 S1 fix 1 evidence

1. Task label / work unit / gate / slice / date / author-provider
   - Task label: `pa3-v003-s1-fix1-mimo26flash-20261028-01`
   - Work unit: `PA3-v003-subject-number-and-claim-foundation`
   - Gate: `fix` (S1 only)
   - Slice: **S1 only** (test-evidence fix for accepted finding `F-S1-01`; no S2, no migration change)
   - Date: 2026-10-28
   - Author/provider: original author provider `mimo26flash` (bounded fix)

2. Binding inputs read and control-document confirmation
   - Read: `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-plan.md` (accepted plan).
   - Read: `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-implementation.md` (S1 evidence).
   - Read: `docs/reviews/code-review-20261028-002620.md` (S1 code review; residual risk on PUBLIC column ACL).
   - Read: current PA3 section of `docs/gateflow/project-execution-control.md` (gate record: `F-S1-01 accepted`; **never edited by this agent**).
   - Goal Confirmation remains binding: V003-only persistence foundation, exact catalog/ACL evidence, no application service, no V004+.
   - Control document: **not modified** by this agent. `git status` shows only the pre-existing controller `docs/gateflow/project-execution-control.md` entry already present at task start.

3. Accepted finding and exact code change
   - Finding `F-S1-01` (controller-accepted, low, evidence-only):
     - Live V003 ACL is correct, but `PostgresContainerIT.assertV003PublicAclIsZero` checked PUBLIC schema, table and function ACL rows and omitted explicit column ACLs stored in `pg_attribute.attacl`.
     - The 15-role column matrix excludes PUBLIC, so a future explicit PUBLIC column grant on any of the four V003 tables could pass the checked-in exact-ACL suite.
     - This conflicted with the confirmed fail-closed PUBLIC and column ACL evidence signal.
   - Exact change (only `assertV003PublicAclIsZero` extended; no other Java or SQL change):
     - Kept the three existing assertions byte-for-byte: schema `nspacl`, table `relacl`, function `proacl`, each with `aclexplode(coalesce(...))` and `grantee = 0` / `isZero()`.
     - Appended one new exact zero-count assertion:

```java
assertThat(queryLong(
        connection,
        "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "JOIN pg_attribute a ON a.attrelid = c.oid, "
                + "aclexplode(coalesce(a.attacl, acldefault('c', c.relowner))) x "
                + "WHERE n.nspname = 'subject_number' "
                + "AND c.relkind IN ('r','p') "
                + "AND c.relname IN ('business_subject','caller_number_binding',"
                + "'subject_claim','subject_risk_state') "
                + "AND a.attnum > 0 AND NOT a.attisdropped "
                + "AND x.grantee = 0"))
        .as("PUBLIC has no subject_number column privilege").isZero();
```

   - Assertion semantics:
     - Scope: schema `subject_number`, `relkind IN ('r','p')`, and exactly the four V003 tables by name (`business_subject`, `caller_number_binding`, `subject_claim`, `subject_risk_state`).
     - Column filter: non-dropped user columns only (`attnum > 0 AND NOT attisdropped`).
     - ACL API: PostgreSQL structured `aclexplode(coalesce(attacl, acldefault('c', relowner)))` — same exact pattern as the sibling schema/table/function checks; `acldefault('c', ...)` is the documented column default ACL (empty for PUBLIC), so NULL `attacl` contributes zero PUBLIC rows and any explicit PUBLIC column grant in `attacl` contributes `grantee = 0` rows.
     - Grantee: `0` (PUBLIC) only. Asserts `count(*) == 0` (fail-closed).
     - Role column grants (e.g. verification real-name columns) remain non-null `attacl` entries for named roles and do not affect the PUBLIC count.
   - Disposable live probe (fresh `postgres:16`, V001+V002+V003 applied, then removed):
     - Baseline count: `0`; non-dropped user columns examined: `36`.
     - Negative: `GRANT SELECT (id) ON subject_number.business_subject TO PUBLIC` → count `1` (would fail the assertion).
     - After `REVOKE SELECT (id) ... FROM PUBLIC` → count `0`.
     - Probe container `pa3fix-probe` removed; `docker ps -a` shows no probe container.

4. Changed paths (exactly two allowed writes), SHA-256
   | Path | Action | SHA-256 (worktree) |
   | --- | --- | --- |
   | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited (one assertion appended in `assertV003PublicAclIsZero`) | `d3848f098a5f6ea0034fc63c70d52295b6aa50a9c70839148c3f6932e9464af4` |
   | `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-fix-1.md` | new (this file) | (this file) |
   - Pre-fix IT SHA-256 (S1): `09a1ae4e6142a8c4b5bbd836467d9e0a3ca6290cd25c3bc7a5f21b2e0cad74e8`.
   - No other path written. V003, CI, README, S1 implementation artifact, review artifact, plan and control document untouched by this agent.
   - IT remains LF ASCII (`file` reports no CRLF); zero CR bytes; no trailing whitespace.

5. Method set and assertion preservation
   - `grep -c '@Test'` on `PostgresContainerIT.java` = **20** (unchanged).
   - Failsafe XML testcase names = CI `expected` set = source `@Test` names = **20/20 exact equality** in all three directions (zero difference either way).
   - No `@Disabled`, no `Assumptions.` skip, no Docker skip added.
   - Every pre-existing assertion preserved; the fix only **adds** one zero-count PUBLIC column-ACL assertion. No assertion weakened.

6. Unchanged frozen hashes
   - V001 HEAD blob SHA-256: `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` (matches frozen contract).
   - V002 HEAD blob SHA-256: `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` (matches frozen contract).
   - `git diff --ignore-cr-at-eol --exit-code HEAD -- <V001> <V002>` → exit **0**.
   - V003 worktree SHA-256: `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` (unchanged from S1 final; production migration not edited).
   - S1 implementation artifact SHA-256 remains `a1cc7a55e8854b91dee102b70077dfa71f7985c59211ea856446e4bae35e025f`.
   - S1 review artifact SHA-256 remains `53b2db46b38fd842e6c94d90c042ea3279f30cf1ddc7b76179b69779b6d666f9`.

7. Exact commands, environment, exits, counts
   Environment (same WSL JDK21/Docker environment as S1):
   - JDK: Eclipse Temurin **21.0.13+9** at `/opt/jdk21` (`JAVA_HOME` set).
   - Maven: **3.9.9** via Maven Wrapper at `/tmp/mvnw-run/mvnw` (worktree `backend/mvnw` has CRLF shebang; temp LF wrapper only — no repo write).
   - Docker: **29.1.3**; `docker info` OK before container runs.
   - PostgreSQL: Testcontainers image **postgres:16** → server **PostgreSQL 16.15**.
   - Flyway: applied **3** migrations to `v003`.
   - OS: Linux WSL2 `6.18.33.2-microsoft-standard-WSL2`.

   Commands and exits (final runs after the fix edit):
   | Command | Exit | Result |
   | --- | --- | --- |
   | Disposable `postgres:16` probe of the new SQL + negative GRANT/REVOKE | 0 | baseline `0`, columns `36`, after PUBLIC GRANT `1`, after REVOKE `0`; container removed |
   | `bash /tmp/mvnw-run/mvnw -B -ntp clean verify` | 0 | surefire **10**, `failures=0 errors=0 skipped=0`; `BUILD SUCCESS` |
   | `docker info` + `bash /tmp/mvnw-run/mvnw -B -ntp -Pcontainer-smoke verify` | 0 | surefire **10** and failsafe **20**, `failures=0 errors=0 skipped=0`; PostgreSQL `16.15`; Flyway 3 migrations to `v003`; `BUILD SUCCESS` |

   Counts confirmed from Surefire/Failsafe XML:
   - Default surefire total: `tests=10 failures=0 errors=0 skipped=0` (8 ModuleBoundary + 1 BootstrapDependencyBoundary + 1 OneCallApplicationTests).
   - Container surefire total: `tests=10 failures=0 errors=0 skipped=0`.
   - Container failsafe `PostgresContainerIT`: `tests=20 failures=0 errors=0 skipped=0`.

8. Whitespace / scope / method-set checks
   - Scoped `git -c core.whitespace=cr-at-eol -c core.autocrlf=false -c core.safecrlf=false diff --check -- backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` → exit **0**, no diagnostics.
   - `grep -n -E '[[:blank:]]+$'` on the IT → no output.
   - IT contains zero CR bytes; `file` reports ASCII text (LF).
   - `grep -c '@Test'` = 20; CI expected set equality = 20/20; failsafe XML = same 20 names.
   - No `@Disabled` / `Assumptions.` in the IT.
   - `git status --porcelain=v1` at end of fix: only the pre-existing S1/controller entries (`ci.yml`, `README.md`, `PostgresContainerIT.java`, control doc, V003, S1 evidence, review artifact) plus this new fix artifact. No stage, commit, push, PR or branch/config mutation.

9. Residual risks (classified)
   | ID | Residual | Class | Owner / destination |
   | --- | --- | --- | --- |
   | S1F-R01 | S2 five behavior/concurrency methods not yet implemented (suite remains 20, not final 25) | later-approved-slice | S2 implementation agent / controller |
   | S1F-R02 | Exact PG16 deparse literals frozen from 16.15; unpinned `postgres:16` digest | fixed-current (fail-closed in tests) / later-work-unit | Backend/QA; Platform (Q17/Q23) |
   | S1F-R03 | GitHub Actions has not run this head | later-work-unit | Phaseflow/CI exact-head PR gate |
   | S1F-R04 | Q2/Q17/Q23/Q24, enterprise ownership transfer, service claim protocols deferred | later-work-unit | Product/Security/Platform per plan R-01..R-08 |
   | S1F-R05 | `RETIRED` not DB-terminal; `OPEN` not a dialing ban (by design) | fixed-current (explicit non-claim) | subject-number service / Q24 |
   | S1F-R06 | F-S1-01 itself is now fixed-current: PUBLIC column ACLs on the four V003 tables are explicitly asserted zero via `pg_attribute.attacl` | fixed-current | — |

10. Stop-condition check
    - No stop condition triggered: no migration/production change; no method-count change; no CI/README update; no S2 behavior; no out-of-scope file write; no assertion weakening; no skipped/disabled test added.

11. Completion statement
    - **S1 fix 1 complete** for accepted finding `F-S1-01` only.
    - Explicitly **not** performed: self-review gate, stage, commit, push, PR, merge, branch/config mutation, `git config`, control-document edit, later gate entry, S2 implementation, GitHub Actions run, production provisioning or production acceptance.

12. No-goal-drift statement and exact changed-file list
    - No goal drift: only the two allowed paths were written; the change is a test-evidence assertion over `pg_attribute.attacl` for PUBLIC on the four existing V003 tables; production V003 SQL, CI, README, plan, review, S1 implementation artifact and control document are unchanged; no new domain object, grant, role, service or V004+ object; Goal Confirmation scope (V003 persistence foundation with fail-closed PUBLIC/column ACL evidence) is reinforced, not altered.
    - Exact changed-file list:
      1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (edited)
      2. `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-fix-1.md` (new; this report)
