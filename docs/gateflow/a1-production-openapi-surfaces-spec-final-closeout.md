# A1 Production OpenAPI Surfaces Specification Final Closeout

- Work unit: `A1-production-openapi-surfaces-spec`
- Type: architecture-sensitive contract deliverable
- Gate: `final closeout`
- Status: `final closeout pass`
- Date: 2026-10-25
- Draft PR: `https://github.com/zh94695424/one-call/pull/2`
- Accepted PR review checkpoint: `ad50d07f7663f79dc66edd0f64fc26d3baec6d63`
- Reviewed-content/controller push verified at: `0f8f62e02e45e58e470256ffe45dac0eb0500105`

## What changed

- Delivered one code-generation-ready OpenAPI 3.1 source for 49 first-release operations across customer, operations, self-built IVR, public recording share and authenticated provider-callback surfaces, with counts `18/21/6/3/1`.
- Defined separate trust identities, server-derived authorization boundaries, request/response schemas, stable statuses, RFC 9457 Problem responses, write idempotency/event identity, cursor/filter semantics and direct A0 persistence/state mappings.
- Added deterministic Redocly lint/bundle, generated TypeScript compatibility, stage isolation, docs/A0 mapping, secret/trusted-fact, surface/audience/role, security-scheme, error-family, registry-backing and schema-shape checks with adversarial probes.
- Closed aggregate findings by aligning `AuditFact` to A0, removing unowned `TrialPolicyDraft.createdAt`, adding the dedicated I4 conflict response, hardening fail-closed validators, enforcing A0 NOT NULL projections, replacing vacuous optionality assertions and reconciling registry/docs surfaces.
- Closed the PR finding by canonicalizing optional `ClaimResult.verifiedAt <- subject_number.subject_claim.verified_at`, removing the `acceptedAt` alias and pinning the mapping in schema, rules, generated-client assertions and docs/15.

## Verification

- `node --check api/openapi/contract-rules.mjs`: pass.
- `npm run check:s3`: pass, including Redocly validation, bundle, OpenAPI TypeScript generation, strict `tsc`, docs/A0 mapping and exact stage check; 49 operations, surface counts `18/21/6/3/1`.
- Two consecutive post-PR-fix generations produced identical declaration SHA-256 `5F922A763848EF97E4B13E164BE85CEE28DA69ED4F10F68D01B5D1A13FCD3C64`.
- `npm run check:s1`: expected negative stage only, exactly 35 later operations plus the corresponding count mismatches.
- `npm run check:s2`: expected negative stage only, exactly 17 S3 operations plus the corresponding count mismatches.
- Scoped whitespace, intended-path and empty-index checks passed before each accepted checkpoint. Unrelated prototype screenshots and `docs/reviews/plan-review-20261021-205118.md` never entered an A1 commit.
- Aggregate re-review `docs/reviews/code-review-20261024-230656.md`: pass-with-risks, no material finding.
- PR re-review `docs/reviews/code-review-20261025-001308.md`, SHA-256 `B8B13396DD6AE6141C2CEA6BB876CDBF0A191CAEF36264E85B62A012C3B8F902`: pass-with-risks, F-PR-01 fully closed and no new material finding.
- GitHub PR #2 head was confirmed at accepted PR review checkpoint `ad50d07f7663f79dc66edd0f64fc26d3baec6d63`, then the controller-only acceptance record was pushed at `0f8f62e02e45e58e470256ffe45dac0eb0500105`. The PR remains Draft. GitHub reports no configured checks; this is not treated as a passing CI signal.

These checks prove the static contract and deterministic tooling only. They do not prove a runtime service, migration, cloud resource, telephony/media behavior, provider integration or production readiness.

## Finding status

- Aggregate F-01 `AuditFact` A0 vocabulary/nullability: fixed.
- Aggregate F-02 unowned `TrialPolicyDraft.createdAt`: fixed.
- Aggregate F-03 I4 409 response family: fixed.
- Aggregate F-04 trust/security/schema validator gaps: fixed within the accepted bounded scope.
- Aggregate F-05 A0 requiredness/null guards: accepted portion fixed; share status-union narrowing rejected with reason.
- Aggregate F-06 vacuous P1 TypeScript optionality assertions: fixed.
- Aggregate F-07 docs/registry surface mismatch: fixed.
- Aggregate fix-2 read-filter false positive: fixed with the exact `GET`/`HEAD` + `query` + operation-local exact filter-digest predicate; adjacent fail-closed paths remain enforced.
- PR F-PR-01 claim verification timestamp naming/ownership: fixed and independently re-reviewed.
- No accepted finding remains unresolved.

## Residual risks and destinations

| Residual | Owner | Destination |
| --- | --- | --- |
| Java/Spring runtime controllers, authorization, transactions, persistence, concurrency, idempotency and response enforcement | Backend / Database / Security | Phase A implementation work units and integration/Testcontainers suites |
| Exact IVR session/heartbeat/end/media-upload/retry wire behavior, lost-response recovery and A0 task wording reconciliation | IVR / Telephony / Backend | Next `A2-production-ivr-protocol-spec` goal confirmation; Q15/Q26/Q28 |
| Key hierarchy, HMAC/encryption, service identity, replay windows, token/log redaction, object policy and numeric security values | Security / Platform / Backend | Security-design work unit; Q17/Q22/Q23 |
| Tencent Cloud region/sizing, COS/KMS/WORM, Redis/media gateway, backup/DR and capacity assumptions | Infrastructure / Platform / Security | Capacity/deployment and cloud implementation work units; Q2/Q11 |
| Payment/identity/provider payloads and operational policy values remain typed/deferred rather than invented | Product / Finance / Operations / Backend | Provider-specific implementation and Q1/Q4/Q5/Q6/Q7/Q8/Q10/Q18/Q19/Q20/Q24/Q25/Q27/Q28 decisions |
| Registry reverse surface membership, combined `sensitive-fields` rule and global A0 anchor ownership | API Governance | Optional validator-hardening maintenance before production CI lock-down |
| `AuditFact` cross-field null conditions and ClaimResult description-token semantics are not fully encoded as response conditionals | Backend / Audit / API Governance | Runtime response tests and later response-schema hardening |
| C15 mode-conditional extraction-code behavior and public-share media/admission atomicity | Sharing / Security / Backend | Sharing runtime and integration tests |
| Capability tokens in URL paths may be logged by edge/application infrastructure | Security / Platform | Edge and application logging/redaction configuration |
| Local validation ran under Node 20.20.1 although `package.json` requires Node >=22 | Platform / Tooling | Pin Node 22 LTS in CI and developer bootstrap; rerun the same suite |
| GitHub PR #2 has no configured CI checks | Platform / CI | Add repository CI for OpenAPI check/generation drift before production merge policy is enforced |

Every remaining risk has an owner and destination. None is an unclassified blocker for this static A1 work unit.

## Documentation and external state

- Canonical contract: `api/openapi/onecall-v1.openapi.yaml`.
- Contract/tooling guide: `docs/15-production-openapi-surface-spec.md`.
- Execution truth: `docs/gateflow/project-execution-control.md`.
- PR #2 contains the accepted contract, fix evidence, review evidence and controller history. It remains Draft for user-controlled merge/readiness handling.
- This work unit is not a GitHub issue. No issue link, closing keyword, issue comment or issue mutation is required or claimed.
- No merge, approval, mark-ready action, reviewer request, external comment or branch deletion was performed.

## Next entry point

After the user merges PR #2, update local `main` from `origin/main`. Then start a new Phaseflow goal confirmation for `A2-production-ivr-protocol-spec`, following architecture section 18 item 3 and using the completed A0 data model plus A1 OpenAPI contract as binding inputs. Do not begin A2 from this unmerged feature branch, and do not treat A1 goal confirmation as authorization for IVR protocol scope or unresolved production policy values.
