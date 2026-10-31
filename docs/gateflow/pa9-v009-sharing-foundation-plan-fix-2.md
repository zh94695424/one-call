# PA9 V009 Sharing Persistence Foundation — Plan Fix 2

- Task label: `pa9-v009-planfix2-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`.
- Gate: `fix` — bounded plan fix only; author date: 2026-10-31, Asia/Shanghai.
- status: plan-fix-complete
- Author completion only: no plan acceptance, implementation authorization, review pass or production-readiness claim. The controller owns the next fresh independent A/B re-review gate.

## 1. Inputs and plan identity

Read the current plan, fix-1 record, active control adjudication in `docs/gateflow/project-execution-control.md`, latest re-review A `docs/reviews/plan-review-20261031-075243.md` and B `docs/reviews/plan-review-20261031-075632.md`, frozen `docs/14-production-data-model-and-flyway-schema-spec.md` sharing dictionary/state/guard/privilege contracts (especially §§4.44, 7.4, 10.21–10.22), V008 and cited source/test facts. Inspected the existing `PostgresContainerIT.java` migration/hashing/cleanup harness, CI expected-set checks, container-smoke POM and backend README facts. These are static inputs, not newly executed tests.

| Plan identity | SHA256 |
| --- | --- |
| Pre-fix-2 `docs/gateflow/pa9-v009-sharing-foundation-plan.md` | `2c66a3f7c855d8996b58633defdb957bc2267812bfa1e8df98446925d18aeb5d` |
| Post-fix-2 `docs/gateflow/pa9-v009-sharing-foundation-plan.md` | `951a27df6db1d36a6acb44f08e8d0dead114584753fc8ad1aba89c6c8844ae5a` |

Pre-fix bytes matched the supplied hash. Read-only explicit Git metadata resolves HEAD to `835f2ecece1ac8b5c79ab794427424ea380ac052`. Baseline plan bytes, complete file-hash manifest and tracked diff are retained outside the repository at `/tmp/pa9-v009-planfix2-gpt-20261031-01/`. The Windows `.git` indirection was not repaired. Original plan provenance/completion wording and fix-1 remain historical; this record supplies the separate fix-2 authorization and two-file task delta.

## 2. Accepted findings and exact changed sections

| Finding | Exact plan sections changed | Resolution |
| --- | --- | --- |
| RR-01 | §4.1 enforcement paragraph; §7.4 new explicit extension-predicate witnesses and existing exact mixed-action probe | Bind extension eligibility to OLD.status and OLD.expires_at, not NEW.status; require all four witness classes, retaining mixed structural success and service prohibition. |
| RR-02 | §4.1 enforcement paragraph; §7.4 lifecycle matrix assertions | Always forbid generation decreases, qualify edge-exact revoke/expiry increments by actual status change, prohibit a blanket generation-increase status-edge allow-list, preserve rotation coupling and lock neutrality. |

No other plan section changed. No new finding was introduced, accepted scope reopened or frozen-design incompatibility found. Existing same-state terminal probes in §7.4 remain unchanged and are explicitly binding on the clarified lifecycle oracle.

## 3. RR-01 resolved predicate and four witnesses

Exact resolved predicate wording in §4.1:

> Increasing expiry requires `OLD.status = 'ACTIVE'` and `clock_timestamp() < OLD.expires_at` (`SHARE_GRANT_EXTENSION_INVALID`); the resulting `NEW.status` need not be ACTIVE. This predicate applies only when `NEW.expires_at > OLD.expires_at`.

OLD refers to the stored pre-update row; NEW refers to the resulting row. No `NEW.status = 'ACTIVE'` requirement is introduced. Expiry decreases remain refused, and revision/generation/rotation/status rules continue to apply independently. The named pure extension remains ACTIVE → ACTIVE; structural mixed success does not authorize a mixed owner action.

All witnesses belong to the already-planned S2 method `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops`; no method or service is added:

| Witness | OLD.status → NEW.status | Expiry / revision / generations | Required structural outcome and classification |
| --- | --- | --- | --- |
| Lock-only | ACTIVE → LOCKED | Expiry unchanged; revision +1; generations/count unchanged | One-row success; extension-increase predicate not invoked; named lock witness. |
| Pure extension | ACTIVE → ACTIVE | Strict expiry increase with future OLD.expires_at; revision +1; generations/count unchanged | One-row success; named extension witness. |
| Mixed | ACTIVE → LOCKED | Strict expiry increase with future OLD.expires_at; revision +1; generations/count unchanged | One-row structural success despite NEW.status LOCKED; service-forbidden. |
| Non-ACTIVE OLD.status | LOCKED → LOCKED, EXPIRED → EXPIRED, REVOKED → REVOKED, separately | Attempt strict expiry increase; future OLD.expires_at; revision +1; otherwise unchanged | P0001/SHARE_GRANT_EXTENSION_INVALID; no mutation. Future expiry isolates the status condition rather than a failing clock condition. |

The exact mixed SQL remains `SET status = 'LOCKED', expires_at = expires_at + interval '1 hour', revision = revision + 1` with the existing id/status/expected-revision/future-expiry WHERE. One hour is synthetic test data, not production policy. Each witness uses a valid method-local fixture, sharing_role, lock/read baseline, SAVEPOINT and all frozen guards enabled. Success must change exactly one row and only specified fields; refusal leaves no mutation. ROLLBACK TO SAVEPOINT must restore the full baseline row/history, followed by transaction ROLLBACK and fresh-connection equality for status, expiry, revision, generations, count, all other columns and admission/event counts. No successful probe mutation is committed and no guard is disabled for these probes. These are future test requirements, not observed runtime outcomes.

## 4. RR-02 generation rule and terminal witness

Exact resolved wording in §4.1:

> Generation decreases are always structurally forbidden. Exact revocation_generation +1 for named status-changing revoke/expiry edges applies only when `OLD.status IS DISTINCT FROM NEW.status`; ACTIVE → LOCKED remains generation-neutral. Same-state generation increases are not subject to an exclusive named-status-edge allow-list; exact code-rotation coupling and generation increments still apply whenever code values/code_generation change, including same-state rotation.

This is not blanket permission for arbitrary generation changes: exact salt/hash/code_generation coupling remains intact, rotation retains code_generation +1 and revocation_generation +1, lock remains generation-neutral, and generation/expiry/code/count changes still require revision +1 under the existing rules. Named admission/lock/extension actions keep their service-level unchanged-generation obligations. Status-changing revoke/expiry checks must not be applied as a ban on same-state revocation_generation increases.

For each separately committed valid method-local EXPIRED and REVOKED fixture established through its named transition, the existing S2 probe remains exact:

`UPDATE sharing.evidence_share_grant SET revocation_generation = revocation_generation + 1, revision = revision + 1 WHERE id = :id AND status = :terminal_status AND revision = :old_revision`

With status unchanged and all other fields unchanged, expect exactly one affected row, both increments exactly +1 and no exception: structurally accepted but service-forbidden, not a valid expiry/revoke replay or proof of exactly-once service behavior. Retain BEGIN, lock/read, SAVEPOINT, full-row/history restoration after ROLLBACK TO SAVEPOINT, transaction ROLLBACK and fresh-connection verification. No probe mutation is committed. §7.4 additionally requires same-state generation-decrease negatives with otherwise valid nonnegative results and P0001/SHARE_GRANT_GENERATION_INVALID, alongside edge-exact increments, lock neutrality and rotation coupling. An implementation/oracle that accepts generation increases only on named status-changing edges fails these required successes.

## 5. Unchanged boundaries and residual ownership

Tables, columns, checks, indexes, FKs, defaults, ACLs, slices and test counts remain unchanged: three sharing tables, 38 columns, 31 named checks, three PKs, five UNIQUE constraints, six ordinary immediate RESTRICT FKs, nine defaults, five secondary indexes and 13 physical indexes (5/4/4), three owner-local helpers and four triggers. Exact ACLs, frozen five-function baseline, all file boundaries and non-goals remain binding. No new DDL object, service, policy value or implementation is added; existing plan predicates are clarified, not expanded into new bans. V001–V008 bytes are unchanged.

Three slices, seven added methods plus one rename, Failsafe 57→60→62→64 and ten Surefire tests remain unchanged. No migration, application, test, CI, README, control, review or fix-1 file was edited. No blocking open question remains for RR-01/RR-02; existing residuals retain their owners/destinations:

| Residual / open input | Owner / destination |
| --- | --- |
| Mixed-action structural success and same-state terminal generation inflation; one named action and exactly-once replay are not DB guarantees | Sharing/Backend; excluded owner transition/replay service; S2 supplies structural/rollback evidence only. |
| Lock discipline, verified inputs/generations, expected revision, count/event atomicity, typed JSON and grant/admission pair semantics | Sharing/Backend/Security; excluded sharing services, typed validation and Q26 mapping. |
| SOLIDIFIED projection, deterministic fingerprints/replay, current ownership/risk/member checks | Evidence/Sharing/Security; later owner collaborators/services and access-policy design. |
| Q12 KDF/SMS/limits/expiry/rate and Q22 tickets/skew/revocation policy | Security/Product/Sharing; policy approval and later service/ticket work; no values guessed. |
| Retention/erasure, media/object lock/KMS and Q3/Q11/Q17/Q23 deployment inputs | Legal/Security/Infrastructure/Platform/Database; existing policy/storage/deployment gates. |
| PostgreSQL deparse/provisioning, source=CI=XML drift, cleanup/blocked-writer proof | Backend/Database/QA and implementer; separately authorized S1/S2/S3 validation. |
| PA8 nonunit-revision negative witness | Evidence/Backend QA; separate post-PA8 test hardening, not V008 changes. |
| Intake controller dirt, private files and Windows Git indirection | Controller; preserve bytes and use read-only explicit metadata; no repository repair. |
| Runtime results, independent plan acceptance, remote CI and production readiness remain unproven | Controller/QA; fresh independent plan re-review, later implementation acceptance/CI and production gates. |

## 6. Changed paths and validation

Exactly two task-owned changed paths:

1. Modified `docs/gateflow/pa9-v009-sharing-foundation-plan.md` (already untracked at intake).
2. Created `docs/gateflow/pa9-v009-sharing-foundation-plan-fix-2.md` (the sole new repository artifact).

Read-only Git uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`; no metadata/configuration is changed. `git diff --check` exits 0 without whitespace errors. Each entire task file is checked using `git -c core.autocrlf=false diff --no-index --check /dev/null <path>`, and the plan is checked against saved pre-fix bytes: exit 1 with no diagnostics denotes different no-index inputs, not whitespace failure. The command-local option changes no repository configuration or bytes.

Final validation recomputes the complete file-hash manifest: only the plan is modified, only this artifact is added, no path is deleted, and the plan post-hash matches §1. The tracked binary diff is byte-identical to intake; controller dirt, `.codex`, fix-1, reviews, design and code/test inputs are preserved. The saved-plan diff changes only §§4.1 and 7.4. Whole-file/delta whitespace checks report no errors.

No implementation/runtime claim is made. No Maven, Docker, PostgreSQL, network call, mutating Git, staging, commit, push, PR/issue operation, approval, ready action or external comment was performed. Stop after validating this two-file task delta; status: plan-fix-complete.
