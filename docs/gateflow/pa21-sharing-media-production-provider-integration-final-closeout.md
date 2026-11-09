# PA21 sharing media production provider integration — final closeout

## Completion

- Work unit: `PA21-sharing-media-production-provider-integration`.
- Status: `final closeout pass`.
- Branch: `codex/pa21-sharing-media-production-provider-integration`.
- Base: `origin/main` merge commit `6cdb01710f9fa24d018f68db499f437d123ff7cd`.
- Draft PR: `https://github.com/zh94695424/one-call/pull/32`, title `PA21: sharing media playback credential authority`.
- PR state at closeout: `OPEN`, `isDraft=true`, `MERGEABLE`, `CLEAN`, head `21ef8f8808c1dd510052165bef1ecc0222af98bc`.
- The user has not authorized merge, approval, mark-ready, reviewer request, external comment or issue mutation. None occurred.

## What changed

- Added one unconditional internal `MediaPlaybackCredentialAuthority` implementing both the existing P3 issuing seam and the PA19 verifier.
- Added the versioned HMAC-SHA-256 `MEDIA_PLAYBACK_SIGNING` wire profile, canonical strict parser, domain separation, 32-byte key validation, canonical Base64URL, token/payload bounds and constant-time MAC verification.
- Bound share grant, admission, evidence/object, code/revocation generations, purpose, issuance/not-before/expiry and the full-object `[0,duration)` window into the exact PA19 `VerifiedMediaClaim`.
- Added active/verify-only rotation overlap and fail-closed zero/ambiguous/invalid/disabled key handling; recorded-kid replay reconstructs only the originally committed nonsecret anchor and expiry and never persists a bearer token.
- Added evidence resolution after authoritative transactions close, a 19-key pre-provider fingerprint, exact 24-key success/20-key failure payloads, replay/current-bound validation and `EventWriteResult.first()` winner convergence.
- Wired the same authority instance through persistence/admission/playback; removed the legacy fallback verifier; legacy P2 issuance remains `Unavailable`.
- Added unit and PostgreSQL 16 integration coverage, architecture/CI pins and Code/PR review records.
- No public OpenAPI operation, DTO/controller, database table/column/lifecycle, migration/ACL, POM dependency, COS/KMS/Secret Manager adapter, Q22 numeric policy, Redis or deployment change is included. V001-V008 remain unchanged.

## Verification

- Local Docker/PostgreSQL 16.15 `mvn -B -ntp -Pcontainer-smoke clean verify`: exit `0`, `BUILD SUCCESS`, 15m19s.
- Surefire `30` classes / `231` tests; Failsafe `13` classes / `214` tests; total `43` classes / `445` tests; failures/errors/skips `0/0/0`.
- Exact source=CI=XML inventory: `30/231 + 13/214 = 43/445`; line scan `446`; POM `13` unchanged Failsafe includes.
- Final local log: `C:\Users\Administrator\.codex\worktrees\pa21-sharing-media-production-provider-integration\pa21-controller-container-full-verify-fix2-final.log`, 440,926 bytes / SHA256 `086F1C55814EC88BFA61F3B957274855965B94499581A40B159BC00C8FB5A74B`.
- `npm ci && npm run check:s3` passed under WSL Node 22 earlier in the implementation gate; public OpenAPI remained 49 operations with exact doc-map/catalog checks.
- GitHub Actions on accepted PR-review head `21ef8f8`: run `42655287347`; `contract-regressions` SUCCESS, `java-runtime` SUCCESS.
- `git diff --check` passed; private `.codex` is untracked/excluded; ignored generated `build/` and `target/` content is not committed.

## Review and finding status

- Plan review, plan fixes 1-4 and fresh A/B re-reviews all passed with no blocking open question.
- Code review A/B found eight accepted test-conformance/cleanup findings in total across A/B: evidence fault matrix/resolver, replay fingerprint/payload/hash, metadata negatives, dead `preFingerprint`, duplicate drift cases, service-level tamper proof and exact-claim capture.
- Fix-1 and fix-2 addressed all accepted findings. Fresh A/B re-review and aggregate A/B review confirmed every finding `fixed`; final PR A/B review raised no material finding.
- No `accepted` finding remains unfixed. No finding is `needs-more-evidence`; supplied-evidence limitations were independently closed by Controller hashes/log/XML/CI checks.
- Rejected/deferred questions remain documented: pre-existing `Malformed` result variants are not PA21 additions; authority replay caller trust is behind the production service contract; cross-purpose concurrent request-key uniqueness would require separate schema/locking hardening.

## Residual risks and destinations

| Residual | Owner / destination |
| --- | --- |
| Production `MEDIA_PLAYBACK_SIGNING` key custody, stable kid-to-material binding, rotation overlap and removal runbook | Platform + Security, future provider-integration work |
| Application-clock vs database-decision-clock skew and the existing short validity-window sensitivity | Platform + Operations monitoring/runbook |
| Evidence metadata drift intentionally conflicts before replay; availability tradeoff | Sharing + Evidence product/operations |
| Future partial-window playback policy; PA21 intentionally issues only `[0,duration)` | Sharing/Media separate work unit |
| Cross-purpose concurrent same-request-key event uniqueness | Sharing/Backend hardening; schema/locking change outside PA21 |
| Authority replay caller trust | Sharing/Backend internal contract; production service validates committed event expiry/anchor |
| Exact architecture/test/CI pin maintenance | Architecture/CI owners |
| Ignored annotation-processor output observed in the local worktree | Controller hygiene only; excluded and not committed |

All remaining risks have an owner/destination. None blocks this slice or final closeout.

## Issue status

This work unit is not associated with a GitHub issue. No closing keyword, closeout comment or issue mutation is required.

## Post-merge next entry

After the user merges PR #32, fetch updated `main`, create a fresh managed worktree and `codex/` branch from that merge commit, then run a fresh Phaseflow goal confirmation for the next bounded ONECALL work unit. The tentative next candidate is `PA22-sharing-media-production-key-custody-and-provider-integration`, covering production key custody/provider adaptation and operational rotation without inheriting PA21 authorization. Do not continue from the unmerged PA21 branch.
