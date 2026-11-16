# PA36 evidence-role IVR association ACL — implementation plan

Task: `pa36-evidence-role-ivr-acl-plan-fix2-gpt-20261116-01` (cumulative plan; original task `pa36-evidence-role-ivr-acl-plan-gpt-20261116-01`)
Work unit: `PA36-evidence-role-ivr-association-acl`  
Date: 2026-11-16 (Asia/Shanghai)  
Status: planning artifact for Controller intake and independent A/B Planreview; **not implementation authorization or a passed gate**.

## 1. Authority, motivation and acceptance contract

The Active PA36 checkpoint in `docs/gateflow/project-execution-control.md`, including accepted `PA36-PLAN-REV-01` through `07` and `PA36-PLAN-REREVIEW-01` through `04`, is authoritative. Controller selects seven non-inheriting SET edges on the existing shared Operations read datasource; this supersedes the original three-edge proposal. The initial reviews (`docs/reviews/plan-review-20261116-061519.md` and `docs/reviews/plan-review-20261116-063117.md`) plus fix1 and re-reviews `docs/reviews/plan-review-20261116-080510.md` and `docs/reviews/plan-review-20261116-111954.md` are inputs, not implementation permission. Option A authorizes precisely two evidence-owner column grants and the identity/verifier/documentation/test lockstep needed by the paused PA35 detail association. Design authority remains `docs/12-technology-stack-and-system-architecture.md` §§6–8: owner-module query interfaces, one database with explicit ownership, independent employee authentication and no automatic employee playback authority. Docs14 defines the existing associations; docs17 defines the runtime boundary. PA36 does not revise those business contracts.

The V017 state permits `evidence_role` to select six `evidence.evidence_record` columns: `id`, `subject_id`, `status`, `source_type`, `source_ref_id`, `identity_at_capture`. It cannot select `session_id` or `call_id`. Inferring either association from `source_ref_id`, reading it through another privileged module, or broadening to table SELECT is not an acceptable workaround. The new capability must remain owned by evidence; PA35 later supplies its own owner collaborator and application authorization.

Success requires all of the following, not merely two successful GRANT statements:

1. Fresh PostgreSQL16 V001→V018 migration and V017→V018 upgrade validate; V001–V017 bytes/history remain unchanged.
2. Under an ordinary login and `SET LOCAL ROLE evidence_role`, all eight approved record columns are selectable; every other record column remains denied with SQLSTATE `42501`. No new table SELECT, writes, grant options, routines, ownership or business facts appear.
3. The dedicated shared Operations read login can SET exactly `operations_iam_role`, `subject_number_role`, `activation_consent_role`, `entitlement_metering_role`, `evidence_role`, `catalog_order_role`, `ivr_role`, cannot inherit their object privileges and restores login/role/transaction state on success and failure. Existing sharing/customer/command/direct-login boundaries remain intact.
4. Current PA25 apply/check validates exactly 18 SQL history rows, the expanded exact ACL oracle and the exact approved graph. Clean application, upgrade, replay and adversarial cases have zero-skip XML evidence. No drift is repaired by revoking or widening object ACLs.
5. Source identities, CI inventories, POM includes and XML agree exactly. Product Java/configuration and both OpenAPI files remain byte-identical; PA35 remains unimplemented.

Non-goals: PA35 routes/controllers/services/DTOs/cursors/collaborators; any API/schema/business-data change; new datasource or pool; new database runtime/owner role; role hierarchy; modifying existing role attributes; RLS/tenancy redesign; new evidence/media payload, URL, locator, crypto or playback exposure; unrelated hardening; credentials in Git; production connection/deployment acceptance. No broad table SELECT or new write is authorized. Existing Operations/IVR role writes are preserved, **not newly granted and not represented as read-only ACLs**.

One coherent S1 slice is required. V018, PA25, docs17/runbook and verification ship together. Splitting would leave either an unusable capability or an obsolete production verifier. Controller bookkeeping/reviews are subsequent gates, not an implementation sub-slice.

## 2. Inspected baseline and immutable facts

Read sources include the active checkpoint; architecture §§6–8; docs14 evidence/IVR identities and privilege rules; docs17 current and historical boundaries; frozen Operations IVR OpenAPI operations; V001–V017; PA25 SQL/runbook/accepted plan; current provisioning, ACL, boundary and Operations fixtures/tests; `backend/pom.xml`; `.github/workflows/ci.yml` including identity/XML/dependency/package firewalls. Important reconciliation facts:

- Control describes merged PA34 base `5200355695d39b6aeba446631c8f3f041b96e9ba`. Original planning HEAD `c89e7845f6b15b18fca2116bd3633b8e565b4aea` was its Control-only child. Fix1 entry HEAD is `ab5424fb218642fe2037647d0d49cf87b04fcc0f` (`gateflow: intake PA36 plan reviews`), after plan intake `3a0872e`. Read-only Git verifies the chain; base→entry changes exactly the cumulative plan, Control and the two review artifacts, not production/test inputs. Pre-existing untracked private `.codex` is not task output.
- The worktree `.git` contains a Windows `D:/...` pointer. In WSL, read-only Git works with `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"`. Do not rewrite `.git` or repository config to fix tooling.
- Fix2 entry HEAD is `17e1dd9ee1aaf99f36573596587be19136937441`; the preceding Fix1 lineage remains historical. Fix2 entry plan is 76,298 bytes /478 lines / SHA256 `cad87e0e558a0db04678cfcb0ce547f085d31fb082fbe0afcf4f1ddae046ca21`. The fix-2 artifact records the new plan digest and read-only validation; fix1 and all review/Control bytes remain untouched.
- Current PA25 `$frozen$` has **288** seven-role tuples, not its stale lower runbook paragraph's 261. Current history is **17**, not lower historical 13/15 statements. PA29's top sections supersede older prose but do not describe PA34's current test inventory.
- Current source/CI/POM baseline is **76 Surefire classes / 704 methods + 31 Failsafe classes / 504 methods = 107 / 1208**. Annotation-line scan is **1209**, with the single frozen `PostgresContainerIT` comment identified by CI. There are 31 exact Failsafe includes. PA29's 49/417 + 21/336 inventory remains historical.
- V014 established evidence16 = one schema USAGE + fifteen column SELECT tuples. V017 changed customer only: customer53 + evidence16 + other219 = 288. The new evidence role total is 18, not eight: eight is only its `evidence_record` column count.
- `OperationsReadContext` currently enters `operations_iam_role`, switches only its existing fixed enum roles and restores the employee role/login. It has **no IVR enum member**. PA36 does not add one. Existing PA33 subject-read fixtures deliberately grant six owner roles to an unmarked synthetic login; their six-role lists remain frozen; PA36 independently proves the Controller-selected seven-edge shared production topology.

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

The **only object-ACL mutation**, executed only when the exact clean evidence16 target state and migration guards are proven, is:

```sql
SET LOCAL ROLE evidence_owner;
GRANT SELECT (session_id, call_id)
    ON TABLE evidence.evidence_record TO evidence_role;
```

Restore the original migration session role with a safely quoted identifier in the same DO block; do not interpolate client input. A single owner-issued GRANT adds both tuples; no grant option. No REVOKE/reconciliation of incompatible states. Exact complete state replay issues no GRANT and changes nothing.

### 3.2 Exact ACL oracle and arithmetic

Use an independent evidence-only literal tuple oracle in V018 (16/18), and the independent expanded nine-monitored-role oracle in current PA25 (410). Tests freeze each expected boundary independently; expected values must not be obtained by parsing the SQL being tested. Tuple identity is `(grantee, kind, qualified object or routine signature, column-or-empty, privilege, grantor, is_grantable)`. Resolve qualified identities to OID/attnum/signature, reject unresolved/dropped/duplicate entries, and compare both set directions **and cardinality**.

The two new tuples are exactly:

```text
evidence_role|column|evidence.evidence_record|session_id|SELECT|evidence_owner|false
evidence_role|column|evidence.evidence_record|call_id|SELECT|evidence_owner|false
```

The original seven-role oracle is **288→290 = customer53 + evidence18 + others219**; the evidence-only target is **16→18**, with the six existing record columns unchanged. The expanded nine-role oracle includes two already-existing role ACL inventories: **operations_iam_role14 + ivr_role106 = 120 unchanged tuples**. Therefore expanded pre/post counts are **408→410**, a PA25/test accounting boundary, **not a V018 acceptance predicate**. This 120-tuple expansion is verifier coverage, **not 120 new grants**. Projecting post-state back to the original seven roles must produce exactly 290; subtracting only the two new tuples must recover exactly 288.

Association types are already fixed by V008/docs14: `session_id uuid` references `ivr.inbound_session(id)` and `call_id text` references canonical `ivr.call_record(call_id)`. They are not interchangeable public UUIDs and neither is derived from `source_ref_id`. Positive fixtures cover null associations, call-only association and valid session+call association with matching immutable source/snapshot facts and existing FK parents. No backfill, index, FK or uniqueness change is needed.

Freeze the two newly monitored role inventories in **PA25 and its independent tests only**, from their original owner-issued grants, all non-grantable:

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
| exact V001–V017 | exact evidence16 | none, or complete historical three slots/four edges | grant both; evidence18; non-target snapshots equal |
| exact V001–V017 | exact evidence18 | none, or complete historical three slots/four edges | no-op raw replay before Flyway records V018 |
| exact V001–V018 metadata | exact evidence18 | none, historical three/four, or complete new four/eleven | no-op replay |
| any other combination | any | any | reject without persistent mutation |

In particular: old ACLs with an applied V018 row, partial one-column additions, four-slot topology before V018, malformed/partial marker sets and histories beyond V018 reject. V018 recognizes exact PA25 marker sets of zero, three old slots or four new slots only; three means all old slots and four approved old edges, four means all slots and eleven edges (the old four plus exactly seven LO edges). Discover actual names from validated creation provenance, never guessed login names. Verify UTF-8 marker equality, current database OID, role OID, slot and role name; reject any other `onecall:pa25%` marker. Partial provisioning must be completed by the compatible PA25 version before retrying a migration, never adopted by V018.

During execution V018 cannot embed its own eventual checksum without a circular file-content dependency. Its optional self history row must have exact rank/version/type/success/script/description and a non-null checksum; **all predecessor checksums are exact**. Flyway validate and current PA25 subsequently enforce the exact V018 checksum. Do not claim raw DO replay alone authenticates its own file checksum. Wrong V018 checksum must fail both external validation paths; predecessor checksum drift must also fail V018 itself.

After V018 bytes settle, obtain its signed checksum from pinned Flyway's pending migration info and cross-check the repository's existing UTF-8 line-by-line CRC32 convention. Record that concrete integer in PA25's eighteenth history tuple and boundary tests; record raw SHA256 and LF SHA256 in docs17/runbook/implementation evidence. Recompute after any V018 edit. There is no invented checksum in this plan and no runtime wildcard/zero/learn-from-database checksum oracle.

Negative guards retain V017 mechanisms over the evidence target, all touching PUBLIC/grantor-side ACLs and validated external topology; they must not embed the unrelated Operations/IVR 120-tuple oracle. Guard scope includes the existing roles touched by the seven LO edges and four external slots: authority, provenance, exact history, settings, role attributes, touching memberships (including grantor), schema/relation/routine/type ownership, explicit and effective ACLs, PUBLIC, database CONNECT-only boundary, default ACLs (including empty runtime-owned rows), shared and extension dependencies. Reject wrong-grantor and grant-option tuples even if effective SELECT succeeds. Compare target schema/table/column/sequence/routine ACLs in both directions, including table-to-column implications, PUBLIC and grantor-side escape paths. All non-target ACLs/owners/definitions are captured before and after and must be identical, without an absolute 408/410 precondition. Unsafe PUBLIC/default/ownership/topology/attribute drift still fails the relevant guard. A different otherwise-safe non-target IVR EXECUTE inventory is preserved by V018, not adopted into its target oracle; current PA25 must reject it against exact410. An extra evidence_role tuple, wrong grantor, grant option or broad evidence SELECT always fails V018. This separates preservation from the cross-module current-state oracle rather than weakening either boundary.

Use fixed categories `V018_AUTHORITY_REJECTED`, `PROVENANCE`, `HISTORY`, `SETTINGS`, `ATTRIBUTES`, `TOPOLOGY`, `OWNERSHIP`, `ACL`, `DATABASE`, `DEFAULT_ACL`, `DEPENDENCY`, `PRESERVATION`, `RECONCILIATION` with the common `V018_…_REJECTED` spelling. Capture before/after invariants for roles/password verifiers/comments/memberships/settings/defaults/databases/owners and all non-target ACLs; assert exact target delta separately. Tests additionally snapshot business rows, definitions, constraints, indexes and triggers. The exception envelope sanitizes all failures and the DO/Flyway transaction rolls the entire delta back. Do not catch-and-continue or commit half a grant.

## 4. Minimum production identity topology

### 4.1 Controller decision and exact final graph

Add one dedicated externally named login LO, PA25 slot `operations_ivr_read`, input `operations_login`. It is the credential for the **existing** `operationsSubjectReadDataSource`, not a new pool. Controller-selected reach is the six merged PA33/PA34 roles plus IVR. LQ/LC/LW and the three direct identities remain unchanged. No new migration-defined runtime/owner role or hierarchy is authorized.

```text
LQ -> sharing_role
LC -> customer_iam_role
LW -> sharing_command_role
LW -> evidence_role
LO -> operations_iam_role
LO -> subject_number_role
LO -> activation_consent_role
LO -> entitlement_metering_role
LO -> evidence_role
LO -> catalog_order_role
LO -> ivr_role
```

Exactly **11** edges, each one catalog row with `ADMIN FALSE, INHERIT FALSE, SET TRUE`; grantor is an actual unmanaged superuser, not one hardcoded name. Preserve compatible existing grantors. No runtime-to-runtime, reverse, transitive, grantor-side or other touching edge. Direct logins `sharing_public_read_role`, `sharing_admission_role`, `evidence_media_reader_role` retain their exact flags/expiry/ACLs and zero touching edges. LO must have exactly seven SET targets, never an eighth.

The required PA25 success metric is **7 logins / 9 effective_roles / 11 memberships**. Define the metric honestly: `effective_roles=9` counts the **nine exact-ACL-monitored roles** (original seven plus Operations/IVR), not every distinct target in the graph. The four existing shared-context targets `subject_number_role`, `activation_consent_role`, `entitlement_metering_role`, `catalog_order_role` form a separate `shared_context_targets` validation set; they do not expand the 410-tuple oracle. Across all identities there are **13 distinct capability-role identities** (nine monitored + four context targets), **13 PA25 managed names** (four external + nine monitored) and **17 distinct boundary names** when those four targets are included. Do not misreport nine as total reachable roles or add those four ACL inventories to 410. Fresh migrations create31 project roles; PA25 creates four external logins for35, versus historical34; the four context targets already exist. Three direct roles count in both login and monitored sets, hence 7+13-3=17 boundary names.

LO and the other external logins are LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS, connlimit -1, no role/database settings, no expiry, no object privileges/ownership/default ACLs. Creation uses PASSWORD NULL and creation-only PA25 v1 marker with the new slot; actual credential assignment/rotation is external. Preserve old OIDs/password verifiers/comments/edges. `operations_iam_role` (V001) and `ivr_role` (V004) have migration-default `rolinherit=true`, NOLOGIN; preserve this. The four context targets also retain their V001/V004 NOLOGIN/default-INHERIT attributes. Login NOINHERIT **and** edge inherit_option=false prevent automatic inheritance; no base-role ALTER is permitted.

“No role DDL” applies to V018 and migration-defined roles. PA25's authorized external-login CREATE/compatible ALTER/creation COMMENT/GRANT mechanism provisions LO only; no application role DDL or new capability role.

### 4.2 Real shared-pool proof and limits

Use LO's real ordinary TCP credentials in a test-only instance of the **existing** Operations datasource configuration/transaction manager and unmodified `OperationsReadContext`. Bind the existing property family `onecall.operations-subject-read.datasource.url|username|password`; never add a pool/property family or IVR production enum. In a read-only REQUIRES_NEW/REPEATABLE_READ transaction call `enter()`, then each of `Role.SUBJECT`, `ACTIVATION`, `TRIAL`, `EVIDENCE`, `ORDER` via `scoped()`, checking restoration to `operations_iam_role` in each finally path. Within that same physical connection/transaction use fixed test-only `SET LOCAL ROLE ivr_role`, check, restore employee, and `exit()` to LO. `session_user`, backend PID, transaction flags and pooled connection identity must stay fixed. Exercise successful, throwing, cleanup-failure, rollback/completion-failure and pool-reuse paths. This proves all six merged roles plus IVR work through the real single-username contract without PA35 implementation. Do not change existing synthetic fixtures' six-role lists or claim production has an IVR enum.

LO has no object access before SET and cannot SET any eighth role: enumerate **all pg_roles**, excluding only its own identity and seven allowed targets, and require denial including owners, all other effective roles, predefined roles and other logins. No implicit inheritance, membership delegation or session authorization. LQ/LC/LW/direct logins cannot SET Operations/IVR/LO; LW keeps evidence. Test schema USAGE independently from column ACLs. The shared read-only transaction is an application safety control, not removal of existing Operations/IVR/context-role writes; preserve those ACLs. Provisioning this union is Controller-selected and no longer an open goal-confirmation question.

## 5. PA25, runbook and docs17 lockstep

In `backend/ops/postgresql/pa25-runtime-identities.sql`, make every following anchored change together (entry line numbers are navigation aids, not post-edit coordinates):

| Input / predicate / inventory anchor | Exact current-state change |
| --- | --- |
| psql defaults and transaction-local settings (12–28) | Add `operations_login` empty-default initialization and `pa25.operations_login`; mode remains exactly apply/check, four required nonempty names, no three-input success |
| `external_names` (34–35), `slots` (36) | Append fourth setting and `operations_ivr_read`; old indices/slots remain 1 sharing_query, 2 customer_context, 3 sharing_command; new index4 LO |
| `effective_names` (37–38), `managed_names` (2637) | Append operations_iam_role, ivr_role, yielding nine monitored roles / thirteen managed names; separate four `shared_context_targets` and seventeen-name boundary union as §4.1 |
| four distinct names (2612) | `count(DISTINCT name COLLATE "C") <> 3` → `<> 4`; preserve length/regex/reserved/owner/schema/session exclusions and add all four context targets to role-name collision exclusions |
| `login_names` (2638) | Replace positional `effective_names[5:7]` with explicit three direct names; four external + three direct =7 |
| provenance and mutation loops (2643, 2926) | Both `FOR slot_index IN 1..3` → `1..4`; marker template v1 unchanged; creation-only COMMENT, no adoption/restamp |
| `$history$`, row count (2669) | Append exact rank18/version018/SQL/success/script/description/checksum; `<>17` → `<>18`; retain exact seventeen prior tuples, no wildcard/repair/bypass |
| `expected_inherit` (2690) | True set becomes exactly sharing_role, customer_iam_role, evidence_role, **operations_iam_role, ivr_role**. Other original monitored roles and four external logins remain false; separate context-target validation expects their true migration defaults, NOLOGIN and unchanged safe flags/settings/ownership. Never ALTER either new monitored role to NOINHERIT |
| touching boundary / allowed-pair predicate (2704–2714) | Preserve old four pairs; add all seven `(target, external_names[4])` pairs in §4.1, including four existing context targets. Validate exact union17 for touching role/member/grantor and reject duplicate rows, unmanaged/non-superuser grantor, reverse/transitive/extra edges or ADMIN/INHERIT/SET drift |
| edge count (2723) and apply VALUES (2939–2940) | `edge_count <>4` → `<>11`; same explicit eleven pairs in validation and mutation; apply adds only absent approved pairs, check repairs nothing |
| `$frozen$`, resolved length (2762) | 288→410 = existing288 + evidence2 + Operations14 + IVR106; exact seven-role projection290 and subtract delta→288 in tests; no context-target ACLs added to this oracle |
| ACL/dependency/default/settings boundaries (2730–2920) | Keep exact410 comparison on monitored roles; use boundary17 for topology/attribute/settings/no external direct ACL/ownership/default/shared-dependency protections without incorrectly forbidding existing context-target object grants. Preserve twelve-owner and43-routine maps unchanged: the Operations guard and four IVR guards already occur in the latter; reuse their frozen signatures/security/search_path, never learn expected definitions from candidate |
| post-COMMIT output (2956, 2958) | Exactly `PA25_APPLY_OK logins=7 effective_roles=9 memberships=11` or `PA25_CHECK_OK logins=7 effective_roles=9 memberships=11`; no names/markers/secrets |

Both modes reject missing/empty/duplicate/malformed/reserved/colliding fourth inputs, including literal `ivr_role`, `operations_iam_role` and every context-target name. Test valid migration-default true-INHERIT Operations/IVR roles in both modes. Before mutation reject bad attributes/markers, unsafe touching edges/grantors, explicit/effective ACL drift, ownership/default/settings/dependency drift. Preserve password verifiers/validity/OIDs/settings/markers and all old edges. Existing three marked logins plus absent LO is the upgrade; unmarked colliding LO is provenance rejection. Shared advisory lock and one transaction cover both passes. Failure categories/redaction unchanged; check never repairs. All test SQL runners, subprocess variable lists, fixture copies, expected output tokens, input matrices and CI per-class required tokens must use four inputs and 11 memberships only for current state.

Prepend `PA36 Current Runtime Boundary (V018)` to docs17, PA25 runbook **and `backend/README.md`**. The new sections supersede every undated current PA25 input/graph statement, including README's three-input/six-seven-four paragraph, by explicitly labeling the retained body historical/non-current. Preserve historical bodies verbatim, including PA29/28/27/26/A3 evidence, commands and old counts; no false undated current claim remains unscoped. Current sections state two-tuple delta, eight evidence_record columns/evidence18, PA25-only408→410/288→290 and unchanged120, V018 target-only16→18 and preservation boundary, exact18 history/checksum/raw+LF digests, seven/nine/eleven metric definition plus context-target distinction, seven LO targets, no inheritance, marker custody, no PA35 code, final test inventories.

**Read amplification is role-wide, not Operations-only.** Every holder/assumer of evidence_role receives both association columns, notably LW's existing `sharing_command_role`→`evidence_role` customer sharing command path (`DefaultCustomerShareCommandService`, `JooqSharingCommandRepository`, evidence `ShareableEvidenceProjectionReader`), and the existing Operations evidence owner reader `core/evidence/internal/operationsread/JooqOperationsEvidenceCounter.java` through `OperationsReadContext.Role.EVIDENCE`. Inventory direct role/owner-maintenance consumers and any externally authorized SET holder as the same trusted boundary. Every production type named in the docs17 amplification inventory must resolve to an actual declared type under `backend/src/main/java`; record its verified path, not an invented reader name. Customer evidence list reads running as customer_iam_role retain their V017 grants; public/admission/media-reader identities do not gain evidence-role membership or raw reads. This amplifies raw database reach but does not add DTO fields, projection content, JSON paths, media locators, payloads or authority; existing six columns and full identity_at_capture JSON were already readable. Docs17 must explicitly name this reach/residual and the separate unchanged public/customer contracts. Targeted new IT assertions must prove LW and LO each can read the two seeded association scalars after SET, other identities retain their own allow/deny matrix, and existing sharing/customer response/log/payload exclusion tests remain unchanged and pass. Do not print association/payload/credential values; assert equality/denial with sanitized diagnostics.

The runbook provides complete current four-variable commands (not a mix with old historical commands) and private `PA25_OPERATIONS_LOGIN` mapping. Seven credentials/four mappings remain external; use `-X -w`, protected service/connection selection, no argv passwords and sanitized pre-script errors. Wire LO to existing server-managed `onecall.operations-subject-read.datasource.url|username|password`, serving merged PA33/PA34 plus future fixed IVR switching, no default identity/client selector/pool rename. Apply/validate V018 before current PA25; stop rollout on mismatch. Actual PG16 patch/digest, exclusive admin/migrator/comment custody, TLS/pg_hba, credential assignment, seven real TCP proofs, rotation/old-session drain/recovery ownership remain NOT EXECUTED until externally proved. Repository tests do not certify production readiness.

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
| 7 | `container/ProductionDatabaseIdentityProvisioningIT.java` | EXISTING; retain twenty method identities; expand graph/oracle/history/credentials/input/negative assertions; boundary13 pair loop91 = allowed14 + rejected77 and ten NOLOGIN password probes (§7.3) |
| 8 | `container/CustomerBusinessNumbersFixture.java` | EXISTING; latest18, fourth PA25 variable and optional explicit historical provision helper; no application behavior |
| 9 | `container/CustomerBusinessNumberReadPrivilegeIT.java` | EXISTING; historical V015 assertions unchanged; latest PA25/current tail18/290/new marker |
| 10 | `container/CustomerSubjectDetailReadPrivilegeIT.java` | EXISTING; historical V016 assertions unchanged; latest history18 / seven-role ACL projection290 |
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
| 29 | `container/OperationsActivationReadsIT.java` | EXISTING; history18 plus separately scoped PA34 frozen manifest/docs17 current digest tail; §7.3 |
| 30 | `container/OperationsSubjectReadRoleLeastPrivilegeIT.java` | EXISTING; latest successful history count17→18 only |
| 31 | `container/OperationsSubjectReadsIT.java` | EXISTING; latest successful history count17→18 only |
| 32 | `container/PublicShareAdmissionIT.java` | EXISTING; latest applied history size17→18 only |
| 33 | `container/SharingAdmissionRoleLeastPrivilegeIT.java` | EXISTING; latest applied history size17→18 only |
| 34 | `container/SharingQueryServiceIT.java` | EXISTING; latest applied history size17→18 only |
| 35 | `container/SubjectClaimRoleLeastPrivilegeIT.java` | EXISTING; latest applied history size17→18 only |
| 36 | `container/PostgresContainerIT.java` | EXISTING; current classpath inventory18 / append V018, retain target013/applied13 and historical semantics |
| 37 | `container/DatabaseRuntimeAclRbacHardeningIT.java` | EXISTING; two actual current PA25 tails migrate014→018 (4), applied18/four inputs/11-edge marker; third current tail `assertFrozen()` unfiltered directory17→18; retain V001–V013 LF pins, target013/applied13 prefix and historical014/oracle28 (customer12+evidence16), V014 old81/final28 |
| 38 | `architecture/CustomerSubjectActivationBoundaryTests.java` | EXISTING; frozen20 (three contracts +17 migrations) retained as prefix; exact current migration18 tail |
| 39 | `architecture/CustomerSubjectClaimBoundaryTests.java` | EXISTING; frozen17 migration hashes retained, current migration18 set/count tail |
| 40 | `architecture/OperationsActivationReadBoundaryTests.java` | EXISTING; PA34 frozen21 retained; current18 inventory + docs17 new raw/LF pins scoped separately |
| 41 | `architecture/OperationsLifecycleBoundaryTests.java` | EXISTING; frozen migration17/contract pins retained; current18 inventory + docs17 new raw/LF pins |
| 42 | `architecture/OperationsSubjectReadBoundaryTests.java` | EXISTING; historical hash rows retained; exact current range001–018 inventory |
| 43 | `backend/README.md` | EXISTING; prepend current PA36 boundary and scope old undated PA25 prose historical |

Implementation permission arithmetic: **43 paths = 3 new + 40 existing**, including two new test classes and one new migration; **36 test/helper paths = 2 new tests +34 existing tests/helpers**, with seven non-test implementation paths. Pure latest-history-only updates are exactly **15** paths: rows15–18 and24–35 excluding29 (4+12−1); row29 also owns docs17/manifest scoping. Row36 is a separate current classpath-inventory-only change. The five newly listed architecture paths own only inventory/hash scope, not application behavior. No existing method identity is added/deleted/renamed. This plan artifact is a separate planning deliverable, not a fourth new S1 implementation file. Controller separately authorizes its implementation-evidence artifact and any Control/review records; do not silently add them to the implementer's source write set.

Forbidden edits include V001–V017, docs12/docs14/docs15, OpenAPI, protocol/security JSON, all production Java/configuration, historical assertions/bodies in `PostgresContainerIT` and `DatabaseRuntimeAclRbacHardeningIT` (their enumerated current tails are expressly allowed, including `assertFrozen()`'s unfiltered directory count17→18; preserve its V001–V013 LF pins and historical target013/applied13 semantics), existing Operations fixtures' role sets, wrapper/toolchain/dependencies, prior reviews/plans and Control. If a newly found current-state assertion requires another file, report the exact path/reason for Controller scope correction, not a broad repository replacement.

## 7. Tests, historical compatibility and inventory

### 7.1 Eight new ordinary Surefire identities

Class `com.onecall.archtests.architecture.EvidenceRoleIvrAssociationAclBoundaryTests` has exactly these eight non-parameterized, non-dynamic `@Test void` methods:

| Method | Required assertion |
| --- | --- |
| `v018NamesOnlyTheTwoOwnerIssuedAssociationColumns` | literal two-tuple delta and V018 evidence16/18 only; no all-role408/410 predicate or other mutation |
| `v018GuardsAuthorityHistoryProvenanceAndExactReplayStates` | fixed guard categories, exact17 prefix, permitted replay matrix, same advisory lock |
| `v001ThroughV017AndPublicContractsRemainByteFrozen` | independent raw/LF manifest; both OpenAPI files, docs12/docs14 and all product Java/config frozen against entry manifest |
| `pa25UsesExactEighteenHistoryAndIndependent410TupleOracle` | independent literal nine-role oracle, 290 projection, concrete V018 checksum and unchanged17 prefix |
| `pa25DefinesOnlyTheElevenApprovedNonInheritingEdges` | four inputs/slots, eleven pairs, seven LO targets, nine monitored roles plus four context targets, direct-login separation, actual true-INHERIT defaults, creation-only marker |
| `currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory` | docs17/runbook/README current counts/limits/checksum and role-wide amplification; require verified §5 consumer names/paths including `JooqOperationsEvidenceCounter`, and resolve every named production type under `backend/src/main/java`; historical document bodies remain byte-identical |
| `pa36HasNoApplicationApiPoolOrDependencyExpansion` | source/path allowlist; no PA35 Java/OpenAPI/config/dependency/new pool changes; V018 only packaged delta |
| `ciPomAndOrdinaryMethodInventoryMatchExactly` | literal names in this section, one include, no parameterized/dynamic/disabled/skipped identities or duplicate token |

Use independent frozen manifests/literals in the test code; never compute an expected baseline from the candidate at test execution. A hash test alone does not prove semantic privilege safety; the ITs do that.

### 7.2 Sixteen new ordinary Failsafe identities

Class `com.onecall.archtests.container.EvidenceRoleIvrAssociationAclIT`, fresh synthetic PG16 fixtures, has exactly these sixteen ordinary methods. Mutations inside one method are explicit loops with savepoints/snapshot comparison, not new parameterized XML identities.

| Method | Required assertion |
| --- | --- |
| `v017DeniesBothAssociationColumnsWith42501` | historical V017288/expanded408 observed in test only; evidence16 oracle and each missing column denied under evidence_role |
| `freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors` | fresh18, upgrade exactly1, validate, full18 metadata/checksum; historical rows byte/semantic-identical |
| `v018AddsExactlyTwoOwnerIssuedTuplesAndMatches410Oracle` | both set differences, no removals, original seven290, customer53/evidence18/others219, Operations14/IVR106 |
| `evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn` | seeded valid nonempty evidence association values; select all8 plus individual positives; enumerate all remaining record columns and deny each, SELECT *, writes, locks, schema CREATE, routines/sequences/grant delegation; retain artifact/media sensitive denials |
| `v018ReplaysOnlyExactCleanOrCompleteStatesWithoutDrift` | raw before-history replay final17 and applied final18, none/old3/new4 marker cases as permitted; no duplicate ACLs or changed snapshots; migrate-again0 |
| `v018RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically` | either one-column pregrant; either missing final column; ungranted payload column; broad table SELECT; extra evidence-target ACL; substitute grantor; either grant option; no partial repair |
| `v018RejectsAuthorityHistoryAndProvenanceDriftAtomically` | non-PG16 guard where feasible/static pin otherwise; non-superuser/current-role mismatch; every predecessor history field/rank deletion plus extras/duplicates; fabricated applied row with old ACL; malformed/wrong database/role/slot/renamed/copied marker, incomplete slot sets |
| `v018RejectsTopologyAttributesOwnershipSettingsAndDefaultDrift` | missing/extra/reversed/transitive/touching-grantor edge, bad options/grantor, unsafe flags, owner swap, role/db settings, PUBLIC/database/default/empty-default/extension/shared-dependency drift; snapshots unchanged |
| `pa25RequiresV018AndExact410OracleInBothModes` | V017 rejection, final18 apply/check success, corrupt each new tuple and representative old/newly monitored ACLs; exact rejection without ACL repair; wrong V018 checksum/metadata denied by PA25 and Flyway validate |
| `pa25UpgradesThreeSlotsToFourAndPreservesExistingCredentialBytes` | old3/4 graph→four11 at V018; LO creation marker; reapply/check exact, old OIDs/password/comments/edge grantors/ACLs/business rows unchanged; new LO PASSWORD NULL |
| `pa25RejectsInvalidOperationsInputAndCollidingProvenance` | absent/empty/duplicate/invalid/overlong/reserved names, owner/role/schema collision, pre-existing unmarked or malformed LO, copied marker; apply/check rollback and sanitized output |
| `pa25ApplyAndCheckEnforceExactElevenEdgeTopology` | apply repairs only absent approved edges; check rejects missing; both reject extra/bad options/duplicate grantors/non-superuser grantor/runtime-grantor/transitive and cross-slot edges; no inheritance |
| `operationsLoginSetsExactlySevenRolesAndRestoresSessionContext` | real existing datasource/context enter + all five scoped enum roles, fixed test-only IVR SET/restore, exit; ordinary LO TCP, same PID/login/transaction, exceptions/rollback/completion failure/pool reuse, no eighth SET target |
| `allOtherLoginAndDirectRoleBoundariesRemainExact` | all seven login identities, nine monitored plus four context-target attributes; LO no pre-SET rights/no eighth SET; other identities cannot enter Ops/IVR; LW and LO can each read association scalars as evidence_role, existing customer/sharing contracts and direct boundaries unchanged, no output payloads |
| `pa25FailuresAreAtomicSanitizedAndNeverAdoptOrRestamp` | late failure rolls back LO/marker/edges together; check no writes; password/marker/identity canaries absent from output; captured SQL diagnostics fixed; no adoption/force path |
| `v018PreservesBusinessDefinitionsOwnersAndAllUnrelatedAcls` | seeded business rows/definitions/owners/PUBLIC/defaults/non-target ACLs equal before/after; safe non-target IVR EXECUTE variation preserved and V018 succeeds while PA25 rejects exact410 drift; extra evidence tuple rejects; no topology mutation |

For the fourth method explicitly deny current ungranted record columns including `consent_version`, `snapshot`, `snapshot_sha256`, `created_at`, `solidified_at`, `revision` when present; enumerate the actual catalog to avoid an incomplete denylist. These may be granted to customer under V017 but **not evidence_role**. Use savepoints after each expected `42501` so an aborted transaction is not mistaken for denial. Read-only transaction write attempts can return `25006`; prove ACL write denial separately in a read-write ordinary-role transaction so the ACL assertion specifically observes `42501`. Do not assert that IVR/Operations inherited writes have disappeared.

Fixtures use fresh random UUID passwords and non-production fixed role names (`pa36_query`, `pa36_customer`, `pa36_command`, `pa36_operations`), not the migration superuser as the read subject. Superuser is used only to migrate, create adversarial fixtures and inspect catalogs. Seed actual associations through owner-valid fixtures; empty table success is insufficient. Report actual PostgreSQL16 patch and image digest, without logging credentials.

### 7.3 Existing tests: preserve historical truth, update only current tails

- `PostgresContainerIT`: keep target013, thirteen applied rows and all historical behavior; `info().all()` is the **current classpath**, not applied prefix. Change line1396 size17→18 and line10800 script list by appending V018; keep applied13 separate. Never globally change historical13.
- `DatabaseRuntimeAclRbacHardeningIT`: keep target014, V013→V014 executed1, fresh014 executed/applied14, independent V014 oracle28 = customer12 + evidence16, V014 `$old$`81/`$final$`28 and all hardening/replay/negative semantics. The current PA25 helper is actually called at lines131/331. In those two explicit latest tails, migrate014→018 executes4 (was3), applied history18 (was17); supply fourth input, require seven/nine/eleven marker and complete current graph. The 247/261 oracles belong to `CustomerBusinessNumberReadPrivilegeIT`, not this class. The third current tail is `assertFrozen()` (around603): retain its unfiltered SQL-file enumeration and change `hasSize(17)` to exact `hasSize(18)`; preserve all thirteen V001–V013 LF hash literals and historical target013/applied13 semantics. The new PA36 boundary test independently pins V018 and the exact old17 paths union sole V018; do not add V018 to the historical V001–V013 `FROZEN` map. Both live callers `v014FreshChainHasExactHistoryAndImmutablePredecessors` and `v014IsPrivilegeOnlyAndLeavesUnrelatedBoundariesEquivalent` must remain green with V018 present. No historical fixture bypass or claim the helper is unused. The twenty existing PA25 methods elsewhere stay unchanged in identity.
- PA27 `current()` remains015 and its261 oracle remains; PA28 `current()` remains016 and269 remains. Their separately named latest/provisioning tails must explicitly migrate018, use current PA25's fourth input/new marker and project the expanded oracle to original-seven290. Update PA28's `currentHistory()` helper to inspect `latest()` rather than accidentally asking its historical016 Flyway object for latest history.
- Migration execution counts are contextual, not a global seventeen-to-eighteen replacement: PA27's latest helper after015 executes3 (016–018), PA28 latest after016 executes2 (017–018), PA28 latest directly after015 executes3, PA29 latest after017 executes1, and fresh latest executes18. Its already-applied history has18 rows. Preserve prefix-specific migration counts everywhere else.
- PA29 `current()` remains017, exact17/288, evidence16/customer53 and V017 checksum remain. Change its fresh-prefix `fixture.start(null)` to explicit017 where it means historical V017. Use its existing `frozenGraph()` machinery (or a narrowly named fixture helper), with synthetic password assignment, for historical app-pool probes at017; do not invoke current PA25 as if it accepted017. Preserve three-slot/four-edge historical topology tests.
- In PA29's current PA25 integration method, retain017/288 as a historical checkpoint and require current PA25 to reject it; then explicitly migrate018, provision/check current state and run current corruption checks. Keep history-corruption checks for ranks16/17 and add18 coverage through the new suite. Preserve existing method identities, even when an old method name mentions the historical PA25 count; comments/prose must distinguish its historical assertions and current tail. Never teach production PA25 an old-version bypass just to satisfy an old test.
- Historical boundary tests continue checking frozen V015/V016/V017 bodies and deltas. Only their explicitly current PA25 clauses change to410/18. PA29's projection subtracts the two new evidence tuples and excludes ops/ivr to recover288; keep its index16 V017 tuple assertion and add index17 V018 separately, never reinterpret V017 as V018.
- Latest-null shared fixtures migrate18. Activation/lifecycle/current Operations ACL tests that reflect PA29's `currentOracle()` must keep that historical oracle unchanged and compare latest data against an explicit +2 projection or the independently frozen new oracle. Total current history18; any explicit BETWEEN1AND17 assertion still checks17 predecessor rows, with a separate eighteenth-row assertion. Do not change a historical literal17 mechanically.
- PA25's existing20 method identities stay20: extend input matrices, names/slots/TARGETS/LOGINS/MANAGED/oracles/history and late-failure snapshots to LO; initialize seven independent synthetic passwords. Its six existing application-pool names remain six; restrict positional loops to the six legacy identities rather than indexing seven LOGINS against six pools. LO is tested through the separate test instance of the **existing** shared Operations datasource configuration, not a newly invented named pool. Extend safe-attribute expectations for Operations/IVR true INHERIT and the separate context targets; don't classify LO as an ACL-write-denied identity after SET to roles with preserved writes. No production verifier/session fixture enters the product.

- Controller selects full **capability boundary13** for `ProductionDatabaseIdentityProvisioningIT.deniesEveryUnapprovedRoleTargetIncludingOwnersAndPredefinedRoles`: replace the inner `EFFECTIVE` iteration with exact nine monitored roles union four `shared_context_targets`, not full boundary17 (which also includes external logins). Keep `EFFECTIVE`9, `MANAGED`13 and PA25 ACL410 unchanged. LOGINS7 × boundary13 =91 counted pairs; `allowed`7→14 = three direct self-pairs + four old approved edges + seven LO edges; `rejected`35→77 =91−14. Count once per login/target pair after testing both `SET ROLE` and `SET LOCAL ROLE` (182 command forms); retain the separate owner/predefined/other-login denial loop outside these counters. Assert exact14/77 and `allowed + rejected == 91`; do not retain the alternative monitored-only10/53. The graph's project-role catalog count35 =31 migration roles +4 external logins is distinct from the old rejected-pair literal35 and from suite/test counts (`suite55` is not this graph oracle): PA25 remains20 methods, the new suites8+16, full109/1232.
- In `unsafeAttributesAndLoginShapeMismatchesFailClosed`, replace positional `EFFECTIVE.subList(0, 4)` password-negative targets with exact boundary13 minus `DIRECT` = ten NOLOGIN roles: `sharing_role`, `customer_iam_role`, `sharing_command_role`, `evidence_role`, `operations_iam_role`, `ivr_role`, `subject_number_role`, `activation_consent_role`, `entitlement_metering_role`, `catalog_order_role`. For each, set a synthetic nonempty password, require both apply/check sanitized rejection, restore PASSWORD NULL in `finally` and preserve all other attributes. Do not include the three direct LOGIN roles, whose synthetic passwords are required positives. Seven LOGINS' random wrong-password TCP probes and valid-password/session restoration remain; no secret values in logs. This adds cases within existing methods, not identities.

### 7.3.1 Complete existing-class and current-tail manifest

Every existing test identity remains exactly the entry source/CI set. The complete changed-class set and method counts are below; package prefix is `com.onecall.archtests.` and directory gives subpackage. Fixture-only paths `container/CustomerBusinessNumbersFixture.java`, `container/SharingQueryFixture.java`, `container/CustomerMeFixture.java` have no test identities. Thus §6 has34 existing test/helper paths =31 classes +3 helpers, not a guessed count of references.

| Existing changed class | Methods | Identity disposition |
| --- | --- | --- |
| `container/ProductionDatabaseIdentityProvisioningIT.java` | 20 | unchanged |
| `container/CustomerBusinessNumberReadPrivilegeIT.java` | 8 | unchanged |
| `container/CustomerSubjectDetailReadPrivilegeIT.java` | 10 | unchanged |
| `container/CustomerEvidenceReadPrivilegeIT.java` | 10 | unchanged |
| `architecture/CustomerBusinessNumberBoundaryTests.java` | 4 | unchanged |
| `architecture/CustomerSubjectDetailBoundaryTests.java` | 4 | unchanged |
| `architecture/CustomerEvidenceBoundaryTests.java` | 4 | unchanged |
| `container/SharingRoleLeastPrivilegeIT.java` | 12 | unchanged |
| `container/EvidenceMediaReadIT.java` | 8 | unchanged |
| `container/ActivationCommandRoleLeastPrivilegeIT.java` | 10 | unchanged |
| `container/OperationsLifecycleRoleLeastPrivilegeIT.java` | 12 | unchanged |
| `container/OperationsActivationReadRoleLeastPrivilegeIT.java` | 10 | unchanged |
| `container/CustomerBusinessNumbersIT.java` | 18 | unchanged |
| `container/CustomerEvidenceIT.java` | 24 | unchanged |
| `container/CustomerSubjectClaimsIT.java` | 24 | unchanged |
| `container/CustomerSubjectsIT.java` | 20 | unchanged |
| `container/EvidenceMediaReadRoleLeastPrivilegeIT.java` | 8 | unchanged |
| `container/OperationsActivationReadsIT.java` | 18 | unchanged |
| `container/OperationsSubjectReadRoleLeastPrivilegeIT.java` | 12 | unchanged |
| `container/OperationsSubjectReadsIT.java` | 24 | unchanged |
| `container/PublicShareAdmissionIT.java` | 42 | unchanged |
| `container/SharingAdmissionRoleLeastPrivilegeIT.java` | 9 | unchanged |
| `container/SharingQueryServiceIT.java` | 10 | unchanged |
| `container/SubjectClaimRoleLeastPrivilegeIT.java` | 10 | unchanged |
| `container/PostgresContainerIT.java` | 64 | unchanged |
| `container/DatabaseRuntimeAclRbacHardeningIT.java` | 12 | unchanged |
| `architecture/CustomerSubjectActivationBoundaryTests.java` | 6 | unchanged |
| `architecture/CustomerSubjectClaimBoundaryTests.java` | 6 | unchanged |
| `architecture/OperationsActivationReadBoundaryTests.java` | 8 | unchanged |
| `architecture/OperationsLifecycleBoundaryTests.java` | 8 | unchanged |
| `architecture/OperationsSubjectReadBoundaryTests.java` | 8 | unchanged |

Existing affected Surefire8 classes/48 methods + Failsafe23/395 =31/443; adding PA36's1/8 +1/16 gives the complete owned compatibility focus **9/56 +24/411 =33/467**. All other baseline classes/methods are unchanged. Full baseline canonical identity manifest (sort fully qualified class then method, each `class#method\n`, UTF-8/LF) SHA256 is `89262b28603810de7858f39f2bcedf4e598137fc6cf54ecdb8c6249f8dc67843` for107/1208; no existing identity may silently disappear from a historical map.

Current inventory/hash/PA25 tails requiring special scoping (not generic literal replacement):

| Surface / actual method or helper | Frozen assertion preserved | Separate current-state assertion |
| --- | --- | --- |
| `PostgresContainerIT.v001ThroughV009MigrateCleanPostgres16WithoutSkipping`, script inventory helper around10800 | target013/applied13; V001–V013 bodies | all classpath18, append exact V018 script to old17 list |
| `DatabaseRuntimeAclRbacHardeningIT.v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle`, `v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects`, `psql()`, **`assertFrozen()`** | target013/applied13 prefix, all V001–V013 LF pins; target014/oracle28 = customer12+evidence16, V014 old81/final28 and all V014 negative cases | two current PA25 tails: Flyway executed4/applied18, four variables, eleven-edge graph/marker; third current tail: unfiltered `assertFrozen()` directory exact18, not17; both live frozen-check callers retained |
| `CustomerSubjectActivationBoundaryTests.openApiDataDocsAndAllSeventeenMigrationsRemainFrozen`, `FROZEN` | all20 old hash rows (3 contracts +17 migrations) | compare exact directory to17 frozen migration paths union sole V018; count18 |
| `CustomerSubjectClaimBoundaryTests.openApiMigrationsAndProductionProviderAbsenceRemainFrozen`, `FROZEN` | every old raw/LF row including17 migrations | old migration subset17 plus exact new filename, total18 |
| `OperationsSubjectReadBoundaryTests.v001ThroughV017AndFrozenOpenApiAndDesignDocsRemainUnchanged`, `FROZEN` | old raw/LF digests and V001–V017 | current rangeClosed(1,18), exact sole additional filename |
| `OperationsActivationReadBoundaryTests.frozenInputsAndSeventeenMigrationInventoryRemainByteIdentical` | PA34 plan manifest21 and old migration17/contract digests | exact current directory18 and independent V018 digest; historical docs17 body vs new full-document pins |
| `OperationsLifecycleBoundaryTests.frozenOpenApiMigrationsAndPolicyBytesAreUnchanged` | old map22 including17 migrations, docs12 and historical docs17 body | current migration count18, exact set supplied by new boundary test; current docs17 raw/LF pins |
| `OperationsActivationReadsIT.postgres16UsesExactlyFrozenSeventeenMigrations` | PA34 plan manifest21/hash keys21 and frozen17 migration hashes, old docs17 body | applied18, old migration paths union V018 exact directory, current docs17 digest pair; do not edit PA34 plan |
| PA27/PA28/PA29 privilege IT `history`, `currentHistory`, `frozenFiles`, `currentOracle`, `frozenGraph`, current-PA25 methods; three corresponding boundary tests | 015/261,016/269,017/288 and original two-role deltas/history/checksums | only latest() tails018/410, seven-role290 projection, fourth psql input/new output; split historical graph/pool assertions from new PA25, retain old method names |
| `ActivationCommandRoleLeastPrivilegeIT.fixtureCleanupAndFrozenAclInventoryRemainExact`; `OperationsLifecycleRoleLeastPrivilegeIT.frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact` | reflected PA29 `currentOracle()`288 and predecessor row range1..17 | actual latest ACL290 equals independent old288 union two literal tuples; V018 history row count1 (was0); complete total18 |
| `OperationsActivationReadRoleLeastPrivilegeIT`, other latest-history-only rows in §6 | every old ACL/definition/behavior assertion | only total/applied/migrationsExecuted18 as enumerated; current PA25 source token assertions410/18 if present |
| `ProductionDatabaseIdentityProvisioningIT` `EXTERNAL/EFFECTIVE/LOGINS/MANAGED/SLOTS/TARGETS/POOLS`, `psql`, `assertGraph`, `migrationHashes`, `Hash`/hash guards, `currentOracle`/history literals, `deniesEveryUnapprovedRoleTargetIncludingOwnersAndPredefinedRoles`, `unsafeAttributesAndLoginShapeMismatchesFailClosed` | twenty identities, all31 migration-defined roles and V001–V017 hashes; six legacy pool names | external4, monitored9, logins7, managed13, capability boundary13/full boundary17, slots4, TARGETS eleven pairs, project-role count35 (was34; not a suite count), fresh migration/applied18, independent410, exact new markers and four subprocess variables; inner target loop boundary13:91 pairs, allowed14/rejected77; replace `EFFECTIVE.subList(0,4)` with ten NOLOGIN password probes as §7.3, including Operations/IVR and four context targets |

For the three docs17 hash consumers (Operations activation boundary, lifecycle boundary, activation IT), **do not replace a historical pin inside a frozen PA34 manifest**. Keep the **21-row** historical maps/manifests in `OperationsActivationReadBoundaryTests` and `OperationsActivationReadsIT`, and the **22-row** map in `OperationsLifecycleBoundaryTests` (also pins docs12); all three retain their17 migration rows. Preserve raw/LF docs17 pins `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` / `8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f`; apply these to the byte-identical old document body after stripping only the exact newly prepended PA36 section at its deterministic boundary. Separately pin candidate full-doc raw/LF digests in a current map/test assertion after the section bytes settle. The old PA34 plan is never edited; no blanket contains/hash exception. V018 stays outside the historical17 set and is pinned independently in the new PA36 boundary test; exact current directory is union18. `Hash`/`hashes`/`migrationHashes` are embedded helpers/maps, not an undiscovered extra source file.

PA25 current history corruption templates need explicit rebase as well as counts: final row/rank17→18, valid version017→018, wrong-rank sentinel18→19, alternate malformed version17→18, extra versions018/019, copied rank1018 and its exact cleanup rather than1017. Preserve independent corruptions of older rows13/16/17, with save/restore of their exact tuples. An insertion violating a unique key before invoking PA25 is not verifier rejection coverage. Update all expected inheritance predicates in test matrices to match §5; safe Operations/IVR true-INHERIT is a positive, not a mutation to repair.

A read-only repository sweep covers migration directory enumeration, Flyway all/applied/pending counts, `migrationsExecuted`, current PA25 execution/copy/inputs/outputs, `$frozen$`/`$history$` mirrors, reflected oracle projections, docs17 pins and per-class CI tokens. An unbounded migration-directory enumeration is always a **current tail**, including `DatabaseRuntimeAclRbacHardeningIT.assertFrozen()`, even when its hash map covers only a historical prefix. Classify an enumeration as historical only when its filter has an explicit version upper bound matching that prefix; old hash keys alone are not such a bound. Other HashMap/HashSet hits, dynamic before/after hash snapshots, explicitly version-bounded V010–V017 prefix tests, and six-role synthetic Operations fixtures require no edits where they have no latest assertion and intentionally test historical behavior. Customer/public sharing regressions run unchanged (§8); new amplification assertions live in the sixteen-method PA36 suite, not extra test methods hidden in those classes.

### 7.4 CI/POM/XML/token arithmetic

Add exactly `**/EvidenceRoleIvrAssociationAclIT.java` to container-smoke POM includes. No dependency/plugin/profile/toolchain change. CI gets literal `pa36_unit_additions` (8 names) and `pa36_container_additions` (16 names), with fully qualified class keys above. Merge into the existing source/XML identity maps, reject duplicate names and apply existing forbidden-test-style/zero-skip rules.

Preserve **all historical PA11/PA18…PA34 addition/start maps and totals**. Define per class `pa36_names = pa36_unit_additions.get(class_name,set()) | pa36_container_additions.get(class_name,set())`, merge each literal dictionary into its corresponding expected unit/container map before source/XML checking, and include `pa36_names` in `additions` and its subset guard. For PA36-start only subtract8/16 (24 combined), asserting704/504 and1209. For **every earlier** baseline/start reconstruction add `| pa36_names` to that checkpoint's existing subtraction set, both method-count and annotation-removal line-scan paths. This includes baseline plus PA19,20,21,22,23,25,27,28,29,30,31,32,33,34; there is no standalone PA24 or PA26 start counter in current CI (PA26 additions are already in earlier suffix unions). Do not replace old unions by PA36-only subtraction, subtract twice, or inflate historical totals. New classes must be visited in the expected-map iteration and contribute zero to all pre-PA36 starts. Add a new initialized `pa36_start_totals` and `pa36_start_line_scan_total` using current names minus pa36_names.

Exact historical `(Surefire,Failsafe; annotation lines)` targets remain: baseline/PA18 `(112,178;291)`, PA19 `(128,192;321)`, PA20 `(162,198;361)`, PA21 `(196,204;401)`, PA22 `(231,214;446)`, PA23 `(266,214;481)`, PA25 `(309,214;524)`, derived PA26 `(309,234;544)` (printed history, not a standalone accumulator), PA27 `(309,246;556)`, PA28 `(341,272;614)`, PA29 `(375,302;678)`, PA30 `(417,336;754)`, PA31 `(468,370;839)`, PA32 `(520,404;925)`, PA33 `(574,440;1015)`, PA34 `(644,476;1121)`. New PA36 start `(704,504;1209)`. Preserve comment_hits exactly; full candidate line total alone becomes1233. Final totals:

```text
Surefire:  76 + 1 classes = 77; 704 + 8 methods = 712
Failsafe:  31 + 1 classes = 32; 504 +16 methods = 520
Combined: 107 + 2 classes =109; 1208+24 methods =1232
Annotation lines: 1209+24=1233 (same one historical comment)
Failsafe includes: 31+1=32, exact set equality with 32 container classes
PA25 existing methods:20 unchanged; new PA36 IT:16; new PA36 boundary:8
```

Canonical candidate identity-manifest SHA256, under the same sorted `class#method\n` convention as §7.3.1, is `a082db1f77d93e9c76ed20500ca52c9e5c8b96fdc97b7140f0fa7d1972f58b23` (unchanged1208 plus the exact24 named methods above). This is a planning-derived identity set, not executed XML evidence. New-class literal map cardinality lists are `[8]` / `[16]`; no existing class gains a method.

CI current-count token locations to update explicitly: early Surefire sum/error162–163 from704→712; method_totals1899 `[704,504]/1208`→`[712,520]/1232`; only current `line_scan_total` in1900 from1209→1233 (retain new PA36-start1209); class lengths1902–1903 76/31→77/32; include length1907 31→32; exact_inventory calls1916–1917 704/504→712/520; success summary1918 becomes PA36 current77/712+32/520=109/1232, appends PA36-start704/504 and current712/520 without altering any earlier printed history (including derived PA26-start309/234/543 and544 scan). Historical addition-map cardinalities and every old method-name literal remain unchanged, even names containing Seventeen/288/Six. Dependency package delta31 at2311 is **not** a test include count and remains31. POM changes just the one include; no plugin/dependency change. New required assertion tokens include exact two-tuples, evidence16/18, PA25410/history18, four inputs, eleven pairs/options, true-INHERIT role attributes and both sanitized success markers. Preserve PostgresContainerIT64 XML identities and its early exact64 check unchanged.

Source method lists = CI literal names = XML testcase identities; POM include set = container class set. Preserve existing per-class token/forbidden-output contracts, update only current PA25 success/count tokens, add new literal identities and exact expected assertion tokens. No broad regex acceptance, “at least” counts, filtered-out suite, duplicate XML, disabled or skip allowance. CI's full XML evidence must have tests1232, failures0, errors0, skipped0; retain both report directories and signed/count manifests in implementation evidence.

## 8. Validation commands and expected evidence

These are future implementation commands, **not executed by this planning task**. Run from repo root unless `cd backend` is shown. Use the pinned Temurin21.0.13+9 and Maven3.9.9 wrapper; verify Docker first. Tool availability must be checked in the implementation environment; PA25 apply/check may run via container psql as existing tests do. No external documentation/network search was needed to establish repository facts.

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
  -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,CustomerEvidenceBoundaryTests,CustomerSubjectActivationBoundaryTests,CustomerSubjectClaimBoundaryTests,OperationsActivationReadBoundaryTests,OperationsLifecycleBoundaryTests,OperationsSubjectReadBoundaryTests \
  -Dit.test=EvidenceRoleIvrAssociationAclIT,ProductionDatabaseIdentityProvisioningIT,CustomerBusinessNumberReadPrivilegeIT,CustomerSubjectDetailReadPrivilegeIT,CustomerEvidenceReadPrivilegeIT,SharingRoleLeastPrivilegeIT,EvidenceMediaReadIT,ActivationCommandRoleLeastPrivilegeIT,OperationsLifecycleRoleLeastPrivilegeIT,OperationsActivationReadRoleLeastPrivilegeIT,CustomerBusinessNumbersIT,CustomerEvidenceIT,CustomerSubjectClaimsIT,CustomerSubjectsIT,EvidenceMediaReadRoleLeastPrivilegeIT,OperationsActivationReadsIT,OperationsSubjectReadRoleLeastPrivilegeIT,OperationsSubjectReadsIT,PublicShareAdmissionIT,SharingAdmissionRoleLeastPrivilegeIT,SharingQueryServiceIT,SubjectClaimRoleLeastPrivilegeIT,PostgresContainerIT,DatabaseRuntimeAclRbacHardeningIT \
  clean verify
./mvnw -B -ntp -Pcontainer-smoke \
  -Dtest=EvidenceRoleIvrAssociationAclBoundaryTests \
  -Dit.test=EvidenceRoleIvrAssociationAclIT,CustomerShareCommandsIT,PublicSharesIT,CustomerEvidenceIT \
  clean verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

The complete owned compatibility focus expects9 Surefire classes/56 methods and24 Failsafe/411, total33/467. The separate amplification/customer-sharing regression focus expects Surefire1/8 and Failsafe4/66 (16+18+8+24), total5/74. Require exact selected class/method unions, not just totals, and zero skips/errors/failures. Do not compare focused runs to full CI XML inventories or reuse XML across clean runs. Full clean run expects **77/712 +32/520 =109/1232**, zero skipped/error/failure and all32 includes represented. Execute the updated CI XML/inventory/dependency/package scripts verbatim against that run's output, not a hand-simplified surrogate.

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

Run these from repo root (leave `backend` first); protected external connection/service environment selects the intended cluster and actual administrator without putting a password on argv. Expected stdout is exactly the two new seven/nine/eleven markers, exit0, after COMMIT. Reapply/check preserves snapshots. V017-only or any history/ACL/provenance/topology mutation yields the specified sanitized nonzero category, no success marker and no persistent mutation. No hosted live execution is implied by these commands.

Capture before/after raw and LF manifests for V001–V017 and frozen contracts/product tree. Also validate an **external `/tmp` LF projection** of the migration/contract inputs with exact expected checksum/history and raw frozen input comparison; never normalize the real worktree. Exercise fresh and017 upgrade plus focused replay in that projection. Reuse existing repository conventions; if Windows wrapper line endings require a launcher, an external temporary LF wrapper shim is permitted, not a wrapper edit. Do not reuse stale XML after `clean` or report earlier candidate results as current.

Packaging/dependencies: `V018` is the sole newly packaged production resource. PA25 SQL stays outside `src/main/resources` and out of the executable JAR; new tests/test helpers/fixtures/verifiers must not be packaged. Keep existing six-tuple dependency and JAR allowlists **unchanged**: baseline37 resolved/34 packaged; approved72/65, deltas35/31, dependency-only8, counterpart64 and packager1. Run existing adversarial firewall probes unchanged (currently116) and the packaged launcher/transport checks from CI. Extend the JAR resource assertion to require exactly one V018 entry, unchanged predecessor entries and no PA25 script. No new library, pool, environment secret, port, network/firewall rule, DNS/TLS exemption or PostgreSQL extension is needed. Existing PG/CI Docker access and external protected credential/TLS controls remain prerequisites.

Run existing contract regressions unchanged after `npm ci` in `api/openapi`: `npm run check:s3`, then the A2/A3/A4/A5 commands exactly as in `.github/workflows/ci.yml`. Classify already-recorded A2/A5 baseline drift against the baseline before calling a failure pre-existing; do not “fix” frozen contracts or reduce validators. New docs17 content must not invalidate A3. This task adds no npm/dependency change.

## 9. Residual risks, stop rules and handoff

| Risk | Owner / destination | Required treatment |
| --- | --- | --- |
| Existing IVR/Operations roles contain writes and sensitive cross-module rights | Database/Security; docs17 current boundary and production acceptance | Preserve bytes/ACLs; explicit SET is not a read-only privilege sandbox. LO is a trusted backend credential; the existing shared read transaction must stay read-only. Narrowing old roles is a separate goal, not PA36. |
| Full `identity_at_capture` JSON is already readable at raw evidence-role boundary | Security/Evidence Backend; docs17 and PA35 owner collaborator | No new JSON access; scalar-safe outputs/logging are application controls, not JSON-path ACLs. No PA35 payload contract invented here. |
| Role-wide association-read amplification reaches existing LW/customer-sharing consumers | Security/Evidence/Sharing Backend; docs17 + targeted PA36 IT and unchanged sharing regressions | Prove LW/LO scalar readability and unchanged response/log exclusions; no payload output, new application authority or RLS claim. |
| Migration own-checksum circularity and post-migration tampering | Database/CI; Flyway validate + PA25 exact18 oracle | Exact prior17 in V018; exact self checksum in external verifier and pinned tests. No claim raw replay alone authenticates source bytes. |
| Cluster-global roles, other DB dependencies, concurrent administrators and marker custody | Platform/Database/Security; PA25 runbook Q17/Q23 and change process | Shared advisory lock is cooperative only; exclusive administration and protected comment/catalog writers remain external prerequisites. No adoption/force recovery. |
| TLS/pg_hba/secrets, authentication/rotation, pool drain and stale sessions | Platform/Security/Operations; production acceptance checklist | Seven real TCP/TLS/credential proofs, old-password denial on new connections and old-session policy remain NOT EXECUTED. PASSWORD NULL is not credential readiness. |
| `postgres:16` unpinned digest and hosted test headroom | CI/Platform; implementation evidence | Record actual patch/digest; unchanged30-minute job budget must be checked with full run. Do not silently repin dependencies or reduce tests; escalate exhausted budget. |
| LF/raw differences, Windows Git pointer and historical inventory drift | Controller/Backend CI; intake and implementation evidence | Preserve raw bytes and old assertions; validate LF separately; retain the reconciled c89e784 Control-only child vs merged5200355 base evidence; no repository plumbing mutation. |
| Existing A2/A5 validator drift | Protocol/Data/Backend contract maintainers; existing baseline destinations | Reproduce/classify; no unrelated fix in PA36. |

Minimality argument: exactly two object-ACL tuples; no business/schema/API/application edit; one additional external credential instead of contaminating existing identities; exactly seven non-inheriting SET edges for the existing shared read consumer (eleven total); unchanged existing120 privileges merely enter the verifier's coverage. One migration and one atomic verifier/topology slice prevent drift. Historical prefixes remain historical rather than weakening checks to make latest-chain tests pass.

Stop if implementation needs another column, broad SELECT, write grant, runtime/owner role alteration, role hierarchy, extra LO membership, new pool, PA35 code or public contract change. Route the concrete requirement to Controller for goal confirmation. Scope corrections for newly discovered current-only test assertions require a named path and unchanged inventory intent, not implicit permission.

**Controller handoff:** stop for intake and fresh peer-isolated independent A/B re-review of fix2; no passed gate or implementation authorization is asserted. All four accepted re-review findings have explicit plan dispositions, with unchanged option-A scope,43 paths and8+16 new identities. No runtime validation has been run or claimed by the planning agent. Only this cumulative plan is updated and its fix-2 planning artifact added; fix1 remains historical and byte-identical. No source/test/migration/current documentation/Control/CI/POM edit, staging, commit, push, PR, review request, approval, merge, issue mutation or implementation has been performed. PA35 stays paused until PA36 independently passes its later gates and Controller explicitly resumes it.
