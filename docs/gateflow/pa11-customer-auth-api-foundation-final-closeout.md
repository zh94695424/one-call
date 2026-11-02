# PA11 customer authentication and API foundation — final closeout

- Work unit: `PA11-customer-auth-api-foundation`
- Type: architecture-sensitive runtime/API feature
- Status: `final closeout pass`
- Draft PR: `https://github.com/zh94695424/one-call/pull/19`
- Base: `main` at `96f7a625cca302ed1ec55093591b62fb059939ef`
- Reviewed/accepted PR head: `4aa61f8c9698201ea111289d94c850cb03a2da5b`
- Local/remote/PR head match: verified
- Issue association: not applicable; no closing keyword or issue mutation

## What changed

- Added an opt-in `web` runtime while preserving default
  `spring.main.web-application-type: none` and database-free startup.
- Added a provider-neutral opaque customer-session verifier port and fixed audience
  `onecall:customer-session:v1`; no production verifier, issuer, Redis store, TTL,
  provider adapter, cookie rollout or bypass profile.
- Added only `GET /api/v1/customer/me`, returning the frozen server-derived
  `CustomerContext` from the current ACTIVE account, ACTIVE memberships and ACTIVE
  organizations.
- Added one read-only, role-bound PostgreSQL projection with PERSONAL-first deterministic
  workspace ordering, no writes, no locks and no secret/provider facts in the response.
- Added sanitized, closed Problem responses, request-local trusted principal handling,
  default/web JAR smokes, exact source/CI/XML inventories and a positive-first dependency
  firewall.
- Kept V001-V009, OpenAPI, architecture/design/security documents and default/persistence
  configuration unchanged.

## Verified evidence

- Local unrestricted `clean verify -Pcontainer-smoke`: BUILD SUCCESS in 2:01,
  **49 Surefire + 84 Failsafe = 133**, zero failures/errors/skips.
- Exact source/CI/XML inventory passed for 11 Surefire classes and 3 Failsafe classes
  (`64+10+10`).
- Dependency firewall passed baseline 37/34, web 55/48, 47 exact counterparts,
  8 dependency-only entries, one exact packager JAR, seven approved legacy-regex
  collisions and exactly 82 adversarial probes.
- Default and opt-in web packaged-JAR smokes passed with no production verifier or
  database pool.
- GitHub Actions run `41379734415` on exact head `4aa61f8c9698201ea111289d94c850cb03a2da5b`:
  `contract-regressions` SUCCESS and `java-runtime` SUCCESS.
- Protected V001-V009/OpenAPI/design/security/default-config diff is empty.
- Final aggregate review and PR review each used isolated DeepSeek A and Qwen B inputs,
  with both reviewers submitting before Controller intake.

## Review and finding status

- Final aggregate re-review A: `docs/reviews/code-review-20261102-094213.md`,
  `pass-with-risks`.
- Final aggregate re-review B: `docs/reviews/code-review-20261102-095958.md`,
  `pass-with-risks`.
- PR review A: `docs/reviews/pr-19-review-20261102-101554.md`,
  `pass-with-risks`.
- PR review B: `docs/reviews/pr-19-review-20261102-104233.md`,
  `pass-with-risks`.
- No Critical/High/Medium runtime, authentication, business authorization, ACL, write or
  default-runtime defect remains in the accepted scope.
- Accepted provenance findings were fixed in the controller record: current PR head,
  current S1 report hash, current plan hash and durable 33-path product/test/CI/README
  manifest are now explicit; `.gitignore` is separately covered by the frozen branch
  diff.
- The optional server-side trace emission is deferred to Backend/Observability under the
  accepted plan's optional-diagnostic contract.
- Source-text test coupling is deferred to Backend QA under the existing N-07
  maintenance boundary.
- Authenticated negotiation/non-frozen container statuses mapping to sanitized 500 is
  rejected as a defect because it is the explicitly frozen PA11 plan behavior.

## Documentation updates

- `backend/README.md`: current opt-in web/profile/credential/refusal semantics, current
  dependency summary and exact 49/84/133 CI contract.
- `docs/gateflow/pa11-customer-auth-api-foundation-plan.md`,
  `pa11-customer-auth-api-foundation-plan-fix-1/2/3.md`: accepted design, dependency
  inventory and provenance corrections.
- `docs/gateflow/pa11-customer-auth-api-foundation-s1-implementation.md` and
  `pa11-customer-auth-api-foundation-s1-review-fix-1.md`: implementation and review-fix
  evidence.
- `docs/gateflow/pa11-customer-auth-api-foundation-aggregate-fix-1/2.md`: aggregate
  review fixes, implementation identity and final Controller validation.
- `docs/gateflow/project-execution-control.md` and committed `docs/reviews/`: complete
  Gateflow decisions, dual-review artifacts and residual ledger.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| Production verifier/issuer/store/revocation/TTL/provider and authenticated success | Identity/Backend/Security; separately approved session-provider integration |
| No production trace sink or denial-audit acknowledgement | Backend/Observability and Identity/Backend/Security; tracing/audit integration |
| Reserved 403/422/429 vocabulary has no HTTP producer | Backend/API with A1/docs/15 convention work |
| Positional prepared SQL typing | Backend/Database; typed-query refactor |
| Advisory workspace snapshot has no authorization lease | Backend/Domain/Security; protected-operation authorization |
| Unbounded success-body buffering and unpaginated workspace capacity | Backend/Web and Platform/Capacity; measured capacity/transport hardening |
| Source-text/architecture test maintenance and N-07 overlap | Backend QA; implementation-time test maintenance without weakening 49/84/133 |
| Reproducible packaged-JAR bytes and durable evidence carriers | Backend/CI and Controller; reproducible-build/provenance work |
| Mutable `postgres:16` tag, image digest and production DB provisioning/collation | Backend/CI, Platform/Database/Security; deployment/capacity gates |
| Connector-level malformed/oversized requests and committed-response failures | Platform/Security/Observability; edge/transport policy |
| A2/A5 canonical baseline status previously red on untouched inputs | A0/A2/A5 contract owners; current PR checks now pass, no PA11 ownership transfer |
| The next HTTP slice must deliberately widen or scope the highest-precedence `/*` filter | Backend/Architecture; next API work unit |

All remaining risks have an owner and destination. No residual risk is unclassified or
blocking a merge decision for the accepted PA11 scope.

## Final decision

- `draft-PR-pass`: accepted.
- `final closeout`: passed.
- No merge, approval, mark-ready action, reviewer request, external PR comment, issue
  mutation or branch deletion was performed.
- PR #19 remains Draft for the user's explicit review and merge decision.

## Next entry point

After the user merges PR `#19`, update `origin/main`, create a fresh managed worktree and
`codex/` branch from the merged commit, then perform a new Phaseflow goal confirmation.
Do not reuse PA11 confirmation or this branch as authorization. The next bounded
candidate is customer-session provider integration (issuer/store/revocation/TTL and
provider mapping), but its exact scope requires explicit user confirmation.
