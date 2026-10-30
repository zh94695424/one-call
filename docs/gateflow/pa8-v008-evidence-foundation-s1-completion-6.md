# PA8 V008 S1 — bounded correction7 candidate

- Task: `pa8-s1-gpt-recover7-20261030T152030Z`.
- Date: 2026-10-30.
- Status: candidate fixture fixes only; no gate acceptance or gate advancement.
- Recomputed from this isolated snapshot. Lost prior candidates/logs are not evidence.
- This completion-6 filename is the explicitly authorized artifact for correction7.

## Exact scope and actual changes

Only `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
and this new artifact are written. The Java diff is confined to V008-local fixture
paths reachable from `v008ForeignKeyAndDeferredCreationCycleFkBackstops` and its
committed cleanup helper. No tests are added or renamed. All 54 test names remain
identical to the snapshot. Historical pre-V008 helpers/tests are unchanged.

1. `v008InsertCallAndSessionSameTx` now invokes the existing read-only
   `v005CallProbeSql` / `v005ValidCallParams` conventions, supplying a fresh call-row
   UUID and synthetic 64-hex caller HMAC. This removes nonexistent `direction` and
   `peer_number_*` columns and supplies the real mandatory caller, access, node,
   protocol and identity columns with valid shapes and ACTIVE-state timestamps.
   The existing synthetic V005 ciphertext/masked placeholders are reused; no real
   telephone numbers, identities, secrets or external services are introduced.
2. The V008 reservation prerequisite employee uses synthetic 64-hex login and
   auth-subject HMACs, synthetic issuer/display metadata and the valid V002 `OPS`
   role value, rather than invalid prefixed HMACs and `OPERATIONS`.
3. Three callers resolve their own session UUID by their method-local call ID on
   the same transactional connection, while still holding `ivr_role`. The shared
   mutable `v008LastCommittedSessionId` field is removed.
4. Committed cleanup explicitly deletes each method-local call by call ID before
   deleting sessions, retains session-keyed call cleanup, and checks zero call
   residue by both call ID and session ID on the fresh verification connection.
   Existing reservation/balance/grant/policy/employee and subject cleanup stays
   intact. The cleanup ordering description now matches call/session-before-subject.

## Inspected dependencies and preserved contract

- Frozen plan: sections 2, 9 S1, helper patterns and completion obligations.
- V001 role/schema foundation; V002 employee columns/checks/defaults/grants and
  UPDATE-only guard; V003 subject columns/checks/defaults/grants and guards.
- V004/V007 references to employee, subject and session parents were inspected
  for additional prerequisites; these fixtures create no rows in those modules.
- V005 session/call columns, caller formats, mandatory fields, ACTIVE caller
  uniqueness, runtime grants, update guards and immediate foreign keys.
- V006 policy/grant/balance/reservation columns, checks, defaults, grants,
  immutable triggers and FK chain, including the policy-to-employee prerequisite.
- V008 recording/media/record/artifact/event inserts, state and JSON shapes,
  ordinary/deferred FKs, creation-cycle triggers and grants; existing role-switch,
  query, random-HMAC, employee and V005 fixture helpers were inspected read-only.
- Every reachable insert was reviewed, including direct missing-parent inserts,
  dedicated session-only construction, shared helpers and the committed matching
  record/creation-event pair. No further fixture changes were identified by source
  inspection. Valid V006 defaults, disabled method-local policy construction and
  reservation prerequisites remain unchanged.
- Negative probe SQL, SQLSTATE/constraint assertions, same-connection transactions,
  savepoint/rollback isolation, deferred commit checks, survival observation before
  cleanup, and existing transactional trigger disable/restore logic are unchanged.
  All method-local committed prerequisite rows retain explicit cleanup paths.
- All migrations, prior documentation, CI, README, plan/spec and controller are
  untouched. No delegation, model selection, commit, push, PR or remote mutation.

## Non-test validation actually performed

- Bounded `git diff --stat`, `--numstat` and unified diff review: Java has 24 added
  and 14 removed lines, confined to ten V008-local changed regions.
- A bounded snapshot-to-candidate diff audit confirms all 54 `@Test` names are
  identical and all existing `expectSqlFailure` / `expectCommitFailure` calls are
  byte-identical. It also confirms all 24,930 unchanged Java lines retain exact
  bytes. Mixed endings remain: snapshot 24,769 CRLF / 175 LF-only lines; candidate
  24,778 CRLF / 176 LF-only lines. No whole-file normalization was performed.
- Plain `git diff --check` flags retained CR line endings on added CRLF lines as
  trailing whitespace. `git -c core.whitespace=cr-at-eol diff --check` passes;
  existing mixed line endings are intentionally preserved, not rewritten.
- Final bounded scope/status and CR-aware whitespace checks cover both allowed
  paths, including a no-index whitespace check for this untracked new artifact.
- No compilation, Maven, Docker, PostgreSQL execution or tests were run. No
  PostgreSQL correctness or test-pass claim is made. The controller's supplied
  correction6 result is context only, not validation of this candidate.

## Residual risks and owner handoff

The controller owns independent compilation, focused `v008*` execution, and the
cumulative full 54-container-test plus separate 10-unit/module-test validation;
all remain pending for this candidate. Runtime SQL/ACL/FK/deferred-trigger behavior,
trigger restoration and committed-row cleanup still require that evidence.
The unchanged `max(version_no) + 1` policy allocation can contend if external
concurrent policy writers are introduced; the controller owns execution isolation
and any resulting follow-up. No contract/ownership contradiction requiring schema
or assertion changes was identified by source inspection. Any runtime failure
requires a new bounded correction, not weakened assertions or presumed acceptance.
