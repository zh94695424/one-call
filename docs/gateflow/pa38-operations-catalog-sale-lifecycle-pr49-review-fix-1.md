# PA38 PR #49 review fix 1

## Gate and finding intake

- Work unit: `PA38-operations-catalog-sale-lifecycle`
- Gate: first PR review fix
- PR: #49, `https://github.com/zh94695424/one-call/pull/49`
- Reviewed head: `649eae6866d81a8348156d82dec3621011d6350e`
- Base: `main` at `c9f91337f8bba8e9340ef6239912b44f26373c25`
- Reviewer A: `docs/reviews/pr-49-review-20261120-001534.md`, `pass-with-risks`
- Reviewer B: `docs/reviews/pr-49-review-20261120-003856.md`, `request changes`
- GitHub Actions run `44562401251` failed on the reviewed head.

Both reviews found no product authorization, concurrency, state-transition,
atomicity or replay defect.

## Adjudication

### Accepted blockers

- `PA38-PRR-B-01` (`accepted`, High; convergent with A-03): validation evidence
  must be tied to the final PR head. The prior run at `649eae6` failed
  `OperationsCatalogVersionBoundaryTests.preservesFrozenInputsAndNoV019`
  because GitHub checked out the frozen PA25 SQL with LF while the historical
  guard pins its CRLF bytes. This fix makes checkout byte behavior explicit and
  requires a complete green run on the corrected head.
- `PA38-PRR-B-02` (`accepted`, High, partially recharacterized):
  `.gitattributes` was absent, so EOL depended on the contributor checkout.
  GitHub's Linux checkout did not fail on `mvnw`; `mvnw` and `.java-version`
  are LF blobs. The real observed failure was the PA25 frozen SQL checkout.
  This fix pins executable text to LF and the historical PA25 guard input to
  CRLF deterministically.
- `PA38-PRR-B-03` (`accepted`, Medium): the final pushed head/tree and the
  successful exact-head CI run must be recorded in Control/current status. The
  reviews remain in the PR as durable historical evidence.

### Deferred with explicit owners

- `PA38-PR49-A-01`: independent catalog-sale enablement/kill switch belongs to
  Platform/Operations/Security deployment control.
- `PA38-PR49-A-02` / `PA38-PRR-B-06`: the retired-object audit projection is a
  deliberate frozen T-S2-14/plan contract; a future audit/retention unit owned
  by Catalog-order/Operations/Audit may add a richer projection.
- `PA38-PR49-A-04` / `PA38-PRR-B-08`: route-shape and problem-writer duplication
  belongs to Backend maintainability hardening.
- `PA38-PRR-B-04`: `OperationsProblemWriter` parity guard belongs to Backend
  maintainability hardening.
- `PA38-PRR-B-05`: authority failure cause/sqlState cleanup belongs to Backend
  recovery/test hardening.
- `PA38-PRR-B-07`: future draft-write work must bind canonical
  `snapshot_sha256`; owner API governance/Catalog-order.
- `PA38-PRR-B-09` / `PA38-PRR-B-10`: 429 reachability, deprecated
  `txid_current()` and floating PostgreSQL image belong to existing
  API-governance and Platform/CI residuals.

No accepted finding requires a product implementation change.

## Changes

- Added `.gitattributes`:
  - `mvnw`, `.java-version` and shell executables are LF on checkout;
  - Windows command shims are CRLF;
  - `backend/ops/postgresql/pa25-runtime-identities.sql` is explicitly CRLF so
    its frozen `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9`
    guard remains reproducible on Linux;
  - `api/openapi/onecall-v1.openapi.yaml` is explicitly CRLF because the same
    guard reads its raw bytes and requires
    `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`.
- Added the two durable PR review artifacts.
- Updated Control/current status with the PR review findings, EOL remediation,
  exact-head CI requirement and residual owners.

The original 31 non-CI product/configuration paths and implementation report
remain byte-identical. To make the EOL fix trigger Linux CI reliably while
keeping the workflow self-recheckable, the same 32-path candidate now changes
only `.github/workflows/ci.yml` (+74 bytes, `11dfd54c...` to
`a26acaa965130209f50a57b08b9b7fde513fa3e0c85851ac30623391ea989787`):
32 paths / 572,735 raw bytes /
`db38b4375c29addd13bfbb8cacde90a172bc32469a736b6b70d7190768500555`.

## Validation

- Local Windows focused boundary test:
  `OperationsCatalogVersionBoundaryTests` `8/8`, 0 failures/errors/skips.
- GitHub Actions run `44562401251` exposed the PA25 raw-hash mismatch; the
  first `.gitattributes` retry advanced past PA25 but run `44569633243` then
  exposed the OpenAPI raw-hash mismatch at `ce8de66d…`. The same deterministic
  CRLF policy now covers both raw-hashed inputs.
- `git ls-files --eol` confirms `mvnw`, `.java-version` and the PA25 SQL have
  LF index content; `.gitattributes` now supplies deterministic checkout
  policy, including CRLF for the PA25 guard input.
- The next commit must be pushed and the complete GitHub Actions `ci` workflow
  must pass on that exact head before PR re-review can pass.

## Next gate

Push this fix, wait for exact-head `ci`, record the run, then dispatch fresh
peer-isolated PR re-review against the corrected head. No merge, approval,
mark-ready, reviewer request, external comment or issue mutation is authorized.
