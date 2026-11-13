# PA31 Customer Subject Activation Foundation — Plan Fix 2

## 1. Identity, authority and stop boundary

- Task: `pa31-subject-activation-plan-fix2-gpt-20261113-01`; date: 2026-11-13; gate: bounded **plan fix-2**, following two independent pass-with-risks re-reviews.
- Runtime: supplied Codex CLI in WSL/bash. Requested routing: Codex / GPT `gpt-6.1-sol` / high. Exact executing model/provider metadata is not independently exposed; the requested routing is not runtime attestation. No subagents were spawned.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa31-next-work-unit/codex-call`; verified branch: `codex/pa31-next-work-unit`; verified HEAD/base: `5f8ed0b69c2832ce98224c8222dac1410c4d05f8`.
- Authority: user instruction and Active PA31 Control checkpoint accepting `PA31-PLAN-REV-06` as Medium. Only the cumulative plan is modified and this fix-2 artifact created. Control, reviews and fix-1 remain unchanged.
- Handoff status: **ready only for fresh independent targeted A/B re-review, not plan acceptance or implementation**. This is document readiness only, not a mark-ready action or authorization to dispatch re-review. No later gate is entered.

## 2. Verified inputs and output identities

| Artifact | Raw SHA256 / size where applicable |
| --- | --- |
| Original cumulative plan | `4a70049c82764ee0817d8c6789cd3e5f63346ce57761a0a56a08a48d668fe85a`; 80,459 bytes / 465 LF lines |
| Fix-1 `docs/gateflow/pa31-customer-subject-activation-foundation-plan-fix-1.md` | `5419ce0fd5b6373a80b7b0565e6f0cf2d92448c3d953fc161c7141207cf1af83`; unchanged |
| Re-review A `docs/reviews/plan-review-20261113-104754.md` | `6b89c7aa8d488439b04056ac36df1a1c5dfe86f188ce78726e600d9d6f35ac72`; unchanged |
| Re-review B `docs/reviews/plan-review-20261113-111155.md` | `33cb01183e0042b36864da3e6d8df451916f2d576acd42e96e1fb47bc32ab13f`; unchanged |
| Fixed cumulative plan `docs/gateflow/pa31-customer-subject-activation-foundation-plan.md` | `6dde7211bda0c3a0e3af4c6b0d8d468d091787a6fa66c6d480c34e1c654d80b1`; 84,500 bytes / 471 LF lines |
| New fix-2 artifact | This file, `docs/gateflow/pa31-customer-subject-activation-foundation-plan-fix-2.md`; final size/lines/SHA256 are emitted in the completion report, not self-embedded |

The Active PA31 checkpoint, cumulative plan, fix-1 and both supplied re-reviews inform this bounded correction. The current source assertion was inspected directly in `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`; no implementation file was edited.

## 3. PA31-PLAN-REV-06 correction

**Accepted Medium; closed in plan text for targeted re-review, not independently re-reviewed here.** Plan §6 item 7 now explicitly instructs the future implementation agent to update `ModuleBoundaryTests.discoveredProductionEdgesStayInsideTheConfiguredMap` as follows:

- Change the literal flattened discovered-target multiset from six to seven: `"sharing", "evidence", "subject_number", "activation_consent", "evidence", "catalog_order"` becomes `"sharing", "evidence", "subject_number", "activation_consent", "evidence", "catalog_order", "entitlement_metering"`.
- Add only `entitlement_metering` for the already-authorized activation-consent -> entitlement-metering production edge. Preserve the duplicate `evidence` entries and every existing target; do not deduplicate, replace or relax the exact multiset assertion.
- Replace the exact description/message `"exactly six production edges exist: customer query owners and sharing -> evidence"` with `"exactly seven production edges exist: customer query owners, sharing -> evidence and activation_consent -> entitlement_metering"`.
- Preserve the method identity and all other assertions apart from the prior accepted dependency updates. No new test method/class or test-method/class count is introduced; that preservation is not an instruction to leave the six-entry literal unchanged.
- Plan §6 coding checkpoint, §8 focused validation and §9 fix-2 checkpoint explicitly name the same correction. The existing focused `ModuleBoundaryTests` command covers this exact assertion and its seven-entry expectation; it is a future authorized implementation run, not an executed test in this task.

The only semantic amendment is the exact architecture assertion enumeration/description. Identity/base/gate/completion metadata now describes fix-2; prior fix-1 evidence remains historical. No policy, access, replay, audit, descriptor, state, transaction, schema, grant, provider or live scope is changed.

## 4. Preserved closures and arithmetic

The five prior finding-disposition rows and all behavioral sections (§§2–5), new test identities and complete §7 inventory are byte-identical to the input plan:

- `PA31-PLAN-REV-01`: new valid SUSPENDED ACCEPT remains FORBIDDEN/403, zero writes; authorized exact historical replay and DECLINE remain separate.
- `PA31-PLAN-REV-02`: all 15 existing update paths remain exact, including five added architecture paths and prior accepted dependency/internal-file/profile assertions; no existing method identity/count changes.
- `PA31-PLAN-REV-03`: no decision audit on either decision; DECLINE remains one committed refusal consent only; frozen transition/trial Outbox deltas remain unchanged.
- `PA31-PLAN-REV-04`: retained immutable event-time descriptor, ACCEPT/DECLINE shapes, canonical hashes, replay/link checks and INTERNAL corruption handling remain unchanged.
- `PA31-PLAN-REV-05`: rejected-with-reason as accepted by Control; retain the authorized root-metadata parser and fail-closed CONSENT_REQUIRED without fallback. Publication remains an external operational prerequisite.

One coherent slice remains: **17 new production files + seven new test classes + one fixture = 25 new files; 15 existing updates = 40 implementation paths**, excluding any separately authorized implementation report. New methods remain **52 unit + 34 IT = 86**. Baseline **54/468 + 23/370 = 77/838**, annotation839/POM23; projected **59/520 + 25/404 = 84/924**, annotation**925**, POM**25**. Existing architecture assertion changes add zero classes/methods. Source=CI=XML target84/924 remains future acceptance evidence, not executed XML evidence here.

## 5. Read-only validation and limitations

**Results: PASS.** Exact task-delta/frozen-byte comparisons, REV-06 enumeration/description/reference checks, prior closure/path/test-inventory preservation, arithmetic, UTF-8/LF/whitespace and embedded syntax checks all pass. The explicit frozen-path Git diff is empty; Git whitespace checks report no whitespace defect. No-index checks return the normal added-file difference status 1, not a whitespace failure.

- **Input hashes and Git identity:** supplied plan/fix-1/A/B raw hashes match. Default Git fails the Windows gitdir; explicit read-only `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call8 --work-tree="$PWD"` verifies branch/HEAD without metadata repair. Final Git checks use `GIT_OPTIONAL_LOCKS=0`. Plan, fix-1 and `.codex` were already untracked on entry; task scope is measured against the entry content snapshot, not a falsely clean Git baseline.
- **Exact two-path delta:** an outside-repository snapshot captured 1911 existing regular files, including the gitfile. Completion comparison requires exactly the modified cumulative plan and this new fix-2 artifact; all other 1910 existing file hashes, the gitfile and Git index remain unchanged. No deletion or other addition is allowed. Temporary checker/snapshot files reside only under `/tmp`.
- **Enumeration/runbook:** exact six-to-seven literal and exact old-description replacement instructions are present; both `evidence` occurrences and all six original targets are preserved. Item 7 no longer uses ambiguous “no new method/count” wording. Checkpoint/focused-run/closure/artifact references name `discoveredProductionEdgesStayInsideTheConfiguredMap` and the seven-edge `entitlement_metering` addition. No residual instruction requires retaining the six-entry assertion; old wording appears only as the text to replace.
- **Preservation/inventory:** compare all 15 existing update paths, unchanged items other than item 7, 17 new production paths, all seven test-method lists (86=52+34), prior disposition rows and behavioral/test sections. Recheck baseline annotation839 and POM23 statically and projected84/924/annotation925/POM25; preserve 40-path accounting.
- **Frozen boundaries:** all 20 frozen path hashes remain equal to entry: OpenAPI, docs14, docs15 and exactly V001–V017. Explicit-path Git comparison against HEAD is empty; no V018 exists. The whole-tree comparison also covers every source/test/CI/POM/README/Control/review file and fix-1, with no changes.
- **Text/syntax:** strict UTF-8, LF/final newline, no U+FFFD/conflict markers/trailing whitespace, balanced fenced blocks, `bash -n` on plan shell blocks and AST parsing of embedded Python; explicit no-index Git whitespace checks cover both untracked output documents. Final sizes/lines/raw SHA256 are reported separately.
- **Checker/environment diagnostics:** `python` is unavailable; checks use `python3`. One initial patch invocation omitted its required opening marker and was rejected without modifying any file; the corrected patch succeeded. An intermediate checker matched the quoted old description as though it were a retention instruction; the corrected check requires that its sole occurrence be the explicit replacement instruction. Git emitted existing LF-to-CRLF normalization warnings without rewriting the documents/reviews; raw hashes remain authoritative. Such tooling diagnostics are not implementation failures or a Git metadata repair.

No Maven/npm/JDK/Docker/PostgreSQL/application/JAR/firewall/OpenAPI/CI execution, test/XML execution or production/live acceptance is claimed. The focused `ModuleBoundaryTests` coverage above is specified and statically checked, not run. Read-only structural/hash/syntax checks are the only executed validation at this gate.

## 6. Residual ownership and no-later-gate statement

No unresolved bounded plan-fix blocker remains after structural checks. Prior residuals are unchanged: Operations/product owns actual policy publication; Platform/Database/Security/Operations owns dedicated login/two SET memberships, credentials/TLS/rotation/pool operations; Database/Operations owns descriptor/trial corruption incidents; Controller/runtime owner supplies future implementation tooling and executed acceptance. Historical replay is not a live-state projection. No new schema/grant/migration/provider/live scope is authorized.

The two documents are **ready only for fresh independent targeted A/B re-review**. No re-review was dispatched or entered; no acceptance or implementation gate began. No commit, push, PR open/update, merge, approval, mark-ready action, reviewer request, external comment, issue mutation or other later-gate action occurred.
