# PA39-S1 Catalog Draft Write Foundation — Code Review Fix 2

Task: `pa39-operations-catalog-draft-write-foundation-s1-code-review-fix2-qwen38max-20261121-01`  
Date: 2026-11-21. Current gate: **code review fix 2**. Next gate: **final independent code re-review**, not entered.

## Custody

Preserved HEAD `5ddeb3cd7e03111e2867135a5757ddf35322e410`, tree `4eac083650c4969ba0d4d281ed8ba34c7332ba55`, branch `codex/pa39-next-work-unit`, and the accepted dirty implementation. No reset, restart, stage, commit, push, PR, merge, approval, readiness, reviewer request, external comment or issue mutation occurred. The GPT primary and its single same-provider retry failed before model execution with the identical provider precharge-quota `403`; this `qwen38max` task is the recorded fallback under the same frozen scope and completion contract. The prior same-task attempt's bounded edits, interrupted by the provider `403`, were inspected path-by-path against the gate-entry manifest, preserved, completed where partial and independently verified; the one non-canonical artifact (mixed CRLF/LF bytes in the inherited manifest-guard file) was normalized to LF.

Final repository changes are limited to these accepted 29-path allowlist paths:
- `backend/src/main/java/com/onecall/core/catalog_order/internal/operationsdraft/DefaultOperationsCatalogDraftCommand.java`
- `backend/src/test/java/com/onecall/archtests/OperationsCatalogDraftWebTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftFixture.java`
- `backend/src/test/java/com/onecall/archtests/container/OperationsCatalogDraftLifecycleIT.java`
- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationsdraft/CatalogDraftRepositoryTests.java`
- `backend/src/test/java/com/onecall/core/catalog_order/internal/operationsdraft/DefaultOperationsCatalogDraftCommandTests.java`
- `docs/gateflow/pa39-operations-catalog-draft-write-foundation-implementation.md` (this report)

No test identity, dependency, route, migration, schema, grant, public field, frozen doc or OpenAPI change was made; no V019 exists. The inherited guard updates only the required governed manifest digest, because production bytes changed in `DefaultOperationsCatalogDraftCommand.java`; class/method/annotation inventories and frozen inputs remain unchanged.

## Accepted Low Fixes — Final Status

- **SQL cause-chain classification (`PA39-RR-A-01` + `PA39-CR-RR-B-03`) — 已修复.** `sqlState` again traverses the full cause chain: a stateless or `00000` `SQLException`/`DataAccessException` node no longer stops the scan before a nested stateful cause, and `null` again means "no recognized state". A new `sqlRelated` predicate returns true for any `SQLException`/`DataAccessException` in the chain even when SQLSTATE is null or `00000`. The 500-vs-503 decision uses `sqlRelated`: non-SQL without a state → sanitized `INTERNAL` 500; any SQL-related failure without a recognized state → `SERVICE_UNAVAILABLE` 503. The cleanup/restoration gate uses `!sqlRelated(failure)`, so unknown-state SQL failures no longer attempt invalid reset SQL while non-SQL failures still perform best-effort restore. Authority-port-first 503 normalization, owner lock 55P02/55P03 409, raw 23505/40001 single bounded recovery, typed `Failure` precedence and cleanup-failure 503 are preserved unchanged. Subcases inside the existing command identity `recoveryReauthenticatesOnceAndNeverRetriesWrites` prove: a stateless `DataAccessException` (null and `00000` override forms) wrapping a stateful `SQLException` still reaching 409/one-recovery; stateless SQL failures (`DataAccessException` without cause, `SQLException` wrappers with null/`00000`/`XX000` states) yielding 503 rather than 500; non-SQL runtime still yielding 500; and restoration attempted exactly on non-SQL failures only, on both first-attempt and recovery-attempt routes.
- **POST draft-template concealment (`PA39-CR-RR-B-01`) — 已修复.** Inside the existing `OperationsCatalogDraftWebTests.concealsAllOtherCatalogMutationTemplatesBeforeAuthentication`, `POST /api/v1/operations/catalog/versions/<canonical UUID>/draft` is now asserted to return the exact sanitized 404 `NOT_FOUND` problem before authentication, with `verifyNoInteractions(command)` retained; PUT malformed/trailing/extra variants and valid PUT coverage are unchanged.
- **Zero-row reread outcome matrix (`PA39-CR-RR-B-02`, test-only arm) — 已修复.** Inside the existing repository unit identity `zeroRowsAndRawUniqueRacesUseBoundedReread`, test-local subcases now probe every production outcome arm of the bounded reread through the existing mutable-row mock: unchanged DRAFT `r=e` → 503; successor `r=e+1` with equal projection → 200 replay with no writes; successor `r=e+1` with different projection → stale 409; other revision relation → stale 409; and non-DRAFT/legacy/corrupt classifications continuing to obey the existing `validateStored` precedence (corrupt shape → 503 before non-DRAFT 409; legacy member set → 409; hash disagreement → 503 before stale). Each subcase re-asserts one UPDATE only, two locked reads, no offer/audit SQL, empty audit/child writes and byte-unchanged stored facts. No production fault switch, public API, table or second UPDATE was added; identity counts are unchanged.
- **Private web trace-attribute coupling (`PA39-CR-RR-B-04`) — 已修复.** `OperationsCatalogDraftFixture` no longer references `RequestContextHolder` or the private `OperationsBoundaryFilter` trace-attribute literal, and the `auditTraces` list was removed. A test-local `callerTrace()` helper extracts the caller trace from the recorded `audit.append_fact` bind values (the final request_id/trace bind set: asserted 8 binds, request_id shape `catalog_version:...:draft:...`, trace parsed from bind index 7) via the existing jOOQ execute-listener seam. Lifecycle audit assertions still compare the durable `audit.audit_fact` `trace_id` to that captured caller trace, and all before/after/action/actor/reason/request assertions are preserved.

## Fresh Validation

Fresh external projection: `/tmp/pa39-code-review-fix2-validation.62577f`. Candidate: `/tmp/pa39-code-review-fix2-validation.62577f/candidate`. Evidence: `/tmp/pa39-code-review-fix2-validation.62577f/evidence`. Construction used a new `git -c core.autocrlf=false archive HEAD` plus the complete 29-path LF-canonical overlay only; the six tracked `.pa5-*` private helpers were excluded as in the accepted predecessor run, and no Control/review/private metadata/build/node_modules overlay occurred. Archive SHA256 `b84423dd16ba1321885657e98f41ab98a205101c7edffaade117f4eeaf199814`; `2376` intended files; only `docs/gateflow/project-execution-control.md` differs non-representationally from the dirty worktree (HEAD projection retained by design). Primary worktree has no backend target or node_modules. Toolchain: Java 21.0.13, Maven 3.9.9, Node v22.23.4, npm 10.9.9, Python 3.10.12, Docker 29.1.3, PostgreSQL 16.16. Projection-only `npm ci` exited 0; installed manifest 20434 entries / SHA256 `739fa3927c474ffe17b010f2440a576518054172b84708861bec87a8460995b3`; package and lockfile hashes unchanged.

| Gate | Result |
|---|---:|
| Focused PA39 unit | 7 classes / 84 methods, exit 0 |
| Architecture | 3 / 25, exit 0 |
| Exact CI/README guards | 1 / 1 and 1 / 1, exit 0 |
| Frozen regression | 6 / 52, exit 0 |
| PA37/PA38 unit | 13 / 138, exit 0 |
| PA39 PostgreSQL IT | 2 / 36, exit 0 |
| PA37/PA38 PostgreSQL IT | 4 / 74, exit 0 |
| Race/fault | 2 / 8 in each of 3 fresh processes; 24 / 24, exit 0 |
| Clean `-Pcontainer-smoke clean verify` + XML oracle | 144 / 1,684, zero skipped/failures/errors, exit 0 |
| Explicit all-Failsafe | 40 / 666, zero skipped/failures/errors, exit 0 |
| JAR smokes / dependency firewall | exit 0 / exit 0 |
| Contracts | npm ci, A1–A5 and individual A2-s3/A3-s3/A4-s2/A5-s1/A5-s2, all exit 0 |

Final totals are **104/1,018 Surefire + 40/666 Failsafe = 144/1,684**. Identity SHA256 is `6041250383865a75ff49037c1f736bb87db552561206b9990aedaf8604a51770`; annotation lines 1,685; POM includes 40; governed manifest 318 / SHA256 `26a10794d635d28bb402a4c88ee27b4c486ac09175b637f90faa122b38114f4a` (the actual final digest after the production edit, pinned in the inherited guard and independently recomputed); V001–V018 only; active 26-row frozen aggregate `afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761` over 3,507 bytes.

Exact commands, exits, logs, XML, hashes, environment, projection construction and oracle are retained under the evidence root in `maven-commands-and-exits.json`, `contracts-commands-and-exits.json`, `contract-steps-commands-and-exits.json`, `final-oracle.json`, `static-final.json` and the named gate directories. Explicit contract commands exported `PA39_VALIDATION_ROOT`.

## Limitations And Stop

The live already-login restore branch (witness-equal no-op in `JooqCatalogDraftRepository.restore`) **remains unit-only coverage**: it is exercised by the mock-based `CatalogDraftRepositoryTests.restoresRoleAndDiscardsWitnessOnEveryExit`, and no live PostgreSQL test forces a pre-`SET LOCAL ROLE` failure, because normal/denial live paths always switch roles before restore and no new production hook was added. Its live isolation stays `deferred-with-owner` to Backend test-maintenance per the accepted adjudication. Other residual owners are unchanged: exact-integer canonical documentation reconciliation remains `deferred-with-owner` to Architecture/Documentation (`PA39-CR-B-04`); the PA38-vs-PA39 unknown non-SQL 503/500 divergence remains a cross-unit consistency residual owned by the catalog owner; production credential/verifier wiring is unavailable-by-default and pre-existing catalog grants are outside this slice. No hosted-CI or production-readiness claim is made. Residual blockers: **none**. This report is the final repository write; its external bytes/SHA256 are reported outside the repository to avoid self-referential instability. Next gate is **final independent code re-review**, not entered.
