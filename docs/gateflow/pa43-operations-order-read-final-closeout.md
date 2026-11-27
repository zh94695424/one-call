# PA43 operations order read foundation — final closeout

## Work unit

- Work unit: `PA43-operations-order-read-foundation`
- Type: employee-facing read-only API feature
- Status: completed at `final closeout pass`
- Base: merged PA42 `main` at
  `29e715a3dccd8bc3fbedb4a094555294b7482a67`
- Branch: `codex/pa43-next-work-unit`
- Draft PR: https://github.com/zh94695424/one-call/pull/55
- PR title: `feat(PA43): add operations order read foundation`
- PR state at closeout: OPEN, Draft, base `main`

## What changed

- Added `GET /api/v1/operations/orders` / `listOperationsOrders` and
  `GET /api/v1/operations/orders/{orderId}` / `getOperationsOrder`.
- Added trusted employee-session authentication through the existing
  Operations request boundary, followed by current database authority for
  ACTIVE `ADMIN` or `OPS`.
- Added endpoint-isolated cursor pagination bound to the normalized
  `subjectId`/`paymentStatus` filter digest with stable
  `createdAt desc, orderId desc` ordering.
- Added exact `OrderPage`, 11-field `OrderSummary` and 16-field `Order`
  projections, immutable historical snapshot validation and current payment
  status.
- Added computed issuance status from the entitlement owner: `NONE`,
  `PENDING`, `ISSUED`, `FAILED`, `MIXED`.
- Reused the existing Operations read-only transaction, pool, role context,
  ORDER/TRIAL owner scopes and LO SET edges. No V021, migration, grant, role,
  membership, pool, index or contract change was introduced.
- Added the exact employee order GET pass-through in the global customer
  boundary filter, plus a bounded rejected-alias ERROR-dispatch family that
  emits generic 404 without concealing canonical GET application failures.
- Added exact source, census, mutation, least-privilege, no-write and EXPLAIN
  guards and the required CI/POM/README inventory updates.

## What was verified

- Focused order unit/web/boundary tests: 38 passed.
- Regression/guard suites: 177 passed.
- Focused PostgreSQL 16 ITs: 35 passed.
- Clean full `container-smoke` verify:
  - Surefire: 1202 tests
  - Failsafe: 775 tests
  - Total: 1977
  - Failures/errors/skips: 0/0/0
- Exact CI inventory: 130 unit suites / 1202 methods and 48 IT suites / 775
  methods; 1978 annotation lines; 48 POM Failsafe includes.
- Packaged-JAR web smoke, class/resource/migration parity and dependency
  firewall with 116 adversarial probes passed.
- PostgreSQL 16.16 proved least-privilege ACL behavior, no-write inventories,
  ORDER/TRIAL role and pool restoration, concurrent snapshot visibility,
  list-500/detail-404 corruption split, and 20 prepared-query EXPLAIN plans.
- All 36 frozen migration/OpenAPI/design/PA25 files and unchanged customer
  sources were byte-checked; V001–V020, all OpenAPI files, docs12/14/15/17,
  PA25 and the accepted historical guards remain frozen.

## Hosted CI

- Reviewed product/test head:
  `222aa6f6b1952939225707e454f14ff9ca106dab`
- `contract-regressions`: SUCCESS in 33 seconds
- `java-runtime`: SUCCESS in 1 hour 25 minutes 19 seconds
- The later controller-only review artifact/Control commit changes documentation
  only; no hosted run was reported for the resulting docs-only head. This
  closeout therefore claims hosted success for the reviewed product/test bytes,
  not a newly executed final-docs commit.

## Documentation

- Accepted plan:
  `docs/gateflow/pa43-operations-order-read-plan.md`
- Implementation evidence:
  `docs/gateflow/pa43-operations-order-read-s1-implementation.md`
- Plan, code, aggregate and PR review artifacts are committed under
  `docs/reviews/`.
- `backend/README.md` records the current PA43 capability and measured
  validation inventory.

## Finding status

- All accepted implementation and aggregate-review findings are resolved and
  re-reviewed.
- Deferred architecture work:
  - issuance-status and operations route-table ownership consolidation;
  - customer/operations duplicate route literals.
  Owner/destination: Backend/Architecture, later hardening work unit.
- Deferred test-maintenance work:
  - remaining budget checkpoints and SQL predicate mutation coverage;
  - exact zero-padded limit assertions;
  - shared-fate sibling test delegation and global inventory coupling;
  - duplicated ERROR fixture and R2b real-container attribution.
  Owner/destination: Backend/Test Maintenance, later hardening work unit.
- Deferred production-capacity work:
  - keyset/capacity assessment for unindexed list shapes and issuance fan-out.
  Owner/destination: Backend/Database, separate migration/performance slice
  before production enablement.
- Existing security/operations boundaries:
  - LO SET-role amplification and non-linearizable authority snapshots;
  - outside-family TRACE sanitized-500 carve-out;
  - cursor key custody/rotation, TLS, production provisioning and live
    validation.
  Owners/destinations: Security/Database, Security/Backend,
  Backend/Security, Security/Operations/Platform.

## Completion

No blocking open question remains for the PA43 slice. No merge, approval,
mark-ready action, reviewer request, external comment or issue mutation was
performed by the Phaseflow controller.

After the user merges PR #55, fetch the updated `main`, confirm the merge
commit, create a fresh managed worktree and `codex/` branch from that merge
commit, reread this Control, and perform a new Phaseflow goal confirmation for
the next user-approved ONECALL work unit. This unmerged branch and PA43 goal
authorization must not be reused for that next work unit.
