# PA17 Sharing Public Admission And Ticket — Implementation Report

## Status

**code-review-fix-1-complete**

The final Controller run completed against a clean PostgreSQL 16.15 runtime. Surefire ran 20 classes / 103 methods and Failsafe ran 9 classes / 162 methods; both had 0 failures, 0 errors, and 0 skips (265 total). The exact source=CI=XML inventory script passed. Failsafe includes `PublicShareAdmissionIT` (24 methods) and `SharingAdmissionRoleLeastPrivilegeIT` (8 methods).

The accepted A/B code-review findings are fixed: post-commit expiry recheck; personal ownership and malformed captured identity handling; risk-before-missing-witness ordering; a separate post-authoritative observation transaction; typed validation; redundant facts removal; all V001–V011 hash pins; and README cleanup. Deferred/rejected exactly as Control: A F-PA17-05, B-F-03, B-F-05, B-F-06.

## Runtime Evidence

- Full Controller log: `/tmp/pa17-controller-full-code-review-fix6.log`; Surefire 20/103 and Failsafe 9/162 = 265, with zero failures, errors, or skips.
- Focused suite log: `/tmp/pa17-focused-code-review-fix6.log`; 12 unit + 24 admission + 8 role = 44 green.
- The full runtime exposed and fixed two production issues: PostgreSQL admission/event `OffsetDateTime` binds now use `?::timestamptz`, and event identity conflict comparison uses event type, result, and `payloadSha256` rather than raw JSON-node equality.
- Fixture-only corrections cover risk projection, ownership/expiry/generation transitions, role password replay, and the expected ticket-event conflict diagnostic.
- Default and web JAR smokes from the current CI heredoc passed. The production dependency firewall passed all 82 probes.
- V001–V011 hash tests and all migrations passed; source=CI=XML inventory, diff/scope checks, and `git diff --check` passed.
- No production Q22/provider/deployment values were invented.

## Fix 1 Changes And Validation

- Post-commit replay rechecks persisted session/admission expiry before policy lookup and returns typed `SHARE_SESSION_EXPIRED` without provider calls or `TICKET_FAILED`.
- Grant projection now carries `created_by_account_id`; personal ownership is matched to the creator, and malformed captured identity JSON is unresolved fail-closed. Organization ownership remains exact.
- Missing-witness denial follows risk and current member/account/tenant checks; verification observations use a separate `REQUIRES_NEW` transaction; direct validation returns `DeniedCommitted(VALIDATION_FAILED)`.
- Removed the redundant pre-lock facts read, pinned all eleven V001–V011 hashes in the existing role test, and marked stale PA16/ten-entry README statements historical while preserving the current 265 inventory.
- The focused and full Controller runtime, JAR smokes, firewall probes, migration/hash checks, inventory, and scope audits are green. Local WSL/Maven limitations are historical and do not qualify or contradict the recorded Controller evidence.

## Scope And Provenance

- Work unit: `PA17-sharing-public-admission-ticket`; task: `pa17-public-admission-ticket-implementation-report-finalize-gpt-20261107`.
- The implementation changed exactly the 38 accepted-plan paths plus this report. Control changes, if any, are Controller bookkeeping only.
- The accepted plan is 406 lines / 107,832 bytes, SHA-256 `6C00949299EACEF2A37F3723B450FAB77B8F8F341D279A620632713A8F262001`; checkpoint `fa4353dc7b1a4da606b9c432e6f2e62e31bab7a2`.
- No commit, push, PR, or review action was performed.

## Implementation Summary

- Added a closed request binder, redacted request/token/caller/view values, the named `public-admission` API, exact POST route, optional bearer boundary support, and frozen problem mappings.
- Added separate opt-in `sharing_admission_role` datasource/transaction/DSL/SHA wiring. Provider defaults fail closed; no production policy TTL, proof algorithm, credential provider, Redis dependency, or credential is invented.
- Added repository-only prepared SQL, V012 privilege-only grants, and service sequencing. Preliminary lookup precedes proof; proof/policy I/O stays outside transactions; final grant lock samples DB time and atomically stores admission, event, and count. Ticket work follows PostgreSQL closure.
- Fresh policy absence fails before write. Successful replay exits its authoritative transaction before one fresh ticket-policy lookup; persisted windows and counts are not renewed.
- Event IDs and payloads are redacted and deterministic. No new table, column, or function was added, and V001–V011 are unchanged.

## Runtime Limitations

- PA17 review fix 2 restores the `Facts facts = facts(grant, caller);` binding after the post-lock clock sample and extends existing `ShareAdmissionServiceTests` methods for post-commit expiry, personal-owner mismatch, risk precedence over a missing replay witness, malformed captured-identity fail-closed behavior, and separate transaction history for proof-denied observations. No test method/class was added.
- The local reviewer sandbox could not run Docker; the Controller host ran the PostgreSQL/Testcontainers runtime successfully.
- Production Q22/provider/media values and related adapters remain external and fail-closed pending approved Product Security / Platform implementations and configuration.
- This report makes no claim of production readiness, merge readiness, or deployment approval. The recorded green evidence is the specified Controller runtime and smoke validation.

## Review Fix 3

- Corrected three existing `ShareAdmissionServiceTests` fixtures: expiry replay assertions now compare against the post-initial-admission event baseline; malformed captured identity runs with no existing admission or witness and asserts no active-binding fallback or insert; and the member wrong-tenant case restores the normal ownership projection.
- The focused runtime in `/tmp/pa17-focused-code-review-fix2.log` ran 12 tests and reported these three fixture-attribution failures. This fix was statically inspected only; no rerun or passing test result is claimed.

## Review Fix 4

- Reworked the malformed `identity_at_capture` case to use its own fixture with no persisted admission/witness, asserting fail-closed denial and no admission insert or count update without attributing behavior to Mockito stub invocation counts.
- Reworked the organization-member wrong-tenant case to use an independent member-mode fixture with its active organization, membership, account, and mismatched trusted organization, exercising tenant validation ordering rather than unresolved personal ownership.
- No production code changed. Static checks only; no test run or passing test result is claimed.

## Review Fix 5

- Proof-denied `authoritative` now returns `SHARE_VERIFICATION_FAILED` without writing an observation. After `writes.execute` closes, `admit` writes the deterministic redacted observation through exactly one separate `REQUIRES_NEW` transaction, sampling its event time inside that transaction. Observation failure remains fail-closed through the existing outer exception mapping; admission/count writes do not occur on this path.
- Updated the existing proof-denied assertion to verify transaction history is `begin, commit, begin, commit` after proof setup, showing the authoritative transaction commits before the observation transaction begins. No test method/class was added.
- The later focused and full Controller runs recorded above supersede this historical local validation note.

## Review Fix 6

- Corrected the existing fresh proof-denied transaction-trace assertion to require exactly three `begin, commit` pairs after the prior baseline: preliminary read, authoritative denial, and separate `REQUIRES_NEW` observation. No test method/class or production code was added or changed.
- The later focused and full Controller runs recorded above supersede this historical local validation note.

## Review Fix 7

- Removed the redundant pre-lock `facts` read in `ticketContext`; the single post-lock read remains a declaration.
- Added a service-level invalid-request assertion to the existing test method, verifying `DeniedCommitted(VALIDATION_FAILED)`, no events, and no admission/count writes. The separate dead malformed-fixture assignment remained in the candidate and was not removed in Fix 7.
- This fix was not runtime-tested in this task; no runtime success is claimed.

## Review Fix 8

- Removed only the dead malformed-fixture assignment from the existing service test while preserving the independent malformed fixture and assertions.
- Extended the existing web test method to post authenticated organization-member admission through MockMvc, assert HTTP 201, and verify the captured `TrustedShareAdmissionCaller` has `ACCOUNT` and `ORGANIZATION`; the same method now verifies a schema-valid typed `DeniedCommitted(SHARE_EXPIRED)` maps through the controller handler to the frozen `410 SHARE_EXPIRED` problem response.
- This agent did not claim the Controller's runtime as its own; the recorded Controller evidence remains separately attributed.
