# PA15 Public Share Read — Implementation Report

## 1. Identity, Authority And Status

| Item | Value |
| --- | --- |
| Report task | `pa15-public-share-read-implementation-report-gpt-20261106` |
| Work unit | `PA15-sharing-public-share-read` |
| Gate | `implementation` |
| Candidate status | **implementation-complete** |
| Evidence date | 2026-11-06, Asia/Shanghai (`UTC+08:00`) |
| Accepted plan | `docs/gateflow/pa15-sharing-public-share-read-plan.md` |
| Plan identity | **399 lines**, SHA256 `EED82E23743C07B71AFAE09C5183757DBAFDFC44E3B71FEA55CF67C2518A8012` |
| Accepted checkpoint authority supplied by task and Control | `7bd5d28449880c0b89ab26b40f36bbf35b2f0cb7` |
| Reporting artifact | `docs/gateflow/pa15-sharing-public-share-read-implementation.md` |

The current 25-path implementation candidate, retained successful validation logs, current test source, CI inventories and fresh XML reports agree. The single authorized public-read slice is implemented and locally validated. This status is an implementation result, **not production readiness, independent review acceptance, gate advancement or release authorization**.

This report is authored as the final response for the runner to persist. This reporting task edits no repository file and runs no additional Maven or Docker tests. The implementation artifact is separate Gateflow reporting authority, not a 26th production/test/configuration candidate path.

### Historical Attempts And Retries

The active implementation retry state in `docs/gateflow/project-execution-control.md` records:

1. **Initial implementation attempt:** stopped without edits because “prepared statements disabled” conflicted with the accepted indexed prepared-binding requirement. The Controller clarified that prepared statements remain enabled; SQL/bind/secret logging must be disabled.
2. **Retry 1:** reproduced Tomcat 11.0.27 rejecting encoded slash before the servlet filter with HTML 400, contrary to the required closed 404. Only the blocked implementation report was written. The subsequent accepted fix-4 plan authorized the bounded connector customizer and expanded the candidate boundary from 23 to 25 paths.
3. **Retry 2:** stopped without writes because its prompt still pinned the superseded 372-line plan, SHA256 `07605141492B281337FC37046C01CD5995C071867970D5FA7BB17EC7B38D26AE`, while Control accepted the 399-line fix-4 plan.
4. **Retry 3:** implemented and validated the candidate. Its final handoff remained blocked only on replacing the durable report because the required dedicated patch interface was unavailable.
5. **This reporting task:** independently inspects the candidate and retained evidence and supplies the complete replacement report.

Before runner persistence, the existing report remains the historical 171-line blocked artifact, SHA256 `73E1EB3AE835252F7BC273004866463642495A8EBF010603CE4F0D3896803DA2`. Its earlier blocker and baseline results are historical, not the current candidate’s acceptance evidence.

### Local Git Provenance Qualification

The accepted checkpoint identifier above is preserved exactly as supplied by the task and Control. Read-only inspection of the available local Git history instead shows:

- Current HEAD: `175f9c2bd8d848808d0dcbf30eba36e46f40a0e5`, `gateflow: retry PA15 implementation 3`.
- Previous entry: `98e964a8b4f68ed46267abf5e041a32671b06f4f`, `gateflow: enter PA15 implementation retry 2`.
- Local fix-4 entry: `7bd5d284013def5b1e9dc63eabe6c61bad320524`, `gateflow: accept plan fix 4 for PA15-sharing-public-share-read`.

The plan blob at that local fix-4 entry independently hashes to the exact accepted plan SHA256. The two full checkpoint identifiers are **not asserted to be identical**. Controller owns reconciling this provenance discrepancy before checkpoint-based acceptance; it does not alter the verified plan contents or local candidate/test results.

## 2. Exact Candidate Boundary

Expanded `git status --short` and the retained final audit identify exactly **25 candidate files: 11 modified tracked files and 14 new files**. `M` means modified; `N` means new.

| # | State | Exact path |
| --- | --- | --- |
| 1 | M | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` |
| 2 | M | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java` |
| 3 | N | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/PublicSharesController.java` |
| 4 | M | `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` |
| 5 | N | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/package-info.java` |
| 6 | N | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/OpaqueCapabilityToken.java` |
| 7 | N | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryException.java` |
| 8 | N | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareView.java` |
| 9 | N | `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java` |
| 10 | N | `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicShareQueryRepository.java` |
| 11 | N | `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqPublicShareQueryRepository.java` |
| 12 | N | `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicSharePersistenceConfiguration.java` |
| 13 | N | `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java` |
| 14 | N | `backend/src/test/java/com/onecall/archtests/sharing/PublicShareQueryServiceTests.java` |
| 15 | N | `backend/src/test/java/com/onecall/archtests/PublicSharePersistenceConfigurationTests.java` |
| 16 | N | `backend/src/test/java/com/onecall/archtests/container/PublicSharesFixture.java` |
| 17 | N | `backend/src/test/java/com/onecall/archtests/container/PublicSharesIT.java` |
| 18 | M | `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java` |
| 19 | M | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` |
| 20 | M | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` |
| 21 | M | `backend/pom.xml` |
| 22 | M | `.github/workflows/ci.yml` |
| 23 | M | `backend/README.md` |
| 24 | M | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java` |
| 25 | M | `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java` |

The raw status also contains the private untracked `.codex` entry and collapses the new `publicquery` directory. Expanding that directory yields the individual files above; `.codex` is excluded from the candidate and was not opened.

`git diff --check` passes. Git emits LF/CRLF conversion warnings, but no whitespace-error failure. The staged diff is empty. The retained final audit records an unchanged index.

## 3. One-Slice Implementation And Production Decisions

The implementation is one end-to-end behavior:

`GET /api/v1/public/shares/{capabilityToken}` → existing boundary filter → public controller → public-query service → digest lookup and conditional evidence projection → closed response.

The files are responsibility partitions within that slice, not independently completed feature slices.

### Route, Optional Authentication And Error Precedence

- The existing highest-precedence `/*`, **REQUEST-only** filter remains the sole admission gate.
- Only the exact public GET is admitted. Method/path/canonical-URI rejection occurs before authentication, token construction, service resolution or database access.
- The canonical gate excludes slash, semicolon and percent presentations in the token segment and requires `contextPath + servletPath == requestURI`. Encoded/noncanonical presentations receive sanitized `404 NOT_FOUND`.
- Absence of Authorization on an admitted public GET permits anonymous access. A supplied Authorization header uses the existing strict authenticator; malformed or invalid credentials never downgrade to anonymous. Unavailable verification fails closed.
- Authentication retains precedence over negotiation and controller/service work. Token shape validation precedes public-service lookup; missing, ambiguous or unavailable service resolution returns 503 rather than partial content.
- The controller is web-profile and servlet-conditional. Its only route returns `PublicShareView`; it lazily resolves qualified `publicShareQueryService` and, only when membership is actually required, qualified `customerContextService`.

`OpaqueCapabilityToken` validates 1–512 Unicode code points, rejects NUL and invalid UTF-8-encodable Java strings, and redacts `toString()`. Digesting uses the existing SHA-256 provider over **exact UTF-8 bytes**: no trimming, case folding, Unicode normalization or service-side decoding. Lookup binds the resulting lowercase 64-hex digest through an indexed prepared statement; it never binds the raw token.

Unknown and retired tokens share `404 SHARE_TOKEN_INVALID`. Route rejection is separately `404 NOT_FOUND`. Typed mappings include 401 unauthenticated, 403 forbidden, 410 revoked/expired, 422 validation, 429 rate-limited, 503 unavailable and sanitized 500 internal.

Problems retain exactly `{type,title,status,code,traceId}`, `type: "about:blank"`, server-generated UUID trace IDs and `application/problem+json`. Responses retain `Cache-Control: no-store`, `Pragma: no-cache` and `X-Content-Type-Options: nosniff`. No request URI, exception detail, client trace echo, redirect, cookie, session or media hint is introduced.

### Tomcat Encoded-Solidus Correction

`CustomerWebConfiguration` adds the accepted, web-profile:

`WebServerFactoryCustomizer<TomcatServletWebServerFactory>`

Its connector customizer calls:

`connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue())`

This is verified against Boot **4.1.2** and Tomcat **11.0.27**. It prevents Tomcat’s earlier HTML 400 rejection from bypassing the application’s closed Problem contract. It does **not** authorize encoded slash: the preserved canonical filter gate sees and rejects that presentation with closed 404 before credentials or lookup.

No second filter, ERROR-dispatch registration, valve, dependency change, deployment setting or permissive route replacement is added. Existing real-server tests are strengthened in place, without increasing their method count.

### Enforced Read-Only Command-Pool Transaction

The public query reuses the existing qualified `sharingCommandDsl`, transaction manager and command datasource. V010’s list role cannot read the token predicate or snapshot; its privileges are not widened.

The dedicated `publicShareQueryTransactionTemplate` is:

- `PROPAGATION_REQUIRES_NEW`;
- `ISOLATION_READ_COMMITTED`;
- Spring read-only.

Inside the transaction, the first service SQL is `SET TRANSACTION READ ONLY`, followed by an assertion that `transaction_read_only` is `on`. The service selects and verifies `sharing_command_role` before grant lookup.

The repository exposes exactly:

- `findByTokenDigest(String) -> Optional<PublicGrant>`;
- `sampleDatabaseTime() -> Instant`.

No repository method accepts `DSLContext`. Its immutable internal `PublicGrant` contains exactly evidence ID, optional owner organization ID, verification mode, status, validity bounds, download flag and snapshot; its diagnostic representation is redacted. SQL selects only those required columns by digest and samples `clock_timestamp()`, with no locking query.

The service’s DSL dependency is the explicit, bounded **service-only exception** for read-only/role orchestration and passing the same transaction-aware DSL to the existing evidence collaborator. It is not a general SQL escape hatch for controllers or DTOs.

### Evidence Reader, Membership And State

Content uses the existing **MANDATORY** `ShareableEvidenceProjectionReader.readShareableProjection(DSLContext, UUID)` on the same transaction-aware connection. The service switches locally to `evidence_role`, verifies role and read-only state, and restores/verifies `sharing_command_role` in `finally`. Sharing adds no direct evidence/media/IAM/organization/subject SQL.

Membership is a lazy controller-owned bridge:

`MemberAccess.check(UUID grantOrganizationId) -> MemberMatch`

`MemberMatch` is exactly `MATCH`, `NONMATCH`, `UNAUTHENTICATED`, `UNAVAILABLE`, `INTERNAL`. No servlet or customer-module type crosses into sharing.

Only `ORGANIZATION_MEMBER` invokes that bridge. Current trusted account/context and active membership in the **grant organization** determine eligibility, not client identity or selected workspace. Anonymous and nonmatching callers receive metadata plus `verificationChallenge.customerSessionRequired=true`, with `content` absent. Disabled/missing account and dependency failures remain fail-closed. Nonmember-mode and terminal/invalid requests do not perform unnecessary customer-context lookup.

After digest resolution, database time determines state precedence:

1. REVOKED → `410 SHARE_REVOKED`.
2. Persisted EXPIRED, or database time at/after expiry → `410 SHARE_EXPIRED`.
3. Before `validFrom` → concealed `404 SHARE_TOKEN_INVALID`.
4. Otherwise ACTIVE or LOCKED remains readable under the page projection rules.

LOCKED does not become an admission error. NONE, EXTRACTION_CODE and DESIGNATED_MOBILE do not require code/mobile verification for this GET. No observed expiry or lock transition is persisted.

### Closed Immutable Projection And No Side Effects

The public root has only `verificationMode`, `status`, `expiresAt`, `downloadAllowed`, optional `content` and optional `verificationChallenge`.

- Content: `capturedAt`, optional ORIGINAL `validatedDurationSeconds`, immutable artifact list.
- Artifact: `artifactRole`, `mimeType`, `sizeBytes`, `sha256`.
- Challenge: only `customerSessionRequired`.

Absent optional values are omitted, not serialized as null. No persistence entity, arbitrary map, JSONB, extension bag or internal record reaches the wire.

The frozen grant snapshot is the scope ceiling. The service validates its binding and compares authoritative evidence identity and complete artifact tuples. Artifacts sort by ORIGINAL rank 0, DERIVED rank 1, then SHA-256, numeric size and MIME. Mismatch, malformed projection or unavailable dependency fails closed without returning partial content or rebuilding the grant.

No share/evidence/subject/account/organization/media ID, token/hash, source identity, object key/version/URL, verification secret or unshared content is exposed. Artifact SHA-256 is allowed content metadata, not the capability digest.

The GET performs no admission, count, generation, ticket, session, cache/Redis, media, audit, Outbox or access-event mutation. Purpose/prefetch headers do not change authorization. Subject-risk CLEAR/OPEN does not change the public envelope or invoke creation/rotation policy.

## 4. Test Evidence And Exact Arithmetic

### Retained Successful Runs

| Validation | Result | Evidence |
| --- | --- | --- |
| Focused six-class Surefire | **24 tests**, zero failures/errors/skips | `/tmp/pa15-focused-final.log` |
| Targeted `PublicSharesIT` | **8 tests**, real PostgreSQL 16, zero failures/errors/skips | `/tmp/pa15-targeted.log` |
| Fresh clean full verify | **91 Surefire + 126 Failsafe = 217**, zero failures/errors/skips | `/tmp/pa15-clean-verify.log` |
| Exact inventory checker | Source = CI expected classes/methods = current XML | `/tmp/pa15-inventory-final.log` |

The targeted verify finished at **2026-11-06T07:15:53+08:00**. The fresh clean verify finished at **2026-11-06T07:22:33+08:00**, with `BUILD SUCCESS`.

The focused classes contribute:

`PublicShareWebTests 6 + PublicShareQueryServiceTests 4 + PublicSharePersistenceConfigurationTests 2 + CustomerApiBoundaryTests 3 + SharingQueryBoundaryTests 1 + ModuleBoundaryTests 8 = 24`.

These are overlapping validation runs, not additional methods to add to 217.

### Current Surefire Inventory

Class names below omit the common `com.onecall.` prefix.

| Suite | Methods |
| --- | ---: |
| `archtests.CustomerMeWebTests` | 8 |
| `archtests.CustomerSharesCommandWebTests` | 4 |
| `archtests.CustomerSharesWebTests` | 10 |
| `archtests.CustomerWebConfigurationTests` | 6 |
| `archtests.OneCallApplicationTests` | 1 |
| `archtests.PublicSharePersistenceConfigurationTests` | 2 |
| `archtests.PublicShareWebTests` | 6 |
| `archtests.SharingCommandPersistenceConfigurationTests` | 2 |
| `archtests.SharingPersistenceConfigurationTests` | 2 |
| `archtests.architecture.BootstrapDependencyBoundaryTests` | 1 |
| `archtests.architecture.CustomerApiBoundaryTests` | 3 |
| `archtests.architecture.ModuleBoundaryTests` | 8 |
| `archtests.architecture.SharingQueryBoundaryTests` | 1 |
| `archtests.sharing.CustomerShareCommandServiceTests` | 6 |
| `archtests.sharing.PublicShareQueryServiceTests` | 4 |
| `core.customer_iam.internal.application.CustomerContextServiceTests` | 6 |
| `core.customer_iam.internal.security.CustomerSessionBoundaryTests` | 7 |
| `core.customer_iam.internal.web.CustomerShareCursorCodecTests` | 6 |
| `core.sharing.internal.application.CustomerShareQueryServiceTests` | 8 |
| **19 suites** | **91** |

### Current Failsafe Inventory

All suites are in `com.onecall.archtests.container`.

| Suite | Methods |
| --- | ---: |
| `PostgresContainerIT` | 64 |
| `SharingQueryServiceIT` | 10 |
| `CustomerMeIT` | 10 |
| `CustomerSharesIT` | 8 |
| `SharingRoleLeastPrivilegeIT` | 8 |
| `CustomerShareCommandsIT` | 18 |
| `PublicSharesIT` | 8 |
| **7 suites** | **126** |

The exact Failsafe breakdown is **64+10+10+8+8+18+8=126**. The command suite retains eight create, five revoke and five rotation methods.

PA15 adds three Surefire classes with **6+4+2=12** methods and one Failsafe class with **8** methods:

- Surefire: `16/79 → 19/91`.
- Failsafe: `6/118 → 7/126`.
- Combined: `22 classes/197 methods → 26 classes/217 methods`.
- Net addition: **4 classes and 20 methods**.

The connector proof extends existing `CustomerWebConfigurationTests` methods; that class remains at six methods. Architecture assertions likewise extend existing methods. The fixture is not a test suite.

During this reporting task, the current `backend/target/surefire-reports/TEST-*.xml` and `backend/target/failsafe-reports/TEST-*.xml` were independently parsed. The current CI inventory checker was also executed locally against source and those XML files, without running tests. Both reproduce the exact totals and zero failures/errors/skips, including method identity and duplicate/conditional-test rejection.

### Exact New Method Partitions

**`PublicShareWebTests` — six methods**

- `exactPublicGetRoutePrecedesOptionalAuthentication`
- `optionalBearerFailuresKeepFrozenPrecedence`
- `tokenShapeAndLookupFailuresUseClosedProblems`
- `publicDtoIsClosedAndContentIsOmittedForNonmembers`
- `memberSessionMatchAndNonmatchNeverLeakContent`
- `prefetchAndNegotiationHaveNoAuthorizationSideEffects`

**`PublicShareQueryServiceTests` — four methods**

- `exactUtf8DigestLookupConcealsUnknownTokens`
- `stateAndDatabaseTimeChecksNeverMutateGrant`
- `immutableEvidenceProjectionIsOrderedAndFailClosed`
- `organizationContentRequiresCurrentMatchingMember`

**`PublicSharePersistenceConfigurationTests` — two methods**

- `defaultWebAndDisabledCommandProfilesStayDatabaseFree`
- `publicReadUsesExistingQualifiedManagerAndEnforcedReadOnlyTemplate`

**`PublicSharesIT` — eight methods**

- `publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse`
- `publicGetKeepsRevokedExpiredAndLockedPageSemantics`
- `organizationPageRechecksCurrentAccountTenantAndMembership`
- `publicProjectionUsesEvidenceOwnerOnSameReadOnlyConnection`
- `publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent`
- `repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged`
- `publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState`
- `publicReadPreservesCreateRevokeRotateListAndMeBehavior`

## 5. Real Runtime And Database Proof

### Actual Environment

Successful candidate evidence reports:

| Component | Observed version |
| --- | --- |
| Java | Eclipse Temurin **21.0.13+9**, `/opt/jdk21` |
| Maven | **3.9.9**, including wrapper version confirmation |
| Docker client | **29.1.3** |
| Docker server | **29.9.2**, Docker Desktop |
| PostgreSQL | **16.15**, resolved from disposable `postgres:16` |
| Spring Boot | **4.1.2** |
| Embedded Tomcat | **11.0.27** |
| Testcontainers | **2.0.5** |

Earlier blocked attempts reported different local Java/Docker observations. Those historical observations are not substituted for the successful runtime’s actual versions.

### Encoded-Slash Runtime Behavior

The strengthened embedded-server test asserts the actual connector value `passthrough`, then sends root-path encoded-slash requests with absent or malformed Authorization, `Accept: text/html` and an untrusted trace header. It verifies closed 404 Problems, security/cache headers, server UUID traces and no HTML/URI/client-trace echo.

A separate `/pa15` context-path server tests `%2F`, `%2f` and double-encoded `%252F`, with malformed Authorization and HTML Accept. Each remains `404 NOT_FOUND` with no-store. Packaged-JAR probes independently cover encoded slash with and without hostile headers.

Thus the proof covers real Tomcat admission and the existing filter, not only mocked MVC routing.

### Read-Only, Same-Connection And Pool Reset

`/tmp/pa15-runtime-evidence.log` records:

- `transaction_read_only=[on]`.
- One observed backend PID: **72**.
- Current-user sequence: command login → `sharing_command_role` → `evidence_role` → restored `sharing_command_role`.
- Intentional write rejection: **SQLSTATE 25006**.
- Pool reuse: **PID 72**, current user restored to login, `transaction_read_only=off`, JDBC `isReadOnly=false`.
- Final result: `postgresEnforced=true`, `roleRestored=true`, `poolReset=true`, `subsequentCommandWritable=true`.

The intentional write is a rollback-contained evidence probe, not production GET behavior. The successful subsequent command shows that the public read does not poison the shared command pool.

The real integration suite additionally checks direct unauthorized token-column access and forbidden role escalation with SQLSTATE **42501**.

### Eleven-Table No-Write Equality

The standalone runtime evidence prints complete before/after SHA-256 snapshots. Every corresponding digest is equal:

| Table | Before/after digest |
| --- | --- |
| `audit.audit_fact` | `d26480f801a6120983ff7815d00996eee21fbecd6620ca9a7220d15b68b615ce` |
| `evidence.evidence_artifact` | `85b47c2c30c6f273b367b9e1467fb87de3faa5c2e1858e1a0a68573bcc99b373` |
| `evidence.evidence_event` | `b0c44d67cf99434f777cfd5af909084c0476fa43a63b6dc881000196b2335346` |
| `evidence.evidence_record` | `2d103657c0785f97d9bbc3595d516f9b0eb8a12456366bb27d046289d7d72f2e` |
| `evidence.media_object` | `16c55007fd480d4070c40ba4abc56c7359584a347915f255cfdbe5755fe25326` |
| `evidence.recording` | `d64a5b63e9cea45649fced60b28f0b8b3d73a317885db6bb1166a22a99516644` |
| `platform.idempotency_record` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
| `platform.outbox_event` | `e984ccaef2a57ce3384776a6fb915c98c45e997f5c9dd487a9f4cf4d44776c6e` |
| `sharing.evidence_share_grant` | `b113e358f9affcc3b0fb08ba31441fcb9a7c67ea79e32a595bab65dfe87b74fd` |
| `sharing.share_access_event` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
| `sharing.share_admission` | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |

The repeated-page/prefetch integration test independently asserts complete snapshot equality and absence of mutation/risk SQL. These observations establish zero changes to the protected grant, admission, access-event, evidence, audit, idempotency and Outbox facts during the exercised reads; they are not a claim about unrelated concurrent production activity.

## 6. Packaged-JAR Smokes And Dependency Firewall

The retained default/web smoke script passes against:

`backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`

Observed JAR SHA256:

`AFDE7B9517EE54D8007FD8BDEB0A784863FC672C78A33C20E7BCD0191AB5B742`

Default startup remains non-web/database-free. Opt-in web startup requires no database or verifier adapter. Spawned smoke JVMs are cleaned up.

The **nine bounded HTTP probes** are:

| Request | Expected result |
| --- | --- |
| Customer `/me`, no Authorization | 401 `UNAUTHENTICATED` |
| Customer `/me`, supplied bearer without verifier | 503 `SERVICE_UNAVAILABLE` |
| Customer `/me`, HTML Accept and untrusted trace, no credentials | 401 `UNAUTHENTICATED` |
| `/error`, HTML Accept | 404 `NOT_FOUND` |
| Public synthetic token, anonymous, no persistence service | 503 `SERVICE_UNAVAILABLE` |
| Public synthetic token, malformed Authorization | 401 `UNAUTHENTICATED` |
| POST public synthetic token | 404 `NOT_FOUND` |
| Public encoded-slash token | 404 `NOT_FOUND` |
| Public encoded-slash token plus malformed Authorization, HTML Accept and untrusted trace | 404 `NOT_FOUND` |

These are negative/startup smokes, not successful database-backed public reads. Closed Problems and cache/security headers are checked.

The production dependency firewall also passes **all 82 adversarial probes**:

- Baseline: **37 resolved coordinates / 34 packaged JARs**.
- Web: **55 resolved coordinates / 48 packaged JARs**.
- **47 coordinate-to-JAR counterparts**, **8 dependency-only entries**, **1 packager-only JAR**.
- Preserved reported deltas: **18/0 and 14/0**.

The firewall uses exact `group:artifact:type:version:classifier:scope` coordinates, positive matching before legacy forbidden-name regexes, and exact packaged inventory equality. Approved collision coordinates include:

- `org.springframework.boot:spring-boot-web-server:jar:4.1.2::compile`
- `org.springframework.boot:spring-boot-webmvc:jar:4.1.2::compile`
- `org.springframework:spring-web:jar:7.0.10::compile`
- `org.springframework:spring-webmvc:jar:7.0.10::compile`
- `org.apache.tomcat.embed:tomcat-embed-core:jar:11.0.27::compile`
- `org.apache.tomcat.embed:tomcat-embed-el:jar:11.0.27::compile`
- `org.apache.tomcat.embed:tomcat-embed-websocket:jar:11.0.27::compile`

The sole packager branch remains `spring-boot-jarmode-tools-4.1.2.jar`. `commons-logging` is parsed explicitly. PA15 adds no dependency or broadened firewall exception; the complete approved coordinate inventory remains in the CI firewall and README.

Evidence: `/tmp/pa15-jar-smokes.log`, `/tmp/pa15-firewall.log`, `backend/target/jar-index.txt` and `backend/target/pa11-runtime-dependencies.txt`.

## 7. Architecture, Regression And Protected-Path Audit

The new named interface is **`sharing::public-query`**, with exactly four top-level types, excluding package metadata:

1. `OpaqueCapabilityToken`
2. `PublicShareQueryException`
3. `PublicShareView`
4. `PublicShareQueryService`

Its nested safe types are the exception `Code`, service `MemberAccess`/`MemberMatch`, and view `Content`, `Artifact`, `VerificationChallenge`, `VerificationMode`, `Status`, `ArtifactRole`. Its sole public query operation is `getPublicShare(OpaqueCapabilityToken, MemberAccess) -> PublicShareView`.

Partition enforcement covers:

- Web controller limited to the public named API, existing customer context and Problem writer.
- Service-only DSL exception; no DSL in the repository interface or other public API types.
- Repository implementation owns only grant/time queries.
- Persistence configuration wires the existing qualified transaction graph.
- Existing customer-query and customer-command named-interface inventories remain unchanged.
- No command execution, admission abstraction or direct cross-module persistence access is introduced.

The full green suite retains create/replay/revoke/rotation/list/`me`, module boundaries, least-privilege checks and migration backstops. The new public integration regression exercises those existing behaviors alongside the read.

The final audit records **1,358 protected tracked files byte-identical**. This reporting task independently recomputed those hashes against `/tmp/pa15-baseline-hashes.json` and found **zero mismatches** after excluding the 11 modified candidate files and the separately authorized implementation report.

V001–V010, ACL definitions, OpenAPI, schema specifications, Control, accepted plans and review artifacts remain untouched by the candidate. No private `.codex` content or prior worktree was inspected.

Documentation/build synchronization is complete:

- POM includes `PublicSharesIT` among exactly seven Failsafe suites.
- CI checks exact source/class/method/XML identity at **19/91 + 7/126 = 217**.
- README records the same current totals, exact Failsafe breakdown, read-only command-pool dependency and fail-closed behavior.
- Historical checkpoint counts remain labeled historical rather than presented as current totals.

## 8. Commands, Evidence And Inspection Scope

Candidate validation used the Java 21 toolchain with Maven 3.9.9. Because the checked-in wrapper has CRLF shell text in this environment, successful invocations evaluated an in-memory CR-stripped wrapper without editing it:

```bash
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:$PATH
sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp \
  -Dtest=PublicShareWebTests,PublicShareQueryServiceTests,PublicSharePersistenceConfigurationTests,CustomerApiBoundaryTests,SharingQueryBoundaryTests,ModuleBoundaryTests test

sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp \
  -Pcontainer-smoke -Dit.test=PublicSharesIT verify

sh -c "$(sed 's/\r$//' mvnw)" ./mvnw -B -ntp \
  clean -Pcontainer-smoke verify
```

These retained commands ran from `backend`. Container runs used the evidenced Docker host and Testcontainers host override. This report does not rerun them.

| Retained evidence | Purpose |
| --- | --- |
| `/tmp/pa15-focused-final.log` | Final focused six-class, 24-test success |
| `/tmp/pa15-targeted.log` | Targeted eight-method PostgreSQL public-read success |
| `/tmp/pa15-clean-verify.log` | Fresh complete clean verify, 217 green |
| `/tmp/pa15-inventory-final.log` | Exact source=CI=XML identity |
| `/tmp/pa15-jar-smokes.log` | Default/web packaged startup and nine HTTP probes |
| `/tmp/pa15-firewall.log` | Runtime dependency inventory and 82 firewall probes |
| `/tmp/pa15-runtime-evidence.log` | Explicit database digests, roles, read-only enforcement and reset |
| `/tmp/pa15-final-audit.log` | Exact 25-path scope, protected hashes, index and whitespace audit |

Earlier compile/diagnostic logs are not represented as green acceptance evidence. The successful final focused run and fresh clean verify supersede intermediate failures.

Read-only reporting inspection used the explicit live Git directory:

`/mnt/d/codex-call/.git/worktrees/codex-call15`

with worktree:

`/mnt/c/Users/Administrator/.codex/worktrees/pa15-sharing-public-share-read/codex-call`

This avoids the checkout’s Windows-style `.git` pointer failing under WSL. Inspection included status, diff whitespace, empty staged diff, plan SHA/line count, source/configuration, retained runner evidence, XML parsing, protected-file hashing and local execution of the CI inventory checker. No source, tests, configuration or reporting file was written by this task.

## 9. Limitations And Residual Risks

| Risk / limitation | Owner and disposition |
| --- | --- |
| Production customer-session verifier | **Customer IAM / deployment owner:** tests use controlled verifier seams. Missing production verification fails closed; production session integration is not established here. |
| Dedicated least-privilege public-read ACL | **Database/security owner:** command-role reuse is the accepted bounded design, protected by PostgreSQL read-only enforcement. A dedicated public-read principal/ACL remains future work; V010 is unchanged. |
| Command-pool capacity and dual-role provisioning | **Backend/SRE/database owner:** anonymous reads share command-pool capacity and require the approved command/evidence role transitions. Production sizing, abuse controls and provisioning remain unvalidated. |
| READ COMMITTED observation | **Backend/security owner:** this is not an authorization lease or serializable cross-module snapshot. Later revoke/membership changes require future read/admission/media checks; no locks bridge those boundaries. |
| Production capability-token format | **Security/product/backend owner:** exact-byte 1–512 handling is implemented, not a production token generator, alphabet, entropy, TTL or issuance policy. Noncanonical HTTP presentations remain rejected. |
| Historical documentation reconciliation | **Controller/specification owner:** older `docs/14` page-open/prefetch/observed-expiry prose remains historical. The accepted operation-specific observation-free GET contract controls this slice; general admission rules must not be reintroduced here. |
| Checkpoint identifier discrepancy | **Controller:** reconcile the supplied/Control full checkpoint ID with the differing local fix-4 Git object identified in §1. Exact plan-content identity is verified; checkpoint-object equality is not. |
| Evidence retention | **Controller/runner:** `/tmp` and Maven `target` evidence are local and replaceable. Preserve them with the candidate for subsequent review; this report records their results but does not archive them remotely. |
| Remote CI | **Controller/CI owner:** local execution of CI scripts passed; no hosted CI execution or remote acceptance is claimed. |

## 10. Completion And Action Boundary

The single public-read candidate is **implementation-complete** against the exact accepted 399-line plan: 25 bounded candidate files, focused and real PostgreSQL evidence, fresh **217-test** source=CI=XML agreement, connector regression proof, database no-write/read-only/role-reset proof, packaged smokes and all 82 firewall probes.

No staging, commit, push, branch/worktree creation, remote mutation, review dispatch or later-gate action was performed by this reporting task. No additional Maven/Docker test or runtime validation was launched.

One inspection exception must be recorded: a read-only `git cat-file` lookup of the supplied checkpoint triggered Git’s implicit attempt to retrieve a missing object from the promisor remote. That attempt failed with DNS resolution failure; no object was retrieved and no remote state was changed. Accordingly, this report does **not** claim that zero network attempts occurred.

Runner persistence of this Markdown replaces the historical blocked report. Independent review, provenance reconciliation, gate acceptance and any production rollout remain Controller-owned subsequent decisions.