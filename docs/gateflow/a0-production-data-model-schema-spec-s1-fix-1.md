# A0 S1 fix 1 artifact

- Task label: `a0-spec-s1-fix1-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix`, Slice S1 (V001-V003) only
- Provider: `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Review base: `4a98fbb`
- Status: bounded S1 fix completed; independent re-review pending. S2/S3 remain explicitly planned and are not authored. Specification only; no executable SQL, database, service, UI, cloud, credential or production work.

## Binding inputs

| Input | Value |
| --- | --- |
| Controller and adjudication | `docs/gateflow/project-execution-control.md`; final S1 review adjudication and binding fix contract |
| Accepted plan (frozen) | `docs/gateflow/a0-production-data-model-schema-spec-plan.md`; SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5` |
| Accepted plan review | `docs/reviews/plan-review-20261022-174730.md`; SHA256 `C2432DA173BB4CB61D5554B1A2BF77C7DC5FA4AE79CE5CED1118741BBA34FDCA` |
| Failed S1 code review | `docs/reviews/code-review-20261022-181806.md`; SHA256 `E813BFDA4BA09FC1E99416FFB878CD0739D2EEAA1C5C6EDC7C36F1653D7E4726` |
| Historical S1 implementation artifact | `docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md`; SHA256 `1767FFD21C09173F1EFBF468B627A6166DB40F393D8D4D5DE123C5E901A9C12D`; read-only and unchanged |
| Structural script | `/tmp/a0s1/structural-v2-run.sh`; SHA256 `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545`; unchanged |
| Preserved fix-6 gate | `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`; SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59` |

Frozen baseline preserved: 44 tables; 40 invariants; 9 waves; 3 slices 13/23/8; lifecycle 28 mutable / 16 append-only; 28 sensitive tables; 81 FK decisions = 74 `FK` + 3 `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK`; 24 key sets; 5 function-wave rows = 4 V001 functions + 1 planned V006 function. S1 remains 13 tables / 121 accepted columns / 14 invariant expansions / 4 V001 function contracts. S2/S3 are visibly planned scaffolds, not implementation claims.

## Changed paths

1. `docs/14-production-data-model-and-flyway-schema-spec.md` (the S1 specification; updated in place).
2. `docs/gateflow/a0-production-data-model-schema-spec-s1-fix-1.md` (this fix artifact; new).

No other path was written. The controller, accepted plan, plan review, S1 code review, historical S1 implementation artifact, business source documents, prototypes, screenshots, configuration, Git metadata and unrelated review artifact remain read-only and unchanged by this task. No Git stage, commit, push, PR or merge was performed. No additional Agent was used.

## Fix status F-01 through F-09

| Finding | Status | Fix and exact location | Evidence |
| --- | --- | --- | --- |
| F-01 replay and same-account duplicate are unbound; different organization can collapse | fixed | Added the versioned encrypted verification-reference payload in section 4.14.1 carrying immutable `schemaVersion`, `subjectId`, `bindingId`, `numberHmac`, `verificationMethod` and `proofRef`. Section 9.6 steps 1, 3, 4, 5 and 6 decrypt and compare logical values server-side; ciphertext equality is explicitly not proof. Replay requires stored subject/account, null-safe organization, method and decrypted binding/number/proof identity to match before any stored success. Incomplete/unresolvable references fail closed. Same-account/different-organization or different-binding/proof is `CLAIM_CONTEXT_CONFLICT`, not a reuse. Risk is checked on the locked current binding before replay/duplicate success. The stable claim-code table records `CLAIM_REPLAY_CONFLICT`, `CLAIM_REPLAY_UNRESOLVABLE`, `CLAIM_CONTEXT_CONFLICT`, `CLAIM_RISK_BLOCKED`, `CLAIM_DATA_INTEGRITY` and `CLAIM_VERIFICATION_REF_INVALID`. | Sections 4.14.1, 9.6, 6.2 INV-07, 3.3. No new column, fingerprint column or platform idempotency scope. |
| F-02 fresh claim lacks current binding/account/organization lifecycle predicates | fixed | Section 9.6 step 2 fixes the post-lock order; step 3 requires binding belongs to the target subject, `ACTIVE` with `verified_at`, supplied canonical-number HMAC consistency, authenticated account `ACTIVE`, and for an organization context an `ACTIVE` organization, `ACTIVE` membership and current approved access. Section 9.6 step 7 defines the S1 organization-access conjunction and the deferred enterprise boundary. The claim path does not require subject real-name or activation and imposes no dialing ban. | Sections 9.6, 9.0, 9.12; INV-07 expansion. |
| F-03 function EXECUTE without schema USAGE | fixed | Section 10.8 grants `USAGE` without `CREATE` on `platform` and `audit` to the exact S1 function callers and later-slice roles in their waves; `operations_service_role` receives audit `USAGE`/`EXECUTE` only. Section 11.3 grants schema `USAGE` and function `EXECUTE` in V001. No direct platform/audit table grants, no schema `CREATE`. | Sections 2.1, 2.8, 10.2, 10.8, 11.3. |
| F-04 missing cross-module SELECT privileges; INV-01 omits organization status | fixed | Section 10.8 adds exact cross-module `SELECT` grants: `customer_iam_role` reads organization `id/display_name/status` and membership `organization_id/account_id/role/status`; `subject_number_role` reads organization `id/status`, membership `organization_id/account_id/role/status`, and account `id/status`, with schema `USAGE`. No cross-module insert/update/delete. Section 9.4 step 3 and INV-01 include `organization.status`. | Sections 9.4, 10.8, 6.2 INV-01, 11.3 V002. |
| F-05 immutable relation columns remain updatable | fixed | Section 10.8 replaces broad table `UPDATE` with narrow column grants. Section 10.8.1 defines local `SECURITY INVOKER` `BEFORE UPDATE` guard triggers for `customer_account`, `auth_identity`, `identity_profile`, `organization`, `membership`, `employee_account`, `business_subject`, `caller_number_binding` and `subject_risk_state`, with exact predicates and a controlled key-rotation exception. Section 11.3 installs them in V002/V003. INV-02 now names the guard and bind/disable locks; INV-05 and INV-33 name the owner/real-name guards. | Sections 4.6-4.15, 10.8, 10.8.1, 11.3, 6.2 INV-02/05/33, 12.4. |
| F-06 real-name writer cannot locate its row | fixed | Section 10.8 grants `subject_identity_verification_role` `SELECT` on `business_subject.id`, status, revision and `updated_at`, `UPDATE` only on the two real-name columns plus revision/updated_at, `USAGE` on `subject_number`/`audit`, and `EXECUTE` on `audit.append_fact`. The role cannot write owner, source, binding, claim or risk rows. Section 11.3 applies the grants in V003. | Sections 3.1, 4.12, 10.8, 11.3, 6.2 INV-33. |
| F-07 replacement does not serialize the new canonical number | fixed | Section 9.7 acquires the old and new canonical-number advisory locks in sorted HMAC order before any binding row, then subject advisory/subject `FOR SHARE`, then binding rows in ascending UUID. It validates the destination, returns `BINDING_SAME_NUMBER`, `BINDING_ACTIVE_CONFLICT` or `BINDING_RETIRED_CONFLICT`, supports an already-active same-subject destination, and rolls back to retain the old active binding on any loser. Section 9.7 step 8 lists replace/create/re-enable/same-number assertions. | Sections 9.0, 9.7, 7.1, 9.12. |
| F-08 identity bind not serialized with account disable | fixed | Section 9.5 locks the account lifecycle advisory lock and the parent account row `FOR UPDATE`, rechecks `ACTIVE` before insert, and defines both serial orders: disable first rejects the bind; bind first retains the identity but login remains blocked by the active-account check. Section 9.10 gives the disable/re-enable writer the same locks. No identity-only login bypass is claimed. | Sections 9.5, 9.10, 6.2 INV-02, 9.12. |
| F-09 wrong section references | fixed | All five protected-key denylist references now point to section 10.7 (sections 2.6, 4.3, 4.4, 4.5 and INV-31). The common function-grant references now point to section 10.8 (sections 2.8, 9.2, 10.2 and the existing V001 steps). Section 13.1 rejects `section 10.4` and `section 10.5` navigation regressions. | Sections 2.6, 2.8, 4.3, 4.4, 4.5, 6.2 INV-31, 9.2, 10.2, 13.1. |

## Open-question adjudication

| Open question | Adjudication and location |
| --- | --- |
| Function-inserted row IDs | `platform.idempotency_begin`, `platform.outbox_append` and `audit.append_fact` generate the inserted `id` with function-local PostgreSQL 16 `pg_catalog.gen_random_uuid()`. No table default and no `p_id` argument exists. Caller-owned rows remain application-generated. Sections 2.2, 4.2, 4.3-4.5, 10.3, 10.5, 10.6. |
| S1/S2 lock-order compatibility | Section 9.0 defines exact advisory namespaces for canonical caller, canonical number, subject lifecycle, organization lifecycle, membership lifecycle, account lifecycle and administrator mutation. The combined subject-owner/current-binding step is internally subject-before-binding. S1 locks the subject row before the binding; S2 reads the subject owner under the subject lifecycle advisory lock before the binding and does not take a cross-owner row lock. S2 must carry the same order and the two serial-order revocation/confirmation assertions forward. |
| Claim binding evidence at replay | The existing `verification_ref_ciphertext` is the versioned encrypted replay-identity carrier (section 4.14.1). The service compares decrypted logical binding/number/proof values; randomized ciphertext equality is not used. No new column, fingerprint column or platform scope is added; incomplete references fail closed. |
| Organization-scoped claim ownership | A fresh organization-scoped claim is fail-closed with `CLAIM_ORGANIZATION_OWNERSHIP_UNAPPROVED`, no accepted row and no owner change. The approved verified-personal claim of an unowned IVR subject remains supported and sets only `owner_account_id`. An existing accepted organization-scoped relation is reused only after full identity, explicit-owner, current organization/membership/role/access matching. The deferred enterprise owner/destination is Product/Subject-number. Sections 3.3, 9.6 step 7, 14 and 15.2. |
| Subject real-name audit | The narrow verification role may append the same-transaction verification fact through the existing `audit.append_fact`, with `USAGE` on `audit` and no new audit service or table privilege. Sections 3.1, 10.8, 11.3. |
| Artifact evidence discrepancy | The historical S1 implementation artifact is preserved unchanged. The controller record is authoritative: all 445 pre-review path hashes were unchanged and exactly the S1 review artifact was added. This fix does not reproduce the incomplete historical transcript as a current check and does not claim an unverified writer path. |

## Enterprise policy boundary

The enterprise claim/ownership policy remains deferred. docs/11 sections 2 and 6 assign organizational ownership/transfer to the future business-subject service; docs/12 section 8.3 requires refusing ambiguous ownership. S1 therefore fails closed for a fresh organization-scoped claim instead of choosing personal versus organization ownership from an optional `organization_id`. The approved personal claim of an unowned IVR subject is preserved. Owner: Product/Subject-number; destination: enterprise claim/ownership policy and its S1 follow-up. This is an explicit refusal, not a guessed policy.

## Validation evidence

All commands were run from `/mnt/d/codex-call` on 2026-10-22. These are document/shell checks only. No PostgreSQL instance, Flyway migration, Maven/Spring project, Testcontainers suite, privilege test, concurrency test, function execution, cloud resource or production/service/UI change was run.

### Branch, status and hashes

```bash
git branch --show-current
# codex/project-execution
git rev-parse --short HEAD
# 4a98fbb

sha256sum docs/gateflow/a0-production-data-model-schema-spec-plan.md
# 149472d291dc19f81497a0599c85122bdda311dc92417c3e2086667130b231a5
sha256sum docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md
# 8976f64a215a2b7b5cb116a14d5123aff91f754f512b6fd212e0d8c0c7045f59
sha256sum docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md
# 1767ffd21c09173f1efbf468b627a6166db40f393d8d4d5de123c5e901a9c12d
sha256sum docs/reviews/code-review-20261022-181806.md
# e813bfda4ba09fc1e99416ffb878cd0739d2eeaa1c5c6edc7c36f1653d7e4726
sha256sum /tmp/a0s1/structural-v2-run.sh
# ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545
sha256sum docs/14-production-data-model-and-flyway-schema-spec.md
# 1af4d59258c210a49c314ce5c605d2d7050178acbe3440ca6c1a4b54bdbf5f06
```

Scoped `git status --porcelain=v1` (screenshots and `logo/preview.html` are pre-existing, out-of-scope live changes) showed the pre-existing modified controller, untracked `docs/14-production-data-model-and-flyway-schema-spec.md`, untracked historical S1 implementation artifact, untracked S1 review, the pre-existing untracked `docs/reviews/plan-review-20261021-205118.md`, and this new fix artifact. No `.sql` or `db/migration` path and no staged file existed. The controller's binding record remains: 445 pre-review hashes unchanged and exactly the S1 review artifact added.

### Unchanged structural script

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md \
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md \
bash /tmp/a0s1/structural-v2-run.sh
# fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81
# catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24
# exit 0
```

The script was not modified or weakened. It preserves the frozen 44/40/9/3, 28/16, 28-sensitive, FK 81 = 74/3/2/2 and 24-key-set baseline.

### Section, dictionary, function, invariant and control checks

```text
sections=16
dictionaries=13
function_contracts=4
s1_invariant_expansions=14
function_wave_rows=5
v001_function_rows=4
open_input_rows=28
s1_fk_detail_rows=11
lifecycle_lines=13
sensitive_lines=13
revision_lines=13
sensitive_detail_rows=23
trailing_whitespace=0
denylist_nav=ok
grant_nav=ok
fix_contract_terms=ok
```

The per-table dictionary `awk` exited `0`. The exact accepted-column-name comparison emitted:

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

This is 121 accepted S1 column rows with no accepted column dropped, renamed or replaced.

### Frozen-block comparison

The following blocks extracted from the revised specification were compared with the corresponding accepted-plan extractions using `cmp -s`:

```text
catalog=identical
sensitive=identical
baseline-matrix=identical
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

### Scope and whitespace

```bash
git status --porcelain=v1 | grep -E '\.sql$|db/migration/'
# (none)
git diff --cached --name-only
# (none)
grep -c -E '[[:blank:]]+$' docs/14-production-data-model-and-flyway-schema-spec.md docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md docs/gateflow/a0-production-data-model-schema-spec-s1-fix-1.md
# 0 0 0
```

The new specification/artifact are untracked, so a global `git diff --check` does not inspect them; the targeted blank-at-EOL scan is the whitespace gate. Pre-existing QA screenshot changes, `logo/preview.html` and the unrelated old review remain outside this work unit.

## Docs decision

- Update the in-progress final specification `docs/14-production-data-model-and-flyway-schema-spec.md` in place.
- Add this fix-1 artifact.
- Do not modify the accepted plan, plan review, S1 code review, historical S1 implementation artifact, controller, `docs/09`-`docs/13`, prototypes, screenshots, source, Git metadata or unrelated artifacts.
- Do not advance S2/S3, do not claim S1 acceptance and do not claim full A0 completion.
- No documentation index update and no Git operation is performed.

## Coverage and uncovered areas

Covered:

- all accepted F-01 through F-09 corrections and the OQ adjudications above;
- claim replay identity, same-account/organization handling, enterprise fail-closed boundary and risk preemption;
- fresh claim lifecycle predicates and stable claim outcomes;
- function/schema `USAGE` and `EXECUTE`, exact cross-module `SELECT`, narrow owned-column grants and local guard triggers;
- subject real-name writer `SELECT id`/audit path;
- canonical-number two-key replacement ordering and both identity bind/disable serial orders;
- common S1/S2 lock namespaces, subject-before-binding suborder and inversion prevention;
- function-local `pg_catalog.gen_random_uuid()` IDs;
- denylist and function-grant navigation;
- document counts, frozen blocks, whitespace and scope.

Uncovered:

- executable PostgreSQL DDL, Flyway migrations, privilege tests, lock-wait/deadlock tests, rollback tests and Testcontainers;
- S2/S3 dictionaries, transactions, state machines and tests;
- enterprise ownership/transfer product policy;
- KMS/key-rotation implementation, legal retention/erasure, authentication/session design, deployment role provisioning and provider integrations.

## Residual risks

### Current-fixed (owner: A0 S1 author; destination: independent re-review)

| Item | Status |
| --- | --- |
| F-01 replay binding, null-safe organization and same-account context conflict | fixed in the specification; no runtime proof claimed |
| F-02 post-lock binding/account/organization predicates | fixed in the specification |
| F-03/F-04 schema `USAGE`, function `EXECUTE` and exact cross-module `SELECT` | fixed in the specification |
| F-05 narrow column grants and guard triggers | fixed in the specification |
| F-06 real-name role `SELECT id` and audit path | fixed in the specification |
| F-07 two-number replacement serialization | fixed in the specification |
| F-08 bind/disable account serialization | fixed in the specification |
| F-09 denylist/function-grant navigation | fixed in the specification |
| OQ IDs and S1/S2 lock namespace/suborder | fixed in the specification |

### Later-approved-slice

| Risk / item | Owner / destination |
| --- | --- |
| S2 must consume the exact section 9.0 namespaces and subject-before-binding suborder; S2 must assert both revocation/confirmation serial orders and carry the current organization/membership/account predicates forward | S2 author; V004-V007/S2 gate |
| S2 activation/task/claim dictionaries, transactions, `issue_first_trial` V006 contract and tests | S2 author; V006/S2 gate |
| S3 evidence/media/sharing dictionaries, deferred FK cycle and final structural closure | S3 author; V008-V009/S3 gate |
| S2 test matrix must explicitly cover the permitted never-confirmed session rejection and preservation of already-`CONFIRMED`, current `UNKNOWN` and unrelated calls | S2 author; round-6 test-coverage note |

### Later-work-unit

| Risk / item | Owner / destination |
| --- | --- |
| Executable PostgreSQL/Flyway migrations, privilege tests, lock-order/deadlock tests, rollback tests and Testcontainers | Backend/Database; implementation and migration work unit |
| Enterprise claim/ownership and transfer policy; current S1 behavior is fail-closed, not guessed | Product/Subject-number; enterprise policy and S1 follow-up |
| KMS product, key hierarchy, AEAD/HMAC rotation and envelope algorithm (Q2); the controlled `auth_identity` key-rotation path is not a runtime service | Security/Infrastructure; security design and COS/KMS validation |
| Legal retention, erasure, backup deletion and audit retention (Q3) | Legal/Security; retention/deletion policy and launch review |
| Employee authentication source, MFA and credential rotation (Q1) | Security/Backend; security design and operations IAM |
| WeChat appid/openid/unionid mapping and multi-app identity association (Q19) | Identity/Backend; authentication design and security review |
| Flyway role provisioning, schema ownership transfer and `SECURITY DEFINER` deployment identity (Q17/Q23) | Platform/Database/Security; deployment/security design |
| Number recycling/conflict resolution (Q9) | Product/Risk/Operations; claim-risk policy and operations runbook |
| Real-name verification provider and re-verification policy (Q14) | Security/Product; verification adapter and identity design |
| Pending-task TTL configuration key and scheduler cadence (Q18) | Product/Backend; IVR protocol and operations configuration |
| Future call-blocking risk beyond claim-only risk (Q24) | Product/Risk/IVR; claim-risk/IVR policy and launch review |
| Exact organization role-to-task-permission matrix (Q27) | Organization-access/Security; authorization design and S2 policy tests |
| IVR-local B-leg key, persistence and lost-response recovery (Q28) | Telephony/IVR; IVR protocol and line integration |
| Capacity, SLO, RPO/RTO, cloud provisioning and provider integration | Capacity/Security/Operations/Infrastructure; later work units |

No new unowned risk was introduced. The unresolved enterprise ownership policy is explicitly fail-closed rather than guessed.

## Completion statement

The bounded S1 fix is complete within the allowed two-artifact scope: `docs/14-production-data-model-and-flyway-schema-spec.md` is corrected and this artifact records the F-01 through F-09 dispositions, OQ adjudications, actual document checks, docs decision, coverage, residual-risk owners/destinations and the enterprise fail-closed boundary. The accepted plan and all frozen source blocks are unchanged, the 44/40/9/3 baseline and 13/121 S1 detail are preserved, and the unchanged structural script exits `0`.

No independent re-review, acceptance, S2/S3 authoring, executable migration, runtime test, stage, commit, push, PR or merge was performed. The controller decides the next gate; this artifact does not accept itself and does not claim production behavior.
