# PA36 evidence-role IVR association ACL — implementation plan

Task: `pa36-evidence-role-ivr-acl-plan-gpt-20261116-01`  
Work unit: `PA36-evidence-role-ivr-association-acl`  
Date: 2026-11-16 (Asia/Shanghai)  
Status: planning artifact for Controller intake and independent A/B Planreview; **not implementation authorization or a passed gate**.

## 1. Authority, motivation and acceptance contract

The Active PA36 checkpoint in `docs/gateflow/project-execution-control.md` is authoritative. Option A authorizes precisely two evidence-owner column grants and the identity/verifier/documentation/test lockstep needed by the paused PA35 detail association. Design authority remains `docs/12-technology-stack-and-system-architecture.md` §§6–8: owner-module query interfaces, one database with explicit ownership, independent employee authentication and no automatic employee playback authority. Docs14 defines the existing associations; docs17 defines the runtime boundary. PA36 does not revise those business contracts.

The V017 state permits `evidence_role` to select six `evidence.evidence_record` columns: `id`, `subject_id`, `status`, `source_type`, `source_ref_id`, `identity_at_capture`. It cannot select `session_id` or `call_id`. Inferring either association from `source_ref_id`, reading it through another privileged module, or broadening to table SELECT is not an acceptable workaround. The new capability must remain owned by evidence; PA35 later supplies its own owner collaborator and application authorization.

Success requires all of the following, not merely two successful GRANT statements:

1. Fresh PostgreSQL16 V001→V018 migration and V017→V018 upgrade validate; V001–V017 bytes/history remain unchanged.
2. Under an ordinary login and `SET LOCAL ROLE evidence_role`, all eight approved record columns are selectable; every other record column remains denied with SQLSTATE `42501`. No new table SELECT, writes, grant options, routines, ownership or business facts appear.
3. The dedicated Operations IVR reader login can SET exactly `operations_iam_role`, `ivr_role`, `evidence_role`, cannot inherit their object privileges and restores login/role/transaction state on success and failure. Existing sharing/customer/command/direct-login boundaries remain intact.
4. Current PA25 apply/check validates exactly 18 SQL history rows, the expanded exact ACL oracle and the exact approved graph. Clean application, upgrade, replay and adversarial cases have zero-skip XML evidence. No drift is repaired by revoking or widening object ACLs.
5. Source identities, CI inventories, POM includes and XML agree exactly. Product Java/configuration and both OpenAPI files remain byte-identical; PA35 remains unimplemented.

Non-goals: PA35 routes/controllers/services/DTOs/cursors/collaborators; any API/schema/business-data change; new datasource or pool; new database runtime/owner role; role hierarchy; modifying existing role attributes; RLS/tenancy redesign; new evidence/media payload, URL, locator, crypto or playback exposure; unrelated hardening; credentials in Git; production connection/deployment acceptance. No broad table SELECT or new write is authorized. Existing Operations/IVR role writes are preserved, **not newly granted and not represented as read-only ACLs**.

One coherent S1 slice is required. V018, PA25, docs17/runbook and verification ship together. Splitting would leave either an unusable capability or an obsolete production verifier. Controller bookkeeping/reviews are subsequent gates, not an implementation sub-slice.

## 2. Inspected baseline and immutable facts

Read sources include the active checkpoint; architecture §§6–8; docs14 evidence/IVR identities and privilege rules; docs17 current and historical boundaries; frozen Operations IVR OpenAPI operations; V001–V017; PA25 SQL/runbook/accepted plan; current provisioning, ACL, boundary and Operations fixtures/tests; `backend/pom.xml`; `.github/workflows/ci.yml` including identity/XML/dependency/package firewalls. Important reconciliation facts:

- Control describes the merged PA34 base `5200355695d39b6aeba446631c8f3f041b96e9ba`. Local HEAD is `c89e7845f6b15b18fca2116bd3633b8e565b4aea` (`gateflow: confirm PA36 ACL prerequisite goal`), its direct child. Read-only Git proved the base is an ancestor and the only base→HEAD changed path is Control. These are distinct, reconciled checkpoints; production/test baseline bytes are unchanged. Pre-existing private `.codex` is not task output.
- The worktree `.git` contains a Windows `D:/...` pointer. In WSL, read-only Git works with `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"`. Do not rewrite `.git` or repository config to fix tooling.
- Current PA25 `$frozen$` has **288** seven-role tuples, not its stale lower runbook paragraph's 261. Current history is **17**, not lower historical 13/15 statements. PA29's top sections supersede older prose but do not describe PA34's current test inventory.
- Current source/CI/POM baseline is **76 Surefire classes / 704 methods + 31 Failsafe classes / 504 methods = 107 / 1208**. Annotation-line scan is **1209**, with the single frozen `PostgresContainerIT` comment identified by CI. There are 31 exact Failsafe includes. PA29's 49/417 + 21/336 inventory remains historical.
- V014 established evidence16 = one schema USAGE + fifteen column SELECT tuples. V017 changed customer only: customer53 + evidence16 + other219 = 288. The new evidence role total is 18, not eight: eight is only its `evidence_record` column count.
- `OperationsReadContext` currently enters `operations_iam_role`, switches only its existing fixed enum roles and restores the employee role/login. It has **no IVR enum member**. PA36 does not add one. Existing PA33 subject-read fixtures deliberately grant six owner roles to an unmarked synthetic login; they are not the new three-edge production IVR topology.

Preserve the original bytes, including CRLF. The following are the planning worktree's **raw** SHA256 pins; existing LF-normalized test pins must also stay unchanged. Do not normalize frozen files in place.

| Version | Raw SHA256 |
| --- | --- |
| V001 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| V002 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| V003 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| V004 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| V005 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| V006 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| V007 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| V008 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| V009 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| V010 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |
| V011 | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` |
| V012 | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` |
| V013 | `1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e` |
| V014 | `bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8` |
| V015 | `c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc` |
| V016 | `a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c` |
| V017 | `74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3` |

For example V017's existing documented `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad` is the LF-normalized digest here, not this checkout's raw digest. Its Flyway checksum remains **-41822162** in both representations. New current prose must distinguish raw and normalized digests, without retroactively editing historical claims.

## 3. Append-only V018 specification

### 3.1 Name, envelope and only mutation

Create `backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql`, UTF-8 without BOM, LF, final newline. Version `018`, rank 18, type `SQL`, description `evidence role ivr association read privileges`. Do not reserve V019 or rename any predecessor.

Use the existing guarded `DO $migration$` pattern, two-pass preflight/postflight, fixed sanitized `P0001` categories, PG16 check, actual superuser migration-session requirement (`current_user = session_user`), transaction-local `pg_catalog` search path and the existing shared transaction advisory lock `(725025, 1)`. This is protected offline migration authority, never an application fallback. Neither V018 nor its replay creates/alters roles, comments or memberships.

The **only object-ACL mutation**, executed only when the full clean old ACL state is proven, is:

```sql
SET LOCAL ROLE evidence_owner;
GRANT SELECT (session_id, call_id)
    ON TABLE evidence.evidence_record TO evidence_role;
```

Restore the original migration session role with a safely quoted identifier in the same DO block; do not interpolate client input. A single owner-issued GRANT adds both tuples; no grant option. No REVOKE/reconciliation of incompatible states. Exact complete state replay issues no GRANT and changes nothing.

### 3.2 Exact ACL oracle and arithmetic

Use independent literal tuple oracles in V018, current PA25 and tests; test expected values must not be obtained by parsing the SQL being tested. Tuple identity is `(grantee, kind, qualified object or routine signature, column-or-empty, privilege, grantor, is_grantable)`. Resolve qualified identities to OID/attnum/signature, reject unresolved/dropped/duplicate entries, and compare both set directions **and cardinality**.

The two new tuples are exactly:

```text
evidence_role|column|evidence.evidence_record|session_id|SELECT|evidence_owner|false
evidence_role|column|evidence.evidence_record|call_id|SELECT|evidence_owner|false
```

The original seven-role oracle is **288→290 = customer53 + evidence18 + others219**; the evidence-only target is **16→18**, with the six existing record columns unchanged. The expanded nine-role oracle includes two already-existing role ACL inventories: **operations_iam_role14 + ivr_role106 = 120 unchanged tuples**. Therefore expanded pre/post counts are **408→410**. This 120-tuple expansion is verifier coverage, **not 120 new grants**. Projecting post-state back to the original seven roles must produce exactly 290; subtracting only the two new tuples must recover exactly 288.

Association types are already fixed by V008/docs14: `session_id uuid` references `ivr.inbound_session(id)` and `call_id text` references canonical `ivr.call_record(call_id)`. They are not interchangeable public UUIDs and neither is derived from `source_ref_id`. Positive fixtures cover null associations, call-only association and valid session+call association with matching immutable source/snapshot facts and existing FK parents. No backfill, index, FK or uniqueness change is needed.

Freeze the two newly monitored role inventories from their original owner-issued grants, all non-grantable:

| Role / owner | Existing exact capabilities (one tuple per privilege/column) |
| --- | --- |
| Operations / `platform_reliability_owner` | platform USAGE; EXECUTE `platform.idempotency_begin(text,text,text)`, `platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)`, `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` = 4 |
| Operations / `audit_owner` | audit USAGE; EXECUTE `audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)` = 2 |
| Operations / `operations_iam_owner` | operations_iam USAGE; employee_account table SELECT and INSERT; column UPDATE `display_name,role,status,revision`; EXECUTE `operations_iam.guard_employee_account()` = 8 |
| IVR / `platform_reliability_owner`, `audit_owner` | Same platform4 + audit2 = 6 |
| IVR / `activation_consent_owner`, `entitlement_metering_owner` | respective schema USAGE = 2 |
| IVR / `ivr_owner` | ivr USAGE; table SELECT and INSERT on each of outbound_call_task, inbound_session, call_record, call_leg, call_event, ivr_decision = 13 |
| IVR / `ivr_owner` | UPDATE outbound_call_task `status,reason_code,claimed_call_id,claimed_at,terminal_at,updated_at,revision` = 7; inbound_session `status,confirmed_subject_id,guidance_status,confirmed_at,revision` = 5; call_record `task_id,status,started_at,bridged_at,recording_started_at,recording_ended_at,ended_at,authoritative_seconds,end_reason,revision` = 10; call_leg `state,answered_at,ended_at,end_reason,revision` = 5; call_event `processing_status,reason_code,revision` = 3 |
| IVR / `ivr_owner` | EXECUTE guard_outbound_call_task(), guard_inbound_session(), guard_call_record_parties(), guard_call_leg() = 4 |
| IVR / `customer_iam_owner` | customer_iam USAGE; customer_account SELECT `id,status` = 3 |
| IVR / `organization_access_owner` | organization_access USAGE; organization SELECT `id,status`; organization_membership SELECT `organization_id,account_id,role,status` = 7 |
| IVR / `subject_number_owner` | subject_number USAGE; business_subject SELECT `id,owner_account_id,owner_organization_id,revision`; caller_number_binding SELECT `id,subject_id,number_hmac,status,verified_at,revision`; subject_risk_state SELECT `caller_number_binding_id,risk_type,state,revision` = 15 |
| IVR / `entitlement_metering_owner` | entitlement_balance SELECT `id,grant_id,granted_units,reserved_units,consumed_units,status`; entitlement_grant SELECT `id,subject_id,entitlement_type,granted_units,valid_from,expires_at` = 12 |
| IVR / `activation_consent_owner` | service_activation SELECT `id,subject_id,status,revision`; consent_record SELECT `subject_id,purpose,channel,policy_code,policy_version,prompt_version,action,event_domain,event_id,accepted_at` = 14 |

Check the arithmetic separately: Operations `4+2+8=14`; IVR `6+2+13+30+4+3+7+15+12+14=106`. Qualify all table/function names in literal oracles; normalize PostgreSQL's `timestamp with time zone`/`timestamptz` signature through `regprocedure`, not string guessing.

### 3.3 History, provenance, replay and rejection

Copy the **unchanged** 17 `(version,script,checksum,description)` history facts from current PA25 into V018. Ordinality fixes ranks 1–17. All must be successful SQL rows; reject missing, duplicate, failed, baseline/repair-style replacement, wrong rank/version/description/script/checksum and unexpected rows. Frozen signed checksums, versions 001→017, are:

```text
-709123726,-1229975166,-1355776370,-665258487,-1005314773,366834714,
-1281725572,-245169007,-59961153,247431460,492686454,-291628548,
-611381791,-340359494,-1316980027,1669606497,-41822162
```

Accepted entry combinations are deliberately finite:

| Applied history | ACL state | Marked external topology | Outcome |
| --- | --- | --- | --- |
| exact V001–V017 | exact old 408 | none, or complete historical three slots/four edges | grant both; post-state410 |
| exact V001–V017 | exact final410 | none, or complete historical three slots/four edges | no-op raw replay before Flyway records V018 |
| exact V001–V018 metadata | exact final410 | none, historical three/four, or complete new four/seven | no-op replay |
| any other combination | any | any | reject without persistent mutation |

In particular: old ACLs with an applied V018 row, partial one-column additions, four-slot topology before V018, malformed/partial marker sets and histories beyond V018 reject. V018 recognizes exact PA25 marker sets of zero, three old slots or four new slots only; three means all old slots and four approved old edges, four means all slots and seven edges. Discover actual names from validated creation provenance, never guessed login names. Verify UTF-8 marker equality, current database OID, role OID, slot and role name; reject any other `onecall:pa25%` marker. Partial provisioning must be completed by the compatible PA25 version before retrying a migration, never adopted by V018.

During execution V018 cannot embed its own eventual checksum without a circular file-content dependency. Its optional self history row must have exact rank/version/type/success/script/description and a non-null checksum; **all predecessor checksums are exact**. Flyway validate and current PA25 subsequently enforce the exact V018 checksum. Do not claim raw DO replay alone authenticates its own file checksum. Wrong V018 checksum must fail both external validation paths; predecessor checksum drift must also fail V018 itself.

After V018 bytes settle, obtain its signed checksum from pinned Flyway's pending migration info and cross-check the repository's existing UTF-8 line-by-line CRC32 convention. Record that concrete integer in PA25's eighteenth history tuple and boundary tests; record raw SHA256 and LF SHA256 in docs17/runbook/implementation evidence. Recompute after any V018 edit. There is no invented checksum in this plan and no runtime wildcard/zero/learn-from-database checksum oracle.

Negative guards must retain the V017 mechanisms, expanded to all nine monitored roles and four external slots: authority, provenance, exact history, settings, role attributes, touching memberships (including grantor), schema/relation/routine/type ownership, explicit and effective ACLs, PUBLIC, database CONNECT-only boundary, default ACLs (including empty runtime-owned rows), shared and extension dependencies. Reject wrong-grantor and grant-option tuples even if effective SELECT succeeds. Compare schema/table/column/sequence/routine ACLs with table-to-column implications handled as in PA25; do not accidentally deny the unchanged Operations/IVR table rights listed above.

Use fixed categories `V018_AUTHORITY_REJECTED`, `PROVENANCE`, `HISTORY`, `SETTINGS`, `ATTRIBUTES`, `TOPOLOGY`, `OWNERSHIP`, `ACL`, `DATABASE`, `DEFAULT_ACL`, `DEPENDENCY`, `PRESERVATION`, `RECONCILIATION` with the common `V018_…_REJECTED` spelling. Capture before/after invariants for roles/password verifiers/comments/memberships/settings/defaults/databases/owners and all non-target ACLs; assert exact target delta separately. Tests additionally snapshot business rows, definitions, constraints, indexes and triggers. The exception envelope sanitizes all failures and the DO/Flyway transaction rolls the entire delta back. Do not catch-and-continue or commit half a grant.

## 4. Minimum production identity topology

### 4.1 Decision and exact final graph

Add **one dedicated externally named login LO**, PA25 slot `operations_ivr_read`, input `operations_login`. It uses the existing shared Operations-read datasource properties; no new pool/configuration source or frontend identity is introduced. Alternatives fail least privilege: extending LQ/LC mixes sharing/customer with employee authority; extending LW imports command writes; using a direct public/admission/media login violates frozen direct boundaries; a base NOLOGIN role cannot authenticate and no one existing login already has the required set.

Final graph (all and only these edges):

```text
LQ -> sharing_role
LC -> customer_iam_role
LW -> sharing_command_role
LW -> evidence_role
LO -> operations_iam_role
LO -> ivr_role
LO -> evidence_role
```

Every edge is `ADMIN FALSE, INHERIT FALSE, SET TRUE`, one catalog row per pair, grantor an actual unmanaged superuser as in PA25 (not hardcoded to one superuser name); compatible existing grantors remain unchanged. No runtime role is a member of another. Add no reverse/transitive bridge. Three unrelated direct login boundaries remain `sharing_public_read_role`, `sharing_admission_role`, `evidence_media_reader_role`, with their exact existing flags/expiry/ACLs and no touching edges.

Counts are **7 login identities** (four external plus three direct), **9 effective capability roles** (old seven plus operations/ivr), **7 edges**, **13 managed role names** (four external plus nine roles). Fresh migrations still create **31** project roles; PA25 adds four, for **35** after apply (excluding fixture/admin/pg roles), versus historical34. A direct role is counted once in managed names although it is also a login. No new owner/runtime role is created by Flyway.

LO and the other external logins are LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS, connlimit -1, no role/database settings, no expiry by default, no object privileges, ownership or default ACLs. Creation uses PASSWORD NULL; external credential assignment/rotation remains external. LO has the same exact creation-only PA25 v1 marker format with its new slot. Existing role OIDs/comment/password bytes and old edges survive upgrade. `operations_iam_role` and `ivr_role` stay NOLOGIN and retain their migration-default INHERIT flag; there are no edges for them to inherit, and **membership inherit_option FALSE plus login NOINHERIT** prevents automatic privilege inheritance. Do not ALTER base roles merely to cosmetically make every `rolinherit` false.

“No role DDL” applies to V018 and changing migration-defined roles. The existing PA25 administrator executable necessarily performs its already-authorized external-login CREATE/compatible ALTER/creation COMMENT/membership GRANT mechanism for LO. It does not redefine any existing runtime/owner role. No role DDL is run by application code.

### 4.2 Context switching and limits

On a real ordinary TCP connection as LO: `session_user` stays LO through `SET LOCAL ROLE operations_iam_role`, IVR, evidence, employee, and `SET LOCAL ROLE NONE`. SET authority is rooted in session_user, so restoring from evidence to employee works without a runtime-role hierarchy. Check fixed backend PID/connection, explicit read-only REQUIRES_NEW/REPEATABLE_READ transaction, role and login restoration, rollback/completion failure, and clean pool reuse. Test this PostgreSQL capability with test-only fixed SQL, not by adding an IVR enum/member/service to production `OperationsReadContext`.

LO has no object access before SET, cannot SET any of the other six effective roles or owner/pg roles, cannot grant membership and cannot become an unrelated login. LQ/LC/LW and each direct login cannot SET Operations/IVR or LO; LW retains evidence as before. Test schema USAGE as well as column privileges: a column query failing only because schema is inaccessible does not prove the target column ACL.

This is the **minimum PA35 IVR-read identity**, not a claim that the six-role PA33 subject reader or other Operations command pools are production-provisioned by PA25. Do not add subject/activation/trial/order/service roles to LO to make unrelated endpoints work. The existing pool may be configured with LO for the PA35 capability after PA35 adds its fixed switch; if deployment intends one login to serve additional owner collaborators, that wider production identity decision returns to Controller goal confirmation. Document this destination explicitly rather than silently promising all Operations endpoints will work with LO.

## 5. PA25, runbook and docs17 lockstep

In `backend/ops/postgresql/pa25-runtime-identities.sql`:

- Add optional psql-input initialization for `operations_login` (empty default solely to produce sanitized input rejection); persist only transaction-local `pa25.operations_login`. Both modes require all **four** inputs, distinct validated names, applying the same length/identifier/reserved/owner/schema/effective-role collision rules. No backward-compatible three-input success path.
- Expand slots/names/creation/provenance loops to four. Build direct-login names explicitly rather than accidentally extending positional `[5:7]` slices to the new roles. Add operations/ivr to monitored roles; their existing safe NOLOGIN/INHERIT attributes are exact, not repaired. Use the same twelve-schema owner map, already including operations_iam and ivr.
- Expand independent literal `$frozen$` from288 to410 (two new tuples +120 existing monitored tuples); change the exact cardinality guard to410. Keep old288 as an explicit projection assertion in tests, not an alternate runtime acceptance state. Update `$history$` to exact18 with V018 checksum. V017-only databases fail `PA25_HISTORY_REJECTED` before reconciliation.
- Expand touching-edge allowed pairs and final count4→7. `apply` may create only absent approved external logins and add only absent approved edges; it must reject malformed existing attributes/markers, unsafe edges/grantors, explicit/effective ACL drift or ownership/default changes before mutation. `check` repairs nothing and rejects any missing login/edge. All loops remain within one explicit transaction and the shared advisory lock.
- Preserve existing password verifiers, validity, OIDs, settings and correct markers; never restamp/adopt an existing unmarked name. Existing three marked logins plus absent LO is the intended upgrade path. An unmarked colliding LO name is a provenance failure, not an adoption opportunity. Existing nonempty/empty default-ACL ownership guards apply to LO and the two newly monitored roles.
- Success output after COMMIT is exactly `PA25_APPLY_OK logins=7 effective_roles=9 memberships=7` or `PA25_CHECK_OK logins=7 effective_roles=9 memberships=7`. Do not append login names, credentials, catalog contents or marker values. Retain fixed PA25 rejection categories and redaction.

Prepend `PA36 Current Runtime Boundary (V018)` to docs17 and the PA25 runbook. Preserve all existing PA29/28/27/26/A3 historical text and evidence inventories verbatim under their historical applicability; do not globally replace numbers. New section must explicitly supersede current claims in older sections, state V018's exact two-column delta, 408→410/288→290 counts and unchanged120, eight-column evidence_record set, 18 history rows/checksum/digests, seven/nine/seven graph, no inheritance, old marker custody, ordinary reader limitations, no PA35 implementation and new test counts below. Preserve all existing API descriptions as historical/unchanged behavior, not new PA36 endpoints.

The runbook's new authoritative procedure includes four validated psql variables and a private `PA25_OPERATIONS_LOGIN` mapping alongside the existing three. Extend the external environment/credential checklist to seven credentials and four private mappings; show `-X -w`, protected service/connection selection, no command-line passwords and sanitization of pre-script connection errors. Wire LO only through existing server-managed `onecall.operations-subject-read.datasource.url|username|password` settings when its consumer is authorized; no default username/password, pool rename or client selector. Apply/validate V018 **before** current PA25; stop application rollout on any mismatch. Record actual PG16 patch/image digest, cluster/exclusive administration, superuser/migrator custody, TLS/pg_hba, credential distribution, seven real TCP positive/negative proofs, rotation, old-session drain and recovery ownership as NOT EXECUTED until externally proved. Do not claim repository tests certify live production.

Historical commands below the new section remain historical and must be explicitly scoped as non-current; provide the complete executable current command sequence in the new section rather than expecting readers to combine incompatible instructions.

## 6. Exact write ownership and paths

One Backend/Database implementer owns S1, with Database/Security reviewing grants/provenance and Controller owning gates. Relative test filenames in this table are under `backend/src/test/java/com/onecall/archtests/`. No source implementation path is authorized.

| # | Path | Status / bounded responsibility |
| --- | --- | --- |
| 1 | `backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql` | NEW; §3 only |
| 2 | `backend/ops/postgresql/pa25-runtime-identities.sql` | EXISTING; §5 exact verifier/topology delta |
| 3 | `docs/operations/pa25-production-database-identity-provisioning-runbook.md` | EXISTING; prepend authoritative current procedure |
| 4 | `docs/17-production-security-design-spec.md` | EXISTING; prepend current boundary; historical bytes preserved |
| 5 | `container/EvidenceRoleIvrAssociationAclIT.java` | NEW; sixteen methods in §7 |
| 6 | `architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java` | NEW; eight methods in §7 |
| 7 | `container/ProductionDatabaseIdentityProvisioningIT.java` | EXISTING; retain twenty method identities; expand graph/oracle/history/credentials/input/negative assertions |
| 8 | `container/CustomerBusinessNumbersFixture.java` | EXISTING; latest18, fourth PA25 variable and optional explicit historical provision helper; no application behavior |
| 9 | `container/CustomerBusinessNumberReadPrivilegeIT.java` | EXISTING; historical V015 assertions unchanged; latest PA25/current tail18/290/new marker |
| 10 | `container/CustomerSubjectDetailReadPrivilegeIT.java` | EXISTING; historical V016 assertions unchanged; latest tail018/current history290 |
| 11 | `container/CustomerEvidenceReadPrivilegeIT.java` | EXISTING; pin historical017; explicit historical graph setup; current PA25 tail018; historical288 oracle retained |
| 12 | `architecture/CustomerBusinessNumberBoundaryTests.java` | EXISTING; current PA25410/18 tail only, V015 pins unchanged |
| 13 | `architecture/CustomerSubjectDetailBoundaryTests.java` | EXISTING; current PA25410/18 tail only, V016 pins unchanged |
| 14 | `architecture/CustomerEvidenceBoundaryTests.java` | EXISTING; historical V01750→69/288 checks retained; current PA25410/18 projection and history tail |
| 15 | `container/SharingQueryFixture.java` | EXISTING; latest migration/applied count17→18 only |
| 16 | `container/CustomerMeFixture.java` | EXISTING; latest migration/applied count17→18 only |
| 17 | `container/SharingRoleLeastPrivilegeIT.java` | EXISTING; latest count17→18 only, historical grants unchanged |
| 18 | `container/EvidenceMediaReadIT.java` | EXISTING; latest migrate count17→18 only |
| 19 | `container/ActivationCommandRoleLeastPrivilegeIT.java` | EXISTING; latest-chain ACL projection290/history18, V017 delta19 unchanged |
| 20 | `container/OperationsLifecycleRoleLeastPrivilegeIT.java` | EXISTING; latest projection290/history18, unchanged lifecycle behavior |
| 21 | `container/OperationsActivationReadRoleLeastPrivilegeIT.java` | EXISTING; latest history18 and current PA25 assertion; unchanged activation behavior |
| 22 | `.github/workflows/ci.yml` | EXISTING; exact PA36 inventories/tokens/arithmetic and package assertion only |
| 23 | `backend/pom.xml` | EXISTING; one exact Failsafe include only |
| 24 | `container/CustomerBusinessNumbersIT.java` | EXISTING; latest-chain successful history count17→18 only |
| 25 | `container/CustomerEvidenceIT.java` | EXISTING; latest-chain successful SQL history count17→18 only |
| 26 | `container/CustomerSubjectClaimsIT.java` | EXISTING; latest applied history size17→18 only |
| 27 | `container/CustomerSubjectsIT.java` | EXISTING; latest successful SQL history count17→18 only |
| 28 | `container/EvidenceMediaReadRoleLeastPrivilegeIT.java` | EXISTING; latest applied history size17→18 only |
| 29 | `container/OperationsActivationReadsIT.java` | EXISTING; latest successful history count17→18 only |
| 30 | `container/OperationsSubjectReadRoleLeastPrivilegeIT.java` | EXISTING; latest successful history count17→18 only |
| 31 | `container/OperationsSubjectReadsIT.java` | EXISTING; latest successful history count17→18 only |
| 32 | `container/PublicShareAdmissionIT.java` | EXISTING; latest applied history size17→18 only |
| 33 | `container/SharingAdmissionRoleLeastPrivilegeIT.java` | EXISTING; latest applied history size17→18 only |
| 34 | `container/SharingQueryServiceIT.java` | EXISTING; latest applied history size17→18 only |
| 35 | `container/SubjectClaimRoleLeastPrivilegeIT.java` | EXISTING; latest applied history size17→18 only |

Implementation permission arithmetic: **35 paths = 3 new + 32 existing**, including two new test classes and one new migration. Twelve of the existing paths are single-purpose latest-history assertion updates, not permission to change their application tests. This plan artifact is a separate planning deliverable, not a fourth new S1 implementation file. Controller separately authorizes its implementation-evidence artifact and any Control/review records; do not silently add them to the implementer's source write set.

Forbidden edits include V001–V017, docs12/docs14/docs15, OpenAPI, protocol/security JSON, all production Java/configuration, historical `PostgresContainerIT` and `DatabaseRuntimeAclRbacHardeningIT`, existing Operations fixtures' role sets, wrapper/toolchain/dependencies, prior reviews/plans and Control. If a newly found current-state assertion requires another file, report the exact path/reason for Controller scope correction, not a broad repository replacement.

## 7. Tests, historical compatibility and inventory

### 7.1 Eight new ordinary Surefire identities

Class `com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests` has exactly these eight non-parameterized, non-dynamic `@Test void` methods:

| Method | Required assertion |
| --- | --- |
| `v018NamesOnlyTheTwoOwnerIssuedAssociationColumns` | literal two-tuple delta, 408/410 and evidence16/18; no other mutating statement |
| `v018GuardsAuthorityHistoryProvenanceAndExactReplayStates` | fixed guard categories, exact17 prefix, permitted replay matrix, same advisory lock |
| `v001ThroughV017AndPublicContractsRemainByteFrozen` | independent raw/LF manifest; both OpenAPI files, docs12/docs14 and all product Java/config frozen against entry manifest |
| `pa25UsesExactEighteenHistoryAndIndependent410TupleOracle` | independent literal nine-role oracle, 290 projection, concrete V018 checksum and unchanged17 prefix |
| `pa25DefinesOnlyTheSevenApprovedNonInheritingEdges` | four inputs/slots, seven pairs, flags, nine roles, direct-login separation, creation-only marker |
| `currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory` | new authoritative counts/limits/checksum; entry snapshots of old document bodies remain byte-identical |
| `pa36HasNoApplicationApiPoolOrDependencyExpansion` | source/path allowlist; no PA35 Java/OpenAPI/config/dependency/new pool changes; V018 only packaged delta |
| `ciPomAndOrdinaryMethodInventoryMatchExactly` | literal names in this section, one include, no parameterized/dynamic/disabled/skipped identities or duplicate token |

Use independent frozen manifests/literals in the test code; never compute an expected baseline from the candidate at test execution. A hash test alone does not prove semantic privilege safety; the ITs do that.

### 7.2 Sixteen new ordinary Failsafe identities

Class `com.onecall.archtests.container.EvidenceRoleIvrAssociationAclIT`, fresh synthetic PG16 fixtures, has exactly these sixteen ordinary methods. Mutations inside one method are explicit loops with savepoints/snapshot comparison, not new parameterized XML identities.

| Method | Required assertion |
| --- | --- |
| `v017DeniesBothAssociationColumnsWith42501` | exact V017288/expanded408; each missing column denied separately under evidence_role |
| `freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors` | fresh18, upgrade exactly1, validate, full18 metadata/checksum; historical rows byte/semantic-identical |
| `v018AddsExactlyTwoOwnerIssuedTuplesAndMatches410Oracle` | both set differences, no removals, original seven290, customer53/evidence18/others219, Operations14/IVR106 |
| `evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn` | seeded valid nonempty evidence association values; select all8 plus individual positives; enumerate all remaining record columns and deny each, SELECT *, writes, locks, schema CREATE, routines/sequences/grant delegation; retain artifact/media sensitive denials |
| `v018ReplaysOnlyExactCleanOrCompleteStatesWithoutDrift` | raw before-history replay final17 and applied final18, none/old3/new4 marker cases as permitted; no duplicate ACLs or changed snapshots; migrate-again0 |
| `v018RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically` | either one-column pregrant; either missing final column; ungranted payload column; broad table SELECT; unrelated extra ACL; substitute grantor; either grant option; no partial repair |
| `v018RejectsAuthorityHistoryAndProvenanceDriftAtomically` | non-PG16 guard where feasible/static pin otherwise; non-superuser/current-role mismatch; every predecessor history field/rank deletion plus extras/duplicates; fabricated applied row with old ACL; malformed/wrong database/role/slot/renamed/copied marker, incomplete slot sets |
| `v018RejectsTopologyAttributesOwnershipSettingsAndDefaultDrift` | missing/extra/reversed/transitive/touching-grantor edge, bad options/grantor, unsafe flags, owner swap, role/db settings, PUBLIC/database/default/empty-default/extension/shared-dependency drift; snapshots unchanged |
| `pa25RequiresV018AndExact410OracleInBothModes` | V017 rejection, final18 apply/check success, corrupt each new tuple and representative old/newly monitored ACLs; exact rejection without ACL repair; wrong V018 checksum/metadata denied by PA25 and Flyway validate |
| `pa25UpgradesThreeSlotsToFourAndPreservesExistingCredentialBytes` | old3/4 graph→four7 at V018; LO creation marker; reapply/check exact, old OIDs/password/comments/edge grantors/ACLs/business rows unchanged; new LO PASSWORD NULL |
| `pa25RejectsInvalidOperationsInputAndCollidingProvenance` | absent/empty/duplicate/invalid/overlong/reserved names, owner/role/schema collision, pre-existing unmarked or malformed LO, copied marker; apply/check rollback and sanitized output |
| `pa25ApplyAndCheckEnforceExactSevenEdgeTopology` | apply repairs only absent approved edges; check rejects missing; both reject extra/bad options/duplicate grantors/non-superuser grantor/runtime-grantor/transitive and cross-slot edges; no inheritance |
| `operationsLoginSetsExactlyThreeRolesAndRestoresSessionContext` | ordinary LO TCP credentials; real transaction, fixed PID/session_user; sequential employee→IVR→evidence→employee→NONE; success/error rollback/read-only REPEATABLE_READ and pooled reuse assertions |
| `allOtherLoginAndDirectRoleBoundariesRemainExact` | all seven login identities and nine role attributes; LO no pre-SET rights/no forbidden SET; other identities cannot enter Ops/IVR; command-evidence and three direct boundaries exactly preserved |
| `pa25FailuresAreAtomicSanitizedAndNeverAdoptOrRestamp` | late failure rolls back LO/marker/edges together; check no writes; password/marker/identity canaries absent from output; captured SQL diagnostics fixed; no adoption/force path |
| `v018PreservesBusinessDefinitionsOwnersAndAllUnrelatedAcls` | seeded business rows, triggers/functions/types/constraints/indexes/owners/PUBLIC/defaults and all other ACLs unchanged; migration has no topology mutation; prior prefixes still validate |

For the fourth method explicitly deny current ungranted record columns including `consent_version`, `snapshot`, `snapshot_sha256`, `created_at`, `solidified_at`, `revision` when present; enumerate the actual catalog to avoid an incomplete denylist. These may be granted to customer under V017 but **not evidence_role**. Use savepoints after each expected `42501` so an aborted transaction is not mistaken for denial. Read-only transaction write attempts can return `25006`; prove ACL write denial separately in a read-write ordinary-role transaction so the ACL assertion specifically observes `42501`. Do not assert that IVR/Operations inherited writes have disappeared.

Fixtures use fresh random UUID passwords and non-production fixed role names (`pa36_query`, `pa36_customer`, `pa36_command`, `pa36_operations`), not the migration superuser as the read subject. Superuser is used only to migrate, create adversarial fixtures and inspect catalogs. Seed actual associations through owner-valid fixtures; empty table success is insufficient. Report actual PostgreSQL16 patch and image digest, without logging credentials.

### 7.3 Existing tests: preserve historical truth, update only current tails

- `PostgresContainerIT` stays its historical V001–V013 prefix. PA26 hardening stays V013→V014 with247 and existing semantics; its unused/current PA25 helper is not a reason to rewrite the historical suite.
- PA27 `current()` remains015 and its261 oracle remains; PA28 `current()` remains016 and269 remains. Their separately named latest/provisioning tails must explicitly migrate018, use current PA25's fourth input/new marker and project the expanded oracle to original-seven290. Update PA28's `currentHistory()` helper to inspect `latest()` rather than accidentally asking its historical016 Flyway object for latest history.
- Migration execution counts are contextual, not a global seventeen-to-eighteen replacement: PA27's latest helper after015 executes3 (016–018), PA28 latest after016 executes2 (017–018), PA28 latest directly after015 executes3, PA29 latest after017 executes1, and fresh latest executes18. Its already-applied history has18 rows. Preserve prefix-specific migration counts everywhere else.
- PA29 `current()` remains017, exact17/288, evidence16/customer53 and V017 checksum remain. Change its fresh-prefix `fixture.start(null)` to explicit017 where it means historical V017. Use its existing `frozenGraph()` machinery (or a narrowly named fixture helper), with synthetic password assignment, for historical app-pool probes at017; do not invoke current PA25 as if it accepted017. Preserve three-slot/four-edge historical topology tests.
- In PA29's current PA25 integration method, retain017/288 as a historical checkpoint and require current PA25 to reject it; then explicitly migrate018, provision/check current state and run current corruption checks. Keep history-corruption checks for ranks16/17 and add18 coverage through the new suite. Preserve existing method identities, even when an old method name mentions the historical PA25 count; comments/prose must distinguish its historical assertions and current tail. Never teach production PA25 an old-version bypass just to satisfy an old test.
- Historical boundary tests continue checking frozen V015/V016/V017 bodies and deltas. Only their explicitly current PA25 clauses change to410/18. PA29's projection subtracts the two new evidence tuples and excludes ops/ivr to recover288; keep its index16 V017 tuple assertion and add index17 V018 separately, never reinterpret V017 as V018.
- Latest-null shared fixtures migrate18. Activation/lifecycle/current Operations ACL tests that reflect PA29's `currentOracle()` must keep that historical oracle unchanged and compare latest data against an explicit +2 projection or the independently frozen new oracle. Total current history18; any explicit BETWEEN1AND17 assertion still checks17 predecessor rows, with a separate eighteenth-row assertion. Do not change a historical literal17 mechanically.
- PA25's existing20 method identities stay20: extend input matrices, names/slots/TARGETS/LOGINS/MANAGED/oracles/history and late-failure snapshots to LO; initialize seven independent synthetic passwords. Its six existing application-pool names remain six; LO is tested through the existing shared Operations datasource/test harness, not an invented seventh named PA25 pool bean. No production verifier/session fixture enters the product.

### 7.4 CI/POM/XML/token arithmetic

Add exactly `**/EvidenceRoleIvrAssociationAclIT.java` to container-smoke POM includes. No dependency/plugin/profile/toolchain change. CI gets literal `pa36_unit_additions` (8 names) and `pa36_container_additions` (16 names), with fully qualified class keys above. Merge into the existing source/XML identity maps, reject duplicate names and apply existing forbidden-test-style/zero-skip rules.

Preserve **all historical PA11/PA19…PA34 addition/start maps and totals**. For PA36-start reconstruction subtract only the new8/16 identities. Assert start704/504 and line1209; final totals:

```text
Surefire:  76 + 1 classes = 77; 704 + 8 methods = 712
Failsafe:  31 + 1 classes = 32; 504 +16 methods = 520
Combined: 107 + 2 classes =109; 1208+24 methods =1232
Annotation lines: 1209+24=1233 (same one historical comment)
Failsafe includes: 31+1=32, exact set equality with 32 container classes
PA25 existing methods:20 unchanged; new PA36 IT:16; new PA36 boundary:8
```

Source method lists = CI literal names = XML testcase identities; POM include set = container class set. Preserve existing per-class token/forbidden-output contracts, update only current PA25 success/count tokens, add new literal identities and exact expected assertion tokens. No broad regex acceptance, “at least” counts, filtered-out suite, duplicate XML, disabled or skip allowance. CI's full XML evidence must have tests1232, failures0, errors0, skipped0; retain both report directories and signed/count manifests in implementation evidence.

## 8. Validation commands and expected evidence

These are future implementation commands, **not executed by this planning task**. Run from repo root unless `cd backend` is shown. Use the pinned Temurin21.0.13+9 and Maven3.9.9 wrapper; verify Docker first. The planning environment has Docker CLI but no host psql; PA25 apply/check can run via container psql as existing tests do. No external documentation/network search was needed to establish repository facts.

```bash
java -version
cd backend
./mvnw -B -ntp -v
docker info
./mvnw -B -ntp -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests test
./mvnw -B -ntp -Pcontainer-smoke \
  -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests \
  -Dit.test=EvidenceRoleIvrAssociationAclIT,ProductionDatabaseIdentityProvisioningIT \
  clean verify
```

First focused run expects Surefire8 and Failsafe36 = new16 + PA25 existing20, all zero failures/errors/skips. Keep full class and method lists, not only BUILD SUCCESS. Run the compatibility focus next:

```bash
./mvnw -B -ntp -Pcontainer-smoke \
  -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests \
  -Dit.test=EvidenceRoleIvrAssociationAclIT,ProductionDatabaseIdentityProvisioningIT,DatabaseRuntimeAclRbacHardeningIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,CustomerEvidenceReadPrivilegeIT,ActivationCommandRoleLeastPrivilegeIT,OperationsLifecycleRoleLeastPrivilegeIT,OperationsActivationReadRoleLeastPrivilegeIT \
  clean verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

For the compatibility focus, calculate exact selected XML identities from the frozen CI class maps before running and require that exact union, no tests skipped. Full clean run expects **77/712 +32/520 =109/1232**, zero skipped/error/failure and all32 includes represented. Execute the updated CI XML/inventory/dependency/package scripts verbatim against that run's output, not a hand-simplified surrogate.

PA25 direct operator validation, after the protected normal migration runner has applied V018 and Flyway validate succeeds, is:

```bash
psql -X -w -v mode=apply \
  -v sharing_login="$PA25_SHARING_LOGIN" \
  -v customer_login="$PA25_CUSTOMER_LOGIN" \
  -v command_login="$PA25_COMMAND_LOGIN" \
  -v operations_login="$PA25_OPERATIONS_LOGIN" \
  -f backend/ops/postgresql/pa25-runtime-identities.sql
psql -X -w -v mode=check \
  -v sharing_login="$PA25_SHARING_LOGIN" \
  -v customer_login="$PA25_CUSTOMER_LOGIN" \
  -v command_login="$PA25_COMMAND_LOGIN" \
  -v operations_login="$PA25_OPERATIONS_LOGIN" \
  -f backend/ops/postgresql/pa25-runtime-identities.sql
```

Run these from repo root (leave `backend` first); protected external connection/service environment selects the intended cluster and actual administrator without putting a password on argv. Expected stdout is exactly the two new seven/nine/seven markers, exit0, after COMMIT. Reapply/check preserves snapshots. V017-only or any history/ACL/provenance/topology mutation yields the specified sanitized nonzero category, no success marker and no persistent mutation. No hosted live execution is implied by these commands.

Capture before/after raw and LF manifests for V001–V017 and frozen contracts/product tree. Also validate an **external `/tmp` LF projection** of the migration/contract inputs with exact expected checksum/history and raw frozen input comparison; never normalize the real worktree. Exercise fresh and017 upgrade plus focused replay in that projection. Reuse existing repository conventions; if Windows wrapper line endings require a launcher, an external temporary LF wrapper shim is permitted, not a wrapper edit. Do not reuse stale XML after `clean` or report earlier candidate results as current.

Packaging/dependencies: `V018` is the sole newly packaged production resource. PA25 SQL stays outside `src/main/resources` and out of the executable JAR; new tests/test helpers/fixtures/verifiers must not be packaged. Keep existing six-tuple dependency and JAR allowlists **unchanged**: baseline37 resolved/34 packaged; approved72/65, deltas35/31, dependency-only8, counterpart64 and packager1. Run existing adversarial firewall probes unchanged (currently116) and the packaged launcher/transport checks from CI. Extend the JAR resource assertion to require exactly one V018 entry, unchanged predecessor entries and no PA25 script. No new library, pool, environment secret, port, network/firewall rule, DNS/TLS exemption or PostgreSQL extension is needed. Existing PG/CI Docker access and external protected credential/TLS controls remain prerequisites.

Run existing contract regressions unchanged after `npm ci` in `api/openapi`: `npm run check:s3`, then the A2/A3/A4/A5 commands exactly as in `.github/workflows/ci.yml`. Classify already-recorded A2/A5 baseline drift against the baseline before calling a failure pre-existing; do not “fix” frozen contracts or reduce validators. New docs17 content must not invalidate A3. This task adds no npm/dependency change.

## 9. Residual risks, stop rules and handoff

| Risk | Owner / destination | Required treatment |
| --- | --- | --- |
| Existing IVR/Operations roles contain writes and sensitive cross-module rights | Database/Security; docs17 current boundary and production acceptance | Preserve bytes/ACLs; explicit SET is not a read-only privilege sandbox. LO is a trusted backend credential; enforce read-only transaction in its eventual consumer. Narrowing old roles is a separate goal, not PA36. |
| Full `identity_at_capture` JSON is already readable at raw evidence-role boundary | Security/Evidence Backend; docs17 and PA35 owner collaborator | No new JSON access; scalar-safe outputs/logging are application controls, not JSON-path ACLs. No PA35 payload contract invented here. |
| Three-edge LO does not supply PA33/PA34's wider owner roles | Controller/Operations Backend/Platform; PA35 resumption and deployment identity design | Do not route unrelated pools/endpoints through LO expecting universal functionality; any required union returns to goal confirmation. |
| Migration own-checksum circularity and post-migration tampering | Database/CI; Flyway validate + PA25 exact18 oracle | Exact prior17 in V018; exact self checksum in external verifier and pinned tests. No claim raw replay alone authenticates source bytes. |
| Cluster-global roles, other DB dependencies, concurrent administrators and marker custody | Platform/Database/Security; PA25 runbook Q17/Q23 and change process | Shared advisory lock is cooperative only; exclusive administration and protected comment/catalog writers remain external prerequisites. No adoption/force recovery. |
| TLS/pg_hba/secrets, authentication/rotation, pool drain and stale sessions | Platform/Security/Operations; production acceptance checklist | Seven real TCP/TLS/credential proofs, old-password denial on new connections and old-session policy remain NOT EXECUTED. PASSWORD NULL is not credential readiness. |
| `postgres:16` unpinned digest and hosted test headroom | CI/Platform; implementation evidence | Record actual patch/digest; unchanged30-minute job budget must be checked with full run. Do not silently repin dependencies or reduce tests; escalate exhausted budget. |
| LF/raw differences, Windows Git pointer and historical inventory drift | Controller/Backend CI; intake and implementation evidence | Preserve raw bytes and old assertions; validate LF separately; retain the reconciled c89e784 Control-only child vs merged5200355 base evidence; no repository plumbing mutation. |
| Existing A2/A5 validator drift | Protocol/Data/Backend contract maintainers; existing baseline destinations | Reproduce/classify; no unrelated fix in PA36. |

Minimality argument: exactly two object-ACL tuples; no business/schema/API/application edit; one additional external credential instead of contaminating existing identities; exactly three non-inheriting SET edges for the new consumer; unchanged existing120 privileges merely enter the verifier's coverage. One migration and one atomic verifier/topology slice prevent drift. Historical prefixes remain historical rather than weakening checks to make latest-chain tests pass.

Stop if implementation needs another column, broad SELECT, write grant, runtime/owner role alteration, role hierarchy, extra LO membership, new pool, PA35 code or public contract change. Route the concrete requirement to Controller for goal confirmation. Scope corrections for newly discovered current-only test assertions require a named path and unchanged inventory intent, not implicit permission.

**Controller handoff:** this plan is ready for intake and fresh peer-isolated A/B Planreview. Checkpoint lineage is reconciled above; there is no new design-blocking open question. No runtime validation has been run or claimed by the planning agent. Only this plan file is created. No source/test/migration/current documentation/Control/CI/POM edit, staging, commit, push, PR, review request, approval, merge, issue mutation or implementation has been performed. PA35 stays paused until PA36 independently passes its later gates and Controller explicitly resumes it.
