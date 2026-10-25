# A2 production IVR protocol specification — S3 fix 1

> Task label: `a2-ivr-s3-fix1-ds-20261025-01`
>
> Gate: `fix` · Slice: **S3 only**
>
> Work unit: `A2-production-ivr-protocol-spec`
>
> Accepted base: `989da947ef4f4d20ef5ea9db1c106a17ca272771` (`gateflow: accept A2-production-ivr-protocol-spec S2`)
>
> Binding plan: `docs/gateflow/a2-production-ivr-protocol-spec-plan.md` §§4.10-4.13, 5, 6, 7, 8.4
>
> Review artifact: `docs/reviews/code-review-20261025-090613.md`
>
> Controller adjudication: `A2 S3 code-review adjudication and fix entry` in `docs/gateflow/project-execution-control.md` (read-only)
>
> Status: **fix complete / awaiting independent re-review**. Executable Node/npm validation was denied by this environment's permission gate; the controller must rerun the exact commands in §8.1. No acceptance, commit, push, PR, issue, merge, re-review or aggregate deepreview was started.

## 1. Scope and changed files

This gate implemented only accepted findings F1-F4 from the S3 code review and preserved rejected F5. It added no endpoint, Problem code, A0/A1 field, A0 state, transition, provider value, numeric policy, ninth fixture ID, evidence runtime or product behavior.

Exact paths written by this fix:

| Path | Change |
| --- | --- |
| `docs/16-production-ivr-protocol-spec.md` | Aligned §7.3-§7.5 expiry, replay/conflict and terminal-outbox semantics; updated the S3 fixture table. §7.1 still contains exactly the same five approved transitions. |
| `protocol/ivr/v1/ivr-protocol-v1.json` | Aligned `recordingLifecycle.uploadObservation` and `completion` descriptive strings and added `recordingLifecycle.outbox.terminalAbsorption`. States, the five transition rows, `eventTypes` and `eventRegistry` are unchanged. |
| `protocol/ivr/validate.mjs` | Reordered upload-observation replay/conflict before expiry, restricted completion expiry to `DECLARED`, made recording-outbox terminal states absorbing, mirrored the model strings and extended S3 fixture expectations. |
| `protocol/ivr/v1/fixtures/s3-04-upload-expired-quarantine.json` | Added the first observation at expiry and outbox/counter assertions. |
| `protocol/ivr/v1/fixtures/s3-05-storage-unknown-retry.json` | Moved the converging completion after `uploadExpiresAt` to prove that a timely observation is not invalidated by a late completion. |
| `protocol/ivr/v1/fixtures/s3-06-recording-identity-conflict.json` | Added observation replay after expiry, post-`UPLOADED` re-declare, changed observation after expiry and outbox/revision assertions; completion now runs after expiry. |
| `protocol/ivr/v1/fixtures/s3-07-post-terminal-conflict-fact.json` | Asserted all eight conflict payload fields with exact digests, completion-outbox `ACKED` absorption, a second distinct conflict payload and a response-loss replay. |
| `docs/gateflow/a2-production-ivr-protocol-spec-s3-implementation.md` | Kept the delivered behavior, fixture inventory and validation status accurate after the fix. |
| `docs/gateflow/a2-production-ivr-protocol-spec-s3-fix-1.md` | This artifact. |

No S1/S2 fixture, A0/A1/OpenAPI file, controller, plan, other review, prototype, screenshot or shared behavior file was modified. `s3-01`, `s3-02`, `s3-03` and `s3-08` were not changed by this fix.

## 2. F1 — expiry semantics and transition consistency

`uploadExpiresAt` now has exactly the controller-selected meaning: the deadline for establishing the first server upload observation.

- `ProtocolMachine.handleObserveUpload` (`protocol/ivr/validate.mjs:2228-2273`) resolves an existing `UPLOADED` row's immutable observation replay/conflict before any expiry check. Identical identity/facts returns the first `200 UPLOADED` result even at/after `uploadExpiresAt`; changed identity/facts returns `409 RECORDING_IDENTITY_CONFLICT` without mutation, also after the deadline. Only the first observation while `DECLARED` at/after expiry quarantines once with `UPLOAD_EXPIRED`.
- `ProtocolMachine.handleComplete` (`protocol/ivr/validate.mjs:2315-2322`) applies expiry only when `recording.status === 'DECLARED'`. An already `UPLOADED` row continues through storage-unknown and server-fact validation; it is not quarantined merely because completion time is at/after `uploadExpiresAt`.
- The frozen transition set is unchanged: `null→DECLARED`, `DECLARED→UPLOADED`, `UPLOADED→VALIDATED`, `UPLOADED→QUARANTINED`, `DECLARED→QUARANTINED`. No `UPLOADED→QUARANTINED` expiry edge was added.
- `docs/16` §7.3, §7.4 and the machine-readable `uploadObservation`/`completion` strings now state the same semantics. `eventTypes`, `eventRegistry` and the S2 event digest are untouched.

## 3. F2 — recording-outbox terminal absorption

`ProtocolMachine.finishRecordingOutbox` (`protocol/ivr/validate.mjs:2009-2033`) now returns immediately when an entry is already `ACKED` or `PERMANENT_FAILURE`, before the response-loss simulation and before any classification branch. A later normal response, lost-response simulation or post-terminal `409 RECORDING_IDENTITY_CONFLICT` can no longer demote the entry or increment `recordingOutboxPermanent`/`recordingOutboxAcked` again. Backend status codes, conflict-fact creation and conflict dedupe are unchanged.

S3-07 now asserts the completion outbox stays `ACKED` and `recordingOutboxPermanent` stays `0` through the post-terminal conflicts, the second distinct conflict and the response-loss replay. S3-06 also locks the declaration outbox as `ACKED` through changed and post-`UPLOADED` re-declares.

## 4. F3 — coverage without changing fixture inventory

Exactly eight S3 fixture IDs remain (`s3-01`…`s3-08`); cumulative inventory remains `6+11+8=25`, and the negative set remains the three S2 fixtures.

| Required proof | Fixture evidence |
| --- | --- |
| Re-declare after `UPLOADED` returns `409` with no mutation | S3-06 `re-declare-after-upload`: status/code, `UPLOADED` revision 2, declaration facts, original `uploadExpiresAt`, first media object, declaration outbox `ACKED`, zero permanent failures. |
| Identical observation replay returns the first `UPLOADED` result with no second media object | S3-06 `upload-observation-replay-after-expiry` at `10:11:00Z` after the `10:10:00Z` deadline: `200`, one media object, `uploadObservationReplays=1`, no quarantine. |
| Changed observation identity/facts returns `409` with no mutation | S3-06 `upload-observation-changed-after-expiry` at `10:12:00Z`: `409 RECORDING_IDENTITY_CONFLICT`, revision/media/declare facts unchanged, no quarantine. |
| First upload observation at/after expiry quarantines once | S3-04 `upload-observation-after-expiry` at `10:11:00Z`: `409 UPLOAD_EXPIRED`, `QUARANTINED` revision 2, no media object, one quarantine, declaration outbox unchanged. |
| Completion after expiry still validates when the observation was accepted before expiry | S3-05 `complete-retry` at `10:11:00Z`: `VALIDATED` revision 3 with completion outbox `ACKED`; the earlier observation was at `10:01:00Z`. |
| Second distinct post-terminal payload appends one distinct fact; identical payload stays deduped | S3-07 repeat keeps `recordingConflictFacts=1`; `post-terminal-conflict-distinct` appends the second fact with `recordingConflictFacts=2`; the response-loss step replays the first payload and keeps the count at 2. |

Every added step asserts status/code, persisted recording state, revision/media preservation, recording-outbox state and the affected counters. S3-07 also exercises terminal absorption after a response-loss simulation while preserving the `409` backend response and dedupe behavior.

## 5. F4 — complete conflict payload assertion

S3-07 asserts all eight frozen payload fields — `recordingId`, `originalStatus`, `conflictDigest`, `presentedSha256`, `presentedSizeBytes`, `presentedMime`, `presentedDuration`, `reasonCode` — for both distinct payloads. The exact deterministic digests were independently recomputed from the canonical conflict input and hashed with `sha256sum`:

- First payload: `bcbd5af31b74b03f9d20d95e1770a95a2aa4340ca3622e8ab814a42193b2e754`
- Second payload: `827efab79d660ced5c4474110b083efbb911440acbe2a317cd71ed0ccb12f5f1`

Raw object key/bucket/version, credentials and media bytes remain absent from the payload and projections.

## 6. Rejected F5 preservation

F5 was not implemented. `projectRecordings` still emits `evidenceCreated: false` and `evidenceSolidified: false`; `assertExecutionInvariants` still requires `evidenceRecordsCreated === 0` and `evidenceSolidified === 0` after every step; no evaluator evidence persistence or runtime was invented. The media-to-evidence handoff remains typed-only with residual owner `R-A2-EVIDENCE-HANDOFF`.

## 7. Documentation decision

`docs/16` was updated because its §7.3/§7.4 prose previously described `UPLOADED` expiry quarantine and its §7.5 outbox text did not state terminal absorption. The §7.1 transition table was intentionally not changed. The model and the validator's `EXPECTED_RECORDING_LIFECYCLE` were updated together because `validateS3Registries` requires exact equality. No new section, row, transition, identity, message, endpoint or Problem code was added.

## 8. Validation evidence

### 8.1 Required executable commands — attempted, denied

This environment denied every Node/npm script invocation with `This command requires approval`; version probes only (`node --version`, `python3 --version`) succeeded. No pass count, byte-identical dual run, stage-S2 exit code or probe result is claimed by the author.

| Required command | Actual result |
| --- | --- |
| `npm run check:s3` from `api/openapi` (attempted as `npm -C api/openapi run check:s3` and `npm --prefix api/openapi run check:s3`) | Denied: `This command requires approval`; not executed. |
| `node --check protocol/ivr/validate.mjs` | Denied: `This command requires approval`; not executed. |
| `node protocol/ivr/validate.mjs --stage s3 …` (run 1 and run 2) | Denied: `This command requires approval`; not executed. |
| `node protocol/ivr/validate.mjs --stage s2 …` | Denied: `This command requires approval`; not executed. |
| `node tmp/a2-s3-fix1-probes.mjs` | Denied: `This command requires approval`; the probe was removed after the denial. |
| `python3 tmp/a2-digest.py` / `./tmp/a2s3_static.py` | Denied: `This command requires approval`; not executed. |

Expected controller target (not observed by the author): stage-S3 twice, byte-identical, with `messages=12 event_types=11 event_map_form=full-frozen identities=22 fixtures=25 passed=25 failed=0 negative=3 slice_fixtures=6+11+8`, A1 `49/6`, A0 `44`, every drift/doc/stage/fixture counter `0`, and `event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`. Stage S2 on the S3 tree must fail only for the S3 projection and the eight S3 fixture IDs.

### 8.2 Static checks executed

| Check | Actual result |
| --- | --- |
| `git diff --check -- <all changed text paths>` | Exit 0, no output. |
| `file -i` on the changed spec, validator, model, fixtures and reports | UTF-8 (`docs/16`, `validate.mjs`, reports) and US-ASCII valid UTF-8 (fixtures/model); no encoding defect. |
| Conflict-marker search over `docs/16`, `protocol/ivr/**` and the S3 reports | No `<<<<<<<`, `=======` or `>>>>>>>` matches. |
| Fixture inventory via `git ls-files --others --exclude-standard protocol/ivr/v1/fixtures` and glob counts | Exactly 8 S3 fixtures; S1=6, S2=11, S3=8, total 25. |
| Model diff `git diff -- protocol/ivr/v1/ivr-protocol-v1.json` | Changes are confined to the S3 sections added after `eventOutbox`; `eventTypes` and `eventRegistry` are untouched. |
| `sha256sum tmp/a2_event_canonical.json` (pre-existing ignored canonical event registry) | `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`, matching the required S2 digest. Combined with the untouched `eventTypes` section, the digest is preserved. |
| Exact conflict-digest recomputation | Canonical JSON strings were written under ignored `tmp/` and hashed with `sha256sum`, producing the two digests in §5; the temporary files were removed. |

### 8.3 Probe design (not executed)

A temporary `tmp/a2-s3-fix1-probes.mjs` imported `validateFixture`, ran the extended S3 fixtures, then re-ran them with deliberately wrong expectations: replay `200→409`, changed observation `409→200`, first observation at expiry `409→200`, late completion `200→409`, completion outbox `ACKED→PERMANENT_FAILURE`/`PERMANENT_FAILURE→SENT`, post-`UPLOADED` re-declare `409→201`, distinct-conflict count `2→1`, and a one-character conflict-digest mutation. Each wrong variant was designed to force a fixture failure. Execution was denied before any result existed, and the probe file was deleted.

## 9. Residual risks and uncovered areas

| Risk | State / owner |
| --- | --- |
| Executable validation and byte-identical dual runs were not performed in this environment | Controller must rerun §8.1 before independent re-review. Static evidence is recorded above. |
| Real object-store observation, provider event shape, versioning and malware scanning | Unchanged residual `R-A2-UPLOAD-OBSERVE`; Telephony/IVR + Evidence, Q7/Q8. |
| Evidence creation/solidification identity and policy | Unchanged residual `R-A2-EVIDENCE-HANDOFF`; evidence owner. |
| Multi-recording policy, retention, credential issuance and exact expiry values | Unchanged; Q7/Q8/Q11/evidence policy. |
| Evaluator is a deterministic protocol machine, not a backend, IVR runtime, media adapter or production acceptance matrix | Unchanged from S3 implementation. |

No accepted finding required an A0/A1 change, new transition, endpoint, Problem code, ninth fixture ID, provider value, numeric policy or evidence runtime; the fix stayed inside the approved scope.

## 10. Completion status

- F1: implemented and documented.
- F2: implemented and fixture-locked.
- F3: implemented inside the existing eight S3 fixture IDs; inventory still `6+11+8=25`, negatives still 3.
- F4: implemented with all eight payload fields and exact digests.
- F5: intentionally not implemented; evidence boundary and zero-counter invariant preserved.
- Executable validation: blocked by the environment permission gate; controller rerun required.
- Stop state: **fix complete / awaiting independent re-review**.

Artifact path: `docs/gateflow/a2-production-ivr-protocol-spec-s3-fix-1.md`.
