# PA4 V004 S2 runtime correction 2

- Task label: `pa4-v004-s2-runtime-fix2-mimo26flash-20261028-01`
- Provider: MiMo 2.6 Flash
- Work unit: `PA4-v004-catalog-order-payment-foundation`
- Gate/slice: `implementation` / S2 only (runtime test correction; not review, not a new slice)

## Controller evidence after fix 1 (accepted findings)

The controller ran the four focused S2 methods (Java 21.0.12.1, Maven 3.9.9,
Docker 29.7.2, PostgreSQL 16.15, Flyway V001-V004, Surefire 10/0/0/0) and got
`tests=4 failures=4 errors=0 skipped=0`. Fix 1 was proven effective: all four
tests now pass the former JSONB binding point and fail later, on four accepted
test-implementation findings (sale-status probe target-isolation; legal
PAID-without-transaction probe; missing payment-event child for the
payment-order parent delete; wrong unlocked-publication loser-row assertion).

## Finding 1 - Catalog sale-status probe violated the wrong check first

**Root cause.** `ck_catalog_version_sale_status` probe (pre-fix lines
9244-9247) paired `sale_status='INVALID'` with `published_at=null`. That row
violates both `ck_catalog_version_sale_status`
(`'INVALID' IN ('DRAFT','ACTIVE','OFFSALE')` is false) and
`ck_catalog_version_published_shape`
(`('INVALID'='DRAFT') = (published_at IS NULL)` is `false = true`). When a row
violates several CHECK constraints, PostgreSQL reports only one; the controller
observed `ck_catalog_version_published_shape` reported, so the exact-name
assertion for the sale-status check failed.

**Exact correction.** The probe's `published_at` argument changed from `null`
to `OffsetDateTime.now(ZoneOffset.UTC)` (post-fix line 9248). With a non-null
published timestamp, `ck_catalog_version_published_shape` becomes
`false = false` (satisfied), and the row violates exactly
`ck_catalog_version_sale_status`. The expected `23514` +
`ck_catalog_version_sale_status` assertion is preserved verbatim. All other
predicates in the probe row were already valid and are unchanged.

**Plan/V004 alignment.** Plan 8.4 lists the violating value as
`sale_status='INVALID'` and requires each probe to "satisfy the exact
predicate, never weaken it" - the fix makes every non-target predicate valid,
which is the isolation property plan 8.4 assumes. The frozen check definitions
are untouched.

**51-probe target-isolation audit (per controller instruction).** All 51
`V004_CHECK_PROBES` entries were statically re-audited against every CHECK on
their table. Fifty are target-isolated (exactly one violated check each,
including the corrected sale-status probe). One further entry has the same
multi-violation shape and is recorded as residual risk, not changed, because
the controller bound additional probe changes to executable evidence:

- `ck_catalog_version_offer_type` (pre/post-fix lines 9249-9252) uses
  `offer_type='INVALID'` with `billing_cycle='MONTHLY'`. That row violates
  `ck_catalog_version_billing_matrix` as well, because all three branches of
  the billing-matrix predicate require `offer_type` in
  `{SUBSCRIPTION, ONE_TIME, ADD_ON, QUOTE}`. PostgreSQL reports only one of
  the two failing constraints. Supporting context: the controller's
  executable observation for finding 1 (published_shape reported over
  sale_status) is consistent with PostgreSQL evaluating table CHECK
  constraints in alphabetical constraint-name order within a table
  (pg_constraint conname index order); under that order
  `ck_catalog_version_billing_matrix` would be reported before
  `ck_catalog_version_offer_type`, so this probe may fail its exact-name
  assertion once reached. This probe was NOT changed in fix 2: no executable
  evidence for this specific probe exists yet, and the controller's finding-1
  text bound additional probe changes to executable evidence. Note for any
  future correction: because every value outside the offer_type allow-list
  also fails the billing-matrix predicate, a full-row INSERT probe cannot
  isolate `ck_catalog_version_offer_type` alone; a future bounded correction
  will need a different probe shape or a renamed expected constraint - that
  decision belongs to the controller, not to this task.

No other probe change was made.

## Finding 2 - PAID-without-transaction probe was actually legal

**Root cause.** The runtime probe at pre-fix lines 9541-9546 set
`status='PAID'` on a payment order that had already been moved through PAID
with a non-null `provider_transaction_id` (lines 9497-9502) and then CLOSED.
The probe left that transaction id in place, so
`ck_payment_order_paid_shape` (`status <> 'PAID' OR provider_transaction_id IS
NOT NULL`) was satisfied and no 23514 was raised; `expectSqlFailure` failed
with "expected SQL failure 23514 but the statement succeeded".

**Exact correction.** The same allowed runtime update now also sets
`provider_transaction_id = NULL` (post-fix line 9544). The resulting PAID row
violates exactly `ck_payment_order_paid_shape`. Isolation re-verified:
`ck_payment_order_transaction` permits NULL; `PAID` is in the status
allow-list; `updated_at = now()` satisfies `ck_payment_order_time_order`;
revision becomes 4 (>= 1); `provider_transaction_id` is not one of the six
identity columns guarded by `guard_payment_order_identity`. The expected
`23514` + `ck_payment_order_paid_shape` assertion and the preceding valid
transition/guard evidence are unchanged.

**Plan/V004 alignment.** Plan 7.2 item 2 and plan 8.4 both specify the probe
as `status='PAID', provider_transaction_id=NULL` - the correction implements
exactly the frozen contract (plan section 8.4 row
`payment_order | ck_payment_order_paid_shape`).

## Finding 3 - Payment-order parent delete had no payment-event child

**Root cause.** `paymentOrderId` was inserted (pre-fix lines 9905-9907) but no
`payment_event` row referenced it before the parent-delete assertion at
pre-fix lines 10002-10003. `payment_order` has no append-only delete trigger,
so `DELETE FROM catalog_order.payment_order WHERE id = paymentOrderId`
succeeded (0 constraint violations); `expectSqlFailure` failed because no
23503 was raised. The separate missing-parent insert probe (pre-fix lines
9982-9990) uses a random nonexistent parent id and leaves no child, so it was
unaffected.

**Exact correction.** One method-local valid payment-event child is inserted
via the established `insertPaymentEvent` helper immediately after
`resetToMigrationIdentity(connection)` and before the parent-delete probes
(post-fix lines 9996-10002), with established valid values
(`provider='WECHAT'` helper literal, `TRANSACTION.SUCCESS`,
`signature_verified=false`, `payload_ciphertext='v1.s2.event.fkparent'`
matching `ck_payment_event_payload_ciphertext`, random 64-hex sha,
`transaction_id=NULL` permitted, `amount_cents=9900`,
`occurred_at=createdAt`). Deleting the payment order now fails with
`23503` + `fk_payment_event_order` as required. The whole transaction still
rolls back (post-fix lines 10088-10091), so the child is method-local fixture
hygiene; the missing-parent probe and all other parent-delete probes are
unchanged.

**Plan/V004 alignment.** Plan 7.2 item 3 lists `payment_order` (payment_event
child) among the parent-delete RESTRICT 23503 probes - the child fixture makes
that probe executable; FK `fk_payment_event_order` is `ON DELETE RESTRICT`
(frozen V004, plan 8.2).

## Finding 4 - Unlocked publication loser-row assertion was semantically wrong

**Root cause.** Both DRAFT versions are inserted and committed by the setup
transaction (pre-fix lines 10146-10165) before the race. The loser's UPDATE
fails on `uq_catalog_version_active` (23505) and its transaction rolls back
(pre-fix lines 10190-10197), so the pre-existing loser version row correctly
remains exactly as committed: `DRAFT`, `published_at IS NULL`, `revision 1`.
The assertion at pre-fix lines 10211-10213 wrongly expected that row's count
to be zero.

**Exact correction.** The zero-count assertion is replaced by a fail-closed
explicit-state assertion (post-fix lines 10220-10226): the loser row must
still exist and read exactly `DRAFT|false|1`
(`sale_status || '|' || (published_at IS NULL)::text || '|' || revision::text`
via `queryString`, which asserts exactly one row). The exactly-one-ACTIVE
winner assertion (post-fix lines 10216-10219) and the winner-identity
assertion on the loser connection (post-fix lines 10208-10211) are retained
verbatim. No loser fixture is deleted; the bounded lock-wait concurrency
protocol (two connections, distinct `application_name`,
`pg_stat_activity` blocking detection, bounded `statement_timeout`, loser
rollback before reading the winner) is untouched.

**Plan/V004 alignment.** Plan 7.2 item 4 and 8.5: "one commits, the loser
receives exact 23505 `uq_catalog_version_active`, rolls back ... final
exactly one ACTIVE". A rolled-back loser transaction must leave its committed
DRAFT row in place; the corrected assertion is the fail-closed expression of
that frozen outcome.

## Exact files and logical lines changed

Only `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
was written (four bounded edits). Logical lines (post-edit numbering):

| # | Region | Lines | Change |
| --- | --- | --- | --- |
| 1 | `ck_catalog_version_sale_status` probe | 9244-9248 | `published_at` argument `null` -> `OffsetDateTime.now(ZoneOffset.UTC)` |
| 2 | `ck_payment_order_paid_shape` runtime probe | 9541-9548 | SET clause adds `provider_transaction_id = NULL` |
| 3 | FK parent-delete section | 9994-10002 | new method-local valid `insertPaymentEvent` child after `resetToMigrationIdentity` |
| 4 | Unlocked publication verify block | 10215-10227 | zero-count loser assertion -> explicit `DRAFT|false\|1` state assertion + comment |

No other repository file was written. No test method, probe list, probe count
assertion, FK definition, lifecycle/guard assertion, append-only evidence,
RR-09 ACL assertion, concurrency protocol, migration, product source, CI,
README, controller or prior artifact was modified.

## Hashes

| Artifact | SHA-256 |
| --- | --- |
| `PostgresContainerIT.java` before this correction (fix-1 after state) | `b23efab5e47cefd683059dc03ab2bffd03b80a90a1647136da9ab6e0f94823ab` |
| `PostgresContainerIT.java` after this correction | `a7456380559357033c099c496310d46c443d04f0f6ef9263d940b547966f64a1` |
| `V004__catalog_order_and_payment.sql` (unchanged; required frozen value `A7BA7A8C...EDC`) | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` (unchanged) | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` (unchanged) | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` (unchanged) | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |

## Validation commands and honest results

Executable validation was attempted first and is harness-denied in this WSL
worktree (same environment as fix 1). Denials are recorded verbatim; none of
these commands ran to completion, so none is claimed as a pass.

| # | Command | Result |
| --- | --- | --- |
| 1 | `cd backend && ./mvnw.cmd -B -ntp -Pcontainer-smoke "-Dit.test=PostgresContainerIT#<four S2 methods>" verify` (controller's quoted-selector form) | Denied: "Command contains quoted characters in flag names" |
| 2 | `cd backend && ./mvnw.cmd -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT#<four S2 methods> verify` (unquoted-selector form) | Denied: "Command contains mid-word # which is parsed differently by shell-quote vs bash" |
| 3 | `./mvnw -B -ntp -f backend/pom.xml validate` | Denied: "This command requires approval" |
| 4 | `./mvnw -B -ntp -f backend/pom.xml clean verify` | Denied: "This command requires approval" |
| 5 | `./mvnw -B -ntp -Pcontainer-smoke -f backend/pom.xml verify` (expect Surefire 10/0/0/0, Failsafe 31/0/0/0) | Denied: "This command requires approval" |
| 6 | Focused four S2 methods (any invocation) | Not executed (Maven denied) |
| 7 | Repeat `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner` 3x | Not executed (Maven denied) |
| 8 | `git status --porcelain` | Failed: `fatal: not a git repository: .../codex-call/D:/codex-call/.git/worktrees/codex-call4` (broken worktree gitdir pointer; git unusable here) |
| 9 | `docker ps -a` | Ran. No Testcontainers/ryuk residue attributable to PA4 V004 (visible containers are pre-existing unrelated services: `pa3-pr11-review-pg`, `mgmt-*`, `dify-*`). |

Denial is NOT treated as a pass. The controller must rerun executable
validation (focused S2 methods, 3x concurrency repeat, `validate`,
`clean verify`, and `-Pcontainer-smoke verify` expecting Surefire 10/0/0/0 and
Failsafe 31/0/0/0).

### Static checks performed instead (all passed)

- Source `@Test` count: exactly 31 (grep-verified; the `void run()` at line
  ~4215 is the `SqlAction` functional interface, not a test).
- Source test method names compared one-by-one against the CI expected set in
  `.github/workflows/ci.yml` lines 78-108: exact 31/31 equality, including
  the four S2 methods `v004CatalogVersionLifecycleAndGuardBackstops`,
  `v004OrderPaymentLifecycleAndAppendOnlyBackstops`,
  `v004ForeignKeyAndAbsentConsentFkBackstops`,
  `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner`. No rename,
  removal, disable or assumption was introduced.
- Probe structure intact: `hasSize(22)` (catalog family, line 9348) +
  `hasSize(29)` (order/payment family, line 9794) = 51 named-check probes;
  probe-name sets still compared to `v004ExpectedCheckNames(...)`.
- `V004_FK_DEFS` still holds exactly 11 FK rows (lines 317-328); the 11-row
  definition/property map assertions, missing-parent probes, parent-delete
  probes and the `confdeltype='r'`/trigger-shadow backstop are unchanged.
- 51-probe target-isolation audit completed (see Finding 1): 50 isolated,
  1 (`ck_catalog_version_offer_type`) recorded as residual risk without
  change, per the controller's executable-evidence bound.
- V004 hash equals the required frozen value; V001-V003 hashes recorded and
  files untouched (only the one test class was written).
- Lifecycle/guard assertions, append-only evidence, RR-09 ACL hardening, the
  exact 31-method set and the bounded deterministic concurrency protocol are
  untouched apart from the four bounded edits above.
- No checkstyle/LineLength enforcement exists in `backend/pom.xml`; the edited
  lines introduce no style-gate risk.

## Residual risks

| Risk | Owner / destination |
| --- | --- |
| `ck_catalog_version_offer_type` probe also violates `ck_catalog_version_billing_matrix` on the same row; under PostgreSQL's alphabetical CHECK-name evaluation order (consistent with the controller's published_shape-over-sale_status observation) the exact-name assertion may fail once the earlier catalog probes pass. Not changed here: controller bound extra probe changes to executable evidence. A future correction cannot isolate this probe with a plain full-row INSERT (every value outside the offer_type allow-list also fails billing-matrix), so it needs a controller-directed probe-shape decision. | Controller: on rerun evidence, issue a bounded S2 correction with the probe-shape decision |
| Executable validation (focused S2, 3x concurrency, cumulative container-smoke) not run in this environment because Maven is harness-denied and git is broken | Controller: rerun `implementation`-gate validation; further fixes only on new evidence |
| Worktree gitdir pointer is broken (`D:/codex-call/.git/worktrees/codex-call4`), so author-side `git status/diff/commit` is impossible | Environment/controller ownership; not in this task's write scope |

## Stop statement

Author work stops here. No self-review, no commit, no push, no PR, no merge,
and no transition into review or any later gate. This artifact is
implementation evidence only.
