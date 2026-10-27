# PA2 V002 Identity, Organization and Employee Foundation — S1 Implementation Evidence

- Task label: `pa2-v002-identity-organization-employee-foundation-s1-implementation`
- Gate: `S1 implementation` (approved slice S1 only; author evidence; not a self-review)
- Work unit: `PA2-v002-identity-organization-employee-foundation`
- Agent: controller-dispatched implementation-author (provider `mimo-v2.6-flash`, model `mimo-v2.6-flash`)
- Date: 2026-10-27
- Binding plan checkpoint: `6f14f7fdf6e69960452913631b419bda75071fc8`
- Binding goal: PA2 V002-only goal contract in `docs/gateflow/project-execution-control.md` (unchanged)
- Revised plan (Plan-fix 1): `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md`, SHA-256 `FF2E9597DC0D9C2AC1772DEAD16726251765CC53FB2E8B5058F6EC5529EA4C0A`
- This artifact is evidence written by the S1 author. It does **not** accept the checkpoint, does **not** perform S1 review, and does **not** enter S2/S3.

## 1. Scope and stop statement

Implemented and validated exactly the approved S1 slice only:

1. Author the complete frozen V002 SQL migration once.
2. Prove on real PostgreSQL 16 that Flyway applies V001 then V002.
3. Prove the exact catalog/ACL/guard contract (including F-PA2-PR-01 34-check deparse inventory).
4. Prove valid runtime-role inserts across all six tables succeed and roll back.
5. Rename two container S1 methods; keep container suite at exactly 12 methods; keep default surefire at exactly 10 tests.
6. Update CI expected method names and backend README for V001+V002 S1.

**Did not:** commit, stage, push, open a PR, merge, self-review, enter S2/S3, edit V001, edit pom.xml, edit application Java, edit V901 fixture, edit `docs/12`, edit `docs/14`, edit the control document, add roles/schemas/V003+, add service functions beyond the six guards, widen privileges, invent Q2/Q17/Q23.

## 2. Writable paths (exactly five)

| # | Path | Action | SHA-256 (final written content) |
| --- | --- | --- | --- |
| 1 | `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | new | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| 2 | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited (S1 implementation; revised by S1 fix 1) | `f3f530924ab626babd662f44ee4f68b0d9282915a81e75dafb725b375357be5b` (post-fix final; the pre-fix S1 hash `23267f0cc289260ded268ce75bd811f02c3774a20447be1d8f69b8d68f8b81e6` is superseded and recorded only as history in `pa2-v002-identity-organization-employee-foundation-s1-fix-1.md`) |
| 3 | `.github/workflows/ci.yml` | edited (two S1 method names in failsafe expected set only) | `9396aedd64bb60f394dcb3776c6514fb6394653ca1672fda01570deb132626c7` |
| 4 | `backend/README.md` | edited (V001+V002 S1 description; 10/12 counts unchanged) | `07125182763b1ce2cffe0cb2d8bda610ef2190f6b1633afca6c42845dcd1c984` |
| 5 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md` | new (this artifact; corrected and extended by S1 fix 1) | recorded in the S1 fix 1 task report close-out |

Not written by this task: V001, `backend/pom.xml`, application Java, `V901` broken fixture, `docs/12`, `docs/14`, control document, plan/reviews.

Note: `git status` also showed `docs/gateflow/project-execution-control.md` as modified. That change was **not** produced by this S1 author (controller-owned; left untouched).

## 3. Environment and toolchain

| Component | Observed value |
| --- | --- |
| Host | Linux WSL2 6.18.33.2-microsoft-standard-WSL2, Docker Server 29.1.3, API 1.52 |
| JDK | Temurin `21.0.13+9` at `/opt/jdk21` (`SEMANTIC_VERSION="21.0.13+9"`, `IMPLEMENTOR="Eclipse Adoptium"`) |
| Maven | Apache Maven `3.9.9` at `/opt/maven-3.9.9` |
| Spring Boot | `4.1.2` (parent BOM) |
| Testcontainers | `2.0.5` |
| PostgreSQL image | floating tag `postgres:16` → observed server **PostgreSQL 16.15** |
| Flyway | resolved `12.4.0` (Boot BOM; test scope only) |
| PostgreSQL JDBC | `42.7.14` (Boot BOM; test scope only) |

`./mvnw` is CRLF and fails with `bad interpreter` in this WSL shell; validation used `/opt/maven-3.9.9/bin/mvn` with `JAVA_HOME=/opt/jdk21`. Windows interop `java.exe` cannot see `/mnt/c` classpath paths (`ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`), which is why a Linux JDK was required.

## 4. Migration summary (V002)

Order per plan §6.1:

1. `SET LOCAL ROLE customer_iam_owner` → three tables + indexes → `RESET ROLE`
2. `SET LOCAL ROLE organization_access_owner` → two tables + indexes → `RESET ROLE`
3. `SET LOCAL ROLE operations_iam_owner` → one table + indexes → `RESET ROLE`
4. Migration-identity `ALTER TABLE ... ADD CONSTRAINT` for the four ordinary immediate FKs
5. Per-owner blocks: six local guard functions + triggers, `REVOKE ALL ... FROM PUBLIC`, exact owned/cross-module `GRANT`s, `RESET ROLE`

Objects:

| Kind | Count | Notes |
| --- | --- | --- |
| Tables | 6 | `customer_iam.customer_account`, `auth_identity`, `identity_profile`; `organization_access.organization`, `organization_membership`; `operations_iam.employee_account` (accepted order) |
| Named checks | 34 | 4 / 7 / 7 / 4 / 4 / 8; only `ck_identity_profile_state_shape` (no duplicate `verified_shape`) |
| PKs | 6 | one per table |
| Unique constraints | 4 | `uq_auth_identity_subject`, `uq_identity_profile_account`, `uq_employee_login_hmac`, `uq_employee_issuer_subject` |
| Indexes (incl. partial) | 18 | includes unique partial `uq_membership_live` (`CREATE UNIQUE INDEX ... WHERE status <> 'REVOKED'`) and non-unique partial `ix_employee_enabled_admin` (`WHERE role = 'ADMIN' AND status = 'ACTIVE'`) |
| FKs | 4 | ordinary immediate, `ON UPDATE RESTRICT ON DELETE RESTRICT`; created by migration identity after all parent tables |
| Guard functions/triggers | 6 / 6 | SECURITY INVOKER, owner, `<own schema>, pg_temp` search_path, stable P0001 errors; auth-identity rotation tuple requires both ciphertext+hmac change with `NEW.key_version = OLD.key_version + 1` |
| Sequences / V003+ / `issue_first_trial` | 0 | fail-closed asserted |
| `id` defaults on V002 tables | 0 | IDs are application-supplied UUIDs |

Predicates were taken verbatim from `docs/14` 4.6–4.11. No hand-authored “equivalent” predicate. No application service function, no schema/role creation beyond the frozen V001 role set, no `SECURITY DEFINER` guards.

## 5. Test evolution (12 / 16 / 18 preserved)

| Method count | Profile | Result |
| --- | --- | --- |
| surefire default | `clean verify` (no Docker) | exactly **10** tests, 0 skipped / failures / errors |
| container S1 | `-Pcontainer-smoke` | **2** methods (renamed; no new `@Test`) |
| container S1+S2+S3 | `-Pcontainer-smoke` | exactly **12** methods, 0 skipped / failures / errors |

Renamed S1 methods (inside `PostgresContainerIT` only):

| Before (PA1) | After (PA2 V002 S1) |
| --- | --- |
| `v001MigratesCleanPostgres16WithoutSkipping` | `v001ThenV002MigrateCleanPostgres16WithoutSkipping` |
| `v001InventoryMatchesTheFrozenContract` | `v001AndV002InventoryMatchesTheFrozenContract` |

`brokenMigrationFailsVisibly` and all S2/S3 methods are behavior-preserving.

### 5.1 S1 migrate test

`v001ThenV002MigrateCleanPostgres16WithoutSkipping`:

- `migrationsExecuted == 2`
- history rows: `001` / `platform schemas roles idempotency outbox audit` / `V001__platform_schemas_roles_idempotency_outbox_audit.sql` and `002` / `identity organization and employee` / `V002__identity_organization_and_employee.sql`, both `success=true`
- `flyway.validate()` passes; `info().pending()` empty; repeated migrate is zero
- V001 function smoke retained (granted-role calls then rollback)
- `exerciseV002InsertsAsOwningRuntimeRolesAndRollBack()`: one transaction, `SET LOCAL ROLE` per owning runtime role, one insert into each of the six tables (valid `v1.` ciphertext, 64-hex HMACs), then `rollback`; asserts zero residual rows for all six IDs

### 5.2 S1 inventory test (F-PA2-PR-01)

`v001AndV002InventoryMatchesTheFrozenContract` calls `assertInventory`, which includes:

- exact schemas/owners/roles (V001 baseline retained)
- nine tables / ordered column-name maps / 34 check names / FK defs / zero sequences / exact table owners
- **S1 fix 1 (finding 1):** exact per-column rows `(column_name, data_type, is_nullable, column_default)` in ordinal order for all six V002 tables (6/10/9/6/7/9 columns) using canonical PG16 representations (text defaults `'ACTIVE'::text` / `'UNVERIFIED'::text`, integer default `1`, `now()`, absent default `<none>`), per-table defaulted-column counts, total V002 defaulted-column count 16 and `id`-default zero — exact row-set equality fails closed on same-named wrong type/nullability/default and on any missing, extra or reordered column
- **S1 fix 1 (finding 1):** exact six-PK and four-unique inventory keyed by schema/table/name/contype with whole PG16 `pg_get_constraintdef` equality (`PRIMARY KEY (id)` / exact `UNIQUE (...)` column sequences) plus `conkey`-ordered column-name equality; a same-named key on the wrong table, wrong contype or wrong column set/order fails closed
- **S1 fix 1 (finding 1):** exact whole `indexdef` equality for all 18 V002 indexes (6 PK, 4 unique-constraint, 6 named non-unique, unique partial `uq_membership_live`, non-unique partial `ix_employee_enabled_admin`), binding schema/table, name, ordered columns/expression, uniqueness and predicate; the 18-name list check fails on missing/extra indexes and the whole-string checks fail on column-order/uniqueness/predicate changes
- **new** `assertV002CheckPredicateInventory`: explicit static 34-entry map; total `contype='c'` rows in the three schemas == 34; per-table exact name set; per-check exactly one row and **whole-definition equality** after a normalizer that only trims and collapses ASCII whitespace (`[ \t\r\n]+` → one space). No substring / contains / prefix / regex / semantic-equivalence matching. Missing name, extra check, or definition mismatch fails closed with an uncaught `AssertionError`.
- exact trigger matrix (7 triggers: 1 V001 audit + 6 V002 guards; BEFORE / ROW / event bits / expected function)
- exact owned/cross-module grants: table-level SELECT+INSERT only on owned tables; table-level UPDATE false (column-level UPDATE only); DELETE/TRUNCATE/REFERENCES/TRIGGER false for all non-owner roles; exact UPDATE columns true and all other columns false; cross-module exact column SELECT true and table-level SELECT/INSERT/UPDATE/DELETE/TRUNCATE false; PUBLIC zero on schemas/tables/functions; guard EXECUTE only to the owning runtime role (+ implicit owner EXECUTE excluded as non-grant)
- **S1 fix 1 (finding 2):** complete explicit expected effective-privilege matrix over all 15 S1 roles × all six V002 tables × table-level and column-level SELECT/INSERT/UPDATE: table-owner rows model inherent privileges (all true) separately from explicit ACL; owning runtime-role rows are table-level SELECT/INSERT true and table-level UPDATE false with exact allowed update columns true and every other column false; the five cross-module pairs are table-level SELECT/INSERT/UPDATE false with only their exact column SELECT grants true and every other column false; every other role/table pair is all-false with no column-level leakage, so an undeclared GRANT to `operations_iam_role`, a platform role or another owner-role on the wrong table fails closed. The existing all-role DELETE/TRUNCATE/REFERENCES/TRIGGER negatives are retained.
- `functionsHaveFixedSearchPathAndNoTransactionControl` extended with the six guard rows (`prosecdef=false`, owner, ordered `search_path=<schema>, pg_temp`, zero transaction-control matches, zero `gen_random_uuid` in guards) while retaining V001 SECURITY DEFINER assertions and the `issue_first_trial` zero check

## 6. F-PA2-PR-01 — 34-check deparse inventory (docs/14 source → raw PG16 deparse)

Expected literals are PostgreSQL 16 `pg_get_constraintdef` strings captured from a real `postgres:16` container after V001+V002 applied in one transaction, then frozen as test literals. `docs/14` 4.6–4.11 remains the authoritative predicate source; a deparse that disagrees with `docs/14` is not valid evidence (fix RF-01 / re-review RSK-RPR-01). The table below renders the raw PG16 deparse strings; regex rows show actual single backslashes (e.g. `^v1\.[A-Za-z0-9._:/-]+\..+$`), matching live `pg_get_constraintdef` output on PostgreSQL 16.15 (S1 fix 1, finding 3).

| # | docs/14 source | Constraint | Frozen PG16 `pg_get_constraintdef` |
| --- | --- | --- | --- |
| 1 | 4.6 customer_account | `ck_customer_account_display_name` | `CHECK ((((length(display_name) >= 1) AND (length(display_name) <= 200)) AND (btrim(display_name) <> ''::text)))` |
| 2 | 4.6 customer_account | `ck_customer_account_status` | `CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text])))` |
| 3 | 4.6 customer_account | `ck_customer_account_revision` | `CHECK ((revision >= 1))` |
| 4 | 4.6 customer_account | `ck_customer_account_time_order` | `CHECK ((updated_at >= created_at))` |
| 5 | 4.7 auth_identity | `ck_auth_identity_provider` | `CHECK ((provider = ANY (ARRAY['WECHAT'::text, 'PHONE'::text])))` |
| 6 | 4.7 auth_identity | `ck_auth_identity_scope` | `CHECK ((((length(provider_scope) >= 1) AND (length(provider_scope) <= 128)) AND (btrim(provider_scope) <> ''::text)))` |
| 7 | 4.7 auth_identity | `ck_auth_identity_ciphertext` | `CHECK ((provider_subject_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'::text))` |
| 8 | 4.7 auth_identity | `ck_auth_identity_hmac` | `CHECK ((provider_subject_hmac ~ '^[0-9a-f]{64}$'::text))` |
| 9 | 4.7 auth_identity | `ck_auth_identity_status` | `CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text])))` |
| 10 | 4.7 auth_identity | `ck_auth_identity_key_version` | `CHECK ((key_version >= 1))` |
| 11 | 4.7 auth_identity | `ck_auth_identity_revision` | `CHECK ((revision >= 1))` |
| 12 | 4.8 identity_profile | `ck_identity_profile_status` | `CHECK ((real_name_status = ANY (ARRAY['UNVERIFIED'::text, 'VERIFIED'::text])))` |
| 13 | 4.8 identity_profile | `ck_identity_profile_legal_ciphertext` | `CHECK (((legal_name_ciphertext IS NULL) OR (legal_name_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'::text)))` |
| 14 | 4.8 identity_profile | `ck_identity_profile_id_ciphertext` | `CHECK (((id_number_ciphertext IS NULL) OR (id_number_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'::text)))` |
| 15 | 4.8 identity_profile | `ck_identity_profile_id_hmac` | `CHECK (((id_number_hmac IS NULL) OR (id_number_hmac ~ '^[0-9a-f]{64}$'::text)))` |
| 16 | 4.8 identity_profile | `ck_identity_profile_verification_ref` | `CHECK (((verification_ref_ciphertext IS NULL) OR (verification_ref_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'::text)))` |
| 17 | 4.8 identity_profile | `ck_identity_profile_state_shape` | `CHECK ((((real_name_status = 'UNVERIFIED'::text) AND (legal_name_ciphertext IS NULL) AND (id_number_ciphertext IS NULL) AND (id_number_hmac IS NULL) AND (verification_ref_ciphertext IS NULL) AND (verified_at IS NULL)) OR ((real_name_status = 'VERIFIED'::text) AND (legal_name_ciphertext IS NOT NULL) AND (id_number_ciphertext IS NOT NULL) AND (id_number_hmac IS NOT NULL) AND (verification_ref_ciphertext IS NOT NULL) AND (verified_at IS NOT NULL))))` |
| 18 | 4.8 identity_profile | `ck_identity_profile_revision` | `CHECK ((revision >= 1))` |
| 19 | 4.9 organization | `ck_organization_display_name` | `CHECK ((((length(display_name) >= 1) AND (length(display_name) <= 200)) AND (btrim(display_name) <> ''::text)))` |
| 20 | 4.9 organization | `ck_organization_status` | `CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'SUSPENDED'::text, 'DISABLED'::text])))` |
| 21 | 4.9 organization | `ck_organization_revision` | `CHECK ((revision >= 1))` |
| 22 | 4.9 organization | `ck_organization_time_order` | `CHECK ((updated_at >= created_at))` |
| 23 | 4.10 organization_membership | `ck_membership_role` | `CHECK ((role = ANY (ARRAY['ADMINISTRATOR'::text, 'MEMBER'::text, 'EVIDENCE_MANAGER'::text, 'AUDITOR'::text])))` |
| 24 | 4.10 organization_membership | `ck_membership_department` | `CHECK (((department_label IS NULL) OR (((length(department_label) >= 1) AND (length(department_label) <= 200)) AND (btrim(department_label) <> ''::text))))` |
| 25 | 4.10 organization_membership | `ck_membership_status` | `CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'SUSPENDED'::text, 'REVOKED'::text])))` |
| 26 | 4.10 organization_membership | `ck_membership_revision` | `CHECK ((revision >= 1))` |
| 27 | 4.11 employee_account | `ck_employee_login_hmac` | `CHECK ((login_name_hmac ~ '^[0-9a-f]{64}$'::text))` |
| 28 | 4.11 employee_account | `ck_employee_login_masked` | `CHECK ((((length(login_name_masked) >= 1) AND (length(login_name_masked) <= 128)) AND (btrim(login_name_masked) <> ''::text)))` |
| 29 | 4.11 employee_account | `ck_employee_issuer` | `CHECK ((((length(auth_issuer) >= 1) AND (length(auth_issuer) <= 128)) AND (btrim(auth_issuer) <> ''::text)))` |
| 30 | 4.11 employee_account | `ck_employee_subject_hmac` | `CHECK ((auth_subject_hmac ~ '^[0-9a-f]{64}$'::text))` |
| 31 | 4.11 employee_account | `ck_employee_display_name` | `CHECK ((((length(display_name) >= 1) AND (length(display_name) <= 200)) AND (btrim(display_name) <> ''::text)))` |
| 32 | 4.11 employee_account | `ck_employee_role` | `CHECK ((role = ANY (ARRAY['ADMIN'::text, 'OPS'::text])))` |
| 33 | 4.11 employee_account | `ck_employee_status` | `CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text])))` |
| 34 | 4.11 employee_account | `ck_employee_revision` | `CHECK ((revision >= 1))` |

Alias note (plan R-01 / migration comment): `docs/14` 4.8 may abbreviate the canonical named check as `ck_identity_profile_verified_shape`; that alias is **not** a separate constraint. Only `ck_identity_profile_state_shape` exists.

Normalization samples: the expected map was frozen from the live deparse with no non-whitespace rewrite. `normalizeCheckDef` is applied only as trim + collapse of ASCII whitespace runs; both container runs (≥2) matched the frozen literals without any non-formatting stop-and-escalate condition.

## 7. Required validations performed

| Check | Command / method | Outcome |
| --- | --- | --- |
| Toolchain pins | `java -version`, `mvn -v` | 21.0.13+9 / Maven 3.9.9 |
| Docker | `docker info` | OK (Server 29.1.3) |
| Default `clean verify` | `JAVA_HOME=/opt/jdk21 mvn -B -ntp clean verify` | BUILD SUCCESS; surefire **10/0/0/0** |
| Container smoke run 1 | `mvn -B -ntp -Pcontainer-smoke verify` | BUILD SUCCESS; failsafe **12/0/0/0**; PostgreSQL **16.15**; applied 001 then 002 |
| Container smoke run 2 | same command (determinism) | BUILD SUCCESS; failsafe **12/0/0/0** |
| Failsafe method set | Python XML parse | exact 12 names; includes both renamed S1 methods; `skipped=0 failures=0 errors=0` |
| Boot smoke | `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | exit `0`; log contains `Started OneCallApplication` |
| Fat-jar forbidden-library firewall | `jar tf` + grep `BOOT-INF/lib/(spring-security\|flyway\|jooq\|HikariCP\|postgresql\|testcontainers)` | FIREWALL_OK (no match) |
| `pom.xml` / `BootstrapDependencyBoundaryTests` | unchanged files | not written; surefire still 10 including that test |
| V001 HEAD blob SHA-256 | Windows `git cat-file blob HEAD:...V001...` content SHA-256 | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` (**matches** controller expected) |
| V001 worktree LF-normalized SHA-256 | Python CRLF→LF | same `946A0A13...` (content unchanged; on-disk remains CRLF baseline 25597 bytes / 536 CR / raw SHA-256 `96E72BDACB32610DD03975E3E4FD6B828579B574A375E4241482F19C96B4E1C6`) |
| V001 git status | Windows git | V001 not listed as modified |
| V002 SHA-256 | `sha256sum` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` (352 lines / 16447 bytes; LF; final newline; 0 CR; 0 trailing whitespace; no conflict markers) |
| Scoped CR-aware whitespace | Windows `git -c core.whitespace=cr-at-eol diff --check` on modified tracked paths + `--no-index --check` for untracked V002 | zero diagnostics (exit content-only for `--no-index`); encoding: UTF-8/LF final newline/0 trailing whitespace on the four content files |
| Smoke residue | probe container `v002probe` removed; failsafe/surefire only under `target/` | no leftover probe DB |
| CI contract | `.github/workflows/ci.yml` | only the two expected S1 method names updated; count remains 12; no new job/step |

### 7.1 Windows git / CRLF environment limitation (reported accurately)

WSL `git` cannot resolve this worktree’s `.git` pointer (`gitdir: D:/codex-call/.git/worktrees/codex-call2`, a Windows path). Therefore:

- V001 HEAD blob hash, `git status`, and scoped `diff --check` were run via `powershell.exe` + Windows Git at `C:\Users\Administrator\.codex\worktrees\pa2-v002-identity-foundation\codex-call`.
- HEAD at validation time: `6f14f7fdf6e69960452913631b419bda75071fc8` (accepted plan commit).
- V001 blob object id: `d7e2e6879701a5db6517434cbb9b22d5c8f2d707`.
- Line endings were not normalized; V001 remains the CRLF checkout baseline.

### 7.2 S1 fix 1 revalidation (2026-10-27; supersedes the pre-fix hash/results above)

After the accepted S1 fix 1 (findings 1-3 in `docs/reviews/code-review-20261027-162020.md`) extended the IT catalog/ACL inventory and corrected this evidence document, the complete validation suite was rerun with the same pins (Temurin `21.0.13+9` at `/opt/jdk21`, Maven `3.9.9` at `/opt/maven-3.9.9`, Docker Server `29.1.3`, floating `postgres:16` observed as PostgreSQL `16.15`, Flyway `12.4.0`):

| Check | Outcome |
| --- | --- |
| Default `clean verify` | BUILD SUCCESS; surefire **10/0/0/0** (XML-parsed) |
| Container smoke run 1 | BUILD SUCCESS; failsafe **12/0/0/0**; PostgreSQL 16.15; `Successfully validated 2 migrations` |
| Container smoke run 2 | BUILD SUCCESS; failsafe **12/0/0/0**; PostgreSQL 16.15 |
| Failsafe method set | exact 12 names, unchanged; no `@Test` added (`@Test` count = 12) |
| Disposable mutation/GRANT probes (transaction + rollback) | wrong type/default/nullability, wrong unique columns, wrong index order and undeclared GRANTs to `operations_iam_role`/`platform_reliability_role` each flipped the frozen evidence (`t` = detected); post-rollback residue checks all back to frozen values; probe containers removed |
| Boot smoke | `Started OneCallApplication` present |
| Fat-jar firewall | FIREWALL_OK (27 `BOOT-INF/lib/` items; no forbidden library) |
| V001 HEAD blob SHA-256 | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` (matches frozen value; V001/pom.xml/`BootstrapDependencyBoundaryTests` content diff vs HEAD = 0) |
| V002 SHA-256 | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` (**unchanged** by the fix) |
| Postfix `PostgresContainerIT` SHA-256 | `f3f530924ab626babd662f44ee4f68b0d9282915a81e75dafb725b375357be5b` (final; pre-fix `23267f0c…` superseded) |
| Scoped CR-aware whitespace (`git -c core.whitespace=cr-at-eol diff --check`) | zero diagnostics on the fix-touched paths; IT remains uniform CRLF |

The pre-fix rows in §7 and the pre-fix IT hash in §2 are retained only as S1-implementation history; the final executable evidence for the corrected slice is this §7.2 plus the fix artifact `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md`.

## 8. Deviations and blockers

None.

During container-smoke bring-up, three assertion-calibration fixes were applied **inside the writable IT only** (migration SQL unchanged):

1. Owned tables grant column-level UPDATE only → table-level UPDATE expectation set to `false` (column privileges asserted separately as `true`).
2. Table owners implicitly retain DELETE/TRUNCATE/REFERENCES/TRIGGER → those false-assertions skip the owning table owner (runtime roles still fail-closed).
3. Function owners implicitly retain EXECUTE → guard EXECUTE negative loop skips the owning function owner (PUBLIC and all non-owner roles still `false`; owning runtime role still `true`).

No plan predicate, success signal, method count, or scope boundary changed as a result.

## 9. Residual risks carried into S1 review

| ID | Residual | Classification | Owner | Destination |
| --- | --- | --- | --- | --- |
| RSK-S1-01 | Expected deparse literals were captured from the applied migration; S1 review must confirm each entry against `docs/14` semantics, not only against the migration (fix RF-01). | Test-authoring / review-discipline | Backend/QA reviewer | S1 independent review of §6 table and `V002_CHECK_DEF_MAP` |
| RSK-S1-02 | PG16 check deparse formatting may vary across minor images (plan R-12 / RF-02). Two container runs matched; non-whitespace drift is stop-and-escalate. | Test-only fail-closed | Backend/QA | Keep whitespace-only normalizer |
| RSK-S1-03 | Floating `postgres:16` tag (plan R-11 / RF-03). Observed 16.15. | Inherited | Platform/CI | Supply-chain hardening; no digest guess |
| RSK-S1-04 | Q2 / Q17 / Q23 remain deferred and were not guessed. | Deferred input | Security/Platform/Database | Later design work units |
| RSK-S1-05 | Service-owned login/bind/membership/last-admin ordering and audit-append remain outside V002. | Deferred behavior | Application services | Later service slices |
| RSK-S1-06 | `docs/gateflow/project-execution-control.md` shows as modified in the worktree by a non-S1 writer. | Environment / controller | Controller | Controller re-owns that file; S1 did not touch it |

## 10. Explicit non-actions

- No commit, stage, push, PR, merge.
- No self-review and no S1 gate acceptance.
- No S2 / S3 slice entry.
- No edit to V001, pom, application sources, V901 fixture, `docs/12`, `docs/14`, control document, or plan/reviews.
- No V003+ object, role, schema, service function, or Q2/Q17/Q23 guess.

## 11. Stop statement

S1 implementation and author validation are complete. This task stops here and reports to the controller for independent S1 review.
