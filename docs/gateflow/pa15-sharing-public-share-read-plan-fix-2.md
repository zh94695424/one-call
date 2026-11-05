# PA15 Public Share Read — Plan Fix 2

- Task label: `pa15-public-share-read-plan-fix2-gpt-20261106`.
- Work unit: `PA15-sharing-public-share-read`; date: 2026-11-06.
- Role: GPT plan-fix author, continuation fix 2; gate/status: `fix`, bounded documentation correction complete for Controller intake, not gate advancement or a review verdict.
- Authority: current PA15 checkpoint in `docs/gateflow/project-execution-control.md`, both independent reviews `docs/reviews/plan-review-20261106-035347.md` and `docs/reviews/plan-review-20261106-043640.md`, and `docs/gateflow/pa15-sharing-public-share-read-plan-fix-1.md`.

## Accepted Intake Defect And Exact Resolution

Fix 1's blanket `org.jooq`/`DSLContext` ban across all five `publicquery/` files contradicts the approved service transaction orchestration and the existing `ShareableEvidenceProjectionReader.readShareableProjection(DSLContext, UUID)` contract. `JooqShareableEvidenceProjectionReader` uses its caller-supplied DSL and is MANDATORY on `sharingCommandTransactionManager`; removing that argument or introducing a replacement reader/path is not authorized.

- §3, **Frozen Architecture Inventory And Bans**, old line 150 becomes final lines 150–151: retain all-five prohibitions, make DML/DDL/writable CTE/function, command invocation and row/advisory-lock bans explicit, and permit only `PublicShareQueryService.java` to import/use `org.jooq.DSLContext` for active transaction repository/evidence-reader orchestration and the exact §5 read-only/current-role setup/assertion/restore calls. No other jOOQ type/use is allowed in `publicquery/`.
- Freeze the negative assertion inside the existing `SharingQueryBoundaryTests` method: `package-info.java`, `OpaqueCapabilityToken.java`, `PublicShareQueryException.java` and `PublicShareView.java` under `publicquery/` remain free of `org.jooq`/`DSLContext`; any jOOQ addition to any of those four fails. This is a future assertion requirement, not an implemented test.
- §5, **Minimal API Surface**, old line 182 becomes final line 183: clarify that `PublicShareQueryRepository`, through `JooqPublicShareQueryRepository`, alone performs the frozen grant/database-time SELECTs. The service supplies the same active transaction DSL to repository orchestration and the existing MANDATORY evidence-owner reader. Repository operations, collaborator contract, transaction manager, template, bean qualifiers and paths are unchanged; no new abstraction or method signature is introduced.
- The service cannot select grant/database-time data directly, contain `capability_token_hash`/`content_snapshot` column strings, access foreign tables, write, lock, invoke command services/repositories, append audit/Outbox/access events, log secrets or use wildcard SELECTs. Existing persistence bans and the sole digest/snapshot-column exception in `JooqPublicShareQueryRepository` are unchanged.

## Exact Write Scope And Artifact Identities

Only these repository paths receive task writes:

1. `docs/gateflow/pa15-sharing-public-share-read-plan.md` — existing untracked plan modified in place.
2. `docs/gateflow/pa15-sharing-public-share-read-plan-fix-2.md` — this new fix artifact; absent at intake, not an empty baseline file.

| Artifact identity | Before | Final |
| --- | --- | --- |
| Plan SHA-256 | `65056012664b687d76295913cafc4d0f8870c82c95740a2319671e7a8751d7b7` | `8157aaaaafb4827b277f81d17ab3249e033b1ce99733bd8dfd00f5f32b203417` |
| Plan line count (`wc -l`) | 371 | 372 |
| Fix-2 SHA-256 / line count | Absent; no before identity | 51 lines; final byte hash is measured after the EOF reread and recorded in the completion report, not self-embedded because that would change the hash. |

## Preserved Decisions And Boundaries

- Exact disjoint sharing partitions remain 8 public-read / 13 customer-query / 19 command paths. The closed future implementation allowlist remains exactly 23 paths, nine existing plus fourteen new; one end-to-end slice. Named API inventories, existing architecture method identities and counts are unchanged.
- All fix-1 canonical-route versus token-domain error precedence, current ACTIVE grant-tenant membership without an extra selected-workspace gate, equal CLEAR/OPEN response shape, and before-`validFrom` 404 concealment decisions survive byte-for-byte outside the two corrected locations.
- Current source remains 16 Surefire classes / 79 methods and six Failsafe classes / 118 methods = 197. Future additions remain 6+4+2 Surefire methods and eight Failsafe methods, yielding 19/91 and 7/126 = 217; net +20 methods / +4 classes. These are planned counts, not runtime results.
- No change to V001–V010, ACLs, OpenAPI, configuration, dependencies, source, tests, fix 1, Control or reviews. Observation-free GET, no admissions/tickets/Redis/media, no-write/no-lock/no-cross-module/no-secret-logging/no-SELECT-* boundaries remain frozen.

## Validation Performed

- Read the current plan through EOF, active Control checkpoint, both reviews and fix 1; inspected the evidence reader implementation/interface and current `SharingQueryBoundaryTests` inventory/bans before editing. The existing caller-DSL/MANDATORY seam and §5 orchestration are now mutually implementable at plan level; actual PostgreSQL behavior still needs future evidence.
- Read-only Python 3 assertions reversed exactly the two local corrections and recovered the intake plan SHA-256 and 371 lines, proving every other byte unchanged. Compared the exact 13/19 ordered lists with architecture source and the actual current sharing files; checked disjoint 8/13/19, all 23 distinct numbered paths, 9+14 existence classification and one slice.
- Recounted current unconditional `@Test void` methods as 79/16 and 118/6; extracted the unchanged twelve/eight unique future method names and 6+4+2 distribution; verified 79/118/197 -> 91/126/217, +20/+4. Checked explicit four-file jOOQ-negative requirement and all service no-write/no-lock/foreign-table/command/logging/column-string restrictions.
- Used process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call15`, `GIT_WORK_TREE` and `GIT_OPTIONAL_LOCKS=0` for read-only `git diff --check`, `git diff --name-only` and status checks. Tracked diff stays empty; the plan and fix 1 were already untracked, so explicit text and no-index whitespace checks cover the two output files. No `.git` rewrite or index mutation. Existing LF/CRLF conversion warnings are not whitespace failures.
- Checked whitespace/tabs, paths, SHA-256 and line counts; reread both outputs through EOF. Fix 1 and both review hashes match Controller intake. Default Git initially hit the known Windows pointer diagnostic; `python` was unavailable, so assertions ran successfully with `python3`. Neither limitation was treated as a passing check.
- No Maven, Docker, PostgreSQL, Testcontainers, packaged-JAR smoke, network or remote CI execution. No implementation/runtime evidence or production-readiness claim.

## Residual Risks, Owners And Stop State

- Security/Identity: absent production customer-session verifier; retain fail-closed supplied-credential handling and existing session-issuance/OTP/MFA owner destination.
- Sharing Backend/Database/Platform: future PostgreSQL read-only/role assertions, same-connection/PID/MANDATORY proof, SQLSTATE 25006, role restoration, pool reset, no-write/regression evidence; operational dual-role provisioning and shared-command-pool capacity remain there. Dedicated least-privilege public-read ACL remains a later Security/Schema work unit.
- Security/Product: future capability-token representability/format policy. Sharing/Product and future admission/media owners: READ COMMITTED observation is not an authorization lease; recheck current authorization later. No new blocking product question is introduced.
- Gate remains `fix` for Controller intake. No source/runtime/later-gate/remote action occurred: no staging, commit, push, branch/worktree creation, PR, merge, approval, ready action, reviewer request, external comment, issue mutation, re-review entry or implementation dispatch.
