# PA31 Customer Subject Activation Foundation — Implementation Plan

## 1. Identity, gate and authority

- Original task: `pa31-subject-activation-plan-gpt-20261113-02`; prior fix: `pa31-subject-activation-plan-fix1-gpt-20261113-01`; current bounded fix task: `pa31-subject-activation-plan-fix2-gpt-20261113-01`; work unit: `PA31-customer-personal-subject-activation-foundation`; date: 2026-11-13; gate: **plan fix only**.
- Requested dispatch: Codex / GPT `gpt-6.1-sol` / high. This artifact is produced in the supplied Codex CLI runtime; the runtime does not expose independently verifiable model/provider metadata. Do not treat the requested dispatch name as proof of the executing model.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa31-next-work-unit/codex-call`; branch `codex/pa31-next-work-unit`; current committed fix-2 base `5f8ed0b69c2832ce98224c8222dac1410c4d05f8`; prior fix-1 base `6ddb81f0b9d7a48edeb6428468ce7332701cc69b`. The plan was authored from `6d02c9b461fa449306dde7de1e2a108873edbc75`; Control recorded intake at `35b223d4e08a2079a8fae940a345e9bd56fde6fd`, where reviews froze. These are successive bookkeeping contexts, not a plan-content mismatch or different product baseline.
- Binding precedence: confirmed user objective and role-handoff clarification; `docs/gateflow/project-execution-control.md` Active PA31 checkpoint, including Controller adjudications `PA31-PLAN-REV-01..06` and fix routing; frozen OpenAPI C5; docs/15 C5; docs/14 data/transaction/owner contracts; actual V001–V017 privileges and existing Java architecture. Earlier blocked intake 1 is not the accepted plan and is not a single-role constraint.
- This gate modifies only this cumulative plan and creates `docs/gateflow/pa31-customer-subject-activation-foundation-plan-fix-2.md`; fix-1 remains unchanged. Every implementation file named below is a **future allowlist**, not authorization to edit it at this gate. The result is ready only for fresh independent targeted A/B re-review, not implementation; no re-review or later gate starts here.

### Goal, motivation and success signal

Implement only `createCustomerSubjectActivation`, `POST /api/v1/customer/subjects/{subjectId}/activations`, over the existing persistence. Explicit current consent must activate an accessible subject and issue/reuse its one permanent FIRST-TRIAL atomically; refusals and retries must not accidentally activate or grant twice. Success is an ACCEPT-only exact 200 result, a durable replayable DECLINE 409, fail-closed conflicts/access/policy checks, and PostgreSQL16 evidence of atomicity, ownership, replay, least privilege and redaction without any frozen-artifact change.

### Goal alignment registry

Every design and test below cites these confirmed requirements; these are labels, not additional goals:

| Label | Confirmed requirement |
| --- | --- |
| G1 | Verified session, server-derived account/channel/workspace and current personal/organization subject access |
| G2 | One current enabled/published FIRST-TRIAL snapshot; exact policy/prompt/content validation, no fallback |
| G3 | ACCEPT: consent, INACTIVE→ACTIVE, exactly one revision transition and permanent trial in one transaction |
| G4 | DECLINE: immutable refusal, committed 409, no activation/trial; exact replay and semantic conflict |
| G5 | Frozen first result; later ACTIVE event has no duplicate transition/trial; no customer SUSPENDED reactivation |
| G6 | Same connection, MANDATORY owner composition, exact role assertions/restoration, frozen locks and rollback |
| G7 | Exact public shape/error set, non-disclosure and no sensitive logging/proof/account leakage |
| G8 | Frozen bytes/schema/privileges, existing module/pool/test/CI boundaries, no expanded product scope |

## 2. Scope and first-principles evidence

**Non-goals:** IVR confirmation, Operations suspend/resume, purchases/orders/catalog, real-name prerequisites, claim/risk changes, owner transfer, UI, trial-policy management, provider/KMS implementation, production credentials/provisioning, live acceptance, generic workflow/idempotency/event/canonicalization frameworks, dependency additions, migrations/schema/ACL changes, OpenAPI/docs14/docs15 edits, and all remote/Git/later-gate actions listed in the dispatch. Organization **access** is required; organization **ownership acquisition** is not. Trial expiry/exhaustion never creates another eligibility gate or a replacement trial. [G1–G8]

### Direct evidence and exact gaps

| Evidence in current tree | Consequence / missing implementation |
| --- | --- |
| `api/openapi/onecall-v1.openapi.yaml:528` and `:4572`; docs/15 §C5, error catalog and authorization summary | Route, six closed request fields, ACCEPT-only result, nine operation error codes are frozen. No header idempotency identity or new public conflict code is available. |
| docs/14 §§2.5, 2.9–2.12, 4.29–4.38, 7.3, 9.0.1, 9.25, 10.12, 10.17, 10.20 and T-S2 assertions | Canonical semantic sets, permanent trial, append-only consent/transition, source/state matrix, same-connection services and total lock order already exist. Broad DB transition checks do not authorize customer resume. |
| `activation_consent/customerquery/CustomerActivationStatusReader.java`, `internal/customerquery/JooqCustomerActivationStatusReader.java`, `CustomerActivationQueryConfiguration.java` | Activation has a read seam only; it cannot implement this command or authorize writes. Leave these files unchanged. |
| `entitlement_metering/ModuleMarker.java`, `package-info.java`; no entitlement application service | Add one narrow published owner interface and its same-connection implementation; no entitlement pool or generic entitlement API. |
| V004 schema grants (including entitlement schema USAGE to activation), V006 `issue_first_trial` at lines 153–360 and EXECUTE at 664–665, V007 tables/guards and access column grants | Activation can write only its own three tables and call existing service functions. USAGE/EXECUTE is not foreign table SELECT. Entitlement role already owns policy/trial/grant/balance read capability. No privilege defect requires a migration. |
| V010–V017 later privilege-only migrations | They do not add the missing entitlement reads to activation; preserve their exact ACL oracles. V014–V017 customer/query hardening is not permission to reuse query credentials for commands. |
| `sharing/internal/persistence/JooqSharingCommandRepository.java:31` role helper and creator lifecycle locks | Existing controlled `SET LOCAL ROLE` → `current_user` assertion → owner handoff → restoration is usable evidence, not authorization to call sharing internals. |
| `subject_number/internal/claim/{SubjectClaimPersistenceConfiguration,SubjectClaimPersistenceProperties,DefaultCustomerSubjectClaimService,JooqSubjectClaimRepository}.java` | Dedicated opt-in Hikari/transaction-aware jOOQ, bounded whole-transaction retries and SQLSTATE allowlisting are established patterns. Do not copy claim provider/proof/risk behavior. |
| `customer_iam/internal/{application/CustomerSubjectClaimCommandService,web/CustomerSubjectClaimsController,web/CustomerSubjectClaimJsonBinder,web/CustomerBoundaryFilter,web/CustomerProblemWriter,web/CustomerWebConfiguration}.java` | Reuse trust/binder/problem composition patterns. Filter currently does **not** admit this POST; its claim/revoke header idempotency branch must not absorb activation. Problem writer lacks activation domain mapping. |
| Module `package-info.java` files and `archtests/architecture/ModuleBoundaryTests.java` exact dependency map | Add only two named-interface edges described in §6; maintain the 14-module set and existing test identities. |
| `archtests/container/{CustomerSubjectClaimFixture,SubjectClaimRoleLeastPrivilegeIT}.java` | Synthetic NOINHERIT login, explicit SET memberships, role/transaction probes, single-slot pool reuse and isolated cleanup already have local examples. |
| `backend/pom.xml`, `.github/workflows/ci.yml:1383`, `backend/README.md` PA30 current section | Current source baseline is 54/468 unit + 23/370 IT = 77/838, 839 `@Test\b` lines including one retained historical comment, 23 Failsafe includes. Add exact identities, not count-only evidence. README must describe PA31 truth rather than leave PA30 as the latest boundary. |

Read the actual client-activation section (§9.25), not an assumed numbering from an older checkpoint. The OpenAPI operation error set wins over internal docs14 examples such as `IVR_ARGUMENT_INVALID`/trial function exception strings. Existing real-name/risk tables must neither be queried nor changed by this slice. [G7,G8]

## 3. Exact HTTP contract and precedence

### Input and trust [G1,G2,G7]

1. Extend only exact POST route admission for `/api/v1/customer/subjects/[^/;%]+/activations`, retaining raw-URI/context equality and encoded-separator protections. Wrong method/path remains existing 404 behavior. Authenticate through the existing verifier before content/body/subject validation. No fake verifier/profile/default credential is added.
2. Require a trusted non-null account, channel MINIAPP or WEB, and selected workspace. PERSONAL must have null organization; ORGANIZATION must have a non-null organization. Structurally missing/inconsistent context is 503 SERVICE_UNAVAILABLE. Context must come only from `TrustedCustomerCaller`, never body, query parameters or caller-selected role headers.
3. Add activation to JSON Content-Type validation, **not** the Idempotency-Key-required branch. The body event is the sole idempotency identity; presence/absence of an unrelated header cannot replace it. Preserve existing Accept/problem behavior and no-store/security headers.
4. Binder accepts exactly six required string keys: `activationEventId` nonblank 1–128 Unicode code points; `decision` exactly ACCEPT or DECLINE; `policyCode`, `policyVersion`, `promptVersion` nonblank 1–64 code points; `contentSha256` exactly lowercase `[0-9a-f]{64}`. Reject nulls, numbers/coercions, unknown/missing/duplicate keys, arrays, malformed UTF-8, lone surrogates, NUL, trailing JSON and oversized strings. Preserve exact strings; do not trim/case-fold semantic input. Strict 36-character UUID path syntax, parsed to canonical lowercase identity. Invalid input is 422 VALIDATION_FAILED. Context structure validation precedes binding, following established customer command composition; database lifecycle/access is rechecked after binding under locks.

### Ordered domain decisions [G1–G7]

| Order | Condition | Result and effects |
| --- | --- | --- |
| 1 | Authentication absent/invalid | 401 UNAUTHENTICATED, bare Bearer challenge; no pool/effect |
| 2 | Malformed trusted context / unavailable command graph | 503 SERVICE_UNAVAILABLE; no effect; validate request before resolving the owner pool where possible |
| 3 | Invalid route UUID/body/content type | 422 VALIDATION_FAILED after authentication/context structure; no effect |
| 4 | Locked reread finds missing/inactive account; inactive/missing organization, missing/inactive membership, or disallowed membership role | 403 FORBIDDEN, even if target is missing/foreign; no subject-existence disclosure |
| 5 | Lifecycle valid, subject absent or not owned by selected personal account/organization | 404 NOT_FOUND; no effect; an inconsistent both-owner subject is integrity failure, not an access grant |
| 6 | Existing ACTIVATION event belongs to other semantics/account/subject/channel or differs in stored content identity | 409 CONSENT_REQUIRED; no returned stored IDs, no writes; do not expose collision details |
| 7 | Exact durable ACCEPT or DECLINE replay with currently valid access | Return first frozen 200 ACCEPT result or same 409 ACTIVATION_CONSENT_DECLINED; zero domain/Outbox/audit appends; replay is before fresh current-policy/state checks |
| 8 | New event: missing/disabled/unpublished/stale/malformed/mismatched policy/prompt/content | 409 CONSENT_REQUIRED; zero consent/head/transition/trial/grant/balance/Outbox/audit writes |
| 9 | New event, valid policy, DECLINE | Append only one replayable SERVICE_ACTIVATION/DECLINE consent; commit, then map to 409 ACTIVATION_CONSENT_DECLINED. Never throw the refusal inside the transaction to roll it back. No activation head, transition, trial, grant, balance, Outbox or audit write; existing activation/trial rows are untouched. |
| 10 | New event, valid policy, ACCEPT, current head SUSPENDED | 403 FORBIDDEN, zero consent/head/transition/trial/grant/balance/Outbox/audit writes; never use global-catalog `ACTIVATION_STATE_CONFLICT`, which C5 does not permit |
| 11 | New ACCEPT, absent/INACTIVE head | One consent; absent head starts INACTIVE/revision 1 then conditionally becomes ACTIVE/revision 2; existing INACTIVE revision r becomes ACTIVE/r+1. One transition and first-trial call; all effects commit atomically. |
| 12 | New ACCEPT, head ACTIVE with valid latest transition and permanent trial | One new ACCEPT consent, no head/revision/transition change; reference the latest valid ACTIVE transition anchor; existing function returns ALREADY_GRANTED (or valid REPLAY), with original trial units/expiry. Missing/inconsistent anchor/permanent trial is INTERNAL, not permission to repair or issue another trial. |

Exact replay is a historical response, not a current-state projection: after later Operations suspension/resume or policy replacement, a currently authorized exact replay returns its originally frozen ACTIVE revision/anchor, without reactivation or any write. A legitimate **new** event on ACTIVE uses the latest valid anchor at that event, including an Operations resume anchor; it does not require that anchor's consent to equal the new consent. A legitimate new ACCEPT on SUSPENDED returns 403 FORBIDDEN with zero writes, per the user-confirmed boundary and Controller REV-01; docs/15's resource/access-predicate FORBIDDEN channel already exists. Preserve ordered precedence: exact historical replay is row 7, fresh policy rejection row 8, valid-policy DECLINE row 9, and valid-policy SUSPENDED ACCEPT row 10. DECLINE records only a refusal irrespective of the head state, never a suspension denial, but only after fresh policy validation. The problem writer maps the SUSPENDED ACCEPT failure to FORBIDDEN/403, not CONSENT_REQUIRED/409. [G4,G5,G7]

200 shape is exactly `activationStatus` (always ACTIVE here), positive `activationRevision`, UUID `transitionId`, UUID `consentId`, `trialOutcome` in GRANTED/REPLAY/ALREADY_GRANTED, and `trial` with positive integer `grantedSeconds` and stored UTC date-time `expiresAt`. No extra wrapper, subject/account/workspace/policy/trial IDs, fingerprints or balances. Error set is exactly UNAUTHENTICATED/401, FORBIDDEN/403, NOT_FOUND/404, VALIDATION_FAILED/422, CONSENT_REQUIRED/409, ACTIVATION_CONSENT_DECLINED/409, RATE_LIMITED/429, SERVICE_UNAVAILABLE/503, INTERNAL/500. Rate-limit mapping remains available without inventing a new rate-limiter/provider. Unknown SQL/integrity detail never escapes Problem titles/codes. [G7,G8]

## 4. Policy, canonical identities and durable replay

### One policy source and exact parser [G2,G6,G8]

`ActivationTrialService.readCurrentPolicy()` reads only `entitlement_metering.trial_policy_version WHERE campaign_key='FIRST-TRIAL' AND enabled AND published_at IS NOT NULL` under the entitlement role on the activation connection. Select `id,campaign_key,version_no,granted_seconds,valid_days,enabled,published_at,snapshot,snapshot_sha256`; one row is required. Zero rows or invalid consent metadata yields a typed invalid-policy outcome mapped to CONSENT_REQUIRED; multiple rows are INTERNAL corruption. No latest-by-date, draft, environment, fixture, code constant, catalog policy or old-grant fallback is allowed.

The existing immutable JSONB `snapshot` must be an object with **top-level string** `policyCode`, `promptVersion`, `contentSha256`. Code/prompt obey nonblank 1–64 constraints and content obeys lowercase64-hex. Policy version identity is exactly the positive `version_no` in plain base-10 text (`Integer.toString(versionNo)`, no prefix/leading zero/sign/UUID alias); request `policyVersion` must equal it. If snapshot also contains `policyVersion`, it must be a string equal to that identity; an explicitly present null/wrong type/mismatched value invalidates the snapshot. Other snapshot policy facts may remain present; they never supply missing required metadata or become public output. Frozen docs14/V006 define a generic JSON object, not these member paths, and no publisher exists yet. Controller REV-05 explicitly retains this bounded fail-closed application parser contract over the user-confirmed policy source; a later publisher must conform. It does not authorize a publisher, table, seed, fallback source or management feature here. Missing/mismatched metadata is runtime CONSENT_REQUIRED, not a blocking parser decision. `snapshot_sha256` is a stored snapshot digest and must be well-formed; it is **not** the content digest. Do not substitute it for `contentSha256`, invent raw prompt text, or hash the whole snapshot to guess missing content identity. No policy snapshot is returned/logged. [G2,G7,G8]

Compare all four request policy/prompt/content values exactly against these facts, for ACCEPT and DECLINE. Invalid policy metadata is a consent refusal rather than technical fallback. DB/role/connection failures are SERVICE_UNAVAILABLE, not a missing-policy success/refusal shortcut. `issue_first_trial` performs the authoritative fresh-issuance enabled/published/expected-ID read: wrapper lookup is preliminary, with no publication advisory or second enabled recheck. If disable commits before the function's selection, TRIAL_POLICY_NOT_FOUND causes whole rollback and maps CONSENT_REQUIRED; after selection it does not retroactively revoke the grant. Already-granted/replay return stored facts before a new function policy selection, as frozen docs14 §4.29 requires. A later ACTIVE event still validates current consent metadata at its preliminary owner read. [G2,G6]

### Exact hashes and replay identity [G3–G7]

Use lowercase SHA256 over §2.5 canonical UTF-8 JSON. Implement a **bounded activation-local serializer** for the known string/null/integer/object values used here: UTF-16 key ordering, exact JSON escaping, no whitespace, canonical lowercase UUIDs, plain exact integers within supported safe range; no float/general-purpose JSON-number framework or dependency. Inputs contain no fractional numbers. Do not copy policy JSON into this serializer or try to canonicalize arbitrary policy extension values. Unit vectors cover astral keys/characters, control escaping, Unicode, nulls and insertion-order independence. Snapshot parsing is separate from bounded command canonicalization. [G8]

- `sourceRef = 'activation:' + sha256(canonical({accountId,activationEventId}))` (75 characters); sourceType ACCOUNT, purpose SERVICE_ACTIVATION, eventDomain ACTIVATION, source account from trusted session. Consent `event_id` is the exact body event (global unique `(event_domain,event_id)`, not account-scoped).
- Consent fingerprint semantic set remains **exactly** docs14 §10.17: `{accountId,action,channel,eventDomain,policyCode,policyVersion,promptVersion,purpose,sourceRef,sourceType,subjectId}`. Never add generated consent/transition IDs, workspace role, timestamps, status or revision. The sourceRef already binds account/event.
- Content is additionally compared against immutable consent snapshot `contentSha256` on replay. It is intentionally not silently added to the frozen fingerprint registry: semantic equality for C5 is **stored canonical consent fingerprint plus exact stored content identity**. This detects a changed content field under an unchanged policy/prompt even before fresh policy lookup.
- Trial granting fingerprint: exact `{accountId,activationEventId,channel,consentRequestFingerprint,policyVersionId,sourceActionDomain,subjectId}`, with sourceActionDomain ACTIVATION and selected current policy UUID. Use original stored policyVersionId when verifying an old event, never today's policy.
- Transition fingerprint: exact `{accountId,actorEmployeeId:null,channel,consentRequestFingerprint,reasonCode,sourceActionDomain,sourceActionEventId,statusEventDomain,statusEventId,subjectId,toStatus}`; reasonCode `CLIENT_ACTIVATION_ACCEPTED`; statusEventDomain ACTIVATION_STATUS; statusEventId `activation-status:<canonical-activation-uuid>:<decimal-toRevision>`. Source action ID is unchanged body event. The reason is an internal bounded reason, not a new public code/state.

### Freeze the first result without a new result table [G3–G6,G8]

Current rows alone cannot always reconstruct the event-time result: a later legitimate ACCEPT on an already-ACTIVE subject creates a consent but no transition for that event; subsequent Operations transitions change the latest head/anchor, and subsequent policy publication changes the current policy identity. Original trial rows preserve grant units/expiry and the original granting action, not every later ACCEPT's event-time anchor/action-policy/outcome. Neither today's latest transition nor today's policy is a valid substitute. Controller REV-04 therefore retains the bounded descriptor. The existing immutable consent snapshot is the only available frozen carrier written once for **each** ACCEPT or DECLINE event without adding a schema/table or changing frozen fingerprint sets. This is activation-local application data in the existing object column, not a new public response or general result store. [G4,G5,G8]

Use that snapshot for one small, versioned redacted replay descriptor. Exact six root keys: `schemaVersion` integer 1, `policyCode`, `policyVersion`, `promptVersion`, `contentSha256`, `activationResult`. The four policy/content strings obey the request/parser constraints and match the consent policy columns and event's content identity. Fresh DECLINE stores this exact minimal descriptor with `activationResult` JSON null; no ACCEPT result or trial/anchor fields may appear. Fresh ACCEPT stores the same root keys with `activationResult` an object of exactly five keys: `activationStatus` string ACTIVE, `activationRevision` positive exact integer, `transitionId` canonical UUID string, `trialOutcome` string GRANTED/REPLAY/ALREADY_GRANTED, `trialPolicyVersionId` canonical UUID string. The last is the validated policy ID **for this action**, internal only. Both descriptors use `snapshot_sha256 = lowercaseSha256(canonicalUtf8(descriptor))` with the bounded serializer above, not raw JSONB text, the content digest or the consent request fingerprint. Recompute this hash on replay. Row `account_id`, `subject_id`, `channel`, `source_ref`, purpose/action/event/policy columns and canonical fingerprint remain authoritative and must agree with the descriptor and semantic inputs. Unknown/missing keys, wrong types, invalid enums/UUIDs/revision or digest mismatch are persisted integrity failures, not missing current policy.

Synthetic snapshot hash vectors (test-only facts, not a production policy seed): both use `policyCode="FIRST-TRIAL"`, `policyVersion="1"`, `promptVersion="1"`, `contentSha256` = 64 lowercase `a` characters and integer `schemaVersion=1`. DECLINE with `activationResult=null` hashes to `c3ce0a81bbe9da0c148e6ab01d9d5cb7ddeca47cb4c0218475ac012423bddc65`. ACCEPT with `activationResult={activationStatus:"ACTIVE",activationRevision:2,transitionId:"11111111-1111-1111-1111-111111111111",trialOutcome:"GRANTED",trialPolicyVersionId:"22222222-2222-2222-2222-222222222222"}` hashes to `9673ed6bb19b601880627c428ee20d5b66cf7ad119a0fcf1a55d5f92d3709a22`. Canonical root order is activationResult,contentSha256,policyCode,policyVersion,promptVersion,schemaVersion; nested order is activationRevision,activationStatus,transitionId,trialOutcome,trialPolicyVersionId. Unit vector method 15 and descriptor replay/IT matrices must use these exact digests; insertion-order or JSONB textual formatting changes must not change the digest.

Before the immutable consent insert, the held activation lock determines the new transition's allocated UUID/revision, or the latest existing valid ACTIVE anchor. The entitlement owner reads any permanent original trial on this same connection: none predicts GRANTED for a fresh activation; a trial from another action predicts ALREADY_GRANTED. For an ACTIVE head, no permanent trial is corruption. A permanent trial claiming this same ACTIVATION event but no corresponding durable consent is also corruption; do not invent a partial-write recovery path. Freeze that predicted outcome in the descriptor, then require the actual function outcome/identity to agree before commit. Subject serialization makes this deterministic. For an existing original action, exact consent replay takes the replay branch and does not issue again. No historical units/expiry are predicted or stored from current policy.

After function issuance the entitlement owner loads actual trial/grant/balance rows by returned IDs and returns their stored facts. On replay it loads the unique original trial by subject with matching grant/balance and validates the frozen action-policy/fingerprint for an original granting event. Reconstruct public trial units/expiry only from immutable `entitlement_grant.granted_units` and `trial_grant.expires_at`, cross-checked against grant expiry and original balance granted units. Snapshot `trialOutcome` preserves first GRANTED rather than changing it to the function's later REPLAY; validate outcome against original source domain/event and fingerprint. For ALREADY_GRANTED, preserve the other action's original trial, not the action's current policy units/expiry. Expired/exhausted/consumed balances do not invalidate original immutable facts. Missing/inconsistent immutable facts are INTERNAL, never zero/default/current-policy values. [G5,G7]

On replay find consent by `(ACTIVATION,eventId)` and transition by `(ACTIVATION,eventId)` **before any append**. For an accessible matching event validate the correct ACCEPT or DECLINE descriptor, exact key sets/types, policy-column agreement and recomputed snapshot hash. A DECLINE must have the legitimate six-key descriptor with `activationResult=null` and no corresponding transition; reject an ACCEPT result inside DECLINE, **not** the legitimate DECLINE descriptor itself. Its replay returns the same committed refusal without trial issuance/reads or head/result reconstruction, regardless of later state/policy. Fresh-transition ACCEPT must have the matching consent/transition/head identity, revision chain and non-null result descriptor; later-ACTIVE ACCEPT legitimately has no transition for its own event and references the validated older anchor. Validate that anchor's immutable subject/activation identity, ACTIVE destination and revision against the descriptor, not equality with today's latest anchor. Current head may have a later revision, but never an earlier revision than the historical anchor. Missing/malformed/hash-inconsistent persisted descriptors or inconsistent links fail INTERNAL with no repair; legitimate descriptor presence alone is never corruption. A valid stored descriptor with different incoming semantic/content identity maps CONSENT_REQUIRED; a global event collision on another subject/account also fails CONSENT_REQUIRED without reading that account's policy/proof or returning IDs. [G4,G5,G7]

## 5. Transaction, locks, owner handoff and failures

### Pool and owner service composition [G6,G8]

Add one dedicated activation command pool, opt-in only with profile `persistence` and `onecall.activation-command.enabled=true`; HTTP also requires `web`. Properties `onecall.activation-command.datasource.url|username|password`, with explicit environment mappings `ONECALL_ACTIVATION_COMMAND_DB_URL|DB_USERNAME|DB_PASSWORD` and `ONECALL_ACTIVATION_COMMAND_ENABLED` (false default). Require all credentials; no fallback to customer/sharing/query/admin defaults. Redact property `toString`, pool initialization exceptions and SQL logging. Use qualified beans `activationCommandDataSource`, `activationCommandTransactionManager`, `activationCommandTransactionTemplate`, `activationCommandDsl`; same TransactionAwareDataSourceProxy, prepared/indexed jOOQ, execute logging off, rollback-on-commit-failure. UTC session/connection timezone is required for the frozen V006 interval calculation; set fixed connection initialization SQL `SET TIME ZONE 'UTC'` and prove it in fixtures. No network I/O besides transactional PostgreSQL access occurs while locks are held. [G3,G6]

Outer `DefaultCustomerSubjectActivationService.activate(command,caller)` owns at most three READ_COMMITTED, writable REQUIRES_NEW whole attempts. Its `recordClientActivation` owner operation executes through a MANDATORY TransactionTemplate over the **same manager**; it never begins/commits independently. The entitlement implementation constructs its own MANDATORY template from qualified `activationCommandTransactionManager` and uses qualified `activationCommandDsl`, not a second pool/transaction. Its interface exposes no DSLContext, JDBC Connection, role name or raw policy JSON. Programmatic `PROPAGATION_MANDATORY` must reject an out-of-transaction call; do not rely on self-invoked annotations. Both owner implementations assert actual transaction binding and that the expected datasource resource is bound. [G6]

Role sequence, with literal identifiers only:

1. At every attempt entry: `SET LOCAL ROLE activation_consent_role`; `SELECT current_user` must equal `activation_consent_role` before any access read/lock/local write.
2. Each entitlement owner method enters a controlled scope: assert entry role activation_consent_role, execute `SET LOCAL ROLE entitlement_metering_role`, assert exact `current_user`, then perform only entitlement-owned reads/function invocation. The service must not accept a caller-supplied role or tolerate ambient unexpected roles.
3. In a finally scope, restore `SET LOCAL ROLE activation_consent_role` and assert exact `current_user` **before returning facts and before any further activation write/Outbox/completion**. Assert activation role again before successful owner return/commit. No `RESET ROLE` to an accidentally privileged login, session-wide role change or role interpolation.
4. If PostgreSQL has aborted the transaction, restoration SQL can itself fail (25P02). Preserve the primary exception, attempt restoration without masking it, and roll back the entire attempt; never continue or commit. SET LOCAL unwinds on rollback; test subsequent checkout's `current_user=session_user` and next attempt's fresh role assertion. Restoration failure on an otherwise healthy transaction is SERVICE_UNAVAILABLE and forces rollback. No owner independently commits/rolls back or uses savepoints to keep partial activation effects.

Deployment must externally supply a safe dedicated LOGIN/NOINHERIT/non-owner identity with only two SET membership edges to `activation_consent_role` and `entitlement_metering_role` (`ADMIN FALSE, INHERIT FALSE, SET TRUE`). This slice adds neither production provisioning nor a membership migration/runbook expansion. Existing PA25 provisioning remains unchanged and does not magically provision the activation login. The application trust boundary can assume either approved owner role; role separation is not a sandbox against arbitrary compromised application SQL. [G6,G8]

### Locks, rereads and local write sequence [G1,G3,G6]

Acquire `pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))` with canonical keys in this order: (3) `subject_number.subject:<subjectId>`; (4) existing activation row `FOR UPDATE` under activation role (missing row is covered by rank3); (5) nonlocking subject owner read; (7) organization advisory `organization_access.organization:<orgId>` if selected; (8) membership pair `organization_access.membership:<orgId>:<accountId>` if selected; (9) `customer_iam.account:<accountId>`. After all lifecycle locks reread subject owner/revision, account status and organization/member status/role before any replay/policy/local effect. Never foreign `FOR UPDATE`, owner transfer or subject/account/org DML. Personal access must match owner_account_id with null owner_organization_id; organization access must match owner_organization_id with null owner_account_id plus ACTIVE account/org/membership and one of ADMINISTRATOR/MEMBER/EVIDENCE_MANAGER/AUDITOR. No rank1/2/6/10/task/reservation key or generic idempotency row is needed. [G1,G8]

After replay and current-policy validation, DECLINE inserts only the refusal consent with its null-result descriptor and returns a committed typed result; it never calls audit or Outbox. For ACCEPT first reject SUSPENDED as FORBIDDEN/403 with zero writes. Otherwise read immutable original-trial facts under entitlement role and restore; allocate/validate descriptor; append consent under activation role; create INACTIVE head if absent; conditionally update status/revision/time with exact expected revision/status (one bounded reread on zero rows, then fail closed rather than invent an alternative transition); append one transition only for INACTIVE→ACTIVE; call entitlement owner `issueFirstTrial` using campaign FIRST-TRIAL/current policy ID/same event/canonical fingerprint, restore/assert activation role; construct exact result; append activation Outbox if a transition was created (rank14); final role assertion and commit. Existing ACTIVE uses its latest transition and never updates the head. No rank15 audit append is introduced. [G3–G6]

V006 re-enters the already-held subject advisory; it is not a newly acquired lower-ranked lock. It emits the trial Outbox itself only on GRANTED; do not duplicate it. Activation owner emits only the frozen §10.17 ACTIVATION_STATUS fact, via existing `platform.outbox_append`: dedupe `activation_transition:<transitionId>`, aggregate SERVICE_ACTIVATION/id, event ACTIVATION_TRANSITION_APPLIED, exact frozen payload keys `activationId,transitionId,subjectId,fromStatus,toStatus,fromRevision,toRevision,sourceChannel,sourceActionDomain,sourceActionEventId,statusEventDomain,statusEventId,occurredAt`, hash of canonical payload, null encrypted payload. Both rows belong to rank14; issue-first-trial Outbox precedes activation Outbox. ACTIVE-with-no-transition and DECLINE emit no new activation Outbox. [G3,G6,G8]

**Audit decision (Controller REV-03):** remove the decision audit for both ACCEPT and DECLINE; do not call `audit.append_fact`, add direct audit DML, invent action/reason/snapshot registries, or treat the absence of a frozen action registry as permission to extend it. DECLINE's complete durable write set is the one refusal consent. Audit unavailability cannot affect that refusal because its path never invokes audit. For ACCEPT, docs14 §9.25 step 6's Outbox/audit evidence is fulfilled by the exact frozen transition Outbox and V006 trial Outbox when their respective facts are created, atomically with the consent/head/transition/trial; no separate decision audit is required under this adjudication. Later-ACTIVE ACCEPT creates neither fact again and relies on the existing immutable transition/trial evidence plus its own frozen consent. Assert exact deltas: fresh INACTIVE→ACTIVE with new trial = one transition Outbox plus one trial Outbox; INACTIVE→ACTIVE with an existing permanent trial = one transition Outbox only; later-ACTIVE ACCEPT, DECLINE and exact replays = zero new Outbox; **every** PA31 branch = zero new `audit.audit_fact` rows. Outbox failure on ACCEPT rolls back its whole transaction; DECLINE has only consent/commit failure coupling. [G3–G8]

### Failure and retry table [G4,G6,G7]

- Return a typed `ActivationDecision` (Accepted result or Declined) from the transaction; map Declined to the domain 409 **after commit**. Missing/stale policy refusals have no writes. A post-write TRIAL_POLICY_NOT_FOUND is caught outside the rolled-back attempt and becomes CONSENT_REQUIRED.
- Retry only 40001, 40P01, or 23505 on named serialization keys `uq_consent_event`, `uq_service_activation_subject`, `uq_activation_transition_action`, `uq_activation_transition_revision`, `uq_activation_transition_status_event`, `uq_trial_grant_subject`, `uq_trial_grant_source`. Roll back before rereading; no aborted-transaction replay query. Maximum three attempts, then SERVICE_UNAVAILABLE. No provider/network retry under locks; no unbounded backoff/framework.
- Global event collision across different subjects can reach the unique consent constraint without sharing rank3. After losing and restarting, the committed consent is detected by event before any append and maps CONSENT_REQUIRED; same-subject same-event callers serialize at rank3 and converge on one frozen result.
- Explicit TRIAL_ACTION_CONFLICT maps CONSENT_REQUIRED after full rollback; TRIAL_POLICY_NOT_FOUND maps CONSENT_REQUIRED; TRIAL_DATA_INTEGRITY, argument/campaign exceptions from a validated internal call, unknown constraint/P0001/invariant failure map INTERNAL. Missing privilege/role/pool/connection/timeout maps SERVICE_UNAVAILABLE. Inspect bounded SQLSTATE/constraint/function exception allowlists internally, never publish SQL, schema, constraint, account, provider or fingerprint detail. Unknown failures map INTERNAL. Commit failure is not success; a later retry resolves ambiguous commit through the durable event, not compensation.

## 6. One implementation slice and exact file boundary

**S1 — complete authenticated customer activation command** is the only slice. ACCEPT, DECLINE and replay are one state machine; HTTP, owner handoff and permanent-trial atomicity cannot be independently accepted as a module/file split. No extra slice has independent confirmed behavior. Ordered coding tasks below are checkpoints inside S1, not separate deliverables/gates. [G1–G8]

### New production files (17; all paths relative to `backend/src/main/java/com/onecall/core/`)

| Exact path | Required functions/types and alignment |
| --- | --- |
| `activation_consent/customeractivation/package-info.java` | NamedInterface `customer-subject-activation-command` [G8] |
| `activation_consent/customeractivation/CustomerSubjectActivationService.java` | `activate(ActivationCommand,ActivationCaller)`; nested Decision, Channel, WorkspaceType enums, caller(account/channel/workspace/org), command(subject/event/decision/policy/prompt/content/trace), ActivationResult and ActivationTrial records; redacted command/caller `toString` [G1,G3–G7] |
| `activation_consent/customeractivation/SubjectActivationException.java` | Only the nine C5 codes, fixed safe exception messages [G7] |
| `activation_consent/internal/command/DefaultCustomerSubjectActivationService.java` | `activate`, MANDATORY `recordClientActivation`, ordered access/replay/fresh-decision branches, descriptor validation, typed commit outcome, retry classifier [G1–G7] |
| `activation_consent/internal/command/SubjectActivationRepository.java` | Owner-local row records; role assertion, advisory/activation locks, exact cross-column rereads, consent/event/anchor reads, conditional head/transition inserts, exact transition Outbox only, no audit; no entitlement SQL [G1,G3–G6] |
| `activation_consent/internal/command/JooqSubjectActivationRepository.java` | Prepared SQL implementation of that repository, schema-qualified identifiers and exact allowed columns, no foreign row lock/DML [G6,G8] |
| `activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java` | Four qualified pool/manager/template/DSL beans, repository and owner service beans; opt-in profile/property graph and no default pool [G6,G8] |
| `activation_consent/internal/command/ActivationCommandPersistenceProperties.java` | Explicit nonblank URL/username/password, redacted string [G6,G7] |
| `activation_consent/internal/command/ActivationSemantics.java` | Bounded canonical JSON/SHA, exact consent/trial/transition fingerprints, sourceRef/status identity, descriptor encode/decode/hash; no generic public utility [G3–G8] |
| `entitlement_metering/activationtrial/package-info.java` | NamedInterface `activation-trial` [G6,G8] |
| `entitlement_metering/activationtrial/ActivationTrialService.java` | `readCurrentPolicy()`, `readOriginalTrial(UUID subjectId)`, `issueFirstTrial(TrialIssueCommand)`; immutable records described below, typed invalid-policy/integrity/unavailable/conflict failures; no owner-internal imports [G2,G3,G5,G6] |
| `entitlement_metering/internal/activation/DefaultActivationTrialService.java` | Conditional/profiled component bean `activationTrialService`; constructor qualified same manager/DSL; MANDATORY scopes; literal role switch/assert/finally restoration; policy/trial/grant/balance SQL and existing function only [G2,G3,G5,G6] |
| `entitlement_metering/internal/activation/ActivationTrialPolicySnapshot.java` | Strict current-snapshot metadata parser under §4; no seeding/publication/fallback/general canonicalizer [G2,G7,G8] |
| `customer_iam/internal/application/CustomerSubjectActivationCommandService.java` | Validate trusted caller structure; translate to published owner caller/command; qualified lazy owner lookup and safe unavailable mapping, no SQL [G1,G7,G8] |
| `customer_iam/internal/web/CustomerSubjectActivationsController.java` | Exact POST and ACCEPT-only DTO; authentication/context before binder; UUID parsing and domain/fallback problem handlers [G1,G7] |
| `customer_iam/internal/web/CustomerSubjectActivationJsonBinder.java` | Strict closed six-field request parser, no client context/result fields [G1,G2,G7] |
| `customer_iam/internal/web/CustomerSubjectActivationDtos.java` | Exact six-field request and six-field success plus two-field trial, no descriptor/internal metadata serialization [G7] |

Entitlement interface facts: `CurrentPolicy(id,versionNo,policyCode,policyVersion,promptVersion,contentSha256,snapshotSha256)` is returned as a valid/invalid typed policy result, not Optional-with-defaults. `OriginalTrial(trialGrantId,entitlementGrantId,balanceId,subjectId,trialPolicyVersionId,sourceActionDomain,sourceActionEventId,requestFingerprint,grantedSeconds,grantedAt,expiresAt)` is optional only when **no** permanent trial exists; its implementation validates all V006 identity/unit/time joins and balance presence without interpreting mutable availability. `TrialIssueCommand(subjectId,policyVersionId,activationEventId,requestFingerprint)` has fixed FIRST-TRIAL/ACTIVATION, not caller-configurable domains. `TrialIssueResult(outcome,originalTrial)` always carries complete stored facts. Failure kinds distinguish invalid policy, action conflict, integrity and unavailable without importing activation internals; activation maps them to C5 codes. Owner facts/failure `toString` must be redacted. [G2,G5–G8]

### Existing files to update (15 only)

1. `backend/src/main/java/com/onecall/core/activation_consent/package-info.java`: retain subject_number, add only `entitlement_metering::activation-trial`.
2. `backend/src/main/java/com/onecall/core/customer_iam/package-info.java`: retain every existing edge, add only `activation_consent::customer-subject-activation-command`.
3. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`: exact POST route and JSON content handling, no activation Idempotency-Key requirement.
4. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerProblemWriter.java`: overload for activation code enum, exact status/fixed safe titles via existing generic writer; explicitly map new valid ACCEPT on SUSPENDED to FORBIDDEN/403, policy/semantic mismatch to CONSENT_REQUIRED/409 and committed DECLINE to ACTIVATION_CONSENT_DECLINED/409; no expanded operation codes.
5. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerWebConfiguration.java`: lazy qualified `customerSubjectActivationCommandService` composition bean only.
6. `backend/src/main/resources/application-persistence.yml`: explicit opt-in activation property/env block, no credential defaults or changes to existing blocks.
7. `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`: assertion-only exact dependency-map expectations for the two new named edges; customer dependency `hasSize(11)` becomes `hasSize(12)` at every applicable assertion, all exact customer sets add `activation_consent::customer-subject-activation-command`, and activation adds only `entitlement_metering::activation-trial`. In `discoveredProductionEdgesStayInsideTheConfiguredMap`, update the literal flattened discovered-target multiset from six to seven: `"sharing", "evidence", "subject_number", "activation_consent", "evidence", "catalog_order"` becomes `"sharing", "evidence", "subject_number", "activation_consent", "evidence", "catalog_order", "entitlement_metering"`. Add only `entitlement_metering` for the activation-consent -> entitlement-metering production edge; preserve the duplicate `evidence` entries and all existing targets. Replace the exact description/message `"exactly six production edges exist: customer query owners and sharing -> evidence"` with `"exactly seven production edges exist: customer query owners, sharing -> evidence and activation_consent -> entitlement_metering"`. Preserve the method identity and all other assertions apart from the already specified dependency updates. No new test method/class or change to test-method/class counts, and no relaxed boundary; the discovered-edge assertion enumeration itself must change.
8. `backend/pom.xml`: add exactly `**/CustomerSubjectActivationsIT.java`, `**/ActivationCommandRoleLeastPrivilegeIT.java` Failsafe includes; no dependency/plugin/version change.
9. `.github/workflows/ci.yml`: exact PA31 class/method additions from §7, historical subtraction arithmetic, XML totals, two IT includes and activation packaged-web smoke; dependency/firewall inventories stay unchanged.
10. `backend/README.md`: prepend truthful PA31 current boundary/config/role/deployment/testing statement, retain PA30 as history; no production/live readiness claim.
11. `backend/src/test/java/com/onecall/archtests/architecture/CustomerBusinessNumberBoundaryTests.java`: assertion-only customer allowed-dependency `hasSize(11)` becomes `hasSize(12)`; preserve every existing method identity/count and all unrelated assertions.
12. `backend/src/test/java/com/onecall/archtests/architecture/CustomerSubjectDetailBoundaryTests.java`: assertion-only customer allowed-dependency `hasSize(11)` becomes `hasSize(12)`; preserve every existing method identity/count and all unrelated assertions.
13. `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`: assertion-only exact customer dependency set adds only `activation_consent::customer-subject-activation-command` (11→12); preserve every existing method identity/count and all unrelated assertions.
14. `backend/src/test/java/com/onecall/archtests/architecture/CustomerEvidenceBoundaryTests.java`: assertion-only exact customer dependency set adds only `activation_consent::customer-subject-activation-command` (11→12); preserve every existing method identity/count, including the historically named `namedOwnerInterfacesAndTenCustomerDependenciesAreExact`, and all unrelated assertions.
15. `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`: assertion-only internal regular-file count `hasSize(43)` becomes `hasSize(47)` for the four new customer-internal files in the production table; extend its fixed controller/profile lists with `web/CustomerSubjectActivationsController.java`, retaining every existing entry and requiring the same `@Profile("web")`/SERVLET guards. Preserve every existing method identity/count, no exclusions from the internal scan and no relaxed source/import/SQL prohibitions.

After separate plan acceptance this is an implementation input artifact; **implementation must not edit it**. The S1 existing-file edit allowlist is precisely items 1–15, plus any separately authorized implementation report. The five added architecture paths are existing assertion updates, not new test classes/methods. Together with ModuleBoundaryTests these six existing test classes retain all their identities/counts. File arithmetic is 17 new production + 7 new test classes + 1 new fixture = 25 new implementation files; 15 existing updates = 40 implementation paths, excluding a separately authorized report. Gate records/Control updates are Controller-owned and not part of S1's code allowlist. No entitlement package dependency change is needed because its published service has no activation-module dependency. No cycles or imports of another module's internal package are approved. Keep 14 discovered modules and dependency-firewall artifacts unchanged. [G8]

### S1 coding checkpoints, invariants, errors and stop condition

1. Establish interfaces, exact parser/semantics and MANDATORY owner handoff, with isolated unit/config tests. No new pool for entitlement and no widened dependency edges.
2. Implement ordered owner transaction, replay descriptor and ACCEPT/DECLINE/state branches with exact errors from §3; add route/binder/problem composition. No product/provider/eligibility side quest.
3. Add PostgreSQL fixture/ACL/concurrency/rollback/redaction evidence, reconcile CI/POM/README and execute §8 validation after implementation authorization. The architecture checkpoint includes item 7's `ModuleBoundaryTests.discoveredProductionEdgesStayInsideTheConfiguredMap` literal six-to-seven multiset update adding only `entitlement_metering`, retaining both `evidence` entries/all existing targets, and its seven-edge description; unchanged method/class counts do not mean an unchanged six-entry literal.

S1 invariants are G1–G8 and the exact contracts in §§3–5; no relaxed acceptance alternative. **Stop** if code requires foreign SELECT for activation, a new table/column/function/grant/migration, another policy source, changed public schema/code, new activation state transition, provider work, a dependency addition, or changed historical test identities. Return precise evidence to Controller rather than implement a workaround. Normal policy absence/unusable snapshot is a specified runtime CONSENT_REQUIRED, not permission to seed/repair. Completion requires all §7 tests and §8 evidence, unchanged frozen bytes, exact allowlist and no unclassified failures; the implementer reports evidence and stops at its authorized gate without declaring production readiness. [G1–G8]

## 7. Exact test implementation and inventory

Add eight files under `backend/src/test/java/`: seven test classes plus one fixture. All new cases are ordinary, uniquely named zero-argument `@Test void` methods; matrices run inside methods without dynamic/parameterized/disabled/repeated tests. No extra `@Test` text in new comments. Preserve all 838 current methods and the one historical comment. Expected method identities follow; parenthetical coverage aligns each method to the confirmed goal, not new requirements.

### `com/onecall/archtests/CustomerSubjectActivationsWebTests.java` — 12 methods

1. `acceptUsesExactRouteAndFrozenResultShape` (G3,G7: exact six-plus-two result).
2. `activationUsesBodyEventWithoutIdempotencyHeader` (G4,G7: header absent/present never replaces body event).
3. `verifiedSessionSuppliesAccountChannelAndWorkspace` (G1: spoofed query/context ignored, body context rejected).
4. `authenticationPrecedesValidationAndPoolResolution` (G1,G7: 401 before malformed body).
5. `malformedTrustedContextFailsClosed` (G1,G6: null/inconsistent workspace, account/channel).
6. `closedBodyRejectsUnknownDuplicateMissingAndWrongTypedFields` (G2,G7).
7. `bodyRejectsMalformedUtf8TrailingJsonAndInvalidLengths` (G2,G7: 128/129, 64/65, whitespace, NUL, surrogate matrices).
8. `pathContentTypeAndDecisionValidationUseFrozenErrors` (G7: UUID, ACCEPT/DECLINE enum, lowercase digest, unsupported content).
9. `declineReturnsOnlyCommittedTypedProblem` (G4,G7: never a DECLINE 200; committed refusal even when audit is unavailable, no audit invocation).
10. `domainFailuresUseOnlyOperationErrorSet` (G7: all nine code/status pairs, explicitly SUSPENDED new valid ACCEPT→403 FORBIDDEN, no catalog-only code).
11. `encodedAndUnrelatedRoutesRemainClosed` (G7,G8: method/raw path/encoded separators).
12. `responsesProblemsAndLogsRedactInternalActivationFacts` (G7: canary account/policy/proof/fingerprint/credential values).

### `com/onecall/archtests/activation/CustomerSubjectActivationServiceTests.java` — 18 methods

1. `personalAccessRequiresActiveAccountAndMatchingOwner` (G1).
2. `organizationAccessRequiresActiveWorkspaceMembershipAndAllowedRole` (G1: all four roles, inactive/revoked/disallowed).
3. `lifecycleFailurePrecedesNonDisclosingSubjectLookup` (G1,G7).
4. `missingAndForeignSubjectsReturnNotFoundWithoutEffects` (G1,G7).
5. `currentPolicyPromptAndContentMustMatchExactly` (G2: both decisions, no consent side effects).
6. `acceptCreatesOneRevisionTransitionAndIssuesTrialAtomically` (G3: absent and existing INACTIVE).
7. `declineCommitsRefusalBeforeExceptionMapping` (G4,G6: one refusal consent only, exact six-key null-result descriptor/hash, zero audit/Outbox/invocations; audit failure cannot affect it).
8. `exactAcceptReplayReturnsFirstFrozenResultWithoutWrites` (G4,G5: exact ACCEPT descriptor/hash; original GRANTED and event-time action policy/anchor preserved).
9. `exactDeclineReplayReturnsSameRefusalWithoutWrites` (G4: accept the legitimate DECLINE descriptor with activationResult=null, validate its hash; no trial reads or issuance).
10. `sameEventSemanticOrContentMismatchReturnsConsentRequired` (G4,G7: each canonical input, content and cross-account collision).
11. `laterActiveEventFreezesLatestAnchorAndOriginalTrial` (G5: different policy/units, Operations anchor; replay this no-own-transition event after another Operations transition and policy replacement still returns its event-time descriptor, zero new Outbox/audit).
12. `suspendedRejectsNewAcceptButNeverMutatesOnHistoricalReplay` (G5: new valid ACCEPT is exactly 403 FORBIDDEN with every write delta zero; exact historical ACCEPT remains frozen 200; valid DECLINE is separately one refusal only).
13. `missingAnchorTrialOrCorruptReplayDescriptorFailsInternal` (G5,G7: missing/extra keys, wrong types, bad hash, null ACCEPT result, ACCEPT result inside DECLINE, invalid links; valid null-result DECLINE is not corruption; no repair).
14. `roleAndLockOrderIsExactBeforeRereadsAndWrites` (G6: owner entry/restore assertions).
15. `canonicalFingerprintsAndSourceIdentitiesMatchFrozenVectors` (G3–G6: exact sets/exclusions, Unicode/null/int vectors, snapshot hash).
16. `trialOutboxAndCommitFailuresRespectExactDecisionWriteSets` (G3,G4,G6: ACCEPT rollback on trial/transition Outbox or commit fault; DECLINE rollback on consent/commit fault only, never audit/Outbox failure coupling; exact branch Outbox deltas and zero audit).
17. `retryableFailuresRestartWholeAttemptAndExhaustAtThree` (G4,G6: named constraints/states only, commit ambiguity replay).
18. `unknownFailuresAndDomainStringsNeverEscapePublicProblems` (G7).

### `com/onecall/archtests/entitlement/ActivationTrialServiceTests.java` — 8 methods

1. `snapshotRequiresExactRootConsentMetadataWithoutFallback` (G2: missing/null/wrong-type metadata, nested alternatives, extension fields).
2. `policyVersionUsesSamePositiveDecimalVersionIdentity` (G2: optional snapshot version mismatch, UUID/leading-zero refusal).
3. `missingDisabledUnpublishedAndMalformedPoliciesFailClosed` (G2: invalid typed outcome, multiple rows integrity).
4. `mandatoryOwnerScopeRequiresExistingActivationTransaction` (G6: real template/proxy semantics, no self-annotation illusion).
5. `ownerSwitchesAssertsAndRestoresExactRoles` (G6: role assertion and restoration failures).
6. `freshIssueLoadsStoredTrialGrantAndBalanceByReturnedIds` (G3,G5: no policy-derived units/expiry).
7. `replayAndAlreadyGrantedReturnOriginalImmutableFacts` (G5: expired/consumed mutable balance does not rewrite history).
8. `incompleteOrInconsistentTrialFactsFailWithoutSynthesis` (G5,G7: IDs, units, times, domain/fingerprint, missing balance).

### `com/onecall/archtests/CustomerSubjectActivationConfigurationTests.java` — 8 methods

1. `disabledActivationCreatesNoPoolOrOwnerGraph` (G6,G8).
2. `enabledActivationRequiresExplicitDedicatedCredentials` (G6,G7).
3. `activationUsesQualifiedWritableReadCommittedTransaction` (G6).
4. `entitlementMandatoryTemplateUsesSameManagerAndDsl` (G6: no entitlement datasource bean).
5. `ambiguousOrMissingOwnerGraphFailsClosed` (G6,G7: no bean-selection fallback).
6. `propertiesAndInitializationFailuresRedactCredentials` (G7).
7. `webWithoutPersistenceRemainsAuthenticatedFailClosed` (G1,G6).
8. `utcInitializationAndPoolClosePreserveLifecycle` (G3,G6: lifecycle cleanup, prepared/logging settings).

### `com/onecall/archtests/architecture/CustomerSubjectActivationBoundaryTests.java` — 6 methods

1. `customerUsesOnlyPublishedActivationCommandInterface` (G8).
2. `activationUsesOnlyPublishedEntitlementOwnerInterface` (G6,G8).
3. `activationSqlNeverReadsEntitlementTablesOrLocksForeignRows` (G6,G8).
4. `ownerHandoffHasMandatorySameConnectionAndLiteralRoles` (G6,G8: no network/provider imports).
5. `openApiDataDocsAndAllSeventeenMigrationsRemainFrozen` (G8: exact 20-path raw hashes + LF hashes, migration filenames exactly V001–V017, no V018).
6. `commandSurfaceHasNoExtraEndpointPolicySourceOrStateTransition` (G2,G5,G8).

### `com/onecall/archtests/container/CustomerSubjectActivationsIT.java` — 24 methods

1. `personalAcceptCommitsExactActivationTrialAndResult` (G1,G3,G7).
2. `existingInactiveHeadAdvancesExactlyOneRevision` (G3).
3. `webAndMiniappUseTrustedChannelAndClosedRequest` (G1,G7).
4. `organizationAllAllowedRolesCanActivateAccessibleSubject` (G1).
5. `inactiveOrganizationMembershipOrDisallowedRoleIsForbidden` (G1).
6. `inactiveAccountIsForbiddenBeforeMissingSubjectDisclosure` (G1,G7).
7. `foreignPersonalAndOrganizationSubjectsAreNotFound` (G1,G7).
8. `malformedContextAndMissingGraphAreServiceUnavailable` (G1,G6).
9. `missingDisabledUnpublishedOrMalformedPolicyHasNoEffects` (G2).
10. `stalePolicyPromptOrContentHasNoConsentOrGrant` (G2).
11. `declineCommitsOneRefusalAndReplaysSameProblem` (G4: exact six-key null-result snapshot and canonical hash; one consent, zero audit/Outbox/activation/trial deltas; audit unavailable still commits refusal).
12. `acceptReplayReturnsOriginalIdsOutcomeUnitsAndExpiry` (G4,G5: exact ACCEPT descriptor/hash and links, original GRANTED preserved).
13. `sameEventSemanticMismatchHasNoAdditionalEffects` (G4).
14. `sameEventAcrossAccountsOrSubjectsFailsWithoutDisclosure` (G4,G7).
15. `laterActiveEventUsesLatestAnchorWithoutTransitionOrTrialDuplication` (G5: no own transition; replay after subsequent Operations suspend/resume and policy replacement returns the event-time anchor/action-policy/outcome, not the then-current rows).
16. `changedPolicyNeverChangesPermanentTrialUnitsOrExpiry` (G2,G5).
17. `suspendedCannotBeReactivatedByNewCustomerEvent` (G5: new valid ACCEPT→403 FORBIDDEN, zero consent/head/transition/trial/grant/balance/Outbox/audit writes; separate valid DECLINE→409 ACTIVATION_CONSENT_DECLINED with one refusal only).
18. `historicalReplayAfterPolicyChangeOrSuspensionHasNoMutation` (G4,G5: original ACCEPT frozen 200 and DECLINE same 409; valid DECLINE descriptor accepted; missing/malformed/hash-inconsistent descriptors or ACCEPT result inside DECLINE→INTERNAL with zero writes).
19. `concurrentSameEventAcceptsConvergeOnOneFrozenResult` (G3,G4,G6).
20. `concurrentDifferentEventsShareOneTransitionAndPermanentTrial` (G3,G5,G6).
21. `concurrentDeclineReplayAndSemanticConflictNeverAccept` (G4,G6).
22. `trialOutboxAndCommitFaultsRespectExactDecisionWriteSets` (G3,G4,G6: full ACCEPT rollback on consent/head/transition/trial/Outbox/commit fault, DECLINE rollback on consent/commit fault only; exact Outbox deltas per §5 and zero audit on all branches).
23. `policyDisableAroundFunctionSelectionHasFrozenLinearization` (G2,G6).
24. `storedResultsProblemsAuditOutboxAndLogsAreRedacted` (G7: descriptor and frozen Outbox key allowlists/canaries; audit table delta is exactly zero, not an expected decision audit).

### `com/onecall/archtests/container/ActivationCommandRoleLeastPrivilegeIT.java` — 10 methods

1. `syntheticLoginHasOnlyTwoNonInheritedSetMemberships` (G6,G8).
2. `activationRoleCannotReadAnyEntitlementBaseTable` (G6,G8: both table and column SELECT probes, SQLSTATE42501).
3. `activationRoleHasExistingSchemaUsageAndIssueExecuteOnly` (G6,G8: function signature and frozen PUBLIC/owner ACL).
4. `entitlementOwnerReadsCompletePolicyAndOriginalTrialFacts` (G2,G5,G6).
5. `handoffUsesOneBackendPidAndOneTransactionId` (G6).
6. `mandatoryCallsOutsideTransactionAndUnexpectedRoleAreRejected` (G6).
7. `activationRoleIsRestoredBeforeLocalWritesAndCompletion` (G6).
8. `rollbackAndPoolReuseCannotLeakEntitlementRole` (G6: one-slot pool, SQL abort/restoration failure).
9. `noForeignWritesRowLocksOrExtraPrivilegesAreNeeded` (G6,G8: account/org/subject locks forbidden, entitlement role lacks activation DML).
10. `fixtureCleanupAndFrozenAclInventoryRemainExact` (G6,G8: existing independent 19-column/288 privilege oracle and Flyway17 unchanged).

### `com/onecall/archtests/container/CustomerSubjectActivationFixture.java` — no test methods

Reuse `CustomerBusinessNumbersFixture` scaffolding for migrated `postgres:16`, verified sessions and HTTP startup, not its query credentials for commands. Create a random isolated activation LOGIN with synthetic UUID password and safe LOGIN/NOINHERIT/NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION/NOBYPASSRLS attributes. Grant exactly the two approved SET edges in **test setup only**. Seed valid/invalid policy snapshots and existing personal/organization subjects via fixture administrator/owner, never application repair. No production migration/grant is edited. Preflight real owner bean classes, qualifiers, manager datasource identity, READ_COMMITTED/writable/UTC, current_user/session_user, backend PID and transaction ID across both owners; distinguish each login/pool. Use max-size-one for pool-leak checks and at least two real connections/independent applications for concurrency tests. [G1,G2,G6,G8]

Concurrency is latch/barrier controlled with finite deadlines, not sleeps as proof: same event ACCEPT→identical results; two new ACCEPT events→one transition/trial but two consents; same-event DECLINE→one refusal, conflicting ACCEPT→CONSENT_REQUIRED; cross-subject event collision→one winning event and no loser effects. Use real lock observation (`pg_locks`/backend PIDs) and separate lifecycle writer to prove rechecks after account/member revocation serialization. Corruption/fault matrices use disposable fixture transactions or test-only repository decorators/functions; disable/re-enable immutable guards only in isolated administrator cleanup where necessary, never runtime grants. Instrument function-selection timing using a test-only controlled seam before/after the real invocation or PostgreSQL statement/log observation; do not replace `issue_first_trial` or claim a race is proven by sleeps. Disable-before-call exercises function rejection after preliminary lookup; disable after a real returned function result but before outer commit proves no second enabled recheck. [G3–G6]

ACCEPT failure points: after consent, after head update, after transition, after function-created trial Outbox, after activation Outbox, and transaction commit; enumerate consent/head/transition/trial/grant/balance and exact §5 Outbox deltas before/after, with audit delta always zero. For DECLINE consent/commit failure, refusal must not survive; successful DECLINE must survive its public exception mapping and audit unavailability. Prove no audit invocation on either decision and no Outbox invocation on DECLINE, rather than injecting a nonexistent DECLINE audit failure. Descriptor matrices cover valid ACCEPT/DECLINE shapes and canonical hashes, missing/extra keys/wrong types/bad hash, null result on ACCEPT and an ACCEPT result inside DECLINE. Corrupt trial/anchor/descriptor must yield INTERNAL, no guessed fields or repair; a legitimate null-result DECLINE descriptor must replay its refusal. Test logs/HTTP/serialized records with canaries, persisted descriptor/Outbox key allowlists and zero audit-row deltas rather than searching only HTTP. Close app contexts/pools and JDBC handles, drop isolated login/memberships and fixture rows in FK-safe order; no skipped tests for unavailable Docker. [G3–G8]

### Exact arithmetic and CI requirements [G8]

- New unit methods: `12 + 18 + 8 + 8 + 6 = 52` in five classes. `54 + 5 = 59` classes; `468 + 52 = 520` methods.
- New container methods: `24 + 10 = 34` in two classes. `23 + 2 = 25` classes/includes; `370 + 34 = 404` methods.
- Combined: `77 + 7 = 84` classes; `838 + 86 = 924` methods; regex `@Test\b` line scan `839 + 86 = 925`, retaining exactly the historical PostgresContainerIT comment. The broad substring `@Test` is not this oracle (it also matches TestConfiguration).
- Existing architecture assertion updates in §6 items 7 and 11–15 add zero methods/classes: all 77 existing class identities and 838 existing method identities remain exact. In particular, do not rename a historical method because its name contains an old dependency count. The 15-file existing-update allowlist changes file accounting, not source=CI=XML or annotation/POM totals.
- CI must introduce exact `pa31_unit_additions` and `pa31_container_additions` sets containing the above names; reject duplicate/extra/missing methods/classes. Record PA31-start `[468,370]`, annotation839; subtract PA31 additions when computing **every older checkpoint** so existing historical arithmetic does not drift. Keep PA30-start417/336 and all older values untouched. Update exact XML oracles to 520 and404, 59/25 class counts, includes25, total924/annotation925 and current log banner. Do not merely loosen inequalities/counts.
- Preserve original tests, V017 19-column/288 hardening inventory and existing ACL/function/PUBLIC oracles. New synthetic login edges are not new module base-table privileges or production provisioning. Existing 72 resolved coordinates / 65 packaged JARs and 116 negative firewall probes remain exact; no dependency additions are needed.

## 8. Validation runbook — future authorized S1, not executed at plan gate

Prerequisites: Java21, Maven3.9.9 wrapper, Docker daemon, `postgres:16`, Node22.22.2 per CI, loopback/ports and external synthetic test credentials. Use a clean authorized implementation workspace with a working Git mapping. Do not claim source/XML evidence from stale reports. Tooling/environment failure is failure, not a skip/pass. At this gate only §9 read-only structural checks run. [G6,G8]

### Frozen raw-byte baseline, before and after implementation

Save the 20 existing frozen raw hashes **outside the repository** before edits; compare raw bytes afterward, independent of Git CRLF normalization. No new repository cache/helper is authorized:

```bash
export PA31_FROZEN_MANIFEST="$(mktemp /tmp/pa31-frozen-XXXXXX.json)"
python3 - <<'PY'
import hashlib, json, os
from pathlib import Path
paths = [Path('api/openapi/onecall-v1.openapi.yaml'),
         Path('docs/14-production-data-model-and-flyway-schema-spec.md'),
         Path('docs/15-production-openapi-surface-spec.md')]
migrations = sorted(Path('backend/src/main/resources/db/migration').glob('V*.sql'))
assert len(migrations) == 17
assert sorted(int(path.name.split('__')[0][1:]) for path in migrations) == list(range(1,18))
paths += migrations
Path(os.environ['PA31_FROZEN_MANIFEST']).write_text(json.dumps({str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in paths}, sort_keys=True))
PY
```

After validation, from repository root:

```bash
python3 - <<'PY'
import hashlib, json, os
from pathlib import Path
expected = json.loads(Path(os.environ['PA31_FROZEN_MANIFEST']).read_text())
assert len(expected) == 20
for name, digest in expected.items():
    assert hashlib.sha256(Path(name).read_bytes()).hexdigest() == digest, name
actual = {str(path) for path in Path('backend/src/main/resources/db/migration').glob('V*.sql')}
assert actual == {name for name in expected if '/migration/' in name}
print('20 frozen raw byte hashes and V001-V017 path set unchanged; no V018')
PY
git diff --check
git diff --exit-code 6ddb81f0b9d7a48edeb6428468ce7332701cc69b -- api/openapi/onecall-v1.openapi.yaml docs/14-production-data-model-and-flyway-schema-spec.md docs/15-production-openapi-surface-spec.md backend/src/main/resources/db/migration
git status --short
```

The architecture test must embed the actual original 20 raw/LF hashes (copy the frozen oracle values from existing CustomerSubjectClaimBoundaryTests, verify raw against starting bytes, not hashes computed from modified files). Git normalized comparisons supplement, never replace, raw-byte equality. Review all changed/untracked paths against §6–7 allowlists. No migration inventory is allowed to grow. [G8]

### Focused unit, focused PostgreSQL16 and full suite

From `backend`:

```bash
java -version
./mvnw -version
docker info
./mvnw -B -ntp -Dtest=CustomerSubjectActivationsWebTests,CustomerSubjectActivationServiceTests,ActivationTrialServiceTests,CustomerSubjectActivationConfigurationTests,CustomerSubjectActivationBoundaryTests,ModuleBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests,CustomerEvidenceBoundaryTests,CustomerApiBoundaryTests test
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=CustomerSubjectActivationsIT,ActivationCommandRoleLeastPrivilegeIT verify
./mvnw -B -ntp -Pcontainer-smoke clean verify
```

Focused new IT XML must contain exactly24+10=34 methods, zero failures/errors/skips and PostgreSQL16 evidence; focused unit new classes exactly52 methods, plus the six existing architecture classes' unchanged method identities/counts (assertions updated per §6). The focused `ModuleBoundaryTests` run above covers the exact `discoveredProductionEdgesStayInsideTheConfiguredMap` assertion: its literal multiset must be the seven entries in §6 item 7, adding only `entitlement_metering`, preserving both `evidence` entries/all existing targets, with the description updated to exactly seven production edges. Verify that exact assertion, not merely the configured map or unchanged method count. Full clean verify must run all25 Failsafe classes/404 methods and59 Surefire classes/520 methods, 924 total, zero failures/errors/skips, BUILD SUCCESS. There is no substituted lightweight H2/unit-only acceptance. Existing migrations/ACL tests must still run, including their historical chain fixtures. [G1–G8]

### Execute the actual CI inventory, JAR smokes and firewall blocks locally

From repository root, this stdlib-only extractor executes the **unchanged meaning of the checked-in run blocks**, not a separately maintained approximation; it writes only the same generated build artifacts as CI in an authorized implementation workspace:

```bash
python3 - <<'PY'
import re, subprocess
from pathlib import Path
workflow = Path('.github/workflows/ci.yml').read_text()
for name in ('assert container smoke actually ran', 'default and opt-in web jar smokes', 'production jar dependency firewall'):
    pattern = r'^      - name: ' + re.escape(name) + r'\n        run: \|\n((?:          [^\n]*\n|\n)+)'
    match = re.search(pattern, workflow, re.M)
    assert match, name
    script = '\n'.join(line[10:] if line.startswith('          ') else '' for line in match.group(1).splitlines())
    subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', script], cwd='backend', check=True)
PY
```

Require exact source=CI=XML **84/924**, annotation925, POM25 with no unexpected class/method/XML duplicates. Require default JAR exits without server/pool; web-only JAR binds with no DB pool, exact activation POST returns 401 without token and 503 for syntactically valid bearer while the real verifier remains unavailable, with fixed redacted problems. Existing routes remain unchanged. Require firewall exact72 resolved/65 packaged inventories and116 rejection probes, no production Flyway/Testcontainers/test/provider leakage. No new JAR or runtime dependency is approved. Keep full commands/stdout/stderr/exit statuses in external implementation evidence; CI timeout remains30 minutes. [G6–G8]

### OpenAPI static contract

From repository root in the authorized validation workspace:

```bash
node --version
npm --prefix api/openapi ci
npm --prefix api/openapi run check:s3
```

All lint/bundle/generated TypeScript/typecheck/doc-map and media-gateway lint must pass with the source YAML unchanged. Generation outputs under existing ignored `tmp/openapi-client` are future validation outputs, **not allowed at this plan gate**. Confirm operationId/path/method, six request keys, six result keys/two trial keys, ACCEPT-only200 and exact nine operation errors using unchanged contract plus new web tests. Do not edit client compatibility/contract rules to conceal divergence. [G7,G8]

## 9. Plan-gate evidence, documentation decision and residual ownership

**Documentation decision:** at future S1 update only backend README as listed; record opt-in dedicated pool, two-owner handoff, exact policy root metadata/version convention, six-field body idempotency, replay vs live state, zero-write SUSPENDED ACCEPT→403 FORBIDDEN, committed consent-only DECLINE, no decision audit on either decision, original trial facts, frozen inventory, and external login/policy publication prerequisites. Do not claim the PA25 script provisions activation, real policy content exists, a production session verifier is delivered, or live acceptance occurred. OpenAPI/docs14/docs15 and V001–V017 remain byte-identical. Implementation evidence/report belongs to the Controller-authorized later artifact destination, not an extra product goal or an amendment to frozen docs. [G8]

**Read-only inspection and Git context:** the default WSL `.git` path `D:/codex-call/.git/worktrees/codex-call8` still fails native status/HEAD resolution. In this fix session, read-only commands with explicit `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call8 --work-tree="$PWD"` resolve HEAD `6ddb81f0b9d7a48edeb6428468ce7332701cc69b` and branch `codex/pa31-next-work-unit` without gitfile/config repair. Use `GIT_OPTIONAL_LOCKS=0` for validation. The supplied existing plan and pre-existing `.codex` appear untracked on entry; neither is evidence of an unauthorized task addition. Compare task content/path deltas against the entry snapshot as well as Git. Git comparison from authoring `6d02c9b4` to intake/review `35b223d4` shows only Control bookkeeping, consistent with §1 and the Controller's reconciliation. No content mismatch or new product-base decision remains.

**Original intake evidence (historical, not this fix's baseline):** the authoring task reported a 1904-file manifest excluding its then-new plan, aggregate `c0f7eb28bc544b3977409a420f7d6fd45989bcb0ae963051923def336f69a587`. The original reviewed plan was 66,647 bytes / 445 lines, SHA256 `380f29a592a45f26ac73debdac711e5e30a6cd4285db4c60ed02d905b15bdd10`. Reviews A/B and later Control bookkeeping now also exist, so do not present the old whole-tree manifest as the current entry snapshot. Both review hashes were reverified against the fix dispatch; their artifacts remain unchanged.

**Fix-1 structural validation:** before edits a sorted path/raw-SHA256 snapshot captured 1907 existing regular files (excluding `.git`), aggregate `76f25b84c2cb7e7c0fca5b666ecdb10cdd66580cd2f75e66936d2a127ebc5502`, saved outside the repository. Compare afterward for exactly one modified existing plan and one added fix artifact; every other 1906 existing content hash and the gitfile must remain exact. The ordered 20 frozen paths (OpenAPI, docs14, docs15, sorted migrations) still use aggregate `f6f48d2448284ac82d06c2046242d1d58e4f13cc2a56522fb79304bf6e444ad8`; manifest encoding is `path + NUL + lowercaseRawSha256 + LF`. Validate raw equality, V001–V017 exact path set and zero Git content diff for these paths. Source-regex/POM-XML and CI AST registry extraction (including updates/subscript assignments) recheck 54/468 +23/370 =77/838, annotation839/POM23. Parse all nine plan sections, the 17 new production paths, 15 existing updates, seven distinct lists with86 unique methods (52 unit/34 IT), projected59/520 +25/404 =84/924, annotation925/POM25, closure consistency and all plan/CI shell syntax via `bash -n`. Actual results, final bytes/lines/SHA256 and limitations are recorded in the companion fix artifact and completion report. No future Maven/npm/JAR/container commands or test/XML execution run at this gate; source=CI=XML84/924 remains an implementation acceptance target, not executed XML evidence.

### Fix-1 Controller finding dispositions

| Finding | Binding disposition and cumulative-plan closure |
| --- | --- |
| PA31-PLAN-REV-01 | Accepted, Severe: §3 row10, narrative/problem mapping, §5 prewrite branch, §7 web/unit/IT assertions and risk row now require new valid SUSPENDED ACCEPT→403 FORBIDDEN, zero writes; exact historical replay and DECLINE remain separate. |
| PA31-PLAN-REV-02 | Accepted, High: §6 adds the five omitted architecture paths with exact 11→12 dependency assertions/edge and 43→47 internal-file count/controller-profile list; all existing identities/counts stay fixed. Fifteen existing updates, 25 new/40 total implementation paths; §7 arithmetic and §8 focus list agree. |
| PA31-PLAN-REV-03 | Accepted in part, Medium: §3/§5/§7 remove all decision audits, especially DECLINE's extra write/failure coupling; refusal consent only. ACCEPT retains exact frozen transition/trial Outbox with §5 docs14 compliance explanation and branch-by-branch row-delta tests. No invented audit taxonomy. |
| PA31-PLAN-REV-04 | Accepted, Medium: §4 states the no-own-transition/current-row reconstruction gap and keeps the immutable event-time descriptor; exact ACCEPT object vs DECLINE null, canonical snapshot hash, replay/link checks and INTERNAL corruption mapping agree with §7 tests. |
| PA31-PLAN-REV-05 | Rejected-with-reason, retained: §4 expressly identifies generic frozen JSON/no publisher and pins one Controller-authorized root-metadata/positive-decimal version parser, fail-closed CONSENT_REQUIRED with no fallback/new source/schema. Operational publication remains external, not a blocking design choice. |

### Fix-2 accepted correction and read-only checkpoint

`PA31-PLAN-REV-06` (accepted Medium; A-REREV-01/B-REV-B2-01): §6 item 7 now explicitly requires `ModuleBoundaryTests.discoveredProductionEdgesStayInsideTheConfiguredMap` to change the literal discovered-target multiset from six to seven by adding only `entitlement_metering`, preserving both `evidence` entries and every existing target, and replacing the exact six-edge description/message with the seven-edge description. Preserve method identity and all other assertions except the prior accepted dependency updates. The §6 coding checkpoint and §8 focused `ModuleBoundaryTests` run cover this exact assertion; zero added test methods/classes never means leaving the six-entry literal unchanged. REV-01..05 dispositions above and all behavioral contracts remain unchanged.

Fix-1 Git context and structural evidence above are historical. At fix-2 the same read-only Git mapping resolves §1's current HEAD/branch without metadata repair; the plan, fix-1 and pre-existing `.codex` are already untracked at entry. Compare an outside-repository entry path/raw-SHA256 snapshot against completion for exactly the modified cumulative plan and new fix-2 artifact; preserve every other entry file, gitfile and index. Verify all 15 existing update paths, the 17 new production paths, seven new test classes plus fixture, 40 implementation paths, 86 new methods (52+34), target84/924, annotation925/POM25; byte-compare frozen OpenAPI/docs14/docs15 and V001–V017 and require no V018. Check exact six-to-seven enumeration/description instructions, no residual instruction to retain the six-entry assertion, UTF-8/LF/whitespace/fences and embedded shell/Python syntax. Record original/fixed plan hashes, output sizes/lines/SHA256 and actual read-only outcomes in fix-2/completion evidence, not self-referential hashes. No runtime/test/XML/CI execution or later gate is claimed. Handoff is only for fresh independent targeted A/B re-review, not acceptance or implementation.

### Risks, questions and residual owners

| Item | Disposition / owner / destination |
| --- | --- |
| Production policy snapshot may lack required root metadata | Runtime CONSENT_REQUIRED with no fallback. Policy content/publication population is Operations/product owner work outside PA31; no seed/migration/management endpoint here. Version identity/parser is fixed in §4, not an unresolved implementation choice. |
| Dedicated login/two role edges, credentials, pg_hba/TLS, rotation and pool drain | Platform/Database/Security/Operations deployment input; no production provisioning in S1. Existing frozen grants are sufficient. Do not extend PA25 script in this slice. |
| Historical replay while current state is SUSPENDED | Resolved: currently authorized exact replay returns frozen history with no write; new valid ACCEPT is 403 FORBIDDEN with zero writes. DECLINE remains one refusal consent only after valid policy. Operations alone owns resume; clients must not treat historical ACTIVE as a live-state projection. |
| Replay descriptor corruption or incomplete historical trial facts | INTERNAL, no repair/synthesis. Database/Operations incident destination; not an automatic reconciliation feature. |
| Policy publication concurrent with issuance | V006 selection is sole fresh-issuance authority; wrapper is preliminary and never takes publication lock. Preserve immutable original trial on replay. Covered by focused IT, no second policy source/check. |
| Broad privileges of two approved owner roles within one application login | Existing trusted-application boundary, explicitly tested SET scopes; Security owns runtime isolation and production administration. No claim of RLS or protection against arbitrary compromised application SQL. |
| Docker/JDK/Node/default Git mapping unavailable | Controller/runtime owner supplies future implementation validation environment outside repository; no skip/fabricated pass. Explicit read-only Git path overrides verify this session's branch/HEAD, not toolchain execution or production deployment. |

No unresolved product/policy/schema/privilege/state-transition blocker was found. No new public code or grant is required. A newly discovered contradiction triggers the S1 stop rule, not goal expansion. [G1–G8]

### Required completion report

Plan-fix report: task `pa31-subject-activation-plan-fix2-gpt-20261113-01`, actual observable Codex CLI runtime/provider limitation; exactly this one modified existing file plus the one new fix-2 artifact; both paths/bytes/lines/SHA256; one slice;17 new production files, seven test classes plus fixture, 15 future existing-file edits (25 new/40 total implementation paths); exact86 new methods (52+34) and84/924 target, annotation925/POM25; REV-06 exact assertion/description closure, preserved REV-01..05 dispositions, actual structural validation outcomes and residual ownership. State ready only for fresh independent targeted A/B re-review, not implementation. No commit/push/PR/merge/approval/ready action/reviewer request/external comment/issue mutation/re-review dispatch/later gate occurred.

Future S1 report (only after separate authorization): authorized allowlist vs actual changed paths; each G1–G8 proof with test method identities; focused/full commands, exit codes and zero-skip XML identities; source=CI=XML arithmetic; role/PID/transaction/ACL evidence; byte/hash/migration path-set comparisons; JAR/firewall/OpenAPI outcomes; README truth and external deployment residuals. Separate **executed evidence** from **planned/not executed** checks. Never equate local/container success with live production acceptance or permission to advance a gate.
