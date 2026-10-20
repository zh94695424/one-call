# Cross-channel alignment aggregate deepreview fix

## Gate and decision boundary

- Work unit: cross-channel alignment.
- Gate: aggregate deepreview `fix` for accepted findings `A-01`, `A-02`, `A-03`, `B-01`, `C-01`, and `C-02`, plus four repository-hygiene failures.
- Status: implementation and local validation complete; every substantive finding is **implemented, awaiting independent re-review**.
- This artifact does not perform re-review, accept/close any finding, advance the Gateflow state, stage/commit/push changes, or create a PR.
- `docs/gateflow/cross-channel-alignment-control.md` was not edited by this fix round. Its existing dirty change belongs to the controller.

## Finding fixes and evidence

### A-01 - canonical evidence must have exactly one event owner

The shared contract now requires bidirectional `EVIDENCE` coverage. Every canonical evidence record is owned by exactly one valid `processedEvents.EVIDENCE` record, and every event result references an existing canonical evidence record with matching subject/session/input facts. Import, old-key replay, and new writes reject missing owners, dangling event results, and duplicate ownership atomically.

Operations historical evidence is now created through `consumeTrial`, so it has the same canonical ownership as dynamic evidence. Shared tests cover missing event owners, dangling/replaced result IDs, duplicate ownership, valid multiple-evidence history, import, old-key replay, and new-key rejection.

### A-02 - claim risk precedes replay and same-account duplicate handling

`claimSubject` checks `CONFLICT` and `RECYCLED` before event replay and before same-account duplicate handling. A prior request ID, a new request ID, an existing account association, or a valid current verification cannot bypass risk review. Rejections leave the complete state unchanged.

Shared, Mini, and Web regressions cover both risks with original and new request IDs. Client pages keep protected evidence/trial identifiers hidden and show only the offline-review path.

### A-03 - catalog schedule matrix belongs to the shared contract

`validateCatalogVersion` now enforces:

- `SUBSCRIPTION`: `MONTHLY`, `QUARTERLY`, or `ANNUAL`, with positive integer `installments` and `grantIntervalMonths`.
- `ONE_TIME` / `ADD_ON`: `ONCE`, one installment, and zero-month interval.
- `QUOTE`: `QUOTE`, one installment, and zero-month interval.

A custom subscription with a three-month interval and four installments remains valid. Invalid combinations are rejected by catalog import, order creation, and entitlement issuance without partial mutation. Shared tests exercise the valid custom case and invalid matrix combinations.

### B-01 - exhausted trial fixture is monotonic

The Operations exhausted scenario is seeded once at `280/300` and advances to `300/300` through canonical `consumeTrial`. Closing and reopening the scenario no longer rewinds consumption or rewrites history. QA asserts the grant remains at 300 seconds, evidence and `EVIDENCE` event ledgers remain byte-for-byte stable, a repeated use is rejected, and the rejection is atomic.

### C-01 - Mini fixed-line success follows a canonical mutation

The fixed-line flow creates or reuses `MINI-LANDLINE-01088886688`, records `CALLER_CHALLENGE`, associates the current account, invokes shared `activateService`, and uses the normal first-trial policy. The success page is guarded by the canonical subject and MINIAPP activation, and it disclaims proof of registered name or property ownership.

QA verifies one activation, one grant, result/trial/number-page subject continuity, duplicate replay idempotency, rejection atomicity, and screenshots. Cross-channel QA imports the resulting subject into Operations and Web. During final integration, a verified subject whose source was IVR exposed an overly narrow Mini identity lookup; `activateLandline` now accepts any `VERIFIED` subject for the current account while still rejecting an account that has only unverified subjects. The ordinary mobile path was preserved unchanged.

### C-02 - Web lifecycle actions mutate canonical activation state

`WEB-ACCOUNT-001` starts inactive and is activated through shared `activateService`; its authorization record references that real activation ID. Stop/reactivate uses shared `setActivationStatus`, which updates the canonical subject status and revision and appends an event-owned `activationTransitions` record. Reactivation authorization references the real transition ID and does not regrant trial.

Number detail and authorization views render the canonical status, revision, and activation/transition IDs. Touched lifecycle views no longer fabricate a carrier activation receipt or carrier-side status. QA covers stop/restore, unchanged activation/grant counts, transition revisions/IDs, exact replay, conflicting event-key reuse, malformed/missing transition-event import rejection, and visible stopped/restored states.

### EOF hygiene

The extra terminal blank line was removed from exactly these four files:

```text
docs/10-cross-channel-rules.md
docs/gateflow/cross-channel-alignment-s3-fix.md
docs/gateflow/cross-channel-alignment-s3-implementation.md
shared/README.md
```

The two completed S3 artifacts received no content change beyond removal of the extra EOF blank line.

## Schema and compatibility note

The local review schema remains `schemaVersion: 1`, but its required business shape now includes `activationTransitions` and the `ACTIVATION_STATUS` event domain. A snapshot missing either required structure, or with incomplete/malformed bidirectional transition ownership, is intentionally rejected. Transition IDs use the `ASTAT` allocator and import collision checks. This is a local fixture contract, not a production migration or compatibility promise.

## Exact fix-round non-image manifest

These 21 files were edited or created by this aggregate fix, distinct from the pre-existing work-unit diff, controller file, and aggregate review artifact:

```text
docs/09-business-activation-and-lean-admin.md
docs/10-cross-channel-rules.md
docs/gateflow/cross-channel-alignment-aggregate-fix.md
docs/gateflow/cross-channel-alignment-s3-fix.md
docs/gateflow/cross-channel-alignment-s3-implementation.md
operations-prototype/README.md
operations-prototype/onecall-operations-admin.html
operations-prototype/preview.html
operations-prototype/qa/qa.js
prototype/README.md
prototype/onecall-miniapp-pages.html
prototype/preview.html
prototype/qa/qa.js
shared/README.md
shared/onecall-business.js
shared/qa/business.test.js
shared/qa/cross-channel.js
web-prototype/README.md
web-prototype/evidence-hub.html
web-prototype/preview.html
web-prototype/qa/qa.js
```

The three previews were regenerated by the existing runner/build flow and were not hand-edited. `docs/reviews/code-review-20261020-202826.md` is the pre-existing aggregate review input, not a fix-round write.

## Validation chronology

| Order | Command / check | Result |
| --- | --- | --- |
| 1 | `node --test shared/qa/business.test.js` | PASS, 24/24 |
| 2 | `node --test shared/qa/runner-exit.test.js` | PASS, 1/1 |
| 3 | `node shared/build-prototypes.js --check` | PASS; generated shared sections current |
| 4 | Required `node --check` set for shared module/generator/tests/cross runner and all three browser QA files | PASS |
| 5 | Every nonempty inline script in all six current source/preview HTML files parsed with `vm.Script` | PASS, 11 scripts; the first direct `node -e` attempt did not execute because PowerShell stripped JavaScript quotes, then the stdin-based invocation passed |
| 6 | `powershell -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1` | PASS; employee/catalog/history and all twelve IVR scenarios, including monotonic exhaustion |
| 7 | `powershell -ExecutionPolicy Bypass -File prototype/qa/run.ps1` | PASS; 61 pages, mobile/fixed-line flows, claims, commerce, and 320px checks |
| 8 | `powershell -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS; lifecycle, authorization, content, roles, and responsive checks |
| 9 | Initial `node shared/qa/cross-channel.js` diagnostics | Three intermediate attempts failed at the new fixed-line success assertion and exposed the IVR-origin verified-subject edge; no other assertion was reached or claimed as passing in those attempts |
| 10 | Affected Mini runner after the final fixed-line-only correction | PASS; preview regenerated; ordinary mobile logic restored unchanged |
| 11 | Post-correction cross-runner syntax, generated-section check, and six-file inline-script parse | PASS |
| 12 | Final `node shared/qa/cross-channel.js` | PASS; publication, stale/off-sale/channel checkout, IVR claim/evidence/trial continuity, Web/Mini payment and issuance, all event replays, fixed/conflict/recycled paths, forbidden imports, five reference tamper cases, account scope, support, generated hashes, and no browser errors |

The final cross-channel suite was the last browser-based validation. No browser runner was executed afterward. The intermediate failures were resolved within the accepted C-01 scope and are not omitted from this record.

## Screenshot delta and visual inspection

SHA-256 hashes for all existing `alignment-*.png` files were captured before this aggregate fix and compared after the final cross-channel run. The exact delta is **12 added, 27 changed, 0 deleted**. Full runner refreshes can change timestamps and generated IDs on screens unrelated to a finding; those files remain included.

All 39 added/changed files below were opened with `view_image`. Checks covered nonblank rendering, clipping, overlap, status consistency, canonical IDs/revisions, risk-history hiding, fixed-line continuity, and exhausted-state presentation. No new visual defect was found. The known 1024px export-task screenshot captures only the left portion of a horizontally scrollable table; the 1440px image shows the complete action columns. That pre-existing limitation remains outside this fix scope.

Added (12):

```text
operations-prototype/qa/screenshots/alignment-cross-landline-activation-1440.png
operations-prototype/qa/screenshots/alignment-ivr-exhausted-reopened-1440.png
prototype/qa/screenshots/alignment-claim-same-account-conflict-393.png
prototype/qa/screenshots/alignment-claim-same-account-recycled-393.png
prototype/qa/screenshots/alignment-cross-landline-activation-393.png
prototype/qa/screenshots/alignment-landline-activation-numbers-393.png
prototype/qa/screenshots/alignment-landline-activation-result-393.png
prototype/qa/screenshots/alignment-landline-activation-trial-393.png
web-prototype/qa/screenshots/alignment-claim-same-account-conflict-1024.png
web-prototype/qa/screenshots/alignment-claim-same-account-recycled-1024.png
web-prototype/qa/screenshots/alignment-number-canonical-restored-1280.png
web-prototype/qa/screenshots/alignment-number-canonical-stopped-1280.png
```

Changed (27):

```text
operations-prototype/qa/screenshots/alignment-cross-payment-pending-1440.png
operations-prototype/qa/screenshots/alignment-ivr-exhausted-1440.png
operations-prototype/qa/screenshots/alignment-login-1440.png
operations-prototype/qa/screenshots/alignment-user-trial-1440.png
prototype/qa/screenshots/alignment-cross-account-scope-profile-393.png
prototype/qa/screenshots/alignment-cross-payment-pending-393.png
prototype/qa/screenshots/alignment-cross-returning-393.png
prototype/qa/screenshots/alignment-profile-760.png
web-prototype/qa/screenshots/alignment-auditor-export-partial-1280.png
web-prototype/qa/screenshots/alignment-authorization-reconfirmed-1280.png
web-prototype/qa/screenshots/alignment-authorization-revoked-detail-1280.png
web-prototype/qa/screenshots/alignment-call-ai-version-1024.png
web-prototype/qa/screenshots/alignment-certificate-new-review-1280.png
web-prototype/qa/screenshots/alignment-claim-returning-1024.png
web-prototype/qa/screenshots/alignment-cross-payment-pending-1280.png
web-prototype/qa/screenshots/alignment-cross-returning-1280.png
web-prototype/qa/screenshots/alignment-enterprise-organization-1280.png
web-prototype/qa/screenshots/alignment-export-tasks-1024.png
web-prototype/qa/screenshots/alignment-export-tasks-1440.png
web-prototype/qa/screenshots/alignment-number-add-synchronous-result-1280.png
web-prototype/qa/screenshots/alignment-number-detail-1024.png
web-prototype/qa/screenshots/alignment-number-detail-1280.png
web-prototype/qa/screenshots/alignment-number-disabled-1280.png
web-prototype/qa/screenshots/alignment-payment-pending-1280.png
web-prototype/qa/screenshots/alignment-personal-order-detail-1024.png
web-prototype/qa/screenshots/alignment-schedule-annual-history-1024.png
web-prototype/qa/screenshots/alignment-schedule-custom-history-1024.png
```

## Residual risks and ownership

| Classification | Remaining item | Owner / destination |
| --- | --- | --- |
| Current gate follow-up | A-01/A-02/A-03/B-01/C-01/C-02 and EOF fixes require independent re-review. | Controller / independent reviewer |
| Local fixture boundary | The new transition/event ownership rules are in-memory demo semantics; no persistent transaction, migration, concurrent reservation, or distributed replay service exists here. | Backend / data / IVR work units |
| Identity and claim proof | WeChat, SMS, caller challenge, identity completion, conflict, and recycled-number states are deterministic fixtures, not real proof or risk-service integrations. | Identity / security / carrier integration work units |
| Payment and fulfillment | Payment verification, reconciliation, transactional issuance, recurring scheduling, renewal/stacking/refund rules, and production catalog approval remain unimplemented or unapproved. | Payment / backend / product / finance |
| Build/release | Review hooks must be disabled before production reuse; portable browser/CI and real integration validation remain future release work. | Build / release |
| Existing visual evidence limit | The 1024px export-task table screenshot is intentionally partial because right-side columns require horizontal scroll; 1440px provides full-table evidence. | Web UI owner / future layout scope |
| External gate dependency | Push, draft PR, remote review, and later Gateflow actions remain outside this handoff and require their authorized gates. | Repository maintainer / controller |

No residual risk is being reclassified as fixed beyond the six accepted findings and four EOF hygiene items. No finding acceptance or gate advancement is claimed.

## Round 2 - aggregate re-review fixes

### Gate and decision boundary

- Gate: aggregate deepreview `fix` after the independent re-review in `docs/reviews/code-review-20261020-213407.md`.
- Scope: the remaining C-02 fixed-line path, N-01, and N-02 only.
- Status for all three findings: implemented, awaiting independent re-review.
- This round does not perform re-review, accept or close a finding, advance the Gateflow state, edit the controller, stage or commit files, push, create a PR, or run deepreview.

### Finding status and implementation evidence

| Finding | Round-2 status | Implementation evidence |
| --- | --- | --- |
| C-02 remaining - Web fixed-line lifecycle bypassed canonical state | **Implemented, awaiting independent re-review** | The success-valued local fallback and local disabled-number truth were removed. Web resolves an actionable number to exactly one canonical subject by account plus exact masked number, seeds deterministic mobile and landline subjects, and routes stop/restore only through shared `setActivationStatus`. Missing or ambiguous resolution rejects without changing `#content` or the canonical snapshot. The Mini-created `010 **** 6688` subject is reused instead of duplicated. Browser and cross-channel QA cover mobile and landline stop/restore, revision anchors, transition/event ownership, replay/conflict, no trial regrant, import, and atomic failure. Enterprise member authorization actions also resolve deterministic canonical subjects for `139 **** 7812` and `186 **** 3360`. |
| N-01 - Operations bypassed the shared lifecycle owner | **Implemented, awaiting independent re-review** | `OPERATIONS` is an explicit activation-status source. Operations suspend/resume calls `setActivationStatus` directly and no longer writes `subject.activation` or `subject.revision`. Fresh subjects and subjects carrying Web history now receive one auditable transition with the correct source, revision increment, and immutable replay result. Browser QA and shared tests cover fresh state, existing Web history, exact replay, conflicting key reuse, invalid current state, and atomic failure. |
| N-02 - transition order and old-key replay lacked complete ledger validation | **Implemented, awaiting independent re-review** | Every activation transition now requires `fromRevision` and `toRevision`, with exactly one revision increment. Shared `activationTransitionLedgerValid` enforces per-subject revision order, state continuity, non-regressing timestamps, non-overlapping revision anchors, and alignment with the subject's final revision/status. Snapshot/import validation and the `setActivationStatus` pre-replay path use the same complete validator, so malformed old-key and new-key calls reject atomically. Tests cover reorder, duplicate/overlapping and decreasing revisions, state discontinuity, timestamp regression, final-state mismatch, replay, import, and new writes. |

### Strict schema-version-1 decision

`schemaVersion` intentionally remains `1`. Its accepted shape is strict: activation transitions require `fromRevision` and `toRevision`, `OPERATIONS` is an allowed transition source, and every imported transition ledger must satisfy the complete ordering and final-state invariants. A prior local snapshot that lacks the revision anchors or violates the now-required ledger shape is intentionally rejected rather than inferred or silently upgraded. This is a deterministic prototype fixture contract, not a production persistence migration or backwards-compatibility promise.

### Exact round-2 non-image manifest

These 17 non-image files were edited by round 2. The three source pages and three previews contain generated shared sections; previews were regenerated by the existing build/runner flow and were not hand-edited.

```text
docs/09-business-activation-and-lean-admin.md
docs/10-cross-channel-rules.md
docs/gateflow/cross-channel-alignment-aggregate-fix.md
operations-prototype/README.md
operations-prototype/onecall-operations-admin.html
operations-prototype/preview.html
operations-prototype/qa/qa.js
prototype/onecall-miniapp-pages.html
prototype/preview.html
shared/README.md
shared/onecall-business.js
shared/qa/business.test.js
shared/qa/cross-channel.js
web-prototype/README.md
web-prototype/evidence-hub.html
web-prototype/preview.html
web-prototype/qa/qa.js
```

`docs/gateflow/cross-channel-alignment-control.md` and existing files under `docs/reviews/` were not edited by this round.

### Round-2 validation chronology

| Order | Command / check | Result |
| --- | --- | --- |
| 1 | Initial `node --test shared/qa/business.test.js` after the shared changes | Behavioral assertions passed, but the generated-hash assertion failed because generated source/preview sections were stale. The existing generator refreshed them; the next full run passed `27/27`. |
| 2 | `node --test shared/qa/runner-exit.test.js` | PASS, `1/1`; controlled Web child failure still propagates. |
| 3 | `node shared/build-prototypes.js --check` and eight relevant `node --check` commands | PASS; all generated sections current and all shared/generator/test/cross/browser QA JavaScript parses. |
| 4 | Inline scripts in the six source/preview HTML files | The first direct parser command did not execute because PowerShell stripped JavaScript quotes. The stdin-based parser then passed all 11 nonempty scripts. |
| 5 | Operations full browser runner | PASS; fresh and Web-history Operations lifecycle paths, transition ownership, revision/audit state, replay/conflict, and atomic failure covered. |
| 6 | Mini full browser runner | PASS; existing mobile and fixed-line canonical activation flows remain intact. |
| 7 | First Web full browser runner | The new lifecycle atomicity assertions passed; the run failed only because the QA check used nonexistent `#toast-root`. The selector was corrected to the existing `#toast`. |
| 8 | Corrected Web full browser runner | PASS; fresh mobile and landline stop/restore/replay/failure coverage completed. |
| 9 | Cross-channel runner after the corrected Web run | PASS; imported fixed-line stop/restore and canonical continuity covered. |
| 10 | Final actionable-enterprise-member audit | Deterministic canonical subjects were added for `139 **** 7812` and `186 **** 3360`; this source-only fixture correction did not change generated shared sections. |
| 11 | Final `node shared/build-prototypes.js --check` | PASS; `ONECALL business sections are current`. |
| 12 | Final eight `node --check` commands | PASS for shared module, generator, business tests, runner-exit test, cross-channel runner, and all three browser QA files. |
| 13 | Final stdin-based inline parser | PASS, 11 nonempty inline scripts across six HTML files. |
| 14 | Final `node --test shared/qa/business.test.js` | PASS, `27/27`. |
| 15 | Final `node --test shared/qa/runner-exit.test.js` | PASS, `1/1`. |
| 16 | Final `powershell -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS; login/session, interaction, role authorization, content, and responsive checks. |
| 17 | Final `node shared/qa/cross-channel.js` | PASS; publication, frozen checkout constraints, IVR continuity, payment/issuance, all replays, fixed/conflict/recycled flows, forbidden imports and reference tamper, account scope, support, generated hashes, and no browser errors. |

The final cross-channel suite was the last browser execution. No browser runner was invoked afterward. Intermediate failures are retained above because they are part of the evidence chronology; each was caused by stale generated evidence or the QA harness rather than a concealed product assertion failure.

### Round-2 screenshot delta and visual inspection

The round-2 baseline contained 139 `alignment-*.png` files. SHA-256 comparison after the final cross-channel run found exactly **4 added, 29 changed, 0 deleted**, for 143 current files.

Added (4):

```text
web-prototype/qa/screenshots/alignment-cross-landline-lifecycle-restored-1280.png
web-prototype/qa/screenshots/alignment-cross-landline-lifecycle-stopped-1280.png
web-prototype/qa/screenshots/alignment-number-landline-canonical-restored-1280.png
web-prototype/qa/screenshots/alignment-number-landline-canonical-stopped-1280.png
```

Changed (29):

```text
operations-prototype/qa/screenshots/alignment-cross-landline-activation-1440.png
operations-prototype/qa/screenshots/alignment-ivr-exhausted-1440.png
operations-prototype/qa/screenshots/alignment-ivr-exhausted-reopened-1440.png
operations-prototype/qa/screenshots/alignment-user-trial-1440.png
web-prototype/qa/screenshots/alignment-auditor-export-partial-1280.png
web-prototype/qa/screenshots/alignment-authorization-reconfirmed-1280.png
web-prototype/qa/screenshots/alignment-authorization-revoked-detail-1280.png
web-prototype/qa/screenshots/alignment-certificate-new-review-1280.png
web-prototype/qa/screenshots/alignment-claim-returning-1024.png
web-prototype/qa/screenshots/alignment-cross-account-scope-rights-1280.png
web-prototype/qa/screenshots/alignment-cross-conflict-1280.png
web-prototype/qa/screenshots/alignment-cross-recycled-1280.png
web-prototype/qa/screenshots/alignment-cross-returning-1280.png
web-prototype/qa/screenshots/alignment-cross-support-1280.png
web-prototype/qa/screenshots/alignment-enterprise-access-1280.png
web-prototype/qa/screenshots/alignment-enterprise-members-1280.png
web-prototype/qa/screenshots/alignment-enterprise-order-detail-1280.png
web-prototype/qa/screenshots/alignment-enterprise-organization-1280.png
web-prototype/qa/screenshots/alignment-export-tasks-1024.png
web-prototype/qa/screenshots/alignment-export-tasks-1440.png
web-prototype/qa/screenshots/alignment-number-add-synchronous-result-1280.png
web-prototype/qa/screenshots/alignment-number-canonical-restored-1280.png
web-prototype/qa/screenshots/alignment-number-canonical-stopped-1280.png
web-prototype/qa/screenshots/alignment-number-disabled-1280.png
web-prototype/qa/screenshots/alignment-payment-pending-1280.png
web-prototype/qa/screenshots/alignment-personal-order-detail-1024.png
web-prototype/qa/screenshots/alignment-personal-plans-1024.png
web-prototype/qa/screenshots/alignment-schedule-annual-history-1024.png
web-prototype/qa/screenshots/alignment-schedule-custom-history-1024.png
```

All 33 added or changed screenshots were opened at original resolution with `view_image`. The inspection covered nonblank output, framing, clipping, overlap, lifecycle status/revision/transition consistency, risk-history hiding, fixed-line continuity, dense tables, and responsive layouts. No new visual defect was found. The existing 1024px export-task screenshot still shows only the left portion of its horizontally scrollable table; the 1440px companion image shows the complete action columns. That already-classified evidence limitation remains outside this fix scope.

### Round-2 residual risks and ownership

| Classification | Remaining item | Owner / destination |
| --- | --- | --- |
| Current gate follow-up | C-02 remaining, N-01, and N-02 require independent re-review; this artifact does not accept or close them. | Controller / independent reviewer |
| Local schema boundary | Strict schema-version-1 transition anchors intentionally reject older or malformed local snapshots; no migration/upgrader is provided. | Shared contract owner; production data migration work unit if persistence is introduced |
| Runtime boundary | Lifecycle atomicity and replay are in-memory prototype semantics, without durable transactions, concurrency control, or distributed idempotency. | Backend / data / Operations integration work units |
| Identity and carrier proof | Mobile, fixed-line, SMS, WeChat, caller challenge, conflict, and recycled-number states remain deterministic fixtures rather than real proof/risk/carrier integrations. | Identity / security / carrier / IVR work units |
| Payment and fulfillment | Real payment verification, reconciliation, transactional issuance, recurring scheduling, refund, renewal, and stacking rules remain outside this local fix. | Payment / backend / product / finance |
| Build and release | Review hooks must be disabled before production reuse; portable browser/CI and real integration validation remain future release work. | Build / release |
| Existing visual evidence limit | The 1024px export-task screenshot is horizontally partial; its 1440px companion provides the full-table evidence. | Web UI owner / future responsive-table scope |
| External gate dependency | Re-review, accepted-finding disposition, staging/commit, push, draft PR, remote review, and later Gateflow actions remain outside this handoff. | Controller / repository maintainer |

All residual items are classified with an owner or destination. No new blocker is hidden by the passing local suites, and no later gate is claimed.

## Round 3 - remaining C-02 enterprise authorization revocation

### Gate and decision boundary

- Input finding: the remaining C-02 in `docs/reviews/code-review-20261020-225338.md`; controller decision: `docs/gateflow/cross-channel-alignment-control.md`, aggregate round-3 fix for enterprise authorization revocation only.
- Branch: `feat/operations-admin-prototype`.
- Status: **implemented, awaiting independent re-review**.
- This implementation handoff does not perform independent re-review, accept or close findings, advance the gate, run deepreview, edit the controller or existing reviews, stage/commit/push, or create a PR.

### Implementation and behavior evidence

The Web `agreement-revoke` handler now reads the current visible authorization record and requires that it is the active detail record, currently valid, confirmed for revocation, and permitted for the current role. It passes that authorization directly to the existing `setNumberActivation` path. That path uniquely resolves account plus exact masked number and calls shared `setActivationStatus`. Whether `numberRecords()` renders a display card no longer determines whether the canonical authorization subject exists. The resolver does not force a fixture `subjectId`, so the imported Mini same-number subject continues to be reused.

| Independent fresh enterprise-admin case | Resolved same-account masked number | Verified canonical result |
| --- | --- | --- |
| `auth-6688` | `010 **** 6688` | Unique subject, ACTIVE / revision 2 to SUSPENDED / revision 3 |
| `auth-9056` | `021 **** 9056` | Unique subject, ACTIVE / revision 2 to SUSPENDED / revision 3 |
| `auth-3360` | `186 **** 3360` | Unique subject, ACTIVE / revision 2 to SUSPENDED / revision 3 |
| Newly reconfirmed version of `auth-7812` | `139 **** 7812` | New authorization version, unique subject, ACTIVE / revision 2 to SUSPENDED / revision 3 |

Web QA opens a fresh independent browser page for each case, uses the visible login/workspace/authorization-snapshot/confirmation flow, and verifies exactly one WEB activation transition with revision anchors and one owning processed event. Only the selected subject's activation/revision changes; all original canonical facts, grants, evidence, orders, other subjects, and existing event entries remain unchanged. The visible revocation timeline references the committed transition, original consent items remain unchanged, and the valid authorization no longer offers another revoke action.

The same Web regression covers the member's own reconfirmed `auth-7812` and the evidence manager's own `auth-3360`. Unrelated authorizations remain hidden/inaccessible to those roles; the auditor remains read only. Forged forbidden/repeated revoke attempts preserve the entire canonical snapshot and `#content.innerHTML`. Missing and ambiguous canonical subjects are tested through actual confirmation clicks; they return NOT_FOUND / INVALID_TRANSITION respectively, with the entire snapshot and content DOM unchanged.

Cross-channel QA independently reloads fresh Web state for all four administrator cases, performs their visible revocations, and imports each complete resulting snapshot through the actual Operations and Mini review hooks. The shared load helper verifies complete snapshot equality after every import, including transition/event ownership and unchanged grant/evidence facts. The existing Mini-import fixed-line lifecycle regression remains in the final passing cross-channel suite.

The unrelated pre-existing enterprise number-card display branch is deliberately outside this accepted finding's implementation scope.

### Exact round-3 non-image manifest

Exactly these five non-image files were edited by this round; the preview was regenerated through the existing runner flow, not hand-edited.

```text
web-prototype/evidence-hub.html
web-prototype/preview.html
web-prototype/qa/qa.js
shared/qa/cross-channel.js
docs/gateflow/cross-channel-alignment-aggregate-fix.md
```

No shared business-module change, Operations/Mini source or QA change, README change, controller edit, or existing review edit was made by round 3. Pre-existing dirty/untracked workspace files remain preserved. The generator reported `Updated 0 shared sections`.

### Round-3 validation chronology

All product/test checks below passed on their first execution in this round, with no product or test correction cycle.

| Order | Command / check | Result |
| --- | --- | --- |
| 1 | `node --test shared/qa/business.test.js` | PASS, 27/27 |
| 2 | `node --test shared/qa/runner-exit.test.js` | PASS, 1/1; controlled child failure propagates |
| 3 | `node shared/build-prototypes.js --check` | PASS, shared sections current |
| 4 | Eight `node --check` commands | PASS: shared module, generator, business test, runner-exit test, cross-channel runner, Operations QA, Mini QA, Web QA |
| 5 | Six source/preview HTML files parsed through `vm.Script` | PASS, all 11 nonempty inline scripts |
| 6 | `powershell -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS, full Web suite including all new authorization, role, and atomic-failure regressions; no page/console errors |
| 7 | `node shared/qa/cross-channel.js` | PASS, full cross-channel suite including all four enterprise-admin authorization revocations/imports and existing Mini-import same-number continuity; no browser errors |
| 8 | `git diff --check` | PASS; only ordinary LF/CRLF notices |

Cross-channel was the last browser execution; no browser was started afterward. Operations/Mini full standalone runners were not rerun because their implementations were unchanged in this round; their real import surfaces were exercised by the cross-channel suite. A read-only exploratory `rg` command initially had a PowerShell quoting error and was immediately corrected; it did not execute validation, mutate files, or constitute a product/test failure.

Post-append static handoff checks passed: `git diff --check`, explicit artifact whitespace check, focused source/QA/cross-channel inspection, and Web source/preview byte equality. Both Web files have SHA-256 `cde926c10d4b273a7f14f35786ea2a861c9b0eebb9a3fff476d2675777bccc9b`. The final 147-image hash inventory still exactly matches the saved post-run inventory. Branch remains `feat/operations-admin-prototype`, HEAD remains `df5250865f94d82eac4e167e44ae8e9fa4853d7d`, staging is empty, and no Git remote is configured. Only this documentation record was edited after the passing suites, so those suites were not repeated.

### Round-3 screenshot delta and visual inspection

The saved before/after SHA-256 inventories contain 143 and 147 `alignment-*.png` files. Exact delta: **4 added, 19 changed, 0 deleted**. These are runner-generated side effects; no screenshot was deleted or manually edited. The following table is the durable path/hash manifest. “Viewed” means opened at original resolution in this round.

| Delta | Repository path | Before SHA-256 | After SHA-256 | Viewed |
| --- | --- | --- | --- | --- |
| Added | `web-prototype/qa/screenshots/alignment-authorization-admin-3360-revoked-1280.png` | absent | `f5faa468927890fb21c5aeb9ec52f10acdb9a9648b7a056bf91c6b65f72236dc` | Yes |
| Added | `web-prototype/qa/screenshots/alignment-authorization-admin-6688-revoked-1280.png` | absent | `854965ab047fd0f590f70619d56becec33c4f7822d0887e661ff91f992ea23d8` | Yes |
| Added | `web-prototype/qa/screenshots/alignment-authorization-admin-7812-revoked-1280.png` | absent | `0d232c1b6f4b1431888331e10304a395d497f77a51d1a830fdf5a77ae5f4876a` | Yes |
| Added | `web-prototype/qa/screenshots/alignment-authorization-admin-9056-revoked-1280.png` | absent | `791c00f7723893837f842c427cbef07e8998f765d84aa002c1db83f56a2b93ab` | Yes |
| Changed | `operations-prototype/qa/screenshots/alignment-cross-landline-activation-1440.png` | `e136bcf22945eb5a4f632c3f2868f9c44b9aac42b9badffa8fd1147e164bcc12` | `cb047f3f36c07e0a802d3d8d4fe183012314b8beeb9a718f47fffd2622378479` | No |
| Changed | `web-prototype/qa/screenshots/alignment-auditor-export-partial-1280.png` | `25f3006cf5c921d647005655f1d268aa6fef9f10d586fe22aed68000153c166c` | `76e87db89fe3ff54c96691560f302d9ca1038850a2cbed66aff750b5070ccfe6` | No |
| Changed | `web-prototype/qa/screenshots/alignment-authorization-reconfirmed-1280.png` | `e5545b4b6e42d4b957cfe1f0e8de94256c2aa496de00f19ead6418aacab322c0` | `64fc3b1d74d9f843c54f20ee73288b0f91812aa9aa987a870ef751c599cf95d0` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-authorization-revoked-detail-1280.png` | `c28e334e9d44ab2e62fcf956ae0cdaa67151e8078d45a5f7dee70fef307ccb88` | `cdcac6172ca05aba27df7509b89afa3dda188b050027d1888f0982ccf6663815` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-certificate-new-review-1280.png` | `ba0554feb3625d0ed8fe442d35e68382aa68b4638f2b6446cad28604c99653be` | `dde05c7cbdf0acffc38b53bacefa68189488837ee0c38d72fa5e6ddf49220256` | No |
| Changed | `web-prototype/qa/screenshots/alignment-claim-returning-1024.png` | `27a644f41fd17291e6a4ce7bff7219f75f0f4de6dc2bbb5d5ebfdb2c57396fac` | `b41d4d13a8b527e190c33039dde5b5236586bc74992f709e6624c6e6c717792f` | No |
| Changed | `web-prototype/qa/screenshots/alignment-cross-landline-lifecycle-restored-1280.png` | `62b944c743446cca3b83a9ca0f82f04bbedf2b048e203b34f96d150b8c628baf` | `51ea7259281bc2384e8b609d2cd7ebed6ae2dc12a94acbdc697ab0696ea42a27` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-cross-landline-lifecycle-stopped-1280.png` | `801c2771ecb9d63368152b76471b9fcdbaf6932918b2bec8959f716b19eee0d6` | `91f5a179126ba4945c8f17a2b8ff1ade87a14f70ee576ab7ef8fe9a90c793492` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-cross-returning-1280.png` | `4a686a59f5de1d59c84c436e8b178111561fab4903ecd7aa9688265488e040d7` | `73ee25d36a9d1842aa57dd267904c42d4cbb0db44876fb707c0df1839a5ffca9` | No |
| Changed | `web-prototype/qa/screenshots/alignment-cross-support-1280.png` | `95e63bbe7b0b04f268bf6e5fadc61782a0eee3cfa560014b31c45c2a8d01f6ce` | `3ca0aa871921b5adc52ce6c343dd1ac082fbed94b5a14dd46ee3ae8cfd0b8273` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-export-tasks-1024.png` | `cec30adbd0d1abb23cbdddcfed4bf2704f0a63e29ed75f28e3574186e1951f5b` | `04f8f735729e36a1a4b7f1674fbf87816e06e904be61ad3bc70382b65b9429da` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-export-tasks-1440.png` | `8796f1ac57cb903e28ca34a7c08cc280299e87d908f19867d1834dc136f7f683` | `5f697f85ffb1684670d64015b51eab8b06204dd9feff3af9f12a56f7e3141f28` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-matter-create-evidence-1280.png` | `2b2e780577e2a69067ebecb82841b76a04b4c1427b68db96fa1ee9555ccfeaf3` | `170f470585509fd9767194945cb1ec754137e44dc32e688872fd81a73e2e169f` | No |
| Changed | `web-prototype/qa/screenshots/alignment-number-canonical-restored-1280.png` | `f7f5d74d02d31be861feedc3d4a8d170019676e44e87012194ca7340e91c0866` | `8cea933e994b7f3347e77c029bacd02121e03d0f1148e899587b5b08498ea338` | No |
| Changed | `web-prototype/qa/screenshots/alignment-number-canonical-stopped-1280.png` | `21d4b8ad01d8a66eab7e0eeaf9b0afcd870c3d9ced38a67ea067f56301353102` | `61a819df105183b9ebe0836a79fad692aa12665fb01fa1bdefbced2908af2953` | No |
| Changed | `web-prototype/qa/screenshots/alignment-number-disabled-1280.png` | `21d4b8ad01d8a66eab7e0eeaf9b0afcd870c3d9ced38a67ea067f56301353102` | `61a819df105183b9ebe0836a79fad692aa12665fb01fa1bdefbced2908af2953` | No |
| Changed | `web-prototype/qa/screenshots/alignment-number-landline-canonical-restored-1280.png` | `18525323ea7652e6c08ec3dfbe0a65c2f127e55fc2925410ab8594416bdaf0cb` | `94fcc913e8a975d5f1c3b29a075e57ca6c6b1cfdbadf7a78da3b60e7c6751449` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-number-landline-canonical-stopped-1280.png` | `274068db56b4ae9064a9170fb1a6b92d377c27eecafd6b2eddb85bd5fcbe64a0` | `a5222316ee773e413fc4db0afff970fbb8f1fe8581111599095dc6b742ea4c2b` | Yes |
| Changed | `web-prototype/qa/screenshots/alignment-share-create-1440.png` | `de60ab8ddca0c4f3da020342efd07ced880a5d49460bcee37770abe2afbee976` | `a2eb65be0b2a015eb2d5df4dd35332f7e9298b4cfa7b22f6276691f3d88846c1` | No |

Visual inspection covered **all four added administrator-revocation images plus nine changed images: 13 of 23 total**. The checks covered nonblank output, framing, new overlap/clipping in authorization main content, revoked status, the canonical transition ID, preserved consent/reconfirmation history, and fixed-line status/revision consistency. No new defect was found within those inspected regions. The other ten changed images were not visually inspected this round and are not claimed as visually accepted.

The existing 1024px export-table image shows the left portion of a horizontally scrollable table; the 1440px companion shows the full actions. Some older lifecycle/personal images retain a transient toast; canonical snapshots and main-content assertions are the behavior evidence. The cross-support screenshot now uses the enterprise workspace because the new administrator flows precede support checks; its support content remains correct. Runner-generated timestamps/IDs can refresh other images without establishing full visual acceptance.

### Round-3 residual risks and ownership

| Classification | Remaining item | Owner / destination |
| --- | --- | --- |
| Current gate follow-up | The remaining C-02 fix is implemented and awaits independent re-review; no finding acceptance or closure is claimed. | Independent reviewer / controller |
| Separate pre-existing Web UI issue | The enterprise number-card display branch remains outside the accepted authorization-revocation scope. | Controller / later Web UI work unit |
| Runtime and persistence boundary | Canonical mutation, atomicity, revision, and replay semantics remain in-memory prototype behavior, without durable transactions, concurrent reservations, or distributed idempotency. | Backend / data / Operations integration work units |
| Identity and carrier boundary | Mobile/fixed-line identity, SMS, WeChat, caller challenge, claim-risk, and carrier states remain deterministic fixtures. | Identity / security / carrier / IVR work units |
| Payment and fulfillment boundary | Payment verification, reconciliation, transactional issuance, scheduling, refund/renewal/stacking, and approved production parameters remain outside this fix. | Payment / backend / product / finance |
| Build and release boundary | Review hooks must be disabled before production reuse; portable CI/browser and real integration verification remain later work. | Build / release |
| Visual evidence limits | Ten changed images were not inspected this round; partial horizontal-table evidence, transient toasts, and previously classified reopened-IVR image limits remain. | UI / visual acceptance owner; use canonical/action assertions for behavior |
| External gate dependency | No Git remote is configured; re-review, accepted-finding disposition, stage/commit, push, draft PR, remote review, and later gates remain outside this handoff. | Controller / repository maintainer |

This round ends at implementation handoff, awaiting independent re-review.
