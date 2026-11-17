# PA37 Operations Catalog Product Read Foundation — Final Closeout

Work unit: `PA37-operations-catalog-product-read-foundation`

Gate: `final closeout`

Status: **final closeout pass**

Date: 2026-11-18 Asia/Shanghai

## Delivered behavior

PA37 adds exactly two authenticated Operations read endpoints:

- `GET /api/v1/operations/catalog/products`
- `GET /api/v1/operations/catalog/products/{productId}`

Both reuse the existing opaque employee session and shared Operations read
runtime. A currently `ACTIVE`, server-derived `ADMIN|OPS` employee is required
before request parsing, cursor-ring/provider resolution or catalog SQL. Reads run
in one shared read-only `REPEATABLE_READ` transaction with the existing timeout,
role, connection and cleanup assertions.

The catalog-owner adapter runs under `catalog_order_role` and reads only approved
columns from:

- `catalog_order.catalog_product`
- `catalog_order.catalog_version`
- `catalog_order.catalog_benefit_line`

It does not read `catalog_offer`, snapshots, payment/order internals, credentials,
media or unapproved draft content.

List reads use C-collated roots with `limit+1`, discard the lookahead before phase
two, and load versions only for emitted roots. Cursors are endpoint-bound
AES-GCM values over the existing key ring; the maximum encoded cursor is 219
characters, below the 512-character input bound. Detail reads return the frozen
version/benefit projection with deterministic ordering and declared JSON nulls.

## Verification

Final product and evidence identities:

- 24 product paths
- 441,648 canonical bytes
- canonical manifest
  `045473e8781e8d6c99ba987e8dfe550ba0a2993ef7b57f71486001e7fff1038b`
- 299-file guarded production manifest
  `12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1`
- accepted plan
  `1fb2216b4046b0ca21ad03643fcc6a4a0d5037e912e7feb60d266df390812486`
- implementation report SHA256
  `3d82a76047ae5097bbd6a84028aff844fcc63bb6408625f354b82f2b1be9950e`

Executed validation at the final bytes:

- PA37 focused six classes plus lockstep guard: **68/68**
- Final ordinary Surefire: **89 classes / 848 methods**
- Final whole-tree LF full Failsafe: **36 classes / 592 methods**
- Final-byte PA37 PostgreSQL 16.15 Failsafe: **36/36**
- All runs: zero failures, errors and skips
- OpenAPI `check:s3`: PASS

GitHub hosted checks on reviewed head
`0b27faf7f7be4e1ec3858f0af19658d6cad4adcf` both passed:

- `contract-regressions`: SUCCESS
- `java-runtime`: SUCCESS
- Run:
  `https://github.com/zh94695424/one-call/actions/runs/44146001266`

The runtime job passed the full build/module/container smoke, container-smoke
presence assertion, default/web JAR smokes and production dependency firewall.

## Independent review and finding status

Plan review, code review, aggregate deepreview, aggregate re-review, and final
re-review each used peer-isolated A/B artifacts submitted before Controller
intake. All material findings were fixed and re-reviewed:

- authority-first reading and exact error precedence;
- canonical-LF-safe byte assertions;
- injectable aggregate deadline with phase-boundary checks;
- successful `Content-Length` framing;
- real-container 401/403/concealment and exact RFC 7807 evidence;
- exact299-file digest lockstep;
- immutable Set membership for detail benefit validation;
- commit-form candidate identity and genuine whole-tree LF execution.

PR #48 review A and B both returned **PASS, zero findings**:

- `docs/reviews/pr-48-review-20261118-014643.md`, SHA256
  `7f0e17a1583fee1f741c7b1a860eae08adc7b14af44e7f6f8400e2e8cfafc5d3`
- `docs/reviews/pr-48-review-20261118-015434.md`, SHA256
  `d14a656086eb391b53364b8a99d37eebeb299bb744b6a5f98e05cd0b4076ef0b`

Reviewed accepted checkpoints:

- plan: `1f7d745d2ad6739efb27814f56d04aa707af540c`
- slice: `4e0a910ac710d3bc3da5d68a7373876c0dee22e2`
- deepreview: `0b3bd3482057d7158be710b2a55f8a9ace991de0`
- PR review: `0b27faf7f7be4e1ec3858f0af19658d6cad4adcf`

No accepted finding remains open. No merge, approval, mark-ready transition,
reviewer request, external PR comment, issue mutation, or branch deletion was
performed.

## PR and issue status

- Draft PR: `https://github.com/zh94695424/one-call/pull/48`
- Base: `main`
- Head branch: `codex/pa37-next-work-unit`
- Final reviewed head:
  `0b27faf7f7be4e1ec3858f0af19658d6cad4adcf`
- State at closeout: OPEN / Draft / mergeable
- Body SHA256:
  `e421af5488e84a621ba919cd4e2bee5e554be3891f5d1dfe417d2c401e76f0d6`
- Changed PR paths: 47

This architecture-sensitive work unit is not tied to a GitHub issue. No issue
link, closing keyword, closeout comment or issue mutation is required.

## Residual risks and owners

- **Capacity/Database + Catalog-order:** per-product version/benefit arrays and
  the benefits `IN` bind list remain unbounded. Roots and cursor capacity are
  bounded.
- **Database/Security + Backend:** V004's existing `catalog_order_role` has broad
  table `SELECT/INSERT`, selected-column `UPDATE` and routine `EXECUTE`
  privileges. PA37 changes none of them; the read-only transaction is not a
  SELECT-only grant boundary.
- **Platform/Security + Backend:** production `SET LOCAL ROLE
  catalog_order_role` provisioning remains an identity-runbook responsibility.
- **Operations IAM/Backend:** `429 RATE_LIMITED` is declared/mapped but no limiter
  is implemented.
- **Contract/Data + Catalog-order:** docs15 wording and the frozen OpenAPI
  `x-onecall-a0-models: catalog_order.catalog_offer` annotation retain offer
  wording although PA37 exposes no offer field and performs no offer read.
- **Backend test-hardening:** static SQL extraction is narrower than the executed
  PostgreSQL statement capture; defensive nullable-membership behavior remains
  schema-unreachable.
- **Operations IAM/Backend:** `PA37-RESIDUAL-BUDGET-SEMANTICS` records the
  deliberate family divergence from siblings that check the budget after
  transaction completion; destination is the next Operations read-consistency
  work unit.
- **CI/Backend/Platform:** the pre-existing PA35 line-ending-sensitive oracle and
  other environment-specific evidence remain separately owned. PA37's final
  exact-head hosted CI and whole-tree LF runs are green.
- **Production readiness:** no deployment, production credentials, external
  traffic, approval, merge or operational-readiness claim is made.

## Next entry point

PA37 is complete at `final closeout pass`. The user may merge Draft PR #48 when
ready. After merge, fetch the updated `main` and start the next Phaseflow work
unit from that merged baseline with a fresh goal confirmation. Do not reuse the
PA37 goal confirmation or this unmerged branch as authorization for later work.
