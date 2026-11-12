# PA29 Customer Evidence Read Foundation — Plan Fix 1

## 1. Authority and scope

- Task: `pa29-evidence-plan-fix1-gpt-20261112-01`; ONECALL work unit: `PA29-customer-evidence-read-foundation`; date: 2026-11-12; gate: **fix**.
- Authority: active PA29 checkpoint/adjudication in `docs/gateflow/project-execution-control.md`; both independent reviews were read, with all accepted findings addressed below. This is a plan-only repair, not implementation, acceptance, re-review dispatch or advancement of the gate.
- Exactly two repository writes: update `docs/gateflow/pa29-customer-evidence-read-foundation-plan.md`; add this artifact. No code/test/SQL/OpenAPI/CI/POM/Control/review/private/generated file was edited.
- Confirmed goal, the two frozen evidence GET routes, exact projections, one composed S1 read slice, nineteen-tuple V017 delta and frozen OpenAPI/V001–V016 remain unchanged. Controller decisions are binding: whole-list OPEN-risk403; always-present artifactRoles with empty[]; trusted-application JSONB residual with scalar-only capture SQL/no JSON emission or logging; actual V017 checksum computed and sealed during implementation.

## 2. Inputs and changed-file hashes

All digests are raw-byte SHA256, lowercase. The original plan digest was checked before any write.

| Input / changed file | Before / reviewed SHA256 | After SHA256 |
| --- | --- | --- |
| `docs/gateflow/pa29-customer-evidence-read-foundation-plan.md` | `c92a2c7e8ff7dc09bd84f08529be3e4c82736b80cd144ad665fc60ecb5b47795` | `6e8567fc5207e17bdb7a969339384c456610617dc4c208ddb27d76e61dc8e9e3` |
| `docs/gateflow/pa29-customer-evidence-read-foundation-plan-fix-1.md` | Absent; new authorized artifact | Final raw digest supplied in the handoff, not embedded recursively in its own contents |
| `docs/reviews/plan-review-20261112-070311.md` | `7aabef2cff36f068e9adc94c032bbb312596b9cf4eeee1db39da4dbba2c659e1` | Unchanged |
| `docs/reviews/plan-review-20261112-071426.md` | `9234e732eeec18750fd5d0d1a7f8f0f88b82f48c97d3d0081bedb26653e8e456` | Unchanged |

The active Control, reviewed source/interfaces/controllers/configurations, all test/fixture dependency and exact-assertion surfaces, migrations, PA25 SQL/runbook, specifications, CI and POM were inspected. In-memory input reading covered 307 source/test/SQL/CI/POM/specification/PA25 files (6,773,325 bytes); that is inspection, not runtime test execution. The SHA-matched original plan in the frozen review input was also used to compare all76 proposed names and their order against the fixed plan.

## 3. Accepted finding closures

**Fixed means the cumulative plan now gives bounded implementation instructions, not that implementation code has been changed or executed.** All existing test identities and unrelated assertions must remain unchanged.

### PA29-PLAN-REV-01 — Fixed (High)

Plan §8.3 now explicitly authorizes and bounds all ten previously omitted latest-chain write paths under `backend/src/test/java/com/onecall/archtests/container/`:

- `CustomerMeFixture.java`, `SharingQueryFixture.java`: fresh migrate16->17 and applied16->17, preserving role/reset/cleanup behavior.
- `SharingQueryServiceIT.java`, `SharingAdmissionRoleLeastPrivilegeIT.java`, `EvidenceMediaReadRoleLeastPrivilegeIT.java`, `PublicShareAdmissionIT.java`: only their identified latest applied-history size16->17 assertions.
- `SharingRoleLeastPrivilegeIT.java`: fresh migrate16->17, applied16->17, append version017/script at index16; keep earlier rows and V009/V010/V011 historical inputs exact.
- `EvidenceMediaReadIT.java`: nested Fixture fresh migrate16->17 only.
- `CustomerBusinessNumbersIT.java`, `CustomerSubjectsIT.java`: their identified successful-history string count `"16"` -> `"17"` only.

The plan also retains/bounds the previously authorized CustomerBusinessNumbersFixture, CustomerBusinessNumberReadPrivilegeIT, CustomerSubjectDetailReadPrivilegeIT and ProductionDatabaseIdentityProvisioningIT changes. It makes historical-vs-current behavior explicit rather than globally replacing sixteen, ACL counts or PostgreSQL major-version literals.

Directly proven dependent paths are enumerated **read/run only**, not extra writes: `CustomerMeIT.java`, `CustomerSharesFixture.java`, `CustomerSharesIT.java`, `CustomerShareCommandsIT.java`, `PublicSharesFixture.java`, `PublicSharesIT.java`, `MediaPlaybackIT.java`, `MediaGatewayTransportIT.java` under the same container root. The documented chains run through CustomerMeFixture, SharingQueryFixture, PublicSharesFixture and MediaPlaybackIT.Fixture. No independent latest16 literal requires editing these dependent paths. Existing all19-suite/302-method regression validation covers them; full S1 will have21 suites/336 methods. No method is added to an existing class.

### PA29-PLAN-REV-02 — Fixed (High)

Plan §8.3 now adds all four omitted exact surfaces:

- `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`: customer dependency9->10, adding only `evidence::customer-evidence-query`; preserve eight methods and sharing/admission/media assertions.
- `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java`: exact Customer map12->15, with new transaction template qualifier `[customerQueryTransactionManager]`, service qualifiers `[customerEvidenceTransactionTemplate, customerQueryDsl, customerBusinessNumberAccessRepository, customerSubjectAccessReader, customerEvidenceReader]` in order, and codec qualifiers `[]` (Properties skipped). Preserve six methods and unrelated graphs/lazy/profile/non-Primary assertions.
- `backend/src/test/java/com/onecall/core/customer_iam/internal/application/CustomerBusinessNumberQueryServiceTests.java`: declared method count12->15 only; preserve eight methods and existing isolation/qualifier/DSL assertions.
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`: customer internal regular-file count34->39 for the five new Customer files; preserve three methods and per-file boundary constraints.

The already planned ModuleBoundaryTests, CustomerBusinessNumberBoundaryTests and CustomerSubjectDetailBoundaryTests edits remain bounded. The seven modified Surefire suites retain42 existing identities (9+4+4+8+6+8+3); this is regression coverage, not42 additional tests.

### PA29-PLAN-REV-03 — Fixed (High)

`backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java` is allowlisted; the inaccurate blanket prefix-pinned exclusion is removed. Chosen option: **current V017 expectations** at the two unfiltered `current` sites in `v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle` and `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects`: after V014, migrate2->3, applied16->17, validate, then existing PA25 apply/check at288/history17.

Historical proof stays separate: target013 prefix, target014 latest, V014 fresh14/upgrade1/replay0; target ACL oracle customer12/evidence16 =28; identity, credentialed/uncredentialed loops, graph, frozen input, atomic rejection and replay snapshots remain unchanged. V014 replay/oracle runs before upgrading current, never against V017's enlarged ACL. Current PA25 still independently validates full history and288 tuples without repair, including all preserved V001–V016 literals. All twelve methods remain. Simply pinning current016 without addressing current PA25 would not satisfy this plan.

### PA29-PLAN-REV-04 — Fixed (Medium)

Plan §8.3 explicitly distinguishes both `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` surfaces:

- V013-target resolved inventory: `all()` size16->17; exact V014/V015/V016 ABOVE_TARGET/not-applied retained; add one V017 with version017, exact script `V017__customer_evidence_read_privileges.sql`, ABOVE_TARGET and not-applied. Applied remains13; pending-through-target empty; repeat migrate0.
- Independent `assertV009LiveFence`: append that V017 script after V016 to the complete ordered V001–V017 resolved script list; do not change the historical applied-history list ending V013. The plan spells out all17 script literals and a separate exact V014–V017 ABOVE_TARGET table.

All64 methods, historical schema/ACL/live-fence assertions and the one historical annotation-comment hit remain unchanged.

### PA29-PLAN-REV-05 — Fixed (Medium)

Plan §§4.3/5.2 require `artifact.artifact_role = 'ORIGINAL'` **and** source-recording ID/media/status correlation, in the optional LEFT JOIN/CASE rather than a row-dropping WHERE. DERIVED never receives duration even if it legally shares the ORIGINAL media. This consumes existing planned grants and does not alter nineteen-tuple arithmetic.

Plan §9.6 retains exact proposed method identity `durationUsesOnlyUniqueMatchingValidatedSourceRecording`: seed VALIDATED source duration37 and legal ORIGINAL/DERIVED rows for one evidence/media; assert two rank-ordered artifacts with equal safe media facts, ORIGINAL duration37, DERIVED duration key absent, summary roles `[ORIGINAL,DERIVED]`, and no fan-out. Absent/nonvalidated/misbound source, unrelated recordings and DERIVED-only shared media remain covered inside that method. The existing proposed service test also covers shared-media owner rows. There is no77th addition or unnecessary test suite.

### PA29-PLAN-REV-06 — Fixed (Low)

Plan §§1/2/11.2/12 consistently state merged-main base `ef9d0823975ef66d1df3b70e921709d8562b4dcb`, plus Controller goal-confirmation HEAD `73813534bb3a8ec8f3f3c38db6c16e0d12109b5c`. Read-only `git.exe log -2` verified this exact parent/child pair.

Actual environment is WSL Bash in the specified managed Windows worktree. Native Linux Git fails on the Windows `.git` pointer, but installed Windows `git.exe` works. No repair, clone, fetch, branch change, staging or commit is required/performed. Initial status has modified Control plus untracked plan/reviews/private prompts; these are preserved Controller inputs, not implementation changes. Plan commands audit both baselines and use status for untracked candidates; diff alone is insufficient. Control/goal/plan/fix/review/private bookkeeping is excluded from the53 implementation candidates. No new remote-state verification is claimed.

## 4. Arithmetic and static validations

Executed with Python3 via stdin (in-memory parsing/hashing, no repository helper/cache/report), `sha256sum`, and read-only Windows Git. All final checks passed:

| Validation | Observed / required result |
| --- | --- |
| Document structure | UTF-8, newline terminated, no NUL/trailing whitespace; plan numbered sections1–12; six accepted closures in this artifact |
| New method identities | `[12,12,8,6,4,24,10]`, unique76; identical names/order to SHA-matched pre-fix plan |
| Existing CI/source identity equality | CI literal maps evaluated in memory, compared with comment-stripped Java test names: Surefire44/375, Failsafe19/302; no changed source |
| Baseline annotation lines / POM | 678 hits, including one historical comment; parsed POM includes exactly the19 CI Failsafe identities |
| Planned final source/CI/XML | 49/417 +21/336 =70/753; additions42+34=76; annotation lines754, POM21 (static forecast, not generated XML) |
| Exact write-path arithmetic | 23 existing test/fixture paths resolve; production Java14 plus test Java31 =45 Java; SQL2 + YAML1 + POM XML1 + docs4 ->53 candidates; new19 + modified34 |
| Existing edited test paths | Twenty suites = Surefire7 + Failsafe13, plus three fixtures; eight unchanged dependent paths excluded from writes |
| Privilege arithmetic | Delta8+3+4+4=19; customer34+19=53 =6 schema+47 column; pair50->69; global269+19=288 =53+16+219; history16->17 |
| Frozen bytes | All18 raw SHA256 values in plan §11.3 match: two OpenAPI files and V001–V016; V016 LF-normalized hash also matches `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f` |
| Exact fix literals | Dependencies9->10; bean/declaration12->15; internal files34->39; current migrate2->3/applied16->17; both resolved-history surfaces; ORIGINAL-only predicate/shared-media duration37 assertions present |
| Git | `git.exe status --short`, `rev-parse HEAD`, `log -2`, diffs against both baselines and `diff --check` succeeded; native Linux Git failure is a path-translation limitation, not missing managed Git |
| Scope equality | All1,825 files outside the two permitted paths have identical before/after path sets and raw hashes; no extra files outside the allowlist |

Scope manifest algorithm: exclude `.git` path components and only the two permitted files; sort by relative path; join `path + NUL + raw SHA256` records with newline; hash UTF-8 bytes. Before/after digest is `c6391b728ea05078820cf1fe26b8b9936ada72ae98f6bb1964914956964a277f`. Private inputs are only protected by equality, not reproduced or modified. The pre-fix manifest was captured before repository writes; it is not an invented clean Git baseline.

Reproduction entry points are plan §11.1 (static algorithms/invariants) and §11.2 (later focused/full commands). The fixed plan now includes exact regression commands for seven existing Surefire suites and all19 baseline Failsafe suites, followed by full clean verification and the complete updated CI inventory parser. No build, Docker, PostgreSQL, PA25 apply/check, XML generation or full CI was run in this fix gate. Runtime expectations are not represented as observed passes.

## 5. Residuals, questions and stop

- Remaining policy/contract question: **none** after Controller decisions. The accepted findings are closed at plan level; independent re-review and implementation remain later Controller gates, not performed here.
- V017 actual checksum remains intentionally implementation-derived and must be sealed in PA25, tests and report. Existing V001–V016 literals, especially V016 checksum1669606497, are frozen. No plan-time checksum invented.
- Security/Database own the accepted whole-column JSONB trusted-application residual. API SQL extracts only capturedAt and never emits/logs JSON. This is not JSON-path/RLS enforcement.
- Backend/Database own tenant-sized subject-set capacity evidence; Security/Product own non-linearizable repeatable-read revocation semantics. No cache/index/read-model expansion is authorized to hide these residuals.
- Evidence owner retains data-quality handling for legally malformed capture timestamps/oversized MIME; detail404/list500 remains bounded and sanitized. ORIGINAL-only duration avoids guessing DERIVED recording facts.
- Implementer/CI own Java21/Docker/PostgreSQL16, migration/replay/pool/concurrency and complete source=CI=XML/full-CI execution. Platform/Database/Security/Operations own production credentials/TLS/deployment/rotation prerequisites; production readiness is not claimed.
- Stop now after these two documents and static verification. No code/test/migration/API/CI/POM/Control/review/private/generated edits, re-review dispatch, stage/commit/push/PR/merge/approve/mark-ready/reviewer request/external comment/issue mutation was performed or authorized.
