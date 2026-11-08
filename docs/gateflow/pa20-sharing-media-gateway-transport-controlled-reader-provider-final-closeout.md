# PA20 sharing media gateway transport and controlled-reader provider - final closeout

## Work unit and PR

- Work unit: `PA20-sharing-media-gateway-transport-controlled-reader-provider`
- Type: backend media transport/security feature
- Branch: `codex/pa20-sharing-media-gateway-transport-controlled-reader-provider`
- Base: `main` at `9da37930651d2d0f4ba76a1e8c8d1c15a6ba435e`
- Draft PR: <https://github.com/zh94695424/one-call/pull/31>
- Accepted PR review checkpoint: `d77995f0b0eae58bbd03eef8d5f7c4ea588673e3`
- Controller bookkeeping head before closeout delivery: `d83aaee1be536880f5a561d3715cd1159fbceee5`
- Final closeout delivery head: `be68653edbd8659600319b0e2f16d6411a50e941`
- PR state at closeout: `OPEN`, draft `true`, merge state `CLEAN`

This is a feature work unit with no associated GitHub issue. No issue closing
keyword, issue closeout comment or issue mutation is required or claimed.

## What changed

- Added one bounded internal media-gateway GET data-plane route that accepts an
  opaque Bearer playback credential, delegates authorization to the accepted
  PA19 `MediaPlaybackService`, and opens one evidence-owned controlled reader
  only after successful metadata authorization.
- Added deterministic full-GET and single-byte-range handling, exact object-byte
  transfer, safe status/header mapping, terminal reader cleanup, redacted
  problems and fail-closed behavior for unavailable providers.
- Kept production verifier, COS/KMS/Secret Manager and public origin deployment
  unavailable by default; deterministic providers exist only in tests.
- Added the minimum public `ReadSession` lifecycle contract, explicit gateway
  response headers and no reliance on YAML merge keys.
- Added unit, architecture and real PostgreSQL 16/embedded-HTTP tests, the
  standalone gateway OpenAPI contract, POM/CI inventory synchronization and
  bounded CI portability fixes.

The work unit did not change V001-V013 migrations, the public-control
`onecall-v1.openapi.yaml`, PA19 production playback behavior, production
credentials, frontend surfaces or deployment manifests.

## Verification

- Controller clean command: `mvn -B -ntp -Pcontainer-smoke clean verify`.
- PostgreSQL 16.15 in Docker.
- Surefire `29 classes / 196 methods`; Failsafe `13 classes / 204 methods`.
- Combined fresh XML inventory `42 classes / 400 methods`, with zero failures,
  errors or skips. `MediaGatewayTransportIT` passed `6/6`.
- Exact source=CI=XML identity and embedded inventory assertions passed:
  `42/400`, Failsafe includes `13`, line scan `401`.
- `npm run check:s3` passed after the final PR fix, including standalone gateway
  Redocly validation, generation/typecheck, doc-map checks and S3 assertions.
- Strict YAML 1.2 parsing passed with no `<<:` tokens and with all required
  `206`/`401`/`405`/`416` headers present.
- Controller bookkeeping head `d83aaee1...eee5` passed hosted CI run
  `42519442863`: `contract-regressions` SUCCESS and `java-runtime` SUCCESS.
- Final closeout delivery head `be68653e...e941` also passed hosted CI run
  `42520130741`: `contract-regressions` SUCCESS and `java-runtime` SUCCESS.
  The remote branch and PR #31 head matched that SHA and remained Draft/CLEAN.

## Documentation and review record

- Accepted plan and plan-fix-1 are recorded under `docs/gateflow/`.
- Four plan review/re-review artifacts are recorded under `docs/reviews/`.
- The implementation report, implementation fix and two bounded CI-fix
  artifacts are recorded under `docs/gateflow/`.
- Two slice code reviews, two aggregate deepreviews and four PR
  review/re-review artifacts are recorded under `docs/reviews/`.
- Fresh re-review snapshots, manifests, prompts and runner output remain
  private/untracked and are not part of the deliverable.

## Finding disposition

- No material finding remains accepted or unresolved.
- `PA20-PR-A1` is `fixed`: the public controlled-reader lifecycle is documented
  in production Javadoc and agrees with implementation and tests.
- `PA20-PR-A2` is `rejected-with-reason`: the accepted historical plan remains
  immutable; its CRLF checkout digest was superseded by the canonical-LF CI fix
  without changing the public contract bytes.
- `PA20-PR-A3` is `fixed`: the standalone gateway OpenAPI uses explicit headers,
  contains no YAML merge key and passes strict YAML 1.2 validation.
- Fresh DeepSeek A and MiMo V2.6 Flash B re-reviews both returned
  `pass-with-risks` with zero new material findings. They used identical frozen
  manifests and submitted before the Controller read either conclusion.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| Production credential verifier source, opaque wire format, signing/key rotation | Security + Sharing + Platform |
| Real COS/KMS/Secret Manager byte authority, read timeout and provider enablement | Evidence + Platform + Security |
| Q22 TTL, clock skew, revocation propagation and snapshot-to-transfer race | Security + Sharing + Platform |
| Partial temporal windows and format-aware time-to-byte mapping | Evidence + Media/Backend + Security; separate authorization contract if required |
| Public origin/path, TLS, ingress, CDN, LB, WAF, CORS and player deployment | Infrastructure + Security + Backend |
| Sanitized observability for fail-closed unavailable providers and timeouts | Observability + Platform |
| Already committed/buffered bytes cannot be recalled; late truncation must not invent a second HTTP status | Platform + Security + Observability |
| Container normalization, slow-client and resource-budget behavior before the application filter | Backend + Platform operational hardening |
| `awaitility` is currently inherited from Spring Boot test support rather than declared directly | Build/Backend dependency hardening |
| Architecture test checks required OpenAPI header tokens at file level rather than a per-status structural map | API/contract test maintenance |
| Javadoc failure-path wording does not spell out the close-throws branch ordering | Evidence/Sharing documentation maintenance |
| RFC range-unit case sensitivity: only the accepted lowercase `bytes=` form is enabled | Sharing gateway policy if intentionally broadened |

No residual risk is unclassified. None authorizes additional implementation
inside PA20.

## Final disposition

- Code review and aggregate deepreview: pass.
- PR review and fresh PR re-review: pass with classified risks.
- Accepted PR review checkpoint and final push: verified.
- Exact-head hosted CI: pass.
- `draft-PR-pass`: achieved.
- Final closeout: **pass**.
- Work unit status: **completed**.

The normal Git HTTPS route to `github.com:443` was unavailable during final
delivery, while authenticated `api.github.com` remained reachable. The exact
local commits were reproduced through GitHub's Git Data API with identical
parent/tree/blob content, author/committer identity, timestamps and messages;
non-forced ref updates and PR head reads confirmed the resulting SHAs. No
credential was printed or stored.

No merge, approval, mark-ready action, reviewer request, branch deletion,
external comment or issue mutation occurred.

## Next entry point

After the user merges PR #31, fetch the updated target `main`, create a fresh
managed worktree and `codex/` branch from that merge commit, and run a new
Phaseflow goal confirmation for the next bounded work unit. The tentative next
unit is `PA21-sharing-media-production-provider-integration`, focused on
production verifier/provider, private object authority, policy inputs and
deployment/observability integration. PA21 is not yet named as final and this
closeout does not pre-authorize its scope. Do not continue from this unmerged
branch or carry PA20 goal/review authorization into the next phase.
