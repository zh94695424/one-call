# PA33 Operations subject read foundation — S1 final implementation evidence report

## 1. Task, authority and handoff

- Task: `pa33-operations-subject-read-s1-final-report-gpt-20261114-01`; date: **2026-11-14**; gate: **implementation final evidence-report handoff**.
- Required dispatch: **Codex / GPT `gpt-6.1-sol`, reasoning `high`**. This document records the required dispatch, not independent model attestation; Controller owns verification of runner metadata. No provider switch or delegated work is claimed here.
- Actual measured HEAD: **`33260cf6d643dc6f31e8b54c4014d85794bdfb60`**. This is the current bookkeeping/base HEAD, not a claim that the uncommitted S1 implementation is already committed. Historical `6f0b7be8` and `185a91f3` references in earlier artifacts are not the current HEAD.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa33-next-work-unit/codex-call`. The Windows `.git` pointer is preserved. Read-only Git uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call17` and `GIT_COMMON_DIR=/mnt/d/codex-call/.git`.
- Authority: the PA33 implementation tail of `docs/gateflow/project-execution-control.md`, cumulative accepted plan and accepted `PA33-PLAN-CORRECTION-01`. Exactly **one S1 behavior slice**, complete authoritative Operations subject list/detail, is delivered. Production/tests work packets, bounded fixes, validation and report handoff are not additional acceptance slices.
- Controller has completed full candidate validation. This author task replaces only this report; it does not self-accept S1 or start independent review. Controller's next destination is report intake, accepted S1 local commit and a freshly frozen independent A/B code-review gate.

This supersedes the earlier blocked-intake report. Initial child-sandbox Docker denial, the later context-budget stop and raw Windows Git-pointer failure were execution problems, not continuing database/source blockers. Controller subsequently proved Docker Server 29.1.3 and PostgreSQL16 availability and completed the required validation. The interrupted production packet's accidental Maven cache was moved recoverably outside the worktree; it is not shipped scope. No historical blocked or failed run is relabeled as passing.

### Aggregate fix 1 update — 2026-11-14

Task `pa33-aggregate-fix1-gpt-20261114-01` applies the authorized aggregate fixes against accepted-slice bookkeeping base `8e0ec184d0584b75cd1929873d8f895b2e5267ef` (slice `5cec8a6da16c3531b8e21afadee85144320641a3`). The original task/HEAD, full-validation measurements and handoff statements elsewhere in this report remain historical S1 evidence, not measurements or acceptance claims for this fix. Current fix evidence, exact ten-path manifest, runner outcomes and residual limits are in `docs/gateflow/pa33-aggregate-fix-1.md`.

- **PA33-AGG-01 fixed:** a request-local monotonic five-second budget is captured before transaction acquisition, checked around authority/owner calls, each activation-filter batch and successful cleanup, and fails closed with sanitized 503/rollback/no partial response. A package-private clock constructor permits deterministic assertions inside the existing `candidateBudgetExhaustionNeverInventsTailOrContinuation` identity. Transaction timeout and statement/lock/idle settings and capacity ceilings remain unchanged; transaction timeout alone is not the deadline mechanism.
- **PA33-AGG-02 fixed:** known owner `Failure` retains its code; expected jOOQ/Spring data, transaction and bean exceptions remain sanitized 503; unknown owner runtime faults reach sanitized INTERNAL/500. The existing `sqlBeanTransactionAndUnknownFailuresMapWithoutLeakingCauses` identity now exercises mock ports as well as authority and structural failures on both GETs.
- **PA33-AGG-04 bounded part fixed:** one enum-owned role-name table serves both role switching and assertion; all five owner adapters explicitly import only `Failure` and `Code` from the service. Operations ownership, public interfaces, generated SQL and frozen contracts are unchanged.
- **PA33-AGG-03 remains deferred:** the frozen detail A0 metadata omission belongs to a separately authorized Contract/OpenAPI and Security/Data correction; no OpenAPI/docs/14/15/17/migration edit is included.
- **Fresh validation:** required focused Surefire **83/83**; required container-profile verify **644/644 Surefire + 36/36 Failsafe** (12 role least-privilege + 24 reads), zero failures/errors/skips. Exact **106 PA33 identities** remain unchanged; full static source/CI/POM inventory remains **70/644 + 29/476 = 99/1120**, with 29 Failsafe includes and 1121 line tokens. All **22 raw frozen hashes** and the empty frozen diff are preserved; `git diff --check` passes. The other 440 container identities are static inventory/historical evidence, not newly executed tests in this bounded fix.

Fix-author completion does not accept the aggregate or start re-review. The five-second budget is checked at synchronous operation boundaries, not an asynchronous cancellation/watchdog; an already-running owner operation and required cleanup can finish before exhaustion is observed. Per-statement DB bounds remain in force. Existing deployment, capacity, index/SLO and identity/key-custody risks remain with their recorded owners.

## 2. Exact delivered scope: 37 product paths

**37 = 25 new + 12 existing**: 15 new production paths, eight new annotated test classes, two new non-test fixtures; eight existing production paths, two existing architecture tests, POM and CI. Equivalently: **23 production + 10 new test/fixture + 2 existing test + 2 build/CI paths**. The 15 new production files contain 14 top-level types plus one package-info; the eight existing production files comprise three classes and five package-info files.

In the following exact manifest, `P/` expands literally to `backend/src/main/java/com/onecall/core/` and `T/` to `backend/src/test/java/`; these are path prefixes, not globs.

| # | Classification | Exact path |
| --- | --- | --- |
| 1 | New production metadata | `P/operations_iam/subjectread/package-info.java` |
| 2 | New production API | `P/operations_iam/subjectread/OperationsSubjectReadService.java` |
| 3 | New production inbound ports | `P/operations_iam/subjectread/OperationsSubjectReadPorts.java` |
| 4 | New production role context | `P/operations_iam/subjectread/OperationsReadContext.java` |
| 5 | New production service | `P/operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java` |
| 6 | New production parser | `P/operations_iam/internal/subjectread/OperationsSubjectReadRequest.java` |
| 7 | New production cursor | `P/operations_iam/internal/subjectread/OperationsSubjectCursorCodec.java` |
| 8 | New production properties | `P/operations_iam/internal/subjectread/OperationsSubjectReadProperties.java` |
| 9 | New production persistence configuration | `P/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java` |
| 10 | New production controller | `P/operations_iam/internal/web/OperationsSubjectReadController.java` |
| 11 | New production subject owner adapter | `P/subject_number/internal/operationsread/JooqOperationsSubjectReader.java` |
| 12 | New production activation owner adapter | `P/activation_consent/internal/operationsread/JooqOperationsActivationReader.java` |
| 13 | New production trial owner adapter | `P/entitlement_metering/internal/operationsread/JooqOperationsTrialReader.java` |
| 14 | New production evidence count adapter | `P/evidence/internal/operationsread/JooqOperationsEvidenceCounter.java` |
| 15 | New production order count adapter | `P/catalog_order/internal/operationsread/JooqOperationsOrderCounter.java` |
| 16 | Existing production filter | `P/operations_iam/internal/web/OperationsBoundaryFilter.java` |
| 17 | Existing production filter | `P/customer_iam/internal/web/CustomerBoundaryFilter.java` |
| 18 | Existing production problem writer | `P/operations_iam/internal/web/OperationsProblemWriter.java` |
| 19 | Existing production module metadata | `P/subject_number/package-info.java` |
| 20 | Existing production module metadata | `P/activation_consent/package-info.java` |
| 21 | Existing production module metadata | `P/entitlement_metering/package-info.java` |
| 22 | Existing production module metadata | `P/evidence/package-info.java` |
| 23 | Existing production module metadata | `P/catalog_order/package-info.java` |
| 24 | New Surefire class | `T/com/onecall/archtests/OperationsSubjectReadWebTests.java` |
| 25 | New Surefire class | `T/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadRequestTests.java` |
| 26 | New Surefire class | `T/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectCursorCodecTests.java` |
| 27 | New Surefire class | `T/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadServiceTests.java` |
| 28 | New Surefire class | `T/com/onecall/archtests/OperationsSubjectReadConfigurationTests.java` |
| 29 | New Surefire architecture class | `T/com/onecall/archtests/architecture/OperationsSubjectReadBoundaryTests.java` |
| 30 | New Failsafe class | `T/com/onecall/archtests/container/OperationsSubjectReadsIT.java` |
| 31 | New Failsafe class | `T/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java` |
| 32 | New non-test container fixture | `T/com/onecall/archtests/container/OperationsSubjectReadFixture.java` |
| 33 | New non-test unit fixture | `T/com/onecall/archtests/operations/OperationsSubjectReadTestProviders.java` |
| 34 | Existing architecture oracle | `T/com/onecall/archtests/architecture/ModuleBoundaryTests.java` |
| 35 | Existing architecture oracle, accepted correction | `T/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java` |
| 36 | Existing build configuration | `backend/pom.xml` |
| 37 | Existing CI configuration | `.github/workflows/ci.yml` |

The cumulative plan and new `docs/gateflow/pa33-operations-subject-read-foundation-plan-fix-2.md` are separately authorized gate artifacts, not hidden product entries. This final report is a separate gate artifact too. Control and prior review artifacts are inputs, not writes by this task. Pre-existing untracked `.codex` and `.pa33-plan-fix1-prompt.md` are preserved/excluded. Controller's `final-static.log` reports the exact 37-path product set, frozen=22, migrations=17, and `FINAL_STATIC_OK`. Generated target/XML/JAR/npm outputs used for validation are not delivery paths; no generated or unlisted product path is included. The workspace is not claimed literally clean.

## 3. Delivered behavior and trust boundary

### Routes, authority and error precedence

Only `GET /api/v1/operations/subjects` and `GET /api/v1/operations/subjects/{subjectId}` are newly admitted. Existing lifecycle POSTs retain their behavior. Raw/context/servlet path agreement and exact route predicates conceal wrong methods, encoded/percent/semicolon spellings, double/trailing slashes, extra segments, wrong case and hidden `/employees` routes as sanitized 404 before authentication. Customer-boundary pass-through uses the same exact admission boundary rather than a broad Operations-prefix bypass.

The existing independent opaque employee Bearer verifier seam and audience **`onecall:employee-session:v1`** are reused, with exactly one verifier and exactly one read-service provider. Missing/malformed/duplicate/rejected bearer or wrong audience is 401; unavailable, ambiguous or uncreatable dependencies are sanitized 503. Client employee/role/organization/source-domain assertions never confer authority. Within the read transaction, `operations_iam.employee_account` is re-read: missing employee is 401, current DISABLED is 403, current ACTIVE ADMIN or OPS is admitted, corrupt/mismatching authority is 503. No employee row/advisory serialization lock is acquired.

For admitted GETs, bearer verification precedes service resolution, transaction/current authority, query validation, Accept validation, cursor configuration/authentication, owner-provider resolution and projection. Thus disabled authority plus incompatible Accept is 403, not 422; not-found does not precede authority. GET Accept is not negotiated by the filter or Spring `produces`: successful buffered output explicitly uses `application/json`, errors use `application/problem+json`, and incompatible/malformed JSON media is 422 after authority, not Spring 406. GET bodies and command headers do not become inputs. Existing POST media behavior is unchanged.

Problems retain exactly `type,title,status,code,traceId`, `about:blank`, server UUID traces, no-store/no-cache/nosniff, Bearer challenge on 401 only and no cookies. Cursor-invalid/filter-mismatch are 400; validation 422; absence 404; SQL/data/transaction/bean/role/timeout/capacity failures 503; ordinary unknown implementation failure 500 INTERNAL. No raw cause, SQLSTATE, SQL, credential, employee fact or cursor is exposed; cleanup failure overrides a would-be success/error with sanitized unavailability.

### Query, pagination and opaque cursor

List accepts exactly `cursor,limit,subjectId,source,claimStatus,activationStatus,realNameStatus,riskState`; detail accepts no query parameters. Unknown keys, repeated keys including identical cursor values, empty/invalid noncursor values and noncanonical UUIDs fail with 422 after authority. UUIDs require canonical lowercase spelling. Enums are exact, without trimming/case folding. Limit defaults to 20, accepts ASCII one-to-three-digit values 1–100, and normalizes `020` to 20; signs, whitespace, Unicode digits and overflow fail. Noncursor/shape validation precedes single-cursor validation. No free-text/full-number search, offset or total is added.

Seek order is immutable `(created_at DESC, id DESC)` with matching lookahead, at most limit returned rows, and continuation anchored to the **last returned row**, not the lookahead/scanned row. `hasMore` requires a proved matching lookahead; empty/tail responses have `nextCursor: null`. Latest claim is selected by `risk_checked_at DESC,id DESC` before claim filtering. Activation filtering performs owner-port batch lookup, with 101 candidates per batch and at most 100 batches/10,100 candidates. If it cannot prove a tail or sufficient lookahead within budget, the whole read fails 503; no fabricated continuation, partial success or offset fallback.

Cursor v1 uses JDK AES-256-GCM with a fresh 12-byte nonce, purpose-bound AAD, explicit version/key ID header, authenticated 60-byte payload (epoch seconds, nanos, UUID, SHA-256 filter digest) and 16-byte tag. Canonical unpadded base64url output is 122–163 characters, within the public 512-character bound. Payload nanos must be PostgreSQL microsecond aligned (`nano % 1000 == 0`). No plaintext is interpreted before authentication. Corrupt/noncanonical/tampered/wrong-purpose/retired-key tokens yield CURSOR_INVALID; only authenticated ciphertext with a different normalized filter digest yields CURSOR_FILTER_MISMATCH. The ordered six-filter digest uses explicit absent sentinels and excludes page size, so parameter order/page-size changes do not change it.

The lazy independent key ring has one active issuing key and up to two retained verify-only keys, strict unique key IDs and independent canonical 32-byte key material via indexed external properties. No generated fallback, key reuse, implicit secret discovery, new SDK or production secret is shipped. Lists require usable keys even on page one/empty results; detail does not. Request/key/anchor/failure representations are redacted, including the fixed anchor representation.

### Safe projections and bounded completeness

SubjectSummary has exactly **eight allowed properties**, not seven: required `subjectId,source,activationStatus,realNameStatus,riskState,createdAt`; optional `maskedNumber,claimStatus`. SubjectPage has exactly `items,nextCursor,hasMore`. Detail has exactly **12 allowed properties**: always `subjectId,source,realNameStatus,riskState,bindings,claims,evidenceCount,orderCount,createdAt`, optionally `maskedNumber,activation,trial`. Empty histories remain arrays; missing optional facts are omitted. Missing activation projects list INACTIVE but no fabricated detail activation/revision.

Display binding ranks ACTIVE, DISABLED, RETIRED then ID; histories use binding ID ascending and claim risk-check timestamp/ID descending. Risk is OPEN if any binding, including disabled/retired, has OPEN risk. Unsafe display strings become `[REDACTED:PHONE]`. No customer/organization ownership, full-number/ciphertext, verification reference, recording, media key or playback access is returned. Detail fetches at most 101 bindings/claims each: 0/1/100 are complete, 101 means whole-response 503 and rollback, not truncation.

Trial is absent only when no trial exists. Existing trial/grant/balance relationships, FIRST-TRIAL and TRIAL/RECORDING_SECONDS semantics, original granted units, matching stored expiries and consistent counters/status are required; malformed facts fail 503 without repair. Remaining seconds are granted minus reserved minus consumed. One DB transaction timestamp decides EXPIRED before EXHAUSTED before GRANTED; fully reserved but unconsumed may remain GRANTED with zero remaining. Evidence/order counts are exact nonnegative int64 totals across all subject statuses and purchasers, without ownership filters or multiplying joins. No policy lookup, trial reissue, balance mutation or media access occurs.

### Owner roles, snapshot, cleanup and zero-write proof

The read graph is lazy and conditional on `persistence` plus explicit `onecall.operations-subject-read.enabled=true`; the servlet controller additionally needs `web`. It uses dedicated credentials/pool, not customer/command resources. Default/web-only/disabled graphs create no read pool or production verifier. Missing credentials/providers fail at the affected authenticated request, not inert boot.

One dedicated transaction is **REQUIRES_NEW, REPEATABLE_READ, enforced database READ ONLY**, timeout 5 seconds; pool maximum four, connection timeout 3000 ms; local statement/lock/idle timeouts 2000/250/5000 ms. The same bound physical connection/backend and snapshot carry authority and all five owner projections. `operations_iam_role` reads authority; literal scoped roles `subject_number_role`, `activation_consent_role`, `entitlement_metering_role`, `evidence_role`, `catalog_order_role` execute owner-local reads, never foreign base-table SQL in the controller/application or owner elevation.

Tests prove a synthetic login with exactly six non-inherited SET memberships suffices without changing frozen grants. Foreign owner reads fail PostgreSQL 42501. Owner handoffs restore Operations role; outer cleanup restores the entry login; rollback/reset removes local role/settings and transaction locks. Restoration faults fail closed, and same physical backend reuse is verified clean (role, read-only/isolation/autocommit, local settings and no residual locks/transaction). No savepoint continuation, privilege repair or retry is introduced.

Success, denial, not-found and injected faults perform zero business/domain writes, audit/access-event facts or Outbox effects. PostgreSQL read-only enforcement rejects writes and locking queries. One-snapshot tests cover employee, subject, activation, trial and counts; committed changes become visible on the next request. These are disposable PG16 runtime proofs, not production-login provisioning or cross-page export-snapshot guarantees.

## 4. Test inventory and measured validation

### Exact inventory and focused run

**106 new method identities = 70 Surefire + 36 Failsafe** in exactly six + two new classes. The two fixtures produce no test XML. Existing method identities remain, including all four CustomerEvidenceBoundaryTests and all nine ModuleBoundaryTests methods.

| Suite | New class | New identities |
| --- | --- | ---: |
| Surefire | `OperationsSubjectReadWebTests` | 16 |
| Surefire | `OperationsSubjectReadRequestTests` | 10 |
| Surefire | `OperationsSubjectCursorCodecTests` | 12 |
| Surefire | `OperationsSubjectReadServiceTests` | 16 |
| Surefire | `OperationsSubjectReadConfigurationTests` | 8 |
| Surefire | `OperationsSubjectReadBoundaryTests` | 8 |
| Failsafe | `OperationsSubjectReadsIT` | 24 |
| Failsafe | `OperationsSubjectReadRoleLeastPrivilegeIT` | 12 |

Focused final run: **12 classes/133 Surefire + 4 classes/72 Failsafe = 16 classes/205 tests**, zero failures/errors/skips, BUILD SUCCESS, **02:50**. Evidence: `/mnt/d/codex-temp/pa33-s1-tests-retry2-20261114-01/focused-final.log`, `focused-final-xml/` and `focused-xml-equality.log`. New 70 + retained (8+12+8+8+9+18)=133; new 36 + retained lifecycle24/privilege12=72. Archived focused source=CI=XML maps identify the exact 16 classes/method sets; these are separate from later full XML, not inferred from overwritten target reports. The accepted correction additionally passes 21/21 architecture regression tests (§6).

### Controller full clean and checked-in CI with wrapper substitution

Controller evidence root **E** = `/mnt/d/codex-temp/pa33-controller-full-validation.XZ1XlF` (Windows `D:\codex-temp\pa33-controller-full-validation.XZ1XlF`).

| Measurement | Observed result | Evidence under E |
| --- | --- | --- |
| Full `-Pcontainer-smoke clean verify` | BUILD SUCCESS, **27:49**, finished **2026-11-14T19:50:07+08:00** | `full.log` final results/build time |
| Surefire | **70 classes / 644 tests**, zero failures/errors/skips | `xml/surefire/`, Surefire final result in `full.log` |
| Failsafe | **29 classes / 476 tests**, zero failures/errors/skips | `xml/failsafe/`, final result in `full.log` |
| Total | **99 classes / 1120 tests**, no bad/skipped XML nodes | 99 XML copies plus `xml-manifest.txt` |
| Exact inventories | **source = CI = POM = XML**, `70/644 + 29/476 = 99/1120` | `ci-step-1.log` |
| Annotation/token lines and POM | **1121** comment-inclusive `@Test` token lines; **29** exact Failsafe includes | `ci-step-1.log` |
| PostgreSQL | **16.15**, exactly 17 applied/validated migrations; new IT 24 + privilege IT 12 pass | targeted results in `full.log`, corresponding XML |
| Packaged default/web smokes | **passed**, no DB/verifier adapter, JVM cleaned up | `ci-step-2.log` |
| Dependency firewall | **passed**, unchanged Python/comparison logic, baseline/deltas and **116 adversarial probes**; single Maven wrapper invocation substituted as documented below | `ci-step-3.log` |

The current total rises from retained **574+440=1014** to **644+476=1120**, exactly 106. Token 1121 is not a 1121st executed test: it is the separate comment-inclusive source oracle. POM has two new IT includes and 27 retained includes; no dependency/plugin/version expansion is justified by this feature. CI preserves historical inventory/subtraction oracles rather than just accepting aggregate totals.

This reporting task independently rehashed all **99/99 XML manifest entries**, parsed 70/29 XML to reproduce **644/476** with zero failures/errors/skips, and verified all **22/22 frozen manifest hashes** against the current bytes. The expensive suite was not rerun by the reporting author; the fresh full-run and CI claims are explicitly Controller evidence. `full.stderr` contains Mockito/ByteBuddy dynamic-agent/CDS warnings, not a test failure; the resolved agent is existing `byte-buddy-agent/1.18.15`.

Default packaged JAR exits successfully without web/Hikari startup; opt-in web boots without DB/verifier and exercises read auth/unavailability/hidden route boundaries alongside retained POST/customer/public/internal probes. Packaged safeguards cover exact Problem fields/headers, no test provider/fixture or production EmployeeSessionVerifier adapter, and no duplicate ZIP entries. Passing without an adapter means fail-closed boot/admission proof, not usable production employee authentication.

Firewall output is exactly: **baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1; deltas35/0 and31/0; 116 adversarial probes**. Positive-first approved legacy-regex collisions: `spring-boot-web-server-4.1.2.jar`, `spring-boot-webmvc-4.1.2.jar`, `spring-web-7.0.10.jar`, `spring-webmvc-7.0.10.jar`, `tomcat-embed-core-11.0.27.jar`, `tomcat-embed-el-11.0.27.jar`, `tomcat-embed-websocket-11.0.27.jar`. Only packager branch: `spring-boot-jarmode-tools-4.1.2.jar`; commons-logging is parsed; tuple order is `group:artifact:type:version:classifier:scope`. These are the existing firewall's exact classifications, not newly approved dependencies.

## 5. A1–A5, CRLF evidence and frozen boundaries

**A1 passes**: locked OpenAPI `check:s3`, including lint/bundle/generate/typecheck/doc-map/media gateway checks, with **49 total A1 API operations** (`a1_operations=49`), not 49 Operations endpoints or 49 new endpoints; PA33 delivers only the two named GETs. Evidence: E/`contract.log`, ending A1 at `PASS check-doc-map (s3)`.

The **raw A2–A5 regression sequence is not green**. E/`contract.log` reproduces the known CRLF A0-parser failure and stops at A2 `FAIL protocol-validation (s3)`: 25/25 fixtures pass and A1 drift=0, but parsed A0 status/registry/column drift is **8/6/40**. Thus this Controller raw log does not independently show A3–A5 were each run to completion. Earlier per-validator raw logs under `/mnt/d/codex-temp/pa33-s1-tests-retry2-20261114-01/` retain A2 failure, A3/A4 passes and A5 s1/s2 `state table header changed in section 7.1`; it would be inaccurate to say every raw validator failed or all passed.

The permitted remedy is comparison-only LF-equivalent temporary input mirrors **outside the repository**, preserving frozen checkout bytes. E/`contract-lf.log` records:

- **A2 s3 PASS**: 25/25 fixtures, negative=3, 44 A0 tables, all A0 status/registry/column drift **0/0/0**, A1 operations49/drift0.
- **A3 s3 PASS**: 23/23 fixtures, negative=10, drift=0; 39 audit events/persistence entries and 21 invariants.
- **A4 s2 PASS**: 113/113 fixture cases, 471 nonfinite probes (157 leaves × NaN/+Infinity/-Infinity), deterministic/repeat checks; deployment template remains INCOMPLETE and productionReady/procurementReady/cloudVerified/a3RiskClosed remain false.
- **A5 s1 PASS**: 96 cells (90 represented/6 not applicable), 547 source refs, drift/unresolved/ambiguous=0, 3/3 fixtures.
- **A5 s2 PASS including doc links**: same matrix/source closure, 4/4 fixtures, final `A5_S2_LF_EQUIVALENT_OK`; runtime_evidence=NOT_RUN, evidence manifests/admitted/acceptance records=0, production_accepted=false.

The LF log also honestly retains two intermediate A5 s2 mirror-setup failures: missing temporary `README.md`, then unresolved relative links for an acceptance fixture and gateflow plan. Controller completed the temporary mirror's link context (`lf-docs/`, `lf-root/`); the final s2/doc-link run passes. These were not product/frozen-document edits or suppressed failures. LF-equivalent pass is not a raw-byte-checkout pass, hosted clean-Linux CI pass, production acceptance or cloud/runtime A5 evidence.

**22 frozen raw SHA256 hashes match**: docs/12, docs/14, docs/15, docs/17, OpenAPI, and **V001–V017**. E/`frozen-after.txt` is the exact manifest reproduced in Appendix B; E/`frozen-diff.log` is empty. Migration enumeration is exactly 17, no V018, added migration, schema/grant/function/index change or frozen-file normalization. E/`git-diff-check.log` is empty; `final-static.log` reports `FINAL_STATIC_OK` with only known LF-to-CRLF Git advisories. Generated mirror/build outputs remain evidence, not delivered source paths.

## 6. Findings, accepted correction and regressions

| Identity | Root cause and disposition | Exact regression result |
| --- | --- | --- |
| **PA33-IMPL-01 — fixed** | Anchor's default representation exposed subject UUID/time; `OperationsSubjectReadPorts.Anchor.toString()` now returns `Anchor[REDACTED]`. | Initial parser/codec run: **22 tests, 21 passed, 1 failure**, zero errors/skips, failing `OperationsSubjectCursorCodecTests.keysRequestsAnchorsAndFailuresHaveRedactedRepresentations`. Fixed request/codec rerun: **22/22**, zero failures/errors/skips; all 22 identities also pass full XML. |
| **PA33-IMPL-02 — fixed** | Spring bean-creation availability failure escaped as 500; read-service classification now includes `BeansException` with data/transaction failures as sanitized 503. Unknown `IllegalStateException` remains INTERNAL/500. | `OperationsSubjectReadServiceTests.sqlBeanTransactionAndUnknownFailuresMapWithoutLeakingCauses` initially failed **BeanCreationException expected 503 but was 500**. Exact service regression plus prior request/codec suites: **23/23**, zero failures/errors/skips; final service class **16/16** and prior request/codec **22/22** pass full XML. |
| **PA33-PLAN-CORRECTION-01 — accepted** | Full clean reached 644 Surefire with one existing oracle failure in `CustomerEvidenceBoundaryTests.namedOwnerInterfacesAndTenCustomerDependenciesAreExact`; its subject_number/evidence dependency assertions were stale relative to approved named Operations read ports. Controller accepted the 37th path and two exact assertion updates, not new behavior or weakened subset checks. | **CustomerEvidenceBoundaryTests 4/4 + ModuleBoundaryTests 9/9 + OperationsSubjectReadBoundaryTests 8/8 = 21/21**, zero failures/errors/skips, BUILD SUCCESS at **2026-11-14T19:18:37+08:00**. Evidence `/tmp/pa33-correction1-boundary-tests.log` and `docs/gateflow/pa33-operations-subject-read-foundation-plan-fix-2.md`. Subsequent Controller full clean **1120/1120** is green. |

Correction retains all four CustomerEvidence identities, all nine ModuleBoundary identities, exact target/named-interface equality and the approved dependency graph. It adds no endpoint/type/test identity/schema/grant. Live manifest is 37; historical 36-path statements are historical. Both implementation findings are closed by regressions, not deferred to production; accepted capacity/deployment limits below are separate residuals.

## 7. Commands, environment and evidence destinations

Measured toolchain: native **Temurin 21.0.13+9**, **Maven 3.9.9**, **Node 22.23.4**, Docker Server **29.1.3**; disposable database reports **PostgreSQL 16.15**. Source wrapper CRLF fails in WSL; use native `/opt/maven-3.9.9/bin/mvn` without modifying `backend/mvnw`. Explicit `/root/.m2/repository` avoids the earlier accidental worktree cache. Toolchain/agent evidence: focused root `toolchain.log`, `wrapper-raw.log`, `resolved-agent.log`; current full warnings E/`full.stderr`.

The following reproduces the validated command families; it is not a claim that the reporting author reran Maven, npm or validators. Controller executed full clean and three extracted checked-in CI run bodies: the first two verbatim, the dependency firewall with only the Maven wrapper invocation substituted as documented below. The firewall's checked-in Python and comparison logic ran unchanged; focused and correction commands/results belong to their earlier archived runs.

```bash
export GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call17
export GIT_COMMON_DIR=/mnt/d/codex-call/.git
export JAVA_HOME=/opt/jdk21
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
git rev-parse HEAD
git status --short --untracked-files=all
cd backend
mvn -Dmaven.repo.local=/root/.m2/repository -B -ntp -Pcontainer-smoke \
  -DargLine=-javaagent:/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar \
  -Dtest=OperationsSubjectReadWebTests,OperationsSubjectReadRequestTests,OperationsSubjectCursorCodecTests,OperationsSubjectReadServiceTests,OperationsSubjectReadConfigurationTests,OperationsSubjectReadBoundaryTests,EmployeeSessionBoundaryTests,OperationsSubjectsWebTests,OperationsLifecycleConfigurationTests,OperationsLifecycleBoundaryTests,ModuleBoundaryTests,OperationsSubjectLifecycleServiceTests \
  -Dit.test=OperationsSubjectReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT,OperationsSubjectLifecycleIT,OperationsLifecycleRoleLeastPrivilegeIT verify
mvn -Dmaven.repo.local=/root/.m2/repository -B -ntp -Pcontainer-smoke clean verify
mvn -Dmaven.repo.local=/root/.m2/repository -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.15/byte-buddy-agent-1.18.15.jar \
  -Dtest=CustomerEvidenceBoundaryTests,ModuleBoundaryTests,OperationsSubjectReadBoundaryTests test
```

After full clean, Controller extracted and executed the existing `.github/workflows/ci.yml` named run bodies from `backend` (plan §11.1 records the extraction). `assert container smoke actually ran` and `default and opt-in web jar smokes` ran **verbatim**, with results in E/`ci-step-1.log` and E/`ci-step-2.log`. For `production jar dependency firewall`, the checked-in Python and comparison logic ran unchanged, but its single `./mvnw dependency:list` invocation was executed as `mvn -Dmaven.repo.local=/root/.m2/repository dependency:list` because the local checked-out wrapper has CRLF and fails as a shell script in WSL. The substitution is explicitly logged as the same kind of raw-CRLF/LF-equivalent limitation recorded for A2–A5 (§5), not a verbatim raw-wrapper run; it does not alter firewall assertions. Firewall results are E/`ci-step-3.log`; wrapper failure evidence is the focused root's `wrapper-raw.log`. No simplified aggregate check substitutes for the source/CI/POM/XML oracle.

Raw contract command family, from repository root (LF-equivalent runs substitute only explicitly temporary mirror inputs/link context, never frozen source bytes):

```bash
(cd api/openapi && npm ci && npm run check:s3)
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
for stage in s1 s2; do
  node acceptance/validate.mjs --stage "$stage" --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures $([ "$stage" != s2 ] || printf '%s' --check-doc-links)
done
sha256sum docs/12-technology-stack-and-system-architecture.md docs/14-production-data-model-and-flyway-schema-spec.md docs/15-production-openapi-surface-spec.md docs/17-production-security-design-spec.md api/openapi/onecall-v1.openapi.yaml backend/src/main/resources/db/migration/V*.sql
git diff --check
```

For this report, only targeted reads of final results/build time, CI/contract/static logs and manifests were needed; neither the 1 MB full log nor the full cumulative plan was dumped. Read-only manifest verification uses SHA256 of each named file plus XML suite/testcase parsing; report-only before/after scope is checked against `/tmp/pa33-final-report-before.json`. Report measurements are made after the final write and handed off outside the report so it does not contain an impossible self-hash.

## 8. Residual owners and production-readiness limits

| Residual | Owner and destination / disposition |
| --- | --- |
| Detail over 100 bindings/claims or activation scan beyond 100 batches/10,100 candidates | **Product/Backend/Database**, accepted bounded-capacity design: 503/rollback/no partial success. Arbitrary-cardinality success needs newly authorized work; no silent truncation or PA33 V018/index/grant. |
| No stable-sort covering index, count volume and deployed latency/capacity unproven | **Database/Platform/Backend**, later synthetic/deployed query-plan and SLO evidence. PG16 functional tests are not production-scale benchmarking; schema/index change requires authorization. |
| Real six-role login, credential custody and indexed cursor key distribution/replica rotation | **Platform/Database/Security**, Q17/Q23 and deployment-readiness work. Synthetic six-membership proof does not establish a real deployment login/key ring; absence fails closed. Do not widen existing PA25 roles. |
| Employee issuer/session store/revocation/MFA/Q1 and production verifier adapter | **Identity/Security/Platform**, external authentication/deployment integration. Test synthetic verifiers are not packaged production identity infrastructure. |
| Concurrent disable after snapshot and no cross-page export snapshot | **Backend/Security**, accepted read-snapshot semantics; next request rechecks authority/facts. No row-lock serialization or multi-page frozen export promise. |
| Trial precedence, output minimization/mask fallback | **Entitlement/Security/Controller**, fresh code-review confirmation of existing-fact policy and output allowlist, not live-policy/provider extension. |
| CRLF-sensitive raw document parsing, temporary link context and firewall wrapper substitution | **Controller/tooling owners**, retain raw failures plus LF-equivalent evidence and the documented single firewall Maven invocation substitution; firewall Python/comparison logic and assertions are unchanged. Clean hosted Linux contract/CI validation remains distinct later evidence. No normalization of frozen files or wrapper. |
| Report acceptance, runner attestation, accepted commit and independent A/B code review | **Controller**, next gate after author handoff. Full local validation does not itself constitute independent review, hosted CI, PR acceptance or production readiness. |

No open source-design/schema/grant/public-contract blocker is asserted for this approved S1. Original JDK/Docker/XML execution blockers have been resolved for local validation, not carried forward as false residual blockers. No real production IdP/login/secret deployment, production load/SLO, cloud/media end-to-end, hosted final-head CI or production acceptance is established by this report. Existing cloud/security/legal/retention and broader product questions remain with their established later-work-unit owners; PA33 metadata reads do not close them.

No code/test/POM/CI/plan/fix/Control/review/migration/OpenAPI/private-helper write is performed by this reporting task. No staging, commit, push, branch/PR creation or change, merge, approval, ready marking, reviewer request, external comment, issue mutation or review initiation is performed.

## Appendix A. Exact 106 new method identities from Controller full XML

Each heading is the exact test-class identity; every listed method passed with zero failures/errors/skips in the frozen Controller XML. Together these are the source=CI method maps reconciled by `ci-step-1.log`, not synthetic names or inferred counts.

### `com.onecall.archtests.OperationsSubjectReadWebTests` — surefire, 16

```text
allEnumsAndLimitDefaultsAndBoundsAreExact
concurrentReadCallersAndFiltersRemainIsolated
cursorInvalidAndFilterMismatchUseFrozen400Problems
customerAndOperationsFiltersPassOnlyExactAdmittedRoutes
employeeAuthenticationPrecedesEveryReadInput
exactDetailRouteReturnsOnlyFrozenSafeProjection
exactListRouteReturnsOnlyFrozenPageAndSummaryProperties
getBodiesAndCommandHeadersNeverBecomeReadAuthority
hiddenMethodsAndNoncanonicalOperationsRoutesRemainNotFound
jsonMediaAndProblemHeadersNeverExposeSensitiveCanaries
missingTwoAndUncreatableReadServicesFailClosed
notFoundFollowsAuthorityAndValidation
onlyCurrentActiveAdminAndOpsMayRead
optionalFieldsAreOmittedButTailCursorIsExplicitNull
queryDuplicatesUnknownKeysAndCanonicalUuidUse422
zeroTwoAndUncreatableVerifiersAreSanitizedUnavailable
```

### `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadRequestTests` — surefire, 10

```text
canonicalLowercaseUuidRulesApplyToPathAndFilter
detailQueriesAndIncompatibleOrMalformedAcceptAreRejected
duplicatesIncludingIdenticalCursorValuesAreRejectedFirst
emptyAndUnknownNoncursorInputsAreValidationFailures
enumValuesAreExactWithoutTrimmingOrCaseFolding
filterDigestHasExactOrderedAbsentSentinelEncoding
limitDefaultRangeAndDecimalGrammarAreExact
listAllowsExactlyEightQueryKeys
parameterOrderAndPageSizeNeverChangeNormalizedFilterDigest
singleMalformedCursorIsDeferredUntilAfterNoncursorValidation
```

### `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodecTests` — surefire, 12

```text
activeKeyIssuesAndRetainedKeysVerifyAcrossRotation
configurationFailurePrecedesMalformedToken
encryptedRoundTripPreservesExactTimestampUuidAndDigest
invalidNanosAndPlaintextShapesNeverReachQueries
keysRequestsAnchorsAndFailuresHaveRedactedRepresentations
missingMalformedAndAmbiguousKeyMaterialFailsClosed
onlyAuthenticatedDifferentDigestIsFilterMismatch
retirementNeverFallsBackOrReissuesAReplacement
sameAnchorProducesRandomizedOpaqueBoundedTokens
strictBase64VersionLengthsAndTrailingBytesAreInvalid
tamperedNonceHeaderPayloadAndTagAreInvalid
wrongPurposeKeyAndRetiredKidAreInvalid
```

### `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests` — surefire, 16

```text
activationFilterBatchesCandidatesWithoutSkippingMatchingRows
allSuccessDenialAndFailurePathsHaveZeroWriteAuditAndOutboxCalls
candidateBudgetExhaustionNeverInventsTailOrContinuation
countsRemainLongExactAndIndependentOfHistoryJoins
detailHistoriesAreCompleteWithinCapAndOverflowFailsClosed
displayBindingAndOptionalProjectionOrderingAreDeterministic
invalidOwnerFactsAndRoleRestorationFailuresAreUnavailable
latestClaimSelectionPrecedesClaimStatusFiltering
listUsesDescendingSeekAndLimitPlusOneWithoutOffset
missingActivationAndAnyBindingRiskProjectExactly
oneReadOnlyRepeatableNewTransactionOwnsEveryProjection
ownerHandoffsAssertAndRestoreLiteralRolesOnOneResource
sqlBeanTransactionAndUnknownFailuresMapWithoutLeakingCauses
trialUsesOriginalGrantBalanceAndDatabaseDecisionTime
trustedCallerAndCurrentEmployeePrecedeInputAndSubjectReads
zeroTwoAndUncreatableOwnerPortsAreUnavailable
```

### `com.onecall.archtests.OperationsSubjectReadConfigurationTests` — surefire, 8

```text
defaultWebAndDisabledProfilesCreateNoReadPool
detailDoesNotRequireKeysOrAnyCommandResource
everyReadBeanUsesExactQualifiedDatasourceDslAndContext
missingCredentialsAndCursorKeysFailAtAffectedRequestNotBoot
ownerAdaptersAreConditionalAndImplementOnlyInboundReadPorts
persistenceEnabledGraphIsLazyAndUsesDedicatedCredentials
startupPropertiesProvidersAndPoolFailuresAreRedacted
templateIsReadOnlyRepeatableRequiresNewWithBoundedTimeouts
```

### `com.onecall.archtests.architecture.OperationsSubjectReadBoundaryTests` — surefire, 8

```text
allowlistedRoutesProjectionTypesAndDependencyGraphAreExact
controllerAndApplicationContainNoForeignBaseTableSql
employeeVerifierIsReusedWithoutPackagedProductionImplementation
fiveOwnersImplementInboundPortsWithoutReverseModuleCycles
operationsPublishesOnlyExactReadContractsAndInboundPorts
ownerSqlSelectsOnlyApprovedSafeColumnsAndCounts
readPathHasNoLocksWritesAuditOutboxOrCommandCalls
v001ThroughV017AndFrozenOpenApiAndDesignDocsRemainUnchanged
```

### `com.onecall.archtests.container.OperationsSubjectReadsIT` — failsafe, 24

```text
activationFilteredCandidateBatchesRemainCompleteWithinBudget
activeAdminAndOpsReadWithIndependentEmployeeSessions
allBindingRiskStatesAndAbsentActivationHaveExactProjection
authoritySubjectActivationTrialAndCountsShareOneSnapshot
candidateBudgetAndDatabaseTimeoutsNeverReturnPartialSuccess
committedEmployeeAndDomainChangesAreVisibleOnNextRequest
detailHistoryZeroOneHundredAndOverflowAreBounded
detailReturnsOnlySafeFieldsAndOmissionRules
disabledMissingAndWrongAudienceEmployeesFailBeforeDisclosure
displayBindingAndClaimOrderingAreDeterministic
evidenceAndOrderCountsIncludeAllSubjectStatusesAndPurchasers
latestClaimFilterDoesNotMatchOlderHistoricalStatus
limitPlusOneEmptyAndTailPagesUseLastReturnedAnchor
listDefaultsAndEveryStructuredFilterAreExact
missingOrInconsistentTrialRelationsFailWithoutRepair
normalizedFiltersBindCursorButPageSizeDoesNot
postgres16AppliesExactlyFrozenSeventeenMigrations
readEndpointsAndExistingCommandCustomerPublicRoutesStayIsolated
responsesProblemsAndCapturedLogsNeverContainSensitiveCanaries
stableTimestampUuidTiesSeekWithoutDuplicatesOrOffset
strictInputsAndDuplicatesHaveFrozenErrorPrecedence
successDenialNotFoundAndFaultsPerformZeroBusinessWrites
tamperedWrongKeyAndRotatedCursorsHaveExactProblems
trialAbsentOriginalExpiredExhaustedAndReservedRulesAreExact
```

### `com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT` — failsafe, 12

```text
allOwnerReadsUseSameBackendPidTransactionAndSnapshot
eachMissingMembershipFailsClosedWithoutPrivilegeRepair
eachOwnerReadsOnlyItsOwnApprovedProjection
failedRestorationRollsBackAndReusesSameCleanPhysicalConnection
frozenAclOracleMigrationsRolesAndFunctionsRemainUnchanged
noResidualLocksTransactionsAuditOrOutboxFactsRemain
normalAndExceptionalHandoffsRestoreOperationsRole
operationsRoleCannotReadForeignBaseTablesWith42501
poolReadOnlyIsolationAutocommitAndLocalSettingsAreRestored
readOnlyDatabaseRejectsWritesAndLockingQueries
syntheticLoginHasExactlySixNonInheritedSetMemberships
wrongResourceEntryRoleAndMissingTransactionAreRejected
```

## Appendix B. Exact 22 frozen raw-byte SHA256 identities

These values are copied from Controller E/`frozen-after.txt` and independently rehashed during report preparation; all match the current frozen files. They are raw checkout hashes, not LF-mirror hashes.

```text
9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db  docs/12-technology-stack-and-system-architecture.md
0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31  docs/14-production-data-model-and-flyway-schema-spec.md
db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0  docs/15-production-openapi-surface-spec.md
7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e  docs/17-production-security-design-spec.md
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
c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc  backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql
a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c  backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql
74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3  backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql
```

## 9. Final artifact and stop boundary

Final measured artifact path: `/mnt/c/Users/Administrator/.codex/worktrees/pa33-next-work-unit/codex-call/docs/gateflow/pa33-operations-subject-read-foundation-implementation.md`.

Its final byte size/SHA256 are measured externally after the final write and supplied in the handoff response. This report delivers implementation-author completion only; Controller acceptance and independent review remain separate.

implementation author complete, stop for Controller validation and independent review.
