# PA16 Public-Read Least-Privilege ACL — Bounded Plan

## 1. Identity, Authority And Gate

- Work unit: `PA16-sharing-public-read-least-privilege-acl`; original task: `pa16-public-read-acl-plan-gpt-20261106`; fix task: `pa16-public-read-acl-plan-fix1-gpt-20261106`.
- Date: 2026-11-06, Asia/Shanghai. Gate: **fix following plan review**; status: **plan repaired for Controller validation, implementation not started**.
- Binding authority: the accepted PA16 goal-confirmation checkpoint in `docs/gateflow/project-execution-control.md`, not historical PA15 design latitude.
- Branch: `codex/pa16-sharing-public-read-least-privilege-acl`. HEAD and locally available `origin/main` both resolve to `0ccff786a6877229c278d0584423a65014669eed`.
- Actual Git directory: `/mnt/d/codex-call/.git/worktrees/codex-call16`. The checkout's `.git` contains a Windows `D:/codex-call/.git/worktrees/codex-call16` pointer, which ordinary WSL Git cannot resolve. Use command-local `GIT_DIR` and `GIT_WORK_TREE`; do not repair the pointer or Git configuration.
- Initial status: modified Controller-owned `docs/gateflow/project-execution-control.md`; untracked `.codex` and `.pa16-plan/`. Neither private path was opened. The Controller change is neither our candidate nor permission to edit/revert it.
- Original plan-gate Controller document SHA-256: `6d4647e785eb1377953ffed1ff84a0969efc44df5f1210c89f0a904f27396e92` (historical, not the current adjudication identity).
- Fix authority: current Control goal confirmation and accepted review adjudication; both reviews `docs/reviews/plan-review-20261106-095836.md` and `docs/reviews/plan-review-20261106-102208.md` were read. This gate authorizes modifying only this plan and creating `docs/gateflow/pa16-sharing-public-read-least-privilege-acl-plan-fix-1.md`. Everything in the implementation allowlist is a **future proposal**, not permission to implement now. No re-review is started.
- The nested-record repair corrects the original 17-path intake to exactly **16 paths (15 modified + one new V011)**; Control's earlier 17-path wording is historical and Controller-owned, not permission to add a production file or edit Control here.

## 2. Evidence And Boundary

Read authorities: Control's PA16 checkpoint and PA15 final-closeout history; `pa15-sharing-public-share-read-final-closeout.md`, `pa15-sharing-public-share-read-plan.md`, and `pa15-sharing-public-share-read-implementation.md`; `docs/14-production-data-model-and-flyway-schema-spec.md` §§2.1, 2.4–2.6, 2.12, S3 dictionary/ownership, 9.27–9.31, 10.21–10.24, 11 and 13.8–13.11; V008/V009/V010; the public service/repository/configuration; evidence projection interface/implementation/configuration; command/query configuration and properties; named public/ACL tests and their fixtures; relevant `PostgresContainerIT` migration and ACL assertions; CI, POM and README. Historical claims were checked against live source and CI rather than copied as current evidence.

Current defect: `PublicSharePersistenceConfiguration` uses `sharingCommandTransactionManager` and `sharingCommandDsl`. `PublicShareQueryService.getPublicShare` asserts READ ONLY, assumes `sharing_command_role`, then temporarily assumes writable `evidence_role` in `content`. The shared evidence implementation has `@Transactional(MANDATORY, transactionManager="sharingCommandTransactionManager")`. Moving only the repository to another DSL would retain the wrong transaction boundary; removing only SET ROLE would fall through to the command login's ability to assume writable roles.

Deliver **one end-to-end behavior slice**: V011 privileges + separate public connection graph + an evidence-owned MANDATORY adapter + executable database/API evidence. Migration-only or pool-only intermediate slices would not independently repair the public GET boundary. No feature/API/schema-definition slice is needed.

Preserve the three V009 tables, all columns/checks/indexes/FKs/triggers/immutability/lifecycles and every byte of V001–V010. Keep exact digest bytes, optional customer-session/member decisions, safe DTO omission/redaction, 404/410/422/503 mappings and existing 401/500 member/dependency outcomes. Do not edit controllers, error writers, route normalization, OpenAPI or policy providers. The read remains an observation, not an admission or lease.

## 3. Explicit Topology Decision

**One dedicated role, also the isolated login:** `sharing_public_read_role`. Create it as LOGIN with no password; actual authentication material is supplied only by later deployment owners. This is a deliberate alternative to the existing NOLOGIN group-role plus synthetic-login pattern. PostgreSQL roles can be both a login and a privilege principal. No second credential carrier, role membership, inherited privilege, SET ROLE grant, or public-path role transition is necessary.

Role attributes: `rolcanlogin=true`; `rolsuper=false`, `rolcreatedb=false`, `rolcreaterole=false`, `rolinherit=false`, `rolreplication=false`, `rolbypassrls=false`; `rolconnlimit=-1`; `rolvaliduntil IS NULL`; no role/database-role settings; no ownership. Fresh V011 leaves the password null. The NULL password does not substitute for pg_hba/TLS policy: trust/peer/certificate authentication is deployment-owned, and no usable authentication route is established by this plan. Existing compatible instances may have deployment-managed authentication material. Compatibility rejection must never depend on `rolpassword` value/content or nullness. No migration/test/snapshot SQL may select or log an existing password hash. Only a fresh-creation test, before fixture authentication is assigned, may query `SELECT rolpassword IS NULL FROM pg_authid WHERE rolname='sharing_public_read_role'` and require true; never select the value itself. Compatible replay with a disposable assigned password must succeed without reading its hash.

**One separate Hikari pool**, logged in directly as this exact role; own transaction manager and transaction-aware jOOQ DSL. The public graph never borrows the command or evidence pool. Keep the existing `persistence` profile and `onecall.sharing-command.enabled=true` activation boundary to avoid changing PA15 activation semantics; add only required dedicated datasource properties, not a new enable flag. Enabling that existing graph without the new explicit credentials fails startup, never defaults to command/query/admin credentials. Default/web-only/disabled-command contexts remain database-free.

Why not two projection roles: the existing operation necessarily reads both projections on one connection, and the evidence interface already accepts the caller's DSL. A second role adds membership/SET capability and restoration failure modes without withholding data this operation must read. Why not reuse the command login with NOINHERIT: NOINHERIT does not remove SET ROLE authorization, RESET ROLE returns to that broad session identity, and READ ONLY is transaction state rather than an ACL. This plan requires **zero** membership rows where the public role is either `member` or `roleid`, including ADMIN/INHERIT/SET options. No `sharing_command_role` or `evidence_role` membership is permitted, directly or transitively.

Proof under a defect/injection: at connection creation, transaction entry, repository read, evidence read and pool return, `session_user=current_user=sharing_public_read_role`. There is no intended role switch to omit. RESET ROLE/SET ROLE NONE returns to the same narrow principal; assuming either writable role is 42501. Even a separate READ WRITE probe has no application write/row-lock/DDL privilege. SQL remains indexed/prepared; read-only transaction enforcement is an additional layer, not the reason the role cannot write application data. Wrong configured username is rejected before pool construction; wrong runtime current/session identity fails closed before repository invocation. Credential theft, DBA changes and arbitrary privileged Java execution are not made safe by a pool: deployment and process trust remain residuals.

### PUBLIC TEMP Is Part Of The Boundary

V001 removes PUBLIC privileges on schema `public`; V010 removes PUBLIC application ACLs. Neither revokes the default PUBLIC database TEMPORARY privilege. PostgreSQL ACLs have no per-role DENY overriding PUBLIC, so a read-only principal could otherwise create temporary tables in a READ WRITE connection. V011 must revoke **only TEMPORARY on the current database from PUBLIC**, with no replacement grants. This necessary database-level ACL adjustment is privilege-only, not a table change or generic hardening project. Existing application code/test SQL has no CREATE TEMP/TEMPORARY requirement. Explicit TEMP grants to unrelated principals are untouched. Database CREATE granted to PUBLIC, unknown PUBLIC application grants, or dependency on PUBLIC TEMP in an actual deployment is a fail-closed incompatibility for Database/SRE to resolve, not permission to grant additional capabilities or silently skip this check.

PUBLIC CONNECT and PostgreSQL's ordinary catalog/built-in privileges are not newly granted or claimed absent. The zero-PUBLIC assertion concerns application schema/object ACLs and database CREATE/TEMP, not catalog visibility or built-in time functions. No application function privilege is allowed; `clock_timestamp()` requires no new grant. This is an application-data privilege boundary, not a replacement PostgreSQL built-in sandbox or isolation from the trusted JVM/DBA.

## 4. Exact SQL Shape And Column Allowlist

The repository SQL and evidence implementation's SELECT/JOIN/predicate shapes remain unchanged. Do not replace them with SELECT *, a view, a function, an alternate projection, or direct evidence SQL in sharing.

```sql
SELECT evidence_id, owner_organization_id, verification_mode, status,
       valid_from, expires_at, download_allowed, content_snapshot
FROM sharing.evidence_share_grant WHERE capability_token_hash = ?;
SELECT clock_timestamp();
SELECT id, subject_id, status, source_type, source_ref_id, identity_at_capture
FROM evidence.evidence_record WHERE id = ?;
SELECT a.artifact_role, m.sha256, m.size_bytes, m.mime_type, r.validated_duration
FROM evidence.evidence_artifact a
JOIN evidence.media_object m ON m.id = a.media_object_id
LEFT JOIN evidence.recording r ON r.media_object_id = a.media_object_id
WHERE a.evidence_id = ?;
```

The complete allowlist is **24 column SELECT grants across five tables**, plus two schema USAGE grants. Predicates and join keys require SELECT even when not returned.

| Table | Exact permitted columns | Reason |
| --- | --- | --- |
| `sharing.evidence_share_grant` | `capability_token_hash`, `evidence_id`, `owner_organization_id`, `verification_mode`, `status`, `valid_from`, `expires_at`, `download_allowed`, `content_snapshot` (9) | Digest predicate and existing eight-field internal PublicGrant |
| `evidence.evidence_record` | `id`, `subject_id`, `status`, `source_type`, `source_ref_id`, `identity_at_capture` (6) | Evidence predicate/owner contract and captured identity |
| `evidence.evidence_artifact` | `evidence_id`, `media_object_id`, `artifact_role` (3) | Predicate, joins and role |
| `evidence.media_object` | `id`, `sha256`, `size_bytes`, `mime_type` (4) | Join and frozen artifact facts |
| `evidence.recording` | `media_object_id`, `validated_duration` (2) | Join and validated duration |

Exact currently forbidden complements, derived from V008/V009 rather than a guessed sensitive-field list:

| Table | Every forbidden column |
| --- | --- |
| `sharing.evidence_share_grant` | `id`, `share_id`, `created_by_account_id`, `code_salt`, `code_hash`, `code_generation`, `max_admissions`, `admissions_used`, `content_sha256`, `revocation_generation`, `revision` (11) |
| `evidence.evidence_record` | `source_event_domain`, `source_event_id`, `call_id`, `session_id`, `reservation_id`, `consent_version`, `revision`, `snapshot`, `snapshot_sha256`, `created_at`, `solidified_at` (11) |
| `evidence.evidence_artifact` | `id`, `created_at` (2) |
| `evidence.media_object` | `storage_provider`, `bucket`, `object_key`, `object_version_id`, `crypto_key_ref`, `worm_status`, `created_at` (7) |
| `evidence.recording` | `id`, `call_id`, `recording_id`, `declared_format`, `declared_size`, `declared_duration`, `declared_sha256`, `validated_sha256`, `status`, `upload_expires_at`, `created_at`, `validated_at`, `revision` (13) |

Additionally deny every column of `sharing.share_admission` (7), `sharing.share_access_event` (11) and `evidence.evidence_event` (8). Tests enumerate pg_attribute and assert the complement equals these frozen lists: there are 44 forbidden columns on the five readable tables and 26 on the three entirely denied S3 tables, **70 individual denied SELECT probes**, not merely one representative secret. Audit/Outbox/IAM and every other application table remain entirely inaccessible.

## 5. V011 Migration Contract

New path: `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql`. Flyway version `011`, description `sharing public read least privilege`, SQL migration, transactional PostgreSQL execution. No table/column/constraint/index/trigger/function/view/sequence creation or alteration; no data DML, default-privilege expansion, existing-role grant expansion, extension or explicit COMMIT. DO blocks for checks are anonymous, not persisted service functions.

### Preconditions And Incompatible State

1. `server_version_num >= 160000 AND < 170000`; current migration identity must actually be superuser, following V010's explicit check. Require `sharing_owner`, `evidence_owner`, expected schema owners, all five target relations and all 24 target columns to exist with their V010 ownership. Require the V009 three-table structure; structural inventory is proved in tests, not rebuilt in V011.
2. Resolve the target role OID only if it exists. A missing role is created by the exact CREATE ROLE below. An existing role must have the attributes above, no `pg_auth_members` row in either direction, no `pg_db_role_setting`, no ownership dependencies (`pg_shdepend.deptype='o'`, including shared/other-database objects), no direct database/tablespace/parameter/foreign-server/large-object capability, and no cross-database ACL dependency. Reject, do not ALTER a broad preexisting role into apparent compatibility. Explicitly exclude `rolpassword` from compatibility predicates and snapshots; deployment-managed password presence/content is not an incompatibility.
3. For a preexisting role, existing explicit application ACL rows may be a **subset** of the exact postcondition set, with correct object-owner grantor and `is_grantable=false`. Any extra schema/table/column/sequence/function grant, grant option, default ACL involving the role, or unrecognized non-catalog capability rejects the migration. A pristine compatible role is acceptable. No membership is added or silently revoked. No DROP OWNED, REASSIGN OWNED, CASCADE or privilege laundering.
4. Inspect all non-system application schemas, not only the V010 nine-schema reset subset: `platform`, `audit`, `customer_iam`, `organization_access`, `operations_iam`, `subject_number`, `catalog_order`, `ivr`, `entitlement_metering`, `activation_consent`, `evidence`, `sharing`, plus `public` and any unexpected non-system schema. Reject PUBLIC application schema/table/column/sequence/routine grants or PUBLIC database CREATE. Catalog/information_schema defaults are excluded explicitly. TEMPORARY on the current database is the one expected PUBLIC capability that V011 removes. Do not broaden V011 into a global pg_catalog function-ACL rewrite.
5. Use `RAISE EXCEPTION USING ERRCODE='P0001', MESSAGE='<fixed V011 incompatibility message>'` for migration precondition/postcondition refusal; include object/role identifiers only, never credentials. All failures must roll back role creation, grants and the PUBLIC TEMP revoke. Unexpected operational SQL failures retain their original SQLSTATE; do not catch-and-ignore them.

### Exact Mutating SQL

Run the following only after compatible-state checks. Guards wrap CREATE ROLE; schema/column grants are issued as the existing object owners so grantor identity is deterministic. Anonymous check blocks must restore migration current_user before the next owner section.

```sql
CREATE ROLE sharing_public_read_role LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
    NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD NULL;
-- In a DO block; quote the actual current database identifier, not a configured URL:
-- EXECUTE format('REVOKE TEMPORARY ON DATABASE %I FROM PUBLIC', current_database());

SET LOCAL ROLE sharing_owner;
GRANT USAGE ON SCHEMA sharing TO sharing_public_read_role;
GRANT SELECT (capability_token_hash, evidence_id, owner_organization_id,
    verification_mode, status, valid_from, expires_at, download_allowed, content_snapshot)
    ON sharing.evidence_share_grant TO sharing_public_read_role;
RESET ROLE;

SET LOCAL ROLE evidence_owner;
GRANT USAGE ON SCHEMA evidence TO sharing_public_read_role;
GRANT SELECT (id, subject_id, status, source_type, source_ref_id, identity_at_capture)
    ON evidence.evidence_record TO sharing_public_read_role;
GRANT SELECT (evidence_id, media_object_id, artifact_role)
    ON evidence.evidence_artifact TO sharing_public_read_role;
GRANT SELECT (id, sha256, size_bytes, mime_type)
    ON evidence.media_object TO sharing_public_read_role;
GRANT SELECT (media_object_id, validated_duration)
    ON evidence.recording TO sharing_public_read_role;
RESET ROLE;
```

The database TEMP revoke is executable SQL inside its DO block, not an omitted operation. Never GRANT table-level SELECT, CONNECT/TEMP/CREATE to this role, function EXECUTE, sequence privileges or membership. No runtime SQL uses either owner role. Migration owner switches are not application permission requirements.

### Postconditions And Idempotence

Re-run role/membership/ownership checks; exact application schema ACL = two USAGE rows and exact column ACL = 24 SELECT rows; zero table ACL, sequence/routine/default ACL, database CREATE/TEMP, or grant option. Every schema grantor equals nspowner; every column grantor equals relowner. All other application privileges are absent both explicitly and effectively. Assert `has_table_privilege(...,'SELECT')=false` on all five readable tables while every permitted `has_column_privilege(...,'SELECT')=true`. For SELECT/INSERT/UPDATE/REFERENCES check every application column, not just relation ACLs. Existing command/query/evidence ACLs and attributes are unchanged. PUBLIC TEMP is absent; existing unrelated explicit database ACLs and PUBLIC CONNECT are unchanged.

Normal Flyway second migrate executes zero migrations. Direct transactional replay of V011 on a compatible deployed role is also safe and ACL-idempotent, including after a test-only password was assigned. An incompatible role/ACL/ownership/membership/settings state is rejected atomically, not repaired destructively. New scratch tests must independently inject each family of incompatibility, snapshot state, assert deepest SQLException P0001, and prove exact restoration. Test transaction rollback after the final grant by executing a temporary copy with appended `SELECT 1/0`; deepest state must be 22012 and role/ACL/PUBLIC-TEMP/history state must equal the pre-migration snapshot. Temporary test resources belong only in implementation's disposable directories.

## 6. Minimum Runtime Changes

### Properties And Dedicated Beans

Define the properties type **inside** the existing `PublicSharePersistenceConfiguration` class, not in a separate source file. Exact declaration and compact constructor (record nesting does not add methods to the enclosing configuration):

```java
@ConfigurationProperties("onecall.sharing-public-read.datasource")
public static record SharingPublicReadPersistenceProperties(String url, String username, String password) {
    public SharingPublicReadPersistenceProperties {
        if (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank() || !username.equals("sharing_public_read_role")) {
            throw new IllegalArgumentException("Explicit dedicated public-read credentials are required");
        }
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
```

Import `org.springframework.boot.context.properties.ConfigurationProperties` and `EnableConfigurationProperties`; annotate the enclosing class with `@EnableConfigurationProperties(PublicSharePersistenceConfiguration.SharingPublicReadPersistenceProperties.class)` alongside its unchanged `@Configuration(proxyBeanMethods=false)`, `@Profile("persistence")` and command-enabled condition. Use this nested type as the `publicShareQueryDataSource` parameter; external tests import `PublicSharePersistenceConfiguration.SharingPublicReadPersistenceProperties`. Do not annotate the constructor with `@ConstructorBinding` or register the record as a component/extra bean; its canonical constructor is the binding constructor. Binding prefix is exactly `onecall.sharing-public-read.datasource`, requiring `.url`, `.username`, `.password`. Do not trim/normalize, default, interpolate or include credentials in exceptions/toString. No new production Java path, application YAML, policy/capacity value or fallback credential source is permitted. The protected `SharingQueryBoundaryTests` file sets and assertions remain byte-for-byte unchanged.

Exact bean graph in `PublicSharePersistenceConfiguration` (six bean methods, existing three plus three):

| Bean/method | Exact dependency/behavior |
| --- | --- |
| `publicShareQueryDataSource` | New properties only; Hikari JDBC URL/username/password; `destroyMethod="close"`; no alias/Primary/command pool access; default pool sizing unchanged |
| `publicShareQueryTransactionManager` | Qualifier `publicShareQueryDataSource`; DataSourceTransactionManager; `setRollbackOnCommitFailure(true)` and `setEnforceReadOnly(true)` |
| `publicShareQueryTransactionTemplate` | Qualifier `publicShareQueryTransactionManager`; READ_COMMITTED, REQUIRES_NEW, readOnly=true |
| `publicShareQueryDsl` | Qualifier `publicShareQueryDataSource`; POSTGRES; executeLogging=false; ParamType.INDEXED; PREPARED_STATEMENT; DataSourceConnectionProvider(TransactionAwareDataSourceProxy) |
| `publicShareQueryRepository` | Qualifier `publicShareQueryDsl`; existing JooqPublicShareQueryRepository |
| `publicShareQueryService` | Qualifiers in order: `publicShareQueryTransactionTemplate`, `publicShareQueryDsl`, `publicShareQueryRepository`, `publicShareableEvidenceProjectionReader`, `sha256Provider` |

Update constructor qualifiers in `JooqPublicShareQueryRepository` and `PublicShareQueryService` consistently. Repository interface, PublicGrant, SQL and result mapping are read-only inputs and remain unchanged. Do not touch existing SharingCommandPersistenceConfiguration/Properties or SharingPersistenceConfiguration.

### Evidence-Owned MANDATORY Adapter

Keep `JooqShareableEvidenceProjectionReader` and its command-bound MANDATORY method byte-for-byte unchanged; command create must still run on its command transaction and existing evidence-role switch. In the existing evidence-owned `ShareProjectionReaderConfiguration`, retain the class's `persistence` profile, command-enabled condition and `@EnableTransactionManagement`. Add the package-access method `@Bean ShareableEvidenceProjectionReader publicShareableEvidenceProjectionReader()` with the additional method annotation `@ConditionalOnProperty(prefix="onecall.sharing-public-read.datasource", name="url", matchIfMissing=false)` (no `havingValue`, no new enable flag). It returns a nested `public static class PublicReadProjectionReader extends JooqShareableEvidenceProjectionReader`. Override `public ReadResult readShareableProjection(DSLContext transactionDsl, UUID evidenceId)` with `@Override` and `@Transactional(transactionManager="publicShareQueryTransactionManager", propagation=Propagation.MANDATORY)`, returning `super.readShareableProjection(transactionDsl, evidenceId)` directly.

Spring proxies the public bean's most-specific override. The super call is a Java self invocation, not another proxy hop, so the superclass command annotation does not start/check a command transaction. URL absent: command-only contexts contain exactly the original named `shareableEvidenceProjectionReader`; their by-type lookups remain unambiguous, including all three protected `CustomerShareCommandsIT` sites. URL present with valid credentials: public contexts contain exactly both named readers; injection and lookups select the appropriate name. The condition also treats literal `false` as disabled by Spring's default semantics; it is an activation guard, not credential validation (blank/invalid credentials still fail binding). In `PublicSharesIT`, replace both current by-type reader lookups (the owner-reader setup and `service(...)` fallback) with `fixture.context.getBean("publicShareableEvidenceProjectionReader", ShareableEvidenceProjectionReader.class)`; no public helper may fall back to the command reader. Never edit `CustomerShareCommandsIT` or add `@Primary`. No SQL copy, generic reader abstraction, annotation removal, thread-local manager selector, second evidence pool or sharing-to-evidence-internal import. MANDATORY remains manager-specific; tests below must prove real proxy behavior, including absence of the named public manager.

### Public Service Orchestration

In `getPublicShare`, keep `SET TRANSACTION READ ONLY` before any service lookup and `requireReadOnly()`. Replace `sharingRole()` with `requirePublicReadIdentity()`; its single query is `SELECT current_user, session_user`, require both columns equal `sharing_public_read_role`, otherwise SERVICE_UNAVAILABLE. Never SET ROLE in the public service.

In `content(PublicGrant, JsonNode)`, replace the evidence-role try/finally block with `requirePublicReadIdentity(); requireReadOnly(); reader.readShareableProjection(context, grant.evidenceId()); requirePublicReadIdentity(); requireReadOnly();` and the unchanged Found/Missing/Unavailable mapping and immutable projection validation. Remove obsolete `sharingRole()`/`role(String)` and role-restoration logic. No mutation, new exception mapping, token processing or member checks change. Reader null, wrong identity, READ ONLY failure, missing ACL, broken pool, MANDATORY failure, malformed snapshot/projection and transaction failure still produce sanitized 503 with no partial content. Known public-domain exceptions retain their exact existing mappings.

## 7. Future File Ownership And Exact Allowlist

One implementation author owns the whole slice. No parallel workers or review authors are authorized by this plan. Required **15 existing modified paths**:

1. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/PublicSharePersistenceConfiguration.java` — isolated graph.
2. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqPublicShareQueryRepository.java` — constructor qualifier only.
3. `backend/src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java` — narrow identity assertions; remove switches.
4. `backend/src/main/java/com/onecall/core/evidence/internal/shareprojection/ShareProjectionReaderConfiguration.java` — evidence-owned adapter bean/override.
5. `backend/src/test/java/com/onecall/archtests/PublicSharePersistenceConfigurationTests.java` — isolated wiring, properties, profiles and MANDATORY assertions.
6. `backend/src/test/java/com/onecall/archtests/sharing/PublicShareQueryServiceTests.java` — direct principal mocks and fail-closed cases.
7. `backend/src/test/java/com/onecall/archtests/container/PublicSharesFixture.java` — separate public pool/DSL/credentials and observations.
8. `backend/src/test/java/com/onecall/archtests/container/PublicSharesIT.java` — retained eight behavior partitions, new isolation expectations.
9. `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java` — four added V011 methods; V010 historical target fences.
10. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` — cumulative history and exact new-role inventory row only; existing 64 behavior methods retained.
11. `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java` — latest migration/applied counts 10→11 only.
12. `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java` — latest migration/applied counts 10→11 only.
13. `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java` — applied-history count 10→11 only.
14. `.github/workflows/ci.yml` — exact method inventories/count assertions/status text only.
15. `backend/README.md` — current inventory/history and public-role/pool/property/provisioning contract only.

Required **one new implementation path**:

16. `backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql`.
Thus exactly **16 implementation candidate paths** (15 modified + one new V011), separate from this plan and its fix artifact. The properties record is nested in path 1; there is no separate properties Java path. In particular, `SharingQueryBoundaryTests` and `CustomerShareCommandsIT` remain protected and absent from this allowlist. No POM edit: all new integration methods live in an already included Failsafe class. No new dependency or runtime resource/config file. All other source/tests/migrations/docs, Control, previous Gateflow artifacts and private scaffolding remain protected. Later Controller-owned implementation/review reports are not pre-authorized candidate files.

### Exact Cumulative History Sites

- `PostgresContainerIT.startContainer`: latest migration after the V008 staged prefix now executes 3 (V009–V011), not 2. Preserve the V004/V005/V006/V007/V008 staged counts and fences. In `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`, append the exact V011 version/description/type/script/success row after V010. The historical method name remains unchanged. In `assertV009LiveFence`'s history and `flyway.info().all()` script lists, append `011|V011__sharing_public_read_least_privilege.sql|SQL|true` and the script name. In `assertSchemasAndRoles`, add exactly `sharing_public_read_role:true:false:false:false` to `expectedRoles` before sorting; do not ignore extra roles. Do not change unrelated hasSize(10) constraints/FK maps.
- `SharingRoleLeastPrivilegeIT.start` now executes 11; `v010MigrationIdentityAndPostgres16` expects cumulative applied size 11, keeps index 9 = V010, checks index 10 = V011. Its historical scratch `staleMigration` must explicitly target `10`, so V010 refusal/repair scenarios still execute one migration rather than opportunistically applying V011. Its filesystem failing-V010 copy still contains V001–V010 only. Preserve all eight V010 methods and canonical normalized digest expectations.
- CustomerMeFixture currently has migrate/applied assertions at lines 87/89; SharingQueryFixture at 70/72; SharingQueryServiceIT at 36. Change only cumulative 10→11 assertions. These locations are inspection anchors, not permission to replace every numeric ten.
- Existing `assertV009AclCatalog` deliberately filters old sharing roles; existing V008 owned/cross-module checks enumerate old roles. Do not weaken their exact sets to tolerate an unrecognized principal. The new V011 catalog test independently checks the full new-role allowlist and PUBLIC state; old-role ACLs must still pass unchanged. Any unexpected unfiltered legacy assertion must be investigated within the named Postgres test file, not relaxed globally.

## 8. Test Inventory And Behavioral Partitions

Read-only extraction of JUnit `@Test void` methods and evaluation of CI's literal inventories/updates proved exact source=CI identity: **19 Surefire classes / 91 methods; 7 Failsafe classes / 126 methods; 217 combined**. No Maven/XML runtime claim is made at plan gate. Reject duplicate, conditional, disabled, parameterized or generated-test substitution using the existing inventory checker.

| Current Surefire class (package follows live source/CI) | Now → future |
| --- | --- |
| `com.onecall.archtests.architecture.ModuleBoundaryTests` | 8→8 |
| `com.onecall.archtests.OneCallApplicationTests` | 1→1 |
| `com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests` | 1→1 |
| `com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests` | 8→8 |
| `com.onecall.archtests.SharingPersistenceConfigurationTests` | 2→2 |
| `com.onecall.archtests.architecture.SharingQueryBoundaryTests` | 1→1 |
| `com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests` | 7→7 |
| `com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests` | 6→6 |
| `com.onecall.archtests.CustomerWebConfigurationTests` | 6→6 |
| `com.onecall.archtests.CustomerMeWebTests` | 8→8 |
| `com.onecall.archtests.architecture.CustomerApiBoundaryTests` | 3→3 |
| `com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests` | 6→6 |
| `com.onecall.archtests.CustomerSharesWebTests` | 10→10 |
| `com.onecall.archtests.sharing.CustomerShareCommandServiceTests` | 6→6 |
| `com.onecall.archtests.CustomerSharesCommandWebTests` | 4→4 |
| `com.onecall.archtests.SharingCommandPersistenceConfigurationTests` | 2→2 |
| `com.onecall.archtests.PublicShareWebTests` | 6→6 |
| `com.onecall.archtests.sharing.PublicShareQueryServiceTests` | 4→4 |
| `com.onecall.archtests.PublicSharePersistenceConfigurationTests` | 2→2 |

| Failsafe class, all `com.onecall.archtests.container` | Now → future |
| --- | --- |
| `PostgresContainerIT` | 64→64 |
| `SharingQueryServiceIT` | 10→10 |
| `CustomerMeIT` | 10→10 |
| `CustomerSharesIT` | 8→8 |
| `SharingRoleLeastPrivilegeIT` | 8→12 |
| `CustomerShareCommandsIT` | 18→18 |
| `PublicSharesIT` | 8→8 |

Future: **91 + (64+10+10+8+12+18+8=130) = 221 methods in the same 26 classes**. No test-method inflation for each column/SQLSTATE/scenario: loops and helper assertions live in four cohesive new methods. Existing six-class PA15-focused Surefire selection remains 24 methods. Only two existing method identities are renamed to remove false architecture claims:

- `PublicSharePersistenceConfigurationTests.publicReadUsesExistingQualifiedManagerAndEnforcedReadOnlyTemplate` → `publicReadUsesIsolatedQualifiedManagerAndEnforcedReadOnlyTemplate`.
- `PublicSharesIT.publicProjectionUsesEvidenceOwnerOnSameReadOnlyConnection` → `publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection`.

### New And Modified Method Assertion Map

| Method | Mandatory behavioral assertions |
| --- | --- |
| NEW `SharingRoleLeastPrivilegeIT.v011UpgradeIsPrivilegeOnlyAtomicAndRejectsIncompatibleState` | Clean V010→V011, one migration, 11 rows, repeat zero, direct replay identical; before/after structural catalog and old-role ACL equality; PUBLIC TEMP removal only; injected unsafe attributes/membership/ownership/extra ACL/grant-option/settings/PUBLIC state rejected P0001; appended 22012 failure restores all state |
| NEW `SharingRoleLeastPrivilegeIT.v011PublicReadRoleAndAclAllowlistAreExact` | All role attributes, zero membership/ownership/inheritance paths, exact 2 schema/24 column grants with correct grantor and no grant option; no table/functions/sequence/default/database CREATE/TEMP capability; no PUBLIC application ACL; exact effective column complement |
| NEW `SharingRoleLeastPrivilegeIT.v011PublicReadAllowsOnlyProjectionAndDeniesWritesAndEscalation` | Independent login as public role; every allowed column and real joined projection/time succeeds; every denied column/table/operation below has exact SQLSTATE in separately controlled READ WRITE/READ ONLY phases; RESET ROLE and failed escalation keep same identity; no durable changes |
| NEW `SharingRoleLeastPrivilegeIT.v011LegacyMigrationBytesRemainUnchanged` | All ten checkout-local raw digests captured before/after fixture migration are equal; all ten canonical LF baseline hashes match; external pre/post-edit raw manifest and Git diff prove implementation byte immutability without CRLF-specific CI expectations |
| KEEP `PublicSharePersistenceConfigurationTests.defaultWebAndDisabledCommandProfilesStayDatabaseFree` | Existing profile absence; properties-only runner and nested-record constructor cases below prove missing/blank/wrong credentials fail with the precise deepest sanitized cause, not a missing reader; no fallback datasource |
| RENAME `PublicSharePersistenceConfigurationTests.publicReadUsesIsolatedQualifiedManagerAndEnforcedReadOnlyTemplate` | Six exact bean methods/qualifiers, nested record binding/access/redaction; one-reader command-only vs two-reader public contexts; exact method condition and manager-specific proxied annotations; absent public manager fails before SQL; isolated pool, enforced READ ONLY/REQUIRES_NEW/READ_COMMITTED, prepared/indexed/no logging; pool closes; no writable-role SQL |
| KEEP `PublicShareQueryServiceTests.exactUtf8DigestLookupConcealsUnknownTokens` | Exact UTF-8 digest and unresolved concealment unchanged; direct public/session role mocks; no SET ROLE invocation |
| KEEP `PublicShareQueryServiceTests.stateAndDatabaseTimeChecksNeverMutateGrant` | Existing revoked/expired/window/locked precedence and time/error assertions; READ ONLY/identity guards precede lookup |
| KEEP `PublicShareQueryServiceTests.immutableEvidenceProjectionIsOrderedAndFailClosed` | Existing frozen projection validation and ordering; replace obsolete restore failure canary with wrong session/current identity before/after collaborator, read-only assertion failure and collaborator exception; null/throwing dependencies remain 503 |
| KEEP `PublicShareQueryServiceTests.organizationContentRequiresCurrentMatchingMember` | All existing MemberMatch outcomes and omitted-content behavior; no new session rules |
| KEEP `PublicSharesIT.publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse` | Existing token/routing/error/closed-view canaries and digest lookup |
| KEEP `PublicSharesIT.publicGetKeepsRevokedExpiredAndLockedPageSemantics` | Exact existing public state/time/404/410 precedence and locked-page behavior |
| KEEP `PublicSharesIT.organizationPageRechecksCurrentAccountTenantAndMembership` | All current optional-session and grant-tenant membership outcomes unchanged |
| RENAME `PublicSharesIT.publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection` | Real HTTP 200; captured facts/order/duration unchanged; one public backend PID, both identities narrow, READ ONLY=on, one bound digest, two evidence SELECTs; public MANDATORY proxy outside public transaction fails without command connection |
| KEEP `PublicSharesIT.publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent` | Snapshot/reader/time/database failures remain 503; replace role-switch/restore canaries with public identity assertion failure and revoked required column; restore ACL in finally; command-role membership removal cannot break public GET; no partial response/writes |
| KEEP `PublicSharesIT.repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged` | All existing repeats/prefetch/member/risk observation checks; 11 table hashes unchanged; no forbidden provider invoked |
| KEEP `PublicSharesIT.publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState` | READ ONLY 25006; same-PID public pool reuse/reset after success and failure; no writable-role fallback; bounded concurrent HTTP reads and unchanged snapshots; distinct command pool and subsequent committed create with positive command INSERT count and every INSERT readOnly=off |
| KEEP `PublicSharesIT.publicReadPreservesCreateRevokeRotateListAndMeBehavior` | Existing create/revoke/rotate/list/me outcomes and side effects only on explicit command calls; distinct pool ownership |
| KEEP `SharingQueryServiceIT.queryRuntimeStartsAfterFixtureProvisioning` | Applied history 11 rather than 10; existing single query DSL/datasource/template/repository and runtime-role assertions unchanged |
| KEEP eight V010 methods and all 64 Postgres methods | Existing per-method contracts retained; only cumulative history and explicit V010 scratch target change as specified; no legacy ACL or DML expectation is weakened |

Do not create a synthetic migration-count test. Appendix A freezes every actual existing JUnit identity and its assertion contract (the live method body); all unlisted-for-change methods retain their assertions byte-for-byte. New methods are the four names above only.

In the unit fixture replace the single switch-driven `role` field with independent `currentRole` and `sessionRole`, both initially `sharing_public_read_role`; mock `SELECT current_user, session_user` at indexes 0 and 1, and mock transaction_read_only separately. Replace every InOrder expectation of the three SET ROLE statements with the identity/read-only assertion sequence around the same repository and reader calls. In all four unit methods verify that no SQL containing SET ROLE or either writable role is executed. A wrong-current-role and a wrong-session-role case must each fail before repository access; a post-reader wrong-identity case must suppress content. These are assertions inside existing methods, not extra JUnit methods.

### Exact Configuration-Test Contexts (No Additional JUnit Methods)

In existing `defaultWebAndDisabledCommandProfilesStayDatabaseFree`, retain the disabled/default/web runners using only `PublicSharePersistenceConfiguration.class`. For credential failures, use a **separate properties-only runner**, `new ApplicationContextRunner().withUserConfiguration(PublicReadPropertiesBindingOnly.class)`. Define that nested test helper as `@Configuration(proxyBeanMethods=false)` plus `@EnableConfigurationProperties(PublicSharePersistenceConfiguration.SharingPublicReadPersistenceProperties.class)`, with no bean methods/imports/component scan. Do not register `PublicSharePersistenceConfiguration`, `ShareProjectionReaderConfiguration`, a service, reader, manager, datasource or auto-configuration in this runner. Thus a missing-reader dependency cannot mask binding failure, and Hikari cannot start. Supply the exact prefix and loop over all absent, each missing property, each blank/whitespace-only property, and a nonpublic username; use only in-memory synthetic values. Follow `getStartupFailure().getCause()` to the deepest cause, require exactly `IllegalArgumentException` with message `Explicit dedicated public-read credentials are required` and no cause; assert that message contains none of the supplied URL/username/password canaries. Do not log/dump the outer binding exception (which may describe input). Separately construct the public nested record for the same null/blank/wrong-name cases and assert the same error; valid binding must expose the three unchanged component values and `toString().equals("[REDACTED]")`, with no canary in that string. Inspect the nested class/constructor public access and static nesting, its `@ConfigurationProperties` prefix and the enclosing `@EnableConfigurationProperties` target. This is binding evidence, not a claim that a properties-only context constructs a production pool; the full valid graph is exercised by `PublicSharesIT`.

In existing renamed `publicReadUsesIsolatedQualifiedManagerAndEnforcedReadOnlyTemplate`, retain/update reflection checks for exactly the six §6 `@Bean` methods and their qualifiers. Add reader-only runners using **only** `ShareProjectionReaderConfiguration.class`, active profile `persistence`, `onecall.sharing-command.enabled=true`, and a named `sharingCommandTransactionManager` bean of actual `DataSourceTransactionManager` type backed by a mocked DataSource (no connection needed for fail-before-SQL cases). With public URL absent, `getBeansOfType(ShareableEvidenceProjectionReader.class).keySet()` must equal exactly `{shareableEvidenceProjectionReader}` and by-type lookup must be the same instance as that named bean. With a synthetic public URL present, also register a distinct real `DataSourceTransactionManager` named `publicShareQueryTransactionManager` backed by a different mocked DataSource; the reader key set must equal exactly `{shareableEvidenceProjectionReader, publicShareableEvidenceProjectionReader}` and the named instances must differ. Assert `AopUtils.isAopProxy` for each reader; resolve methods on `AopUtils.getTargetClass` and assert command MANDATORY names `sharingCommandTransactionManager` while public MANDATORY names `publicShareQueryTransactionManager`. Reflect the public factory method condition: exact prefix/name `url`, `matchIfMissing=false`, empty `havingValue`; no `@Primary` on either bean/class. Without a public transaction, invoking the named public proxy with a mocked DSL and nonnull UUID must throw `IllegalTransactionStateException` before any DSL interaction. Repeat the URL-present runner **without** registering `publicShareQueryTransactionManager`: context refresh succeeds (lazy manager-name resolution), but invocation must fail with `NoSuchBeanDefinitionException` naming `publicShareQueryTransactionManager`, before any DSL interaction, never resolve the command manager instead. The real connection success and command-only ambient-transaction denial are required in the renamed existing `PublicSharesIT` projection method below; do not substitute mock-only success or add a JUnit method/class.

### Every Count/Inventory Site

- CI lines 162–163: Surefire 91 stays 91. Exact inventories near lines 338–366: replace the two old method names; append the four V011 names to the existing `SharingRoleLeastPrivilegeIT` set near line 306. `exact_inventory(...,91)` stays; Failsafe argument near 398 changes 126→130. Success text near 399 changes PA15→PA16 and breakdown/combined to 64+10+10+8+12+18+8 / 221. The separate Postgres 64-method expected set and len/source/XML assertions remain 64. No POM include/count/dependency changes.
- README current-summary line 40: 19/91, 7/130, 221; least-privilege suite 12; 11 Flyway entries, nine structural waves; describe PA16's +4 methods and isolated pool, not PA15 command-pool reuse. Line 41 acknowledges V010 and V011 privilege-only increments. Add the exact public datasource property names, fixed narrow username, password-free migration and deployment-only authentication ownership here, not sample credentials.
- README CI summary line 291: 130, 12-method ACL suite, 221; 91/19 and 7 classes unchanged. Lines 300 and 322: relabel old PA15 totals as historical and give current PA16 totals. Line 306: current history becomes eleven, with V010/V011 outside the nine structural waves. Line 15: label the unwired-command statement as historical V010 rather than current runtime, and reference the dedicated public-read boundary; do not rewrite old migration facts. Preserve all genuinely historical PA9/12/14/15 arithmetic, not mass replacement.
- Cumulative migration assertion sites are enumerated in §7. No count changes in Control, docs/14, historical PA15 artifacts, POM, unrelated constraint/FK counts or review directories.

## 9. PostgreSQL 16 Executable Acceptance Protocol

All commands in this section are **implementation-stage requirements, NOT RUN at plan gate**. Use existing disposable `postgres:16` Testcontainers and ephemeral fixture authentication. Capture actual `SHOW server_version` and `server_version_num`, require major 16, zero skipped tests. Do not imply a fixed patch version before execution. Never run on a production database.

### Migration Upgrade, ACL And Structure

In new V011 upgrade method, initialize an isolated cluster with `Flyway.configure().dataSource(...).locations("classpath:db/migration").target("10").load().migrate()`; assert 10 migrations and current `010`. Capture schema/table/column/default/check/index/FK/trigger/function definitions, table data, role attributes/memberships and exploded ACLs. Migrate latest with the same locations: exactly one new migration, current `011`, successful SQL history 11. Compare all structural definitions/data and all preexisting role ACL rows; permitted differences are new role + exact public grants + current-database PUBLIC TEMP removal + V011 Flyway row. No sequence or function catalog change. Validate and repeat (zero).

Catalog queries, executed by fixture admin to avoid information_schema filtering:

```sql
SELECT rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit,
       rolreplication, rolbypassrls, rolconnlimit, rolvaliduntil
FROM pg_roles WHERE rolname='sharing_public_read_role';
-- Exactly (true,false,false,false,false,false,false,-1,NULL), one row.
SELECT roleid, member, grantor, admin_option, inherit_option, set_option
FROM pg_auth_members
WHERE roleid='sharing_public_read_role'::regrole
   OR member='sharing_public_read_role'::regrole;
-- Zero rows; inspect all three PG16 membership options, not only NOINHERIT.
SELECT n.nspname, x.grantor, x.grantee, x.privilege_type, x.is_grantable
FROM pg_namespace n
CROSS JOIN LATERAL aclexplode(coalesce(n.nspacl,acldefault('n',n.nspowner))) x
WHERE x.grantee='sharing_public_read_role'::regrole;
-- sharing/evidence USAGE only, respective owner grantors, no grant option.
SELECT n.nspname, c.relname, a.attname,
       x.grantor, x.grantee, x.privilege_type, x.is_grantable
FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid
JOIN pg_namespace n ON n.oid=c.relnamespace
CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x
WHERE a.attnum>0 AND NOT a.attisdropped
  AND x.grantee='sharing_public_read_role'::regrole;
-- Exactly the 24 tuples in §4, owner grantors, SELECT, false.
SELECT has_database_privilege('sharing_public_read_role',current_database(),'CREATE'),
       has_database_privilege('sharing_public_read_role',current_database(),'TEMPORARY');
-- false,false, including PUBLIC and ownership effects.
```

Compare sorted full tuples, not just row counts. Explode pg_class.relacl with `acldefault('r',relowner)` for relations and `acldefault('S',relowner)` for sequences; pg_proc.proacl with `acldefault('f',proowner)`; pg_database.datacl with `acldefault('d',datdba)`; pg_default_acl.defaclacl, pg_tablespace.spcacl, pg_parameter_acl.paracl, pg_foreign_server.srvacl and large-object metadata ACL/ownership. New-role rows are zero outside the two schema/24 column tuples, including unexpected non-system objects. Check pg_shdepend/pg_db_role_setting for ownership/other-database/settings exclusions. Use role OIDs/grantor OIDs resolved from pg_roles, never hardcoded OIDs. For all application ACL families assert no grantee=0 or is_grantable row conveying public access; ordinary owner grant-option entries are not incorrectly rejected. PUBLIC CONNECT remains an explicit database-default exception; no application PUBLIC exception.

Independently enumerate all application columns and compare effective `has_column_privilege(role,table,column,'SELECT')` to the five-table map; INSERT/UPDATE/REFERENCES always false. Relation-level SELECT/INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER all false. Schema CREATE always false, USAGE true only on sharing/evidence among application schemas. `has_function_privilege` false for every application routine; all sequence privileges false. No new default grants. This checks inherited/PUBLIC access as well as explicit ACL rows.

### Positive And Negative SQL Probes

Set a test-only password on the dedicated role through the fixture admin; never grant it to a probe login. Connect as that role, `setAutoCommit(false)`, first execute `SET TRANSACTION READ WRITE`. This isolates ACL denial from read-only denial. For every negative statement establish a savepoint and rollback to it after asserting SQLException.getSQLState(); finally rollback the outer probe transaction. Merely finding an exception or an empty table is not acceptance.

| Probe | Required result |
| --- | --- |
| `SELECT current_user,session_user,current_setting('transaction_read_only'),pg_backend_pid()` | narrow role twice, `off`, recorded PID in ACL phase |
| Each of 24 permitted column SELECTs, explicit full permitted projection per table, exact repository/evidence SQL in §4, `SELECT clock_timestamp()` | Success; seeded end-to-end test returns all expected facts; no SELECT * |
| Each of 70 forbidden S3 columns from §4; `SELECT *` on each of the eight S3 tables | 42501, including readable tables' denied complements |
| `SELECT id FROM audit.audit_fact`; `SELECT id FROM platform.outbox_event`; `SELECT id FROM platform.idempotency_record` | 42501 |
| For each of all three sharing tables: INSERT DEFAULT VALUES; UPDATE an existing column `SET column=column WHERE false`; DELETE WHERE false; TRUNCATE | 42501 in READ WRITE; no mutation/trigger/FK failure may stand in for permission denial |
| For each of four readable evidence tables and denied evidence_event: INSERT DEFAULT VALUES; UPDATE column=self WHERE false; DELETE WHERE false; TRUNCATE | 42501 in READ WRITE |
| For audit_fact, outbox_event and idempotency_record: INSERT DEFAULT VALUES, UPDATE id=id WHERE false, DELETE WHERE false, TRUNCATE | 42501 |
| For each readable table choose an allowed column (`evidence_id`, `id`, `evidence_id`, `id`, `media_object_id` in §4 table order), execute SELECT with each of FOR UPDATE / FOR NO KEY UPDATE / FOR SHARE / FOR KEY SHARE (20 statements) | 42501 in READ WRITE; allowed SELECT columns but absent UPDATE privilege; assert no row-level lock acquired |
| `CREATE TABLE sharing.pa16_probe(id int)`; `CREATE TABLE evidence.pa16_probe(id int)`; `CREATE TABLE public.pa16_probe(id int)`; `CREATE SCHEMA pa16_probe`; `CREATE TEMP TABLE pa16_probe(id int)` | 42501; the temp case proves database PUBLIC revoke, not just schema ACL |
| `ALTER TABLE sharing.evidence_share_grant ADD COLUMN pa16_probe int`; `DROP TABLE sharing.share_access_event`; `CREATE SEQUENCE sharing.pa16_probe`; `CREATE FUNCTION sharing.pa16_probe() RETURNS int LANGUAGE sql AS 'SELECT 1'` | 42501; rollback and exact structural snapshot prove no DDL |
| `CREATE ROLE pa16_probe_role NOLOGIN`; `CREATE VIEW sharing.pa16_probe_view AS SELECT evidence_id FROM sharing.evidence_share_grant`; `CREATE INDEX pa16_probe_index ON sharing.evidence_share_grant (evidence_id)` | Each 42501 in READ WRITE; denied operations must create no object/role |
| `CREATE DATABASE pa16_probe_database` on a fresh direct public connection with autocommit=true, outside any transaction/savepoint | 42501 (not 25001); assert no database exists, no new role or helper function is provisioned |
| `MERGE INTO sharing.evidence_share_grant AS target USING (VALUES (NULL::uuid)) AS source(evidence_id) ON target.evidence_id=source.evidence_id WHEN MATCHED THEN UPDATE SET evidence_id=source.evidence_id`; `COPY sharing.evidence_share_grant (evidence_id) FROM STDIN` | Each 42501 in READ WRITE; MERGE uses an allowed SELECT column but lacks UPDATE. Send COPY's SQL with JDBC Statement.execute before any data; require privilege rejection before COPY input mode (not a driver unsupported-COPY error). No data is submitted |
| `SET ROLE sharing_command_role`; `SET LOCAL ROLE evidence_role`; `SET ROLE sharing_owner`; `SET ROLE evidence_owner`; `SET SESSION AUTHORIZATION sharing_command_role` | 42501, identity unchanged after savepoint rollback |
| `RESET ROLE`; `SET ROLE NONE` | Success, both identities remain public; no privilege gain |
| `GRANT sharing_command_role TO sharing_public_read_role`; `ALTER ROLE sharing_public_read_role SUPERUSER` | 42501 |
| Calls to existing `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)`, `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)`, both platform idempotency functions | 42501 using typed NULL arguments; schema denial acceptable, effective routine ACL must independently be false |
| Calls to every existing sharing/evidence trigger function with its actual zero-argument signature | 42501, not 0A000 from attempting to invoke an authorized trigger; enumerate pg_proc signatures |

The effective role-attribute/membership/ownership and complete explicit/effective ACL catalog checks are **authoritative for capabilities not individually probed**, including REFERENCES/TRIGGER and unenumerated DDL variants; the finite executable matrix is representative negative-operation evidence, not an exhaustive enumeration of PostgreSQL syntax. Relevant DDL attempts remain denied 42501. Do not use a table GRANT without grant option as a 42501 probe: PostgreSQL can warn and grant nothing; the exact ACL/grant-option assertions prove absence of delegation. Do not pretend a failed CREATE TABLE/CREATE TRIGGER proves which later REFERENCES/TRIGGER check would fail, and do not broaden the public role or add a helper role just to reach that check. No new permanent probe function is permitted in V011 or tests. Sequence denial uses an admin-created disposable sequence with PUBLIC revoked: `SELECT nextval('sharing.pa16_probe_seq')`, `SELECT last_value FROM sharing.pa16_probe_seq` and `SELECT setval('sharing.pa16_probe_seq',1)` each 42501; no sequence is added to production migrations. Drop every admin-created probe object in a finally block before that JUnit method returns, so arbitrary JUnit order cannot pollute legacy object-inventory assertions; do not rely only on container shutdown for cleanup.

Concrete typed calls for the four append/idempotency denial probes (run separately under savepoints, expect 42501; arguments need not reach business validation):

```sql
SELECT platform.outbox_append(NULL::text,NULL::text,NULL::text,NULL::uuid,
    NULL::text,NULL::jsonb,NULL::text,NULL::text);
SELECT audit.append_fact(NULL::timestamptz,NULL::text,NULL::uuid,NULL::text,
    NULL::text,NULL::uuid,NULL::text,NULL::jsonb,NULL::jsonb,NULL::text,NULL::text);
SELECT platform.idempotency_begin(NULL::text,NULL::text,NULL::text);
SELECT platform.idempotency_complete(NULL::uuid,NULL::integer,NULL::text,
    NULL::uuid,NULL::jsonb,NULL::text);
```

For trigger denials the exact existing calls are `sharing.guard_share_grant()`, `sharing.guard_share_admission()`, `sharing.reject_row_mutation()`, `evidence.guard_recording()`, `evidence.guard_evidence_record()`, `evidence.guard_evidence_record_creation_cycle()`, `evidence.guard_evidence_event_creation_cycle()` and `evidence.reject_row_mutation()`; invoke each as SELECT under its own savepoint. Catalog enumeration must equal these sharing/evidence routine signatures, not silently skip a new one.

Then open a **fresh** public transaction, first `SET TRANSACTION READ ONLY`; observe narrow identities and `on`. `UPDATE sharing.evidence_share_grant SET admissions_used=admissions_used WHERE false` must return **25006** (read-only check), whereas the same statement in the earlier READ WRITE phase returned **42501** (ACL). All reads still succeed. In the real service, no SQL can set READ WRITE before lookup; the enforced transaction and first identity query establish the boundary. SQLSTATE precedence is tested on real PG16 rather than inferred from an arbitrary exception. Do not assert that a forbidden statement necessarily returns 42501 when executed in READ ONLY.

### Pool, Proxy, Concurrent HTTP And No Writes

`PublicSharesFixture` logs in as the fixed role after setting only a disposable password. Keep command/customer/query logins and privileges unchanged. Add new three property bindings, get `pool=context.getBean("publicShareQueryDataSource", HikariDataSource.class)` and `dsl=context.getBean("publicShareQueryDsl", DSLContext.class)`. Keep `commandPool=context.getBean("sharingCommandDataSource", HikariDataSource.class)` and `commandDsl=context.getBean("sharingCommandDsl", DSLContext.class)` as distinct references. Retain `executions` as the **public-only** stream and add `final List<Execution> commandExecutions = new CopyOnWriteArrayList<>()`. Attach the retained public listener only to `dsl.configuration()` and a separate listener to `commandDsl.configuration()` before seeding/command execution. Both executeEnd observers use the execution's own JDBC connection to collect current_user, session_user, transaction_read_only, backend PID, prepared-statement status and bind count (add session identity to `Execution` and update its constructor/accessors). The command listener does not run public `tryWrite`/failure canaries. `clearObservations()` clears both lists and existing observation state. Remove `failRestore`; use identity-query and required-column failure canaries only in the public listener. `tryWrite` remains a public-transaction probe expecting 25006. Never log tokens, digests, object identifiers, JDBC credentials or connection properties.

The fixture observation record is exactly `record Execution(String sql, String role, String sessionUser, String readOnly, int pid, boolean prepared, int bindings) { }`. Each listener's JDBC observation query is `SELECT current_user, session_user, current_setting('transaction_read_only'), pg_backend_pid()`; map columns 1–4 to `role`, `sessionUser`, `readOnly`, `pid`. Add the resulting record to its own stream only. This preserves existing `role()` assertions and makes session-identity/command evidence concrete without another production path or test identity.

- With public pool maximum size 1 in the **test only**, request success, borrow again and require same PID, narrow identities, JDBC readOnly=false/autocommit=true and transaction_read_only=off. Repeat after 404/410/503 and a required-column permission failure restored in finally. No command/evidence role is ever observed in the public stream. A separate READ WRITE public borrow still cannot write or assume command/evidence roles.
- In existing renamed `PublicSharesIT.publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection`, use the named public reader at both lookup sites and assert exactly the two reader bean names. Require the public bean is an AOP proxy; without any transaction its invocation raises `IllegalTransactionStateException` with zero new public SQL observations. Execute the same invocation inside the real `sharingCommandTransactionTemplate` with no public transaction: require the same exception, zero new public SQL observations, and no reader SQL on the command stream. Then execute it inside the real `publicShareQueryTransactionTemplate`, passing `fixture.dsl`: require `ReadResult.Found`, READ ONLY=on, both public identities, and the same public backend PID across repository and evidence reads, with no new command executions. Assert the public template's actual manager is the named `publicShareQueryTransactionManager`, distinct from the named command manager. Real HTTP content success supplements this executable public-manager/MANDATORY proof, not a mock/annotation-only substitute. The missing-manager case remains in the existing configuration test above. Existing command projection success continues under its original MANDATORY manager.
- Temporarily remove command login's evidence_role membership; public NONE GET still returns 200, then restore membership in finally. Temporarily revoke a required public column; GET returns sanitized 503, then restore as the owner. These are disposable fixture mutations, not migration membership handling.
- After all public-only assertions/snapshots in `publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState`, clear `commandExecutions`, record public observation count, then explicit command create on the **separate** command pool must commit grant/audit/Outbox and succeed. Collect `commandExecutions.stream().filter(execution -> execution.sql().startsWith("INSERT INTO sharing.evidence_share_grant")).toList()`; require `isNotEmpty()` / `size()>0` **before** asserting every element has `readOnly().equals("off")`, command current_user=`sharing_command_role` and session_user=`commandLogin`. No public-stream-only or empty `allMatch` assertion is acceptable. Require unchanged public observation count during that command and no command/evidence identity in the retained public stream. Prove distinct simultaneously borrowed public/command PIDs and unchanged command login/role restoration. Do not demand public/command same PID. Existing 18 command tests plus list/me regressions remain mandatory.
- Snapshot before/after public-only phases using the existing fixture's sorted complete-row serialization and SHA-256 for exactly: `sharing.evidence_share_grant`, `sharing.share_admission`, `sharing.share_access_event`, `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.media_object`, `evidence.recording`, `evidence.evidence_event`, `audit.audit_fact`, `platform.outbox_event`, `platform.idempotency_record`. Compare every digest and row count for success, missing/expired/revoked, nonmember, dependency failures and prefetch. Restore deliberate admin fixture changes before comparison; exclude explicit subsequent command regression from the public no-write interval.
- Preserve bounded six HTTP tasks on three workers with per-future timeout and identical safe envelopes. Additionally use public test pool size 2 and a barrier across two held transactions to prove overlapping reads on **two distinct public PIDs** with READ ONLY=on, rather than merely queueing six reads through size 1. Barrier waits/timeouts and finally rollback/close ensure no hung CI. Both passes leave the 11-table snapshot identical; no concurrent test mutates lifecycle facts.

### Required Commands And Evidence

Run from `backend`, with the repository's Java 21/Maven 3.9.9 toolchain and reachable disposable Docker. Record actual versions, exit status, XML identities and zero failures/errors/skips. If wrapper CRLF prevents direct execution under WSL, use the existing in-memory CR-stripped invocation, not a wrapper edit.

```bash
./mvnw -B -ntp -Dtest=PublicShareWebTests,PublicShareQueryServiceTests,PublicSharePersistenceConfigurationTests,CustomerApiBoundaryTests,SharingQueryBoundaryTests,ModuleBoundaryTests test
docker info
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT,PublicSharesIT verify
./mvnw -B -ntp clean -Pcontainer-smoke verify
```

Focused Surefire = 24; targeted Failsafe = 12+8=20 (overlapping the full inventory, never add these to 221). Run the unchanged CI source=expected=XML checker against fresh clean reports; require 19/91 and 7/130. Run existing default/web packaged-JAR smokes and all 82 dependency-firewall probes from CI unchanged. Full verify carries every create/revoke/rotate/list/me regression. Execute no implementation test merely to populate this planning artifact. Missing Docker/toolchain/SQLSTATE evidence is an implementation blocker, not a waiver or production claim.

## 10. V001–V010 Raw Immutability Manifest

These are SHA-256 of **actual worktree bytes**, including checkout CRLF. They intentionally differ from historical normalized-LF digest constants. Preserve both: raw bytes for PA16 no-edit proof; existing normalized canonical checks for historical migration identity. Do not normalize files to make one manifest match the other.

| Script | Raw SHA-256 |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| `V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| `V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| `V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| `V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| `V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |

Implementation must capture its own pre-edit raw manifest and compare after all work with `sha256sum backend/src/main/resources/db/migration/V0{01,02,03,04,05,06,07,08,09,10}__*.sql`. On another checkout with different line-ending conversion, use that checkout's pre-edit byte manifest for local immutability and canonical LF constants for portable JUnit resource validation; do not hardcode CRLF-dependent expected bytes into CI. The new digest method reads repository migration paths before/after migration in its fixture and also compares normalized bytes to the following baseline canonical constants, covering V010 as well as the existing nine. It must not invoke Git or require a .git checkout inside JUnit. This records checkout-local byte identity without introducing a platform-dependent green condition.

Canonical LF SHA-256 constants for the new portable ten-script JUnit assertion (existing nine constants remain unchanged):

```text
V001__platform_schemas_roles_idempotency_outbox_audit.sql 946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045
V002__identity_organization_and_employee.sql 534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5
V003__subject_number_and_claim.sql 1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1
V004__catalog_order_and_payment.sql a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc
V005__ivr_task_session_call.sql 2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310
V006__trial_entitlement_issuance_reservation_usage.sql 830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475
V007__activation_and_consent.sql bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659
V008__evidence_media.sql 2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e
V009__sharing.sql ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae
V010__sharing_role_least_privilege.sql fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343
```

## 11. Risks, Stop Conditions And No Drift

| Residual / stop condition | Owner and destination |
| --- | --- |
| Dedicated authentication, pg_hba/TLS, secret distribution/rotation, deployment property mapping, per-cluster role-name collision and post-deploy ACL drift | Database/Security/SRE deployment review. V011 creates no usable password; this gate provisions nothing in production. Existing incompatible global role state fails closed. |
| PUBLIC TEMP revoke may affect consumers outside this repository | Database/SRE deployment compatibility review; no observed repository temp-table dependency. Do not restore PUBLIC TEMP or add public-role TEMP to make deployment pass. Escalate incompatibility to Controller. |
| Shared cluster role plus ACLs in other databases; PUBLIC nonstandard defaults or unexpected schema grants | Database/Security; V011 refuses unsafe existing state. Do not silently expand privileges or normalize unrelated databases. |
| New pool consumes separate connections; no capacity/abuse/load evidence | Backend/SRE capacity and abuse-control gate; no production sizing value is selected here. |
| Spring most-specific MANDATORY override/proxy behavior | Backend implementation owner; executable proxy and same-PID test required. If this mechanism requires command/evidence membership or a broader login, stop and report a blocker; do not weaken MANDATORY or widen scope. |
| PostgreSQL built-in/catalog availability, self-managed password/settings and trusted process/DBA powers | Database/Security deployment threat model. No claim of general SQL sandboxing, zero catalog access or immunity to privileged code/DBA reconfiguration; application data writes/locks/DDL remain denied. |
| Production customer-session verifier, token entropy/KDF/TTL/policy and cloud/media/admission integration | Existing Customer IAM/Security/Product and later work units; unchanged fail-closed PA15 seams, no readiness claim. |
| READ COMMITTED read is not a future admission/media authorization lease | Sharing/Security later admission/media work; current behavior unchanged. |
| Historical docs/14 statements about unwired roles/page-open persistence | Controller/Specification documentation reconciliation, outside candidate; active goal and observation-free GET prevail. |
| Test evidence is local/synthetic; no hosted CI or production execution at plan gate | Controller/Platform: preserve implementation logs and obtain later independent acceptance. |

Goal check: only database privileges for the existing GET, minimal isolated credentials/pool/manager/DSL and existing evidence projection adapter, exact catalog/effective ACL proof with representative executable denial probes and full regressions. No admission, table evolution, function-based read API, framework refactor, cache, policy choice, deployment action or alternate endpoint. The only non-role ACL mutation is the necessary current-database PUBLIC TEMP revoke; it is explicitly justified, tested and deployment-owned, not hidden as readiness. If any binding goal cannot be met within this boundary, stop for Controller rather than granting broader roles or editing protected files.

## 12. Planning Validation And Action Boundary

Original plan-gate validation (historical): read-only filesystem/source/config/history inspection; explicit-GIT_DIR status/branch/HEAD/origin-main verification with `GIT_OPTIONAL_LOCKS=0` and `GIT_NO_LAZY_FETCH=1`; source/CI class-and-method identity extraction; exact SQL predicate/join/column reconciliation; raw migration/Control hashing; inspection for temporary-table use; plan scope/count/hash/whitespace checks. PostgreSQL documentation lookup was attempted then; no retrievable result was used as execution evidence.

Fix-1 validation is recorded in `docs/gateflow/pa16-sharing-public-read-least-privilege-acl-plan-fix-1.md`: both review identities and current adjudication/source were re-read; read-only text/inventory/hash checks reconcile the 16-path allowlist, protected source file sets, bean condition/lookups, dual observers, SQLSTATE matrix, CI/README anchors, exact test identities and migration digests. No Docker, PostgreSQL, Maven, implementation test, deployment or runtime assertion is executed in this fix gate; Reviewer B's separate scratch-PG16 observations are review evidence, not a run by this author.

Protected: Controller change, `.codex`, `.pa16-plan/`, all source/tests/migrations/CI/POM/README and historical/review artifacts. Only this plan and its new fix-1 Markdown artifact are written. No staging, commit, fetch, push, branch/worktree creation, Git config change, PR/issue/comment/reviewer action, merge/approval/ready action, implementation or independent re-review is performed. Gate remains **fix** pending Controller validation; Controller owns any revised intake/freeze/re-review authorization, including reconciliation of its historical 17-path record to 16.

## Appendix A. Exact Existing Method Inventory

The following source-derived identities freeze the current 217 methods. For every unchanged identity, its existing source body is the behavioral assertion contract and remains unchanged except the precisely enumerated history-count sites. The explicit mapping in §8 replaces architecture assertions only for the changed methods; four new V011 methods are added to the ACL class. This appendix is an identity inventory, not a count inferred from historical reports.

### com.onecall.archtests.CustomerMeWebTests (Surefire, 8)

- `authenticatedGetReturnsFrozenCustomerContext` — assert authenticated Get Returns Frozen Customer Context.
- `missingMalformedAndDuplicateBearersReturnFrozen401` — assert missing Malformed And Duplicate Bearers Return Frozen401.
- `clientSelectorsHeadersCookiesAndBodyCannotOverrideContext` — assert client Selectors Headers Cookies And Body Cannot Override Context.
- `reachableProblemsAndResponseHeadersAreClosedAndSanitized` — assert reachable Problems And Response Headers Are Closed And Sanitized.
- `otherPathsMethodsAndAliasesAreNotCustomerOperations` — assert other Paths Methods And Aliases Are Not Customer Operations.
- `concurrentRequestsKeepAccountAndChannelIsolated` — assert concurrent Requests Keep Account And Channel Isolated.
- `applicationNegotiationAndErrorDispatchUseFrozenProblems` — assert application Negotiation And Error Dispatch Use Frozen Problems.
- `reservedProblemVocabularyIsWriterOnly` — assert reserved Problem Vocabulary Is Writer Only.

### com.onecall.archtests.CustomerSharesCommandWebTests (Surefire, 4)

- `createUsesTrustedCallerAndMapsCreatedAndReplay` — assert create Uses Trusted Caller And Maps Created And Replay.
- `revokeAndRotateMapClosedResponseUnions` — assert revoke And Rotate Map Closed Response Unions.
- `invalidCommandAndMissingServiceMapToProblemResponses` — assert invalid Command And Missing Service Map To Problem Responses.
- `commandRoutesDoNotChangeShareListRoute` — assert command Routes Do Not Change Share List Route.

### com.onecall.archtests.CustomerSharesWebTests (Surefire, 10)

- `authenticatedPersonalAndOrganizationGetsUseTrustedCreatorScope` — assert authenticated Personal And Organization Gets Use Trusted Creator Scope.
- `missingInvalidAndDuplicateBearersFailBeforePageParsing` — assert missing Invalid And Duplicate Bearers Fail Before Page Parsing.
- `clientSelectorsAndRequestAttributesCannotOverrideTrustedScope` — assert client Selectors And Request Attributes Cannot Override Trusted Scope.
- `limitAndCursorValidationHaveDeterministicPrecedence` — assert limit And Cursor Validation Have Deterministic Precedence.
- `queryErrorsUseExactClosedProblems` — assert query Errors Use Exact Closed Problems.
- `exactGetAndPostRoutesPassHighestPriorityFilter` — assert exact Get And Post Routes Pass Highest Priority Filter.
- `meRemainsCompatibleWithLegacyAndSelectedSessions` — assert me Remains Compatible With Legacy And Selected Sessions.
- `concurrentRequestsKeepAccountAndSelectedWorkspaceIsolated` — assert concurrent Requests Keep Account And Selected Workspace Isolated.
- `negotiationAndBufferedFailuresPreservePa11Boundary` — assert negotiation And Buffered Failures Preserve Pa11 Boundary.
- `webWithoutSharingPersistenceFailsClosedAndDefaultStaysDatabaseFree` — assert web Without Sharing Persistence Fails Closed And Default Stays Database Free.

### com.onecall.archtests.CustomerWebConfigurationTests (Surefire, 6)

- `defaultRuntimeHasNoCustomerOrServletBeans` — assert default Runtime Has No Customer Or Servlet Beans.
- `webProfileIsExplicitAndMissingAdapterHasNoDatabaseEffects` — assert web Profile Is Explicit And Missing Adapter Has No Database Effects.
- `customerCredentialsAreExplicitAndValidatedBeforeConnection` — assert customer Credentials Are Explicit And Validated Before Connection.
- `customerAndSharingGraphsUseExactQualifiers` — assert customer And Sharing Graphs Use Exact Qualifiers.
- `testProfileAndProductClasspathCannotEnableVerifierBypass` — assert test Profile And Product Classpath Cannot Enable Verifier Bypass.
- `webSettingsDisableUnsafeLoggingAndSessionBehavior` — assert web Settings Disable Unsafe Logging And Session Behavior.

### com.onecall.archtests.OneCallApplicationTests (Surefire, 1)

- `contextLoadsWithoutDatabaseOrSecurityAutoConfiguration` — assert context Loads Without Database Or Security Auto Configuration.

### com.onecall.archtests.PublicSharePersistenceConfigurationTests (Surefire, 2)

- `defaultWebAndDisabledCommandProfilesStayDatabaseFree` — assert default Web And Disabled Command Profiles Stay Database Free.
- `publicReadUsesExistingQualifiedManagerAndEnforcedReadOnlyTemplate` — assert public Read Uses Existing Qualified Manager And Enforced Read Only Template.

### com.onecall.archtests.PublicShareWebTests (Surefire, 6)

- `exactPublicGetRoutePrecedesOptionalAuthentication` — assert exact Public Get Route Precedes Optional Authentication.
- `optionalBearerFailuresKeepFrozenPrecedence` — assert optional Bearer Failures Keep Frozen Precedence.
- `tokenShapeAndLookupFailuresUseClosedProblems` — assert token Shape And Lookup Failures Use Closed Problems.
- `publicDtoIsClosedAndContentIsOmittedForNonmembers` — assert public Dto Is Closed And Content Is Omitted For Nonmembers.
- `memberSessionMatchAndNonmatchNeverLeakContent` — assert member Session Match And Nonmatch Never Leak Content.
- `prefetchAndNegotiationHaveNoAuthorizationSideEffects` — assert prefetch And Negotiation Have No Authorization Side Effects.

### com.onecall.archtests.SharingCommandPersistenceConfigurationTests (Surefire, 2)

- `defaultAndWebProfilesCreateNoCommandDatasource` — assert default And Web Profiles Create No Command Datasource.
- `commandProfileUsesDistinctPoolAndResetsLocalRole` — assert command Profile Uses Distinct Pool And Resets Local Role.

### com.onecall.archtests.SharingPersistenceConfigurationTests (Surefire, 2)

- `defaultProfileCreatesNoPersistenceBeans` — assert default Profile Creates No Persistence Beans.
- `persistenceRequiresExplicitQueryCredentials` — assert persistence Requires Explicit Query Credentials.

### com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests (Surefire, 1)

- `approvedPersistenceFrameworksAndForbiddenStacksAreExact` — assert approved Persistence Frameworks And Forbidden Stacks Are Exact.

### com.onecall.archtests.architecture.CustomerApiBoundaryTests (Surefire, 3)

- `customerApiSignaturesAndProjectionAreExact` — assert customer Api Signatures And Projection Are Exact.
- `customerDependenciesAndReadOnlySqlStayInsideBoundary` — assert customer Dependencies And Read Only Sql Stay Inside Boundary.
- `profileAndVerifierArtifactsCannotEscapeIntoProduction` — assert profile And Verifier Artifacts Cannot Escape Into Production.

### com.onecall.archtests.architecture.ModuleBoundaryTests (Surefire, 8)

- `discoversExactlyTheFrozenModuleSet` — assert discovers Exactly The Frozen Module Set.
- `everyExpectedModuleDeclaresApplicationModule` — assert every Expected Module Declares Application Module.
- `assertsExactAllowedDependencyMapFromPlan62` — assert asserts Exact Allowed Dependency Map From Plan62.
- `discoveredProductionEdgesStayInsideTheConfiguredMap` — assert discovered Production Edges Stay Inside The Configured Map.
- `verifiesNoCyclesOrBoundaryViolations` — assert verifies No Cycles Or Boundary Violations.
- `rejectsCycleFixture` — assert rejects Cycle Fixture.
- `rejectsForbiddenDependencyFixture` — assert rejects Forbidden Dependency Fixture.
- `rejectsInternalPackageAccessFixture` — assert rejects Internal Package Access Fixture.

### com.onecall.archtests.architecture.SharingQueryBoundaryTests (Surefire, 1)

- `sharingQueryApiAndProjectionAreExact` — assert sharing Query Api And Projection Are Exact.

### com.onecall.archtests.container.CustomerMeIT (Failsafe, 10)

- `webRuntimeReadsMigratedDatabaseUnderCustomerRole` — assert web Runtime Reads Migrated Database Under Customer Role.
- `personalContextRequiresCurrentActiveAccount` — assert personal Context Requires Current Active Account.
- `onlyActiveMembershipsUnderActiveOrganizationsAreProjected` — assert only Active Memberships Under Active Organizations Are Projected.
- `workspaceOrderingIsUuidDeterministicAndPersonalFirst` — assert workspace Ordering Is Uuid Deterministic And Personal First.
- `exactGrantsPermitProjectionAndRejectForeignColumnsAndLocks` — assert exact Grants Permit Projection And Reject Foreign Columns And Locks.
- `committedLifecycleChangesAreObservedOnNextRequest` — assert committed Lifecycle Changes Are Observed On Next Request.
- `requestsCannotSelectAnotherAccountOrganizationRoleOrChannel` — assert requests Cannot Select Another Account Organization Role Or Channel.
- `successAndFailuresDoNotWriteAndRestorePoolState` — assert success And Failures Do Not Write And Restore Pool State.
- `webAndPa10PersistenceCoexistWithoutBeanOrRoleLeakage` — assert web And Pa10 Persistence Coexist Without Bean Or Role Leakage.
- `verifierOutageAndDatabaseFailureExposeNoSecrets` — assert verifier Outage And Database Failure Expose No Secrets.

### com.onecall.archtests.container.CustomerShareCommandsIT (Failsafe, 18)

- `commandRoleIsAssertedBeforeFirstCommandRead` — assert command Role Is Asserted Before First Command Read.
- `queryRoleCannotAssumeCommandRole` — assert query Role Cannot Assume Command Role.
- `createPersistsGrantAuditAndOutboxAtomicallyWithoutAccessEvent` — assert create Persists Grant Audit And Outbox Atomically Without Access Event.
- `createReplayReturnsMetadataAndDoesNotPersistOrReturnSecrets` — assert create Replay Returns Metadata And Does Not Persist Or Return Secrets.
- `createRiskAndPolicyChecksPrecedeReplay` — assert create Risk And Policy Checks Precede Replay.
- `createConcurrentSameAndDifferentIdentities` — assert create Concurrent Same And Different Identities.
- `evidenceProjectionUsesCommandTransactionConnection` — assert evidence Projection Uses Command Transaction Connection.
- `createAppendFailureRollsBackGrantAuditAndOutbox` — assert create Append Failure Rolls Back Grant Audit And Outbox.
- `revokeWritesRegisteredEventAuditAndOutboxAtomically` — assert revoke Writes Registered Event Audit And Outbox Atomically.
- `revokeExactReplayAddsNoFactsOrRevision` — assert revoke Exact Replay Adds No Facts Or Revision.
- `revokeChangedReasonTerminalExpiryAndStaleWitnessConflict` — assert revoke Changed Reason Terminal Expiry And Stale Witness Conflict.
- `revokeConcurrentActionsProduceOneTransition` — assert revoke Concurrent Actions Produce One Transition.
- `revokeFailureAtEachFactAppendRollsBackAllEffects` — assert revoke Failure At Each Fact Append Rolls Back All Effects.
- `rotateWritesRegisteredEventAuditAndOutboxAtomically` — assert rotate Writes Registered Event Audit And Outbox Atomically.
- `rotateReplayReturnsNoCodeOrCapabilityToken` — assert rotate Replay Returns No Code Or Capability Token.
- `rotateFailureAtEachFactAppendRollsBackAllEffects` — assert rotate Failure At Each Fact Append Rolls Back All Effects.
- `rotateConcurrentActionsProduceOneTransition` — assert rotate Concurrent Actions Produce One Transition.
- `rotateRequiresExactLockedStateAndGenerationWitness` — assert rotate Requires Exact Locked State And Generation Witness.

### com.onecall.archtests.container.CustomerSharesIT (Failsafe, 8)

- `authenticatedHttpListsPersonalAndOrganizationCreatorMetadata` — assert authenticated Http Lists Personal And Organization Creator Metadata.
- `httpPaginationPreservesTiesAndConcurrentInsertOrder` — assert http Pagination Preserves Ties And Concurrent Insert Order.
- `httpCursorsCannotCrossTrustedAccountOrWorkspace` — assert http Cursors Cannot Cross Trusted Account Or Workspace.
- `committedAccountMembershipAndOrganizationChangesDenyNextPage` — assert committed Account Membership And Organization Changes Deny Next Page.
- `customerAndSharingConnectionsRemainSeparatelyOwned` — assert customer And Sharing Connections Remain Separately Owned.
- `existingAclsAndReadOnlyTransactionsForbidWritesAndForeignReads` — assert existing Acls And Read Only Transactions Forbid Writes And Foreign Reads.
- `successfulAndFailedHttpPagesNeverWriteOrProjectSecrets` — assert successful And Failed Http Pages Never Write Or Project Secrets.
- `missingRolesVerifierAndDatabaseFailuresStaySanitized` — assert missing Roles Verifier And Database Failures Stay Sanitized.

### com.onecall.archtests.container.PostgresContainerIT (Failsafe, 64)

- `v001ThroughV009MigrateCleanPostgres16WithoutSkipping` — assert v001 Through V009 Migrate Clean Postgres16 Without Skipping.
- `v001ThroughV003InventoryMatchesTheFrozenContract` — assert v001 Through V003 Inventory Matches The Frozen Contract.
- `brokenMigrationFailsVisibly` — assert broken Migration Fails Visibly.
- `idempotencyNewReplayConflictAndFinalizeOnce` — assert idempotency New Replay Conflict And Finalize Once.
- `committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow` — assert committed New Idempotency Row Is Alertable And Rolled Back Writes Leave No Row.
- `concurrentIdempotencyRetryObservesOneWinner` — assert concurrent Idempotency Retry Observes One Winner.
- `outboxReplayConflictRollbackAndDenylist` — assert outbox Replay Conflict Rollback And Denylist.
- `auditIsAppendOnlyAndRuntimeWritesAreDenied` — assert audit Is Append Only And Runtime Writes Are Denied.
- `auditReasonAndDenylistRulesAreEnforced` — assert audit Reason And Denylist Rules Are Enforced.
- `functionsHaveFixedSearchPathAndNoTransactionControl` — assert functions Have Fixed Search Path And No Transaction Control.
- `ownerPublicAndRoleExecutionBoundariesAreExact` — assert owner Public And Role Execution Boundaries Are Exact.
- `hostileSearchPathCannotRedirectFunctionEffects` — assert hostile Search Path Cannot Redirect Function Effects.
- `mutableHeadsEnforceLifecycleRevisionAndNamedChecks` — assert mutable Heads Enforce Lifecycle Revision And Named Checks.
- `localImmutabilityGuardsRejectRelationAndRotationChanges` — assert local Immutability Guards Reject Relation And Rotation Changes.
- `identityCollisionAndMembershipRevokeRegrantSemantics` — assert identity Collision And Membership Revoke Regrant Semantics.
- `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner` — assert concurrent Identity And Membership Live Collisions Have One Winner.
- `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied` — assert runtime And Cross Module Privileges Are Exact And Writes Denied.
- `lastEnabledAdminTransactionIsSerializedAndFailsClosed` — assert last Enabled Admin Transaction Is Serialized And Fails Closed.
- `v003SubjectNumberCatalogAndGuardsAreExact` — assert v003 Subject Number Catalog And Guards Are Exact.
- `v003OwnerPublicAndRuntimeAclsAreExact` — assert v003 Owner Public And Runtime Acls Are Exact.
- `subjectOwnerSetOnceAndRealNameSeparation` — assert subject Owner Set Once And Real Name Separation.
- `bindingLifecycleTerminalityAndActiveNumberUniqueness` — assert binding Lifecycle Terminality And Active Number Uniqueness.
- `appendOnlyClaimAndForeignKeyBackstops` — assert append Only Claim And Foreign Key Backstops.
- `riskOpenClearReopenRevisionProtocol` — assert risk Open Clear Reopen Revision Protocol.
- `concurrentActiveBindingUniquenessHasOneWinner` — assert concurrent Active Binding Uniqueness Has One Winner.
- `v004CatalogOrderInventoryAndGuardsAreExact` — assert v004 Catalog Order Inventory And Guards Are Exact.
- `v004OwnerPublicRuntimeAndCrossModuleAclsAreExact` — assert v004 Owner Public Runtime And Cross Module Acls Are Exact.
- `v004CatalogVersionLifecycleAndGuardBackstops` — assert v004 Catalog Version Lifecycle And Guard Backstops.
- `v004OrderPaymentLifecycleAndAppendOnlyBackstops` — assert v004 Order Payment Lifecycle And Append Only Backstops.
- `v004ForeignKeyAndStagedConsentFkBackstops` — assert v004 Foreign Key And Staged Consent Fk Backstops.
- `concurrentCatalogPublicationAndCommerceIdentityHasOneWinner` — assert concurrent Catalog Publication And Commerce Identity Has One Winner.
- `v005IvrTaskSessionCallInventoryAndGuardShapesAreExact` — assert v005 Ivr Task Session Call Inventory And Guard Shapes Are Exact.
- `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact` — assert v005 Owner Public Runtime And Cross Module Acls Are Exact.
- `v005TaskTtlLifecycleAndGuardBackstops` — assert v005 Task Ttl Lifecycle And Guard Backstops.
- `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops` — assert v005 Session Call Canonical Caller And Reciprocal Link Backstops.
- `v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops` — assert v005 Leg Event Decision Frozen Identity And Append Only Backstops.
- `v005ForeignKeyAndDeferredReservationFkBackstops` — assert v005 Foreign Key And Deferred Reservation Fk Backstops.
- `concurrentV005TaskClaimAndActiveCallHasOneWinner` — assert concurrent V005 Task Claim And Active Call Has One Winner.
- `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact` — assert v006 Entitlement Reservation Usage Inventory And Guard Shapes Are Exact.
- `v006OwnerPublicRuntimeAndCrossModuleAclsAreExact` — assert v006 Owner Public Runtime And Cross Module Acls Are Exact.
- `v006IssueFirstTrialGrantReplayConflictAndPolicySemantics` — assert v006 Issue First Trial Grant Replay Conflict And Policy Semantics.
- `v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops` — assert v006 Trial Balance Issuance Lifecycle And Append Only Backstops.
- `v006ReservationUsageForeignKeyAndLateFkBackstops` — assert v006 Reservation Usage Foreign Key And Late Fk Backstops.
- `v006NestedOutboxAtomicityAndReplayRollbackEvidence` — assert v006 Nested Outbox Atomicity And Replay Rollback Evidence.
- `concurrentV006FirstTrialAndReservationIdentityHasOneWinner` — assert concurrent V006 First Trial And Reservation Identity Has One Winner.
- `v007ActivationConsentInventoryAndGuardShapesAreExact` — assert v007 Activation Consent Inventory And Guard Shapes Are Exact.
- `v007OwnerPublicRuntimeAndCrossModuleAclsAreExact` — assert v007 Owner Public Runtime And Cross Module Acls Are Exact.
- `v007ForeignKeyAndLateOrderConsentFkBackstops` — assert v007 Foreign Key And Late Order Consent Fk Backstops.
- `v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops` — assert v007 Consent Shape Defaults Append Only And Rollback Backstops.
- `v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops` — assert v007 Activation Head Transition Revision Matrix And Immutability Backstops.
- `concurrentV007ActivationAndTransitionIdentityHasOneWinner` — assert concurrent V007 Activation And Transition Identity Has One Winner.
- `v008EvidenceInventoryAndGuardShapesAreExact` — assert v008 Evidence Inventory And Guard Shapes Are Exact.
- `v008OwnerPublicRuntimeAndCrossModuleAclsAreExact` — assert v008 Owner Public Runtime And Cross Module Acls Are Exact.
- `v008ForeignKeyAndDeferredCreationCycleFkBackstops` — assert v008 Foreign Key And Deferred Creation Cycle Fk Backstops.
- `v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops` — assert v008 Media Recording Shape Defaults Append Only And Rollback Backstops.
- `v008CreationCycleBothOrdersAndTerminalLifecycleBackstops` — assert v008 Creation Cycle Both Orders And Terminal Lifecycle Backstops.
- `concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner` — assert concurrent V008 Evidence Creation And Recording Identity Has One Winner.
- `v009SharingInventoryAndGuardShapesAreExact` — assert v009 Sharing Inventory And Guard Shapes Are Exact.
- `v009OwnerPublicRuntimeAndCrossModuleAclsAreExact` — assert v009 Owner Public Runtime And Cross Module Acls Are Exact.
- `v009ForeignKeyAndRestrictDeleteBackstops` — assert v009 Foreign Key And Restrict Delete Backstops.
- `v009ShapeDefaultsAndAppendOnlyBackstops` — assert v009 Shape Defaults And Append Only Backstops.
- `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops` — assert v009 Grant Lifecycle Revision Matrix And Immutability Backstops.
- `v009AdmissionCountOnceLimitAndAtomicRollbackBackstops` — assert v009 Admission Count Once Limit And Atomic Rollback Backstops.
- `concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner` — assert concurrent V009 Admission Identity Limit And Lifecycle Has One Winner.

### com.onecall.archtests.container.PublicSharesIT (Failsafe, 8)

- `publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse` — assert public Get Resolves Only Exact Token Digest And Redacts Every Response.
- `publicGetKeepsRevokedExpiredAndLockedPageSemantics` — assert public Get Keeps Revoked Expired And Locked Page Semantics.
- `organizationPageRechecksCurrentAccountTenantAndMembership` — assert organization Page Rechecks Current Account Tenant And Membership.
- `publicProjectionUsesEvidenceOwnerOnSameReadOnlyConnection` — assert public Projection Uses Evidence Owner On Same Read Only Connection.
- `publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent` — assert public Projection And Dependency Failures Fail Closed Without Partial Content.
- `repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged` — assert repeated Page And Prefetch Leave All Persistent Facts Unchanged.
- `publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState` — assert public Read Is Postgres Enforced And Restores Pool Role And Transaction State.
- `publicReadPreservesCreateRevokeRotateListAndMeBehavior` — assert public Read Preserves Create Revoke Rotate List And Me Behavior.

### com.onecall.archtests.container.SharingQueryServiceIT (Failsafe, 10)

- `queryRuntimeStartsAfterFixtureProvisioning` — assert query Runtime Starts After Fixture Provisioning.
- `personalListingIsCreatorScoped` — assert personal Listing Is Creator Scoped.
- `organizationListingIsCreatorAndWorkspaceScoped` — assert organization Listing Is Creator And Workspace Scoped.
- `inactiveAndMissingCreatorContextsFailClosed` — assert inactive And Missing Creator Contexts Fail Closed.
- `keysetTiesAndConcurrentInsertRemainStable` — assert keyset Ties And Concurrent Insert Remain Stable.
- `pageLimitsEmptyAndTailStatesAreExact` — assert page Limits Empty And Tail States Are Exact.
- `invalidAndCrossContextCursorsNeverWidenScope` — assert invalid And Cross Context Cursors Never Widen Scope.
- `listingProjectionNeverReadsOrReturnsSecrets` — assert listing Projection Never Reads Or Returns Secrets.
- `runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure` — assert runtime Role And Pool Cleanup Hold On Success And Failure.
- `committedLifecycleChangesDenyNextQueryAndFailuresRollBack` — assert committed Lifecycle Changes Deny Next Query And Failures Roll Back.

### com.onecall.archtests.container.SharingRoleLeastPrivilegeIT (Failsafe, 8)

- `v010MigrationIdentityAndPostgres16` — assert v010 Migration Identity And Postgres16.
- `v010RoleAttributesAndMembershipContainment` — assert v010 Role Attributes And Membership Containment.
- `v010QueryRoleExactCatalogAcl` — assert v010 Query Role Exact Catalog Acl.
- `v010CommandRoleExactCatalogAcl` — assert v010 Command Role Exact Catalog Acl.
- `v010QueryRolePositiveAndNegativeBehavior` — assert v010 Query Role Positive And Negative Behavior.
- `v010CommandRolePositiveAndNegativeBehavior` — assert v010 Command Role Positive And Negative Behavior.
- `v010PrivilegeFailuresAre42501AndReadOnlyAuthorizedWriteIs25006` — assert v010 Privilege Failures Are42501 And Read Only Authorized Write Is25006.
- `v010LegacyMigrationDigestsRemainCanonical` — assert v010 Legacy Migration Digests Remain Canonical.

### com.onecall.archtests.sharing.CustomerShareCommandServiceTests (Surefire, 6)

- `createBuildsDeterministicIdentityAndFingerprint` — assert create Builds Deterministic Identity And Fingerprint.
- `createChecksRiskAndPolicyBeforeReplay` — assert create Checks Risk And Policy Before Replay.
- `createReplaysMetadataWithoutSecrets` — assert create Replays Metadata Without Secrets.
- `createRejectsChangedFingerprint` — assert create Rejects Changed Fingerprint.
- `revokeReplaysBeforeStatePreconditions` — assert revoke Replays Before State Preconditions.
- `rotateReplayNeverReturnsCode` — assert rotate Replay Never Returns Code.

### com.onecall.archtests.sharing.PublicShareQueryServiceTests (Surefire, 4)

- `exactUtf8DigestLookupConcealsUnknownTokens` — assert exact Utf8 Digest Lookup Conceals Unknown Tokens.
- `stateAndDatabaseTimeChecksNeverMutateGrant` — assert state And Database Time Checks Never Mutate Grant.
- `immutableEvidenceProjectionIsOrderedAndFailClosed` — assert immutable Evidence Projection Is Ordered And Fail Closed.
- `organizationContentRequiresCurrentMatchingMember` — assert organization Content Requires Current Matching Member.

### com.onecall.core.customer_iam.internal.application.CustomerContextServiceTests (Surefire, 6)

- `trustedCallerIsTheOnlyAccountAndChannelSource` — assert trusted Caller Is The Only Account And Channel Source.
- `activeContextHasExactPersonalAndOrganizationProjection` — assert active Context Has Exact Personal And Organization Projection.
- `missingAndDisabledAccountsAreUnauthenticated` — assert missing And Disabled Accounts Are Unauthenticated.
- `invalidProjectionNeverReturnsPartialContext` — assert invalid Projection Never Returns Partial Context.
- `roleAssertionPrecedesRepositoryAndTransactionIsReadOnly` — assert role Assertion Precedes Repository And Transaction Is Read Only.
- `technicalAndProgrammingFailuresAreSanitized` — assert technical And Programming Failures Are Sanitized.

### com.onecall.core.customer_iam.internal.security.CustomerSessionBoundaryTests (Surefire, 7)

- `verifierReceivesOnlyOpaqueBearerAndCustomerAudience` — assert verifier Receives Only Opaque Bearer And Customer Audience.
- `attestedWorkspaceIsCarriedWithoutClientAuthority` — assert attested Workspace Is Carried Without Client Authority.
- `workspaceStructureIsCarriedWithoutChangingAuthentication` — assert workspace Structure Is Carried Without Changing Authentication.
- `invalidRevokedExpiredDisabledAndWrongAudienceSessionsHaveSameDenialShape` — assert invalid Revoked Expired Disabled And Wrong Audience Sessions Have Same Denial Shape.
- `malformedVerifierResultsFailClosed` — assert malformed Verifier Results Fail Closed.
- `missingAmbiguousAndUnavailableVerifiersFailClosed` — assert missing Ambiguous And Unavailable Verifiers Fail Closed.
- `secretWrappersAndPrincipalNeverRetainOrExposeBearer` — assert secret Wrappers And Principal Never Retain Or Expose Bearer.

### com.onecall.core.customer_iam.internal.web.CustomerShareCursorCodecTests (Surefire, 6)

- `personalAndOrganizationCursorsRoundTripCanonicalAnchors` — assert personal And Organization Cursors Round Trip Canonical Anchors.
- `cursorBindingRejectsOtherAccountsAndWorkspaces` — assert cursor Binding Rejects Other Accounts And Workspaces.
- `malformedAndNonCanonicalCursorsAreRejected` — assert malformed And Non Canonical Cursors Are Rejected.
- `anchorBoundsMatchPa10Validation` — assert anchor Bounds Match Pa10 Validation.
- `maximumUnicodeShareIdsFitFrozenWireBound` — assert maximum Unicode Share Ids Fit Frozen Wire Bound.
- `wirePageHasOnlyFrozenFieldsAndNullableCursor` — assert wire Page Has Only Frozen Fields And Nullable Cursor.

### com.onecall.core.sharing.internal.application.CustomerShareQueryServiceTests (Surefire, 8)

- `defaultsAndBoundsAreDeterministic` — assert defaults And Bounds Are Deterministic.
- `trustedContextCannotBeOverridden` — assert trusted Context Cannot Be Overridden.
- `cursorBindingAndAnchorValidationAreDeterministic` — assert cursor Binding And Anchor Validation Are Deterministic.
- `lookaheadUsesLastReturnedRow` — assert lookahead Uses Last Returned Row.
- `emptyAndTailPagesHaveNoCursor` — assert empty And Tail Pages Have No Cursor.
- `projectionComponentsMatchFrozenListing` — assert projection Components Match Frozen Listing.
- `creatorContextDenialDoesNotFetchShares` — assert creator Context Denial Does Not Fetch Shares.
- `technicalFailuresReturnNoPartialPageOrSensitiveText` — assert technical Failures Return No Partial Page Or Sensitive Text.
