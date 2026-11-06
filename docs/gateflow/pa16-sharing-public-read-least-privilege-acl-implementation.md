# PA16 Public-Read Least-Privilege ACL — Implementation S1 Retry 2

## 1. Status And Authority

- Status: **implementation-complete** (author-side implementation and required local validation complete; not gate acceptance, code-review approval or production acceptance).
- Work unit: `PA16-sharing-public-read-least-privilege-acl`; gate: `implementation S1`.
- Task: `pa16-public-read-acl-implementation-gpt-retry2-20261106`; date: 2026-11-06, Asia/Shanghai.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa16-sharing-public-read-least-privilege-acl/codex-call`.
- Branch: `codex/pa16-sharing-public-read-least-privilege-acl`; starting and final HEAD: `a4f7ac93963acd4d9e7dfcc288fe7de8c29f81ac`.
- Accepted plan checkpoint remains `685bd50`. The 765-line accepted plan SHA-256 remains `400f7e69ecd55480c0d05dc7dfda29db1bf4c0ea011b8e680f1f352b00d2be21`.
- Binding inputs: accepted plan, fix 1 and current PA16 Control section. These were read and remain unchanged. This record supersedes the earlier blocked-attempt report; earlier attempts are not substituted for this retry's evidence.
- Sole implementation author; no agents/delegation, independent code review or subsequent gate was started.

## 2. Delivered Slice And Scope

Exactly 16 implementation paths: 15 modified existing paths and the new V011 migration. The completion report is separate reporting authority, not another implementation path. The exact final path/hash inventory is below. No protected source, migration, POM, OpenAPI, frontend, policy value, Control, plan/fix or review artifact was modified.

- V011 creates the direct-login `sharing_public_read_role` with the exact safe attributes, no membership, ownership, settings or usable password. Compatibility checks deliberately do not inspect an existing authentication hash or require an existing password to be null. Only fresh-creation tests query the permitted password-null boolean.
- Migration preconditions reject incompatible role attributes/dependencies, extra ACLs, grant options, wrong ownership/columns and PUBLIC application privileges with SQLSTATE `P0001`. They inspect non-system application objects, not only V010's reset subset. Mutation is limited to role creation, current-database PUBLIC TEMPORARY removal, two schema USAGE grants and 24 column SELECT grants issued under the respective object owners. No PUBLIC CONNECT or unrelated ACL is changed.
- Postconditions check role containment, exact grant counts/grantors/options and effective schema/table/column/routine/sequence privileges. No application table-level SELECT, write, row-lock, DDL, function/sequence or escalation capability is granted. No application definition or data is changed.
- `PublicSharePersistenceConfiguration` contains the public nested properties record and exactly six named bean factories: dedicated datasource, manager, template, DSL, repository and service. Credentials are explicit and redacted; the username must be the fixed public role. The manager enforces READ ONLY; the template remains READ_COMMITTED/REQUIRES_NEW/readOnly. There is no command-credential fallback.
- Public GET uses only the public DSL/manager and checks both `current_user` and `session_user` before repository access and around the evidence reader. It retains `SET TRANSACTION READ ONLY` and never switches roles. Repository SQL/result projection and PA15 API/lifecycle behavior remain unchanged.
- The evidence-owned nested adapter has the exact URL-based method condition, no `@Primary`, and a public-manager MANDATORY override. The original command reader and its SQL/annotation remain unchanged. Command-only reader contexts contain exactly one named reader; public contexts contain exactly both approved names and public callers select the public name.
- Tests observe public and command SQL through distinct listeners/pools. A positive command INSERT observation is required before every-INSERT identity/readOnly assertions, and command execution cannot alter the public observation stream. History assertions, CI inventories and README counts are updated without broadening architecture inventories.

## 3. Environment And Commands

Private, uncommitted retained evidence: `/tmp/pa16-retry2-evidence` (directory mode 700). Logs, XML, extracted unchanged CI Python programs, source/method/hash manifests and packaged-JAR smoke output are retained there; none is added to Git.

Observed actual versions (commands exited 0):

| Component | Actual observation |
| --- | --- |
| Java | Temurin OpenJDK `21.0.13`, build `21.0.13+9-LTS`, `/opt/jdk21` |
| Maven | Apache Maven `3.9.9`, `/opt/maven-3.9.9` |
| Docker | Client `29.1.3`; Docker Desktop server `29.9.2` |
| PostgreSQL server | `SHOW server_version` returned `16.15` in the running disposable `postgres:16` environment-check container; integration tests also identify PostgreSQL 16.15 and assert `160000 <= server_version_num < 170000` |
| PostgreSQL client | `psql (PostgreSQL) 16.15`, executed inside that running container |

The Controller-authorized equivalent installed Maven 3.9.9 distribution is used rather than the CRLF checkout wrapper. The wrapper was neither edited nor invoked. All Docker/container/build/test commands use the required TCP bridge and Testcontainers gateway; no socket permissions, bridge configuration, Git pointer or Git configuration was repaired. The environment-check container was stopped/removed; test containers are disposable and closed by their fixtures.

Build/test environment:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
export DOCKER_HOST=tcp://172.20.211.232:2375
export TESTCONTAINERS_HOST_OVERRIDE=172.20.208.1
```

Actual commands and final outcomes:

| Command / working directory | Exit | Evidence and result |
| --- | --- | --- |
| `/opt/jdk21/bin/java -version`; `/opt/maven-3.9.9/bin/mvn -version`; `docker info`; `docker version --format 'client={{.Client.Version}} server={{.Server.Version}}'` | 0 | `versions.log`, `docker-info.log`; versions above |
| `docker exec pa16-environment-check psql -U postgres -Atc 'SHOW server_version'`; `docker exec pa16-environment-check psql --version` | 0 | `versions.log`; actual server/client 16.15 |
| From `backend`: `/opt/maven-3.9.9/bin/mvn -B -ntp -Dtest=PublicShareWebTests,PublicShareQueryServiceTests,PublicSharePersistenceConfigurationTests,CustomerApiBoundaryTests,SharingQueryBoundaryTests,ModuleBoundaryTests test` | 0 | `focused-final.log`: **24 tests, 0 failures/errors/skips**; selected classes 6/4/2/3/1/8 respectively |
| From workspace root: `/opt/maven-3.9.9/bin/mvn -B -ntp -f backend/pom.xml -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT,PublicSharesIT verify` | 0 | `targeted-final.log`, `targeted-final-xml`: **12+8=20 Failsafe tests, 0 failures/errors/skips**; Surefire also runs 91 |
| From workspace root: `/opt/maven-3.9.9/bin/mvn -B -ntp -f backend/pom.xml clean -Pcontainer-smoke verify` | 0 | `clean-verify-final.log`, `clean-final-surefire-xml`, `clean-final-failsafe-xml`: **19 classes/91 Surefire + 7 classes/130 Failsafe = 221**, 0 failures/errors/skips; fresh clean run, 5:14, finished 2026-11-06 11:53:06 +08:00 |
| From `backend`: `python3 /tmp/pa16-retry2-evidence/inventory.py` | 0 | `inventory-final.log`: exact **source=CI=XML**, all 26 classes and 221 method identities, no duplicate/disabled/conditional/parameterized/generated substitution |
| From `backend`: `python3 /tmp/pa16-retry2-evidence/jar-smokes.py` | 0 | `jar-smokes-final.log`, `boot.log`, `web-boot.log`: unchanged default/non-web and opt-in web packaged-JAR smokes, all original HTTP probes, no datasource/verifier adapter and JVM cleanup |
| From `backend`: `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar`; `/opt/maven-3.9.9/bin/mvn -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt`; `python3 /tmp/pa16-retry2-evidence/firewall.py` | 0 | `dependency-list-final.log`, `pa11-runtime-dependencies.txt`, `jar-index.txt`, `firewall-final.log`: unchanged firewall, **82/82 adversarial probes**; baseline37/34, web55/48, counterparts47, dependency-only8, packager1, deltas18/0 and14/0 |
| Raw-byte/canonical manifest checks, exact scope/method-delta checks and explicit-directory `git diff --check` | 0 | Private manifests/logs; V001–V010 10/10 unchanged raw and 10/10 canonical; only four added methods/two approved renames; no whitespace error or extra implementation path |

The inventory checker is extracted from the updated CI block without semantic changes beyond approved names/counts. The packaged-JAR and firewall Python blocks are exactly identical to starting HEAD after normalizing checkout line endings; `scope-and-ci-identity.log` records that comparison. They were rerun against the final clean build's packaged artifact. These are local executions of CI checks, not a claim that hosted GitHub Actions ran.

### Implementation-Time Failed Runs (Retained, Not Counted As Pass)

- `focused-initial.log`: exit 1, 24 executed with 3 failures and 2 errors while the old tests still expected the removed role-switch wiring. `focused.log`: exit 1 at test compilation with two stale references. These were implementation/test-update failures, not environment skips; corrected focused runs passed 24/24.
- `clean-verify.log`: exit 1, Surefire 91 green and Failsafe 130 executed with exactly one failure, zero errors/skips. `PostgresContainerIT.v001ThroughV009MigrateCleanPostgres16WithoutSkipping` still expected the history cursor to end after V010. A broad textual replacement had also put the V011 script string into AssertJ's formatting varargs rather than adding the V011 row. This was corrected to assert each exact V010 and V011 version/description/type/script/success row before end-of-results, as required by plan §7. No assertion was weakened. The failed XML is retained in `first-clean-surefire-xml` / `first-clean-failsafe-xml`; the subsequent fresh clean run passed all 221.
- Private ad-hoc static inventory extraction initially omitted referenced CI literal variables/updates and raised `NameError`; these exploratory script attempts were not acceptance evidence. The complete unchanged CI checker subsequently ran successfully against fresh final source/XML.
- Expected negative migration probes intentionally log migration refusals/rollbacks, including retained legacy V010 cases. Those expected exceptions are asserted tests, not ignored failures or skipped tests.

## 4. Database And Runtime Evidence

The four new methods and existing public/configuration methods execute these assertions against real PostgreSQL 16.15 or the specified isolated Spring contexts. Final XML and `database-evidence.log` retain execution evidence; no credential, authentication hash or sensitive fixture value is included in this report.

| Partition | Executed result |
| --- | --- |
| Upgrade / repeat / replay | Isolated cluster migrated explicitly to V010 (10 applied), then latest executed exactly one migration and current V011/11 applied. Validation and second migrate executed zero. Transactional replay is ACL-idempotent both before and after disposable authentication assignment; a compatible partial schema/column grant set is completed without widening the approved allowlist. |
| Privilege-only / preservation | Structural catalog snapshots cover namespaces, relations, attributes, defaults, constraints, indexes, triggers and routines; application data is compared separately. Existing role ACLs and unrelated database ACLs are preserved. An explicit unrelated TEMP grant survives; only the PUBLIC TEMP tuple disappears and PUBLIC CONNECT remains. |
| Atomic refusal / rollback | Independent unsafe role attributes, both membership directions, schema/table ownership, role and database-role settings, extra table/column/schema/routine/sequence/default/database privileges, cross-database ACL dependency, grant option and unsafe PUBLIC state return `P0001`. Each probe snapshots and restores role/dependency/settings/ACL/PUBLIC/history/structure state. In-memory V011 copies with appended `SELECT 1/0` return `22012` after grants and restore both absent-role V010 state and deployed-role/PUBLIC-TEMP state. No persistent helper function or helper role is added. |
| Role / exact ACL catalog | LOGIN true; superuser/createdb/createrole/inherit/replication/bypassrls false; connection limit -1; validity null; settings/membership/ownership absent. Exactly two schema USAGE and 24 column SELECT tuples, owner grantors, no grant option. Table ACLs, routine/sequence/default privileges, direct database grants, effective database CREATE/TEMP and effective foreign privileges are absent; inherited PUBLIC CONNECT is preserved. Complete effective application column SELECT/INSERT/UPDATE/REFERENCES and relation privilege checks are performed. |
| Projection reads / forbidden complements | Direct public login succeeds on all **24 individual permitted columns**, complete permitted projections, the existing repository and joined evidence SQL, and database time. Forbidden complements are checked against pg_attribute: 44 columns on readable tables plus 26 on entirely denied S3 tables, **70 individual denied reads**, all `42501`; eight SELECT-star and audit/Outbox/idempotency reads are denied. Public HTTP returns the same captured facts, artifact ordering/duration and redacted view. |
| READ WRITE ACL denials | In separately controlled READ WRITE transactions, DML/TRUNCATE across all 11 protected tables, 20 row-lock variants, schema/table/TEMP/sequence/function/view/index/role/database DDL, MERGE, COPY FROM STDIN, routine/sequence calls and role/escalation attempts return exactly `42501`. CREATE DATABASE uses a fresh autocommit connection, not a transaction. COPY is rejected before input mode. RESET ROLE / SET ROLE NONE retain both public identities. No tuple lock or durable data/definition change occurs; the disposable sequence is dropped in finally. |
| READ ONLY precedence | A fresh direct public READ ONLY transaction has both narrow identities and `on`; the same update denied `42501` in READ WRITE now returns **25006**. Existing command-role authorized-write READ ONLY denial remains 25006. Public-listener write injection independently observes 25006 on the actual service transaction. |
| Properties / named reader / manager | Properties-only binding and direct constructors reject missing/blank/wrong-username cases with the exact deepest sanitized IllegalArgumentException and no cause; valid values remain unchanged and toString is `[REDACTED]`. Six exact factories/qualifiers/settings are checked. Command-only reader set is exactly one; URL-present set is exactly both names, distinct proxies/managers, with exact URL condition and no Primary. Missing public transaction throws IllegalTransactionStateException; absent public manager throws NoSuchBeanDefinitionException naming it, before any DSL access, never command-manager fallback. |
| Real public proxy / pool identity | Real HTTP/evidence projection succeeds under the public MANDATORY proxy and the same read-only backend connection. Outside a transaction and inside only an ambient command transaction, the public proxy fails before DSL interaction. Every observed public statement has current=session=`sharing_public_read_role`, readOnly=on, no role transitions; the digest is prepared with one binding and two evidence SELECTs are observed. Final clean projection-case PID was 221. |
| Reset / failure / concurrency / no writes | Public pool max size 1 (test only): success and 404/410/503/required-column failure return a connection with narrow current/session identity, JDBC readOnly=false, autocommit=true, transaction_read_only=off and reused PID. The read-only write-injection/reset case recorded PID 109 in the final clean run (separate fixture from PID 221). Required column ACL is restored in finally; removing command evidence membership cannot break public GET. Six bounded concurrent HTTP reads preserve all 11 table snapshots. Both owned pools close on context shutdown. |
| Positive command / regressions | After clearing only command observations, command create produces a nonempty INSERT stream; every INSERT has readOnly=off, command current identity and command session login. Public observations remain exactly unchanged and command/public pools are distinct. All 18 protected create/revoke/rotate tests, ten query tests, eight list tests, ten me tests, eight public tests and original API/unit boundaries pass unchanged except the approved identity/wiring/history updates. |

The finite SQL matrix is representative executable denial evidence; complete role/membership/ownership and explicit/effective catalog checks are authoritative for other PostgreSQL syntax/capabilities. Ordinary PostgreSQL catalog/built-in visibility is not claimed absent.

## 5. Frozen Bytes And Final Implementation Identities

V001–V010 raw SHA-256 below were captured before editing and matched again after validation. Canonical LF values also match all ten accepted-plan constants (including V010). The new V011 raw SHA-256 is `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` (204 LF lines).

| Frozen migration | Unchanged raw SHA-256 |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| `V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| `V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| `V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| `V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| `V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |

Exact 16 implementation paths and final raw SHA-256 (report excluded):

| Path | SHA-256 |
| --- | --- |
| `.github/workflows/ci.yml` | `635ee5a689fbb6007fce2e1ec31fc289a0b80e6dbc4014c41255560d8ecea976` |
| `backend/README.md` | `2aa0f9fa48c701420e36af4f9197004169dd6512be01fb3e2a000bf8188148b4` |
| `backend/src/main/java/com/onecall/core/evidence/internal/shareprojection/ShareProjectionReaderConfiguration.java` | `aee5f2f66e8029ae677342f1b8660426b6b3f276dcd52308e456ab3d8f22a70f` |
| `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqPublicShareQueryRepository.java` | `fd385d11e4bf208bc4e0b2fb88b3731fface7a8bdc423778ce55d24fc2095ba1` |
| `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicSharePersistenceConfiguration.java` | `cfb9e3b272e35a5909f882eda9de67bbd7a3d46da495680435797d86e1b921f2` |
| `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java` | `8ba35a1021801d95d0e4c3092366c02a8d0eb9b37aa1dc15b9113494c8031233` |
| `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `backend/src/test/java/com/onecall/archtests/PublicSharePersistenceConfigurationTests.java` | `4d62c56f74df71e2a0c1c006ef36402fc29d2fd2950c771ffd6f542d347a504d` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java` | `ebf50cf9c418c75d256ef1cec44a2d1b3a0f26972c24fcaa3a56cfcb4a8c70fd` |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `b342b8edc091f8f1a08b96026e22c465bc598abd4480d9bc49d3c31537fafd35` |
| `backend/src/test/java/com/onecall/archtests/container/PublicSharesFixture.java` | `8629e1a4583d496b7bd433fdb33f1b229ad7707cc4f862676019ae58116f5d92` |
| `backend/src/test/java/com/onecall/archtests/container/PublicSharesIT.java` | `9f6a31d047407fb4457d554d33893ddf05370ebdb467c8d39f32a9356f107187` |
| `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java` | `8feb3b35dcf4970772e434c0d17bace261a19710e454b198fb2ae436a29dc7e9` |
| `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java` | `856ed65845e9a1c9815c4d5c2d266addc14aa57d9978c4e1fb94eee7a74aad28` |
| `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java` | `021dd46cd805e390118fc1c074bebbf729044ec657a468fdd585dd575cee0eac` |
| `backend/src/test/java/com/onecall/archtests/sharing/PublicShareQueryServiceTests.java` | `b9cc5dd42116f5c7cfe10de62d05370f4e63abe8972f4839c188618d6861a639` |

Protected authority hashes were checked before/after and are unchanged. Exact source/CI/XML method inventory is listed in Appendix A, rather than replacing it with aggregate arithmetic. Source method delta versus starting HEAD is exactly four new Failsafe methods and the following two renames:

- `PublicSharePersistenceConfigurationTests.publicReadUsesExistingQualifiedManagerAndEnforcedReadOnlyTemplate` → `publicReadUsesIsolatedQualifiedManagerAndEnforcedReadOnlyTemplate`.
- `PublicSharesIT.publicProjectionUsesEvidenceOwnerOnSameReadOnlyConnection` → `publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection`.

## 6. Residual Owners And Stop Boundary

- No required local validation remains skipped, pending or blocked. The initial full-run failure is disclosed above and superseded by a fresh zero-failure clean run, not hidden or waived.
- Database/SRE must assess external consumers of the necessary current-database PUBLIC TEMP removal. Unrelated deployments and cross-database ACL/authentication drift are outside this disposable-cluster evidence; incompatibility is escalated, never fixed by broadening this role or restoring PUBLIC TEMP automatically.
- Database/Security/SRE owns production role-name collision handling, pg_hba/TLS/authentication provisioning, credential distribution/rotation and drift monitoring. The migration intentionally creates no usable password. No production credentials, policies or provisioning are delivered.
- Backend/SRE owns subsequent pool-capacity/abuse/load review; no admission, media authorization, lease/cache or production load/penetration/restore proof is claimed. Hosted CI and independent acceptance remain separate authorities; no A5/production acceptance claim is made.
- Pre-existing `.codex` and `.pa16-plan/` state is untouched. No Git stage/commit/fetch/push/branch/configuration/pointer change, PR/issue/comment/reviewer/approval/ready action or external gate mutation occurred. Git inspection used command-local actual `GIT_DIR`/`GIT_WORK_TREE` with optional locks/lazy fetch disabled.
- Stop here at **implementation-complete**. No code-review gate or other work unit is started.

## Appendix A. Exact Final Source=CI=XML Inventory

Every listed identity has exactly one final XML testcase; all suites have zero failures/errors/skips. No duplicate, conditional, parameterized, disabled, repeated or generated-test substitution is present. Totals: **19 Surefire / 91 methods; 7 Failsafe / 130 methods; 26 classes / 221 methods**.

### com.onecall.archtests.CustomerMeWebTests (Surefire, 8)

- `applicationNegotiationAndErrorDispatchUseFrozenProblems`
- `authenticatedGetReturnsFrozenCustomerContext`
- `clientSelectorsHeadersCookiesAndBodyCannotOverrideContext`
- `concurrentRequestsKeepAccountAndChannelIsolated`
- `missingMalformedAndDuplicateBearersReturnFrozen401`
- `otherPathsMethodsAndAliasesAreNotCustomerOperations`
- `reachableProblemsAndResponseHeadersAreClosedAndSanitized`
- `reservedProblemVocabularyIsWriterOnly`

### com.onecall.archtests.CustomerSharesCommandWebTests (Surefire, 4)

- `commandRoutesDoNotChangeShareListRoute`
- `createUsesTrustedCallerAndMapsCreatedAndReplay`
- `invalidCommandAndMissingServiceMapToProblemResponses`
- `revokeAndRotateMapClosedResponseUnions`

### com.onecall.archtests.CustomerSharesWebTests (Surefire, 10)

- `authenticatedPersonalAndOrganizationGetsUseTrustedCreatorScope`
- `clientSelectorsAndRequestAttributesCannotOverrideTrustedScope`
- `concurrentRequestsKeepAccountAndSelectedWorkspaceIsolated`
- `exactGetAndPostRoutesPassHighestPriorityFilter`
- `limitAndCursorValidationHaveDeterministicPrecedence`
- `meRemainsCompatibleWithLegacyAndSelectedSessions`
- `missingInvalidAndDuplicateBearersFailBeforePageParsing`
- `negotiationAndBufferedFailuresPreservePa11Boundary`
- `queryErrorsUseExactClosedProblems`
- `webWithoutSharingPersistenceFailsClosedAndDefaultStaysDatabaseFree`

### com.onecall.archtests.CustomerWebConfigurationTests (Surefire, 6)

- `customerAndSharingGraphsUseExactQualifiers`
- `customerCredentialsAreExplicitAndValidatedBeforeConnection`
- `defaultRuntimeHasNoCustomerOrServletBeans`
- `testProfileAndProductClasspathCannotEnableVerifierBypass`
- `webProfileIsExplicitAndMissingAdapterHasNoDatabaseEffects`
- `webSettingsDisableUnsafeLoggingAndSessionBehavior`

### com.onecall.archtests.OneCallApplicationTests (Surefire, 1)

- `contextLoadsWithoutDatabaseOrSecurityAutoConfiguration`

### com.onecall.archtests.PublicSharePersistenceConfigurationTests (Surefire, 2)

- `defaultWebAndDisabledCommandProfilesStayDatabaseFree`
- `publicReadUsesIsolatedQualifiedManagerAndEnforcedReadOnlyTemplate`

### com.onecall.archtests.PublicShareWebTests (Surefire, 6)

- `exactPublicGetRoutePrecedesOptionalAuthentication`
- `memberSessionMatchAndNonmatchNeverLeakContent`
- `optionalBearerFailuresKeepFrozenPrecedence`
- `prefetchAndNegotiationHaveNoAuthorizationSideEffects`
- `publicDtoIsClosedAndContentIsOmittedForNonmembers`
- `tokenShapeAndLookupFailuresUseClosedProblems`

### com.onecall.archtests.SharingCommandPersistenceConfigurationTests (Surefire, 2)

- `commandProfileUsesDistinctPoolAndResetsLocalRole`
- `defaultAndWebProfilesCreateNoCommandDatasource`

### com.onecall.archtests.SharingPersistenceConfigurationTests (Surefire, 2)

- `defaultProfileCreatesNoPersistenceBeans`
- `persistenceRequiresExplicitQueryCredentials`

### com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests (Surefire, 1)

- `approvedPersistenceFrameworksAndForbiddenStacksAreExact`

### com.onecall.archtests.architecture.CustomerApiBoundaryTests (Surefire, 3)

- `customerApiSignaturesAndProjectionAreExact`
- `customerDependenciesAndReadOnlySqlStayInsideBoundary`
- `profileAndVerifierArtifactsCannotEscapeIntoProduction`

### com.onecall.archtests.architecture.ModuleBoundaryTests (Surefire, 8)

- `assertsExactAllowedDependencyMapFromPlan62`
- `discoveredProductionEdgesStayInsideTheConfiguredMap`
- `discoversExactlyTheFrozenModuleSet`
- `everyExpectedModuleDeclaresApplicationModule`
- `rejectsCycleFixture`
- `rejectsForbiddenDependencyFixture`
- `rejectsInternalPackageAccessFixture`
- `verifiesNoCyclesOrBoundaryViolations`

### com.onecall.archtests.architecture.SharingQueryBoundaryTests (Surefire, 1)

- `sharingQueryApiAndProjectionAreExact`

### com.onecall.archtests.sharing.CustomerShareCommandServiceTests (Surefire, 6)

- `createBuildsDeterministicIdentityAndFingerprint`
- `createChecksRiskAndPolicyBeforeReplay`
- `createRejectsChangedFingerprint`
- `createReplaysMetadataWithoutSecrets`
- `revokeReplaysBeforeStatePreconditions`
- `rotateReplayNeverReturnsCode`

### com.onecall.archtests.sharing.PublicShareQueryServiceTests (Surefire, 4)

- `exactUtf8DigestLookupConcealsUnknownTokens`
- `immutableEvidenceProjectionIsOrderedAndFailClosed`
- `organizationContentRequiresCurrentMatchingMember`
- `stateAndDatabaseTimeChecksNeverMutateGrant`

### com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests (Surefire, 6)

- `activeContextHasExactPersonalAndOrganizationProjection`
- `invalidProjectionNeverReturnsPartialContext`
- `missingAndDisabledAccountsAreUnauthenticated`
- `roleAssertionPrecedesRepositoryAndTransactionIsReadOnly`
- `technicalAndProgrammingFailuresAreSanitized`
- `trustedCallerIsTheOnlyAccountAndChannelSource`

### com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests (Surefire, 7)

- `attestedWorkspaceIsCarriedWithoutClientAuthority`
- `invalidRevokedExpiredDisabledAndWrongAudienceSessionsHaveSameDenialShape`
- `malformedVerifierResultsFailClosed`
- `missingAmbiguousAndUnavailableVerifiersFailClosed`
- `secretWrappersAndPrincipalNeverRetainOrExposeBearer`
- `verifierReceivesOnlyOpaqueBearerAndCustomerAudience`
- `workspaceStructureIsCarriedWithoutChangingAuthentication`

### com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests (Surefire, 6)

- `anchorBoundsMatchPa10Validation`
- `cursorBindingRejectsOtherAccountsAndWorkspaces`
- `malformedAndNonCanonicalCursorsAreRejected`
- `maximumUnicodeShareIdsFitFrozenWireBound`
- `personalAndOrganizationCursorsRoundTripCanonicalAnchors`
- `wirePageHasOnlyFrozenFieldsAndNullableCursor`

### com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests (Surefire, 8)

- `creatorContextDenialDoesNotFetchShares`
- `cursorBindingAndAnchorValidationAreDeterministic`
- `defaultsAndBoundsAreDeterministic`
- `emptyAndTailPagesHaveNoCursor`
- `lookaheadUsesLastReturnedRow`
- `projectionComponentsMatchFrozenListing`
- `technicalFailuresReturnNoPartialPageOrSensitiveText`
- `trustedContextCannotBeOverridden`

### com.onecall.archtests.container.CustomerMeIT (Failsafe, 10)

- `committedLifecycleChangesAreObservedOnNextRequest`
- `exactGrantsPermitProjectionAndRejectForeignColumnsAndLocks`
- `onlyActiveMembershipsUnderActiveOrganizationsAreProjected`
- `personalContextRequiresCurrentActiveAccount`
- `requestsCannotSelectAnotherAccountOrganizationRoleOrChannel`
- `successAndFailuresDoNotWriteAndRestorePoolState`
- `verifierOutageAndDatabaseFailureExposeNoSecrets`
- `webAndPa10PersistenceCoexistWithoutBeanOrRoleLeakage`
- `webRuntimeReadsMigratedDatabaseUnderCustomerRole`
- `workspaceOrderingIsUuidDeterministicAndPersonalFirst`

### com.onecall.archtests.container.CustomerShareCommandsIT (Failsafe, 18)

- `commandRoleIsAssertedBeforeFirstCommandRead`
- `createAppendFailureRollsBackGrantAuditAndOutbox`
- `createConcurrentSameAndDifferentIdentities`
- `createPersistsGrantAuditAndOutboxAtomicallyWithoutAccessEvent`
- `createReplayReturnsMetadataAndDoesNotPersistOrReturnSecrets`
- `createRiskAndPolicyChecksPrecedeReplay`
- `evidenceProjectionUsesCommandTransactionConnection`
- `queryRoleCannotAssumeCommandRole`
- `revokeChangedReasonTerminalExpiryAndStaleWitnessConflict`
- `revokeConcurrentActionsProduceOneTransition`
- `revokeExactReplayAddsNoFactsOrRevision`
- `revokeFailureAtEachFactAppendRollsBackAllEffects`
- `revokeWritesRegisteredEventAuditAndOutboxAtomically`
- `rotateConcurrentActionsProduceOneTransition`
- `rotateFailureAtEachFactAppendRollsBackAllEffects`
- `rotateReplayReturnsNoCodeOrCapabilityToken`
- `rotateRequiresExactLockedStateAndGenerationWitness`
- `rotateWritesRegisteredEventAuditAndOutboxAtomically`

### com.onecall.archtests.container.CustomerSharesIT (Failsafe, 8)

- `authenticatedHttpListsPersonalAndOrganizationCreatorMetadata`
- `committedAccountMembershipAndOrganizationChangesDenyNextPage`
- `customerAndSharingConnectionsRemainSeparatelyOwned`
- `existingAclsAndReadOnlyTransactionsForbidWritesAndForeignReads`
- `httpCursorsCannotCrossTrustedAccountOrWorkspace`
- `httpPaginationPreservesTiesAndConcurrentInsertOrder`
- `missingRolesVerifierAndDatabaseFailuresStaySanitized`
- `successfulAndFailedHttpPagesNeverWriteOrProjectSecrets`

### com.onecall.archtests.container.PostgresContainerIT (Failsafe, 64)

- `appendOnlyClaimAndForeignKeyBackstops`
- `auditIsAppendOnlyAndRuntimeWritesAreDenied`
- `auditReasonAndDenylistRulesAreEnforced`
- `bindingLifecycleTerminalityAndActiveNumberUniqueness`
- `brokenMigrationFailsVisibly`
- `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow`
- `concurrentActiveBindingUniquenessHasOneWinner`
- `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner`
- `concurrentIdempotencyRetryObservesOneWinner`
- `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner`
- `concurrentV005TaskClaimAndActiveCallHasOneWinner`
- `concurrentV006FirstTrialAndReservationIdentityHasOneWinner`
- `concurrentV007ActivationAndTransitionIdentityHasOneWinner`
- `concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner`
- `concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner`
- `functionsHaveFixedSearchPathAndNoTransactionControl`
- `hostileSearchPathCannotRedirectFunctionEffects`
- `idempotencyNewReplayConflictAndFinalizeOnce`
- `identityCollisionAndMembershipRevokeRegrantSemantics`
- `lastEnabledAdminTransactionIsSerializedAndFailsClosed`
- `localImmutabilityGuardsRejectRelationAndRotationChanges`
- `mutableHeadsEnforceLifecycleRevisionAndNamedChecks`
- `outboxReplayConflictRollbackAndDenylist`
- `ownerPublicAndRoleExecutionBoundariesAreExact`
- `riskOpenClearReopenRevisionProtocol`
- `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied`
- `subjectOwnerSetOnceAndRealNameSeparation`
- `v001ThroughV003InventoryMatchesTheFrozenContract`
- `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`
- `v003OwnerPublicAndRuntimeAclsAreExact`
- `v003SubjectNumberCatalogAndGuardsAreExact`
- `v004CatalogOrderInventoryAndGuardsAreExact`
- `v004CatalogVersionLifecycleAndGuardBackstops`
- `v004ForeignKeyAndStagedConsentFkBackstops`
- `v004OrderPaymentLifecycleAndAppendOnlyBackstops`
- `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact`
- `v005ForeignKeyAndDeferredReservationFkBackstops`
- `v005IvrTaskSessionCallInventoryAndGuardShapesAreExact`
- `v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops`
- `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact`
- `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops`
- `v005TaskTtlLifecycleAndGuardBackstops`
- `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact`
- `v006IssueFirstTrialGrantReplayConflictAndPolicySemantics`
- `v006NestedOutboxAtomicityAndReplayRollbackEvidence`
- `v006OwnerPublicRuntimeAndCrossModuleAclsAreExact`
- `v006ReservationUsageForeignKeyAndLateFkBackstops`
- `v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops`
- `v007ActivationConsentInventoryAndGuardShapesAreExact`
- `v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops`
- `v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops`
- `v007ForeignKeyAndLateOrderConsentFkBackstops`
- `v007OwnerPublicRuntimeAndCrossModuleAclsAreExact`
- `v008CreationCycleBothOrdersAndTerminalLifecycleBackstops`
- `v008EvidenceInventoryAndGuardShapesAreExact`
- `v008ForeignKeyAndDeferredCreationCycleFkBackstops`
- `v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops`
- `v008OwnerPublicRuntimeAndCrossModuleAclsAreExact`
- `v009AdmissionCountOnceLimitAndAtomicRollbackBackstops`
- `v009ForeignKeyAndRestrictDeleteBackstops`
- `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops`
- `v009OwnerPublicRuntimeAndCrossModuleAclsAreExact`
- `v009ShapeDefaultsAndAppendOnlyBackstops`
- `v009SharingInventoryAndGuardShapesAreExact`

### com.onecall.archtests.container.PublicSharesIT (Failsafe, 8)

- `organizationPageRechecksCurrentAccountTenantAndMembership`
- `publicGetKeepsRevokedExpiredAndLockedPageSemantics`
- `publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse`
- `publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent`
- `publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection`
- `publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState`
- `publicReadPreservesCreateRevokeRotateListAndMeBehavior`
- `repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged`

### com.onecall.archtests.container.SharingQueryServiceIT (Failsafe, 10)

- `committedLifecycleChangesDenyNextQueryAndFailuresRollBack`
- `inactiveAndMissingCreatorContextsFailClosed`
- `invalidAndCrossContextCursorsNeverWidenScope`
- `keysetTiesAndConcurrentInsertRemainStable`
- `listingProjectionNeverReadsOrReturnsSecrets`
- `organizationListingIsCreatorAndWorkspaceScoped`
- `pageLimitsEmptyAndTailStatesAreExact`
- `personalListingIsCreatorScoped`
- `queryRuntimeStartsAfterFixtureProvisioning`
- `runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure`

### com.onecall.archtests.container.SharingRoleLeastPrivilegeIT (Failsafe, 12)

- `v010CommandRoleExactCatalogAcl`
- `v010CommandRolePositiveAndNegativeBehavior`
- `v010LegacyMigrationDigestsRemainCanonical`
- `v010MigrationIdentityAndPostgres16`
- `v010PrivilegeFailuresAre42501AndReadOnlyAuthorizedWriteIs25006`
- `v010QueryRoleExactCatalogAcl`
- `v010QueryRolePositiveAndNegativeBehavior`
- `v010RoleAttributesAndMembershipContainment`
- `v011LegacyMigrationBytesRemainUnchanged`
- `v011PublicReadAllowsOnlyProjectionAndDeniesWritesAndEscalation`
- `v011PublicReadRoleAndAclAllowlistAreExact`
- `v011UpgradeIsPrivilegeOnlyAtomicAndRejectsIncompatibleState`
