# A1 Production OpenAPI Surfaces Spec — bounded plan-fix 3 (F-10..F-13 + accepted clarifications)

> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: `fix` (plan-only correction; no runtime, schema, UI, commit, push or PR)
>
> Basis: controller adjudication in `docs/gateflow/project-execution-control.md` (A1 binding goal, gate record, F-10..F-13 adjudication and A1 re-review clarification adjudication), independent re-review `docs/reviews/plan-review-20261023-171627.md`, and historical evidence `docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-1.md` / `...-plan-fix-2.md`.
>
> Edits attributable to this task: `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` and this new artifact. No other path was edited, staged, committed, pushed or turned into a PR.

## 1. Invariants preserved

| Invariant | Evidence |
| --- | --- |
| Exactly 49 operations with surface counts customer=18, operations=21, IVR=6, public-share=3, provider-callback=1 | Anchored inventory extraction: 18/21/6/3/1 = 49 row-count; operationId anchors 18/21/6/3/1. |
| Exact `/api/v1/**` routes and five approved prefixes | 51 total `METHOD /path` tokens in the plan = 49 inventory rows + 2 existing versioned prose examples; 0 unversioned; 0 `/api/v2`; no duplicate row added or removed. |
| Three-slice allocation 14/18/17 | S1 = C1/C2/C3/C4/C5/C13/C14 + O1-O7 = 14; S2 = C9-C12 + O8-O21 = 18; S3 = C6/C7/C8/C15-C18 + I1-I6 + P1-P3 + K1 = 17; 14+18+17 = 49. |
| Five-surface scope, non-goals, security/authorization architecture, open-input ownership | Unchanged; no operation, route, schema family, provider field, policy value or role was added. |
| F-01..F-09 fixes | Unchanged at their accepted locations; this artifact does not reopen them. |

## 2. Exact edits (plan only)

| Plan area | Edit |
| --- | --- |
| Header / correction basis | Gate line now records bounded plan-fix-3 / F-10..F-13 + clarifications; correction basis names the failed re-review and this artifact, and states F-01..F-09 plus 49 operations/routes/counts/slices are unchanged. |
| §1.3 signal 6 | Explicitly records O18-O21 as state-conditional natural-identity/`expectedRevision` writes with no platform idempotency scope and no `Idempotency-Key`. |
| §4.1 contract root | Top-level `servers` is required and must contain exactly one relative root `{url: /}`; missing, absolute or additional server roots fail validation. |
| §4.6 registry strategy | Adds the exact customer task codes and forbids A0 IVR reasons as Problem codes; A0 IVR reasons remain only in `IvrDecision`. |
| §4.6 representative registry | Adds `OUTBOUND_CALL_IN_PROGRESS`, `OUTBOUND_TASK_CONFLICT`, `OUTBOUND_TASK_NOT_CANCELLABLE`, `ACTIVATION_STATE_CONFLICT`, `PURCHASE_CONSENT_REQUIRED`, `PURCHASE_CONSENT_STALE`, `PURCHASE_CONSENT_DECLINED`; splits `CLAIM_REPLAY_CONFLICT` / `CLAIM_CONTEXT_CONFLICT` into exact rows; tightens DRAFT conflict/stale meanings. |
| §4.7 idempotency table | O18/O20 create and O19/O21 replace are rewritten to state-conditional natural identity + canonical persisted-content projection and `expectedRevision` + requested/current projection; no `Idempotency-Key`, no `platform.idempotency_record`, no new scope/table/transaction. Initial/replay success status is deterministic (create 201 first / 200 identical replay; replace 200 applied or proven-applied replay). Adds the exact projection field sets and `x-onecall-idempotency` / `x-onecall-a0-contracts` expectations. |
| §4.8 cursor rules | Adds O1: sort `createdAt desc, subjectId desc`; structured filters `subjectId?/source?/claimStatus?/activationStatus?/realNameStatus?/riskState?`; filter digest bound to cursor; full-number/free-text search forbidden. |
| §4.9 / §4.11 sensitive projections | Adds P1 `ORGANIZATION_MEMBER` gating (challenge metadata only, `content` omitted until tenant/member authorization); marks the three media/upload credentials `readOnly` + `x-onecall-log-redaction: required` without return-once; adds generated-type assertions. |
| §5.1 customer rows | C4 exact `CLAIM_*` codes + `VALIDATION_FAILED` 422; C6 exact `OUTBOUND_*` codes; C8 exact `OUTBOUND_TASK_NOT_CANCELLABLE` / `OUTBOUND_TASK_CONFLICT`; C10 exact `PURCHASE_CONSENT_*` codes + `VALIDATION_FAILED`. |
| §5.2 operations rows | O1 sort/filter/cursor codes; O4 `ACTIVATION_STATE_CONFLICT`; O11 `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW`; O14 `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW`; O18-O21 remove `Idempotency-Key` and `platform.idempotency_record`, define state-conditional success/conflict behavior, and remove the O20 temporary-publisher claim in favour of `R-A0-O20-PUBLISHER`. |
| §5.4 / §5.6 components | P1 member-mode variant documented; `PublicShareView.content` optional with `verificationChallenge`; `RecordingDeclaration`, `ShareAdmission`, `MediaAuthorization` credential annotations; `TrialPolicyDraft` notes the runtime prerequisite and never exposes the publisher field. |
| §5.5.1 inventory audit | `servers` item tightened to required exactly-one relative `url: /`. |
| §7.1 A0 mapping | DRAFT group removes `platform.idempotency_record` and the temporary publisher construction; records natural identity/conditional revision and `R-A0-O20-PUBLISHER`. The `Problem/...` row limits `platform.idempotency_record` to approved-scope order paths and excludes O18-O21. |
| §7.2 open inputs | Adds `R-A0-O20-PUBLISHER` with owner/destination (A0 schema/catalog-order/entitlement-metering before runtime O20 implementation). |
| §7.3 residual risks | Adds the O20 publisher persistence mismatch and owner. |
| §8.2 / §8.3 / §8.4 slice validation | Replaced final-only `npm run check` expectations with `npm run check:s1` / `check:s2` / `check:s3` and exact per-stage operationId sets/counts; S2 completion/stop now carries `R-A0-O20-PUBLISHER`. |
| §9.2 commands/assertions | Adds the stage scripts; stage-set/count row; server exactness; expanded Problem rows; credential annotations; O1 cursor fixtures; P1 member gating; DRAFT no-key/no-platform-scope negative fixtures; stage-aware versioned-path row |
| §9.3 rules | Rules 1, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14 now enforce the exact codes, exact servers, credential annotations, O1 cursor, P1 gating, O18-O21 state-conditional semantics and stage-aware path/count checks. |
| §9.4 / §10.3 / §11 | Record the O20 runtime prerequisite and no-runtime claim; final report includes `R-A0-O20-PUBLISHER`; plan completion definition references plan-fix-3. |

## 3. F-10 disposition — customer task Problem codes

**Status: fixed.**

- C6 now returns `409 OUTBOUND_CALL_IN_PROGRESS` for an existing same-subject/canonical-caller `ACTIVE`/`UNKNOWN` call (current `callId` excluded by the guard) and `409 OUTBOUND_TASK_CONFLICT` for an incompatible existing/nonterminal task or same action identity with changed semantics.
- C8 now returns `409 OUTBOUND_TASK_NOT_CANCELLABLE` when the task has been claimed or is otherwise no longer `PENDING`, and `409 OUTBOUND_TASK_CONFLICT` for same-action changed semantics.
- `TASK_CONFLICT`, `CALL_IN_PROGRESS`, `TASK_CLAIMED` and `TASK_UNAVAILABLE` are removed from customer Problem mappings and are prohibited as Problem codes by the registry strategy, §6.1, §9.2 and §9.3 rules 3/12. They remain valid only in `IvrDecision.reasonCode` (A0 registry `docs/14` §2.9 L220) and the existing IVR decision rows I1-I3.
- Registry, docs/15 requirements and validation now name the three exact customer codes; wildcard/placeholder error summaries are rejected.

## 4. F-11 disposition — DRAFT idempotency and A0 mapping

**Status: fixed.**

### Why no new platform idempotency scope is allowed (plan + A0 evidence)

- `docs/14` §2.9 L222 states: "`platform.idempotency_begin`/`complete` are used by S2 with scope `IVR_CONFIRM` ... and `PURCHASE` ... The scope registry remains exactly `IVR_CONFIRM` and `PURCHASE`; task creation uses its own `(created_by_account_id, idempotency_key)` key and direct authorization uses the call/purpose decision, not a third platform scope."
- `docs/14` §4.3 L469/L483 defines `ck_idempotency_scope`: `scope IN ('IVR_CONFIRM','PURCHASE')`; a new scope requires an approved later migration.
- `docs/14` §9.26 T-S2-14 only publishes an already-existing `DRAFT`/never-published catalog version or trial-policy version; it defines no draft create/replace platform transaction. The plan therefore cannot claim an A0 transaction or stored fingerprint for O18-O21.
- Consequently the plan now uses the existing natural unique identities and conditional revision semantics only:
  - O18 create: natural key `(product_id, version_no)`; O20 create: `(campaign_key = FIRST-TRIAL, version_no)`; canonical content projection derived from persisted fields.
  - O19/O21 replace: `expectedRevision` + requested/current mutable-content projection; one conditional update/revision increment on the first matching revision.
- Initial/replay status is deterministic: O18/O20 first insert `201`, identical same-identity replay `200` with the existing DRAFT; O19/O21 success or proven-applied replay `200`. Different content under the same natural identity returns the existing `CATALOG_DRAFT_CONFLICT` / `TRIAL_POLICY_DRAFT_CONFLICT`; a stale/unprovable revision returns `CATALOG_DRAFT_STALE_VIEW` / `TRIAL_POLICY_DRAFT_STALE_VIEW`; non-DRAFT/published/immutable-content changes return `*_DRAFT_CONFLICT`.
- O18-O21 inventory rows and §7.1 no longer list `platform.idempotency_record`; they declare no `Idempotency-Key`; `x-onecall-idempotency = state-conditional`; `x-onecall-a0-contracts` references only `uq_catalog_version_no`, `uq_trial_policy_campaign_version`, conditional revision and the §4.17/§4.29 publication guards.
- Negative fixtures now require rejection of `platform.idempotency_record` on O18-O21, any new/unapproved scope (`CATALOG_DRAFT`, `TRIAL_POLICY_DRAFT`), and any `Idempotency-Key` on O18-O21.
- No benefit-line or channel-set mutation is newly authorized: O19/O21 still operate only on existing mutable draft content; benefit lines/channels remain append-only/immutable.

### Why publisher semantics remain a runtime prerequisite (plan + A0 evidence)

- `docs/14` §4.29 L1608 defines `published_by_employee_id` as "Publishing employee" and it is `NOT NULL`; L1630 states it and `published_at` are immutable once published and that publication is one owner `UPDATE` that sets `published_by_employee_id` to "the actual authenticated publishing employee" while `OLD.published_at IS NULL`: "It is a publisher fact, not a draft-owner label".
- `docs/14` §9.26 step 5 repeats that the publication `UPDATE` sets the actual authenticated publisher and that "no separate pre-publication publisher statement is required". A0 defines no nullable/draft creator column and no draft-creation value for this field.
- A1 therefore must not expose the field, rename it as a draft creator, or fill it with the creating employee. The plan records `R-A0-O20-PUBLISHER` as an A0 schema/catalog-order/entitlement-metering prerequisite before runtime trial-policy draft creation. O20 remains in the OpenAPI inventory as the public contract only; the plan makes no runtime insert claim, and S2 stop/completion reporting must carry the prerequisite.

## 5. F-12 disposition — O1 pagination

**Status: fixed.**

- O1 sort is exactly `createdAt desc, subjectId desc` using A0 `business_subject.created_at` and the stable `subjectId` tie-break.
- Documented structured filters are limited to already exposed/A0-backed fields: `subjectId`, `source`, `claimStatus`, `activationStatus`, `realNameStatus`, `riskState`. Full-number and free-text search remain forbidden.
- The normalized filter digest is bound into the cursor; reusing a cursor under different filters fails closed.
- O1 enumerates `CURSOR_INVALID` and `CURSOR_FILTER_MISMATCH`.
- §9.2 adds static/fixture assertions for filter mismatch and deterministic tie-break; §9.3 rule 4 checks the O1 sort/filter digest.

## 6. F-13 disposition — slice-aware validation

**Status: fixed.**

- Shared `onecall/versioned-paths` always enforces `/api/v1` approved-prefix membership, uniqueness, absence of `/api/v2` and absence of unversioned routes. It no longer hard-codes the final 49 counts.
- `check-doc-map.mjs` supports `--stage s1|s2|s3` and asserts the exact operationId set and surface counts for that stage:
  - S1: `C1,C2,C3,C4,C5,C13,C14,O1..O7`; 7/7/0/0/0 = 14.
  - S2 cumulative: S1 + `C9..C12,O8..O21`; 11/21/0/0/0 = 32.
  - S3 final: all C1-C18/O1-O21/I1-I6/P1-P3/K1; 18/21/6/3/1 = 49.
- `package.json` must provide fixed `npm run check:s1`, `check:s2`, `check:s3` scripts. A wrong stage set or count exits non-zero.
- §8.2/§8.3/§8.4 validation commands and §9.2 expected assertions now use the stage-aware commands, and the final document-level 49-row audit remains a separate §5.5.1 check.

## 7. Accepted clarification dispositions

| Clarification | Disposition |
| --- | --- |
| `servers` required with exactly one relative `url: /`; reject missing/absolute/additional roots | Applied in §4.1, §5.5.1 item 4, §9.2 Redocly/versioned-path rows and §9.3 rule 7. |
| C4 exact codes; invalid proof `VALIDATION_FAILED` 422 | C4 now lists `CLAIM_RISK_BLOCKED`, `CLAIM_REPLAY_CONFLICT`, `CLAIM_CONTEXT_CONFLICT` and `VALIDATION_FAILED`; registry rows split exactly. |
| C10 exact purchase-consent codes | C10 now lists `PURCHASE_CONSENT_REQUIRED`, `PURCHASE_CONSENT_STALE`, `PURCHASE_CONSENT_DECLINED`; registry/validation updated. |
| Replace bare 409s: O4, O11, O14 | O4 = `ACTIVATION_STATE_CONFLICT`; O11 = `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW`; O14 = `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW`. |
| Exact error enumeration; reject wildcard/bare conflict placeholders | §9.2 and §9.3 rule 3 require exact codes in registry/docs/15, forbid `*`/`_*` and bare 409; generic 401/403/404/422/429/500/503 remain the documented common set. |
| Credential annotations | `RecordingDeclaration.upload.credential`, `ShareAdmission.mediaAuthorization.credential`, `MediaAuthorization.credential` are `readOnly: true` + `x-onecall-log-redaction: required`; no return-once is claimed. §4.9, §4.11, §5.6 and §9.2/§9.3 rule 11 enforce it. |
| `ORGANIZATION_MEMBER` P1 gating | P1 without a matching authenticated customer session returns only verification/challenge metadata and omits `content`; content only after matching tenant/member authorization. `NONE`/code/mobile public page behavior and P2/P3 witness checks are preserved. §4.9, §5.4, §5.6, §9.2 and §9.3 rule 13 enforce it. |
| O20 publisher semantics | Temporary-publisher/creator language removed; `R-A0-O20-PUBLISHER` recorded in §5.2, §7.1, §7.2, §7.3, §8.3, §9.4 and §10.3. No exposure, renaming, A0 change or runtime claim. |

## 8. Plan-level validation performed

| Check | Result |
| --- | --- |
| Inventory/path/count audit | 49 anchored operation rows: customer=18, operations=21, IVR=6, public-share=3, provider-callback=1. 51 `METHOD /path` tokens = 49 rows + 2 versioned prose examples; unversioned=0; `/api/v2`=0; no row added/removed/renamed. |
| OperationId audit | 18 C rows + 21 O rows + 6 I rows + 3 P rows + 1 K row = 49; no duplicate IDs or routes introduced. |
| Slice audit | 14 + 18 + 17 = 49; S1/S2/S3 sets match the plan and the new stage assertions. |
| Prohibited IVR reasons as customer Problem | No customer/operations Problem mapping uses `TASK_CONFLICT`/`CALL_IN_PROGRESS`/`TASK_CLAIMED`/`TASK_UNAVAILABLE`; remaining occurrences are IVR decision reasons or explicit prohibitions. |
| O18-O21 A0/idempotency | No O18-O21 inventory/§7.1 entry contains `platform.idempotency_record` or `Idempotency-Key`; both are stated as forbidden and covered by negative fixtures. |
| Exact error enumeration | No `CLAIM_*`/`PURCHASE_CONSENT_*`/bare O4/O11/O14 409 remains; C4/C6/C8/C10/O4/O11/O14 use exact registry codes; wildcard placeholders are rejected by §9.2/§9.3. |
| Stage command/count consistency | `check:s1/s2/s3` commands, §8 validations and §9.2 assertions all use 14/32/49 and the same exact sets; shared path rule is count-free. |
| Server exactness | §4.1, §5.5.1, §9.2 and §9.3 rule 7 require exactly one relative `url: /` and reject missing/absolute/additional roots. |
| Credential annotations / P1 gating | Three credentials annotated `readOnly` + log-redaction without return-once; P1 member-mode content omission and challenge metadata specified consistently. |
| Encoding/whitespace | `file -bi` reports `text/plain; charset=utf-8`; trailing-whitespace scan `[ \t]+$` = 0 matches; conflict-marker scan `^(<<<<<<<|=======|>>>>>>>)$` = 0 matches. |
| `git diff --check` | `git diff --check -- docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` completed with no output. Content-level checks were also run because both edited paths are untracked. |
| Worktree boundary | This task changed only the plan and this new artifact. Pre-existing modified screenshots/prototypes/controller and untracked historical control files remain outside scope and untouched. |

## 9. Residual risks and ownership

| Risk | Owner / destination |
| --- | --- |
| `R-A0-O20-PUBLISHER`: A0 `published_by_employee_id` is a NOT NULL publisher fact with no draft-creation value, so runtime O20 draft insert is not implementable before an A0 schema/catalog-order/entitlement-metering resolution. A1 only freezes the public contract and static mapping. | A0 schema/catalog-order/entitlement-metering; S2 completion report; later runtime trial-policy draft implementation. |
| O18/O19 draft create/replace concurrency, natural-key uniqueness and canonical projection are contract statements, not runtime-verified. | Backend/catalog-order; S2 implementation and integration tests. |
| O1 filter digest/tie-break and cursor behavior need real-data/concurrency verification. | Backend/QA; O1 implementation tests. |
| Stage sets/counts and custom Redocly rules are specified but not executed here (no npm/Redocly/tsc run in this plan-only task). | A1 implementation slices; `check:s1/s2/s3` evidence in each slice report. |
| Customer/employee credentials, mTLS/signature, capability-token resolution, media-gateway credential validation and log-redaction enforcement remain runtime/security unproven. | Security/Backend/Sharing; Q1/Q6/Q7/Q19/Q22 and security design. |
| Pre-existing dirty screenshots/prototypes/controller artifacts are outside A1. | Controller; preserve untouched. |

## 10. Scope confirmation

No OpenAPI source, runtime service, database/schema, Flyway migration, UI/prototype, screenshot, controller, review, docs 09-14 or other path was edited by this task. No staging, commit, push, PR, agent invocation or later gate was performed. Both edited paths are untracked plan artifacts; `git diff --check` and content-level UTF-8/trailing-whitespace/conflict-marker checks pass.
