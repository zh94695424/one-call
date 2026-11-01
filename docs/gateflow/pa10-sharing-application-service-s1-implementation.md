# PA10-S1 — query-only sharing application-service implementation

## Authority and outcome

- Task: `pa10-s1-implementation-gpt-20261101-02`; work unit: `PA10-sharing-application-service-foundation`; gate: implementation S1; date: 2026-11-01, Asia/Shanghai.
- Detached checkout/unchanged HEAD: `03dfb5c902bc4e16a1d7e638cbed088ddd6e9400`; product base: `bdfafc706ce7c1f9f515fa27434139e31769576f`; accepted-plan checkpoint supplied by controller: `b2943f65bfc254f2ec190dd10e6c12294cbaae12`.
- Fixed plan: `docs/gateflow/pa10-sharing-application-service-plan.md`, SHA256 `b323f5368ed215c389b7448b8c90643cfe1cd22c55597e3419e65568cb54ec86`.
- Fix artifact: `docs/gateflow/pa10-sharing-application-service-plan-fix-1.md`, SHA256 `9b86b9543327538bb1d922ac6e0f01cbe31ddc3390929548a1a6f3553dd5590c`.
- Controller supplied environment-entry approval. This implementation additionally confirms Temurin 21.0.13+9, Maven 3.9.9, Docker Linux Engine 29.9.2 and actual PostgreSQL 16.15 locally. No environment downgrade, H2, skip, mocked SQL replacement or dependency-version override is used.
- Implemented exactly one behavioral slice: internal, read-only `listCustomerShares` on the real profile-gated PostgreSQL query runtime. Author completion is not independent review, controller acceptance, remote CI green or production readiness.

## Exact changed paths

All paths are repository-relative; these are the 26 intended source/document changes. Generated build/test/contract outputs remain ignored local evidence, not additional product edits.

```text
.github/workflows/ci.yml
backend/pom.xml
backend/README.md
backend/src/main/resources/application-persistence.yml
backend/src/main/java/com/onecall/core/sharing/internal/application/CustomerShareQueryService.java
backend/src/main/java/com/onecall/core/sharing/internal/application/TrustedShareCaller.java
backend/src/main/java/com/onecall/core/sharing/internal/application/SharePageRequest.java
backend/src/main/java/com/onecall/core/sharing/internal/application/ShareCursor.java
backend/src/main/java/com/onecall/core/sharing/internal/application/ShareSummary.java
backend/src/main/java/com/onecall/core/sharing/internal/application/SharePage.java
backend/src/main/java/com/onecall/core/sharing/internal/application/ShareQueryException.java
backend/src/main/java/com/onecall/core/sharing/internal/application/ShareQueryDiagnostic.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/ShareQueryRepository.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqShareQueryRepository.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingPersistenceConfiguration.java
backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingPersistenceProperties.java
backend/src/test/java/com/onecall/core/sharing/internal/application/CustomerShareQueryServiceTests.java
backend/src/test/java/com/onecall/archtests/SharingPersistenceConfigurationTests.java
backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/BootstrapDependencyBoundaryTests.java
backend/src/test/java/com/onecall/archtests/OneCallApplicationTests.java
backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java
backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java
docs/README.md
docs/gateflow/pa10-sharing-application-service-s1-implementation.md
docs/gateflow/pa10-sharing-application-service-s1-completion-1.md
```

`ModuleBoundaryTests.java`, historical `PostgresContainerIT.java`, module markers/annotations, default `application.yml`, V001–V009, OpenAPI, design/security documents, historical reviews/plans and `project-execution-control.md` remain untouched. No public module interface, schema, ACL, index or migration is added.

## Goal and acceptance mapping

| Approved signal / acceptance | Implemented behavior and executable proof |
| --- | --- |
| Minimal production-stack foundation | Explicit Hikari/Spring JDBC/jOOQ beans only under `persistence`; config/default-bootstrap tests and `queryRuntimeStartsAfterFixtureProvisioning`; default JAR exits normally without a DB. |
| Trusted creator context | Internal typed account plus optional organization; no role/owner/filter/transport selectors. One service business method and exactly two repository methods; unit context test, boundary reflection and personal/org/cross-account IT. |
| Exact current lifecycle checks | Each call reads only account id/status; selected organization id/status; membership organization/account/status. Missing/non-ACTIVE denies before grant fetch. All four ACTIVE membership roles work without expanding creator scope; revoked row plus replacement ACTIVE succeeds. |
| Exact secret-free projection | Eleven ordered typed fields are selected, not all-column fetch/redaction. Exact executed JDBC statement/parameter inventory and record-component allowlists; poisoned snapshots/hash/salt/credential/error-message witnesses, unknown/null mappings and unchanged database fingerprints. |
| Stable keyset listing | PostgreSQL-native `(valid_from DESC, share_id DESC)` and strict timestamp/text predicate; default 20/max 100/+1 lookahead. DB baseline traversal, mixed text/microseconds, UTC/non-UTC equality, insertion above anchor and invalid/cross-scope/nonexistent anchors. |
| Runtime privilege boundary | Distinct random NOINHERIT, non-owner, non-superuser query LOGIN; fixed `SET LOCAL ROLE sharing_role` first in each fresh read-only transaction, effective-role assertion, one transaction-bound checkout; denial/probe/read-only/rollback/pool-reuse/outer-transaction IT. |
| Preserve frozen foundations | All fourteen modules and exact dependency map/negative fixtures pass; unchanged 64 historical IT cases; exact source/CI/XML validation and positive/negative JAR firewall. Frozen-file hashes and unchanged contract-regression commands pass. |
| Honest capability statement | README/docs describe only internal metadata listing and opt-in query persistence. No auth/API/mutation/playback/provisioning/readiness assertion; controller control document is not edited. |

### Runtime and dependency evidence

Boot parent 4.1.2, Java 21, Modulith 2.1.2 and Testcontainers 2.0.5 pins remain. Effective POM, verbose dependency tree and enforcer validate pass, including convergence. BOM-managed resolved coordinates/scopes:

| Coordinate | Version | Scope |
| --- | --- | --- |
| `org.springframework:spring-jdbc` | 7.0.10 | compile |
| `org.springframework:spring-tx` (transitive) | 7.0.10 | compile |
| `org.jooq:jooq` | 3.21.9 | compile |
| `com.zaxxer:HikariCP` | 7.0.2 | compile |
| `org.postgresql:postgresql` | 42.7.14 | runtime |
| `org.flywaydb:flyway-core`, `flyway-database-postgresql` | 12.4.0 | test only |
| `org.testcontainers:testcontainers` | 2.0.5 | test only |
| `org.springframework.boot:spring-boot-starter-test` | 4.1.2 | test only |

No generated jOOQ code/plugin, SQL starter, JPA/web/security stack, production Flyway bean/import/property or migration identity is added. Surefire retains its two Flyway classpath exclusions. Enforcer retains original toolchain/convergence/firewall rules, narrows jOOQ/Hikari permission to the approved compile coordinates and bans product test tooling. The packaged JAR has 34 libraries, including exactly one each of `jooq-3.21.9.jar`, `HikariCP-7.0.2.jar`, `postgresql-42.7.14.jar`, `spring-jdbc-7.0.10.jar`, `spring-tx-7.0.10.jar`; all are compared with resolved runtime coordinates/versions/scopes, not wildcard acceptance. Existing Modulith/support transitive jars remain; jOOQ's r2dbc-spi/reactive-streams support is not a reactive server. Flyway, security/JPA/web/server/validation frameworks, unapproved codegen/DB frameworks, Testcontainers/JUnit/mock/test tooling are absent as checked by CI's negative inventory.

Only `onecall.persistence.url`, `.username`, `.password` and their `ONECALL_DB_URL`, `ONECALL_DB_USERNAME`, `ONECALL_DB_PASSWORD` environment inputs exist for runtime persistence. Missing/blank values refuse with `Explicit query credentials are required` before a pool can connect. The opt-in profile config suppresses jOOQ/Hikari logging; no URL/login/password default, common datasource key, production pool/timeout/SLO policy or migration control exists. Default bootstrap creates no DataSource/Hikari, DSLContext, transaction manager/template, query service or repository.

Fixture Flyway applies and validates all nine migrations before `SpringApplicationBuilder(OneCallApplication.class)` starts with in-memory query-only properties. The independently provisioned random query LOGIN has only test-owned `sharing_role` membership, no owner memberships/direct object grants; a distinct negative login cannot assume it. Tests assert non-superuser/session versus effective identities. Tests also temporarily revoke the query login's membership: the actual service refuses, does not fetch and does not fall back, before membership is restored.

`DataSourceTransactionManager` and the dedicated template use the same managed raw query source; jOOQ's provider wraps a `TransactionAwareDataSourceProxy` over that source. The template is READ_COMMITTED, readOnly=true and REQUIRES_NEW. Effective role/read-only/isolation/backend-pid probes, one checkout per invocation, fixed first role statement and outer write-transaction suspension prove the wiring. Role changes expire after commit/rollback. Illegal writes inside the listing transaction return SQLSTATE 25006; forbidden foreign-column reads/writes/schema creation/foreign row locks return 42501 in dedicated probes. These do not claim `sharing_role` lacks approved V009 mutation privileges globally.

### Exact service, port and immutable component contract

```text
SharePage CustomerShareQueryService.listCustomerShares(TrustedShareCaller, SharePageRequest)
void ShareQueryRepository.requireActiveCreatorContext(TrustedShareCaller)
List<ShareSummary> ShareQueryRepository.fetch(TrustedShareCaller, SharePageRequest, int fetchSize)

TrustedShareCaller(UUID accountId, Optional<UUID> organizationId)
SharePageRequest(Optional<ShareCursor> cursor, Optional<Integer> limit)
ShareCursor(UUID accountId, Optional<UUID> organizationId, Instant validFrom, String shareId)
SharePage(List<ShareSummary> items, Optional<ShareCursor> nextCursor, boolean hasMore)
ShareSummary(String shareId, UUID evidenceId, VerificationMode verificationMode,
  ShareStatus status, Instant validFrom, Instant expiresAt, Integer maxAdmissions,
  int admissionsUsed, boolean downloadAllowed, int codeGeneration, int revocationGeneration)
ShareQueryDiagnostic(String operation, ShareQueryException.Code errorCode,
  String exceptionClass, Optional<String> sqlState, String category, boolean roleAssertionPassed)
```

The only twelve new production types are the eight application types and four persistence types in the exact path list. Nested `ShareSummary.VerificationMode` = NONE/EXTRACTION_CODE/DESIGNATED_MOBILE/ORGANIZATION_MEMBER; `ShareStatus` = ACTIVE/EXPIRED/REVOKED/LOCKED. Exception codes = FORBIDDEN/VALIDATION_FAILED/CURSOR_INVALID/SERVICE_UNAVAILABLE/INTERNAL, each with fixed value-free text and no raw cause. Nullable max admissions is retained; all other summary values are required and validated. Pages copy immutable items; no map/entity/jOOQ record/snapshot escape or partial mapping result is returned.

### SQL, ordering and paging evidence

Executed grant projection, in order:

```text
share_id, evidence_id, verification_mode, status, valid_from, expires_at,
max_admissions, admissions_used, download_allowed, code_generation, revocation_generation
```

Every query has `created_by_account_id = caller`, followed by either personal `owner_organization_id IS NULL` or exact organization equality. There is no owner-account shortcut, OR-member widening, nullable-org equality, status/time/evidence filter, count or offset query. The only optional predicate is `valid_from < anchor OR (valid_from = anchor AND share_id < anchorId)`, followed by fixed descending timestamp/text ordering and a bound limit+1. Typed jOOQ identifiers/conditions, indexed prepared binds and PostgreSQL dialect are fixed; caller input never builds SQL. Timestamps use TIMESTAMPWITHTIMEZONE/OffsetDateTime, results become exact Instants, and anchors use ZoneOffset.UTC; IDs/enums use VARCHAR, UUIDs UUID, counters INTEGER and download BOOLEAN.

The test-only BeanPostProcessor wraps the actual managed source's Connection/PreparedStatement, delegates close/unwrapping, records executed placeholder SQL and parameter indexes/counts, and never stores setter values or driver toString output. No product listener/hook or second query pool/DSL is added. Executed first/anchored parameter counts are **personal 2/5, organization 3/6**, including the timestamp twice. The test proves exact eleven-column order and strict role/lifecycle/grant statement allowlist, with no wildcard, joins, secret/evidence/event fetch, advisory lock, foreign row lock, FOR UPDATE/FOR SHARE or alternate query path.

Actual fixture: `postgres:16`, image ID `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`; `PostgreSQL 16.15 on x86_64-pc-linux-musl`, gcc Alpine 15.2.0, 64-bit. Database `sharing_query`: datcollate=C, datctype=C, datistemplate=false; initial administrator session TimeZone=Asia/Shanghai and JVM=Asia/Shanghai. Query fixture sessions explicitly exercise UTC and America/New_York. This is fixture evidence, not production collation equivalence.

Fresh valid subject/session/call/SOLIDIFIED evidence/event/media/artifact parents support the grants without trigger disabling or FK shortcuts. A frozen ten-row baseline is obtained using the same database comparator, never Java sorting. Full service traversal exactly matches that baseline under JVM Asia/Shanghai/session America/New_York and JVM UTC/session UTC, including mixed-case/text IDs, timestamp ties and .123455/.123456/.123457 microseconds. Equal returned Instant anchors are asserted. Newer and database-comparator-selected tied-above-anchor inserts between page calls cause no baseline replay/gaps; this is not snapshot isolation. Legal unusual share IDs stay bound data. Well-formed nonexistent anchors follow the strict predicate without a row lookup. Scope reuse, null/invalid/unrepresentable/sub-microsecond anchors and invalid ID shapes refuse before SQL.

Defaults/bounds/lookahead/tails are exact: default 20, explicit 1–100, invalid values refused rather than clamped; at most 101 fetched. `hasMore` only reflects an extra row; the next cursor uses the last returned row, not the lookahead. Empty, under-limit and exact-limit tails have no cursor. No total/offset or serialized/signed/wire cursor is implemented.

### Lifecycle and sanitized failure evidence

Every listing rechecks ACTIVE account and, if selected, ACTIVE organization plus any current ACTIVE non-revoked membership. All four roles merely validate workspace participation; none allow another creator's grants. Missing/disabled account; missing/suspended/disabled organization; absent/suspended/revoked membership fail FORBIDDEN without the grant SELECT; an ACTIVE replacement succeeds. Personal listing does not require organization state.

Sequential owner-role commits of account disable, organization suspend and membership suspend deny the next query. Concurrent owner transactions hold uncommitted row updates behind bounded latches/future acknowledgments: unlocked listing sees the previously committed ACTIVE facts, then denies after acknowledged commit. Each rollback counterpart remains allowed. Tests have fixture-only statement/future deadlines and no sleeps/advisory/foreign-row-lock protocol. A race after a check can leave an in-flight advisory projection stale; no lease, serialized authorization or cross-page snapshot is promised.

Only one six-component diagnostic is emitted per failed invocation: constant operation, closed error code, actual exception class name, validated five-character SQLSTATE or absent, two-character class or NON_SQL, and effective-role assertion boolean. There are no throwable/message/SQL/bind/row/context/cursor/credential log arguments. Unit tests capture the real service logger; IT captures application/root logging. Test-only proxy failures yield distinct 42501/category42/role=false and 08006/category08/role=true facts; poisoned unknown/null enum mapping yields INTERNAL without values. Public exceptions are constant and cause-free. Role/pool cleanup, rollback and no partial page/no fallback are checked. Before/after fingerprints of grants, admissions/events, account/org/member, evidence/event, Outbox and audit remain equal across reads/failures.

## Exact tests and local validation

All tests are ordinary named `@Test` methods. Final source sets = CI literal expected sets = XML class/name pairs, with separate cardinality checks and **zero failure/error/skip/disabled**. The unchanged historical source and all 64 CI literal names are preserved. The sole approved rename is `BootstrapDependencyBoundaryTests.approvedPersistenceFrameworksAndForbiddenStacksAreExact`.

| Class | Runner | Exact methods | Final XML seconds |
| --- | --- | ---: | ---: |
| ModuleBoundaryTests | Surefire | 8 unchanged | 1.424 |
| OneCallApplicationTests | Surefire | 1 preserved name | 1.104 |
| BootstrapDependencyBoundaryTests | Surefire | 1 approved rename | 0.009 |
| CustomerShareQueryServiceTests | Surefire | 8 | 1.008 |
| SharingPersistenceConfigurationTests | Surefire | 2 | 0.061 |
| SharingQueryBoundaryTests | Surefire | 1 | 0.012 |
| PostgresContainerIT | Failsafe | 64 unchanged | 44.736 |
| SharingQueryServiceIT | Failsafe | 10 | 5.853 |

New exact names:

```text
CustomerShareQueryServiceTests:
defaultsAndBoundsAreDeterministic
trustedContextCannotBeOverridden
cursorBindingAndAnchorValidationAreDeterministic
lookaheadUsesLastReturnedRow
emptyAndTailPagesHaveNoCursor
projectionComponentsMatchFrozenListing
creatorContextDenialDoesNotFetchShares
technicalFailuresReturnNoPartialPageOrSensitiveText

SharingPersistenceConfigurationTests:
defaultProfileCreatesNoPersistenceBeans
persistenceRequiresExplicitQueryCredentials

SharingQueryBoundaryTests:
sharingQueryApiAndProjectionAreExact

SharingQueryServiceIT:
queryRuntimeStartsAfterFixtureProvisioning
personalListingIsCreatorScoped
organizationListingIsCreatorAndWorkspaceScoped
inactiveAndMissingCreatorContextsFailClosed
keysetTiesAndConcurrentInsertRemainStable
pageLimitsEmptyAndTailStatesAreExact
invalidAndCrossContextCursorsNeverWidenScope
listingProjectionNeverReadsOrReturnsSecrets
runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure
committedLifecycleChangesDenyNextQueryAndFailuresRollBack
```

The complete actual source/CI/XML sets for all eight classes, including the 64 historical names, are retained in `backend/target/pa10-evidence/test-inventory.json` (SHA256 `5bbcd5016d037974337542a5f3ce3d595072c6eb0eb24fa2845e5581a267af10`). CI rejects extra/missing/duplicate reports or testcases, unexpected source classes and parameterized/repeated/dynamic/conditional/disabled test annotations. It scans all container reports, not only narrow historical globs. The six Surefire classes total **21**; two Failsafe classes total **74=64+10**.

### Required commands, exits and timings

Exact toolchain exports: `JAVA_HOME=/opt/jdk21`; `PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"`. Commands below run from `backend/`. All exit **0**. Log stems refer to `backend/target/pa10-evidence/<stem>.log`; `commands.json` records exact commands, exits, durations and paths.

| Command | Log stem | Measured seconds |
| --- | --- | ---: |
| `./mvnw -B -ntp -v` | toolchain | 0.132 |
| `./mvnw -B -ntp help:effective-pom -Doutput=target/pa10-effective-pom.xml` | effective-pom | 1.432 |
| `./mvnw -B -ntp dependency:tree -Dverbose -Dincludes=org.springframework.boot:*,org.springframework:*,org.jooq:*,com.zaxxer:*,org.flywaydb:*,org.postgresql:*,org.testcontainers:*` | dependency-tree | 1.901 |
| `./mvnw -B -ntp validate` | validate | 1.390 |
| `./mvnw -B -ntp -Dtest=CustomerShareQueryServiceTests,SharingPersistenceConfigurationTests,SharingQueryBoundaryTests test` | targeted | 7.843 |
| `./mvnw -B -ntp verify` | default-verify | 6.376 |
| `docker info` | docker-info | 0.611 |
| `./mvnw -B -ntp -Pcontainer-smoke verify` | container-verify | 53.081 |
| `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1` | boot | 1.038 |
| `grep -F 'Started OneCallApplication' target/boot.log` | boot-started | 0.004 |
| `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt` | jar-index | 0.058 |

Final instrumented required-command sequence: start `2026-11-01T17:37:28.082821+08:00`, finish `2026-11-01T17:38:49.236520+08:00`, measured monotonic end-to-end **73.872 seconds**. Individual elapsed values use the same monotonic harness, not Maven/XML clocks. Maven's container-build timer reports 56.480 seconds; XML execution totals are recorded separately above, not substituted for harness timing.

Targeted run: 11 Surefire/0 Failsafe. Default verify: 21 Surefire/0 Failsafe; its Failsafe report directory is absent. Container profile: 21 Surefire +74 Failsafe. Each phase starts with report directories cleared and archives its own reports under `backend/target/pa10-evidence/{targeted,default-verify,container-verify}/{surefire-reports,failsafe-reports}` as applicable. Final live reports are `backend/target/surefire-reports/TEST-*.xml` and `backend/target/failsafe-reports/TEST-*.xml`.

Both updated CI Java validators were executed locally verbatim from workflow YAML using bash fail/pipefail: exact source/report inventory (0.032 seconds) and resolved production-JAR firewall (1.601 seconds), both exit 0. Full commands/paths: `ci-local.json`; logs: `assert-container-smoke-actually-ran.log`, `production-jar-dependency-firewall.log`. The firewall's `./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa10-runtime-dependencies.txt` also exits 0.

The **unchanged** contract-regressions job commands were extracted from CI YAML and executed locally, preserving command text/cwd: Node exact-version assertion (0.006s), `npm ci` (5.914s), `npm run check:s3` (8.386s), A2 s3 (0.074s), A3 s3 (0.083s), A4 s2 (0.342s), A5 s1 (1.858s), A5 s2 with doc links (2.542s). All exit 0, Node v22.23.4/npm 10.9.9; full validator flags and individual logs are retained in `contracts.json` and `contract-*.log`. Contract-command measured total **19.205s**; local CI guards total **1.633s**; sum with required-command sequence **94.710s** (separate phases, not authoring duration or production latency).

Development attempts are not hidden: initial targeted compilation/mock/config-test errors and three initial container fixture/probe runs were corrected without weakening accepted assertions. Corrections covered typed assertion compilation, mock restubbing, failed-context inspection, global fixture ID uniqueness, caller-HMAC uniqueness, choosing tied-above IDs with the DB comparator, requiring a real transaction for negative SET LOCAL, and probing a truly forbidden evidence column rather than an existing approved id-column ACL. Final phase archives and hashes are the authoritative passing runs. Prior container attempt logs remain `container-first.log`, `container-second.log`, `container-third.log`; a broken-migration ERROR log in the passing historical suite is its intentional failure-visibility testcase, not a failed build.

## Frozen hashes and evidence retention

`backend/target/pa10-evidence/frozen-hashes.json` covers **188 tracked frozen files**, each byte-compared with HEAD before hashing: migrations, OpenAPI, matching design specs, security/protocol/capacity/acceptance inputs, fixed plan/fix, historical container source and controller control file. Manifest SHA256: `4a49ded7de6432fb657302634c9098f5045dcdbff821def6a33b3fbea77b57ad`. No V010, migration/index/ACL/design/OpenAPI/control edit exists.

| Frozen migration | SHA256 |
| --- | --- |
| V001 | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| V002 | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| V003 | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| V004 | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| V005 | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| V006 | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| V007 | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| V008 | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| V009 | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |

Data-model design SHA256: `3a506e578fdde6a89e684a64ddd31261b397e22f7bdb0ac589331bb6eebba761`; production-security design SHA256: `4e7ac923556c5ac1b8ec4103e7a85f02e79597e852ab4a4a51c84db54b6cd447`. Per-file OpenAPI/design hashes are in the frozen manifest; their bytes and contract regression outputs are unchanged.

Additional local evidence SHA256:

| Path | SHA256 |
| --- | --- |
| `backend/target/pa10-effective-pom.xml` | `9885e0392d1065a30df16d4363cf1487c04be0ee1197763e1d987e8dbf9e7cb1` |
| `backend/target/pa10-runtime-dependencies.txt` | `9af85de2b8b38ba170c84b5727ec9f52b389b3a550446309ae6b4b8df2ed8c5f` |
| `backend/target/pa10-evidence/commands.json` | `011365175fc9cf2285a22869ce095cd9f094d39291530018f0371dc8046be597` |
| `backend/target/pa10-evidence/contracts.json` | `8f64f414c22141ebaa868e23d7d31a571295a9de81363046f5db5bf1a4689617` |
| `backend/target/pa10-evidence/ci-local.json` | `7976e3c93caf007b40ee758e45ede4d6b0713993ce933a78d535c81af332efde` |
| `backend/target/pa10-evidence/jar-inventory.json` | `a5481c900a93c12754dd7f556d2784e2aa41b8ef092a26cd4b6e8f05c481fe91` |
| `backend/target/pa10-evidence/summary.json` | `8535a554770f4b85790b0cf929c5b8dd0d0423216e7326e7ea7d0e8e80d8a8c6` |
| `backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar` | `11574315cd7a9ba2f140bdcfd1cf888eb90b005c4dcc0ee3f38a1b4b4e630afc` |
| `backend/target/boot.log` | `182e5a953c19a9b421bf0a761666523e77b68f1a5bdabaa43fb21122f0e3c830` |
| `backend/target/jar-index.txt` | `129480f2efc371cb20cab2f8c50dac279499eb87e863a31b1b612d0a1dc2d9d1` |

All retained command logs, phase XML copies and evidence outputs also have a per-file digest in `backend/target/pa10-evidence/evidence-manifest.json`, SHA256 `7df35ec4c368d59700bc730db12009c55406b0cff2052c7c3c9229de4aec7ce2`. JDBC commit/rollback counters additionally assert successful commit and exactly one rollback/no commit on injected technical failures; missing properties and blank properties are tested separately.

These ignored target outputs are retained in this isolated checkout for controller intake; they are not remote CI artifacts and are not added to source control. `test-inventory.json` includes SHA256 for every final XML report. The completion report repeats those report hashes for durable provenance.

## Residual ownership and stop boundary

- Identity/Backend/Security owns real authentication and trusted adapter context. Sharing/Backend owns any eventual same-module adapter or public module promotion; neither is implemented here.
- Backend/API/Security owns Q15 opaque wire cursor integrity/serialization/512-character policy. This slice supplies typed internal scope binding only.
- Product/Security/Sharing owns organization resource policy and Q12/Q22 inputs. No role matrix, KDF/TTL, mutation/public-share/admission/ticket/media/playback or evidence service is implemented or planned by this unit.
- Platform/Database/Security owns Q17/Q23 production provisioning identity, migration ordering, role membership and ownership transfer. Flyway fixture provisioning is not a deployment implementation.
- Backend/Database/Platform Reliability owns production collation/time zone, creator/order index review, scan/sort capacity and timeout/SLO decisions. Limit+1 bounds returned rows, not scan cost or latency.
- Backend/API owns current-state advisory semantics and protected-request rechecks; Security/Observability/API owns downstream serializer/edge/log-collector egress verification. Local canaries are not an end-to-end production redaction/readiness claim.
- No required local command remains blocked. Remote CI was not dispatched and no exact-head remote run/PR is claimed. Controller retains acceptance/review/gate authority and owns `project-execution-control.md`.
- Clean stop: no staging, commit, branch, push, PR/issue/reviewer/comment/external mutation or code-review phase; no other worktree/private controller state was accessed. Only this detached checkout and task-owned ephemeral test containers/build outputs were used.
