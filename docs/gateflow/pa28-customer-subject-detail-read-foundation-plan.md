# PA28 Customer Subject-Detail Read Foundation — Plan

## 1. Authority, status, and immutable goal

- Work unit: `PA28-customer-subject-detail-read-foundation`.
- Original task: `pa28-subject-detail-plan-gpt-20261111-01`; authored 2026-11-11. Cumulative candidate revised by `pa28-plan-fix1-gpt-20261111-01` on 2026-11-11 at gate `fix`, applying only Controller-accepted `PA28-PLAN-REV-01`–`07`.
- Authority: active PA28 goal-confirmation checkpoint in `docs/gateflow/project-execution-control.md`, followed by the user's binding confirmation. The checkpoint records merged PA27 at `3f534ac58c6471f139dacb551ea438cc20205dc4`. This environment's Git worktree pointer resolves to an unavailable Windows Git directory; that commit is Control-attested, not independently reverified here. No Git metadata is repaired or changed.
- This document is a proposal, not implementation acceptance, review disposition, a migration execution report, or permission to advance gates. This bounded fix writes only this cumulative plan and `docs/gateflow/pa28-customer-subject-detail-read-foundation-plan-fix-1.md`; source implementation and entry into re-review remain unauthorized.

Implement **only** `GET /api/v1/customer/subjects/{subjectId}` / `getCustomerSubject`. Success serializes exactly `subjectId`, `activationStatus`, `evidenceCount`, `orderCount`. Preserve the OpenAPI and V001–V015 bytes. Do not add `claimState`; it is not in `CustomerSubjectDetail`. Do not serialize even null-valued `numberMasked`, `trial`, or `consentRequirements`. No business write, other endpoint, client-selected authority, or new lifecycle is authorized.

One composed S1 slice is sufficient: verified customer request → current lifecycle → owned subject → current binding risk → activation and two counts → four-field response, all database reads on one customer-role repeatable-read snapshot. Owner query seams, the necessary privilege delta, regressions, inventories, and bounded current-boundary documentation are part of this same verifiable behavior, not independent delivery slices.

## 2. Evidence and first-principles decisions

### 2.1 Frozen contract and architecture

Read sources: `docs/12-technology-stack-and-system-architecture.md` module ownership, persistence/transaction and security sections; `docs/14-production-data-model-and-flyway-schema-spec.md` current V015 boundary, §§4.12–4.15, 4.20, 4.31–4.32, 4.36–4.38, 4.41 and ACL contracts; `docs/15-production-openapi-surface-spec.md` operation/model and summary tables; `docs/17-production-security-design-spec.md` current boundary, authorization and sensitive-data rules; the active Control checkpoint; `api/openapi/onecall-v1.openapi.yaml` operation at lines 391–470 and schema at 4509–4533.

The operation's prose mentions a larger future projection, including claim state, trial and consent. The closed schema requires only four fields and has no claim-state property. The confirmed PA28 goal selects that required subset; do not reinterpret prose as authorization to extend the frozen schema. Missing activation is a projection default, not an activation insertion. Counts are row counts, not content authorization or status-filtered lists.

Current production implementations inspected include customer authentication/context, boundary/controller/configuration and PA27 query flow; subject-number and activation customer-query adapters; sharing's evidence projection and evidence media readers; module descriptors; migrations V003/V004/V006/V007/V008/V014/V015; PA25 provisioning/check SQL; corresponding web/service/architecture/PostgreSQL fixtures and tests. `catalog_order` currently contains only `ModuleMarker.java` and its module descriptor: there is no order-read implementation to reuse. Entitlement/trial persistence exists, but there is no required PA28 trial projection. Reading consent, claim, entitlement or policy data would add unsupported behavior and unnecessary privileges.

### 2.2 Reuse and minimum additions

- `CustomerBoundaryFilter`, `CustomerBearerAuthenticator`, `VerifiedCustomerSession`, `CustomerRequestPrincipal`, `TrustedCustomerCaller` and `SelectedCustomerWorkspace` already establish verified account/selected workspace. Reuse them; no new session adapter, issuer, TTL or principal convention.
- `JooqCustomerBusinessNumberAccessRepository.requireActiveContext(Scope)` already enforces ACTIVE account, ACTIVE selected organization, ACTIVE membership and the exact four roles. Reuse it unchanged. Its name is historical; do not introduce a second lifecycle policy or rename PA27 classes for this feature.
- Reuse `CustomerBusinessNumberQueryService.requireScope(caller)` solely as the existing trusted-workspace shape validator and `CustomerBusinessNumberReader.Scope` as its account/organization value. This static method performs no list query and has no cursor/key dependency. PA28 must not call `listCustomerBusinessNumbers` or instantiate its cursor codec. This bounded reuse avoids a new generic context framework; extracting a neutral abstraction is deferred.
- Reuse `CustomerActivationStatusReader.findStatuses(Set<UUID>)`, with a singleton subject set, its existing qualified bean and owner implementation. The existing `(subject_id,status)` grant suffices. No activation interface/configuration/SQL change is necessary.
- New subject ownership/risk and evidence/order scalar query seams stay inside their semantic owners. The orchestrator must not execute their SQL or import owner internals. All adapters use the existing `customerQueryDsl`, never the evidence/media/sharing/command pools.
- Customer's existing dedicated PA27 transaction template establishes the correct pattern. Add an endpoint-named template rather than mutate any shared template or change `/me` isolation.

## 3. Behavior and error precedence

### 3.1 Authentication and request closure

Add exactly the GET path to the existing boundary. The segment pattern is `/api/v1/customer/subjects/[^/;%]+`; preserve the existing `contextPath + servletPath == requestURI` safeguard. Wrong prefixes, extra path segments, trailing slash, empty/multiple segments, encoded/semicolon paths and non-GET methods (including Spring's otherwise implicit HEAD) remain 404. Admit a single non-slash malformed segment through authentication so a verified malformed UUID receives 422 rather than a new route oracle. In particular `<uuid>.json` is one malformed subject value (422), not an extra-segment route (404); `<uuid>/json` is an extra segment (404). Do not add POST/activation/claim routes.

`CustomerSubjectsController.getCustomerSubject(Principal, String, HttpServletRequest, HttpServletResponse)` uses `@PathVariable("subjectId") String`, not Spring's default UUID conversion/error page. Require `CustomerRequestPrincipal`, take only `principal.caller()`, validate trusted workspace shape, then validate the path as the full canonical hyphenated 8-4-4-4-12 hexadecimal UUID format and parse it. Uppercase hexadecimal is accepted; abbreviated `UUID.fromString` forms, whitespace, dotted values and other malformed forms are not. Extra query parameters (including arbitrary/duplicate owner, account, workspace, role and cursor selectors) and nonempty GET bodies are non-authoritative and ignored, matching `/me`; do not inspect, bind, parse or reject them. Fixed-length/chunked and JSON/non-JSON bodies cannot select scope or alter projection. Headers purporting to supply account/workspace/role are also ignored as authority. No idempotency key or cursor key is required.

Use a qualified lazy `ObjectProvider<CustomerSubjectDetailQueryService>` so route/authentication/validation failures cannot activate a database pool. Match existing `CustomerProblemWriter`, trace/cache/security headers and controller exception handlers. Do not change shared error codes, messages or global serialization settings.

The service returns `Optional<CustomerSubjectDetailQueryService.Detail>`. Only a completed `Optional.empty()` representing an absent/inaccessible owned subject becomes 404: the controller calls the existing `CustomerProblemWriter.notFound(response, CustomerBoundaryFilter.trace(request))`, then returns no success body (a `Detail`-returning handler may return null only after the writer has written the problem). Declare `throws IOException` for the writer. A present result is unwrapped and only its `Detail` is serialized, never the Optional. Null service/transaction results are INTERNAL/500, never 404; no `NOT_FOUND` addition to `CustomerContextException.Code`, shared writer change, new exception hierarchy or extra source path is authorized. Transaction completion must precede both successful serialization and the empty-result 404; completion failure is sanitized 503 instead.

### 3.2 Literal precedence

Evaluate in this order; tests must assert code/status and absence of downstream reads:

1. Route/method/URI closure: existing customer `NOT_FOUND` / 404.
2. Existing bearer authentication: missing/malformed/invalid session → `UNAUTHENTICATED` / 401; unavailable verifier → existing sanitized 503. No caller inference from headers/body.
3. Missing or malformed trusted account/selected-workspace shape → `SERVICE_UNAVAILABLE` / 503, preserving PA27's fail-closed session-context boundary.
4. Malformed subject UUID alone → `VALIDATION_FAILED` / 422, before service/pool resolution. Query parameters and bodies are ignored, never validation failures or authority sources.
5. Missing/invalid persistence resources, transaction start, `SET LOCAL ROLE` or current-role assertion failure → `SERVICE_UNAVAILABLE` / 503. Never continue unassumed.
6. Missing/inactive account, missing/inactive selected organization, or missing/inactive/nonaccepted membership → `FORBIDDEN` / 403, before subject lookup.
7. Subject absent or outside selected owner scope (including unowned, another account, another organization, or personal-versus-organization mismatch) → `Optional.empty()` after lifecycle and owner lookup, then the same customer `NOT_FOUND` / 404 via the controller's existing writer after transaction completion. Do not query risk, activation, evidence or orders for an inaccessible ID. A null owner Optional or mismatched returned subject ID is an invalid projection (500), not absence.
8. Any OPEN risk on any binding belonging to the accessible subject → existing `FORBIDDEN` / 403. Check before activation/counts. A foreign subject with OPEN risk is still 404; an inactive context with a foreign subject is still 403.
9. Read activation and counts. SQL/connection/transaction/bean failures → sanitized 503, including failures during completion. Invalid/null/inconsistent owner projections → `INTERNAL` / 500; never default an infrastructure failure to zero or INACTIVE.

Do not add a rate-limit implementation merely because 429 appears in the contract. Reuse existing boundary behavior without expanding its policy surface.

### 3.3 Access and projection

Personal access requires `business_subject.owner_account_id = verified accountId`; organization access requires `owner_organization_id = verified selected organizationId`. Organization membership role is read from PostgreSQL and must be one of `ADMINISTRATOR`, `MEMBER`, `EVIDENCE_MANAGER`, `AUDITOR`; all four are equally accepted for this read. No claim row, real-name status, binding status or activation state is an additional eligibility gate.

Risk is binding-scoped in V003. Therefore check for OPEN on **every** binding of the accessible subject, not just an ACTIVE/primary/latest binding. Do not require a binding or risk row to exist; no binding/no risk and CLEARED-only risk mean no OPEN risk. The goal specifies OPEN denial, not a new missing-risk denial rule. No risk detail is returned.

Activation: use the singleton activation map; missing key alone → `INACTIVE`. Present values must be exactly `INACTIVE`, `ACTIVE`, `SUSPENDED`. Null map, null/unknown status, extra subject keys or a mismatched subject projection are internal failures. There is no consent/entitlement/claim read and no fallback to those tables.

Evidence: all `evidence.evidence_record` rows with this subject ID, including PENDING/SOLIDIFIED/QUARANTINED/FAILED. Evidence rows have **no account or organization column**; the selected workspace is enforced by the preceding subject ownership predicate on the same snapshot. Do not invent a capture-JSON owner predicate or count only shareable evidence.

Orders: all `catalog_order.customer_order` rows for the accessible subject **and selected workspace**, without a payment-status filter. Personal: matching `account_id` and `owner_organization_id IS NULL`. Organization: matching `owner_organization_id`, regardless of purchasing account; V004 describes `account_id` as purchasing natural person, not organization ownership. Do not accidentally count only orders purchased by the current organization member. Exclude same-subject rows belonging to a different workspace and all other-subject rows.

Use Java `long`/PostgreSQL bigint for counts (OpenAPI integer has no int32 restriction). Count zero is valid; negative or absent aggregate projections are 500. Wrap the success `CustomerSubjectDetailQueryService.Detail(UUID subjectId, String activationStatus, long evidenceCount, long orderCount)` in `Optional.of(detail)` inside the service; the controller unwraps it and serializes the record directly. Its four record components are the complete JSON response. No generic map, inheritance, optional response properties, owner/role/timestamp fields or content objects.

## 4. Exact call path, SQL, transactions and wiring

### 4.1 New public owner interfaces

All UUID inputs are typed internal server inputs, never deserialized ownership selectors. Non-null subject/account are required; null organization explicitly means personal. Owner implementations validate malformed internal inputs without querying. Owner seam failures are sanitized by the customer service.

| Owner / new package and named interface | Interface and exact methods |
| --- | --- |
| Subject-number: `com.onecall.core.subject_number.customersubjectquery`, `customer-subject-query` | `CustomerSubjectAccessReader`: `Optional<UUID> findOwnedSubject(UUID subjectId, UUID accountId, UUID organizationId)`; `boolean hasOpenRisk(UUID subjectId)` |
| Evidence: `com.onecall.core.evidence.customerquery`, `customer-subject-count-query` | `CustomerSubjectEvidenceCountReader`: `long countForSubject(UUID subjectId)` |
| Catalog/Order: `com.onecall.core.catalog_order.customerquery`, `customer-subject-count-query` | `CustomerSubjectOrderCountReader`: `long countForWorkspace(UUID subjectId, UUID accountId, UUID organizationId)` |

Each package has `@NamedInterface` in `package-info.java`. Do not add DTOs exposing persistence rows, Spring/jOOQ/JDBC types, or customer internal types to these interfaces. `Optional<UUID>` is solely an existence/identity witness; verify its value equals the requested ID before risk/projection. Existing activation interface is unchanged.

### 4.2 Exact SQL responsibility and order

All values use indexed prepared parameters; SQL is fixed except choosing one of two fixed workspace branches. No interpolated UUIDs, `SELECT *`, joins that multiply counts, row locks, advisory locks, offset, sort, pagination or list fetch is needed.

Within one transaction:

1. `SET LOCAL ROLE customer_iam_role`; assert `select current_user` equals that literal. This is session-local transaction state, not a business write. Use the existing PA27 role-failure redaction pattern.
2. Existing lifecycle repository queries, unchanged: `SELECT id,status FROM customer_iam.customer_account WHERE id=? AND status='ACTIVE'`; then for organization, select `o.id,o.status,m.account_id,m.organization_id,m.status,m.role` from organization joined to membership on `m.organization_id=o.id`, with selected `o.id`, `o.status='ACTIVE'`, caller `m.account_id`, `m.status='ACTIVE'`, and four-role `IN` predicate. Personal skips membership query.
3. Subject owner's `JooqCustomerSubjectAccessReader.findOwnedSubject`:
   - Personal: `SELECT s.id FROM subject_number.business_subject s WHERE s.id = ? AND s.owner_account_id = ?` (subject, account).
   - Organization: `SELECT s.id FROM subject_number.business_subject s WHERE s.id = ? AND s.owner_organization_id = ?` (subject, selected organization).
   - Primary key guarantees at most one row. An empty owner result returns `Optional.empty()` from the transaction callback immediately, with no risk/activation/count calls, and becomes 404 only after successful transaction completion. Null Optional or a different returned UUID is INTERNAL/500. No need to select owner values or inspect claims. Single-owner constraint prevents simultaneous personal/org owners.
4. Same reader's `hasOpenRisk`: `SELECT EXISTS (SELECT 1 FROM subject_number.caller_number_binding b JOIN subject_number.subject_risk_state r ON r.caller_number_binding_id = b.id WHERE b.subject_id = ? AND r.state = ?)` (subject, `OPEN`). Select only a boolean; no status/type/source/time/internal risk IDs. Do not filter `b.status`.
5. Existing activation reader: `SELECT subject_id, status FROM activation_consent.service_activation WHERE subject_id IN (?)`, singleton accessible subject. Existing unique subject key supplies single current activation. Validate map then default absent row as specified.
6. Evidence owner's `JooqCustomerSubjectEvidenceCountReader`: `SELECT count(subject_id) AS evidence_count FROM evidence.evidence_record WHERE subject_id = ?`. `subject_id` is NOT NULL; this equals row count and requires only its column SELECT. Do not grant/select `id` merely for COUNT.
7. Order owner's `JooqCustomerSubjectOrderCountReader`:
   - Personal: `SELECT count(subject_id) AS order_count FROM catalog_order.customer_order WHERE subject_id = ? AND account_id = ? AND owner_organization_id IS NULL`.
   - Organization: `SELECT count(subject_id) AS order_count FROM catalog_order.customer_order WHERE subject_id = ? AND owner_organization_id = ?`.
8. Validate nonnegative counts, construct the four-component `Detail`, return `Optional.of(detail)`, complete/close transaction, then unwrap and serialize in the controller. No later database query or media/object provider call.

No ORDER BY is appropriate for PK/EXISTS/aggregate reads; this is explicitly not PA27's binding-list ordering. Existing PK, subject/binding/risk and subject-order indexes support bounded predicates. No schema/index migration is authorized; Evidence's subject-count performance on representative cardinality is an owner-routed measurement, not grounds for guessing a new index here.

### 4.3 Transaction and resource rules

`CustomerSubjectDetailQueryService.getCustomerSubject(TrustedCustomerCaller caller, UUID subjectId)` is the only application operation and returns `Optional<CustomerSubjectDetailQueryService.Detail>`. The transaction callback and `transaction.execute(...)` result use that same Optional type; only a genuine empty ownership lookup yields empty. Reject a null transaction result as INTERNAL/500, without `ofNullable` or any null-to-empty/404 conversion. Constructor arguments, in order: `TransactionTemplate transaction`, `DSLContext context`, `JooqCustomerBusinessNumberAccessRepository repository`, `CustomerSubjectAccessReader subjectReader`, `CustomerActivationStatusReader activationReader`, `CustomerSubjectEvidenceCountReader evidenceReader`, `CustomerSubjectOrderCountReader orderReader`.

Add `customerSubjectDetailTransactionTemplate`: customer manager, `PROPAGATION_REQUIRES_NEW`, `ISOLATION_REPEATABLE_READ`, readOnly=true. All readers use the same transaction-aware `customerQueryDsl` connection; they do not own transaction templates, assume roles, fetch lifecycle, write, or open another datasource. The existing `DataSourceTransactionManager` and `TransactionAwareDataSourceProxy` bind one connection. Suspend/restore ambient transactions. Never mutate the supplied/shared template at request time.

The first snapshot-taking SELECT (including the role assertion) establishes the PostgreSQL repeatable-read snapshot; `SET LOCAL ROLE` itself is not a snapshot guarantee. Lifecycle, subject, risk, activation and both counts then share that snapshot. Concurrent commits cannot create a mixed response. This is snapshot-consistent authorization, not RLS or linearizable revocation: a change committed after the snapshot applies on a subsequent request. Avoid lifecycle/advisory/row locks because they would broaden capabilities and conflict with the strictly read-only goal.

Use the existing cycle-safe SQL/DataAccess/Transaction/Beans exception-cause classification from PA27 without logging causes or request material. Preserve known customer codes; unknown programming/projection failures are 500. Role assumption/assertion failures are always 503. A null transaction result is 500. `SET LOCAL` automatically expires on commit/rollback; transaction manager restores isolation/readOnly/autocommit and ambient resources. Tests must prove both success and exception cleanup on an actual one-connection pool, not just mock `RESET ROLE` calls.

### 4.4 Exact bean changes

All new configurations use `@Configuration(proxyBeanMethods=false)`, `@Profile("web")`; all bean methods are `@Bean @Lazy`, no `@Primary`.

- In `CustomerPersistenceConfiguration`: `customerSubjectDetailTransactionTemplate(@Qualifier("customerQueryTransactionManager") DataSourceTransactionManager)` and `customerSubjectDetailQueryService` with qualifiers, in constructor order, `customerSubjectDetailTransactionTemplate`, `customerQueryDsl`, `customerBusinessNumberAccessRepository`, `customerSubjectAccessReader`, `customerActivationStatusReader`, `customerSubjectEvidenceCountReader`, `customerSubjectOrderCountReader`.
- In existing subject `CustomerBusinessNumberQueryConfiguration`: add `customerSubjectAccessReader(@Qualifier("customerQueryDsl") DSLContext)` → `new JooqCustomerSubjectAccessReader(context)`. Do not make its existing list-reader bean eager or change its interface.
- New evidence `CustomerSubjectEvidenceCountQueryConfiguration`: `customerSubjectEvidenceCountReader(@Qualifier("customerQueryDsl") DSLContext)`.
- New order `CustomerSubjectOrderCountQueryConfiguration`: `customerSubjectOrderCountReader(@Qualifier("customerQueryDsl") DSLContext)`.
- Controller constructor: `@Qualifier("customerSubjectDetailQueryService") ObjectProvider<CustomerSubjectDetailQueryService>`, plus `CustomerProblemWriter`.

The bounded cross-owner qualified-DSL wiring matches PA27. No new pool, credential, environment variable, key provider, Flyway runtime dependency or default-profile DB activation is introduced. Backend/Architecture retains the existing deferred qualified-DSL inversion review; do not solve it as part of PA28.

### 4.5 Module map

Append exactly these three entries to `customer_iam/package-info.java`'s six existing allowed dependencies:

```
subject_number::customer-subject-query
evidence::customer-subject-count-query
catalog_order::customer-subject-count-query
```

Retain all six existing entries including activation and business-number named interfaces. New count interfaces depend only on JDK UUID; no reverse import of `customer_iam.internal`. Evidence's entitlement allowlist, catalog's existing `customer_iam`/`subject_number` allowlist, subject's empty allowlist and activation's existing allowlist remain unchanged. Catalog has no existing implementation edge back to customer; its descriptor permission alone does not introduce a cycle. Preserve this one-way actual graph despite bidirectional allowed declarations: customer → sharing/subject/activation/evidence/catalog, sharing → evidence; do not add catalog → customer imports or narrow its existing declarations. Require real `ApplicationModules.verify()` to remain cycle-free; if a genuine cycle is discovered, stop for owner adjudication rather than widen allowlists or bypass verification. No catch-all module dependency or foreign-internal exposure.

## 5. Proven privilege delta and PA25 integration

### 5.1 Existing grants are insufficient

V014 removed customer table/routine/write privileges. V015's final customer set is **26 = 4 schema USAGE + 22 column SELECT**: IAM/organization lifecycle columns, subject `(id,owner_account_id,owner_organization_id,real_name_status,created_at)`, binding `(id,subject_id,number_masked,number_type,status)`, activation `(subject_id,status)`. The independent PA25 seven-effective-role total is **261 = customer 26 + evidence 16 + other roles 219**.

Ownership/lifecycle/activation queries and the binding side of risk already fit V015. There is no customer SELECT on risk columns, no evidence/catalog schema USAGE, and no evidence/order count columns. Executing as an owner, media role or command role to avoid those missing grants would violate both the customer-role boundary and least privilege. A V016 append-only privilege-only migration is necessary; no earlier file may be changed.

### 5.2 Exact eight tuples

Create `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql`. Tuple format below is `(grantee,kind,object,column,privilege,grantor,is_grantable)`; empty column means schema:

```
customer_iam_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false
customer_iam_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false
customer_iam_role|schema|evidence||USAGE|evidence_owner|false
customer_iam_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false
customer_iam_role|schema|catalog_order||USAGE|catalog_order_owner|false
customer_iam_role|column|catalog_order.customer_order|subject_id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.customer_order|account_id|SELECT|catalog_order_owner|false
customer_iam_role|column|catalog_order.customer_order|owner_organization_id|SELECT|catalog_order_owner|false
```

Arithmetic: **delta 8 = 2 schema + 6 column**; customer **34 = 6 schema + 28 column**; global **269 = 34 + 16 + 219**. Evidence role remains 16, all other roles 219. The extra customer USAGE on evidence does not add evidence-role membership. Retain six logins/seven effective roles/four membership edges, ADMIN FALSE / INHERIT FALSE / SET TRUE. No role creation, membership, ownership, default ACL, table SELECT, sequence/routine execution, GRANT OPTION, schema CREATE, business DDL/DML or privilege revocation is needed. Queries use every new tuple and no omitted sensitive column.

Neither risk ID/type/source/timestamps nor evidence ID/snapshot/capture/media columns nor order ID/payment/snapshot/amount/consent columns are granted. No new claim/consent/trial/entitlement grants. Count-column grants necessarily permit raw reads of the count/predicate columns at the SQL role level: this is not row-level database security. Application ownership predicates and private server-only interfaces remain essential; do not claim aggregate-only SQL privileges or RLS.

### 5.3 V016 reconciliation contract

Use V015's bounded transactional catalog-validation/replay pattern, not a blanket GRANT script. This is migration-time validation, never a per-request registry. Keep its validation target array `['customer_iam_role','evidence_role']`: `old_acls` is V015's exact final 42-tuple set (customer26+evidence16); `final_acls` is that set plus the eight additions, exactly50 (customer34+evidence16). Only customer receives new grants; evidence is a validation/preservation target, not a privilege recipient. Keep all seven effective-role topology checks and preservation of all non-target ACLs. Encode those exact old/final literals independently from Java and PA25 oracles. Use owner-local `SET LOCAL ROLE` when granting the eight missing tuples; restore migration session role after each grant. Retain the migration-only `pg_advisory_xact_lock(725025,1)` and catalog search-path hardening from V015; these are not request-path locks or business writes.

Accept exactly a clean complete V015 pre-state or complete V016 final state, with either no PA25 external identities or the complete correctly marked/provenanced external graph, as in V015. Reject partial tuple additions, missing old grants, widened ACLs, wrong grantor, grant option, PUBLIC/default ACL drift, unexpected dependencies, ownership/role attributes/settings/membership/provenance drift and non-authoritative invokers. Do not repair or silently normalize drift. Preserve OIDs, credentials, comments, memberships, role settings, owners, all non-target grants, routines/tables/columns/constraints/indexes/triggers and business rows. Two-pass pre/post verification and rollback must ensure failed migration adds nothing.

Two V015 algorithm details must change explicitly, not by renaming its prefix:

- Pre/final state selection on pass1 must use a **new** addition as discriminator: absence of explicit customer USAGE on `evidence` selects old42; presence selects final50. V015's `subject_number` USAGE discriminator is already present in both V016 states and is invalid here. The complete bidirectional ACL-set equality check still rejects every partial mixture, whichever discriminator branch is selected. Pass2 always requires final50.
- Extend `expected_history` from exact001–014 to exact001–015, appending the existing V015 script/description/checksum `-1316980027`. Require that exact prefix and history cardinality15 or16. Any row above rank15 must be exactly rank16/version`016`/SQL/success/exact V016 script and description. On first Flyway execution its own history row is not installed yet; direct post-install replay has the16th row. Do not require a circular self-embedded V016 checksum inside V016; independent exact own-checksum validation belongs to PA25 and the integration oracle after the file is finalized. Do not accept extra/failed/baseline/noncanonical history rows.

Keep all thirteen sanitized categories, exactly the V015 categories with prefix V016: `AUTHORITY`, `PROVENANCE`, `HISTORY`, `SETTINGS`, `ATTRIBUTES`, `TOPOLOGY`, `OWNERSHIP`, `ACL`, `DATABASE`, `DEFAULT_ACL`, `DEPENDENCY`, `PRESERVATION`, `RECONCILIATION`, each formatted `V016_<CATEGORY>_REJECTED`. No dynamic object/secret/error text escapes the migration exception. Do not remove an existing safety check merely to reduce the migration text. Transactional direct replay must be stable after V016. Replaying V015 against V016's enlarged grants is not supported and must not be used to make historical tests pass.

### 5.4 PA25 current oracle/history

In `backend/ops/postgresql/pa25-runtime-identities.sql`, append the eight independently literal tuples to its expected ACL set; change its exact total **261 → 269**. Append exact history `016`, script `V016__customer_subject_detail_read_privileges.sql`, description `customer subject detail read privileges`, SQL type, installed rank 16 and the actual final Flyway checksum. Change exact history cardinality **15 → 16**. Keep all existing 001–015 checksums/descriptions/ranks and graph/provenance/permission assertions. Derive the V016 checksum from Flyway's resolved final file during implementation and seal that literal before acceptance; no checksum value exists at plan time and none is invented here. Do not parse the migration to manufacture the independent ACL oracle or accept arbitrary checksums/history lengths.

Current apply/check must reject V015-only history and any partial/widened final ACL without mutation. They must reject missing/duplicate/extra/reordered/non-SQL/failed/tampered V016 and V015 history rows. Maintain the existing six-login success marker exactly. Real credentials/TLS/pg_hba and production provisioning remain out of scope.

## 6. Exact implementation path allowlist

These are future implementation edits, not edits authorized in this plan-fix task. All Java paths below are relative to `backend/src/main/java/com/onecall/core/`; test paths are separately explicit. No unlisted production change is anticipated. A discovered necessary expansion requires Controller adjudication, not silent scope growth.

Exact implementation allowlist: **52 distinct paths = 21 production/configuration + 7 new test/fixture + 20 existing test/fixture + 4 existing docs**. Of these, 21 are new (13 Java + V016 + 7 test/fixture), and 31 existing (4 Java + 3 ops/build/CI + 20 test/fixture + 4 docs). Plan fix 1 added two existing test paths in §6.3 (49→51 total; 17→19 existing test/fixture paths), and corrected the README path without adding a documentation path. Controller-accepted `PA28-S1-IMPL-01` at implementation fix 1 adds only `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java` (51→52 total; 19→20 existing test/fixture paths): change the declared-method expectation in `webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact` from literal 10 to 12, preserving all eight test names and every other assertion. The 64 additions and `44/375 + 19/302 = 63/677`, scan 678 and nineteen includes remain unchanged. This cumulative-plan amendment and the separately authorized S1 implementation report and S1-fix-1 artifact are outside the 52 implementation paths; Control and prior artifacts remain immutable.

### 6.1 Production (13 new Java files; four existing Java files modified)

New customer files:

- `customer_iam/internal/application/CustomerSubjectDetailQueryService.java` — service plus nested four-field `Detail` record.
- `customer_iam/internal/web/CustomerSubjectsController.java` — one GET handler, closure/validation and existing sanitized problem handling.

New subject files:

- `subject_number/customersubjectquery/package-info.java`.
- `subject_number/customersubjectquery/CustomerSubjectAccessReader.java`.
- `subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java`.

New evidence files:

- `evidence/customerquery/package-info.java`.
- `evidence/customerquery/CustomerSubjectEvidenceCountReader.java`.
- `evidence/internal/customerquery/JooqCustomerSubjectEvidenceCountReader.java`.
- `evidence/internal/customerquery/CustomerSubjectEvidenceCountQueryConfiguration.java`.

New order files:

- `catalog_order/customerquery/package-info.java`.
- `catalog_order/customerquery/CustomerSubjectOrderCountReader.java`.
- `catalog_order/internal/customerquery/JooqCustomerSubjectOrderCountReader.java`.
- `catalog_order/internal/customerquery/CustomerSubjectOrderCountQueryConfiguration.java`.

Modify only:

- `customer_iam/package-info.java` — three exact named-interface dependencies.
- `customer_iam/internal/web/CustomerBoundaryFilter.java` — GET admission only.
- `customer_iam/internal/persistence/CustomerPersistenceConfiguration.java` — two lazy qualified beans.
- `subject_number/internal/customerquery/CustomerBusinessNumberQueryConfiguration.java` — one lazy qualified owner-reader bean.

No activation implementation change, new lifecycle repository, customer property class or web configuration change is needed. Customer-internal production file inventory becomes **32 → 34** (two new files).

Other implementation/configuration paths:

- `backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql` (new).
- `backend/ops/postgresql/pa25-runtime-identities.sql`.
- `backend/pom.xml` — two explicit container-smoke includes only, no dependency/plugin/toolchain change.
- `.github/workflows/ci.yml` — exact inventories and bounded runtime smoke probes only.

### 6.2 New tests and fixture

- `backend/src/test/java/com/onecall/archtests/CustomerSubjectsWebTests.java` (12 methods).
- `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectDetailQueryServiceTests.java` (12 methods).
- `backend/src/test/java/com/onecall/archtests/CustomerSubjectDetailConfigurationTests.java` (6 methods).
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java` (4 methods).
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailFixture.java` (helper; no test annotations).
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java` (20 methods).
- `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java` (10 methods).

### 6.3 Existing tests/fixtures requiring bounded adjustment

Paths below are relative to `backend/src/test/java/com/onecall/archtests/`:

- `architecture/ModuleBoundaryTests.java`: exact customer dependency map and both six-entry named-interface assertions become nine, preserving exact assertions, named-interface/negative-fixture verification and all method names/counts. Explicitly update the third production-edge assertion, `discoveredProductionEdgesStayInsideTheConfiguredMap`: its flattened target stream must `containsExactlyInAnyOrder("sharing", "evidence", "subject_number", "activation_consent", "evidence", "catalog_order")`. Keep both `evidence` occurrences: one customer→evidence and one sharing→evidence. The six flattened edges must not be reduced to a distinct five-target set or weakened to `containsAll`; retain the per-module configured-map subset guard and cycle-free `ApplicationModules.verify()`.
- `architecture/SharingQueryBoundaryTests.java`: in `sharingQueryApiAndProjectionAreExact`, change only the customer allowed-dependency expectation from six to the exact nine entries: `containsExactlyInAnyOrder("sharing::customer-query", "sharing::customer-command", "sharing::public-query", "sharing::public-admission", "subject_number::customer-business-number-query", "activation_consent::customer-activation-query", "subject_number::customer-subject-query", "evidence::customer-subject-count-query", "catalog_order::customer-subject-count-query")`. Preserve exact `containsExactlyInAnyOrder`, all other API/projection/negative assertions and all eight existing methods. This is an existing Surefire class, not a new CI addition.
- `architecture/CustomerApiBoundaryTests.java`: customer-internal file count 32→34; preserve every banned import/SQL/profile/response check, no broad exclusions.
- `architecture/CustomerBusinessNumberBoundaryTests.java`: its customer dependency assertion 6→9; retain old seam/response/migration-byte tests; current PA25 strings become `expected_resolved) <> 269` and `history) <> 16`, retaining presence of V015 as a predecessor. Do not change canonical frozen literals.
- `CustomerWebConfigurationTests.java`: extend `customerAndSharingGraphsUseExactQualifiers`' exact declared-method/qualifier/@Lazy map with the two customer bean methods and the exact argument qualifiers in §4.4. Preserve existing six methods and default/no-resource assertions.
- `container/PostgresContainerIT.java`: retain target V013, all 64 methods and historical ACLs/checksums. Update both independent Flyway inventory surfaces explicitly:
  - In `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`, change `info().all()` size 15→16, keep `info().applied()` size 13, pending empty and repeat migrate zero. Add exactly one version `016` entry with the actual V016 filename, state `ABOVE_TARGET` and `isApplied()==false`, alongside unchanged V014/V015 assertions; preserve the exact applied-state `SUCCESS` assertion.
  - In `assertV009LiveFence`, retain `containsExactly` and every existing script in order, appending the actual V016 filename (`V016__customer_subject_detail_read_privileges.sql` per §6.1) after V015 for sixteen `info().all()` scripts. This helper serves `v005ForeignKeyAndDeferredReservationFkBackstops`, `v006ReservationUsageForeignKeyAndLateFkBackstops`, `v007ForeignKeyAndLateOrderConsentFkBackstops` and `v009SharingInventoryAndGuardShapesAreExact`; preserve all four callers. Do not alter the adjacent separate `flyway_schema_history` applied-only list ending at `013`; it remains thirteen entries. No invented checksum is used for either future test instruction; use the actual V016 filename, with actual checksum custody separately governed by §§5/9/10.
- `container/DatabaseRuntimeAclRbacHardeningIT.java`: keep V014-pinned twelve-method oracle. Its two explicit tail upgrades to latest (upgrade/replay methods) now execute two migrations and see 16 applied, then current PA25 apply/check. `assertFrozen()` discovers 16 resources but still hashes its historical prefix; do not change V014 ACL expectations or run current PA25 at V014.
- `container/CustomerBusinessNumberReadPrivilegeIT.java`: preserve all eight names/counts and V014→V015 delta tests; precise history-versus-current handling is specified below.
- `container/ProductionDatabaseIdentityProvisioningIT.java`: current latest chain 16; independent ACL expected/actual sets 269 with literal eight additions; last-history-row probes rank 16/version `016`, noncanonical `16`, wrong rank 17, restoration/deletion filters `('016','16')`, extra-version probes `('016','017')`, duplicate rank 1016. Preserve an explicit V015-row corruption probe as well, within existing methods, and all 20 names/counts.
- `container/CustomerBusinessNumbersFixture.java`: latest startup expectation 15→16; explicit historical target startup still uses the numeric requested target and does not provision automatically. Existing business-number tests keep 18 names/counts and behavior.
- `container/CustomerBusinessNumbersIT.java`: in `postgres16FullChainAndCustomerRuntimeRoleAreUsed`, change the successful latest-chain `flyway_schema_history` count string `"15"`→`"16"` to match the updated fixture; preserve the runtime role checks, all other behavior and all eighteen existing method names. This existing Failsafe class retains its existing CI entry and POM include, with no new test identity.
- `container/CustomerMeFixture.java`, `container/SharingQueryFixture.java`: unpinned fresh/applied migration assertions 15→16 only; no old endpoint policy changes.
- `container/SharingRoleLeastPrivilegeIT.java`, `container/SharingQueryServiceIT.java`, `container/SharingAdmissionRoleLeastPrivilegeIT.java`, `container/PublicShareAdmissionIT.java`, `container/EvidenceMediaReadRoleLeastPrivilegeIT.java`, `container/EvidenceMediaReadIT.java`: latest-chain fresh/applied assertions 15→16; in `SharingRoleLeastPrivilegeIT.v010MigrationIdentityAndPostgres16` retain the index14 V015 predecessor checks and append index15 version `016`/exact V016 script checks. In particular `EvidenceMediaReadIT.start()` has the otherwise easily missed unpinned fresh count. Do not alter an unrelated projection assertion merely because it contains the number 15.

No new tests are added to existing classes, no existing methods renamed/deleted, and no historical security assertion is weakened.

### 6.4 Historical PA27 privilege fixture handling

Do not globally turn its V015 historical assertions into V016 assertions. In `CustomerBusinessNumberReadPrivilegeIT`:

1. Pin `current()` and `upgrade()` to target `015`; upgrade from its `014` fixture remains exactly one migration. Historical total remains 261 and customer 26. The resource-byte loop excludes V016 explicitly and covers only V001–V014; the new PA28 boundary test freezes V001–V015.
2. Keep `v014CustomerCannotReadSubjectBindingOrActivationWith42501`, `v015AddsExactlyFourteenOwnerGrantedTuplesAndNothingElse`, and `v015RejectsPartialUnexpectedGrantorGrantOptionAndAuthorityDriftAtomically` wholly historical. Do not run V016 before their assertions or replays.
3. `customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501`: use the class's existing `frozenGraph()` instead of current `fixture.provision()` after the pinned V015 upgrade; this helper already supplies marked external logins, exact memberships and passwords. Its exhaustive denied-column set must remain the V015 set, including PA28's future columns as denied.
4. `v015ReplayPreservesAclsGraphCredentialsDefinitionsAndBusinessData`: existing `frozenGraph(); upgrade();` suffices; remove current PA25 provision at that historical point, keep business seeding and both V015 direct replays/catalog snapshots before any V016.
5. `v015FreshAndUpgradeHaveExactFifteenHistoryEntriesAndFrozenBytes`: do the fifteen-row upgrade and fresh-chain checks explicitly at target 015 (`fixture.start("015")` for the fresh branch, avoiding auto-provision). Only after those assertions, use a separate unpinned latest Flyway to apply V016 (one migration), validate 16, then current `fixture.provision()`/check. This retains literal V015 history/checksum coverage.
6. `pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle`: retain its historical method name as CI identity, but extend its body: V014 and then clean V015 must both fail the **current** PA25 apply/check without mutation; assert 261 at the V015 checkpoint; explicitly upgrade to V016 and then provision. Existing removal tests for each of the fourteen V015 additions and V015-history corruption remain exercised against the current 269-tuple oracle. Their clean/widened totals become 269/270, not 261/262. New V016-only removal/history cases live in the new privilege class. No current PA25 success claim is made at V015.
7. `allSixLoginBoundariesAndRolePoolRestorationRemainUnchanged`: preserve frozen-graph identity before/after V015 assertion, then explicitly migrate V016/validate16 before current provision/web/PA25 check. Keep the existing marker, success/denial pool assertions and all six login checks.

This avoids copying a stale PA25 script into a second oracle, weakening V015 tests, or invoking old replay against a changed ACL. Shared helpers must distinguish pinned historical Flyway from current-latest Flyway by name, not hidden auto-upgrades.

### 6.5 Documentation paths and decisions

Modify only these four existing documentation files during future implementation:

- `backend/README.md`.
- `docs/14-production-data-model-and-flyway-schema-spec.md`.
- `docs/17-production-security-design-spec.md`.
- `docs/operations/pa25-production-database-identity-provisioning-runbook.md`.

Prepend a short **PA28 Current Runtime Boundary (V016)** with the endpoint's four fields, literal access/risk/count matrix, customer34/global269/delta8, unchanged evidence16/other219 and login graph, exact sixteen-row current PA25 prerequisite, snapshot/no-write limitations and new inventory. Relabel the preceding PA27 boundary **historical (V015; superseded by PA28 V016)** and add a historical-only notice, preserving PA27/PA26 numeric evidence and catalog blocks. `backend/README.md` must not call both V015 and V016 current; the nonexistent root README is not an authorized new file. Runbook commands apply V001→V016 before current apply/check; older commands remain explicitly historical.

No edits to docs12 (architecture model unchanged), docs15 (frozen operation inventory unchanged), OpenAPI, business-model tables, existing Gateflow reports/plans/reviews or Control. The prose/schema mismatch and optional-field deferral are documented here and in the bounded PA28 boundary, not repaired by contract drift. Any later implementation report is separately Controller-authorized; this plan does not authorize writing extra report paths.

## 7. Exact test design and names

All additions use ordinary undecorated `@Test void methodName()` methods, no parameterized/repeated/dynamic/conditional/disabled tests. Use loops inside named methods for enum/role matrices. Capture logs through lifecycle-injected facilities rather than decorating XML method identity with parameter names. The methods below are the literal CI sets, not examples.

### 7.1 `com.onecall.archtests.CustomerSubjectsWebTests` — 12

1. `getUsesExactAuthenticatedSubjectRouteAndFourFieldResponse` — exact operation path, content type/cache/trace/security headers; JSON key-set equality and number types.
2. `unsupportedMethodsExtraSegmentsAndTrailingRoutesRemainNotFound` — POST/PUT/DELETE/HEAD/OPTIONS, wrong prefix, empty segment, `<uuid>/json`, trailing slash, encoded/semicolon cases: 404 without service activation. Dotted values are not in this route-404 set.
3. `missingMalformedDuplicateAndRejectedBearerNeverResolveService` — existing bearer cases, exact 401/no I/O.
4. `unavailableVerifierFailsClosedBeforeSubjectResolution` — sanitized 503, no pool.
5. `trustedSelectedWorkspaceIsTheOnlyAuthority` — no authoritative owner/account/role headers; personal/org sessions passed unchanged.
6. `malformedAbbreviatedAndDottedSubjectValuesReturnValidationFailed` — full canonical hyphenated UUID forms and uppercase accepted; abbreviated, whitespace, invalid and `<uuid>.json` values yield 422 after authentication, without service/pool resolution. Contrast `<uuid>/json` route 404 explicitly.
7. `extraQueryParametersAndGetBodiesAreIgnoredWithoutChangingScope` — arbitrary/duplicate owner/account/workspace/role/cursor query parameters and fixed/chunked JSON/non-JSON bodies leave verified caller, subject ID, outcome and exact four-field projection unchanged; verify the same service invocation as the clean request, no body binder/read or cursor provider. Malformed UUID still yields 422 before service regardless of these ignored inputs; selectors cannot make foreign subjects accessible.
8. `invalidTrustedWorkspacePrecedesPathValidation` — missing/malformed selected workspace 503, not 422.
9. `missingServiceAndPersistenceFailuresUseSanitizedUnavailable` — no exception text/SQL/credentials.
10. `subjectNotFoundForbiddenAndInternalUseExistingProblemEnvelope` — stub `Optional.empty()` and verify the existing `notFound(response, trace)` writer produces uniform 404; present Optional unwraps to four fields only. Null Optional yields INTERNAL/500, never 404; existing forbidden/internal codes/status/messages remain, with no subject-dependent details or enum extension.
11. `responseNeverAddsOptionalClaimOwnershipOrContentFields` — absence rather than null for optional fields and `claimState`; no content/owner/role.
12. `subjectReadDoesNotRequireCursorKeyAndExistingRoutesStayUnchanged` — missing cursor key does not block PA28; `/me`, shares and business-number route behavior unchanged.

### 7.2 `com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryServiceTests` — 12

1. `personalAndOrganizationScopesUseOnlyVerifiedCaller`.
2. `nullCallerWorkspaceAndSubjectFailClosedBeforeQueries`.
3. `roleIsAssumedAndAssertedBeforeLifecycleAndSubjectQueries`.
4. `roleAssumptionOrAssertionFailureIsUnavailableWithoutOwnerReads`.
5. `inactiveContextPrecedesSubjectAndRiskReads`.
6. `missingOwnedSubjectIsEmptyAndInvalidProjectionIsInternal` — empty ownership produces `Optional.empty()` only after role/lifecycle/ownership reads and successful completion, with zero risk/activation/evidence/order calls; null owner Optional or mismatched returned UUID is INTERNAL/500, also without downstream calls. No null-as-404 path.
7. `openRiskPrecedesActivationAndBothCounts`.
8. `missingActivationDefaultsInactiveAndPresentStatesAreExact`.
9. `nullExtraUnknownAndMisboundActivationProjectionsAreInternal`.
10. `zeroAndLongCountsAreExactAndNegativeCountsFailClosed` — include values above Integer.MAX_VALUE without truncation.
11. `sqlTransactionAndBeanFailuresAreRedactedAndUnknownFaultsAreInternal` — SQL causes nested cyclically, completion failures on present and empty paths (503, not 200/404), null transaction result (500, never empty/404), exact exception mapping.
12. `oneReadOnlyRepeatableTransactionContainsOrderedOwnerReadsAndNoWrites` — exact order, `Optional.of(Detail)` success and `Optional.empty()` absence after completion, singleton activation request, no list/cursor/policy/provider, immutable template settings and exactly four `Detail` record components.

### 7.3 `com.onecall.archtests.CustomerSubjectDetailConfigurationTests` — 6

1. `defaultAndNonWebProfilesRemainInert`.
2. `webWiringIsLazyAndSubjectValidationDoesNotCreatePools`.
3. `subjectTemplateHasIndependentReadOnlyRepeatableNewTransactionSettings`.
4. `ownerReadersShareExactlyTheQualifiedCustomerDsl`.
5. `controllerUsesOnlyQualifiedSubjectServiceProviderAndProblemWriter`.
6. `missingResourcesFailClosedWithoutCursorEvidenceMediaOrCommandResources`.

Assert the new beans' actual types, no @Primary, exact profiles/qualifiers; instantiate the real application graph with fixture resources where needed. Do not declare all new configurations correct solely by source-string tests.

### 7.4 `com.onecall.archtests.architecture.CustomerSubjectDetailBoundaryTests` — 4

1. `subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces` — exact interface method signatures, no persistence/internal type leaks, exact module map, actual Modulith verification.
2. `customerCompositionContainsNoForeignSqlOrInternalImports` — SQL belongs only to owners; no foreign writes/locks, no cursor/session supplier, exact GET and four record fields.
3. `v016AddsOnlyTheEightRequiredPrivilegeTuplesAndPa25Uses269AndSixteen` — static privilege-only guard, exact tuple set, no business DDL/DML/role creation or broad grant, current oracle/history requirements.
4. `openApiAndV001ThroughV015RemainFrozenAcrossNewlineRepresentations` — retain PA27 canonical-LF byte-hash policy, add V015 literal, prove LF/CRLF/lone-CR equivalence and content-mutation rejection. Raw local byte preservation is independently checked by the acceptance manifest in §9, not replaced by normalization.

### 7.5 `com.onecall.archtests.container.CustomerSubjectsIT` — 20

Use disposable PostgreSQL 16, real Flyway V001→V016, current PA25 apply/check, actual HTTP boundary, a fixture-only verifier and the qualified real owner readers. Put concurrency latches in a test-only delegated reader/execute listener, not production hooks. Fixture creates five organizations or distinct accounts so all four roles are possible under membership uniqueness; do not repeatedly insert four memberships for the same account/organization pair.

1. `postgres16LatestChainAndCustomerRuntimeRoleAreUsed` — sixteen SQL history rows, expected runtime `session_user` and `current_user`, real HTTP 200.
2. `personalOwnerReadsExactlyFourRequiredFields` — selected personal owner, no activation/claim/trial preconditions.
3. `allFourActiveOrganizationRolesReadOnlySelectedOrganizationSubjects` — each role literal, own organization success, other organization 404.
4. `organizationCountsIncludeOtherPurchasersButNotOtherWorkspaces` — current-member and other-member purchases count, same-subject other-org/personal rows excluded.
5. `missingAndInactiveAccountsAreForbiddenBeforeSubjectLookup`.
6. `missingInactiveAndForeignSelectedOrganizationsAreForbidden`.
7. `missingInactiveAndRemovedMembershipsAreForbiddenForEveryRole`.
8. `missingUnownedForeignAccountAndForeignWorkspaceSubjectsAreUniformNotFound` — real service empty results use the existing controller writer, same stable problem fields/message/status, allowing request-specific trace IDs only; OPEN risk on a foreign subject cannot change 404. Capture SQL/delegated-reader calls: lifecycle and ownership occur, but risk, activation, evidence and order readers are never invoked after empty ownership. Completion failure on this path yields sanitized 503, not 404.
9. `openRiskOnAnyActiveDisabledOrRetiredBindingIsForbidden` — each binding status, all risk types; no activation/count calls after OPEN.
10. `noBindingsNoRiskAndClearedRiskDoNotInventDenial` — accessible empty subject returns 200/zero counts/default INACTIVE.
11. `missingActivationDefaultsInactiveAndEveryStoredStatusIsReturned` — INACTIVE/ACTIVE/SUSPENDED unchanged, no activation insertion.
12. `evidenceCountsEveryStatusForOnlyTheAccessibleSubject` — PENDING/SOLIDIFIED/QUARANTINED/FAILED, other subjects excluded, no snapshot/media reads.
13. `personalOrderCountsAllPaymentStatesOnlyInPersonalWorkspace` — UNPAID/UNKNOWN/PAID/CLOSED; same-subject wrong purchasing account/org rows excluded.
14. `emptyAndMultipleCountsNeverMultiplyAcrossBindingsOrActivation` — several bindings/evidence/orders, independent aggregates; no joined cross product.
15. `lifecycleRiskActivationAndCountsShareOneRepeatableReadSnapshot` — pause after lifecycle/ownership, commit new risk/activation and evidence/order rows on a second admin connection; current response is entirely old snapshot, next request sees OPEN denial; clear risk and prove next response sees new activation/counts. Also exercise a lifecycle change in a separate subcase. Coordinate with latches/timeouts, not sleeps; production readers use one connection.
16. `committedLifecycleAndRiskChangesAreRecheckedOnNextRequest` — disable/re-enable account/org/membership and OPEN/CLEARED risk; no cached authority.
17. `successDenialAndDependencyFailurePerformZeroBusinessWrites` — snapshots of every business table including audit/outbox/idempotency/transitions/claims before/after 200/403/404/422/503; capture runtime SQL and show only transaction/role/read statements. Setup writers are fixture-admin connections outside request traces.
18. `pooledRoleReadOnlyIsolationAutoCommitAndAmbientTransactionsRestore` — one-connection Hikari reuse after 200/403/404, injected SQL/role and completion failures; next borrower sees original login role/autocommit/readOnly/isolation, no transaction/session leak; suspended ambient connection restored.
19. `responsesProblemsAndCapturedLogsNeverDiscloseSecretsOrRowContents` — seeded number ciphertext/HMAC, bearer/password, owner IDs, risk internals, consent/trial/claim/capture/order snapshot/media key sentinels absent from response/logs/exception rendering. Requested `subjectId` is the only allowed subject identity on success.
20. `cursorlessUuidValidationAndIgnoredInputsPreserveExistingCustomerEndpoints` — invalid/abbreviated/dotted UUID path values yield 422 before queries; extra segments/trailing routes/non-GET yield 404. Arbitrary/duplicate selector parameters and fixed/chunked GET bodies are ignored, preserve clean-request personal/org scope and counts, and cannot make a foreign subject accessible; malformed path still wins before service. No cursor HMAC key needed for detail; smoke existing `/me`, shares and business numbers without changing their semantics or credentials.

Counts/status fixtures must satisfy V004/V007/V008 immutable/FK/terminal-state constraints. Build consent/catalog/call/evidence prerequisites as fixture administrator, not with request-role grants. Do not disable triggers/constraints or seed impossible lifecycle/role values to manufacture a matrix.

### 7.6 `com.onecall.archtests.container.CustomerSubjectDetailReadPrivilegeIT` — 10

1. `v015DeniesEveryMissingRiskEvidenceAndOrderCapabilityWith42501` — exact eight-tuple prerequisites, positive existing ownership/activation/lifecycle reads.
2. `v016FreshAndUpgradeHaveExactSixteenHistoryEntriesAndFrozenPredecessors` — fresh V001→V016 and V015→V016, exact script/description/checksum/rank/type/success, raw predecessor bytes, validate and no pending migrations.
3. `v016AddsExactlyEightOwnerIssuedTuplesAndMatchesIndependent269Oracle` — before261/after269/set difference exactly §5.2, customer34/evidence16/other219, no grant options; independent Java literal set.
4. `customerRoleReadsOnlyApprovedColumnsAndDeniesAllOtherCapabilitiesWith42501` — enumerate actual columns/objects: approved old+new columns succeed; every other application column, table-wide SELECT, DML/TRUNCATE/TRIGGER/REFERENCES/row lock, sequences/routines/schema CREATE, grants and owner-role substitution fail. Run write-denial probes in a **read-write** transaction with savepoints so 42501 proves ACL, not merely read-only 25006.
5. `v016ReplayPreservesAclsRolesCredentialsDefinitionsAndBusinessRows` — clean and correctly credentialed external graphs; two direct replays, whole-catalog/business snapshots unchanged, Flyway rerun zero migrations.
6. `v016RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically` — each individual added tuple as a partial state, missing retained tuple, sensitive/table grant, wrong grantor and grant option; check sanitized category and exact rollback, no repair.
7. `v016RejectsAuthorityRoleGraphOwnershipSettingsPublicAndDefaultDriftAtomically` — unauthorized role, LOGIN/SUPERUSER/INHERIT/role settings, membership/provenance/comments/OIDs, owner drift, PUBLIC/default ACL and extension/dependency cases, with unchanged catalog after rejection.
8. `pa25RequiresExactV016HistoryAndIndependent269TupleOracleWithoutRepair` — current apply/check rejects V015-only, every removed one of eight tuples, added sensitive grant and malformed/duplicate/extra history; succeeds only after fixture-admin restoration. Seal real checksum; PA25 not allowed to fix drift.
9. `allSixLoginsAndSevenEffectiveRolesRetainExactMembershipBoundaries` — LC unassumed reads fail, SET LOCAL customer succeeds; other logins cannot assume customer; LC cannot become owner/evidence/command; unchanged four edges/options, all unrelated effective ACLs identical.
10. `realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure` — request-like repeatable/read-only role and pooled restoration using the PA25 customer login, sanitized permission/transaction failure, no credential output. Complements HTTP integration without relying on mocks.

## 8. Source, CI, XML and POM inventory

### 8.1 Measured/attested baseline, not fabricated execution

Local read-only inspection counted method declarations and evaluated the CI inventory-construction statements: every CI class/method set equals current source, **40 Surefire classes / 341 methods + 17 Failsafe classes / 272 methods = 57 classes / 613 methods**. Current CI asserts annotation-line scan **614**, including the one historical `PostgresContainerIT` comment; POM has 17 explicit includes. PA27's merged S1 report (`docs/gateflow/pa27-customer-business-number-read-foundation-s1-implementation.md`, inventory/command tables) and Control attest matching actual XML, zero skips/errors/failures. There is no local `backend/target` XML in this planning environment: no baseline or post-change Maven/container execution is claimed here. The unavailable Git pointer and absent durable XML must not be described as freshly verified execution.

Baseline Failsafe class/method inventory, retained unchanged:

| Class (under `com.onecall.archtests.container`) | Methods |
| --- | ---: |
| PostgresContainerIT | 64 |
| SharingQueryServiceIT | 10 |
| CustomerMeIT | 10 |
| CustomerSharesIT | 8 |
| SharingRoleLeastPrivilegeIT | 12 |
| CustomerShareCommandsIT | 18 |
| PublicSharesIT | 8 |
| PublicShareAdmissionIT | 42 |
| SharingAdmissionRoleLeastPrivilegeIT | 9 |
| EvidenceMediaReadRoleLeastPrivilegeIT | 8 |
| EvidenceMediaReadIT | 8 |
| MediaPlaybackIT | 9 |
| MediaGatewayTransportIT | 8 |
| ProductionDatabaseIdentityProvisioningIT | 20 |
| DatabaseRuntimeAclRbacHardeningIT | 12 |
| CustomerBusinessNumbersIT | 18 |
| CustomerBusinessNumberReadPrivilegeIT | 8 |

The complete exact 613 baseline identities remain those in the merged CI/source sets; do not rebase, collapse or rename those sets to meet a number. Existing latest-chain assertion updates do not change method counts.

### 8.2 Computed post-change requirement

| Runner | Baseline | Additions | Required post-change |
| --- | --- | --- | --- |
| Surefire | 40 classes / 341 methods | 4 classes / (12+12+6+4)=34 methods | **44 / 375** |
| Failsafe | 17 classes / 272 methods | 2 classes / (20+10)=30 methods | **19 / 302** |
| Total | 57 / 613 | 6 / 64 | **63 / 677** |
| Annotation-line scan | 614 | 64 real annotations, no annotation-text comments | **678** |
| Explicit Failsafe POM includes | 17 | 2 | **19** |

These are computed acceptance targets, not executed results. No XML exists yet for these names. Fix 1's two added allowlist paths are existing classes (SharingQueryBoundaryTests: eight methods; CustomerBusinessNumbersIT: eighteen methods), so they add no class, method, annotation or POM include. The clarified new test names in §7 replace candidate names one-for-one within `[12,12,6,4]`/`[20,10]`; CI must use these revised literal names, without aliases or duplicate old names. All 613 existing identities remain unchanged.

CI changes in `.github/workflows/ci.yml`:

1. Keep historical dictionaries, unions, frozen method names and historical totals: 112/178, 128/192, 162/198, 196/204, 231/214, 266/214, 309/214, 309/234, 309/246, and the merged PA27 341/272 checkpoint. Keep historical line-scan values 291/321/361/401/446/481/524/544/556 and PA28-start614.
2. Define `pa28_unit_additions` in §7.1→§7.4 order and `pa28_container_additions` in §7.5→§7.6 order with the exact FQCN/method sets above. Union into the corresponding runner map **after** PA27 union, **before** `source_classes` equality and any source/total scans. Do not infer runner from a filename glob that can omit a class.
3. Assert addition length vectors `[12,12,6,4]`, `[20,10]`; capture `pa28_start_totals == [341,272]` by subtracting PA28 methods. Every older per-class baseline computation must subtract PA28 names as well as its existing later-wave names. Every older historical line-scan method-removal union must also remove PA28 annotations; retain its existing totals. Add `pa28_start_line_scan_total == 614`. Do not merely update final totals while breaking historical scans.
4. Change early Surefire report sum 341→375, current method totals `[375,302]`, total677, line678, class counts44/19, explicit POM includes19 and both `exact_inventory` calls to 375/302. Keep the sole historical comment-hit literal. Keep strict source class equality, duplicate/name checks, no conditional tests and real XML identity/count/skips/failure/error checks.
5. Add POM includes `**/CustomerSubjectsIT.java` and `**/CustomerSubjectDetailReadPrivilegeIT.java` after existing includes; retain the other17 and strict POM=container class equality.
6. Update final inventory print to PA28 and the computed totals, preserving the PA27 historical checkpoint. Existing PA27 vector `[12,8,8,4,18,8]` remains unchanged.
7. Extend the existing opt-in web jar smoke's route probe list minimally: unauthenticated valid subject GET→401, opaque bearer without verifier→503, unsupported subject method→404, no database activation. No deployment, credential or successful-live-subject smoke is added.

## 9. Validation commands and immutable-input custody

Future implementation acceptance requires JDK21/Maven3.9.9 as pinned by the repository, Docker and disposable PostgreSQL16. Do not execute these during this plan task. Run commands from repo root unless shown otherwise; preserve actual commands/exits/logs in separately authorized evidence custody. Do not invent a passing result if tooling/network/Docker is unavailable.

1. Before implementation, record raw `sha256sum api/openapi/onecall-v1.openapi.yaml backend/src/main/resources/db/migration/V0{01,02,03,04,05,06,07,08,09,10,11,12,13,14,15}__*.sql` to Controller-authorized external evidence; repeat and compare after implementation. In this snapshot raw files include CRLF transport bytes; the local OpenAPI SHA256 is `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`, local V015 is `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc`. These are **local raw transport hashes**, not claims about the merged LF Git blobs. PA27's canonical-LF V015 literal, independently computed here, is `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3`. Never rewrite frozen files to reconcile newline representations. The canonical OpenAPI literal remains PA27's `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30`.
2. Focused unit/configuration/architecture: `cd backend && ./mvnw -B -ntp -Dtest=CustomerSubjectsWebTests,CustomerSubjectDetailQueryServiceTests,CustomerSubjectDetailConfigurationTests,CustomerSubjectDetailBoundaryTests,ModuleBoundaryTests,SharingQueryBoundaryTests,CustomerApiBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerWebConfigurationTests test`. Including the existing Sharing boundary class detects the six→nine ripple early without changing any inventory.
3. Focused real database/HTTP: `cd backend && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerSubjectsIT,CustomerSubjectDetailReadPrivilegeIT,CustomerBusinessNumberReadPrivilegeIT,DatabaseRuntimeAclRbacHardeningIT,ProductionDatabaseIdentityProvisioningIT,CustomerBusinessNumbersIT verify`. Also execute new privilege tests before weakening any denied-capability checks; no test-skipping properties.
4. Full clean acceptance: `cd backend && ./mvnw -B -ntp clean -Pcontainer-smoke verify` — require all44/375 Surefire and19/302 Failsafe XML, zero skipped/failures/errors, exact identity equality and no initialization-error placeholders. Then execute the **complete** CI heredoc for `assert container smoke actually ran`, extracted verbatim/dedented from the final workflow in authorized evidence storage with working directory `backend`; do not replace it with a totals-only script or fabricate XML.
5. Run the workflow's existing default/opt-in-web jar smoke and dependency/runtime boundary checks exactly. For contract-regressions, use the actual commands reproduced below, from repository root after the A1 subshell, with Node22.23.4 and the locked dependencies. These do not authorize edits to contract inputs or generated clients.
6. Recheck raw frozen manifest, canonical frozen tests, V016 Flyway validation/checksum/current PA2516-row oracle, final source/CI/XML/POM totals and narrow path diff. In a working Git environment Controller may run normal read-only diff/whitespace checks; this author task neither repairs Git nor mutates it.

```bash
(cd api/openapi && npm ci && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 \
  --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s1 \
  --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s2 \
  --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures --check-doc-links
```

No local build, Docker, PostgreSQL, contract-validator or XML acceptance result is asserted by this plan.

## 10. Risks, open questions, residual owners and limits

No product-policy question blocks this slice after the explicit goal confirmation. Concrete decisions are fixed here: all bindings participate in OPEN risk; absent risk is not OPEN; evidence counts all subject rows; personal orders require account+no organization; organization counts include all purchasers in that organization; all statuses count; four fields only. A reviewer finding that contradicts frozen constraints or the confirmed goal must go to Controller, not be silently settled by broadening the endpoint.

| Risk / residual | Owner and disposition |
| --- | --- |
| Current snapshot is not RLS/linearizable revocation; count-column grants permit raw predicate-column reads in SQL | Security/Database: explicitly accept the existing trusted-application boundary; no RLS redesign in PA28 |
| Large evidence/order cardinality and subject/risk query performance | Backend/Database: measure EXPLAIN on representative disposable fixtures; no guessed index/DDL in V016; escalate only a demonstrated blocker |
| Existing owner configs bind the customer's qualified DSL by bean name | Backend/Architecture: existing deferred inversion review, retained with real wiring tests; no generic factory/framework |
| Optional primary number/trial/consent and prose-only claim state remain unresolved future features | Product/Backend/Security: separate confirmed work unit and, if needed, contract change; no PA28 fields/grants |
| V016 checksum cannot be known before the actual file is final | Database/CI: compute and independently seal actual Flyway checksum, never a placeholder or generated self-oracle at runtime |
| Old migration replay rejects later ACLs; current PA25 rejects old history | Database/Test: keep prefix-pinned tests before explicit latest upgrades as §6.4; do not weaken historical guards |
| Windows Git pointer unavailable, CRLF/LF transport, external XML custody unavailable locally | Controller/CI: restore normal managed environment outside this author's scope; retain raw per-environment preservation and canonical Java policy, distinguish attestation from fresh execution |
| Real credential assignment, TLS/pg_hba, migrations/comment custody, pool drain/session invalidation, Q17/Q23 | Platform/Database/Security/Operations: existing external residuals unchanged; production execution NOT EXECUTED, readiness NOT CLAIMED |

No-goal-drift/no-overdesign: no frontend, list endpoint, content/media access, command workflow, claim/activation/consent/trial write, policy provider, entitlement balance read, primary-number choice, new public error, cursor, key, role, pool, framework, table/index, cache or deployment work. Do not mechanically split this read by owner/file/module. S1 is done only when the literal composed behavior, minimal eight-tuple grant delta, immutable inputs, historical security tests and computed exact inventories are demonstrated together. Gate review/implementation authorization and any later external actions remain Controller responsibilities.
