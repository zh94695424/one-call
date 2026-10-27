# PA3 V003 S2 implementation evidence

1. Task label / work unit / gate / slice / date / author-provider
   - Task label: `pa3-v003-s2-evidence-finalize-ds-20261028-05` (this evidence-finalization revision supersedes the correction label `pa3-v003-s2-implementation-correction-ds-20261028-04`, which superseded the original S2 author label `pa3-v003-s2-implementation-ds-20261028-03`)
   - Work unit: `PA3-v003-subject-number-and-claim-foundation`
   - Gate: `implementation`
   - Slice: **S2 only** (five executable data/lifecycle/concurrency methods; no V003, POM, application-Java or design-doc change)
   - Date: 2026-10-28
   - Author/provider: fallback code author (`deepseek`); this revision only records the controller's completed validation of the corrected tree. It does not change code, CI, README, migration, plan/design/control docs or any other file, and does not commit, stage, push, open a PR or enter the next gate.

2. Binding inputs read and control-document confirmation
   - Read the accepted plan `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-plan.md`, especially §§7.2, 8.1, 8.2, 8.3 and 8.4.
   - Read the accepted S1 evidence `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-implementation.md` (V003 SHA-256, 20-method checkpoint, environment notes).
   - Read the accepted V003 migration in full, the current `PostgresContainerIT.java` structure/helpers/methods, `.github/workflows/ci.yml`, `backend/README.md`, `backend/.java-version` and `backend/.mvn/wrapper/maven-wrapper.properties`.
   - Control document `docs/gateflow/project-execution-control.md` was **not read beyond repository context and was not modified** by this agent.
   - Binding design inputs (`docs/12`, `docs/14`) were not modified; only the plan's S2 allowed paths were written.

3. Base / hashes / V003 before-after
   - S1 accepted checkpoint HEAD is `9e4d25d3e9a732015d95fc024a96183edff323c6`; this S2 task started from the on-disk S1 state. (`093a98a24f659e764ce2dc97e9e69ac32a6b8b7b` is the accepted-plan checkpoint recorded in the S1 artifact, not the S1 accepted checkpoint.)
   - V003 SHA-256 before this task (S1 record): `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1`.
   - V003 SHA-256 after this task: `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` (**unchanged**).
   - V001 worktree SHA-256: `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` (matches the S1-recorded raw `96e7…` prefix).
   - V002 worktree SHA-256: `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` (matches the S1-recorded raw `4cf2…` prefix).
   - Author-environment Git limitation (historical): the author's WSL runner could not resolve the linked-worktree pointer (`.git` contained `gitdir: D:/codex-call/.git/worktrees/codex-call3`); `git status` exited `128` with `fatal: not a git repository: .../D:/codex-call/.git/worktrees/codex-call3`, so the author could not run HEAD-blob or `git diff` checks. The controller has since executed these checks (see §9).
   - Controller HEAD-blob evidence: V001 HEAD blob SHA-256 `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`; V002 HEAD blob SHA-256 `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`; CR-normalized V001/V002 diff exit 0. V003 HEAD/source SHA-256 remains `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` (unchanged).

4. Changed paths (exactly the four allowed writes), SHA-256
   | Path | Action | SHA-256 (worktree) | Note |
   | --- | --- | --- | --- |
   | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited | `8202eec4cc6cb5d6b5c62f00d19736557b04b809e8d9d468a872c3b0c025207c` | LF; 7458 lines; +862 lines (6596 → 7458); 25 `@Test`; includes the S2 correction described in §15 |
   | `.github/workflows/ci.yml` | edited | `7a68191e3f297b2553171cf012e261f754aa09fefaa3c823a96b8a430fd4bec1` | LF; +5 expected names |
   | `backend/README.md` | edited | `2b52fc9009c92b2aa32c5280392d2bf9aafb2e67e55ac8904f17e4fa7cec5da3` | LF; S2/25 scope and evidence boundary |
   | `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s2-implementation.md` | new | (this file) | LF evidence artifact |
   - No V003, V001, V002, POM, application Java, migration, other test, design/plan/control doc or other file was written.
   - The correction revision (§15) wrote only the Java test file and this artifact; the CI and README hashes above are unchanged from the original S2 author run.
   - This evidence-finalization revision (task label `pa3-v003-s2-evidence-finalize-ds-20261028-05`) wrote only this artifact. The broader exact status contains the four approved S2 paths plus controller-owned `docs/gateflow/project-execution-control.md`; the control doc is not an author write.

5. Scope actually implemented (S2 behavior methods only)
   - Added exactly five `@Test` methods (20 → 25):
     - `subjectOwnerSetOnceAndRealNameSeparation`
     - `bindingLifecycleTerminalityAndActiveNumberUniqueness`
     - `appendOnlyClaimAndForeignKeyBackstops`
     - `riskOpenClearReopenRevisionProtocol`
     - `concurrentActiveBindingUniquenessHasOneWinner`
   - Added bounded private helpers only: `resetToMigrationIdentity`, `insertSubject`, `insertBinding`, `insertClaim`, `insertRisk`, `commitSubjects`, `cleanupCommittedBindingsAndSubjects`, and the five assertion/race helpers called by the `@Test` methods.
   - Reused existing helpers/patterns: `openConnection`, `switchLocalRole`/`setLocalRole`, `expectSqlFailure` (savepoints), `CheckProbe`/`checkProbe`, `insertCustomerAccount`, `insertOrganization`, `configureBlockedWriter`, `awaitBlockedBackend`, `awaitFuture`, `queryString`/`queryLong`/`queryUuid`, `toRegclass`, `fail`, `randomSha256`, `V003_CHECK_DEF_MAP`.
   - Methods 21-24 use `autoCommit=false` and roll back; expected failures use savepoints; claim rows are never deleted. The concurrency method commits only binding/subject rows and deletes bindings before subjects.
   - No `@Disabled`, `Assumptions`, `assumeTrue`, environment skip, weakened S1 assertion, V003 edit, POM edit, application-Java edit or V004+ object.

6. CI expected set/count and README
   - `.github/workflows/ci.yml` `expected` set now contains exactly the final **25** names (the prior 20 plus the five S2 methods); `tests` is compared against `len(expected)` (25), with `skipped=0 failures=0 errors=0` and exact set equality. Docker-first order, `-Pcontainer-smoke verify`, boot smoke, fat-jar firewall, contract jobs, permissions and path filters unchanged.
   - `backend/README.md`: title/intro updated to `PA3 V003 S1–S2`; container count updated to 25; a new bullet describes the five S2 methods, S2 evidence boundary, V003 SHA preservation, and Q2/Q17/Q23/Q24/service-protocol deferral; no production-readiness claim. Toolchain pins, module table, Modulith note and firewall description unchanged.

7. Commands / environment / results
   - Author environment (static inspection only): WSL2 Linux `6.18.33.2-microsoft-standard-WSL2`; `/opt/jdk21` present; Docker binary `/usr/bin/docker` present; Maven Wrapper pin `3.9.9` / Temurin `21.0.13+9` (from repo pins). The author's runner permission layer blocked Maven/Docker/Java execution, so author-time validation was static only.
   - Static author checks executed:
     | Command | Result |
     | --- | --- |
     | `grep -c '@Test' .../PostgresContainerIT.java` | `25` |
     | `grep -n '@Disabled|Assumptions|assumeTrue|disabledWithoutDocker' ...` | no output (no skip path) |
     | `ls backend/src/main/resources/db/migration` | exactly `V001...`, `V002...`, `V003__subject_number_and_claim.sql` |
     | `sha256sum .../V003__subject_number_and_claim.sql` | `1d3ffb5f...73fb1` (matches frozen S2 value) |
     | `sha256sum .../V001... .../V002...` | `96e72bda...e1c6` / `4cf2b66d...f505` (match S1 raw prefixes) |
     | `file <four source/CI/README paths>` | `ASCII text` (no CRLF reported) for Java/CI/V003; README UTF-8 text |
     | `wc -l` | Java `7458` (pre-correction author tree `7457`), README `110`, CI `199` |
     | `sha256sum <test,CI,README>` | `8202eec4...` (corrected; pre-correction `ccf513b6...`), `7a68191e...`, `2b52fc90...` |
   - Controller environment: WSL JDK Eclipse Temurin `21.0.13+9` at `/opt/jdk21`; Maven Wrapper `3.9.9` invoked through the LF temporary wrapper `/tmp/mvnw-run/mvnw` because the checked-in `mvnw` has CRLF; Docker `29.1.3`; `postgres:16` server PostgreSQL `16.15`; Flyway applied exactly `V001`, `V002`, `V003`.
   - Controller post-gate evidence (reported by the controller; not re-executed by this author):
     - Default `clean verify`: exit 0; Surefire 10 tests, failures=0, errors=0, skipped=0; BUILD SUCCESS.
     - First pre-correction full container run (`-Pcontainer-smoke verify`, PostgreSQL 16.15): 25 tests, exactly one error from `ck_risk_cleared_after_observed` in `riskOpenClearReopenRevisionProtocol`. This failure and its correction are retained as history (§11, §15).
     - After the correction, full `-Pcontainer-smoke verify`: exit 0; 25 tests, failures=0, errors=0, skipped=0.
     - A final full `-Pcontainer-smoke verify` after the focused runs: exit 0; restored the Failsafe XML at 25/0/0/0.
     - Focused `subjectOwnerSetOnceAndRealNameSeparation`: one run exit 0; Failsafe 1/0/0/0.
     - Focused `appendOnlyClaimAndForeignKeyBackstops`: one run exit 0; Failsafe 1/0/0/0.
     - Focused `concurrentActiveBindingUniquenessHasOneWinner`: three independent runs, each exit 0 and Failsafe 1/0/0/0.
     - Final source, CI expected set and final Failsafe XML each contain exactly 25 identical unique method names; suite XML failures/errors/skipped all zero; no skip constructs; migration directory exactly `V001`/`V002`/`V003`.
     - Boot jar smoke: exit 0; log contains `Started OneCallApplication`.
     - Fat-jar firewall: no spring-security, flyway, jooq, HikariCP, postgresql or testcontainers production dependency.
   - No author-time success is claimed for commands the author did not run; all execution results above are the controller's evidence on the corrected tree.

8. Negative / concurrency evidence encoded and controller-executed on the corrected tree
   - The author encoded these assertions statically; the controller's corrected-tree full and focused runs (§7) executed them successfully, with no assertion weakened.
   - `subjectOwnerSetOnceAndRealNameSeparation`: unowned `IVR` insert with default `UNVERIFIED`/`revision=1`; runtime null->account owner set increments revision to 2; different/cleared owner `P0001` + `SUBJECT_OWNER_IMMUTABLE`; runtime `owner_organization_id`/real-name/binding-HMAC writes `42501`; verification-role real-name update to `VERIFIED` revision 3; verification owner/source/insert `42501`; owner-issued real-name/source change `P0001` + `SUBJECT_OWNER_IMMUTABLE`; all six `business_subject` checks probed with exact `23514` + constraint name; rollback leaves zero rows.
   - `bindingLifecycleTerminalityAndActiveNumberUniqueness`: seven binding checks with exact `23514` + names (including ACTIVE-without-`verified_at`); cross-subject ACTIVE HMAC conflict `23505` + `uq_binding_active_number`; `(subject_id, number_hmac)` conflict `23505` + `uq_binding_subject_number`; conditional disable (rev 2) → re-enable (rev 3) → retire (rev 4); stale revision zero rows; second retire from `RETIRED` zero rows; owner HMAC edit `P0001` + `CALLER_NUMBER_BINDING_IMMUTABLE`; runtime HMAC edit `42501`; missing-parent and parent-delete `23503` + `fk_binding_subject`; rollback.
   - `appendOnlyClaimAndForeignKeyBackstops`: valid ACCEPTED row; runtime UPDATE/DELETE `42501`; owner UPDATE/DELETE `P0001` + `SUBJECT_CLAIM_APPEND_ONLY`; all six claim checks with exact `23514` + names; accepted-duplicate `23505` + `uq_claim_accepted_subject_account`; request duplicate `23505` + `uq_claim_request`; missing-parent `23503` for `fk_claim_subject`/`fk_claim_account`/`fk_claim_organization`; parent-delete `23503` for the same three FKs; transaction rolls back and no claim is ever deleted.
   - `riskOpenClearReopenRevisionProtocol`: all six risk checks with exact `23514` + names; `(binding,type)` `23505` + `uq_risk_binding_type`; conditional clear (rev 2) → re-open (rev 3); stale/revision-mismatch and wrong-state updates zero rows; owner risk-type edit `P0001` + `SUBJECT_RISK_STATE_IMMUTABLE`; runtime edit `42501`; missing-parent and parent-delete `23503` + `fk_risk_binding`; no `catalog_order`/`ivr`/`entitlement_metering`/`activation_consent`/`evidence`/`sharing` schema and no `ivr.call_task`/`ivr.call_session` (OPEN is not a dialing/IVR behavior); rollback.
   - Correction to the valid clear path: the only CLEAR update that affects a row (OPEN→CLEARED at revision 1) now sets `cleared_at = GREATEST(observed_at, clock_timestamp())`, guaranteeing `cleared_at >= observed_at` regardless of the transaction-start clock and app/DB clock skew. The exact negative probe for `ck_risk_cleared_after_observed` (`cleared_at = observed_at - 60s`) and every other exact `23514`/`23505`/`23503`/`P0001`/`42501` probe are unchanged. The two zero-row stale/revision-mismatch CLEAR attempts are unchanged because they match no row and cannot violate the check; conditional state/revision semantics are preserved.
   - `concurrentActiveBindingUniquenessHasOneWinner`: two real connections insert ACTIVE bindings with the same random 64-hex HMAC on two committed subjects; loser is detected blocked via `application_name` + `pg_stat_activity` (`state='active'`, `wait_event_type='Lock'`); winner commit makes the loser fail with exact `23505` + `uq_binding_active_number`; loser rolls back before reading the winner; exactly one ACTIVE row remains. Rollback variant: winner rolls back, loser's insert succeeds and commits; exactly one ACTIVE row (the loser). Bounded statement/future timeouts; cleanup deletes bindings then subjects.
   - All methods use method-local random UUIDs and random 64-hex HMACs; methods 21-24 leave no rows after rollback.

9. V001/V002 hash and whitespace/diff evidence
   - V001/V002 worktree SHA-256 values above match the S1-recorded raw prefixes; this agent did not write those files.
   - V003 worktree SHA-256 exactly matches the frozen S2 value; this agent did not write V003.
   - `file` reports no CRLF for the Java/CI/V003 files (README is UTF-8 text); edits were written with LF.
   - Controller HEAD-blob gate: V001 HEAD blob SHA-256 `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`; V002 HEAD blob SHA-256 `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`; CR-normalized V001/V002 diff exit 0. V003 SHA-256 remains `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1`.
   - Controller whitespace gate: tracked exact-path whitespace checks passed (only Git line-ending warnings); the untracked artifact `--no-index` check returned the expected diff exit 1 with zero whitespace diagnostics; trailing whitespace scan clean.
   - Exact status contains only the four approved S2 paths plus controller-owned `docs/gateflow/project-execution-control.md`. The control doc is not an author write.
   - The author's own runner could not execute these git checks (§3); the results above are the controller's completed evidence.

10. README/CI changes and documentation decision
    - CI: only the failsafe expected set (25 names) and count-by-set assertion; no other step changed.
    - README: S2 evidence, 25-method profile, hash-preservation statement, deferral of service protocols/Q2/Q17/Q23/Q24, and the non-production boundary.
    - Documentation decision: no `docs/12`, `docs/14`, control document, plan artifact or PA1/PA2 artifact modified; no new architecture/API/security/schema doc. Only this S2 evidence artifact was added.

11. Deviations, stop conditions, unresolved issues
    - No stop condition triggered: no V003/V001/V002 edit, no V004+ object, no POM/application-Java/module change, no extra production function/service, no assertion weakening, no write outside the four allowed paths.
    - Controller-reproduced implementation test bug (accepted; V003 is correct and frozen): the first valid OPEN→CLEARED update used `cleared_at = now()`, whose transaction-start value could precede the row's `observed_at`, tripping `ck_risk_cleared_after_observed`. Corrected in §15 by deriving `cleared_at` from `GREATEST(observed_at, clock_timestamp())`; production SQL, V003 and all exact probes are unchanged.
    - Environment note: the author's runner approval policy blocked Maven/Docker/Java execution (the same class of block that stopped the MiMo retry), so the author claimed no execution success. The controller subsequently executed the full matrix in a Docker-capable environment (§7), and its results are recorded there.
    - The worktree `backend/mvnw` has CRLF line endings (S1 environment note); no attempt was made to normalize repository line endings. The controller invoked Maven Wrapper `3.9.9` through the LF temporary wrapper `/tmp/mvnw-run/mvnw`.
    - The author's `git` could not resolve the Windows worktree pointer from WSL; the controller completed the HEAD-blob/CR-normalized diff and whitespace gates (§9).

12. Residual risks
    - Closed validation residuals (controller-completed):
      | ID | Residual | Closed by |
      | --- | --- | --- |
      | S2-R01 | Author could not execute Maven/Docker; corrected-tree rerun was pending | Controller: `clean verify` 10/0/0/0 exit 0; corrected `-Pcontainer-smoke verify` 25/0/0/0 exit 0; final full run 25/0/0/0; focused runs and three focused concurrency runs each 1/0/0/0 |
      | S2-R02 | Exact PG16 error text/SQLSTATE behavior was not executed at author time | Controller's PostgreSQL 16.15 container run exercised the exact assertions successfully |
      | S2-R03 | Concurrency blocking detection timing-sensitive | Three independent focused `concurrentActiveBindingUniquenessHasOneWinner` runs each 1/0/0/0; the bounded helper remains in place |
      | S2-R07 | HEAD-blob/diff and whitespace gates not executable in the author runner | Controller HEAD-blob hashes, CR-normalized diff exit 0 and whitespace checks passed (§9) |
    - Retained later-owner risks:
      | ID | Residual | Class | Owner / destination |
      | --- | --- | --- | --- |
      | S2-R04 | `RETIRED` is not DB-terminal; service conditional-revision protocol is only simulated | service-owned | subject-number service (plan R-03) |
      | S2-R05 | Q2/Q17/Q23/Q24, enterprise ownership and service claim protocols remain deferred | later-work-unit | Product/Security/Platform per plan R-01..R-08 |
      | S2-R06 | GitHub Actions did not run this head; production boundary is not accepted | later-work-unit | Phaseflow/CI exact-head PR gate |

13. Completion statement
    - **Implementation author-complete and ready for independent code review; not an accepted checkpoint and not production-ready.** The five methods, CI set/count and README are written; the controller's corrected-tree validation passed `clean verify` (10/0/0/0), full `-Pcontainer-smoke verify` (25/0/0/0), the final full rerun (25/0/0/0), focused method runs (1/0/0/0 each) and three focused concurrency runs (1/0/0/0 each), plus boot-smoke and fat-jar firewall checks.
    - Corrected-tree history is preserved: the first pre-correction container run had exactly one error at `ck_risk_cleared_after_observed` in `riskOpenClearReopenRevisionProtocol`; §15 records the clock/ordering correction. No production or migration change was made.
    - Explicitly **not** performed by this author: commit, stage, push, open/merge PR, next-gate entry, V003/V001/V002/POM/application-Java/design-doc edit, Maven/Flyway/Docker execution, GitHub Actions run, production provisioning or production acceptance.

14. No-goal-drift statement and exact changed-file list
    - No goal drift: the methods prove only DB backstops and the bounded transient protocols in plan §8.3; no activation/consent/IVR/order/entitlement/evidence object, no application service, no binding-as-login, no Q24 dialing ban, no enterprise-ownership transfer, no role-to-claim permission.
    - Exact changed-file list:
      1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (edited)
      2. `.github/workflows/ci.yml` (edited)
      3. `backend/README.md` (edited)
      4. `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s2-implementation.md` (new; this report)

15. Post-gate correction `pa3-v003-s2-implementation-correction-ds-20261028-04`
    - Trigger: controller's first PostgreSQL 16.15 container run executed 25 tests and reported exactly one error in `riskOpenClearReopenRevisionProtocol`: SQLSTATE `23514`, constraint `ck_risk_cleared_after_observed`; the failing row's `observed_at` was later than `cleared_at`. Accepted as an implementation test bug; V003 is correct and frozen.
    - Root cause: the valid OPEN→CLEARED update used `cleared_at = now()`. PostgreSQL `now()` is the transaction-start timestamp, while the row's `observed_at` was written from Java `OffsetDateTime.now(ZoneOffset.UTC)` after the transaction began; the Java timestamp can therefore exceed `now()`.
    - Exact correction (Java test only): in `assertRiskOpenClearReopenRevisionProtocol`, the valid conditional clear now uses
      `UPDATE subject_number.subject_risk_state SET state = 'CLEARED', cleared_at = GREATEST(observed_at, clock_timestamp()), revision = revision + 1 WHERE id = ? AND state = 'OPEN' AND revision = 1`.
      `GREATEST(observed_at, clock_timestamp())` guarantees `cleared_at >= observed_at` even under app/DB clock skew, while retaining the statement-clock intent for a normal clear.
    - Scope review of all valid CLEAR updates in the method: the OPEN→CLEARED revision-1 update is the only CLEAR update that affects a row. The two subsequent CLEAR statements are intentional stale/revision-mismatch zero-row probes and remain unchanged; they cannot violate `ck_risk_cleared_after_observed` because no row is updated. The re-open update sets `cleared_at = NULL` and is unchanged. Conditional state/revision predicates are unchanged.
    - Probe preservation: the exact negative probe `ck_risk_cleared_after_observed` (direct insert with `cleared_at = observed_at - 60s`) and every other exact `23514`/`23505`/`23503`/`P0001`/`42501` probe and constraint-name assertion are unchanged. No production SQL, V003, V001/V002, POM, application code, CI or README was modified by this correction.
    - Files written by this correction: only `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (SHA-256 `8202eec4cc6cb5d6b5c62f00d19736557b04b809e8d9d468a872c3b0c025207c`, 7458 lines, ASCII/LF) and this artifact (UTF-8, no mojibake markers).
    - Read-only re-checks executed: `grep -c '@Test'` = 25; V003 SHA-256 = `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` (unchanged); CI SHA-256 = `7a68191e3f297b2553171cf012e261f754aa09fefaa3c823a96b8a430fd4bec1` (unchanged); README SHA-256 = `2b52fc9009c92b2aa32c5280392d2bf9aafb2e67e55ac8904f17e4fa7cec5da3` (unchanged).
    - Validation status after correction (controller-completed; §7): default `clean verify` exit 0 with Surefire 10/0/0/0; full `-Pcontainer-smoke verify` exit 0 with 25/0/0/0; a final full run after the focused runs also exit 0 and restored the XML at 25/0/0/0; focused `subjectOwnerSetOnceAndRealNameSeparation` 1/0/0/0; focused `appendOnlyClaimAndForeignKeyBackstops` 1/0/0/0; three focused `concurrentActiveBindingUniquenessHasOneWinner` runs each 1/0/0/0. No commit/stage/push/PR/review/next-gate action was taken.

16. Evidence finalization `pa3-v003-s2-evidence-finalize-ds-20261028-05`
    - This revision records the controller's completed validation of the corrected tree and closes the validation-blocked residuals. It writes only this artifact; no code, CI, README, migration, plan/design/control doc or other file was modified.
    - Environment actually used by the controller: WSL JDK Eclipse Temurin `21.0.13+9` at `/opt/jdk21`; Maven Wrapper `3.9.9` through the LF temporary wrapper `/tmp/mvnw-run/mvnw` because the checked-in `mvnw` has CRLF; Docker `29.1.3`; `postgres:16` server PostgreSQL `16.15`; Flyway applied exactly `V001`, `V002`, `V003`.
    - Results: default `clean verify` exit 0 with Surefire 10/0/0/0 and BUILD SUCCESS; corrected full `-Pcontainer-smoke verify` exit 0 with 25/0/0/0; a final full run after the focused runs also exit 0 and restored the Failsafe XML at 25/0/0/0; focused `subjectOwnerSetOnceAndRealNameSeparation` one run 1/0/0/0; focused `appendOnlyClaimAndForeignKeyBackstops` one run 1/0/0/0; focused `concurrentActiveBindingUniquenessHasOneWinner` three independent runs, each 1/0/0/0.
    - Preservation: source, CI expected set and final Failsafe XML each contain exactly 25 identical unique method names; suite XML failures/errors/skipped are all zero; no skip constructs; migration directory exactly `V001`/`V002`/`V003`. The pre-correction one-error run is retained in §7/§15.
    - Hash/whitespace gates: V001 HEAD blob `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`; V002 HEAD blob `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`; CR-normalized V001/V002 diff exit 0; V003 SHA-256 unchanged at `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1`. Tracked exact-path whitespace checks passed (only Git line-ending warnings); the untracked artifact `--no-index` check returned the expected diff exit 1 with zero whitespace diagnostics; trailing whitespace scan clean.
    - Boot/firewall: boot jar smoke exit 0 and log contains `Started OneCallApplication`; the fat-jar firewall has no spring-security, flyway, jooq, HikariCP, postgresql or testcontainers production dependency.
    - Scope boundary: implementation is author-complete and ready for independent code review; this is not an accepted checkpoint and not production-ready. Exact status contains only the four approved S2 paths plus controller-owned `docs/gateflow/project-execution-control.md`; the control doc is not an author write. No commit/stage/push/PR/review/next-gate action was taken.

17. Evidence-record fix `pa3-v003-s2-fix1-ds-20261028-01`
    - Applied exactly the two controller-accepted low-severity evidence-record findings from `docs/reviews/code-review-20261028-023647.md`; this revision writes only this artifact and changes no code, test, CI, README, V001/V002/V003, plan/design/control doc, review artifact or other file.
    - F-S2-01 (corrected): §3 now records the S1 accepted checkpoint HEAD as `9e4d25d3e9a732015d95fc024a96183edff323c6` (verified by the review via `git rev-parse HEAD`, message `gateflow: accept PA3-v003-subject-number-and-claim-foundation S1`, parent `093a98a…`), replacing the previously recorded unresolvable S1 checkpoint hash. `093a98a24f659e764ce2dc97e9e69ac32a6b8b7b` remains recorded as the accepted-plan checkpoint.
    - F-S2-02 (corrected): §4 now states `+862 lines (6596 → 7458)` for `PostgresContainerIT.java`, replacing the previously recorded overstated net delta and pre-fix-1 line-count baseline. The final count `7458`, the SHA-256 `8202eec4cc6cb5d6b5c62f00d19736557b04b809e8d9d468a872c3b0c025207c` and the 25 `@Test` count are unchanged.
    - No other evidence claim was altered. This is a documentation errata pass only: it does not assert acceptance, re-review, a passed fix gate or any next-gate entry, and no commit/stage/push/PR/merge/re-review action was taken.
