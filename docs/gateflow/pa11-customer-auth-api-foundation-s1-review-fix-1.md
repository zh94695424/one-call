# PA11 S1 code-review fix 1

Task label: `pa11-s1-code-review-fix1-gpt-20261102-01`

## Findings and status

- Registration and mapping cardinality — fixed in the production-scanned web context and no-DB HTTP context. Existing tests assert exactly one `CustomerProblemWriter`, `CustomerApiErrorController`, and boundary-filter registration; exactly one `GET /api/v1/customer/me` mapping and `/error` mapping; and absence of `BasicErrorController` bean/handler. Spring's mapping registry also rejects ambiguous duplicate mappings during context creation.
- Wrong non-null database role — fixed within `roleAssertionPrecedesRepositoryAndTransactionIsReadOnly`; the `postgres` result asserts `SERVICE_UNAVAILABLE`, transaction rollback, and no repository interaction. Existing null-role coverage remains.
- Verifier candidate count — fixed; the count check now occurs outside the verifier-failure translation `try` block. Zero/multiple candidates yield 503; verifier invocation failures retain the specified status translation.
- Exact persistence qualifiers — fixed; each parameter qualifier is compared with its full expected value from accepted plan §4.
- Successful media type — fixed; the frozen successful-response test requires `Content-Type: application/json`, preserving existing JSON and header checks.
- Adjudicated residual B3 — deferred-with-owner, Backend/Database maintainability; positional prepared SQL remains unchanged because bindings and source/field allowlists are already enforced.
- Adjudicated residual B4 — rejected; persistence configuration remains web-profile-only and lazy. No non-servlet path reaches its graph; servlet conditional scope remains on web wiring/controllers.
- Accepted q=0/Accept behavior — unchanged, as required by the accepted plan.
- Other residual risks — partial response-wrapper contract, type-loose default branches, historical README wording, and missing production tracing remain with their existing owners; no changes made.

## Evidence and validation

- Changed implementation/test files: `backend/src/main/java/com/onecall/core/customer_iam/internal/security/CustomerBearerAuthenticator.java`, `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerContextServiceTests.java`, `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java`, and `backend/src/test/java/com/onecall/archtests/CustomerMeWebTests.java`.
- `git diff --check` — could not run: this checkout's `.git` file points to unavailable `D:/codex-call/.git/worktrees/codex-call9`; Git reports “not a git repository”.
- Focused Surefire command: `./mvnw -q -Dtest=CustomerContextServiceTests,CustomerWebConfigurationTests,CustomerMeWebTests test` — not run; wrapper has CRLF shebang (`/bin/sh^M`). Bash invocation also fails on CRLF syntax.
- System Maven alternative: `mvn -q -Dtest=CustomerContextServiceTests,CustomerWebConfigurationTests,CustomerMeWebTests test` — blocked by the local Maven launcher (`ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`).
- Compile and container-smoke verification — blocked by the same unavailable Maven launcher. Required `49 + 84 = 133` clean smoke verification is therefore not claimed.
- Existing Surefire artifacts (pre-existing; not produced by this fix): `backend/target/surefire-reports/com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests.txt` reports 6 tests, zero failures/errors/skips; `backend/target/surefire-reports/com.onecall.archtests.CustomerWebConfigurationTests.txt` reports 6, zero failures/errors/skips; `backend/target/surefire-reports/com.onecall.archtests.CustomerMeWebTests.txt` reports 8, zero failures/errors/skips. These are not validation of the current edits.
- Existing report XML paths: `backend/target/surefire-reports/TEST-com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests.xml`, `backend/target/surefire-reports/TEST-com.onecall.archtests.CustomerWebConfigurationTests.xml`, and `backend/target/surefire-reports/TEST-com.onecall.archtests.CustomerMeWebTests.xml`; timestamps/results predate this validation attempt.
- Per-file SHA-256: authenticator `20168ae939b6275f5ef3a98cdefa250762e825596c61c13473d3a6e57fca4542`; context tests `3776d1547eb0b1beb48c2bf185e46b530bcd39d2268240e3e2afada670ca8abb`; web configuration tests `6ddc9b02233e6568c7c0790ef24254e54d92178d2f506289deaf5f3e966ac9f0`; web tests `3bb7b226356c48dd83e5a521f9023c26b687a9473800c8c7fcd56342ffe8670f`.
- Aggregate diff hash and frozen unchanged-file comparisons could not be calculated because Git metadata is unavailable. The working edits were limited to the four files listed above and this artifact; no other files were intentionally edited.
- `git diff --check` likewise remains unverified due to missing Git metadata.

## Controller validation addendum

- The author runner could not launch Git/Maven because of its mounted-path/toolchain setup. The controller reran validation with the approved WSL Temurin 21.0.13 and Maven 3.9.9.
- Focused Surefire command `mvn -f backend/pom.xml -B -ntp -Dtest=CustomerContextServiceTests,CustomerWebConfigurationTests,CustomerMeWebTests,CustomerSessionBoundaryTests test` exited `0`: 25 tests, 0 failures, 0 errors, 0 skipped.
- Full `clean verify -Pcontainer-smoke` exited `0` in 1:53. The fresh XML inventory is 49 Surefire across 11 classes and 84 Failsafe across 3 classes, totaling 133, with zero failures/errors/skips. Log: `/tmp/pa11-web-probe/full-fix1.log`.
- `git diff --check` is clean. The post-fix 33-file product/test/CI/README diff SHA256 is `0f70cf59d40f9aee6f7234dfe57b36c2f25ebf6f2f9ac97cdf420bebbccf4d64`; file manifest `/tmp/pa11-web-probe/fix1-implementation-files-sha256.json`; fresh test inventory `/tmp/pa11-web-probe/fix1-test-inventory.json`.
- Post-fix product JAR SHA256 is `f70f36d5c9aeeccf01139eafa627f881bd46ad3f41828238894d27abf79ea1ea`.
- Generated `build/` output was moved out of the worktree; no ignored/generated files are part of the fix diff. The only intended source/test edits are the four files above plus this artifact.

status: code-review-fix-1-complete
