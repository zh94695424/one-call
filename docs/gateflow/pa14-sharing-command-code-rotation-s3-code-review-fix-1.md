# PA14 Extraction-Code Rotation — S3 Code-Review Fix 1

## Identity And Disposition

- Task: `pa14-code-rotation-s3-code-review-fix1-gpt-20261105`.
- Work unit: `PA14-sharing-command-code-rotation`; Gateflow gate: `fix`; date: 2026-11-05, Asia/Shanghai.
- Author: current Codex CLI session; requested routing is GPT `gpt-6.1-sol` / high, not independently attested runtime-model metadata.
- Status: **`fix-complete`**, author completion only. Accepted low finding **CR-F1 is fixed**. No gate acceptance, approval, independent re-review, remote CI or production-readiness claim is made.
- Scope: one existing test-method mapping extension and this new artifact only. The accepted end-to-end S3 slice and all production behavior remain unchanged.

## Candidate, Base And Fix Identity

No commit is created; the fix is identified by its content hashes, not an invented fix commit.

| Identity | Exact value |
| --- | --- |
| Merged implementation baseline | `d9cc5bd22f6eda0523f989348f66b09137087fc0` |
| Reviewed candidate HEAD recorded by both reviews | `83f910b91952a09b084e601e5e5d7f62a868f97c` |
| This fix's intake and unchanged closing HEAD | `24653c4629b5cf8c5a6745fad83aa9b36f5a78f1` |
| Intake tracked candidate diff SHA256 | `de3f4a669f2f5a7cfa2e33f05e97662ee6a99576d4a26e03317a8b9f0020f77d` |
| Fixed tracked candidate diff SHA256 | `281b118dfdac2443fd2ef5e8e90d67a707bc576acc962eb65c9249b8fb898648` |
| Fix-only unified test diff SHA256 | `0f19a0b3444d854e742233e0b8ccbd6838d437bf8bd221d026edaa793af7c3f7` |
| Intake test raw SHA256 | `87b0ff0fcfc2ce71d86db234b48709af1c164902b628d76d745c7d6e6f911efc` |
| Fixed test raw SHA256 | `e3aefa35ccb5cb37f81b9a5d2a0f18752271b78ecda03437015c25b4412a7470` |
| Intake / fixed test LF-normalized Git blob IDs | `7a9cae5320ba6af3a83d7209c0646cdabbe11cfe` / `a980690467fd3e0264225f7f7ae0da26ad026f36` |
| Raw intake manifest, 1,348 nonprivate paths, SHA256 | `bb3512cb503f00dc17bacc465668c9f511b668fd03e2176f629a7a3f16a229c7` |

Tracked diff hashes use `git -c core.safecrlf=false diff --binary --no-ext-diff`. The fix-only diff is `/tmp/pa14-fix1-only.diff`, generated with Python `difflib.unified_diff` from the raw intake test and fixed test, with repository-relative filenames and three context lines. Git blob IDs above normalize CRLF only in memory and use `git hash-object --stdin` without `-w`. Raw manifest comparison separately proves that no unrelated byte or line-ending change occurred.

Binding inputs: Control's active PA14 rotation/fix checkpoint; the accepted 317-line plan; the 250-line implementation artifact; both code-review artifacts; the current candidate diff and complete web-test class; unchanged OpenAPI rotation error mappings and the shared filter/authentication/Problem path. Accepted plan SHA256 remains `c1d4de2813abad92d9d6077dc1221fcf888dd312831e7b4acaed27f0b0f0cb0c`; implementation artifact remains `cde846e97b2097573cb061d6d8693b1afe6e0b27d5abf1d5cea8bc789600e03a`; reviews remain `97c26e621749a61bc018314ebfc737ef093367b850f199b153afa5b96e8c4bd1` and `ddd00c049c8f36f40460b2988dbd1ecafe131979f1abe7681b7c47be238b2ac6`. Current Control raw SHA256 is `c06c6f050e28506bc674c4c2be375b9aa62bb12dc486cf70ce5c6185ddf98864` and is preserved.

## CR-F1 And Exact One-File Test Diff

Plan §8.3 requires all eight rotation error-code mappings. The existing rotation loop had seven, omitting `ShareCommandException.Code.UNAUTHENTICATED -> 401`. Add that pair in `CustomerSharesCommandWebTests.revokeAndRotateMapClosedResponseUnions`, using the existing `problem()` helper:

```diff
--- backend/src/test/java/com/onecall/archtests/CustomerSharesCommandWebTests.java
+++ backend/src/test/java/com/onecall/archtests/CustomerSharesCommandWebTests.java
@@ -173,7 +173,8 @@
                 }
             }
         }
-        for (var row : Map.of(ShareCommandException.Code.FORBIDDEN, 403, ShareCommandException.Code.NOT_FOUND, 404,
+        for (var row : Map.of(ShareCommandException.Code.UNAUTHENTICATED, 401,
+                ShareCommandException.Code.FORBIDDEN, 403, ShareCommandException.Code.NOT_FOUND, 404,
                 ShareCommandException.Code.SHARE_TRANSITION_CONFLICT, 409, ShareCommandException.Code.VALIDATION_FAILED, 422,
                 ShareCommandException.Code.RATE_LIMITED, 429, ShareCommandException.Code.SERVICE_UNAVAILABLE, 503,
                 ShareCommandException.Code.INTERNAL, 500).entrySet()) {
```

The added iteration injects the command exception through the existing authenticated MockMvc rotation fixture. It exercises the existing HTTP 401 mapping, `WWW-Authenticate: Bearer`, five-field Problem response, code/status/type/trace validation, cache/security headers and sanitization. It is not a new missing-bearer fixture or an authentication implementation change. Production authentication remains owned by `CustomerBoundaryFilter`: admitted route → authenticator → existing catch/Problem writer. No production defect was exposed and no production edit is justified.

A static comparison confirms the loop now contains exactly `UNAUTHENTICATED/401`, `FORBIDDEN/403`, `NOT_FOUND/404`, `SHARE_TRANSITION_CONFLICT/409`, `VALIDATION_FAILED/422`, `RATE_LIMITED/429`, `SERVICE_UNAVAILABLE/503`, `INTERNAL/500`, matching the eight unchanged OpenAPI rotation codes. No revoke-only `SHARE_GENERATION_STALE`, `SHARE_REVOKED` or 410 entry is introduced. Existing assertions, including wrong-result rejection, secretless replay and query noninteraction, are not removed or weakened. Existing service/IT state and witness assertions remain intact and pass.

## Execution Environment And Commands

All runtime commands use `danger-full-access` and only these process-local overrides; no repository, WSL, Docker-daemon or environment configuration is edited:

```sh
export DOCKER_HOST=tcp://172.20.211.232:2375
export TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
AGENT='-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar'
FOCUSED=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests,SharingCommandPersistenceConfigurationTests,CustomerWebConfigurationTests,SharingQueryBoundaryTests
java -version
mvn -v
docker info --format '{{.ServerVersion}} CPUs={{.NCPU}} Memory={{.MemTotal}}'
mvn -B -ntp -f backend/pom.xml "$AGENT" -Dtest=CustomerSharesCommandWebTests test
mvn -B -ntp -f backend/pom.xml "$AGENT" -Dtest="$FOCUSED" test
mvn -B -ntp -f backend/pom.xml "$AGENT" -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify
mvn -B -ntp -f backend/pom.xml "$AGENT" -Pcontainer-smoke clean verify
```

Observed toolchain: Temurin OpenJDK **21.0.13+9 LTS**, Maven **3.9.9**, Docker server **29.9.2**, four CPUs and **8,328,425,472 bytes** memory. Prerequisites, Docker API and explicit Mockito-agent existence checks pass. Testcontainers **2.0.5** starts ordinary Ryuk **0.14.0**, logging that it monitors and terminates containers on JVM exit. Ryuk is not disabled or bypassed. The unchanged fixture image is `postgres:16`; JDBC uses the authorized reachable gateway `172.20.208.1`.

Both targeted and full runs execute `SELECT version(), current_setting('server_version')` in the existing rotation IT and record:

```text
PA14 PostgreSQL version: PostgreSQL 16.15 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit; server_version=16.15
```

This satisfies the binding **PostgreSQL major 16** requirement; no patch version is assumed or newly pinned.

## Validation Results

Runtime validation commands and the corrected closing audit wrapper exit **0**. No required validation is omitted, reduced, skipped or combined with older reports to manufacture acceptance. With this Git version, `diff --no-index --check /dev/null <nonempty-new-file>` returns **1** for the file difference even when it emits no whitespace diagnostics; the closing audit checks both that exit status and empty diagnostic output rather than misclassifying the ordinary difference as a failure.

| Command / evidence under `/tmp/pa14-fix1-` | Result, 2026-11-05 +08:00 |
| --- | --- |
| Tool/version/Docker/agent checks, `tools.log` | Pass; observed versions above; started 21:31:10. |
| Focused web class, `web.log` | **4 tests**, failures/errors/skips **0/0/0**; BUILD SUCCESS; 21.394 s; finished 21:32:59. |
| Plan §10 six-class focused suite, `focused.log` | **29 tests**, **0/0/0**; BUILD SUCCESS; 49.297 s; finished 21:34:09. |
| Plan §10 targeted command IT verify, `targeted.log` | **79 Surefire + 18 command IT**, **0/0/0**; BUILD SUCCESS; 1:56; finished 21:36:29. This alone is not full acceptance. |
| Full `-Pcontainer-smoke clean verify`, `full.log` | **79 Surefire + 118 Failsafe = 197**, **0/0/0**; BUILD SUCCESS; 4:10; finished 21:40:40. |
| Existing CI `assert container smoke actually ran`, `inventory.log` | Exact **source=CI=XML**, 16/79 Surefire and 6/118 Failsafe; no duplicate/missing/extra names/classes; zero failures/errors/skips. |
| Existing CI `default and opt-in web jar smokes`, `jar-smokes.log` | Default non-web/database-free and opt-in web missing-verifier fail-closed smokes pass; no DB/verifier adapter; JVM cleaned up. |
| Existing CI `production jar dependency firewall`, `firewall.log` | All **82 adversarial probes** pass; baseline37/34, web55/48, counterparts47, dependency-only8, packager1; deltas18/0 and14/0. Dependency-list BUILD SUCCESS, finished 21:41:29. |
| Independent fresh XML aggregate check, `xml.log` | 16 Surefire classes/79 and six Failsafe classes/118; every XML suite and exact testcase count has failures=errors=skipped=0. |
| Raw scope/exact replacement/method-delta checks, `pre-artifact-audit.log`, `only.diff` | Exactly the intended raw test replacement; zero method/class delta caused by this fix; all other 1,347 intake files byte-identical; empty index and unchanged HEAD. |
| Closing whitespace/changed-path/protected-path/index audit, `final-audit.log` | Pass; exact closing scope and preservation checks described below, including explicit untracked-file whitespace checks. |

The first closing audit wrapper stopped at the provider's no-index exit 1 because it incorrectly used `check=True`; all scope, raw-byte, method and empty-index assertions before that point passed. This is an audit exit-code interpretation error, not a source/test/tool-availability defect. Its output is retained in `/tmp/pa14-fix1-final-audit-initial.log`. The corrected audit requires status 0 or 1 **and no stdout/stderr diagnostics** for each untracked whitespace check, rejects any other status or diagnostic, and completes every closing assertion. No source or test change is made in response.

The full clean run's six Failsafe suites are `PostgresContainerIT` 64, `CustomerMeIT` 10, `SharingQueryServiceIT` 10, `CustomerSharesIT` 8, `SharingRoleLeastPrivilegeIT` 8, `CustomerShareCommandsIT` 18: **64+10+10+8+8+18=118**. Reports are fresh ignored files under `backend/target/{surefire,failsafe}-reports`. Expected negative-fixture error logs are not test failures; the Maven exits, per-suite XML and existing exact inventory all pass.

The three existing CI `run` bodies are extracted from `.github/workflows/ci.yml` by their exact step names, stopping at the YAML scalar indentation boundary, and executed in `backend` using `bash -e -c`. The only local executable substitution is `./mvnw` → `/opt/maven-3.9.9/bin/mvn`. No assertion or body is replaced. Exact extraction/execution results are in `/tmp/pa14-fix1-ci-bodies.log`; pre-substitution body SHA256 values equal the existing implementation evidence:

| Existing CI body | SHA256 |
| --- | --- |
| Exact inventory | `6ea12e5f56c95882f9c160b8c8abd3c752fbd8123b66807f82c165620469e491` |
| JAR smokes | `1ba848682b26ead5cfc300d11e08a7269784e52c6c67e53029a2a9164c023ab4` |
| Firewall | `05f23f1538f94c1b1c01c5a688de3d973659752d4ddab6c039a8df4dd13a3bc7` |

## Scope, Inventory And Protected Paths

Read-only Linux Git workaround for every Git operation:

```sh
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call14
export GIT_WORK_TREE="$PWD"
export GIT_OPTIONAL_LOCKS=0
git -c core.safecrlf=false diff --check
git -c core.safecrlf=false diff --name-only
git diff --cached --name-only
git ls-files --others --exclude-standard -- . ':!.codex' ':!.codex/**'
git -c core.safecrlf=false diff --no-index --check /dev/null docs/gateflow/pa14-sharing-command-code-rotation-s3-code-review-fix-1.md
```

- Intake manifest covers **1,344 tracked files plus the four pre-existing nonprivate untracked candidate/artifact files = 1,348 paths**. Every raw hash except the allowed web test remains identical. Reversing precisely the displayed mapping replacement reconstructs the intake test byte-for-byte, including mixed line endings. No normalization is written to disk.
- Exact changed-path audit compares tracked paths to the frozen intake candidate diff and nonprivate untracked paths to the explicit allowed set. Closing total is **19 modified tracked candidate paths + five nonprivate untracked paths = 24**. The five are the existing rotation provider, existing implementation artifact, both existing Controller-copied code reviews, and this new fix artifact. There is no deletion, rename or additional task-created repository path. Full exact path output is in `final-audit.log`.
- Task delta is exactly **one modified existing test and one created fix artifact**. All production files, the rotation provider, CI, README, Control, plan/plan-fix/reviews, implementation artifact, other tests, V001–V010, OpenAPI, POM and application configuration remain byte-identical to intake. All protected paths remain untouched.
- The modified web class still has exactly its four existing `@Test` methods, with identical names. This fix adds/removes/renames/disables **zero methods and zero classes**. Relative to the merged baseline the candidate retains exactly its original **one Surefire + five Failsafe** named additions and no removed baseline method. Source=CI=XML remains **79/118/197**; the focused/targeted reruns are duplicate executions, not extra inventory.
- `git diff --check` and explicit `git diff --no-index --check /dev/null` checks for all five nonprivate untracked files pass. Index remains empty and HEAD remains `24653c4629b5cf8c5a6745fad83aa9b36f5a78f1`. No Git metadata is rewritten.
- Private untracked `.codex` is excluded from enumeration/hashing and is not inspected, copied or modified. Validation creates only ordinary ignored build outputs and ephemeral `/tmp` evidence in addition to the two authorized task paths.

## Residual Risks And Stop Boundary

| Residual risk / uncovered area | Owner / destination |
| --- | --- |
| Production rotation policy, approved KDF/material custody and provisioning remain intentionally unspecified; missing adapters fail closed. | Security/Product/Sharing Backend → separately approved provider/security design and provisioning gate. |
| Local Docker evidence depends on the Controller-owned temporary bridge/gateway; `postgres:16` can drift within major 16. | Controller/Platform → runner/bridge lifecycle and actual version recording; Backend/Database → ordinary Docker-capable execution. |
| Frozen V009 guards structurally admit some service-forbidden shapes. | Sharing Backend/Database → retain passing exact state/mode/witness/rollback/concurrency assertions; no DDL change here. |
| First-response loss cannot recover plaintext code through exact replay; abuse/rate limiting is gateway-owned. | Sharing/Product → frozen one-time-secret/secretless-replay contract; Security/Platform → existing gateway controls. |
| These are fresh local author results, not remote CI, independent re-review, production, load or security acceptance. | Controller → decide any subsequent authorized gate; no subsequent gate is started here. |

No blocking tool/runtime evidence gap or open question remains for this bounded fix. The accepted state/replay/policy/material/update/fact/rollback/concurrency rules and one-time-secret semantics are unchanged and their existing full regressions pass.

**No other repository path or external Git action was touched.** No staging, commit, push, branch creation, PR creation/update, merge, approval, ready action, reviewer request, external comment or issue change occurs. No private `.codex` access, environment configuration edit, Ryuk bypass, skipped test, weakened assertion or re-review occurs. Stop after this bounded fix, evidence and scope audit; return author completion only.
