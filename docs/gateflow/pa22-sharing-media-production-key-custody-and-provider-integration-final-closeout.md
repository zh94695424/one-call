# PA22 Sharing Media Production Key Custody and Provider Integration - Final Closeout

- Work unit: `PA22-sharing-media-production-key-custody-and-provider-integration`
- Type: backend/security feature
- Base: `main` at `8452296bb0b2cc79968801cde0a40eef2ab20dd8`
- Final accepted PR-review head: `e694e1889b5599a80d825e5f7b016a1083961d32`
- Draft PR: https://github.com/zh94695424/one-call/pull/33
- Final status: `final closeout pass`

## What changed

- Added one strict schema-v1 external `MEDIA_PLAYBACK_SIGNING` key-bundle parser behind the existing `MediaPlaybackSigningKeyProvider` seam.
- The bundle maps to exactly one `ACTIVE` and zero or more deterministically ordered `VERIFY_ONLY` keys, with valid kids and exactly 32 bytes of HMAC material per key.
- Added one explicit opt-in, read-only local/mounted file provider with an absolute normalized path, bounded reads, no-follow/no-fallback behavior, before/after file identity checks, per-call reread and fail-closed `Result.Unavailable`.
- Added one conditional Spring provider bean while preserving PA21's single authority, zero/one/many-provider ambiguity handling, snapshot-before-token parsing, ACTIVE-only issuance, verify-only overlap replay, recorded-key-removal failure and current-state enforcement.
- Added 35 named tests with exact `12/10/8/4/1` distribution and maintained source=CI=XML architecture inventory.
- No public OpenAPI/DTO/controller, database/migration/ACL, POM, resource, deployment or real cloud-provider change was made.

## Verification

- Focused suite: 74/74, zero failures/errors/skips.
- PostgreSQL 16.15 `-Pcontainer-smoke clean verify`: `BUILD SUCCESS`; Surefire 266/266, Failsafe 214/214, zero failures/errors/skips.
- Exact checked-in source=CI=XML inventory: `33/266 + 13/214 = 46/480`; historical PA22 start `231+214=445`; annotation scan `446 -> 481`; sharing internal files `76 -> 79`; POM `13` Failsafe includes and dependency/plugin surfaces unchanged.
- `backend/pom.xml`, public OpenAPI, migrations/resources and deployment have no diff from the base merge commit.
- Hosted CI on final head `e694e188`: `java-runtime=SUCCESS`, `contract-regressions=SUCCESS`.
- PR #33 at closeout: `OPEN`, Draft=true, `MERGEABLE`, `CLEAN`; no reviewer request or ready-for-review mutation occurred.

## Review and finding status

- Plan: accepted after independent DeepSeek/DeepPlan A and MiMo B review.
- Code: accepted after replacement DeepSeek A and MiMo B review; both passed with no material finding.
- Aggregate deepreview: independent DeepSeek A and MiMo B both passed with no material finding.
- PR review: initial A/B found one Low audit-record issue while both independently passed product behavior; two bounded documentation fixes closed it. Final targeted A/B re-reviews both returned `pass` with finding status `fixed`.
- No accepted finding remains unfixed. No blocking open question remains.

## Residual risks and ownership

- Real Tencent Secret Manager identity, SDK/API, access policy, provisioning, key version lifecycle and custody evidence: Platform/Security later work unit.
- Read-only direct-regular-file mount/ACL, coherent whole-file replacement and stable kid-to-material identity: Platform/Security operations.
- Hostile filesystem race limits and per-call read latency: Platform/Security hardening and Operations measurement; current behavior fails closed.
- Q22 credential TTL/skew/rotation/revocation timing: Security and sharing contract owners.
- Process-memory key residence and HMAC replay during verify-only overlap: Security; current redaction and defensive copying are not HSM isolation.
- Cross-snapshot same-kid substitution is not detectable without a historical registry: Platform/Security key-identity process, explicitly outside PA22.

## Issue and merge state

- This work unit is not associated with a GitHub issue; issue linkage, closeout comment and automatic close are not applicable.
- No merge, approval, mark-ready action, reviewer request, external comment, branch deletion or issue mutation was performed.

## Next entry point

After the user merges PR #33, fetch updated `main`, create a fresh managed worktree and `codex/` branch from the merge commit, then run a new Phaseflow goal confirmation. The tentative next candidate is the real Tencent Secret Manager adapter/provisioning boundary for K20, owned by Platform/Security; PA22 authorization does not inherit into that unconfirmed scope.
