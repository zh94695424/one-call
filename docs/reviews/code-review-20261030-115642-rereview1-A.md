# Code Review

## Scope

- Mode: Current Changes (frozen S1 diff, gate `S1 rereview1`)
- Task label: `pa7-s1-rereview1-A-20261030-115642`
- Branch: `codex/pa7-v007-activation-consent-foundation` (worktree HEAD detached at selected base)
- Base: `64b576abd4efce956d51d4e57a6b8afeeb13cd45`
- Output file: `docs/reviews/code-review-20261030-115642-rereview1-A.md`
- Work unit: `PA7-v007-activation-consent-foundation`, Slice S1 only
- Included scope (exactly the frozen review set in `.pa7-s1-review-freeze.json`, hashes verified before and after):
  - `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` (`bd919cf3…4e17d659`)
  - `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (`b568fc42…92ca0412`, 21,886 lines)
  - `.github/workflows/ci.yml` (`f34379ce…2e4480`)
  - `backend/README.md` (`0756cda8…8712832`)
  - `docs/gateflow/pa7-v007-activation-consent-foundation-s1-implementation.md` (`f497230f…e9e75a`)
  - `docs/gateflow/pa7-v007-activation-consent-foundation-s1-validation-fixes.md` (`6d4ee285…18bcf4`)
  - `docs/gateflow/pa7-v007-activation-consent-foundation-s1-review-fix-1.md` (`268e8c3e…ea5bc1f`)
- Binding sources read: accepted plan (854 lines); docs/12 §5, §6, §14, §15 Phase B; docs/14 §4.36–4.38, §5, §9.0/§9.0.1/§9.0.2/§9.11/§9.25, §10.12, §10.16–§10.20, §11.1, §11.4.
- Controller evidence inspected: `backend/target/pa7-resume-full-wsl.log`, `pa7-resume-focused-wsl.log`, `pa7-resume-boot.log`, `pa7-resume-jar-index.txt`, final Failsafe XML.
- Excluded scope: S2 lifecycle/rollback/concurrency methods, API/service/frontend/runtime wiring, V008/V009 objects. The 885 KB schema spec and 1.3 MB test/spec were not dumped; only the diff-relevant helpers/callers and binding sections were read. Peer reviewer B's prompt/output/artifact was not read.
- Parallel review coverage: none (single reviewer).

## Findings

**No new material correctness, security, or contract defect was found in the corrected S1 diff.**
The four controller-accepted fix items are present, executable, and green in the post-fix full profile. No new finding is raised; the only remaining observations are classified as residual risk.

### Disposition of my own accepted findings (from `code-review-20261030-113444-A.md`)

| ID | Original finding | Status | Direct evidence in current frozen hash |
| --- | --- | --- | --- |
| A/F1 (LOW, coverage) | No executed foreign row-lock denial probe for `activation_consent_role`. | **FIXED** | `assertV007RuntimeDenialsAndLegalUpdateControl` (test 8633–8755) now executes savepoint-isolated `FOR UPDATE` **and** `FOR SHARE` denials under `activation_consent_role` on all four foreign read tables — `subject_number.business_subject` (8670–8675), `customer_iam.customer_account` (8676–8681), `organization_access.organization` (8682–8687), `organization_access.organization_membership` (8688–8695) — and under `ivr_role`/`catalog_order_role` on the V007 reads (8703–8745). `expectSqlQueryFailure` (3083–3104) asserts the exact SQLSTATE (`42501`) and rolls back to its savepoint; a successful query would fail the test. Full post-fix run is 48/0/0/0, so these probes executed and raised `42501`. |
| A/F2 (LOW, coverage) | Trigger event-bit assertion was partial (no INSERT/TRUNCATE/INSTEAD absence proof). | **FIXED** | `assertV007FunctionsAndTriggersExact` now selects `t.tgtype` (7982) and asserts full bitmask equality: `27` for `trg_consent_record_immutable`/`trg_activation_transition_immutable` (BEFORE ROW UPDATE OR DELETE = 1+2+8+16) and `19` for `trg_service_activation_guard` (BEFORE ROW UPDATE = 1+2+16) at 7960–7972 and 7995–7997. This rejects extra INSERT(4)/TRUNCATE(32)/INSTEAD(64) bits while retaining relation/function/timing/row assertions. |
| A/F3 (LOW, robustness) | Representative parent-delete RESTRICT name was coupled to RI trigger order when the same `customer_account` was referenced by two FKs. | **FIXED** | The representative fixture (8205–8229) now creates a dedicated `restrictAccountId`/`restrictSubjectId`/`restrictConsentId`/`restrictActivationId`, inserts only one consent referencing the account, and deliberately inserts no transition, so `customer_account` is referenced only by `fk_consent_account`; the `23503`/`fk_consent_account` pin is now single-FK. The removed dependency (`v007InsertIvrTransition`) is documented in the comment at 8205–8209. |
| A/F4 (LOW, coverage) | Head-guard behavior probed only `subject_id`, not `id`/`created_at`. | **STILL OPEN — deferred to S2 by controller** | The owner `P0001` probe still changes only `subject_id` (8696–8701); `id`/`created_at` remain behaviorally unprobed in S1. This is not a V007 defect: the plan assigns broader head immutability/lifecycle behavior to Slice S2, and the migration guard uses `IS DISTINCT FROM` on all three columns (migration 180–183). |
| A/F5 (INFO, process) | Frozen historical implementation artifact records a stale test hash. | **STILL OPEN — intentional historical preservation** | `s1-implementation.md:23` still records `64820b93…` / 21,358 lines while the final frozen test is `b568fc42…` / 21,886 lines; `s1-validation-fixes.md:18` and `s1-review-fix-1.md:10` carry the authoritative values. The controller confirmed the blocked historical report is preserved intentionally. No code action. |

Controller-accepted fix B/F-02 (consent cleanup verification) was also verified although it is not my finding: `cleanupV004ConcurrentFixtures` collects method-local consent IDs by `account_id`/`subject_id` (18798–18801), deletes orders before consents (18819–18826) after disabling and then restoring `trg_consent_record_immutable` under the migration identity (18809–18810, 18855–18856), asserts `tgenabled = 'O'` (18857–18869), and adds per-ID `count(*) = 0` consent verification (18913–18917).

### New findings

None material. No new correctness, security, contract, or test-proof defect was found in the corrected S1 diff, including in the four changed fix hunks.

## Open Questions

- None blocking. The focused evidence log (`pa7-resume-focused-wsl.log`, 10:46, 3/0/0/0) predates the test-only fix (11:51), so it is not a post-fix focused rerun; however, the final full profile at 11:56:08 exercises the same three V007 methods and all four fixed hunks, so this is an evidence-timestamp gap, not a correctness gap.

## Residual Risk

| Residual | Classification | Owner / destination |
| --- | --- | --- |
| The V004 helper still pins exact FK names for account/subject parent deletes (`fk_customer_order_account`, `fk_customer_order_subject`) where each parent is now also referenced by a V007 consent FK (test 18040–18046). On a fresh database the RI trigger order follows the fixed migration creation order, so this is deterministic today, but it remains coupled to engine/trigger-order behavior. | Low test-robustness residual; not a new material finding and not a migration defect | V007 test author / PA7 S2 BackendQA |
| Boot log and jar-index firewall evidence are timestamped 10:27 and the focused log 10:46, all before the 11:51 test-only fix. Production sources and the V007 migration are unchanged by the fix, and the post-fix full run is green; exact build-provenance alignment would require a controller rerun. | Evidence-timestamp note | Controller |
| Head immutability behavior for `id`/`created_at`, lifecycle/transition matrix behavior, head/ledger atomicity, lock order, narrow service matrix | Intentional S2/application-service boundary | PA7 S2 and activation-consent application service |
| Policy/prompt currentness, deterministic source refs/fingerprints, purchase-order semantic matching, operations employee authorization, runtime provisioning/Q17/Q23 | Deferred, not guessed | Product / activation-consent / IVR / catalog / operations-IAM / Platform-Security |
| V008 reciprocal deferred FKs and V009 sharing | Future waves | PA8/PA9 |
| No independent Maven/Docker execution was performed; the shared `backend/target` is read-only during parallel review. Large `git diff` of the test file and `git hash-object` for V001–V006 required runner approval and were not executed; prior-migration immutability was instead evidenced by `git status` showing only untracked V007. | Validation limitation | Recorded here; no execution claimed |

## Validation Performed and Limitations

- Frozen SHA-256 verified before inspection and again after inspection; all seven current hashes matched `.pa7-s1-review-freeze.json` exactly.
- V001–V006 immutability: `git status --short -- backend/src/main/resources/db/migration` shows only `?? V007__activation_and_consent.sql`; the six tracked prior migrations are unmodified at HEAD == base. Exactly seven V001–V007 migration files exist; no V008/V009 file.
- Migration reviewed line-by-line (312 lines) against docs/14 §4.36–4.38, §5, §10.12, §10.16, §11.1/§11.4: 3 tables, 43 columns (19/6/18), 31 checks (15/3/13), 3 PKs, 5 uniques, 4 standalone/12 total indexes, 10 local immediate RESTRICT FKs + 1 late immediate `ALTER_FK`, 6 defaults, 0 id defaults/sequences, 2 `SECURITY INVOKER` functions, 3 triggers, 0 SQL service functions; creation/grant waves and `RESET ROLE` ordering match the accepted plan.
- ACL proof reviewed: exhaustive 23-role (`S1_ROLES` 15 + `V004_ROLES` 8) plus `public` schema/table/column/function matrices, exact four foreign column-read sets, no `operations_iam` access, zero PUBLIC with default-inclusive OID-0 ACL expansion, runtime `42501`/owner `P0001` probes, and the corrected row-lock denials.
- Source/CI/XML identity: carrier source has 48 `void` test methods (49 raw `@Test` occurrences, one inline comment); CI `expected` set contains 48 names including both renames and all three V007 methods; final XML has `tests="48" errors="0" skipped="0" failures="0" flakes="0"` and 48 matching testcases with no `<failure>`/`<error>`/`<skipped>` elements.
- Runtime evidence inspected: `pa7-resume-full-wsl.log` shows PostgreSQL 16.15, staged migrations 4→1→1→1, Surefire 10/0/0/0, Failsafe 48/0/0/0, `BUILD SUCCESS` at `2026-10-30T11:56:08+08:00`; test source mtime 11:51:53 predates the run, so the run covers the frozen post-fix test. `pa7-resume-boot.log` records `Started OneCallApplication in 1.198 seconds`; `pa7-resume-jar-index.txt` has zero matches for the forbidden runtime dependency pattern.
- README reviewed: V001–V007/48-method S1 boundary is described without claiming S2, V008+, or production acceptance.
- Not performed: independent Maven/Testcontainers execution; independent end-to-end source/CI/XML parser execution (proven by direct name/count inspection and controller evidence); large test-file diff and git blob hashing (runner approval required).
- Scope discipline: no implementation, staging, commit, push, PR, merge, agent spawn, or other gate was entered. No peer reviewer output was read.

## Final Status

- Review status: **complete** for corrected S1 (catalog/security/ACL/FK/staged-prefix boundary).
- Verdict: **No new material finding. A/F1, A/F2 and A/F3 are fixed and verified; A/F4 and A/F5 remain open exactly as the controller deferred/preserved them.**
- Not claimed: S2 acceptance, PR readiness, merge authority, or production acceptance.
