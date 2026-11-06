# PA15 Sharing Public Share Read - Final Closeout

## Identity And Status

- Work unit: `PA15-sharing-public-share-read`.
- Status: `final closeout pass`; one observation-free public-read slice completed.
- Accepted plan: `docs/gateflow/pa15-sharing-public-share-read-plan.md`, 399 lines, SHA256 `EED82E23743C07B71AFAE09C5183757DBAFDFC44E3B71FEA55CF67C2518A8012`.
- Accepted slice: `6d905daa33a34f22bc4ae82d6cba48931b1413d9`.
- Accepted deepreview: `3ccae89a7c58b55c9054040ed73b2b8b38975a00`.
- Accepted PR review checkpoint: `d68c1c33a480fcf802ac058705fb1c371ddc4c78`.
- Draft PR: https://github.com/zh94695424/one-call/pull/25, state OPEN, `isDraft=true`, base `main`, head branch `codex/pa15-sharing-public-share-read`.
- PR head at accepted PR-review checkpoint and final push: `d68c1c33a480fcf802ac058705fb1c371ddc4c78`.
- No issue was linked because this work unit was a planned backend phase, not a GitHub issue.

## What Changed

The slice implements exactly:

`GET /api/v1/public/shares/{capabilityToken}`

The endpoint:

- resolves the exact UTF-8 capability token to the stored SHA-256 digest;
- returns the closed `PublicShareView` without IDs, token/digest, object identifiers or unshared content;
- uses an optional customer session only for `ORGANIZATION_MEMBER` content and otherwise returns safe metadata with `verificationChallenge.customerSessionRequired=true`;
- preserves ordered `SHARE_REVOKED`, `SHARE_EXPIRED`, validity-window, `ACTIVE`/`LOCKED` and evidence-owner semantics;
- performs no admission, counter, generation, revision, ticket, Redis/media, cookie, session or `share_access_event` mutation;
- reuses the existing command/evidence transaction graph under PostgreSQL-enforced read-only operation with current-role assertions and restoration;
- handles encoded/noncanonical URI presentations with the existing canonical route gate and a closed 404 Problem rather than pre-filter HTML;
- preserves create, revoke, rotate, list and `/me` behavior.

The implementation candidate is exactly 25 paths: 11 modified and 14 new. V001-V010, OpenAPI, generated API, ACL definitions, frontend and deployment settings are unchanged.

## Verification

- Focused six-class Surefire: 24/24.
- Targeted `PublicSharesIT`: 8/8 on disposable PostgreSQL 16.15.
- Fresh clean `-Pcontainer-smoke verify`: 19 Surefire classes / 91 methods plus 7 Failsafe classes / 126 methods = 217, with zero failures, errors or skips.
- Exact source = CI = XML inventory passed, including per-class/per-method identity and conditional/duplicate-test rejection.
- The evidence bundle is retained under `.review-evidence/` and the local run directories referenced by the implementation report.
- PostgreSQL evidence proves:
  - one backend PID/connection across the grant and evidence reads;
  - `transaction_read_only=on`;
  - `sharing_command_role -> evidence_role -> sharing_command_role` restoration;
  - intentional write rejection with SQLSTATE `25006`;
  - pool reuse reset to the login/read-write state;
  - a subsequent command write succeeds;
  - byte-identical before/after digests for all 11 protected tables.
- Default/web packaged-JAR smokes passed, including nine bounded HTTP probes and encoded-slash closed-404 behavior.
- All 82 dependency-firewall probes passed.
- Remote GitHub Actions checks were observed as pending at closeout (`contract-regressions`, `java-runtime`); no remote-pass claim is made.

## Documentation And Gateflow Evidence

- Accepted plan, plan fixes, implementation report and code-review fix artifact are committed.
- Eight plan-review/re-review artifacts, six code-review/aggregate artifacts and two PR-review artifacts are committed.
- The dedicated public-read design, exact 25-path boundary, test arithmetic, runtime evidence and residual ownership are recorded in the accepted plan and implementation report.
- `project-execution-control.md` contains the authoritative gate history and next entry point.

## Finding Status

- Original code-review F-PA15-01 was rejected because the existing valid non-null throwing callback path already exercised the intended branch.
- Accepted code-review F-PA15-02 and F-PA15-03 were fixed in the three authorized existing test/fixture files and independently re-reviewed.
- Both aggregate deepreviews returned `pass-with-risks` with zero material findings.
- Both PR reviews returned `pass-with-risks` with zero material findings.
- No accepted finding remains open.

## Remaining Risks And Owners

| Risk | Owner / destination |
| --- | --- |
| Production customer-session verifier/provider integration is not implemented; tests use deterministic seams. | Customer IAM / Security; later session-verification work unit. |
| Public read reuses `sharing_command_role`/`evidence_role` inside a PostgreSQL-enforced read-only transaction; V010 has no dedicated digest-capable public-read role. | Database / Security / Sharing Backend; next independent database work unit `PA16-sharing-public-read-least-privilege-acl`. |
| Command-pool capacity, dual-role NOINHERIT provisioning, abuse controls and production load are not validated. | Backend / SRE / Database; deployment capacity and provisioning review. |
| READ COMMITTED observation is not an authorization lease; later revoke/membership/risk/media operations must recheck current state. | Backend / Security; later admission/media slices. |
| Production capability-token format, entropy, TTL and issuance policy remain outside this slice. | Security / Product / Backend; production token-policy gate. |
| Historical `docs/14` page-open/prefetch prose is not edited; the operation-specific observation-free GET contract controls. | Controller / Specification; later documentation reconciliation. |
| Remote CI checks were still pending at closeout; local evidence is not a remote-CI or production-acceptance claim. | Platform / Controller; monitor PR checks when the user marks the draft ready. |
| Local `.review-evidence` and `/tmp` logs are replaceable and are not a durable production evidence store. | Controller / Platform; preserve with the merge decision or rerun remote CI. |

## User Actions And Next Entry Point

The PR remains a draft. The user may mark it ready for review and merge it when satisfied. This work unit did not merge, approve, mark ready, request reviewers, send external comments, delete branches or mutate issues.

After the user merges PR #25, start the next ONECALL Phaseflow cycle from updated `main` with a fresh goal confirmation for the independent database work unit:

`PA16-sharing-public-read-least-privilege-acl`

Do not reuse this PA15 goal confirmation as authorization for PA16.
