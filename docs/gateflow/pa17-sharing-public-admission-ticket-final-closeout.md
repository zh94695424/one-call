# PA17 Sharing Public Admission And Ticket — Final Closeout

## Status

**Final closeout pass.** The PA17 public admission/ticket slice is complete and
submitted as Draft PR #27. This does not merge, approve or mark the PR ready.

## What Changed

- Added `POST /api/v1/public/shares/{capabilityToken}/admissions` with the four
  frozen verification modes, digest-only capability lookup, strict closed JSON
  binding and exact frozen Problem mappings.
- Added PostgreSQL admission persistence with current grant/status/time/
  generation/admission-limit/risk/member predicates, deterministic same-key
  replay, atomic admission/event/count/revision writes and full rollback.
- Added commit-before-ticket behavior, deterministic ticket issue/failure
  identities, post-commit expiry rechecks and same-key reconstruction without a
  second admission, recount or session renewal.
- Added a dedicated fail-closed `sharing_admission_role` datasource and the
  privilege-only V012 ACL migration.
- Preserved V001-V011, the V009 three sharing tables, constraints and lifecycle,
  and the OpenAPI contract.
- Synchronized CI, README, architecture tests, service/controller tests and
  PostgreSQL 16 integration/role tests.

## Verification

- Local full runtime: Surefire 20 classes / 103 methods + Failsafe 9 classes /
  162 methods = **265**, zero failures, errors or skips.
- Hosted GitHub Actions run `42314216705` on executable-code head
  `8bb5f4bb668ae100c401d6692c9bbddc99b95c8d`: both `contract-regressions` and
  `java-runtime` succeeded, including container/XML inventory, JAR smokes and
  the 82-probe dependency firewall.
- Targeted PA17 evidence: the 50-method web/service/PostgreSQL subset is green
  inside the full run; the post-fix canonical-LF role IT is 8/8.
- V001-V011 are unchanged and their eleven canonical-LF hashes are pinned.
  V012 is the sole added migration and is privilege-only.
- Source, CI and XML method inventories agree exactly.
- Fixed final code review: A/B `pass`; aggregate deepreview: A/B `pass`; initial
  PR review: A/B pass. Hosted CI found PVR-01, the CRLF/LF hash portability gap;
  it was fixed and fresh PR A/B re-review both returned `pass`.

## Review And Finding Status

- All accepted code-review findings are fixed.
- PVR-01 is fixed by canonical LF normalization and eleven canonical pins.
- No accepted finding or blocking open question remains.
- Deferred/rejected items retain the owners recorded in Control:
  R-04 request-size bounding; N-02 strict captured-binding parsing;
  F-PA17-05 and related test attribution; B-F-05 expiry micro-window; O-01/O-03
  and AGG-O1/AGG-O3 test maintenance; plan raw-hash convention and report
  wording bookkeeping; AGG-O2 historical label traceability.

## Documentation

- Accepted plan and all plan-fix/review records remain in the branch history.
- Implementation report includes the bounded review fixes.
- Control records goal confirmation, all gate decisions, exact review hashes,
  runtime evidence, API transport limitations and residual ownership.
- Eight code-review artifacts, two aggregate-review artifacts and four PR-review
  artifacts record the independent review chain.

## Draft PR

- URL: https://github.com/zh94695424/one-call/pull/27
- Base: `main` at `c7d4d213e7f83bdff767278238033d51b44a13fc`
- Final remote review head: `6337afe7ab903957682dc486298ce27caaffa424`
- Final closeout delivery head: `5e7ae5fcb2ea2443531621a3523af9811b41792b`
- Shared final-closeout tree: `308ce128659c6019e6945c6ad5e9df4733af870e`
- State: OPEN / Draft / MERGEABLE / no reviewer requested.

Hosted run `42316466316` passed both `contract-regressions` and `java-runtime`
on the final closeout delivery head. The preceding reviewed executable-code
head `8bb5f4bb…` also passed both jobs and contains the same product code.
Any subsequent controller-only status update is documentation-only and does not
change this closed work unit.

## Residual Risk / Owners

- Anonymous request-body size/413 enforcement: Platform / Security Web hardening.
- Approved Q22 ticket/session TTL, skew/revocation window, KDF, mobile proof,
  Redis/media credential format and production credentials: Product / Security /
  Sharing Backend / Platform.
- Media gateway, media-authorization/playback/seek/renew behavior: a separate
  media work unit.
- V012 direct-login provisioning, pg_hba/TLS, secret rotation, role collision,
  pool sizing and Redis capacity: Database / SRE / Platform.
- Application-consistent risk/membership reads rather than PostgreSQL RLS:
  Security / Database threat model.
- Test-maintenance residuals, observation fail-closed 503 mapping and the
  signing-window micro-race: Sharing Backend / Database test owners.
- Evidence provenance labels and private-witness limitations: Controller / CI /
  Security evidence owners.

## Issue

No GitHub issue is associated with PA17. Issue linking and a closeout comment are
not applicable.

## Next Entry Point

After the user merges PR #27:

1. Fetch the updated `main` merge commit.
2. Create a fresh managed worktree and `codex/` branch from that merged base.
3. Run fresh Phaseflow goal confirmation before any plan or implementation.

A tentative next work unit is
`PA18-sharing-media-authorization-and-playback`, limited to the frozen
`POST /api/v1/public/shares/{capabilityToken}/admissions/{admissionId}/media-authorizations`
surface and its playback/seek/renew semantics. The ID and scope are not
authorized until the user confirms them, and Q22/provider/media-gateway policy
must not be invented.
