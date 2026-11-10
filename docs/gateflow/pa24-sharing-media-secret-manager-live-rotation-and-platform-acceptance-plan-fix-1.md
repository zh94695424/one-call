# PA24 Plan Fix 1 - evidence validity and key-retirement classification

## 1. Task, authority and status

- Task: `pa24-secret-manager-rollout-plan-fix1-gpt-20261110-01`; date: 2026-11-10; work unit: `PA24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`.
- Author runtime: Codex CLI / GPT. Requested routing is `gpt-6.1-sol` / high; the session cannot independently attest that model identifier. No delegation occurred.
- Binding scope: active PA24 Goal Confirmation and current `fix` gate in `docs/gateflow/project-execution-control.md:15` and `docs/gateflow/project-execution-control.md:40`. The Controller accepted only Reviewer A's Medium evidence-lifetime/classification finding. This artifact records its bounded correction, not a new product requirement.
- Status: `FIX AUTHOR-COMPLETE`; validation exit `0`; `NOT RE-REVIEWED`; no gate acceptance, S1 execution, production-readiness or live-platform result claimed. Stop before re-review.
- Observed HEAD remains `ed9487273e3ce006b904aeeaa9ae7a9623f685d7`; requested merged product base remains `d46140d012acb42574503dc049c2b8778fc116e5`. No Git metadata repair or mutation was performed.

## 2. Accepted finding and direct evidence

The original plan required B's old-K1 Verified/exact replay evidence, retained B until the approved maximum outstanding credential/session/replay horizon, then required a still-time-valid old-K1 C observation. An ordinary A-issued credential cannot generally remain valid after that full retention horizon. Crossing expiry during A-to-B quiesce also invalidates overlap evidence. C's structural absence of K1 must not be inferred from expiry or from Invalid verification alone.

Read both submitted reviews without modifying either:

- Reviewer A: `docs/reviews/plan-review-20261110-141317.md`, 18,598 bytes, SHA256 `9265684f7840d09722e1c6039c05d57a2366523a4b531f8d273908b00713fbc0`; accepted Medium finding, with other questions retaining the Controller's existing dispositions.
- Reviewer B: `docs/reviews/plan-review-20261110-142200.md`, 20,695 bytes, SHA256 `5d20da7e2e0e6cabc0d109f553f8657866b059cc644d5047e48f65e261e0ef7a`; no material finding. Its conclusion does not override the accepted A finding.

Direct read-only implementation/test evidence:

- `backend/src/main/java/com/onecall/core/sharing/internal/credential/MediaPlaybackCredentialAuthority.java:30`: snapshot precedes issuance/replay. At line 46, replay selects the recorded kid; absence returns Unavailable before claim validity at line 55. When K1 exists but the claim is expired, replay can instead return Malformed.
- `backend/src/main/java/com/onecall/core/sharing/internal/credential/MediaPlaybackCredentialAuthority.java:62`: verification decodes using only the current snapshot before evaluating claim validity; decode/validity failure returns Invalid, while source unavailability is distinct.
- `backend/src/main/java/com/onecall/core/sharing/internal/credential/MediaPlaybackCredentialCodec.java:57`: decode selects only a key present in the current map. At line 85 validity requires issuedAt equal to notBefore, issuedAt no later than normalized now and expiresAt strictly after now; runtime skew does not extend expiry.
- `backend/src/test/java/com/onecall/core/sharing/internal/keysource/MediaPlaybackTencentKeySourceIntegrationTests.java:40`: fixed NOW and NOW+60-second expiry are synthetic fixtures. `explicitVersionRolloutVerifiesOldAndNewKeys` (line 63) verifies overlap; `overlapReplaysExactCredentialForEveryPurpose` (line 74) advances only one second and asserts exact credential/anchor/expiry; `removedRecordedKidNeverMintsReplacement` (line 85) uses fixed NOW and proves Invalid/Unavailable/no replacement plus fresh K2 issuance. This is not a post-retention live-cloud experiment.
- `backend/src/main/java/com/onecall/core/sharing/internal/keysource/TencentSsmSecretStringClient.java:28`: exact requested and response secret/version equality, closed SDK failure mapping and direct CvmRoleCredential construction remain unchanged. `TencentSsmMediaPlaybackSigningKeyProvider.currentKeys` still rereads and uses the strict parser, with failure mapped to Unavailable. No concrete runtime gap was found; the no-code decision stands.

## 3. Exact bounded changes

Only the plan and this new fix artifact are task writes. In the plan:

1. Section 1 adds bounded fix provenance and distinguishes historical planning-only stop/write language from the current fix gate; no future permission is expanded.
2. Section 4 preconditions and the new evidence/validity contract require issuedAt, notBefore, expiresAt, start/completion current timestamps, validity booleans, approved validity/horizon/clock/skew references, original input custody, per-instance source/version/bundle provenance, expected/actual results, evidence class, disposition and owner. Only timestamp/assertion/reference metadata is recorded, never credential or anchor contents.
3. Section 4 B evidence is valid only inside the approved ordinary validity window. Boundary-crossed/unknown evidence is INVALID/NOT EXECUTED and blocks overlap acceptance/reopening; expiry is never overlap success. There is no clock adjustment, expiry extension or hidden long-lived credential.
4. Section 4 C evidence is explicitly KEY-ABSENCE. It may use the original expired K1 credential/anchor only alongside independently established current C/K2-only identity/bundle evidence and the recorded-kid control-flow oracle. It proves no selectable K1/no replacement, not continued K1 validity, session continuity or instantaneous revocation. Fresh K2 issuance and verification remain time-valid requirements. Source-wide failure or bare Invalid cannot pass retirement.
5. The A/B/C table and fleet sequence now collect valid-time B proof before expiry, retain serving B for the full approved retirement horizon, then collect C key-absence evidence. They do not shorten retention to fit an evidence credential.
6. A separately approved non-serving acceptance-cohort/external-input path is defined only if Security/Sharing requires additional time-valid old-K1 C evidence. It needs policy-compliant timing, isolation, provenance and harness/custody approvals; otherwise that additional row is NOT EXECUTED and blocks that requirement. It cannot alter serving retention or restore K1 to serving C. No execution is authorized.
7. Sections 7, 10 and 11 carry the same fields/classification/sequence into future S1 obligations and the external-input/owner register. No unrelated review item is promoted into a code change or resolved by assertion.

## 4. Preserved decisions and scope review

- No goal drift or scope expansion: CVM attached-instance-role only; A = K1 ACTIVE, B = K2 ACTIVE plus identical K1 bytes/kid VERIFY_ONLY, C = K2 only. Existing authority/wire and all current-state checks remain binding.
- Missing/wrong config, source/CAM/credential failure and malformed bundle remain fail-closed. Explicit file is never fallback. Moving aliases, cache, watcher, second authority/registry, new runtime services and retry frameworks remain prohibited.
- No production/test/POM/CI change; one future docs-only S1 and its exact one-document allowlist remain unchanged. All canonical product/source paths, configuration/property contract, inventories and validation commands remain unchanged. Original planning-gate allowlist language remains historical, with the fix's two paths explicitly identified in section 1.
- Inventory claims remain focused 9/117, Surefire 36/309, Failsafe 13/214, and firewall 72 coordinates / 65 JARs / 8 dependency-only / 64 counterparts / sole packager / seven collisions / 116 probes. These are preserved claims, not new executed build results.
- V001-V008 and all other protected migrations, source/tests, POM, CI, Control, canonical docs and review artifacts are unchanged by this task. Q22 numeric values remain unselected.
- No provisioning, live credential use, cloud call, Tencent contact, stage/commit/push/branch operation, PR mutation, approval, ready action, reviewer request or external comment occurred. No later gate was entered.

## 5. Validation and write inventory

Native Git initially returned exit 128 because `.git` holds a Windows drive path. All subsequent Git inspection used the existing metadata read-only with `GIT_OPTIONAL_LOCKS=0 git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call26 --work-tree="$PWD"`; no metadata edit was needed.

| Check | Exit / result |
| --- | --- |
| Read-only `git diff --check` | `0`; tracked whitespace clean |
| `git diff --exit-code HEAD -- backend .github/workflows/ci.yml api protocol security acceptance deployment` | `0`; source/tests/POM/CI and protected product surfaces unchanged |
| Entry-to-final protected-file digest comparison | `0`; 1,693 tracked plus nonignored untracked regular files outside the two allowed writes unchanged; digest below |
| Nine in-memory plan contract checks | `0`; timestamps/references, B expiry blocker, C expired-input limitations, fresh K2 verification, full retention, optional C approvals, no-code decision, exact S1 allowlist and removal of contradictory still-time-valid fixture requirement all PASS |
| Direct whitespace check on both task artifacts | `0`; required because the plan and fix artifact are untracked and ordinary Git diff excludes them |
| Changed/untracked scope reconciliation and staged-diff check | `0`; only the two allowed task paths differ from entry; no staged changes |
| Runtime/build/application/SDK/cloud tests | `NOT RUN` / `NOT EXECUTED`; outside this docs-only fix gate |

Protected-file comparison sorts tracked plus nonignored untracked file paths, excludes only the two task paths, then hashes each path, a NUL and its binary SHA256 digest. Entry and final SHA256: `7f4ca799283dac6427d08604011c8afae55680429a06772c9316cadba60d81eb` (1,693 files). This includes source/tests/POM/CI, Control, canonical docs, migrations, both reviews and the pre-existing private files; it does not claim to hash ignored build outputs or Git metadata.

Task paths:

- Modified pre-existing untracked plan: `docs/gateflow/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance-plan.md`.
- New artifact: `docs/gateflow/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance-plan-fix-1.md`.

Entry status already included tracked-modified Control and untracked `.codex`, `.pa24-planfix1-prompt.txt`, `.pa24-planreview-a-prompt.txt`, `.pa24-planreview-b-prompt.txt`, the plan and both reviews. None besides the plan is a task write. Control's entry/final SHA256 remains `f091df9159f447a2e1f4bd4f79b289a7659bcd7a4e03958e00209e1719434f39` (1,925,991 bytes); review hashes remain as above. No unrelated path was created.

## 6. Plan integrity and residual ownership

| Plan version | Bytes | SHA256 |
| --- | --- | --- |
| Original at fix entry | 51,859 | `98966688dc1627e0c22a8dfd81ca577b4cf32689afb1e36fd8fa728de4e39b81` |
| Final after bounded correction | 60,607 | `cbcc122964e2ba0c4bf9d4e0458cca95aa3bbbc00484d48d673b799a0cdfedcf` |

This artifact deliberately contains no self-hash; its final byte size and SHA256 are reported separately after final validation.

Remaining external inputs are not resolved by this fix:

- Security/Sharing + Operations: approve ordinary validity, clock/skew and full retirement horizon/last-issuance/drain references; prove a feasible A-to-B observation window; supply protected harness/custody and independently evidenced bundle/source continuity. Missing/expired evidence blocks the affected acceptance, not a reason to invent Q22 values.
- Security/Sharing: decide whether extra time-valid old-K1 C evidence is required. If required, approve the separate feasible non-serving cohort path with Platform/Operations or retain NOT EXECUTED and block that requirement. This is not authorized cloud work or a substitute for safe serving B retention.
- Platform/Security + Operations: actual CVM/CAM/immutable resource/version evidence, quiesce/restart feasibility, capacity/latency and metadata/DNS/transport limitations remain external live-acceptance dependencies. All live rows remain NOT EXECUTED.
- Existing Controller dispositions remain unchanged: conditional recovery mirror and harness/K1-continuity details go to Security/Sharing + Operations in S1; SDK precision/absent-kid citations are not new blockers; helper fragility and inherited test-maintenance residuals remain Backend QA work. No unrelated review finding is silently closed.
- Backend QA/build: future separately authorized deterministic runner and actual XML/JAR/smoke/firewall evidence. No historical deterministic oracle is relabeled a live-cloud result.

Completion: `FIX AUTHOR-COMPLETE`, `VALIDATION PASS`, `RE-REVIEW NOT STARTED`. The Controller owns any subsequent re-review dispatch and acceptance decision.
