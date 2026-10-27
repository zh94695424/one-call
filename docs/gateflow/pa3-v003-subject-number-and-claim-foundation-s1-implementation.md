# PA3 V003 S1 implementation evidence

1. Task label / work unit / gate / slice / date / author-provider
   - Task label: `pa3-v003-s1-implementation-mimo26flash-20261027-01`
   - Work unit: `PA3-v003-subject-number-and-claim-foundation`
   - Gate: `implementation`
   - Slice: **S1 only** (migration, exact catalog/ACL/structural guards; no S2 lifecycle/concurrency methods)
   - Date: 2026-10-27 (validation completed 2026-10-28 local)
   - Author/provider: code author (`mimo26flash`, one-shot bounded implementation)

2. Binding inputs read and control-document confirmation
   - Read in full: `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-plan.md` (accepted checkpoint plan).
   - Read current PA3 section of `docs/gateflow/project-execution-control.md` (never edited by this agent).
   - Binding design: `docs/12-technology-stack-and-system-architecture.md` sections 5, 6, 14, 15 Phase A; `docs/14-production-data-model-and-flyway-schema-spec.md` sections named by the plan (2.1–2.8, 3.1–3.2, 4.12–4.15, 5.3, 6.2, 7.1–7.2, 8.2, 9.0–9.0.1, 9.6–9.8, 9.10, 9.23, 10.8, 10.8.1, 11.3, 12.4, 13.5).
   - Read: `V001__platform_schemas_roles_idempotency_outbox_audit.sql`, `V002__identity_organization_and_employee.sql`, full `PostgresContainerIT.java`, `.github/workflows/ci.yml`, `backend/README.md`, `backend/pom.xml`.
   - Control document: **not modified** by this agent. `git status` still shows only the controller-owned PA3 `accepted plan commit` row diff already present at task start.

3. Commits, hashes, V003 before/after, probe
   - Base / HEAD at implementation: `093a98a24f659e764ce2dc97e9e69ac32a6b8b7b` (accepted-plan checkpoint; message `gateflow: accept plan for PA3-v003-subject-number-and-claim-foundation`).
   - Prior accepted ancestry (from plan): base `58c13de895cccf44c0101a865b97e19ae2afef20`; PA2 final head `96a23c4f837afe6784188bec2ee35dd16dfaa937`.
   - V001 HEAD blob SHA-256: `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` (matches frozen contract).
   - V002 HEAD blob SHA-256: `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` (matches frozen contract).
   - V003 SHA-256 (authored content before the source-guard comment wording fix): `557897f35e3c872f777e9be0bf1477f388ed0ca20571ecf66fcf7604dc33dc5a`.
   - V003 SHA-256 **final S1** (after comment-only reword so plan §8.4 `grep -R -E 'V00[4-9]|V0[1-9][0-9]'` on `db/migration` exits clean): `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1`.
   - Catalog behavior identical across both V003 hashes (SQL comment only). Full container suite re-run after final content.
   - Planning disposable `v003-probe` container: not present at S1 (`docker ps -a` grep empty). Testcontainers-managed containers stop with the JVM; no leftover probe name.

4. Changed paths (exact five allowed writes), SHA-256, content-only diff manifest
   | Path | Action | SHA-256 (worktree) | Content-only note (CRLF baseline) |
   | --- | --- | --- | --- |
   | `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` | new | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` | LF; 260-line migration |
   | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited | `09a1ae4e6142a8c4b5bbd836467d9e0a3ca6290cd25c3bc7a5f21b2e0cad74e8` | LF; HEAD was LF; +1208/−29 content lines; 5405 → 6584 lines |
   | `.github/workflows/ci.yml` | edited | `0fafc0ecda03dccfa85b07e7796b837c24fb50e8eec2e735812981e1171ece12` | LF restored after edit; +6/−4; failsafe expected set only |
   | `backend/README.md` | edited | `443229fbce1e27ec31aba0ccae3191e37d86b3327251b64a904fb2806ac712b9` | LF restored after edit; +8/−8; S1 inventory/count only |
   | `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-implementation.md` | new | (this file) | LF evidence artifact |
   - Untracked/modified outside the five paths: only pre-existing controller `docs/gateflow/project-execution-control.md` (untouched by this agent).
   - No repository-wide line-ending normalization.

5. Scope actually implemented (S1 catalog/ACL totals, proven by container assertions)
   - **4 tables** in schema `subject_number`, owner `subject_number_owner`: `business_subject` (9 cols), `caller_number_binding` (9), `subject_claim` (10), `subject_risk_state` (8) = **36 columns**.
   - **25 named checks** (6/7/6/6) with whole `pg_get_constraintdef` equality vs frozen `V003_CHECK_DEF_MAP` (ASCII-whitespace normalizer only).
   - **7 ordinary immediate FKs** `ON UPDATE RESTRICT ON DELETE RESTRICT`, catalog props asserted `rrs|false|false|true`.
   - **7 PK/ordinary-unique** constraints (4 PK + 3 UNIQUE) with ordered conkey columns.
   - **18 indexes** (incl. `uq_binding_active_number`, `uq_claim_accepted_subject_account` partial unique; `ix_risk_open` partial non-unique).
   - **4 SECURITY INVOKER trigger functions** with fixed `search_path = subject_number, pg_temp`: `reject_row_mutation`, `guard_subject_owner`, `guard_caller_number_binding`, `guard_subject_risk_state`; no `p_id`, no `gen_random_uuid`, no transaction control.
   - **4 triggers**: three BEFORE ROW UPDATE guards + `trg_subject_claim_immutable` BEFORE ROW UPDATE OR DELETE via `tgtype` bits (not raw event-order string).
   - Exact owned/cross/column grants; `subject_identity_verification_role` narrow real-name SELECT/UPDATE columns + EXECUTE only on `guard_subject_owner`; `reject_row_mutation` not granted to runtime/verification/PUBLIC.
   - Zero V003 `id` defaults; zero sequences in `subject_number`; zero V004+ schemas/roles/`issue_first_trial`.
   - Global after V003: 15 functions, 11 non-internal triggers across six S1 schemas (asserted in inventory).
   - V003 defaulted columns total = **9** (four on `business_subject`, two on `caller_number_binding`, three on `subject_risk_state`; claim has none) — matches per-column default rows; no `id` default.
   - No application service, no POM/module/main Java change, no V004+ object.

6. Test methods added/changed and CI expected set/count (20)
   - Renamed: `v001ThenV002MigrateCleanPostgres16WithoutSkipping` → `v001ThenV002ThenV003MigrateCleanPostgres16WithoutSkipping` (migrationsExecuted=3; history 001/002/003; three exercise calls incl. `exerciseV003InsertsAsRuntimeRoleAndRollBack`).
   - Renamed: `v001AndV002InventoryMatchesTheFrozenContract` → `v001ThroughV003InventoryMatchesTheFrozenContract` (now also calls V003 column/check helpers).
   - Added: `v003SubjectNumberCatalogAndGuardsAreExact`, `v003OwnerPublicAndRuntimeAclsAreExact`.
   - Unchanged prior 16 method names preserved with prior assertions extended only where required (function/search_path inventory includes subject_number; grants include subject_number).
   - `@Test` count in `PostgresContainerIT` = **20**. No `@Disabled`, `Assumptions`, or Docker skip.
   - CI `.github/workflows/ci.yml` failsafe expected set = exactly those **20** names; labels `V002 test count` / `differ from V002` → **`V003 test count` / `differ from V003`**. Docker-first order, `-Pcontainer-smoke verify`, boot smoke, fat-jar firewall, contract jobs unchanged.

7. Exact commands, environment, exits, counts
   Environment:
   - JDK: Eclipse Temurin **21.0.13+9** at `/opt/jdk21` (`JAVA_HOME` set; matches `.java-version` / Enforcer).
   - Maven: **3.9.9** via Maven Wrapper distribution (SHA-checked by wrapper); invoked as `bash /tmp/mvnw-run/mvnw` because worktree `backend/mvnw` has CRLF shebang (wrapper properties copied to temp; no repo write).
   - Docker: **29.1.3**; `docker info` OK before container runs.
   - PostgreSQL: Testcontainers image **postgres:16** → server **PostgreSQL 16.15**.
   - Flyway: Boot `4.1.2` BOM-managed **12.4.0** (test scope only; Surefire classpath-excluded).
   - Testcontainers **2.0.5**.
   - OS: Linux WSL2 `6.18.33.2-microsoft-standard-WSL2`.

   Commands and exits (final run after last V003 comment edit):
   | Command | Exit | Result |
   | --- | --- | --- |
   | `./mvnw -B -ntp validate` | 0 | Enforcer 5/5 passed |
   | `./mvnw -B -ntp clean verify` | 0 | surefire **10** (`8+1+1`), `skipped=0 failures=0 errors=0`; BUILD SUCCESS |
   | `docker info` + `./mvnw -B -ntp -Pcontainer-smoke verify` | 0 | failsafe **20**, `skipped=0 failures=0 errors=0`; surefire 10 also `skipped=0`; BUILD SUCCESS; Flyway applied 3 migrations to v003; validate clean; repeat migrate zero |
   | `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` + `grep -F 'Started OneCallApplication'` | 0 | boot log contains `Started OneCallApplication` |
   | `jar tf` + firewall `grep -Eq 'BOOT-INF/lib/(spring-security\|flyway\|jooq\|HikariCP\|postgresql\|testcontainers)'` | 0 (no match) | FIREWALL_OK |

   Intermediate fail-closed debugging runs (same suite, assertion fixes only, no weakened checks): defaulted-columns total corrected 7→9; FK property query cast `"char"`→`text`; trigger expected-array indices aligned to `tgtype` matrix. Final run is green.

   Failsafe exact method set (20): the CI expected list in §6 above (alphabetical XML order matches CI set equality).

8. Negative / concurrency evidence at S1
   - S1 is catalog/ACL only. No dedicated S2 concurrency method (`concurrentActiveBindingUniquenessHasOneWinner`) exists or was run; plan §8.4 focused S2 repetitions are not applicable at S1.
   - Negative/structural evidence inside the 20 methods:
     - `brokenMigrationFailsVisibly`: intentional Flyway failure sentinel; marker table rolled back (pre-existing).
     - V003 smoke exercise inserts then **rolls back**; post-rollback zero-row checks for all six method-local IDs.
     - Claim append-only path asserted structurally (trigger timing/events/function); runtime DELETE cleanup never used on claim rows.
     - `assertNoV004PlusObjects`: zero S2/S3 schemas/roles and zero `issue_first_trial`.
     - Guard EXECUTE boundary: `reject_row_mutation` false for runtime/verification/PUBLIC; verification false on non-owner guards.
     - Column-level vs table-level: `business_subject` table INSERT false for runtime while column INSERT true.
   - No sleeps-only timing assertions introduced at S1.

9. V001/V002 hash and whitespace/diff evidence
   - `git show HEAD:V001... | sha256sum` → `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` ✓
   - `git show HEAD:V002... | sha256sum` → `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` ✓
   - `git diff --ignore-cr-at-eol --exit-code HEAD -- V001 V002` → exit **0** ✓
   - Migration dir contains exactly V001, V002, V003; `grep -R -E 'V00[4-9]|V0[1-9][0-9]' backend/src/main/resources/db/migration` → no match ✓
   - Scoped whitespace: `git -c core.whitespace=cr-at-eol -c core.autocrlf=false -c core.safecrlf=false diff --check` on the three edited tracked paths → exit **0**, no diagnostics ✓
   - New file: `git diff --no-index --check /dev/null V003...` → no whitespace diagnostics ✓
   - `grep -n -E '[[:blank:]]+$'` on all four code/CI/README/migration files → no output ✓
   - New/edited files are LF (`file` reports no CRLF) ✓
   - **Caveat (plan R-02):** raw global `git diff --check` is not authoritative on this Windows-checkout worktree (CRLF baseline + pre-existing documentation trailing whitespace). The scoped CR-aware check above is the fail-closed gate. No repository-wide EOL normalization was performed.

10. README/CI changes and documentation decision
    - CI: only failsafe expected-set rename/add/`tests=20` and V002→V003 labels; all other steps untouched.
    - README: title/intro/evidence describe V001+V002+V003; V003 inventory (4 tables/36 columns/25 checks/7 FKs/18 indexes/3 guards/append-only claim/exact grants); container count **20**; renamed/new methods described; evidence boundary updated to PA3 S1; **no production-readiness claim**; toolchain pins, module table, Modulith note, firewall description unchanged.
    - Documentation decision: did **not** modify `docs/12`, `docs/14`, control document, plan artifact, or any PA1/PA2 artifact. Added only this S1 evidence file. No new architecture/API/security/schema docs.

11. Deviations, stop conditions, unresolved issues
   - No stop condition triggered: no V001/V002 edit, no V004+ object, no frozen predicate change, no extra production function/service, no POM/application/module change, no assertion weakening, no write outside the five allowed paths.
   - Bounded implementation corrections (not scope changes): defaulted-column total assertion 7→9 to match actual catalog/per-column rows; `::text` casts on FK `"char"` property concat; trigger expected-array index alignment. All are fail-closed exactness fixes, not relaxations.
   - V003 header comment reworded from literal `V004+` to `later-version` so the plan’s exact migration-dir grep guard passes; comment does not change catalog behavior.
   - Worktree `backend/mvnw` CRLF shebang prevented direct `./mvnw` under WSL; used a temp LF wrapper copy with identical `maven-wrapper.properties` (no repo mutation). Documented here as environment note, not a product deviation.
   - Owner/destination for any residual: see §12.

12. Residual risks (classified)
   | ID | Residual | Class | Owner / destination |
   | --- | --- | --- | --- |
   | S1-R01 | S2 five behavior/concurrency methods not yet implemented (suite is 20, not final 25) | later-approved-slice | S2 implementation agent / controller |
   | S1-R02 | Exact PG16 deparse literals frozen from 16.15; other 16.x minors could differ | fixed-current (fail-closed in tests) | Backend/QA; escalate on mismatch |
   | S1-R03 | GitHub Actions has not run this head | later-work-unit | Phaseflow/CI exact-head PR gate; local success ≠ CI green claim |
   | S1-R04 | Q2/Q17/Q23/Q24, enterprise ownership transfer, service claim protocols deferred | later-work-unit | Product/Security/Platform per plan R-01..R-08 |
   | S1-R05 | `RETIRED` not DB-terminal; `OPEN` not a dialing ban (by design) | fixed-current (explicit non-claim) | subject-number service / Q24 |
   | S1-R06 | Testcontainers `postgres:16` digest not pinned; superuser is fixture only | later-work-unit | Platform (Q17/Q23) |

13. Completion statement
    - **S1 implementation complete** for the allowed scope: V003 authored, container suite evolved 18→20 with exact catalog/ACL assertions, CI/README synchronized, validation green in author environment.
    - Explicitly **not** performed: commit, stage, push, PR, merge, branch/config mutation, control-document edit, self-review gate, S2 implementation, later-gate entry, GitHub Actions run, production provisioning, or production acceptance.

14. No-goal-drift statement and exact changed-file list
    - No goal drift: only the five S1 paths were written; V003 contains only the frozen four-table/25-check/7-FK/18-index/4-function/4-trigger/grant contract; tests prove catalog/ACL only; no IVR/activation/entitlement/order/evidence object; no application service; no binding-as-login; no Q24 dialing policy; no invented enterprise ownership or role-to-claim permission.
    - Exact changed-file list:
      1. `backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql` (new)
      2. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (edited)
      3. `.github/workflows/ci.yml` (edited)
      4. `backend/README.md` (edited)
      5. `docs/gateflow/pa3-v003-subject-number-and-claim-foundation-s1-implementation.md` (new; this report)
