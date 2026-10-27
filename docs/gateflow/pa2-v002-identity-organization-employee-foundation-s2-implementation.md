# PA2 V002 Identity, Organization and Employee Foundation — S2 Implementation Evidence

- Task label: `pa2-v002-s2-impl-ds-20261027-01`
- Work unit: `PA2-v002-identity-organization-employee-foundation`
- Gate: `S2 implementation` (approved slice S2 only; author evidence; not a self-review)
- Slice: S2 — row lifecycle, local guards, identity collision and membership replacement
- Date: 2026-10-27
- Author/provider: controller-dispatched implementation author (DeepSeek implementation author)
- This artifact is evidence written by the S2 author. It does **not** accept the checkpoint, does **not** perform S2 review, and does **not** enter S3.

## 1. Scope and stop statement

Implemented and validated exactly the approved S2 slice:

1. Added exactly four container `@Test` methods, preserving the twelve S1 methods.
2. Proved six-table defaults, valid conditional revision updates, stale zero-row updates, all 34 named checks (exact `23514` and exact constraint name), FK missing-parent/parent-delete RESTRICT (`23503`), and no id defaults/sequences.
3. Proved owner-role immutable-column and auth-rotation guards, runtime column-privilege denials (`42501`), and allowed mutable updates.
4. Proved identity same-account idempotent / different-account `IDENTITY_CONFLICT`, profile and employee uniqueness, membership live uniqueness, revoke/new-row regrant, revival conflict, multiple revoked rows and immutable pair.
5. Proved concurrent identity and live-membership unique collisions (commit winner / `23505` loser / read-classify plus rollback variant) with bounded two-connection races and FK-safe exact cleanup.
6. Updated only the CI expected failsafe set/count to 16 and the README S2 description/count to 16.

**Did not:** commit, stage, push, open a PR, merge, self-review, enter S3, edit V001/V002, edit `pom.xml`, edit application Java, edit `BootstrapDependencyBoundaryTests`, edit `docs/12`, `docs/14`, the control document, the plan/reviews or the S1 artifacts, add any migration/schema/role/grant/sequence, add a service function or generic trigger framework, or widen any privilege.

## 2. Binding inputs read and control-document status

Read at implementation time:

- `docs/gateflow/project-execution-control.md` — PA2 goal/current gate (read-only).
- `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md` §7.2, §8.1, §8.3-§8.7, §10-§12 (binding).
- `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan-fix-1.md` (F-PA2-PR-01 context).
- `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md` and the accepted S1 migration/tests/evidence.
- `docs/12-technology-stack-and-system-architecture.md` and `docs/14-production-data-model-and-flyway-schema-spec.md` sections referenced by the plan (4.6-4.11, 5.3, 9.4, 9.5, 9.9, 9.10, 10.8, 10.8.1, 11.3).
- `V001` and `V002` migrations, `PostgresContainerIT`, `pom.xml`, `.github/workflows/ci.yml`, `backend/README.md`.

The control document was **not modified** by this task. `git status` shows only the three allowed tracked edits plus the new artifact path below.

## 3. Base, prior checkpoint and frozen hashes

| Item | Value |
| --- | --- |
| Controller checkpoint HEAD | `207f130f3f35ff0750b638b9485a445d88920c02` |
| Accepted S1 implementation commit | `8d322814cf8d9aa36ce1d618222e39441afbd010` |
| V001 HEAD blob SHA-256 (re-verified) | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` |
| V001 HEAD-blob vs worktree CR-normalized diff | exit `0`, no content change |
| V002 frozen SHA-256 | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` |
| V002 SHA-256 before this task | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` |
| V002 SHA-256 after this task | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` |
| `pom.xml` / `src/main/java` / `BootstrapDependencyBoundaryTests` diff vs HEAD | exit `0` (unchanged) |

V001 is not edited. The worktree V001 file remains the CRLF checkout baseline (raw on-disk hash differs from the HEAD blob; the authoritative check is the HEAD blob hash plus the CR-normalized diff, per plan §8.7).

## 4. Changed paths, hashes and content-only diff manifest

Allowed tracked edits (uniform CRLF worktree; LF in the git index under `core.autocrlf=true`):

| # | Path | Action | On-disk SHA-256 (CRLF) | LF-normalized SHA-256 |
| --- | --- | --- | --- | --- |
| 1 | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited | `b0ab8051098ccaa5ec1d9517e22b563fdaf547ec866a714fa8fe3b65e12e1054` | `ac28b6526af3d21c3015cb47ce7ded0be0b66aee76aa15f03ed180b511cd8473` |
| 2 | `.github/workflows/ci.yml` | edited | `bbee0d37cd2ae59228ee3487311f263c3a7300ce7fe81b0c9b719ce6128e34bc` | `374c0ff67a0344e0bc82128bad02adc8c40eb930290b5651707e02d60ce8f4de` |
| 3 | `backend/README.md` | edited | `65ac1fcc0066d956b399f8cef8c68f489fcb698d6e158ba623418bbb6c5bdfb0` | `80c2961098cdf924735bd81bca724fac7b1023892d2fdb5ffe9451adf1d923ca` |
| 4 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s2-implementation.md` | new (this artifact, LF) | recorded in the task return report | recorded in the task return report |

Content-only diff manifest (`git diff --numstat` against HEAD):

| Path | Added | Deleted |
| --- | --- | --- |
| `.github/workflows/ci.yml` | 6 | 2 |
| `backend/README.md` | 6 | 5 |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | 1446 | 1 |

The single IT deletion is the `awaitFuture` signature line, genericized from `Future<BeginResult>` to `Future<T>` with no behavior change. The S1 `V002_CHECK_DEF_MAP` and all S1 assertions are otherwise untouched (additions only). No file outside the four allowed paths changed.

CRLF baseline accounting: no repository-wide line-ending normalization was performed. The three tracked files were already uniform CRLF in this Windows checkout (CR count equals LF count: IT 4736/4736, README 107/107, CI 190/190). Git stores LF (`i/lf`), so the content diff is line-ending-neutral. The new artifact is LF.

## 5. Scope implemented (methods and helpers)

Exactly four `@Test` methods added (the twelve S1 methods are preserved unchanged):

| Method | Evidence |
| --- | --- |
| `mutableHeadsEnforceLifecycleRevisionAndNamedChecks` | Six-table defaults (`ACTIVE`/`UNVERIFIED`, revision 1, timestamps), valid conditional updates (+1 revision), stale-revision zero rows/no change for all six tables; 34 named-check violating probes with exact `23514` and exact constraint name; FK missing-parent and parent-delete RESTRICT `23503`; no id default/sequence. |
| `localImmutabilityGuardsRejectRelationAndRotationChanges` | Owner-role probes for every immutable column with exact stable `P0001` errors; runtime column privileges deny immutable changes with `42501`; auth rotation one-sided/both-with-wrong-increment rejects and both-changed+one-increment succeeds atomically; allowed mutable updates succeed. |
| `identityCollisionAndMembershipRevokeRegrantSemantics` | Same-account idempotent and different-account `IDENTITY_CONFLICT` classification; `uq_identity_profile_account`; `uq_employee_login_hmac` and `uq_employee_issuer_subject`; `uq_membership_live`; revoke then new-row regrant; revoked-row revival conflict while live; multiple revoked rows; immutable pair guard. |
| `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner` | Two connections per scenario; identity commit race (one winner, loser `23505`, reads/classifies `IDENTITY_CONFLICT`), identity rollback race (loser inserts), membership commit race (one live winner, loser `23505`, reads winner), membership rollback race (loser inserts); exactly one committed winner per scenario; bounded lock-wait detection and exact cleanup. |

Bounded helpers only: savepoint/expected-failure helper, role switch, insert/query helpers, check-probe record, transactional profile-status isolation, runtime/owner immutability probes, identity classification, role-parameterized blocked-writer configuration, generic `awaitFuture<T>`, and FK-safe committed-row cleanup. No generic trigger framework, service function, new object or runtime dependency was added.

## 6. Test methods and CI expected set

- `@Test` count in `PostgresContainerIT.java`: **16**.
- Failsafe report methods: exactly the 16 S1+S2 names; set equality with `@Test` methods and with the CI `expected` set (verified by parsing the failsafe XML and the workflow text).
- CI change: four names appended to `expected`, test-count message label changed `S3`→`S2`; `len(expected)` now requires 16. No step, job, permission, path filter, toolchain pin, boot smoke or firewall step was removed or weakened.
- README change: title/intro and container count 12→16 plus the four PA2 V002 S2 method descriptions; toolchain pins, module-boundary table, firewall description and Modulith note unchanged.

## 7. Commands, environment and exact results

Environment observed:

| Component | Observed |
| --- | --- |
| Host | Linux WSL2 6.18.33.2-microsoft-standard-WSL2 |
| JDK | Temurin `21.0.13+9` at `/opt/jdk21` |
| Maven | Apache Maven `3.9.9` at `/opt/maven-3.9.9` |
| Docker | Docker Engine server `29.1.3` |
| PostgreSQL image | floating `postgres:16` → server **PostgreSQL 16.15** |
| Flyway | `12.4.0` (Boot 4.1.2 BOM, test scope) |
| Testcontainers | `2.0.5` |

All commands run from `backend/` with `JAVA_HOME=/opt/jdk21` and `/opt/maven-3.9.9/bin/mvn` (the CRLF `./mvnw` and Windows `java.exe` remain unusable in this WSL shell, as recorded in S1).

| Check | Command | Result |
| --- | --- | --- |
| Validate | `mvn -B -ntp validate` | exit `0`, `BUILD SUCCESS`; Enforcer rules pass |
| Default build | `mvn -B -ntp clean verify` | exit `0`; surefire **10** tests, `failures=0 errors=0 skipped=0` |
| Docker | `docker info` | exit `0`, server `29.1.3` |
| Full container suite | `mvn -B -ntp -Pcontainer-smoke verify` | exit `0`; failsafe **16** tests, `skipped=0 failures=0 errors=0`; applied V001→V002 (`Successfully applied 2 migrations ... now at version v002`); repeated migrate `No migration necessary`; PG **16.15** |
| Focused concurrency run 1 | `mvn -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner'` | exit `0`; failsafe **1** test, `skipped=0 failures=0 errors=0` |
| Focused concurrency run 2 | same command (separate execution) | exit `0`; failsafe **1** test, `skipped=0 failures=0 errors=0` |
| Focused concurrency run 3 | same command (separate execution) | exit `0`; failsafe **1** test, `skipped=0 failures=0 errors=0` |
| Boot smoke | `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | exit `0`; log contains `Started OneCallApplication` |
| Fat-jar firewall | `jar tf ... > target/jar-index.txt` + forbidden-library grep | `FIREWALL_OK`; 27 `BOOT-INF/lib/` entries; no `spring-security`/`flyway`/`jooq`/`HikariCP`/`postgresql`/`testcontainers` |
| V001 HEAD blob hash | Windows `git cat-file blob HEAD:...V001...` → SHA-256 | `946A0A13...CF81045` (matches frozen) |
| V001 CR-normalized diff | `git diff --ignore-cr-at-eol --exit-code HEAD -- ...V001...` | exit `0` |
| V002 hash | `sha256sum` | `534A0A06...9795D5` (unchanged) |
| Forbidden-scope diff | `git diff --exit-code HEAD -- backend/pom.xml backend/src/main/java ...BootstrapDependencyBoundaryTests.java` | exit `0` |
| Scoped whitespace | `git -c core.whitespace=cr-at-eol diff --check -- <three edited paths>` | exit `0`, zero diagnostics |
| Conflict markers | scoped grep for `<<<<<<<`/`=======`/`>>>>>>>` | none |
| Exact sets | Python parse of CI `expected`, `@Test` names, failsafe XML | 16 = 16 = 16; CI set == XML set == `@Test` set |

## 8. Negative and concurrency evidence

Expected failures are savepoint-scoped and assert exact SQLSTATE and exact constraint/error marker:

| Area | SQLSTATE / code | Exact names |
| --- | --- | --- |
| 34 named checks | `23514` | all 34 S1 names: `ck_customer_account_{display_name,status,revision,time_order}`, `ck_auth_identity_{provider,scope,ciphertext,hmac,status,key_version,revision}`, `ck_identity_profile_{status,legal_ciphertext,id_ciphertext,id_hmac,verification_ref,state_shape,revision}`, `ck_organization_{display_name,status,revision,time_order}`, `ck_membership_{role,department,status,revision}`, `ck_employee_{login_hmac,login_masked,issuer,subject_hmac,display_name,role,status,revision}` |
| FK child missing parent | `23503` | `fk_auth_identity_account`, `fk_identity_profile_account`, `fk_membership_organization`, `fk_membership_account` |
| FK parent delete RESTRICT (migration identity) | `23503` | `fk_auth_identity_account`, `fk_membership_account`, `fk_membership_organization` |
| Owner immutable guards | `P0001` | `CUSTOMER_ACCOUNT_IMMUTABLE`, `AUTH_IDENTITY_IMMUTABLE`, `IDENTITY_PROFILE_IMMUTABLE`, `ORGANIZATION_IMMUTABLE`, `MEMBERSHIP_IMMUTABLE`, `EMPLOYEE_ACCOUNT_IMMUTABLE` |
| Runtime immutable changes | `42501` | column-privilege denial for every immutable column |
| Unique collisions | `23505` | `uq_auth_identity_subject`, `uq_identity_profile_account`, `uq_employee_login_hmac`, `uq_employee_issuer_subject`, `uq_membership_live` |

Concurrency evidence (per scenario):

- Two connections; method-local UUIDs and random 64-hex HMACs; distinct `application_name` per contender; loser `statement_timeout=20000`; future bound `30s`; winner and loser inserts race the same tuple/pair.
- Lock wait is detected from `pg_stat_activity` (`application_name`, `state='active'`, `wait_event_type='Lock'`), bounded to 15s.
- Identity commit race: winner commits one row; loser receives `23505` on `uq_auth_identity_subject`, rolls back, reads the winner and classifies a different requested account as `IDENTITY_CONFLICT`; exactly one committed row and the loser id is absent.
- Identity rollback race: winner rolls back; loser insert returns `1` and commits; exactly one committed row (loser id), winner id absent.
- Membership commit race: winner commits one live row; loser receives `23505` on `uq_membership_live`, reads the live winner; exactly one live row for the pair and the loser id is absent.
- Membership rollback race: winner rolls back; loser insert returns `1` and commits; exactly one live row (loser id), winner id absent.
- Cleanup is migration-identity, FK-safe and exact: committed membership ids, then auth-identity ids, identity-profile ids, employee ids, organization ids, customer-account ids; each delete is followed by a zero-residue assertion. No `TRUNCATE`; no audit/append-only table mutation. Uncommitted methods roll back and assert zero residue for every method-local id.

## 9. Hash, line-ending and whitespace evidence

- V001: HEAD blob `946A0A13...CF81045`; CR-normalized diff exit `0`; no worktree normalization.
- V002: `534A0A06...9795D5` before and after; untouched.
- Changed tracked files: uniform CRLF worktree (CR == LF), LF-equivalent content hashes recorded in §4; no mixed endings introduced.
- Scoped `git -c core.whitespace=cr-at-eol diff --check` on the three edited tracked paths: exit `0`, zero diagnostics.
- New artifact: LF, no trailing whitespace, no conflict markers; `git diff --no-index --check /dev/null <artifact>` must report only the expected exit `1` for a new file with zero diagnostic output.
- A global raw `git diff --check` is not authoritative in this CRLF Windows checkout (baseline CRLF and pre-existing documentation whitespace), per plan §8.4/§8.7; the scoped CR-aware check is the fail-closed gate.

## 10. README/CI changes and documentation decision

- `.github/workflows/ci.yml`: only the failsafe expected set (four names) and its count/labels (S3→S2) changed. Other steps preserved.
- `backend/README.md`: title/intro and the 12→16 container count plus the four PA2 V002 S2 method descriptions. Default 10-test count, toolchain pins, module boundaries, Modulith note and evidence-boundary statements are unchanged.
- No design/plan/control/review/S1 document was modified. Only this one new evidence artifact was added.

## 11. Deviations, stop conditions and unresolved issues

1. **Subsumed profile status check (bounded test-only isolation).** `ck_identity_profile_status` is logically subsumed by `ck_identity_profile_state_shape`: any `real_name_status` outside `{UNVERIFIED,VERIFIED}` also falsifies the state-shape predicate, and PostgreSQL 16.15 reports `ck_identity_profile_state_shape` for a straightforward violating insert. To satisfy the requirement of a violating probe with the **exact** status constraint name, the test isolates it inside a savepoint by transactionally dropping `ck_identity_profile_state_shape` as `customer_iam_owner`, inserting the violating row (exact `23514`, exact `ck_identity_profile_status`), then rolling back to the savepoint. The rollback restores the catalog exactly; the helper asserts both constraints exist after rollback. This is test-only, transaction-local, leaves no persistent schema change, and does not alter V002 or weaken the S1 exact 34-check inventory. No production object or migration is added.
2. **First internal container run (calibration).** The initial version used the plain violating insert and observed `ck_identity_profile_state_shape` instead of the status name for that one probe (15/16 green). The bounded isolation above resolved it; every subsequent run is 16/16. No other assertion was relaxed.
3. No stop condition was triggered: V002 did not need a change, semantics were not ambiguous beyond the subsumption/isolation above, no service/function was needed, and cleanup required no `TRUNCATE` or append-only mutation.
4. No unresolved implementation issue. Deferred inputs (Q2/Q17/Q23) remain deferred and were not guessed.

## 12. Residual risks

| ID | Residual | Classification | Owner | Destination |
| --- | --- | --- | --- | --- |
| RSK-S2-01 | The transactional constraint-isolation technique for the subsumed status check should be reviewed for acceptable test-only DDL with guaranteed rollback. | Test-only; review attention | Backend/QA reviewer | S2 independent review |
| RSK-S2-02 | Concurrency races are timing-sensitive; mitigated by two connections, `application_name`, `pg_stat_activity` lock-wait detection, `statement_timeout=20s`, `30s` futures and three separate focused repetitions. | Bounded test risk (plan R-14) | Backend/QA | Keep bounded waits; focused repetitions |
| RSK-S2-03 | Floating `postgres:16` tag (observed 16.15) remains unpinned. | Inherited (plan R-11) | Platform/CI | Supply-chain hardening; no digest guess |
| RSK-S2-04 | Q2 provider-subject rotation, Q17 migration provisioning and Q23 `SECURITY DEFINER` capability remain deferred; only the frozen guard shape is tested. | Deferred input | Security/Platform/Database | Later design work units |
| RSK-S2-05 | Service-owned login gating, membership pair advisory locking/terminal transition and account-status ordering are outside V002. | Deferred behavior | customer-iam / organization-access services | Later service slices |
| RSK-S2-06 | GitHub Actions was not run at this gate; local container/boot/firewall success is not production acceptance. | Gate residual (plan R-16) | Phaseflow/CI | Exact-head PR gate; no production claim |

## 13. Completion and non-actions

S2 implementation and author validation are **complete**: exactly 16 container methods green, the focused concurrency method green in three separate `tests=1` executions, default 10-test verify green, validate/docker/boot/firewall/hash/whitespace checks green, and V002/V001 unchanged.

No later gate, commit, stage, push, PR, merge, review, S3 work or production acceptance was performed. This artifact reports to the controller for independent S2 review.

## 14. No-goal-drift and exact changed-file list

No goal drift:

- No V003+ object, schema, role, grant, sequence or migration.
- No production service, SQL function, generic trigger framework or application wiring.
- No POM/runtime dependency change.
- No UI/API, S3 privilege/last-admin behavior or production acceptance.
- No edit to V001/V002, `pom.xml`, production Java, `BootstrapDependencyBoundaryTests`, design/plan/control/review/S1 documents, `.git` or `.claude`.

Exact changed paths:

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (edited)
2. `.github/workflows/ci.yml` (edited)
3. `backend/README.md` (edited)
4. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s2-implementation.md` (new)
