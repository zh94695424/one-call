# PA28 Customer Subject-Detail Read Foundation — S1 Fix 1

## 1. Bounded authority and final disposition

- Work unit `PA28-customer-subject-detail-read-foundation`; current gate `implementation fix 1`; this is the sole S1-fix artifact, not a new gate or fix-2 report path.
- Documentation closer `pa28-customer-subject-detail-implementation-report-gpt-20261112-01`, 2026-11-12 (Asia/Shanghai), authors only this file and the companion S1 implementation report using the explicitly authorized shell `apply_patch`. It changes no source/test/plan/Control/CI/POM/migration/existing doc/helper.
- Evidence root `E = /mnt/d/codex-temp/pa28-implementation-evidence-20261111-02` (`D:\codex-temp\pa28-implementation-evidence-20261111-02`); final root `F = E/fix2`. Unprefixed artifact names below resolve under `F`.
- Final status **PASS — local implementation fix and report/seal closure** because fix2's focused/full execution, exact inventory and preservation passed before report authoring. Original failed/incomplete evidence remains failed/incomplete; no assertion or runner result is retroactively changed.
- `E/fix1/validation-disposition.json` and `E/fix1/repair-ledger.md` are the original outcome/repair record. `F/validation-disposition.json` reports technical PASS but blocked report/seal custody; this artifact and the companion report resolve only those missing deliverables under the current Controller instruction. Historical JSON is not rewritten.

## 2. Original failures, not acceptance

Retry3's source candidate passed its 64 focused additions, then full Surefire exposed the unlisted exact configuration assertion in `CustomerBusinessNumberQueryServiceTests.webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact`: expected declared-method size 10, actual 12. The Controller accepted `PA28-S1-IMPL-01`, expanded the cumulative implementation allowlist from 51 to 52 and authorized only that literal 10→12 change, with all eight test identities and other assertions/bytes preserved.

Fix1 retained that accepted repair and completed the previously missing four boundary documents, but its full-clean result was **exit 1**, not acceptance: Surefire 44 classes/375 tests, zero failures/errors/skips; Failsafe 19/302, **two failures**, zero errors/skips. Strict CI inventory also exited **1**. Exact failures were:

| Class | Method | Actual failed assertion |
| --- | --- | --- |
| com.onecall.archtests.container.CustomerSubjectsIT | missingUnownedForeignAccountAndForeignWorkspaceSubjectsAreUniformNotFound | expected 503, was 404 |
| com.onecall.archtests.container.CustomerSubjectsIT | pooledRoleReadOnlyIsolationAutoCommitAndAmbientTransactionsRestore | expected 503, was 200 |

The exact raw XML mismatch was independent of those failures:

- Missing planned/source identity: `(com.onecall.archtests.container.CustomerSubjectsIT, responsesProblemsAndCapturedLogsNeverDiscloseSecretsOrRowContents)`.
- Extra raw XML identity: `(com.onecall.archtests.container.CustomerSubjectsIT, responsesProblemsAndCapturedLogsNeverDiscloseSecretsOrRowContents(CapturedOutput))`.

No normalized or suffix-stripped identity equivalence was accepted. Fix1's successful contracts, JAR/firewall, checksum and preservation checks did not override either the HTTP failures or inventory mismatch. Its report-authoring/tool warning was a workflow blocker, not a product pass. The later explicit authorization of shell `apply_patch` supersedes that historical warning for this closer only.

## 3. Root causes and exact bounded repair

### PA28-S1-IMPL-02 — controller provider seam

The old test helper removed/re-registered the service bean definition and asserted `fixture.service()` returned the replacement. The actual registered controller retained its injected Spring dependency `ObjectProvider` resolution of the original service. Factory lookup identity therefore did not prove the HTTP controller used the completion-failing service: the empty branch completed normally as 404 and the present branch as 200.

The bounded test helper now obtains the actual registered `CustomerSubjectsController`, creates a `StaticListableBeanFactory` containing only the supplied `customerSubjectDetailQueryService`, obtains its typed provider, and uses `ReflectionTestUtils.setField(controller, "service", provider)` to replace that test seam. It asserts the installed field is that provider and `provider.getObject()` is the supplied service. Production controller/service/transaction behavior and dependency wiring are not changed. No production hook, relaxed expectation or bean framework redesign is introduced.

Both HTTP 503 expectations remain unchanged. The original service is restored through the same helper in `finally` on the empty-result test and in the pooled/completion-failure test. SQL/role failures, sanitation, one-connection pool checks, read-only/isolation/autocommit and ambient-transaction restoration assertions remain intact.

### PA28-S1-IMPL-03 — CapturedOutput suffix

Method-parameter injection of `CapturedOutput` caused JUnit XML to decorate the method identity with `(CapturedOutput)`, conflicting with the accepted ordinary no-argument inventory. The fix adds a test-instance `CapturedOutput output` field, injects/stores it in existing `@BeforeEach seed(CapturedOutput output)`, retains `OutputCaptureExtension` lifecycle cleanup, and makes the disclosure test no-argument. Its response/problem/log offset and sentinel assertions are unchanged; it does not stop checking logs, suppress output, rename the method, strip XML suffixes or modify CI inventory to accept a mismatch.

### Exact write scope and non-weakening proof

Fix2 changed **only** `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectsIT.java`:

- Before SHA256: `19d3ac05ef6e5251986b119578c1b3b916c0ac04c7c56f2866cc60e9d4f86aef`.
- Final SHA256: `2c7d5682335cac8384a71791274fd7ba6f3407202ade0577f73066d048a9d763`.
- Exact patch: `test-only-repair.patch`; reverse patch reproduces the fix1 hash, as recorded by `repair-delta.json`.
- `http_status_assertions_unchanged`, `original_service_finally_restoration_unchanged`, and `security_assertions_unchanged` are all true; CustomerSubjectsIT retains exactly 20 test methods. Changes are limited to output lifecycle, helper implementation and removal of the test parameter.

The accepted 10→12 literal in the separate existing baseline test is retained unchanged by fix2; reverse-literal hashing reproduces its historical baseline bytes. All eight original methods and every other assertion remain. No source/test weakening, skip, disabled/conditional test, ignored-failure option, rename, count reduction or scope expansion is used to make acceptance pass.

## 4. Prior bounded repairs and provenance

`E/fix1/repair-ledger.md` records earlier authored fixture/tool corrections, not additional fix2 edits: cast the ACL union identity to text to avoid PostgreSQL `name` truncation; mutate inherited role state to NOINHERIT rather than the original no-op INHERIT probe; use constraint-valid uppercase `PA28_` order product codes; preserve all 269 independent ACL literals and drift/rollback assertions. The initial bean-definition replacement was insufficient and is superseded by the actual-controller provider seam above.

The ledger also retains failed CRLF A0 validator attempts, external LF diagnostic, authorized docs14 LF-only repair with canonical equality, EXPLAIN classpath/product-code diagnostics and preservation-tool failures. Six final small-fixture EXPLAIN probes passed (`E/fix1/explain-attempt3.log/.exit`, exit 0), but no production-scale performance claim follows. Earlier root/retry3/fix1 evidence manifests remain sealed and independently verified by fix2 custody checks.

## 5. Final focused/full execution and exact inventory

These are already-executed fix2 results, independently recounted from the copied XML for this report, not newly run Maven suites in the documentation closer. Exact commands and exits are in each `<stem>.command.json` / `<stem>.exit`; output is `<stem>.log` under `F`. The external runner is `E/retry3/runner/mvnw`, JDK21, with the tracked wrapper unchanged.

| Stem / evidence | Result | Exit |
| --- | --- | ---: |
| focused-64-attempt1; focused-64-inventory.json | Surefire 4/34 + Failsafe 2/30 = **64 passed**; zero failures/errors/skips. | 0 |
| focused-postgres-attempt1; focused-postgres-inventory.json | Surefire 4/34 plus PostgreSQL/HTTP Failsafe 6/88 = **88 integration tests passed**, zero failures/errors/skips. | 0 |
| full-clean-attempt1; full-clean-inventory.json | `clean -Pcontainer-smoke verify`: **44/375 Surefire + 19/302 Failsafe**, zero failures/errors/skips. | 0 |
| ci-inventory-final; exact-raw-identity.json | **677 exact source=CI=raw XML identities**, scan **678**, **19** POM includes; no suffix normalization. | 0 |
| contract-validators-final | npm/OpenAPI s3, protocol s3, security s3, capacity s2, acceptance s1/s2/doc links pass. | 0 |
| jar-smoke-final; default-jar-final.log; web-jar-final.log | Default/web bounded JAR behavior passes without DB/verifier activation; JVM cleanup. | 0 |
| jar-index-final; dependency-list-final; jar-firewall-final | Exact packaged/runtime inventory and dependency/JAR firewall pass; 116 adversarial probes. | 0 each |
| prior-root-custody; prior-retry3-custody; prior-fix1-custody | Prior evidence `sha256sum -c` checks pass in corresponding roots. | 0 each |
| preservation-final; path-and-preservation.json | **613 baseline identities preserved**, 64 planned additions, **52 = 31 existing + 21 new** implementation paths, wrapper/source/docs preservation. | 0 |
| v016-checksum; v016-inventory.json | Actual Flyway checksum **1669606497**; exact eight tuples; customer34/evidence16/other219/global269/history16. | 0 |
| final-jar-sha256; final-jar-equality | Final JAR digest computed and equality verified. | 0 each |
| environment-final; blocked-custody-check | Environment capture and historical blocked-handoff custody verification pass; latter is not the new final seal. | 0 each |

The complete executable inventory is **63 classes/677 tests = 613+64**, not 678 tests. The extra annotation-line hit is a preserved historical comment. New vectors remain `[12,12,6,4]` / `[20,10]`. `full-clean-attempt1-surefire-reports/` and `full-clean-attempt1-failsafe-reports/` contain final raw XML; inherited/stale XML is not counted. Both formerly failing methods now pass with the unchanged 503 expectations; the disclosure test appears under its exact no-argument planned identity. Real transaction completion, sanitized HTTP failures, selected-workspace security, zero writes and pool restoration are proven together, not merely by source inspection.

Frozen OpenAPI and V001–V015 remain byte-identical (`frozen-final.sha256`); V016 SHA256 is `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f`. Its target ACL sets are 42→50 and global totals 261→269; those are distinct inventories. Final JAR SHA256 is `be4e0fee89357bf353b09d55b3f753846d710ac581a821f1375e2a4417854e41`. Wrapper equality, independent PA25 literals, all original historical tests and current-history checks pass. The four-field endpoint uses one composed read-only `REQUIRES_NEW` / `REPEATABLE_READ` customer transaction; none of these repairs alters its business scope.

## 6. Final accepted-finding states and hashes

| Finding | Final state | Final proof |
| --- | --- | --- |
| PA28-S1-IMPL-01 | **Fixed and verified** | Exact 10→12 literal only, eight baseline methods/bytes preserved; original configuration method passes in full Surefire. |
| PA28-S1-IMPL-02 | **Fixed and verified** | Actual-controller provider identity proof, real HTTP 503 on present/empty completion failures, unchanged security/pool/transaction checks and original-service restoration in finally. |
| PA28-S1-IMPL-03 | **Fixed and verified** | Lifecycle output injection, unchanged disclosure assertions and ordinary raw XML method identity; 677 source=CI=XML. |

| Final artifact | SHA256 |
| --- | --- |
| repair-delta.json | `da5c6ba6b2712313cb86e26c1fd671e3cc79b6d5d8a5cc2ae90d65a09e9281eb` |
| test-only-repair.patch | `8d955af48a1cf0c261d7626baef5a928546336bff2e89747c9ecc37285b8c4ac` |
| full-clean-inventory.json | `db00505182a91f01084430cc77bc409ae6d400a6b48007bdf2159ddf58359878` |
| exact-raw-identity.json | `75d68c0a5c8c5c608c452464074446a2f90f845a0628e2660985eac49cd34efd` |
| path-and-preservation.json | `ea95b762f7651bafc957129669bba633ff850b5f243ce216297fbd94de887be0` |
| v016-inventory.json | `1f8ab455f3513a00b7977b939ae9cd106954bb3a71e7b6be65d1698c55d5c950` |
| full-clean-attempt1.log | `f72495beaeeacec267bd346200bd4c1e693ee0f833062fcf94285428329c86fd` |
| focused-64-attempt1.log | `5687eb62c176199ecd65423af8514d9fa64b91a3529629aa51f0b466dcf5cf2a` |
| focused-postgres-attempt1.log | `80d24cf54254453a43b6724f000e89df24b300fa98563b139e0dad7e06ece51f` |

The companion implementation report records the full scope/access/privilege/path arithmetic, documentation decisions, command ledger and additional hashes. `F/report-closure-audit.json` records both report byte sizes, LF counts, SHA256, exact rehash of the 1,810-entry fix2 source snapshot, two-new-report-only delta, JSON parseability and report consistency/disclosure audit. `F/implementation-evidence-manifest.sha256` seals both reports and all external final/prior evidence, excluding only itself. Verify from `F` with `sha256sum -c implementation-evidence-manifest.sha256`. Neither report embeds its own digest or a circular manifest digest.

## 7. Residuals, owners and stop boundary

| Residual classification | Owner / unchanged disposition |
| --- | --- |
| Dependency security, out of scope | Dependency/Security: 17 locked npm vulnerabilities (1 low, 9 moderate, 5 high, 2 critical); no lockfile/dependency repair or security acceptance. |
| Authorization architecture | Security/Database: non-RLS application predicates, raw count/predicate-column sensitivity and repeatable-read rather than linearizable revocation remain explicit. |
| Performance | Backend/Database: six small-fixture probes do not establish production-scale/cardinality/SLO acceptance. |
| Deferred architecture/features | Backend/Architecture and Product: qualified-DSL inversion, optional number/trial/consent and claim policy remain separate work, not new PA28 behavior. |
| Tooling | Backend/CI: Mockito/Byte Buddy dynamic-agent, JVM sharing and compiler/deprecation warnings remain non-failing follow-ups, not skipped tests. |
| Repository/runner custody | Controller/CI/runtime: metadata-free export; byte-level preservation is not managed commit or outer process/JSONL/provider attestation. No Git repair. |
| Operations | Platform/Database/Security/Operations: production credentials/connections, TLS/pg_hba, deployment/rotation, exclusive administration, session invalidation/pool drain, Q17/Q23 and live acceptance are NOT EXECUTED; production readiness NOT CLAIMED. |

The historical missing-editor/report/seal blocker is resolved only for the authorized local closer; outer runner custody still belongs to Controller. No remote or later-gate mutation occurs: no stage, commit, branch, push, PR create/update, review, merge, approval, mark-ready, reviewer request, external comment, issue mutation or Control edit. Stop at bounded `implementation fix 1` report/seal completion; do not infer Code Review or production approval from **PASS**.
