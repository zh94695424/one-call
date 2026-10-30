# PA8 V008 S1 Completion 3 — Bounded Test Correction

## Verdict and scope

- Task: `pa8-s1-gpt-fix-20261030-220343`; date: 2026-10-30 (Asia/Shanghai).
- **Verdict: blocked for S1 acceptance; corrections applied; controller rerun pending.** Compilation and static checks pass, but this session has no passing PostgreSQL run or full suite.
- Binding documents: `docs/gateflow/pa8-v008-evidence-foundation-plan.md` and `docs/14-production-data-model-and-flyway-schema-spec.md`, especially §§4.39–4.43, 10.22, 11.5 and 13.9.
- S1 test repair only. No S2, new test methods, contract changes, weaker assertions, delegation, commit/push/PR, or gate transition. Prior corrections 1/2 remain intact.
- Requested route: GPT / `gpt-6.1-sol`, high reasoning. The controller supplied the prior `ROUTE_OK` observation; this session did not re-probe routing.
- Run directory: `/tmp/sub-agents.pa8-s1-gpt-fix-20261030-220343`.

## Exact repository files changed by this correction

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
2. **New:** `docs/gateflow/pa8-v008-evidence-foundation-s1-completion-3.md`

Migrations, specification, plan, controller, CI, README and historical artifacts were read-only. Existing unrelated working-tree changes were not reverted. Maven output and the validation snapshot are under the run directory, not the repository build tree.

## Controller evidence received, not reproduced here

The prior isolated snapshot at `/tmp/pa8-controller-compile.7zqn0_nw` compiled successfully. Its `focused.log` records PostgreSQL 16.15, Flyway V001–V008 applied, and **3 tests / 2 failures / 0 errors / 0 skipped**. ACL passed; the failures were the capture-shape literal and the under-bound recording-call INSERT. These are pre-correction observations, not a passing result for this delivery.

The capture-shape literal now pins the controller-observed complete output exactly:

```sql
CHECK (((jsonb_typeof(identity_at_capture) = 'object'::text) AND (identity_at_capture ? 'subjectId'::text) AND (identity_at_capture ? 'capturedAt'::text) AND (identity_at_capture ? 'sourceChannel'::text)))
```

## Repairs and prerequisite trace

### Exact catalog assertions

- Preserve all 39 named checks, the per-table counts, and whole-definition equality. V008 now compares raw strings directly, without even the previous whitespace-only normalization; no parenthesis stripping or semantic comparison is introduced.
- Review the other literals against V008 SQL and existing PostgreSQL deparse pins in this test class. Correct the evidence state-shape OR grouping and snapshot-shape outer wrapper plus `NOT (... IS DISTINCT FROM ...)` spelling. These two additional complete literals are source-derived corrections, **not newly captured PostgreSQL output**; their runtime confirmation remains pending.
- Correct V008 trigger catalog decoding: `tgconstraint <> 0` identifies a constraint trigger; `tgtype` bits 4/8/16 identify INSERT/DELETE/UPDATE. Preserve the exact seven expected trigger signatures.
- Read restored `tgenabled` with `queryString` and assert exact `"O"`, rather than attempting to parse the catalog character as a long.

### Ordinary missing-parent probes

All eight ordinary FK expectations retain SQLSTATE **`23503`** and their exact named-constraint markers. The static audit confirms these placeholder/argument counts:

| Expected FK | Placeholders / arguments | Isolation prerequisites |
| --- | --- | --- |
| `fk_recording_call` | 4 / 4 | Explicit nonblank protocol `recording_id`; generated absent call; default DECLARED shape, valid SHA and future upload expiry |
| `fk_evidence_subject` | 9 / 9 | Absent subject is mirrored in capture and snapshot; snapshot recording identity equals `source_ref_id`; optional links null |
| `fk_recording_media_object` | 5 / 5 | Subject/session/call actually inserted in the same transaction; random absent media, not the existing media fixture; UPLOADED shape |
| `fk_evidence_call` | 11 / 11 | Existing subject, absent call, null session/reservation, complete matching snapshot; fills the missing ordinary-FK coverage within the existing test |
| `fk_evidence_session` | 13 / 13 | Existing subject/call, absent session; snapshot includes exactly that session and the source recording identity |
| `fk_evidence_reservation` | 13 / 13 | Existing subject/call; an explicitly populated absent `reservation_id` and matching snapshot, rather than an omitted nullable column |
| `fk_evidence_artifact_evidence` | 3 / 3 | Existing media; absent evidence record |
| `fk_evidence_artifact_media` | 3 / 3 | Existing evidence record; absent media, rather than two absent parents |

The existing `expectSqlFailure` creates a savepoint before execution and rolls back to it in `finally`, including on SQLSTATE/name assertion failures. Ordinary-FK record fixtures intentionally do not force the deferred creation cycle; the enclosing transaction rolls back before COMMIT, as with the plan's record-only parent-delete fixtures. No immediate constraint is disabled to manufacture a negative INSERT result.

All missing-parent prerequisites now use rollback-only transactions; the previous independently committed subject/call fixtures are removed. Snapshot-building helpers explicitly cast nullable JSON source parameters to text, so null call/session/reservation values remain typed and satisfy the complete snapshot checks instead of causing polymorphic-parameter inference errors.

### Parent deletes, committed fixtures and trigger restoration

- Preserve all eight immediate RESTRICT-delete `23503`/named-FK assertions. The subject-delete fixture has null optional call/session/reservation links, rather than an absent call that would prevent fixture insertion.
- Both protected-media deletes wrap trigger disabling and the negative statement inside the savepoint-protected `try`; `finally` rolls back to that savepoint. Successful paths additionally assert exact enabled state `"O"`; the outer transaction always rolls back.
- The sole committed fixture graph is now built atomically: subject, session/call, reservation prerequisites, evidence record and matching creation event share one transaction. Setup failure rolls back all prerequisites. Remove the unused separate subject/call/reservation commit helpers.
- Preserve the deferred-delete witness: verify the committed pair exists, assert DELETE affects one row, require COMMIT to fail with **`23503` / `fk_evidence_event_record`**, then verify both rows survived rollback. Pair cleanup runs in an enclosing `finally`, even if any observation or expected-failure assertion fails.
- Reservation policy fixtures use the next available version and `enabled=false`, avoiding collision with existing FIRST-TRIAL versions or the single enabled published-policy index without changing shared fixtures.
- Cleanup retains FK-safe deletion and per-fixture zero-residue checks. Disable the existing V006 entitlement-grant immutable trigger only for deleting the method-local grant, restore it in `finally`, and assert `"O"`. Otherwise the original cleanup would be rejected by that trigger.
- Cleanup re-enables and checks the three evidence immutable triggers before committing. Its outer `finally` rolls back on every exit, including `AssertionError`; on failure this also reverses transactional trigger DDL. Thus temporary disabling cannot be committed on a failing cleanup path. Cleanup failure itself remains a failure, not a swallowed green result.

## Validation actually performed

All Maven commands ran in `/tmp/sub-agents.pa8-s1-gpt-fix-20261030-220343/backend`, an isolated source snapshot, with `/opt/jdk21` and `/opt/maven-3.9.9`.

| Check | Actual result |
| --- | --- |
| `mvn -o -B -ntp test-compile -DskipTests` | **PASS**, exit 0; final source compiled successfully, snapshot byte equality checked |
| `mvn -o -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v008*' -DskipTests=false failsafe:integration-test failsafe:verify` | **FAIL / environment-blocked**, exit 1; class initialization cannot obtain a Docker environment; Failsafe reports **1 test / 0 failures / 1 error / 0 skipped** |
| `docker ps --format '{{.Names}}'` | Docker socket permission denied; no attempt to bypass sandbox permissions |
| Static SQL-binding audit | **PASS**: 71 literal SQL helper calls checked, including all eight missing-parent and eight RESTRICT-delete probes; one dynamic cleanup IN clause manually checked against `callIds` and its generated placeholder count |
| Test-method name-set comparison with this session's input | **PASS**: same 54 total methods and same 3 V008 methods; no new test methods |
| Turn-only diff scope check | **PASS**: all Java changes are within the V008 helper section; no legacy-wave changes retained |
| SHA-256 comparison against this session's input | **PASS** for V001–V008, all eight migration files unchanged |
| `git diff --check -- backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | **PASS**; Git emits its existing CRLF conversion warning |

Evidence files: `test-compile.log`, `test-compile.exit`, `focused.log`, `focused.exit`, `sql-binding-audit.log`, `final-static.log`, `migrations.before.sha256`, `correction.diff`, and `backend/target/failsafe-reports/` beneath the run directory. The input Java snapshot is retained there as `PostgresContainerIT.before.java` for turn-only comparison.

## Remaining validation and ownership

**Controller rerun pending.** No database migration, catalog assertion, FK probe, trigger restoration, deferred-delete rollback or fixture-cleanup behavior executed successfully in this session: Docker initialization failed before Flyway and before the focused methods. In particular, the two source-derived additional check literals and the corrected prerequisite/cleanup paths need PostgreSQL 16.15 confirmation.

The controller must rerun the three V008 methods from a fresh current snapshot, then the full unchanged **54-method S1** container suite with zero skips, and inspect Failsafe results and fixture/trigger residue. No full-suite pass, S1 acceptance, S2 readiness, or gate transition is claimed. Storage/WORM, retention and other non-persistence residual owners remain unchanged.
