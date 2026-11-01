# PA9-v009-sharing-persistence-foundation final closeout

- Gate: `final closeout pass`; architecture-sensitive PostgreSQL/Flyway persistence feature completed.
- Draft PR: https://github.com/zh94695424/one-call/pull/17, base `main`, head `codex/pa9-v009-sharing-foundation`.
- Merged base: `835f2ecece1ac8b5c79ab794427424ea380ac052`.
- Frozen reviewed head: `179aa6b044ed662835865baedc1bf7f0f8f1b3dd`.
- Reviewed range: `835f2ecece1ac8b5c79ab794427424ea380ac052..179aa6b044ed662835865baedc1bf7f0f8f1b3dd`; 50 files, 8,990 insertions and 146 deletions.
- Accepted PR-review checkpoint: `12cc99902e49647d9fb570094ca4d252f3922a5e`.
- Final push: remote branch and `refs/pull/17/head` both matched `12cc99902e49647d9fb570094ca4d252f3922a5e`. GitHub independently reported PR #17 `OPEN`, `isDraft=true`, base `main`, head branch `codex/pa9-v009-sharing-foundation`.

## Delivered

V009 adds exactly three sharing tables: `sharing.evidence_share_grant`, `sharing.share_admission` and `sharing.share_access_event`. The frozen migration contains 38 ordered columns, 31 named checks, 13 physical indexes, six immediate validated non-deferrable `RESTRICT/RESTRICT` foreign keys, nine explicit defaults, three owner-local `SECURITY INVOKER` trigger functions and four enabled row triggers. It creates the `sharing_owner`/`sharing_role` roles, exact schema/table/column/function ACLs, staged V008 sharing-absence fences and the final exact V009/no-V010 live fence.

The grant guard enforces the frozen lifecycle, terminality, revision, expiry, generation and immutable-content rules. The admission and access-event rows are append-only; the SQL harness exercises atomic count-once admission, replay-before-limit, ACTIVE/LOCKED replay, REVOKED/EXPIRED denial, rollback/no-residue behavior and 18 bounded two-connection identity/limit/lifecycle races.

V001-V008 remain byte-identical to the merged base. V009 is frozen at canonical SHA256 `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`. No V010, sharing application service, API, worker, Redis/COS/KMS integration, ticket/playback implementation, runtime DataSource/Flyway wiring or invented policy value is delivered.

## Validation

- Final PostgreSQL 16.15/FlywayTestcontainers run: 64 Failsafe plus 10 Surefire tests, zero failures/errors/skips.
- Source `@Test` methods = CI expected literal = Failsafe XML = 64 distinct names; Surefire = 10. No duplicate, skipped, disabled or assumed tests.
- Independent Reviewer A reran focused/full 64+10 and PR metadata checks. Reviewer B independently re-derived the complete frozen contracts and per-method prior-wave comparison, compiled the project and passed 10 Surefire tests; its sandbox blocked the Docker socket, so it did not claim a rerun of the 64 Failsafe cases.
- After accepted PR-review push `12cc999`, GitHub run `41273454908` passed both `java-runtime` and `contract-regressions` on that exact PR head.
- PR review A: `docs/reviews/pr-17-review-20261031-202658.md`, SHA256 `a9a02224b25ecf54d30b8496a25c430be533be2c2a7b43b9eeaf830e7ffd81081`, verdict `pass`.
- PR review B: `docs/reviews/pr-17-review-20261101-122852.md`, SHA256 `14c7157303799105d5aace2034d816d86141f80f43d1f0f932e8b7b579f35566`, verdict `pass-with-risks`.

Both reviewers inspected the same frozen base/head pair in isolated workspaces and submitted their artifacts without reading the peer conclusion. The controller revalidated the repository state, structured Qwen completion, imported artifact bytes, hashes and scope. Qwen B produced 325 valid JSONL lines, exactly one `turn.completed`, no `error`/`turn.failed` events, empty stderr and a final `review-complete` result.

## Finding disposition

No material or blocking finding remains. All accepted plan, slice, aggregate and PR findings have final fixed/closed status through independent re-review. Reviewer B's Docker and GitHub-observability limitations are retained as evidence limitations, not converted into a passing local 64-case claim; A's independent full run and the successful exact-head remote CI provide separate runtime evidence.

## Residual risk reconciliation

| Remaining risk / limitation | Owner and destination |
| --- | --- |
| Reviewer B's sandbox could not access Docker or remote GitHub; its 64-case/CI observations are limitations rather than independent reruns. | Controller/QA evidence retention; Controller independently verified push, exact-head CI and review artifacts, while A reran 64+10. |
| CHECK `UNKNOWN`, presence-only JSON, independent event/admission FKs and pair consistency cannot prove typed semantic validation or one grant/admission/event/count pairing. | Sharing/Backend/Security; future sharing-service typed validation, authorization and transaction orchestration gate. |
| same-state terminal/LOCKED generation increases, ACTIVE same-state rotation, mixed lock+extension and revision-only writes are structurally accepted but service-forbidden; expected-revision WHERE, one-admission-one-event and replay semantics remain caller obligations. | Sharing/Backend; owner-transition/admission service and deterministic replay implementation gates. |
| KDF/OTP/TTL/limit/rate/risk/ticket-expiry and playback policy values are not selected or implemented. | Security/Product/Sharing; existing policy-decision and service implementation work units. |
| Production role provisioning, multi-owner migration identity, PostgreSQL image digest pinning, backup/restore, retention and environment controls are test-fixture or later-gate concerns. | Platform/Database/Security/Legal; production provisioning, deployment and retention gates. |
| synthetic five-second expiry windows, bounded lock observers and 18 races prove ordering/state only, not throughput, deadlock freedom, chaos, load or real session revocation. | Backend QA/Platform; later performance, resilience and acceptance work. |
| no sharing application service, API, worker, Redis/COS/KMS/gateway, ticket/playback implementation, runtime wiring or production readiness is delivered. | Sharing/Backend/Evidence/Infrastructure/Security; subsequent application-service and integration work units. |

Each remaining item has an owner and destination; none is an unclassified PA9 completion requirement.

## Completion and next entry

`draft-PR-pass` and `final closeout pass` are accepted. This work unit is not linked to a GitHub issue, so issue linkage, closeout comments and automatic closure are not applicable. No merge, approval, mark-ready action, reviewer request, external comment, issue mutation or branch deletion was performed.

After the user merges PR #17, fetch/update target `main`, create a fresh managed worktree and `codex/` branch from that merged commit, and start Phaseflow goal confirmation for the next sharing application-service foundation. Its exact tables, services, policy inputs, APIs and test boundary must be confirmed from the current design; PA9's persistence-only authorization does not carry into that work unit.
