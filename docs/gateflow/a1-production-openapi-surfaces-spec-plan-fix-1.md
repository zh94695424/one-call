# A1 Production OpenAPI Surfaces Spec — bounded plan-fix 1

> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: `fix` (plan-only correction; no runtime implementation)
>
> Basis: independent plan review `docs/reviews/plan-review-20261023-162543.md` (F-01..F-08), controller adjudication and bounded plan-fix requirements in `docs/gateflow/project-execution-control.md`, accepted A0 truth in `docs/14-production-data-model-and-flyway-schema-spec.md`, and docs 09/10/11/12/13.
>
> Result: revised `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md`; this new fix artifact. No other repository path was edited by this task.

## 1. Exact edits made

| Area | Edit |
| --- | --- |
| Plan header / status | Changed the gate line to `fix` (bounded plan-fix-1), added the review/fix-artifact reference, and kept the plan-only/no-runtime declaration. |
| §0 summary | Changed 47 to 49 operations; removed support-entry from the customer/operations API descriptions; narrowed catalog scope to four bounded DRAFT candidate operations; recorded offline support as UI/deployment configuration. |
| §1 goals / non-goals / drift | Removed C19/O18 support APIs; added the offline-support non-API requirement; bounded catalog draft work; updated the drift table for IVR decisions, DRAFT candidates, and the no-anonymous-support rule. |
| §1.3 success signals | Made `x-onecall-a0-models` exact schema-qualified matching mandatory, with negative fixtures and no tableless API exception. |
| §4.2 / §4.3 / §4.9 / §4.11 | Removed the anonymous support exception; corrected response/request secret direction (`readOnly` + `x-onecall-return-once` for response secrets, `writeOnly` for incoming verification secrets); added `ShareCodeRotation` and generated-type assertions. |
| §4.5 / §4.6 | Removed `CONFIRMATION_NOT_ALLOWED` from the Problem registry; added `ACTIVATION_NOT_ACTIVE`, `CATALOG_DRAFT_CONFLICT`, `CATALOG_DRAFT_STALE_VIEW`, `TRIAL_POLICY_DRAFT_CONFLICT` and `TRIAL_POLICY_DRAFT_STALE_VIEW`; documented normal IVR decisions as `200 IvrDecision`. |
| §4.7 / §4.8 | Added idempotency rows for DRAFT candidate create/replace and P2 ticket replay; added the C9 deterministic sort and filter-digest cursor; made P1 observation-free; labelled pagination defaults as V1 API-governance defaults. |
| §5.1 customer | Customer count 19 → 18; removed C19; added C6 active-activation/no-other-active-call predicates; added C9 sort/cursor; added C10 `ACTIVE` activation predicate and `403 ACTIVATION_NOT_ACTIVE`; changed C18 to `ShareCodeRotation`; canonicalized all A0 names. |
| §5.2 operations | Operations count 18 → 21; removed O18 support entry; canonicalized O1-O17 A0 names; added O18-O21 DRAFT catalog/trial candidate create/replace operations. |
| §5.3 IVR | I1/I2/I3 normal domain outcomes are `200 IvrDecision`; 409 is limited to identity/fingerprint/access-number conflicts that prevent a frozen decision; canonicalized A0 names. |
| §5.4 public share | P1 observation-free; P2 ticket-failure replay/fail-closed semantics; P3 is credential-issuance/reconstruction control plane, allows an already-admitted `LOCKED` session, and no longer lists `SHARE_LOCKED`; canonicalized A0 names; clarified secret direction. |
| §5.6 components | Removed `SupportEntry`; added `ShareCodeRotation`, `CatalogVersionDraft`, `TrialPolicyDraft`; corrected share/secret annotations. |
| §6 IVR boundary | Added explicit 200-vs-409 mapping, confirmation-not-allowed/consent-required as frozen decisions, and direct non-dial results as `200 IvrDecision`; changed internal callee payload to response-side `readOnly` + internal marker. |
| §7.1 A0 mapping | Replaced shorthand/wildcards with exact `docs/14` catalog names; added the complete referenced A0 set; added DRAFT candidate mapping; removed the `SupportEntry` row. |
| §7.2 inputs | Preserved Q1..Q28; added N-1 product provisioning, N-2 offline support configuration, and API-DEF-1 pagination defaults with owners/destinations. |
| §7.3 risks | Added DRAFT candidate, exact-name parser, pagination-default, and support-configuration risks. |
| §8 slices | Reallocated operations: S1 14, S2 18, S3 17; updated cumulative counts and stop conditions. |
| §9 validation | Added A0 exact-name, negative-fixture, secret-direction, IVR-status, share-semantics and DRAFT-candidate assertions; added custom rules 11-14. |
| §10 / §11 | Updated no-overdesign/final-report counts to 49; updated the completion definition to the bounded plan-fix/re-review gate. |

## 2. Finding disposition

| Finding | Verdict | Correction applied |
| --- | --- | --- |
| F-01 | Resolved | C19 and O18 are removed. Customer is 18 and operations is 21 after F-06. Offline support contact/instructions are UI/deployment configuration with no A0 table and no anonymous API boundary (N-2). |
| F-02 | Resolved | Every `x-onecall-a0-models` item in §5 and §7.1 is an exact schema-qualified name from the 44-table catalog, including all required `catalog_order.*` tables. §1.3, §9.2 and §9.3 require exact-set matching and reject short names, wildcards and wrong-schema names. Negative fixtures: `usage_reservation`, `catalog_*`, `sharing.catalog_version`, `catalog_order.share_access_event`. |
| F-03 | Resolved | C18 now returns `ShareCodeRotation` with `shareId`, `verificationMode`, `status`, `codeGeneration`, `revocationGeneration`, and one-time `extractionCode`; it never returns or reconstructs `capabilityToken`. Generated-type assertion required in §4.11/§9.2/§8.4. |
| F-04 | Resolved | I1/I2/I3 return `200 IvrDecision` for normal domain results, including `REJECT_CALL_IN_PROGRESS`, `REJECT`/`TASK_CONFLICT`, `CONFIRMATION_NOT_ALLOWED`, `CONSENT_REQUIRED`, and direct non-dial outcomes. 409 is limited to identity/fingerprint/access-number conflicts that prevent a frozen decision. `CONFIRMATION_NOT_ALLOWED` is no longer a Problem code. |
| F-05 | Resolved | C10 requires current `service_activation.status = ACTIVE` and defines `403 ACTIVATION_NOT_ACTIVE`. C6 requires current active activation and no other `ACTIVE`/`UNKNOWN` call for the same canonical caller/subject before creating a task. |
| F-06 | Resolved | Added O18 `createOperationsCatalogVersionDraft`, O19 `replaceOperationsCatalogVersionDraft`, O20 `createOperationsTrialPolicyDraft`, O21 `replaceOperationsTrialPolicyDraft`. They are DRAFT/never-published only; benefit lines remain A0 append-only; published content is immutable; no generic CRUD, approval engine or independent offer withdrawal. Counts recomputed. |
| F-07 | Resolved | C9 uses `productCode asc, versionNo desc, catalogOfferId asc` and binds its opaque cursor to the normalized `channel + audience + offerType` filter digest; `CURSOR_FILTER_MISMATCH` is defined. |
| F-08 | Resolved | P2 new admission still rejects `SHARE_LOCKED`. P3 accepts an already-admitted valid session under `LOCKED` subject to current generation/expiry/risk/member-witness checks; `SHARE_LOCKED` is removed from P3 errors. P3 returns a credential for the media gateway to validate. |

Plan-shape clarifications also applied: P1 is observation-free; P2 ticket failure after committed admission is fail-closed `503` and same-key replay reissues the ticket without recounting; P3 is the credential-issuance/reconstruction control plane with `ORGANIZATION_MEMBER` bound to the admitted recipient witness; response secrets are `readOnly` + `x-onecall-return-once` and incoming verification secrets are `writeOnly`; `limit default=20, max=100` is labelled an API-governance default.

## 3. Recomputed endpoint inventory and slice allocation

| Surface | Count | Operation IDs |
| --- | --- | --- |
| Customer `/api/v1/customer/**` | 18 | C1-C18 |
| Operations `/api/v1/operations/**` | 21 | O1-O21 |
| Self-built IVR `/api/v1/ivr/**` | 6 | I1-I6 |
| Public recording shares `/api/v1/public/shares/**` | 3 | P1-P3 |
| Authenticated provider callbacks `/api/v1/callbacks/**` | 1 | K1 |
| **Total** | **49** | unique operationId union |

Slice allocation after correction:

| Slice | Operations | Count | Cumulative |
| --- | --- | --- | --- |
| S1 — base plus human identity/resource/activation truth | C1/C2/C3/C4/C5/C13/C14 + O1/O2/O3/O4/O5/O6/O7 | 7 customer + 7 operations = 14 | 14 |
| S2 — server pricing/order/catalog/trial control | C9/C10/C11/C12 + O8-O21 | 4 customer + 14 operations = 18 | 32 |
| S3 — external execution/capability/callback convergence | C6/C7/C8/C15/C16/C17/C18 + I1-I6 + P1-P3 + K1 | 7 customer + 6 IVR + 3 public-share + 1 callback = 17 | 49 |

Arithmetic: 18 + 21 + 6 + 3 + 1 = 49; 14 + 18 + 17 = 49. The prior 47 count is obsolete.

## 4. Evidence from the listed sources

- **F-01 / offline support.** docs 12 §1.1 requires a unified offline support entry, contact and handling instructions; docs 10 §6 defines `mode=OFFLINE`, `configured=false`, and explicitly forbids online submission/approval/receipt and invented contacts. Neither source defines an A0 business table or an anonymous API boundary. The plan now records this as UI/deployment configuration (N-2).
- **F-02 / exact A0 names.** docs 14 §4.1 catalog block (L356-403) lists the exact 44 schema-qualified names. The plan now uses `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `entitlement_metering.usage_reservation`, `sharing.share_access_event`, and other exact names; no wildcard/short name remains in an A0 column.
- **F-03 / rotation.** docs 14 T-S3-03 step 4 (L3386) says there is no token reissuance/reconstruction; step 6 (L3388) says code rotation replaces salt/hash, increments `code_generation` and `revocation_generation`, clears `LOCKED` to `ACTIVE` as the explicit unlock, and appends `CODE_ROTATED`. The new `ShareCodeRotation` response reflects that and excludes the token.
- **F-04 / IVR HTTP.** docs 14 T-S2-03 step 5 (L3191) freezes `REJECT_CALL_IN_PROGRESS` with a terminal current call and no claim/reservation; T-S2-04 steps 3/5 (L3203-3205) allow a non-dial direct refusal and define a successful direct authorization as purpose `DIRECT_AUTHORIZATION` with `action` null. docs 13 §6.3 (L212-222) says the IVR only executes the response action. The plan now maps these to `200 IvrDecision`; only decision-blocking identity/fingerprint conflicts are 409 Problem.
- **F-05 / activation.** docs 12 §9.4 (L325) requires current business-activation status at order creation; docs 14 T-S2-07 step 1 (L3236) requires current active service activation. docs 13 §6.1 (L138) requires activation status and no active call for task creation; docs 14 T-S2-02 step 1 (L3177) requires no other `ACTIVE`/`UNKNOWN` call for the canonical caller. The plan now states both predicates and adds `ACTIVATION_NOT_ACTIVE`.
- **F-06 / DRAFT candidates.** docs 09 §5 (L103-104) approves “编辑新版本草稿、确认发布、上下架” and “额度、有效天数、启用”. docs 14 §4.17/§4.18/§4.19 allow mutable draft version/offer content with append-only benefit lines; §4.29 allows a never-published trial-policy version; T-S2-14 steps 3/5 (L3347/L3349) require the candidate to already exist as `DRAFT`/never-published. The four bounded operations create/replace only those candidates and leave publication/offsale/disable in O10/O11/O13/O14.
- **F-07 / cursor.** docs 12 §8 (L283) requires stable sorting and cursor pagination; the plan now gives C9 a deterministic three-key sort and a normalized filter digest.
- **F-08 / share LOCKED.** docs 14 §7.4 (L2808-2809) says `LOCKED` blocks new admission but permits an existing admitted session’s replay/reconstruction; T-S3-04 steps 3/5 (L3395/L3397) deny a new admission under `LOCKED` and resolve an existing admission before the limit; T-S3-05 steps 3/4 (L3407-3408) reissue/revalidate only for an already-admitted session and recheck generation/expiry/risk/member. P3 now follows this and no longer returns `SHARE_LOCKED`.
- **P1/P2/P3 clarifications.** docs 14 T-S3-04 step 1 (L3393) allows observations but does not require them; the plan chooses observation-free P1 to avoid an undefined GET identity. Step 6 (L3398) says ticket failure never rolls back the admission or increments the count and a lost response reissues the ticket; the plan uses fail-closed 503 and same-key reissue. T-S3-05 (L3405-3408) defines ticket/credential reconstruction and the media gateway’s validation role.
- **Secret typing.** docs 14 T-S3-03/T-S3-05 keep plaintext token/code out of events, audit and logs; the plan uses response-only `readOnly` + `x-onecall-return-once` and request-only `writeOnly`, with generated-type assertions.

## 5. Intentionally unresolved inputs / owners

The 15 named inputs remain unchanged with their owners/destinations: Q1 (Security/Backend), Q6 (Payments/Backend), Q7 (Telephony/IVR), Q8 (IVR/Backend/QA), Q10 (Product/Legal), Q12 (Security/Product), Q14 (Security/Product), Q15 (Backend/Security), Q18 (Product/Backend), Q19 (Identity/Backend), Q22 (Security/Sharing), Q25 (IVR/Entitlement/Backend), Q26 (IVR/Backend/Security), Q27 (Organization-access/Security), Q28 (Telephony/IVR). Other A0 inputs remain with docs 14 §14.

New plan-level inputs recorded in §7.2:

| ID | Input | Owner | Destination |
| --- | --- | --- | --- |
| N-1 | Initial supply of immutable `catalog_product` identities | Product/Operations/catalog-order | Catalog operational data provisioning; no product-create API in A1 |
| N-2 | Offline support contact/instructions values | Product/Operations | Deployment configuration and three-end UI; no API/A0 table |
| API-DEF-1 | V1 pagination defaults `limit=20`, `max=100` | Backend/API governance | Final OpenAPI description and docs/15; not security/product policy |

`customer_iam.auth_identity` is deliberately not an operation business model; it stays behind the customer authentication/session boundary (Q19). The other 43 A0 tables are explicitly listed in §7.1.

## 6. Plan-only validation performed

| Check | Method | Outcome |
| --- | --- | --- |
| Exact operation inventory | Counted table rows by prefix in §5: C=18, O=21, I=6, P=3, K=1 | PASS; 49 unique IDs; no duplicate row IDs in the inventory |
| Count arithmetic | Per-surface sum and slice allocation sum | PASS: 18+21+6+3+1=49 and 14+18+17=49 |
| A0 model-name audit | Compared every A0 cell in §5 and every A0-table cell in §7.1 against the docs 14 44-table catalog block | PASS: all entries are exact schema-qualified names; no short name, wildcard or wrong-schema name remains in an A0 mapping; all seven `catalog_order.*` tables are explicitly used; 43 catalog tables are referenced and `customer_iam.auth_identity` is intentionally session-only |
| A0 negative fixtures | Plan now specifies rejection fixtures for `usage_reservation`, `catalog_*`, `sharing.catalog_version`, `catalog_order.share_access_event` | Defined in §9.2/§9.3/§10.3; implementation must fail them |
| Path/scope audit | Checked every operation path against `/customer/`, `/operations/`, `/ivr/`, `/public/shares/`, `/callbacks/`; searched for support-entry paths | PASS: all paths belong to the five surfaces; no `getCustomerSupportEntry`, `getOperationsSupportEntry`, or `SupportEntry` API remains |
| IVR 409 audit | Inspected I1/I2/I3 error cells for 409 tails | PASS: 409 lists only identity/fingerprint/access-number conflicts; no `TASK_CONFLICT`, `CALL_IN_PROGRESS`, `CONFIRMATION_NOT_ALLOWED` or `CONSENT_REQUIRED` after 409 |
| Secret-direction audit | Inspected `ShareCreated`, `ShareCodeRotation`, `ShareAdmission`, §4.3/§4.9/§4.11/§9.2 | PASS at plan level: response secrets use `readOnly` + `x-onecall-return-once`; incoming code uses `writeOnly`; `ShareCodeRotation` has no `capabilityToken`; generated-type assertions specified |
| Share semantics audit | Inspected P1/P2/P3 and §4.7/§4.8/§9.2 | PASS at plan level: P1 observation-free; P2 ticket failure 503/replay; P3 `LOCKED` allowed for existing admission and `SHARE_LOCKED` absent from P3 errors |
| Whitespace | `git diff --check -- docs/gateflow/a1-production-openapi-surfaces-spec-plan.md docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-1.md`; plus a trailing-whitespace scan of the revised plan | `git diff --check` returned no output. The plan was untracked at task start, so the check cannot show its contents; a `[ \\t]+$` scan over the revised plan returned no matches. This artifact is new/untracked. |
| Worktree boundary | `git status --porcelain` for the two allowed paths; no other path edited by this task | Plan is untracked/modified as expected; this fix artifact is new. Pre-existing modified screenshots, prototype HTML, controller file, docs, and historical review artifacts were not touched by this task. |

No Redocly, `openapi-typescript`, `tsc`, npm, service, database, or generated client was executed. The validations above are document/static checks only.

## 7. Residual risks

- N-1: A fresh deployment still needs initial `catalog_product` identities; A1 deliberately provides no product-create endpoint.
- The four DRAFT candidate operations are bounded by A0 table semantics, not by a new named A0 transaction. Implementation must enforce DRAFT/never-published state, append-only benefit lines, server-derived tokens/snapshots, and the trial-policy provisional `published_by_employee_id`/`enabled=false` insert rule.
- P3 media credential format/TTL (Q22), the media gateway validation, P1 observation-free behavior, P2 ticket failure and P3 `LOCKED` continuation are contract statements; integration tests must prove them.
- Exact A0 name validation depends on parser stability of the docs 14 catalog block; no runtime/concurrency validation was performed.
- IVR `CONFIRMATION_NOT_ALLOWED`/`CONSENT_REQUIRED` as frozen 200 decisions requires the implementation to write a deterministic non-dial decision with no activation/trial side effects; it is not independently reviewed at runtime here.
- Offline support contact/instructions remain a deployment/UI value; no anonymous API or A0 table exists.

## 8. Runtime / repository boundary confirmation

- No runtime implementation, backend service, Flyway migration, database change, UI/prototype change, payment/identity integration, or generated client was produced.
- No file other than `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` and this new fix artifact was edited by this task.
- No stage, commit, push, PR, merge, or later-gate action occurred.
- This fix is ready for a fresh independent plan re-review; implementation remains unauthorized until that re-review passes.
