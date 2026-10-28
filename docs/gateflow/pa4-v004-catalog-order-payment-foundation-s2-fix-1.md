# PA4 V004 S2 runtime correction 1

- Task label: `pa4-v004-s2-runtime-fix1-mimo26flash-20261028-01`
- Provider: MiMo 2.6 Flash
- Work unit: `PA4-v004-catalog-order-payment-foundation`
- Gate/slice: `implementation` / S2 only (runtime fixture correction; not review, not a new slice)

## Confirmed root cause

Controller evidence (Failsafe XML `tests=31, errors=4, failures=0, skipped=0`) showed all four
S2 methods erroring at their first call to `insertCatalogVersion`
(`PostgresContainerIT.java:8965` → `executeUpdate:2719`) with:

```
ERROR: column "snapshot" is of type jsonb but expression is of type character varying
  建议：You will need to rewrite or cast the expression.
  位置：311
```

Root cause: the S2 fixture helpers bound the JSON snapshot argument as a plain JDBC
`String` (`bind()` uses `prepared.setObject(...)`, which sends a `character varying`
parameter) into a `jsonb` column with no explicit cast. PostgreSQL 16 therefore rejects
the INSERT with SQLSTATE 42804 before any row is written, so none of the four S2 test
bodies could exercise the frozen V004 contract.

Type-correct correction: the file's established, already-used pattern for this exact
binding problem (`CAST(? AS jsonb)`, present since S1 at lines 2541, 2568, 2684, 4338,
4368, 4384) was applied to the snapshot placeholder in the four S2 fixture/probe SQL
templates that pass snapshot as a JDBC parameter:

1. `insertCatalogVersion` — 12th placeholder (snapshot) wrapped in `CAST(? AS jsonb)`.
2. `insertCustomerOrder` — 12th placeholder (snapshot) wrapped in `CAST(? AS jsonb)`.
   (The helper's VALUES has a `'WEB'` literal for `channel`, so snapshot is the 12th
   JDBC parameter.)
3. `versionProbeSql()` — 12th placeholder (snapshot) wrapped in `CAST(? AS jsonb)`.
4. `customerOrderProbeSql()` — 13th placeholder (snapshot) wrapped in `CAST(? AS jsonb)`
   (all 21 columns are parameters here; snapshot is column/parameter 13).

`CAST(? AS jsonb)` is the type-correct expression PostgreSQL accepts for a varchar-bound
parameter into a jsonb column: the driver still binds a String, the server casts it, and
valid JSON documents insert as jsonb. Every snapshot value passed by the tests is a valid
JSON document (`{"plan":"pa4-s2"}`, `{"line":1}`, `[]`, etc.).

Sites intentionally NOT changed (already type-correct or unaffected):

- `expectSqlFailure` probes that embed the snapshot as a SQL string literal
  (`'{\"p\":9}'`, `'{\"line\":1}'`, `'{"tampered":true}'::jsonb` at lines 9133-9134,
  9434-9441, 9911-9926, 10104-10110) — unknown-typed literals are coerced to jsonb by
  PostgreSQL; no cast needed.
- All S1 `platform.idempotency_complete` / `platform.outbox_append` /
  `audit.append_fact` call helpers — already used `CAST(? AS jsonb)`.
- `insertCatalogProduct`, `insertCatalogOffer`, `insertCatalogBenefitLine`,
  `insertPaymentOrder`, `paymentEventProbeSql` — no jsonb columns.

Note on the check probes: `ck_catalog_version_snapshot_type` and
`ck_customer_order_snapshot_type` deliberately pass `"[]"`. With the cast, `"[]"`
becomes a valid jsonb array and the failure correctly surfaces from the frozen
CHECK `jsonb_typeof(snapshot) = 'object'` (expected 23514 + exact constraint name),
which is the intended probe behavior. The cast does not mask any probe.

## Exact files and logical lines changed

Only `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
was written. Logical lines (post-edit numbering):

| Helper | Line | Change |
| --- | --- | --- |
| `insertCatalogVersion` | 8971 | `VALUES (...)` 12th placeholder → `CAST(? AS jsonb)` |
| `insertCustomerOrder` | 9026 | `VALUES (...)` 12th placeholder → `CAST(? AS jsonb)` |
| `versionProbeSql` | 9380 | `VALUES (...)` 12th placeholder → `CAST(? AS jsonb)` |
| `customerOrderProbeSql` | 9832 | `VALUES (...)` 13th placeholder → `CAST(? AS jsonb)` |

After correction the VALUES clauses read:

- `insertCatalogVersion` (16 placeholders):
  `VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)`
- `insertCustomerOrder` (20 placeholders, `'WEB'` literal for channel):
  `VALUES (?, ?, ?, ?, ?, 'WEB', ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)`
- `versionProbeSql` (16 placeholders): same 16-arity form as `insertCatalogVersion`
- `customerOrderProbeSql` (21 placeholders):
  `VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)`

Placeholder counts were re-verified against the bound-argument lists of each call site
(16 / 20 / 16 / 21; CAST sits on the snapshot argument in every case).

No other repository file was written. No test method, assertion, probe list,
concurrency protocol, migration, product source, CI or README file was modified.

## Hashes

| Artifact | SHA-256 |
| --- | --- |
| `PostgresContainerIT.java` before correction | `a7f8e5bf67301a753c3cb480720cb0aa07cb539fc5e4a688659eba003060a060` |
| `PostgresContainerIT.java` after correction | `b23efab5e47cefd683059dc03ab2bffd03b80a90a1647136da9ab6e0f94823ab` |
| `V004__catalog_order_and_payment.sql` (unchanged; required frozen value `A7BA7A8C...EDC`) | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` (unchanged) | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` (unchanged) | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` (unchanged) | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |

## Validation commands and honest results

Executable validation was attempted first and is harness-denied in this WSL worktree.
Denials are recorded verbatim; none of these commands ran to completion, so none is
claimed as a pass.

| # | Command | Result |
| --- | --- | --- |
| 1 | `cd backend && ./mvnw -B -ntp -Dit.test=PostgresContainerIT#<four S2 methods> verify` | Not run: harness rejected the `#`-containing selector ("Command contains mid-word # which is parsed differently by shell-quote vs bash"); the quoted-selector retry was rejected ("Command contains quoted characters in flag names"). |
| 2 | `cd backend && ./mvnw -B -ntp test-compile` | Denied: "This Bash command contains multiple operations. The following part requires approval: ./mvnw -B -ntp test-compile" |
| 3 | `./mvnw -B -ntp -f backend/pom.xml test-compile` | Denied: "This command requires approval" |
| 4 | Focused four S2 methods (any invocation) | Not executed (Maven denied) |
| 5 | Repeat `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner` 3× | Not executed (Maven denied) |
| 6 | `./mvnw -B -ntp validate` | Denied: "This command requires approval" |
| 7 | `./mvnw -B -ntp clean verify` | Denied: "This command requires approval" |
| 8 | `./mvnw -B -ntp -Pcontainer-smoke verify` (expect Surefire 10/0/0/0, Failsafe 31/0/0/0) | Denied: "This command requires approval" |
| 9 | `git status --porcelain` / `git diff --stat` | Failed: `fatal: not a git repository: /mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call/D:/codex-call/.git/worktrees/codex-call4` (broken worktree gitdir pointer; git unusable here) |
| 10 | `docker ps -a` | Ran. No Testcontainers/ryuk residue attributable to PA4 V004 (visible containers are pre-existing unrelated services: `pa3-pr11-review-pg`, `mgmt-*`, `dify-*`). |

Denial is NOT treated as a pass. The controller must rerun executable validation
(focused S2 methods, 3× concurrency repeat, `validate`, `clean verify`, and
`-Pcontainer-smoke verify` expecting Surefire 10/0/0/0 and Failsafe 31/0/0/0).

### Static checks performed instead (all passed)

- Source `@Test` count: exactly 31 (`grep`-verified; the `void run()` at line 4215 is the
  `SqlAction` functional interface, not a test).
- Source test method names compared one-by-one against the CI expected set in
  `.github/workflows/ci.yml` lines 78-108: exact 31/31 equality, including the four S2
  methods `v004CatalogVersionLifecycleAndGuardBackstops`,
  `v004OrderPaymentLifecycleAndAppendOnlyBackstops`,
  `v004ForeignKeyAndAbsentConsentFkBackstops`,
  `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner`. No rename, removal,
  disable or assumption was introduced.
- Placeholder/parameter arity re-checked for all four edited SQL templates
  (16/20/16/21); CAST position matches the snapshot argument in each.
- V004 hash equals the required frozen value; V001-V003 hashes recorded and files
  untouched (only the one test class was written).
- 51 named-check probes, 11 FK backstops, lifecycle/guard assertions, append-only
  evidence, RR-09 ACL hardening and the bounded deterministic concurrency protocol are
  untouched: the diff is confined to four SQL string templates.
- No checkstyle/LineLength enforcement exists in `backend/pom.xml`, so the slightly
  longer VALUES lines introduce no style-gate risk.

## Remaining failures

None observable beyond the blocked executable run. The known 42804 fixture errors are
addressed at the SQL-binding level; confirmation that all 31 tests now pass requires the
controller's rerun.

## Residual risks

| Risk | Owner / destination |
| --- | --- |
| Executable validation (focused S2, 3× concurrency, cumulative container-smoke) not yet run in this environment because Maven is harness-denied and git is broken | Controller: rerun `implementation`-gate validation; fix 2 only if new evidence appears |
| If a later runtime failure surfaces in S2 bodies past the fixture insert, it is a new defect requiring its own bounded correction | Controller → future S2 fix task |
| Worktree gitdir pointer is broken (`D:/codex-call/.git/worktrees/codex-call4`), so author-side `git status/diff/commit` is impossible | Environment/controller ownership; not in this task's write scope |

## Stop statement

Author work stops here. No self-review, no commit, no push, no PR, no merge, and no
transition into review or any later gate. This artifact is implementation evidence only.
