# PA16 Public-Read ACL — Code-Review Fix 1

## Status And Boundary

- Status: **`fix-complete`**, 2026-11-06, Asia/Shanghai (+08:00). Gate: `fix` after code review; no later gate or review verdict is attempted.
- Work unit: `PA16-sharing-public-read-least-privilege-acl`; task label: `pa16-public-read-acl-code-review-fix1-gpt-20261106`.
- Branch: `codex/pa16-sharing-public-read-least-privilege-acl`; verified starting HEAD: `a4f7ac93963acd4d9e7dfcc288fe7de8c29f81ac`.
- Authority: current Control code-review adjudication, accepted plan §9 and plan-fix-1, implementation report, and reviews A/B dated 2026-11-06 12:11:14 / 12:16:09. The accepted finding is **A-F-CONCURRENCY**, a test-evidence defect, not a demonstrated production defect.
- Sole author, no delegation. Source/document writes are exactly the following three paths. No production, migration, CI, README, protected test, plan, Control, or review bytes changed in this fix. Generated Maven output and private evidence are not candidate source changes.

| Exact path | Before raw SHA-256 | After raw SHA-256 |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PublicSharesFixture.java` | `8629e1a4583d496b7bd433fdb33f1b229ad7707cc4f862676019ae58116f5d92` | `4d7cdfe83d630c63dfc7e9b0268b971ea2a64729ec4a5f9ed0a1e54c93f28568` |
| `backend/src/test/java/com/onecall/archtests/container/PublicSharesIT.java` | `9f6a31d047407fb4457d554d33893ddf05370ebdb467c8d39f32a9356f107187` | `7c08d2c6e523376eb6fb18c4d98598858133fb44959c7066c46158ae17c7c842` |
| `docs/gateflow/pa16-sharing-public-read-least-privilege-acl-code-review-fix-1.md` | Absent | Recorded externally in private `final-paths.sha256` and the completion response; intentionally not self-embedded. |

## A-F-CONCURRENCY Resolution

All additions execute from the existing `PublicSharesIT.publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState`; no JUnit method or class was added or renamed by this fix.

1. The public test pool temporarily grows from maximum 1 to 2. Two workers enter the real named public transaction template, borrow transaction-aware handles, and use a two-party `CyclicBarrier` twice: after both transactions are acquired and again after their reads. Thus neither transaction can finish before both have acquired connections and completed their projection reads. Each worker performs the real digest repository lookup and invokes the real named public MANDATORY projection proxy using `fixture.dsl`; the expected evidence ID and nonempty artifacts are asserted. Before/after identity observations must be equal. The distinct-PID public SQL stream must contain a grant lookup and both evidence projections for **each** PID, remain entirely `current_user=session_user=sharing_public_read_role`, remain READ ONLY `on`, and contain no SET ROLE transition.
2. A second two-phase barrier holds one real public transaction and one separately borrowed command transaction simultaneously. Their backend PIDs must differ. Public identity/read-only assertions are independent of command assertions: command `current_user=sharing_command_role`, `session_user=commandLogin`, READ ONLY `off`, stable across both phases. Command SET LOCAL ROLE is rolled back; it is never performed on the public connection.
3. Each public callback unconditionally marks rollback-only and explicitly rolls back in `finally`; try-with-resources closes its transaction-aware handle and the real public manager releases the pooled connection. The command worker also unconditionally rolls back and closes. Both workers reborrow on the same thread and assert the **same backend PID** is reset to its login identity, server READ ONLY `off`, JDBC readOnly `false`, and autocommit `true`. The two public backends are also borrowed together after release and checked individually; both pools receive independent post-dual-borrow reset checks.
4. Every barrier wait is bounded at 15 seconds; futures at 40 seconds; executor shutdown/termination at 15 seconds. Identity statements and command role setup have 10-second JDBC query timeouts; public projection SQL has a transaction-local 10-second statement timeout. Futures are cancelled and the executor shut down in `finally`, without an unbounded executor close. Pool maximum is restored in an unconditional nested `finally`; soft eviction retires the extra idle backend rather than relying only on the maximum-size setter. The retained six HTTP reads then succeed, and reset plus actual physical pool size **1** are asserted.
5. The original single-connection same-PID/SQLSTATE `25006`/identity/autocommit reset proof remains **before** the new block, unchanged. The retained 11-table snapshot still matches after all public-only phases. The subsequent real command create still produces a nonempty writable command INSERT stream with the correct command role/session, while the public stream remains unchanged. All public observations, including the new phases and retained HTTP reads, additionally assert narrow identity and READ ONLY `on`.

Executable witnesses, also verified inside the XML testcase rather than only aggregate logs:

| Run | Two-public barrier | Simultaneous public/command barrier | Preserved original PID reuse and subsequent command |
| --- | --- | --- | --- |
| Final targeted | public PIDs **117, 120** | public **120**, command **118** | PID **117**, `25006`, reset true, positive writable command INSERT, public stream unchanged |
| Fresh clean full | public PIDs **108, 111** | public **111**, command **109** | PID **108**, `25006`, reset true, positive writable command INSERT, public stream unchanged |

Both runs emit `twoPublicBarrier=passed` and `simultaneousPoolBarrier=passed`, with distinct identities/PIDs and `bothReset=true`. The original PID-reuse witness is not substituted by either new barrier.

## Commands, Toolchain And Results

Actual environment for Maven/container execution:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
export DOCKER_HOST=tcp://172.20.211.232:2375
export TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1
```

`mvn` resolves to `/opt/maven-3.9.9/bin/mvn`. Versions actually queried with `java -version`, `mvn -version`, `docker version --format 'client={{.Client.Version}} server={{.Server.Version}}'`, `docker info`, `python3 --version`, and `/opt/jdk21/bin/jar --version`: Temurin **21.0.13+9 LTS**, Maven **3.9.9**, Docker client/server **29.1.3 / 29.9.2**, Python **3.10.12**, jar **21.0.13**. The tests actually booted PostgreSQL **16.15** from `postgres:16`; the full-run image identity is `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`.

Commands in the first three rows run from the worktree root; the remaining Python/JAR/dependency commands run from `backend`. Every acceptance command exited **0**.

| Actual command | Acceptance evidence |
| --- | --- |
| `timeout 300 mvn -B -ntp -f backend/pom.xml -Dtest=PublicShareWebTests,PublicShareQueryServiceTests,PublicSharePersistenceConfigurationTests,CustomerApiBoundaryTests,SharingQueryBoundaryTests,ModuleBoundaryTests test` | `focused-final.log`: **6 classes / 24 tests**, zero failures/errors/skips; finished **12:28:31 +08:00**. Six selected final XML reports independently total 24. |
| `timeout 600 mvn -B -ntp -f backend/pom.xml -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT,PublicSharesIT verify` | `targeted-final.log`: **12+8=20 Failsafe**, zero failures/errors/skips; Surefire also 91; finished **12:31:02 +08:00**. Both new barriers executed in the existing method. |
| `timeout 900 mvn -B -ntp -f backend/pom.xml clean -Pcontainer-smoke verify` | `clean-verify-final.log`: fresh **19 classes / 91 Surefire + 7 classes / 130 Failsafe = 221**, zero failures/errors/skips; **5:19**, finished **12:36:22 +08:00**. Archived XML independently verifies counts, individual cases and both barrier witnesses. |
| `python3 /tmp/pa16-fix1-evidence/inventory.py` | `inventory-final.log`: exact **source=CI=XML**, 26 classes / 221 method identities, no duplicate/disabled/conditional/parameterized/generated substitution. PublicSharesIT remains exactly eight accepted identities. Read-only comparison to starting HEAD still gives exactly the already approved **four added / two renamed** methods, with no additional delta from this fix. |
| `timeout 300 python3 /tmp/pa16-fix1-evidence/jar-smokes.py` | `jar-smokes-final.log`: unchanged default/non-web and opt-in web packaged-JAR smokes, all **nine** original HTTP probes, no DB/verifier adapter, JVM cleanup passed. |
| `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar`; `timeout 300 mvn -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt`; `python3 /tmp/pa16-fix1-evidence/firewall.py` | `firewall-final.log`: **82/82 adversarial probes**, baseline37/34, web55/48, counterparts47, dependency-only8, packager1; deltas18/0 and14/0. Checked the fresh clean artifact. |
| From root: `sha256sum backend/src/main/resources/db/migration/V0{01,02,03,04,05,06,07,08,09,10}__*.sql` | `migrations-raw.sha256` and `migration-and-scope.log`: **10/10 raw bytes unchanged**, matching pre-edit baseline and accepted plan; independently **10/10 canonical LF** digests match plan constants. |

Focused 24 and targeted 20 are overlapping subsets, not additions to 221. These are local executions of the existing CI checks, not a hosted-CI or production-deployment claim.

### Failures And Recovery

- **No Maven acceptance run failed**, and no required check was skipped. Preliminary focused/targeted runs also passed; their logs/XML are retained as `focused-first.log` / `targeted-first.log`. They preceded the final idle-backend eviction refinement and are not substituted for the final runs above.
- Plain `git status` initially failed because the worktree's `.git` points to Windows `D:/codex-call/...`. Read-only Git queries succeeded using `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call16 --work-tree="$PWD"`, without repairing or writing metadata. HEAD and branch were confirmed. No commit/stage/fetch/push/branch/PR/issue/comment/approval/ready/reviewer action occurred.
- A private archive-count assertion initially assumed a targeted copy of `TEST-*.xml` contained only two files. The pre-clean target still contained five unrelated older reports. Recovery selected exactly the **two commanded final suites**, verified 12+8, and verified the new markers inside the existing PublicSharesIT testcase. The focused archive similarly uses exactly its six commanded suites. The subsequent **clean** full run removed stale reports and independently provided exactly 19+7 fresh suites; its entire inventory passed unchanged. This was an evidence-selection assertion, not a JUnit failure or a waiver of a failing test. Recovery is retained in `targeted-archive-recovery.log`.
- Expected negative migration probes still emit Flyway refusal/rollback ERROR messages; their asserted test results are green. Normal Mockito/JDK attachment warnings are not ignored test failures.

## Frozen Bytes And Evidence Identities

Private evidence is retained outside the candidate at `/tmp/pa16-fix1-evidence` (directory mode 0700), including original/final logs, selected focused/targeted XML, all final clean XML, exact CI-extracted scripts, XML witness checks, and hash manifests. No deployment credential was used or recorded. A pre-edit manifest covered **1,414** existing files, excluding `.git`, generated `target`/`build`/`node_modules`, and `.codex`; all pre-existing files except the two authorized test sources remain byte-identical, and this report is the only new candidate path. All **14 other frozen implementation paths** also match both the implementation report's recorded hashes and this fix's pre-edit hashes.

| Protected input | Unchanged raw SHA-256 |
| --- | --- |
| Current Control | `1674b222e518b5e762fa2bdda0dd967eed4fab33cd3746ce02ea375d1f54551c` |
| Accepted plan | `400f7e69ecd55480c0d05dc7dfda29db1bf4c0ea011b8e680f1f352b00d2be21` |
| Accepted plan-fix-1 | `61adfc65b904ecc920f65b53a8ecb6572615dda40df39dca53726f9de1d52ba9` |
| Implementation report | `2599ea53a339cbe0ee38102a04f7304518c588763369b0172e662da6ee1d58cc` |
| Review A | `d0ee977f67fffa906b1db6cfe33bdcef77b7c2c47c7b7f3291c884e198a67de1` |
| Review B | `20b65be5c76af7fe241ec29c7513c35694edb40c7bcc5d52871a6951459f782e` |
| `.github/workflows/ci.yml` | `635ee5a689fbb6007fce2e1ec31fc289a0b80e6dbc4014c41255560d8ecea976` |
| `backend/pom.xml` | `2dc46005a76ec6af10d6dbec797f5285cb74d569febc36bfe9efa0d78c060f74` |
| V011 | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |

| Frozen migration | Unchanged raw SHA-256 |
| --- | --- |
| V001 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| V002 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| V003 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| V004 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| V005 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| V006 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| V007 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| V008 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| V009 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| V010 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |

The three private executable scripts are byte-identical to the current CI Python blocks after checkout-line-ending normalization, and to the implementation evidence scripts. They were **all rerun**, not merely hash-compared:

| Script / artifact | SHA-256 |
| --- | --- |
| `inventory.py` | `df583d0e2ab58d523ffa134e043770d23b98edeec760f4882b9a00cdfa2a7876` |
| `jar-smokes.py` | `bd5c0d615d0b0829bd60e355fccc4848b622537b3c8286d9bf8529769c34b420` |
| `firewall.py` | `da7a000423cd871daee755854034cca42be7a4ac13dfa01a1f8376ba520bdbd3` |
| Fresh packaged JAR | `dc5af673f92dec2845c892c76560d9c011984c8e2e36ffb6e5e3cd3ec1f7b44d` |
| Pre-edit `before.json` | `208df511f56d7c85527c1999738ee70e811c171c8d21199e784ca91de0ae0ae5` |
| `focused-final.log` | `62b59f5189016e2a955d25ce056bbf3ca1e6ad4a2e94ec9cf2183d98e6553b92` |
| `targeted-final.log` | `4c9eb3277ba92e576e26f92696d5b23072633b9d67a9480579f4dba03daae5ec` |
| `clean-verify-final.log` | `15bb0046bd854e9d43f14ffbcae263b85fc38863508789cf23a40da5ccfc60d1` |
| `inventory-final.log` | `c5098640662eff4d828429064d1d0639db6281332e6d88988e0eefe5e4f13c9e` |
| `jar-smokes-final.log` | `d41b148440f150df2d6f3e52aaba38aa384f6d4cf43ea42cff55279a5af651dc` |
| `firewall-final.log` | `77998317971ef90bc97617855ebedba4e0f4b9b8336125d321eb572fd605d222` |

## Intentionally Unchanged And Residual Risk

- **B-OQ-1/B-OQ-2/B-OQ-3 are intentionally not implemented**, following the Controller's rejected-with-reason/non-material/fail-closed adjudication.
- Foreign-server and large-object ACL enumeration is not expanded; it remains the adjudicated **test-maintenance residual**. No accepted assertion required production or migration changes, so the stop-and-report production blocker condition was not reached.
- Controlled two-connection PostgreSQL 16.15 evidence is not a production load/capacity or deployment-authentication proof. Existing DBA/SRE ownership of external PUBLIC TEMP consumers, credential injection/rotation, pg_hba/TLS, deployment ordering and ACL drift remains unchanged.
- No required local acceptance remains pending or skipped. Stop at **`fix-complete`**; no staging, commit, remote action, self-review, or later gate.
