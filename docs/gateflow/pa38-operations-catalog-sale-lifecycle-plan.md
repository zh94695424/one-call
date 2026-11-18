# PA38 Operations Catalog Sale Lifecycle — Cumulative Implementation Plan

**Task label:** `pa38-operations-catalog-sale-lifecycle-plan-fix3-gpt-20261118-01`
**Work unit:** `PA38-operations-catalog-sale-lifecycle`
**Planning date:** 2026-11-18
**Status:** cumulative code-generation specification closing accepted Controller findings `PA38-PLAN-REV-01..13`, `PA38-PLAN-RR-01..10` and `PA38-PLAN-RR2-01..06`, subject to fresh independent A/B plan re-review; no product code is implemented by this artifact. Fix3 supersedes the prior bean names and incomplete literal/projection/timeout/content/comment constraints; fix2's other corrections remain binding. Historical fix1/fix2 artifacts remain frozen.

## 1. Binding goal and hard boundary

The binding Goal Confirmation is `docs/gateflow/project-execution-control.md:13-36`, confirmed on **November 18, 2026**. This plan implements exactly one slice, `PA38-S1 catalog publication and offsale`, containing exactly these two routes and operation identities:

- `POST /api/v1/operations/catalog/versions/{versionId}/publications` — `publishOperationsCatalogVersion`.
- `POST /api/v1/operations/catalog/versions/{versionId}/offsales` — `offsaleOperationsCatalogVersion`.

The slice adds no route, method, request field, response field, error code, idempotency scope, table, column, index, trigger, function, grant, role, credential family, migration, cache, read model, customer flow, frontend, offer-withdrawal command, or future-slice behavior. `V001`–`V018`, with no `V019`, the frozen OpenAPI document, and the semantic content of docs 12/14/15/17 remain unchanged. A required capability absent from those frozen inputs is a blocking stop for Controller goal re-confirmation; implementation must not invent it.

Binding behavior:

- Reuse the opaque `employeeSession` authentication path and audience `onecall:employee-session:v1`.
- Require a server-derived employee whose current database status is `ACTIVE` and whose role is exactly `ADMIN`. `OPS` and disabled employees are `403 FORBIDDEN`; client claims never supply authority.
- Keep the exact publication and offsale request schemas from the frozen OpenAPI.
- Publication requires a never-published `DRAFT`; offsale targets one current `ACTIVE` version.
- Serialize by `pg_advisory_xact_lock(hashtextextended('catalog_order.publication:' || product_id::text, 0))`.
- Retire an old `ACTIVE` before activating the candidate; revision increments are statement-local and the committed partial unique index always permits at most one `ACTIVE` row per product.
- Do not accept `Idempotency-Key` as a requirement for either catalog command. Subject commands retain their existing exact key rule.
- Stale expectations return `PUBLICATION_STALE_VIEW`; other non-matchable states return `CATALOG_PUBLICATION_CONFLICT`, using the shipped sanitized problem envelope.
- Only an exact current-state match is replay. A retry carrying pre-transition expectations is stale, not replay.
- Lifecycle state, audit, and Outbox facts are one transaction. Any lifecycle, authority-role, audit, Outbox, constraint, or restoration failure rolls back lifecycle/audit/Outbox work and returns a sanitized failure.

## 2. Frozen source facts and inventories

### 2.1 Baseline identity and frozen files

The Control artifact is authoritative for the clean PA38 base: merge commit `c9f91337f8bba8e9340ef6239912b44f26373c25`, parent baseline `f719d5eb07eade04072df773a3c5fc5b931f7f42`, reviewed PA37 parent `ff02a28afa7ebb8d8e97882b0cfef7d764b3df94`, branch `codex/pa38-next-work-unit`, and managed worktree `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call` (the Control records the Windows checkout identity).

The implementation/review gate must hash, then assert unchanged, each frozen input:

- `api/openapi/onecall-v1.openapi.yaml` — baseline SHA256 `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`.
- `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` through `V018__evidence_role_ivr_association_read_privileges.sql` — exactly 18 files, no `V019`.
- `docs/12-technology-stack-and-system-architecture.md`.
- `docs/14-production-data-model-and-flyway-schema-spec.md`.
- `docs/15-production-openapi-surface-spec.md`.
- `docs/17-production-security-design-spec.md`.
- `backend/ops/postgresql/pa25-runtime-identities.sql`.

The plan changes none of those files. The implementation gate records each SHA256 before and after; a mismatch blocks the slice.

### 2.2 Current and final filesystem inventory

The current counts were independently enumerated from the checkout, excluding `.git`, `target`, `tmp`, `node_modules`, private prompt/runner files, and generated output. Modified files do not change counts.

| Inventory | Current | PA38 final | Delta and calculation |
| --- | ---: | ---: | --- |
| production Java files under `backend/src/main/java` | 279 | 290 | `+11` new |
| test Java files under `backend/src/test/java` | 169 | 180 | `+11` new: 10 test classes and one no-test fixture |
| production resources | 21 | 21 | `+0` |
| test resources | 1 | 1 | `+0` |
| Flyway migrations | 18 | 18 | `+0`; `V019` absent |
| Surefire class identities | 89 | 97 | `+8` |
| Surefire `@Test` identities | 848 | 934 | `+86` |
| Failsafe class identities | 36 | 38 | `+2` |
| Failsafe `@Test` identities | 592 | 630 | `+38` |
| all focused identities | 125 / 1,440 | 135 / 1,564 | `+10` classes / `+124` methods |
| Failsafe POM includes | 36 | 38 | `+2` exact includes |
| CI source `@Test` line scan | 1,441 | 1,565 | `+124`; one scan line per method |

The current 89/848 and 36/592 were independently reproduced using the shipped CI method regex and the POM include set, not inferred from Java-file counts. The final source, CI expected maps, POM include set, and Surefire/Failsafe XML must all equal 97/934 and 38/630. The one historical comment contributes one source-scan hit but no method (§10.1). Production changes are **11 new + 4 modified = 15 Java paths**; test changes are **11 new + 2 modified = 13 Java paths**; CI and POM add **2 modified non-Java paths**. The complete implementation allowlist is **30 paths: 22 new + 8 modified**, excluding later Controller-owned reporting artifacts. No report path is implicitly authorized for implementation.

### 2.3 Existing runtime and ACL facts

The existing **LO/operations-read** datasource family is owned by `backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java`, enabled by profile `persistence` and `onecall.operations-subject-read.enabled=true`. Credentials remain exclusively `onecall.operations-subject-read.datasource.url`, `.username`, `.password`. PA38 binds to its existing physical `operationsSubjectReadDataSource` and transaction-aware `operationsSubjectReadDsl`. Extend that exact configuration with new qualified `operationsSubjectReadCatalogSaleTransactionManager` and `operationsSubjectReadCatalogSaleTransactionTemplate` beans on the **same raw datasource object**. The manager method takes `@Qualifier("operationsSubjectReadDataSource") DataSource`; the template method takes `@Qualifier("operationsSubjectReadCatalogSaleTransactionManager") DataSourceTransactionManager`. Bean methods, names, injection qualifiers, tests and diagnostics use these exact names. This satisfies the shipped `OperationsSubjectReadConfigurationTests.everyReadBeanUsesExactQualifiedDatasourceDslAndContext` prefix oracle (`:78-94`) without editing that test or bypassing its inspected parameter types. Keep both beans in this configuration so the template can call the private guarded datasource's `assertClean()` (`:187`). The new DataSourceTransactionManager has `enforceReadOnly=false` and `rollbackOnCommitFailure=true`; the new template is explicitly `readOnly=false`, `PROPAGATION_REQUIRES_NEW`, `ISOLATION_READ_COMMITTED`, Spring transaction timeout 5 seconds. **Spring template timeout alone does not bound arbitrary jOOQ statements**; the command connection must execute the three database-local limits in §6.1. Its execute/finally calls the existing guarded datasource's `assertClean()` just as the read template does. Do not use the existing read-only `operationsSubjectReadTransactionTemplate` for writes; preserve its read-only `REPEATABLE_READ` behavior, manager, DSL, context, cursor configuration and pool guard unchanged. Adding a manager/template is not adding a pool. The existing four-connection maximum and 3000ms acquisition timeout (`:76-81`) remain unchanged; shared read/write contention is classified in §12.

Do not reuse `operationsLifecycleDataSource` or any `operationsLifecycle*` manager/template/DSL: those belong to `activation_consent` and their shipped PA32 login oracle has only `activation_consent_role`, `operations_iam_role`, `operations_service_role`, not `catalog_order_role`. Do not transfer LO role membership evidence to that login. PA38 creates no `HikariDataSource`, login, credential family, configuration prefix, or second pool; the existing LO pool constructor remains untouched.

The externally provisioned LO login is PA25's `operations_login`. Its exact seven SET targets are `operations_iam_role`, `subject_number_role`, `activation_consent_role`, `entitlement_metering_role`, `evidence_role`, `catalog_order_role`, `ivr_role`; there is no audit/platform role membership. Audit/Outbox use the existing SECURITY DEFINER function grants while effective role is `catalog_order_role`. The exact membership flags are `ADMIN FALSE`, `INHERIT FALSE`, `SET TRUE`; preserve all PA25/PA32 existing-login assertions. The PA38 disposable test login needs only the two exercised roles; it is not evidence of a production identity change. No migration or ACL file changes.

`V004__catalog_order_and_payment.sql` already supplies `catalog_version` lifecycle columns and checks, `published_at`, positive `revision`, `uq_catalog_version_active`, `guard_catalog_version_published`, and catalog-order lifecycle update privileges. `V001` already supplies `audit.append_fact` and `platform.outbox_append`; PA38 calls those functions and adds no database object.

The catalog read path remains the existing `operations_iam`-owned read service and the existing `catalog_order` reader. PA38 does not make the catalog role read `operations_iam.employee_account`.

## 3. Module boundaries and exact ownership

The command port is owned by `operations_iam`, not `catalog_order`:

- `operations_iam` named interface: `com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand` in named interface `operations-catalog-sale-command`.
- `catalog_order` implements that interface in `com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommand`.
- The `operations_iam` controller resolves the interface through `ObjectProvider<OperationsCatalogSaleCommand>`; it never imports a `catalog_order` class. This preserves the existing dependency direction and creates no reverse module edge.
- `catalog_order/package-info.java` adds only named dependencies on `operations_iam::operations-catalog-sale-command` and `operations_iam::employee-authority-read`, while preserving its existing `customer_iam`, `subject_number`, and `operations_iam::operations-subject-read` dependencies.
- `operations_iam/package-info.java` remains `allowedDependencies = {"operations_audit"}`. The existing `operations_iam -> operations_audit` declaration is preserved exactly; no `operations_iam -> catalog_order` edge is added.
- `operations_audit` remains an existing database-function owner and has no new Java dependency.

The authority read port is also owned by `operations_iam`:

- `com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort` is the named interface `employee-authority-read`.
- `com.onecall.core.operations_iam.internal.employeeauthority.DefaultEmployeeAuthorityReadPort` implements it using qualified `operationsSubjectReadDataSource` / `operationsSubjectReadDsl` and the command's writable transaction binding.
- The port accepts verified employee ID plus immutable scalar `TransactionWitness(login, backendPid, transactionId)` and returns immutable `EmployeeAuthoritySnapshot(employeeId, status, role, revision)` or an unavailable/ambiguous failure. No JDBC/Spring/jOOQ type crosses either named API. Its internal implementation obtains the LO `ConnectionHolder`; the command repository retains/checks its own connection reference before and after delegation. It performs no lock, no `FOR UPDATE`, no advisory lock, and no mutation.
- It reads only `operations_iam.employee_account(status, role, revision)` under exact `SET LOCAL ROLE operations_iam_role` on the same connection used by the mandatory transaction. `catalog_order` consumes the named interface and never references `operations_iam.employee_account` in SQL or source.
- The authority port uses the direct transaction-bound DSL with a mandatory-transaction assertion (equivalent to `PROPAGATION_MANDATORY`), **never `REQUIRES_NEW`**, never a read template or independent manager. It checks an active writable `READ_COMMITTED` transaction and the datasource-bound `ConnectionHolder`, asserts the command's backend PID and transaction ID, enters `operations_iam_role`, reads authority and returns **without resetting to login**. The command alone next enters `catalog_order_role`, then restores login on exit: exactly `operations_iam_role -> catalog_order_role -> login` on admitted execution. Denied/failed authority returns directly to login with rollback; no catalog role or SQL is reached. The command owns cleanup, not the port; any unproven role restoration fails closed.

## 4. Exact product file plan

### 4.1 New production files — 11

1. `backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java` — named command interface; immutable `PublishCommand`, `OffsaleCommand`, `Caller`, `CatalogVersionState`, `Code`/failure vocabulary; no Spring or SQL types.
2. `backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java` — `@NamedInterface("operations-catalog-sale-command")`.
3. `backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java` — named read interface and immutable `EmployeeAuthoritySnapshot`/`AuthorityFailure`; no employee table exposure.
4. `backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/package-info.java` — `@NamedInterface("employee-authority-read")`.
5. `backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java` — transaction-mandatory, lock-free, exact-role authority implementation using the existing qualified transaction/DSL beans.
6. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java` — command-port implementation and transaction orchestration; calls the authority port, repository, and semantics; owns no web parsing.
7. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java` — narrow repository interface and records for candidate/current rows, authority snapshot, transition result, and fact arguments.
8. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java` — prepared SQL on the one DSL connection; advisory lock, catalog reads/writes, audit/outbox calls, timestamp, and role assertions/restoration.
9. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java` — canonical UUID, lock key, state classifier, revision, dedupe, reason, payload, and snapshot derivation; no request reason.
10. `backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java` — exact qualified bean wiring, one command provider, `REQUIRES_NEW`/`READ_COMMITTED` assertions, no resource construction.
11. `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java` — exact two POST mappings, strict path/body/media parser, command-port resolution, response serialization and headers.

### 4.2 Modified production files — 4

- `backend/src/main/java/com/onecall/core/catalog_order/package-info.java` — add the two named-interface dependencies only; preserve existing dependencies.
- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java` — add exact catalog POST templates to the admission classifier, split key/media checks, preserve reads and subject-command behavior, conceal every other catalog mutation method/path.
- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java` — add only an overload accepting `OperationsCatalogSaleCommand.Code`, using the existing `Problem` record, reset/committed behavior, headers, UTF-8 serialization and exact byte Content-Length policy. Map `PUBLICATION_STALE_VIEW` to `409` / title `Publication stale view` and `CATALOG_PUBLICATION_CONFLICT` to `409` / title `Catalog publication conflict`; other closed codes retain shipped statuses/titles. No generic arbitrary-string code API or second envelope/writer.
- `backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java` — expose the writable manager/template specified in §2.3 over the existing LO pool; reuse the existing DSL and cleanup guard; do not alter read-template semantics or construct another datasource.

`OperationsLifecyclePersistenceConfiguration.java` is not modified or consumed. The catalog command configuration imports no `activation_consent` implementation; it wires only the qualified LO template/DSL and IAM ports. No other production path is authorized. The request binder remains controller-local/nested; no additional production binder file is planned.

### 4.3 Test ownership and new test files — 11

Every new test package has the production owner listed above; no test package is orphaned.

- `backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java` — authority-port unit behavior.
- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java` — deterministic semantic functions.
- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java` — SQL ordering/arguments/role boundary with mocks.
- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java` — command classifier, transaction and mapping orchestration.
- `backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java` — strict path/body/media parsing.
- `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java` — admission, precedence, response and concealment.
- `backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java` — exact provider/datasource/transaction binding.
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java` — source/module/SQL boundary.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java` — PostgreSQL16 lifecycle and concurrency behavior.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java` — disposable login, effective roles, ACL, role restoration and V001–V018 proofs.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java` — shared disposable PostgreSQL16/web setup, teardown, command-pool recording and exact fault injection; **zero test annotations/method identities** and no POM include or CI expected class. Its owner is the two PA38 container classes, not an orphan production package.

The two modified existing test paths are exactly `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` and `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` (§10.2). The two modified non-Java paths are exactly `.github/workflows/ci.yml` and `backend/pom.xml` (§10). These close the 30-path allowlist; every other source/test/resource path is frozen.

## 5. Admission, parsing, and response contract

### 5.1 Three-way classifier

`OperationsBoundaryFilter` classifies every Operations request before route-specific validation:

1. **Read** — existing exact GET operations. No `Idempotency-Key` check is added; existing read behavior remains byte-for-byte.
2. **Idempotent subject command** — existing subject lifecycle POST operations. Preserve their exact `Idempotency-Key` required/format/replay behavior and existing content negotiation.
3. **State-conditional catalog command** — exactly the two PA38 POST templates. Require no `Idempotency-Key`; ignore its presence as a command identity and do not create an idempotency row. Apply the existing non-read exact `Content-Type` and `Accept` checks.

The classifier is explicit and independently unit-tested. It never shares an `Idempotency-Key` branch with media validation. Unknown catalog mutation methods, alternate paths, encoded separators, duplicate segments, trailing segments, and every non-allowlisted catalog mutation are concealed as the shipped sanitized `NOT_FOUND`; no controller is reached.

**RR2-02 filter source lockstep:** only add non-breaking catalog classification and separate the subject-key branch from the shared non-read media checks. Preserve the current exact source literals and URI equality in `OperationsBoundaryFilter.java:38-49`, not merely equivalent routing behavior:

- Keep `"GET".equals(request.getMethod())`, `"/api/v1/operations/activations".equals(path)` and every existing GET path/regex literal, including `"/api/v1/operations/ivr-sessions"` and `"/api/v1/operations/ivr-sessions/[^/;%]+"`.
- Keep `"POST".equals(request.getMethod())` and `path.matches("/api/v1/operations/subjects/[^/;%]+/(suspensions|resumptions)")`, including the exact substring `[^/;%]+/(suspensions|resumptions)`.
- Keep the exact equality `!(request.getContextPath() + path).equals(request.getRequestURI())`; do not normalize it, replace it with prefix matching, decode alternate paths or remove the context-path check.

The unchanged shipped oracles are `OperationsLifecycleBoundaryTests.routesBinderProblemsAndCustomerPassThroughStayExact` (`:51-56`), `OperationsActivationReadBoundaryTests.employeeVerifierAndFilterLockstepStayIndependentAndExact` (`:103-106`) and `OperationsIvrSessionReadBoundaryTests.operationsRoutesAndControllerMappingsAreExact` (`:58-61`, `BASE` is `/api/v1/operations/ivr-sessions`). Run them without editing them. `OperationsSubjectReadConfigurationTests` also stays frozen. No additional existing test edit is necessary or authorized; the 30-path allowlist and two modified test paths remain exact.

### 5.2 Precedence

The exact sequence is:

1. Boundary route/method classification and path-template admission.
2. Existing employee-session authentication and audience verification (`onecall:employee-session:v1`). Missing, malformed, invalid, expired, revoked, unavailable, or ambiguous authentication maps to the existing sanitized `401`/`503` path.
3. Strict structural validation: canonical lowercase `versionId`, exact path shape, exact body fields/types, UTF-8 JSON, one compatible `Content-Type`, and acceptable `Accept`. Structural failures are returned before any database authority witness. There is no authority-before-input rule.
4. Resolve exactly one `OperationsCatalogSaleCommand`; missing or ambiguous provider maps to sanitized `503`.
5. Within the command transaction, read the current employee status/role/revision through `EmployeeAuthorityReadPort` under `operations_iam_role`. `ACTIVE + ADMIN` admits; `ACTIVE + OPS` and `DISABLED` return `403 FORBIDDEN`; missing/ambiguous/unavailable authority returns sanitized `503`.
6. Only after authority is admitted does the catalog command read candidate state and mutate.

`Accept: text/html` uses the shipped Operations sanitized outcome: `500 INTERNAL` with the normal problem envelope, `Cache-Control: no-store`, `Pragma: no-cache`, `X-Content-Type-Options: nosniff`, and `Content-Length`. Disabled employee is pinned to `403 FORBIDDEN`, including after a structurally valid request. Success is `200`, `application/json`, exact serialized `CatalogVersionState`, `Content-Length`, and the same no-store/no-cache/nosniff headers. The problem body exposes only the declared frozen fields and never SQL, role, credentials, host, snapshot, request body, or raw exception text.

The command's closed `Code` enum is exactly `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `PUBLICATION_STALE_VIEW`, `CATALOG_PUBLICATION_CONFLICT`, `VALIDATION_FAILED`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL`; the new writer overload exhaustively maps it. PA38 deliberately uses the read-family disabled-employee 403 precedent, not the subject-command inactive-employee 401 rule. Existing subject behavior is unchanged. RATE_LIMITED remains a frozen platform error mapping; no limiter/extra route/dependency is introduced here.

### 5.3 Strict input types

`CatalogVersionRequestBinderTests` proves:

- `versionId` is a canonical lowercase UUID equal to `UUID.toString()`; uppercase, braces, whitespace, encoded slash/semicolon, duplicate/trailing segments, and empty values are rejected by the frozen boundary/parser.
- Publication body contains exactly `expectedActiveVersionId` and `expectedRevision`; both names occur once; `expectedActiveVersionId` may be explicit JSON `null`; unknown, duplicate, missing, trailing, non-object, invalid UTF-8, oversized, quoted, fractional, overflow, zero, or negative revisions fail `422 VALIDATION_FAILED`.
- Offsale body contains exactly `expectedRevision` and no other field; the same numeric and duplicate/trailing rules apply.
- No request reason, actor, role, snapshot, or idempotency field is accepted.

## 6. Transaction, role, and SQL sequence

### 6.1 One physical connection and transaction

The command uses qualified `operationsSubjectReadCatalogSaleTransactionTemplate`, backed by new `operationsSubjectReadCatalogSaleTransactionManager` on existing `operationsSubjectReadDataSource`; command/repository/authority all use existing transaction-aware `operationsSubjectReadDsl`. The command owns the sole outer `REQUIRES_NEW`, `READ_COMMITTED`, writable transaction. The authority port joins by direct DSL with mandatory assertions (§3), never a new transaction. Never use the read template or `OperationsReadContext` for writes: that context requires read-only `REPEATABLE_READ`. Reuse its exact-role/PID/connection assertion pattern, not its incompatible transaction semantics. The new command template calls the existing guarded pool's `assertClean()` in execute/finally; the original read template/guard remain intact.

**RR2-04 database-enforced bounds:** immediately after Spring begins each command transaction, on its transaction-bound connection through `operationsSubjectReadDsl`, execute `SET LOCAL statement_timeout='2000ms'`, then `SET LOCAL lock_timeout='250ms'`, then `SET LOCAL idle_in_transaction_session_timeout='5000ms'`. These precede identity/authority/catalog reads, role switches, advisory/row-lock acquisition, lifecycle updates and audit/Outbox effects; inability to install all three rolls back and returns sanitized `503 SERVICE_UNAVAILABLE`. Assert their effective values on that same connection with the initial witness. Apply the identical setup to the one fresh recovery transaction before its reads. Do not alter session/global settings or the shipped read context. The existing read precedent is `OperationsReadContext.java:38-40`; `SET LOCAL` settings unwind on commit/rollback before the unchanged close-time baseline probe. The Spring 5-second timeout remains transaction metadata, not a proven jOOQ query bound or a five-second total HTTP deadline: statement_timeout bounds each statement, lock_timeout each lock wait, idle timeout an idle transaction, and pool acquisition remains 3000ms. Shared-pool saturation is not eliminated by these limits.

The admitted connection sequence is exactly `operations_iam_role -> catalog_order_role -> login`:

1. Capture `SELECT session_user, current_user, pg_backend_pid(), txid_current(), current_setting('transaction_read_only')` before role entry. Assert login=current user, `transaction_read_only='off'`, JDBC `autoCommit=false`, `readOnly=false`, isolation `READ_COMMITTED`, and the LO datasource's transaction-bound `ConnectionHolder`. The authority implementation receives this witness and asserts the same connection/PID/transaction, executes `SET LOCAL ROLE operations_iam_role`, and asserts exact current user and unchanged login/PID/transaction.
2. Authority alone reads `operations_iam.employee_account(id,status,role,revision)` for the verified employee. No `FOR UPDATE`, employee mutation or administrator advisory lock. On admitted `ACTIVE + ADMIN`, return without resetting the role. On denial/failure, mark rollback-only and exit to login without any catalog work.
3. The command executes `SET LOCAL ROLE catalog_order_role`, asserts exact role and the same connection/PID/transaction, reads candidate identity/product, acquires the product advisory lock, and re-reads candidate/current ACTIVE under that lock.
4. Capture **one** `SELECT clock_timestamp()` after lock acquisition as `decision_time`. No JVM clock, pre-lock time or second audit clock. Classify by §7; on a real transition retire old ACTIVE before activating candidate, or offsale target. Append exactly §8 facts and project response in the same transaction. Exact replay projects state and appends nothing.
5. Before commit, execute `SET LOCAL ROLE NONE`; assert `current_user=session_user=original_login` and unchanged connection/PID/transaction. A restoration failure marks rollback-only and returns sanitized `503`; no known failed restoration is allowed to commit. Expected failures also clean up; SQL-aborted transactions roll back before any further query.
6. The existing pool guard checks login identity, statement/lock/idle transaction timeouts, JDBC autoCommit/readOnly/isolation on return and evicts an unsafe connection. The new template observes its cleanup-failure flag in finally. A connection with unproven cleanup is never returned reusable; cleanup failure overrides a prospective 409 with sanitized `503`. No PA38 SQL changes `search_path`; **no search-path restoration is claimed**. All owner SQL is schema-qualified.

After SQLSTATE `23505` or `40001` only, `DefaultOperationsCatalogSaleCommand` waits for complete rollback/unwind, then runs **one bounded reread** in a fresh `REQUIRES_NEW` / `READ_COMMITTED` transaction (Spring timeout 5 seconds plus the three explicit database-local limits above) through the **same LO command template/pool**. A new backend connection is permitted; a new transaction identity is required. Enter/assert exactly `catalog_order_role`, select target and current ACTIVE once each, project §7.3 inside this transaction, execute `SET LOCAL ROLE NONE`, assert login restoration, complete. Do not invoke the authority port/read template, acquire an advisory/write lock, retry a mutation, select another target, append audit/Outbox, or loop. This is a zero-write operation on the writable template, not a second pool or an invocation of the shipped read-only template. If the reread proves the exact current-state replay of §7, return it with zero new facts; otherwise return `409 CATALOG_PUBLICATION_CONFLICT` (not the ordinary stale branch). Reread/identity/cleanup failure returns sanitized `503`. Ordinary stale classification never triggers recovery.

After complete rollback, SQLSTATE `55P02` maps to `409 CATALOG_PUBLICATION_CONFLICT` as explicitly required by the accepted RR2-04 contract; actual PostgreSQL lock-timeout/lock-unavailable SQLSTATE `55P03` maps to that same code. PostgreSQL 16 Appendix A names `55P02` `cant_change_runtime_param` and `55P03` `lock_not_available`: do not test only injected `55P02` and miss the real database lock timeout. SQLSTATE `57014` (`query_canceled`, including statement timeout) maps to sanitized `503 SERVICE_UNAVAILABLE`. Classify by SQLSTATE in the exception cause chain, never public/raw exception-message text; no new public code is added. These timeout branches do not retry, perform the 23505/40001 recovery reread or append facts. Installation failure for the local limits and any unproven cleanup still take sanitized 503 precedence. Recovery errors remain 503 under the preceding paragraph; no recursive recovery is allowed.

### 6.2 Exact SQL operations and owners

Use prepared statements and explicit columns only:

- IAM authority implementation only: `SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = ?`. Neither catalog repository SQL nor catalog source reads that table; catalog unit tests mock the authority port.
- Preliminary candidate identity/product read: `SELECT id, product_id FROM catalog_order.catalog_version WHERE id = ?`; this discovers only the lock key, never the publication decision or response.
- Product lock: `SELECT pg_advisory_xact_lock(hashtextextended('catalog_order.publication:' || CAST(? AS text), 0))` with canonical product UUID text.
- Locked candidate reread: `SELECT id, product_id, version_no, sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot, snapshot_sha256, created_by_employee_id, created_at, published_at, revision FROM catalog_order.catalog_version WHERE id = ?`. Use it for §6.3 validation; no content enters the response, audit or Outbox beyond their frozen fields.
- Locked current ACTIVE and post-transition projection: `SELECT id, product_id, version_no, sale_status, published_at, revision, snapshot_sha256 FROM catalog_order.catalog_version WHERE product_id = ? AND sale_status = 'ACTIVE'`. Use the post-transition row for §7.3; recovery selects target's response columns and this ACTIVE projection once each.
- Existing benefit rows for the locked DRAFT only: `SELECT id, catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months FROM catalog_order.catalog_benefit_line WHERE catalog_version_id = ? ORDER BY id`; validate §6.3 without any INSERT/UPDATE/DELETE.
- Retirement: `UPDATE catalog_order.catalog_version SET sale_status='OFFSALE', revision=revision+1 WHERE id=? AND product_id=? AND sale_status='ACTIVE' AND revision=?`.
- Activation: `UPDATE catalog_order.catalog_version SET published_at=?, sale_status='ACTIVE', revision=revision+1 WHERE id=? AND sale_status='DRAFT' AND published_at IS NULL AND revision=?`.
- Offsale: `UPDATE catalog_order.catalog_version SET sale_status='OFFSALE', revision=revision+1 WHERE id=? AND sale_status='ACTIVE' AND revision=?`.
- Audit: `SELECT audit.append_fact(?, 'EMPLOYEE', ?, ?, 'CATALOG_VERSION', ?, 'ACTION_DERIVED', ?::jsonb, ?::jsonb, ?, ?)`; bind the single captured decision_time and every §8.1 argument, never another clock call.
- Outbox: `SELECT platform.outbox_append('OPERATIONS', ?, 'CATALOG_VERSION', ?, ?, ?::jsonb, NULL, ?)`; all eight values are §8.2.
- Response: project exactly the seven frozen `CatalogVersionState` fields and derive `currentActiveVersionId` by §7.3 inside the transaction before role restoration.

Conditional updates must affect exactly one row or roll back. Never expose raw SQL/constraint/exception details. No `SELECT *`, employee lock, catalog-role employee read, offer/content/benefit/history/order/consent/grant/issuance mutation is permitted.

### 6.3 Persisted DRAFT structural validation (RR2-05)

T-S2-14 is explicit (`docs/14-production-data-model-and-flyway-schema-spec.md:3451`): publication validates the persisted candidate's immutable content and benefit lines. It is not deferred wholesale to a future draft slice. Under the product lock, validate the persisted never-published DRAFT, not request-supplied/reconstructed content, before any lifecycle UPDATE or fact append. Apply V004's existing structural invariants (`:63-110`): identity/product/version number and positive revision, unique `(product_id,version_no)`, lifecycle/published shape, non-null immutable fields, offer/audience enum and shape, price-null/nonnegative shape, currency format, billing/installment/grant-interval matrix, object-valued `snapshot` and lowercase 64-hex `snapshot_sha256`. Read the existing snapshot/hash and preserve both byte-identically; the frozen schema specifies object/hash format, not a snapshot canonicalization or hash-recomputation algorithm. Do not invent such an algorithm or rewrite the stored hash. Outbox payload hashing in §8.2 is a separate frozen algorithm.

Read all existing candidate benefit rows with the explicit projection in §6.2. Validate V004 `catalog_benefit_line` constraints (`:136-151`) and its version FK: non-null/unique row identity, matching candidate ID, unique entitlement type per version, entitlement type in `RECORDING_SECONDS`, `AI_SECONDS`, `EVIDENCE_COUNT`, `STORAGE_BYTES`, quantity >=1, validity_days >=1 and grant_interval_months >=0. This checks structural consistency, not a new cross-field commercial rule or a requirement that snapshot JSON duplicate those rows. A zero-benefit DRAFT remains allowed under the frozen schema: neither V004 nor T-S2-14 imposes nonempty benefits. This plan adds no nonempty-benefit rule, UI approval/second confirmation, semantic price rule, content repair or content mutation. Malformed persisted structures are P2's sanitized 503 with zero writes/facts; schema-unreachable malformed test inputs are defense-in-depth, not new normal business states. Future draft replacement must serialize under the same product lock; it is not implemented here.

## 7. State and expectation matrix

`expectedRevision` is always the **target version's current revision**, not the old ACTIVE revision. Publication compares `expectedActiveVersionId` null-safely with the locked current ACTIVE ID; explicit null means no ACTIVE expected. It never skips candidate revision comparison.

After authority and the locked reread, evaluate each table **top to bottom: first match wins**. Each later row excludes previous rows; do not implement independent stale/conflict checks with catch-order precedence. All failure and replay rows write zero lifecycle/audit/Outbox rows. SQL race recovery is the separate terminal branch in §6.1, not an overlapping normal-state row.

### 7.1 Publication

| Order / locked state | Expectation comparison | Classification | HTTP/code | Writes/facts |
| --- | --- | --- | --- | --- |
| P1 target missing | not evaluated | not found | `404 NOT_FOUND` | zero |
| P2 invalid persisted shape/revision, duplicate ACTIVE, or ACTIVE candidate not current | not evaluated | unavailable invariant | `503 SERVICE_UNAVAILABLE` | zero |
| P3 valid candidate `OFFSALE` or other non-matchable lifecycle | not evaluated | state conflict | `409 CATALOG_PUBLICATION_CONFLICT` | zero |
| P4 valid never-published `DRAFT` or candidate is current `ACTIVE` | expected active ID differs from current ACTIVE ID (null-safe) | stale active witness | `409 PUBLICATION_STALE_VIEW` | zero |
| P5 same matchable states as P4; active witness matches | target revision differs; explicitly **ACTIVE candidate + same current expectation + revision mismatch** | stale revision | `409 PUBLICATION_STALE_VIEW` | zero |
| P6 candidate is current `ACTIVE`; P4/P5 excluded | both expectations equal current state | replay | `200` | zero; return current state |
| P7 candidate never-published `DRAFT`; no ACTIVE; P4/P5 excluded | expected active null; target revision exact | real publish | `200` | candidate ACTIVE/published_at=decision_time/revision+1; one candidate audit/one publish Outbox |
| P8 candidate never-published `DRAFT`; old ACTIVE exists; P4/P5 excluded | expected active=old.id; target revision exact | publish with retirement | `200` | old OFFSALE/revision+1 first, then candidate ACTIVE/published_at/revision+1; one candidate audit/two Outboxes |

`DRAFT + published_at IS NOT NULL` is **schema-unreachable** under V004's published-shape check, not a normal actionable branch. P2 covers it only as corrupt/test-injected defense-in-depth input. Valid OFFSALE remains P3 even if request revision differs. A non-current ACTIVE candidate violates the invariant, rather than overlapping the valid ACTIVE stale rows. Before a real transition, check every to-be-incremented revision for overflow and fail sanitized 503 with zero writes/facts; a valid maximum stored revision alone must not prevent exact replay or override an earlier stale result. A pre-transition lost-response retry carries old revision and/or old/null active witness and is stale, never replay simply because the target is now ACTIVE.

### 7.2 Offsale

| Order / locked state | Expectation comparison | Classification | HTTP/code | Writes/facts |
| --- | --- | --- | --- | --- |
| O1 target missing | not evaluated | not found | `404 NOT_FOUND` | zero |
| O2 invalid persisted shape/revision, duplicate ACTIVE, or ACTIVE target not current | not evaluated | unavailable invariant | `503 SERVICE_UNAVAILABLE` | zero |
| O3 valid `DRAFT` or other non-matchable lifecycle | not evaluated | state conflict | `409 CATALOG_PUBLICATION_CONFLICT` | zero |
| O4 valid current `ACTIVE` or valid `OFFSALE` | target revision differs | stale revision | `409 PUBLICATION_STALE_VIEW` | zero |
| O5 target `OFFSALE`; O4 excluded | exact current revision | replay | `200` | zero; return current state, even when another version is ACTIVE |
| O6 target current `ACTIVE`; O4 excluded | exact current revision | real offsale | `200` | OFFSALE/revision+1; one audit/one Outbox |

A retry carrying pre-transition revision is stale. A first call to an already-OFFSALE target with its current revision is exact current-state replay; it appends nothing. The recovery branch proves replay by the same exact conditions, otherwise returns conflict with zero new facts.

### 7.3 Exact response projection (RR2-03)

`CatalogVersionState` has exactly `catalogVersionId`, `productId`, `versionNo`, `saleStatus`, `revision`, `publishedAt`, `currentActiveVersionId` (OpenAPI `:5741-5779`). Project it inside the command transaction, after any mutation and before `SET LOCAL ROLE NONE`; do not lazily read after commit/role restoration. The current ACTIVE identity belongs to the product, not necessarily the target:

| Successful branch | `currentActiveVersionId` | Required evidence |
| --- | --- | --- |
| P7/P8 real publication | candidate ID | post-mutation ACTIVE projection is the candidate, never the retired old ACTIVE |
| P6 publication replay | candidate ID | locked reread proves candidate is the product's current ACTIVE |
| O6 real offsale | null | post-mutation ACTIVE projection is empty under the same lock |
| O5 offsale replay | locked current ACTIVE ID or null | already-OFFSALE target is not substituted for a different current ACTIVE; cover both another-version and no-ACTIVE cases |

For successful 23505/40001 replay recovery, use that fresh transaction's target/current-ACTIVE reread and the same derivation before restoring role, with no additional read/transaction outside §6.1. All other fields come from the target's post-transition/current row. Ordinary publication/offsale hold the product lock through projection; recovery remains the frozen bounded lock-free reread and fails closed unless exact replay is proven.

## 8. Audit, Outbox, timestamp, and rollback contract

### 8.1 Audit: all eleven arguments

`audit.append_fact(occurred_at, actor_type, actor_id, action_code, object_type, object_id, reason_code, before_snapshot, after_snapshot, request_id, trace_id)` is bound as follows:

| Position / argument | Candidate publication | Real target offsale |
| --- | --- | --- |
| 1 occurred_at | captured post-lock `decision_time` | captured post-lock `decision_time` |
| 2 actor_type | `EMPLOYEE` | `EMPLOYEE` |
| 3 actor_id | authenticated server-derived employee UUID | same source |
| 4 action_code | `CATALOG_VERSION_PUBLISH` | `CATALOG_VERSION_OFFSALE` |
| 5 object_type | `CATALOG_VERSION` | `CATALOG_VERSION` |
| 6 object_id | candidate version UUID | target version UUID |
| 7 reason_code | `ACTION_DERIVED` | `ACTION_DERIVED` |
| 8 before_snapshot | exactly `{status:"DRAFT",revision:r,snapshotSha256:h}` | exactly `{status:"ACTIVE",revision:r}` |
| 9 after_snapshot | exactly `{status:"ACTIVE",revision:r+1,snapshotSha256:h}` | exactly `{status:"OFFSALE",revision:r+1}` |
| 10 request_id | `catalog_version:<candidateVersionId>:published` | `catalog_version:<targetVersionId>:offsale` |
| 11 trace_id | filter trace UUID carried through command, lowercase UUID string | same source |

Publication snapshots have exactly `status`, `revision`, `snapshotSha256`; offsale exactly `status`, `revision`. Never add `saleStatus`, `publishedAt`, request data, commercial contents or PII. Snapshot hash is immutable. `ACTION_DERIVED` is the existing docs17 technical audit projection, never client input; the action is derived from the command.

The controller obtains the **filter-created** UUID with `OperationsBoundaryFilter.trace(request)` and passes it explicitly in immutable `Caller(employeeId, traceId)` on both command records. Do not assume `OperationsRequestPrincipal` contains a trace or regenerate it. Bind the same trace through repository, audit and HTTP problem response. Request identity is the deterministic event dedupe key, not the trace, a generated UUID or an Idempotency-Key.

Append **one candidate publish audit**, no separate old-retirement audit; **one real offsale audit**; **zero replay audits**. Bind decision_time unchanged; no second clock call.

### 8.2 Outbox: all eight arguments and exact cardinality

Each `platform.outbox_append(event_domain, dedupe_key, aggregate_type, aggregate_id, event_type, payload_redacted, payload_ciphertext, payload_sha256)` call freezes:

1. `event_domain='OPERATIONS'` (actual parameter name; not `source`).
2. Exact deterministic dedupe key in the event table below.
3. `aggregate_type='CATALOG_VERSION'`.
4. Affected version UUID: old version for retirement, candidate for publication, target for real offsale.
5. Exact event type in the table.
6. Canonical redacted JSON from the affected **post-transition** row, exactly six fields in this fixed serialization order: `catalogVersionId`, `productId`, `versionNo`, `saleStatus`, `publishedAt`, `snapshotSha256`. No insignificant whitespace; canonical lowercase UUIDs/hash, integer versionNo, UTF-8 JSON strings, publishedAt as UTC `Instant.toString()` text (or JSON null where applicable). Candidate publication uses decision_time; retirement/offsale preserve published_at. Canonical bytes are the pre-jsonb serializer bytes, not PostgreSQL jsonb textual formatting. No snapshot body, request, credentials or PII.
7. `payload_ciphertext=NULL` (SQL NULL, not JSON null or string).
8. `payload_sha256`: lowercase 64-hex SHA256 of **exactly those canonical UTF-8 payload bytes**. Compute once and bind alongside the same serialized JSON; tests independently recompute it.

| Command / fact order | Dedupe key | Event type | Cardinality |
| --- | --- | --- | --- |
| publication, old retirement first if present | `catalog_version:<oldVersionId>:offsale` | `CATALOG_VERSION_OFFSALE` | zero or one |
| publication, candidate publish second (or only) | `catalog_version:<candidateVersionId>:published` | `CATALOG_VERSION_PUBLISHED` | one |
| real target offsale | `catalog_version:<targetVersionId>:offsale` | `CATALOG_VERSION_OFFSALE` | one |
| replay/stale/conflict | no append | none | zero |

State, audit and Outbox share the one transaction. Audit/Outbox/role/constraint/response-projection failures roll back **all** lifecycle/fact changes. Exactly one candidate audit plus one/two Outboxes for real publication, one target audit/Outbox for real offsale, zero new facts for replay/recovery. Tests compare all arguments and row cardinalities, not just success statuses.

## 9. Required tests and exact identities

All listed methods are exact JUnit 5 `@Test` identities. New Surefire additions total 86 methods; new Failsafe additions total 38. RR2-01..06 add assertions/subcases inside these identities only: no class/method rename, additional annotation, parameterized/dynamic test or count increase. Every new test-bearing file, as well as the zero-test fixture, must avoid annotation-like `// @Test` comments and any other comment containing an `@Test` scan token; exactly the one historical comment in §10.1 contributes to `comment_hits`.

### 9.1 Surefire — 8 classes / 86 tests

`com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPortTests` (8): `readsOnlyStatusRoleRevision`, `isTransactionMandatory`, `usesOperationsIamRole`, `doesNotUseForUpdateOrAdvisoryLock`, `preservesCommandConnectionOnSuccess`, `marksOuterTransactionRollbackOnlyOnFailure`, `mapsMissingEmployeeToUnavailable`, `returnsImmutableSnapshot`.

`com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemanticsTests` (10): `canonicalLockKeyUsesUtf8ProductId`, `dedupeKeysUseCanonicalVersionId`, `publicationReasonIsActionDerived`, `offsaleReasonIsActionDerived`, `publicationSnapshotsContainOnlyFrozenFields`, `offsaleSnapshotsContainOnlyFrozenFields`, `rejectsMalformedPersistedState`, `rejectsRevisionOverflow`, `nullActiveExpectationIsSyntaxOnlyForActiveIdentity`, `preTransitionExpectationIsNotReplay`.

`com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryTests` (12): `readsAuthorityWithoutLocking`, `entersExactOperationsIamRole`, `entersExactCatalogOrderRoleForMutation`, `acquiresProductLockBeforeDecisionTime`, `publicationReadsCandidateAndCurrentActiveUnderLock`, `retiresOldBeforeActivatingCandidate`, `offsaleUsesConditionalRevisionUpdate`, `updatesOnlyLifecycleColumns`, `usesOnePublishAudit`, `usesTwoPublishOutboxesWhenOldActiveExists`, `usesOneOffsaleAuditAndOutbox`, `restoresRoleOnSuccessAndFailure`.

`com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommandTests` (16): `requiresActiveAdminWitness`, `opsMapsToForbidden`, `disabledMapsToForbidden`, `missingAuthorityMapsToServiceUnavailable`, `mapsStaleExpectation`, `mapsUnmatchableStateToConflict`, `replayPerformsZeroWritesAndFacts`, `rollsBackAllFactsOnRepositoryFailure`, `publishCommandHasFrozenFields`, `offsaleCommandHasFrozenFields`, `returnsExactCatalogVersionState`, `usesClosedFailureCodes`, `doesNotExposeRequestReason`, `doesNotExposeDatabaseTypes`, `acceptsNullableExpectedActiveOnly`, `rejectsNonPositiveExpectedRevision`.

`com.onecall.core.operations_iam.internal.web.CatalogVersionRequestBinderTests` (10): `publicationRequiresExactlyTwoFields`, `publicationAcceptsExplicitNullActive`, `offsaleRequiresExactlyOneField`, `rejectsUnknownFields`, `rejectsDuplicateFields`, `rejectsTrailingTokens`, `rejectsQuotedFractionalOverflowAndZeroRevision`, `rejectsNonCanonicalUuid`, `rejectsInvalidUtf8AndOversizedBody`, `rejectsReasonAndIdempotencyFields`.

`com.onecall.archtests.OperationsCatalogVersionWebTests` (14): `admitsExactPublicationPost`, `admitsExactOffsalePost`, `classifiesReadsSubjectsAndCatalogCommandsSeparately`, `catalogCommandsDoNotRequireIdempotencyKey`, `subjectCommandsRetainExactIdempotencyRule`, `authenticationPrecedesStructuralParsing`, `structuralValidationPrecedesAuthorityWitness`, `disabledEmployeeReturnsForbiddenAfterValidParsing`, `opsReturnsForbidden`, `rejectsInvalidContentTypeAndShippedAcceptOutcome`, `successHasExactStateAndHeaders`, `staleViewHasExact409ProblemHeadersAndLength`, `concealsOtherCatalogMethodsAndUris`, `publicationConflictHasExact409ProblemHeadersAndLength`.

`com.onecall.archtests.OperationsCatalogVersionConfigurationTests` (8): `reusesExistingOperationsDatasource`, `usesNewWritableRequiresNewReadCommittedTemplate`, `reusesSamePhysicalDslBinding`, `doesNotCreateSecondPoolOrCredentialFamily`, `resolvesExactlyOneCommandProvider`, `missingProviderFailsClosed`, `ambiguousProviderFailsClosed`, `defaultRuntimeHasNoWriteBeans`.

`com.onecall.archtests.architecture.OperationsCatalogVersionBoundaryTests` (8): `scansExactProductionFiles`, `commandPortIsOperationsIamOwned`, `catalogOrderImplementsNamedPortWithoutReverseEdge`, `employeeAuthorityPortIsOperationsIamOwned`, `catalogSqlNeverReadsEmployeeAccount`, `rejectsSelectStarForbiddenTablesAndContentWrites`, `requiresOneTransactionAndPublicationOrder`, `preservesFrozenInputsAndNoV019`.

### 9.2 Failsafe PostgreSQL16 — 2 classes / 38 tests

`com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT` (24): `adminPublishesWithoutPreviousActive`, `adminPublishesAndRetiresPreviousActive`, `retirementStatementPrecedesActivation`, `publicationIncrementsBothRevisions`, `publicationUsesOneDecisionTimestamp`, `publicationAuditArgumentsAreExact`, `publicationOutboxArgumentsAreExact`, `publicationWithOldActiveHasOneAuditAndTwoOutboxes`, `offsaleTransitionsActive`, `offsaleAuditAndOutboxArgumentsAreExact`, `publicationCurrentStateReplayIsZeroWrite`, `offsaleCurrentStateReplayIsZeroWrite`, `preTransitionPublicationRetryIsStale`, `preTransitionOffsaleRetryIsStale`, `stalePublicationMakesNoChange`, `staleOffsaleMakesNoChange`, `unexpectedStatesMapToConflict`, `publishedContentAndHistoryRemainByteIdentical`, `auditFailureRollsBackLifecycle`, `outboxFailureRollsBackLifecycle`, `concurrentPublishersYieldOneActiveWinner`, `concurrentOffsalesYieldOneWinner`, `oneActiveInvariantHoldsAfterEveryCommit`, `routeMutationOutsideTwoTemplatesIsConcealed`.

`com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT` (14): `disposableLoginHasAdminFalseInheritFalseSetTrueRoles`, `loginCanSetOperationsIamRole`, `loginCanSetCatalogOrderRole`, `operationsIamRoleReadsOnlyAuthorityColumns`, `catalogRoleCannotReadEmployeeAccount`, `catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes`, `v001ThroughV018ProvideAllRequiredObjects`, `noV019IsNeededOrPresent`, `setLocalRoleIsExactAndRestored`, `restorationFailureEvictsDirtyConnection`, `readOnlyOrWrongRoleCannotMutateCatalog`, `publishedTriggerRejectsContentMutation`, `activePartialUniqueIndexHolds`, `historicalRowsRemainUnchangedAfterRepeatedCommands`.

The no-test `OperationsCatalogVersionLifecycleFixture.java` owns shared provisioning/teardown, isolated PostgreSQL16 data, web context, and **one LO pool** configured through `onecall.operations-subject-read.datasource.*`. Its disposable PA38 login has exactly `operations_iam_role` and `catalog_order_role` with `ADMIN FALSE`, `INHERIT FALSE`, `SET TRUE`; no production login/membership or PA32 oracle changes. Both IT classes consume this carrier; no helper test identity is invented. Use the existing DSL execute-listener pattern and test-only JDBC recording to capture statement order, bind values, backend PID, transaction ID, current/session user, transaction_read_only and row/fact before/after state. Never add a production fault switch or dependency.

### 9.3 Exact assertion ownership and fault seams (no additional tests)

- `staleViewHasExact409ProblemHeadersAndLength` and `publicationConflictHasExact409ProblemHeadersAndLength`: each drives **both routes** through the real controller and writer overload with a failing command stub. Assert status 409, exact code respectively `PUBLICATION_STALE_VIEW` / `CATALOG_PUBLICATION_CONFLICT`, exact title from §4.2, exactly the existing five problem fields (`type=about:blank`, title, status, code, traceId), filter trace UUID equality, `application/problem+json`, no-store/no-cache/nosniff, Content-Length equal to serialized bytes, no `WWW-Authenticate`, no extra fields, and no SQL/role/credential/host/body/snapshot/exception canary in response or captured logs. Fold success byte-length into existing `successHasExactStateAndHeaders`; preserve canary probes in both new 409 identities and other error scenarios. Thus two planned methods are **replaced**, not appended: web remains 14 and Surefire remains 86 additions.
- `isTransactionMandatory` rejects no transaction/read-only/wrong resource and verifies no REQUIRES_NEW on the port; `preservesCommandConnectionOnSuccess` asserts the IAM role remains for the command's next switch; `marksOuterTransactionRollbackOnlyOnFailure` verifies command cleanup ownership. `setLocalRoleIsExactAndRestored` is the real PostgreSQL proof of one PID/transaction across IAM authority, catalog state/audit/Outbox, and login restoration; denial/failure variants restore without entering catalog.
- `reusesExistingOperationsDatasource`, `usesNewWritableRequiresNewReadCommittedTemplate`, and `reusesSamePhysicalDslBinding` assert raw LO datasource object identity, exact `operationsSubjectReadCatalogSaleTransactionManager` / `operationsSubjectReadCatalogSaleTransactionTemplate` qualifiers, transaction-aware DSL reuse, Spring timeout=5/propagation/isolation/writability/rollback and cleanup guard; original read manager/template remains read-only REPEATABLE_READ. Assert every new inspected configuration parameter retains the `operationsSubjectRead` qualifier prefix; run the shipped prefix test unchanged. `defaultRuntimeHasNoWriteBeans` covers missing profile/enablement. No dependency on operations-lifecycle credentials/beans is permitted.
- **RR2-02 unchanged filter proof:** `classifiesReadsSubjectsAndCatalogCommandsSeparately` covers all three branches and context-path mismatches. `OperationsCatalogVersionBoundaryTests.requiresOneTransactionAndPublicationOrder` also checks the preserved §5.1 filter source literals/equality. The three shipped boundary methods named there run unchanged alongside `OperationsSubjectsWebTests`; no extra editable path or method identity is created.
- **RR2-03 projection assertions:** `DefaultOperationsCatalogSaleCommandTests.returnsExactCatalogVersionState` and `OperationsCatalogVersionWebTests.successHasExactStateAndHeaders` each iterate real publication without/with retirement, publication replay, real offsale and offsale replay with another ACTIVE or no ACTIVE. Assert all seven fields, specifically candidate/candidate/null/other-or-null `currentActiveVersionId` as in §7.3, and web byte length/headers. `CatalogSaleLifecycleRepositoryTests.publicationReadsCandidateAndCurrentActiveUnderLock`, `retiresOldBeforeActivatingCandidate`, `offsaleUsesConditionalRevisionUpdate` and `restoresRoleOnSuccessAndFailure` verify projection order before role restoration and no stale pre-mutation ACTIVE reuse. In `OperationsCatalogVersionLifecycleIT`, add candidate ID assertions to `adminPublishesWithoutPreviousActive`, `adminPublishesAndRetiresPreviousActive`, `publicationCurrentStateReplayIsZeroWrite`; null to `offsaleTransitionsActive`; other-version ID and null subcases to `offsaleCurrentStateReplayIsZeroWrite`, with zero new facts on each replay. The existing unique/serialization recovery subcases assert the same field from the fresh transaction, never the failed transaction's cached ACTIVE.
- **RR2-04 bounds/mapping unit assertions:** `CatalogSaleLifecycleRepositoryTests.acquiresProductLockBeforeDecisionTime` verifies all three exact §6.1 SET LOCAL statements precede role/authority/catalog/lock/effect SQL on the bound connection; `restoresRoleOnSuccessAndFailure` includes setup failure and timeout rollback with unchanged guard behavior. `DefaultOperationsCatalogSaleCommandTests.mapsUnmatchableStateToConflict` adds injected `55P02` and native `55P03` subcases; `usesClosedFailureCodes` adds `57014` and pool-acquisition failure -> sanitized 503, no raw canaries, no retry/recovery facts and cleanup-failure override ->503. Setup failure remains503, even if the failed SET reports55P02. New codes are not introduced.
- **RR2-04 PostgreSQL/shared-read-pool proof:** inside existing `OperationsCatalogVersionLifecycleIT.concurrentPublishersYieldOneActiveWinner`, retain the two-publisher one-winner case and add a bounded-contention subcase. A separate fixture-admin connection holds the target product advisory lock; an admitted command on the single four-slot LO pool attempts that same lock. Capture/assert its effective `statement_timeout=2s`, `lock_timeout=250ms`, `idle_in_transaction_session_timeout=5s` on its own PID/transaction before the lock attempt. While that command waits, run an existing catalog-product read through the unchanged read service/template on another connection of the **same raw pool** and assert success with exact data and restored read settings. The waiting command must terminate with real lock-timeout SQLSTATE55P03 internally and sanitized409 `CATALOG_PUBLICATION_CONFLICT`, zero lifecycle/audit/Outbox changes, no recovery/retry; release the held admin lock in finally. Use latches/SQL observation, not arbitrary sleeps; a generous 10-second harness deadline bounds the test, not a false exact250ms HTTP assertion. After rollback, assert an existing read still succeeds and all returned/next-checkout sessions match guard baselines. This proves one blocked write does not consume the whole pool; it does not claim fairness or a production saturation SLO.
- **RR2-04 real statement-cancel proof:** extend existing `auditFailureRollsBackLifecycle` with a timeout variant of fixture stage `AFTER_AUDIT_APPEND`: instead of its ordinary one-shot exception, execute `SELECT pg_sleep(3)` through a JDBC statement on that captured command connection. The already-installed 2000ms statement timeout must raise real57014, return sanitized503 and roll back the just-appended audit plus all lifecycle/Outbox work. Assert restored timeout/role/JDBC baselines or eviction before reuse and a subsequent successful read. The listener is fixture-only, uses the bound connection and does not recursively call jOOQ or alter production SQL; retain all prior fault-stage variants. Unit mapping, real lock/read availability, and real statement-cancel subcases add zero identities. Sustained pool saturation/production capacity remains the fully classified §12 residual, not an unexecuted-runtime claim.
- **RR2-05 validation assertions:** extend `CatalogSaleLifecycleSemanticsTests.rejectsMalformedPersistedState` with malformed persisted immutable/snapshot-object/hash-format/benefit-row inputs, expected sanitized503 and no effects; these are defensive inputs where constraints make them schema-unreachable. `CatalogSaleLifecycleRepositoryTests.publicationReadsCandidateAndCurrentActiveUnderLock` verifies the locked explicit content/benefit SELECT projections and validation before UPDATE/facts. Extend `OperationsCatalogVersionLifecycleIT.adminPublishesWithoutPreviousActive` with schema-valid zero-benefit and valid existing-benefit DRAFT subcases; both publish, keep snapshot/hash/benefits unchanged, and return the candidate ACTIVE ID. `publishedContentAndHistoryRemainByteIdentical` includes the unchanged snapshot/hash and every pre-existing benefit row. No semantic pricing or invented snapshot/benefit equivalence requirement is asserted.
- `readsAuthorityWithoutLocking` / `entersExactOperationsIamRole` in catalog repository tests assert mocked authority-port delegation/order and absence of employee SQL, not an invented catalog-owned employee query. Authority SQL itself is tested by the IAM suite and PostgreSQL identity trace. `mapsStaleExpectation` covers every P4/P5/O4 variant, especially ACTIVE+matching-current+wrong-revision. `mapsUnmatchableStateToConflict` covers P3/O3 separately. All table rows have exact one-outcome assertions, including unreachable-input defense-in-depth, null witness, pre-transition retries and OFFSALE replay while another target is ACTIVE.
- Fixture named stages `AFTER_AUDIT_APPEND`, `AFTER_RETIREMENT_OUTBOX`, `AFTER_PUBLISH_OUTBOX`, `AFTER_OFFSALE_OUTBOX`: throw once from a test-installed DSL executeEnd listener **after** the matched real function SQL succeeded, before returning to command code. `auditFailureRollsBackLifecycle` and `outboxFailureRollsBackLifecycle` iterate relevant stages inside their existing methods and assert unchanged lifecycle/revisions/content and zero committed command facts. SQL/bind matching and per-command ordinal prevent failures at an unrelated call; no ACL, migration or function rewrite is needed.
- Fixture `BEFORE_ROLE_RESTORE` throws once at executeStart for exact `SET LOCAL ROLE NONE`, proving rollback-only and 503 even though rollback naturally resets LOCAL role. Fixture `TERMINATE_BEFORE_ROLE_RESTORE` instead terminates that captured backend via a separate test-admin connection immediately before restore. `restorationFailureEvictsDirtyConnection` verifies rollback/zero committed facts, cleanup failure, backend retirement and a clean replacement pool checkout; the existing guard's failed baseline probe/eviction is the mechanism. Do not claim that an ordinary LOCAL-role failure necessarily leaves a dirty connection after successful rollback.
- Fixture `BEFORE_CANDIDATE_ACTIVATION` uses a deterministic latch after locked reread and before UPDATE: an independent test-admin connection deliberately bypasses the owner advisory lock to commit a competing ACTIVE version. The original activation then produces a **real 23505** from `uq_catalog_version_active`. Inside existing `concurrentPublishersYieldOneActiveWinner`, retain the normal cooperating two-command race and add this adversarial subcase; assert rollback first, one fresh transaction on LO, exact catalog role/login restoration, no retries, conflict with zero failed-command facts, one committed ACTIVE competitor. Recovered state not matching the requested result cannot become stale/replay. A one-shot test listener `SERIALIZATION_AT_ACTIVATION` injects SQLSTATE 40001 for the same recovery classifier; exact-current recovery proof and reread/cleanup failure variants run within the same existing method, without creating new test identities.
- `publicationAuditArgumentsAreExact`, `publicationOutboxArgumentsAreExact`, `offsaleAuditAndOutboxArgumentsAreExact`, `publicationUsesOneDecisionTimestamp` and snapshot semantics tests assert every §8 argument, canonical payload hash, distinct deterministic request vs carried trace identity, one post-lock clock, cardinality/order, and zero replay facts.

`catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes` asserts the shipped V004 **42-tuple catalog-schema ACL matrix**, with exact privilege/grantor/non-grantable tuples: schema USAGE (1), SELECT+INSERT on seven catalog tables (14), column UPDATE on catalog_version's 12 columns / catalog_offer's 3 / customer_order's 4 / payment_order's 4 (23), and four guard-function EXECUTEs (4). Column sets are exactly V004: version `sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,published_at,revision`; offer `offer_status,version_token,revision`; customer_order `payment_status,paid_at,closed_at,revision`; payment_order `status,provider_transaction_id,updated_at,revision`. Tables are `catalog_product,catalog_version,catalog_offer,catalog_benefit_line,customer_order,payment_order,payment_event`; functions are `guard_catalog_version_published,guard_catalog_offer,guard_customer_order_snapshot,guard_payment_order_identity`, all owned/granted by `catalog_order_owner`. Assert the existing V004 cross-schema read columns and V001/V004 audit/Outbox function EXECUTEs as in the shipped PA37 matrix test; negative probes deny catalog DELETE, direct audit/Outbox table writes, employee-account SELECT and unrelated writes. Published-content UPDATE rejection is a trigger proof, **not absence of its ACL grant**. The historical `RoleLeastPrivilegeIT` class suffix is retained for inventory compatibility; this test does not call the existing broad role least privilege. Over-privilege remains §12's classified residual. Likewise `operationsIamRoleReadsOnlyAuthorityColumns` asserts the port's narrow SELECT, not that the role lacks its other frozen privileges.

Fix2 planned-identity replacements (no shipped identity changes): authority `restoresRoleOnSuccess -> preservesCommandConnectionOnSuccess`, `restoresRoleOnFailure -> marksOuterTransactionRollbackOnlyOnFailure`; web `successContentLengthMatchesBytes -> staleViewHasExact409ProblemHeadersAndLength`, `problemsNeverExposeSensitiveCanaries -> publicationConflictHasExact409ProblemHeadersAndLength`; configuration `reusesExistingRequiresNewReadCommittedTemplate -> usesNewWritableRequiresNewReadCommittedTemplate`; ACL `catalogRoleHasOnlyFrozenLifecyclePrivileges -> catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes`. All other §9 identities are unchanged; fixture contributes zero.

## 10. CI, POM, guard, and manifest lockstep

Only `.github/workflows/ci.yml`, the `container-smoke` includes in `backend/pom.xml`, the two existing guard paths in §4.3, and the product/test paths in §4 are authorized for implementation. No existing test identity is renamed or deleted. All §9 replacements concern **unimplemented planned identities**, not shipped methods. Keep one source-scan annotation line per new test method. Every new test-bearing file and the new fixture must avoid annotation-like `// @Test` comments and all other comment forms containing an `@Test` scan token. Do not add comment hits to either modified guard. Preserve exactly the one historical `comment_hits` entry below; no additional test edit is authorized to relax it.

### 10.1 Complete source-subtraction/union chain

Add `pa38_unit_additions` (8 classes) and `pa38_container_additions` (2 classes) containing exactly §9's FQCN/method sets, and merge them into `unit_expected` / `container_expected` using the existing duplicate-checked union loop. Define per-class `pa38_names = pa38_unit_additions.get(class_name, set()) | pa38_container_additions.get(class_name, set())`; add it to `additions` and hence the baseline subtraction. The no-test fixture is in neither map.

**Both** historical families must subtract PA38, not just source lines. For each existing prefix `pa37_start`, `pa35_start`, `pa36_start`, `pa34_start`, `pa33_start`, `pa32_start`, `pa31_start`, `pa30_start`, `pa29_start`, `pa28_start`, `pa27_start`, `pa25_start`, `pa23_start`, `pa22_start`, `pa21_start`, `pa20_start`, `pa19_start`, and `baseline`:

- In its `*_totals[suite_index]` calculation, union `pa38_names` into the subtracted method-name set (baseline already uses updated `additions`).
- In its `*_source = method_pattern.sub(...)` calculation, union `pa38_names` into the matching set (baseline likewise uses updated `additions`), preserving that expression's historical replacement form (`replace("@Test", "", 1)` vs empty-string replacement). Feed the result to its existing `*_line_scan_total` calculation.

Add `pa38_start_totals[suite_index] += len(names) - len(pa38_names)` and a corresponding `pa38_start_source` which removes only PA38 annotations, accumulating `pa38_start_line_scan_total`. `pa26_start` is represented historically through the existing additions/arithmetic and diagnostic, not an invented new source variable. Do not change prior PA additions maps, historical assertions or replacement semantics.

Final added assertions: PA38 map method lengths `[8,10,12,16,10,14,8,8]` and `[24,14]`; map sizes 8/2; `pa38_start_totals == [848,592]`, sum 1440; `pa38_start_line_scan_total == 1441`; current `method_totals == [934,630]`, sum 1564, `line_scan_total == 1565`; class counts 97/38; POM includes 38; exact XML method sets equal the union maps, no skips/failures/errors.

Preserve the **exact one historical comment** assertion:

```python
assert comment_hits == [("PostgresContainerIT.java", "// evidence inside this existing method; no new @Test and no catalog weakening.")]
```

It accounts for `line_scan_total = sum(method_totals) + 1`, both before PA38 (1441 vs 1440) and after (1565 vs 1564). Preserve the comment itself. Extend the existing historical tuple to `(baseline_line_scan_total, pa19_start_line_scan_total, pa20_start_line_scan_total, pa21_start_line_scan_total, pa22_start_line_scan_total, pa23_start_line_scan_total, pa38_start_line_scan_total, line_scan_total) == (291,321,361,401,446,481,1441,1565)`; do not replace a historical value with a current one.

### 10.2 Exact changed literal / diagnostic ledger

These are context-specific replacements, never global search/replace. All other historical literals stay frozen.

| Carrier / context | Current literal or fix1 error | Required final value / reason |
| --- | --- | --- |
| New bean methods/injection/configuration tests/diagnostics (§2.3/§6.1/§9.3) | unimplemented fix2 manager/template names | `operationsSubjectReadCatalogSaleTransactionManager` / `operationsSubjectReadCatalogSaleTransactionTemplate`; shipped prefix oracle stays unchanged, no extra path |
| OperationsBoundaryFilter source and its three shipped source-literal oracles | exact current §5.1 literals and context-path URI equality | unchanged; add only non-breaking catalog classification, no extra existing test edit |
| CI early `assert container smoke actually ran`, sum of Surefire XML tests (currently line 162) | `!= 848` | `!= 934` = 848+86 |
| Same early diagnostic | `expected exactly 848 Surefire unit/module tests` | `expected exactly 934 Surefire unit/module tests` |
| CI current line assertion | `line_scan_total == 1441` | `line_scan_total == 1565` = 1441+124 |
| CI current method assertion | `[848,592]`, sum `1440` | `[934,630]`, sum `1564` = +86/+38 |
| CI historical tuple | `(291,321,361,401,446,481,1441)` | `(291,321,361,401,446,481,1441,1565)`, adding PA38-start before current |
| Fix1 erroneous historical tuple tail (not shipped CI) | `1557` | `1565` |
| CI expected-class literals | `len(unit_expected) == 89`, `len(container_expected) == 36` | `97`, `38` |
| CI POM include count | `len(includes) == 36` | `38` |
| POM include set | existing 36 exact includes | add exactly `**/OperationsCatalogVersionLifecycleIT.java` and `**/OperationsCatalogVersionRoleLeastPrivilegeIT.java`; final 38, no fixture include |
| CI Surefire exact_inventory argument | `848` (fix1 error `926`) | `934` |
| CI Failsafe exact_inventory argument | `592` | `630` |
| CI summary label and leading inventory | `PA37 ... 89/848 + 36/592 = 125/1440` | `PA38 ... 97/934 + 38/630 = 135/1564` |
| CI method-chain diagnostic tail | `PA37-start 788+556=1344 -> current 848+592=1440` | `PA37-start 788+556=1344 -> PA38-start 848+592=1440 -> current 934+630=1564` |
| CI line-chain diagnostic tail | `... -> 1233 -> 1345 -> 1441; POM 36 includes` | `... -> 1233 -> 1345 -> 1441 -> 1565; POM 38 includes` |
| CI PA38 map/union/subtraction diagnostic | absent | added 8/86 and 2/38 maps, PA38-start `[848,592]` / `1441`, complete §10.1 union and subtraction families; preserve `zero skips/errors/failures` suffix |
| ModuleBoundaryTests catalog_order allow-map | 3 entries | 5 entries; add exactly the two §3 named interfaces |
| Fix1 erroneous discovered-edge instruction | `13 -> 14` | **13 -> 13**, preserve entire discovered-edge multiset and diagnostic |
| Evidence guard CI-token expectations | `89`, `36`, `36` in the three `len(...)` tokens | `97`, `38`, `38`, plus PA38 map/start/current tokens; retain every historical PA36/35/37 token |
| Evidence guard product manifest count | `299` | `310` = (279+11)+21-1 |
| Evidence guard product manifest digest | `12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1` | recomputed 64-hex LF digest after all 15 production edits; procedure §10.3, no fabricated future hash |
| Fix1 Java inventory ledger | test `179`, new test files `10`, modified production `2` | test `180`, new test files `11` (fixture no tests), modified production `4`; main `290` unchanged |
| Fix1 manifest/report carrier and aggregate claim | CI carrier / count plus nonexistent byte aggregate | Java guard carrier / count and SHA256 only; remove the nonexistent field, add no replacement size aggregate |

The final exact calls are `exact_inventory("target/surefire-reports/TEST-*.xml", unit_expected, 934)` and `exact_inventory("target/failsafe-reports/TEST-*.xml", container_expected, 630)`. The final summary preserves this complete historical chain unchanged and appends PA38/current:

```text
historical 112+178=290 -> PA19-start 128+192=320 -> PA20-start 162+198=360 -> PA21-start 196+204=400 -> PA22-start 231+214=445 -> PA23-start 266+214=480 -> PA25-start 309+214=523 -> PA26-start 309+234=543 -> PA27-start 309+246=555 -> PA28-start 341+272=613 -> PA29-start 375+302=677 -> PA30-start 417+336=753 -> PA31-start 468+370=838 -> PA32-start 520+404=924 -> PA33-start 574+440=1014 -> PA34-start 644+476=1120 -> PA36-start 704+504=1208 -> PA35-start 712+520=1232 -> PA37-start 788+556=1344 -> PA38-start 848+592=1440 -> current 934+630=1564
line scans 291 -> 321 -> 361 -> 401 -> 446 -> 481 -> 524 -> 544 -> 556 -> 614 -> 678 -> 754 -> 839 -> 925 -> 1015 -> 1121 -> 1209 -> 1233 -> 1345 -> 1441 -> 1565; POM 38 includes; zero skips/errors/failures
```

`ModuleBoundaryTests.java` retains all **9** method identities, exact 14-module set, no-cycle verification, internal-import/cycle/forbidden-dependency negative fixtures and `containsExactlyInAnyOrder` on the unchanged **13-entry discovered-module-edge multiset**, including six operations_iam targets. The `catalog_order -> operations_iam` pair already exists through operations-subject-read, so two more named interfaces add no unique module edge. Preserve the operations_iam allow-map entry `{"operations_audit"}` (a declaration, not a claim of an additional discovered Java edge).

Within existing `verifiesNoCyclesOrBoundaryViolations`, assert catalog_order `getAllowedDependencies(modules)` size **5** and, filtering target operations_iam, exact tuples `("operations_iam","operations-subject-read")`, `("operations_iam","operations-catalog-sale-command")`, `("operations_iam","employee-authority-read")`. The exact map separately preserves customer_iam and subject_number. Assert both new IAM named interfaces exist, expose only their API packages and no internal implementation; no operations_iam -> catalog_order import. Do not weaken the discovered-edge assertion to accommodate the new ports.

`EvidenceRoleIvrAssociationAclBoundaryTests.java` retains all **8** method identities. Its `ciPomAndOrdinaryMethodInventoryMatchExactly` token oracle adds the PA38 maps, subtraction/start/current/XML tokens and updated class/include values; existing historical source/method oracles are preserved. Its `v001ThroughV017AndPublicContractsRemainByteFrozen` alone carries the product manifest count/digest update. Neither class gains a test method. A CI/POM/source/guard mismatch blocks implementation acceptance.

### 10.3 Actual manifest carrier and digest recomputation

The carrier is **`backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`**, method `v001ThroughV017AndPublicContractsRemainByteFrozen`, currently lines 98–102. CI is not the manifest carrier; there is no separate manifest file or aggregate byte-count field.

Reproduce the Java algorithm exactly, from `backend`: recursively walk **all regular files** under `src/main/java` and `src/main/resources`; exclude only filename `V018__evidence_role_ivr_association_read_privileges.sql` as the guard does. For each, normalize CRLF to LF, SHA256 the bytes, render lowercase hex, create `forward/slash/backend-relative/path + " " + fileHash`, sort complete lines lexicographically, join with LF and append one terminal LF. UTF-8 encode and SHA256 that string. Current independent result is **299** / **`12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1`**. After implementation count must be **310** = 290 Java +21 resources -1 excluded V018. Both writer/config modifications affect the digest but not the count. The final digest cannot be computed before the implementation bytes exist; implementation must replace the guard's literal with the calculated value, never commit a placeholder.

Exact non-mutating recomputation (prints count and digest only):

```bash
cd backend
python3 - <<'PY'
from pathlib import Path
from hashlib import sha256
manifest = []
for root in ("src/main/java", "src/main/resources"):
    for path in Path(root).rglob("*"):
        if path.is_file() and path.name != "V018__evidence_role_ivr_association_read_privileges.sql":
            digest = sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
            manifest.append(path.as_posix() + " " + digest)
print(len(manifest), sha256(("\n".join(sorted(manifest)) + "\n").encode("utf-8")).hexdigest())
PY
```

Report count, computed digest, carrier/method and exact old/new literal; no nonexistent byte aggregate is added or reported. Source=CI=POM=XML refers to test inventories; the Java guard independently checks the production manifest and frozen hashes.

## 11. Validation commands and expected assertions

This planning task does not execute Maven, Docker, PostgreSQL16, or the implementation tests. The following are exact implementation/review commands and expected results.

### 11.1 Focused Surefire

```bash
cd backend && ./mvnw -B -ntp -Dtest='EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests' test
```

Expected: all **8** new unit classes execute; the selector contains all eight names and no class is silently omitted; 86 tests, zero skipped/failures/errors; assertions include `OPS=403`, disabled-after-parse `403`, unacceptable Accept shipped `500`, exact **both 409** codes/envelopes/trace/headers/length on both routes, success Content-Length/headers, leak canaries, exact LO wiring, mandatory authority, port ownership, and zero catalog employee-table reads.

Run the following unchanged shipped configuration/filter regression group separately (5 existing classes / 44 existing methods = 8+8+8+8+12, no additions to global totals):

```bash
cd backend && ./mvnw -B -ntp -Dtest='OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests' test
```

Expected: prefix oracle, all three exact filter source-literal/equality guards and subject behavior stay green without editing any of these five files. The 8 new-class/86-method selector above additionally covers RR2-03 response derivation, RR2-04 setup/error mapping and RR2-05 structural/zero-benefit subcases from §9.3, with unchanged counts.

Focused existing guards (run separately so totals are unambiguous):

```bash
cd backend && ./mvnw -B -ntp -Dtest='ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests' test
```

Expected: **2 classes / 17 tests** (9+8), unchanged shipped identities; exact 14 modules, 13 discovered edges, catalog allowed set 5, three IAM named-interface tuples, frozen operations_iam allow-map, manifest 310/computed digest and CI-token lockstep. The manifest expectation applies after implementation, not in this document-only task.

### 11.2 Focused Failsafe

```bash
cd backend && ./mvnw -B -ntp -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify
```

Expected: exactly two PA38 Failsafe reports, 38 tests (24+14), zero skipped/failures/errors; assertions include both replays, pre-transition stale, ACTIVE/matching-current/wrong-revision stale, one-winner races, direct 23505 and injected 40001 recovery, new recovery transaction on LO, zero recovery facts, one-ACTIVE invariant, immutable content/history, exact eleven/eight audit/Outbox arguments, rollback fault stages, PID/transaction/role restoration/eviction, exact V004 42-tuple matrix plus negatives, membership flags, V001–V018/no-V019. Include every §9.3 RR2 subcase: exact currentActiveVersionId values (including other ACTIVE/null offsale replay), zero-benefit publication, actual local limits, real55P03 lock conflict with a successful concurrent existing read on the same LO pool, real57014 statement cancel with atomic rollback and clean subsequent read. Failsafe's `verify` lifecycle also runs the complete Surefire suite unless explicitly skipped; its expected Surefire totals are 97/934, not zero or 8/86. The no-test fixture produces no report.

### 11.3 Full gate

```bash
cd backend && ./mvnw -B -ntp -Pcontainer-smoke verify
```

Expected: existing complete Surefire/Failsafe suites plus PA38, exact final `97/934` and `38/630`, POM includes 38, no skipped/failures/errors, all CI source-subtraction and manifest guards green, and no frozen input changes.

Then execute the shipped complete CI Python oracle, without installing a YAML parser, from the repository root:

```bash
cd backend && python3 - <<'PY'
from pathlib import Path
from textwrap import dedent
workflow = Path('../.github/workflows/ci.yml').read_text()
marker = "          python3 - <<'PY'\n"
start = workflow.index(marker) + len(marker)
end = workflow.index('\n          PY', start)
exec(compile(dedent(workflow[start:end]), 'ci-container-inventory', 'exec'))
PY
```

Expected: the early XML assertion uses 934, all source=CI=POM=XML exact identities/totals match §10, historical method/line subtraction families remain unchanged, comment_hits is exactly one, and the PA38 summary ends 135/1564, line scan 1565, POM 38. Run after the full verify (not after focused-only report generation, which can leave a partial/stale XML set).

### 11.4 Static/frozen verification

```bash
git status --short --branch
git rev-parse HEAD
git rev-parse HEAD^{tree}
git diff --check
find backend/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' -printf '%f\n' | sort
test "$(find backend/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' | wc -l)" -eq 18
test -z "$(find backend/src/main/resources/db/migration -maxdepth 1 -name 'V019__*.sql' -print -quit)"
git diff --exit-code -- api/openapi/onecall-v1.openapi.yaml docs/12-technology-stack-and-system-architecture.md docs/14-production-data-model-and-flyway-schema-spec.md docs/15-production-openapi-surface-spec.md docs/17-production-security-design-spec.md backend/src/main/resources/db/migration backend/ops/postgresql/pa25-runtime-identities.sql
```

Expected: only the authorized plan files are changed in this planning task; in implementation, only the §4/§10 paths are changed.

## 12. Residuals and destinations

| Owner | Residual | Destination and disposition |
| --- | --- | --- |
| Platform/Security/Operations | Production LO property enablement, credentials/secret delivery, exact seven PA25 SET edges including catalog_order_role, pool health | deployment readiness; PA38 proves fail-closed unavailable/ambiguous behavior and tested cleanup, not production secret provisioning |
| Backend (Catalog-order + Operations IAM) and Platform/Operations + Security | Catalog writes share the existing four-connection LO read pool; bounded statements/lock waits do not guarantee read availability under sustained saturation, connection eviction or production load | Backend implementation destination: `OperationsCatalogVersionLifecycleIT.concurrentPublishersYieldOneActiveWinner` bounded lock/concurrent existing-read subcase and `auditFailureRollsBackLifecycle` real57014/clean-read subcase (§9.3), mandatory before implementation acceptance. Platform/Operations + Security deployment-readiness destination: load/timeout/connection-eviction capacity evidence for the unchanged four-slot/3000ms LO pool, with read503 rate and recovery observations before production enablement. Full-load capacity proof is deferred-with-owner to that gate; no production SLO, pool resize, second pool or new credential family is claimed or authorized |
| Database/Security + Catalog-order | Existing catalog_order_role over-privilege and effective ACL drift | exact-V004 matrix/negative probes, not a least-privilege claim; frozen broad SELECT/INSERT/UPDATE remains a residual; unexpected ACL drift blocks, no migration/ACL fix here |
| Platform | Audit/Outbox retention, dispatch, monitoring, and retry operations | existing A3 platform residual; PA38 adds no retention/retry policy |
| Security | Existing employee verifier availability and sanitized logger/response controls | PA38 web tests and existing Operations security owner; unavailable dependencies remain `503` |
| API governance | UI second confirmation and future trial-policy/catalog-draft commands | future slice only; no PA38 code or route |
| Security | Lock-free authority witness admits at its database read instant; a later concurrent disable may finish after that witness | accepted owner-serialization boundary, no employee/admin lock or extra audit snapshot fields; future security policy only |
| Catalog-order | Future draft replacement serialization/authoring and customer catalog filtering | separate future slices; PA38 itself performs §6.3 persisted DRAFT/snapshot/hash-format/existing-benefit structural validation, preserves zero-benefit validity and never edits content/benefit lines or extends the two commands |

No residual is unclassified. None is a blocking open question for the confirmed two-route goal. A request for a new public/schema/ACL behavior stops and returns to Controller goal confirmation.

## 13. Completion report and next gate

The implementation gate must report all 30 exact §4/§10 paths (22 new/8 modified), production Java `279 -> 290` (11 new/4 modified), test Java `169 -> 180` (11 new/2 modified, one zero-test fixture), exact §9 identities, final `97/934` Surefire plus `38/630` Failsafe, POM/CI/XML equality, source line scan `1565` vs method total `1564` with exactly one historical comment hit, 14 modules/13 discovered edges/catalog allow set 5, manifest **310/SHA256** with the Java carrier/digest procedure (no byte aggregate), frozen-file hashes, no-V019 proof, transaction/role/audit/Outbox/rollback/concurrency evidence, and §12's owner-classified residuals. It must enumerate every old/new §10 literal/diagnostic and distinguish executed results from planned commands. RR2-01..06 require the exact prefixed beans and unchanged shipped prefix/filter tests, §7.3 currentActiveVersionId values, actual §6.1 local bounds and timeout outcomes/shared-pool subcases, §6.3 structural validation including allowed zero benefits, and the all-new-test comment discipline. These are count-preserving subcases, not new identities or paths. This planning artifact claims no Maven/container/CI execution or implementation acceptance.

**Blocking open questions: none.** The next gate is **fresh independent A/B plan re-review** in peer-isolated inputs. No implementation, commit, stage, push, approval, PR mutation, or external review action is part of this plan.
