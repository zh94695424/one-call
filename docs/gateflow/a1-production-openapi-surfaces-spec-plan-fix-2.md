# A1 Production OpenAPI Surfaces Spec — bounded plan-fix 2 (F-09)

> Work unit: `A1-production-openapi-surfaces-spec`
>
> Gate: `fix` (plan-only correction; no runtime implementation)
>
> Basis: controller-accepted finding F-09 in `docs/gateflow/project-execution-control.md`, independent plan review `docs/reviews/plan-review-20261023-162543.md` (F-01..F-08), and the plan-fix-1 result in `docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-1.md`.
>
> Result: the 49-row versioned inventory and every route example in `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` now use their exact `/api/v1/...` path with `servers: /` unchanged; this new fix artifact. No other repository path was edited by this task.

## 1. F-09, cause and exact edit scope

**F-09.** The plan requires every contract path to start with `/api/v1/` while `servers` is the relative root `/`; however, all 49 inventory rows and one prose route example used unversioned paths such as `/customer/me` and `/operations/...`. With `servers: /` unchanged, those paths would resolve as `/customer/me` rather than `/api/v1/customer/me`.

**Cause.** Plan-fix-1 recomputed the inventory and added/changed rows and examples, but its path audit only checked that each path belonged to one of the five surfaces (`/customer/`, `/operations/`, `/ivr/`, `/public/shares/`, `/callbacks/`). That surface-membership check passed unversioned paths, so the §4.1 versioned-path rule was never applied row by row.

**Exact edit scope (plan only).**

| Plan area | Edit |
| --- | --- |
| Header / correction basis | Gate line records bounded plan-fix-2 / F-09; correction basis adds the controller-accepted F-09 reference and states F-01..F-08 are unchanged. |
| §4.1 contract root | Clarified that `/api/v1` must appear in every OpenAPI `paths` entry and every §5 operation path, not only in a server/base URL prefix; `servers: /` remains the relative deploy root. |
| §4.9 route example | `GET /operations/evidence` prose reference changed to `GET /api/v1/operations/evidence`. |
| §5.1-§5.5 inventory | Inserted `/api/v1` before each of the 49 existing method/path strings. No operationId, method, suffix, ordering, count, schema, A0 mapping, error set, idempotency rule or surface assignment changed. |
| §5.5.1 (new) | Plan-level static versioned-path audit: five approved prefixes, single-surface ownership, 18/21/6/3/1=49 arithmetic, `servers: /` retention, and zero unversioned operation paths/examples. |
| §9.2 assertions | Added the F-09 row for inventory and OpenAPI `paths` extraction, counts, unversioned/duplicate/multi-surface zeroes, relative `servers`, and versioned prose examples. |
| §9.3 rule 8 | `onecall/versioned-paths` now asserts the five exact prefixes and the 18/21/6/3/1=49 counts, with no unversioned paths or duplicate `method + path`. |
| §11 completion definition | Records plan-fix-2 as the current bounded fix and requires all paths to use `/api/v1`; F-01..F-08 results unchanged. |

No operation ID, operation count (49), per-surface count (18/21/6/3/1), slice allocation (14/18/17), HTTP semantics, request/response schema, A0 mapping, policy input, design scope or non-goal changed. F-01..F-08 from plan-fix-1 are unchanged.

## 2. Complete versioned inventory (49 rows)

Customer surface — 18.

| # | operationId | Method | Exact path |
| --- | --- | --- | --- |
| C1 | `getCustomerMe` | GET | `/api/v1/customer/me` |
| C2 | `listCustomerBusinessNumbers` | GET | `/api/v1/customer/business-numbers` |
| C3 | `getCustomerSubject` | GET | `/api/v1/customer/subjects/{subjectId}` |
| C4 | `createCustomerSubjectClaim` | POST | `/api/v1/customer/subject-claims` |
| C5 | `createCustomerSubjectActivation` | POST | `/api/v1/customer/subjects/{subjectId}/activations` |
| C6 | `createCustomerOutboundCallTask` | POST | `/api/v1/customer/outbound-call-tasks` |
| C7 | `getCustomerOutboundCallTask` | GET | `/api/v1/customer/outbound-call-tasks/{taskId}` |
| C8 | `cancelCustomerOutboundCallTask` | POST | `/api/v1/customer/outbound-call-tasks/{taskId}/cancellations` |
| C9 | `listCustomerCatalogOffers` | GET | `/api/v1/customer/catalog/offers` |
| C10 | `createCustomerOrder` | POST | `/api/v1/customer/orders` |
| C11 | `listCustomerOrders` | GET | `/api/v1/customer/orders` |
| C12 | `getCustomerOrder` | GET | `/api/v1/customer/orders/{orderId}` |
| C13 | `listCustomerEvidence` | GET | `/api/v1/customer/evidence` |
| C14 | `getCustomerEvidence` | GET | `/api/v1/customer/evidence/{evidenceId}` |
| C15 | `createCustomerShare` | POST | `/api/v1/customer/shares` |
| C16 | `listCustomerShares` | GET | `/api/v1/customer/shares` |
| C17 | `revokeCustomerShare` | POST | `/api/v1/customer/shares/{shareId}/revocations` |
| C18 | `rotateCustomerShareCode` | POST | `/api/v1/customer/shares/{shareId}/code-rotations` |

Operations surface — 21.

| # | operationId | Method | Exact path |
| --- | --- | --- | --- |
| O1 | `listOperationsSubjects` | GET | `/api/v1/operations/subjects` |
| O2 | `getOperationsSubject` | GET | `/api/v1/operations/subjects/{subjectId}` |
| O3 | `suspendOperationsSubject` | POST | `/api/v1/operations/subjects/{subjectId}/suspensions` |
| O4 | `resumeOperationsSubject` | POST | `/api/v1/operations/subjects/{subjectId}/resumptions` |
| O5 | `listOperationsActivations` | GET | `/api/v1/operations/activations` |
| O6 | `listOperationsIvrSessions` | GET | `/api/v1/operations/ivr-sessions` |
| O7 | `getOperationsIvrSession` | GET | `/api/v1/operations/ivr-sessions/{sessionId}` |
| O8 | `listOperationsCatalogProducts` | GET | `/api/v1/operations/catalog/products` |
| O9 | `getOperationsCatalogProduct` | GET | `/api/v1/operations/catalog/products/{productId}` |
| O10 | `publishOperationsCatalogVersion` | POST | `/api/v1/operations/catalog/versions/{versionId}/publications` |
| O11 | `offsaleOperationsCatalogVersion` | POST | `/api/v1/operations/catalog/versions/{versionId}/offsales` |
| O12 | `listOperationsTrialPolicies` | GET | `/api/v1/operations/trial-policies` |
| O13 | `publishOperationsTrialPolicy` | POST | `/api/v1/operations/trial-policies/{policyVersionId}/publications` |
| O14 | `disableOperationsTrialPolicy` | POST | `/api/v1/operations/trial-policies/{policyVersionId}/disables` |
| O15 | `listOperationsOrders` | GET | `/api/v1/operations/orders` |
| O16 | `getOperationsOrder` | GET | `/api/v1/operations/orders/{orderId}` |
| O17 | `listOperationsAuditFacts` | GET | `/api/v1/operations/audit` |
| O18 | `createOperationsCatalogVersionDraft` | POST | `/api/v1/operations/catalog/versions` |
| O19 | `replaceOperationsCatalogVersionDraft` | PUT | `/api/v1/operations/catalog/versions/{versionId}/draft` |
| O20 | `createOperationsTrialPolicyDraft` | POST | `/api/v1/operations/trial-policies` |
| O21 | `replaceOperationsTrialPolicyDraft` | PUT | `/api/v1/operations/trial-policies/{policyVersionId}/draft` |

Self-built IVR surface — 6.

| # | operationId | Method | Exact path |
| --- | --- | --- | --- |
| I1 | `resolveIvrInbound` | POST | `/api/v1/ivr/inbound/resolve` |
| I2 | `confirmIvrActivation` | POST | `/api/v1/ivr/calls/{callId}/activation-confirmations` |
| I3 | `authorizeIvrDirectOutbound` | POST | `/api/v1/ivr/calls/{callId}/outbound-authorizations` |
| I4 | `ingestIvrCallEvent` | POST | `/api/v1/ivr/calls/{callId}/events` |
| I5 | `declareIvrRecording` | POST | `/api/v1/ivr/calls/{callId}/recordings` |
| I6 | `completeIvrRecording` | POST | `/api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete` |

Public recording shares surface — 3.

| # | operationId | Method | Exact path |
| --- | --- | --- | --- |
| P1 | `getPublicShare` | GET | `/api/v1/public/shares/{capabilityToken}` |
| P2 | `createPublicShareAdmission` | POST | `/api/v1/public/shares/{capabilityToken}/admissions` |
| P3 | `authorizePublicShareMedia` | POST | `/api/v1/public/shares/{capabilityToken}/admissions/{admissionId}/media-authorizations` |

Authenticated provider callbacks surface — 1.

| # | operationId | Method | Exact path |
| --- | --- | --- | --- |
| K1 | `receivePaymentProviderCallback` | POST | `/api/v1/callbacks/payment-events` |

Total: 18 + 21 + 6 + 3 + 1 = 49. Nothing else about the rows changed.

## 3. Validation methods and outcomes

All checks are document/static checks over the two allowed plan paths. No Redocly, `openapi-typescript`, `tsc`, npm, service, database or generated client was executed.

| Check | Method | Outcome |
| --- | --- | --- |
| Inventory row count | Regex over table rows: `^\| (C\|O\|I\|P\|K)[0-9]+ \| \`[A-Za-z0-9]+\` \`(GET\|POST\|PUT\|PATCH\|DELETE) /api/v1/` | 49 matching rows |
| Per-surface count | Same anchored row regex restricted to each ID letter and prefix | customer=18, operations=21, ivr=6, public-share=3, provider-callback=1 |
| Arithmetic | Per-surface sum and existing slice allocation | PASS: 18+21+6+3+1=49 and 14+18+17=49 |
| Exact method/path extraction | Extracted inventory `operationId`/`method`/`path` triples; compared to §2 of this artifact | PASS: 49 triples match row by row |
| Unversioned inventory paths | Anchored row regex with `/(?!api/v1/)` instead of the versioned prefix | 0 matches |
| Unversioned prose examples | Regex over all backticked `METHOD /path` tokens in the plan, not only table rows | 51 tokens (49 inventory + 2 prose examples); 0 unversioned |
| Bare surface prefixes | Regex `(?<!/api/v1)/(customer\|operations\|ivr\|public/shares\|callbacks)(?=[/\`\s])` over the plan | 0 matches |
| Single-surface ownership | Each of the 49 paths matched against the five approved prefixes | PASS: exactly one prefix per path; no multi-surface path |
| Duplicate operation IDs / paths | Compared the 49 extracted IDs and `method + path` values | PASS: C1-C18, O1-O21, I1-I6, P1-P3, K1 each appear once; no duplicate `method + path` |
| `servers: /` retained | Read §4.1 and the new §5.5.1 audit in the plan | PASS: relative root `/` retained; no base URL prefix convention introduced |
| Route examples | Searched prose route references; found and versioned `GET /api/v1/operations/evidence` in §4.9 and the §5.5.1 example | PASS: all prose examples versioned |
| UTF-8 | `file -bi` on both allowed plan paths | See execution record below |
| Whitespace A — required command | `git diff --check -- docs/gateflow/a1-production-openapi-surfaces-spec-plan.md docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-2.md` | Exit 0, no output. Both files are untracked, so this command cannot inspect their content; the content-level checks below compensate |
| Whitespace B — trailing scan | Regex `[ \t]+$` over both allowed plan paths | 0 matches |
| Conflict-marker scan | Regex `^(<<<<<<<\|=======\|>>>>>>>)` over both allowed plan paths | 0 matches |
| Worktree boundary | `git status --porcelain -- docs/gateflow/a1-production-openapi-surfaces-spec-plan.md docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-2.md`; compared against the task-start worktree | Only the plan (untracked) and this new artifact; controller document, plan-fix-1, sources, prototypes, screenshots, review artifacts and all other paths untouched by this task |

Execution record: `git diff --check` for the two allowed plan paths exits 0 with no output; the trailing-whitespace and conflict-marker scans return 0 matches on both files; `file -bi` reports `text/plain; charset=utf-8` for both; `git status --porcelain` shows only the plan (untracked), this new artifact and plan-fix-1 (pre-existing untracked) for the checked plan paths, plus the controller document as a pre-existing modification outside this task. A `git diff --no-index` comparison against `/dev/null` is blocked by the session's git working-directory restriction, so it was not used as evidence.

## 4. Boundaries and non-changes

- No runtime implementation, backend service, Flyway migration, database change, UI/prototype change, payment/identity integration or generated client was produced.
- No operation ID, operation count, per-surface count, slice allocation, HTTP semantics, schema, A0 mapping, policy input, design scope or non-goal changed.
- F-01..F-08 from plan-fix-1 are unchanged.
- No file other than `docs/gateflow/a1-production-openapi-surfaces-spec-plan.md` and this new fix artifact was edited by this task.
- No stage, commit, push, PR, merge, agent invocation or later-gate action occurred.

## 5. Residual risk (F-09 only)

- The plan and fix-1 artifact were untracked at task start, so `git diff --check` alone cannot inspect their contents. This artifact therefore also records content-level trailing-whitespace and conflict-marker scans; `git diff --no-index` against `/dev/null` is blocked by the session's git working-directory restriction. No whitespace or conflict-marker issue was found.
- The §5.5.1 audit is a plan-level document audit. The implementation gate must re-run the same versioned-path/count/single-surface assertions against the bundled OpenAPI `paths` via custom rule 8, because only the actual contract source can prove the emitted paths.
