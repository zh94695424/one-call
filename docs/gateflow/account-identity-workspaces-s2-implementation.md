# Account identity and workspaces S2 - implementation

## Gate / scope

- Work unit: `account-identity-workspaces`; slice: S2 Web and Mini adoption.
- Allowed files: Web/Mini source, generated previews, QA, cross-channel QA, READMEs and account/business documents from the accepted plan.
- Implemented goal: shared natural-person account, verified login identities, membership-derived spaces and roles, independent business numbers.
- Non-goals preserved: production authentication, database migrations, server-side authorization and employee-account redesign.

## Changes

- Web resolves only known active demo identities, derives available spaces from memberships and shows a read-only role. Session recovery resolves the newly authenticated person and rechecks the destination route.
- Settings separates account, real-name status, login identities, selected workspace and business numbers. Other organizations show empty fixture data; customer and Operations employee accounts remain separate.
- Web member phone subjects now belong to the matching natural-person accounts. Canonical authorization changes target the original subject and preserve cross-channel business facts.
- Mini resolves the same demo WeChat account as Web and adds a reachable account/login-identity page; navigation now covers 62 pages.
- Account context remains outside business snapshots; cross-channel import tests assert that imports cannot replace identities or membership roles.
- Fixed S2 review findings: complete scoped UI cleanup on logout/account/workspace switch, account/workspace-owned export tasks, visibility-filtered matter selections and organization number-list isolation.
- Added `docs/11-account-identity-and-workspaces.md` explaining recommended production table responsibilities, current prototype boundaries and demo logins.

## Validation

- `node --test shared/qa/business.test.js shared/qa/runner-exit.test.js`: 31/31 passed.
- `node shared/build-prototypes.js --check`: passed.
- `powershell -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1`: passed after fixes; no page/console errors, including account change, draft cleanup, export isolation, role and responsive checks.
- `powershell -ExecutionPolicy Bypass -File prototype/qa/run.ps1`: passed earlier in S2; 62-page navigation, account page, number/IVR/payment and responsive coverage. Mini source did not change after this pass.
- `powershell -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1`: passed earlier in S2; employee/catalog/IVR regression. Operations source did not change after this pass.
- `node shared/qa/cross-channel.js`: passed after scoped UI fixes; publication, frozen checkout, IVR, claim, trial, payment/entitlement transitions, account scope, four authorization revocations, forbidden imports and hash equality.
- `git diff --check`: passed (only Git line-ending normalization notices).
- Visual QA: new Mini account page at 320/393/760 and Web settings/empty workspace reviewed; no overflow. Five account-focused screenshots selected for this checkpoint; unrelated historical screenshot churn excluded.

## Docs decision / review

- Updated client READMEs, business/rule documents and document index to link the current account contract.
- Code review/re-review: `docs/reviews/code-review-20261021-103742.md`.
- Fix artifact: `docs/gateflow/account-identity-workspaces-s2-fix.md`.
- Completion: S2 implementation and review loop passed; ready for accepted slice commit.

## Residual risks / uncovered areas

- R1 demo credentials/session security: assigned to later production identity/auth work unit.
- R2 simplified natural-person linkage for organization business numbers: assigned to later subject ownership/schema work unit.
- R3 fixture role/name filtering is not server-side tenant authorization: assigned to later production tenant authorization work unit.
- Real OAuth/SMS, production database and backend authorization are intentionally outside this prototype.
