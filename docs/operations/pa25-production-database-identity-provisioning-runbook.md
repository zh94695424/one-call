# PA25 PostgreSQL 16 runtime identity provisioning

## PA29 Current Runtime Boundary (V017)

This current boundary supersedes PA28 V016 and all older current capability/history/inventory statements below; their evidence remains historical. PA29 adds only `GET /api/v1/customer/evidence` / `listCustomerEvidence` and `GET /api/v1/customer/evidence/{evidenceId}` / `getCustomerEvidence`. OpenAPI and V001–V016 remain byte-frozen. Existing customer endpoints retain their prior contracts.

Exact safe shapes: page has only `items`, `nextCursor`, `hasMore`; summary has only `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, `createdAt`, `artifactRoles` (always present, including `[]`). Detail has only `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, optional `solidifiedAt`, `artifacts`; **detail has no `createdAt`**. Each artifact requires `artifactRole`; optional `mimeType`, `sizeBytes`, `validatedDurationSeconds`, `sha256` are omitted when absent, not fabricated. `capturedAt` is the validated immutable `identity_at_capture.capturedAt` scalar, never live identity or snapshot fallback. All four statuses PENDING, SOLIDIFIED, QUARANTINED, FAILED are visible; detail re-reads current facts. Artifacts order ORIGINAL then DERIVED, then hash/size/MIME; duration is authoritative only for ORIGINAL with a unique validated source/media match, never DERIVED.

Only the verified session supplies account/workspace authority. Personal scope matches subject `owner_account_id`; selected organization scope matches `owner_organization_id`, requires ACTIVE account/organization/membership and accepts ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR. Inactive lifecycle is FORBIDDEN 403. Missing/unowned/foreign-workspace/other-subject detail is uniform NOT_FOUND 404. Any OPEN risk on any binding, regardless of binding status, of **any accessible subject having evidence** denies the **whole list with 403 before pagination**, including off-page subjects; owned subject without evidence does not deny the list. Detail checks requested subject risk after ownership. Malformed UUID/limit returns 422; malformed/tampered/context-replayed cursor returns 400; unsupported methods/aliases/extra route segments return 404. Authentication precedes input parsing; ignored query/header/body selectors never grant authority. Unrepresentable persisted detail facts fail closed as 404, list facts as sanitized 500; dependency/completion failure is sanitized 503.

Stable keyset order is `createdAt DESC, evidenceId DESC` using PostgreSQL UUID order, fetch limit+1, next anchor from the last returned item; tail has `nextCursor: null`. Default limit 20, inclusive 1–100. HMAC-SHA256 cursor binds version, fixed evidence endpoint, verified account, workspace type/organization, fixed sort and anchor; limit is deliberately not cursor-bound; no offset/count or cross-request snapshot promise. Explicit independent `onecall.customer.evidence.cursor-hmac-key` must be canonical base64 of exactly 32 bytes; there is no production default. Key/service resolution stays lazy after structural input validation; detail never resolves the cursor key.

Customer composes only named `customer-subject-query` and `customer-evidence-query` owner interfaces. Lifecycle, ownership, all-binding risk and batched safe facts share one customer-role **read-only REQUIRES_NEW / REPEATABLE_READ** transaction/connection; no N+1 or business/event/audit/outbox writes. Pool state and suspended transactions restore on success/failure. Authorization is application predicates plus exact ACLs, not RLS or linearizable revocation: later committed changes govern the next request.

V017 is privilege-only: **19** owner-issued column SELECT tuples, no grant options: `evidence_record(id,status,source_type,source_ref_id,identity_at_capture,consent_version,created_at,solidified_at)` (8), `evidence_artifact(evidence_id,artifact_role,media_object_id)` (3), `media_object(id,mime_type,size_bytes,sha256)` (4), `recording(id,status,media_object_id,validated_duration)` (4). All grantors are `evidence_owner`, grantee `customer_iam_role`. Arithmetic: **269 + 19 = 288 = customer53 + evidence16 + others219**; customer53 = 6 schema USAGE + 47 column SELECT, target pair50→69. No new table SELECT, writes, routines, sequences, schema CREATE, roles, memberships or pools. The JSONB column grant makes full capture JSON readable at the raw-role boundary: scalar-only SQL/output/logging is a trusted-application Security/Database residual, **not JSON-path security**.

Apply/Flyway-validate V001→V017 before current PA25 apply/check: exact **17** successful SQL history rows, V016-only rejected. V017 raw SHA256 `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad`; Flyway checksum **-41822162**. Replay accepts only exact V016 pre-state or complete V017 final state; partial/widened/wrong-grantor/grant-option/history drift rejects atomically without PA25 repair. Six logins/seven effective roles/four membership edges and credential/provenance/owner/PUBLIC/default-ACL invariants remain unchanged.

Required source=CI=POM=XML inventory is **49 classes / 417 Surefire + 21 / 336 Failsafe = 70 / 753**, 76 additions (42/34), annotation lines **754** with one historical comment, **21** exact Failsafe includes. Execution outcomes and any NOT EXECUTED gates belong to `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md`, not an inferred pass from these requirements.

No media bytes/playback/download/URLs/storage/crypto references, share credentials/operations, live identity, snapshot/event payloads, lifecycle mutation or session issuance is added. External production credentials, TLS/pg_hba, migrator custody, deployment/rotation/pool drain/session invalidation and Q17/Q23 remain Platform/Database/Security/Operations work: **NOT EXECUTED; production/live readiness NOT CLAIMED**.

## PA28 Historical Runtime Boundary (V016; superseded by PA29 V017)

This boundary supersedes all older current-runtime capability, history and inventory claims below; historical model/catalog evidence remains unchanged. Only `GET /api/v1/customer/subjects/{subjectId}` / `getCustomerSubject` is added: success contains exactly `subjectId`, `activationStatus`, `evidenceCount`, `orderCount`. OpenAPI and V001–V015 remain byte-frozen. Optional `numberMasked`, `trial`, `consentRequirements` are absent, including nulls; `claimState` is not a schema field. Larger operation prose does not authorize those future projections.

Authority comes only from the verified session's account and selected workspace. ACTIVE account is required. Personal access requires matching `owner_account_id`; organization access requires matching `owner_organization_id`, ACTIVE organization/membership and ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER or AUDITOR. Missing/unowned/foreign-workspace subjects return the same NOT_FOUND after lifecycle checks; inactive contexts are FORBIDDEN. After ownership, any OPEN risk on any binding of the subject is FORBIDDEN, regardless of binding status; absent risk is not OPEN. No client owner/workspace/role selector is authoritative. Extra query/body inputs are ignored; malformed full UUID values return 422, extra path segments/unsupported methods 404. Null owner/service results are INTERNAL, not not-found.

Activation uses the existing owner projection, defaulting to INACTIVE without insertion. Evidence counts every `evidence_record` for the subject, all statuses. Orders count all statuses for that subject: personal requires purchaser account equality AND NULL organization; organization requires selected organization equality, regardless of purchaser. Counts disclose no row content/media. Lifecycle, ownership, all-binding risk, activation and counts execute on one customer-role read-only REQUIRES_NEW / REPEATABLE_READ snapshot; commit/completion failures return sanitized 503, including the empty/not-found path. This is not RLS or linearizable revocation and adds no business writes.

Owner seams remain in subject-number (`customer-subject-query`), evidence and catalog/order (`customer-subject-count-query`); customer orchestrates without owner-internal SQL. Existing activation seam is reused. V016 is privilege-only and adds exactly **8 = 2 schema USAGE + 6 column SELECT** owner-issued/no-grant-option tuples: USAGE on evidence/catalog_order; SELECT `subject_number.subject_risk_state(caller_number_binding_id,state)`, `evidence.evidence_record.subject_id`, and `catalog_order.customer_order(subject_id,account_id,owner_organization_id)`. Customer becomes **34 = 6 schema + 28 column**; evidence remains **16**, other roles **219**, independent seven-effective-role PA25 oracle **269**. No table SELECT, ciphertext/HMAC, claim/consent/trial reads, routines/sequences, writes, schema CREATE, grant options, new roles or pools. Predicate-column raw reads remain sensitive within the trusted-application boundary.

Apply and Flyway-validate V001 → V016 before current PA25 apply/check. Exactly sixteen successful SQL rows with exact rank/version/script/description/checksum are required; V014/V015-only is rejected. V016 `V016__customer_subject_detail_read_privileges.sql` has Flyway checksum **1669606497**. Replay accepts only exact clean V015 pre-state or complete V016 final state; partial/widened/wrong-grantor/grant-option states fail without repair. Six logins / seven effective roles / four edges remain LQ→sharing, LC→customer, LW→command and evidence, with ADMIN FALSE / INHERIT FALSE / SET TRUE; direct login boundaries, OIDs, credential verifiers, provenance, owners, PUBLIC/default ACLs and unrelated privileges are preserved.

Required inventory: **44 classes / 375 Surefire + 19 / 302 Failsafe = 63 / 677**, annotation-line scan **678** (one historical comment), nineteen exact Failsafe includes; 64 new named tests preserve all 613 baseline identities. Prefix-pinned PostgresContainerIT/PA26/PA27 security checks remain historical; only explicit latest upgrades use V016/current PA25. Execution evidence, not these requirements, is recorded in `docs/gateflow/pa28-customer-subject-detail-read-foundation-s1-implementation.md`.

External production connection, credential assignment, TLS/pg_hba, migrator/comment custody, deployment/rotation/pool-drain/session-invalidation acceptance and Q17/Q23 remain Platform/Database/Security/Operations residuals: **NOT EXECUTED; production readiness NOT CLAIMED**.

## PA27 historical (V015; superseded by PA28 V016)

Historical-only notice: the following PA27 statements, numbers and commands describe V015, not the current V016 runtime. Current PA25 requires V001→V016 as specified above.

This section supersedes the following PA26 V014 section, its capability rows, history prerequisites and inventory claims, and all older current customer-capability claims. It does not change the structural/business/security model or unrelated roles. V001–V014 and OpenAPI remain byte-frozen. Apply and validate V001 → V015 before current PA25 apply/check: exactly fifteen successful SQL history entries with independent exact script/description/checksum/rank validation are required; V014-only is rejected. V015 is privilege-only, transactional and replayable only on the exact clean pre-state or complete final state; partial, widened, wrong-grantor or grant-option states fail without repair.

Customer has **26 = 4 schema USAGE + 22 column SELECT** explicit owner-issued/no-grant-option tuples. Retained V014 reads are `customer_iam.customer_account(id,display_name,status)`, `organization_access.organization(id,display_name,status)` and `organization_access.organization_membership(organization_id,account_id,role,status)` plus USAGE on those two schemas. Exactly **14 = 2 schema + 12 column** additions are:

| Owner and grantor | Exact addition to `customer_iam_role` |
| --- | --- |
| `subject_number_owner` | USAGE on `subject_number`; SELECT `(id,owner_account_id,owner_organization_id,real_name_status,created_at)` on `subject_number.business_subject`; SELECT `(id,subject_id,number_masked,number_type,status)` on `subject_number.caller_number_binding` |
| `activation_consent_owner` | USAGE on `activation_consent`; SELECT `(subject_id,status)` on `activation_consent.service_activation` |

Evidence remains **16** tuples; the independent seven-effective-role PA25 oracle is **261**, with the other-role **219** unchanged. No table SELECT, number ciphertext/HMAC, claim/risk/consent reads, business writes, routines, sequences, schema CREATE, grant option or privilege outside this delta is added. Existing V014 restrictions remain. Six logins / seven effective roles / four edges remain unchanged: LQ → sharing; LC → customer; LW → command and evidence; three direct logins retain their boundaries. Edge options remain ADMIN FALSE / INHERIT FALSE / SET TRUE; provenance, OIDs, credentials, owners, PUBLIC/default ACLs and unrelated grants are preserved, never repaired or logged.

Only `GET /api/v1/customer/business-numbers` / `listCustomerBusinessNumbers` is added. Verified session account and selected workspace alone select authority. Current ACTIVE account is required; organizations additionally require ACTIVE organization/membership with any of ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR. Inactive/missing context returns generic 403; active empty scope returns 200 with `items: []`, `nextCursor: null`, `hasMore: false`. Every owned binding is visible for ACTIVE/DISABLED/RETIRED and both number types/real-name states. All INACTIVE/ACTIVE/SUSPENDED activations remain visible; absent activation alone defaults INACTIVE. Seven item fields and three page fields are the frozen masked projection; timestamps and owner IDs are not serialized.

One LC/customer-pool read-only REQUIRES_NEW REPEATABLE_READ transaction sets/asserts `customer_iam_role`, checks lifecycle and runs the owned subject-number query plus one bounded activation batch on the same snapshot. Cleanup restores pooled state. This is not RLS or linearizable revocation: application predicates enforce tenant isolation; a later committed revocation applies on the next request. Order is subject.createdAt descending / native binding UUID ascending; limit defaults 20, range 1–100, fetch limit+1, cursor from last returned row, null on tail; no offset/count or cross-page snapshot promise.

Security/Operations must externally supply `ONECALL_CUSTOMER_BUSINESS_NUMBER_CURSOR_HMAC_KEY`, canonical padded standard base64 of exactly 32 random bytes. The endpoint-only HMAC-SHA256 cursor is canonical 93 bytes / 124 base64url characters, binds account/workspace/order/anchor and has no TTL. The key is mandatory even for first/empty pages; absent/invalid key produces sanitized 503 without eagerly activating unrelated endpoints. All replicas require one consistent key; replacement invalidates prior cursors with CURSOR_INVALID. No live secret is assigned here. Error precedence is route 404 → authentication 401/503 → workspace 503 → limit 422 → structural cursor 400 → key 503 → MAC/scope 400 → lifecycle 403; role failures map 503 and projection failures 500.

Current exact acceptance inventory is **40 Surefire classes / 341 methods + 17 Failsafe classes / 272 methods = 57 / 613**, annotation-line scan **614** (one retained historical comment), **17** explicit Failsafe includes. Existing methods are preserved. `PostgresContainerIT` remains the V001–V013 prefix (64 methods), observing V014/V015 ABOVE_TARGET; twelve PA26 methods remain pinned to V014; latest-chain suites target V015. These are source/CI requirements, not a passing XML or production claim. Actual commands, failures and unavailable validation are recorded in the PA27 S1 implementation report.

Backend/Architecture owns deferred qualified-DSL inversion review; Backend/Database owns representative sort/index EXPLAIN; Security/Database owns the non-RLS limitation. Platform/Database/Security retain Q17/Q23, migration/comment custody, credential assignment, TLS/pg_hba and exclusive administration; Security/Operations retain key distribution/rotation, old-session invalidation and pool drain. Production connection, deployment, credential assignment and live acceptance: **NOT EXECUTED**. Production readiness: **NOT CLAIMED**.

## PA26 Historical Runtime Boundary (V014; superseded by PA27 V015)

Historical-only notice: every capability row, history prerequisite, supersession ledger and inventory claim in this PA26 section describes V014-era evidence only. The preceding PA27 section is the current authority, including fifteen-row history and customer26/global261; no older “current” wording in this section overrides it.

This section supersedes the two target roles' V001–V013 capability claims, not the frozen structural/business model or any other role. Apply/validate V001 → V014 before PA25 apply/check; a V013-only database is rejected. V014 is append-only and privilege-only: no production Java/configuration, role graph, ownership, business object, trigger/function definition, PUBLIC or default-ACL change. No future customer/profile mutation or evidence uploader capability is reserved.

All grants below are explicit owner-issued grants without grant option. There are no target-role table-level grants.

| Role | Schema USAGE only | Exact column SELECT only |
| --- | --- | --- |
| `customer_iam_role` | `customer_iam`, `organization_access` | `customer_iam.customer_account(id,display_name,status)`; `organization_access.organization(id,display_name,status)`; `organization_access.organization_membership(organization_id,account_id,role,status)` |
| `evidence_role` | `evidence` | `evidence.evidence_record(id,subject_id,status,source_type,source_ref_id,identity_at_capture)`; `evidence.evidence_artifact(evidence_id,media_object_id,artifact_role)`; `evidence.media_object(id,sha256,size_bytes,mime_type)`; `evidence.recording(media_object_id,validated_duration)` |

The grantors are `customer_iam_owner`, `organization_access_owner` and `evidence_owner` for their respective objects. Customer has 12 explicit tuples (2 schema + 10 column), replacing 39; evidence has 16 (1 + 15), replacing 42. PA25's independent seven-role oracle is exactly 247 tuples: the other 219 are unchanged. Neither target can read any `auth_identity`, `identity_profile` or `evidence_event` column; evidence cannot read storage locators/keys or remaining recording/record payloads. No INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER, sequence privilege, schema CREATE, application routine EXECUTE or grant option remains. FOR UPDATE/FOR SHARE are denied. PostgreSQL built-ins are not revoked.

The historical customer `platform`/`audit` USAGE and four append/idempotency routines, three IAM guard EXECUTEs, all three IAM table SELECT/INSERT and fifteen IAM UPDATE-column tuples are superseded by the customer row above. Its seven organization projection columns remain. Evidence's five old table SELECT/INSERT pairs, nine UPDATE columns, five guard EXECUTEs, two append routines, former subject/IVR/entitlement column reads and `subject_number`/`ivr`/`entitlement_metering`/`platform`/`audit` USAGE are removed. Owner fixtures retain ordinary invoker guards and force deferred FK/cycle checks before leaving their owner role; guard definitions are not upgraded to SECURITY DEFINER.

Six logins / seven effective roles / four edges remain: LQ → `sharing_role`; LC → `customer_iam_role`; LW → `sharing_command_role` and separately `evidence_role`; three direct login boundaries remain unchanged. Each edge is ADMIN FALSE / INHERIT FALSE / SET TRUE. SET ROLE authority follows `session_user`: LW command ↔ evidence restoration is allowed; LC cannot enter evidence/command. No additional membership or production Java switch is introduced. Exact PA25 v1 database-OID/role-OID/slot/name provenance is retained; non-NULL externally assigned verifiers are allowed and preserved byte-identically, never printed or cleared. No unmarked-name adoption or broad ACL repair exists.

The current required inventory is 36 classes / 309 Surefire methods + 15 / 246 Failsafe = 51 / 555; annotation scan 556 (one historical comment), fifteen exact Failsafe includes. `PostgresContainerIT` deliberately proves only the V001–V013 historical prefix (64 methods); the twelve-method PA26 suite and every other latest-chain application/provisioning suite prove V014. Counts are requirements, not a substitute for zero-skip XML evidence.

Projection access remains cross-row sensitive, especially `identity_at_capture`; no RLS/tenant boundary or content declassification is claimed. Platform/Database/Security retain Q17/Q23, migrator/comment custody, external login selection, pg_hba/TLS and exclusive administration; Security/Operations retain rotation, old-session invalidation and pool drain. Live production connection, credential assignment, deployment/rotation acceptance: **NOT EXECUTED**. Production readiness: **NOT CLAIMED**.


## Authority and limits

This is the bounded operator handoff for the accepted PA25 S1 contract. The executable is `backend/ops/postgresql/pa25-runtime-identities.sql`, not a Flyway migration or an application initializer. It is outside the runtime JAR. V001–V014 remain byte-frozen; V014 narrows the two target roles and V015 adds only the approved fourteen customer read tuples before this script runs. There is no business DDL, provisioning-time object ACL repair, new datasource or credential manager.

Backend owns this nonsecret contract and disposable acceptance tests. Platform/Database owns the selected cluster/database, actual superuser migrator/provisioner, protected service profile and exclusive cluster-wide administration window. Security owns credential generation, distribution, authentication policy, TLS, `pg_hba`, expiry approval and administrative audit. Operations owns deployment evidence, pool drain/restart and recovery coordination. No production connection, identity, password or endpoint is supplied here.

Role comments are trusted administrative assertions, not signatures. Anyone authorized to write the relevant shared-catalog comments can forge provenance; PostgreSQL administrative/catalog-writing authority must therefore be explicitly protected and audited. An advisory lock only coordinates cooperative executions in the same database. Platform must exclude concurrent role/catalog writers across the entire cluster, including other databases. A provider that cannot provide the frozen PostgreSQL 16 superuser authority blocks deployment; CREATEROLE alone is insufficient.

## Exact current identity map

The symbols LQ, LC and LW are private, externally approved names, not literal deployed names. There are six logins, seven effective roles, ten managed nodes and four membership edges.

| Boundary | Credential inputs | Effective identity |
| --- | --- | --- |
| Sharing query (`persistence`) | `ONECALL_DB_URL`, `ONECALL_DB_USERNAME`, `ONECALL_DB_PASSWORD` | LQ → `sharing_role` |
| Customer context (`web`) | `ONECALL_CUSTOMER_DB_URL`, `ONECALL_CUSTOMER_DB_USERNAME`, `ONECALL_CUSTOMER_DB_PASSWORD` | LC → `customer_iam_role` |
| Sharing commands (`persistence`, `ONECALL_SHARING_COMMAND_ENABLED=true`) | `ONECALL_SHARING_COMMAND_DB_URL`, `ONECALL_SHARING_COMMAND_DB_USERNAME`, `ONECALL_SHARING_COMMAND_DB_PASSWORD` | LW → `sharing_command_role` and separately `evidence_role` |
| Public share GET (command-enabled persistence) | `ONECALL_SHARING_PUBLIC_READ_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Direct `sharing_public_read_role` |
| Admission (`persistence`, `ONECALL_SHARING_ADMISSION_DATASOURCE_ENABLED=true`) | `ONECALL_SHARING_ADMISSION_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Direct `sharing_admission_role` |
| Evidence media read (`persistence`, `ONECALL_EVIDENCE_MEDIA_READ_DATASOURCE_ENABLED=true`) | `ONECALL_EVIDENCE_MEDIA_READ_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Direct `evidence_media_reader_role` |

The `_USERNAME` and `_PASSWORD` suffixes in the last three rows extend that row's full `_URL` prefix. Direct usernames are fixed. Use actual Spring environment binding; do not invent dashed-name aliases. No pool may fall back to another boundary's credentials. The evidence projection reader uses the command's same transaction/connection, not a seventh datasource. Default application startup remains database-free.

All four approved edges require `ADMIN FALSE, INHERIT FALSE, SET TRUE`. No nested/reversed/duplicate-grantor/additional/inbound-login edge is accepted. In particular, do not grant `evidence_role` to `sharing_command_role`: the membership belongs to LW. Direct logins have no memberships in either direction. Session-wide SET ROLE to the same approved target is a PostgreSQL capability; the application deliberately uses SET LOCAL ROLE. Own-role/RESET/NONE restoration is allowed.

The former customer/evidence write/routine and broad evidence capabilities are superseded by V014's restrictions with the exact PA27 V015 read delta in the current-boundary table. LW retains the needed evidence assumption but not its historical writes or routines. Read-only transactions are not the authorization proof; exact database ACLs are. PA25 still does not repair object ACLs. The evidence media reader has no runtime current-user assertion; its fixed username, qualified pool and ACL boundary are distinct proofs.

## Preconditions and execution order

1. Platform/Database records the intended cluster/database and authorized administrator privately; verifies PostgreSQL major 16 and actual current/session superuser authority; approves protected, sanitized client/server/audit logging and an exclusive cluster-wide change window. A structurally identical wrong cluster cannot be identified by this script: independently verify service resolution.
2. The migration owner runs/validates V001 → V015 using the approved external Flyway mechanism. Require exactly fifteen successful SQL history entries, exact scripts/checksums and named module ownership. Preserve migration-era provenance for the three direct logins: V011/V012/V013 may encounter existing names during migration, so custody of the original migration process/evidence remains necessary. PA25 does not retrospectively attest their creation and does not rewrite or replay those migrations after password handoff.
3. Platform/Security approves absent LQ/LC/LW names matching `[a-z_][a-z0-9_]{0,62}`. They must be distinct and cannot be managed/system/administrator names. Provisioner runs initial `apply`, or `check` for an existing provisioned deployment. An existing compatible but unmarked name is a collision, never an adoption candidate.
4. Security generates six independent credentials and distributes them using the existing private mechanism. Use an approved non-logged process such as controlled interactive `psql \password`, with server/audit/history handling separately approved. Never put passwords/verifiers in `-v`, a URI, command-line ALTER PASSWORD, shell trace, repository `.pgpass`, report or CI artifact. The script accepts only the four nonsecret variables below; it neither reads nor changes login password verifiers.
5. Provisioner reruns `check`. Platform/Database/Security then independently prove each actual runtime login's TCP authentication, TLS/peer verification, `pg_hba` policy, current/session identities, role-switch boundary and actual application operation. Container-local fixture trust is not production authentication evidence. Production checks must exclude trust/other password-bypass paths as required by approved policy.
6. Operations records sanitized evidence and obtains the external deployment/security decisions. Synthetic success alone does not authorize deployment or establish production readiness.

Six logins are LOGIN/NOINHERIT, non-owner, NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION/NOBYPASSRLS, connection limit -1, no role/database-role settings or direct database grants. Validity is NULL except the frozen evidence reader's infinity. The four effective service roles remain NOLOGIN; sharing/customer/evidence retain INHERIT while command retains NOINHERIT, with NULL passwords. There is no automatic attribute demotion, expiry normalization, settings reset, ownership transfer or object ACL repair.

## Nonsecret invocation

Run from repository root with an externally approved service/authentication profile. All shell values here are externally supplied nonsecret profile/identifier names. No real values or secret-bearing example file belongs in Git.

```sh
PGSERVICE="$PA25_ADMIN_SERVICE" psql -X -w -v ON_ERROR_STOP=1 \
  -v mode=apply -v sharing_login="$PA25_SHARING_LOGIN" \
  -v customer_login="$PA25_CUSTOMER_LOGIN" -v command_login="$PA25_COMMAND_LOGIN" \
  -f backend/ops/postgresql/pa25-runtime-identities.sql

PGSERVICE="$PA25_ADMIN_SERVICE" psql -X -w -v ON_ERROR_STOP=1 \
  -v mode=check -v sharing_login="$PA25_SHARING_LOGIN" \
  -v customer_login="$PA25_CUSTOMER_LOGIN" -v command_login="$PA25_COMMAND_LOGIN" \
  -f backend/ops/postgresql/pa25-runtime-identities.sql
```

`-X` suppresses local startup commands; `-w` prevents unexpected password prompting. Retain only the exit status and fixed sanitized marker. Success is exactly `PA25_APPLY_OK logins=6 effective_roles=7 memberships=4` or `PA25_CHECK_OK logins=6 effective_roles=7 memberships=4`, after COMMIT. Failure exits nonzero with a fixed `PA25_*_REJECTED` category; provenance failures use `PA25_PROVENANCE_REJECTED`. Do not retain raw connection/authentication diagnostics or catalog rows. Client connection failures can occur before the script takes control: the external launcher/operator must sanitize them too.

## Creation-only provenance and replay

Each newly created external login receives exactly one shared role comment, in the same explicit transaction as creation and membership grants:

```text
onecall:pa25:v1:db=<database_oid>:role=<role_oid>:slot=<slot>:name=<login_name>
```

There is no whitespace/newline. Decimal OIDs have no leading zero. Slots are `sharing_query`, `customer_context`, `sharing_command`, respectively. The script constructs the marker from the current database/role catalogs and chosen validated name. All existing external markers are checked byte-for-byte using UTF-8 bytea equality, independently of collation, before any DDL, and checked again after reconciliation.

`apply` creates only absent external roles, immediately comments only those newly created roles, reaffirms already-compatible marked login flags and repairs only absent approved edges. It preserves existing marker bytes, role OIDs, compatible edge grantors, password verifiers, validity, settings and object ACLs. `check` never changes persistent state. Both modes validate history, safe attributes, ownership, all touching memberships, shared dependencies, explicit/effective ACLs, PUBLIC/current-database/default ACLs and the exact 261-tuple current seven-role privilege set. Locking and transaction-local GUCs stay inside the same explicit transaction. All guarded changes roll back together on rejection.

Unmarked, missing, empty, malformed, wrong-version, copied, renamed or OID-mismatched markers reject. Copying a marker between slots, roles or databases is invalid. Restore/drop-recreate changes to OIDs require external disposition. PA25 never adopts an existing name, regenerates/restamps a lost comment, accepts an operator-provided marker, or adds comments to direct-login/NOLOGIN migration roles. Protect comment-writing authority accordingly.

## Stop, recovery and rotation handoff

On any incompatibility stop rollout, retain only sanitized category/version/count evidence, and escalate to Platform/Database/Security. Do not retry with disabled guards, shared application superuser credentials or a guessed old name. A provenance rejection is not permission to manually stamp a colliding role. The script has no adoption/override/force/revoke/drop/rename mode and no destructive rollback.

Investigation and any authorized remediation occur through a separate protected external change process. Do not transfer ownership, alter object grants, delete settings, replay old migrations, drop old logins or terminate sessions as an implicit PA25 recovery action. Current-database PUBLIC CONNECT is preserved, while CREATE/TEMP and unapproved settings are rejected. Other-database/shared dependencies and strict cluster policy conflicts need explicit disposition. The script does not certify all cluster PUBLIC reachability or client connection options.

Security owns external password/credential rotation and expiry decisions; Operations owns distribution, rollback window and rolling pool drain/restart. Existing authenticated pooled sessions survive password changes. Prove new-password success, old-password denial on new connections, required TLS/authentication, pool recycling and the unchanged graph using separately approved evidence. Script rollback does not roll back external credential rotation. No rotation interval, secret manager, certificate, `pg_hba` rule or topology is invented here.

## Acceptance evidence checklist

| Evidence | Owner | Live status |
| --- | --- | --- |
| Approved intended cluster/database, exact PG16 patch/image/provider and actual superuser principal | Platform/Database | NOT EXECUTED |
| Exclusive cluster-wide administration and protected comment/catalog writers/audit | Platform/Database/Security | NOT EXECUTED |
| V001–V015 validate/checksums/history and frozen direct-login migration custody | Database | NOT EXECUTED |
| Private LQ/LC/LW mapping and six independent credential-version/distribution records | Platform/Security | NOT EXECUTED |
| Sanitized initial apply, post-handoff check and replay/no-drift evidence | Database/Operations | NOT EXECUTED |
| Six real TCP/TLS/password-authentication and wrong-password-denial proofs | Platform/Database/Security | NOT EXECUTED |
| Role/ACL/read-only/writable application probes and pool restoration | Backend/Operations | NOT EXECUTED |
| Rotation, old-password denial, session drain, recovery/rollback window | Security/Operations | NOT EXECUTED |
| Production deployment/security acceptance and residual-risk ownership | Platform/Database/Security/Operations | NOT EXECUTED |

The checked-in test class has twenty ordinary methods, each with a fresh migrated `postgres:16` fixture and synthetic UUID credentials; the image tag is inherited and unpinned. Actual image digest/patch and XML results belong in execution evidence, not inferred from the tag. The full current expected inventory is 40/341 Surefire plus 17/272 Failsafe = 57/613, annotation scan 614 and seventeen Failsafe includes. These are acceptance requirements, not claims that tests or live checks have passed.

**Live acceptance NOT PERFORMED / production readiness NOT CLAIMED.**
