# PA17 Public Admission/Ticket — Bounded Plan Fix 7

## Contract And Status

- Work unit: `PA17-sharing-public-admission-ticket`; Gateflow gate `fix`, round 7 after re-review 5.
- Task label: `pa17-public-admission-ticket-plan-fix7-gpt-20261106`; date: 2026-11-06.
- Status: RR5-01 and the bounded contract corrections are repaired at plan level only. No later gate is entered; implementation is not started.
- Authorized writes: only `docs/gateflow/pa17-sharing-public-admission-ticket-plan.md` and this new artifact.

## Plan Identity

| Version | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| Verified fix-6 input | 382 | 90,414 | `BE8EF1659600D74E46C62739F39CE16CCF3400372EE37B9195441E17F35FA778` |
| Fix-7 output | 395 | 93,475 | `36169dd2c3b027326a4ce6ebb377e75c48eeba893c5b92973bef0dbc4bc89806` |

The output identity is stated here rather than self-hashed into this artifact. The fix-5 byte-identity claim remains a historical author assertion, not independently reproducible; no current requirement depends on it, and no prior artifact is rewritten.

## Repairs

- **RR5-01:** Member replay/reconstruction explicitly binds the trusted current account, active membership, active organization/account, grant owner organization, and witness recipient organization/account. Mismatch is `SHARE_WRONG_TENANT`; missing, malformed, or ambiguous witness fails closed as `DEPENDENCY_UNAVAILABLE` (503). Existing `wrongTenantAccountOrMembershipFailsClosed` and `lostResponseRetryReconstructsWithoutSecondAdmission` cover a different currently-ACTIVE account in the same organization.
- **Bounded closure:** The plan freezes all five event result values, post-commit expired-ticket-window behavior and grant-terminal precedence, exact expiry generation/revision updates, absent-session and bearer precedence, malformed-body-before-token precedence, nested test-double placement, and the corrected aggregate-Surefire reference while preserving 91 -> 103.
- **Preservation:** No path, schema, OpenAPI/public field, or test method is added. Existing goal/non-goals, one-slice scope, 37 implementation paths, 8 API / 10 implementation paths, 12/24/8 method counts, `20/103 + 9/162 = 265`, migrations V001–V011, V012 privilege boundary, event identities, replay/no-I/O, T/admitted_at, 201 success, and error/race contracts remain frozen.

## Validation And Completion

- Re-read RR5 clauses against the edited plan: member witness comparisons, event result mapping, ticket-window/terminal mapping, expiry generation wording, absent-session handling, and malformed-body precedence.
- Recount paths, named path sets, method inventory, arithmetic, and migration hashes; static checks only. No runtime, build, CI, or test claims are made.
- Final plan identity: 395 lines, 93,475 bytes, SHA-256 `36169dd2c3b027326a4ce6ebb377e75c48eeba893c5b92973bef0dbc4bc89806`.
- Only the target plan and this new fix-7 artifact were written. No Control, source, test, migration, OpenAPI, CI, README, review, private, or later-action path was modified.
