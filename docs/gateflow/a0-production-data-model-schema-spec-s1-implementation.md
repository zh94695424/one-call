# A0 S1 implementation artifact

- Task label: `a0-spec-s1-impl-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `implementation`, Slice S1 only (V001-V003)
- Provider: `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Status: S1 authoring completed; independent review pending. S2 and S3 are not authored. No production execution or test is claimed.

## Binding inputs

| Input | Value |
| --- | --- |
| Accepted plan | `docs/gateflow/a0-production-data-model-schema-spec-plan.md`; SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; unchanged |
| Accepted plan review | `docs/reviews/plan-review-20261022-174730.md`; SHA256 `C2432DA173BB4CB61D5554B1A2BF77C7DC5FA4AE79CE5CED1118741BBA34FDCA` |
| Controller | `docs/gateflow/project-execution-control.md`; accepted-plan checkpoint `4a98fbb` |
| Frozen baseline | 44 tables; 40 invariants; 9 waves; 3 slices 13/23/8; lifecycle 28/16; 81 FK decisions = 74/3/2/2; 24 key sets; 5 function-wave rows = 4 V001 + 1 planned V006 |

## Changed paths

1. `docs/14-production-data-model-and-flyway-schema-spec.md` (new final-spec work in progress; S1 portion authored).
2. `docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md` (this artifact).

No other path was written by this task. The pre-existing modified QA screenshots, `logo/preview.html`, the pre-existing modified controller and the unrelated `docs/reviews/plan-review-20261021-205118.md` remain outside this work unit and were not changed by this task. No Git stage, commit, push, PR, merge or later gate was performed. No additional Agent was used.

## S1 scope completed

### Tables

The 13 exact V001-V003 tables are fully expanded in the specification:

1. `platform.idempotency_record`
2. `platform.outbox_event`
3. `audit.audit_fact`
4. `customer_iam.customer_account`
5. `customer_iam.auth_identity`
6. `customer_iam.identity_profile`
7. `organization_access.organization`
8. `organization_access.organization_membership`
9. `operations_iam.employee_account`
10. `subject_number.business_subject`
11. `subject_number.caller_number_binding`
12. `subject_number.subject_claim`
13. `subject_number.subject_risk_state`

Each dictionary records every accepted column, type, nullability, default, named check, key/index, foreign key, revision/immutable rule, lifecycle/deletion class and sensitive representation. The S1 column total is 121.

### Invariants

S1 authored enforcement and real behavior assertions for INV-01 through INV-07, INV-27 through INV-31, INV-33 and the S1 platform/Outbox/audit portion of INV-40. All 14 S1 rows have one owner, a DB/TX/SERVICE class and an exact location in the specification.

### Functions

Exactly four V001 function contracts are authored:

1. `platform.idempotency_begin`
2. `platform.idempotency_complete`
3. `platform.outbox_append`
4. `audit.append_fact`

Each has typed arguments/returns/outcomes, caller and owner roles, `SECURITY DEFINER`, a fixed `search_path`, schema-qualified objects, `PUBLIC` revocation and named EXECUTE grants, no transaction control, same-connection atomic rollback, semantic replay/conflict/finalize-once behavior, immutable results/payloads and redacted/encrypted snapshot rules.

`entitlement_metering.issue_first_trial` is visibly planned in the frozen function-wave block as V006/S2 and is not created, required or tested by S1. The two named append-only trigger guard functions (`audit.reject_row_mutation` and `subject_number.reject_row_mutation`) are structural trigger support required by the accepted append-only controls; they are not cross-schema function-wave contracts and do not change the 4 V001 + 1 V006 function count.

### Frozen baseline preserved

The specification copies the accepted plan's machine-readable catalog, baseline matrix, sensitive inventory, FK catalog, key catalog, function-wave block, nine exact wave rows, exact S1/S2/S3 slice membership, 40 invariant rows and 28 open-input rows as frozen planned baseline. Per-slice status is recorded outside the parser blocks. S2 and S3 dictionaries, transactions, state machines and tests are not authored.

## Validation evidence

All commands were run from `/mnt/d/codex-call` on 2026-10-22. Results below are document/shell checks only.

### Inputs and structural script

```bash
sha256sum docs/gateflow/a0-production-data-model-schema-spec-plan.md
# 149472d291dc19f81497a0599c85122bdda311dc92417c3e2086667130b231a5

sha256sum docs/reviews/plan-review-20261022-174730.md
# c2432da173bb4cb61d5554b1a2bf77c7dc5fa4ae79ce5ced1118741bba34fdca

awk '/^# structural-script-v2$/{f=1} f{print} /^# end structural-script-v2$/{exit}' docs/gateflow/a0-production-data-model-schema-spec-plan.md > /tmp/a0s1/structural-v2-run.sh
sha256sum /tmp/a0s1/structural-v2-run.sh
# ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545

FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh
# fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81
# catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24
# exit 0
```

The extracted script SHA matches the script hash recorded by the accepted round-6 review. `FIX2` was set to the preserved fix-6 artifact; it is a file-presence gate for the frozen baseline, not an S1 content dependency.

### S1 document checks

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
grep -c -E '^## [0-9]+\. ' "$FILE"                    # 16
grep -c -E '^### 4\.[0-9]+ [a-z_]+\.[a-z_]+$' "$FILE" # 13
grep -c -E '^### 10\.[3-6] (platform|audit)\.[a-z_]+$' "$FILE" # 4
grep -c -E '^\| INV-[0-9][0-9] \(S1' "$FILE"           # 14
awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && NF' "$FILE" | wc -l # 5
awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && /^platform\.|^audit\./ && /[|]V001[|]/' "$FILE" | wc -l # 4
grep -c '^| Q[0-9]' "$FILE"                            # 28
sed -n '/^### 5.3 S1 foreign-key detail/,/^## 6\./p' "$FILE" | grep -c '| FK |' # 11
sed -n '/^### 4.3 /,/^## 5\./p' "$FILE" | grep -c '^Purpose:.*Lifecycle:' # 13
sed -n '/^### 4.3 /,/^## 5\./p' "$FILE" | grep -c '^Sensitive representation:' # 13
sed -n '/^### 4.3 /,/^## 5\./p' "$FILE" | grep -c -E '^(Revision/immutability|Append-only control):' # 13
```

The per-table `awk` check in the specification's section 13.2 was also executed and exited 0:

```text
S1 dictionaries: 13 headings, 121 column rows, exact accepted counts
```

The exact accepted-column-name loop in specification section 13.2 was executed with POSIX `awk`/`grep`/`sort` and shell string comparison. It emitted one `exact` line per S1 table and ended with `ALL_EXACT`: platform.idempotency_record 12, platform.outbox_event 14, audit.audit_fact 12, customer_iam.customer_account 6, customer_iam.auth_identity 10, customer_iam.identity_profile 9, organization_access.organization 6, organization_access.organization_membership 7, operations_iam.employee_account 9, subject_number.business_subject 9, subject_number.caller_number_binding 9, subject_number.subject_claim 10, subject_number.subject_risk_state 8. No accepted S1 column was dropped, renamed or replaced.

### Frozen block identity

The following blocks extracted from the specification were compared with the corresponding accepted-plan extractions using `cmp -s`. All were identical:

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
```

### Scope, whitespace and hash safety

```bash
git status --porcelain=v1 | grep -E '\.sql$|db/migration/'
# (none)
grep -c -E '[[:blank:]]+$' docs/14-production-data-model-and-flyway-schema-spec.md
# 0
git status --porcelain=v1 | grep '^??'
# ?? docs/14-production-data-model-and-flyway-schema-spec.md
# ?? docs/reviews/plan-review-20261021-205118.md   (pre-existing, outside scope)
git diff --cached --name-only
# (none)
```

A global `git diff --check` is not a valid S1 pass/fail gate in this worktree because the pre-existing, out-of-scope `logo/preview.html` change contains trailing whitespace. The targeted blank-at-EOL scan on the two new artifacts is the authoritative equivalent check and is clean. The new artifacts are untracked, so a global `git diff --check` does not inspect them either.

### Honest limitations

No PostgreSQL instance, Flyway migration, Maven/Spring project, Testcontainers suite, concurrency test, privilege test, function execution or production test was run. The green structural script proves document structure and frozen membership only. It does not prove S2/S3 detailed dictionaries, executable SQL correctness or production behavior.

## Docs decision

- Add `docs/14-production-data-model-and-flyway-schema-spec.md` as the in-progress final specification; only its S1 portion is authored.
- Add this S1 implementation artifact.
- Do not modify the accepted plan, historical fix/review artifacts, controller, `docs/09` through `docs/13`, `docs/README.md`, source/prototype files, QA screenshots or unrelated review artifacts.
- No documentation index update is made; controller authority remains unchanged.
- No retention period, capacity value, credential, provider configuration or legal policy is recorded as approved.

## Residual risks and classification

### Fixed-current

| Risk / item | Status | Owner / destination |
| --- | --- | --- |
| 13 S1 dictionaries, 121 columns, named checks/indexes/FKs/lifecycle/sensitive rules | fixed in this artifact | A0 S1 author; independent review |
| Four V001 function contracts with typed outcomes and grant matrix | fixed in this artifact | A0 S1 author; independent review |
| S1 invariant expansion for INV-01..07, 27..31, 33, 40 portion | fixed in this artifact | A0 S1 author; independent review |
| Frozen catalog/matrix/sensitive/FK/key/function/wave/slice/invariant/open-input blocks | preserved byte-identically | A0 S1 author; independent review |
| No S1 later-slice prerequisite | validated by the frozen structural script | controller / independent review |

### Later-approved-slice

| Risk / item | Owner / destination |
| --- | --- |
| S2 catalog/order/payment, IVR, entitlement, activation dictionaries and transactions | S2 author; V004-V007/S2 gate |
| S2 `issue_first_trial` V006 function contract and first-trial tests | S2 author; V006/S2 gate |
| S3 evidence/media/sharing dictionaries, deferred FK cycle, transactions and tests | S3 author; V008-V009/S3 gate |
| S2/S3 state machines, sensitive expansions, lifecycle details and final 44-table closure | S2/S3 authors; later slices |
| Round-6 test-coverage note: category-1 permitted never-confirmed session rejection and preservation of already-CONFIRMED/UNKNOWN/unrelated calls | S2 author; S2 test matrix |

### Later-work-unit

| Risk / item | Owner / destination |
| --- | --- |
| Executable PostgreSQL/Flyway migrations and Testcontainers concurrency, privilege, rollback and failure-injection tests | Backend/Database; implementation and migration work units |
| KMS product, key hierarchy, AEAD/HMAC rotation and envelope algorithm (Q2) | Security/Infrastructure; security design and COS/KMS validation |
| Legal retention, erasure, backup deletion and audit retention (Q3) | Legal/Security; retention/deletion policy and launch review |
| Employee authentication source, MFA and credential rotation (Q1) | Security/Backend; security design and operations IAM |
| WeChat appid/openid/unionid mapping and login-identity association (Q19) | Identity/Backend; authentication design and security review |
| Role provisioning, schema ownership transfer and SECURITY DEFINER deployment identity (Q17/Q23) | Platform/Database/Security; deployment/security design |
| Number recycling/conflict resolution (Q9) | Product/Risk/Operations; claim-risk policy and operations runbook |
| Real-name verification provider and re-verification policy (Q14) | Security/Product; verification adapter and identity design |
| Pending-task TTL configuration key and scheduler cadence (Q18) | Product/Backend; IVR protocol and operations configuration |
| Future call-blocking risk rule beyond claim-only risk (Q24) | Product/Risk/IVR; claim-risk/IVR policy and launch review |
| Exact organization role-to-task-permission matrix (Q27) | Organization-access/Security; authorization design and S2 policy tests |
| IVR-local B-leg key, persistence and lost-response recovery (Q28) | Telephony/IVR; IVR protocol and line integration |
| Customer session storage, OAuth/SMS integration and account-recovery flow | Customer-iam/Backend; authentication implementation |
| Capacity, SLO, RPO/RTO, cloud provisioning and provider integration | Capacity/Security/Operations/Infrastructure; later work units |

### Uncovered areas

S1 does not implement or validate production OAuth/SMS/WeChat authentication, session storage, KMS encryption, HMAC rotation, Flyway execution, database constraints, function privileges, concurrency, retention hooks, S2/S3 tables or any API/UI/cloud behavior. These are explicitly outside this S1 authoring gate.

## Completion statement

S1 authoring is complete within the allowed two-artifact scope. The accepted plan hash is unchanged, the frozen 44/40/9/3 baseline is preserved, the 13 S1 dictionaries and four V001 function contracts are fully specified, and the document checks pass. No independent review, later slice, Git stage/commit/push/PR/merge or production test was performed. No essential plan conflict was found; no blocker is declared.
