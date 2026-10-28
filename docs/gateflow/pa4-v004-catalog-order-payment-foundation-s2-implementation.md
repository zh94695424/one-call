# PA4 V004 S2 implementation evidence

1. Task label / work unit / gate / slice / date / author-provider
   - Task label: `pa4-v004-s2-implementation-mimo26flash-20261028-01`
   - Work unit: `PA4-v004-catalog-order-payment-foundation`
   - Gate: `implementation`
   - Slice: **S2 only** (four approved behavior/concurrency evidence methods, 27→31; no migration, no application service, no S3/review/commit)
   - Date: 2026-10-28
   - Author/provider: MiMo 2.6 Flash (one-shot bounded implementation; no self-review, no later gate, no git stage/commit/push/PR)

2. Binding inputs read and control-document confirmation
   - Binding plan: `docs/gateflow/pa4-v004-catalog-order-payment-foundation-plan.md` — §7.2 (four-method behavior contract), §8.1 (31-name expected set), §8.4 (probe table), §8.5 (concurrency protocol), §8.6 (validation commands), §10/§11 (residuals, no-goal-drift).
   - Accepted S1 checkpoint: `20e16dc9521d26484c15578a282693d75f0fa67a` (from task binding; `git show`/`git status` unavailable this session — see §9).
   - S1 artifacts read (read-only): `pa4-v004-catalog-order-payment-foundation-s1-implementation.md` (§15–§17 controller validation: 27/0/0/0 on PostgreSQL 16.15), `pa4-v004-catalog-order-payment-foundation-s1-fix-1.md` (ACL-matrix fix; records the S1-final `PostgresContainerIT.java` SHA-256 `85634104cf3981f3aade5c0e42816d883737c3257cee33651886f1d28381acd8`, 8807 lines), plan-fix-1, plan.
   - Read-only truth: `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` (guards/checks/FKs/grants); V001–V003 migrations; full existing `PostgresContainerIT.java` helpers (`expectSqlFailure`, `checkProbe`, `setLocalRole`/`switchLocalRole`, `configureBlockedWriter`, `awaitBlockedBackend`, `awaitFuture`, fixture insert/commit helpers); `.github/workflows/ci.yml`; `backend/README.md`.
   - `docs/gateflow/project-execution-control.md`, `docs/12`, `docs/14`, the accepted plan, and all S1 artifacts: **not modified** by this agent. No controller-owned path was written.

3. Frozen migration hashes (before = after, measured this session)
   - V001 `V001__platform_schemas_roles_idempotency_outbox_audit.sql`: `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6`
   - V002 `V002__identity_organization_and_employee.sql`: `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505`
   - V003 `V003__subject_number_and_claim.sql`: `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7`
   - V004 `V004__catalog_order_and_payment.sql`: **`a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc`** (required frozen value `A7BA7A8C…6EDC`; measured at session start and re-measured after all S2 edits — byte-identical; zero migration writes in S2).
   - Migration directory contains exactly V001–V004; no V005+ file exists or was created.

4. Exact changed paths with before/after SHA-256 (CRLF baseline for maintained files; new artifact is LF)
   | Path | Action | Before SHA-256 | After SHA-256 (final) | Note |
   | --- | --- | --- | --- | --- |
   | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited | `85634104cf3981f3aade5c0e42816d883737c3257cee33651886f1d28381acd8` (8807 lines, S1 final) | `bca7a7784d4820ab28c389ed67a0c4690d95434b0892ffcaac4ab586e5fcaa06` (10733 lines) | CRLF preserved; `@Test` 27→31; +51-entry `V004_CHECK_PROBES`; RR-09 block; S2 helpers |
   | `.github/workflows/ci.yml` | edited | `cd034cd75248f73e04ee3e708713cf4e9332dd356c5d96440723546acdc0c05f` (201 lines) | `fa0cecd93fbe5203b5d4112075c4f4f9267f9ed48319e58982e4db712698c37f` (205 lines) | CRLF preserved; expected set 27→31 names only |
   | `backend/README.md` | edited | `4a520022ede0431e903bb20fb6bd85e8de6fdbdeb8f71aa1721751c864a48fc1` | `0b71a738aac4e4a7d8e2d3a4b7b41ef0f1b82361db34e80f0934a0373136ab60` (111 lines) | CRLF preserved; 31-method final V004 evidence + deferrals |
   | `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s2-implementation.md` | new | — | (this file) | LF evidence artifact |
   - Paths outside these four: **none written**. V001–V004, POM, application source, docs/12, docs/14, plan, S1 artifacts, control document untouched. No repository-wide line-ending normalization. No git stage/commit/push.

5. Contract: exactly four new `@Test` methods (final count 31)
   - The four thin delegating methods were appended directly after `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact` (now lines 1638–1655), names exactly per plan §8.1 — no rename, no removal of the existing 27:
     1. `v004CatalogVersionLifecycleAndGuardBackstops` → `assertV004CatalogVersionLifecycleAndGuardBackstops()` (line 9075)
     2. `v004OrderPaymentLifecycleAndAppendOnlyBackstops` → `assertV004OrderPaymentLifecycleAndAppendOnlyBackstops()` (line 9382)
     3. `v004ForeignKeyAndAbsentConsentFkBackstops` → `assertV004ForeignKeyAndAbsentConsentFkBackstops()` (line 9849)
     4. `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner` → `runConcurrentCatalogPublicationAndCommerceIdentity()` (line 10112)
   - `@Test` count = **31** (grep `^    @Test\r?$` = 31; the 31 `void …() throws Exception` names listed in §9 equal the CI set exactly).
   - No `@Disabled`, no `Assumptions`, no environment skip, no Docker guard anywhere in the file.

6. Method 1 — `v004CatalogVersionLifecycleAndGuardBackstops` (rolled-back transaction)
   - Fixtures (method-local UUIDs, catalog_order_role): employee (migration identity), `catalog_product`, two DRAFT `catalog_version` rows, `catalog_offer`, `catalog_benefit_line`.
   - Lifecycle: conditional revision update asserted rowcount 1 with revision+1; stale revision update → 0 rows; DRAFT→ACTIVE publish asserted `ACTIVE|true|3`; OFFSALE; re-activate (second version) with exactly one-ACTIVE invariant maintained via retire-then-activate; `uq_catalog_version_active` second concurrent-style ACTIVE insert → `23505` + exact constraint (CHECK-before-unique ordering respected: probes with wrong literal shapes use `23514` + exact name first).
   - Guards (plan §7.2, all `P0001` with exact stable messages): owner UPDATE on published version → `CATALOG_VERSION_PUBLISHED_IMMUTABLE` (snapshot columns incl. `published_at` NULL probe); offer relation columns (channel, version id) → `CATALOG_OFFER_RELATION_IMMUTABLE`.
   - Append-only/deny: product/benefit runtime role UPDATE/DELETE → `42501`; owner UPDATE/DELETE → `CATALOG_ROW_APPEND_ONLY` via `reject_row_mutation`.
   - Named checks: **22** `checkProbe`s (catalog_product 2, catalog_version 12, catalog_offer 4, catalog_benefit_line 4), each exact `23514` + exact constraint name, selected through `v004ExpectedCheckNames(...)` which asserts `V004_CHECK_PROBES` has size 51 and equals the flattened `V004_CHECK_DEF_MAP` name set.
   - Whole method rolls back; post-rollback counts on the touched tables asserted 0.

7. Method 2 — `v004OrderPaymentLifecycleAndAppendOnlyBackstops` (rolled-back transaction)
   - Fixtures: employee, customer_account, organization, business_subject, product, version, offer; valid order with method-local random `purchase_consent_id`.
   - Order: NULL `purchase_consent_id` → exact `23502` (NOT NULL violation on the column); snapshot columns (order_no, amount, consent) under owner → `CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE` (`P0001`); UNPAID→PAID→CLOSED with the frozen biconditional shape honored (`paid_at=NULL` when closing; assert `CLOSED|true|true|3`); stale revision → 0 rows; shape violation → `23514` + `ck_customer_order_payment_shape`.
   - Payment order: UNPAID→PAID(with `provider_transaction_id`)→CLOSED conditional revision; stale → 0 rows; all six identity columns under owner → `PAYMENT_ORDER_IDENTITY_IMMUTABLE` (`P0001`); runtime PAID-without-txn → `23514` + `ck_payment_order_paid_shape`.
   - Payment event: verified + unverified rows retained (counts asserted); duplicate `(provider, provider_event_id)` → `23505` + `uq_payment_event_provider_event`; runtime UPDATE/DELETE → `42501`; owner → `CATALOG_ROW_APPEND_ONLY`.
   - Named checks: **29** probes (customer_order 13, payment_order 9, payment_event 7) via parameterized `customerOrderProbeSql`/`paymentOrderProbeSql`/`paymentEventProbeSql` (channel/provider are true `?` parameters so every enum-domain check is reachable). Total 22+29 = **51 = `V004_CHECK_PROBES`**.
   - Whole method rolls back; post-rollback zero counts.

8. Method 3 — `v004ForeignKeyAndAbsentConsentFkBackstops` (rolled-back transaction)
   - Fixtures: parents (account/org/subject/employee) + four versions (offer-child, benefit-child, employee-delete-child, order-child) + offer/benefit/order/payment_order rows.
   - All **11** ordinary immediate RESTRICT FKs probed for missing parent → exact `23503` (order-side probes via `customerOrderMissingParentSql` with 11 parameters incl. `randomSha256()`).
   - Parent-delete RESTRICT → `23503` for: offer-referenced version, benefit-referenced version, order-referenced version (×…), customer_order (account/org/subject/version), payment_order (order), employee_account — asserted rowcount unchanged.
   - Product DELETE is shadowed by `trg_catalog_product_immutable` → `P0001` (not `23503`); RI revalidate flags asserted `rrs|false|false|true`.
   - `V004_FK_DEFS` (11 entries) re-asserted whole-definition equality; `fk_customer_order_consent` count 0; `purchase_consent_id` NOT NULL / no default / no FK; `activation_consent` schema zero tables (absent consent FK backstop, V007 deferred).
   - Whole method rolls back; post-rollback zero counts.

9. Method 4 — `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner` (committing; bounded two-connection scenarios)
   - Setup commits (FK-safe): customer_account, organization, business_subject, employee. Five scenario helpers run, then `finally`: executor shutdown + `cleanupV004ConcurrentFixtures`.
   - Protocol per plan §8.5: two real connections, distinct `application_name` per racer, `statement_timeout` via `configureBlockedWriter`, `pg_stat_activity` `wait_event_type='Lock'` blocking probe (`awaitBlockedBackend`, 15 s), `awaitFuture` 30 s, exact loser SQLSTATE + constraint name; no sleep-only assertion anywhere.
   - Scenarios (six races in five helpers):
     1. `runUnlockedPublicationRace` — two DRAFT versions race to ACTIVE: loser blocks on the partial unique index, then `23505` + `uq_catalog_version_active`; loser rolls back and observes the winner; terminal state exactly one ACTIVE.
     2. `runAdvisoryLockPublicationProtocol` — winner takes `pg_advisory_xact_lock(hashtextextended(?,0))`, retires v1, activates v2, commits; loser takes the same lock after, observes v2 ACTIVE, its stale retire/activate updates hit 0 rows, rolls back; final `v1=OFFSALE|rev3`, `v2=ACTIVE|rev2`.
     3. `runOrderIdempotencyRace` — same `(account_id, idempotency_key)` → loser `23505` + `uq_customer_order_idempotency`.
     4. `runPaymentOrderIdentityRaces` — same `order_id` → `23505` + `uq_payment_order_order`; same non-null `provider_transaction_id` across two orders → `23505` + `uq_payment_order_transaction`.
     5. `runPaymentEventIdentityRace` — same `(provider, provider_event_id)` → `23505` + `uq_payment_event_provider_event`.
   - Each scenario asserts exactly one committed winner and reads the winner after loser rollback (or loser failure), per §8.5.
   - Cleanup (plan §7.2 item 4): **one migration-identity `TRUNCATE TABLE` statement covering all seven `catalog_order` tables**; then FK-safe deletes of method-local parents (`business_subject`, `organization`, `employee_account`, `customer_account`); finally asserts `count(*)=0` on all seven catalog_order tables **and** zero rows for each parent UUID. No append-only audit table is ever truncated.

10. RR-09 (test-only ACL strengthening; no count change)
    - Appended inside the existing `assertV004OwnedAndCrossModuleGrants` (lines 7046–7067): for each of the five S1 tables in `s1AllColumns` (`customer_iam.customer_account`, `customer_iam.identity_profile`, `organization_access.organization`, `organization_access.organization_membership`, `subject_number.business_subject`) × the four S2 owner roles (`catalog_order_owner`, `ivr_owner`, `entitlement_metering_owner`, `activation_consent_owner`): all **7** table privileges (`SELECT INSERT UPDATE DELETE TRUNCATE REFERENCES TRIGGER`) asserted false (4×5×7 = 140 assertions), and column-level `SELECT/INSERT/UPDATE` asserted false for all 37 listed columns (4×37×3 = 444 assertions), labeled `RR-09: …`.
    - Test-only: no grant, schema, table, function or trigger was created/changed; `@Test` count unchanged by RR-09.

11. Check-probe coverage contract (`V004_CHECK_PROBES`, lines 365–416)
    - 51 `"table|check_name"` literals (2 product, 12 version, 4 offer, 4 benefit, 13 customer_order, 7 payment_event, 9 payment_order).
    - `v004ExpectedCheckNames` asserts: list size = 51 **and** list set equals the flattened names of the frozen `V004_CHECK_DEF_MAP` (the S1 whole-definition inventory), then returns each method's per-table subset. A dropped or renamed probe fails both methods statically at assertion time. Every probe is exact `23514` + exact constraint name via `checkProbe` (savepoint-rollback per expected failure).

12. CI change (`.github/workflows/ci.yml`)
    - Only change: the Failsafe `expected` set gains exactly the four S2 names (lines 105–108), keeping all 27 existing names byte-identical and `cases != expected` fail-closed. Final expected set = **31** names (count of `"\w+",` lines in the set = 31; ordered list equal to source `@Test` list — see §9).
    - Docker-first order, `-Pcontainer-smoke verify`, count/skip/failure assertions, boot smoke, fat-jar firewall, contract-regressions job: unchanged. No weakening anywhere.

13. README change (`backend/README.md`)
    - Title → `PA4 V004 S1–S2`; intro paragraph adds the S2 four-method description and container count **31**; new bullet documenting the four S2 methods (lifecycle/guards/probes/FK/concurrency/cleanup); smoke section, failsafe-count sentence and CI job description updated to 31; evidence-boundary bullet rewritten as `PA4 V004 S1–S2 证据边界（最终）` recording: 10 surefire / 31 failsafe, V004 SHA-256 `a7ba7a8c…6edc` unchanged by S2, fixture values random/hashed (no guessed business values), and explicit deferrals of **Q4/Q6/Q17/Q20/Q23 plus OQ-4 (offer withdrawal endpoint) and Q2 (envelope/KMS)** to their named service/owner work.
    - Toolchain pins, module-boundary table, Modulith note, production-jar firewall: untouched; no production-readiness or CI-green claim.

14. Exact commands — executed vs BLOCKED this session
    - Environment attempted (WSL2 sandbox): same class of environment as S1 author sessions (Temurin 21 pin, Maven Wrapper, Docker-backed Testcontainers `postgres:16`/PostgreSQL 16.15 — not re-verifiable here).
    - Required plan §8.6 commands and outcomes:
      | Command | Outcome |
      | --- | --- |
      | `docker info` | **DENIED** — Bash: `This command requires approval` |
      | `./mvnw -B -ntp -v` | **DENIED** — `This command requires approval` |
      | `./mvnw -B -ntp validate` | **NOT RUN** — same `./mvnw` class already denied this session |
      | `./mvnw -B -ntp clean verify` | **NOT RUN** — denied class |
      | `./mvnw -B -ntp -Pcontainer-smoke verify` (expected 31/0/0/0) | **NOT RUN** — denied class |
      | Focused runs of the four new methods | **NOT RUN** — denied class |
      | Concurrency method ×3 repetitions | **NOT RUN** — denied class |
      | boot smoke / fat-jar firewall | **NOT RUN** — jar cannot be built here |
      | `python3` name-set script (heredoc) | **DENIED** — `This command requires approval` |
      | `git status`/`git diff`/HEAD blob re-verification | **NOT RUN** — `.git` points to unmounted `D:/codex-call/.git/worktrees/codex-call4`; git interop denied (S1 precedent) |
      | `sha256sum` (migrations + three maintained files) | **EXECUTED** — §3/§4 hashes |
      | `file` (CRLF check ×3), `wc -l`, `grep -c` | **EXECUTED** — CRLF preserved; 10733/205/111 lines |
      | Grep: `@Test`=31; four names present; CI expected-set list; `V004_CHECK_PROBES` 51 entries (lines 366–416); RR-09 block; helper definitions ×10; guard-code references ×20; trailing whitespace 0; stale “27 个” in README 0 | **EXECUTED** — all pass |
    - Therefore **no** surefire/failsafe XML, no runtime `31/0/0/0`, no exit codes, no boot log, no fat-jar index this session. Per S1 precedent: blocked validations are reported exactly, never invented as passes. Executable validation is **controller-owned**.

15. Static validation summary (all executed this session)
    - `@Test` = **31**; source method-name list (31) = CI `expected` set (31), identical membership and order.
    - `V004_CHECK_PROBES` = **51** entries; equals flattened `V004_CHECK_DEF_MAP` (asserted at runtime by `v004ExpectedCheckNames`).
    - V004 SHA-256 unchanged (`a7ba7a8c…6edc`); V001–V003 unchanged (§3).
    - `PostgresContainerIT.java`, `ci.yml`, `README.md` all report **CRLF** via `file`; no trailing-whitespace hits in the Java file; file ends with the class-closing `}` (line 10733).
    - All ten S2 helper methods present at the lines cited in §5; four guard codes + `CATALOG_ROW_APPEND_ONLY` referenced 20× total.

16. Deviations, stop conditions, unresolved issues
    - **Deviations from the written contract: none.** Exactly four `@Test` added, no renames/removals, no new SQL object, no POM/application/plan/S1-artifact/control-doc write, CRLF preserved, new doc is LF.
    - One correctness fix made within the allowed file before validation: two `setLocalRole(connection, "catalog_order_role")` calls that executed while `subject_number_role` was active in the same transaction (PostgreSQL `SET ROLE` current_user check would fail) were changed to `switchLocalRole` (RESET then SET) — methods 2 and 3 fixture setup only; no assertion or scope change. Remaining `setLocalRole` sites are all on fresh connections, post-rollback, or migration-identity and are correct as-is.
    - **Stop condition hit:** toolchain (Docker/Maven/Python heredoc/git) unavailable under sandbox approval limits → this report is **blocked** on executable validation, not a green completion claim.
    - No self-review, no code-review gate, no later gate, no git stage/commit/push/PR/merge.

17. Residual risks (classified)
    | ID | Residual | Class | Owner / destination |
    | --- | --- | --- | --- |
    | S2-R01 | Required commands (validate, clean verify, `-Pcontainer-smoke verify` expecting exactly 31/0/0/0, focused four-method run, concurrency ×3, boot smoke, fat-jar firewall) did not execute this session — approval denied | blocked-current (author evidence incomplete) | Controller: run full plan §8.6 validation in Docker-capable, approval-enabled environment before accepting S2 |
    | S2-R02 | Git-backed checks (dirty-path set, CR-aware `git diff --check`, HEAD blob re-verification of V001–V003 against checkpoint `20e16dc9…`) not run this session | blocked-current | Controller/Backend on re-validation (plan §8.6) |
    | S2-R03 | Static-only assurance that the 51 probes and exact SQLSTATE/constraint expectations match live PostgreSQL 16.15 deparse behavior (frozen literals from plan §8.2/V004) | fixed-current (fail-closed in tests once run) | Backend/QA; escalate on any mismatch |
    | S2-R04 | GitHub Actions has not run this head (CI file changed but never executed here) | later-work-unit | Phaseflow/CI exact-head PR gate; **no CI-green claim** |
    | S2-R05 | Q4/Q6/Q17/Q20/Q23 publication/order/payment/issuance behaviors, OQ-4 offer withdrawal endpoint, Q2 envelope/KMS, production role provisioning deferred by design | later-work-unit | Product/Platform/Security/Payments per plan R-* (recorded in README evidence boundary) |
    | S2-R06 | Testcontainers `postgres:16` digest not pinned; superuser fixture only (unchanged from S1) | later-work-unit | Platform (Q17/Q23) |
    | S2-R07 | RR-09 owner-role negatives proven only by the existing ACL method once the container suite runs (no runtime proof this session) | covered-by-S2-R01 | Controller re-run |

18. Cleanup evidence (authored; execution controller-owned)
    - Non-concurrency methods 1–3: every expected failure runs through `expectSqlFailure` savepoints or a dedicated connection with exact SQLSTATE (+ constraint/message where required); each method executes one final rollback and asserts zero residual rows on its fixtures.
    - Method 4 (committing): `finally` → `executor.shutdownNow()` → `cleanupV004ConcurrentFixtures`: **single** `TRUNCATE TABLE catalog_order.…(all 7 tables)` statement executed as migration identity, then FK-safe parent deletes (`business_subject`, `organization`, `employee_account`, `customer_account` restricted to method-local UUIDs), then zero-count assertions on all seven catalog tables and each parent UUID. Append-only audit never truncated; decoys/outbox/hostile fixtures from earlier slices untouched.

19. Completion statement
    - **S2 implementation authored; executable validation BLOCKED by sandbox approval.** On disk: the four contract methods (final `@Test` = 31), the 51-entry probe contract, the RR-09 test-only ACL block, CI expected set = 31, README 31-method final evidence, and this artifact — with recorded before/after hashes (§4) and frozen migration hashes (§3). Static checks (§15) all pass; Docker/Maven/git/python invocations were denied and are reported as NOT RUN (§14).
    - Explicitly **not** performed: commit, stage, push, PR, merge, branch/config mutation, control-document/plan/S1-artifact edit, self-review gate, code review, S3 or any later gate, GitHub Actions run, production provisioning, production acceptance.
    - This report does **not** claim CI green, container smoke 31/0/0/0, A5 PASS, or production acceptance.

20. No-goal-drift statement and exact changed-file list
    - No goal drift: only the four allowed paths were written; no V005+ object, no `issue_first_trial`, no V007 consent FK, no application service/API/JPA/runtime DataSource/Flyway/POM change, no new SQL function/trigger/grant/schema/table, no guessed Q2/Q4/Q6/Q17/Q20/Q23/OQ-4 value; tests prove database-visible backstops only.
    - Exact changed-file list:
      1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (edited)
      2. `.github/workflows/ci.yml` (edited)
      3. `backend/README.md` (edited)
      4. `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s2-implementation.md` (new; this report)
    - **No later-gate action taken or implied.** The next step belongs to the controller (executable validation) and, if accepted, the independent reviewer — not to this agent.

21. Validation fix 1 (2026-10-28, MiMo 2.6 Flash, task `pa4-v004-s2-validation-fix1-mimo26flash-20261028-01`) — controller-observed `ck_catalog_product_code` fixture failure
    - Scope: test-only fixture correction in `PostgresContainerIT.java` responding to controller runtime evidence; §14/§19 blocked-execution evidence above is **retained as originally authored** (this session likewise did not run Docker/Maven/git — nothing below is rewritten as an author-run container pass).
    - Controller failure evidence (as observed by the controller, not by this author session):
      - `./mvnw -B -ntp validate` passed; `./mvnw -B -ntp clean verify` passed (Surefire 10/0/0/0); Docker 29.7.2 + PostgreSQL 16.15 clean V001–V004 migration succeeded.
      - Full `./mvnw -B -ntp -Pcontainer-smoke verify` ran exactly 31 tests: **27 passed, 4 errors, 0 failures/0 skips**. All four new methods errored before their intended assertions at `insertCatalogProduct` (`PostgresContainerIT.java` ~line 8940).
      - Observed failing `product_code` values: `P20631c3b67454cdcb1f1ec532152aec5`, `Paf7f9a1af0bb4d369bafb1759c256b34`, `P3299861f2dd24400bfbefd5c8c17198e`, `P3bb225586a0d4e53a472a45cd94b339d`. PostgreSQL SQLSTATE `23514`, constraint `ck_catalog_product_code`.
    - Root cause: frozen predicate `CONSTRAINT ck_catalog_product_code CHECK (product_code ~ '^[A-Z][A-Z0-9_-]{1,63}$')` (V004 line 57) requires an uppercase first character followed **only** by uppercase `A-Z`, digits, `_` or `-`. The S2 helper `randomProductCode()` returned `"P" + UUID.randomUUID().toString().replace("-", "")` — UUID hex is lowercase `a-f`, so the 33-character result (`P` + 32 hex chars; length within {1,63}) violated the uppercase-only charset. Every method/scenario insert path routes through this single helper, so all four methods (and the `ck_catalog_product_family` probe's *valid* code argument) failed deterministically at first `catalog_product` insert.
    - Exact edit (smallest deterministic correction; no assertion weakened, no check/probe expectation changed):
      - `randomProductCode()` now appends `.toUpperCase(Locale.ROOT)` after `replace("-", "")`, making the generated code `P` + 32 uppercase hex chars — matches `^[A-Z][A-Z0-9_-]{1,63}$`, still UUID-random hence unique across methods/scenarios and across the `uq_catalog_product_code` unique index.
      - One import added in sorted position: `import java.util.Locale;` (between `java.util.List` and `java.util.Map`).
      - CRLF line terminators preserved (byte-verified: both edited lines end `\r\n`; `file` reports CRLF-only).
    - Analogous generated-fixture inspection (four methods vs frozen checks; corrected only evidenced defects — none beyond the product code):
      - `randomSha256()` returns 64 **lowercase** hex chars → satisfies `^[0-9a-f]{64}$` (`ck_catalog_version_snapshot_sha`, `ck_customer_order_snapshot_sha`, `ck_customer_order_fingerprint`, `ck_payment_event_payload_sha`) — correct as-is.
      - `order_no` / `merchant_order_no` / `idempotency_key` / `provider_event_id` / `provider_transaction_id` generators are checked only by length + `btrim` non-empty predicates — all satisfy (lengths ≤ 64/64/475/128/128).
      - `payload_ciphertext` literals (`v1.s2.event.*`) match `^v1\.[A-Za-z0-9._:/-]+\..+$`; `version_token` literals non-blank ≤128; currency literals uppercase ×3; channel/status/audience/offer-type/billing values from the frozen allowed sets; product_family `'CORE'` matches `^[A-Z][A-Z0-9_]{0,63}$`; benefit types from the frozen IN list. No second generator defect evidenced.
    - Before/after SHA-256:
      | Path | Before | After |
      | --- | --- | --- |
      | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `bca7a7784d4820ab28c389ed67a0c4690d95434b0892ffcaac4ab586e5fcaa06` (10733 lines, S2 authored) | `a7f8e5bf67301a753c3cb480720cb0aa07cb539fc5e4a688659eba003060a060` (10734 lines) |
      | `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` | **unchanged** (re-measured this session; required frozen value `A7BA7A8C…6EDC`) |
      | `.github/workflows/ci.yml` | `fa0cecd93fbe5203b5d4112075c4f4f9267f9ed48319e58982e4db712698c37f` | unchanged (not written) |
      | `backend/README.md` | `0b71a738aac4e4a7d8e2d3a4b7b41ef0f1b82361db34e80f0934a0373136ab60` | unchanged (not written) |
    - Contract invariants re-verified statically after the edit: `@Test` count = **31**; the four approved names unchanged at lines 1639–1656; 51 probes / concurrency-cleanup contract / CI expected set / README content untouched; V004 byte identity holds; no trailing whitespace introduced.
    - Validation status (this fix session): `sha256sum`, `file`, `od -c` byte checks, and Grep static checks **executed** (above). `./mvnw` compile/focused/full container rerun **NOT RUN** — permission-denied class in this sandbox (S1/S2 author-session precedent). Executable re-validation (expect 31/31 pass, 0 errors) is **controller-owned**.
    - Residuals introduced/cleared by fix 1: the 4 controller-observed errors are addressed at root cause (deterministic format fix, not an assertion relaxation) but remain **runtime-unconfirmed** until the controller re-runs plan §8.6 (`S2-R01` remains open until that rerun is green). No new residual paths added; no later gate touched.
