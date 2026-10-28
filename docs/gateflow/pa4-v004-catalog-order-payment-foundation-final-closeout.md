# PA4 V004 Catalog, Order and Payment Foundation — Final Closeout

- Work unit: `PA4-v004-catalog-order-payment-foundation`
- Branch: `codex/pa4-v004-catalog-order-payment-foundation`
- Base: `81022037dc5dced625788477835472a84a16d52d` (merged PR #11 / `main`)
- Draft PR: [#12](https://github.com/zh94695424/one-call/pull/12), Open/Draft, base `main`
- Closeout scope: V004 persistence foundation and executable PostgreSQL evidence only

## What changed

- Added the frozen `V004__catalog_order_and_payment.sql` foundation for the four S2 schemas, eight NOLOGIN roles, seven catalog/order/payment tables, exact constraints, indexes, foreign keys, guards, triggers and ACL boundaries.
- Added cumulative PostgreSQL 16 evidence for inventory, lifecycle shapes, immutable snapshots, append-only rows, ACLs, FK backstops and bounded concurrency behavior.
- Corrected lifecycle evidence to use legal `DRAFT -> ACTIVE -> OFFSALE`, terminal PAID fixtures, and independent legal `UNPAID -> CLOSED` fixtures. Forbidden `OFFSALE -> ACTIVE` and `PAID -> CLOSED` are not presented as service workflows.
- Restored runtime-role execution for positive `catalog_offer` updates and corrected the stale-query wording in the fix artifact.
- Kept V001-V004 byte hashes, service boundaries, V007 consent-FK deferral and all 31/51/11 evidence counts unchanged.

## Verification

- MiMo bounded fix validation: `validate` passed; default verify `10/0/0/0`; PostgreSQL 16.15 container profile `31/0/0/0`; no skipped or disabled tests.
- Independent DeepSeek PR re-review 2: `docs/reviews/pr-12-review-20261028-133733.md`, conclusion `pass`; findings F-PA4-PR-02, F-PA4-PR-RR1-01 and F-PA4-PR-RR1-02 are all `已修复`.
- Accepted checkpoint: `5290a82190b9f7062295381e6623fd1a6e3bbc13`.
- Exact-head CI for controller push `02857575108913c49df2302ddd2dcf476c4fb6e0`, run `40639574536`: `java-runtime` SUCCESS and `contract-regressions` SUCCESS. A later closeout-only documentation head must receive the same two checks before completion is considered externally current.
- PR remains Draft/Open; no merge, approval, Ready-for-review transition, reviewer request or issue mutation was performed.

## Finding and residual-risk reconciliation

Closed findings: F-PA4-PR-02, F-PA4-PR-RR1-01, F-PA4-PR-RR1-02, F-PA4-S1-01 and F-PA4-AGG-01.

Remaining risks retain explicit owners and destinations: exact-head CI/JDK and action deprecation warnings (Controller/CI); floating PostgreSQL image, superuser fixture and provisioning/digest (Platform/Database/Security Q17/Q23); serial cleanup, parallel isolation, failure-injection cleanup, PG deparse literals and column-level REFERENCES denial coverage (Backend/QA hardening); stale non-count README/Javadoc labels (Backend/QA documentation pass); production catalog/order/payment/issuance/provider work and Q2/Q4/Q6/Q20/OQ-4 (named later service owners); purchase-consent FK (V007); historical focused concurrency XML gap (Controller/QA). The timeout/aborted-race failure injection was never run and is not claimed as evidence.

## Next entry point

After the user merges PR #12, update local `main` from `origin/main` and start a fresh Phaseflow goal confirmation for the next bounded work unit: **PA5 V005 IVR task/session/call persistence foundation**. Do not implement PA5 from this branch before PR #12 is merged.

## Completion

The PA4 V004 work unit satisfies its confirmed persistence-only goal and reaches `final closeout pass` once this closeout documentation commit's exact-head CI is green. The PR intentionally remains Draft/Open pending the user's merge decision.
