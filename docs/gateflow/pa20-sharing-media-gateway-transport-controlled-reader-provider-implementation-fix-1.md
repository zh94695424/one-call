# PA20 implementation fix 1 — Controller full-verification defects

## Identity, authority and target gate

- Task: `pa20-sharing-media-gateway-transport-fullverify-fix1-gpt-20261108`; date: 2026-11-08.
- Work unit: `PA20-sharing-media-gateway-transport-controlled-reader-provider`; target gate: **implementation**, after failed Controller runtime validation. This is not code review or gate acceptance.
- Authority: Active resume checkpoint in `docs/gateflow/project-execution-control.md`, accepted plan `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-plan.md` (SHA256 `ea603bf87358079b700a0d975b5b2b5fc24b4b905b05720b539d2459a41b8fdf`), and the bounded fix dispatch. Accepted implementation base remains `2e7b78ba83c050964a8b82790a2afc7d4fae51a1`.
- Scope: ONE composed PA20 slice, with exactly two test-only corrections. No production defect is evidenced by these failures; no production source is changed. No new/replacement test, assertion weakening, skip or inventory change is introduced.

## Controller evidence and exact defects

Read-only evidence:

- Full UTF-16LE log: `/mnt/c/Users/Administrator/.codex/worktrees/pa20-media-gateway-transport-provider/controller-runtime/pa20-implementation-full-verify.log`.
- XML: `backend/target/failsafe-reports/TEST-com.onecall.archtests.container.MediaGatewayTransportIT.xml`; existing Surefire/Failsafe XML was inspected, not regenerated.

Controller `container-smoke` full verification finished at `2026-11-08T15:39:10Z`: Surefire **29 classes / 196 tests, zero failures/errors/skips**; Failsafe **13 classes / 204 tests, 1 failure, 1 error, zero skips**. All 198 existing Failsafe tests passed; the six transport cases had four passes, one failure and one error. Overall `BUILD FAILURE`, `PA20_FULL_VERIFY_EXIT=1`. Log evidence identifies `postgres:16`, PostgreSQL **16.15**, image ID `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`; these are Controller observations, not a container run by this agent.

1. `realHttpAndLogsRedactSecretsAndLeaveExistingPublicFlowUnchanged`, original line 234: expected `transport.responseBuffer == 8192`, actual `0`. The field was never assigned by the existing test Valve. This is missing measurement, not evidence that the production buffer is zero.
2. `committedRevocationRotationAndRiskDenyNextByteRequest`, original line 122: risk insertion violated `uq_risk_binding_type`. `MediaPlaybackIT.Fixture.scenario` already creates a CLEARED CONFLICT for the binding. V003 makes `(caller_number_binding_id, risk_type)` unique regardless of state, so inserting another CONFLICT is invalid before authorization is exercised.

## Minimal changes and preserved assertions

- In the existing Valve's `invoke`, assign `transport.responseBuffer = response.getBufferSize()` before delegating `getNext().invoke(request, response)`. This observes the actual Catalina response buffer without setting it or hardcoding the recorded value. Keep the exact `8192` assertion, transferred-byte comparison, truncation/redaction checks and snapshots unchanged.
- In the existing RISK mutation only, insert `RECYCLED` instead of `CONFLICT`. V003 explicitly permits RECYCLED; the reused scenario does not insert it. OPEN with OPERATIONS source, current observed timestamp, default revision and absent cleared timestamp satisfies the existing schema shape and avoids the occupied unique key. Keep the subsequent 403, zero physical-source opens and unchanged snapshot assertions exactly as written. No schema or fixture superclass is edited.
- The implementation report now separates original sandbox history from authoritative Controller results and this bounded follow-up. Earlier Docker/loopback limitations are preserved as historical context, not used to deny the Controller's runtime evidence.

Exactly these repository paths change in fix 1:

1. `backend/src/test/java/com/onecall/archtests/container/MediaGatewayTransportIT.java`
2. `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-implementation-report.md`
3. `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-implementation-fix-1.md` (new)

## Commands and results

Validation used Java `21.0.13+9` at `/opt/jdk21` and Maven `3.9.9` at `/opt/maven-3.9.9`. `JAVA_HOME=/opt/jdk21` and a PATH preferring these binaries were supplied. An initial unqualified `java -version` resolved to an unusable Windows launcher (`UtilBindVsockAnyPort: socket failed`); explicit Linux binaries worked. The existing Windows `.git` pointer was not repaired; read-only Git commands used the existing `/mnt/d/codex-call/.git/worktrees/codex-call22` with explicit `--work-tree`.

To preserve Controller reports, copied only `backend/pom.xml` and `backend/src` into `/tmp/pa20-fullverify-fix1/backend`. Maven commands are offline and use that copy; generated validation evidence lives under `/tmp`, not the authoritative report directories. No Docker command was run.

| Command / inspection | Outcome |
| --- | --- |
| `iconv -f UTF-16LE -t UTF-8 <Controller-log>` with targeted `rg` inspection; Python ElementTree inspection of existing XML | Confirmed exact two failures, full-run exit 1, 196/204 counts and PostgreSQL 16.15 identity. Reads only. |
| `mvn -o -B -ntp -f /tmp/pa20-fullverify-fix1/backend/pom.xml test-compile` | Exit 0, BUILD SUCCESS; 70 test source files compiled with Java release 21, including corrected IT. Log: `/tmp/pa20-fix1-test-compile.log`. |
| From the temporary backend: `mvn -o -B -ntp -Dtest=SingleByteRangeTests,ControlledMediaByteSourceTests,MediaGatewayConfigurationTests,MediaPlaybackServiceTests -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test` | Exit 1: 34 tests, 0 failures, 1 error, 0 skips. `MediaGatewayConfigurationTests.webGatewayPrecedesCustomerBoundaryAndDefaultBearerIsUnavailable` could not bind TCP (`java.net.SocketException: Operation not permitted`). This is not a passing selection. Log: `/tmp/pa20-fix1-focused.log`. |
| Same temporary backend: `mvn -o -B -ntp -Dtest=SingleByteRangeTests,ControlledMediaByteSourceTests,MediaPlaybackServiceTests -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test` | Exit 0: 30 tests, 0 failures/errors/skips. Non-socket subset only, not a replacement for the failed configuration or IT run. Log: `/tmp/pa20-fix1-nonsocket.log`. |
| Same temporary backend: `mvn -o -B -ntp -Dtest=MediaGatewayWebTests,SingleByteRangeTests,ControlledMediaByteSourceTests,MediaPlaybackServiceTests -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test` | Exit 0: **48 tests, 0 failures/errors/skips**, including all 18 direct-filter tests, six range tests, four source tests and 20 PA19 playback service tests. Log: `/tmp/pa20-fix1-nonsocket-with-filter.log`. |
| Read-only Python execution of the unchanged CI inventory script up to its terminal `exact_inventory` calls, then comparison of all XML `(classname, method)` identities against CI sets | Exit 0: source=CI=Controller XML identities **29/196 + 13/204 = 42/400**, line scan **401**, POM includes **13**. The two failing XML statuses remain present; identity equality is explicitly not a green XML/runtime assertion. |
| `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD" diff --check` | Exit 0; existing LF/CRLF checkout warnings only. Read-only filesystem snapshot comparison and focused whitespace checks verify the three-path fix boundary, including untracked allowed files. |

The 34-, 30- and 48-test selections overlap; their counts are not additive. No test source or CI inventory was altered to avoid the environment failure. The author did not execute the corrected six-case PostgreSQL IT or a Docker full suite; Controller execution is recorded below.

## Controller post-fix verification

- The authoritative rerun completed at `2026-11-08T16:11:51Z` with `BUILD SUCCESS` after 13m41s. Fresh XML contains Surefire **29 classes / 196 tests** and Failsafe **13 classes / 204 tests**, with zero failures, errors and skips. PostgreSQL is **16.15** with image ID `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`.
- `MediaGatewayTransportIT` passed **6/6**. The two repaired cases now prove the actual 8192-byte Tomcat response buffer and the valid distinct OPEN `RECYCLED` risk denial with no physical source open and unchanged durable state.
- The Controller executed the workflow's unchanged source=CI=XML inventory check: `29/196 + 13/204 = 42/400`, 13 POM includes, line scan 401, zero failures/errors/skips.
- Additional Controller checks passed: Node `v22.23.4` `npm run check:s3` including both OpenAPI documents; exact CI default/web JAR smoke; 82-probe dependency firewall; shared business QA 30/30 and call-task QA 16/16; frozen V001-V013/public-control diff zero; `git diff --check`.
- Controller full log: `controller-runtime/pa20-implementation-full-verify-fix1.log`, 820,318 bytes / SHA256 `95E8AC77649DD7E07DE11FE076E971493CC7D61C0D2D4917CC29915A403B0081`.

## Residual risks, owners and next validation

| Residual | Owner / required next action |
| --- | --- |
| Corrected Valve measurement and valid risk mutation | Closed: Controller ran all six transport cases and fresh `42/400` full Docker/PostgreSQL verification with zero failures/errors/skips. |
| Sandbox TCP-bind restriction in the author environment | Closed for this gate: the unchanged configuration test passed under the Controller Docker runtime. |
| Post-fix actual image identity and runtime freshness | Closed for this gate: PostgreSQL 16.15 / image ID above was observed in the fresh post-fix run. Hosted CI remains a separate Controller/CI residual and is not claimed here. |
| Production verifier/source, Q22 timing, deployed redaction, origin/TLS and transport operational policy | Existing Security/Sharing/Evidence/Platform/Infrastructure owners and accepted-plan destinations remain unchanged; no production enablement is authorized. |

## Completion status and prohibited actions

**Bounded implementation fix complete; Controller post-fix runtime validation passed.** The implementation gate is ready for independent code review. Production source, PA19 behavior, V001-V013, public-control OpenAPI, accepted plan, control document, CI/POM inventory and all other frozen paths remain unchanged by this fix. All six IT method names and all assertions remain intact.

No staging, commit, push, branch creation, PR action, merge, approval, ready action, reviewer request, external comment or issue mutation occurred. No `.codex` or Controller private file was written, and no authoritative generated report or full verification log was modified. Stop after these three paths; do not enter code review.
