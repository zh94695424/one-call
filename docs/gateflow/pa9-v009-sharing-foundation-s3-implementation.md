# PA9 V009 Sharing Foundation — S3 Implementation

## Current aggregate-fix evidence — 2026-10-31

Task `pa9-v009-aggregate-codefix-gpt-20261031-01`; aggregate-review **fix only**, intake/current HEAD `96db75a17f6ea2b657cfb06d30dfcdf8d88f4dfc`. This section supersedes current-state claims below; all earlier execution history and raw identities remain historical. The complete author evidence, commands, identities and residuals are in `docs/gateflow/pa9-v009-sharing-foundation-aggregate-fix-1.md`. This is not aggregate re-review acceptance.

- AF-1 fixed: cumulative PA0–PA9/V001–V009 class javadoc explicitly covers V008/V009 and exactly 64 methods.
- AF-2 fixed: V006 inventory javadoc describes the final V009/no-V010 live fence and retained staged V008 prefix booleans; its body is unchanged.
- AF-3 fixed: SQL-only harness checks current ACTIVE/LOCKED status and statement-clock validity before fact replay; new admissions retain the existing ACTIVE/generation/limit checks. ACTIVE/LOCKED same-key replay passes; legal REVOKED/EXPIRED transitions are followed by same-key DENIED witnesses with current generation, exact unchanged admission/event/count/revision rows and a committed fresh-connection comparison. All 18 existing Lock races remain. No production service or policy is added; admission-key 129 logic is unchanged (B non-finding).
- Provenance corrected additively: historical S3 raw hashes describe the controller's mixed-LF/CRLF working tree, not canonical committed LF blobs. Intake canonical Git-content SHA256: Java `5e6a4fd04d06c94dddd443551fbdcc25839cccfeb1cb489a434fed27eed45d0d`, CI `7bffd120aa3da912977aca923a375cc814dc67803614a55fdfd0204df6b1d55f`, README `93cdb627cdd481e2873ac98078ccc6e0485555c65ce56ef37676cbce6f074b41`. Final uncommitted Java LF-normalized SHA256 `336f2b0669da4fb4d981affc46d4ca1a5eea334509602428c77327bb8edc890f`; environment-specific raw `2403afc612a1b7c9f460272387fff0d7b44b769f73fff34a16e16417b91ba8f6`. CI/README remain byte-identical; historical review/fix artifacts are not rewritten.

Actual commands from `backend`, with `JAVA_HOME=/opt/jdk21` and that JDK first on PATH; Maven `/opt/maven-3.9.9/bin/mvn`:

| Command / audit | Actual result |
| --- | --- |
| `mvn -B -ntp test-compile` | exit 0, BUILD SUCCESS, 7.129 s |
| `timeout 240s mvn -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009AdmissionCountOnceLimitAndAtomicRollbackBackstops+concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner' verify` | exit 0, 40.179 s; Failsafe 2 + Surefire 10, zero failures/errors/skips; ACTIVE/LOCKED replay, REVOKE/EXPIRY denial, 18 Lock/7 rollback/2 cleanup markers |
| Exactly one `timeout 240s mvn -B -ntp -Pcontainer-smoke verify` | exit 0, 59.458 s; Failsafe 64 + Surefire 10, zero failures/errors/skips; same markers |
| `python3 /tmp/pa9-aggregate-fix-01/audit.py preflight /tmp/pa9-aggregate-fix-01/preflight-xml`, then `python3 /tmp/pa9-aggregate-fix-01/audit.py final` | exit 0; base/slice ancestry, accepted plan/A/B pins, V001–V008 base equality/plan pins and frozen V009 verified; independent source=CI=XML exact 64, Surefire 10; allowed scope/frozen raw bytes/empty index/whitespace verified |
| `cd backend && python3 /tmp/pa9-aggregate-fix-01/ci-validator.py` (unchanged extracted CI heredoc) | exit 0, `container-smoke report assertions passed` |
| `docker ps -a --format '{{.ID}} {{.Image}} {{.Names}}'` | exit 0, empty after cleanup |

Runtime/provider: OpenAI assistant through Codex CLI, Linux/WSL2 amd64, Temurin 21.0.13, Maven 3.9.9, real Docker Desktop engine 29.9.2 and Testcontainers `postgres:16` / PostgreSQL 16.15. No subagent; backing model/provider routing is not independently terminal-attested. Logs/XML/audits and final non-circular file-hash manifest are under `/tmp/pa9-aggregate-fix-01`. Full log SHA256 `d9002f902896c38c49349b932fc7d5f9f7749e3d624802029e63029ceda9209c`; full Failsafe XML `e9e98b4d8a6292ce38f568b4142c72f88fc62a435df9ef00812248e92ca968a7`.

Bounded-race/timing, CHECK UNKNOWN, policy/provisioning and remote-CI residuals are unchanged and not addressed. V001–V009, CI, README, plan/design/controller, historical artifacts and pre-existing `.codex`/`build/` bytes remain untouched. No stage/commit/push/PR/review/approval/comment or external workflow action. Stop at author aggregate fix; fresh aggregate A/B re-review is separate.

## Current S3 code-fix evidence — 2026-10-31

This section supersedes the historical pre-fix source/evidence identities below, not their retained execution history. Task `pa9-v009-s3-codefix-gpt-20261031-01`, intake/current HEAD `209b658e249ef26e22be69db77726de63cc255f4`, gate **fix** only. F1 is fixed: README lines 3/117 now say 64; its only remaining 62 explicitly describes the accepted S2 checkpoint. F2 is fixed: only `v009S3Fixture()` changed; its outer `Exception | Error` handler independently removes the possibly committed synthetic account and cleans the committed V008 fixture, suppresses cleanup failures, and rethrows the original setup failure. Rollback failures also become suppressed rather than replacing that failure. All 64 test bodies, normal S3 cleanup and every other helper/assertion are unchanged.

| Current item | SHA256 |
| --- | --- |
| Java, raw checkout | `9f089e03efd3bafce68e949dd590d8aaf8f29a943c90a9da43829a33f8e9dcb1` |
| README, raw checkout | `c9395e749738e7c1435840bf88d8026ff8a4b32879efb9817a1b8356db73132f` |
| CI, unchanged raw checkout | `9f04d0b0a710be0143b7977aa20a28f3492f0d281ac59f7bbb456da862f09238` |
| Three-source diff against current intake HEAD | `b79ab38c3e4cd736a350ed53d89e64845f50d605bdb075ec4e63d236894981ad` |
| V009, unchanged canonical Git bytes | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| Focused Failsafe XML | `cac228428c3ddb091507a21b8c17713169414e5b46cb0a3f682abcf289b8d9fb` |
| Full Failsafe XML | `f9e604fe5137050c65850f81d87fc1f06baa510c26e1b6ac3c02554919ff5f61` |
| Focused Maven log | `27ed625d3ddde2179a015982a5631becf2e454aa6a3822eb3ca72bd461ce5c8e` |
| Full Maven log | `e8387ae9a5e719f575e7cdcb6d5f17dabd682ca7f2de959b6753e8c4e05341ad` |
| Real-PostgreSQL setup-failure probe log | `9699a8a6cd8df2283dcde8a891a4d2e4859836c5aed858d0181c6be6ce9045cf` |

Required author validation passed on Java 21.0.13/Maven 3.9.9/Docker Engine 29.9.2/PostgreSQL 16.15: test-compile; focused S3 **2 Failsafe + 10 Surefire**; exactly one bounded full run **64 Failsafe + 10 Surefire**, zero failures/errors/skips. Independent source=CI=XML exact-set parsing and the unmodified CI validator passed. Each run retained 18 Lock observations, seven rollback/no-residue cases and two cleanup/restoration markers. Five additional external JDBC fault probes cover setup connection failure, assertion plus rollback failure, post-commit failure, account-cleanup close failure and V008-cleanup close failure. Every probe retained original exception identity, checked suppression, and restored exact all-table rows plus trigger/FK flags. Normal setup/cleanup also restored the same baseline. No test container remains; V001–V009 and CI are frozen.

Current logs/XML/scripts, final record hashes and non-circular manifest: `/tmp/pa9-s3-fix-01/`. Full commands, review identities, findings, final I3/E3 hashes, failed scratch attempts and residual ownership are in `docs/gateflow/pa9-v009-sharing-foundation-s3-code-fix-1.md`. One scratch javac invocation inadvertently enabled the Modulith annotation processor and regenerated pre-existing root `build/generated-spring-modulith/javadoc.json` (current bytes `[]\n`); no manual build edit occurred and it remains excluded, but strict no-build-write compliance is not claimed. The retry used `-proc:none` outside the checkout. Earlier source/evidence and failed-attempt records below remain historical, not current validation.

Runtime/provider: OpenAI assistant through Codex CLI on WSL2; actual backing model/provider route is not independently attested by shell/database output. Residual ownership is unchanged: Backend QA owns synthetic five-second timing and bounded races/CRLF parsing; Sharing/Backend/Security owns service semantics and CHECK UNKNOWN boundaries; Security/Product owns policy; Platform/Database/Security/Legal owns provisioning/storage; Controller/QA owns remote CI, archival/provider attestation and separate fresh dual re-review. No remote CI or external workflow action occurred. This is author fix completion, **not re-review acceptance**; no stage/commit/push/PR/review/approval/external comment occurred.

## Historical pre-fix S3 record

## Identity and authority

- Task: `pa9-v009-s3-implementation-gpt-20261031-01`; work unit: `PA9-v009-sharing-persistence-foundation`; gate: `implementation S3` only; date: 2026-10-31, Asia/Shanghai.
- Intake and unchanged final HEAD: `a95f96dfa4ebb4e73271d1c53c2e792eb1c98087`; branch: `codex/pa9-v009-sharing-foundation`. Accepted S2 `6111a535918ca0d53be893351d736558fa5aaa3a` is an ancestor (exit 0).
- Accepted plan: `docs/gateflow/pa9-v009-sharing-foundation-plan.md`, SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`. Complete accepted plan, active Goal Confirmation/current controller checkpoint, V009 definitions/function split and existing S1/S2 fixtures/helpers were read before editing. No authority conflict or scope deviation was accepted.
- Runtime: Codex CLI/GPT execution in the OpenAI assistant session. Controller-requested route is GPT `gpt-6.1-sol` / `high`; actual backing provider/model is not independently attested by these terminal/database results. No substitute provider or subagent was invoked.
- Author status: S3 implementation-complete with local required database evidence passed; not controller acceptance, code review, remote CI or production readiness.
- Repository writes are exactly T/C/R/I3/E3: the Java test, CI, README, this implementation record and `docs/gateflow/pa9-v009-sharing-foundation-s3-completion-1.md`. Pre-existing untracked `.codex` and `build/` are not task source. Maven-generated build metadata, ignored Node dependencies/generated `tmp/` bundles, backend target files and external `/tmp/pa9-s3-01` evidence are generated validation output, not committed artifacts. No manual edit to `.codex`, `build/`, controller or any other source occurred.

## Tested source identity

| Source | Raw checkout SHA256 |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `eb84f41a547f7c3d675f4d6106cc2ddb475b928d90eb5f32f9e18d114a022175` |
| `.github/workflows/ci.yml` | `9f04d0b0a710be0143b7977aa20a28f3492f0d281ac59f7bbb456da862f09238` |
| `backend/README.md` | `1c1f94b31674121446484033d3e7d706b2cff641de4e5035a17dc443e769bcb9` |

Three-source Git diff against intake HEAD: SHA256 `4dc8808070a44a7827020f8ae09560e6bdbc61b92dddd016548689f3e8f8965f`, retained as `/tmp/pa9-s3-01/tested-source.diff`. `final-source-before.sha256` and `final-source-after.log` establish that T/C/R did not change during or after the one final full Maven run. Record self-hashes are external in `/tmp/pa9-s3-01/SHA256SUMS`; they are not embedded circularly.

## Implementation decisions and witnesses

Exactly two `@Test` methods were added:

1. `v009AdmissionCountOnceLimitAndAtomicRollbackBackstops`.
2. `concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner`.

All 62 accepted test names/bodies and all 31 existing V009 helper bodies remain identical to canonical intake source. No existing assertion was reduced. New private method-local helpers reuse `V008S2Fixture`, `v009GrantSql`, `v009S2Clone`, `v009S2Rows`, existing SQLSTATE/constraint assertions, runtime role switching, blocked-backend/future helpers and FK-safe V008 cleanup. They are SQL test fixtures, not a sharing application or database service. No migration, dependency, application source or test resource changed.

### Admission transaction and replay

- One real READ COMMITTED connection under `sharing_role` obtains the owning grant `FOR UPDATE`, reads current count/revision, looks up the existing admission key before any limit test/INSERT, then checks ACTIVE/current statement clock/expected revocation generation/limit for new keys.
- A new key inserts the next admission number, then one `SHARE_ACCESS` / synthetic event ID / `ADMISSION_GRANTED` / `SUCCESS` event linked to that admission and grant. The exact redacted JSON is asserted against the locked grant's verification mode, code/revocation generations and grant expiry, plus explicitly synthetic admission/session expiry labels; event identity/result and the fixed synthetic digest are also pinned. No ticket TTL, deterministic production hashing, token, KDF, authorization or session policy is invented.
- The conditional count/revision update must affect exactly one row before COMMIT. First admission returns count 1/revision 2. Exact-key replay returns the original admission/event/count without writing another row, including at synthetic max=1 and in LOCKED state. A new key is denied in each case; complete row/history snapshots stay unchanged.
- `PAGE_OPENED`, `PREFETCH_IGNORED`, `VERIFICATION_FAILED` and an independently committed post-admission `TICKET_FAILED` shape append observations without changing grant/admission facts. There is no `TICKET_ISSUED` event, ticket issuance or compensating decrement. Event read-before-write uses the exact domain/id; replay leaves the full snapshot unchanged. Direct domain/id reuse independently fails `23505/uq_share_access_event_domain_id`.
- Clock probes use otherwise valid future/past intervals and observed statement-clock crossing. In a long-lived transaction `now() < expires_at` remains true while `clock_timestamp() >= expires_at`; the enabled admission guard refuses insertion. The loop has a ten-second bound, not a sleep-based success assertion.

### Whole-transaction rollback

Seven logged cases compare full grant/admission/event baseline snapshots on fresh connections after explicit whole-transaction rollback:

| Case | Expected refusal / termination | Fresh-state result |
| --- | --- | --- |
| Admission + event + count sentinel | Test-only client `P0001/S3_SENTINEL` | Complete original grant/history restored |
| Duplicate event after admission/count | `23505/uq_share_access_event_domain_id` | No partial admission/event/count |
| Duplicate key after admission/count | `23505/uq_share_admission_key` | No partial second admission/history |
| Duplicate number after admission/count | `23505/uq_share_admission_no` | No partial second admission/history |
| Wrong next admission number | Server `P0001/SHARE_ADMISSION_REFUSED` | Count/revision/history restored |
| Forced stale expected revision | Exactly zero updated rows; bounded current grant reread; test-only `P0001/S3_EXPECTED_REVISION_ZERO` | Explicit rollback removes admission/event; zero rows alone do not abort PostgreSQL |
| Admission, revoke, transition event, audit and Outbox | Existing `audit.append_fact` and `platform.outbox_append` execute as sharing_role, then sentinel | Both nested row IDs and aggregate/object counts absent |

Audit observation is performed only after resetting to the privileged test observer; `sharing_role` receives no direct audit SELECT or other new grant. Nested append execution itself remains under real `sharing_role`. These are fixture transaction facts, not a promise that an excluded production service uses them correctly.

### Bounded real races

All 18 cases log a real `pg_stat_activity` Lock wait before releasing the winning transaction. Both connections explicitly use READ COMMITTED, distinct `application_name` and `statement_timeout=20000`; existing wait detection is bounded at 15 seconds and each future/executor termination at 30 seconds. Sleep is not the proof: the existing polling helper returns only after observing the lock wait. Expiry ordering separately observes `clock_timestamp() >= expires_at`. No explicit foreign row lock is taken, no future is joined while the winning transaction is still held, and connections roll back/close plus the executor shuts down in finally.

- uq_share_grant_share_id winner-commit; fresh identity/sentinel/state exact
- uq_share_grant_share_id winner-rollback; fresh identity/sentinel/state exact
- uq_share_grant_token_hash winner-commit; fresh identity/sentinel/state exact
- uq_share_grant_token_hash winner-rollback; fresh identity/sentinel/state exact
- uq_share_access_event_domain_id winner-commit; fresh identity/sentinel/state exact
- uq_share_access_event_domain_id winner-rollback; fresh identity/sentinel/state exact
- same-key=true max=null winner-commit=true; fresh one admission/event/count
- same-key=true max=1 winner-commit=true; fresh one admission/event/count
- same-key=false max=1 winner-commit=true; fresh one admission/event/count
- same-key=false max=1 winner-commit=false; fresh one admission/event/count
- admission vs REVOKE transition-first=true; current state/generations/revision/no-residue exact
- admission vs REVOKE transition-first=false; current state/generations/revision/no-residue exact
- admission vs ROTATION transition-first=true; current state/generations/revision/no-residue exact
- admission vs ROTATION transition-first=false; current state/generations/revision/no-residue exact
- admission vs EXPIRY transition-first=true; current state/generations/revision/no-residue exact
- admission vs EXPIRY transition-first=false; current state/generations/revision/no-residue exact
- admission vs LOCK transition-first=true; current state/generations/revision/no-residue exact
- admission vs LOCK transition-first=false; current state/generations/revision/no-residue exact

Grant share-ID and token-hash uniqueness are raced separately. Winner commit produces the exact named `23505` loser; winner rollback allows the waiting insert to commit. Event uniqueness likewise pins `uq_share_access_event_domain_id`. Same admission key is serialized by the grant lock and replays exactly once, both unlimited and max=1. Distinct keys at max=1 give one committed admission/event/count; with first-winner rollback only the waiter survives.

Each lifecycle ordering asserts current status, revocation/code generations, revision, identities, event counts and loser-sentinel absence from a fresh connection. Transition-first denies a new admission after commit, including ACTIVE-after-rotation with stale pre-rotation expected generation. Admission-first preserves byte-identical admission/event rows and refuses a stale expected-revision update before applying the current transition. Revoke/expiry each add exactly one revocation generation and revision; LOCK is generation-neutral. Rotation uses a separate legal ACTIVE→LOCKED prerequisite followed by legal LOCKED→ACTIVE salt/hash replacement: two individually conditional revision increments, one code-generation increment and one revocation-generation increment; final revision accounts for both SQL actions (3 transition-first, 4 admission-first). It is not the service-forbidden same-state ACTIVE rotation. Admission-first then LOCK permits exact-key fact replay without a new insertion. No throughput, deadlock-freedom, real session revocation or authorization claim follows from these bounded fixtures.

### Cleanup and cumulative controls

New committed fixtures register method-local grant IDs; histories are scoped to those grants. Cleanup transaction disables only the two named sharing immutable triggers, deletes access events before admissions before grants, re-enables both and asserts ordinary enabled states before COMMIT. Finally rolls back on any failure rather than committing cleanup. Existing V008 cleanup performs FK-safe reciprocal deletion with its two deferred cycle FKs restored to IMMEDIATE and its named immutable/optional entitlement append triggers restored.

Fresh connections assert every collected sharing admission/event ID, every registered grant and the synthetic creator absent, plus the complete V009 catalog/trigger/FK inventory and unchanged V008 FK deferrability flags. Enabled evidence/entitlement trigger flags are rechecked; existing V008 cleanup checks its own dependency IDs. Both S3 methods emit successful cleanup markers in focused and full XML. Final Docker listing is empty. No TRUNCATE, all-row reset, `DISABLE TRIGGER ALL`, replication bypass, foreign table change or permanent privilege leak was introduced.

Final cumulative XML still demonstrates all 31 V009 CHECK names, 32 NOT NULL probes and eight exact PK/UNIQUE names from accepted S2, as well as the accepted schema/ACL/FK/lifecycle inventory: three tables, 38 columns, 31 checks, six ordinary FKs, 13 indexes, three local trigger functions and four ordinary triggers. The five SECURITY DEFINER service-function baseline is unchanged.

## Actual validation sequence

Tool environment for Maven commands: `JAVA_HOME=/opt/jdk21`, `PATH=/opt/jdk21/bin:$PATH`; executable `/opt/maven-3.9.9/bin/mvn`. Workdir is `backend` unless stated. All invocations use `-B -ntp`; no skip flag or relaxed timeout was used.

- Java: Eclipse Temurin `21.0.13+9-LTS` (OpenJDK 21.0.13); Maven `3.9.9`.
- Docker client `29.1.3`; Docker Desktop `4.97.0 (244584)`; server Engine `29.9.2`; Linux amd64 on WSL2 kernel `6.18.33.2-microsoft-standard-WSL2`.
- Actual PostgreSQL image/tag used by tests: `postgres:16`; server version `16.15`, confirmed by Flyway logs. Locally available image identity `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`, repository digest `postgres@sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`. This is an observed local image, not a production pin.
- Node for supplemental unchanged contracts: `v22.23.4`. `npm ci --offline` completed from local cache, exit 0 in 71 seconds; no production dependency was changed.

Exact Maven commands:

```sh
timeout 600s /opt/maven-3.9.9/bin/mvn -B -ntp test-compile
# Root workdir retry after the compiler fix:
timeout 600s /opt/maven-3.9.9/bin/mvn -f backend/pom.xml -B -ntp test-compile
# Backend workdir:
timeout 600s /opt/maven-3.9.9/bin/mvn -B -ntp verify
# Focused attempts 1, 2 and 3 use this same method selector:
timeout 900s /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009AdmissionCountOnceLimitAndAtomicRollbackBackstops+concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner' verify
timeout 900s /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v007ForeignKeyAndLateOrderConsentFkBackstops+v008EvidenceInventoryAndGuardShapesAreExact+v008OwnerPublicRuntimeAndCrossModuleAclsAreExact+v008ForeignKeyAndDeferredCreationCycleFkBackstops+v008CreationCycleBothOrdersAndTerminalLifecycleBackstops' verify
# Exactly one final bounded full run:
timeout 1500s /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify
```

| Attempt | Exit | Wall elapsed | Actual test result |
| --- | --- | --- | --- |
| test-compile 1 | 1 | 7 s | Ambiguous AssertJ overload on SQLException (which is also Iterable); fixed only the new test assertion using Object cast |
| test-compile 2 | 0 | 8 s | Compilation passed |
| default verify | 0 | 13 s | Surefire 10, failures/errors/skips 0; no Failsafe execution |
| focused 1 | 1 | 35 s | Failsafe 2, failures 1/errors 0/skips 0; Surefire 10 clean; all 18 races passed but direct audit observer SELECT failed 42501 |
| focused 2 | 0 | 47 s | Failsafe 2 + Surefire 10, all clean after observer fix |
| focused 3 | 0 | 46 s | Final-source Failsafe 2 + Surefire 10, all clean after strengthening bounded stale reread and independent loser-token oracle |
| V007/V008 fence regression | 0 | 24 s | Failsafe 5 + Surefire 10, all clean |
| final full verify | 0 | 58 s | Failsafe 64 + Surefire 10, failures/errors/skips 0; completed 2026-10-31 17:45:37 +08:00 |
| Independent final audit | 0 | 20 s | Exact source/CI/XML identities, frozen bytes, unchanged accepted bodies/helpers, scope and whitespace |
| Actual CI XML heredoc | 0 | less than 1 s | `container-smoke report assertions passed` |
| Boot jar smoke | 0 | 2 s | `Started OneCallApplication`, normal process exit |
| Production jar firewall | 0 | less than 1 s | No forbidden runtime library entry |

Additional actual commands: `python3 /tmp/pa9-s3-01/audit.py /tmp/pa9-s3-01/final-xml`; backend workdir `python3 /tmp/pa9-s3-01/ci-validator.py` (literal CI heredoc extracted without rewriting its assertions); `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar`; `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar` followed by CI's unchanged `BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)` deny pattern. `reports-summary.json` independently parses every retained successful/failed run's XML; each verify invocation retains its separate ten clean Surefire cases. Boot jar SHA is retained in `boot-jar.sha256`.

Initial ordinary Git commands failed because the Windows `.git` pointer is not directly resolvable under Linux. All authoritative Git reads use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`; no metadata repair was performed. An initial generated README patch with unified-diff hunk headers was rejected with no file change; applying the same scoped patch with supported hunk syntax succeeded. An initial scratch CI-heredoc extraction used the wrong cwd and failed without writing a validator; root-cwd extraction succeeded and the actual validator then passed. These setup diagnostics are not erased or represented as test passes.

### Unchanged contract-regression job

The contract job body is byte-identical to intake (SHA256 `9f23daab76d2d138e09d31f485e8e01b906741c0acbe6f67a22d312705a697e9`). Its commands are extracted by `/tmp/pa9-s3-01/contracts.py`; full commands/cwd/exit/elapsed/logs are retained in the results JSONs, not reconstructed from intended outcomes.

Two supplemental attempts failed honestly: direct Windows-checkout execution (exit 1, 42 s) and initial external `git archive HEAD` snapshot execution (exit 1, 69 s). In both, A1 passed; A2 reported missing A0 CHECK/column dictionaries (25/25 fixtures still passed). The archive inherited core.autocrlf conversion, so it also contained 4,896 CRLF sequences in A0. No contract assertion or repository document was modified to hide the failures.

A new external snapshot was populated with `git -c core.autocrlf=false archive HEAD api/openapi protocol security deployment/capacity acceptance docs .github/workflows/ci.yml` piped to `tar` under `/tmp/pa9-s3-01/contracts-canonical` (archive exit 0, 5 s). A0 bytes were asserted equal to `git show HEAD:docs/14-production-data-model-and-flyway-schema-spec.md`, SHA256 `3a506e578fdde6a89e684a64ddd31261b397e22f7bdb0ac589331bb6eebba761`, zero CRLF. Existing ignored toolchain dependencies were reused by a scratch symlink. The exact unchanged contract steps then all passed, total exit 0 in 59 s, without touching frozen checkout bytes:

| Step | Exit | Elapsed | Retained log |
| --- | --- | --- | --- |
| assert exact node version | 0 | 0.008 s | `contracts-canonical-lf-assert-exact-node-version.log` |
| A1 s3 | 0 | 51.312 s | `contracts-canonical-lf-a1-s3.log` |
| A2 s3 | 0 | 0.114 s | `contracts-canonical-lf-a2-s3.log` |
| A3 s3 | 0 | 0.114 s | `contracts-canonical-lf-a3-s3.log` |
| A4 s2 | 0 | 0.417 s | `contracts-canonical-lf-a4-s2.log` |
| A5 s1 | 0 | 1.97 s | `contracts-canonical-lf-a5-s1.log` |
| A5 s2 | 0 | 2.519 s | `contracts-canonical-lf-a5-s2.log` |

This is local canonical-Git evidence, not remote CI. Direct CRLF-checkout parser sensitivity remains a Backend QA/CI portability residual outside S3; no unrelated source repair was made.

## Exact cumulative method identity

The independent audit extracts source annotations with regex, CI literal names with Python AST (including duplicate-literal detection), and final Failsafe testcase names with XML parsing separately. Source total/distinct = CI literal total/distinct = XML total/distinct = **64**, and all three sets are equal. XML header total also equals 64, no testcase skip/failure/error element exists. The three separate Surefire XML suites total **10**, with zero failures/errors/skips. The unchanged actual CI validator additionally passes against the real backend reports. Focused subset reports are not used as cumulative equality evidence.

Exact shared sorted 64-method set (also retained three independently extracted ways in `final-audit.json`):

```text
appendOnlyClaimAndForeignKeyBackstops
auditIsAppendOnlyAndRuntimeWritesAreDenied
auditReasonAndDenylistRulesAreEnforced
bindingLifecycleTerminalityAndActiveNumberUniqueness
brokenMigrationFailsVisibly
committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow
concurrentActiveBindingUniquenessHasOneWinner
concurrentCatalogPublicationAndCommerceIdentityHasOneWinner
concurrentIdempotencyRetryObservesOneWinner
concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner
concurrentV005TaskClaimAndActiveCallHasOneWinner
concurrentV006FirstTrialAndReservationIdentityHasOneWinner
concurrentV007ActivationAndTransitionIdentityHasOneWinner
concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner
concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner
functionsHaveFixedSearchPathAndNoTransactionControl
hostileSearchPathCannotRedirectFunctionEffects
idempotencyNewReplayConflictAndFinalizeOnce
identityCollisionAndMembershipRevokeRegrantSemantics
lastEnabledAdminTransactionIsSerializedAndFailsClosed
localImmutabilityGuardsRejectRelationAndRotationChanges
mutableHeadsEnforceLifecycleRevisionAndNamedChecks
outboxReplayConflictRollbackAndDenylist
ownerPublicAndRoleExecutionBoundariesAreExact
riskOpenClearReopenRevisionProtocol
runtimeAndCrossModulePrivilegesAreExactAndWritesDenied
subjectOwnerSetOnceAndRealNameSeparation
v001ThroughV003InventoryMatchesTheFrozenContract
v001ThroughV009MigrateCleanPostgres16WithoutSkipping
v003OwnerPublicAndRuntimeAclsAreExact
v003SubjectNumberCatalogAndGuardsAreExact
v004CatalogOrderInventoryAndGuardsAreExact
v004CatalogVersionLifecycleAndGuardBackstops
v004ForeignKeyAndStagedConsentFkBackstops
v004OrderPaymentLifecycleAndAppendOnlyBackstops
v004OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005ForeignKeyAndDeferredReservationFkBackstops
v005IvrTaskSessionCallInventoryAndGuardShapesAreExact
v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops
v005OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005SessionCallCanonicalCallerAndReciprocalLinkBackstops
v005TaskTtlLifecycleAndGuardBackstops
v006EntitlementReservationUsageInventoryAndGuardShapesAreExact
v006IssueFirstTrialGrantReplayConflictAndPolicySemantics
v006NestedOutboxAtomicityAndReplayRollbackEvidence
v006OwnerPublicRuntimeAndCrossModuleAclsAreExact
v006ReservationUsageForeignKeyAndLateFkBackstops
v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops
v007ActivationConsentInventoryAndGuardShapesAreExact
v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops
v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops
v007ForeignKeyAndLateOrderConsentFkBackstops
v007OwnerPublicRuntimeAndCrossModuleAclsAreExact
v008CreationCycleBothOrdersAndTerminalLifecycleBackstops
v008EvidenceInventoryAndGuardShapesAreExact
v008ForeignKeyAndDeferredCreationCycleFkBackstops
v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops
v008OwnerPublicRuntimeAndCrossModuleAclsAreExact
v009AdmissionCountOnceLimitAndAtomicRollbackBackstops
v009ForeignKeyAndRestrictDeleteBackstops
v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops
v009OwnerPublicRuntimeAndCrossModuleAclsAreExact
v009ShapeDefaultsAndAppendOnlyBackstops
v009SharingInventoryAndGuardShapesAreExact
```

## Frozen-byte evidence

V009 raw checkout bytes equal intake Git bytes and SHA256 `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`. No migration path is changed. For each V001–V008, original base `835f2ec` Git blob equals intake/final HEAD blob; checkout bytes normalized only for the existing CRLF→LF comparison equal that canonical blob. Raw checkout hashes are separately retained below to distinguish preservation from normalization. No repository file was normalized as a fix.

| Migration | Canonical Git SHA256 (base = HEAD) | Unchanged raw checkout SHA256 |
| --- | --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| `V005__ivr_task_session_call.sql` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| `V007__activation_and_consent.sql` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| `V008__evidence_media.sql` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |

Plan SHA remains the accepted `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`. Controller raw SHA256 remains `82e20669b483a71d1d9d1e328373c38c3803e13264d40d8c618f09f149ec53e0`; `.codex` remains the empty-file SHA256 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`. Staged delta is empty and HEAD/branch unchanged.

## Retained evidence hashes

All paths in these two tables are relative to external `/tmp/pa9-s3-01`. `SHA256SUMS` is generated after records/final audit and includes every retained log/result/XML, audit/validator scripts, source diff/provenance and the five final repository path hashes. Failed attempts remain retained alongside passing reports; original source hashes for the final focused/full runs are retained separately. Evidence remains local and ephemeral unless the controller archives it.

| Log / audit / validator | SHA256 |
| --- | --- |
| `preflight.log` | `bb18a4c42f7d0cb0a4bd59f0907a32e73988ce1006228904b4c6096d1e154475` |
| `test-compile.log` | `973bb74455cb476056ae3a9a40651604e1412439a20e9336169caebe9e86b93d` |
| `test-compile-2.log` | `a583ea68419556796acbdb24bf91fd0a459678070af5780fc3a0076da55a503f` |
| `default.log` | `90584968d473d4c6e609aeb564853ca096c3139725f38e15c5c5f2a7f027e2f4` |
| `focused-1.log` | `735ce509ca4dbee5bfad680d48cbec3e5a11cbcec6a79b6d8104e5d6f793ae15` |
| `focused-2.log` | `f83c26e3e8cd5444f5f86f3974956ea6bcc325bef8b1efe1dd5d665e2bb03608` |
| `focused-3.log` | `07816e58458f53669aba662233a8a97d22a0e52363cd8155df2fb2b8ed809108` |
| `fence.log` | `6b424e3cb38a95cded4df2568194c3da5a89ddd6864c290a32ca5b03f828ef02` |
| `final.log` | `e812a99b69dbf137550e485bf75f5f5d4fb222a7d47946a98f38bd0de594bc43` |
| `final-audit.json` | `97aa4db5ab3fded8aeb239d7aca9a4e83d8e4a174eb80bea356cf457e6f4833a` |
| `reports-summary.json` | `a99ecb7b47f215a425cf24cbad6aee7eee74afc0cde757000aa1d3a5a3ca99ac` |
| `ci-validator.py` | `db73a2eeda91d31036fde622dbcf0c8b0c0acf2b03386c2c4c8aefb3e65edefe` |
| `ci-validator.log` | `f590d8167cad47859c57405688c1ea785c67a9972c31ffd9b39fe23a85481fd7` |
| `boot.log` | `821f6c27bce92314e15d90b73f2616f77550306270a0fb5aca0f3f8c64e7ad9f` |
| `jar-index.txt` | `8e6949362a07e63bceb657dbca50a51f49c9a7269d479363173bb43d94948c9d` |
| `contracts.log` | `c1bd731f924a89b82fbb8d890272c372bf7da055a9c4818933245f82d5435aba` |
| `contracts-canonical.log` | `94cdba9b789e497fccbc1a977bd6b56b7e2972625f7cba21d729abc1ec5cfd06` |
| `contracts-canonical-lf.log` | `7d55bca64ce9c8f8fab59f530f1c18e519c7fa0052cef31a2b19902950f194e2` |
| `contracts-canonical-lf-results.json` | `34fdd9dc52da2b6af38e09f1b2769e366174831b76927b431714c485219e0fcc` |
| `contracts-provenance.json` | `978492a3c4101c5f41181806ce63b9d060f2d4ee5624cbfc9bf13b255d13fcce` |

| XML, including failed attempt | SHA256 |
| --- | --- |
| `default-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `d27162344c360493c958cb0fd76c9ecb6c2a5ca3d39d5cb5beb355822213112a` |
| `default-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `8c1d946b6f64484519c43bef2009a935742c1d26ac3ddfdd2e86b717f3023a94` |
| `default-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `d5f8cd2623d8dd3767894863a54858bec64c3b869f4de22e5d7e5ab8395f1793` |
| `fence-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `cf8b66eb775fae6bb893aa37c5d14ac53d0373f741675d03f5a87d49c4f7ac22` |
| `fence-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `94b0de19dae4e8f8e07d5b6270089a9566b1230c2d7a1067123f58f2f02dee5f` |
| `fence-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `46de0e0173488aeb66996c0199ae5f8b4c338363a4f950844b0024106f9b8ca7` |
| `fence-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `c04865011179da78580262b006940d2ca2c9b2d2542583ff40075bec124c070d` |
| `fence-xml/failsafe-summary.xml` | `a81d75e6f1b44d866827ca77b54e23e883ce942020df3ae3313b9a33a8c77c9e` |
| `final-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `661f316ea8fc4faa768933d3f42efa2c4d2c20d2df4e0524cf86bd02933b1d56` |
| `final-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `91779c250be30d765fd4ceb4f3de798e9d27774b29afb2a864fa10351c71b643` |
| `final-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `900f60003b783f04ed1496e9c7b6c2b050a9c30b9358fa11f1a310a091a8d29c` |
| `final-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `20cdcb931c7a1972a40981c3f2ed2715eaff34b4c464c4394ffdeb605135b95d` |
| `final-xml/failsafe-summary.xml` | `79fc0c6364bc87ed6b1f9e4fb54d6be13cd408701eb9278ad1173a379d2822e0` |
| `focused-1-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `1609b11231aee51b4e1e1b634361a8484816a43123a1cfb1a220cb37a94f4fb8` |
| `focused-1-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `a02de621c1bf7ac2a14982e0f34390e71447c2c6917b2df8a0304363c334918e` |
| `focused-1-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `7862890da7ac374d4154ad386041419396ef4cc5cfaf7d132eed8b25a3abffe6` |
| `focused-1-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `8504f27a25b9fb4aa313f62622e0fbd459fd58169a1332e9430c519dec878cea` |
| `focused-1-xml/failsafe-summary.xml` | `7f42f05671d4d0dd9d840cac8dcce31577c3130329ff08abc13f362d39dcba4b` |
| `focused-2-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `00533870190e6dfb4488c461524ccb6789bf99bbb26bf51e0ccc7140ffc163ae` |
| `focused-2-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `85426209d1607a638c9892e714bc97183ac29f76c7decbe3f7179347cad17609` |
| `focused-2-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `f8c1ad8287ff7f5882088c7960ed84b6c9b8b2a437ab8bf2a822300fd6b353a8` |
| `focused-2-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `ef08fc52a5525a6485025fb5ab6db274dbedec6fa1b33477ba56e007eaca1c3f` |
| `focused-2-xml/failsafe-summary.xml` | `148c53f8f020ff6213746b781b2f0ee3f842e0f10cef43f51cb392155a44c655` |
| `focused-3-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `78de02adf59c3088037e2dac685530c5885e82fe3871d895b226b2e4a4755773` |
| `focused-3-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `f912377b2e668d216dbfe4b9b5c7f8a224eb6e9c3bebeb5ec4fe503e546fd692` |
| `focused-3-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `1da78797cb404d1305fdea3b138fd561e98a6ff07b2d3fb5c824bc1a8b405740` |
| `focused-3-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `aa06423f5a9e99231d9440c7abec48cd1cf87326f1507fffd1f3ff020e111d3b` |
| `focused-3-xml/failsafe-summary.xml` | `148c53f8f020ff6213746b781b2f0ee3f842e0f10cef43f51cb392155a44c655` |

## Residual ownership and stop boundary

- Sharing/Backend/Security owns future service use of the same-connection SQL/replay/conditional-revision discipline, typed JSON validation (CHECK UNKNOWN is unchanged), coherent grant/admission binding, current ownership/member/risk checks, deterministic production identity/fingerprint/hash derivation and exclusion of structurally accepted service-forbidden writes. Database tests do not prove production service consumption.
- Security/Product owns unapproved Q12/Q22 KDF/code/mobile/TTL/limit/rate/ticket policy. No production value, ticket reconstruction or playback rule was invented.
- Platform/Database/Security owns deployment identities, image pinning, retention/legal/storage/KMS and production provisioning; Backend QA owns bounded-fixture limitations/throughput/deadlock testing and Windows CRLF contract-parser portability. The successful canonical snapshot is not evidence that the unchanged Windows checkout's Node command succeeds directly.
- Controller owns provider/model attestation, evidence archival, independent review and later gate acceptance. No unresolved local S3 database blocker remains; local-only and portability limitations are explicit. No remote CI, PR state or production readiness is claimed.

No sharing application service, API, worker, Redis/COS/KMS integration, ticket issuance/reconstruction, playback, production policy, V010 or change to V001–V009/POM/application/test-resource/design/controller/prior artifacts was made. No staging, commit, push, branch creation, PR/issue/reviewer request, code review, approval, external comment or other external workflow action occurred. Stop at S3 author completion after the post-record scope/frozen-byte/whitespace audit.
