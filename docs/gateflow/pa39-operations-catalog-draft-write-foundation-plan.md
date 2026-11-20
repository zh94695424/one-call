# PA39 Operations Catalog Draft Write Foundation — Implementation And Review Plan

Task: `pa39-operations-catalog-draft-write-foundation-plan-gpt-20261120-01`.
Date: 2026-11-20. Gate: plan only; next gate: **independent peer-isolated plan review**.
Working directory: `/mnt/c/Users/Administrator/.codex/worktrees/pa39-next-work-unit/codex-call`.
This document specifies future implementation; it does not claim product implementation, test execution, acceptance or production readiness.

## 1. Binding Scope And Evidence

Read `docs/gateflow/project-execution-control.md` first. Its top PA39 Goal Confirmation, not an older plan, is the scope contract. Implement exactly:

- `POST /api/v1/operations/catalog/versions` / `createOperationsCatalogVersionDraft`.
- `PUT /api/v1/operations/catalog/versions/{versionId}/draft` / `replaceOperationsCatalogVersionDraft`.

Both require opaque employee-session authentication and current server-derived ACTIVE ADMIN authority. Create an existing product's never-published draft, replay an equal natural identity, or conflict. Replace only mutable commercial content with revision-conditional application or proven applied replay. Preserve immutable benefit/channel facts, canonical snapshots/hashes, atomic audit, and sanitized shipped errors. No Idempotency-Key requirement or platform idempotency record.

No product creation, deletion, trial policy, independent offer withdrawal/channel administration, publication/offsale rewrite, customer/order/payment/entitlement flow, frontend, cache, approval engine or generic CRUD. No new database object, role, grant, migration, public field or state. V001–V018, OpenAPI and docs12/14/15/17 are frozen. Control is Controller-owned and must not be edited. Discovery of a required schema/ACL/public-contract change is a stop for renewed goal confirmation, not permission to add V019.

Evidence inspected directly:

| Source | Decision supported |
| --- | --- |
| Control, PA39 top section | Exact scope, ADMIN, replay, atomic audit, no V019, one-slice presumption |
| docs12 §§1, 6, 10, 13 | Modular monolith, owner writes, one PostgreSQL transaction, explicit boundaries and tests |
| OpenAPI routes at 2326/2406; schemas at 5274/6031/6048/6128/6184; conflict response at 4151 | Exact commands, required/null fields, enum/array shapes and response/status surface |
| docs14 §§4.16–4.19, catalog lifecycle and §7.3 owner transaction contracts | Product/version identity, draft commercial mutability, immutable children and snapshot ownership |
| docs15 operations inventory, exact error sets and draft descriptions | No extra operation/errors; conditional natural-key and revision behavior |
| docs17 audit registry/high-risk map near 630 | EMPLOYEE/ADMIN, ACTION_DERIVED, CATALOG_DRAFT_CREATE/REPLACE, atomicity |
| V004 lines 52–157, 281–329, 380–432 | Natural uniqueness, checks/defaults/indexes, triggers and actual owner ACL |
| V010–V018 and PA25 provisioning script | Effective ACL preservation and runtime-login topology, not migration-only assumptions |
| PA37 product read plan and current reader/web/configuration/tests | Current product/benefit read shape, shared pool, zero children allowed, no invented minItems |
| PA38 plan, code-review fix3, aggregate fix1 and shipped lifecycle code/tests | Exact role/witness assertions, filter ordering, bounded bodies, recovery pitfalls, manifest carrier and LF validation |
| POM, CI, architecture guards and PostgreSQL fixtures | Complete inventory, named interfaces, exact runtime role tests and acceptance commands |

### 1.1 Independently Measured Git Identity

`.git` says `gitdir: D:/codex-call/.git/worktrees/codex-call5`. Its mapped directory `/mnt/d/codex-call/.git/worktrees/codex-call5` exists, `gitdir` points back to this exact Windows worktree, and `commondir` is `../..`. Only after verifying those facts were read-only commands used with explicit GIT_DIR/GIT_WORK_TREE; no metadata was edited.

- Branch: `codex/pa39-next-work-unit`.
- Current HEAD: `4ce8abd270e57171b72fcc54deb8c9176d8405c1` (`gateflow: confirm PA39 catalog draft write goal`).
- Current HEAD tree: `77b34ff8695841c4714674326aec804d11d9216f`.
- Parent: merged PA38 main `55a225c22c87eea82e33e3f6a08b1fe471ced9ee`; its reviewed PA38 parent is `cc1bbbf8e5d1c9c5917bddabdcb810644a6f2053`.
- Initial tracked worktree/index changes: none. Initial untracked private entries: `.codex`, `.pa39-plan-prompt.md`, `.pa39-plan-run.sh`; preserved, not opened or changed. Thus the historical start-at-55a225c statement is not the current HEAD.
- This planning gate adds only this plan. Implementation must re-record its dispatched commit/tree and dirty-file allowlist, not assume this document's future commit identity.

Read-only metadata command pattern, from the worktree root:

```bash
GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call5 GIT_WORK_TREE="$PWD" \
  git --no-optional-locks rev-parse HEAD HEAD^{tree}
```

### 1.2 Measured Baseline, Not Historical Prompt Arithmetic

| Inventory | Current measured value |
| --- | --- |
| Production Java / physical source lines | 290 / 15,328 |
| Test Java / physical source lines | 180 / 68,907 |
| Production resources / migrations | 21 / 18, exactly V001–V018 |
| Surefire classes / distinct method identities | 97 / 934 |
| Failsafe classes / distinct method identities | 38 / 630 |
| All executable classes / methods | 135 / 1,564 |
| Lines containing `@Test` / executable methods | 1,565 / 1,564 |
| Extra historical comment hit | Only PostgresContainerIT's `// evidence inside this existing method; no new @Test and no catalog weakening.` |
| Failsafe explicit POM includes | 38, set-equal to current container classes |
| Governed production manifest | 310 entries; SHA256 `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045` |
| Modulith modules / discovered module edges | 14 / 13; configured catalog allowed dependencies 5 |

The source regex is `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(`. Complete current identities are the source-extracted FQCN plus `#method`, sorted lexicographically, joined by LF plus one terminal LF: 1,564 entries, SHA256 `a6b67db97e2aa7b4f765de7e5d1320e9226302da4a3e63c22c50d93e761dff7c`. This fixes every baseline identity without copying 1,564 names into prose. The complete literal maps in `.github/workflows/ci.yml` (`unit_expected`, `container_expected`, all cumulative additions through PA38) were independently reconstructed and compared class-by-class to source: **97/934 and 38/630, zero identity differences**. This was a static comparison, not execution of the XML oracle.

Current XML totals are **not available**: this checkout has no `backend/target`. The source/CI/POM baseline above is measured; zero failures/errors/skips and XML equality are future acceptance expectations, not results inferred from old reports. Physical LOC after implementation cannot be honestly predicted before code exists; report measured final LOC then, separately from the exact planned file/method/annotation-line arithmetic below.

Manifest algorithm, exactly as `EvidenceRoleIvrAssociationAclBoundaryTests.v001ThroughV017AndPublicContractsRemainByteFrozen`: all regular files below `backend/src/main/java` and `backend/src/main/resources`, exclude only filename `V018__evidence_role_ivr_association_read_privileges.sql`; normalize CRLF to LF per file; lowercase SHA256; render `backend-relative/path + " " + digest`; sort lines; LF join with terminal LF; hash UTF-8 bytes. Current count is 290+21−1=310. Do not hash concatenated file bytes or retain a stale pin.

## 2. Sufficiency Proof: V001–V018, No V019

**Conclusion: sufficient.** This is a source/ACL proof, with required live PostgreSQL verification in §9; no current runtime execution is claimed.

1. V004 supplies `catalog_product` (immutable product), `catalog_version`, `catalog_offer`, and `catalog_benefit_line`. Application-generated UUIDs need no sequence grant. Version defaults are DRAFT, revision 1 and created_at now(); published_at is nullable and DRAFT iff published_at is null. Offer defaults are ACTIVE, revision 1 and created_at now(). There is no requirement to mark an offer WITHDRAWN merely because its parent is DRAFT; the version lifecycle gates sale.
2. `uq_catalog_version_no(product_id,version_no)` is the immediate natural uniqueness constraint. Version PK, `uq_catalog_offer_version_channel`, and `uq_catalog_benefit_version_type` separately enforce identity, one row/channel and one line/type. Existing product/version FKs and positive/check constraints suffice. Positive versionNo is not an API requirement to be greater than every historical version; do not add a max-version scan/rule.
3. `uq_catalog_version_active` is partial for ACTIVE and unaffected by DRAFT insertion; existing product/version, channel/status and benefit/version indexes support the bounded lookups. No new index is justified.
4. Catalog role gets table SELECT/INSERT on all seven V004 tables. Exact catalog-owned ACL inventory is **42 tuples**, including 14 table grants, 23 column UPDATE grants, four trigger-function EXECUTEs and schema USAGE. Version UPDATE columns are sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot, snapshot_sha256, published_at, revision. Draft replace needs only the seven commercial columns plus snapshot, snapshot_sha256 and revision. Offer UPDATE is only offer_status, version_token and revision; PA39 uses version_token and revision only. No benefit UPDATE/DELETE or version DELETE is granted.
5. Product trigger rejects UPDATE/DELETE; benefit trigger rejects UPDATE/DELETE even for a more privileged test role. Offer trigger prevents catalog_version_id/channel changes; runtime ACL also prevents ID/created_at/relation changes. Published-version trigger freezes commercial fields/snapshot/creator/timestamps once published. Draft immutability of product/version identity follows absent UPDATE grants plus the command SQL, not a nonexistent blanket DRAFT trigger. Child append-only means no new/removed/replaced child identities on replace; server token/revision regeneration is the expressly permitted offer-row exception.
6. V001 provides `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)`; V004 grants catalog_order_role schema USAGE and EXECUTE. Direct INSERT on audit.audit_fact remains forbidden. No platform idempotency or outbox function is needed for these commands. The draft contract declares audit actions, not a new publication event.
7. V010–V013 narrow sharing/read/admission/media roles, not the catalog owner mutation grants. V014 hardens runtime RBAC; V015–V017 add bounded cross-owner SELECTs to existing context roles and preserve the catalog-owned matrix; V018 changes evidence association SELECTs and verifies the prior matrix/topology. None removes these V004 catalog INSERT/column UPDATE or audit EXECUTE capabilities. Preserve the existing full 410-tuple provisioning oracle and its role membership count, not a newly invented ACL baseline.
8. The existing production PA25 `operations_ivr_read` login slot already has operations_iam_role plus the shared context targets including catalog_order_role, with ADMIN FALSE / INHERIT FALSE / SET TRUE. It is not a table owner or privileged fallback. The PA38 disposable fixture further strips IVR/evidence memberships and proves commands with only operations_iam_role/catalog_order_role. Reuse this production credential family and its shared pool; do not deploy fixture credentials or add another login.
9. `OperationsSubjectReadPersistenceConfiguration` already supplies a writable REQUIRES_NEW, READ_COMMITTED, timeout-5 transaction template over the existing shared datasource. PA38 already proves PostgreSQL role switching and same-physical-connection authority. No new pool, secret, transaction manager, property family or grant is needed.

Required independent reviewer probes: reconstruct the 42 tuples; verify later migration deltas do not accidentally revoke catalog writes; test INSERT/allowed UPDATE/audit function with a non-owner runtime login; prove forbidden identity/benefit writes, read-only denial and restoration. Any contrary result blocks rather than expands scope.

## 3. One Behaviorally Verifiable Slice And Exact Ownership

**S1 only: authenticated natural-key draft create + revision-conditional draft replace, canonical commercial projection, immutable children, owner transaction and atomic audit.** Create and replace cannot be independently accepted without their shared canonical equality and lifecycle substrate. Splitting by web/domain/repository would create artificial slices, not behavioral boundaries. This is the smallest justified slice within the Gateflow default; no prerequisite/future slice is introduced.

### 3.1 Production File Allowlist (8 New, 3 Modified)

All paths below are repository-relative and exact. Prefix `M = backend/src/main/java/com/onecall/core/` is expanded by simple concatenation; no wildcard authorizes another file.

| Action / path relative to M | Owner and exact responsibility |
| --- | --- |
| NEW `operations_iam/operationscommand/OperationsCatalogDraftCommand.java` | Operations-owned public typed command/result/error port; no SQL/Spring/Jackson types |
| NEW `operations_iam/internal/web/OperationsCatalogDraftController.java` | Two exact endpoints, trusted caller/trace, bounded body, lazy unique provider, exact bytes and problems |
| NEW `operations_iam/internal/web/CatalogDraftRequestBinder.java` | Closed strict JSON, canonical UUIDs, validated commercial and immutable inputs |
| NEW `catalog_order/internal/operationsdraft/CatalogDraftConfiguration.java` | Lazy persistence bean binding the existing writable template and employee authority port |
| NEW `catalog_order/internal/operationsdraft/DefaultOperationsCatalogDraftCommand.java` | Authority, owner transaction, result/status, bounded recovery; never reads employee table |
| NEW `catalog_order/internal/operationsdraft/CatalogDraftRepository.java` | Owner-local persistence interface and transaction-role operations |
| NEW `catalog_order/internal/operationsdraft/JooqCatalogDraftRepository.java` | Explicit catalog SQL, exact role/witness checks, locks, conditional mutations, audit and restoration |
| NEW `catalog_order/internal/operationsdraft/CatalogDraftSemantics.java` | Single commercial validator, canonical bytes/hash/equality, response encoding, tokens and safe increment |
| MODIFY `operations_iam/internal/web/OperationsBoundaryFilter.java` | Add precisely POST collection / PUT single draft admission; preserve all shipped templates and precedence |
| MODIFY `customer_iam/internal/web/CustomerBoundaryFilter.java` | Exact early chain continuation for those two templates only; no customer authentication/header rewrite |
| MODIFY `operations_iam/internal/web/OperationsProblemWriter.java` | Exhaustive draft Code overload using the shipped five-field sanitized envelope |

Reuse existing `operations_iam/operationscommand/package-info.java` named interface `operations-catalog-sale-command`: the new port belongs to the same Operations catalog command family, with no rename or broadened internal exposure. `catalog_order` already depends on that named interface, employee-authority-read and operations-subject-read. The configured catalog set remains 5, module edges remain 13 and modules remain 14. No `package-info.java` or ModuleBoundaryTests modification is necessary. operations_iam retains only its configured operations_audit dependency; its controller must not import catalog internals. Keep the new owner code in `internal/operationsdraft`, so PA38's sale repository SQL guards continue to constrain that repository unchanged.

### 3.2 Expected Types And Functions

Top-level types are exactly the eight new filenames above. Nested types/functions are pinned here; private mechanical exception/encoding helpers may be private methods within these files, not new architectural abstractions.

- `OperationsCatalogDraftCommand`: `create(CreateCommand)`, `replace(ReplaceCommand)`; records `Caller(UUID employeeId, UUID traceId)`, `Commercial(String offerType,String audience,Long priceCents,String currency,String billingCycle,int installments,int grantIntervalMonths)`, `BenefitLine(String entitlementType,long quantity,int validityDays,int grantIntervalMonths)`, `CreateCommand(UUID productId,int versionNo,Commercial commercial,List<BenefitLine> benefitLines,List<String> channels,Caller caller)`, `ReplaceCommand(UUID versionId,int expectedRevision,Commercial commercial,Caller caller)`, `CatalogVersionDraft` (17 frozen fields in §5.2 order), `CreateResult(CatalogVersionDraft draft,boolean created)`; closed `Code` exactly UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CATALOG_DRAFT_CONFLICT, CATALOG_DRAFT_STALE_VIEW, VALIDATION_FAILED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL; sanitized `Failure(Code)`. Lists defensively copied; command construction enforces numeric/canonical content invariants too. No client revision in create, no hidden IDs/tokens in benefit DTOs.
- `CatalogDraftRequestBinder`: `create(byte[])`, `replace(String,byte[])`, `canonicalUuid(String)`, `positiveInt(JsonNode)`, `nonNegativeInt(JsonNode)`, `positiveLong(JsonNode)`, `commercial(JsonNode)`, `benefits(JsonNode)`, `channels(JsonNode)`, `object(JsonNode,Set<String>)`. Return typed validated input without inventing authority. Reuse commercial validation from `Commercial` construction, not a cross-module import of owner semantics.
- `OperationsCatalogDraftController`: `createOperationsCatalogVersionDraft`, `replaceOperationsCatalogVersionDraft`, `body`, `caller`, `command`, `success`, `known`, `unexpected`. Body reads at most 8193 bytes once. Provider stream limit 2 and lazy-instantiation failures sanitize 503, including an unavailable required capability.
- `CatalogDraftConfiguration.operationsCatalogDraftCommand`: bind exactly `operationsSubjectReadCatalogSaleTransactionTemplate`, one EmployeeAuthorityReadPort and one CatalogDraftRepository. Resolve authority/repository through ObjectProvider streams limited to two and require exactly one, including when a duplicate is Primary; do not let Spring's preferred-candidate selection conceal ambiguity. Missing/throwing/ambiguous dependency construction fails lazy command resolution with sanitized 503, never an alternate authority. Keep `@Profile("persistence")`, the existing `onecall.operations-subject-read.enabled=true` condition and lazy resolution. Validate template writable/REQUIRES_NEW/READ_COMMITTED/timeout 5. Repository qualifiers are exactly `operationsSubjectReadDataSource` and `operationsSubjectReadDsl`.
- `DefaultOperationsCatalogDraftCommand`: `create`, `replace`, `execute`, `attempt`, `recover`, `sqlState`, `requireAdmin`. One normal transaction, at most one recovery transaction, no write retry loop.
- `CatalogDraftRepository`: `begin`, `catalogRole`, `restore`, `create`, `replace`, `recoverCreate`, `recoverReplace`; recovery is read-only in behavior, not a read-only PostgreSQL transaction incompatible with the authority witness.
- `JooqCatalogDraftRepository`: those methods plus `connection`, `identity`, `assertIdentity`, `lockProduct`, `readProduct`, `readNaturalDraft`, `readDraft`, `readBenefits`, `readOffers`, `insertDraft`, `insertBenefits`, `insertOffers`, `updateDraft`, `regenerateTokens`, `appendAudit`, `exactlyOne`. Nested owner-only `StoredDraft`, `StoredBenefit`, `StoredOffer` records retain hidden IDs/creator/timestamps without exporting them.
- `CatalogDraftSemantics`: `projection`, `canonicalBytes`, `sha256`, `sameProjection`, `validateStored`, `response`, `token`, `increment`, `auditSnapshot`, `auditRequestId`. No second commercial JSON projection in the repository or controller. Serialization helpers use explicit member order, not unspecified map iteration.

The controller serializes the frozen result using one explicit ordered response encoder local to its file, with golden cross-checks against owner response construction; it must not call catalog_order internals. The canonical owner encoder derives the response's commercial fields from the same projection. This avoids a forbidden reverse dependency while tests pin byte equality. No framework type crosses the public port.

## 4. Admission, Parsing, Validation And Error Precedence

### 4.1 Exact Route And Media Behavior

Operations filter adds two predicates, both with `(request.getContextPath() + path).equals(request.getRequestURI())`:

- POST and path exactly `/api/v1/operations/catalog/versions`.
- PUT and `path.matches("/api/v1/operations/catalog/versions/[^/;%]+/draft")`.

Keep current reads, subject commands and publication/offsale predicates untouched. Unknown catalog mutation paths/methods, trailing/extra segments, encoded/semicolon variants and suffixes are concealed 404 before authentication. An admitted safe single segment that is not a canonical UUID fails structural parsing with 422 after authentication. No new GET/PATCH/DELETE/HEAD/OPTIONS mapping, Location header, route alias, query parameter or idempotency scope. Reject any query string on the two new commands with 422 after session authentication; it cannot convey authority.

CustomerBoundaryFilter adds a separate early branch before trace creation, customer headers/authentication/buffering. Its predicate is the same method + servlet-path + context/URI equality for exactly those two draft shapes. It calls the incoming chain once and returns with the same trusted request and response. Do not rewrite existing read/subject/PA38 branches or filter registrations. Operations remains HIGHEST_PRECEDENCE; Customer remains +1; do not add ERROR dispatch behavior.

Precedence, including negative combinations:

1. Exact admission/concealment → 404 on unadmitted paths, without verifier/provider/body access.
2. Existing employee authenticator with audience `onecall:employee-session:v1`: absent/invalid/wrong audience → 401; unavailable/ambiguous verifier → 503. Authentication wins over invalid bodies/media.
3. Shipped media handling: exactly one Content-Type, application/json (case-insensitive), no parameters except optional UTF-8 charset; otherwise 422. Accept parser/specificity/quality behavior stays byte-for-byte compatible: absent or acceptable JSON succeeds, exclusion/no compatible JSON produces shipped 500 INTERNAL, not invented 406; malformed Accept follows the existing sanitized 500 path. Do not change this for PA38/subject commands.
4. Strict URI/query/body/schema/commercial validation → 422, before command provider or database authority. No authority-before-input rule from PA37 reads is imported into writes.
5. Exactly one usable command provider → otherwise 503.
6. Inside owner transaction, current employee snapshot → ACTIVE ADMIN admits; ACTIVE OPS or DISABLED → 403; missing/ambiguous/mismatched/malformed/unavailable authority → 503. No authority row locking or advisory employee lock.
7. Product/version existence → 404; then well-formed persisted state, state/revision/projection decisions → §6; then mutations/audit.

Shipped success/problem headers: Cache-Control `no-store`, Pragma `no-cache`, X-Content-Type-Options `nosniff`; exact UTF-8 byte Content-Length. Success Content-Type `application/json`; problem `application/problem+json`. Only 401 has `WWW-Authenticate: Bearer`. No token, raw snapshot, SQL/role/constraint, credential, body or exception cause leaks. No optional details/errors array is invented.

### 4.2 Strict Request Structure

Create fields exactly once: productId, versionNo, offerType, audience, priceCents, currency, billingCycle, installments, grantIntervalMonths, benefitLines, channels. Replace fields exactly once: expectedRevision plus the seven commercial fields. Every field is required, including explicitly nullable priceCents. Nested benefit fields exactly once: entitlementType, quantity, validityDays, grantIntervalMonths.

Strict duplicate detection at every object depth; reject unknown, missing, wrong-type, null except price, non-object root, malformed/trailing JSON, multiple roots, BOM/non-JSON content, invalid UTF-8, oversized/empty/partially unreadable body. JSON numbers must be integral nodes, not strings, floating/exponent forms or booleans. UUID strings equal `UUID.fromString(text).toString()` with no case/space/braces/short-form normalization. Body limit 8192 bytes is a local bounded-parser guard matching PA38, not an OpenAPI edit: readNBytes(8193), reject >8192; never unbounded readAllBytes.

versionNo, expectedRevision, installments, validityDays are integers 1..2,147,483,647; grantIntervalMonths is 0..2,147,483,647; priceCents is null or 0..9,223,372,036,854,775,807; benefit quantity is 1..9,223,372,036,854,775,807. Arithmetic must not overflow; compare replay successor using widened arithmetic. No tighter schema cap or JavaScript-safe-integer restriction.

| offerType | audience | priceCents | billingCycle | installments | grantIntervalMonths |
| --- | --- | --- | --- | --- | --- |
| SUBSCRIPTION | PERSONAL | non-null nonnegative long | MONTHLY / QUARTERLY / ANNUAL | positive int | positive int |
| ONE_TIME | PERSONAL | non-null nonnegative long | ONCE | exactly 1 | exactly 0 |
| ADD_ON | PERSONAL | non-null nonnegative long | ONCE | exactly 1 | exactly 0 |
| QUOTE | ENTERPRISE | explicitly null | QUOTE | exactly 1 | exactly 0 |

Currency exactly CNY even though the database check permits other uppercase triples. Reject all cross-matrix combinations, unknown/case-varied enums and negative/overflow values. Subscription does not impose an invented 1/3/12 relation between cycle, installments and grant interval. Benefit grant interval is independently nonnegative, not forced equal to parent.

benefitLines may be empty; allowed unique entitlementType identities are AI_SECONDS, EVIDENCE_COUNT, RECORDING_SECONDS, STORAGE_BYTES. Reject duplicate types even if quantities match; no duplicate-merge semantics. Canonical ordering is ascending ASCII entitlementType in that listed order, irrespective of input order and generated UUIDs. channels may be empty; allowed values MINIAPP, WEB, each at most once, canonically in that order. Do not add minItems or require all types/channels. Child row IDs generated server-side once, retained on replace/replay, and never projected. SQL orders by entitlement_type COLLATE "C" / channel COLLATE "C"; never uses physical row order.

Client status/saleStatus, publishedAt, tokens, creator, timestamps, snapshot/hash, create revision, versionId/catalogVersionId are unknown fields → 422. Replace productId/versionNo/benefitLines/channels are also unknown → 422 even when equal; the frozen replace schema has no such members. This is structural rejection, not a semantic 409. A 409 immutable-content conflict means a valid command cannot match the stored immutable projection/state; it is not permission to accept forbidden members. Idempotency-Key is not required, inspected or used; presence alone does not alter identity, status, audit request ID or outcome. Existing subject commands retain their own exact requirement.

## 5. One Canonical Redacted Commercial Projection

### 5.1 Definition, Hash And Equality

Define P as exactly this JSON member sequence and types, with no whitespace or trailing LF:

```text
{"productId":"<canonical UUID>","versionNo":<int>,"offerType":"<enum>","audience":"<enum>","priceCents":<long-or-null>,"currency":"CNY","billingCycle":"<enum>","installments":<int>,"grantIntervalMonths":<int>,"benefitLines":[{"entitlementType":"<enum>","quantity":<long>,"validityDays":<int>,"grantIntervalMonths":<int>}],"channels":["MINIAPP","WEB"]}
```

Arrays in the template illustrate element/member form, not mandatory cardinality. Use §4 ordering, integer base-10 without leading plus/zero padding, explicit price null, all required members always present, UTF-8, standard JSON quoting, and no BOM. No creator, generated version/child/offer ID, token, lifecycle status, revision, createdAt, publishedAt or hash enters P. Those are response metadata, not commercial content. In particular, excluding revision/generated values makes identical create retries comparable without trusting client identity/timestamp and lets mutable draft content have a meaningful canonical hash.

Independent golden vector: productId `00000000-0000-0000-0000-000000000001`, versionNo1, ONE_TIME/PERSONAL, price0, CNY/ONCE, installments1, grantIntervalMonths0, empty benefitLines/channels yields **238 UTF-8 bytes** and SHA256 `3e4aed3f160778c327aab7b942bf9e3f30f20b51eef92a60f3c67f081b740271`. With catalogVersionId `00000000-0000-0000-0000-000000000002`, DRAFT, revision1, createdAt `2026-11-20T00:00:00.123456Z`, null publishedAt and that hash, the §5.2 response is exactly **475 bytes** at either 201 or replay200. These are test vectors, not a runtime seeded product or a new public field.

Persist `snapshot = P` as bound jsonb and `snapshot_sha256 = lowercaseHex(SHA-256(exact canonical UTF-8 P bytes))`. PostgreSQL jsonb reorders textual keys; never hash `snapshot::text`. Rebuild P from strictly parsed persisted snapshot, canonicalize it and verify its bytes/hash against P independently reconstructed from version columns and immutable benefit/channel rows. A valid legacy object whose member set is not exactly P is a nonmatchable draft and returns 409 CATALOG_DRAFT_CONFLICT (no backfill). Once the exact P member set is recognized, wrong types/null/array shape, row/snapshot disagreement or a bad stored hash are dependency/data-integrity 503, not silently repaired. JSONB object physical ordering is not an authority. Legitimate reordered request arrays canonicalize equal; duplicate array identities are invalid, not equal.

`sameProjection` means exact canonical byte equality of P; hash equality alone is insufficient. Compare productId/versionNo and every commercial value, all four values per benefit in canonical order, channel set/order, snapshot canonical bytes and verified stored hash. Never compare only seven mutable fields or ignore corrupted immutable children. Existing never-published fixtures without this canonical projection are not silently migrated; nonmatching legacy member sets conflict, while malformed/internally inconsistent recognized persistence is 503. No data-repair work is added. Read and classify valid lifecycle state before demanding draft snapshot recognition: a published/non-DRAFT row remains 409 regardless of its historical snapshot format.

Create comparison uses requested P against the locked existing P. Replace comparison substitutes requested Commercial into the locked current product/version/benefits/channels to form P_requested; there is no client immutable override. Applied retry requires current revision == expectedRevision+1 and exact P_requested == verified current P, not arbitrary later equality. Metadata (current revision, ID and createdAt) comes from the existing row, never a newly generated retry candidate.

### 5.2 Exact Response And Tokens

CatalogVersionDraft has exactly these 17 members in this order:

`catalogVersionId, productId, versionNo, saleStatus, offerType, audience, priceCents, currency, billingCycle, installments, grantIntervalMonths, benefitLines, channels, revision, createdAt, publishedAt, snapshotSha256`.

catalogVersionId is server UUID; productId/versionNo/commercial/arrays come from verified P; saleStatus is DRAFT; revision is positive stored revision; createdAt is stored timestamptz; publishedAt explicitly null; snapshotSha256 is verified P hash. No omitted nulls, hidden IDs or versionToken. createdAt is encoded UTC RFC 3339 using Instant.toString(), with PostgreSQL microsecond precision (choose one `clock_timestamp()` value read from PostgreSQL for insertion, do not hash an unroundtripped nanosecond clock); preserve it byte-stably on all retries/replaces. Return no fresh timestamp on replay. Response has no trailing newline. Golden tests compare exact UTF-8 bytes and computed byte length, including QUOTE null and empty arrays.

New version/child/offer identities use server UUID.randomUUID(). For each channel, version_token is the 64 lowercase hex SHA256 of UTF-8 `catalog-draft-token:v1\n` + canonical version UUID + `\n` + channel + `\n` + new version revision in decimal + `\n` + P hash; no terminal newline. Distinct channel/revision/content bind distinct preimages. On replace regenerate every existing channel token, increment its own revision by exactly one; zero channels means zero offer updates. Do not expose the preimage/token or accept client token. Version and offer revision overflow yields sanitized 503 before any write; no wrap/reset. Token/revision are deliberately excluded from P because they are server-derived, not commercial equality inputs.

## 6. Owner Transaction, Locks, Race Recovery And State Matrix

### 6.1 Shared Transaction And Authority Sequence

Use existing `operationsSubjectReadCatalogSaleTransactionTemplate`: REQUIRES_NEW, READ_COMMITTED, writable, timeout 5, rollback-on-commit-failure and GuardedReadDataSource cleanliness assertion. All jOOQ SQL uses the transaction-aware existing DSL and physical datasource. No nested authority transaction or independently borrowed connection.

`begin()` mirrors PA38: require actual writable transaction, ConnectionHolder, non-autocommit READ_COMMITTED physical connection; set LOCAL statement_timeout 2000ms, lock_timeout 250ms, idle_in_transaction_session_timeout 5000ms; assert actual settings; capture login/session_user, backend PID, txid_current and current role==login. Save the physical connection identity and after-completion cleanup.

Authority port `read(employeeId,witness)` checks that same connection/witness, performs exact `SET LOCAL ROLE operations_iam_role`, and reads only `SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = ?`. Require one matching positive-revision snapshot. Missing row → 503; DISABLED or OPS → 403. Unknown/malformed status/role/revision is 503, not an accidental ADMIN allowance. No caller-supplied role/status/creator. After admitting current ACTIVE ADMIN, repository asserts operations_iam_role then executes exactly `SET LOCAL ROLE catalog_order_role`; assert same login/PID/transaction/connection/read-only-off before owner SQL and after mutation. Audit executes as catalog_order_role through the granted function, not SET ROLE audit_owner.

Normal SQL role sequence is exactly operations_iam_role → catalog_order_role → NONE. Authority denial is operations_iam_role → NONE; role entry failure rolls back. On success execute `SET LOCAL ROLE NONE`, assert login and the same witness, remove connection witness; transaction commits only afterwards. On non-SQL failure mark rollback-only and attempt restoration; SQL-aborted transactions must roll back rather than issue unusable reset SQL, then verify/evict dirty pooled connections using existing guard. Restoration failure overrides success with sanitized 503 and rollback; never return success after a cleanup/commit failure. ThreadLocal cleanup occurs on both normal and afterCompletion paths, with no cross-request witness reuse.

### 6.2 Locks And Create

Reuse PA38's product publication advisory key **exactly**: text `catalog_order.publication:` + canonical product UUID; SQL `SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))`. This is necessary, not a second lock subsystem: PA38 reads candidate content under this same lock before its conditional publication update; draft replace must not let PA38 publish/audit an obsolete content snapshot. Create also holds it until complete children/audit commit, so publication cannot inspect partially built application state. Do not invent a different seed/key, two-key advisory lock or employee/natural-key lock.

Create after authority: read existing product by ID (404 if absent); acquire product advisory lock; then select natural `(product_id,version_no)` with explicit columns FOR UPDATE. Product is immutable, so no product FOR UPDATE, max-version scan or product creation. Lock order is authority read (unlocked) → product advisory lock → version row → offers ordered by channel. Benefits are immutable and read ordered without row locks. No unrelated product/version locking; all same-product lifecycle writers share advisory order. READ_COMMITTED takes fresh statements after lock wait, not a pre-lock decision snapshot.

If natural row exists, validate it and its children. Non-DRAFT/published or unequal valid P → 409 conflict. Equal never-published DRAFT → current response, 200, no DML/audit/token changes, even if revision >1 after intervening valid changes that restore equal content. No historical-original-content replay is promised after content changes.

If absent, choose server version UUID and PostgreSQL time once; derive P/hash; `INSERT INTO catalog_order.catalog_version` with explicit identity/product/version/commercial/snapshot/hash/creator/created_at; fix DRAFT/revision1/published_at null (defaults may supply those fixed values). Use `ON CONFLICT ON CONSTRAINT uq_catalog_version_no DO NOTHING RETURNING id` only for this natural key. Insert child rows once, in canonical order, with new IDs and ACTIVE offer status/revision1/tokens. Append exactly one create audit, return verified response with 201. Any child/audit/role/commit failure rolls back all rows; do not publish partial state.

If natural-key INSERT returns zero, perform one locked natural-key reread under the already held product lock; classify equal DRAFT as 200, valid mismatch/state as 409, absent after reread as 503. No second INSERT and no audit. Normal application writers serialize; this is defensive against direct/non-cooperating unique races. There is no generic `ON CONFLICT DO NOTHING` hiding unrelated PK/child/token constraints.

### 6.3 Replace

After authority: preliminary explicit read of version ID/product ID (404 absent); acquire same product advisory lock; reread full target FOR UPDATE and verify identity/product, then children. If it disappeared, 404; malformed identity/persistence is 503. Require never-published DRAFT before comparing revision. Build P_requested from stored immutable facts and requested seven commercial fields.

- Current revision r == expected e: apply once, even if requested commercial content equals current content. No-op intent is still a revision-conditional command; increment version revision once, derive canonical snapshot/hash, regenerate existing offer tokens and increment each offer revision once, append one replace audit, return 200. Precheck all revision increments.
- r == e+1 and exact verified P_requested == current P: applied replay, 200; no SQL UPDATE/INSERT, no token regeneration/audit or timestamp changes.
- Every other revision relation, including r<e, r>e+1 or r==e+1 with different content: 409 CATALOG_DRAFT_STALE_VIEW, no writes.
- Non-DRAFT / published target takes precedence over stale/projection replay: 409 CATALOG_DRAFT_CONFLICT.

Conditional SQL sets only `offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,revision=revision+1`, with WHERE `id=? AND product_id=? AND sale_status='DRAFT' AND published_at IS NULL AND revision=?`. Never SET identity, status, published_at, created_by_employee_id, created_at, product_id or version_no. Offer SQL sets only version_token/revision with WHERE id/version/channel/old revision; require exactly one update per existing row and unchanged channel cardinality. No benefit INSERT/UPDATE/DELETE or offer INSERT/DELETE on replace. Child IDs, relation, channel, status and created_at remain byte-unchanged; benefits are entirely byte-unchanged. Concurrent independent withdrawal is not implemented or papered over: unexpected stored offer status/shape is nonmatchable conflict (or malformed-data 503).

A zero-row version update, despite held locks, must not proceed to offer/audit mutation: bounded locked reread once and use state/revision/projection matrix; unchanged DRAFT r=e is anomalous 503, never a second update. A zero-row offer update after version mutation requires rolling back the whole attempt before recovery; never commit a changed version with skipped channel. No repeated updates to chase a moving view.

### 6.4 Audit And Recovery

Each actual create/replace appends one `audit.append_fact` in the same transaction after all owner writes and before restoration. actor_type EMPLOYEE, actor_id actual authority-checked employee UUID, action CATALOG_DRAFT_CREATE or CATALOG_DRAFT_REPLACE, object_type CATALOG_VERSION, object_id server target UUID, reason_code ACTION_DERIVED. occurred_at PostgreSQL decision time (UTC); trace_id is the filter UUID string.

request_id is action-derived, independent of caller headers: `catalog_version:<uuid>:draft:create` or `catalog_version:<uuid>:draft:replace:<newRevision>`. It is an audit correlation identity, not a database-enforced audit dedupe or platform idempotency scope. Create before_snapshot is SQL NULL; after_snapshot is exactly ordered `{\"productId\":...,\"versionNo\":...,\"saleStatus\":\"DRAFT\",\"revision\":1,\"snapshotSha256\":...}`. Replace before/after use that same five-member shape with old/new revision/hash. Never place P, token, creator credential or full request in audit. Replays/conflicts/stale/denials append **zero** facts, including no separate denial audit in this bounded slice. No draft outbox event or platform idempotency record is added.

Raw 23505 or 40001 requires rollback before any reread. Permit at most one new recovery transaction: begin with login witness, **recheck current ACTIVE ADMIN through the authority port**, enter catalog role, reacquire the same product lock and read current owner state. Do not copy PA38's recovery shortcut that enters catalog role without a caller witness. Recovery cannot execute INSERT/UPDATE/audit or regenerate tokens. Natural identity absent after unexpected unique failure → 409 CATALOG_DRAFT_CONFLICT; present equal never-published draft → 200; different valid P/state → 409. Unrelated UUID/child uniqueness must never synthesize a second identity and retry insertion. Replace recovery permits only exact successor replay; state conflict/stale/missing use the matrix. If r==e after a failed write, it was not applied: return sanitized 503, not fabricated 200 or a second application. Authority/dependency/recovery failure → 503 (current legitimate authority denial remains 403). No recursion or second recovery after recovery failure.

Lock/statement timeout, unavailable role/connection/capability or unexpected integrity/check/trigger exception → sanitized 503; do not classify every constraint as a user 409/422. Only decoded valid natural/state/projection results get frozen domain errors. Unexpected application serialization/programming failure → sanitized 500. Timeouts do not trigger write retries. All rejected/replayed paths preserve catalog, child, audit, outbox and platform idempotency durable state.

### 6.5 Complete Observable Matrix

Earlier admission/authentication/structural/provider/authority rows in §4 always win over the rows below.

| Operation / locked condition | Status / code | Durable changes |
| --- | --- | --- |
| Create, product absent | 404 NOT_FOUND | none |
| Create, natural key absent, all writes commit | 201 CatalogVersionDraft | one version, N benefits, M offers, one audit |
| Create, equal valid never-published DRAFT, any positive revision | 200 current CatalogVersionDraft | none |
| Create, valid natural row but changed P | 409 CATALOG_DRAFT_CONFLICT | none |
| Create, ACTIVE/OFFSALE/published natural row | 409 CATALOG_DRAFT_CONFLICT | none |
| Create, zero insert count then one reread equal/different/absent | 200 / 409 conflict / 503 | none from losing attempt |
| Create, raw unique/serialization after rollback and recovery natural row absent | 409 CATALOG_DRAFT_CONFLICT | none |
| Replace, version absent | 404 NOT_FOUND | none |
| Replace, ACTIVE/OFFSALE/published, any e/content | 409 CATALOG_DRAFT_CONFLICT | none |
| Replace, DRAFT r=e and valid content, including equal content | 200 updated CatalogVersionDraft | version r+1, M token/revision updates, one audit |
| Replace, DRAFT r=e+1 and exact P_requested=current P | 200 current CatalogVersionDraft | none |
| Replace, DRAFT r=e+1 unequal P, r>e+1 or r<e | 409 CATALOG_DRAFT_STALE_VIEW | none |
| Replace, unmatchable valid immutable child/channel state | 409 CATALOG_DRAFT_CONFLICT | none |
| Recovery after failed replace, DRAFT r=e | 503 SERVICE_UNAVAILABLE | none |
| Valid syntax but malformed/inconsistent persistence, missing role/audit/dependency or cleanup | 503 SERVICE_UNAVAILABLE | rollback all |
| Invalid/unknown/missing/duplicate request fields or matrix violation | 422 VALIDATION_FAILED before owner lookup | none |
| Frozen mapped limiter result, if supplied | 429 RATE_LIMITED | none; no new limiter |
| Unexpected controller/application exception | 500 INTERNAL | rollback if transaction started |

409 bodies are exactly `{"type":"about:blank","title":"Catalog draft conflict","status":409,"code":"CATALOG_DRAFT_CONFLICT","traceId":"<filter UUID>"}` or title `Catalog draft stale view` / code CATALOG_DRAFT_STALE_VIEW, with the same member order. Other titles match the shipped writer: Authentication required, Access denied, Not found, Request validation failed, Too many requests, Service unavailable, Internal error. Do not expose winner IDs, expected/current revisions, snapshot or constraint details in problems.

## 7. Exact Future Tests And File Inventory

Every listed name is one ordinary `@Test void` identity, no parameterized/dynamic expansion or annotation mentions in new comments. Matrix subcases are loops/assertions inside the named method, not additional methods. Tests must assert behavior, not only source substrings. Prefix `T = backend/src/test/java/com/onecall/`; paths concatenate exactly.

### 7.1 Seven New Surefire Classes: 84 Methods

`Tcore/operations_iam/internal/web/CatalogDraftRequestBinderTests.java` — 12:

1. `createAcceptsExactlyFrozenFields`
2. `replaceAcceptsExactlyFrozenFields`
3. `rejectsUnknownDuplicateAndMissingFieldsAtEveryDepth`
4. `rejectsInvalidUtf8TrailingTokensAndNonObjects`
5. `requiresCanonicalProductAndVersionIds`
6. `enforcesIntegralPositiveAndStorageBounds`
7. `enforcesEveryOfferAudiencePriceBillingMatrixCell`
8. `requiresCnyAndRejectsUnknownEnumValues`
9. `canonicalizesUniqueBenefitLinesAndChannels`
10. `acceptsEmptyBenefitsAndChannels`
11. `rejectsAllClientOwnedMetadataAndImmutableReplaceFields`
12. `rejectsOversizedEmptyAndUnreadableBodies`

`Tcore/catalog_order/internal/operationsdraft/CatalogDraftSemanticsTests.java` — 14:

1. `projectionHasExactMemberOrderTypesAndNulls`
2. `projectionOrdersBenefitsAndChannelsIndependentlyOfInput`
3. `projectionExcludesAllGeneratedAndHiddenMetadata`
4. `snapshotHashUsesExactUtf8BytesWithoutNewline`
5. `reconstructsJsonbWithoutHashingDatabaseText`
6. `requiresBytesAsWellAsHashForProjectionEquality`
7. `rejectsMalformedOrInconsistentPersistedFacts`
8. `responseContainsExactlySeventeenFrozenFields`
9. `responsePreservesNullPriceAndPublishedAtAndEmptyArrays`
10. `responseUsesStoredUtcMicrosecondTimestamp`
11. `tokensBindVersionChannelRevisionAndProjectionHash`
12. `revisionArithmeticRejectsOverflowWithoutWrapping`
13. `replayRequiresExactSuccessorRevisionAndProjection`
14. `auditSnapshotsAndRequestIdsAreActionDerivedAndRedacted`

`Tcore/catalog_order/internal/operationsdraft/CatalogDraftRepositoryTests.java` — 12:

1. `requiresWritableSharedPhysicalTransactionWitness`
2. `assertsExactRoleSequenceAndLocalTimeouts`
3. `locksExistingPublicationKeyBeforeCatalogDecision`
4. `createUsesNaturalConstraintAndExplicitInsertColumns`
5. `createInsertsCanonicalChildrenExactlyOnce`
6. `createReplayAndConflictUseNoDmlOrAudit`
7. `replaceSetsOnlyAllowedVersionColumnsConditionally`
8. `replaceOnlyRegeneratesExistingOfferTokensAndRevisions`
9. `neverRewritesBenefitsOrImmutableOfferFacts`
10. `appendsExactlyOneActionDerivedAuditAndNoOutbox`
11. `zeroRowsAndRawUniqueRacesUseBoundedReread`
12. `restoresRoleAndDiscardsWitnessOnEveryExit`

`Tcore/catalog_order/internal/operationsdraft/DefaultOperationsCatalogDraftCommandTests.java` — 14:

1. `requiresCurrentActiveAdminForBothCommands`
2. `opsAndDisabledEmployeesAreForbidden`
3. `missingMalformedAndUnavailableAuthorityFailClosed`
4. `createReturnsCreatedOrReplayStatusWithoutDuplicateFacts`
5. `changedNaturalProjectionAndNonDraftCreateConflict`
6. `missingProductAndVersionAreNotFound`
7. `replaceAppliesOnlyAtExpectedRevision`
8. `sameContentAtExpectedRevisionStillIncrementsOnce`
9. `exactSuccessorProjectionReplaysWithoutWrites`
10. `allOtherRevisionRelationsAreStale`
11. `nonDraftConflictPrecedesStaleAndReplay`
12. `auditMutationAndCleanupFailuresRollbackEverything`
13. `recoveryReauthenticatesOnceAndNeverRetriesWrites`
14. `closedPortAndFailuresExposeNoPersistenceOrCredentialTypes`

`Tarchtests/OperationsCatalogDraftConfigurationTests.java` — 8:

1. `reusesExistingWritableTransactionTemplate`
2. `reusesSameSharedDatasourceAndDsl`
3. `preservesExactTransactionAndTimeoutSettings`
4. `constructsNoSecondPoolCredentialOrRoleFamily`
5. `resolvesExactlyOneLazyCommandProvider`
6. `missingAndAmbiguousProvidersFailClosed`
7. `unavailableRequiredCapabilitiesFailClosedWithoutFallback`
8. `defaultAndWebOnlyRuntimeHaveNoDraftWriteBeans`

`Tarchtests/OperationsCatalogDraftWebTests.java` — 16:

1. `admitsOnlyExactCreatePostAndReplacePut`
2. `concealsAllOtherCatalogMutationTemplatesBeforeAuthentication`
3. `preservesFullFilterChainAndNeverInvokesCustomerAuthentication`
4. `authenticationAndAudiencePrecedeBodyAndProviderResolution`
5. `shippedContentTypeAndAcceptOutcomesRemainExact`
6. `structuralValidationPrecedesProviderAndAuthority`
7. `doesNotRequireOrUseIdempotencyKey`
8. `strictBodyParsingRejectsUnknownMetadataAndNestedDuplicates`
9. `bodyConsumptionIsBoundedAt8193BytesForBothRoutes`
10. `create201AndReplay200HaveExactBytesHeadersAndLength`
11. `replaceAndReplay200HaveExactBytesHeadersAndLength`
12. `conflictAndStale409HaveExactProblemBytesHeadersAndLength`
13. `authorityAndMissingResourceErrorsUseFrozenPrecedence`
14. `missingAmbiguousAndThrowingProvidersAreSanitized503`
15. `unexpectedFailuresAndDeclaredRateLimitUseFrozenProblems`
16. `preservesExistingReadSubjectAndPublicationAdmissionBehavior`

`Tarchtests/architecture/OperationsCatalogDraftBoundaryTests.java` — 8:

1. `scansExactPlannedProductionAndTestFiles`
2. `reusesExistingOperationsOwnedNamedInterfaceWithoutNewEdge`
3. `catalogOwnerNeverReadsEmployeeAccountOrForeignTables`
4. `requiresExactSharedTransactionRolesAndConnectionAssertions`
5. `sqlWritesOnlyFrozenDraftAndTokenColumns`
6. `forbidsChildMutationIdempotencyOutboxAndNewResources`
7. `admissionAndCustomerPassThroughHaveOnlyTwoNewTemplates`
8. `preservesFrozenContractsEighteenMigrationsAndProvisioning`

### 7.2 Two New Failsafe Classes: 36 Methods

`Tarchtests/container/OperationsCatalogDraftLifecycleIT.java` — 24:

1. `postgres16RunsExactlyV001ThroughV018`
2. `createPersistsOneDraftAndExactCanonicalSnapshotHash`
3. `sameNaturalIdentityReplays200WithoutDuplicateRowsOrAudit`
4. `changedNaturalProjectionConflictsWithoutWrites`
5. `nonDraftNaturalIdentityConflictsEvenForEqualCommercialFields`
6. `missingProductAndVersionReturn404AfterAuthority`
7. `allCommercialMatrixVariantsRoundTripExactly`
8. `emptyBenefitsAndChannelsRemainValidAndStable`
9. `canonicalOrderingIgnoresRequestAndPhysicalRowOrder`
10. `replaceUpdatesCommercialSnapshotAndRevisionExactlyOnce`
11. `replaceRegeneratesTokensWithoutChangingOfferIdentityOrChannelSet`
12. `benefitRowsAndProductVersionCreatorTimestampsRemainUnchanged`
13. `appliedReplaceRetryReplaysWithoutAnyWrites`
14. `staleFutureAndNonSuccessorViewsReturnExact409`
15. `nonDraftReplaceConflictsBeforeRevisionReplay`
16. `concurrentIdenticalCreatesYieldOne201AndOne200`
17. `concurrentChangedCreatesYieldOne201AndOne409`
18. `concurrentEqualReplacesYieldOneApplicationAndOneReplay`
19. `concurrentDifferentReplacesYieldOneApplicationAndOneStaleView`
20. `draftReplaceAndPublicationShareProductLockAndCoherentSnapshot`
21. `zeroRowAndRawUniqueRacesRollbackBeforeBoundedRecovery`
22. `auditAndChildFailuresRollbackAllOwnerFacts`
23. `replaysConflictsAndDenialsHaveNoAuditOutboxOrIdempotencyWrites`
24. `fullHttpResponsesHaveExactBytesHeadersLengthsAndNoPrivateFacts`

`Tarchtests/container/OperationsCatalogDraftRoleLeastPrivilegeIT.java` — 12:

1. `runtimeLoginUsesOnlyExistingSettableNonInheritedOwnerRoles`
2. `v004EffectiveCatalogAclRemainsExactlyFortyTwoTuples`
3. `laterMigrationsAndProvisioningRetainExactEffectiveAclOracle`
4. `authorityReadsOnlyCurrentEmployeeColumnsOnOwnerConnection`
5. `createAndReplaceUseExactLocalRoleSequenceAndWitness`
6. `missingInactiveAndNonAdminAuthorityCannotMutate`
7. `forbiddenIdentityBenefitAndForeignWritesAreDenied`
8. `publishedAndAppendOnlyTriggersRejectForbiddenChanges`
9. `naturalAndChildUniqueConstraintsAndDefaultsHold`
10. `readOnlyWrongRoleAndMissingAuditCapabilityFailClosed`
11. `restorationRollbackAndPoolReuseLeaveNoDirtyRoleOrWitness`
12. `lockTimeoutsTerminatedConnectionsAndOverflowNeverPartiallyCommit`

One new zero-test fixture: `Tarchtests/container/OperationsCatalogDraftFixture.java`. Compose the existing `OperationsCatalogVersionLifecycleFixture`/catalog/shared fixtures; start real PostgreSQL16 through existing Testcontainers bootstrap, migrations and disposable non-owner login; reuse synthetic employee sessions only in test code. No fixture or verifier bean is packaged. Add request builders, canonical expected-body fixtures and jOOQ observation/fault hooks here, not production fault switches. Do not modify existing fixtures. Runtime login role setup remains operations_iam_role/catalog_order_role; retain shared provisioning oracle separately.

Concurrency tests use two actual connections, bounded executor/latch waits, and PostgreSQL lock observations proving the second transaction is waiting on the intended first transaction before release. No sleep-only race and no claim based solely on both calls returning. Exercise both lock acquisition orders for replace/publication, winner rollback, same/different natural identity, identical/divergent replace and separate products. Assert committed state, total audit cardinality, unchanged immutable row bytes and absence of duplicate rows after all threads complete. Timeout/environment failures must fail, not skip. Exact per-case outcomes follow §6; one winner/one replay means one actual revision application.

### 7.3 Existing Modified Tests And Supporting Paths

Only two existing test files are modified, with no added/removed/renamed methods:

- `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java`: only `scansExactProductionFiles` count literals 290→298 and 180→190. Retain all eight identities and all PA38 SQL/role/path guards; do not weaken content-write prohibitions for the existing sale repository.
- `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`: only current governed manifest count 310→318 and recomputed final digest in `v001ThroughV017AndPublicContractsRemainByteFrozen`. Keep independent frozen-file hashes, migration checksum/history, ACL/topology oracles and all eight identities unchanged. This plan expressly includes the manifest carrier so a legitimate product-byte change cannot be accepted with a stale pin.

Other future changes, exact paths:

- MODIFY `backend/pom.xml`: add only `**/OperationsCatalogDraftLifecycleIT.java` and `**/OperationsCatalogDraftRoleLeastPrivilegeIT.java` to existing container-smoke Failsafe includes. No dependency/plugin/toolchain change.
- MODIFY `.github/workflows/ci.yml`: exact additive maps/counts/diagnostics and draft JAR route-concealment subcases, as §8. Keep historical maps/values immutable.
- MODIFY `backend/README.md`: concise two-route draft runtime/ADMIN/snapshot/replay/immutable-child documentation and test invocation; explicit unavailable-by-default behavior, no production credentials or semantic spec rewrite.
- NEW `docs/gateflow/pa39-operations-catalog-draft-write-foundation-implementation.md`: executed evidence, final manifests/hashes/inventory and limitations, not a second specification. This plan is an input, not an implementation-edit target.

Implementation allowlist totals: **19 new + 8 modified = 27 paths**: 8 new/3 modified production Java; 10 new/2 modified test Java; 2 modified POM/CI; 1 modified README; 1 new implementation report. The plan artifact already exists at implementation entry and is not counted as a new implementation path. No Control/review/runner/generated path is included.

## 8. Exact Cumulative Arithmetic, CI Mirrors And Architecture Guards

| Inventory family | Current | PA39 delta | Final expectation |
| --- | --- | --- | --- |
| Production Java | 290 | +8 | 298 |
| Test Java, including zero-test fixtures | 180 | +10 | 190 |
| Surefire classes / methods | 97 / 934 | +7 / +84 | 104 / 1,018 |
| Failsafe classes / methods | 38 / 630 | +2 / +36 | 40 / 666 |
| Executable classes / methods | 135 / 1,564 | +9 / +120 | 144 / 1,684 |
| Annotation-bearing source lines | 1,565 | +120 | 1,685 |
| Failsafe explicit includes | 38 | +2 | 40 |
| Production resources / migrations | 21 / 18 | 0 / 0 | 21 / 18 |
| Governed manifest entries | 310 | +8 | 318 = 298+21−1 |
| Modules / discovered module edges / catalog allowed set | 14 / 13 / 5 | 0 / 0 / 0 | 14 / 13 / 5 |

New Surefire family sizes, in §7 order: `[12,14,12,14,8,16,8]`, sum 84, seven map keys. New Failsafe family sizes `[24,12]`, sum 36, two keys. Neither modified test adds an identity. Final physical production/test LOC and final manifest digest are measured after implementation; no made-up future digest or LOC assertion is permitted.

CI changes must be exhaustive, not merely an updated print string:

1. Add `pa39_unit_additions` and `pa39_container_additions` literal FQCN→set maps with **every §7 name**, ordered consistently. Merge with a disjointness assertion into the cumulative exact inventories. Preserve all historical additions and baseline calculations; do not replace old expected sets with current source discovery.
2. In the complete source scan subtract PA39 identities when reconstructing each older historical baseline (just as later additions are subtracted now). Add `pa39_start_totals == [934,630]`, sum 1564, `pa39_start_line_scan_total == 1565`. Do not accidentally count the PA39 names in PA38-start or older totals. Existing chronological labels, including PA36/PA35 order, remain historical.
3. Update current top-level Surefire total assertion 934→1018; current method_totals `[934,630]`→`[1018,666]`; total 1564→1684; line_scan_total 1565→1685; unit/container class counts 97/38→104/40; exact_inventory arguments 934/630→1018/666. Keep one historical comment hit and prohibit new accidental annotation comments.
4. POM include equality must be set-exact for all 40 Failsafe classes, not a subset. `source_classes == set(unit_expected)|set(container_expected)` must include the nine new executable classes, not the zero-test fixture. XML filenames/class names/method names/counts must match those literal maps, with zero duplicate methods, omissions, extras, skips, failures or errors. Do not synthesize XML or accept stale focused files.
5. New current diagnostic: `PA39 exact source=CI=POM=XML inventory passed: 104/1018 + 40/666 = 144/1684; PA39-start 934+630=1564; current 1018+666=1684; line scans 1565 -> 1685; POM 40 includes; zero skips/errors/failures`. Preserve the full old historical progression in the surrounding diagnostic rather than rewriting its values.
6. Retain PostgresContainerIT's independent 64-method oracle; existing 38-class Failsafe map, all unit identities and dependency-firewall/JAR checks remain intact. Add smoke assertions for only the two new routes: unauthenticated 401, syntactically valid authenticated request without required adapter 503, nonallowlisted method/path 404. Do not ship deterministic employee/session/draft providers.

Architecture gates: ModuleBoundaryTests nine methods (including exact allowed map, production edges, no cycles, negative cycle/forbidden/internal-import fixtures); EvidenceRoleIvrAssociationAclBoundaryTests eight; existing PA38 boundary eight with count-only update; new draft boundary eight. No new module edge is needed. Existing OperationsCatalogProductReadBoundaryTests eight remain untouched (including no offers in its reader). Current shipped filter regression classes must remain unchanged: OperationsSubjectReadConfigurationTests, OperationsLifecycleBoundaryTests, OperationsActivationReadBoundaryTests, OperationsIvrSessionReadBoundaryTests, OperationsSubjectsWebTests and OperationsSubjectReadBoundaryTests. Also run all PA37 and PA38 unit/web/configuration/owner tests: no alias, idempotency or precedence regressions are acceptable.

Final manifest carrier must hash final production bytes after every code adjustment. It cannot be predicted here. Recompute the exact §1.2 algorithm, pin count318/digest together and rerun the guard after the final production edit. A stale pin blocks; changing unrelated frozen hashes to make the guard green is prohibited.

## 9. Future Validation Commands And Exact Acceptance Expectations

All commands here are **future expectations** unless explicitly listed as actually performed in §11. Never run Maven/npm in this planning gate: that would write target/tmp/generated artifacts outside the allowed single file. No dependency install is authorized by this plan gate.

### 9.1 Preflight And Canonical Checkout Materialization

Current non-mutating diagnostics: default `java -version` and `/opt/maven-3.9.9/bin/mvn -v` hit WSL vsock errors; Docker client is 29.1.3 but daemon access is denied on `/var/run/docker.sock`. These are environment limitations, not product defects, passing tests, or blocking schema questions. Future implementer must resolve launcher/daemon access under Controller authority before runtime validation; no sudo/install/credential fallback is authorized here.

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
MVN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
"$JAVA_HOME/bin/java" -version
"${MVN[@]}" -v
docker version
docker info
```

Expected preflight: Java21.0.13, Maven3.9.9, working Docker server and loopback sockets; require actual PostgreSQL16 (current image patch must be reported, not assumed 16.15/16.16). Use the pinned Node22.23.4 for contract commands. If unavailable, report and resolve; never use disabledWithoutDocker to produce skips or count it as acceptance.

Inherited PA35 materialization issue is measured in this checkout: `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md` has 1,344 CRLF lines; raw SHA256 `d5ba84e34124e0cb44fd5a6c8fdc008a8894c3dee472268997d6a4f02500de55`, LF SHA256 `58ec889fe812cd1e0c439a7d3c1054aa41bfe8bd44bcd27183512fbe3ffd85d3`. Its unchanged IVR guards derive a raw frozen-table aggregate. As in accepted PA38 validation, use a Controller-recorded external `/tmp/pa39-lf-validation.*/candidate` projection, **not** repository tmp/target for preparation and not the primary worktree. Copy all tracked inputs plus implementation allowlisted additions, exclude private metadata/helpers and generated directories; verify each source byte equals the candidate before normalization. Normalize only that PA35 document CRLF→LF in the projection; keep every production/test/POM/CI byte identical to the implementation candidate. Record original HEAD/tree, raw/final file manifests, normalization exception and durable validation root. Never normalize frozen inputs in the primary worktree or edit the IVR tests. This is a test-input materialization contract, not a product-code change.

All runtime/focused/full/contract/JAR commands use that one recorded projection, after restoring the same toolchain. Recheck byte equality of product/test/CI inputs before accepting results. A future implementation can create build outputs only there under its execution authorization. Raw-checkout diagnostics are supplemental: known possible failures are `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact` and `OperationsIvrSessionReadsIT.postgres16UsesExactlyFrozenEighteenMigrations`; no other failure may be relabeled CRLF. Any canonical failure blocks. Do not reuse a raw/focused XML file as a successful full-run result.

### 9.2 Focused Gates

From `$PA39_VALIDATION_ROOT/backend`, with the MVN array above:

```bash
"${MVN[@]}" -Dtest=CatalogDraftRequestBinderTests,CatalogDraftSemanticsTests,CatalogDraftRepositoryTests,DefaultOperationsCatalogDraftCommandTests,OperationsCatalogDraftConfigurationTests,OperationsCatalogDraftWebTests,OperationsCatalogDraftBoundaryTests test
"${MVN[@]}" -Dtest=ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsCatalogVersionBoundaryTests test
"${MVN[@]}" -Dtest=OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectReadBoundaryTests test
"${MVN[@]}" -Dtest=OperationsCatalogProductReadRequestTests,OperationsCatalogProductCursorCodecTests,OperationsCatalogProductReadServiceTests,OperationsCatalogProductReadConfigurationTests,OperationsCatalogProductReadWebTests,OperationsCatalogProductReadBoundaryTests,EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionWebTests test
"${MVN[@]}" -Pcontainer-smoke -Dit.test=OperationsCatalogDraftLifecycleIT,OperationsCatalogDraftRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
"${MVN[@]}" -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT,OperationsCatalogProductReadsIT,OperationsCatalogProductReadRoleLeastPrivilegeIT failsafe:integration-test failsafe:verify
```

First gate: 7 classes/84 methods. Second: 3/25 (9+8+8). The six-class frozen regression selector is 6/52; the thirteen-class PA37/PA38 Surefire selector is 13/138, unchanged identities. Draft live gate: 2/36 (24+12), all PostgreSQL16-backed with no skips, exact SQL/connection/role observations and no production test hook. PA37 live gate is 2/36 (20+16) and PA38 live gate 2/38 (24+14), so combined regression IT selector is 4/74. Preserve each gate's XML outside the report directory before the next command overwrites evidence, under the future Controller-designated evidence root; do not let focused reports masquerade as the final clean inventory.

Repeat concurrency/fault methods in three independent Maven processes after normal compilation; include observer-proven blocking and loser rollback. This is supplemental reliability evidence, not extra source identities. Missing authority, audit failure at each owner stage, restore termination, read-only/wrong role, zero-row result, raw unique cause, timeout and revision overflow all need actual assertions, not only mocked method invocations.

### 9.3 Clean Full Sweep And Unmodified-Logic CI/XML Oracle

```bash
cd "$PA39_VALIDATION_ROOT/backend"
"${MVN[@]}" -Pcontainer-smoke clean verify && python3 - <<'PY'
from pathlib import Path
import textwrap
ci = Path('../.github/workflows/ci.yml').read_text()
marker = "          python3 - <<'PY'\n"
start = ci.index(marker) + len(marker)
end = ci.index('\n          PY', start)
exec(compile(textwrap.dedent(ci[start:end]), 'shipped-ci-inventory', 'exec'))
PY
```

Expected after implementation: all 104 Surefire XML suites/1,018 methods and all 40 Failsafe XML suites/666 methods; 144 distinct suite files and 1,684 exact distinct class#method identities; zero skipped/errors/failures; source=CI=POM=XML; source-line scan1685 with one historical comment. The success-gated same-shell chain is mandatory. Do not edit the extracted oracle, use a softer parallel inventory, or run it against reports from separate focused executions. Record command, exit, class/method inventories and raw report-file hashes. Preserve the entire full report set before any further Maven invocation. Final POM include-set check and independent regex/source scan must agree with §8.

### 9.4 JAR, Contract And Frozen Checks

Run the shipped CI `default and opt-in web jar smokes` and `production jar dependency firewall` heredocs against the same full-build artifact without weakening them. Extract their exact Python bodies by step name and run from backend (the JAR smoke's delimiter is PYCODE). Default JAR remains non-servlet/database-free; opt-in web without production adapters must fail closed. Assert no packaged fixture/verifier/credential/fault configuration, no duplicate JAR entries, exactly 18 migration entries, same dependency allowlist and framework boundary. New POST/PUT route smoke cases must not expose an authenticated success with a fake provider. README cannot imply a deployable credential adapter exists.

Exact executable extraction for the JAR smoke (from projection/backend, after the successful full build and report custody):

```bash
python3 - <<'PY'
from pathlib import Path
import textwrap
ci = Path('../.github/workflows/ci.yml').read_text()
start = ci.index('      - name: default and opt-in web jar smokes\n')
marker = "          python3 - <<'PYCODE'\n"
start = ci.index(marker, start) + len(marker)
end = ci.index('\n          PYCODE', start)
exec(compile(textwrap.dedent(ci[start:end]), 'shipped-jar-smoke', 'exec'))
PY
```

For the dependency firewall, run its complete shipped CI run block, including its pre-Python dependency-tree/JAR-index commands, not only its heredoc. The following step-name extractor executes precisely that shell block without copying or weakening its assertions:

```bash
python3 - <<'PY'
from pathlib import Path
import subprocess
import textwrap
ci = Path('../.github/workflows/ci.yml').read_text()
start = ci.index('      - name: production jar dependency firewall\n')
start = ci.index('        run: |\n', start) + len('        run: |\n')
tail = ci[start:]
lines = []
for line in tail.splitlines(keepends=True):
    if line.strip() and not line.startswith('          '):
        break
    lines.append(line)
subprocess.run(['bash', '-euo', 'pipefail', '-c', textwrap.dedent(''.join(lines))], check=True)
PY
```

Contract checks in the validation projection only, using already provisioned locked dependencies, with **no install** in this task:

```bash
cd "$PA39_VALIDATION_ROOT/api/openapi"
npm run check:s3
```

Then run unchanged CI protocol/security/deployment/acceptance blocks by exact step name, from the projection root. The extractor deliberately selects only validation steps, never setup/install steps:

```bash
cd "$PA39_VALIDATION_ROOT"
python3 - <<'PY'
from pathlib import Path
import subprocess
import textwrap
ci = Path('.github/workflows/ci.yml').read_text()
job = ci[ci.index('  contract-regressions:\n'):]
for name in ('A2 s3', 'A3 s3', 'A4 s2', 'A5 s1', 'A5 s2'):
    start = job.index('      - name: ' + name + '\n')
    start = job.index('        run: |\n', start) + len('        run: |\n')
    lines = []
    for line in job[start:].splitlines(keepends=True):
        if line.strip() and not line.startswith('          '):
            break
        lines.append(line)
    subprocess.run(['bash', '-euo', 'pipefail', '-c', textwrap.dedent(''.join(lines))], check=True)
PY
```

These commands use the projection's generated A1 bundle and cannot write the primary repository's tmp. If the locked toolchain is absent, record an environment block for Controller rather than installing or weakening checks. OpenAPI and docs12/14/15/17 raw byte hashes must still equal §10; migrations exactly the pinned 18, no V019. Contract-green does not substitute for live PostgreSQL tests.

## 10. Frozen Byte Pins

Raw bytes in this Windows-materialized checkout are pinned below. LF-normalized aggregate checks are separate and must not replace raw equality on untouched primary files. Migration filenames are part of the set; omission/rename/addition fails.

```text
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db  docs/12-technology-stack-and-system-architecture.md
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205  docs/17-production-security-design-spec.md
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
c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc  backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql
a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c  backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql
74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3  backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql
ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c  backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql
```

Controller-owned Control raw SHA256 at inspection: `623ab15ce0ed3ee4d64fcbcf1fede27b69ec283ad562e6bf1fcc24012f9d3fd4`. This is a custody record, not permission to pin/update Control inside product tests. Existing PA25 LF hash guard remains `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`; do not edit the provisioning script.

## 11. Goal Alignment, Review Protocol And Residual Owners

| Decision / validation | Binding goal advanced; overdesign rejected |
| --- | --- |
| One S1 create+replace lifecycle | Shared canonical identity/replay and owner transaction; no mechanical web/SQL slices |
| Existing Operations named interface and employee authority | Current ACTIVE ADMIN, no reverse owner dependency, second credential or invented role |
| Exact new admission plus Customer pass-through | Only two routes work end-to-end; preserve concealment/authentication and all older flows |
| Single redacted P and explicit response mapping | One commercial truth for snapshot/hash/replay/response; no generic JSON framework or client metadata authority |
| Product publication lock reused | Prevent stale PA38 publication read/audit; no independent lock registry or new database facility |
| Conditional update and bounded read-only recovery | Exact 201/200/409/404/422 semantics, no duplicate rows/facts or blind write retries |
| Immutable children with token-only regeneration | Matches append-only constraints and frozen replacement; no channel administration/delete |
| Same-connection audit and restoration tests | Atomic domain/audit commit, real employee/trace, no dirty pooled role or partial writes |
| Exact test identities/manifest/POM/CI/XML | Measurable cumulative regression and architecture preservation, no count-only or stale-report green claim |
| Focused + PG16 + clean full + JAR/contract gates | Behavior, privileges, races, packaging and frozen contract each independently evidenced |
| README + implementation evidence only | Document shipped bounded capability; no semantic docs12/14/15/17/OpenAPI/Control edit |

First-principles judgment: use one owner command pair, one projection and one repository, not generic catalog CRUD, an approval engine, event-sourced draft store, platform idempotency integration or a new credential topology. Some exact transaction-witness mechanics duplicate the adjacent PA38 adapter intentionally to avoid an unrelated security-sensitive shared-framework refactor. Existing broad catalog runtime privileges are a known substrate, not a reason to add grants or weaken role tests. Empty arrays and arbitrary positive valid subscription intervals remain allowed because neither frozen schema nor database mandates stronger commercial policy.

Independent plan reviewer must be fresh and peer-isolated, inspect the same dispatched source/tree and this artifact without relying on planner confidence, independently reconstruct effective ACL and cumulative identities, and challenge projection/replay/lock/error precedence. Specifically test create→replace→create retry, same-content replacement, successor versus older replay, publication in both lock orders, immutable-field syntax versus semantic conflict, raw uniqueness after rollback, missing employee during recovery, and audit/role-restoration failure after owner mutation. Review every proposed production/test path and exact count mirror. Submit review before seeing any peer conclusion; Controller adjudicates. This plan does not spawn reviewers, request review externally, approve itself or enter implementation.

Residual risks and owners:

- **Backend/Database/Security (implementation acceptance):** prove exact role membership, writable same connection, real PostgreSQL locks/constraints, token/version overflow and transaction/audit failure paths. Source proof does not replace live tests; failure blocks acceptance.
- **Platform/Controller (environment):** current WSL launcher vsock and Docker daemon permission limitations; resolve before runtime gates without dependency installation or credential disclosure. Not a blocking scope question.
- **Backend test-boundary/Controller (inherited):** PA35 raw-CRLF harness issue remains deferred, with bounded canonical LF projection validation; no primary frozen file or guard weakening.
- **Database/Security (deferred):** pre-existing catalog role's broad SELECT/INSERT and lifecycle UPDATE privileges; PA39 application is narrower, grants remain unchanged. Future privilege redesign requires a separately confirmed unit.
- **Catalog/Data owner (operational data):** legacy/manual never-published snapshots may not match PA39 P. No silent backfill/repair or assumption that PA38 test fixtures are production-shaped; sanitized mismatch/integrity behavior and golden tests are required.
- **Security/Backend/Operations (deployment):** production employee verifier/credential wiring, UI second confirmation and real secret provisioning are not delivered by this slice. Default runtime remains fail-closed; test sessions are not production authentication.
- **Contract/Business owner (future policy):** no added minimum benefits/channels, tighter subscription policy, product creation or independent offer lifecycle. Any such need returns to goal confirmation.

Blocking open questions: **none**. V001–V018 suffice under the confirmed contract. A contrary implementation finding must stop for Controller, not mutate frozen inputs.

Actually performed during planning: read-only file/schema/ACL/code/guard/CI inspection; verified mapped worktree identity and read-only Git HEAD/tree/branch/status/log; regex-counted all source identities and annotation lines; reconstructed complete CI literal maps and compared every class/method set to source; measured production/test files and physical LOC; recomputed governed manifest count/digest; hashed frozen inputs; inspected exact POM includes; diagnosed PA35 CRLF bytes; non-mutating Java/Maven/Docker availability diagnostics. No Maven tests/build, PostgreSQL container, full CI/XML oracle, JAR smoke, npm/contract generation, dependency install, source/config/test/frozen-file edit or remote action was performed.

Stop after writing and statically auditing this single plan artifact. Exact next gate: **independent peer-isolated plan review**.
