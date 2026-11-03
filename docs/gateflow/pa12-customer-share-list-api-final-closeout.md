# PA12 customer share list API - final closeout

- Work unit: `PA12-customer-share-list-api`
- Work-unit type: architecture-sensitive customer API feature
- Branch: `codex/pa12-customer-share-list-api`
- Base: `b458be9a52bf6f7cd9543355dceb98c9d89b364c`
- Draft PR: https://github.com/zh94695424/one-call/pull/20
- Final PR-review checkpoint: `4bcca1cc327280238dbab8a3cd50b765888ee5ef`
- Final pushed controller head before this closeout: `eddb80a23fd4d2b0d3c32328f8873ce80c824e9c`
- State: `Draft`, `OPEN`, `MERGEABLE`, no reviewer request or approval

## What changed

- Added the opt-in `GET /api/v1/customer/shares` API through the existing
  customer-session boundary.
- The endpoint returns the frozen `SharePage` for shares created by the
  server-authenticated account in its server-attested selected personal or
  organization workspace.
- Reused the frozen PA10 creator-scoped query with cursor/limit semantics and
  `validFrom DESC, shareId DESC` ordering.
- Added the bounded `sharing::customer-query` named interface, customer HTTP
  adapter, cursor codec, exact 11-field metadata projection, read-only
  transaction and sanitized error mapping.
- Preserved `/me`, default non-web/database-free startup, V001-V009, OpenAPI,
  design/security/protocol/acceptance/deployment/configuration and unrelated
  customer operations.

## What was verified

- Final Controller run in WSL Ubuntu 22.04 with JDK 21.0.13 and Maven 3.9.9:
  `mvn -B -ntp clean -Pcontainer-smoke verify` completed `BUILD SUCCESS` in
  2:36.
- Surefire: 67 tests / 13 classes. Failsafe: 92 tests / 4 classes
  (`64+10+10+8`). Total: 159 tests, zero failures, errors or skips.
- PostgreSQL 16.15; Docker reported approximately 7,942 MB; Flyway
  validated/applied V001-V009; `CustomerSharesIT` passed 8/8.
- The final PR Reviewer A independently reran all 159 tests successfully.
- Final PR Reviewer B was routed by explicit user instruction to
  `qwen3.8-flash`; it returned a valid `turn.completed`, no failed/error event,
  and `PASS` with no required fix.
- PR #20 local head, upstream branch and PR metadata matched at
  `4bcca1cc327280238dbab8a3cd50b765888ee5ef` before the closeout transition,
  then at `eddb80a23fd4d2b0d3c32328f8873ce80c824e9c` for the final pushed
  controller record.
- `git diff --check origin/main...HEAD` passes; the branch delta contains exactly
  74 intended paths; V001-V009 and all frozen contracts are byte-identical to
  base.
- The corrected Draft PR body quotes all five aggregate/corrected-package
  verdicts, lists their review evidence, and makes no production-readiness or
  `sharing_role` least-privilege claim.

## Review and finding status

- Initial aggregate review found the High `PA12-ACL-01` issue: frozen V009
  grants `sharing_role` table-wide `SELECT, INSERT` and lifecycle `UPDATE`,
  allowing direct secret-column reads after `SET ROLE`.
- User scope decision kept V001-V009 frozen. `PA12-ACL-01` remains
  `deferred-with-owner`, not fixed, and is the sole High pre-production blocker
  outside PA12.
- Accepted aggregate Low findings for owner scope/login wording, validation
  precedence, real-row projection and generation-transposition detection were
  fixed and re-reviewed.
- Corrected-package review accepted the package. Remaining Low/Info items were
  classified with owners.
- PR-review Low PR20-B-01 (inaccurate PR verdict labels) and PR20-B-02
  (historical digest/EOL provenance ambiguity) were fixed and re-reviewed.
- Final Reviewer A found one Low ledger annotation mismatch; Controller fixed
  both LF and shipped CRLF identities in the record. Final Reviewer B was
  `PASS` with no required fix.
- No accepted High/Medium product finding remains open within PA12 scope.

## Documentation

- Updated `backend/README.md` with the current PA11/PA12 API and test inventory.
- Recorded the plan, implementation/fix evidence, aggregate reviews,
  corrected-package reviews, PR reviews and final dispositions in
  `docs/gateflow/project-execution-control.md`.
- Preserved historical review artifacts and private PA8/PA11 worktrees; no
  `.pa8-*`, `.codex`, `tmp/`, replay directory or helper script was committed.

## Remaining risks and destinations

| Risk | Severity | Owner / destination |
| --- | --- | --- |
| `PA12-ACL-01`: direct secret-column reads and write grants under `sharing_role` | High | Next Backend/Database/Security least-privilege work unit; must land before production rollout |
| No production customer-session verifier/session store | Deferred | Customer IAM/Identity + Security; PA12 fails closed until a separately approved adapter exists |
| No supporting share-list index/capacity evidence | Deferred | Backend/Database/Platform capacity assessment |
| Package-coarse `customer-query` export includes the diagnostic record | Low | Architecture/Sharing API governance |
| Legacy workspace compatibility constructors allow a null sentinel | Low | Customer IAM/Security contract hardening |
| Four unused imports and `CustomerSharePage.Item` component-order defense-in-depth | Low/Info | Backend hygiene / later contract-test hardening |
| Canonical LF manifest and non-ASCII snapshot materialization | Low | Controller/CI snapshot tooling |
| Hosted CI has not run for this head | Unverified | PR automation owner; local 159-test evidence is not hosted-CI evidence |

No unclassified residual risk remains. The issue workflow is not applicable:
PA12 was not tied to a GitHub issue, so no closing keyword, issue closeout
comment or issue mutation is required.

## Next entry point

After the user merges PR #20, fetch the latest `main` and create a fresh managed
worktree plus `codex/` branch for the next work unit. The recommended next entry
is `PA13-sharing-role-least-privilege` under a fresh Phaseflow goal
confirmation, with a Backend/Database/Security scope and a role-effective
secret-column denial test. Do not reuse PA12 goal confirmation or begin from
this unmerged branch.
