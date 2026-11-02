# PA12 customer share list API — implementation plan

- Work unit: `PA12-customer-share-list-api`.
- Task: `pa12-customer-share-list-api-plan-gpt-20261102-01`.
- Date: 2026-11-02. Gate: `plan`. Author completion: `status: plan-complete`.
- Bounded revision: task `pa12-customer-share-list-api-plan-fix1-gpt-20261102-01`, 2026-11-02, current gate `fix`; revision completion `status: plan-fix-1-complete`. Dispositions/input hashes: `docs/gateflow/pa12-customer-share-list-api-plan-fix-1.md`. Original completion above is historical, not gate advancement.
- Bounded revision: task `pa12-customer-share-list-api-plan-fix2-gpt-20261102-01`, 2026-11-02, gate `fix`; historical `status: plan-fix-2-complete`. Input hashes, accepted precision dispositions and validation: `docs/gateflow/pa12-customer-share-list-api-plan-fix-2.md`.
- Current bounded revision: task `pa12-customer-share-list-api-plan-fix3-gpt-20261102-151500`, 2026-11-02, gate `fix`; `status: plan-fix-3-complete`. Input hashes, Controller-accepted/rejected findings, dispositions and validation: `docs/gateflow/pa12-customer-share-list-api-plan-fix-3.md`. Earlier completion records are historical; no gate advancement is implied.
- Binding scope: `project-execution-control.md`, “PA12 customer share list API - goal confirmation accepted”. Baseline identity recorded there: `b458be9a52bf6f7cd9543355dceb98c9d89b364c` (PA11 merged). This is not an independently verified local Git-head claim.
- Artifact is a code-generation-ready proposal, not implementation, review acceptance, production readiness, or permission to advance gates.

## 1. Outcome, evidence, and prerequisites

Implement exactly one behavioral slice, S1: an authenticated creator-scoped, read-only `GET /api/v1/customer/shares`, through the existing customer boundary, returning the frozen OpenAPI `SharePage`. No independently useful intermediate behavior requires another slice.

Read evidence and consequences:

| Evidence | Binding consequence |
| --- | --- |
| Control document PA12 goal and PA11/PA10 checkpoints; PA10/PA11 final-closeout artifacts | Reuse accepted query/authentication behavior; baseline is 49 Surefire methods/11 classes and 84 Failsafe methods/3 classes (`64+10+10`), 133 total. Earlier checkpoint totals are historical, not the current baseline. |
| `docs/11-account-identity-and-workspaces.md`, account/workspace rules | PERSONAL derives from the natural-person account; ORGANIZATION is not another login. Membership lifecycle is authoritative; administrator status does not widen creator scope. |
| `docs/12-technology-stack-and-system-architecture.md` §§6–7, 9.5 | Call a declared query interface, not another module's repository. Customer API composition must be explicitly recorded because the current customer-iam dependency row does not include sharing. No security-stack expansion. |
| `docs/14-production-data-model-and-flyway-schema-spec.md`, sharing listing/ACL contract; V002/V009 SQL | Existing identity/organization SELECT grants suffice. Preserve role-local transactions and exact foreign-column grants; no migration or provisioning change. |
| `docs/15-production-openapi-surface-spec.md` §5 and `api/openapi/onecall-v1.openapi.yaml`, `listCustomerShares`, `ShareSummary`, `SharePage`, `CursorParam`, `LimitParam`, `Cursor` | Stable `validFrom DESC, shareId DESC`; cursor length 1–512; default limit 20, maximum 100; required nullable nextCursor; no offset/total or internal cursor object in JSON. |
| `docs/17-production-security-design-spec.md` §§10–11 and dependency-failure rules | Fixed `onecall:customer-session:v1`; no cross-audience/session-store fallback, key-policy invention, credential logging, or secret projection. |
| PA11 `internal/security`, `internal/application`, `internal/web`, persistence and their unit/socket/container tests | Single highest-priority filter, wrapped principal, server trace UUID, closed Problem writer, opt-in web, independently qualified customer pool, `/me` ACTIVE-account behavior. |
| PA10 application records/service, repository interface/jOOQ SQL/configuration, unit/container/boundary tests | Validate page before transaction; set/assert sharing_role; recheck ACTIVE creator context; fetch limit+1; use last returned row, PostgreSQL ordering and microsecond anchors; sanitize diagnostics. Do not fork this algorithm. |
| Module declarations, `ModuleBoundaryTests`, `CustomerApiBoundaryTests`, `SharingQueryBoundaryTests`, `backend/README.md`, `.github/workflows/ci.yml`, `backend/pom.xml` | Exact dependency/file/test inventories currently prohibit this edge and new files. Amend the precise assertions, not broad bans or classpath firewalls. Explicitly add the fourth container class to Failsafe. |

Implementation prerequisites: accepted PA12 plan gate, unchanged PA10/PA11 baseline, Java/Maven pins already in the repository, Docker-capable PostgreSQL 16 validation environment. No production verifier is a prerequisite for code generation: all authenticated executable evidence uses test-owned verifier beans. Absence of a production verifier remains fail-closed, not a hidden implementation TODO.

## 2. Server-attested workspace and `/me` compatibility

Add `SelectedCustomerWorkspace(WorkspaceType workspaceType, UUID organizationId)` in customer `internal/application`, reusing `CustomerContext.WorkspaceType` (PERSONAL/ORGANIZATION). For `/shares`, PERSONAL requires null organizationId; ORGANIZATION requires non-null organizationId; null workspaceType is invalid. Record/session/caller constructors must carry even invalid non-null selections without throwing: structural validation is shares-only. No role, owner-account override, membership snapshot, provider subject, permission matrix, expiry, or token is carried.

Extend `VerifiedCustomerSession` to `(accountId, channel, audience, selectedWorkspace)` and `TrustedCustomerCaller` to `(accountId, channel, selectedWorkspace)`. Retain the existing three-argument session and two-argument caller constructors as delegating compatibility constructors with null selection. Null means **unattested selection**, not PERSONAL. This avoids silently changing PA11 test/adaptor contracts or guessing a workspace.

`CustomerBearerAuthenticator.authenticate` retains opaque bearer parsing, exactly-one-verifier checks and fixed audience. Validate existing account/channel/audience fields and reject a wrong non-null audience with 401 exactly as before. Carry the verifier-provided selected workspace unchanged, including null and structurally invalid non-null values; do not validate or reject its structure in this shared path. Null selection is accepted for legacy `/me`; it is never sufficient for `/shares`. Update verifier Javadoc to require that new `/shares` callers be selected from trusted server session state, never request query/header/body/cookie state.

`CustomerRequestPrincipal` continues to carry only the trusted caller, with its existing caller/name behavior; bearer redaction belongs to `OpaqueCustomerBearer`. No credential retention and no new request attribute. The filter's locally constructed principal is installed through its request wrapper. The new controller accepts only `CustomerRequestPrincipal` and reads `caller()`. An attribute called account/workspace/principal is never an identity source. Request trace attributes remain tracing only.

After the existing filter Accept negotiation, `CustomerSharesController.getCustomerShares` checks the principal with `instanceof CustomerRequestPrincipal`, then calls private `requireSelectedWorkspace(TrustedCustomerCaller)` to reject null or malformed selection as SERVICE_UNAVAILABLE before parsing page input or resolving the query service. This is a verifier/trusted-boundary configuration failure, not a client validation problem. Missing/wrong principal is UNAUTHENTICATED. Valid bearer + structurally invalid selection + incompatible/malformed Accept yields 500; acceptable Accept yields 503. No implicit PERSONAL default and no fallback to the first `/me` workspace.

`/me` continues to call only `CustomerContextService.getCustomerMe`, ignores selection and its structure entirely for its existing response/query path, lists PERSONAL first and currently ACTIVE organization memberships in repository UUID order, and keeps `account/workspaces/channel` JSON and existing 401 for missing/disabled accounts. It does not acquire a sharing connection or add a selectedWorkspace response field. A valid but now inaccessible organization, null selection, null workspaceType, PERSONAL with organizationId, and ORGANIZATION without organizationId must not alter `/me` status, body, ordering or dependency resolution relative to the same legacy session. No new selection-caused 503 is permitted on `/me`; existing authentication, negotiation and account-state behavior remains unchanged.

Client `accountId`, `ownerAccountId`, `organizationId`, `workspace`, `workspaceType`, `role`, channel/audience hints, cookies, headers and request bodies are ignored as authority. The shares adapter reads only cursor/limit query parameters. There is no workspace-selection endpoint or parameter.

## 3. Route ownership and explicit module seam

Own `CustomerSharesController` in `customer_iam.internal.web`, alongside `/me`. Its responsibility is HTTP/authenticated-principal/page adaptation, not sharing business logic. It invokes the **existing** `CustomerShareQueryService.listCustomerShares` once. It never accesses sharing persistence, SQL, a Spring bean by string/reflection, or `/me` JSON to reconstruct authority.

Expose the existing `sharing.internal.application` package with new `package-info.java`, `@org.springframework.modulith.NamedInterface("customer-query")`. Controller-adopted B evidence (`docs/reviews/plan-review-20261102-124556.md`, executed Modulith 2.1.2 probes V1/V8) proves discovery, exposure and successful verification under both fixture `@Modulithic` and `@SpringBootApplication` discovery roots despite the historical `internal` path. V2 proves bare `sharing` is insufficient; V5 proves persistence and the default sharing API remain inaccessible to a consumer declaring only `sharing::customer-query`. These are executed library probes, not project Maven/container validation.

A named interface exports the **whole package**, including public `ShareQueryDiagnostic` and the service's public constructor; it is not Java access control or a Java `interface`. The package still contains only the existing eight query/record/exception/diagnostic source files plus package-info; persistence is not exported. Restrict consumer imports/usages to `CustomerShareQueryService`, `TrustedShareCaller`, `SharePageRequest`, `ShareCursor`, `SharePage`, `ShareSummary`, `ShareQueryException` and their required nested enum types with exact import/source architecture assertions. Forbid consumer diagnostic imports/construction and direct service construction; diagnostic construction stays sharing-owned by these assertions, not type visibility. No class relocation, duplicate service/projection, synthetic/open module, reflection shortcut or reverse dependency is needed.

Change customer module declaration to `allowedDependencies = {"sharing::customer-query"}`. Keep sharing `allowedDependencies = {"evidence"}`. Architecture tests restrict sharing imports to the new customer web adapter/page/codec and to the exact named-interface types; customer application/security/persistence continue to have no sharing imports. Sharing remains web-free and customer-import-free. Its SELECTs of identity tables use existing DB ACLs, not Java reverse dependencies.

This is a deliberately documented **transport-composition exception** to the docs/12 customer-iam ownership row, not a claim that the old map already permits it. Only customer web transport depends on the sharing query seam; identity domain/service/persistence do not. Accepted PA12 plan approval must encompass this bounded exception. The discovered production graph has exactly one edge and no cycle. The broader allowed map does contain a potential path sharing → evidence → entitlement_metering → catalog_order → customer_iam; these are currently permissions, not implemented edges. Do not claim the allowed graph itself is acyclic. Architecture owns future composition extraction before implementing edges that would close that cycle; S1 must prove the exact discovered graph and reject any actual cycle. Do not add a new fifteenth API-composition module for one route.

Exact allowed map after S1 (all unmentioned code remains unchanged):

| Module | Allowed dependencies |
| --- | --- |
| customer_iam | sharing::customer-query |
| organization_access | customer_iam |
| operations_iam | operations_audit |
| subject_number | none |
| activation_consent | subject_number |
| ivr | subject_number, activation_consent, entitlement_metering, evidence |
| catalog_order | customer_iam, subject_number |
| entitlement_metering | catalog_order, subject_number |
| evidence | entitlement_metering |
| sharing | evidence |
| ai_orchestration | evidence, entitlement_metering |
| notary | evidence |
| operations_audit | none |
| platform_reliability | none |

`ModuleBoundaryTests` must compare annotation strings including the named interface exactly, compare discovered **module** edges to the normalized module names (strip `::customer-query` only for this comparison), and replace “all discovered edges empty” with exactly `customer_iam -> sharing`, all other discovered edges empty. Keep exact 14 modules, cycle/forbidden/internal negative fixtures, and `ApplicationModules.verify()`. Assert the sharing `NamedInterfaces` non-unnamed names exactly (`customer-query`, no additional named export) and the named top-level member set exactly: `CustomerShareQueryService`, `TrustedShareCaller`, `SharePageRequest`, `ShareCursor`, `SharePage`, `ShareSummary`, `ShareQueryException`, `ShareQueryDiagnostic`; pin nested members exactly to `ShareSummary.VerificationMode`, `ShareSummary.ShareStatus`, `ShareQueryException.Code`, with no unrelated members. Assert no persistence/default sharing API member in this named interface.

Concrete project assertion homes are the existing two discovery models/methods in `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`; use model-level member/allowed-target and exact source/import assertions, not new executable archfixture variants:

- Production `@SpringBootApplication` model: `ApplicationModules.of(OneCallApplication.class)` in `verifiesNoCyclesOrBoundaryViolations()`. Fold in the exact named-interface/member assertions above and positive customer-query exposure/consumer allowance; negatively assert that bare `sharing` is not the declared allowed target and that persistence/default sharing API members are absent from the allowed named member set. Retain successful production `verify()`. Keep graph normalization/exact-edge assertions in existing `discoveredProductionEdgesStayInsideTheConfiguredMap()` and literal annotation-map assertions in `assertsExactAllowedDependencyMapFromPlan62()`.
- Include-all `@Modulithic` fixture model: `ApplicationModules.of(InternalFixtureRoot.class, INCLUDE_TEST_FIXTURES)` in `rejectsInternalPackageAccessFixture()`. Retain its original `assertThatThrownBy(() -> ApplicationModules.of(InternalFixtureRoot.class, INCLUDE_TEST_FIXTURES).verify()).isInstanceOf(Violations.class)` generic internal-access rejection unchanged in strength. Within this same method, explicitly assert the fixture provider model's exposed/default versus unexposed internal members, and apply the same positive named-target/member and negative bare-target/persistence/default-API source/import policy assertions to the production sharing/customer declarations and member model obtained through `ApplicationModules.of(OneCallApplication.class)`. The fixture root discovers `consumer`/`provider`, not production sharing: do not pretend it discovers `customer-query`. These are concrete project model/source-policy assertions in both existing discovery methods; the actual named-interface behavior under both root annotations is supported separately by the attributed executed library probes, not claimed as a new project fixture replay.

For missing/misspelled named-interface verification, the normative throwable assertion wraps the **whole discovery-plus-verification expression**, `assertThatThrownBy(() -> ApplicationModules.of(...).verify()).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("No named interface named 'customer-query' found!")`, or its exact include-all equivalent. Executed Modulith 2.1.2 absent/misspelled-interface probes show that the actual message continues with ` Original dependency declaration: customer_iam -> sharing::customer-query.`; the fixed-prefix assertion deliberately permits that suffix. `ApplicationModules.of(...)` and `getDirectDependencies()` return normally; the exception surfaces when the expression verifies. Never expect `Violations` for this mistake class or assert that discovery alone throws. Do not mutate production annotations to manufacture this negative case; the source/model assertions pin the required name and the attributed probes supply the mistake-class evidence. Fold all assertions into existing methods (test-owned, non-test helpers may stay in the same file); no new method in the eight-test inventory, fixture/source file, or weakening of any existing negative fixture.

## 4. Page adaptation, cursor encoding and input rules

New `CustomerSharePage(List<ShareSummary> items, String nextCursor, boolean hasMore)` is a wire adapter, not another domain snapshot. Reuse the existing `ShareSummary` record and its exact 11 fields/enums. `items` is immutable. Always serialize exactly `items`, `nextCursor`, `hasMore`, including `nextCursor:null`. Controller-adopted B's executed Jackson 3.1.7 probe shows nullable record fields (including maxAdmissions) already emitted and Instants emitted as ISO date-time strings with adaptive 0/3/6/9 fractional digits, preserving microseconds, enums and UUIDs. Start without a redundant inclusion annotation. `wirePageHasOnlyFrozenFieldsAndNullableCursor` is normative against the actual web mapper: only if it proves null omission, add record/property-local `@JsonInclude(ALWAYS)` on the new page, never global Jackson configuration or a PA10 record edit. No Optional, caller IDs, selected workspace or internal cursor record is serialized.

Controller algorithm: enter only after the filter's unchanged authentication and PA11 Accept negotiation; require trusted principal; validate attested workspace via `requireSelectedWorkspace`; parse/validate limit; decode/validate cursor against the caller; create `TrustedShareCaller(caller.accountId(), Optional.ofNullable(selected.organizationId()))` and `SharePageRequest(Optional<ShareCursor>, Optional<Integer>)`; resolve `@Qualifier("customerShareQueryService") ObjectProvider<CustomerShareQueryService>`; invoke once; adapt the returned page. Do not resolve service/persistence before authentication, negotiation and local validation. No customer-context lookup before or after sharing query.

- Missing limit: `Optional.empty()` to PA10 (effective 20). Exactly one supplied limit: ASCII `[0-9]+`, parsed with checked integer bounds, value 1–100; empty, sign, whitespace, decimal, exponent, overflow or duplicates: VALIDATION_FAILED. Leading zeroes normalize to the same integer; never change the accepted value based on an override parameter.
- Missing cursor: `Optional.empty()`. Exactly one supplied cursor: nonempty, at most 512 characters and valid canonical encoding below. Empty, duplicate, overlong, offset/raw timestamp/raw ID, wrong version, malformed or context-mismatched value: CURSOR_INVALID. Do not trim or double-decode servlet query values.
- Unknown parameters do not enter normalization or SQL. Ignore them, consistent with PA11 selector handling; only trusted account/selection determine scope. No `total`, offset, client sort, filter, owner or role field is introduced.
- Both invalid limit and invalid cursor: limit validation wins. Malformed parameter/framework binding must not leak Spring's default HTML/400: use raw request parameter arrays, not automatic Integer conversion or a bindable caller DTO.
- `hasMore=false` yields JSON `nextCursor:null`, including empty/tail pages. `hasMore=true` encodes the **internal next cursor**, which PA10 derives from last returned row, not the lookahead row. Verify internal cursor account/organization match the caller before encoding; impossible internal inconsistency is INTERNAL, never emitted as a new cursor.

### 4.1 Exact opaque cursor wire format (v1)

Implement one package-private `CustomerShareCursorCodec` with `encode(TrustedShareCaller, ShareCursor)` and `decode(TrustedShareCaller, String)`. Stateless, no key registry, store, token provider or TTL. The cursor is a query continuation, **not** an authorization credential or an integrity-signed token. Scope always comes from the principal and repository predicates. Changing a valid anchor cannot grant visibility outside that scope. No authenticity claim or cryptographic anti-tampering policy is invented.

Wire is canonical unpadded base64url of this big-endian binary payload:

| Field | Exact encoding |
| --- | --- |
| Version | One byte `0x01` |
| Normalized filter/sort binding | 32 bytes SHA-256 of UTF-8 `onecall:customer-shares:v1\nvalidFrom:desc,shareId:desc\n` followed by canonical lowercase account UUID, `\n`, then `PERSONAL` or `ORGANIZATION:` plus canonical lowercase organization UUID. No final newline. Channel and limit are deliberately excluded; they do not change this query's visibility or ordering. |
| Anchor seconds | Signed 64-bit epoch seconds |
| Anchor nanos | Unsigned-range 32-bit integer 0–999999999, divisible by 1000 |
| Share ID length | One unsigned byte: Unicode scalar count 1–128 |
| Share ID | Each Unicode scalar as 21 bits, most-significant bit first; concatenate and zero-pad only final byte |

Payload length must equal `46 + ceil(21 * scalarCount / 8)` bytes. Reject trailing bytes, nonzero pad bits, padding `=`, non-base64url characters, surrogate scalars, scalars above U+10FFFF, NUL, an all-ASCII-space shareId, invalid date ranges or non-microsecond anchors. Fixed 21-bit scalars have no variable-width overlong alternative representation; no separate test obligation is implied. Decode at most 384 bytes after enforcing wire length. Re-encode and require byte-for-byte equality to enforce canonical base64url. Apply the PA10 PostgreSQL anchor year range (-4712 through 294276 at UTC) and existing shareId constraints without signed UUID/String sorting. Use the same validation rules for internal anchors before encode; an invalid internal result maps INTERNAL.

The fixed-width scalar encoding is narrowly necessary to meet the frozen bound for every legal 128-code-point shareId: naive UTF-8 base64 can exceed 512 for valid non-ASCII IDs. Maximum payload is 382 bytes, hence at most 510 unpadded base64url characters. This small bounded bit loop avoids a compression dependency, DB cursor state, truncation, lossy hashes of sort keys, or rejecting legal DB identifiers. Include full-size supplementary-Unicode round-trip tests.

On decode compare the 32-byte binding to a digest freshly computed from the **trusted caller**; reject mismatch before DB/service resolution. Neither account nor organization IDs are carried as decoded authority. Construct `ShareCursor` with account/organization from the caller and only anchor fields from decoded bytes. Reusing an unchanged cursor across account, PERSONAL/ORGANIZATION, or two organizations fails CURSOR_INVALID; changing page size is allowed. The unkeyed binding is a normalized-filter fingerprint, not a secret HMAC: a client able to construct another anchor still cannot alter SQL creator/workspace predicates. No IDs, digest, cursor or bearer are logged.

## 5. Error mapping and precedence

Extend `CustomerContextException.Code` by appending `CURSOR_INVALID`; extend writer mapping with status 400/title `Invalid cursor`. Existing codes/statuses/titles remain byte-shape compatible. Map sharing's five enum codes explicitly to customer codes; do not map by exception message or forward sharing diagnostic text. Missing/ambiguous service beans or bean construction failure: SERVICE_UNAVAILABLE. Unexpected mapping/serialization/programming failure: INTERNAL. Preserve buffering so no partial success body survives a failure.

| Code | HTTP | Producer and policy |
| --- | --- | --- |
| UNAUTHENTICATED | 401 | Missing/malformed/duplicate bearer, invalid/revoked/expired verifier result, wrong audience, missing trusted principal; WWW-Authenticate: Bearer. |
| FORBIDDEN | 403 | PA10 current creator-context denial: missing/disabled account, missing/inactive organization/membership. Do not remap PA10 FORBIDDEN to `/me`'s 401. |
| VALIDATION_FAILED | 422 | Invalid limit/request page shape. |
| CURSOR_INVALID | 400 | Wire/anchor/context failure or PA10 typed cursor denial. |
| RATE_LIMITED | 429 | Reserved Problem vocabulary only, as PA11; no reachable rate-limit producer, policy, counter, retry interval or fake quota is added. Test writer directly. |
| SERVICE_UNAVAILABLE | 503 | Missing/ambiguous/outage verifier; malformed trusted selection; absent/unusable sharing graph, role assertion failure, existing PA10 SQL/transaction/connectivity failure. |
| INTERNAL | 500 | Unexpected application/serialization failure; authenticated unacceptable/malformed/q=0 Accept retains PA11 mapping. |

Order: (1) exact method/path rejection (404 NOT_FOUND, no authentication); (2) bearer/verifier/audience checks (401 or 503); (3) existing Accept negotiation (500 if incompatible/malformed); (4) trusted selection (503); (5) limit (422); (6) cursor (400); (7) query bean availability (503); (8) PA10 role assertion (503), then ACTIVE context (403), then fetch/mapping and commit. Thus malformed input beats DB outage/inactive lifecycle once authentication and selection succeed, exactly because PA10 validates pages before its transaction; no extra lifecycle preflight reverses that order. Technical failure during lifecycle read is 503, not a fabricated 403. No response is published until the query returns after transaction completion.

Step (2) explicitly excludes selected-workspace structure; step (4) is shares-only and includes both null and malformed selection. Valid bearer + invalid non-null selection + incompatible/malformed Accept is 500 at step (3), while the same selection + acceptable Accept is 503 at step (4), before page parsing/query resolution. Pin both in existing `negotiationAndBufferedFailuresPreservePa11Boundary`; `/me` never executes step (4).

Every application-reachable Problem remains exactly `type,title,status,code,traceId`, `type=about:blank`, server UUID trace, application/problem+json, no detail/instance/exception/SQL/IDs/cursor/secret. Retain no-store, Pragma:no-cache and nosniff on success/failure, and existing sanitized handling of unexpected downstream status/sendError. Do not widen `/error` mapping or treat request error attributes as authorization. Connector-level malformed/oversized traffic remains the accepted PA11 Platform/Security edge boundary, not a new universal Problem guarantee.

## 6. Filter, lifecycle and database ownership

Change only the admission predicate in `CustomerBoundaryFilter.doFilter`: method must be literal GET; servletPath must equal one of `/api/v1/customer/me`, `/api/v1/customer/shares`; raw requestURI must equal contextPath plus that exact admitted servletPath. No wildcard/prefix/suffix matching, decoding, normalization or forwarding exception. Keep one registration, `Ordered.HIGHEST_PRECEDENCE`, `DispatcherType.REQUEST`, existing trace/header/buffering/wrapper behavior. Controller GetMapping uses exact shares path and `produces=application/json`; both controller profile conditions match `/me`. HEAD (even MVC's implicit GET HEAD), OPTIONS/preflight, POST/PUT/PATCH/DELETE/TRACE, aliases, trailing slash, encoded path segments, semicolon parameters, repeated slashes, public/provider/mutation paths and direct `/error` are rejected before verifier/query. Genuine container ERROR dispatch retains the existing separate sanitized error-controller behavior; direct requests cannot select it.

Selection **provenance** belongs to the verifier/trusted boundary; selection **structure** is validated only in the shares adapter after Accept, never by shared authentication or `/me`, and never by a new DB selector. Selection **current accessibility** belongs in the existing PA10 `requireActiveCreatorContext`, inside its sharing transaction on every page. Its ACTIVE account, ACTIVE organization, ACTIVE membership reads remain authoritative regardless of server session age, role, channel or previously issued cursor. No new role gate: all currently active member roles still see only their own created shares. A disabled account yields `/shares` 403 and `/me` 401, intentionally preserving both internal contracts.

Keep PA10 READ_COMMITTED/REQUIRES_NEW/read-only transaction, `SET LOCAL ROLE sharing_role` plus current_user assertion before repository use. Share query and lifecycle SELECTs use `sharingQueryDataSource`, `sharingQueryTransactionManager`, `sharingQueryTransactionTemplate`, `sharingQueryDsl`, `sharingQueryRepository` and `customerShareQueryService`, with existing `onecall.persistence.url/username/password`, activated only by `persistence`. New controller uses only the exactly qualified lazy ObjectProvider; no new bean graph, pool, transaction or fallback credentials.

`/me` retains `customerQueryDataSource`, `customerQueryTransactionManager`, `customerQueryTransactionTemplate`, `customerQueryDsl`, `customerContextRepository`, `customerContextService`, `onecall.customer.persistence.*`, and customer_iam_role. Shares never borrows that connection; `/me` never borrows sharing's. `web` without `persistence` may authenticate using a test verifier but shares returns 503 rather than instantiating a fallback query. Default no-profile startup remains non-web and database-free; `web` without credentials/verifier remains fail-closed as PA11. Opt-in `web,persistence` retains existing explicit-credential requirements and eager sharing graph behavior; do not silently make persistence activation/defaults different.

Separate test login identities are LOGIN/NOINHERIT/non-superuser: customer login is member of customer_iam_role only, sharing login of sharing_role only, denied login of neither. No dual-role login, broad onecall_runtime shortcut, owner/superuser production connection, or new grants. Existing V009 sharing foreign reads are customer_account(id,status), organization(id,status), organization_membership(organization_id,account_id,role,status), plus its already frozen evidence/subject ACLs; do not enlarge them. sharing_role owns mutation privileges from V009, but this application transaction is read-only: prove writes fail with SQLSTATE 25006 rather than falsely claiming that role has no sharing UPDATE grant. Foreign unauthorized columns/writes/locks must still fail 42501.

Read-committed lifecycle semantics remain PA10: changes committed before the next request are observed; no locking/serializable/current-state cache is added and no guarantee of instantaneous revocation after an in-flight check is claimed. Do not edit repository SQL, lookahead/keyset predicates, clock/status policy, collation, schema or grants.

## 7. S1 implementation write set and dependency order

All paths below are repository-relative. This is the **future implementation** allowlist, not authorization to modify them at this plan gate. Unless identified as new, update only the functions/assertions described.

| Path under `backend/src/main/java/com/onecall/core/` | Allowed change |
| --- | --- |
| `customer_iam/internal/application/SelectedCustomerWorkspace.java` (new) | Carrier record only; no throwing structural validation in constructors, persistence or role fields. |
| `customer_iam/internal/application/TrustedCustomerCaller.java` | Add selectedWorkspace record component and compatibility constructor. |
| `customer_iam/internal/security/VerifiedCustomerSession.java` | Add selectedWorkspace and compatibility constructor. |
| `customer_iam/internal/security/CustomerSessionVerifier.java` | Clarify attestation contract only; same verify method, no production implementation. |
| `customer_iam/internal/security/CustomerBearerAuthenticator.java` | Preserve existing auth checks; carry selection unchanged without structure rejection; no client reads beyond Authorization. |
| `customer_iam/internal/application/CustomerContextException.java` | Append CURSOR_INVALID only. |
| `customer_iam/internal/web/CustomerBoundaryFilter.java` | Exact two-path GET allowlist only. |
| `customer_iam/internal/web/CustomerProblemWriter.java` | CURSOR_INVALID 400/title mapping only. |
| `customer_iam/internal/web/CustomerSharesController.java` (new) | `getCustomerShares(Principal,HttpServletRequest)` checks trusted principal; private `requireSelectedWorkspace(TrustedCustomerCaller)` validates after filter Accept, before raw page parsing/provider resolution; explicit exception translation/handlers using existing writer/trace. |
| `customer_iam/internal/web/CustomerSharePage.java` (new) | Frozen wire page adapter, nullable nextCursor included; no duplicated ShareSummary. |
| `customer_iam/internal/web/CustomerShareCursorCodec.java` (new) | The exact stateless encode/decode/filterBinding/anchor-validation helpers in §4.1. |
| `customer_iam/package-info.java` | Single named-interface dependency only. |
| `sharing/internal/application/package-info.java` (new) | Export customer-query named interface only. |

No edits to `CustomerContextService`, its repository/SQL, `CustomerMeController`, `CustomerRequestPrincipal`, `CustomerWebConfiguration`, error controller, or sharing application/service/repository/SQL/configuration are needed. Sharing's module declaration remains unchanged; customer_iam's declaration has the intended one-line `allowedDependencies = {"sharing::customer-query"}` change. Customer internal source-file inventory becomes **23** (19 existing + four new Java files); sharing internal inventory becomes **13** (12 existing + package-info). Preserve exact source lists, not an unbounded count increase.

Test/support/configuration/doc allowlist:

- `backend/src/test/java/com/onecall/core/customer_iam/internal/security/CustomerSessionBoundaryTests.java`: add the two methods below; expand current malformed-result/redaction coverage while retaining all five existing methods.
- `backend/src/test/java/com/onecall/archtests/CustomerMeWebTests.java`: remove only shares from old nonexistent-route assertion, replace with an unimplemented customer path; expand existing methods for `/me` compatibility and the appended writer code. Keep all eight methods and existing status/negotiation behavior.
- `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java`: expand existing six methods for absent shares bean dependencies/default startup and exact provider qualifier; do not add methods or assume sharing graph exists in web-only profile.
- `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`: exact map/edge/named-interface changes and the two concrete model/source-policy assertion homes in §3 (`verifiesNoCyclesOrBoundaryViolations()` and `rejectsInternalPackageAccessFixture()`); retain eight methods and all negative fixtures, with no new fixture/source file.
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`: keep all three existing methods, pin new controller/page/session/caller shapes and 23-file inventory; replace blanket sharing-import ban only with exact new web-file/type allowlist. In existing `profileAndVerifierArtifactsCannotEscapeIntoProduction()`, add `web/CustomerSharesController.java` to the existing internal-relative file list (production path `customer_iam/internal/web/CustomerSharesController.java`) so the existing source assertion pins `@Profile("web")` and `ConditionalOnWebApplication.Type.SERVLET` on the new controller as well. Preserve every other ban and unchanged `/me`/SQL checks; no new method/class.
- `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`: keep one method; add package-info to exact 13-file list and assert named export. Preserve all public PA10 signatures, SQL/projection assertions and web/customer/reverse-import bans.
- New `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerShareCursorCodecTests.java`: six methods in the codec's package for ordinary package-private access. CI FQCN is `com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests`. No test bridge, reflective accessibility or production-public codec for tests.
- New `backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java`: ten socket/web methods using test-only verifier/service mocks and random-port server, no DB required. Nested test-only `SerializationFailureTestConfiguration` installs a Jackson module/serializer for `ShareSummary` only in a dedicated fault web context: a mocked service returns a deterministic sentinel shareId; its serializer writes a field then throws a deterministic serialization exception. Delegate nonsentinel values to the normal serializer. Construct and close this context programmatically inside `negotiationAndBufferedFailuresPreservePa11Boundary`, using the existing `CustomerMeFixture` `SpringApplicationBuilder(OneCallApplication.class, ...).profiles("web").properties("server.port=0", ...).run()` pattern with explicit test-only verifier/mock/fault configuration; close it independently of the normal context. The nested configuration is support only, with no tests. Do not use `@Nested`, a second `@SpringBootTest` test class, or any mechanism emitting an extra Surefire report/class. Never activate the module in normal wire tests or production. This makes the existing method prove clean 500 Problem/no partial success or sentinel/exception leakage. No production hook or record modification.
- Mapper support stays inside the two allowlisted new codec/web test files: duplicate the same literal normal web-mapper builder/configuration in `CustomerShareCursorCodecTests` and `CustomerSharesWebTests`, matched to the actual web mapper and excluding the fault module. Do not rely on a package-private helper crossing `com.onecall.core.customer_iam.internal.web` / `com.onecall.archtests`, add a fifth new test/support source file, or expose a production test bridge. This support adds no test/report class and keeps the 13-class Surefire inventory.
- New `backend/src/test/java/com/onecall/archtests/container/CustomerSharesIT.java`: eight methods against PostgreSQL 16 and real HTTP/query service.
- New `backend/src/test/java/com/onecall/archtests/container/CustomerSharesFixture.java`: test-only composition of existing CustomerMeFixture provisioning plus PA10-style valid evidence/share seed SQL. Start/migrate once, close initial web context before starting dual context; use fresh per-method IDs and explicit session-map reset, no parallel static state. Do not modify existing fixture sources or old IT method sets. Test-only admin/owner seeding is never request-path authority. Implement these package-private hooks in this new file (nested support types stay here):
  - `startSharesContext(boolean persistence, String sharingLogin)`: build a new random-port `OneCallApplication` context with explicit test-only verifier configuration/session map, separate customer/sharing credentials, and `web` or `web,persistence`. Use the existing fixture's provisioned customer, sharing and denied login identities. Passing deniedLogin for sharing exercises role denial without membership changes; `persistence=false` omits the sharing graph. Reset sessions per context, close each context/pools before replacement, and keep customer credentials healthy for `/me` comparison. A second verifier/fault context is built explicitly, not by mutating a live bean graph.
  - `capturePoolsByBeanName()`: a test-owned initializer/BeanPostProcessor maps both `customerQueryDataSource` and `sharingQueryDataSource` to their underlying Hikari sources before proxying, including dual-profile mode (do not rely on CustomerMeFixture's web-only `pools` map). Per-bean DataSource/Connection/PreparedStatement proxies record checkout counters, backend pid, session_user/current_user, read-only/isolation, SQL/bind values, commit/rollback and state reset. Use fixture-owned interception for statement/disconnect/commit faults and a deterministic ResultSet mapping exception, not production hooks.
  - `snapshotSharingAndEvidenceRows()`: owner connection, fixed test-only table allowlist and deterministic sorted full-row JSON/hash using the existing ordered `to_jsonb(row_value)::text` pattern. Include all three V009 tables `sharing.evidence_share_grant`, `sharing.share_admission`, `sharing.share_access_event`, `evidence.media_object`, `evidence.recording`, `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.evidence_event`, and existing identity/organization, outbox/idempotency/audit snapshot tables. Assert snapshot key coverage against migrated table metadata; compare contents before/after each HTTP operation, including rows/revisions/events. Seeding/lifecycle changes occur outside the measured interval. These are test DB assertions, not production snapshots.
  - `assertSharingReadOnlyWriteDenied()`: independent test-owned sharing-login connection, `setReadOnly(true)` before beginning a non-autocommit transaction, `SET LOCAL ROLE sharing_role`, assert current_user and transaction_read_only, then one otherwise permitted write (`UPDATE sharing.evidence_share_grant SET revision=revision+1 WHERE share_id=?` on a seeded row). Require SQLSTATE 25006, rollback, restore read-only/autocommit and assert clean state/unchanged rows. Do not inject this into `CustomerShareQueryService`; pair it with real HTTP-captured read-only transaction facts. Run 42501 ACL/foreign-column/write/lock/other-role probes separately in autocommit so aborted transactions cannot mask subsequent ACL results.
- `backend/pom.xml`: add exactly `**/CustomerSharesIT.java` to container-smoke Failsafe includes; no dependency/plugin/profile activation change.
- `.github/workflows/ci.yml`: exact new method/class inventories and totals, no firewall relaxation or new endpoint in default smokes.
- `backend/README.md`: update the explicit adapter/module exception, profiles/roles/session attestation, exact tests and commands. No root README or unrelated documentation edit.

Dependency order within S1: attestation contracts + named seam; HTTP/filter/page/codec/error adapter; unit/web/boundary assertions; real-DB evidence; CI/README inventory. These are implementation steps of one end-to-end behavior, not independent slices or gate transitions.

## 8. Exact S1 acceptance tests

No parameterized/dynamic/repeated/conditional/disabled tests. Use ordinary @Test methods and table loops within methods, preserving CI's exact source/XML inventory model.

### 8.1 Surefire: 18 additions, 67 methods / 13 classes total

Add to existing `CustomerSessionBoundaryTests` (five to seven methods):

1. `attestedWorkspaceIsCarriedWithoutClientAuthority`: PERSONAL and ORGANIZATION carry unchanged through verifier → caller → principal; no token retention, role field or attribute trust; legacy constructors preserve `/me` input shape.
2. `workspaceStructureIsCarriedWithoutChangingAuthentication`: null, null workspaceType, PERSONAL with organizationId and ORGANIZATION without organizationId are carried unchanged by authenticator/caller constructors, not rejected there; wrong audience still 401 first, other existing malformed verifier fields retain their failures. Shares-only 503/no fallback/no service calls are asserted in existing web methods, not as an authenticator rejection.

New `com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests` (six methods):

1. `personalAndOrganizationCursorsRoundTripCanonicalAnchors`: negative/positive epoch, UTC/microseconds, exact trusted caller reconstruction and deterministic wire.
2. `cursorBindingRejectsOtherAccountsAndWorkspaces`: other account, PERSONAL↔organization, organization A↔B fail; same scope/different limit succeeds.
3. `malformedAndNonCanonicalCursorsAreRejected`: missing bytes/version, bad base64/padding/trailing bytes/pad bits, wrong digest, length/count, offset/raw key, NUL/all-space/invalid scalars and malformed Unicode inputs.
4. `anchorBoundsMatchPa10Validation`: PostgreSQL year edges, nanos precision/range and shareId constraints; internal invalid anchors fail INTERNAL, request invalid anchors CURSOR_INVALID.
5. `maximumUnicodeShareIdsFitFrozenWireBound`: 128 ASCII, BMP and supplementary scalar IDs, mixed IDs, exactly 382 maximum payload bytes and 510 maximum wire characters, no truncation; deterministic full round-trip corpus.
6. `wirePageHasOnlyFrozenFieldsAndNullableCursor`: normative empty/tail/lookahead serialization against actual web mapper configuration (without fault module), nextCursor property present as string/null not internal object/Optional, exact 11 row fields, nullable maxAdmissions property present, ISO instants preserving whole seconds and microseconds, all four modes/statuses, forbidden-field canaries absent. Duplicate the literal normal web-mapper builder/configuration in both new tests as required by §7, so package boundaries need no inaccessible helper or additional source file; no production test bridge.

New `CustomerSharesWebTests` (ten methods):

1. `authenticatedPersonalAndOrganizationGetsUseTrustedCreatorScope`: exact URI/status/media/JSON, only one qualified query invocation, caller from selection, lookahead-derived cursor.
2. `missingInvalidAndDuplicateBearersFailBeforePageParsing`: unauthenticated plus bad limit/cursor/Accept, wrong audience, malformed verifier, missing/ambiguous/outage verifier; exact 401/503 and zero query calls.
3. `clientSelectorsAndRequestAttributesCannotOverrideTrustedScope`: query/header/cookie/body account/org/owner/role/channel/selection canaries, spoofed request attributes/principal bypass attempt, no authority changes; legacy null-selection shares returns 503.
4. `limitAndCursorValidationHaveDeterministicPrecedence`: default/1/100/leading zeroes, invalid/duplicate limit/cursor, 512 boundary, changed page size, cross-context cursor, no DB provider resolution for rejected input.
5. `queryErrorsUseExactClosedProblems`: all five PA10 typed errors plus query-bean absence/ambiguity/construction failure/unexpected exception; no partial page or exception text, 401 header only where appropriate, server UUID trace, security/cache headers; reserved 429 directly via writer, no fake HTTP producer.
6. `onlyTwoExactGetPathsPassHighestPriorityFilter`: both paths accepted, all methods/aliases in §6 denied with zero verifier/query calls, application context prefix handled exactly, direct `/error` denied; HEAD body may be empty per servlet behavior but is 404, never authenticated success. Inspect exactly one highest-priority REQUEST registration. Within this same method, programmatically start and close a separate `SpringApplicationBuilder(OneCallApplication.class, ...)` context with explicit test-only verifier/service mocks, profile `web`, `server.port=0` and `server.servlet.context-path=/pa12`; use §7's fixture pattern, no persistence or fault serializer. Send authenticated GETs to `/pa12/api/v1/customer/me` and `/pa12/api/v1/customer/shares`, require 200 and the frozen JSON with exact verifier/query call counts (no sharing invocation for `/me`). Also exercise prefixed aliases, non-GET paths and `/pa12/error`, require 404 and zero verifier/query calls after resetting mock counts per case; unprefixed customer URLs must not reach either boundary. No new method, class/report or test-visible production hook; the suite remains ten methods.
7. `meRemainsCompatibleWithLegacyAndSelectedSessions`: same frozen response/order/status with null/PERSONAL/now-inaccessible ORGANIZATION selection and each invalid non-null structure (null workspaceType, PERSONAL with organizationId, ORGANIZATION without organizationId), selected field absent, no sharing invocation/provider resolution on `/me`; missing/disabled account still 401 independent of selection. Expand existing CustomerMeWebTests compatibility assertions likewise without adding methods.
8. `concurrentRequestsKeepAccountAndSelectedWorkspaceIsolated`: interleave accounts and both workspace kinds with one immutable caller per request, cursor binding cannot leak via shared state; bounded executor/future teardown.
9. `negotiationAndBufferedFailuresPreservePa11Boundary`: JSON/* success, text/html/malformed/q=0 500 after auth, no Accept bypass. Fold in valid bearer + each invalid non-null selection + incompatible/malformed Accept → 500, versus acceptable Accept → 503; null selection follows the same shares-only precedence. Assert zero query provider resolution/calls, including bad page input that must not win. Build/close the dedicated fault context programmatically via §7's `SpringApplicationBuilder` random-port pattern within this method, not `@Nested` or another `@SpringBootTest`/Surefire report class. Test-only sentinel throwing serializer/module above proves serialization failure discards partial success; downstream failure/genuine error dispatch sanitized and direct `/error` blocked. No extra method.
10. `webWithoutSharingPersistenceFailsClosedAndDefaultStaysDatabaseFree`: test-owned verifier + web-only shares 503, `/me` unchanged; default context has no servlet/customer/sharing pools or verifier; no-production-adapter packaged behavior remains fail-closed.

Retain all baseline 49 methods; modify only assertions needing this intentional scope extension. In particular, existing writer-only test must account explicitly for appended CURSOR_INVALID 400/title instead of relying on the old six-enum ordinal list.

### 8.2 Failsafe: eight additions, 92 methods / four classes total

New `CustomerSharesIT`:

1. `authenticatedHttpListsPersonalAndOrganizationCreatorMetadata`: migrated PG16 → real server/filter/verifier → real PA10 service; same account's personal/org plus another creator in same organization and organization B; exact SQL-backed expected order and JSON. All roles remain creator-scoped, not organization-wide.
2. `httpPaginationPreservesTiesAndConcurrentInsertOrder`: limit=1 and multirow pages, equal validFrom ties including non-ASCII share IDs; compare to PostgreSQL ordered baseline, not Java sorting. Round-trip cursor via percent-encoded query; newer inserted row after page one does not duplicate prior rows or move continuation backwards; empty/tail/null cursor correct.
3. `httpCursorsCannotCrossTrustedAccountOrWorkspace`: reuse real returned cursor across accounts/workspaces; selector tampering cannot redirect; invalid local page input acquires zero query connections. Valid cursor plus reselected server workspace still fails binding.
4. `committedAccountMembershipAndOrganizationChangesDenyNextPage`: same still-valid session/cursor, commit ACTIVE→disabled account, membership revoked/suspended, organization inactive; next valid page 403 and no share fetch; missing contexts also 403. Validated page precedes lifecycle denial. `/me` retains its separate 401/active-workspace projection behavior.
5. `customerAndSharingConnectionsRemainSeparatelyOwned`: alternate/concurrent `/me` and `/shares`; capture session_user/current_user, checkout/pid, read-only and read committed; no customer checkout for shares, no sharing checkout for me, no login can SET ROLE into the other's role; exact bean qualifiers and state reset after commit/rollback.
6. `existingAclsAndReadOnlyTransactionsForbidWritesAndForeignReads`: real role autocommit SQL 42501 probes for undeclared columns, foreign writes/locks, other-role SET ROLE, each separate from read-only probing. Invoke test-owned `assertSharingReadOnlyWriteDenied()` for independent read-only transaction + SET LOCAL ROLE sharing_role + one permitted sharing write → 25006/rollback; do not claim a write was injected into the application query transaction. Pair with real HTTP-captured read-only facts. Reuse frozen V002/V009 ACL inventories without new grants; query prepared placeholders, no inline selector or secret SELECT.
7. `successfulAndFailedHttpPagesNeverWriteOrProjectSecrets`: before/after DB row hashes for success, 400/401/403/422/503/500; no mutations/events/count/revision changes. Canary token/code/hash/salt/object key/media/permanent URL/private identity fields absent from HTTP/logs/throwables/SQL projection. Statement failure, mapping failure and commit failure return no page and restore pool state.
8. `missingRolesVerifierAndDatabaseFailuresStaySanitized`: denied-role sharing login, missing/query graph case, verifier outage, disconnected pool/statement/commit errors → sanitized 503 or typed INTERNAL for nontechnical mapping fault; exercise recovery and pool reuse. No SUPERUSER/owner fallback and no sharing-role failure changes `/me` when its customer graph is healthy.

Fixture assertions: migrate/validate exactly V001–V009 (nine successful history rows), PostgreSQL major 16, record collation/timezone/image ID, test-owned bounded timeouts/cleanup, owner-only lifecycle changes with revision protocol, independent one-role logins. Keep all 64 PostgresContainerIT, ten SharingQueryServiceIT and ten CustomerMeIT methods unchanged. Existing separate-role evidence is supplemented, not replaced by superuser-only HTTP tests. No skipped-container substitute counts as execution.

## 9. CI, commands, documentation and residual ownership

CI exact changes: Surefire `49 → 67`, classes `11 → 13` (existing session class +2; codec +6; web +10); Failsafe `84 → 92`, classes `3 → 4` (`64+10+10+8`); combined `133 → 159`. Add the literal new method sets/FQCNs from §8 to `unit_expected`/`container_expected`, update every aggregate assertion and summary string, and keep source=CI=XML equality, duplicate/missing-class/method detection, zero failures/errors/skips and ban on conditional tests. Do not change the 64-method migration literal set or the two ten-method container sets. Retain 30-minute job ceiling and Docker-required preflight.

Exact current aggregate sites in `.github/workflows/ci.yml`: **both** Surefire sites at lines **162-163** (count and error string, 49 → 67) and **284** (`exact_inventory(..., unit_expected, 49)` → 67); Failsafe at **285** (`exact_inventory(..., container_expected, 84)` → 92); summary string at **286** (Surefire 67; Failsafe `64+10+10+8=92`; combined 159). Preserve the frozen 64-method migration literal set at `.github/workflows/ci.yml:79-144`, its source/XML/count assertions at `.github/workflows/ci.yml:147-159`, and both existing ten-method container sets byte-for-byte; enumerate the eight additions separately.

No new dependency: keep PA11 firewall baseline 37/34, web 55/48, exact 47 counterparts, eight dependency-only entries, one packager JAR, seven accepted legacy-regex collisions and **82 adversarial probes** unchanged. Named-interface and codec use existing Modulith/JDK APIs only. Retain Security/JPA/Flyway-prod/Testcontainers-prod/test-fixture/production-verifier bans and dependency tuple/JAR identity matching. Do not permit fixtures because of new package names. Default and opt-in web packaged-JAR smokes must remain no-verifier/no-pool, and unauthenticated shares now returns the same closed 401 as me rather than 404; update only that expectation if asserted. No change to the frozen dependency coordinates, .java-version, wrapper, application.yml, application-web.yml or application-persistence.yml.

Current-state README sites verified against the source and to be updated only during authorized S1: `backend/README.md:39` (current validation inventory and container command breakdown: 67/13 + 92/4, `64+10+10+8`, 159); `backend/README.md:265` (customer_iam row `{}` → `{"sharing::customer-query"}`); `backend/README.md:280` (discovered production edges no longer empty; exact named edge/composition assertion wording, not a broader map relaxation); `backend/README.md:289` (java-runtime CI inventory: 67/13 + 92/4, 159); `backend/README.md:298` (current CI/evidence boundary and new eight-method container set, while retaining PA11 historical contribution). Preserve historical checkpoint statements at `backend/README.md:226`, `backend/README.md:303`, `backend/README.md:311` and `backend/README.md:320`; their old totals describe earlier evidence, not the current aggregate. No README edit occurs in this fix.

Commands below are **implementation validation instructions**, not commands executed during this planning task. Run in an environment with a working Linux/Windows Git worktree and the pinned toolchain; read Git state only. Focused checks do not replace the full run:

```bash
cd backend
java -version
./mvnw -B -ntp -v
./mvnw -B -ntp -Dtest=CustomerSessionBoundaryTests,CustomerShareCursorCodecTests,CustomerSharesWebTests,CustomerMeWebTests,CustomerWebConfigurationTests,ModuleBoundaryTests,CustomerApiBoundaryTests,SharingQueryBoundaryTests test
./mvnw -B -ntp clean verify
docker info
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerSharesIT verify
./mvnw -B -ntp clean -Pcontainer-smoke verify
```

Focused `-Dit.test` run is deliberately not a full-inventory pass. After the full clean run, execute the exact Python heredoc bodies from `.github/workflows/ci.yml`'s “assert container smoke actually ran”, “default and opt-in web jar smokes”, and production-JAR firewall step in `backend/`; require 67/92/159 source/CI/XML and all 82 firewall probes. Do not substitute source counts for actual XML. Preserve logs and role/ACL/cursor test evidence in later implementation reporting, not in this plan as purported results.

Frozen-contract regression commands (from repository root, Node v22.23.4; output/generated assets must stay outside any prohibited source write set):

```bash
cd api/openapi && npm ci && npm run check:s3 && cd ../..
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
```

Run the exact remaining CI bodies without abbreviating A5 argument lists. This repository-root standard-library helper also supplies runnable local commands for the three Java evidence steps described above; run its first invocation only after full container verification, and its second after the contract prerequisites above:

```bash
run_ci_steps() {
  python3 - "$@" <<'PY'
from pathlib import Path
import subprocess
import sys
import textwrap

lines = Path('.github/workflows/ci.yml').read_text().splitlines()
for wanted in sys.argv[1:]:
    matches = [index for index, line in enumerate(lines) if line == '      - name: ' + wanted]
    assert len(matches) == 1, wanted
    start = matches[0] + 1
    while start < len(lines) and lines[start] != '        run: |':
        assert not lines[start].startswith('      - '), wanted
        start += 1
    assert start < len(lines), wanted
    end = start + 1
    while end < len(lines) and (not lines[end].strip() or lines[end].startswith('          ')):
        end += 1
    script = textwrap.dedent('\n'.join(lines[start + 1:end]))
    directory = 'backend' if wanted in {
        'assert container smoke actually ran', 'default and opt-in web jar smokes',
        'production jar dependency firewall',
    } else '.'
    subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', script], cwd=directory, check=True)
PY
}
run_ci_steps 'assert container smoke actually ran' 'default and opt-in web jar smokes' 'production jar dependency firewall'
run_ci_steps 'A5 s1' 'A5 s2'
git diff --check
```

Do not edit contract models/fixtures to pass. Before handoff, protected-path hash/diff comparison must show V001–V009, OpenAPI, docs/11/12/14/15/17 and default/web/persistence config (`application.yml`, `application-web.yml`, `application-persistence.yml`) unchanged. Hash comparison is preferable when line-ending-sensitive Git views are unreliable.

GitHub validation, **only after an authorized later controller push**: inspect exact-SHA runs with `gh run list --workflow ci.yml --commit "$IMPLEMENTATION_SHA"` and `gh run view "$RUN_ID" --json headSha,status,conclusion,jobs`, then `gh run view "$RUN_ID" --log`. Require headSha equality and SUCCESS for both `java-runtime` and `contract-regressions`, including literal test inventory, JAR smokes/firewall and A1–A5 evidence. A missing/not-started run is NOT RUN, not passed. These commands do not authorize push, PR creation or review at this gate.

Documentation decision: original plan gate wrote only this plan. Historical fixes added `docs/gateflow/pa12-customer-share-list-api-plan-fix-1.md` and `docs/gateflow/pa12-customer-share-list-api-plan-fix-2.md`; their provenance remains unchanged. Current bounded fix-3 may revise only this plan and add `docs/gateflow/pa12-customer-share-list-api-plan-fix-3.md`. Future authorized S1 updates backend/README and CI inventories as listed, not architecture/design/security/OpenAPI/control/history documents. The composition exception remains visible through implementation/PR gates; docs/12 does not already permit it and is not edited here. Controller owns subsequent checkpoint/provenance records; request authorization if a design-document amendment becomes necessary.

| Residual / owner | Destination and nonblocking boundary |
| --- | --- |
| Provider-neutral session issuance/storage, selecting trusted workspace, TTL/revocation — Customer IAM/Identity + Security (Q1/Q19) | Future verifier adapter/authentication integration. It must populate selection from trusted session state; production remains fail-closed without it. No request selector or production adapter in S1. |
| Bounded customer-web → sharing-query composition exception and existing Boot/Spring Security stack reconciliation — Architecture + Gateflow controller | Future authorized docs/12/runtime architecture reconciliation. The exact map/seam in this plan is the current reviewable decision, not a runtime ambiguity or permission to edit docs/12 now. |
| Production DB logins/provisioning, image digest pinning — Platform/DBA (Q17/Q23) | Deployment hardening; S1 only proves existing ACLs using test-owned separate-role identities on postgres:16. |
| Rate-limit values, denial/audience-mismatch audit, connector error normalization — Security/Platform + audit integration | Retain PA11 deferred SECURITY_AUTH_DENIED/SECURITY_AUDIENCE_MISMATCH and A5-IDENT-PERMISSION-DENIED-01 obligations; no write added to satisfy them. Reserved 429 is not policy implementation. |
| In-flight revocation timing, optional trace emission, source-text guard coupling, JAR reproducibility — Backend/Security, Observability, QA/CI respectively | Existing READ_COMMITTED semantics and PA11 residual destinations; no new locking, telemetry subsystem or reproducible-build promise. |
| Unsigned opaque query cursor is not an authentication credential — Backend/API governance + Security | Current v1 binds normalized scope and relies on mandatory server predicates. Signing/encryption/TTL would require a separately authorized cursor policy and key lifecycle, not a hidden session/security implementation. |
| No creator/workspace/sort supporting list index — Backend/Database/Platform capacity | Deferred capacity assessment of creator/workspace predicates and `valid_from DESC, share_id DESC`; small PG16 fixtures are not production capacity evidence. Future index/migration work needs separate authorization; none is added in S1. |
| Local Windows Git metadata and Java launcher incompatibility — workspace controller/environment owner | Run later executable checks in the pinned functional environment. Do not repair .git, JDK, wrapper or host configuration within plan scope. |

No blocking product/open question remains: route ownership, selected-workspace semantics, cursor format, HTTP mapping/precedence, ACTIVE rechecks, connection roles and inventories are decided here. Residuals have owners/destinations rather than unowned implementation choices.

## 10. Plan-only validation and stop condition

Planning actions actually performed: read the listed source/spec/control/test/CI evidence; inspect source test inventories and exact current roles/qualifiers; capture a pre-write filesystem SHA-256 manifest outside the repository. `git status --short` failed because `.git` points to a Windows-style `D:/codex-call/.git/worktrees/codex-call10` path unusable from this Linux worktree. `java -version` failed with a WSL socket/launcher error. No Git metadata or toolchain repair was attempted. No Maven, Docker daemon/container, HTTP, npm, GitHub or production validation was run; no runtime pass is claimed.

The original plan-only validation intended to check artifact structure, arithmetic (49+18=67, 84+8=92, total159; 382 bytes→510 base64url characters), exact new test inventories and a full-tree filesystem manifest. That intended equality condition is not a globally clean provenance result. Fix-1 disclosed separately enumerated concurrent drift: seven unrelated additions, `tmp/env-config/agent-tools.zsh`, `tmp/env-config/codex-agent-run`, `tmp/env-config/qwen38max-config.toml`, `tmp/env-config/sub-agents-SKILL.md`, `tmp/install_qwen38max.sh`, `tmp/probe_qwen_max.sh`, `tmp/probe_qwen_models.sh`, plus later control-document bytes. The fix-1 full-tree comparison must be read with that disclosure, not as “no other changes.” This fixer neither attributes those writers nor deletes/edits/reverts `tmp/`, control, Git metadata or user/private data. Filesystem comparison cannot substitute for unavailable Git-head/index provenance; no global clean-tree claim is made.

For fix-2, compare an external pre-write full-tree SHA-256 manifest to the final tree, distinguishing the authorized author outputs (this plan modified and fix-2 added) from any separately enumerated concurrent drift. A locally unchanged non-output set during this interval does not erase fix-1's drift or establish Git provenance. Verify all ten structural sections, 2+6+10=18 Surefire additions and eight Failsafe additions, unchanged totals/classes/cursor bounds, concrete discovery-method/support/CI-site obligations, and no trailing whitespace in either output. Record the five frozen input hashes and dispositions in fix-2; hash both final outputs externally without embedding either final output hash in the outputs. No runtime validation or implementation is authorized by this fix.

Non-goals/forbidden drift: no share creation/revocation/rotation/admission/public-share/media/provider/login/logout endpoint, production verifier/session store, permission-matrix invention, migrations/grants/schema/status/index changes, mutations/audit writes, frontend, architecture/security/OpenAPI edit, Spring Security enablement, generic cursor framework, fifteenth module, copied sharing query/projection snapshot, new persistence graph, reflection or request-attribute authorization. The small wire adapter and bounded codec exist only to bridge the frozen HTTP contract to the accepted PA10 types. One end-to-end read-only slice is the minimal complete behavior.

For current fix-3, compare the external pre-write repository regular-file SHA-256 manifest with the final interval manifest: only this plan may change and fix-3 may be added. Exclude `.git`, `tmp/` and symlinks from this scan; no claim is made about those excluded paths, Git head/index or global cleanliness. Preserve the historical fix-1/fix-2 disclosures above. Recheck five frozen inputs (the plan against its pre-write copy), all ten sections, unchanged 26 method names and 67/13 + 92/4 (`64+10+10+8`) = 159, cursor bounds and closed scenarios; correlate README/CI citations with actual source and check both outputs for trailing whitespace. Publish both output hashes externally, never embed self-referential final hashes.

Original planning, fix-1 and fix-2 completion records remain historical `status: plan-complete`, `status: plan-fix-1-complete` and `status: plan-fix-2-complete`. Current fix-3 stops after these two documents and plan-only verification, with `status: plan-fix-3-complete`, gate `fix`. Do not implement, stage/commit/push, create a PR, dispatch reviewers, enter re-review or advance the gate. Gateflow controller owns the next authorized action and any independent provenance reconciliation.
