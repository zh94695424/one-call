# PA8 V008 Slice2 bounded cycle-marker correction

## Status and scope

- Task: `pa8-s2-fix2-gpt-20261030T192927Z`.
- Accepted reviewer B finding 1 is corrected at source level. **Controller revalidation is pending; this report does not claim post-fix runtime PASS, Slice2 acceptance, or completion of later gates.**
- Read accepted plan §9 Slice2 cycle classification, the plan fix-3 classification clarification, `v008S2CycleFailures`, and `expectCommitFailure`.
- The controller reports that the incoming candidate passed the focused 3 tests, then the full 57 Failsafe tests and 10 Surefire tests. These are supplied pre-fix results, not independently executed or post-fix validation by this author.
- No migration defect was found. Deliverables are limited to the Java test source and this new completion artifact. Existing intake changes are preserved; V001–V008, designs, plans, CI, README, and previous reports are frozen and unmodified by this task.

## Exact correction

In `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java:25316`, replace the noncreation-only marker selection and null fallback with the shared `EVIDENCE_CREATION_CYCLE_VIOLATION` fallback. The two preceding named-FK branches remain unchanged.

| Case | Required commit SQLSTATE | Required message marker |
| --- | --- | --- |
| `missing_event` | `23503` | `fk_evidence_source_event` |
| `missing_record` | `23503` | `fk_evidence_event_record` |
| `noncreation_decoy` | `P0001` | `EVIDENCE_CREATION_CYCLE_VIOLATION` |
| `crosslink` | `P0001` | `EVIDENCE_CREATION_CYCLE_VIOLATION` |
| `source_mismatch` | `P0001` | `EVIDENCE_CREATION_CYCLE_VIOLATION` |

`expectCommitFailure` is unchanged: it checks exact SQLSTATE equality and checks message containment when the marker is non-null. All five cases now supply a non-null marker. The three FK-satisfied contradictions assert the class-level contract, without named-trigger specificity or a SQLSTATE union.

The source-byte comparison proves no other Java changes: fixtures, insertion order (including crosslink events-before-records and source-mismatch D/C-before-R), valid-order positive controls, fresh-connection zero checks, ACLs, cleanup, and all 57 test names are preserved. No production/schema change, grants, broader refactor, or optional redundancy maintenance is included.

## Actual local validation

Source-level evidence is under `/tmp/pa8-s2-fix2-validation/`, outside the deliverables.

1. Byte-level comparison with the task-intake Java snapshot confirms exactly the single marker-selection replacement; every other source byte remains unchanged.
2. Static inventory confirms exactly 57 unique `@Test` method names, equal to the frozen CI expected-name set. CI was read only, not changed.
3. Tracked diffs outside the allowed Java file match the captured intake diff. Before creating this report, repository status also matches intake. Pre-existing CI, README, Java and prior-artifact changes are not attributed to this correction.
4. The intake-to-current Java diff contains only the stated one-line replacement. `git -c core.whitespace=cr-at-eol diff --check` succeeds, respecting the source's existing mixed line endings.
5. No compile, build, tests, Docker commands, focused run, full-suite run, or runtime probe was executed. Static marker classification is not runtime evidence.

## Residual owners and destinations

| Residual | Owner / destination |
| --- | --- |
| Post-fix exact SQLSTATE/message behavior and preserved controls | Controller: run the focused three S2 tests first and retain logs/XML. |
| Full regression confirmation | Controller: only after focused success, run the conditional full 57 Failsafe tests and 10 Surefire tests; retain actual results. |
| Runtime mismatch or any subsequently identified frozen migration/design defect | Stop and report to controller and relevant design owner; do not weaken assertions, introduce grants, or silently amend migrations. |
| `same_source_pair` versus source-unique redundancy observation | Controller / future test-maintenance owner: optional future maintenance, outside this fix. |
| Existing Q11 WORM and Q22 TTL/legal-retention policy work | Existing production/security/compliance design owners; outside this correction. |
| Independent acceptance and later gates | Controller-owned; not initiated or claimed here. |

**Bounded author fix and completion artifact are complete; runtime revalidation and gate acceptance remain with the controller.** No staging, commit, branch, push, PR, remote operation, external comment, delegation, or reviewer-directory access was performed. Work stops here; no later gate is entered.
