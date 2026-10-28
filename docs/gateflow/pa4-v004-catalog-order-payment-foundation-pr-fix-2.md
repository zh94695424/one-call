# PA4-v004-catalog-order-payment-foundation PR fix 2 (F-PA4-PR-RR1-01, F-PA4-PR-RR1-02)

- Work unit: `PA4-v004-catalog-order-payment-foundation`; gate: fix after independent PR re-review 1.
- Task: `pa4-v004-pr12-fix2-mimo26flash-20261028-01`; provider `mimo26flash` / `mimo-v2.6-flash`; author role (not reviewer/controller).
- Base/head: immutable base `81022037dc5dced625788477835472a84a16d52d`; pre-fix head `c9e3358957458c7d87330b9aad481777cebb633f`; branch `codex/pa4-v004-catalog-order-payment-foundation`.
- Binding goal: frozen V004 database persistence foundation only (4 schemas / 8 roles / 7 tables / 78 columns / 51 checks / 11 FKs / 5 functions / 7 triggers / exact ACLs). V001-V004 byte-identical. No services, APIs, JPA, provider integration, runtime DataSource/Flyway wiring, V005+, frontend, cloud or provisioning work. Existing S1/S2/aggregate slices and closed finding `F-PA4-PR-02` remain accepted. This fix closes accepted re-review findings `F-PA4-PR-RR1-01` and `F-PA4-PR-RR1-02` only.

## Scope

- Source of the findings: `docs/reviews/pr-12-review-20261028-131050.md` (SHA-256 `E1A1204815719FAFD35239CD1BDE3DA45E8CB6A155EB4FE068819097DCD4B52A`), findings `F-PA4-PR-RR1-01` and `F-PA4-PR-RR1-02` (accepted by the controller at the PR re-review 1 gate; both low severity, test/evidence layer only).
- Frozen rules: docs/14 §7.3 (catalog `OFFSALE`, order/payment `PAID`/`CLOSED` terminality is service-owned; DB owns shapes and revision-conditional updates only) and docs/14 §10.16 (`guard_catalog_offer` rejects `catalog_version_id`/`channel` changes; `SECURITY INVOKER` guards with `EXECUTE` granted to the owning runtime role; owner-path guard probes remain valid owner-path checks and are not role-branched).
- Changed paths (exactly the allowed delta, three paths):
  - `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (test-only; net +3/−1 relative to the fix-1 state: one `switchLocalRole` line added plus a two-line comment replacing the one-line comment; cumulative vs HEAD `c9e3358` now +106/−43, previously +104/−43)
  - `docs/gateflow/pa4-v004-catalog-order-payment-foundation-pr-fix-1.md` (one sentence corrected; +1/−1)
  - this new artifact `docs/gateflow/pa4-v004-catalog-order-payment-foundation-pr-fix-2.md`
- No migration, service, API, CI, application code, frontend, design source, README, historical review artifact, prior gateflow artifact (other than the single sentence in the author-owned fix-1 artifact), controller `docs/gateflow/project-execution-control.md` or `.pa4-*` controller file was modified. The pre-existing controller-owned control-document worktree modification and untracked controller files were not touched by the author. Historical artifacts `docs/reviews/*` and the S1/S2/aggregate/fix-1 implementation artifacts were not rewritten.

## Finding author-status

- `F-PA4-PR-RR1-01`: author-reported **已修复**, pending fresh independent re-review. The two positive `catalog_offer` UPDATE probes (`version_token` revision 1→2, `offer_status` revision 2→3) now execute under runtime role `catalog_order_role` again, restoring the dynamic runtime-role positive ACL execution coverage that the fix-1 deletion of the `OFFSALE→ACTIVE` reactivation block had dropped. Owner-path published-content guards remain owner-path checks before the switch; the `CATALOG_OFFER_RELATION_IMMUTABLE` relation guards remain owner-path checks after the retained owner switch.
- `F-PA4-PR-RR1-02`: author-reported **已修复**, pending fresh independent re-review. The fix-1 artifact's paid-order stale-probe sentence no longer claims "real stale revision on the matching status"; it now describes the probe accurately as a stale `UNPAID`/revision-1 replay (`WHERE payment_status='UNPAID' AND revision=1` against the now-`PAID`/revision-2 row) rejected by the combined status and revision mismatch, and defers genuine status-matching stale-revision evidence to the independent `closedOrderId`, `paymentOrderId` and `closedPaymentOrderId` fixtures that already provide it (`CLOSED`/rev1 vs `CLOSED`/rev2, `PAID`/rev1 vs `PAID`/rev2, `CLOSED`/rev1 vs `CLOSED`/rev2). No code was changed for this finding and no probe was relabeled as status-matching.

## Exact changes

### `assertV004CatalogVersionLifecycleAndGuardBackstops` (F-PA4-PR-RR1-01)

1. Unchanged: owner-path `CATALOG_VERSION_PUBLISHED_IMMUTABLE` probes (snapshot tamper; `published_at=NULL`) execute while `versionId` is still `ACTIVE`, under `catalog_order_owner`.
2. Added immediately after those owner probes and before the first positive `catalog_offer` UPDATE: `switchLocalRole(connection, "catalog_order_role");` plus a comment noting the runtime-role positive updates and owner-path relation guards.
3. Unchanged: both positive `catalog_offer` UPDATEs keep their exact SQL, WHERE clauses, expected rowcount `1`, and order (`version_token` then `offer_status`); they now run under `catalog_order_role` as before the fix-1 overlay (per pre-fix `git show HEAD`).
4. Unchanged: the subsequent `switchLocalRole(connection, "catalog_order_owner")` and both `CATALOG_OFFER_RELATION_IMMUTABLE` probes (`channel`, `catalog_version_id`) remain owner-path.
5. Unchanged: everything else in the method — publication flow, second-ACTIVE `uq_catalog_version_active` denial, legal `ACTIVE→OFFSALE`, second-version publish, exactly-one-ACTIVE assertions, append-only probes, 22 `checkProbe` entries, rollback and post-rollback zero counts. Zero assertions were added, removed, duplicated or re-expectationed; the delta is one role-switch statement and a comment clarification only.

### fix-1 artifact sentence (F-PA4-PR-RR1-02)

1. `assertV004OrderPaymentLifecycleAndAppendOnlyBackstops` code is untouched. The `orderId` stale probe still uses `WHERE id = ? AND payment_status='UNPAID' AND revision = 1` against the `PAID`/revision-2 row and still asserts zero rows.
2. `docs/gateflow/pa4-v004-catalog-order-payment-foundation-pr-fix-1.md` item 1 of the order/payment section now ends: stale revision-1 `UNPAID→PAID` replay (`WHERE payment_status='UNPAID' AND revision=1` against the now-`PAID`/revision-2 row) → 0 rows, rejected by the combined status and revision mismatch (not a status-matching probe; genuine status-matching stale-revision evidence is provided by the independent `closedOrderId`, `paymentOrderId` and `closedPaymentOrderId` fixtures below). No other fix-1 sentence, section or claim was altered; the historical S2 artifact remains superseded exactly as fix-1 recorded.

## No migration/hash/count changes

- Frozen Git blobs (recomputed this session): V001 `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`; V002 `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5`; V003 `1D3FFB5FFD60D1BE5F85B93B87F52C727E7B43DAD5929280123CC567AB873FB1`; V004 `A7BA7A8CBAF5EECAF04F117B53C1B4BFD31C31B04E9432984D6C108B6E496EDC` — all match the frozen values; `git diff HEAD` over `backend/src/main/resources/db/migration/` is empty.
- `backend/README.md` unchanged from fix-1: SHA-256 `FA37B8C58BFCA104348529C96BCE0273F6EF9ABC65CB1679C9B7F90CD2E43AB8`.
- Counts unchanged: 31 `@Test` methods (source/XML/CI exact-equal, zero `@Disabled`/`Assumptions`/skips); 51 `checkProbe` entries (catalog 22 + order/payment 29); 11 FK backstop assertions (`assertV004ForeignKeyAndAbsentConsentFkBackstops` untouched); ACL/denial matrices, five concurrency scenarios, cleanup/rollback assertions untouched. No service, API, controller, frontend or CI file changed.

## Actual validation (fresh runs in this session, Windows JDK 21.0.12.1)

All Maven commands run via `powershell.exe -NoProfile` with `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` and backend cwd; WSL `mvnw` was not used (CRLF). Runs were sequential.

| Check | Command | Result |
| --- | --- | --- |
| Workspace | `pwd -P`; `git rev-parse HEAD`; `git branch --show-current`; `git status --short` | Correct workspace; HEAD `c9e3358957458c7d87330b9aad481777cebb633f`; branch `codex/pa4-v004-catalog-order-payment-foundation`; modified tracked files are the test class, README (fix-1 state), controller-owned control doc (untouched by author); fix-1/fix-2 and review artifacts untracked as expected. |
| validate | `mvnw.cmd -B -ntp validate` | BUILD SUCCESS; exit 0; Enforcer rules passed. |
| default verify | `mvnw.cmd -B -ntp clean verify` | BUILD SUCCESS; exit 0; Surefire `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` (1+8+1); total time 7.817 s. |
| container smoke | `mvnw.cmd -B -ntp -Pcontainer-smoke verify` | BUILD SUCCESS; exit 0; Docker 29.7.2 (npipe); PostgreSQL **16.15**; Flyway validated and applied 4 migrations (001-004); Failsafe `Tests run: 31, Failures: 0, Errors: 0, Skipped: 0` (18.73 s IT); total time 24.984 s. The catalog lifecycle method executed its two positive offer UPDATEs under `catalog_order_role` and passed. |
| XML/method sets | read-only Python parse | Failsafe `tests=31 errors=0 skipped=0 failures=0`; the 31 `testcase` names equal the source `@Test` set and the CI expected set exactly; Surefire 1+8+1 = 10; `@Disabled`=0; `Assumptions.assume`=0. |
| Probe counts | read-only Python count | `checkProbe(` in the catalog lifecycle method = 22; in the order/payment lifecycle method = 29; total = **51**, unchanged. |
| Frozen Git blobs | `git cat-file blob HEAD:<migration> \| sha256sum` | V001-V004 all match the frozen values listed above. |
| Scope/whitespace | `git diff --numstat HEAD -- backend/`; `git diff --check HEAD -- backend/ docs/gateflow/`; migration diff | backend: README +1/−1 (fix-1 state, unchanged), test class +106/−43 (fix-1 was +104/−43; this fix adds net +2 lines); `git diff --check` exit 0, empty output; migration diff empty. |
| Post-fix hashes | `sha256sum` | `PostgresContainerIT.java` `9B2AA50DD54B612CECC66CDA9833D183470FCB18D05A130CCE935C757538439C` (was `F81FC178FF0CFE895584B28630D033DBAFC9C8F0585966799E8A758CA9FBD72E` at fix-1); fix-1 artifact `C797D839119EE05E6140317168E0A650390CB45895305B27113EBDEE010759DF` (was `4C78352AA8599C2968710B99B8F46D9886CA4E5C84B29A7D44D74ADEC6B50AD8`). |
| Docker cleanup | `docker ps -a` after final run | No Testcontainers/ryuk residue; `pa3-pr11-review-pg` and unrelated long-lived containers preserved. |
| Permission denials | session-wide | None. One initial log-redirect attempt mis-parsed the PowerShell `*>` redirection and wrote a misnamed UTF-16 log file (`backend/UsersAdministratorAppDataLocalTemppa4-fix2-default-verify.log`); it was deleted immediately, the identical command without redirect ran successfully, and the final tree contains no stray file. No commit, stage, push, PR mutation, merge, gate entry or nested agent occurred. |

Inherited evidence (not rerun in this session): boot smoke, production-jar dependency firewall, focused three-run concurrency repetitions, exact-head GitHub CI run `40632305220` (pre-fix head only), and all prior S1/S2/aggregate/PR-fix-1 review conclusions. Failure-injected timeout/race cleanup was **never run** in any session and is not claimed here.

## Residual risks and owners

| ID | Risk | Owner | Destination |
| --- | --- | --- | --- |
| RR-01 | Exact-head CI re-run and JDK pin (local 21.0.12.1 vs CI 21.0.13+9) after the fix commit/push; remote CI run `40632305220` covers only pre-fix head `c9e3358`. | Controller/CI | After re-review acceptance, commit and follow-up push. |
| RR-02 | `postgres:16` floating tag (16.15 observed), fixture superuser, production provisioning/digest not pinned. | Platform/Database/Security (Q17/Q23) | Production/image hardening waves. |
| RR-03 | Serial seven-table `TRUNCATE` cleanup; timeout/aborted-race failure injection was never executed. | Backend/QA | Later test-hardening slice if requested. |
| RR-04 | V004 check/index literals come from PG 16.15 deparse; minor-version deparse drift fails the suite. | Backend/QA | Pin 16.15; real diffs stop for review. |
| RR-05 | Other stale README/Javadoc labels outside line 64 (counts are correct). | Backend/QA | Later documentation pass. |
| RR-06 | Column-level REFERENCES denial coverage still absent (no live grant). | Backend/QA | Deferred hardening slice. |
| RR-07 | Production purchase/payment/issuance/provider payloads, Q2/Q6/Q20, catalog-offer withdrawal (OQ-4) remain unimplemented service work. | Named service owners | Later service phases, never guessed in V004. |
| RR-08 | `purchase_consent_id` FK deferred to V007; current frozen state is NOT NULL with no FK. | V007 owner | Named migration continuation. |
| RR-09 | Focused three-run concurrency repetitions are inherited controller evidence without per-run XML from this session. | Controller/QA | Full container profile suffices; repetition XML on demand. |
| RR-10 | Closed by this fix subject to fresh independent re-review of the final test-class delta, corrected fix-1 sentence and this artifact. | Controller/re-review | Next gate: fresh DeepSeek PR re-review 2. |

## Completion

- Runtime/provider/task: local Windows JDK 21.0.12.1 + Docker 29.7.2 / PostgreSQL 16.15; provider `mimo26flash` (`mimo-v2.6-flash`); task `pa4-v004-pr12-fix2-mimo26flash-20261028-01`.
- Paths + artifact: this artifact at `docs/gateflow/pa4-v004-catalog-order-payment-foundation-pr-fix-2.md`; changed files `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (SHA-256 `9B2AA50DD54B612CECC66CDA9833D183470FCB18D05A130CCE935C757538439C`) and `docs/gateflow/pa4-v004-catalog-order-payment-foundation-pr-fix-1.md` (SHA-256 `C797D839119EE05E6140317168E0A650390CB45895305B27113EBDEE010759DF`).
- `F-PA4-PR-RR1-01` author status: **已修复**, awaiting independent re-review. `F-PA4-PR-RR1-02` author status: **已修复**, awaiting independent re-review. No self-review performed.
- Actual changes: one `switchLocalRole(connection, "catalog_order_role")` statement restored immediately before the two positive `catalog_offer` UPDATE probes, with owner-path published-content and relation-immutable guards preserved on the owner path; one fix-1 artifact sentence corrected to describe the paid-order stale probe as a combined status+revision-mismatch rejection instead of a status-matching stale revision.
- Validation: fresh sequential `validate`, `clean verify` 10/0/0/0 and `-Pcontainer-smoke verify` 31/0/0/0 (PG 16.15, four migrations, Docker 29.7.2) on the final code; exact source/CI/XML method sets of 31 with zero disabled/skip/failure/error; Surefire total 10; 51 probes (22+29) unchanged; frozen V001-V004 hashes unchanged; README unchanged from fix-1; scope/whitespace/cleanup checks clean.
- Permission denials or blocks: none material; no commit, stage, push, PR mutation, merge, comment, gate entry, nested agent or runner was performed. The controller owns intake, control-document update and the fresh DeepSeek re-review.
