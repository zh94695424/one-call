# PA14 Customer Share Revocation Final Closeout

- Work unit: `PA14-sharing-command-revocation`
- Type: backend sharing command feature, slice 2 of the accepted PA14 plan
- Status: `final closeout pass`
- Date: 2026-11-05, Asia/Shanghai
- Branch: `codex/pa14-sharing-command-revocation`
- Merged/base commit: `71e91f0e0fac5b2ea14976f390c1924f7825f3a5` (PR #22 merge)
- Accepted plan checkpoint: `e99dc4f18dc697e897124716b9447b13db497600`
- Accepted slice checkpoint: `c1d10250522047e9c5554b3b5aa1bfe07835df42`
- Accepted aggregate-deepreview checkpoint: `b26a6111626c6069d0748980840894b9bc11a70a`
- Accepted PR-review checkpoint: `c2bc274ac02e5fdd1fecafc251ca65360085b923`
- Draft PR: [#23 PA14 revoke S2: add customer share revocation](https://github.com/zh94695424/one-call/pull/23)

## What Changed

Implemented only:

`POST /api/v1/customer/shares/{shareId}/revocations`

The change preserves create, share-list and `/me`. It adds:

- exact customer route admission, authentication, one unchanged `Idempotency-Key`, strict closed request binding and trusted workspace handling;
- one command transaction with command-role assertion, workspace advisory locks, active caller context, owner/workspace-filtered grant locking and one database clock sample;
- deterministic action identity, request digest and fingerprint; exact persisted replay before current-state/time/witness preconditions;
- terminal `REVOKED` 410 precedence, expired/incompatible 409 behavior, optional revocation-generation witness behavior and one conditional transition;
- one redacted access event, one conditional status/generation/revision update, one `SHARE_REVOKED` Outbox fact and one `SHARE_GRANT_REVOKE` audit fact in the accepted order;
- one bounded reread for `NotUpdated`, rollback on every append/commit failure and no partial success;
- exact flat 200 state and existing sanitized Problem responses, with no secret/raw-key leakage;
- six exact test additions and in-place web/regression expansions, synchronized to a cumulative **78 Surefire + 113 Failsafe = 191** inventory.

Code rotation, public admission/ticket/media, schema/V001–V010, OpenAPI, frontend, production policy/provider/KDF/HMAC/TTL values and deployment provisioning remain unchanged.

## What Was Verified

- A fresh unified `-Pcontainer-smoke clean verify` passed with Surefire **78/78** and Failsafe **113/113**, zero failures/errors/skips, against PostgreSQL **16.15**.
- The exact CI source=CI=XML inventory block passed: `64+10+10+8+8+13=113`, combined **191**.
- Packaged default and opt-in web JAR smokes passed.
- The production-JAR dependency firewall passed all **82** adversarial probes.
- `git diff --check` passed; the implementation allowlist, protected paths, exact six added test methods, frozen plan hash, create/list/`me` regressions and rotate-404 boundary were audited.
- GitHub Actions run `42000708721` passed both required jobs on the final PR-review head `c2bc274`: `java-runtime` and `contract-regressions`.
- Local branch, remote branch and Draft PR head were verified at `c2bc274` before the controller-only closeout delivery.

## Review And Finding Status

- Independent code-review A (DeepSeek): `docs/reviews/code-review-20261105-160013.md`, zero material findings.
- Independent code-review B (MiMo 26 Flash): `docs/reviews/code-review-20261105-161638.md`, `pass-with-risks`, zero material findings.
- Aggregate Deepreview, GPT Code Primary Judge: `docs/reviews/code-review-20261105-163845.md`, `pass-with-risks`, zero material findings, no blocking question.
- Independent PR-review A (DeepSeek): `docs/reviews/pr-23-review-20261105-165631.md`, zero material findings.
- Independent PR-review B (MiMo 26 Flash): `docs/reviews/pr-23-review-20261105-171207.md`, zero material findings.

No accepted material finding remains open. Reviewer A's PR-review observation about one time-anchored Control sentence is resolved by the subsequent controller bookkeeping commit; no code or review artifact change was required.

## Residual Risks And Owners

- **Sharing/Backend architecture:** extract the canonical hash helper into a neutral internal collaborator in later bounded work; current behavior has one canonicalization source.
- **Web/Gateway:** validate ingress behavior for coalesced or whitespace-modified duplicate headers and enforce body-size/resource limits at the gateway; application-level comma splitting or re-normalization is forbidden.
- **Backend/Database/SRE:** extend testing for lock waits, deadlocks, event-collision failures, multi-replica contention and fault campaigns beyond the accepted two-connection proof.
- **Customer IAM / Organization / Sharing lifecycle:** enforce shared advisory-lock and owner-writer conformance; hostile/non-service replay writers are outside this slice.
- **Security/Product/Media/Infrastructure:** schedule credential and buffered-media recall, gateway 429 policy, approved TTL/propagation windows, production providers/provisioning and deployment verification.
- **Evidence/Legal/Retention:** retain existing evidence lifecycle, immutable snapshot naming and retention destinations.

No residual is unclassified.

## Issue And Closeout Status

This work unit is not linked to an external issue. No issue comment, closing keyword, issue mutation or external PR comment is required or claimed.

The PR remains OPEN and Draft. This closeout does not approve, mark ready, request reviewers, merge or delete the branch, and does not claim production readiness.

## Next Entry Point

After the user merges PR #23, update `main` from `origin/main` and begin a fresh Phaseflow goal confirmation for the remaining PA14 sharing command behavior, tentatively `PA14-sharing-command-code-rotation`. Do not continue from this unmerged branch or reuse this revocation goal confirmation as authorization for rotation.
