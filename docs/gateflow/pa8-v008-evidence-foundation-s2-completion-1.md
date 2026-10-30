# PA8 V008 Slice2 bounded test-role correction

## Status and scope

- Task: `pa8-s2-fix1-gpt-20261030T185808Z`.
- Bounded source correction complete; offline compilation succeeds. **Post-fix runtime validation is pending; this is not a runtime PASS or Slice2 acceptance record.**
- Read accepted plan §9 Slice2, the current `pa8-v008-evidence-foundation-s2-implementation.md`, `v008S2RecordingRollback`, and `.pa8-controller-evidence/focused.log`.
- Controller's supplied focused execution used PostgreSQL 16.15/JDK 21/Maven 3.9.9: 3 tests, 0 failures, 1 error, 0 skipped. The error is `permission denied for schema ivr` in `v008S2RecordingRollback`, pre-fix Java line 25374. The other two S2 tests passed in that controller run; that is not post-fix validation.
- Cause: the test retained `evidence_owner` for a parent-table observation outside that role's schema privileges. This correction does not identify or amend a migration defect.
- Deliverables are limited to `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` and this new completion record. Existing CI, README, prior implementation artifact and controller evidence were already present and remain byte-identical to this task's starting state.

## Exact correction and preserved witness

All Java references below are in `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` after this fix:

| Lines / entry | Result |
| --- | --- |
| 25355, `v008S2RecordingRollback` | Existing helper retained; precisely two role-switch statements added, with no other source-byte change relative to task intake. |
| 25374 | Switch to `ivr_owner` before observing the IVR parent. Uses existing `switchLocalRole`, including its role reset; no grant is introduced. |
| 25375–25376 | Existing call-parent count remains exactly one. |
| 25377 | Restore `evidence_owner` before its scoped evidence reads. |
| 25378–25380 | Existing media and recording positive-count assertions remain exactly one. |
| 25382 / 25440–25446 | Existing distinct sentinel helper is invoked only after the positive counts; its deliberate division-by-zero SQLSTATE remains exactly `22012`. |
| 25383–25384 | Existing unconditional transaction rollback retained. |
| 25387–25392 | Existing fresh-connection zero counts retained for media, recording, call, inbound session and business subject. |

All assertions, SQLSTATE expectations, frozen role ACLs and all three new plus 54 original test methods are preserved. No production change, new grant, migration amendment, broader refactor, dependency change or constraint weakening is included.

## Actual local validation

Local build/static evidence is under `/tmp/pa8-s2-fix1-validation/`, outside deliverables.

1. Executed offline compilation with the requested toolchain:

   ```bash
   JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH \
   /opt/maven-3.9.9/bin/mvn -o -B -ntp -f backend/pom.xml -Pcontainer-smoke test-compile
   ```

   `compile.log`: **BUILD SUCCESS**, 4.002 seconds; 26 test source files compiled with release 21. This is compilation only, not test execution. Log completion timestamp: `2026-10-31T02:59:49+08:00`.

2. `static.log`: byte-level comparison against the task-intake Java snapshot proves only the two specified role switches were added. Every previous assertion and all other source bytes remain unchanged.
3. Static inventory: 57 unique source test methods equal the exact 57-name CI expected set; all original 54 methods from starting HEAD remain, with exactly the three approved S2 names added. Neither CI nor the method inventory was edited by this correction.
4. Starting-file SHA-256 comparison across 1,003 existing files identifies only the allowed Java source as changed. Frozen migrations, designs, plans, CI, README, prior artifacts and controller evidence remain unchanged. The new completion record is the only additional deliverable.
5. `git -c core.whitespace=cr-at-eol diff --check` succeeds. CR-aware checking preserves the existing mixed-line-ending source convention.
6. No Docker command, focused runtime test, full suite, or Surefire test was run for this correction. No runtime PASS is claimed. Generated Maven/build outputs are bounded local build evidence only.

## Residual owners and stop boundary

| Residual | Owner / next action |
| --- | --- |
| Post-fix SQL role behavior, positive counts, sentinel and rollback execution | Controller: independently rerun the exact focused three S2 tests and retain actual logs/XML before considering the full suite. |
| Full 57-method container suite and existing Surefire 10 validation | Controller, only after focused validation; not executed or advanced by this author. |
| Independent assessment, reviews and later gates | Controller-owned; none initiated here. |
| Any subsequently discovered frozen migration defect or material design contradiction | Stop/report to controller and design owner; no silent migration amendment, grant or assertion weakening. |
| Q11 WORM and Q22 TTL/legal-retention production policies | Existing production/security/compliance design owners; unchanged and outside this test-role correction. |

No stage, commit, branch, push, PR, remote operation, comment, delegation or reviewer access was performed. Work stops at this bounded correction and completion artifact; no later gate is entered.
