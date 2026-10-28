# PA5 V005 S2 Implementation Completion Report

- Task label: `pa5-v005-s2-impl-mimo26flash-20261028-185933`
- Runtime / provider / model: Linux WSL2; Claude Code harness; MiMo-V2.6-Flash (implementation author)
- Author role: S2 implementation author only (not plan author, controller, reviewer, or approver)
- Completion timestamp: `20261028-212718` (system clock via `date +%Y%m%d-%H%M%S`)
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Branch / base: `codex/pa5-v005-ivr-task-session-call-foundation` / accepted S1 checkpoint `02cb35494c6fcbaa8027003d83949ec2c30f211b`
- Preserved Git env: explicit `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call4 GIT_WORK_TREE=<workspace>`
- Accepted S1 checkpoint: `02cb35494c6fcbaa8027003d83949ec2c30f211b` (`gateflow: accept PA5-v005-ivr-task-session-call-foundation S1`)
- Frozen V005 SHA-256: `49f7c7500f2a09de801289ac4e33fe80c3228f076bb69e1874bd778d32683386` (re-verified; unchanged)
- Private validation copy: `/tmp/pa5-s2-priv.aSTo2Y/backend` (fresh mktemp; no Maven on shared `backend/target`)
- Toolchain: `JAVA_HOME=/opt/jdk21` (Temurin 21.0.13+9) + `M2_HOME=/opt/maven-3.9.9` (system `mvn`; `./mvnw` is CRLF and fails under WSL)

## 1. Scope and non-goals

S2-only: exactly five new container-IT behavioral/concurrency methods plus bounded helpers, CI expected-set/count/labels 33→38, README S2 evidence lines, and this artifact. No migration edits, no POM/wrapper/application-code changes, no application services, no API/JPA/runtime DataSource/Flyway wiring, no trial/entitlement/reservation/activation/consent/evidence/sharing/cloud/UI objects. No commit/stage/push/PR/merge/self-accept. V001–V005 blobs untouched.

## 2. Changed paths (exactly four allowed writes)

| Path | Action | SHA-256 |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified (five @Test + bounded helpers; 33 prior methods preserved; 15,126 lines) | `f6b1d934287cacead46d2c17d12b2112f90b580905ef3a3b430f9848154b0d0d` |
| `.github/workflows/ci.yml` | modified (Failsafe expected set/labels 33→38 only) | `54d316b269300b5ca91f80353f55ddd7bf446b94cf53921debfe01699b5c2abf` |
| `backend/README.md` | modified (S1–S2 header, five S2 method descriptions, 38 profile lines) | `fa989fb67fb05816ceae700352c30d4083f6eb65601848e886bb55315a5c4449` |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s2-implementation.md` | new (this report) | written at completion |

Preexisting controller-owned / runner files (`docs/gateflow/project-execution-control.md`, `.pa5-s2-*.sh`, `.pa5-s2-prompt.md`) preserved and not edited by author. No edits to V001–V005, `pom.xml`, wrapper, or application Java.

## 3. Five S2 methods (database-visible backstops only)

| Method | Evidence |
| --- | --- |
| `v005TaskTtlLifecycleAndGuardBackstops` | 16 outbound named CHECK isolated `23514` probes (probe-set equality vs `V005_CHECK_DEF_MAP`); PENDING/ttl-30/revision-1 defaults; duplicate idempotency `23505` without TTL change; `OUTBOUND_CALL_TASK_IMMUTABLE`/`TERMINAL`/`CLAIM_INVALID` owner `P0001` with `clock_timestamp()` distinct values; live-caller partial-unique `23505`; conditional state+revision success and stale zero-row; terminal blocks new PENDING; runtime `42501`; method-local rollback |
| `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops` | 14 session + 15 call CHECK probes with probe-set equality; ACTIVE/UNKNOWN caller occupancy (`uq_call_record_active_caller`) and terminal release; immutable-origin owner `P0001` and runtime `42501`; F-PA5-PLAN-04 `started_at` UPDATE allowed by grant but rejected `P0001` `CALL_RECORD_PARTIES_IMMUTABLE` (same-transaction `now()` is a no-op for `IS DISTINCT FROM`, so probes use `clock_timestamp()`); reciprocal claim link order (`claimed_call_id`+`claimed_at` in one UPDATE, then `call_record.task_id`), relink/clear rejected; `uq_outbound_task_claimed_call` `23505`; distinct caller HMACs so occupancy/uniqueness probes are not preempted |
| `v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops` | 7 leg + 10 event + 11 decision CHECK probes with probe-set equality; `CALL_LEG_IDENTITY_IMMUTABLE`; event domain/id and sequence partial uniques `23505`; payload/identity runtime `42501`; legal ENTER_CALLEE / DIAL_TASK_CALLEE + random `reservation_id` (no FK) / direct success / refusal shapes; `uq_ivr_decision_call_purpose` `23505`; wrong DIRECT domain isolated to `ck_ivr_decision_source`; owner `IVR_DECISION_APPEND_ONLY` `P0001` and runtime `42501`; matching redacted/column SHA so CHECK does not preempt unique/FK |
| `v005ForeignKeyAndDeferredReservationFkBackstops` | all 15 missing-parent `23503` probes (probe-set equality vs `V005_FK_DEFS`); representative parent-delete RESTRICT; whole-FK map recheck `containsExactlyInAnyOrderElementsOf(V005_FK_DEFS)`; two NO_FK root `call_id` columns; `reservation_id` nullable with no FK and no `fk_ivr_decision_reservation`; no entitlement/activation tables; distinct probe HMACs and dedicated empty sessions so unique-before-FK indexes do not mask FK SQLSTATE |
| `concurrentV005TaskClaimAndActiveCallHasOneWinner` | six dual-connection races (live-caller task, active-call occupancy, conditional claim, event domain/id, event sequence, decision purpose); distinct `application_name`, `statement_timeout`, `pg_stat_activity` wait-event blocking probes, future timeout; unique losers observe exact `23505`+constraint name; conditional-claim loser zero-row; category-2 savepoint clears both reciprocal links while pre-savepoint call survives; category-3 whole rollback leaves no claim/link; finally migration-identity single `TRUNCATE` of six ivr tables plus FK-safe V003/V002 parent deletion with zero-row assertions |

### 3.1 Subsumed / precedence notes (frozen catalog, no migration change)

- PG 16 CHECK precedence observed throughout: CHECK (`23514`) before unique (`23505`) before FK (`23503`); partial unique indexes fire before FK when the live-caller/active-caller predicate matches.
- `ck_call_leg_type` is logically subsumed by `ck_call_leg_direction` (any `leg_type` outside CALLER/CALLEE also fails the direction pairing), so PG reports only the direction constraint on a bare BOGUS insert. Following the established in-repo isolation pattern (`expectProfileStatusViolationWithIsolatedConstraint`, `expectCatalogVersionOfferTypeViolationWithIsolatedConstraint`), the new bounded helper `expectCallLegTypeViolationWithIsolatedConstraint` drops `ck_call_leg_direction` inside a savepoint as `ivr_owner`, proves exact `23514` + `ck_call_leg_type`, rolls back to the savepoint, asserts both constraints are restored in `pg_constraint`, and returns to `ivr_role`. Probe-set equality still requires the full 7-name `call_leg` set.
- `payload_redacted` CHECK requires `receipt.payloadSha256 = payload_sha256` column; concurrent unique races share a single SHA between JSON and column so `23514` cannot preempt lock-wait on the unique index.
- `started_at` UPDATE probes use `clock_timestamp()` because same-transaction `now()` equals the row value and `IS DISTINCT FROM` is false (F-PA5-PLAN-04 guard path is `P0001`, not `42501`).

## 4. CI and README

- `.github/workflows/ci.yml`: expected Failsafe set extended by exactly the five V005 S2 method names; count assertion text `V005 S2 test count (38)`; Docker-first flow, boot smoke, fat-jar firewall, contract jobs, permissions, and path filters unchanged.
- `backend/README.md`: header extended to `PA5 V005 S1–S2`; intro paragraph states the five S2 methods prove database-visible lifecycle/guard/named-check/FK/append-only/bounded-concurrency backstops only (no application service) and container methods total 38; dedicated five-method S2 bullet added; stale “33 test” container-evidence line corrected to 38; V001–V005 evidence-boundary paragraph extended.

## 5. Validation matrix (all on private copy `/tmp/pa5-s2-priv.aSTo2Y`)

| Check | Result |
| --- | --- |
| Enforcer (`mvn validate`) | 5/5 rules pass |
| Default `clean verify` | Surefire **10** run, 0 fail / 0 err / 0 skip |
| `docker info` | Docker daemon available |
| Full `-Pcontainer-smoke verify` | Failsafe **38** run, **0** failure, **0** error, **0** skip |
| source = CI = XML method sets | **38 == 38 == 38**, all equal; XML counts `tests=38 skipped=0 failures=0 errors=0` |
| Full 38 failsafe XML preserved | `/tmp/pa5-s2-xml-full/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` |
| Focused `v005TaskTtlLifecycleAndGuardBackstops` | 1/0/0/0 |
| Focused `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops` | 1/0/0/0 |
| Focused `v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops` | 1/0/0/0 |
| Focused `v005ForeignKeyAndDeferredReservationFkBackstops` | 1/0/0/0 |
| `concurrentV005TaskClaimAndActiveCallHasOneWinner` ×3 independent runs | 1/0/0/0 each |
| Boot jar smoke | `Started OneCallApplication` matched, exit 0 |
| Production fat-jar firewall | no spring-security/flyway/jooq/HikariCP/postgresql/testcontainers in `BOOT-INF/lib` |
| V001–V004 git blob HEAD vs worktree | all four match (unchanged) |
| V005 content SHA-256 | `49f7c7500f2a09de801289ac4e33fe80c3228f076bb69e1874bd778d32683386` (frozen value) |
| `git diff --check` | exit 0 (CRLF-normalization warnings only; no whitespace errors) |
| Write-scope | exactly the four allowed paths above |

### 5.1 V001–V004 git blob hashes (HEAD == worktree)

| File | git blob |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `d7e2e6879701a5db6517434cbb9b22d5c8f2d707` |
| `V002__identity_organization_and_employee.sql` | `562302e19e8d7b84399ed3dff44eab00e411cfae` |
| `V003__subject_number_and_claim.sql` | `4c274b53c7ff75416e16e7ae8dd7d0d9973170b7` |
| `V004__catalog_order_and_payment.sql` | `3197138f7f7b5740f2bd0b6a4cd7b0cbd76b2cf7` |

## 6. Iterative smoke history (this validation cycle)

1. First post-third-round smoke on stale private copy: 3F/0E (sync lag; leg-type name, FK SHA, `now()` no-op).
2. After sync + `ck_call_leg_type` savepoint isolation: 1F/1E — FK recheck SQL missing `||` before `CASE` (syntax `WHEN`); session time-order probe tripped `ck_inbound_session_confirmed_shape` first.
3. After `||` fix + `sessionProbeBadTimeOrder` (legal CONFIRMED shape, `confirmed_at < created_at`): 1E — second missing `||` before `c.condeferrable`.
4. After aligning the FK recheck SQL to the proven S1 `queryStrings` shape: **38/0/0/0 BUILD SUCCESS**.

No stop condition triggered: no migration/frozen-contract/grant/count/owner change was needed; validation was available; scope was unambiguous; no true DB defect was found (all failures were test-probe construction issues against frozen V005).

## 7. Residual risks (classified)

| Risk | Class | Owner |
| --- | --- | --- |
| S2 methods are author-reported green pending independent A/B review and controller intake | process | controller |
| Local/private green ≠ GitHub Actions green ≠ accepted checkpoint | process | later PR/CI gate |
| Claim/reservation orchestration, expiry housekeeping, `CALL_IN_PROGRESS`, `UNKNOWN` reconciliation, event absorption, selection services | deferred | later IVR/entitlement waves |
| `call_record.created_at`/`subject_id`/`call_event` payload/identity owner-level immutability | service-owned residual | IVR service wave |
| `started_at` UPDATE-grant vs guard inconsistency (F-PA5-PLAN-04) | frozen inconsistency | exercised as `P0001` in S2; no PA5 redesign |
| DTMF/ENTER_CALLEE/Q-input legal shapes only (no protocol semantics) | shapes-only | IVR protocol owners |
| Q7/Q8/Q10/Q15/Q16/Q18/Q24–Q28; Q17/Q23 | open questions | named later owners |

## 8. Explicit non-claims

No false executable evidence. No Maven/Docker result beyond those listed above. No production acceptance, no A5 pass, no application-service claim, no V006+ object, no reservation FK, no grant/migration change. Local success ≠ CI green on GitHub Actions and ≠ accepted checkpoint until controller runs independent review. Closure is author-reported; self-acceptance was not performed.
