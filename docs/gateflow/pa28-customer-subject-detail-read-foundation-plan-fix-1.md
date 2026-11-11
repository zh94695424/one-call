# PA28 Customer Subject-Detail Read Foundation — Plan Fix 1

## 1. Authority and bounded result

- Task: `pa28-plan-fix1-gpt-20261111-01`; date: 2026-11-11; work unit: `PA28-customer-subject-detail-read-foundation`; current gate: `fix`.
- Authority: active PA28 checkpoint and seven Controller-accepted findings in `docs/gateflow/project-execution-control.md`; independent inputs `docs/reviews/plan-review-20261111-220606.md` and `docs/reviews/plan-review-20261111-222437.md`.
- Revised cumulative candidate: `docs/gateflow/pa28-customer-subject-detail-read-foundation-plan.md`. The pre-fix candidate is the reviewed SHA256 `7d7700ae39074ecd5c5d7aa57e7d695f135688f28721115af871081f9e900aa0`.
- Result: all seven accepted findings are **fixed candidates in the plan**, not independently accepted fixes or passing implementation results. Exactly two repository files are writable for this task: the cumulative plan and this new artifact. No source, tests, migration, OpenAPI, existing documentation/review, Control or Git metadata is edited. No implementation, gate advancement or re-review is performed.

## 2. Accepted findings and exact candidate deltas

| Accepted finding | Severity / status | Exact cumulative-plan delta |
| --- | --- | --- |
| `PA28-PLAN-REV-01` | High / fixed candidate | §6.3 adds `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`. In `sharingQueryApiAndProjectionAreExact`, retain exact `containsExactlyInAnyOrder` and the six old entries, appending `subject_number::customer-subject-query`, `evidence::customer-subject-count-query`, `catalog_order::customer-subject-count-query` for nine. All other assertions and eight existing method names remain. §9's focused Surefire command now includes this existing class. |
| `PA28-PLAN-REV-02` | High / fixed candidate | §6.3 adds `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java`. `postgres16FullChainAndCustomerRuntimeRoleAreUsed` changes only its latest-chain successful Flyway history count string `"15"`→`"16"`; all eighteen names and other behavior remain. No new CI identity or POM include. |
| `PA28-PLAN-REV-03` | Medium / fixed candidate | §§4.5/6.3 explicitly preserve the one-way actual graph and update `ModuleBoundaryTests.discoveredProductionEdgesStayInsideTheConfiguredMap` to exact six-element flattened multiset `"sharing", "evidence", "subject_number", "activation_consent", "evidence", "catalog_order"`. Repeated evidence is intentional (sharing→evidence and customer→evidence). Retain exact multiset matching, per-module guard, both nine-entry named-interface assertions, negative fixtures, all names and cycle-free `ApplicationModules.verify()`. Catalog's existing allowed declarations stay unchanged; no actual catalog→customer edge is introduced. |
| `PA28-PLAN-REV-04` | Medium / fixed candidate | §6.3 now names both `PostgresContainerIT` surfaces. `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`: `all()` 15→16; `applied()` remains 13; V016 has exactly one entry, actual V016 filename, `ABOVE_TARGET` and not applied, retaining V014/V015, pending-empty, exact SUCCESS and zero rerun assertions. `assertV009LiveFence`: append the actual V016 filename after V015 in the exact ordered sixteen-script list. Preserve the separate applied-only history list ending at 013 and all 64 methods. No checksum is invented. |
| `PA28-PLAN-REV-05` | High / fixed candidate | §§3.1–3.3/4.2–4.3 select `Optional<CustomerSubjectDetailQueryService.Detail>` as the service and transaction result. Only empty ownership after lifecycle yields empty, with no risk/activation/evidence/order calls. After successful transaction completion the controller calls existing `CustomerProblemWriter.notFound(response, CustomerBoundaryFilter.trace(request))`; add only the response argument and `throws IOException` to the planned handler. Present results unwrap to the four-field record. Null owner Optional, mismatched UUID and null service/transaction results are INTERNAL/500, never 404; completion failure is sanitized 503 on both present/empty paths. No enum extension, writer change or extra source file. §§7.1/7.2/7.5 pin web/service/real-IT tests and downstream non-invocation. |
| `PA28-PLAN-REV-06` | Low / fixed candidate | §6.5 replaces nonexistent root `README.md` with existing `backend/README.md` and uses that path in the boundary instruction. Four documentation paths remain; creating a root README is not authorized. |
| `PA28-PLAN-REV-07` | Medium / fixed candidate | §§3.1/3.2 remove all arbitrary-query/body rejection and body-presence inspection. Extra/duplicate query selectors and fixed/chunked JSON/non-JSON GET bodies are ignored, matching `/me`, and cannot alter verified scope, projection or outcome. Only malformed path UUIDs yield 422 after authentication; extra path segments, trailing slash, unsupported methods and closed URI forms yield 404. §§7.1/7.5 rename and clarify the affected new tests without changing their counts. |

### 2.1 Test-manifest replacements and 404 coverage

These are one-for-one replacements of proposed new method names, not renames of existing source methods:

| New class | Previous candidate name | Revised literal name |
| --- | --- | --- |
| `CustomerSubjectsWebTests` | `unsupportedMethodsAndNoncanonicalRoutesRemainNotFound` | `unsupportedMethodsExtraSegmentsAndTrailingRoutesRemainNotFound` |
| `CustomerSubjectsWebTests` | `malformedAndAbbreviatedSubjectIdsReturnValidationFailed` | `malformedAbbreviatedAndDottedSubjectValuesReturnValidationFailed` |
| `CustomerSubjectsWebTests` | `querySelectorsAndNonemptyBodiesAreRejectedWithoutServiceCalls` | `extraQueryParametersAndGetBodiesAreIgnoredWithoutChangingScope` |
| `CustomerSubjectDetailQueryServiceTests` | `missingOrMismatchedOwnedSubjectStopsBeforeRiskAndCounts` | `missingOwnedSubjectIsEmptyAndInvalidProjectionIsInternal` |
| `CustomerSubjectsIT` | `cursorlessValidationAndExistingCustomerEndpointsRemainIndependent` | `cursorlessUuidValidationAndIgnoredInputsPreserveExistingCustomerEndpoints` |

Existing proposed `subjectNotFoundForbiddenAndInternalUseExistingProblemEnvelope` now explicitly checks empty→existing writer/404, present→unwrapped four fields, null→500. The service's revised ownership method proves ordered lifecycle/ownership reads and zero downstream calls on empty/invalid projection; `sqlTransactionAndBeanFailuresAreRedactedAndUnknownFaultsAreInternal` covers null transaction 500 and completion failures on both branches. `missingUnownedForeignAccountAndForeignWorkspaceSubjectsAreUniformNotFound` now proves the real writer envelope and no downstream SQL/reader calls, including foreign subjects with OPEN risk, and sanitized completion failure instead of premature 404. Existing lifecycle-before-ownership and OPEN-before-count tests retain precedence.

### 2.2 Closed review questions

- The filter may admit a single segment for authentication/validation, but validation accepts only a full canonical hyphenated UUID (uppercase hexadecimal remains accepted). `<uuid>.json` is malformed 422; `<uuid>/json` is extra-segment 404. There is no extension-stripping or UUID abbreviation acceptance.
- Preserve the actual acyclic one-way module graph even though catalog already declares customer among its allowed dependencies. Do not narrow catalog's declaration, add reverse imports or bypass Modulith verification.
- Future test instructions use the **actual V016 filename**, planned as `V016__customer_subject_detail_read_privileges.sql`. The actual final Flyway checksum must be independently sealed during implementation, never guessed or invented here.
- The focused `-Dtest` command includes `SharingQueryBoundaryTests` for early ripple detection; exact CI/source/XML identity rules and totals remain intact.

## 3. Recomputed inventories

### 3.1 Exact future implementation path allowlist

| Category | Before fix | Revised candidate |
| --- | ---: | ---: |
| Production/configuration (§6.1) | 21 | 21 |
| New test/fixture paths (§6.2) | 7 | 7 |
| Existing test/fixture paths (§6.3) | 17 | 19 |
| Existing documentation paths (§6.5) | 4 | 4 |
| Distinct future implementation paths | **49** | **51** |

Revised split: **21 new + 30 existing = 51**. New: thirteen production Java files, one V016 migration, seven test/fixture paths. Existing: four production Java files, three ops/build/CI files, nineteen test/fixture paths, four docs. README is a corrected existing path, not an extra path. The cumulative plan and this fix artifact are not part of that future implementation allowlist; they are the two current-task writes.

### 3.2 Exact tests, CI and POM

| Inventory | Baseline | Addition | Required candidate target |
| --- | --- | --- | --- |
| Surefire | 40 classes / 341 methods | 4 / 34 (`12+12+6+4`) | **44 / 375** |
| Failsafe | 17 classes / 272 methods | 2 / 30 (`20+10`) | **19 / 302** |
| Total | 57 / 613 | 6 / 64 | **63 / 677** |
| Annotation-line scan | 614 | 64 | **678** |
| Explicit Failsafe POM includes | 17 | 2 | **19** |

No test-count change results from this fix. The two newly authorized paths already belong to the baseline: eight Sharing boundary methods and eighteen business-number IT methods. All 613 existing identities, PostgresContainerIT's 64 methods and historical inventories remain unchanged. §8 requires the revised §7 names as the literal PA28 addition sets, with no obsolete aliases. Customer-internal file target remains 32→34; privileges remain customer26→34/evidence16/other219/global261→269, delta eight, current history15→16.

## 4. Validation performed

Read-only repository checks, not implementation acceptance:

- Read the active Control goal/fix adjudication and both reviews; inspected the named architecture assertions, business-number fixture/IT history, Postgres history/helper/callers, existing `/me` controller, boundary filter, problem writer, exception enum and PA27 query-service transaction pattern. Checked the owner descriptors, named persistence/migration predicates, OpenAPI operation/schema, design ownership/security sections, CI inventory and POM includes. No external lookup was needed.
- Parsed §6 path bullets with explicit production/test bases: category vector **[21,7,19,4]**, exactly **51 unique paths**, **21 not yet present / 30 existing**. Confirmed `backend/README.md` exists. The two previously omitted test paths are now explicit.
- Parsed all six §7 method manifests: **[12,12,6,4,20,10]**, unique within each class, total **64**. Recomputed 341+34=375, 272+30=302, 375+302=677, 614+64=678, 17+2=19. The five clarified names replace their old candidate identities one-for-one.
- Evaluated only the CI's inventory-construction AST statements (not report/XML execution) and compared every per-class method set to local source: **40/341 Surefire + 17/272 Failsafe** match exactly. Confirmed existing Sharing boundary8/business-number IT18/Postgres64, annotation-line scan614 and seventeen POM IT includes. The first read-only checker attempt stopped on an omitted `Path` binding while selecting too many AST nodes; narrowing selection to inventory construction succeeded. This was a checker error, not a repository test failure.
- Mechanical assertion sweep: `getAllowedDependencies` and customer module lookups identify customer expectations at `ModuleBoundaryTests:121–122,219–220`, `SharingQueryBoundaryTests:113` and `CustomerBusinessNumberBoundaryTests:46`; all are covered in §6.3. `SharingQueryBoundaryTests:463` is the separate unchanged sharing→evidence assertion. The exact configured map and flattened production edges at `ModuleBoundaryTests:40,99–114` are covered, not collapsed into a weaker set assertion.
- Mechanical `info().all()` sweep has five hits, all in `PostgresContainerIT`: history size/filter/state assertions at 1396/1398/1404/1410 and the independent script list at 10788. §6.3 covers both logical surfaces. Its applied-only history list at 10771–10787 remains separate and frozen through 013. Confirmed helper callers at 2535/2588/2672/3095 are the four actual method names recorded in the plan.
- Swept literal fifteen-size/history/migration assertions and V015 script references: latest-chain changes remain bounded to §6.3; the PA27 historical fixture is explicitly handled by §6.4. Unrelated projection/column counts (including EvidenceMediaReadRoleLeastPrivilegeIT's selected-column count) are not migration counts. No historical V015 checksum is replaced with a fabricated V016 value.
- Compared repository file-content SHA256 snapshots taken before edits and after the fix: only the cumulative plan changes and this single artifact is added; no file is deleted. Frozen OpenAPI, V001–V015, Control, both review artifacts and all implementation inputs retain their pre-fix content. Git status could not run because the existing worktree pointer names an unavailable Windows Git directory; no repair or Git metadata edit was attempted.
- No Maven, Docker/PostgreSQL, contract-validator, jar smoke or XML acceptance execution was performed. All future commands and post-implementation totals remain requirements, not passing results.

## 5. Residuals and completion boundary

- All seven accepted findings have fixed-candidate text; independent verification/disposition remains with Controller. No approval, mark-ready, reviewer request, external comment, stage/commit/push/PR/merge, implementation or re-review is performed.
- Actual V016 content/checksum, real transaction/HTTP/ACL behavior and full CI/XML acceptance remain future implementation evidence, not plan-fix output. The Git pointer prevents normal local diff/status evidence; content snapshots provide this task's bounded-write evidence without changing Git.
- Existing §10 residual ownership is unchanged: Security/Database retains non-RLS/snapshot and predicate-column limitations; Backend/Database retains representative performance measurement; Backend/Architecture retains qualified-DSL inversion; Product owns deferred optional fields; Database/CI owns final checksum custody; Controller/CI owns managed Git/newline/XML custody; Platform/Database/Security/Operations retain credentials, TLS/pg_hba, exclusive administration, session/pool invalidation and Q17/Q23. None is promoted into a PA28 goal.
- Completion: cumulative candidate updated comprehensively and exactly one fix artifact added. Stop at bounded gate `fix`; production execution **NOT EXECUTED**, readiness **NOT CLAIMED**.
