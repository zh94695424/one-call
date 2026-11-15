# PA34 code-review fix 1

Task: `pa34-operations-activation-history-code-review-fix1-gpt-20261115-01`.

Work unit: `PA34-operations-activation-history-read-foundation`.

Date: 2026-11-15 (Asia/Shanghai).

Gate: author fix and validation only; not independent review, acceptance, production readiness or a later gate.

## 1. Authority and scope

The Controller PA34 section, accepted cumulative plan, implementation report and independent reviews `docs/reviews/code-review-20261115-071731.md` and `docs/reviews/code-review-20261115-075611.md` govern this fix. Dispatch HEAD is `08425f79f3bb844a39093440306f41f524e4c607`; it is not the older implementation checkpoint recorded in the historical report.

The effective implementation allowlist remains the original 23 paths plus `OperationsWebConfiguration.java`, exactly 24. This fix changes existing files within that set, updates the existing implementation report and adds only this artifact. No new production file, dependency, ordinary test identity, endpoint, field, filter parameter, verifier, migration, role or grant is introduced. Controller, accepted plan/fix artifacts, independent reviews, frozen contracts and migrations remain untouched. B-F04 remains deferred to later Operations route-boundary/contract work.

All generated validation scripts, logs and LF-checkout evidence are external under `D:\codex-temp\pa34-code-review-fix1-evidence-20261115-01` (WSL `/mnt/d/codex-temp/pa34-code-review-fix1-evidence-20261115-01`). Maven uses the unchanged authorized shim `/mnt/d/codex-temp/pa34-implementation-evidence-20261115-01/mvnw-shim.sh`; the CRLF repository wrapper is not edited. A1-A5 use only the separately authorized `D:\codex-temp\pa34-a1a5-lf-20261115-01` projection.

## 2. Accepted finding remediation

### PA34-CODE-REV-01: newline-portable frozen guards

Both existing frozen test methods now contain exactly 21 path-keyed pairs of immutable SHA-256 constants: the accepted plan's CRLF bytes and the corresponding LF Git blobs read directly from dispatch HEAD. Each actual file digest must be `isIn(...)` its own pair. The methods also check the plan manifest's exact 21 path names and original CRLF constants against that map, so neither omitted/extra entries nor a modified manifest silently changes the oracle. Expectations are not regenerated from files during tests and no input normalization weakens the raw-byte guard.

The architecture test no longer pins the plan artifact's own raw hash. It retains a regular-file assertion and the newline-independent content count of 21 manifest entries. Both tests enumerate the actual migration directory, assert `hasSize(17)` and require the exact V001-V017 filenames. Main-worktree raw frozen inputs are never rewritten.

### PA34-CODE-REV-02: configuration-aligned ERROR registration and regression protection

`OperationsWebConfiguration.operationsErrorBoundaryFilterRegistration` resolves `server.error.path` from `Environment`, defaulting to `/error`, exactly matching the existing application error controller's property/default. It rejects malformed/noncanonical servlet mapping paths with a fixed safe exception rather than broadening the mapping or silently falling back. REQUEST wildcard, dispatcher type, precedence, verifier wiring and the existing exact activation TRACE predicate are unchanged.

Existing configuration tests exercise default and non-default paths, actual servlet registration calls for ERROR versus REQUEST, unchanged order/wildcard and malformed-path refusal. Existing Web route tests dispatch ERROR explicitly with original request URI/method attributes across mounted/unmounted contexts: exact activation TRACE alone produces sanitized 404; unrelated customer TRACE, Operations subject TRACE, activation trailing-slash TRACE and non-TRACE errors chain the same request/response without rewriting the downstream body/status/header or authenticating.

The existing PostgreSQL route test and packaged CI probes exercise exact activation TRACE404 and unrelated TRACE500/INTERNAL. The packaged body runs both default and non-default error destinations and includes HEAD404 with an empty HTTP body. The existing safe JSON/header/unique-trace/no-cookie/no-credential assertions remain in force, with each probe logged individually.

Important pinned-framework detail: Spring Boot 4.1.2's `ErrorPageCustomizer` obtains the container error destination from `WebProperties` (`spring.web.error.path`), while this application's unchanged `CustomerApiErrorController` maps `${server.error.path:/error}`. Non-default real-container fixtures/probes therefore explicitly set both existing properties to `/internal/pa34-error` and assert the bound framework destination. Merely changing `server.error.path` would not move Boot's container error page. The fix does not hide that pre-existing deployment coordination requirement, change the customer controller, add a production registrar/customizer or edit production application configuration. Bytecode evidence is retained in `boot-error-path-bytecode.log`.

### PA34-CODE-REV-03: all eleven projection values

The existing exact-projection Web and PostgreSQL methods seed distinguishable V007-valid INACTIVE-to-ACTIVE IVR facts. Transition/subject UUIDs, event/reason strings, status pair, revision pair, IVR/IVR_CONFIRM channel/domain and microsecond timestamp are all asserted individually against source facts. The Web fixture uses revisions 17/18; PostgreSQL uses valid persisted revisions 1/2 and independently reads the seeded subject UUID with a prepared query. Both methods still assert the exact eleven keys, three page keys, singleton item count, integral revisions, null tail cursor and false `hasMore`. Both ADMIN and OPS exercise the real persistence projection. No mapping production code changes.

### PA34-CODE-REV-04: stronger existing assertions

Every formerly broad PA34 `Failure` type-only assertion now also requires the exact safe failure code, including per-stage cleanup errors. Known route variants require their exact observed 404 status and safe NOT_FOUND envelope instead of accepting either 400 or 404; no-data/no-auth-challenge conditions remain explicit. No connector or production route policy is relaxed to obtain that status.

Self-referential assertions on the test-created transaction template are removed. The existing configuration identity now obtains the actual production `operationsSubjectReadTransactionTemplate` bean using a mock transaction-manager resource, without instantiating a pool, and asserts its manager binding, read-only flag, REPEATABLE_READ, REQUIRES_NEW and five-second timeout. Existing source checks remain supplemental. Actual filesystem listings have hard counts for the four activation implementation files and 17 migration files, plus existence-based counting of the eight approved new production paths.

## 3. Validation results

All results below are from this fix's external evidence directory, not the historical implementation directory. Runs use Temurin 21.0.13, Maven 3.9.9, Node 22.23.4, Docker 29.1.3 and real PostgreSQL 16.15 (`postgres:16`). Version/image evidence is `versions.log`. Final documentation update: 2026-11-15T08:54:55+08:00.

| Required validation | Actual result | Evidence |
| --- | --- | --- |
| Focused PA34 unit/boundary | Six classes, 60 tests, zero failures/errors/skips | `focused-unit-final.log` |
| Focused two-IT container verify in the mixed-EOL main worktree | 60 Surefire +28 Failsafe; zero failures/errors/skips; exit0 | `focused-container-aligned.log`, `.exit`; `focused-surefire-xml/`, `focused-failsafe-xml/` |
| Exact focused identities | Eight classes; exactly the accepted 60+28 identities, no duplicates or extras | `focused-xml-summary.log` |
| Accepted focused/adjacent boundary command | 113 tests; zero failures/errors/skips | `focused-adjacent-boundaries.log` |
| Canonical Linux/LF frozen guard rerun | Existing architecture identity1 + existing PostgreSQL identity1; zero failures/errors/skips; exit0 | `lf-frozen-final.log`, `.exit`; `lf-projection.py`, `linux-lf/` |
| Full clean container-smoke verify | 76 suites/704 Surefire +31 suites/504 Failsafe =107 suites/1208; zero failures/errors/skips; exit0 | `full-clean-verify.log`, `.exit`; `full-surefire-xml/`, `full-failsafe-xml/`, `full-xml-summary.json` |
| Exact current CI inventory body | Source=CI=POM=XML; token1209/POM31; exact unchanged PA34 88 identities; exit0 | `exact-ci-bodies.log`, `.exit`; `accepted-identities.json` |
| Exact current CI JAR body | Default non-web startup/exit plus 60 HTTP probes for each of `/error` and `/internal/pa34-error`; all120 pass; no pool/verifier adapter; bounded JVM cleanup; exit0 | `exact-ci-bodies.log`; `boot.log`, `web-boot.log`, `web-boot-custom-error.log` |
| Exact current CI dependency firewall body | Runtime72/packaged65, counterparts64, dependency-only8, packager1; all116 adversarial probes pass; exit0 | `exact-ci-bodies.log`; `jar-index.txt`, `pa11-runtime-dependencies.txt` |
| Exact accepted A1-A5 section9.3 command block | Every command passes in the authorized LF projection; aggregate exit0 | `contracts-a1-a5.log` |
| Main raw frozen inputs, migration inventory and report-inclusive content scope/hygiene | All21 accepted raw hashes; exact V001-V017/no V018; authorized scope only; UTF-8/final newline/no BOM or replacement characters/conflicts/trailing whitespace; unchanged private helpers | Timestamped `final-audit.log`; `baseline.json`, `final-audit-complete.py` |
| Git hygiene | Dispatch HEAD and worktree pointer preserved; no staged delta; **index raw-byte preservation not established** because two different index digests are observed | `final-audit.log`, `.exit`; `final-audit-metadata-failure.log`; `index-read-only-investigation.log` |

The authoritative full clean run started at 2026-11-15T08:23:06+08:00, finished at 2026-11-15T08:52:31+08:00 and reports 29:24 minutes. The selected final LF guards finished at 08:27:38+08:00. Full XML is retained before running the extracted CI bodies; focused and LF runs do not replace the full XML proof. The three CI bodies are extracted verbatim from the modified workflow, syntax-checked and run with Bash errexit/pipefail in `backend`; their exact body hashes and individual exit0 results are logged.

Exact Maven invocations (from `backend` after sourcing the external shim):

```bash
./mvnw -B -ntp -Dtest=OperationsActivationReadWebTests,OperationsActivationReadConfigurationTests,OperationsActivationReadRequestTests,OperationsActivationCursorCodecTests,OperationsActivationReadServiceTests,OperationsActivationReadBoundaryTests test
./mvnw -B -ntp -Dtest=OperationsActivationReadWebTests,OperationsActivationReadConfigurationTests,OperationsActivationReadRequestTests,OperationsActivationCursorCodecTests,OperationsActivationReadServiceTests,OperationsActivationReadBoundaryTests,ModuleBoundaryTests,OperationsSubjectReadBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadWebTests,OperationsSubjectsWebTests test
./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsActivationReadWebTests,OperationsActivationReadConfigurationTests,OperationsActivationReadRequestTests,OperationsActivationCursorCodecTests,OperationsActivationReadServiceTests,OperationsActivationReadBoundaryTests -Dit.test=OperationsActivationReadsIT,OperationsActivationReadRoleLeastPrivilegeIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

The first command isolates the60 identities; the next three are the accepted section9.1 test commands. The final LF-only selected guard command runs in external `linux-lf/backend`:

```bash
./mvnw -B -ntp -Pcontainer-smoke -Dtest=OperationsActivationReadBoundaryTests#frozenInputsAndSeventeenMigrationInventoryRemainByteIdentical -Dit.test=OperationsActivationReadsIT#postgres16UsesExactlyFrozenSeventeenMigrations verify
```

The LF projection combines the dispatch Git archive with the current authorized implementation files, with all21 frozen files checked byte-for-byte against their LF Git blobs. Main frozen files retain their original raw CRLF hashes. For example docs/14 remains `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` in the main worktree and `bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58` in the LF projection. The pair manifest is in `frozen-dual-oracle.log`.

The final main JAR SHA-256 is `f6b49f1ea912f8aa68600fe7b29669c81cd1c7ef98b0b8754f7ff476ab34b296`. Per error destination, packaged exact activation TRACE returns404/NOT_FOUND, HEAD returns404 with no HTTP body, and customer TRACE, Operations subject TRACE and activation trailing-slash TRACE each return500/INTERNAL. Exact safe envelope/header/no-data/no-auth-success checks remain active. Malformed path variants have exact404 expectations rather than an accepted400-or404 range. The default non-web process exits itself; web probe processes use bounded terminate/wait with kill fallback in `finally`. These are local executions of the real CI bodies, not a hosted Actions claim.

Earlier failed attempts are retained, not counted as success: a test helper compile error, one Maven invocation outside `backend`, overly broad guessed connector400 expectations corrected to observed exact404, and a custom error destination with only the controller property set. The last failure exposed the pinned Boot property distinction documented in section2; the final fixture/probes coordinate both existing properties. An initial CI-body indentation error and an external audit's treatment of a benign autocrlf warning were corrected before final passing runs. The index digest discrepancy was not repaired or suppressed. Read-only measurements alternate between the baseline and another digest despite no staged delta; a single matching intermediate measurement is not treated as proof of byte preservation. Final audit records content/frozen results separately and retains strict index preservation as a failing hygiene check; no baseline rewrite, index restoration or Git-state repair is performed.

## 4. Residuals and boundaries

- Index-byte hygiene discrepancy: baseline SHA-256 is `e23cd2e7c9d2ed4bec9dd0442c9bf48057bc0ec88fa33833a9b554dd90fe86c4`; another observed digest is `6927b28ee7ccdf4bdff73bc8e7d113dfc67791e4b6aac872bda13b94e6ab7e17`. Read-only `git diff --cached --name-only` remains empty and HEAD/pointer match the dispatch baseline. Cause is not established; no actor attribution or metadata repair is claimed. Controller/tooling owner must adjudicate the byte-only discrepancy; final handoff is not all-green hygiene.
- B-F04 remains deferred-with-owner to later Operations route-boundary/contract work. Unrelated TRACE errors retain their established sanitized 500 mapping; this fix does not extend activation concealment to sibling routes or centralize app-wide route literals.
- Non-default deployment error destinations must coordinate the unchanged controller property with Boot 4's container error-page property. Owner: Platform/Backend deployment configuration. Defaults coincide at `/error`; no production config change or production-readiness claim is supplied.
- Independent production verifier/session issuance/MFA/provisioning, runtime read-role credentials and shared cursor-ring custody/rotation remain external Security/Operations IAM/DBA prerequisites, with existing fail-closed behavior.
- Global ordering still lacks a matching V007 global index; sort/timeout capacity remains Backend/DBA-owned. Domain-impossible persisted facts still fail the entire page without repair. No migration or grant is authorized.
- Repeatable-read revocation remains snapshot-relative; continuation pages have fresh snapshots, not linearizable revocation or a pinned cross-page snapshot.
- A1-A5 execute in the authorized LF projection, not raw Windows bytes; hosted GitHub Actions and live production acceptance are not claimed. Existing npm development-tool vulnerabilities remain separately owned and are not repaired here.

## 5. Controller handoff

Author remediation is complete for PA34-CODE-REV-01 through PA34-CODE-REV-04, and all required test/runtime/contract/inventory checks pass. The final audit retains the unresolved index-byte hygiene discrepancy rather than claiming every validation is green. This is a fix report, not independent finding acceptance or self-review. Controller decides how to resolve that hygiene residual and whether to enter a subsequent gate.

Exactly nine existing code/CI paths changed relative to the dispatch worktree snapshot:

- `.github/workflows/ci.yml`
- `backend/src/main/java/com/onecall/core/operations_iam/internal/web/OperationsWebConfiguration.java`
- `backend/src/test/java/com/onecall/archtests/OperationsActivationReadConfigurationTests.java`
- `backend/src/test/java/com/onecall/archtests/OperationsActivationReadWebTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
- `backend/src/test/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadServiceTests.java`

The existing implementation report is updated and this sole fix artifact is added:11 changed paths total, with the ten existing paths inside the effective24-path allowlist. No POM/dependency or test-identity delta is introduced. All other baseline content files, all21 private-helper files, frozen inputs and accepted artifacts remain unchanged; the worktree `.git` pointer and dispatch HEAD are preserved and nothing is staged. Index-byte preservation is the separately disclosed unresolved hygiene residual. Baseline comparison includes inherited untracked implementation files rather than relying only on `git diff`.

No staging, commit, branch creation, push, PR/comment, approval, review request, self-review or re-review is performed. Stop for Controller intake. No production readiness is asserted.
