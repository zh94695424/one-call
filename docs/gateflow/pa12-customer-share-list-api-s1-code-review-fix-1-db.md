# PA12 customer share list API — code-review fix 1 DB evidence

- Gate: `fix`
- Task: `pa12-code-review-fix1-gpt-db-20261102-192000`
- Date: 2026-11-02
- Status: `db-evidence-authored; runtime pending Controller`
- Scope: only the PostgreSQL evidence portion of Primary F-01, A F7, and B F-08. This report does not claim closure of the other accepted findings or advance the review gate.

## Inputs and boundary

- Read accepted plan §§6, 7, and 8.2; Primary `docs/reviews/code-review-20261102-180831.md`; Reviewer A `docs/reviews/code-review-20261102-181145.md`; Reviewer B `docs/reviews/code-review-20261102-192607.md`; prior fix report `docs/gateflow/pa12-customer-share-list-api-s1-code-review-fix-1.md`; and the PA12 Controller adjudication in `docs/gateflow/project-execution-control.md`.
- Changed paths: `backend/src/test/java/com/onecall/archtests/container/CustomerSharesFixture.java`, `backend/src/test/java/com/onecall/archtests/container/CustomerSharesIT.java`, and this report only.
- No production, migration, POM, CI, README, `CustomerMeFixture`, protected, review, plan, control, or prior-artifact path changed. No commit was created.

## Evidence changes

- Preserved all eight existing `@Test` method names in `CustomerSharesIT`; per-method `@BeforeEach` still resets migrated PostgreSQL data. The frozen baseline `PostgresContainerIT` 64, `SharingQueryServiceIT` 10, and `CustomerMeIT` 10 methods were not edited; the intended container inventory remains 92 methods across four classes.
- Added a PostgreSQL `valid_from DESC, share_id DESC` baseline query, equal-time non-ASCII IDs, encoded cursor round trips, limit-one and multirow/tail pages, empty next-cursor coverage, and insertion of a newer row after page one without continuation regression.
- Added zero-checkout assertions for cross-account, cross-workspace/organization, tampered cursor, and invalid-limit requests. The next-page lifecycle matrix exercises disabled account, suspended and revoked membership, and non-ACTIVE (`DISABLED`) organization; request validation is asserted before lifecycle denial. `/me` is compared before and after a sharing-query failure.
- Captured checkout ownership by exact datasource bean name and retained SQL/session facts for `session_user`, `current_user`, role, read-only, read-committed isolation, PID, checkout, and pool. Assertions distinguish customer and sharing pools; all three test logins are checked as login-capable, non-superuser, non-DB-creator, and non-role-creator identities.
- Added real ACL probes for undeclared foreign columns, foreign writes/locks, and role escalation (`42501`). The separate test-owned read-only transaction probes an otherwise permitted sharing write for `25006`, rolls back, restores `readOnly=false` and `autoCommit=true`, and verifies unchanged row hashes. No application read-only transaction write is claimed.
- Snapshots now SHA-256 the canonical ordered row JSON for 16 tables, including lifecycle revisions, sharing state, evidence/media state, audit facts, outbox events, and idempotency records. The IT compares before/after hashes for success and HTTP 400/401/403/422/500/503 paths; success and failure paths assert no row/event/revision changes.
- Added test-owned statement disconnect, ResultSet mapping failure using typed `ShareQueryException.Code.INTERNAL`, commit failure, and pool-reuse checks. Web-only missing query graph, denied sharing login, verifier outage, sanitized database failures, and unchanged healthy `/me` behavior are exercised within the existing methods. Service diagnostic logs, HTTP bodies, SQL projection text, and bindings are checked against credential, token/hash/salt, object/media/permanent-URL, and private-identity canaries.

## Counts and audits

- `CustomerSharesIT`: 8 `@Test` methods; method-name comparison against `HEAD` is identical (the `reset` lifecycle method is unchanged as well).
- Container baseline preserved: 64 + 10 + 10 = 84 methods; with the existing eight PA12 additions, 92 methods / four Failsafe classes. This is a source-level inventory statement, not a runtime report.
- Snapshot coverage: 16 full-table row-hash entries per snapshot.
- `mvn -B -ntp -f backend/pom.xml -DskipTests -Pcontainer-smoke test-compile`: `BUILD SUCCESS` on Java 21.0.13 / Maven 3.9.9.
- Native `git diff --check`: passed.
- Protected-path audit: tracked diff contains only the two owned Java test paths; the only intended new path is this report. `.codex` was a pre-existing untracked harness marker and was not modified.
- No Docker, Testcontainers, PostgreSQL/Flyway, or integration-test execution is claimed. The Controller owns the final Windows Docker/PostgreSQL run.

## Residual verification

- Runtime behavior of the newly added PostgreSQL fault, mapping, commit/disconnect, ACL, logging, and pool-reset assertions remains unverified in this worktree. In particular, Controller must confirm that the test-owned ResultSet proxy reaches the intended typed `INTERNAL` mapping path and that all injected failures are recovered by the actual PostgreSQL/Hikari transaction boundary.
- This bounded report addresses only the database portion of the accepted review findings. All other accepted findings remain subject to their separately authorized fixes and re-review; no broader S1 acceptance or production-readiness claim is made.
