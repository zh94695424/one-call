# PA14 Sharing Command Service Foundation Final Closeout

- Work unit: `PA14-sharing-command-service-foundation` (backend feature).
- Status: `final closeout pass` for the submitted create/foundation PR scope.
- PR: `https://github.com/zh94695424/one-call/pull/22`, OPEN/Draft, base `main`.
- Accepted PR-review commit and verified remote head: `6d0ba5c6f1485626a1ed802c20c0d40517dc65f8`.
- Base merge: PR #21 at `259aeb458883c484e15077c3f7a157ef521e2203`.
- Issue association: not applicable; no issue was linked or mutated.

## What changed

- Added the customer share-create command foundation behind the trusted customer boundary, including the separate command datasource/transaction, command-role assertion, same-connection evidence projection read/restore, ownership/risk and creation-policy ordering, deterministic creation identity/fingerprint, replay/conflict behavior, one-time capability/code material, and atomic grant/Outbox/audit persistence.
- Corrected the accepted PR-review finding so `scopeDigest` hashes persisted `{content, verification}` and `content_sha256` hashes the complete persisted `{content, verification, creation}`. `creationFingerprint` remains the canonical `creation` object with only its fingerprint member removed.
- No V001-V010, OpenAPI, schema, public admission/ticket/media, revoke/rotate success path, frontend or production provider/policy values changed.

## What was verified

- Targeted `CustomerShareCommandServiceTests`: 4/4 PASS.
- Focused `CustomerShareCommandsIT`: 8/8 PASS on PostgreSQL 16.15, with Flyway V001-V010 applying through v010.
- Source/CI inventory: 77 Surefire + 108 Failsafe = 185, with no test-method additions or inventory drift from the fix.
- Corrected local candidate: exactly seven review paths; A/B peer-free snapshots had 899 identical files and zero review-artifact leakage.
- Corrected PR re-review: A DeepSeek and B MiMo 26 Flash both `PASS`, zero material findings, after independently verifying the frozen diff/manifest and source hashes.
- Pushed GitHub CI run `41956552624`: `contract-regressions` PASS in 24 seconds and `java-runtime` PASS in 3 minutes 9 seconds.
- A local full-suite attempt on the restricted execution host reached 56 passing unit tests but was blocked by AF_UNIX loopback restrictions in 21 unchanged web tests before Failsafe. That environment limitation is retained as evidence; the remote Java CI job is the full-suite pass for this candidate.

## Docs updates

- `docs/gateflow/pa14-sharing-command-service-foundation-plan.md` now has one canonical digest-member contract matching frozen docs/14.
- Fix artifact: `docs/gateflow/pa14-sharing-command-service-foundation-pr-review-fix-1.md`.
- Controller validation: `docs/gateflow/pa14-sharing-command-service-foundation-pr-review-fix-1-validation.md`.
- Initial PR review artifacts:
  - `docs/reviews/code-review-20261105-095233.md`
  - `docs/reviews/code-review-20261105-100748.md`
- Corrected re-review artifacts:
  - `docs/reviews/code-review-20261105-103150.md`
  - `docs/reviews/code-review-20261105-103500.md`

## Finding status

- Initial A medium finding: `fixed`.
- Initial B zero-finding result: retained.
- Corrected A/B re-review: `PASS`, zero material findings.
- No accepted finding or blocking open question remains for this PR scope.

## Remaining risks and owners

- Application request-body cap: Gateway/Security, with later Backend hardening.
- Evidence projection lifecycle semantics: Evidence owner, assigned to later approved work.
- Design-document snapshot-naming ambiguity outside the frozen docs/14 digest contract: Design/Evidence owner.
- Canonicalizer/read-back oracle coupling for shapes beyond the current vectors: Backend QA test hardening.
- Production policy, KDF, HMAC, mobile verification, provisioning and enablement inputs: Security/Product/Platform owners; current implementations fail closed.
- Host AF_UNIX loopback restriction for local web tests: Controller/QA environment risk; remote CI passed the full Java workflow.
- Revoke/code-rotation success behavior and broader public admission/media work remain outside this PR and require a fresh Phaseflow goal confirmation.

## Gate result

`draft-PR-pass` and `final closeout pass` are accepted for the submitted create/foundation scope. No merge, GitHub approval, ready-for-review action, reviewer request, branch deletion, issue mutation or external comment was performed.

After the user merges PR #22, fetch the updated target `main`, record the merge commit, and start a fresh Phaseflow goal confirmation from the accepted PA14 plan for the next planned sharing-command slice. No implementation is authorized by this closeout.
