# PA15 Public Share Read — Bounded Implementation Plan

- Task: `pa15-public-share-read-plan-gpt-20261106`.
- Work unit: `PA15-sharing-public-share-read`; Gateflow gate: `plan`.
- Author role: GPT `gpt-6.1-sol` / high plan author; date: 2026-11-06.
- Baseline authority: accepted PA15 Goal Confirmation in `project-execution-control.md`, merged PA14 at `b93d39052195f530518a7351b27ba4953fdfa5fe`.
- Deliverable now: this plan only. Implementation, gate advancement and plan review are not authorized by its creation.

## 1. Problem, Motivation And Success

A capability URL must let its holder inspect the frozen, safe description of exactly one shared evidence scope without turning an observation into an authorization or a billable/countable admission. Possession is not customer identity. A customer identity is not organization membership, and membership in one tenant is not membership in the grant tenant. Returning an existing persistence entity would leak identifiers and secrets. Reusing the customer-list transaction cannot resolve the token: V010 deliberately denies its role access to `capability_token_hash` and `content_snapshot`.

Implement exactly one externally verifiable behavior: `GET /api/v1/public/shares/{capabilityToken}`. A successful request yields the closed `PublicShareView`; organization-restricted content is omitted unless a trusted current member matches the grant tenant. All outcomes, including prefetch and observed expiry, leave persistent and ephemeral authorization state unchanged. Existing create/revoke/rotate/list/`me` behavior remains unchanged.

Success requires executable evidence of exact routing and precedence, exact-byte digest lookup, uniform unresolved-token concealment, 410 terminal/time denials, safe immutable projection, current membership gating, fail-closed dependencies, and no writes. Source, CI expected methods and fresh XML must agree at **91 Surefire + 126 Failsafe = 217** tests, all green with zero skips. These are future acceptance criteria, not execution results of this planning task.

### Explicit Non-Goals

No admissions endpoint or admission key, count increment, admission/session/ticket issuance, ticket policy, extraction/mobile verification, failed-verification policy, lock/unlock/expiry mutation, Redis/cache/media state, playback/seek/renewal, object storage/COS/KMS/SMS, frontend/public UI, generic gateway, future endpoint scaffolding, production token generator/format/entropy implementation, TTL/KDF/rate-limit values, migration/OpenAPI edits, new dependencies, credentials or deployment provisioning. No Git staging/commit/push/branch creation, PR/reviewer/comment/issue mutation, merge, approval, ready action or plan-review dispatch.

## 2. Direct Evidence And Contract Precedence

Required authorities were examined through their EOF, with PA15 and the following applicable sections controlling this slice:

| Authority | Applicable evidence and consequence |
| --- | --- |
| `docs/gateflow/project-execution-control.md`, active 2026-11-06 checkpoint | One observation-free public GET, optional member session, no side effects, protected write boundary; PA14 is complete. Historical checkpoints do not widen PA15. |
| `api/openapi/onecall-v1.openapi.yaml:3621` | `getPublicShare`: anonymous OR customer-session security; 1–512 token; exact metadata-only member fallback; operation-specific domain codes are only TOKEN_INVALID/REVOKED/EXPIRED. |
| Same file, `PublicShareView` at line 7020, security schemes at 3994, Problem at 4242, and union/discriminator definitions through EOF | Root/content/artifact/challenge are closed objects. Content is optional, not nullable. Admission verification is a closed discriminated union belonging to a different operation, not an input to this GET. |
| `docs/12-technology-stack-and-system-architecture.md`, §§7–8 and sharing/media boundaries | Distinct trust surfaces, server-trusted account/workspace, capability URL rather than object URL, sanitized Problems, UTC times and private media. |
| `docs/14-production-data-model-and-flyway-schema-spec.md`, §§2.4–2.6/2.12, 4.41–4.46, 7.4, 9.0–9.0.2, 9.29–9.31, 10.21/10.23/10.24 | Immutable grant/content and evidence facts, separate lifecycle/admission/media operations, role boundaries, captured identity, artifact ordering and redaction. |
| V009 and V010 complete migrations | V009 immutable capability hash/scope; unique digest; terminal states and generation/revision guards. V010 list role is column-limited; command role can read grant digest/snapshot; evidence-owner reader needs `evidence_role`. No ACL/schema change is permissible. |
| Accepted `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`, subsequent revoke/rotation plans and implemented Java/tests | Regression authority: exact create/replay/revoke/rotation facts, separate command pool, query role isolation, snapshot `content`/`verification`/`creation` objects, token digest over exact UTF-8 bytes, artifact rank and return-once secrets. |
| `docs/15-production-openapi-surface-spec.md`, public-share table and operation error-set clarification | Explicitly confirms that GET is observation-free and does not have admission-only risk/wrong-tenant/locked/generation errors. |

Resolve older general S3 prose that records PAGE_OPENED/PREFETCH_IGNORED or persists observed expiry in favor of the later frozen **operation-specific GET** and active Goal Confirmation. No documentation/schema edit is needed to make that precedence effective. In particular, §9.30's admission lock/write/TTL rules and the admission verification discriminator must not be implemented as page-read prerequisites.

### Existing Code That Determines The Design

- `customer_iam/internal/web/CustomerBoundaryFilter.java`: one REQUEST-dispatch `/*` filter, highest precedence; canonical URI equality; whitelist before authentication; Bearer before content negotiation; buffered output; request-local trace. Today it rejects every public path.
- `CustomerBearerAuthenticator`: strict single Authorization header, customer-only audience, exactly one verifier, typed 401 versus unavailable 503. It carries an attested workspace but does not prove current membership. There is no production verifier implementation.
- `CustomerContextService.getCustomerMe` and `JooqCustomerContextRepository.findActiveContext`: trusted caller only; ACTIVE account; current ACTIVE organization/membership rows; separate customer role/read transaction; missing/disabled account is UNAUTHENTICATED. These APIs suffice for member verification without sharing reading IAM tables.
- `CustomerShareQueryService`/`ShareQueryRepository`: exact creator-list API; read-only `sharing_role`; no digest/snapshot entitlement. Preserve these APIs, projections, pool and tests.
- `SharingCommandPersistenceConfiguration`: opt-in `persistence` + `onecall.sharing-command.enabled=true`, explicit existing command credentials, separate datasource/DSL/manager, indexed prepared statements and disabled SQL logging, existing SHA-256 provider and fail-closed unapproved production providers.
- `ShareableEvidenceProjectionReader.readShareableProjection`: evidence-owner named interface; typed Found/Missing/Unavailable; MANDATORY `sharingCommandTransactionManager`; caller-provided transaction DSL; immutable source/identity/artifact projection without media-object identity. Implementation reads evidence/media tables **inside evidence**, not sharing, and sorts ORIGINAL before DERIVED.
- PA14 `DefaultCustomerShareCommandService.content`: `schemaVersion:1`, evidence/subject/source references, `identityAtCapture`, artifact metadata with nullable validated duration; capture time already exists at `identityAtCapture.capturedAt`. No new capture-time column/provider is needed.
- Architecture tests enforce exact package/file inventories and module dependencies. They must be extended precisely, not weakened globally. CI explicitly includes IT classes in POM and rejects unexpected test source classes, duplicate methods/cases and conditional tests.

## 3. One Behavior Slice And Closed Future Allowlist

There is **one slice**: canonical HTTP request → optional trusted customer authentication → digest-resolved read → current member check if applicable → immutable safe projection → closed JSON/Problem, with PostgreSQL and regression evidence. The following is a closed **future implementation** allowlist, not permission to edit those paths in this plan gate. Paths are repository-relative; no glob grants or optional additional files are implied.

| # | Path | Action and necessity |
| --- | --- | --- |
| 1 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | Modify only public-GET whitelist/optional authentication branch; retain the single filter and every customer branch/order. |
| 2 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java` | Add typed public-query overload and TOKEN_INVALID/EXPIRED status/title rows; preserve existing rows and closed envelope. |
| 3 | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/PublicSharesController.java` | Add one web-profile servlet controller and current-member adapter; no persistence access. |
| 4 | `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` | Add only `sharing::public-query` to allowed dependencies. |
| 5 | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/package-info.java` | Add named interface `public-query`, distinct from frozen customer-list/command APIs. |
| 6 | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/OpaqueCapabilityToken.java` | Add final redacted ephemeral token wrapper; exact UTF-8 bytes, no record-generated secret `toString`. |
| 7 | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryException.java` | Add stackless code-only errors, restricted to this operation's frozen codes. |
| 8 | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareView.java` | Add closed safe response records/enums and NON_NULL omission, nested content/artifact/challenge types only. |
| 9 | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java` | Add one query entry point; local transaction orchestration, state/time checks, lazy member seam, snapshot/evidence consistency and safe field projection. |
| 10 | `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicShareQueryRepository.java` | Add narrow read interface and nested internal grant row; no mutation method. |
| 11 | `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqPublicShareQueryRepository.java` | Add exact prepared digest lookup, DB time/role/read-only assertions; selects sharing data only. |
| 12 | `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicSharePersistenceConfiguration.java` | Wire public read repository/service/template against existing command DSL/manager, never list credentials; no new pool/properties/secrets. |
| 13 | `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java` | Add exactly six HTTP/closed DTO/boundary tests below. |
| 14 | `backend/src/test/java/com/onecall/archtests/sharing/PublicShareQueryServiceTests.java` | Add exactly four deterministic service tests below. |
| 15 | `backend/src/test/java/com/onecall/archtests/PublicSharePersistenceConfigurationTests.java` | Add exactly two profile/qualifier/read-only configuration tests below. |
| 16 | `backend/src/test/java/com/onecall/archtests/container/PublicSharesFixture.java` | Add package-local test fixture: compose existing PostgreSQL/Flyway/customer fixtures and deterministic providers; seed complete immutable grants/evidence; HTTP and SQL observations. No production substitute. |
| 17 | `backend/src/test/java/com/onecall/archtests/container/PublicSharesIT.java` | Add exactly eight PostgreSQL-backed HTTP/TX tests below. |
| 18 | `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java` | Extend existing methods for one new controller (25→26 customer internal files), exact sharing imports/profile/signatures; retain old restrictions elsewhere. |
| 19 | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` | Add explicit PUBLIC_READ_FILES third classification and named API inventory; preserve exact original customer-query and command sets and bans. |
| 20 | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | Add `sharing::public-query` to EXPECTED_ALLOWED; in both existing production-boundary methods change allowed-interface count2→3 and exact named-interface/dependency lists to customer-query/customer-command/public-query. Retain 14 modules, two module-level edges and cycle/internal negative fixtures. |
| 21 | `backend/pom.xml` | Add only explicit `PublicSharesIT.java` container-smoke include. No dependency/plugin/profile/version change. |
| 22 | `.github/workflows/ci.yml` | Add exact four class/method inventories, totals and bounded public packaged-JAR smoke probes; preserve firewall/protected checks. |
| 23 | `backend/README.md` | Update current test totals/breakdowns and bounded read behavior/command-profile dependency; preserve historical evidence. |

No implementation-plan bookkeeping file is implicitly allowed. If a necessary change falls outside these 23 paths, stop and obtain a bounded plan correction rather than silently widening scope.

### Deliberately Excluded Because Current APIs Suffice

Do not edit customer authenticator/verifier/session/principal/context/repository/persistence APIs, web configuration/error controller/settings, existing customer controllers/query service/repository/DTO/cursor code, existing command service/repository/providers/persistence configuration/properties, evidence projection interface/record/reader/configuration, sharing/evidence module declarations, existing fixture/provider helpers, existing behavioral test method names/counts, migrations V001–V010, OpenAPI/generated API, dependencies or deployment files. Reuse their APIs; no generic public filter framework or new IAM/evidence provider is needed. The three existing architecture files are the only existing test files requiring changes.

The main write boundary for this task remains exactly `docs/gateflow/pa15-sharing-public-share-read-plan.md`; the future implementation allowlist does not include editing this plan or the Control document.

### Frozen Architecture Inventory And Bans (Items 18–20)

Verified against the current `SharingQueryBoundaryTests`, `CustomerApiBoundaryTests` and `ModuleBoundaryTests` sources. Extend their existing methods only; do not add/rename methods, weaken old restrictions or change counts. The three sharing file partitions are disjoint and exhaustive. All paths below are repository-relative to `backend/src/main/java/com/onecall/core/sharing/internal/`.

`PUBLIC_READ_FILES` is exactly these eight paths:

```text
publicquery/package-info.java
publicquery/OpaqueCapabilityToken.java
publicquery/PublicShareQueryException.java
publicquery/PublicShareView.java
publicquery/PublicShareQueryService.java
persistence/PublicShareQueryRepository.java
persistence/JooqPublicShareQueryRepository.java
persistence/PublicSharePersistenceConfiguration.java
```

Preserve the existing exact 13 `QUERY_FILES` entries and all their current bans:

```text
application/CustomerShareQueryService.java
application/TrustedShareCaller.java
application/SharePageRequest.java
application/ShareCursor.java
application/ShareSummary.java
application/SharePage.java
application/ShareQueryException.java
application/ShareQueryDiagnostic.java
application/package-info.java
persistence/ShareQueryRepository.java
persistence/JooqShareQueryRepository.java
persistence/SharingPersistenceConfiguration.java
persistence/SharingPersistenceProperties.java
```

Command-path discovery excludes **both** `QUERY_FILES` and `PUBLIC_READ_FILES`; its remaining exact 19 entries and all existing command bans stay unchanged:

```text
command/package-info.java
command/CustomerShareCommand.java
command/CustomerShareCommandResult.java
command/CustomerShareCommandService.java
command/ShareCommandCaller.java
command/ShareCommandException.java
commandservice/DefaultCustomerShareCommandService.java
persistence/SharingCommandRepository.java
persistence/JooqSharingCommandRepository.java
persistence/SharingCommandPersistenceConfiguration.java
persistence/SharingCommandPersistenceProperties.java
provider/ProviderResult.java
provider/ShareCreationPolicyProvider.java
provider/CapabilityTokenProvider.java
provider/ShareExtractionCodeProvider.java
provider/DesignatedMobileBindingProvider.java
provider/Sha256Provider.java
provider/ExtractionCodeMaterial.java
provider/ShareRotationPolicyProvider.java
```

The named interfaces become exactly `customer-query`, `customer-command`, `public-query`. Freeze the public-query top-level simple names as exactly `OpaqueCapabilityToken`, `PublicShareQueryException`, `PublicShareQueryService`, `PublicShareView`. Freeze nested API types as `PublicShareQueryException.Code`, `PublicShareQueryService.MemberAccess`, `PublicShareQueryService.MemberMatch`, and `PublicShareView.Content`, `.Artifact`, `.VerificationChallenge`, `.VerificationMode`, `.Status`, `.ArtifactRole`; no internal persistence row is exported. Preserve customer-query's exact top-level inventory `CustomerShareQueryService`, `TrustedShareCaller`, `SharePageRequest`, `ShareCursor`, `SharePage`, `ShareSummary`, `ShareQueryException`, `ShareQueryDiagnostic`, its nested `VerificationMode`, `ShareStatus`, `Code`, and customer-command's exact top-level inventory `CustomerShareCommandService`, `CustomerShareCommand`, `CustomerShareCommandResult`, `ShareCommandCaller`, `ShareCommandException`, with all existing nested types/signature/component assertions unchanged.

Freeze source assertions by partition (SQL checks cover case-insensitive SQL operation tokens and jOOQ write/lock calls, not merely one spelling):

- All five `publicquery/` files: no `java.sql`/`javax.sql`/`DataSource`/Hikari, INSERT/UPDATE/DELETE/MERGE/DDL (including TRUNCATE/CREATE/ALTER/DROP), writable CTE or write function, `forUpdate`/`forShare`/`FOR UPDATE`/`FOR SHARE`/`pg_advisory` or other row/advisory lock, audit/Outbox/access-event append, command service/repository invocation (`CustomerShareCommandService`, `DefaultCustomerShareCommandService`, `SharingCommandRepository`, `JooqSharingCommandRepository`), `com.onecall.core.customer_iam` or `com.onecall.core.organization_access` imports, `SELECT *`/`selectFrom(`, `capability_token_hash` or `content_snapshot`. No secret logging; enforce `Logger`, `System.out`, `System.err`, `printStackTrace` bans. TransactionTemplate orchestration and the existing evidence-owner API are allowed; no foreign-table SQL or direct SQL except the service's exact §5 read-only/current-role setup/assertion calls below.
- Only `publicquery/PublicShareQueryService.java` may import/use `org.jooq.DSLContext`, solely as the active transaction context constructor-injected with qualifier `sharingCommandDsl`, passed only to the existing MANDATORY evidence-owner reader, and used for §5's exact `SET TRANSACTION READ ONLY`, `current_setting('transaction_read_only')`, `SET LOCAL ROLE sharing_command_role`/`evidence_role` and `current_user` setup/assertion calls, including the required role restore. Repository calls take no DSL argument; the repository implementation is constructor-wired with that same qualified DSL. No other jOOQ type/use is allowed in `publicquery/`. Freeze a negative architecture assertion in the existing `SharingQueryBoundaryTests` method: `publicquery/package-info.java`, `publicquery/OpaqueCapabilityToken.java`, `publicquery/PublicShareQueryException.java` and `publicquery/PublicShareView.java` must remain free of `org.jooq`/`DSLContext`; adding any jOOQ import/use to any of those four fails. The service exception permits no grant/database-time SELECT or relaxation of the all-five bans above.
- All three public-read `persistence/` files: no INSERT/UPDATE/DELETE/MERGE/DDL (including TRUNCATE/CREATE/ALTER/DROP), writable CTE or write function, `forUpdate`/`forShare`/`FOR UPDATE`/`FOR SHARE`/`pg_advisory` or other row/advisory lock, audit/Outbox/access-event append, command service/repository (`CustomerShareCommandService`, `DefaultCustomerShareCommandService`, `SharingCommandRepository`, `JooqSharingCommandRepository`), cross-module direct SQL or logging (`Logger`, `System.out`, `System.err`, `printStackTrace`, SQL/bind listeners). Ban `SELECT *`/`selectFrom(` and IAM/organization imports here too. Fixed SELECTs and §5's read-only/role setup/assertions are the only SQL allowances; existing command DSL/manager bean qualifiers are not command invocations.
- `capability_token_hash` and `content_snapshot` are allowed **only** in `JooqPublicShareQueryRepository.java` within `PUBLIC_READ_FILES`, because §5's fixed prepared SELECT needs the digest predicate and snapshot column. Ban both strings in every other public-read file, including its repository interface/configuration. This exception does not relax any customer-query or command restriction or permit selecting/logging the stored digest. Internal typed `contentSnapshot` projection/orchestration remains allowed.

In `CustomerApiBoundaryTests.customerDependenciesAndReadOnlySqlStayInsideBoundary`, change the exact customer_iam internal file count 25→26 and add only `PublicSharesController.java` to the sharing-import exception list. The writer's exact sharing imports become `import com.onecall.core.sharing.internal.command.ShareCommandException;` plus `import com.onecall.core.sharing.internal.publicquery.PublicShareQueryException;`, no others. `PublicSharesController` may import only the four named public-query API types (or their enumerated nested types) from sharing; forbid repository/DSL/DataSource/command/internal-row access and retain the existing controller reflection/lookup/attribute bypass bans. Keep all original restrictions on other files and exact existing customer controller imports/signatures. Extend `customerApiSignaturesAndProjectionAreExact` with §4's exact public GET mapping, `PublicShareView` return type, method signature and constructor provider roles; behavior tests, not architecture tests, own runtime fail-closed cases.

In `profileAndVerifierArtifactsCannotEscapeIntoProduction`, the exact web/servlet-conditional list grows from four to five: `web/CustomerWebConfiguration.java`, `web/CustomerMeController.java`, `web/CustomerSharesController.java`, `web/CustomerApiErrorController.java`, `web/PublicSharesController.java`, each retaining `@Profile("web")` and `ConditionalOnWebApplication.Type.SERVLET`; all production verifier/test-profile restrictions remain. `ModuleBoundaryTests` retains its exact module map except the single `sharing::public-query` addition to customer_iam, both existing named-interface/allowed-dependency checks become exactly three, and all 14-module/two-edge/negative-fixture assertions remain as item 20 requires.

## 4. Exact HTTP Boundary And Precedence

Keep `CustomerWebConfiguration` and its REQUEST-only, highest-precedence registration unchanged. Extend the **existing** filter; do not register a competing filter or rely on MVC's implicit HEAD/OPTIONS mappings.

1. Allocate a server UUID trace and set `Cache-Control: no-store`, `Pragma: no-cache`, `X-Content-Type-Options: nosniff`, as today. Never trust inbound trace/account/organization/workspace headers, servlet principal or request attributes as identity.
2. Route before any credential parsing, token hashing, service lookup or database access. Retain `(contextPath + servletPath).equals(requestURI)`. Admit only exact uppercase `GET` and servlet path matching `/api/v1/public/shares/[^/;%]+`. Preserve every existing customer GET/create/revoke/rotate matcher unchanged. Query strings do not participate in route identity.
3. Unknown methods/routes, HEAD/OPTIONS/POST, trailing slash, extra segment (including `/admissions`), matrix/semicolon, percent-encoded aliases/token segments, encoded slash/double-encoding, duplicate slash, case/alternate prefix and noncanonical context URI retain sanitized 404 `NOT_FOUND`, before verifier/service calls. Empty/missing **path segment** is not a new route and retains this rule; a missing **digest resolution** for a presented token is SHARE_TOKEN_INVALID. This distinction avoids inventing a collection/empty-token endpoint.
4. For this public GET only, absence of Authorization means anonymous: never call the verifier, never create a principal from container/client data, and never demand customer persistence merely to return nonmember metadata. If any Authorization header is present, use the unchanged strict `CustomerBearerAuthenticator`; empty/malformed/duplicate/rejected/expired/revoked/wrong-audience credentials yield 401 UNAUTHENTICATED, missing/ambiguous/unavailable verifier or malformed verifier result yields 503 SERVICE_UNAVAILABLE. Do not silently downgrade supplied invalid credentials to anonymous. Install a `CustomerRequestPrincipal` only on successful verification; an anonymous wrapper must mask any incoming principal/remote-user.
5. Retain existing Accept parsing/quality precedence **after authentication**: absent or compatible JSON accepts; incompatible/q=0 or malformed negotiation maps to sanitized INTERNAL/500, not HTML/406. Keep response buffering and all customer Content-Type/Idempotency-Key checks untouched. GET ignores Idempotency-Key, body verification material and client workspace selectors; none becomes authority or triggers mutation.
6. Controller path binding is an opaque String, never UUID parsing. Validate 1–512 Unicode code points, no NUL, well-formed UTF-8 encodability, matching existing PA14 token-material representability. No trim/case-fold/normalization, alphabet, prefix, entropy or UUID rule. Length/representability violation on an otherwise canonical admitted route is 422 VALIDATION_FAILED **before query/provider lookup**. Unresolved-token 404 SHARE_TOKEN_INVALID applies only after a canonical admitted, valid-shaped token (including an altered-case value) fails exact digest resolution. Percent-encoded/URI-encoded whitespace, matrix/semicolon, duplicate-slash or other noncanonical presentations are instead sanitized 404 NOT_FOUND at the existing route gate, before token shape/provider/service lookup; they never reach digest lookup. Test 1/512 acceptance and 513 rejection; direct service tests own constructible whitespace/normalization distinctions and Unicode/invalid-surrogate/NUL cases, without overriding the HTTP route gate.
7. Call the public service through a qualified `ObjectProvider`; missing/ambiguous/unavailable public service is 503. Return only `PublicShareView`. Handle known query/customer errors through the existing writer, unexpected failures as sanitized INTERNAL. No redirects, Set-Cookie, session creation, Location, media/verification hints, URI/detail echo or entity serialization.

`PublicSharesController` is `@Profile("web")`, servlet-conditional, with exactly `@GetMapping(path="/api/v1/public/shares/{capabilityToken}", produces="application/json")` and exactly `public PublicShareView getPublicShare(@PathVariable("capabilityToken") String capabilityToken, Principal principal)`, returning only `PublicShareView`, never Object/ResponseEntity/entity. Freeze constructor injection as `public PublicSharesController(@Qualifier("publicShareQueryService") ObjectProvider<PublicShareQueryService> service, @Qualifier("customerContextService") ObjectProvider<CustomerContextService> customerContexts, CustomerProblemWriter problems)`. Resolve the query service lazily for the request; resolve `customerContexts` lazily only inside the organization-member callback via existing `getCustomerMe(caller)`, never for anonymous/nonmember-mode/invalid/terminal requests. Missing services or provider resolution failure remain fail-closed SERVICE_UNAVAILABLE, never anonymous downgrade or partial content. Its only sharing imports are the public-query named API, never a repository, DSL, command API, internal grant or provider implementation. Controller fields may hold only these two providers and the existing problem writer; no repositories, DSLContext, command service or HTTP/session state.

## 5. Read API, Digest Lookup And Enforced Transaction

### Minimal API Surface

`PublicShareQueryService.getPublicShare(OpaqueCapabilityToken token, MemberAccess memberAccess) -> PublicShareView` is its sole public query operation. `MemberAccess` is a nested operation-specific functional interface `check(UUID grantOrganizationId) -> MemberMatch`; `MemberMatch` is exactly MATCH, NONMATCH, UNAUTHENTICATED, UNAVAILABLE, INTERNAL. It is a lazy bridge, not a session issuer/provider framework. Its implementation is a controller-owned closure over the filter-created trusted caller. No HTTP objects/customer classes cross into sharing.

`PublicShareQueryException.Code` is exactly UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, SHARE_TOKEN_INVALID, SHARE_REVOKED, SHARE_EXPIRED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL. `PublicShareView` uses fixed VerificationMode and Status enums with exactly the OpenAPI values, plus nested Content, Artifact and VerificationChallenge records; expose no internal persistence type. Architecture assertions must enumerate this named interface's four top-level types (service, view, exception and opaque token; package metadata excluded), its nested safe types and sole service operation. Preserve existing customer-query and customer-command named-interface type inventories byte-for-byte; public-query is a third interface, not extra customer-query methods.

Anonymous closure returns NONMATCH without any customer service call. Authenticated closure uses the existing qualified `CustomerContextService.getCustomerMe(caller)` only when service state/mode requires member content. It translates typed context outcomes into the enum; never throw a customer-module type into sharing. A null/throwing/unknown callback outcome fails closed, never MATCH. The callback may execute the existing independent customer read transaction while the public transaction holds no row/advisory locks; there is no distributed transaction or cross-module mutation.

`PublicShareQueryRepository` methods remain exactly `findByTokenDigest(String digest) -> Optional<PublicGrant>` and `sampleDatabaseTime() -> Instant`, plus explicit role/read-only assertion helpers if needed for the fixed orchestration; no repository interface method takes a `DSLContext` parameter. It remains the only abstraction performing the frozen grant/database-time SELECTs, through `JooqPublicShareQueryRepository`, which is constructor-wired with the existing qualified `sharingCommandDsl` backed by `TransactionAwareDataSourceProxy`, so its queries join the public transaction. `PublicShareQueryService` receives that same `@Qualifier("sharingCommandDsl") DSLContext` and `@Qualifier("publicShareQueryTransactionTemplate") TransactionTemplate`, uses the DSL for the frozen read-only/current-role setup/assertions/restore, calls the no-DSL repository interface, and passes the same DSL only to the existing `ShareableEvidenceProjectionReader.readShareableProjection(DSLContext, UUID)` MANDATORY collaborator. No alternate pool, manager or DSL and no new abstraction or path. `PublicGrant` is a nested immutable internal record with exactly `evidenceId`, nullable-as-Optional `ownerOrganizationId`, `verificationMode`, `status`, `validFrom`, `expiresAt`, `downloadAllowed`, `contentSnapshot`. It never escapes the persistence/service implementation or enters diagnostics. Redact its `toString`; no raw JSONB/scope display. No grant ID/share ID/token digest/code/counter/generation columns need be selected.

### Frozen ACL Constraint And Reuse

V010's `sharing_role` cannot read a token predicate or snapshot. Do not widen it, change V010, grant owner privileges, repurpose the list pool or call a command method as a read workaround. Reuse **existing** `sharingCommandDsl` and `sharingCommandTransactionManager`/datasource because that approved ACL principal already has the required grant read and evidence-role transition. Its name does not authorize writes in this operation.

`PublicSharePersistenceConfiguration` uses the existing persistence profile and command-enabled condition. Define `publicShareQueryTransactionTemplate` on that existing manager: REQUIRES_NEW, READ_COMMITTED, `readOnly=true`. Define qualified public repository/service beans using the existing DSL, SHA-256 provider and evidence reader. It creates no pool, property class, credential, policy/generator invocation or alternative fallback. Default/web-only/command-disabled modes contain no public persistence service; canonical page requests fail 503 with no DB acquisition. Missing command credentials retain existing startup validation; missing/malformed public dependencies fail closed.

Inside this public transaction the **first application SQL** is `SET TRANSACTION READ ONLY`, before role/domain reads (a JDBC readOnly hint alone is insufficient). Assert `current_setting('transaction_read_only') = 'on'`; then `SET LOCAL ROLE sharing_command_role` and assert `current_user`. No code may later execute READ WRITE. Keep that role until the bounded evidence-owner call, switch/assert `evidence_role`, and restore/assert `sharing_command_role` in `finally`. Both role and read-only assertions fail as 503 before content can escape. The existing evidence reader's MANDATORY annotation joins the same manager/resource transaction even though this particular template is read-only; leave the command template itself writable and unchanged.

Use the provider's lowercase SHA-256 over the **exact UTF-8 bytes** of the presented token, consistent with PA14. Validate its result as 64 lowercase hex; unavailable/throwing/invalid digest provider is 503, never a false missing token. Do not issue a token, derive a shareId or call a random/token/code/KDF/mobile provider. The token wrapper's string representation is always `[REDACTED]`; do not use a secret-bearing record or exception message.

Exact lookup shape, with prepared binding and explicit names:

```sql
SELECT evidence_id, owner_organization_id, verification_mode, status,
       valid_from, expires_at, download_allowed, content_snapshot
FROM sharing.evidence_share_grant
WHERE capability_token_hash = ?
```

The unique digest constraint supplies cardinality; no SELECT *, shareId/evidenceId lookup fallback, partial matching, JOIN, row lock, advisory lock, write CTE/function or command repository access. Zero rows → SHARE_TOKEN_INVALID uniformly; multiple/undecodable rows → SERVICE_UNAVAILABLE. Do not select or retain the stored digest. A previously returned token for which no live digest resolves is the same denial; there is no invented retirement table or token-rotation operation. Revocation does not retire/remove the persisted digest and therefore remains distinguishable as the frozen 410 after a valid digest match.

Sample `SELECT clock_timestamp()` once after a found row; reuse this database Instant for the page's validity decision. No Java wall clock/TTL, scheduled expiry, retries, status persistence or generation adjustment. Prepared statements/SQL/bind logging remain disabled; do not add logs carrying request URI, token, digest, snapshot, IDs or underlying exception text.

## 6. State, Membership And Dependency Decisions

Order is canonical route → supplied-session authentication → negotiation → token shape → dependencies/digest → found grant → state/time → member gate for ORGANIZATION_MEMBER → evidence/content projection if allowed. Nothing downstream is evaluated to distinguish an unresolved token. Typed credential failures may precede lookup, as on the established customer boundary.

| Condition after digest resolution | GET result and work explicitly excluded |
| --- | --- |
| No row, mistyped/unknown/retired presented token | 404 SHARE_TOKEN_INVALID; identical title/envelope; no evidence/member lookup. |
| Persisted REVOKED, including time past expiry | 410 SHARE_REVOKED first; no evidence/member call or expiry mutation. |
| Persisted EXPIRED | 410 SHARE_EXPIRED regardless of stored future-looking timestamp; no correction. |
| ACTIVE or LOCKED and sampled time >= expiresAt | 410 SHARE_EXPIRED, no status/revision/generation/event write. Equality is expired. |
| ACTIVE or LOCKED but sampled time < validFrom | 404 SHARE_TOKEN_INVALID as not-yet-usable capability after canonical admission/digest resolution; expose neither metadata nor content, perform no persistence mutation or member/evidence call. No invented pending status. |
| ACTIVE or LOCKED within [validFrom, expiresAt) | Page projection; preserve actual ACTIVE/LOCKED status. LOCKED is not SHARE_LOCKED on GET; count exhaustion is not a page error and counters are not read. |
| Unknown/malformed mode/state/time/snapshot metadata or organization-mode row without tenant | 503 SERVICE_UNAVAILABLE; no partial page. V009 guards are not sufficient reason to trust arbitrary decoded JSON. |
| ORGANIZATION_MEMBER, absent session or current nonmatching member/tenant | 200 exactly safe root metadata plus challenge=true, no content key, no evidence/artifact enumeration. |
| ORGANIZATION_MEMBER, valid current matching member | Eligible for the same safe content projection as other modes; never grant creator/admin ownership privileges. |
| Supplied credential verified, but account now missing/DISABLED | Member callback maps existing context UNAUTHENTICATED → 401; no content. Verifier revocation/expiry/wrong audience already mapped at filter. |
| Customer context SQL/role/provider unavailable | 503 SERVICE_UNAVAILABLE, not anonymous downgrade and not content. Context structural/programming INTERNAL stays sanitized 500. |
| Evidence reader Missing or a known non-SOLIDIFIED evidence status | 403 FORBIDDEN, no evidence existence/details. This is evidence eligibility, not token lookup failure. |
| Evidence reader Unavailable/null/throwing, role restore failure, malformed/ambiguous/inconsistent immutable projection | 503 SERVICE_UNAVAILABLE, no partial content or extra fields. |

Member MATCH means: filter-attested account; current CustomerContext ACTIVE account for that same account/channel; current ACTIVE organization and ACTIVE membership for **the grant's `owner_organization_id`**. Do not match by display name, role string, creator ID, client-selected tenant, cached memberships or another organization's membership. All existing membership roles qualify as membership; do not invent a recording-policy role matrix for a recipient page.

A successfully verified optional session plus current ACTIVE account and current ACTIVE membership in the grant tenant organization returned by trusted CustomerContext is sufficient for member content eligibility. A well-formed selected PERSONAL workspace or selected ORGANIZATION workspace different from the grant tenant does **not** make an otherwise current grant-tenant member NONMATCH; legacy sessions without selection likewise qualify. Selected values from the client remain non-authoritative and session selection alone never compensates for absent current membership. Missing/suspended/revoked grant-tenant membership or inactive grant organization yields NONMATCH and metadata-only 200; disabled/missing account remains 401. Verifier failures and malformed trusted verifier/session shapes still fail closed through the existing boundary as in §4; do not invent a public session/workspace policy or change `/me`, list or command selection behavior.

The GET domain-code set **excludes** SHARE_WRONG_TENANT, SHARE_VERIFICATION_FAILED, SHARE_RISK_BLOCKED, SHARE_LOCKED, SHARE_ADMISSION_LIMIT_REACHED, SHARE_GENERATION_STALE, SHARE_SESSION_EXPIRED and ticket-policy codes. Wrong tenant/member is the frozen metadata fallback, not an admission 403. Subject risk/ownership policy is checked by creation/admission/reconstruction/media operations, not this safe page: do not read subject-number tables, invoke creation/rotation policy or import their lock sequence. For equal grant/mode/session inputs, CLEAR and OPEN produce identical public GET status and JSON envelope: eligible member content stays present and nonmember metadata/challenge stays unchanged. OPEN neither strips otherwise eligible content nor authorizes media. Assert equality and zero risk SQL/policy calls within the existing named web/IT methods in §9. Evidence status still comes through the evidence-owner projection for any content that is returned.

Read consistency is statement-level READ COMMITTED observation, not a lock-protected authorization lease. A revoke/member change committed before the relevant read must be observed; a later change is handled by future reads/admission/media checks, not by retroactive page mutation. Do not promise serializable membership/revoke races or add cross-module locks.

## 7. Immutable Projection And Exact Wire Shape

Do not expose `content_snapshot` or the evidence-owner record directly. Validate the PA14 schemaVersion=1 content and its binding to the found evidence/tenant/mode; decode only the expected typed fields. The immutable grant is the **scope ceiling**. Only after the mode/member gate allows content, use the existing evidence reader under evidence_role and compare its evidence/subject/source/capture identity and complete canonical artifact tuples with the frozen grant content. An added/changed/unshared artifact must not expand the page; a mismatch fails 503, not a live rebuild or a new grant. No direct SQL from sharing against evidence/media/IAM/organization/subject tables.

Projection field ownership:

| JSON position | Exact fields/source and omission rule |
| --- | --- |
| Root always | `verificationMode`, `status`, `expiresAt`, `downloadAllowed` from resolved grant. UTC Instant serialization; no createdAt/validFrom/IDs/counts/generations. |
| Organization nonmatch root | Exactly the four root fields plus `verificationChallenge: {customerSessionRequired: true}`; `content` is **absent**, never null/empty object. No extra challenge field. |
| Allowed-content root | Four root fields plus `content`; omit `verificationChallenge`, not a false challenge. Applies to NONE/EXTRACTION_CODE/DESIGNATED_MOBILE without checking code/phone/proof, and matching ORGANIZATION_MEMBER. |
| `content` | Required `capturedAt`, `artifacts`; optional `validatedDurationSeconds` only when an authoritative validated ORIGINAL duration exists. `capturedAt` comes from frozen `content.identityAtCapture.capturedAt`, parsed as an explicit-offset Instant and checked against evidence capture identity; never substitute grant validFrom/evidence createdAt/current time. |
| Content duration | Existing evidence artifacts identify exactly one ORIGINAL; use only its non-null validated duration. Do not sum/max/select a DERIVED duration or invent zero. Missing is omitted, not null; malformed/negative/conflicting source fails closed. |
| Every artifact | Exactly required `artifactRole`, `mimeType`, `sizeBytes`, plus `sha256` from immutable `mediaSha256` when valid (the current provider requires it, so current successful artifacts include it). Rename internal `mediaMimeType`/`mediaSizeBytes`, never copy arbitrary JSON members. |

Artifacts sort by ORIGINAL rank 0, DERIVED rank 1, then lowercase SHA-256, numeric size, MIME ascending, matching PA14, not lexical role order. Preserve complete deterministic tuples; duplicate/ambiguous ORIGINAL or artifact identity is 503 as in the existing reader. Enforce ORIGINAL/DERIVED enums, MIME 1–128 and nonblank under existing source validity, nonnegative wire sizes/duration, lowercase 64-hex hashes and timestamp validity. Existing evidence reader's stricter persisted positive sizes/durations remain unchanged. New DTO omission annotations target optional content/challenge/duration only; do not change global Jackson inclusion or list's explicitly nullable nextCursor.

PublicShareView and every nested type have fixed record components; no Map/JSONB/org.jooq.Record/entity/extension catch-all. Tests enumerate every object's keys, not just forbidden substrings. Never return token/plaintext/digest, share/evidence/subject/account/organization/media/recording IDs, subject/caller identity or display name, source references, identityAtCapture object, mobile HMAC/key version/code/salt/hash, internal content digest, bucket/object key/version/URL, transcript/unshared content or media credential. Artifact SHA-256 is explicitly allowed content metadata, not the capability-token hash. No media-object identity or locator is necessary to compute the response.

## 8. Problems And No-Write Proof

The existing writer retains exact required fields `{type,title,status,code,traceId}` with `type:"about:blank"`, server UUID trace, `application/problem+json`, no detail/instance/errors/request URI/exception text. Add only public typed handling and these domain rows:

| HTTP | Code | Title |
| --- | --- | --- |
| 404 | SHARE_TOKEN_INVALID | Invalid share token |
| 410 | SHARE_REVOKED | Share revoked |
| 410 | SHARE_EXPIRED | Share expired |

Reuse unchanged common titles/statuses: 401 UNAUTHENTICATED/Authentication required (and `WWW-Authenticate: Bearer` only here), 403 FORBIDDEN/Access denied, 422 VALIDATION_FAILED/Request validation failed, 429 RATE_LIMITED/Too many requests, 503 SERVICE_UNAVAILABLE/Service unavailable, 500 INTERNAL/Internal error. Existing route 404 NOT_FOUND/Not found remains separate from token-domain denial. RATE_LIMITED is representable through existing typed mapping; this plan adds no limiter/provider or threshold. Unknown errors remain INTERNAL, never raw SQL/HTML or a new operation domain code.

No-write proof has independent layers:

1. Public API/repository has no command methods and never receives command service/repository, policy, token issuance/code/mobile provider, Redis/media/cache client or access-event writer. Member and evidence collaborators are existing query boundaries only.
2. Capture SQL proves only fixed read-only/role setup plus prepared SELECTs. Ban `INSERT`, `UPDATE`, `DELETE`, `MERGE`, DDL, advisory/row locks, writable CTEs, audit/Outbox/access append functions, SELECT *, command invocation and connection escape. Existing evidence reader's SELECTs remain owner-local.
3. PostgreSQL asserts actual `transaction_read_only=on` on the same physical backend for grant/evidence reads. A test-only attempted grant UPDATE in that transaction must fail SQLSTATE 25006, even though sharing_command_role has mutation ACL. Test deliberate mutation separately, not as part of the production GET. Guard against READ WRITE reset and verify role/read-only cleanup on pool reuse.
4. Snapshot all columns/rows of relevant grants, admissions, share_access_event, evidence facts, audit and Outbox around repeated GET/prefetch and every denial/failure path. Assert byte-identical grants (revision, both generations, count, status, expiresAt, content/token hash included), no new admissions/access events/audit/Outbox/evidence changes and no cookie/ticket/cached authorization. Observe provider counters and context checkouts; issuance/policy providers stay at zero during GET.

The expiry page denies without persisting EXPIRED, even when the general owner lifecycle would append an expiry event. No PAGE_OPENED, PREFETCH_IGNORED, VERIFICATION_FAILED, FAILURE or any other `share_access_event` is written. A successful content description is not a validated admission or media authorization.

## 9. Exact Test Ownership And Inventory

All new methods are plain, unconditional `@Test void` methods. Use method-local loops/tables for variants; no parameterized/repeated/dynamic/conditional tests or disabled/container-skipped success. Test substitutions live only in the new test files/fixture; reuse `DeterministicShareCommandProviders` without editing it. Fixture roles/passwords/time intervals/canaries are synthetic test values, not production policy/secrets. Use real PostgreSQL 16 and all V001–V010 via existing Flyway fixture; no H2/manual replacement schema.

### New Surefire: Exactly 12 Methods In Three Classes

| Owner and exact method | Required assertions |
| --- | --- |
| `PublicShareWebTests.exactPublicGetRoutePrecedesOptionalAuthentication` | Root/context-prefixed exact GET succeeds anonymously; customer mandatory-auth remains; canonical route/path/method rejection matrix in §4: percent-encoded token/whitespace, matrix/semicolon, duplicate-slash and other noncanonical URI→sanitized 404 NOT_FOUND before token shape/provider/service lookup, with zero verifier/digest/query calls; distinguish canonical valid-shaped unknown token→404 SHARE_TOKEN_INVALID after exact digest resolution. HEAD body empty; unknown path wins over bad bearer/Accept/token. |
| `PublicShareWebTests.optionalBearerFailuresKeepFrozenPrecedence` | Absent verifier never used anonymously; one valid session carried; empty/duplicate/malformed/rejected/wrong-audience→401; absent/ambiguous/outage/malformed verifier→503; supplied-auth before Accept/token; no client/header/attribute/container-principal override. |
| `PublicShareWebTests.tokenShapeAndLookupFailuresUseClosedProblems` | 1/512 pass to exact wrapper, canonical 513→422 before missing service/lookup; missing service→503; canonical unknown/replaced token→404 SHARE_TOKEN_INVALID after exact digest resolution, versus URI-encoded/noncanonical→404 NOT_FOUND before shape/provider/service lookup; terminal→410; all common typed statuses including representable 429 and sanitized INTERNAL; exact Problem keys/titles/headers/fresh server traces and no token/IDs/URI/canary. |
| `PublicShareWebTests.publicDtoIsClosedAndContentIsOmittedForNonmembers` | Enumerate root/content/artifact/challenge keys, all modes, absent optional fields rather than null, metadata-only/nonmatch shape, SHA-256 metadata distinction, duration source and ordering, no internal/grant/media identity. |
| `PublicShareWebTests.memberSessionMatchAndNonmatchNeverLeakContent` | Anonymous/grant-tenant nonmember→five-key metadata; verified active grant-tenant member→content, including legacy/no selection, well-formed PERSONAL or different selected ORGANIZATION workspace; current account disabled/missing→401; inactive grant org/revoked member→metadata; unavailable context→503; malformed trusted verifier/session shape fails closed through existing boundary, no new session policy. Callback only runs for organization-mode valid live grant; client tenant ignored. |
| `PublicShareWebTests.prefetchAndNegotiationHaveNoAuthorizationSideEffects` | Purpose/Sec-Purpose/prefetch and repeated GET are equivalent; equal grant/mode/session inputs under CLEAR versus OPEN return identical status/JSON envelope (eligible member content present, nonmember metadata/challenge unchanged), zero risk SQL/policy calls; unrelated Idempotency-Key/body/query cannot authorize; Accept precedence; buffered serialization failure emits only sanitized Problem, no partial content; no cookie/session/ticket/issuance/policy/media collaborator call. |
| `PublicShareQueryServiceTests.exactUtf8DigestLookupConcealsUnknownTokens` | Exact UTF-8, case/normalization/space distinctions and wrapper redaction for directly constructed service inputs only, never an HTTP route-gate override; direct Unicode 1/512/513/NUL/surrogate cases; absent/malformed/throwing SHA dependency; no fallback by IDs; invalid/unresolved→no evidence/member work. |
| `PublicShareQueryServiceTests.stateAndDatabaseTimeChecksNeverMutateGrant` | REVOKED precedence, persisted EXPIRED, ACTIVE/LOCKED time boundary including expiry equality; both ACTIVE and LOCKED sampled before validFrom→404 SHARE_TOKEN_INVALID with neither metadata nor content, no member/evidence call or persistence mutation; count-limit irrelevant, only one DB sample, no access-event/expiry/lock mutation. |
| `PublicShareQueryServiceTests.immutableEvidenceProjectionIsOrderedAndFailClosed` | Full canonical tuple/identity/scope consistency; capturedAt source, ORIGINAL-only duration, omitted null duration, artifact order; reader Missing/nonSOLIDIFIED→403; Unavailable/null/SQL/decode/malformed/extra artifact/role restore→503, no partial content. |
| `PublicShareQueryServiceTests.organizationContentRequiresCurrentMatchingMember` | Callback grant tenant is server row's tenant; MATCH/nonmatch/401/503/500 translations; absent tenant, null/throwing callback fails closed; no callback for nonmember modes/invalid/terminal; nonmatch never calls evidence reader. |
| `PublicSharePersistenceConfigurationTests.defaultWebAndDisabledCommandProfilesStayDatabaseFree` | Default, web-only and command-disabled contexts have no public persistence service/pool or new secret defaults; controller missing service503; no production verifier/test adapter; missing credentials retain fail-closed behavior. |
| `PublicSharePersistenceConfigurationTests.publicReadUsesExistingQualifiedManagerAndEnforcedReadOnlyTemplate` | Exact command manager/DSL/reader/SHA qualifiers, no third datasource, template REQUIRES_NEW/READ_COMMITTED/readOnly, SQL enforcement order; original query/command templates unchanged, evidence MANDATORY compatibility. |

### New Failsafe: Exactly Eight Methods In PublicSharesIT

| Exact method | PostgreSQL/HTTP evidence owned |
| --- | --- |
| `publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse` | Complete seeded/PA14-created grant per mode; real prepared digest bind, no hash selected; canonical altered/unknown/retired fixture token→uniform404 SHARE_TOKEN_INVALID after exact digest resolution; percent-encoded/URI-encoded whitespace, matrix/semicolon, duplicate-slash/noncanonical URI→sanitized404 NOT_FOUND before token shape/provider/service lookup and zero verifier/digest/query calls; exact root/content keys and secret/identity/log canaries, context path and no cookie/ticket. |
| `publicGetKeepsRevokedExpiredAndLockedPageSemantics` | Persisted REVOKED/EXPIRED410, ACTIVE/LOCKED within time200, expiresAt equality/past410 without status update; both ACTIVE and LOCKED sampled before validFrom→404 SHARE_TOKEN_INVALID, neither metadata nor content, no member/evidence call and byte-identical persistence snapshots; count exhaustion unchanged, terminal checks before projection; controlled test repository time seam for exact equality, real DB clock for past/future. |
| `organizationPageRechecksCurrentAccountTenantAndMembership` | Trusted test session + real customer reads: verified active grant-tenant member200 content even with well-formed PERSONAL or different selected ORGANIZATION workspace or legacy/no selection; anonymous/grant-tenant nonmember/other account without membership/revoked or suspended membership/inactive grant org200 metadata; disabled/missing account401; verifier errors/malformed trusted session fail closed through existing boundary, no new public policy; client selectors cannot override. Assert relevant committed revocations are observed on subsequent GET. |
| `publicProjectionUsesEvidenceOwnerOnSameReadOnlyConnection` | Same backend PID/connection for sharing grant and evidence reader, sharing_command_role→evidence_role→restore, MANDATORY transaction, immutable snapshot versus real artifacts/source/capture identity, capture timestamp and ORIGINAL duration, deterministic two-artifact ordering. Sharing source contains no foreign-table SQL. |
| `publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent` | Denied role, missing reader/service, evidence Missing/nonSOLIDIFIED/Unavailable, SQL/decode/tuple corruption/ambiguity, malformed capture/snapshot and role-restore failure; exact403/503/500 distinctions, no raw provider/DB/token echo. Corruption only in isolated test setup with safe fixture cleanup, never production guard edits. |
| `repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged` | Full before/after grant/admission/event/evidence/audit/Outbox snapshots across repeated requests and all denial/failure scenarios; equal grant/mode/session inputs with CLEAR versus OPEN risk fixtures must yield identical status/JSON envelope, including member content and nonmember metadata/challenge; zero subject risk SQL/policy/issuance calls; no count/revision/generation/time/status mutation, no Redis/media/client beans or cookie/ticket. |
| `publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState` | `SHOW transaction_read_only`/current_user and PID evidence; first app SQL READ ONLY; intentional test UPDATE25006; query-role cannot perform hash lookup or assume command role, command login NOINHERIT denied before SET ROLE, read succeeds only under intended role; concurrent public reads isolated; next pool checkout role/read-only reset and subsequent PA14 command remains writable. |
| `publicReadPreservesCreateRevokeRotateListAndMeBehavior` | Use deterministic test-only creation providers for real create return-once/replay, read created token, revoke→public410, LOCKED extraction rotation→public ACTIVE with unchanged token, rotation replay no code, customer list/me remain exact/tenant-bound; clear read counters after setup and assert each intervening GET adds no command/access/audit/Outbox effect. No admission endpoint implemented. |

`PublicSharesFixture` composes the package-local existing `SharingQueryFixture` and/or `CustomerMeFixture` APIs as needed, but does not change them or add infrastructure abstractions. Follow existing CustomerShareCommandsIT provisioning for a NOINHERIT login granted command/evidence roles, complete SOLIDIFIED evidence/artifacts and deterministic command context. Configure customer read and list pools independently as existing HTTP fixtures do. Because the base bare sharing fixtures do not necessarily contain PA14's complete content/artifacts, build valid PA14 schemaVersion=1 scope or call real deterministic create; do not rely on shallow JSON shape fixtures for positive projection evidence.

### Existing Tests And Source/CI/XML Arithmetic

Planning-time source extraction using CI's `@Test void` regex found **16 Surefire classes/79 methods**, **six Failsafe classes/118 methods**. Current Failsafe breakdown: PostgresContainerIT64 + SharingQueryServiceIT10 + CustomerMeIT10 + CustomerSharesIT8 + SharingRoleLeastPrivilegeIT8 + CustomerShareCommandsIT18 =118. CI's exact inventory calls also currently require79/118; this is a directly counted baseline, not an estimate.

Future inventory:

- Surefire: `79 + (6 + 4 + 2) = 91`, `16 + 3 = 19` classes.
- Failsafe: `118 + 8 = 126`, `6 + 1 = 7` classes; breakdown `64+10+10+8+8+18+8`.
- Combined: `197 + 12 + 8 = 217 = 91 + 126`; net **+20 methods, +4 classes**.
- Existing architecture test methods are extended, not added/renamed; all other existing test sources remain unchanged. Full PA14 command tests (six service/four web/18 IT), customer list/query/cursor/role tests and `/me` tests retain all existing assertions and inventory identities.

CI must add exactly the four new fully qualified class keys with the method sets above and atomically update every current hardcoded count in `.github/workflows/ci.yml`: the early Surefire aggregate condition at current line162 changes `79`→`91`, and its diagnostic at163 changes `expected exactly 79 Surefire unit/module tests`→`expected exactly 91 Surefire unit/module tests`; the `exact_inventory` calls at current lines366–367 change unit `79`→`91` and Failsafe `118`→`126`; the current print summary at368 becomes PA15, 19 Surefire classes/91 methods, 7 Failsafe classes/126 methods, breakdown `64+10+10+8+8+18+8=126`, combined `217`, zero skips/errors/failures. Preserve class expectations, source=CI=XML exactness, source-class equality, unsupported-annotation/duplicate checks and zero failures/errors/skips; the historical PA9 64-method aggregate check remains 64. Add `PublicSharesIT` to POM includes or the new source class would lack XML. Parse **fresh** XML from a clean full run, not additive/stale targeted reports. Require exact per-method identity as well as total counts; extra methods/classes require plan correction.

In `backend/README.md`, update only current-count clauses at current lines40,291,300,322: line40's current inventory and container-suite enumeration become 19 Surefire classes/91 methods and 7 Failsafe classes/126 methods, breakdown `64+10+10+8+8+18+8`, combined217; line291's current CI/XML clause uses those same totals/classes/breakdown; line300's current clause appends PA15 19/91 + 7/126 =217 while preserving PA12's159 and PA14 slice-1 185/revoke191/rotation197 as historical; line322's current cumulative clause becomes those same PA15 totals/classes/breakdown while its co-located PA9 historical64 and ten separate Surefire tests remain unchanged. State PA15 adds exactly 12 Surefire and eight Failsafe methods. Preserve all existing historical clauses, including those co-located at40/300/322, PA9 64 at305/313/322, and every PA12/PA14 historical count; relabel only current rotation-count wording as historical when appending PA15 current totals, never rewrite history. Document safe GET/optional membership/command-enabled read dependency within these current clauses; no count edit to line305 or other historical clauses. Do not rewrite architecture/spec docs or claim production readiness.

## 10. Implementation Sequence And Validation

These are integration checkpoints **within one slice**, not independent module slices/gates:

1. Add narrow public API/repository/read-only wiring and safe immutable projection using existing evidence and member boundaries; keep commands/list untouched.
2. Add one controller and extend the existing filter/writer, then extend exact architecture assertions and named dependency expectations.
3. Add the named tests/fixture; atomically update POM, CI and README counts; validate whole behavior and unchanged regressions. No standalone provider/framework work unit.

Future validation requires pinned Java21.0.13+9, Maven wrapper3.9.9 and Docker/PostgreSQL16 as current CI. Commands, run from `backend` unless marked otherwise:

```sh
java -version
./mvnw -B -ntp -v
docker info
./mvnw -B -ntp -Dtest=PublicShareWebTests,PublicShareQueryServiceTests,PublicSharePersistenceConfigurationTests,CustomerApiBoundaryTests,SharingQueryBoundaryTests,ModuleBoundaryTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PublicSharesIT verify
./mvnw -B -ntp clean -Pcontainer-smoke verify
```

First command group proves focused boundary/service/configuration and existing module constraints. Targeted Failsafe proves eight real PostgreSQL methods with zero skips; its interim report directory is not the final inventory. Final clean run must yield exactly19 Surefire/7 Failsafe XML suites,91/126 methods and217 combined, zero skipped/failures/errors, source=CI=XML equality under the existing CI Python inventory logic updated only as specified. Retain logs/XML and explicit SQLSTATE25006, actual read-only/current_user/backend-PID observations, no-write before/after snapshots and role cleanup evidence.

Run existing CI default/web packaged-JAR smokes and production-JAR dependency/firewall checks unchanged, adding bounded web-only anonymous `/api/v1/public/shares/<synthetic-token>`→503 (no service/DB), malformed supplied bearer→401 and non-GET→404 probes. Anonymous page request must never fail401 merely for absent session. Default stays nonservlet/DB-free, web-only no Hikari/verifier/cache, no unexpected production dependencies/test verifier/secret defaults. Preserve the82 adversarial firewall probes; adding HTTP smoke probes does not add JUnit methods or change that firewall count. Do not claim remote CI unless actually run remotely by its owner.

Repository-level read-only Git checks must use process-local environment, never rewrite `.git`. Before using the documented Linux environment below, confirm live-worktree `git rev-parse --git-dir` matches `/mnt/d/codex-call/.git/worktrees/codex-call15`; if the live worktree changes, re-confirm its actual directory rather than hardcoding a different worktree. This is a validation precondition, not a change to the rejected worktree-path finding:

```sh
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15 GIT_WORK_TREE="$PWD" GIT_OPTIONAL_LOCKS=0 git diff --check
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15 GIT_WORK_TREE="$PWD" GIT_OPTIONAL_LOCKS=0 git diff --name-only
```

For this plan task only: verify plan path was absent before writing; count current source methods/class names and compare current CI79/118; verify allowlist/named test arithmetic; validate Markdown whitespace, `sha256sum` and `wc -l`; use scoped/read-only Git diffs to ensure no tracked edits and inspect only this new untracked artifact. Do **not** run Maven/Docker/implementation tests, write reports elsewhere, inspect private `.codex`, modify Control or initiate review. Planning validation cannot stand in for future PostgreSQL/runtime evidence.

## 11. Risks, Open Questions And Completion Report

- No product-scope blocking question remains. The exact GET operation supersedes historical observation-event/admission prose. Do not add admission/risk/tenant errors to reconcile the older text.
- Required production session verification is absent: supplied credentials fail closed via the existing verifier seam; anonymous safe reads remain possible. Approved session issuance/OTP/MFA inputs remain with their existing owners. Tests substitute deterministic sessions only in test sources.
- Frozen V010 does not provide a dedicated digest-capable read ACL. This plan intentionally reuses the existing command pool **inside a PostgreSQL-enforced read-only operation**, without changing grants, command enablement or credentials. Runtime must supply existing approved command/evidence-role membership or return503. A dedicated least-privilege public-read principal is a later security/schema work unit, not a hidden PA15 migration. SQLSTATE25006 evidence is mandatory; a JDBC hint alone is unacceptable.
- Current immutable scope/provider anomalies are denied503, never repaired or broadened. Missing/non-SOLIDIFIED evidence is generic403. No missing production provider, malformed capture timestamp or absent duration may be replaced with invented data/policy.
- READ COMMITTED public observation is not an authorization lease or serializable membership check; future admission/media owners must recheck current grant/member/risk state. No media authorization is implied by page content or OPEN-risk safe metadata.
- Actual PostgreSQL, concurrency, role/read-only reset, no-write and regression results remain future implementation evidence. Network/Docker/toolchain limitations must be reported as unexecuted/blocked, never as passing or skipped-success.

Future implementation completion report: task/work unit/gate/status; one-slice confirmation; exact changed paths against23-path allowlist; final source=CI=XML19/7 classes and91+126=217 arithmetic; exact commands/results/retained evidence; endpoint/Problem/omission/member/no-write assertions; protected paths unchanged; unresolved operational dependencies and owners; no production-readiness or unperformed remote action claims.

Plan-author completion report: task/gate/status; artifact path/SHA-256/line count; exact future allowlist; slice count1; current79+118=197 and future91+126=217/net+20; validations actually performed; protected paths and residual risks; explicit statement that only this plan artifact was written and no mutating Git/external/review action occurred. Read-only Git inspection is validation, not a stage/commit/push action.

This is not a mechanical module split: all paths serve the single public-read acceptance path. Existing authenticator, context service, evidence projection, pools, digest primitive, Problems and test fixtures are reused. The small named API and lazy membership seam prevent forbidden cross-module SQL without adding a gateway/provider framework, token issuer, cache, admission model or future route. No independent implementation slice can be marked complete merely because a layer compiles.
