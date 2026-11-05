# PA14 Slice 1 — Create/Foundation Implementation And Validation

## Provenance and boundary

- Implementation task: `pa14-sharing-command-service-foundation-s1-impl-gpt-20261104-184500`.
- Validation/fix task: `pa14-sharing-command-service-foundation-s1-fix1-gpt-20261104-192500`, November 4, 2026, Asia/Shanghai.
- Provider: OpenAI GPT; Control routes these tasks to `gpt-6.1-sol` / `high`. This records task routing, not independent model-identity attestation.
- Accepted plan: `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`, SHA-256 `53e98e96d24c7179a45760481e6f54d1a3765bc0373124183f85f792d75d814b`; accepted-plan checkpoint `88890f56715312f250583be69f31e44e0525340b`.
- Scope is create/foundation only. Revoke/rotate commands, providers, persistence transitions, success responses and routes are not implemented. Their paths still return sanitized 404. Shared Problem enum mappings do not enable those operations.
- Read the accepted plan, PA14 Control, predecessor prompt/final execution evidence, and current changed/new source, tests, POM, CI and README. The predecessor's socket/Docker denials were environmental, not code-failure evidence.

## Create behavior

- Only exact `POST /api/v1/customer/shares` joins the existing exact GET `/shares` and `/me` allowlist, retaining context-path/raw-URI checks. Authentication supplies the trusted account/workspace; no client owner, workspace or trace field is accepted. Create ignores unrelated `Idempotency-Key` headers.
- Require valid `application/json` Content-Type, then strict local JSON binding: duplicate/unknown fields, missing required fields, wrong/null types, invalid UUID/mode/limits/mobile, malformed or non-offset timestamps and more than six fractional digits return 422 before service/provider/DB access. Six-digit timestamps normalize to canonical UTC `Z` text. Created responses are flat 201 with one-time capability token and code only for extraction mode; replay is flat metadata-only 200. Problems remain closed, sanitized and non-cacheable.
- The command pool exists only with `persistence` plus explicit `onecall.sharing-command.enabled=true` and complete separate credentials. No production enablement or credential values are added. Default, web-only and disabled persistence graphs have no command pool. Query configuration and its read-only transaction are unchanged.
- One writable READ COMMITTED transaction uses command-role assertion, personal rank-9 or organization ranks 7→8→9 advisory keys, active caller/evidence reference, mandatory same-connection evidence projection under `evidence_role`, restoration/assertion of `sharing_command_role`, and one database clock sample.
- Canonical content/verification/creation are JSONB object nodes. Identity hashes exact UTF-8 creator/request-key properties; scope, creation fingerprint and content digest exclude generated secrets/observed time. Designated-mobile binding occurs before identity lookup without a grant UUID and persists only HMAC/key version. Ownership/risk and creation policy precede replay.
- Only an identity miss allocates grant UUID/token/code. Validate provider material against V009, precheck token-hash uniqueness, insert with `ON CONFLICT (share_id) DO NOTHING RETURNING`, and reselect once on zero rows for exact replay versus changed-fingerprint conflict. Token-hash races roll back as sanitized INTERNAL rather than continuing an aborted transaction.
- Durable order is grant → `SHARE_CREATED` Outbox → `SHARE_GRANT_CREATE` audit, with exact redacted payload digests, explicit optional trusted trace and no access event. Append/commit failures roll back. Production policy/token/code/mobile defaults fail closed; deterministic adapters are test-only.

## Defect fixed in this validation

Real PostgreSQL execution exposed a genuine plain-SQL jOOQ binding defect: the two `OffsetDateTime` INSERT arguments were bound as character varying, and PostgreSQL rejected `valid_from` before any create could complete. Seven of eight create IT methods initially errored with sanitized INTERNAL. A single-method diagnostic confirmed `column "valid_from" is of type timestamp with time zone but expression is of type character varying`.

Add explicit `?::timestamptz` casts for both `valid_from` and `expires_at` in `JooqSharingCommandRepository.insertGrantReturning`. Preserve bound parameters, the exact conflict target/returning semantics and the schema. Extend the existing `createAppendFailureRollsBackGrantAuditAndOutbox` SQL-shape assertion to require both casts; no new test method is added. Existing six-digit timestamp and canonical JSONB/typed read-back assertions exercise the runtime regression. Temporary diagnostic exception instrumentation was removed.

## Changed-file inventory

Paths inherited from the slice implementation, inspected here (repository-relative):

```text
.github/workflows/ci.yml
backend/README.md
backend/pom.xml
backend/src/main/resources/application-persistence.yml
backend/src/main/java/com/onecall/core/customer_iam/package-info.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSharesController.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandDtos.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandJsonBinder.java
backend/src/main/java/com/onecall/core/evidence/shareprojection/package-info.java
backend/src/main/java/com/onecall/core/evidence/shareprojection/ShareableEvidenceProjection.java
backend/src/main/java/com/onecall/core/evidence/shareprojection/ShareableEvidenceProjectionReader.java
backend/src/main/java/com/onecall/core/evidence/internal/shareprojection/JooqShareableEvidenceProjectionReader.java
backend/src/main/java/com/onecall/core/evidence/internal/shareprojection/ShareProjectionReaderConfiguration.java
backend/src/main/java/com/onecall/core/sharing/package-info.java
backend/src/main/java/com/onecall/core/sharing/internal/command/package-info.java
backend/src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommand.java
backend/src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommandResult.java
backend/src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommandService.java
backend/src/main/java/com/onecall/core/sharing/internal/command/ShareCommandCaller.java
backend/src/main/java/com/onecall/core/sharing/internal/command/ShareCommandException.java
backend/src/main/java/com/onecall/core/sharing/internal/commandservice/CustomerShareCommandServiceImpl.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandRepository.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqSharingCommandRepository.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceConfiguration.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceProperties.java
backend/src/main/java/com/onecall/core/sharing/internal/provider/ProviderResult.java
backend/src/main/java/com/onecall/core/sharing/internal/provider/ShareCreationPolicyProvider.java
backend/src/main/java/com/onecall/core/sharing/internal/provider/CapabilityTokenProvider.java
backend/src/main/java/com/onecall/core/sharing/internal/provider/ShareExtractionCodeProvider.java
backend/src/main/java/com/onecall/core/sharing/internal/provider/ExtractionCodeMaterial.java
backend/src/main/java/com/onecall/core/sharing/internal/provider/DesignatedMobileBindingProvider.java
backend/src/main/java/com/onecall/core/sharing/internal/provider/Sha256Provider.java
backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java
backend/src/test/java/com/onecall/archtests/CustomerSharesCommandWebTests.java
backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java
backend/src/test/java/com/onecall/archtests/SharingCommandPersistenceConfigurationTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java
backend/src/test/java/com/onecall/archtests/sharing/CustomerShareCommandServiceTests.java
backend/src/test/java/com/onecall/archtests/sharing/DeterministicShareCommandProviders.java
backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java
```

This fix changes only the repository INSERT, the existing IT assertion, and this report. The worktree also arrived with two Controller-owned additions to `docs/gateflow/project-execution-control.md`; they are not authored, reverted or included as implementation changes here. The pre-existing untracked empty `.codex` file is private and untouched/excluded. No generated target files, wrapper copies or diagnostic logs are implementation artifacts to commit.

## Environment and reproducible commands

Validation runs locally in full-access WSL, not in the predecessor's workspace-write sandbox. Docker client/server `29.1.3` respond through the Unix socket; Testcontainers `2.0.5` starts actual `postgres:16`, reported by Flyway as PostgreSQL `16.15`. Python bind/listen on `127.0.0.1` succeeds; Java Tomcat/HttpClient fixtures and packaged web startup also succeed. No environmental socket/Docker/PostgreSQL blocker remains.

The inherited PATH resolves `java` to Windows `/mnt/d/tools/jdk21/bin/java.exe`; the checked-in `backend/mvnw` has CRLF, so direct execution initially fails with `/bin/sh^M: bad interpreter`. Do not change that unowned wrapper. Use its exact contents with line-ending normalization outside the worktree and pin the native Linux JDK:

```sh
# From repository root; all copies/results stay outside tracked source.
mkdir -p /tmp/pa14-s1-fix1/wrapper
sed 's/\r$//' backend/mvnw > /tmp/pa14-s1-fix1/wrapper/mvnw
ln -s "$PWD/backend/.mvn" /tmp/pa14-s1-fix1/wrapper/.mvn
cd backend
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:$PATH
sh /tmp/pa14-s1-fix1/wrapper/mvnw -version
```

This is the actual project Maven wrapper and its unchanged `.mvn/wrapper/maven-wrapper.properties`, selecting/checksumming Maven `3.9.9`, not a substitute system Maven. Native Java is Eclipse Adoptium `21.0.13`; enforcer/toolchain requirements pass. Git's Windows worktree pointer is resolved read-only with `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call12 --work-tree=<repository-root>`; no Git metadata is repaired or edited.

Exact Maven invocations, from `backend/` with the native Java environment above:

| Command after `sh /tmp/pa14-s1-fix1/wrapper/mvnw` | Result | External log |
| --- | --- | --- |
| `-B -Dtest='*BoundaryTests,CustomerWebConfigurationTests,SharingCommandPersistenceConfigurationTests,CustomerShareCommandServiceTests,CustomerSharesCommandWebTests,CustomerSharesWebTests' test` | PASS: 46, zero failures/errors/skips | `/tmp/pa14-s1-fix1/focused.log` |
| `-B -Pcontainer-smoke -Dit.test=CustomerShareCommandsIT verify` before fix | Surefire 77 green; create IT 8 run, 7 errors from timestamp binding, 0 skipped | `/tmp/pa14-s1-fix1/create-it.log` |
| `-B -Pcontainer-smoke -Dtest=CustomerShareCommandServiceTests '-Dit.test=CustomerShareCommandsIT#commandRoleIsAssertedBeforeFirstCommandRead' verify` with temporary test-only diagnostic | Confirmed PostgreSQL type error; 4 unit tests pass, diagnostic IT fails | `/tmp/pa14-s1-fix1/diagnostic-it.log` |
| `-B -Pcontainer-smoke -Dtest=CustomerShareCommandServiceTests -Dit.test=CustomerShareCommandsIT verify` after fix | PASS: 4 unit + all 8 create IT, zero failures/errors/skips | `/tmp/pa14-s1-fix1/create-it-fixed.log` |
| `-B -ntp -Pcontainer-smoke verify` after final regression assertion | PASS: 77 Surefire + 108 Failsafe, zero failures/errors/skips; 2:31 elapsed | `/tmp/pa14-s1-fix1/full-container.log` |
| `-B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt` | PASS | `/tmp/pa14-s1-fix1/runtime-dependencies.log` |

The existing CI Python heredocs are extracted verbatim, with only YAML indentation removed, to `/tmp/pa14-s1-fix1/{inventory,jar-smokes,jar-firewall}.py`. Execute from `backend/`:

```sh
python3 /tmp/pa14-s1-fix1/inventory.py
python3 /tmp/pa14-s1-fix1/jar-smokes.py
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
python3 /tmp/pa14-s1-fix1/jar-firewall.py
```

- Default/web packaged-JAR smoke: PASS, no command/database pool, no verifier adapter, expected sanitized HTTP responses, JVMs cleaned up; `/tmp/pa14-s1-fix1/jar-smokes.log`. Default exits successfully without a web server; opt-in web binds loopback without Hikari startup.
- Production-JAR firewall: PASS, approved dependencies and 82 adversarial probes; `/tmp/pa14-s1-fix1/jar-firewall.log`. An initial invocation lacked native `jar` on PATH and produced an empty index; regenerating the index with the explicit JDK environment resolves that harness-only failure without source changes.
- Full XML/source inventory: PASS, 16 Surefire classes/77 methods and 6 Failsafe classes/108 methods, 185 combined, zero failures/errors/skips; `/tmp/pa14-s1-fix1/inventory.log`. Final whitespace/scope checks also pass.

## Implemented test-method inventory

Slice 1 adds exactly 10 Surefire methods across three classes, not the final plan's 12. Existing 67 methods remain, including the renamed `CustomerSharesWebTests.exactGetAndPostRoutesPassHighestPriorityFilter`. Current Surefire total is **16 classes / 77 methods**.

| Class | Exact added method names |
| --- | --- |
| `CustomerShareCommandServiceTests` (4) | `createBuildsDeterministicIdentityAndFingerprint`; `createChecksRiskAndPolicyBeforeReplay`; `createReplaysMetadataWithoutSecrets`; `createRejectsChangedFingerprint` |
| `CustomerSharesCommandWebTests` (4) | `createUsesTrustedCallerAndMapsCreatedAndReplay`; `revokeAndRotateMapClosedResponseUnions`; `invalidCommandAndMissingServiceMapToProblemResponses`; `commandRoutesDoNotChangeShareListRoute` |
| `SharingCommandPersistenceConfigurationTests` (2) | `defaultAndWebProfilesCreateNoCommandDatasource`; `commandProfileUsesDistinctPoolAndResetsLocalRole` |

The retained name `revokeAndRotateMapClosedResponseUnions` currently asserts shared Problem rows and sanitized 404/no command invocation for both deferred routes; it does **not** test transition success. Keeping the accepted final method name avoids alternate inventory identities while preserving slice scope.

`CustomerShareCommandsIT` implements exactly the first eight accepted methods:

1. `commandRoleIsAssertedBeforeFirstCommandRead`
2. `queryRoleCannotAssumeCommandRole`
3. `createPersistsGrantAuditAndOutboxAtomicallyWithoutAccessEvent`
4. `createReplayReturnsMetadataAndDoesNotPersistOrReturnSecrets`
5. `createRiskAndPolicyChecksPrecedeReplay`
6. `createConcurrentSameAndDifferentIdentities`
7. `evidenceProjectionUsesCommandTransactionConnection`
8. `createAppendFailureRollsBackGrantAuditAndOutbox`

Failsafe is **6 classes / 108 methods**: `PostgresContainerIT` 64 + `SharingQueryServiceIT` 10 + `CustomerMeIT` 10 + `CustomerSharesIT` 8 + `SharingRoleLeastPrivilegeIT` 8 + `CustomerShareCommandsIT` 8. Combined expected implemented inventory is **185**. POM explicitly includes the command IT; CI requires exact source/class/method/XML agreement and zero failures/errors/skips. README current totals reflect 77/108/185, preserving historical 159/64 and prior checkpoints. The final-plan 79/118/197 totals are intentionally not claimed in this slice.

## Protected surfaces and remaining work

- No migration, schema, OpenAPI, docs/12/14/15 contract or accepted-plan edits. V001–V010 stay frozen. No command SQL is added to the query service/repository/configuration. GET `/shares`, GET `/me`, query role and read-only transaction remain unchanged; existing unit/web and database integration regressions supply compatibility evidence.
- No revoke/rotate success behavior, transition repository methods, access events or rotation policy provider are added. Later slices must add the remaining two service methods and ten IT methods and expand existing web method bodies; their replay, generation/state/witness, policy/material, concurrency and rollback contracts remain uncovered here.
- Production policy, approved code/KDF parameters, mobile normalization/HMAC/key configuration and external verification remain unapproved and fail closed. Test adapters are synthetic, not production parameters. Gateway rate limiting, admission/tickets, media authorization, Redis/COS/KMS/SMS, frontend and provisioning remain outside this work unit.
- Evidence is local validation, not a GitHub Actions run, slice acceptance, external review or production acceptance. No staging, commit, branch creation, push, PR, reviewer request, comment, approval, gate advancement or next-slice work occurs.

## Final validation status

Slice-1 implementation and local validation are complete after the targeted timestamp-binding fix. Full `-Pcontainer-smoke verify` passes: **77 Surefire + 108 Failsafe = 185**, with zero failures, errors or skips. The eight create IT methods execute against real Testcontainers PostgreSQL 16; all 77 Surefire methods execute with working Java loopback. Exact source/CI/XML class/method/count parsing passes, including the final SQL-shape regression assertion.

The three existing CI Python checks pass locally: inventory, packaged default/web smoke and production-JAR firewall. No GitHub Actions outcome is inferred. `git diff --check` passes; frozen contracts, migrations, accepted-plan hash, original wrapper, GET controller/query implementation and read-only query configuration are unchanged. The index is empty and `.codex` remains untracked, empty and excluded. Scope/whitespace evidence is retained in `/tmp/pa14-s1-fix1/{scope-check,diff-check}.log`; XML results remain under ignored `backend/target/{surefire-reports,failsafe-reports}`. Full-suite intentional negative migration probes log errors while their assertions pass; final suite totals, not those expected log lines, determine success.

No full-access environment blocker remains. Stop at this slice-1 validation handoff; acceptance and later gates remain Controller-owned.
