# PA34 Operations activation-history plan fix 3 — bounded retry evidence

Task: `pa34-operations-activation-history-plan-fix3b-gpt-20261115-01`.

Work unit: `PA34-operations-activation-history-read-foundation`.

Date: 2026-11-15 (Asia/Shanghai).

Gate: **bounded plan fix only**. Author static verification is not independent review, plan acceptance, implementation authorization or production readiness. Final fresh peer-isolated A/B re-review remains pending and belongs to the Controller.

## 1. Binding clarification and exact changes

Read the PA34 Control block, cumulative plan, reviews `docs/reviews/plan-review-20261115-{033925,035649,043352,044112}.md` and fix-1/fix-2 as history. The previous fix3 attempt correctly stopped without writing; this retry follows the Controller's subsequent explicit count clarification, not the superseded instruction to retain Control inside S1.

Only the cumulative plan and this new evidence artifact change:

- Header: identify the current retry and cumulative fix-1/fix-2/fix-3 history, all outside S1; carry REV-01..06 and FIX-01 without claiming independent acceptance.
- Section4.2: pin the row08 owner class registration directly on `JooqOperationsActivationHistoryReader` as `@Component("operationsActivationHistoryReader")`, `@Profile("persistence")`, `@ConditionalOnProperty(prefix="onecall.operations-subject-read", name="enabled", havingValue="true")` and `@Lazy`. Forbid an Operations-configuration `@Bean`, duplicate registration and a reverse module edge.
- Section5: make the table exclusively the complete future S1 implementation allowlist. Remove the old Controller-only Control row and the P/F gate-artifact rows; renumber the implementation report to row23. Replace the old arithmetic with exactly 23 rows, 19 new/4 existing. Do not replace Control with another existing path or invent work.
- Sections5/6: helpers/fixtures contain zero methods annotated `@Test`; framework setup annotations needed for infrastructure are permitted and are not test identities. Every planned test places exact `@Test` immediately before its no-argument signature, whitespace only, with no intervening annotation/comment; the frozen CI regex stays unchanged.
- Sections10/11: distinguish this retry's two authorized outputs from future S1 and Controller bookkeeping; remove obsolete initial-gate hashes/status counts, single-fix claims and artifact/S1 sum calculations. Require actual retry-baseline evidence and report this artifact's own identity outside artifacts only.

Sections1/2/3/7/8/9 and the complete A-H method tables are byte-identical to the retry input. The corrected shared-ring fixture wording from fix-2 remains present. No public behavior, test identity, scope, slice, dependency or runtime-validation requirement changes.

## 2. Exact cumulative-plan identity

Path: `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md`.

| Measurement | Before this retry | After this retry |
| --- | --- | --- |
| Raw SHA-256 | `7d93c2773e30168e5e7bd7969211cd342b47e48badf2040c2ea48de276d0ce38` | `6afe1d7a6f14f69d628694b3bd9ef9f088f08a1e32a0378161a8449e0922ee69` |
| Raw bytes | 83,525 | 84,995 |
| Lines (LF count) | 574 | 573 |

Delta: +1,470 bytes / -1 line. Measurements use raw bytes, not normalized hashes. This fix-3 artifact's final raw SHA-256, bytes and LF count are reported **outside this artifact only**, avoiding self-reference.

## 3. Finding dispositions and preserved scope

These are author fix/re-verification statuses, not reviewer verdicts. Independent final A/B re-review remains pending for every item.

| Finding | Static evidence in cumulative plan | Author status |
| --- | --- | --- |
| `PA34-PLAN-REV-01` | Sections3/4.3 and unchanged unit/IT assertions separate nine DB-persistable shapes from five domain-legal shapes. Successful: INACTIVE→ACTIVE via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION; ACTIVE→SUSPENDED and SUSPENDED→ACTIVE via OPERATIONS/OPERATIONS. Four DB-shape-only negatives are INACTIVE→ACTIVE via OPERATIONS/OPERATIONS and ACTIVE→SUSPENDED via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION. Any invalid row/lookahead rejects the entire page with sanitized503; frozen constraints are not weakened. | FIXED, retained |
| `PA34-PLAN-REV-02` | Sections3/4.1/4.3 retain the port-local eleven-field `TransitionFacts`, all raw values nullable and revisions boxed `Integer`. Owner maps SQL only, with no semantic constructor validation, unboxing or public-record construction. Service validates all facts/lookahead and constructs public summaries with primitive `int` revisions. Malformed stubs remain constructible. | FIXED, retained |
| `PA34-PLAN-REV-03` | Sections4.2/6 reuse only the existing shared `onecall.operations-subject-read.cursor.*` ring and unchanged binder. AAD retains endpoint purpose `onecall:operations-activation-list:cursor:v1\0`, fixed `occurredAt desc, transitionId desc\0` sort and raw SHA-256 of UTF-8 `{}` without BOM/newline. Cross-purpose cursors fail both ways. No activation prefix/property class/binder/operator secret is introduced; missing/invalid shared material503 even first/empty pages. | FIXED, retained |
| `PA34-PLAN-REV-04` | Existing G method `onlyTransitionFactsAreReadAndNoHiddenFieldsLeak` still captures application/server/pool/jOOQ logs across success, cursor400, dependency/integrity503 and empty200. Cursor, employee identity, SQL/JDBC and key-material canaries must be absent from rendered/argument/MDC/throwable data; a harmless marker proves capture and cleanup runs in finally. Still 18 IT methods, not 19. | FIXED, retained |
| `PA34-PLAN-FIX-01` | Fixture/helper text continues to supply scoped synthetic values only to the existing shared ring. Full-plan search finds no affirmative instruction to supply new activation cursor properties. Legacy method names retain their explicitly corrected assertions and do not authorize a new prefix. | FIXED, retained |
| `PA34-PLAN-REV-05` | Header and sections5/10/11 describe cumulative history and separate gate artifacts. Section5 has only rows01–23, no Control/P/F/review/gate row, and only 19 new/4 existing S1 arithmetic. Obsolete sole-fix/sole-path statements and artifact/S1 sum calculations are removed. | FIXED in this retry |
| `PA34-PLAN-REV-06` | Section4.2 pins direct owner-class component/profile/property/lazy declarations, not an Operations `@Bean`. Rows17/20 and section6 allow infrastructure setup annotations but zero `@Test` methods. Section6 pins immediate exact `@Test` before no-argument signatures, no interposed annotation/comment, matching unchanged CI regex. | FIXED in this retry |

Scope retained: one `GET /api/v1/operations/activations` / `listOperationsActivations`, three-field `ActivationPage`, eleven-field `ActivationSummary`, five existing/new Operations route templates in total, current ACTIVE ADMIN/OPS authority and exactly one S1 slice. No additional endpoint, projection property, filter, secret, migration, privilege, dependency or acceptance criterion is added.

## 4. Exact S1 arithmetic and test inventory

Section5 is only future S1 permission data, not current write permission:

| Category | New | Existing | Total |
| --- | --- | --- | --- |
| Production Java | 8 | 2 filter Java | 10 |
| Test/helper Java | 10 | 0 | 10 |
| S1 implementation report | 1 | 0 | 1 |
| POM and CI | 0 | 2 | 2 |
| Future S1 allowlist | **19** | **4** | **23** |

Parsed exact sequential row IDs01–23 and resolved literal M/T prefixes: 23 unique paths, all 19 planned new paths absent, all four existing paths present. New rows01–08 are production Java; rows11–20 are ten test/helper Java; rows09/10 are existing filters; rows21/22 are existing POM/CI; row23 is the new implementation report. Control, cumulative plan, fix-1/fix-2/fix-3 and all reviews/gate artifacts are excluded. No artifact/S1 sum is computed.

| Manifest | Planned methods | Suite |
| --- | --- | --- |
| A — OperationsActivationReadWebTests | 12 | Surefire |
| B — OperationsActivationReadConfigurationTests | 8 | Surefire |
| C — OperationsActivationReadRequestTests | 8 | Surefire |
| D — OperationsActivationCursorCodecTests | 10 | Surefire |
| E — OperationsActivationReadServiceTests | 14 | Surefire |
| F — OperationsActivationReadBoundaryTests | 8 | Surefire |
| G — OperationsActivationReadsIT | 18 | Failsafe |
| H — OperationsActivationReadRoleLeastPrivilegeIT | 10 | Failsafe |

All eight full method tables, including assertion cells, equal the pre-retry tables. Parsed method counts equal their headings; all 88 `FQCN#method` identities are unique and unchanged: A–F = 60, G–H = 28, 60+28=88. Helpers/fixtures contribute no test methods.

Direct current-source recount using CI's unchanged `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(` regex gives 70/644 Surefire +29/476 Failsafe =99/1120. The `@Test\b` token-line scan is1121 with the sole preserved PostgresContainerIT comment; POM currently has29 exact IT includes. Synthetic in-memory signatures for each of the 88 planned method names all match the frozen regex; no test source is generated or executed.

Planned totals remain **76/704 +31/504 =107/1208**, token **1209**, POM **31**. This is future source=CI=POM=XML arithmetic, not executed XML evidence. No current source, CI or POM edit occurs. The existing owner component pattern, `operations-subject-read` named interface and `activation_consent` allowed dependency directly support the pinned declaration without modifying `ModuleBoundaryTests.EXPECTED_ALLOWED` or the plan's frozen 12-target multiset.

## 5. Frozen inputs, hygiene and preservation

All21 exact section8 raw SHA-256 entries were parsed, unique and verified against current files: OpenAPI +docs/14 +docs/15 +docs/17 +17 migrations, zero mismatches. Actual migration SQL filename-set equality passes: **exactly V001–V017, no V018**. Section8 is byte-identical to the input; no oracle or frozen content changes. Docs/12–19 and every other unauthorized path are also covered by the whole-worktree preservation check below.

The retry-start whole-worktree snapshot excludes only the two authorized output paths. It covers **2,074 existing files**, including `.git` pointer, Control, prompts, private helpers, fix-1/fix-2, all four reviews, frozen documents, code, tests, POM and CI. Sorted aggregate SHA-256 before/after is `e4fc98ca77df8b5925ec00428ec5c23db787ded089168808dde805d0d8e74dd1`: concatenate each sorted relative POSIX path encoded as UTF-8, NUL and its raw32-byte SHA-256 digest. File maps must be identical, detecting unauthorized changes, additions and removals. External Git metadata is only read and is outside this worktree-file aggregate.

Read-only history/control raw identities captured at this retry's start and preserved:

| Path under docs/ | SHA-256 |
| --- | --- |
| `gateflow/project-execution-control.md` | `33de562b2b27192643376cf269ba8ce33cf21f95ff6981f7f61755a96fdc1bfe` |
| `gateflow/pa34-operations-activation-history-plan-fix-1.md` | `d3882c4b2eb72a1a5cae1998be2d37f5bf0a78fa906cb1a73b011e12291d628e` |
| `gateflow/pa34-operations-activation-history-plan-fix-2.md` | `0019d556213b5b97ed62a38f7cc1961ccfc9ed277ea1fb48b18e18f0fc7f63c7` |
| `reviews/plan-review-20261115-033925.md` | `cd1cadcf68eb86a36ee049c0a6fba9b944c4abd5074125a480d8203db289ef76` |
| `reviews/plan-review-20261115-035649.md` | `00a59c3d094049175253959192b3efbc4c284407a5c3e9e872b1020253553d97` |
| `reviews/plan-review-20261115-043352.md` | `e42bd2c91cd14d58d405aa57bb215f255546cf48a524a537844607a4094f099e` |
| `reviews/plan-review-20261115-044112.md` | `2ce0e24e7ffb024fffa7b50f9594b0d250c9e908cfabbf3811f5687cb73bda52` |

| Static check | Result |
| --- | --- |
| Stale single-fix/sole-path/old-count/combined-count search | PASS; no obsolete affirmative bookkeeping remains. Remaining sole-owner/sole-SELECT/comment wording is unrelated; references to removed concepts are exclusions, not totals or permissions. |
| Exact S1 rows/new-existing split, eight manifests/88 unchanged identities, current source/token/POM recount | PASS; inline Python assertions, no helper or evidence file generated. |
| Reader declaration, named-interface/module boundary, exact frozen CI regex and synthetic no-argument signature matches | PASS; static only. |
| All21 frozen hashes and exact17 migration filenames | PASS; zero mismatches, no V018. |
| Both authorized outputs: strict UTF-8, final LF, no CR, conflict markers, U+FFFD or trailing spaces/tabs; balanced Markdown fences | PASS; zero findings. |
| Read-only baseline/file-map and aggregate comparison | PASS; 2,074 unauthorized paths byte-identical, no unauthorized addition/removal. |
| Tracked `git diff --check` with existing newline configuration | PASS, exit0, no diagnostics. |
| Each untracked authorized output: `git -c core.autocrlf=false diff --no-index --check /dev/null <path>` | Exit1 for added content, empty stdout/stderr; no whitespace diagnostics, not reported as exit0. |

The initial direct `git status --short` failed because Linux Git cannot resolve the existing Windows `D:/...` gitdir pointer. Subsequent read-only Git commands use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call18`, `GIT_COMMON_DIR=/mnt/d/codex-call/.git`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`. No pointer/config/index repair, index refresh or Git metadata write occurs. Preexisting modified Control and untracked history/prompts/helpers are not this agent's changes.

## 6. Owned residuals and stop

| Residual | Owner | Destination / boundary |
| --- | --- | --- |
| Independent final fresh peer-isolated A/B re-review and plan acceptance | Controller | Pending; this fix neither self-passes nor dispatches/request reviews or authorizes implementation. |
| Unindexed global occurredAt/id sort and bounded timeouts | Backend/DBA | Authorized implementation report and existing capacity assessment; no performance/readiness claim, index or migration. |
| Snapshot-relative employee revocation; fresh continuation snapshots | Backend/Security | Existing accepted semantics and future implementation evidence, not linearizable revocation or cross-page snapshot pinning. |
| Connector rejection before filters | Backend | Future real HTTP/direct-filter probes; no admission relaxation to manufacture a status. |
| Independent verifier, employee/read-login provisioning and credentials | Security/Operations IAM and DBA/Backend | External prerequisites; absence fails503; no credential, role or grant work. |
| Existing shared-ring custody/rotation and AEAD nonce uniqueness | Security/Backend | Existing subject-read secret process and future implementation report; no separate activation secret. |
| Toolchain/Docker/network and mandatory runtime validation | Controller/authorized implementer | Pending implementation validation; no waiver, skip or readiness inference. |
| Unexpected frozen/schema/privilege/API requirement | Controller/user | Stop for renewed authorization; no silent scope expansion or V018. |

Exact agent-changed paths:

1. `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md` — cumulative corrections described above.
2. `docs/gateflow/pa34-operations-activation-history-plan-fix-3.md` — this new evidence artifact, outside S1.

**Runtime NOT RUN.** No build/Maven/test/application, Docker/PostgreSQL, generated XML, JAR/firewall smoke, install, contract/client generation or acceptance validator is run or claimed. Static source/regex/hash checks do not substitute for runtime evidence. No source, test, migration, OpenAPI, docs/12–19, POM, CI, Control, review, fix-1/fix-2, prompt, helper, Git metadata or target/tmp output is edited.

No implementation, stage, commit, push, branch/PR/comment/approval mutation, review request, reviewer dispatch or later-gate entry. **Bounded fix complete; final A/B re-review pending; stop for the Controller.**
