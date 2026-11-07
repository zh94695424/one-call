# PA18 Plan Fix 2 — RR2 Adjudication and Bounded Completion

- **Task label:** `pa18-sharing-media-authorization-plan-fix2-gpt-20261107`
- **Work unit:** `PA18-sharing-media-authorization-and-playback`
- **Gate:** `fix`
- **Runtime/provider:** Java/Spring MVC, PostgreSQL 16, existing PA17 transaction/repository seams, deterministic test media-provider seam; production Q22/provider values remain unavailable and fail closed.
- **Allowed repository changes:** only `docs/gateflow/pa18-sharing-media-authorization-and-playback-plan.md` and this artifact.
- **Disposition:** bounded fix complete; Controller intake and fresh A/B re-review next. No implementation, commit, push, PR, approval, ready action, reviewer request, external comment, or re-review is claimed.

## Scope retained

The confirmed Goal remains binding. This fix adds no endpoint, schema, migration, ACL, OpenAPI, frontend, production Q22/provider value, media delivery, or future-slice behavior. The exact 17-path implementation allowlist remains unchanged: 10 production paths, 6 existing test paths, and 1 CI workflow path. V001–V012, source trees, tests, OpenAPI, POM, CI behavior outside the authorized inventory edits, Control, prior reviews, and unrelated files are not modified.

## RR2 disposition

| Finding | Status | Exact plan repair | Remaining residual |
| --- | --- | --- | --- |
| RR2-01 caller handoff and service contract | fixed | §2.2 defines the one-method `ShareMediaAuthorizationService.authorize(...)` contract and nested functional `CallerResolver`; §3 steps 2–3 pin lazy absent/valid/invalid bearer behavior, one-call member resolution, and 401/404/503 precedence. §6.1 lists web/service/IT assertions. | Context service implementation details remain within existing PA17 seams; unavailable production context still fails closed. |
| RR2-02 concurrent convergence | fixed | §3 steps 8–10 and §4.3 remove fresh TTL expiry, use `min(witness.sessionExpiresAt, witness.admissionExpiresAt, witness.grantExpiresAt)`, use `eventId` as provider idempotency key, treat fingerprint as consistency input, define `REUSED_IDENTICAL`/mismatch behavior, and name the two-connection convergence IT. | A real provider remains an external seam and must honor deterministic replay; no lock may span provider I/O. |
| RR2-03 FAILURE then retry | fixed | §3 step 10 and §4.5 explicitly choose fresh retry after transient `FAILURE`, preserve first success on outage, reuse identical failures, and reject changed purpose/fingerprint before provider I/O. §6.1/§6.2 name retry-success and success-replay-during-outage tests. | Failure/success append ordering remains governed by the existing repository conflict authority. |
| RR2-04 provider compatibility | fixed | §4.4 keeps `ShareMediaCredentialProvider` a valid functional interface and makes `issueOrReplay` plus PA18 request/result types an additive default fail-closed method. It names the singleton and test-fixture overrides; `DefaultShareAdmissionService.java` remains unchanged. | Java compiler and existing PA17 tests remain the implementation proof. |
| RR2-05 output and error precision | fixed | §4.3–§4.5 pin credential code-point/NUL/UTF-8 validation, expiry validation against the witness bound, first durable replay expiry, payload reuse/conflict, and exact policy/provider/service mappings. §6.1/§7 require malformed-output and boundary tests. | The operation’s frozen error vocabulary remains authoritative; no new error code is introduced. |
| RR2-06 replay/conflict precision | fixed | §3 steps 8–10 explicitly map changed fingerprint to 409 `SHARE_GENERATION_STALE` with no provider/event and post-provider mismatch to 503 `DEPENDENCY_UNAVAILABLE` with no success claim. | The 409 semantic overload remains a documented contract residual because OpenAPI is out of scope. |
| RR2-07 exact architecture test sets | fixed | §6.1 rewrites A12 as complete unions: public-admission 8+2=10, implementation 10+1=11, simple names 7+2=9, nested names 11+2, exact command classifier preserved, and provider/result nested types asserted separately in `admission`. | The implementation must preserve exact classifier strength when editing the existing architecture test. |
| RR2-08 attribution and arithmetic | fixed | §6.1 assigns constructor parameters to `customerApiSignaturesAndProjectionAreExact` and public-controller imports to `customerDependenciesAndReadOnlySqlStayInsideBoundary`. §6.3/§7 distinguish actual 265 methods from the 266 line scan with the `PostgresContainerIT.java:2466` comment and retain `103+162=265 → 119+176=295`. | The actual counts are implementation/CI verification obligations, not changed by this plan-only fix. |

## Exact contract/error summary

- Invalid token, including unavailable context that would otherwise be needed, returns 404 `SHARE_TOKEN_INVALID` with no context lookup.
- Non-member with absent bearer performs no resolver/context call; member with absent/invalid caller returns 401 `UNAUTHENTICATED`; unavailable member context returns 503 under the existing typed sharing mapping.
- Wrong tenant/member/risk remains 403; changed purpose or changed same-purpose semantic fingerprint returns 409 `SHARE_GENERATION_STALE` with no provider/event; revoked/expired/session-expired outcomes retain the frozen 410 mapping.
- Missing, malformed, non-positive, or decision-time-invalid witness bounds return `SHARE_SESSION_POLICY_UNAVAILABLE`; provider unavailable/malformed credential or expiry, post-provider payload mismatch, and same-event-ID payload conflict return `DEPENDENCY_UNAVAILABLE`; injected service absence returns `SERVICE_UNAVAILABLE` where PA17 precedent requires it.
- Valid replay returns the first durable credential and provider-returned expiry. Fresh retry after a historical transient `FAILURE` may append the purpose-specific success; after success, provider outage does not append a new failure and returns `DEPENDENCY_UNAVAILABLE` while preserving the first success fact.

## Test additions pinned

The plan names the exact web/service/IT/architecture additions without adding test files. Coverage includes lazy resolver call counts and precedence, invalid-token/context-unavailable 404, non-member absent-bearer zero-call behavior, member absent-caller 401, two-connection independently forced database-time convergence, failure→retry-success, success replay during outage, malformed credential length/NUL/UTF-8, null/equal/expired/above-bound expiry, changed fingerprint, provider-output mismatch, exact A12 unions, import attribution, V012 compatibility, and the full nine-class `container-smoke` suite.

## Validation and completion report

- **Final 17-path arithmetic:** 10 production = 3 new + 7 modified; 6 existing test files; 1 workflow = 17 total. The plan file itself and this fix artifact are gate documents, not implementation allowlist paths.
- **CI arithmetic:** `103 + 162 = 265` actual baseline methods → `119 + 176 = 295`; the separate word-boundary line scan is `266 → 296` because of one comment at `PostgresContainerIT.java:2466`.
- **Required read-only checks:** verify `git status --short`; exact plan/fix artifact existence; exact 17-path list and counts; V001–V012 hashes; source/test/CI/OpenAPI tree hashes; exact architecture sets; method arithmetic; and final line/byte counts and SHA-256 for both changed documents.
- **Actual fix-2 artifact identity:** plan `325 lines / 38,177 bytes / SHA-256 f43355af9a881f4998324f9b7f1db0e1a54e549abd747fedd0ce05df2d3449b5`; fix-2 is `49 lines / 8,344 bytes`, with its final SHA-256 reported in the completion handoff (a file cannot contain its own non-self-referential final hash).
- **Exit status:** plan-only bounded fix exit status is `0` when the read-only validation commands complete successfully. Runtime/provider is Java/Spring MVC + PostgreSQL 16 + deterministic test seam; no PostgreSQL/provider execution is claimed in this gate.

## Remaining blocking questions

None for this bounded fix. Controller intake and a fresh independent A/B re-review remain required before implementation. Any discovery that needs a new endpoint, schema, ACL, OpenAPI change, production Q22/provider value, media delivery behavior, or a path outside the exact 17-path implementation allowlist must stop and request renewed Goal Confirmation.
