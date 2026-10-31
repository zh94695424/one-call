# PA9 V009 Sharing Foundation — S3 Code Fix 1

## Authority and status

- Task: `pa9-v009-s3-codefix-gpt-20261031-01`; work unit: `PA9-v009-sharing-persistence-foundation`; date: 2026-10-31, Asia/Shanghai; authorized gate: **fix only after S3 dual review**.
- Branch: `codex/pa9-v009-sharing-foundation`; intake and final unchanged HEAD: `209b658e249ef26e22be69db77726de63cc255f4`. Accepted S2 `6111a535918ca0d53be893351d736558fa5aaa3a` is an ancestor, exit 0.
- Accepted plan: `docs/gateflow/pa9-v009-sharing-foundation-plan.md`, canonical Git-byte SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`. Current control, V009, both review artifacts, S3 tests/helpers and V008 cleanup were read; only accepted F1/F2 were implemented.
- Runtime/provider: OpenAI assistant through Codex CLI, Ubuntu 22.04/WSL2, Linux amd64. No subagent or alternate provider was used. The actual backing model/provider route is not independently attested by terminal/database output; the requested controller route is not represented as verified runtime identity.
- Status: author fix-complete with local database validation; **not** dual re-review, controller acceptance, remote CI, approval or production readiness. Fresh dual re-review remains a separate controller gate.

## Preflight and frozen review input

Ordinary Git reads initially failed because `.git` points to Windows `D:/codex-call/.git/worktrees/codex-call7`. Commands then used environment-only WSL metadata overrides; no Git pointer/config repair, staging or history mutation occurred:

```sh
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7
export GIT_WORK_TREE="$PWD"
export GIT_OPTIONAL_LOCKS=0
git rev-parse HEAD
git merge-base --is-ancestor 6111a535918ca0d53be893351d736558fa5aaa3a HEAD
git diff HEAD -- backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java .github/workflows/ci.yml backend/README.md
docker version
docker image inspect postgres:16 --format '{{.Id}} {{json .RepoDigests}}'
docker run --rm postgres:16 postgres --version
JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH /opt/maven-3.9.9/bin/mvn -version
```

All frozen input identities matched before editing:

| Input | SHA256 |
| --- | --- |
| Three-source pre-fix diff | `4dc8808070a44a7827020f8ae09560e6bdbc61b92dddd016548689f3e8f8965f` |
| Pre-fix Java | `eb84f41a547f7c3d675f4d6106cc2ddb475b928d90eb5f32f9e18d114a022175` |
| Pre-fix README | `1c1f94b31674121446484033d3e7d706b2cff641de4e5035a17dc443e769bcb9` |
| Pre-fix I3 | `78e99a936851b2279613f8d98eda45e2f1d24cdbcd8a0c09ff382d64653729ac` |
| Pre-fix E3 | `877d8fe1755c7d543e45bf654844eb8d081f03c68d5abde98279b0370b33842b` |
| Review A, `docs/reviews/code-review-20261031-181213.md` | `964287db995eea00bb372fb49cb4fcd2dca78c74c15946ea4463ef9bec598bb4` |
| Review B, `docs/reviews/code-review-20261031-181841.md` | `e5638e10820ddab4c6fd86fb98488c36f03c53a0b909a7ee945b0572e1d910aa` |

Docker client 29.1.3; Docker Desktop 4.97.0 (244584), Engine 29.9.2; Temurin Java 21.0.13 at `/opt/jdk21`; Maven 3.9.9 at `/opt/maven-3.9.9`; Testcontainers 2.0.5; actual PostgreSQL 16.15. Observed `postgres:16` image/digest: `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`. This is observation, not a production image pin.

## Findings and exact changes

### F1 — fixed by author

Only two README substitutions: line 3 current cumulative count `62` → `64`; line 117 current CI expected-set `62 names` → `64 names`. The only remaining standalone 62 in README explicitly says **accepted S2 checkpoint**, immediately contrasted with current S3 total 64. No current-state 62 claim remains.

### F2 — fixed by author

Only `v009S3Fixture()` changed in Java; both S3 tests, all other 62 test bodies, every other helper and all existing assertions remain identical to frozen pre-fix source.

1. Allocate the synthetic account UUID before committing the V008 fixture, so UUID allocation is not an unprotected post-fixture step.
2. Keep the S3 account insertion, evidence SOLIDIFIED update, immediate constraints and commit in the same normal transaction. Replace the inner unconditional rollback-finally with a failure handler that attempts rollback and suppresses rollback failure on the original `Exception | Error` before rethrowing. Successful commit no longer performs an unnecessary rollback.
3. Return the complete fixture inside the connection resource boundary. An outer `Exception | Error` handler therefore covers connection acquisition, setup, commit, fixture construction and resource close after V008 setup has returned.
4. On failure, independently open a cleanup transaction as `customer_iam_owner`, delete the scoped synthetic account if present (zero rows is valid before commit), and commit. Independently invoke existing `v008S2Cleanup(evidence)` even if account cleanup failed.
5. Suppress account/V008 cleanup failures on the original setup failure, then rethrow that original object. Java try-with-resources also supplies its normal resource-close suppression. Infrastructure failure is exposed, not hidden or represented as guaranteed successful cleanup.

No normal `v009S3Cleanup` assertion, trigger/FK restoration, timing, privilege, SQL predicate, race bound, test selector/count or production behavior was weakened. No CI, migration, POM, application/test resource, control, accepted plan/design or historical review change was made.

## Required validation and commands

Evidence directory: `/tmp/pa9-s3-fix-01/`; backend workdir for every Maven command. All Maven commands used:

```sh
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:$PATH
timeout 180s /opt/maven-3.9.9/bin/mvn -B -ntp test-compile
timeout 240s /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009AdmissionCountOnceLimitAndAtomicRollbackBackstops+concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner' verify
timeout 300s /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify
```

| Execution | Exit/result |
| --- | --- |
| test-compile, one attempt | 0; Maven 7.379 s |
| Focused two S3 methods, one attempt | 0; Maven 39.842 s; Failsafe 2 + Surefire 10, failures/errors/skips all 0 |
| Exactly one bounded full container-smoke run | 0; Maven 01:03 min; Failsafe 64 + Surefire 10, failures/errors/skips all 0; finished 2026-10-31 19:15:43 +08:00 |
| Independent Python AST/source/XML audit | 0; source = CI expected = full Failsafe XML exactly 64 distinct names, separate Surefire 10; focused set exactly the two approved S3 methods |
| Unmodified CI Python heredoc extracted and executed locally | 0; `container-smoke report assertions passed`; no workflow write or remote CI |
| Real-PostgreSQL external setup-failure probes, corrected attempt | 0; five failure modes plus unchanged normal setup/cleanup restore exact baseline |
| Whitespace/frozen/source/cleanup/scope audit | 0; unchanged HEAD/ancestor/CI/reviews/migrations, unchanged 64 test bodies and other helpers; no test container remains; generated-write qualification below |

Independent validation commands, repository-root workdir unless indicated:

```sh
python3 /tmp/pa9-s3-fix-01/audit.py > /tmp/pa9-s3-fix-01/final-audit.json
(cd backend && python3 /tmp/pa9-s3-fix-01/ci-validator.py)
sha256sum -c /tmp/pa9-s3-fix-01/source-during-full.sha256
git diff --check
git diff --cached --name-only
git diff HEAD -- backend/src/main/resources/db/migration
rg -n '\b62\b' backend/README.md
docker ps -a --format '{{.ID}} {{.Image}} {{.Names}}'
```

New untracked evidence records also receive `git diff --no-index --check /dev/null <path>` checks, accepting only normal diff exit 1 with no whitespace diagnostic. External `SHA256SUMS` records final file/log/XML/script hashes, including this artifact's non-circular self-hash. Source hash verification during/after full execution passed; there was no second full run for evidence-only updates.

### Setup-error and normal cleanup proof

External `/tmp/pa9-s3-fix-01/SetupFailureProbe.java` invokes the actual compiled test class's existing migration/setup/cleanup methods against a fresh real Docker PostgreSQL 16 container. A custom JDBC proxy injects faults without editing repository code or adding JUnit methods. Compilation/execution uses the actual test classpath extracted from Failsafe XML:

```sh
# Corrected probe compilation/run, workdir /tmp:
/opt/jdk21/bin/javac -proc:none -cp "$(cat /tmp/pa9-s3-fix-01/classpath)" -d /tmp/pa9-s3-fix-01 /tmp/pa9-s3-fix-01/SetupFailureProbe.java
timeout 180s /opt/jdk21/bin/java -cp "/tmp/pa9-s3-fix-01:$(cat /tmp/pa9-s3-fix-01/classpath)" SetupFailureProbe
```

| Injected failure after committed V008 fixture | Observed original/suppressed outcome |
| --- | --- |
| S3 connection acquisition fails | Same SQLException object; zero suppressed |
| Assertion before S3 status update, rollback reports failure after actual rollback | Same AssertionError object; exact rollback SQLException suppressed |
| S3 commit succeeds, then driver reports setup failure | Same SQLException object; committed synthetic account removed |
| Post-commit setup failure plus account-cleanup close reports failure after cleanup commit | Same setup SQLException; exact cleanup SQLException suppressed; V008 cleanup still executes |
| Post-commit setup failure plus V008-cleanup close reports failure after cleanup commit | Same setup SQLException; exact V008 cleanup SQLException suppressed |

After each case, a fresh direct connection compares **every application table's complete sorted JSON rows**, ordinary trigger enabled flags and FK validated/deferrable/deferred flags to the pre-probe baseline, not merely row counts. All five passed. The unchanged normal fixture and `v009S3Cleanup` then restored that same exact baseline. Suppressed-cleanup probes inject report/close failures after actual database cleanup; they do not claim that genuinely unavailable storage or denied cleanup writes can succeed.

Focused and full logs each contain exactly **18** `PA9 S3 observed Lock:` markers, **7** `PA9 S3 rollback/no-residue:` markers and **2** `PA9 S3 cleanup: scoped IDs absent; V009 catalog/triggers/FKs and V008 FK flags restored` markers. Independent audit checks these counts, source/test identity and actual XML flags. Final `docker ps -a` is empty, including the ephemeral PostgreSQL version/probe containers.

### Failed attempts and generated-write qualification

- Initial ordinary Git commands failed on the Windows `.git` indirection; environment-only overrides resolved them without repository repair.
- First external probe exited 1 before exercising setup faults: its metadata snapshot concatenated PostgreSQL internal `"char"` without `::text`, causing ambiguous `text || "char"`. The external-only query was corrected; the failed log remains `setup-failure-probe-attempt-1.log`. Required Maven commands all passed first attempt.
- First scratch javac compilation omitted `-proc:none`. The discovered Modulith annotation processor **inadvertently regenerated pre-existing root `build/generated-spring-modulith/javadoc.json`**, currently three bytes `[]\n`, SHA256 `37517e5f3dc66819f61f5a7bb8ace1921282415f10551d2defa5c3eb0985b570`. Its prior content hash was not captured, so strict no-build-write compliance/byte equality is **not claimed**. No manual build edit, deletion or attempted speculative restoration occurred. Corrected probe compilation used `-proc:none` from `/tmp`. This operational exception is disclosed, not treated as authorized scope expansion or controller acceptance.
- A pre-full scratch SHA command used root-relative paths from backend and emitted three missing-path errors. The single full Maven invocation still ran once and passed. Correct root-workdir source hashing during full execution and after execution matched; the empty erroneous `source-before-full.sha256` is not evidence.
- A dedicated `apply_patch` tool was not exposed in this runtime. All source/artifact edits used the installed `apply_patch` executable via the available command tool; no alternative file-edit mechanism was used.

## Final identities and exact source scope

Five manually edited/new task paths and final raw checkout SHA256:

| Path | SHA256 |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `9f089e03efd3bafce68e949dd590d8aaf8f29a943c90a9da43829a33f8e9dcb1` |
| `backend/README.md` | `c9395e749738e7c1435840bf88d8026ff8a4b32879efb9817a1b8356db73132f` |
| `docs/gateflow/pa9-v009-sharing-foundation-s3-implementation.md` | `7dfaef1b28994b1de9f621d498c69505c59f911f160a45d697ffef9923d956b6` |
| `docs/gateflow/pa9-v009-sharing-foundation-s3-completion-1.md` | `6f484ea2426f1492a6a3824f353578355fb40457f9f5a98f053be92414eee4f2` |
| `docs/gateflow/pa9-v009-sharing-foundation-s3-code-fix-1.md` | Non-circular self-hash in `/tmp/pa9-s3-fix-01/SHA256SUMS` |

Three-source diff against current intake HEAD: `b79ab38c3e4cd736a350ed53d89e64845f50d605bdb075ec4e63d236894981ad`. CI remains at raw SHA256 `9f04d0b0a710be0143b7977aa20a28f3492f0d281ac59f7bbb456da862f09238`, including its **pre-existing S3 intake delta**; this fix did not modify it. I3/E3 now begin with current F1/F2 dispositions and final source/evidence identities; their original pre-fix histories remain labeled historical rather than silently rewritten.

| Current evidence | SHA256 |
| --- | --- |
| Focused Failsafe XML | `cac228428c3ddb091507a21b8c17713169414e5b46cb0a3f682abcf289b8d9fb` |
| Full Failsafe XML | `f9e604fe5137050c65850f81d87fc1f06baa510c26e1b6ac3c02554919ff5f61` |
| Full Surefire OneCallApplicationTests XML | `f83cd093f5f32877066d2257819498ce728ca343f9ef9152d9b78653b27a5a8f` |
| Full Surefire BootstrapDependencyBoundaryTests XML | `09c4ab5f9d366331950d0d782e9668b63772acd0fa26808c81ec5e75b2332c2b` |
| Full Surefire ModuleBoundaryTests XML | `15611d96501f72f2c0a1dfc07e0c66ed2cbad93eca5a844f73b56ae0044b43d7` |
| Focused log | `27ed625d3ddde2179a015982a5631becf2e454aa6a3822eb3ca72bd461ce5c8e` |
| Full log | `e8387ae9a5e719f575e7cdcb6d5f17dabd682ca7f2de959b6753e8c4e05341ad` |
| Setup-failure probe log | `9699a8a6cd8df2283dcde8a891a4d2e4859836c5aed858d0181c6be6ce9045cf` |

V009 canonical SHA256 remains `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`. V001–V008 canonical hashes and byte-equality to accepted S2/HEAD are independently enumerated in `preflight.log` and `final-audit.json`; all nine checkout migrations equal canonical bytes after existing CRLF normalization, and `git diff HEAD -- backend/src/main/resources/db/migration` is empty. Plan and both reviews retain their frozen identities. Control is unchanged at `72c89bf9c342fe7bbb4ac14776e801bf3f178b2fa40e70ccdd10687b519b2852`; pre-existing `.codex` remains empty (SHA256 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`). Root `build/` has the generated-write exception above; backend target outputs and external `/tmp` probe/evidence files are validation output, not task source.

## Residual ownership and stop

- Backend QA: unchanged synthetic five-second expiry window, bounded races/timeouts, no throughput/deadlock-freedom claim, and raw-Windows CRLF-sensitive contract parsing. No timing/predicate relaxation was made.
- Sharing/Backend/Security: excluded production authorization/replay/typed JSON/binding/risk/fingerprint services and accepted CHECK UNKNOWN/schema semantics; no service, policy or schema hardening was attempted.
- Security/Product/Sharing: Q12/Q22 code/KDF/TTL/limits/ticket policy. Platform/Database/Security/Legal: provisioning, production image pinning, deployment/storage/KMS/legal gates.
- Controller/QA: provider attestation, evidence archival, remote CI and fresh independent dual re-review; Controller also owns disposition of the disclosed scratch-generated build write. There is no claim of remote CI execution or acceptance.

No stage, commit, push, new branch, PR, review request, code review, approval, external comment or other external workflow action occurred. No production service/API/worker/Redis/COS/KMS/ticket/playback/V010 work occurred. Stop after this fix artifact and I3/E3 evidence updates; do not enter re-review or any later gate.
