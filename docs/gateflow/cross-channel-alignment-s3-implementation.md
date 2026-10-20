# Cross-channel alignment — S3 implementation

- Work unit: `cross-channel-alignment`; slice: S3; gate: implementation only.
- Date: 2026-10-20; accepted S1 `8fe4c3f`, accepted S2 baseline `4cef1d8`.
- Goal mapping: G1/G3/G4/G5/G6; accepted plan §§3/6/7/8, with final cross-surface fixture handoff after client/IVR implementation and all three browser suites.
- Result: implementation and required local validation complete, ready for root-controlled code review. This artifact is not review approval, slice acceptance or final closeout.
- Ownership: no control_doc edit, stage, commit, push, PR, review or deepreview was performed by this agent. The controller-owned dirty `docs/gateflow/cross-channel-alignment-control.md` remains outside this manifest.

## Goal-to-outcome mapping

| Goal | Implemented outcome / evidence |
| --- | --- |
| G1 | Miniapp and Web consume the same generated `OnecallBusiness` catalog, trial, labels and support configuration as admin. The dynamic handoff verifies the same published version, exact cents, rights, validity, channels and version token on both clients. |
| G3 | Twelve IVR scenarios; trusted caller and versioned key-1 consent; stable original grant/session/evidence across safe account claim; identity remains UNVERIFIED. Fixed lines use caller challenge. Conflict/recycled-number cases reveal no history. |
| G4 | Ordinary client activation is an internal service operation. Both clients create MINIAPP/WEB orders with frozen offer tokens and snapshots; UNKNOWN, PAID/PENDING, PAID/FAILED and ISSUED remain separate. IVR only records GUIDED and creates no order/payment. |
| G5 | Shared, unconfigured offline support covers consultation, claim, refund and invoice. No invented phone/email, online submission, fake receipt or processing promise remains on the touched support/finance flows. |
| G6 | Shared tests, runner propagation regression, all three browser QA suites, required responsive widths and dynamic cross-surface handoff pass. Key new views were opened with `view_image` for human visual inspection. These checks prove local fixture semantics, not real integration. |

## Implemented behavior and decisions

Miniapp now has 61 indexed views, adding `ivrReturn`, `claimVerify`, `claimResult` and `claimBlocked`. Returning users verify the same number and claim the original subject without a retroactive activation or trial grant. A successful claim appends account association only; it leaves the original trial amount, usage, rule version, expiry, session subject links, evidence subject and identity-at-capture unchanged. Claim result exposes metadata only, with no original IVR audio player/download/export/share. Current SMS success cannot override conflict or recycled-number risk. An unverified claimed subject offers the separately labelled identity step before ordinary purchase.

Both client surfaces use the demo account `ACCOUNT-DEMO-001`, while their regular seed subjects remain `MINI-ACCOUNT-001` / `WEB-ACCOUNT-001`. Purchase selection prefers a linked IVR subject even while unverified, so it cannot silently purchase through an unrelated verified seed. Current order lists are filtered to the current account. Scenario selection is read-only and does not itself grant, claim or reveal historical evidence.

Miniapp trial displays derive from the actual grant. Published offer fields drive plans, product details, enterprise consultation and checkout, including channels and exact two-decimal prices. Entering purchase confirmation freezes the selected offer and token. Loading a newer review snapshot while confirmation is open does not silently reprice that checkout; stale, off-sale or channel-removed selections reject without an order and direct reselection. Existing order/payment state remains stable through review import. Payment result only displays success for PAID, distinguishes FAILED issuance, and never treats payment as entitlement issuance.

Web supports ordinary identity/number verification/explicit consent and synchronous internal service activation entirely within Web; activation records come from the shared helper. It shows the seven subscription/one-time offers plus shared add-ons; enterprise QUOTE remains consultation-only. Returning IVR success, unverified, conflict and recycled cases use shared subject/trial/evidence state. Selection freezes the business review offer, and review imports preserve both the pending selected snapshot and an existing order. Old online-invoice routes resolve to offline guidance, and the touched carrier-receipt generation / online invoice submission paths are removed. Offline support is also available to the preserved enterprise roles. Existing evidence, workspace, share/export, matters, AI, certificate and member workflows retain regression coverage.

The admin IVR demo now includes `new`, `fixed`, `repeat`, `timeout`, `wrong`, `missing`, `suspended`, `disabled`, `expired`, `exhausted`, `conflict` and `recycled`. Consent explicitly describes recording and versioned notice; key 1 confirms. IVR does not require prior registration or verified identity. The recording event uses stable `OPS-REC-${sessionId}`, so replay returns the first result. The exhausted fixture has 20 seconds remaining: requesting 60 records and charges 20, then reports exhaustion. Wrong key, timeout, missing trusted caller, suspended/disabled/expired paths do not create unexpected facts. Guidance records GUIDED only; a fixed line receives WeChat-search/caller-challenge guidance rather than a fictional SMS success.

The local-only `__onecallOps`, `__onecallMini` and `__onecallWeb` review hooks transfer only canonical business snapshots. Admin import still requires its actual demo employee login. The cross test rejects employee records/login/credentials/roles, customer-auth state, UI state and code one category at a time, atomically. Dynamic IVR sessions remain required business context and round-trip with their original evidence. No shared executable contract, generator, existing shared unit test, QA runner, dependency or external service change was needed.

## Files

The exact non-image S3 file manifest is:

```text
docs/06-miniapp-ui-spec.md
docs/07-web-capability-hub-spec.md
docs/09-business-activation-and-lean-admin.md
docs/10-cross-channel-rules.md
docs/README.md
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
shared/qa/cross-channel.js
web-prototype/README.md
web-prototype/evidence-hub.html
web-prototype/preview.html
web-prototype/qa/qa.js
```

The three `preview.html` files are runner-generated, not hand-edited. Current documentation in 06/07/09/10 and the index points to the new local semantics while retaining completed historical work-unit records. Historical screenshot families remain intact. The only modified tracked images are `alignment-login-1440.png` and `alignment-user-trial-1440.png`; all newly created images also use `alignment-*`. The complete image manifest is at the end of this artifact.

## Validation evidence

All final commands/checks below exited 0 on 2026-10-20. Browser suites run with the repository's installed Playwright and Microsoft Edge; no real network/telephony/payment interaction is involved.

| Command / check | Result |
| --- | --- |
| `node --test shared/qa/business.test.js` | 21/21 pass, including immutable IVR references, atomic import, event replay, payment/issuance and generated hash checks |
| `node --test shared/qa/runner-exit.test.js` | 1/1 pass; existing Web runner propagates a controlled child failure |
| `node --check shared/onecall-business.js` | Pass |
| `node --check shared/build-prototypes.js` | Pass |
| `node shared/build-prototypes.js --check` | All business sections current |
| `node --check operations-prototype/qa/qa.js` | Pass |
| `node --check prototype/qa/qa.js` | Pass |
| `node --check web-prototype/qa/qa.js` | Pass |
| `node --check shared/qa/cross-channel.js` | Pass |
| Parse all inline scripts in each editable HTML source with `vm.Script` | Two scripts per source parsed successfully |
| `powershell -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1` | Complete S3 pass: S2 employee/catalog/session regressions, all twelve IVR scenarios, stable confirm/consume replay, bounded remaining use and 1440/1280/1024/720 layouts |
| `powershell -ExecutionPolicy Bypass -File prototype/qa/run.ps1` | Complete pass: 61-page navigation, ordinary activation, landline/caller and claim branches, catalog checkout/payment/support, no page errors; all indexed pages at 320 and touched S3 views at 393 |
| `powershell -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1` | Complete pass: login/roles/workspaces, evidence/share/export/matters/AI/certificates/members, internal activation, catalog/order/claim/support and 720/1024/1280/1440 layouts |
| `node shared/qa/cross-channel.js` | Final rerun after all three complete browser suites passed; dynamic publication → both clients → IVR claim → separate WEB and MINIAPP purchase/issuance → all surfaces |
| `git diff --check` | Pass; Git emits only existing LF/CRLF conversion notices |

The final cross-channel run used an actual admin demo login (`admin.lin`) to publish BASIC monthly V2 at ¥41.25 with 83 recording minutes, 77 AI minutes and 45 days, and trial v2 at 120 seconds / 7 days. Both clients visibly matched those fields, channel labels and token; historical orders stayed unchanged. Changed-token, off-sale and channel-removal imports retained frozen checkout price/rights, rejected checkout and created no new order.

Admin then dynamically created an unregistered UNVERIFIED IVR caller, confirmed, recorded 60 seconds and set GUIDED without a purchase. Miniapp claim added account association, kept the same subject, 60/120 trial and original expiry, preserved every pre-existing session/evidence/grant object, and replayed claim without mutation. Web received the same claimed state; separately labelled identity verification did not rewrite identityAtCapture. WEB payment moved through UNKNOWN and PAID/PENDING; admin displayed paid but not issued. FAILED was visibly distinct in both clients; independent issue added exactly one entitlement. MINIAPP repeated its own purchase path; IVR never became an order channel. Historical order snapshots and original recording facts survived the final round trip.

Every processed event was replayed using its original input fingerprint and action identifiers; the entire business state stayed equal. Five invalid IVR-reference cases—cross-subject confirmed ID, unresolved links, removed subject, replaced ID and duplicated subject—were rejected atomically through every hook and by `consumeTrial` with both existing/new event IDs, leaving grants, evidence, sequence and processedEvents unchanged. The eight forbidden top-level categories were rejected individually by all three hooks. Fixed-line claim uses CALLER_CHALLENGE and preserves capture facts. Conflict/recycled fixtures expose no history. Shared support equality, no fake contacts/forms/receipts, equal generated module hashes and zero page/console/CSP errors were asserted.

## Human visual inspection

The implementation agent opened the following exact files with `view_image`. These are local mutated QA fixtures, not approved pricing or evidence.

| Screenshot | Observation |
| --- | --- |
| `prototype/qa/screenshots/alignment-cross-returning-393.png` | Original subject, 60/120-second v2 trial, expiry and metadata card fit; current/capture identity remain unverified; continuation action visible |
| `prototype/qa/screenshots/alignment-cross-payment-pending-393.png` | Payment success and pending entitlement are separately legible; frozen V2 and independent issue control are visible |
| `prototype/qa/screenshots/alignment-cross-conflict-393.png` | Conflict requires offline verification, shows no recording/trial/order history and keeps support action visible |
| `prototype/qa/screenshots/alignment-cross-recycled-393.png` | Recycled-number risk is distinct, blocked, and exposes no history |
| `prototype/qa/screenshots/alignment-cross-support-393.png` | Unconfigured contact and all consultation/refund/invoice/claim instructions fit within the phone without clipped text or fake contact action |
| `web-prototype/qa/screenshots/alignment-cross-returning-1280.png` | Unverified subject/trial and preserved metadata are clearly separated; internal activation is labelled and identity continuation visible |
| `web-prototype/qa/screenshots/alignment-cross-payment-pending-1280.png` | Exact ¥41.25 and frozen 83/77-minute, 45-day V2 snapshot; PAID/PENDING and independent issue action are clear |
| `web-prototype/qa/screenshots/alignment-cross-conflict-1280.png` | Explicit conflict explanation, offline direction and no historical records |
| `web-prototype/qa/screenshots/alignment-cross-recycled-1280.png` | Explicit recycled-number explanation, offline direction and no historical records |
| `web-prototype/qa/screenshots/alignment-cross-support-1280.png` | Consistent four support instructions and unconfigured contact; readable rows, no submission form |
| `operations-prototype/qa/screenshots/alignment-cross-payment-pending-1440.png` | Read-only WEB order shows PAID/PENDING and exact published purchase snapshot; lower support notice is readable |
| `operations-prototype/qa/screenshots/alignment-cross-support-1440.png` | Same contact placeholder/instructions as clients; no online progress or receipt |
| `operations-prototype/qa/screenshots/alignment-ivr-exhausted-1440.png` | Modal explains 20-second bounded recording, exhaustion and idempotent replay; consent and no-order guidance remain readable |

No overlapping/clipped controls or unreadable content were found in these views. Miniapp full-page captures include its existing index above the embedded phone; that is the prototype viewer wrapper. Automated overflow assertions cover the required narrow widths in addition to the inspected screenshots.

## Findings handled and residual risks

Implementation validation exposed stale checkout imports silently replacing selection, unverified claimed-subject purchase falling back to an unrelated verified seed, one-decimal price rounding, and Web's old 1024px minimum layout. These were corrected in this slice; final complete suites and dynamic handoff include the corrected paths. Stable recording-event identity prevents repeated consumption from creating extra evidence. No failing assertion was waived as a prototype limitation.

| Classification | Residual item / boundary | Owner / destination |
| --- | --- | --- |
| fixed in current slice | Divergent client catalog/trial/support, missing safe IVR return presentation, implicit paid=issued, carrier-only activation, fake online invoice/support flow, stale reprice/identity fallback | S3 implementation / root code review and subsequent gate artifacts |
| assigned to later work unit | Real employee/customer authentication, SMS and caller trust, durable claim proof, recycled-number adjudication and historical original-recording access policy | Backend/security/product / production identity and claim service |
| assigned to later work unit | Persistent uniqueness, concurrent trial reservations, measured recording duration, real hangup, replay protection and immutable evidence storage | IVR/backend / production IVR integration |
| assigned to later work unit | Payment verification, late callbacks/reconciliation, real independent fulfillment, transactionality and recurring scheduler | Payment/backend / production commerce integration |
| requiring explicit user decision | Production prices/rights/enterprise specifications, renewal/add-on stacking, debit order, auto-renewal, refund eligibility/SLA | Product/finance / docs10 production decisions; current offers remain DEMO_NOT_APPROVED |
| requiring explicit user decision | Actual support channel/contact/hours | Operations / shared support configuration; configured=false is explicit and blocks a production release |
| assigned to later work unit | Actual offline ticket/refund/invoice receipt and processing | Operations/finance / offline SOP; current prototype claims no submission |
| assigned to later work unit | Remove/disable local review hooks before any production reuse; portable/CI browser runtime configuration | Build/release / production release or CI setup; existing repository QA uses this installed Windows runtime |
| assigned to later work unit | Unrelated evidence/legal/certificate/organization authorization changes beyond touched flows | Corresponding product/engineering owner / separately scoped work; existing regression suite retained |
| existing external-state blocker | No Git remote, so push/draft PR/remote review/final closeout cannot be claimed | Repository maintainer / root external gates after local review/acceptance |

No local S3 blocker remains. No later review gate or acceptance decision was executed by this agent.

## Complete S3 image manifest

105 alignment images (103 new and 2 refreshed tracked images); historical non-alignment screenshots were retained:

```text
operations-prototype/qa/screenshots/alignment-cross-payment-pending-1440.png
operations-prototype/qa/screenshots/alignment-cross-support-1440.png
operations-prototype/qa/screenshots/alignment-ivr-exhausted-1440.png
operations-prototype/qa/screenshots/alignment-login-1440.png
operations-prototype/qa/screenshots/alignment-user-trial-1440.png
prototype/qa/screenshots/alignment-claim-conflict-320.png
prototype/qa/screenshots/alignment-claim-exhausted-320.png
prototype/qa/screenshots/alignment-claim-expired-320.png
prototype/qa/screenshots/alignment-claim-recycled-320.png
prototype/qa/screenshots/alignment-claim-returning-320.png
prototype/qa/screenshots/alignment-claim-suspended-320.png
prototype/qa/screenshots/alignment-cross-conflict-393.png
prototype/qa/screenshots/alignment-cross-payment-pending-393.png
prototype/qa/screenshots/alignment-cross-recycled-393.png
prototype/qa/screenshots/alignment-cross-returning-393.png
prototype/qa/screenshots/alignment-cross-support-393.png
prototype/qa/screenshots/alignment-dial-handoff-320.png
prototype/qa/screenshots/alignment-enterprise-plans-320.png
prototype/qa/screenshots/alignment-evidence-320.png
prototype/qa/screenshots/alignment-free-evidence-limit-320.png
prototype/qa/screenshots/alignment-free-evidence-result-320.png
prototype/qa/screenshots/alignment-free-evidence-result-actions-320.png
prototype/qa/screenshots/alignment-free-toolbox-320.png
prototype/qa/screenshots/alignment-landline-call-320.png
prototype/qa/screenshots/alignment-landline-call-bottom-320.png
prototype/qa/screenshots/alignment-mixed-numbers-320.png
prototype/qa/screenshots/alignment-phone-methods-320.png
prototype/qa/screenshots/alignment-phone-sms-authorization-320.png
prototype/qa/screenshots/alignment-phone-sms-authorization-bottom-320.png
prototype/qa/screenshots/alignment-phone-wechat-authorization-320.png
prototype/qa/screenshots/alignment-phone-wechat-authorization-bottom-320.png
prototype/qa/screenshots/alignment-plans-320.png
prototype/qa/screenshots/alignment-profile-760.png
prototype/qa/screenshots/alignment-support-393.png
prototype/qa/screenshots/alignment-trial-entry-320.png
prototype/qa/screenshots/alignment-trial-home-320.png
prototype/qa/screenshots/alignment-trial-limit-320.png
web-prototype/qa/screenshots/alignment-auditor-export-partial-1280.png
web-prototype/qa/screenshots/alignment-authorization-reconfirmed-1280.png
web-prototype/qa/screenshots/alignment-authorization-revoked-detail-1280.png
web-prototype/qa/screenshots/alignment-call-ai-version-1024.png
web-prototype/qa/screenshots/alignment-call-detail-1440.png
web-prototype/qa/screenshots/alignment-certificate-new-review-1280.png
web-prototype/qa/screenshots/alignment-certificate-supplement-submitted-1024.png
web-prototype/qa/screenshots/alignment-claim-conflict-1024.png
web-prototype/qa/screenshots/alignment-claim-recycled-1024.png
web-prototype/qa/screenshots/alignment-claim-returning-1024.png
web-prototype/qa/screenshots/alignment-cross-conflict-1280.png
web-prototype/qa/screenshots/alignment-cross-payment-pending-1280.png
web-prototype/qa/screenshots/alignment-cross-recycled-1280.png
web-prototype/qa/screenshots/alignment-cross-returning-1280.png
web-prototype/qa/screenshots/alignment-cross-support-1280.png
web-prototype/qa/screenshots/alignment-enterprise-access-1280.png
web-prototype/qa/screenshots/alignment-enterprise-auditor-overview-1440.png
web-prototype/qa/screenshots/alignment-enterprise-auditor-records-1280.png
web-prototype/qa/screenshots/alignment-enterprise-consultation-1440.png
web-prototype/qa/screenshots/alignment-enterprise-evidence-manager-list-1280.png
web-prototype/qa/screenshots/alignment-enterprise-evidence-manager-overview-1440.png
web-prototype/qa/screenshots/alignment-enterprise-member-change-audit-1024.png
web-prototype/qa/screenshots/alignment-enterprise-member-detail-1280.png
web-prototype/qa/screenshots/alignment-enterprise-member-evidence-1280.png
web-prototype/qa/screenshots/alignment-enterprise-member-overview-1024.png
web-prototype/qa/screenshots/alignment-enterprise-member-overview-1440.png
web-prototype/qa/screenshots/alignment-enterprise-members-1280.png
web-prototype/qa/screenshots/alignment-enterprise-order-detail-1280.png
web-prototype/qa/screenshots/alignment-enterprise-organization-1280.png
web-prototype/qa/screenshots/alignment-enterprise-overview-1440.png
web-prototype/qa/screenshots/alignment-evidence-chat-screenshot-1280.png
web-prototype/qa/screenshots/alignment-evidence-detail-1280.png
web-prototype/qa/screenshots/alignment-evidence-import-review-1024.png
web-prototype/qa/screenshots/alignment-evidence-import-review-1280.png
web-prototype/qa/screenshots/alignment-export-tasks-1024.png
web-prototype/qa/screenshots/alignment-export-tasks-1440.png
web-prototype/qa/screenshots/alignment-login-enterprise-invite-1280.png
web-prototype/qa/screenshots/alignment-login-first-use-onboarding-1280.png
web-prototype/qa/screenshots/alignment-login-wechat-1024.png
web-prototype/qa/screenshots/alignment-login-wechat-1440.png
web-prototype/qa/screenshots/alignment-login-workspace-selection-1440.png
web-prototype/qa/screenshots/alignment-matter-archived-1024.png
web-prototype/qa/screenshots/alignment-matter-create-evidence-1024.png
web-prototype/qa/screenshots/alignment-matter-create-evidence-1280.png
web-prototype/qa/screenshots/alignment-matter-detail-created-1024.png
web-prototype/qa/screenshots/alignment-matter-detail-created-1280.png
web-prototype/qa/screenshots/alignment-matter-maintained-1024.png
web-prototype/qa/screenshots/alignment-notifications-unread-1024.png
web-prototype/qa/screenshots/alignment-number-add-synchronous-result-1280.png
web-prototype/qa/screenshots/alignment-number-detail-1024.png
web-prototype/qa/screenshots/alignment-number-detail-1280.png
web-prototype/qa/screenshots/alignment-number-disabled-1280.png
web-prototype/qa/screenshots/alignment-payment-pending-1280.png
web-prototype/qa/screenshots/alignment-personal-evidence-1440.png
web-prototype/qa/screenshots/alignment-personal-order-detail-1024.png
web-prototype/qa/screenshots/alignment-personal-overview-1024.png
web-prototype/qa/screenshots/alignment-personal-overview-1440.png
web-prototype/qa/screenshots/alignment-personal-plans-1024.png
web-prototype/qa/screenshots/alignment-personal-plans-1440.png
web-prototype/qa/screenshots/alignment-share-create-1024.png
web-prototype/qa/screenshots/alignment-share-create-1440.png
web-prototype/qa/screenshots/alignment-share-created-1280.png
web-prototype/qa/screenshots/alignment-share-recipient-expired-1024.png
web-prototype/qa/screenshots/alignment-share-recipient-locked-1024.png
web-prototype/qa/screenshots/alignment-share-recipient-playback-1280.png
web-prototype/qa/screenshots/alignment-share-recipient-revoked-1024.png
web-prototype/qa/screenshots/alignment-shares-management-1440.png
web-prototype/qa/screenshots/alignment-support-1024.png
```

