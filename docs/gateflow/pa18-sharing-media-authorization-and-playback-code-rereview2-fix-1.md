# PA18 Second Re-Review Fix 1 — Controller Evidence Closeout

## Provenance and status

- Current task: `pa18-sharing-media-authorization-rereview2-fix1-report-gpt-20261107`. Prior compile correction: `pa18-sharing-media-authorization-rereview2-fix1a-compile-gpt-20261107`; prior accepted test-boundary repair: `pa18-sharing-media-authorization-rereview2-fix1-test-boundaries-gpt-20261107-retry1`.
- Date: November 7, 2026. Gate: current `fix` only. Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa18-sharing-media-authorization-and-playback/codex-call`.
- Runtime: Codex CLI coding agent. The requested model/provider name is not independently verified by this artifact. No delegation or peer review was performed.
- Inputs: existing accepted repair/compile evidence and the supplied confirmed Controller validation after RH-01/RH-02/RH-03 and the compile correction. The accepted production behavior remains unchanged; this task changes documentation only and does not rerun Maven/Docker.
- Bounded outcome: RH-01/RH-02/RH-03 implementation and Controller intake are complete. Controller accepted these repairs and added the compile correction: the existing cross-purpose/changed-fingerprint test declares `throws Exception`, matching neighboring tests and repairing the unhandled `IOException` contract without changing assertions, scenarios, names or counts. Controller focused/full validation is complete as recorded below; only fresh independent A/B remains pending. No reviewer has accepted this second fix; no independent review closure, gate pass or slice commit/readiness/PR claim is made.

## Compile correction and completed Controller validation

The prior compile correction added only `throws Exception` to `sameAuthorizationRequestAcrossPurposesConflictsWithoutSecondCredentialOrEvent`. All prior accepted test-boundary repairs below are preserved. Current method counts remain `ShareAdmissionServiceTests=21`, `PublicShareWebTests=12`, `PublicShareAdmissionIT=37`. No Java edit is made by this documentation task.

Confirmed Controller evidence after all repairs and the compile correction:

- Focused `ShareAdmissionServiceTests` + `PublicShareWebTests` passed twice: **21/21 and 12/12 each time**, zero failures/errors/skips.
- The first full run failed only because stale `TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1` could not reach Dify-engine containers. This is an environment diagnosis, not a code failure. The corrected full run used the same Linux Java 21 Maven container with `TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1`; unfiltered `mvn -q -Pcontainer-smoke verify` exited **0**.
- Surefire **20 XML classes / 119 tests**; Failsafe **9 XML classes / 176 tests**; combined **295**, zero failures/errors/skips throughout; PostgreSQL **16.15**. Exact source=CI=XML inventory passes **119 / 176 / 295**; `PublicShareAdmissionIT=37`, `SharingAdmissionRoleLeastPrivilegeIT=9`.
- Actual-method inventory remains `103+162=265 -> 119+176=295`. Separately labelled comment-inclusive word-boundary line scan remains `266 -> 296`, not a method count.
- Controller confirms exactly the frozen 17 implementation paths; V001-V012, OpenAPI and POM have no diff/status change; canonical LF and `git diff --check` pass.

This is supplied Controller evidence, not a documentation-author rerun, external CI result or independent A/B acceptance. Earlier blocked attempts below remain historical environment evidence only.

### Historical local compile-correction attempts

After the compile edit, the following exact focused unit command was attempted twice:

```bash
export DOCKER_HOST=unix:///var/run/dify.sock
docker run --rm --network host \
  -e DOCKER_HOST=unix:///var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1 \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -v /mnt/c/Users/Administrator/.codex/worktrees/pa18-sharing-media-authorization-and-playback/codex-call:/workspace \
  -v /root/.m2:/root/.m2 \
  -v /var/run/dify.sock:/var/run/docker.sock \
  -w /workspace/backend \
  docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21 \
  mvn -q -Dtest=ShareAdmissionServiceTests,PublicShareWebTests test
```

The approved focused PostgreSQL path was also attempted once with the same export and Docker prefix, replacing Maven arguments with `mvn -q -Pcontainer-smoke -Dit.test=PublicShareAdmissionIT -DskipTests=false verify`. All three commands exited 1 before container/JVM startup:

```text
permission denied while trying to connect to the docker API at unix:///var/run/dify.sock
```

No Docker Desktop proxy, socket permission change, daemon configuration change or alternative runtime was used in those attempts. Logs: `/tmp/pa18-rereview2-fix1a/unit-1.log`, `/tmp/pa18-rereview2-fix1a/unit-2.log`, `/tmp/pa18-rereview2-fix1a/it.log`. Those attempts generated no passing XML and could not assess focused PostgreSQL reliability. Their limitations are superseded by Controller's completed focused and unfiltered full validation above; they are not reclassified as green.

The historical compile-correction write set was exactly the single Java test above and the two allowlisted evidence docs. Its task-entry SHA-256 comparison excluded generated `target` and `.git` paths and preserved every other file; its static checks verified the one-line Java change, unchanged method names/counts, canonical LF, all 17 implementation identity rows and `git diff --check`. No production, Control, CI, POM, OpenAPI, migration/ACL or other test edits were made in that correction; no commit/stage/push/PR/approval/ready/reviewer request/external comment or other gate was performed.

Current Java identity, recomputed for this closeout: 718 LF lines / 62481 bytes / SHA-256 `683d66b7346a8614007ff7ad8064ad0cb9f600024b16c9349138bbd89393a9cd`. The current three-test and implementation-report identities are recorded below. Prior manifests under `/tmp/pa18-rereview2-fix1a/` remain historical, not final identities for this documentation revision.

## Repairs — implementation and Controller intake complete

Controller accepted RH-01/RH-02/RH-03 below and added the compile correction. Each finding is implemented and Controller-validated; independent acceptance remains pending fresh A/B only. These dispositions do not replace or claim either reviewer's verdict.

### RH-01 — provider validation

- Expanded `providerFailureAppendsOnlyDeterministicFailureAndRetrySucceeds` without adding a method: 2049 Unicode code points (supplementary characters), embedded NUL, unpaired high surrogate, null expiry, expiry one nanosecond before fixed `NOW`, and expiry exactly equal to fixed `NOW`.
- Each rejected result is exercised twice through the real media service and projected through `CustomerProblemWriter`: exact 503 `DEPENDENCY_UNAVAILABLE`; no returned success; exactly one deterministic `FAILURE`/`FAILED`, reason `MEDIA_CREDENTIAL_MALFORMED`; INSERTED then REUSED_IDENTICAL; no credential cache, success event, admission insertion or count update.
- Expanded the existing PostgreSQL provider/failure matrix with over-length, NUL, unpaired-surrogate, null-expiry and past-expiry modes. Existing independently derived event identity, canonical payload/hash, redaction, deterministic retry and no-state-mutation assertions apply to every new mode. Equality uses the controlled service fixture rather than a wall-clock estimate of database decision time.

### RH-02 — missing media service

- Expanded the existing web method that already covers missing service behavior with the exact media POST path, valid JSON and content type, absent media bean, and exact 503 `SERVICE_UNAVAILABLE` through the real filter/controller/problem writer.
- Public query, admission, media, context and verifier mocks have zero interactions; the POST is not replaced by a public GET assertion.

### RH-03 — order and changed fingerprint

- Read-only non-locking token/grant discovery is recorded in the shared service trace. The member assertion requires exactly `discovery < resolver < org-lock < membership-lock < account-lock < grant-lock`, with resolver count one. A non-member call supplies a resolver and proves count zero with exactly discovery then grant lock.
- Expanded the existing cross-purpose conflict method with a synthetic same-purpose prior SUCCESS: same request digest, event type and deterministic event identity, but an independently constructed fingerprint using a different session expiry. The fixture contains this event before the only authorization call; there is no provider warm-up.
- Exact `SHARE_GENERATION_STALE` and 409 are asserted via the real problem writer; provider call list is empty, existing event object/payload/hash remain identical, no event insert occurs, and no admission/count/grant-expiry write occurs. No OpenAPI code is added or changed.

## Historical test-boundary validation and limitation

The required focused command was attempted **twice** (both exit 1 before a container or JVM starts):

```bash
docker run --rm --network host \
  -e DOCKER_HOST=unix:///var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1 \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -v /mnt/c/Users/Administrator/.codex/worktrees/pa18-sharing-media-authorization-and-playback/codex-call:/workspace \
  -v /root/.m2:/root/.m2 \
  -v /mnt/wsl/docker-desktop/shared-sockets/guest-services/docker.proxy.sock:/var/run/docker.sock \
  -w /workspace/backend \
  docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21 \
  mvn -q -Dtest=ShareAdmissionServiceTests,PublicShareWebTests test
```

The focused PostgreSQL command used the same Docker prefix and approved existing profile/lifecycle, with Maven arguments `mvn -q -Pcontainer-smoke -Dit.test=PublicShareAdmissionIT -DskipTests=false verify`. It was attempted once, exit 1 before startup. Actual output for all three attempts:

```text
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
```

A read-only daemon probe using the mounted Docker Desktop proxy directly was also denied. No socket permissions, daemon setup or environment configuration were changed. Local default Java points to a Windows binary and fails under this environment; it is not substituted for Linux Java 21. Temporary attempt logs are `/tmp/pa18-rereview2-fix1/unit-1.log`, `/tmp/pa18-rereview2-fix1/unit-2.log` and `/tmp/pa18-rereview2-fix1/it.log`.

**Historical limitation, now superseded by Controller evidence above:** those local attempts could not assess focused Failsafe reliability because Docker never started. Controller subsequently completed both focused unit passes and the corrected full Linux Java 21 `container-smoke` run, including all 37 `PublicShareAdmissionIT` tests. This documentation task makes no new Maven/Docker run. Only fresh isolated independent A/B review remains pending.

Historical test-boundary task static checks (not a new assertion-execution claim):

- Extracted and executed the repository's unchanged CI inventory Python guard against current source/CI and **historical existing XML**: 20 Surefire classes / 119 methods, 9 Failsafe classes / 176 methods, 295 combined; exact name sets match, historical XML has zero failures/errors/skips. This is an inventory audit only, not evidence that new assertions executed.
- Target method counts/names unchanged: `ShareAdmissionServiceTests=21`, `PublicShareWebTests=12`, `PublicShareAdmissionIT=37`; actual-method recipe `103+162=265 -> 119+176=295`; separately labeled comment-inclusive line scan `266 -> 296` remains unchanged.
- Repository-wide Java `assertThat(true)` count is zero. All five outputs are UTF-8 without BOM, canonical LF, final newline, no CR bytes. `git diff --check` and task-entry diff whitespace checks pass.
- Windows-style worktree `.git` metadata is read-only. Git checks explicitly use `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call18 --work-tree=...`; no metadata repair, staging or Git configuration mutation occurs.
- Task-entry raw SHA-256 snapshot comparison excludes generated `target` and `.git` paths and requires exactly the five-path write set below; existing implementation changes are preserved, not attributed to this task. All 17 implementation inventory identities in the implementation report are checked, with only the three affected test rows refreshed.

## Historical five-path write set and identities

The historical test-boundary task wrote three existing test files plus the implementation report and this fix artifact. Production, POM, OpenAPI, migrations, ACL, CI, Control, accepted plan and all existing reviews were unchanged relative to that task's entry. No commit, staging, push, PR mutation, external comment, approval, ready action, reviewer request or other gate was performed.

## Current evidence-closeout scope and identities

Only `docs/gateflow/pa18-sharing-media-authorization-and-playback-implementation.md` and this artifact change in the current task. All source, tests, Control, CI, POM, OpenAPI, V001-V012, plans and reviews remain read-only and unchanged relative to closeout entry. Task-entry raw SHA-256 comparison excludes generated `target` and `.git` paths and verifies exactly these two changed docs. Both docs are UTF-8 without BOM, canonical LF, no CR, with final newline; `git diff --check` passes. Existing implementation changes are preserved, not attributed to this task. No Maven/Docker rerun, commit, staging, push, PR, external comment, approval, ready action, reviewer request, Control edit or other gate is performed.

Line counts count LF bytes; SHA-256 covers raw bytes. Recomputed current identities are:

| Path | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | 1252 | 109286 | `f2ffb3ba5b3fcf1b8e29f48160b65cf2c1db91514783cb8cf1147e5410e401bf` |
| `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java` | 514 | 42736 | `ef7c77a2181f4f94c88e3178c85c8e9ffda35d6344743d9d3484b156fd52d8a0` |
| `backend/src/test/java/com/onecall/archtests/sharing/ShareAdmissionServiceTests.java` | 718 | 62481 | `683d66b7346a8614007ff7ad8064ad0cb9f600024b16c9349138bbd89393a9cd` |
| `docs/gateflow/pa18-sharing-media-authorization-and-playback-implementation.md` | 89 | 20888 | `3093813dc0c78c1659ff38d27fc1cd1982a1da1a516ec9f15e58121c550cbf74` |

The fifth identity is for `docs/gateflow/pa18-sharing-media-authorization-and-playback-code-rereview2-fix-1.md` (this artifact). Its raw line/byte/SHA-256 identity is recomputed only after content is final and emitted with all four identities above in the verification transcript and `/tmp/pa18-rereview2-fix1-report/final-identities.txt`; it is not embedded here, avoiding a self-referential hash. That temporary manifest is not a repository output; Controller refreeze can independently measure the final raw identity.

## Preserved boundaries and next step

All prior finding dispositions, rejected items and owned deferrals remain as recorded in the implementation report and prior fix artifacts. A R-A18-06's provider-concurrency residual (concurrent first calls under partial outage can append both SUCCESS and FAILURE) remains with Sharing backend/provider integration plus Platform/Security. B-R-03's durable-failure residual (database-down insertion cannot guarantee a FAILURE row while fail-closed) remains with Sharing persistence/backend plus Platform database reliability. Sequential replay tests do not close either residual. Production Q22 TTL/skew/revocation/signing/Redis/provider settings remain unavailable and fail closed; deterministic fixtures do not certify production-provider idempotency or deployment. Media gateway/COS/object resolution, streaming/range delivery and buffered-byte recall remain outside PA18.

Controller refreeze and fresh independent A/B are next. No reviewer has accepted the second fix, and this closeout claims no independent closure, re-review pass, readiness, merge, approval, PR or external CI.

second re-review fix 1 evidence closeout complete; Controller refreeze and fresh A/B next
