# PA38 Operations Catalog Sale Lifecycle — Plan Fix 1

**Task label:** `pa38-operations-catalog-sale-lifecycle-plan-fix1-gpt-20261118-01`  
**Cumulative plan:** `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan.md`  
**Scope:** bounded closure of accepted Controller findings `PA38-PLAN-REV-01..13`; no source, test, CI, POM, OpenAPI, migration, docs, Control, or review-artifact edit.

## Closure matrix

| Finding | Closure in cumulative plan | Verification anchor |
| --- | --- | --- |
| `PA38-PLAN-REV-01` | Moves `OperationsCatalogSaleCommand` and `EmployeeAuthorityReadPort` into `operations_iam` named interfaces; `catalog_order` implements/consumes them; preserves `operations_iam -> operations_audit`; forbids an `operations_iam -> catalog_order` edge and explicitly enumerates boundary oracle edits. | §3, §4.1–4.2, §10.2 |
| `PA38-PLAN-REV-02` | Defines the `operations_iam`-owned lock-free authority reader, exact `operations_iam_role` projection, same-connection transaction, no `FOR UPDATE`/admin lock, exact existing datasource family and role membership assertions. | §2.3, §3, §6.1–6.2, §9.2 |
| `PA38-PLAN-REV-03` | Separates stale expectations (`PUBLICATION_STALE_VIEW`) from non-matchable state/race conflict (`CATALOG_PUBLICATION_CONFLICT`) in both complete matrices; no stale-inside-conflict wording remains. | §1, §7.1–7.2 |
| `PA38-PLAN-REV-04` | Adds an explicit read/idempotent-subject/state-conditional-catalog classifier; catalog routes require no key; subject key rule and all non-read media checks remain exact and independent. | §5.1, §9.1 `classifiesReadsSubjectsAndCatalogCommandsSeparately` |
| `PA38-PLAN-REV-05` | Orders authentication, structural path/body/media validation, then database ACTIVE/ADMIN witness; removes authority-before-input; names `authenticationPrecedesStructuralParsing` and `structuralValidationPrecedesAuthorityWitness`. | §5.2, §9.1 |
| `PA38-PLAN-REV-06` | Authorizes and enumerates `ModuleBoundaryTests`, `EvidenceRoleIvrAssociationAclBoundaryTests`, all PA38 source-subtraction/union sets, line/method/class/include totals, manifest/digest carrier, diagnostics, and historical PA-start preservation. | §10.1–10.3 |
| `PA38-PLAN-REV-07` | Defines `expectedRevision` as target-row current revision; null active expectation is syntax-only against nonexistent ACTIVE; exact-current replay, stale, conflict, no-write, and pre-transition lost-response behavior are fully tabulated for publication and offsale. | §7.1–7.2, §9.2 |
| `PA38-PLAN-REV-08` | Freezes one candidate publish audit, old-retirement plus candidate-publish Outbox facts when applicable, one real-offsale audit/Outbox, zero replay facts, all 11 audit arguments, payload fields, actor/object/request/trace, and decision timestamp. | §8.1–8.2, §9.2 |
| `PA38-PLAN-REV-09` | Reconciles 11 production, 2 modified production, 10 test paths, package owners, exact FQCN/method identities, 8 Surefire classes/86 tests, 2 Failsafe classes/38 tests, focused selector, and safe V019 glob check. | §2.2, §4, §9, §11 |
| `PA38-PLAN-REV-10` | Gives PA38 PostgreSQL tests a disposable login/isolated fixture and exact `operations_iam_role`/`catalog_order_role` memberships with `ADMIN FALSE`, `INHERIT FALSE`, `SET TRUE`; preserves PA32 assertions. | §2.3, §9.2 |
| `PA38-PLAN-REV-11` | Names `OperationsLifecyclePersistenceConfiguration.java` as existing owner, reuses exact qualified beans without editing that file, and forbids a second pool/credential family or unlisted refactor. | §2.3, §4.2, §6.1 |
| `PA38-PLAN-REV-12` | Pins disabled employee to `403 FORBIDDEN`, `Accept: text/html` to shipped sanitized `500 INTERNAL`, success/problem headers and `Content-Length`, and sensitive leak-canary assertions. | §5.2, §9.1 |
| `PA38-PLAN-REV-13` | Defines rollback-first fresh reread on the same command pool with exact role entry, restoration before commit, connection eviction on unproven restoration, and one PostgreSQL `clock_timestamp()` after lock acquisition. | §6.1, §7, §8, §9.2 |

## Frozen implementation ledger

- **Routes/slice:** exactly two POST routes and one `PA38-S1` slice; no goal expansion.
- **Production inventory:** current `279` Java / final `290`; `11` new production files and `2` modified production files are enumerated in cumulative-plan §4.
- **Test inventory:** current `169` Java / final `179`; `8` new Surefire classes / `86` tests and `2` new Failsafe classes / `38` tests are enumerated in cumulative-plan §9.
- **Final oracle:** `97/934` Surefire, `38/630` Failsafe, `135/1,564` focused identities, `38` POM includes, and CI `@Test` line scan `1,565`.
- **Database boundary:** V001–V018 only; no V019, no ACL/role/grant/schema change. The PA38 fixture is disposable test-only setup.
- **Datasource boundary:** existing `OperationsLifecyclePersistenceConfiguration.java` bean family and physical pool only; writable `REQUIRES_NEW`/`READ_COMMITTED` binding; no second credentials or pool.
- **Authority boundary:** exact `operations_iam_role` status/role/revision read, lock-free and same connection; catalog role never reads employee account.
- **Durability boundary:** state, one/two Outbox facts, and one audit fact as tabulated; all rollback together; replay is zero-write/zero-fact.
- **Evidence status:** this fix artifact and cumulative plan are planned evidence only. Maven, Docker, PostgreSQL16, CI, and re-review have not been run in this task.

## Open questions and next gate

Blocking open questions: **none**. Any implementation discovery requiring a new public/schema/ACL behavior must stop and return to Controller goal confirmation rather than alter this plan. The next gate is **fresh independent A/B plan re-review**.
