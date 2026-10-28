# PA4 V004 S2 runtime correction 3

- Task label: `pa4-v004-s2-runtime-fix3-mimo26flash-20261028-01`
- Provider: MiMo 2.6 Flash
- Work unit: `PA4-v004-catalog-order-payment-foundation`
- Gate/slice: `implementation` / S2 only (third bounded runtime test correction; not review, not a new slice)

## Controller evidence after fix 2 (accepted findings)

The controller reran the combined four focused S2 methods on Java 21.0.12.1,
Maven 3.9.9, Docker 29.7.2 and PostgreSQL 16.15, with Flyway V001-V004 applied
and Surefire 10/0/0/0. The focused suite reported
`tests=4 failures=2 errors=0 skipped=0`; `v004OrderPaymentLifecycleAndAppendOnlyBackstops`
and `v004ForeignKeyAndAbsentConsentFkBackstops` passed. Exactly two
implementation findings remained (verbatim from
`backend/target/failsafe-reports/com.onecall.archtests.container.PostgresContainerIT.txt`,
XML `tests="4" errors="0" skipped="0" failures="2"`):

1. `ck_catalog_version_offer_type` probe reported
   `ck_catalog_version_billing_matrix`, exactly as fix-2 predicted:
   `[exact error/constraint marker for ck_catalog_version_offer_type] Expecting
   actual: "ERROR: new row for relation "catalog_version" violates check
   constraint "ck_catalog_version_billing_matrix" ..."` at
   `expectSqlFailure(...:2806)` <- `assertV004CatalogVersionLifecycleAndGuardBackstops(...:9339)`
   (pre-fix numbering). Not solvable with a normal full-row INSERT because an
   invalid `offer_type` necessarily violates the billing-matrix predicate too.
2. Loser-state assertion expected `DRAFT|false|1`, but its SQL selects
   `(published_at IS NULL)::text`:
   `expected: "DRAFT|false|1" but was: "DRAFT|true|1"` at
   `runUnlockedPublicationRace(...:10226)` (pre-fix numbering) - the test
   output itself proves the correct unchanged DRAFT row is `DRAFT|true|1`.

## F1 - Isolate the logically subsumed offer-type check (established repository pattern)

**Root cause.** `ck_catalog_version_offer_type`
(`offer_type IN ('SUBSCRIPTION','ONE_TIME','ADD_ON','QUOTE')`, frozen V004
line ~625 of the plan contract dump) is logically subsumed by
`ck_catalog_version_billing_matrix` (plan 8.4): every value outside the
offer-type allow-list also fails all three branches of the billing-matrix
predicate. PostgreSQL reports only one violated CHECK per statement, in
alphabetical constraint-name order, so the probe at post-fix lines 9292-9295
(`offer_type='INVALID'`, `billing_cycle='MONTHLY'`) reports
`ck_catalog_version_billing_matrix` instead of the target. The controller's
finding-1 observation (published_shape reported over sale_status) had already
confirmed that evaluation order.

**Exact correction.** The exact existing solution for the same subsumption
shape (`ck_identity_profile_status` subsumed by
`ck_identity_profile_state_shape`, helper
`expectProfileStatusViolationWithIsolatedConstraint` at lines 3316-3352) was
reused for the catalog-version offer-type probe:

- New narrowly named helper
  `expectCatalogVersionOfferTypeViolationWithIsolatedConstraint` (lines
  3354-3395, directly after the V002 pattern helper):
  - creates an outer savepoint (line 3365);
  - `switchLocalRole(connection, "catalog_order_owner")` (line 3367) - the
    schema/table owner role already established by V004 (`CREATE SCHEMA
    catalog_order AUTHORIZATION catalog_order_owner`, tables created under
    `SET LOCAL ROLE catalog_order_owner`); no migration change;
  - `ALTER TABLE catalog_order.catalog_version DROP CONSTRAINT
    ck_catalog_version_billing_matrix` inside the transaction/savepoint
    (lines 3368-3370);
  - executes the existing `ck_catalog_version_offer_type` `CheckProbe` INSERT
    (lines 3371-3374) and asserts exact SQLSTATE `23514` plus exact constraint
    marker `ck_catalog_version_offer_type` (lines 3377-3382); if the statement
    unexpectedly succeeds, `fail(...)` raises (line 3375-3376), mirroring the
    V002 helper;
  - in `finally`, `connection.rollback(savepoint)` (line 3384) so both the
    dropped constraint and the failed row are fully restored;
  - immediately queries `pg_constraint` and asserts both
    `ck_catalog_version_offer_type` and `ck_catalog_version_billing_matrix`
    exist again, count exactly 2 (lines 3386-3393) - the probe-restoration
    evidence;
  - then `switchLocalRole(connection, "catalog_order_role")` (line 3394),
    restoring the loop's pre-call role exactly as the V002 helper restores
    `customer_iam_role`.
- Loop integration (lines 9380-9390): the 22-probe loop now branches - the
  `ck_catalog_version_offer_type` entry calls the specialized helper once, all
  other 21 entries keep the unchanged `expectSqlFailure` path - and every
  iteration still executes `probed.add(probe.constraint())` exactly once, so
  exact set equality against `v004ExpectedCheckNames(...)` (lines 9391-9395)
  and `hasSize(22)` (line 9396) remain fail-closed. No double execution: the
  probe has exactly one definition site (line 9292) and exactly one call site
  (line 9383).

**Isolation re-verified statically.** With billing-matrix dropped, the probe
row (version_no=3, DRAFT, INVALID, PERSONAL, 9900, CNY, MONTHLY, 1, 1, valid
snapshot/sha, published_at NULL, revision 1) violates exactly
`ck_catalog_version_offer_type` among the remaining catalog_version checks:
audience_shape `false = false` OK; price `(NULL)=(QUOTE)` and `>= 0` OK;
published_shape `true = true` OK; revision `>= 1` OK; sale_status DRAFT OK;
version_no `>= 1` OK; currency CNY OK; snapshot type/sha OK; uq/PK random or
free (version_no 3 not used by the committed fixtures 1 and 2). No trigger
interferes: `trg_catalog_version_guard` is `BEFORE UPDATE` only. CHECK
evaluation precedes FK AFTER-triggers, so the random product/employee FK
surface is not reached.

**Plan/V004 alignment.** Plan 8.4 row `catalog_version |
ck_catalog_version_offer_type | offer_type='INVALID'` is preserved verbatim -
the violating predicate is unchanged; only the runtime probe shape isolates it,
which is the property plan 8.4 assumes for every probe ("satisfy the exact
predicate, never weaken it"). No expected constraint was renamed and
billing-matrix was not accepted as offer-type evidence. The constraint drop is
transaction-local and always rolled back; no migration, final schema, grant or
product code changed (V004 hash unchanged).

## F2 - Correct the loser-row boolean expectation

**Root cause.** The loser's UPDATE fails on `uq_catalog_version_active`
(23505) and its transaction rolls back, so the pre-existing committed loser row
remains `DRAFT`, `published_at IS NULL`, `revision 1`. The assertion's SQL
renders `(published_at IS NULL)::text`, which yields `true` for a NULL
timestamp - the controller's output (`DRAFT|true|1`) is the correct actual
state; fix-2's expected literal `DRAFT|false|1` was semantically wrong for that
projection.

**Exact correction.** Only the expected literal changed from `DRAFT|false|1`
to `DRAFT|true|1` (line 10276), with the comment (lines 10268-10271) made
explicit that `true` means `published_at` remains NULL (the query renders
`published_at IS NULL`). The winner-identity assertion on the loser connection
(lines 10256-10259), the exactly-one-ACTIVE assertion (lines 10264-10267) and
the loser row/state query itself (lines 10272-10276) are otherwise unchanged.
No loser fixture, lock-wait protocol, `application_name` separation,
`pg_stat_activity` blocking detection, bounded `statement_timeout` or
loser-rollback-before-winner-read step was touched.

**Plan/V004 alignment.** Plan 7.2 item 4 and 8.5: one commits, the loser
receives exact 23505 `uq_catalog_version_active`, rolls back, final exactly
one ACTIVE. A rolled-back loser transaction must leave its committed DRAFT row
(`published_at` still NULL, revision 1) in place; `DRAFT|true|1` is the exact
fail-closed rendering of that frozen outcome.

## Exact files and logical lines changed

Only `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
was written (three bounded edits). Logical lines (post-edit numbering):

| # | Region | Lines | Change |
| --- | --- | --- | --- |
| 1 | New isolation helper after the V002 pattern helper | 3354-3395 (javadoc 3354-3362, method 3363-3395) | added `expectCatalogVersionOfferTypeViolationWithIsolatedConstraint` (savepoint -> owner role -> drop billing-matrix -> offer-type probe INSERT with exact 23514/name -> rollback to savepoint -> pg_constraint count==2 -> switch back to `catalog_order_role`) |
| 2 | 22-probe loop in `assertV004CatalogVersionLifecycleAndGuardBackstops` | 9380-9390 | branch: offer-type probe calls the helper once; other 21 probes unchanged `expectSqlFailure`; `probed.add` still exactly once per probe; set-equality (9391-9395) and `hasSize(22)` (9396) unchanged |
| 3 | Unlocked-publication loser verify block in `runUnlockedPublicationRace` | 10268-10276 | comment clarified (`true` = published_at still NULL) and expected literal `DRAFT|false|1` -> `DRAFT|true|1` |

No other repository file was written. No test method, probe list, probe count
assertion, FK definition, lifecycle/guard assertion, append-only evidence,
RR-09 ACL assertion, concurrency protocol, migration, product source, CI,
README, controller or prior artifact was modified.

## Hashes

| Artifact | SHA-256 |
| --- | --- |
| `PostgresContainerIT.java` before this correction (fix-2 after state) | `a7456380559357033c099c496310d46c443d04f0f6ef9263d940b547966f64a1` (10747 lines, CRLF) |
| `PostgresContainerIT.java` after this correction | `3579b376d90f1c10804e47a673606813b9e875c4b845ab247cb18ccdfc5f084b` (10797 lines, CRLF) |
| `V004__catalog_order_and_payment.sql` (unchanged; required frozen value `A7BA7A8C...EDC`) | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` (unchanged) | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` (unchanged) | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` (unchanged) | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |

## Validation commands and honest results

Executable validation was attempted first. Every Maven/git/`docker run` form is
harness-denied in this WSL worktree (same environment as fix 1 and fix 2).
Denials are recorded verbatim; none of these commands ran to completion, so
none is claimed as a pass.

| # | Command | Result |
| --- | --- | --- |
| 1 | `cd backend && ./mvnw.cmd -B -ntp -Pcontainer-smoke "-Dit.test=PostgresContainerIT#<four S2 methods>" verify` (quoted-selector form) | Denied: "Command contains quoted characters in flag names" |
| 2 | `cd backend && ./mvnw.cmd -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT#<four S2 methods> verify` (unquoted-selector form) | Denied: "Command contains mid-word # which is parsed differently by shell-quote vs bash" |
| 3 | `cd backend && ./mvnw.cmd ... -Dit.test=PostgresContainerIT#concurrentCatalogPublicationAndCommerceIdentityHasOneWinner verify` (concurrency alone) | Denied: "Command contains mid-word # which is parsed differently by shell-quote vs bash" |
| 4 | `./mvnw -B -ntp -f backend/pom.xml validate` | Denied: "This command requires approval" |
| 5 | `./mvnw -B -ntp -f backend/pom.xml clean verify` | Denied: "This command requires approval" |
| 6 | `./mvnw -B -ntp -Pcontainer-smoke -f backend/pom.xml verify` (expect Surefire 10/0/0/0, Failsafe 31/0/0/0) | Denied: "This command requires approval" |
| 7 | `mvn -v` (system Maven probe) | Denied: "This command requires approval" |
| 8 | Focused four S2 methods (any invocation) | Not executed (Maven denied) |
| 9 | Repeat `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner` 3x | Not executed (Maven denied) |
| 10 | `docker run --rm -d --name pa4-s2-fix3-pg ... postgres:16` (author-side SQL smoke of the isolation sequence on a fresh temporary container) | Denied: "This command requires approval" - no author-side SQL execution possible; the pre-existing `pa3-pr11-review-pg` container was deliberately not touched (unrelated PA3 review database) |
| 11 | `git status --porcelain` | Failed: `fatal: not a git repository: .../codex-call/D:/codex-call/.git/worktrees/codex-call4` (broken worktree gitdir pointer; git unusable here) |
| 12 | `docker ps -a` | Ran. No Testcontainers/ryuk/PA4 residue; visible containers are pre-existing unrelated services (`pa3-pr11-review-pg`, `mgmt-*`, `dify-*`) |

Denial is NOT treated as a pass. The controller must rerun executable
validation (focused S2 methods, 3x concurrency repeat, `validate`,
`clean verify`, and `-Pcontainer-smoke verify` expecting Surefire 10/0/0/0 and
Failsafe 31/0/0/0).

### Static checks performed instead (all passed)

- Source `@Test` count: exactly 31 (grep-verified; the `void run()` around the
  `SqlAction` functional interface is not a test).
- Source test method names compared one-by-one against the CI expected set in
  `.github/workflows/ci.yml` lines 78-108: exact 31/31 equality, each name
  unique, including the four S2 methods. No rename, removal, disable or
  assumption was introduced.
- Probe structure intact: `V004_CHECK_PROBES` `hasSize(51)` (line 8960);
  `hasSize(22)` catalog family (line 9396) + `hasSize(29)` order/payment family
  (line 9842) = 51 named-check probes; probe-name sets still compared to
  `v004ExpectedCheckNames(...)`.
- Offer-type probe executes exactly once: one `checkProbe("ck_catalog_version_offer_type"`
  definition (line 9292), one specialized-helper call (line 9383), zero plain
  `expectSqlFailure` executions for that name (branch at lines 9382-9388);
  `probed.add` still runs once per loop iteration.
- Loser literal: `DRAFT|false|1` has 0 occurrences; `DRAFT|true|1` has exactly
  1 occurrence (line 10276).
- Probe-restoration assertion present and exact: `pg_constraint` join over
  `catalog_order.catalog_version` with both constraint names, `isEqualTo(2)`
  (lines 3386-3393), executed immediately after the savepoint rollback and
  before the role switch-back - the runtime half of this evidence is pending
  the controller rerun (denied here).
- Structure sanity: braces 858/858, parentheses 9359/9359 (grep -o counts);
  `file` reports ASCII text with CRLF line terminators only (no mixed EOL);
  byte delta (+3051 bytes for +50 lines) is consistent with CRLF on every new
  line.
- V004 hash equals the required frozen value; V001-V003 hashes recorded and
  files untouched (only the one test class plus this artifact were written).
- Allowed-path check: within this session the only repository file written was
  `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
  (mtime 2026-10-28 11:00 +0800) plus this new artifact. `.github/workflows/ci.yml`
  (mtime 09:41) and `backend/README.md` (mtime 09:43) predate this session and
  were not written by this task.
- Lifecycle/guard assertions, append-only evidence, RR-09 ACL hardening, the
  exact 31-method set, the 51-probe name set and the bounded deterministic
  concurrency protocol are untouched apart from the three bounded edits above.
- No checkstyle/LineLength enforcement exists in `backend/pom.xml`; the edited
  lines introduce no style-gate risk.

## Residual risks

| Risk | Owner / destination |
| --- | --- |
| Executable validation (focused S2 methods, 3x concurrency, `validate`, `clean verify`, cumulative container-smoke) not run in this environment: Maven and `docker run` are harness-denied and git is broken. The new isolation helper has therefore never executed; it is a line-for-line transplant of the proven V002 `expectProfileStatusViolationWithIsolatedConstraint` pattern with only table/role/constraint names changed, and all SQL strings were statically re-verified, but runtime proof (exact 23514 + exact offer-type marker under the dropped billing-matrix, restoration count==2, role switch-back) is pending. | Controller: rerun `implementation`-gate validation; any further fix only on new evidence |
| Worktree gitdir pointer is broken (`D:/codex-call/.git/worktrees/codex-call4`), so author-side `git status/diff/commit` is impossible; allowed-path and cleanup verification used mtimes, hashes and `docker ps` instead | Environment/controller ownership; not in this task's write scope |
| If the controller's rerun surfaces a failure inside the new helper (e.g., an unforeseen role/privilege or savepoint semantic difference on PostgreSQL 16.15), it is within this same approved S2 test-implementation scope and needs a new bounded evidence-bound correction | Controller: issue fix-4 only with the new failure output |

## Stop statement

Author work stops here. Both accepted corrections (F1 isolation helper + loop
integration, F2 loser-row literal) are applied to
`backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
and statically verified; all executable validation attempts are recorded above
as denials, not passes. No self-review, no commit, no push, no PR, no merge,
and no transition into review or any later gate. This artifact is
implementation evidence only: `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s2-fix-3.md`.

**Status: complete** (authoring and static validation of the two accepted
corrections; executable validation unavailable in this environment and
controller-owned, per the task's conditional validation order).
