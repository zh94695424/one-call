# PA37 Operations Catalog Product Read Foundation — Code-Generation-Ready Plan

Task label: `pa37-operations-catalog-product-read-plan-fix3-gpt-20261117-01`
Work unit: `PA37-operations-catalog-product-read-foundation`
Date: 2026-11-17 (Asia/Shanghai)
Gate: plan only; no implementation or later gate is authorized by this artifact.

## 1. Binding goal and stop boundary

Implement exactly these two already-frozen Operations reads:

- `GET /api/v1/operations/catalog/products` / `listOperationsCatalogProducts`.
- `GET /api/v1/operations/catalog/products/{productId}` / `getOperationsCatalogProduct`.

Reuse the existing `employeeSession` audience `onecall:employee-session:v1`, shared
Operations read datasource/transaction template, `OperationsReadContext` with
`ORDER("catalog_order_role")`, and shared cursor key ring. Require a current server-
derived `ACTIVE` `ADMIN|OPS` employee. Do not edit OpenAPI, migrations, grants,
roles, datasource/pool/key-ring configuration, or add catalog mutation/publication/
off-sale, trial, order, payment, audit, frontend, media, generic query/cache, or
unrelated module scope. No `catalog_offer` read is authorized or required.

The one implementation slice is S1, an end-to-end behavior family: exact route
admission, request/cursor parsing, DTOs, service/context wiring, owner-local reader,
configuration, focused tests, PostgreSQL16 tests, and the mandatory inventory mirrors.
No S2 is proposed.

## 2. Frozen contract and exact behavior

The frozen route/DTO source is `api/openapi/onecall-v1.openapi.yaml:1578-1696,5606-5740`.
List accepts only `cursor` and `limit`, each at most once; `limit` defaults to 20 and
is `1..100`. Detail accepts only a canonical lowercase hyphenated UUID. List returns
`CatalogProductPage(items,nextCursor,hasMore)` ordered by `productCode asc, productId
asc`; detail returns the exact frozen product/version fields. Arrays are never null.
DRAFT, ACTIVE, and OFFSALE versions remain visible as persisted facts.

`publishedAt` is always declared and emitted: RFC3339 for published states and JSON
`null` for DRAFT. `priceCents` is required-null exactly for `QUOTE`; non-`CNY`
persisted currency is a sanitized `503`. Benefit lines are ordered by
`catalog_benefit_line.id`; versions are ordered by `version_no,id` (and the list
phase-two result by `product_id,version_no,id`). A zero-version product is represented
as one product item with `versions=[]` and is never dropped.

Authority-first precedence is mandatory and follows shipped PA35. Enter the one
read-only `REPEATABLE_READ` transaction and perform the current employee authority
check before query/cursor parsing or key-ring/provider resolution. Wrong audience is
`401 UNAUTHENTICATED`; disabled is `403 FORBIDDEN`; malformed/invalid persisted
authority is sanitized `503`. These win over malformed filters, blank/invalid cursor,
or missing/ambiguous ring. Blank `cursor=` is `400 CURSOR_INVALID`; other malformed
query/path input is `422 VALIDATION_FAILED`. A valid cursor with a different request
is `400 CURSOR_FILTER_MISMATCH`. Preserve declared `429 RATE_LIMITED`, but PA37 does
not implement a rate limiter. Detail absence is non-disclosing `404 NOT_FOUND`.
Missing/ambiguous dependencies, key ring, timeout, ACL, role cleanup, or transaction
failure are sanitized `503`; unexpected programming failures are sanitized `500`.

Authority × ring precedence is tested explicitly: valid authority × valid ring
succeeds; valid authority × missing/ambiguous/malformed ring is `503`; disabled,
wrong-audience, or malformed authority × every ring state returns the authority error
without resolving/using the ring. Detail never resolves the ring.

The authority SQL is exactly
`SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)`.
This authority query precedes parsing; cursor authentication and request-digest
comparison then precede every catalog SQL statement. A digest mismatch is
`400 CURSOR_FILTER_MISMATCH`, never a catalog lookup. Accept negotiation follows
the shipped PA35 request `accept()` rules unchanged. The request test
`authorityFailurePrecedesMalformedRequest` uses a service harness; the parser does
not acquire authority responsibilities.

## 3. Exact projections and SQL boundary

The Operations DTOs contain only the frozen fields. List version summaries use
`catalog_version.id,version_no,sale_status,offer_type,audience`; detail additionally
uses approved price/currency/billing/installment/benefit/revision/published-at fields.
Never expose snapshot JSON/hash, creator, offer token/status/channel/id, order/payment/
provider fields, role names, credentials, media, internal SQL, or an invented field.
`x-onecall-a0-models` is an inventory annotation, not a join mandate.

The owner-local adapter reads only approved columns from `catalog_product`,
`catalog_version`, and `catalog_benefit_line`. It must not read, join, or `EXISTS`
`catalog_order.catalog_offer`: zero offers are valid, the table permits 0..2 channel
rows, an offer-less DRAFT remains in both responses, and multiple channel offers do
not alter membership. Docs15's broader “offer summary” wording without a frozen
public field is a Contract/Data + Catalog-order-owner residual, not an implementation
allowance.

List SQL is exactly two-phase in one transaction/snapshot:

1. Page roots from `catalog_product` with approved root columns,
   `ORDER BY product_code COLLATE "C" ASC, id ASC`, exclusive anchor
   `(product_code COLLATE "C", id) > (CAST(? AS text) COLLATE "C", CAST(? AS uuid))`
   with code then UUID bound in that order, and
   `LIMIT :limit + 1`.
2. Discard the lookahead root from the emitted set; read versions only for the
   `<=limit` emitted root IDs, never the lookahead root, ordered by
   `product_id,version_no,id`, then attach them in root order. No product/version
   left-join is used for paging, and no version row consumes a root slot.

Java key comparison uses the same UTF-8 bytewise `C` ordering. Reachable collation
fixtures are exactly `AB`, `A-1`, `A_1`, and `A0`; no impossible non-ASCII fixture is
used. Detail reads the product, its versions, and benefit lines only. No `FOR UPDATE`
or write is used.

V004 privilege assertions must reflect the actual frozen matrix, not a false SELECT-
only claim: `catalog_order_role` has table SELECT/INSERT on all seven owner tables,
selected column-level UPDATE including sensitive version columns, function EXECUTE,
and schema USAGE; DELETE is not granted. PA37 separately asserts the actual grant
matrix remains unchanged and structurally proves the production reader references
only approved columns/tables. Read-only PostgreSQL masks reachable mutation attempts
as SQLSTATE `25006`, including DELETE despite its absent grant; the DML, DDL and
`FOR UPDATE` denial tests assert read-only behavior, not a SELECT-only grant matrix.
Read-only PostgreSQL does not deny advisory-lock functions. The read path must neither
use nor leave an advisory lock: static SQL/token checks forbid their use and the IT
compares backend-scoped `pg_locks` advisory entries before/after success and failure,
including inside the transaction before cleanup (not merely after transaction end).
The pre-existing broad role is recorded as `deferred-with-owner` to Database/Security +
Backend and is not changed.

## 4. Transaction, cursor, and capacity rules

Use one existing `REQUIRES_NEW`, read-only, `REPEATABLE_READ` transaction. Authority
is read in `operations_iam_role`; the adapter is scoped in `catalog_order_role`; role,
backend, login, read-only, timeout, and cleanup state are asserted and restored on
success and every failure. No second datasource, pool, role, template, key ring, or
cursor table is added.

The opaque AES-GCM cursor uses the existing validated shared key ring, not a fork.
The exact outer frame is: version byte `0x01`, 1-byte key-id length, 1..32-byte
ASCII key id, 12-byte random nonce, AES-GCM ciphertext, 16-byte authentication tag.
The authenticated header is the version/length/key-id bytes. Encode with the active
key id; decode by that exact id only, never by trying every key. A retained old key
continues to decode after active-key rotation; removed/unknown ids are
`400 CURSOR_INVALID`. Malformed/missing/ambiguous ring configuration is sanitized
`503` after authority succeeds, distinct from an invalid cursor key id.

The encrypted plaintext, in order, is: purpose/version byte `0x01` for this catalog
list format, 4-byte big-endian normalized limit, first 16 bytes of the SHA-256 request
digest, 16-byte UUID (most-significant then least-significant 64 bits, big-endian),
1-byte UTF-8 code length, and product-code bytes. The valid V004 code is 2..64 ASCII
bytes. No UUID or product-code anchor bytes occur outside the ciphertext. Base64url
without padding wraps only the outer frame; generated cursors must not be a base64url
serialization of raw product code/UUID. Assert opacity by inspecting the decoded
outer layout and authenticated round-trip, not by a probabilistic substring test.

Define `LP(s)` as 4-byte unsigned big-endian UTF-8 byte length followed by those
bytes. AAD is exactly
`LP("onecall:operations-catalog-product-list:cursor:v1") || LP("GET /api/v1/operations/catalog/products") || LP("productCode asc, productId asc") || header`.
This binds the intended purpose, exact endpoint, exact sort and complete key-id
header. Wrong-purpose/endpoint/sort/header or tag authentication fails as
`400 CURSOR_INVALID`; reject malformed version, lengths, UUID/code, trailing bytes,
invalid base64url, and frames above the 512-character input maximum.

Canonical request digest bytes are the concatenation of four fields in fixed order:
`purpose`, `endpoint`, `sort`, `limit`. Encode each as `LP(fieldName) || presenceByte
|| uint32be(valueByteLength) || valueBytes`, with presence `0x00` and length zero for
absent fields, or presence `0x01` for present fields. All four are present here:
the first three values are the UTF-8 strings in the AAD tuple; limit value is the
4-byte big-endian normalized integer. An omitted limit is explicitly normalized to
20 and encoded identically to explicit `limit=20`; limit is covered, so changing it
on continuation produces `400 CURSOR_FILTER_MISMATCH`. The transport `cursor` is
excluded from the digest; unknown/repeated query parameters are rejected, not added
as digest fields. Hash these bytes with SHA-256 and compare the authenticated
16-byte prefix and normalized limit before catalog SQL, after authority SQL.

Maximum plaintext is `1+4+16+16+1+64=102` bytes; maximum outer frame at key-id32 is
`1+1+32+12+102+16=164` bytes; unpadded base64url length is
`ceil(164*8/6)=219 <=512`. Tests use key-id32/product-code64 and assert this bound
and `nextCursor.length <= 512`. Within the eight codec methods below, cover the
target codec round-trip, active key, retained old key, removed/unknown key, wrong
purpose, tampering, malformed frame and limit/digest mismatch. `nextCursor` is
required-null at tail.

The cursor is an anchor, not a snapshot token. Tests cover `limit=1`, `limit=100`,
empty/last/exact-boundary pages, multi-version roots at boundaries, no duplicate or
skipped roots, and continuation with `AB`, `A-1`, `A_1`, and `A0`. Unbounded persisted
per-product version arrays/cursor capacity remain a Capacity/Database + Catalog-order
owner residual.

## 5. Exact implementation allowlist and inventory

The following is the complete allowlist. No implementation-time co-location or
addition is permitted.

**Existing files permitted for lockstep edits**

- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java`
- `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`
- `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
- `backend/pom.xml`
- `.github/workflows/ci.yml`

`backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsReadContext.java`
is NOT writable: it must remain byte-unchanged, SHA-256
`75c2e3b4b3cc251e8fe43223512be89e510c711059bf280ffbd466a9848adcdf`.
`Role.ORDER` already exists; assert the unchanged hash and no PA37 token or branch
in this file. Reusing it requires no shared-plumbing change.

**Non-code evidence artifact**

- `docs/gateflow/pa37-operations-catalog-product-read-foundation-implementation.md`

This new implementation report is allowlisted solely as evidence and is excluded
from production, test, CI class/method/scan, manifest and POM counts.

The lockstep architecture test may change only its full-tree production manifest count
and digest plus the exact CI current-literal mirrors required by the eight new
production files and ten new test files. It must retain universal coverage of every
regular `src/main/java` and `src/main/resources` file; PA37 paths must not be excluded.
The current production manifest is 291 files with digest
`95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0`; expected count
is exactly 299 for the eight listed production files. The final digest is recomputed
from the full sorted LF-normalized manifest and recorded in the implementation report;
no alternative co-location/addition is allowed.

**Eight new production Java files**

- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogProductReadController.java`
- `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsCatalogProductReadService.java`
- `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsCatalogProductReadPorts.java`
- `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadRequest.java`
- `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductCursorCodec.java`
- `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/DefaultOperationsCatalogProductReadService.java`
- `backend/src/main/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadConfiguration.java`
- `backend/src/main/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsCatalogProductReader.java`

The catalog-owner reader self-registers as
`@Component("operationsCatalogProductReader")`, `@Profile("persistence")`,
`@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")`
and `@Lazy`, exactly the production order counter's existing condition/lazy pattern.
It implements the Operations port and uses the existing qualified read DSL/context;
Operations consumes only the port via `ObjectProvider`, with no operations→catalog
concrete import, concrete `@Bean` construction or extra module edge.
`OperationsCatalogProductReadConfiguration` uses PA35's exact
`@Configuration(proxyBeanMethods = false)`, `@Profile("persistence")`, the same
enabled-property condition above and class-level `@Lazy`; its service `@Bean` is
also `@Lazy`. Reuse the existing qualified transaction/DSL/context and lazy provider
resolution; no new property, resource or eager key-ring resolution is authorized.

**Ten new test Java files**

Exact paths and FQCNs (no alternate placement):

| Exact path | Exact FQCN | Inventory |
|---|---|---|
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadRequestTests.java` | `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests` | unit 8 |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductCursorCodecTests.java` | `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests` | unit 8 |
| `backend/src/test/java/com/onecall/core/operations_iam/internal/catalogread/OperationsCatalogProductReadServiceTests.java` | `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests` | unit 18 |
| `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadWebTests.java` | `com.onecall.archtests.OperationsCatalogProductReadWebTests` | unit 10 |
| `backend/src/test/java/com/onecall/archtests/OperationsCatalogProductReadConfigurationTests.java` | `com.onecall.archtests.OperationsCatalogProductReadConfigurationTests` | unit 8 |
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogProductReadBoundaryTests.java` | `com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests` | unit 8 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadsIT.java` | `com.onecall.archtests.container.OperationsCatalogProductReadsIT` | container 20 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadRoleLeastPrivilegeIT.java` | `com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT` | container 16 |
| `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogProductReadFixture.java` | `com.onecall.archtests.container.OperationsCatalogProductReadFixture` | helper 0 |
| `backend/src/test/java/com/onecall/archtests/operations/OperationsCatalogProductReadTestProviders.java` | `com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders` | helper 0 |

The method dictionaries below map these exact FQCNs (not simple-name aliases) to
the listed method sets, in table order. Use these FQCNs in CI dictionary keys,
source-equality checks, XML reconciliation, focused Maven selectors and the
implementation report. The helpers never enter dictionaries or selectors.

Six unit/architecture test classes, with exact `@Test` identities and counts:

- `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests` (8): `rejectsUnknownAndRepeatedParameters`, `acceptsDefaultAndBoundaryLimits`, `rejectsInvalidLimits`, `rejectsMalformedCanonicalUuid`, `blankCursorIsCursorInvalid`, `authorityFailurePrecedesMalformedRequest`, `detailRejectsAnyQueryParameter`, `requestDigestIsCanonical`.
- `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests` (8): `roundTripsExactFrame`, `rejectsTampering`, `rejectsWrongPurpose`, `rejectsFilterMismatch`, `rejectsMissingAndRetiredKeys`, `rejectsMalformedFrame`, `rejectsOversizedEncodedCursor`, `keepsGeneratedCursorWithinOpenApiMaximum`.
- `com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests` (18): `activeAdminReadsList`, `activeOpsReadsDetail`, `authorityRunsBeforeRequestAndRing`, `disabledWinsOverMalformedFilter`, `wrongAudienceWinsOverMissingRing`, `malformedAuthorityWinsOverInvalidCursor`, `ringAuthorityPrecedenceMatrix`, `usesOneRepeatableReadTransaction`, `usesTwoPhaseRootAndVersionRead`, `mapsZeroVersionRootToEmptyArray`, `mapsDraftPublishedAtToNull`, `mapsQuotePriceToNull`, `rejectsNonCnyCurrency`, `rejectsInconsistentPersistedFacts`, `mapsTailCursorToNull`, `mapsLookaheadToHasMore`, `restoresRoleOnSuccess`, `restoresRoleOnFailure`.
- `com.onecall.archtests.OperationsCatalogProductReadWebTests` (10): `admitsExactListGet`, `admitsExactDetailGet`, `concealsOtherMethodsAndUris`, `rejectsInvalidAccept`, `serializesDeclaredNullPublishedAt`, `serializesRequiredNullPriceCents`, `serializesTailNullNextCursor`, `rejectsDetailQueryParameter`, `mapsFrozenErrors`, `doesNotExposeUnapprovedFields`.
- `com.onecall.archtests.OperationsCatalogProductReadConfigurationTests` (8): `reusesSharedDatasource`, `reusesSharedTransactionTemplate`, `reusesOrderRoleContext`, `reusesSharedCursorKeyRing`, `resolvesProvidersLazily`, `rejectsMissingProvider`, `rejectsAmbiguousProvider`, `doesNotConstructNewResources`.
- `com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests` (8): `scansAllEightProductionFiles`, `rejectsSelectStarAndForbiddenTokens`, `rejectsWritesAndLocks`, `rejectsOfferReads`, `rejectsUnapprovedTablesAndColumns`, `assertsApprovedProjection`, `assertsNoNewResourceConstruction`, `preservesFrozenOpenApiReference`.

Two Failsafe classes, with exact identities:

- `com.onecall.archtests.container.OperationsCatalogProductReadsIT` (20): `readsAdminAndOps`, `returnsAllVersionStates`, `retainsOfferlessDraft`, `ignoresMultiChannelOffersForMembership`, `returnsZeroVersionProduct`, `ordersRootsWithCCol`, `pagesLimitOne`, `pagesLimitOneHundred`, `pagesExactBoundary`, `pagesLastPage`, `doesNotDuplicateOrSkipRoots`, `ordersVersions`, `ordersBenefits`, `returnsDeclaredPublishedAtNull`, `returnsQuotePriceNull`, `rejectsNonCnyCurrency`, `returnsDetailHistory`, `returnsEmptyBenefits`, `rejectsMalformedFacts`, `keepsRepeatableReadSnapshot`.
- `com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT` (16): `observesFrozenRoleMatrix`, `selectsApprovedProjection`, `deniesInsertByReadOnlyTransaction`, `deniesUpdateByReadOnlyTransaction`, `deniesDeleteByReadOnlyTransaction`, `deniesForUpdate`, `deniesDdl`, `assertsNoAdvisoryLocks`, `restoresRoleAfterSuccess`, `restoresRoleAfterFailure`, `assertsReadOnlyState`, `assertsTimeoutState`, `assertsNoNewGrant`, `assertsNoNewRole`, `assertsNoNewSchema`, `assertsNoNewFunction`.

The two zero-test helpers are `OperationsCatalogProductReadFixture.java` and
`OperationsCatalogProductReadTestProviders.java`; they are excluded from both CI
identity dictionaries and from the `@Test` method totals.

ACL assertion ownership uses only the existing method identities above:

- `OperationsCatalogProductReadBoundaryTests.rejectsUnapprovedTablesAndColumns`
  and `assertsApprovedProjection` assert approved tables and columns;
  `rejectsWritesAndLocks` rejects `FOR UPDATE`, DDL, and advisory locks.
- `OperationsCatalogProductReadRoleLeastPrivilegeIT.observesFrozenRoleMatrix`
  compares the unchanged V004 matrix; `selectsApprovedProjection` asserts the
  approved tables/columns. The existing `deniesInsertByReadOnlyTransaction`,
  `deniesUpdateByReadOnlyTransaction`, and `deniesDeleteByReadOnlyTransaction`
  methods assert read-only SQLSTATE `25006`, with actual grants asserted separately;
  `deniesForUpdate` and `deniesDdl` retain read-only denial coverage;
  `assertsNoAdvisoryLocks` asserts absence of use/retained advisory locks, not denial.
- Its `restoresRoleAfterSuccess` and `restoresRoleAfterFailure` assert restoration;
  `assertsNoNewGrant`, `assertsNoNewRole`, `assertsNoNewSchema` and
  `assertsNoNewFunction` assert empty before/after deltas relative to the existing
  PA36 catalog oracles (including the 410-tuple ACL and 7/9/11 topology oracles),
  not duplicate whole-cluster scans or new copies of those oracles. The frozen
  matrix comparison includes column UPDATE and cross-schema SELECT facts, not
  merely the three projected tables.
  These are additional assertions within the listed methods, not new identities.

Thus the ten files are exactly six unit/architecture classes, two container IT
classes, and two zero-test helpers: unit `8+8+18+10+8+8=60` methods and container
`20+16=36` methods. The CI dictionaries reproduce only these eight test-bearing
classes and their listed method names; no extra ACL class or identity is permitted.

**Measured and expected inventory**

- Current: unit 83 classes / 788 methods; container 34 / 556; total 117 / 1,344;
  annotation line scan 1,345; Failsafe container-smoke includes 34 / all-POM
  `<include>` elements 36; guarded production manifest 291 (raw tree 292).
- Expected: unit 89 / 848 (+6 classes/+60 methods); container 36 / 592 (+2/+36);
  total 125 / 1,440; annotation line scan 1,441; Failsafe container-smoke includes
  36 / all-POM `<include>` elements 38; guarded production manifest 299 (raw tree
  300). Helpers add no identity or method.
- CI must add exactly `pa37_unit_additions` with the six unit dictionaries above and
  `pa37_container_additions` with the two container dictionaries above; exact source
  class equality and exact XML inventory remain enforced.
- CI implementation checklist (all required, no unrelated CI changes):
  add `pa37_unit_additions` lengths `[8,8,18,10,8,8]` and
  `pa37_container_additions` lengths `[20,16]`; merge their exact FQCN/method sets
  into `unit_expected`/`container_expected` and the additions/baseline union.
  Within each source class form `pa37_names` from those maps. OR `pa37_names` into
  every earlier `paNN_start_totals` subtraction and `paNN_start_source`/line-scan
  subtraction: PA19,20,21,22,23,25,27,28,29,30,31,32,33,34,36,35, plus baseline
  totals/source/line-scan and every additions/baseline union. Preserve each existing
  subtraction mechanism (including PA34's full-match removal) and earlier expected
  historical values; none may absorb the 96 PA37 methods.
  Define PA37-only subtraction of `pa37_names` from current method/source scans;
  add only real PA37 historical assertions `pa37_start_totals == [788, 556]`
  (sum1344) and `pa37_start_line_scan_total == 1345`.
  Set current literals to `method_totals == [848, 592]`, sum1440,
  `line_scan_total == 1441`, `len(unit_expected) == 89`,
  `len(container_expected) == 36`, `len(includes) == 36`, exact XML848/592;
  also replace the earlier Surefire aggregate check and its diagnostic788→848,
  and the two `exact_inventory` call totals with848/592.
  update the aggregate line-scan assertion's current tail to1441.
  Update the CI diagnostic tail to PA37, `89/848 + 36/592 = 125/1440`, retain all
  earlier history, append `PA37-start 788+556=1344 -> current 848+592=1440`,
  line scans `... -> 1233 -> 1345 -> 1441`, and `POM 36 includes`.
  Update the existing architecture-test CI mirrors for `len(unit_expected)`,
  `len(container_expected)` and `len(includes)` to89/36/36. There are no historical
  variants of the old83/34/34 mirror literals: replace them, do not preserve or
  invent historical class/include assertions. The `[788, 556]` and1345 method/scan
  history remains only via the real PA37 subtraction assertions.
  Do not add a comment/Javadoc containing the test-annotation token in new Java
  files: the existing single `comment_hits` entry is unchanged, so scan delta is96.
- `backend/pom.xml` adds exactly these two Failsafe include lines and no others:
  `<include>**/OperationsCatalogProductReadsIT.java</include>` and
  `<include>**/OperationsCatalogProductReadRoleLeastPrivilegeIT.java</include>`.
  Surefire has no include delta. No inventory identity may be co-located or added
  during implementation.

## 6. Test and validation contract

Tests must prove exact route/method behavior, authority-first precedence, ring ×
authority precedence, canonical UUID/query rules, all frozen DTO fields/nulls,
version-array and benefit-line order, exact authority SELECT projection, two-phase
paging, offer-less DRAFT membership, multi-channel membership invariance, zero-version
roots, collation anchors, limits/boundaries/no duplicate or skipped roots, cursor
framing/512 maximum, snapshot consistency, role restoration, and sanitized errors.

ACL tests must compare the unchanged V004 matrix, not claim SELECT-only privileges.
Production static scans must prove no `catalog_offer` token/read, no forbidden table or
column, no `SELECT *`, write, lock, audit/outbox, new resource, or owner violation.

Run the repository gate, not ad hoc contract scripts:

```text
cd api/openapi && npm ci && npm run check:s3
cd ../../backend && ./mvnw -B -ntp -DfailIfNoTests=true -Dtest=com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadRequestTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductCursorCodecTests,com.onecall.core.operations_iam.internal.catalogread.OperationsCatalogProductReadServiceTests,com.onecall.archtests.OperationsCatalogProductReadWebTests,com.onecall.archtests.OperationsCatalogProductReadConfigurationTests,com.onecall.archtests.architecture.OperationsCatalogProductReadBoundaryTests test
./mvnw -B -ntp -Pcontainer-smoke -DfailIfNoTests=true -Dfailsafe.failIfNoSpecifiedTests=true -Dit.test=com.onecall.archtests.container.OperationsCatalogProductReadsIT,com.onecall.archtests.container.OperationsCatalogProductReadRoleLeastPrivilegeIT verify
./mvnw -B -ntp -Pcontainer-smoke verify
```

Also execute the existing CI source/history/token/POM/XML reconciliation, packaged-JAR
checks, PostgreSQL16 and hosted CI evidence at implementation. This plan gate runs no
runtime tests because no implementation is authorized.

## 7. Residual owners and completion stop

- Contract/Data + Catalog-order owner: docs15 offer-summary wording without a frozen
  public field; no `catalog_offer` read or invented field.
- Database/Security + Backend: pre-existing broad `catalog_order_role`, recorded as
  deferred-with-owner and unchanged.
- Capacity/Database + Catalog-order owner: unbounded per-product version arrays and
  cursor capacity.
- Platform/Security + Backend/Database: `COLLATE "C"`, timeout, dependency, and key
  delivery behavior.
- CI/release: exact inventory, packaged-JAR, PostgreSQL16, and hosted execution evidence.
- Operations IAM/Backend: authority-first precedence and no rate limiter implementation.

The implementation report at
`docs/gateflow/pa37-operations-catalog-product-read-foundation-implementation.md`
must include all changed paths exactly matching §5, all ten exact test paths/FQCNs
(the helpers explicitly excluded from test-bearing inventory), before/after counts,
the production manifest count and recomputed digest, the unchanged context hash,
current and historical CI literals, POM includes, exact commands run/not run, and
all residual owners. The report itself is excluded from every code/test/CI count.
Stop without widening scope if any frozen contract, approved projection, ACL,
role restoration, read-only snapshot, or exact inventory assertion cannot be proven.
Do not stage, commit, push, open/update a PR, approve, mark ready, request reviewers,
comment externally, merge, close, or enter a later gate.

## 8. Retained closure register and lineage

All prior closure rows remain binding; this fix changes only accepted second-round
re-review precision. Authoritative pre-fix1 cumulative plan SHA-256 is
`e0a5d3435cecd2e60919021757326bf6bab4c36d68a787eb3ef8f14b3ee8e069`
(35,714 bytes / 514 lines), as recorded in Control and the initial reviews. This
supersedes fix1's historical unavailability claim without editing fix1. Fix3 input
is the fix2-output cumulative plan SHA-256
`7d9cb67c07485655783b90ed74857df336b3c8a91e4613daa2dc626ae69098c6`
(19,346 bytes / 270 lines). Fix1/fix2 artifacts and both re-reviews remain unchanged.

| Finding | Binding closure retained |
|---|---|
| `PA37-PLAN-REV-01` | §5 existing tree/CI lockstep, guarded manifest291→299, full normalized digest and universal coverage/no new exclusions. |
| `PA37-PLAN-REV-02` | §5 eight production/ten test files; exact six-unit/two-IT/two-helper paths, FQCN/method inventory, CI/POM arithmetic. |
| `PA37-PLAN-REV-03` | §§1/3 no offer read/join/EXISTS; inventory annotation only; offer-less DRAFT and multi-channel membership invariant. |
| `PA37-PLAN-REV-04` | §§3/4 two-phase root-bounded snapshot paging, emitted-root-only version reads, zero-version roots/no duplicate or skipped roots. |
| `PA37-PLAN-REV-05` | §§3/5 unchanged truthful V004 matrix, approved projection, read-only denial and deferred broad-role owner. |
| `PA37-PLAN-REV-06` | §3 C collation on ordering/typed exclusive anchor, matching UTF-8 comparison and reachable fixtures. |
| `PA37-PLAN-REV-07` | §2 authority-first and ring×authority precedence, blank/detail/error/429 behavior, no rate limiter. |
| `PA37-PLAN-REV-08` | §6 repository check:s3 gate and focused Maven no-test guards, exact six-unit/two-IT selectors. |
| `PA37-PLAN-REV-09` | §§2–4 declared nulls/QUOTE/CNY, exact authority SELECT, ordering and encrypted cursor/512 bound. |
| `PA37-PLAN-FIX1-REV-01` | §5 six unit60/two IT36/two helper0; no redundant ACL class/selector; fixed current/history counts. |
| `PA37-PLAN-FIX1-REV-02` | This lineage records the authoritative pre-fix1 hash; historical fix1 bytes stay unchanged. |
| `PA37-PLAN-RR2-01` | §§3/5 truthful no-advisory-use method identity and behavior; IT count16 unchanged. |
| `PA37-PLAN-RR2-02` | §4 exact encrypted key-id envelope/AAD, ring rotation semantics, 164-byte/219-character maximum and codec coverage. |
| `PA37-PLAN-RR2-03` | §5 exact CI additions/history/current/mirrors/diagnostic checklist and correctly scoped POM counts. |
| `PA37-PLAN-RR2-04` | §§5/6 ten exact paths/FQCNs and exact FQCN dictionary keys/selectors/equality/report. |
| `PA37-PLAN-RR2-05` | §5 catalog self-registration and PA35 configuration conditions/lazy semantics, no concrete import/new edge. |
| `PA37-PLAN-RR2-06` | §§2/4 exact authority SQL and presence-aware digest with normalized limit20, checked before catalog SQL/mismatch400. |
| `PA37-PLAN-RR2-07` | §§3/5 context byte-unchanged/hash/no PA37 branch; actual grants separate from read-only25006 denial. |
| `PA37-PLAN-RR2-08` | §§5/7 exact implementation-report path allowlisted as non-code evidence outside every inventory count. |
| `PA37-PLAN-RR2-09` | §§3/5 emitted-root-only phase2, typed C anchor, delta checks against PA36 oracles; totals and inventory unchanged. |
