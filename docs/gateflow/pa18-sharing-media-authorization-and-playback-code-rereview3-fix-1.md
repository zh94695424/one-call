# PA18 third re-review fix 1 — Controller evidence closeout

## Task and boundary

- Task: `pa18-sharing-media-authorization-rereview3-fix1-report-gpt-20261107`; November 7, 2026, Asia/Shanghai; bounded documentation closure in the current Gateflow `fix` only. Prior test-repair task: `pa18-sharing-media-authorization-rereview3-fix1-test-execution-gpt-20261107`. Runtime: Codex CLI; no independently verified model/provider override is claimed.
- Inputs: `docs/reviews/code-review-20261107-212800.md` (accepted A LR-01/LR-02) and `docs/reviews/code-review-20261107-212801.md` (context only). B LOW-1/LOW-2 are explicitly deferred outside this fix; production code is unchanged.
- Write scope: only the existing implementation evidence report and this existing report. The preceding repair changed the two existing tests; this closeout does not change tests or any implementation path. No new test methods; service/web/media IT method counts remain **21 / 12 / 37**.

## Finding dispositions

| Finding | Repair present in source | Validation boundary |
|---|---|---|
| A LR-01 — equivalent HTTP/witness expiry text | `PublicShareAdmissionIT.java:1032` parses both HTTP `expiresAt` and durable witness expiry using `Instant.parse` before comparing. The exact payload, hash, credential, response-key and redaction assertions remain unchanged. | Fixed and Controller-validated by the post-repair unfiltered Linux Java 21/PostgreSQL 16.15 verification, including media IT 37/37. Only final fresh independent A/B remains pending. |
| A LR-02 — current-account identity/status controller branches | `PublicShareWebTests.java:389` adds an ACTIVE context account with a different ID, expecting exact 403 `SHARE_WRONG_TENANT`; `:392` adds the bearer account with existing `AccountStatus.DISABLED`, expecting exact 403 `FORBIDDEN`. Both retain one active organization and use the existing `WorkspaceDenial` matrix inside `organizationMemberMediaAuthorizationRequiresTrustedCurrentSession`. | Fixed and Controller-validated: web 12/12 twice and in full verification. The real filter/controller/problem writer calls the captured resolver; `problem` pins HTTP/JSON status/code, title, headers and shape. Every case clears invocations and asserts lazy access, exactly one verifier/context/media-service call, the verified account/workspace argument and no more interactions. Only final fresh independent A/B remains pending. |
| B LOW-1 — duplicate fingerprint/event derivation | Deferred by Controller as Sharing backend maintainability, not a current-scope defect. | Owner: Sharing backend maintainability; future disposition remains with Controller / Sharing backend maintenance. No production change. |
| B LOW-2 — optional defensive checks | Deferred by Controller: optional defensive `bound()` branches are Sharing backend maintainability, not current-scope defects. | Owner: Sharing backend maintainability; future disposition remains with Controller / Sharing backend maintenance. No production change. |

These are fixed, Controller-validated repairs, not independent reviewer acceptance or a gate pass. No reviewer has accepted this final test fix; no slice commit/readiness/PR is authorized.

## Completed Controller validation

- Supplied Controller evidence after LR-01/LR-02: focused Surefire passed twice with `ShareAdmissionServiceTests=21/21` and `PublicShareWebTests=12/12`, zero failures/errors/skips on each run.
- Unfiltered Linux Java 21 `mvn -q -Pcontainer-smoke verify` exited **0** on PostgreSQL **16.15**. Surefire: **20 classes / 119 tests**; Failsafe: **9 classes / 176 tests**; combined **295**, zero failures/errors/skips. `PublicShareAdmissionIT=37`; ACL `SharingAdmissionRoleLeastPrivilegeIT=9`.
- Exact source=CI=XML inventory passes **119 / 176 / 295**; service/web/media IT method counts remain **21 / 12 / 37**. Exactly the frozen **17 implementation paths**; V001-V012/OpenAPI/POM unchanged; canonical LF and `git diff --check` pass.
- These are current Controller results on the repaired sources, not a documentation-author rerun, independent acceptance or external CI. No Maven/Docker was rerun for this closeout.

## Historical local blocked attempts

At **2026-11-07T21:54:55+08:00**, the exact focused Docker/Surefire command below was attempted **twice** in the prior test-repair task. Each command exited **1** before Maven or Java started, with `permission denied while trying to connect to the docker API at unix:///var/run/dify.sock`. No container was created and neither suite executed in those attempts. They remain blocked attempts, not passing results; the subsequent Controller runs above establish the required passes on the repaired sources.

```bash
export DOCKER_HOST=unix:///var/run/dify.sock
docker run --rm --network host \
  -e DOCKER_HOST=unix:///var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1 \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -v /mnt/c/Users/Administrator/.codex/worktrees/pa18-sharing-media-authorization-and-playback/codex-call:/workspace \
  -v /root/.m2:/root/.m2 \
  -v /var/run/dify.sock:/var/run/docker.sock \
  -w /workspace/backend \
  docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21 \
  mvn -q -Dtest=ShareAdmissionServiceTests,PublicShareWebTests test
```

- Local attempt logs: `/tmp/pa18-rereview3-fix1/surefire-attempt-1.log` and `/tmp/pa18-rereview3-fix1/surefire-attempt-2.log`; each is 3 LF lines / 129 bytes / SHA-256 `a4766e78d65c1a20a9a783d1b14156115e292610b9c9b74aa2b0492e77e14291` (identical timestamps and output). These are blocked-attempt logs, not test reports.
- During that prior task the Dify socket existed; even the read-only `docker info` probe was denied. No Docker Desktop proxy, alternative daemon, permission workaround or elevated execution was used. Java 21 execution was not verified by those attempts; subsequent Controller Linux Java 21 validation is separately established above.
- Focused/full PostgreSQL tests were not attempted locally with the daemon inaccessible. Surefire XML from 21:15 and Failsafe XML from 21:17 on November 7, 2026 predate the repair and remain historical only; they are not substituted for current post-repair Controller results.
- Prior static inspection confirmed method inventories **21 / 12 / 37**; byte diffs against test-repair task-start copies showed only six web-matrix lines added and the one IT assertion replaced. No payload/hash assertion or method was removed. The prior local audit records UTF-8/LF, whitespace and four-path test-repair scope; the current closure validates only its two-document write scope, encoding, identities and consistency.
- The checked-out `.git` file uses a Windows `D:/...` path that plain WSL Git cannot resolve. Read-only Git inspection instead uses `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call18 --work-tree=<workspace>`; no Git metadata/configuration is edited. `git diff --check` passes; Git's existing autocrlf warnings do not change raw file bytes.

## Raw identities

Lines count LF bytes; bytes and SHA-256 cover the complete raw file. The service row is an unchanged count reference, not an additional write path.

| Path | Lines | Bytes | SHA-256 |
|---|---:|---:|---|
| `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java` | 520 | 43365 | `683bf271fb37975d95aea3474a91845ab4179a3f58f4f138a1a860e5a973a8cc` |
| `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | 1252 | 109316 | `4da7cde6f9226eda0c3700faef088a0abce489e067727c785dc6acde1cad08bd` |
| `backend/src/test/java/com/onecall/archtests/sharing/ShareAdmissionServiceTests.java` (unchanged) | 718 | 62481 | `683d66b7346a8614007ff7ad8064ad0cb9f600024b16c9349138bbd89393a9cd` |
| `docs/gateflow/pa18-sharing-media-authorization-and-playback-implementation.md` | 100 | 24652 | `001b4dca56fd4cae954570121e3353ac64d00f139062831a505b9bf24f59795a` |

All target test identities above were recomputed from current raw bytes; tests remain unchanged. This report records the implementation report identity in one direction only; neither report embeds its own identity, and the implementation report does not embed this report's identity. The prior `/tmp/pa18-rereview3-fix1/final-identities.tsv` and `/tmp/pa18-rereview3-fix1/static-audit.log` belong to the earlier test-repair task, not the current documentation identities. Local `/tmp` artifacts are historical supporting evidence, not CI or persistent repository artifacts.

## Residuals and pending Controller work

- A LR-01/LR-02 are fixed and Controller-validated; focused passes twice, full Linux Java 21/PostgreSQL verification and source=CI=XML inventory are complete. The historical daemon blocker is superseded, not recast as a passing attempt.
- Only final fresh peer-isolated A/B remains pending for these repairs; no reviewer has accepted the final test fix. Controller adjudication/refreeze is next. No independent closure, slice commit/readiness/PR authorization, review request or gate transition is claimed or performed.
- B LOW-1 duplicate fingerprint/event derivation and B LOW-2 optional defensive checks remain deferred maintainability residuals owned by Sharing backend maintainability, under Controller / Sharing backend maintenance disposition; neither is a current-scope defect.
- A R-A18-06 remains owned by Sharing backend/provider integration plus Platform/Security (concurrent SUCCESS/FAILURE under partial provider outage). B-R-03 remains owned by Sharing persistence/backend plus Platform database reliability (failure-event persistence while DB is down). Neither is repaired by this task.
- Production provider idempotency, TTL/skew/revocation/signing/Redis configuration remain fail-closed integration residuals owned by Backend/Platform/Security; deterministic fixtures do not certify a production provider or deployment. Media gateway/COS/object resolution, streaming/range and buffered-byte recall remain outside PA18.
- Only the two existing evidence reports change; no test, production, Control, CI, POM, OpenAPI, migration, plan or existing review edits; no commit, stage, push, PR, approval, ready action, reviewer request, external comment or other gate.

third re-review fix 1 evidence closeout complete; Controller refreeze and final A/B next
