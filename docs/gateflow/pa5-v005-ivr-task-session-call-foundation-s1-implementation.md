# PA5 V005 S1 Implementation Completion Report

- Task label: `pa5-v005-s1-impl-mimo26flash-20261028-01`
- Runtime / provider / model: Linux WSL2; Claude Code harness; MiMo-V2.6-Flash (implementation author)
- Author role: S1 implementation author only (not plan author, controller, reviewer, or approver)
- Completion timestamp: `20261028-173410` (system clock via `date +%Y%m%d-%H%M%S`)
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Branch / base: `codex/pa5-v005-ivr-task-session-call-foundation` / HEAD `fcc7c89eba9192cf3ebf8daf048d69548b79b654` (accepted plan)
- Preserved Git env: explicit `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call4 --work-tree=<workspace>`
- Binding plan: `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md`
- Binding option B (sole authorized contract change): `ivr.call_record.created_at timestamptz NOT NULL DEFAULT now()` after `end_reason` before `revision`; literal `ix_call_record_subject_time (subject_id, ended_at, created_at)`

## 1. Scope and non-goals

S1-only: entire V005 migration + inventory/ACL evidence. No S2 behavior/concurrency methods. No commit/stage/push/PR/merge/approve. No Gateflow, no nested agents. No V006+ objects, `issue_first_trial`, V007 consent FK, application service (including `ivr.select_call_action` / `ivr.record_activation_next_action`), API/Gateway/JPA, runtime DataSource/Flyway wiring, orchestration/reservation/activation/evidence/media/sharing/payment/cloud/UI.

## 2. Changed paths (exactly five allowed writes)

| Path | Action | SHA-256 |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | new (654 lines) | `de7bc4b354d282ec70ed25d0937686c42de15accb32eb55de33355468d348a77` |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified | `1ad5e4e83bf7dccac11016d9220403910af11ecaa89c4341d17c308ef5fe68e7` |
| `.github/workflows/ci.yml` | modified (expected set/labels only) | `781e1f4db11925bd225c75d7dfa711dfd381eb619c56c38710f208224366d135` |
| `backend/README.md` | modified (S1 evidence only) | `e06f6333b532ecf8f4b653a5ed08dea76d73b73b27218a5faf7f5c5d66bd25e4` |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s1-implementation.md` | new (this report) | written at completion |

Preexisting controller-owned / runner files preserved and not edited by author: `docs/gateflow/project-execution-control.md`, `.pa5-s1-launch.sh`, `.pa5-s1-prompt.md`. No edits to plan/design/history, V001-V004, `pom.xml`, or application Java.

## 3. V005 contract implemented (exact frozen counts)

| Quantity | Value |
| --- | --- |
| Tables | 6 (`outbound_call_task`, `inbound_session`, `call_record`, `call_leg`, `call_event`, `ivr_decision`) |
| Columns | 23+17+23+11+14+15 = **103** |
| `call_record.created_at` | `timestamptz NOT NULL DEFAULT now()` after `end_reason`, before `revision` |
| Named checks | 73 (16+14+15+7+10+11) |
| PKs | 6 |
| Ordinary UNIQUE constraints | 8 |
| Partial unique indexes | 6 |
| Unique keys | 14 |
| Total indexes (`pg_indexes`) | 31 |
| Immediate RESTRICT FKs | 15 |
| NO_FK root `call_id` columns | 2 (`inbound_session.call_id`, `call_record.call_id`) |
| V006-deferred reservation FK | 1 absent (`reservation_id` nullable, no FK; `fk_ivr_decision_reservation` not created) |
| Trigger functions / triggers | 5 / 5 (`SECURITY INVOKER`, `SET search_path = ivr, pg_temp`) |
| Defaulted columns | 17 |
| Id defaults / sequences | 0 / 0 |
| New roles / schemas | 0 |
| Frozen three-column index | `CREATE INDEX ix_call_record_subject_time ON ivr.call_record (subject_id, ended_at, created_at)` |

DDL order follows plan §4.1 (18 steps): owner CREATE TABLE with inline PK/UNIQUE/CHECK → partial/ordinary indexes → functions → triggers → REVOKE PUBLIC → schema USAGE → owned column/table grants → EXECUTE on four guards → RESET ROLE → cross-module grants → 15 FK ALTERs (14 block + `fk_outbound_task_claimed_call` last).

## 4. ACL and immutability (option B)

- Runtime (`ivr_role`): schema USAGE without CREATE; table SELECT/INSERT on all six; exact column UPDATE allowlists; cross-module column SELECT only on customer_account / organization / membership / business_subject / caller_number_binding / subject_risk_state; EXECUTE on four guards only; `reject_row_mutation` owner-only.
- PUBLIC: zero ACL on ivr schema/tables/functions/columns and V005-touched S1 tables.
- `ivr_role catalog_order USAGE` remains **FALSE** (comment-only label correction applied in tests; no catalog_order USAGE grant).
- Runtime cannot `UPDATE call_record.created_at` (column not in UPDATE grant → `42501`). Owner-level immutability of `created_at` / `subject_id` / `call_event` payload/identity is **service-owned residual**; no new guard/check/grant invented (option B forbids it).
- `started_at` remains in UPDATE grant while frozen `guard_call_record_parties` rejects change — both implemented unchanged (F-PA5-PLAN-04 class).
- Reciprocal claim: `outbound_call_task.claimed_call_id` UPDATE requires NULL→existing `call_record` with that `call_id`; `call_record.task_id` UPDATE requires NULL→existing task whose `claimed_call_id = NEW.call_id`.

## 5. Test / CI changes

- Migration method renamed only: `v001ThroughV004MigrateCleanPostgres16WithoutSkipping` → `v001ThroughV005MigrateCleanPostgres16WithoutSkipping` (`migrationsExecuted=5`, V005 history row asserted).
- Added exactly two S1 methods:
  - `v005IvrTaskSessionCallInventoryAndGuardShapesAreExact`
  - `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact`
- No S2 methods added.
- Global function/trigger inventories, search_path scan, risk-protocol no-IVR block, V004 schema-usage/grant universe, and absence helper evolved to V005 boundary (`assertNoV006PlusObjects`).
- Runtime smoke `exerciseV005IvrInsertsAsRuntimeRoleAndRollBack` inserts task/session/call/leg/event/decision with method-local parents then rolls back.
- CI expected set: exactly **33** names; labels/error text V005; Docker-first flow, boot smoke, fat-jar firewall, contract jobs, permissions, filters unchanged.
- Total `@Test` methods: **33**.

## 6. Commands executed (author environment)

| Command | Exit / result |
| --- | --- |
| `mvn -B -ntp validate` (Maven 3.9.9 + Temurin 21.0.13+9 at `/opt`) | **0** — Enforcer 5/5 pass |
| `mvn -B -ntp clean verify` | **0** — Surefire **10** run, 0 fail/err/skip |
| `docker info` | **0** — Docker 29.1.3 |
| `mvn -B -ntp -Pcontainer-smoke verify` | **0** — Failsafe **33** run, **0** failure, **0** error, **0** skip |
| Source/CI/XML method-set equality | **33 == 33 == 33**, all equal |
| V001-V004 Git-blob SHA-256 (`git cat-file`/`show` via explicit `--git-dir`) | unchanged vs plan §2.2 |
| V005 SHA-256 | `de7bc4b354d282ec70ed25d0937686c42de15accb32eb55de33355468d348a77` |
| Boot smoke (`Started OneCallApplication`, timeout 60s) | exit 0, log match |
| Fat-jar firewall | pass — no spring-security/flyway/jooq/HikariCP/postgresql/testcontainers in BOOT-INF/lib |
| docs/14 §13.2.1 S2 awk | exit 0, no output |
| docs/14 §13.2.1 catalog-vs-dictionary | 23 `exact` lines, counts `…,23,17,23,11,14,15,…`, **TOTAL=308**, `ALL_EXACT`, `ivr.call_record exact 23` |
| Diff-scope check | only the five allowed paths (+ preexisting controller/runner files); V001-V004/pom/application untouched |

### 6.1 V001-V004 HEAD blob SHA-256 (recomputed)

| File | SHA-256 |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `V002__identity_organization_and_employee.sql` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `V003__subject_number_and_claim.sql` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |

### 6.2 Note on `./mvnw`

`backend/mvnw` is CRLF and cannot execute under this WSL shell (`bad interpreter`). Author ran the already-distributed Maven **3.9.9** from `/opt/maven-3.9.9` with `/opt/jdk21` (Temurin 21.0.13+9), matching CI pins. Wrapper distribution SHA was not re-downloaded. CI continues to use `./mvnw` on a CRLF-capable runner.

## 7. Residual risks (classified)

| Risk | Class | Owner |
| --- | --- | --- |
| 103-column / 17-default / three-column-index contract now has live PG 16.15 container evidence (S1 revalidated); plan §12 102-column probe remains historical/superseded only | closed for S1 inventory | fresh independent review |
| PG16.15 `pg_get_constraintdef` literals in `V005_CHECK_DEF_MAP` were aligned by container-smoke iteration (BETWEEN→AND grouping, flat AND/OR chains, boolean `= true`/`= false`, jsonb `?`/`->` casts); they match PG16.15 exactly as asserted | closed for S1 | none |
| `call_record.created_at` owner-level immutability is service-owned; runtime INSERT-only via column grant; no guard | service-owned residual | IVR service wave |
| `call_record.subject_id` and `call_event` payload/identity owner-level immutability remain service-owned | service-owned residual | IVR service wave |
| `started_at` UPDATE-grant vs guard inconsistency (F-PA5-PLAN-04) | frozen inconsistency | S1/S2 artifact risk note; no PA5 redesign |
| F-PA5-PLAN-03 comment-only ACL label | documentation | S1 test comment already labels §10.12 |
| Claim+reservation atomicity, expiry housekeeping, `CALL_IN_PROGRESS`, `UNKNOWN` reconciliation, event absorption, selection services | deferred | later IVR/entitlement waves |
| Q7/Q8/Q10/Q15/Q16/Q18/Q24-Q28; Q17/Q23 | open questions | named later owners; not guessed in PA5 |
| Closure is author-reported; controller intake + independent A/B review pending | process | controller |

## 8. Explicit non-claims

No false executable evidence. No Maven/Docker result beyond those listed above. No production acceptance, no A5 pass, no service-owned immutability claim, no V006+ object, no `issue_first_trial`, no reservation FK. Local success ≠ CI green on GitHub Actions and ≠ accepted checkpoint until controller runs independent review.

## 9. Conclusion

S1 implementation is complete inside the five-path write boundary. V005 creates the six frozen `ivr` tables with option-B `call_record.created_at` and the literal three-column `ix_call_record_subject_time`, full 103/73/14/31/15/5/5/17 inventory, fail-closed ACL, and reciprocal claim guards. Container smoke is 33/33 green on PostgreSQL 16.15. Ready for controller intake and independent A/B review.

---

## S1 fix-1 addendum (2026-10-28, appended after historical original)

The original S1 result above is preserved verbatim. After S1 completion, independent code review `docs/reviews/code-review-20261028-174721.md` accepted three findings against the frozen V005 contract; the user replied 同意 to bounded frozen-contract errata. A separate task `pa5-v005-s1-fix1-mimo26flash-20261028-180623` authored the bounded fix. Full evidence, changed paths, predicate decisions, validation commands and residual risks are recorded in the dedicated artifact `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s1-fix-1.md`.

### S1 fix-1 finding status

| Finding | Title | Final author status |
| --- | --- | --- |
| F-PA5-S1-01 | `ck_ivr_decision_source` / `ck_ivr_decision_shape` allow NULL source identity / NULL refusal action | **已修复** |
| F-PA5-S1-02 | `ck_call_event_payload_redacted` does not pin payload object type or receipt hash string type | **已修复** |
| F-PA5-S1-03 | `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact` catalog-only, no executed SET LOCAL ROLE denial probes | **已修复** |

### S1 fix-1 outcome (summary; details in the fix-1 artifact)

- V005 three named CHECK definitions amended only (`ck_ivr_decision_source`, `ck_ivr_decision_shape`, `ck_call_event_payload_redacted`); 73-count and all other predicates/keys/indexes unchanged.
- `PostgresContainerIT`: deparse map synchronized for the three predicates; F01/F02 regression probes and F03 runtime 42501 denial probes wired into the two existing S1 methods via new private helpers; **no new `@Test`**, total remains **33**.
- docs/14 §§4.27/4.28 named predicates and errata rationale updated; plan §4.3/§6.2/§9 synchronized.
- Re-validation on the post-fix tree: `mvn -B -ntp validate` exit 0; `clean verify` 10/0/0/0; `docker info` OK; `-Pcontainer-smoke verify` **33/0/0/0**; source/CI/XML method sets equal 33; boot smoke `Started OneCallApplication`; fatjar firewall pass; docs/14 §13.2.1 S2 checks exit 0 with six IVR dictionaries exact (23/17/23/11/14/15 = 103); V001-V004 blob SHA-256 unchanged vs plan §2.2; `git diff --check` exit 0.
- Closure is author-reported; controller re-review is still required before any independent acceptance claim.
