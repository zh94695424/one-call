# A1 production OpenAPI surfaces — S3 bounded fix-2 report

Task label: `a1-openapi-s3-fix2-mimo26flash-20261024-01`
Author provider: `mimo26flash` / actual model `mimo-v2.6-flash`
Gate: S3 `fix` (second bounded fix after re-review 1; single accepted finding R-01 from `docs/reviews/code-review-20261024-192907.md`, adjudicated in `docs/gateflow/project-execution-control.md` §A1 S3 re-review 1 result and fix 2 entry).
Scope: static OpenAPI 3.1 contract + Redocly custom rules + generated-type compatibility + fix-artifact synchronization. This is not runtime implementation.

## 1. Gate and write boundary

The controller remained in the `fix` gate after re-review 1 failed with exactly one finding (R-01, 中低). This author task modified only the four cumulative S3 implementation paths plus the existing fix-1 artifact (stale-status synchronization) and created exactly this fix-2 artifact. No other path was written, staged, committed, pushed or opened as a PR.

Allowed paths (all modified/created as authorized):

- `api/openapi/onecall-v1.openapi.yaml`
- `api/openapi/contract-rules.mjs`
- `api/openapi/client-compat.ts`
- `docs/15-production-openapi-surface-spec.md` — **not modified**: it does not document IvrDecision field-level nullability (checked: no `nullable`/nullability statement about `availableRecordingSeconds`/`balanceAsOf`/reservation fields anywhere in the document), so it was left unchanged per the binding instruction.
- `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-1.md` — synchronized: its F-08 section claimed all five fields are explicitly nullable; a superseded note pointing to this artifact was appended (the historical claim text itself is retained for auditability).
- `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-2.md` (this file)

Explicitly not written: `redocly.yaml`, `package.json`/lock/config, generated `tmp/` output, `docs/gateflow/project-execution-control.md`, the accepted plan, `docs/13`, `docs/14`, any review artifact, prototypes/screenshots, Git index/history.

## 2. Changed files and post-fix hashes

| File | Post-fix SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `64e7c216deccb556afd42c05022ad81d87fd9a167907990784b0952a60bedb71` |
| `api/openapi/contract-rules.mjs` | `b38fe32c047f22ab81043e695bb7cba5eb74c0f0059670e051c991fbf79c20cb` |
| `api/openapi/client-compat.ts` | `cade5e69d33a72fd40fe51fd22cb8ac4859a038999b6270909271e6b2aa37ab6` |
| `docs/15-production-openapi-surface-spec.md` | `f5acbc9d7c0e9f4df92f740f23a51bb435d2caeaf8fe61bc71611799ed55c707` (unchanged) |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-1.md` | `4f91c7277f086e833001e407818687b486cde1629048a1387521d895ce8848b8` |

Pre-fix-2 hashes (fix-1 post-fix values, superseded by the values above for the three modified implementation files):

| File | Pre-fix SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `d609a2c67b717cf5eb739af74a3e78ce5bd0f52475f56e11353e0c5071c5b6ae` |
| `api/openapi/contract-rules.mjs` | `c5f918d43ce49bb6e3ecd585124be1e00cdef2dbfb6f1ab793d9bc29c9301f5b` |
| `api/openapi/client-compat.ts` | `61b58e6fd10a4b44574a88082a054d9f1e88897aace64fa5f1403b660c915d6e` |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-1.md` | `9a88be3494562a50c9bd3fdf049091a529df03891105d7662d99477e9097b79c` |

### Exact changed lines

| File | Lines | Change |
| --- | --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | 6668 | `IvrDecision` schema `description` reworded: no longer claims "five nullable domain fields … explicitly nullable when unset"; now states the two routing snapshot fields are non-null per A0 and only the three reservation fields are explicitly nullable. |
| `api/openapi/onecall-v1.openapi.yaml` | 6737-6739 | `availableRecordingSeconds`: `type: [integer, 'null']` → `type: integer`, `minimum: 0` retained (non-null). |
| `api/openapi/onecall-v1.openapi.yaml` | 6740-6742 | `balanceAsOf`: `type: [string, 'null']` → `type: string`, `format: date-time` retained (non-null). |
| `api/openapi/contract-rules.mjs` | 84-88 | Stale module-header comment ("Any schema may also opt in via the explicit x-onecall-opaque: true annotation") replaced with the fail-closed `OPAQUE_CLOSURE_EXCEPTION_SCHEMAS` restriction (comment-only; behavior already correct). |
| `api/openapi/contract-rules.mjs` | 2114-2141 | `validateS3FixShapes` F-08 block rewritten: all five fields must stay in `required`; `availableRecordingSeconds`/`balanceAsOf` must be non-nullable (single-string `type`, not an array containing `'null'`, not literal `'null'`); only `reservationId`/`authorizedRecordingSeconds`/`authorizationExpiresAt` keep the array-includes-`'null'` assertion. |
| `api/openapi/client-compat.ts` | 348-370 | "five nullable domain fields" comment replaced; five requiredness assertions kept; five new nullability assertions added: `Equal<...['availableRecordingSeconds'], number>`, `Equal<...['balanceAsOf'], string>` (non-null) and `Equal<...['reservationId'], string \| null>`, `Equal<...['authorizedRecordingSeconds'], number \| null>`, `Equal<...['authorizationExpiresAt'], string \| null>` (nullable). |
| `api/openapi/client-compat.ts` | 899-903 | The five new assertion types exported exactly once in the tuple (each Assert* identifier occurs exactly twice: definition + tuple). |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-1.md` | 76 | Superseded-by-fix-2 note appended under the historical F-08 section (stale "all five nullable" claim). |

## 3. R-01 mapping

R-01 (`docs/reviews/code-review-20261024-192907.md` §2, severity 中低): `IvrDecision` snapshot fields stayed nullable against A0 §10.14 NOT NULL, and the rule/comment encoded the error.

| R-01 sub-claim | Authority | Correction |
| --- | --- | --- |
| `availableRecordingSeconds` must be `integer >= 0` NOT NULL | A0 §10.14 (`docs/14:3736`) | YAML now `type: integer`, `minimum: 0`; rule asserts non-nullable; generated `Equal<..., number>`. |
| `balanceAsOf` must be `timestamptz` NOT NULL | A0 §10.14 (`docs/14:3737`) | YAML now `type: string`, `format: date-time`; rule asserts non-nullable; generated `Equal<..., string>`. |
| Only the three reservation fields are conditionally nullable | A0 §10.14 (`docs/14:3743-3745`) | `reservationId`, `authorizedRecordingSeconds`, `authorizationExpiresAt` keep explicit `type: [T, 'null']`; rule keeps the array-includes-null assertion for these three only; generated `Equal<..., T \| null>` for each. |
| All five stay `required` | Adjudicated F-08 requiredness (unchanged) | `IvrDecision.required` list untouched (YAML 6669-6682); rule still requires all five; the five requiredness assertions in client-compat kept. |
| `validateS3FixShapes` actively asserted the wrong nullability | Re-review 1 evidence (`contract-rules.mjs` old 2113-2129) | Rewritten block at 2114-2141 encodes the corrected split. |
| client-compat "five nullable domain fields" label | Re-review 1 evidence (`client-compat.ts:348`) | Comment corrected; non-null/nullable Equal assertions added and exported. |
| Stale opaque-opt-in comment (folded per controller decision) | Control doc line 423 | Comment at 84-88 now describes the fail-closed two-component restriction; no behavior change. |
| Stale YAML `IvrDecision` description encoding "five nullable" | Discovered during fix; same defect class as R-01 | Description at 6668 reworded (descriptive metadata only, no schema-semantics change beyond the R-01 type correction). |

No other `IvrDecision` property, operation, error set, probe expectation, C6 replay-order marker or S1/S2 semantic was touched. All other F-01..F-11 fix-1 corrections are preserved.

## 4. Validation commands and outcomes

| Command | Outcome |
| --- | --- |
| `node --check api/openapi/contract-rules.mjs` | **Blocked** — sandbox requires interactive approval for `node` (also blocked with sandbox disabled); not executed by this author task. Controller-replay evidence. Static review of the edited block shows balanced braces/brackets and valid JS syntax. |
| `npm --prefix api/openapi run check:s3` | **Blocked** — sandbox requires interactive approval for `npm`; not executed. Controller-replay evidence. |
| Double generation with stable declaration hash | **Blocked** — depends on `npm run generate`. Controller-replay evidence. Note: `tmp/openapi-client/onecall-v1.d.ts` still holds pre-fix-2 types until the controller regenerates; the new Equal assertions are written against the post-regeneration contract. |
| Exact counts `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1 total=49` | **Verified by grep** — 49 `operationId` entries; operation `tags:` counts 18/21/6/3/1; 46 `^  /api/v1/` path keys. |
| Snapshot/reservation nullability in final YAML | **Verified** — `availableRecordingSeconds` `type: integer`/`minimum: 0` (6737-6739); `balanceAsOf` `type: string`/`format: date-time` (6740-6742); `reservationId`/`authorizedRecordingSeconds`/`authorizationExpiresAt` keep `type: [T, 'null']` (6781-6794); all five remain in `required` (6669-6682). |
| ProblemCode enum count | **Verified** — exactly 59 entries (4273-4331). |
| C6 replay-order marker | **Verified** — `x-onecall-replay-order: activation-and-call-check-before-replay` present exactly once; C6 guards-before-replay wording unchanged. |
| Opaque annotation count | **Verified** — exactly two `x-onecall-opaque: true` occurrences (IvrEventPayload, OpaqueProviderPayload); comment fix did not alter behavior. |
| Assert* definition/export pairing | **Verified by grep** — the ten `AssertIvrDecision*` required/nullability identifiers occur exactly 20 times (each defined once at 351-370 and exported once at 894-903). |
| UTF-8 / no CR / no U+FFFD / no conflict markers (all modified files) | **Pass** — Grep scans return zero matches. |
| `git diff --check` scoped to the four files | **Attempted; blocked** — `git` commands require interactive approval in this sandbox. Static inspection shows no introduced trailing whitespace or conflict markers. Controller-replay evidence. |
| Empty Git index / HEAD unchanged | **Attempted; blocked** — `git` commands require approval. This task issued no `git add`/`commit`/`push`/`restore`/`checkout` and no staging tool; index is expected empty and HEAD unchanged. Controller must confirm. |
| S1/S2 negative stage checks | **Blocked** — depend on `npm`. Controller-replay evidence. |

The controller must independently run `node --check`, `npm run check:s3`, the double-generation hash, the S1/S2 negative stages and the focused probes before any acceptance decision. The author does not claim those passed.

## 5. Residuals and owners

- A0 §10.14 `task.taskId = uuid` wording conflicts with the accepted A1 opaque-identity rule; classified residual owned by A0/IVR contract maintenance (does not override A1 §4.10 or the A0 table definitions). Not touched by this fix.
- Generated `tmp/openapi-client/onecall-v1.d.ts` is stale relative to this fix until the controller re-runs `npm run generate`; the new client-compat Equal assertions will type-check only against the regenerated declaration.
- Static contracts do not prove backend, database, auth, payment, telephony, COS, Redis, share-media or provider runtime behavior. Empty-list/duplicate-list semantics, runtime secret/log enforcement, concurrency, admission atomicity, media/upload observation, storage, Redis/session and callback verification remain classified later-work-unit risks.
- All named open inputs and `R-A0-O20-PUBLISHER` retain their owners/destinations.
- Exact future allowed DTMF key-value negotiation stays in the Q15/Q26 owner flows and was not invented.

## 6. Write-boundary confirmation

Modified: `api/openapi/onecall-v1.openapi.yaml`, `api/openapi/contract-rules.mjs`, `api/openapi/client-compat.ts`, `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-1.md` (stale-status note only). Created: `docs/gateflow/a1-production-openapi-surfaces-spec-s3-fix-2.md` (this file). Unchanged: `docs/15-production-openapi-surface-spec.md` (nullability not documented there). Not written: `redocly.yaml`, `package.json`/lock/config, generated `tmp/`, `docs/gateflow/project-execution-control.md`, the accepted plan, `docs/13`, `docs/14`, any review artifact, prototypes/screenshots, Git index/history. No commit, stage, push, PR, issue, merge, re-review or aggregate-review action was taken.

## 7. Completion status

The bounded S3 fix-2 is complete within the allowed write boundary. R-01 is implemented: the two IvrDecision routing snapshot fields are non-null per A0 §10.14, the three reservation-conditional fields remain explicitly nullable, all five stay required, the validation rule and generated-type assertions now encode the corrected split, and the stale opaque-opt-in comment and stale fix-1 status text are synchronized. The author stops here: no self-review, no re-review claim, no S3 acceptance claim, no commit, no stage, no push, no PR. Independent re-review 2 and controller dynamic validation remain mandatory before S3 acceptance.
