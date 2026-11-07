# PA18 Sharing Media Authorization and Playback - Final Closeout

## Status

- Work unit: `PA18-sharing-media-authorization-and-playback`
- Gate: `final closeout pass`
- Branch: `codex/pa18-sharing-media-authorization-and-playback`
- Base: `main` at `9064af34b38d73774b01670d3ed7ed22df6567dd`
- Reviewed/controller PR head: `e0aa805b4d197d02c9541f1e8001b33309691ccf`
- Draft PR: `https://github.com/zh94695424/one-call/pull/28`
- Issue linkage: not applicable; this work unit was not tied to an external issue

## What changed

The slice adds the frozen operation:

`POST /api/v1/public/shares/{capabilityToken}/admissions/{admissionId}/media-authorizations`

It delivers:

- the exact two-field request `{authorizationRequestId, purpose}` and response
  `{credential, expiresAt}`;
- digest-only capability lookup and exact admission/grant/witness association;
- subject risk, captured/current binding, tenant/member/account and current
  grant status/time/generation checks;
- ACTIVE and already-admitted LOCKED behavior without creating, recounting or
  extending an admission/session;
- stable same-purpose replay, changed semantic conflict and deterministic
  redacted success/FAILURE events;
- PostgreSQL transaction and lock closure before provider I/O, with the DB
  append as conflict authority;
- canonical microsecond provider expiry across validation, payload, durable
  comparison and response;
- an additive fail-closed provider seam preserving PA17 functional-interface
  and admission behavior.

No V001-V012, OpenAPI, POM, schema or ACL file changed.

## Verification

- Linux Java 21 unfiltered `mvn -q -Pcontainer-smoke verify`: exit 0.
- PostgreSQL 16.15.
- Surefire: 20 classes / 119 tests.
- Failsafe: 9 classes / 176 tests.
- Combined: 295 tests, zero failures/errors/skips.
- Exact source=CI=XML inventory: 119 / 176 / 295.
- `PublicShareAdmissionIT=37`; `SharingAdmissionRoleLeastPrivilegeIT=9`.
- Actual-method recipe: `103+162=265 -> 119+176=295`.
- Comment-inclusive line scan: `266 -> 296`, explicitly not a method total.
- Exactly the frozen 17 implementation paths; V001-V012/OpenAPI/POM unchanged.
- Canonical LF and `git diff --check` pass.
- GitHub Actions run `42394982448` on reviewed head `e0aa805` passed both
  `contract-regressions` and `java-runtime`.
- GitHub PR metadata for that head: OPEN, Draft, base `main`, head branch
  correct, `mergeStateStatus=CLEAN`.

## Review and finding status

- The accepted plan and all plan reviews/fixes completed before implementation.
- Initial code A/B reviews returned fail; the 30 placeholders and production
  transaction/authorization defects were fixed and re-reviewed.
- Subsequent post-fix A/B rounds fixed CURRENT-binding, witness bounds,
  selected-workspace, outage/replay/format/content-type, provider-boundary,
  ordering, account-mapping and provider-expiry precision gaps.
- Final independent DeepSeek A and MiMo 2.6 Flash B both returned
  `pass-with-risks` with no new current-scope substantive finding.
- Aggregate Deepreview A/B both found no current-scope substantive defect.
- PR Review A/B both found no substantive defect.
- No accepted finding remains open.

## Residual risks and destinations

| Residual | Owner / destination |
| --- | --- |
| Production media-provider idempotency, deterministic replay, signing and expiry contract | Sharing backend/provider integration + Platform/Security; production provider contract tests before enablement |
| Production Q22 TTL/skew/revocation/signing/Redis/provider/configuration values | Security + Platform + Sharing production integration |
| Same-purpose concurrent first calls can append both SUCCESS and FAILURE under partial provider outage | Sharing backend/provider integration + Platform/Security |
| Database-down `appendFailure` cannot guarantee a durable FAILURE row while the request fails closed | Sharing persistence/backend + Platform database reliability |
| Cross-purpose concurrent first calls can append two purpose-specific successes; linearization needs a later durable-reservation design | Sharing backend/API contract owner in a later work unit |
| Duplicate fingerprint/event derivation and optional defensive checks | Sharing backend maintainability |
| `SHARE_GENERATION_STALE` represents both generation staleness and semantic conflict because it is the operation's only listed 409 | API contract owner; future OpenAPI evolution |
| Media gateway/COS/object resolution, streaming/range delivery and buffered-byte recall | Separate media-delivery work unit |
| Production risk-state provisioning expectations | Spec/product/database owner; the frozen media contract fails closed on absent/unresolvable risk |
| Residual test-maintenance observations from PR/aggregate review | Sharing backend test maintenance |

The default application provider is unavailable and the operation fails closed
until production integration supplies the approved provider/policy values.

## PR and external state

- PR #28 remains OPEN and Draft. It was not marked ready.
- No reviewer was requested, no approval/merge occurred, no branch was deleted,
  no external review comment was posted and no issue was mutated.
- The initial reviewed PR body explicitly distinguishes media authorization from
  end-to-end delivery and does not claim external CI by description; Controller
  separately verified the exact-head GitHub Actions jobs above.

## Completion and next entry

PA18 is complete at `final closeout pass`. This controller-only closeout
delivery commit adds this summary and the Control closeout records; the reviewed
product/evidence tree remains `e0aa805b4d197d02c9541f1e8001b33309691ccf`.

After the user merges PR #28, update local `main` from `origin/main` and start a
fresh Phaseflow goal confirmation for the next bounded ONECALL deliverable. The
tentative next work unit is `PA19-sharing-media-gateway-playback-delivery`, limited
to the separate media gateway/COS/object-resolution and streaming/range-delivery
remainder. It must not inherit PA18 authorization automatically and must not
start from this unmerged branch.
