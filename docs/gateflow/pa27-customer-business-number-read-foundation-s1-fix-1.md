# PA27 customer business-number read foundation: S1-fix-1

- Task: `pa27-business-number-implementation-fix1-gpt-20261111-01`.
- Work unit: `PA27-customer-business-number-read-foundation`; sole accepted S1.
- Date: 2026-11-11, Asia/Shanghai.
- Snapshot: `/mnt/d/codex-temp/pa27-implementation-fix1-20261111-01`, metadata-free.
- Authority: current Control adjudication `PA27-S1-IMPL-01`, accepted cumulative plan and the user's bounded fix instruction. No Control or historical review/fix artifact edit is authorized.
- Durable evidence: `D:\codex-temp\pa27-implementation-evidence-20261111-03` (`/mnt/d/codex-temp/pa27-implementation-evidence-20261111-03`).
- Disposition: `PA27-S1-IMPL-01` closed for this author task; final-source clean verification and unchanged exact source=CI=XML checker pass. This author artifact is not approval, review entry, deployment or production acceptance.

## 1. Finding disposition and exact plan correction

`PA27-S1-IMPL-01` is fixed. The original S1 report's `PA27-S1-VAL-01` is the same omitted test-oracle ripple. Existing `SharingQueryBoundaryTests.sharingQueryApiAndProjectionAreExact` now requires exactly:

```text
sharing::customer-query
sharing::customer-command
sharing::public-query
sharing::public-admission
subject_number::customer-business-number-query
activation_consent::customer-activation-query
```

`containsExactlyInAnyOrder` remains exact. Every other logical character, assertion and behavior in the file is preserved; strict UTF-8/LF is retained in the edited output. Eight ordinary test methods remain. `static-audit.py` reconstructs the final file from the intake file using only this literal replacement and asserts full logical-text equality. No assertion is weakened, removed, disabled, renamed or skipped.

Only these cumulative-plan sections change:

| Section | Bounded correction |
| --- | --- |
| §7 exact implementation allowlist arithmetic | 49 = 26 M + 23 N; production 18 (4 M/14 N), database/tests/CI 26 (18 M/8 N), docs 5 (4 M/1 N) |
| §7.2 database/tests/CI paths | Add exactly `M backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` |
| §8 assertion manifest preamble | Specify exact four-to-six expectation, retain eight methods and every other assertion/behavior |

Test targets remain **40/341 + 17/272 = 57/613**, annotation-line scan **614**, Failsafe includes **17**. The cumulative plan correction and this fix artifact are separately authorized outputs, outside the 49-path S1 implementation allowlist. Historical plan-fix-1 and four reviews are preserved, not rewritten to pretend they originally included the missing path.

## 2. Validation-discovered corrections within existing authorization

Restoring real execution and the exact XML checker exposed four defects in the prior candidate, all in paths already present in the accepted 48-path implementation allowlist. No new authorization/path is introduced:

| Existing path | Observed failure and minimal correction | Preserved contract |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/CustomerBusinessNumbersWebTests.java` | Repeated `when(mock.call()).thenThrow(...)` invokes the previously throwing stub during restubbing. Use `doThrow(...).when(service).listCustomerBusinessNumbers(...)` for both repeated exception stubs | Same twelve methods, 503/500 and sanitization assertions, real HTTP, no production change |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumberReadPrivilegeIT.java` | Runtime-authority mutation makes the catalog-snapshot helper itself fail on `pg_authid` before invoking V015. Capture the effective role, reset only for before/after catalog inspection, restore it before the migration, and retain the same transaction/savepoint/rollback assertions | All eight methods and drift mutations; migration still executes under the denied runtime role; complete catalog equality remains exact; no catalog grant added |
| `backend/src/main/java/com/onecall/core/subject_number/internal/customerquery/JooqCustomerBusinessNumberReader.java` | Real continuation requests returned 503. Disposable diagnostic reproduced PostgreSQL `timestamp with time zone < character varying` from the bound OffsetDateTime seek values. Add explicit `CAST(? AS timestamptz)` to both timestamp placeholders | Same bound values, microsecond anchors, tenant predicate, DESC/native UUID ASC keyset, limit+1, read-only privileges and projection; no new API or schema |
| `backend/src/test/java/com/onecall/archtests/container/CustomerBusinessNumbersIT.java` | First clean verify passes all613 cases, but exact CI rejects the XML name suffix `(CapturedOutput)` on the log-canary method. Move CapturedOutput injection to the existing BeforeEach method and retain it in a field, following existing gateway/playback IT patterns | All eighteen test names and all assertions remain; the log-canary test still inspects the actual captured output; exact CI checker/XML identity requirements unchanged |

The external diagnostic initially lacked Flyway on the Surefire classpath; that failed attempt remains recorded. Rerunning with the actual Failsafe classpath reproduced the SQL type failure, and compiling the final reader externally then returned two continuation rows. Diagnostic code is outside the repository and never substitutes for the checked-in test/CI inventory or final clean build.

Relative to fix intake, only eight paths change: the cumulative plan, sharing boundary test, four existing authorized paths in the table, existing implementation report and this new artifact. The other 43 S1 delivery paths remain byte-identical to fix intake. No OpenAPI, V001-V014, Control, plan-fix-1, review, dependency, CI or POM bytes change in this fix.

## 3. Execution environment and custody

Authorized danger-full-access permits Docker sockets, real servlet HTTP, Node child processes and durable external writes. `/opt/jdk21` is Temurin 21.0.13+9; the Maven wrapper resolves 3.9.9. The protected checked-in wrapper has CRLF, so an external LF execution copy under evidence `wrapper/`, with unchanged pinned distribution properties, runs against this backend. The repository wrapper bytes and mode remain unchanged. No test skipping, failure ignoring, Mockito preload, assertion suppression or validator determinism bypass is used.

Node is v22.23.4. Docker server is 29.1.3. `postgres:16` executes PostgreSQL 16.15 (Debian 16.15-1.pgdg13+2), digest `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`. Disposable synthetic databases only; no production credential/database is used.

Each validation stem retains its exact `.command`, `.log`, `.exit`, `.started` and `.finished` files. `baseline.json` captures intake hashes/modes; `frozen.sha256` covers OpenAPI, fourteen migrations and actual intake Control (`66a831d997f08a7594ce5f9294ba5a24f27bd2c5c5ded3779f690b85e4763e05`); `protected.sha256` covers historical plan-fix/reviews. `intake/` preserves the original blocked report. That historical run's lost `/tmp` logs are not claimed recovered.

API commands run in an external byte-identical contract workspace to keep npm/generated output outside the delivery tree. API/A2/A3/A4/A5 commands are extracted unchanged from checked-in CI; the exact inventory and JAR Python blocks are dedented without assertion edits and run against this backend's fresh outputs. Local CI-block execution is not a claim that a GitHub-hosted workflow was run.

## 4. Exact validation and results

Final-source validation is complete. All commands below have durable `.command`, `.exit`, `.log`, `.started` and `.finished` files under the requested evidence root. The implementation report §7 lists selections, representative full commands and all relevant earlier failed attempts.

| Evidence stem | Exit / exact observed outcome |
| --- | --- |
| `focused-unit-rerun` | 0; Web/service/cursor/boundary/module/configuration eight classes,58 cases, zero failures/errors/skips |
| `focused-postgres-rerun` | 0; Surefire341 plus selected PostgreSQL122: endpoint18, privilege8, PA25 twenty, PA26 twelve, baseline64; zero failures/errors/skips |
| `focused-inventory-repair`, `exact-ci-focused-repair` | 0 each; endpoint18 with lifecycle-captured logs and unchanged exact XML checker pass |
| `final-clean-verify-rerun` | 0; `/opt/jdk21` + pinned Maven wrapper3.9.9, `-B -ntp clean -Pcontainer-smoke verify`; Surefire40/341 and Failsafe17/272, zero failures/errors/skips |
| `exact-ci-inventory-rerun`, `measured-inventory-rerun` | 0 each; unchanged complete source=CI=XML check passes57/613, scan614, seventeen includes, all historical checkpoints; JSON identities and copied XML in `reports/final/` |
| `jar-smokes-rerun`, `jar-firewall-rerun` | 0 each; final JAR default/opt-in web checks and dependency firewall/116 adversarial probes; JAR hash, boot logs, runtime dependencies and index preserved |
| `api-contract` | 0; `npm ci && npm run check:s3`, frozen49 operations and generation/types/doc mapping/media lint pass |
| `contract-a2-s3`, `contract-a3-s3`, `contract-a4-s2` | 0 each;25 protocol fixtures,23 security fixtures,113 capacity cases; productionReady/procurementReady/cloudVerified/a3RiskClosed remain false |
| `contract-a5-s1`, `contract-a5-s2` | 0 each; deterministic=true, runtime_evidence=NOT_RUN, production_accepted=false; no live acceptance claim |
| `contract-replica-final` | 0;1152 contract inputs raw-byte equal, hash inventory retained; only task-specific plan/report/fix docs excluded |
| `static-audit-final`, `scope-audit-final`, `report-inventory-final`, `frozen-raw-after-reports` | Final sealing checks: exact49 arithmetic/assertion replacement, seven modified+one new fix path, preserved existing modes/test names/assertion counts, UTF-8/LF,48 non-self report hashes, frozen/protected raw bytes |

Initial `focused-unit` exits1 (58 cases, one repeated-stubbing error); `focused-postgres` exits1 (six continuation failures and one catalog-helper error). First `final-clean-verify` exits0 for all613 cases, but `exact-ci-inventory` and `measured-inventory` exit1 for the log-capture parameter suffix. These outputs remain separate from successful final reruns, with original XML in focused report directories and `reports/final-clean-first/`. No failed result is overwritten or labeled an inventory pass. External diagnostic classpath/type-error attempts and preliminary scope-audit failures are retained too. A disposable three-byte generated build metadata file was copied externally and removed; sharing CRLF/mixed EOL was normalized to required LF without further logical edits. No unlisted final source file is delivered.

Final `allowlist.json`, `final-scope-audit.json`, `source-ci-inventory.json`, `xml-measured.json`, `final-jar.sha256` and copied final plan/report/fix documents are covered by external `evidence-manifest.sha256`. It hashes commands/exits/logs/scripts/intake/XML/report copies and final documents without self-referential report hashes; sealing checks required successful stems and verifies every manifest entry. Existing modes remain unchanged; all eight edited/new outputs are strict UTF-8/LF with no BOM/trailing whitespace and one final LF.

Final V015 remains byte-identical to intake: SHA256 `1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3`; actual Flyway 12.4.0 ChecksumCalculator returns `-1316980027`. Independent SQL literal comparison confirms delta14, customer26, evidence16, other roles219, global261, fifteen-row history. Actual focused and final PostgreSQL IT executions additionally pass the independent Java oracle, replay/atomic rejection, exact history and runtime capability/role-restoration assertions; static arithmetic is not substituted for runtime proof.

## 5. Residuals and stop boundary

| Residual | Owner / destination |
| --- | --- |
| Mandatory cursor key custody, replica consistency, replacement invalidating cursors | Security/Operations → deployment handoff; no live key is assigned |
| Qualified customer DSL runtime inversion | Backend/Architecture → accepted deferred post-PA27 follow-up; actual same-connection/snapshot IT passes for current implementation |
| Column grants are not RLS; application owns row isolation | Security/Database → least-privilege acceptance; no RLS claim |
| Single-request repeatable-read semantics, not cross-page snapshot or linearizable revocation | Backend/Security → documented behavior and executed concurrency evidence |
| Large-owned-set sort/index performance | Backend/Database → representative EXPLAIN; schema/index change returns to Controller |
| Existing Q17/Q23, migrator/comment custody, credentials/TLS/pg_hba, pool drain/old sessions | Platform/Database/Security/Operations → PA25 runbook; production NOT EXECUTED / NOT CLAIMED |
| Metadata-free snapshot / managed-worktree and remote integration | Controller → later managed process; no Git/PR verification or mutation claimed |

Stop after this bounded fix, exact validation and truthful report. No stage, commit, push, branch, PR creation/mutation, merge, approval, ready action, reviewer request, external comment or code-review entry occurs. No gate advancement or production acceptance is authorized by this author report.
