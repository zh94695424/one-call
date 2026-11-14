# PA33 Operations subject read foundation — bounded plan fix 1

## 1. Task, authority and gate

- Task: `pa33-operations-subject-read-plan-fix1-gpt-20261114-01`; date: 2026-11-14.
- Gate: **plan review → bounded fix; author complete only**. Stop for fresh independent A/B Planreview. No implementation authorization, self-approval or plan-pass claim.
- Required dispatch: Codex runner / GPT `gpt-6.1-sol` / high. Runner/model metadata is not independently inspectable here; Controller must verify it, not infer it from this artifact.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa33-next-work-unit/codex-call`.
- Binding authority: current PA33 checkpoint in `docs/gateflow/project-execution-control.md`, including accepted findings PA33-PLAN-REV-01..06 and Controller's capacity adjudication. Confirmed Goal Confirmation and exactly `listOperationsSubjects` / `getOperationsSubject` remain unchanged.
- Complete independent inputs read: `docs/reviews/plan-review-20261114-162057.md` (A: fail) and `docs/reviews/plan-review-20261114-164255.md` (B: pass-with-risks). Neither review is edited; their original verdicts are not reclassified as passes.
- Authorized writes only: cumulative `docs/gateflow/pa33-operations-subject-read-foundation-plan.md` and this one new fix artifact. All patches use apply_patch; no source, tests, migrations, OpenAPI, CI, POM, Control, reviews, other docs or `.codex` edits.

## 2. Accepted-finding closure map

| Binding finding | Cumulative correction and validation obligation |
| --- | --- |
| PA33-PLAN-REV-01 (Medium; A-01/B-02) | §§3.1–3.3, 8, 9.1: admitted GET filter does route admission + bearer verification only, bypassing POST Accept rejection; POST unchanged. Controller copies every raw Accept value as system accept data, has no produces condition or negotiable MVC body, serializes/buffers success explicitly as application/json. Post-current-DB-authority parser rejects malformed/incompatible/quality-0 Accept with read VALIDATION_FAILED/422. Existing named web/parser/IT methods pin DISABLED + text/html → 403 versus ACTIVE ADMIN/OPS + text/html, JSON q=0, malformed q → 422 on both GETs; no 406/HTML/500/Spring error body. |
| PA33-PLAN-REV-02 (Medium; A-02) | §9.2 replaces the contradictory seed sentence with distinct deterministic subject families and exact named-test mapping: EMPTY; 1/100 histories; independent binding101/claim101/both101; six lifecycle/risk combinations and mixed risk; three latest-claim statuses with older claims; absent/present INACTIVE/ACTIVE/SUSPENDED activation; sixteen source/real-name/risk combinations; sort ties; absent/available/reserved/fully reserved/exhausted/time-expired/balance-expired/boundary trial states; relation faults; counts and bounded candidate scans. Exact 0/1/100 success and 101 refusal assertions retained. |
| PA33-PLAN-REV-03 (Low; A-03) | §4 explicitly requires nano % 1000 == 0 as well as range checking after tag authentication, before candidate/owner queries; otherwise CURSOR_INVALID/400, no rounding. Named invalidNanosAndPlaintextShapesNeverReachQueries includes correctly authenticated nonmicrosecond payloads and valid precision boundaries. Authority ordering remains intact. |
| PA33-PLAN-REV-04 (Low; A-04/B-01) | §7.2 corrects activation_consent_role schema USAGE to V004:485–486 and entitlement_metering_role schema USAGE to V004:492–493; table reads separately cite V007:207–212 and V006:641–646. Six-role sufficiency conclusion and no-new-grant hard stop unchanged. |
| PA33-PLAN-REV-05 (Low; B-03) | §6.1 pins all five replacement allowedDependencies sets, exact 12-target multiset (operations_iam ×5, evidence ×2), and exact “exactly twelve production edges exist” assertion message. Retains containsExactlyInAnyOrder, subset checks, exact maps, 14 modules and all nine Appendix A ModuleBoundaryTests identities. |
| PA33-PLAN-REV-06 (Low; accepted clarifications) | §§4–5, 8–9.2: existing Mockito/jOOQ DSL/result/record doubles through the already-required DSL dependency inject unknown role/status/revision/identity without a production seam. Real constraints remain enabled; impossible FK relations use existing port doubles, constraint-valid setup is isolated/cleaned. Trial/grant expiry mismatch → sanitized 503 + rollback/no partial response; only validated equal trial_expires_at is projected and used for status. Cursor keys use indexed ordered entries with explicit id/material values, case-sensitive ASCII kid constraints, explicit deployment secret-mounted properties and no kid-derived environment names/new reader. Retained OperationsSubjectsWebTests / OperationsLifecycleBoundaryTests are not edited and their existing POST/customer-pass-through literals stay intact. |

No test method, implementation path or new public operation is added by these clarifications. Assertions are cases inside already-named tests, not additions to the 106-test budget.

## 3. Binding capacity decision and residual destination

Controller accepts fail-closed capacity: detail >100 bindings or >100 claims, or activation filtering after 100 batches / 10,100 candidates without sufficient matching lookahead or a proven tail, returns whole-response sanitized SERVICE_UNAVAILABLE/503 with rollback and no partial response. The plan's §§2, 5, 11.2 and 12 now record this as **accepted**, not an unanswered pre-implementation adjudication.

Destination: **Product/Backend/Database** for capacity/SLO follow-up. No silent truncation, offset pagination, arbitrary-history success claim, V018, index, grant or OpenAPI change is authorized. If the frozen contract is later interpreted to require arbitrary-cardinality success, obtain new user authorization; do not silently redesign within PA33.

External residuals remain: native-JDK/Docker/full XML validation; production six-role login and key custody/rotation; production employee issuer/session/revocation; missing covering index/count-volume SLOs; request-snapshot rather than concurrent-disable serialization/cross-page export. Owners and hard stops remain in cumulative §12. No runtime readiness is claimed.

## 4. Static checks and preserved arithmetic

Checks actually run (read-only except the two allowed artifact patches):

- Captured a SHA256 manifest of all 2,014 pre-existing workspace files outside the repository in `/tmp/pa33-fix1-before.json`, plus original plan bytes in `/tmp/pa33-plan-before.md`. Final comparison must show exactly one changed existing file and this one new artifact, with no deletion/unlisted change.
- Parsed Appendix A and compared exact retained identities to Java ordinary @Test methods and the actual CI literal dictionaries plus their update/addition loops; parsed actual POM Failsafe includes with ElementTree. **SOURCE=CI=POM passed**, not merely matching totals.
- Baseline: Surefire **64 classes / 574**, Failsafe **27 / 440**, total **91 / 1,014**; raw @Test occurrences and token-bearing lines **1,015** (retained PostgresContainerIT comment included); POM includes **27**.
- Planned additions remain six Surefire classes / **70** + two Failsafe classes / **36** = eight classes / **106** new tests. Final planned inventory remains **70/644 + 29/476 = 99/1,120**, tokens **1,015 → 1,121**, POM **27 → 29**. These are planned counts, not executed tests.
- Compared §8's exact expanded-path labels against the original plan: **36** unchanged paths, **25 new = 15 production + 8 test classes + 2 fixtures**, **11 existing = 8 production + 1 test + POM + CI**; one S1 slice retained.
- Appendix A and Appendix B remain byte-identical to the original plan. Independently rehashed all **22** Appendix B files and checked exact sizes/digests; migration inventory is exactly **V001–V017**, no V018.
- Inspected frozen V004 schema USAGE statements and retained filter-literal assertions directly; no related source/test edit.
- Ran whitespace/trailing-space, conflict-marker and U+FFFD scans on the cumulative plan; passed. Final two-artifact scans and manifest comparison are repeated after this artifact is written.
- Native git status initially failed on the existing Windows gitdir pointer. Read-only explicit git-dir/work-tree diff --check succeeded; the cumulative plan is not a tracked delta in that Git view, so direct original-vs-final checks and full manifest are the bounded-edit evidence. No Git metadata repair, staging or index write.

No Maven, Docker, Flyway, packaged-JAR, A1–A5, hosted CI or runtime XML test was run for this documentation-only fix. Zero-runtime-evidence limitations remain explicit; source=CI=POM is not source=CI=POM=XML proof. Small initial inventory-check harness errors were corrected before the static inventory pass; no product test failure/pass is inferred from those scripts.

## 5. Hashes, counts and stop

| Artifact / binding input | Final bytes / lines where applicable | SHA256 |
| --- | --- | --- |
| Original reviewed cumulative plan (unchanged reference) | 138,239 / 2,047 | `a89585363d65da38772686729814404a922161a92d6bc0fc0840b1a7465cbdcb` |
| Corrected cumulative plan | 154,012 / 2,091 | `4e5cbb8fffb33dcfe879a58153e2c594bf6f2d3115653ab4b26aeffe7aec9b37` |
| Current binding Control (preserved) | Rehashed in this workspace | `a74b4ae11d55c9b0a2b35256ff09e9e408823eb242264a927f416378fd9879f3` |
| Complete review A (preserved) | 23,797 / 115 | `cfcf768f623d5d4ad3f9a64e8d4c87980b735ad1c74bf82cf11eb3694e356275` |
| Complete review B (preserved) | 24,803 / 187 | `594618eefc24b15eaf473c7fcb4c7d62dddd75cf8bd8c2790bfb84b4f043b40a` |

Current Control contains the later Controller adjudication and therefore is not asserted equal to the earlier Control digest embedded in the independent reviews. This artifact does not embed its own recursive hash; final measured bytes/lines and SHA256 for **both allowed artifacts** are reported in the author handoff after all writes.

**Author complete — stop for fresh independent A/B Planreview.** No commit, stage, push, PR, merge, approve, ready action, reviewer request, external comment or issue mutation was performed or is requested. Controller independently verifies dispatch and routes the next gate; this author does not initiate it.
