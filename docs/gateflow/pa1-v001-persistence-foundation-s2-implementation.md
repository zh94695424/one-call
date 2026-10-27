# PA1 V001 Persistence Foundation — S2 Implementation (Author-Complete; Ready for Independent Code Review)

- Task label: `pa1-v001-s2-impl-ds-20261027-01`
- Work unit: `PA1-v001-persistence-foundation`, behavior slice S2
- Gate: `S2 implementation`
- Author/provider: DeepSeek (`ds`)
- Date: 2026-10-27
- Base: bookkeeping head `34f9a153bc23f4d5d45f68c8d88fcd59e83f26bb`; accepted S1 slice checkpoint `03e8b916058a79e1913fb4708a146c6c3326dc42`
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa1-v001-persistence-foundation/codex-call`
- Artifact: `docs/gateflow/pa1-v001-persistence-foundation-s2-implementation.md`
- Status: **S2 author-complete and ready for independent code review. S2 is not accepted.** No commit, push, PR, code review, merge or S3 entry was performed. No GitHub Actions or production-acceptance claim is made.

## 1. Binding goal and scope

S2 adds executable PostgreSQL 16 evidence that the already-authored V001 idempotency and Outbox primitives preserve first-result semantics and transaction atomicity. It adds no domain table, workflow, API, runtime DB wiring or later migration wave.

Binding inputs read:

- `docs/gateflow/project-execution-control.md`: current PA1 section, PA1 binding goal contract, slice principle, S1 accepted checkpoint and F-PA1-PR-03.
- `docs/gateflow/pa1-v001-persistence-foundation-plan.md`: §§5.3-5.5, 5.7-5.8, 6.2, 7.1-7.3, 8-10.
- `docs/reviews/plan-review-20261027-093407.md`: especially F-PA1-PR-03.
- `docs/gateflow/pa1-v001-persistence-foundation-s1-implementation.md` and the S1 fix/re-review evidence recorded in the control document.
- `docs/14-production-data-model-and-flyway-schema-spec.md`: §4.3-4.5, §6.1-6.2, §7.1-7.2, §9.0, §9.1-9.3, §9.23, §10.2-10.8, §11.3, §12.1-12.4.
- Current V001 SQL, `PostgresContainerIT.java`, `backend/README.md`, `.github/workflows/ci.yml`, `backend/pom.xml`.

F-PA1-PR-03 is honored: the shared container is retained, every row count/read is scoped to a method-local key, Flyway/system metadata remains excluded from inventory assertions, and no append-only table is truncated. No temporary schema or role is created by S2; the S1 broken-migration schemas retain their existing `finally`-equivalent cleanup.

## 2. Exact behavior implemented

The IT method count is exactly 7: the accepted 3 S1 methods are unchanged, and exactly the 4 approved S2 methods were added. No helper is annotated as a test and no placeholder/skip/assumption exists.

### 2.1 `idempotencyNewReplayConflictAndFinalizeOnce`

Data (`scope = PURCHASE`, method-local key `s2-idempotency-<uuid>`):

- random 64-lowercase-hex fingerprint and result SHA-256;
- result type `PURCHASE_RESULT`, random result UUID, JSON object `{"order":"ok"}`.

Flow and assertions, all function calls as `subject_number_role` via `SET LOCAL ROLE` in an explicit transaction:

1. `idempotency_begin` returns `NEW`, revision `1`, non-null record id and all four result fields null; `idempotency_complete(record, 1, ...)` returns `COMPLETED` exactly once; commit.
2. A new transaction replays the same key/fingerprint: `REPLAY` returns the same record id, revision `2`, stored result type/id/snapshot/hash, and no result is recomputed; rollback.
3. A new transaction uses the same key with a changed fingerprint: `CONFLICT` with null record id/revision/result fields; rollback. The stored row (all 12 columns, including `created_at`/`completed_at`/`revision`) is byte-identical to the committed first result.
4. The same result hash with expected revision `2` returns `ALREADY_COMPLETED` without a second write; rollback; row still unchanged.
5. A different result hash on the completed row raises stable `IDEMPOTENCY_CONFLICT` (SQLSTATE `P0001`); rollback; row still unchanged.
6. A fresh method-local key is begun `NEW` at revision `1`; `idempotency_complete` with the stale/different expected revision `2` raises `IDEMPOTENCY_CONFLICT`; rollback removes that `NEW` row.
7. Final scoped assertions: the stale key has exactly 0 rows; the main key has exactly 1 row and is still equal to the committed first result.

### 2.2 `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow`

Data (`scope = IVR_CONFIRM`, method-local keys).

Flow and assertions:

1. A transaction begins a `NEW` row at revision `1` and commits it.
2. The next `begin` with the same key/fingerprint raises stable `IDEMPOTENCY_INCOMPLETE`; the transaction is rolled back before any further use.
3. The alert predicate `status = 'NEW' AND scope = ? AND idempotency_key = ?` finds exactly the committed record id.
4. `platform.ix_idempotency_new_alert` is asserted valid through `pg_index.indisvalid`, and `EXPLAIN (FORMAT JSON)` with `SET LOCAL enable_seqscan = off` shows the alert query using `ix_idempotency_new_alert`.
5. Begin-only rollback on a second key leaves 0 rows.
6. Begin-plus-complete rollback on a third key leaves 0 rows.

### 2.3 `concurrentIdempotencyRetryObservesOneWinner`

The method runs two deterministic races, each using two JDBC connections and one executor future with real PostgreSQL 16:

- **Winner commits:** the winner connection begins `NEW` and completes the first result without committing. The loser connection sets `application_name`, `statement_timeout = 20000` and `SET LOCAL ROLE subject_number_role`, then a future submits its `idempotency_begin`. The main thread polls `pg_stat_activity` until the named loser is `active` and `wait_event_type = 'Lock'` (bounded 15 s deadline, 50 ms poll interval; failure to block fails the test). The winner commits, the loser future is awaited with a 30 s bound, and the loser returns `REPLAY` at revision `2` with the winner's result fields. Exactly one contender observed `NEW`. The final method-local row is one `COMPLETED` row with the winner id and result hash.
- **Winner rolls back:** the winner begins `NEW` without completing. The loser is again detected blocked on the same key. The winner rolls back; the loser returns `NEW` at revision `1` and commits. Exactly one final method-local row exists, it is the loser's `NEW` row, and the rolled-back winner left nothing.

No outcome is based on a sleep-only ordering assertion; the decision point is the observed PostgreSQL lock wait, and every wait is bounded by `statement_timeout`, the lock-detection deadline and the future timeout. Executor and connection cleanup use `try`/`finally` and try-with-resources. On a first failure the test fails; there is no silent retry.

### 2.4 `outboxReplayConflictRollbackAndDenylist`

Data (`event_domain = IVR_EVENT`, method-local dedupe keys, random aggregate UUID, redacted JSON `{"eventType":"REPLAY"}`).

Flow and assertions, all calls as `subject_number_role`:

1. First append commits and returns an id; the scoped count is 1.
2. A second transaction appends the same domain/dedupe key and hash and receives the original id; commit; the scoped count remains 1.
3. A changed hash raises stable `OUTBOX_DEDUPE_CONFLICT`; rollback. The stored row (all 14 columns, including identity, payload, hash, status, revision and timestamps) is byte-identical and the count remains 1.
4. A new key appended inside a caller transaction that rolls back leaves 0 scoped rows.
5. Three unique keys exercise `OUTBOX_PAYLOAD_INVALID` with no row: missing payload (both payload columns null), bad ciphertext envelope (`not-a-v1-envelope`), and protected plaintext (`{"phone":"13800000000"}`). Each aborts and rolls back; each scoped count is 0.

## 3. Changed paths and SHA-256

| Path | Change | SHA-256 |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | Added exactly 4 S2 test methods plus private helpers/records; the 3 S1 methods are unchanged | `9bfddc4d5f0063e1978fa93de9ac88f54eba0f82111afc66bf3895e89e8a12b3` |
| `.github/workflows/ci.yml` | Container report assertion now requires exactly the 7 exact method names and zero skips/failures/errors; no other CI behavior changed | `ad2c45ef54718cc3ed23a771f1df3fd44fa10dbed6150607edae6299fc608842` |
| `backend/README.md` | S1+S2 title, 7-method list/count, S2 behavior evidence, honest S3-not-implemented scope | `4625f0ec81ac18e7b6f2f0427035d59b1fe65fb80ccb3fd3942549e8e2e8d631` |
| `docs/gateflow/pa1-v001-persistence-foundation-s2-implementation.md` (this artifact) | New S2 implementation record | A file cannot embed its own final SHA-256 without changing it; the final read-only digest is reported in the task completion response. |

## 4. V001 decision: unchanged

No executable S2 assertion demonstrated a V001 contract violation. V001 was therefore not modified:

- `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` SHA-256 remains `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` (identical to the accepted S1 evidence).
- The one-migration rule is preserved; no V002+ object or patch migration exists.
- Forbidden/unchanged inputs confirmed by hash and by Git scope: `backend/pom.xml` `fbc2d9ecedc2dac942c3c460b1bfc17049d4cb5ffbdda39c7b1496ebc6ddfda5`; `backend/src/test/resources/db/migration-broken/V901__pa1_broken_migration_fixture.sql` `fb3b83b384f1ccf3c9bf7ed9c60e6c3413cc987dd2035dd9d062c698cf287923`. Main Java, the existing architecture/guardrail tests, `docs/12`, `docs/14`, the control document, the accepted plan/review and the S1 artifacts were not modified.

## 5. Validation performed

Environment (task-level): WSL Ubuntu-22.04; `JAVA_HOME=/tmp/temurin-21.0.13`; `PATH` beginning `/tmp/temurin-21.0.13/bin:/mnt/d/tools/maven/bin`; Maven local repository `/mnt/d/tools/m2`; Java Temurin `21.0.13+9`; Apache Maven `3.9.9`; Docker server `29.1.3` (API `1.52`); Testcontainers `2.0.5`; PostgreSQL `16.15`; Flyway `12.4.0` (`flyway-core` and `flyway-database-postgresql`, both test scope only).

| Command | Result |
| --- | --- |
| `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 clean verify` | **PASS.** BUILD SUCCESS. Surefire totals: `tests=10`, `failures=0`, `errors=0`, `skipped=0`. |
| `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify` (full run 1) | **PASS.** Exactly one `PostgresContainerIT` report: `tests=7`, `failures=0`, `errors=0`, `skipped=0`; PostgreSQL `16.15`; V001 applied as version `001`; repeat migrate applied zero; isolated V901 reached its sentinel and rolled back. |
| `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify` (full run 2) | **PASS.** Repeat full run: `tests=7`, `failures=0`, `errors=0`, `skipped=0`; PostgreSQL `16.15`; BUILD SUCCESS. |
| Focused concurrency repetition ×3: `mvn ... -Pcontainer-smoke -Dit.test='PostgresContainerIT#concurrentIdempotencyRetryObservesOneWinner' verify` | **PASS ×3.** Each run started a real PostgreSQL `16.15` container and reported exactly `tests=1`, `failures=0`, `errors=0`, `skipped=0`, the intended method only; BUILD SUCCESS each time. No run was retried to hide a failure; there was no first failure. |
| Total concurrency executions | **5** (2 full-profile + 3 focused), all PASS. |
| Boot jar smoke | **PASS.** `java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` exited `0` and logged `Started OneCallApplication`. |
| Production fat-jar firewall | **PASS.** `jar tf` contained none of `spring-security`, `flyway`, `jooq`, `HikariCP`, `postgresql` or `testcontainers` under `BOOT-INF/lib` (grep exit `1`). |
| Flyway dependency scope | **PASS.** `flyway-core:jar:12.4.0:test` and `flyway-database-postgresql:jar:12.4.0:test`; no compile/runtime occurrence. |
| Windows Git (`powershell.exe`, worktree root): `git diff --check`; `git status --porcelain=v1`; `git diff --stat` | **PASS.** `git diff --check` exit `0` (only a CRLF-normalization warning on the Java test file, no whitespace error). Confirmed post-artifact scope is exactly three modified paths (`.github/workflows/ci.yml`, `backend/README.md`, `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`) plus this new untracked artifact; tracked diff stat `3 files changed, 714 insertions(+), 9 deletions(-)`. |

The failsafe report for the full container runs listed exactly these seven methods: `v001MigratesCleanPostgres16WithoutSkipping`, `v001InventoryMatchesTheFrozenContract`, `brokenMigrationFailsVisibly`, `idempotencyNewReplayConflictAndFinalizeOnce`, `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow`, `concurrentIdempotencyRetryObservesOneWinner`, `outboxReplayConflictRollbackAndDenylist`. A script cross-check confirmed the method-name set equals the CI expected set exactly.

## 6. Data, transaction and concurrency discipline

- Every S2 functional call runs as the granted `subject_number_role` through `SET LOCAL ROLE`; the superuser is used only for catalog reads, row reads and the existing test fixture operations. No role grant was added.
- Success paths commit explicitly where the contract requires a durable first result or Outbox row. Conflict, invalid and denial probes use `expectFailure`, which asserts SQLSTATE `P0001` and the stable code, then rolls back before any reset/cleanup.
- Every count and read is scoped to the method's own `(scope, idempotency_key)` or `(event_domain, dedupe_key)`; no test asserts a global row count and no append-only table is truncated. Committed method-local rows (the alertable `NEW` row and the replayable Outbox row) intentionally remain in the shared container, but cannot affect another method's scoped assertions.
- Concurrency uses two connections, one executor future per race, `pg_stat_activity` lock-wait detection with a bounded deadline, loser `statement_timeout`, and future timeouts. There is no timing-only pass condition and no unbounded wait.
- No `@Disabled`, `Assumptions`, `disabledWithoutDocker`, environment-variable skip or placeholder test was added.

## 7. CI and docs decision

- `.github/workflows/ci.yml` now requires exactly one failsafe `PostgresContainerIT` report with `tests=7`, `skipped=0`, `failures=0`, `errors=0`, and the exact seven-method set. Docker-first execution, toolchain assertions, boot jar smoke, production fat-jar firewall, path filters, permissions and the `contract-regressions` job are unchanged.
- `backend/README.md` now records the S1+S2 scope, the seven container methods and S2 behavior evidence, and explicitly states that the S3 audit/security matrix is not implemented. It claims no GitHub Actions success and no production acceptance.
- `docs/12`, `docs/14`, the control document, the accepted plan/review and the S1 artifacts were not modified.

## 8. Residual risks and uncovered areas

| ID | Risk / uncovered area | Status | Owner / destination |
| --- | --- | --- | --- |
| S2-R-01 | S3 audit append-only/trigger/privilege/search-path behavior is not implemented or claimed. | Deferred to the approved S3 slice | Backend/QA; S3 implementation and review |
| S2-R-02 | Two-connection races remain a bounded test risk (plan R-12). This implementation removes timing-only decisions via observed lock waits and bounded timeouts, but a host pause longer than the 15 s detection / 20 s statement / 30 s future bounds would fail closed rather than hang. | Bounded test risk | Backend/QA; independent S2 review |
| S2-R-03 | `postgres:16` image digest remains unpinned (plan R-10). | Inherited residual | Platform/CI; supply-chain hardening, no guessed digest |
| S2-R-04 | Committed method-local `NEW`/Outbox rows persist in the shared container by design for alert/replay evidence; all assertions are key-scoped and no append-only table is truncated. | Bounded decision (F-PA1-PR-03) | Backend/QA; S2/S3 review |
| S2-R-05 | The Testcontainers superuser is a test fixture, not production role/credential provisioning; Q17/Q23 remain deferred. | Deferred input | Platform/Database/Security |
| S2-R-06 | The `ix_idempotency_new_alert` plan check is PostgreSQL-16-specific; it runs against the real PG16 container and fails closed on a planner change. | Bounded test detail | Backend/QA; S2 review |
| S2-R-07 | V001 checksum finalization and the next gate decision remain controller-owned (plan R-13). | Open | Backend/Controller |
| S2-R-08 | GitHub Actions has not run; no CI-green claim is made. | Not run | Controller/Platform |

Uncovered by design: all S3 audit/security behavior, V002-V009 objects, runtime DataSource/Flyway/Hikari/jOOQ/Spring Security wiring, domain workflows, API/UI/cloud, dispatcher/queue/scheduler and production provisioning.

## 9. Completion status

**S2 IS AUTHOR-COMPLETE AND READY FOR INDEPENDENT CODE REVIEW. S2 IS NOT ACCEPTED.**

- Task label: `pa1-v001-s2-impl-ds-20261027-01`
- Artifact: `docs/gateflow/pa1-v001-persistence-foundation-s2-implementation.md`
- V001 changed: **no** (SHA-256 `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`).
- Container evidence: 2 full-profile runs at 7/7 and 3 focused concurrency runs at 1/1; 5 total concurrency executions, all passing; no failure was retried away.
- Default evidence: 10/10 surefire tests; boot smoke and fat-jar firewall pass.
- No commit, staging, push, PR creation/update, merge, code review or S3 entry was performed by this task.
- No GitHub Actions success, CI-green status or production acceptance is claimed.
