# PA14 Extraction-Code Rotation — S3 Implementation

## Identity And Status

- Task: `pa14-code-rotation-s3-impl-gpt-retry4-20261105`; continuation-validation of the candidate authored by retry 3.
- Work unit: `PA14-sharing-command-code-rotation`; gate: Gateflow `implementation`; one end-to-end slice, S3 only.
- Author: current Codex CLI session. Requested routing/provider label: GPT `gpt-6.1-sol` / high; assignment metadata does not independently attest the runtime model.
- Date: 2026-11-05, Asia/Shanghai.
- Status: **`implementation-complete`**. Retry 4 preserves the existing twenty-path implementation candidate without source changes and closes its runtime evidence gap: full clean verification passes 79 Surefire + 118 Failsafe = 197, zero failures/errors/skips; exact CI source=CI=XML inventory, packaged-JAR smokes and all 82 firewall probes pass. This is author completion only, not implementation-gate acceptance, independent code review, production enablement or remote CI success.
- Merged implementation baseline: `d9cc5bd22f6eda0523f989348f66b09137087fc0`.
- Retry 4 intake HEAD and unchanged closing HEAD: `7eccb333640482a5295ae79d67d1ebdff0743cc3`. Retry 3's recorded HEAD was `b251b6f74650c6f7da3f3f2b1d3f3958734c7c92`; the intervening controller checkpoint records the runtime-retry authorization. No commit is authored here.

| Binding input | Exact SHA256 |
| --- | --- |
| Accepted 317-line rotation plan | `c1d4de2813abad92d9d6077dc1221fcf888dd312831e7b4acaed27f0b0f0cb0c` |
| Accepted plan fix 1 | `d3844b311091a8d7728be0c4913c6ca0938e6eeac2d20aa0d1873ed3d0d32a27` |
| Plan re-review 20261105-190738 | `e001bc32cbd1912893cd4475f76ca4f40fbcd3deb66fa7a4fe72166e52c12fa8` |
| Plan re-review 20261105-191346 | `be3d44641dae00e3c2cb3ef1fd3679a48626e6b8fb563caee81b88f71dde2638` |

The active Control goal and these accepted authorities remain unchanged. The accepted full command-foundation plan, frozen OpenAPI rotation request/result branches, existing repository/provider contracts, V009 lifecycle guards and V010 column grants supply the implementation boundary. No schema, public contract, production provider parameter or enablement value changes.

## Changed Paths And Functions

The following are the exact twenty implementation paths; this report is the twenty-first permitted write. Prefix `M` means `backend/src/main/java/com/onecall/core/`; prefix `T` means `backend/src/test/java/com/onecall/archtests/`.

| Path | Bounded change |
| --- | --- |
| M `customer_iam/internal/web/CustomerBoundaryFilter.java` | Admit only exact rotation POST; reuse strict single exact key and JSON media validation, authentication and negotiation precedence. |
| M `customer_iam/internal/web/CustomerSharesController.java` | `rotateCustomerShareCode`: ID-before-workspace validation, closed binding, trusted caller/trace, lazy qualified service lookup, flat response union. |
| M `customer_iam/internal/web/CustomerShareCommandDtos.java` | Exactly CodeRotationRequest and the flat ROTATED/REPLAYED DTOs; replay has no secret field. |
| M `customer_iam/internal/web/CustomerShareCommandJsonBinder.java` | `rotate(byte[])`: closed object, optional nonnegative integral int witness; duplicates/trailing JSON/null/unknowns rejected. |
| M `sharing/internal/command/CustomerShareCommand.java` | Nested Rotate with exact ID/key/Optional trace/witness components. |
| M `sharing/internal/command/CustomerShareCommandResult.java` | Exact flat Rotated and RotationReplayed records; five result alternatives remain. |
| M `sharing/internal/commandservice/DefaultCustomerShareCommandService.java` | Qualified policy collaborator, Rotate dispatch/validation and ordered replay-first rotation transaction. |
| M `sharing/internal/persistence/JooqSharingCommandRepository.java` | Strict second registered replay combination; rotation-only six-column conditional UPDATE with explicit twenty-column RETURNING; revoke branch preserved. |
| M `sharing/internal/persistence/SharingCommandPersistenceConfiguration.java` | Qualified policy dependency after creation policy; missing policy lambda returns Unavailable(SERVICE_UNAVAILABLE). |
| M `sharing/internal/provider/ShareRotationPolicyProvider.java` | Sole new production file: internal request, ALLOW/DENY enum and evaluate contract; required references non-null. |
| T `sharing/CustomerShareCommandServiceTests.java` | One named rotation method: independent identity/fingerprint, persisted replay, order, policy/material refusal, overflow and structural validation. |
| T `sharing/DeterministicShareCommandProviders.java` | Test-only rotation ALLOW factory and later-generation-distinct code/salt/hash; generation-one outputs preserved. |
| T `container/CustomerShareCommandsIT.java` | Five named PostgreSQL methods and local helpers/probes for exact facts/state, replay, matrix, zero-row rollback, bounded races and append/commit rollback. |
| T `CustomerSharesCommandWebTests.java` | Extend existing four methods; exact unions, strict errors/closedness, key/media/ID validation and invalid-ID/malformed-workspace 422 with zero resolution. |
| T `CustomerSharesWebTests.java` | Extend existing exact-route method; context-prefixed rotation admission and missing/registered command behavior; retain aliases/method rejection. |
| T `SharingCommandPersistenceConfigurationTests.java` | No disabled graph; fail-closed default and exact explicit override without duplicate policy. |
| T `CustomerWebConfigurationTests.java` | Added qualifier in the frozen collaborator position. |
| T `architecture/SharingQueryBoundaryTests.java` | Exact nineteen command files/eight providers, nested request/decision and flat result components; unchanged five command exports. |
| `.github/workflows/ci.yml` | Only six accepted method-name additions and expected counts/message; all validator bodies, smokes and firewall unchanged. |
| `backend/README.md` | Only current totals/admitted behavior and bounded cumulative history; historical 159/64 and nine waves/ten migrations preserved. |

## Data Flow And State Contract

1. Filter: exact route/raw-URI check, trusted authentication, one exact Idempotency-Key, one JSON Content-Type, existing Accept negotiation. Controller validates opaque ID before trusted workspace shape, then reads/binds the body once before resolving commands. Invalid route-shaped ID plus malformed workspace is 422; valid ID plus malformed workspace is 503.
2. One existing writable READ COMMITTED/REQUIRES_NEW transaction: role assertion, personal rank 9 or organization ranks 7→8→9, active caller context, creator/workspace-filtered twenty-column FOR UPDATE, one microsecond-aligned database clock sample.
3. Exact UTF-8 key digest and canonical CODE_ROTATE request identity/fingerprint. Persisted event lookup precedes current state, expiry, witness, policy and material. Exact replay returns the original ACTIVE status and generation counters, never code/token, even after relock/later rotation/revoke/expiry or policy refusal. Fingerprint conflict is 409; corrupt facts are 503, never regeneration.
4. New action: only LOCKED + EXTRACTION_CODE with sampled time strictly before expiry and absent/equal code witness. All other states/modes/equal-or-elapsed expiry/stale witness return 409, not revoke-specific 410/stale codes. No lower-valid-from or admission-count gate is introduced.
5. Evaluate source-snapshot rotation policy once. Denied or Allowed(DENY) is 403; unavailable/null/malformed/throwing policy is 503. No material/facts/writes before this succeeds. Missing production policy remains fail closed; deterministic ALLOW is test-only.
6. Checked old+1 revision, code generation and revocation generation; generate code once for the existing grant/new generation. Validate code transport shape, salt/hash storage shape and independent salt/hash freshness. Invalid/denied/unavailable/throwing material and arithmetic overflow are 503 before append.
7. CODE_ROTATED access event → exactly one conditional six-column UPDATE → full intended-row equality → SHARE_CODE_ROTATED Outbox → SHARE_GRANT_CODE_ROTATE audit. Commit precedes returning the in-memory one-time code as ROTATED. A zero-row result performs exactly one owned reread, then always 409 with full rollback, never retry/replay of an uncommitted event.

Only `status`, `code_salt`, `code_hash`, `code_generation`, `revocation_generation`, `revision` change. Fourteen other grant fields remain identical. No token generation, evidence/creation/mobile policy call, expiry extension, unowned read or additional grant identity is added. Source LOCKED becomes ACTIVE; all three counters increment exactly once.

Access payload has exactly seven registered keys. Outbox has exactly ten, with independent canonical hash and literal double-prefix dedupe `share-transition:share-transition:<64hex>`. Audit snapshots contain only status/revision, request ID is the key digest, reason is SQL null, trace is the trusted UUID or SQL null for direct Optional.empty. Admission/risk/ciphertext nulls and common sampled timestamps are asserted. Raw key, code, token, salt and code hash never enter facts/Problems/responses beyond the first permitted code field; only grant salt/hash storage is retained.

## Exact Test Inventory

No test class or baseline method is removed, renamed, disabled or skipped. Exactly six ordinary named @Test methods are added:

| Suite | New method |
| --- | --- |
| Surefire, existing CustomerShareCommandServiceTests | `rotateReplayNeverReturnsCode` |
| Failsafe, existing CustomerShareCommandsIT | `rotateWritesRegisteredEventAuditAndOutboxAtomically` |
| Failsafe, existing CustomerShareCommandsIT | `rotateReplayReturnsNoCodeOrCapabilityToken` |
| Failsafe, existing CustomerShareCommandsIT | `rotateRequiresExactLockedStateAndGenerationWitness` |
| Failsafe, existing CustomerShareCommandsIT | `rotateConcurrentActionsProduceOneTransition` |
| Failsafe, existing CustomerShareCommandsIT | `rotateFailureAtEachFactAppendRollsBackAllEffects` |

Arithmetic: baseline 78 + 1 = **79 Surefire** in 16 classes; 113 + 5 = **118 Failsafe** in six classes; **79 + 118 = 197**. Service unit class has six methods; command IT has eighteen (8 create/foundation + 5 revoke + 5 rotate). Failsafe breakdown is **64+10+10+8+8+18=118**. Existing web/config/boundary methods gain assertions without increasing their inventories.

The PostgreSQL proof covers all four modes × four statuses × before/equal/after expiry × absent/exact/stale witness, refused policy before material/writes, independent salt/hash freshness, exact row/fact/hash/null/trace binding, and replay with later durable heads. Malformed replay facts are appended in a rollback-isolated command transaction, decoded by the real repository, then rolled back without guard disabling or mutation of existing facts. Conditional-miss probes alter actual JDBC revision/status/code-witness parameters, execute the real UPDATE, and prove one owned reread, no retry, event rollback and unchanged durable snapshots. Race rendezvous precedes serialized locks; two connections use finite futures/barrier/lock/statement timeouts and finally cleanup. Identical keys yield one ROTATED/one REPLAYED, distinct keys one ROTATED/one 409, with one secret/transition/fact set. Nine before/after event/update/Outbox/audit/commit fault seams verify full twenty-field rollback against fresh admin reads, same-key retry, and secretless replay; commit failure verifies explicit rollback.

## Retry 3 Historical Runtime Evidence

This section retains the previous attempt's commands and failures as historical evidence, not the current disposition. Retry 4's fresh passing runs and controller-authorized host override are recorded separately below; they supersede the prior runtime blocker without changing the implementation or test contract.

Every Docker/Maven/Testcontainers invocation explicitly exports `DOCKER_HOST=tcp://172.20.211.232:2375`. No WSL/daemon/repository configuration is modified, and Ryuk is neither disabled nor bypassed. The bridge remains controller-owned.

Canonical invocation environment, from repository root:

```sh
export DOCKER_HOST=tcp://172.20.211.232:2375
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
MVN=/opt/maven-3.9.9/bin/mvn
AGENT='-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar'
FOCUSED=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests,SharingCommandPersistenceConfigurationTests,CustomerWebConfigurationTests,SharingQueryBoundaryTests
```

Observed tools: Temurin OpenJDK **21.0.13+9 LTS**, Maven **3.9.9**, Linux x86_64 WSL2; Docker client 29.1.3/server 29.9.2, API 1.56, four CPUs/7.756 GiB; Testcontainers **2.0.5**, Ryuk **0.14.0**, test-only image `postgres:16`. Initial inherited PATH selected Windows Java 21.0.12.1 and an unusable Windows Maven launcher (`ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`); the existing Linux /opt toolchain resolved this without configuration edits.

Actual SQL executed in the new successful-facts IT method:

```sql
SELECT version(), current_setting('server_version');
```

Actual result: **PostgreSQL 16.15 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit; server_version=16.15**. Major 16 satisfies the hard requirement; no patch version is inferred from the image tag.

### Validation Ledger

The commands below use the exported environment above; logs are local ephemeral files, not new repository artifacts. Early failures are retained rather than represented as passing acceptance.

| Command / log under `/tmp/pa14-s3-retry3-` | Actual outcome |
| --- | --- |
| `docker info`; `java -version`; `mvn -version` | Docker bridge healthy; inherited Windows Maven failed; `/opt` Java/Maven pins verified as above. |
| `mvn -f backend/pom.xml -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify` — `prerequisite.log` | Exit 0 before implementation: 78 Surefire + 13 command IT, all zero failures/errors/skips; Ryuk/PostgreSQL started through TCP bridge. Baseline only, not rotation acceptance. |
| `mvn -B -ntp -f backend/pom.xml -Dtest=$FOCUSED test` — `focused.log` | Exit 1: 28 tests, two old route-rejection assertions failed after endpoint admission; corrected within existing methods. |
| Same focused command with `"$AGENT"` — `focused2.log` | Exit 1: 29 tests, one duplicate-Content-Type fixture assertion failed. Mock servlet special-cases Content-Type; the fixture now explicitly supplies duplicate enumeration to the real filter. |
| `mvn -B -ntp -f backend/pom.xml "$AGENT" -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify` — `targeted.log` | Exit 1 at testCompile: heterogeneous malformed mock outcomes inferred Object; corrected test stubbing. No IT acceptance claimed. |
| Same targeted command — `targeted2.log` | Exit 1: 79 Surefire, one duplicate-header fixture failure; IT not reached. |
| `mvn -B -ntp -f backend/pom.xml "$AGENT" -Dtest=CustomerSharesCommandWebTests test` — `webfix.log` | Exit 1: four methods, one MockMvc request-builder error for literal blank ID; fixture now sets route-shaped URI/servlet path after building. Required 422 assertion remains, not weakened. |
| `mvn -B -ntp -f backend/pom.xml "$AGENT" -Dtest=CustomerSharesCommandWebTests,CustomerShareCommandServiceTests -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify` — `targeted3.log` | Exit 0: 10 focused unit/web + 18 command IT; zero failures/errors/skips; actual version above. Subsequent test-only strengthening adds JDBC/timeouts/rollback-isolated corrupt-fact proof, requiring the final full run. |
| `mvn -B -ntp -f backend/pom.xml "$AGENT" -Pcontainer-smoke clean verify` — `full.log` | Exit 1: 79 Surefire passed; six Failsafe class-initialization errors, no behavioral IT execution. Ryuk started but host connection to 172.20.211.232:32788 was refused. Docker API health remained good; no configuration or assertion changes. |
| Identical full clean command, one fresh bounded rerun — `full2.log` | Exit 1: 79 Surefire passed; 111 Failsafe cases, zero assertion failures, one error, zero skips. Five complete IT classes pass, including all 18 final command methods. CustomerSharesIT fails before its eight methods on PostgreSQL mapped-port connection refusal. Not 197-test acceptance. |
| Accepted plan §10 extraction wrapper, three existing CI bodies — `ci-bodies.log` | Wrapper exit 1. Exact inventory exits 1 with `wrong aggregate test inventory`, correctly rejecting incomplete XML. Default/opt-in web JAR smokes exit 0. Initial firewall extraction exits 1 because the plan's last-step split includes the following YAML job, leaving the Python heredoc indented; this is an invocation/extraction error, not a firewall result. |
| Re-extract only the existing final firewall YAML run block by its ten-space indentation, retaining body bytes and adapting only Maven executable — `firewall.log` | Exit 0. Production-JAR firewall passes all **82 adversarial probes**; baseline37/34, web55/48, counterparts47, dependency-only8, packager1; deltas18/0 and14/0. No workflow/firewall assertion changes. |
| Python XML parse of final `backend/target/{surefire,failsafe}-reports/TEST-*.xml` | 16/79 Surefire, zero failures/errors/skips; six Failsafe report classes/111 cases, zero failures/one error/zero skips. Exact method arithmetic remains unfulfilled; reports are not combined with older runs to fabricate 118/197. |
| Process-local read-only Git checks, SHA256 baseline comparison, CI reverse-delta comparison and allowed-path audit | All task changes remain within the exact 21 paths; no staged entries or protected tracked-byte changes. Closing whitespace and final path result are recorded below. |

First full-run environment failure: Ryuk container `14681e73fe9e924b245f84bb9682e260a59961c5d6f44024ae76c5df706f3e7a` started at 20:02:23 +08:00, but Java received repeated `java.net.ConnectException: Connection refused` at 172.20.211.232:32788 and the run ended 20:02:54 with six class-init errors. A read-only `docker info --format` still reported server 29.9.2/four CPUs/8328425472 bytes, and `docker ps -a` showed no remaining containers. One unchanged fresh full rerun was justified by the earlier passing targeted run and healthy API; no host override, socket change, retry loop, disabled Ryuk or skipped test was introduced.

Second full-run environment failure: fresh Ryuk `42453dbf4c6ae67b1026a648b2c55a07b25c95c06e4d6ed92fec91c53dddd971` is reachable on mapped port 32789. The final command suite passes 18/18 and records actual PostgreSQL 16.15. CustomerSharesIT's PostgreSQL container `22643bd952e350005543d32267f8a30677375dbc3d5a2df5b59f34270f4c32da` reports started at 20:07:08 +08:00, but Flyway cannot connect to `jdbc:postgresql://172.20.211.232:32792/customer_me`: SQLSTATE **08001**, caused by `java.net.ConnectException: Connection refused`. Failure path is CustomerSharesIT.start:28 → CustomerSharesFixture.start:53 → CustomerMeFixture.start:87, before its eight test methods. The run ends 20:08:03, exit 1. Docker API health again passes and no containers remain after normal cleanup. Underlying mapped-port refusal cause is **not established**; daemon health alone is not claimed as container reachability. No further full-run retry, port selection, host override, test modification or daemon/WSL configuration workaround is used to mask this blocker.

Retry 3 final Failsafe XML breakdown: CustomerMeIT 10/0/0/0; CustomerShareCommandsIT 18/0/0/0; CustomerSharesIT 1/0/1/0 (class-initialization placeholder, not one of its eight methods); PostgresContainerIT 64/0/0/0; SharingQueryServiceIT 10/0/0/0; SharingRoleLeastPrivilegeIT 8/0/0/0, where each tuple is tests/failures/errors/skips. Thus that historical run had 79 unit + 110 successful container methods = 189 successful named methods, plus one initialization error; **not** 197 passing cases.

### CI Bodies, Scope And Closing Evidence

The existing CI source=CI=XML body was executed unchanged except the accepted expected names/counts/message. It correctly fails on the incomplete Failsafe aggregate. The existing default/opt-in web JAR smoke body passes: default non-web/database-free startup, missing verifier sanitized responses, security/cache headers, no pool, and JVM cleanup. The unchanged production-JAR firewall body passes all 82 probes after correcting only local YAML-block extraction. No reduced validator substitutes for the failing 197-case inventory.

Local extraction command used from repository root (same body, corrected last-step boundary):

```sh
export DOCKER_HOST=tcp://172.20.211.232:2375
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
python3 - <<'PY'
from pathlib import Path
import os, subprocess
workflow = Path('.github/workflows/ci.yml').read_text()
block = workflow.split('      - name: production jar dependency firewall\n', 1)[1].split('        run: |\n', 1)[1]
lines = []
for line in block.splitlines():
    if line.strip() and not line.startswith('          '):
        break
    lines.append(line[10:] if line.startswith('          ') else '')
script = '\n'.join(lines) + '\n'
assert script.rstrip().endswith('PYCODE')
script = script.replace('./mvnw', '/opt/maven-3.9.9/bin/mvn')
subprocess.run(['bash', '-e', '-c', script], cwd='backend', env=os.environ.copy(), check=True)
PY
```

Retry 3's scope audit recorded twenty implementation paths plus this report, an empty index, the accepted six-method delta and exact reconstruction of baseline CI after reversing only accepted names/counts/message. Retry 4 independently repeats those checks below. Git object/worktree byte comparisons must account for this checkout's pre-existing CRLF conversion; raw intake hashes, rather than raw Git blob equality, establish byte preservation during this continuation.

## Retry 4 Continuation Validation

The candidate diff and existing report were audited against the binding goal, accepted plan/fix/reviews, full-plan authority, transport/schema/provider/repository contracts and test/CI inventory. No necessary source fix was identified. The only write by this continuation is this report; all twenty implementation files remain as received. The sole new production provider remains SHA256 `e7188679def45e0ae076e03d201fba57b660a63d5c80240fe4f4be518c6dda95`.

Every Docker/Maven/Testcontainers invocation uses the following process-local environment, with no daemon, WSL or repository configuration changes and no Ryuk disablement/bypass:

```sh
export DOCKER_HOST=tcp://172.20.211.232:2375
export TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
MVN=/opt/maven-3.9.9/bin/mvn
AGENT='-DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar'
FOCUSED=CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests,SharingCommandPersistenceConfigurationTests,CustomerWebConfigurationTests,SharingQueryBoundaryTests
java -version
"$MVN" -v
docker info --format '{{.ServerVersion}} CPUs={{.NCPU}} Memory={{.MemTotal}}'
"$MVN" -B -ntp -f backend/pom.xml "$AGENT" -Dtest="$FOCUSED" test
"$MVN" -B -ntp -f backend/pom.xml "$AGENT" -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify
"$MVN" -B -ntp -f backend/pom.xml "$AGENT" -Pcontainer-smoke clean verify
```

Observed tools: Temurin OpenJDK **21.0.13+9 LTS**, Maven **3.9.9**, Docker server **29.9.2**, four CPUs, **8,328,425,472 bytes** memory. The actual SQL `SELECT version(), current_setting('server_version')` executes in `rotateWritesRegisteredEventAuditAndOutboxAtomically` in both targeted and full runs. Both print **PostgreSQL 16.15 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit; server_version=16.15**. The `postgres:16` fixture is unchanged. Ryuk 0.14.0 starts normally and monitors cleanup. Full-run JDBC URLs use the required reachable gateway `172.20.208.1`, including CustomerSharesIT, whose eight methods now pass without fixture edits.

| Retry 4 validation command / local log | Actual result, 2026-11-05 +08:00 |
| --- | --- |
| Tool/version and Docker API commands above | Exit 0; exact toolchain and healthy bridge recorded above. |
| Focused Surefire/web command — `/tmp/pa14-retry4-focused.log` | Exit 0, 29 tests, failures/errors/skips 0; finished 20:21:33, elapsed 49.638 s. |
| Targeted command IT verify — `/tmp/pa14-retry4-targeted.log` | Exit 0, 79 Surefire plus all 18 CustomerShareCommandsIT methods, failures/errors/skips 0; finished 20:23:42, elapsed 1:55. Targeted results alone are not aggregate acceptance. |
| Full `-Pcontainer-smoke clean verify` — `/tmp/pa14-retry4-full.log` | Exit 0, 79 Surefire in 16 classes and 118 Failsafe in six classes; zero failures/errors/skips; finished 20:28:57, elapsed 4:09. No reduced includes, skips or assertion changes. |
| Existing CI `assert container smoke actually ran` body — `/tmp/pa14-retry4-inventory.log` | Exit 0: exact source=CI=XML equality, no duplicate/missing/extra methods/classes, 79 + (64+10+10+8+8+18) = **197**, zero failures/errors/skips. |
| Existing CI `default and opt-in web jar smokes` body — `/tmp/pa14-retry4-jar-smokes.log` | Exit 0: default non-web/database-free JAR, opt-in web fail-closed missing verifier, sanitized Problems/security/cache headers, no datasource pool and JVM cleanup pass. |
| Existing CI `production jar dependency firewall` body — `/tmp/pa14-retry4-firewall.log` | Exit 0: **82 adversarial probes**; frozen resolved/packaged dependency inventories, approved collisions/packager branch and absence of packaged deterministic test adapters pass. Dependency-list Maven finishes 20:29:45. |

Fresh full-run Failsafe XML: PostgresContainerIT **64**, CustomerMeIT **10**, SharingQueryServiceIT **10**, CustomerSharesIT **8**, SharingRoleLeastPrivilegeIT **8**, CustomerShareCommandsIT **18**. Every suite has failures=errors=skipped=0. The existing source/CI/XML body independently checks all sixteen Surefire classes / 79 exact method names and all six Failsafe classes / 118 exact method names, not merely aggregate counts. Reports remain in ignored `backend/target/surefire-reports` and `backend/target/failsafe-reports`; logs under `/tmp` are local ephemeral evidence, not committed artifacts or remote CI.

The actual extraction/execution command for the three existing CI bodies, from repository root with the environment above, is:

```sh
python3 - <<'PY'
from pathlib import Path
import os, subprocess, hashlib
workflow = Path('.github/workflows/ci.yml').read_text()
steps = ('assert container smoke actually ran', 'default and opt-in web jar smokes', 'production jar dependency firewall')
for index, name in enumerate(steps):
    block = workflow.split('      - name: ' + name + '\n', 1)[1].split('        run: |\n', 1)[1]
    lines = []
    for line in block.splitlines():
        if line.strip() and not line.startswith('          '):
            break
        lines.append(line[10:] if line.startswith('          ') else '')
    script = '\n'.join(lines) + '\n'
    assert script.rstrip().endswith('PY' if index == 0 else 'PYCODE')
    print('RUN', name, 'CI-body SHA256', hashlib.sha256(script.encode()).hexdigest(), flush=True)
    script = script.replace('./mvnw', '/opt/maven-3.9.9/bin/mvn')
    log = '/tmp/pa14-retry4-' + ('inventory', 'jar-smokes', 'firewall')[index] + '.log'
    with open(log, 'w') as stream:
        result = subprocess.run(['bash', '-e', '-c', script], cwd='backend', env=os.environ.copy(), stdout=stream, stderr=subprocess.STDOUT)
    print('EXIT', result.returncode, 'LOG', log, flush=True)
    print('\n'.join(Path(log).read_text().splitlines()[-12:]), flush=True)
    if result.returncode:
        raise SystemExit(result.returncode)
PY
```

Extraction stops at the YAML scalar boundary, including the last Java step before the next job; it does not alter any body or weaken a validator. The only local executable adaptation is `./mvnw` to installed Maven 3.9.9. Extracted CI-body SHA256 before that substitution: inventory `6ea12e5f56c95882f9c160b8c8abd3c752fbd8123b66807f82c165620469e491`; smokes `1ba848682b26ead5cfc300d11e08a7269784e52c6c67e53029a2a9164c023ab4`; firewall `05f23f1538f94c1b1c01c5a688de3d973659752d4ddab6c039a8df4dd13a3bc7`.

### Evidence And Scope Closeout

- The fresh targeted and full runs execute all five named rotation IT methods. They prove exact six changed / fourteen unchanged grant fields and old+1 counters, independent salt/hash freshness, exact registered facts and canonical hashes/nulls/trace, one physical command connection/role/clock and required lock/context/ownership order. Replay stays secretless after later states/time/witness/policy changes; refusal stops before material/writes. The state matrix, real conditional-miss predicates/one bounded reread and nine append/update/commit fault seams prove rollback with fresh durable reads. Bounded personal/organization two-connection races prove one transition, one secret and one event/Outbox/audit set. Existing create/revoke/list/me, module/ACL/schema and query regressions all pass.
- Process-local read-only Git environment: `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call14`, `GIT_WORK_TREE="$PWD"`, `GIT_OPTIONAL_LOCKS=0`. No `.git` rewrite. Plan-table-derived allowlist check of `git diff --name-only` plus `git ls-files --others --exclude-standard -- . ':!.codex' ':!.codex/**'` passes: **19 tracked implementation changes + one new provider + this report = exactly 21 allowed paths**; no deletions/renames or other nonprivate untracked task file. The private `.codex` entry is excluded without inspection/copy/modification.
- SHA256 manifest of all **1,344 tracked files** at retry 4 intake matches closing raw bytes. The new provider also retains the hash above. Thus this continuation preserves the entire twenty-path implementation candidate and every protected file byte-for-byte; only the pre-existing untracked implementation report is updated. Control, accepted plan/fix/reviews, V001–V010, OpenAPI, POM, application YAML/properties, design docs, frontend/public and excluded interfaces/providers/adapters remain untouched.
- A first direct raw HEAD-blob/worktree comparison rejected pre-existing CRLF differences; it was an audit-method error, not a source/runtime failure. The corrected content comparison normalizes only in memory, finds exactly nineteen allowed tracked content differences from HEAD and makes no filesystem changes. Raw intake hashes separately prove byte preservation. Reversing exactly the six accepted CI method names and totals/message reconstructs HEAD CI exactly. Source test comparison finds exactly six added named `@Test` methods and no removed/renamed baseline method; no test class or skip is added.
- `git -c core.safecrlf=false diff --check` and explicit `git -c core.safecrlf=false diff --no-index --check /dev/null <path>` for the new provider and this report pass, including the post-report check. `git diff --cached --name-only` is empty; HEAD remains `7eccb333640482a5295ae79d67d1ebdff0743cc3`. Final changed-path audit again requires exact equality with the twenty plan paths plus this artifact.

## Residual Risks And Boundaries

| Risk / uncovered area | Owner and destination |
| --- | --- |
| Production rotation policy, approved code KDF/material custody and provisioning intentionally unspecified; missing adapters fail closed. | Security/Product/Sharing Backend → approved provider/security design and later provisioning gate; no production ALLOW or key/KDF/TTL choice here. |
| Retry 3's published-port blocker is closed by the controller-authorized process-local host override; the full clean 197-case run, inventory, smokes and 82 probes now pass. Local reruns still depend on the temporary controller-owned bridge and reachable gateway. | Controller/Platform → own bridge lifecycle and runner reachability; Backend/Database → retain ordinary Docker-capable CI execution. No repository/daemon/WSL configuration or Ryuk bypass is introduced. |
| V009 structurally allows some service-forbidden transitions. | Sharing Backend/Database → exact state/mode/witness tests and immutable schema; no DDL strengthening in this slice. |
| Lost first response cannot recover/reissue plaintext code; request abuse/rate limiting is outside this endpoint slice. | Sharing/Product → frozen secretless replay contract; Security/Platform → existing gateway/rate-limit destination. |
| Local runtime evidence is not remote CI, independent review or production/security/load/cloud acceptance. | Controller → next authorized independent code-review gate only after implementation/evidence intake; production owners retain later acceptance responsibilities. |

No public admission/media/frontend, generic transition framework, replay table, secret-recovery endpoint or later-slice work is added. The private untracked `.codex` entry is excluded from task paths and is not inspected, copied or modified. No staging, commit, branch creation, push, PR creation/update, merge, approval, ready action, reviewer request, independent code review, external comment or issue mutation occurs. Stop remains before independent code review.

**Disposition: `implementation-complete` (author completion only).** The existing bounded candidate is preserved and all required fresh full/inventory/smoke/firewall and scope checks pass. No required runtime evidence gap remains for this slice. Return to Controller intake and stop before independent code review; no gate approval or external action is performed or implied.
