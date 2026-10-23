# A0 Production Data Model Schema Specification Final Closeout

- Work unit: `A0-production-data-model-schema-spec`
- Type: architecture-sensitive documentation deliverable
- Gate: `final closeout`
- Status: `final closeout pass`
- Date: 2026-10-23
- Draft PR: `https://github.com/zh94695424/one-call/pull/1`
- Accepted PR review checkpoint: `b7fe5d49cb017eb39e3026dc8b0c5f3abf61b7e1`
- Reviewed-content/controller push verified at: `4aa7f310882a1f8f32908c2ac34f6e18a45987b3`

## What changed

- Delivered a code-generation-ready PostgreSQL 16 data-model and initial Flyway schema specification for the accepted 44-table production baseline across identity, organization, IVR, activation/consent, entitlement/metering, catalog/order/payment, evidence, sharing, idempotency, Outbox and audit ownership boundaries.
- Made replay/conflict identities, immutable snapshots, state machines, total lock order, cross-owner collaborator contracts, privilege boundaries, migration waves and deferred production inputs explicit.
- Closed the PR review findings by restoring stable evidence `eventId` identity, defining the fixed non-lexical artifact rank, aligning durable acceptance metadata, publishing the IVR-owned reconciliation caller-lock collaborator, separating non-authoritative `UNKNOWN` terminal writes and defining absolute ticket expiry without choosing a Q22 value.

## Verification

- Independent PR re-review: `docs/reviews/pr-1-review-20261023-102439.md`, SHA256 `A68D6DD230C4477189296C1414A678E782F164A5C640960A3C961143817FF8E6`, document-only PASS with no material finding.
- Specification: SHA256 `5CC3A7FC4F1CD27D961459339CABE9E814B931A921F48DF145122449C9049303`, 885692 bytes / 4885 lines.
- Controller-executed structural script: exit `0`; 44 tables, 40 invariants, nine waves, slices 13/23/8, FK modes 74/3/2/2 = 81, lifecycle 28/16, sensitive inventory 28 and key sets 24.
- Controller-executed exact checker: exit `0`; nine frozen blocks, 44 dictionaries, 522 columns split S1=121/S2=307/S3=94, `Errors=[]`.
- Ten required embedded document blocks, UTF-8/whitespace checks and exact intended-path diff checks passed. These are document checks only.
- No PostgreSQL, Flyway, role/privilege, trigger, lock/concurrency, COS/KMS/WORM, Redis/media-gateway, IVR-line, payment-provider, Testcontainers, CI or production execution is claimed.

## Finding status

- PR-01 stable evidence source identity: `已修复`.
- PR-02 deterministic non-lexical artifact ordering: `已修复`.
- PR-03 durable aggregate checkpoint/current-gate ownership: `已修复`.
- PR-04 IVR-owned reconciliation caller lock: `已修复`.
- PR-05 non-authoritative `UNKNOWN` terminal shape: `已修复`.
- OQ-PR-01 absolute playback-ticket expiry formula: `已修复`.
- Independent re-review found no new material issue. Its parenthetical length nit is rejected: `evidence-solidified:` is 20 characters, plus a 64-character digest equals the specified 84.

## Residual risks and destinations

| Residual | Owner | Destination |
| --- | --- | --- |
| Executable migrations, roles/privileges, deferred constraints/triggers, lock/savepoint/concurrency behavior and the reconciliation collaborator role path | Backend / Database / Platform / Security / IVR | Migration and Testcontainers work units; Q17/Q23 security/deployment design |
| Stable upstream evidence-event allocation, replay/conflict behavior and artifact-order digest execution | Evidence / Sharing / Backend / IVR | S2/S3 integration tests and Q15/Q26 wire contracts |
| `UNKNOWN` authoritative convergence | IVR / Backend | IVR protocol and integration tests |
| Ticket TTL, skew and revocation-window values plus Redis clock/failure behavior | Security / Sharing / Backend | Q22 security design and Redis integration tests |
| COS object identity/hash, KMS/envelope, WORM/object lock, malware and duration validation | Infrastructure / Security / Backend | Q2/Q11 cloud/media implementation |
| SMS possession proof, OTP/KDF parameters, admission/rate-limit policy | Security / Product / Sharing | Q12 security/product design |
| Retention, erasure, backup deletion and legal hold | Legal / Security | Q3/Q11/Q13 retention design |
| IVR-local B-leg operation and lost-response recovery | Telephony / IVR | Q28 IVR protocol/integration |
| Prices, quantities, trial wording, due/grace, allocation and access/risk policy | Product / Finance / Operations / Risk / IVR | Q4/Q5/Q8/Q10/Q20/Q24/Q25/Q27 |
| PR title, author, draft flag and CI/check metadata unavailable | Controller | Official authenticated metadata API when available; not a passing signal |

Every remaining risk has an owner and destination. None is an unclassified blocker for this document-only work unit.

## Documentation and external state

- Design source remains `docs/12-technology-stack-and-system-architecture.md`; execution truth remains `docs/gateflow/project-execution-control.md`.
- PR #1 contains the fixed specification, PR fix evidence and independent review evidence. The branch and PR head were verified at `4aa7f310882a1f8f32908c2ac34f6e18a45987b3` before this controller-only closeout delivery.
- This work unit is not a GitHub issue. No issue link, closing keyword, issue comment or issue mutation is required or claimed.
- No merge, approval, mark-ready action, reviewer request, external comment or branch deletion was performed.

## Next entry point

After the user merges PR #1, update the target `main` branch from `origin/main`. Then start a new Phaseflow goal confirmation for `A1-production-openapi-surfaces-spec`, using the architecture section 18 ordering and this completed A0 specification as inputs. Do not begin A1 from the unmerged feature branch, and do not treat A0 goal confirmation as authorization for API scope.
