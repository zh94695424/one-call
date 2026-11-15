# PA34 code-review re-review fix 1 - fix2 author finalization

Task: `pa34-operations-activation-history-code-review-rereview-fix1-finalize-gpt-20261115-02`.

Work unit: `PA34-operations-activation-history-read-foundation`.

Date: 2026-11-15 (Asia/Shanghai).

Gate: finalization of the already-implemented, Controller-validated test-only fix2. This is author documentation, not independent review, finding acceptance, gate approval, live acceptance or production readiness.

## 1. Authority and exact scope

Authority is the active PA34 section of `docs/gateflow/project-execution-control.md`, the accepted cumulative `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md`, current implementation report, historical `docs/gateflow/pa34-code-review-fix-1.md`, independent re-reviews A `docs/reviews/code-review-20261115-091434.md` and B2 `docs/reviews/code-review-20261115-093905.md`, and the current bounded author-finalization instruction. Checkpoint HEAD is `6dd4aa79edb95a93023f219cffa6b1a0366d7fde`, verified before and after documentation.

The failed prior author task was `pa34-operations-activation-history-code-review-rereview-fix1-gpt-20261115-01`; its test edits are inherited, not recreated. The only writes authorized and performed by this finalization are the existing `docs/gateflow/pa34-operations-activation-history-read-foundation-implementation.md` and this sole new artifact. No Java, fixture, CI, POM, application configuration, contract, migration, Control, plan, freeze/review artifact, private helper or Git-state repair is authorized or performed. An initial mandatory-hash mismatch or concrete code/test defect would require stopping without documentation; neither was found during this inspection.

Managed worktree: `C:\Users\Administrator\.codex\worktrees\pa34-next-work-unit\codex-call` (WSL `/mnt/c/Users/Administrator/.codex/worktrees/pa34-next-work-unit/codex-call`). Linux Git reads use the existing Windows-linked metadata via explicit `GIT_DIR`, `GIT_COMMON_DIR`, `GIT_WORK_TREE` and `GIT_OPTIONAL_LOCKS=0`, without rewriting `.git` or refreshing/restoring the index. No test/build/contract/packaged process is rerun here.

## 2. Accepted findings and inherited repair direction

### PA34-REREVIEW-01 - all five legal projection shapes

Accepted Low finding A F-01 was a regression-detection gap, not a demonstrated production defect: the existing service method asserted too little per legal shape and the PostgreSQL method counted items without checking all values. The accepted direction was to strengthen those existing methods only, retaining legacy names and exact identities.

`OperationsActivationReadServiceTests.allPersistableStatusChannelAndDomainCombinationsProjectExactly` now compares all eleven returned values against each input `TransitionFacts` for every legal shape and checks the exact record component types. The boundary record-name assertion protects the same eleven service property names. `OperationsActivationReadsIT.allNinePersistableTransitionCombinationsReturnAllEnums` now keeps distinguishable expected facts keyed by transition UUID, seeds V007-valid rows without weakening constraints, and asserts every expected identifier exactly once, exact eleven JSON keys, nine string types, two integral revisions and all eleven values. It also checks exact page keys, five items, null tail cursor and false `hasMore`.

The five successful shapes are:

| From | To | Channel | Action domain |
| --- | --- | --- | --- |
| INACTIVE | ACTIVE | WEB | ACTIVATION |
| INACTIVE | ACTIVE | MINIAPP | ACTIVATION |
| INACTIVE | ACTIVE | IVR | IVR_CONFIRM |
| ACTIVE | SUSPENDED | OPERATIONS | OPERATIONS |
| SUSPENDED | ACTIVE | OPERATIONS | OPERATIONS |

The four DB-shape-only combinations remain integrity failures: INACTIVE-to-ACTIVE via OPERATIONS/OPERATIONS, and ACTIVE-to-SUSPENDED via WEB/ACTIVATION, MINIAPP/ACTIVATION or IVR/IVR_CONFIRM. Existing negative loops retain whole-page sanitized503, exact `SERVICE_UNAVAILABLE`, and no items/cursor/invalid sentinel leakage for returned-row and lookahead scenarios. PostgreSQL space-only rejection and tab/astral text coverage remain in the same identities. No production mapping, grant, constraint, fixture class or test identity changed.

### PA34-REREVIEW-02 - exact boundary, shared-ring and production wiring guards

Accepted Low finding A F-02 concerned narrower-than-planned guards despite correct production behavior. Its accepted direction was to strengthen the existing two boundary and two configuration methods, without a new pool, property fallback or identity.

- `publishesOnlyExactActivationReadServicePortAndRecords` asserts `Anchor` names `occurredAt,transitionId` and types `Instant,UUID`; `ActivationPage` names `items,nextCursor,hasMore`, erased types `List,String,boolean` and exact generic `List<ActivationSummary>`.
- `ownerReaderSelectsOnlyElevenApprovedTransitionColumns` extracts both owner SELECT lists and requires exactly, in order, `id,subject_id,status_event_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,reason_code,occurred_at`. Existing exact owner FROM, keyset/order/limit and forbidden-column guards remain.
- `activationKeysUseOnlyExactEndpointSpecificProperties` retains its historical name but uses only the existing shared `onecall.operations-subject-read.cursor.*` ring. With a valid shared ring and conflicting complete activation-prefix keys present, it asserts the shared properties bean/ring, bidirectional interoperability with the shared-ring codec and `CURSOR_INVALID` for an activation-only-key token. An invalid shared active ID still yields503 rather than falling back to the activation prefix. Existing activation-only failure and cross-purpose checks remain.
- `enabledPersistenceGraphIsLazyAndReusesQualifiedReadInfrastructure` obtains the actual production `operationsSubjectReadTransactionManager` and `operationsSubjectReadTransactionTemplate` beans. Only the qualified datasource resource is mocked, alongside an unrelated datasource to expose missing qualification; the manager itself is not replaced. It asserts datasource identity, rollback-on-commit-failure and enforced-read-only; template manager identity, read-only, REPEATABLE_READ, REQUIRES_NEW and timeout5; and no datasource interaction/pool creation. Existing lazy-graph/activation-service qualifier checks remain.

These assertions address the accepted gaps as author remediation; only subsequent Controller intake and fresh independent A/B re-review can adjudicate acceptance.

## 3. Exact four-test delta and immutable candidate hashes

The fix2 implementation changed only these four existing test content paths:

1. `backend/src/test/java/com/onecall/archtests/OperationsActivationReadConfigurationTests.java`
2. `backend/src/test/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadServiceTests.java`
3. `backend/src/test/java/com/onecall/archtests/container/OperationsActivationReadsIT.java`
4. `backend/src/test/java/com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java`

Mandatory raw whole-file SHA-256 values were verified before documentation and again after it. The starting/ending values are identical to the supplied Controller full-run candidate hashes, not a later normalized substitute; the saved LF candidate copies also have these exact bytes.

| Test file | Starting SHA-256 | Ending SHA-256 |
| --- | --- | --- |
| `OperationsActivationReadConfigurationTests.java` | `A3B36AB4367D18AE404A67063CA93ECA0358613DB8D5B6E7F5E48F17DD022086` | `A3B36AB4367D18AE404A67063CA93ECA0358613DB8D5B6E7F5E48F17DD022086` |
| `OperationsActivationReadServiceTests.java` | `5D9B5A5095931F3C500895089878579C26C7031FC998A06B5E651DA24CBA92F6` | `5D9B5A5095931F3C500895089878579C26C7031FC998A06B5E651DA24CBA92F6` |
| `OperationsActivationReadsIT.java` | `831B566DAC88BC8AA0A48C790BCD4C31EC518A6CCD42CD47EDB718ED38025C9F` | `831B566DAC88BC8AA0A48C790BCD4C31EC518A6CCD42CD47EDB718ED38025C9F` |
| `OperationsActivationReadBoundaryTests.java` | `C5314D39603500F6C4A21018D61DF9CB9D98B7E13127C358431ECCAA3D788AE1` | `C5314D39603500F6C4A21018D61DF9CB9D98B7E13127C358431ECCAA3D788AE1` |

Evidence root E is `D:\codex-temp\pa34-code-review-rereview-fix1-evidence-20261115-01` (WSL `/mnt/d/codex-temp/pa34-code-review-rereview-fix1-evidence-20261115-01`). Controller `pre-document-scope.log` records the exact four-test candidate delta against `baseline.json`; `baseline-files/` preserves the four original files, and `test-only.patch` is reconstructed exactly from them and the current four files during finalization. This proves no production/CI/POM/fixture change is hidden among the inherited untracked implementation files. The older PA34 implementation/fix1 CI and POM changes relative to HEAD remain inherited; they are not claimed absent from the cumulative PA34 worktree.

Scope chronology matters: a raw comparison of the current finalization start against the older fix2 baseline additionally shows the later Controller-owned `docs/gateflow/project-execution-control.md` update and supplied `.pa34-code-review-rereview-fix1-finalize-prompt.md`. `starting-baseline-delta.log` identifies those inherited bookkeeping paths separately. They are not fix2 candidate edits or finalization edits, and are preserved untouched. Against this task's own starting content snapshot, exactly the report update and this new artifact differ. All Java/tests, CI, POM, fixtures, production/application configuration, OpenAPI, docs/14/15/17, V001-V017, accepted plan/reviews and private content remain unchanged by finalization.

## 4. Controller validation reused, not rerun

All entries below are existing evidence under E, checked against the current immutable candidate. The full Controller rerun, not the interrupted prior run or a focused aggregate, is authoritative for full-suite counts. All named passing test runs have zero failures, errors and skips.

| Validation | Result | Evidence under E |
| --- | --- | --- |
| Focused unit/boundary | 6 suites,60/60; exit0; finished 2026-11-15T09:51:33+08:00 | `focused-unit.log`, `focused-unit.exit`; `focused-surefire-xml/` |
| Focused real PostgreSQL Failsafe | 2 suites,28/28; exit0; finished 2026-11-15T09:53:05+08:00 | `focused-container.log`, `focused-container.exit`; `focused-failsafe-xml/` |
| Controller full clean PostgreSQL16 rerun | 76 suites/704 Surefire +31 suites/504 Failsafe =107 suites/1208; exit0; finished 2026-11-15T10:47:39+08:00; elapsed28:32 | `full-clean-verify-controller-rerun.log`, `full-clean-verify-controller-rerun.exit`; `full-surefire-xml/`, `full-failsafe-xml/` |
| Focused/full exact XML identities | All accepted88 names unchanged; full1208 names equal all source and previously accepted full XML; no missing/extra/duplicate/skip | `accepted-identities.json`, `inventory.py`, `focused-inventory.log`; all four focused/full XML directories |
| Source/CI/POM inventory | PA34 exactly60 Surefire +28 Failsafe; source1208, token-line1209, POM31 exact unique includes; unchanged CI PA34 dictionaries equal accepted88 names | `accepted-identities.json`, `inventory.py`; current unchanged `.github/workflows/ci.yml` / `backend/pom.xml` |
| Canonical LF frozen guards | Selected architecture1 +real PostgreSQL1; exit0; finished 2026-11-15T10:49:29+08:00; exact V001-V017/no V018 | `lf-frozen-controller.log`, `lf-frozen-controller.exit`; `lf-surefire-xml/`, `lf-failsafe-xml/`, `lf-projection.log`, `linux-lf/` |
| Frozen raw/full candidate/scope | Main21 raw frozen hashes match the plan;21 LF inputs match HEAD Git blobs; exact17 migration filenames/no V018; exact four-test candidate delta and hashes in section3 | `baseline.json`, `baseline-files/`, `test-only.patch`, `pre-document-scope.log`; accepted frozen manifest |

The log SHA-256 values independently read during finalization are:

| Existing passing evidence log | SHA-256 |
| --- | --- |
| `focused-unit.log` | `B35B5468CAFEC621B49070943D32F3BB7D43DB491E70F58FE892D517292BAE8C` |
| `focused-container.log` | `668F3A15E0FF4CC5AE13B5389EF518C80A68E099849A78817D0C1CE1432503F0` |
| `full-clean-verify-controller-rerun.log` | `B96308B5CA1CA70AE06BAE89D047AFB394261CA5A5128C9CC9EB3708B718AAD9` |
| `lf-frozen-controller.log` | `3476A18CBEEACCDB541E2DF4E56C2BBBEC09D55150415DBE4338B0869EF8570A` |

No-new-identity proof: the accepted plan manifest, `accepted-identities.json`, current source and CI additions agree on the same six unit classes with12/8/8/10/14/8 methods and two IT classes with18/10 methods. Saved focused XML matches those88 identities exactly. Full XML contains exactly704+504 unique testcase identities, equals current all-source names and the preceding accepted full1208 names. The word-boundary annotation token-line count remains1209 and POM includes31. No method/class was added, removed, renamed, disabled or duplicated. Loops and assertions stay inside existing identities.

Finalization ran only read-only content/JSONL/XML/inventory checks: `python3 E/inventory.py --full`, direct AST comparison of the unchanged CI PA34 dictionaries, patch reconstruction, frozen raw/LF checks and the final two-document audit. Generated outputs are confined to F: `D:\codex-temp\pa34-code-review-rereview-fix2-finalize-20261115-02` (WSL `/mnt/d/codex-temp/pa34-code-review-rereview-fix2-finalize-20261115-02`): `starting-content.json`, `starting-git-pointer.sha256`, `starting-baseline-delta.log`, `identity-verification.log`, `evidence-verification.log`, `provenance-verification.log`, and `final-audit.log`. E is reused read-only; its helpers are inspected before selective execution, not blindly rerun. No new full run, JAR/firewall/A1-A5 run, hosted Actions result or live acceptance is claimed. Historical fix1 packaged/contract evidence remains historical in the report.

`focused-container-skipped-attempt.log/.exit` is an earlier skipped attempt, not the accepted28/28 result. The previous `full-clean-verify.log` was interrupted before author completion and is not substituted for `full-clean-verify-controller-rerun.log/.exit`. Other exploratory/frozen-attempt logs remain historical; only the named passing evidence supports this handoff.

## 5. Provider failure provenance and recovery

The prior run directory is `D:\codex-temp\pa34-code-review-rereview-fix1-run-20261115-01`. Its `fix2-gpt.jsonl` has161 valid nonempty JSONL lines: six `error` events reporting403 Forbidden/insufficient prepaid balance, one `turn.failed`, zero `turn.completed`, and the remaining start/item events. Controller records runner exit1; `fix2-gpt.stderr` is empty. There is no last-message file or new rereview-fix artifact from that failed run. The four authorized test edits survived, but the author documentation/handoff did not complete. This is an external provider-account failure, not a test failure, review verdict or retroactive author success.

The later recovery probe at `D:\codex-temp\pa34-gpt-recovery-probe-20261115-01` returned `GPT_RESTORED` in `probe.last.md`; `probe.jsonl` has the matching agent message and `turn.completed`, with empty `probe.stderr`. This establishes restored provider access, not validation of PA34. Controller independently completed the full rerun and canonical LF validation after the interruption. This finalization is a fresh documentation task over those verified bytes and evidence; it does not erase or relabel the failed run.

## 6. Residual risks and open questions

- No new blocking hash mismatch or concrete code/test defect was found during author finalization. Both accepted findings have inherited test-only repairs and Controller validation; their independent acceptance remains pending fresh A/B re-review.
- Platform/Backend retains the deferred root/trailing-slash error-path mapping question and coordination of `server.error.path` with Boot's `spring.web.error.path` for non-default deployments. Tested default/custom destinations do not prove every deployment profile. B-F04 remains deferred to later Operations route-boundary/contract work; no broader TRACE behavior is changed.
- Backend/DBA retains global-sort/index capacity and timeout risk, and Backend/DBA + Operations owns a separately authorized corrupt-fact remediation runbook. Domain-impossible facts still fail the whole containing page, including lookahead; no repair, row-dropping, migration or V018 is authorized.
- Revocation remains repeatable-read snapshot-relative, with fresh snapshots for continuation pages. Production employee verifier/session/MFA/provisioning, runtime role credentials and shared-ring custody/rotation remain external Security/Operations IAM/DBA prerequisites. The accepted PA33 bean/enablement coupling remains unchanged.
- The standalone customer-filter test limitation noted by reviewers remains bounded by existing real-HTTP customer401 coverage. LF-only A1-A5 evidence, no hosted/live acceptance claim and existing development-tool vulnerabilities retain their historical ownership; this documentation task supplies no new runtime or production assurance.
- Historical `PA34-FIX-HYGIENE-01` raw-index dual-digest observation is retained in fix1/report section11. Control subsequently `rejected-with-reason` its status as a blocking finding after content/HEAD/staging checks. This task does not reopen it, claim continuous raw-index-byte preservation, weaken the old failed check or repair Git state; current scope/HEAD/pointer/empty staging are the semantic checks.

## 7. Finish checks and Controller handoff

The final audit in F records exact starting/ending mandatory test hashes, unchanged checkpoint HEAD/worktree pointer, no staged paths, and exactly two changed content paths from the finalization snapshot: the existing implementation report and this sole new artifact. It includes inherited untracked implementation/private files, not only tracked `git diff`; all protected content is preserved. Both documentation files pass strict UTF-8/no BOM, trailing-whitespace, conflict-marker, U+FFFD and final-newline checks, including `git diff --no-index --check` for untracked content. Current frozen raw hashes/migration inventory remain exact.

No staging, commit, push, branch creation, PR creation/change, approval, review request, merge, mark-ready, comment or re-review entry is performed. Stop for Controller intake and fresh independent A/B re-review of this same frozen candidate. This artifact and report are author finalization only, not independent review/acceptance or production readiness.
