# PA27 customer business-number read foundation — final closeout

- Work unit: `PA27-customer-business-number-read-foundation`
- Type: architecture-sensitive customer read API backed by PostgreSQL persistence
- Status: `final closeout pass`
- Draft PR: <https://github.com/zh94695424/one-call/pull/38>
- Base: `main` `7b62233136f6fa1f0c09f64772f391ee53de9428`
- Reviewed/final-push head: `3f4215f9ee8236e4504ca1f9433f8f4de233cc02`
- Issue association: not applicable; this work unit was not created from or linked to an issue.

## Delivered behavior

`GET /api/v1/customer/business-numbers` / `listCustomerBusinessNumbers` now returns the frozen `BusinessNumberPage` contract:

- the account and selected personal/organization workspace derive only from the verified customer session;
- every binding under a subject owned by the selected personal account or active organization is visible for all binding, activation and real-name states, with missing activation projected as `INACTIVE`;
- inactive account/organization/membership, foreign-workspace, unowned and cross-tenant subjects remain forbidden or invisible;
- personal and all four active organization membership roles are supported;
- masked numbers and the exact seven item fields are server-derived;
- stable keyset pagination is `subject.createdAt DESC, bindingId ASC`;
- cursors are endpoint-only HMAC-SHA256 values with exact scope, length, error and empty-tail behavior;
- lifecycle, page and activation reads share one read-only `REPEATABLE_READ` transaction under `customer_iam_role`;
- full numbers, ciphertext, HMACs, ownership selectors and foreign rows are never exposed.

The change adds two owner-bounded named query seams and a privilege-only V015 migration with exactly fourteen tuples:

- customer ACL `12 -> 26`;
- evidence remains `16`;
- global ACL `247 -> 261`;
- other roles remain `219`;
- six-login / seven-effective-role / four-edge graph unchanged;
- OpenAPI and V001-V014 unchanged.

## Validation

- `clean -Pcontainer-smoke verify`: PASS.
- Surefire: 40 classes / 341 tests, zero failures, errors or skips.
- Failsafe: 17 classes / 272 tests, zero failures, errors or skips.
- Total: 57 classes / 613 tests.
- Exact source = CI = XML inventory: PASS; annotation-line scan 614; seventeen Failsafe includes.
- PostgreSQL 16 full V001-V015 chain, PA25 apply/check/replay, role isolation, pool restoration and zero-write proofs: PASS.
- Default/opt-in JAR smoke, API contract checks, firewall probes and frozen-byte checks: PASS.
- GitHub Actions run `43020500908` at head `3f4215f9`: `java-runtime` PASS in 15m17s and `contract-regressions` PASS in 28s.
- Final PR fix `PA27-PR38-CI-01` was verified by both hosted Linux CI and fresh independent PR re-review.

Production/live credentials, TLS, `pg_hba`, deployment, rotation and production acceptance remain **NOT EXECUTED / NOT CLAIMED**.

## Documentation

- cumulative executable plan and plan-fix;
- S1 implementation and S1-fix reports;
- PR cross-platform frozen-hash fix;
- four plan-review/re-review artifacts;
- four code-review/re-review artifacts;
- two aggregate deepreview artifacts;
- two PR-review artifacts and two final PR-re-review artifacts;
- current runtime-boundary updates in docs/14, docs/17 and the PA25 runbook;
- backend README synchronization;
- this closeout and Control.

## Finding disposition

- `PA27-PLAN-REV-01..04`: fixed and re-reviewed.
- `PA27-S1-IMPL-01`: fixed and re-reviewed.
- `PA27-CR-01..05`: fixed inside existing test methods and independently re-reviewed.
- Prior B findings on `WithoutPageReads` and EOL-only historical artifacts: `rejected-with-reason`.
- Aggregate deepreview: both reviewers `pass-with-risks`, no material finding.
- `PA27-PR38-B-01` V015 snapshot hash observation: `rejected-with-reason`; the PR blob is LF and hashes to the documented raw SHA256.
- `PA27-PR38-CI-01` Linux/Windows frozen-hash oracle: `fixed`; canonical-LF normalization preserves all non-newline bytes, with hosted CI and A/B re-review passing.

No accepted finding remains unfixed and no blocking open question remains.

## Residual risks and owners

- Cursor HMAC key custody, distribution, replica consistency and rotation: Security/Operations.
- Column grants are not RLS; tenant filtering remains an application responsibility: Security/Database.
- Qualified cross-module DSL bean inversion: Backend/Architecture follow-up.
- Representative large-owned-set sort/index EXPLAIN: Backend/Database.
- `REPEATABLE_READ` is per request, not a cross-page snapshot or linearizable revocation: Backend/Security.
- Production credential assignment, TLS, `pg_hba`, pool drain and old-session invalidation: Platform/Database/Security/Operations.
- Reverse-scope exact-code assertion, duplicated newline helper and historical wording cleanup: Backend/Test/docs opportunistic maintenance.

## Next entry point

After the user merges PR #38, fetch updated `origin/main`, create a fresh managed worktree and `codex/` branch from the merge commit, and run a fresh Phaseflow goal confirmation for the next user-selected bounded work unit. Do not continue from this branch or reuse PA27 authorization.
