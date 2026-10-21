# Account identity and workspaces S1 - implementation

## Gate / scope

- Work unit: `account-identity-workspaces`; slice: `S1 shared identity contract`; gate: `implementation`.
- Allowed scope implemented: shared immutable customer-account fixture, login/workspace query API, validation tests, shared README and generated inline modules/previews.
- Non-goals preserved: no real OAuth/SMS/session service, no business snapshot schema change, no Operations employee-account change.

## Changes

- Added a private validated directory for natural-person accounts, login identities, real-name profiles, organizations and memberships.
- Added a redacted frozen public projection and cloned query results; raw provider subjects are used only for exact demo matching and the explicitly named fixture-test factory.
- Added fail-closed `resolveDemoLogin`, `accountById`, `loginIdentitiesForAccount` and membership-derived `workspacesForAccount`.
- Kept customer account/session data outside `BUSINESS_FIELDS`, `snapshot` and `loadReviewSnapshot`.
- Added negative tests for unknown/wrong/disabled identities, business-only mobile/fixed-line login attempts, duplicate provider identities, duplicate active memberships and disabled organization/account filtering.
- Regenerated the shared module in all three sources and previews.

## Validation

- `node --test shared/qa/business.test.js`: 30/30 passed.
- `node shared/build-prototypes.js --check`: passed.
- `node --test shared/qa/runner-exit.test.js`: 1/1 passed.
- `operations-prototype/qa/run.ps1`: passed existing employee, catalog, IVR and responsive suite.
- `web-prototype/qa/run.ps1`: passed existing login/session, role, interaction and responsive suite.
- `prototype/qa/run.ps1`: passed existing 61-page, number, package, trial and responsive suite.

## Residual risks

- R1 real authentication/session security: assigned to later production identity/auth work unit.
- R2 organization-owned business-subject schema: assigned to later subject ownership/schema work unit.
- R3 server-side tenant authorization: assigned to later production tenant authorization work unit.

## Completion

- S1 implementation complete and ready for slice code review.
