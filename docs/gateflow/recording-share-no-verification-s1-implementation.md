# S1 implementation — recording share without verification

- Gate / slice: implementation / S1; baseline: accepted plan `9fc715f`.
- Changed source: `web-prototype/evidence-hub.html`, generated `preview.html`, `qa/qa.js`, `README.md`, three new QA screenshots.
- Behavior: added no-verification selection and risk copy; creation now stores a per-share snapshot; created link previews the same record; recipient access resolves exact IDs and gates lifecycle before no-verification or saved-code admission; access count is consumed once per opening; stored content/download settings drive recipient output; management omits code rotation for no-verification records.
- Invariants: extraction code remains default; existing code shares never consult mutable form state; public access requires no platform session but never exposes an original address; revoked/expired/exhausted records cannot newly enter playback; only permitted calls can create a share.
- Error handling: invalid IDs fail closed; invalid custom expiration is rejected; unsupported SMS/member verification is labelled as outside the static recipient simulation.
- Validation: full Web QA passed; generated sections check passed; `git diff --check` passed. New creation/success/recipient screenshots were visually inspected at 1440/1024/720 with no clipping or overlap.
- Docs: README updated with behavior and static-prototype limitation.
- Residual risks: R1–R3 from control remain assigned. Existing history screenshots are unrelated dirty files and are excluded from this slice commit.
- Completion: implementation complete; next gate `code review`.
