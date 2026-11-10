# PA25 S1 implementation candidate

## Status and authority

- Work unit: `PA25-production-database-role-and-credential-readiness`.
- Gate: `implementation`, author/controller-complete candidate; independent code review required.
- Accepted plan: `docs/gateflow/pa25-production-database-role-and-credential-readiness-plan.md`, SHA256 `02635A174F9D2ADD867951463284B29F6AB518E4FE102B1AB3F483F05C1BA940`.
- Accepted plan checkpoint: `295e8fdecea55511cbc1f84a88686b91e30f368f`.
- Implementation entry bookkeeping HEAD: `4508b10087f02ded77165ae29cb22fa9f0daaae4`.
- Product base/latest `origin/main`: `a7c5fe40467da7bb352c774ad2ce3dbcd7500069`.

The implementation author was task `pa25-production-db-role-provisioning-s1-implementation-gpt-20261110-01` under requested GPT `gpt-6.1-sol` / high routing. Four controller-directed corrections were applied under separate fresh GPT tasks after real PostgreSQL 16.15 tests exposed defects. This artifact records candidate evidence, not implementation acceptance, production readiness or live deployment.

## Changed paths

Exactly the six accepted implementation paths differ from entry:

| Path | Bytes | SHA256 |
| --- | ---: | --- |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | 75,515 | `A1C408A98E1DCD67542D4286F797858288FDA6D29ED4D76078027E9577645ED0` |
| `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java` | 102,675 | `E1735AD81F65D2906B3A5A49BA7E4853F817B29D715235AEC7B24E08A3A29FEB` |
| `backend/pom.xml` | 8,573 | `960C0051508290F6A0CED3437EA4607ABDB7F34E9E778312656A7B5E1DB4369C` |
| `.github/workflows/ci.yml` | 100,467 | `F18DCDAD454C8E3D850B7F813A04464B2425CCA25907C1CC5C0743913C0C1A8B` |
| `backend/README.md` | 80,085 | `65450A8A2F65283F55330189BDF3214C20EEB271F6FE5F567761EF6809907AF6` |
| `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | 13,737 | `0630D7A4F9A37014489AC181505E25E7CD66F9BDC540E6411C815E708981610C` |

The SQL, Java, workflow, README and runbook are strict UTF-8/LF with final LF and no trailing whitespace. The existing POM remains mixed CRLF/LF and has no trailing whitespace. No `.attach_pid*` file is present.

## Implemented contract

- Standalone PostgreSQL 16 `psql` script with exact `apply` and `check` modes, explicit transaction, transaction-local inputs, fixed advisory lock and `ON_ERROR_STOP`.
- Six logins, seven effective roles and four exact membership edges using `ADMIN FALSE, INHERIT FALSE, SET TRUE`.
- Creation-only shared role comments bind database OID, role OID, slot and login name. Existing unmarked or mismatched roles fail before DDL; no adoption, restamping or override exists.
- UTF-8 bytea comparison makes provenance exact independent of text collation.
- Exact role attributes, ownership, settings, history, dependency, PUBLIC/database/default ACL and literal 300-tuple ACL checks. No object ACL is repaired or widened.
- Frozen V001-V013 remain byte-identical; no V014, runtime role, helper schema/table/function, credential input or runtime Java/config change exists.
- Twenty ordinary Failsafe methods use fresh `postgres:16` databases, the unmodified script, synthetic passwords, real TCP authentication and role-boundary probes.
- POM/CI inventory moves to `36/309 Surefire + 14/234 Failsafe = 50/543`, annotation scan 544 and 14 includes.
- README and runbook document responsibility separation, actual environment-style activation keys, stop/recovery, credential handoff, rotation, `pg_hba`/TLS and live-acceptance exclusions.

## Code-review fix 1 candidate (November 10, 2026)

- Task: `pa25-production-db-role-provisioning-s1-codereview-fix1-gpt-20261110-01`; sole accepted finding: `PA25-CR-01`.
- The default-ACL preflight now checks `pg_default_acl.defaclrole = ANY(managed_oids)` directly in a catalog-row `EXISTS`, independently of lateral ACL expansion. Empty ACL arrays cannot hide runtime-owned default-privileges rows. The existing PUBLIC/managed grantee and managed grantor checks remain in the separate `aclexplode` branch.
- The existing `publicDatabaseAndDefaultAclDriftFailClosed` method now grants a table default privilege for `pa25_query`, then revokes all table default privileges from both `pa25_query` and the grantee. It asserts exactly one matching catalog row with owner `pa25_query` and `cardinality(defaclacl) = 0`.
- That fixture calls `rejectBoth("PA25_DEFAULT_ACL_REJECTED")`: the existing rejection helper checks `apply` and `check` independently, asserts the exact diagnostic category, and compares the complete in-memory snapshot before/after each rejection, including raw `pg_default_acl` rows. No helper or snapshot coverage was weakened.
- Static validation passes: the exact same 20 ordinary test method names remain, with no parameterized/conditional expansion; the independent ownership and preserved ACL guards are present; all three allowed files remain strict UTF-8/LF with final LF and no trailing whitespace.
- Compilation passes with local Linux Temurin `21.0.13+9`, `javac -proc:none --release 21`, existing `backend/target/classes` / `backend/target/test-classes` and cached Maven dependency JARs; generated classes are confined to `/tmp`. This is a targeted source compile, not a fresh Maven build or suite run.
- Only the SQL, existing integration-test source and this implementation artifact are changed by this fix. No Control, plan, review, POM, CI, README, runbook, migration or runtime Java/config edit, report file, commit or push is made.
- Limitation: focused/full PostgreSQL 16 suites are not rerun by the implementation provider. Controller reruns and independent re-review remain required; the earlier controller evidence below predates this fix and does not certify the revised candidate.

**PA25 CODE-REVIEW FIX 1 COMPLETE; INDEPENDENT RE-REVIEW REQUIRED**

## Controller validation (before code-review fix 1)

Environment: Temurin `21.0.13+9`, Maven `3.9.9`, Docker `29.1.3`, pulled image `postgres@sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`, server PostgreSQL `16.15 (Debian 16.15-1.pgdg13+2)`.

The checked-in cron-sensitive `./mvnw` working copy has CRLF from the Windows checkout, so validation used the same pinned installed Maven 3.9.9 with the same Maven goals and no skips.

### Focused

Command:

```sh
mvn -B -ntp -o -Pcontainer-smoke -Dit.test=ProductionDatabaseIdentityProvisioningIT verify
```

- Exit `0`; Surefire `309/0/0/0`; focused Failsafe `20/0/0/0`; BUILD SUCCESS in 6m25s.
- Durable log: `D:\codex-temp\pa25-controller-validation-20261110-8f4d2c61\focused.log`, 240,974 bytes, SHA256 `C78039CC59B9C95C17831E2A4C2FF06627952DE73027AA544354B77CA8F53A76`.

### Full

Command:

```sh
mvn -B -ntp -o -Pcontainer-smoke clean verify
```

- Exit `0`; Surefire `309/0/0/0`; Failsafe `234/0/0/0` in 14 classes; total `543` in 50 classes; no skips.
- New class passed all 20 fresh-container methods. The log records PostgreSQL `16.15` twenty times, once per test.
- BUILD SUCCESS in 12m19s.
- Durable log: `D:\codex-temp\pa25-controller-validation-20261110-8f4d2c61\full.log`, 568,096 bytes, SHA256 `ABACB2B074FE6B2BC9D441822537679E20C8D2AE00BB3F0CFDBE63C054D07E12`.

### Inventory, JAR and firewall

- Exact CI source/CI/XML inventory against the fresh reports exited `0`: `36/309 + 14/234 = 50/543`, line scan `544`, POM includes `14`, zero skips/errors/failures. Log `inventory.log`, 381 bytes, SHA256 `047BFDA3567718E063BDF242B1DFC52B4737A0336DDEB8BFDDABDBBD381FF4B1`.
- Default and opt-in Web JAR smoke block exited `0`, including all bounded API probes and JVM cleanup. Log `smokes.log`, 83 bytes, SHA256 `D41B148440F150DF2D6F3E52AABA38AA384F6D4CF43EA42CFF55279A5AF651DC`.
- Production JAR dependency firewall exited `0`: 72 approved coordinates, 65 packaged JARs, 8 dependency-only coordinates, 64 counterparts, one packager role, 116 adversarial probes. Log `firewall.log`, 1,216 bytes, SHA256 `9B90B55E19BFB229CBAB74769CF0D9E444EC1B0C095F410D6137FB6A41FB7C4F`.

### Existing contract regressions

- A1 s3, A3 s3 and A4 s2 passed.
- A2 s3 fails in the current tree on pre-existing A0 documentation/derived-model drift: 14 named CHECKs and many column dictionary/model references are reported missing.
- A5 s1 and A5 s2 fail in the current tree on the same pre-existing acceptance section 7.1 state-table-header guard.
- The identical failures occur from a clean detached worktree at the exact product base `a7c5fe40`: `base-a2.log` exit `1` and `base-a5s1.log` / `base-a5s2.log` exit `2` with identical messages. They are not caused by PA25's six-path diff.
- Logs: `contracts.log` 8,109 bytes SHA256 `78A3351169F2E59AA432A9297CE60E2E0A41F97EB992E5362D8D995F0671AD69`; `contracts-rest.log` 20,403 bytes SHA256 `0E49445A1FDF0CD5DC876D1932CFD2413E2B06AAF50FB9E814174AF479313FC8`; `base-a2.log` 3,987 bytes SHA256 `ECFD019E5BAF747214D658EC82183660C85749E2594920D9381F825F3F888BD8`; `base-a5s1.log` and `base-a5s2.log` 89 bytes each with matching failure text.

## Frozen migrations and hygiene

- All 13 V001-V013 SHA256 values match accepted plan section 10 exactly.
- `git diff --check` passed.
- Path audit shows only the six accepted implementation paths plus pre-existing private `.codex` and `.pa25-plan-prompt.txt`.
- Secret-pattern scan found no private key, API key, credential-bearing PostgreSQL URL or hardcoded password in the changed artifacts.
- No production database login, password, credential, `pg_hba`, TLS, cluster or live endpoint was created or used.

## Implementation discoveries and residuals

1. Spring `SystemEnvironmentPropertySource` requires underscores in compound environment keys. The tested and documented actual keys are `ONECALL_SHARING_COMMAND_ENABLED`, `ONECALL_SHARING_PUBLIC_READ_DATASOURCE_*`, `ONECALL_SHARING_ADMISSION_DATASOURCE_*` and `ONECALL_EVIDENCE_MEDIA_READ_DATASOURCE_*`. The accepted plan's earlier dash-removed canonical key text is historical and is not used by implementation. This discrepancy must be reviewed and retained as an implementation-evidence correction, not silently claimed as original plan text.
2. Existing A2/A5 contract regressions are baseline documentation drift at `origin/main`, owned by Protocol/Data/Backend contract-document maintainers and not repaired here.
3. Platform/Database/Security/Operations still own real role names, superuser custody, passwords, TLS, `pg_hba`, cluster selection, marker-loss/restore disposition, rotation, live probes and production acceptance.
4. The script and tests certify synthetic PostgreSQL authorization structure only. There is no claim of hosted CI, live cluster, actual authentication readiness or production deployment.

## Decision

Implementation candidate passes focused/full PostgreSQL 16, inventory, JAR smoke, dependency firewall, migration immutability and six-path hygiene. Freeze this exact candidate for independent A/B code review. No commit, push, PR, approval, mark-ready, reviewer request or external comment occurs in the implementation gate.

**PA25 S1 IMPLEMENTATION CANDIDATE COMPLETE; INDEPENDENT CODE REVIEW REQUIRED**
