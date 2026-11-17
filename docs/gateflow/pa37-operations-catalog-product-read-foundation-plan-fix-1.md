# PA37 Operations Catalog Product Read Foundation — Plan Fix 1

Task label: `pa37-operations-catalog-product-read-plan-fix1-gpt-20261117-01`
Date: 2026-11-17 (Asia/Shanghai)
Scope: plan text only; implementation and later gates remain unauthorized.

## 1. Before/after artifacts

- Cumulative plan before fix: the pre-edit file was an uncommitted worktree artifact;
  the worktree's Git metadata points to an unavailable Windows path and `HEAD` does
  not contain this path, so a trustworthy pre-edit SHA-256 cannot be recovered.
  This limitation is recorded rather than inventing a hash.
- Cumulative plan after fix, canonical LF bytes:
  `docs/gateflow/pa37-operations-catalog-product-read-foundation-plan.md`
  SHA-256: `8758c2b6ba7f0d79640786c3e0e5d28ef414722336f4eb3cfdc8d8980c51f800`
  (18,348 bytes; 251 lines at fix-artifact creation).
- This fix artifact is new, so it has no before hash. Its canonical LF SHA-256 and
  line/byte count are reported in the final completion report after writing it; the
  hash is not embedded recursively in its own contents.

## 2. Accepted finding closure map

| Finding | Closure in cumulative plan |
|---|---|
| `PA37-PLAN-REV-01` | §5 adds `EvidenceRoleIvrAssociationAclBoundaryTests.java` to the exact allowlist; permits only the full-tree manifest count/digest update and exact CI current-literal mirrors. It pins 291→299 for exactly eight production files, preserves universal production coverage, and forbids exclusions/co-location. |
| `PA37-PLAN-REV-02` | §5 enumerates all eight production files, ten test files, six unit and two container identity dictionaries, every new test method, helper exclusion, current/final class/method/annotation/POM/CI/manifest values, historical `pa37_start_*` forms, and exactly two Failsafe include lines. |
| `PA37-PLAN-REV-03` | §§1 and 3 prohibit all `catalog_offer` reads. They state that `x-onecall-a0-models` is inventory-only, offer-less DRAFTs remain, multi-channel offers do not alter membership, and docs15 offer-summary wording is a Contract/Data + Catalog-order-owner residual. |
| `PA37-PLAN-REV-04` | §3 specifies two-phase root-bounded paging: `catalog_product` roots with `COLLATE "C"`, exclusive `(code,id)` anchor, `LIMIT limit+1`, then versions for exactly returned IDs in `product_id,version_no,id` order. It defines zero-version roots and boundary/no-duplicate tests. |
| `PA37-PLAN-REV-05` | §3 records the actual V004 matrix (SELECT/INSERT on seven owner tables, selected UPDATE, EXECUTE, USAGE), requires unchanged-state and structural approved-projection assertions, attributes mutation denial to read-only/trigger behavior, and records the broad role as `deferred-with-owner`. |
| `PA37-PLAN-REV-06` | §3 pins `COLLATE "C"` in both SQL order and anchor predicate, specifies matching Java bytewise semantics, and uses reachable `AB`, `A-1`, `A_1`, `A0` fixtures. |
| `PA37-PLAN-REV-07` | §2 makes authority first inside the transaction, specifies all precedence outcomes, blank cursor 400, detail query 422, ring×authority rows, preserved 429 mapping, and an explicit no-rate-limiter rule. |
| `PA37-PLAN-REV-08` | §6 replaces ad hoc contract commands with `cd api/openapi && npm ci && npm run check:s3`; focused Maven commands include `-DfailIfNoTests=true` and Failsafe includes `-Dfailsafe.failIfNoSpecifiedTests=true`. |
| `PA37-PLAN-REV-09` | §§2, 4, and 6 pin declared `publishedAt`/DRAFT null, QUOTE null price, CNY validation, reachable inconsistency tests, exact cursor framing/max 512, version/benefit ordering, and exact authority projection. |

## 3. Exact inventory decisions

The plan intentionally forecasts the following implementation, with no deferred
identity decisions:

- Current: unit 83/788, container 34/556, total 117/1,344, annotation scan 1,345,
  scoped POM includes 34, global Failsafe includes 36, production manifest 291.
- Final: unit 89/848, container 36/592, total 125/1,440, annotation scan 1,441,
  scoped POM includes 36, global Failsafe includes 38, production manifest 299.
- New test method additions are unit `8+8+18+10+8+8=60` and container `20+16=36`.
  The two helper/provider files have zero tests and are absent from identity maps.
- The exact method names are in cumulative-plan §5; CI must reproduce those names,
  exact source class equality, and exact Surefire/Failsafe XML inventories.
- The only new POM include strings are
  `**/OperationsCatalogProductReadsIT.java` and
  `**/OperationsCatalogProductReadRoleLeastPrivilegeIT.java`.
- Historical preservation uses `pa37_start_totals == [788, 556]` and
  `pa37_start_line_scan_total == 1345`; current literals become `[848,592]`, `1441`,
  unit/container class counts `89/36`, and include length `36`.

## 4. Residual owners retained

- Contract/Data + Catalog-order owner: docs15 offer-summary wording without a frozen
  public field.
- Database/Security + Backend: pre-existing broad `catalog_order_role`.
- Capacity/Database + Catalog-order owner: unbounded per-product version arrays and
  cursor capacity.
- Platform/Security + Backend/Database: collation, timeout/dependency/key delivery.
- CI/release: exact inventory, packaged-JAR, PostgreSQL16, and hosted execution evidence.
- Operations IAM/Backend: authority precedence and no rate limiter.

No source, test, POM, CI, migration, OpenAPI, control, review, or product document
other than the two permitted Markdown outputs was edited. This artifact does not
accept the plan or authorize implementation, review actions, or any later gate.
