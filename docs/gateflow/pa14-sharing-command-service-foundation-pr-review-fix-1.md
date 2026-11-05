# PA14 PR #22 — Bounded PR Review Fix 1

- Task: `pa14-pr22-review-fix1-gpt-20261105`.
- Work unit: `PA14-sharing-command-service-foundation` (backend feature).
- Gate: PR Review -> fix, following the mandatory independent A/B review.
- Date: 2026-11-05 (Asia/Shanghai).
- Task-declared runtime/provider/model: Codex runner / `gpt` / `gpt-6.1-sol`, reasoning `high`.
- Frozen head verified: `b18278bd1958690609f54bebc4fac516bfa6124d`.
- status: needs-more-evidence

## Accepted finding and bounded correction

Reviewer A's accepted finding: the service, unit oracle, container read-back oracle and competing-grant helper hashed wrapper keys `contentObject`, `verificationObject` and `creationObject`, although V009 persists `content`, `verification` and `creation`. The accepted plan repeated that contradiction. An independent verifier following frozen docs/14 §2.12 would therefore compute a different durable `content_sha256`.

The service now constructs scope with the persisted member names and hashes the complete snapshot directly for `content_sha256`. The stored snapshot shape remains exactly `{content, verification, creation}`. The unit expected canonical JSON vectors use those names. The IT independently selects the persisted row through an admin connection, parses its JSONB, constructs scope and complete objects from the read-back `content`, `verification` and `creation` members, and recomputes both digests against the stored values. Its object-type, creation fingerprint, expiry and other assertions remain intact. The competing-grant helper hashes the same complete persisted snapshot shape.

No canonicalizer, error mapping, lock, transaction, secret, facts, policy or provider logic changes. No tests or assertions are removed or disabled, and no future slice is added.

## Exact changed paths

1. `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java`
2. `backend/src/test/java/com/onecall/archtests/sharing/CustomerShareCommandServiceTests.java`
3. `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java`
4. `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`
5. `docs/gateflow/pa14-sharing-command-service-foundation-pr-review-fix-1.md` (this new artifact).

## Canonical digest contract

Let `JCS` be RFC 8785 canonical JSON encoded as UTF-8, and `H` be lowercase hexadecimal SHA-256:

- `scopeDigest = H(JCS({"content": content_snapshot.content, "verification": content_snapshot.verification}))`.
- `content_sha256 = H(JCS(content_snapshot))`, where the complete snapshot has exactly the members `content`, `verification` and `creation`.
- `creationFingerprint = H(JCS(content_snapshot.creation with only creationFingerprint removed))`.

The creation fingerprint's input fields and exclusion rule are unchanged; its value changes only through the corrected `scopeDigest` embedded in `creation`. JSONB textual ordering is not the hash input; RFC 8785 canonicalization remains authoritative.

## Validation commands and outcomes

All Maven commands below were attempted from `backend`. Linux fallback runs used `JAVA_HOME=/opt/jdk21` (Temurin 21.0.13) with `/opt/jdk21/bin` and `/opt/maven-3.9.9/bin` prepended to `PATH`. Installed Maven 3.9.9 matches the wrapper's configured distribution version. No wrapper, POM or other tracked environment file was changed.

### Required commands

1. `git diff --check`: passes, exit 0, with explicit `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call12` and `GIT_WORK_TREE` set to this worktree. Ordinary Git discovery initially failed because `.git` points to a Windows path: `fatal: not a git repository: /mnt/c/Users/Administrator/.codex/worktrees/pa14-sharing-command-service-foundation/codex-call/D:/codex-call/.git/worktrees/codex-call12`. Environment overrides resolve this without editing `.git`. Git emitted existing LF/CRLF conversion warnings, not whitespace errors.
2. `./mvnw -B -ntp -Dtest=com.onecall.archtests.sharing.CustomerShareCommandServiceTests test`: exit 126 before Maven starts. Exact error: `/bin/bash: ./mvnw: /bin/sh^M: bad interpreter: No such file or directory`. Log: `/tmp/pa14-pr22-review-fix1-unit.log`.
3. `./mvnw -B -ntp -Pcontainer-smoke -Dit.test=com.onecall.archtests.container.CustomerShareCommandsIT verify`: exit 126 before Maven starts with the same exact CRLF interpreter error. Log: `/tmp/pa14-pr22-review-fix1-it.log`.

### Supplemental Linux Maven evidence

1. `/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=com.onecall.archtests.sharing.CustomerShareCommandServiceTests test`: compilation succeeds; exit 1, 4 tests / 0 failures / 4 errors / 0 skipped. Mockito initialization fails in the sandbox: `Could not initialize inline Byte Buddy mock maker.`; `It appears as if your JDK does not supply a working agent attachment mechanism.`; `Could not self-attach to current VM using external process - set a property net.bytebuddy.agent.attacher.dump to dump the process output to a file at the specified location`. Log: `/tmp/pa14-pr22-review-fix1-unit-linux-maven.log`.
2. `/opt/maven-3.9.9/bin/mvn -B -ntp -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar -Dtest=com.onecall.archtests.sharing.CustomerShareCommandServiceTests test`: passes, exit 0, 4 tests / 0 failures / 0 errors / 0 skipped. The invocation-only agent avoids unavailable self-attachment; no test or build file changes. Log: `/tmp/pa14-pr22-review-fix1-unit-agent.log`.
3. `/opt/maven-3.9.9/bin/mvn -B -ntp -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar -Pcontainer-smoke -Dit.test=com.onecall.archtests.container.CustomerShareCommandsIT verify`: exit 1 in Surefire, 77 tests / 0 failures / 21 errors / 0 skipped. Web server startup is denied with `java.net.SocketException: Operation not permitted`; Failsafe is not reached. The changed service's 4 tests pass again. Log: `/tmp/pa14-pr22-review-fix1-it-agent.log`.
4. `/opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke -Dit.test=com.onecall.archtests.container.CustomerShareCommandsIT failsafe:integration-test failsafe:verify`: diagnostic direct execution using the compiled test classes; exit 1, 1 class-setup error / 0 failures / 0 skipped. Exact exception: `java.lang.IllegalStateException: Could not find a valid Docker environment. Please see logs and check configuration`, from `CustomerShareCommandsIT.startDatabase`. None of its 8 test methods executes. The Docker CLI probe also reports `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`. Log: `/tmp/pa14-pr22-review-fix1-it-direct.log`.

These supplemental attempts do not turn either required wrapper command into a pass. No assertion, skip flag or repository configuration was changed to bypass an environment failure.

## Source / CI / XML implications

- Compared test method inventories with frozen HEAD: the changed unit class remains exactly 4 named tests; the changed IT remains exactly 8 named tests. No method names or counts change.
- Read-only extraction of the current CI inventories and comparison with source succeeds: 16 Surefire classes / 77 tests; 6 Failsafe classes / 108 tests; 185 combined. These are the frozen slice's actual CI/source counts, not the plan's prospective full-work-unit 79/118/197 counts, which are also untouched.
- Current service Surefire XML confirms 4 tests / 0 failures / 0 errors / 0 skipped. The supplemental full verification's current Surefire XML totals are 16 classes / 77 tests / 0 failures / 21 errors / 0 skipped, matching the socket failures above.
- Current target IT XML records a single class-setup error, not eight successful method results. Other pre-existing container XML is not evidence for this fix. A fresh green full source/CI/XML reconciliation is not claimed.
- CI, POM, README and test inventory definitions are unchanged. No PR or CI success is observed or asserted.

## Docs decision and scope

Frozen `docs/14-production-data-model-and-flyway-schema-spec.md` §§2.5/2.12 is authoritative and unchanged. The plan's three contradictory canonical-data passages now consistently use the exact persisted member names and explicitly retain RFC 8785; the wrapper formulas are replaced, not retained under an exception. No product or schema decision is invented. Docs/12, docs/15, V001–V010, OpenAPI, CI, README, Control and prior review artifacts remain untouched.

No staging, commit, push, branch change, PR mutation, review action, delegation or gate transition was performed. The pre-existing untracked `.codex` entry is not part of this fix and is untouched.

## Residual risks and exit decision

The bounded code, test oracle, helper and plan corrections are implemented, and the targeted unit suite passes with the documented invocation-only environment adaptation. PostgreSQL 16 read-back, replay/concurrency and transaction behavior under the corrected digests remain unobserved because Docker access is denied in this runner; full verification also cannot start web sockets. No migration/backfill, historical-row rewriting or compatibility scheme is added or authorized.

Status remains `needs-more-evidence`, not `fix-complete`, pending successful required validation in an environment that can execute the existing wrapper and access Docker/PostgreSQL and web sockets. Do not expand this write scope or alter frozen contracts to make validation pass.
