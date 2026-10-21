# Recording share without verification — plan

## Goal and boundary

Binding scope: `recording-share-no-verification-control.md`. Add a selectable “免验证 · 链接直听” mode to the existing Web prototype, retaining extraction-code default and the existing sharing authority boundary. No design document supplied; user request and source are authoritative.

## S1 — create and open an independent no-verification share

- Allowed files: `web-prototype/evidence-hub.html`, generated `preview.html`, `qa/qa.js`, new QA screenshots, `README.md`, and work-unit/review artifacts.
- Prerequisite: accepted plan. Completion: the behaviors and Web regression below pass and code review is accepted.
- Add `shareAudience=none` with持链接者均可收听说明. Right-hand preview and success page explicitly say no verification; no code/copy-code/separate-delivery or code-rotation copy for that mode.
- Save a per-share snapshot on creation: unique demo ID/link, current account/workspace, permitted call, verification label, code only for code mode, content scope, download flag, expiration timestamp, visit count/limit. Set `createdShareId`; success/preview/management refer to that same record. Use existing visibility/creation permission checks; account/workspace changes reset draft state. Do not serialize into shared business snapshots.
- Opening a share clears the previous recipient session/audio. Resolve the record by exact ID; invalid ID fails closed. Inspect revoked/expired/visit-limit states before admitting playback. An allowed no-verification record enters playback immediately; code records require their own saved code (legacy fixture code defaults to 8K3P), regardless of the current form mode. Admit/count once per opening, not per render, play/pause or seek. The last admitted visit can play; reopening after the limit blocks.
- Recipient rendering respects the stored content scope and download flag. The existing simulated player remains click-to-play. Record-specific expiration/visits replace hard-coded metadata. Revocation/expiration also override an already admitted recipient on subsequent interaction/render. No original storage URL is added.
- Management shows “免验证” and allows revoke/extend; rotation only appears/executes for extraction-code records. New records remain scoped to the creating account/workspace. Existing static role fixtures stay unchanged.
- Retain existing sms/member options; do not pretend extraction-code entry validates these modes. If previewed, show that the corresponding authentication is outside this static demo.

## Validation and goal alignment

The one slice maps directly to creating, receiving and managing the requested mode. A stored record is necessary to avoid changing old shares through mutable form state; content/expiry/count checks are necessary to preserve selected restrictions. No new abstraction, service, dependency, migration or cross-channel contract is introduced.

Extend the existing Playwright share flow: retain all code/error/locked/expired/revoked cases; verify default code; create none, inspect clear warning and code-free success, open with signed-out session, play/pause without repeated counts, retain scope/download defaults, reopen/limit/revoke/expire rejection, invalid ID rejection, old code still gated with none selected, independent code rotation, and account/workspace visibility. Capture new creation/success/player screens at 1440/1024/720 as relevant.

Commands: `powershell -ExecutionPolicy Bypass -File web-prototype/qa/run.ps1`, `node shared/build-prototypes.js --check`, `git diff --check`. Inspect new screenshots. README documents how to exercise the mode and the static-demo boundary. No shared rules change, so no full cross-channel regression is required.

## Risks / delivery

Risks and owners: R1–R3 in control; no blocking open questions. Avoid scope drift into full sharing backend or all-channel share development. Report changed behavior, validation, preview location, and material limitations concisely. Review/commit locally, then record missing remote at push gate without claiming final-closeout-pass.
