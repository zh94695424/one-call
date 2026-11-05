# PA15 Public Share Read — Plan Fix 3

- Task label: `pa15-public-share-read-plan-fix3-gpt-20261106`.
- Work unit: `PA15-sharing-public-share-read`; date: 2026-11-06.
- Gate/status: `fix` after fixed-plan A/B re-review; final bounded precision correction complete for Controller intake only. No review verdict or gate advancement.
- Authority: active Control checkpoint and accepted re-review adjudication in `docs/gateflow/project-execution-control.md`; target plan, fix-1, fix-2, original reviews `20261106-035347`/`20261106-043640` and fixed-plan re-reviews `20261106-051000`/`20261106-051649` in `docs/reviews/`.

## Exact Write Scope And Identities

Only these two repository paths receive task writes:

1. `docs/gateflow/pa15-sharing-public-share-read-plan.md` — existing untracked plan modified in place.
2. `docs/gateflow/pa15-sharing-public-share-read-plan-fix-3.md` — this new artifact, absent at intake.

| Artifact identity | Before | Final |
| --- | --- | --- |
| Plan SHA-256 (LF repository content) | `8157aaaaafb4827b277f81d17ab3249e033b1ce99733bd8dfd00f5f32b203417` | `07605141492b281337fc37046c01cd5995c071867970d5fa7bb17ec7b38d26ae` |
| Plan line count (`wc -l`) | 372 | 372 |
| Fix-3 SHA-256 / line count | Absent; no before identity | Final byte hash and line count are measured after the EOF reread and recorded in the completion report; no self-referential hash is embedded in the hashed artifact. |

## Accepted Resolutions And Exact Sections Changed

- **A R-F-01 — repository/transaction wiring:** §3, Frozen Architecture Inventory And Bans, line151, and §5, Minimal API Surface, line183. `PublicShareQueryRepository` retains exactly `findByTokenDigest(String)` and `sampleDatabaseTime()`, plus already permitted explicit role/read-only assertion helpers; no interface method takes DSLContext. `JooqPublicShareQueryRepository` is constructor-wired with existing qualified `sharingCommandDsl` backed by TransactionAwareDataSourceProxy. The service receives that same qualified DSL and `publicShareQueryTransactionTemplate`, performs the frozen read-only/role setup/assertions/restore, calls the no-DSL repository, and passes the DSL only to the existing MANDATORY evidence-owner reader. Replaced both ambiguous repository-DSL handoff clauses; no alternate pool/manager/DSL or new abstraction.
- **A OQ-1 — exact controller:** §4 line171 freezes `public PublicShareView getPublicShare(@PathVariable("capabilityToken") String capabilityToken, Principal principal)` and unchanged `@GetMapping(path="/api/v1/public/shares/{capabilityToken}", produces="application/json")`. Constructor uses `@Qualifier("publicShareQueryService") ObjectProvider<PublicShareQueryService>`, `@Qualifier("customerContextService") ObjectProvider<CustomerContextService>` and the existing problem writer. Query service resolution is lazy; customer context resolution occurs only in the authenticated organization-member callback. Missing providers/services remain SERVICE_UNAVAILABLE, never downgrade or partial content. No repository/DSL/command/HTTP/session fields. §3 line155 assigns exact mapping/return/method/provider-role assertions to architecture tests; behavior tests retain runtime fail-closed ownership.
- **A OQ-4 — all CI count sites:** §9, Existing Tests And Source/CI/XML Arithmetic, line323 explicitly names `.github/workflows/ci.yml` current lines162–163 (early Surefire condition and diagnostic, both 79→91), 366 (unit exact_inventory 79→91), 367 (Failsafe exact_inventory 118→126) and 368 (PA15 print summary: 19/7 classes, 91/126 methods, `64+10+10+8+8+18+8=126`, combined217). Atomic update, source=CI=XML equality and zero-skip/failure/error checks remain mandatory. Historical PA9 aggregate64 stays64. This is a future edit instruction; CI is not edited here.
- **A OQ-3 — mixed README clauses:** §9 line325 names only current-count clauses at `backend/README.md` current lines40/291/300/322: 19 Surefire classes/91 methods, seven Failsafe classes/126 methods, breakdown `64+10+10+8+8+18+8`, combined217; PA15 adds exactly12 Surefire and eight Failsafe methods. Preserve every historical clause, especially PA9 64 and ten separate Surefire tests co-located at322, PA12 159 and PA14 slice-1 185/revoke191/rotation197 co-located at300, and history at40/305/313. Removed the instruction to update cumulative history at305. README is not edited here.
- **A OQ-5 — validation only:** §10 line350 retains the documented Linux read-only Git environment with an explicit pre-use check that live `git rev-parse --git-dir` matches `/mnt/d/codex-call/.git/worktrees/codex-call15`; re-confirm the actual directory if it changes rather than hardcoding another worktree. No `.git` rewrite or new worktree. This is process validation, not a reopened path finding.
- **Rejected A OQ-2 unchanged:** recorded hashes are over LF repository content; isolated clone checkout materialization can differ (CRLF). No review or Control hash/text change, no renewed question and no plan-design correction for this rejected item.

## Preserved Boundaries And Inventory

- One observation-free public GET slice; exact unchanged 23-path future implementation allowlist (nine existing, fourteen new). Exact disjoint 8 public-read / 13 query / 19 command partitions and all existing bans remain; only the fix-2 service-level DSL exception is permitted.
- Fix-1 route NOT_FOUND versus token-domain SHARE_TOKEN_INVALID, current grant-tenant membership independent of selected workspace, equal OPEN/CLEAR response shape and before-validFrom 404 semantics remain. State/projection/no-write proof and all named test rows are unchanged.
- Current source remains 16 Surefire classes/79 methods and six Failsafe classes/118 methods =197. Future remains 19/91 and 7/126 =217: exactly12 Surefire (6+4+2) and eight Failsafe methods, net+20 methods/+4 classes. Architecture methods extend in place; no names or inventories changed.
- No V001–V010, ACL, OpenAPI, source, tests, configuration, dependency, fix-1/fix-2, Control or review edit. No admission, ticket, media, Redis, provider design, production token format or new abstraction.

## Validation Actually Performed

- Read active Control, the 372-line input plan, fix-1/fix-2 and both original/fixed-plan A/B reviews. Inspected actual CustomerSharesController, lazy customerContextService bean and getCustomerMe API, command DSL/template wiring, proposed public repository, evidence reader/interface with MANDATORY sharingCommandTransactionManager, current CI and mixed README clauses before correction.
- Confirmed existing ObjectProvider/Qualifier/Principal APIs and fail-closed BeansException pattern; no eager customer dependency is required. Verified transaction-aware command DSL and caller-DSL evidence reader support the frozen constructor/no-DSL repository design without changing existing APIs. These are source-level checks, not compilation/runtime claims.
- Plain Git first exposed the existing Windows pointer diagnostic. Process-local GIT_DIR/GIT_WORK_TREE with GIT_OPTIONAL_LOCKS=0 resolved `git rev-parse --git-dir` to the exact documented Linux directory before read-only diff/status validation; `.git` remained unchanged.
- Read-only assertions compared all23 allowlist rows and all8/13/19 partition entries byte-for-byte with intake, checked query/command paths against architecture source, confirmed9 existing/14 new, and compared unchanged state/projection/test rows. CI-compatible source extraction recounted16/79 and6/118; named rows contain12 unique Surefire and8 unique IT methods. Recomputed79+118=197,79+12=91,118+8=126,91+126=217 and the126-method breakdown.
- Exhaustive current CI count search found only lines162/163/366/367/368; all are now named. Inspected all four current README count lines and their historical clauses; no instruction alters historical64/159/185/191/197. Source=CI=XML and zero-skip/failure/error requirements remain future acceptance criteria.
- Read-only whitespace/hash/line-count/diff/path checks include the untracked outputs explicitly, not just an empty tracked diff. Final outputs are reread through EOF; SHA256/wc values are measured after writing. LF/CRLF checkout warnings do not change the LF hash convention. Only the authorized plan and new fix artifact receive task writes; pre-existing private `.codex` is neither inspected nor changed.
- Not run: Maven, Docker, PostgreSQL/Testcontainers, packaged-JAR smokes, runtime tests, network or remote CI. No source/runtime result or production-readiness claim.

## Residual Risks And Stop State

- Security/Identity owns the absent production session verifier. Backend/Database/Platform own future PostgreSQL first-SQL/read-only/PID/role/25006/pool-reset and regression proof, dual-role provisioning and command-pool capacity. Dedicated least-privilege public-read ACL remains a later Security/Schema work unit.
- Security/Product owns production token-format representability; Sharing/Product owns admission/media rechecks because READ COMMITTED observation is not an authorization lease. No new risk is resolved by claiming runtime execution here.
- Documentation-only precision fix; gate remains `fix` for Controller intake. No stage, commit, push, PR/worktree/branch creation, merge, approval, ready action, reviewer request, external comment, issue mutation, re-review dispatch, implementation or later-gate action.
