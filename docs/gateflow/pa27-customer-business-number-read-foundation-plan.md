# PA27 customer business-number read foundation: implementation plan

- Task: `pa27-business-number-plan-gpt-20261111-02`.
- Work unit: `PA27-customer-business-number-read-foundation`.
- Date: 2026-11-11, Asia/Shanghai; snapshot: `/mnt/d/codex-temp/pa27-plan-20261111-03`, metadata-free.
- Status: cumulative proposal amended by plan-fix-1; findings addressed in plan text, pending Controller disposition; no implementation authorization or passing-execution claim.
- Fix provenance: `pa27-plan-fix1-gpt-20261111-01`, 2026-11-11, snapshot `/mnt/d/codex-temp/pa27-plan-fix1-20261111-01`; input plan SHA256 `190e24b2ae984e3202abf01f4dece3f0356b9a118664eb172237f54f2c4108d8`. Original task/snapshot above remain historical provenance. Dispositions: `docs/gateflow/pa27-customer-business-number-read-foundation-plan-fix-1.md`.
- Binding authority: `docs/gateflow/project-execution-control.md`, active PA27 goal confirmation and user-confirmed `PA27-PLAN-01` resolution.
- Prior provenance: read the blocked artifact at this same path before replacement; raw SHA256 `1b7e96db4618271a66d271856368b5ed4abeab52a5e415744052ecddcf3eceae`. Its ambiguity is resolved, not silently defaulted.
- Gate: **stop before re-review**. Only this cumulative plan and the plan-fix-1 artifact are written in this fix; no source, test, SQL, CI, OpenAPI, Control, Git or external-state mutation is authorized.

## 1. Goal, evidence and single slice

Implement only `GET /api/v1/customer/business-numbers` / `listCustomerBusinessNumbers`. Preserve the OpenAPI and V001-V014 bytes. Derive account and selected workspace exclusively from the verified customer session. Return exactly the masked frozen projection with deterministic keyset pagination, current lifecycle authorization, no business writes, and minimum PostgreSQL read privileges.

Direct evidence inspected:

- OpenAPI operation at lines 334-389, `CursorParam`/`LimitParam` at 4020-4038 and `BusinessNumber`/`BusinessNumberPage` at 4425 onward: default 20, limit 1-100, cursor at most 512 characters, seven item fields and three page fields.
- `CustomerBearerAuthenticator`, `TrustedCustomerCaller`, `SelectedCustomerWorkspace`, `CustomerBoundaryFilter`, `CustomerSharesController`, `CustomerProblemWriter`: established authentication, selected-workspace, route, parsing, error and no-store behavior.
- `CustomerContextService`, `JooqCustomerContextRepository`, `CustomerPersistenceConfiguration`: qualified lazy pool, transaction-aware prepared jOOQ, read-only transaction and transaction-local role assertion. A separate context transaction is insufficient for PA27.
- V003 and V007: immutable subject ownership, binding-to-subject relationship, all binding/activation enums, one activation per subject when present; no requirement that an activation row exists.
- `customer_iam`, `subject_number`, `activation_consent` package declarations, `ModuleBoundaryTests`, `CustomerApiBoundaryTests`: 14 modules; customer currently has four sharing named-interface dependencies, no subject/activation query seam.
- V014, PA25 executable, PA26/PA25 tests and CI: current customer 12-tuple ACL, global seven-role 247-tuple oracle, exact inventory and historical-prefix testing.

**S1: complete authenticated masked business-number listing.** One composed, behaviorally verifiable slice includes route/service, owned query seams, cursor, V015, PA25/check oracle, tests, CI and documentation. Backend is integration owner; Subject-number/Activation-consent own query semantics; Database owns privilege/provisioning changes; Security reviews grants and cursor key handling; CI owns measured inventory. Do not deliver an uncallable SQL-only or mock-only intermediate slice as completion. Build dependencies may be implemented in order within S1; they are not separate acceptance slices.

Acceptance: literal matrix below, exact response/errors, PostgreSQL 16 execution through LC/customer role, approved privileges only, zero writes, pool restoration, source=CI=XML evidence, frozen-byte checks, updated bounded operator documentation and implementation completion report. No frontend/deployment/live acceptance is claimed.

## 2. Literal visibility and lifecycle contract

| Selected verified workspace | Required current facts | Subject ownership predicate | Read roles |
| --- | --- | --- | --- |
| PERSONAL, organization ID absent | account exists and status ACTIVE | `s.owner_account_id = :accountId` | no organization role required |
| ORGANIZATION, organization ID present | account ACTIVE, selected organization ACTIVE, membership for this exact account/organization ACTIVE | `s.owner_organization_id = :organizationId` | ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR, all equally permitted |

Use explicit role membership in the four-value allowlist, not administrator-only and not a role hierarchy. Account, organization and membership IDs/status/role are read again from PostgreSQL; stale session assertions never substitute for these checks. Missing/disabled account and absent/inactive organization or membership fail generically with 403 `FORBIDDEN`, matching the established shares current-context behavior. They never return a partial page. A valid active context with no owned rows returns 200 with an empty page. Neither outcome identifies whether an unrelated subject exists.

After that context check, include **every** binding of each owned subject:

- `binding.status`: ACTIVE, DISABLED and RETIRED, all included, no active-only predicate.
- `service_activation.status`: INACTIVE, ACTIVE and SUSPENDED, all included; absent activation row projects INACTIVE. A present invalid value is an internal projection failure, not a missing-row default.
- `business_subject.real_name_status`: UNVERIFIED and VERIFIED, both included.
- `binding.number_type`: MOBILE and LANDLINE, both included.
- No activation, binding verification time, source, claim, risk-state or real-name filter is added.
- Unowned subjects, subjects owned by another account/organization and subjects in a different selected workspace are excluded, even if their binding mask/HMAC matches an accessible row. Personal ownership does not grant organization-scope visibility or vice versa. An organization listing is not restricted to a binding creator or to subjects personally owned by the reader.

The confirmed matrix is a product/security decision, not inferred from response enum membership. It closes `PA27-PLAN-01`; no blocking visibility question remains.

### 2.1 Exact response

`BusinessNumberPage` has exactly `items`, `nextCursor` (present, nullable), `hasMore` (boolean). Each item has exactly:

1. `bindingId`: binding UUID.
2. `subjectId`: subject UUID.
3. `numberMasked`: stored `number_masked`, never reconstructed from secrets.
4. `numberType`: MOBILE or LANDLINE.
5. `bindingStatus`: ACTIVE, DISABLED or RETIRED.
6. `activationStatus`: INACTIVE, ACTIVE or SUSPENDED, with missing-row INACTIVE.
7. `realNameStatus`: UNVERIFIED or VERIFIED.

Use explicit records/enum conversion, not serialization of persistence records/maps. No owner field, created timestamp, total count, revision, verification/source field, full number, ciphertext, number HMAC, raw SQL, key, exception/cause or diagnostic field. Internal sort anchors never become extra JSON properties. Validate projected UUIDs/enums/mask constraints; inconsistent data fails the whole page as sanitized 500 `INTERNAL`.

## 3. Request and error precedence

1. Existing boundary first admits only the exact GET path and canonical request URI. Extend its existing GET allowlist by this one path. Non-GET methods, trailing slash, path suffixes and noncanonical path encodings remain the existing sanitized 404; do not add a broad customer route rule, HEAD endpoint or new filter.
2. Existing authenticator validates one customer bearer header, audience and exactly one verifier. Missing/invalid/duplicate/wrong-audience credentials: 401 `UNAUTHENTICATED`; unavailable verifier: 503 `SERVICE_UNAVAILABLE`. Authentication precedes query parsing and database work.
3. Require `CustomerRequestPrincipal` and a structurally valid selected workspace. Missing selection, null workspace kind, PERSONAL with organization ID, or ORGANIZATION without it: 503 `SERVICE_UNAVAILABLE`, exactly the shares behavior. No fallback to personal scope.
4. Parse limit before cursor, as shares do. Absent limit means 20. Exactly one ASCII digit string is accepted if Java integer parsing succeeds and value is 1-100 inclusive. Thus `001` is accepted as 1, consistent with shares; empty, signs, whitespace, decimal, exponent, Unicode digits, zero, 101, overflow, repeated identical or differing values: 422 `VALIDATION_FAILED`. Do not silently choose one duplicate value.
5. Structurally validate exactly one optional cursor before resolving the keyed codec: empty/repeated/noncanonical, wrong length/alphabet/decoded size/version, invalid timestamp or failed roundtrip: 400 `CURSOR_INVALID`. Limit 422 wins over structural cursor 400. With structurally valid or absent cursor, validate the mandatory key next: missing/malformed/wrong-length key is 503 `SERVICE_UNAVAILABLE`, including first/empty pages. Only with a valid key verify MAC and scope; failed MAC or scope mismatch is 400 `CURSOR_INVALID`, before lifecycle. Structural checks must not instantiate a key-validating bean prematurely.
6. Resolve lazy service/dependencies and open the read transaction; current lifecycle failure: 403 `FORBIDDEN`. SQL/transaction/connection/bean and role-assumption/current-role assertion failures: 503 `SERVICE_UNAVAILABLE`. Unexpected logic/projection failure: 500 `INTERNAL`. Preserve code only when rethrowing known exceptions, never a sensitive cause chain or message.

Public precedence: the existing route guard returns sanitized 404 `NOT_FOUND` for non-GET/noncanonical paths before authentication (unchanged boundary behavior). On an admitted exact GET: authentication 401/503 -> selected-workspace structure 503 -> limit 422 -> structural cursor 400 -> missing/invalid cursor-key 503 -> keyed MAC/scope cursor 400 -> current lifecycle 403 -> successful projection. Technical failures at a reached stage are sanitized 503; projection/logic failures are sanitized 500, never a substitute for an earlier rejection. Thus invalid limit plus bad cursor/key is 422, structural bad cursor plus absent key is 400, and absent/valid-structure cursor plus absent key plus inactive context is 503, not 403. No downstream lookup runs after an earlier failure; no error reveals ownership, foreign row existence or another workspace's lifecycle. Route 404 remains unconditional, not an authority oracle.

Use the existing `CustomerContextException.Code` and `CustomerProblemWriter`; no new public error code. Success and Problems retain no-store/pragma/nosniff headers; 401 retains the existing Bearer challenge and trace ID behavior. Unknown query fields, including `accountId`, `organizationId`, `workspace`, `ownerAccountId`, cannot affect authority and are ignored as in the shares controller. Never read selector headers/body/attributes as ownership inputs. No rate limiter is introduced merely because RATE_LIMITED is representable by the frozen contract; retain existing infrastructure behavior and do not manufacture 429 responses.

## 4. Transaction, owned seams and exact queries

### 4.1 One read transaction and one qualified pool

Create a **separate** `customerBusinessNumberTransactionTemplate`, using the existing `customerQueryTransactionManager`, REQUIRES_NEW, read-only, isolation REPEATABLE_READ. Do not change the existing customer/sharing transaction templates. Lifecycle check, ordered binding query and activation batch run on the same transaction-aware `customerQueryDsl`/physical connection and snapshot. This avoids a READ_COMMITTED gap between separate lifecycle and page statements without locking rows or adding writable privileges.

At transaction start execute `SET LOCAL ROLE customer_iam_role`, then assert `current_user = 'customer_iam_role'` before any protected read. Role-assumption failure, missing current-role row or role mismatch aborts with sanitized 503 `SERVICE_UNAVAILABLE`, without retaining SQL/cause/message; never map these to 500 or continue to a reader. No owner-role switch, dynamic role name, extra datasource, direct DriverManager connection, session-level SET ROLE, manual commit, session setting or cross-transaction context call in production. Existing LC is the sole login. Spring cleanup must restore role, isolation, readOnly and autoCommit on success, denial and exception; test connection reuse with a single-connection pool. Fully qualify all tables, use bound parameters, prepared statements and disabled SQL logging.

REPEATABLE_READ defines current lifecycle as the read transaction snapshot; a lifecycle change committed before that snapshot must deny. A concurrent change committed after snapshot creation applies on the next request. Do not promise linearizable revocation or cross-request snapshot pagination. No locks, long-lived transaction, snapshot token, session epoch or lifecycle cache is introduced.

### 4.2 Minimal module API

Add only two public named interfaces, using public packages rather than exporting internal implementations:

- `subject_number.customerquery` annotated `@NamedInterface("customer-business-number-query")`. `CustomerBusinessNumberReader` exposes `List<Row> fetch(Scope scope, Optional<Anchor> anchor, int fetchSize)`. Nested immutable `Scope(UUID accountId, UUID organizationId)` uses null organization only for personal; `Anchor(Instant createdAt, UUID bindingId)`; `Row(UUID bindingId, UUID subjectId, String numberMasked, String numberType, String bindingStatus, String realNameStatus, Instant subjectCreatedAt)`. Constructor/request validation prevents missing IDs or illegal fetchSize (2-101 for endpoint requests). No owner IDs returned in Row.
- `activation_consent.customerquery` annotated `@NamedInterface("customer-activation-query")`. `CustomerActivationStatusReader` exposes `Map<UUID,String> findStatuses(Set<UUID> subjectIds)` for a bounded non-null set of at most 101 subject IDs. Return only present subject/status pairs; absent row handling belongs to the composition service. Empty input performs no SQL. No customer/session types or jOOQ/connection types in either public interface.

Their respective modules own implementations and lazy `web` configuration. Internally inject the same qualified `customerQueryDsl`; a shared Spring bean qualifier is not a Java import of another module's internal configuration. Readers never start transactions or change roles. Customer's composition service calls them only inside the transaction it owns. No internal implementation imports across modules. Do not add an activation-to-subject query dependency: the activation query uses only supplied subject IDs and its own table.

Append exactly `subject_number::customer-business-number-query` and `activation_consent::customer-activation-query` to customer's four existing allowed dependencies. Preserve all other allowedDependencies including activation's existing `subject_number` edge. Production discovered module edges become exactly four: customer -> sharing, subject_number, activation_consent; sharing -> evidence. Module count remains 14; customer allowed named dependencies become six. Do not open whole modules or copy existing sharing internal-import exceptions into PA27.

### 4.3 Customer lifecycle query

`JooqCustomerBusinessNumberAccessRepository.requireActiveContext` selects only account `id,status`; organization `id,status`; membership `account_id,organization_id,status,role` (existing privileges). Use the exact trusted IDs and ACTIVE predicates. Personal never needs a membership. Organization must pass all three entity checks and the four-role allowlist. No account display name is needed by PA27, but its old grant stays for `/customer/me`. Checks run before the owned page query in the same repeatable-read transaction; denial prevents either module reader from running.

### 4.4 Subject-number page query

Use two fixed parameterized SQL variants (personal/org), differing only in owner predicate. Select the following, no `*`:

```sql
SELECT b.id AS binding_id, s.id AS subject_id, b.number_masked,
       b.number_type, b.status AS binding_status, s.real_name_status,
       s.created_at AS subject_created_at
FROM subject_number.business_subject s
JOIN subject_number.caller_number_binding b ON b.subject_id = s.id
WHERE /* personal: s.owner_account_id = :accountId
         organization: s.owner_organization_id = :organizationId */
      /* if cursor present, also:
      AND (s.created_at < :anchorTime
           OR (s.created_at = :anchorTime AND b.id > :anchorBindingId)) */
ORDER BY s.created_at DESC, b.id ASC
LIMIT :fetchSize
```

The comments describe generation choices, not literal executable predicate placeholders. No OR across two ownership domains, no unowned branch, no status filter, OFFSET, distinct-subject collapse, Java sort or UUID string comparator. PostgreSQL's native UUID order is the tie-break, including across the Java UUID signed-long boundary. Bind times at PostgreSQL microsecond precision; preserve them as Instant, not JVM-local formatted text.

### 4.5 Activation composition

For unique subject IDs in the fetched candidate rows (at most limit+1), query only `SELECT subject_id,status FROM activation_consent.service_activation WHERE subject_id IN (...)` with bound UUIDs. No consent/order/activation-reference columns, no joined table, no status filter. Validate the returned map contains only requested IDs, no duplicate subject rows and valid statuses; fail closed otherwise. Project missing entries to INACTIVE. Unrequested activation rows are neither selected nor returned. This bounded second query is not N+1 and does not change ordering or cardinality.

Strip lookahead before response construction. The service's nested immutable Request/Page/Item records carry only pagination input, seven-field items and an optional internal Anchor for continuation. It must not leak the subject reader's internal timestamp via record serialization.

## 5. Endpoint-specific authenticated cursor

Existing shares uses an unkeyed scope digest; copying it would not authenticate anchors. PA27 uses one small endpoint-only HMAC codec, not a reusable cursor framework or a change to sharing.

### 5.1 Payload and scope

Encode exactly 93 bytes as canonical unpadded base64url, exactly 124 ASCII characters:

| Byte offsets | Content |
| --- | --- |
| 0 | version byte 1 |
| 1-32 | 32-byte scope SHA-256 |
| 33-40 | big-endian signed epoch seconds (8 bytes) |
| 41-44 | big-endian nanoseconds (4 bytes), 0 <= n < 1,000,000,000 and n divisible by 1,000 |
| 45-60 | binding UUID bits, most-significant then least-significant, big-endian (16 bytes) |
| 61-92 | HMAC-SHA256 (32 bytes) |

Scope digest UTF-8 input, with exactly the shown LF separators and lowercase canonical UUIDs:

```text
onecall:customer-business-numbers:v1
subject.createdAt:desc,bindingId:asc
<verified-account-uuid>
PERSONAL
```

For organization replace the final PERSONAL line with `ORGANIZATION:<selected-organization-uuid>`. Include a final LF in both forms. The account remains in organization scope, so another account in the same organization cannot replay the cursor. No channel binding, session epoch or client-provided owner selector; same-account/same-workspace session refresh can continue. Scope UUIDs are hashed, not embedded as plaintext owner metadata. HMAC input is UTF-8 `onecall:customer-business-numbers:cursor:v1\n` (a literal LF) followed by payload bytes 0-60; use a separate Mac instance per operation or otherwise thread-safe construction. Compare MAC and expected scope with constant-time digest comparison.

### 5.2 Canonical validation and key

Input first obeys frozen maximum 512; this version then requires length 124, regex `[A-Za-z0-9_-]+`, decoded length 93, version 1, exact re-encoding equality, valid timestamp fields and roundtrip equality. Reject padding, whitespace, alternate base64 alphabets, trailing data, impossible dates, non-microsecond nanoseconds and unknown versions. Allowed UTC calendar year range is -4712 through 294276, matching the existing bounded timestamp policy; reject date conversion overflow. Binding UUID has no version/variant restriction because PostgreSQL uuid does not impose one. A valid MAC authenticates the precise anchor; no anchor-row lookup is needed, so a deleted anchor does not invalidate a legitimate continuation. Reject any payload/MAC/scope byte change and any wrong account/workspace, without echoing the value. Do not call SHA-256 scope binding an authenticity proof: the HMAC is that proof.

Add nonsecret configuration wiring `onecall.customer.business-numbers.cursor-hmac-key: ${ONECALL_CUSTOMER_BUSINESS_NUMBER_CURSOR_HMAC_KEY:}`. The value is a canonical padded standard-base64 encoding of exactly 32 externally supplied random bytes. Blank/malformed/wrong-length key is 503 when this endpoint needs its codec; never derive a key from bearer tokens, database passwords, public IDs, source literals or random per-request/per-process defaults. The codec is obtained lazily and validated for every endpoint invocation, including a first/empty page, so configuration failure is deterministic; unrelated web/default profile boot and endpoints remain unaffected. No key or properties value in toString/logs/Problems. Tests use explicit nonproduction keys.

One configured key, no key IDs, storage, key service, expiry or rotation registry. All replicas must use the same key. External key replacement deliberately invalidates existing cursors with CURSOR_INVALID; Security/Operations own safe distribution and rollout. This is an operational residual, not a license to assign a live secret during implementation.

### 5.3 Page semantics

Fetch `effectiveLimit + 1` rows; `hasMore = candidateCount > effectiveLimit`. Return first effectiveLimit rows in database order. If hasMore true, `nextCursor` encodes the last **returned** row (not the lookahead). Otherwise it is JSON null. Empty results always have empty items/false/null. A single row with limit 1 has false/null unless a second row exists. Cursor limit is intentionally not bound; clients may change limit within 1-100 on continuation. No offset/count query or previous-page API.

On fixed data, successive pages enumerate each visible binding exactly once. Across requests, new rows before the anchor are not retroactively returned; newly committed data after the anchor can appear, and deleted rows disappear. Every request reauthorizes, including a valid cursor. No snapshot-consistency promise across pages or cursor TTL is added to the frozen contract.

## 6. Minimum exact database privilege extension

### 6.1 Necessity and exact delta

Current customer privileges: two schema USAGE plus ten column SELECT = 12 explicit ACL tuples. Existing subject/binding/activation reads are impossible under LC -> customer_iam_role. Java delegation does not create SQL authority. Switching to owner/write roles, broad table SELECT, new membership, SECURITY DEFINER view/function or using another pool is either broader or adds unnecessary machinery. Therefore add exactly one append-only privilege-only migration:

`backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql`.

All following grants target `customer_iam_role`, are issued by the named object owner, and have **no grant option**:

| Owner and grantor | Object | Privilege | Columns / explicit tuple count | Reason |
| --- | --- | --- | --- | --- |
| subject_number_owner | schema subject_number | USAGE | 1 | resolve two qualified tables |
| subject_number_owner | subject_number.business_subject | SELECT | id, owner_account_id, owner_organization_id, real_name_status, created_at: 5 | join/projection, two scope variants, sort/seek |
| subject_number_owner | subject_number.caller_number_binding | SELECT | id, subject_id, number_masked, number_type, status: 5 | join, projection, tie-break |
| activation_consent_owner | schema activation_consent | USAGE | 1 | resolve activation table |
| activation_consent_owner | activation_consent.service_activation | SELECT | subject_id, status: 2 | bounded lookup/projection |

Delta = 2 schema + 12 column = **14**. Customer final = 4 schema + 22 column = **26**. Evidence remains 16. Seven-role independent oracle = 247 + 14 = **261**; 219 other-role tuples remain unchanged. Grantors/owners are part of equality, not just has_column_privilege booleans. Old ten columns remain because existing endpoints require them. No service_activation id, binding verified_at/revision/number_ciphertext/number_hmac, subject source/revision, ownership write, table SELECT, sequence, routine EXECUTE, INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER, schema CREATE or WITH GRANT OPTION.

### 6.2 Migration behavior

Migration authority is the existing approved migrator, never runtime LC. Use one transactional DO block with fixed validated identifiers and bounded catalog preflight, followed by owner-issued grants via `SET LOCAL ROLE subject_number_owner` / `activation_consent_owner`, restoring the invoking role before exit. No CREATE/ALTER/DROP of business/schema/role/function/view/trigger/index objects, role membership, default ACL or PUBLIC ACL changes. No data DML or backfill. The only durable effect is the 14 tuples plus normal Flyway history.

Preflight must reject unauthorized runtime execution, wrong target/owner attributes, owner/object mismatch, role graph/settings/provenance drift and unsafe direct/inherited/PUBLIC/default privileges, using the same V014/PA25 catalog evidence pattern scoped to this change. Check the exact existing customer 12 tuples with original owner grantors plus either none or **all** of the 14 approved tuples. Permit only clean pre-state or exact final state for raw SQL replay; reject mixed/partial additions and unexpected customer privileges, wrong grantor or grant option. Verify six-login/seven-effective-role/four-edge boundary when externally provisioned; on a clean migration chain, absent PA25 external slots are allowed, but partially present or unmarked/adopted slots are rejected. The three direct login boundaries remain frozen. Preserve existing verifier bytes, attributes, role OIDs and provenance markers.

Use sanitized categories such as V015_AUTHORITY_REJECTED, V015_OWNERSHIP_REJECTED, V015_TOPOLOGY_REJECTED, V015_ACL_REJECTED and V015_SETTINGS_REJECTED without object IDs, role credentials or SQL detail. Exact category suffixes are internal; tests assert the chosen fixed categories. Rejection rolls back every grant; do not repair broad or malformed state by revoking it. Postflight proves exact customer 26, unchanged evidence/other ACLs, owners, definitions, graph, PUBLIC/default ACLs and business data. A raw replay on exact final state changes nothing; Flyway itself applies V015 once. Do not replay V014 after V015 as a downgrade: its exact historical ACL oracle deliberately differs.

### 6.3 PA25 and historical/latest-chain compatibility

Update `backend/ops/postgresql/pa25-runtime-identities.sql` in both apply/check modes:

- Append the exact 14 owner/grantor tuples to expected_acl, taking its independent full seven-role oracle to 261. Do not derive expectations by reading the just-installed migration or current database ACLs. Preserve all 247 original tuples, ordering/canonical handling and all rejection logic except the explicit delta.
- Append exact history tuple `015`, script `V015__customer_business_number_read_privileges.sql`, description `customer business number read privileges`, Flyway-calculated checksum; change exact successful SQL history count 14 to 15. Determine the checksum with Flyway on the finalized migration; do not guess, substitute SHA256 or query an unvalidated target to establish expected history. V001-V014 checksums stay unchanged. V014-only is now rejected by current PA25 apply/check.
- Continue no object-ACL repair; apply only reconciles the previously approved login provenance/edges. Check remains read-only. No new password/credential behavior, authority relaxation, broad repair, OID marker version or graph edge.

Update the existing 20-method `ProductionDatabaseIdentityProvisioningIT` without adding/renaming methods: fresh migration execution and latest history size 15, all known terminal-history corruption probes move from rank14/014 to rank15/015, synthetic extra-rank cleanup becomes rank1015, duplicate terminal and future version probes use 015/016. The bad-rank assignment becomes `installed_rank=16`, decimal alias becomes `version='15'`, and restoration deletes versions `015`/`15` before restoring the saved terminal row. Preserve negative tests against every earlier row, including V014. Extend the independently literal ACL oracle by precisely the 14 tuples, change both expected/actual ACL size assertions from 247 to 261, and retain all previous credential/provenance/tamper cases. Do not change harmless fixture numeric values merely because they contain 14.

Preserve the twelve PA26 methods and their **historical V014** 12/16-tuple assertions. Pin their `latest` Flyway fixture instances, including fresh-chain replacement, to target 014; applied remains 14 with V015 ABOVE_TARGET. In the fresh-chain history/frozen-resource helpers distinguish applied V001-V014 from all 15 discovered resources. Iterate only applied entries for exact history; the existing predecessor logical-hash loop must explicitly filter V001-V013 rather than accidentally looking up V015 in its thirteen-entry FROZEN map. V014 raw identity is independently checked against this plan's baseline. At the tail of `v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle` and `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects`, finish the V014 assertions/replay first, then use a separate un-targeted Flyway instance to apply V015 and run the updated current PA25 apply/check. Never rerun V014 assertions/replay after that tail upgrade. This retains the historical proof and current provisioning proof without embedding an obsolete duplicate PA25 script or relaxing V014's 12-tuple oracle.

`PostgresContainerIT` stays at its intentional V013 target and 64 test methods. Update discovery-only `info().all()` size from 14 to 15, include V015 in complete script discovery and assert both V014/V015 ABOVE_TARGET; applied history remains exactly 13, historical privileges unchanged. No broad replacement of historical 13/14 expectations.

Explicitly remove the V014-time PA25 calls from their old positions inside each credentialed/uncredentialed iteration: the `psql("apply"); psql("check")` pair near original line 127 in `v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle`, and both apply/check pairs near original line 323 in `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects`. Relocate them to the end of the same iteration, only after all historical V014 assertions (including the first method's post-pair `assertOracle`, now performed before upgrade), replay snapshots and historical validation checks. Use a separate untargeted Flyway, migrate and validate through V015, assert fifteen applied entries, then run one apply/check pair in the first method and two consecutive apply/check pairs in the replay method. No PA25 apply/check remains at V014 state; no V014 oracle/replay runs after the V015 upgrade. Preserve both credential variants and current PA25 idempotency proof.

Latest-chain application fixtures/suites move only their final-chain history expectations to 15: CustomerMeFixture, SharingQueryFixture, SharingQueryServiceIT, SharingRoleLeastPrivilegeIT, SharingAdmissionRoleLeastPrivilegeIT, EvidenceMediaReadRoleLeastPrivilegeIT, EvidenceMediaReadIT, PublicShareAdmissionIT. In `EvidenceMediaReadIT`'s nested fixture `start()` (original line 245), change the one relevant unpinned `flyway.migrate().migrationsExecuted` assertion from 14 to 15; retain untargeted full-chain migration/validation, all eight methods and every media/query/denial assertion without weakening behavior. Preserve V014 index13 script assertion and add V015 index14 where asserting terminal order. All unrelated query/command/media/denial semantics remain unchanged. Test source names/counts remain unchanged in all existing classes.

## 7. Exact implementation file allowlist

Paths below are prospective S1 authorization only, not paths changed by this planning task. No wildcard production/test expansion. Any necessary additional path or changed test count returns to Controller before implementation scope expansion. N = new, M = modify. All paths are repository-relative.

Exact prospective allowlist: **48 unique paths = 25 M + 23 N** (original 44 + two existing tests + two existing docs). By section: production 18 (4 M/14 N), database/tests/CI 25 (17 M/8 N), documentation 5 (4 M/1 N). The two planning artifacts are not S1 implementation targets and are not counted here.

### 7.1 Production

```text
M backend/src/main/java/com/onecall/core/customer_iam/package-info.java
M backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java
M backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java
M backend/src/main/resources/application-web.yml
N backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumbersController.java
N backend/src/main/java/com/onecall/core/customer_iam/internal/web/BusinessNumberPage.java
N backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorCodec.java
N backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorProperties.java
N backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryService.java
N backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/JooqCustomerBusinessNumberAccessRepository.java
N backend/src/main/java/com/onecall/core/subject_number/customerquery/package-info.java
N backend/src/main/java/com/onecall/core/subject_number/customerquery/CustomerBusinessNumberReader.java
N backend/src/main/java/com/onecall/core/subject_number/internal/customerquery/JooqCustomerBusinessNumberReader.java
N backend/src/main/java/com/onecall/core/subject_number/internal/customerquery/CustomerBusinessNumberQueryConfiguration.java
N backend/src/main/java/com/onecall/core/activation_consent/customerquery/package-info.java
N backend/src/main/java/com/onecall/core/activation_consent/customerquery/CustomerActivationStatusReader.java
N backend/src/main/java/com/onecall/core/activation_consent/internal/customerquery/JooqCustomerActivationStatusReader.java
N backend/src/main/java/com/onecall/core/activation_consent/internal/customerquery/CustomerActivationQueryConfiguration.java
```

Configuration uses existing conditional web/profile/lazy bean conventions, enables cursor properties and wires the codec, service and access repository. The two owner configurations expose only their public reader types. Controller uses ObjectProvider for service/codec and existing problem handling, not repositories/DSL. BusinessNumberPage has a nested seven-field item DTO; query service uses nested records rather than extra public classes. Reuse existing exception codes, authenticator and problem writer without editing them. No new library, generated OpenAPI server, cryptography abstraction, cache or session implementation.

The exact `CustomerPersistenceConfiguration` bean-method map is the six existing methods, unchanged, plus exactly four new methods below (ten declared methods total). Every method has `@Bean` and `@Lazy`, none has `@Primary`; class remains `@Profile("web")`. Enable `CustomerBusinessNumberCursorProperties` alongside the existing persistence properties. Parameter order and qualifiers are literal:

| New method / bean name | Parameters in declaration order | `@Qualifier` values in the same order |
| --- | --- | --- |
| `customerBusinessNumberTransactionTemplate` | `DataSourceTransactionManager manager` | `customerQueryTransactionManager` |
| `customerBusinessNumberAccessRepository` | `DSLContext context` | `customerQueryDsl` |
| `customerBusinessNumberQueryService` | `TransactionTemplate template`, `DSLContext context`, `JooqCustomerBusinessNumberAccessRepository repository`, `CustomerBusinessNumberReader reader`, `CustomerActivationStatusReader activationReader` | `customerBusinessNumberTransactionTemplate`, `customerQueryDsl`, `customerBusinessNumberAccessRepository`, `customerBusinessNumberReader`, `customerActivationStatusReader` |
| `customerBusinessNumberCursorCodec` | `CustomerBusinessNumberCursorProperties properties` | no qualifier: the existing reflection rule skips Properties parameters |

The access-repository bean returns its concrete customer-owned type; the service returns `CustomerBusinessNumberQueryService`, codec returns `CustomerBusinessNumberCursorCodec`. Owner configurations name their lazy public-reader beans `customerBusinessNumberReader` and `customerActivationStatusReader`, each taking only `@Qualifier("customerQueryDsl") DSLContext`; no customer internal imports. Controller uses qualified ObjectProviders `customerBusinessNumberQueryService` and `customerBusinessNumberCursorCodec`, never eager resolution before parsing. Structural cursor validation is a side-effect-free static codec operation, so no extra bean method/helper file is needed. No extra declared helper method is added to `CustomerPersistenceConfiguration`.

Update `CustomerWebConfigurationTests.customerAndSharingGraphsUseExactQualifiers` within its existing method: preserve all six existing map entries, add exactly these four ordered qualifier lists (empty list for codec), and retain exact declared-name set equality, every non-Properties parameter qualifier assertion, all-method `@Lazy`, no-`@Primary`, profile and sharing-graph checks. Keep its six test methods unchanged; do not bypass unknown methods or weaken equality. The separate template remains readOnly/REQUIRES_NEW/REPEATABLE_READ; old template remains READ_COMMITTED.

### 7.2 Database, tests and CI

```text
N backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql
M backend/ops/postgresql/pa25-runtime-identities.sql
M backend/pom.xml
M .github/workflows/ci.yml
N backend/src/test/java/com/onecall/archtests/CustomerBusinessNumbersWebTests.java
N backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java
N backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorCodecTests.java
N backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java
N backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java
N backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java
N backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java
M backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java
M backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java
M backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java
M backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java
M backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java
M backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java
M backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java
M backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java
M backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java
M backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java
M backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java
M backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java
M backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java
M backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java
```

### 7.3 Documentation

```text
M backend/README.md
M docs/14-production-data-model-and-flyway-schema-spec.md
M docs/17-production-security-design-spec.md
M docs/operations/pa25-production-database-identity-provisioning-runbook.md
N docs/gateflow/pa27-customer-business-number-read-foundation-s1-implementation.md
```

Document current V015/customer26/global261 boundary; mark V014/customer12/global247 as historical, not erased. Document required 15-row history, cursor key variable/custodian/absence/rotation behavior, active-context versus empty result, all four roles, all statuses/missing activation, pagination and exact test totals. Preserve prior work-unit evidence and production NOT EXECUTED/NOT CLAIMED statements. The implementation report records commands/results and residuals. This plan and Control are not implementation-edit targets.

Bounded docs/14 and docs/17 exception: in each, preserve the PA26 V014 section as historical evidence, relabel its heading `PA26 Historical Runtime Boundary (V014; superseded by PA27 V015)` and add a historical-only notice covering its capability rows, history prerequisites and inventory claims. Add a preceding `PA27 Current Runtime Boundary (V015)` section explicitly superseding that section and any older current customer-capability rows, without rewriting the structural/business/security model. The current section must state customer **26 = 4 schema + 22 column**, evidence **16** unchanged, seven-role global **261**, other-role **219** unchanged, and V015 delta **14 = 2 schema + 12 column**, listing the exact owner-issued/no-grant-option additions in §6.1 plus retained V014 reads. State V001-V014 unchanged, required fifteen-row history before current PA25 apply/check (V014-only rejected), unchanged six-login/seven-role/four-edge graph, no table SELECT/business writes/privileges outside the delta, current inventory §9 and no production acceptance claim. README, runbook and both new current sections must agree; 12/247 and fourteen-row prerequisites may remain only as explicitly historical claims. All unrelated docs/14/docs/17 content and other frozen architecture/API documents remain unchanged; OpenAPI and V001-V014 remain byte-frozen.

## 8. Exact test manifest and assertions

All additions are ordinary, uniquely named `@Test void` methods. Use loops/table data **inside** each method, no parameterized/repeated/factory/dynamic/conditional/disabled tests. No new test annotation in comments. Fixture helper has zero test methods. Do not rename or remove existing methods to satisfy counts. The following fenced blocks are the exact addition manifest.

### 8.1 CustomerBusinessNumbersWebTests: 12

```text
com.onecall.archtests.CustomerBusinessNumbersWebTests
exactGetRouteReturnsOnlyFrozenPageAndSevenItemFields
missingInvalidDuplicateAndWrongAudienceBearerAreUnauthenticated
unavailableVerifierAndMissingOrMalformedWorkspaceAreUnavailable
personalAndOrganizationSelectorsComeOnlyFromVerifiedPrincipal
absentAndValidLimitsUseDefaultAndInclusiveBounds
invalidAndDuplicateLimitsAreValidationFailed
malformedDuplicateAndTamperedCursorsAreCursorInvalid
crossAccountPersonalOrganizationAndCrossOrganizationCursorsAreInvalid
authenticationWorkspaceLimitCursorAndLifecycleErrorPrecedenceIsExact
serviceConfigurationAndUnexpectedFailuresUseSanitizedProblems
nonGetAndNonCanonicalRoutesStayNotFoundWithoutNewSurface
successAndProblemsNeverLeakSecretsAndRetainNoStoreHeaders
```

Exercise real servlet boundary/authenticator with test-only verifiers, not only direct controller calls. Exact JSON property-name equality includes `nextCursor: null` and forbids every noncontract field. Inject query/header/body selectors and prove unchanged authority. Limit cases: absent, 1, 20, 100, 001, empty, 0, 101, -1, +1, whitespace, 1.0, 1e1, non-ASCII digits, overflow and duplicate identical/different values. Cursor cases include repeated identical values. Combine bad bearer+bad limit/cursor, missing workspace+bad query, invalid limit+invalid cursor/key, structural invalid cursor+missing key, missing/invalid key+inactive context (absent and structurally valid cursors), and valid key+bad MAC/scope+inactive context; assert §3 literally, including role-assumption/assertion 503 and projection 500; assert precedence and that downstream dependencies are not invoked. Test missing/duplicate verifier, missing bean/key and projection faults without exposing injected sentinel bearer/DSN/password/SQL/ciphertext/HMAC/key/cursor strings. Ordinary raw request-body selectors on GET are not an authority channel.

### 8.2 CustomerBusinessNumberQueryServiceTests: 8

```text
com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryServiceTests
defaultBoundsAndLookaheadRequestAreExact
trustedWorkspaceAndCurrentLifecycleAreRequiredBeforeReaders
allBindingActivationAndRealNameStatesComposeWithoutFiltering
missingActivationDefaultsToInactiveAndForeignProjectionFailsClosed
lookaheadUsesLastReturnedRowAndNullCursorOnTail
emptySingleAndMultiPageProjectionIsExact
technicalAndProjectionFailuresReturnNoPartialOrSensitivePage
webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact
```

Use reader/access doubles for composition only; PostgreSQL tests prove the transaction. Assert map input is unique and bounded, no N+1, missing activation only defaults, invalid/foreign/duplicate returned activation and bad row/enums/mask fail. Validate lazy default/web boot unaffected by missing endpoint key and unrelated endpoints unaffected. Check the separate transaction template's REQUIRES_NEW/readOnly/REPEATABLE_READ and unchanged existing READ_COMMITTED template; assert qualified wiring and prepared/nonlogging settings.

### 8.3 CustomerBusinessNumberCursorCodecTests: 8

```text
com.onecall.core.customer_iam.internal.web.CustomerBusinessNumberCursorCodecTests
personalAndOrganizationRoundTripsAreCanonicalAndDeterministic
payloadVersionLengthDomainAndMicrosecondAnchorAreExact
invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected
truncatedExtendedUnknownVersionAndInvalidTimestampAreRejected
everyPayloadOrMacByteMutationIsRejected
accountWorkspaceKindAndOrganizationReplayAreRejected
missingMalformedAndWrongLengthKeysAreUnavailableWithoutDisclosure
keyReplacementInvalidatesOldCursorAndLimitDoesNotBindScope
```

Assert exact 93-byte/124-character format, domain separators, final LF and scope digest, deterministic fixed-key vectors, timestamp boundaries/overflow and unsigned UUID representation. Recompute test MACs for structural-invalid payloads so validation is independently exercised, not masked by a bad MAC. Byte-mutation loop covers all 93 bytes; wrong-key/scope must fail with the same public cursor-invalid result. Do not use production key material or publish key values in logs. Very long input rejects before expensive parsing; codec failure exceptions are generic.

### 8.4 CustomerBusinessNumberBoundaryTests: 4

```text
com.onecall.archtests.architecture.CustomerBusinessNumberBoundaryTests
subjectAndActivationExposeOnlyTheTwoOwnedNamedQueryInterfaces
customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation
controllerSqlAndProjectionStayInsideTheirSemanticOwners
v015AndPa25DeltasAreReadOnlyAndFrozenInputsRemainByteIdentical
```

Use Modulith verification and explicit source/API shape assertions: reader public methods/nested records exact, no public jOOQ/connection/customer internal type; implementation packages not exported; no customer import of either foreign internal implementation, owner-role string or SQL for subject/activation tables. In existing `CustomerApiBoundaryTests.customerDependenciesAndReadOnlySqlStayInsideBoundary`, change the internal regular-source-file count **26 -> 32**: six additions = four web + one application + one persistence (post-state 16 web + 6 application + 5 persistence + 5 security). Preserve the full banned-string scan and its existing narrow sharing exceptions: no `ThreadLocal`, `getSession(`, `RequestContextHolder`, `Redis`, `SELECT *`, `FOR UPDATE`, `FOR SHARE`, `append_fact`, forbidden framework/module imports, or new `com.onecall.core.sharing` exemption. In existing `customerApiSignaturesAndProjectionAreExact`, assert `CustomerBusinessNumbersController.listCustomerBusinessNumbers(Principal, HttpServletRequest)` returns `BusinessNumberPage`, exact GET path `/api/v1/customer/business-numbers`, JSON media type, exact page/item fields and qualified service/codec ObjectProviders. In existing `profileAndVerifierArtifactsCannotEscapeIntoProduction`, include the new controller in the `@Profile("web")`/servlet-conditional source assertions; retain production verifier-bypass checks. Verify lazy wiring via the existing configuration method and new tests. No added `@Test` method: all three existing boundary methods remain. Update existing ModuleBoundaryTests expected six named dependencies/four discovered production edges, retain negative cycle/internal-access tests and verify new named-interface presence/forbidden-internal access. No module opening to get a green build. Assert V015 forbids writes/business DDL/functions/grant option and frozen SHA checks as described in section 10.

### 8.5 CustomerBusinessNumbersIT: 18

```text
com.onecall.archtests.container.CustomerBusinessNumbersIT
postgres16FullChainAndCustomerRuntimeRoleAreUsed
personalScopeReturnsOnlyEveryOwnedBinding
organizationScopeIncludesEveryActiveRoleAndOnlyOrganizationOwnedSubjects
allBindingActivationMissingActivationAndRealNameCombinationsAreVisible
inactiveAndMissingAccountsAreForbiddenWithoutPageReads
inactiveMissingAndForeignOrganizationsOrMembershipsAreForbidden
unownedForeignAndOtherSelectedWorkspaceSubjectsNeverAppear
subjectCreatedAtDescendingAndNativeUuidAscendingTiesAreExact
emptySingleAndExactLimitPagesHaveExactTailSemantics
firstMiddleAndLastPagesEnumerateEveryBindingOnce
defaultOneAndHundredLimitsAndChangedContinuationLimitAreExact
malformedTamperedAndCrossScopeCursorsCannotWidenResults
committedLifecycleChangesAreRecheckedForEveryPage
lifecyclePageAndActivationShareOneRepeatableReadSnapshot
concurrentInsertAndDeletedAnchorFollowDocumentedKeysetSemantics
successDenialAndFailurePerformZeroBusinessWrites
roleIsolationReadOnlyAndAutoCommitRestoreOnPooledConnectionReuse
responsesAndCapturedLogsNeverContainSecretOrOwnershipSentinels
```

Use PostgreSQL 16, current full chain, PA25 apply/check, LC NOINHERIT and `SET LOCAL customer_iam_role`; seed through correct owner roles, not runtime writes. Reuse/composition of CustomerMeFixture is permitted; new helper owns legal subject/binding/activation seeds and explicit test key, no production verifier. Assertions must traverse real HTTP for scope/errors/projection and the real service for coordinated transaction checks.

Visibility Cartesian fixture: for each scope, 3 binding statuses x 4 activation variants (missing, INACTIVE, ACTIVE, SUSPENDED) x 2 real-name states x 2 number types = 48 visible combinations, checked literally. Organization role loop runs all four ACTIVE roles, not only one representative. Use exactly two ACTIVE accounts A (reader) and B (other member), and five initially ACTIVE organizations O_ADMIN, O_MEMBER, O_EVIDENCE, O_AUDIT and O_FOREIGN. Seed exactly one ACTIVE membership for A in each of the first four, with roles ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER and AUDITOR respectively; B has MEMBER in those four and O_FOREIGN, A has no O_FOREIGN membership. This obeys `uq_membership_live(organization_id,account_id) WHERE status <> 'REVOKED'`. Run the literal organization visibility assertions with A selecting each of the four role organizations, each seeded with its own 48 visible combinations; never insert four live roles for one pair. Reset fixtures or update the single existing membership between inactive-case iterations, not duplicate live memberships. Preserve A/B personal scopes and selected-workspace isolation: each role organization must exclude the other three and O_FOREIGN, and personal selection excludes all organizations. Seed another member's organization-owned subject, several bindings on one subject, unowned subjects, personal-owned subjects while org selected, org-owned subjects while personal selected and foreign accounts/orgs. Ensure all foreign masks/IDs have distinctive sentinels and never appear. Literal V002 inactive cases are account DISABLED; organization SUSPENDED and DISABLED; membership SUSPENDED and REVOKED, each independently and across all four roles, plus missing account/organization/membership. Also test an ACTIVE membership for a different account or different organization. Do not invent invalid INACTIVE/REMOVED lifecycle values. Bindings/subjects cannot reference missing parent rows through valid FKs; use missing trusted context IDs rather than disabling constraints to construct those tests.

Ordering must include different subjects with identical created_at, multiple bindings on one subject, microsecond timestamps and UUIDs straddling high-bit boundaries (e.g. 7fffffff... and 80000000...). Expect database UUID order, not Java compareTo. Use enough rows for 20-default and 100-bound lookahead. Page shapes cover zero/one/exact-limit/limit+1, first/middle/last and cursor after final anchor returning empty. Walk fixed fixtures with a uniqueness set and expected ordered IDs; never assert only counts. Confirm lookahead row is next page's first row and cursor anchors the previous page's last returned row.

Lifecycle concurrency test uses latches/barriers, not sleeps: commit disable/removal before a new page transaction and assert generic 403; pause after lifecycle SELECT in one transaction, commit a lifecycle/activation change on another connection, then prove page/projection remains the first snapshot and the next request sees the change. Verify same physical connection/backend PID and transaction identity for all readers, transaction_read_only on and transaction_isolation repeatable read. Concurrent insert/deleted-anchor test proves documented cross-request behavior without promising snapshot continuation.

Zero-write test snapshots all application tables including audit/outbox/idempotency/claims/consents/activations/risk and sequence state after seed, then compares after success, denial, malformed request and injected dependency exception; no audit event is written just for listing. Connection reuse test has maximumPoolSize=1; assert LC current_user/session_user and restored defaults after commit/rollback; issue subsequent valid reads after each failure. No mock establishes a PostgreSQL permission or restoration claim.

### 8.6 CustomerBusinessNumberReadPrivilegeIT: 8

```text
com.onecall.archtests.container.CustomerBusinessNumberReadPrivilegeIT
v014CustomerCannotReadSubjectBindingOrActivationWith42501
v015FreshAndUpgradeHaveExactFifteenHistoryEntriesAndFrozenBytes
v015AddsExactlyFourteenOwnerGrantedTuplesAndNothingElse
customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501
v015ReplayPreservesAclsGraphCredentialsDefinitionsAndBusinessData
v015RejectsPartialUnexpectedGrantorGrantOptionAndAuthorityDriftAtomically
pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle
allSixLoginBoundariesAndRolePoolRestorationRemainUnchanged
```

Migration fixture paths: clean V001->V015; existing V001->V014 without PA25 external slots -> V015; V014 with legally provisioned frozen graph/non-null test verifiers -> V015. Seed marked graph using owner/admin fixture logic already used by PA26, not the now-current PA25 executable on an obsolete database. Before upgrade assert exact 42501 for subject/binding/activation attempts; after upgrade assert exactly the table in section 6 and unchanged 219 other-role/16 evidence tuples, owners, function/trigger/index definitions, PUBLIC/default ACLs, role OIDs/attributes/verifiers/markers/edges, application data and sequence state. Never print credential snapshots.

Negative capability oracle enumerates every non-dropped column in all application relations: approved customer columns SELECT successfully; every unapproved column is probed separately and must fail SQLSTATE **42501**, not 42P01/42703, read-only 25006, trigger P0001 or a generic exception. Include full table SELECT/SELECT *, sensitive number ciphertext/HMAC, claims/risk/consents, auth_identity/identity_profile, audit/platform, unrelated schema reads, schema CREATE, application routines, sequences, INSERT, per-column UPDATE, DELETE/TRUNCATE, REFERENCES/TRIGGER, SELECT FOR UPDATE/FOR SHARE and unauthorized SET ROLE. Run denial probes in writable test transactions with savepoints so readOnly cannot mask missing ACL protection; choose otherwise valid SQL/fixture references. REFERENCES/TRIGGER probes use disposable administrator-created supporting objects and known existing routines so missing prerequisites cannot cause a false denial. No-op DML still requires its privilege and must get 42501. Positive approved reads must work outside application readOnly as an independent ACL proof. Separately prove no grant option and no ACL expansion from attempted re-grants through catalog equality; PostgreSQL may emit a no-privileges-granted warning instead of an error for GRANT, so do not falsely require SQLSTATE 42501 for that distinct command behavior.

V015 drift cases include partial delta; full table grant; unapproved column; grant option; wrong grantor via disposable administrative catalog fixture; PUBLIC/default/inherited widening; owner mismatch; runtime invocation; malformed/partial provenance graph or changed membership options. Assert unchanged complete before/after snapshots on every rejection, no partial grants. PA25 cases independently delete/change every new tuple or add forbidden tuple, tamper rank15/history checksum/script/type/description/version/success, and verify both apply/check fail closed without repair. Current PA25 must reject a pure V014 database. Existing PA25/PA26 credential/log sanitization tests remain green.

## 9. Measured baseline, explicit addition budget and CI changes

Planning measurements performed against this snapshot, using CI's method regex `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`, annotation line regex `@Test\b`, POM include parsing and static evaluation of CI's literal inventory construction:

| Inventory | Classes | Methods |
| --- | --- | --- |
| Baseline Surefire source = CI | 36 | 309 |
| Baseline Failsafe source = CI | 15 | 246 |
| Baseline total | 51 | 555 |
| Exact new Surefire manifest, sections 8.1-8.4 | 4 | 12+8+8+4 = 32 |
| Exact new Failsafe manifest, sections 8.5-8.6 | 2 | 18+8 = 26 |
| Required post-S1 Surefire source = CI = XML | 40 | 341 |
| Required post-S1 Failsafe source = CI = XML | 17 | 272 |
| Required post-S1 total | 57 | 613 |

Baseline annotation-line scan is **556**, with exactly one historical comment hit in PostgresContainerIT; required post-S1 scan is **614** (556+58). Baseline Failsafe includes **15**; required post-S1 includes **17**, exactly the existing fifteen plus `**/CustomerBusinessNumbersIT.java` and `**/CustomerBusinessNumberReadPrivilegeIT.java`. New helper has no annotations and is not an include. Existing changed classes retain names/methods/counts, specifically CustomerWebConfigurationTests 6, EvidenceMediaReadIT 8, ModuleBoundaryTests 9, CustomerApiBoundaryTests 3, PA26 12, PA25 20, PostgresContainerIT 64.

These post-S1 values are **computed acceptance targets from the exact named manifest**, not fabricated observations of source or XML that does not exist yet. Planning measures the baseline and the plan's addition list; implementation must freshly measure post-change source, CI, scan/includes and actual XML. An actual post-change execution count cannot truthfully be supplied before code generation. No green XML/build/container claim is made by this plan.

CI edits must preserve exact name-set equality, duplicate detection, zero skipped/failures/errors, class discovery and per-class XML identity. Construct the six-class `pa27_additions` mapping from §§8.1-8.6 after the existing PA26 additions are installed and before `source_classes` discovery/equality. Union exactly four class/method sets (CustomerBusinessNumbersWebTests 12, CustomerBusinessNumberQueryServiceTests 8, CustomerBusinessNumberCursorCodecTests 8, CustomerBusinessNumberBoundaryTests 4) into `unit_expected` and exactly two (CustomerBusinessNumbersIT 18, CustomerBusinessNumberReadPrivilegeIT 8) into `container_expected`, using fully qualified manifest names and set unions. Then run the existing exact source discovery and `source_classes == set(unit_expected) | set(container_expected)` check, followed by historical source/method/annotation checks and final XML `exact_inventory` calls; retain the existing later historical-addition bookkeeping and every equality. Do not place all six classes into either runner or perform source equality before the unions. Update the early Surefire aggregate assertion from 309 to 341, not only final totals. Add pa27 names to historical-baseline subtraction at every prior epoch and preserve all historical checkpoints through PA26; capture PA27-start as [309,246], scan556. Final method totals [341,272], total613, scan614, classes40/17 and include17. Final `exact_inventory` calls use 341/272. Existing per-class counts stay unchanged. Update summary text to PA27 and retain historical timeline. Do not bypass assertions or count @TestConfiguration as tests. No existing annotation/comment is removed to force the scan.

## 10. Frozen bytes and validation commands

### 10.1 Baseline raw SHA256

The snapshot contains CRLF in some frozen inputs; preserve **raw bytes**, not just LF-normalized checksums. Existing Java frozen checks may intentionally hash logical LF text; retain that policy and additionally compare raw pre/post manifests. The raw snapshot hashes are:

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6  backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql
4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505  backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql
4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7  backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql
8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030  backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql
6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065  backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql
7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d  backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql
bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81  backend/src/main/resources/db/migration/V007__activation_and_consent.sql
32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde  backend/src/main/resources/db/migration/V008__evidence_media.sql
3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1  backend/src/main/resources/db/migration/V009__sharing.sql
d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559  backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql
ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e  backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql
ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6  backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql
1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e  backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql
bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8  backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql
```

Control raw SHA256 at original planning: `3f530bfe6bf11668cbfbcb6ea346d3196170f2d6f434a1e9bb601ca4ca9aeaa1`; at plan-fix-1 intake after Controller adjudication: `ee1657f1e286d076ed3f6ce754120d6562d0551dd75424c8623287d310e7da7b`. These are time-specific provenance, not interchangeable frozen baselines. S1 must capture the actual implementation-intake Control hash in its manifest and report the collection point separately. Control is never an edit target. Metadata-free snapshot cannot prove branch/PR/merge claims; accept Control's merge context as authority, do not fabricate Git verification.

### 10.2 Exact future implementation validation

Prerequisites: baseline-compatible Temurin Java 21.0.13, Maven wrapper 3.9.9, Docker daemon/Testcontainers with PostgreSQL `postgres:16`, Node/npm only for frozen API validation. No production credentials, live database or network calls to business services. Execute from snapshot/repository root unless the subshell changes directory. Do not run Maven or containers as part of this planning-only task.

Before source edits, make a protected baseline manifest outside tracked paths, and save the sixteen hashes (OpenAPI + fourteen migrations + Control):

```bash
sha256sum api/openapi/onecall-v1.openapi.yaml \
  backend/src/main/resources/db/migration/V0{01,02,03,04,05,06,07,08,09,10,11,12,13,14}*.sql \
  docs/gateflow/project-execution-control.md > /tmp/pa27-frozen.sha256
java -version
(cd backend && ./mvnw -B -ntp -v)
docker info
```

Focused source/web/module validation:

```bash
(cd backend && ./mvnw -B -ntp \
  -Dtest=CustomerBusinessNumbersWebTests,CustomerBusinessNumberQueryServiceTests,CustomerBusinessNumberCursorCodecTests,CustomerBusinessNumberBoundaryTests,ModuleBoundaryTests,CustomerApiBoundaryTests test)
(cd backend && ./mvnw -B -ntp -Pcontainer-smoke \
  -Dit.test=CustomerBusinessNumbersIT,CustomerBusinessNumberReadPrivilegeIT,ProductionDatabaseIdentityProvisioningIT,DatabaseRuntimeAclRbacHardeningIT,PostgresContainerIT verify)
```

The focused integration command may also execute the full Surefire suite; do not claim its partial Failsafe reports satisfy full inventory. Then remove stale reports with clean and run the complete acceptance build:

```bash
(cd backend && ./mvnw -B -ntp clean -Pcontainer-smoke verify)
sha256sum -c /tmp/pa27-frozen.sha256
```

Run the exact updated CI inventory Python, not a looser home-grown XML aggregate. This command executes the existing literal `PY` block from `assert container smoke actually ran` with backend as working directory:

```bash
python3 - <<'PY'
from pathlib import Path
import os
import textwrap
workflow = Path('.github/workflows/ci.yml').read_text(encoding='utf-8')
section = workflow.split('- name: assert container smoke actually ran', 1)[1]
body = section.split("python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0]
os.chdir('backend')
exec(compile(textwrap.dedent(body), 'ci-exact-inventory', 'exec'))
PY
```

Additionally run CI's existing `default and opt-in web jar smokes` block unmodified through the normal CI job: default boot has no web/database activation, opt-in web boot remains lazy without cursor key or DB credentials, unavailable authentication remains sanitized. Full GitHub CI must pass existing API/other jobs; do not introduce a replacement test job that omits them. The existing API CI command is the following (Node >=22, existing locked dependencies); it may generate only the existing disposable `tmp/openapi-client` validation outputs, never modify frozen contracts, committed generated code or lockfiles:

```bash
(cd api/openapi && npm ci && npm run check:s3)
```

Implementation checks also measure all source method/class names with the same CI regex, annotation-line scan614, POM include-name equality17, and XML zero skips/errors/failures. Generate V015 Flyway checksum only after finalized SQL, synchronize PA25 literal history, rerun relevant and full tests, and record raw SHA256 for V015 plus original frozen manifest. A changed checksum requires retesting; never add a Flyway repair or ignored migration.

### 10.3 Planning artifact validation and limitations

This task performs only read-only evidence gathering, source/CI inventory comparison, plan-format/manifest arithmetic checks and whole-snapshot byte comparison outside the two authorized planning artifacts. It does not run or claim Maven, containers, actual XML, API validation, CI jobs or live acceptance. Planning artifact must decode strict UTF-8, contain no CR, end in exactly one LF and have no trailing whitespace. Hash/line/byte metadata is reported externally after writing, not embedded as a self-referential hash. Recheck that all pre-existing files outside the two authorized planning artifacts are byte-identical and no other new file exists.

## 11. Documentation decisions, risks and residual destinations

| Item | Decision / remaining risk | Owner and destination |
| --- | --- | --- |
| PA27-PLAN-01 | Closed by user-confirmed literal matrix; no speculative active-only or role policy | Subject-number/Activation-consent/Security; independent plan review checks literal tests |
| Cursor authenticity | Endpoint-only HMAC, required lazy external key is an accepted mandatory deployment availability precondition, even for first/empty pages; replacement invalidates cursors; no expiry promise | Security/Operations -> deployment handoff, distribution/rotation/pool rollout evidence outside S1 |
| Cross-module DSL wiring | Current bounded qualified-bean inversion is accepted with real runtime-wiring IT; no additional Java module edge | Backend/Architecture -> deferred after PA27; current S1 must prove same connection/snapshot |
| Lifecycle concurrency | One read-only repeatable-read snapshot; next request observes later revocation | Backend/Security -> S1 concurrency tests; stronger linearizability is not promised |
| Cross-row DB access | Column grants are not RLS; LC compromise can read approved columns across rows; application owns tenant filtering | Security/Database -> least-privilege review, approved SQL and isolation tests; RLS redesign outside PA27 |
| Migration/check authority | Exact 26/261 tuple oracle; V014 remains historical; no automatic repair | Database -> V015/PA25 acceptance and protected administrative execution |
| Frozen bytes versus logical checksums | Raw snapshot hashes and Java/Flyway logical checks differ because line endings differ | Backend/CI -> raw comparison plus unchanged logical predecessor checksum assertions |
| Performance | Existing owner indexes and bounded limit+1/batch avoid N+1; join/sort may still scan many owned rows | Backend/Database -> disposable representative EXPLAIN inspection if needed; index/schema change returns to Controller, not silently added |
| Test counts | Baseline measured, addition budget exact, actual post-S1 XML unavailable until implementation | CI -> freshly measured 40/341 + 17/272, scan614/includes17 at S1 acceptance |
| Existing production residuals | Q17/Q23, migration/comment custody, credential assignment/TLS/pg_hba, pool drain/old-session invalidation stay unresolved here | Platform/Database/Security/Operations -> existing PA25 runbook; production NOT EXECUTED/NOT CLAIMED |
| Snapshot metadata | No Git metadata or remote verification; no staging/commit/PR actions | Controller -> later managed-worktree process, never this planning run |

No blocking product or implementation design question remains. Final V015 checksum is a deterministic implementation output, not an unresolved policy choice. Missing build/container prerequisites must be reported as validation blockers, not worked around by disabling tests. Any newly discovered frozen-contract conflict, required extra privilege/path, semantic change or inventory deviation goes to Controller before expanding scope.

## 12. Completion report and no-goal-drift rationale

S1 implementation report at the allowlisted path must state: accepted goal and one-slice result; exact modified/new files; verified response/matrix/errors/cursor; transaction/role/pool proof; old12/new26/delta14/global261/evidence16 and unchanged graph; V015 checksum/hash and PA25 apply/check results; raw OpenAPI/V001-V014 comparison; exact source=CI=XML class/method sets and totals/scan/includes; commands/exit results including full PostgreSQL16/CI; docs decisions; unresolved deployment/security owners; no live production acceptance claim. Distinguish actually executed from not executed. Do not claim completion based solely on tests existing or expected arithmetic.

Planning completion reports this artifact path, prior and new metadata hashes, actual validation performed, protected-byte result and residual destinations, then **stop before re-review**. No implementation, plan approval, merge, mark-ready, reviewer request, external comment, stage, commit, push or PR mutation follows.

No goal drift: only the frozen business-number GET and the exact privileges/seams/tests needed to execute it. No create/bind/unbind/claim/identity-verification/risk/activation-consent mutation, session issuer/TTL/WeChat mapping, trial/catalog/order/sharing/evidence/operations API, full-number encryption/HMAC facility, frontend, deployment or credential assignment. Number HMAC is never queried; the cursor MAC serves only pagination authenticity.

No over-design: one slice, two narrow owner APIs, one bounded read composition transaction, one small cursor codec and one privilege-only migration. No generic RBAC/pagination registry, repository framework, new datasource/role/membership, RLS layer, SECURITY DEFINER machinery, schema/index/data-model change, key-management service or future capability reservation. These choices implement the confirmed goal at the existing semantic and runtime boundaries rather than redesigning adjacent foundations.
