# A2 Production IVR Protocol Spec — S1 implementation report

> Task label: `a2-ivr-s1-impl-ds-20261025-01`
>
> Work unit: `A2-production-ivr-protocol-spec`
>
> Gate: `implementation`, slice S1 only
>
> Accepted plan commit: `8efb906f00d47ae2c203269c5382ab8e2079ca1a`
>
> Binding plan: `docs/gateflow/a2-production-ivr-protocol-spec-plan.md` (§3, §4.1-§4.4, §4.11-§4.13, §5, §6.3, §7, §8.2)
>
> Binding goal/constraints: A2 block in `docs/gateflow/project-execution-control.md`
>
> Runtime observed: Node `v22.23.4`. npm was not executed; its version was not obtained because the permission mode rejected npm execution.
>
> Completion status: **author-complete; runtime validator execution blocked by harness permission mode**. No S2/S3 semantics, staging, commit, push, PR or review occurred.

## 1. Scope delivered

S1 delivers the canonical authorization-truth foundation:

- protocol version `1.0`, grammar/compatibility and fail-closed rules;
- the exact 12-message registry over the six accepted A1 IVR operations;
- the exact 22-row identity/ownership/precondition/replay/A0-A1 matrix;
- one unified `IvrDecision` action-selection contract for resolve, activation next action and direct authorization;
- task, direct and activation action invariants, purpose chains, decision replay/conflict and exact A1 error mapping;
- S1-approved task/session/decision/reservation transitions only;
- 15 invariant IDs registered without implementing S2/S3 behavior;
- the exact 11-event identity-only registry with `eventType,payloadState` and every sentinel `DEFERRED_S2`, plus adjacent non-confusion text;
- deterministic stage-s1 model/doc/A1/A0 drift validation and exactly six S1 fixtures.

The user-visible rules are preserved explicitly in the specification and fixtures:

- A task-backed call auto-dials the frozen task callee; there is no callee confirmation step.
- Only the no-task direct path collects and confirms the callee through DTMF.
- Key `1` may grant exactly one first trial to an unregistered/unclaimed/unverified caller without creating a login account or real-name record.
- IVR purchase behavior is guidance to the Mini Program or Web only; no order or payment is created.
- No operator/PBX integration is part of the protocol.

## 2. Changed files (exact)

1. `docs/16-production-ivr-protocol-spec.md` (new)
2. `protocol/ivr/v1/ivr-protocol-v1.json` (new)
3. `protocol/ivr/v1/fixtures/s1-01-task-path-normal.json` (new)
4. `protocol/ivr/v1/fixtures/s1-02-direct-path-normal.json` (new)
5. `protocol/ivr/v1/fixtures/s1-03-activation-redecision.json` (new)
6. `protocol/ivr/v1/fixtures/s1-04-decision-exact-replay.json` (new)
7. `protocol/ivr/v1/fixtures/s1-05-decision-semantic-conflict.json` (new)
8. `protocol/ivr/v1/fixtures/s1-06-version-unsupported.json` (new)
9. `protocol/ivr/validate.mjs` (new)
10. `docs/gateflow/a2-production-ivr-protocol-spec-s1-implementation.md` (new, this report)

No other repository path was modified by this task. Pre-existing modified prototypes/screenshots, `logo/preview.html`, the controller document and the untracked historical review remain untouched.

## 3. Exact inventories

| Inventory | S1 value |
| --- | --- |
| Protocol version | `1.0` |
| Supported versions | `["1.0"]` |
| Messages | 12 |
| Event types | 11, identity-only, every `payloadState=DEFERRED_S2` |
| Identity rows | 22 |
| Invariant IDs | `INV-A2-01` through `INV-A2-15` |
| S1 fixtures | 6 (`s1-01` through `s1-06`) |
| Cumulative fixture count | 6 |
| Cumulative marked negatives | 0 |
| A1 operations | 49 |
| A1 IVR operations | 6 |
| A0 catalog tables | 44 |
| A0 named CHECK registries checked | 14 |

Message IDs in fixed order:

`resolve.request`, `resolve.decision`, `activation-confirm.request`, `activation-confirm.decision`, `direct-authorization.request`, `direct-authorization.decision`, `call-event.request`, `call-event.receipt`, `recording-declare.request`, `recording-declare.result`, `recording-complete.request`, `recording-complete.result`.

Event types in fixed order:

`CALLER_LEG_ANSWERED`, `CALLEE_LEG_RINGING`, `CALLEE_LEG_ANSWERED`, `BRIDGE_STARTED`, `RECORDING_STARTED`, `RECORDING_ENDED`, `CALL_TERMINATED`, `CALL_TERMINATION_UNKNOWN`, `CALL_TERMINAL_RECONCILED`, `OUTBOUND_NOT_STARTED`, `CALL_HEARTBEAT`.

Identity rows in fixed order:

`callId`, `sessionId`, `decisionId`, `decision fingerprint`, `taskId`, `reservationId`, `action / executable intent`, `eventId`, `sequenceNo`, `legId`, `B-leg operation`, `recordingId`, `upload credential`, `upload expiry`, `terminal fact`, `heartbeat`, `bridge fact`, `recording start/end`, `media object`, `evidence record`, `settlement fact`, `IVR event outbox`.

Fixture IDs and scenarios:

| Fixture | Scenario | Assertions |
| --- | --- | --- |
| `s1-01-task-path-normal` | Resolve to `DIAL_TASK_CALLEE` | 200; task `CLAIMED`; reservation `HELD`; frozen decision; task auto-dial; no callee confirmation |
| `s1-02-direct-path-normal` | `ENTER_CALLEE` then direct success | `action=null`; reservation present; no task/task fields; direct DTMF confirmation counted once |
| `s1-03-activation-redecision` | `GUIDE_ACTIVATION`, key `1`, then `DIAL_TASK_CALLEE` | session `CONFIRMED`; one trial; resolve decision unchanged; shared decision shape; no login/real-name creation |
| `s1-04-decision-exact-replay` | Resolve, confirm and direct retries | first frozen responses; no second claim, reservation or trial |
| `s1-05-decision-semantic-conflict` | Changed caller, access, prompt and direct callee | 409 `SESSION_IDENTITY_MISMATCH`; persisted facts/counts unchanged |
| `s1-06-version-unsupported` | `2.0` and malformed version | 400 `IVR_ARGUMENT_INVALID`; no call/session; zero fact counts |

## 4. Protocol and validation decisions

- `protocolVersion` is a resolve-only request field. Unknown/malformed versions fail with 400 before any session/call/task/reservation write. The version is frozen on the call; no mid-call upgrade or downgrade.
- The backend action selector is the sole routing authority. The IVR executes the returned action and never derives caller state, selects another callee or falls back to a task.
- Task-backed success atomically pairs the `PENDING` to `CLAIMED` task update, one `HELD` reservation and one frozen decision. Replay returns the first decision and never renews authorization.
- Direct success is `purpose=DIRECT_AUTHORIZATION`, `action=null`, `reasonCode=DIRECT_AUTHORIZED`, with a reservation and no task fields.
- Activation confirmation requires a prior `GUIDE_ACTIVATION` resolve, key `1`, current policy/prompt/consent and the same confirmed subject. It grants at most one first trial and creates no login/real-name identity.
- Decision fingerprints exclude transport `eventId` for confirm/direct, so a regenerated event ID with identical semantics replays the first decision; any semantic change is `409 SESSION_IDENTITY_MISMATCH`.
- S1 freezes only task claim, session confirmation, decision freeze and initial `HELD` reservation transitions. Call/leg/terminal/reconciliation/settlement and recording/media behavior are explicitly deferred.
- The S1 event block contains identities only. The validator checks the exact two-column form and sentinel; S2 full payload/envelope/state data is not present or implied.
- The validator uses a deterministic in-memory `ProtocolMachine` with fixed fixture inputs, canonical JSON sorting and SHA-256 fingerprints. It has no current-time, network, random, database, line or cloud dependency. It computes action, claim, reservation, trial, replay, conflict and fixture counts and compares the result with `expected`; it does not echo `expected`.
- The validator enforces the stage-s1 top-level section allowlist, exact marker headers/rows/order/duplicates, A1 operation/schema/error drift, A0 catalog/CHECK/column drift, exact fixture ID sets, and fixture/server/expected shapes.
- No new A1 path, operation, Problem code, A0 table, column, state or numeric policy is introduced. No deferred Q-value is invented.

## 5. Validation

### 5.1 Commands attempted

The plan commands were attempted exactly (only shell path syntax unchanged):

```bash
cd api/openapi && npm ci && npm run check:s3
node protocol/ivr/validate.mjs --stage s1 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
```

Both were **not executed**: the harness permission mode returned `This command requires approval` for `npm` and for any `node <script>` invocation, including a subagent attempt. No stdout, stderr or exit code was produced. This report does not claim Node 22 command execution.

Static checks that did run:

| Check | Command / method | Result |
| --- | --- | --- |
| Model JSON syntax | `jq empty protocol/ivr/v1/ivr-protocol-v1.json` | exit 0 |
| Fixture JSON syntax | `jq empty` on all six fixtures | exit 0 |
| Model inventory | `jq` counts and key sets | 12 messages, 11 event rows with exactly `eventType`/`payloadState`, 22 identities, 15 invariants |
| Identity precondition/replay fields | `jq` presence check | 0 missing |
| A1 operation inventory | bundled OpenAPI inspection | 22 GET + 25 POST + 2 PUT = 49; 6 IVR operations |
| A1 key enums/required fields | bundled OpenAPI inspection | `IvrDecision`, action, purpose, reason, callerState and taskMatch match the model |
| A1 error arrays | bundled OpenAPI inspection | exact arrays match the model for all six IVR operations |
| A1 payload opacity | bundle inspection | `IvrEventPayload` is opaque and open |
| A0 catalog | document parse | 44 tables |
| A0 named CHECKs | document grep | all 14 exact registry lines present |
| A0 column refs | manual/jq comparison | all referenced tables/columns exist |
| Doc marker row counts | grep | 12 message rows, 11 event sentinel rows, 22 machine identity rows |
| UTF-8 | `file -bi` | `text/plain; charset=utf-8` for the spec/validator; `application/json; charset=utf-8` for the model |
| Conflict markers | grep `^(<<<<<<<\|=======\|>>>>>>>)` | 0 |
| Trailing whitespace | grep `[ \t]+$` | 0 |
| Whitespace diff | `git diff --check -- docs/16-production-ivr-protocol-spec.md protocol/ivr` | exit 0 |
| Scope | `git status --porcelain` | only the ten S1 paths are task-owned; pre-existing dirty paths preserved |

### 5.2 Expected validator output (not observed)

If the permission blocker is removed, the accepted S1 output is expected to be:

```text
stage=s1 messages=12 event_types=11 event_map_form=identity-only identities=22 fixtures=6 passed=6 failed=0 negative=0 slice_fixtures=6
a1_operations=49 ivr_operations=6 a1_drift=0
a0_tables=44 a0_status_drift=0 a0_registry_drift=0 a0_column_drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
invariants=15
PASS protocol-validation (s1)
```

This is an expected-output statement, not an execution claim. The implementation is author-complete, but the required executable validation evidence is missing until the controller runs the two commands in an environment that permits Node/npm.

## 6. A1/A0 drift evidence from static inspection

- A1 operation split: `a1_operations=49`, `ivr_operations=6`.
- A0 catalog: `a0_tables=44`.
- The six IVR operation IDs, methods, paths, request schemas and success response schemas match the model.
- The exact per-operation error arrays match A1 for resolve, confirm, direct, event, declare and complete.
- `IvrDecision` required fields and the action/purpose/reason/callerState/taskMatch enums match A1.
- `task.calleeNumber` and `authorization.calleeNumber` remain internal payload fields.
- `IvrEventPayload` remains opaque; no A1 envelope change is claimed.
- The 14 A0 named CHECK registries are bound in the model and compared by the validator.
- Every model A0 column reference exists in A0 §4; all decision fields exist in A0 §10.14.
- No new A1 endpoint, Problem code, A0 table, column, state or transaction is introduced.

## 7. Negative and determinism evidence

- S1 has no marked negative fixtures; cumulative marked negatives are 0.
- Replay fixtures use a different server-supplied decision/reservation identity on the retry to prove the machine returns the first frozen result rather than trusting the new identity.
- Semantic-conflict fixtures change caller, access number, direct callee and activation prompt; all return 409 with unchanged snapshot/counts.
- The version fixture proves `2.0` and malformed `1` fail before any call/session row exists.
- The duplicate counts are asserted explicitly: second claim, reservation and trial remain absent on exact replay.
- The validator sorts object keys for canonical comparison and uses fixed fixture times/identities. It has no `Date`, `Math.random`, network or filesystem-time dependency. Two runs should therefore produce identical stdout and fixture-result hashes, but this was not executable in this environment.

## 8. Prototype alignment

Aligned / pass:

- Task-backed calls auto-dial the frozen callee and never require callee confirmation.
- No-task calls use the direct DTMF collection/confirmation path only.
- Key `1` activation grants at most one trial to an unregistered caller without login or real-name creation.
- Purchase guidance is Mini Program/Web only; the IVR creates no order and takes no payment.
- No operator/PBX/PBX-adjacent integration is introduced.

## 9. Residual risks and deferrals

| Area | Status |
| --- | --- |
| S2 event payload/envelope/state/terminal/reconciliation/B-leg/outbox semantics | Deferred to S2; only event identities are registered |
| S3 recording/media/evidence lifecycle | Deferred to S3 |
| Real mTLS/signature/nonce/time window/clock sync | Deferred Q7/Q15/Q25 |
| SIP/line/CDR, B-leg dispatch, bridge, recording cutoff | Not runtime-proven; Q7/Q28 |
| PostgreSQL concurrency, locks, unique constraints and transaction rollback | Not runtime-proven |
| COS upload observation, media validation and evidence solidification | Not runtime-proven; S3/evidence owner |
| `R-A2-A0-TASKID-TYPE` | Classified; S1 follows normative A0 §4.23/A1 text 1-64 |
| `R-A2-DECLARE-REPLAY`, `R-A2-SESSION-ABANDONED`, `R-A2-UPLOAD-OBSERVE`, `R-A2-EVIDENCE-HANDOFF` | Classified with their plan owners |
| `R-A2-HEARTBEAT-VOLUME`, retention/capacity | Classified; Q8/Q13 |
| `R-A2-PROMPT-DTMF`, prompt/consent copy | Deferred; Q10 |
| Executable validation evidence | **Blocked** by the current harness permission mode; no Node/npm command actually ran |

## 10. Blocking questions

None on protocol design or on the accepted A0/A1 contracts. The only blocker in this task is operational: the environment rejected Node/npm execution, so the two required commands and the two-run determinism check could not be executed or observed. The controller must run them in a permission-enabled environment before claiming executable validation.

## 11. Stop state

Only the ten paths listed in §2 were created. Nothing was staged, committed, pushed, merged, opened as a PR/issue or reviewed. No S2/S3 work was started. No review or future-finding fix was performed.

## Review-fix addendum 1 — F1 cumulative fixture slice (2026-10-25)

> Task label: `a2-ivr-s1-fix-ds-20261025-01`
>
> Gate: `fix`, slice S1 only. Accepted plan checkpoint `8efb906f00d47ae2c203269c5382ab8e2079ca1a`.
>
> Scope: accepted code-review finding F1 only (`docs/reviews/code-review-20261025-000422.md`). F2 remains rejected-with-reason and unchanged.

- **Before:** the fixture runner called `validateFixture(entry.fixture, 's1', counters, issues)` for every entry, so any future registered `slice=s2`/`slice=s3` fixture would be rejected as wrong-slice during S2/S3 cumulative runs.
- **After:** each entry's expected slice is derived by `expectedSliceForFixture(entry.fixture?.id, config)` from the exact registered fixture ID prefix constrained by `config.sliceOrder`. Stage S1 still resolves every current fixture to `s1`; hypothetical S2 resolves registered `s1-*` to `s1` and `s2-*` to `s2`; hypothetical S3 resolves `s1-*`/`s2-*`/`s3-*` to their own slices. `validateFixture` now also fails closed when the resolved slice is not a non-empty string. Unknown IDs and registered-ID/`slice` disagreements therefore still fail closed.
- **Production file changed:** `protocol/ivr/validate.mjs` only (helper, unresolved-slice guard, stage-derived call site, entry-module guard for a testable export). No model, spec, fixture, A1, A0, plan or controller content changed. No S2/S3 semantics were added.
- **Runtime validation in this task:** blocked. `npm run check:s3`, the two accepted S1 validator runs, `node --check` and the temporary production-path probe were all refused by the harness permission mode before execution (`This command requires approval`), including a subagent retry. No stdout, stderr, exit code, counter or two-run hash was observed here; §5.2's expected output remains an expectation, not an execution claim. The controller must rerun `npm run check:s3`, the S1 validator twice (byte compare) and `tmp/a2-ivr-s1-fix-probe/probe.mjs` in a permission-enabled environment.
- **Static validation performed:** UTF-8 (`file -bi`) for the changed files; no conflict markers and no trailing whitespace (Grep); `git diff --check -- protocol/ivr/validate.mjs docs/gateflow/a2-production-ivr-protocol-spec-s1-implementation.md` exit 0 with no output (these paths are untracked, so the direct Grep checks are the substantive whitespace evidence); `git status --porcelain` and `git ls-files --others --exclude-standard -- protocol docs tmp` show no new repository path outside the three allowed writes. A repository-wide `git diff --check` exits 2 only because of pre-existing trailing whitespace in the untouched `logo/preview.html`.
- **Detail:** see `docs/gateflow/a2-production-ivr-protocol-spec-s1-fix-1.md` for the full disposition, command record, probe method and hashes. This addendum does not rewrite any original author evidence above.
