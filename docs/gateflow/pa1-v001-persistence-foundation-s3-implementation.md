# PA1 V001 Persistence Foundation — S3 Implementation (Author-Complete; Ready for Independent Code Review)

- Task label: `pa1-v001-s3-impl-ds-20261027-01`
- Work unit: `PA1-v001-persistence-foundation`, behavior slice S3 (final approved V001 slice)
- Gate: `S3 implementation`
- Author/provider: DeepSeek (`ds`)
- Date: 2026-10-27
- Base: worktree HEAD `8c7f4bbde1e849aa6a6b54a402a0eae943fb6fb2` (`gateflow: record PA1 S2 checkpoint`); accepted S2 checkpoint `96e8134cf69266b70aa28f400eee12fc51d37ffe`; accepted S1 checkpoint `03e8b916058a79e1913fb4708a146c6c3326dc42`
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa1-v001-persistence-foundation/codex-call`
- Artifact: `docs/gateflow/pa1-v001-persistence-foundation-s3-implementation.md`
- Status: **S3 author-complete and ready for independent code review. S3 is not accepted.** No commit, push, PR, merge, code review, control-document change or aggregate work was performed. GitHub Actions has not run. No production acceptance or production provisioning is claimed.

## 1. Binding goal and scope

S3 completes the approved final behavior-proof slice over the already-authored V001 migration. It adds executable PostgreSQL 16 evidence for audit append-only enforcement, audit reason/denylist rules, fixed function `search_path` and absence of transaction control, exact owner/`PUBLIC`/role execution boundaries, and hostile-caller-`search_path` resistance. It adds no domain object, migration, runtime DataSource/Flyway, API, queue/dispatcher, cloud resource or production provisioning.

Binding inputs read:

- `docs/gateflow/project-execution-control.md`: current PA1 section, binding goal contract, slice principle, accepted S1/S2 checkpoints, F-PA1-PR-03 and the slice-CI clarification.
- `docs/gateflow/pa1-v001-persistence-foundation-plan.md`: §§5.6, 6.3, 7.1-7.3, 8-10.
- `docs/reviews/plan-review-20261027-093407.md`: F-PA1-PR-01, F-PA1-PR-02 and F-PA1-PR-03.
- `docs/14-production-data-model-and-flyway-schema-spec.md`: §§2.1, 2.2, 2.6, 2.8, 4.5, 9.0, 9.3, 10.1-10.8, 10.10, 11.3 and the V001 verification requirements.
- Accepted S1/S2 implementation and review artifacts, the frozen V001 SQL, the existing `PostgresContainerIT`, `backend/README.md`, `.github/workflows/ci.yml` and `backend/pom.xml`.
- Repository instruction files: no `AGENTS.md` or `CLAUDE.md` is present in the repository root.

F-PA1-PR-03 is honored: the shared container is retained, every row count/read is scoped to a method-local key or object ID, Flyway/system metadata remains excluded from inventory assertions, the audit append-only table is never truncated by a test (the only `TRUNCATE` statements are denied probes), and all temporary S3 objects are unique and removed in `finally`.

## 2. Base checkpoints and frozen hashes

| Item | Value |
| --- | --- |
| Worktree HEAD / controller record | `8c7f4bbde1e849aa6a6b54a402a0eae943fb6fb2` |
| Accepted S2 checkpoint | `96e8134cf69266b70aa28f400eee12fc51d37ffe` |
| Accepted S1 checkpoint | `03e8b916058a79e1913fb4708a146c6c3326dc42` |
| Accepted plan SHA-256 | `B3EF442258BD09A01435C292D5077B1C51C657A34FB71A7C880A55FA9693234A` |
| Accepted plan-review SHA-256 | `E171524399E1B65AEA5E3F16BC3098879A2B8C0D98592E311F354EC9A06F0423` |
| V001 accepted-through-S2 SHA-256 | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| V001 SHA-256 after S3 | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` (unchanged) |
| `backend/pom.xml` SHA-256 | `fbc2d9ecedc2dac942c3c460b1bfc17049d4cb5ffbdda39c7b1496ebc6ddfda5` (unchanged) |
| `migration-broken/V901__pa1_broken_migration_fixture.sql` SHA-256 | `fb3b83b384f1ccf3c9bf7ed9c60e6c3413cc987dd2035dd9d062c698cf287923` (unchanged) |

## 3. Exact changed paths and SHA-256

Only the allowed S3 paths were modified or created:

| Path | SHA-256 |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `fc094f51b1907d226adfce06906c59d692e6126c89b37e21b7c4046b518d17ff` |
| `.github/workflows/ci.yml` | `8fc26dada5a8ed031ee2b774141a0a40622b8e718b8e2d59ae11f9279a631061` |
| `backend/README.md` | `a2753e062fb8d96be8c8cffb4419d9aad9b9c3b875be2902c185f3a6159c46a9` |
| `docs/gateflow/pa1-v001-persistence-foundation-s3-implementation.md` | This artifact; its own final SHA-256 is reported in the completion response and controller intake because a file cannot contain its own stable hash. |

The frozen V001 migration, `backend/pom.xml`, the V901 broken-migration fixture, `docs/14`, the accepted plan/review/S1/S2 artifacts, the control document, all production Java sources and all other paths are unchanged. Windows `git status --porcelain` before this artifact reported exactly the first three modified paths and no untracked path.

## 4. V001 change decision

**V001 was not changed.** No executable S3 test proved the frozen V001 contract defective. All required S3 behaviors are produced by the existing append-only trigger, function validation, grants and fixed `search_path`; the follow-the-contract tests passed against the accepted V001 without correction. The V001 SHA-256 therefore remains `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`.

## 5. Final test surface: exactly 12 approved container methods

The failsafe suite contains exactly the accepted 3 S1 methods, the accepted 4 S2 methods and the 5 approved S3 methods. Every accepted S1/S2 assertion and behavior is preserved; the S3 change adds test methods and helpers only, plus the stale class-level Javadoc correction.

| # | Slice | Method |
| --- | --- | --- |
| 1 | S1 | `v001MigratesCleanPostgres16WithoutSkipping` |
| 2 | S1 | `v001InventoryMatchesTheFrozenContract` |
| 3 | S1 | `brokenMigrationFailsVisibly` |
| 4 | S2 | `idempotencyNewReplayConflictAndFinalizeOnce` |
| 5 | S2 | `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow` |
| 6 | S2 | `concurrentIdempotencyRetryObservesOneWinner` |
| 7 | S2 | `outboxReplayConflictRollbackAndDenylist` |
| 8 | S3 | `auditIsAppendOnlyAndRuntimeWritesAreDenied` |
| 9 | S3 | `auditReasonAndDenylistRulesAreEnforced` |
| 10 | S3 | `functionsHaveFixedSearchPathAndNoTransactionControl` |
| 11 | S3 | `ownerPublicAndRoleExecutionBoundariesAreExact` |
| 12 | S3 | `hostileSearchPathCannotRedirectFunctionEffects` |

No placeholder, `@Disabled`, `Assumptions` or environment-variable skip was added.

## 6. S3 behavior and security evidence

### 6.1 `auditIsAppendOnlyAndRuntimeWritesAreDenied`

- `subject_number_role` appends through `audit.append_fact` and receives a non-null audit id; `audit_reader_role` can read the row inside the same transaction.
- The caller transaction rolls back; the row disappears (scoped `object_id` count becomes zero).
- Direct `INSERT` into `audit.audit_fact` as `customer_iam_role` fails with SQLSTATE `42501`; no row exists afterward.
- `audit_reader_role` `UPDATE`, `DELETE` and `TRUNCATE` each fail with SQLSTATE `42501`.
- A row is inserted as `audit_owner` inside an explicit transaction, then `UPDATE` and (separately) `DELETE` as `audit_owner` each raise stable `AUDIT_APPEND_ONLY` with SQLSTATE `P0001`; the aborted transaction rolls back the probe row. The owner is therefore protected by the trigger, not only by grants.
- All audit assertions use method-local object UUIDs; the append-only table is never truncated and no global audit row count is asserted.

### 6.2 `auditReasonAndDenylistRulesAreEnforced`

- `EMPLOYEE` actor without a reason raises `AUDIT_REASON_REQUIRED`/`P0001`.
- High-risk `SERVICE` action `RISK_STATE_CHANGE` without a reason raises `AUDIT_REASON_REQUIRED`/`P0001`.
- A valid reason on an employee append succeeds; the reader sees exactly one method-local row before rollback, and the rollback removes it.
- A redacted snapshot containing the protected key `phone` raises `AUDIT_ARGUMENT_INVALID`/`P0001`; no row is written.
- An approved masked variant (`calleeNumberMasked`) succeeds; the reader sees exactly one method-local row before rollback, and the rollback removes it.
- Failure probes use explicit transactions; the shared `expectFailure` helper rolls back the aborted transaction before any further statement.

### 6.3 `functionsHaveFixedSearchPathAndNoTransactionControl`

- Catalog query over the four declared cross-schema functions proves, exactly and in order:
  - `audit.append_fact|true|audit_owner|search_path=audit, pg_temp`
  - `platform.idempotency_begin|true|platform_reliability_owner|search_path=platform, pg_temp`
  - `platform.idempotency_complete|true|platform_reliability_owner|search_path=platform, pg_temp`
  - `platform.outbox_append|true|platform_reliability_owner|search_path=platform, pg_temp`
- The `true` field is `prosecdef`, so all four are `SECURITY DEFINER`; the owner and the exact ordered `<owner schema>, pg_temp` `proconfig` are asserted without normalization.
- No declared function body matches transaction-control language. The assertion matches only `COMMIT`, `ROLLBACK`, `START TRANSACTION` and `SET TRANSACTION` with PostgreSQL word boundaries. Ordinary PL/pgSQL `BEGIN`/`END` block delimiters are intentionally excluded and do not create a false positive.
- `pg_catalog.gen_random_uuid()` appears in exactly the three inserting functions: `audit.append_fact`, `platform.idempotency_begin` and `platform.outbox_append` (`idempotency_complete` performs an update and is absent).
- `issue_first_trial` is absent from V001.

### 6.4 `ownerPublicAndRoleExecutionBoundariesAreExact`

- Every owner role (`platform_reliability_owner`, `audit_owner`, `customer_iam_owner`, `organization_access_owner`, `operations_iam_owner`, `subject_number_owner`) is `NOLOGIN`, `NOSUPERUSER`, `NOCREATEDB` and `NOCREATEROLE`.
- `PUBLIC` has zero ACL entries on the six business schemas plus `public`, on the three business tables, and on all platform/audit functions (including the trigger function), proving no schema `USAGE`/`CREATE`, table privilege or function `EXECUTE`.
- The declared roles hold their exact schema `USAGE` without `CREATE`; cross-schema non-grantees are explicitly denied.
- The exact `EXECUTE` sets are asserted for the three platform functions and `audit.append_fact`; reliability, dispatcher, audit-reader and the two audit-only service roles do not receive platform `EXECUTE`.
- Table privileges are asserted exactly: `platform_reliability_role` and `platform_dispatcher_role` have `SELECT` only on `platform.outbox_event`, `platform_reliability_role` has `SELECT` on `platform.idempotency_record`, `audit_reader_role` has `SELECT` on `audit.audit_fact`, and no runtime role has `INSERT`, `UPDATE`, `DELETE` or `TRUNCATE` on any business table.
- Dispatcher column grants hold exactly for `status`, `attempt_count`, `available_at`, `published_at` and `revision`; all nine undeclared columns are denied.
- Live behavior: an Outbox row is created through `platform.outbox_append`, then `platform_dispatcher_role` updates exactly the five declared columns (status becomes `PUBLISHED`, attempt count 1, published time non-null, revision 2). Updating each of the nine undeclared columns, `DELETE` and `TRUNCATE` each fail with SQLSTATE `42501`; the committed row is unchanged afterward.
- The method-local Outbox row is deleted as the test superuser in `finally` (bounded cleanup); the row is not an append-only audit row.

### 6.5 `hostileSearchPathCannotRedirectFunctionEffects`

- A unique decoy schema (`pa1_decoy_<random>`) with a same-named `idempotency_record` table and `idempotency_begin(text,text,text)` function is created for the method only.
- A granted runtime role sets a hostile caller `search_path` to `<decoy>, pg_catalog` and calls the schema-qualified `platform.idempotency_begin`.
- The real function returns `NEW` at revision 1; the real `platform.idempotency_record` contains exactly the method-local key, while the decoy table remains empty. The fixed `SET search_path = platform, pg_temp` inside the `SECURITY DEFINER` function is therefore not redirected by caller state.
- `finally` drops the decoy schema with `CASCADE` and deletes the method-local real row; both cleanup statements are bounded.

## 7. Executed validation

Environment used exactly for every command:

```bash
export JAVA_HOME=/tmp/temurin-21.0.13
export PATH="$JAVA_HOME/bin:/mnt/d/tools/maven/bin:$PATH"
```

Versions: Temurin `21.0.13+9`, Maven `3.9.9`, Docker `29.1.3`, PostgreSQL `16.15`, Flyway `12.4.0`, Maven repository `/mnt/d/tools/m2`.

All commands were run from `backend/`.

| # | Command | Result |
| --- | --- | --- |
| 1 | `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 test-compile` | `BUILD SUCCESS`; 26 test sources compiled |
| 2 | `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 clean verify` | `BUILD SUCCESS`; surefire `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`; no container |
| 3 | `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify` (first complete run) | `BUILD SUCCESS`; surefire 10/10; failsafe `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` |
| 4 | `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify` (second complete run) | `BUILD SUCCESS`; surefire 10/10; failsafe `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` |
| 5 | `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | exit `0`; log contains `Started OneCallApplication` |
| 6 | `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar` + forbidden-pattern grep | 206 entries; no `spring-security`, `flyway`, `jooq`, `HikariCP`, `postgresql` or `testcontainers` under `BOOT-INF/lib` |
| 7 | `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 dependency:tree -Dincludes=org.flywaydb:*` | `flyway-core:12.4.0:test` and `flyway-database-postgresql:12.4.0:test`; no compile/runtime occurrence |
| 8 | Windows PowerShell `git diff --check` in this worktree | exit `0` (a line-ending conversion notice for the Java file; no whitespace error) |
| 9 | Windows PowerShell `git status --porcelain` | exactly the three implementation paths; no untracked path before this artifact |

Failsafe XML inspection after the second run: exactly one `TEST-*PostgresContainerIT.xml`, `tests=12`, `failures=0`, `errors=0`, `skipped=0`, and the 12 testcase names equal the set in section 5. Each container run launched a fresh `postgres:16` container (different mapped port), so the second run independently exercised the shared-container/order behavior.

The two container runs left no S3 temporary schema, decoy object or committed S3 audit row. The S2 rows created by the accepted S2 methods retain their accepted F-PA1-PR-03 method-local isolation model.

## 8. CI and docs decisions

- `.github/workflows/ci.yml`: only the failsafe report expectation changed. The expected set is now the exact 12 approved method names, the failure count remains compared against `len(expected)` (12), and the diagnostic text now names S3. Everything else is unchanged: `docker info` before Maven, `./mvnw -B -ntp -Pcontainer-smoke verify`, boot jar smoke, production fat-jar firewall, `contents: read` permissions, path filters and all contract-regression commands.
- `backend/README.md`: updated to state the complete S1+S2+S3 12-test V001 evidence, the five S3 methods, the 10-test default verify, the exact permissions/audit/search-path behavior, and the unchanged limitations: the Testcontainers superuser is a test fixture (not Q17/Q23 production provisioning), `postgres:16` digest is unpinned, GitHub Actions has not run for this change, and author/container evidence is not an accepted checkpoint or production acceptance.
- The stale S1-only class Javadoc was corrected to S1/S2/S3. Accepted S1/S2 assertions and semantics were not changed.
- No other documentation was added or modified. `docs/14`, the accepted plan/review/S1/S2 artifacts and the control document remain unchanged.

## 9. Residual risks with owner and destination

| ID | Residual / risk | Classification | Owner | Destination |
| --- | --- | --- | --- | --- |
| S3-R-01 | The hostile-`search_path` behavioral probe uses `platform.idempotency_begin` as the representative function; the other three functions have catalog-proven fixed `search_path`/owner/mode but no separate live hijack probe. The approved test matrix names only `idempotency_begin`. | Bounded test-only risk | Backend/QA | Later persistence-test hardening if broader behavioral coverage is wanted; not an S3 acceptance criterion. |
| S3-R-02 | The audit denylist success case uses an approved masked key; a single-key encrypted audit envelope success path is not separately appended in S3 (S2 covers Outbox encrypted-envelope rejection). | Bounded test-only gap | Backend/QA | Later persistence-test hardening; not an S3 acceptance criterion (the approved variant is masked/redacted). |
| S3-R-03 | `postgres:16` image digest remains unpinned. | Inherited PA0/PA1 residual | Platform/CI | Supply-chain hardening; no digest guess is made. |
| S3-R-04 | GitHub Actions has not run for this change. | Deferred verification | Platform/CI | PR gate; only a real run is evidence. |
| S3-R-05 | Testcontainers superuser and `SET ROLE` are test fixtures; production role provisioning, migration identity and `SECURITY DEFINER` creation remain Q17/Q23. | Deferred input | Platform/Database/Security | Deployment/security design; no production provisioning claim. |
| S3-R-06 | Shared-container order/isolation relies on method-local keys and `finally` cleanup (accepted F-PA1-PR-03 model). Two complete runs passed; a future test could still introduce an unscoped assertion. | Accepted-risk / test discipline | Backend/QA | Code review and future test discipline. |
| S3-R-07 | PostgreSQL-16-specific catalog/deparse assertions (`indexdef`, ACL and `prosrc` forms) remain version-sensitive. | Inherited bounded test risk | Backend/QA | Fail-closed test maintenance when the test PostgreSQL major changes. |
| S3-R-08 | Windows checkout reports LF-to-CRLF conversion for the edited Java file; `git diff --check` passes and repository content semantics are unchanged. | Repository line-ending configuration | Platform/CI | Optional `.gitattributes` normalization outside this slice. |

## 10. First-failure history

No failure occurred in the S3 implementation validation. `test-compile`, the first default `clean verify`, the first `-Pcontainer-smoke verify`, the second `-Pcontainer-smoke verify`, the Boot jar smoke, the fat-jar firewall, the Flyway scope check and the Windows `git diff --check` all passed on their first execution. No failing command was retried to conceal a failure, and no test was skipped.

## 11. Explicit limitations and status

- **Author-complete; S3 is not accepted.** Independent code review, any fixes/re-review and the accepted S3 checkpoint remain controller gates.
- The implementation did not perform code review, did not enter another gate, did not stage/commit/push, did not open or update a PR, did not merge, did not create a review artifact, and did not modify `docs/gateflow/project-execution-control.md` or any other control state.
- No V002-V009 migration, domain schema/table/API/UI, runtime DataSource/Flyway, queue/dispatcher, cloud resource, credential, production role provisioning or production acceptance was added or claimed.
- GitHub Actions has not run. Local Testcontainers/PostgreSQL 16 evidence is build/test evidence only.
- The only claimed container evidence is the exact 12-method failsafe suite on PostgreSQL `16.15` under the explicit `container-smoke` profile, with `tests=12`, `skipped=0`, `failures=0`, `errors=0`, plus the unchanged 10-test default surefire build.
