# PA9 V009 Sharing Persistence Foundation — Plan Fix 1

- Task label: `pa9-v009-planfix1-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`.
- Gate: `fix` — bounded plan fix only; author date: 2026-10-31, Asia/Shanghai.
- status: plan-fix-complete
- Author completion only: no plan acceptance, implementation authorization, review pass or production-readiness claim. Fresh independent A/B re-review remains the controller's next gate.

## 1. Frozen inputs and plan identity

Read the current plan, the active 2026-10-31 checkpoint and adjudication in `docs/gateflow/project-execution-control.md`, both independent reviews (`docs/reviews/plan-review-20261031-072631.md` and `docs/reviews/plan-review-20261031-073319.md`), the frozen doc-14 sharing table/state/privilege/structural-trigger contracts, V008, and cited `PostgresContainerIT.java`, CI, POM and backend README facts. In particular, doc-14 §§4.44, 7.4 and 10.22 distinguish owner transition semantics from structural controls; no frozen-design incompatibility blocks the accepted classifications.

| Plan identity | SHA256 |
| --- | --- |
| Pre-fix `docs/gateflow/pa9-v009-sharing-foundation-plan.md` | `4606a9dad75a7ae914eb3446b4cbbab0c9efc12711758fcd11073ecd4b4cb601` |
| Post-fix `docs/gateflow/pa9-v009-sharing-foundation-plan.md` | `2c66a3f7c855d8996b58633defdb957bc2267812bfa1e8df98446925d18aeb5d` |

The pre-fix bytes matched the supplied frozen hash before editing. Read-only explicit Git metadata resolves HEAD to `835f2ecece1ac8b5c79ab794427424ea380ac052`; the Windows `.git` pointer was not repaired. Baseline plan bytes, full-file SHA256 manifest and tracked diff were retained outside the repository at `/tmp/pa9-v009-planfix1-gpt-20261031-01/`.

## 2. Accepted findings and exact sections changed

| Accepted finding | Exact plan sections changed | Resolution |
| --- | --- | --- |
| A / `F-PA9-PR-01` | §3 inventory row; §7.1 S1 method responsibility; §7.3 catalog/index oracle | Correct physical indexes to 5/4/4=13 and require an independent complete named-index definition oracle, not a total-only assertion. |
| B / Finding 01 | §4.1 structural/service classification; §7.1 S2 method responsibility; §7.4 lifecycle classification and terminal probes; §10 residual table | Same-state EXPIRED/REVOKED generation-changing writes are structurally accepted but service-forbidden, with exact rollback-isolated S2 success witnesses. Exactly-once expiry increment remains an excluded service obligation. |
| B / Finding 02 | §4.1 matrix qualification and mixed-action classification; §7.1 S2 method responsibility; §7.4 lifecycle qualification and mixed probe; §10 residual table | Matrix describes named atomic owner actions, not every mutable SQL shape. Combined structurally valid changes can succeed structurally but are service-forbidden; services must issue one named action/transition per conditional update. |

No other finding was introduced or repaired. Original plan-author provenance/completion text in §12 remains historical; this artifact records the separate two-file fix authorization and task delta.

## 3. Exact index oracle

Expected physical inventory: grant **5**, admission **4**, access event **4**, total **13**. Secondary nonunique indexes remain **2/1/2=5**; no migration object is added, removed or altered.

The S1 `v009SharingInventoryAndGuardShapesAreExact` responsibility and §7.3 oracle now explicitly assert every physical index by **name, table, uniqueness, key-column order and each key's DESC flag**, against the independent frozen §3.3 inventory, never migration/catalog-derived expected definitions. Existing btree/validity/ownership/constraint-relationship requirements remain binding.

| Table in `sharing` | Named physical indexes (unchanged) |
| --- | --- |
| evidence_share_grant | `pk_evidence_share_grant`, `uq_share_grant_share_id`, `uq_share_grant_token_hash`, `ix_share_grant_evidence`, `ix_share_grant_expiry` |
| share_admission | `pk_share_admission`, `uq_share_admission_key`, `uq_share_admission_no`, `ix_share_admission_grant_time` |
| share_access_event | `pk_share_access_event`, `uq_share_access_event_domain_id`, `ix_share_access_event_grant`, `ix_share_access_event_admission` |

Only the `occurred_at` key of `ix_share_access_event_grant(share_grant_id, occurred_at DESC)` is DESC; other keys retain their §3.3 order and ascending flags. PK/UNIQUE backing indexes are unique; the five secondary indexes are nonunique.

## 4. Exact S2 classifications, probes and rollback evidence

All three probes belong inside the existing planned `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops` method. They add no test method or slice. SQL below is a plan witness specification, not implemented or executed code; `:id`, `:terminal_status` and `:old_revision` denote bound fixture values.

### Same-state terminal generation increases — B / Finding 01

For each separately committed valid method-local terminal fixture, established through its named transition, run independently with `:terminal_status = 'EXPIRED'` and then `'REVOKED'`:

```sql
UPDATE sharing.evidence_share_grant
SET revocation_generation = revocation_generation + 1,
    revision = revision + 1
WHERE id = :id
  AND status = :terminal_status
  AND revision = :old_revision;
```

Expected: structural success, exactly one affected row, terminal status unchanged, revocation generation +1, revision +1, every other column unchanged. No SQLSTATE refusal is expected. Both EXPIRED→EXPIRED and REVOKED→REVOKED shapes are **service-forbidden**, not authorized owner actions or valid expiry/revoke retries. The frozen guard rejects decreases and terminal→different-status transitions, not same-state terminal increases. Expiry's generation increment exactly once remains an excluded owner/service transition and deterministic replay obligation; do not invent a blanket terminal-row prohibition.

### Mixed lock+extension — B / Finding 02

Use a separately committed valid method-local ACTIVE fixture with synthetic old expiry safely in the future:

```sql
UPDATE sharing.evidence_share_grant
SET status = 'LOCKED',
    expires_at = expires_at + interval '1 hour',
    revision = revision + 1
WHERE id = :id
  AND status = 'ACTIVE'
  AND revision = :old_revision
  AND clock_timestamp() < expires_at;
```

Expected: structural success, exactly one affected row, ACTIVE→LOCKED, strict expiry increase by the synthetic one-hour delta, revision +1, unchanged generations/count and all other columns. This single combined action is **service-forbidden**, even though the individual structural predicates permit it. One hour is a synthetic test delta, not a production TTL/expiry policy. Services must issue one named action/transition per conditional update. The lifecycle matrix does not exhaust all mutable SQL shapes, and no mixed-action DDL ban is supplied or added.

### Required rollback evidence for each probe

Capture the full baseline row and admission/event counts. BEGIN under `sharing_role`, lock/read the fixture, SAVEPOINT, execute exactly one bounded UPDATE and assert the expected row/deltas. Keep all frozen guards enabled. ROLLBACK TO SAVEPOINT and assert the full baseline row/history restored; then ROLLBACK the transaction. A fresh connection must see the original status, expiry, generations, revision, count and all other columns, plus unchanged admission/event counts. No probe mutation is committed; committed fixtures remain subject to existing method-local cleanup.

These are required later S2 runtime assertions, **not observed runtime rollback results**. No PostgreSQL or test execution occurred in this fix gate. Structural acceptance is not proof of owner authorization, event discipline, idempotency or exactly-once service execution.

## 5. Unchanged boundaries and residual ownership

DDL objects, columns, checks, grants and slices remain unchanged: exactly three tables, 38 columns, 31 named checks, three PKs, five UNIQUE constraints, six ordinary immediate RESTRICT FKs, nine defaults, five secondary indexes, three owner-local trigger helpers and four triggers. The physical-index correction is inventory arithmetic only. No trigger/check predicate was strengthened, no new prohibition added, and no application code was written. Exact ACLs and the frozen five-function baseline remain binding.

The three-table scope (`evidence_share_grant`, `share_admission`, `share_access_event`), all §1.1 non-goals, all implementation file boundaries and all three slices remain binding. V001–V008 bytes are unchanged. Seven planned added methods plus one rename, Failsafe 57→60→62→64 and ten Surefire tests remain unchanged; no tests, CI or README were edited.

No blocking open question remains for these accepted fixes. Residual risks are retained rather than solved by unauthorized schema/service work:

| Residual risk / open input | Owner / destination |
| --- | --- |
| Same-state EXPIRED/REVOKED generation inflation structurally succeeds; expiry exactly-once is not DB-enforced | Sharing/Backend; excluded owner transition/replay service, expiry persistence and deterministic action replay; S2 classification/rollback evidence only |
| Mixed-action single UPDATE structurally succeeds; one named action per conditional update is not DB-enforced | Sharing/Backend; excluded owner transition service and its action discipline; S2 mixed-action witness, no new DDL ban |
| CHECK UNKNOWN, presence-only JSON and independent grant/admission FKs do not prove typed semantics or pair consistency | Sharing/Backend/Security; later sharing-service validation and Q26 mapping |
| Grant locks, verified generations/input, expected-revision WHERE and count/admission/event atomicity require caller discipline | Sharing/Backend; excluded same-connection admission/transition services |
| SOLIDIFIED projection, fingerprints, replay ordering, current ownership/risk/member access | Evidence/Sharing/Security; later owner collaborators/services and access-policy design |
| Q12 KDF/SMS/limit/expiry/rate inputs and Q22 ticket/skew/revocation policy absent | Security/Product/Sharing; policy approval and later service/ticket work |
| Retention/erasure, media/object lock/KMS and Q3/Q11/Q17/Q23 deployment identity | Legal/Security/Infrastructure/Platform/Database; policy, storage and deployment gates |
| Exact PG16 deparse/provisioning prerequisites and cumulative source=CI=XML/cleanup evidence remain unexecuted | Backend/Database/QA and Implementer/Backend QA; separately authorized focused/full implementation validation |
| PA8 nonunit-revision negative witness remains deferred separately | Evidence/Backend QA; post-PA8 test hardening, not a V008 change |
| Pre-existing controller/private files and Windows Git indirection | Controller; preserve intake bytes and use read-only explicit metadata, no repository repair |
| Runtime, remote CI and production acceptance remain unproven | Controller/QA; later implementation acceptance/CI and production gates |

## 6. Changed paths and validation

Exactly two task-owned changed paths:

1. Modified `docs/gateflow/pa9-v009-sharing-foundation-plan.md` (already untracked at intake).
2. Created `docs/gateflow/pa9-v009-sharing-foundation-plan-fix-1.md` (the sole new repository artifact).

Read-only Git commands use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`; no Git configuration or metadata is written. `git diff --check` result: **exit 0, no whitespace errors**. Because both task files are untracked, also check each whole file with `git -c core.autocrlf=false diff --no-index --check /dev/null <path>` and the plan against saved pre-fix bytes: exit 1 denotes differing files under no-index, with no whitespace-error diagnostics (exit 2 would indicate whitespace errors). The command-local setting suppresses checkout-conversion warnings without changing repository config or bytes.

Final validation compares the complete repository-file SHA256 manifest to intake: only the plan changes, only this fix artifact is added, and no path is deleted. The tracked diff remains byte-identical to intake, preserving the controller's unrelated dirty state; `.codex` and both old reviews are preserved. Final plan hash is checked against §1. No Maven, Docker, PostgreSQL, network call, mutating Git command, staging, commit, push, PR/issue operation, approval, ready action or external comment was performed. Stop after this two-file delta validation.
