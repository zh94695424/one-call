# PA19 sharing media gateway playback foundation - final closeout

## Work unit and PR

- Work unit: `PA19-sharing-media-gateway-playback-foundation`
- Type: backend/security feature
- Branch: `codex/pa19-sharing-media-gateway-playback-delivery-resume`
- Base: `main` at `3afc3e9a68900ea718b534650ca661861f036d4e`
- Draft PR: <https://github.com/zh94695424/one-call/pull/30>
- Final reviewed-content head: `e731d1529305a6c79876022801695894b5e9c97f`
- PR state at closeout: `OPEN`, draft `true`, `MERGEABLE`, `CLEAN`

This is a feature work unit with no associated GitHub issue. No issue closing
keyword or closeout comment is required or claimed.

## What changed

- Added the provider-neutral internal playback chain:
  trusted credential verifier -> immutable claim validation -> exact
  audience/purpose/share/admission/object/time checks -> closed current-state
  sharing snapshot -> evidence-owned object resolution -> exactly one bounded
  controlled open.
- Added fail-closed typed outcomes for malformed, invalid, unavailable, expired,
  not-yet-valid, wrong-audience, wrong-purpose, revoked, stale-generation,
  cross-admission, cross-object, missing-evidence, ambiguous-evidence,
  membership/ownership/binding/risk and ambient-transaction cases.
- Added the minimum sharing-owned current-state read and retained evidence as the
  owner of private object facts through the existing `evidence::media-read`
  named interface. Controlled delivery remains behind an injected reader
  contract; its default opens nothing.
- Added deterministic provider ambiguity handling, read-only
  `REPEATABLE_READ`/`REQUIRES_NEW` state reads, generation and frozen-bound
  validation, monotonic grant-extension handling without extending frozen
  admission/session/claim authority, redaction and no-post-open-I/O guarantees.
- Added the exact unit, architecture and real-PostgreSQL tests plus synchronized
  POM/CI source=CI=XML inventory.

The implementation remains inside the accepted 20-path author allowlist.
V001-V013, OpenAPI, frontend, public data-plane routes, deployment manifests and
production credentials remain unchanged.

## Verification

- Controller clean command:
  `mvn -B -ntp -Pcontainer-smoke clean verify`
- PostgreSQL `16.15`.
- Surefire `25 classes / 162 methods`; Failsafe `12 classes / 198 methods`.
- Combined `37 classes / 360 methods`, zero failures, errors or skipped tests.
- `MediaPlaybackIT` `6/6`, including real role/transaction/evidence behavior,
  committed revocation/rotation, extension bounds, duplicate/ambiguous evidence
  guard restoration, failure snapshots, post-open no-I/O and redaction.
- Exact source=CI=XML inventory: `SOURCE_ONLY []`, `EXPECTED_ONLY []`,
  `162/198 = 360`, line scan `361`, Failsafe includes `12`.
- Hosted CI at final reviewed-content head `e731d15...`:
  `contract-regressions` PASS (`22s`) and `java-runtime` PASS (`7m18s`),
  including container smoke, inventory, web-jar smokes and dependency firewall.
- Two transient `actions/setup-java@v4` runs failed before any repository step
  with repeated HTTP `504` downloading Temurin; a later rerun succeeded. This is
  classified as external GitHub release-download infrastructure, not a code,
  test or workflow regression.

## Documentation

- Accepted plan and plan-fix-1 under `docs/gateflow/`.
- Four plan-review / re-review artifacts under `docs/reviews/`.
- Implementation report under `docs/gateflow/`.
- Two slice code reviews and two aggregate deepreviews under `docs/reviews/`.
- Two PR review artifacts under `docs/reviews/`.
- Authoritative Controller runtime log, fresh XML and inventory evidence under
  the PA19 resume `controller-runtime` directory.
- Lifecycle, review, commit, CI and next-entry facts remain authoritative in
  `docs/gateflow/project-execution-control.md`.

## Findings

No material finding remains accepted or unresolved.

- Plan A-01 (grant extension vs frozen caps), A-02 (scope retention vs exposure),
  A-03 (inconsistent `Allowed`) and B-01 (future provider module boundary) are
  closed or correctly classified as later-work residual.
- Slice code review A/B: pass.
- Aggregate deepreview A/B: pass.
- PR review A/B: pass. Reviewer B's first PR-review run had an empty result and
  no artifact, so it was non-gating; the one permitted same-provider fresh retry
  succeeded against the identical frozen input.
- Reviewer sandbox limitations around the parent manifest and UTF-16 log were
  independently resolved by Controller manifest validation, full log decode and
  commit/tree inspection.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| Authenticated issuance-time claim provenance, signature/key/wire verifier | Security + Platform + Sharing provider integration; future cross-module wiring requires approved PA0-R-09 named-interface/module-map and docs/12 adapter-placement review |
| Production controlled reader, private location resolution, COS/KMS/Secret Manager and byte delivery | Platform + Security + Evidence provider integration; default reader remains unavailable and opens nothing |
| Q22 TTL/skew/revocation propagation and snapshot-to-open race | Security + Sharing + Platform; no numeric policy or zero-race guarantee is invented here |
| Gateway/player/origin/CDN/range/streaming transport | Infrastructure + Security / `R-A3-MEDIA-GATEWAY-TRANSPORT` |
| Deployed edge/collector/analytics redaction | Platform + Security + Observability / `R-A3-REDACTION-RUNTIME` |
| Sanitized operational diagnostics for fail-closed `Unavailable` and query/connection timeouts | Observability + Platform operational hardening |
| Deployment activation and dedicated credential provisioning | Deployment + Database + Security deployment runbook |

No residual risk is unclassified, and none authorizes additional work in PA19.

## Final disposition

- Code review: pass.
- Aggregate deepreview: pass.
- PR review: pass.
- Final reviewed-content hosted CI: pass.
- Draft PR: #30.
- `draft-PR-pass`: achieved.
- Final closeout: **pass**.
- Work unit status: **completed**.

No merge, approval, mark-ready action, reviewer request, branch deletion,
external comment or issue mutation occurred.

## Next entry point

After the user merges PR #30, fetch the updated target `main`, create a fresh
managed worktree and `codex/` branch from that merge commit, and run a new
Phaseflow goal confirmation for the tentative next bounded work unit
`PA20-sharing-media-gateway-transport-and-controlled-reader-provider`. Do not
continue from this unmerged branch or carry PA19 goal/review authorization into
PA20.
