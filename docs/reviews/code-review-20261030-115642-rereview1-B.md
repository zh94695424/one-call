# PA7 V007 S1 Rereview1 — Independent Code Reviewer B

| Field | Value |
| --- | --- |
| Gateflow work unit | `PA7-v007-activation-consent-foundation` |
| Gate | S1 rereview1 (independent B) |
| Task label | `pa7-s1-rereview1-B-20261030-115642` |
| Reviewer | Independent Code Reviewer B / mimo26 |
| Mode | Current Changes vs selected base `64b576abd4efce956d51d4e57a6b8afeeb13cd45` |
| Branch | `codex/pa7-v007-activation-consent-foundation` |
| Frozen set | `.pa7-s1-review-freeze.json` (7 files, gate `S1 rereview1`) |
| Binding plan | `docs/gateflow/pa7-v007-activation-consent-foundation-plan.md` (SHA256 `cb2fc46f…dab0682a`) |
| Own initial report | `docs/reviews/code-review-20261030-113444-B.md` (only comparison baseline) |
| Accepted fix doc | `docs/gateflow/pa7-v007-activation-consent-foundation-s1-review-fix-1.md` |
| Verdict | **APPROVE — no new material findings.** All three Reviewer-B accepted findings are fixed; accepted A/F2 trigger-tgtype pin is fixed. |

## Scope

Reviewed the **full corrected frozen S1 diff**, not only the fix hunks.

- `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` (312 lines)
- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (21,886 lines; V007 helpers + staged lifecycle + cleanup/legacy callers)
- `.github/workflows/ci.yml`, `backend/README.md`
- `docs/gateflow/pa7-v007-activation-consent-foundation-s1-implementation.md`
- `docs/gateflow/pa7-v007-activation-consent-foundation-s1-validation-fixes.md`
- `docs/gateflow/pa7-v007-activation-consent-foundation-s1-review-fix-1.md`

Contracts: plan §1.3 / §4–§9 / §8.1 S1 acceptance. Binding design sources alongside the plan: docs/12 §5,6,14,15 Phase B and docs/14 §4.36–4.38,5,9,10.12,10.16–10.20,11.1,11.4.

S1 scope is exactly V007 persistence: **3 tables / 43 columns / 31 checks / 3 PK / 5 unique / 12 indexes / 11 FK / 6 defaults / 2 trigger functions / 3 triggers / 48 tests**. S2 lifecycle/concurrency methods, API/service/frontend, and V008+ remain out of scope.

Excluded: peer initial/rereview reports (not read); shared `backend/target` treated read-only; no Maven/Java/Docker execution by this reviewer.

Parallel review coverage: 无.

## Frozen hash verification (before and after)

| Path | Freeze SHA-256 | Before | After |
| --- | --- | --- | --- |
| `V007__activation_and_consent.sql` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` | match | match |
| `PostgresContainerIT.java` | `b568fc4261b8ac37d359835a3533b418e51ad6f4e216da2e06a1eecf92ca0412` | match | match |
| `.github/workflows/ci.yml` | `f34379ce1741ac8d19bf3f047894365fb1e611a3b5d92caa8229472a272e4480` | match | match |
| `backend/README.md` | `0756cda807b522b5fab31d6f27695332f6c80f42b8441b967a44ac7678712832` | match | match |
| `s1-implementation.md` | `f497230f4ade5490a8b66189b34ea5949efcbb66bf6e978925b7a37eebe9e75a` | match | match |
| `s1-validation-fixes.md` | `6d4ee285db2917c1dc74992adc7b000c317dff3302c0117a1e30b0f93418bcf4` | match | match |
| `s1-review-fix-1.md` | `268e8c3e2e4d934f5e0639f6d088b6110f5e270f59e13430d28e057f3ea5bc1f` | match | match |

Base `64b576abd4efce956d51d4e57a6b8afeeb13cd45` accepted. No frozen file was modified by this review. Post-fix test hash `b568fc42…a0412` is the frozen rereview identity (pre-fix `79717037…b16378aa` was the initial-review identity).

## Findings

All three Reviewer-B initial findings were controller-accepted and landed in the sole modified path `PostgresContainerIT.java`. Each is re-walked below in the fixed finding format and declared **fixed**. Accepted A/F2 (trigger event set) is also verified fixed from the same fix batch. **No new material findings.**

### F-PA7-S1-B-01 — 已修复-中-executable row-lock denials (`FOR UPDATE` + `FOR SHARE`)

- **入口/函数**: `PostgresContainerIT.assertV007RuntimeDenialsAndLegalUpdateControl` (method starts ≈8633)
- **文件(行号)**: `PostgresContainerIT.java` 8666–8745 (probes); helper `expectSqlQueryFailure` 3083–3104
- **输入场景**: Plan §8.1 requires zero foreign writes/**row locks**/CREATE for V007 runtime and foreign roles. Initial report showed only `INSERT`/`CREATE` denials and ACL-metadata proof, no live lock probes.
- **实际分支**: Corrected code now executes savepoint-isolated `expectSqlQueryFailure(..., "42501", ...)` for both `FOR UPDATE` and `FOR SHARE`.
- **预期行为**: Every foreign row-lock path named by the contract fails closed with SQLSTATE `42501`.
- **实际行为**:
  - `activation_consent_role`: `FOR UPDATE`/`FOR SHARE` on `subject_number.business_subject(id)`, `customer_iam.customer_account(id)`, `organization_access.organization(id)`, `organization_access.organization_membership(organization_id)` (permitted columns only; random local IDs, zero rows).
  - `ivr_role`: `service_activation(id)` `FOR UPDATE`/`FOR SHARE` on the existing activation row; `consent_record(subject_id)` (ivr lacks `id`) on the existing subject row.
  - `catalog_order_role`: `service_activation(id)` on the existing activation row; `consent_record(id)` with random ID.
  - Legal head `UPDATE`, prior runtime `42501`/owner `P0001` controls, and `CREATE` denial preserved.
- **直接证据**: Fix doc §Accepted fix 1; source 8666–8745 matches V005/V006 `expectSqlQueryFailure` precedent (savepoint rollback in `finally`). Initial open questions 1–2 (live probes vs metadata; `FOR SHARE` inclusion) are closed by the landed probes covering both lock modes.
- **影响**: Required S1 executable proof is now present. Residual risk from the initial report (unnoticed privilege edge only a live probe would catch) is closed at the test-proof layer.
- **建议改法和验证点**: None remaining for this finding. Runtime confirmation is controller Failsafe 48/0/0/0/0 post-fix (2026-10-30T11:55).
- **修复风险（低/中/高）**: 低 (test-only, savepoint-isolated)
- **严重程度（低/中/高/严重）**: 中 (original) — **status: fixed**

### F-PA7-S1-B-02 — 已修复-低-cleanup verify covers `consent_record`

- **入口/函数**: `PostgresContainerIT.cleanupV004ConcurrentFixtures` (≈18757–18931)
- **文件(行号)**: `PostgresContainerIT.java` consent collection ≈18798; verify per-ID `count(*) = 0` 18913–18917
- **输入场景**: Plan §8.1 “rollback leaves no fixtures”; validation-fixes §3 complete rollback with no-fixture evidence.
- **实际分支**: After deleting collected `consentIds` under disabled `trg_consent_record_immutable` and restoring triggers (`tgenabled = 'O'` asserted), the verify block now asserts zero rows for every consent ID.
- **预期行为**: Every fixture class collected by cleanup, including V007 `consent_record`, has an executable zero-row proof.
- **实际行为**: Per-ID `count(*) = 0` for each `consentIds` entry is asserted after the `payment_event` loop. Cleanup order, named trigger disable/restore, `tgenabled='O'` check, and all prior checks unchanged.
- **直接证据**: Fix doc §Accepted fix 2; source 18913–18917. Initial gap (verify omitted consents) is closed without weakening prior assertions.
- **影响**: Silent leftover consent rows would now fail cleanup verification.
- **建议改法和验证点**: None remaining.
- **修复风险（低/中/高）**: 低
- **严重程度（低/中/高/严重）**: 低 (original) — **status: fixed**

### F-PA7-S1-B-03 — 已修复-低-RESTRICT name pin decoupled (single-FK representative fixture)

- **入口/函数**: `PostgresContainerIT.assertV007MissingParentFkProbesAndParentDeleteRestrict` (≈8083–8248)
- **文件(行号)**: `PostgresContainerIT.java` representative fixture 8205–8229; exact pin `expectSqlFailure(..., "23503", "fk_consent_account", ...)` retained
- **输入场景**: Plan §8.1 representative parent-delete RESTRICT; validation-fixes §4.2 names `fk_consent_account`.
- **实际分支**: Fixture creates fresh account/subject/consent/activation but **no transition row** (comment 8205–8209), so `customer_account` is referenced only by `fk_consent_account`.
- **预期行为**: The RESTRICT probe pins exactly `fk_consent_account` without catalog-order coupling to `fk_activation_transition_actor_account`.
- **实际行为**: `v007InsertIvrTransition` removed from this fixture; `expectSqlFailure(connection, "23503", "fk_consent_account", "DELETE FROM customer_iam.customer_account WHERE id = ?", restrictAccountId)` retained verbatim. All ten missing-parent FK probes and the positive control unchanged.
- **直接证据**: Fix doc §Accepted fix 3; source 8205–8229. Initial open question 3 (intentional dual pin vs accidental coupling) resolved as accidental coupling, fixed by option (a) from the initial report.
- **影响**: Multi-FK RESTRICT name-portability flake risk removed; exact name pin still enforced.
- **建议改法和验证点**: None remaining.
- **修复风险（低/中/高）**: 低
- **严重程度（低/中/高/严重）**: 低 (original) — **status: fixed**

### A/F2 — 已修复-低-exact trigger event set (`tgtype` 27 / 19)

*(Accepted in the same controller fix batch; verified here because rereview1 requires the full corrected diff. Not re-opened from a peer report.)*

- **入口/函数**: `PostgresContainerIT.assertV007FunctionsAndTriggersExact` (≈7923–8002)
- **文件(行号)**: `PostgresContainerIT.java` 7961–7997
- **输入场景**: Trigger catalog must reject added INSERT(4)/TRUNCATE(32)/INSTEAD(64) bits beyond the frozen event sets.
- **实际分支**: Query selects `t.tgtype`; per-trigger expected array carries the full bitmask; exact equality.
- **预期行为**: `trg_consent_record_immutable` and `trg_activation_transition_immutable` are `tgtype = 27` (BEFORE ROW UPDATE OR DELETE = 1+2+8+16); `trg_service_activation_guard` is `tgtype = 19` (BEFORE ROW UPDATE = 1+2+16).
- **实际行为**: Exact `tgtype = 27` / `19` equality implemented; relation/function/existence/uniqueness assertions preserved.
- **直接证据**: Fix doc §Accepted fix 4; source comment and assertions at 7961–7997.
- **影响**: Trigger event-set drift would fail closed.
- **建议改法和验证点**: None remaining.
- **修复风险（低/中/高）**: 低
- **严重程度（低/中/高/严重）**: 低 — **status: fixed**

### New material findings (rereview1 full corrected S1 diff)

未发现实质性问题.

Re-walked surfaces with no new material defect: V007 inventory vs plan §4/§8.1 and docs/14 §4.36–4.38/§11.4 (3/43/31/3/5/12/11/6/2/3/48); DDL order tables → indexes/checks → triggers → grants → 10 local FKs → late `fk_customer_order_consent`; ACL waves and column grants vs docs/14 §10.12 including cross-module reads; zero PUBLIC via `aclexplode`/`acldefault`; undeclared-negative column loops; staged V004 orphan-consent probe immutable and before V005; final-database order inserts use real consent parents with orphan `23503` + `fk_customer_order_consent`; V001–V006 immutability; source/CI/Failsafe XML 48-name identity with no `@Disabled`/assumptions/skips.

## Open Questions

- 无 blocking questions for S1 rereview1.
- Initial-review questions 1–3 are closed by the accepted fixes (live `FOR UPDATE`+`FOR SHARE` probes landed; `FOR SHARE` included; dual-FK coupling removed).
- Deferred per controller (not S1 blockers): A/F4 S2 immutability probes; A/F5 historical artifact hash; runtime build-timestamp note.

## Residual Risk

| Residual | Owner |
| --- | --- |
| S2 lifecycle / rollback / concurrency methods (3 methods, 51 total) | PA7 S2 BackendQA |
| Activation/consent service orchestration, locking, semantic currentness | Backend / Activation-IVR-Catalog (future service work) |
| A/F4 immutability probes deferred to S2 | PA7 S2 test author |
| A/F5 historical artifact hash / runtime build-timestamp note | Controller / build pipeline |
| Real provisioning/security Q17/Q23 and cloud/media/provider integration | PlatformSecurity / Telephony |
| Independent Maven/Docker re-execution not performed by this reviewer (controller evidence used) | Controller acceptance |

No new product policy was invented. No stronger product rule than the frozen schema was required.

## Coverage and validation (honest)

**Done in this rereview session:**

- Deepreview skill identity verified (`.pa7-review-skills/deepreview-SKILL.md` SHA256 `8287d098…3c467e6`).
- Frozen SHA-256 verified **before and after** (all seven match `.pa7-s1-review-freeze.json`).
- Own initial report `code-review-20261030-113444-B.md` used as the sole finding baseline; peer reports not read.
- Full corrected S1 diff re-walked (not fix-hunks only): V007 migration in full; V007 constants/maps; staged `@BeforeAll` + `probeV004OrphanConsentOrderAndRollback` + immutable `V004OrphanConsentProbeEvidence`; `assertV007FunctionsAndTriggersExact` (tgtype 27/19); `assertV007MissingParentFkProbesAndParentDeleteRestrict` (decoupled RESTRICT); `assertV007SchemaUsageMatrix` / `assertV007OwnedAndCrossModuleGrants` / `assertV007PublicAclIsZero` / `assertV007GuardFunctionExecuteBoundaries`; `assertV007RuntimeDenialsAndLegalUpdateControl` (row-lock probes); `assertV004ForeignKeyAndStagedConsentFkBackstops` + `insertPurchaseConsent`/`insertCustomerOrder`; `cleanupV004ConcurrentFixtures` consent verify; CI expected set; README; implementation/validation/fix artifacts.
- Binding plan + docs/12 §5,6,14,15 Phase B and docs/14 §4.36–4.38,5,9,10.12,10.16–10.20,11.1,11.4 checked against the migration and test proofs.
- Controller evidence inspected (shared `backend/target` read-only):
  - `failsafe-summary.xml`: completed 48 / errors 0 / failures 0 / skipped 0 / flakes 0.
  - Failsafe XML: `tests="48" errors="0" skipped="0" failures="0" flakes="0"` (post-fix run 2026-10-30).
  - `pa7-resume-full-wsl.log`: Surefire 10/0/0/0; Failsafe `PostgresContainerIT` 48/0/0/0; `BUILD SUCCESS` (Flyway `pa1_broken_history` ERROR is the intentional negative fixture).
  - `pa7-resume-boot.log`: Boot 4.1.2 `Started OneCallApplication`.
  - `pa7-resume-jar-index.txt`: no `flyway`/`postgresql`/`testcontainers`/`liquibase` entries (forbidden-dependency firewall consistent with validation-fixes §6).
- Tests reviewed as executable proof of catalog/ACL/lock/FK behavior, not style.

**Not done / limitations:**

- No independent Maven/Java/Docker run by this reviewer. Runtime claims are controller evidence only.
- Shared `backend/target` was not mutated.
- Peer initial/new reports intentionally unread.
- No fix, stage, commit, push, PR, merge, agent spawn, or gate entry.
- S2 methods and non-persistence layers not in this gate.

**Rereview1 disposition:** all accepted Reviewer-B findings **fixed**; accepted A/F2 **fixed**; **no new material findings**; S1 corrected diff is acceptable for controller disposition. Reviewer B has no merge authority.
