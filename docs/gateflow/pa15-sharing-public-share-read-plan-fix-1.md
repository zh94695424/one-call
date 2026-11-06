# PA15 Public Share Read — Plan Fix 1

- Task label: `pa15-public-share-read-plan-fix1-gpt-20261106`.
- Work unit: `PA15-sharing-public-share-read`; date: 2026-11-06.
- Provider/requested model: GPT / `gpt-6.1-sol`, high, as dispatched by Control; no independent runtime model-identity claim.
- Gate/status: `fix` following `plan review`; bounded documentation correction complete, no gate advancement or review verdict.
- Authority: binding Goal Confirmation and current review adjudication in `docs/gateflow/project-execution-control.md`; independent reviews `docs/reviews/plan-review-20261106-035347.md` and `docs/reviews/plan-review-20261106-043640.md`.
- Read the target plan through EOF before editing, both reviews, the active Control checkpoint, and directly relevant architecture, route, authentication, CustomerContext, OpenAPI, V009/V010 and evidence-reader contracts. No independent implementation/runtime verification is implied.

## Exact Write Scope And Identity

Only these two paths are changed by this task:

1. `docs/gateflow/pa15-sharing-public-share-read-plan.md` — existing input plan revised in place (already untracked at intake).
2. `docs/gateflow/pa15-sharing-public-share-read-plan-fix-1.md` — this new durable fix artifact.

| Artifact | Before | After |
| --- | --- | --- |
| Target plan SHA256 | `cc8ab0499f9d8a6566b1d1502567e2a81595cabb39673b8dad38162a8e2c0244` | `65056012664b687d76295913cafc4d0f8870c82c95740a2319671e7a8751d7b7` |
| Target plan line count (`wc -l`) | 300 | 371 |
| Fix artifact | Absent; no before hash or line count (not an empty file) | Final SHA256 and line count are measured after writing/rereading and recorded in the completion report; no self-referential hash is embedded here. |

## Accepted Resolutions

- **B-F-01 — route/error precedence:** §4 now separates sanitized route 404 `NOT_FOUND` for percent-encoded/URI-encoded whitespace, semicolon/matrix, duplicate-slash and other noncanonical presentations from canonical valid-shaped unresolved-token 404 `SHARE_TOKEN_INVALID`. Rejected routes precede shape/provider/service lookup with zero verifier/digest/query calls. Removed the HTTP whitespace-bearing digest claim; exact whitespace/normalization distinctions remain only for directly constructed service inputs. Pinned both codes/precedence in existing `exactPublicGetRoutePrecedesOptionalAuthentication`, `tokenShapeAndLookupFailuresUseClosedProblems` and `publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse`, without new methods. Found-but-before-validFrom concealment is explicitly a later state decision, not a route rejection.
- **B-F-02 — architecture inventory:** new §3 architecture subsection freezes exactly the eight `PUBLIC_READ_FILES` paths, disjoint from unchanged 13 customer-query and 19 command paths; command discovery excludes both read sets. Exact source inventories were compared to `SharingQueryBoundaryTests` before writing and mechanically rechecked afterward. Public-query top-level API is exactly `OpaqueCapabilityToken`, `PublicShareQueryException`, `PublicShareQueryService`, `PublicShareView`, with nested safe types explicitly enumerated; original customer-query/customer-command inventories and signature/component assertions are preserved.
- **B-F-02 — architecture bans:** publicquery has no jOOQ/DSLContext, JDBC/DataSource/Hikari, SQL writes/locks, audit/Outbox/access append, IAM/organization imports, secret logging, wildcard SELECT, digest-column or snapshot-column strings. Public persistence has no write/DDL/writable CTE, locks, append, command invocation, cross-module direct SQL or logging. Only `JooqPublicShareQueryRepository` may contain `capability_token_hash`/`content_snapshot` within the public-read partition, solely for the frozen prepared SELECT; no digest selection/logging or weakening of old bans. Existing read-only/role SQL and command DSL/manager qualifiers remain allowed.
- **A-OQ-3 folded into B-F-02 — CustomerApiBoundaryTests precision:** exactly 25→26 internal files; writer sharing imports exactly existing `ShareCommandException` plus `PublicShareQueryException`; only `PublicSharesController` joins the existing sharing-import exception list, and its sharing imports are restricted to the named public-query API. Exact web/servlet-conditional list grows four→five by adding that controller. Original customer controller/import/reflection/SQL/profile/verifier restrictions remain. Verified against `CustomerApiBoundaryTests` and retained `ModuleBoundaryTests`' exact map, 14 modules, two production edges and negative fixtures, with only the planned third named interface.
- **A-F-02 — membership semantics:** removed the extra selected-workspace equality/PERSONAL denial. Successful optional verification plus current ACTIVE account and current ACTIVE grant-organization membership from trusted CustomerContext governs content eligibility; selection is not an additional gate. Existing member web/IT methods explicitly require content for an active grant-tenant member with a well-formed PERSONAL or different selected ORGANIZATION workspace, including legacy/no selection. Nonmember/inactive organization/revoked membership retains metadata/challenge; disabled/missing account retains 401. Client selectors remain non-authoritative; malformed trusted verifier/session results and verifier failures retain existing fail-closed boundary behavior, not a new public session policy.
- **A-OQ-1 — OPEN risk:** §6 and existing `prefetchAndNegotiationHaveNoAuthorizationSideEffects` / `repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged` require identical status/JSON envelope for equal grant/mode/session inputs under CLEAR versus OPEN. Eligible member content stays present; nonmember metadata/challenge is unchanged. Risk SQL/policy calls remain zero, not just an absence of a risk error.
- **A-OQ-4 — validFrom:** §6 and existing `stateAndDatabaseTimeChecksNeverMutateGrant` / `publicGetKeepsRevokedExpiredAndLockedPageSemantics` require both ACTIVE and LOCKED grants sampled before validFrom to return 404 `SHARE_TOKEN_INVALID`, with neither metadata nor content, no member/evidence calls and no persistence mutation; IT requires byte-identical persistence snapshots.

## Preserved Boundaries And Inventory

- Confirmed goal/non-goals unchanged: only observation-free `GET /api/v1/public/shares/{capabilityToken}`; one end-to-end slice, no admission/ticket/media/Redis or write behavior.
- All 23 numbered future implementation paths preserved exactly; no path added/removed or authorization to implement. Architecture subsection only refines the already allowlisted tests/API.
- Current source: 16 Surefire classes / 79 methods + six Failsafe classes / 118 methods = 197. Future unchanged: 19 / 91 + seven / 126 = 217, net +20 methods / +4 classes; new methods remain 6+4+2 Surefire and eight Failsafe, all exact original names. Existing architecture methods extend in place, not new methods/classes.
- V001–V010, OpenAPI, source, tests, configuration, dependencies, docs/12, docs/14, Control and both reviews remain untouched. V010 command-pool/role reuse, evidence-owner MANDATORY reader and PostgreSQL-enforced read-only transaction design remain unchanged; no ACL or JDBC-hint-only substitute.
- Rejected A-F-01 received no plan change: the existing `/mnt/d/codex-call/.git/worktrees/codex-call15` target is used only through process-local read-only Git environment. No `.git` rewrite. Control's existing/new path bookkeeping is not acted on.

## Validation Actually Performed

- Intake `sha256sum`, `wc -l` and absence check recorded the target's 300-line original identity and absent fix artifact. Default Git invocation exposed the known Windows pointer diagnostic; existing Linux Git-dir was confirmed and used without changing it.
- Read the current three architecture sources before freezing inventories. Read-only Python assertions verified exact ordered 13/19 path lists against source, exact public eight against the future allowlist, disjoint 8/13/19 partitions, and 23 distinct numbered allowlist rows.
- Read-only source recount with unconditional `@Test`/`void` extraction reproduced 79/16 and 118/6. Plan table extraction confirmed 12 unique Surefire methods (6+4+2) and eight unique IT methods; future 91/126/217 remains planned, not executed.
- Read-only Git commands use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15 GIT_WORK_TREE="$PWD" GIT_OPTIONAL_LOCKS=0`: `git diff --check`, `git diff --name-only`, `git status --short`; additionally `git diff --no-index --check -- /dev/null <artifact>` for each untracked documentation file so tracked-diff emptiness is not mistaken for whitespace validation. Tracked checks pass with empty diff; no-index checks return 1 for added content without whitespace diagnostics, independently checked by text assertions. Git emits LF/CRLF conversion warnings, not whitespace errors.
- Compared final status to intake: tracked diff remains empty; only the new fix artifact is added to the pre-existing untracked plan and private `.codex` entry. The latter is not inspected or modified. Exactly the two authorized documentation paths receive task writes; no staging/index mutation.
- Reread both final files through EOF; checked trailing whitespace/tabs, final SHA256 and `wc -l`. Completion report records both final artifact identities.
- Not run: Maven, Docker, PostgreSQL, Testcontainers, packaged-JAR smokes, remote CI or network operations. No source/test/runtime result or production-readiness claim.

## Residual Risks And Stop State

- Security/Identity owns the absent production session verifier; optional supplied credentials remain fail-closed until an approved provider exists. No provider or new session policy is implemented here.
- Sharing Backend/Database/Platform own future PostgreSQL read-only/role-switch/PID/SQLSTATE 25006, role cleanup, pool reuse and regression evidence. Command-pool capacity and dual-role provisioning remain operational concerns; dedicated least-privilege public-read ACL belongs to a later Security/Schema work unit.
- Security/Product owns future token-format representability under the canonical URI gate; PA15 does not invent a production alphabet/generator. Sharing/Product owns later admission/media rechecks because READ COMMITTED page observation is not an authorization lease.
- Documentation correction only. No source, tests, runtime configuration or later-gate action occurred. No stage, commit, push, branch/worktree creation, PR, merge, approval, ready action, reviewer request, external comment or issue mutation; no re-review/implementation dispatch. Gate remains `fix` for Controller intake.
