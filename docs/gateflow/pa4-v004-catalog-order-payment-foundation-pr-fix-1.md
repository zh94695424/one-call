# PA4-v004-catalog-order-payment-foundation PR fix 1 (F-PA4-PR-02)

- Work unit: `PA4-v004-catalog-order-payment-foundation`; gate: fix after independent PR review.
- Task: `pa4-v004-pr12-fix1-mimo26flash-20261028-01`; provider `mimo26flash` / `mimo-v2.6-flash`; author role (not reviewer/controller).
- Base/head: immutable base `81022037dc5dced625788477835472a84a16d52d`; fixed head `c9e3358957458c7d87330b9aad481777cebb633f`; branch `codex/pa4-v004-catalog-order-payment-foundation`.
- Binding goal: frozen V004 database persistence foundation only (4 schemas / 8 roles / 7 tables / 78 columns / 51 checks / 11 FKs / 5 functions / 7 triggers / exact ACLs). V001-V004 byte-identical. No services, APIs, JPA, provider integration, runtime DataSource/Flyway wiring, V005+, frontend, cloud or provisioning work. Existing S1/S2/aggregate slices remain accepted. This fix closes accepted finding `F-PA4-PR-02` only.

## Scope

- Source of the finding: `docs/reviews/pr-12-review-20261028-123429.md` (SHA-256 `AF745EDAAC960C2D2B3BE2B8E8827395ECA190E753A6E8E2C670053BC5992B35`), finding `F-PA4-PR-02` (accepted by the controller; open question resolved in favor of legal independent fixtures, not relabeling forbidden transitions).
- Frozen rule (docs/14 §7.3): catalog version `OFFSALE` is terminal for sale; customer order `PAID` and `CLOSED` are terminal for the ordinary flow; payment order has the same verified-evidence transitions as the order. Terminality is owned by the later catalog-order service. The database intentionally owns only shapes and immutable data (docs/14 §10.16 allows `sale_status`/`revision` and payment status/terminal time changes; no state-machine trigger was or may be added).
- Changed paths (exactly the allowed delta):
  - `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (test-only; +104/−43)
  - `backend/README.md` (line 64 lifecycle text only; +1/−1)
  - this new artifact `docs/gateflow/pa4-v004-catalog-order-payment-foundation-pr-fix-1.md`
- No migration, CI, control-document, design-document, prior-artifact, controller `.pa4-*` or product file was modified. The pre-existing controller-owned `docs/gateflow/project-execution-control.md` worktree modification and untracked controller files were not touched by the author.

## Finding author-status

- `F-PA4-PR-02`: author-reported **已修复**, pending fresh independent DeepSeek re-review. The suite no longer executes or documents `OFFSALE→ACTIVE` or `PAID→CLOSED` as successful lifecycles; forbidden transitions are now either absent or present only as clearly labeled synthetic invalid-shape probes that must fail with the exact check name.

## Exact legal fixture paths

### `assertV004CatalogVersionLifecycleAndGuardBackstops`

1. `versionId` (product A): insert `DRAFT` revision 1 → conditional `DRAFT` revision 1→2 (rowcount 1) → stale revision-1 repeat → 0 rows → publish `DRAFT` revision 2→`ACTIVE` (`published_at=now()`, revision 3, state asserted `ACTIVE|true|3`) → owner published-content guards (`CATALOG_VERSION_PUBLISHED_IMMUTABLE` on snapshot and on `published_at=NULL`) while still `ACTIVE` → offer mutable updates and `CATALOG_OFFER_RELATION_IMMUTABLE` relation guards → `uq_catalog_version_active` second-ACTIVE denial (`23505` + exact constraint on `secondVersionId`) while `versionId` remains `ACTIVE` → legal `ACTIVE→OFFSALE` revision 3→4 (terminal; never reactivated).
2. `secondVersionId`: insert `DRAFT` revision 1 → the second-ACTIVE denial above (zero rows committed by the failed statement) → legal publish `DRAFT` revision 1→`ACTIVE` revision 2 only after the first version is retired → exactly-one-ACTIVE assertions (count = 1; active id = `secondVersionId`).
3. No statement in the method transitions `OFFSALE` back to `ACTIVE`. The former re-activate probe was deleted, not relabeled.
4. Unchanged: 22 product/version/offer/benefit `23514`+exact-name probes (including the transaction-local `ck_catalog_version_offer_type` isolation/rollback), append-only runtime `42501` and owner `CATALOG_ROW_APPEND_ONLY` probes, method rollback and post-rollback zero counts.

### `assertV004OrderPaymentLifecycleAndAppendOnlyBackstops`

1. `orderId`: insert `UNPAID` revision 1 → legal `UNPAID→PAID` (`paid_at=now()`) revision 1→2 → state asserted `PAID|false|false|2` and the fixture stays terminal (never closed) → stale revision-1 `UNPAID→PAID` replay (`WHERE payment_status='UNPAID' AND revision=1` against the now-`PAID`/revision-2 row) → 0 rows, rejected by the combined status and revision mismatch (not a status-matching probe; genuine status-matching stale-revision evidence is provided by the independent `closedOrderId`, `paymentOrderId` and `closedPaymentOrderId` fixtures below).
2. `closedOrderId` (new independent fixture with fresh order_no/idempotency key/consent): insert `UNPAID` revision 1 → legal `UNPAID→CLOSED` (`closed_at=now()`, `paid_at` stays NULL) revision 1→2 → state asserted `CLOSED|true|true|2` → stale revision-1 `CLOSED→CLOSED` repeat → 0 rows (real stale revision on the matching status).
3. Synthetic invalid-shape probe on `closedOrderId` (expected failure, not a lifecycle): `PAID` with `paid_at=NULL` where `payment_status='CLOSED' AND revision=2` must fail `23514` + `ck_customer_order_payment_shape`. The WHERE clause matches the live row so the check fires; it cannot pass as a zero-row no-op (`expectSqlFailure` fails the test if the statement succeeds).
4. `paymentOrderId`: insert `UNPAID` revision 1 → legal `UNPAID→PAID` with `provider_transaction_id` revision 1→2, stays terminal (never closed) → stale revision-1 `PAID→PAID` repeat → 0 rows → all six owner `PAYMENT_ORDER_IDENTITY_IMMUTABLE` guards unchanged.
5. `closedPaymentOrderOrderId` + `closedPaymentOrderId` (new independent order and payment-order fixtures, required because `uq_payment_order_order` allows one payment order per order): payment order insert `UNPAID` revision 1 → legal `UNPAID→CLOSED` revision 1→2 → stale revision-1 `CLOSED→CLOSED` repeat → 0 rows.
6. Synthetic invalid-shape probe on `closedPaymentOrderId` (expected failure, not a lifecycle): `PAID` with `provider_transaction_id=NULL` where `status='CLOSED' AND revision=2` must fail `23514` + `ck_payment_order_paid_shape`.
7. Clock anchoring: this method now reads `createdAt` from the database (`SELECT now()`) instead of the host JVM clock. The first container run of this fix failed `ck_customer_order_time_order` because the container clock lagged the Windows JVM clock by ~73 ms, making transaction `now()`-based `paid_at` earlier than the Java-captured `created_at`. Anchoring to the database clock removes host/container skew; all time-order checks now compare one clock. No assertion was weakened.
8. Unchanged: NULL `purchase_consent_id` `23502`, three `CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE` guards, payment-event verified/unverified retention counts, `uq_payment_event_provider_event` `23505`, event runtime `42501`/owner `CATALOG_ROW_APPEND_ONLY`, the 29 customer_order/payment_order/payment_event `23514`+exact-name probes (13+9+7 = 51 total with the catalog 22), method rollback, and post-rollback zero counts (extended to the new fixture ids).

### Preserved overall

- Fixed 31 `@Test` methods; zero `@Disabled`, `Assumptions`, Docker guards or skips; no method renamed or removed.
- 51 check probes, 11 FK backstops (`v004ForeignKeyAndAbsentConsentFkBackstops` untouched), exact ACL inventory and denial matrices untouched, five concurrency scenarios untouched.
- No assertion was deleted or weakened; invalid-shape statements that are expected to fail remain tests and are labeled as synthetic probes.

## README and historical-artifact decision

- `backend/README.md` line 64: the two lifecycle fragments were rewritten. Catalog now reads legal `DRAFT→ACTIVE→OFFSALE` plus a second version's `DRAFT→ACTIVE`, explicitly labeled synthetic DB backstops with service-owned `OFFSALE` terminality and "the database does not prove `OFFSALE→ACTIVE`". Order/payment now reads legal `UNPAID→PAID` with the PAID fixture kept terminal plus an independent `UNPAID→CLOSED` fixture, explicitly labeled synthetic DB backstops with service-owned PAID/CLOSED terminality and verified-provider evidence and "the database does not prove `PAID→CLOSED`". No other README line changed.
- Historical artifact `docs/gateflow/pa4-v004-catalog-order-payment-foundation-s2-implementation.md` lines 45 and 53-54 still describe the pre-fix probes ("re-activate"; "UNPAID→PAID→CLOSED"; payment order "UNPAID→PAID→CLOSED"). That file is an immutable historical record and was **not** edited. **This artifact explicitly supersedes those lifecycle claims**: they described the then-accepted synthetic probe sequencing, are stale as lifecycle evidence, and must not be cited as proof that `OFFSALE→ACTIVE` or `PAID→CLOSED` are legal. The authoritative lifecycle evidence is the post-fix test class plus this artifact; the frozen state machine in docs/14 §7.3 is unchanged and was always binding.
- No docs/14, docs/12, plan, migration or CI text changed. No state-machine trigger, service or provider evidence was added; DB-owned shape guards are exactly as frozen.

## Actual validation (fresh runs in this session, Windows JDK 21.0.12.1)

All commands run via `powershell.exe -NoProfile` with `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` and backend cwd; WSL `mvnw` was not used (CRLF). Runs were sequential; no other Maven process was running.

| Check | Command | Result |
| --- | --- | --- |
| Workspace | `pwd -P`; `git branch --show-current`; `git rev-parse HEAD` | Correct workspace; branch `codex/pa4-v004-catalog-order-payment-foundation`; head `c9e3358957458c7d87330b9aad481777cebb633f`. |
| validate | `mvnw.cmd -B -ntp validate` | BUILD SUCCESS; Enforcer 5 rules passed. |
| default verify | `mvnw.cmd -B -ntp clean verify` (final code) | BUILD SUCCESS; Surefire `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` (1+8+1). |
| container smoke (first run) | `mvnw.cmd -B -ntp -Pcontainer-smoke verify` | Failed 1/31: `ck_customer_order_time_order` on the PAID transition caused by ~73 ms host/container clock skew (`paid_at` from container `now()` earlier than Java-captured `created_at`). No assertion or schema issue. |
| container smoke (after DB-clock anchoring, final code) | `mvnw.cmd -B -ntp -Pcontainer-smoke verify` | BUILD SUCCESS; PostgreSQL 16.15; Flyway applied/validated four migrations (001-004); Failsafe `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`; elapsed ~15.3 s. |
| XML method sets | read-only Python parse of Failsafe/Surefire XML | Failsafe `tests=31 errors=0 skipped=0 failures=0`; the 31 `testcase` names equal the source `@Test` set and the CI expected set exactly; Surefire 1+8+1 = 10; zero disabled/assumption markers. |
| Frozen Git blobs | `git cat-file blob HEAD:<migration> \| sha256sum` | V001 `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`; V002 `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5`; V003 `1D3FFB5FFD60D1BE5F85B93B87F52C727E7B43DAD5929280123CC567AB873FB1`; V004 `A7BA7A8CBAF5EECAF04F117B53C1B4BFD31C31B04E9432984D6C108B6E496EDC` — all match the frozen values; `git diff HEAD` touches no migration. |
| V004 worktree/packaged | `sha256sum`; Python `zipfile` of the boot jar | Worktree and packaged `BOOT-INF/classes/db/migration/V004__catalog_order_and_payment.sql` both `A7BA7A8CBAF5EECAF04F117B53C1B4BFD31C31B04E9432984D6C108B6E496EDC`; jar contains only V001-V004 migrations. |
| Scope/whitespace | `git diff --stat/--numstat/--check HEAD -- backend/`; `git status --short` | Only `backend/README.md` (+1/−1) and the test class (+104/−43) changed; `git diff --check` exit 0; controller-owned control-doc modification and `.pa4-*` untracked files untouched by the author. |
| Post-fix hashes | `sha256sum` | `PostgresContainerIT.java` `F81FC178FF0CFE895584B28630D033DBAFC9C8F0585966799E8A758CA9FBD72E`; `backend/README.md` `FA37B8C58BFCA104348529C96BCE0273F6EF9ABC65CB1679C9B7F90CD2E43AB8`. |
| Docker cleanup | `docker ps -a` after final run | Testcontainers containers removed; `pa3-pr11-review-pg` and unrelated long-lived containers preserved. |

Inherited evidence (not rerun in this session): boot smoke, production-jar dependency firewall, focused three-run concurrency repetitions, exact-head GitHub CI run `40632305220`, and all prior S1/S2/aggregate review conclusions. Failure-injected timeout/race cleanup was **never run** in any session and is not claimed here.

## Residual risks and owners

| ID | Risk | Owner | Destination |
| --- | --- | --- | --- |
| RR-01 | Exact-head CI re-run and JDK pin (local 21.0.12.1 vs CI 21.0.13+9) after the fix commit/push. | Controller/CI | After re-review acceptance and follow-up push; head changes require a fresh CI run. |
| RR-02 | `postgres:16` floating tag (16.15 observed), fixture superuser, production provisioning/digest not pinned. | Platform/Database/Security (Q17/Q23) | Production/image hardening waves. |
| RR-03 | Serial seven-table `TRUNCATE` cleanup; timeout/aborted-race failure injection was never executed. | Backend/QA | Later test-hardening slice if the controller requests failure-injection evidence. |
| RR-04 | V004 check/index literals come from PG 16.15 deparse; minor-version deparse drift fails the suite. | Backend/QA | Pin 16.15; real diffs stop for review. |
| RR-05 | Other stale README/Javadoc labels outside line 64 (counts are correct). | Backend/QA | Later documentation pass. |
| RR-06 | Production purchase/payment/issuance/provider payloads, Q2/Q6/Q20, catalog-offer withdrawal (OQ-4) remain unimplemented service work. | Named service owners | Later service phases, never guessed in V004. |
| RR-07 | Column-level REFERENCES denial coverage still absent (no live grant). | Backend/QA | Deferred hardening slice (carried from PR review). |
| RR-08 | Focused three-run concurrency repetitions are inherited controller evidence without per-run XML from this session. | Controller/QA | Full container profile suffices; repetition XML on demand. |
| RR-09 | Concurrency failure-path (race interruption then cleanup) not injected. | Backend/QA | Same destination as RR-03. |
| RR-10 | Closed by this fix subject to fresh independent re-review of the final diff, README text and this artifact. | Controller/re-review | Next gate: fresh DeepSeek PR re-review. |

## Completion

- Runtime/provider/task: local Windows JDK 21.0.12.1 + Docker PostgreSQL 16.15; provider `mimo26flash` (`mimo-v2.6-flash`); task `pa4-v004-pr12-fix1-mimo26flash-20261028-01`.
- Paths + artifact: this artifact at `docs/gateflow/pa4-v004-catalog-order-payment-foundation-pr-fix-1.md`; changed files `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (SHA-256 `F81FC178FF0CFE895584B28630D033DBAFC9C8F0585966799E8A758CA9FBD72E`) and `backend/README.md` (SHA-256 `FA37B8C58BFCA104348529C96BCE0273F6EF9ABC65CB1679C9B7F90CD2E43AB8`).
- `F-PA4-PR-02` author status: **已修复**, awaiting independent re-review; no self-review performed.
- Actual changes: catalog probes reordered so the first ACTIVE stays ACTIVE for the partial-unique denial and is then legally retired before the second DRAFT is published (no `OFFSALE→ACTIVE`); order/payment fixtures keep the PAID rows terminal and use independent legal `UNPAID→CLOSED` fixtures for CLOSED shapes, with real stale-revision probes and clearly labeled synthetic invalid-shape probes that fail the exact checks; fixture timestamps anchored to the database clock to remove host/container skew; README line 64 lifecycle text corrected; historical S2 lifecycle claims explicitly superseded here without editing the immutable artifact.
- Validation: fresh sequential `validate`, `clean verify` 10/0/0/0 and `-Pcontainer-smoke verify` 31/0/0/0 (PG 16.15, four migrations) on the final code; exact source/CI/XML method sets of 31 with zero disabled/skip/failure/error; Surefire total 10; frozen V001-V004 hashes unchanged; scope/whitespace/cleanup checks clean. One intermediate container failure (clock skew) is disclosed above and was fixed within the allowed delta.
- Permission denials or blocks: none in this session; no commit, stage, push, PR mutation, merge, comment, nested agent or runner was performed. The controller owns intake, control-document update and the fresh DeepSeek re-review.
