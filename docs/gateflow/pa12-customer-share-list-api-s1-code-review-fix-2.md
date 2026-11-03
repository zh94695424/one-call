# PA12 S1 code-review fix-2

- Task: `pa12-rereview-fix2-report-gpt-20261102`
- Date: 2026-11-02
- Base: `c49d241`; detached worktree with supplied implementation/review inputs already copied in.
- status: s1-code-review-fix-2-complete-pending-re-review

## Scope and changed paths

Only the accepted Low findings from reviewer A R1-R6 (`docs/reviews/code-review-20261102-213856.md`) and reviewer B F-01-F-04 (`docs/reviews/code-review-20261102-223602.md`) are addressed. This is a fix submission, not re-review acceptance.

Changed paths in this task:

1. `backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java`
2. `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`
3. `backend/README.md`
4. `docs/gateflow/pa12-customer-share-list-api-s1-code-review-fix-2.md` (new)

No production code, DB fixture/IT, CI/POM, plan, control, review, or other protected path was edited for the implementation fix. Supplied tracked/untracked implementation inputs remain separate from this report-finalization task's changes. No commit, push, PR, approval, ready action, reviewer request, or external comment was performed.

## Finding closure

| Findings | Implemented correction |
| --- | --- |
| A R1; B F-03 | Replace the permissive/vacuous invocation assertion after the `/me` loop with explicit `SELECTION == 0`. The assertion no longer resolves the sharing service through `context.getBean` to inspect it. |
| A R2; B F-04 (dead state) | Remove the unread `DUPLICATE_VERIFIER` field and its reset; existing ambiguous-verifier coverage is retained. |
| B F-04 (stale description) | Describe the discovered edge assertion as exactly one production edge, `customer_iam -> sharing`, without changing the configured map or source/model assertions. |
| A R3; B F-02 | Correct README assertion homes: `SharingQueryBoundaryTests` pins the exact named interface and exported members including `ShareQueryDiagnostic`; `CustomerApiBoundaryTests` bans controller diagnostic import/use. |
| A R4 | Shared `assertProblem` now requires `WWW-Authenticate: Bearer` for 401 and absence of that header for every other status passed to the helper. |
| A R5 | Interleave twelve `personal`/`organization`/`other` requests, spanning both accounts and PERSONAL/ORGANIZATION scopes. Unique valid limits correlate each recorded query invocation to its originating request independently of completion order. Assert exactly twelve invocations, the exact request-limit set, and equality of every `TrustedShareCaller` to its server-generated account and optional organization scope. Retain bounded future waits and use `shutdownNow` plus a five-second termination assertion in `finally`. |
| A R6 | Add empty `limit=` (422), literal cursor values of 512 and 513 characters (400), and a real HTTP-returned personal cursor replayed with `other` (400). Assert zero query calls for the initial invalid-input matrix and no additional query call for cross-context replay; retain the valid-limit cases and their adjusted exact call count. |
| B F-01 | Inside existing `verifiesNoCyclesOrBoundaryViolations`, use the ArchUnit `ImportOption` callback's `Location.asURI().getPath()` to exclude test packages and only the sharing application `package-info.class`, then require `IllegalArgumentException` starting with `No named interface named 'customer-query' found!`. Preserve successful production `verify()` and all existing source/model/fixture assertions. |

All closures above describe implemented test/document changes, not newly executed HTTP or Modulith evidence.

## Compile correction and runtime evidence

- The initial focused compile failed because ArchUnit `ImportOption` receives a `Location`, not a `JavaClass`. The author corrected the callback to use `Location.asURI().getPath()`; that correction was imported before the focused rerun.
- Focused rerun passed **49/49 tests**, with zero failures, errors, or skips. Docker server **29.9.2 linux/x86_64** was available.
- `mvn -B -ntp clean -Pcontainer-smoke verify` completed with **BUILD SUCCESS in 2:02**. Surefire reported **67 tests / 13 classes**; Failsafe reported **92 tests / 4 classes** (`64+10+10+8`), for **159 total** with zero failures, errors, or skips.
- PostgreSQL **16.15** was used. Flyway validated and applied **V001-V009**; `CustomerSharesIT` passed **8/8**.
- The exact CI source/CI/XML inventory script passed. Default and opt-in web packaged-JAR smokes passed.
- The production-JAR firewall passed: baseline **37/34**, web **55/48**, **47** counterparts, **8** dependency-only, **one** packager, deltas **18/0** and **14/0**, and all **82** adversarial probes.
- `git diff --check` passed. Protected V001-V009 migrations, OpenAPI, design/security/protocol/acceptance/deployment, and `application*.yml` paths are unchanged. Git runtime/metadata may be unavailable in this WSL workspace; the Controller will audit the final main worktree.

## Residual risks and handoff

- Residual production/deployment/DB and broader review risks remain unchanged and unresolved by this fix. Their existing owners and destinations remain the responsible implementation/deployment owners and the existing design, security, protocol, acceptance, deployment, and review records; this report does not reassign or close them.
- Controller owns the final main-worktree audit and fresh re-review. Runtime evidence above does not constitute reviewer acceptance. No claim is made that the supplied full worktree is clean relative to `c49d241` or that hosted CI is green.

## Status

`s1-code-review-fix-2-complete-pending-re-review`
