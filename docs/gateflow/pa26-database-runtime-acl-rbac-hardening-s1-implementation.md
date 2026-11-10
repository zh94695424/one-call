# PA26 S1 Database Runtime ACL/RBAC Implementation

## 1. Task, Inputs and Boundary

- Task: `pa26-acl-rbac-s1-implementation-gpt-20261111-02`; role: Phaseflow implementation author; provider requested by the task: Codex / GPT `gpt-6.1-sol` / high.
- Date: 2026-11-11 (Asia/Shanghai). Gate: implementation, sole approved composed S1; no code review or later gate entered.
- Input: metadata-free isolated export of accepted controller commit `fb3fe3ec`. Missing Git metadata is intentional; no Git evidence is invented.
- Binding plan: `docs/gateflow/pa26-database-runtime-acl-rbac-hardening-plan.md`, independently measured before edits: 73,811 bytes, 466 LF lines, SHA256 `12490ef70b9cceaa6e0b82970086ee1450036f49b505f2b2ee6b44abed2608cb`.
- Control Active PA26 checkpoint, accepted plan/fix and the required four plan-review artifacts were read in full before implementation. Those inputs remain unchanged; the baseline manifest additionally preserves the overlaid review artifacts.
- Required input hashes are preserved in `E/implementation-inputs.sha256`; the full source baseline is `E/baseline.sha256`.
- Evidence root, abbreviated `E` throughout this report: `/tmp/pa26-acl-rbac-s1-implementation-gpt-20261111-02`. Logs, report copies, command bodies, validator manifests and private disposable-fixture material are outside the repository. Do not publish the private synthetic connection environment file.
- Task exit disposition: **1 / validation-limited**, implementation completed but not all required validators green. `E/aggregate-validation.log/.exit` records the actual aggregate command; individual numeric exits are recorded below. This is not independent approval, mark-ready, live acceptance or production readiness.

### Exact Implementation Changed Paths

```text
backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql
backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java
backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java
backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java
backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java
backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java
backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java
backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java
backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java
backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java
backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java
backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java
backend/ops/postgresql/pa25-runtime-identities.sql
backend/pom.xml
.github/workflows/ci.yml
backend/README.md
docs/14-production-data-model-and-flyway-schema-spec.md
docs/17-production-security-design-spec.md
docs/operations/pa25-production-database-identity-provisioning-runbook.md
docs/gateflow/pa26-database-runtime-acl-rbac-hardening-s1-implementation.md
```

There are exactly these twenty implementation paths: **seventeen existing files and three additions** (V014, its IT, this report). No non-allowlisted original file changed or disappeared. Existing touched files retain their pre-existing line-ending bytes outside patched hunks; inserted/replaced hunks use LF. Immutable V001-V013 raw bytes are untouched. Build/npm generation is confined to normal ignored validation outputs, not additional implementation source. The complete baseline comparison and final per-path bytes/SHA256 are external artifacts `E/final-scope.log` and `E/final-path-metadata.tsv`; no staging or Git operations occurred.

## 2. Source-Derived Result and Privilege Delta

No production Java/configuration behavior, dependency, plugin version, role graph or object owner changed. No new policy registry, role simulator, runtime catalog query, wrapper function, view or future privilege was introduced. Existing service assumption of `evidence_role` is retained rather than widening command privileges.

All final target ACL tuples are explicit, owner-granted and non-grantable; neither target has a table-level privilege:

| Role | Exact schema USAGE | Exact column SELECT | Tuple count |
| --- | --- | --- | --- |
| `customer_iam_role` | `customer_iam`, `organization_access` | `customer_iam.customer_account(id,display_name,status)`; `organization_access.organization(id,display_name,status)`; `organization_access.organization_membership(organization_id,account_id,role,status)` | 2 schema + 10 column = 12 |
| `evidence_role` | `evidence` | `evidence.evidence_record(id,subject_id,status,source_type,source_ref_id,identity_at_capture)`; `evidence.evidence_artifact(evidence_id,media_object_id,artifact_role)`; `evidence.media_object(id,sha256,size_bytes,mime_type)`; `evidence.recording(media_object_id,validated_duration)` | 1 schema + 15 column = 16 |

Grantors respectively are `customer_iam_owner`, `organization_access_owner` and `evidence_owner`. Customer 39 -> 12 and evidence 42 -> 16 replace the complete old 81-tuple pair with the complete new 28-tuple pair. No mixed/partial state is adopted. Seven-role PA25 total is **247**, with the other **219** unchanged: sharing query 23, command 57, public read 26, admission 97, evidence-media reader 16. Actual catalog evidence is `E/catalog-cardinality.log`.

Removed customer privileges: `platform`/`audit` USAGE; four append/idempotency EXECUTEs; all three IAM guard EXECUTEs; IAM table-wide SELECT/INSERT on account/auth_identity/identity_profile; fifteen IAM column UPDATEs. The seven organization projection SELECTs are retained, and three account SELECTs replace table SELECT. Removed evidence privileges: five table SELECT/INSERT pairs; nine UPDATE-column tuples; five local guard EXECUTEs and two append EXECUTEs; ten subject/IVR/entitlement SELECT columns; USAGE on `subject_number`, `ivr`, `entitlement_metering`, `platform`, `audit`. Only the fifteen evidence projection columns remain readable. Auth/profile/event columns, storage locators/keys, other payloads, writes, row locks, grant options, schema CREATE, sequences and application routines are not approved. PostgreSQL built-ins are not globally revoked.

The unchanged six-login / seven-effective-role / four-edge graph is LQ -> sharing, LC -> customer, LW -> command and independently evidence, plus the three frozen direct-login boundaries. Each edge remains ADMIN FALSE / INHERIT FALSE / SET TRUE. Migration-owned NOLOGIN roles retain NULL verifiers; compatible externally provisioned LOGIN/NOINHERIT roles may have non-NULL verifiers, which are preserved privately byte-for-byte. Exact PA25 v1 database-OID/role-OID/slot/name marker bytes are retained. There is no name adoption, marker restamping, credential clearing or destructive repair. External Q17/Q23 responsibilities remain unchanged.

## 3. V014, Byte Integrity and Atomicity

`V014__database_runtime_acl_rbac_hardening.sql`: **47,297 bytes; 891 LF lines; SHA256 `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a`; actual Flyway 12.4.0 checksum `-340359494`**. The checksum was obtained through Flyway after final SQL bytes settled, then frozen in PA25 history; V014 contains no own checksum or manual history stamp. No subsequent SQL byte change was made after freezing it.

V014 checks PG16, superuser and `current_user = session_user`, takes transaction advisory lock `(725025,1)`, validates owner/object identity, exact V001-V013 history, safe attributes/settings/dependencies/role graph/provenance/default/PUBLIC/ACL surfaces, and distinguishes clean from complete credential-assigned PA25 topology. It rejects partial graph or partial/mixed/contaminated old/new ACL state before reconciliation. Only the exact old target surfaces are revoked (no CASCADE), followed by owner-issued final grants. Independent raw/effective postflight verifies grantor/owner/no-grant-option, no write/routine/cross-service leakage, preserved unrelated ACLs and snapshot equality for roles/graph/defaults/owners. No business DDL/DML, role attribute/membership change, owner transfer, sequence/view/function/trigger definition or unrelated repair exists.

The twelve-method suite compares business rows, namespace/relation/column definitions, constraints/indexes, routines including security mode/search path, triggers, owners, default/PUBLIC and non-target ACLs across upgrade/replay. This includes the three IAM invoker guards. Direct SQL is safe twice; normal Flyway replay executes zero migrations. Clean and complete credential-assigned upgrades/replays are separately exercised in the named tests below, with private full identity snapshots preserving role OIDs, markers, verifiers, validity and edge rows.

### V001-V013 Raw-Byte Manifest

Both the before-capture and after-validation files match the accepted plan §9; every result is unchanged. `E/v001-v013.before.sha256`, `E/predecessor-integrity.log` and `E/predecessor-integrity-final.log` are the mechanical evidence. SHA256 is not the Flyway checksum.

| Predecessor filename | Before SHA256 = after SHA256 | Result |
| --- | --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` | unchanged |
| `V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` | unchanged |
| `V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` | unchanged |
| `V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` | unchanged |
| `V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` | unchanged |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` | unchanged |
| `V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` | unchanged |
| `V008__evidence_media.sql` | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` | unchanged |
| `V009__sharing.sql` | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` | unchanged |
| `V010__sharing_role_least_privilege.sql` | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` | unchanged |
| `V011__sharing_public_read_least_privilege.sql` | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` | unchanged |
| `V012__sharing_admission_least_privilege.sql` | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` | unchanged |
| `V013__evidence_media_reader_role.sql` | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` | unchanged |


### PR Fix 1: Checkout-Independent Predecessor Oracle (2026-11-11)

The raw-byte table above is the **accepted Windows CRLF worktree provenance from plan §9**, not a portable checkout oracle or a Git object ID. It remains valid historical before/after evidence; those values are deliberately retained. Hosted `java-runtime` at PR head `8bd89f7bda832fa19519b64a104807f014fcea45` failed on November 10, 2026 UTC (November 11 Asia/Shanghai) because Linux V001 bytes hash to `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`, not the CRLF value. `contract-regressions` passed; that hosted Java run remains a failure, not a retroactive pass.

PR fix 1 changes only the IT oracle: read UTF-8, replace CRLF with LF, then lone CR with LF, encode UTF-8 and SHA256. The independent literal expectations below are not read from Git, V014 output, Flyway history or generated migration metadata. Python byte normalization/hashlib and a separate Perl normalization/GNU `sha256sum` calculation agree for all thirteen files, including the hosted V001 value. SHA256 here is neither a Git blob object ID nor Flyway's checksum.

| Version | Canonical-LF SHA256 (literal IT expectation) |
| --- | --- |
| `V001` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `V002` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `V003` | `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1` |
| `V004` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V005` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `V006` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| `V007` | `bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659` |
| `V008` | `2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e` |
| `V009` | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| `V010` | `fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343` |
| `V011` | `f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b` |
| `V012` | `13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828` |
| `V013` | `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` |

The supplied base/candidate V001-V013 files match byte-for-byte, and the final fix audit checks them against the pre-edit capture. No migration file or Git metadata is written. Thus the accepted Git blobs/content stay unchanged: only interpretation of checkout line endings in the test changes; this metadata-free workspace does not claim an independent Git-object database inspection. V014 is also byte-identical and semantically unchanged.

`PA26-PR37-CI-01` and `PA26-PR37-A-01` are **fixed candidates**, pending PR re-review. Every rejection mutation now supplies its intended first `V014_*_REJECTED` category; exact `P0001`, savepoint rollback and full catalog equality remain. PUBLIC and unexpected-sequence ACLs reach `V014_ACL_REJECTED` before dependency checks; actual default privileges reach `V014_DEFAULT_ACL_REJECTED`. Partial/renamed/stale markers reach provenance before topology/attributes; settings precede attributes. Authority negatives also assert `V014_AUTHORITY_REJECTED`. No ordering change or V014 defect is proposed.

Fix commands, two-form hash provenance, all mutation/category pairs, validation outcomes and residuals are recorded in `pa26-database-runtime-acl-rbac-hardening-pr-fix-1.md`. The original S1 counts, artifact hashes, exit disposition and “stop before code review” statements below remain historical S1 evidence, not current artifact metadata or this fix's gate boundary. This fix stops **before PR re-review**. All existing residual classifications and the privilege/public/runtime contract are preserved. Fix-author validation exits 0: focused V014/PA25 2/32 plus Surefire 36/309; final full clean verify 36/309 + 15/246 = 51/555, zero failures/errors/skips; exact source=CI=XML inventory 51/555, scan 556 and fifteen includes. The actual hash helper passes LF/CRLF/lone-CR/mixed copies and rejects non-line-ending corruption. Logs/XML/manifests are outside the repository at `/mnt/d/codex-temp/pa26-pr37-fix1-evidence-20261111-050751`; original failure evidence remains separate and unchanged. No prior residual is closed by these local successes.

## 4. Test Responsibilities and Runtime Evidence

Exactly twelve ordinary `@Test void` methods were added, with no parameterized/dynamic additions:

```text
v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle
v014FreshChainHasExactHistoryAndImmutablePredecessors
customerContextUsesOnlyApprovedColumnsAcrossAllCurrentStates
sharingProjectionUsesOnlyApprovedEvidenceColumns
customerRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject
evidenceRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject
assumedRolesAndPoolRestorationPreserveRuntimeBoundaries
ownerInvokerGuardsAndSharingWritesRemainOperational
v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects
v014RejectsUnsafeRolesMembershipsOwnershipAndSettingsAtomically
v014RejectsPublicDefaultAndUnexpectedAclDriftAtomically
v014IsPrivilegeOnlyAndLeavesUnrelatedBoundariesEquivalent
```

`v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle` names both clean and complete credential-assigned V013 upgrades; `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects` names both replay variants, including direct SQL twice, normal Flyway zero migrations and successful updated psql apply/check. `v014FreshChainHasExactHistoryAndImmutablePredecessors` separately proves fourteen-history fresh migration and frozen predecessor bytes. The expected final target ACL oracle is a literal independent column/schema specification, not parsed from V014 or PA25.

Positive current paths include actual `JooqCustomerContextRepository` ACTIVE/DISABLED account behavior, absent/multiple workspace projections and disabled-organization/suspended-membership filtering, actual `JooqShareableEvidenceProjectionReader` original/derived projections with/without recording duration, nonempty identity/media projection, and existing service-level sharing/media/customer suites. A genuine validated synthetic recording yields original duration 60 and derived NULL. The writable genuine-login negatives assert exact **42501**, not a read-only surrogate: every removed local column read/write, table-wide read/write, grant/trigger/schema creation, row lock, application routine and foreign-object access. Auth identity/profile, event, storage locator and cross-service access remain denied. Business snapshots remain unchanged.

Separate cases prove **25006** for an otherwise authorized command write in READ ONLY, **P0001** for original owner invoker-guard immutability, and **23503** for a genuine missing parent. Fixtures seed as named owners, force `SET CONSTRAINTS ALL IMMEDIATE` for deferred evidence cycles before owner exit and leave original guards SECURITY INVOKER. Customer account/auth/profile owner inserts and revisioned updates remain valid; real command-role sharing INSERT remains valid. Existing application tests continue to cover command lifecycle and media transport behavior.

Role matrices are based on `session_user`, not current role. LC enters only customer; LW command <-> evidence works in both directions on the same PID/session. Unauthorized owner/direct/query/customer/predefined roles return 42501. Own-login/RESET/NONE and pooled commit/rollback cleanup restore identity and autocommit/read-only state. Actual application missing-edge failure/restoration is strengthened in PA25 without Java changes.

Adversarial migration tests cover superuser/current-session preconditions, unsafe login/target attributes, settings, bad ownership, inherited/admin/no-SET/additional edges, half-provisioned identities, malformed/OID/slot/name markers, PUBLIC schema/table/column/routine exposure, default grants, sequence dependencies, grant option, sensitive/missing columns, partial old ACL and contaminated final ACL. They compare private before/after snapshots around rolled-back rejections, rather than repairing incompatible state.

### Historical Flyway Target Observation

Before altering the historical test oracle, a disposable PG16 observation with locked Flyway recorded `info().all()` = fourteen resources, thirteen SUCCESS/applied predecessor rows, V014 `ABOVE_TARGET` / `isApplied=false`, `pending()` empty and target-capped `validate()` success. `E/flyway-observation-3.log` is the successful observation; draft probes remain separately retained and are not passes.

`PostgresContainerIT` is now explicitly capped at `013`, preserving all 64 methods/oracles, `initialMigration == 5`, thirteen applied history rows ending 013 and zero replay. It still asserts fourteen resolved resources, thirteen applied SUCCESS entries, exactly one unapplied V014 ABOVE_TARGET and empty pending. No pending/resolved oracle was deleted. All other latest-chain suites remain uncapped and run V014.

### Toolchain and Invocation Provenance

- WSL/Linux shell; Eclipse Temurin **21.0.13+9** at `/opt/jdk21`; locked Maven **3.9.9**, Flyway **12.4.0**; Node **22.23.4**, npm **10.9.9**.
- Actual PostgreSQL: **16.15 (Debian 16.15-1.pgdg13+2)**. Testcontainers uses disposable `postgres:16`; inspected digest/image ID `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97` (`E/postgres-image.txt`). Mutable tag is recorded, not silently pinned or redesigned.
- The exported, unchanged `backend/mvnw` contains a CRLF shebang. Every literal requested `./mvnw` build invocation exits **126** (`/bin/sh^M`); corresponding `LABEL.literal.log/.exit` files preserve this. No wrapper edit was allowed. The same locked wrapper body was executed after CR removal **in memory only**, with `$0=./mvnw` so properties resolution is unchanged:

```sh
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/node-v22.23.4-linux-x64/bin:$PATH"
sh -c "$(tr -d '\r' < ./mvnw)" ./mvnw <the exact Maven arguments below>
```

`E/wrapper-inputs.sha256` and `E/wrapper-normalized.sha256` preserve both inputs. The build driver initially repeated the command prefix while formatting its `.command` capture; original captures remain `.command-format-draft`, and corrected `.command` files reconstruct the actual arguments from the unchanged driver and Maven logs. This evidence-formatting correction does not rerun or alter the commands. This is an explicit invocation workaround, not a claim the literal wrapper passed or a dependency/plugin/config change. Recovery for literal invocation belongs to the export/toolchain owner outside this allowlist. Initial failing drafts and initial successful focused reports are preserved separately; only final-source results are claimed below.

| Label | Exact Maven arguments (backend cwd) | Equivalent-wrapper exit | Surefire / Failsafe methods | XML destinations |
| --- | --- | --- | --- | --- |
| `focused-1-final` | `-B -ntp -Pcontainer-smoke -Dit.test=DatabaseRuntimeAclRbacHardeningIT\,ProductionDatabaseIdentityProvisioningIT verify` | 0 | 309 (failures=0, errors=0, skips=0) / 32 (failures=0, errors=0, skips=0) | `E/focused-1-final-reports/surefire-reports`, `E/focused-1-final-reports/failsafe-reports` |
| `focused-2` | `-B -ntp -Pcontainer-smoke -Dit.test=CustomerMeIT\,CustomerSharesIT\,CustomerShareCommandsIT\,SharingQueryServiceIT verify` | 0 | 309 (failures=0, errors=0, skips=0) / 46 (failures=0, errors=0, skips=0) | `E/focused-2-reports/surefire-reports`, `E/focused-2-reports/failsafe-reports` |
| `focused-3` | `-B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT\,PublicSharesIT\,PublicShareAdmissionIT\,SharingAdmissionRoleLeastPrivilegeIT\,EvidenceMediaReadRoleLeastPrivilegeIT\,EvidenceMediaReadIT\,MediaPlaybackIT\,MediaGatewayTransportIT verify` | 0 | 309 (failures=0, errors=0, skips=0) / 104 (failures=0, errors=0, skips=0) | `E/focused-3-reports/surefire-reports`, `E/focused-3-reports/failsafe-reports` |
| `focused-4` | `-B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT verify` | 0 | 309 (failures=0, errors=0, skips=0) / 64 (failures=0, errors=0, skips=0) | `E/focused-4-reports/surefire-reports`, `E/focused-4-reports/failsafe-reports` |
| `full-default` | `-B -ntp verify` | 0 | 309 (failures=0, errors=0, skips=0) / 0 (failures=0, errors=0, skips=0) | `E/full-default-reports/surefire-reports` |
| `full-container` | `-B -ntp -Pcontainer-smoke clean verify` | 1 | 309 (failures=0, errors=0, skips=0) / 246 (failures=1, errors=0, skips=0) | `E/full-container-reports/surefire-reports`, `E/full-container-reports/failsafe-reports` |
| `full-container-retry` | `-B -ntp -Pcontainer-smoke clean verify` | 0 | 309 (failures=0, errors=0, skips=0) / 246 (failures=0, errors=0, skips=0) | `E/full-container-retry-reports/surefire-reports`, `E/full-container-retry-reports/failsafe-reports` |

For each label, `E/LABEL.command`, `.literal.log/.literal.exit`, `.log/.exit` preserve exact requested arguments, literal failure and actual equivalent invocation respectively. Focused runs and full-default have zero failures/errors/skips; the first full-container run has one MediaPlayback failure, retained below. Full-default deliberately does not activate Failsafe. A single unchanged full rerun is separately labeled, never substituted for the failing attempt. Focused XML was copied outside the repository before the next overwrite. `E/focused-1-initial-reports` preserves the initial successful 32-method focus, and `E/draft-new-suite-xml` preserves earlier failing drafts, not acceptance evidence.

### Latest Suites Requiring Zero Changes

| Unchanged latest suite | Methods / result | Exact final full-run XML |
| --- | --- | --- |
| `CustomerMeIT` | 10; failures=0, errors=0, skips=0 | `/tmp/pa26-acl-rbac-s1-implementation-gpt-20261111-02/full-container-retry-reports/failsafe-reports/TEST-com.onecall.archtests.container.CustomerMeIT.xml` |
| `CustomerSharesIT` | 8; failures=0, errors=0, skips=0 | `/tmp/pa26-acl-rbac-s1-implementation-gpt-20261111-02/full-container-retry-reports/failsafe-reports/TEST-com.onecall.archtests.container.CustomerSharesIT.xml` |
| `CustomerShareCommandsIT` | 18; failures=0, errors=0, skips=0 | `/tmp/pa26-acl-rbac-s1-implementation-gpt-20261111-02/full-container-retry-reports/failsafe-reports/TEST-com.onecall.archtests.container.CustomerShareCommandsIT.xml` |
| `MediaPlaybackIT` | 9; failures=0, errors=0, skips=0 | `/tmp/pa26-acl-rbac-s1-implementation-gpt-20261111-02/full-container-retry-reports/failsafe-reports/TEST-com.onecall.archtests.container.MediaPlaybackIT.xml` |
| `MediaGatewayTransportIT` | 8; failures=0, errors=0, skips=0 | `/tmp/pa26-acl-rbac-s1-implementation-gpt-20261111-02/full-container-retry-reports/failsafe-reports/TEST-com.onecall.archtests.container.MediaGatewayTransportIT.xml` |
| `PublicSharesIT` | 8; failures=0, errors=0, skips=0 | `/tmp/pa26-acl-rbac-s1-implementation-gpt-20261111-02/full-container-retry-reports/failsafe-reports/TEST-com.onecall.archtests.container.PublicSharesIT.xml` |

These inspected suites need no new privilege oracle or count edit: their assertions operate through shared owner-seeded fixtures or unchanged service boundaries; the MediaPlayback exception below prevents an unqualified zero-change acceptance claim for that suite. Review B's CustomerMeIT / CustomerSharesIT / CustomerShareCommandsIT / MediaPlaybackIT / MediaGatewayTransportIT question is addressed by latest-V014 execution evidence, not by expanding the write allowlist; independent disposition remains outside this implementation gate. PublicSharesIT is also unchanged. Every other test source outside the allowlist is byte-identical to baseline.

### Initial Full-Run Failure and Unchanged Reproduction

The first full `-Pcontainer-smoke clean verify` run fails `MediaPlaybackIT.witnessExpiryMembershipBindingAndRiskChangesFailClosedWithoutWrites` at line 140: expected `PlaybackStateReader.Result.Allowed`, actual `Denied[reason=SCOPE_MISMATCH]`; its other eight methods pass. `E/full-container-first-failure` immediately preserves the XML/text before any overwrite; the complete first full run remains `E/full-container.log/.exit` and `E/full-container-reports`. This is not an ACL SQLSTATE 42501 or validator baseline failure and is **not** reclassified as a pass or proven pre-existing defect. The same unchanged nine-method suite previously passed in `E/focused-3-reports`.

Read-only inspection of the exact assertion and state reader identifies the logical scope/time comparisons involved, but the available failure lacks per-comparison values: clock jitter or another cause is not proven. No production Java, MediaPlaybackIT, timing assertion, filter or skip is changed. One whole full command is rerun unchanged, with distinct logs/XML, rather than selectively retrying/omitting the failed method. First-attempt inventory is run unchanged and fails its non-green report guard; its `E/inventory.*` evidence is retained. Backend/Test/Sharing owns root-cause and reliability disposition; any required source edit outside the twenty paths needs a plan amendment before editing. A passing rerun does not erase the original failure or constitute independent acceptance.

### Exact Inventory, JAR Smokes and Firewall

The **exact checked-in CI step bodies** were extracted read-only into `E/retry-inventory.command`, `E/retry-jar-smokes.command` and `E/retry-firewall.command`, then executed after the final full build. Inventory exit **0** (`E/retry-inventory.log/.exit`): source = CI = XML, **36/309 Surefire + 15/246 Failsafe = 51/555**, annotation scan **556**, exactly fifteen Failsafe includes, exact named historical sets, twelve PA26 additions, preserved twenty PA25 identities, zero skips/errors/failures. Full XML copies are in `E/full-container-retry-reports`; `E/full-inventory.tsv` additionally enumerates each suite/method count.

Default and opt-in web JAR smokes exit **0** (`E/retry-jar-smokes.log/.exit`), preserving default no-server launch and opt-in web/error-response checks. The literal firewall step exits **126** at its unchanged CRLF `./mvnw` prerequisite (`E/retry-firewall.log/.exit`); its same complete body with **only** the wrapper invocation normalized in memory exits **0** (`E/retry-firewall-normalized-wrapper.command/.log/.exit`). Runtime dependency listing and JAR index prerequisites were actually run; no firewall probe, allowlist, filter, dependency or packaging assertion changed. All **82 initial + 34 additional = 116** adversarial coordinate/JAR probes pass. Generated boot/dependency/JAR evidence is copied to `E/smoke-artifacts`. The final workflow hash in `E/final-path-metadata.tsv` pins the step bodies; this is not a copied incomplete firewall subset.

## 5. Actual PA25 psql Apply/Check/Replay

The updated script requires exactly V001-V014 (filename, description, actual checksum) and the independent literal 247-tuple oracle. The four nonsecret psql variables/modes, output suppression/redaction, six/seven/four graph, creation-only marker semantics, no-adoption and missing-edge-only repair rules remain. No business object ACL repair or credential assignment was added to PA25.

Actual disposable psql execution used `psql -X -w -v ON_ERROR_STOP=1 -v mode=<apply|check> -v sharing_login=<synthetic> -v customer_login=<synthetic> -v command_login=<synthetic> -f backend/ops/postgresql/pa25-runtime-identities.sql`, via container-local synthetic connection configuration. No password/URI/verifier appeared in argv or output. `E/psql-apply-check-replay.log/.exit` records apply -> check -> apply -> check, exit 0. Only fixed committed markers appear: `PA25_APPLY_OK logins=6 effective_roles=7 memberships=4` and `PA25_CHECK_OK logins=6 effective_roles=7 memberships=4`.

`E/psql-credentialed-negative-evidence.log/.exit` adds actual synthetic non-NULL LC/LW verifier evidence (boolean only), apply/check twice with `state_preserved=true`, two direct credentialed V014 SQL replays with full private state preservation, then Flyway replay `migrationsExecuted=0`. Comparison retains OIDs, marker bytes, passwords/verifiers, validity, graph, ACL and business state without printing secret values. Each of stale V013 history, restored old INSERT, missing projection column and wrong provenance is tried in **both** psql modes: expected exit **3**, respective fixed `PA25_HISTORY_REJECTED`, `PA25_ACL_REJECTED`, `PA25_ACL_REJECTED`, `PA25_PROVENANCE_REJECTED`, all `state_preserved=true`; final restored check exits 0. The helper itself exits 0. Existing PA25 tests additionally reject malformed/duplicate/future V014 history before provisioning, restored routines/sensitive grants/grant options, PUBLIC/default drift, unsafe identities/membership and redaction violations; all original twenty method identities remain.

These are local synthetic proofs, not production TCP/TLS/pg_hba authentication, rotation, old-password/session invalidation or live credential acceptance. Owned standalone disposable container cleanup is recorded in `E/disposable-cleanup.log`; no unrelated container is intentionally touched.

## 6. Documentation Supersession Ledger

All four allowlisted docs state the exact 12/16/247 boundary, unchanged graph/owners/provenance, no Java switch or future writes, V001 -> V014 -> PA25 apply -> external credential assignment -> check/connection acceptance, current inventory, remaining sensitivity and live NOT EXECUTED / readiness NOT CLAIMED. README and runbook obsolete current assertions are directly replaced; data/security historical model text and machine markers are not rewritten. Unrelated role claims remain historical/current as before.

| Input anchor / document | Old current claim | Exact current replacement / preserved context |
| --- | --- | --- |
| `backend/README.md`: pre-S1 line 9 and Current PA25 section | Customer/evidence retain write/routine capability; no V014; 300-tuple-era boundary / 50/543 inventory | Former capability explicitly historical; current exact projection table, 247 tuples, V014 and 51/555 inventory. No runtime Flyway bean is added. |
| PA25 runbook: pre-S1 line 28 ACK/risk statement | Customer/evidence have existing write/routines; LW inherits broader evidence capability, risk remains with Backend/Security | Superseded by projection-only evidence/customer ACLs; LW assumption retained without historical writes/routines; ACLs, not READ ONLY, prove denial. PA25 still does not repair object ACLs. |
| PA25 runbook: boundary/order/history/oracle/inventory | No V014; V001-V013 thirteen-row history; 300 tuples; 50/543 | V001-V014 fourteen rows and final checksum; 247 tuples; 51/555, scan 556, fifteen includes. Frozen V001-V013 bytes and external Q17/Q23 controls retained. |
| `docs/17-production-security-design-spec.md`: PA26 current boundary before original model | Historical foundation-role structural access could be read as current broad target authority | Exact named projection and removal ledger explicitly controls both targets; original machine-frozen security model and unrelated roles remain unchanged. |

### Complete Historical-to-Current Supersession Ledger

Input anchors refer to the accepted pre-S1 document, not shifted line numbers. The original text and machine-frozen blocks below remain historical. The current table above is the binding replacement at every listed target-role capability occurrence; unrelated-role portions of shared rows are expressly untouched.

| Pre-S1 input anchors / sections | Historical claim retained as history | Current disposition |
| --- | --- | --- |
| 245, 272, 280, 296–298; schema/owner/table catalog | V008/V009 role creation, module ownership and runtime labels | Identity/ownership history unchanged; labels do not imply writes or table-wide reads. |
| 329–333; evidence table catalog | Evidence media/artifact/event INSERT and recording/record lifecycle UPDATE | No target writes; only the evidence projection row above. Event has no read. |
| 626, 664, 700; customer account/auth/profile dictionaries | Customer account/profile/identity UPDATE and profile verification workflow | No customer runtime mutation, auth/profile read or local guard EXECUTE. Historical workflows are not implemented current privileges. |
| 2087, 2225, 2262; evidence dictionaries | Evidence table-wide SELECT/INSERT on media, artifacts and events | Exact four media columns / three artifact columns; no event SELECT or INSERT. |
| 3074, 3320, 3540, 3554–3555, 4200; workspace/cross-module reads | Customer organization USAGE and seven SELECT columns; no foreign writes/row locks | Retained unchanged, plus only account(id,display_name,status); no lifecycle-lock privilege inferred. |
| 3538–3539, 3542 and adjacent platform/audit routine rows in §10.7 | Customer platform/audit USAGE; idempotency_begin/idempotency_complete/outbox_append/append_fact EXECUTE | All removed for customer only; all other grantees remain untouched. |
| 3564–3566; IAM owned-table ACL rows | Customer table SELECT/INSERT and fifteen UPDATE columns | Replaced by three account column SELECTs; auth/profile entirely inaccessible. |
| 3599; guard-function execution description | Customer local guard EXECUTE | Removed for customer; owner-trigger invocation and all unrelated module grants unchanged. |
| 3994, 3996–4000; §10.21 evidence/cross-schema rows | Evidence own USAGE plus five cross-service USAGE/append capabilities | Only evidence schema USAGE remains for evidence; shared-row unrelated grantees unchanged. |
| 4008–4012; evidence owned-table ACL rows | Five table SELECT/INSERT pairs and nine UPDATE columns | No table/write grants; exactly fifteen evidence projection columns. |
| 4021–4024; evidence foreign SELECT rows | Subject id, IVR call/session/subject and usage-reservation columns | All ten foreign column grants removed, not deferred future privileges. |
| 4036; sharing projection collaborator | Owner-local projection under evidence role, no storage locator exposure | Actual Java projection reader uses the exact fifteen columns above; no new SQL wrapper/function is created. |
| 4037; capability summary | Evidence uses platform/audit append functions | Superseded: no EXECUTE or USAGE in those schemas for evidence; sharing-role claims untouched. |
| 4058; deferred creation-cycle explanation | Evidence runtime inserts with broad local SELECT for invoker triggers | Historical V008 fixture capability only. Current owner writes keep all original invoker/deferred-FK behavior; runtime readers cannot insert. |
| 4254, 4264–4265, 4276; V008 migration order | Evidence schema creation, guard EXECUTE, cross-service USAGE/read/append grants | Immutable migration history only; V014 subsequently removes those runtime capabilities. Role/DDL order and frozen model remain unchanged. |

The docs/14 input-anchor ledger above is also present immediately before its historical model, with precise links to the named current projection table. A generic unlinked overlay is not the only supersession evidence. Original input occurrences are retained externally in `E/docs14-claims.before.txt`.

## 7. Contract Validators and Known Baseline Failures

Prerequisite trees were present and actually used; none was presumed missing. A1 was generated by the checked-in locked toolchain using `(cd api/openapi && npm ci && npm run check:s3)`, exit **0**, with Node 22.23.4/npm 10.9.9. `E/a1-baseline.log/.exit` and `E/A1.inputs.sha256` preserve provenance. Generated `tmp/openapi-client/onecall-v1.bundle.json` (external copy `E/onecall-v1.bundle.json`) SHA256: `6b93e097978e45056ee5378aebb03c3a6cf518fa57c26573a52c1ab0031bc220`.

The exact checked-in CI validator commands were extracted and executed before the documentation edits and again against the final touched docs and real bundle; full commands are `E/{baseline,final}-<validator>.command`, sanitized outputs `.log`, numeric exits `.exit`. They were not skipped or weakened. Final command bodies:

```sh
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
```

```sh
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures
```

```sh
node acceptance/validate.mjs --stage s1 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures
```

```sh
node acceptance/validate.mjs --stage s2 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures \
  --check-doc-links
```

| Validator | Before-doc baseline exit | Final exit | Status / exact evidence prefix |
| --- | --- | --- | --- |
| A2-s3 | 1 | 1 | reproduced baseline failure; `E/final-A2-s3` |
| A3-s3 | 0 | 0 | PASS; `E/final-A3-s3` |
| A5-s1 | 2 | 2 | reproduced baseline failure; `E/final-A5-s1` |
| A5-s2 | 2 | 2 | reproduced baseline failure; `E/final-A5-s2` |

A2 actually runs the fixtures (25 passed, zero fixture failures) but fails A0 integration: missing named CHECK/column dictionary references and model references, `a0_status_drift=8`, `a0_registry_drift=6`, `a0_column_drift=40`. Its baseline and final logs are byte-identical. A5 s1 and s2 actually invoke the validator but fail `invalid-arguments: state table header changed in section 7.1` before successful acceptance validation; baseline/final logs are byte-identical. They are **reproduced pre-existing failures, not passes or missing-prerequisite exemptions**. A3 s3 passes 23 fixtures with zero drift. No unrelated frozen model repair was authorized or attempted.

Recovery/ownership: Protocol/Data/Backend must disposition the A2/A5 baseline fixture/model/header mismatch in a separately authorized change and rerun the exact input commands; Security owns A3 disposition and external security acceptance. These failures prevent an all-green contract-validation claim. The CRLF wrapper invocation issue is separately owned by the export/toolchain custodian; unchanged wrapper input hashes and exact exit126 logs identify its cause and in-memory recovery. No Docker/network prerequisite failure blocked PostgreSQL tests.

Per-validator exact file manifests (not merely directory names) and their SHA256 are `E/A1.inputs.sha256`, `E/A2-s3.inputs.sha256`, `E/A3-s3.inputs.sha256`, `E/A5-s1.inputs.sha256`, `E/A5-s2.inputs.sha256`:

| Manifest | Files | Final manifest SHA256 |
| --- | --- | --- |
| `A1.inputs.sha256` | 12 | `dca53a72a6f68ade6b51725c5d70d643ad087ee59883b94ca78d6814452f4c97` |
| `A2-s3.inputs.sha256` | 31 | `4fb6c8b3e8533ea16ceaa2220d6b0049884ce099c47a355c7bf42a3f54288581` |
| `A3-s3.inputs.sha256` | 55 | `579b2086f2f4359964f88f398539f31ef947ed78eb98e4628b87fca554a946a7` |
| `A5-s1.inputs.sha256` | 73 | `d0b5329e6722cb9d4918f82683b260503dce82b3833d03ff72d8b9ebaf459c36` |
| `A5-s2.inputs.sha256` | 73 | `d0b5329e6722cb9d4918f82683b260503dce82b3833d03ff72d8b9ebaf459c36` |

Baseline exact input manifests are additionally reconstructed as `E/baseline-<validator>.inputs.sha256` from the original source hash manifest for pre-edit docs/14 and docs/17, retaining the already-updated CI and byte-identical remaining inputs/bundle. Baseline validator execution predates both doc edits and postdates the CI edit (filesystem timestamps preserved); the reconstruction is labeled, not presented as a contemporaneous capture. `E/baseline-validator-manifests.txt` records their hashes; `E/validator-baseline-comparison.log` proves baseline/final output byte equality for all four validators.

## 8. Finding and Decision Preservation

| Accepted finding / precision decision | S1 treatment / evidence | Residual owner |
| --- | --- | --- |
| PA26-PLAN-A-01 | Real A1 generation and actual A2/A3/A5 commands/input manifests/results above; baseline failures individually reproduced, not a blanket blocked excuse. | Backend/Protocol/Data; Security for A3 |
| PA26-PLAN-A-02 | Clean and complete credential-assigned V013 upgrade/replay are named in the twelve-method suite; private full identity snapshots and actual psql preserve non-NULL verifier/provenance/OID/edges. | Backend/Database; real custody Platform/Security/Operations |
| PA26-PLAN-A-03 | Session-user LC/LW matrices, positive command <-> evidence, genuine 42501 forbidden switches and pool restoration without membership/Java changes. | Backend/Database |
| PA26-PLAN-B-01 | Complete bounded current-claim supersession ledger, including README:9/runbook:28 and each docs/14 anchor group; frozen history/machine markers and other roles preserved. | Backend/Security/Data |
| PA26-PLAN-B-02 | Actual locked-Flyway info/all/pending/V014/validate observation before assertion edits; exact 013 cap, 64 identities, initial five, tail013, replay0 and strict resolved/pending retained. | Backend/Database |
| Clean vs complete / half-provisioning | V014 itself accepts only clean or complete marked graph before ACL mutation; partial identities/edges reject atomically. | Database |
| Current-final-state IAM guards | Named owner test validates writes/guard errors/FK; structural test compares exact guard definitions/owners/invoker/security/path/triggers. | Backend/Database |
| Firewall wording precision | Unchanged source assertions prove 82 initial + 34 additional = 116. No probe/filter/allowlist change or independent Controller disposition of prior contradictory wording is claimed. | Controller; Backend/CI evidence |
| Latest-suite zero-change question | Named latest execution/disposition table above, including all five review-B examples; no unallowlisted suite edited. | Backend |
| No future feature / over-design | Only required privilege removal; no Java/config change, role-graph change, future write privileges, duplicate runtime authority, business DDL/DML or credential manager. | Future Backend/Database/Security feature owner |

## 9. Residuals, Scope and Handoff

- Platform/Database/Security -> Q17/Q23 deployment/security readiness: actual login names, real credentials, migrator superuser custody, protected role-comment writers, exclusive cluster administration, pg_hba/TLS and real TCP connection acceptance remain external and **NOT EXECUTED**.
- Security/Operations -> PA25 live acceptance: credential rotation, old-password denial, existing-session termination, pool drain and outage recovery remain external; local replay is not rotation.
- Backend/CI/Database -> hosted resource ceiling and mutable postgres tag: actual local patch/digest are recorded; this does not certify hosted CI capacity or alter image policy.
- Platform/Database/Security -> incompatible pre-existing roles/defaults/provenance: reject and stop, separately approve remediation; no adoption/destructive cleanup.
- Sharing/Evidence/Security -> retained row-level sensitivity: `identity_at_capture` and cross-row projections remain sensitive; no tenant RLS or broad content declassification claim.
- Backend/Test/Sharing -> initial full-run MediaPlayback SCOPE_MISMATCH assertion failure: focused success and the unchanged full rerun are separate evidence, not proof of root cause or an exemption. Do not edit outside the allowlist without an amendment.
- Protocol/Data/Backend -> reproduced A2/A5 baseline failures; export/toolchain owner -> literal CRLF wrapper failure. Exact commands/input hashes/status/logs and recovery above remain unresolved external dispositions; no all-green acceptance claim.
- Future customer/profile/evidence writes require a separately approved feature and privilege-only migration; no capability is reserved by PA26.
- Only the twenty listed implementation paths changed. V001-V013 and all production Java/configuration, OpenAPI source, frontend/deployment source, Control, plan/fix and reviews remain byte-identical to the captured baseline. No source dependency/plugin/filter/skip/firewall allowlist changed. No commit/stage/push/branch/PR/merge/approval/mark-ready/reviewer request/external comment/issue mutation or later gate occurred. **Stop before code review.**

### Artifact Metadata and Exit Disposition

This report's path is `docs/gateflow/pa26-database-runtime-acl-rbac-hardening-s1-implementation.md`. Its final bytes/LF-line/SHA256 are measured **externally after the final write** in `E/implementation-report-metadata.txt` and the task handoff; no self-referential hash is embedded. The nineteen other implementation files' finalized byte/hash rows are below; the external final manifest also includes this report.

| Changed implementation path (excluding this report) | Bytes | SHA256 |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql` | 47297 | `e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a` |
| `backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java` | 48277 | `05105890ba69f324af8a538d4d87547c6d602a6a391767bfe693e12cfd71227c` |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | 1797161 | `b57d7e5f3e642120988f3ec8644dc5c1fe3997b756e32b58bb3c13f9e90bf55b` |
| `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java` | 19230 | `0f9f9d75401a83fe364b886dfaf93816b5dc09975079f8769b46830c9bdb46c2` |
| `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java` | 19535 | `00a63ad5830326a7be73de73486e55bf6a4f534847e1c6da80725fd65c348a36` |
| `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java` | 32365 | `911f46f09e41da12f582ceefbb584658888792d43b648b6f11378013c8922875` |
| `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java` | 66246 | `996b9ee3c927df72d00a025fe8463fd7f41690e944d08e634078754ed71a81c6` |
| `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java` | 22797 | `3001b51d712d691e04347910b636f78ef0e0ef0a92a3a5cc52ebe9dbce328098` |
| `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java` | 21915 | `2ac5b63a3a18ddfb7dc5714cf1e12d7830fbdb44d55b59df584577d8db8d8e89` |
| `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | 130244 | `bc0c4673d47617a056006d14f0b64c511a6bf8d9ce8794a1f014520bfaaf00e3` |
| `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java` | 29638 | `80594b284c2f99aab5061f0d7431fee30641ab09c83efeb14a675a87eefe8f15` |
| `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java` | 102011 | `322c7ddd7e8aa2beba34fa5c6bd333764a98523b38a1811cfac4c686be6fbd1a` |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | 71137 | `cafabf54afaece737d1df1c269c8d285b01970f8a1c8c0e7b8e1c64321f3d6ff` |
| `backend/pom.xml` | 8646 | `3ccefed0c5b108cd6572716fcf0405345602e336688938387515cd0462eda326` |
| `.github/workflows/ci.yml` | 103515 | `9245c7c8013221da7b4a348d8974310c90d8cf102444b47ad196211599a899b6` |
| `backend/README.md` | 84880 | `54d8715aa950120b6a473e12945e01008d6e7bc8064e21ab761aa1533f02cb3c` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | 910646 | `5dee313c067f0614ceff657fe2e7427cc43bec6c0e7cfab634ef5d631fead2c4` |
| `docs/17-production-security-design-spec.md` | 91594 | `f0f1323a264948fe82ceebe20433d6b6f1c8955d6f86e0adfa55bac40d345bd1` |
| `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | 18239 | `2280c67db2c7403f946bd54361ed2abd0732421bbc2d470872affab493e6af33` |

**Exit disposition:** S1 implementation and PostgreSQL/focused/full/inventory/psql/JAR/firewall validation completed; focused/default Maven commands and the final unchanged full rerun plus executable acceptance checks exit 0; the initial full-container run exits 1 with the preserved MediaPlayback assertion failure. Overall required validation is **NOT ALL GREEN**: A2 s3 exits 1 and A5 s1/s2 exit 2 with reproduced pre-existing errors; literal wrapper commands exit 126, with unchanged-wrapper in-memory equivalents passing. The initial full-run MediaPlayback failure remains an additional unresolved reliability finding despite focused/final rerun success. These are expressly reported limitations, not manufactured passes. No all-green gate, live production acceptance, readiness or later-gate status is asserted.
