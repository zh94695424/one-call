# PA18 fourth re-review fix 1 — canonical provider-expiry precision evidence closeout

## Identity and scope

- Current task: `pa18-sharing-media-authorization-rereview4-fix1-report-gpt-20261107`; preceding repair task: `pa18-sharing-media-authorization-rereview4-fix1-expiry-precision-gpt-20261107`.
- Date: **2026-11-07**, timezone **Asia/Shanghai (+08:00)**; Gateflow **fix**.
- Runtime: Codex CLI, direct local work, no subagents. The task-assigned identity is GPT `gpt-6.1-sol`, reasoning effort `high`; backing model routing is not independently attested by terminal evidence.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa18-sharing-media-authorization-and-playback/codex-call`.
- Accepted input: fourth-round A Finding 1 in `docs/reviews/code-review-20261107-221200.md`. It is **fixed and Controller-validated, pending only final fresh A/B**. No reviewer has accepted the final canonicalization repair. B's fourth-round no-new-finding verdict is preserved, not treated as acceptance of this final fix; no peer fourth-round artifact was consulted for the preceding repair.
- Current write allowlist: only `docs/gateflow/pa18-sharing-media-authorization-and-playback-implementation.md` and this existing report. The preceding repair's five-path scope comprised `DefaultShareMediaAuthorizationService.java`, `ShareAdmissionServiceTests.java`, `PublicShareAdmissionIT.java` and these two documents. All source/tests/Control/CI/POM/OpenAPI/migrations/reviews and earlier evidence artifacts are read-only for this closeout; pre-existing worktree changes are not this task's writes.

## Correction and coverage

The service previously validated a raw nanosecond expiry, formatted it to durable microseconds, then compared the parsed durable expiry against the raw provider value after committing SUCCESS. A valid `bound.minusNanos(1)` could therefore receive 503 on INSERTED, followed by 200 on replay. This accepted finding supersedes the historical “unreachable INSERTED/expiry-mismatch” characterization; the defensive mismatch check itself remains in place.

Immediately after provider issuance, `providerExpiry` now preserves null or truncates the provider's `Instant` once with `ChronoUnit.MICROS`. All provider-expiry future/bound checks, success-payload formatting, durable-expiry fallback and INSERTED equality checks use this canonical value. The response continues to use the first durable expiry, so initial success and replay expose the same canonical instant. The raw provider expiry has no later semantic use. Null, equal-time or past canonical expiry is still malformed and fails closed through the existing failure/error contract. No new API error, schema, ACL, OpenAPI field, provider policy or work-unit behavior is introduced.

Existing methods/fixtures were expanded, not multiplied:

- Service `sameAuthorizationRequestAndPurposeReplaysCredentialAndEvent` retains its normal provider case and adds a deterministic `witness.sessionExpiresAt().minusNanos(1)` provider result. It asserts successful first return, canonical `2026-01-01T00:07:59.999999Z` response/payload, exactly one `PLAYBACK_AUTHORIZED` SUCCESS, equal replay, INSERTED then REUSED_IDENTICAL and two provider calls. No exception-to-retry accommodation is used.
- Service `providerFailureAppendsOnlyDeterministicFailureAndRetrySucceeds` adds `NOW.plusNanos(1)`. The raw expiry is future, but its microsecond truncation equals NOW; the existing matrix asserts repeated 503 / malformed-provider FAILURE, no SUCCESS and deterministic failure replay. Existing null/equal/past rejection cases remain.
- Media IT fixture adds `SUB_MICROSECOND` through the existing deterministic issuance cache. `mediaAuthorizationUsesExactRouteAndFrozenResponse` uses `request.credentialExpiresAt().minusNanos(1)` and asserts first HTTP 200, canonical response instant, exact six-digit durable expiry, one durable SUCCESS, structurally identical JSON replay at HTTP 200, unchanged durable event, zero FAILURE, two provider calls and one semantic credential issuance. The expanded IT passed in the Controller's post-canonicalization unfiltered PostgreSQL run below.

## Completed Controller validation

The supplied Controller evidence applies to the final canonicalized source identities in the table below:

- Focused Surefire passed **twice**, service **21/21**, web **12/12**, each with zero failures/errors/skips.
- Unfiltered Linux Java **21** `mvn -q -Pcontainer-smoke verify` exited **0** on PostgreSQL **16.15**. Surefire: **20 classes / 119 tests**; Failsafe: **9 classes / 176 tests**; combined **295**, zero failures/errors/skips throughout.
- Full exact **source=CI=XML 119/176/295** audit passed; `PublicShareAdmissionIT=37`, ACL `SharingAdmissionRoleLeastPrivilegeIT=9`. Service/web/media IT method counts remain **21/12/37**.
- Controller confirmed exactly **17 implementation paths**, V001–V012/OpenAPI/POM unchanged, canonical LF and passing `git diff --check`.

These are supplied Controller results after the canonicalization repair, not an independent closeout-author rerun, reviewer acceptance or external CI. They supersede the earlier local Docker blocker without recasting its failed attempt as a pass. No Maven or Docker command is rerun during this documentation-only task. Only final fresh independent A/B of this final fix remains pending; Controller refreeze is next.

## Historical local validation commands and outcomes

The preceding repair's native Maven invocations ran from `backend` with:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:$PATH
export DOCKER_HOST=unix:///var/run/dify.sock
export TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1
```

`java -version` identifies Linux Temurin **21.0.13+9-LTS**, and Maven identifies **3.9.9** on Linux amd64. No POM, CI or repository configuration was changed.

| Attempt | Exact Maven arguments after `/opt/maven-3.9.9/bin/mvn` | Outcome |
| --- | --- | --- |
| Initial environment diagnosis | `-B -ntp -Dtest=ShareAdmissionServiceTests,PublicShareWebTests test` | Exit **1**, finished **22:45:47 +08:00**; test compilation succeeds including the changed media IT. Surefire **33 / 0 failures / 31 errors / 0 skips**: Mockito's inline mock maker cannot self-attach through an external process. Preserved, not classified as an assertion failure or a green run. |
| Focused run 1 | `-B -ntp -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar -Dtest=ShareAdmissionServiceTests,PublicShareWebTests test` | Exit **0**, **11.951 s**, finished **22:46:25 +08:00**; service **21**, web **12**, total **33**, zero failures/errors/skips. |
| Focused run 2 | Same exact arguments as focused run 1 | Exit **0**, **11.968 s**, finished **22:47:18 +08:00**; service **21**, web **12**, total **33**, zero failures/errors/skips. |

Using Mockito's existing startup agent is a command-line environment adjustment only; no tests, assertions, mock implementation or dependencies were disabled or weakened.

The requested socket-mounted focused PostgreSQL attempt was made from the worktree root:

```bash
DOCKER_HOST=unix:///var/run/dify.sock docker run --rm --network host \
  -v /var/run/dify.sock:/var/run/docker.sock \
  -v "$PWD":/workspace -v /root/.m2:/root/.m2 -w /workspace/backend \
  -e DOCKER_HOST=unix:///var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1 \
  maven:3.9.9-eclipse-temurin-21 mvn -B -ntp -Pcontainer-smoke \
  -Dtest=ShareAdmissionServiceTests,PublicShareWebTests \
  -Dit.test=PublicShareAdmissionIT verify
```

It exited **1** with `permission denied while trying to connect to the docker API at unix:///var/run/dify.sock`, before container creation or Maven/PostgreSQL execution. A preceding `docker info` and image-list probe hit the same permission denial. The sandbox did not permit escalation, so no workaround changing permissions, Control or daemon configuration was attempted. This historical attempt is **not a media IT pass**; that local repair task did not run full `container-smoke`. The Controller subsequently completed the authoritative unfiltered run recorded above on the final sources. Historical Failsafe XML is not substituted for that post-repair evidence.

### Exact source and report identities

Raw files are **UTF-8 without BOM, canonical LF**. Source lengths include final LF:

| File | Lines | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| `backend/src/main/java/com/onecall/core/sharing/internal/admission/DefaultShareMediaAuthorizationService.java` | 231 | 21424 | `1ea53fbadc350f6677b98fb06e38edb33b9ae5c11092894bb755db0d9692f847` |
| `backend/src/test/java/com/onecall/archtests/sharing/ShareAdmissionServiceTests.java` | 731 | 63638 | `2418a638e3c242d4722f8b573048b28f49fadbdf7bc85aef1e18f636a4e46cc8` |
| `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | 1270 | 110836 | `6c5eb47ec4f98d2ffaa7bf953dd2b0d5e5b1c97097df2bd6e64baa321758a35c` |
| Unchanged `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java` | 520 | 43365 | `683bf271fb37975d95aea3474a91845ab4179a3f58f4f138a1a860e5a973a8cc` |
| Unchanged `.github/workflows/ci.yml` | 1068 | 67853 | `5fa2243a674c2448619f9baef80c772a8196e6ae04927789214517cddcd38225` |
| `docs/gateflow/pa18-sharing-media-authorization-and-playback-implementation.md` (current evidence closeout) | 114 | 30170 | `ba725063cad491c4a2c3bd920676df187209c0206abe4c03ee3e3b249ce5b0d4` |

All documented current source identities were recomputed from raw bytes; all 17 implementation-path identities are in the implementation report. This fix report does not embed its own identity; the implementation report embeds neither its own identity nor this report's identity, avoiding circular/self-referential hashes.

Historical local evidence root: `/tmp/pa18-rereview4-fix1/`. Live XML report paths at the end of focused run 2 were `backend/target/surefire-reports/TEST-com.onecall.archtests.sharing.ShareAdmissionServiceTests.xml` and `backend/target/surefire-reports/TEST-com.onecall.archtests.PublicShareWebTests.xml`; later runs may replace live reports. Separate copies preserve each attempt, and their hashes were rechecked during closeout. These are the earlier local focused/blocked artifacts, not hashes of the Controller's subsequent full run:

| Evidence relative to the evidence root | SHA-256 |
| --- | --- |
| `surefire-1.log` (failed self-attachment attempt) | `fc192f209aaaf7552b4f03341eea60b03078420bb61080df0ed6584717374457` |
| `surefire-agent-1.log` | `c8514cf29da54ea33a81b6f3e0d9b67539c4d26f639e97f9e59905d565cc22b2` |
| `surefire-agent-2.log` | `4b6be13b0c732d1a3cccb3fdf839ee092f701b69a281f7c8636abb1be0b1479e` |
| `media-it-container.log` (Docker denial) | `7edb01029eab45becf335fdf3fc2559295d31feb96c6e744323cc6cb614db735` |
| `initial-xml/TEST-com.onecall.archtests.PublicShareWebTests.xml` | `43ffeab0dd6c5d34ff7971f71241d352320c46192f485b741e16b313e4295809` |
| `initial-xml/TEST-com.onecall.archtests.sharing.ShareAdmissionServiceTests.xml` | `15050d423ec0e475558a4ad2656d6a4ee47d60da689e6c5e634acc8933adeadf` |
| `agent-1-xml/TEST-com.onecall.archtests.PublicShareWebTests.xml` | `2e31417e374b7851b2e21a5e0a796babfa2e5d2d7e71226543d70365d325b40e` |
| `agent-1-xml/TEST-com.onecall.archtests.sharing.ShareAdmissionServiceTests.xml` | `cb83ca04924284902709d5c76110da83d891b2c196ae0c00aa038efd565940c0` |
| `agent-2-xml/TEST-com.onecall.archtests.PublicShareWebTests.xml` | `1027b6d214cb888e47ca519fa4dfc65e1039bc2e6f7350fd28bce76e1681d097` |
| `agent-2-xml/TEST-com.onecall.archtests.sharing.ShareAdmissionServiceTests.xml` | `5ee12b2def14fa67d95430cb5aa196873c8dbedf14166bbdb611a5b3e2cc6907` |

### Inventory and scope checks

The preceding repair's `python3 /tmp/pa18-rereview4-fix1/check-inventory.py` extracted only inventory definitions from the unchanged CI Python block, compared exact source method names for all classes and checked both green focused runs' XML names/counts/outcomes. It passed: **20 Surefire classes / 119 methods**, **9 Failsafe classes / 176 methods**, combined **295**; service **21**, web **12**, media IT **37**, admission-role IT **9**. Actual-method baseline remains **103+162=265 -> 119+176=295**. The separately labelled comment-inclusive line scan remains **266 -> 296**, not a test count. Both focused runs match source=CI=XML for their **two classes / 33 tests**. The Controller subsequently completed the authoritative full post-canonicalization source=CI=XML audit, **119/176/295**, with zero failures/errors/skips as recorded above; it is no longer pending.

Historical repair scope: a pre-edit raw SHA-256 manifest of **1,094** files under `backend/src`, `docs`, `api`, `.github` plus `backend/pom.xml` is stored as `before.json` in the earlier evidence root. That repair's final comparison permitted only its five assigned paths; unchanged files included Control, CI, POM, migrations, OpenAPI, prior reviews/evidence and every other production/test file. The three Java files and both evidence documents passed UTF-8/no-BOM/LF/final-newline/trailing-whitespace checks, as did read-only `git diff --check`.

Current closeout scope: a separate pre-edit **1,994-file** worktree snapshot and document copies are stored under `/tmp/pa18-rereview4-fix1-report/`. The incremental comparison permits only the two existing evidence documents. Local validation is limited to that two-file scope, current documented identities, consistency, UTF-8/no-BOM/LF/no-CR/final-newline/trailing-whitespace and read-only `git diff --check`. Git's existing Windows `.git` pointer does not resolve natively; read-only checks use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call18` and `GIT_COMMON_DIR=/mnt/d/codex-call/.git`, without changing any Git configuration, index or metadata. No Maven/Docker, Control edit or other gate is performed.

## Dispositions and next gate

- Fourth-round A Finding 1 is **fixed and Controller-validated, pending only final fresh A/B**. Controller's full unfiltered Linux Java 21/PostgreSQL validation, full source=CI=XML **119/176/295** inventory and 17-path scope/LF audits are complete on the final canonicalized sources. Controller refreeze and final fresh independent A/B are next; no reviewer has accepted this final fix, and no independent closure or final gate acceptance is claimed.
- A LR-01/LR-02, RH-01/RH-02/RH-03, R-A18-01..04 and prior accepted repairs remain fixed. Their disposition history is unchanged. Earlier full-run evidence remains historical; it is not substituted for the supplied post-canonicalization validation.
- B's fourth-round **no-new-finding** verdict is preserved. B LOW-1 duplicate fingerprint/event derivation and B LOW-2 optional defensive `bound()` checks remain deferred maintainability residuals, not current-scope defects. Owner: Sharing backend maintainability; future disposition remains with Controller / Sharing backend maintenance. Neither is repaired or closed here.
- Existing residuals/owners are unchanged: A R-A18-06 provider concurrency remains with Sharing backend/provider integration plus Platform/Security (concurrent first calls may append SUCCESS and FAILURE under partial outage); B-R-03 durable-failure outage behavior remains with Sharing persistence/backend plus Platform database reliability (database-down insertion cannot guarantee a FAILURE row while the request stays fail-closed). Provider determinism remains with Sharing backend / Platform / Security. Production Q22 TTL/skew/revocation/signing/Redis/provider values remain unavailable and fail closed under Backend/Platform/Security and production integration owners. Deterministic fixtures do not certify production provider idempotency or deployment. Cross-purpose concurrency and media gateway/COS/object resolution, streaming/range delivery and buffered-byte recall remain outside this repair; sequential replay does not close these residuals.
- No stage, commit, push, PR, approval, readiness, reviewer request, external comment, Control edit or other gate was performed.

fourth re-review fix 1 evidence closeout complete; Controller refreeze and final A/B next
