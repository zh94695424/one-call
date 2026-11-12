# PA30 Final Closeout — Customer Personal Subject-Claim Foundation

## Work unit

- Work unit: `PA30-customer-personal-subject-claim-foundation`
- Type: architecture-sensitive authenticated customer command feature over existing PostgreSQL persistence
- Status: **completed — final closeout pass**
- Base: `b48cbb88cec5302b00e3af3be5f621f69838c20f`
- Accepted PR-review checkpoint: `23fe1e373e14c5a15148c72a6e2f7c2a5cf1b3f6`
- Draft PR: https://github.com/zh94695424/one-call/pull/41

## What changed

- Added only the frozen authenticated customer operation `createCustomerSubjectClaim`, exposed as `POST /api/v1/customer/subject-claims`.
- The request is limited to a verified customer session, a personal selected workspace, the exact four server-owned fields and a bounded `Idempotency-Key`. Organization workspaces, foreign owners and client attempts to supply account, organization, role, number, HMAC, ciphertext, OTP or proof content fail closed.
- Implemented risk-before-replay, non-disclosing exact replay/context conflict handling, append-only claim history and set-once owner assignment. A different existing owner is never overwritten, cleared, repaired or transferred.
- Added the minimum fail-closed subject-claim proof-verifier and verification-reference cipher SPI, a dedicated subject-claim command pool, exact `subject_number_role` assertion, READ COMMITTED/REQUIRES_NEW transaction behavior, bounded retry, ordered advisory/row locks and same-transaction audit append.
- Updated the bounded customer/owner wiring, tests, CI inventory checks, POM Failsafe selection and backend README needed by this frozen operation.
- Kept OpenAPI, `docs/14`, `docs/15` and V001-V017 byte-identical. No V018, new grant, production proof provider, production cipher/KMS, credential provisioning or live acceptance was added.

## What was verified

- Focused PostgreSQL 16 Failsafe: `CustomerSubjectClaimsIT` 24/24 plus `SubjectClaimRoleLeastPrivilegeIT` 10/10, total `34/34`, with zero failures, errors or skips.
- Final clean full-container build: `468` Surefire + `370` Failsafe = `838` tests in `54 + 23 = 77` classes, with zero failures, errors or skips.
- Exact source=CI=XML inventory: `77/838`; test-annotation count `839`; POM Failsafe includes `23`.
- Default and opt-in web JAR smoke checks passed. The dependency firewall passed all `116` adversarial probes.
- OpenAPI `npm run check:s3` passed with `49` operations and all stage-S3/document-map checks.
- All 20 implementation-frozen OpenAPI/docs/14/docs/15/V001-V017 raw and LF-canonical hashes matched. The final 41 implementation paths plus implementation report were within the accepted allowlist; no frozen contract or migration changed.
- Hosted GitHub Actions run `43320204173` passed on the exact final PR head:
  - `contract-regressions`: PASS, 41 seconds.
  - `java-runtime`: PASS, 25 minutes 38 seconds; the full Maven/container build, container-smoke assertion, default/web JAR smokes and production dependency firewall all succeeded.

## Documentation

- Accepted plan: `docs/gateflow/pa30-customer-personal-subject-claim-foundation-plan.md`
- Plan fixes: `docs/gateflow/pa30-customer-personal-subject-claim-foundation-plan-fix-1.md`, `docs/gateflow/pa30-customer-personal-subject-claim-foundation-plan-fix-2.md`
- Implementation evidence: `docs/gateflow/pa30-customer-personal-subject-claim-foundation-s1-implementation.md`
- Code review and re-review artifacts: `docs/reviews/code-review-20261113-024637.md`, `docs/reviews/code-review-20261113-040003.md`, `docs/reviews/code-review-20261113-050739.md`, `docs/reviews/code-review-20261113-051526.md`
- Aggregate deepreview: `docs/reviews/code-review-20261113-053008.md`
- PR reviews: `docs/reviews/pr-41-review-20261113-054131.md`, `docs/reviews/pr-41-review-20261113-062442.md`
- Backend README records the delivered operation, exact flags/credentials, fail-closed defaults, validation limits and production residual boundary.

## Finding status

- Initial review A found three Low test/README gaps; review B passed with zero findings at that stage.
- `CR-FIX-01` (accepted-context coverage across proof/method/binding/HMAC): **fixed** by both independent re-reviewers.
- `CR-FIX-02` (all accepted-owner relations plus organization corruption): **fixed** by both independent re-reviewers.
- `CR-FIX-03` (README current-truth wording): **fixed** by both independent re-reviewers.
- Aggregate deepreview on the accepted 47-path checkpoint: PASS, zero Critical/High/Medium/Low findings.
- Independent PR review A and B on the same frozen PR input: both PASS with zero findings; neither read the peer conclusion before submission.
- No accepted, blocking or unclassified finding remains.

## Remaining risks, owners and destinations

| Remaining risk | Owner / destination |
| --- | --- |
| Unbounded `readAllBytes()` request-body allocation | Platform/Security Web and gateway; coordinated ingress limits and future application hardening |
| `RATE_LIMITED`/429 mapping without an implemented limiter; proxy-coalesced opaque header semantics | Platform/Web Gateway; quota, ingress and proxy behavior |
| Real subject-claim proof verifier and supported-method evidence ownership | Subject-number/Security plus the appropriate proof provider, under a later confirmed work unit |
| Production verification-reference cipher, KMS/key ownership, algorithm and key lifecycle | Subject-number/Security/Platform under a later confirmed KMS/security work unit |
| Production credentials, database-login provisioning, deployment configuration and live acceptance | Platform/Operations and acceptance owners; not proved by synthetic test identities |
| External provider/KMS I/O and side-effect transaction design | New future goal/design for Subject-number/Security/Platform; external I/O must not be added under the current row locks by implication |
| Operational observability and replay/audit growth | Backend/SRE/Security; future operational review |
| Slow-host, multi-replica and fully contended database behavior | Backend/Database/SRE; future resilience and load validation |
| CRLF/LF repository custody | Controller/Repository; canonical hashes currently reconcile the environment |
| GitHub Actions Node 20 deprecation and future `ubuntu-latest` image migration warnings | Platform/CI; non-blocking action/image maintenance |

These residuals are classified and have owners/destinations. None is a current-scope acceptance failure, and none authorizes future work under the PA30 goal.

## PR and issue status

- Draft PR #41 remains OPEN, Draft and mergeable at exact head `23fe1e373e14c5a15148c72a6e2f7c2a5cf1b3f6`.
- The PR body accurately states delivered behavior, validation, independent reviews and deferred production/platform risks.
- This work unit is not associated with a GitHub issue. Issue association, closing keywords, closeout comments and issue mutation are not applicable.
- No merge, approval, mark-ready action, reviewer request, external comment, issue mutation or branch deletion was performed.

## Gate result and next entry point

- `draft-PR-pass` is accepted: both PR review artifacts exist, findings are adjudicated, the accepted PR-review checkpoint and exact-head CI are green, and every residual has an owner/destination.
- PA30 is complete at **final closeout pass**.
- Next entry point: the user may review and merge Draft PR #41. After merge, fetch the updated target `main`, create a fresh managed worktree and `codex/` branch from the merge commit, reread the Control/design sources, and run a new Phaseflow goal confirmation for the next bounded work unit.
- Do not continue the next work unit from this unmerged branch and do not reuse PA30 goal authorization, freeze, evidence or residual decisions as authorization for a different scope.
