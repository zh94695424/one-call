# PA26 database runtime ACL/RBAC hardening plan

## 1. Authority, goal and gate

- Work unit: `PA26-database-runtime-acl-rbac-hardening`.
- Planning task: `pa26-acl-rbac-plan-gpt-20261111-02`; provider: Codex / OpenAI GPT. The controller's dispatch/model provenance is authoritative; this artifact does not independently attest a model identifier.
- Date: 2026-11-11. Input: controller export of HEAD `eabf3db1`, without Git metadata.
- Binding authority: `docs/gateflow/project-execution-control.md:14`, especially the accepted Goal Confirmation at lines 21-29. This plan is subordinate to that current-implemented-runtime-only contract, not to historical future-service privilege matrices.
- Sole planning output: `docs/gateflow/pa26-database-runtime-acl-rbac-hardening-plan.md`.
- Gate: plan authoring only. No implementation, plan review, acceptance, deployment, PR operation or external mutation is performed or authorized by authoring this document.
- Cumulative fix candidate: task `pa26-acl-rbac-plan-fix1-gpt-20261111-01`, Codex / GPT, dispatch profile `gpt-6.1-sol` / high, 2026-11-11. The Active resume checkpoint's accepted Goal Confirmation and Controller adjudication govern this fix. Reviewed input SHA256: `FCAC43B25658D2577A83E898BB1C429128D93016F00835F56BD564CAFFDF520A`; reviews: `docs/reviews/plan-review-20261111-012530.md` and `docs/reviews/plan-review-20261111-014448.md`. Findings below are fixed candidates, not independently accepted closures. This fix gate authorizes only this cumulative plan and `docs/gateflow/pa26-database-runtime-acl-rbac-hardening-plan-fix-1.md`; stop before re-review.

**Goal.** Derive the exact current Java/SQL requirements of `customer_iam_role` and `evidence_role`, replace their broad frozen privileges with the minimum executable privileges, and preserve all implemented public behavior. Ship exactly one append-only, privilege-only V014 on PostgreSQL 16. Keep PA25 identity provisioning/checking consistent with it.

**Motivation.** Current runtime reads do not justify credentials/identity-profile access, storage locators, evidence lifecycle writes, cross-service reads, or append-function capabilities. Merely making a datasource read-only would leave these capabilities usable through another transaction. Remove database authorization, not just application opportunities to use it.

**Binding success signal.** Disposable PostgreSQL 16 proves the exact schema/table/column/routine ACLs and membership graph; current customer/sharing/evidence paths succeed; removed sensitive reads, writes, cross-service privileges, unexpected role assumptions and unassumed-role access fail closed. Application privilege failures are SQLSTATE `42501`, distinct from read-only `25006`, business guards `P0001`, and integrity failures. V001-V013 bytes remain unchanged, V014 replays, and PA25 apply/check, its independent ACL oracle and exact inventory agree. This is not production acceptance or live readiness.

**Non-goals.** No new business write path, authentication/profile workflow, evidence uploader/solidifier, permission matrix, schema/table/column/view/function/trigger/business constraint, SECURITY DEFINER wrapper, generic RBAC framework, credential provisioning capability, public API/response change, frontend/protocol/deployment change or future-feature privilege reservation. Do not redo PA13 or change `sharing_role`, `sharing_command_role`, `sharing_public_read_role`, `sharing_admission_role`, or `evidence_media_reader_role`. No production credentials, provider access or Docker operation is needed for this planning gate.

## 2. Source-derived judgment and design alignment

Line references in this section describe the exported input, not future edited line numbers.

### 2.1 Implemented paths, not table ownership, determine grants

| Decision | Direct evidence | Consequence |
| --- | --- | --- |
| Customer role is a projection reader | `backend/src/main/java/com/onecall/core/customer_iam/internal/application/CustomerContextService.java:29` runs the transaction, fixes and asserts `SET LOCAL ROLE customer_iam_role`; `backend/src/main/java/com/onecall/core/customer_iam/internal/persistence/JooqCustomerContextRepository.java:18` contains its only implemented SQL | Keep only the three account, three organization and four membership columns used in SELECT, join, filters and ordering. No INSERT, UPDATE, append routine or guard execution is required. |
| Evidence role is currently a share-projection reader | `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java:121` asserts command role, reads context, switches at lines 130-133, invokes the reader, then restores command role in `finally` | The evidence assumption is necessary and narrowly scoped to the projection. Keep the command login's evidence edge. Removing it would break implemented share creation. |
| Evidence projection needs exactly fifteen columns | `backend/src/main/java/com/onecall/core/evidence/internal/shareprojection/JooqShareableEvidenceProjectionReader.java:18` reads six evidence-record columns and joins artifact/media/recording using nine further columns | Include join/predicate columns, not just returned fields. Do not grant `recording.id` or `recording.status`: this reader joins by `media_object_id`, unlike the separate media reader. |
| Command writes use another effective role | `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqSharingCommandRepository.java:34` switches role; lines 74-144 read IAM/evidence/subject context as command role, lines 160-186 insert/lock grants; service line 133 restores before later work | Sharing INSERT/UPDATE, advisory locks, row locks, outbox and audit requirements do not justify customer/evidence-role write privileges. Preserve command grants unchanged. |
| Media resolution is not evidence_role use | `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/JooqEvidenceMediaObjectReader.java:29`; `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql:8` | Preserve the separate direct-login media reader's fifteen-column model, including recording id/status. Do not union its columns into evidence_role. |
| Role membership is session authority, not Java module dependency | `backend/ops/postgresql/pa25-runtime-identities.sql:35`, `backend/ops/postgresql/pa25-runtime-identities.sql:2920`, and the four-edge apply loop near line 3144; `backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java:209` proves same connection and missing-edge denial | Preserve three external NOINHERIT login slots, four effective NOLOGIN roles, three direct LOGIN roles, and four edges. No Java change is needed or authorized. |

Repository-wide role-token and SQL inspection found no implemented Java INSERT/UPDATE path under either target role. Customer writes in the large historical PostgreSQL suite are database-foundation fixtures/contracts, not an implemented customer mutation service. Evidence lifecycle writes in that suite likewise do not establish a production evidence write service. This distinction is essential: retaining those capabilities would violate the confirmed runtime-only boundary.

### 2.2 Migration and privilege semantics reviewed

- V001: roles/schema ownership at lines 1-26; platform grants at lines 375-388; audit grants at lines 527-534. It gives customer role four executable append/idempotency capabilities across platform/audit in addition to its later IAM grants.
- V002: IAM table definitions at lines 8-80, local SECURITY INVOKER guards and triggers at lines 153-232, IAM grants at lines 241-256, organization collaboration grants at lines 303-312. Its guards inspect OLD/NEW, not an independently privileged owner session.
- V003-V007: subject, catalog/payment, IVR, entitlement and activation/consent structural waves, cross-module grants, local invoker guards, late FKs and the entitlement SECURITY DEFINER issuance function. None grants another current capability to either target role. Their declared lifecycle capability matrices are not implemented customer/evidence features.
- V008: five evidence tables/56 columns, ordinary and deferred FKs at lines 228-285; five SECURITY INVOKER guards at lines 289-438; seven triggers at lines 440-467; own/cross-service grants at lines 484-544. In particular, `guard_evidence_record_creation_cycle` reads evidence_event, while `guard_evidence_event_creation_cycle` SELECTs the entire evidence_record row and evidence_event. These are local invoker reads, not implicit owner-powered runtime access.
- V009: sharing FK references at lines 137-166 and invoker guards at lines 170-288; later V010 supersedes its sharing-role capabilities. The guard reads its own sharing data and NEW/OLD; it does not require evidence_role INSERT/UPDATE or full evidence reads.
- V010: owner-context revoke/regrant and exact sharing query/command split; no reason was found to alter it. V011/V012: public reader/admission direct-login boundaries. V013: direct media reader, history guards and exact no-membership/ACL checks. Preserve their bytes and effective grants.

Invoker functions execute with the effective caller's privileges, not the function owner's, even when owner-local. Trigger installation checks TRIGGER on the table and EXECUTE on its function; these are not a reason to retain explicit guard EXECUTE for SELECT-only application roles. No implemented target-role SELECT fires a write trigger. Owner fixture writes must stay under their owning role through any deferred trigger execution, or force deferred checks before leaving that role. Never convert an invoker function to SECURITY DEFINER to make tests pass.

Foreign-key enforcement is PostgreSQL RI machinery, not a Java SELECT on the referenced table. Existing sharing INSERTs retain their original FK/backstop behavior without giving either target role foreign writes or REFERENCES. Current target-role projections have no FOR UPDATE/FOR SHARE. Those row-lock forms need UPDATE privilege in addition to SELECT; they must now be denied even on otherwise readable projection columns. Built-in pg_catalog expressions, clock functions, UUID values and application advisory locks do not require user-schema EXECUTE grants to these readers. Test application-schema routines separately from PostgreSQL built-ins; do not revoke pg_catalog privileges.

### 2.3 Alignment with the design documents

- `docs/12-technology-stack-and-system-architecture.md:222` and `docs/12-technology-stack-and-system-architecture.md:246`: keep module ownership and prohibit shared foreign writes. No module dependency or deployment unit changes.
- `docs/14-production-data-model-and-flyway-schema-spec.md:266`: retain the structural owner map and historical V001-V009 grants as history; append a clearly current post-V014 overlay, not a rewrite of the business write-path model. Supersede only current runtime assertions for these two roles.
- `docs/17-production-security-design-spec.md:232` and `docs/17-production-security-design-spec.md:236`: Q17/Q23 still belong to Platform/Database/Security. Least privilege here is executable local acceptance, not selection of production principals or credential authority.
- `docs/operations/pa25-production-database-identity-provisioning-runbook.md:28` distinguishes PA25 identity readiness from object ACL changes. PA26 supplies that previously deferred object narrowing while preserving PA25's external custody and provenance contracts.
- Add prose runtime overlays outside machine-frozen model/fixture markers. Do not rewrite historical counts, Q registries, architecture fixtures or security/deployment models to mask existing baseline validation drift.
- Explicitly supersede current-tense broad-capability claims, not merely append an unconnected overlay: `backend/README.md:9` (customer/evidence write/routine capability), `docs/operations/pa25-production-database-identity-provisioning-runbook.md:28` (current risk/ACK statement and LW's broader evidence capability), and the affected target-role grant/capability rows and sections of `docs/14-production-data-model-and-flyway-schema-spec.md:329`, `docs/14-production-data-model-and-flyway-schema-spec.md:626`, `docs/14-production-data-model-and-flyway-schema-spec.md:664`, `docs/14-production-data-model-and-flyway-schema-spec.md:700`, `docs/14-production-data-model-and-flyway-schema-spec.md:2087`, `docs/14-production-data-model-and-flyway-schema-spec.md:2225`, `docs/14-production-data-model-and-flyway-schema-spec.md:2262`, `docs/14-production-data-model-and-flyway-schema-spec.md:3538`, `docs/14-production-data-model-and-flyway-schema-spec.md:3564` and `docs/14-production-data-model-and-flyway-schema-spec.md:4037` (input anchors; inspect neighboring rows and all equivalent current claims). Cover INSERT/UPDATE/EXECUTE and cross-service schema USAGE/read claims for the two target roles. Preserve V001-V013 model text as explicitly historical, without deleting or rewriting frozen text; place a precise adjacent or section-linked supersession outside frozen markers where needed. Leave unrelated-role capability claims untouched. Section 11 requires a complete before/after or historical-to-current supersession ledger.

## 3. Exact before/after ACL contract

Scope of “none”: all non-system/application schemas, relations, sequences, columns and routines, including `public`; no new database ACL. All retained/new explicit ACLs are granted by the named object owner, with no grant option. No ownership or role attribute changes. Owners retain their existing implicit owner rights. Unlisted privileges are forbidden, not deferred optional grants.

### 3.1 customer_iam_role

| Kind/object | Before V014 | After V014 / explicit disposition |
| --- | --- | --- |
| Schema customer_iam | USAGE | Retain USAGE only |
| Schema organization_access | USAGE | Retain USAGE only |
| Schemas platform, audit | USAGE on each | Remove both; no USAGE or CREATE |
| All other application schemas | None | None; no CREATE anywhere |
| customer_iam.customer_account | Table SELECT, INSERT; UPDATE(display_name,status,revision,updated_at) | Remove both table grants and every listed UPDATE. Add only column SELECT(id,display_name,status). Deny revision,created_at,updated_at reads. |
| customer_iam.auth_identity | Table SELECT, INSERT; UPDATE(status,verified_at,key_version,revision) | Remove both table grants and all four UPDATE grants. No column grants, including no provider_subject_ciphertext/provider_subject_hmac read. |
| customer_iam.identity_profile | Table SELECT, INSERT; UPDATE(real_name_status,legal_name_ciphertext,id_number_ciphertext,id_number_hmac,verification_ref_ciphertext,verified_at,revision) | Remove both table grants and all seven UPDATE grants. No column grants at all. |
| organization_access.organization | SELECT(id,display_name,status) | Retain exactly these three column SELECTs |
| organization_access.organization_membership | SELECT(organization_id,account_id,role,status) | Retain exactly these four column SELECTs; no id,department_label,revision read |
| customer_iam.guard_customer_account(), guard_auth_identity(), guard_identity_profile() | EXECUTE on each | Remove all three; no other local guard privilege |
| platform.idempotency_begin(text,text,text) | EXECUTE | Remove |
| platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text) | EXECUTE | Remove |
| platform.outbox_append(text,text,text,uuid,text,jsonb,text,text) | EXECUTE | Remove |
| audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text) | EXECUTE | Remove |
| All remaining objects/privilege kinds | No authorized target-role privileges | None; in particular no table-level SELECT, INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER, column INSERT/UPDATE/REFERENCES, sequence privilege, application routine EXECUTE, schema CREATE or grant option |

After: two schema-USAGE tuples and ten column-SELECT tuples = **12** explicit ACL tuples. Before: four schema, six table, twenty-two column and seven routine tuples = **39**. Retain nine old tuples, remove thirty, add three column tuples.

### 3.2 evidence_role

| Kind/object | Before V014 | After V014 / explicit disposition |
| --- | --- | --- |
| Schema evidence | USAGE | Retain USAGE only |
| Schemas subject_number, ivr, entitlement_metering, platform, audit | USAGE on each | Remove all five; no USAGE or CREATE |
| All other application schemas | None | None; no CREATE anywhere |
| evidence.evidence_record | Table SELECT, INSERT; UPDATE(status,solidified_at,revision) | Remove both table grants and all three UPDATE grants. Add SELECT(id,subject_id,status,source_type,source_ref_id,identity_at_capture) only. |
| evidence.evidence_artifact | Table SELECT, INSERT | Remove both. Add SELECT(evidence_id,media_object_id,artifact_role) only. |
| evidence.media_object | Table SELECT, INSERT | Remove both. Add SELECT(id,sha256,size_bytes,mime_type) only. In particular no storage_provider,bucket,object_key,object_version_id,crypto_key_ref,worm_status,created_at read. |
| evidence.recording | Table SELECT, INSERT; UPDATE(media_object_id,validated_duration,validated_sha256,status,validated_at,revision) | Remove both table grants and all six UPDATE grants. Add SELECT(media_object_id,validated_duration) only. |
| evidence.evidence_event | Table SELECT, INSERT | Remove both. No remaining column grant. |
| subject_number.business_subject | SELECT(id) | Remove |
| ivr.call_record | SELECT(call_id,session_id,subject_id) | Remove all three |
| ivr.inbound_session | SELECT(id,call_id,subject_id) | Remove all three |
| entitlement_metering.usage_reservation | SELECT(id,call_id,subject_id) | Remove all three |
| evidence.reject_row_mutation(), guard_recording(), guard_evidence_record(), guard_evidence_record_creation_cycle(), guard_evidence_event_creation_cycle() | EXECUTE on each | Remove all five |
| platform.outbox_append(text,text,text,uuid,text,jsonb,text,text) | EXECUTE | Remove |
| audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text) | EXECUTE | Remove |
| All remaining objects/privilege kinds | No authorized target-role privileges | None; same blanket exclusions as customer role, including no application routine EXECUTE and no table-level SELECT |

After: one schema-USAGE tuple and fifteen column-SELECT tuples = **16**. Before: six schema, ten table, nineteen column and seven routine tuples = **42**. Retain one old tuple, remove forty-one, add fifteen column tuples. `identity_at_capture` stays readable because the implemented command compares and freezes that projection; removing it would change current semantics. This plan does not claim a row-level tenant boundary or redact currently returned projection content.

### 3.3 Role graph: no delta

Use PA25 slot labels, not invented production login names:

| Login / effective role | Required relationship |
| --- | --- |
| LQ: sharing query external LOGIN NOINHERIT | sharing_role -> LQ |
| LC: customer context external LOGIN NOINHERIT | customer_iam_role -> LC |
| LW: sharing command external LOGIN NOINHERIT | sharing_command_role -> LW AND evidence_role -> LW |
| sharing_public_read_role | Direct LOGIN NOINHERIT, no memberships |
| sharing_admission_role | Direct LOGIN NOINHERIT, no memberships |
| evidence_media_reader_role | Direct LOGIN NOINHERIT, no memberships |

Each edge has `admin_option=false`, `inherit_option=false`, `set_option=true` and the existing compatible superuser grantor requirement. Four effective NOLOGIN roles plus three direct-login effective roles = seven effective roles; three external plus three direct = six logins; four edges. No runtime role is a member of another role. `customer_iam_role` and `evidence_role` retain their historical INHERIT attribute: with zero outgoing memberships it confers no extra capability; changing it is unnecessary hardening outside the behavioral need. The external NOINHERIT flag and per-edge INHERIT FALSE remain essential.

V014 does not GRANT/REVOKE memberships, create logins, adopt names, change passwords/comments or change attributes. In particular, **do not remove LW -> evidence_role** and do not remove/replace the Java role switch. The source proves the edge is needed, and narrowing SQL alone preserves the implemented behavior. No production Java file belongs in the implementation allowlist.

## 4. V014 design: one transactional privilege reconciliation

New path: `backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql`; Flyway version `014`, description `database runtime acl rbac hardening`. No companion migration. Do not modify V001-V013 or stamp history manually.

### 4.1 Preconditions before any persistent ACL mutation

1. Require PostgreSQL `server_version_num` in `[160000,169999]`, `current_user=session_user` and actual superuser migration authority, as PA25 requires. Use transaction-local `search_path=pg_catalog` and fully qualified catalog/object names. Acquire the existing PA25 transaction advisory lock `(725025,1)` to serialize local reconciliation with provisioning; exclusive deployment administration remains an external prerequisite, not solved by this lock.
2. Require the existing owner roles and expected relation/routine identities from sections 2-3; owners must still be the named owners, not either runtime role. Validate safe target-role flags, NOLOGIN, password NULL, no role GUCs, expected historical INHERIT, connection limit/validity, no database CREATE/TEMP or direct database grants, no ownership/extension dependency. Reject missing objects, wrong ownership, role aliases, unsafe role state and grantor/grant-option drift; do not repair them.
3. Validate exact successful ordered V001-V013 predecessor history (including version spelling, scripts, types, descriptions and existing Flyway checksums). Permit either no 014 row during initial application or exactly one successful correctly named rank-14 row for direct SQL replay; forbid other/duplicate/failed/extra history. Do not embed V014's own checksum in its own SQL: Flyway validates that checksum externally, and PA25 freezes it once V014 bytes are finalized.
4. Before any mutation, distinguish exactly two supported topologies: (a) clean/no external provisioning, with no PA25 external-login markers or managed-boundary edges, the four existing effective NOLOGIN roles and the three migration-created direct logins intact; or (b) the complete PA25 six-login/seven-effective-role/four-edge graph in section 3.3. In (b), require all three external logins and all four edges, including LQ -> sharing_role, LC -> customer_iam_role and both LW -> sharing_command_role/evidence_role. Reject a half-provisioned graph, any subset of marked external logins/edges, outgoing runtime-role memberships, managed-role grantors, extra/transitive/owner/predefined-role edges or unsafe edge options in V014 itself, not only at later PA25 check. Resolve the three external identities from exact existing provenance, without adding a name registry or adopting arbitrary names: byte-exact UTF-8 marker `onecall:pa25:v1:db=<current database OID>:role=<actual role OID>:slot=<slot>:name=<actual role name>`, slots `sharing_query`, `customer_context`, `sharing_command` bound respectively to LQ/LC/LW. Require unique slot/OID/name bindings, no copied/renamed/stale/wrong-slot markers, compatible superuser edge grantors and exact section 3.3 options. Validate the full graph without changing it; reject ambiguity rather than infer intent from compatible attributes.
   External LQ/LC/LW safe attributes are exactly LOGIN, NOINHERIT, NOSUPERUSER, NOCREATEDB, NOCREATEROLE, NOREPLICATION, NOBYPASSRLS, connection limit -1, `rolvaliduntil IS NULL`, `rolconfig IS NULL`, no role/database GUC overrides, no direct ACL/ownership or unexpected dependencies. Their password/verifier may be NULL or non-NULL after credential assignment: preserve its bytes exactly, never clear/rewrite it or print it. This allowance does not apply to the two target NOLOGIN roles, whose passwords remain NULL under precondition 2. Preserve direct-login attributes/credentials and the existing media-reader infinity-validity exception; do not impose external-login validity rules on direct logins.
5. Catalog-scan both roles across all non-system schemas, relations, columns, routines, sequences and shared ACL/dependency surfaces. Input explicit ACLs must equal either the complete old 81-tuple pair from section 3 or the complete new 28-tuple pair. Missing/extra grants, mixed/partial states, wrong grantors, grant options or grantor dependencies reject atomically rather than being silently adopted. This is a bounded migration guard, not a second runtime authorization registry.
6. PUBLIC must have no application schema/table/column/sequence/routine privileges (normalize NULL routine ACLs with `acldefault`, rather than treating NULL as empty). Target roles must not own default ACL rows; reject defaults granting PUBLIC or either target role, including global and owner/schema-local defaults and target-role grantor entries. Preserve current database PUBLIC CONNECT and the existing denial of PUBLIC CREATE/TEMP. Do not repair PUBLIC/default ACL drift or change defaults for unrelated future services.

An incompatible database stops with a fixed sanitized `P0001` migration diagnostic. Unauthorized runtime operations use `42501`; do not require migration-precondition errors to masquerade as runtime privilege errors.

### 4.2 Reset then owner-grant exact projection

In one Flyway transaction, after preflight succeeds:

1. Revoke the target roles' schema privileges across their known old/new schemas.
2. Revoke all target-role table privileges on their known old/new relations. Separately revoke SELECT/INSERT/UPDATE/REFERENCES on every user column of those relations; table REVOKE alone does not erase column ACLs. This clears historical cross-service SELECTs, old UPDATEs and already-current SELECTs on replay.
3. Revoke the fourteen old role/routine EXECUTE tuples using exact signatures, not ambiguous overloaded names. No CASCADE, object drop, role drop, DML, owner transfer, function/trigger definition, default-ACL rewrite or unrelated-grantee revoke. Unexpected ACL surfaces already failed preflight; do not indiscriminately repair arbitrary catalog contamination.
4. `SET LOCAL ROLE` to each owning role and grant section 3's exact final schema/column privileges. customer_iam_owner grants account projection; organization_access_owner grants organization/membership projection; evidence_owner grants evidence projection. Reset to the migration identity between owners and before postflight. No WITH GRANT OPTION and no effective membership inheritance shortcut.
5. Assert exact final raw ACL and effective privilege sets over all non-system objects, no routine/table/sequence/write/grant-option leakage, and unchanged role/membership/owner/PUBLIC/default state. Assert raw ACL grantor ownership, not just `has_*_privilege`. Reject any discrepancy to roll back the entire transaction.

### 4.3 Replay and ownership boundaries

- Re-executing V014 SQL against its own exact final ACL state repeats revoke/regrant to the same owner-granted 28 tuples. Two direct SQL replays must leave catalog ACLs, object identities, role OIDs/attributes/comments, graph, history and business facts equivalent. Normal Flyway rerun must execute zero migrations and validate fourteen applied rows.
- Test upgrade from V013 both before provisioning and with a synthetic PA25-compatible already-provisioned graph. Then apply/check PA25 at V014 and replay V014 with that graph intact. Credentials and provenance markers must survive byte-identically.
- Named responsibility: `v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle` covers both clean V013 upgrade and complete, credential-assigned PA25 V013 graph upgrade (fixture setup reproduces the frozen PA25 contract, not the updated script on stale V013). Assign synthetic non-NULL LC/LW verifiers before V014; capture verifier/marker bytes, OIDs, names, slots, validity and edge options/grantors privately, then prove exact preservation and updated PA25 apply/check success. `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects` covers SQL replay and zero-execution Flyway replay for both resulting clean and credential-assigned graphs, including byte-identical verifier/provenance preservation. `v014RejectsUnsafeRolesMembershipsOwnershipAndSettingsAtomically` covers partial external-login/edge graphs and wrong marker/OID/slot/name/safe-attribute cases, proving failure before mutation. `v014FreshChainHasExactHistoryAndImmutablePredecessors` remains the separate fresh-chain responsibility; no extra method identities are added.
- A partial or contaminated role state is not a supported reconciliation input. Fail closed and route remediation externally; never remove an unexpected membership to make the migration pass.
- Preserve relation/column definitions, defaults, constraints, indexes, FK behavior, trigger definitions, function definitions/search paths/security modes and all ACLs for other principals. Compare before/after structural catalog snapshots excluding only the authorized ACL fields and the new Flyway history row.
- Rollback is transaction rollback before commit. After committed V014, do not instruct replay of V001-V013 or automatic broad regrant; an external reviewed recovery decision is required.

## 5. PA25 provisioning/check compatibility

### 5.1 Exact staleness and required delta

`backend/ops/postgresql/pa25-runtime-identities.sql:41` embeds a literal JSON ACL oracle. It does not derive the expected ACLs from the live database. At line 2457 it embeds exact V001-V013 history; lines 2864-2878 query only `public.flyway_schema_history`, compare symmetric set differences, and require thirteen total rows. At lines 2962-2971 it resolves the literal object identities and requires exactly 300 ACL tuples. The function-owner/security-mode oracle and role/dependency/setting checks are separate and must remain intact.

Required changes in that existing script:

- Freeze one exact current fourteen-entry history: retain the thirteen prior entries byte-for-byte as data; append rank 14 / version `014` / chosen filename / description / actual finalized Flyway checksum. Change total rows 13 -> 14. No max(version), dynamic trust, wildcard checksum, dual 013/014 acceptance or fallback legacy oracle. PA25 apply/check on a stale V013 database must now reject with `PA25_HISTORY_REJECTED` before any login creation.
- Replace only target-role entries in `expected_acls` using section 3: 39 -> 12 customer tuples and 42 -> 16 evidence tuples. Keep the other **219** tuples unchanged, including their existing routine-signature spelling/resolution. New total: `300 - 39 - 42 + 12 + 16 = 247`. Do not globally recanonicalize other entries as an unrelated cleanup.
- Change the 300 cardinality assertion to 247. Maintain raw ACL comparison (owner/grantor/no grant option), effective schema/table/column/routine/sequence checks, shared-dependency closure, PUBLIC/default/database checks and missing-grant rejection. The script remains an identity provisioner/checker, not an ACL repairer.
- Preserve six-login/seven-effective-role/four-edge markers exactly: `PA25_APPLY_OK logins=6 effective_roles=7 memberships=4` and corresponding CHECK marker. Role attributes, edge options, fixed nonsecret inputs and the four-edge grant loop do not change.
- Preserve explicit transaction, ON_ERROR_STOP, advisory serialization, two-pass pre/post verification, sanitized category-only errors, output suppression before COMMIT, creation-only provenance, bytewise comment checking, no adoption/override/force mode, and no printing credentials/endpoints/catalog contents. Apply may create absent marked external logins and repair only absent approved edges; check performs no persistent changes.
- Preserve external credentials/password verifiers and validity, existing role OIDs/comments, compatible grantors, direct-login credentials and ownership. Preserve Q17/Q23, exclusive-admin assumption, external launcher redaction, TLS/pg_hba/rotation/pool-drain handoff and explicit non-production boundary.

### 5.2 PA25 acceptance updates within its existing twenty methods

- `ProductionDatabaseIdentityProvisioningIT.java:119` and line 660: latest history becomes fourteen; its `migrationHashes()` at line 990 intentionally continues freezing exactly V001-V013, not an accidentally empty glob and not replacing the immutable predecessor set with V014. Separately validate V014 checksum/history.
- Replace the independent literal Java 300-tuple oracle at line 1005 and its two cardinality assertions at lines 1334-1335 with 247. Build it from the source-derived list in this plan, never by reading the script's JSON or the migration's expected relation.
- At line 363, change the formerly successful no-op evidence UPDATE into `42501` on a writable transaction. At lines 317-320, keep the independent read-only `25006` demonstration, but use an ACL-authorized operation under an appropriate unchanged role (for example command role updating a permitted grant column). Add customer UPDATE denial in a writable transaction. Do not assume PostgreSQL error ordering for a doubly forbidden operation.
- Strengthen existing command-switch method (line 209): nonempty seeded evidence exercises both reader statements, same backend PID/session_user, expected role switch and restoration, then missing-edge `42501` and pool restoration on rollback. Retain actual customer repository and sharing/admission writes, cross-role denials and direct-login positives.
- Extend existing history/input method (line 580) with missing/bad/duplicate V014, wrong checksum/description/script/rank/type/success, extra future row and stale V013 history. Prove stale history rejects both modes without creating logins. Extend existing ACL contamination method (line 495) to restore an old INSERT/UPDATE/routine grant, remove a new projection column, add a forbidden sensitive SELECT, and add a wrong-grantor/grant-option grant: apply/check reject, not repair.
- Preserve all twenty existing method identities and their provenance, password, redaction, role/topology/default-ACL and atomicity tests. Tests remain synthetic nonproduction credentials over disposable PostgreSQL; no new runtime registry or provisioning mode.

## 6. Executable proof and test oracle

### 6.1 Independent final-state oracle

Add `DatabaseRuntimeAclRbacHardeningIT` with literal final projection maps and expected owner/schema tuples authored from sections 2-3. Do not parse expected grants out of V014 or the PA25 JSON, and do not generate expected ACLs from the actual catalog being checked. The separately maintained PA25 Java 247-tuple oracle provides full seven-role coverage.

Enumerate every live non-system schema/relation/column/routine/sequence, comparing both physical ACL tuples (`aclexplode`, owners, grantors, grantability, column attacl) and effective `has_*_privilege`. Resolve SQL type aliases to OIDs/signatures before comparison; `timestamptz` and `timestamp with time zone` are the same type, not a new ACL delta. Include PUBLIC expansion/default ACLs, database privileges, role attributes, exact touching membership edges/options/grantors, predefined-role reachability, ownership and shared dependencies. Assert absence as well as presence. The oracle must fail if any one removed grant is put back or any one required column is removed.

### 6.2 Positive runtime-path proofs on the final chain

| Proof | Execution and expected behavior |
| --- | --- |
| Customer context | Real service/repository and existing CustomerMeIT/CustomerSharesIT: active account with personal workspace, active organization memberships/roles, ordered multiple workspaces, disabled/missing account, inactive organization/membership filtering. Same HTTP/result/error semantics, same read-only and pool cleanup behavior. |
| Evidence projection | Invoke the actual JooqShareableEvidenceProjectionReader under assumed evidence_role with nonempty original/derived artifacts, matching media, recording duration and nullable LEFT JOIN recording; missing evidence and malformed/duplicate projection outcomes remain unchanged. The join and filter columns, not a hand-written smaller substitute query, must execute. |
| Share creation | Existing CustomerShareCommandsIT create, replay/conflict, organization/designated/extraction modes, role-switch SQL trace and failure restoration continue on latest V014. Frozen projection values/digests and responses remain identical. Revocation/rotation/replay concurrency, locks, events/outbox/audit and rollback behavior retain their existing proofs. |
| Other sharing/evidence readers and writers | Existing sharing query, customer shares, public shares, admission/media authorization, direct media read, playback and transport suites all run latest V014. Sharing/admission writes and FK checks succeed with unchanged roles; no target-role write is invented as a positive. |
| Owner versus runtime | On final V014, owner-seed valid evidence/record/event cycle and force deferred checks while still evidence_owner; prove valid owner writes and selected immutable/terminal/cycle guard failures remain their original errors. Runtime writes deny with 42501 before those business guards. Compare invoker/security-mode/function/trigger definitions unchanged. |
| Customer IAM guards on current final state | `ownerInvokerGuardsAndSharingWritesRemainOperational` must execute valid customer-account/auth-identity/identity-profile owner writes under customer_iam_owner on V014 and representative invalid transitions with their original guard errors, not runtime 42501. `v014IsPrivilegeOnlyAndLeavesUnrelatedBoundariesEquivalent` must compare the complete definitions of customer_iam.guard_customer_account(), guard_auth_identity() and guard_identity_profile(), owner identities, invoker/security modes, search paths and trigger bindings against the V013 snapshot. Historical-prefix behavior alone is insufficient; no new business capability or function edit is authorized. |
| Identity transitions | Synthetic PA25 provisioned logins connect over test TCP; LC assumes only customer role; LW assumes command then evidence and back on same PID. Missing evidence edge fails 42501; normal/exception/rollback/commit restores session login and transaction state. Three direct logins stay direct and cannot assume target/owner/predefined roles. |

### 6.3 Negative runtime SQLSTATE 42501 matrix

Use genuine NOSUPERUSER NOINHERIT external logins, not the migration connection merely SET ROLE without auditing session authority. Run ACL denials in explicit writable transactions or autocommit with fresh connections/savepoint recovery after each failure. Do not substitute read-only transaction failures for ACL proof.

1. Customer: attempt every nongranted column in each of its five formerly accessible relations, SELECT * on each, all auth_identity and identity_profile reads, including every ciphertext/HMAC/verification column; attempt every old UPDATE column, every formerly allowed table INSERT, DELETE/TRUNCATE and column INSERT/REFERENCES/TRIGGER/DDL/GRANT attempts. Allowed account/organization/membership projection SELECTs must still work.
2. Evidence: per-column SELECT denial for the complement of the exact fifteen-column set across all five evidence tables, SELECT * on each, all evidence_event reads; explicitly probe media bucket/object_key/object_version_id/crypto_key_ref and record snapshot/snapshot_sha256/consent_version/event linkage and recording identifiers/declarations/status/validated hashes/time/revision. Attempt all five old INSERTs and every old nine UPDATE columns, plus DELETE/TRUNCATE/REFERENCES/TRIGGER/DDL/GRANT.
3. Cross-service: evidence role cannot read former subject/IVR/entitlement columns; neither target role can read/write platform/audit tables or sharing lifecycle data; customer cannot read evidence and evidence cannot read IAM/organization tables. Test containing-schema denial as well as effective catalog absence so hidden column grants cannot pass unnoticed.
4. Routines: directly invoke each removed executable routine where its call shape permits, with safe synthetic arguments, and assert 42501/no side effect; check every application routine's effective EXECUTE is false. Explicitly assert no outbox/audit/idempotency facts appear. Do not mistake a trigger-function call's unsupported-return-type error for a permission denial.
5. Locks: FOR UPDATE and FOR SHARE on allowed account, organization/membership and evidence projection columns fail 42501; ordinary SELECT still succeeds. Sharing command/admission lock positives remain untouched.
6. Without assumption: LC and LW cannot read any target-role application projection or write any application table until the intended SET LOCAL ROLE. SET ROLE authorization derives from session_user membership, not the current effective role. With session_user=LC, entry to customer_iam_role is positive; entry to evidence/command/query/owner/direct/predefined roles is denied with 42501. With session_user=LW, both sharing_command_role -> evidence_role and evidence_role -> sharing_command_role are approved positive transitions on the same connection; restoration is not a negative. From either LW effective role, customer/query/owner/direct/predefined roles remain 42501 negatives. Own-login/RESET/NONE restoration is positive. Exercise these genuine unapproved-role attempts in `assumedRolesAndPoolRestorationPreserveRuntimeBoundaries`; do not change the role graph or Java to force a false effective-role-origin rule. Graph contamination is separately rejected by PA25/V014 catalog guards.
7. Sensitive/write privilege restoration, unexpected members, INHERIT/SET/ADMIN option drift, membership grantor changes, PUBLIC leakage, defaults, ownership, unsafe role flags, GUCs and wrong grants must make migration/provisioning fail atomically with their expected sanitized categories. These catalog-contamination failures are not themselves required to be 42501.

For every negative, capture SQLSTATE and operation identity without printing secrets or raw connection diagnostics; compare business-row and ACL/graph snapshots after rollback. A transaction already aborted by the preceding test is not evidence of denial.

### 6.4 Historical tests must not dictate current privileges

`PostgresContainerIT.java` contains 64 foundation methods and extensive old role contracts: customer role fixtures/functions near lines 1754, 5199, 6638, 7861, 20926 and 23430; evidence role ACL/lifecycle assertions near lines 2783 and 26920-27110; final migration/history/resource assertions near lines 1236, 1387 and 10769. Regranting old rights to keep these passing is forbidden.

Keep that suite explicitly a **frozen V001-V013 historical-prefix database contract**: cap only its final Flyway configuration at line 1093 with target 013, retaining its earlier 004/005/006/007/008 staging and old behavior assertions. Preserve `initialMigration == 5` at line 1236, thirteen applied rows with history tail 013 at lines 1385-1390, and `repeated.migrationsExecuted == 0`. Resolve all fourteen resources, including exactly one V014, without treating it as applied. Before finalizing the assertion changes, Backend/Database must observe the locked Flyway version on disposable PostgreSQL 16 with V014 present and target=013: record `info().all()` entries (version/script/state/applied status), `pending()` entries, V014's exact MigrationInfo state and target-capped `validate()` result in S1 evidence. Do not guess ABOVE_TARGET versus ignored/pending semantics from precedent. Assert the actually observed exact state and membership in both API result sets, preserving the line-1394 pending assertion as empty if observed, or replacing it with an equally strict exact-set/state assertion if that is what the observation proves. Update resolved-resource assertions at lines 10769/10782 to require V014 and thirteen applied resources. Deleting or weakening pending/resolved assertions, removing the target, marking V014 applied, removing old ACL tests or renaming methods to make the test green is prohibited. An unexpected result requires owner disposition, not an implicit weakening. This observation is required later implementation evidence, not a result claimed here.

This small test-only isolation avoids rewriting dozens of historical fixture/guard/concurrency protocols and avoids retaining unimplemented capabilities. It is acceptable only together with the new uncapped V014 suite, explicit fresh/upgrade final history proof, and every other application/provisioning suite migrated to latest. Historical-prefix success alone cannot satisfy PA26 acceptance. Verify all earlier staged/absence fences remain intentional, not accidentally applied to the latest database.

## 7. One implementation slice and exact file allowlist

Prefer **one behaviorally composed S1**: V014, final-state acceptance, PA25 compatibility, inventory and documentation must agree before the slice is complete. No independent intermediate deployable state is claimed. No extra slice or production Java edit is authorized; a newly discovered need requires plan amendment before implementation.

The following is the complete implementation write allowlist (repository-root relative). Existing plan/review/control files are not implementer scratch space.

| # | Allowed path | Exact permitted responsibility |
| --- | --- | --- |
| 1 | backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql | New sole privilege-only reconciliation |
| 2 | backend/src/test/java/com/onecall/archtests/container/DatabaseRuntimeAclRbacHardeningIT.java | New twelve-method PG16 acceptance class; self-contained test helpers |
| 3 | backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java | Section 6.4's explicit 013 cap and observed exact pending/resolved/state assertions; preserve initialMigration==5, 013 history tail, zero replay, 64 methods and old structural/behavior oracles |
| 4 | backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java | Latest migration count/applied count 13 -> 14 at lines 87/89 |
| 5 | backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java | Latest migration count/applied count 13 -> 14 at lines 70/72 |
| 6 | backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java | Latest applied count at line 36 |
| 7 | backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java | Latest counts/history at lines 62,569-574; preserve role ACL boundary |
| 8 | backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java | Latest fixture migrate count at line 245 only; preserve direct reader |
| 9 | backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java | Latest applied count at line 49 only; retain V013-specific history/replay and V001-V012 hash test as historical assertions |
| 10 | backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java | Latest applied count at line 285 only |
| 11 | backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java | Latest applied count at line 51 only |
| 12 | backend/src/test/java/com/onecall/archtests/container/ProductionDatabaseIdentityProvisioningIT.java | Section 5's oracle/history/denial/positive strengthening inside existing twenty methods |
| 13 | backend/ops/postgresql/pa25-runtime-identities.sql | Exact V014 history and 247-tuple ACL oracle; preserve identity/security contract |
| 14 | backend/pom.xml | One Failsafe include; no dependency/plugin/runtime change |
| 15 | .github/workflows/ci.yml | New named source/CI/XML inventory and cumulative counts only; no weakened filters/skip rules |
| 16 | backend/README.md | Current privileges, PA25 history/oracle, historical-prefix distinction, validation and exact current inventory; explicitly supersede current write/routine claim at input line 9 under section 2.3 |
| 17 | docs/14-production-data-model-and-flyway-schema-spec.md | Current V014 runtime overlay with explicit target-role current grant/capability supersessions at section 2.3's anchors; preserve V001-V013 text as historical and frozen structural/business model; unrelated roles untouched |
| 18 | docs/17-production-security-design-spec.md | Runtime least-privilege overlay, retained projection sensitivity and unchanged Q17/Q23 boundary |
| 19 | docs/operations/pa25-production-database-identity-provisioning-runbook.md | V014-required order, 247 oracle, unchanged graph/provenance/credentials, current inventory and live NOT EXECUTED boundary; explicitly supersede input line 28's current broad customer/evidence capability/ACK statement under section 2.3 |
| 20 | docs/gateflow/pa26-database-runtime-acl-rbac-hardening-s1-implementation.md | Later implementation completion/evidence report only |

No other path is allowed by this plan. In particular V001-V013, application Java/configuration, OpenAPI, module boundaries, frontend, fixture/model registries, Maven wrapper and packaged-JAR firewall are read-only. Controller-owned gate records/reviews may be produced only by separately authorized later gates, not by S1. A necessary additional file or test inventory delta is a reviewable plan deviation, not implicit permission.

### 7.1 Planned new method identities and exact inventory delta

The new class has exactly these twelve ordinary `@Test void` methods (no parameterized/dynamic tests):

1. `v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle`
2. `v014FreshChainHasExactHistoryAndImmutablePredecessors`
3. `customerContextUsesOnlyApprovedColumnsAcrossAllCurrentStates`
4. `sharingProjectionUsesOnlyApprovedEvidenceColumns`
5. `customerRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject`
6. `evidenceRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject`
7. `assumedRolesAndPoolRestorationPreserveRuntimeBoundaries`
8. `ownerInvokerGuardsAndSharingWritesRemainOperational`
9. `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects`
10. `v014RejectsUnsafeRolesMembershipsOwnershipAndSettingsAtomically`
11. `v014RejectsPublicDefaultAndUnexpectedAclDriftAtomically`
12. `v014IsPrivilegeOnlyAndLeavesUnrelatedBoundariesEquivalent`

Group matrix cases within these methods using independently recoverable transactions; do not add method identities merely for loop cases. Reuse existing fixture/application suites rather than introduce a second application simulator.

Baseline derives from `.github/workflows/ci.yml:939-961`, the source sets there and `backend/pom.xml:182-196`, not from stale historical README paragraphs: **36 classes / 309 Surefire methods + 14 classes / 234 Failsafe methods = 50 classes / 543 methods**, annotation line scan 544 including exactly one known comment hit in PostgresContainerIT. PA25 contributes twenty methods. Existing IT methods remain unchanged in identity/count.

Expected S1: **36/309 Surefire + 15/246 Failsafe = 51/555 total**, annotation line scan **556**, **15** exact POM includes. Add one PA26 additions set with twelve names to CI; preserve all earlier named baseline/additions assertions and the single comment-hit assertion. Ensure source names = exact CI names = unique XML testcase names, zero failures/errors/skips, no duplicate classes/cases and no unlisted test class. Update current inventory assertions/output and current README/runbook inventory; do not rewrite historical checkpoint counts. If implementation legitimately needs a different method set, reconcile the exact delta before acceptance and document the deviation instead of inventing a passing count.

## 8. Validation commands and evidence requirements

These are **later implementation commands**, not commands executed or authorized against Docker/credentials at the planning gate. Run in an approved disposable environment with the repository's Temurin 21.0.13+9, Maven 3.9.9 wrapper and functioning PostgreSQL-16 Testcontainers prerequisite. Record exact image digest/server patch from that run; `postgres:16` is currently an unpinned test fixture, not a production version guarantee. Do not claim a skipped container suite passed.

### 8.1 Before editing and focused acceptance

From repository root, preserve immutable predecessor bytes in an external evidence directory:

```sh
sha256sum backend/src/main/resources/db/migration/V0{01,02,03,04,05,06,07,08,09,10,11,12,13}__*.sql > /tmp/pa26-v001-v013.before.sha256
cd backend
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=DatabaseRuntimeAclRbacHardeningIT,ProductionDatabaseIdentityProvisioningIT verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerMeIT,CustomerSharesIT,CustomerShareCommandsIT,SharingQueryServiceIT verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=SharingRoleLeastPrivilegeIT,PublicSharesIT,PublicShareAdmissionIT,SharingAdmissionRoleLeastPrivilegeIT,EvidenceMediaReadRoleLeastPrivilegeIT,EvidenceMediaReadIT,MediaPlaybackIT,MediaGatewayTransportIT verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT verify
```

Expected: new class twelve and PA25 twenty focused methods; all targeted existing methods run, not skipped; their reports have zero failures/errors/skips. Maven verify also runs Surefire; do not label focus logs as full Failsafe inventory. Preserve each focused report before the next invocation overwrites XML. Show actual catalog results for 12/16 target tuples, 247 seven-role tuples, exact four-edge graph, writable 42501 denials, same-connection role cleanup, fourteen-history fresh/upgrade and SQL replay. The historical suite still proves the thirteen-history prefix explicitly, not latest.

### 8.2 Full build, inventory, JAR and document checks

```sh
cd backend
./mvnw -B -ntp verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

After the final full run, execute the exact Python heredoc from CI step `assert container smoke actually ran` (updated in `.github/workflows/ci.yml`) from `backend`, without editing counts at execution time. Require 36/309 + 15/246, source=CI=XML and fifteen POM includes. A convenient read-only extraction/execution command from repository root is:

```sh
python3 - <<'PY'
from pathlib import Path
import os
import subprocess
workflow = Path('.github/workflows/ci.yml').read_text()
step = workflow.split('      - name: assert container smoke actually ran\n', 1)[1]
body = step.split("          python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0]
script = '\n'.join(line[10:] if line.startswith('          ') else line for line in body.splitlines())
os.chdir('backend')
subprocess.run(['python3', '-c', script], check=True)
PY
```

Run unchanged CI `default and opt-in web jar smokes` and `production jar dependency firewall` steps after the full build. No dependency or production Java change is planned, so no firewall allowlist edit is allowed: preserve the first **82 dependency-coordinate adversarial probes**, plus **34 additional coordinate/JAR rejection probes in the packaged-JAR block**, for **116 total adversarial probes**, and current JAR exclusions/entry behavior. Precision correction candidate: the Controller/task wording "82 + 38 = 116" is arithmetically inconsistent and contradicted by checked-in CI: after `assert probes == 82` at line 1400, seventeen coordinates each invoke `rejected` twice before `assert probes == 116` at line 1427. Thus 34, not 38, is source-proven; neither assertion nor CI is changed. Controller disposition of this bounded wording correction remains open, without authorizing a later gate here. The following exact root-level command executes those checked-in step bodies, including the firewall's JAR index/dependency-list prerequisites, rather than copying an incomplete probe:

```sh
python3 - <<'PY'
from pathlib import Path
import re
import subprocess
import textwrap
workflow = Path('.github/workflows/ci.yml').read_text()
for name in ('default and opt-in web jar smokes', 'production jar dependency firewall'):
    pattern = r'(?ms)^      - name: ' + re.escape(name) + r'\n        run: \|\n(.*?)(?=^      - |^  [A-Za-z]|\Z)'
    match = re.search(pattern, workflow)
    assert match is not None, name
    subprocess.run(['bash', '-e', '-c', textwrap.dedent(match.group(1))], cwd='backend', check=True)
PY
```

The actual repository contains `api/openapi`, `protocol/ivr`, `security`, `deployment/capacity` and `acceptance`, including their checked-in tools/models/fixtures; only the earlier metadata-free plan-authoring export omitted those trees. Do not pre-classify them as absent or blocked. For the touched docs/14 and docs/17, implementation validation must first generate A1's bundle using the locked CI toolchain (Node 22.23.4 and the checked-in package-lock), from repository root:

```sh
node --version
(cd api/openapi && npm ci && npm run check:s3)
sha256sum api/openapi/package.json api/openapi/package-lock.json api/openapi/onecall-v1.openapi.yaml tmp/openapi-client/onecall-v1.bundle.json
```

Require the exact CI Node version, capture npm version, command exit statuses and sanitized log destinations, and hash all additional referenced A1 inputs/tool scripts. Alternatively use an explicitly supplied equivalent `tmp/openapi-client/onecall-v1.bundle.json` with provider/source revision, producing command/toolchain, input manifest and bundle SHA256; Backend/Protocol/Data must establish equivalence to these checked-in inputs, not silently accept a stale generated file. Generation of untracked validation outputs does not expand the twenty-path implementation write allowlist. Then run the required A2/A3/A5 checked-in commands, preserving all stage/fixture/model arguments, with this root-level extractor:

```sh
python3 - <<'PY'
from pathlib import Path
import re
import subprocess
import textwrap
for required in ('protocol/ivr/validate.mjs', 'security/validate.mjs', 'acceptance/validate.mjs', 'tmp/openapi-client/onecall-v1.bundle.json'):
    assert Path(required).is_file(), 'external validation prerequisite missing: ' + required
workflow = Path('.github/workflows/ci.yml').read_text()
for name in ('A2 s3', 'A3 s3', 'A5 s1', 'A5 s2'):
    pattern = r'(?ms)^      - name: ' + re.escape(name) + r'\n        run: \|\n(.*?)(?=^      - |^  [A-Za-z]|\Z)'
    match = re.search(pattern, workflow)
    assert match is not None, name
    subprocess.run(['bash', '-e', '-c', textwrap.dedent(match.group(1))], check=True)
PY
```

A validator may be recorded as blocked only after a concrete prerequisite check or command fails. Record each attempted command/check and exact exit status, failing prerequisite, exact input SHA256 manifest (workflow, validator scripts, specs, models, fixtures, lockfiles and generated/supplied bundle as applicable), sanitized log/artifact destination, owner and recovery destination. For a genuinely absent file record its exact missing path and the available-input manifest, not an invented hash; record downstream commands as not executed due to that specific failure, never as successful. Backend/Protocol/Data owns A1/input recovery and contract-regression rerun in the authorized validation environment; Security owns A3 security-model disposition. When real checked-in inputs are present, A2 s3, A3 s3, A5 s1 and A5 s2 are mandatory, not blanket blocked evidence. Record each validator's status separately. Existing A2/A5 baseline drift, only if concretely reproduced with original inputs and unchanged-file hashes, must be separated with owner disposition, not fixed by this work or counted as PA26 success; do not presume A3 baseline drift. No validator, npm install/network operation or build is executed or authorized at this fix gate.

### 8.3 PA25 psql apply/check/replay

ProductionDatabaseIdentityProvisioningIT's actual psql subprocess seam is the required automated proof; do not test only a JDBC-translated script, since psql flags, output suppression, variable quoting and exit markers are part of the contract. In its disposable PG16 container, also retain sanitized apply -> check -> apply -> check evidence using the existing script invocation:

```sh
psql -X -w --dbname=service=pa26_disposable_migration \
  -v mode=apply -v sharing_login=pa26_lq -v customer_login=pa26_lc -v command_login=pa26_lw \
  -f backend/ops/postgresql/pa25-runtime-identities.sql
psql -X -w --dbname=service=pa26_disposable_migration \
  -v mode=check -v sharing_login=pa26_lq -v customer_login=pa26_lc -v command_login=pa26_lw \
  -f backend/ops/postgresql/pa25-runtime-identities.sql
```

The service is a test-only externally configured connection, not a new repository file or a production service name; no password/URL secret in argv or output. The fixture may invoke these commands inside its container using the existing synthetic connection mechanism. Require only fixed unchanged six/seven/four success markers after commit; bad history/oracle/provenance/ACL/membership inputs exit nonzero with sanitized expected categories. Compare OIDs, marker bytes, passwords/verifiers, validity, graph, ACLs and business facts across replay, without printing them. Both modes reject V013-only, malformed V014 and future-history databases; neither mode silently repairs privileges. Existing settings, PUBLIC/default and redaction adversarial tests remain active.

### 8.4 Byte integrity and implementation scope

From repository root after implementation:

```sh
sha256sum -c /tmp/pa26-v001-v013.before.sha256
```

Compare additionally to section 9's exported SHA256 values. In a metadata-free snapshot, raw-byte hashes plus the controller's mechanically exported baseline are the authoritative local proof; never invent Git evidence. In the controller's real Git worktree only, the controller may additionally run:

```sh
git diff --exit-code eabf3db1 -- backend/src/main/resources/db/migration/V0{01,02,03,04,05,06,07,08,09,10,11,12,13}__*.sql
git diff --check
git diff --name-only eabf3db1
```

Expect no predecessor diff, no whitespace defect and only reviewed allowlisted implementation/Gateflow paths appropriate to that gate. Do not run these Git commands in this isolated export. Verify exactly fourteen migration resources, exactly one added migration and no business DDL/DML/function definitions in V014; compare actual catalog shapes as the stronger executable check. Validation is evidence, not permission to commit or enter a later gate.

## 9. Immutable predecessor manifest

Raw exported-file SHA256, measured during planning; filenames reside under `backend/src/main/resources/db/migration/`:

```text
96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6  V001__platform_schemas_roles_idempotency_outbox_audit.sql
4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505  V002__identity_organization_and_employee.sql
4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7  V003__subject_number_and_claim.sql
8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030  V004__catalog_order_and_payment.sql
6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065  V005__ivr_task_session_call.sql
7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d  V006__trial_entitlement_issuance_reservation_usage.sql
bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81  V007__activation_and_consent.sql
32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde  V008__evidence_media.sql
3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1  V009__sharing.sql
d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559  V010__sharing_role_least_privilege.sql
ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e  V011__sharing_public_read_least_privilege.sql
ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6  V012__sharing_admission_least_privilege.sql
1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e  V013__evidence_media_reader_role.sql
```

Flyway checksums are a separate algorithm from these raw-byte hashes. Compute and freeze V014's actual Flyway checksum only after its bytes settle; never substitute SHA256 or guess a signed checksum value.

## 10. Documentation decision, residuals and over-design judgment

Documentation is required, limited to the four allowlisted README/security/data-model/runbook paths. Each must distinguish historical structural capabilities from the current runtime overlay, state no Java/membership change, document exact projection sensitivity and no future write capability, and keep live acceptance NOT PERFORMED / production readiness NOT CLAIMED. Runbook order becomes migrate/validate V001-V014 -> PA25 apply -> externally assign credentials -> check/connection acceptance; a V013-only deployment must stop, not choose a legacy oracle. Q17/Q23 owners remain unchanged.

Section 2.3's explicit current-claim supersessions are mandatory, including README:9, the PA25 runbook:28 current ACK/risk statement, and the affected docs/14 grant/capability rows/sections. The S1 report must enumerate every changed or superseded current target-role claim with its input anchor, old claim, current replacement or precise linked supersession, and preserved historical context; merely adding a general overlay is not enough. Do not rewrite/delete V001-V013 model history or change unrelated-role claims. Contract validation must follow section 8.2: generate the A1 bundle via locked `npm ci` / `npm run check:s3` or use a supplied equivalent with provenance/hash, then require A2/A3/A5 against the real checked-in inputs. Only a concrete prerequisite/command failure may become an individually evidenced blocked item.

| Residual / blocked external item | Owner and destination | Treatment |
| --- | --- | --- |
| Real credentials, pg_hba/TLS, login names, migration superuser custody, protected comment writers and exclusive cluster administration | Platform/Database/Security -> Q17/Q23 deployment/security readiness | External, not executed; no production-readiness inference |
| Credential rotation, old-password denial, existing sessions/pool recycling, outage recovery | Security/Operations -> PA25 runbook live acceptance | Preserve handoff and redaction; local replay is not credential rotation |
| Mutable postgres:16 image, runtime patch/digest and hosted resource ceiling | Backend/CI/Database -> actual test evidence and existing CI operation | Record actual PG16/digest and zero skips; no image/CI architecture redesign |
| Previously provisioned non-PA25 or contaminated roles/defaults | Platform/Database/Security -> separately approved remediation | Migration/check stop; no adoption or destructive automatic cleanup |
| Future customer/profile/evidence writes and their invoker-guard query needs | Backend/Database/Security -> future separately confirmed feature and privilege-only migration | Intentionally unsupported now; not an acceptance target or deferred PA26 implementation |
| Remaining row-level sensitivity of identity_at_capture and cross-row projection SELECT | Sharing/Evidence/Security -> existing application authorization contract | Retained because currently used; no claim of tenant RLS or broad content declassification |
| Historical foundation suite versus latest runtime suite | Backend/Test maintenance -> explicit section 6.4 and full exact inventory | Prefix isolation must be visible; latest uncapped application, PA25 and V014 proofs are mandatory |
| Existing A2/A5 fixture/model baseline drift | Protocol/Data/Backend -> pre-existing baseline owner workflow | No unrelated fixture edit; separate reproduced limitation from PA26 results |
| A1 bundle generation/equivalence and required A2/A3/A5 execution evidence | Backend/Protocol/Data (Security for A3 disposition) -> authorized contract-regression environment and S1 report | Real tool/model trees exist; not pre-classified blocked. Follow section 8.2 and record exact input hashes/status/owner/recovery only after a specific failure; no validator pass claimed here |
| Flyway target=013 pending/all/V014 state observation | Backend/Database -> S1 disposable PG16 evidence and PostgresContainerIT assertions | Open implementation evidence question; preserve exact pending/resolved oracles and section 6.4's five-migration/history/replay contracts |
| Firewall precision wording: requested 38 versus source-proven 34 additional probes | Controller -> disposition of this fixed-plan candidate; Backend/CI -> unchanged 116-probe S1 evidence | Bounded source-proven correction proposed in section 8.2; no CI edit or independent acceptance claimed |
| Latest suites requiring zero changes (review B's open question) | Backend -> S1 implementation report | List inspected suites and latest-chain results, including CustomerMeIT, CustomerSharesIT, CustomerShareCommandsIT, MediaPlaybackIT and MediaGatewayTransportIT when unchanged; reporting does not add them to the implementation allowlist |

No blocking product question remains after the binding scope confirmation. Runtime validation unavailable at plan time is an implementation evidence prerequisite, not a reason to fabricate success.

**First-principles/over-design judgment:** removing unused SQL capabilities solves the actual problem. Keep one migration and one composed implementation slice. Keep the needed evidence assumption rather than expanding command privileges or rewriting a public projection collaborator. Do not add a second policy registry, catalog-drift query per request, role simulator, future-service matrix, credential manager, wrapper function, view, new module or generic ACL framework. Literal expected sets are confined to migration/provisioning guards and independently authored tests; they do not become another runtime authority. The historical test prefix is explicit evidence preservation, not a substitute for latest-state acceptance.

## 11. Later implementation completion report format

The later task writes only its allowlisted report `docs/gateflow/pa26-database-runtime-acl-rbac-hardening-s1-implementation.md` in addition to the implementation paths. It must contain:

1. Task/provider, input/final provenance, gate and exact changed path list; accepted plan path **`docs/gateflow/pa26-database-runtime-acl-rbac-hardening-plan.md`** and its independently measured SHA256.
2. Source-derived result: no production Java change; unchanged six/seven/four role graph; exact customer 12 and evidence 16 ACL tuples, seven-role total 247 and unchanged 219 other tuples; list actual retained/removed grants and any reviewed deviation.
3. V014 filename/bytes/SHA256/Flyway checksum; predecessor raw-byte manifest comparison; append-only privilege-only structural/catalog proof; replay/atomic rejection results; no ownership/default/PUBLIC/membership or business-definition drift.
4. Every focused/full command, exit status, actual PG16/image/toolchain facts, logs/XML artifact destinations and source=CI=XML unique-method proof; expected 36/309 + 15/246 = 51/555, scan 556, fifteen includes, twelve new methods and preserved PA25 twenty. Include section 6.4's actual info().all()/pending()/V014 MigrationInfo state observation under target=013 and the exact resulting non-weakened assertions; preserve initialMigration==5, 013 history tail and zero replay.
5. Positive application paths, exact writable 42501 negatives, separate read-only/business/integrity errors, pool restoration, owner-invoker/deferred-FK proof, and unchanged JAR smokes/firewall probes.
6. Actual psql apply/check/replay and stale-history/ACL/provenance failure evidence, sanitized markers, preserved credential/provenance state without secret values, and documentation consistency. Identify named clean and credential-assigned graph upgrade/replay cases and byte-identical verifier/marker/OID/edge preservation. Provide the complete section 10 current-claim supersession ledger and section 8.2 A1 bundle provenance/hash plus per-validator exact input manifests, exit statuses and individually justified blocked/recovery records if any.
7. Residual owners/destinations, blocked external work, baseline failures and scope deviations; explicit no live production acceptance, no readiness claim and no unauthorized later gate/external operation.
8. Report's own artifact path and externally measured byte/line/SHA256 metadata in the task handoff. Do not embed a self-referential hash as if the file could hash itself.
9. Backend must list every inspected latest suite that required zero changes, with latest-V014 execution evidence and the reason its existing oracle remains valid; include the disposition of review B's CustomerMeIT/CustomerSharesIT/CustomerShareCommandsIT/MediaPlaybackIT/MediaGatewayTransportIT open question. This is report responsibility, not implementation scope expansion; any proven required edit outside the twenty paths still requires plan amendment before editing.

## 12. Plan-author completion boundary

The original plan-author gate produced only this plan. At the current fix gate the only authorized outputs are this cumulative fixed candidate and `docs/gateflow/pa26-database-runtime-acl-rbac-hardening-plan-fix-1.md`. Repository source, migration/test/POM/CI inputs, role/ACL histories, Control and both existing reviews remain read-only. Both outputs' byte/line/SHA256 metadata is measured externally after strict UTF-8/LF, exactly one final LF and no-trailing-whitespace validation. This is neither re-review nor independent acceptance: no implementation, runtime/contract test, build, Docker, credential/production operation, later gate or external mutation is performed.
