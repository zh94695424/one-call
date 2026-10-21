# Account identity and workspaces S2 - fix and re-review

## Gate / findings

- Work unit: `account-identity-workspaces`; gates: fix / re-review.
- Source review: `docs/reviews/code-review-20261021-103742.md`.
- S2-01 accepted, final status `已修复`: logout and account/workspace switch restore all scoped UI fields from the initial state, remove dynamic drafts, stop audio and clear transient dialogs. Sensitive phone/login inputs and business-review selections clear on logout. Persisted export tasks use explicit account/workspace ownership at list and action entry points.
- S2-02 accepted, final status `已修复`: non-fixture organizations show no Huaqi numbers in settings, counts or resolver. Ordinary-member number branch no longer overwrites administrator cards when no numbers have been added.

## Validation / decision

- Added real-browser regression: verify a number, create a personal export, logout/login, check verification reset, preserve original task in the original account/space, switch space and verify task absence, upload a private filename and submit AI correction, expire session and authenticate as another person, verify clean import/AI/matter/export state.
- Reauthentication UI cleanup leaves the full canonical business snapshot unchanged. Same-account evidence detail recovery still succeeds.
- Added cloud-trade organization settings empty-state and administrator landline-list assertions.
- Final Web QA passed; shared 31/31, generator check, cross-channel QA and whitespace checks passed. The final additional number-branch correction is covered by the subsequent full Web QA pass.
- Generated Web preview matches the source; no edits to generated business sections.
- Docs decision: explain cleanup and export ownership in account rules; no business schema change.
- Decision: both accepted findings closed; no blocking questions. Ready for accepted S2 commit.

## Residual risks

- R1 real identity/session security, R2 production resource ownership and R3 backend authorization retain the assigned production work units in the control file. No new residual risk is introduced by these fixes.
