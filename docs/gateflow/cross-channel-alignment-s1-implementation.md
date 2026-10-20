# Cross-channel alignment S1 implementation

## Gate / scope / status

- Work unit: `cross-channel-alignment`.
- Gate: `implementation`, slice `S1 — A single executable demo contract`.
- Date: 2026-10-20.
- Accepted plan: `c2cefc0bf1cf2eee63940c046fd36fa2dc782746`.
- Artifact: `docs/gateflow/cross-channel-alignment-s1-implementation.md`.
- Goal mapping: G1, G3, G4, G5 and G6.
- Implementation status: **complete**. The user explicitly authorized the narrowly evidenced Web export baseline repair after the first full validation exposed it; all required S1 validation now passes.

This slice did not migrate employee/admin UI, catalog UI, miniapp/Web business pages, copy or workflows. Apart from the explicitly authorized export-ID/expiry baseline repair, the only HTML-source change is the generated shared-module section bounded by `ONECALL_BUSINESS` markers. S2 and S3 remain unstarted.

## Changed files

New shared implementation and tests:

- `shared/onecall-business.js`
- `shared/build-prototypes.js`
- `shared/qa/business.test.js`
- `shared/qa/runner-exit.test.js`
- `shared/README.md`

Current documentation:

- `docs/10-cross-channel-rules.md`
- `docs/README.md`
- `docs/09-business-activation-and-lean-admin.md`
- this artifact

Bounded generated packaging and previews:

- `operations-prototype/onecall-operations-admin.html`
- `operations-prototype/preview.html`
- `prototype/onecall-miniapp-pages.html`
- `prototype/preview.html`
- `web-prototype/evidence-hub.html`
- `web-prototype/preview.html`

QA runner integration:

- `operations-prototype/qa/run.ps1`
- `prototype/qa/run.ps1`
- `web-prototype/qa/qa.js`
- `web-prototype/qa/run.ps1`

`docs/gateflow/cross-channel-alignment-control.md` was already modified by the root controller before this implementation agent started and is not owned by this artifact. Browser QA rewrote five existing screenshot files; those exact generated changes were restored to HEAD because S1 has no UI change and screenshots are excluded from this slice.

## Implementation decisions and data transitions

### Canonical contract

`shared/onecall-business.js` is a dependency-free UMD module. It exposes exactly the approved catalog/configuration data and narrow helpers: `schemaVersion`, `labels`, `support`, `demoCatalog`, `createDemoState`, `validateCatalogVersion`, `visibleOffers`, `createOrder`, `applyPaymentResult`, `issueEntitlements`, `activateService`, `grantTrialOnce`, `claimSubject`, `startIvrSession`, `confirmIvrSession`, `consumeTrial`, `trialStatus`, `snapshot` and `loadReviewSnapshot`.

The catalog contains the 19 approved demo SKUs. All purchase channels are MINIAPP/WEB; enterprise rows are quote-only; every row is `DEMO_NOT_APPROVED`. Support is fixed to `OFFLINE`, `configured=false`, `contactValue=null`, `hours=null` and shared non-SLA instructions.

### IVR, trial, claim and evidence

- `startIvrSession` only consumes named, masked fixtures.
- `confirmIvrSession` requires trusted caller, current versioned consent and key `1`; it freezes the same stable ID into `subjectId` and `confirmedSubjectId`, internally activates once and grants `FIRST-TRIAL` at most once.
- `consumeTrial` has no subject input. It first proves one confirmed session, equal frozen subject links, exactly one referenced subject and valid existing evidence references. Only then does it find the trial grant, bound recorded seconds to the remainder and append metadata with `identityAtCapture`.
- Invalid or tampered calls do not change `usedSeconds`, evidence, sequence or `processedEvents`. Replayed events return the original result.
- `claimSubject` verifies the matching subject and number-type method, rejects conflict/recycled risk, adds only the account association/claim and preserves IVR sessions, evidence, identity-at-capture and trial snapshots.

### Catalog, order and entitlement

- Catalog validation enforces approved enums/ranges, no IVR channel, at most one ACTIVE and one DRAFT version per SKU in imported state, and no individual enterprise-seat fields.
- `createOrder` requires an account-linked verified subject, explicit consent, MINIAPP/WEB source and the exact still-sellable version/fingerprint. The order deep-copies its product snapshot and blocks a second checkout while an UNKNOWN order exists.
- `applyPaymentResult` and `issueEntitlements` are independent. PAID produces PENDING; only a separate issuance creates one entitlement. UNKNOWN/CLOSED/UNPAID cannot issue. Replays are no-ops.

### Snapshot boundary

Only the 13 approved top-level business fields are accepted. Complete import validation covers enums, numeric bounds, unique IDs, subject/grant/session/evidence/activation/order/entitlement references, IVR session-to-activation compatibility and order-to-subject/entitlement compatibility. Dynamic IDs are accepted. Invalid snapshots and representative employee, credential, customer-auth and UI/widget fields reject atomically without replacing any current business state.

### Packaging and runner behavior

`shared/build-prototypes.js` copies the canonical module into one marker pair per source, escapes `</script`, records the module SHA-256, writes only on difference and provides a non-writing `--check` mode. All three runners invoke it before their original preview generation. The Web runner now accepts an optional QA-only script path, captures `$LASTEXITCODE` immediately and throws on nonzero; the default remains `web-prototype/qa/qa.js`.

### Authorized Web baseline repair

The first truthful Web runner execution exposed two coupled fixture defects. A newly submitted task used `exportJobs.length + 1`, which generated seeded duplicate `EXP-261019-004`; its completion lookup then selected the older record. Once that was made unique, the completion handler overwrote the new task's already-computed 24-hour expiry with fixed `2026-10-20 12:00`, immediately rendering it expired after that time.

Under the user's explicit narrow authorization, `nextExportJobId()` now inspects existing IDs under the fixed `EXP-261019-` prefix, takes the greatest numeric suffix, advances it, and retains a defensive collision check over the finite in-memory fixture set. Noncontiguous or seeded IDs therefore generate a deterministic unused value (`005` for the current fixtures). Completion preserves the 24-hour expiry calculated at submission. The regression captures the new ID, asserts every visible job ID is unique, completes that exact ID, verifies its row becomes `可下载`, and downloads that exact ID. No other export state, copy, route or assertion changed.

## Validation record

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` | PASS, exit 0 |
| `node --check shared/build-prototypes.js` | PASS, exit 0 |
| `node --check shared/qa/business.test.js` | PASS, exit 0 |
| `node --check shared/qa/runner-exit.test.js` | PASS, exit 0 |
| `node --check web-prototype/qa/qa.js` | PASS, exit 0 |
| `node --test shared/qa/business.test.js` | PASS, 10/10 tests |
| `node --test shared/qa/runner-exit.test.js` | PASS, 1/1; expected child exit 23 was propagated as nonzero and the OS-temp fixture was removed |
| `node shared/build-prototypes.js --check` | PASS; all three source sections current |
| `powershell -NoProfile -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1` | PASS; existing 13-route/IVR/catalog/role/responsive suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File prototype/qa/run.ps1` | PASS; existing 57-page/navigation/320px suite |
| `powershell -NoProfile -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | PASS; login/session, interaction, role authorization, content and responsive checks, including unique new export ID and exact-target completion/download |
| `git diff --check` | PASS, exit 0; only line-ending conversion warnings |

The shared tests cover canonical SKUs and support nulls; no-confirm/no-caller rejection; stable subject and one grant across repeat calls/rule change/claim; unverified evidence and frozen identity; conflict/recycled rejection; 20-second bounded exhaustion; A-session/B-subject, unresolved, removed/replaced/duplicated subject tampering; exact grant/evidence/sequence/event-map non-mutation; dynamic session/evidence round-trip; eight forbidden snapshot-key categories; client identity gate; exact order snapshot; UNKNOWN; payment/issuance separation and replay; and equal generated hashes in all sources/previews.

No S1 screenshots require visual review because no layout, styling or visible copy changed. Operations and miniapp full browser suites loaded the embedded module without page/CSP regression; Web completed its full suite after the authorized baseline fix. The final validation regenerated seven existing screenshots; all seven files were restored to HEAD because the fix does not require visual-baseline changes.

## Documentation decision

`docs/10-cross-channel-rules.md` is the compact current reference for state independence, IVR/trial/claim semantics, demo catalog, offline support and local validation. `docs/README.md` links it. `docs/09-business-activation-and-lean-admin.md` contains only a current-alignment note and preserves the prior work unit's historical implementation evidence. Individual prototype READMEs and docs06/07/08 remain untouched because their UI status changes only in S2/S3.

## Findings and residual risks

| Classification | Item | Owner / destination |
| --- | --- | --- |
| fixed in current slice | Canonical contract, bounded `file://`/srcdoc-safe packaging, drift detection, atomic snapshot rejection and Web runner exit propagation | S1 implementation |
| fixed in current slice | User-authorized Web baseline repair: deterministic unique export IDs, preserved dynamic expiry and exact-target QA regression | S1 implementation |
| covered by later approved slice | Current UIs do not yet consume the shared state and still contain old carrier/current-price/support semantics | S2 admin migration and S3 miniapp/Web/IVR migration |
| assigned to later work unit | Real trusted caller, persistence, concurrent quota reservation, hangup, replay protection, claim risk adjudication, payment verification, fulfillment and employee authentication | Production backend/security/payment/IVR work units |
| requiring explicit user decision | Production prices/rights, renewal and stacking rules, support contact/hours, refund eligibility and SLA | Product, finance and operations; null/demo values must block production release |
| existing external-state blocker | Repository has no Git remote; push/PR gates cannot be claimed | Repository maintainer at later external gate |

No unclassified residual risk or failing required validation remains.

## Completion / next entry

S1 implementation is complete and its completion signal is met: the executable contract, generated packaging, failure-propagating runner, authorized baseline repair and all required validation pass. The next Gateflow entry is S1 `code review`, owned by the root controller. This artifact does not enter S2/S3, commit, review, push or modify the controller document.
