# PA35 S1 implementation evidence

## Status and custody

### CR fix 1 continuation, 2026-11-17

Author fix task `pa35-s1-code-review-fix1-gpt-20261117-01` is completed by
continuation `pa35-s1-code-review-fix1-cont1-gpt-20261117-01`, not a new review.
Dispatch attribution remains Codex runner / gpt / GPT-6.1-SOL / high (supplied
metadata, not independent model introspection). CR-01, CR-02, CR-03 and CR-04
are closed with the existing strengthened test candidate; this continuation
does not modify production code or add test identities. Detailed closure,
hashes, commands, custody, residual owners and completion are recorded in
`docs/gateflow/pa35-operations-ivr-session-read-foundation-s1-code-review-fix-1.md`.

The intake comparison against `/tmp/pa35-cr-fix1/baseline.json` found exactly
three changed tests: OperationsIvrSessionReadBoundaryTests,
OperationsIvrSessionReadsIT and OperationsIvrSessionReadWebTests. This report
was still byte-identical to intake despite its earlier mtime; it is now
updated, and the fix artifact is the only added candidate path. Final scope
is the accepted 30 paths plus that artifact, with no staged changes.

Fix validation (distinct from the original implementation attempts below):

| Command / evidence | Actual result |
|---|---|
| Focused attempt 1, `/tmp/pa35-cr-fix1/focused-attempt1.log` | Failed test compilation: authority-projection regex escaping; corrected before the passing runs |
| Focused attempt 2, `/tmp/pa35-cr-fix1/focused-attempt2.log` | Failed launcher cwd (repository root without POM); corrected to backend cwd |
| `bash /tmp/pa35-cr-fix1/run-focused3.sh`, backend cwd, JDK21/Maven3.9.9 | Preserved attempt3 `.exit` is 0; 9/109 Surefire + 3/48 Failsafe = 157 exact identities, zero failures/errors/skips |
| `mvn clean -Pcontainer-smoke verify`, backend cwd, same native toolchain | BUILD SUCCESS at 2026-11-17 12:42:55 +08:00; 83/788 Surefire + 34/556 Failsafe, zero failures/errors/skips; no `full.exit` was written before interruption and none is fabricated |
| Prior `/tmp/pa35-cr-fix1/finish-validation.log` | Failed evidence-helper report lookup (0 Surefire XML), not a Maven failure; superseded by native current XML parsing |
| Prior `/tmp/pa35-cr-fix1/static.py` | Invalid substring annotation count 1373 included `@TestConfiguration`; no tests were changed to satisfy it |
| `python3 /tmp/pa35-cr-fix1-cont1/verify.py`, repository root | PASS: focused preserved XML and current full XML copied/hashed, exact identities compared, and unchanged CI inventory body executed from backend cwd |
| `python3 /tmp/pa35-cr-fix1-cont1/static.py`, repository root | PASS: exact candidate scope, intake delta, frozen raw/LF pairs, binary pair map, 291-file production aggregate, source identities, word-boundary annotation count and whitespace |

Fresh continuation evidence is `/tmp/pa35-cr-fix1-cont1/`. Full XML was present,
not overwritten by focused validation: all 117 suites / 1344 identities were
parsed and preserved there. The exact extracted CI shell body is
`ci-exact-inventory.sh`, with `.log` and `.exit` (0); source=CI=POM=XML and all
historical arithmetic pass, including 1344 methods, 1345 `@Test\b` line hits,
83 unit classes, 34 container classes and 34 POM includes. Source/test/POM/CI
hashes are unchanged throughout continuation verification; the three test
mtimes precede the full build start at approximately 12:06:18 +08:00.

Original A1-A5, production-JAR firewall and default/opt-in JAR smoke evidence
under `/tmp/pa35-evidence/` is reused, not rerun or represented as fresh fix
execution. Their inputs are unaffected by this test/report-only fix and all
recorded exit markers are 0. The 26 frozen pins, V001-V018/no V019, binary
pair-map `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`
and 291-file production aggregate
`95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0`
are freshly verified. Existing residual ownership remains unchanged. This
is author fix completion only, not approval, readiness or independent review.

The original implementation account below is historical; its statement of
30 paths predates the expressly authorized fix artifact. No Control, plan,
review, production, POM, CI, migration or OpenAPI file is edited by this fix.
No commit, push, PR, approval, ready action, reviewer request, external comment,
re-review or later gate is performed.

Candidate implementation for **PA35-operations-ivr-session-read-foundation**, task
`pa35-implementation-s1-gpt-20261117-01`. Dispatch metadata supplied with the task:
**Codex runner / gpt / GPT-6.1-SOL / high**; this is dispatch attribution, not
independent model introspection. Task date: 2026-11-17. Host Maven logs use
Asia/Shanghai (+08:00); timestamps in those logs are not America/New_York dates.

**Verification status: focused final PASS; full clean verification PASS; exact CI bodies PASS.**
This report is implementation evidence, not approval, acceptance, production
readiness, or a later-gate action. No required S1 behavior is deferred.

Workspace/cwd root:
`/mnt/c/Users/Administrator/.codex/worktrees/pa35-evidence-role-ivr-detail-read/codex-call`.
Accepted-plan commit: `5aed1ca1f50ab1216b4167baeb1fc3ee6c8d7c3b`.
Intake HEAD / Control checkpoint: `4730214bb7af05490debffe5d69b3ce44902c7bc`.
The `.git` pointer contains the Windows path
`D:/codex-call/.git/worktrees/codex-call23`, which native WSL Git cannot resolve.
Read-only Git inspection uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call23`
and `GIT_COMMON_DIR=/mnt/d/codex-call/.git`; `.git` is unchanged.

Read authority, with SHA-256 measured from current raw bytes:

| Input | SHA-256 |
|---|---|
| cumulative accepted plan | `58ec889fe812cd1e0c439a7d3c1054aa41bfe8bd44bcd27183512fbe3ffd85d3` |
| fix8 | `d4ac74d671cb14082f821983ec0269220763855d207ed5fdfa091cfc0ceb03d8` |
| final A review, plan-review-20261117-095837.md | `349fd19af06790296b6fd6af801859cc1d1f974a3b2f4b2bafb9d596c1a312d5` |
| final B review, plan-review-20261117-100340.md | `8d966f93b95b5eb51fca3822ceff2cd926e2eeaf60f173d3389fe96334836efd` |

Control's Active PA35 checkpoint, cumulative plan, fix8, and both final reviews
were inspected before editing. Their goal confirmation is unchanged.

## Implemented behavior

- Exactly `GET /api/v1/operations/ivr-sessions` and
  `GET /api/v1/operations/ivr-sessions/{sessionId}`, implementing the frozen
  `listOperationsIvrSessions` / `getOperationsIvrSession` operations.
- Independent opaque employee bearer, audience `onecall:employee-session:v1`;
  one persisted authority lookup in Operations role. Validate ID/status/role/
  revision before DISABLED: only current ACTIVE ADMIN/OPS proceeds. Missing
  authority is 401, valid disabled authority 403, corrupt trusted/persisted facts
  503, all before request parsing or owner-provider resolution.
- Only cursor/limit/subjectId/callId/taskId/status, at most once. Default20,
  ASCII limit grammar and range1–100, lowercase canonical UUIDs, exact Unicode
  call/task code-point bounds, exact four status values; detail has no query.
- Literal twelve-column inbound-session summaries. Task presence alone emits
  the owner-local call_record/outbound_call_task EXISTS association; public task
  text is never compared to the internal task UUID. No task means no call/task
  table in the list SQL. All supplied root filters intersect.
- Descending created_at/id keyset, PostgreSQL-native unsigned UUID ordering,
  limit+1 with validation of lookahead, exclusive continuation, and cursor only
  from the last returned item. No offset, total, storage, or generic cursor.
- Shared-ring endpoint-specific AES-256-GCM cursor: random12 nonce,128-bit tag,
  version/key header,60-byte encrypted anchor+digest,91–122-byte envelope,
  canonical unpadded base64url122–163 characters. AAD length-prefixes purpose,
  GET endpoint, sort, header. Presence-aware filter digest uses the frozen
  prefix/field order/presence/length grammar; limit/query order do not bind it.
  Authentication/payload validation precedes constant-time digest comparison;
  invalid400 and authenticated filter mismatch400 remain distinct.
- Safe complete detail root/call/decisions/legs/events/evidence, deterministic
  orders, nullable decision.action and page.nextCursor retained, SQL-NULL
  optionals omitted. ACTIVE call with NULL end/start/seconds is valid; terminal
  and decision purpose/action/reservation/time shapes are validated. Missing
  root404; missing required call/conflicting links/corruption503, never partial.
- Evidence selects only record(id,status,session_id,call_id) and artifact
  (evidence_id,artifact_role). Sorted IDs use fixed1000-ID grouped IN chunks in
  one Evidence scope/snapshot; zero IDs means zero artifact queries; roles are
  deduplicated ORIGINAL then DERIVED, including empty arrays. No hidden cap or
  N+1, unexpected chunk IDs or invalid enums fail the entire response.
- Existing qualified Operations datasource/DSL/template/context/properties,
  existing pool maximum4/connectionTimeout3000ms, REQUIRES_NEW/REPEATABLE_READ/
  readOnly timeout5s, statement2000ms/lock250ms/idle5000ms. No new pool, manager,
  ring, role, grant, migration, capability, or production verifier.
- Owner-local IVR and Evidence adapters implement distinct typed ports;
  ObjectProvider stream-at-most-two requires exactly one even with @Primary.
  Component names are diagnostic, not runtime cross-module qualifiers. IVR adds
  only the existing Operations named read-interface edge:13 discovered edges,
  six Operations targets, no reverse compile edge/cycle/new named interface.
- IVR owner encloses its complete read, restores/asserts Operations before
  Evidence entry; context asserts/restores original login and resource/backend/
  snapshot state, rolls back on cleanup fault, and reused guarded pool evicts
  contamination. No writes, audit/Outbox, retained locks, media or playback.
- Both admission filters add exactly the two GET templates with URI equality.
  Existing routes and ERROR branch remain unchanged. MockMvc REQUEST TRACE404;
  actual default-error-path Tomcat TRACE500 INTERNAL for both new templates.
  New fixture uses the default ERROR path; it does not change the existing
  PA34 custom-error-path fixture or production ERROR registration/controller.
- Shared Failure/Code passes unchanged at service and controller, including
  NOT_FOUND, CURSOR_FILTER_MISMATCH, RATE_LIMITED. Actual dependency/transaction/
  timeout/context faults503, unknown runtime500. Complete-buffer serialization,
  closed safe problems, trace/no-store/challenge, no sensitive canaries in
  responses or captured application logs.

## Exact write set: 30 = 20 new + 10 existing

Production scope is13 =9 new +4 existing. The remaining17 paths are verification,
fixtures, inventory wiring and this report. No other tracked path is authorized.

| Row | State | Exact path | Literal responsibility/change |
|---|---|---|---|
| 01 | existing | `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java` | Admit exactly two GET templates; preserve URI equality and concealment. |
| 02 | existing | `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` | Add exact two GET bypasses in lockstep; no broad Operations bypass. |
| 03 | existing | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsReadContext.java` | Only add nested enum IVR("ivr_role"); retain EVIDENCE("evidence_role") and all context logic. |
| 04 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsIvrSessionReadController.java` | Two mappings, raw request capture, exact DTO serialization and sanitized problems. |
| 05 | new | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsIvrSessionReadService.java` | Only list/detail; immutable raw requests and exact frozen DTO records. |
| 06 | new | `backend/src/main/java/com/onecall/core/operations_iam/subjectread/OperationsIvrSessionReadPorts.java` | Distinct narrow IvrSessionReader/IvrEvidenceReader types and immutable facts/filters/anchor; no bean-name contract or generic query port. |
| 07 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadRequest.java` | Strict parsing and presence-aware filter digest. |
| 08 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionCursorCodec.java` | One endpoint-specific AES-GCM envelope using shared ring. |
| 09 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/DefaultOperationsIvrSessionReadService.java` | Single authority check, transaction, mapping/consistency and failure classification; type-based ObjectProvider ports with existing one(ObjectProvider) fail-closed semantics. |
| 10 | new | `backend/src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java` | Compose service over existing qualified infrastructure and type-based owner ports only; no owner-name qualifier or datasource/key bean. |
| 11 | new | `backend/src/main/java/com/onecall/core/ivr/internal/operationsread/JooqOperationsIvrSessionReader.java` | IVR-owned safe SQL/EXISTS task filter and detail chain inside Role.IVR; distinct typed IvrSessionReader port, component name diagnostic only; no cross-owner join. |
| 12 | new | `backend/src/main/java/com/onecall/core/evidence/internal/operationsread/JooqOperationsIvrEvidenceReader.java` | Evidence-owned safe SQL inside Role.EVIDENCE; exact call/session correlation, no foreign join or media read. |
| 13 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadRequestTests.java` | Exact ordinary test identities A below. |
| 14 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionCursorCodecTests.java` | Exact ordinary test identities B below. |
| 15 | new | `backend/src/test/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadServiceTests.java` | Exact ordinary test identities C below. |
| 16 | new | `backend/src/test/java/com/onecall/archtests/OperationsIvrSessionReadWebTests.java` | Exact ordinary test identities D below. |
| 17 | new | `backend/src/test/java/com/onecall/archtests/OperationsIvrSessionReadConfigurationTests.java` | Exact ordinary test identities E below. |
| 18 | new | `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` | Exact ordinary test identities F below. |
| 19 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` | Exact ordinary test identities G below; PostgreSQL16. |
| 20 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadRoleLeastPrivilegeIT.java` | Exact ordinary test identities H below; PostgreSQL16. |
| 21 | new | `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadFixture.java` | Disposable login, frozen synthetic facts and catalog/data fingerprints; zero tests. |
| 22 | new | `backend/src/test/java/com/onecall/archtests/operations/OperationsIvrSessionReadTestProviders.java` | Synthetic properties/facts/providers only; zero tests; no production verifier. |
| 23 | existing | `.github/workflows/ci.yml` | Exact named PA35 additions, source/history/token/POM/XML reconciliation; retain firewall/A1-A5. |
| 24 | existing | `backend/pom.xml` | Only two Failsafe includes, no dependency/plugin/profile change. |
| 25 | new | `docs/gateflow/pa35-operations-ivr-session-read-foundation-implementation.md` | Actual authorized scope, source/CI/POM/XML, commands/hashes, residuals; not a plan-gate runtime claim. |
| 26 | existing | `backend/src/main/java/com/onecall/core/ivr/package-info.java` | Retain four dependencies; add exactly operations_iam::operations-subject-read. |
| 27 | existing | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | Add exact IVR allowed edge and change discovered target multiset12→13/five→six Operations targets; no identity delta. |
| 28 | existing | `backend/src/test/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadServiceTests.java` | Include IVR after ORDER in existing exact nested Role.values assertion; no identity delta. |
| 29 | existing | `backend/src/test/java/com/onecall/archtests/container/OperationsSubjectReadRoleLeastPrivilegeIT.java` | Replace generic Role.values loop with PA33 fixture's exact five-role list, excluding IVR; no identity delta. |
| 30 | existing | `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | In ciPomAndOrdinaryMethodInventoryMatchExactly reconcile every current CI total location enumerated below; in v001ThroughV017AndPublicContractsRemainByteFrozen change only current manifest count282→291 and current aggregate pin after authorized production bytes exist. Preserve full-tree guard, historical V001–V018/public-contract/ACL/topology/method assertions. |

Existing-file changes are restricted to: exact GET admission in both filters;
append IVR after ORDER in Role; one IVR descriptor/map edge and exact13-edge
multiset; append IVR to the existing role-values assertion; restrict the PA33
fixture's existing loop to its configured original five roles; two Failsafe
includes; exact CI historical/current inventory wiring; row30 mirrors and the
measured full-tree hash/count. No existing test identity changes.

## Commands, environment, and exploratory failures

Validation evidence directory: `/tmp/pa35-evidence` (outside tracked source).
Java: `/opt/jdk21`, Eclipse Temurin21.0.13. Maven:3.9.9, matching the pinned
wrapper distribution. Node:`/opt/node-v22.23.4-linux-x64/bin/node`22.23.4;
npm10.9.9. Docker client/server29.1.3 is reachable. Tests actually run
PostgreSQL16.15 (`postgres:16`), all18 Flyway migrations, with no container skip.

Environment for native backend Maven commands:

```bash
cd WORKSPACE/backend
export JAVA_HOME=/opt/jdk21 MAVEN_SKIP_RC=true
export PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:$PATH
```

The literal checked-in `./mvnw -version` fails127 on WSL with
`/bin/sh^M: bad interpreter: No such file or directory`: its frozen existing
checkout representation is CRLF. Unqualified host `mvn -version` also fails1
with ClassNotFoundException for Plexus because of the host launcher/RC setup.
No wrapper/RC/private config is modified. Setting JAVA_HOME and MAVEN_SKIP_RC
and using `/opt/maven-3.9.9/bin/mvn` executes the exact same Maven version and
lifecycle/options instead. These are tooling limitations, not Docker blockers.

Final focused command, native backend cwd:

```bash
mvn -Pcontainer-smoke -Dtest=OperationsIvrSessionReadRequestTests,OperationsIvrSessionCursorCodecTests,OperationsIvrSessionReadServiceTests,OperationsIvrSessionReadWebTests,OperationsIvrSessionReadConfigurationTests,OperationsIvrSessionReadBoundaryTests,OperationsSubjectReadServiceTests,ModuleBoundaryTests,EvidenceRoleIvrAssociationAclBoundaryTests -Dit.test=OperationsIvrSessionReadsIT,OperationsIvrSessionReadRoleLeastPrivilegeIT,OperationsSubjectReadRoleLeastPrivilegeIT -DfailIfNoTests=true -Dfailsafe.failIfNoSpecifiedTests=true -DskipTests=false -DskipITs=false verify
```

Actual exit0;109 Surefire +48 Failsafe =157 methods across9+3=12 classes;
zero failures/errors/skips. Final log:`/tmp/pa35-evidence/focused-final.log`,
time2:27. XML is preserved outside source before clean under
`/tmp/pa35-evidence/focused-final/{surefire-reports,failsafe-reports}`.

Full command, native backend cwd, after focused XML preservation:

```bash
mvn clean -Pcontainer-smoke verify
```

Actual exit0, BUILD SUCCESS, on reachable Docker/PostgreSQL16.15.
[INFO] Total time:  39:06 min; [INFO] Finished at: 2026-11-17T11:19:52+08:00. Measured XML:83 Surefire classes/788 methods and
34 Failsafe classes/556 methods,117/1344 total; every suite reports zero
failures/errors/skips. Log:`/tmp/pa35-evidence/full.log`; exit file:`full.exit`.
All18 frozen migrations are exercised, including the new PA35 fixtures.
No disabled-container substitute or inferred runtime result is used.

Earlier bounded validation attempts are not represented as passing evidence:

| Log / command family | Exit / issue | Resolution within allowlist |
|---|---|---|
| `/tmp/pa35-compile.log`, root cwd, Maven `-f backend/pom.xml -DskipTests compile` |1; wrong generated import and ambiguous request imports | Explicit shared Failure/Code imports and correct typed-port import |
| `/tmp/pa35-units.log`, root cwd, six new Surefire classes |1; controller request imports ambiguous | Explicit Failure/Code imports |
| `/tmp/pa35-units2.log`, same six-class command |1; generated test quote syntax | Correct Java string literals |
| `/tmp/pa35-units3.log`, backend cwd, same six classes |1; Modulith API and JDBC fixture helper arity | Existing getDirectDependencies API; prepared JDBC test helper |
| `/tmp/pa35-units4.log`, same six classes |1;76 tests,1 failure/3 errors from Mockito restubbing/verification | doThrow for throwing stubs; exactly-two authority reads over two requests |
| `focused-attempt1.log` |1; fixture selected nonexistent call_record.prompt_version; custom error-path TRACE expectation | Fixture inserts actual V005 columns; default error-path fixture; production ERROR branch unchanged |
| `focused-attempt2.log` |1; expanded Mockito varargs in batch test | getRawArguments for the actual UUID bind array |
| `focused-attempt3.log` |1;2 failures/1 error among48 ITs: confirmed fixture lacked trusted caller, replacement used old activation bean name causing provider ambiguity | Valid frozen confirmed fixture; replace correct IVR service bean |
| `focused-attempt4.log` |0;157 tests, no skips/errors/failures | Then strengthened existing G18/G19/G20/G23/G24 identities, without adding methods |
| `focused-final.log` |0;157 tests, no skips/errors/failures | Authoritative final focused run, including strengthened assertions |

All source edits use apply_patch. No failing assertion is removed/weakened,
no test is skipped, disabled, parameterized, repeated, or dynamically generated.
Fixture-only corrections do not widen production semantics.

## Source / CI / POM / XML reconciliation

Static measured final source:83 Surefire classes/788 methods +34 Failsafe
classes/556 methods =117/1344; annotation-line scan1345 (the historical
PostgresContainerIT comment token remains). Additions are exactly8 classes/112
ordinary no-argument methods:12/12/18/16/10/8 unit,24/12 container. Existing four
modified test classes add zero identities. Scoped Failsafe POM34; global POM36.
The unrelated `baseline_jars == 34` remains unchanged.

Final full XML source-identity equality PASS. Independently parsed every
TEST-*.xml, required83/34 exact files and788/556 exact testcase counts,
zero failures/errors/skips, no duplicate identities, and exact equality of
all117 class→method sets against the measured source inventory. The unchanged
CI assertion additionally proves source=CI=POM=XML and historical/token maps.

Full named method inventory:`/tmp/pa35-evidence/full-methods.txt`,1344 lines,
SHA-256 `8ed03a7a78c38bcb2b9e2fdc8f9fa1c6bdde41e3f1639634fc27243974c4bee6`.
Preserved XML:`/tmp/pa35-evidence/full-final/{surefire-reports,failsafe-reports}`;
XML path/hash manifest:`/tmp/pa35-evidence/full-xml.manifest`, SHA-256
`8bc4479c881640cfce58e1ecc6a57b40d969d7a3cef3fdd226bcacbb64f48d8c`.

Exact final source=CI=POM=XML class inventory (method identities additionally
checked by the workflow; all named focused identities follow):

| Exact class | Engine | Methods |
|---|---|---|
| `com.onecall.archtests.CustomerBusinessNumbersWebTests` | Surefire | 12 |
| `com.onecall.archtests.CustomerEvidenceConfigurationTests` | Surefire | 6 |
| `com.onecall.archtests.CustomerEvidenceWebTests` | Surefire | 12 |
| `com.onecall.archtests.CustomerMeWebTests` | Surefire | 8 |
| `com.onecall.archtests.CustomerSharesCommandWebTests` | Surefire | 4 |
| `com.onecall.archtests.CustomerSharesWebTests` | Surefire | 10 |
| `com.onecall.archtests.CustomerSubjectActivationConfigurationTests` | Surefire | 8 |
| `com.onecall.archtests.CustomerSubjectActivationsWebTests` | Surefire | 12 |
| `com.onecall.archtests.CustomerSubjectClaimsWebTests` | Surefire | 12 |
| `com.onecall.archtests.CustomerSubjectDetailConfigurationTests` | Surefire | 6 |
| `com.onecall.archtests.CustomerSubjectsWebTests` | Surefire | 12 |
| `com.onecall.archtests.CustomerWebConfigurationTests` | Surefire | 6 |
| `com.onecall.archtests.EvidenceMediaReadPersistenceConfigurationTests` | Surefire | 4 |
| `com.onecall.archtests.MediaGatewayConfigurationTests` | Surefire | 4 |
| `com.onecall.archtests.MediaGatewayWebTests` | Surefire | 18 |
| `com.onecall.archtests.MediaPlaybackConfigurationTests` | Surefire | 19 |
| `com.onecall.archtests.OneCallApplicationTests` | Surefire | 1 |
| `com.onecall.archtests.OperationsActivationReadConfigurationTests` | Surefire | 8 |
| `com.onecall.archtests.OperationsActivationReadWebTests` | Surefire | 12 |
| `com.onecall.archtests.OperationsIvrSessionReadConfigurationTests` | Surefire | 10 |
| `com.onecall.archtests.OperationsIvrSessionReadWebTests` | Surefire | 16 |
| `com.onecall.archtests.OperationsLifecycleConfigurationTests` | Surefire | 8 |
| `com.onecall.archtests.OperationsSubjectReadConfigurationTests` | Surefire | 8 |
| `com.onecall.archtests.OperationsSubjectReadWebTests` | Surefire | 16 |
| `com.onecall.archtests.OperationsSubjectsWebTests` | Surefire | 12 |
| `com.onecall.archtests.PublicSharePersistenceConfigurationTests` | Surefire | 2 |
| `com.onecall.archtests.PublicShareWebTests` | Surefire | 12 |
| `com.onecall.archtests.SharingCommandPersistenceConfigurationTests` | Surefire | 2 |
| `com.onecall.archtests.SharingPersistenceConfigurationTests` | Surefire | 2 |
| `com.onecall.archtests.SubjectClaimPersistenceConfigurationTests` | Surefire | 8 |
| `com.onecall.archtests.activation.CustomerSubjectActivationServiceTests` | Surefire | 18 |
| `com.onecall.archtests.activation.OperationsSubjectLifecycleServiceTests` | Surefire | 18 |
| `com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests` | Surefire | 1 |
| `com.onecall.archtests.architecture.CustomerApiBoundaryTests` | Surefire | 3 |
| `com.onecall.archtests.architecture.CustomerBusinessNumberBoundaryTests` | Surefire | 4 |
| `com.onecall.archtests.architecture.CustomerEvidenceBoundaryTests` | Surefire | 4 |
| `com.onecall.archtests.architecture.CustomerSubjectActivationBoundaryTests` | Surefire | 6 |
| `com.onecall.archtests.architecture.CustomerSubjectClaimBoundaryTests` | Surefire | 6 |
| `com.onecall.archtests.architecture.CustomerSubjectDetailBoundaryTests` | Surefire | 4 |
| `com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests` | Surefire | 8 |
| `com.onecall.archtests.architecture.ModuleBoundaryTests` | Surefire | 9 |
| `com.onecall.archtests.architecture.OperationsActivationReadBoundaryTests` | Surefire | 8 |
| `com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests` | Surefire | 8 |
| `com.onecall.archtests.architecture.OperationsLifecycleBoundaryTests` | Surefire | 8 |
| `com.onecall.archtests.architecture.OperationsSubjectReadBoundaryTests` | Surefire | 8 |
| `com.onecall.archtests.architecture.SharingQueryBoundaryTests` | Surefire | 8 |
| `com.onecall.archtests.container.ActivationCommandRoleLeastPrivilegeIT` | Failsafe | 10 |
| `com.onecall.archtests.container.CustomerBusinessNumberReadPrivilegeIT` | Failsafe | 8 |
| `com.onecall.archtests.container.CustomerBusinessNumbersIT` | Failsafe | 18 |
| `com.onecall.archtests.container.CustomerEvidenceIT` | Failsafe | 24 |
| `com.onecall.archtests.container.CustomerEvidenceReadPrivilegeIT` | Failsafe | 10 |
| `com.onecall.archtests.container.CustomerMeIT` | Failsafe | 10 |
| `com.onecall.archtests.container.CustomerShareCommandsIT` | Failsafe | 18 |
| `com.onecall.archtests.container.CustomerSharesIT` | Failsafe | 8 |
| `com.onecall.archtests.container.CustomerSubjectActivationsIT` | Failsafe | 24 |
| `com.onecall.archtests.container.CustomerSubjectClaimsIT` | Failsafe | 24 |
| `com.onecall.archtests.container.CustomerSubjectDetailReadPrivilegeIT` | Failsafe | 10 |
| `com.onecall.archtests.container.CustomerSubjectsIT` | Failsafe | 20 |
| `com.onecall.archtests.container.DatabaseRuntimeAclRbacHardeningIT` | Failsafe | 12 |
| `com.onecall.archtests.container.EvidenceMediaReadIT` | Failsafe | 8 |
| `com.onecall.archtests.container.EvidenceMediaReadRoleLeastPrivilegeIT` | Failsafe | 8 |
| `com.onecall.archtests.container.EvidenceRoleIvrAssociationAclIT` | Failsafe | 16 |
| `com.onecall.archtests.container.MediaGatewayTransportIT` | Failsafe | 8 |
| `com.onecall.archtests.container.MediaPlaybackIT` | Failsafe | 9 |
| `com.onecall.archtests.container.OperationsActivationReadRoleLeastPrivilegeIT` | Failsafe | 10 |
| `com.onecall.archtests.container.OperationsActivationReadsIT` | Failsafe | 18 |
| `com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT` | Failsafe | 12 |
| `com.onecall.archtests.container.OperationsIvrSessionReadsIT` | Failsafe | 24 |
| `com.onecall.archtests.container.OperationsLifecycleRoleLeastPrivilegeIT` | Failsafe | 12 |
| `com.onecall.archtests.container.OperationsSubjectLifecycleIT` | Failsafe | 24 |
| `com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT` | Failsafe | 12 |
| `com.onecall.archtests.container.OperationsSubjectReadsIT` | Failsafe | 24 |
| `com.onecall.archtests.container.PostgresContainerIT` | Failsafe | 64 |
| `com.onecall.archtests.container.ProductionDatabaseIdentityProvisioningIT` | Failsafe | 20 |
| `com.onecall.archtests.container.PublicShareAdmissionIT` | Failsafe | 42 |
| `com.onecall.archtests.container.PublicSharesIT` | Failsafe | 8 |
| `com.onecall.archtests.container.SharingAdmissionRoleLeastPrivilegeIT` | Failsafe | 9 |
| `com.onecall.archtests.container.SharingQueryServiceIT` | Failsafe | 10 |
| `com.onecall.archtests.container.SharingRoleLeastPrivilegeIT` | Failsafe | 12 |
| `com.onecall.archtests.container.SubjectClaimRoleLeastPrivilegeIT` | Failsafe | 10 |
| `com.onecall.archtests.entitlement.ActivationTrialServiceTests` | Surefire | 8 |
| `com.onecall.archtests.evidence.ControlledMediaByteSourceTests` | Surefire | 4 |
| `com.onecall.archtests.evidence.EvidenceMediaObjectReaderTests` | Surefire | 4 |
| `com.onecall.archtests.sharing.CustomerShareCommandServiceTests` | Surefire | 6 |
| `com.onecall.archtests.sharing.MediaPlaybackCredentialAuthorityTests` | Surefire | 24 |
| `com.onecall.archtests.sharing.MediaPlaybackServiceTests` | Surefire | 21 |
| `com.onecall.archtests.sharing.PublicShareQueryServiceTests` | Surefire | 4 |
| `com.onecall.archtests.sharing.ShareAdmissionServiceTests` | Surefire | 26 |
| `com.onecall.archtests.sharing.SharingPlaybackStateReaderTests` | Surefire | 8 |
| `com.onecall.archtests.sharing.SingleByteRangeTests` | Surefire | 6 |
| `com.onecall.archtests.subjectnumber.CustomerSubjectClaimServiceTests` | Surefire | 17 |
| `com.onecall.archtests.subjectnumber.SubjectClaimVerificationReferenceTests` | Surefire | 8 |
| `com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryServiceTests` | Surefire | 8 |
| `com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests` | Surefire | 6 |
| `com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryServiceTests` | Surefire | 12 |
| `com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryServiceTests` | Surefire | 12 |
| `com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests` | Surefire | 7 |
| `com.onecall.core.customer_iam.internal.web.CustomerBusinessNumberCursorCodecTests` | Surefire | 8 |
| `com.onecall.core.customer_iam.internal.web.CustomerEvidenceCursorCodecTests` | Surefire | 8 |
| `com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests` | Surefire | 6 |
| `com.onecall.core.operations_iam.internal.activationread.OperationsActivationCursorCodecTests` | Surefire | 10 |
| `com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadRequestTests` | Surefire | 8 |
| `com.onecall.core.operations_iam.internal.activationread.OperationsActivationReadServiceTests` | Surefire | 14 |
| `com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests` | Surefire | 12 |
| `com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests` | Surefire | 12 |
| `com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests` | Surefire | 18 |
| `com.onecall.core.operations_iam.internal.security.EmployeeSessionBoundaryTests` | Surefire | 8 |
| `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodecTests` | Surefire | 12 |
| `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadRequestTests` | Surefire | 10 |
| `com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests` | Surefire | 16 |
| `com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests` | Surefire | 8 |
| `com.onecall.core.sharing.internal.keysource.FileMediaPlaybackSigningKeyProviderTests` | Surefire | 10 |
| `com.onecall.core.sharing.internal.keysource.MediaPlaybackFileKeySourceIntegrationTests` | Surefire | 8 |
| `com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests` | Surefire | 12 |
| `com.onecall.core.sharing.internal.keysource.MediaPlaybackTencentKeySourceIntegrationTests` | Surefire | 10 |
| `com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProviderTests` | Surefire | 18 |
| `com.onecall.core.sharing.internal.keysource.TencentSsmSecretStringClientTests` | Surefire | 8 |

Final load-bearing workflow and row30 mirror literals are distinct:

```text
workflow: method_totals == [788, 556] and sum(method_totals) == 1344
workflow: line_scan_total == 1345
workflow: len(unit_expected) == 83
workflow: len(container_expected) == 34
workflow: len(includes) == 34
row30: contains("[788, 556]")
row30: contains("== 1345")
row30: contains("len(unit_expected) == 83")
row30: contains("len(container_expected) == 34")
row30: contains("len(includes) == 34")
row30: contains("pa36_start_totals == [704, 504]")
row30: contains("pa36_start_line_scan_total == 1209")
row30: contains("pa35_start_totals == [712, 520]")
row30: contains("pa35_start_line_scan_total == 1233")
```

The contains forms are AssertJ varargs in one assertion, not a new vocabulary.
Neither bare two-digit count mirror is used. Historical838/839 and320 remain
untouched and cannot satisfy the qualified unit/container/POM mirrors.
Workflow early aggregate and diagnostic are788, XML exact_inventory totals
788/556, current success report117/1344 and83/788+34/556, current token1345.
Historical PA36 [704,504]/1208/1209 and new PA35 [712,520]/1232/1233 coexist.
Three existing embedded Python bodies parse successfully. Current/final frozen
source inventory is matched against CI/POM, not inferred from target classes.

### Exact named focused identities (157)

Both row30 methods `v001ThroughV017AndPublicContractsRemainByteFrozen` and
`ciPomAndOrdinaryMethodInventoryMatchExactly` execute once in the focused XML.
The four lockstep classes'45 existing identities are included, not counted as
new additions. Every entry below has zero failure/error/skip in final focused XML.

```text
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#defaultWebAndDisabledPersistenceCreateNoIvrReadResources
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#detailDoesNotResolveCursorKeyMaterial
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#enabledGraphLazilyReusesAllQualifiedSubjectReadBeans
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#missingInvalidAmbiguousAndUncreatableResourcesFailClosed
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#noProductionEmployeeVerifierOrNewDependencyIsRegistered
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#onlyExistingSubjectReadPrefixConfiguresKeysAndCredentials
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#ownerReadersRegisterExactlyOnceWithoutReverseModuleEdges
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#requestsPropertiesKeysAndFailuresHaveRedactedRepresentations
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#serviceConfigurationCreatesNoPoolDslContextOrKeyRing
com.onecall.archtests.OperationsIvrSessionReadConfigurationTests#transactionRetainsFiveSecondReadOnlyRepeatableReadSettings
com.onecall.archtests.OperationsIvrSessionReadWebTests#authenticationPrecedesMalformedQueryAndPath
com.onecall.archtests.OperationsIvrSessionReadWebTests#currentAuthorityPrecedesOwnerDependencies
com.onecall.archtests.OperationsIvrSessionReadWebTests#customerAndOperationsFiltersAdmitOnlyExactTwoGets
com.onecall.archtests.OperationsIvrSessionReadWebTests#duplicateUnknownMalformedParametersAndPathAre422
com.onecall.archtests.OperationsIvrSessionReadWebTests#encodedSemicolonDoubleSlashAndExtraSegmentsStayConcealed
com.onecall.archtests.OperationsIvrSessionReadWebTests#exactDetailGetReturnsOnlyFrozenSafeChain
com.onecall.archtests.OperationsIvrSessionReadWebTests#exactListGetReturnsOnlyFrozenPageAndSummary
com.onecall.archtests.OperationsIvrSessionReadWebTests#existingSubjectActivationAndLifecycleRoutesRemainUnchanged
com.onecall.archtests.OperationsIvrSessionReadWebTests#malformedTamperedAndFilterMismatchCursorsAre400
com.onecall.archtests.OperationsIvrSessionReadWebTests#missingAmbiguousAndBrokenServicesFailClosed
com.onecall.archtests.OperationsIvrSessionReadWebTests#missingAmbiguousAndBrokenVerifiersFailClosed
com.onecall.archtests.OperationsIvrSessionReadWebTests#noCallerRoleWorkspaceBodyOrHeaderBecomesAuthority
com.onecall.archtests.OperationsIvrSessionReadWebTests#optionalFieldsAreOmittedAndRequiredNullsRemainPresent
com.onecall.archtests.OperationsIvrSessionReadWebTests#partialAndUnknownFailuresNeverLeakSensitiveCanaries
com.onecall.archtests.OperationsIvrSessionReadWebTests#problemsHaveExactSafeEnvelopeHeadersAndChallenge
com.onecall.archtests.OperationsIvrSessionReadWebTests#wrongMethodsAndUnknownOperationsPathsStayConcealed
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#ciPomAndOrdinaryMethodInventoryMatchExactly
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#pa25DefinesOnlyTheElevenApprovedNonInheritingEdges
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#pa25UsesExactEighteenHistoryAndIndependent410TupleOracle
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#pa36HasNoApplicationApiPoolOrDependencyExpansion
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#v001ThroughV017AndPublicContractsRemainByteFrozen
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#v018GuardsAuthorityHistoryProvenanceAndExactReplayStates
com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests#v018NamesOnlyTheTwoOwnerIssuedAssociationColumns
com.onecall.archtests.architecture.ModuleBoundaryTests#assertsExactAllowedDependencyMapFromPlan62
com.onecall.archtests.architecture.ModuleBoundaryTests#discoveredProductionEdgesStayInsideTheConfiguredMap
com.onecall.archtests.architecture.ModuleBoundaryTests#discoversExactlyTheFrozenModuleSet
com.onecall.archtests.architecture.ModuleBoundaryTests#everyExpectedModuleDeclaresApplicationModule
com.onecall.archtests.architecture.ModuleBoundaryTests#evidenceMediaReadIsNamedAndInternalImplementationCannotBeImported
com.onecall.archtests.architecture.ModuleBoundaryTests#rejectsCycleFixture
com.onecall.archtests.architecture.ModuleBoundaryTests#rejectsForbiddenDependencyFixture
com.onecall.archtests.architecture.ModuleBoundaryTests#rejectsInternalPackageAccessFixture
com.onecall.archtests.architecture.ModuleBoundaryTests#verifiesNoCyclesOrBoundaryViolations
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#applicationAndControllerContainNoForeignSql
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#evidenceSqlNeverReadsMediaRecordingOrProtectedColumns
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#ivrSqlUsesOnlyExplicitApprovedColumns
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#namedInterfaceAndExactThirteenModuleEdgesArePreserved
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#operationsRoutesAndControllerMappingsAreExact
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#publishesOnlyTwoReadMethodsAndExactFrozenRecords
com.onecall.archtests.architecture.OperationsIvrSessionReadBoundaryTests#readPathHasNoWritesAuditOutboxLocksOrCursorStorage
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#disposableLoginHasExactlyThreeExistingNonInheritedSetMemberships
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#evidenceScopeReadsOnlySafeAssociationAndArtifactProjection
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#failedRestorationRollsBackAndEvictsDirtyConnections
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#forbiddenQueriesStayOutsideProductionSqlWithoutPretendingOwnerColumnAcls
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#handoffsAssertAndRestoreSameConnectionBackendAndSnapshot
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#missingEvidenceMembershipFailsDetailWithoutRepair
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#missingIvrMembershipFailsClosedWithoutRepair
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#missingOperationsMembershipFailsClosedBeforeParsing
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#operationsRoleCannotReadIvrButScopedIvrRoleCan
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#poolRolesSettingsLocksCatalogsAndFactsAreCleanAfterEveryExit
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#readOnlyTransactionRejectsAllDmlAndLockingQueries
com.onecall.archtests.container.OperationsIvrSessionReadRoleLeastPrivilegeIT#wrongEntryResourceAndTransactionStateFailClosed
com.onecall.archtests.container.OperationsIvrSessionReadsIT#allResponsesLogsAndBusinessAuditOutboxFingerprintsStaySafe
com.onecall.archtests.container.OperationsIvrSessionReadsIT#callAndDecisionRequiredNullableAndOptionalFieldsAreExact
com.onecall.archtests.container.OperationsIvrSessionReadsIT#callFilterMatchesOnlyExactCallIdentity
com.onecall.archtests.container.OperationsIvrSessionReadsIT#changingLimitReusesCursorWithoutMismatch
com.onecall.archtests.container.OperationsIvrSessionReadsIT#combinedFiltersAndPresenceAwareDigestRemainExact
com.onecall.archtests.container.OperationsIvrSessionReadsIT#concurrentDomainChangesRemainInvisibleWithinSnapshot
com.onecall.archtests.container.OperationsIvrSessionReadsIT#concurrentEmployeeDisableUsesSnapshotThenDeniesNextRequest
com.onecall.archtests.container.OperationsIvrSessionReadsIT#dependencyTimeoutCleanupAndUnknownFailuresAreSanitized
com.onecall.archtests.container.OperationsIvrSessionReadsIT#emptySingleAndExactLimitPagesHaveNullCursor
com.onecall.archtests.container.OperationsIvrSessionReadsIT#legsEventsAndEvidenceHaveDeterministicOrdering
com.onecall.archtests.container.OperationsIvrSessionReadsIT#lookaheadAndTailUseOnlyLastReturnedAnchor
com.onecall.archtests.container.OperationsIvrSessionReadsIT#missingRootIs404ButMissingCallAndInconsistentLinksAre503
com.onecall.archtests.container.OperationsIvrSessionReadsIT#postgres16UsesExactlyFrozenEighteenMigrations
com.onecall.archtests.container.OperationsIvrSessionReadsIT#realHttpActiveAdminAndOpsReturnExactList
com.onecall.archtests.container.OperationsIvrSessionReadsIT#realHttpAuthenticationAuthorityAndAudiencePrecedenceAreExact
com.onecall.archtests.container.OperationsIvrSessionReadsIT#realHttpDetailReturnsExactSafeChain
com.onecall.archtests.container.OperationsIvrSessionReadsIT#realHttpMalformedTamperedWrongPurposeAndFilterMismatchAre400
com.onecall.archtests.container.OperationsIvrSessionReadsIT#realHttpMethodsAndNoncanonicalVariantsStayConcealed
com.onecall.archtests.container.OperationsIvrSessionReadsIT#realHttpParameterDefaultsDuplicatesAndBoundsAreExact
com.onecall.archtests.container.OperationsIvrSessionReadsIT#sessionOptionalNullsAreOmittedAndGuidanceIsNotInferred
com.onecall.archtests.container.OperationsIvrSessionReadsIT#statusFilterMatchesAllFourFrozenStatuses
com.onecall.archtests.container.OperationsIvrSessionReadsIT#subjectFilterMatchesOnlyExactSessionSubject
com.onecall.archtests.container.OperationsIvrSessionReadsIT#taskFilterRequiresPersistedAssociationAndNeverSilentlyDropsPredicate
com.onecall.archtests.container.OperationsIvrSessionReadsIT#timestampTiesAndHighBitUuidsContinueWithoutDuplicates
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#allOwnerReadsUseSameBackendPidTransactionAndSnapshot
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#eachMissingMembershipFailsClosedWithoutPrivilegeRepair
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#eachOwnerReadsOnlyItsOwnApprovedProjection
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#failedRestorationRollsBackAndReusesSameCleanPhysicalConnection
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#frozenAclOracleMigrationsRolesAndFunctionsRemainUnchanged
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#noResidualLocksTransactionsAuditOrOutboxFactsRemain
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#normalAndExceptionalHandoffsRestoreOperationsRole
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#operationsRoleCannotReadForeignBaseTablesWith42501
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#poolReadOnlyIsolationAutocommitAndLocalSettingsAreRestored
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#readOnlyDatabaseRejectsWritesAndLockingQueries
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#syntheticLoginHasExactlySixNonInheritedSetMemberships
com.onecall.archtests.container.OperationsSubjectReadRoleLeastPrivilegeIT#wrongResourceEntryRoleAndMissingTransactionAreRejected
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#activeAndRetainedSharedKeysSupportRotation
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#authenticatedChangedFilterDigestIsFilterMismatch
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#digestTamperingIsInvalidNotFilterMismatch
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#encodingIsRandomizedOpaqueCanonicalAndBounded
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#invalidAnchorPrecisionAndRedactedRepresentationsFailClosed
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#lengthAlphabetVersionAndFrameFailuresAreCursorInvalid
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#missingInvalidAndAmbiguousKeyMaterialIsUnavailable
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#roundTripPreservesMicrosecondAnchorAndUuid
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#subjectAndActivationCursorsAreCursorInvalid
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#tamperedHeaderNonceCiphertextAndTagAreCursorInvalid
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#unknownAndRetiredKeysAreCursorInvalid
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionCursorCodecTests#wrongPurposeEndpointAndSortAreCursorInvalid
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#absentLimitDefaultsToTwenty
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#acceptNegotiationUsesExistingJsonReadRules
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#callAndTaskIdsUseExactCodePointBoundsWithoutTrimming
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#detailRejectsEveryQueryParameter
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#duplicateUnknownAndNullParametersAreValidationFailed
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#limitGrammarAndOneThroughOneHundredBoundsAreExact
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#listAllowsExactlySixQueryKeys
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#presenceAwareDigestHasFixedFieldOrderAndUnambiguousLengths
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#queryOrderAndLimitDoNotChangeDigest
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#sessionStatusesAreExactAndCaseSensitive
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#singleMalformedCursorIsDeferredToCodec
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadRequestTests#subjectAndPathUuidsAreCanonicalLowercase
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#activeAdminAndOpsAreOnlyCurrentAuthorities
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#allFourFilterValuesReachOnlyExactReaderFilters
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#authorityPrecedesParsingAndOwnerDependencyResolution
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#continuationIsExclusiveAndLimitIsNotBound
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#dependencyConfigurationAndDeadlineFailuresAreUnavailable
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#detailRequiresExactlyOneMatchingCall
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#disabledAuthorityIsForbiddenAndCorruptEmployeeFactsAreUnavailable
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#emptyAndTailPagesHaveExplicitNullCursor
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#inconsistentPersistedFactsAndLookaheadFailWholeRequest
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#listUsesLookaheadAndOnlyLastReturnedAnchor
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#missingRootIsNotFoundAndEmptyArraysAreValid
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#nestedArraysAndArtifactRolesAreDeterministic
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#nullableDecisionActionRemainsPresent
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#sessionProjectionHasExactRequiredAndOmittedOptionalProperties
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#singleSnapshotRoleCleanupAndZeroSideEffectsCoverEveryExit
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#tiedInstantsUseUnsignedUuidDescendingOrder
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#unknownProgrammingFailuresAreInternalAndSanitized
com.onecall.core.operations_iam.internal.ivrread.OperationsIvrSessionReadServiceTests#wrongAudienceAndMissingEmployeeAreUnauthenticated
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#activationFilterBatchesCandidatesWithoutSkippingMatchingRows
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#allSuccessDenialAndFailurePathsHaveZeroWriteAuditAndOutboxCalls
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#candidateBudgetExhaustionNeverInventsTailOrContinuation
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#countsRemainLongExactAndIndependentOfHistoryJoins
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#detailHistoriesAreCompleteWithinCapAndOverflowFailsClosed
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#displayBindingAndOptionalProjectionOrderingAreDeterministic
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#invalidOwnerFactsAndRoleRestorationFailuresAreUnavailable
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#latestClaimSelectionPrecedesClaimStatusFiltering
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#listUsesDescendingSeekAndLimitPlusOneWithoutOffset
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#missingActivationAndAnyBindingRiskProjectExactly
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#oneReadOnlyRepeatableNewTransactionOwnsEveryProjection
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#ownerHandoffsAssertAndRestoreLiteralRolesOnOneResource
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#sqlBeanTransactionAndUnknownFailuresMapWithoutLeakingCauses
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#trialUsesOriginalGrantBalanceAndDatabaseDecisionTime
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#trustedCallerAndCurrentEmployeePrecedeInputAndSubjectReads
com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadServiceTests#zeroTwoAndUncreatableOwnerPortsAreUnavailable
```

Focused sorted identity serialization is UTF-8 `class#method\n`, SHA-256
`970c81fe08cfd565b78c8eadc60d2b2cf02fdb405989d0878f02157268afb64d`.
Focused12-file XML manifest SHA-256:
`6e423d810301d2b7db17567a4145855066d4778ecb803c336faf0cea75b124ed`.

All157 exact named focused identities above occur once in the full1344
identity set, with identical class/method names and successful statuses.
Both focused and full executions pass
`EvidenceRoleIvrAssociationAclBoundaryTests#v001ThroughV017AndPublicContractsRemainByteFrozen`.
The full run additionally executes every unrelated baseline test identity.

## Frozen inputs: current = final raw/LF, all26 paths

Raw hashes are checked-out bytes; LF hashes replace only CRLF with LF. Every
raw and LF hash is rechecked against accepted plan §10. V001–V018 are
byte-identical; exactly18 migration filenames, no V019. OpenAPI and
current docs12/14/15/17, current PA25 SQL/runbook/README are unchanged.
Sorted pair-map serialization is path UTF-8, NUL,32 raw digest bytes,32 LF
bytes, no row terminator, SHA-256
`ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`.

```text
api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68 ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30
backend/README.md fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64 bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177
backend/ops/postgresql/pa25-runtime-identities.sql b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9 8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451
backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql 96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6 946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045
backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql 4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505 534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5
backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql 4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7 1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1
backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql 8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030 a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc
backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql 6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065 2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310
backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql 7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d 830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475
backend/src/main/resources/db/migration/V007__activation_and_consent.sql bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81 bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659
backend/src/main/resources/db/migration/V008__evidence_media.sql 32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde 2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e
backend/src/main/resources/db/migration/V009__sharing.sql 3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1 ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae
backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559 fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343
backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b
backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6 13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828
backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql 1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e 691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6
backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8 e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a
backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc 1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3
backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c 988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f
backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql 74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3 57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad
backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c 892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817
docs/12-technology-stack-and-system-architecture.md 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db 772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435
docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31 bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58
docs/15-production-openapi-surface-spec.md db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0 0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02
docs/17-production-security-design-spec.md b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205 4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6
docs/operations/pa25-production-database-identity-provisioning-runbook.md 44ced2245fc1c89175eeb386bc6cc6fa036302e368829cdd163f3da022425423 792a32f72d6657b59a220a42ec7c11f94361078c8c0d491ff52d777fe22f08ab
```

## Full production-tree guard: measured282 ->291

Baseline uses every regular file under backend/src/main/java and
backend/src/main/resources from intake HEAD, excluding only the inherited
V018 filename. Baseline282-file manifest aggregate:
`df6ff3b9140a365ecac1cd376a2abc8bc3a15e8fab6562fbe6673994d82af5ea`.
It is independently reconstructed from Git blobs, not stale build output.

Final uses the same full walk and sole V018 exclusion, with NO PA35 or other
exclusions. Measured291 =282+9 files, including all13 authorized production
changes. Each line is backend-relative path, one space, canonical-LF SHA256;
lexicographically sort lines and hash UTF-8 joined with LF plus final LF.
Measured aggregate:
`95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0`.
This value replaces the old aggregate in row30 and is not invented from counts.
The production count291 is independent of historical test-token count291.

Final exact291-path manifest basis:

```text
src/main/java/com/onecall/core/OneCallApplication.java 9cf42dab5e5f2238518778067fa7706ad72d8d89365cd9f76b1d08c6c6924a8d
src/main/java/com/onecall/core/activation_consent/ModuleMarker.java 297f41c742abbce48292d72d92195b34371b32e4ed74343a9c56a7da055f23e4
src/main/java/com/onecall/core/activation_consent/customeractivation/CustomerSubjectActivationService.java 1ff7e36a6bba8f2122aeb51e9ccd866444d76b1f623d2899a95574b8033f15ea
src/main/java/com/onecall/core/activation_consent/customeractivation/SubjectActivationException.java 2530313ddd32ed0848b29800e67b5d3666ac3f1b4adc893819719f01be726841
src/main/java/com/onecall/core/activation_consent/customeractivation/package-info.java 0aa943e4fc521b6afd109c4504229d834750f3a2009e9c0836523b6c285f6377
src/main/java/com/onecall/core/activation_consent/customerquery/CustomerActivationStatusReader.java 2b8c89c425e8fca6e19f12411a6a7835df6a3e5acf82b749aed3b7d651aecbb0
src/main/java/com/onecall/core/activation_consent/customerquery/package-info.java ebde4ee2c769829fabdca1d84cb16eff2788cee9e3dcf710b3f5c3e6c1e20d86
src/main/java/com/onecall/core/activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java f6e722782146b6281dab9255e852f36a73f749cd5745a017229f92c9b86fa2b4
src/main/java/com/onecall/core/activation_consent/internal/command/ActivationCommandPersistenceProperties.java 7bf6908784a73c00caffd993ba0c99af7573f2f59517024a0c8b75b4cb13d405
src/main/java/com/onecall/core/activation_consent/internal/command/ActivationSemantics.java 8fb2309c75f6a2b10e4b6b6d1f9d9d626b66429deb88e0299a1706ff7b0d499b
src/main/java/com/onecall/core/activation_consent/internal/command/DefaultCustomerSubjectActivationService.java 0880c4725dd8070828fe1635f369254a4ffbc8535817436d1363dee7fcaf54ec
src/main/java/com/onecall/core/activation_consent/internal/command/JooqSubjectActivationRepository.java ab2a2fb4dfc9c91434e9fb0d1a1a6a50a2c833363a8b3d474bf4c95b7d565a01
src/main/java/com/onecall/core/activation_consent/internal/command/SubjectActivationRepository.java a35c409c58380ab73f9ae47e3ba824d9e4b56d86cba959c77930ad564530455d
src/main/java/com/onecall/core/activation_consent/internal/customerquery/CustomerActivationQueryConfiguration.java 216d2e49c09977a338c806402a569ba75a81ef801e2ad6aec68d2bc841feecc2
src/main/java/com/onecall/core/activation_consent/internal/customerquery/JooqCustomerActivationStatusReader.java 8081a4d502ea6d3d75c154348e91d982efea840e2384e0ede0b8c26b50ed4db8
src/main/java/com/onecall/core/activation_consent/internal/operations/DefaultOperationsSubjectLifecycleService.java cdcfe7949c835e1531bd0df72aeca769fc68c70c608f90fc2519f88e61b611ed
src/main/java/com/onecall/core/activation_consent/internal/operations/JooqOperationsLifecycleRepository.java ab0807d06e20e423c9fe60f9e0d3fa23707290de9625959045d067e06dac481c
src/main/java/com/onecall/core/activation_consent/internal/operations/OperationsLifecyclePersistenceConfiguration.java 7a63c86bb42a6d99f5e2581fad3d8cd6da9224a6ab04ceaf56dd2ad09fccfa35
src/main/java/com/onecall/core/activation_consent/internal/operations/OperationsLifecyclePersistenceProperties.java d3bc5913d77b0fcfc0a2e96668d36efd679556944644bb87eabf6f9fdebc6abd
src/main/java/com/onecall/core/activation_consent/internal/operations/OperationsLifecycleRepository.java f690d1f7f1e6846a5b14c2495a574cf8fef89741c24b8b62bb769b43af9ef1be
src/main/java/com/onecall/core/activation_consent/internal/operations/OperationsLifecycleSemantics.java d7401e57d7cf75579b6b348ed7000263cc53b967fc7298ffa663e093e95dd91c
src/main/java/com/onecall/core/activation_consent/internal/operationsread/JooqOperationsActivationHistoryReader.java 7d38c5d5aca33c42ea25c090d3d510c3ac89c23def17ea0a2b97d896cc4e19ca
src/main/java/com/onecall/core/activation_consent/internal/operationsread/JooqOperationsActivationReader.java 83845d262fc1edd53cee83797bd2a1c243a7e155d3d6590a28c805b447bcab67
src/main/java/com/onecall/core/activation_consent/package-info.java 23d42b13980b53e001a4b62a26e361a7e228d9add018bac9cb26e132f7d2e8ca
src/main/java/com/onecall/core/ai_orchestration/ModuleMarker.java 92af7c66f48f5ea201674f0e09d2f9d9d4d40c2ab56eba4516361287406e4b84
src/main/java/com/onecall/core/ai_orchestration/package-info.java 074490377867b44852009ad0d51c9b63388534c5f86291b6b5d7f88a2d25b702
src/main/java/com/onecall/core/catalog_order/ModuleMarker.java 7e060a820cb6298a23a3561b6c939eb6995a36d8677e6e5a2062793d77c02c94
src/main/java/com/onecall/core/catalog_order/customerquery/CustomerSubjectOrderCountReader.java 1c9c5f714b171f0718eef267583582b6b7d17e392c9d8d016ca4bd4d74f3c167
src/main/java/com/onecall/core/catalog_order/customerquery/package-info.java 6528499e2f68353d98521036dea1b5908034bd705f38c823cd9d66e556dd576f
src/main/java/com/onecall/core/catalog_order/internal/customerquery/CustomerSubjectOrderCountQueryConfiguration.java beeddc522c2dc289a0e84848c8477977a1c1dbd15f10aeda4012d8cf4e9d04f3
src/main/java/com/onecall/core/catalog_order/internal/customerquery/JooqCustomerSubjectOrderCountReader.java 12035485f801abbd9690be41fea8da246b049dfef5bab3ce37ae310d858fc41a
src/main/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsOrderCounter.java b66fe398e2ae094cae198adb36b7d8de973309be18c6dc4835b8c4c7ee2eda17
src/main/java/com/onecall/core/catalog_order/package-info.java 86bf29ce6b4544204c7dd3e0ec984bf3848d839569775de7cd8d32d8a8faa52e
src/main/java/com/onecall/core/customer_iam/ModuleMarker.java f18d49a01ed416e61eca2e13b8cec218ba65b910f877670804718adfe51414fa
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryService.java 4106e51871ac7904e4dfa7724518be443ada01f597039a6f80fb663b65431a6a
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContext.java abccb4163d0cbf6b73203996d49740151887099c18e9fcfe858bf24a1a686bce
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContextException.java 41e1860eb2f7f32aa1883a951a7672790b2e0cbe2df349048bc10d29570894ec
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContextService.java 9635d99031b5a223be4770cb7aaf746d6945f13783ad2c78a951eb516347a8ac
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerEvidenceQueryService.java 43cf46a2fe0421a688e16eb505f0a285b2cb378514623e8dad133252798c8ae3
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectActivationCommandService.java fef08a59a2b511023b913bdac4458554b912ee4ab8a4618bfcd47ae8c021d23d
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectClaimCommandService.java cfca344d14fc5c5a130bb4125d45f0b9aacf04dc22c11edba79c6f0348ce3af8
src/main/java/com/onecall/core/customer_iam/internal/application/CustomerSubjectDetailQueryService.java 945446d03ceccf18b7fd45d21c32849ce318c06e32598af8747585736ca62dda
src/main/java/com/onecall/core/customer_iam/internal/application/SelectedCustomerWorkspace.java 0cec63f824843df504ca26233efd945df7d6cf1ececca0e9e22039943931caa3
src/main/java/com/onecall/core/customer_iam/internal/application/TrustedCustomerCaller.java 36f3ea1c8af7875c44ff445658dc5258ff4f6c5d477bf148834264b0d932843c
src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerContextRepository.java 17679250dc65ef3f5e2dc04241d5e1cb129c13e7c4d43938af1381f63298694d
src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java 113eea2cf94f102dec747eb7281a698a579bdc6c1ef933c25c1d2c92fd81c600
src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceProperties.java fafd75036bc711e647447ed2a5a4e75fb22f708572161843d66a93d5927f2442
src/main/java/com/onecall/core/customer_iam/internal/persistence/JooqCustomerBusinessNumberAccessRepository.java 0ebed8ca6b3790ad45a1c60976585aa6fbc2d8f81a4bcb8c000c25bc666ffaa9
src/main/java/com/onecall/core/customer_iam/internal/persistence/JooqCustomerContextRepository.java 0d646989f2c2c911968b38a6388f5f090c5e6252154588086f2359d94392d983
src/main/java/com/onecall/core/customer_iam/internal/security/CustomerBearerAuthenticator.java 9def8dc92b20edb65e7f1a1aef81bc886043d75055c28420af1eb3f87e42f79a
src/main/java/com/onecall/core/customer_iam/internal/security/CustomerSessionVerificationException.java f59d8bc9d50b974ea406ff049dbfaefa8f48222019ab51af79031fe812378589
src/main/java/com/onecall/core/customer_iam/internal/security/CustomerSessionVerifier.java f5aa97f50ed4d6e729fcb4540604d617b55ef305759e5cae855b11d760e54b8b
src/main/java/com/onecall/core/customer_iam/internal/security/OpaqueCustomerBearer.java d83a9edbddef1027b246fec08c9b81d7f8ed4d22dfeefc49544c5d981716d6bc
src/main/java/com/onecall/core/customer_iam/internal/security/VerifiedCustomerSession.java 5abc12914f77e5831fecb319c904717bb2f08d3e30e0f0487e1a5777c90ff932
src/main/java/com/onecall/core/customer_iam/internal/web/BusinessNumberPage.java 706a3939275a028552b497ab4256eff7d55d907f419e911fccbf80819382fc2a
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerApiErrorController.java 88524c3f576752435407fb1d8cf3c26fcc9b6c1aa2b333e9bc910389e064fb29
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java abfc5b70248f4196a5265386c562a665730fccb86e660d6a272535dd1c502953
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorCodec.java 5f48be1e66513522605859a9e15bba07ad2086ea9ebd2239dfa3dd86aff65553
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumberCursorProperties.java d557edc31aa948a5dc76222923370c22c63838d50e5b70ebb5005f306ad93cf0
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBusinessNumbersController.java 4cbc4ec79235dee5064264ff47b054c0368f9104201c729b07af59f910e7ae2d
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceController.java b0c5484ec642889f2ca652213be27c76aa040dbf3026d636afb9971bc194b460
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceCursorCodec.java e9ff116adfda5e89d7805bd7099d473175ff5272440d1814d4867e56b37a2e70
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceCursorProperties.java 326aef52b39fbf4be1ff1379438b9a6bbf7b78a830454a27334c3734f24ad5e2
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerEvidenceDtos.java f17ca3ba4a12f60a5c7499ba33436bfd7ba46ff78b12787f8a9a6ed126972b11
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerMeController.java bb357e4a85684358c78195c3f6126e87b97bb42dd88bc5d5453bc08b0c6dd11e
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java 1b7600195df3ffa6e78132c89de251c00dfbd40a6afc2584cea7c974ecc5cc3f
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerRequestPrincipal.java 12175355a625df2bd1bbbcc59e1c6d91803e349656161842d16d3abca2f5cfe2
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandDtos.java 8b22b0790c137a0fdd43980f94fbdf7960f6ed5457b2c6e5be533a931bece444
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCommandJsonBinder.java a79cb02763b9b55d91ca9b1c95591d3a968797bf919dbb58d69b87c40b2fed2e
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerShareCursorCodec.java 8c8d1c25be2464e4933105273bbd2651a71fcd676d55d7bcc665cd10b6f54c52
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSharePage.java 6d6b3833c2d20dc037c33921487167549782efd8c6e7e3d37afc437ce1923bad
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSharesController.java 61b928e98539bfd9b4b9ef885fec520b84266b8899f17a5fa0172a84d87dec79
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationDtos.java 50f73907c95adf1b61afccdfe0425ceeebe8712e7d236e06b43b66cf5c3ea53b
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationJsonBinder.java 1572a3e10770f1d0f80823c366e203690d1f67d2a65257bd22ab6174567d7be4
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationsController.java 590f183860d338f5798814e032abf4401450b4ac0a6b7dda8f0cbc60cbfd0db6
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimDtos.java faae4237909c0b20965ac0a64695deb2b607f6674e345acfc1cffb0b52e2ecfd
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimJsonBinder.java 0298b80bf6eb44b76043869d40916bb77903381e62270664a44098d20004c20e
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectClaimsController.java ee514f8f491a7a6c8778dc17038f85c022a0937fdaf5c9b9606aeded3ac1c253
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectsController.java 231081f7befe725d419f64f454e43e9f8dcfa34d3bf982c971f23fd206b13112
src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java 34d201117728d0bc2e50a0ccb5a3c8c0b6c7887a2476c2338df7032e41cf8e37
src/main/java/com/onecall/core/customer_iam/internal/web/PublicSharesController.java c186150ba183891e5a93c605328ce8d24ecc64213755844268d2ec59c8aa0d38
src/main/java/com/onecall/core/customer_iam/package-info.java dbb97d8ee38bf98ae3e4ea622c52cc4349aaae83f6f6356cf4ebc251ed6ffae5
src/main/java/com/onecall/core/entitlement_metering/ModuleMarker.java c9f8f35d4d9e02adc0e12878c2e37883b2d017b6f815b06c37ea19dcffaaa780
src/main/java/com/onecall/core/entitlement_metering/activationtrial/ActivationTrialService.java c6ca59698ae10329bff0c75c4a03c2ac28e2399cb7351691d4d3e6ae8defb72e
src/main/java/com/onecall/core/entitlement_metering/activationtrial/package-info.java 46023fa34915dba35a09def2467e0de47535370e2075d7d80fe0fae16b97df84
src/main/java/com/onecall/core/entitlement_metering/internal/activation/ActivationTrialPolicySnapshot.java 310ba6255d9f3e9748d7f043633874469d393b4206bab27fa70498cdac4f83a5
src/main/java/com/onecall/core/entitlement_metering/internal/activation/DefaultActivationTrialService.java 0c308786dcd27d86be22ed85074b6d673a85b7e5f8cc557adf91943a04884ec4
src/main/java/com/onecall/core/entitlement_metering/internal/operationsread/JooqOperationsTrialReader.java 2e2b09e197c6e3b16628b0fa38e934b90f7a016fa29bc8b48427ed3fb47b8f39
src/main/java/com/onecall/core/entitlement_metering/package-info.java f1a59e78a2a2da1f40242491769b7545afaa2d0bccd94b1f8932ff94a33d9a4d
src/main/java/com/onecall/core/evidence/ModuleMarker.java 1141690370e4463d1bdffc8ae9b8e0e49a32e3342694125f5a75a55f8d3c9b99
src/main/java/com/onecall/core/evidence/customerevidencequery/CustomerEvidenceReader.java fe929e2e43867767ca3d95d4676d9818763fa770fbcefdd291d02b2b6044086a
src/main/java/com/onecall/core/evidence/customerevidencequery/package-info.java 03e24b42c17430ba6389e94e924fa06e918cce0397e38e39c1c889fc9b13cd09
src/main/java/com/onecall/core/evidence/customerquery/CustomerSubjectEvidenceCountReader.java 5d2aaa1aae1bae604838d91caf8efdceeb3a8396b1cc57a9b48352aee64fd323
src/main/java/com/onecall/core/evidence/customerquery/package-info.java da63655503f5ee8ddbc80a0e5f40ea4e24d3f4a86e12550c625d6ea10d1a42d4
src/main/java/com/onecall/core/evidence/internal/customerevidencequery/CustomerEvidenceQueryConfiguration.java 330b3553a2d14a85498d3a7ce5617e82bd36d8bf0c409ec2dc07f6c07ee6cc1e
src/main/java/com/onecall/core/evidence/internal/customerevidencequery/JooqCustomerEvidenceReader.java 95a2ceb9f13a425f30cfe3e41f66a04328ff0d5248bd58ac4ed175c86876feff
src/main/java/com/onecall/core/evidence/internal/customerquery/CustomerSubjectEvidenceCountQueryConfiguration.java b92d842e942a0c9b01c2b311dc5cb2f780f3ef429c829e7972546c9a0fdeb627
src/main/java/com/onecall/core/evidence/internal/customerquery/JooqCustomerSubjectEvidenceCountReader.java d37b468a509eadd7072691f94ae63b0a5ab9c25c1726d2baafbc512cd5fadb35
src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceConfiguration.java 1a9c7de1457f6a862eb2f02fc095cc3ff9bc5f2a4d2d10b1ddcc6266d74e0c17
src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceProperties.java 43c8b744e9a2d1dda0c9f486df5914eb4247478185d5f0f027d1a9dd64b650fc
src/main/java/com/onecall/core/evidence/internal/mediaread/JooqEvidenceMediaObjectReader.java db7ce58228053f1288424f44c1022f7d12b7a20b2bdd52fd310b90efab4cfb31
src/main/java/com/onecall/core/evidence/internal/operationsread/JooqOperationsEvidenceCounter.java f7dc9cfde51fa555ac083b90a2b3e532d06fa51cfffee4f33d308c5440c099a4
src/main/java/com/onecall/core/evidence/internal/operationsread/JooqOperationsIvrEvidenceReader.java 1b945ad4a0fa422b400fa8712888083e09787c5d801dbd367fe9109146ea5738
src/main/java/com/onecall/core/evidence/internal/shareprojection/JooqShareableEvidenceProjectionReader.java ba64ef85dd687a8d8a99b4e8efd9feabcab8549adb226da84855ecb8c30bb60f
src/main/java/com/onecall/core/evidence/internal/shareprojection/ShareProjectionReaderConfiguration.java 5e458e0f77f6810e6a225db0133587d7f8dc38f21e438f5fbbfe000dccfcc7aa
src/main/java/com/onecall/core/evidence/mediaread/ControlledMediaByteSource.java 8d2da779a7ce2713f5cc8a5a8726b3173c7dfe7a3e093fddecde9beb5e9d10a7
src/main/java/com/onecall/core/evidence/mediaread/EvidenceMediaObjectReader.java add665ffe72d908f9dd541da6857f572303f01937b6f3c10aa0d0207c5375234
src/main/java/com/onecall/core/evidence/mediaread/ResolvedMediaObject.java 1d85d12d006915e886be046dc9b96ca475c7eb4c18b7d83f112289d7412743b3
src/main/java/com/onecall/core/evidence/mediaread/package-info.java 1924dda27139946485a917b56a95f33f4d597e58ab8058d1557b85dbb7374bbf
src/main/java/com/onecall/core/evidence/package-info.java f6555cd6144ba963e88cc0cc433b6833cee44e990c64fd53e4633610ec3721e6
src/main/java/com/onecall/core/evidence/shareprojection/ShareableEvidenceProjection.java 7d376431c346fbc35579022b27ffb6983ee988325a5c932f49dd7293afd17f4b
src/main/java/com/onecall/core/evidence/shareprojection/ShareableEvidenceProjectionReader.java 2b307e0d882071e7596566af101018b1ed35dd839f55f7bad2afacb93e8238fc
src/main/java/com/onecall/core/evidence/shareprojection/package-info.java 52477b24bcf9cfdc32193d39d3767c0880531ecc0280447d69b63b7a49390de1
src/main/java/com/onecall/core/ivr/ModuleMarker.java 0398c11169d7ac59e2b7cdea134736250e5d0b58aedad80051817c69285da1f8
src/main/java/com/onecall/core/ivr/internal/operationsread/JooqOperationsIvrSessionReader.java 3a6626a4c9e01679fc3a638bab8e7d7d2fa991d069c1c35705aacc7390e7adfe
src/main/java/com/onecall/core/ivr/package-info.java 5f893bc09c81e71e4687d10de20d8076e60b766ced542e50ed636e3f3f38c3f4
src/main/java/com/onecall/core/notary/ModuleMarker.java f16b705fbae3589ff9615d2a04d05fd10b7324dbe125836ba04a157bb3f3221f
src/main/java/com/onecall/core/notary/package-info.java 47f1ab843a417d45eafe64bd0ed269b2d987d28e77a211b6801405042dcaaf17
src/main/java/com/onecall/core/operations_audit/ModuleMarker.java 23f430c40bcc3680651e587a93074775cba8954f548dccb883b7eff09146da11
src/main/java/com/onecall/core/operations_audit/package-info.java 354a05fce8ad111134da6359f950350b1bfd249fdff170d0ed64e9675870326b
src/main/java/com/onecall/core/operations_iam/ModuleMarker.java e62e33c3d4bf04c834890724210e431c82cb175a0bb793745026e238c8539509
src/main/java/com/onecall/core/operations_iam/employeelock/EmployeeAdministratorLock.java 529d001741309cdd2762a9a0a87de5ac8330185b9a7796cac1475144c1998600
src/main/java/com/onecall/core/operations_iam/employeelock/package-info.java dad65ed4a541e0b46ecfa57094cfab34053d91cf655b3773fbd70fff3bb33649
src/main/java/com/onecall/core/operations_iam/internal/activationread/DefaultOperationsActivationReadService.java 6fb61939fa9a98a4d5113650d9b9a17d8c6f3f7e03ca494d03ea5013a7a6408c
src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationCursorCodec.java f9c7ff4c10ce0b655d276eb8f03fc62ce3e8df234829d83cdf3ecc995863c453
src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadConfiguration.java b13b8c36afcaa86959eade568e479fe92008aa49ae138da80fdbecbbd73a500f
src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadRequest.java aa849958fdd872522b6868a822782aadedf0082db662c450cee83bebe0f8b419
src/main/java/com/onecall/core/operations_iam/internal/employeelock/DefaultEmployeeAdministratorLock.java 10e08fa18e858e9cb812771b542f0b5de9e824f8c1456b3aeaca95fe30ae9d45
src/main/java/com/onecall/core/operations_iam/internal/ivrread/DefaultOperationsIvrSessionReadService.java 8118afed2c8a4da946bb80af0b3ab0e728630c0514a4862cb2288d64b4276572
src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionCursorCodec.java 1242d4573b31f2954ab037a4f353e035322f709d8f581f36bddbaed687a2f38d
src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java e83c96c549b9f7bcca69b9ca4f2b8907b9f5c5e54b9fa39ef0711654f5c2150c
src/main/java/com/onecall/core/operations_iam/internal/ivrread/OperationsIvrSessionReadRequest.java 245514f2b47252e2220dae8d02382abc2a9113d7ebc13694a7ae01a4373a5ddf
src/main/java/com/onecall/core/operations_iam/internal/security/EmployeeBearerAuthenticator.java 43fde32830c8a55693698853477eb7509accd3b53403892980165c2a82ed8fc4
src/main/java/com/onecall/core/operations_iam/internal/security/EmployeeSessionVerificationException.java 3353f229d8d88b62bfd629ba5c9e54338f45b4936d2c0f13cc7ade96e863a3ea
src/main/java/com/onecall/core/operations_iam/internal/security/EmployeeSessionVerifier.java ef08bf9d0b4921d3da4b93e04ba4e60ff184a3612f5114446c7f8ccb01187e64
src/main/java/com/onecall/core/operations_iam/internal/security/OpaqueEmployeeBearer.java eb1e51078b6361203bf9c038df1c0e2487c6bf5dd406b5ace2dc9c26caa8d062
src/main/java/com/onecall/core/operations_iam/internal/security/VerifiedEmployeeSession.java 990511899e0d7c0cbfe543816037effd8c66bcbce45ea2094c7c5356e8318d94
src/main/java/com/onecall/core/operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java e66f36e7ac65aa94407f85ed3b30855172d860e02c117e7c37d92f581e438bd9
src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectCursorCodec.java 55fae62e843ef835a7fe2de5fd67e0e78b0f9d4af8e5e2142d08b43b74bfbd24
src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java 46b59622370616b24d33cf804a20c72a4994ffe3bcaa68044f6d91ba90436190
src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadProperties.java c917ee0f12d97844dfa15dad3ea229955cae9a52049bfe3cafd3d9c147b4f186
src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadRequest.java 62ab1a5bdeb99b9309dd31d37003d97f845bc1f3e18bc6c87cc4084b760f241d
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsActivationReadController.java 53e36835bb13dba7f80ac02f6c0d40d670be74d917424d04e444af2a5600c80d
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsBoundaryFilter.java 91942aca2856e4340b19cf457b494ae19a1c3c9db41b83d0d351df3bff0824f5
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsIvrSessionReadController.java a052f257bb92ffb3a05118edae1ba920d35767a1efef8f14699835d7af7103ce
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsProblemWriter.java 51ba30aa3ea9503f455cd3b92480319623e50a67bf84a37325c899c6c432d757
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsRequestPrincipal.java bca09da6465b390cb4fe58b50e003f90c9803ebefdcad58daa08c96f2566dcdd
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsSubjectLifecycleJsonBinder.java 02f0a5a12d2d89b147edc12a2e900c4177938efcad2a74dede28e49f1a79bc1b
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsSubjectReadController.java b469885f288bbfa736fe8a911ad57c736c18239f624d80e6b9757bd8db6f7c5d
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsSubjectsController.java 8724b0433d473cf6051de72730686b750c050d96953771de1e3ed94c40887ea4
src/main/java/com/onecall/core/operations_iam/internal/web/OperationsWebConfiguration.java 9115b665925d30f191eff930ee037deb96fdf56244a58926c487491da8e82ad7
src/main/java/com/onecall/core/operations_iam/package-info.java 09574d6dd730fc57426f0828ed9ce3827bae5ff3de11b0d24df5e02ef6758eac
src/main/java/com/onecall/core/operations_iam/subjectlifecycle/OperationsSubjectLifecycleService.java 34df02f1f89f178f7693e7ca5a18f12d8dd48e7a418585dc635b4193e1414041
src/main/java/com/onecall/core/operations_iam/subjectlifecycle/package-info.java 20ee68689125f2cc017701dd95596e0fbb06367e5b4d9af3703d02d7cc594786
src/main/java/com/onecall/core/operations_iam/subjectread/OperationsActivationReadPorts.java 74fbecc4a4d3f5141fda28162deb33d5baa66d61ff0caf8a74ca005d391964a3
src/main/java/com/onecall/core/operations_iam/subjectread/OperationsActivationReadService.java 6fca57492b08a4001f3edbae574fde04d0338d7a30025b993e867e33da1e6205
src/main/java/com/onecall/core/operations_iam/subjectread/OperationsIvrSessionReadPorts.java 2c204ebad08d12b7da02493bb770a6cb3f186156de419327d14f14a6c56931f4
src/main/java/com/onecall/core/operations_iam/subjectread/OperationsIvrSessionReadService.java 5736f8112a1f47c99d3d9af5d311313fb8b6b681dffbb04ae556377d921de026
src/main/java/com/onecall/core/operations_iam/subjectread/OperationsReadContext.java 734e82e2bfc1f33a1d0c2e3f1cce4cf634bf15796503f82338253e99aff4289d
src/main/java/com/onecall/core/operations_iam/subjectread/OperationsSubjectReadPorts.java 4c0fba5dccaaf6ca8bbe858b85c3450928c531d08a4b724b015df034cbb4592b
src/main/java/com/onecall/core/operations_iam/subjectread/OperationsSubjectReadService.java d4acabf31ee1cbfb3cfb2c8c3b3cb79cd9524803954580fa70d7c40eb8a77ce9
src/main/java/com/onecall/core/operations_iam/subjectread/package-info.java 99087378fd57d523eaa8907509b33bebefdc91cc89358af51870717f326ae4c3
src/main/java/com/onecall/core/organization_access/ModuleMarker.java a36cda0bf966df7afce6fed0c53de985e03c61115d199d18a60ae952ba8406ab
src/main/java/com/onecall/core/organization_access/package-info.java ea2202b9105fdbbc473f061cf0d8e9aad44cb01fbf10569983dc2406da835db4
src/main/java/com/onecall/core/platform_reliability/ModuleMarker.java 307b65386aab09d0683f10af366252ae3e9a93e5d9e1952477f4a43b38f077c0
src/main/java/com/onecall/core/platform_reliability/package-info.java b0ce6ae5c04158a94ca755d47bb3b62f5f51f496aac7e26d6be5724bd6f42738
src/main/java/com/onecall/core/sharing/ModuleMarker.java 589b7a4083979a7164a66a99164a494843b89ae803b705034fb08857d205cca6
src/main/java/com/onecall/core/sharing/internal/admission/DefaultShareAdmissionService.java ab748d5ef2ef908d2f65b53fb9f910ab0bb034ee1935d43d10565c1c12d2304f
src/main/java/com/onecall/core/sharing/internal/admission/DefaultShareMediaAuthorizationService.java abb582a1c324452aba1530cc97bb1f6607ad2be6de471af9707ef70cdb161e07
src/main/java/com/onecall/core/sharing/internal/admission/DesignatedMobileProofVerifier.java e40db31210763a4230e7c8c27005f49f2c3a4a3f1b174e8d2409c7d33e0d1a7e
src/main/java/com/onecall/core/sharing/internal/admission/JooqShareAdmissionRepository.java e98328071d7731d5dc17beae73daa8e317867e4d3eb39b697c447b4425b29731
src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionCodeVerifier.java 775815cf99025c112bdf23d65e9d6d47d6b5b6b1cac851839d48a8d347013d71
src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionPolicyProvider.java dd3379563daabcf9f831b656941996f613f33d414a8ad7046285d0444c64d87e
src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionRepository.java bc6a3745c0cbfcfeb5ea01b3a9f21d501fcdcb1df6f3e8b1ba6642582e27be4b
src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionSha256Provider.java 753ca4348e36ae0e59a4f89bfba50ce2f1b237bc6e7d5a3dcf799f517024008c
src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionTicketProvider.java 99e0546cf9a0988f434c4e4eee65003b90299e9f9c069b430ac2d4a62951a061
src/main/java/com/onecall/core/sharing/internal/admission/ShareMediaCredentialProvider.java f24cabf7eab2fd733f316aa1e97a72b01c07a06a02ff565492b3af4de78a4b3e
src/main/java/com/onecall/core/sharing/internal/application/CustomerShareQueryService.java d825d5f1af12d2f5f2fda1ea09dbb22c620f3bac62d564d97c29e3b2af64ea71
src/main/java/com/onecall/core/sharing/internal/application/ShareCursor.java 7a581a59288a9278e717d40ed277a91f13b90018d3659382ab65aac573368a76
src/main/java/com/onecall/core/sharing/internal/application/SharePage.java 479600092d49a5f23f3ceeeb50e07d750561e6b885d5fa56eb7714eff07e7903
src/main/java/com/onecall/core/sharing/internal/application/SharePageRequest.java 0dc74c3b61d88d54474dbfb2f488664fa6ed378c27cf0c5d10f5654e1dfdbc2f
src/main/java/com/onecall/core/sharing/internal/application/ShareQueryDiagnostic.java b116b3c5a57f4d7bb9bdc7a04665c5210780f474f42913a94c8b4eaeb25fb2ff
src/main/java/com/onecall/core/sharing/internal/application/ShareQueryException.java c1473ed79d78f23c071d1fc06ec5de1ef91148b30a7ee199bd25bb9843a16d4d
src/main/java/com/onecall/core/sharing/internal/application/ShareSummary.java daba1db7b08f179046ceea81d5aab5c7814491c5b352b25441760601ecf20c28
src/main/java/com/onecall/core/sharing/internal/application/TrustedShareCaller.java 52c634b1b33d64072e59c86e57d93a8143b384287761cfeeb9e48dcee7fda7e8
src/main/java/com/onecall/core/sharing/internal/application/package-info.java 51c2c1bcb34255ee3b31c046d15d79ecc7a928680b3afe69d576610a155e5599
src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommand.java cbd41d15fb13f6772d3dcfba7282a2bf6f7800d49c31e1e4f0824cf5c15d0858
src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommandResult.java 7930b35bab0b43e39146953dda2bc3c99e49bded5144db16ba5d59adbdea3a4d
src/main/java/com/onecall/core/sharing/internal/command/CustomerShareCommandService.java fe0d79b151a369ba5226a2a91f76b8485b3719d8ebfadf2572d62849b0ae3403
src/main/java/com/onecall/core/sharing/internal/command/ShareCommandCaller.java 9457bb6cfdabbab652b888e78b3aff3dbab704ce2a377d6c9293457d5e705db0
src/main/java/com/onecall/core/sharing/internal/command/ShareCommandException.java 92bb4871acf8d8a9de2d28553619e2ec237e92c74acb317e690624ab1b639b18
src/main/java/com/onecall/core/sharing/internal/command/package-info.java bba4e90561535fef3c8f81e2c7ce7f7dcbd2701414df870bc7caf378b0cf4308
src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java fee79e17d1561a19fe54481c3d876a709e67d28666c4c1eca5ba9cacaa12a6b1
src/main/java/com/onecall/core/sharing/internal/credential/MediaPlaybackCredentialAuthority.java 7dbbaaaa8f4562663676be715c02990d0c662d0001ca9718f40301b721b47208
src/main/java/com/onecall/core/sharing/internal/credential/MediaPlaybackCredentialCodec.java a30b082e4a87ebec8ca01f75af61830733c6b7f6c24cf042657edc4ba0c855de
src/main/java/com/onecall/core/sharing/internal/credential/MediaPlaybackSigningKeyProvider.java e0c9a284db66ff771d2060ddedd738dfe7c98deb093a0262522e9ec7ac2fac35
src/main/java/com/onecall/core/sharing/internal/gateway/MediaGatewayConfiguration.java ce84220daa6dea0143ab12f8c6703bc287ad68d92260756e7f1f94a920a47ed6
src/main/java/com/onecall/core/sharing/internal/gateway/MediaGatewayFilter.java babcd5ddeddad864814eb7222405965739a9a66279f372c28e5d0cbd330c1e79
src/main/java/com/onecall/core/sharing/internal/gateway/MediaGatewayProblemWriter.java 9ca0e5bb47df307ae85d2301454d94df5155e086a2915551b9fbe1b5127fbc11
src/main/java/com/onecall/core/sharing/internal/gateway/SingleByteRange.java 1ca901545b809cc5ac1af19f1a9faf0533a0714b9365bb8ec96765ea25faf0d9
src/main/java/com/onecall/core/sharing/internal/keysource/FileMediaPlaybackSigningKeyProvider.java 3a58b091860ddb40e15e2620099341e1769f20aacd9849a570a69e4c0acf78fc
src/main/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackKeyBundleParser.java c54d653a9ac9e55de860b2b6638127db0741dcbcb2c977c0578774e44f250122
src/main/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackKeySourceConfiguration.java b15b3796b3bb54ae277625f4cbd34b4eafddc67faee22bfbd1a84372a3ca5e5a
src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmMediaPlaybackSigningKeyProvider.java b481cda71ef7943eed41a26479e722d8fe5d5a0f09d2b0f719e056da5e08a12c
src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmSecretStringClient.java 639dc952f6bfc587ef2bbd6e61eb7040c9126fa106ef065e6d1a2a7e66589f7c
src/main/java/com/onecall/core/sharing/internal/persistence/JooqPublicShareQueryRepository.java 2ce3f1be0d298e75336188106e9e16e36884479d3d632541be8b3e04020bd09e
src/main/java/com/onecall/core/sharing/internal/persistence/JooqShareQueryRepository.java 483c1a37b18ce2818d216e16fe2e16c75a8b311e33d95a185b3c49d51cd9cf87
src/main/java/com/onecall/core/sharing/internal/persistence/JooqSharingCommandRepository.java 49ec3dcfb87b595a31ecee88f99e941cb2c78ead3560f40a270407ee8c84d2ba
src/main/java/com/onecall/core/sharing/internal/persistence/PublicSharePersistenceConfiguration.java 14d4b1788f7e44634621aa1b09ca800f716e313a30fb54da8f4e773fedf45daa
src/main/java/com/onecall/core/sharing/internal/persistence/PublicShareQueryRepository.java 7662e6beaa7b2f43b707b6e9efb8db2f2b101d2599eefa557d44509d81bc0260
src/main/java/com/onecall/core/sharing/internal/persistence/ShareAdmissionPersistenceConfiguration.java 0f6a58f384ae6236bd1878326cdb8c7a5585fa005ed5679ba3efd636a8d663ac
src/main/java/com/onecall/core/sharing/internal/persistence/ShareQueryRepository.java 83d0ff063808f9d18a92386f74c9bbabc69450af4ae0dd90ca20e39957f92030
src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceConfiguration.java 481d68d3c13ff0f734e7680d5d5b1b75ab34e1449f422aa151e18ff661920d4d
src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceProperties.java 25fcd0600902d9a75986495516733a76144eac7f9e4e651be50d8eacf9451663
src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandRepository.java 7880395403cc80de38104814d3322e168f9f5095a9bdcb2dfdb1fe2c4810bc42
src/main/java/com/onecall/core/sharing/internal/persistence/SharingPersistenceConfiguration.java 43b3a7e370a5042486f75374dab78e95057e24c00a09ebd80fc18369c0c3c283
src/main/java/com/onecall/core/sharing/internal/persistence/SharingPersistenceProperties.java d400ee193b3f25bc79b528f2ee732483abd3b143f5d798cecf19424ac267f6c7
src/main/java/com/onecall/core/sharing/internal/playback/ControlledMediaReader.java 983e51a1fd9b774d548ef11eb5a48d5d7fdd6ff1fcf011ee640e3ae581c4219c
src/main/java/com/onecall/core/sharing/internal/playback/MediaCredentialVerifier.java f4c90a4407a4deb67e9480bd362502c232eaac2be2bd4794c33c39aed5daad07
src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackConfiguration.java f92db751676b52e6dfa9e07538119671d817dc39867f259ac8e8bb636f2c6762
src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackService.java 1b3b4ef2fcc3baa9da4390a6700e979444b88e228db3463b37f27fc714efc7b6
src/main/java/com/onecall/core/sharing/internal/playback/OpaquePlaybackCredential.java dec333ab3364bd28fe577c797788b694dc752a702e1f2b78f2852f005b4e0591
src/main/java/com/onecall/core/sharing/internal/playback/PlaybackStateReader.java acf6077c3476c7573ef9ca14c2ede07a24e0beb161551ff3987ab7961a24cd81
src/main/java/com/onecall/core/sharing/internal/playback/SharingPlaybackStateReader.java 3914d3e7e6a07c173d2908b40667fb87e5223ee39330e743f1dfb29e40dd12e4
src/main/java/com/onecall/core/sharing/internal/playback/VerifiedMediaClaim.java 8fd3bbbb84ac12cbbd81027199daadc00cc5c55d98014d7e59aa1c8d00bce549
src/main/java/com/onecall/core/sharing/internal/provider/CapabilityTokenProvider.java d7eb4c18a18366b53ecf33034fca14a7fe43ca758018c4dd1bebb95701b7118a
src/main/java/com/onecall/core/sharing/internal/provider/DesignatedMobileBindingProvider.java c45bee652e212852e45c27fdf93a9930bf7b20dc10b726d755d3ebb8d22409c7
src/main/java/com/onecall/core/sharing/internal/provider/ExtractionCodeMaterial.java 1698cc785f80ccbbf9e93ad00256cf1144540a5123104ee061ebfa21a9203b20
src/main/java/com/onecall/core/sharing/internal/provider/ProviderResult.java 676cb1a795ed6ab3ab7a72a82b0059e6595ac5c77e4d18c2d7827d5eeee18641
src/main/java/com/onecall/core/sharing/internal/provider/Sha256Provider.java ec82993d4a20ec1016773130aa4e435a991741c8b582fe5ce008b569034f1aff
src/main/java/com/onecall/core/sharing/internal/provider/ShareCreationPolicyProvider.java 92946571f23cf7ce55e11e792ded92a954384a5cb687543504a6aa7e8a3a5f79
src/main/java/com/onecall/core/sharing/internal/provider/ShareExtractionCodeProvider.java d4d3653cdbf08bd666fcf34135c1b577262aa809bb67d92d65849cc9eff629b0
src/main/java/com/onecall/core/sharing/internal/provider/ShareRotationPolicyProvider.java e7188679def45e0ae076e03d201fba57b660a63d5c80240fe4f4be518c6dda95
src/main/java/com/onecall/core/sharing/internal/publicadmission/MediaAuthorizationRequest.java 35bd54e6c5e6a62f846b3ea96e1caf3762c0306f38c0bcb4fe1b150927314001
src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionCapabilityToken.java d7bb8b57b42daae56a8d76de5ef7413fd46fcde3f6b84d59d0cdfbe877b1cd3b
src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionException.java 59ed146889e158e432f7e797e278580071751cabfeb953e98a95d0fbe8d17144
src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionJsonBinder.java 82cf549b0e30c2ccbf16960bf6d822ccb2bb12e7a3316a650ab0badfcade23fc
src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionRequest.java 83d8d6a9c2f30708ad3506589597c6489ca56ccc597d9e96a7afd148160eaefe
src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionService.java f87cf252e7ea751f3c63c3d40a72861470f0a9dc7bb33ec6066842d07ecbb292
src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionView.java 9c0c1cff0d2a3dec6009c51923a9a7fa42dd81523079731b42bcab1e78e6f240
src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareMediaAuthorizationService.java 2efd95096e12e159d084f4d439944446b26c501efc943fa00d7948560012f6ba
src/main/java/com/onecall/core/sharing/internal/publicadmission/TrustedShareAdmissionCaller.java 001c62ab809b4341a8c308b8aea2c3282419c1c277f26f53407eff89e963f6c5
src/main/java/com/onecall/core/sharing/internal/publicadmission/package-info.java 01f782526937e05ae6a07cde945f516c877333b05fac9517d71771c4074885a6
src/main/java/com/onecall/core/sharing/internal/publicquery/OpaqueCapabilityToken.java 2c48379237df2735b3e34bd919239de1d4b1a6ef9a54abb75bf04e8a8993d891
src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryException.java bff003c2ce788f364173ba201a5149f9a76ecdc4d771ec1033255b0a61997537
src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java 3f5b03800d6a73afc008dd88baaded746e810d15935e11211573a6a12f47987c
src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareView.java f996561a55a27cf20e487c31ccf70dcd518f2dacb747f7fed3f220fdd434daa0
src/main/java/com/onecall/core/sharing/internal/publicquery/package-info.java ca3ec2020b9130226acbf6950db0b99b0a17f3b7172ac44338fbaecae4fef8f5
src/main/java/com/onecall/core/sharing/package-info.java aa27cf807a442eed6d6675d2740d8f22c8501663655a9a328e51d9f69926c717
src/main/java/com/onecall/core/subject_number/ModuleMarker.java 761a9b920cd979ad6496c4730541ba6594a57c58a105309204551b7b0fe5d6d0
src/main/java/com/onecall/core/subject_number/customerclaim/CustomerSubjectClaimService.java 9d3b313d7738fe0d29d240e6c9a91e842aa965d4afd187c497b753f93fa3c31d
src/main/java/com/onecall/core/subject_number/customerclaim/SubjectClaimException.java 3c90b438fee661e1a02085fb1668a67317a35163e2cf19a8d1c3b0262b615f24
src/main/java/com/onecall/core/subject_number/customerclaim/package-info.java f3c498f9f7a5d73af278a92811647ffabf69cf092d5fee4b8afcda77ea62fff4
src/main/java/com/onecall/core/subject_number/customerquery/CustomerBusinessNumberReader.java 691d5855f76762f72fb2f916c5db01243775677557b125b89b21ada9afd2fe97
src/main/java/com/onecall/core/subject_number/customerquery/package-info.java eb3926dff8bcf6a3c1bd830550a37363911883a02a74767c790503d735cf1e43
src/main/java/com/onecall/core/subject_number/customersubjectquery/CustomerSubjectAccessReader.java a18b369c1b50a484625ee5ca0a84d738c4d1fa6cda5bf623612c086deb3b070c
src/main/java/com/onecall/core/subject_number/customersubjectquery/package-info.java a672dbc578eb43b7be1cfd0972a8f33fd19c9ed82750630975018430a5435106
src/main/java/com/onecall/core/subject_number/internal/claim/DefaultCustomerSubjectClaimService.java 6c24a8dedc71644349430a4e207b9665a584d670fe422c24d07a1e61b0e5b6a9
src/main/java/com/onecall/core/subject_number/internal/claim/JooqSubjectClaimRepository.java 6131cf77dabb7e49efffd4fe60c2a487d224ca4ab6c3a350cea69f73b6f9e34a
src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceConfiguration.java a020b69003dde0df15eee9ef471ed210439c83f63a616a9f77be08b9cc51753a
src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceProperties.java bfa2bb4bcb8449e183a559bc8c4f7e341eb0dbe635fa1ea3fe5e9dfc810aac9a
src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimProofVerifier.java 54ec29c5b5a0e8ca248f792e066032409d06785e1927f5a0e30fac385909e9c0
src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimProviderResult.java 2eb58481e1656684de18f28bc363282bf927c33364346ebf450df899795c712e
src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimReferenceCipher.java 335d4f6e27731f5c3927d13460c01b6510aaf22bb0e8b2081d4232e535253aa2
src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimRepository.java 57141c0764e8250469b57fb43a7b66bb6fe70342d70609d49ba54ef98984b2da
src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimVerificationReference.java 8939b8401ecebcd3d4cb659c38971bdcdf381a88d710b7277a0caca661f50508
src/main/java/com/onecall/core/subject_number/internal/customerquery/CustomerBusinessNumberQueryConfiguration.java b3f8dac627909ec238ae13770ef13fecbc310ed2d213bbfdcac4d9dcf1198bc9
src/main/java/com/onecall/core/subject_number/internal/customerquery/JooqCustomerBusinessNumberReader.java 3336a48affd74e6aacf5597c97e25f1f6bad83ba02b7f3f79cbb56b4837f10a2
src/main/java/com/onecall/core/subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java 239cf223fe07b68ee0fadf59509c75abbe2fb98fde9080f40606bd4c9d691c2d
src/main/java/com/onecall/core/subject_number/internal/operationsread/JooqOperationsSubjectReader.java f037197285a7d27638eceaf5ad13541bf5c5465f0d1e79618005deb2313aa1fa
src/main/java/com/onecall/core/subject_number/package-info.java 7785c2b828aea48eb8c768088c04841d70201b29409eccefffcb03983fea9616
src/main/resources/application-persistence.yml 9ebff8eef793433356997bd9472c2a17f4690f490c2d1d4df0fc501f0ed82efc
src/main/resources/application-web.yml 76efd0117d412faec0e55ad6b36671f1f2fc606fbd15f1117f44d40bf7c7558c
src/main/resources/application.yml 0c048bbd592c904af067ea42c8ddd736b38543e3a7b2b3316eaff4a145bd1d15
src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql 946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045
src/main/resources/db/migration/V002__identity_organization_and_employee.sql 534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5
src/main/resources/db/migration/V003__subject_number_and_claim.sql 1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1
src/main/resources/db/migration/V004__catalog_order_and_payment.sql a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc
src/main/resources/db/migration/V005__ivr_task_session_call.sql 2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310
src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql 830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475
src/main/resources/db/migration/V007__activation_and_consent.sql bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659
src/main/resources/db/migration/V008__evidence_media.sql 2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e
src/main/resources/db/migration/V009__sharing.sql ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae
src/main/resources/db/migration/V010__sharing_role_least_privilege.sql fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343
src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b
src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql 13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828
src/main/resources/db/migration/V013__evidence_media_reader_role.sql 691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6
src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a
src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql 1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3
src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql 988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f
src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql 57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad
```

## Contract, packaged runtime, and dependency firewall

Exact commands are the plan §11 commands; the six A-stage invocations (A1
includes npm ci followed by npm run check:s3) run with Node22.23.4. Original
native checkout results: A1=0, A2=1, A3=0, A4=0, A5s1=2, A5s2=2. A2's existing
CRLF-sensitive A0 parser reports missing CHECK/column rows; A5 reports
`state table header changed in section 7.1`. Neither failure is repaired by
editing frozen input or checker source.

A disposable external LF-equivalent projection at
`/tmp/pa35-lf-validation` converts CRLF to LF only for text, leaves binary bytes
unchanged, and does not alter any workspace frozen file. All exact A1–A5
commands rerun there with locked npm ci: A1=0, A2=0, A3=0, A4=0, A5s1=0,
A5s2=0. Native logs are retained under native-validators; final projection logs
and per-command exit files are in /tmp/pa35-evidence. No semantic checker,
assertion, fixture, or contract is patched. Capacity/acceptance validators
correctly remain productionReady/production_accepted=false.

Exact commands (cwd projection root, except A1 cwd api/openapi):

```bash
cd api/openapi && npm ci && npm run check:s3
node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 --template deployment/capacity/inputs.template.json --fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s1 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s2 --matrix acceptance/v1/acceptance-matrix-v1.json --doc docs/19-production-acceptance-matrix.md --fixtures acceptance/fixtures --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures --a3-model security/v1/security-design-v1.json --a3-fixtures security/v1/fixtures --a4-calc deployment/capacity/calc.mjs --a4-fixtures deployment/capacity/fixtures --check-doc-links
```

After successful full clean verify, all three exact CI bodies run in the
accepted order, without assertion edits, using the §11 extraction algorithm.
Command:`python3 /tmp/pa35-finish-validation.py`, cwd native repository root;
each extracted body executes `bash -e -o pipefail -c <unchanged-body>` with cwd
`/tmp/pa35-lf-validation/backend`, JAVA_HOME/MAVEN_SKIP_RC/PATH as above.
Actual exits: inventory0, default/opt-in web JAR smokes0, production dependency
firewall0. Logs:`/tmp/pa35-evidence/post-full-validation.log` and
`post-full-assert-container-smoke-actually-ran.log`,
`post-full-default-and-opt-in-web-jar-smokes.log`,
`post-full-production-jar-dependency-firewall.log`; corresponding .exit files
all contain0. The native/full XML remains the sole runtime inventory.

Earlier JAR checks were launched concurrently with packaging; the first attempt
read an incomplete ZIP and each exited1. Their exact logs are retained as
`default-and-opt-in-web-jar-smokes.attempt1.log` and
`production-jar-dependency-firewall.attempt1.log`. Retrying only after packaging
completed passed both checks; the ordered post-full executions also pass.
No source/dependency/assertion change was made to turn that race into a pass.

External candidate projection includes current backend and workflow bytes,
LF-only normalized, with `backend/target` linked to the actual workspace's
full-run output. The exact existing CI bodies are extracted without edits and
run using `bash -e -o pipefail`; the firewall's own ./mvnw can execute in this
LF-only projection. This is launcher/checker portability evidence, not a
second source authority. The final source set is compared to the native tree.

Final packaged JAR:`backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar`;
SHA-256 `71f55d38bf604de61671c70891e04828c6ac9f4e506f8f7152a396646e54d095`.
The unchanged default/opt-in executable JAR startup smokes pass. The firewall
passes baseline37/34, SSM72/65,64 counterpart entries,8 dependency-only entries,
one packager entry and116 adversarial probes. Existing V018 packaged-entry
check remains exactly once. No production employee verifier, test fixture,
test class, private identity file, new dependency or relaxed firewall rule
is packaged. No live/cloud/media access is exercised by the new reads or
claimed as acceptance; existing baseline media regression tests remain intact.

Additional native package inspection uses Python zipfile.testzip() and the
packaged-entry list to confirm CRC integrity, zero fixture/test-provider/
private-identity matches, and exactly18 migration entries whose raw bytes
equal their unchanged source files, with noV019. Actual exit0; measured
results:`/tmp/pa35-evidence/package-inspection.json`.

## Runtime security evidence and capacity limits

Focused tests really migrate PostgreSQL16.15 through V018. H validates the
three existing non-inherited SET memberships, foreign-read denials42501,
Evidence four-column success/protected-column and broad-SELECT denials42501,
read-only DML/locking rejection25006, missing memberships/providers without
repair, snapshot/backend handoffs, wrong transaction/resource denial,
restoration fault rollback, contaminated backend eviction, and clean settings/
locks/catalogs/facts. H12 asserts the exact existing maximum4/3000ms/5s/
2000ms/250ms/5000ms labels; H11's temporary test-only pool size1 is reset4
before each test. This does not pretend IVR's broad unused grants are ACL-denied.

C13 exercises0/1/1000/1001/2000/3000 evidence rows with the actual adapter over
bound test facts; G19 and H3 exercise0/1/1000/1001/2000 against PostgreSQL,
assert ceil(N/1000) queries and no missing entries; H3 proves one snapshot and
whole-request failure on second chunk. G18 persists a valid direct decision
with nullable action and actual reservation/grant/balance facts. G20 persists
conflicting Evidence and IVR associations and denies the complete detail.
G21/22 mutate domain/employee facts concurrently through separate admin
connections after the read snapshot: current request stays on its snapshot,
next request observes the change/disable. G23 injects all shared codes,
actual dependency fault, unknown runtime500, and real PostgreSQL statement
sleep/timeout503, then proves recovery. G24 checks safe wire data, captured
application logs, and all business/audit/Outbox fingerprints unchanged.

EXPLAIN evidence is captured in existing G9: unfiltered/status pages may sort/
scan; subject query may use ix_inbound_session_subject_time plus tie sorting;
call/task association has its semijoin plan; decision/evidence session OR
lookups may scan. No planner-specific index choice is asserted and no index,
new migration, capacity claim or hidden truncation is introduced. These are
synthetic small-fixture plans, not production throughput proof.

## Residual owners and destinations

| Residual | Owner | Destination / boundary |
|---|---|---|
| Actual production Operations identity/secret delivery and SET proof | Platform/Database/Security | Existing current PA25 four-input SQL/runbook; preserve seven shared-login SET edges and actual production handoff/restoration proof; no PA35 grant/migration/login/pool change |
| Independent employee verifier, issuer/IdP/MFA production integration | Identity/Security | Existing production identity integration; no synthetic verifier in production JAR |
| Cursor signing/encryption key lifecycle, rotation/retirement and secret delivery | Security/Platform | Existing subject-read ring/secret custody; retained-key local tests do not establish production delivery |
| Frozen A1/A0 artifact metadata omission | Contract/Data | Metadata reconciliation backlog; artifactRoles response is authoritative, frozen x-onecall-a0-models/docs15 not rewritten |
| Session-without-call detail corruption assumption | IVR/A2 + Contract/Data | docs14 section9.13/7.3 and A2 invariant clarification; session stays listable, missing required detail call503 |
| TRACE concealment beyond activations/custom error-path alignment | Existing B-F04 Operations route-boundary owner | Later route-boundary work; S1 pins default-dispatch500 and keeps ERROR branch/shared controller unchanged |
| Snapshot cleanup/deadlines/large chains/shared-pool/index/OR-scan capacity | Backend/Database/Operations/Capacity | Local G/H proof and docs18 volume model/production EXPLAIN/load follow-up; same five-second budget and existing indexes/pool; timeout503, no cap/partial success |
| Hosted CI and independent code/aggregate/PR reviews | Controller/Test/Platform | Later authorized gates; local results are not hosted or independent acceptance |
| CRLF-sensitive existing contract checkers/WSL wrapper launcher | Test/Platform/tooling owners | Portability follow-up; source bytes unchanged, external LF-equivalent exact-body validation documented |
| Existing locked npm dependency audit/deprecation notices | Contract-tooling maintainers/Security | Separate locked-toolchain maintenance; no dependency/lockfile change in PA35 |
| Live/cloud/media and production readiness | Operations/Infrastructure | Separate acceptance; not claimed by this implementation or local synthetic evidence |

The final review's OQ-B-01 is addressed by recording both selective mirror
literals and workflow load-bearing assertions and their successful focused
and full execution. Bookkeeping parent/child provenance
remains Controller-owned and no accepted plan/review/Control text is rewritten.
No S1 authority/filter/cursor/DTO/read/cleanup behavior is assigned to a future
owner instead of being implemented now.

## Final scope / disposition

Final static command:`python3 /tmp/pa35-final-static.py`, cwd native repository
root, exit0; log:`/tmp/pa35-evidence/final-static.log`. It checks the exact
allowlist, staged diff, all26 raw/LF pairs and sorted binary pair-map digest,
V001–V018/noV019, full291-file aggregate, all461 projection files and executes
`git diff --check`. The report finalization command is
`python3 /tmp/pa35-finalize-report.py` from the same cwd, using apply_patch only.

Read-only final Git reconciliation confirms exactly30 authorized paths:
10 existing modified plus20 new, matching §7 one-for-one; no other tracked
path changes and no staged/index changes. The exact paths are listed above.
Final scope JSON:`/tmp/pa35-evidence/final-scope.json`.
All26 frozen raw/LF pairs match intake/accepted pins; V001–V018 remain
byte-identical, noV019. The measured291-path production aggregate remains
`95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0` after runtime validation.
All461 backend/workflow files in the CI LF projection match their native
source bytes modulo CRLF→LF only. No projection is source authority.
`git diff --check` exits0; existing LF/CRLF conversion notices are warnings,
not whitespace errors. There is no Docker blocker; only the exact native
wrapper/host-RC and CRLF-validator limitations documented above.
All feasible required local validations are complete with the native failures
and successful permitted equivalents distinguished; hosted CI, independent
acceptance, production identity/key delivery and capacity remain owner-routed.

No git add/stage, commit, new branch, push, PR creation/update, approval,
mark-ready, reviewer request, external comment, merge, issue mutation, later-gate
action, or Control/plan/fix/review edit occurs. The pre-existing untracked
`.codex` runner marker is preserved and excluded. Private helper scripts and
validation projections live only under /tmp, not in the source write set.
