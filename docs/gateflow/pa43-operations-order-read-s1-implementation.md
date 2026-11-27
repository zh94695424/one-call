# PA43 S1 implementation — resume 3 completion report

- Task: `pa43-s1-implementation-qwen4max-20261127-160000-resume3`.
- Code review fix1 task: `pa43-code-review-fix1-qwen4max-20261127-180000`; gate `code review fix1`; accepted findings B F1/F3/F4/F6 resolved in §13; A F1, B F2, B F5 deferred by Controller to Backend/Test Maintenance. §§1–12 are retained as the pre-fix1 implementation record and historical evidence; §13 is the retained fix1/validation record.
- Code review fix2 task: `pa43-code-review-fix2-qwen4max-20261127-210000`; gate `code review fix2`; accepted re-review B findings F1/F2/F3 (artifact `docs/reviews/code-review-20261127-153655.md`) resolved in §14; A F1, B F2, B F5 deferrals unchanged. §§1–13 are retained history; §14 is the current authoritative fix2/validation record.
- Gate: implementation; work unit `PA43-operations-order-read-foundation`; S1 is the single authorized end-to-end read-only slice.
- Binding capability: only `GET /api/v1/operations/orders` / `listOperationsOrders` and `GET /api/v1/operations/orders/{orderId}` / `getOperationsOrder`.
- Accepted plan: `docs/gateflow/pa43-operations-order-read-plan.md`, 452 lines / 121,834 bytes / SHA256 `6604a175ad46ceeab8b0557596cd81d67344edd7d62bbfb77c82df0e8103b804` (re-measured, unchanged).
- Controller retry Control SHA256: `e3fc3613ccb94baef43952328f2100c5308aa1c5831971013b0048ffd8ec2387` (re-measured, unchanged).
- Start HEAD `bf7d2c0ce3a753e202ad08abcc6149f2d68f386f`; branch `codex/pa43-next-work-unit`; baseline `origin/main=29e715a3dccd8bc3fbedb4a094555294b7482a67`. HEAD/branch unchanged; nothing staged.
- Status: **IMPLEMENTATION COMPLETE for the accepted slice; full focused/clean/CI/package/firewall validation green.** This report supersedes the retry-2 blocker report (`PA43-TRACE-EXCLUDED-ALIAS-01`), whose decisive raw runtime evidence is retained below. No commit/stage/push/PR/merge/approval/ready/reviewer/external-comment action occurred.
- Fix1 status: **CODE REVIEW FIX1 COMPLETE; all four accepted findings resolved; focused, clean full container-smoke, CI inventory, packaged-JAR smoke and dependency-firewall validation re-run green (§13.4).** Stopped for fresh A/B code re-review; no external action.
- Fix2 status: **CODE REVIEW FIX2 COMPLETE; all three accepted re-review B findings resolved; focused, clean full container-smoke, CI inventory, packaged-JAR smoke and dependency-firewall validation re-run green (§14.4).** Stopped for fresh A/B code re-review; no external action.

## 1. Resume scope and what changed

The worktree already held retry-2's complete 34-path implementation. Per the Active PA43 checkpoint and re-review-7 adjudication, this resume preserved all 32 unaffected paths and adapted only the rejected-alias ERROR contract:

1. `OperationsBoundaryFilter` ERROR-dispatch branch extended from the canonical-only union to the plan-fix-7 bounded order family, with **type-safe** error-attribute gates (re-review-7 B finding 2).
2. `OperationsOrderReadBoundaryTests` independently re-authored `EXPECTED_REQUIRED_ERROR`, re-measured and pinned the selected-variant canonical-LF hash, updated the ERROR-region census to the 17-occurrence contract, added ERROR gate sentinels, the five plan-fix-7 mutants and real-filter positive/negative/type-safety behavior fixtures. The pre-change baseline region constants and the `NO_ERROR_DELTA` variant/hash are unchanged and still reconstruct the pinned baseline without test-time Git.
3. `OperationsOrderReadWebTests` added one method `errorDispatchFamilyConcealsRejectedAliasesButNeverCanonicalGetFailures` (real filter, native ERROR attributes, both context paths, problem-body/header assertions, type-safe missing/non-String method cases, sibling/unrelated preservation, zero verifier interaction).
4. `OperationsOrdersReadIT.traceProbeSelectsExactErrorDispatchVariant` extended to the complete rejected alias/method family with native ERROR witnesses, credential variants, zero-business-SQL assertion, sibling TRACE preservation and per-class outside-family recording.
5. Current-aggregate/CI/POM/README bookkeeping re-measured from final bytes (one added unit method; filter content bytes changed). README current section bytes are unchanged (it carries no hard-coded method counts), so both IVR README pin slots and the composite remain valid.

No new product/test/build path beyond the §9 allowlist was required. No V021, index, schema/contract change, new pool/identity/role/membership/grant, provider payload, payment/refund/invoice/reconcile logic, audit/trial-policy endpoint, frontend, cache/search/read model or unrelated refactor. The sole customer-source change remains the exact two-alternative GET pass-through in `CustomerBoundaryFilter`.

## 2. Exact changed paths (34) and allowlist proof

Enumeration: `git diff --name-only` + `git ls-files --others --exclude-standard backend`, excluding this report. The result is exactly the §9 34-path set (9 new production + 2 modified production + 12 new test + 8 modified test + POM + CI + README). Plan, Control and all review artifacts are byte-identical to HEAD (`git diff --name-only` over them is empty). Sorted 34-path canonical-LF content manifest (`sha256(lf(bytes))`, two spaces, repo-relative path, LF): 34 lines / 5,426 bytes / SHA256 `482d16f5e00c069ce6ff3dc7a96ef7dc54eba9518980caaf21e660ea4682b12d`.

```text
.github/workflows/ci.yml
backend/README.md
backend/pom.xml
backend/src/main/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsOrderReader.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java
backend/src/main/java/com/onecall/core/entitlement_metering/internal/operationsread/JooqOperationsOrderIssuanceReader.java
backend/src/main/java/com/onecall/core/operations_iam/internal/orderread/DefaultOperationsOrderReadService.java
backend/src/main/java/com/onecall/core/operations_iam/internal/orderread/OperationsOrderCursorCodec.java
backend/src/main/java/com/onecall/core/operations_iam/internal/orderread/OperationsOrderReadConfiguration.java
backend/src/main/java/com/onecall/core/operations_iam/internal/orderread/OperationsOrderReadRequest.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsOrderReadController.java
backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsOrderReadPorts.java
backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsOrderReadService.java
backend/src/test/java/com/onecall/archtests/OperationsOrderReadConfigurationTests.java
backend/src/test/java/com/onecall/archtests/OperationsOrderReadWebTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferReadAclBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerOrderCreationBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/CustomerOrderReadBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogDraftBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/OperationsOrderReadBoundaryTests.java
backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java
backend/src/test/java/com/onecall/archtests/container/OperationsOrderReadFixture.java
backend/src/test/java/com/onecall/archtests/container/OperationsOrderReadRoleLeastPrivilegeIT.java
backend/src/test/java/com/onecall/archtests/container/OperationsOrdersReadIT.java
backend/src/test/java/com/onecall/archtests/operations/OperationsOrderReadTestProviders.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsOrderReaderTests.java
backend/src/test/java/com/onecall/core/entitlement_metering/internal/operationsread/JooqOperationsOrderIssuanceReaderTests.java
backend/src/test/java/com/onecall/core/operations_iam/internal/orderread/OperationsOrderCursorCodecTests.java
backend/src/test/java/com/onecall/core/operations_iam/internal/orderread/OperationsOrderReadRequestTests.java
backend/src/test/java/com/onecall/core/operations_iam/internal/orderread/OperationsOrderReadServiceTests.java
```

## 3. Production design, call paths and key state transitions

- `OperationsBoundaryFilter` (REQUEST dispatch) admits exactly nine GET-only `read` alternatives (seven retained siblings + the two exact order templates), under the shared `"GET".equals(method) && path != null` and `(contextPath + path).equals(requestURI)` gates; route denial precedes authentication. Employee bearer authentication runs upstream; the global `CustomerBoundaryFilter` passes the two canonical order GETs through unchanged (same request/response/principal, once, no customer verifier) before customer trace/auth/body/idempotency.
- ERROR-dispatch concealment (non-admission) is the plan-fix-7 bounded family: retained activations exact-TRACE (R1) plus `errorUri.matches(Pattern.quote(contextPath) + "/api/v1/operations/orders([/;%].*)?")` gated by type-safe (R2a) `ERROR_METHOD instanceof String errorMethod && !"GET".equals(errorMethod)` OR (R2b) `"GET".equals(ERROR_METHOD)` on a non-canonical alias (canonical-list/detail exclusion) carrying native `Integer.valueOf(400)`/`Integer.valueOf(405)`. A canonical GET application/internal failure is never concealed; the else-chain to `CustomerApiErrorController` and buffered endpoint error behavior are preserved. The family regex never leaks into the GET `read` gate.
- `OperationsOrderReadController` exposes exactly the two operation-named GET methods, trusted principal, immutable serialization after transaction completion.
- `DefaultOperationsOrderReadService` (lazy, profile `persistence`, conditional on existing `onecall.operations-subject-read.enabled`) reuses `operationsSubjectReadDataSource/TransactionTemplate/Dsl/Context` and `OperationsSubjectReadProperties`; read-only REQUIRES_NEW/REPEATABLE_READ, five-second budget, existing local timeouts. Authority first under `operations_iam_role`: malformed/null/out-of-CHECK role → 503, missing row → 401, valid DISABLED → 403, ACTIVE ADMIN/OPS → accepted; then parse/filter/Accept, key-free cursor structure, endpoint-isolated authenticated cursor (no limit/employee/TTL binding). ORDER port: one explicit-column root query with optional structured predicates + strict tuple seek + limit+1; service validates identity/order/filter/sentinel invariants; TRIAL port batches only emitted IDs; issuance status is the distinct PENDING/ISSUED/FAILED set projection (NONE/PENDING/ISSUED/FAILED/MIXED), payment-independent. Empty page skips issuance resolution. Detail directly constructs owner-local `new DefaultCustomerOrderSnapshotReader()` (no web bean/config), validates frozen snapshot/hash/row agreement; missing/unrepresentable detail → 404, unrepresentable list facts → 500, SQL/dependency/role/completion failures → 503. Exactly 11 summary / 16 detail fields; nullable paidAt/closedAt emitted as null. No `payment_order`/provider read.
- Role scopes witness one bound read-only repeatable-read backend/PID, restore owner → operations → login on success/denial/failure; guarded cleanup restores settings/readOnly/isolation/autocommit before pool reuse.

## 4. Independent filter expectations and frozen-remainder proof

`OperationsBoundaryFilter` pre-change baseline (HEAD blob, canonical-LF): 136 lines / 8,463 LF bytes / SHA256 `2eadb3c7bf7118408fb84defba1508b0ef7d10be4e8242c7210e70c01cf8d507` (the plan's pinned canonical-LF identity; the plan's raw CRLF worktree pin was `d9817c73…` / 8,599 bytes). The boundary test reconstructs this baseline from independently transcribed literal regions (`EXPECTED_PREFIX + BASELINE_ERROR + EXPECTED_BETWEEN + BASELINE_READ + EXPECTED_SUFFIX`) and asserts the pinned hash — no test-time Git, no baseline resource, no actual-derived expectation.

Two independently fixed complete expected final variants are defined and hashed in the boundary test:

- `EXPECTED_NO_ERROR_DELTA_SHA256 = 9b1515baa01e2c5bf47a50828e5365094e66304a41d83829d152ab752fbc80ec` (unchanged from retry-2).
- `EXPECTED_PROBE_REQUIRED_ERROR_DELTA_SHA256 = 9818a08d947121c36b2303ff6d054aad6d66935e0cb39f846d12deb2a5b99146` (re-measured this resume from the independently re-authored fix7 expected constant, not from production source).

Selected variant: `PROBE_REQUIRES_ERROR_DELTA = true`, anchored to the retained pre-decision native-405 TRACE evidence (§6). The actual production canonical-LF source equals the selected expected variant byte-for-byte and hashes to its pinned literal; the other variant is rejected by the selected-variant guard. Final `OperationsBoundaryFilter`: 146 lines / 9,645 raw worktree bytes (mixed CRLF/LF checkout) / raw SHA256 `cd00c28b7ed982aa992901c857d7f454adcd654e775e23169f4d367a853b55ad`; canonical-LF SHA256 `9818a08d947121c36b2303ff6d054aad6d66935e0cb39f846d12deb2a5b99146`.

`CustomerBoundaryFilter`: baseline canonical-LF `4c254b856c643f13a6f5f2172ecd16707daaa36f4e5a1e3f8e675657bf37e237` (214 lines); final canonical-LF `07786a892143817e1076be7aa2179b5e11deec567a7bfd28a875395d1a5319a3` (216 lines / 13,115 raw bytes / raw `1984a98b1fc3e08bc15bbb7f7b60289fcc26fbbda54bd8bdf5ad8e8d1c865e1c`). The guard reverses only the independently expected two-alternative GET addition and hashes the remainder to the pinned baseline; all other customer bytes frozen.

Exact cumulative diff vs HEAD is limited to: the two GET `read`/pass-through order alternatives in each filter, and the `OperationsBoundaryFilter` ERROR-branch replacement to the bounded family. Whole-file route-literal census = 9 read + 4 command + 4 ERROR = 17 (operations) and 9 GET + 4 command = 13 (customer). Non-route `Code.NOT_FOUND`→`Code.INTERNAL` mutation fails the frozen-remainder/whole-source and fixed-hash guards while route checks stay unchanged; the unselected valid variant fails the selected production guard.

## 5. Validation commands and honest results

Toolchain: repository Maven wrapper, `JAVA_HOME=/opt/jdk21` (Maven 3.9.9 / Java 21.0.13), Docker server/client 29.1.3, cached `postgres:16` image `sha256:3950ad7c3148ca2175ef1b6c36c9efbf78957ae8fbe8b3a5d7c35af50500382f` (PostgreSQL 16.16). `docker version` and `docker image inspect postgres:16` exited 0 before any product edit. Git exports per plan. All commands run from `backend`.

| Command | Exit | Result (run/fail/error/skip) |
| --- | --- | --- |
| `./mvnw -B -ntp -Dtest=OperationsOrderReadRequestTests,OperationsOrderCursorCodecTests,OperationsOrderReadServiceTests,JooqOperationsOrderReaderTests,JooqOperationsOrderIssuanceReaderTests,OperationsOrderReadWebTests,OperationsOrderReadConfigurationTests,OperationsOrderReadBoundaryTests test` | 0 | Surefire 34/0/0/0 |
| `./mvnw -B -ntp -Dtest=CustomerCatalogOfferReadAclBoundaryTests,CustomerOrderReadBoundaryTests,CustomerOrderCreationBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsOrderReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsCatalogDraftBoundaryTests,OperationsCatalogVersionBoundaryTests test` | 0 | Surefire 59/0/0/0 |
| `./mvnw -B -ntp -Dtest=ModuleBoundaryTests,CustomerOrderReadBoundaryTests,CustomerOrderCreationBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsCatalogDraftBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsCatalogProductReadBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,CustomerOrdersWebTests,CustomerWebConfigurationTests,CustomerApiBoundaryTests,CustomerSessionBoundaryTests,OperationsSubjectsWebTests,OperationsCatalogProductReadWebTests,OperationsCatalogVersionWebTests,OperationsCatalogDraftWebTests test` | 0 | Surefire 177/0/0/0 |
| `./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsOrderReadBoundaryTests -Dit.test=OperationsOrdersReadIT,OperationsOrderReadRoleLeastPrivilegeIT verify` | 0 | Surefire 6/0/0/0; Failsafe 11/0/0/0 (order 7 + role 4) |
| `./mvnw -B -ntp -Pcontainer-smoke clean verify` | 0 | Surefire **1198/0/0/0**; Failsafe **775/0/0/0**; total **1973/0/0/0**; BUILD SUCCESS (1:23 h) |
| CI inventory checker (extracted `assert container smoke actually ran` PY block) against fresh full XML | 0 | source=CI=POM=XML exact: 130/1198 + 48/775 = 178/1973; annotation lines 1974; POM 48; historical PA42 122/1164 + 46/764 and all older identities retained; zero skips/errors/failures |
| CI `default and opt-in web jar smokes` PYCODE block | 0 | default fail-closed boot + two web error-path HTTP campaigns; order canonical 401/503, all non-GET incl. TRACE 404, aliases 404, HEAD empty body |
| `jar tf … > target/jar-index.txt`; `./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt`; CI `production jar dependency firewall` PYCODE block | 0 | baseline 37/34, ssm 72/65, counterparts 64, dependency-only 8, packager 1; deltas 35/0 and 31/0; **116 adversarial probes**; packaged class/resource parity + 20 migrations byte-equal |
| `git diff --check` | 0 | no whitespace errors |

Earlier authoring iterations of the extended IT probe failed honestly before correction (HEAD body assertion; `List.of` null token; canonical GET wrongly placed in the alias-rejection loop). These were test-authoring fixes, not product/test weakening; the final commands above are green. The clean full verify is the authoritative inventory; focused Failsafe runs alone are not presented as full inventory proof.

Evidence logs (local, generated): `/tmp/pa43-full-verify.log` SHA256 `df1e8f15b96040b93e85f13ea335d43ce4834c88d90d5db0c8a1461c46c633a7`; CI inventory out `71914493c99bf564cd16c1f2107c3823e454e57f43a90dcd63890794e3d0aab6`; smoke out `cfd8797e32c32fa0a7e6d73ff44539cad9336083b55393863aa4c06ab7343540`; firewall out `981d7f7af1b485a8f7906a8177949d6bd87bc0bee66eb0724cf072593704c150`.

## 6. Retained pre-decision TRACE evidence and post-change family probes

The retry-2 blocker report (SHA256 `2956ebed633e7ad16c17ae04084a8d4b8221ee0b55ae5a05357977325646493a`, preserved at `/tmp/pa43-evidence/retry2-blocker-report.md`) established `PA43-TRACE-EXCLUDED-ALIAS-01`: canonical list/detail TRACE already returned 404 via the canonical ERROR union, but real `TRACE /api/v1/operations/orders/` produced a container-native 405 ERROR dispatch (`ERROR_METHOD=TRACE`, `ERROR_REQUEST_URI=/api/v1/operations/orders/`, `ERROR_STATUS_CODE=405`) that the canonical-only union did not recognize, chaining to sanitized 500. Decisive retained raw excerpt:

```text
PA43 observed ERROR attributes method=TRACE uri=/api/v1/operations/orders/ status=405 contextPath=
PA43 TRACE excluded-alias method=TRACE rawURI=/api/v1/operations/orders/ contextPath= status=500 headers={cache-control=[no-store], connection=[close], content-length=[127], content-type=[application/problem+json], ...} body={"type":"about:blank","title":"Internal error","status":500,"code":"INTERNAL","traceId":"15751605-9291-41a6-8c2f-b8022eea7c73"}
```

That native-405 witness selects `PROBE_REQUIRED_ERROR_DELTA`. This resume re-confirms the decision across the full rejected family. Post-change real-HTTP probes (full clean verify, `OperationsOrdersReadIT`, PostgreSQL 16.16; XML preserved at `/tmp/pa43-evidence/orders-it-fullverify.xml`):

- In-family TRACE → generic 404 NOT_FOUND problem+json for all 12 probes: canonical list, canonical detail, trailing `/`, doubled `//`, `//x`, matrix `;x`, `/x;y`, percent `/%2fx`, `/x%25`, extra `/first/second`, deeper `/a/b/c`, `/x/`.
- Non-GET methods (POST/PUT/PATCH/DELETE/HEAD/OPTIONS) on canonical + aliases → 404 (HEAD empty body, problem content-type).
- GET aliases with valid/absent/invalid employee credentials → 404 before auth; zero business/employee SQL (jOOQ witness counter == 0).
- TRACE canonical with valid/absent/invalid credentials → 404.
- Sibling activations TRACE → 404 (retained R1). Sibling subjects/ivr-sessions/catalog/products TRACE → existing sanitized 500 INTERNAL (preserved). Native ERROR witness for these chained dispatches: `method=TRACE status=405` with raw URI.
- Outside bounded family (preserved existing sanitized 500 INTERNAL, recorded per class): lookalikes `ordersfoo`, `orders-x`; dot-segment `/api/v1/operations/./orders`; letter-percent `/api/v1/operations/%6frders`; case-variant `/api/v1/operations/ORDERS`.

The bounded family satisfies the user-confirmed 404 contract for every in-family rejected alias/method; outside-family classes keep their existing results per plan §10's carve-out. No stop condition triggered.

## 7. PostgreSQL16 / ACL / no-write / role-restoration evidence

Fixture composes existing migration/provisioning support; application reads run as existing NOINHERIT `pa36_operations`, never an admin request connection; fixture-admin seeding/ANALYZE/inventory separated from requests. All 20 V001–V020 migrations apply and validate (PostgreSQL 16.16).

`OperationsOrderReadRoleLeastPrivilegeIT` (4/0/0/0 in full verify): exactly seven existing LO SET edges (activation_consent_role, catalog_order_role, entitlement_metering_role, evidence_role, ivr_role, operations_iam_role, subject_number_role), no admin/inherit option; PA25 eleven total graph edges; exact existing 449-tuple ACL oracle, no new grants. No direct LO order SELECT; no operations_iam_role order/issuance SELECT; owner/customer/platform/service escalation denied 42501 using actual existing role names. Read-only transaction denies INSERT/UPDATE/DELETE/FOR UPDATE/FOR SHARE 25006; DDL/default-ACL escalation denied 42501 separately. ORDER/TRIAL scopes witness same backend PID/session login, expected current role, repeatable-read/read-only; restore operations after each scope and synthetic owner failure, then login on exit; next pooled checkouts assert login/current-user, autocommit, readOnly, isolation and three timeout baselines. Real CHECK constraints reject impossible employee role and PAID-without-paidAt; nothing disabled.

`OperationsOrdersReadIT` (7/0/0/0): actual HTTP ADMIN/OPS exact 11/16-field projection, current mutable payment status, structured filters + cross-employee/page-size cursor continuation, authorization/validation/dependency no-write outcomes, list-500/detail-404 corruption split, latch-based concurrent committed payment/issuance isolation (invisible in current snapshot, visible next request), and dense EXPLAIN. Successful and explicit-error campaigns compare exact all-table row digests plus security inventory (roles/memberships/settings, namespace/relation/column owners/ACLs, defaults, constraints/triggers/functions, sequences, database settings, Flyway facts); fixture/concurrent-writer changes outside comparisons.

## 8. Measured owner-query EXPLAIN (full clean verify)

`explainsActualPreparedQueriesWithDensePostgres16Fixtures` passed: fixture seeds 10,001 orders (repeated timestamps, dense subject, two payment states) and 10,100 issuance rows (100 for one root); admin ANALYZE outside measurement. 20 actual prepared owner queries/binds captured (11 ORDER + 9 TRIAL), EXPLAIN ANALYZE BUFFERS JSON inside the same read-only owner scopes, all-table inventory unchanged. Empty page adds no issuance query. Observations (loops 1, shared-read 0; not pass thresholds):

| # | Role | Rows | Root node | Shared hit | Planning ms | Execution ms |
| --- | --- | ---: | --- | ---: | ---: | ---: |
| 1 | ORDER | 101 | Limit (Sort/Seq Scan top-N) | 1667 | 0.045 | 4.690 |
| 2 | TRIAL | 100 | Seq Scan | 166 | 0.095 | 0.645 |
| 3 | ORDER | 101 | Limit | 1667 | 0.088 | 7.608 |
| 4 | TRIAL | 199 | Seq Scan | 166 | 0.090 | 0.655 |
| 5 | ORDER | 101 | Limit | 1667 | 0.050 | 3.043 |
| 6 | TRIAL | 100 | Seq Scan | 166 | 0.091 | 0.649 |
| 7 | ORDER | 101 | Limit | 1667 | 0.072 | 4.765 |
| 8 | TRIAL | 199 | Seq Scan | 166 | 0.106 | 0.935 |
| 9 | ORDER | 101 | Limit | 1667 | 0.076 | 4.442 |
| 10 | TRIAL | 100 | Seq Scan | 166 | 0.098 | 0.672 |
| 11 | ORDER | 101 | Limit | 1667 | 0.071 | 6.972 |
| 12 | TRIAL | 199 | Seq Scan | 166 | 0.106 | 0.927 |
| 13 | ORDER | 101 | Limit (Incremental Sort/Index Scan) | 104 | 0.056 | 0.175 |
| 14 | TRIAL | 100 | Seq Scan | 166 | 0.102 | 0.643 |
| 15 | ORDER | 101 | Limit (Incremental Sort/Index Scan) | 204 | 0.082 | 0.277 |
| 16 | TRIAL | 199 | Seq Scan | 166 | 0.096 | 0.648 |
| 17 | ORDER | 1 | Index Scan | 3 | 0.038 | 0.024 |
| 18 | TRIAL | 100 | Bitmap Heap Scan | 5 | 0.044 | 0.031 |
| 19 | ORDER | 0 | Index Scan | 2 | 0.040 | 0.016 |
| 20 | ORDER | 0 | Limit | 2 | 0.044 | 0.019 |

General operations pagination can scan/sort despite LIMIT; issuance batching bounds IDs not fan-out rows. No forced planner settings, no new index, no production-scale latency guarantee. Backend/Database owns scale/fan-out/index follow-up outside this no-migration slice.

## 9. Frozen-input, aggregate/inventory, package and firewall results

Frozen inputs (raw worktree SHA256 separately from canonical-LF; repo-root-relative sorted manifests, two-space separator): 36 unique files (20 migrations, 10 `api/openapi`, docs12/14/15/17, PA25 SQL + runbook). Every file's canonical-LF bytes equal the HEAD blob — **zero drift**. Raw manifest 36 lines / SHA256 `f6dbc48948af921a5378389565e155ba2eeb24427d5e5823135c61aec388384d`; canonical-LF manifest 36 lines / SHA256 `d2ec0373fff8ef217219a95078f28747354e71bbf2a8096a8c8d0b1c1c96ea09`. All 59 other customer production Java sources equal HEAD (canonical-LF); their sorted canonical-LF manifest SHA256 `b24875c8cda549cbcddc83f017f46978a5d0b7b569b506f3cea2e3287a4224f1` (matches retry-2). `CustomerBoundaryFilter` delta limited to the two GET alternatives (§4).

Measured source bookkeeping (final): 341 production Java, 228 test Java, 23 resources, 20 migrations; 130 unit suites / 1,198 methods, 48 IT suites / 775 methods = 178 / 1,973; 1,974 annotation lines (incl. retained historical comment); 48 POM includes. Sorted class#method identity SHA256 `05adee92f5e86c2083ab4f5edf128e61097498bafba8d7f0f3e8c3b9bd27ae70`. Added 34 unit + 11 IT identities vs PA42 baseline; no existing identity removed.

Current canonical-LF product/resource aggregates (re-measured from final bytes; member counts unchanged): 364 all `65235e7bb26f10850efd2dd2629e8baf161e04119a9b93715d5e7558cc98969b`; 362 excluding V019/V020 `57db7f771d90b37c5c969b65a206682d21f21f344f1112c97f57818d753a0ec1`; 361 excluding V018/V019/V020 `db80b3a3e9218b37cd856243da37bc82009e51b844c771eb22d1e4f804fb3090`. Only these current aggregates and current CI/POM pins were re-pinned; all frozen V001–V020/OpenAPI/docs12/14/15/17/PA25 bytes, historical baselines/pins, owner lists and behavioral assertions unchanged. README current canonical-LF SHA256 `c107d05d74f1757e70ed6678f9d054be14c85e38209b9e5413c366981885f7db` (unchanged); both IVR current pin slots use it; current composite `d7ecca7f6b46570134add374bdd4650f84ce4ecdea0cbc0b54b8a1e943ab5575` (unchanged); FROZEN table/consumers==4/four path tokens intact.

Package proof (firewall block, executed): executable JAR SHA256 `c82f0243fa10add47660b055cf2ebecb3e8247c879f1d76da23a90ab5c07c56f`, 40,247,376 bytes. Complete `BOOT-INF/classes` set equals `target/classes`, every entry byte-equal, no duplicates; all 20 migrations exactly once and byte-equal; new order classes present; no fixture/test/verifier-adapter classes; no `CustomerSessionVerifier`/`EmployeeSessionVerifier`/`SecurityFilterChain` interfaces in production classes; no PA25/ops resources packaged. Runtime dependency inventory equals approved (no dependency delta); 116 adversarial firewall probes passed.

## 10. Documentation decision

Only `backend/README.md`'s current PA43 capability section is in scope; its bytes are unchanged this resume (it delegates exact measured results to this artifact and carries no hard-coded method counts), so no design/OpenAPI/Control/PA25/runbook edit and no README re-pin was needed. No separate closeout is authored here; Controller decides gate actions.

## 11. Residual risks / uncovered areas and destinations

- Outside-family TRACE aliases (dot-segment/traversal, letter-level percent-encoding, case variants, prefix-lookalikes) retain existing sanitized 500; the bounded family deliberately does not conceal them (plan §10 carve-out; re-review-7 B finding 3). Owner Backend/Security; per-class evidence recorded in §6; any later concealment widening is a separate hardening unit, not this slice.
- R2b GET native-rejection trigger: real GET aliases return 404 and the GET-alias native-400/405 path is proven by real-filter fixtures; the precise native ERROR dispatch attribution for concealed GET aliases is not separately witnessed server-side (the witness observes only chained dispatches). Owner Backend/Test Maintenance; structural fixtures + real 404 outcomes bound the behavior.
- Existing LO can SET command-capable owner roles; the application transaction enforces read-only (accepted security boundary, not column-only/read-only DB roles). Security/Database owns later dedicated read-role hardening.
- No optimized global operations keyset index; issuance fan-out unbounded in row count. Backend/Database records EXPLAIN/deadlines; index/schema work deferred.
- Authority is one request's repeatable snapshot, not linearizable revocation/RLS/cross-page snapshot. Security/Backend; tests prove next-request visibility.
- Shared operations cursor key ring custody/rotation/replica consistency; production provisioning/key assignment/TLS/live validation **NOT EXECUTED**. Security/Operations/Platform.
- `origin/main` differs from supplied checkpoint HEAD — nonbehavioral provenance owned by Controller; exact hashes recorded; no remote mutation/reset.

## 12. Completion status

**COMPLETE for the accepted S1 slice.** Both endpoints and the bounded rejected-alias ERROR contract pass focused, clean full, ordered CI inventory, packaged-JAR smoke/parity and dependency-firewall validation with zero failures/errors/skips; frozen/unrelated deltas are zero; final inventories reconcile. No source outside the allowlist, no customer behavior change beyond the sole exact GET pass-through, no contract/migration/PA25 modification, no production/live validation claim. No stage/commit/amend/push/PR/merge/approval/ready/reviewer/external-comment action occurred; no future review artifact was read or authored. Remaining gate advancement is Controller-owned.

## 13. Code review fix1 — accepted findings B F1/F3/F4/F6 resolved

- Task: `pa43-code-review-fix1-qwen4max-20261127-180000`; gate: `code review fix1`; date 2026-11-27.
- Frozen inputs re-verified before/after: HEAD `bf7d2c0ce3a753e202ad08abcc6149f2d68f386f` (nothing staged); accepted plan SHA256 `6604a175ad46ceeab8b0557596cd81d67344edd7d62bbfb77c82df0e8103b804`; Control SHA256 `9d54c904ea1749642a84db976dddd5324e1b8f77a8e5f08f7232929f0c9ff79d` (pre-existing worktree modification, untouched); implementation-artifact input SHA256 `98340858e2a56e099b3e1e79c91373e8b0215edfb7ea7e4154087ec454eb9849`; Review A `docs/reviews/code-review-20261127-124517.md` SHA256 `a8868d51ba47f38dbad2cd1c2cca4eacff41c4617aede5ee5969cef8a9f4b050`; Review B `docs/reviews/code-review-20261127-125221.md` SHA256 `3960334b6e9ce86fc765394bd2f17568f57f3e91c478772bff3e9445563a6cea`. No plan/Control/review byte changed.
- Scope: only allowlisted paths changed. Final changed set (git diff + untracked backend, excluding this report and Controller-owned Control) is byte-for-byte the same 34-path set as §2; no path added/removed.

### 13.1 Accepted finding resolutions

1. **B F1 (Medium) — real deterministic native ERROR witness.** `OperationsOrderReadFixture.Sessions` now registers a recording `ErrorWitness` bean at `Ordered.HIGHEST_PRECEDENCE` and a test-only static `BeanPostProcessor` pins the production `operationsErrorBoundaryFilterRegistration` to `HIGHEST_PRECEDENCE + 1`, so the witness is strictly and deterministically first on every ERROR dispatch (production source bytes unchanged; these are the only two ERROR-dispatch filters). The witness records `ERROR_METHOD/ERROR_REQUEST_URI/ERROR_STATUS_CODE/contextPath` per dispatch and still prints the retained `PA43 observed ERROR attributes` line. `OperationsOrdersReadIT.traceProbeSelectsExactErrorDispatchVariant` now asserts, per probe, exactly one server-side witness row with the expected method/raw URI/native 405 for all 12 in-family TRACE aliases, the 6 canonical TRACE credential variants, retained activations R1, and all 8 outside-family chained dispatches; REQUEST-dispatch rejections (non-GET methods, GET aliases with all credential variants) assert an empty witness drain. The `not-visible-at-client` substitute literal is deleted. Focused Failsafe XML records 27 witness lines (12 family + 6 canonical-credential + 1 activations + 8 outside-family), each asserted. Canonical-GET application-failure preservation is unchanged (production filter bytes identical; real-filter fixtures and IT 500/503 chains still green).
2. **B F3 (Medium) — single payment-status truth + explicit port-rejection classification.** `OperationsOrderReadPorts` (operations-owned contract) now exposes the sole authoritative `Set<String> PAYMENT_STATUSES = Set.of("UNPAID","UNKNOWN","PAID","CLOSED")`. `OperationsOrderReadRequest.parse`, `JooqOperationsOrderReader.fetchOrders` and `DefaultOperationsOrderReadService.validate` all reference that one constant; the two duplicate literal sets are deleted. `DefaultOperationsOrderReadService.execute` classifies `IllegalArgumentException` anywhere in the cause chain as `SERVICE_UNAVAILABLE` (same family as the sibling subject-read `catch (RuntimeException) → unavailable()`; no accepted-plan evidence maps port contract rejection to a stricter code). New service-level test `OperationsOrderReadServiceTests.ownerPortContractRejectionIsServiceUnavailableNotInternal` makes each owner port (list/detail/issuance) throw `IllegalArgumentException` and asserts `Failure(SERVICE_UNAVAILABLE)` — never the generic `INTERNAL` path. The issuance-status domain `PENDING/ISSUED/FAILED` stays service-validated per plan §7 (unchanged).
3. **B F4 (Low) — vacuous truncation assertions replaced.** Both self-equality lines in `OperationsOrderReadBoundaryTests` are replaced with substantive assertions: the truncated text excludes both order alternatives, and truncation cuts inside a route literal so `tokens(...)` fails with `complete Java literal` and `readSentinels(...)` fails — moving the truncation point now fails the guard.
4. **B F6 (Low) — semantic file selection + working-directory independence.** `PRODUCTION.get(7/8/4)` replaced by unique-suffix selection `productionFile("JooqOperationsOrderReader.java" / "JooqOperationsOrderIssuanceReader.java" / "DefaultOperationsOrderReadService.java")` (asserts exactly one match). `MAIN` and the OpenAPI path resolve from a `backendRoot()` walk (pom.xml + src/main/java/com/onecall/core, or nested `backend/`), independent of the Maven working directory.

Deferred by Controller, untouched: A F1 shared-fate sibling-test delegation; B F2 repository-wide inventory/CI coupling (mechanism retained; only current pins re-measured); B F5 duplicated ERROR behavior fixture. Owner: Backend/Test Maintenance, later hardening work unit.

### 13.2 Inventory re-pins (current values only; mechanism/frozen members unchanged)

Production bytes changed in exactly 4 allowlisted files (Ports/Request/Reader/DefaultService); one unit @Test method added. Re-measured with the exact existing algorithms (verified by reproducing every pre-fix1 pin byte-for-byte from reversed deltas):

- 362-member aggregate (excl. V019/V020): `c43ef606dd607554d57d5a5ffd6b9231c7809e03a2f0937da3268656b5d53f36` (was `57db7f77…`).
- 364-member aggregate: `db073c4bf799d1c908dddb87289c63e77759e7c2d9fec34a515e8620b5163e9a` (was `65235e7b…`).
- 361-member aggregate (excl. V018/V019/V020): `b5d0c27533771fdae540de84e9f4f834d90ca35db5720c5416797a0e2675043d` (was `db80b3a3…`).
- Method identity manifest: `450d9279fec0c23a343f8916e41f8526a7771208cd391c3e8376a1e39b5a9213` (was `05adee92…`); unit 1198→1199, IT 775 unchanged, total 1973→1974, annotation lines 1974→1975.
- `ci.yml`: `pa43_unit_additions` gains exactly `ownerPortContractRejectionIsServiceUnavailableNotInternal`; current totals/print/`exact_inventory` updated to 1199/775/1974/1975; PA43-start reconstruction still `[1164, 764]` / 1929; all historical baselines, frozen members, POM 48 includes, suite counts 130/48 unchanged.
- Re-pinned sibling guards: `CustomerCatalogOfferReadAclBoundaryTests` (362/364 hashes, unit/annotation counts, identity hash, CI literals), `CustomerOrderReadBoundaryTests` (CI literals), `EvidenceRoleIvrAssociationAclBoundaryTests` (361 hash, CI literals). `CustomerOrderCreationBoundaryTests`, `OperationsCatalogDraft/VersionBoundaryTests` (341/228 counts) and both IVR README-current pin slots/composite are unchanged because their measured inputs did not change. README bytes unchanged (`c107d05d…` current canonical-LF pin still valid).
- Frozen remainder: 36-file frozen raw manifest `f6dbc489…` and canonical-LF manifest `d2ec0373…` byte-identical to retained evidence; `OperationsBoundaryFilter` raw `cd00c28b…`/canonical `9818a08d…` and `CustomerBoundaryFilter` raw `1984a98b…`/canonical `07786a89…` unchanged; all 59 other customer production Java sources byte-equal to HEAD (canonical-LF), zero drift.

### 13.3 Final 34-path manifest (post-fix1)

Sorted 34-path canonical-LF content manifest (`sha256(lf(bytes))`, two spaces, repo-relative path, LF): 34 lines / 5,426 bytes / SHA256 `c4e14a1625601e4209224e00d08147d896ca8a25f7558892712de9ca7aca7392` (path set identical to §2; §2's `482d16f5…` is the pre-fix1 historical manifest). Full manifest retained at `/tmp/pa43-fix1-evidence/changed-34-canon.manifest`.

### 13.4 Validation (fix1, all executed)

Environment: WSL Linux JDK 21.0.13 (Temurin), Maven wrapper 3.9.9, Docker 29.1.3 linux/amd64, Testcontainers `postgres:16` (PostgreSQL 16.16), working directory `backend/` unless stated.

| Command / check | Exit | Result |
| --- | --- | --- |
| `./mvnw -B -ntp -Dtest=OperationsOrderReadServiceTests,OperationsOrderReadRequestTests,JooqOperationsOrderReaderTests,JooqOperationsOrderIssuanceReaderTests,OperationsOrderCursorCodecTests,OperationsOrderReadBoundaryTests,OperationsOrderReadWebTests,OperationsOrderReadConfigurationTests test` | 0 | Surefire **35/0/0/0** (34 prior + 1 new F3 service test) |
| `./mvnw -B -ntp -Dtest=ModuleBoundaryTests,CustomerOrderReadBoundaryTests,CustomerOrderCreationBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsCatalogDraftBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsIvrSessionReadBoundaryTests test` | 0 | Surefire **62/0/0/0** (re-pinned guards + real module verification) |
| `./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsOrderReadBoundaryTests -Dit.test=OperationsOrdersReadIT,OperationsOrderReadRoleLeastPrivilegeIT verify` | 0 | Surefire 6/0/0/0; Failsafe **11/0/0/0** (orders 7 + role 4); 27 asserted native ERROR witness lines in orders XML |
| `./mvnw -B -ntp -Pcontainer-smoke clean verify` | 0 | Surefire **1199/0/0/0**; Failsafe **775/0/0/0**; total **1974/0/0/0**; BUILD SUCCESS (**1:18 h**); `grep -c 'Skipped: [1-9]'` = 0 |
| CI inventory checker (current `assert container smoke actually ran` PY block re-extracted from edited `ci.yml`) against fresh full XML | 0 | source=CI=POM=XML exact: **130/1199 + 48/775 = 178/1974**; annotation lines **1975**; POM **48**; PA43-start reconstruction `[1164, 764]`/1929 and all historical identities retained; zero skips/errors/failures |
| CI `default and opt-in web jar smokes` PYCODE block (re-extracted) | 0 | default fail-closed boot + web error-path HTTP campaigns passed; no DB/verifier adapter |
| `jar tf … > target/jar-index.txt`; `./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt`; CI `production jar dependency firewall` PYCODE block (re-extracted) | 0 | baseline 37/34, ssm 72/65, counterparts 64, dependency-only 8, packager 1; deltas 35/0 and 31/0; **116 adversarial probes**; `BOOT-INF/classes` parity + 20 migrations byte-equal. Executable JAR SHA256 `1d2cba0a8e8e081a0693d5baed88ec6e75413e323a52ed10b78407d5d7dedf00`, 40,247,507 bytes |
| `git diff --check` | 0 | no whitespace errors |
| F6 working-directory proof: JUnit Platform launcher run of `OperationsOrderReadBoundaryTests` with CWD = repo root (outside Maven) | 1 (expected) | **5/6 pass from repo root**, including the F6 inventory test and PA43-owned OpenAPI resolution; the single failure is `frozenOpenApiPropertiesRequiredSetsAndMigrationsRemainExact` → `NoSuchFileException: src/main/java` inside the **deferred A F1** sibling-method delegation (`CustomerCatalogOfferReadAclBoundaryTests`), not in PA43-owned path resolution. Under the normal `backend/` CWD all 6 pass. |

Honest authoring note: the first run of the new F3 service test failed (`IllegalArgumentException` leaked from a Mockito `when(...)` re-stub of an already-throwing mock); fixed by `doReturn(...).when(...)` re-stubbing — a test-authoring correction, not a product/test weakening. No check requested by the gate was left unexecuted; production provisioning/key assignment/TLS/live validation remains **NOT EXECUTED** (§11, unchanged).

Evidence (local, generated; SHA256): full-verify log `/tmp/pa43-fix1-full-verify.log` `510d07e86371a813ffc46e8ab2b3fa14b06756529d3295a6cdbce9dcdb434c0c`; focused unit log `355b09deaaf4650477455787ac7afa761052c4c36d698a6f6e05babd7af8d807`; guard log `5ea4b9c3ee2d30b083e7298f94146ed7ef939f09050c8aeb31c0e15752fc424a`; focused IT log `3029e781a3fb8f3a351a7f086513831506817cf087d5d2c754bace0ec27e0bd9`; CI inventory out `bd4695eef12a540fd3d34208e84c995a5dda0cebebb270e51f20cdb0487e4e47`; smoke out `cfd8797e32c32fa0a7e6d73ff44539cad9336083b55393863aa4c06ab7343540` and firewall out `981d7f7af1b485a8f7906a8177949d6bd87bc0bee66eb0724cf072593704c150` (both byte-identical to the retained pre-fix1 outputs — deterministic campaigns, unchanged results); full-verify orders IT XML `06133ff4d40c4876a9db5d3838567e650da9d2f27483e8149336d7dd634f8934` (27 witness lines); root-CWD check out `ee6ec5df24533a5051f7f78255827358e251572a4c04fd99ff1020f19a517fc3`; 34-path manifest `c4e14a1625601e4209224e00d08147d896ca8a25f7558892712de9ca7aca7392`. All under `/tmp/pa43-fix1-evidence/`; prior `/tmp/pa43-evidence/` and `/tmp/pa43-full-verify.log` retained untouched.

### 13.5 Residual risks / deferred ownership (updated)

- B F1 residual closed: in-family rejected aliases now have asserted server-side native ERROR attribution; witness order is explicit, not tie-break dependent. R2b GET-alias native-400/405 remains proven by real-filter fixtures plus real 404 outcomes (no real Tomcat GET alias produces a native 400/405 ERROR dispatch in the current matrix; unchanged from §11).
- A F1 (shared-fate sibling delegation), B F2 (global inventory/CI coupling), B F5 (duplicated ERROR behavior fixture) remain deferred to Backend/Test Maintenance in a later hardening work unit; this fix1 intentionally re-pins within the existing mechanism instead of refactoring it.
- All §11 residuals unchanged: outside-family TRACE sanitized-500 carve-out (Backend/Security), LO SET ROLE boundary (Security/Database), no global keyset index / issuance fan-out (Backend/Database), request-snapshot authority (Security/Backend), shared cursor keyring custody/rotation + production provisioning/TLS/live validation NOT EXECUTED (Security/Operations/Platform), origin/main provenance (Controller).
- The prior report's 59-file customer aggregate `b24875c8…` and the gate-supplied 34-path manifest `6b690438…` encodings remain unreproducible (Review B Open Question 1); fix1 instead directly verified per-file byte equality (59/59 vs HEAD; 36/36 frozen manifests byte-identical) and publishes its own manifest encoding above. Controller owns clarification.

## 14. Code review fix2 — accepted re-review B findings F1/F2/F3 resolved

- Task: `pa43-code-review-fix2-qwen4max-20261127-210000`; gate `code review fix2`; input re-review B `docs/reviews/code-review-20261127-153655.md` (SHA256 `e1827de41f5e262d27a2b0305773da927eade334f6236428b313addbf6b58949`, re-measured). Gate HEAD `bf7d2c0ce3a753e202ad08abcc6149f2d68f386f`, plan `6604a175…`, Control `5d321ff5…`, pre-fix2 artifact 250 lines / 43,862 B / `f930bb07…` all re-measured and matched before editing. §§1–13 are retained history; this §14 is the current authoritative fix2/validation record.
- Fix2 changed exactly 13 of the 34 allowlisted paths (4 production + 8 test + `ci.yml`), verified by joining the retained post-fix1 manifest against the post-fix2 manifest (path set identical, 21 paths byte-identical). No path added/removed; no plan/Control/review/private/unrelated file touched. Deferred A F1, B F2 inventory-coupling mechanism and B F5 duplicated fixture remain untouched (fix2 re-pins current values within the existing mechanism only).

### 14.1 Accepted finding resolutions

1. **B F1 (Low) — narrow port-rejection classification.** `OperationsOrderReadPorts` now owns `final class ContractRejectionException extends IllegalArgumentException` (nested contract exception, same style as `OperationsSubjectReadService.Failure`). The three owner-port rejection sites throw exactly that type: `JooqOperationsOrderReader.fetchOrders` ("Invalid order query"), `fetchOrder` ("Missing order identity"), `JooqOperationsOrderIssuanceReader.fetchIssuances` ("Invalid order batch"). `DefaultOperationsOrderReadService.execute` cause-chain classification now matches only `ContractRejectionException` for `SERVICE_UNAVAILABLE`; the blanket `IllegalArgumentException` arm is deleted, so other `IllegalArgumentException` (incl. `NumberFormatException`) and generic runtime failures keep `Code.INTERNAL`. All other classification arms (SQLException/jOOQ/Spring DAO/Transaction/Beans) and the known-`Failure` passthrough are unchanged. Proof: `OperationsOrderReadServiceTests.ownerPortContractRejectionIsServiceUnavailableNotInternal` now throws the contract type from each owner port (list/detail/issuance) plus a wrapped `IllegalStateException(ContractRejectionException)` case — all `SERVICE_UNAVAILABLE`; new `nonPortIllegalArgumentFailuresRemainInternalNotServiceUnavailable` throws wrapped/plain `IllegalArgumentException` and a `NumberFormatException` subclass from list/detail/issuance paths — all `INTERNAL`. Both owner-reader unit tests now assert `isExactlyInstanceOf(ContractRejectionException.class)` for every rejection input.
2. **B F2 (Low) — explicit ERROR order-pin assertion.** `OperationsOrderReadFixture.start()` now calls `assertErrorWitnessOrderPin()` before any IT body runs (both orders and role ITs share the fixture): the ERROR-dispatch `FilterRegistrationBean` set must be exactly {`errorWitnessRegistration`, `operationsErrorBoundaryFilterRegistration`}; the production registration must exist with effective order `Ordered.HIGHEST_PRECEDENCE + 1`; the witness must be `HIGHEST_PRECEDENCE` and strictly before production. Failure messages name the bean and state that the bean-name/order pin did not apply. Verification point executed: mutating the BeanPostProcessor name literal to a non-existent bean made `OperationsOrdersReadIT.start` fail with the explicit diagnostic ("production ERROR filter order pin not applied … expected: -2147483647 but was: -2147483648"); the mutation was reverted and the full clean verify is green. Production filter/registration bytes are unchanged.
3. **B F3 (Low) — payment-status single-truth source guard.** `OperationsOrderReadBoundaryTests.ownerOnlySqlNamedInterfacesAndExactProductionInventoryStayBounded` now runs `paymentStatusSingleTruth` over all 9 PA43 production sources: the exact definition `Set<String> PAYMENT_STATUSES = Set.of("UNPAID", "UNKNOWN", "PAID", "CLOSED")` appears exactly once and only in `OperationsOrderReadPorts`; exactly the three intended consumers (`OperationsOrderReadRequest`, `JooqOperationsOrderReader`, `DefaultOperationsOrderReadService`) each reference `OperationsOrderReadPorts.PAYMENT_STATUSES` exactly once; every other PA43 production file has zero references; the `"UNPAID"` literal appears exactly once across the whole PA43 production set (contract only). Three inline mutants must fail and do: consumer literal re-duplication, contract domain narrowing, and an unqualified consumer reference.

### 14.2 Inventory re-pins (current values only; mechanism/frozen members unchanged)

Production bytes changed in exactly 4 allowlisted files (Ports/Reader/IssuanceReader/DefaultService); one unit @Test method added. Re-measured with the exact existing algorithms; every pre-fix2 pin was reproduced byte-for-byte from reversed deltas before re-pinning (362/364/361 aggregates and the identity manifest all matched `c43ef606…`/`db073c4b…`/`b5d0c275…`/`450d9279…`):

- 362-member aggregate (excl. V019/V020): `36e4bf3bdfdbeaaf93521fe03eed5ed5673a2791d00e45a0a3eb756c95241eba` (was `c43ef606…`).
- 364-member aggregate: `cbeed5adbd2b3d57cd43c425df60ba1ce0a1048fd1f89ea1a702bf610fd22b23` (was `db073c4b…`).
- 361-member aggregate (excl. V018/V019/V020): `39f2b9a5db96e8d5261cd968ceb81b334ed88ca41164e1c7d8ad397153805ea9` (was `b5d0c275…`).
- Method identity manifest: `f8c75479e759e048c57962aac7e011ac2fe77c5030364d26ccba5d511921816d` (was `450d9279…`); unit 1199→1200, IT 775 unchanged, total 1974→1975, annotation lines 1975→1976.
- `ci.yml`: `pa43_unit_additions` gains exactly `nonPortIllegalArgumentFailuresRemainInternalNotServiceUnavailable`; current totals/print/`exact_inventory`/Surefire-count gate updated to 1200/775/1975/1976; PA43-start reconstruction still `[1164, 764]` / 1929; all historical baselines, frozen members, POM 48 includes, suite counts 130/48 unchanged.
- Re-pinned sibling guards: `CustomerCatalogOfferReadAclBoundaryTests` (362/364 hashes, unit/annotation counts, identity hash, CI literals), `CustomerOrderReadBoundaryTests` (CI literals), `EvidenceRoleIvrAssociationAclBoundaryTests` (361 hash, CI literals). `CustomerOrderCreationBoundaryTests`, `OperationsCatalogDraft/VersionBoundaryTests`, both IVR suites/README pin slots and `backend/README.md` bytes unchanged (`c107d05d…` current canonical-LF pin still valid).
- Frozen remainder: all 36 frozen files re-hashed against the retained `/tmp/pa43-evidence/frozen-{raw,canon}.manifest` — 36/36 match, 0 drift (raw manifest `f6dbc489…`, canonical `d2ec0373…`); `OperationsBoundaryFilter` raw `cd00c28b…`/canonical `9818a08d…` and `CustomerBoundaryFilter` raw `1984a98b…`/canonical `07786a89…` unchanged; plan/Control/re-review bytes equal the gate inputs; no migration/OpenAPI/docs12/14/15/17 file differs from HEAD.

### 14.3 Final 34-path manifest (post-fix2)

Sorted 34-path canonical-LF content manifest (`sha256(lf(bytes))`, two spaces, repo-relative path, LF): 34 lines / 5,426 bytes / SHA256 `b12e98cab4aff5db390e28a2178cdf636e962dea3ecdb33e95d8a6f3be2c38df` (path set identical to §2; §13.3's `c4e14a16…4209224…` is the retained post-fix1 historical manifest). Full manifest retained at `/tmp/pa43-fix2-evidence/changed-34-canon.manifest`.

### 14.4 Validation (fix2, all executed)

All commands from `backend/` with Linux JDK `/opt/jdk21` (Temurin 21.0.13) and the Maven wrapper; PostgreSQL 16.16 via Testcontainers/Docker.

| Command / check | Exit | Result |
| --- | --- | --- |
| `./mvnw -B -ntp -Dtest=OperationsOrderReadServiceTests,OperationsOrderReadRequestTests,JooqOperationsOrderReaderTests,JooqOperationsOrderIssuanceReaderTests,OperationsOrderCursorCodecTests,OperationsOrderReadBoundaryTests,OperationsOrderReadWebTests,OperationsOrderReadConfigurationTests test` | 0 | Surefire **36/0/0/0** (35 prior + 1 new F1 negative test; service suite 10) |
| `./mvnw -B -ntp -Dtest=ModuleBoundaryTests,CustomerOrderReadBoundaryTests,CustomerOrderCreationBoundaryTests,CustomerCatalogOfferReadAclBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests,OperationsCatalogDraftBoundaryTests,OperationsCatalogVersionBoundaryTests,OperationsIvrSessionReadBoundaryTests test` | 0 | Surefire **62/0/0/0** (re-pinned guards + real module verification) |
| `./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsOrderReadBoundaryTests -Dit.test=OperationsOrdersReadIT,OperationsOrderReadRoleLeastPrivilegeIT verify` | 0 | Surefire 6/0/0/0; Failsafe **11/0/0/0** (orders 7 + role 4); 27 asserted native ERROR witness lines in orders XML |
| F2 verification point: BeanPostProcessor name literal mutated to a non-existent bean, then `-Dit.test=OperationsOrdersReadIT#traceProbeSelectsExactErrorDispatchVariant verify` | 1 (expected) | `OperationsOrdersReadIT.start` fails with the explicit diagnostic "production ERROR filter order pin not applied: bean-name/order pinning did not match operationsErrorBoundaryFilterRegistration … expected: -2147483647 but was: -2147483648"; mutation reverted byte-exactly; full clean verify green afterwards |
| `./mvnw -B -ntp -Pcontainer-smoke clean verify` | 0 | Surefire **1200/0/0/0**; Failsafe **775/0/0/0**; total **1975/0/0/0**; BUILD SUCCESS (**01:20 h**); `grep -c 'Skipped: [1-9]'` = 0; full-verify orders IT XML carries the same 27 witness lines |
| CI inventory checker (current `assert container smoke actually ran` PY block re-extracted from edited `ci.yml`) against fresh full XML | 0 | source=CI=POM=XML exact: **130/1200 + 48/775 = 178/1975**; annotation lines **1976**; POM **48**; PA43-start reconstruction `[1164, 764]`/1929 and all historical identities retained; zero skips/errors/failures |
| CI `default and opt-in web jar smokes` PYCODE block (re-extracted) | 0 | default fail-closed boot + web error-path HTTP campaigns passed; no DB/verifier adapter; output byte-identical to fix1 (`cfd8797e…`, deterministic campaign) |
| `jar tf … > target/jar-index.txt`; `./mvnw -B -ntp dependency:list -DincludeScope=runtime -DoutputFile=target/pa11-runtime-dependencies.txt`; CI `production jar dependency firewall` PYCODE block (re-extracted) | 0 | baseline 37/34, ssm 72/65, counterparts 64, dependency-only 8, packager 1; deltas 35/0 and 31/0; **116 adversarial probes**; `BOOT-INF/classes` parity + 20 migrations byte-equal; output byte-identical to fix1 (`981d7f7a…`). Executable JAR SHA256 `9f78e8ed2d4d712167ec880ed825f7200f795ccff2241d906eda9ada1042191f`, 40,248,355 bytes |
| `git diff --check` | 0 | no whitespace errors |
| `ci.yml` YAML parse | 0 | workflow parses; jobs `java-runtime`, `contract-regressions` intact |

Honest authoring/environment notes: the first focused run failed test compilation because Spring Boot 4.1.2 exposes `determineDispatcherTypes()` (not `getDispatcherTypes()`) on `FilterRegistrationBean`; the fixture accessor was corrected — a test-authoring correction, not a product/test weakening. An initial `./mvnw` attempt resolved the Windows JDK through WSL PATH interop and was re-run with the Linux JDK; no source impact. No check requested by the gate was left unexecuted; production provisioning/key assignment/TLS/live validation remains **NOT EXECUTED** (§11, unchanged).

Evidence (local, generated; SHA256): full-verify log `/tmp/pa43-fix2-full-verify.log` `accd6c729b7b0c01c0ddb2112199074bd3621a1a3949b5041711d79c28bf41c7`; focused unit log `90806c2d04bf99b6c7a4b026447b214e4d2f5faa3873a16c5d2a0dfc78535405`; guard log `579655b925fc1ea06863568ab1454ceac978d3cca140704b9f274a58845c81f4`; focused IT log `d559d3fea2b3e8f9cbb4ceebd24747846efa3a080d1b2d5ad225a7a6545cabcd`; F2 mutation log `477a6b4db51bf74ce8b8e50e4183b1d55b143e7e81ba069623ca3d5328e49ca2` and mutation XML `88dfd98c56a85a749bcb51d1c6e7339a9fdf105779764c298b3cc49bf7464a0b`; CI inventory out `cd2d8b0698e5381b5660d9889409c0afc48520539162b6ab9c5ea2aeead2280b`; smoke out `cfd8797e32c32fa0a7e6d73ff44539cad9336083b55393863aa4c06ab7343540` and firewall out `981d7f7af1b485a8f7906a8177949d6bd87bc0bee66eb0724cf072593704c150` (both byte-identical to the retained fix1 outputs); full-verify orders IT XML `8bcd6e7ff6f3577ffa0997cbda2ed428239d31a63a8082d7e76eb8e0ac087a29` (27 witness lines); focused orders/role XML `5ef1d49a045f18029348f23f85c9cbada279a972d2818f09c3578a3b2aa37332` / `dbf2facbf99e1d36df0b2b8425762f9bac55744bbc38ee226255f404571b5cf1`; 34-path manifest `b12e98cab4aff5db390e28a2178cdf636e962dea3ecdb33e95d8a6f3be2c38df`. All under `/tmp/pa43-fix2-evidence/`; prior `/tmp/pa43-evidence/` and `/tmp/pa43-fix1-evidence/` retained untouched.

### 14.5 Residual risks / deferred ownership (updated)

- All three accepted re-review B findings are closed with executed proof; no new product behavior, route, projection, cursor, authority/error/SQL/role semantic was introduced (production delta is one nested exception type, three throw-site types and one narrowed classification arm).
- A F1 (shared-fate sibling delegation, incl. its non-`backend/` CWD manifestation), B F2 (global inventory/CI coupling) and B F5 (duplicated ERROR behavior fixture) remain deferred to Backend/Test Maintenance in a later hardening work unit; fix2 intentionally re-pins within the existing mechanism instead of refactoring it.
- All §11/§13.5 residuals unchanged: R2b GET-alias native-400/405 real-Tomcat attribution gap; outside-family TRACE sanitized-500 carve-out (Backend/Security); LO SET ROLE boundary (Security/Database); no global keyset index / issuance fan-out (Backend/Database); request-snapshot authority (Security/Backend); `OperationsReadContext.enter()` ThreadLocal cleanup on SET LOCAL failure (Backend, pre-existing); shared cursor keyring custody/rotation + production provisioning/TLS/live validation NOT EXECUTED (Security/Operations/Platform); origin/main provenance (Controller).
- No stage/commit/push/PR/merge/approval/comment action occurred. Stopped for fresh A/B code re-review.
