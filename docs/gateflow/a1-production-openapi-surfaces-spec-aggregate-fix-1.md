# A1 aggregate deepreview fix 1

- Task label: `a1-openapi-aggregate-fix1-ds-20261024-01`
- Work unit / gate: `A1-production-openapi-surfaces-spec` / `fix` after aggregate deepreview
- Author: fresh DeepSeek bounded fix author
- Date: 2026-10-24
- Accepted S3 HEAD: `58587f8e1bdf62b11e62f0add01e4fa76c149320`
- Adjudication: `docs/gateflow/project-execution-control.md` §“A1 aggregate deepreview result and fix adjudication”
- Aggregate review: `docs/reviews/code-review-20261024-223823.md` (F-01..F-07)
- A0 truth: `docs/14-production-data-model-and-flyway-schema-spec.md`
- IVR truth: `docs/13-ivr-call-task-and-inbound-design.md`
- Surface spec: `docs/15-production-openapi-surface-spec.md`

This artifact records a bounded implementation of the adjudicated F-01..F-07 corrections only. It is not an aggregate acceptance, a review, an S3 closeout, a runtime claim, or a push/PR approval.

## Scope and preservation

Only these paths were modified:

- `api/openapi/onecall-v1.openapi.yaml`
- `api/openapi/contract-rules.mjs`
- `api/openapi/client-compat.ts`
- `docs/15-production-openapi-surface-spec.md`
- this artifact (`docs/gateflow/a1-production-openapi-surfaces-spec-aggregate-fix-1.md`)

Preserved unchanged: exact 49 operations and surface counts `customer 18 / operations 21 / ivr 6 / public-share 3 / provider-callback 1`; every prior accepted finding and exact per-operation Problem set; secret boundaries and return-once directions; stage sets; A0 catalog/model/anchor mappings not named by the adjudication; controller, accepted plan, config/package files, docs/09-14, prototypes/screenshots, prior artifacts, generated tracked state, Git index/history and remote state.

## Finding-by-finding corrections

### F-01 — exact A0 `AuditFact` projection

Scheme `components.schemas.AuditFact` (`onecall-v1.openapi.yaml:5942`):

- `actorType` enum is now exactly `ACCOUNT | EMPLOYEE | SERVICE | SYSTEM`; `CUSTOMER`/`PROVIDER` were removed.
- `actorId` moved into `required`, remains nullable (`type: [string, 'null']`) and `format: uuid`; description records the A0 conditional rule: null only when `actorType = SYSTEM`, otherwise a required actor UUID. No ID is invented.
- `objectId` remains `required`, is now nullable (`type: [string, 'null']`) and `format: uuid`; description records null only when `objectType = SYSTEM`.
- `actionCode` is `required` string, `minLength: 2`, `maxLength: 128`, `pattern: '^[A-Z][A-Z0-9_]{1,127}$'`.
- `reasonCode` moved into `required`, remains nullable string with `minLength: 1`, `maxLength: 128` on the string branch.
- `traceId` remains `required` and is now nullable bounded string 1-128.
- `beforeSnapshot`/`afterSnapshot` and all other fields are unchanged.
- `contract-rules.mjs::validateAggregateFix1Shapes` pins enum, requiredness/nullability, UUID formats, string bounds and the action-code pattern.
- `client-compat.ts` assertion block (`AuditFact` alias) pins the generated enum and required-nullable indexed types.

`docs/15` §3.1 `listOperationsAuditFacts` now states the exact A0 projection and conditional-null semantics.

### F-02 — remove unowned `TrialPolicyDraft.createdAt`

- Removed `createdAt` from `TrialPolicyDraft.required` and from `TrialPolicyDraft.properties` (`onecall-v1.openapi.yaml:6223`). No replacement or derivation was added.
- `validateAggregateFix1Shapes` rejects a declared or required `createdAt`; `client-compat.ts` adds `AssertTrialDraftNoCreatedAt`.
- `docs/15` §3.5 records that the accepted plan §5.6 provisional `createdAt` is superseded because A0 `entitlement_metering.trial_policy_version` has no owner timestamp column (A0 §4.29).
- `RecordingDeclaration.declaredAt` and `RecordingCompletion.completedAt` wire fields are unchanged; docs/15 §3.5 records the exact mapping `declaredAt <- evidence.recording.created_at` and `completedAt <- evidence.recording.validated_at` (A0 §4.40).

### F-03 — dedicated I4 `EventIdConflict` response

- Added reusable `components.responses.EventIdConflict` (`onecall-v1.openapi.yaml:4199`) whose description is tied to `EVENT_ID_CONFLICT`.
- Pointed only `ingestIvrCallEvent` 409 at it (`:3422-3423`).
- I1-I3 (`resolveIvrInbound`, `confirmIvrActivation`, `authorizeIvrDirectOutbound`) keep `SessionIdentityMismatch` at 409.
- I4's exact Problem codes/statuses and docs/15 §4 remain unchanged.
- `validateI4ResponseFamily` pins the family mapping and rejects any non-I4 use of `EventIdConflict`; executable negative probes cover I4→SessionIdentityMismatch and I1→EventIdConflict drift.

### F-04 — bounded fail-closed validator hardening

Implemented as a minimal strict design, not a generic OpenAPI interpreter:

1. **Parameter trust coverage** (`validateParameterTrust`): every operation-level and path-item `path`/`query`/`header`/`cookie` parameter is resolved and scanned. Parameter names and every traversed schema property are checked against the trusted-fact and forbidden-secret sets. Local parameter `$ref`s use the existing cycle-safe resolver; external, unresolved, sibling-carrying and cyclic refs fail closed. Unknown parameter locations fail closed. No parameter exceptions were added.
2. **Path Item `$ref` rejection** (`validatePathItemRefs`): any Path Item carrying `$ref` (with or without siblings) is rejected. No merging is implemented.
3. **Audience and exact role semantics** (`validateSurfaceAudienceRoles`): pinned surface→audience map (`customer→customer`, `operations→employee`, `ivr→ivr-service`, `public-share→public-capability`, `provider-callback→provider`). Each of the 21 operations-surface operations has an exact pinned role set (`[ADMIN,OPS]`, `[OPS]` or `[ADMIN]`); non-operations operations must declare no role extension.
4. **Security scheme shapes** (`validateSecurityBySurface` extension): `customerSession`/`employeeSession` = `http`/`bearer`/`opaque-session`; `ivrMtls`/`providerCallbackMtls` = `mutualTLS`; `ivrSignature` = header API key `X-ONECALL-SIGNATURE`; `providerCallbackSignature` = header API key `X-ONECALL-CALLBACK-SIGNATURE`. Unknown schemes and drifted structural keys fail closed. Per-operation security ownership is the existing accepted mapping.
5. **Unowned request-schema keywords** (`walkRequestSchema`): own-property presence of `patternProperties`, `dependentSchemas`, `unevaluatedProperties`, `if`, `then`, `else`, `not`, `contains`, `propertyNames`, `$defs` is rejected fail-closed in every traversed request schema (request bodies and parameter schemas). No partial interpretation is attempted.
6. **Executable probes** (`validateAggregateFix1Probes`): positive controls run every new validator against the canonical contract; adversarial negatives cover forbidden parameter facts, external/unresolved/cyclic parameter refs, path-item `$ref`, audience drift, role drift on customer and operations surfaces, malformed/unapproved security schemes, all ten rejected schema keywords, registry-surface orphaning, I4/I1-I3 response-family drift, direct-`null`/nullable-array `expiresAt`, optional/nullable A0 NOT NULL projections, AuditFact drift and a reintroduced trial-draft `createdAt`. All existing closure/ref/array/opaque checks are preserved.

Note: the adjudication text names `providerMtls` and `capabilityToken`; the accepted contract uses `providerCallbackMtls` and the `capabilityToken` path parameter. No alternate scheme was invented: the validator pins the actual approved scheme names/shapes and the existing public-share path-parameter rule.

### F-05 — accepted portion only

- `CatalogVersionDraft.snapshotSha256` and `TrialPolicyDraft.snapshotSha256` are now `required` non-null strings (`onecall-v1.openapi.yaml:6068`, `:6236`).
- `ShareState`, `ShareSummary`, `ShareCreated` now require non-null `evidenceId` (UUID) and `codeGeneration` (`:6371`, `:6413`, `:6454`). These are direct creators of the same projections; no duplicate properties were added.
- `OutboundCallTask.expiresAt` guard now rejects both a direct `type: 'null'` and any type array containing `null` (`contract-rules.mjs:2306-2310`).
- `validateAggregateFix1Shapes` and `client-compat.ts` assert requiredness/non-nullability; executable probes cover regression to optional/nullable forms.
- Share status unions were **not** narrowed, per the controller's rejected-with-reason disposition.

### F-06 — real P1 optionality assertions

`client-compat.ts` now uses genuine indexed optionality:

- key existence: `'content' extends keyof PublicShareView` and `'verificationChallenge' extends keyof PublicShareView`;
- optionality: `undefined extends PublicShareView['content']` and `undefined extends PublicShareView['verificationChallenge']`.

The vacuous `Required<T>` comparisons were removed. The canonical `PublicShareView` optionality validator is retained.

### F-07 — registry/docs reconciliation

- docs/15 `VALIDATION_FAILED` surfaces now include `public-share` (`docs/15:180`).
- YAML registry `DEPENDENCY_UNAVAILABLE.surfaces` is now exactly `[public-share]` (`onecall-v1.openapi.yaml:268-271`); K1's exact error set was not expanded.
- `validateRegistrySurfaceBacking` requires every registry `(ProblemCode, surface)` pair to be backed by at least one operation on that surface declaring the exact code, and pins `DEPENDENCY_UNAVAILABLE` to `[public-share]`.
- Consequential reconciliation: the new backing rule exposes that no public-share operation declares `NOT_FOUND` (P1-P3 use `SHARE_TOKEN_INVALID` for 404). To keep the registry truthful without changing any operation's exact error set, `NOT_FOUND.surfaces` was narrowed to `[customer, operations]` in YAML (`:56-59`) and the matching docs/15 row (`docs/15:179`). No code was added to any operation.
- Exact per-operation error inventories, common-code handling (`UNAUTHENTICATED`, `FORBIDDEN`, `VALIDATION_FAILED`, `RATE_LIMITED`, `SERVICE_UNAVAILABLE`, `INTERNAL`) and docs/15 §4 exact-set bullets are unchanged.

## Validation evidence

### Required dynamic commands — all blocked by the approval gate

The session's Bash approval gate rejected every non-version execution attempt with “This command requires approval”. The controller/reviewer environment likewise blocked these commands in the prior aggregate review. No attempt was silently skipped; each required command was attempted once.

| Command (from `api/openapi`) | Outcome |
| --- | --- |
| `node --check contract-rules.mjs` | BLOCKED (approval gate); not executed. |
| `npm run check:s3` | BLOCKED (approval gate); not executed. |
| `npm run generate` (twice, SHA-256 compare) | BLOCKED; not executed. Generated `tmp/openapi-client/onecall-v1.d.ts` therefore remains the pre-fix generation and was not edited. |
| `npm run check:s1` | BLOCKED; not executed. |
| `npm run check:s2` | BLOCKED; not executed. |

Consequently no dynamic probe result, lint result, bundled-document result or regenerated-declaration hash is claimed. The controller must run the full dynamic suite before re-review.

Static stage arithmetic is preserved: `check:s1` should report the 35 later-operation IDs (49 − 14) plus surface-count mismatches after shared checks pass; `check:s2` should report the 17 S3-operation IDs (49 − 32) plus surface-count mismatches. Operation counts were independently re-counted as 49 and `18/21/6/3/1`.

### Static evidence actually performed

- Operation inventory: 49 `operationId` entries; surface extension counts `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1`.
- `AuditFact` enum is `[ACCOUNT, EMPLOYEE, SERVICE, SYSTEM]`; no `CUSTOMER, EMPLOYEE, SYSTEM, PROVIDER` remainder.
- No `TrialPolicyDraft` block contains `createdAt`.
- `EventIdConflict` appears exactly twice: the component definition and the single I4 409 reference.
- `ShareState`/`ShareSummary`/`ShareCreated` required arrays include `evidenceId` and `codeGeneration`; both draft `snapshotSha256` fields are in `required`.
- Registry `DEPENDENCY_UNAVAILABLE` = `[public-share]`; `NOT_FOUND` = `[customer, operations]`; no public-share operation declares `NOT_FOUND`; all other registry surface pairs have a declaring operation.
- Scoped `git diff --check` over the four modified contract/docs paths: exit 0, no output.
- Encoding/hygiene on the four paths: UTF-8, zero U+FFFD, zero CR, zero conflict markers, zero trailing whitespace.
- Git index empty (`git diff --cached --name-only | wc -l` = 0).
- Exact allowed-path audit: only the four intended tracked files are modified by this fix; pre-existing controller/logo/screenshot/untracked-review changes remain untouched.

### Executable probes authored (will run under `npm run check:s3`)

`validateAggregateFix1Probes` runs in-process whenever the `onecall/sensitive-fields` rule runs. It contains positive controls for canonical parameters, path items, audience/roles, security schemes, request-schema keywords, I4 family, registry backing and aggregate shapes, plus one negative probe per finding class listed under F-04.6. No `/tmp` fixture file is needed because the probes are in-process synthetic roots; nothing was written outside the repository except this artifact.

## Hashes (post-fix, SHA-256)

| Path | SHA-256 |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | `6AAE590BCAE7AFA59434ABB321E53C57835A6E724FC7A6CBA27A8F5B84D9D014` |
| `api/openapi/contract-rules.mjs` | `54D4085386478B73BB517820DB0052E879B84AF9A2A2675012175B858EB0BD0E` |
| `api/openapi/client-compat.ts` | `6798574288480A0288FD0FBFB9BBCE99DC312DDAA7DEFA86541084967F73538B` |
| `docs/15-production-openapi-surface-spec.md` | `58570CE5041DF6BD3FC9CB935AC8DE80F3E0321A6C8736422FDD486BF5B77FCC` |

## Residual risks and explicit limits

- Dynamic lint/typecheck/stage/generation validation was blocked, so the regenerated `onecall-v1.d.ts` and the new `client-compat.ts` assertions are unverified at execution time. The controller must run `npm run check:s3`, double `npm run generate` hash comparison and `check:s1`/`check:s2` before re-review.
- The `NOT_FOUND` registry-surface narrowing is a consequential correction required by the adjudicated F-07 backing rule; it is called out explicitly in case the controller wants to record it as an adjudication clarification.
- No runtime service, database, auth, payment, telephony, storage, Redis, media or provider behavior is claimed.
- Existing classified residuals (C15 mode-conditional extraction code, A0 §10.14 `taskId` wording, global T-S/uq/ck anchor ownership, capability-token path logging, runtime behavior) remain with their existing owners and were not expanded.

## Status statement

No independent review was performed, no aggregate acceptance is claimed, no commit was created, nothing was staged, nothing was pushed, no PR was created or mutated, and no other gate was entered. Controller validation and a fresh cross-provider aggregate re-review are mandatory.
