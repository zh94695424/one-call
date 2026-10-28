# PA5 V005 IVR task/session/call foundation — S2 fix-1

| Field | Value |
| --- | --- |
| Task label | `pa5-v005-s2-fix1-mimo26flash-20261028-214500` |
| Fix author / provider | MiMo-V2.6-Flash (mimo-v2.6-flash) |
| Gate | code-review fix only (controller-accepted findings F-PA5-S2-01/02/03) |
| Date | 2026-10-28 |
| Accepted S1 checkpoint | `02cb35494c6fcbaa8027003d83949ec2c30f211b` |
| Frozen V005 SHA-256 | `49f7c7500f2a09de801289ac4e33fe80c3228f076bb69e1874bd778d32683386` (re-verified, unchanged) |
| Historical S2 artifact (unmodified) | `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s2-implementation.md` SHA-256 `d97c7ad81505c99fc5f2de5b75826ce244d9e617cecab9d0b10e19dcaaf56e47` |
| Private backend | `/tmp/pa5-s2fix-build.aS3FPv/backend` (fresh rsync of workspace `backend/` without `target/`) |
| Run directory | `/tmp/sub-agents.pa5-s2fix.9EXjF4` |

Binding sources consulted (read-only): `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md` §6.3 (items 3–4) and §7.1; `docs/14-production-data-model-and-flyway-schema-spec.md` §4.23–4.28, §5.4, §9.11–9.16, §9.21–9.24, §10.12–10.17, §11.4; current control doc. No plan, spec, control, migration, CI, or README edit was required or made by this fix.

## 1. Changed paths (this fix)

| Path | Change |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | the only file modified; post-fix SHA-256 `097e45c7bd5ccba9ccde3f60ac1d74b178703ab228cf6f0f940570381b0415f9` |
| `backend/README.md` | **not modified** — still SHA-256 `fa989fb67fb05816ceae700352c30d4083f6eb65601848e886bb55315a5c4449` (matches the S2 artifact record) |
| `.github/workflows/ci.yml` | **not modified** — expected method set/count/labels need no semantic change (still 38) |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s2-fix-1.md` | new artifact (this file) |

No commit, stage, push, PR, approval, merge, or other gate was performed.

## 2. Findings fixed

### F-PA5-S2-01 (A/F-01) — real schema-level zero-table assertion

`assertV005ForeignKeyAndDeferredReservationFkBackstops` previously filtered
`information_schema.tables` with `table_schema = 'ivr' AND table_name IN ('entitlement_metering','activation_consent')`.
That predicate can never match: the V006 tables would live in their own
`entitlement_metering` / `activation_consent` schemas (created empty earlier), never inside
`ivr`, so the count was trivially zero and proved nothing.

Fixed at `PostgresContainerIT.java:14329–14337`: replaced with the schema-level
`pg_class`/`pg_namespace` zero-table assertion

```sql
SELECT count(*) FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname IN ('entitlement_metering','activation_consent')
   AND c.relkind = 'r'
```

— identical in meaning to the existing `assertNoV006PlusObjects` check, now executed inside
the FK backstop method itself. No V006 objects or production behavior were added.

Attribution: `backend/README.md` line 65 attributes "无 entitlement/activation 表" to this
method. That claim was not truthfully proven by the old query; after this fix the method
itself proves it at schema level, so the README attribution became truthful with no README
edit. The historical S2 implementation artifact (§ method table, same claim) was not modified
per instructions; its attribution is likewise made truthful by this fix.

### F-PA5-S2-02 (A/F-02) — isolated missing-parent probes for the two decision FKs

The `fk_ivr_decision_session` probe previously passed a nonexistent session **and** a
nonexistent call, so either decision FK could have been reported.

Fixed at `PostgresContainerIT.java:14267–14281`: the probe now uses the already-inserted
fixture `call_id` and only a nonexistent `session_id`, so exactly `fk_ivr_decision_session`
can fire. The separate `fk_ivr_decision_call` probe (existing `sessionId`, `"missing-call-" +
UUID.randomUUID()`) is unchanged. Both keep `expectSqlFailure(connection, "23503", <exact
constraint name>, …)` — exact SQLSTATE and exact constraint name for each.

### F-PA5-S2-03 (B/F-B-01) — executable exact 23505 + `uq_ivr_decision_source` probe

Plan §6.3 item 3 requires duplicate `(call_id, source_event_domain, source_event_id)` →
`23505`, but the frozen `ck_ivr_decision_source` (binds source tuple to purpose) and
`uq_ivr_decision_call_purpose` (preempts same-purpose duplicates on the shared call) make a
solo `uq_ivr_decision_source` violation unreachable while unisolated.

Fixed with the established bounded owner savepoint-isolation technique (same pattern as
`expectProfileStatusViolationWithIsolatedConstraint` /
`expectCatalogVersionOfferTypeViolationWithIsolatedConstraint` /
`expectCallLegTypeViolationWithIsolatedConstraint`):

- New helper `expectDecisionSourceUniqueViolationWithIsolatedConstraint`
  (`PostgresContainerIT.java:3786–3857`), called from
  `assertV005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops`
  (`PostgresContainerIT.java:13781–13785`), immediately after the existing
  `uq_ivr_decision_call_purpose` `23505` and `ck_ivr_decision_source` `23514` probes, which
  are **not weakened or removed**.
- Inside a savepoint, as `ivr_owner`: `ALTER TABLE ivr.ivr_decision DROP CONSTRAINT
  ck_ivr_decision_source` and `ALTER TABLE ivr.ivr_decision DROP CONSTRAINT
  uq_ivr_decision_call_purpose` (the minimal drops needed to construct the two rows; the
  partial unique index `uq_ivr_decision_source` itself is never dropped).
- As `ivr_role`: insert a legal `INBOUND_RESOLVE` row, then a legal `ACTIVATION_NEXT_ACTION`
  row with the same non-null `('IVR_CONFIRM', evt-source-unique-…)` source tuple; assert exact
  `23505` and exact `uq_ivr_decision_source` via `expectSqlFailure` (nested savepoint).
- `ROLLBACK TO SAVEPOINT`; assert `ck_ivr_decision_source` restored (pg_constraint count = 1),
  `uq_ivr_decision_call_purpose` restored (`to_regclass` non-null), and
  `uq_ivr_decision_source` still present; return to `ivr_role`.
- All DDL is transactional savepoint work; the committed schema, migration, and catalog are
  untouched. PostgreSQL isolated the unique index without mutating the committed schema, so
  the stop-condition escape was not needed.

## 3. Failed attempts (recorded)

1. **First full-container run (21:57)** — helper dropped only `ck_ivr_decision_source` and
   kept `uq_ivr_decision_call_purpose`, assuming distinct purposes made it harmless. The
   first `INBOUND_RESOLVE` probe row collided with the method's pre-existing
   `inboundDecisionId` row on the shared `call_id`:
   `ERROR: duplicate key value violates unique constraint "uq_ivr_decision_call_purpose"
   Detail: Key (call_id, purpose)=(pa5-s2-call-…, INBOUND_RESOLVE) already exists.` —
   38 run, 1 error. Resolution: also drop `uq_ivr_decision_call_purpose` inside the savepoint
   (controller-sanctioned "as needed"), with post-rollback restore assertion for it.
2. **Second full-container run (21:58)** — `DROP INDEX ivr.uq_ivr_decision_call_purpose`
   failed: `cannot drop index ivr.uq_ivr_decision_call_purpose because constraint
   uq_ivr_decision_call_purpose on table ivr.ivr_decision requires it`. — 38 run, 1 error.
   Resolution: `ALTER TABLE ivr.ivr_decision DROP CONSTRAINT
   uq_ivr_decision_call_purpose` (it is a constraint-backed unique index).
3. No other attempt failed; no assertion was weakened to obtain green.

## 4. Private validation matrix (post-fix tree)

| Check | Result |
| --- | --- |
| Default `mvn clean verify` (no profile) | `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS |
| Full `-Pcontainer-smoke verify` (PG 16.15 Testcontainers) | `Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS (final run 21:59) |
| Focused four behavioral methods (`v005TaskTtl…`, `v005SessionCall…`, `v005LegEvent…`, `v005ForeignKey…`) | `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` |
| `concurrentV005TaskClaimAndActiveCallHasOneWinner` ×3 independent runs | each `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS |
| source = CI = XML method sets | 38 = 38 = 38, `EQUAL: True` (source `@Test` names; `ci.yml` `expected` set; Failsafe `TEST-*.xml` testcase names) |
| Boot smoke (repackaged fatjar) | `Started OneCallApplication` matched, process exit 0 |
| Production fat-jar firewall | 27 `BOOT-INF/lib` entries; zero matches for spring-security/flyway/jooq/HikariCP/postgresql/testcontainers |
| V005 frozen hash | `49f7c7500f2a09de801289ac4e33fe80c3228f076bb69e1874bd778d32683386` — unchanged before/after |
| V001–V004 blob immutability | all five migration files `sha256sum -c` OK against pre-run manifest (`/tmp/sub-agents.pa5-s2fix.9EXjF4/migration-hashes-before.txt`) |
| `git diff --check` (workspace, work-tree mode) | exit 0 |
| Test-method count invariants | 38 `@Test` methods; 0 `@Disabled`; 0 `Assumptions.`; no sleep-only assertions added |

All 33 prior methods and the five S2 methods are preserved; the 73 CHECK probe-set equality
and 15-FK probe-set equality run inside the green suite unchanged; all positive controls,
ACL/guard/concurrency assertions, and V001–V005 immutability invariants hold.

## 5. Residual risks and blockers

- The `uq_ivr_decision_source` behavioral probe inherently depends on transactional DDL
  (savepoint drop/rollback) as `ivr_owner`. This is the same bounded isolation technique the
  suite already uses for subsumed CHECKs; if a future reviewer rejects DDL-in-savepoint
  isolation entirely, the only alternatives are weakening the probe (forbidden) or a frozen
  V005 redesign (out of scope).
- PostgreSQL-specific: relies on constraint/index DDL being transactional. Acceptable — the
  whole suite is PostgreSQL 16.x-only by design.
- No open blocker. Controller adjudication of this fix (plus re-review A/B) remains the next
  owner; this author does not self-accept.
