# A3 Production Security Design Specification Final Closeout

- Work unit: `A3-production-security-design-spec`
- Type: architecture-sensitive security specification deliverable
- Gate: `final closeout`
- Status: `final closeout pass`
- Date: 2026-10-26
- Draft PR: `https://github.com/zh94695424/one-call/pull/5`
- Accepted PR review checkpoint: `25f4a0369f731b5f99fcb6a859cd7ee9bcdfcb08`
- Reviewed-content push verified at: `25f4a0369f731b5f99fcb6a859cd7ee9bcdfcb08`

## What changed

- Delivered a code-generation-ready production security specification for purpose-separated envelope encryption and HMAC lookup, key lifecycle/recovery, five authentication domains, service identities/audiences, IVR/provider signing and replay protection, short-lived media authority, private Tencent COS policy, audit/high-risk controls and deterministic pre-emit redaction.
- Added the machine-readable security model, zero-third-party-dependency staged validator and 23 deterministic fixtures across S1/S2/S3.
- Closed the final PR-review defects by removing construction-guaranteed stage-summary assertions, scoping provider nonce uniqueness to `(serviceIdentity, nonce)`, correcting invalid Q15 references to the existing Q7/Q6 inputs, and freezing the exact masked-display safe classes `PHONE`, `IDENTITY_NUMBER`, `LEGAL_NAME` with fail-closed handling for every other `*Masked` key.
- Preserved the accepted A0/A1/A2 contracts and static-spec boundary. No Java/runtime service, cloud resource, real credential, UI/prototype, provider integration, numeric production policy or production-readiness claim was added.

## Verification

- `node --check security/validate.mjs`: pass.
- `api/openapi` `npm run check:s3`: pass; 49 operations with surface counts `18/21/6/3/1` and zero mapping drift.
- Accepted A2 stage-S3 validation: pass with 12 messages, 11 event types, 22 identities, 25/25 fixtures, three negatives and unchanged digest `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`.
- Security S1: pass with 20 key classes, 28 data classes, 23 deferred inputs, six fixtures and two negatives.
- Security S2: pass with 5 trust surfaces, 5 auth domains, 7 boundaries, 6 service identities, 10 credential classes, 2 signature profiles, 15 fixtures and seven negatives.
- Security S3: two byte-identical passes at SHA-256 `F08B5D24F8C6E801761CDD903021952EF3534816AD57D8DBB6539460C1F393B3`; exact counts `2/39/39/2/37/10/10`, 21 invariants, 23 fixtures, ten negatives and `6+9+8`; every drift/doc/token/stage/fixture counter is zero.
- Removing `maskedDisplaySafeClasses` or broadening it with `TOKEN` is rejected by the exact projection guard.
- Aggregate re-review `docs/reviews/code-review-20261026-022005.md`: pass; all accepted aggregate findings fixed, no new material finding.
- PR re-review `docs/reviews/pr-5-review-20261026-031124.md`, SHA-256 `74EA61112E01AEFB1DFDD86EC7F241E77C8B034AF8375CFBE4692A135B27324B`: pass; `F-PR5-01..04` fixed, all prior findings remain fixed, zero new material findings.
- GitHub branch and Draft PR #5 head were verified at `25f4a0369f731b5f99fcb6a859cd7ee9bcdfcb08`. The PR remains OPEN/Draft and reports no configured checks; CI absence is not a green signal.

These checks prove the static security contract and deterministic reference evaluator only. They do not prove live Tencent KMS/Secret Manager/CAM/COS, Redis/PostgreSQL, mTLS/IVR/provider, media-gateway, logging/analytics or production-capacity behavior.

## Finding status

- Plan `F-01..F-09`: fixed.
- S1 `F-S1-C1-01` and `F-S1-R1-01`: fixed.
- S2 `F-S2-R1-01` and `F-S2-R1-02`: fixed.
- S3 `F-S3-C1-01`, `F-S3-R1-01` and `F-S3-R1-02`: fixed.
- Aggregate `F-AGG-01..04` and `F-AGG-C1-01`: fixed.
- PR review `F-PR5-01..04`: fixed.
- PR re-review: no new material finding. No accepted finding remains unresolved.

## Residual risks and destinations

| Residual | Owner | Destination |
| --- | --- | --- |
| `R-A3-HMAC-ROTATION-REPLAY` | Security / IVR / Backend | Integration plus Q3/Q25 runtime compatibility |
| `R-A3-KMS-CAPABILITY` | Infrastructure / Security | Q2/CV capability validation |
| `R-A3-COS-CAPABILITY` | Infrastructure / Security | Storage/capacity validation |
| `R-A3-KEY-DESTRUCTION` | Security / Infrastructure | Incident/recovery policy |
| `R-A3-LEGAL-RETENTION` | Legal / Security | Launch review |
| `R-A3-NO-WORM` | Infrastructure / Security | Storage policy and Q11 validation |
| `R-A3-REGION-SKU` | Infrastructure / Security | Capacity/deployment work unit |
| `R-A3-A2-AUTH-OUTBOX` | IVR / Backend | A2/IVR integration review |
| `R-A3-MTLS-CAPABILITY` | Telephony / IVR / Security | Line integration and Q7 |
| `R-A3-PROVIDER-VERIFY` | Payments / Backend | Payment adapter and Q6 |
| `R-A3-SESSION-LIFECYCLE` | Identity / Backend | Authentication design |
| `R-A3-NONCE-STORE` | Security / Backend | Production readiness and Q25 |
| `R-A3-A1-EMPLOYEE-REASON-PROJECTION` | Product / Operations / API governance | Future A1/UI refinement |
| `R-A3-A1-UPLOAD-CREDENTIAL-SHAPE` | Security / Backend / Evidence | API/evidence integration review |
| `R-A3-MEDIA-GATEWAY-TRANSPORT` | Infrastructure / Security | Media-gateway deployment/storage capacity |
| `R-A3-REDACTION-RUNTIME` | Platform / Security / Observability | Runtime logging review |
| `R-A3-AUDIT-VOLUME` | Operations / Backend | Runbook/observability and Q13 |
| `R-A3-SUPPORT-VIEW` | Product / Security / Operations | Future break-glass work unit |
| Cross-identity provider nonce discriminator is absent because only one provider identity exists | Security / Payments | Add a fixture only if a second provider service identity is introduced |
| Evaluator has no live IVR redial/second-reservation/settlement/re-resolve or evidence-solidification path | Security / Backend | Add live observables if those paths enter a future evaluator |
| GitHub PR #5 has no configured CI checks | Platform / CI | Add repository CI before production merge policy is enforced |

Every remaining risk has an owner and destination. None is an unclassified blocker for this static A3 work unit.

## Documentation and external state

- Canonical specification: `docs/17-production-security-design-spec.md`.
- Machine model: `security/v1/security-design-v1.json`.
- Deterministic evaluator and fixtures: `security/validate.mjs` and `security/v1/fixtures/`.
- Execution truth: `docs/gateflow/project-execution-control.md`.
- Draft PR #5 contains the accepted specification, fixtures, fixes and review evidence. HTTPS Git transport was unavailable during the final push; the same non-force push succeeded through the repository's existing authenticated GitHub SSH-over-443 configuration. The remote and PR heads were then independently verified.
- This work unit is not a GitHub issue. No issue link, closing keyword, issue closeout comment or issue mutation is required or claimed.
- No merge, approval, mark-ready action, reviewer request, external comment or branch deletion was performed.

## Next entry point

After the user merges Draft PR #5, update local `main` from `origin/main`. Then start a new Phaseflow goal confirmation for `A4-production-capacity-deployment-spec`, following architecture section 18 item 5: use approved line concurrency and data-retention inputs to calculate node count, bandwidth, storage and backup requirements. Use accepted A0/A1/A2/A3 contracts as binding inputs. Do not begin A4 from this unmerged A3 branch, and do not inherit A3 authorization to invent concurrency, retention, region, SKU, pricing or capacity values.
