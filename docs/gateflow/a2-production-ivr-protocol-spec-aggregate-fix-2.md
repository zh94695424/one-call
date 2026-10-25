# A2 Aggregate Deepreview Fix 2

- Task label: `a2-ivr-aggregate-fix2-ds-20261025-01`
- Work unit: `A2-production-ivr-protocol-spec`
- Gate: aggregate `fix`
- Author model: DeepSeek (one-shot)
- Binding branch / HEAD: `codex/a2-production-ivr-protocol-spec` / `c1145bc5a84f4d2632f8a96621c6e4d231d74761`
- Binding base: `origin/main` `754b7dfc53c277e79c5d63e24d578a93fa557cda`
- Findings: F-AGG-R1, F-AGG-R2, F-AGG-R3 from `docs/reviews/code-review-20261025-102141.md`
- Correction 1 label: `a2-ivr-aggregate-fix2-correction1-ds-20261025-01`
- Status: correction 1 author-complete; executable controller revalidation pending (author environment denied Node/npm)

## Scope

Bounded correction only for the three accepted aggregate findings, plus correction 1 for the two controller-found stale fixture expectations. No new fixture ID, message, event, state, reason code, transition, identity, invariant, table, column, endpoint or public inventory item. Task-path/Direct-path behavior, the evidence boundary and all frozen counts are untouched.

## Exact changed files

| Path | Change |
| --- | --- |
| `protocol/ivr/validate.mjs` | R1 event-outbox absorbing guard; R2 backend receive-time threading and fail-closed deadline judgement |
| `docs/16-production-ivr-protocol-spec.md` | R2 explicit authority ownership in §6.1/§6.3; fixture-table descriptions updated for R1–R3 |
| `protocol/ivr/v1/fixtures/s2-04-terminal-unknown-reconciliation.json` | R3 reconciliation negatives and frozen-counter assertions |
| `protocol/ivr/v1/fixtures/s2-05-response-loss-retry.json` | R1 ACKED-then-response-loss absorption |
| `protocol/ivr/v1/fixtures/s2-07-authorization-expiry-not-started.json` | R2 server-time/`occurredAt` disagreement in both directions; R3 report-only deadline release |
| `protocol/ivr/v1/fixtures/s2-10-terminal-contradiction-quarantine.json` | Correction 1: R1 terminal-absorption expectations in `changed-event-conflict` |
| `protocol/ivr/v1/fixtures/s2-11-resolve-never-committed-not-started.json` | R1 PERMANENT_FAILURE-then-response-loss absorption |
| `protocol/ivr/v1/fixtures/s3-01-recording-validated.json` | R3 changed-`clientReference` completion replay; correction 1 fixes its cumulative recording-outbox counts |
| `docs/gateflow/a2-production-ivr-protocol-spec-aggregate-fix-2.md` | This artifact; correction 1 record |

`protocol/ivr/v1/ivr-protocol-v1.json` was not changed: its existing `eventOutbox` section states terminal classification but no demotion rule, and its B-leg guards do not contradict receive-time deadline authority. No model authority required correction.

Working-tree SHA-256 at correction-1 author completion (supersedes the fix-2 block):

```text
0e662b6929be52ec9f94f064ac49a373536e7909166dca9b5dd9d6fd2edbcc94  protocol/ivr/validate.mjs
70a675a65559d400478be516c0ba125116a8aada3e835f47c5adb95701642047  docs/16-production-ivr-protocol-spec.md
124fe698ceb7d32890cdfab3a4cc8918e2d8266ac91ccf064cac266bc5523050  protocol/ivr/v1/fixtures/s2-04-terminal-unknown-reconciliation.json
108b8f6c603566a5bd0fa1b2ebc9ac578d9781252af4fe6e9f0f1ce3f0f428e8  protocol/ivr/v1/fixtures/s2-05-response-loss-retry.json
f31ebc33f5c86ed1d6e479224eea579f4b3376e5155596e7e952e12c547b8c0a  protocol/ivr/v1/fixtures/s2-07-authorization-expiry-not-started.json
73e1248f3617685e4885c5de7db6c1c26798e8973dce2fe1e51faf3c3539a08d  protocol/ivr/v1/fixtures/s2-10-terminal-contradiction-quarantine.json
12b094fa0b13c4aff504725fca289c8e5b23dc7f66f3829ced6b1b6b89163aef  protocol/ivr/v1/fixtures/s2-11-resolve-never-committed-not-started.json
cbc8afeb0ca58145c0b18b377a269fd62aacda0c14712d7054d0c8a0fe99a7dd  protocol/ivr/v1/fixtures/s3-01-recording-validated.json
```

Correction 1 changed exactly two fixture paths relative to the fix-2 author state: `s2-10` is newly included (it was previously untouched by fix 2) and `s3-01` moved from `4905211c…` to `cbc8afeb…`. `validate.mjs`, `docs/16`, `s2-04`, `s2-05`, `s2-07` and `s2-11` are byte-identical to the fix-2 author state; those hashes were re-computed after correction 1.

## Correction 1 — two stale fixture expectations (controller-found)

The controller ran the correctly parameterized stage S3 validation on the fix-2 tree and reported **23/25** with exactly two failures:

1. `s2-10-terminal-contradiction-quarantine#4: persisted projection assertion mismatch`
2. `s3-01-recording-validated#6: fact counter assertion mismatch`

Stage S2 showed the same `s2-10` failure plus the eight expected future S3 fixtures. Both failures are stale fixture expectations, not machine defects: `validate.mjs` is read-only here and its accepted R1/R2 semantics are correct.

### s2-10 `changed-event-conflict`

The same `eventId` (`evt-s2-10a-terminal-2`) had already received a `200 QUARANTINED` result in step 3, so its event-outbox entry was already `ACKED`. The R1 absorbing guard at `validate.mjs:1365-1367` means the later conflicting retry still returns `409 EVENT_ID_CONFLICT` and increments `eventsConflicted`, but it cannot demote the terminal entry or increment `outboxPermanent`. Corrected expectations only:

- `persisted.outbox` entry `evt-s2-10a-terminal-2`: `PERMANENT_FAILURE` → `ACKED`.
- `counts.outboxPermanent`: `1` → `0` (asserted, not removed, so the non-increment stays non-vacuous).

Unchanged: `status: 409`, `code: EVENT_ID_CONFLICT`, `eventsConflicted: 1`, `eventsQuarantined: 1`, `terminalFactsAccepted: 1`, `releases: 1`, `usageFacts: 0`, the frozen call/terminal projection, and the first entry `evt-s2-10a-terminal-1` `ACKED`.

### s3-01 `complete-changed-client-reference`

Declaration and completion are distinct recording-outbox keys (`touchRecordingOutbox` at `validate.mjs:2003-2013`). After the changed-`clientReference` replay the cumulative counts are driven by two entries (declaration + completion), both ACKED, with one retry on the completion key. Corrected expectations only:

- `counts.recordingOutboxEntries`: `1` → `2`.
- `counts.recordingOutboxAcked`: `1` → `2`.
- `counts.recordingOutboxRetried`: stays `1`.

Unchanged: identity-exclusion replay response (`responseEqualsStep: complete`), `revision: 3`, `completedAt`, all media-object and validated fields, `recordingCompletionReplays: 1`, `recordingQuarantines: 0`, `recordingConflictFacts: 0`, and `evidenceRecordsCreated: 0` / `evidenceSolidified: 0`.

## F-AGG-R1 — event-outbox terminal absorption

`finishOutbox(entry, httpStatus)` now begins with the same absorbing guard already used by `finishRecordingOutbox`:

```js
if (entry.status === 'ACKED' || entry.status === 'PERMANENT_FAILURE') {
  return;
}
```

The guard runs before the `activeDelivery.responseLost` branch and before HTTP classification, so a later retry, lost-response simulation or conflicting result cannot demote a terminal entry or increment a second terminal counter.

Non-vacuous fixture proof:

- `s2-05#terminal-acked-response-lost`: after `SENT → ACKED`, a further `delivery.responseLost=true` retry of the same `eventId` returns `200` (first receipt), keeps `outbox[].status=ACKED`, and freezes `outboxEntries=1`, `outboxAcked=1`, `outboxPermanent=0`, `terminalFactsAccepted=1`, `settlements=0`, `releases=1`, `usageFacts=0`; only `outboxRetried` advances to 2 and `eventsReplayed` to 2.
- `s2-11#not-started-permanent-response-lost`: after `404 CALL_UNKNOWN → PERMANENT_FAILURE`, a `delivery.responseLost=true` retry keeps `PERMANENT_FAILURE`, `outboxEntries=1`, `outboxAcked=0`, `outboxPermanent=1`, `eventsReceived=0`, and advances only `outboxRetried=1`; no placeholder, release or usage appears.

## F-AGG-R2 — backend receive time owns deadline judgement

`handleEvent` now threads the server context into `processEvent(call, body, sequenceConflict, server)` and `applyOutboundNotStarted(call, body, server)`. On that evaluator path:

- The authorization deadline is compared against `server.receivedAt`, never `body.occurredAt`.
- `server.receivedAt` must be a second-precision UTC `date-time` string (`^YYYY-MM-DDTHH:MM:SSZ$`). Missing or invalid input throws before any state check, so it fails closed and can never authorize a release. There is no `occurredAt` fallback and no IVR-controlled branch.
- `occurredAt` keeps its frozen telephony role: accepted `endedAt` and request fingerprint unchanged.
- The receipt path is unchanged; only the deadline judgement was re-owned.

Documentation decision: `docs/16` §6.1 now states that authorization/deadline judgement belongs to the backend-observed receive time, and §6.3 now states that `OUTBOUND_NOT_STARTED` is judged from that time, fails closed on a missing/invalid observation, and keeps `occurredAt` as the accepted `ended_at`.

Non-vacuous fixture proof (second call inside `s2-07`, no new fixture ID):

- `s2-07#occurred-after-received-before`: `occurredAt=10:06` (after the 10:05 deadline) but `receivedAt=10:04` → `QUARANTINED`/`AUTHORIZATION_NOT_EXPIRED`; call stays `ACTIVE`, reservation `HELD`, no release.
- `s2-07#occurred-before-received-after`: `occurredAt=10:04` (before deadline) but `receivedAt=10:06` → `APPLIED` release with no preceding local `bleg.expire`; `endedAt` is the telephony `10:04` value, task `FAILED`, reservation `RELEASED`, 0 seconds, no usage.

## F-AGG-R3 — frozen-branch coverage

- Reconcile negatives in `s2-04`:
  - `#reconciled-missing-resolves` → `400 IVR_ARGUMENT_INVALID`; no event receipt (`eventsReceived` stays 8), call stays `UNKNOWN`, reservation `HELD`, outbox entry permanent.
  - `#reconciled-foreign-resolves` → `400 IVR_ARGUMENT_INVALID`; same frozen call state and event counters, second permanent outbox entry.
  - `#reconciled-not-unknown` → `200 QUARANTINED`/`NOT_UNKNOWN` after the call is `ENDED`; `terminalFactsAccepted=1`, `settlements=1`, `releases=0`, `usageFacts=1`, `reconciliations=1` stay frozen.
- Changed `clientReference` in `s3-01#complete-changed-client-reference`: completion replay with `ref-s3-01-changed` returns the first `VALIDATED` response, recording revision stays 3, and `mediaObjectsCreated=1`, `recordingValidations=1`, `evidenceRecordsCreated=0`, `evidenceSolidified=0`; only `recordingCompletionReplays=1` increments. The cumulative recording outbox reflects the separate declaration and completion entries: `recordingOutboxEntries=2`, `recordingOutboxAcked=2`, `recordingOutboxRetried=1` (corrected in correction 1).
- Report-only deadline expiry: the `s2-07#occurred-before-received-after` step is an applied `OUTBOUND_NOT_STARTED` with no preceding local `bleg.expire`, using backend receive time as the deadline authority.

## Validation

### Executed (author environment)

| Command | Result |
| --- | --- |
| `jq -e . protocol/ivr/v1/fixtures/s2-04-terminal-unknown-reconciliation.json` | exit 0, valid JSON |
| `jq empty` on `s2-05`, `s2-07`, `s2-11`, `s3-01` fixtures | all exit 0 |
| `jq -e .` on corrected `s2-10`, `s3-01` (correction 1) | exit 0, valid JSON |
| `sha256sum` over `validate.mjs`, `docs/16`, `s2-04`, `s2-05`, `s2-07`, `s2-10`, `s2-11`, `s3-01` (correction 1) | matches block above; only `s2-10` (new) and `s3-01` changed |
| `git diff --check -- <three correction paths>` (correction 1) | clean, no output |
| `file -i` on corrected fixtures/artifact | fixtures `application/json; charset=us-ascii`; artifact `text/plain; charset=utf-8` |
| Conflict-marker grep over `protocol/ivr/v1/fixtures` (correction 1) | none |
| `git status --porcelain -- <three correction paths>` | only the two fixtures `M` and this untracked artifact; no other path touched |
| `jq` model inventory | `messages=12 event_types=11 identities=22 invariants=15 reasons=21 actions=7 purposes=3 end_reasons=9 quarantine_reasons=10 event_registry_frozen=s2` |
| `jq -cS '.eventTypes' … \| tr -d '\n' \| sha256sum` | `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` (matches frozen digest) |
| Fixture inventory (Glob) | 25 IDs: 6 `s1-*`, 11 `s2-*`, 8 `s3-*` |
| `git diff --check` scoped to the eight changed source/fixture/spec paths | clean |
| `file -i` on changed files | UTF-8/ASCII, no encoding anomaly |
| Conflict-marker grep on `protocol/ivr` and `docs/16…` | none |
| `git status --porcelain` scoped paths | only the eight intended source/fixture/spec paths; no Git state change |

### Controller-found validation failure and correction-1 outcome

| Item | Result |
| --- | --- |
| Controller `node --check` and `api/openapi npm run check:s3` on the fix-2 tree | pass (binding evidence) |
| Controller stage S3, correctly parameterized | **23/25**, failing exactly `s2-10-terminal-contradiction-quarantine#4: persisted projection assertion mismatch` and `s3-01-recording-validated#6: fact counter assertion mismatch` |
| Controller stage S2 | same `s2-10` failure plus the eight expected future S3 fixtures |
| Correction 1 static checks (author) | JSON parse, hashes, `git diff --check`, encoding, conflict markers, scoped status all pass (table above) |
| Correction 1 executable suite (author) | **not executed** — Node/npm permission denied (table below). No `25/25` claim is made by the author |
| Post-correction expectation | Controller rerun should clear both failures and restore S3 to 25/25 with the same inventory/digest counters; only the controller can certify this |

### Attempted and permission-denied (not executed, not claimed)

| Command | Result |
| --- | --- |
| `node --check protocol/ivr/validate.mjs` | requires approval — denied (fix-2 and correction-1 rounds) |
| `npm --prefix api/openapi run check:s3` | requires approval — denied (fix-2 and correction-1 rounds) |
| `node protocol/ivr/validate.mjs --stage s3` with the standard model/fixtures/spec/A1/A0 arguments | requires approval — denied (fix-2 and correction-1 rounds) |
| `node protocol/ivr/validate.mjs --stage s2` with the same arguments | requires approval — denied (correction-1 round) |
| Stage S3 twice byte-identical, S1/S2/S3 dual runs, focused adversarial probes | not runnable without Node in this environment; not executed and not claimed |

Denial is environmental, not a product finding. Controller must rerun `check:s3`, `node --check`, stage S1/S2/S3 (S3 twice, byte-compare), and the focused probes on this tree before acceptance.

### Focused adversarial probes encoded for controller rerun

1. Event outbox: `s2-05#terminal-acked-response-lost` (ACKED) and `s2-11#not-started-permanent-response-lost` (PERMANENT_FAILURE).
2. Server time vs `occurredAt`: `s2-07#occurred-after-received-before` and `s2-07#occurred-before-received-after`.
3. Reconciliation negatives: `s2-04#reconciled-missing-resolves`, `#reconciled-foreign-resolves`, `#reconciled-not-unknown`.
4. Changed `clientReference`: `s3-01#complete-changed-client-reference`.
5. Report-only deadline expiry: `s2-07#occurred-before-received-after`.
6. A temporary probe (not a fixture) should call `ingestIvrCallEvent` with `OUTBOUND_NOT_STARTED` and no `server.receivedAt` (and with a malformed value) against an `ACTIVE` call and assert the evaluator throws without releasing; this fail-closed branch is code-verified but not executable here.
7. Correction-1 regression checks: `s2-10#changed-event-conflict` must return `409 EVENT_ID_CONFLICT` with `evt-s2-10a-terminal-2` still `ACKED`, `outboxPermanent=0`, and frozen terminal/release/usage counters; `s3-01#complete-changed-client-reference` must keep `recordingOutboxEntries=2`, `recordingOutboxAcked=2`, `recordingOutboxRetried=1` with the first `VALIDATED` result and unchanged revision.

## Inventory invariants (static)

- 12 messages, 11 event types, 22 identities, 15 invariants, 21 reasons, 7 actions, 3 purposes unchanged.
- Event registry digest unchanged: `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`.
- 25 fixture IDs with slice counts `6/11/8` and cumulative `6/17/25`, negative `0/3/3`; no fixture ID added.
- Five recording transitions, 13 B-leg phases / 26 transitions, four outbox states, 21 reasons, 7 actions, 3 purposes, six A1 IVR operations and the A0 44 catalog tables untouched.
- No new endpoint, schema, message, event, state, reason code, transition, identity row, invariant, table, column or fixture ID.

## Residual risks

| ID | Risk | Owner / destination |
| --- | --- | --- |
| R-FIX2-ENV | Node/npm denied in the author environment; executable suite and probes were not rerun here | Controller revalidation on this tree |
| R-FIX2-THROW | Missing/invalid `server.receivedAt` fails closed by throwing; no fixture can assert a throw, so the probe must stay controller-run | Controller focused probe |
| R-FIX2-TIME | Receive-time format is constrained to second-precision `...Z`; fractional/precision variants are intentionally rejected as invalid and fail closed | Evaluator contract; documented in §6.1/§6.3 |
| R-AGG-SUBSET | Pre-existing S2/S3 `persisted`/`counts` subset assertion limitation | Carried forward, pre-existing |
| R-A2-* | 13 classified residuals unchanged | Carried forward per `docs/16` §11 |

No new residual beyond the environmental/probe items above. The first two close on controller execution.

## Completion status and stop state

Author-complete for the bounded fix and for correction 1. Correction 1 wrote exactly three paths: the two corrected fixtures and this artifact. No staging, commit, push, PR/issue change, merge, re-review, deepreview acceptance or draft-PR work was performed; no self-review or later gate was entered. Controller, validator, `docs/16`, model, plans, prior reviews/fix artifacts, A0/A1/OpenAPI, prototypes, screenshots and Git state were left untouched. Stop after this bounded correction; next Gateflow step is controller revalidation, not review.
