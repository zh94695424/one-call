# Recipient share-link scenario — plan

## Goal and design

Scope authority is `recipient-share-link-scenario-control.md`; no separate design document exists. A recipient link must be independently addressable: it cannot depend on the sender first creating in-memory state in the same tab. The minimal solution is a stable public-demo share fixture selected by `?share=<id>`, rendered by the existing `shareAccessPage` state machine.

One responsive page remains the source for both PC and phone. Add mobile-only CSS below 520px to make the public page full width, reduce fixed padding, wrap metadata, and place player time on its own row. Do not duplicate HTML or introduce device detection. Add sender context so the page reads as an incoming share rather than an internal preview.

## S1 — directly open and listen on PC/mobile

- Allowed: `web-prototype/evidence-hub.html`, generated `preview.html`, `qa/qa.js`, two new screenshots, `README.md`, and work-unit/review artifacts.
- Add one `publicDemo` no-verification fixture with sender, recording, transcript scope, visit/expiry/download policy. Exclude it from sender management rows so it remains scenario data rather than a fabricated user-created record.
- On initial load, read only the `share` query parameter. When present, set signed-out recipient route and exact active share before the first render. Known IDs use existing lifecycle/admission; unknown IDs fail closed. Normal URLs preserve the login entry.
- Add a “接收方场景” action to share management which navigates to the stable query URL. Keep the product-style generated share link copy unchanged.
- Add sender context and device-responsive public-page/player layout; playback remains explicit click-to-play and uses existing audit/risk copy. Do not autoplay.
- QA opens a fresh signed-out page at the query URL, proves direct player/no login/no verification, sender context, single admission, play/pause and no original URL; captures 1440px PC and 393px phone screenshots and checks overflow/layout stability. Existing Web regression, build check and diff check must pass.
- README publishes the local scenario URL and static-prototype limitation.
- Completion: direct URL and management entry work; visual QA passes at PC/mobile widths; no regression in sender/code-share paths.

## Alignment and risk

Every change maps to direct-link accessibility or PC/mobile usability. Reusing the public page prevents two divergent receiver implementations; a query bootstrap is sufficient for a file-based prototype and avoids a router/service abstraction. R1–R3 remain assigned in control; no blocking question. Final report gives the direct path, validation and production limitation.
