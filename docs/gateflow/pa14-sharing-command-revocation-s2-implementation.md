# PA14 Revocation S2 — Implementation Evidence

- Task: `pa14-revocation-s2-impl-gpt-20261105`.
- Evidence reconciliation: `pa14-revocation-s2-evidence-fix-gpt-20261105`.
- Date: 2026-11-05, Asia/Shanghai.
- Assigned runtime/provider/model: Codex runner / `gpt` / `gpt-6.1-sol`, reasoning `high` (task assignment label).
- Work unit/gate: `PA14-sharing-command-revocation` / implementation S2.
- Baseline: `e99dc4f18dc697e897124716b9447b13db497600`, branch unchanged at `codex/pa14-sharing-command-revocation`.
- Accepted plan: `docs/gateflow/pa14-sharing-command-revocation-plan.md`, verified SHA256 `173c719cf9de20c25ea02c4acb0aaf681c3b71af5c6eea3404863f0fe0993912`.
- status: `implementation-complete`.
- Implementation code/tests/CI/README are complete within the authorized slice. Author-local sandbox attempts were historically blocked by loopback and Docker socket restrictions; Controller subsequently closed the loopback/Docker/PostgreSQL verification gap on the same implementation content. This is implementation evidence, not review acceptance or production readiness.

## Objective And Exact Changed Paths

Implement only `POST /api/v1/customer/shares/{shareId}/revocations`, retaining create, share-list and `/me` behavior, the existing command transaction/providers, and query/command separation. Rotation remains sanitized 404. No future slice or production policy/material decision is implemented.

Exactly these 15 existing paths are changed by this task:

1. `backend/src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommand.java`
2. `backend/src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommandResult.java`
3. `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java`
4. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandRepository.java`
5. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqSharingCommandRepository.java`
6. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`
7. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSharesController.java`
8. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandDtos.java`
9. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandJsonBinder.java`
10. `backend/src/test/java/com/onecall/archtests/sharing/CustomerShareCommandServiceTests.java`
11. `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java`
12. `backend/src/test/java/com/onecall/archtests/CustomerSharesCommandWebTests.java`
13. `backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java`
14. `.github/workflows/ci.yml`
15. `backend/README.md`

Exactly one new artifact is added: `docs/gateflow/pa14-sharing-command-revocation-s2-implementation.md`, explicitly authorized by the implementation task. No new production/test file, top-level type/export, dependency, provider, migration or configuration is added.

## Design And Ordering

- Added only nested command `Revoke` and result `Revoked`; reused existing state and enums. The service constructor and all create providers remain unchanged.
- The highest-priority Filter admits only exact revoke POST, authenticates first, enumerates the header once, validates exactly one lossless nonblank 1–128-code-point key, stores it unchanged, then validates JSON Content-Type. Single comma-containing values are not split. Create still ignores unrelated invalid/duplicate keys.
- The package-private static `CustomerBoundaryFilter.idempotencyKey(HttpServletRequest)` is the Controller's only key accessor. Controller source contains no `getAttribute(` or header re-enumeration. Path/trusted workspace/closed body validation precedes qualified command-provider resolution; invalid keys beat media/body/workspace failures. The local strict mapper rejects unknown/duplicate/trailing/null/type-invalid input without changing global binding.
- Revoke returns only the existing flat 200 ShareState with explicit null `maxAdmissions`; the existing sanitized Problem handler/writer remains unchanged. Rotate is never admitted. Existing create/list mappings and serializers remain intact.
- The existing single command TransactionTemplate dispatches validated Create/Revoke. Revoke performs command-role assertion → exact creator/workspace advisory ranks 7/8/9 (personal 9 only) → active caller context → owned/workspace-filtered explicit twenty-column `FOR UPDATE` → one microsecond database clock → deterministic action lookup.
- Event identity hashes the canonical locked grant UUID/action/exact raw source key. Request digest hashes the exact key's UTF-8 bytes. Fingerprint includes exact reason-or-null, null new expiry, action and request digest; witness/trace/current state/time are excluded. Only digests, not raw keys, are persisted.
- Exact persisted replay precedes terminal/expiry/witness checks and writes nothing. Changed semantics under the same event identity conflicts first. New REVOKED actions return 410; EXPIRED or elapsed/equal-expiry ACTIVE/LOCKED actions conflict; only compatible new actions compare an optional witness. No lower-valid-from, admission or creation/material-policy gate is added.
- New transitions append the exact canonical `SHARE_ACCESS`/`REVOKED`/`SUCCESS` event → conditionally change only status/revocation generation/revision with explicit twenty-column RETURNING → verify all twenty intended fields → append `SHARE_REVOKED` Outbox → append `SHARE_GRANT_REVOKE` audit. Generations/revision increment exactly once; code material and all other fields remain unchanged.
- The Outbox dedupe retains the literal double prefix `share-transition:share-transition:<64hex>`. Independent access/Outbox canonical payload hashes, sampled event/audit/Outbox-payload timestamps, exact reason, request digest and optional trusted audit trace follow the plan. No secret/raw key enters event/Outbox payloads.
- NotUpdated executes one owned-grant reread and throws a stable terminal/expiry/stale/conflict failure. It never retries UPDATE or treats its uncommitted event as successful replay. Throws propagate through the template to roll back the preceding event. SQL/decode failures remain controlled unavailable; unexpected append/commit failures retain the existing sanitized classifier.
- Repository additions are limited to the four specified operations and nested transition/event/result records. Grant lock/update use bound owner/workspace SQL, the existing explicit twenty-column projection, one optional revocation-witness predicate and no foreign row locks/wildcards. Persisted-event decoding verifies the closed successful revoke payload and its canonical SHA-256 hash.

## Tests Added And Expanded

Exactly six plain `@Test` methods are added; no baseline method is removed, renamed, disabled or weakened:

1. Surefire `CustomerShareCommandServiceTests.revokeReplaysBeforeStatePreconditions` — independently recomputes digest/event identity/fingerprint for exact `revoke,raw-key`; replay before terminal/time/witness; changed reason conflict; active context/ownership before replay; role/lock/clock order; invalid direct inputs before transaction; new-action terminal/expiry/stale precedence; no replay writes or evidence/material/policy calls.
2. Failsafe `CustomerShareCommandsIT.revokeWritesRegisteredEventAuditAndOutboxAtomically` — four modes × ACTIVE/LOCKED, zero/nonzero source generation and absent/equal witness; exact facts/hash/key sets/trace/nulls; all twenty head fields; role/connection/clock/append order; terminal denial without further changes.
3. Failsafe `CustomerShareCommandsIT.revokeExactReplayAddsNoFactsOrRevision` — stale/changed/absent witness and a controlled later database-derived clock; byte/semantic durable snapshots and all head fields unchanged; current ownership/context still enforced.
4. Failsafe `CustomerShareCommandsIT.revokeChangedReasonTerminalExpiryAndStaleWitnessConflict` — state/time/witness matrix including equality boundary, changed reason and terminal precedence, ownership/workspace concealment, missing/inactive context, controlled SQL failures; real zero-row conditional SQL for absent/present witnesses with one bounded reread and event rollback across compatible/terminal/expired/stale/absent reread shapes. Synthetic expired fixtures use valid insertion timestamps and owner lifecycle SQL, never disabled guards.
5. Failsafe `CustomerShareCommandsIT.revokeConcurrentActionsProduceOneTransition` — bounded two-connection identical/distinct-key races, personal/organization rank keys, one physical connection per operation, one update/fact set and changed-semantics conflict. Rendezvous precedes serialized locks; finite waits and executor cleanup are mandatory.
6. Failsafe `CustomerShareCommandsIT.revokeFailureAtEachFactAppendRollsBackAllEffects` — before/after event, update, Outbox and audit faults, plus commit-after-callback failure; fresh admin snapshots/counts and same-key clean retry. Existing fixture shutdown retains container cleanup.

Existing web methods expand in place, with no new web `@Test`: actual Filter/Controller capture asserts exact `revoke,raw-key`, personal/org caller and trusted trace, closed first/replay response, the ten-code revoke union, isolated rotate-only no-interaction 404, header/media/body/path/auth/workspace/provider precedence, UTF-8/NUL guards and boundary-key lengths. Provider-resolution counters distinguish binding failures from command invocation. The route method separates revoke POST from rejected methods and retains `/pa12`, encoded/matrix/extra/trailing alias defenses, `/me` and create regressions, missing-command 503, and a registered-command 200 path.

## Historical Author Commands And Observed Results

The following table preserves the earlier author-local attempts, not the final Controller results. The effective toolchain is `/opt/jdk21` and `/opt/maven-3.9.9/bin/mvn`; each Maven invocation uses `JAVA_HOME=/opt/jdk21` and `PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH`. Commands with `-f backend/pom.xml` run from the repository root.

Invocation-only definitions used to abbreviate the exact flags below:

```sh
MVN=/opt/maven-3.9.9/bin/mvn
AGENT=-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar
SOCKET=unix:///mnt/wsl/docker-desktop/shared-sockets/guest-services/docker.proxy.sock
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call13
GIT_WORK_TREE="$PWD"
```

These are invocation/environment adaptations, not edits to wrapper/config/POM/test assertions. The existing Windows `.git` pointer is never rewritten. Initial ordinary `git status --short` reported `fatal: not a git repository: .../codex-call/D:/codex-call/.git/worktrees/codex-call13`; explicit read-only Git overrides resolve it.

| Command | Observed result |
| --- | --- |
| `./backend/mvnw -version` with the toolchain environment | Exit 126: `/bin/bash: ./backend/mvnw: /bin/sh^M: bad interpreter: No such file or directory`. Wrapper bytes are preserved; installed Maven 3.9.9 is used instead. |
| From `backend`: `$MVN -B -ntp -Dtest=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests test` | Exit 1; 19 tests, 0 failures, 19 errors, 0 skips. Mockito could not self-attach: `Could not self-attach to current VM using external process`. No assertion/config relaxation. |
| `$MVN -B -ntp -f backend/pom.xml $AGENT -Dtest=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests test` | Exit 1; 19 tests, 0 failures, 10 errors, 0 skips: service 5/5 and MockMvc web 4/4 pass; all 10 embedded-server share-web cases fail context startup with `java.net.SocketException: Operation not permitted`. Log `/tmp/pa14-focused-agent.log`. |
| `$MVN -B -ntp -f backend/pom.xml $AGENT -Dtest=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests test` | Exit 0, BUILD SUCCESS; 9 tests, 0 failures/errors/skips (5 service + 4 web). Log `/tmp/pa14-unit.log`. |
| `$MVN -B -ntp -f backend/pom.xml test-compile` | Exit 0, BUILD SUCCESS; production and all test sources compile. Final log `/tmp/pa14-compile.log`, finished 15:02:25 +08:00. |
| `$MVN -B -ntp -f backend/pom.xml $AGENT -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify` | Exit 1 in unchanged Surefire phase: 78 tests, 0 failures, 21 errors, 0 skips. Does not reach Failsafe. Log `/tmp/pa14-targeted-verify.log`. |
| `DOCKER_HOST=$SOCKET docker info --format '{{.ServerVersion}}'` | Exit 1: `permission denied while trying to connect to the docker API at unix:///mnt/wsl/docker-desktop/shared-sockets/guest-services/docker.proxy.sock`. The installed guest socket was discovered rather than assuming `/var/run/docker.sock`; `guest-services/docker.sock` is absent. |
| `DOCKER_HOST=$SOCKET $MVN -B -ntp -f backend/pom.xml $AGENT -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT failsafe:integration-test failsafe:verify` | Exit 1; class startup error before the 13 methods: `java.net.SocketException: Operation not permitted`, then `Could not find a valid Docker environment`. XML: 1 class/setup case, 0 failures, 1 error, 0 skips. Final log `/tmp/pa14-targeted-it-final.log`. No PostgreSQL assertion was executed. |
| `DOCKER_HOST=$SOCKET $MVN -B -ntp -f backend/pom.xml $AGENT -Pcontainer-smoke verify` | Exit 1; final run 78 Surefire tests / 16 classes, 0 failures, 21 errors, 0 skips, 57 successful cases. Errors are CustomerMeWebTests 8, CustomerSharesWebTests 10 and CustomerWebConfigurationTests 3, all rooted in denied embedded-Tomcat socket startup. All unchanged architecture/module/query/context/session and other non-loopback cases pass. No package/Failsafe phase reached. Final log `/tmp/pa14-full-final.log`, finished 14:58:33 +08:00. |
| Execute the existing CI exact-inventory Python heredoc unchanged from `backend`, extracted from `.github/workflows/ci.yml` | Exit 1: `missing report: target/failsafe-reports/TEST-*PostgresContainerIT.xml`. This is not a successful full XML inventory. |
| Read-only source/CI/XML and baseline-method audit | Exit 0. Exact 16/78 and 6/113 source/CI names; no baseline method deletion; exactly the six named additions; Surefire XML names match all 78 expected names; 18 command-tree files; accepted plan hash matches. `/tmp/pa14-static-audit.log`. |
| `GIT_DIR=... GIT_WORK_TREE="$PWD" git diff --check` | Exit 0, no whitespace errors. Existing repository CRLF-conversion notices do not change file contents or require metadata repair. |

The Docker denial occurs before any container or Ryuk mount. `TESTCONTAINERS_RYUK_DISABLED` is not used. No permission escalation, ACL/config change, skipped test, Assumption, disabled annotation or reduced assertion is used. The installed wrapper/toolchain and initial local compile iterations are not a basis for claiming runtime PostgreSQL success.

Default/web packaged-JAR smokes and the production-JAR firewall's 82 probes were unchanged and **unexecuted in the author-local attempts**: verification stopped before producing the packaged JAR, and servlet smokes required the same denied loopback capability. These historical limitations are superseded by the Controller results below, not erased.

## Controller Runtime Validation

Controller reported the following successful validation of the same implementation content; this evidence-reconciliation task does not rerun Maven, Docker or PostgreSQL.

| Validation | Controller-observed result |
| --- | --- |
| Single fresh unified `mvn -B -ntp -f backend/pom.xml -Pcontainer-smoke clean verify` | BUILD SUCCESS in a disposable `docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21` container using WSL Docker, `--network host`, the WSL Docker socket and `TESTCONTAINERS_HOST_OVERRIDE=localhost`. Surefire: 78 tests / 16 classes; Failsafe: 113 tests / 6 classes; both zero failures/errors/skips. PostgreSQL 16.15. |
| Existing exact-inventory Python block from `.github/workflows/ci.yml`, executed unchanged against those fresh reports under Linux/WSL path semantics | `PA14 revoke slice exact source/CI/XML inventory passed: Surefire 78; Failsafe 64+10+10+8+8+13=113; combined 191; zero skips/errors/failures` |
| Unchanged default and opt-in web packaged-JAR smoke block | `PA11 bounded default/web JAR smokes passed; no DB/verifier adapter; JVM cleaned up` |
| Unchanged production-JAR dependency firewall | `PA11 firewall passed: baseline37/34, web55/48, counterparts47, dependency-only8, packager1; deltas18/0 and14/0; 82 adversarial probes` |

Controller's `git diff --check` passed before these validations. Controller later performed a Maven `clean`; generated `backend/target` files are validation outputs, not source changes. In particular, `backend/target/boot.log`, `backend/target/web-boot.log`, `backend/target/jar-index.txt` and `backend/target/pa11-runtime-dependencies.txt` are generated evidence only, not additional implementation artifacts.

## Source–CI–XML Inventory

| Suite | Source = CI expected | Observed final XML | Green? |
| --- | --- | --- | --- |
| Surefire | 78 methods / 16 classes, all exact method names verified | 78 cases / 16 classes; 0 failures / 0 errors / 0 skips; exact name sets match | Yes: Controller fresh clean run |
| Failsafe | 113 methods / 6 classes, all exact method names verified | 113 cases / 6 classes; 0 failures / 0 errors / 0 skips; exact name sets match | Yes: Controller fresh clean run |
| Combined | **78 + 113 = 191**, 22 classes | 191 cases / 22 classes; 0 failures / 0 errors / 0 skips | Yes: exact source=CI=XML inventory passed |

Surefire source/CI class counts: CustomerMeWebTests 8; CustomerSharesCommandWebTests 4; CustomerSharesWebTests 10; CustomerWebConfigurationTests 6; OneCallApplicationTests 1; SharingCommandPersistenceConfigurationTests 2; SharingPersistenceConfigurationTests 2; BootstrapDependencyBoundaryTests 1; CustomerApiBoundaryTests 3; ModuleBoundaryTests 8; SharingQueryBoundaryTests 1; CustomerShareCommandServiceTests 5; CustomerContextServiceTests 6; CustomerSessionBoundaryTests 7; CustomerShareCursorCodecTests 6; CustomerShareQueryServiceTests 8.

Failsafe source/CI class counts: PostgresContainerIT 64; SharingQueryServiceIT 10; CustomerMeIT 10; CustomerSharesIT 8; SharingRoleLeastPrivilegeIT 8; CustomerShareCommandsIT 13. Breakdown **64+10+10+8+8+13=113**. No rotation's extra methods or 79/118/197 inventory are imported.

CI dictionaries add exactly the six specified names and retain exact source/class/XML/duplicate/conditional-test checks. Controller observed the exact success message recorded above against the fresh clean-run XML. README current totals/status/history match this increment while retaining historical PA14 S1 77/108/185, PA12 159 and PA9 S3 64 checkpoints. The earlier author-local inventory failure remains historical evidence, not the final runtime result.

## Protected-Path Audit

- Baseline comparison reports exactly the 15 authorized existing implementation paths, plus the already modified `docs/gateflow/project-execution-control.md`. This task never edits Control. Its observed SHA256 is `23d4418f17d99f21bcc586f798560a524cc4accad06c1102a860731f5bc4fb2f`.
- The pre-existing untracked `.codex` remains untouched and empty, SHA256 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`. This report is the only new task artifact.
- Accepted/full plan/fix/review artifacts, migrations V001–V010, schema, OpenAPI, docs/12/14/15, POM, application configuration, providers, query/evidence code and architecture/configuration test sources remain unchanged.
- There are still exactly 18 command-tree source files, five top-level named command exports, the same 14 modules and unchanged dependency boundaries. The unchanged architecture tests pass in the full local Surefire run.
- No code rotation command/result/provider, production material/TTL/KDF/HMAC decision, expiry persistence/extension, admission/ticket/media path, new transaction, platform idempotency scope, foreign lock, Redis/COS/KMS/SMS, frontend or deployment change is introduced.
- No staging, commit, push, branch change, delegation, PR/issue mutation, merge, approval, ready action, reviewer request or external comment occurs.

## Residual Evidence And Owners

Runtime PostgreSQL/loopback/Testcontainers evidence is **closed** by Controller's fresh unified 78/113/191 run and exact source=CI=XML check. Packaged-JAR default/web smoke evidence and the 82-probe dependency firewall are also **closed**. No runtime evidence rerun remains assigned by this artifact.

- **Security/Product/Sharing owners → later production-policy/provider work units:** production policy, provisioning, KDF/mobile normalization/HMAC/OTP/TTL/provider decisions remain deferred; rotation and other later command slices remain outside this implementation.
- **Gateway/Security owners → gateway rate-limiting work:** gateway rate limiting remains deferred, not implemented by this revoke slice.
- **Sharing/Media owners → issued-credential/media recall work:** revoke does not recall issued short-lived credentials or buffered media; any stronger recall guarantee belongs to later work.
- **Evidence owners → evidence lifecycle/snapshot work:** previously deferred lifecycle/snapshot questions remain outside this slice; revoke reuses immutable grant metadata only.

No product/schema/frozen-contract conflict or out-of-allowlist code requirement was found. Status is `implementation-complete` on Controller-completed runtime evidence. This is implementation evidence only, not review acceptance, approval or production readiness; those decisions remain with the subsequent review/production gates.
