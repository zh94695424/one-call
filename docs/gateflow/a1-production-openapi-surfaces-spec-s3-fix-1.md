# A1 production OpenAPI surfaces — S3 bounded fix-1 report

Task label: `a1-openapi-s3-fix1-mimo26flash-20261024-01`
Author provider: `mimo26flash` / actual model `mimo-v2.6-flash`
Gate: S3 `fix` (controller-accepted findings F-01..F-11 from `docs/reviews/code-review-20261024-181047.md`, adjudicated in `docs/gateflow/project-execution-control.md` §A1 S3 code-review result).
Scope: static OpenAPI 3.1 contract + Redocly custom rules + generated-type compatibility + human governance doc. This is not runtime implementation.

## 1. Gate and write boundary

The controller entered one bounded `fix` gate after the S3 code review failed with eleven findings. This author task modified only the four cumulative S3 implementation paths and created exactly this fix artifact. No other path was written, staged, committed, pushed or opened as a PR.

Allowed paths (all modified/created as authorized):

- `api/openapi/onecall-v1.openapi.yaml`
- `api/openapi/contract-rules.mjs`
- `api/openapi/client-compat.ts`
- `docs/15-production-openapi-surface-spec.md`
- `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-1.md` (this file)

Explicitly not written: `redocly.yaml`, `package.json`/lock/config, generated `tmp/` output, `docs/gateflow/project-execution-control.md`, the accepted plan, `docs/13`, `docs/14`, any review artifact, prototypes/screenshots, Git index/history.

## 2. Changed files and post-fix hashes

| File | Post-fix SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `d609a2c67b717cf5eb739af74a3e78ce5bd0f52475f56e11353e0c5071c5b6ae` |
| `api/openapi/contract-rules.mjs` | `c5f918d43ce49bb6e3ecd585124be1e00cdef2dbfb6f1ab793d9bc29c9301f5b` |
| `api/openapi/client-compat.ts` | `61b58e6fd10a4b44574a88082a054d9f1e88897aace64fa5f1403b660c915d6e` |
| `docs/15-production-openapi-surface-spec.md` | `f5acbc9d7c0e9f4df92f740f23a51bb435d2caeaf8fe61bc71611799ed55c707` |

Pre-fix hashes (controller-recorded, superseded by the values above):

| File | Pre-fix SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `9F40D1A5B7B9DCFC37EBADB85EAA6722987C85EE2DD4A645DDFFDE434E97C963` |
| `api/openapi/contract-rules.mjs` | `F95385D635E1E5985E18EC7163E7120A5FEE68118DA56B4A0E168B862D8673FB` |
| `api/openapi/client-compat.ts` | `2C8D071E1FEBD4D75DC5D02A9207D6C784A80639D47120FDF9272D12B5B013A2` |
| `docs/15-production-openapi-surface-spec.md` | `1B1FFCEA20BB8CF602F6D2EB031CC1A7160E335FBDD4E6FC44EF47F3E5BEC1A2` |

The historical S3 implementation report `docs/gateflow/a1-production-openapi-surfaces-spec-s3-implementation.md` is left unchanged; it holds the pre-fix controller-verified hashes and remains the historical record. This fix artifact supersedes its hash values for the current contract state.

## 3. Finding mapping (F-01..F-11)

### F-01 — opaque A0 protocol identities map non-UUID bounded strings

External `taskId`/`callId`/`recordingId`/`shareId` now use `minLength: 1` + exact `maxLength` (taskId 1-64; callId/recordingId/shareId 1-128) instead of `format: uuid`, at all 20 occurrences (10 path parameters + 10 component properties, plus the nested `IvrDecision.task.taskId`). Internal `decisionId`/`sessionId`/`reservationId`/`evidenceId`/`admissionId` and true internal resources (e.g. `callerBindingId`, `subjectId`, `orderId`) retain `format: uuid`. Positive non-UUID value probes (`OT-EXAMPLE-0001`, `CALL-20261021-001`, `REC-20261021-001`, `SHARE-20261021-001`) and over-bound/empty negative probes are asserted in `validateS3FixProbes`. The contradictory A0 §10.14 `task.taskId = uuid` line remains a classified A0/IVR documentation residual (owner: A0/IVR contract maintenance) and was not edited.

### F-02 — ShareCreated is a satisfiable closed JSON Schema 2020-12 response

The unsatisfiable `allOf: [ShareState, {additionalProperties:false, ...}]` construction was replaced with a direct flattened closed object: all eight ShareState fields (with opaque `shareId` 1-128) plus `capabilityToken` (required, `readOnly`, `x-onecall-return-once`, `x-onecall-log-redaction: required`, 1-512) and `extractionCode` (optional, same annotations, 1-128, mode-conditional description). `validateSecretDirection` was rewritten to use a new `collectResolvedProperties` helper that resolves `$ref` chains and walks `allOf`/`anyOf`/`oneOf`, so secret-direction validation actually inspects both properties after ref/composition shape. Generated compatibility asserts `capabilityToken` required, `extractionCode` present-optional and no `createdAt`.

### F-03 — C15 201/200 and C18 discriminated result

C15 201 points at the flattened `ShareCreated` (`capabilityToken` required; `extractionCode` required only for `EXTRACTION_CODE` mode via optional property). C15 200 now points at metadata-only `ShareState` (no secrets). `ShareCodeRotation` is rebuilt as a closed discriminated wrapper (`type: object`, `additionalProperties: false`, `discriminator.propertyName: result`, `oneOf` over exactly two branches) following the proven P2 verification-wrapper pattern: `ShareCodeRotationRotated` (const `result: ROTATED`, `extractionCode` required) and `ShareCodeRotationReplayed` (const `result: REPLAYED`, no `extractionCode` property). Neither branch nor the wrapper carries `capabilityToken`. `validateS3FixShapes` asserts the wrapper/branch requiredness and the absence of `capabilityToken`; generated compatibility asserts branch key/requiredness (not literal const values, to avoid discriminator renaming).

### F-04 — createdAt removed

`createdAt` was removed from `ShareSummary` (required list + property) and does not exist in the flattened `ShareCreated`. `validFrom` is the authoritative creation timestamp. `validateS3FixShapes` and generated compatibility assert the absence.

### F-05 — Order.currency assertion restored

`AssertOrderCurrency` (`components['schemas']['Order']['currency']` equals `'CNY'`) was restored as one direct generated-type assertion and exported exactly once in the tuple.

### F-06 — protocolVersion, I4 and expiresAt

`IvrInboundResolveRequest.protocolVersion` is now required, bounded 1-32, and described as a client-supplied authenticated/fingerprinted input (never server-derived); the I1 operation description was updated accordingly. `IvrCallEventRequest` (I4): `sequenceNo` is optional with `minimum: 1`, `payload` is required, `legId` is bounded 1-64. `OutboundCallTask.expiresAt` is now a non-nullable required `date-time`.

### F-07 — recording declaration bounds and I5 400

`RecordingDeclarationRequest.sha256` is required; `durationSeconds` has `minimum: 1`; `mimeType` is bounded 1-64. `RecordingCompletion.validatedDurationSeconds` has `minimum: 1` when present. `declareIvrRecording` (I5) now declares `IVR_ARGUMENT_INVALID` in its error inventory and exposes an explicit `'400': $ref IvRArgumentInvalid` response.

### F-08 — IvrDecision required nullable fields

`IvrDecision.required` now includes `availableRecordingSeconds`, `balanceAsOf`, `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt`, retaining their explicit nullable types. `validateS3FixShapes` and generated compatibility assert requiredness and nullability.

> **Superseded by fix-2 (R-01):** the claim that all five fields are explicitly nullable is stale. Per A0 §10.14, `availableRecordingSeconds` and `balanceAsOf` are NOT NULL; only the three reservation-conditional fields (`reservationId`, `authorizedRecordingSeconds`, `authorizationExpiresAt`) are nullable. See `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-2.md`.

### F-09 — I2/I3 required confirmation fields

`IvrActivationConfirmRequest.required` now includes `decision` (const/enum `ACCEPT`). `IvrDirectAuthorizationRequest.required` now includes `promptVersion` (1-64), `confirmationKey` (1-64) and `confirmationContent` (1-128), bounded from existing conventions. Exact key-value negotiation stays in the Q15/Q26 owner flows and was not invented.

### F-10 — approved anchor ownership and opaque closure restriction

`a0ContractAnchors()` no longer extracts every `§N.N` token from docs/14; it uses an explicit approved ownership set (`§7.1`, `§7.3`, `§7.4`, `§9.18`, `§9.24`, `§9.27`, `§9.28`, `§10.14`, `§10.17`) and verifies each exists in the A0 text (fails closed if missing). Opaque closure exemptions are restricted to the two named components `OpaqueProviderPayload`/`IvrEventPayload` by node identity (`walkRequestSchema`) and by component name (`validateSensitiveFields`); any other `x-onecall-opaque: true` annotation — including inline schemas — fails closed with an explicit problem. `validateS3FixProbes` asserts the approved set contains no unapproved cited sections and that only the two approved components carry the annotation.

### F-11 — P2/P3 error reconciliation

`createPublicShareAdmission` (P2) inventory now includes `SHARE_TOKEN_INVALID`, `SHARE_REVOKED`, `SHARE_EXPIRED` and `SHARE_GENERATION_STALE` (keeping `SHARE_LOCKED`); `authorizePublicShareMedia` (P3) now includes `SHARE_TOKEN_INVALID`, `SHARE_REVOKED` and `SHARE_EXPIRED` (keeping `SHARE_GENERATION_STALE`; `SHARE_LOCKED` remains absent). Both operations already declared 404/409/410 responses, so no new response blocks were needed. `exactDomainSets` in `contract-rules.mjs` and the docs/15 exact-set paragraphs (§4 and §3.3 rows) were reconciled to match.

C6 lifecycle/access guards preceding keyed replay, and keyed replay preceding housekeeping/effects, remain unchanged (not an S3 finding).

## 4. Rule registration

`redocly.yaml` is not modifiable and does not enable new named rules. `validateS3FixShapes` and `validateS3FixProbes` were chained into the existing enabled `sensitive-fields` rule (alongside `validateSensitiveFields`, `validateTrustedRequestFacts` and `validateS2FixShapes`), so they run under the current configuration without touching `redocly.yaml`.

## 5. Validation commands and outcomes

| Command | Outcome |
| --- | --- |
| `node --check api/openapi/contract-rules.mjs` | **Blocked** — sandbox requires interactive approval for `node`; not executed by this author task. Controller-replay evidence. |
| `npm --prefix api/openapi run check:s3` | **Blocked** — sandbox requires interactive approval for `npm`; not executed by this author task. Controller-replay evidence. |
| Double generation with stable declaration hash | **Blocked** — depends on `npm run generate`. Controller-replay evidence. |
| Exact counts `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1 total=49` | **Verified by grep** — 49 `operationId` entries; surface tags count 18/21/6/3/1. |
| `check:s1`/`check:s2` negative stage checks | **Blocked** — depend on `npm`. Controller-replay evidence. |
| Focused positive/negative probes per finding | **Implemented in `validateS3FixProbes`** (runs when the controller executes `check:s3`). Author could not execute due to the `node`/`npm` block above. |
| `git diff --check` (scoped to the four files) | **Pass** — no whitespace diagnostics on the four modified files. (An unrelated pre-existing `logo/preview.html` diff, not written by this task, reports trailing whitespace; it is out of scope.) |
| UTF-8 / no CR / no U+FFFD / no conflict markers | **Pass** — Grep scans of all four files return zero matches. |
| Empty Git index | **Pass** — `git diff --cached --stat` returns empty. |
| Only allowed paths modified | **Pass** — `git status --porcelain` shows the four cumulative files modified plus pre-existing unrelated modifications (screenshots, `logo/preview.html`, control doc, prior untracked artifacts) that this task did not write. |

The controller must independently run `node --check`, `npm run check:s3`, the double-generation hash, the S1/S2 negative stage checks and the focused probes. The author does not claim those passed; the blocked commands are recorded as controller-replay evidence, consistent with prior S3 author-session constraints.

## 6. Residuals and owners

- A0 §10.14 `task.taskId = uuid` wording conflicts with the accepted A1 opaque-identity rule; classified residual owned by A0/IVR contract maintenance (does not override A1 §4.10 or the A0 table definitions).
- Static contracts do not prove backend, database, auth, payment, telephony, COS, Redis, share-media or provider runtime behavior. Empty-list/duplicate-list semantics, runtime secret/log enforcement, concurrency, admission atomicity, media/upload observation, storage, Redis/session and callback verification remain classified later-work-unit risks.
- All named open inputs and `R-A0-O20-PUBLISHER` retain their owners/destinations.
- Exact future allowed DTMF key-value negotiation stays in the Q15/Q26 owner flows and was not invented.

## 7. Completion status

The bounded S3 fix-1 implementation is complete within the allowed write boundary. All eleven controller-accepted corrections (F-01..F-11) are implemented in the four cumulative files. The author stops here: no self-review, no re-review claim, no commit, no stage, no push, no PR. Independent re-review and controller validation remain mandatory before S3 acceptance.
