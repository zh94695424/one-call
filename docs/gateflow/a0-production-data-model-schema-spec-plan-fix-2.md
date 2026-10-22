# A0 Production Data Model and Flyway Schema Specification Plan — fix artifact (round 2)

- Task label: `a0-plan-fix2-claude-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix` round 2
- Date: 2026-10-22
- Revised plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`
- Accepted inputs: `docs/reviews/plan-review-20261022-113039.md` (PR-001..PR-014), `docs/reviews/plan-review-20261022-124546.md` (F-01..F-07 and five partial states), `docs/gateflow/project-execution-control.md` fix-round-2 adjudication
- Historical artifacts preserved unmodified: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix.md`, both review artifacts
- Scope: plan correction and this fix artifact only; no final specification, executable migration, backend/API/UI/cloud/deployment/import/microservice work, re-review, commit, push, PR, merge or later gate was entered

## 1. Final finding status

### 1.1 F-01 through F-07

| Finding | Final status | Correction | Direct plan evidence | Validation evidence |
| --- | --- | --- | --- | --- |
| F-01 / PR-001: V001 creates the first-trial function before its V006 tables; S1 completion and INV-40 depend on S2 | 已修复 | V001 creates only `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append` and `audit.append_fact`. `entitlement_metering.issue_first_trial` is created in V006 after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance`. INV-40 is worded as S1 for the four platform/audit functions and S2 for the first-trial function. S1 completion names only the four platform/audit functions; S2 owns the first-trial tests. | §11 creation-wave paragraph lines 463-471; function-wave catalog lines 465-471; function contracts lines 475-479; INV-40 line 397; §12 V001/V006 rows lines 506/511; intra-wave V001 line 658 and V006 line 663; S1 completion line 700 and S2 completion line 739. | The function-wave catalog has exactly 5 rows: four `V001` functions and `issue_first_trial` `V006` with its four V006 table dependencies. The V001 intra-wave line contains the four platform/audit functions and says the first-trial function is not created there; the V006 line creates it. The S1 completion line names the four platform/audit functions and says the first-trial function is neither created nor required by S1. INV-40's verified column is split S1/S2. |
| F-02 / PR-006 / PR-014: `platform.idempotency_begin` has no completion/finalization path; replay cannot return a first result; the row is called append-only although completion updates it | 已修复 | Added `platform.idempotency_complete` with a single conditional update guarded by `status='NEW'` and `revision=p_expected_revision`, filling `result_type/result_id/result_snapshot/result_sha256`, `completed_at` and `revision+1` exactly once. `begin` does `INSERT ... ON CONFLICT DO NOTHING` then `SELECT ... FOR UPDATE`; `NEW` is the only outcome that may execute side effects; a concurrent same-key caller waits and returns `REPLAY` after the winner commits, or becomes `NEW` if the winner rolls back; same fingerprint on `COMPLETED` returns `REPLAY`; changed fingerprint returns stable `CONFLICT`; a committed `NEW` row raises `IDEMPOTENCY_INCOMPLETE` and alerts. `platform.idempotency_record` is reclassified mutable-head with `status`, `completed_at` and `revision`. | Table row line 199; lifecycle text line 289; matrix line 300; D34 line 186; INV-27 line 384; state-machine idempotency row and activation row lines 405-406; sequences 1 and 7 lines 424 and 430; contracts lines 475-476; tests lines 485-490. | `idempotency_record` matrix state is `MUTABLE_HEAD`; lifecycle counts are 28 mutable / 16 append-only; begin/complete contracts, concurrency, write-once completion, rollback and committed-`NEW` alert tests are present; the activation and order sequences call complete with the returned `record_id`/`revision` in the same transaction. |
| F-03 / PR-009: single-column evidence FK targets a non-unique column | 已修复 | Replaced it with the composite FK `evidence_record.(source_event_domain, source_event_id) → evidence_event.(event_domain, event_id)`, whose target is the declared composite unique key. Both sides of the cycle are `DEFERRABLE INITIALLY DEFERRED`: `evidence_record`'s composite FK and `evidence_event.evidence_id → evidence_record.id`. V008 creates both tables, then adds both constraints by exact `ALTER TABLE`, then creates a `DEFERRABLE INITIALLY DEFERRED` constraint trigger that validates exactly one matching `EVIDENCE_CREATED` event at commit. | Evidence table rows lines 237 and 239; FK mode table rows lines 536-537; FK matrix rows lines 615 and 618; key catalog line 651; V008 intra-wave line 665; sequence 9 line 429; INV-25 line 382; D28/D37 lines 180/189; tests line 492. | FK matrix has exactly two `DEFERRABLE_FK` rows, both the evidence cycle; the composite row has two child and two parent columns; the key catalog declares `evidence.evidence_event|event_domain,event_id|UNIQUE`; no single-column evidence FK token remains (0 matches); both cycle FKs are in V008 with `DEFERRABLE INITIALLY DEFERRED`; the commit-time trigger is specified. |
| F-04 / PR-014: audit function is in an undeclared schema | 已修复 | The only audit write path is `audit.append_fact`, owned by the `operations-audit` role, `SECURITY DEFINER`, `search_path = audit, pg_temp`, appending `audit.audit_fact`. The undeclared legacy audit-schema token was removed from the plan. | D11 line 163; audit table row line 201; INV-29 line 386; ownership contract line 456; contract row line 478; V001 row/intra-wave lines 506/658; tests lines 475-486. | Exact search for the legacy undeclared token returns 0; `audit.append_fact` appears in the contract, INV-29, V001, ownership and tests; the table remains `audit.audit_fact` with owner `operations-audit`. |
| F-05: order creation has no request fingerprint or first immutable order result | 已修复 | `catalog_order.customer_order.request_fingerprint` is required. Order creation uses platform scope `PURCHASE` with key `account_id || ':' || idempotency_key`: `begin` with the fingerprint, only `NEW` inserts the first order, then `complete` stores the order ID and frozen order snapshot in the same transaction. Identical retry returns the first order; changed input returns `CONFLICT`; a forced failure rolls back the order and idempotency row together. | Customer-order table row line 216; D35 line 187; INV-21 line 378; state-machine order row line 415; sequence 7 line 430; schema-contract bullet lines 440-442; tests line 490. | The owned row carries `request_fingerprint`; the unique `(account_id, idempotency_key)` remains; the `PURCHASE` binding, first-order result, replay/conflict and rollback tests are present; the event-domain registry includes `PURCHASE`. |
| F-06 / PR-014: first-trial function compares action identity/fingerprint it does not persist | 已修复 | `entitlement_metering.trial_grant` now persists `source_action_domain`, `source_action_event_id` and `request_fingerprint`, with unique `(source_action_domain, source_action_event_id)`. `issue_first_trial` locks the subject key and returns `GRANTED` on first grant; same action identity plus same fingerprint returns the original ids and `REPLAY`; same action identity plus changed fingerprint raises `TRIAL_ACTION_CONFLICT`; a different action on an already-granted subject returns `ALREADY_GRANTED` original ids and never creates a second trial, grant or balance. | Trial-grant table row line 227; D36 line 188; INV-16 line 373; state-machine trial row line 413; sequence 1 line 424; function contract line 479; tests line 491. | The trial row carries all three provenance columns and the unique action key; the function outcomes and no-second-grant rule are explicit; the activation sequence passes the action domain/event/fingerprint and returns `GRANTED`/`REPLAY`/`ALREADY_GRANTED`; the test list covers same-action replay, changed fingerprint and different-action already-granted. |
| F-07 / PR-012: validation uses a failing global count, does not compare rendered §12/§13 membership or FK target uniqueness, and conflates FK modes | 已修复 | The 44-table count is scoped to the catalog block, not a global regex. Added rendered §13 membership blocks and a machine-readable `key-catalog`; the structural script compares catalog names exactly with §12 wave and §13 slice membership (with missing/extra/duplicate output), validates every FK parent column set as PK/UNIQUE, validates composite cardinality, parent wave, required links and child/parent tables, and distinguishes `FK`, `ALTER_FK` and `DEFERRABLE_FK`. The same script is extracted verbatim and reused with `FILE=docs/14-production-data-model-and-flyway-schema-spec.md`. | Validation plan lines 780-1060; key catalog lines 629-654; slice membership lines 681-757; FK matrix lines 543-625; function-wave catalog lines 465-471; D33/D38 lines 185/189. | Scoped catalog count is 44; catalog, matrix, §12 wave membership and §13 slice membership counts are 44/44/44/44 with slice distribution 13/23/8; key catalog has 24 PK/UNIQUE sets; FK rows 81 with modes 74 `FK` + 3 `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK`; 0 legacy FK-mode tokens; 0 later-slice prerequisites. The embedded script was not executed end-to-end because this session's Bash permission layer blocks `awk`, `sed`, redirects and process substitution; the equivalent assertions below were executed with allowed tools and the Grep tool. The script is embedded unchanged for the next gate; no script pass is claimed here. |

F-status totals: 已修复 7; 部分修复 0; 未修复 0; 证据失效 0. No accepted finding is claimed unresolved at the artifact level; the F-07 execution limitation is a residual verification risk, not a claim that the script passed.

### 1.2 PR-001, PR-006, PR-009, PR-012 and PR-014

| Finding | Claimed final status after this round | Closure evidence |
| --- | --- | --- |
| PR-001 slice order and forward dependencies | 已修复 | F-01: the first-trial function is created and tested in V006/S2; V001/S1 creates/tests only the four platform/audit functions; INV-40 verification is split S1/S2; S1 completion no longer depends on V006. The three-slice graph remains S1 V001-V003 (13), S2 V004-V007 (23), S3 V008-V009 (8). |
| PR-006 activation replay/fingerprint/first result and operations event identity | 已修复 | F-02 and F-03: `IVR_CONFIRM` uses begin/complete with a request fingerprint, a write-once first result and stable `CONFLICT`; `source_action_event_id` remains NOT NULL and unique per domain, including `OPERATIONS`; activation sequence and tests are explicit. |
| PR-009 reciprocal evidence creation ownership | 已修复 | F-03: valid composite FK to the declared `(event_domain,event_id)` unique key; both cycle sides `DEFERRABLE INITIALLY DEFERRED`; exact V008 ALTER order; commit-time exactly-one constraint trigger; no single-column FK remains. |
| PR-012 deterministic structural validation | 已修复 | F-07: scoped catalog count, rendered wave/slice set equality, key catalog, FK target validation, FK-mode split and unchanged script reuse; the embedded script execution limitation is recorded in the F-07 validation evidence and residual risks. |
| PR-014 code-generation-ready cross-schema function contracts | 已修复 | F-02, F-04 and F-06: begin/complete with write-once finalization and concurrency semantics; `audit.append_fact` in schema `audit`; `issue_first_trial` created in V006 and persists/compares action identity/fingerprint with `GRANTED`/`REPLAY`/`ALREADY_GRANTED`. |

## 2. Before and after derived counts

| Dimension | Round-1 state | Round-2 state | Derivation / change |
| --- | --- | --- | --- |
| Tables | 44 | 44 | No table added or removed |
| Invariants | 40 | 40 | No invariant added or removed; INV-16/21/25/27/29/37/40 wording and verification updated |
| Migration waves | 9 | 9 | V001-V009; table distribution 3/6/4/7/6/7/3/5/3 |
| Slices | 3 | 3 | S1 13, S2 23, S3 8; no fourth slice |
| Lifecycle classes | 27 mutable + 17 append-only | 28 mutable-head + 16 append-only | `platform.idempotency_record` moved from append-only to mutable-head because `platform.idempotency_complete` updates `status`, result columns, `completed_at` and `revision` once |
| Sensitive-covered tables | 28 `Y` | 28 `Y` | No sensitive table added or removed |
| FK decisions | 81 | 81 | 2 `NO_FK` unchanged |
| FK modes | 75 ordinary + 4 deferred-labelled + 2 `NO_FK` | 74 `FK` + 3 `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK` | Three postponed/ALTER additions reclassified as `ALTER_FK`; the evidence record/creation-event cycle has two truly deferrable sides instead of one; `evidence_event.evidence_id` moved from ordinary to deferrable, so ordinary drops 75→74 and non-ordinary non-`NO_FK` rises 4→5 |
| Truly `DEFERRABLE INITIALLY DEFERRED` constraints | 1 | 2 | Both sides of the evidence creation cycle |
| Function contracts | 4 | 5 | Added `platform.idempotency_complete`; `issue_first_trial` remains but moves to V006 creation |
| Function creation waves | 4 in V001 (including the first-trial function) | 4 in V001 + 1 in V006 | V001: begin, complete, Outbox append, audit append; V006: `issue_first_trial` |
| Machine-readable candidate-key catalog | absent | 24 PK/UNIQUE sets | Every FK parent target used by the matrix |
| Rendered §13 slice membership | prose only | 3 explicit blocks, 13/23/8 | Compared exactly to the catalog by the structural script |
| Function-wave catalog | absent | 5 rows | Four V001 functions and one V006 function with table dependencies |

The 44-table, 40-invariant, 9-wave and 3-slice shape is preserved. The only count changes are direct consequences of the accepted corrections: idempotency lifecycle (28/16), FK-mode classification (74/3/2/2) and the added fifth function contract.

## 3. Exact commands run and outcomes

All counts below were executed against `docs/gateflow/a0-production-data-model-schema-spec-plan.md` (1221 lines at validation time) from the repository root.

| Check | Command | Outcome |
| --- | --- | --- |
| Plan non-empty | `wc -l docs/gateflow/a0-production-data-model-schema-spec-plan.md` | 1221; exit 0 |
| Fix-2 non-empty | `test -s docs/gateflow/a0-production-data-model-schema-spec-plan-fix-2.md` | exit 0 |
| Required numbered sections | `grep -c '^## [0-9]' <plan>` | 18; exit 0 |
| Slice headings | `grep -c 'Slice S[1-3]' <plan>` | 3; exit 0 |
| Wave rows | `grep -c '^| V00[1-9] |' <plan>` | 9; exit 0 |
| Catalog rows (scoped) | `head -242 <plan> | tail -n +197 | grep -c '^| [^A-Za-z0-9 -]'` | 44; exit 0 |
| Baseline matrix rows | `head -343 <plan> | tail -n +300 | grep -c '^[a-z]'` | 44; exit 0 |
| Matrix slice distribution | `... grep -c '|S1|'` / `|S2|` / `|S3|` | 13 / 23 / 8; exit 0 |
| Lifecycle distribution | `... grep -c '|MUTABLE_HEAD|'` / `|APPEND_ONLY|'` | 28 / 16; exit 0 |
| Sensitive `Y` tables | `... grep -c '|Y'` | 28; exit 0 |
| Invariant rows | `grep -c '^| INV-' <plan>` | 40; exit 0 |
| Invariants with DB/TX/SERVICE | `grep -c -E '^\| INV-.*(DB\|TX\|SERVICE)' <plan>` | 40; exit 0 |
| FK decision rows | `head -624 <plan> | tail -n +544 | grep -c '^[a-z]'` | 81; exit 0 |
| FK modes | `... grep -c '|FK|'` / `|ALTER_FK|` / `|DEFERRABLE_FK|'` / `|NO_FK|'` | 74 / 3 / 2 / 2; exit 0 |
| Key catalog rows | `head -653 <plan> | tail -n +630 | grep -c '^[a-z]'` | 24; exit 0 |
| Function-wave rows | `head -470 <plan> | tail -n +466 | grep -c '^[a-z]'` | 5; exit 0 |
| S1/S2/S3 membership rows | scoped membership blocks | 13 / 23 / 8; exit 0 |
| Legacy audit-schema token | `grep -c` for the legacy undeclared token | 0; exit 0 |
| Legacy FK-mode token | `grep -c 'DEFERRED[_]FK' <plan>` | 0; exit 0 |
| Legacy single-column evidence FK | `grep -c 'evidence.evidence_record.source_event_id|evidence.evidence_event.event_id' <plan>` | 0; exit 0 |
| Later-slice prerequisite | `grep -c -E 'S2 dep[ei]nds on S3|...' <plan>` | 0; exit 0 |
| Stale 43-table/35-invariant counts | `grep -c '43 tables'` / `grep -c '35 invariants'` | 0 / 0; exit 0 |
| Stale four-function claim | `grep -c 'four cross-schema functions' <plan>` | 0; exit 0 |
| V001 creation scope | `grep '^- V001:' <plan>` | Four platform/audit functions; first-trial function explicitly not created there |
| V006 creation scope | `grep '^- V006:' <plan>` | First-trial function created after its four tables |
| S1 completion scope | `grep 'Completion assertion: every table in V001-V003' <plan>` | Four platform/audit functions; first-trial function neither created nor required |
| Trailing whitespace | Grep tool pattern `[ \t]+$` | No matches |
| Embedded structural script | `awk '/^# structural-script-v2$/...' <plan> > /tmp/a0-structural-v2.sh; bash /tmp/a0-structural-v2.sh` | **Not executed**: the session permission layer blocked `awk`, `sed`, redirects and process substitution before the command ran. Exit status not available. No pass is claimed. The script is embedded unchanged in §14.1 for the next gate. |

The scoped counts above are the direct evidence for the 44/40/9/3, 13/23/8, 28/16, 28-sensitive, 81-decision, 74/3/2/2 and 24-key results. The exact catalog-to-wave-to-slice set comparison and FK-key validation are implemented in the embedded script; because the sandbox could not run it, those exact-equality assertions are not claimed as executed here. The plan's §14.2 requires the next gate to run the script unchanged with `FILE=docs/14-production-data-model-and-flyway-schema-spec.md`.

## 4. Residual risks and blockers

- No accepted finding is unresolved in the plan artifact. The only verification limitation at this gate is that the embedded structural script could not be executed end-to-end because the session permission layer blocks `awk`/`sed`/redirects/process substitution. Owner: plan author/controller; destination: independent re-review and the final-spec implementation gate, which must run the unchanged script.
- No executable Flyway migration, PostgreSQL constraint test or Testcontainers concurrency suite exists at this gate. Owner: Backend; destination: implementation gate.
- The `SECURITY DEFINER` function contracts still depend on environment-specific role provisioning, ownership transfer and security review. Owner: Platform/Database/Security; destination: security design and production readiness (Q17/Q23).
- Installment due-date timing, grace/retry policy and scheduler cadence for future `entitlement_issuance` rows remain unapproved. Owner: Product/Finance/Backend; destination: entitlement issuance design (Q20).
- Reconciliation scan cadence, alert thresholds and manual adjustment approval remain unapproved. Owner: Operations/Backend/IVR; destination: operations runbook (Q21).
- Redis playback-ticket TTL, clock skew and revocation propagation remain unapproved. Owner: Security/Sharing; destination: sharing policy (Q22).
- Idempotency begin/complete concurrency, committed-`NEW` alerting and write-once result tests, order `PURCHASE` replay tests and the evidence deferred-cycle migration test do not yet exist. Owner: Platform/Catalog-order/Evidence/Backend; destination: implementation concurrency and integration suites.
- The source-level launch-blocking risks (caller-only matching, number recycling, real-line/payment/COS validation) remain unchanged and are recorded in plan §16.
- Pre-existing modified screenshots and `logo/preview.html` remain outside this work unit and were not touched.
- No Git remote/PR acceptance is claimed; this is a local fix artifact only.

## 5. Scope statement

This gate modified only `docs/gateflow/a0-production-data-model-schema-spec-plan.md` and created only `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-2.md`. It did not modify the first fix artifact, the controller, either review artifact, `docs/09` through `docs/13`, source code, prototypes, QA screenshots, `.codex`, Git metadata, credentials or external systems. It did not write `docs/14`, SQL, migrations or production code. Temporary validation files created during this gate were removed. It did not stage, commit, push, create a PR, merge, re-review or enter any later gate.
