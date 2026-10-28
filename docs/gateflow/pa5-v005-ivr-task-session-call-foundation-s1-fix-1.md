# PA5 V005 S1 fix-1 Completion Artifact

- Work unit / gate / scope: **PA5-v005-ivr-task-session-call-foundation**, gate **S1 fix-1** (not S2). Bounded frozen-contract errata for the two CHECK defects and the ACL denial-test execution gap accepted in `docs/reviews/code-review-20261028-174721.md` (F-PA5-S1-01/02/03). This artifact is not a new work unit or slice; the two approved S1 behavioral slices remain minimal and unchanged.
- Task label: `pa5-v005-s1-fix1-mimo26flash-20261028-180623`
- Runtime / provider / model: Linux WSL2; external runner; MiMo-V2.6-Flash (implementation/fix author)
- Author role: implementation/fix author only (not plan author, controller, reviewer, or approver)
- Completion timestamp: `20261028-184000` (system clock)
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Branch / base: worktree of `codex/pa5-v005-ivr-task-session-call-foundation`; accepted plan HEAD `fcc7c89eba9192cf3ebf8daf048d69548b79b654`; project base `4006e8e09c39e4527cb543f03646a30073d1e1fa`
- Preserved Git env: explicit `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call4 --work-tree=/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Binding plan: `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md`
- Binding review: `docs/reviews/code-review-20261028-174721.md` (accepted findings, all previously 未修复)
- Historical supporting review (evidence only, not re-opened): `docs/reviews/code-review-20261028-174722.md`
- Run directory (private logs): `/tmp/sub-agents.pa5-s1fix1.zPeUJD/`
- Private build copy: `/tmp/pa5-s1fix1-build.f2ddtX/backend`

## 1. Authorization

User explicitly replied **同意** to bounded frozen-contract errata limited to:

1. the two CHECK defects (F-PA5-S1-01, F-PA5-S1-02), and
2. ACL denial tests that must actually execute `SET LOCAL ROLE ivr_role` probes (F-PA5-S1-03).

No optional hardening, unrelated open question, service behavior, V006+ object, count change or ownership-file expansion is authorized or performed.

## 2. Accepted findings and final author status

| Finding | Summary | Final author status |
| --- | --- | --- |
| F-PA5-S1-01 | `ck_ivr_decision_source` allowed event-based `ACTIVATION_NEXT_ACTION`/`DIRECT_AUTHORIZATION` with NULL source identity; `ck_ivr_decision_shape` allowed `DIRECT_AUTHORIZATION` refusal with NULL `action`. | **已修复** |
| F-PA5-S1-02 | `ck_call_event_payload_redacted` did not pin `source.payload` to a JSON object or `receipt.payloadSha256` to a JSON string. | **已修复** |
| F-PA5-S1-03 | `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact` asserted catalog ACL rows but never executed undeclared-column UPDATE, DELETE, TRUNCATE, cross-module write/SELECT FOR UPDATE, or `reject_row_mutation` as `ivr_role`. | **已修复** |

## 3. Changed paths (exact allowed writes)

| Path | Action | SHA-256 (LF-normalized) |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | modified — only the three named CHECK definitions | `49f7c7500f2a09de801289ac4e33fe80c3228f076bb69e1874bd778d32683386` |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified — deparse-map sync + probes inside existing methods/helpers; **no new `@Test`** | `bf50e7af250fb008434ec0889693a97fcdb45222b8a3ebab6d9d649ca73ee3f0` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | modified — §§4.27/4.28 named predicates + explicit errata rationale (CRLF preserved) | `4a4db35db8591e5b4c92718eae813803392395d223448a0a44b6cbae83b0aec6` |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md` | modified — §4.3 item 3 errata, §6.2 item 9 runtime probes, §9 risk rows | `3fec86aae66c3ab23697e10fe173828942dfe4873ce22f6f5f17c385734737e0` |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s1-implementation.md` | appended fix-1 addendum; historical original preserved verbatim | see §7 |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s1-fix-1.md` | **new** — this durable artifact | written at completion |
| `backend/README.md` | **not edited** (optional path; no count change needed) | unchanged from pre-fix S1 dirty delivery |

Not edited: controller `docs/gateflow/project-execution-control.md` (pre-existing dirty), CI, prior reviews, V001-V004, POM, wrapper, dependencies, application Java. Controller temporary dotfiles (`.pa5-*`) are not deliverables.

## 4. Predicate decisions (exact)

### F-PA5-S1-01 / `ck_ivr_decision_source`

Event-based branches now explicitly require non-NULL `source_event_domain` AND `source_event_id` before domain equality/nonblank length:

```sql
CONSTRAINT ck_ivr_decision_source
    CHECK ((purpose = 'INBOUND_RESOLVE' AND source_event_domain IS NULL AND source_event_id IS NULL)
        OR (purpose = 'ACTIVATION_NEXT_ACTION' AND source_event_domain IS NOT NULL AND source_event_id IS NOT NULL AND source_event_domain = 'IVR_CONFIRM' AND length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> '')
        OR (purpose = 'DIRECT_AUTHORIZATION' AND source_event_domain IS NOT NULL AND source_event_id IS NOT NULL AND source_event_domain = 'IVR_DIRECT_AUTH' AND length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> '')),
```

`INBOUND_RESOLVE` retains NULL source identity. Domain literals, length bounds and nonblank checks unchanged.

### F-PA5-S1-01 / `ck_ivr_decision_shape`

`DIRECT_AUTHORIZATION` refusal branch now explicitly requires `action IS NOT NULL` and a four-action `IN` (excluding `DIAL_TASK_CALLEE`, which is resolve/activation-only). Direct success stays `action IS NULL` with non-NULL `reservation_id`. All other action/reason combinations and keys/indexes preserved:

```sql
OR (purpose = 'DIRECT_AUTHORIZATION' AND ((action IS NULL AND reservation_id IS NOT NULL) OR (action IS NOT NULL AND action IN ('GUIDE_PURCHASE','REJECT','REJECT_SUSPENDED','REJECT_CALL_IN_PROGRESS') AND reservation_id IS NULL)))
```

### F-PA5-S1-02 / `ck_call_event_payload_redacted`

Two typeof conjuncts added after the respective key-existence checks; existing key-existence, structure, strip-to-empty and hash-equality predicates stay:

```sql
AND (payload_redacted->'source') ? 'payload'
AND jsonb_typeof((payload_redacted->'source')->'payload') = 'object'
AND (payload_redacted->'receipt') ? 'payloadSha256'
AND jsonb_typeof((payload_redacted->'receipt')->'payloadSha256') = 'string'
```

No additional `eventFingerprint` semantics, canonical hashing, service behavior, new guards or constraints.

### F-PA5-S1-03 / runtime denial probes

New private helper `assertV005RuntimeDenialsAndLegalUpdateControl()` (wired from existing `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact`, no new `@Test`) executes inside a fixture transaction that is fully rolled back:

- Legal column `UPDATE ivr.call_leg SET state = 'ANSWERED', revision = revision + 1` as `ivr_role` → positive control, 1 row.
- Undeclared column `UPDATE ivr.call_leg SET leg_type = 'CALLEE'` → exact `42501`.
- Undeclared column `UPDATE ivr.outbound_call_task SET caller_number_hmac = ?` → `42501`.
- Append-only `UPDATE ivr.ivr_decision SET reason_code = reason_code` → `42501`.
- `DELETE FROM ivr.call_event WHERE id = ?` → `42501`.
- `TRUNCATE ivr.call_leg` → `42501`.
- Cross-module `UPDATE subject_number.business_subject SET revision = revision + 1` → `42501`.
- Cross-module `DELETE FROM customer_iam.customer_account` → `42501`.
- Cross-module `SELECT ... FROM customer_iam.customer_account / subject_number.business_subject ... FOR UPDATE` → `42501` (two probes).
- Owner-only `SELECT ivr.reject_row_mutation()` as `ivr_role` → `42501` (EXECUTE privilege checked before trigger-only restriction).

Each failure uses bounded savepoint rollback (`expectSqlFailure` / `expectSqlQueryFailure`); the whole fixture transaction rolls back in `finally`. Exact catalog ACL assertions are not weakened.

### F01/F02 regression probes

New private helper `assertV005ErrataCheckRegressionProbes()` (wired from existing `v005IvrTaskSessionCallInventoryAndGuardShapesAreExact`, no new `@Test`) uses method-local parents and savepoint-isolated positives inside one rolled-back transaction:

- Negatives with exact SQLSTATE `23514` **and** named constraint:
  - DIRECT success with both `action` NULL and `reservation_id` NULL → `ck_ivr_decision_shape`
  - ACTIVATION with NULL `source_event_id` → `ck_ivr_decision_source`
  - ACTIVATION with NULL domain+id → `ck_ivr_decision_source`
  - DIRECT refusal with NULL `source_event_id` → `ck_ivr_decision_source`
  - DIRECT refusal with NULL domain+id → `ck_ivr_decision_source`
  - payload JSON null / payload string → `ck_call_event_payload_redacted` (nonobject separated from null hash)
  - receipt `payloadSha256` JSON null → `ck_call_event_payload_redacted`
- Positives (savepoint-isolated, each proving one legal shape):
  - DIRECT success with non-NULL `reservation_id` (legal UUID; V006 reservation FK absent)
  - DIRECT refusal with four-action and NULL reservation
  - INBOUND_RESOLVE with NULL source identity
  - ACTIVATION with `IVR_CONFIRM` domain + nonblank id
  - call_event with object payload `{}`, matching `payloadSha256` string and `eventFingerprint`

Each negative violates exactly one named constraint so the reported constraint name is deterministic. No committed residue. S2 broad 73-check / 15-FK / concurrency probes stay deferred.

### Deparse map

Three `V005_CHECK_DEF_MAP` entries synchronized to PG16.15 `pg_get_constraintdef` deparse (typeof conjuncts, `IS NOT NULL` conjuncts, refusal `action = ANY (ARRAY[...])`), verified green by container smoke.

## 5. Preserved inventory (unchanged)

6 tables / 103 columns (23/17/23/11/14/15) / 73 CHECKs / 6 PK / 8 ordinary UNIQUE / 6 partial unique / 14 unique keys / 31 indexes / 15 ordinary immediate RESTRICT FK / 5 functions / 5 triggers / 17 defaults / 0 id defaults / 0 sequences / 0 new roles or schemas. Option-B `call_record.created_at` + three-column `ix_call_record_subject_time` unchanged. Two root `call_id` NO_FK; V006 reservation FK absent. Approved `started_at` grant/guard inconsistency and owner/service immutability residual preserved; no invented guards/grants. V001-V004 Git blobs immutable.

## 6. Validation (commands, results, private logs)

Toolchain: JDK 21 (`/opt/jdk21`), Maven 3.9.9 (`/opt/maven-3.9.9`). Backend copied without `target` to a new mktemp private directory; no Maven on the shared original `backend/target`. CRLF `mvnw` wrapper untouched.

| Command | Result | Log |
| --- | --- | --- |
| `mvn -B -ntp validate` | exit 0 (Enforcer 5/5) | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/validate.log` |
| `mvn -B -ntp clean verify` | exit 0 — Surefire **10** / 0 / 0 / 0 | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/clean-verify.log` |
| `docker info` | OK — Docker 29.1.3 | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/docker-info.log` |
| `mvn -B -ntp -Pcontainer-smoke verify` | exit 0 — Failsafe **33** / 0 / 0 / 0 | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/container-smoke.log` |
| Source / CI / Failsafe-XML method-set equality | **33 == 33 == 33**, all equal | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/{xml,src,ci}-methods.txt` |
| Boot smoke (`/opt/jdk21/bin/java -jar … --server.port=0`) | exit 0; `Started OneCallApplication in 0.736 seconds` | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/boot-smoke.log` |
| Fatjar firewall (`BOOT-INF/lib` scan) | pass — no `spring-security`/`flyway`/`jooq`/`HikariCP`/`postgresql`/`testcontainers` (27 entries) | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/boot-inf-lib.txt` |
| docs/14 §13.2.1 S2 awk (LF-normalized temp copy) | exit 0, no output | — |
| docs/14 §13.2.1 catalog-vs-dictionary | 23 `exact` lines including six IVR at 23/17/23/11/14/15 (**IVR6_TOTAL=103**), `ALL_EXACT` | — |
| V001-V004 blob SHA-256 vs plan §2.2 | all four match; `git status` clean on each | — |
| `git diff --check` | exit 0 (only expected LF→CRLF index warnings) | `/tmp/sub-agents.pa5-s1fix1.zPeUJD/git-diff-check.log` |

V001-V004 recomputed SHA-256 (LF):

| File | SHA-256 | Match |
| --- | --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` | yes |
| `V002__identity_organization_and_employee.sql` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` | yes |
| `V003__subject_number_and_claim.sql` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` | yes |
| `V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` | yes |

Diff scope vs HEAD: modified `ci.yml`, `backend/README.md`, `PostgresContainerIT.java`, `docs/14`, plan, controller control doc; untracked V005 SQL, s1-implementation doc, review files, controller dotfiles. Author-touched files this gate are exactly the allowed paths in §3. No production resources used.

## 7. docs decision

- docs/14 §§4.27/4.28: named predicates updated to the executable SQL; explicit errata paragraphs state the F-PA5-S1-01/02 rationale, reference unchanged §10.14 semantics, and note the 73-count is unchanged. CRLF line endings preserved; no source-format churn.
- Plan: §4.3 lead sentence and items document the one transcription correction plus two user-authorized schema errata (user 同意 2026-10-28); §6.2 item 9 records the S1 fix-1 runtime probes and regression-probe requirement; §9 risk table gains `F-PA5-S1-01/02/03` rows closed by this artifact. Two approved behavioral slices retained; no future slice implemented.
- s1-implementation: historical original result preserved verbatim; fix-1 addendum appended with finding status and re-validation summary, pointing to this artifact for full evidence.
- backend/README.md: left unchanged (optional path; no count change required by the fix).

## 8. Residual risks (owner / destination)

| Residual | Owner / destination |
| --- | --- |
| S2 broad 73-check / 15-FK / concurrency probes | later approved S2 slice |
| IVR service / orchestration / reservation / activation / evidence behavior | named later IVR/entitlement/evidence waves |
| Production config / Q7-Q28 open questions | named later owners (Platform / product / security); not guessed in this fix |
| Exact-head CI on GitHub Actions | later PR gate (controller/CI runner) |
| Existing npm tooling audit | later Platform security triage (not this fix) |
| Closure is author-reported; independent acceptance not claimed | controller re-review of this artifact + fresh A/B of the post-fix tree |

## 9. Completion status

- All three accepted findings: **已修复**.
- No unresolved blockers inside the bounded scope.
- No claim of independent acceptance before controller re-review.
- No commit, stage, push, PR, merge, approval, external comment or other gate performed.
- Artifact path: `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s1-fix-1.md`
