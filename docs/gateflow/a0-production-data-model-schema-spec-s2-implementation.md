# A0 S2 implementation artifact

- Task label: `a0-spec-s2-implementation-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `implementation`, Slice S2 only (V004-V007); S1 accepted, S2 author-complete / independent review pending, S3 planned
- Provider: `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Status: authoring complete within the two allowed artifacts. No independent review, later slice, Git mutation or production test is claimed.

## Binding inputs and checkpoint

| Input | Value |
| --- | --- |
| Controller | `docs/gateflow/project-execution-control.md`; current gate S2 implementation; accepted S1 checkpoint `5404d9caa73b72c412ca2065bef19dfb46d154d3` |
| Accepted plan | `docs/gateflow/a0-production-data-model-schema-spec-plan.md`; SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; unchanged |
| Plan fix-6 | `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`; SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`; unchanged |
| Accepted plan review | `docs/reviews/plan-review-20261022-174730.md`; SHA256 `C2432DA173BB4CB61D5554B1A2BF77C7DC5FA4AE79CE5CED1118741BBA34FDCA` |
| Accepted S1 specification | SHA256 `B7FE164B701F8813A75212C4D8EC4624A97867FB7289EA652F3FE2283E510099`; commit `5404d9c` |
| Accepted S1 semantic review | `docs/reviews/code-review-20261022-191047.md`; SHA256 `5F3068271C48DA54DABB26D8942CA5C26E06658A98F4945DF91C17F347C7103A` |
| Accepted S1 EOF-only review | `docs/reviews/code-review-20261022-191848.md`; SHA256 `B30410B5ECBC181FD1581C3DCC375110203E551E17A6BC408C2C1D6D47F29E6F` |
| Frozen baseline | 44 tables; 40 invariants; 9 waves; 3 slices 13/23/8; lifecycle 28/16; 81 FK decisions = 74/3/2/2; 24 key sets; 5 function-wave rows = 4 V001 + 1 V006 |

The accepted plan's 44-table catalog, baseline matrix, sensitive inventory, FK catalog, key catalog, function-wave block, nine wave rows, three slice membership blocks, 40 invariant rows and 28 open-input rows remain byte-identical to the accepted plan.

## Changed paths

1. `docs/14-production-data-model-and-flyway-schema-spec.md` (S2 extension of the accepted S1 specification; S1 sections preserved).
2. `docs/gateflow/a0-production-data-model-schema-spec-s2-implementation.md` (this artifact).

No other path was written by this task. The pre-existing modified QA screenshots, `logo/preview.html`, the pre-existing modified controller and the unrelated `docs/reviews/plan-review-20261021-205118.md` remain outside this work unit and were not changed by this task. No Git stage, commit, push, PR, merge or later gate was performed. No additional Agent or Gateflow run was used.

## S1 acceptance and consumed restrictions

S1 is recorded as accepted, not re-authored: commit `5404d9c`, specification SHA256 `B7FE164B701F8813A75212C4D8EC4624A97867FB7289EA652F3FE2283E510099`, semantic review `docs/reviews/code-review-20261022-191047.md` and EOF-only review `docs/reviews/code-review-20261022-191848.md`. The accepted S1 dictionaries, identity/claim/platform semantics, exact S1 FK/grant/lock contracts and frozen blocks are preserved.

The three authorized behavior-neutral S1 clarifications are consumed in the specification:

1. `§9.6` step 2 now states that the organization lifecycle and membership-pair locks are acquired only when `organization_id` is supplied; a personal claim takes neither.
2. `§9.6` step 2 now explicitly returns `CLAIM_BINDING_NOT_FOUND` on an initial zero-row binding read, with no claim row, owner change or further lock/write.
3. `§9.7` steps 3 and 4 now return stable `BINDING_SUBJECT_NOT_FOUND` with no binding, owner or access change for an unknown subject.

These are behavior-neutral clarifications in the next bounded edit, not schema, policy or S1 reopening. Final public error representation remains later API work. S1's request-ID whole-transaction rollback/bounded retry and S2's same-connection phase-B savepoint are kept distinct.

The S1 review handoffs are carried forward: the stable membership-pair key `organization_access.membership:<organization_id>:<account_id>` with canonical lowercase UUID text; canonical-number acquisition after canonical-caller and before subject/session/call/activation; owner-only row locks and cross-owner reads under the same advisory lock; the exact global acquisition order; both revocation/confirmation serial-order outcomes; owner-role same-connection collaborators; and the enterprise fail-closed boundary.

## S2 scope completed

### Tables

The 23 exact V004-V007 tables are fully expanded with every accepted column, PostgreSQL type, nullability, default, named PK/UNIQUE/CHECK/index/foreign key, FK action, revision or append-only control, lifecycle/deletion rule and sensitive representation:

1. `catalog_order.catalog_product`
2. `catalog_order.catalog_version`
3. `catalog_order.catalog_offer`
4. `catalog_order.catalog_benefit_line`
5. `catalog_order.customer_order`
6. `catalog_order.payment_order`
7. `catalog_order.payment_event`
8. `ivr.outbound_call_task`
9. `ivr.inbound_session`
10. `ivr.call_record`
11. `ivr.call_leg`
12. `ivr.call_event`
13. `ivr.ivr_decision`
14. `entitlement_metering.trial_policy_version`
15. `entitlement_metering.entitlement_grant`
16. `entitlement_metering.trial_grant`
17. `entitlement_metering.entitlement_balance`
18. `entitlement_metering.entitlement_issuance`
19. `entitlement_metering.usage_reservation`
20. `entitlement_metering.usage_fact`
21. `activation_consent.consent_record`
22. `activation_consent.service_activation`
23. `activation_consent.service_activation_transition`

The S2 detailed total is 307 columns and the cumulative detailed total is 428 (121 S1 + 307 S2). Planned catalogs remain 44; detailed dictionaries are now 36 (13 S1 + 23 S2); S3 retains eight planned dictionaries and is not claimed as authored.

### Invariants

S2 authored enforcement and behavior assertions for INV-08 through INV-14, INV-16 through INV-23, INV-32, INV-34 through INV-38 and the S2 `issue_first_trial` portion of INV-40: 22 S2 expansion rows, each with one owner, a DB/TX/SERVICE class and an exact location. S1's 14 expansion rows are preserved.

### Functions and services

One new V006/S2 SQL contract is authored: `entitlement_metering.issue_first_trial`, `SECURITY DEFINER`, fixed `search_path = entitlement_metering, platform, pg_temp`, no transaction control, `PUBLIC` denied, EXECUTE only to `activation_consent_role` and `entitlement_metering_role`, created after its four dependency tables. Its `GRANTED`/`REPLAY`/`ALREADY_GRANTED`/conflict/integrity semantics and same-transaction rollback are explicit.

The four V001 functions remain S1-owned and unchanged. The following are application services in the same local transaction, not SQL functions and not new function-wave rows:

- `ivr.select_call_action` (the one shared ordered-predicate/decision service);
- `ivr.record_activation_next_action` (thin IVR wrapper);
- `activation-consent.record_activation_confirmation`;
- `entitlement-metering.reserve_call_usage`.

### Behavior completed

- Immutable catalog product/version/offer/benefit and order snapshots; mutable sale/payment lifecycle; no order-level entitlement status; `PURCHASE` begin/fingerprint/first-order-result completion with changed-input conflict.
- Verified payment status separate from durable per-line/installment/type `entitlement_issuance` state, attempt count, last error, grant linkage and retry.
- Permanent `FIRST-TRIAL` per subject with persisted source action domain/event/fingerprint, explicit `GRANTED`/`REPLAY`/`ALREADY_GRANTED` and changed-input conflict, and one grant/balance/trial/Outbox in the activation transaction.
- Effective entitlement routing snapshot versus spend authorization; frozen `authorized_units`, `authorization_expires_at` as the B-leg start cutoff, partial use, exactly-once settlement, missing/late/duplicate reconciliation, `UNKNOWN` occupation, terminal convergence, no release of unknown use and no redial on response loss.
- One `PENDING`/`CLAIMED` task and one `ACTIVE`/`UNKNOWN` call per canonical caller across accounts/workspaces; current `callId` excluded; competing fresh refusal with a valid terminal `REJECTED` parent and no claim/reservation; 30-second configurable default TTL frozen at creation, lock-then-`clock_timestamp()` expiry and no duplicate refresh; no latest-callee/queue fallback.
- One IVR-owned `ivr.select_call_action` predicate implementation with separate `INBOUND_RESOLVE`, `ACTIVATION_NEXT_ACTION` and `DIRECT_AUTHORIZATION` purposes; wrappers authenticate/replay/confirmation-gate only; no cross-owner `ivr_decision` write.
- Authenticated replay and immutable identity checks before expiry cleanup or any accepted effect; event-level and call/purpose-level precedence; same-fingerprint first result (including after mutable state changes), changed-fingerprint conflict with `NEW` rollback, event reuse on another call conflict.
- Exactly seven actions; `DIRECT_AUTHORIZATION` as a purpose; successful direct authorization with `action` null, null task fields, committed reservation/seconds/cutoff and encrypted `authorization.calleeNumber`; task dial uses encrypted `task.calleeNumber`; replay never renews authorization.
- Three explicit outcomes: category-1 pre-activation refusal, category-2 valid activation/trial retained with a stable no-dial decision and no claim/reservation, category-3 full technical rollback. The same-connection savepoint is after activation/trial and before the tentative phase-B claim/reservation; there is no independent commit, subtransaction or swallowed SQL exception.
- Current prompt/consent policy-version/key/content validated before accepted append; `CONSENT_REQUIRED` on an `ACTIVE` activation with missing/expired consent; stale/invalid confirmation content rejected before accepted effects; fresh suspended activation never reactivated by IVR; operations-only resume with reason/event and no trial reissue.
- Exact serial-order outcomes: revocation before access locks yields the access refusal; confirmation/claim first may keep a frozen committed dial; no post-commit access reread or reversal.
- Round-6 assertion is explicit as T-S2-26: a fresh suspended/in-progress category-1 refusal may reject a never-confirmed current session while preserving an already-`CONFIRMED` session, a current `UNKNOWN` call and unrelated calls, using the full category-1 write set.
- Self-developed IVR media/B-leg/bridge/recording/cutoff ownership; matched task taken once and auto-dialed without callee confirmation; no IVR payment loop, only mini-program purchase guidance; direct trial without registration/login/membership/real-name prerequisites.
- `call_record.call_id` is the canonical call parent; the two `NO_FK` roots and the three same-wave `ALTER_FK` timings are explicit.
- S2 lifecycle split is 14 mutable-head + 9 append-only; combined with S1 and the planned S3 split it preserves 28/16.

### Frozen baseline preserved

The machine-readable catalog, matrix, sensitive inventory, FK catalog, key catalog, function-wave block, nine wave rows, slice memberships, 40 invariant rows and 28 open-input rows are unchanged. S2 does not create S3 tables/functions/transactions, a fourth slice, an eighth action, an extra SQL function or a new business workflow.

## Validation evidence

All commands were run from `/mnt/d/codex-call` on 2026-10-22. Results are document/shell checks only; no PostgreSQL instance, Flyway run, IVR line, payment provider or Testcontainers suite was executed.

### Inputs and unchanged structural script

```text
plan SHA256       149472d291dc19f81497a0599c85122bdda311dc92417c3e2086667130b231a5  (unchanged)
plan fix-6 SHA256 8976f64a215a2b7b5cb116a14d5123aff91f754f512b6fd212e0d8c0c7045f59  (unchanged)
extracted structural script SHA256 ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md
exit 0
fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81
catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24
```

The script was extracted verbatim from the accepted plan and executed unchanged; no regex or count was weakened.

### Specification hash, size and EOF

```text
spec SHA256 c09a8b227c715d854a8ea80552809d2aab6f4c8fa8a561b5853ceca50e2821c8
size 504210 bytes; 3686 LF-terminated lines; CR count 0; trailing LF count 1; no blank line at EOF
```

### Document checks

```text
16 required top-level sections
dictionary headings 36 (13 S1 + 23 S2)
S1 function contract headings 4
S1 invariant expansion rows 14
S2 invariant expansion rows 22
function-wave rows 5 (4 V001 + 1 V006)
V006 function contract heading 1
S2 activation sequence heading 1
zero trailing-whitespace lines
zero stale baseline-count, stale navigation, later-slice-prerequisite or stale lock-key tokens
all required S1/S2 contract terms present, including BINDING_SUBJECT_NOT_FOUND
S1 lifecycle/sensitive/revision coverage 13/13/13; S1 FK detail 11; S1 sensitive detail 23 rows
S2 lifecycle/sensitive/revision coverage 23/23/23; S2 FK detail 54; S2 sensitive detail 25 rows
```

### Per-table dictionary coverage

S1 `awk` coverage exited 0 for all 13 tables and 121 columns. S2 `awk` coverage exited 0 for all 23 tables and 307 columns.

Exact accepted-column comparison (catalog field 6 versus dictionary first column) produced 36 exact lines and `ALL_EXACT`:

```text
platform.idempotency_record 12; platform.outbox_event 14; audit.audit_fact 12;
customer_iam.customer_account 6; customer_iam.auth_identity 10; customer_iam.identity_profile 9;
organization_access.organization 6; organization_access.organization_membership 7;
operations_iam.employee_account 9; subject_number.business_subject 9;
subject_number.caller_number_binding 9; subject_number.subject_claim 10; subject_number.subject_risk_state 8;
catalog_order.catalog_product 4; catalog_order.catalog_version 17; catalog_order.catalog_offer 7;
catalog_order.catalog_benefit_line 6; catalog_order.customer_order 21; catalog_order.payment_order 11;
catalog_order.payment_event 12; ivr.outbound_call_task 23; ivr.inbound_session 17;
ivr.call_record 22; ivr.call_leg 11; ivr.call_event 14; ivr.ivr_decision 15;
entitlement_metering.trial_policy_version 11; entitlement_metering.entitlement_grant 13;
entitlement_metering.trial_grant 10; entitlement_metering.entitlement_balance 8;
entitlement_metering.entitlement_issuance 14; entitlement_metering.usage_reservation 16;
entitlement_metering.usage_fact 12; activation_consent.consent_record 19;
activation_consent.service_activation 6; activation_consent.service_activation_transition 18
```

Per-dictionary completeness also exited 0: every S2 dictionary has a Purpose/Lifecycle line, the explicit type/null/default/key column table, named checks, keys/indexes, revision/immutability or append-only control, sensitive representation and an explicit FK or "No foreign key" statement.

### Frozen-block equality

The following extracts from the current specification are byte-identical to the accepted plan: `catalog`, `baseline-matrix`, `sensitive`, `fk-matrix`, `key-catalog`, `function-wave`, `slice-S1-tables`, `slice-S2-tables`, `slice-S3-tables`, the nine wave rows, the 40 frozen invariant rows and the 28 open-input rows.

### Scope and cleanliness

```text
staged paths: none
.sql or db/migration paths: none
specification: modified (the authorized S2 extension)
S2 artifact: new
S1 implementation/fix-1/fix-2/EOF artifacts: hashes unchanged
plan, plan fix-6, accepted reviews: hashes unchanged
pre-existing controller diff, QA screenshots, logo/preview.html and old plan-review: outside this task, untouched
```

Whole-new-file `git diff --no-index --check` comparisons for both S2 artifacts exit 1 with zero diagnostic output; the exit status is the expected content difference, not a whitespace failure.

## Docs decision

- Modify only `docs/14-production-data-model-and-flyway-schema-spec.md` to add the S2 V004-V007 dictionaries, S2 invariant/state/transaction/service/validation sections and the three authorized S1 clarifications, while preserving S1 and the frozen blocks.
- Add this S2 implementation artifact.
- Do not modify the accepted plan, fix artifacts, historical reviews, controller, `docs/09` through `docs/13`, README, prototypes, screenshots or Git metadata.
- No documentation index update is made; controller authority remains unchanged.
- No retention period, capacity value, credential, provider configuration or legal policy is recorded as approved.

## Residual risks and classification

### Fixed-current

| Risk / item | Status | Owner / destination |
| --- | --- | --- |
| 23 S2 dictionaries, 307 columns, exact accepted-column sets | fixed in this artifact; document-verified | A0 S2 author; independent review |
| S2 dictionaries include types, nullability, defaults, named keys/checks/indexes/FKs, lifecycle, sensitive and revision controls | fixed in this artifact; document-verified | A0 S2 author; independent review |
| 22 S2 invariant portions with owners and enforcement locations | fixed in this artifact; document-verified | A0 S2 author; independent review |
| V006 `issue_first_trial` contract and S2 application-service boundaries | fixed in this artifact; document-verified | A0 S2 author; independent review |
| S2 transaction/state/test matrix, including T-S2-26 round-6 write set | fixed in this artifact; document-verified | A0 S2 author; independent review |
| Three S1 behavior-neutral clarifications | fixed in this artifact; no schema/policy change | A0 S2 author; independent review |
| Frozen 44/40/9/3, 28/16, 81 FK modes, 24 key sets and 5 function-wave rows | preserved; structural script exit 0 | controller / independent review |
| S2 has no S3 prerequisite | validated by the unchanged structural script and S2 test T-S2-43 | controller / independent review |

### Later-approved-slice

| Risk / item | Owner / destination |
| --- | --- |
| S3 evidence/media/sharing dictionaries, evidence composite deferred FK cycle, evidence/share transactions and final 44-table closure | S3 author; V008-V009/S3 gate |
| S2 independent semantic review, including exact column-set, lock/outcome and typed-snapshot scrutiny | Independent reviewer; next S2 gate |
| Executable PostgreSQL privileges, lock waits, savepoints, failure injection, IVR and payment integration | Backend/Database/IVR/Payments; later implementation work unit |

### Later-work-unit

| Risk / item | Owner / destination |
| --- | --- |
| S2 production PostgreSQL/Flyway V004-V007, Testcontainers, privilege/lock/concurrency tests | Backend/Database; implementation and migration work unit |
| Q18 pending-task TTL configuration key/versioning and scheduler cadence | Product/Backend; IVR protocol and operations configuration |
| Q24 call-blocking risk policy beyond claim-only `OPEN` | Product/Risk/IVR; claim-risk and launch-readiness review |
| Q27 exact organization role-to-task-permission matrix | Organization-access/Security; authorization design and S2 policy tests |
| Q28 IVR-local B-leg key/persistence/lost-response recovery | Telephony/IVR; IVR protocol and line integration |
| Q4/Q5/Q6 production catalog/payment values and provider contract; Q7/Q8 line/media/duration; Q10/Q15/Q26 prompt/consent and wire mappings; Q20/Q21 instalment/reconciliation; Q25 authorization window/clock skew | Named Product/Finance/Operations/Payments/Telephony/IVR/Security owners; later Phase C/D and protocol/security work units |
| KMS/envelope/HMAC rotation, legal retention/erasure, authentication/session design and deployment role provisioning (Q1/Q2/Q3/Q17/Q19/Q23) | Security/Infrastructure/Legal/Platform; named later work units |
| Capacity, SLO, RPO/RTO, cloud provisioning and provider integration | Capacity/Security/Operations/Infrastructure; later work units |

### Preserved open production inputs

The frozen 28-row open-input block is unchanged. S2 does not guess Q18 (TTL configuration key/approval and cadence), Q24 (any call-blocking risk rule), Q27 (organization task-access role matrix) or Q28 (IVR-local B-leg key/recovery), and it does not guess Q4/Q5/Q6/Q7/Q8/Q10/Q15/Q20/Q21/Q25/Q26. The source default of 30 seconds is frozen per task; `authorizationExpiresAt` stores the committed reservation value and invents no window; no retention period, capacity value or vendor credential is introduced.

### Uncovered areas

S2 does not implement or validate executable Flyway SQL, PostgreSQL constraints/privileges/locks/savepoints, Testcontainers, IVR line/media behavior, payment provider verification, KMS/envelope rotation, legal retention, S3 evidence/sharing, any API/UI/cloud resource or production S3 closure. The S2 test matrix is a document requirement, not executed test code.

## Completion statement

S2 authoring is complete within the allowed two-artifact scope. S1 is recorded as accepted and preserved. The 23 S2 dictionaries, 307 columns, 22 invariant portions, one V006 function contract, four application-service boundaries, S2 state/transaction/outcome/snapshot contracts and the explicit round-6 assertion are specified. Frozen membership and baseline remain unchanged, and the unchanged structural script exits 0 with the expected 44/40/9/3, 28/16, 81 FK and 24 key-set output. No essential accepted-contract conflict was found; no blocker is declared.

S2 remains **author-complete / independent review pending**, S3 remains **planned / not authored**, and no full A0 completion is claimed. No independent review, later slice, Git stage/commit/push/PR/merge or production test was performed. This report stops here.
