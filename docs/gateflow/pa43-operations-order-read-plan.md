# PA43 operations order read foundation — implementation plan

## 1. Authority, goal, motivation and success

- Original task: `pa43-operations-order-read-plan-gpt-20261126-170000`; plan-fix task: `pa43-plan-fix1-gpt-20261126-172612`; date: 2026-11-26. This document is a planning artifact, not implementation, approval, readiness or permission to advance gates.
- Worktree: `C:\Users\Administrator\.codex\worktrees\pa43-next-work-unit\codex-call`. Use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call2` and `GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa43-next-work-unit/codex-call`.
- Plan-authoring verified HEAD: `cfaf07ba688ac556adfe8c2cc8200babdaa32cf8`. Current dispatch Control SHA256, independently re-hashed for this fixed plan: `1fa06277e12918b37636a22d0ffe5ce6870e7271ccd3f522a44711569a781f2b`.
- Provenance history, not a defect: `cd1987a52697e491a40326a2d53acf769af06b4df133e05a33305029419fc342` was Control at original plan-authoring time. `edd1011b848f341ee41bd0876c33a9a17925ca92f5e7514980a39eb4c9cc80b3` was subsequent plan-review-entry bookkeeping, not an implementation input. Controller rejects the reviewers' Control-hash defect interpretation; this fix preserves that history rather than retroactively changing it. Later Controller bookkeeping may advance Control again without changing the binding PA43 goal.
- Fix inputs independently re-hashed: original plan 317 lines / 50,490 bytes / SHA256 `f186bbe0ad62161891bd6ea723e6083bcc1f70aa08794e6f1db73e510bf47734`; review A `docs/reviews/plan-review-20261126-170211.md` SHA256 `37c0f50288833682bc16a7c56d8192845fcd78ac205fa8390bd5220298904dbc`; review B `docs/reviews/plan-review-20261126-172009.md` SHA256 `ff92a51fce85aa6c0d093b928dbf69322ddcaf65013dca11f6bcc6db74639bde`.
- Baseline provenance discrepancy, not silently repaired: local `origin/main` resolves to `29e715a3dccd8bc3fbedb4a094555294b7482a67`, not HEAD. Control's original merge-intake paragraph also names that merge. The supplied checkpoint HEAD and current PA43 goal govern this plan; do not fetch/reset/rebase or claim the two refs are equal. Controller owns subsequent branch provenance bookkeeping.
- Initial status has the pre-existing untracked `.pa43-plan-prompt.md`. Do not edit, delete, stage or include it in delivery. The ancestor `/mnt/c/Users/Administrator/.codex/AGENTS.md` is empty; no repository/nested AGENTS.md was found.

Implement exactly:

1. `GET /api/v1/operations/orders` / `listOperationsOrders`.
2. `GET /api/v1/operations/orders/{orderId}` / `getOperationsOrder`.

Motivation: employees need trustworthy historical commerce visibility without turning order inspection into payment repair or entitlement issuance. Only trusted employee sessions and current fixed `ADMIN`/`OPS` authority may read. List returns `OrderPage`; detail returns frozen `Order`, including immutable snapshot, current order payment status and computed issuance status.

Success: both operations work end-to-end through owner ports on PostgreSQL 16, satisfy exact frozen shapes/errors/filter binding/sort, preserve customer behavior and immutable migrations/contracts, restore transaction/pool identity after every outcome, and generate zero business, payment, entitlement, audit, Outbox or idempotency writes. Exactly **one end-to-end slice** is authorized by this plan, subject to Controller intake; no artificial list/detail/layer slices.

## 2. Scope, non-goals and alignment

Binding inputs remain byte-identical: all V001–V020 migrations, all `api/openapi/` files, docs12/14/15/17, PA25 provisioning SQL, and existing customer source. No migration, new grant, role, membership, credential, pool, schema, index, constraint, function, view or default privilege is needed or permitted.

No payment mutation/manual marking, checkout, provider call/payload, callback, reconciliation, retry/repair, refund, invoice, entitlement mutation, customer endpoint change, operations audit/trial-policy endpoint, frontend, cache, search/read model, extra filter/field, dependency upgrade or unrelated refactoring. Read-only HTTP does not produce an audit event. No idempotency requirement, lookup or write.

| Binding goal | Design and acceptance evidence |
| --- | --- |
| Employee-only ADMIN/OPS | Existing employee bearer boundary plus current employee row under operations role; negative audience/session/role/status tests, never customer IAM reuse (§5). |
| Structured filtered list | Four permitted query keys, canonical filter digest, authenticated opaque cursor, PostgreSQL keyset order and limit+1 (§6–7). |
| Exact historical detail | Owner-local validating snapshot seam, row agreement, current payment facts and batched issuance status; corruption/missing detail uniformly 404 (§4, §7–8). |
| Read-only and bounded ownership | Existing LO connection, same repeatable snapshot, ORDER/TRIAL role scopes, owner SQL only; SQL and database no-write inventories (§4, §9). |
| Preserve frozen/unrelated behavior | No migration/contracts/customer source edits; byte manifests, regressions, historical inventory reconstruction and package/dependency firewall (§10–12). |

Design alignment: docs12's modular monolith, jOOQ/Flyway and separate employee identity; docs14's immutable order/current payment facts and owner ACLs; docs15's O15/O16 public contract; docs17's trusted audience, purpose separation, fail-closed projection and pooled-state discipline. Do not confuse an internal grant/expiry entitlement projection with this API's status-set field.

## 3. Direct repository evidence

Paths below are relative to the repository; Java paths abbreviated in later sections are rooted at `backend/src/main/java/com/onecall/core/`.

| Direct evidence | Consequence |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml:2135` and `:2210` | O15/O16 are employeeSession reads; required roles ADMIN/OPS; list digest fields subjectId/paymentStatus; no Idempotency-Key. |
| Same file `:4022`, `:5411`, `:5462`, `:5539`, `:5589` | Cursor/limit and exact snapshot/order/summary/page shapes; default20, range1–100; payment is a status field, not a nested payment object. |
| `docs/12-technology-stack-and-system-architecture.md:228`, `:232`, `:274`; `docs/15-production-openapi-surface-spec.md:66`, `:128`, `:264`, `:271` | Employee API/owner separation; fixed order and normalized filters; payment and issuance are independent. |
| `docs/14-production-data-model-and-flyway-schema-spec.md:1207`, `:1265`, `:1267`, `:1271` | Immutable historical snapshot; current customer_order.payment_status; payment status never proves issuance. Internal R-04's grant/expiry join is not required for the frozen API enum. |
| `docs/17-production-security-design-spec.md:13`, `:37`, `:73`, `:75` | Existing shared LO pool and exact SET edges; role-wide amplification is an acknowledged boundary; unrepresentable detail/list facts fail closed, dependency failures sanitize. Historical sections are not current provisioning authority. |
| `catalog_order/internal/customerquery/JooqCustomerOrderReader.java:1`, `customer_iam/internal/application/CustomerOrderQueryService.java:1` | PA42 root query plus raw issuance batch, empty-page short circuit, no payment_order join; snapshot/row agreement and list500/detail404 precedent. Customer ownership/service/ports must not be reused to fabricate employee authority. |
| `catalog_order/internal/customerorder/DefaultCustomerOrderSnapshotReader.java:1`, `catalog_order/customerorder/CustomerOrderSnapshotReader.java:1` | Full stored snapshot validation, canonical hash recomputation and projection exist; mere JSON extraction is insufficient. Reuse inside catalog owner only. |
| `operations_iam/internal/web/OperationsBoundaryFilter.java:1`, `OperationsCatalogProductReadController.java:1`, `OperationsProblemWriter.java:1` in the same directory | Exact GET admission, trusted request Principal, manual safe JSON serialization, lazy graph and existing read-code problem mapping. New order routes are not admitted today. |
| `operations_iam/internal/security/EmployeeBearerAuthenticator.java:1`, `VerifiedEmployeeSession.java:1` | Verifier supplies employee UUID/audience only; no caller-supplied role or customer account/workspace selector. Missing/ambiguous verifier fails closed. |
| `operations_iam/internal/catalogread/DefaultOperationsCatalogProductReadService.java:1`, `OperationsCatalogProductReadRequest.java:1`, `OperationsCatalogProductCursorCodec.java:1`, `OperationsCatalogProductReadConfiguration.java:1` | Current authority before parsing, existing AES-GCM key ring, deadline/provider handling and reuse of subject-read resources. Copy the pattern, not catalog-specific limit-binding/projection-failure choices. |
| `operations_iam/subjectread/OperationsReadContext.java:1`, `operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java:1` | Same connection/PID witnesses; read-only REQUIRES_NEW/REPEATABLE_READ template; ORDER/TRIAL role scopes; final role NONE; guarded connection cleanup. The separate writable catalog-sale template is not a read resource. |
| `catalog_order/internal/operationsread/JooqOperationsOrderCounter.java:1`, `entitlement_metering/internal/operationsread/JooqOperationsTrialReader.java:1` | Owner adapters implement operations-owned named-interface ports and scope role switches; a subject order count is not a list/detail implementation. |
| `operations_iam/subjectread/package-info.java:1` and module package-info files | Existing `operations-subject-read` named interface is allowed by both owners. operations_iam permits only operations_audit; importing catalog owner internals or public customer types into it would break ownership/dependency boundaries. |
| `docs/gateflow/pa40b-customer-catalog-offer-read-plan.md`, its fix-1/fix-2 plans, PA41 creation plan, PA42 read plan and implementation | Preserve their validating seams, historical guards and exact inventories. PA40B/PA42's LC grant prerequisite is not an LO grant prerequisite; PA41's writable command graph is not reused for this read. |
| `backend/pom.xml:182`, `.github/workflows/ci.yml:2558`, `:2694`, `:2934` | Explicit Failsafe identities and method-level historical reconstruction; current baseline 122/1164 unit + 46/764 container, annotation scan1929. Source names and XML identities, not aggregate totals alone, are authoritative. |
| `backend/src/test/java/com/onecall/archtests/architecture/CustomerOrderReadBoundaryTests.java`, `CustomerOrderCreationBoundaryTests.java` | Exact file counts currently332 production/216 test Java and current CI/POM totals require narrow bookkeeping changes. Historical behavior and frozen migration checks remain intact. |

Inspection also covers V014–V020 target/grant blocks, operations subject/catalog configuration/web/ACL tests, PA42 HTTP/query/snapshot/privilege tests, `ModuleBoundaryTests`, CI package/firewall steps and `backend/ops/postgresql/pa25-runtime-identities.sql`.

## 4. Ownership, authorization resources and no-migration proof

Use dependency inversion already established by operations reads:

- `operations_iam.subjectread.OperationsOrderReadService` exposes raw requests and exact response records, with no catalog/customer type imports.
- `operations_iam.subjectread.OperationsOrderReadPorts` exposes nested `OrderReader`, `IssuanceReader`, anchors, filters and typed facts. These are stable contracts in existing named interface `operations-subject-read`, not a new module dependency.
- Catalog owner implements OrderReader in `catalog_order.internal.operationsread.JooqOperationsOrderReader`. It alone reads order SQL and directly instantiates the stateless, no-argument `catalog_order.internal.customerorder.DefaultCustomerOrderSnapshotReader` inside the catalog owner package boundary. Do not inject the `CustomerOrderSnapshotReader` bean from `CustomerOrderQueryConfiguration` (`@Profile("web")`), add a snapshot bean/configuration, or make persistence-profile detail depend on web. It maps validated snapshot values into the operations interface's exact snapshot/benefit records; do not expose raw snapshot JSON or import owner internals into operations_iam.
- Entitlement owner implements IssuanceReader in `entitlement_metering.internal.operationsread.JooqOperationsOrderIssuanceReader`, reading only raw order_id/status under TRIAL scope.
- Employee service orchestrates, validates facts and projects results. Employee SQL is limited to its own employee_account authority row. No SQL over catalog_order/entitlement_metering in employee code.

Exact privilege proof:

| Required action | Existing authority |
| --- | --- |
| Read id/status/role/revision of employee_account | Existing operations_iam_role owner-module access used by current read services. |
| SELECT explicit order summary/detail columns and filter by subject_id/payment_status/id/created_at | V004 lines421–435 grants catalog schema USAGE and customer_order table SELECT to catalog_order_role. No new cross-module columns are required. |
| SELECT entitlement_issuance(order_id,status) | V004 lines492–493 grants entitlement_metering schema USAGE to entitlement_metering_role; V006 line649 grants SELECT, INSERT on entitlement_issuance to that role. This read uses only SELECT; Role.TRIAL already selects this role. No V021 or privilege widening is permitted. |
| Switch LO into these roles | docs17 §Existing shared Operations reader identity and PA25 existing eleven edges include LO→operations_iam_role, catalog_order_role, entitlement_metering_role, all ADMIN FALSE/INHERIT FALSE/SET TRUE. LO itself has no object rights before SET. |
| Preserve these grants through latest chain | V014–V017 target customer/evidence; V018 targets evidence; V019/V020 target customer. Their context-role preservation does not revoke the catalog/entitlement owner grants. V020's customer-only17-tuple delta is already applied, not extended. |

V006 also gives catalog_order_role narrow issuance SELECT, so a cross-schema read is technically possible. Deliberately use the entitlement-owner port instead: it retains first-principles data ownership and existing TRIAL scope without any grant or topology change.

Conclusion: **V021 is prohibited**. No missing privilege has been found. If PostgreSQL acceptance disproves any required grant/SET edge, stop with the exact failing role/table/column/SQLSTATE and existing grant evidence. Do not work around it with admin connections, broad grants or a speculative V021. A new plan decision would be required before any minimum owner-issued ACL delta.

Reuse `operationsSubjectReadDataSource`, `operationsSubjectReadTransactionTemplate`, `operationsSubjectReadDsl`, `operationsSubjectReadContext`, and `OperationsSubjectReadProperties`. Configuration is lazy, profile `persistence`, conditional on existing `onecall.operations-subject-read.enabled=true`; controller retains web/servlet conditions. No new property/environment/key/pool family. Detail must work with missing/invalid cursor configuration.

## 5. HTTP contracts and deterministic error precedence

OpenAPI fixes public statuses/codes, not every simultaneous-failure ordering; the following ordering deliberately follows current operations authority-first patterns, with PA42's order-corruption distinction.

1. Exact canonical route/method gate first: only GET list and one literal detail segment. All other methods including HEAD/OPTIONS/TRACE, trailing slash, semicolon/matrix parameter, encoded alias, double separator, extra segment and raw URI/servlet-path disagreement return generic404 without session or business-port resolution. Extend the existing narrow ERROR-dispatch TRACE handling for these order paths if needed to prevent container-native405/HTML; preserve other routes.
2. Existing EmployeeBearerAuthenticator: absent/malformed/duplicate Authorization, rejected token or wrong audience →401 UNAUTHENTICATED; missing/ambiguous/broken verifier or malformed verified-session result →503 SERVICE_UNAVAILABLE. A customer token never works. A valid employee principal is required even in controller-unit entry tests.
3. Resolve the lazy read service/resources and enter the guarded transaction. Connection/disabled graph/missing or multiple required service beans/role witness failure →503. Query current employee by trusted UUID under operations_iam_role: missing row→401. Validate authority facts first: null/unknown status, role null/outside the employee CHECK domain ADMIN|OPS, invalid identity/revision or any malformed row→503 SERVICE_UNAVAILABLE, including mock rows representing that corruption. Only well-formed facts proceed: DISABLED→403 FORBIDDEN; ACTIVE ADMIN or OPS→accepted. There is no supported third employee role in the current schema, so no unsupported-role mock maps403; do not disable the CHECK or invent a representable role. This deterministic mapping matches sibling operations authority validation and O15/O16's ADMIN|OPS authorization contract; 403 covers valid disabled authority, not database corruption. Role is never taken from session claims, headers or query. Keep the existing inline employee SQL pattern, with the single ADMIN|OPS domain constant in the new service checked by its boundary test; no authority-port refactor or extra path.
4. Parse request shape and noncursor values after authority: invalid query key/cardinality, blank/invalid UUID/status/limit, invalid/unacceptable Accept→422 VALIDATION_FAILED. Unknown keys are not additional filters. Detail accepts no query parameters. Limit defaults20, valid decimal integer1–100; use current bounded decimal parser (including numerically valid leading zeros), reject signs/fraction/exponent/whitespace/overflow. UUID input must have full 8-4-4-4-12 syntax; accept ASCII hex case and normalize to UUID before digest/SQL. paymentStatus accepts exactly UNPAID/UNKNOWN/PAID/CLOSED; no trim/casefold/comma lists. Absent differs from blank.
5. List cursor structural validation, key-free: empty/blank, >512, noncanonical base64url, padding, offset/plain number, wrong envelope version/length/key-ID syntax→400 CURSOR_INVALID. Validate limit/filters first, so malformed filters beat malformed cursor.
6. Resolve/validate existing AES-GCM key ring even for first or empty list page→503 on invalid/missing config. Authenticate/decode supplied cursor: unknown key, wrong-purpose/endpoint/sort ciphertext, altered bytes/tag or malformed authenticated anchor→400 CURSOR_INVALID. Only an authenticated structurally valid cursor can yield400 CURSOR_FILTER_MISMATCH for a different normalized filter digest. No detail key resolution.
7. Owner reads/projection: empty list→200 empty page; absent/unavailable or unrepresentable detail→404 NOT_FOUND; unrepresentable list facts→500 INTERNAL. Missing/ambiguous owner provider and SQL/timeout/permission/completion/cleanup failures→503, not404. Unexpected programming/serialization failure→500. A missing detail short-circuits issuance/snapshot work.
8. Existing infrastructure/service RATE_LIMITED code maps429; retain problem-writer behavior and inject it in tests, but add no rate limiter, counter or new write. Transaction completion/cleanup failure overrides an otherwise successful or domain response with sanitized503; bytes are not sent before completion.

Use existing operations read `Code`/`Failure` and problem writer. Exact problem properties are `type:"about:blank"`, `title`, numeric `status`, `code`, server UUID `traceId`; retain current fixed titles including Invalid cursor, Cursor filter mismatch and Not found. Always no-store, Pragma:no-cache and nosniff; 401 includes WWW-Authenticate:Bearer. Never expose SQL/SQLState, provider data, tokens, keys, employee IDs, exception messages or stack traces.

No request body or caller-provided principal/role/workspace selector grants authority. No Idempotency-Key is required or inspected for these GETs; absent/arbitrary/duplicate headers must not activate idempotency or command logic. Preserve existing Accept specificity behavior (JSON-compatible positive-quality range, UTF-8 only when charset supplied; unsupported parameters or most-specific q=0 fail422). Do not introduce framework406/405/redirect aliases.

## 6. Cursor and filter contract

Use a new endpoint-specific codec, not customer IAM's codec and not direct reuse of another operations endpoint's ciphertext domain. Reuse only the existing validated AES-256 key ring and rotation rules: active key emits, retained keys decode, unknown/removed key invalidates, invalid configuration503, no TTL or production default key.

Fix the wire algorithm before coding:

- Purpose `onecall:operations-order-list:cursor:v1`; endpoint `GET /api/v1/operations/orders`; sort `createdAt desc, orderId desc`.
- Digest is SHA256 of a deterministic length-prefixed encoding with fixed ordered field names subjectId/paymentStatus, one presence byte for each, and canonical UUID bytes / exact ASCII enum bytes when present. No string-concatenation ambiguity or null/empty equivalence. Query order and UUID hex case do not change the digest.
- Digest binds **exactly the normalized structured filters**. Limit is not bound; changing only page size remains valid. Cursor does not bind employee identity: ADMIN and OPS share the same read dataset, each request reauthorizes current employee facts, and the cursor is not an authorization capability. Test cross-employee continuation plus next-request denial after disablement. Purpose/endpoint/sort bind through authenticated additional data. This is a deliberate O15 choice, not an accidental copy of the catalog-product codec's limit binding.
- Header: format version byte1, key-ID length byte, exact ASCII key-ID bytes. Then random12-byte nonce and AES/GCM/NoPadding ciphertext with128-bit tag. AAD is length-prefixed purpose, endpoint and sort followed by the header.
- Plaintext: payload-version byte1, full32-byte filter digest, epoch-second int64, nanos int32, UUID two int64 values. Big-endian fixed-width fields, exact consumption/no trailing bytes. Anchor range0001-01-01 through9999-12-31T23:59:59.999999Z, microsecond precision. Standard unpadded canonical URL-safe base64; total length must fit512. Authenticate before classifying mismatch.
- Encode/decode use defensive key/array handling and constant-time digest equality. No raw filter/order facts or material in toString/logs. No employee-ID binding: these roles read the same global dataset and every request reauthorizes; a cursor is not a capability.

Test all four filter-presence combinations, changed/added/removed filters, UNKNOWN, query-order and UUID-case normalization, same-filter continuation, different limits, endpoint/customer/cross-purpose replay, bit flips in header/nonce/payload/tag, unknown/old keys, key rotation, invalid/trailing frames, invalid time precision/range and maximum allowed length. Mismatch is never reported for an unauthenticated cursor.

## 7. SQL facts, state and data flow

Both operations execute one existing read-only REQUIRES_NEW/REPEATABLE_READ transaction. Use the existing five-second request budget with monotonic deadline checks around authority/owner calls; existing local statement_timeout2000ms, lock_timeout250ms, idle_in_transaction_session_timeout5000ms apply. Do not use the writable catalog-sale READ_COMMITTED template.

`context.enter()` witnesses non-autocommit/read-only/repeatable-read, one bound connection/PID and login/current-user relationship, then SET LOCAL ROLE operations_iam_role. Read authority there. Catalog port uses `context.scoped(Role.ORDER,...)`; issuance port uses `context.scoped(Role.TRIAL,...)`. Both restore operations_iam_role in finally. Exit uses SET LOCAL ROLE NONE and asserts original login. Existing guarded resource cleanup must restore all settings/readOnly/isolation/autocommit before reuse; do not use session-level SET ROLE, RESET ROLE, nested transactions, explicit commit/savepoints or a second connection.

OrderReader contract: `fetchOrders(Filters,Optional<Anchor>,int fetchLimit)` and `fetchOrder(UUID)` returning explicit facts/Optional; validate port inputs and bounded fetchLimit2–101. IssuanceReader contract: `fetchIssuances(List<UUID>)`; unique nonnull batch1–100 only. No empty batch SQL.

List root SQL selects only:

```sql
SELECT o.id, o.order_no, o.subject_id, o.channel,
       o.catalog_version_id, o.catalog_offer_id, o.amount_cents,
       o.currency, o.payment_status, o.created_at
FROM catalog_order.customer_order o
WHERE <only supplied structured predicates and optional seek>
ORDER BY o.created_at DESC, o.id DESC
LIMIT ?
```

Construct only fixed predicate fragments; omit WHERE when none exist. Predicates are `o.subject_id=CAST(? AS uuid)` and `o.payment_status=?`; seek is `(o.created_at,o.id)<(CAST(? AS timestamptz),CAST(? AS uuid))`. Bind every value. No account/workspace ownership predicate on employee reads, subject existence/lifecycle lookup, exclusion of terminal statuses, latest-catalog eligibility, COUNT, OFFSET, SELECT *, row/advisory locks or unbounded root result.

Validate root result count≤limit+1, nonnull identities/times, unique IDs, strict descending PostgreSQL timestamp/unsigned UUID order and strict relation to incoming anchor. Validate requested filter agreement on returned facts. Never Java re-sort. The sentinel contributes only hasMore/order integrity, not snapshot validation, response or issuance batch. Retain first limit; if none, return exactly `items:[],nextCursor:null,hasMore:false` without resolving issuance provider. Else batch only returned IDs:

```sql
SELECT order_id, status
FROM entitlement_metering.entitlement_issuance
WHERE order_id IN (<one bound UUID placeholder per returned ID>)
```

No limit/aggregate/distinct hides invalid issuance facts; read the complete status set. Validate nonnull rows, IDs belonging to this batch and statuses before grouping. Duplicate statuses across different issuance rows are legitimate. Null collections, unexpected identities, over-limit/duplicate roots or malformed list facts are INTERNAL.

Detail root SQL selects the same ten root columns plus `o.snapshot::text AS snapshot`, `o.snapshot_sha256`, `o.purchase_consent_id`, `o.paid_at`, `o.closed_at`, `WHERE o.id=CAST(? AS uuid)` only. No row returns Optional.empty and skips snapshot/issuance; present row is validated by catalog owner, then issuance is read for its one ID. InvalidSnapshotException/explicit persisted-fact invalidity yields the same404 as unavailable order; dependency and role failures must escape as503, not get swallowed by a broad corruption catch.

Payment projection is **customer_order.payment_status**, plus detail paid_at/closed_at; no nested payment field exists in OpenAPI. **payment_order must not be joined or read.** It is optional payment workflow data, not the authoritative source for these fields; its absence must not suppress an order or change its current status. payment_event/provider payload is also unnecessary. This resolves the OpenAPI x-onecall-a0-models metadata without adding a field/join.

Issuance is payment-independent:

| Complete valid raw status set | issuanceStatus |
| --- | --- |
| Empty | NONE |
| Only PENDING | PENDING |
| Only ISSUED | ISSUED |
| Only FAILED | FAILED |
| Any two or three distinct allowed statuses | MIXED |

Never derive issuance from PAID, granted units, entitlement_grant, expiry, installment count/schedule, trial or benefit quantities. No grant join is needed. UNKNOWN with no issuance is UNKNOWN/NONE; multiple equal statuses remain homogeneous. This API is a status-set projection, not issuance completeness against a commercial schedule.

## 8. Exact projection and snapshot validation

List page has exactly items,nextCursor,hasMore. `OrderSummary` has exactly 11 fields: orderId,orderNo,subjectId,channel,catalogVersionId,catalogOfferId,amountCents,currency,paymentStatus,issuanceStatus,createdAt. `Order` detail has the same 11 plus snapshotSha256,snapshot,purchaseConsentId,paidAt,closedAt = exactly 16 fields (14 required and two optional nullable timestamps). The latter two are deliberately emitted as null when absent, matching existing order DTO convention. No additional fields. Tests must compare actual key/required sets against frozen OpenAPI properties/required, not merely repeat these counts.

Snapshot has exactly schemaVersion,catalogVersionId,catalogOfferId,channel,versionToken,productCode,offerType,audience,amountCents,currency,billingCycle,installments,benefitLines,snapshotSha256. Each benefit has exactly entitlementType,quantity,validityDays,grantIntervalMonths. Use immutable records/defensive List.copyOf; UTC ISO timestamps, UUID strings and integral safe JSON numbers, not database records/maps/raw JSON.

Validate orderNo nonblank≤64 code points/valid UTF-8/no NUL; nonnull UUIDs; channel MINIAPP/WEB; currency CNY; payment enum; amount0..9007199254740991; createdAt and nonnull payment times within API time range at microsecond precision. PAID iff paidAt nonnull; CLOSED iff closedAt nonnull; payment times≥createdAt. Detail ID must match requested ID; purchaseConsentId nonnull. Nonnull facts of unsupported domains are not coerced or leaked.

Catalog adapter directly constructs existing `DefaultCustomerOrderSnapshotReader` as specified in §4, which performs storedSnapshot validation, canonical SHA256 recomputation and safe projection without any Spring/web dependency. Do not deserialize directly into a permissive DTO or hash jsonb::text bytes. Then require row↔snapshot catalogVersionId, catalogOfferId, channel, amountCents, currency and nested/top-level snapshotSha256 agreement. Preserve stored versionToken/product/benefit/schedule history even after current catalog changes or offsale. No current catalog, consent-policy or owner lifecycle join/reconstruction.

Map all snapshot fields/benefits exactly into operations-owned response types; do not add hidden internal commercial schedule, account/organization identity, idempotency key, request fingerprint, revision, provider IDs or payment payload. Do not expose the raw snapshot string from owner facts. Existing validating seam remains unchanged and independent of purchase command/policy beans.

Tests must exercise valid historical snapshots, canonical hash versus jsonb rendering, tampered hash, semantic corruption, invalid benefit/type/product, row mismatches and safe-integer overflow. Use constraint-valid fixtures or listener/mock corruptions; never disable/drop triggers/CHECKs/FKs to manufacture impossible rows. Prove PostgreSQL rejects forbidden null/status shapes separately.

## 9. One implementation slice and exact allowed paths

**S1 — employee order list/detail read foundation.** Deliver routes, trusted authority, owner ports/SQL, cursor, validating projections, tests and exact validation bookkeeping together. Each endpoint alone or a DTO/ACL layer alone would leave the confirmed capability incomplete; no independently useful increment justifies another review boundary.

Expected new production paths (all under `backend/src/main/java/com/onecall/core/`):

1. `operations_iam/subjectread/OperationsOrderReadService.java` — raw requests and frozen response records, no new error registry.
2. `operations_iam/subjectread/OperationsOrderReadPorts.java` — OrderReader/IssuanceReader, filter/anchor/typed facts.
3. `operations_iam/internal/orderread/OperationsOrderReadRequest.java` — key-free query/media validation, normalized digest and structural cursor checks.
4. `operations_iam/internal/orderread/OperationsOrderCursorCodec.java` — endpoint-specific authenticated cursor.
5. `operations_iam/internal/orderread/DefaultOperationsOrderReadService.java` — current employee authority, orchestration, list validation/issuance grouping and errors.
6. `operations_iam/internal/orderread/OperationsOrderReadConfiguration.java` — lazy existing-resource/owner-provider wiring.
7. `operations_iam/internal/web/OperationsOrderReadController.java` — exactly two operation-named GET methods, trusted principal, immutable serialization after completion.
8. `catalog_order/internal/operationsread/JooqOperationsOrderReader.java` — ORDER-scoped SQL, existing validating snapshot seam and mapping, owner-local detail corruption handling.
9. `entitlement_metering/internal/operationsread/JooqOperationsOrderIssuanceReader.java` — TRIAL-scoped raw status batch.

Expected modified production path: `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java` — add only exact GET order admission and any narrowly needed order TRACE ERROR-dispatch normalization. Existing web registration, problem writer, read context/resources/properties, module and named-interface declarations need no change.

Expected new test paths (under `backend/src/test/java/com/onecall/`):

| Path | Required assertions |
| --- | --- |
| `core/operations_iam/internal/orderread/OperationsOrderReadRequestTests.java` | Default/range/cardinality/unknown keys, UUID normalization, payment enum, Accept and parse precedence, redaction, digest truth table. |
| `core/operations_iam/internal/orderread/OperationsOrderCursorCodecTests.java` | Every cursor invariant in §6; exact envelope/domain, rotation, independent digest bytes, no limit binding and key-free structural precedence. |
| `core/operations_iam/internal/orderread/OperationsOrderReadServiceTests.java` | Authority before parse/key/owners; ACTIVE ADMIN/OPS accepted, valid DISABLED403, missing401, null/out-of-CHECK role and all malformed authority503, including malformed+disabled precedence; sentinel/order/anchor/filter validation; empty-page no issuance resolution; complete status sets; malformed order facts/list500/detail404; deadlines/dependency/completion errors. |
| `core/catalog_order/internal/operationsread/JooqOperationsOrderReaderTests.java` | Exact prepared SQL/column allowlist, parameter combinations/seek/limits; owner role scope and validated historical snapshot/row agreement; no customer/payment/provider reads. |
| `core/entitlement_metering/internal/operationsread/JooqOperationsOrderIssuanceReaderTests.java` | Batch bounds/duplicates/nulls, exact two columns, complete result fan-out, TRIAL role/restoration; no empty query/aggregate/write. |
| `archtests/OperationsOrderReadWebTests.java` | Both exact routes/operation IDs and response key equality; all status mappings/headers/redaction; route aliases/methods/auth/filter/cursor/Accept/idempotency nonrequirement; no interactions on denied gates. |
| `archtests/OperationsOrderReadConfigurationTests.java` | With only persistence profile + existing enabled read resources and owner providers, no web profile, no CustomerOrderSnapshotReader bean, no purchase-command graph and missing/invalid cursor keys: resolve the service and execute valid detail successfully using the real catalog adapter's directly constructed validator. Assert exactly one existing resource/provider per qualifier and no new snapshot bean/configuration. Missing detail or invalid stored snapshot/hash/row agreement→DETAIL-404; unavailable/ambiguous owner, SQL/role/timeout/completion failure→DEPENDENCY-503, never masked as404. Preserve condition/laziness checks and read-only template (not writable sale). |
| `archtests/architecture/OperationsOrderReadBoundaryTests.java` | Exact nine new production paths, allowed SQL/tables/columns/imports/ports/role scopes, direct stateless owner-local snapshot construction with no injected web bean, single ADMIN|OPS authority domain constant; no forbidden writes/locks/network/command/customer coupling; OpenAPI property/required sets and 11/16-field key equality; V001–V020/contracts/PA25 byte preservation; final inventories. |
| `archtests/operations/OperationsOrderReadTestProviders.java` | Mock employee/owner graph helper following existing catalog-read test-provider pattern; no @Test methods. |
| `archtests/container/OperationsOrderReadFixture.java` | PostgreSQL16, current V020 chain, real provisioned LO, synthetic employee verifier, valid snapshots/order/issuance fixtures, actual prepared-query and pool-state witnesses/no-write snapshots; no @Test methods. |
| `archtests/container/OperationsOrdersReadIT.java` | Real HTTP + actual owner-port SQL/filter/paging/snapshot/status/concurrency/EXPLAIN/no-write outcomes (§10). |
| `archtests/container/OperationsOrderReadRoleLeastPrivilegeIT.java` | Real LO role edges/denials, read-only write/lock failures, role cleanup/failure/pool reuse, unchanged privileges/topology/history (§10). |

Expected modified test/build/documentation paths:

- `backend/src/test/java/com/onecall/archtests/architecture/CustomerOrderReadBoundaryTests.java`: update only current inventory/path total assertions to measured final values; retain snapshot/customer/V020/history checks and method identities.
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerOrderCreationBoundaryTests.java`: update only current production/test/POM/include bookkeeping; do not extend its command graph allowlist with order reads or weaken command invariants.
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java`: re-pin only current production/resource aggregate counts/hashes and current test/method identities/CI pins (§11); retain all frozen members and assertions.
- `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`: re-pin only current aggregate manifest and current CI identity/count pins (§11); retain V001–V020/contracts/PA25 history and semantics.
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java`: re-pin only current production/test Java totals; preserve exact draft-owner list membership and assertions.
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java`: re-pin only current production/test Java totals; preserve every version boundary assertion.
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java`: re-pin only both README current canonical-LF pin slots (not the CRLF worktree raw hash) and the current composite digest (§11); preserve historical README pins, member lists, negative/canary checks and immutable bytes.
- `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java`: same narrow README-current/composite re-pin; preserve historical/current membership, digest construction and mutation-detection strength.
- `backend/pom.xml`: add exactly the two named IT includes; no dependency/plugin/profile/runtime change.
- `.github/workflows/ci.yml`: exact PA43 unit/container method additions, method/file/XML current totals and POM equality, historical PA42 reconstruction, order smoke probes, final inventory message only. Preserve package/dependency firewall and all prior historical maps.
- `backend/README.md`: append the PA43 current capability/validation section for these two reads and no-migration fact, using finalized measured numbers, only after the existing `## PA42 Current Provisioning Boundary` section and before `<!-- PA36 HISTORICAL BODY -->`. Do not change any byte before the PA42 heading marker, the PA42 heading marker itself, or the PA36 historical marker or any byte after it; retain the existing append-only current section and all Historical/FROZEN tables and hashes byte-identically. The new section must not introduce backticked `backend/src/main/java/...java` path tokens: reference new production types by bare class names, keeping the exact pre-marker controlled path-token count at four without deleting or replacing any of the existing four. No credential, provisioning or frozen design rewrite. Apply only the canonical-LF current-pin/composite update in §11.

This corrected implementation allowlist is exhaustive: exactly 33 paths = nine new production + one modified production + twelve new test (eight unit, two IT, two helpers) + eight modified existing test + POM + CI + backend README. The root prefixes above expand each entry into one exact repository-relative path; the plan artifact itself is the only write authorized during this planning task and is not an implementation path. Any newly discovered required path is a stop condition: enumerate its exact assertion/evidence for Controller intake, never broaden search/replace constants or weaken behavior to obtain green. Test fixtures should compose the existing database base/synthetic session helpers without changing them, copying only necessary order seeding semantics locally. No new top-level DTO/properties/package-info file is expected.

Slice validation: §10–12, focused tests then clean full verify, exact workflow checks and package/firewall proof. Stop condition: both endpoints and every required invariant pass, frozen/unrelated deltas are zero, final inventories reconcile, or any §13 blocker occurs. A partially working list/detail or skipped PostgreSQL suite is not completion.

## 10. PostgreSQL 16 and security acceptance

Use the existing Docker/Testcontainers PostgreSQL16 setup, not H2/mock SQL acceptance. The fixture must use the current provisioned LO (`pa36_operations` in existing test setup) and current V001–V020 chain; never a superuser production query connection. Fixture-admin connections may seed and collect inventories only outside measured requests. Synthetic tokens/keys remain test-only.

Required real-HTTP/database assertions:

- ADMIN and OPS see all orders, including different accounts/personal/organization workspaces, with optional exact subject/payment filtering. Employee access does not inherit customer's owner predicates or activation/real-name/risk restrictions.
- Missing/disabled employee and session failures follow §5; client employeeId/role/customer workspace spoofing cannot grant authority. Prove invalid database roles are CHECK-rejected; mock null/out-of-CHECK authority role→503, valid DISABLED→403, missing→401 and ACTIVE ADMIN/OPS→200. Assert identical service/web classification and validate malformed facts before disabled status; never weaken constraints.
- List no filters/each filter/both filters, no matches, UNKNOWN and all other statuses, default20, limit1/100, exact/tail/limit+1 boundaries, same-timestamp UUID sign-boundary ordering, no duplicate/skipped roots. Cursor combinations include filter removal/add/change, page-size change and empty continuation.
- Summary/detail exact property sets, null timestamp policy, historical snapshot/hash/row agreement, current mutable payment status, optional payment_order absent/present without changing projection; all homogeneous and mixed issuance sets and zero/one/many rows. Mutating current catalog cannot rewrite historical detail.
- Missing detail short-circuits issuance; empty list skips issuance provider/query after authorization/key validation. Nonempty page performs one root and one issuance query, not N+1; detail at most the same, excluding authority/witness statements.
- Latch-based concurrent committed payment/issuance update after root read is invisible within the current repeatable snapshot and visible in the next request. No sleep-only proof or cross-page snapshot promise.
- Inject SQL/timeout/provider/role-exit/transaction-completion/connection-close faults; verify sanitized503 and no serialized partial success. Test list corruption500/detail corruption404 separately from dependency503.
- Run real HTTP method/alias/encoded-path/TRACE probes so servlet behavior cannot bypass the filter. Keep matching customer/operations regressions green.

Authorization/ACL proof:

- LO has no object SELECT before SET. operations_iam_role cannot directly SELECT order/issuance facts; use savepoints/separate transactions for expected denial so subsequent assertions are meaningful.
- ORDER reads its explicit columns; TRIAL reads its two issuance columns; verify session_user/current_user/PID/transaction isolation/read-only state at every scope, restoration to operations then login, settings/autocommit/isolation/readOnly after success, denial, rollback, failure and next checkout.
- Real NOINHERIT LO edges are existing safe SET edges, not a test grant expansion. Deny attempts to assume owners, admin/platform/service or customer roles not already permitted; do not deny approved pre-existing LO edges.
- Under the actual read-only transaction, INSERT/UPDATE/DELETE and SELECT FOR UPDATE/SHARE fail, even though owner runtime roles have pre-existing command capabilities. Separately test DDL/default-ACL/owner-role escalation ACL denial. Distinguish read-only25006 from ACL42501, and do not claim owner roles are intrinsically read-only or that an approved owner SELECT * is ACL-denied.
- Catalog/entitlement role broad own-table SELECT is unchanged; forbidden runtime column access is enforced by exact SQL/source inventories, not a fictional new column-only ACL. No new grant or role switch is allowed to make a test pass.

No-write inventories surround successful, empty,404,401/403/422/400,429,500/503 outcomes where applicable. Inventory business tables including the existing CustomerOrdersFixture.TABLES set (catalog tables/order/payment_order/payment_event, consent, idempotency, Outbox, audit, issuance/grants/balances/trial, activations/transitions), plus unchanged employee/subject/account/membership facts. Snapshot exact row data/hashes, not counts alone. Record sequence state, schemas/relations/columns/constraints/triggers/functions, object/column/default ACLs, owner OIDs, role attributes/settings/memberships, database settings and exact Flyway metadata before/after the read campaign. Fixture/concurrent-writer changes occur outside no-write comparisons or are explicitly separated.

No runtime SQL may contain DML/DDL, idempotency/audit/Outbox functions, advisory/row locks, remote calls or SELECT *. Allow only read SELECT and existing transaction-local role/timeout setup/witness statements. Role/timeout SET LOCAL is not a business mutation. No ANALYZE/EXPLAIN in production request paths.

Representative EXPLAIN acceptance: fixture-admin seed at least10,001 constraint-valid orders across subjects/payment states, with repeated timestamps, one dense subject and at least10,000 issuance rows including high fan-out; admin ANALYZE outside measurement. Keep this seed inside the already planned `backend/src/test/java/com/onecall/archtests/container/OperationsOrderReadFixture.java`; it adds no helper path or allowlist entry. Capture `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)` for actual prepared owner SQL/real binds under ORDER/TRIAL read roles: unfiltered head and continuation; subject-only, payment-only and combined head/continuation; detail present/missing; issuance1/100 IDs and empty short-circuit. Report rows/loops/sort/scan/buffers/planning/execution and fan-out, not an invented millisecond pass threshold.

V004 has subject/status/created_at and organization/version indexes, not a general `(created_at DESC,id DESC)` operations index; V006 has order/status issuance index. A sequential scan/sort can legitimately win. Do not disable seqscan, assert every query uses an index, infer bounded scan cost from LIMIT, or add an index migration. Record reproducible plans and SQL facts; a demonstrated unacceptable timeout/scan requirement becomes Backend/Database follow-up or a blocker if it prevents this goal's acceptance.

## 11. Exact identity, historical and packaging bookkeeping

Baseline evidence is source/CI declarations, not a claim of tests run during planning: 122 Surefire classes/1164 methods +46 Failsafe classes/764 methods =168 classes/1928 methods; annotation-line scan1929 includes one retained historical comment; POM46 includes; production332/test216 Java files. V001–V020 count20; current PA25 oracle449 and customer92 are unchanged by PA43.

Enumerated layout adds nine production Java and twelve test Java (eight unit classes, two IT classes, two helpers). These are expected layout deltas, **not final generated identities**. Calculate final file/class/method/annotation/POM/XML values from implemented sources, not promised counts. If the exact layout is retained, arithmetic follows from those additions; never set a gate to a guessed number.

Deterministic guard impact, independently scanned for this fix:

| Current guard and baseline | Authorized implementation treatment |
| --- | --- |
| CustomerOrderRead/CustomerOrderCreation/OperationsCatalogDraft/OperationsCatalogVersion current global Java totals; CustomerCatalogOfferReadAcl production total | Planned layout implies production Java 332→341 and test Java 216→228. Recount final files; these are layout arithmetic, not an asserted implementation result. Resources remain23 and migrations20; no new resource is authorized. |
| CustomerCatalogOfferReadAcl production/resources excluding V019/V020:353 / SHA256 `ad4662b82014ee9a79c9342be68102c8a26f3041e82c46620c17addf702b7898`; all production/resources:355 / SHA256 `d722d31a2c0a35e26243ab40380464c94d15edbfc769c72567fa1e07ad844433` | Recompute both sorted canonical-LF path/content manifests and their aggregate counts/hashes from final bytes using the existing exact algorithms/exclusions. Preserve every existing member; add only authorized production paths. |
| EvidenceRoleIvrAssociationAcl production/resources excluding V018/V019/V020:352 / SHA256 `8f4ca11a2270169fda716622238e18ea0f4af64e943c7c1e2cf672bb39951818` | Recompute its current aggregate count/hash with unchanged exclusions, sorting and LF algorithm. A historical-sounding method name does not make its live all-production aggregate immutable. |
| CustomerCatalogOfferReadAcl current unit1164 / integration764 / annotation1929; sorted class#method identity SHA256 `0af27da2f85f6239d0e04a3a694fd4bbde7b325208a7771fbf359db80c056ae8`; current CI/POM pins also in CustomerOrderRead/Creation and EvidenceRoleIvrAssociationAcl | Recompute final identities/counts/digest and all corresponding current CI/POM assertions; preserve all existing method identities and historical reconstruction. Do not predict final methods, annotations, manifest counts or digests. |
| OperationsIvrSessionReadBoundaryTests and OperationsIvrSessionReadsIT both current README pin slots are canonical-LF SHA256 `d6674a369a3d8e2f682a5e28926ca9cbf72fa7d9a3b4e644ef11d4a82d47a418`; current composite SHA256 `59ec9b3298ab2675d63a993aae3b742d5db6ee01ba77088e92be9eeebb2a1e6a` | Set both current README pin slots to the final README's canonical-LF SHA256 (UTF-8 bytes after CRLF→LF normalization), never the CRLF worktree raw hash: verifyCurrentBytes requires actualLf==slot0==slot1. Recompute each current binary composite from the existing ordered member list/algorithm: UTF-8 path, NUL, decoded slot0 hash bytes, decoded slot1 hash bytes per member, then SHA256 of the concatenation. Raw worktree and canonical-LF hashes remain separately measured for frozen-input acceptance, not separate current README slot semantics. Historical README pins/composites and the FROZEN table remain byte-identical; no member deletion/substitution, skipped comparison or weakening of mutation canaries. |

The §9 README insertion interval is exactly the current addendum excluded by both IVR guards' historicalBytes reconstruction: retain bytes before the PA42 heading and from the PA36 historical marker onward. Later focused validation (§12) must leave OperationsIvrSessionReadBoundaryTests, OperationsIvrSessionReadsIT, EvidenceRoleIvrAssociationAclBoundaryTests and CustomerCatalogOfferReadAclBoundaryTests green with the FROZEN table, the exact consumers==4 assertion, all existing four controlled path tokens and historical bytes unchanged. Do not re-pin or weaken these frozen/historical assertions to accommodate the README edit.

The guard scan covered backend test sources, CI/POM and README for current exact Java/resource counts, method/annotation identities, aggregate manifests and README-current pin references; no additional deterministically affected guard path was found beyond §9. Re-scan final implementation before validation; a newly discovered path stops work. Only **current aggregate** counts/hashes/identity and README-current pins may be re-pinned, with assertion strength/list membership retained (authorized additions only). Individual frozen V001–V020/OpenAPI/docs12/14/15/17/PA25 bytes and their hashes/checksums, historical baselines/pins, owner lists and behavioral/security assertions are immutable. A changed aggregate is not permission to update a frozen input.

CI must introduce explicit `pa43_unit_additions`/`pa43_container_additions` class→method sets for every new or deliberately added test method, assert each addition exists, then remove those names/annotation hits only for reconstructing the existing PA42 baseline. Do this before the existing PA42 removal/rename normalization. Preserve the PA42 CustomerOrdersWebTests historical rename and every older baseline map/total/comment witness. Existing method identities stay unchanged.

Update both early Surefire count gates and late source/CI/XML totals, current class maps, annotation-line totals, Failsafe include equality and final messages together. Preserve exact class→method source=CI=XML equality, zero skips/errors/failures, disjoint identities and no missing/extra/stale reports. Include exactly OperationsOrdersReadIT and OperationsOrderReadRoleLeastPrivilegeIT; helpers are not test suites.

New architecture guard must assert exact implementation file allowlist, owner-only SQL, stable named-interface port method sets, no new module edge/resource/configuration, exact response records and immutable contract/migration/PA25 manifests. Run ModuleBoundaryTests' real module verification and adversarial forbidden/internal/cycle fixtures; retaining declarations alone is insufficient.

Retain all V001–V020 Flyway checksums, prefix-target semantics, 20 successful latest history rows, no V021, PA25 exact grant/oracle/provenance/topology reconstruction and existing historical digest tests. No migration-specific suite/oracle target advancement is needed. Inventory changes never justify editing individual frozen-input or historical hashes; only the explicitly identified current aggregate guards in §11 may be recomputed.

Package proof uses the built executable JAR: complete BOOT-INF/classes set equals target/classes, every corresponding entry byte-equal, no duplicates, all twenty migrations exactly once and byte-equal, and expected new order classes present. Do not hard-code JAR hash/byte/entry counts before packaging. Run existing production dependency firewall, baseline/SSM profile JAR comparisons and adversarial probes; no dependency delta is authorized.

## 12. Validation commands and documentation decision

Run from this worktree with the required Git exports. Use the repository Maven wrapper (CI pins Maven3.9.9/JDK21.0.13), the actual `container-smoke` profile in backend/pom.xml and Docker PostgreSQL16 access. Do not install/upgrade tools or repair unrelated failures. Planning writes only this document; the commands below are implementation acceptance instructions, not executions claimed here.

```bash
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call2
export GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa43-next-work-unit/codex-call
git diff --check
cd backend
./mvnw -B -ntp -Dtest=OperationsOrderReadRequestTests,OperationsOrderCursorCodecTests,OperationsOrderReadServiceTests,JooqOperationsOrderReaderTests,JooqOperationsOrderIssuanceReaderTests,OperationsOrderReadWebTests,OperationsOrderReadConfigurationTests,OperationsOrderReadBoundaryTests test
./mvnw -B -ntp -Dtest=ModuleBoundaryTests,CustomerOrderReadBoundaryTests,CustomerOrderCreationBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsCatalogDraftBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogProductReadBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,CustomerOrdersWebTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=OperationsOrdersReadIT,OperationsOrderReadRoleLeastPrivilegeIT,OperationsIvrSessionReadsIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

Then execute the actual repository CI checker blocks from `.github/workflows/ci.yml` in their documented order against fresh full-run XML, including historical source/CI/POM/XML equality, web smoke, packaged class/resource parity and the production dependency firewall's baseline/SSM builds/probes. The focused Failsafe run alone has partial XML and must not be presented as full inventory proof. Use the workflow's actual dependency/profile commands, including its runtime dependency listing, not a homegrown approximate checker.

Frozen-input manifest must cover raw worktree SHA256 separately from canonical-LF hashes used by historical tests; never conflate CRLF checkout bytes with Git blob bytes. Compare before/after V001–V020, api/openapi, docs12/14/15/17, PA25 and unchanged customer sources. Check actual changed paths against §9. Validation artifacts and reports are produced only in later authorized execution/report locations, not by this planning task.

Documentation decision: no design/OpenAPI/Control/PA25/runbook edits. Implementation may update only backend/README's current capability and measured validation inventory within §9. No separate implementation report/closeout is authorized by this plan-writing task. Controller decides later reporting/gate actions.

## 13. Risks, residuals, open questions and stop rules

| Item | Classification and owner/destination |
| --- | --- |
| Existing LO can SET command-capable owner roles; application transaction enforces read-only | Accepted existing security boundary, not a claim of column-only/read-only database roles. Security/Database owns any later dedicated read-role hardening; deferred outside PA43. |
| No optimized global operations keyset index; issuance fan-out unbounded in row count | Measured acceptance risk. Backend/Database records EXPLAIN and deadlines; index/schema work is deferred and must not be folded into this slice. Stop if demonstrated failure makes the agreed read capability unachievable. |
| Authority is one request's repeatable snapshot, not linearizable revocation/RLS/cross-page snapshot | Residual owned by Security/Backend; tests prove next-request visibility, not stronger guarantees. |
| Shared operations cursor key ring operational custody/rotation and replica consistency | Existing deployment responsibility of Security/Operations; no new key. Production provisioning/key assignment/TLS/live validation remain NOT EXECUTED. |
| Owner-local snapshot mapping duplicates only frozen API value shape | Backend/catalog owner owns exact mapping/schema tests; never duplicate canonical commerce validation or refactor customer code in this slice. |
| origin/main differs from supplied checkpoint HEAD | Nonbehavioral provenance discrepancy owned by Controller. Record exact hashes (§1); no remote mutation or reset. |
| Frozen metadata names payment_order while projection needs no join | Resolved contract interpretation: public status is on customer_order; optional model metadata does not require gratuitous reads. If a reviewer requires payment fields absent from Order, stop for contract decision rather than add them. |
| Unavailable Docker/toolchain or unrelated baseline failure | Validation blocker, not permission to skip/claim success or fix unrelated code. Record command, exact failing test and disposition owner. |

No unresolved functional authorization/schema/contract question was found in direct source inspection. Runtime ACL/transaction/HTTP proofs are still required; this is not a runtime passing claim.

### Accepted review decisions — resolved by plan-fix-1

| Review item / decision | Disposition and binding section |
| --- | --- |
| A F1 / B F1–F2: incomplete current aggregate/README guard allowlist | RESOLVED: all six guard paths added to exhaustive 33-path implementation set (§9), baseline counts/hashes and narrow recomputation rules (§11), focused unit/IT validation (§12). No frozen contract/migration relaxation. |
| A F3 / B F4: summary/detail cardinality | RESOLVED: exactly11 summary and16 detail fields, explicit OpenAPI key/required cross-check (§8–9). |
| B F5: snapshot seam/profile wiring | RESOLVED: owner-local direct stateless DefaultCustomerOrderSnapshotReader construction, no injected web bean/new bean/configuration; persistence-only successful detail, DETAIL-404 versus DEPENDENCY-503 assertions (§4, §8–9). |
| A F4 / B F6 and authority open questions | RESOLVED: malformed/out-of-CHECK role503; valid DISABLED403; missing401; ACTIVE ADMIN/OPS accepted; no third supported role or conflicting unsupported-role403 canary (§5, §9–10). |
| A F2 / B F3: reviewer Control-hash findings | REJECTED by Controller as defects; provenance clarification RESOLVED in §1: cd1987 at authoring, edd1011 later review bookkeeping, current dispatch1fa06277 binding for this fix. No history rewritten. |
| Cursor identity binding / shared dataset | RESOLVED: no employee binding; ADMIN/OPS same dataset, every request reauthorizes (§6). |
| payment_order optional metadata | RESOLVED: not read/joined; public payment facts come from customer_order (§7). |
| 429 open question | RESOLVED: inherited infrastructure error mapping/test injection only; PA43 adds no limiter/counter/write (§5). |
| EXPLAIN seed/helper scope | RESOLVED: 10,001-order/10,000-issuance seed stays in the already listed OperationsOrderReadFixture, no helper path (§10). |
| Additional guard/path discovery | RESOLVED: §9 is exhaustive; newly required paths stop for Controller intake (§9, §11). No remaining plan decision is delegated to implementers. |

### Fresh rereview decisions — plan-fix-2

- Fresh rereview B F1 (Medium), `docs/reviews/plan-review-20261126-175521.md`: **accepted** and **resolved** by plan-fix-2 (`pa43-plan-fix2-gpt-20261126-180000`). §9 binds the exact append-only README insertion interval and unchanged historical bytes/markers/four path tokens; §11 binds both current pin slots to final canonical-LF SHA256, the unchanged composite member algorithm and the four focused guards with FROZEN and consumers==4 unchanged. No frozen assertion is weakened or re-pinned; earlier adjudications remain intact.
- Fresh rereview B F2 (Low), same artifact: **accepted** and **resolved** by plan-fix-2. §4 now attributes entitlement_metering_role's schema USAGE to V004:492–493 and its table SELECT, INSERT to V006:649; the read relies only on SELECT and continues to prohibit V021 and privilege widening.

Blocking questions must be evidence-backed: if a frozen field/domain cannot be represented by the validating seam, an owner role/SET edge is absent, an implementation would require forbidden migration/contract/customer change, or a previously unknown exact guard requires an unlisted path, stop and report its file/line, actual failing fact, minimal decision needed and owner. Do not speculate or ask to widen scope without evidence. Refund/provider/payment repair, audit endpoints, catalog schedule redesign, performance indexes and other future capabilities are deferred to their owners, never implemented here.

## 14. Implementation completion format

Later authorized completion must state: task/slice identity; exact two operations and allowed changed paths; measured file/class/method/annotation/POM/XML inventories plus historical equality; raw/LF frozen manifests; no-migration/no-grant/no-write evidence; SQL/role/pool/snapshot/cursor/EXPLAIN findings; exact commands with pass/fail/skipped status; packaged JAR hash/bytes/class-resource parity/firewall result; risk dispositions and any blocker.

Explicitly state no source outside the allowlist, no customer behavior change, no contract/migration/PA25 modification, no production/live validation claim, and no stage/commit/push/PR/merge/approval/ready/reviewer/external-comment action. This plan itself does not authorize any such action.
