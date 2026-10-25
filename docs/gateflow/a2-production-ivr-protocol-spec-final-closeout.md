# A2 Production IVR Protocol Specification Final Closeout

- Work unit: `A2-production-ivr-protocol-spec`
- Type: architecture-sensitive protocol specification deliverable
- Gate: `final closeout`
- Status: `final closeout pass`
- Date: 2026-10-25
- Draft PR: `https://github.com/zh94695424/one-call/pull/4`
- Accepted PR review checkpoint: `2ccec09762d0fff754cb89968da838480fa69969`
- Reviewed-content push verified at: `2ccec09762d0fff754cb89968da838480fa69969`

## What changed

- Delivered a versioned, code-generation-ready protocol between the self-built IVR and ONECALL backend from inbound resolve through activation re-decision, B-leg execution/recovery, event ingestion, terminal settlement, recording declaration and media-upload completion.
- Defined one shared action-selection contract for initial resolve and post-activation decisions. Task-backed calls automatically dial the frozen callee without in-call callee confirmation; only the no-task direct path uses DTMF collection/confirmation.
- Preserved the approved trial and commerce boundary: key `1` may grant one trial before platform login or real-name completion; purchase remains guidance to Mini Program/Web; operator/PBX systems are not participants.
- Added one deterministic machine model, a zero-third-party-dependency validator and 25 cumulative fixtures covering normal, duplicate, delayed, reordered, conflicting, lost-response, restart, expiry, reconciliation and recording/media paths.
- Closed event-outbox terminal absorption, backend-received-time authorization expiry, fail-closed missing/invalid receive time, reconciliation negatives, `clientReference` identity exclusion and report-only expiry gaps without adding endpoints, persistence states or runtime claims.

## Verification

- `node --check protocol/ivr/validate.mjs`: pass.
- `api/openapi` `npm run check:s3`: pass; 49 operations, 6 IVR operations, A0 catalog 44 and zero OpenAPI/A0 drift.
- Fully parameterized IVR stage-S3 validation: pass with 12 messages, 11 events, 22 identities, 25/25 fixtures, 3 negative fixtures, slice counts `6+11+8`, 15 invariants and zero drift/doc/stage/fixture counters.
- Frozen event registry SHA-256: `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`.
- Earlier acceptance evidence includes byte-identical dual S3 output at SHA-256 `4A2FD84B071BBD6990849C23DDDF9EDF5B353724A445470263909BD47533C70E`, S2 acceptance of all 17 S1/S2 fixtures with rejection of only 8 future S3 fixtures, and 10 focused mutation probes.
- Aggregate re-review `docs/reviews/code-review-20261025-105904.md`: pass, all accepted aggregate findings fixed, no new finding.
- PR review `docs/reviews/pr-4-review-20261025-111611.md`, SHA-256 `B5CDA89DED82474F7D07F794421AD420061492AFED29AC81CB4073266C61C6D0`: pass, zero material findings.
- GitHub PR #4 head, origin branch and local branch were all verified at `2ccec09762d0fff754cb89968da838480fa69969`. The PR remains Draft/Open. GitHub reports no configured checks; this is not treated as a passing CI signal.

These checks prove the static protocol contract and deterministic evaluator only. They do not prove a Java/backend runtime, PostgreSQL concurrency, SIP/line/CDR behavior, real COS observation, evidence creation, security signing or production capacity.

## Finding status

- Slice S1 accepted validator/report finding: fixed; rejected A0/A1 registry-escape claim remains rejected with direct exact-registry evidence.
- Slice S2 accepted fail-closed, transition-pair and documentation findings: fixed.
- Slice S3 F1-F4: fixed; F5 evidence-boundary redesign rejected with zero evidence counters and no evidence mutation path.
- Aggregate F-AGG-1 incorrect S3 checkpoint binding: fixed.
- Aggregate R1 event-outbox terminal absorption: fixed.
- Aggregate R2 backend `receivedAt` deadline ownership and fail-closed invalid/missing time: fixed.
- Aggregate R3 reconciliation, `clientReference` and report-only expiry coverage: fixed.
- PR review: no new material finding.
- No accepted finding remains unresolved.

## Residual risks and destinations

| Residual | Owner | Destination |
| --- | --- | --- |
| `R-A2-A0-TASKID-TYPE` A0 presentation mismatch | Data model / API governance | A0 documentation clarification |
| `R-A2-DECLARE-REPLAY` declaration projection boundary | API / Evidence | API/evidence projection review |
| `R-A2-SESSION-ABANDONED` has no v1 wire trigger | IVR / Backend / Product | IVR/backend product polish |
| `R-A2-HEARTBEAT-VOLUME` durable heartbeat volume/retention | Telephony / Operations | Q8/Q13 runtime and operations design |
| `R-A2-UPLOAD-OBSERVE` real object-storage observation trigger | IVR / Evidence / Infrastructure | Q7/Q8 storage integration |
| `R-A2-EVIDENCE-HANDOFF` evidence creation/solidification ownership | Evidence / Backend | A0 §9.28/§10.24 implementation |
| `R-A2-TASK-TERMINAL` terminal task mapping not runtime-proven | IVR / Backend | Integration and concurrency tests |
| `R-A2-RECONCILIATION-AUTHORITY` depends on authoritative CDR/line records | Telephony / IVR | Q28 line/CDR integration |
| `R-A2-CALLID-UNIQUENESS` is a cross-node runtime guarantee | IVR gateway | Q28 gateway implementation |
| `R-A2-MULTI-RECORDING` A0 may allow multiple recordings per call | Evidence / Product | Evidence policy decision |
| `R-A2-NO-RETRY-CALLEE` one B-leg per call forbids callee retry | Product / IVR | Explicit future product decision if changed |
| `R-A2-PROMPT-DTMF` prompt copy and DTMF pacing not frozen | Product / Legal / IVR | Q10 prompt/consent design |
| `R-A2-SIGNATURE` signature string, nonce and time window not frozen | Security / Backend | Next security-design work unit; Q7/Q15/Q25 |
| Evaluator subset assertions, non-deadline receipt fallback and second-precision UTC input | Protocol tooling / Backend | Runtime conformance tests; preserve fail-closed deadline behavior |
| PostgreSQL task/reservation concurrency, IVR telephony execution, COS/media observation and evidence lifecycle are not executable here | Backend / Database / IVR / Telephony / Infrastructure / Evidence | Production implementation and integration work units |
| GitHub PR #4 has no configured CI checks | Platform / CI | Add repository CI for OpenAPI and IVR protocol validation before production merge policy is enforced |

Every remaining risk has an owner and destination. None is an unclassified blocker for this static A2 work unit.

## Documentation and external state

- Canonical protocol: `docs/16-production-ivr-protocol-spec.md`.
- Machine model: `protocol/ivr/v1/ivr-protocol-v1.json`.
- Deterministic evaluator and fixtures: `protocol/ivr/validate.mjs` and `protocol/ivr/v1/fixtures/`.
- Execution truth: `docs/gateflow/project-execution-control.md`.
- PR #4 contains the accepted protocol, validation fixtures, fix/review evidence and controller history. It remains Draft for user-controlled readiness and merge handling.
- This work unit is not a GitHub issue. No issue link, closing keyword, issue comment or issue mutation is required or claimed.
- No merge, approval, mark-ready action, reviewer request, external comment or branch deletion was performed.

## Next entry point

After the user merges PR #4, update local `main` from `origin/main`. Then start a new Phaseflow goal confirmation for `A3-production-security-design-spec`, following architecture section 18 item 4: key hierarchy, field encryption/HMAC, service identities, object-storage policy and audit access. Use the completed A0 data model, A1 OpenAPI contract and A2 IVR protocol as binding inputs. Do not begin A3 from this unmerged feature branch, and do not inherit A2 goal authorization for security values, provider choices or production numeric policy.
