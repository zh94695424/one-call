# PA14 Revoke Slice 2 — Bounded Plan Fix 1

- Task: `pa14-revocation-plan-fix1-gpt-20261105`.
- Date: 2026-11-05; work unit: `PA14-sharing-command-revocation`; gate: `fix` after independent A/B Planreview.
- Assigned runtime/provider/model: Codex runner / `gpt` / `gpt-6.1-sol`, reasoning `high`.
- status: fix-complete
- Baseline plan SHA256: `ad2e303c6148bf8bfbca66987d0ec6edd8dcf6d367cdd5a52ca7d8eab24aaf44`.
- Current plan SHA256: `173c719cf9de20c25ea02c4acb0aaf681c3b71af5c6eea3404863f0fe0993912`.

## Accepted Findings And Adjudication

1. Accepted: §3.1 allowed either an accessor or controller re-enumeration, while `CustomerApiBoundaryTests` bans literal `getAttribute(` in `CustomerSharesController`. Freeze the Filter-owned attribute plus package-private static accessor pattern, matching existing trace handoff; remove the alternative. Do not weaken the architecture test.
2. Accepted: the valid single comma-containing header contract lacked named assertion ownership. Bind the same exact `revoke,raw-key` string to the existing web method and already accepted new service method; add no other method.
3. Accepted: the existing combined revoke/rotate rejection loop ends with `verifyNoInteractions(fixture.commands)`. Once revoke is admitted, that assertion must belong only to an isolated retained rotate-only 404 branch and must not suppress or mask revoke execution.

These are bounded clarity and assertion-ownership corrections, not new behavior or permission to implement.

## Exact Plan Text Changed

Only these three replacements were made; every other byte of the baseline plan is unchanged.

### §3.1 — exact Filter-to-Controller handoff

Before:

```text
Store the validated value in a filter-owned request attribute/accessor for controller use, or re-enumerate identically; never derive identity from JSON.
```

After:

```text
Following the existing `CustomerBoundaryFilter.trace(request)` pattern, the Filter stores the exact accepted string in a filter-owned request attribute and exposes package-private `static String idempotencyKey(HttpServletRequest request)` to retrieve that stored string unchanged. The Controller calls only `CustomerBoundaryFilter.idempotencyKey(request)` for the key; it must not contain the literal `getAttribute(` (including `request.getAttribute(`), re-enumerate the header or split its value. The accessor performs no trim, normalization, case folding, truncation or comma splitting; never derive identity from JSON.
```

### §8.1 — accepted service-test assertion ownership

Before:

```text
| Surefire `CustomerShareCommandServiceTests.revokeReplaysBeforeStatePreconditions` | Same eventId/fingerprint succeeds before current REVOKED/EXPIRED/time/witness checks; changed witness still replays; changed exact reason conflicts before terminal check. Assert deterministic canonical identity/digest/fingerprint (witness/trace excluded; exact raw-key bytes preserved), current context/ownership before replay, role/locks/clock order, no writes/UUID/material/evidence/policy interactions for replay. Cover direct invalid ID/key/witness/reason before transaction and ordinary new-action REVOKED/expiry/stale order via table loops. Use existing mocked template/repository fixture, without treating mock rollback as PostgreSQL proof. |
```

After:

```text
| Surefire `CustomerShareCommandServiceTests.revokeReplaysBeforeStatePreconditions` | Same eventId/fingerprint succeeds before current REVOKED/EXPIRED/time/witness checks; changed witness still replays; changed exact reason conflicts before terminal check. Assert deterministic canonical identity/digest/fingerprint (witness/trace excluded; exact raw-key bytes preserved), use the exact comma-containing key `revoke,raw-key` in a replay/identity assertion that independently recomputes the request digest and transition event identity from that exact raw string and proves replay without writes; current context/ownership before replay, role/locks/clock order, no writes/UUID/material/evidence/policy interactions for replay. Cover direct invalid ID/key/witness/reason before transaction and ordinary new-action REVOKED/expiry/stale order via table loops. Use existing mocked template/repository fixture, without treating mock rollback as PostgreSQL proof. |
```

### §8.2 — existing web-test assertion ownership

Before:

```text
- `CustomerSharesCommandWebTests.revokeAndRotateMapClosedResponseUnions`: replace only revoke's old all-404 branch with first/replay 200 closed ShareState, selected personal/org caller and trusted trace assertions. Retain all six exact shared writer domain rows/common status/header tests; keep code-rotations unadmitted 404. Drive actual command exception mapping through controller for revoke's exact union and prove no code/token/hash/salt/key leakage.
```

After:

```text
- `CustomerSharesCommandWebTests.revokeAndRotateMapClosedResponseUnions`: replace only revoke's old all-404 branch with first/replay 200 closed ShareState, selected personal/org caller and trusted trace assertions. Send one `Idempotency-Key` value `revoke,raw-key` through the actual filter/controller path and assert the captured `CustomerShareCommand.Revoke.actionRequestId()` equals that exact string. Retain all six exact shared writer domain rows/common status/header tests; keep code-rotations unadmitted 404. After mapping revoke to its command path, scope `verifyNoInteractions(fixture.commands)` only to the retained rotate-only rejected 404 branch, using an isolated rotate fixture; it must not cover or mask revoke command invocation. Drive actual command exception mapping through controller for revoke's exact union and prove no code/token/hash/salt/key leakage.
```

## Resulting Contract And Ownership

- Filter: authenticate, enumerate and validate exactly one revoke header value under the unchanged precedence, then store the exact accepted string in a Filter-owned attribute. Package-private `static String idempotencyKey(HttpServletRequest request)` retrieves it unchanged, like `trace(request)`.
- Controller: call `CustomerBoundaryFilter.idempotencyKey(request)` only; no literal `getAttribute(`, header re-enumeration, splitting or normalization. Create-header behavior remains unchanged.
- `CustomerSharesCommandWebTests.revokeAndRotateMapClosedResponseUnions`: actual filter/controller passage of one `revoke,raw-key` value; exact captured `Revoke.actionRequestId()` equality; revoke command invocation remains observable. Use an isolated rotate fixture for rotate-only 404 and its `verifyNoInteractions(fixture.commands)`.
- `CustomerShareCommandServiceTests.revokeReplaysBeforeStatePreconditions`: same raw comma key; independent request-digest and transition-event-identity recomputation, exact replay and no writes. This remains the single accepted additional Surefire method.

## Preserved Boundaries

No change to the confirmed revoke-only goal, one implementation slice (Slice 2), contract/state/error/replay behavior, future implementation file list or counts. Section 7's exact 15-file implementation allowlist is byte-for-byte unchanged. Expected inventory remains 78 Surefire / 16 classes + 113 Failsafe / 6 classes = 191 methods, with only the accepted one service and five IT additions. No new test method, source, test, configuration, CI, README, Control, review, schema, OpenAPI or V001–V010 edit. No rotate success, public admission/media, production provider/policy choice or external mutation. Only the plan and this fix artifact are written.

## Static Checks Run

- Verified the baseline plan's full-file SHA256 against the supplied hash before editing.
- Read applicable ancestor instructions and checked `docs` for nested AGENTS.md instructions; no additional scoped instructions were found.
- Read existing Filter trace accessor, architecture source-string restriction and web test's rejection/no-interaction assertion; no implementation files changed.
- Read-only Git status using `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call13` and `GIT_WORK_TREE` because the native Windows worktree pointer fails in this shell. Observed pre-existing Control modification and untracked `.codex`, plan and two review artifacts; preserve them without editing metadata.
- Asserted exactly three replacements; reversing those replacements reproduces the complete baseline plan byte-for-byte. Verified the entire §7 allowlist unchanged and exactly 15 file rows.
- Asserted unchanged 78/113/191 inventory, exactly two occurrences of the shared comma-key literal in the updated plan and removal of the ambiguous handoff alternative. Checked plan whitespace statically.
- Computed current plan SHA256 and artifact payload SHA256. The final completion report supplies the full-file artifact SHA256 after the final write.
- No Maven, Docker, package build, implementation, staging, commit, branch/PR mutation, approval, ready action, reviewer request, external comment, issue mutation or delegation was performed.

## Residual Risks And Owners

- Backend/web implementation owner: implement the specified accessor and named in-place web assertions without violating the architecture ban; this plan fix does not execute or prove them.
- Backend/service and database test owners: implement the accepted replay identity/no-write assertion and existing future PostgreSQL rollback/concurrency evidence; runtime evidence remains deferred to implementation.
- Environment owner: Windows Git-pointer and loopback/Docker limitations remain unchanged. The read-only Git override suffices for this bounded authoring task; do not repair metadata here.
- Security/Product/Sharing and evidence owners: existing deferred production policy/provider and evidence-lifecycle questions remain out of scope and unchanged.
- No open bounded handoff, comma-assertion or rotate-scoping question remains. No blocker to this plan correction; author completion is not gate acceptance or implementation readiness.

## Artifact Integrity

A file cannot embed its own final full-file SHA256 without a self-reference. The following artifact payload SHA256 covers the UTF-8 bytes of this artifact with the single `Artifact payload SHA256:` line omitted (including its newline). The final report records the current artifact's full-file SHA256 separately; the current plan hash above is its ordinary full-file SHA256.
Artifact payload SHA256: `d4deea210d50be5103f037f533ec2e62ba36ca637eb6bc81f78b6ddbd52fd000`
