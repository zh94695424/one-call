# PA41 Customer Order Creation Foundation — Final Closeout

## Status

- Work unit: `PA41-customer-order-creation-foundation`
- Type: customer-facing transactional API feature
- Scope: ACTIVE PERSONAL non-QUOTE payable offers only
- Status: **final closeout pass**
- Draft PR: `https://github.com/zh94695424/one-call/pull/53`
- Accepted PR-review checkpoint: `c87ec63906c80195aba31bdb03821ea92b7616c7`
- Base: `main` at `ea8247b106a6fe80ea5da052eca351ae1d3049a5`

## What changed

- Implemented the frozen `POST /api/v1/customer/orders` / `createCustomerOrder` operation.
- Creates one immutable `UNPAID` `catalog_order.customer_order` snapshot, one current `ACCEPT` purchase-consent record, one completed `PURCHASE` idempotency record, two Outbox facts and two audit facts in one LO-local transaction.
- Server-derives the customer account, workspace, channel, price, currency, offer/version identity, full snapshot and version token; client-supplied price, payment or authority facts are not trusted.
- Implements exact 201 creation, 200 identical replay, 409 semantic conflict, UNKNOWN blocking, frozen typed denials and zero-partial-write rollback.
- ENTERPRISE and QUOTE checkout fail closed and create no order, consent, idempotency completion, audit/Outbox fact or payment fact.
- Added customer HTTP admission, strict binding, catalog-order owner orchestration, activation-consent owner collaboration, PostgreSQL16 integration coverage, role/lock/concurrency/fault injection and architecture guards.

## What was verified

- Target review selectors: 8 Surefire + 12 Failsafe, zero failures/errors/skips.
- Complete focused PA41 selector: 113 Surefire + 37 Failsafe, zero failures/errors/skips.
- Authority-token helper: 48/48 negative probes rejected.
- Inventory sensitivity control: disposable index creation changed the catalog inventory, drop restored it, and a real checkout then left it unchanged.
- Historical full suite before final test-only evidence hardening: 1,127 Surefire + 738 Failsafe, zero failures/errors/skips.
- PR Reviewer B independently ran the full 1,127-Surefire identity set and the 113+37 focused suite on the frozen head.
- PostgreSQL16.16, Flyway19 and PA25 role/topology checks passed, including deterministic concurrency, lock order, restoration, post-commit ambiguity and zero payment/issuance/catalog writes.
- Final 47-path candidate manifest: `8353c5e3c230a0f6ca7297054e85dcbec13004579db37d95c74ec1b84c794411`.
- Hosted GitHub Actions jobs did not start because of GitHub billing/spending-limit. This is an account/platform blocker, not a test result and not a green-CI claim.

## Review and finding status

- Plan review, code review, aggregate deepreview and PR review each received independent A/B review.
- All accepted findings were fixed and independently re-reviewed.
- Final aggregate re-review 3: A PASS and B Approved, both 0 material findings.
- Final PR review: A `pass-with-risks` and B `pass-with-risks`, both 0 material findings.
- No blocking open question or unclassified residual remains for PA41.

## Documentation

- Plan: `docs/gateflow/pa41-customer-order-creation-foundation-plan.md`
- Cumulative implementation/review evidence: `docs/gateflow/pa41-customer-order-creation-foundation-implementation.md`
- Controller truth: `docs/gateflow/project-execution-control.md`
- Plan, code and aggregate/PR review artifacts are recorded under `docs/reviews/`.
- V001–V019, OpenAPI, docs12/14/15/17 and PA25 remain unchanged.

## Residual risks and destinations

- Current hosted CI and an uninterrupted full `clean verify` at the final evidence head: Repository/Platform/Test-CI after billing restoration; required before any merge decision.
- Production Q10 purchase-policy values: Activation/Legal/Product.
- Full docs14 §10.19 catalog publisher and schedule/terms inputs: Catalog/Product/Finance, Q4/Q20.
- Customer order read and UNKNOWN reconciliation/recovery: Payment reconciliation plus API/Product, tentatively the next PA42 work unit.
- Shared 4-connection LO capacity, hosted-CI duration and timeout tuning: Capacity/Test infrastructure/docs19.
- Publication advisory rank 9→10 not yet in frozen docs14 lock table: Data Model/Concurrency/Catalog/Lifecycle.
- Future entitlement→catalog module cycle must be cut before adding that edge: Backend/Architecture.
- Frozen OpenAPI enterprise wording, advertised order reads and `payment_order` model mismatch: API/A0 governance plus Product.
- Raw-checkout A2/A5 CRLF-sensitive parser limitation: Repository/Test tooling.
- Approved terms beyond fixed bounds: Catalog/Product/Finance/API policy decision.
- Conservative source-text authority guard and PostgreSQL-version-specific catalog formatting: Backend architecture/Test infrastructure.
- Enterprise payable checkout requires a separate commercial-model/schema/design decision; it is outside PA41.

## Issue status

PA41 is not tied to a GitHub issue. Issue linkage and closeout comments are not applicable; no issue was created or mutated.

## Completion and next entry

PA41 is complete at `final closeout pass`. PR #53 remains OPEN and Draft; this closeout does not merge, approve, mark ready, request reviewers or comment externally.

After the user merges PR #53, fetch the updated `origin/main`, create a fresh managed worktree and `codex/` branch from that merge, preserve prior worktrees/private helpers, reread Control and applicable design sources, and perform a fresh Phaseflow goal confirmation for the next bounded work unit. The recommended next entry is `PA42-customer-order-read-and-unknown-recovery-foundation`; this is tentative and requires fresh goal confirmation from the merged base.
