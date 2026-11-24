# PA41 customer order creation foundation — PERSONAL-only implementation plan

## 1. Authority, goal, and decision

- Task: `pa41-personal-order-plan-gpt-20261124-02`; planning date: 2026-11-24.
- Bounded correction task: `pa41-personal-order-plan-fix-gpt-retry-20261124-211647`; accepted findings A-01–A-05 and B-1–B-7 are incorporated below. This is the one permitted fresh same-provider retry; it changes this plan only.
- Final bounded correction task: `pa41-personal-order-plan-fix2-gpt-20261124-215159`; Controller-accepted A-RR-01 and B-RR-01–B-RR-03 are incorporated without changing the earlier accepted corrections, slice, allowlist or test identities.
- Binding HEAD: `e01cad94b466d408021fcd399885322a01a550e5`; merged predecessor: `ea8247b106a6fe80ea5da052eca351ae1d3049a5`.
- HEAD was verified read-only with `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call19 --work-tree="$PWD" rev-parse HEAD`. The checkout's Windows `.git` pointer is not directly Linux-resolvable; do not repair it or alter Git metadata.
- Binding decision: Control `docs/gateflow/project-execution-control.md:18` and `:33`, and the user's 2026-11-24 B1 resolution, authorize successful **PERSONAL payable offers only**. B1 is resolved, not an open question. The previous stop artifact is superseded by this plan.
- Judgment: **the PERSONAL-only unit is implementable using existing tables, grants, LO credentials, pool and transaction manager; no DB/schema/topology prerequisite is necessary.** Runtime evidence remains an implementation acceptance requirement, not a claim that runtime tests ran during planning.
- Exactly **one end-to-end behavioral slice**, described below. This artifact is ready for bounded code generation after the controlling gate accepts it; it does not itself enter implementation or a later gate.

Motivation: establish the server-authoritative order/consent/idempotency foundation before payment. Success means an authenticated customer HTTP request creates exactly one immutable, correctly priced `UNPAID` PERSONAL order, one ACCEPT consent, one completed PURCHASE idempotency result, two Outbox facts and two audit facts in one local transaction; retry returns that original order with 200, semantic change conflicts, and every denial/failure has zero partial writes. ENTERPRISE/QUOTE success is prohibited and its rejection must be proven with real PostgreSQL16 zero-delta assertions.

Non-goals: payment provider/API/event/confirmation/payment_order creation, entitlement issuance/grants/balance changes, order GET/list, cancellation/refund, scheduler, frontend, catalog mutation, demo catalog approval, enterprise payable design, new generic framework, unrelated cleanup or hardening. No new pool, login, credential, role, membership, schema, table, constraint, SQL function, grant, migration or resource. Keep V001–V019, PA25, OpenAPI, docs12/14/15/17 and existing customer behavior unchanged. Never stage/commit/push, mutate PRs, comment externally, approve, merge, or advance gates under this task.

## 2. Evidence and goal-aligned decisions

References below use actual repository paths and 1-based source lines, not assumed capabilities inferred from bean names.

| Evidence | Finding / decision / goal alignment |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml:705`, `:748`, `:765`, `:5373`, `:5411`, `:5462` | One POST, exact ACCEPT-only request, 201 first result / 200 replay, frozen error set and response projections. Do not expose an enterprise success path because of the known description mismatch at :711. |
| `docs/10-cross-channel-rules.md:65`, `:69`; `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql:86` | ENTERPRISE iff QUOTE, null price iff QUOTE; PERSONAL accepts only SUBSCRIPTION/ONE_TIME/ADD_ON. Reject, never coerce null to zero. |
| `docs/12-technology-stack-and-system-architecture.md:56`; `docs/15-production-openapi-surface-spec.md:55`, `:117`, `:243`, `:277` | Explicit jOOQ/Flyway/Hikari topology, service ownership and exact frozen checkout contract; no client price/currency or additional public code. |
| `docs/14-production-data-model-and-flyway-schema-spec.md:269`, `:3053`, `:3079`, `:3338`, `:3717`, `:3812`, `:3921`, `:3938`, `:4009` | Canonical hashes, lock ranks, same-transaction owner consent, exact grants, semantic fields, Outbox registry and complete historical schedule. These are implementation requirements, not suggestions. |
| `docs/17-production-security-design-spec.md:15`; `backend/ops/postgresql/pa25-runtime-identities.sql:4064`, `:4296` | Four external NOINHERIT logins and eleven exact SET edges. LO may SET catalog_order_role and activation_consent_role; LC cannot. Retain this exact topology. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/security/CustomerSessionVerifier.java:1`; `internal/application/TrustedCustomerCaller.java:1` under that module; `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java:84`, `:110`, `:123` | Verifier supplies account/channel/selected workspace; filter currently does not admit orders. Extend only literal POST orders admission and its own header/body rules; preserve other endpoints' 128-character keys. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationsController.java:1`; `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectActivationCommandService.java:15` | Reuse strict manual JSON binding, trusted Principal, lazy provider failure behavior, current-context checks, typed no-store problems. Customer adapter owns no SQL/transaction. |
| `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java:49`, `:128`; `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/JooqCustomerContextRepository.java:1` | Customer LC query pool uses read-only REPEATABLE_READ transactions. Its context result is not transactional write authorization. Do not reuse that transaction for checkout. |
| `backend/src/main/java/com/onecall/core/subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java:20`; `backend/src/main/java/com/onecall/core/activation_consent/internal/command/DefaultCustomerSubjectActivationService.java:67` | Existing personal-vs-organization owner predicates and lifecycle advisory namespaces. Checkout re-reads exact permitted columns on its write connection rather than calling independently pooled readers. |
| `backend/src/main/java/com/onecall/core/catalog_order/internal/customercatalogquery/JooqCustomerCatalogOfferReader.java:1`; `backend/src/main/java/com/onecall/core/catalog_order/internal/customerquery/JooqCustomerSubjectOrderCountReader.java:1` | Existing catalog reader deliberately lacks snapshot/line identity and cannot be checkout authority; do not widen its read projection or V019 ACLs. Existing order count is not an order command. |
| `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java:34`, `:58`, `:105`; `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java:15` | Existing same-connection transaction witness, literal SET LOCAL ROLE and restoration pattern; publication/offsale use `catalog_order.publication:<productId>`. Checkout must share that lock, not invent an isolated catalog namespace. |
| `backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java:69`, `:116`, `:125`, `:145`, `:172` | LO-backed guarded pool, existing writable catalog-sale transaction manager/template, transaction-aware jOOQ and connection cleanup/eviction. Reuse the writable template, not the read-only Operations template. |
| `backend/src/main/java/com/onecall/core/activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java:30`, `:48`; `backend/src/main/java/com/onecall/core/entitlement_metering/internal/activation/DefaultActivationTrialService.java:30` | Activation already owns a separate pool/REQUIRES_NEW boundary. Calling that public activation command would split atomicity and issue a trial. Reuse only the MANDATORY owner-handoff design, on LO's manager/DSL. |
| `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql:28`, `:41`, `:433`, `:459`, `:477`; `backend/src/main/resources/db/migration/V007__activation_and_consent.sql:223` | Catalog role already has owned order insert/read, platform/audit functions, exact account/identity/organization/membership/subject/activation/consent SELECT. Consent column grant includes source_ref but excludes snapshot; owner must return content validation facts. No foreign row lock/write. |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql:100`, `:173`, `:285`, `:439`; `backend/src/main/resources/db/migration/V007__activation_and_consent.sql:311` | Actual V001 filename differs from the shorthand in the task. Begin/complete/Outbox/audit and order-consent FK already exist; no replacement tables/functions. |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersFixture.java:61`, `:78`; `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java:35`, `:49` | Reuse PostgreSQL16/Flyway19/actual PA25 apply+check and SQL listener fault/witness testing patterns. Do not copy older fixtures' role-revocation shortcuts into the PA41 exact-topology fixture. |

### 2.1 First-principles topology conclusion

LC is provisioned with only customer_iam_role; LW with sharing_command_role/evidence_role. Neither can perform catalog order plus owner consent writes. The activation pool can be supplied a login by properties, but it is a **different DataSource and transaction manager**: matching credential strings cannot join transactions or connections. A call across these pools is forbidden even if role membership happens to permit it.

LO has no inherited object authority before SET, but the exact superuser-granted edges give it SET TRUE/INHERIT FALSE/ADMIN FALSE for operations_iam_role, subject_number_role, activation_consent_role, entitlement_metering_role, evidence_role, catalog_order_role and ivr_role. Only catalog_order_role and activation_consent_role are used here. The exact grantor, attributes, eleven edges and PA25 432-tuple oracle are unchanged. Naming the existing pool “Operations reader” does not make it read-only: the code already defines a separate writable READ_COMMITTED catalog-sale template on it. Conversely the default read template is unusable for checkout.

Preferred runtime reuse: inject `operationsSubjectReadCatalogSaleTransactionTemplate`, its existing `operationsSubjectReadCatalogSaleTransactionManager`, `operationsSubjectReadDataSource`, and `operationsSubjectReadDsl` by literal qualifiers in the two new owner configuration classes. Assert the template references that manager and manager references that same DataSource; writable, REQUIRES_NEW, READ_COMMITTED, timeout 5 seconds. Existing guarded template performs its existing clean-connection check after completion. No new transaction manager, DataSource, role enum, operations session or employee authority is introduced. Enabling this feature requires the already existing `persistence` profile and `onecall.operations-subject-read.enabled=true`; missing dependencies produce sanitized 503 lazily, not an unsafe fallback. Feature beans must not interfere with older web-only contexts.

This is the **shared fixed four-connection LO pool**, with a **3000ms connection-acquisition budget**, not reserved checkout capacity. Existing Operations traffic competes with checkout; acquisition exhaustion maps to 503 SERVICE_UNAVAILABLE with no transaction/effects, never 409 or 429. Do not resize the pool, add credentials or change the existing manager/template. Pool wait can precede the five-second transaction budget; do not describe five seconds as an end-to-end SLA. Capacity sizing/contention acceptance belongs to Platform/Operations + capacity/docs19 owners as an owned residual (§10.2).

At entry record connection object identity plus `(session_user, pg_backend_pid(), txid_current())`, requiring `current_user=session_user`, active transaction, writable connection and a bound ConnectionHolder for the exact DataSource. Every owner handoff asserts that witness and the expected current role. Owner collaborator uses MANDATORY with **the same existing manager** and transaction-aware DSL. It accepts no DataSource/DSL/raw connection from the HTTP caller. It never invokes the separate activation pool or trial service.

Successful role sequence: LO login → catalog_order_role → activation_consent_role (lock) → catalog_order_role → activation_consent_role (consent read/append) → catalog_order_role → LO login. Use literal `SET LOCAL ROLE` statements, finally restoration and verified witness after each switch; `SET LOCAL ROLE NONE` only at outer exit. No dynamic role interpolation, SET ROLE, RESET ROLE, manual commit/rollback, savepoint or REQUIRES_NEW owner collaborator. On SQL-aborted transactions restoration SQL can fail: preserve the primary failure (suppressed sanitized cleanup failure), propagate out, force rollback, then rely on transaction-local reset plus guarded pool cleanup/eviction. Never mask it as success. Non-SQL restoration failure also rolls back. Tests must prove pooled reuse baseline after both paths and connection termination.

## 3. Exact public behavior and precedence

Only `POST /api/v1/customer/orders`, operationId `createCustomerOrder`. Existing strict URI/method authentication boundary applies; no encoded/semicolon/trailing-slash alias and no GET. JSON max 8192 UTF-8 bytes; exactly one application/json Content-Type, existing Accept handling, no cookies/session or trusted headers. Strict binder rejects duplicate JSON properties, unknown fields, trailing tokens, malformed UTF-8, non-object/null body, coercion and malformed UUIDs. UUID input parses to canonical lowercase text for hashing. Channel must be MINIAPP or WEB and exactly equal verifier-attested channel.

Request fields: required `subjectId`, `catalogOfferId`, `channel`, `decision`, `policyCode`, `policyVersion`, `promptVersion`, `contentSha256`; optional `expectedVersionToken` absent only (explicit null rejected). `decision` must be `ACCEPT`; policy/prompt strings nonblank length 1–64; content digest exactly lowercase 64 hex; expected token nonblank length 1–128, exact case-sensitive comparison. No trimming, Unicode normalization or case-folding of semantic strings. Reject client amount, price, currency, paymentStatus, issuanceStatus, snapshot, account/workspace/role, catalogVersionId, purchaseConsentId/eventId or any extra field.

Exactly one Idempotency-Key header value: nonblank, valid UTF-8, no NUL, 1–475 Unicode code points (PostgreSQL character length). The server-received header string exposed by the HTTP stack is authoritative: the application neither trims nor normalizes it; preserve any leading/trailing OWS still present in that string and a comma in a single value. This does not promise that raw wire OWS survives proxy/container parsing, nor that wire variants which the stack exposes identically have distinct identities. This opaque header is not parsed as a comma-separated list. Reject multiple header occurrences. All uses of exact/original client key below mean this same server-received string; HTTP tests assert the actually received value, while direct identity tests may supply distinct surrounding-space strings. Header constraints do not change existing endpoint key limits.

### 3.1 Ordered decisions (first applicable wins)

1. Non-admitted URI/method: existing not-found behavior, not a new order problem. Admitted route missing/invalid/repeated bearer → 401 UNAUTHENTICATED. Missing/inconsistent verifier context or unavailable/ambiguous required owner bean → 503 SERVICE_UNAVAILABLE. Then header/media/body structural validation → 422 VALIDATION_FAILED. Thus missing `decision`/policy fields and HTTP DECLINE are structural 422; PURCHASE_CONSENT_DECLINED applies to owner-side current consent refusal, not a widened request enum.
2. Inside transaction take authority locks as §5. Read current account/workspace/subject owner. Missing/inactive account, missing/disabled organization or membership, unsupported current role, wrong workspace/subject owner, nonexistent subject, or unowned/dual-owner subject → 403 FORBIDDEN (avoid existence leakage; malformed persisted dual owner is INTERNAL if the DB invariant has actually been violated). PERSONAL workspace requires subject.owner_account_id=caller.accountId and null organization owner; ORGANIZATION workspace requires matching owner_organization_id and null personal owner, ACTIVE membership role in ADMINISTRATOR/MEMBER/EVIDENCE_MANAGER/AUDITOR as existing activation behavior. PERSONAL **catalog audience** is not a synonym for PERSONAL **workspace**: do not add an undocumented workspace restriction.
3. Ordinary client means PERSONAL workspace: missing/non-VERIFIED identity_profile.real_name_status → 422 REAL_NAME_REQUIRED. Organization workspace uses current organization authority; do not invent enterprise KYC. Then missing/non-ACTIVE activation → 403 ACTIVATION_NOT_ACTIVE. These checks precede any replay, so revoked authority cannot retrieve an old result.
4. Resolve current offer/version/product and take publication lock. Missing offer/version, offer channel mismatch, non-ACTIVE offer/version, audience other than PERSONAL, QUOTE/unsupported type, null/negative/non-integral price, non-CNY currency, invalid billing matrix or expectedVersionToken unequal current exact token → 422 VALIDATION_FAILED. This is the frozen general invalid-checkout-input problem; do not add CATALOG_NOT_PAYABLE, NOT_FOUND, QUOTE_ONLY or stale-token codes. ENTERPRISE+QUOTE is always rejected here even with valid consent or an existing key. No idempotency row has yet been created. Nonpayable checks precede UNKNOWN and consent to prevent misleading commercial eligibility.
5. Read own existing order by `(account_id,idempotency_key)` without writing; validate stored snapshot/hash/projection/reference integrity. Construct incoming order fingerprint from current validated catalog facts (§4). Call `idempotency_begin` at rank 10. CONFLICT → 409 IDEMPOTENCY_CONFLICT before UNKNOWN/new consent. For an existing completed order, compare consent request semantics using the owner read-only replay validator and content digest from its immutable snapshot. Changed policy/prompt/action/content, subject/channel/workspace or incompatible optional expected token → 409 IDEMPOTENCY_CONFLICT. `expectedVersionToken` absent versus present with the same accepted token is equivalent. The frozen fingerprint field lists are not extended to disguise this additional semantic comparison.
6. REPLAY: verify result_type=ORDER, exact orderId/consent/account/subject/offer, result hash equals order snapshot hash and stored redacted result matches the immutable original response. Return first order 200, no new consent/idempotency completion/events/audit. Catalog must still satisfy step 4: an offsale replay fails 422, a current changed token with omitted expected token conflicts through the current PURCHASE fingerprint; no stale purchase authorization is revived. A same-subject/offer UNKNOWN order does not block an already completed identical creation replay; UNKNOWN prevents **new checkout**, not returning the first creation result. The creation result remains its original UNPAID/NONE projection even if later payment changed the mutable row; never synthesize a new result from current payment state.
7. NEW only: any existing same subject/offer order with payment_status=UNKNOWN (across accounts/workspaces where visible through catalog ownership, not merely same key) → 409 ORDER_UNKNOWN_PAYMENT_PENDING. Otherwise validate approved complete snapshot/schedule; absent/unapproved commercial terms/schedule → 422 VALIDATION_FAILED; internally contradictory hash/row/snapshot facts → 500 INTERNAL. No calendar/pricing guess. Then owner purchase policy missing or invalid → 409 PURCHASE_CONSENT_REQUIRED; policy/version/prompt/content mismatch → 409 PURCHASE_CONSENT_STALE; existing event with DECLINE → 409 PURCHASE_CONSENT_DECLINED; wrong subject/account/channel/purpose/source, already consumed by a different order or mismatched owner consent → 409 PURCHASE_CONSENT_REQUIRED. A changed semantic event fingerprint → 409 IDEMPOTENCY_CONFLICT. All throw and roll back NEW idempotency.
8. Dependency/connection/timeouts/42501/witness/role restoration/configuration failures → 503 SERVICE_UNAVAILABLE; corrupt immutable stored facts and unexpected programming errors → 500 INTERNAL. Pre-commit faults roll back; a post-commit guarded `assertClean`/close failure may instead return sanitized503 after the whole bundle committed, an ambiguous/lost-201 outcome, not a rollback. Exact platform/constraint backstops are specified in §3.3. No SQL text, SQLState, credentials, content or exception message in the public problem. No automatic retry loop: same-key caller retry is safe subject to current eligibility and the lost-201 limitation below; timeout cannot be misreported as conflict. 429 RATE_LIMITED remains the existing boundary/infrastructure response, not a new limiter.

Problems use existing `{type:"about:blank",title,status,code,traceId}` and headers no-store/Pragma/no-sniff; 401 includes WWW-Authenticate. Add an OrderCreationException.Code writer overload and explicit titles “Idempotency conflict”, “Unknown payment pending”, “Purchase consent required”, “Purchase consent stale”, “Purchase consent declined”, “Service activation not active”, “Real-name verification required”, preserving common titles. No additional public fields or code registry changes.

### 3.2 Result

201 first creation, 200 replay. Return exactly required Order fields `orderId,orderNo,subjectId,channel,catalogVersionId,catalogOfferId,amountCents,currency,paymentStatus,issuanceStatus,createdAt,snapshotSha256,snapshot,purchaseConsentId`; omit optional paidAt/closedAt for this creation result. paymentStatus=UNPAID and issuanceStatus=NONE (computed from no issuance; no stored issuance column or entitlement query). `orderNo="OC-" + lowercase UUID-without-hyphens(orderId)` (35 characters), order UUID generated once for NEW; createdAt is database transaction time, RFC3339 UTC microsecond precision. No Location/GET implementation.

Snapshot **projection**, distinct from full stored snapshot, has exactly `schemaVersion,catalogVersionId,catalogOfferId,channel,versionToken,productCode,offerType,audience,amountCents,currency,billingCycle,installments,benefitLines,snapshotSha256`; benefitLines use frozen CatalogBenefitLine fields only (`entitlementType,quantity,validityDays,grantIntervalMonths`). Map stored priceCents to amountCents; do not expose terms/schedule/lineKey/versionNo outside the frozen projection. Both top-level and projection hash refer to the complete stored snapshot, not a self-referential projection hash.

### 3.3 Internal function/constraint failures: exact existing public outcomes

Translate only allowlisted internal function tokens (P0001) or structured SQLState + exact frozen constraint identity at the owning repository boundary, never arbitrary substring text. Always propagate out of the transaction before forming a public problem; no query on an aborted connection, savepoint recovery, partial commit or invented public code. Client invalidity is handled before SQL; server-derived invalid arguments are not blamed on the client.

| Internal outcome | Exact public outcome / proof |
| --- | --- |
| `IDEMPOTENCY_INCOMPLETE` from begin only | 503 SERVICE_UNAVAILABLE; no usable completed result, roll back all tentative effects. A durable NEW/incomplete fixture remains unchanged; do not recover by creating another order/key. V001 complete does not raise this token. |
| `IDEMPOTENCY_ARGUMENT_INVALID` from begin or complete | 500 INTERNAL; validated, server-derived function arguments violated an invariant. Invalid HTTP keys/body remain pre-SQL 422 VALIDATION_FAILED. |
| `IDEMPOTENCY_NOT_FOUND` from complete | 500 INTERNAL; the server's NEW record/revision association is invalid; roll back the whole tentative bundle. |
| `IDEMPOTENCY_CONFLICT` P0001 from complete | 500 INTERNAL; completion integrity/revision/result disagreement, not a client conflict. Distinct from begin's CONFLICT return tuple and the exact 23505 replay backstops below, which map to409. |
| `OUTBOX_PAYLOAD_INVALID` | 500 INTERNAL; owner-generated fact is invalid, entire order/consent/completion/facts roll back. |
| `OUTBOX_DEDUPE_CONFLICT` | 500 INTERNAL; contradictory owner fact is not a client idempotency conflict; entire bundle rolls back. |
| `AUDIT_ARGUMENT_INVALID` from append_fact | 500 INTERNAL; owner-generated audit arguments violated an invariant; entire tentative bundle rolls back. |
| `AUDIT_REASON_REQUIRED` from append_fact | 500 INTERNAL; server actor/action/reason disagreement, not a missing client field; entire tentative bundle rolls back. |
| `AUDIT_APPEND_ONLY` trigger, if reachable at an actual call site | 500 INTERNAL; forbidden audit mutation, never409. V001 append_fact only inserts and cannot reach its UPDATE/DELETE trigger; prove this call-site non-reachability rather than adding an audit mutation path. |
| 23505 `uq_customer_order_idempotency`, `uq_consent_event` | 409 IDEMPOTENCY_CONFLICT; defensive backstop only, do not return unverified replay or raw database errors. Normal serialization must produce 200 for identical completed replay without hitting this exception. |
| Other 23505 (generated order ID/no, generated consent ID, platform/fact identities escaping the functions' own typed handling) | 500 INTERNAL; generated identity/invariant collision, no automatic regenerated-ID retry. |
| 23514 check/trigger validation and 23503 order/account/subject/catalog/consent foreign-key backstops | 500 INTERNAL after successful domain validation: impossible server tuple or stored integrity/race failure. Known inactive/missing catalog and authority/consent denials retain their earlier §3.1 422/403/409 outcomes; do not blanket-map raw SQLStates to those client codes. |
| 55P03 lock timeout, 57014 statement timeout, 40P01 deadlock, 42501 privilege failure, pre-commit connection/acquisition/restoration/cleanup failure | 503 SERVICE_UNAVAILABLE, never IDEMPOTENCY_CONFLICT; rollback/no transaction as applicable. |
| Post-commit guarded assertClean/connection-close failure | Sanitized503 SERVICE_UNAVAILABLE may replace201 after a confirmed commit; exactly one complete bundle remains, no rollback claim. Apply §5 lost-201 retry precedence and the §8 post-commit oracle. |

Repository tests and real PostgreSQL fault/constraint probes must cover each row, assert exact frozen status/code/redaction and the §8 zero-delta oracle for pre-commit faults (the separate complete-bundle oracle for post-commit faults). Within existing `injectedFailureAtEveryWriteStageLeavesZeroPartialRows` and `sqlAbortAndRestorationFaultRollbackAndCleanPool` identities, require a one-to-one labelled `(call site, P0001 token)` probe set: begin/IDEMPOTENCY_ARGUMENT_INVALID, begin/IDEMPOTENCY_INCOMPLETE, complete/IDEMPOTENCY_ARGUMENT_INVALID, complete/IDEMPOTENCY_NOT_FOUND, complete/IDEMPOTENCY_CONFLICT, Outbox/OUTBOX_PAYLOAD_INVALID, Outbox/OUTBOX_DEDUPE_CONFLICT, audit/AUDIT_ARGUMENT_INVALID and audit/AUDIT_REASON_REQUIRED; each must assert its own exact outcome, rollback and redaction, not a generic catch-all loop assertion. Assert AUDIT_APPEND_ONLY non-reachability at append_fact, or include its own500 probe if actually reachable. Include function-token injections at the real transaction's begin/complete/fact call sites; use lawful duplicate/FK/check probes or transaction-local injected invalid arguments, never disable constraints or persist impossible fixtures. Unknown tokens/constraints/errors default to sanitized 500 INTERNAL unless an existing dependency/connection failure above applies. No new public code or test identity.

Expand the Outbox/audit token pairs across both actual consent-owner and order-owner fact call sites within those same identities; each injected site/token has its own expected500/rollback/redaction assertion. The begin CONFLICT return tuple and23505 backstops retain separate409 probes and must not substitute for complete's P0001 IDEMPOTENCY_CONFLICT500 probe.

## 4. Immutable identities, consent, pricing and snapshots

Use docs14 §2.5 RFC8785-compatible canonical JSON, sorted UTF-16 object keys, compact UTF-8, lowercase SHA256; arrays retain their explicitly defined order. Domain money/quantity fields are integer values, not decimal strings/floats. Reject unsupported numbers rather than silently rounding; use checked 64-bit integers and reject values outside exact interoperable JSON integer representation if they cannot be faithfully canonicalized. No new canonicalization library/framework; implement the bounded integral/string/boolean/null/object/array shapes in the two owner semantics classes using existing Jackson patterns, with independent byte vectors. Preserve null ownerOrganizationId in canonical objects.

`terms` is a required JSON **object** copied from the approved version snapshot, not arbitrary Jackson values. Its bounded domain: objects with unique string keys, arrays in source order, strings, booleans, null and integral JSON number tokens in [-9007199254740991,9007199254740991]. Root depth=1, maximum depth=8; count every value/container as one node, maximum 256 nodes; each key/string at most 1024 Unicode code points; canonical terms UTF-8 at most 8192 bytes. Strings/keys must be Unicode scalar sequences (no unpaired surrogate or U+0000), no normalization/case-folding. Reject duplicate keys before tree conversion, missing/root-null/non-object terms, fractional/exponent-form numeric tokens (including 1.0/1e0), unsafe/out-of-range numbers, NaN/Infinity, binary/POJO/missing Jackson nodes and exceeded bounds **before hashing** any snapshot containing those terms. Do not stringify, drop fields, replace values with null, round or infer terms. A missing/unsupported approved input is 422 VALIDATION_FAILED on NEW with zero delta; a structurally supported snapshot whose stored digest/row agreement is false is 500 INTERNAL. Unsupported/corrupt persisted original order or consent facts during replay are 500 INTERNAL (not a new client 422). Validate source numeric tokens before any tree coercion; exponent/decimal syntax irreversibly normalized by existing jsonb cannot be reconstructed and must not be claimed as proven by a database round-trip. The domain applies to the JSON text actually read from the approved stored snapshot, not unverifiable pre-jsonb lexical history. Terms are not a PURCHASE fingerprint field; keep complete current-source snapshot/terms eligibility validation NEW-only at §3.1 step7, without moving it ahead of replay.

These fixed terms bounds are plan-owned size policy, not proof that every future legitimately approved terms object fits. An approved-but-over-bound input still fails NEW with422 VALIDATION_FAILED and zero delta; no truncation or bypass. Catalog/Product/Finance/API owners must route a separately authorized size-policy/bound revision to the Q4/Q20 commerce-input/publication follow-up and a successor bounded checkout plan before production purchase content approval (§10.2). Observability is sanitized server log only: fixed reason enum `TERMS_DEPTH_LIMIT`, `TERMS_NODE_LIMIT`, `TERMS_TEXT_LIMIT` or `TERMS_UTF8_LIMIT`, the exceeded dimension and configured limit, and observed/bounded counts only. Never log terms keys/values/content or key/bearer; no diagnostic in public responses, audit payloads or a new contract field.

Fixed canonical terms vectors, UTF-8 without BOM/newline, must be pinned independently in `snapshotHashUsesCanonicalFullSnapshotNotProjection`: `{}`; `{"a":[null,true,false,-9007199254740991,9007199254740991],"z":"é"}` (reversed source object keys produce identical bytes/hash); and `{"a":1,"b":[2,1]}` (array reversal changes bytes/hash). Pin literal SHA256 values in implementation tests, independently recomputed from these bytes, not from the production canonicalizer. Extend `snapshotCopiesAllApprovedCommerceAndScheduleFields` with boundary/unsupported loops: depth8/9, nodes256/257, strings1024/1025, bytes8192/8193, integer limits/limits±1, decimal/exponent, null root, duplicates, U+0000/unpaired surrogate and unsupported node types; every rejected value maps as above. PostgreSQL IT covers stored representable unsupported shapes/values and zero delta; parser-only forms are tested before jsonb normalization. Include legitimately approved-but-over-bound terms in the existing semantics identity and `completeApprovedScheduleIsFrozenAndNeverComputed` IT, asserting422/zero delta and sanitized server-log-only diagnostics, never public/audit data; no new method/class.

| Fixed canonical terms bytes | SHA256 |
| --- | --- |
| `{}` | `44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a` |
| `{"a":[null,true,false,-9007199254740991,9007199254740991],"z":"é"}` | `625ae4c671502c27a5448d9c889ef115851e4cdc208dafbdfc0cfdf18c383d53` |
| `{"a":1,"b":[2,1]}` | `e9d26fb0100c3f9ef569c38ced9811ec35059c3c999b8683fde59ed24e6e66e8` |

`purchaseConsentEventId = 'purchase-consent:' + sha256(canonical({accountId: canonicalLowercaseAccountUUID, clientIdempotencyKey: exactKey}))`, exactly 81 characters, both consent event_id and source_ref. Platform scope PURCHASE; key `accountId + ':' + exactKey`, max 512. Consent event_domain=PURCHASE, purpose=PURCHASE, action=ACCEPT, source_type=ACCOUNT, source account=trusted account. Never truncate/randomize event identity or accept it from client.

Keep the two key stores distinct: `customer_order.idempotency_key` is **the exact original client key** (1–475 code points), and own lookup is `(account_id, exactClientKey)`; only `platform.idempotency_record.idempotency_key` stores `canonicalAccountId + ':' + exactClientKey` (38–512 code points within its 1–512 constraint). Neither store contains the consent event hash in place of its required key. Long-key IT asserts exact values in both rows, not merely their lengths.

Order fingerprint exact docs14 :3921 set: `accountId,amountCents,catalogOfferId,catalogVersionId,channel,currency,ownerOrganizationId,purchaseConsentEventId,subjectId,versionToken`. Consent fingerprint exact :3922 set: `accountId,action,channel,eventDomain,eventId,policyCode,policyVersion,promptVersion,purpose,sourceType,subjectId`. No generated IDs, transport traces, timestamps, mutable payment/issuance state, snapshot hash or extra policy/content fields in the order fingerprint. Compare policy/content separately as §3 requires; contentSha256 is bound in the consent snapshot and current policy validation, not silently added to the frozen fingerprint list.

Current purchase policy is activation-consent-owned validated deployment input, not the trial policy and not client authority. In new PurchaseConsentConfiguration declare nested immutable configuration-properties records for `onecall.purchase-consent.current` with exactly policyCode/policyVersion/promptVersion/contentSha256; absent/invalid identity yields PURCHASE_CONSENT_REQUIRED, not default/demo content. Owner takes one immutable configuration snapshot per invocation; tests bind approved synthetic values. No new database setting/table, policy publication API, remote fetch or production policy value. Updating deployed current policy changes only NEW eligibility; identical replay compares original consent, not a new policy identity. Production wording approval remains Q10 owner input, not a coding blocker or permission to guess.

Consent snapshot exact new bounded shape: `{schemaVersion:1,accountId,subjectId,purpose:"PURCHASE",channel,eventDomain:"PURCHASE",eventId,sourceType:"ACCOUNT",sourceRef:eventId,action:"ACCEPT",policyCode,policyVersion,promptVersion,contentSha256}`. Hash its canonical bytes. Owner reads/appends consent under activation_consent_role, validates event uniqueness/fingerprint/full snapshot hash/current approved identity, and returns immutable record ID plus validated descriptor. Catalog separately re-reads only the V007 :225 allowed consent columns (including source_ref, action and fingerprint) and matches them to that descriptor before order insert. It cannot SELECT consent.snapshot or write/lock foreign consent rows. Reuse of an existing unconsumed identical owner event is permitted; another order consuming it is denied. Under normal NEW key path append exactly once. Successful replay does not re-append or re-audit consent.

Price = current catalog_version.price_cents, not sum of benefits, demo numbers or a client value; non-null nonnegative integer is permitted (the frozen schema allows zero; do not add a >0 policy). Currency CNY. Validate PERSONAL matrix, exact current version/offer/channel/token and benefit rows against frozen source snapshot. Benefit rows have no ACTIVE column: eligibility is inherited from ACTIVE version and ACTIVE offer; do not invent a benefit lifecycle.

Full stored order snapshot is **all docs14 §10.19 fields**, not merely OpenAPI projection: schemaVersion=1, catalogVersionId, catalogOfferId, channel, versionToken, productCode, versionNo, offerType, audience, priceCents, currency, billingCycle, installments, benefitLines and immutable terms. Each line has lineKey=actual benefit row UUID, entitlementType, quantity, validityDays, grantIntervalMonths and schedule. Each schedule has exactly installmentNo/entitlementType/units/validFrom/expiresAt; one per installment in 1..installments, correct type/quantity, ordered installmentNo ascending, valid absolute UTC window with expiry>start. Sort lines by entitlementType then lineKey. Copy approved times/terms from immutable version snapshot; never calculate a future window or accept it from request. Verify catalog snapshot_sha256 before copying and exact agreement of every corresponding product/version/benefit fact. Add only offer/channel/token; hash complete canonical order snapshot before projecting.

**Existing data limitation explicitly resolved:** PA39 `CatalogDraftSemantics.projection` stores a smaller commercial representation without approved terms/schedule/lineKey; PA38 publication does not manufacture those inputs. Such a row cannot be made payable by fabricating fields here: NEW checkout fails VALIDATION_FAILED. The existing schema accepts the full approved §10.19 version snapshot without any DDL; lawful PostgreSQL fixtures seed that complete approved snapshot/rows through owner setup, preserving every constraint/trigger. This is enough to implement and prove the frozen PERSONAL operation without catalog mutation in PA41. Making the existing draft/publication UI produce approved full snapshots is a separate Catalog/Product/Finance Q4/Q20 work item, not an authorized PA41 expansion or a topology/schema blocker. Do not claim every historical ACTIVE catalog row becomes checkout-ready.

## 5. One atomic transaction and concurrency

Order service starts the existing LO write template once. All its work and owner MANDATORY calls remain on its one connection. **Choose bounded customer-write fail-fast behavior:** set LOCAL statement_timeout=2000ms, lock_timeout=250ms, idle_in_transaction_session_timeout=5000ms explicitly in checkout. The 250ms value previously describes Operations context behavior; checkout deliberately adopts it, not a claimed global manager default. Longer production contention may yield 503 for the losing request; 201/200 or 201/409 is guaranteed only when contention resolves inside the budgets. This preserves bounded shared-pool occupancy and existing Operations behavior without globally changing any timeout. No network call under locks or automatic retry. The transaction's five-second template budget and 3000ms pool acquisition budget remain distinct.

1. Enter catalog_order_role; rank 3 subject lifecycle advisory `subject_number.subject:<canonical subject UUID>` using pg_advisory_xact_lock(hashtextextended(key,0)).
2. Activation owner `lockServiceActivation` switches to activation_consent_role; rank 4 `SELECT id,subject_id,status,revision ... WHERE subject_id=? FOR UPDATE` on existing head only. Missing row creates nothing. Return LockedActivation descriptor with witness and subject; restore catalog role. Only this owner takes that row lock.
3. Rank 5 nonlocking exact subject owner/revision read. No number/binding/risk reads or arbitrary OPEN-risk veto: checkout has no approved claim/evidence-risk ban or needed canonical-number lock.
4. **Only ranks 7/8 are ORGANIZATION-only:** rank 7 organization lifecycle advisory then re-read id/status; rank 8 logical organization/account membership advisory then read exact organization_id/account_id/role/status non-REVOKED row. **Rank 9 account lifecycle advisory and subsequent account/status/permitted real-name re-read are unconditional for both PERSONAL and ORGANIZATION.** Use the existing lifecycle namespace/key, not a checkout-only lock. Re-read subject owner under held subject advisory; refuse changes. No foreign FOR SHARE/UPDATE. PERSONAL account-disable races must serialize on rank 9, not merely pass a stale nonlocking account check. The production account-disable writer is outside PA41; lawful fixtures emulate the documented customer-lifecycle owner writer by acquiring the same rank-9 account key before its status write and holding it through commit, without adding or claiming a production writer here.
5. Catalog preliminary own SELECT resolves offer→version→product; acquire existing `catalog_order.publication:<productId>` advisory **after rank 9 and before rank 10**; re-read all catalog facts after acquiring it. Existing catalog publication/draft paths never acquire customer/subject/activation locks, so adding this owner-local lock in this slot creates no reversed cross-flow edge. Hold it through commit; no need for owned catalog row locks. Forward invariant: any future path acquiring this publication advisory must preserve this rank-9→publication→rank-10 order and must never subsequently acquire ranks 3/4/7/8/9; no publication-held callback to account/subject/activation lifecycle. Static/SQL-order guards cover that no-reverse edge, with a separately owned docs14 lock-table amendment residual (§10.2); frozen docs14 is not edited here. Test both publication-wins and checkout-wins schedules with blocked-lock witnesses.
6. Own order/key read and current-catalog fingerprint. `platform.idempotency_begin('PURCHASE',accountId+':'+key,fingerprint)` acquires rank 10. Only NEW continues. Same key changed subject across transactions still serializes at platform unique row; no earlier rank may be acquired afterward. Check same subject/offer UNKNOWN for NEW under subject advisory. Catalog lifecycle can no longer change while pricing/consent/snapshot are frozen.
7. Owner consent MANDATORY append reuses LockedActivation; it asserts witness/current ACTIVE locked head but does not reacquire a new rank-4 lock. Validate consent as §4, insert order with the complete explicit tuple below. No omitted identity/hash/key column, foreign catalog DML or payment write.
8. `platform.idempotency_complete(recordId,revision,'ORDER',orderId,redactedOrderResult,snapshotSha256)` using exact NEW returned revision. The redacted result is the original full public Order creation result; its result_sha256 is the complete order snapshot hash as docs14 §9.17 specifies, not a recomputed hash of the public projection. Treat non-COMPLETED/invalid completion outcome as failure. Complete, order, consent and all event/audit effects remain tentative until final commit.
9. Rank 14 append both owner Outbox facts; then rank 15 append both audit facts. To preserve total order do **not** let consent append audit/Outbox before the order/idempotency phase. Owner `appendPurchaseConsent` returns its descriptor; a separate owner `appendPurchaseConsentFacts` MANDATORY method emits its own facts later. Consent Outbox PURCHASE / `consent_record:<consentId>` / CONSENT_RECORD / PURCHASE_CONSENT_RECORDED; order Outbox PURCHASE / `customer_order:<orderId>` / CUSTOMER_ORDER / ORDER_CREATED, exact docs14 :3938/:3939 payload fields. Owner fact method has phase enum OUTBOX or AUDIT so sequence is consent Outbox, order Outbox, consent audit, order audit without foreign writes. Audit uses ACCOUNT actor=trusted account, actions PURCHASE_CONSENT_RECORDED and ORDER_CREATED, object types CONSENT_RECORD/CUSTOMER_ORDER, before=null, after=corresponding redacted event payload, reason=null, request_id=null, trace_id=server trace UUID; neither key nor bearer logged/audited. Canonical payload hashes are independent from snapshot hashes.
10. Restore LO baseline inside the transaction callback's finally, then let `super.execute` commit once. Any pre-commit typed denial, complete mismatch, injected SQL/dependency/role-restoration/cleanup failure escapes the callback and rolls back everything, including SQL-abort/42501/55P03 cases. The existing guarded template runs `guarded.assertClean()` in its outer finally after `super.execute`; when commit already succeeded, assertClean/close failure cannot undo it. Treat that sanitized503 as post-commit ambiguous/lost-201 with one complete bundle, never as zero-delta rollback. Evict a contaminated connection and prove clean pool reuse without changing the shared template. No exception swallowed into a successful return or committed refusal.

Complete `catalog_order.customer_order` INSERT tuple (all 21 V004 columns; no issuance_status column is invented):

| Column | Exact source/value |
| --- | --- |
| id | Server-generated order UUID, generated once for this NEW attempt. |
| order_no | `OC-` + lowercase order UUID without hyphens, exactly as §3.2; 35 characters. |
| account_id | Trusted caller account UUID, revalidated under rank 9. |
| owner_organization_id | Current validated ORGANIZATION workspace UUID; SQL NULL for PERSONAL workspace. |
| subject_id | Current authorized subject UUID under subject lifecycle advisory. |
| channel | Verifier-attested MINIAPP/WEB matching current offer and request. |
| catalog_version_id | Current validated offer's ACTIVE version UUID. |
| catalog_offer_id | Current validated ACTIVE PERSONAL offer UUID. |
| purchase_consent_id | Exact ACCEPT descriptor/row UUID returned and verified from owner append. |
| amount_cents | Validated current version.price_cents, checked integer; never client input. |
| currency | Validated current version currency CNY. |
| payment_status | Literal UNPAID. |
| snapshot | Full immutable canonical §4/§10.19 snapshot as jsonb, not the public projection. |
| snapshot_sha256 | Lowercase SHA256 of the full canonical snapshot bytes. |
| version_token | Exact current accepted version token copied without normalization. |
| idempotency_key | Exact original client key, 1–475 code points, **without account prefix**. |
| request_fingerprint | Exact frozen ten-field order PURCHASE fingerprint from §4. |
| revision | Literal 1. |
| created_at | Database transaction timestamp (`transaction_timestamp()`); use RETURNING for the original response/consent-associated facts. |
| paid_at | SQL NULL. |
| closed_at | SQL NULL. |

The subject advisory serializes same-subject checkout/activation/lifecycle and protects UNKNOWN check; platform key handles cross-subject same-account key races. With budget-compliant serialization, same key/same semantics has one 201 then 200; same key/changed semantics has one 201 and one 409. If the wait exceeds 250ms the loser instead gets 503 and zero effects; one committed winner/effect bundle is still mandatory, and a later **explicit caller** same-key retry yields 200/409 only if current eligibility still passes. Separate keys may legitimately create separate UNPAID orders (no invented “one unpaid order” restriction). Same event is account/key scoped, not subject scoped. Different accounts with same key have different platform/consent identities. Foreign key and unique constraints remain final backstops, translated exactly §3.3. No application retry with a new ID/key.

**Lost-201 limitation (accepted precedence):** authority, lifecycle and current catalog eligibility checks stay before replay. This includes a committed bundle whose201 is replaced by503 from post-commit guarded assertClean/close failure, as well as transport-lost201. A later same-key retry returns200 for the original immutable result if current eligibility/semantics still pass; if the account/activation is later disabled, the offer becomes WITHDRAWN or version OFFSALE, or the accepted current token changes, retry may return403/422/409 instead. Do not move replay ahead of checks to promise unconditional recovery or create a compensating/new-key order. UNKNOWN blocks NEW; PA41 adds no order-read recovery path. Both limitations have explicit owners/destinations in §10.2.

## 6. Owner seams, dependencies and responsibility

Customer is HTTP/trusted-caller orchestration only. `CustomerOrderCommandService.requireContext/ create` maps TrustedCustomerCaller to immutable public order Caller; no LC context query, SQL, DSL, Connection or transaction in this adapter. Catalog's order aggregate service owns the checkout sequence, not general cross-domain services. Activation owner owns activation lock and purchase consent validation/append/facts only.

New named interface `catalog_order::customer-order-command`, package `catalog_order.customerorder`:

- `CustomerOrderCreationService` has exactly one method `Result create(Command command, Caller caller)`.
- Nested immutable Command: subjectId,catalogOfferId,channel,decision,policyCode,policyVersion,promptVersion,contentSha256,expectedVersionToken nullable for absence,clientIdempotencyKey,traceId. Caller: accountId,channel,workspaceType,organizationId nullable. Result: replay boolean plus immutable Order DTO/public snapshot/benefit records matching §3.2. No SQL/Spring/internal types in signature. Amount uses long; timestamps Instant; identifiers UUID.
- `OrderCreationException` exposes only frozen Code enum specified §3 and sanitized constructors. No public SQLState/message pass-through.

New named interface `activation_consent::purchase-consent`, package `activation_consent.purchaseconsent`:

- `PurchaseConsentService` methods: `LockedActivation lockServiceActivation(UUID subjectId, TransactionWitness witness)`; `ConsentDescriptor validateReplay(PurchaseConsentInput input, UUID consentId, TransactionWitness witness)`; `ConsentDescriptor appendPurchaseConsent(PurchaseConsentInput input, LockedActivation locked, TransactionWitness witness)`; `void appendPurchaseConsentFacts(ConsentDescriptor consent, FactPhase phase, UUID traceId, TransactionWitness witness)`.
- TransactionWitness is its own nested stable value record login/backendPid/transactionId, not operations EmployeeAuthorityReadPort and never a connection capability. Catalog creates it from DB witness. LockedActivation includes subjectId, nullable activationId/status/revision, witness. PurchaseConsentInput includes trusted subject/account/channel/event identity, ACCEPT/action and policy/prompt/content strings. Descriptor includes all V007 readable consent identity fields, canonical consent fingerprint, content digest, snapshot hash, acceptedAt, and whether newly appended. FactPhase=OUTBOX/AUDIT; descriptors must be verified against owner row in the same transaction before emitting facts, not trusted arbitrary record fields. No standalone consent endpoint.
- Owner throws its own nested Failure with REQUIRED/STALE/DECLINED/CONFLICT/UNAVAILABLE/INTEGRITY; catalog maps exactly §3. No activation dependency on catalog exception/API, no circular dependency.

Dependency deltas: customer_iam allowedDependencies 13→14 by adding catalog_order::customer-order-command. Catalog allowedDependencies 5→6 by adding activation_consent::purchase-consent. Activation's five dependencies unchanged. Actual Modulith direct module edges remain the existing 13 plus catalog_order→activation_consent =14 (customer→catalog already exists); module count remains 14. Existing root dependencies are not permission to import customer internals; new catalog/activation implementations must not do so. Configuration uses bean qualifiers, not Operations internal class imports. No generic transaction/router/helper module or employee authentication dependency.

Forward dependency constraint: the declared path permits the future cycle **catalog_order→activation_consent→entitlement_metering→catalog_order**. PA41 materializes only catalog→activation; activation→entitlement already exists for the separate trial flow, while entitlement→catalog must remain non-materialized. Keep the actual graph acyclic and exactly 14 edges. Extend the existing `newNamedInterfacesAndExactDependencyDeltasAreBounded` guard to reject actual entitlement_metering→catalog_order imports/dependencies and reject purchase-consent invocation of the activation-trial seam; preserve `ModuleBoundaryTests#verifiesNoCyclesOrBoundaryViolations` rather than suppressing a cycle. A future entitlement→catalog seam requires separately authorized design to break this cycle first; do not treat the existing allowedDependencies entry as permission to close it. Backend architecture + activation/entitlement/catalog owners own the recorded design residual (§10.2).

## 7. Exactly one implementation slice and exhaustive path allowlist

One slice: strict HTTP → trusted authority → LO transaction → owner activation lock → current catalog → PURCHASE replay/consent → immutable order → Outbox/audit → response, with real PostgreSQL proofs. Layer-only slices would obscure the one-transaction invariant; no multi-slice justification exists.

**This planning turn writes only this artifact.** The following is the complete proposed later implementation allowlist, not permission to modify those files during planning. All lists are exhaustive, brace groups below mean exactly the separately named files, not globs.

### 7.1 New production Java (18 files)

Prefix `backend/src/main/java/com/onecall/core/`:

```text
customer_iam/internal/application/CustomerOrderCommandService.java
customer_iam/internal/web/CustomerOrdersController.java
customer_iam/internal/web/CustomerOrderJsonBinder.java
customer_iam/internal/web/CustomerOrderDtos.java
catalog_order/customerorder/package-info.java
catalog_order/customerorder/CustomerOrderCreationService.java
catalog_order/customerorder/OrderCreationException.java
catalog_order/internal/customerorder/CustomerOrderConfiguration.java
catalog_order/internal/customerorder/DefaultCustomerOrderCreationService.java
catalog_order/internal/customerorder/CustomerOrderRepository.java
catalog_order/internal/customerorder/JooqCustomerOrderRepository.java
catalog_order/internal/customerorder/CustomerOrderSemantics.java
activation_consent/purchaseconsent/package-info.java
activation_consent/purchaseconsent/PurchaseConsentService.java
activation_consent/internal/purchase/PurchaseConsentConfiguration.java
activation_consent/internal/purchase/DefaultPurchaseConsentService.java
activation_consent/internal/purchase/JooqPurchaseConsentRepository.java
activation_consent/internal/purchase/PurchaseConsentSemantics.java
```

Configuration records for current approved policy are nested in PurchaseConsentConfiguration; no additional properties/resource file. Order repository seam is package-private and aggregate-specific; activation repository is owner-private. DTOs and binder remain customer-owned; no unrelated production utility file.

### 7.2 Modified production Java (5 files)

Same production prefix:

```text
customer_iam/package-info.java
catalog_order/package-info.java
customer_iam/internal/web/CustomerBoundaryFilter.java
customer_iam/internal/web/CustomerProblemWriter.java
customer_iam/internal/web/CustomerWebConfiguration.java
```

### 7.3 New test Java (9 files)

Prefix `backend/src/test/java/com/onecall/`:

```text
archtests/CustomerOrdersWebTests.java
archtests/CustomerOrderConfigurationTests.java
core/catalog_order/internal/customerorder/CustomerOrderSemanticsTests.java
core/catalog_order/internal/customerorder/CustomerOrderCreationServiceTests.java
core/activation_consent/internal/purchase/PurchaseConsentServiceTests.java
archtests/architecture/CustomerOrderCreationBoundaryTests.java
archtests/container/CustomerOrdersFixture.java
archtests/container/CustomerOrdersIT.java
archtests/container/CustomerOrderRoleLeastPrivilegeIT.java
```

Fixture contains nested verifier/test configuration only in test sources. Use no new top-level helper file or test class identity.

### 7.4 Modified tests and exact existing identities (9 files; no added/deleted methods)

Prefix `backend/src/test/java/com/onecall/archtests/architecture/`:

| File | Existing method identities to modify; exact reason |
| --- | --- |
| ModuleBoundaryTests.java | assertsExactAllowedDependencyMapFromPlan62 (map); discoveredProductionEdgesStayInsideTheConfiguredMap (13→14 with catalog→activation); verifiesNoCyclesOrBoundaryViolations (catalog 5→6/customer 13→14 and exact tuples); rejectsInternalPackageAccessFixture (second customer 13→14 tuple list). Preserve cycle/internal negative probes. |
| CustomerApiBoundaryTests.java | customerDependenciesAndReadOnlySqlStayInsideBoundary: customer internal files 52→56, preserve SQL/internal bans for every added file. |
| CustomerCatalogOfferBoundaryTests.java | customerImportsOnlyThePublicCatalogSeamAndNoForeignInternalImplementation: customer dependency 13→14, do not widen reader/ACL. |
| CustomerBusinessNumberBoundaryTests.java | customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation: 13→14 only. |
| CustomerSubjectDetailBoundaryTests.java | subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces: 13→14 only, preserve old query signatures. |
| OperationsCatalogVersionBoundaryTests.java | scansExactProductionFiles: production 307→325, test Java 199→208. |
| OperationsCatalogDraftBoundaryTests.java | scansExactPlannedProductionAndTestFiles: same counts; unchanged exact operationsdraft owner file set. |
| EvidenceRoleIvrAssociationAclBoundaryTests.java | ciPomAndOrdinaryMethodInventoryMatchExactly: current CI literals/classes/POM counts become future values §9; historical checkpoints and 410 oracle preserved. **v001ThroughV017AndPublicContractsRemainByteFrozen**: manifest excluding V018 and V019, **327+18=345**, recompute its LF-normalized sorted manifest digest from implementation bytes; preserve exact exclusion/frozen byte assertions. |
| CustomerCatalogOfferReadAclBoundaryTests.java | migrationOnlyAddsOnePackagedResource: current manifest 329→347, Java307→325/resources22 unchanged, new computed manifest hash, keep V019 assertion; **frozenContractsAndV001ThroughV018RemainByteIdentical**: manifest excluding V019, **328+18=346**, recompute its LF-normalized sorted manifest digest from implementation bytes; sourceCiPomAndXmlIdentitiesMatchExactly: exact future methods/annotations/hash/CI literals. Preserve all frozen/ACL/history/topology assertions. |

Other future modified paths: exactly `backend/pom.xml` (two Failsafe includes), `.github/workflows/ci.yml` (exact method maps, class/count/XML assertions). This artifact may receive final code-generation evidence reconciliation only if later controller permits it. No README/runbook/Control/other docs/private helpers changes; no change to V001–V019 or PA25. If implementation needs an unlisted file, stop for plan revision, not opportunistic allowlist widening.

## 8. Exact new ordinary test identities and assertions

Every name below is one ordinary `@Test void` method, no parameterized/dynamic/repeated test, no assumptions/skips/disabled. Loops cover variants without changing identities. Identity = package plus class plus `#methodName`. No extra test-bearing nested classes. Existing tests retain their identities.

### 8.1 Unit/module tests: 56 methods in six classes

`com.onecall.archtests.CustomerOrdersWebTests` (10):

- `personalOrderReturns201AndFrozenProjection` — exact endpoint/status/fields/no-store, UNPAID/NONE, DTO excludes schedule/terms.
- `identicalReplayReturns200AndSameBody` — immutable original body/status choice and no duplicate owner invocation effects.
- `authenticationPrecedesBodyAndHeaderValidation` — missing/repeated/malformed bearer beats malformed body/key; no owner call.
- `trustedChannelWorkspaceAndAccountCannotBeOverridden` — body/header forgeries rejected; server caller mapped unchanged.
- `strictBodyRejectsDuplicatesUnknownFieldsAndCoercion` — UTF8/trailing/null/array/number UUID/oversize/extra client money and consent identity cases.
- `acceptOnlyAndPolicyFieldBoundsAreExact` — DECLINE/missing action malformed policy/digest/token →422; lengths and null optional behavior.
- `idempotencyHeaderIsSingleExactAndBounded475` — missing/blank/multiple/NUL/unpaired surrogate/476/512 invalid; 1/74/75/128/475 and single-value comma valid exact key, supplementary Unicode count. Assert server-received leading/trailing OWS behavior under the actual HTTP stack, not raw wire preservation; no application trimming.
- `typedProblemsHaveFrozenStatusTitlesAndTrace` — every §3 code/status/title/headers and no leaked cause.
- `ordersRouteDoesNotAdmitReadOrAliasEndpoints` — GET/detail/trailing/encoded/matrix aliases remain rejected, existing routes unchanged.
- `unavailableOwnersAndUnexpectedFailuresAreSanitized` — missing/multiple beans, bad verifier context →503; unexpected→500.

`com.onecall.archtests.CustomerOrderConfigurationTests` (8):

- `reusesOnlyExistingLoPoolManagerAndDsl` — exact beans/identities, shared max4 pool/3000ms acquisition budget unchanged; exhaustion sanitized503/no effects, no new pool/manager.
- `requiresWritableReadCommittedRequiresNewTemplate` — reject read-only/wrong propagation/isolation/timeout/manager.
- `ownerHandoffIsMandatoryOnSameResource` — no active transaction, wrong resource/witness and read-only all rejected.
- `missingPersistenceDependenciesFailClosedLazily` — web-only starts; checkout503; no fallback LC/activation pool.
- `currentPurchasePolicyHasNoDemoOrTrialFallback` — absent/invalid current policy typed REQUIRED, no trial invocation.
- `customerAdapterUsesOnlyTrustedPrincipalValues` — no extra context pool read, caller/trace mapping exact.
- `duplicateOrAmbiguousOwnerBeansFailClosed` — fail closed, never choose arbitrary provider.
- `legacyCustomerConfigurationsRemainUnchanged` — original query templates/activation command continue existing qualifiers and flags.

`com.onecall.core.catalog_order.internal.customerorder.CustomerOrderSemanticsTests` (12):

- `consentEventIdentityMatchesCanonical81CharacterVectors` — independent fixed bytes/SHA, lowercase UUID+exact key;81 length.
- `unicodeAndCaseKeysRemainDistinctWithoutNormalization` — composed/decomposed/case/space/codepoint variants of the supplied server-received string differ reproducibly; direct identity space vectors do not assert wire OWS survival.
- `platformPurchaseKeyMatches512CharacterBoundary` — canonical36+colon+475=512, invalid lengths rejected.
- `orderFingerprintUsesOnlyFrozenTenSemanticFields` — exact key set/canonical bytes; all semantic changes alter hash, excluded IDs/time/hash/state do not.
- `consentFingerprintUsesOnlyFrozenElevenSemanticFields` — exact §4 fields, owner vectors agree, content compared separately.
- `snapshotCopiesAllApprovedCommerceAndScheduleFields` — complete exact §10.19 fields including terms/versionNo/lineKey/absolute schedules; bounded §4 terms boundary/unsupported-value mapping loops before hashing.
- `snapshotHashUsesCanonicalFullSnapshotNotProjection` — independent canonical JSON/hash, §4 fixed terms vectors/digests, object-order invariance and array-order rule.
- `personalPayableMatrixAndIntegerAmountAreExact` — all three types, zero allowed, null/negative/decimal/unsafe overflow rejected, CNY only.
- `enterpriseQuoteAndNonPersonalCatalogFactsFailClosed` — lawful QUOTE plus owner-private malformed-fact mocks fail422, no coercion.
- `approvedSchedulesMustMatchEveryLineAndInstallment` — missing/duplicate/mismatched type/units/id/time/interval invalid; no computed dates.
- `versionTokenAndOptionalExpectationAreComparedExactly` — case-sensitive, absent versus accepted same token equivalent, stale invalid.
- `responseProjectionAndOrderNumberAreDeterministic` — exact frozen DTO shapes, hash alias, UUID-based35 orderNo.

`com.onecall.core.catalog_order.internal.customerorder.CustomerOrderCreationServiceTests` (10):

- `newPersonalOrderRunsOneOwnerBoundTransaction` — full ordered interactions and one commit boundary.
- `authorityRealNameAndActivationPrecedeReplay` — every auth/lifecycle denial beats replay and no begin/append.
- `nonpayableCatalogPrecedesUnknownAndConsent` — ENTERPRISE/QUOTE/inactive/channel/token fail422 before begin.
- `replayValidatesOriginalConsentAndReturnsImmutableResult` — policy deployment change doesn't re-consent; creation body remains original.
- `changedInputConflictsBeforeNewConsent` — purchase semantic and policy/content changes conflict without append.
- `unknownOrderBlocksOnlyNewCheckout` — NEW409, exact prior completed replay200, rollback tentative begin.
- `consentDenialRollsBackNewIdempotency` — required/stale/declined/mismatch owner failures all propagate.
- `completeOutboxAndAuditHaveExactOwnerOrder` — rank10 complete then both14 facts then both15 audits; no owner foreign writes.
- `failureAndRestorationPreservePrimaryException` — fault each pre-commit phase; all §3.3 platform tokens/constraint backstops map exactly, outer rollback, sanitized mapping and suppressed cleanup. Post-commit guarded assertClean/close failures are the distinct complete-bundle subcases in the existing role IT, not rollback assertions.
- `noPaymentIssuanceOrRemoteCollaboratorIsInvoked` — no payment/grant/provider/trial dependency in command seam or invocation graph.

`com.onecall.core.activation_consent.internal.purchase.PurchaseConsentServiceTests` (8):

- `lockOnlyReturnsExistingActivationWithoutCreatingHead` — owner row lock only, missing returns absence, no write.
- `appendRequiresPriorLockWitnessAndMandatoryTransaction` — witness/resource/current role invalid denied, no late lock.
- `currentApprovedPolicyAndContentAreRequired` — exact current four-field identity; required versus stale precedence.
- `consentSnapshotAndFingerprintMatchFrozenInputs` — event/source81, account/source/purpose/action fields and canonical hashes.
- `sameEventReturnsFirstConsentAndChangedSemanticsConflict` — replay no append/facts, changed action/policy/source conflicts.
- `declinedMismatchedAndConsumedConsentFailClosed` — exact failure kinds and no accepted append.
- `factPhasesEmitOnlyOwnedPurchaseOutboxAndAudit` — exact payload and rank separation; caller descriptor cannot forge owner facts.
- `roleRestoresOnSuccessFailureAndAbortedSql` — same witness, suppression/rollback behavior; no new transaction/pool.

`com.onecall.archtests.architecture.CustomerOrderCreationBoundaryTests` (8):

- `newNamedInterfacesAndExactDependencyDeltasAreBounded` — method signatures/counts, actual14 edges/no cycles, future cycle/cut-edge guard in §6, no foreign internal import.
- `customerOrderAdapterHasNoSqlOrTransactionAuthority` — all four new customer files obey existing bans.
- `checkoutUsesOnlyExistingLoResourcesAndLiteralRoles` — qualifiers/flags/literal roles/MANDATORY, no new credential/property pool.
- `ownerSqlUsesOnlyFrozenGrantedColumnsAndWrites` — exact SELECT lists and all21 §5 INSERT values; statically exclude INSERT/UPDATE/DELETE/MERGE/TRUNCATE on catalog_product/catalog_version/catalog_offer/catalog_benefit_line from the checkout/owner graph, and all foreign FOR UPDATE/NO KEY UPDATE/SHARE/KEY SHARE locks; consent snapshot only owner role, no SELECT*. Publication advisory is not catalog row mutation. Include literal SQL and delegated calls, not just direct repository strings.
- `frozenContractsMigrationsAndPa25RemainByteIdentical` — raw+LF baselines §9; exact19 migrations/432 tuples/11 edges.
- `commandGraphHasNoPaymentEntitlementOrRemoteSideEffects` — no payment DML/entitlement DML/provider/network/scheduler dependency.
- `productionAndTestAllowlistIsExact` — exact §7 files/counts, test-only verifier excluded from runtime jar.
- `sourceCiPomAndXmlInventoryIsExact` — exact §9 names/counts/hash and no skipped/hidden tests.

### 8.2 Real PostgreSQL16 HTTP/transaction tests: 37 methods in two classes

`com.onecall.archtests.container.CustomerOrdersIT` (25):

- `personalSubscriptionCheckoutCreatesExactUnpaidOrder` — real HTTP201; full stored snapshot/price/consent/fingerprint/result hash, two Outbox/two audits, no payment/issuance.
- `personalOneTimeAndAddOnCheckoutUseServerPrices` — loop both types plus zero-priced valid PERSONAL; server values never request values.
- `miniappAndWebUseVerifierAttestedChannels` — success each channel, forged channel422 zero delta.
- `identicalRetryReturnsFirstImmutableOrderWith200` — same IDs/body/times/hashes, zero delta, consent not duplicated.
- `changedPurchaseSemanticsConflictWithZeroDelta` — subject/offer/channel/workspace/current version/token changes; compatible optional token alone equivalent.
- `changedConsentPolicyPromptOrContentConflictsWithZeroDelta` — same order/key changed request consent semantics409, no duplicate consent.
- `concurrentSameKeyHasOneCreateAndOneReplay` — deterministic §8 scheduling,201/200 same result and exact one committed effect bundle; separate held-lock timeout subcase503 and explicit same-key retry200, never accept503 as the success/replay assertion.
- `concurrentChangedKeyPayloadHasOneWinnerAndConflict` — deterministic same-key changed valid semantics,201/409 and one bundle (two subjects to prove platform serialization beyond subject lock); separate timeout503 then explicit caller retry409 with no extra effects.
- `unknownOrderBlocksNewCheckoutButNotCreationReplay` — lawful setup transitions via constraints; NEW409, identical completed replay200; no GET endpoint.
- `purchaseConsentIdentityAndLongKeysMatchCanonicalBytes` — 74/75/128/475 keys derive81, exact unprefixed customer_order key and prefixed platform key length512, independent hash bytes; HTTP OWS cases use the server-received string as authoritative in both stores/event hash, not presumed wire bytes. Unicode/space variants through owner call if HTTP header transport cannot carry them faithfully.
- `invalidKeysAndForgedBodiesNeverWrite` — 476/512/duplicate/header and forbidden request fields422 zero delta.
- `ordinaryClientRequiresCurrentVerifiedRealName` — missing/UNVERIFIED identity422 then VERIFIED success; org authority separate. V002 permits only UNVERIFIED/VERIFIED; do not seed invented PENDING/FAILED statuses.
- `inactiveMissingOrSuspendedActivationDeniesBeforeReplay` —403, including prior key, no consent/head/trial write.
- `currentAccountWorkspaceMembershipAndOwnershipAreReread` — current disable/revoke/owner move/wrong tenant/unknown403; all lifecycle predicates current.
- `enterpriseQuoteCheckoutFailsClosedWithZeroDelta` — lawful ACTIVE ENTERPRISE+QUOTE+null price for both channels and workspace kinds;422 beats UNKNOWN/stale policy; preserve all writes zero.
- `inactiveAndWrongChannelCatalogOffersFailClosed` — distinct lawful DRAFT/OFFSALE **version** and WITHDRAWN **offer** fixtures, wrong channel/missing offer/version;422 zero delta. Never invent OFFSALE offer status.
- `staleVersionAndNullOrNonpayableFactsCannotCheckout` — stale exact token422; null price actual lawful QUOTE; unsupported/malformed facts covered in semantics mocks, never disable DB constraints to seed impossible PERSONAL+null/ENTERPRISE+payable.
- `completeApprovedScheduleIsFrozenAndNeverComputed` — custom intervals/installments/absolute windows copied exactly; smaller legacy catalog snapshots fail422 zero delta. Legitimately approved terms exceeding each fixed §4 bound also fail422 with zero delta before hashing; assert only sanitized server-log reason enum/dimension/limit/counts, no public/audit diagnostics or content.
- `missingStaleDeclinedOrMismatchedConsentRollsBack` — approved owner policy absence/mismatch409 codes; persisted DECLINE/wrong source/subject/consumed fixtures all denied, no new accepted row.
- `publicationAndOffsaleSerializeWithCheckout` — both lock orders using pg_locks/pg_stat_activity; checkout first freezes old facts, publication first yields only new ACTIVE facts or422, no mixed snapshot.
- `activationAndLifecycleMutationsSerializeWithCheckout` — rank3/4/7/8/9 wait witnesses; winner state re-read; no stale acceptance/deadlock.
- `personalAccountDisableSerializesWithCheckout` — PERSONAL workspace skips7/8 but always takes9 using exact existing lifecycle key. Lawful fixture emulates the documented owner disable writer (production writer outside PA41), holding the same rank-9 key through its status-write commit. Disable-first committed schedule: checkout403 after re-read, zero checkout effects; checkout-first: one201/bundle before disable can commit, then replay403 under current-authority precedence. Witness blocked advisory/rank/current account read; separate held>250ms subcase503/zero delta, never stale201. Admin lifecycle deltas are expected fixture effects, not checkout effects.
- `injectedFailureAtEveryWriteStageLeavesZeroPartialRows` — pre-commit listener faults after begin/consent/order/complete/each Outbox/each audit/before restore and terminated connection; every §3.3 one-to-one token/call-site probe and23505/23514/23503 backstop exact public outcome; complete-table deltas zero after rollback, no success. Post-commit cleanup is the distinct subcase in the existing role identity below, never asserted to roll back.
- `replayAfterCatalogOrPolicyChangeFollowsExactPrecedence` — current offsale422/current token conflict409/current authority denies first/current policy changed identical retry200; persisted mutable payment state doesn't rewrite creation result.
- `allPathsAvoidPaymentIssuanceAndProviderCalls` — full-row zero delta catalog_product/catalog_version/catalog_offer/catalog_benefit_line plus payment_order/payment_event/entitlement_issuance/grants/balance/trial/activation transition across success/replay/denial/fault; no catalog DML/foreign row lock in observed checkout SQL, no provider bean or network call.

`com.onecall.archtests.container.CustomerOrderRoleLeastPrivilegeIT` (12):

- `postgres16Flyway19AndExactPa25TopologyAreUsed` — SHOW server_version_num=16xxxx; Flyway19 validate; exact PA25 apply/check markers logins7/effective9/memberships11,432 oracle unchanged.
- `loHasNoRightsUntilLiteralOwnerRoleSwitch` — login no inherited table rights; exact seven SET edges, not superuser/owner.
- `customerAndActivationPoolsCannotSubstituteForLoTransaction` — LC42501 for catalog/consent writes, separate activation resource cannot satisfy MANDATORY witness even with same LO credentials.
- `orderAndConsentShareConnectionTransactionAndLogin` — every SQL listener entry same connection/pid/txid/login and expected owner current_user.
- `activationRowLockIsTakenOnlyByOwnerBeforeIdempotency` — observed SQL/order/role, catalog foreign FOR UPDATE42501, missing head no insert.
- `catalogReadsOnlyPermittedForeignColumnsWithoutLocks` — allowed exact V004/V007 columns positive; unlisted consent snapshot/identity secret and foreign lock/write42501.
- `roleHandoffsRestoreOnSuccessReplayAndTypedDenial` — after every path current_user=session_user, flags/isolation/timeouts baseline, repeated checkout pooled reuse.
- `sqlAbortAndRestorationFaultRollbackAndCleanPool` — pre-commit SQL-abort/restoration/42501/55P03/deadlock/terminated-backend faults produce sanitized503 and zero delta, contaminated connection evicted. Extend this same identity with post-commit assertClean/close-failure subcases: observer confirms commit before injecting cleanup failure, caller sees sanitized503, exactly +1 order/+1 ACCEPT consent/+1 completed PURCHASE/+2 Outbox/+2 audit remain, no partial/unrelated/catalog/payment writes, and clean pool reuse is proven. Later same-key retries separately assert200 for unchanged eligibility,403 for disabled authority/activation,422 for withdrawn/offsale catalog and409 for changed current fingerprint/token under §3.1; retries add no second bundle. Record fixture lifecycle/publication deltas separately. No additional method/class.
- `missingMembershipOrRoleFailsClosedWithoutAclRepair` — controlled negative provisioned clone/transaction probe denies; original PA25 exact graph restored by fixture reset, no application GRANT.
- `lockOrderAndTimeoutsMatchExistingLifecycleAndCatalogKeys` — witness pg locks literal namespaces/ranks/publication interleaving/2s250ms5s; PERSONAL9 unconditional, publication9→10 no reverse edges, no foreign row locks; held-lock timeout503 separate from deterministic success schedules.
- `platformAuditAndOutboxTablesAreFunctionOnly` — table writes42501 but functions succeed in owner roles; success facts exact/replay none.
- `noNewDatabaseObjectPrivilegeOrMigrationAppears` — before/after exact catalog ACL/owners/functions/roles/memberships/schema/history digest unchanged.

Fixture requirements: compose/reuse CustomerBusinessNumbersFixture's PostgreSQL16 migration/provision primitive, but build a fresh PA41 web+persistence Spring context with its test-only verifier and **actual PA25-provisioned LO login `pa36_operations`**; do not revoke its legitimate seven edges. Set only its test password using privileged setup and existing datasource properties. HTTP uses real random port and verified bearer map, no controller mock for IT. Setup may use admin/owner credentials to insert lawful catalog/subject/activation/consent fixtures and approved synthetic full schedules; runtime command never does. No constraint/trigger disablement or impossible enterprise payable fixture. Keep the actual shared pool max4/acquisition3000ms; serial reuse probes release each connection before the next request (do not resize to one). Bound active customer/Operations runtime requests to **two per scenario**, with no parallel IT methods/classes sharing this fixture; witness/admin observer connections are a separate existing test-setup facility, never extra runtime capacity. Two runtime requests leave capacity for baseline restoration/guard probes; do not hold all four while awaiting a third request. Separate acquisition-exhaustion probe may deliberately borrow all four, expect fifth-request503, then release/verify clean reuse; it is not run alongside concurrency schedules. Restore fixture state per test, with bounded test-only latches.

**Deterministic schedules, not latency assertions:** SQL listener barriers pause the losing HTTP transaction **before submitting** its contested advisory/idempotency SQL, outside any server lock wait. Assert the winner owns the expected transaction advisory/row lock and the loser reached that exact SQL/key/role; let the winner commit, verify termination/commit from the observer, then release the loser. This yields mandatory 201/200 or 201/409 with the unmodified250ms setting; never rely on a CI thread observing a lock and releasing it within250ms, arbitrary sleeps, a timeout retry loop or a weakened “200/409/503 all accepted” assertion. Apply both deterministic orderings to publication/activation/organization/account-disable scenarios. The two-subject case proves key scope beyond the subject lock, not simultaneous platform contention: unconditional same-account rank9 also serializes it. Separate real-contention subcases hold a known lock until the losing statement returns55P03/503, then verify zero losing effects, let the winner complete and explicitly issue the documented caller same-key retry. A blocked-lock witness when observable plus the exact held key and55P03 establish real database contention; do not require a timing-sensitive polling success inside250ms. Bound pre-SQL barriers to complete before the existing five-second transaction deadline; scheduler/resource exhaustion is an explicit test infrastructure failure, never an alternate passing outcome. No test-only enlarged runtime pool/timeouts, skipped test or mock command path.

Zero-delta oracle: capture ordered complete row JSON/counts for **all rows** of `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line` (not only the chosen rows), customer_order, consent_record, platform.idempotency_record, platform.outbox_event and audit.audit_fact (use actual frozen V001 relation names), payment_order/payment_event, entitlement_issuance/entitlement_grant/entitlement_balance/trial_grant and activation heads/transitions before each scenario. Compare complete bytes/IDs/revisions, not only counts, after rollback/denial/replay. For success require exactly +1 order/+1 consent/+1 completed PURCHASE/+2 Outbox/+2 audit and **zero catalog/product/version/offer/benefit and every other unrelated row delta**; baseline fixture rows excluded by before/after delta, not deletion. Reused lawful unconsumed consent fixtures assert that preexisting consent is unchanged instead of adding another one; normal fresh-key success asserts +1. Admin observation occurs after runtime transaction termination. Concurrent lifecycle/publication fixture mutations must have separately recorded exact expected owner deltas; checkout's catalog delta remains zero after subtracting only that enumerated mutation, with no blanket exclusion of those tables. Also assert no leaked NEW idempotency, hidden refusal consent or paid timestamps. Count exact runtime SQL functions and effect ordering; observe no catalog mutation or foreign row-lock SQL in runtime checkout, complementing the static graph proof. Outbox/audit redaction assertions exclude key/bearer/protected values.

Post-commit failure oracle: for the guarded assertClean/close-failure subcases, use the same complete-row snapshots and unrelated-table zero-delta assertions, but require the **entire success bundle** (+1 order/+1 ACCEPT consent/+1 completed PURCHASE/+2 Outbox/+2 audit) after the observer confirms commit, even if HTTP returns503. Never count a committed complete bundle as rollback failure or allow a partial bundle; later retry changes none of these original rows/facts, apart from separately enumerated owner fixture mutations.

## 9. Current-to-future identities, arithmetic and pins

Measured from current source (ordinary method regex used by existing guards): production Java307; resources22; production manifest329; test Java199; Surefire110 classes/1071 methods; Failsafe42 classes/701 methods; total152 classes/1772 methods; annotation-line scan1773 (one existing non-method annotation line); POM42 exact includes. Keep all old identities.

Future: +18 production Java=325; resources22 unchanged; manifest347. Guard subsets excluding V019:328+18=346; excluding V018/V019:327+18=345. +9 test Java=208. +6 unit classes/+56 methods=116/1127. +2 IT classes/+37 methods=44/738 (one added PERSONAL account-disable method; other corrections extend existing identities with loops/subcases). Total160 classes/1865 methods; annotation-line scan1866. POM includes42+2=44, add only `**/CustomerOrdersIT.java` and `**/CustomerOrderRoleLeastPrivilegeIT.java` to existing container-smoke profile, failIfNoTests retained. No dependency/version change. One slice; later implementation allowlist totals18 new+5 modified production Java,9 new+9 modified test Java, and POM+CI:43 paths, plus this plan only if separately authorized for evidence reconciliation.

CI edit map below refers to current `.github/workflows/ci.yml` source lines (resolve by symbol after insertions), not permission to globally replace `42`, `1071` or `1773`. Retain all historical checkpoint maps/line scans and existing PostgresContainerIT64 and other per-class suite counts. Append `pa41_unit_additions` and `pa41_container_additions` using exactly §8 FQCN→method-name sets, lengths [10,8,12,10,8,8] and [25,12]. Require source=CI=POM=XML exact identity equality, no skips/errors/failures, no extra suite silently ignored. Update direct guards in §7.4 rather than disabling manifest or literal assertions.

| Current CI source line / symbol | Exact implementation edit or preservation |
| --- | --- |
| :165–166 early `sum(... unit_suites)` / diagnostic | Change only the current sum1071→1127 and its “expected exactly1071” diagnostic→1127. |
| :2436–2442 additions union / `source_classes`; :2443–2479 accumulator initialization; :2516–2546 `pa40b_names`, `additions`, checkpoint totals and filtered-source scans onward | Append and union the two PA41 maps before source-class equality; initialize `pa41_start_totals`/`pa41_start_line_scan_total`, derive `pa41_names` per class, and compute the pre-PA41 checkpoint excluding exactly those names, asserting [1071,701]/1773. Include PA41 names in `additions` and every earlier checkpoint's excluded-name union/annotation filter so all historical values remain unchanged; no fabricated checkpoint assignment. |
| :2708 `assert line_scan_total == 1773` | Current scan becomes1866. Keep the new pre-PA41 scan checkpoint1773 distinct. |
| :2709 `method_totals` | Current [1071,701]/sum1772 becomes [1127,738]/sum1865. |
| :2710 historical/current line-scan tuple | Preserve all seven historical values `(291,321,361,401,446,481,1441)` byte-for-value; change **only its final current value1773→1866**. Both current1773 positions (:2708 and :2710) must move, not just the standalone assertion. |
| :2712–2713 `len(unit_expected)`, `len(container_expected)` | Current classes110→116 and42→44. |
| :2716–2718 Failsafe `includes` / equality | Append only the two §9 includes, count42→44; retain exact set equality. |
| :2719 `len(container_expected["com.onecall.archtests.container.PublicShareAdmissionIT"]) == 42`; :2720–2725 per-class literals | Preserve42 and every other existing per-class literal; they are not the includes/current class count. All historical checkpoint literals at :2625–2707 stay unchanged. |
| :2726–2727 `exact_inventory` | Surefire current1071→1127; Failsafe current701→738; same report paths and exact identity checks. |
| :2728 final `print` summary | Change label **PA40B→PA41**, current header to `116/1127 + 44/738 = 160/1865`, current tail to `1127+738=1865`, final current line scan to1866 and POM includes to44. Preserve the full historical chain; retain1071/701/1772 and1773 as the new PA41-start checkpoint before the current totals/scan. Never rewrite historical PA40B-start1026+682=1708 or1709. |

These are future implementation edits only; this correction does not edit CI, POM, guards or source. Current totals remain independently measured baselines; future hashes/manifests still require implementation-time recomputation from actual bytes below.

Current sorted `FQCN#method` LF-terminated identity SHA256: `dd3d1c5e93baa9d9cd5e362cfb3ea2493589b5a3ef73f4a5dace1da16738cab8`. **Planned** union identity SHA256, independently computed from the1772 source identities plus the93 exact names in §8: `fe2162716b64a413ed8d118f64299d4fd49b1f8d60051f7c93d67b1b7aefc8dd`. This is a planning expectation, not an implemented-source attestation; recompute the future source identity/annotation inventory from actual implementation bytes and require agreement with CI/POM/XML and this exact method set. Do not copy a stale pre-correction92-method digest.

Current production manifest SHA256: `3be935b02c6c2a6622880e2d59274e386c6f17649ce712d7d2b728dcfc7b0f97`; algorithm sorted lines `src/main/.../path + ' ' + sha256(LF-normalized file bytes)`, one LF after each. Future production manifest hash **cannot be computed until implementation bytes exist**; implementer recomputes and pins it, reports both digest and exact changed path set. Do not invent a hash.

Three distinct future manifest pins must be computed from implementation bytes: all347 entries;346 excluding V019 for `CustomerCatalogOfferReadAclBoundaryTests#frozenContractsAndV001ThroughV018RemainByteIdentical` (current328 digest `27d4a25ea4e6f792565ea638a1d9a89c3b04c2d4bd978806d15827cbc36373a9`);345 excluding V018/V019 for `EvidenceRoleIvrAssociationAclBoundaryTests#v001ThroughV017AndPublicContractsRemainByteFrozen` (current327 digest `ecc30b8b411debc208cca8415a9a2cdb576ee2e9b1ee54fee129b6d792ca206d`). Preserve each guard's exact path exclusion, sorting, LF normalization and frozen historical file checks. These are live production manifests despite the historical method names, not permission to change migrations/contracts or reuse one digest for different subsets.

### 9.1 Frozen raw-byte SHA256 baseline

Raw hashes intentionally differ from canonical-LF guard hashes in this CRLF checkout. Preserve bytes; don't normalize frozen sources. V001–V019 below identify their existing complete filenames by version in the migration directory (listed in current repository); no twentieth entry.

| Input | Raw SHA256 |
| --- | --- |
| V001 | 96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6 |
| V002 | 4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505 |
| V003 | 4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7 |
| V004 | 8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030 |
| V005 | 6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065 |
| V006 | 7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d |
| V007 | bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81 |
| V008 | 32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde |
| V009 | 3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1 |
| V010 | d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559 |
| V011 | ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e |
| V012 | ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6 |
| V013 | 1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e |
| V014 | bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8 |
| V015 | c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc |
| V016 | a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c |
| V017 | 74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3 |
| V018 | ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c |
| V019 | 0f0638de0c29609c4c6f0c5df969d971ea6e40cb09d46b94c174654bdf22c695 |
| api/openapi/onecall-v1.openapi.yaml | 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68 |
| docs/12-technology-stack-and-system-architecture.md | 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db |
| docs/14-production-data-model-and-flyway-schema-spec.md | 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31 |
| docs/15-production-openapi-surface-spec.md | db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0 |
| docs/17-production-security-design-spec.md | b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205 |
| backend/ops/postgresql/pa25-runtime-identities.sql | e12e485b47b6d95d5161364f36b1a30e798f041e49d93ac4167700523df00e1e |

Current mutable build pins (future bytes unknown): backend/pom.xml raw `f2134433800f6e96a8b2dad4667d91dcd5b345a56e4ad9b30fed9419d0abaca1`; .github/workflows/ci.yml raw `005ec942f910d8eeff4c8a715c81f98b9149584e8b1157bd0566136f2ae03e25`. PA25 LF hash `ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47` unchanged. Exact19/432/11/7 effective SET targets/35 project roles after provision remain unchanged; monitored effective_roles=9 is not the number of LO SET targets.

## 10. Validation, docs decision, residuals and completion

### 10.1 Implementation validation commands (not executed in this planning task)

Use repository-approved Java21/Maven environment and reachable Docker daemon; no credential printed. From repository root:

```sh
git diff --check
mvn -f backend/pom.xml -Dtest=CustomerOrdersWebTests,CustomerOrderConfigurationTests,CustomerOrderSemanticsTests,CustomerOrderCreationServiceTests,PurchaseConsentServiceTests,CustomerOrderCreationBoundaryTests test
mvn -f backend/pom.xml -Dtest=ModuleBoundaryTests,CustomerApiBoundaryTests,CustomerCatalogOfferBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogDraftBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests test
mvn -f backend/pom.xml -Pcontainer-smoke -Dit.test=CustomerOrdersIT,CustomerOrderRoleLeastPrivilegeIT verify
mvn -f backend/pom.xml -Pcontainer-smoke clean verify
mvn -f backend/pom.xml package -DskipTests
(cd api/openapi && npm ci && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s1 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s2 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures --check-doc-links
```

Additionally run the **unchanged complete contract-regressions job commands** in `.github/workflows/ci.yml` for A2 s3, A3 s3, A4 s2, A5 s1/s2 with their exact existing model/fixture/spec/bundle arguments; run its dependency-firewall/runtime-jar checks and its updated exact source/CI/POM/XML inventory block. They are not replaced by Maven success. Extract the CI heredoc scripts exactly as tracked rather than hand-reimplementing relaxed checks; do not execute unreviewed shell interpolation with credentials. Assert1127 Surefire +738 Failsafe =1865 methods, no skip/failure/error, exact method sets,44 includes,19 packaged migrations/no PA25 in jar/no test verifier class or implementation. A failed/unavailable Docker run is NOT EXECUTED, never pass-with-mocks. Only after all focused checks and full clean verify can completion claim behavioral acceptance.

Original planning verification recorded read-only HEAD/source/test/ACL inspection, independent source identity count/hash calculation and frozen raw/LF hashes. That run's `docker version --format '{{.Server.Version}}'` failed with permission denied on `/var/run/docker.sock` (symlink to `/var/run/dify.sock`). This bounded correction rechecks HEAD, plan test-name arithmetic, fixed-vector digests, current manifests/frozen hashes and write scope; it does not rerun Docker, PostgreSQL or Maven behavior suites. This is an environment validation limitation, not evidence that topology is unusable; do not change socket permissions or provision a new daemon under this authorization.

### 10.2 Docs decision and owned residuals

Only this plan is a planning deliverable. No source/README/runbook/Control/spec editing in this turn. Implementation docs are this accepted bounded plan plus completion evidence, subject to the controller's later write authorization; no frozen spec rewrite.

| Residual | Owner / destination | Classification |
| --- | --- | --- |
| Frozen OpenAPI description says “personal or enterprise” | API contract owner + Product/Controller; next separately authorized contract wording correction | Known wording mismatch, no code broadening or edit here. |
| Frozen OpenAPI createCustomerOrder `x-a0-models` advertises `catalog_order.payment_order` (:737) although PA41 creates none | API contract/A0-model owners + Catalog/Payment; separately authorized OpenAPI model-reference alignment | Owned A0-model mismatch, not permission/requirement for payment DML; payment_order and payment_event remain zero delta on every PA41 path. |
| Frozen OpenAPI advertises customer order GET/list, but PA41 implements POST only | API contract owner + Product/Controller; separately authorized advertised-read contract/implementation alignment | Known advertised-read mismatch; no read route/controller/query added here. |
| Current-catalog/authority-before-replay can prevent recovery of a lost201, including committed success replaced by503 after guarded assertClean/close failure | API/Catalog + customer lifecycle and Platform/pool owners; separately authorized retry/recovery contract review | Post-commit cleanup cannot roll back the complete order/consent/idempotency/Outbox/audit bundle. Accepted precedence remains: retry200 when still eligible, otherwise documented403/422/409; never promise unconditional recovery, compensate with a new key or move replay earlier. §8 distinguishes zero-delta pre-commit faults from one-bundle post-commit ambiguity. |
| Existing same-subject/offer UNKNOWN blocks NEW with no PA41 order-read recovery | Payment reconciliation + API/Product; separately authorized UNKNOWN reconciliation and customer recovery/read workstream | 409 ORDER_UNKNOWN_PAYMENT_PENDING remains fail-closed, including cross-account keys; no payment reconciliation/read API added here. |
| Existing catalog draft/publication snapshot lacks complete approved schedule/terms input | Catalog-order + Product/Finance/Operations; Q4/Q20 approved commerce input/publication follow-up | Historical rows fail closed; full lawful §10.19 fixtures prove PA41. Not schema/topology blocker; do not claim commercial launch readiness. |
| Plan-owned fixed terms bounds may reject legitimately approved future content | Catalog/Product/Finance/API owners; separately authorized size-policy/bound revision in Q4/Q20 commerce-input/publication follow-up and a successor bounded checkout plan **before production purchase content approval** | Approved-but-over-bound NEW input remains422/zero delta, no bypass or bound change in PA41. Only sanitized server-log reason enum + exceeded dimension/limit + counts, never terms content/public data/audit payload/new contract field. Existing bounded-terms identities cover this case; no additional methods/classes. |
| Actual production purchase wording/content identity | Activation-consent + Product/Legal/Security; Q10 deployment approval | Required input; no invented default. Missing input denies NEW; tests use labeled synthetic approval only. |
| LO credential has broad existing SET capabilities | Platform/Security/Backend; existing PA25 deployment-security controls | Reuse bounded literal roles/witness/guarded cleanup, no claim of RLS or least-privilege redesign. |
| Shared LO max4 pool,3000ms acquisition and checkout/Operations contention | Platform/Operations + capacity/acceptance owners; separately authorized `deployment/capacity` analysis and `docs/19-production-acceptance-matrix.md` capacity/SLA acceptance | No resizing/new pool or docs19 edit in PA41; bounded concurrency tests prove correctness, not production throughput/isolation. Production overload/timeouts503 remain possible. |
| Future catalog_order→activation_consent→entitlement_metering→catalog_order module cycle | Backend architecture + Catalog/Activation/Entitlement owners; separately authorized dependency/seam design before entitlement→catalog is materialized | Existing allowed edges do not authorize a runtime cycle; actual14-edge acyclicity, no entitlement→catalog edge and no purchase→trial call are guarded in PA41. |
| Publication advisory occupies rank9→10 slot absent from frozen docs14's lock table | Data-model/Concurrency + Catalog/Lifecycle owners; separately authorized docs14 lock-order amendment before later flows extend the graph | Forward no-reverse-edge invariant and executable guards in §5/§8 apply now; docs14 stays byte-frozen, no later path may acquire earlier customer locks while holding publication. |
| Docker unavailable in planning environment | Platform/Test infrastructure; implementation CI PostgreSQL16 acceptance | Runtime proof required before completion; no mock substitution. |
| Frozen complete-snapshot hash versus redacted result hash semantics | Catalog/API owners; frozen §9.17/§10.19 conformance tests | Store result_sha256 as stipulated complete snapshot hash; explicitly verify snapshot and original result association, never assume generic hash(result JSON). |
| Enterprise payable model | Product/Finance/Database; separately authorized future commercial-model/schema design | Out of scope; no prerequisite added to PA41 PERSONAL unit. |

Open blocking questions: **none**. No mandatory topology/schema blocker. Missing approved production inputs and unavailable local Docker are explicitly fail-closed deployment/validation limitations, not unanswered implementation choices. If actual frozen privileges fail the specified lawful LO runtime probes, stop and report a separate prerequisite rather than adding any grant/pool/schema change.

### 10.3 Accepted finding closure (plan-level, not runtime acceptance)

| Finding | Bounded closure / future proof |
| --- | --- |
| A-01 | §7.4/§9 add both omitted live guards,328→346 and327→345; all three future production digests and source identity digest recomputed from implementation bytes. |
| A-02 | §8 all-row catalog/product/version/offer/benefit zero-delta oracle for success/replay/denial/fault; static and observed-SQL no-catalog-write/no-foreign-row-lock proof. |
| A-03 | §5 rank9 unconditional for PERSONAL/ORGANIZATION, only7/8 conditional; explicit PERSONAL disable-race IT raises new IT methods36→37 without adding a file/class/slice. |
| A-04 | §10.2 API/A0-owner payment_order model mismatch residual, zero payment writes unchanged. |
| A-05 | §4 bounded terms domain, validate before hashing, exact422 versus500 mapping and fixed canonical bytes/SHA256 vectors; existing semantics/IT methods extended. |
| B-1 | §5 explicit customer250ms fail-fast/503 choice; §8 deterministic pre-SQL success barriers plus separate actual-contention timeout tests and explicit caller retry, no widened passing oracle. |
| B-2 | §4/§5 exact original1–475 client key versus platform account-prefixed1–512 key and complete21-column INSERT tuple; long-key storage assertions. |
| B-3 | §2.1/§8 shared max4/3000ms pool, at most two concurrent runtime requests/serial fixture tests, separate exhaustion probe; capacity/docs19 owner/destination residual. |
| B-4 | §6 forward cycle/cut-edge guard and §10.2 architecture-owner residual, no actual cycle or trial call. |
| B-5 | §3.3 exact existing platform-token/23505/23514/23503 public mappings, rollback/redaction/zero-delta test loops, no new codes. |
| B-6 | §8 distinct OFFSALE version/WITHDRAWN offer lawful fixtures; no impossible OFFSALE offer seed. |
| B-7 | §5 publication advisory9→10 forward no-reverse-edge rule, §8 guards and §10.2 docs14-owner amendment residual. |
| A-RR-01 | §3.1/§3.3/§5 distinguish pre-commit rollback from post-commit assertClean/close ambiguous503; existing role-fault identity proves exactly one committed bundle, no partial/unrelated writes and later same-key200/403/422/409; lost-201 residual owns this class. |
| B-RR-01 | §3.3 exhausts V001 P0001 tokens/call sites, begin-only INCOMPLETE, complete NOT_FOUND/CONFLICT and audit integrity500 mappings; one-to-one probes in existing identities and append-only reachability proof, no public code. |
| B-RR-02 | §9 source-line/symbol edit map updates both current1773 scan positions, includes44, early/exact inventories and PA41 summary; preserves seven historical tuple values, checkpoints and PublicShareAdmissionIT42. |
| B-RR-03 | §4/§8/§10.2 approved-but-over-bound422/zero-delta subcases, Catalog/Product/Finance/API-owned separately authorized policy revision before production content approval, sanitized server-log-only diagnostics; no added identity or contract field. |

Controller rulings remain binding: current catalog checks precede replay with lost-201 limitation disclosed; future hashes come from implementation bytes; UNKNOWN/no-order-read recovery and OpenAPI wording/payment-model/advertised-read mismatches are owned residuals. Every uncontradicted one-slice decision and frozen-input boundary is preserved. Correction verification is limited to plan/source inventory arithmetic and hash checks; no implementation or runtime acceptance is claimed.

Completion report format for implementation: exact accepted plan/HEAD, one slice, changed paths versus allowlist, source/CI/POM/XML classes+methods+hash, computed production manifest hash, raw frozen-input match, PostgreSQL16/Flyway19/PA25 markers and witness/role evidence, PERSONAL first/replay/conflict/concurrency and ENTERPRISE/QUOTE zero-delta results, fault-injection rollback/no-payment results, full validation commands+exit+test totals (NOT EXECUTED where appropriate), residual owner/destination, blocking questions/topology/schema status, and next controller entry. Do not claim production readiness, merge or later-gate acceptance.
