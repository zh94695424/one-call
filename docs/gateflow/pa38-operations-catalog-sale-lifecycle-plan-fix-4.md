# PA38 plan fix4 — authorized CustomerBoundaryFilter scope closure

## 1. Identity, inputs, and disposition

- Task: `pa38-operations-catalog-sale-lifecycle-plan-fix4-gpt-20261118-01`.
- Work unit / slice: `PA38-operations-catalog-sale-lifecycle` / `PA38-S1 catalog publication and offsale`.
- Date: November 18, 2026, Asia/Shanghai.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`.
- Branch: `codex/pa38-next-work-unit`.
- Controller bookkeeping HEAD, resolved from both `HEAD` and `49d42b2`: `49d42b2a154e9a445a34ef0a0801076a6601e208`; no branch/HEAD change.
- Original plan SHA256: `19eb9e7d6a4585c0ebb5025729e2379896fcafdc41e92270fc6a0047ba306c17`.
- Final cumulative plan SHA256: `4b1edeb9f19d12471e6d034332939fcd44e3d6df7f1a37883159ed367e49f88d`; size: **103,654 bytes**.
- Frozen blocked implementation artifact SHA256: `c45b3ad3769aab00f1b3f52f2b83246ba98e00c878d4fef4c0741b886252e16d`, unchanged.
- Status: **`plan-fix-complete`**, author hand-back only; stop before independent A/B plan review, feasibility, implementation, code review or any later gate. No acceptance or runtime closure is claimed.

Binding scope authority is the active Control checkpoint, `docs/gateflow/project-execution-control.md:123-125`: the user authorized exactly one additional existing production path on November 18, 2026. The historical implementation artifact remains frozen evidence of the previously blocked attempt, not a document to rewrite as successful. No standalone A/B review artifact/conclusion is an input or output of this fix.

Exactly two documentation paths are changed by this task:

1. `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan.md` — cumulative plan update.
2. `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan-fix-4.md` — this closure report.

## 2. Closure matrix

| Obligation | Exact cumulative plan destination | Closure / evidence distinction |
| --- | --- | --- |
| Close `PA38-S1-BLOCK-01` without expanding the goal | Status; §1; §13 | Closed at the scope/specification level by explicit user authorization. Both existing confirmed routes require the downstream filter pass-through; no route or public contract is added. Executable closure remains a later implementation obligation. |
| Sole additional allowlist entry | §4.2, fifth production bullet; §2.2; §4.3 | Only `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java` is added. All previous paths are retained; no source is edited in this task. |
| Exact path/count arithmetic | §2.2; §4.2–4.3; §10.2–10.3; §13 | Historical **30 = 22 new + 8 modified** becomes **31 = 22 new + 9 modified**. Production changed paths become **11 new + 5 modified = 16**; tests stay **11 new + 2 modified = 13**; build/CI stay **2 modified**. Hence 16+13+2=31, with the same 22 new files and eight previously modified paths. |
| Real chain and exact Customer behavior | §5.1 source lockstep; new §5.4 | Operations REQUEST filter remains first, Customer REQUEST filter remains second/global. Only the two exact POST servlet shapes with context+URI equality continue the existing chain before customer trace/headers/authentication/buffering. No customer or employee authority decision is added. |
| Full-chain test evidence, zero identities added | §9 introduction; §9.1 unchanged identities; §9.3 fix4 assertion ownership; §11.1; §13 | The real Operations then Customer filters precede the real controller in the same 14 planned web methods. Positive commands, auth401/503, provider503, route404, key-free commands and response transparency are explicitly assigned below. These tests are specified, not executed here. |
| Frozen source-literal/equality oracles | §5.1; §5.4; §9.3; §11.1; §12 | Preserve OperationsLifecycleBoundaryTests, OperationsSubjectsWebTests, OperationsActivationReadBoundaryTests and OperationsIvrSessionReadBoundaryTests unchanged. Existing early subject/GET branches and exact literals/equalities remain; no new editable test path. No bounded-pass-through contradiction was found in those source checks. Separate baseline IVR line207 failure is owner-classified, not fixed or waived. |
| Manifest and completion requirements | §10.2–10.3; §13 | Guarded count remains310. Recompute the LF-normalized sorted manifest SHA256 only after all 16 production changed paths exist, including CustomerBoundaryFilter; no future digest is guessed. Completion must supply the exact chain proof and distinguish plan-level closure from runtime results. |

Changed plan sections are the identity/status header, **§1, §2.2, §4.2, §4.3, §5.1, new §5.4, §9 introduction, §9.3, §10.2, §10.3, §11.1, §12 and §13**. All §9.1/§9.2 class/method identities remain byte-identical to the original plan.

## 3. Exact chain contract and evidence ownership

Read-only source evidence confirms:

- `OperationsWebConfiguration.java:21-25`: `/api/v1/operations/*`, REQUEST, `Ordered.HIGHEST_PRECEDENCE`.
- `CustomerWebConfiguration.java:47-53`: `/*`, REQUEST, `Ordered.HIGHEST_PRECEDENCE + 1`.
- `OperationsBoundaryFilter.java:78-80`: admitted requests continue through `chain.doFilter(trusted, buffered)`; no bypass of Customer occurs.
- `CustomerBoundaryFilter.java:48-68`: current early subject POST/Operations GET pass-throughs; the later customer allowlist/not-found branch blocks catalog POSTs today.
- `OperationsCatalogProductReadTestProviders.java:59-63` and `OperationsSubjectsWebTests.java:44-46`: real Operations-then-Customer test-chain precedent.

The only future Customer addition is a separate early branch using the existing servlet-path variable, before customer trace/header/authentication setup:

```java
"POST".equals(request.getMethod()) && operationsPath != null
        && operationsPath.matches("/api/v1/operations/catalog/versions/[^/;%]+/(publications|offsales)")
        && (request.getContextPath() + operationsPath).equals(request.getRequestURI())
```

Its action is only `chain.doFilter(request, response)` followed by immediate return. Preserve the incoming Operations request/principal/trace and response, existing subject POST/Operations GET pass-throughs and every other customer not-found/protection behavior. No prefix match, wildcard catalog-version route, encoded slash/segment relaxation, alternate method, body inspection, authority test, employee/customer authentication, generic Operations bypass, response translation or filter reorder is permitted. Canonical UUID/body validation remains with the existing Operations parser; unsafe route alternates404 do not redefine admitted-shape validation422.

Future `OperationsCatalogVersionWebTests` evidence is pinned inside the already planned identities, without a new method, annotation, class or path:

| Existing planned method(s) | Required full-chain subcases |
| --- | --- |
| `admitsExactPublicationPost`, `admitsExactOffsalePost` | Valid Operations credentials/principal and canonical requests reach the real controller and command once for each route, with root/nonempty context paths and no customer-verifier call. |
| `authenticationPrecedesStructuralParsing` | Missing/malformed/invalid/expired/revoked/wrong-audience employee auth yields exact Operations401; unavailable/missing/ambiguous/throwing employee verifier yields sanitized503 before controller/command/authority work. |
| `structuralValidationPrecedesAuthorityWitness` | Valid syntax/auth with missing or ambiguous command provider yields sanitized503, zero command/authority work; structural precedence remains. Configuration provider tests remain complementary, not substitute chain proof. |
| `concealsOtherCatalogMethodsAndUris`, `classifiesReadsSubjectsAndCatalogCommandsSeparately` | Unsafe encoded/semicolon/empty/duplicate/trailing/extra segments, URI/context mismatch, alternate methods and non-allowlisted catalog mutations yield exact sanitized404 without any controller/command call. Keep subject POST/Operations GET controls. Direct Customer sentinel subcases prove bounded forwarding and unchanged customer404 for nonmatches. |
| `catalogCommandsDoNotRequireIdempotencyKey`, `subjectCommandsRetainExactIdempotencyRule` | Both catalog commands work without the key; its presence adds no requirement/identity/row. Subject key behavior remains exact. |
| `successHasExactStateAndHeaders`, both exact409 methods, media/Accept method and denial subcases | Exact Operations status/problem/body/trace/media/length/headers remain unchanged. Direct sentinel checks same request/response objects, untouched status/body/headers and zero customer authentication; no Customer wrapper/translation. |

The frozen tests require existing `operationsPath.matches` before customer trace initialization, prohibit employee authentication references in Customer, and retain GET/subject/activation/IVR literals and context-path URI equality. A separate early branch leaves those exact assertions satisfiable. If implementation discovers a contradiction, it must report the exact source assertion and stop for Controller rather than edit any of the four tests.

## 4. Unchanged inventories and frozen boundaries

Final targets remain Java **290/180** (baseline279/169), Surefire **97/934**, Failsafe **38/630**, focused **135/1,564**, source scan **1,565** with the same one historical comment hit, POM includes **38**, modules/discovered edges **14/13**, catalog allowed dependencies **5**, production resources21, test resources1 and migrations18/no-V019. The production guard remains **310 = 290+21-1**; current historical guard299/digest is not a final implementation digest.

No production source, test, POM, CI, OpenAPI, migration, schema, ACL, Control, review artifact, implementation artifact, previous fix artifact, private `.pa38-*`/`.codex`, wrapper or generated output is edited/created by this task. In particular both filter registrations/configurations, all four frozen regression tests, docs12/14/15/17, V001–V018 and the PA25 runtime identity file remain unchanged. Existing untracked private helpers are preserved, not staged or treated as candidate changes. No staging, commit, branch change, push, PR/issue mutation, merge, approval, readiness mark, reviewer request or external comment occurs.

## 5. Non-mutating validation and command outcomes

Git inspections use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, `GIT_WORK_TREE` equal to this worktree, and `GIT_OPTIONAL_LOCKS=0` for final checks. These are process-local read-only overrides; no `.git` or config file is repaired. Existing `core.autocrlf=true` is preserved; `-c core.safecrlf=false` suppresses advisory warnings without changing content normalization or repository configuration.

| Command/check executed | Exit / result |
| --- | --- |
| Initial ordinary `git branch --show-current`, `git rev-parse HEAD`, `git rev-parse 49d42b2`, `git status --porcelain=v1` | Each failed128 because `.git` contains a Windows `D:/...` gitdir; recovered with the explicit Linux-resolved metadata path. No metadata mutation. |
| Corrected branch/full-HEAD/status inspections; pre-edit `git diff --quiet HEAD --` | 0; correct branch/full HEAD and clean tracked candidate; only pre-existing untracked private helpers. |
| `sha256sum` of original plan/blocker; `wc`, `rg`, `sed`, `cat`, read-only path/source checks | 0; both required SHA256 values match, original allowlist/count literals located by grep before editing, real chain/source constraints inspected. |
| Read-only Python allowlist/identity/hash assertions using `git show HEAD:<plan>` and filesystem existence | 0; original30/final31 unique paths, sole authorized addition, all prior paths/new22 preserved, production11+5/test11+2/build2, unchanged planned identities, unchanged blocker hash and no stale active arithmetic. |
| Exploratory `git -c core.autocrlf=false diff` checks; suppressed targeted CI-file reproduction | Whitespace check2 / difference check1; invalid normalization choice exposed pre-existing checkout CRLF as apparent changes/whitespace. Discarded as scope evidence, not a source defect. Re-run with existing normalization; no files converted or changed. |
| Corrected `git -c core.safecrlf=false diff --check` | 0; no patch whitespace errors. |
| Corrected changed-path enumeration and `git diff --quiet HEAD -- .` excluding exactly these two authorized docs | 0; only the cumulative plan is tracked-modified; this new report is the sole task-created untracked path; every other tracked path is unchanged. |
| Final branch/HEAD, path-set/count/stale-literal assertions, `sha256sum`, `wc -c` | 0; final identities/sizes/digests are included in the hand-back; no implementation file or later-gate artifact changed. |

No Maven, Docker, PostgreSQL, implementation test, CI oracle or generated-output command is executed. The reported earlier 44-test/one-failure run belongs exclusively to the frozen implementation artifact, not this plan fix. This report does not claim tests passed or gate acceptance.

## 6. Residual owners and destinations

| Owner | Residual / destination |
| --- | --- |
| Controller | Renewed peer-isolated independent plan review and subsequent authorized dispatch are still required. Plan-level blocker closure grants no implementation permission in this turn. |
| Backend Operations IAM / Catalog-order | Later implementation must supply the §5.4/§9.3 real-chain evidence and recompute the310-entry manifest digest after all production changes; none is executed or guessed here. |
| Backend test-boundary owner + Controller | Frozen IVR boundary line207 baseline failure remains unresolved; baseline validation intake before implementation acceptance. No test edit/waiver is authorized. |
| Backend; Platform/Operations + Security | Same-pool bounded write/read proofs remain mandatory implementation evidence; sustained four-slot LO pool saturation/capacity/read503/recovery observations remain deployment-readiness work. No production SLO is claimed. |
| Database/Security + Catalog-order | Frozen catalog-role over-privilege/effective ACL drift: exact V004 matrix/negative probes later; no schema/ACL correction here. |
| Platform/Security/Operations; Security; Platform | Production credentials/enablement/memberships/pool health, verifier/lock-free authority timing and audit/Outbox retention/dispatch/monitoring remain their existing deployment/security/A3 destinations. |
| API governance / future-slice owners | Draft authoring, trial policy, customer catalog filtering and UI confirmation remain out of scope. |

**Stop: `plan-fix-complete`, before independent A/B review and every later gate.**
