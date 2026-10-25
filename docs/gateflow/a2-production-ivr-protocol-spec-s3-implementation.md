# A2 production IVR protocol specification — S3 implementation

> Task label: `a2-ivr-s3-impl-ds-20261025-01`
>
> Correction task label: `a2-ivr-s3-impl-correction1-ds-20261025-01`
>
> Fix task label: `a2-ivr-s3-fix1-ds-20261025-01`
>
> Work unit: `A2-production-ivr-protocol-spec`
>
> Gate: `implementation`, slice S3 only
>
> Accepted base: `989da947ef4f4d20ef5ea9db1c106a17ca272771` (`gateflow: accept A2-production-ivr-protocol-spec S2`)
>
> Binding plan: `docs/gateflow/a2-production-ivr-protocol-spec-plan.md` §§4.10-4.13, 5, 6, 7, 8.4
>
> Binding control: top A2 sections and the accepted S2 checkpoint / S3 entry block in `docs/gateflow/project-execution-control.md`
>
> Status: author-complete for S3 implementation, including controller-found correction `a2-ivr-s3-impl-correction1-ds-20261025-01` and the accepted review-finding fix `a2-ivr-s3-fix1-ds-20261025-01`. The fix artifact is `docs/gateflow/a2-production-ivr-protocol-spec-s3-fix-1.md`. No acceptance, aggregate deepreview, commit, push, PR or merge was started.

## 1. Changed files

Exactly the allowed S3 paths were written:

| Path | Change |
| --- | --- |
| `docs/16-production-ivr-protocol-spec.md` | Added the S3 status boundary, §7 recording media lifecycle and evidence boundary, S3 invariant meanings, S3 validation/fixture documentation and final record updates; renumbered the later sections. |
| `protocol/ivr/v1/ivr-protocol-v1.json` | Added only `recordingLifecycle` and `mediaEvidenceBoundary`. Messages, eventTypes, eventRegistry, identities, invariants and every S2 section are unchanged. |
| `protocol/ivr/validate.mjs` | Added S3 registry validation, the recording declaration/observation/completion/conflict/outbox evaluator, S3 fixture validation, S3 token coverage and S3 projection fields. |
| `protocol/ivr/v1/fixtures/s3-01-recording-validated.json` | New S3 fixture. |
| `protocol/ivr/v1/fixtures/s3-02-declaration-replay.json` | New S3 fixture. |
| `protocol/ivr/v1/fixtures/s3-03-upload-observation-required.json` | New S3 fixture. |
| `protocol/ivr/v1/fixtures/s3-04-upload-expired-quarantine.json` | New S3 fixture. |
| `protocol/ivr/v1/fixtures/s3-05-storage-unknown-retry.json` | New S3 fixture. |
| `protocol/ivr/v1/fixtures/s3-06-recording-identity-conflict.json` | New S3 fixture. |
| `protocol/ivr/v1/fixtures/s3-07-post-terminal-conflict-fact.json` | New S3 fixture. |
| `protocol/ivr/v1/fixtures/s3-08-end-to-end-task-media.json` | New S3 fixture. |
| `docs/gateflow/a2-production-ivr-protocol-spec-s3-implementation.md` | This report. |

`git diff --name-only` confirms that no A0/A1/OpenAPI, controller, plan, accepted review/report, prototype, screenshot, S1/S2 fixture or shared business-logic path was modified by this task. The pre-existing dirty controller, `logo/preview.html`, screenshot modifications and untracked historical review remain untouched.

### 1.1 Controller-found correction

The controller's S3 implementation run reported `s3-08-end-to-end-task-media` as the only failure: step 5 (`terminal`) returned `422` instead of `200`, followed by the expected counter and projection mismatches at steps 6 and 8. Root cause was a single closed-body violation: the `ingestIvrCallEvent` request body carried `callId` even though the same step already supplied the path identity as `server.callId`. The A1 `IvrCallEventRequest` body is closed and `ProtocolMachine.handle` correctly rejects the extra member with `422 VALIDATION_FAILED`.

Correction scope was exactly one fixture key: the body `callId` was removed from the `terminal` step. `server.callId = call-s3-08`, the full `CALL_TERMINATED` payload, the expected `ENDED`/`COMPLETED`/`SETTLED` projection, the terminal/settlement/usage fact counts and every subsequent declaration/observation/completion assertion are unchanged. At correction time, the validator, A1/A0 contracts, model, specification and all other fixtures were not modified; the only other write was this report update. The later `a2-ivr-s3-fix1-ds-20261025-01` fix is a separate bounded change recorded in `docs/gateflow/a2-production-ivr-protocol-spec-s3-fix-1.md` and reflected in §2-§4.

## 2. Recording lifecycle

The model `recordingLifecycle` adds exactly the A0 `ck_recording_status` states and the approved edges:

| From | To | Trigger | Guard |
| --- | --- | --- | --- |
| new | `DECLARED` | first declaration for a known parent call | unique `recordingId`; declaration facts only |
| `DECLARED` | `UPLOADED` | server-local upload observation | one immutable media object; identical replay; changed identity fails closed |
| `UPLOADED` | `VALIDATED` | completion with matching server facts | observed hash, size, MIME and duration match the declaration; server facts win |
| `UPLOADED` | `QUARANTINED` | completion with different or mismatched server facts | quarantine once; first media facts preserved |
| `DECLARED` | `QUARANTINED` | completion or observation at/after `uploadExpiresAt` | no successful media or evidence fact |

No state, table, column, transaction or Problem code is added. `VALIDATED` and `QUARANTINED` are terminal for the recording row.

### 2.1 Declaration

- `declareIvrRecording` accepts only the A1 `RecordingDeclarationRequest` fields `recordingId`, `mimeType`, `sizeBytes`, `durationSeconds` and `sha256`, plus the path `callId`. Unknown fields are `422 VALIDATION_FAILED`.
- A first declaration creates one `evidence.recording` in `DECLARED`, revision 1, with declaration facts only. The response is `201` with `recordingId`, `status=DECLARED`, `uploadExpiresAt`, `upload.credential`, `upload.expiresAt` and `declaredAt`.
- No object key, bucket, object version, permanent URL or raw credential appears in a model projection. The credential exists only in the declaration response and is redacted from projections, logs and artifacts; it is never reissued or extended.
- Same `recordingId` and identical declaration facts while `DECLARED` returns the first `201` snapshot byte-for-byte. It creates no second recording, does not mint a new credential and does not renew `uploadExpiresAt`.
- Changed declaration facts, or any re-declaration after `UPLOADED`/`VALIDATED`/`QUARANTINED`, returns `409 RECORDING_IDENTITY_CONFLICT`.
- `clientReference` is not part of declaration identity.

### 2.2 Upload observation

- Upload is outside the public IVR API. It is modeled as the evaluator/server-local `recording.observeUpload` step, not a new endpoint.
- The observation records exactly `objectIdentityDigest`, `sha256`, `sizeBytes`, `mimeType` and `durationSeconds`. The object digest is an opaque server-side digest; raw object key, bucket, version and credential material never cross the boundary.
- Same observation identity and facts replay the first `UPLOADED` result without a second media object, including replays at or after `uploadExpiresAt`. Changed identity or facts fails closed with `RECORDING_IDENTITY_CONFLICT` and mutates nothing, including after `uploadExpiresAt`.
- A first observation while `DECLARED` at or after `uploadExpiresAt` quarantines once with `UPLOAD_EXPIRED` and creates no successful media or evidence fact. Once the first observation is accepted, immutable replay/conflict is resolved before expiry; late completion of the `UPLOADED` row is not itself an expiry event.

### 2.3 Completion and validation

- `completeIvrRecording` accepts only the path `callId`/`recordingId` and the A1 body field `clientReference`. Any client-supplied `bucket`, `objectKey`, `objectVersionId`, `sha256`, `sizeBytes`, `mimeType` or `durationSeconds` is `422 VALIDATION_FAILED`.
- Completion order is: known call/recording, terminal replay or conflict, upload expiry for a still-`DECLARED` recording, upload observation, storage unknown, validation.
- `DECLARED` completion returns `409 UPLOAD_OBSERVATION_REQUIRED` with no mutation. Retrying the same identity after observation may converge.
- A still-`DECLARED` completion at/after expiry returns `409 UPLOAD_EXPIRED`, quarantines once and produces no successful media fact. An already `UPLOADED` recording is not quarantined merely because completion arrives at/after expiry; it continues through storage-unknown and server-fact validation. A terminal row is not updated again.
- `503 STORAGE_UNKNOWN` leaves the row `UPLOADED`/non-terminal with no new media fact or revision change; the same identity retry may converge.
- Identical server facts replay the first `VALIDATED` or `QUARANTINED` result without a second media object. A pre-terminal differing object or declared/server mismatch quarantines once with a redacted reason and preserves the first media object.
- `VALIDATED` returns only the server-recomputed `validatedDurationSeconds` and `validatedSha256` plus `completedAt`. Declared facts never override server facts.
- `clientReference` is excluded from identity, fingerprint, outbox conflict digest and A0 persistence.

### 2.4 Post-terminal conflict and outbox

- A differing server-observed completion after `VALIDATED`/`QUARANTINED` returns `409 RECORDING_IDENTITY_CONFLICT`, keeps status, revision and validated facts unchanged, and appends exactly one deduplicated redacted Outbox fact.
- Conflict fact identity: event domain `EVIDENCE`, aggregate type `RECORDING`, event type `RECORDING_CONFLICT`, dedupe key `recording-conflict:<conflictDigest>`. The redacted payload contains only `recordingId`, `originalStatus`, `conflictDigest`, `presentedSha256`, `presentedSizeBytes`, `presentedMime`, `presentedDuration` and `reasonCode`.
- Object version/key material is hashed into `conflictDigest`; it is never stored, projected or logged. Repeating the same conflict returns the existing Outbox identity and does not increment the fact count. A changed conflicting payload has a different digest and appends one distinct fact.
- Recording outbox identity is `(callId,recordingId)` with an immutable payload. Declaration `201` is terminal; `UPLOAD_EXPIRED`, `RECORDING_IDENTITY_CONFLICT`, `IVR_ARGUMENT_INVALID`, `VALIDATION_FAILED` and `CALL_UNKNOWN` are permanent; rate-limit/service/internal/transport failures are retryable. Completion `200` is terminal; `UPLOAD_OBSERVATION_REQUIRED` and `STORAGE_UNKNOWN` are retryable in addition to the transient codes; expiry/identity/argument failures are permanent.
- `ACKED` and `PERMANENT_FAILURE` are absorbing for one `(callId,recordingId)` recording outbox entry. A later response, lost-response simulation or post-terminal conflict never changes a terminal entry and never increments another terminal counter.
- Lost responses and restart replay the same recording ID and outbox identity; they never create a second recording, media object or validation fact.

### 2.5 Evidence boundary

- The handoff owner is `evidence`; the prerequisite is `recording.status = VALIDATED`.
- The typed handoff-ready fields are `callId`, `recordingId`, `recordingStatus`, `validatedDurationSeconds`, `validatedSha256` and `mediaObjectDigest`.
- `completionCreatesEvidence=false` and `completionSolidifiesEvidence=false`. The evaluator keeps `evidenceRecordsCreated=0` and `evidenceSolidified=0`. No evidence record, creation event, artifact or share state is created or solidified by `complete`.
- There is no IVR evidence endpoint. Evidence creation identity and policy remain `R-A2-EVIDENCE-HANDOFF`.

## 3. Unchanged contracts

- Messages remain exactly 12; S3 changes no message row or A1 operation.
- Event types remain exactly 11 in the S2 full-frozen four-column form. `eventTypes` and `eventRegistry` are unchanged, so the recomputed digest must remain `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`.
- Identities remain exactly 22 and invariant IDs remain exactly `INV-A2-01..15`.
- No new Problem code, A1 endpoint, A0 table/column/state/transaction or provider-specific value was introduced. The S3 model adds no A0 triple reference.
- `eventOutbox.recordingClassification` keeps its accepted S2 pointer value `deferred to S3`; the actual S3 recording classification is frozen in `recordingLifecycle.outbox`, so the S2 section shape is not rewritten.
- Product rules are preserved: task auto-dial without voice callee confirmation, DTMF only on the no-task path, key `1` one-trial activation without login/real-name, purchase guidance only to Mini Program/Web and no operator/PBX/provider integration.

## 4. Fixtures and cumulative counters

Exactly eight S3 fixtures were added. No S3 fixture is marked negative; the cumulative negative set remains the three S2 fixtures `s2-08`, `s2-10` and `s2-11`.

| Fixture | Scenario | Key assertions |
| --- | --- | --- |
| `s3-01-recording-validated` | declare → upload observe → complete | `VALIDATED`, one recording/media object, server duration/hash, client storage facts `422`, no object key/bucket/version |
| `s3-02-declaration-replay` | declaration response loss → restart → same identity retry | identical first `201` snapshot, same credential/expiry, no second recording, no expiry renewal, outbox `SENT → ACKED` |
| `s3-03-upload-observation-required` | complete before observe → observe → same identity retry | first `409 UPLOAD_OBSERVATION_REQUIRED` with no mutation; retry `VALIDATED` |
| `s3-04-upload-expired-quarantine` | first observation at expiry → complete after expiry → repeat | `409 UPLOAD_EXPIRED`, `QUARANTINED` once, no media/validated fact, completion repeats the first expiry result with the outbox terminal |
| `s3-05-storage-unknown-retry` | complete with unknown storage → retry after expiry with the observation accepted before expiry | `503 STORAGE_UNKNOWN` with `UPLOADED` revision and media object preserved; retry `VALIDATED` even though completion is at/after `uploadExpiresAt` |
| `s3-06-recording-identity-conflict` | changed declaration, observation replay/re-declare/changed observation after expiry, then pre-terminal differing completion | replay `200 UPLOADED` with one media object; re-declare and changed observation `409` with no mutation; differing completion `200 QUARANTINED` once; first media object preserved; outbox stays `ACKED` |
| `s3-07-post-terminal-conflict-fact` | conflicting completion after `VALIDATED`, repeated, second distinct payload, response-loss replay | `409 RECORDING_IDENTITY_CONFLICT`; status/revision/facts unchanged; all eight payload fields asserted; identical payload deduped, distinct payload adds one fact; completion outbox stays `ACKED` with zero permanent failures |
| `s3-08-end-to-end-task-media` | task resolve → one B-leg → terminal/settlement → declare → observe → complete | call `ENDED`, task `COMPLETED`, reservation `SETTLED`, one usage fact, one recording `VALIDATED`, evidence counters 0 |

Cumulative fixture accounting:

- S1: 6
- S2: 11
- S3: 8
- Total: 25
- `slice_fixtures=6+11+8`
- Marked negatives: 3 (S2 only)

The bounded fix `a2-ivr-s3-fix1-ds-20261025-01` extended these same eight fixture IDs without adding a ninth. It proves re-declare after `UPLOADED`, identical/changed observation replay, first observation at/after expiry, late completion after a timely observation, a second distinct post-terminal conflict payload, all eight conflict payload fields and recording-outbox terminal absorption. The fix artifact is `docs/gateflow/a2-production-ivr-protocol-spec-s3-fix-1.md`.

Expected stage-S3 output when the controller executes the accepted command:

```text
stage=s3 messages=12 event_types=11 event_map_form=full-frozen identities=22 fixtures=25 passed=25 failed=0 negative=3 slice_fixtures=6+11+8
a1_operations=49 ivr_operations=6 a1_drift=0
a0_tables=44 a0_status_drift=0 a0_registry_drift=0 a0_column_drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
invariants=15
event_registry_frozen=s2 event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9
PASS protocol-validation (s3)
```

The corrected tree is expected to produce exactly this output: the terminal step now sends only the closed event body, so it should return `200` and continue to the unchanged terminal/settlement and recording/media assertions. This is an expected controller-rerun result, not an author-observed execution.

Stage S2 on the S3 tree is expected to exit nonzero only because `recordingLifecycle`/`mediaEvidenceBoundary` are premature and the eight S3 fixture IDs/fixtures are present; the 17 S1/S2 fixtures, A1/A0 checks and event registry remain green. Stage S1 remains a controlled negative for both S2 and S3 surfaces.

### 4.1 Correction static evidence

The correction runner denied `npm run check:s3`, `node --check` and every direct validator invocation with `This command requires approval`, so no executable pass count, dual-run byte equality, stage-S2 exit or zero-counter observation is claimed by the author. These checks remain mandatory in the controller rerun.

The following static checks were executed on the corrected tree:

- `jq empty` passes on `s3-08-end-to-end-task-media.json`; the terminal step's body keys are exactly `eventId`, `eventType`, `occurredAt`, `payload`, and the fixture retains eight steps.
- The `terminal` step retains `server.callId = call-s3-08`, `expected.status = 200`, the full `CALL_TERMINATED` payload, the unchanged `ENDED`/`COMPLETED`/`SETTLED` projection and the unchanged terminal/settlement/release/usage counts.
- The fixture inventory is 25 files with slice counts 6/11/8, and the three negative fixtures `s2-08`, `s2-10` and `s2-11` are present.
- The model still declares 12 messages, 11 event types, 22 identities and 15 invariants.
- The existing A1 bundle still exposes 49 operations, 6 of them IVR operations.
- `jq -cS '.eventTypes' ... | tr -d '\n' | sha256sum` returns `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`; the frozen event registry digest is unchanged.
- Scoped trailing-whitespace scan of the corrected fixture reports zero matches. `git diff --check` reports only pre-existing `logo/preview.html` whitespace, outside this task's scope.

The expected rerun target remains exact: S3 `fixtures=25 passed=25 failed=0`, negative `3`, slices `6+11+8`, A1 `49/6`, A0 tables `44`, invariants `15`, unchanged registry digest and every drift/doc/stage/fixture counter zero.

## 5. Commands, execution limits and static evidence

| Command | Result |
| --- | --- |
| `cd api/openapi && npm run check:s3` | Permission-denied in the author run and again in the correction rerun (`This command requires approval`); not executed and not claimed. |
| `node --check protocol/ivr/validate.mjs` | Permission-denied in the author run and again in the correction rerun; not executed and not claimed. |
| `node protocol/ivr/validate.mjs --stage s3 ...` (twice) | Permission-denied in the author run and again in the correction rerun; not executed and not claimed. |
| `node protocol/ivr/validate.mjs --stage s2 ...` (expected failure) | Permission-denied in the correction rerun; not executed and not claimed. |
| `git diff --check -- docs/16-production-ivr-protocol-spec.md protocol/ivr` | Executed; exit 0 with no output. |
| Conflict-marker scan over `protocol/ivr` and `docs/16` | Executed with the content search tool; no matches. |
| Trailing-whitespace scan over `protocol/ivr` and `docs/16` | Executed with the content search tool; no matches. |
| `jq empty` on the model and all eight S3 fixtures | Executed; exit 0 with no output, so all nine JSON files parse. |
| `file -bi` on the model, validator, spec, fixtures and report | Executed; model/spec/validator/report are UTF-8 and all S3 fixtures are US-ASCII (valid UTF-8). |
| `jq -cS '.eventTypes' ... \| tr -d '\n' \| sha256sum` | Executed; output `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`, confirming the event registry is unchanged. |
| `git diff --name-only` | Executed; confirms only the three planned tracked files plus pre-existing unrelated dirty state. |
| `git ls-files --others --exclude-standard` | Executed; shows exactly the eight S3 fixtures and this report as new S3 paths, plus the pre-existing untracked task/review paths. `tmp/` remains ignored. |

The runner denied Node/npm execution after version probes, so no executable validator, fixture pass count or dual-run byte-equality is claimed by the author. The correction rerun and the later F1-F4 fix run reproduced the same denial for `npm run check:s3`, `node --check`, direct validator invocations and the stage-S2 negative run. The controller must independently rerun the accepted commands; the correction's expected target is recorded in §4, its executed static checks in §4.1, and the fix-run evidence in the fix artifact. JSON and encoding checks above were executed with the available `jq` and `file` tools.

## 6. A1/A0 drift evidence

- A1: no OpenAPI/contract file was modified; no operation, schema, Problem code or error set changed. The six IVR operations remain `resolveIvrInbound`, `confirmIvrActivation`, `authorizeIvrDirectOutbound`, `ingestIvrCallEvent`, `declareIvrRecording` and `completeIvrRecording`.
- A0: no `docs/14` file was modified and no table/column/state/named CHECK/transaction was added. `recordingLifecycle.states` is the exact subset of `ck_recording_status`; the new model sections add no A0 triple reference.
- Expected controller counters: `a1_operations=49`, `ivr_operations=6`, `a0_tables=44`, `a1_drift=0`, `a0_status_drift=0`, `a0_registry_drift=0`, `a0_column_drift=0`, all `doc_*` counters 0, all `stage_*` counters 0 and both `fixture_id_*` counters 0.

## 7. Residual risks and owners

| Risk | Owner / destination |
| --- | --- |
| `R-A2-UPLOAD-OBSERVE` | Real object-storage event/polling trigger and COS HEAD/version observation remain outside A2 wire; Telephony/IVR + Evidence, Q7/Q8. |
| `R-A2-EVIDENCE-HANDOFF` | Evidence creation/solidification source identity and policy remain with the evidence owner; A0 §9.28/§10.24. |
| `R-A2-DECLARE-REPLAY` | A1 only returns `201/DECLARED`; S3 freezes DECLARED-only replay and post-upload fail-closed behavior. API/evidence projection review. |
| `R-A2-MULTI-RECORDING` | A0 may allow multiple recordings per call; v1 evidence baseline policy is unchanged. Evidence policy. |
| Real COS object version, upload credential issuance, malware scan, retention and media tolerance | Q7/Q8/Q11/Infrastructure; not proven by fixtures. |
| Real DB concurrency, unique constraints and conflict Outbox dedupe witness | Evidence/backend integration tests; fixtures are model-level only. |
| `R-A2-A0-TASKID-TYPE`, `R-A2-SESSION-ABANDONED`, heartbeat volume, terminal/reconciliation authority, `callId` uniqueness, no-callee-retry, prompt/DTMF and signature values | Unchanged from S2; no S3 change or claim. |
| Deferred Q7/Q8/Q10/Q18/Q25/Q27 values | Unchanged and not invented. Q15/Q26/Q28 recording media shape is closed by this slice. |

## 8. Explicit limitations and stop state

This report does not claim a runtime backend, database, Flyway, SIP/CDR, B-leg dispatch, COS observation, credential issuance, malware scan, retention policy, evidence creation/solidification, share state or production acceptance. The fixture evaluator is a deterministic protocol model, not a runtime service or media adapter.

Stop state: the S3 implementation correction is author-complete on the allowed paths. The controller-found terminal body `callId` was removed from S3-08 and this report records the correction; no validator, contract, model, specification or other fixture was changed. No staging, commit, push, PR, issue, merge, code review, aggregate deepreview, S3 acceptance or controller edit was performed. The controller must rerun the accepted S3 validation sequence (including stage-S2 expected failure) before the next Gate, which is independent code/spec review by the paired reviewer provider.
