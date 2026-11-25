# PA42 customer order reads and UNKNOWN visibility — implementation plan

## 1. Authority, goal and delivery boundary

- Task: `pa42-customer-order-read-plan-fix6-gpt-20261125-200000`; date: 2026-11-25. This is a planning artifact, not implementation, approval, readiness marking or entry into another gate.
- Binding authority: the active PA42 goal confirmation and all accepted review/re-review findings in `docs/gateflow/project-execution-control.md`. Evidence includes both original reviews, `docs/reviews/plan-review-20261125-174142.md` and `docs/reviews/plan-review-20261125-174143.md`, both first re-reviews, `docs/reviews/plan-review-20261125-180538.md` and `docs/reviews/plan-review-20261125-180539.md`, both final re-reviews, `docs/reviews/plan-review-20261125-184232.md` and `docs/reviews/plan-review-20261125-184233.md`, round-3 A and B-retry artifacts, `docs/reviews/plan-review-20261125-190309.md` and `docs/reviews/plan-review-20261125-191412.md`, and final A/B artifacts, `docs/reviews/plan-review-20261125-194132.md` and `docs/reviews/plan-review-20261125-194133.md`. The accepted final container-guard correction does not promote either review's non-blocking documentation wording into an edit.
- Deliver exactly `GET /api/v1/customer/orders` (`listCustomerOrders`) and `GET /api/v1/customer/orders/{orderId}` (`getCustomerOrder`). Preserve the existing POST operation and its behavior.
- Trust only the authenticated customer principal and its server-selected PERSONAL/ORGANIZATION workspace. Both reads require the trusted account **and** workspace ownership predicates. List returns frozen summaries; detail returns frozen historical snapshot/current order facts. `UNKNOWN` stays visible; `issuanceStatus` is computed, never written.
- Deliver **one end-to-end implementation slice**: routes, lifecycle/ownership, query port, cursor, validating projection, narrow V020 and complete regression/oracle evidence. Splitting list/detail/ACL layers creates artificial acceptance boundaries; repository evidence supplies no real split.
- Success means exact HTTP projections/errors, stable PostgreSQL keyset order, bounded two-stage data reads in one customer-role snapshot, exact 17-tuple ACL enablement, and zero business/audit/Outbox/idempotency writes on success and failure.

### Non-goals and immutable inputs

No reconciliation, provider/callback/retry, UNKNOWN repair, cancel/refund/subscription/issuance action, create-semantics change, operations endpoint, extra filter, frontend, cache, search or read model. No new credential, property, role, membership, pool, table, constraint, index, function, view, default privilege or DDL. V020 may grant only the schema USAGE and exact column SELECTs in §7.

V001–V019, `api/openapi/onecall-v1.openapi.yaml`, `docs/12-technology-stack-and-system-architecture.md`, `docs/14-production-data-model-and-flyway-schema-spec.md`, `docs/15-production-openapi-surface-spec.md` and `docs/17-production-security-design-spec.md` remain byte-identical. The existing PA25 script, current README/runbook, exact guards and build inventories are **mutable bookkeeping only** for V020 and actual PA42 identities, as enumerated in §9. Their history/equality/provenance assertions cannot be weakened.

## 2. Repository evidence and contract decisions

The implementation must retain these decisions, not rediscover a different contract:

| Evidence | Consequence |
| --- | --- |
| OpenAPI paths around lines 802–920; schemas around 5274–5290 and 5411–5605 | Two GETs; cursor/limit only; exact `OrderSummary`, `Order`, `OrderPage`, snapshot and benefit keys. Both payment enums include UNKNOWN. |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql` | Order row owns account/workspace, immutable snapshot/hash, amount, consent ID and current payment facts. Payment-shape constraints remain enforced. |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | Issuance FK is `order_id`; statuses are NOT NULL and constrained to PENDING/ISSUED/FAILED. No grant/expiry inputs are necessary for this API field. |
| docs14 §4.20/§4.33/§9–11; docs15 order read/sort/projection contract | Historical commerce is never rebuilt from current catalog. API issuanceStatus is the status-set projection; docs14 R-04's grant/expiry join is a different internal projection, not this field. |
| docs17 persisted-fact and cursor rules, especially lines 73/75/120/122 | List corruption → sanitized 500; detail corruption → frozen 404; dependency/role/completion failure → sanitized 503. Lazy keys and fixed error precedence; read-only REQUIRES_NEW/REPEATABLE_READ. Do not import unrelated evidence-risk policy into order reads. |
| `catalog_order.customercatalogquery` / `catalog_order.internal.customercatalogquery` PA40 pattern | Owner module exposes the query API and owns SQL; customer IAM orchestrates lifecycle/HTTP only. |
| `CustomerOrderSemantics.storedSnapshot`, `.sha(.canonical(.parse(...)))`, `.projection`; `JooqCustomerOrderRepository.stored` | Projection alone is not validation. Reuse all three steps through one public validating seam, preserving existing Snapshot/Benefit value types. |
| `JooqCustomerBusinessNumberAccessRepository.requireActiveContext` | Reuse existing ACTIVE account/organization/membership/allowed-role logic, not another copy. |
| V016/V019 and PA25 `$frozen$` | Customer currently has only order subject_id/account_id/owner_organization_id; V020 adds 14 order columns, two issuance columns and one schema USAGE. |

OpenAPI `x-onecall-a0-models` lists are descriptive lineage metadata, not instructions to join every listed table. Read `customer_order.payment_status`, `paid_at`, `closed_at` and `purchase_consent_id` as authoritative row facts. Never read `payment_order`, provider payload or `consent_record` to construct these responses.

## 3. Frozen HTTP behavior and errors

### Routes and response shapes

`CustomerBoundaryFilter` admits only exact GET collection/detail shapes, in addition to its unchanged POST admission. Preserve canonical-path checks, authentication, trusted session/workspace derivation, trace IDs, rate limiting, cache/security headers and existing error envelopes. GETs require neither Content-Type nor Idempotency-Key. Trailing slash, encoded/semicolon aliases, extra segments and unsupported methods remain 404; POST detail remains 404. An admitted one-segment detail with malformed UUID is 422, not an alias route.

`OperationsSubjectsWebTests` imposes a source-order invariant on this filter: `source.indexOf("operationsPath.matches") < source.indexOf("UUID traceId = UUID.randomUUID()")`. Any customer GET admission edits must preserve the existing operations bypass before trace-ID generation and its ordering; do not move either block or refactor it. This is a validation constraint, not additional refactoring authority.

List accepts only optional cursor and integer limit: default 20, inclusive 1–100; reject rather than clamp. No subjectId/paymentStatus/owner/account/organization filter is added. Unrecognized selectors cannot change authority. Return only:

- Page keys: `items,nextCursor,hasMore`; serialize tail/empty `nextCursor` as explicit JSON null.
- Each summary's exact 11 keys: `orderId,orderNo,subjectId,channel,catalogVersionId,catalogOfferId,amountCents,currency,paymentStatus,issuanceStatus,createdAt`.
- Include UNPAID, UNKNOWN, PAID and CLOSED with no payment-state exclusion or side effect.

Detail returns the 11 summary keys plus `snapshotSha256,snapshot,purchaseConsentId,paidAt,closedAt`. The first three additional fields are required; choose deterministic explicit null for absent paidAt/closedAt, as allowed by their frozen nullable optional properties. Do not reuse the create `Order` record as the entire detail DTO: it has no paidAt/closedAt components. Reuse only its appropriate shared Snapshot/Benefit types. Neither response contains account/organization IDs, raw snapshot internals, provider facts or entitlement rows.

Serialize UUIDs as canonical UUID strings and instants as deterministic, namespace-free ISO-8601 UTC text (`DateTimeFormatter.ISO_INSTANT` / equivalent Jackson string output); no numeric epoch, Java type wrapper, XML namespace or server-local timezone. Preserve PostgreSQL timestamp precision. Validate all exposed persisted fields, enum domains, UUIDs, required nullability, lengths, nonnegative amounts and payment/timestamp shape. Monetary and benefit integer values must be safely representable: at most `9007199254740991` (2^53−1), with the existing semantic lower bounds. Summary reads need not load/validate a snapshot they do not expose.

### Error mapping and precedence

Reuse `CustomerContextException` and `CustomerProblemWriter`; do not add NOT_FOUND to its enum. As in `CustomerSubjectsController`, service detail returns `Optional.empty()` for missing/inaccessible/unrepresentable detail and controller calls `problems.notFound(response, trace)` with the existing frozen envelope. Never probe foreign existence separately. Unrepresentable list rows/status sets or malformed projection results throw `CustomerContextException(INTERNAL)` → sanitized 500. SQL, role verification, transaction acquisition/completion or missing service/bean failures map to `SERVICE_UNAVAILABLE` → sanitized 503. Do not turn dependency failures into 404, or corrupt persisted facts into retryable 503. Log only safe invariant category/trace metadata, never stored JSON, owner IDs, keys, MACs, provider payload or SQL details in public errors.

Order of evaluation is route 404 → authentication 401 (or verifier dependency 503) → trusted workspace resolution (infrastructure failure 503) → limit 422 → cursor structure 400 → missing/invalid cursor key 503 → MAC/scope/filter 400 → ACTIVE lifecycle 403 → data reads/projection. For authenticated callers, missing/inconsistent required account/workspace context is FORBIDDEN 403, not an account-only fallback; it must never confer authority. Detail validates its UUID as 422 after authentication/trusted-context handling and never resolves any cursor key.

For list specifically: an invalid limit wins over cursor/key failures; malformed cursor plus missing key is CURSOR_INVALID 400; structurally valid cursor plus missing/invalid key is 503; absent cursor still requires a valid key even on the first/empty page. A valid MAC but wrong filter digest gives CURSOR_FILTER_MISMATCH 400; bad MAC/scope/domain gives CURSOR_INVALID 400. Preserve the existing `OrderCreationException` handler and POST status/idempotency/body precedence unchanged; add a distinct context-exception handler for GETs.

## 4. Single implementation architecture and wiring

### Owner query API

Add `com.onecall.core.catalog_order.customerquery.CustomerOrderReader`. This package already contains CustomerSubjectOrderCountReader and its `customer-subject-count-query` named interface: modify the existing `package-info.java` to `@NamedInterface({"customer-subject-count-query", "customer-order-query"})`, preserving the old name and its caller. Both names describe the same public package; verify exact exported classes (the existing count reader, new order reader and its nested value records), no internals, and unchanged subject-count signatures. No authorization is inferred from named-interface membership. Public order-reader methods are exactly:

1. `List<SummaryRow> fetchOrders(Scope scope, Optional<Anchor> anchor, int fetchLimit)`.
2. `Optional<DetailRow> fetchOrder(Scope scope, UUID orderId)`.
3. `List<IssuanceRow> fetchIssuances(List<UUID> orderIds)`.

Nested immutable value records belong to that public interface: `Scope(accountId,organizationId)` (null organization means PERSONAL), `Anchor(createdAt,orderId)`, raw `SummaryRow` (the ten summary facts excluding computed issuanceStatus), `DetailRow(summary,snapshot,snapshotSha256,purchaseConsentId,paidAt,closedAt)`, and `IssuanceRow(orderId,status)`. Preserve nullable raw persisted status values until explicit validation; constructors must not erase or default them. Use boxed raw numeric fields where a null must be distinguishable. No web/Spring/jOOQ types in this API.

`com.onecall.core.catalog_order.internal.customerquery.JooqCustomerOrderReader` implements the three SQL methods. `CustomerOrderQueryConfiguration` in that same internal package is `@Configuration(proxyBeanMethods=false)`, `@Profile("web")`, with exactly two lazy beans: `customerOrderReader(@Qualifier("customerQueryDsl") DSLContext)` and `customerOrderSnapshotReader()` returning the public seam below. No new datasource, transaction manager or entitlement-module Java dependency. SQL references to issuance do not introduce a Modulith module edge.

Add `catalog_order::customer-order-query` to `customer_iam/package-info.java` and the exact named-interface/allowed-dependency guard sets. Customer IAM allowed dependencies become 14→15; catalog_order allowed dependencies remain six; discovered unique module edges remain fourteen and module count fourteen. Keep all existing edges/named interfaces. `catalog_order/package-info.java` does not require a dependency change: the new SQL uses the existing customer DSL, not foreign Java internals. Its declarations are verified, not widened.

### One validating snapshot seam

Public API: `com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader` with exactly one declared method, `CustomerOrderCreationService.Snapshot read(String storedSnapshot, String snapshotSha256)`. A nested `InvalidSnapshotException` is its explicit data-integrity failure type; no new standalone exception file. It shares the already exported `customer-order-command` named interface because its returned Snapshot/Benefit types already live there; the historical name does not authorize calling create. Do not add a method to `CustomerOrderCreationService`, duplicate its records or expose/import internal semantics in customer IAM.

Implementation: `com.onecall.core.catalog_order.internal.customerorder.DefaultCustomerOrderSnapshotReader`, public for same-module configuration access, implements the seam and performs, in this order:

1. `CustomerOrderSemantics.storedSnapshot(storedSnapshot)` (all existing stored-key/commercial/benefit/canonical validation).
2. Validate hash spelling and compare the supplied row hash with `CustomerOrderSemantics.sha(CustomerOrderSemantics.canonical(CustomerOrderSemantics.parse(storedSnapshot)))`, exactly the stored readback hash semantics.
3. Return `CustomerOrderSemantics.projection(storedSnapshot, snapshotSha256)`, reusing `CustomerOrderCreationService.Snapshot` and `.Benefit`.

Translate the existing semantic/runtime parsing/hash failures at this seam to `InvalidSnapshotException`; do not catch SQL/transaction failures here. `CustomerOrderQueryService` maps that exception to detail `Optional.empty()`/404 and validates row-to-snapshot agreement for exposed catalogVersionId/catalogOfferId/channel/amountCents/currency and nested/top-level hash. This comparison belongs to `CustomerOrderQueryService` because `CustomerOrderSnapshotReader.read(String,String)` has no order-row parameter; the seam remains responsible for stored-snapshot semantic/hash/projection integrity. `CustomerOrderQueryService` validates orderNo against its frozen nonempty length bound; do not invent an additional public numbering contract or grant version_token just to compare a non-exposed column. Keep the PA41 create/readback path unchanged.

Stored snapshot exact 15 keys: `schemaVersion,catalogVersionId,catalogOfferId,channel,versionToken,productCode,versionNo,offerType,audience,priceCents,currency,billingCycle,installments,benefitLines,terms`.

API snapshot exact 14 keys: `schemaVersion,catalogVersionId,catalogOfferId,channel,versionToken,productCode,offerType,audience,amountCents,currency,billingCycle,installments,benefitLines,snapshotSha256`. Map priceCents→amountCents and inject row hash; omit terms/versionNo. Each API benefit has exactly `entitlementType,quantity,validityDays,grantIntervalMonths`; omit lineKey and schedule. Validate extra/missing keys, malformed JSON, terms/benefit bounds and hash mismatch before emission, not just via DTO serialization.

PA41 guards must describe the real shape: CreationService still has exactly one method; SnapshotReader exactly one; PurchaseConsentService still four. The existing PA41 PRODUCTION directory allowlist gains the seam and implementation (18→20), retaining the original eighteen entries and all no-authority checks. Exact directory equality, Snapshot/Benefit key assertions and internal-import prohibitions remain. Read SQL is not added to the PA41 command graph; it gets its own strict read-only guard.

### Customer adapter and configuration

`com.onecall.core.customer_iam.internal.application.CustomerOrderQueryService` is the sole new read orchestrator. It uses trusted caller values, existing lifecycle collaborator, the owner port and validating seam. It owns no order/issuance SQL. Its public operations are `listCustomerOrders(caller,request)` and `getCustomerOrder(caller,orderId)`; request/page/summary/detail application values may be nested records, not extra files. Null/malformed repository outputs are explicit projection failures, not silently empty pages.

`CustomerPersistenceConfiguration` adds exactly three lazy beans, `customerOrderTransactionTemplate`, `customerOrderQueryService` and `customerOrderCursorCodec`, preserving its existing eighteen (18→21):

| Bean | Exact dependencies / behavior |
| --- | --- |
| `customerOrderTransactionTemplate` | `@Qualifier("customerQueryTransactionManager") DataSourceTransactionManager`; readOnly=true, PROPAGATION_REQUIRES_NEW, ISOLATION_REPEATABLE_READ. Separate template, same existing manager/pool; do not mutate another endpoint's template. |
| `customerOrderQueryService` | Qualified `customerOrderTransactionTemplate`, `customerQueryDsl`, `customerBusinessNumberAccessRepository`, `customerOrderReader`, `customerOrderSnapshotReader`, passed to the orchestrator. |
| `customerOrderCursorCodec` | Existing `CustomerCatalogOfferCursorProperties`; no new properties type, secret or environment variable. |

The owner query configuration's snapshot bean is independent of `CustomerOrderConfiguration`'s persistence/operations-enabled command graph. Reads must not activate purchase consent, the LO command pool or create service. Keep `CustomerOrderConfiguration` production wiring unchanged. Modify `CustomerOrderConfigurationTests` to prove separation and select its existing factory by exact method name/signature rather than relying on reflection array order; retain all existing writable READ_COMMITTED command-template assertions.

`CustomerOrdersController` adds lazy qualified ObjectProviders for read service and codec. Structural validation is static/key-free and precedes codec/service resolution. Detail resolves only the read service. DTO additions are nested in `CustomerOrderDtos`; no new top-level DTO file. `CustomerWebConfigurationTests.customerAndSharingGraphsUseExactQualifiers` adds the three exact bean signatures (18→21) and owner-configuration bean assertions. New configuration tests assert all profiles, laziness, qualifiers, actual transaction attributes and missing-dependency sanitization.

Reuse `CustomerOrdersFixture`; add `onecall.customer.catalog-offers.cursor-hmac-key` with the same synthetic canonical 32-byte value used by the catalog-offer fixture. Provide a fixture override to omit/corrupt it for precedence tests; do not add an order-key property or real credential. No application YAML change is required. Reverse the obsolete list GET-404 assertion in CustomerOrdersWebTests while retaining every POST, alias and unsupported-method check.

## 5. Transaction, authorization and bounded data flow

Both operations execute within one read-only `REQUIRES_NEW` / `REPEATABLE_READ` transaction using the existing customer query datasource/manager/DSL on one transaction-aware connection. Set `SET LOCAL ROLE customer_iam_role`, assert `SELECT current_user`, then call existing `JooqCustomerBusinessNumberAccessRepository.requireActiveContext` using the trusted account/organization scope. Account must be ACTIVE; ORGANIZATION additionally requires ACTIVE organization and ACTIVE membership with role ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER or AUDITOR. Missing/inactive context is 403. These reads need no new lifecycle privileges. Keep required context validation even when the order page is empty or the detail is missing.

The two data queries below exclude role/lifecycle verification statements from their count. There is no N+1, COUNT, offset, status aggregate hiding invalid raw values, FOR UPDATE/SHARE lock or write. Bind parameters through jOOQ; no input SQL interpolation or SELECT *.

Ownership is mandatory in query 1 for both endpoints:

```sql
-- PERSONAL
o.account_id = :trustedAccountId AND o.owner_organization_id IS NULL
-- ORGANIZATION
o.account_id = :trustedAccountId AND o.owner_organization_id = :trustedOrganizationId
```

List query 1 selects the ten summary facts from `catalog_order.customer_order` with the appropriate predicate and optional `(o.created_at,o.id) < (:anchorCreatedAt,:anchorOrderId)`, `ORDER BY o.created_at DESC,o.id DESC LIMIT :limitPlusOne`. Preserve the result in SQL order; PostgreSQL UUID ordering is authoritative, and Java must **not** re-sort UUIDs or rows. Compute hasMore from the actual limit+1 result, retain the first limit rows, and anchor nextCursor on the last returned row only when hasMore=true. The sentinel is never projected or included in issuance IDs. Empty page skips query 2 entirely and returns `items:[],nextCursor:null,hasMore:false`.

List query 2 selects only raw `(order_id,status)` from `entitlement_metering.entitlement_issuance WHERE order_id IN (:returnedOrderIds)`, at most 100 bound order IDs. Do not limit/truncate the matching issuance rows: the complete status set is required. Validate each raw row (nonnull order ID belonging to this batch; nonnull status in the allowed domain) **before** collecting distinct statuses. Null repository collections, duplicate order identities, over-limit query1 results or unexpected batch identities are projection failures. Status fan-out is measured, not assumed bounded by 100 rows.

Detail query 1 selects the summary facts plus snapshot/hash/purchase_consent_id/paid_at/closed_at with the same ownership predicate and `o.id=:orderId`. No row gives Optional.empty and skips query2. An accessible row uses query2 with its single order ID and the same raw validation/status projection, then calls the validating snapshot seam inside the transaction. Corrupt detail data gives the same 404 as missing/inaccessible detail. Return immutable results only after successful transaction completion; transaction completion failure is 503.

Status-set-only mapping, independently of payment state:

| Valid raw status set | API issuanceStatus |
| --- | --- |
| Empty | NONE |
| Only PENDING (one or many rows) | PENDING |
| Only ISSUED (one or many rows) | ISSUED |
| Only FAILED (one or many rows) | FAILED |
| Two or three distinct allowed statuses | MIXED |

Never join entitlement_grant, validity, expiry, trial, units or schedules. Raw null/unknown canaries are unit mocks; PostgreSQL tests assert that NOT NULL/CHECK constraints reject them. Never bypass/drop/disable constraints or triggers, even inside a rolled-back test. A valid UNKNOWN order with no issuance rows yields NONE.

This is not RLS, a cross-page snapshot, linearizable revocation or a promise of zero scan cost. A request observes one repeatable snapshot; later committed changes apply to the next request. Capture role/session_user/autocommit/isolation/readOnly/settings after commit, rollback and pool reuse. Test both a real customer login membership switch and the direct-role topology as applicable: same-role SET LOCAL alone is not escalation evidence. Explicitly prove inability to assume owner/service/admin roles.

## 6. Cursor contract and precedence implementation

Reuse `CustomerCatalogOfferCursorProperties` and `onecall.customer.catalog-offers.cursor-hmac-key` / its existing secret binding, canonical padded standard Base64 of exactly 32 bytes. No key default, new key bean type, key ID, TTL, encryption layer or secret is introduced. All replicas use the same supplied value. Shared-key rotation coupling is classified in §12.

`CustomerOrderCursorCodec` uses HMAC-SHA256 with exact UTF-8 domain `onecall:customer-orders:cursor:v1\n`, distinct from catalog offers. Encode 125 bytes: version=1 (1), SHA-256 scope binding (32), SHA-256 filter digest (32), epoch seconds (8), nanoseconds (4), UUID bits (16), MAC (32). The MAC input is domain bytes plus the first 93 payload bytes. Canonical unpadded Base64URL length is exactly 167; public input length remains 1–512 before structural validation.

Fix canonical scope bytes as UTF-8 lines, with a terminal newline: `route=/api/v1/customer/orders`, `sort=createdAt desc,orderId desc`, `accountId=<canonical UUID>`, `workspaceType=PERSONAL|ORGANIZATION`, `organizationId=NONE|<canonical UUID>`. Hash those bytes. Filter digest is SHA-256 of exactly `subjectId=ALL&paymentStatus=ALL` (UTF-8, no newline). These are fixed absent-filter placeholders, not accepted query filters. Limit is not bound; changing limit between pages is allowed.

Static validateStructure checks nonempty length/alphabet, no padding, canonical round-trip encoding, exact decoded length, version, timestamp range/nanoseconds and representability at PostgreSQL timestamp precision. UUID is parsed as all 16 bits, not Java signed ordering. Decode verifies MAC, scope, then filter with constant-time byte comparisons. Tests construct an authenticated wrong-filter payload with a synthetic key to distinguish CURSOR_FILTER_MISMATCH; a bad MAC cannot reveal a filter comparison. Wrong route/sort/account/workspace/domain is CURSOR_INVALID. Catalog-offer cursors cannot replay as order cursors even under the shared secret; order cursors cannot replay at catalog offers.

The encoded envelope is opaque to API callers and domain-bound, not confidential ciphertext. Do not accept naked UUID/timestamp, offset or SQL key as a cursor. Use the established authenticated-envelope precedent; changing confidentiality for the entire cursor family is not PA42 scope. Keep structural parsing key-free, key resolution lazy and after limit validation, and service/lifecycle/data access after cryptographic checks as specified in §3.

## 7. V020: exact ACL migration and replay

Only new migration: `backend/src/main/resources/db/migration/V020__customer_order_read_privileges.sql`. Use the V014–V019 family: one transactional `DO $migration$` block; `pg_advisory_xact_lock(725025, 1)`; exact old/final JSON ACL and history/provenance validation; two-pass preservation/reconciliation; sanitized SQLSTATE P0001 with the current named failure category. No ad hoc production grants.

Use all named categories `V020_AUTHORITY_REJECTED`, `V020_PROVENANCE_REJECTED`, `V020_HISTORY_REJECTED`, `V020_SETTINGS_REJECTED`, `V020_ATTRIBUTES_REJECTED`, `V020_TOPOLOGY_REJECTED`, `V020_OWNERSHIP_REJECTED`, `V020_ACL_REJECTED`, `V020_DATABASE_REJECTED`, `V020_DEFAULT_ACL_REJECTED`, `V020_DEPENDENCY_REJECTED`, `V020_PRESERVATION_REJECTED`, `V020_RECONCILIATION_REJECTED`. Preserve the family's checks on migration authority, database/owners, role attributes/memberships/settings, PUBLIC/default privileges, dependencies, foreign ACLs, routines and structural/data inventories; alter only the exact target ACL exclusions required by this delta.

Pre-state: exact global V019 inventory 432, customer subset 75; customer_order already grants SELECT on subject_id/account_id/owner_organization_id from catalog_order_owner. No new tuple below may already exist in a purported old state. Final state: exact global 449, customer subset 92. The scoped order+issuance+new-schema target has 3→20 tuples (17 order columns, two issuance columns, one schema USAGE). Global non-customer tuple count stays 357. Do not confuse 17 final order columns with 17 new order columns.

Exactly these **14 new order columns**, each SELECT, grantor catalog_order_owner:

`id,order_no,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,created_at,paid_at,closed_at`.

Exactly two issuance column SELECTs, `order_id,status`, and schema USAGE on `entitlement_metering`, all grantor entitlement_metering_owner. No table SELECT/grant option; no order revision/version_token/idempotency_key/request_fingerprint; no other issuance column. Existing three order grants stay unchanged. Authorize under owner roles, restoring the original session role between/after grants:

```sql
SET LOCAL ROLE catalog_order_owner;
GRANT SELECT (id, order_no, channel, catalog_version_id, catalog_offer_id,
    purchase_consent_id, amount_cents, currency, payment_status, snapshot,
    snapshot_sha256, created_at, paid_at, closed_at)
    ON TABLE catalog_order.customer_order TO customer_iam_role;
EXECUTE format('SET LOCAL ROLE %I', session_user);
SET LOCAL ROLE entitlement_metering_owner;
GRANT USAGE ON SCHEMA entitlement_metering TO customer_iam_role;
GRANT SELECT (order_id, status)
    ON TABLE entitlement_metering.entitlement_issuance TO customer_iam_role;
EXECUTE format('SET LOCAL ROLE %I', session_user);
```

These are the grant actions **inside** the guarded DO block, not a standalone migration. In pass1 grant only when expected_acls equals the exact old_acls; exact final state is a no-op. Pass2 must observe final ACLs and invariant equality with only these 17 tuples and their exact dependency records excluded from preservation comparisons. Any partial, extra, missing, wrong-grantor or grant-option ACL rejects atomically, not repair/revoke. Full old state with a completed V020 history row rejects. History accepts exactly the V001–V019 pre-migration prefix, or the exact twenty-row completed replay state, with the family's self-row/provenance handling; no future/duplicate/baseline/failed/renumbered rows. Validate V001–V019 checksums/scripts/ranks unchanged and V020's actual resolved identity on replay. Fresh rebuild and V019 upgrade both end with exactly one V020 history row; raw SQL replay does not itself insert a Flyway history row. A second Flyway migrate executes zero migrations.

Normalize ACL tuples as the existing seven fields `grantee|kind|object|column-or-empty|privilege|grantor|is_grantable`. Compare sets for equality/delta, never only has_*_privilege truth. Also prove no object definition, owner, role, topology, default ACL, membership, credential, pool, business or infrastructure row change.

## 8. Exact implementation paths (not edits authorized in this planning turn)

Paths below are repository-relative; no wildcard path authorization. Nested records/exception types in the specified files avoid unnamed extra paths.

### New product paths — 16

Seven production Java files:

1. `backend/src/main/java/com/onecall/core/catalog_order/customerquery/CustomerOrderReader.java`
2. `backend/src/main/java/com/onecall/core/catalog_order/internal/customerquery/JooqCustomerOrderReader.java`
3. `backend/src/main/java/com/onecall/core/catalog_order/internal/customerquery/CustomerOrderQueryConfiguration.java`
4. `backend/src/main/java/com/onecall/core/catalog_order/customerorder/CustomerOrderSnapshotReader.java`
5. `backend/src/main/java/com/onecall/core/catalog_order/internal/customerorder/DefaultCustomerOrderSnapshotReader.java`
6. `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerOrderQueryService.java`
7. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerOrderCursorCodec.java`

One resource:

8. `backend/src/main/resources/db/migration/V020__customer_order_read_privileges.sql`

Eight test Java files, matching production packages for unit tests:

9. `backend/src/test/java/com/onecall/core/catalog_order/internal/customerquery/JooqCustomerOrderReaderTests.java`
10. `backend/src/test/java/com/onecall/core/catalog_order/internal/customerorder/CustomerOrderSnapshotReaderTests.java`
11. `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerOrderQueryServiceTests.java`
12. `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerOrderCursorCodecTests.java`
13. `backend/src/test/java/com/onecall/archtests/CustomerOrderReadConfigurationTests.java`
14. `backend/src/test/java/com/onecall/archtests/architecture/CustomerOrderReadBoundaryTests.java`
15. `backend/src/test/java/com/onecall/archtests/container/CustomerOrdersReadIT.java`
16. `backend/src/test/java/com/onecall/archtests/container/CustomerOrderReadPrivilegesIT.java`

### Modified implementation paths — 10

1. `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` — named dependency addition only.
2. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` — precise GET admission, POST unchanged.
3. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerOrdersController.java` — GET adapters/providers/error mapping; POST unchanged.
4. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerOrderDtos.java` — exact summary/detail/page serialization records.
5. `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java` — three read beans.
6. `backend/src/test/java/com/onecall/archtests/CustomerOrdersWebTests.java` — admitted reads, exact keys/errors/precedence and retained POST/alias regressions.
7. `backend/src/test/java/com/onecall/archtests/CustomerOrderConfigurationTests.java` — exact command factory lookup and independent read wiring evidence; preserve existing assertions.
8. `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java` — exact bean/qualifier sets.
9. `backend/src/test/java/com/onecall/archtests/container/CustomerOrdersFixture.java` — synthetic reused key, overrides and read test seeds/helpers.
10. `backend/src/main/java/com/onecall/core/catalog_order/customerquery/package-info.java` — retain customer-subject-count-query and add customer-order-query to the exact named-interface array.

Do not modify CreationService/Snapshot/Benefit, CustomerOrderSemantics, create repository/service/configuration, existing cursor properties/codecs, application YAML or IAM lifecycle SQL. The new seam calls existing internals within the owning package. No order/issuance owner SQL is placed in customer IAM.

## 9. Exact bookkeeping paths and invariant maintenance

This list is separate from runtime feature files. Each change is only a current V020/history/ACL/identity/file-count adjustment, with historical assertions retained. Preserve meaningful historical targets and counts: not every literal 19/22/432 is a current total.

### PA25, docs and build — 5 paths

1. `backend/ops/postgresql/pa25-runtime-identities.sql`
2. `backend/README.md`
3. `docs/operations/pa25-production-database-identity-provisioning-runbook.md`
4. `backend/pom.xml`
5. `.github/workflows/ci.yml`

The production runbook path above is the actual repository path (not `pa25-database-identity-provisioning-runbook.md`). In both README/runbook update the current boundary to PA42/V020: history **19→20**, total ACL **432→449**, customer tuples **75→92**, with the breakdown **six schema USAGE + 69 column SELECT → seven schema USAGE + 85 column SELECT**. Replace the current provision-boundary block in place, changing title/date `PA40A Current Provisioning Boundary — 2026-11-23` to `PA42 Current Provisioning Boundary — 2026-11-25`, and identify PA42 as the current numeric boundary; do not retain a second current provisioning block or append another one. Change the current apply boundary from `V019__customer_catalog_offer_read_privileges.sql` to `V020__customer_order_read_privileges.sql` before current PA25 apply/check. Retain the unchanged seven-login/eleven-membership-edge topology and existing NOT EXECUTED/not-RLS/operations_login caveats. Preserve bytes after `<!-- PA36 HISTORICAL BODY -->` and all historical PA25–PA40A claims as history; reconstruct historical V019/V018 provisioning evidence independently rather than retaining obsolete current PA40A literals. Do not introduce additional backticked `backend/src/main/java/*.java` paths in the current README/runbook block: its pre-marker Java-path consumer count must remain exactly `consumers == 4`. Record shared-secret rotation coupling without changing frozen docs17 or adding credentials.

PA25 `$frozen$` preserves **all existing positions 0..431**. Append only V020 at 432..448: indices 432..445 are the fourteen new order column tuples in §7 order, 446 is issuance order_id, 447 issuance status, 448 entitlement_metering schema USAGE. Do not sort/reinsert the old array. Set comparisons may normalize independently. Preserve the V018 prefix 0..409 (410 tuples) and V019 delta 410..431 (22 tuples). Update every current expected_resolved/distinct/actual ACL count 432→449; append actual V020 history metadata and change current history count 19→20. Retain all PA25_*_REJECTED category assertions and fail-closed provisioning behavior.

Keep `CustomerCatalogOfferReadPrivilegesIT.current432Oracle()` as an explicitly historical V019 oracle (410+22=432, customer75/noncustomer357). Add independent `CustomerOrderReadPrivilegesIT.orderReadDeltaOracle()` and `.current449Oracle()` (432+17=449, customer92/noncustomer357). Derive the new delta independently from literal approved columns/grantors, not by trusting the PA25 script or migration under test. Current PA25/DB assertions use 449; V019-only tests still assert 432. Derive historicalV019Pa25Script from the current script by removing exactly the appended 17 tuples and V020 history row and reverting only current count tokens; retain an exact historical V019 hash. Make historicalV018Pa25Script peel the historical V019 view, then its original 22 tuples/history row, preserving its existing CRLF/LF hashes. This preserves positional/delta/provenance proof instead of replacing it with loose size assertions.

### Architecture/bookkeeping guards — 17 paths

1. `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`
2. `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`
3. `backend/src/test/java/com/onecall/archtests/architecture/CustomerOrderCreationBoundaryTests.java`
4. `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java`
5. `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java`
6. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java`
7. `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`
8. `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
9. `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java`
10. `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java`
11. `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java`
12. `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`
13. `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferBoundaryTests.java`
14. `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`
15. `backend/src/test/java/com/onecall/archtests/architecture/OperationsLifecycleBoundaryTests.java`
16. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java`
17. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectClaimBoundaryTests.java`

Update exact package/dependency sets and bean/file inventories in the first four, every customer allowed-dependency guard enumerated below, current migration manifests in their consumers, and global production/test/resource counts in the draft/version guards. Preserve frozen-contract hashes and historical PA25 hashes by feeding the correct historical script view. Current PA25 hash changes legitimately; pin the actual new bytes, while asserting exact reconstruction of old bytes. Frozen V001–V018 manifests must exclude both V019 and V020, not accidentally hash V020 as historical; add an independent unchanged V019 pin and a V001–V019 aggregate in the new read boundary guard. Exact current migration manifest becomes V001–V020, rejecting missing/extra/duplicate names. OperationsActivationReadBoundaryTests' frozen source pins remain intact; only its current manifest view may grow.

The `customer_iam` allowedDependencies change is exactly 14→15 at **eight assertion sites in seven files**. Retain all fourteen existing dependency literals verbatim and append exactly `catalog_order::customer-order-query` (the tuple equivalent is `("catalog_order", "customer-order-query")`); preserve every `containsExactlyInAnyOrder(...)` full set, never replace it with `contains`, `containsAll`, a partial match or a range-only check. The exhaustive occurrence map is:

- `ModuleBoundaryTests.verifiesNoCyclesOrBoundaryViolations`: `allowed.hasSize(14)`→15 and the adjacent exact fourteen-tuple set→fifteen tuples.
- `ModuleBoundaryTests.rejectsInternalPackageAccessFixture`: the independent production `allowed.hasSize(14)`→15 and its adjacent exact fourteen-tuple set→fifteen tuples.
- `CustomerOrderCreationBoundaryTests.newNamedInterfacesAndExactDependencyDeltasAreBounded`: customer `getAllowedDependencies(modules).hasSize(14)`→15; preserve module count14 and catalog-order dependency count6.
- `CustomerBusinessNumberBoundaryTests.customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation`: customer `getAllowedDependencies(modules).hasSize(14)`→15.
- `CustomerEvidenceBoundaryTests.namedOwnerInterfacesAndTenCustomerDependenciesAreExact`: exact string projection `containsExactlyInAnyOrder(...)` fourteen→fifteen entries; the historical method name is not a count oracle.
- `CustomerSubjectDetailBoundaryTests.subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces`: customer `getAllowedDependencies(modules).hasSize(14)`→15.
- `CustomerCatalogOfferBoundaryTests.customerImportsOnlyThePublicCatalogSeamAndNoForeignInternalImplementation`: customer `getAllowedDependencies(modules).hasSize(14)`→15.
- `SharingQueryBoundaryTests.sharingQueryApiAndProjectionAreExact`: customer `getAllowedDependencies(modules)` exact string projection `containsExactlyInAnyOrder(...)` fourteen→fifteen entries.

For every live migration-directory exact-set assertion, append exactly `V020__customer_order_read_privileges.sql` with the existing path prefix where required, retaining full `containsExactlyInAnyOrderElementsOf(...)`/set equality. In `CustomerSubjectActivationBoundaryTests.openApiDataDocsAndAllSeventeenMigrationsRemainFrozen`, the live set grows 19→20 (the seventeen frozen migrations plus V018/V019/V020); the historical `expected.hasSize(20)` remains twenty frozen paths (seventeen migrations plus three contract/docs paths). In `CustomerSubjectClaimBoundaryTests.openApiMigrationsAndProductionProviderAbsenceRemainFrozen`, the live set likewise grows 19→20; preserve both historical `frozen.hasSize(20)`/`paths.hasSize(20)` and the frozen migration subset `hasSize(17)` unchanged. Do not add V020 to either FROZEN hash manifest. `OperationsActivationReadBoundaryTests` and `EvidenceRoleIvrAssociationAclBoundaryTests` live migration `hasSize(19)`→20 accompanies the appended exact name; their historical source/hash/delta sizes remain unchanged. In `OperationsLifecycleBoundaryTests.frozenOpenApiMigrationsAndPolicyBytesAreUnchanged`, change only the current `.sql` migration-file count assertion `isEqualTo(19)`→`isEqualTo(20)` for V020; preserve every historical migration/hash assertion unchanged.

`CustomerCatalogOfferReadAclBoundaryTests.MIGRATIONS` and its authoritative shared `assertCurrentMigrationNames` must represent the exact **current V001–V020 twenty-name set**, checking both collection size20 and distinct-set size20/equality. Its independent historical V019 view removes only V020 and remains exactly nineteen names; its V018 view removes V019 and V020 and remains exactly eighteen names. Preserve separate historical pins and hostile missing/extra/duplicate/same-version-wrong-name rejection; do not leave the shared helper on V019, duplicate it for PA42 or weaken equality. `OperationsSubjectReadBoundaryTests` delegates to that helper and to `OperationsLifecycleBoundaryTests`, so it remains green transitively without a textual edit. Its existing `V020__unexpected.sql` is still a valid hostile near-name/extra-entry counterexample, not a future-version claim: appending it to the new twenty-name current set creates an invalid twenty-one-name collection; the missing V019 and wrong V019 replacement cases still reject. Keep those assertions; a counterexample edit is necessary only if the authoritative shared-helper change actually requires it, not merely because V020 is now current.

README/runbook marker coupling has four exact guard consumers, not permission to relax historical proof:

- `OperationsIvrSessionReadBoundaryTests` and the already-listed `OperationsIvrSessionReadsIT` must change their addendum-search literal from `## PA40A Current Provisioning Boundary` to `## PA42 Current Provisioning Boundary`. Keep the positive-offset/order assertions and the existing removal of only the current provisioning block before historical hashing, along with all post-marker historical bytes/hashes.
- `CustomerCatalogOfferReadAclBoundaryTests.currentDocsPreserveHistoricalBoundaries` must assert the pre-`<!-- PA36 HISTORICAL BODY -->` PA42 title and current literals `**20**`, `**449**`, `**92**`, `seven schema USAGE` and `85 column SELECT` in place of the obsolete PA40A/19/432 current literals. Preserve PA36 historical wording, NOT EXECUTED/not-RLS/operations_login checks and the delegated historical-boundary proof.
- `EvidenceRoleIvrAssociationAclBoundaryTests.currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory` must retain its marker split, all post-marker historical hash checks, frozen docs17 hash and exact `consumers == 4` assertion. Assert PA42's same pre-marker current numeric literals for README/runbook only; do not apply PA42 current-document literals to frozen docs17, repin historical bytes or weaken the Java-path scan.

### PostgreSQL fixture/oracle consumers — 36 paths

1. `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOfferReadPrivilegesIT.java`
2. `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java`
3. `backend/src/test/java/com/onecall/archtests/container/CustomerOrderRoleLeastPrivilegeIT.java`
4. `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOffersFixture.java`
5. `backend/src/test/java/com/onecall/archtests/container/CustomerCatalogOffersIT.java`
6. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java`
7. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java`
8. `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java`
9. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectDetailReadPrivilegeIT.java`
10. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceReadPrivilegeIT.java`
11. `backend/src/test/java/com/onecall/archtests/container/CustomerEvidenceIT.java`
12. `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java`
13. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java`
14. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectClaimsIT.java`
15. `backend/src/test/java/com/onecall/archtests/container/SubjectClaimRoleLeastPrivilegeIT.java`
16. `backend/src/test/java/com/onecall/archtests/container/ActivationCommandRoleLeastPrivilegeIT.java`
17. `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java`
18. `backend/src/test/java/com/onecall/archtests/container/EvidenceRoleIvrAssociationAclIT.java`
19. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java`
20. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java`
21. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java`
22. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
23. `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftRoleLeastPrivilegeIT.java`
24. `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadRoleLeastPrivilegeIT.java`
25. `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java`
26. `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java`
27. `backend/src/test/java/com/onecall/archtests/container/OperationsLifecycleRoleLeastPrivilegeIT.java`
28. `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java`
29. `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadsIT.java`
30. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
31. `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java`
32. `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java`
33. `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java`
34. `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java`
35. `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java`
36. `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftLifecycleIT.java`

Current latest-migration assertions become twenty; targeted historical migration tests stay at their targeted version. Upgrade counts are `20 − applied historical version`, not a blanket replacement of literal nineteen. Keep shape/fingerprint/column counts that coincidentally equal nineteen unchanged.

In `OperationsCatalogDraftLifecycleIT.postgres16RunsExactlyV001ThroughV018` (the exact current method name), change the current successful Flyway `string_agg(version,',' ORDER BY installed_rank)` expectation from `IntStream.rangeClosed(1, 19)` to `IntStream.rangeClosed(1, 20)`, retaining `%03d` formatting, comma joining and full-string `isEqualTo(...)`: the exact string is `001,002,003,004,005,006,007,008,009,010,011,012,013,014,015,016,017,018,019,020`, not a range-only partial check. Preserve the existing `SELECT version()` / `PostgreSQL 16.` version check and `fixture.provisioningOracle()` call, including its `PA25_CHECK_OK logins=7 effective_roles=9 memberships=11` assertion. Do not replace migration-string equality with `contains`, `startsWith` or any partial/range-only assertion. Its shared fixture reaches current migrations through `CustomerBusinessNumbersFixture.start(null)`; do not freeze that path at V019 to evade the current guard.

In the already-listed `CustomerBusinessNumbersFixture.start(String target)`, update the exact `flyway.migrate().migrationsExecuted` assertion from `isEqualTo(target == null ? 19 : Integer.parseInt(target))` to `isEqualTo(target == null ? 20 : Integer.parseInt(target))`. Keep `flyway.validate()`, current-only provisioning and explicit-target behavior unchanged: null applies exactly twenty migrations on the fresh fixture, while explicit historical targets still assert their exact parsed count. Every listed consumer of this shared current fixture must use the twenty-migration current boundary; retain independent targeted historical checks.

In the already-listed `OperationsCatalogVersionRoleLeastPrivilegeIT.noV019IsNeededOrPresent` (the exact current method name), replace the current `names` size19/V020-absence expectation with the full exact V001–V020 twenty-name manifest equality specified above, explicitly expecting `V020__customer_order_read_privileges.sql`. Change the hostile near-name guard from `names).noneMatch(name -> name.startsWith("V020"))` to rejection of `V021`/an equivalent unapproved extra entry; exact equality must still reject missing, duplicate, wrong-name and extra entries, including a second near-name V020. Its historical V018 view removes both exact V019 and V020 entries and remains exactly eighteen; retain independent V019 history proof. A V021-prefix absence assertion alone is not a substitute for full exact manifest equality.

CustomerCatalogOfferReadPrivilegesIT retains target019/V019-only fresh-upgrade/replay/delta tests and their exact 432 result; its PA25-current test first advances to V020 and uses current449Oracle, while historical provisioning uses reconstructed historicalV018/V019 scripts. Its new migration is not folded into catalogDeltaOracle. In customer subject/evidence/business-number privilege tests, distinguish the still-forbidden adjacent order/issuance columns from newly approved columns; require denial of table-level/extra-column reads and exact success only for V020 columns. Current recovered historical sets subtract **both** V019's 22 and V020's 17 tuples where appropriate. Operation-role row/ACL projections unaffected by customer grants stay equal to their old independent sets; whole-database current ACL comparisons become449. EvidenceRoleIvrAssociationAclIT retains target018/target019 comparisons and adds V020 only for the latest/provisioning view.

ProductionDatabaseIdentityProvisioningIT updates exact migrate/history/ACL counts, its current-row tamper rank to20, duplicate/future-row counterexamples and the actual V020 metadata. Keep all rejection categories and seven-login/eleven-edge equality. Preserve PA25/PA26/PA27/PA28/PA40A historical checks rather than relabeling old totals as current. Pure current history consumers in sharing/media/subject/operations fixtures receive count/manifest changes only; no unrelated feature assertions change.

### File/method/resource/CI invariants

Re-derive from the **exact final files after implementation**; these are planned arithmetic, not claims that future tests have run:

- Current production/test Java baseline 325/208. The seven/eight new Java paths in §8 imply candidate 332/216; the existing package-info is modified, not added. Confirm via filesystem enumeration before pinning. Resources 22→23 and source+resource manifest347→355 follow the same exact-file check; recompute content hashes from final bytes.
- CustomerApiBoundaryTests' internal-file baseline56 grows by only QueryService and CursorCodec: candidate58. Owner reader/configuration and snapshot implementation are counted in catalog_order, not customer_iam; derive the actual count and retain exact equality.
- PA41 PRODUCTION18→20, CreationService method count1 unchanged, SnapshotReader method count1 added, public query port method count3, IAM allowedDependencies14→15, unique module edges14 unchanged, persistence beans18→21; exact sets accompany every count.
- POM Failsafe includes44→46, adding only CustomerOrdersReadIT and CustomerOrderReadPrivilegesIT. CI exact class/method maps must include all new test files, additions and renamed existing route/oracle methods, and preserve each historical baseline/delta ledger.
- Derive unit/integration method totals, line-scan totals, class totals and sorted FQCN#method digest from final source; never forecast them as measured facts. Current baseline method totals1127/738 and line scan1866 are historical starting facts, not final targets. Source annotations and method identities are distinct counts; preserve their actual relationship.
- Require exact source=CI=POM=Surefire/Failsafe XML identities, zero skipped/failing/error tests, and exactly-once packaged V020 plus exact V001–V020 JAR inventory. Preserve all historical CI subtraction/delta, checksum, grantor, manifest and hostile-input counterexamples. No deletion of assertions, range-only count checks or exclusion of newly failing tests.

Bookkeeping path count is 5+17+36=58 unique paths. All 58 listed bookkeeping files and all ten modified implementation files exist in the current checkout; the sixteen new paths are planned additions, not existing files. Combined planned scope is 16 new +10 modified implementation +58 bookkeeping =84 unique paths; actual implementation completion must derive its real diff, explain any listed path needing no textual change and stop for adjudication before adding an unlisted path. This plan task itself changes only this document.

Focused transitive validation also includes `backend/src/test/java/com/onecall/archtests/architecture/OperationsSubjectReadBoundaryTests.java` and `backend/src/test/java/com/onecall/archtests/OperationsSubjectsWebTests.java`: both are validation-only, unchanged consumers and contribute no edit paths. The existing hostile counterexamples remain valid under the authoritative shared-helper update, so no additional edit is currently required; if that update demonstrably requires a consumer textual change, stop for adjudication and recount rather than silently expanding the 84-path scope. `OperationsIvrSessionReadsIT` is also a focused transitive validation target but is already counted once among the 36 fixture/oracle consumers and requires the marker update above; focused execution adds no second edit path. After adding `OperationsCatalogDraftLifecycleIT` and specifying its exact current assertion update, the repeated full scan of all 20 current architecture files and 62 current container files found no remaining unlisted deterministic red guard for the accepted V020/PA42 changes: the subject-claim fixture's unlisted Flyway default-current loader needs no textual edit, and historical-only guards stay frozen. Implementation must repeat this scan against final source and reject any missing current manifest/dependency/file-count/marker consumer instead of excluding a failing test.

## 10. Exact tests and acceptance evidence

Unit tests exist only at their mirrored identities within the eight test identities listed in §8; no other read-test class is created. The two integration-test identities are the listed container classes. The unit-phase inventory guard checks exact source/CI/POM identities and the presence/precision of CI XML/JAR equality checks; actual generated XML/JAR comparisons run after verify/package in CI. Unit tests must not require reports or a JAR from a later lifecycle phase or reuse stale artifacts as execution proof.

| Test class | Required exact test method identities |
| --- | --- |
| `com.onecall.core.catalog_order.internal.customerquery.JooqCustomerOrderReaderTests` | `ownershipPredicatesAndBoundParametersAreExact`, `pageUsesLimitPlusOneAndPostgresOrder`, `issuanceQueryReturnsRawStatusesForOnlyRequestedOrders`, `detailUsesTheSameAccountWorkspacePredicate`, `queriesUseOnlyApprovedColumnsAndNoWrites` |
| `com.onecall.core.catalog_order.internal.customerorder.CustomerOrderSnapshotReaderTests` | `validatesStoredSemanticsHashThenProjection`, `storedAndApiKeySetsAreExact`, `benefitProjectionHasExactlyFourKeys`, `missingExtraMalformedAndMismatchedFactsAreRejected`, `safeIntegerBoundariesAreEnforced` |
| `com.onecall.core.customer_iam.internal.application.CustomerOrderQueryServiceTests` | `activeContextIsRequiredBeforeBothReads`, `emptyPageSkipsIssuanceQuery`, `sentinelIsNotProjectedOrBatched`, `issuanceMapsEveryHomogeneousAndMixedSet`, `rawNullAndUnknownStatusesMapList500AndDetail404`, `corruptSnapshotAndRowFactsMapDetail404`, `dependencyRoleAndCompletionFailuresMap503`, `transactionUsesOneRepeatableReadSnapshot` |
| `com.onecall.core.customer_iam.internal.web.CustomerOrderCursorCodecTests` | `canonicalRoundTripAndScopeBindingAreExact`, `malformedAndTamperedCursorIsInvalid`, `filterDigestMismatchIsDistinct`, `timestampUuidAndLengthBoundariesAreExact`, `catalogOfferReplayAndOldKeyAreRejected` |
| `com.onecall.archtests.CustomerOrderReadConfigurationTests` | `readTemplateIsReadOnlyRequiresNewRepeatableRead`, `ownerBeansUseOnlyCustomerDslAndSnapshotSeam`, `keyAndServicesResolveLazily`, `readGraphDoesNotActivateCommandPersistence` |
| `com.onecall.archtests.architecture.CustomerOrderReadBoundaryTests` | `ownerPortNamedInterfaceAndSqlBoundaryAreExact`, `snapshotSeamValidatesWithoutDuplicatingProjection`, `v020HasOnlyApprovedGrantActionsAndFailureCategories`, `frozenContractsAndV001ThroughV019RemainByteIdentical`, `finalSourceCiPomXmlAndJarInventoriesAreExact` |
| Existing `com.onecall.archtests.CustomerOrdersWebTests` | Add `exactGetRoutesAndFrozenJsonShape`, `unknownPaymentStatusIsVisibleInListAndDetail`, `invisibleMissingAndCorruptDetailsHaveIdenticalNotFound`, `limitCursorKeyAndLifecyclePrecedenceIsExact`, `detailNeverResolvesCursorKey`. Replace the obsolete read-404 route method with `ordersRoutesAdmitExactReadsAndRejectAliases`; retain all existing POST assertions and account for its identity replacement in CI. |
| `com.onecall.archtests.container.CustomerOrdersReadIT` | `postgres16FullChainAndCustomerRuntimeRoleAreUsed`, `personalAndOrganizationPagesAreExactlyScoped`, `activeLifecycleIsRecheckedForBothEndpoints`, `crossAccountAndCrossWorkspaceRowsNeverAppear`, `stablePostgresUuidOrderAndPagingBoundariesAreExact`, `cursorKeyAndLifecyclePrecedenceIsExact`, `unknownAndAllPaymentStatesRemainVisible`, `issuanceStatusesCoverNoneHomogeneousAndMixed`, `catalogMutationCannotChangeStoredSnapshotProjection`, `missingAndForeignDetailsAreIndistinguishable`, `postgresConstraintsRejectInvalidIssuanceStatuses`, `readsAndInTransactionFailuresHaveZeroWriteDelta`, `representativePersonalAndOrganizationExplainEvidenceIsCaptured` |
| `com.onecall.archtests.container.CustomerOrderReadPrivilegesIT` | `v020HasOnlyExactSeventeenTupleDelta`, `freshAndV019UpgradeReachTwentyMigrationsAnd449Tuples`, `v020ReplaysOldAndCompleteStatesExactly`, `v020RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically`, `v020RejectsAuthorityHistoryTopologyAndProvenanceDrift`, `customerRoleCanReadOnlyApprovedColumns`, `localRoleIsVerifiedAndRestoredAfterCommitRollbackAndReuse`, `successAndFailurePreserveEveryWriteInventory` |

Required scenarios within those methods:

1. PERSONAL own rows, ORGANIZATION own account+selected-org rows, same-org/different-account exclusion, same-account/different-org exclusion, personal-vs-org isolation. Seed organization rows legally as fixture owner (PA41 create remains personal-only), never by expanding POST product behavior. Detail missing/foreign/workspace-invisible have identical 404 status/code/title/detail shape (trace may differ).
2. ACTIVE/inactive/missing account, organization, membership and every allowed/disallowed role for both operations. Recheck within transaction, including empty/missing results. Lifecycle403 must not be swallowed as404/503. No additional subject risk/owner rule is invented.
3. Empty/exact limit/limit+1, default20, limit1/100, invalid0/101/noninteger/duplicate values, repeated timestamps, PostgreSQL UUID sign-boundary ordering, page-limit changes, tail null cursor, anchor deletion and insertion between requests. Assert no duplicate/skipped members of the fixed fixture traversal; do not promise a cross-request snapshot under concurrent mutations.
4. Cursor structure/MAC/scope/filter/domain/key-rotation cases, including missing key with invalid limit, malformed cursor, structurally valid cursor and absent cursor. Assert no service/transaction access on structural/key errors and no key access on detail. GET doesn't call create/consent/command providers.
5. All payment states and allowed issuance multisets/pairs/triples, empty NONE, duplicate statuses and UNKNOWN+NONE. Unit mocks inject null/unknown status and corrupt raw rows; PostgreSQL16 proves invalid inserts/updates fail NOT NULL/CHECK and roll back. No constraint bypass. Query-capture proves only two approved issuance columns and no grant/expiry reads.
6. Snapshot valid roundtrip, canonical hash comparison, mismatched row/hash/catalog/amount/currency/channel, missing/extra keys, unsafe integers, malformed benefits/terms; compare exact snapshot14 and benefit4 key sets, explicitly rejecting terms/versionNo/lineKey/schedule. Current catalog mutation cannot change historical response. Corrupt snapshot tests live at seam/mock layer when a legal DB row cannot represent the defect.
7. Fresh V001–V020 versus V019→V020, SQL replay at pre/final ACL states, second Flyway migrate no-op, exact owner grantors/delta and unchanged first432 tuple positions. Contamination tests use disposable fixtures/savepoints and restore state, with exact rejection category and no collateral delta. Deny table SELECT, adjacent columns, writes, DDL, sequences, executable routines, grant options, defaults and escalation; all seven login identities and eleven membership edges remain equal.
8. Zero-write inventory is content+count (not counts alone) over all business tables, including order/payment/event/issuance/entitlement/consent, and platform audit/Outbox/idempotency, plus definitions/owners/roles/default ACLs/settings. Capture baselines **after** fixture seeding/provisioning. Exercise successful list/detail, empty/missing/foreign404, lifecycle403, persisted-projection failures via legal seams, and an in-transaction dependency503 injected after role/lifecycle/read entry through a test-only failing reader wrapper using the real transaction/pool. Verify rollback and reuse after that failure. Missing-key503 is separately a pre-DB no-access proof; it does **not** prove transaction-failure zero writes. No runtime fault hook is added.

### Representative PostgreSQL16 EXPLAIN evidence, no index change

Within `representativePersonalAndOrganizationExplainEvidenceIsCaptured`, use a separate legal fixture seed of at least 10,000 orders across at least ten accounts and two organizations, with one dense account/workspace, repeated timestamps, shallow/deep anchors and zero/one/many issuance rows. Run ANALYZE as fixture owner, then execute the actual prepared SQL under customer_iam_role with `EXPLAIN (ANALYZE, BUFFERS)` for PERSONAL/ORGANIZATION list first/deep pages at limit20/100, detail, and the batched issuance probe. Record PG16 version, cardinalities/skew, bound shapes, plan nodes/indexes, rows/loops/rows-removed, timing and buffer hit/read counts in validation output.

Assert the SQL limit and batch-ID cap/no-N+1 properties, not a brittle latency/index-node threshold. Inspect for broad scans and document measured cost honestly. V004 has no account-leading index; LIMIT bounds returned rows, not work scanned. V006's order-leading unique index can support issuance probes but does not justify an unmeasured cost claim. Do not add an index or change planner settings to manufacture evidence; read-cost/index action is a separate owned residual in §12.

## 11. Validation commands and completion discipline

Implementation validation runs from repository `backend/` with **Java21**, checked-in **Maven3.9.9** wrapper, Docker and PostgreSQL16 image availability. First run `java -version` and `./mvnw -version` and record actual versions. Commands below are required implementation steps, not tests claimed to have run during this planning-only task.

```bash
./mvnw -B -ntp -Dtest=JooqCustomerOrderReaderTests,CustomerOrderSnapshotReaderTests,CustomerOrderQueryServiceTests,CustomerOrderCursorCodecTests,CustomerOrderReadConfigurationTests,CustomerOrdersWebTests,CustomerOrderConfigurationTests,CustomerWebConfigurationTests test
./mvnw -B -ntp -Dtest=CustomerOrderReadBoundaryTests,ModuleBoundaryTests,CustomerApiBoundaryTests,CustomerOrderCreationBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests,CustomerCatalogOfferBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsCatalogDraftBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsActivationReadBoundaryTests,SharingQueryBoundaryTests,OperationsLifecycleBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerSubjectClaimBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsSubjectsWebTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerOrdersReadIT,CustomerOrderReadPrivilegesIT verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=ProductionDatabaseIdentityProvisioningIT,DatabaseRuntimeAclRbacHardeningIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,CustomerEvidenceReadPrivilegeIT,EvidenceRoleIvrAssociationAclIT,CustomerCatalogOfferReadPrivilegesIT,CustomerOrderRoleLeastPrivilegeIT,OperationsIvrSessionReadsIT verify
./mvnw -B -ntp -Pcontainer-smoke verify
```

The focused Failsafe run includes real PostgreSQL16 HTTP/role/ACL replay and zero-write evidence. The next command covers PA25 identities, PA26 runtime hardening, PA27 business-number ACL, PA28 subject detail, and PA40A catalog-offer ACL plus PA41 nonregression and transitive `OperationsIvrSessionReadsIT` marker/history validation; unit architecture guards verify module cycles/edges via `ApplicationModules.verify()` and include both added subject guards, the shared-helper consumer and filter source-order validation. Validation-only targets do not increase the §9 edit-path counts. The full `-Pcontainer-smoke verify` run is mandatory and validates every current test, not just PA42, and packaging. `OperationsCatalogDraftLifecycleIT` is already a POM Failsafe include but is not in the focused Failsafe commands above: the full run must execute it, and completion evidence must explicitly report its class/method result, including `postgres16RunsExactlyV001ThroughV018`, from fresh Failsafe XML with zero skips/errors/failures. Focused success is not a substitute; do not exclude this IT or infer execution from a stale report. Do not invoke standalone failsafe:verify to imply tests executed when no matching XML exists.

Run the exact source/CI/POM/XML/JAR identity checker maintained in `.github/workflows/ci.yml` with the final derived maps. Verify report identity sets and zero skips/errors/failures, not only Maven's exit status; verify exactly-once `BOOT-INF/classes/db/migration/V020__customer_order_read_privileges.sql` and no extra migrations. Hash frozen V001–V019/OpenAPI/docs12/14/15/17 against the pre-implementation byte manifest; distinguish raw and LF-normalized historical pins. Validate PA25 current449 and reconstructed historical432/410 scripts, ACL grantors, role/session restoration and all zero-write inventories. Capture EXPLAIN output without adding repository artifacts outside the authorized implementation paths.

At implementation completion report actual changed paths/classified counts, exact migration/ACL evidence, named commands/results/versions and report totals, frozen-byte results, source=CI=POM=XML/JAR equality, query/role/transaction/zero-write/EXPLAIN evidence, and residual destinations. Do not implement, stage, commit, push, PR, merge, approve, mark ready, request reviewers, comment externally or enter another gate as part of this plan task.

## 12. Classified residuals and open questions

| Residual | Classification / owner / destination |
| --- | --- |
| Shared catalog-offers/orders cursor-secret rotation | Accepted bounded operational coupling, not a new credential design. Security/Operations owns consistent replica configuration and coordinated rotation; PA42 completion evidence must state both cursor families invalidate on rotation. Domain/replay tests are in this slice. Future endpoint-independent key policy belongs to the credential/cursor security unit, not a silent docs17 change. |
| Account/workspace-leading index absent; order/issuance fan-out/read cost | Deferred-with-owner: Customer Data/Database owner receives measured PostgreSQL16 plans/skew/cardinality evidence; separate capacity/index-DDL work unit under `docs/18-production-capacity-and-deployment-spec.md` capacity review. No index or universal performance claim here; no PA42 blocker. |
| Unrepresentable historical order/snapshot/status facts | Accepted fail-closed policy: list sanitized500, detail404, dependency503. Customer Orders/Data Integrity owner receives safe invariant telemetry and any future repair/backfill decision in a separate data-integrity unit. This slice validates projections and constraints; it neither repairs nor exposes corrupt facts. |
| Organization-order product semantics | Accepted current account+workspace visibility, including same-org/different-account exclusion. Product/Design owner must revisit organization-wide sharing/enterprise order creation semantics before that separate product unit; no organization-wide access or new create feature in PA42. |
| Pool/session cleanup | Assigned to this slice, Implementation/Database Operations owner; commit/rollback/reuse and no-escalation tests close it before implementation completion. Not an untested deferred security assumption. |
| Cursor anchor confidentiality | Accepted existing opaque authenticated-envelope convention; Design/Contract/Security owner owns any future family-wide confidentiality migration. No naked anchor acceptance or cross-domain replay is allowed here. |

Open questions: **none blocking**. All accepted findings have concrete implementation/validation destinations. The artifact defines one read-only slice and no unclassified residual; completion of planning is not approval of implementation or permission to advance a gate.
