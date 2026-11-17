# PA35 S1 code-review fix 1 author evidence

## Gate, workunit and custody

- Gate/workunit: PA35-operations-ivr-session-read-foundation / S1 / code-review fix 1.
- Interrupted author task: `pa35-s1-code-review-fix1-gpt-20261117-01`.
- Continuation: `pa35-s1-code-review-fix1-cont1-gpt-20261117-01`, 2026-11-17, Asia/Shanghai (+08:00).
- Runtime/provider/model/reasoning: Codex runner / gpt / GPT-6.1-SOL / high, supplied dispatch attribution, not independent model introspection.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa35-evidence-role-ivr-detail-read/codex-call`.
- Accepted-plan commit: `5aed1ca1f50ab1216b4167baeb1fc3ee6c8d7c3b`; original implementation intake/Control checkpoint: `4730214bb7af05490debffe5d69b3ce44902c7bc` (historical report attribution).
- Current fix-continuation HEAD, read-only verified: `13196468f83bc11b66b7c67e17a81aa50a23e7ac`; no commit is made by this continuation.
- Intake hashes: `/tmp/pa35-cr-fix1/baseline.json`; fresh evidence: `/tmp/pa35-cr-fix1-cont1/`.
- Windows `.git` pointer is unchanged; read-only Git uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call23` and `GIT_COMMON_DIR=/mnt/d/codex-call/.git`.

This continues the existing author fix, preserves completed work, and is not
a new review, re-review, approval or later-gate action.

## Changed paths and frozen inputs

The intake comparison initially found only the following three test changes;
the implementation report had the same hash as intake. This continuation
changes only the two reports, retaining the passing tests unchanged.

| Path | Fix responsibility |
|---|---|
| `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` | CR-01 F8, CR-03 role completeness, CR-04 projection |
| `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` | CR-01 G1 and real PostgreSQL/Flyway history |
| `backend/src/test/java/com/onecall/archtests/OperationsIvrSessionReadWebTests.java` | CR-02 sanitized invalid call-shape responses |
| `docs/gateflow/pa35-operations-ivr-session-read-foundation-implementation.md` | Actual fix attempts and continuation status |
| `docs/gateflow/pa35-operations-ivr-session-read-foundation-s1-code-review-fix-1.md` | New author fix artifact (this path) |

Fixture and test-provider files remain unchanged. Exact final candidate scope
is the accepted plan's 30 paths plus this one artifact; `.codex` remains the
pre-existing excluded runner marker. No staged/index changes; no new test
identities. No Control/plan/review/production/POM/CI/migration/OpenAPI edits.

Fresh static verification checks all 26 raw and CRLF-to-LF pins against the
accepted plan and independent hardcoded map. The sorted UTF-8 path + NUL +
32-byte raw SHA256 + 32-byte LF SHA256 binary pair map is
`ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409`.
Migrations remain V001-V018, with no V019. The established 291-file production
manifest (excluding the separately pinned V018) remains byte-for-byte equal
to `/tmp/pa35-final-production.manifest`, aggregate
`95c4eef4059fcc92d2408a493bbd89420a7fa5d5a5a3e5cdbd4dc164cc1623e0`.

## Findings: final closure

| Finding | Status | Direct executable evidence |
|---|---|---|
| CR-01 | CLOSED | Existing F8 `frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact` and G1 `postgres16UsesExactlyFrozenEighteenMigrations`, both in passing focused/full XML |
| CR-02 | CLOSED | Existing `optionalFieldsAreOmittedAndRequiredNullsRemainPresent`, passing focused/full XML, now includes eight invalid V005 call summaries |
| CR-03 | CLOSED | Existing `namedInterfaceAndExactThirteenModuleEdgesArePreserved`, passing focused/full XML, checks exercised role union against every enum value |
| CR-04 | CLOSED | Existing `ivrSqlUsesOnlyExplicitApprovedColumns`, passing focused/full XML, pins authority SELECT exactly |

CR-01: F8 and G1 independently bind the actual accepted plan file's 26-row
raw/LF table and 26-row bare raw map to a hardcoded 26-pair oracle, enforcing
exact counts, keys, values and no duplicates. Binary serialization uses
unsigned UTF-8 path ordering, decoded hash bytes, one path NUL and no row
terminator. Negative interpretations explicitly reject hex-text hashes,
extra hash delimiter and row terminators. Both tests run isolated temporary
LF and CRLF positive copies and reject changed bytes, BOM, independent map
mutation/add/remove/duplicate, plan table and bare-map mutation/add/remove/
duplicate, missing file and added V019; the final restored positive passes.
These are actual temp-file verifier calls, not unexercised declarations.
The replaced strings match the currently parsed plan rows, so the mutation
assertions are feasible, not no-op replacements. Expected failure assertions
wrap verifier calls, not an unconditional failure. Temporary files are cleaned.
F8 retains the no-production-EmployeeSessionVerifier scan and authentication
boundary assertion. G1 additionally proves PostgreSQL major 16, UTF8,
18 successful versioned rows, V018 checksum 1606293398, and every installed
rank/version/description/type/script/computed CRC32/success plus no extra row.

CR-02: ACTIVE and UNKNOWN each reject endedAt present and duration present
without endedAt (four shapes); ENDED/FAILED/REJECTED each reject missing
endedAt (three); ENDED rejects end-before-start (one). Each reaches the real
service/controller through the existing web fixture, requires the shared
sanitized SERVICE_UNAVAILABLE/503 problem envelope, and verifies no Evidence
interaction. The existing optional-field/null assertions remain intact.
No frozen database constraint is weakened to manufacture corruption.

CR-03: the guard extracts the actual role loops from both subject-read and
IVR least-privilege ITs, requires exactly one matching loop per file, and
compares their union to all six `OperationsReadContext.Role.values()`.
It therefore fails on an omitted current role or an added enum value not
covered by these tests, while preserving module verification and 13 edges.

CR-04: the employee-account authority SELECT matcher must yield exactly
`SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)`.
This checks the actual production service text, not an unrelated string,
and rejects extra/reordered columns or multiple matched authority queries.
Existing IVR safe-column and forbidden-column assertions remain intact.

## Executed validation and evidence provenance

All commands use repository root unless backend cwd is expressly specified.
Native Maven uses `/opt/jdk21`, `/opt/maven-3.9.9/bin`, and `MAVEN_SKIP_RC=true`.

1. Original fix focused attempt 1 failed compilation on regex escaping;
   attempt 2 failed because Maven ran at root without a POM. These are
   preserved failures, not successful test evidence.
2. `bash /tmp/pa35-cr-fix1/run-focused3.sh`, backend cwd, executes the preserved
   explicit nine-unit/three-IT-class Maven selector with both fail-if-no-tests
   guards and skip flags false. `focused-attempt3.exit` is 0. Final result:
   9 Surefire classes / 109 identities + 3 Failsafe classes / 48 identities =
   157; zero failures, errors or skips. Preserved XML under
   `/tmp/pa35-cr-fix1/focused/` was reparsed, duplicate identities and adverse
   result children rejected, and all identities matched the frozen focused
   inventory. Fresh copies are under `/tmp/pa35-cr-fix1-cont1/focused/`.
3. `mvn clean -Pcontainer-smoke verify`, backend cwd, completed BUILD SUCCESS
   at 2026-11-17T12:42:55+08:00, duration 36:37. Complete current native XML
   exists: 83 Surefire classes / 788 identities + 34 Failsafe classes / 556
   identities = 117 / 1344; zero failures, errors or skips. All were parsed,
   compared to exact source identities and preserved under
   `/tmp/pa35-cr-fix1-cont1/full/`. There is no `/tmp/pa35-cr-fix1/full.exit`:
   the earlier runner was interrupted after Maven returned. No marker is
   fabricated; BUILD SUCCESS, both summary totals and complete XML are the
   executed evidence. Test mtimes 12:01:51-12:02:56 precede the approximately
   12:06:18 build start; source/test/POM/CI hashes remain unchanged throughout
   continuation. This is the current test candidate, not a stale alternate.
4. `python3 /tmp/pa35-cr-fix1-cont1/verify.py` parses both XML sets, snapshots
   all backend source inputs plus POM/CI, and extracts the exact current
   `assert container smoke actually ran` CI body without modifying it.
   `bash -e -o pipefail` executes that body from native backend cwd. The
   extracted `ci-exact-inventory.sh`, `.log` and `.exit` (0) are preserved.
   Exact source=CI=POM=XML identities, 34 POM includes and every historical
   arithmetic assertion pass: 1344 methods and 1345 annotation line hits
   using `re.search(r"@Test\b", line)`. No tests were edited to chase the
   old helper's false 1373 substring count (`@TestConfiguration` included).
5. `python3 /tmp/pa35-cr-fix1-cont1/static.py` checks intake/scope, raw/LF
   pairs, binary digest, production manifest, exact source identity inventory,
   correct token count, clean index and `git diff --check`. Reports and changed
   tests are UTF-8 without BOM, LF with final newline, no replacement
   character, conflict markers or trailing whitespace. Prior helper
   `finish-validation.log` failed on report location, not Maven; this direct
   native XML verification supersedes that failed helper.

### Hash custody

| Evidence | SHA256 |
|---|---|
| BoundaryTests source | `ac11ffe082247ac78e70b9262f6848c85f9aa1d71ce1944a5f2247dcdfbf020c` |
| ReadsIT source | `5d7b96b686165e45474cd1cc18b9e8dbdc232f0f0e4baae42b7b73354f319573` |
| WebTests source | `ff6b753b415587687856994d66404980fb07d8f389a3390f931695f54d45d852` |
| `candidate-inputs.json` | `26b6b7daccbeab0f2ec1368f870f03de6ad1c7c5a696b72fc44afdee593a26ee` |
| Original fix focused attempt3 log | `7dd31d7634537fc3f633ba93b948a21cb6a42cd801c3bd54caf9b16539eabe69` |
| Original fix full log | `b936f0be16991c07287955f99b64f7ff883958aea1138706c0289ccabc1dd5a7` |
| Focused exact method list | `970c81fe08cfd565b78c8eadc60d2b2cf02fdb405989d0878f02157268afb64d` |
| Focused XML manifest | `f0499e55e333e2228ce78526cd54c3177f94ea363acf0b8c5770fc9fcc9add54` |
| Full exact method list | `8ed03a7a78c38bcb2b9e2fdc8f9fa1c6bdde41e3f1639634fc27243974c4bee6` |
| Full XML manifest | `1f3c9f39acb5e9a1f3669368bd7ee33a5794289aed02a0b92d8056c47bbbe198` |

Manifests, inventories, input hashes and continuation scripts are in
`/tmp/pa35-cr-fix1-cont1/`; original logs/markers remain under
`/tmp/pa35-cr-fix1/`, untouched. No full-suite rerun is necessary because the
existing passing execution and complete native XML bind this unchanged test
candidate. Report-only completion does not invalidate those test results.

## Reused evidence and residual owners

A1-A5 checks are reused from `/tmp/pa35-evidence/a1.log`, `a2.log`, `a3.log`,
`a4.log`, `a5-s1.log` and `a5-s2.log`; all corresponding exit markers are 0.
Production JAR default/opt-in smoke and dependency firewall are reused from
`post-full-default-and-opt-in-web-jar-smokes.log` and
`post-full-production-jar-dependency-firewall.log` in the same directory,
both exit 0, plus `package-inspection.json` (ZIP CRC PASS, no forbidden
entries, 18 packaged migrations, no V019). These are the original
implementation's executed evidence, not rerun fix checks; test/report-only
changes leave production, build, workflow and contract inputs unaffected.
The original report's native CRLF-validator limitations and permitted
LF-equivalent validation provenance remain applicable, not erased.

No CR-01..04 action remains owner-deferred. Existing residuals remain:
Platform/Database/Security own production identity/secret delivery and SET
handoff proof; Identity/Security own independent employee verification and
IdP/MFA; Security/Platform own key lifecycle; Contract/Data and IVR/A2 own
frozen metadata/invariant reconciliation; existing B-F04 route-boundary owner
owns TRACE/error-dispatch alignment; Backend/Database/Operations/Capacity
own production load/EXPLAIN/deadline/pool follow-up; Controller/Test/Platform
own hosted CI and later independent gates; tooling/Security own CRLF wrapper
portability and locked npm maintenance. Live/cloud/media and production
readiness remain separate Operations/Infrastructure acceptance.

## Completion and path

Author fix 1 is complete: CR-01..04 CLOSED, exact focused/full/inventory
evidence PASS, candidate scope intact. Output path:
`docs/gateflow/pa35-operations-ivr-session-read-foundation-s1-code-review-fix-1.md`.
This is not hosted CI, independent acceptance, approval or production readiness.
No stage/commit/branch/push/PR/approval/ready/reviewer request/external comment/
re-review/merge/later-gate action occurs. No Control, plan or review is edited.
