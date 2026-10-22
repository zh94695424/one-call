# A0 S1 fix 2 artifact

- Task label: `a0-spec-s1-fix2-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix`, Slice S1 (V001-V003) round 2
- Runtime/provider: `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Review base: `4a98fbb`
- Status: S1 author-complete for the adjudicated round-2 corrections; independent re-review pending. S2/S3 remain visibly planned and are not authored. Specification only; no executable SQL, migration, database, service/API/UI, cloud, credential or production work.

## Binding inputs

| Input | Value |
| --- | --- |
| Controller and final S1 fix-2 adjudication | `docs/gateflow/project-execution-control.md` (read-only for this task; its pre-existing worktree modification is outside this task's writes) |
| Accepted plan (frozen) | `docs/gateflow/a0-production-data-model-schema-spec-plan.md`; SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; unchanged |
| Preserved plan fix-6 gate | `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`; SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`; unchanged |
| Accepted plan review | `docs/reviews/plan-review-20261022-174730.md`; SHA256 `C2432DA173BB4CB61D5554B1A2BF77C7DC5FA4AE79CE5CED1118741BBA34FDCA`; unchanged |
| Prior S1 review (fix 1) | `docs/reviews/code-review-20261022-181806.md`; SHA256 `E813BFDA4BA09FC1E99416FFB878CD0739D2EEAA1C5C6EDC7C36F1653D7E4726`; unchanged |
| S1 re-review round 1 | `docs/reviews/code-review-20261022-184720.md`; SHA256 `0774D8B9B38C22124ADAC9FE67B3D0CC80160ECD53FDB59EF4DF5C3F6874FC6F`; unchanged |
| Historical S1 implementation artifact | `docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md`; SHA256 `1767FFD21C09173F1EFBF468B627A6166DB40F393D8D4D5DE123C5E901A9C12D`; unchanged |
| Historical fix-1 artifact | `docs/gateflow/a0-production-data-model-schema-spec-s1-fix-1.md`; SHA256 `0B39AD526630F2BD0103B3ADCC023BA3E465F99EA1F3D364016F6FD966CC5B43`; unchanged |
| Structural script | `/tmp/a0s1/structural-v2-run.sh`; SHA256 `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545`; unchanged |
| Specification before fix 2 | `docs/14-production-data-model-and-flyway-schema-spec.md`; SHA256 `1AF4D59258C210A49C314CE5C605D2D7050178ACBE3440CA6C1A4B54BDBF5F06` |
| Specification after fix 2 | `docs/14-production-data-model-and-flyway-schema-spec.md`; SHA256 `31DE2BAE94815E844A8C8512AC3A4700AB8C38C22C598D3789587528D0FC3920` |

Frozen baseline preserved: 44 tables; 40 invariants; 9 waves; 3 slices 13/23/8; lifecycle 28 mutable / 16 append-only; 28 sensitive tables; 81 FK decisions = 74 `FK` + 3 `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK`; 24 key sets; 5 function-wave rows = 4 V001 functions + 1 planned V006 function. S1 remains 13 tables / 121 accepted columns / 14 invariant expansions / 4 V001 function contracts. No table, column, key, function, slice, domain, scope or business policy was added.

## Changed paths

1. `docs/14-production-data-model-and-flyway-schema-spec.md` (the S1 specification; updated in place).
2. `docs/gateflow/a0-production-data-model-schema-spec-s1-fix-2.md` (this artifact; new).

No other path was written. The controller, accepted plan, prior reviews, historical S1 implementation artifact, historical fix-1 artifact, business sources, prototypes, screenshots, configuration, Git metadata and the unrelated old review remain read-only and unchanged. No Git stage, commit, push, PR or merge was performed. No additional Agent was used.

## F-10 membership lifecycle key — fixed-current

The membership advisory namespace is now the stable logical pair, not a membership row ID:

- Section 9.0: namespace is `organization_access.membership:<organization_id>:<account_id>` with both components as canonical lowercase UUID text (36 characters, no braces or uppercase); the owner derives the immutable pair from its own row before locking and re-reads after the lock; a create already knows the pair from the request; `subject_number_role` reads the pair from its request inputs and needs no membership `id`.
- Section 9.0: under the pair lock a cross-module reader queries the single non-revoked membership row for the pair and requires `ACTIVE`; absent, `SUSPENDED` or `REVOKED` fails closed. A revoked row and its replacement share the key, so create/suspend/role/revoke/re-grant serialize with all pair readers. No ID-discovery loop and no new SQL function exist.
- Section 9.6 step 2: the claim acquires the pair lock from `(organization_id, account_id)` in the global order.
- Section 9.10: the membership owner writer uses the same pair key for role/suspend/revoke/re-grant, with an explicit create branch and a stable not-found/inactive result for a role/suspend/revoke action on an absent row.
- Sections 4.10, 2.1, 10.8, 12.4 and 15.2 state the same key and the no-widening/no-loop property.
- Section 9.12 adds privilege and serial-order assertions: absent/create, role/revoke/re-grant, reader ACTIVE requirement, and no membership `id` need.
- Section 10.8 confirms the existing exact `organization_id`, `account_id`, `role`, `status` SELECT columns suffice; the former need for an `id` grant is removed. This also resolves the F-04 partial state from the round-1 re-review.

## F-11 unknown referenced identities — fixed-current

- Section 9.6 step 2: a zero-row subject lock returns `CLAIM_SUBJECT_NOT_FOUND`; zero-row organization or account reads return `CLAIM_ORGANIZATION_NOT_FOUND` or `CLAIM_ACCOUNT_NOT_FOUND`; each writes no claim row.
- Section 9.6 step 3: existence is validated before any outcome row. A fresh-request `REJECTED` row is appended only when the target subject, authenticated account and supplied organization exist and the binding and verification reference resolve. Existing inactive parents may produce a `REJECTED` row only in that complete-reference case. A missing binding or invalid reference writes no row. Raw foreign-key failure is never a protocol outcome.
- The stable claim-code table adds the three `*_NOT_FOUND` codes with `none`, and clarifies that `CLAIM_ACCOUNT_INACTIVE`, `CLAIM_ORGANIZATION_INACTIVE` and `CLAIM_MEMBERSHIP_INACTIVE` apply to existing parents/membership absence.
- Risk still preempts replay/duplicate success; replay identity comparison still governs mismatches and never discloses a stored success.
- Section 9.12 adds unknown-subject/account/organization assertions; INV-07 names the enforcement.

## F-12 create versus same-subject DISABLED binding — fixed-current

- Section 9.7 step 2: a same-subject `DISABLED` row is never revived by create; create returns stable `BINDING_DISABLED_CONFLICT` with no mutation and requires the explicit re-enable action. `RETIRED` remains terminal. An identical same-subject `ACTIVE` binding alone is an idempotent create success.
- Section 9.7 step 3: a create unique loser aborts the whole transaction, retries from the beginning with the canonical-number and subject locks reacquired, and maps deterministically to same-subject identical `ACTIVE` idempotent success, same-subject `DISABLED` conflict, same-subject `RETIRED` conflict, other-subject `ACTIVE` conflict, or a stable current-state conflict. No raw unique error and no non-active success.
- Section 9.7 step 4: replacement rollback/loser mapping uses the same whole-transaction retry wording and retains the old active binding.
- Section 9.7 step 6 and step 8: both create/re-enable serial orders are explicit (re-enable first → later create idempotent; create first on `DISABLED` → `BINDING_DISABLED_CONFLICT`, then explicit re-enable succeeds unless another active binding exists).
- Section 9.12 and the section 13.1 term check include the stable code and no-raw-unique behavior.

## Open-question adjudications

| OQ | Status | Exact correction and location |
| --- | --- | --- |
| OQ-1 duplicate verification method | fixed-current | Section 9.6 step 6 compares `verification_method` for a same-account duplicate; a changed method is `CLAIM_CONTEXT_CONFLICT` with no reuse/disclosure. Section 4.14.1 states the same and keeps the encrypted logical comparison (not AEAD byte equality). Section 9.12 asserts it. |
| OQ-2 request-ID unique loser | fixed-current | Section 9.6 step 8 requires a whole-transaction rollback, a bounded retry from the beginning in a new transaction, reacquisition of every lock and recheck of risk/lifecycle/identity, no read in the aborted transaction, no surviving tentative owner write, no S1 savepoint/subtransaction, and `CLAIM_CONCURRENCY_EXHAUSTED` on exhaustion. The stable-code table, 9.12 and INV-07 record it. |
| OQ-3 workspace listing locking | fixed-current | Section 9.4 states T-S1-04 is an unlocked read-only advisory projection, not an authorization lease; every protected request rechecks current authority and side-effecting transactions hold the section 9.0 locks. Sections 9.0, 10.8, INV-01 and 9.12 were narrowed to authorization/mutation transactions. No authentication/session subsystem is specified; Q1/Q19 remain open. |
| OQ-4 S2 canonical-number placement | deferred-with-owner | Section 9.0 and the S2 handoff require S2 to acquire any canonical-number key after canonical-caller and before subject/session/call/activation; section 9.11 records the handoff. No S2 transaction, table or test is authored. Owner: S2 author; destination: V004-V007/S2 gate. |
| OQ-5 subject creation ownership | fixed-current | Every S1 creation source, including `OPERATIONS`, starts with both owner columns null (4.12, 9.6 step 7, 9.12, INV-05). Section 10.8 removes `subject_number_role` INSERT privilege on both owner columns and its UPDATE privilege on `owner_organization_id`; V003 step 8 repeats the restriction. Columns/FKs remain, verified personal claim still sets `owner_account_id` from null (same transaction or later), and legacy explicit enterprise rows remain readable. No optional workspace/source ownership and no fresh enterprise policy/transfer. |
| OQ-6 claim timestamp semantics | fixed-current | Section 4.14 defines `verified_at` as the accepted-claim decision timestamp at/after `risk_checked_at`, never the proof-attempt time; the original attempt timing remains inside the immutable `proofRef` reference (4.14.1). Section 9.6 step 8, 9.12 and INV-07 align. No new column. |
| OQ-7 accepted-claim integrity | fixed-current | Section 9.6 step 7 runs under the subject lock before any owner write and returns `CLAIM_DATA_INTEGRITY` for any accepted claim inconsistent with the effective owner (including any accepted claim on a null-owner subject), with no repair, new accepted claim or owner write. Section 9.12 and INV-07 require a seeded-corruption assertion. No repair workflow is added. |
| OQ-8 docs/12 citation | fixed-current | The current specification now cites authoritative `docs/12` section 7.2 (line 265). The historical fix-1 artifact and reviews were not rewritten; their recorded text remains immutable historical evidence. |
| OQ-9 role-to-claim policy | deferred-with-owner | Section 9.6 step 7 defers the exact role-to-claim permission to Product/Subject-number and Organization-access, does not narrow the accepted four-role set, and grants no fresh enterprise authorization. Section 15.2 records the no-over-design boundary. Owner: Product/Subject-number and Organization-access; destination: enterprise ownership/role policy and its S1 follow-up. |

## F-01 through F-09 regression status

No accepted fix-1 behavior was removed or mechanically replaced. Predicates and lock order were checked rather than string-replaced.

| Finding | Status | Regression evidence |
| --- | --- | --- |
| F-01 replay/duplicate identity and null-safe organization | preserved | Section 4.14.1 encrypted logical identity and section 9.6 steps 4-6 remain; step 6 now also compares method (OQ-1). Ciphertext equality is still rejected; risk still precedes replay/duplicate success; no stored success is disclosed on mismatch. |
| F-02 fresh claim lifecycle predicates | preserved | Section 9.6 step 3 still requires subject/binding/account/organization/membership status after locks; F-11 adds existence distinction without weakening any predicate. Unregistered/unverified IVR eligibility is unchanged. |
| F-03 function EXECUTE with schema USAGE | preserved | Section 10.8 and 11.3 grants are unchanged apart from the directed owner-column narrowing. |
| F-04 cross-module SELECT and INV-01 organization status | fixed-current via F-10 | Exact membership columns now suffice for the pair key; no `id` grant or privilege widening was added. INV-01 includes `organization.status` and the workspace-projection boundary. |
| F-05 immutable relation columns and guards | preserved | Sections 10.8.1 guards and narrow column grants are unchanged; guard compatibility with the set-once personal owner update remains. |
| F-06 real-name writer SELECT/audit | preserved | `subject_identity_verification_role` keeps `SELECT id`, narrow real-name UPDATE columns, `USAGE` on `subject_number`/`audit` and `EXECUTE` on `audit.append_fact`. |
| F-07 sorted replacement locks and rollback safety | preserved and aligned with OQ-2 | Section 9.7 retains ascending two-number canonical locks, rollback of the old active binding on a loser, and now describes the reread in a fresh bounded retry rather than an aborted transaction. |
| F-08 account bind/disable serialization | preserved | Sections 9.5 and 9.10 retain the account lifecycle advisory plus account row lock and both serial orders; login remains account-state gated. |
| F-09 denylist/function-grant navigation | preserved | Section 10.7 references remain; section 13.1 still rejects `section 10.4`/`section 10.5`. The docs/12 citation is corrected only in the current specification (OQ-8). |

Additional regression checks preserved: function-local `pg_catalog.gen_random_uuid()` IDs; exact USAGE/EXECUTE/column grants; sorted replacement lock rollback; account bind-disable orders; no evidence/history rewrite; no dialing ban; S2/S3 planned scaffolding.

## Validation evidence

All commands were run from `/mnt/d/codex-call` on 2026-10-22. These are document/shell checks only. No PostgreSQL instance, Flyway migration, privilege test, lock/concurrency test, function execution, Testcontainers suite, cloud resource or production/service/UI change was run.

### Unchanged structural script

Command:

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md \
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md \
bash /tmp/a0s1/structural-v2-run.sh
```

Actual output:

```text
fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81
catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24
```

Exit: `0`. The script SHA256 was verified as `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545`; it was not modified or weakened.

### Document counts and navigation

```text
sections=16
dictionaries=13
function_contracts=4
s1_invariant_expansions=14
s1_fk_detail_rows=11
lifecycle_lines=13
sensitive_lines=13
revision_lines=13
sensitive_detail_rows=23
trailing_whitespace=0
stale_membership_id_key=0
stale_docs12_8.3_citation=0
section_10.4_references=0
section_10.5_references=0
```

The section 13.1 check block (including the new stale membership-ID-key and stale-citation assertions and the new contract terms `CLAIM_SUBJECT_NOT_FOUND`, `CLAIM_ACCOUNT_NOT_FOUND`, `CLAIM_ORGANIZATION_NOT_FOUND`, `CLAIM_CONCURRENCY_EXHAUSTED`, `CLAIM_DATA_INTEGRITY`, `BINDING_DISABLED_CONFLICT`, `organization_access.membership:` and `docs/12 section 7.2`) exited `0`.

### Exact S1 columns

The per-table `awk` exited `0` with no output. The exact accepted-column comparison emitted:

```text
platform.idempotency_record exact 12 columns
platform.outbox_event exact 14 columns
audit.audit_fact exact 12 columns
customer_iam.customer_account exact 6 columns
customer_iam.auth_identity exact 10 columns
customer_iam.identity_profile exact 9 columns
organization_access.organization exact 6 columns
organization_access.organization_membership exact 7 columns
operations_iam.employee_account exact 9 columns
subject_number.business_subject exact 9 columns
subject_number.caller_number_binding exact 9 columns
subject_number.subject_claim exact 10 columns
subject_number.subject_risk_state exact 8 columns
ALL_EXACT
```

Total: 121 accepted S1 column rows; none dropped, renamed or replaced.

### Frozen-block comparison

Blocks extracted from the revised specification were compared with the accepted plan using `cmp -s`:

```text
catalog=identical
baseline-matrix=identical
sensitive=identical
fk-matrix=identical
key-catalog=identical
function-wave=identical
slice-S1-tables=identical
slice-S2-tables=identical
slice-S3-tables=identical
wave_rows=identical
invariant_rows=identical
open_input_rows=identical
wave_count=9 invariant_count=40 open_input_count=28
```

### Hashes and scope

- Accepted plan, plan fix-6, accepted plan review, prior S1 review, round-1 re-review, historical S1 implementation artifact and historical fix-1 artifact retain the hashes listed above.
- The structural script retains `ee2a0f17...`.
- `git status --porcelain=v1 | grep -E '\.sql$|db/migration/'` -> no match.
- `git diff --cached --name-only` -> empty.
- Task-owned changes observed: only the current specification (untracked in this branch; updated in place) and this new artifact. Pre-existing modified controller/QA screenshots/`logo/preview.html` and pre-existing untracked historical artifacts remain outside this task's writes. A complete repository-wide pre-dispatch hash sweep was not performed for this bounded fix; the listed immutable hashes and the untouched hashes above are the direct scope evidence.
- Targeted whitespace scan of the current specification, historical implementation artifact and historical fix-1 artifact: `0 0 0`.

## Docs decision

- Update the in-progress final specification `docs/14-production-data-model-and-flyway-schema-spec.md` in place.
- Add this fix-2 artifact.
- Do not modify the accepted plan, plan fix-6, prior reviews, controller, historical fix-1/review artifacts, `docs/09`-`docs/13`, prototypes, screenshots, source, Git metadata or the unrelated old review.
- Do not advance S2/S3, do not claim S1 acceptance and do not claim full A0 completion.
- No documentation index update and no Git operation was performed.

## Residual risks

### fixed-current (owner: A0 S1 author; destination: independent S1 re-review)

| Item | Status |
| --- | --- |
| F-10 membership pair namespace, absent/new/re-grant coverage and no ID grant | fixed in the specification; no runtime proof claimed |
| F-11 unknown subject/account/organization stable no-row outcomes and no raw FK | fixed in the specification |
| F-12 create versus same-subject `DISABLED`, unique-loser mapping and both create/re-enable orders | fixed in the specification |
| F-04 partial state | resolved by the pair key without privilege widening |
| OQ-1/OQ-2/OQ-3/OQ-5/OQ-6/OQ-7/OQ-8 | fixed in the specification |
| F-01 through F-09 regression | preserved; no accepted behavior removed |

### later-approved-slice

| Risk / item | Owner / destination |
| --- | --- |
| S2 must consume the exact section 9.0 namespaces (including the logical membership pair), the subject-before-binding suborder and the canonical-number placement after canonical-caller and before subject/session/call/activation; it must assert both revocation/confirmation serial orders and the current organization/membership/account predicates | S2 author; V004-V007/S2 gate |
| S2 activation/task/claim dictionaries, transactions, `issue_first_trial` V006 contract and the round-6 test note (fresh session rejection; preserve `CONFIRMED`/`UNKNOWN`/unrelated calls) remain unauthored | S2 author; V006/S2 gate and S2 test matrix |
| S3 evidence/media/sharing dictionaries, the deferred evidence FK cycle and final structural closure remain unauthored | S3 author; V008-V009/S3 gate |

### later-work-unit

| Risk / item | Owner / destination |
| --- | --- |
| Executable PostgreSQL 16 DDL, Flyway V001-V003, privilege tests, lock-wait/deadlock tests, rollback/concurrency tests and Testcontainers | Backend/Database; implementation and migration work unit |
| Enterprise ownership/transfer and exact role-to-claim permission (OQ-9) remain fail-closed/deferred | Product/Subject-number and Organization-access; enterprise ownership/role policy and S1 follow-up |
| KMS/envelope/HMAC rotation (Q2), legal retention/erasure (Q3), authentication/session design (Q1/Q19), deployment role provisioning (Q17/Q23) and production provider inputs | Security/Infrastructure/Legal/Platform; named later work units |
| Number recycling/conflict policy (Q9), real-name provider (Q14), task-key retention (Q16), TTL configuration (Q18), call-blocking risk (Q24), task-access roles (Q27) and IVR B-leg recovery (Q28) | Named Product/Risk/IVR/Security/Organization-access/Backend destinations |
| No production PostgreSQL, privilege, lock or concurrency behavior is claimed | Backend/Database; production readiness |

## Completion statement

The bounded S1 fix-2 correction is complete within the allowed two-artifact scope: `docs/14-production-data-model-and-flyway-schema-spec.md` implements the adjudicated F-10/F-11/F-12 and OQ-1 through OQ-9 decisions, and this artifact records exact changes, regression status, actual document checks, docs decision and classified residual risks. The accepted plan and every frozen block are unchanged; the 44/40/9/3 baseline, 13/121 S1 detail, 14 invariant expansions and four V001 function contracts are preserved. The unchanged structural script exits `0`.

No independent re-review, S1 acceptance, S2/S3 authoring, executable migration, runtime/privilege/concurrency test, Git stage, commit, push, PR or merge was performed. The controller decides the next gate; this artifact does not accept itself and does not claim production behavior or full A0 completion.
