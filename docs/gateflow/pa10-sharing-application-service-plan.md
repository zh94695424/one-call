# PA10 — sharing application-service foundation plan

## 1. Authority, goal and success signal

- Work unit: `PA10-sharing-application-service-foundation`; task: `pa10-sharing-application-service-plan-gpt-20261101-01`; gate: **plan**, dated 2026-11-01. This document authorizes no implementation or gate transition.
- Product base: `bdfafc706ce7c1f9f515fa27434139e31769576f` (merged PR #17); inspected detached controller checkpoint: `7fce3020c60a466985b5fb4834c8e30127689386`. The active PA10 checkpoint in `project-execution-control.md` is binding, not the historical PA9 entries below it.
- Confirmed goal: introduce the minimal PostgreSQL runtime persistence foundation and implement **only** the read-only `listCustomerShares` application-service path. A trusted internal caller supplies authenticated account, selected optional organization and paging. Client data cannot establish or override account, owner, organization, role or security context.
- Motivation: V001–V009 and the frozen listing contract exist, but no executable service consumes them. A restricted, secret-free creator listing is the smallest useful runtime increment that demonstrates the persistence boundary without requiring unapproved sharing policies.
- Success signal: the real service runs through Hikari/jOOQ against PostgreSQL 16 migrated by Flyway V001–V009, with effective `sharing_role`, separate migration/query credentials, active account/workspace checks, strict creator isolation, deterministic keyset pages and exactly the sanitized listing projection. Existing schema, ACL, module and contract regressions remain intact. Default non-web bootstrap remains database-free.
- Verified instruction discovery: `find .. -name AGENTS.md -o -name CLAUDE.md` and `git ls-files '*AGENTS.md' '*CLAUDE.md'` found no project instruction files; applicable ancestor instruction-file checks also found none. No project `AGENTS.md` or `CLAUDE.md` applies.
- Planning writes are limited to this artifact. Product/test/POM/CI/README/control paths below are ownership for an implementation **after** accepted-plan entry, not changes in this gate. Do not stage, commit, push, mutate external state, approve, request reviewers or claim acceptance/readiness.

### Binding non-goals

No controllers, routing, generated OpenAPI runtime, customer authentication/session/authorization infrastructure; no create, revoke, rotate, public-share GET, admission, ticket, media authorization, playback or public-share service; no evidence collaborator/application service; no Redis/COS/KMS/OTP/KDF, share-policy selection or Q12/Q22 values; no new migration, database object, column, key, status, event or business policy; no production credentials, provisioning implementation, manifests or production-readiness claim. No speculative caching, generic repository framework, unrelated refactor or future implementation unit is included.

## 2. Goal alignment and first-principles judgment

| Confirmed signal | Decision / owned behavior | Validation |
| --- | --- | --- |
| Minimal production-stack foundation | Explicit profile-gated Hikari, Spring JDBC transaction manager, jOOQ and Flyway; no web stack | Bootstrap tests, runtime Spring context, packaged dependency inventory |
| Trusted creator context | Internal typed context, no client DTO/security selector; account predicate always present | Exact type/method inventory; personal/org/cross-account PostgreSQL cases |
| Active personal/organization context | Existing lifecycle advisory locks and exact V009 foreign-column reads only | Disabled/missing account, absent/inactive organization/member; all existing membership roles |
| Secret-free exact projection | Eleven explicit grant columns; immutable closed result, no raw row/snapshot | Exact SQL select-field and record-component sets; poisoned-secret fixtures |
| Stable listing | Database-native descending timestamp/text order and strict keyset predicate | Timestamp ties, lexical IDs, inserts between pages, empty/tail pages, invalid cursor |
| Runtime privilege boundary | Separate query LOGIN; transaction-local fixed `sharing_role`; read-only transaction | Effective/session identity, read-only setting, permission negatives, pool reuse and failures |
| Preserve frozen foundations | No migration/ACL/module annotation changes; retain 64 historical IT methods | Exact source/CI/XML sets, schema/ACL regressions, module negative fixtures |
| Honest capability statement | README/control/docs index describe only this service and default bootstrap | Documentation diff review; no API/mutation/readiness claims |

First principles: this is a metadata read, not permission to play or share evidence. Neither a selected organization nor its administrator role permits reading another creator's shares. Membership validates the selected workspace; it does not widen the grant predicate. No evidence/artifact/media lookup, snapshot reconstruction, KDF, expiry transition, count update, audit append or Outbox is necessary. Do not confuse required lifecycle transaction-control locks with domain writes. One behavioral slice is preferable: separately shipping classpath exceptions, an untested repository or an incomplete access check has no independent review value.

## 3. Reconciled sources and direct evidence

| Source inspected | Relevant evidence and consequence |
| --- | --- |
| `docs/12-technology-stack-and-system-architecture.md` §§2,6,10 | Java 21, modular monolith, PostgreSQL 16+, jOOQ/Flyway/Hikari, explicit local transactions, owner boundaries. Its recommended Boot 3.x predates the actual frozen Boot 4.1.2 pin; retain the repository pin, not a downgrade or broad architecture-doc rewrite. |
| `docs/14-production-data-model-and-flyway-schema-spec.md` §§2.5,2.12,4.44–4.46 (sharing tables),7.4,9.0–9.0.2,9.29–9.31,10.21–10.23 | Creator/workspace predicates and lifecycle advisory namespaces; exact runtime grants; stored lifecycle facts and canonical snapshots/fingerprints. Listing neither hashes nor reconstructs `content_snapshot`, nor invokes the creation-only `evidence.read_shareable_projection`. `status` and generations are stored facts, not inferred admission permissions. |
| `docs/15-production-openapi-surface-spec.md` listing index, projection clarification, error and pagination sections | Listing order, creator-only evidence ID, closed metadata, limit default 20/max 100; opaque cursor bound to sort/filter context. Wire encoding remains Q15. |
| `api/openapi/onecall-v1.openapi.yaml` `listCustomerShares` and transitive schemas | GET has only `CursorParam`/`LimitParam`, customer trust surface, `SharePage.items -> ShareSummary`, not `ShareState` directly. `ShareSummary` and `ShareState` have the same eleven metadata properties and ten required non-null fields; `maxAdmissions` is optional/nullable. Read `Cursor`, error responses, `Problem`, `ProblemCode`, `FieldProblem` as well. Preserve these frozen schemas; do not add a wire cursor or HTTP mapping. |
| `docs/17-production-security-design-spec.md` customer trust, deferred input and audit/redaction sections | Server-derived context; no credential/hash/object identity in results/errors/logs; no raw SQL/stack disclosure at a future external boundary. Q12/Q22 policy and Q17/Q23 provisioning remain deferred. A listing is not an audit-fact read or business mutation; no access-event/audit schema or new event is required. |
| Sharing sources and `OneCallApplication` | Sharing has only `ModuleMarker` and `@ApplicationModule(allowedDependencies = {"evidence"})`; application is a root component scan. No current service or persistence adapter exists. |
| `backend/pom.xml`, `application.yml` | Boot 4.1.2/Java 21/Modulith 2.1.2/Testcontainers 2.0.5. PA0 bans JDBC starter, jOOQ, Hikari, compile/runtime Flyway; PostgreSQL and Flyway are test-only; Surefire hides Flyway. Application explicitly remains non-web, with Modulith Moments excluded. |
| Architecture tests and `.github/workflows/ci.yml` | Ten Surefire methods: eight module tests, one bootstrap context, one absent-framework test. Fourteen modules/exact dependency map and negative cycle/forbidden/internal fixtures. CI insists on 64 distinct historical container methods, exact source/expected/XML equality, zero skips and a fat-jar exclusion of security/Flyway/jOOQ/Hikari/PostgreSQL/Testcontainers. These are executable PA0 assumptions needing narrow replacement, not deletion. |
| V001–V009 migration chain | V001 establishes initial owners/runtime roles/platform/audit; V002 defines account ACTIVE/DISABLED, organization ACTIVE/SUSPENDED/DISABLED and membership ACTIVE/SUSPENDED/REVOKED with a unique non-revoked pair. V003 subject prerequisites, V004–V007 call/order/entitlement/consent prerequisites and late FKs, V008 evidence parent tables/deferred creation cycle, V009 sharing grant/history and ACLs. Full chain is required for real fixtures. V009 grants `sharing_role` account `(id,status)`, organization `(id,status)`, membership `(organization_id,account_id,role,status)` SELECT plus schema USAGE. No Java IAM dependency is needed for these expressly authorized SQL reads. |
| V009 grant/index/lifecycle details | Grant `id` is UUID but `share_id` is unique opaque **text** (1–128); sort/cursor uses `share_id`, never `id`. Existing secondary grant indexes are `(evidence_id,status)` and `(status,expires_at)`, not the listing key. No index/migration is added. Grants remain NOLOGIN roles and existing privileges remain unchanged. |

### Dependency verification performed in this planning environment

Actual local Maven POMs, not remembered versions, were inspected:

- `~/.m2/repository/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom`: manages jOOQ **3.21.9**, HikariCP **7.0.2**, Flyway **12.4.0**, PostgreSQL **42.7.14**, Spring Framework **7.0.10**, Surefire/Failsafe **3.5.6**. The imported local `org.jooq:jooq-bom:3.21.9` explicitly manages `org.jooq:jooq:3.21.9`; local `jooq-parent:3.21.9` sets Java/release 21. Thus managed jOOQ is compatible with the pinned Java baseline at the published-POM level, without an override.
- Boot's actual BOM explicitly lists `spring-boot-starter-jooq:4.1.2`, `spring-boot-jooq`, `spring-boot-jdbc`, `spring-boot-flyway` and the Flyway starter. The jOOQ starter therefore exists in the Boot 4.1.2 managed surface. Its actual artifact/transitive POM is not cached; remote resolution could not be proven here (DNS failure). Do not claim a resolved starter tree.
- Decision: use explicit libraries and explicit wiring, **not** either SQL starter. This keeps only needed mechanisms, avoids starter auto-configuration and makes the no-database default straightforward. Inspection of the cached Boot 4.1.2 `spring-boot-autoconfigure` JAR's `AutoConfiguration.imports` found no JDBC/jOOQ/Flyway/transaction entries; SQL auto-configuration belongs to separate Boot modules. No guessed Boot 3 auto-configuration exclusion names are needed.
- Offline `help:effective-pom` failed because `maven-help-plugin:3.5.2` is not cached; Maven Central curl failed `Could not resolve host: repo.maven.apache.org`. This is a probe limitation, not evidence that the BOM lacks jOOQ. Implementation must obtain effective-POM/tree and successful resolution before declaring its dependency change validated. Do not downgrade, guess versions or weaken convergence to work around resolution failure.
- Baseline probe: `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH /opt/maven-3.9.9/bin/mvn -o -B -ntp -f backend/pom.xml test` **passed**, 10 tests, zero failures/errors/skips (log `/tmp/pa10-baseline-test.log`). Java is Temurin 21.0.13+9, Maven 3.9.9. `docker info` failed permission denied on `/var/run/docker.sock`; no container success is claimed. Real PostgreSQL execution is required during implementation, not replaced with H2/mocks or a skip.

## 4. Implementation ownership and call path

All new product types stay inside the existing `com.onecall.core.sharing` module, under internal subpackages; leave `ModuleMarker` and `package-info.java` untouched. Do not expose a named interface or introduce a module/shared framework. Internal Java types may be public for Spring/tests, but are not Modulith public module API, transport DTOs or authentication facts.

| Likely exact path under `backend/` | Responsibility |
| --- | --- |
| `src/main/java/com/onecall/core/sharing/internal/application/CustomerShareQueryService.java` | Sole entry `listCustomerShares(TrustedShareCaller, SharePageRequest)`; validation, read transaction, role assertion, checks, repository call, page assembly and bounded failures. |
| Same directory: `TrustedShareCaller.java` | Immutable authenticated `UUID accountId`, `Optional<UUID> organizationId`; non-null option, no role/owner/token/client field. Only trusted embedding code may construct/use it; no controller binding/factory/session implementation. |
| Same directory: `SharePageRequest.java`, `ShareCursor.java` | Optional cursor and optional limit (effective default 20). Cursor holds account UUID, optional organization UUID, exact last `Instant validFrom`, text `shareId`; fixed operation/sort by type, no arbitrary selector. |
| Same directory: `ShareSummary.java`, `SharePage.java` | Closed immutable metadata record (with nested closed mode/status enums) and copied immutable item list, optional next cursor, `hasMore`. No map/JSON/record/entity escape. |
| Same directory: `ShareQueryException.java` | Closed codes `FORBIDDEN`, `VALIDATION_FAILED`, `CURSOR_INVALID`, `SERVICE_UNAVAILABLE`, `INTERNAL`; constant sanitized messages, no request/database values or raw SQL causes in public text. |
| `src/main/java/com/onecall/core/sharing/internal/persistence/ShareQueryRepository.java` | Specific port: `requireActiveCreatorContext(TrustedShareCaller)` (locks + exact lifecycle checks) and `fetch(TrustedShareCaller, SharePageRequest, int fetchSize)` returning only `List<ShareSummary>`. No generic CRUD, raw jOOQ result or alternate context. |
| Same directory: `JooqShareQueryRepository.java` | Bound, schema-qualified jOOQ DSL, fixed fields/types, fixed role, lifecycle queries and keyset SELECT. No generated database model/codegen plugin is necessary for eleven columns. |
| Same directory: `SharingPersistenceConfiguration.java`, `SharingPersistenceProperties.java` | `@Configuration @Profile("persistence")`, typed `onecall.persistence` connection properties, Hikari lifecycle, Spring transaction manager/template, transaction-aware jOOQ connection provider and optional separately credentialed Flyway startup. Register the service/repository only here, not unconditionally via component annotations. |
| `src/main/resources/application-persistence.yml` | PostgreSQL dialect/logging settings and environment placeholders; no checked-in password or usable default login. Default `application.yml` keeps `web-application-type: none` and Moments exclusion; no runtime profile active by default. |
| `src/test/java/com/onecall/core/sharing/internal/application/CustomerShareQueryServiceTests.java` | Eight deterministic unit tests using the specific repository port, real Spring transaction-template contract as needed; no Docker. |
| `src/test/java/com/onecall/archtests/SharingPersistenceConfigurationTests.java` | Two no-network config tests: profile gating and missing/separate migration properties. |
| `src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | One exact internal method/input/result/SQL-boundary test, including no forbidden product dependencies. |
| `src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java` | Ten actual-service PostgreSQL methods, manual Testcontainers lifecycle; private fixtures or a package-private `SharingQueryFixture.java` in the same test directory. Do not refactor the historical giant IT to share fixtures. |
| Existing `pom.xml`, architecture/bootstrap tests, `.github/workflows/ci.yml`, `README.md` | Narrow runtime dependency/firewall/Failsafe/test inventory and capability documentation changes described below. |

Call path: trusted in-process embedding/test -> service validate -> transaction template starts fresh read-only transaction -> fixed `SET LOCAL ROLE sharing_role` and effective-role assertion -> repository lifecycle locks/checks -> repository grant SELECT -> service trims lookahead/maps cursor -> commit/rollback and release -> immutable page. No HTTP or identity-provider path is introduced.

Exact business signatures: `SharePage listCustomerShares(TrustedShareCaller caller, SharePageRequest request)`, `void requireActiveCreatorContext(TrustedShareCaller caller)`, and `List<ShareSummary> fetch(TrustedShareCaller caller, SharePageRequest request, int fetchSize)`. Record components: `TrustedShareCaller(UUID accountId, Optional<UUID> organizationId)`; `SharePageRequest(Optional<ShareCursor> cursor, Optional<Integer> limit)`; `ShareCursor(UUID accountId, Optional<UUID> organizationId, Instant validFrom, String shareId)`; `SharePage(List<ShareSummary> items, Optional<ShareCursor> nextCursor, boolean hasMore)`. Summary enum types are nested `VerificationMode` and `ShareStatus`. The repository reuses validated request state and must not accept an independent creator/scope override. Empty optionals are allowed; null option containers are invalid. Reject sub-microsecond cursor values rather than rounding; a returned cursor always uses the exact database-returned instant.

## 5. Runtime, credentials and transaction decisions

### Dependencies and default safety

Retain parent, Java/toolchain, Modulith and Testcontainers pins. Direct **compile** dependencies: `org.springframework:spring-jdbc` (transaction infrastructure transitively), `org.jooq:jooq`, `com.zaxxer:HikariCP`, `org.flywaydb:flyway-core` (explicit configuration code imports it). Direct **runtime**: `org.postgresql:postgresql`, `org.flywaydb:flyway-database-postgresql`. Runtime scope remains available on test classpath; existing container fixtures still compile against Flyway core. Keep Testcontainers and test starter strictly **test**. Use BOM versions only, no jOOQ override, JPA, web or security starter. No database code generation or migration plugin.

`persistence` is an opt-in Spring profile, independent of Maven `container-smoke`. Without it, no Hikari pool, DSLContext, transaction manager, Flyway or service/repository bean is created, even though libraries are packaged. With it, require URL/user/password and construct one managed query Hikari pool. No embedded DB fallback. Configure jOOQ `SQLDialect.POSTGRES`, `Settings.executeLogging=false`; disable jOOQ execute-listener SQL/bind logging and credential/config dumps. Avoid record logging, SQL exception logging and cursor/context logging in service/repository. Profile tests assert absence of all runtime beans by type, not only historical bean names.

Configuration keys are exactly `onecall.persistence.url`, `.username`, `.password`, and `.migration.enabled`, `.migration.url`, `.migration.username`, `.migration.password`. Corresponding environment inputs are `ONECALL_DB_URL`, `ONECALL_DB_USERNAME`, `ONECALL_DB_PASSWORD`, `ONECALL_MIGRATION_ENABLED` (default false), `ONECALL_MIGRATION_DB_URL`, `ONECALL_MIGRATION_DB_USERNAME`, `ONECALL_MIGRATION_DB_PASSWORD`. Validate missing/blank values without echoing them. No default URL/user/password; no production pool-sizing or timeout/SLO policy is selected. Use bounded fixture-only statement/lock/future timeouts in integration tests.

Optional `onecall.persistence.migration.enabled` defaults false. When true, require **separate** migration URL/user/password, same database target and a different user from query login; no fallback to query credentials or common `spring.datasource` credentials. Construct Flyway with this separate connection configuration, locations `classpath:db/migration`, validate enabled, no clean/baseline/repair/out-of-order shortcuts; run migration once before constructing the query service. Order initialization by explicit bean dependency/provider, not incidental declaration order. Migration failure prevents the service bean and startup. Close its connections; it is never supplied to jOOQ. Existing provisioned schema can be queried with migration disabled. No PA10 production login/grant provisioning is implemented.

### Transactions and role

Use a named `DataSourceTransactionManager` on the raw query Hikari source and a dedicated `TransactionTemplate` with `READ_COMMITTED`, `readOnly=true`, `REQUIRES_NEW`. The fresh transaction avoids accidentally inheriting an embedding caller's write transaction, role or isolation. jOOQ `DataSourceConnectionProvider` wraps a `TransactionAwareDataSourceProxy` over the **same** raw pool; do not let DSLContext acquire an independent autocommit connection. Do not use jOOQ's independent transaction API or a second DataSource for queries.

First SQL in each service transaction: fixed literal `SET LOCAL ROLE sharing_role`; verify `current_user = 'sharing_role'` before lifecycle/grant reads. No role supplied in context/config/query. Login must be a separately provisioned non-owner, nonsuperuser member allowed to assume this NOLOGIN role; lack of membership fails closed. `SET LOCAL` expires on commit/rollback, including permission/data errors. No session-scoped SET ROLE and no query as the migration login. Tests assert `transaction_read_only = on`, one bound connection, and rollback/pool reuse. Use no fallback on role failure. Read-only is a service transaction constraint, not a claim that V009 makes `sharing_role` globally SELECT-only (it has approved mutation privileges for other contracts).

Lifecycle serialization uses existing `pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?, 0))` calls with bound canonical lowercase UUID namespace text, in order: selected organization rank 7 `organization_access.organization:<org>`, rank 8 `organization_access.membership:<org>:<account>`, then rank 9 `customer_iam.account:<account>`. Personal takes rank 9 only. Re-read exact account `(id,status)`, and selected organization `(id,status)` and membership `(organization_id,account_id,status)` **after** locks. Require account ACTIVE; selected organization ACTIVE and one non-revoked membership ACTIVE. Missing/disabled/suspended/revoked fails `FORBIDDEN`; an old revoked membership does not deny a replacement ACTIVE row. Membership `role` need not be selected: ADMINISTRATOR/MEMBER/EVIDENCE_MANAGER/AUDITOR are neither newly granted nor denied listing power. No role matrix, owner collaborator or foreign row lock. Locks end with transaction and do not cross I/O. This follows docs/14 §§9.0.2/10.21 without new grants.

## 6. Exact service, SQL and cursor contracts

### Inputs, result, refusal

- Non-null trusted caller with non-null account and optional organization; missing trusted context is an internal precondition failure (`INTERNAL`), not an unauthenticated transport implementation. `UNAUTHENTICATED` remains the absent authentication adapter's responsibility.
- Missing limit -> 20; explicit 1–100 inclusive; otherwise `VALIDATION_FAILED`, never silently clamp. Missing cursor -> first page. No offset, total, owner/account/workspace/role filters, evidence filter or status filter.
- Cursor is internal typed state only: exact caller account/workspace binding must match; non-null valid anchor timestamp and share ID, ID length 1–128 Unicode code points with V009's nonblank-space shape, microsecond precision compatible with PostgreSQL. Reject null/malformed anchors, non-finite/unrepresentable timestamp and mismatched binding with `CURSOR_INVALID`, before SQL. Do not truncate/round client anchors or parse cursor IDs as UUIDs. Cursor constructor/request validation must permit deterministic typed refusal, not leak JVM validation text. Cursor need not reference a currently existing row.
- No wire parser/string, Base64/JSON codec, signature/encryption/key, digest format, wire version or transport status mapper is implemented. Q15 opaque 1–512-character serialization must eventually bind sort and normalized caller/workspace filter without exposing raw IDs/timestamps. Internal scope equality implements binding without inventing that serialization. `CURSOR_FILTER_MISMATCH` is not added to this operation's declared errors; context reuse is `CURSOR_INVALID` here.
- Store and return timestamp instants without millisecond truncation; PostgreSQL returns microseconds. Sorting/comparison is performed by the same database text collation, not Java lexical or numeric comparison. `share_id` uniqueness deterministically breaks timestamp ties under the database ordering. Do not introduce `COLLATE`, new normalized IDs or a new index.
- `ShareSummary` has **exactly** `shareId:String`, `evidenceId:UUID`, `verificationMode`, `status`, `validFrom:Instant`, `expiresAt:Instant`, nullable `maxAdmissions:Integer`, `admissionsUsed:int`, `downloadAllowed:boolean`, `codeGeneration:int`, `revocationGeneration:int`. It is the metadata set shared by OpenAPI ShareState/ShareSummary; include nullable `maxAdmissions` internally. All other fields are required/non-null. Modes exactly NONE/EXTRACTION_CODE/DESIGNATED_MOBILE/ORGANIZATION_MEMBER; statuses exactly ACTIVE/EXPIRED/REVOKED/LOCKED. Unknown/inconsistent database values fail `INTERNAL`, not a new status or partial result.
- `SharePage` has exactly `items`, `nextCursor`, `hasMore`. Fetch effective limit + 1 (at most 101); emit first limit; `hasMore` iff extra row exists; next cursor only if `hasMore`, anchored to **last returned**, not lookahead, row. Empty/tail page returns absent cursor and false; no total query.
- Connection/role/timeout/migration availability failure -> sanitized `SERVICE_UNAVAILABLE`; unexpected mapping/invariant error -> sanitized `INTERNAL`; rollback and no partial page. Programmer defects must not be swallowed as empty state or logged with rows/binds. No retries/fallback credentials. Authentication, rate limiting, RFC 9457 construction and external error logging remain outside scope.

### SQL behavior (all values bound; table/column/role names fixed)

Lifecycle existence reads use only the exact columns above, no JOIN to evidence or foreign write/row lock. Grant query:

```sql
SELECT share_id, evidence_id, verification_mode, status, valid_from, expires_at,
       max_admissions, admissions_used, download_allowed, code_generation,
       revocation_generation
FROM sharing.evidence_share_grant
WHERE created_by_account_id = :callerAccountId
  AND owner_organization_id IS NULL
  -- subsequent page only:
  AND (valid_from < :lastValidFrom
       OR (valid_from = :lastValidFrom AND share_id < :lastShareId))
ORDER BY valid_from DESC, share_id DESC
LIMIT :limitPlusOne;
```

For organization context replace **only** `owner_organization_id IS NULL` with `owner_organization_id = :callerOrganizationId`. For first page omit the entire anchor clause. Build exactly these two workspace branches and optional anchor with jOOQ typed fields/conditions, not caller-built SQL. The creator predicate is mandatory in every branch. No `OR creator OR member`, owner-account shortcut, nullable-org equality, cross-account cursor lookup, OFFSET or order fallback.

Do not add a status/time validity predicate: this is a listing of stored creator metadata, including expired/revoked/locked grants. Do not materialize EXPIRED, alter counters/revisions/generations or re-evaluate creation/playback access policy. No need to read `share_access_event` despite its operation-level A0 model annotation: ShareSummary contains no event field.

No `SELECT *`, generated all-column record, `content_snapshot`, capability hash, salt/code hash, content digest, internal grant UUID, creator/org IDs, object IDs/keys/versions/buckets, URL, phone, HMAC, ciphertext or raw credential enters repository result or service page. No fetch-and-remove redaction. Stored RFC 8785 snapshots/hashes remain immutable and unobserved; PA10 does not validate/rewrite their creation fingerprints. Projection validation is an exact allowlist, not a growing secret blacklist.

## 7. Preserve architecture and evolve only PA0 guards

- Preserve fourteen modules, exact annotation dependency map (sharing -> evidence only), no cycles/internal access; no IAM Java imports, no evidence service. SQL column reads use V009's explicit ACL, not a new Modulith edge. Keep the existing discovered-edge test's emptiness assertion since no cross-module Java edge is introduced; only its PA0-specific explanatory text may be clarified.
- Enforcer: retain JDK/Maven bounds, duplicate dependency/convergence checks, security/JPA bans and unrelated firewall rules. Replace blanket jOOQ/Hikari/Flyway prohibitions with exact approved coordinates/scopes above and a runtime inventory test; retain the unused JDBC **starter** ban because explicit `spring-jdbc` is chosen. Ban unapproved jOOQ codegen/meta families from product dependencies and test-only libraries from compile/runtime. Do not simply remove the enforcer execution.
- Replace the single `forbiddenFrameworksAreAbsentFromTheBootstrapClasspath` method with `approvedPersistenceFrameworksAndForbiddenStacksAreExact`: expected core persistence types present; Security, JPA and web-server stacks absent; test tooling absent from product dependency scopes/JAR (not from Surefire, where it is legitimate). Remove only Flyway Surefire classpath exclusions: they now hide required runtime classes and no longer define safe bootstrap. Safety moves to profile-gated beans plus retained default context test.
- CI JAR firewall must positively require exactly one approved version of jOOQ, HikariCP, PostgreSQL, Flyway core/PostgreSQL plugin and Spring JDBC/transaction jars, and still reject Spring Security/JPA/web servers, Testcontainers/JUnit/mock tooling and unapproved DB frameworks/codegen in product JAR. Compare coordinates/versions against resolved inventory, not wildcard acceptance of all Flyway/jOOQ jars. Other Boot-managed support/transitive jars are not domain authorization changes.
- Keep default fat-JAR smoke non-web/database-free and existing contract-regression job unchanged. No profile-specific DB credentials in workflow YAML, no credentials printed, no weakened missing-report/skip guards.
- V001–V009 bytes, checksums, role definitions, schema/ACL inventory and existing test cases remain untouched. No V010, `GRANT`, RLS, security-definer wrapper or listing index in product code. Test-only login membership is not a migration/provisioning implementation.

## 8. Single verifiable behavioral implementation slice

**Slice PA10-S1: creator-context listing executes under the real runtime boundary.** This one slice owns the paths in §4 and narrow edits in §7 plus the documentation paths in §10. No other implementation slice or future service is planned.

Prerequisites: accepted-plan entry from controller; pinned JDK/Maven; dependency resolution/effective-tree evidence; usable Docker PostgreSQL 16. On unavailable environment/dependency facts stop with evidence, rather than alter contracts or skip checks.

Exact increment:

1. Introduce explicit libraries/profile wiring and replace the obsolete PA0 absent-library assertions while preserving database-free default behavior and unrelated bans.
2. Implement complete trusted personal **and** organization creator listing, lifecycle checks/locks, internal typed cursor, eleven-field projection and transaction-local role in one call path. No intermediate unprotected repository behavior is a completed increment.
3. Exercise it through a Spring context built with `persistence` and the actual query service, not copied SQL or a mocked DSL. Verify separate Flyway and restricted query identities; add the ten integration methods below and eight unit/two configuration/one boundary methods. Update exact CI/Failsafe/Surefire source/XML inventories and README/control statements together.

Stop conditions: account predicate absent in any query; any cross-account result; expanded module/ACL/schema; a secret-bearing field/row escaping; role fallback or superuser product query; incomplete lifecycle check; unresolved dependency compatibility; non-deterministic paging; skipped/missing/extra test methods; weakening existing 64 regressions; need for production provisioning/auth/policy/wire input. Report blocker rather than invent a policy. Completion requires the full validation below, not merely compile or author review.

### PostgreSQL fixtures and identities

Create an independent `GenericContainer` for `postgres:16`, matching existing Testcontainers 2.0.5 manual lifecycle/no-skip practice. Random password per JVM, no persisted credentials. A migration/admin connection applies V001–V009 with existing Flyway locations, and provisions **only in the test fixture** a distinct random LOGIN `NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT` with permission to SET `sharing_role`, no owner memberships or direct object grants. Query Hikari uses this login. Prove `session_user` is this login, effective `current_user` is sharing_role, both non-superuser; no app query uses admin. Negative login has no sharing membership. These fixture grants do not change frozen schema ACLs or implement Q17 deployment provisioning.

Fresh container/test-owned data: accounts A/B/disabled; organizations O1/O2/disabled/suspended; active/suspended/revoked/replacement memberships with all four existing roles; valid frozen parent subject/call/evidence chain and SOLIDIFIED evidence/artifact prerequisites using existing V008/V009 fixture shapes as reference, not copied application services. Insert grants with account/org combinations, tied microsecond timestamps, lexical IDs (e.g. `s-2`, `s-10`, mixed case) and every stored status/mode/max-admissions nullability. IDs/salts/digests are synthetic schema witnesses, not policy values. Populate distinctive non-result secrets/hashes/snapshot canaries to catch projection escape without adding illegal schema fields. No trigger-disable shortcut for query correctness. Reuse committed fixture baselines only as safe read data; per-test changes roll back/restore, or use fresh IDs. Teardown drops the independent container, so no historical immutable-trigger cleanup refactor is needed.

## 9. Exact tests, method sets and validation commands

No parameterized/repeated/dynamic tests in new suites: one named `@Test` produces one XML testcase. All source, CI literal expected method sets and XML names/cardinalities must match; zero disabled/skipped/failures/errors. Preserve existing historical 64 literal names in CI unchanged.

### Surefire: 21 methods = existing 10 + 11 new

Existing module class: retain all eight names and assertions. Existing `OneCallApplicationTests.contextLoadsWithoutDatabaseOrSecurityAutoConfiguration`: retain name, add absence of DSLContext/transaction manager/query service/pools alongside DataSource/Flyway/security. Existing bootstrap dependency class: one renamed replacement method specified in §7, still one test.

`CustomerShareQueryServiceTests` — exactly eight:

1. `defaultsAndBoundsAreDeterministic` — default 20, bounds 1/100, invalid 0/101/negative; fetch limit+1 and invalid inputs before repository access.
2. `trustedContextCannotBeOverridden` — no alternate owner/role/filter path; exact caller is forwarded; null context/preconditions sanitized.
3. `cursorBindingAndAnchorValidationAreDeterministic` — account/org/personal mismatch, missing/invalid anchor/precision/ID shape; CURSOR_INVALID, no query, no raw input text.
4. `lookaheadUsesLastReturnedRow` — trim 21/101 rows; next cursor from returned tail, not lookahead, immutable copied page.
5. `emptyAndTailPagesHaveNoCursor` — zero/exact limit/under-limit returns false and no cursor.
6. `projectionComponentsMatchFrozenListing` — exactly eleven component names/types/enums, nullable max and no secret/snapshot/entity/map.
7. `creatorContextDenialDoesNotFetchShares` — repository lifecycle denial stops fetch, returns only FORBIDDEN.
8. `technicalFailuresReturnNoPartialPageOrSensitiveText` — connection/role/timeouts versus mapping/invariant codes, sanitized exception messages; transaction rollback and no fallback.

`SharingPersistenceConfigurationTests` — exactly two:

1. `defaultProfileCreatesNoPersistenceBeans` — libraries present but no DB/service beans and no attempted DB connection.
2. `persistenceRequiresExplicitAndSeparateCredentials` — missing runtime properties fail before connecting; migration enabled without complete distinct credentials fails; migration disabled requires none. No H2/generated password fallback.

`SharingQueryBoundaryTests` — exactly one: `sharingQueryApiAndProjectionAreExact`. Assert service declares only the listing business method; repository's two declared methods exactly; context/request/cursor/page/summary component sets exactly as §4/6; no transport/security selector fields, generic return containers or forbidden internal/external dependencies; exact eleven selected grant-field names and permitted lifecycle reads, no wildcard/raw row/snapshot. Existing Modulith internal-access negative fixture still fails as expected.

### Failsafe: 74 methods = historical 64 + new 10

Add explicit `**/SharingQueryServiceIT.java` alongside existing `**/PostgresContainerIT.java` in `container-smoke` includes; retain fail-if-no-tests and no Docker skip. New suite exactly:

1. `runtimeStartsWithSeparateMigrationAndQueryCredentials` — V001–V009 successful Flyway history, actual Spring/Hikari/DSL/template beans, service runs, query session non-superuser/different migration user, correct effective role and one read-only transaction; repeat startup migration validates rather than duplicates.
2. `personalListingIsCreatorScoped` — A personal only; excludes A organization, B personal/org, even when A can access same evidence/workspaces.
3. `organizationListingIsCreatorAndWorkspaceScoped` — A/O1 only, not B/O1, A/O2 or personal; exercise all four ACTIVE membership roles without role-based expansion.
4. `inactiveAndMissingCreatorContextsFailClosed` — missing/disabled account; missing/nonactive org; absent/suspended/revoked membership; replacement ACTIVE membership succeeds; personal requires no organization. FORBIDDEN, no page.
5. `keysetTiesAndConcurrentInsertRemainStable` — same timestamps/microseconds and database-native lexical order; concatenated traversal equals expected scope order with no duplicates/gaps for baseline rows; insert newer and tied-above-anchor rows between calls, ensure no replay. Older/below-anchor inserts may appear; do not promise snapshot isolation across pages.
6. `pageLimitsEmptyAndTailStatesAreExact` — default/1/100, 101 lookahead, zero rows, exact boundary/tail; no total/offset; no cursor when no more.
7. `invalidAndCrossContextCursorsNeverWidenScope` — account/org/personal reuse and malformed internal anchors fail deterministically; non-existent but well-formed same-scope anchor follows strict predicate; bound unusual legal text remains data, not SQL.
8. `listingProjectionNeverReadsOrReturnsSecrets` — query listener captures selected field metadata/statement structure **without bind logging**; exactly eleven grant columns; all four modes/statuses and max null/value mapped; canary secret/snapshot/hash values absent from results, exceptions and captured application logs; no evidence/event query; lifecycle/grant/history/count/revision/audit/Outbox before/after unchanged.
9. `runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure` — login without membership denied, forbidden foreign read/write/schema/row-lock operations yield 42501 in dedicated probes; writes on service transaction fail 25006; rollback and success release SET LOCAL, same pool connection reused safely; effective role and transaction state verified. Do not assert sharing_role lacks its legitimate V009 INSERT rights globally.
10. `lifecycleChecksSerializeAndTechnicalFailureRollsBack` — bounded two-connection advisory-lock witness (ranks 7/8/9), owner status/membership update commits before service re-read -> denial; rollback -> allowed old state. Observe pg_stat_activity lock wait, statement/future bounds, not sleeps. Force SQL failure, prove no partial result, all locks/role released and subsequent listing succeeds. Failure during optional Flyway startup prevents service creation; unavailable DB/credentials do not fall back.

### CI exact inventory algorithm

Keep the original 64-method expected literal set and exact count/source/XML checks for `PostgresContainerIT`. Add a second independent literal ten-name set for `SharingQueryServiceIT`; require exactly two Failsafe reports, individual 64/10 and aggregate 74, exact class/name pairs and zero skip/error/failure. Scan **all** container source `@Test` methods and fail on any class outside these two; no extra report ignored by a narrow glob. Check Surefire per-class exact expected literal sets (8+1+1+8+2+1), not only aggregate 21; compare source names and XML class/name pairs and forbid extra/missing reports. Preserve separate source/report cardinality checks so duplicate methods/testcases cannot hide behind set equality. No `-DskipTests`, `@Disabled`, conditional Docker disable or accepted empty suite.

### Commands and expected outcomes (implementation validation, from `backend/`)

```bash
export JAVA_HOME=/path/to/temurin-21.0.13+9
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -B -ntp -v
./mvnw -B -ntp help:effective-pom -Doutput=target/pa10-effective-pom.xml
./mvnw -B -ntp dependency:tree -Dverbose \
  -Dincludes=org.springframework.boot:*,org.springframework:*,org.jooq:*,com.zaxxer:*,org.flywaydb:*,org.postgresql:*,org.testcontainers:*
./mvnw -B -ntp validate
./mvnw -B -ntp -Dtest=CustomerShareQueryServiceTests,SharingPersistenceConfigurationTests,SharingQueryBoundaryTests test
./mvnw -B -ntp verify
docker info
./mvnw -B -ntp -Pcontainer-smoke verify
timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
```

Expected: targeted new Surefire 11, full default Surefire 21/Failsafe 0, full container profile Surefire 21/Failsafe 74; every test zero failure/error/skip. Effective POM/tree confirms §5 scopes and §3 versions, no conflicts, no unapproved SQL starters/Boot SQL auto-config modules. The default JAR exits normally within timeout, no DB required. Updated CI report validator and exact JAR positive/negative firewall must pass. Real runtime-profile startup/listing/migration failure is proven in IT, not a hanging shell smoke requiring credentials. Run the unchanged `contract-regressions` job commands (`npm ci`, `npm run check:s3`, A2/A3/A4/A5 validators) exactly as current CI; expected exit 0, no fixture/contract edits. Retain command exit codes/report paths and actual method sets in the completion report, distinguish local from remote CI. If network/Docker unavailable, report incomplete validation; never turn that into successful skipped tests.

## 10. Documentation decision

Implementation-owned documentation changes are limited to:

- `backend/README.md`: current capability summary, explicit compile/runtime/test scope table, `persistence` profile/credential separation and non-web default, internal call contract, commands and exact 21/74 inventory/firewall transition. Keep historical PA1–PA9 evidence labeled historical; do not rewrite its outcomes. Say listing only, no HTTP/auth/mutation/playback/readiness.
- `docs/README.md`: one link/short capability entry for PA10, not a restatement of the production designs or acceptance matrix.
- `docs/gateflow/project-execution-control.md`: controller-owned active PA10 capability/validation status as authorized during implementation handoff; preserve old checkpoints and do not self-advance gate or mark accepted. Supply proposed factual update to controller if direct control editing is not authorized.

No OpenAPI/design/schema/security model, migration, historical plan/completion artifact or acceptance fixture needs modification. This planning gate writes only this plan; implementation evidence/report destinations are controller-directed, not permission to create extra artifacts now.

## 11. Residual risks, owner and destination (not extra goals)

| Residual boundary | Owner / destination | PA10 treatment |
| --- | --- | --- |
| Trusted context must eventually come from real customer authentication and API boundary | Identity/Backend/Security; authentication design and customer API integration | Internal-only type/callers, no transport factory/session/auth claim. |
| Opaque wire cursor encoding/integrity, 512-character cap and normalized filter serialization (Q15) | Backend/API/Security; existing OpenAPI cursor/API integration ownership | Typed internal anchors/binding only; no codec, keys or wire claims. |
| Organization resource/sharing role policy | Product/Security/Sharing; approved sharing authorization policy | Active membership plus **same creator** only; no new matrix or evidence access/playback power. |
| Q17/Q23 migration login, role membership, ownership transfer and function execution provisioning | Platform/Database/Security; deployment/security design and readiness | Separate runtime/config boundaries, fixture-only provisioning; actual production identity deliberately unspecified. |
| Secret-free projection beyond this in-process service (edge/log collector/serializer) | Sharing/Backend owns this exact projection/test; Security/Observability/API own downstream egress verification (`R-A3-REDACTION-RUNTIME`) | Allowlisted SQL/result and local log canaries prove bounded service behavior, not all production egress. |
| Deferred mutation/admission/media contracts, canonical creation snapshot and KDF/TTL inputs Q12/Q22 | Sharing/Evidence/Product/Security; existing sharing/security design ownership | No implementation, slice, dates or policy values; stored facts only. |
| No creator/order covering index; cross-page reads are not a frozen snapshot | Backend/Database; runtime evidence/capacity review | Bounded 101-row output and deterministic keyset, no invented migration/performance guarantee; newer inserts above anchor omitted until restart. |
| Library resolution and real PostgreSQL behavior not fully executable here | Backend/CI owner; implementation validation evidence | Local BOM compatibility verified; startup/tree and all real DB tests remain mandatory; environment limitations are not waived criteria. |

## 12. Completion report format and boundedness

Implementation handoff report must state: gate/slice/base/HEAD; allowed changed paths and no forbidden changes; mapping to the eight goal signals in §2; exact resolved dependency/scopes and profile/role/credential evidence; exact service/repository/input/result method/component sets; actual SQL projection/context/order/cursor proof; Surefire and Failsafe source/CI/XML sets and counts with zero skip/failure/error; commands, exit codes and report locations; schema/ACL/checksum/module/contract/firewall preservation; docs changed; unresolved risks/blockers and named owners. Separate actual local results from remote CI/controller acceptance; do not claim production readiness or complete deferred auth/API/policy/provisioning/mutations.

This plan is intentionally not layered into infrastructure/repository/service deliveries. One specific query port, one adapter, one service and profile-scoped configuration are sufficient. It adds no reusable security platform, owner collaboration, serializer, state machine, event pipeline or database change. Every listed file/test/guard/doc change either makes creator listing executable, proves its stated boundary, or removes an exactly conflicting PA0 assumption while retaining its purpose. Residual ownership is disclosure, not a future-work roadmap. No future sharing service is implemented or planned by this work unit.
