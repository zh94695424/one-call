# PA38 Operations Catalog Sale Lifecycle — Cumulative Implementation Plan

**Task label:** `pa38-operations-catalog-sale-lifecycle-plan-fix8-gpt-20261118-01`
**Work unit:** `PA38-operations-catalog-sale-lifecycle`
**Planning date:** 2026-11-18
**Status:** cumulative code-generation specification closing accepted Controller findings `PA38-PLAN-REV-01..13`, `PA38-PLAN-RR-01..10`, `PA38-PLAN-RR2-01..06`, `PA38-PLAN-RR4-01..03`, `PA38-PLAN-RR5-01..03`, `PA38-PLAN-RR6-01..04`, `PA38-RR7-01..05` and scope blocker `PA38-S1-BLOCK-01` at the plan level, subject to fresh independent A/B plan re-review; no product code is implemented by this artifact. Fix8 hardens failure-path Docker capture, asserted Git identity and changed-path allowlisting, six existing raw pins plus the 18-migration aggregate, standalone retry shells and non-merging projection setup. Fix7 pins resolved-Git/frozen-hash gates, separate Docker Client/Server evidence, single-root environment retries and the pre-implementation JVM smoke, and repairs digit-adjacent prose without changing counts. Fix6 pins the POSIX Maven/JDK/Docker preflight, both raw CRLF materialization identities, and clean full-sweep XML custody. Fix5's identical Operations/Customer route shapes, every modified-filter shipped oracle and canonical LF-projection validation without a waiver remain binding. Fix4's sole user-authorized CustomerBoundaryFilter path and exact two-command pass-through/full-chain evidence, fix3's bean names and literal/projection/timeout/content/comment constraints and fix2's other corrections remain binding. Historical fix1–7, both re-review4, both re-review5, both re-review6 and both re-review7 artifacts and the blocked implementation artifact remain frozen; runtime closure is not claimed.

## 1. Binding goal and hard boundary

The binding Goal Confirmation is `docs/gateflow/project-execution-control.md:13-36`, confirmed on **November 18, 2026**. This plan implements exactly one slice, `PA38-S1 catalog publication and offsale`, containing exactly these two routes and operation identities:

- `POST /api/v1/operations/catalog/versions/{versionId}/publications` — `publishOperationsCatalogVersion`.
- `POST /api/v1/operations/catalog/versions/{versionId}/offsales` — `offsaleOperationsCatalogVersion`.

The slice adds no route, method, request field, response field, error code, idempotency scope, table, column, index, trigger, function, grant, role, credential family, migration, cache, read model, customer flow, frontend, offer-withdrawal command, or future-slice behavior. `V001`–`V018`, with no `V019`, the frozen OpenAPI document, and the semantic content of docs 12/14/15/17 remain unchanged. A required capability absent from those frozen inputs is a blocking stop for Controller goal re-confirmation; implementation must not invent it.

**PA38-S1-BLOCK-01 — closed in this plan by explicit scope authorization, not by implementation.** Control records the user's November 18, 2026 authorization at its active implementation blocker (`:123-125`), bookkeeping HEAD `49d42b2a154e9a445a34ef0a0801076a6601e208`. Add only existing `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` to the prior implementation allowlist. The real Operations-then-Customer chain otherwise rejects both already-confirmed POSTs before MVC; §5.4 specifies the bounded early pass-through and §9.3 pins full-chain proof. This is goal-bound repair of necessary reachability for those two user-visible routes, not scope creep: no public contract, customer behavior, schema/ACL, filter order, other production path or future slice changes. The historical implementation blocker remains factual; implementation may resume only after renewed independent plan review and Controller dispatch.

Control `:120` is historical dispatch text, superseded by `:123-125` and the current bounded fix8 authorization. Any renewed implementation dispatch must bind the final fix8 cumulative plan SHA256 recorded in the fix8 artifact and the **31-path** §4/§10 allowlist, never the superseded dispatch/hash/path count. After final independent A/B review acceptance, Controller must append the fix4–8/re-review history and final plan hash to Control before implementation dispatch. Control is frozen in fix8. The prior fix7 `...-01` attempt was Controller-blocked before editing because the two re-review6 artifacts had not been imported; it is neither a review nor an edit input here.

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
- `docs/12-technology-stack-and-system-architecture.md` — SHA256 `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db`.
- `docs/14-production-data-model-and-flyway-schema-spec.md` — SHA256 `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31`.
- `docs/15-production-openapi-surface-spec.md` — SHA256 `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0`.
- `docs/17-production-security-design-spec.md` — SHA256 `b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205`.
- `backend/ops/postgresql/pa25-runtime-identities.sql` — SHA256 `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`.

The plan changes none of those files. These are six existing raw pins: OpenAPI, docs12/14/15/17 and PA25. Fix8 remeasured all six before editing, plus the new 18-migration raw-byte aggregate `53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70` (§11.4). The implementation gate must run §11.4 before implementation and again after all gates, record each before/after SHA256 and require equality to these pins, in addition to the resolved-Git frozen diff exiting 0. OpenAPI retains its existing baseline hash. A mismatch or unavailable Git environment blocks the slice; neither check substitutes for or silently skips the other.

### 2.2 Current and final filesystem inventory

The current counts were independently enumerated from the checkout, excluding `.git`, `target`, `tmp`, `node_modules`, private prompt/runner files, and generated output. Modified files do not change counts.

| Inventory | Current | PA38 final | Delta and calculation |
| --- | ---: | ---: | --- |
| production Java files under `backend/src/main/java` | 279 | 290 | `+11` new; 5 modified existing files do not increase count |
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

The current 89/848 and 36/592 were independently reproduced using the shipped CI method regex and the POM include set, not inferred from Java-file counts. The final source, CI expected maps, POM include set, and Surefire/Failsafe XML must all equal 97/934 and 38/630. The one historical comment contributes one source-scan hit but no method (§10.1). Production changes are **11 new + 5 modified = 16 Java paths**; test changes are **11 new + 2 modified = 13 Java paths**; CI and POM add **2 modified non-Java paths**. The complete implementation allowlist is **31 paths: 22 new + 9 modified**, plus the separately authorized implementation report: `(11 + 11) new + (5 + 2 + 2) modified = 31`. The same 22 new files and eight previously modified paths remain; CustomerBoundaryFilter is the sole added entry. The only implementation report path is `docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md`; it is outside the 31 code/build paths and makes the final changed-path set exactly 32, not a 32nd implementation code path.

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

### 4.2 Modified production files — 5

- `backend/src/main/java/com/onecall/core/catalog_order/package-info.java` — add the two named-interface dependencies only; preserve existing dependencies.
- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java` — add exactly the following catalog command predicate to the admission classifier, split key/media checks, preserve reads and subject-command behavior, conceal every other catalog mutation method/path. Both Operations and Customer filters must use the same exact shape (§5.1/§5.4), with only the existing local variable name differing:

  ```java
  "POST".equals(request.getMethod()) && path != null
          && path.matches("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)")
          && (request.getContextPath() + path).equals(request.getRequestURI())
  ```
- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java` — add only an overload accepting `OperationsCatalogSaleCommand.Code`, using the existing `Problem` record, reset/committed behavior, headers, UTF-8 serialization and exact byte Content-Length policy. Map `PUBLICATION_STALE_VIEW` to `409` / title `Publication stale view` and `CATALOG_PUBLICATION_CONFLICT` to `409` / title `Catalog publication conflict`; other closed codes retain shipped statuses/titles. No generic arbitrary-string code API or second envelope/writer.
- `backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java` — expose the writable manager/template specified in §2.3 over the existing LO pool; reuse the existing DSL and cleanup guard; do not alter read-template semantics or construct another datasource.
- `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` — sole fifth modified production path and sole new allowlist entry; add only §5.4's exact early POST pass-through for publications/offsales. Preserve existing subject POST and Operations GET pass-throughs, all customer protection/not-found behavior and source-literal/equality oracles; no employee authentication/authority or filter registration/order change here.

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
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java` — PostgreSQL 16 lifecycle and concurrency behavior.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java` — disposable login, effective roles, ACL, role restoration and V001–V018 proofs.
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java` — shared disposable PostgreSQL 16/web setup, teardown, command-pool recording and exact fault injection; **zero test annotations/method identities** and no POM include or CI expected class. Its owner is the two PA38 container classes, not an orphan production package.

The two modified existing test paths are exactly `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` and `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` (§10.2). The two modified non-Java paths are exactly `.github/workflows/ci.yml` and `backend/pom.xml` (§10). These close the 31-path allowlist; every other source/test/resource path is frozen.

## 5. Admission, parsing, and response contract

### 5.1 Three-way classifier

`OperationsBoundaryFilter` classifies every Operations request before route-specific validation:

1. **Read** — existing exact GET operations. No `Idempotency-Key` check is added; existing read behavior remains byte-for-byte.
2. **Idempotent subject command** — existing subject lifecycle POST operations. Preserve their exact `Idempotency-Key` required/format/replay behavior and existing content negotiation.
3. **State-conditional catalog command** — exactly the two PA38 POST templates, using this complete Operations predicate:

   ```java
   "POST".equals(request.getMethod()) && path != null
           && path.matches("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)")
           && (request.getContextPath() + path).equals(request.getRequestURI())
   ```

   Both Operations and Customer filters must use this same exact method/servlet-path/regex/context+URI shape; §5.4 differs only by its existing local name `operationsPath` instead of `path`. Require no `Idempotency-Key`; ignore its presence as a command identity and do not create an idempotency row. Apply the existing non-read exact `Content-Type` and `Accept` checks. **PA38-PLAN-RR4-01 is closed at the plan level** by §4.2/§5.1 and §9.3's missing/invalid-credential malformed-route subcase; no source oracle is relaxed.

The classifier is explicit and independently unit-tested. It never shares an `Idempotency-Key` branch with media validation. Unknown catalog mutation methods, alternate paths, encoded separators, duplicate segments, trailing segments, and every non-allowlisted catalog mutation are concealed as the shipped sanitized `NOT_FOUND`; no controller is reached.

**RR2-02 filter source lockstep:** only add non-breaking catalog classification and separate the subject-key branch from the shared non-read media checks. Preserve the current exact source literals and URI equality in `OperationsBoundaryFilter.java:38-49`, not merely equivalent routing behavior:

- Keep `"GET".equals(request.getMethod())`, `"/api/v1/operations/activations".equals(path)` and every existing GET path/regex literal, including `"/api/v1/operations/ivr-sessions"` and `"/api/v1/operations/ivr-sessions/[^/;%]+"`.
- Keep `"POST".equals(request.getMethod())` and `path.matches("/api/v1/operations/subjects/[^/;%]+/(suspensions|resumptions)")`, including the exact substring `[^/;%]+/(suspensions|resumptions)`.
- Keep the exact equality `!(request.getContextPath() + path).equals(request.getRequestURI())`; do not normalize it, replace it with prefix matching, decode alternate paths or remove the context-path check.

The unchanged shipped oracles are `OperationsLifecycleBoundaryTests.routesBinderProblemsAndCustomerPassThroughStayExact` (`:51-56`), `OperationsActivationReadBoundaryTests.employeeVerifierAndFilterLockstepStayIndependentAndExact` (`:103-106`), `OperationsIvrSessionReadBoundaryTests.operationsRoutesAndControllerMappingsAreExact` (`:58-61`, `BASE` is `/api/v1/operations/ivr-sessions`) and `OperationsSubjectReadBoundaryTests.employeeVerifierIsReusedWithoutPackagedProductionImplementation` (`:89`). The latter asserts the exact Operations authentication literal `authenticator.authenticate(request.getHeaders("Authorization"))`; preserve it and the identical `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact` assertion at `:228`. Run them without editing them. `OperationsSubjectsWebTests.operationsAndCustomerFiltersNeverCrossAuthenticate` (`:130-134`) constrains the first `operationsPath.matches` occurrence before trace initialization and forbids employee-verifier/authenticator source references there. These literal source-order assertions do not prove the new branch position: §9.3’s behavioral zero-customer-verifier/object-identity sentinel is the binding guard for that position. Preserve every current Customer filter GET/subject regex literal and `(request.getContextPath() + operationsPath).equals(request.getRequestURI())` equality; §5.4 adds a separate early branch without rewriting them. `OperationsSubjectReadConfigurationTests` also stays frozen. **PA38-PLAN-RR4-02 is closed at the plan level:** §11.1 covers all six existing regression classes, including all eight current SubjectRead boundary methods. No additional existing test edit is necessary or authorized; the 31-path allowlist and two modified test paths remain exact. If the bounded early branch cannot satisfy these frozen oracles, identify the exact contradiction and stop for Controller; never invent another test edit. The implementation artifact's unrelated pre-existing IVR line 207 failure has the checkout-materialization cause and mandatory canonical validation contract in §11.0/§12, not permission to weaken a test.

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

### 5.4 Exact downstream Customer pass-through (fix4)

Keep the real REQUEST registrations unchanged: `OperationsWebConfiguration` maps `/api/v1/operations/*` at `Ordered.HIGHEST_PRECEDENCE`; `CustomerWebConfiguration` maps `/*` at `Ordered.HIGHEST_PRECEDENCE + 1`. Operations admission still calls the existing chain with its trusted request and buffered response; Customer must not obstruct either admitted catalog command.

The separate Operations `/error` registration at `OperationsWebConfiguration.java:29-41` is irrelevant to these two commands: both command-chain registrations above are `DispatcherType.REQUEST`, and the Customer registration is REQUEST-only. No ERROR-dispatch registration or behavior changes.

In `CustomerBoundaryFilter.doFilter`, reuse `String operationsPath = request.getServletPath()`. Add only a separate early branch alongside the existing Operations pass-throughs, before `UUID traceId = UUID.randomUUID()`, customer headers, customer authentication and customer response buffering. Its complete predicate is:

```java
"POST".equals(request.getMethod()) && operationsPath != null
        && operationsPath.matches("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)")
        && (request.getContextPath() + operationsPath).equals(request.getRequestURI())
```

On exactly that predicate, call `chain.doFilter(request, response)` and immediately return, preserving the incoming Operations trusted request/principal/trace and response objects. This is only chain continuation, not a second authentication or authorization decision: no employee/customer verifier, client authority claim, role/authority test, body inspection, Idempotency-Key requirement, response/header rewrite or customer exception translation. Operations still owns employee credentials/audience, structural parsing and sanitized errors; command authority still owns current ACTIVE ADMIN. The segment regex is a route-shape guard, not a replacement for the existing canonical UUID parser.

Preserve the existing subject POST and Operations GET pass-through blocks and every other customer not-found/protection behavior. No prefix matching, wildcard catalog-version route, encoded slash/segment relaxation, alternate method, generic Operations bypass, filter reorder or configuration edit is authorized. Nonmatching requests follow the unchanged Customer path; unsafe/non-allowlisted Operations mutations are concealed by the real Operations-first chain as sanitized `404 NOT_FOUND` without controller work. Canonical-UUID/body validation failures on an admitted shape retain §5.2/§5.3's existing parsing outcome, not a newly invented route rule. §9.3 requires positive and negative full-chain evidence inside the existing planned web identities only.

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

Publication snapshots have exactly `status`, `revision`, `snapshotSha256`; offsale exactly `status`, `revision`. Never add `saleStatus`, `publishedAt`, request data, commercial contents or PII. Snapshot hash is immutable. `ACTION_DERIVED` is the existing docs 17 technical audit projection, never client input; the action is derived from the command.

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

All listed methods are exact JUnit 5 `@Test` identities. New Surefire additions total 86 methods; new Failsafe additions total 38. RR2-01..06 and fix4/fix5 add assertions/subcases inside these identities only: no class/method rename, additional annotation, parameterized/dynamic test or count increase. Every new test-bearing file, as well as the zero-test fixture, must avoid annotation-like `// @Test` comments and any other comment containing an `@Test` scan token; exactly the one historical comment in §10.1 contributes to `comment_hits`.

### 9.1 Surefire — 8 classes / 86 tests

`com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPortTests` (8): `readsOnlyStatusRoleRevision`, `isTransactionMandatory`, `usesOperationsIamRole`, `doesNotUseForUpdateOrAdvisoryLock`, `preservesCommandConnectionOnSuccess`, `marksOuterTransactionRollbackOnlyOnFailure`, `mapsMissingEmployeeToUnavailable`, `returnsImmutableSnapshot`.

`com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemanticsTests` (10): `canonicalLockKeyUsesUtf8ProductId`, `dedupeKeysUseCanonicalVersionId`, `publicationReasonIsActionDerived`, `offsaleReasonIsActionDerived`, `publicationSnapshotsContainOnlyFrozenFields`, `offsaleSnapshotsContainOnlyFrozenFields`, `rejectsMalformedPersistedState`, `rejectsRevisionOverflow`, `nullActiveExpectationIsSyntaxOnlyForActiveIdentity`, `preTransitionExpectationIsNotReplay`.

`com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryTests` (12): `readsAuthorityWithoutLocking`, `entersExactOperationsIamRole`, `entersExactCatalogOrderRoleForMutation`, `acquiresProductLockBeforeDecisionTime`, `publicationReadsCandidateAndCurrentActiveUnderLock`, `retiresOldBeforeActivatingCandidate`, `offsaleUsesConditionalRevisionUpdate`, `updatesOnlyLifecycleColumns`, `usesOnePublishAudit`, `usesTwoPublishOutboxesWhenOldActiveExists`, `usesOneOffsaleAuditAndOutbox`, `restoresRoleOnSuccessAndFailure`.

`com.onecall.core.catalog_order.internal.operationscommand.DefaultOperationsCatalogSaleCommandTests` (16): `requiresActiveAdminWitness`, `opsMapsToForbidden`, `disabledMapsToForbidden`, `missingAuthorityMapsToServiceUnavailable`, `mapsStaleExpectation`, `mapsUnmatchableStateToConflict`, `replayPerformsZeroWritesAndFacts`, `rollsBackAllFactsOnRepositoryFailure`, `publishCommandHasFrozenFields`, `offsaleCommandHasFrozenFields`, `returnsExactCatalogVersionState`, `usesClosedFailureCodes`, `doesNotExposeRequestReason`, `doesNotExposeDatabaseTypes`, `acceptsNullableExpectedActiveOnly`, `rejectsNonPositiveExpectedRevision`.

`com.onecall.core.operations_iam.internal.web.CatalogVersionRequestBinderTests` (10): `publicationRequiresExactlyTwoFields`, `publicationAcceptsExplicitNullActive`, `offsaleRequiresExactlyOneField`, `rejectsUnknownFields`, `rejectsDuplicateFields`, `rejectsTrailingTokens`, `rejectsQuotedFractionalOverflowAndZeroRevision`, `rejectsNonCanonicalUuid`, `rejectsInvalidUtf8AndOversizedBody`, `rejectsReasonAndIdempotencyFields`.

`com.onecall.archtests.OperationsCatalogVersionWebTests` (14): `admitsExactPublicationPost`, `admitsExactOffsalePost`, `classifiesReadsSubjectsAndCatalogCommandsSeparately`, `catalogCommandsDoNotRequireIdempotencyKey`, `subjectCommandsRetainExactIdempotencyRule`, `authenticationPrecedesStructuralParsing`, `structuralValidationPrecedesAuthorityWitness`, `disabledEmployeeReturnsForbiddenAfterValidParsing`, `opsReturnsForbidden`, `rejectsInvalidContentTypeAndShippedAcceptOutcome`, `successHasExactStateAndHeaders`, `staleViewHasExact409ProblemHeadersAndLength`, `concealsOtherCatalogMethodsAndUris`, `publicationConflictHasExact409ProblemHeadersAndLength`.

`com.onecall.archtests.OperationsCatalogVersionConfigurationTests` (8): `reusesExistingOperationsDatasource`, `usesNewWritableRequiresNewReadCommittedTemplate`, `reusesSamePhysicalDslBinding`, `doesNotCreateSecondPoolOrCredentialFamily`, `resolvesExactlyOneCommandProvider`, `missingProviderFailsClosed`, `ambiguousProviderFailsClosed`, `defaultRuntimeHasNoWriteBeans`.

`com.onecall.archtests.architecture.OperationsCatalogVersionBoundaryTests` (8): `scansExactProductionFiles`, `commandPortIsOperationsIamOwned`, `catalogOrderImplementsNamedPortWithoutReverseEdge`, `employeeAuthorityPortIsOperationsIamOwned`, `catalogSqlNeverReadsEmployeeAccount`, `rejectsSelectStarForbiddenTablesAndContentWrites`, `requiresOneTransactionAndPublicationOrder`, `preservesFrozenInputsAndNoV019`.

### 9.2 Failsafe PostgreSQL 16 — 2 classes / 38 tests

`com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT` (24): `adminPublishesWithoutPreviousActive`, `adminPublishesAndRetiresPreviousActive`, `retirementStatementPrecedesActivation`, `publicationIncrementsBothRevisions`, `publicationUsesOneDecisionTimestamp`, `publicationAuditArgumentsAreExact`, `publicationOutboxArgumentsAreExact`, `publicationWithOldActiveHasOneAuditAndTwoOutboxes`, `offsaleTransitionsActive`, `offsaleAuditAndOutboxArgumentsAreExact`, `publicationCurrentStateReplayIsZeroWrite`, `offsaleCurrentStateReplayIsZeroWrite`, `preTransitionPublicationRetryIsStale`, `preTransitionOffsaleRetryIsStale`, `stalePublicationMakesNoChange`, `staleOffsaleMakesNoChange`, `unexpectedStatesMapToConflict`, `publishedContentAndHistoryRemainByteIdentical`, `auditFailureRollsBackLifecycle`, `outboxFailureRollsBackLifecycle`, `concurrentPublishersYieldOneActiveWinner`, `concurrentOffsalesYieldOneWinner`, `oneActiveInvariantHoldsAfterEveryCommit`, `routeMutationOutsideTwoTemplatesIsConcealed`.

`com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT` (14): `disposableLoginHasAdminFalseInheritFalseSetTrueRoles`, `loginCanSetOperationsIamRole`, `loginCanSetCatalogOrderRole`, `operationsIamRoleReadsOnlyAuthorityColumns`, `catalogRoleCannotReadEmployeeAccount`, `catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes`, `v001ThroughV018ProvideAllRequiredObjects`, `noV019IsNeededOrPresent`, `setLocalRoleIsExactAndRestored`, `restorationFailureEvictsDirtyConnection`, `readOnlyOrWrongRoleCannotMutateCatalog`, `publishedTriggerRejectsContentMutation`, `activePartialUniqueIndexHolds`, `historicalRowsRemainUnchangedAfterRepeatedCommands`.

The no-test `OperationsCatalogVersionLifecycleFixture.java` owns shared provisioning/teardown, isolated PostgreSQL 16 data, web context, and **one LO pool** configured through `onecall.operations-subject-read.datasource.*`. Its disposable PA38 login has exactly `operations_iam_role` and `catalog_order_role` with `ADMIN FALSE`, `INHERIT FALSE`, `SET TRUE`; no production login/membership or PA32 oracle changes. Both IT classes consume this carrier; no helper test identity is invented. Use the existing DSL execute-listener pattern and test-only JDBC recording to capture statement order, bind values, backend PID, transaction ID, current/session user, transaction_read_only and row/fact before/after state. Never add a production fault switch or dependency.

### 9.3 Exact assertion ownership and fault seams (no additional tests)

- **Fix4 full-chain ownership in `OperationsCatalogVersionWebTests` (the same 14 §9.1 identities):** instantiate the real `OperationsBoundaryFilter` then real `CustomerBoundaryFilter` with their real authenticators/problem writers before the real catalog controller, as in `OperationsCatalogProductReadTestProviders.java:59-63` and the unchanged subject web fixture. Do not substitute a customer-filter mock, bypass it, dispatch directly to MVC or reorder registrations. Add all following subcases to these already planned methods; no test identity/path is added and no shipped test is edited.
  - `admitsExactPublicationPost` / `admitsExactOffsalePost`: each valid route, verified Operations principal/credentials, exact JSON and canonical UUID reaches its controller and the expected command stub once, returns 200/exact state, and preserves the Operations caller/trace. Exercise root and nonempty context paths with equal context+servlet-path/request-URI. Use no CustomerSessionVerifier provider, then a fail-on-call customer verifier registered through the existing fixture bean registry and its `ObjectProvider<CustomerSessionVerifier>` (`beans.getBeanProvider(CustomerSessionVerifier.class)`), as in the shipped fixtures; customer authentication is never invoked. No production seam or dependency is added.
  - `authenticationPrecedesStructuralParsing`: on both routes, missing/malformed/invalid/expired/revoked/wrong-audience employee credentials retain exact Operations `401 UNAUTHENTICATED`; missing/ambiguous/unavailable/throwing employee verifier retains exact sanitized `503 SERVICE_UNAVAILABLE`, including malformed-body precedence subcases. No controller invocation, command call or authority work occurs. The Customer filter is not an alternative employee authenticator and client principal/role claims grant nothing.
  - `structuralValidationPrecedesAuthorityWitness`: after valid credentials and valid syntax, missing or ambiguous `OperationsCatalogSaleCommand` providers produce exact sanitized `503 SERVICE_UNAVAILABLE` through the full chain with zero command/authority work. Retain invalid-structure-before-provider/authority checks. The §9.1 configuration identities `missingProviderFailsClosed` / `ambiguousProviderFailsClosed` remain complementary bean-resolution proof, not a substitute for these web subcases.
  - `concealsOtherCatalogMethodsAndUris` / `classifiesReadsSubjectsAndCatalogCommandsSeparately`: unsafe/malformed route alternates (encoded slash/percent/semicolon segments, duplicate/empty/trailing/additional segments, suffixes, context/servlet/request-URI mismatch), alternate methods and every non-allowlisted catalog mutation stay exact sanitized `404 NOT_FOUND` and never reach any controller/command. Include valid credentials so auth cannot mask concealment, plus unchanged subject POST/Operations GET controls. Inside the already planned `OperationsCatalogVersionWebTests.concealsOtherCatalogMethodsAndUris` identity also send `POST /api/v1/operations/catalog/versions/00000000-0000-0000-0000-000000000038/publications;x` with missing and invalid employee credentials as separate subcases, setting matching servlet path/request URI. Assert that the Operations layer returns exact sanitized `404 NOT_FOUND` before authentication, with zero employee-verifier, MVC/controller, command, authority and customer-authentication work. This malformed alternate must not become 401 or rely on a later Customer 404. Direct customer-filter chain sentinels within these identities additionally prove only §5.4's two POST shapes pass unchanged and unsafe/nonmatching catalog paths retain customer 404 without a downstream call; do not reinterpret admitted-shape UUID/body 422 as route 404. No test identity is added or renamed.
  - `catalogCommandsDoNotRequireIdempotencyKey` / `subjectCommandsRetainExactIdempotencyRule`: both catalog commands succeed without `Idempotency-Key` through both filters; its presence adds no identity/row/requirement. Existing subject commands retain the exact required key rule and existing customer/Operations pass-throughs.
  - `successHasExactStateAndHeaders`, `staleViewHasExact409ProblemHeadersAndLength`, `publicationConflictHasExact409ProblemHeadersAndLength`, `rejectsInvalidContentTypeAndShippedAcceptOutcome` and the denial subcases above: assert exact Operations status/problem or success body, trace, media type, Content-Length and full header values, including no-store/no-cache/nosniff and the shipped authentication-header policy. A direct sentinel through the real Customer branch asserts the same request/response object identity, principal/trace, untouched body/headers/status and zero customer-verifier calls; the Customer filter does not wrap, reset, authenticate, translate problems or overwrite any admitted Operations response. These are future implementation assertions, not executed evidence in this plan fix.
- `staleViewHasExact409ProblemHeadersAndLength` and `publicationConflictHasExact409ProblemHeadersAndLength`: each drives **both routes** through the real controller and writer overload with a failing command stub. Assert status 409, exact code respectively `PUBLICATION_STALE_VIEW` / `CATALOG_PUBLICATION_CONFLICT`, exact title from §4.2, exactly the existing five problem fields (`type=about:blank`, title, status, code, traceId), filter trace UUID equality, `application/problem+json`, no-store/no-cache/nosniff, Content-Length equal to serialized bytes, no `WWW-Authenticate`, no extra fields, and no SQL/role/credential/host/body/snapshot/exception canary in response or captured logs. Fold success byte-length into existing `successHasExactStateAndHeaders`; preserve canary probes in both new 409 identities and other error scenarios. Thus two planned methods are **replaced**, not appended: web remains 14 and Surefire remains 86 additions.
- `isTransactionMandatory` rejects no transaction/read-only/wrong resource and verifies no REQUIRES_NEW on the port; `preservesCommandConnectionOnSuccess` asserts the IAM role remains for the command's next switch; `marksOuterTransactionRollbackOnlyOnFailure` verifies command cleanup ownership. `setLocalRoleIsExactAndRestored` is the real PostgreSQL proof of one PID/transaction across IAM authority, catalog state/audit/Outbox, and login restoration; denial/failure variants restore without entering catalog.
- `reusesExistingOperationsDatasource`, `usesNewWritableRequiresNewReadCommittedTemplate`, and `reusesSamePhysicalDslBinding` assert raw LO datasource object identity, exact `operationsSubjectReadCatalogSaleTransactionManager` / `operationsSubjectReadCatalogSaleTransactionTemplate` qualifiers, transaction-aware DSL reuse, Spring timeout=5/propagation/isolation/writability/rollback and cleanup guard; original read manager/template remains read-only REPEATABLE_READ. Assert every new inspected configuration parameter retains the `operationsSubjectRead` qualifier prefix; run the shipped prefix test unchanged. `defaultRuntimeHasNoWriteBeans` covers missing profile/enablement. No dependency on operations-lifecycle credentials/beans is permitted.
- **RR2-02 unchanged filter proof:** `classifiesReadsSubjectsAndCatalogCommandsSeparately` covers all three branches and context-path mismatches. `OperationsCatalogVersionBoundaryTests.requiresOneTransactionAndPublicationOrder` also checks the preserved §5.1 filter source literals/equality. The three shipped boundary methods named there run unchanged alongside `OperationsSubjectsWebTests`; no extra editable path or method identity is created.
- **RR2-03 projection assertions:** `DefaultOperationsCatalogSaleCommandTests.returnsExactCatalogVersionState` and `OperationsCatalogVersionWebTests.successHasExactStateAndHeaders` each iterate real publication without/with retirement, publication replay, real offsale and offsale replay with another ACTIVE or no ACTIVE. Assert all seven fields, specifically candidate/candidate/null/other-or-null `currentActiveVersionId` as in §7.3, and web byte length/headers. `CatalogSaleLifecycleRepositoryTests.publicationReadsCandidateAndCurrentActiveUnderLock`, `retiresOldBeforeActivatingCandidate`, `offsaleUsesConditionalRevisionUpdate` and `restoresRoleOnSuccessAndFailure` verify projection order before role restoration and no stale pre-mutation ACTIVE reuse. In `OperationsCatalogVersionLifecycleIT`, add candidate ID assertions to `adminPublishesWithoutPreviousActive`, `adminPublishesAndRetiresPreviousActive`, `publicationCurrentStateReplayIsZeroWrite`; null to `offsaleTransitionsActive`; other-version ID and null subcases to `offsaleCurrentStateReplayIsZeroWrite`, with zero new facts on each replay. The existing unique/serialization recovery subcases assert the same field from the fresh transaction, never the failed transaction's cached ACTIVE.
- **RR2-04 bounds/mapping unit assertions:** `CatalogSaleLifecycleRepositoryTests.acquiresProductLockBeforeDecisionTime` verifies all three exact §6.1 SET LOCAL statements precede role/authority/catalog/lock/effect SQL on the bound connection; `restoresRoleOnSuccessAndFailure` includes setup failure and timeout rollback with unchanged guard behavior. `DefaultOperationsCatalogSaleCommandTests.mapsUnmatchableStateToConflict` adds injected `55P02` and native `55P03` subcases; `usesClosedFailureCodes` adds `57014` and pool-acquisition failure -> sanitized 503, no raw canaries, no retry/recovery facts and cleanup-failure override -> 503. Setup failure remains 503, even if the failed SET reports 55P02. New codes are not introduced.
- **RR2-04 PostgreSQL/shared-read-pool proof:** inside existing `OperationsCatalogVersionLifecycleIT.concurrentPublishersYieldOneActiveWinner`, retain the two-publisher one-winner case and add a bounded-contention subcase. A separate fixture-admin connection holds the target product advisory lock; an admitted command on the single four-slot LO pool attempts that same lock. Capture/assert its effective `statement_timeout=2s`, `lock_timeout=250ms`, `idle_in_transaction_session_timeout=5s` on its own PID/transaction before the lock attempt. While that command waits, run an existing catalog-product read through the unchanged read service/template on another connection of the **same raw pool** and assert success with exact data and restored read settings. The waiting command must terminate with real lock-timeout SQLSTATE 55P03 internally and sanitized 409 `CATALOG_PUBLICATION_CONFLICT`, zero lifecycle/audit/Outbox changes, no recovery/retry; release the held admin lock in finally. Use latches/SQL observation, not arbitrary sleeps; a generous 10-second harness deadline bounds the test, not a false exact 250ms HTTP assertion. After rollback, assert an existing read still succeeds and all returned/next-checkout sessions match guard baselines. This proves one blocked write does not consume the whole pool; it does not claim fairness or a production saturation SLO.
- **RR2-04 real statement-cancel proof:** extend existing `auditFailureRollsBackLifecycle` with a timeout variant of fixture stage `AFTER_AUDIT_APPEND`: instead of its ordinary one-shot exception, execute `SELECT pg_sleep(3)` through a JDBC statement on that captured command connection. The already-installed 2000ms statement timeout must raise real 57014, return sanitized 503 and roll back the just-appended audit plus all lifecycle/Outbox work. Assert restored timeout/role/JDBC baselines or eviction before reuse and a subsequent successful read. The listener is fixture-only, uses the bound connection and does not recursively call jOOQ or alter production SQL; retain all prior fault-stage variants. Unit mapping, real lock/read availability, and real statement-cancel subcases add zero identities. Sustained pool saturation/production capacity remains the fully classified §12 residual, not an unexecuted-runtime claim.
- **RR2-05 validation assertions:** extend `CatalogSaleLifecycleSemanticsTests.rejectsMalformedPersistedState` with malformed persisted immutable/snapshot-object/hash-format/benefit-row inputs, expected sanitized 503 and no effects; these are defensive inputs where constraints make them schema-unreachable. `CatalogSaleLifecycleRepositoryTests.publicationReadsCandidateAndCurrentActiveUnderLock` verifies the locked explicit content/benefit SELECT projections and validation before UPDATE/facts. Extend `OperationsCatalogVersionLifecycleIT.adminPublishesWithoutPreviousActive` with schema-valid zero-benefit and valid existing-benefit DRAFT subcases; both publish, keep snapshot/hash/benefits unchanged, and return the candidate ACTIVE ID. `publishedContentAndHistoryRemainByteIdentical` includes the unchanged snapshot/hash and every pre-existing benefit row. No semantic pricing or invented snapshot/benefit equivalence requirement is asserted.
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
| CustomerBoundaryFilter / authorized scope closure | existing subject POST/Operations GET early branches; catalog POST obstruction | add only §5.4's exact early POST/servlet-path/context+URI branch; preserve all existing literal/equality tests and customer behavior; fifth modified production file, 31 total paths (22 new + 9 modified) |
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
| Evidence guard product manifest digest | `12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1` | recomputed 64-hex LF digest after all 16 production changed paths, including CustomerBoundaryFilter; procedure §10.3, no fabricated future hash |
| Fix1 Java inventory ledger | historical test `179`, new test files `10`, modified production `2` | test `180`, new test files `11` (fixture no tests), modified production `5`; main `290` unchanged |
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

Reproduce the Java algorithm exactly, from `backend`: recursively walk **all regular files** under `src/main/java` and `src/main/resources`; exclude only filename `V018__evidence_role_ivr_association_read_privileges.sql` as the guard does. For each, normalize CRLF to LF, SHA256 the bytes, render lowercase hex, create `forward/slash/backend-relative/path + " " + fileHash`, sort complete lines lexicographically, join with LF and append one terminal LF. UTF-8 encode and SHA256 that string. Current independent result is **299** / **`12e70c031e42bcbca806cc1983335408d2c52fe830bd7a2ea77ddb2f1435b8c1`**. After implementation count must be **310** = 290 Java +21 resources -1 excluded V018. All five existing-production modifications, including the newly authorized CustomerBoundaryFilter change, affect the digest but not the count; production changed paths total 11 new + 5 modified = 16. The final digest must be recomputed after all implementation changes and cannot be computed before those bytes exist; implementation must replace the guard's literal with the calculated value, never commit a placeholder or guessed future digest.

Exact non-mutating recomputation (prints count and digest only):

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
cd "$PA38_CANDIDATE_ROOT/backend"
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

This planning task checks only launcher/JDK and Docker preflight commands; it does not execute Maven build/test goals, PostgreSQL 16, or the implementation tests. The following are exact implementation/review commands and expected results, not claimed runtime acceptance.

### 11.0 Mandatory host and checkout-materialization preconditions (fix6/fix7/fix8)

**PA38-PLAN-RR4-03 and PA38-PLAN-RR5-02 are closed at the plan level, without a test waiver.** The raw primary checkout of `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md` is **105,116 bytes, 1,344 CRLF and zero bare LF**; its committed Git blob is **103,772 bytes, zero CRLF and 1,344 LF**, exactly **1,344 bytes smaller**. Independently measured CRLF-to-LF conversion of the worktree bytes equals the committed blob: LF storage with Windows checkout expansion, not a CRLF committed blob. Both frozen consumers build identical LF-terminated removal mutations (`bareLine + "\n"` and `tableLine + "\n"`); against CRLF these replacements are no-ops. Exactly these two raw frozen-document assertion identities are expected:

- Surefire: `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact:207` (mutations at `:201`/`:204`).
- Failsafe: `OperationsIvrSessionReadsIT.postgres16UsesExactlyFrozenEighteenMigrations:183` (mutations at `:177`/`:180`).

Do not edit any test, fixture, drift harness, expected hash or frozen primary file; do not alter `core.autocrlf` or re-checkout/normalize the primary implementation worktree. A legitimate future PA35 revision requires explicit re-pinning, not silently changing the fixed 1,344-line precondition.

Create **one independent Git-less validation projection from the exact stable candidate tree** under WSL `/tmp`. Exclude `.git`, `.codex`, `.pa38-*`, the general private-helper pattern `.pa*-*`, `target`, `tmp` and `node_modules` at every depth. In the projection only, convert the single PA35 plan's CRLF bytes to LF; no test, source, schema or expected hash changes. The primary candidate remains the review/commit source. This projection is test-only generated state outside the candidate, never a committed path or a new implementation allowlist entry. Separate creation/materialization from the standalone host preflight below. Record and export the exact `PA38_VALIDATION_ROOT` and record the printed candidate inventory SHA256 after setup. Run preflight **before source implementation**; launcher, JDK, Docker or smoke failure is an environment stop returned to Controller, not permission to weaken tests.

**PA38-PLAN-RR6-03 single-root retry:** after an environment-only stop, repair host access and rerun only the standalone preflight and smoke against that same recorded root; do not rerun `mktemp` or materialization. Keep `set -euo pipefail` enabled without relaxation. Individual diagnostic exits may be collected only before the fail-fast canonical gate sequence or by separate commands against that same root; a failed diagnostic is not acceptance. If a new shell is needed, restore the recorded root and pinned launcher/JDK environment, not a new projection. Generated `target/` remains excluded from the candidate inventory. Only changed candidate bytes permit a new root: record both roots, the changed-byte reason and old/new candidate inventories, identify the retired root, repeat preflight/smoke and rerun every gate on the new final root. Final preflight, all canonical gates, CI oracle and XML custody must name **one final root and one candidate inventory**, never splice evidence across roots. Do not run canonical gates from the primary checkout or substitute an unlisted launcher.

**PA38-PLAN-RR5-01 / RR6-02 host contract:** the sole canonical POSIX launcher is `/opt/maven-3.9.9/bin/mvn`, JDK `/opt/jdk21` with `JAVA_HOME=/opt/jdk21` and `/opt/jdk21/bin` prepended to `PATH`. Require preflight output `Apache Maven 3.9.9` and Java `21.0.13` from the bound projection backend. Separately record `docker version --format '{{.Client.Version}}'`, `docker version --format '{{.Server.Version}}'` and `docker info --format '{{.ServerVersion}}|{{.OperatingSystem}}|{{.KernelVersion}}'`, with each command's raw exit, exact Client/Server version outputs and a server-specific `docker info` observation (or the server-access error). Require all three to exit 0, nonempty Client/Server versions, and three nonempty server-backed info fields from a reachable daemon. Capture stdout/stderr and each actual exit with explicit `if output=$(command 2>&1); then rc=0; else rc=$?; fi` before printing evidence; never put an unguarded potentially failing substitution before its exit line. Complete diagnostic collection may precede the final nonzero preflight stop; canonical gates remain fail-fast after evidence. Explicitly state that the reported server version came from the **Server field**, not a client banner. Historical `29.1.3` observations and a client-only `29.1.3` cannot satisfy preflight. Windows interop is not required. Never execute or normalize frozen CRLF `backend/mvnw` in canonical runs; no Windows-wrapper fallback is authorized.

**PA38-RR7-03/04:** every standalone command block below re-declares the launcher, JDK, roots and required Controller Git bindings; no blank launcher record is possible. Setup refuses any already-set validation root, creates a fresh temporary parent and a nonexistent `candidate` child, and refuses a second setup or an existing destination rather than merging. Only preflight/smoke/gates reuse that existing root after environment repair.

Run this creation/materialization setup once per candidate from the primary candidate repository root, in one Bash shell retained for all §11 commands; it does not run host preflight. `set -euo pipefail` remains enabled in setup, preflight, smoke and all canonical gates:

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
test "$PWD" = "$PA38_CANDIDATE_ROOT"
test -z "${PA38_VALIDATION_ROOT+x}"
PA38_VALIDATION_PARENT="$(mktemp -d /tmp/pa38-lf-validation.XXXXXX)"
export PA38_VALIDATION_ROOT="$PA38_VALIDATION_PARENT/candidate"
test ! -e "$PA38_VALIDATION_ROOT"
test ! -L "$PA38_VALIDATION_ROOT"
python3 - <<'PY'
import os
from hashlib import sha256
from pathlib import Path
from shutil import copytree, ignore_patterns
candidate = Path(os.environ['PA38_CANDIDATE_ROOT']).resolve()
projection = Path(os.environ['PA38_VALIDATION_ROOT']).resolve()
assert candidate != projection and candidate not in projection.parents
excluded = ignore_patterns('.git', '.codex', '.pa38-*', '.pa*-*', 'target', 'tmp', 'node_modules')
def inventory(root):
    entries = {}
    for directory, directories, filenames in os.walk(root):
        ignored = excluded(directory, directories + filenames)
        directories[:] = sorted(name for name in directories if name not in ignored)
        for filename in sorted(name for name in filenames if name not in ignored):
            path = Path(directory) / filename
            entries[path.relative_to(root).as_posix()] = sha256(path.read_bytes()).hexdigest()
    return entries
before = inventory(candidate)
assert not projection.exists() and not projection.is_symlink(), 'projection already exists; refuse merge'
copytree(candidate, projection, ignore=excluded, symlinks=True)
assert inventory(projection) == before
assert not (projection / '.git').exists()
relative = 'docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md'
target = projection / relative
assert not target.is_symlink()
raw = target.read_bytes()
assert raw.count(b'\r\n') == 1344 and raw.count(b'\n') == 1344
canonical = raw.replace(b'\r\n', b'\n')
target.write_bytes(canonical)
expected = dict(before)
expected[relative] = sha256(canonical).hexdigest()
assert inventory(projection) == expected
assert inventory(candidate) == before
binding = '\n'.join(path + ' ' + digest for path, digest in sorted(before.items())) + '\n'
print('candidate inventory SHA256', sha256(binding.encode('utf-8')).hexdigest())
print('projection', projection)
print('PA35 raw SHA256', sha256(raw).hexdigest())
print('PA35 canonical LF SHA256', sha256(canonical).hexdigest())
print('all other projected bytes and all primary bytes unchanged')
PY
export PA38_VALIDATION_ROOT
printf 'recorded root: %s\n' "$PA38_VALIDATION_ROOT"
printf 'restore in a new shell (all bindings):\n'
printf 'set -euo pipefail\n'
printf 'export JAVA_HOME=%q\nexport PATH=%q\n' "$JAVA_HOME" "$PATH"
printf 'PA38_MAVEN=('; printf '%q ' "${PA38_MAVEN[@]}"; printf ')\n'
printf 'export PA38_CANDIDATE_ROOT=%q\nexport PA38_VALIDATION_ROOT=%q\n' "$PA38_CANDIDATE_ROOT" "$PA38_VALIDATION_ROOT"
printf 'export PA38_EXPECTED_HEAD=%q\nexport PA38_EXPECTED_TREE=%q\nexport PA38_EXPECTED_BRANCH=%q\n' "$PA38_EXPECTED_HEAD" "$PA38_EXPECTED_TREE" "$PA38_EXPECTED_BRANCH"
```

Run this **standalone preflight** after setup, or rerun it alone after environment repair using the recorded root, without recreating the projection. The block collects all four command outputs/exits without disabling `set -euo pipefail`, prints their evidence, then enforces zero exits and server data before any smoke or canonical gate. Failure returns nonzero; never treat captured error text as a version:

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend"
 printf 'launcher: '; printf '%q ' "${PA38_MAVEN[@]}"; printf '\n'
 printf 'JAVA_HOME=%s\nPATH=%s\ncandidate=%s\nprojection_backend=%s\n' "$JAVA_HOME" "$PATH" "$PA38_CANDIDATE_ROOT" "$PWD"
 test -x "${PA38_MAVEN[0]}"
 test -x "$JAVA_HOME/bin/java"
 if PA38_MAVEN_VERSION=$("${PA38_MAVEN[@]}" -v 2>&1); then PA38_MAVEN_RC=0; else PA38_MAVEN_RC=$?; fi
 printf 'Maven/JDK raw exit=%s\n%s\n' "$PA38_MAVEN_RC" "$PA38_MAVEN_VERSION"
 if PA38_DOCKER_CLIENT=$(docker version --format '{{.Client.Version}}' 2>&1); then PA38_CLIENT_RC=0; else PA38_CLIENT_RC=$?; fi
 printf 'Docker Client raw exit=%s\n%s\n' "$PA38_CLIENT_RC" "$PA38_DOCKER_CLIENT"
 if PA38_DOCKER_SERVER=$(docker version --format '{{.Server.Version}}' 2>&1); then PA38_SERVER_RC=0; else PA38_SERVER_RC=$?; fi
 printf 'Docker Server raw exit=%s (Server field, never client banner)\n%s\n' "$PA38_SERVER_RC" "$PA38_DOCKER_SERVER"
 if PA38_DOCKER_INFO=$(docker info --format '{{.ServerVersion}}|{{.OperatingSystem}}|{{.KernelVersion}}' 2>&1); then PA38_INFO_RC=0; else PA38_INFO_RC=$?; fi
 printf 'Docker server-backed info raw exit=%s\n%s\n' "$PA38_INFO_RC" "$PA38_DOCKER_INFO"
 test "$PA38_MAVEN_RC" -eq 0
 test "$PA38_CLIENT_RC" -eq 0
 test "$PA38_SERVER_RC" -eq 0
 test "$PA38_INFO_RC" -eq 0
 test -n "$PA38_DOCKER_CLIENT"
 test -n "$PA38_DOCKER_SERVER"
 printf '%s\n' "$PA38_MAVEN_VERSION" | grep -F 'Apache Maven 3.9.9'
 printf '%s\n' "$PA38_MAVEN_VERSION" | grep -F 'Java version: 21.0.13,'
 printf '%s\n' "$PA38_MAVEN_VERSION" | grep -F 'runtime: /opt/jdk21'
 export PA38_DOCKER_INFO
 python3 - <<'PY'
import os
fields = os.environ['PA38_DOCKER_INFO'].strip().split('|')
assert len(fields) == 3 and all(field.strip() for field in fields), fields
PY
)
```

After launcher/JDK/Docker preflight succeeds, run this one bounded existing-class smoke **before source implementation**, on the same root. It checks build execution and JVM agent/self-attach capability, not PA38 acceptance totals. Failure is an environment stop for Controller/validation-host resolution; do not weaken tests or continue implementation. Report the command, actual exit and root separately from canonical totals:

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend" && \
  "${PA38_MAVEN[@]}" -Dtest=OperationsSubjectReadConfigurationTests test)
```

Raw diagnostics are **optional, supplemental and non-binding**, never acceptance gates. Historical raw evidence may be cited without rerunning it. If recording a fresh raw-checkout regression diagnostic after successful preflight, keep it separate from canonical evidence:

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_CANDIDATE_ROOT/backend" && "${PA38_MAVEN[@]}" -Dtest='OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectReadBoundaryTests' test)
```

Record command, actual exit, XML totals and exact assertion identities. The **only expected raw frozen-document failures** are `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact:207` and `OperationsIvrSessionReadsIT.postgres16UsesExactlyFrozenEighteenMigrations:183`; a raw Surefire-only selector exposes only the first, not evidence that the second is absent. Both are checkout materialization, neither product failure nor waiver. Any additional assertion failure blocks; never relabel it as CRLF. Launcher, Docker, JVM-agent/self-attach, missing dependency or similar environment failures are **environment stop/resolve items**, not relabeled product failures or accepted results. Record and return them to Controller/test-boundary owner; resolve and rerun before acceptance, without weakening a test/fixture/expected hash. Record the exact `PA38_MAVEN` array, JAVA_HOME/PATH, actual Maven/JDK/Docker versions, projection backend and individual exits. Historical blocked runs remain raw evidence, not candidate validation.

All §11.1 regression/focused Surefire/guard commands, §11.2 focused Failsafe, §11.3 **clean** full sweep and shipped CI/XML oracle use the **same LF projection** and named `PA38_MAVEN` launcher/JDK. **Only canonical LF-projection runs are acceptance gates**: all must exit 0 with zero skipped/failures/errors and exact inventories. Any canonical assertion failure, including either frozen-document identity, blocks; environment failures stop for resolution and rerun, never count as acceptance. No raw-focused/full-green claim or waiver replaces these requirements. Report available raw diagnostics separately from canonical results, projection provenance/exclusions, one-document normalization/raw+canonical hashes and unchanged primary/frozen bytes. Static Git/diff/frozen verification (§11.4) remains against the Git-bearing primary candidate. Build/test commands are specified here, not executed by this plan fix.

### 11.1 Focused Surefire

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend" && "${PA38_MAVEN[@]}" -Dtest='EmployeeAuthorityReadPortTests,CatalogSaleLifecycleSemanticsTests,CatalogSaleLifecycleRepositoryTests,DefaultOperationsCatalogSaleCommandTests,CatalogVersionRequestBinderTests,OperationsCatalogVersionWebTests,OperationsCatalogVersionConfigurationTests,OperationsCatalogVersionBoundaryTests' test)
```

Expected: all **8** new unit classes execute; the selector contains all eight names and no class is silently omitted; 86 tests, zero skipped/failures/errors; assertions include `OPS=403`, disabled-after-parse `403`, unacceptable Accept shipped `500`, exact **both 409** codes/envelopes/trace/headers/length on both routes, success Content-Length/headers, leak canaries, exact LO wiring, mandatory authority, port ownership, and zero catalog employee-table reads. The same 14 web identities also prove §9.3's real Operations-then-Customer chain: both commands reach MVC, auth 401/503 and provider 503 retain precedence/no command work, unsafe alternatives 404 never reach a controller, no catalog Idempotency-Key requirement, and no customer authentication/response mutation.

Run the following unchanged shipped configuration/filter regression group separately in the LF projection (**6 existing classes / 52 existing methods** = 8+8+8+8+12+8 = 44+8 = 52, no additions to global totals or test identities). The appended `OperationsSubjectReadBoundaryTests` has exactly eight current `@Test` methods and remains frozen/unmodified:

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend" && "${PA38_MAVEN[@]}" -Dtest='OperationsSubjectReadConfigurationTests,OperationsLifecycleBoundaryTests,OperationsActivationReadBoundaryTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectsWebTests,OperationsSubjectReadBoundaryTests' test)
```

Expected in the canonical LF projection: prefix oracle, every §5.1 exact filter source-literal/equality/authentication guard (including both duplicate authentication-literal assertions) and subject behavior stay green without editing any of these six files; exactly 52 tests, zero skipped/failures/errors. The 8 new-class/86-method selector above additionally covers RR2-03 response derivation, RR2-04 setup/error mapping and RR2-05 structural/zero-benefit subcases from §9.3, plus fix5's unauthenticated malformed-route 404 precedence, with unchanged counts.

Focused existing guards (run separately so totals are unambiguous):

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend" && "${PA38_MAVEN[@]}" -Dtest='ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests' test)
```

Expected: **2 classes / 17 tests** (9+8), unchanged shipped identities; exact 14 modules, 13 discovered edges, catalog allowed set 5, three IAM named-interface tuples, frozen operations_iam allow-map, manifest 310/computed digest and CI-token lockstep. The manifest expectation applies after implementation, not in this document-only task.

### 11.2 Focused Failsafe

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend" && "${PA38_MAVEN[@]}" -Pcontainer-smoke -Dit.test=OperationsCatalogVersionLifecycleIT,OperationsCatalogVersionRoleLeastPrivilegeIT verify)
```

Expected: exactly two PA38 Failsafe reports, 38 tests (24+14), zero skipped/failures/errors; assertions include both replays, pre-transition stale, ACTIVE/matching-current/wrong-revision stale, one-winner races, direct 23505 and injected 40001 recovery, new recovery transaction on LO, zero recovery facts, one-ACTIVE invariant, immutable content/history, exact eleven/eight audit/Outbox arguments, rollback fault stages, PID/transaction/role restoration/eviction, exact V004 42-tuple matrix plus negatives, membership flags, V001–V018/no-V019. Include every §9.3 RR2 subcase: exact currentActiveVersionId values (including other ACTIVE/null offsale replay), zero-benefit publication, actual local limits, real 55P03 lock conflict with a successful concurrent existing read on the same LO pool, real 57014 statement cancel with atomic rollback and clean subsequent read. Failsafe's `verify` lifecycle also runs the complete Surefire suite unless explicitly skipped; its expected Surefire totals are 97/934, not zero or 8/86. The no-test fixture produces no report.

### 11.3 Full gate

**PA38-PLAN-RR5-03:** run `clean -Pcontainer-smoke verify`, then the shipped complete CI Python oracle and XML custody inventory in the **same shell invocation**, chained on success below. `clean` removes reports from focused selectors or earlier/aborted sweeps. No intervening Maven/selector run or report copy is allowed; never use focused-selector reports for final totals. The oracle runs only after that clean full sweep succeeds and against that sweep’s own generated XML.

Expected in the canonical LF projection: existing complete Surefire/Failsafe suites plus PA38, exact final `97/934` and `38/630`, POM includes 38, no skipped/failures/errors, all CI source-subtraction and manifest guards green, and no primary frozen input changes. The sole projection-only PA35 document normalization is §11.0's materialization precondition, not a source/test/schema/hash edit or a waiver.

Execute the clean sweep, shipped oracle (without installing a YAML parser) and custody inventory together:

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
(cd "$PA38_VALIDATION_ROOT/backend" && "${PA38_MAVEN[@]}" clean -Pcontainer-smoke verify && python3 - <<'PY'
from pathlib import Path
from textwrap import dedent
workflow = Path('../.github/workflows/ci.yml').read_text()
marker = "          python3 - <<'PY'\n"
start = workflow.index(marker) + len(marker)
end = workflow.index('\n          PY', start)
exec(compile(dedent(workflow[start:end]), 'ci-container-inventory', 'exec'))
from hashlib import sha256
report_paths = sorted(
    [*Path('target/surefire-reports').glob('TEST-*.xml'),
     *Path('target/failsafe-reports').glob('TEST-*.xml')],
    key=lambda path: path.as_posix())
assert sum(path.parent.name == 'surefire-reports' for path in report_paths) == 97
assert sum(path.parent.name == 'failsafe-reports' for path in report_paths) == 38
report_inventory = {path.as_posix(): sha256(path.read_bytes()).hexdigest()
                    for path in report_paths}
report_binding = ''.join(filename + ' ' + digest + '\n'
                         for filename, digest in report_inventory.items())
print(report_binding, end='')
print('final XML files', len(report_inventory))
print('final XML inventory SHA256', sha256(report_binding.encode('utf-8')).hexdigest())
for class_name in ('OperationsCatalogVersionLifecycleIT',
                   'OperationsCatalogVersionRoleLeastPrivilegeIT'):
    filename = ('target/failsafe-reports/TEST-com.onecall.archtests.container.'
                + class_name + '.xml')
    assert filename in report_inventory
    print('PA38 Failsafe XML', filename, report_inventory[filename])
PY
)
```

Expected: the early XML assertion uses 934, all source=CI=POM=XML exact identities/totals match §10, historical method/line subtraction families remain unchanged, comment_hits is exactly one, and the PA38 summary ends 135/1564, line scan 1565, POM 38. Run only after the successful clean full sweep above, never against focused-selector or copied reports.

Custody algorithm: enumerate exactly `target/surefire-reports/TEST-*.xml` and `target/failsafe-reports/TEST-*.xml` from that sweep (97+38=135 files), sort by backend-relative POSIX filename in ordinal order, hash each XML’s raw bytes with SHA256 to lowercase hex, serialize each entry as `filename + single ASCII space + lowercase SHA256 + LF` including the final LF, UTF-8 encode the concatenation and SHA256 it to lowercase hex. Record the inventory, 135-file count and aggregate, plus the exact filenames and individual SHA256 values of `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml` and `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml`. XML/report files remain generated projection output and are never committed. If sweep, oracle or custody fails, resolve and rerun the clean full sequence; no final totals are accepted from an earlier selector.

### 11.4 Static/frozen verification

**PA38-PLAN-RR6-01:** run from the Git-bearing primary candidate, never the Git-less projection, before implementation and again after validation. Derive the real Windows-pointer gitdir process-locally; every Git invocation in this section must use `"${PA38_GIT[@]}"`. Ordinary unresolved Git is an **environment stop**, never a skipped check. Do not edit `.git` or repository config. Require the pinned raw-byte hashes below both before and after, as well as the exact resolved-Git diff exiting 0. Using `HEAD` in the diff covers staged and unstaged tracked changes; status must also account for untracked paths.

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
cd "$PA38_CANDIDATE_ROOT"
PA38_GIT_POINTER="$(sed -n 's/^gitdir: //p' "$PA38_CANDIDATE_ROOT/.git" | tr -d '\r')"
: "${PA38_GIT_POINTER:?missing gitdir pointer}"
PA38_GITDIR="$(wslpath -u "$PA38_GIT_POINTER")"
test -d "$PA38_GITDIR"
PA38_GIT=(
  env
  GIT_DIR="$PA38_GITDIR"
  GIT_WORK_TREE="$PA38_CANDIDATE_ROOT"
  GIT_OPTIONAL_LOCKS=0
  git
  -c
  core.safecrlf=false
)
"${PA38_GIT[@]}" status --short --branch
"${PA38_GIT[@]}" rev-parse HEAD
"${PA38_GIT[@]}" rev-parse HEAD^{tree}
"${PA38_GIT[@]}" branch --show-current
test "$("${PA38_GIT[@]}" rev-parse HEAD)" = "$PA38_EXPECTED_HEAD"
test "$("${PA38_GIT[@]}" rev-parse HEAD^{tree})" = "$PA38_EXPECTED_TREE"
test "$("${PA38_GIT[@]}" branch --show-current)" = "$PA38_EXPECTED_BRANCH"
test "$PA38_EXPECTED_BRANCH" = codex/pa38-next-work-unit
"${PA38_GIT[@]}" diff --check
find backend/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' -printf '%f\n' | sort
test "$(find backend/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' | wc -l)" -eq 18
test -z "$(find backend/src/main/resources/db/migration -maxdepth 1 -name 'V019__*.sql' -print -quit)"
sha256sum --check <<'PA38_FROZEN_SHA256'
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db  docs/12-technology-stack-and-system-architecture.md
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205  docs/17-production-security-design-spec.md
b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9  backend/ops/postgresql/pa25-runtime-identities.sql
PA38_FROZEN_SHA256
"${PA38_GIT[@]}" diff --exit-code HEAD -- api/openapi/onecall-v1.openapi.yaml docs/12-technology-stack-and-system-architecture.md docs/14-production-data-model-and-flyway-schema-spec.md docs/15-production-openapi-surface-spec.md docs/17-production-security-design-spec.md backend/src/main/resources/db/migration backend/ops/postgresql/pa25-runtime-identities.sql
python3 - <<'PY'
from hashlib import sha256
from pathlib import Path
migration_paths = sorted(Path('backend/src/main/resources/db/migration').glob('V*.sql'))
assert len(migration_paths) == 18
assert [path.name.split('__', 1)[0] for path in migration_paths] == [f'V{number:03}' for number in range(1, 19)]
assert all(path.is_file() and not path.is_symlink() for path in migration_paths)
binding = ''.join(path.as_posix() + ' ' + sha256(path.read_bytes()).hexdigest() + '\n'
                  for path in migration_paths)
aggregate = sha256(binding.encode('utf-8')).hexdigest()
print(binding, end='')
print('18-migration raw-byte aggregate SHA256', aggregate)
assert aggregate == '53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70'
PY
```

**PA38-RR7-02/05:** Controller dispatch must supply `PA38_EXPECTED_HEAD`, `PA38_EXPECTED_TREE`, `PA38_EXPECTED_BRANCH`; neither printing identities nor defaulting to observed values is sufficient. Fix8 observed HEAD `49d42b2a154e9a445a34ef0a0801076a6601e208`, tree `5034910465b8b9e97bfeabe80c192f7742670897`, branch `codex/pa38-next-work-unit`. After the required Controller checkpoint, dispatch must bind its actual full HEAD/tree and the same branch; do not blindly reuse historical values. All six existing raw pins (OpenAPI, docs12/14/15/17, PA25), plus the new 18-migration aggregate, are required before implementation and after gates with equality to these fixed pins. Migration serialization is ordinal-sorted candidate-relative POSIX filename + one ASCII space + lowercase SHA256 of raw file bytes + LF, including final LF; SHA256 the UTF-8 concatenation. Raw migration bytes are never EOL-normalized, closing the `core.autocrlf=true` blind spot; the frozen pathspec diff remains an independent requirement.


**PA38-RR7-02 final changed-path gate:** after implementation, run this additional standalone block. It repeats the resolved identity/frozen checks above and asserts the complete tracked-against-HEAD plus all-untracked set, including ignored untracked files (`ls-files --others` deliberately has no `--exclude-standard`). Exclude only `.codex`, `.pa*-*`, `target`, `tmp` and `node_modules` path components; no plan, fix, review, Control or arbitrary docs exclusion exists. Missing and extra paths both fail. This exact-set check is final-only: before implementation, the 22 new paths must not yet exist. Controller must checkpoint accepted planning/review/bookkeeping artifacts before supplying dispatch HEAD/tree/branch, otherwise those artifacts correctly fail as extras. This fix does not checkpoint them. The independent before/after frozen/identity gate above is mandatory in both phases.

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
: "${PA38_MAVEN[0]:?canonical launcher is required}"
export PA38_CANDIDATE_ROOT=/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call
export PA38_EXPECTED_HEAD="${PA38_EXPECTED_HEAD:?Controller dispatch must supply full HEAD}"
export PA38_EXPECTED_TREE="${PA38_EXPECTED_TREE:?Controller dispatch must supply full tree}"
export PA38_EXPECTED_BRANCH="${PA38_EXPECTED_BRANCH:?Controller dispatch must supply branch}"
: "${PA38_CANDIDATE_ROOT:?candidate root is required}"
test -d "$PA38_CANDIDATE_ROOT/backend"
test -f "$PA38_CANDIDATE_ROOT/.git"
export PA38_VALIDATION_ROOT="${PA38_VALIDATION_ROOT:?restore the recorded root; never recreate it for an environment retry}"
case "$PA38_VALIDATION_ROOT" in /tmp/pa38-lf-validation.*/candidate) ;; *) exit 1 ;; esac
test "$PA38_VALIDATION_ROOT" != "$PA38_CANDIDATE_ROOT"
test -d "$PA38_VALIDATION_ROOT/backend"
cd "$PA38_CANDIDATE_ROOT"
PA38_GIT_POINTER="$(sed -n 's/^gitdir: //p' "$PA38_CANDIDATE_ROOT/.git" | tr -d '\r')"
: "${PA38_GIT_POINTER:?missing gitdir pointer}"
PA38_GITDIR="$(wslpath -u "$PA38_GIT_POINTER")"
test -d "$PA38_GITDIR"
PA38_GIT=(
  env
  GIT_DIR="$PA38_GITDIR"
  GIT_WORK_TREE="$PA38_CANDIDATE_ROOT"
  GIT_OPTIONAL_LOCKS=0
  git
  -c
  core.safecrlf=false
)
"${PA38_GIT[@]}" status --short --branch
"${PA38_GIT[@]}" rev-parse HEAD
"${PA38_GIT[@]}" rev-parse HEAD^{tree}
"${PA38_GIT[@]}" branch --show-current
test "$("${PA38_GIT[@]}" rev-parse HEAD)" = "$PA38_EXPECTED_HEAD"
test "$("${PA38_GIT[@]}" rev-parse HEAD^{tree})" = "$PA38_EXPECTED_TREE"
test "$("${PA38_GIT[@]}" branch --show-current)" = "$PA38_EXPECTED_BRANCH"
test "$PA38_EXPECTED_BRANCH" = codex/pa38-next-work-unit
"${PA38_GIT[@]}" diff --check
find backend/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' -printf '%f\n' | sort
test "$(find backend/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' | wc -l)" -eq 18
test -z "$(find backend/src/main/resources/db/migration -maxdepth 1 -name 'V019__*.sql' -print -quit)"
sha256sum --check <<'PA38_FROZEN_SHA256'
0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68  api/openapi/onecall-v1.openapi.yaml
9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db  docs/12-technology-stack-and-system-architecture.md
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205  docs/17-production-security-design-spec.md
b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9  backend/ops/postgresql/pa25-runtime-identities.sql
PA38_FROZEN_SHA256
"${PA38_GIT[@]}" diff --exit-code HEAD -- api/openapi/onecall-v1.openapi.yaml docs/12-technology-stack-and-system-architecture.md docs/14-production-data-model-and-flyway-schema-spec.md docs/15-production-openapi-surface-spec.md docs/17-production-security-design-spec.md backend/src/main/resources/db/migration backend/ops/postgresql/pa25-runtime-identities.sql
python3 - <<'PY'
from hashlib import sha256
from pathlib import Path
migration_paths = sorted(Path('backend/src/main/resources/db/migration').glob('V*.sql'))
assert len(migration_paths) == 18
assert [path.name.split('__', 1)[0] for path in migration_paths] == [f'V{number:03}' for number in range(1, 19)]
assert all(path.is_file() and not path.is_symlink() for path in migration_paths)
binding = ''.join(path.as_posix() + ' ' + sha256(path.read_bytes()).hexdigest() + '\n'
                  for path in migration_paths)
aggregate = sha256(binding.encode('utf-8')).hexdigest()
print(binding, end='')
print('18-migration raw-byte aggregate SHA256', aggregate)
assert aggregate == '53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70'
PY
python3 - "${PA38_GIT[@]}" <<'PY'
import fnmatch
import os
from pathlib import PurePosixPath
import subprocess
import sys
expected = set(""".github/workflows/ci.yml
backend/pom.xml
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepository.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemantics.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommand.java
backend/src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java
backend/src/main/java/com/onecall/core/catalog_order/package-info.java
backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPort.java
backend/src/main/java/com/onecall/core/operations_iam/employeeauthority/package-info.java
backend/src/main/java/com/onecall/core/operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java
backend/src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsCatalogVersionController.java
backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/OperationsCatalogSaleCommand.java
backend/src/main/java/com/onecall/core/operations_iam/operationscommand/package-info.java
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionConfigurationTests.java
backend/src/test/java/com/onecall/archtests/OperationsCatalogVersionWebTests.java
backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java
backend/src/test/java/com/onecall/archtests/architecture/OperationsCatalogVersionBoundaryTests.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleFixture.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionLifecycleIT.java
backend/src/test/java/com/onecall/archtests/container/OperationsCatalogVersionRoleLeastPrivilegeIT.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleRepositoryTests.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/CatalogSaleLifecycleSemanticsTests.java
backend/src/test/java/com/onecall/core/catalog_order/internal/operationscommand/DefaultOperationsCatalogSaleCommandTests.java
backend/src/test/java/com/onecall/core/operations_iam/employeeauthority/EmployeeAuthorityReadPortTests.java
backend/src/test/java/com/onecall/core/operations_iam/internal/web/CatalogVersionRequestBinderTests.java
docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md""".splitlines())
assert len(expected) == 32

def excluded(path):
    return any(part in {'.codex', 'target', 'tmp', 'node_modules'}
               or fnmatch.fnmatchcase(part, '.pa*-*')
               for part in PurePosixPath(path).parts)

def assert_changed_paths(tracked, untracked):
    actual = {path for path in tracked | untracked if not excluded(path)}
    missing = sorted(expected - actual)
    extra = sorted(actual - expected)
    print('missing implementation paths', missing)
    print('extra changed paths', extra)
    assert not missing and not extra, 'changed-path allowlist mismatch'

def git_paths(arguments):
    result = subprocess.run(sys.argv[1:] + arguments, check=True, stdout=subprocess.PIPE)
    return {os.fsdecode(path) for path in result.stdout.split(b'\0') if path}

tracked = git_paths(['diff', '--name-only', '--no-renames', '-z', 'HEAD', '--'])
untracked = git_paths(['ls-files', '--others', '-z', '--'])
assert_changed_paths(tracked, untracked)
print('changed-path allowlist: exactly 31 implementation paths plus one report')
PY
```

Expected in this planning task: only the two authorized documents differ from task-entry raw inventory; no implementation exact-set success is claimed. In implementation, the final allowlist must pass exactly.

## 12. Residuals and destinations

| Owner | Residual | Destination and disposition |
| --- | --- | --- |
| Platform/Security/Operations | Production LO property enablement, credentials/secret delivery, exact seven PA25 SET edges including catalog_order_role, pool health | deployment readiness; PA38 proves fail-closed unavailable/ambiguous behavior and tested cleanup, not production secret provisioning |
| Backend (Catalog-order + Operations IAM) and Platform/Operations + Security | Catalog writes share the existing four-connection LO read pool; bounded statements/lock waits do not guarantee read availability under sustained saturation, connection eviction or production load | Backend implementation destination: `OperationsCatalogVersionLifecycleIT.concurrentPublishersYieldOneActiveWinner` bounded lock/concurrent existing-read subcase and `auditFailureRollsBackLifecycle` real 57014/clean-read subcase (§9.3), mandatory before implementation acceptance. Platform/Operations + Security deployment-readiness destination: load/timeout/connection-eviction capacity evidence for the unchanged four-slot/3000ms LO pool, with read 503 rate and recovery observations before production enablement. Full-load capacity proof is deferred-with-owner to that gate; no production SLO, pool resize, second pool or new credential family is claimed or authorized |
| Database/Security + Catalog-order | Existing catalog_order_role over-privilege and effective ACL drift | exact-V004 matrix/negative probes, not a least-privilege claim; frozen broad SELECT/INSERT/UPDATE remains a residual; unexpected ACL drift blocks, no migration/ACL fix here |
| Platform | Audit/Outbox retention, dispatch, monitoring, and retry operations | existing A3 platform residual; PA38 adds no retention/retry policy |
| Security | Existing employee verifier availability and sanitized logger/response controls | PA38 web tests and existing Operations security owner; unavailable dependencies remain `503` |
| API governance | UI second confirmation and future trial-policy/catalog-draft commands | future slice only; no PA38 code or route |
| Security | Lock-free authority witness admits at its database read instant; a later concurrent disable may finish after that witness | accepted owner-serialization boundary, no employee/admin lock or extra audit snapshot fields; future security policy only |
| Catalog-order | Future draft replacement serialization/authoring and customer catalog filtering | separate future slices; PA38 itself performs §6.3 persisted DRAFT/snapshot/hash-format/existing-benefit structural validation, preserves zero-benefit validity and never edits content/benefit lines or extends the two commands |
| Backend test-boundary owner + Controller | LF committed PA35 blob 103,772 bytes vs CRLF worktree 105,116 bytes (1,344 CRLF/zero bare LF); identical frozen LF-removal mutations produce exactly two expected raw identities: `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact:207` and `OperationsIvrSessionReadsIT.postgres16UsesExactlyFrozenEighteenMigrations:183`; historical blocked evidence remains unchanged | Raw diagnostics are supplemental/non-binding; only §11.0’s candidate-bound LF projection with named POSIX launcher/JDK and §11.1–§11.3 canonical gates can establish acceptance. Any additional raw assertion or any canonical assertion blocks; launcher/Docker/JVM-agent/self-attach/missing-dependency or similar environment failures stop for Controller/host resolution, not product-failure relabeling. No waiver or test/fixture/expected-hash/frozen-primary edit; no core.autocrlf change. Require clean full sweep, its own CI/XML oracle and §11.3 custody inventory/aggregate plus both PA38 XML hashes; reports remain generated, not committed |
| Controller + validation-host owner | `/opt/maven-3.9.9/bin/mvn`, `JAVA_HOME=/opt/jdk21`, PATH prepended `/opt/jdk21/bin`, Maven 3.9.9/Java 21.0.13 and reachable Docker are mandatory; a client banner is not server evidence | Before source implementation, run §11.0 standalone preflight and existing-class smoke on the recorded WSL `/tmp` root. Record Client/Server fields and each raw exit, plus a server-specific `docker info` observation. Resolve environment access and retry preflight/smoke on that same root; regenerate only when candidate bytes change. Final preflight/gates/oracle/XML custody bind one final root/inventory. No primary-checkout canonical gate, unlisted launcher, CRLF-wrapper normalization or Windows fallback |
| Controller | Historical Control `:120` dispatch is superseded by `:123-125` and current fix7 authorization | After final A/B acceptance, append fix4–7/re-review history and the final hash to Control, then bind renewed implementation dispatch to the fixed plan SHA256 in the fix7 artifact and the 31-path allowlist, preserving `PA38-RR3-OPS-01..05`; never reuse historical dispatch text |

No residual is unclassified. None is a blocking open question for the confirmed two-route goal. A request for a new public/schema/ACL behavior stops and returns to Controller goal confirmation.

## 13. Completion report and next gate

The implementation gate must report all 31 exact §4/§10 paths (22 new/9 modified), production Java `279 -> 290` (11 new/5 modified = 16 changed production paths), test Java `169 -> 180` (11 new/2 modified = 13 changed test paths, one zero-test fixture), and 2 modified build/CI paths: `22 new + 9 modified = 31`. Report exact §9 identities, final `97/934` Surefire plus `38/630` Failsafe = `135/1,564` focused identities, POM includes 38/CI/XML equality, source line scan `1565` vs method total `1564` with exactly one historical comment hit, 14 modules/13 discovered edges/catalog allow set 5, manifest **310/SHA256** recomputed after all production changes with the Java carrier/digest procedure (no byte aggregate or guessed digest), frozen-file hashes, no-V019 proof, transaction/role/audit/Outbox/rollback/concurrency evidence, and §12's owner-classified residuals. It must enumerate every old/new §10 literal/diagnostic and distinguish executed results from planned commands. RR2-01..06 require the exact prefixed beans and unchanged shipped prefix/filter tests, §7.3 currentActiveVersionId values, actual §6.1 local bounds and timeout outcomes/shared-pool subcases, §6.3 structural validation including allowed zero benefits, and the all-new-test comment discipline. Fix4 additionally requires the sole newly authorized CustomerBoundaryFilter path, exact §5.4 POST/servlet-path/context+URI predicate, unchanged filter registrations/order/customer behavior and all six frozen §11.1 regression test files, and §9.3's full-chain assertions within the existing 14 web identities: both valid commands reach the controller, employee auth 401/503 before command work, missing/ambiguous command provider 503, unsafe/non-allowlisted mutations 404 without controller work, no catalog Idempotency-Key requirement, and untouched Operations status/problem/body/headers with zero customer authentication. These are count-preserving subcases, not new identities or test paths. Scope blocker `PA38-S1-BLOCK-01` is closed in this plan only; implementation must supply executable chain proof and §11.0's canonical validation, never claim the frozen raw IVR drift harness was repaired. This planning artifact claims no Maven build/test, container test, CI-oracle execution or implementation acceptance; fix6/fix7 record only non-mutating launcher/JDK/Docker diagnostics; bound-projection smoke and acceptance gates remain unexecuted.

Fix5 implementation evidence must additionally close `PA38-PLAN-RR4-01..03` executably: identical §4.2/§5.1/§5.4 Operations/Customer predicates, the existing concealment identity's missing/invalid-credential malformed alternate returning Operations 404 with zero downstream/authentication work, all §5.1 frozen oracles in the **6-class/52-method** regression selector (`8+8+8+8+12+8 = 44+8 = 52`), and §11.0's exact candidate-bound LF-projection precondition. Report raw-checkout command/exit/assertion separately from canonical LF-projection regression 52, focused Surefire 86, guards 17, focused Failsafe 38 (and its full Surefire 934), full 97/934+38/630 and shipped CI/XML exit 0 evidence; no raw-checkout focused/full-green claim or waiver is required or permitted as a substitute. Record candidate/projection binding, exclusions, one-document raw/LF hashes, unchanged primary/frozen bytes, exact §11.0 PA38_MAVEN launcher/JDK/Docker preflight evidence and runtime limitations. Preserve test identities, expected hashes, registrations and ERROR behavior. Plan-level closure is not executed evidence.

Fix6 implementation evidence must additionally close `PA38-PLAN-RR5-01..03` executably: §11.0 launcher/JDK/Docker preflight before source implementation and again in the final bound projection; the same named launcher/projection for every §11.1–§11.3 gate; both exact raw materialization identities `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact:207` and `OperationsIvrSessionReadsIT.postgres16UsesExactlyFrozenEighteenMigrations:183` classified only as supplemental/non-binding raw diagnostics. Only canonical LF runs establish acceptance; any additional raw assertion or any canonical assertion blocks, while launcher/Docker/JVM-agent/self-attach/missing-dependency or similar environment failure stops for resolution, never product-failure relabeling. No waiver or test/fixture/expected-hash edit. Report `clean -Pcontainer-smoke verify` exit, then the shipped oracle exit against that sweep’s own XML, exact 97/934+38/630 totals, the §11.3 deterministic 135-file filename+SHA256 inventory aggregate and both named PA38 Failsafe XML filenames/SHA256 values. Never use focused-selector reports for final totals; generated reports are not committed. Quote §9.3’s behavioral zero-customer-verifier/object-identity sentinel as the binding guard for the new Customer branch position: literal source-order assertions constrain only the first `operationsPath.matches`.

Fix7 implementation evidence must additionally close `PA38-PLAN-RR6-01..04`: derive the process-local §11.4 Git array from the real `.git` pointer, record resolved status/full HEAD/tree/frozen diff and the six existing raw pins plus the new 18-migration aggregate, all with before/after equality measured before implementation; unresolved Git stops rather than skipping a check. Record Docker Client and Server version commands separately with each raw exit, `docker info` raw exit and server-specific observation, explicitly attributing the server version to the Server field and never a client banner. Record/export the exact validation root after separate materialization; environment-only retries rerun preflight/smoke on that same root without relaxing `set -euo pipefail`. Only changed candidate bytes justify a new root; record why both exist, both inventories and which single final root produced final preflight, every canonical gate, CI oracle and XML custody. Run the bounded `OperationsSubjectReadConfigurationTests` smoke after launcher/Docker preflight and before source implementation for JVM agent/self-attach/build capability, separately from acceptance totals. Any smoke/environment failure stops without weakening tests. Preserve all numbers/identities. Historical frozen fix6's digit-adjacent V004 ACL wording means **V004 42-tuple**; Controller must record that clarification without editing fix6.

Fix8 implementation evidence must additionally close `PA38-RR7-01..05`: explicit guarded stdout/stderr/exit captures and real Server-backed data; `wslpath -u` pointer conversion, required asserted Controller Git identity and exact final 31-path-plus-report allowlist; all six existing raw pins plus the new 18-migration raw aggregate before/after; full independent launcher/JDK/root/Git declarations and complete restore hints; fresh-parent/nonexistent-child setup with no merge and loud second-setup failure. Environment retries reuse only the recorded root's preflight/smoke/gates. All RR4/RR5/RR6 and prior contracts remain binding.

For this bounded plan-fix hand-back, report task/full HEAD, the two documentation-only changed paths, original/final plan SHA256, fix8 SHA256/size, RR7-01..05 plan-level closure, exact migration aggregate and six raw pins, expected Git variables, allowlist algorithm, guarded Docker pattern, retry/projection contract, actual exits and residual owners. Record the original blocked implementation hash unchanged. Controller alone appends fix4–8/re-review history, final hash and V004 42-tuple clarification after final A/B acceptance; do not edit Control here. Stop with `plan-fix-complete` before targeted final independent A/B re-review: no new review conclusion, implementation, later gate or Controller acceptance claim. Supplied re-review4–7 and fix1–7 remain frozen inputs.

**Blocking open questions: none.** The next gate is **fresh independent A/B plan re-review** in peer-isolated inputs. No implementation, commit, stage, push, approval, PR mutation, or external review action is part of this plan.
