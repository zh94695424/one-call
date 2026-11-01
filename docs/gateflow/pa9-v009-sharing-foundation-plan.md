# PA9 V009 Sharing Persistence Foundation — Plan Gate

- Task label: `pa9-v009-plan-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`; architecture-sensitive persistence / Flyway migration feature.
- Gate: `plan` only; author date: 2026-10-31, Asia/Shanghai.
- Base: `835f2ecece1ac8b5c79ab794427424ea380ac052`; branch: `codex/pa9-v009-sharing-foundation`.
- Destination: `docs/gateflow/pa9-v009-sharing-foundation-plan.md`.
- Author outcome: plan-complete, not accepted, reviewed, implemented or production-ready. Later gates require their own authorization.

## 1. Goal, motivation and success signal

Implement only the frozen V009 persistence foundation for `sharing.evidence_share_grant`, `sharing.share_admission` and `sharing.share_access_event`. Freeze the grant's content and verification scope, preserve durable admission/access history, and provide executable PostgreSQL 16/Flyway evidence for schema, least privilege, lifecycle, rollback and serialization. Persistence is the smallest useful boundary: Redis cannot be the admission authority, and a future service cannot safely supply durable idempotency without these keys, guards and privileges.

Success is a clean Flyway V001–V009 chain; exact catalog/ACL inventories; positive and negative predicate, immutability, lifecycle/revision, admission count-once/limit, transaction rollback and bounded race witnesses; unchanged cumulative tests and ten unit/module tests; exact source=CI=Failsafe XML method identity; and byte-identical canonical Git blobs for V001–V008. Runtime success is required later, not claimed by this planning artifact.

### 1.1 Scope and non-goals

The only new database schema objects are one sharing schema, two roles, three tables and their frozen constraints/indexes, three owner-local trigger functions and four triggers. Cross-module grants consume existing objects; no existing table definition changes. No application code, dependency, test resource, API, worker, queue, migration repair, backfill, new service function or extra registry is authorized.

Do not implement `sharing.create_share_grant`, `sharing.admit_share`, `sharing.apply_share_transition`, `evidence.read_shareable_projection`, ticket issue/reconstruction, playback, SMS/OTP, KDF execution, Redis, COS/KMS, gateway or production integration. No invented KDF parameter, expiry/TTL, limit, rate/risk/access policy or non-code unlock rule. The five published SECURITY DEFINER functions and every S1/S2 schema contract remain unchanged. No V010. No commits, branches, pushes, PRs, issues, tags, external comments, approval or mark-ready action is authorized here.

## 2. Authority, first-principles judgement and direct evidence

Normative sources read: doc-14 §§1.6, 2.12, 3.1–3.2, 4.44–4.46, 5.5, 7.4, 8.6, 9.0.1, 9.29–9.31, 10.21–10.23, 11.5, 12.7–12.8, 13.8–13.9 and 14; doc-12 module ownership/architecture; current controller checkpoint. PA8 plan and final closeout supply harness patterns and risk evidence only, not PA9 product rules or transferred acceptance claims.

Direct repository evidence:

| Evidence | Consequence |
| --- | --- |
| `PostgresContainerIT.java:934` onward, `startContainerAndMigrate` | Existing clean database, targets 004→005→006→007 then unrestricted V008; retain all intermediate evidence. |
| `PostgresContainerIT.java:1205` | Rename the existing full-chain method; extend its exact ordered history, do not replace it. |
| `PostgresContainerIT.java:2589`, `:2607`, `:2625` | Preserve the six PA8 methods and exact evidence catalog/ACL/FK controls. |
| `PostgresContainerIT.java:23905`, `:23962`, `:24329` | Reuse canonical-LF digest checking and PostgreSQL canonical predicate maps; move the no-V009 fence to a real staged-V008 checkpoint. |
| `PostgresContainerIT.java:5421`, `:2917`, `:25659` | Reuse real blocked-writer observation, bounded futures, fresh-connection verification and method-local FK-safe cleanup. |
| `V008__evidence_media.sql:9` and doc-14 §11.5 | V008 creates only evidence roles; V009 must create sharing roles before evidence USAGE grants. V008 is never patched. |
| `.github/workflows/ci.yml:59`, `:78` | Current Failsafe expected set has exactly 57 named methods; CI compares XML set, totals and zero skips/failures/errors. |
| `backend/pom.xml:136`, test resources | `container-smoke` runs only `PostgresContainerIT` through Failsafe integration-test+verify; broken V901 fixture remains test-only and unchanged. |
| `backend/README.md`, architecture tests | Default verify has 10 Surefire tests; no runtime DataSource/Flyway or application service is introduced. |
| Controller active 2026-10-31 checkpoint | Goal confirmed; gate is plan; no implementation/acceptance authorization carried from PA8. |

Read-only Git inspection confirmed HEAD equals the supplied base. The Windows `.git` pointer does not resolve automatically in this Linux shell. Read-only commands use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`; no metadata is repaired. At intake the controller already has an unrelated 16-addition/2-deletion diff and `.codex` is untracked. Both are preserved exactly and excluded from this author's changed-path list. Baseline diff and SHA256 were saved outside the repository for final comparison. A whole-worktree-clean assertion would misrepresent this intake state.

### 2.1 Design reconciliation and precedence

1. Tables, checks and ordinary FKs are the exact §4.44–4.46 definitions below. All six V009 FKs explicitly say RESTRICT/RESTRICT, are immediate/nondeferrable and have no cascade. §12.8's broad statement about every S3 FK does not override the two existing V008 deferred NO ACTION cycle FKs, whose exact §11.5 DDL and current catalog must be preserved. This is already reconciled by §5.5 and PA8, not a reason to rewrite V008.
2. “No S3 function” means no published function-wave service. Three `RETURNS trigger` SECURITY INVOKER helpers are explicitly required by §10.22; they are not a sixth service function. `sharing.guard_share_grant()` is the owner-local name corresponding to `trg_share_grant_guard`; `sharing.guard_share_admission()` and `sharing.reject_row_mutation()` follow the named contract/current evidence convention. No overloads/arguments.
3. §11.5 step 1's schema/role wording is executable as: create `sharing_owner`, create `sharing_role`, create schema owned by `sharing_owner`, revoke PUBLIC, grant USAGE only after both roles exist. Grant evidence USAGE only after sharing_role exists. No forward role reference.
4. Use the literal frozen JSON predicates, including PostgreSQL CHECK's TRUE-or-UNKNOWN behavior. Do not silently add JSON type checks, mandatory-member checks, `COALESCE(...,false)`, cross-table payload validation or canonical-hash recomputation. Missing/null nested members can yield UNKNOWN; tests must document those accepted shapes as well as concrete false failures. The creation object is frozen initial input: its initial expiry/code-related content is not rewritten on extension/rotation. The checks bind only the exact members they name.
5. The event admission FK is a single nullable ID FK, not a composite grant/admission FK. It does not prove that an admission belongs to the event's grant. Admission-event JSON proves the listed presence/enum predicates, not numeric generations, timestamp ordering, member UUID format or equality to the admission/grant. Do not add checks, FKs or triggers for these future service responsibilities.
6. The immediate admission guard re-reads without `FOR UPDATE`/`FOR SHARE`; the caller must already hold the parent grant row lock. It neither increments the grant nor writes an event. Same-key replay is resolved before a new INSERT/limit test, not by ON CONFLICT after the guard. Count/event atomicity is a same-transaction caller obligation, exercised by test-only SQL, not an invented database service.
7. §4.44/§7.4 supplies the legal status/revision/action matrix; §10.22 supplies structural guard coverage. Distinguish guarded invariants from caller-only atomicity, expected-revision WHERE clauses, event identity/fingerprint comparison, authorization and approved policy. No live policy/service behavior is claimed by SQL transaction witnesses.

No blocking frozen-design incompatibility was found. Any implementation evidence that these predicates, ACLs or matrix cannot coexist is a stop-and-escalate to the controller/design owner, never permission to change a product rule.

## 3. Exact V009 DDL inventory

| Object class | Grant | Admission | Access event | Total |
| --- | ---: | ---: | ---: | ---: |
| Tables | 1 | 1 | 1 | 3 |
| Ordered columns | 20 | 7 | 11 | 38 |
| NOT NULL / nullable | 16 / 4 | 7 / 0 | 9 / 2 | 32 / 6 |
| Named CHECKs | 17 | 5 | 9 | 31 |
| PK constraints | 1 | 1 | 1 | 3 |
| UNIQUE constraints | 2 | 2 | 1 | 5 |
| Secondary nonunique indexes | 2 | 1 | 2 | 5 |
| All physical indexes, including PK/UNIQUE | 5 | 4 | 4 | 13 |
| Ordinary FKs | 3 | 1 | 2 | 6 |
| Explicit defaults | 7 | 1 | 1 | 9 |
| User triggers | 1 | 2 | 1 | 4 |

No partial unique index, deferred V009 constraint/trigger, sequence, identity/generated column, SQL service, extension or extra table. All tables/indexes/constraints and three functions are owned by `sharing_owner`. Internal RI triggers are excluded from the four-user-trigger count.

### 3.1 Exact ordered columns and defaults

`NO` means NOT NULL, `YES` means nullable, `none` means no default expression (not an explicit NULL default). Use `timestamptz` / catalog `timestamp with time zone`. No generated UUID default.

#### sharing.evidence_share_grant

| Ordinal | Column | SQL type | Nullable | Default |
| --- | --- | --- | --- | --- |
| 1 | id | uuid | NO | none |
| 2 | share_id | text | NO | none |
| 3 | capability_token_hash | text | NO | none |
| 4 | evidence_id | uuid | NO | none |
| 5 | created_by_account_id | uuid | NO | none |
| 6 | owner_organization_id | uuid | YES | none |
| 7 | verification_mode | text | NO | none |
| 8 | code_salt | text | YES | none |
| 9 | code_hash | text | YES | none |
| 10 | code_generation | integer | NO | 0 |
| 11 | valid_from | timestamptz | NO | now() |
| 12 | expires_at | timestamptz | NO | none |
| 13 | max_admissions | integer | YES | none |
| 14 | admissions_used | integer | NO | 0 |
| 15 | download_allowed | boolean | NO | false |
| 16 | content_snapshot | jsonb | NO | none |
| 17 | content_sha256 | text | NO | none |
| 18 | status | text | NO | 'ACTIVE'::text |
| 19 | revocation_generation | integer | NO | 0 |
| 20 | revision | integer | NO | 1 |

#### sharing.share_admission

| Ordinal | Column | SQL type | Nullable | Default |
| --- | --- | --- | --- | --- |
| 1 | id | uuid | NO | none |
| 2 | share_grant_id | uuid | NO | none |
| 3 | admission_key | text | NO | none |
| 4 | admission_no | integer | NO | none |
| 5 | verification_mode | text | NO | none |
| 6 | verification_result | text | NO | none |
| 7 | admitted_at | timestamptz | NO | now() |

#### sharing.share_access_event

| Ordinal | Column | SQL type | Nullable | Default |
| --- | --- | --- | --- | --- |
| 1 | id | uuid | NO | none |
| 2 | share_grant_id | uuid | NO | none |
| 3 | share_admission_id | uuid | YES | none |
| 4 | event_domain | text | NO | none |
| 5 | event_id | text | NO | none |
| 6 | event_type | text | NO | none |
| 7 | result | text | NO | none |
| 8 | occurred_at | timestamptz | NO | now() |
| 9 | risk_digest | text | YES | none |
| 10 | payload_redacted | jsonb | NO | none |
| 11 | payload_sha256 | text | NO | none |

### 3.2 Exact named checks and predicate semantics

The following literals are transcribed from doc-14, not inferred stricter JSON schemas. Registry order is preserved. SQL deparse verification is specified in §7.

#### Grant: 17 checks

- `ck_share_grant_share_id`: `length(share_id) BETWEEN 1 AND 128 AND btrim(share_id) <> ''`.
- `ck_share_grant_token_hash`: `capability_token_hash ~ '^[0-9a-f]{64}$'`.
- `ck_share_grant_verification_mode`: `verification_mode IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER')`.
- `ck_share_grant_code_salt`: `code_salt IS NULL OR (length(code_salt) BETWEEN 22 AND 256 AND code_salt ~ '^[A-Za-z0-9_-]+$')`.
- `ck_share_grant_code_hash`: `code_hash IS NULL OR code_hash ~ '^[0-9a-f]{64}$'`.
- `ck_share_grant_code_generation`: `code_generation >= 0`.
- `ck_share_grant_verification_shape`: `(verification_mode = 'EXTRACTION_CODE' AND code_salt IS NOT NULL AND code_hash IS NOT NULL AND code_generation >= 1) OR (verification_mode <> 'EXTRACTION_CODE' AND code_salt IS NULL AND code_hash IS NULL AND code_generation = 0)`.
- `ck_share_grant_org_member`: `verification_mode <> 'ORGANIZATION_MEMBER' OR owner_organization_id IS NOT NULL`.
- `ck_share_grant_validity`: `expires_at > valid_from`.
- `ck_share_grant_admissions`: `admissions_used >= 0 AND (max_admissions IS NULL OR (max_admissions >= 1 AND admissions_used <= max_admissions))`.
- `ck_share_grant_snapshot_shape`: `jsonb_typeof(content_snapshot) = 'object' AND content_snapshot ? 'content' AND content_snapshot ? 'verification' AND content_snapshot ? 'creation' AND jsonb_typeof(content_snapshot->'content') = 'object' AND jsonb_typeof(content_snapshot->'verification') = 'object' AND jsonb_typeof(content_snapshot->'creation') = 'object' AND content_snapshot->'content'->>'evidenceId' = evidence_id::text AND content_snapshot->'creation'->>'creatorAccountId' = created_by_account_id::text AND content_snapshot->'creation'->>'evidenceId' = evidence_id::text AND (content_snapshot->'creation'->>'ownerOrganizationId') IS NOT DISTINCT FROM owner_organization_id::text AND (content_snapshot->'creation'->>'scopeDigest') ~ '^[0-9a-f]{64}$' AND (content_snapshot->'creation'->>'creationRequestDigest') ~ '^[0-9a-f]{64}$' AND (content_snapshot->'creation'->>'creationFingerprint') ~ '^[0-9a-f]{64}$'`.
- `ck_share_grant_snapshot_binding`: `content_snapshot->'verification'->>'mode' = verification_mode`.
- `ck_share_grant_mobile_binding`: `(verification_mode = 'DESIGNATED_MOBILE' AND (content_snapshot->'verification'->>'mobileHmac') ~ '^[0-9a-f]{64}$' AND length(content_snapshot->'verification'->>'keyVersion') BETWEEN 1 AND 64) OR (verification_mode <> 'DESIGNATED_MOBILE' AND NOT (content_snapshot->'verification' ? 'mobileHmac'))`.
- `ck_share_grant_content_sha256`: `content_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_share_grant_status`: `status IN ('ACTIVE','EXPIRED','REVOKED','LOCKED')`.
- `ck_share_grant_revocation_generation`: `revocation_generation >= 0`.
- `ck_share_grant_revision`: `revision >= 1`.

#### Admission: 5 checks

- `ck_share_admission_key`: `length(admission_key) BETWEEN 1 AND 128 AND btrim(admission_key) <> ''`.
- `ck_share_admission_no`: `admission_no >= 1`.
- `ck_share_admission_mode`: `verification_mode IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER')`.
- `ck_share_admission_result`: `verification_result IN ('VERIFIED','NOT_REQUIRED')`.
- `ck_share_admission_result_shape`: `(verification_mode = 'NONE' AND verification_result = 'NOT_REQUIRED') OR (verification_mode IN ('EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER') AND verification_result = 'VERIFIED')`.

#### Access event: 9 checks

- `ck_share_access_event_domain`: `event_domain = 'SHARE_ACCESS'`.
- `ck_share_access_event_id`: `length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''`.
- `ck_share_access_event_type`: `event_type IN ('PAGE_OPENED','PREFETCH_IGNORED','VERIFICATION_FAILED','LOCKED','ADMISSION_GRANTED','TICKET_ISSUED','TICKET_FAILED','PLAYBACK_AUTHORIZED','SEEK_AUTHORIZED','RENEWED','EXTENDED','CODE_ROTATED','REVOKED','EXPIRED','FAILURE')`.
- `ck_share_access_event_result`: `result IN ('SUCCESS','DENIED','FAILED','IGNORED')`.
- `ck_share_access_event_risk_digest`: `risk_digest IS NULL OR risk_digest ~ '^[0-9a-f]{64}$'`.
- `ck_share_access_event_payload`: `jsonb_typeof(payload_redacted) = 'object'`.
- `ck_share_access_event_payload_sha256`: `payload_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_share_access_event_admission_shape`: `event_type NOT IN ('ADMISSION_GRANTED','TICKET_ISSUED','TICKET_FAILED','PLAYBACK_AUTHORIZED','SEEK_AUTHORIZED','RENEWED') OR share_admission_id IS NOT NULL`.
- `ck_share_access_event_admission_facts`: `event_type <> 'ADMISSION_GRANTED' OR (payload_redacted ? 'verificationMode' AND payload_redacted->>'verificationMode' IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER') AND payload_redacted ? 'revocationGeneration' AND payload_redacted ? 'codeGeneration' AND payload_redacted ? 'grantExpiresAt' AND payload_redacted ? 'admissionExpiresAt' AND payload_redacted ? 'sessionExpiresAt' AND (payload_redacted->>'verificationMode' <> 'ORGANIZATION_MEMBER' OR (payload_redacted ? 'recipientAccountId' AND payload_redacted ? 'recipientOrganizationId')))`.

### 3.3 Keys, indexes and FKs

All keys/indexes are plain btree, no predicate, expression, INCLUDE or altered NULL uniqueness semantics. Pin names, key order, DESC flags, uniqueness, validity, owner and index/constraint relationship, not just counts.

| Table | PK / UNIQUE / secondary indexes |
| --- | --- |
| evidence_share_grant | `pk_evidence_share_grant(id)`; `uq_share_grant_share_id(share_id)`; `uq_share_grant_token_hash(capability_token_hash)`; `ix_share_grant_evidence(evidence_id,status)`; `ix_share_grant_expiry(status,expires_at)` |
| share_admission | `pk_share_admission(id)`; `uq_share_admission_key(share_grant_id,admission_key)`; `uq_share_admission_no(share_grant_id,admission_no)`; `ix_share_admission_grant_time(share_grant_id,admitted_at)` |
| share_access_event | `pk_share_access_event(id)`; `uq_share_access_event_domain_id(event_domain,event_id)`; `ix_share_access_event_grant(share_grant_id,occurred_at DESC)`; `ix_share_access_event_admission(share_admission_id,occurred_at)` |

| FK | Child → parent |
| --- | --- |
| fk_share_grant_evidence | evidence_share_grant(evidence_id) → evidence.evidence_record(id) |
| fk_share_grant_creator_account | evidence_share_grant(created_by_account_id) → customer_iam.customer_account(id) |
| fk_share_grant_owner_organization | evidence_share_grant(owner_organization_id) → organization_access.organization(id) |
| fk_share_admission_grant | share_admission(share_grant_id) → sharing.evidence_share_grant(id) |
| fk_share_access_event_grant | share_access_event(share_grant_id) → sharing.evidence_share_grant(id) |
| fk_share_access_event_admission | share_access_event(share_admission_id) → sharing.share_admission(id) |

Each FK: `ON UPDATE RESTRICT ON DELETE RESTRICT`, MATCH SIMPLE, `confupdtype='r'`, `confdeltype='r'`, `confmatchtype='s'`, validated, `condeferrable=false`, `condeferred=false`; exact referenced PK and child-column ordering. Nullable organization/admission references permit NULL, never orphan non-NULL IDs. No ordinary hard delete of grants is exposed to runtime; no DELETE/TRUNCATE on any sharing table. Append-only rows are guarded even for owner UPDATE/DELETE. Owner administrative cleanup is test-only and never evidence of a production deletion path.

### 3.4 Executable creation and ACL order

1. Migration identity creates `sharing_owner` and `sharing_role` with `NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE`; default INHERIT, NOREPLICATION, NOBYPASSRLS, connection limit -1, no password, no memberships. Create sharing schema AUTHORIZATION sharing_owner; revoke all PUBLIC schema privileges; grant sharing USAGE and existing evidence USAGE to sharing_role only after role creation.
2. `SET LOCAL ROLE sharing_owner`; create grant with named PK/UNIQUE/checks/three FKs, then its indexes; admission after grant with keys/checks/FK/index; access event after both with keys/checks/two FKs/indexes.
3. Create the three local functions and four triggers in §4; revoke PUBLIC EXECUTE on each function, grant only sharing_role EXECUTE; revoke PUBLIC table/column privileges. Keep ownership sharing_owner throughout; transaction-local role switching only.
4. Grant runtime owned-table privileges in §5. Reset to migration identity for grants on existing foreign schemas, exact columns and platform/audit functions. Follow the V008 grant execution pattern; do not transfer foreign ownership or grant runtime role membership in owners.
5. End transaction with no foreign writes, seed rows, backfill, provisioning shortcut, global default-privilege change or application service. Flyway handles the SQL transaction as for V008.

## 4. Guard/function and state contracts

All three functions: zero arguments, `RETURNS trigger`, PL/pgSQL, SECURITY INVOKER (`prosecdef=false`), owner sharing_owner, `SET search_path = sharing, pg_temp`, no transaction control/dynamic SQL/foreign writes, explicit qualified grant read, no hidden service API. PUBLIC EXECUTE revoked; explicit runtime EXECUTE only to sharing_role, owner implicit privilege retained. Proposed internal error labels below are implementation diagnostics, not added product error/action registry entries.

| Trigger | Exact binding / function | Contract |
| --- | --- | --- |
| trg_share_grant_guard | grant BEFORE UPDATE FOR EACH ROW → sharing.guard_share_grant() | Frozen fields, monotonic expiry/generation, legal status matrix and revision/rotation rules below. |
| trg_share_admission_guard | admission BEFORE INSERT FOR EACH ROW → sharing.guard_share_admission() | Immediate, nonlocking parent re-read; mode/status/clock/limit/next-number predicates below. |
| trg_share_admission_immutable | admission BEFORE UPDATE OR DELETE FOR EACH ROW → sharing.reject_row_mutation() | Every owner UPDATE/DELETE, including no-op, raises P0001. |
| trg_share_access_event_immutable | event BEFORE UPDATE OR DELETE FOR EACH ROW → sharing.reject_row_mutation() | Every owner UPDATE/DELETE, including no-op, raises P0001. |

No grant DELETE trigger is added: runtime DELETE denial and FKs are the frozen boundary. All four triggers are ordinary enabled row triggers (`tgenabled='O'`, not internal, not deferred); exact timing/operation bitmask and function OID/binding must be asserted.

### 4.1 Grant guard and revision matrix

Use null-safe `IS DISTINCT FROM` for changes to the 12 immutable columns: `id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id, verification_mode, valid_from, max_admissions, download_allowed, content_snapshot, content_sha256`. Refuse any change with `P0001/SHARE_GRANT_IMMUTABLE`. Refuse decreasing expires_at with `SHARE_GRANT_EXPIRY_DECREASE`; refuse decreasing either generation with `SHARE_GRANT_GENERATION_INVALID`. Refuse changing either code salt/hash without the matching code-generation change, or changing code_generation without both fresh salt and fresh hash, with `SHARE_GRANT_CODE_ROTATION_INVALID`. Non-code modes cannot rotate because verification_shape requires generation=0 and NULL secrets.

Required named owner action/state witnesses (SQL harness, no application service). This matrix describes atomic owner actions, not every structurally possible mutable SQL statement shape; unchanged fields in an action row are service obligations, not an unprovided mixed-action DDL ban:

| Initial → resulting status | Legal mutation | Generation and revision |
| --- | --- | --- |
| ACTIVE → ACTIVE | New admission count update | admissions_used becomes inserted admission_no; generations unchanged; revision +1 |
| ACTIVE → ACTIVE | Extension, only while old expiry is still future | expires_at strictly increases; generations/count unchanged; revision +1 |
| ACTIVE → LOCKED | Owner lock | generations/count/expiry unchanged; revision +1 |
| ACTIVE or LOCKED → REVOKED | Revoke | revocation_generation +1, other generation/count unchanged; revision +1 |
| ACTIVE or LOCKED → EXPIRED | Persist observed expiry | clock_timestamp() >= old expires_at; revocation_generation +1 once; revision +1 |
| LOCKED → ACTIVE | Extraction-code rotation only | salt/hash both replaced, code_generation +1 and revocation_generation +1; revision +1 |
| EXPIRED / REVOKED → different state | Never legal | P0001/SHARE_GRANT_STATUS_TERMINAL |
| Any other state edge / LOCKED non-code unlock | Never legal | P0001/SHARE_GRANT_STATUS_TRANSITION |

Enforce the structural status edges, terminality, exact status-transition revision +1 and matching rotation at the grant guard. Check exact rotation generation increments there; report invalid revision with `SHARE_GRANT_REVISION_INVALID` and invalid generation with the label above. Enforce revision +1 when expiry, code values/generations or admissions_used actually change, as mutable owner transitions require; a skipped delta (+2), unchanged delta or stale expected revision is not accepted as that transition. Generation decreases are always structurally forbidden. Exact revocation_generation +1 for named status-changing revoke/expiry edges applies only when `OLD.status IS DISTINCT FROM NEW.status`; ACTIVE → LOCKED remains generation-neutral. Same-state generation increases are not subject to an exclusive named-status-edge allow-list; exact code-rotation coupling and generation increments still apply whenever code values/code_generation change. A same-state ACTIVE code-salt/code-hash replacement with code_generation +1, revocation_generation +1 and revision +1 is therefore structurally accepted by the frozen guard but is service-forbidden: it is not an owner action, does not unlock anything, and future services must not treat it as a `CODE_ROTATED` success. Increasing expiry requires `OLD.status = 'ACTIVE'` and `clock_timestamp() < OLD.expires_at` (`SHARE_GRANT_EXTENSION_INVALID`); the resulting `NEW.status` need not be ACTIVE. This predicate applies only when `NEW.expires_at > OLD.expires_at`. Persisting expiry before old expiry is refused (`SHARE_GRANT_EXPIRY_INVALID`). Do not introduce an alternative unlock, adjust max_admissions, rewrite initial creation expiry, or change generation on the named admission/lock/extension actions.

Do not invent a blanket ban on same-status no-op/revision-only writes: retain the baseline check `revision>=1`; a same-state revision-only update is not itself an owner action or an idempotency guarantee. Terminality means no change to another status, not a new forbidden-terminal-row-shape check. Count monotonicity and existence of a matching event/admission are not added cross-row guard predicates: tests use the correct insert/event/conditional-update sequence; no trigger tries to implement the excluded admit/transition service. Conditional WHERE includes expected revision and required current status/time; a zero-row update is followed by one bounded reread, never a retry loop.

Explicit structural-versus-service classification: an otherwise valid `EXPIRED → EXPIRED` or `REVOKED → REVOKED` UPDATE changing only `revocation_generation = old + 1` and `revision = old + 1` is structurally accepted by the frozen database guard, but is not an authorized owner action (service-forbidden). The guard rejects generation decreases and terminal changes to another status; it does not ban same-state terminal generation increases. Expiry's generation increment exactly once remains an excluded owner/service transition and replay obligation, not a database guarantee. Do not add a terminal-row DDL prohibition.

A single UPDATE combining otherwise structurally valid action changes is likewise structurally possible but service-forbidden: an `ACTIVE → LOCKED` update may also strictly increase `expires_at` while the old expiry is future and increment revision by one, with generations/count unchanged. This structural acceptance does not authorize a combined lock+extension owner action. Services must issue one named action/transition per conditional update; no new mixed-action trigger/check is authorized. The bounded S2 probes in §7.4 distinguish these successes from legal owner actions and preserve the frozen guard.

### 4.2 Admission guard

Select the parent into a local row, with no new row/advisory lock. Refuse if absent; mode differs; status is not ACTIVE; `clock_timestamp() < valid_from`; `clock_timestamp() >= expires_at`; non-NULL max and `admissions_used >= max_admissions`; or NEW.admission_no differs from parent.admissions_used+1. These refusals use one stable internal `P0001/SHARE_ADMISSION_REFUSED` diagnostic. It is a structural backstop, not the application-level detailed denial registry. Evaluate statement-time clock rather than transaction-stable now(). Do not check NEW.admitted_at against the window or synthesize an increment/event. Explicit max NULL is unlimited; zero/negative max remains a check failure.

The guard runs before constraints: a missing parent admission INSERT normally raises P0001 first, not 23503. Independently prove `fk_share_admission_grant` via an owner savepoint-scoped guard-disable probe plus its exact catalog, then roll back and verify enabled. Do not permanently remove the guard just to obtain an FK error. Mode/next-number/active/validity/limit guards may similarly precede CHECK/UNIQUE probes; isolate those probes with valid parent shapes or the narrowly scoped trigger-disable technique, retaining and rechecking all controls.

### 4.3 Append-only diagnostic

`sharing.reject_row_mutation()` always raises `P0001/SHARING_ROW_APPEND_ONLY`; no successful RETURN path for UPDATE/DELETE. Both table bindings are tested, including no-op UPDATE. Runtime attempts without UPDATE/DELETE privileges instead fail `42501` before triggers; keep these distinct.

## 5. Exact security inventory

Two new roles with the attributes in §3.4; no memberships, elevated attributes or owner-role grants to runtime. One new sharing schema owned by sharing_owner. PUBLIC has zero sharing schema, table, column and function ACL entries/effective privileges. Owner implicit rights are not confused with explicit runtime grants. Use aclexplode including default function ACL handling to detect PUBLIC EXECUTE; checking only information_schema grants is insufficient.

sharing_role's schema USAGE allow-set is exactly seven: `sharing, evidence, subject_number, customer_iam, organization_access, platform, audit`. No CREATE anywhere, no ivr/entitlement/activation schema grant, no PUBLIC schema CREATE. evidence_role retains its previous six-schema set and has no sharing USAGE/table/function privilege; all S1/S2 roles have no sharing/evidence expansion. No grant option. Existing V001 public-schema baseline is retained, not globally reconfigured.

Owned-table explicit privileges: sharing_role SELECT and INSERT on all three tables (six table-level ACL entries), no table-level UPDATE. Exactly eight grant-table column UPDATE entries: `status, admissions_used, revocation_generation, revision, expires_at, code_salt, code_hash, code_generation`. No admission/event UPDATE; no DELETE, TRUNCATE, REFERENCES, TRIGGER or extra column-level grants. Three functions × one explicit runtime EXECUTE; no evidence_role/S1/S2 execution. Effective non-extension user-function EXECUTE set for sharing_role is exactly five signatures: those three trigger helpers plus:

- `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)`.
- `audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)`.

No EXECUTE on `platform.idempotency_begin(text,text,text)`, `platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)` or `entitlement_metering.issue_first_trial` (existing exact signature from its inventory). The global SECURITY DEFINER user-service inventory stays exactly five, by names, arguments, owner, search_path and ACL. Evidence's existing seven effective executable signatures remain unchanged.

Cross-module sharing_role SELECT is column-only, exactly eight tables / 34 column ACL entries, no broad table SELECT:

| Table | Exact columns (count) |
| --- | --- |
| evidence.evidence_record | id, subject_id, status, source_type, source_ref_id, call_id, session_id, reservation_id, identity_at_capture, created_at (10) |
| evidence.evidence_artifact | evidence_id, media_object_id, artifact_role (3) |
| customer_iam.customer_account | id, status (2) |
| organization_access.organization | id, status (2) |
| organization_access.organization_membership | organization_id, account_id, role, status (4) |
| subject_number.business_subject | id, owner_account_id, owner_organization_id, revision (4) |
| subject_number.caller_number_binding | id, subject_id, status, verified_at, revision (5) |
| subject_number.subject_risk_state | caller_number_binding_id, risk_type, state, revision (4) |

No evidence.media_object or evidence.recording grant, evidence function EXECUTE, foreign write or foreign row lock. Enumerate every column in the eight tables: declared columns succeed, every undeclared SELECT fails 42501; `SELECT *` fails. Probe both FOR UPDATE and FOR SHARE using declared projections, every foreign INSERT/UPDATE/DELETE, platform/audit direct table writes, owned immutable-column writes, schema CREATE, DELETE and TRUNCATE. Use real SET LOCAL ROLE sharing_role, savepoints, and known rows to avoid “zero rows” masking privilege failures. Foreign schema USAGE and read grants do not permit row locks. Never widen an existing role allowlist merely because sharing_role now exists.

## 6. Behavioral slices and exact file ownership

Three slices are independently verifiable behaviors: install and isolate the storage boundary; prove lifecycle/history invariants; prove atomic count-once serialization. V009 is created atomically with its entire final frozen DDL in slice 1 and then frozen; later slices add witnesses, not a partially deployable schema. No file/module-only slice and no fourth speculative slice.

Path aliases (exact paths, not globs):

- M = `backend/src/main/resources/db/migration/V009__sharing.sql` (new, slice 1 only).
- T = `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`.
- C = `.github/workflows/ci.yml`.
- R = `backend/README.md`.
- P = `docs/gateflow/project-execution-control.md` (controller-owned bookkeeping only after accepted plan; preserve intake edit).
- I1 / E1 = `docs/gateflow/pa9-v009-sharing-foundation-s1-implementation.md` / `docs/gateflow/pa9-v009-sharing-foundation-s1-completion-1.md`.
- I2 / E2 = `docs/gateflow/pa9-v009-sharing-foundation-s2-implementation.md` / `docs/gateflow/pa9-v009-sharing-foundation-s2-completion-1.md`.
- I3 / E3 = `docs/gateflow/pa9-v009-sharing-foundation-s3-implementation.md` / `docs/gateflow/pa9-v009-sharing-foundation-s3-completion-1.md`.

| Slice | Exact allowed files | Prerequisites | Outcome and tests | Stop conditions |
| --- | --- | --- | --- | --- |
| S1: migrate and isolate sharing storage | M,T,C,R,I1,E1; P controller only | Accepted PA9 plan and separate implementation entry; base/digest/intake recheck; Docker-capable PG16 environment | Entire frozen V009; preserve/stage all old waves; exact catalog/ACL/FK/default/function/trigger evidence; three new methods below; Failsafe 60, Surefire 10 | Any authority conflict; V001–V008 change; extra object/grant; stale V008 fence; catalog/ACL/FK or cumulative failure; source/CI/XML mismatch; missing runtime evidence. |
| S2: preserve lifecycle and immutable history | T,C,R,I2,E2; P controller only | S1 accepted, migration bytes frozen | Two new methods; 31-check matrix, defaults, legal/illegal action and revision matrix, immutable/append-only controls; Failsafe 62, Surefire 10 | Predicate weakening, unexpected UNKNOWN treated as a new rule, missing legal/negative edge, nonunit revision accepted for a controlled mutation, cleanup leak, failing cumulative evidence. |
| S3: atomic admission and bounded races | T,C,R,I3,E3; P controller only | S2 accepted; valid committed fixture and local cleanup proof | Two new methods; count-once SQL transaction/replay/limit/rollback and two-connection orderings; Failsafe 64, Surefire 10 | Unbounded wait, sleep-only race, foreign row lock, count/event split, loser residue, leaked disabled trigger/deferred constraint, test/CI/XML mismatch or runtime failure. |

The plan artifact is immutable input to implementation, not an implementation allowed write. No application file, POM, old migration, test resource or authoritative design document is in any slice write set. Implementation and completion artifacts record work/evidence; they are not acceptance. P updates are part of accepted-plan/implementation bookkeeping, made by the controller at the relevant gate, never by this plan author and never blindly overwriting its current uncommitted checkpoint. README/CI updates belong to each implemented slice, reflecting 60→62→64 actual methods and bounded evidence rather than future pass claims. If fixes require other artifact names/files or migration changes after S1 acceptance, stop for explicit controller scope adjustment; do not invent additional paths.

## 7. Exact test inventory and assertions

### 7.1 Methods and report equality

Rename exactly one existing method:

`v001ThroughV008MigrateCleanPostgres16WithoutSkipping` → `v001ThroughV009MigrateCleanPostgres16WithoutSkipping`.

Retain all other 56 existing names/bodies except necessary cumulative role/fence bookkeeping; no deletion, disable, relaxed predicate, swallowed failure or reduced old-wave assertion.

Add exactly seven @Test methods (helpers are not extra tests):

| Slice | Exact method name | Responsibility |
| --- | --- | --- |
| S1 | v009SharingInventoryAndGuardShapesAreExact | 3/38/31 catalog; all 13 physical indexes (5/4/4, five secondary) by name, table, uniqueness, key-column order and DESC flag; keys/FKs/defaults, 3 functions/4 triggers, bytes and full/future fence |
| S1 | v009OwnerPublicRuntimeAndCrossModuleAclsAreExact | All positive/negative owner/PUBLIC/runtime/schema/function/34 cross-column ACLs |
| S1 | v009ForeignKeyAndRestrictDeleteBackstops | Six missing-parent/delete/update RESTRICT witnesses and exact catalog |
| S2 | v009ShapeDefaultsAndAppendOnlyBackstops | All checks, defaults, uniqueness/NOT NULL, immutable facts, append-only and rollback-isolated probes |
| S2 | v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops | All legal/illegal status edges, named action deltas, expiry/LOCKED→ACTIVE rotation, expected/stale/nonunit revisions; rollback-isolated early-expiry refusals, terminal/non-terminal same-state generation successes, ACTIVE same-state code-rotation structural-success/service-forbidden witness, and mixed-action structural-success/service-forbidden probes (§7.4), not an exhaustive SQL-shape classifier |
| S3 | v009AdmissionCountOnceLimitAndAtomicRollbackBackstops | SQL-only admission/event/count transaction, replay-before-limit, observation events, sentinel/error rollback |
| S3 | concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner | Real bounded races, commit/rollback and lock order outcomes |

Final 57−1+1+7 = 64 Failsafe methods; intermediate 60 and 62. Independently extract @Test method names from T, parse C's Python expected literal set, and parse all `<testcase name>` from the one Failsafe `TEST-*PostgresContainerIT.xml`: exact set and total equality 64, no duplicate names/cases, zero skipped/failures/errors. Do not count helper methods or just compare cardinalities. Update CI's count labels/error messages as well as expected names; extend the validator to assert source=expected=XML, using `src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` from its backend working directory. Preserve Docker preflight, exact tool pins, both jobs, contract checks, boot smoke and dependency firewall.

Surefire stays exactly 10: unchanged application/bootstrap/module tests, no new unit test class and no test provider/config change. Parse their XML separately for total 10 and zero skipped/failures/errors; never compare their names to the Failsafe expected set. Focused Failsafe invocations have only the selected subset and are not evidence for full CI set equality.

### 7.2 Cumulative migration chain, staged fence and immutable bytes

Retain targets 004→005→006→007 and every staged orphan-consent, late-FK/role/schema/function witness. Change the former unrestricted V008 migration to explicit target 008, assigned once to `stagedV008Migration`; capture `migrationsExecuted=1`, evidence's five tables/two roles/functions/triggers/ACLs, the existing `assertNoV009PlusObjects` while sharing is genuinely absent, and evidence_role-only evidence schema USAGE. Capture all no-sharing-role/schema/table conditions before target 009, not after and not as constants. Then unrestricted Flyway migrate applies exactly one V009; `initialMigration.migrationsExecuted=1`. Total ordered history contains nine successful SQL rows, scripts/descriptions 001–008 unchanged, row 009 script `V009__sharing.sql`, description `sharing`, type SQL, success true; no tenth row. `flyway.info()` has no pending migration; preserve `flyway.validate()` and clean-database checks.

The S1 bookkeeping edit is concrete and cumulative, not count-only: in `assertSchemasAndRoles` extend the exact schema and role sets with `sharing=sharing_owner`, `sharing_owner` and `sharing_role`, and remove its post-final zero-sharing assertions; capture those no-sharing schema/role conditions at the staged target-008 checkpoint instead. In `assertFunctionsAndTrigger`, retain the complete prior global function list and append the exact sharing functions `sharing.guard_share_grant`, `sharing.guard_share_admission` and `sharing.reject_row_mutation`; retain the complete prior trigger list and append `sharing.evidence_share_grant.trg_share_grant_guard`, `sharing.share_admission.trg_share_admission_guard`, `sharing.share_admission.trg_share_admission_immutable` and `sharing.share_access_event.trg_share_access_event_immutable`. Do not replace either inventory with a count.

At the explicit staged target-008 checkpoint, capture the actual sharing-absence assertions from both `assertNoV008PlusObjects` and `assertNoV009PlusObjects` while V008 is present and V009 is absent. Preserve each helper's other exact evidence/function-wave checks and all earlier staged evidence; neither staged helper nor its staged call may run against the final V009 database. At final V009, replace the V006 call at `:2459` and V007 call at `:2512` to `assertNoV008PlusObjects`, and the V008 call at `:2595` to `assertNoV009PlusObjects`, with the same exact live V009/no-V010 fence: exactly three sharing tables, two sharing roles, three sharing functions, four user triggers, no V010 object, and the global five SECURITY DEFINER service inventory. Keep all non-sharing evidence, function-wave and prior exact checks. `assertV006PresenceAndLateFk` has no sharing-absence assertion and needs no sharing-specific edit. The risk helper `assertRiskOpenClearReopenRevisionProtocol` has a real direct sharing-schema-absence assertion at `:18125`: capture that absence at staged target 008, then remove or replace only that direct sharing-absence assertion so the helper remains green after V009; retain every non-sharing risk-transition assertion and behavior. Rename `v001ThroughV008MigrateCleanPostgres16WithoutSkipping` to the staged target-008→V009 chain name required by §7.1 and update the source `@Test` set, CI Python expected literal set and Failsafe XML comparison together; the exact set must remain source=CI=XML at 64 (60 and 62 at intermediate staged checkpoints). These are test/CI bookkeeping edits only: no schema expansion, DDL, ACL or slice change.

Implement that final live fence as `assertV009LiveFence(Connection)`, invoked at all three replacement call sites (`:2459`, `:2512`, `:2595`). Its executed assertions must be an equivalence/superset of `assertNoV008PlusObjects`: no non-sharing assertion may be silently removed or relegated only to staged target 008. Only sharing-absence assertions flip to exact sharing presence at final V009. The helper must explicitly execute these checks against the live final database:

- Exact V005 `ivr` six-table set: `call_event`, `call_leg`, `call_record`, `inbound_session`, `ivr_decision`, `outbound_call_task`.
- Exact V006 `entitlement_metering` seven-table set: `entitlement_balance`, `entitlement_grant`, `entitlement_issuance`, `trial_grant`, `trial_policy_version`, `usage_fact`, `usage_reservation`.
- Exact V007 `activation_consent` three-table set: `consent_record`, `service_activation`, `service_activation_transition`.
- Exact V008 `evidence` five-table set: `evidence_artifact`, `evidence_event`, `evidence_record`, `media_object`, `recording`; exactly the two evidence roles `evidence_owner` and `evidence_role`, retained in the complete exact global role inventory.
- `entitlement_metering.issue_first_trial` as the only entitlement_metering SECURITY DEFINER function; exactly the five global user SECURITY DEFINER services `audit.append_fact`, `entitlement_metering.issue_first_trial`, `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`, with no extra SECURITY DEFINER function in any user schema.
- Absence of every future-service name `select_call_action`, `record_activation_next_action`, `reserve_call_usage`, `settle_call_usage`, `publish_trial_policy`, `disable_trial_policy` across `pg_proc`; absence of legacy `ivr.call_task` and `ivr.call_session`; presence of `catalog_order.fk_customer_order_consent` on `catalog_order.customer_order`.
- Each of `evidence_share_grant`, `share_admission`, `share_access_event` absent from `activation_consent`, `entitlement_metering` and `evidence`, but present exactly in `sharing` as its complete three-table set. These misplaced-name exclusions remain final assertions, not staged-only evidence.
- Exact sharing schema/owner `sharing=sharing_owner`, two roles `sharing_owner`/`sharing_role`, three local functions `sharing.guard_share_grant`, `sharing.guard_share_admission`, `sharing.reject_row_mutation`, and four qualified user triggers `sharing.evidence_share_grant.trg_share_grant_guard`, `sharing.share_admission.trg_share_admission_guard`, `sharing.share_admission.trg_share_admission_immutable`, `sharing.share_access_event.trg_share_access_event_immutable`; no V010 or extra sharing object. Preserve complete prior global schema/role/function/trigger inventories and exact nine migration resources/history rows; compare exact sets/identities, not counts alone.

At staged target 008, retain the genuine sharing schema/roles/tables absence from both `assertNoV008PlusObjects` and `assertNoV009PlusObjects`, together with their non-sharing assertions. Neither staged absence helper runs at final V009. Before accepting S1, extract the executed assertion list of `assertV009LiveFence` at each of the three final call sites and compare it row by row with `assertNoV008PlusObjects`: every non-sharing assertion must still execute, sharing absence must become exact sharing presence, and the no-V010/exact global inventories must remain. This clarifies assertion preservation only; the risk-helper handling and `assertV006PresenceAndLateFk` no-sharing-edit rule above are unchanged, with no DDL, ACL, table, service, slice or test-count change.

The retained V008 inventory method still asserts full current evidence catalog/ACL/FK/control inventory, frozen V007 prefix flags and captured V008 no-V009 evidence at the staged target-008 checkpoint. The staged `assertNoV008PlusObjects` and `assertNoV009PlusObjects` checks preserve their other exact evidence/function-wave checks there; all three final inventory calls use the same full-V009/no-V010 live fence, including exact nine migration resources/history rows (V901 broken fixture separate), exactly three sharing tables/two roles/three local functions/four triggers and no extra application objects in sharing; global five-service inventory unchanged. Rename or add a helper for that live fence without weakening either staged helper's remaining checks. Do not simply delete the no-sharing assertions. No fabricated V010 schema-name denylist substitutes for exact set checks.

Rename digest helper to cover V001–V008, invoked by both retained V008 and new V009 inventory tests. Pin these SHA256 values from `git show BASE:path` canonical bytes, not current checkout-generated values:

| Migration | Canonical Git-byte SHA256 |
| --- | --- |
| V001__platform_schemas_roles_idempotency_outbox_audit.sql | 946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045 |
| V002__identity_organization_and_employee.sql | 534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5 |
| V003__subject_number_and_claim.sql | 1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1 |
| V004__catalog_order_and_payment.sql | a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc |
| V005__ivr_task_session_call.sql | 2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310 |
| V006__trial_entitlement_issuance_reservation_usage.sql | 830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475 |
| V007__activation_and_consent.sql | bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659 |
| V008__evidence_media.sql | 2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e |

Classpath test follows existing `readClasspathBytes`→`toLfBytes`→`sha256Hex` for checkout CRLF representation. Separately compare each unchanged repository blob to BASE and ensure no old migration path appears in the implementation diff; LF-normalized runtime hashes alone are not proof of exact Git-byte identity. Pin V009 at slice-1 acceptance for later slices. Never rewrite old resources to normalize line endings.

### 7.3 Catalog and CHECK oracle strategy

Use existing queryStrings/queryLong helpers, pg_attribute/format_type/pg_attrdef, pg_constraint, pg_index/pg_indexes, pg_trigger and pg_proc. Assert exact ordered column name/type/nullability/default expressions with `pg_get_expr(adbin,adrelid)` and no dropped/generated/identity surprises; exact named PK/UNIQUE/FK definitions and actions; all 13 physical indexes, per-table 5/4/4 including exactly five secondary definitions, against the independent named inventory in §3.3; exact function source guard fragments, search_path, owner, security/language/return type; exact trigger enabled/timing/operations/table→function binding. For every physical index assert name, owning table, uniqueness, ordered key columns and each key's DESC flag (only `occurred_at` in `ix_share_access_event_grant` is DESC), plus §3.3's index properties and constraint relationships. Never derive expected index definitions from the actual migration/catalog. Match full sets, not LIKE/count-only checks.

Create a literal `V009_CHECK_DEF_MAP` analogous to V008's 39-entry map, three tables / 17+5+9 entries. PostgreSQL 16 `pg_get_constraintdef(oid)` is canonical: parenthesized comparisons, `::text`, IN→ANY ARRAY and BETWEEN deparse are expected. Generate the expected deparse in a rollback-only test-owned temporary reference relation using the **literal plan predicates**, same column types/names, then freeze and inspect those strings as Java literals; never use actual migration-derived predicate strings as their own oracle and never normalize away casts/operators/grouping/NULL semantics. During author implementation a focused PG16 run supplies exact deparse spelling; any unexpected semantic change stops. A later literal-map assertion must fail on any changed predicate, including a named check with a weaker body. No online research or guessed PostgreSQL deparse strings needed in this plan.

For each of 31 checks, supply a valid committed-or-savepoint fixture and at least one isolated concrete-false negative with exact `23514` and server constraint name, re-read no residue; isolate earlier alphabetical CHECK/trigger failures with correct other fields. For `ck_share_grant_code_generation`, a negative generation needs a narrowly scoped savepoint-only removal of verification_shape (otherwise that shape can fail first); restore via rollback-to-savepoint and reassert full check catalog. To isolate verification_mode/status invalid enums from shape checks, use existing `v008S2IsolatedStatusCheck` pattern: savepoint-scoped drop only overlapping check(s), never permanent schema edits, rollback and catalog equality. Admission mode/no negatives may need scoped guard disable plus overlap isolation. Other checks must remain present and enabled. These are test-only transactional probes under owner, not changes to delivered DDL.

Boundary matrix: nonblank IDs at 1/128 vs empty/whitespace/129; salt 22/256 valid URL-safe vs 21/257/disallowed character; lowercase 64-hex vs uppercase/nonhex/63/65; mode NONE/code/mobile/member combinations, code-gen 0 vs ≥1, required org; expiry > valid_from vs equality/reversal; admissions 0, limit NULL, positive limit/exact max vs negative/zero/over-limit; JSON object vs scalar/array, required top-level members, concrete mismatching evidence/creator/org/mode and invalid digest/HMAC/keyVersion; 15 event types/four results, wrong event domain, invalid risk/payload hash; required admission ID for six scoped types, pre-admission events with NULL; ADMISSION_GRANTED six presence fields plus mode, and recipient pair required for member. Test every conjunct of snapshot bindings with concrete false values, not only one negative per large check.

Explicit UNKNOWN probes: missing/null nested evidence/creator/mode/hash/mobile values where the literal predicate yields UNKNOWN may pass; key presence is not typed value validation. ADMISSION_GRANTED generation/expiry values are presence-only (including JSON null); recipient pair keys are present-only. Preserve these witnesses and document limitations without strengthening DDL. Defaults: insert grant omitting seven defaulted fields, verify exact values and transaction-time now(); admission/event timestamps likewise; omitted required nondefault fields give `23502` and exact column. UUID/type failures are not CHECK evidence. PK and each UNIQUE get independent `23505`+exact name; isolate admission guard before duplicate key/number attempts (or probe with scoped guard disabled) so refusal does not masquerade as uniqueness evidence.

### 7.4 FK, ACL, lifecycle and rollback witnesses

- For each of six FKs, test valid parent, missing non-NULL parent (23503 + exact name, except guard precedence as §4.2), permitted NULL for the two nullable references, parent DELETE RESTRICT and parent key UPDATE RESTRICT. Owner append/identity guards may fire first: use rollback-only savepoints disabling only the masking named guard/immutable trigger, or scoped RI-only clone from existing helper patterns, then roll back and assert the original controls/catalog. Never assert a 23503 when a known P0001 must fire first. No broad `session_replication_role`, DISABLE TRIGGER ALL or permanent constraint removal.
- ACL tests enumerate all explicit/effective inventory in §5, all old role denies and PUBLIC default ACLs; positive parent grant SELECT FOR UPDATE under sharing_role must succeed using its declared mutable column privilege, while foreign locks fail 42501. Table owners and schema owners must be sharing_owner, not migration user/runtime. Verify no role memberships/grant options and no service additions.
- Lifecycle method iterates all 16 old/new status pairs: same-state cases distinguish named actions, no-op/revision-only writes and structurally accepted but service-forbidden mutations; six directed status changes listed in §4.1 legal, remaining directed changes rejected. For each named legal change test revision old+1 success, old/old−1/old+2 P0001, and WHERE stale revision returns zero then bounded reread. Test both terminal states against every different status, LOCKED code unlock succeeds only with all replacement/+1 facts, and non-code unlock fails. Independently mutate all 12 frozen columns under owner (P0001) and runtime (42501); mutable shrink/generation/partial salt/hash replacement failures use exact internal labels. Assert generation decreases fail with P0001/SHARE_GRANT_GENERATION_INVALID even on same-state writes, using otherwise valid nonnegative resulting values. Check exact revoke/expiry generation deltas only when `OLD.status IS DISTINCT FROM NEW.status`, preserve ACTIVE → LOCKED generation neutrality and exact code-rotation coupling, and require the same-state terminal successes below; a blanket named-status-edge allow-list for generation increases is an incorrect oracle. Same-state revision-only behavior and revision>=1 check are separately documented, not confused with action revision rules. This is a directed-edge/named-action matrix, not an exhaustive mutable SQL-shape classifier.
- Exact same-state terminal probes in `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops`: for each separately committed valid method-local `EXPIRED` and `REVOKED` fixture established through its named transition, capture its full baseline row and history counts. BEGIN under sharing_role, lock/read that grant, SAVEPOINT, then execute `UPDATE sharing.evidence_share_grant SET revocation_generation = revocation_generation + 1, revision = revision + 1 WHERE id = :id AND status = :terminal_status AND revision = :old_revision` with bound values from that read (`:terminal_status` separately EXPIRED and REVOKED). Expect structural success, exactly one affected row, unchanged terminal status and all other columns, and exactly +1 for both specified fields; no exception/SQLSTATE refusal is expected. Classify both as service-forbidden, not authorized expiry/revoke retries or proof of exactly-once behavior. ROLLBACK TO SAVEPOINT, assert the entire baseline row/history restored, then ROLLBACK the transaction; a fresh connection must observe the original row/generations/revision and unchanged admission/event counts. Keep the frozen guard enabled throughout; no successful probe is committed.
- Exact non-terminal same-state generation witness in that same method: use a separately committed valid method-local `LOCKED` fixture, BEGIN under `sharing_role`, lock/read it, SAVEPOINT, and execute `UPDATE sharing.evidence_share_grant SET revocation_generation = revocation_generation + 1, revision = revision + 1 WHERE id = :id AND status = 'LOCKED' AND revision = :old_revision` with every other field unchanged. Expect structural success, exactly one affected row, no SQLSTATE, unchanged `LOCKED` status and all other columns, and exactly +1 for both specified fields. Classify this `LOCKED → LOCKED` shape as service-forbidden, not an owner action or exactly-once proof; retain the generation-decrease negative and terminal same-state successes. Roll back to the savepoint, assert full row/history restoration, roll back the transaction, and verify the exact baseline and unchanged admission/event counts from a fresh connection.
- Exact ACTIVE rotation residual witness in that same method: use a valid method-local `EXTRACTION_CODE` grant fixture with `status = 'ACTIVE'`, `code_salt`, `code_hash`, `code_generation >= 1`, future expiry, and all other fields valid. With the frozen guard enabled, BEGIN under `sharing_role`, lock/read the full baseline row and admission/event counts, SAVEPOINT, then execute an exact same-state replacement setting fresh valid `code_salt` and `code_hash`, `code_generation = old + 1`, `revocation_generation = old + 1`, and `revision = old + 1`, with every other field unchanged and a predicate on `id`, `status = 'ACTIVE'`, and `revision = :old_revision`. Expect structural success, exactly one affected row, unchanged `ACTIVE` status, exact +1 for both generations and revision, and every other column unchanged. Classify it as service-forbidden: it is not an owner action, does not unlock anything, and future services must not treat it as a `CODE_ROTATED` success. ROLLBACK TO SAVEPOINT, assert the complete baseline row and history restoration, then ROLLBACK; a fresh connection must observe the original ACTIVE row and unchanged admission/event counts with no event residue. Preserve the separate legal `LOCKED → ACTIVE` extraction-code rotation witness as the sole legal code-rotation action; no guard is disabled, no DDL prohibition is added, and no successful probe is committed.
- Early-expiry refusal witnesses in that same method: because a named expiry transition necessarily requires `clock_timestamp() >= OLD.expires_at`, create each synthetic valid `EXPIRED`-target fixture with a method-local direct `INSERT` solely for this test (future `expires_at`, valid fields/checks/FKs, no production creation-path implication). With the frozen guard enabled, use separate `ACTIVE` and `LOCKED` rows whose old expiry is safely in the future; under `BEGIN`/row lock/`SAVEPOINT`, attempt `status = 'EXPIRED'`, `revocation_generation = old + 1` and `revision = old + 1`, leaving all other fields unchanged. Expect exactly `P0001/SHARE_GRANT_EXPIRY_INVALID`, zero affected-row mutation and guard enabled for both `ACTIVE → EXPIRED` and `LOCKED → EXPIRED`. `ROLLBACK TO SAVEPOINT`, then transaction `ROLLBACK`; a fresh connection must show the exact pre-probe baseline row/history and no committed admission/event change. Delete/clean the direct fixtures in the method-local cleanup path and assert no fixture residue; this synthetic INSERT is not a production creation path.
- Explicit extension-predicate witnesses in the same S2 method, each isolated under sharing_role with the frozen guard enabled, lock/read baseline, SAVEPOINT and expected revision: (1) lock-only ACTIVE → LOCKED, expiry unchanged, revision +1, generations/count unchanged succeeds structurally; (2) pure extension ACTIVE → ACTIVE, strict future expiry increase, revision +1, generations/count unchanged succeeds structurally; (3) mixed ACTIVE → LOCKED plus strict future expiry increase succeeds structurally but is service-forbidden, as detailed below; (4) for each `OLD.status` in LOCKED/EXPIRED/REVOKED, a same-state attempted strict expiry increase with revision +1 and otherwise unchanged fields is structurally refused with P0001/SHARE_GRANT_EXTENSION_INVALID. Use valid method-local stored fixtures with old expiry safely future for these probes so (4) isolates the non-ACTIVE OLD.status predicate, not the clock predicate. For (1), no expiry-increase predicate is invoked; for (2)/(3), `OLD.status = 'ACTIVE'` and `clock_timestamp() < OLD.expires_at` hold regardless of the resulting NEW.status. Successful probes affect exactly one row with only the specified deltas; refused probes leave no mutation. After each, ROLLBACK TO SAVEPOINT, assert full baseline row/history restoration, then ROLLBACK; verify original row and unchanged admission/event counts from a fresh connection. No successful probe mutation is committed.
- Exact mixed-action probe in the same S2 method: use a separately committed valid method-local ACTIVE grant with synthetic old expiry safely in the future and baseline generation/count/history captured. BEGIN under sharing_role, lock/read it, SAVEPOINT, then execute `UPDATE sharing.evidence_share_grant SET status = 'LOCKED', expires_at = expires_at + interval '1 hour', revision = revision + 1 WHERE id = :id AND status = 'ACTIVE' AND revision = :old_revision AND clock_timestamp() < expires_at`. The one-hour delta is a synthetic witness, not production expiry policy. Expect structural success: the extension predicate checks OLD.status ACTIVE and future OLD.expires_at, not NEW.status ACTIVE. Assert exactly one affected row, NEW.status LOCKED, strictly increased expiry by that delta, revision +1, unchanged generations/count and every other column. Classify combined lock+extension as service-forbidden; services must use one named action/transition per conditional update. ROLLBACK TO SAVEPOINT and assert full baseline row/history restoration, then ROLLBACK; a fresh connection must observe the original ACTIVE status/expiry/revision/generations/count and unchanged admission/event counts. No guard is disabled and no probe mutation is committed. These rollback assertions are required later runtime evidence, not execution claimed by this plan fix.
- Admission clock probes use future valid_from, already-past expires_at, and an equal statement-clock boundary using a saved timestamp at/before clock_timestamp(), all with otherwise valid stored intervals; no sleep-dependent precision assertion. Confirm long-lived transaction now() cannot admit after actual expiry. Tests use explicit synthetic past/future timestamps and limits, not production policy values.
- SQL-only success sequence on one connection under sharing_role: BEGIN → grant FOR UPDATE → current read → existing-key lookup → INSERT admission(next no) → INSERT ADMISSION_GRANTED with redacted synthetic witness → conditional grant count/revision update (exactly one row) → COMMIT. No SQL function/Java application service created. At limit, existing-key lookup returns original fact/event/count before any INSERT; new key denied, status/generation unchanged. LOCKED permits reading original fact without new INSERT; it blocks new keys. This does not issue/reconstruct a session or authorize playback.
- PAGE_OPENED/PREFETCH_IGNORED/VERIFICATION_FAILED insert only events and leave count/admission set unchanged. TICKET_FAILED shape witness appends without count change; no ticket issuance. Exact-key event replay uses read-before-write in harness and unique domain/id witness, no new event/count. Do not claim production deterministic hashing or authorization is implemented.
- Sentinel rollback after admission+event+count, rollback after transition+event+`audit.append_fact`, duplicate event/key/number constraint failure, guard refusal and forced zero-row expected-revision update each leave all affected rows and grant state/revision unchanged on a fresh connection. For zero-row update the harness explicitly rolls back; zero rows do not inherently abort PostgreSQL. Include a rollback-only Outbox append through existing `platform.outbox_append`, prove nested side effects absent, preserve five-function baseline. A committed admission followed by an independent synthetic ticket-failure event remains committed exactly once, with no compensating count decrement. Not a rollback/down migration.

### 7.5 Bounded concurrent witnesses

Use two real connections, READ COMMITTED, distinct `application_name`, existing `configureBlockedWriter` (`statement_timeout=20000` ms), `awaitBlockedBackend` (15-second bound), `awaitFuture` (30-second bound), explicit success/exception capture and finally rollback/close/executor shutdown+bounded termination. Observe `pg_stat_activity` lock wait, not sleeps as proof. The test's sequential main thread must release winner while loser future is blocked; do not join before committing/rolling back winner.

Cases inside the single concurrency method:

1. Grant share_id collision and token-hash collision separately: winner commits, unique loser 23505 with exact constraint, no loser grant/history; winner rollback variant allows waiting insert to succeed. Event domain/id collision likewise pins `uq_share_access_event_domain_id`.
2. Same admission key: both transactions lock the same grant before lookup. Winner inserts/event/count commits; waiting contender re-reads then finds existing key and returns the same admission ID, one admission/event/count. Repeat at max=1; same-key replay is not routed through BEFORE INSERT and still succeeds as a fact lookup.
3. Distinct keys at last synthetic slot: winner commits admission/event/count; waiter sees the updated count and refuses without insertion; exactly one admission/event and final count=max, unchanged status/generation. Repeat winner rollback: waiter can claim the slot, no original winner history. Unique admission_no/key direct collision probes are independently isolated as §7.3, not confused with a correctly serialized service sequence.
4. Admission vs revoke, extraction-code rotation and expiry: each transition-wins ordering blocks admission until commit, then current status/generation/expiry check denies without count/history. For rotation, waiter carries the pre-rotation expected generation and the SQL harness refuses mismatch before insert (guard alone cannot know stale verified input). In admission-wins ordering the admission commits once, lifecycle update waits then commits exactly one generation/revision transition, admission/event bytes unchanged; no assertion of real session revocation/renewal.
5. Admission vs LOCK: lock-first denies new admission; admission-first keeps its fact and then LOCKED changes no generation. Existing-key read remains possible without a new insert. Runtime row-lock privilege remains owner-local.

After every case a fresh connection asserts exact winner/loser IDs, event identity, count, status, generations and revisions; loser-prepared sentinel rows are absent. Bound races do not prove production throughput/deadlock freedom or service authorization.

## 8. Fixture creation and FK-safe cleanup

Prefer rollback-only fixtures for probes. For real commits/races, extend test-local V008 fixture patterns, do not modify test resources or introduce application builders. Register every generated ID before operations so failure paths can clean them. Create valid parents in order: employee/policy/grant/balance/reservation only if using the full existing V008 reservation fixture; customer account; optional organization; subject; inbound session/call if the chosen V008 fixture requires it; declared recording and media/validated shape as appropriate; matching evidence record + EVIDENCE_CREATED event in one transaction, with exact call/source/subject/snapshot bindings and deferred cycle valid at COMMIT. Use existing `v008S2Fixture`, `v008S2Pair` / `v008InsertValidRecordAndCreationEvent`, `insertCustomerAccount` and owner switching, not invalid convenience orphan records. Insert a SOLIDIFIED evidence pair for sharing positive prerequisites; creating a non-SOLIDIFIED grant is not prevented by the FK and remains an excluded service rule.

Create synthetic valid grant snapshots with all normative typed creation/verification/content fields, digests and explicit test timestamps/limits; code fixtures use 22+ URL-safe salt and lowercase 64-hex placeholder digest, never actual KDF or policy choices. Member fixtures have organization FK and original recipient witness keys. Fixtures must not require unknown production Q12/Q22 input; all values are labelled synthetic DB-shape probes, no ticket TTL is selected or implemented.

Cleanup after committed probes in one method-local transaction:

1. Under sharing_owner disable only `trg_share_access_event_immutable` and `trg_share_admission_immutable`; delete registered access-event IDs first, admission IDs second, grant IDs last (never all table rows); enable both before COMMIT and assert enabled. Grant guard is UPDATE-only and need not be disabled for scoped DELETE.
2. After all sharing references are removed, invoke/extend existing V008 scoped cleanup: artifacts and creation/events, evidence record, recording/media in FK-safe order, with the two V008 cycle constraints DEFERRED during reciprocal deletion and IMMEDIATE before commit; restore its three immutable triggers exactly as current helper requires. Deferred cycle trigger INSERT/UPDATE checks are not disabled globally. If a split helper is needed, retain its original cumulative behavior and restrict new IDs to the fixture.
3. Remove optional reservation→balance→entitlement grant→policy→employee as existing helper does with its one named entitlement append trigger restored, then call/session links/rows and subject, optional memberships→organization and account last, after checking all referencing rows are gone. Register any extra Outbox/audit rows and remove only those with their existing owner-local cleanup pattern, restoring named append triggers; rollback-only nested-function probes avoid committed platform/audit cleanup where possible.
4. On any cleanup failure roll back the entire cleanup transaction, which also rolls back trigger/constraint state changes; never COMMIT from finally. Finally closes/rolls back connections; restoration exceptions must not be swallowed. Fresh verification checks every registered ID absent, named trigger states `O`, both V008 FK deferrability flags unchanged, exact catalog/check maps unchanged. Failed restoration or leaked rows fails the test, not a warning.

All isolated constraint/trigger changes in negative probes use savepoints and unconditional rollback-to-savepoint, then reassert catalog/trigger state even on an unexpected result. No foreign data reset, TRUNCATE, all-row cleanup, permanently disabled trigger or owner privilege leak.

## 9. Exact validation commands and evidence sequence

These are commands for a separately authorized implementation gate; **none is run in this plan gate**. Backend working directory, existing Java 21.0.13+9 / Maven wrapper 3.9.9 / Docker prerequisite, no `-DskipTests`, `-DskipITs` or disabled failIfNoTests. Docker absence is a blocker, not a skipped success. Preserve focused XML/logs outside target before the next invocation overwrites them. Exit 124 is timeout/failure, not evidence.

1. Read-only diff scope/old migration blob/hash checks, `git diff --check`; verify Java/Maven exact pins and Docker availability. Default build: `timeout 600s ./mvnw -B -ntp verify` (10 Surefire, no Failsafe execution).
2. S1 focused: `timeout 600s ./mvnw -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v001ThroughV009MigrateCleanPostgres16WithoutSkipping+v009SharingInventoryAndGuardShapesAreExact+v009OwnerPublicRuntimeAndCrossModuleAclsAreExact+v009ForeignKeyAndRestrictDeleteBackstops' verify` (4 Failsafe methods).
3. S2 focused: `timeout 600s ./mvnw -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009ShapeDefaultsAndAppendOnlyBackstops+v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops' verify` (2).
4. S3 focused: `timeout 900s ./mvnw -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009AdmissionCountOnceLimitAndAtomicRollbackBackstops+concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner' verify` (2).
5. Targeted cumulative fence regression before final: `timeout 900s ./mvnw -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v007ForeignKeyAndLateOrderConsentFkBackstops+v008EvidenceInventoryAndGuardShapesAreExact+v008OwnerPublicRuntimeAndCrossModuleAclsAreExact+v008ForeignKeyAndDeferredCreationCycleFkBackstops+v008CreationCycleBothOrdersAndTerminalLifecycleBackstops' verify` (5).
6. One final bounded full run after all code/docs/CI inventory changes: `timeout 1500s ./mvnw -B -ntp -Pcontainer-smoke verify` (64 Failsafe +10 Surefire, all zero failures/errors/skips). At S1/S2 checkpoints the same bounded full command must prove cumulative 60/62 if the controller is accepting those slices; avoid duplicate final full runs without a source change. No timeout widening without recording the blocker.
7. Parse final source/CI/XML equality and Surefire totals as §7.1; retain actual source hashes, commit/diff identity, exact command/exit/log/XML hashes and report paths. Execute CI's existing boot smoke/jar firewall and unchanged contract-regression steps in the appropriate CI environment; local Maven alone is not a claim of remote CI green. Scope/byte/diff checks conclude acceptance evidence.

Focused method expressions use Failsafe `-Dit.test`, not Surefire `-Dtest`. Running `verify` rather than only integration-test preserves Failsafe failure propagation. Full XML belongs to `backend/target/failsafe-reports/TEST-*PostgresContainerIT.xml`; ten separate unit/module cases belong to `backend/target/surefire-reports/TEST-*.xml`. Existing broken migration fixture continues to prove visible Flyway failure and rollback through its unchanged test.

## 10. Risks, open questions and no-over-design judgement

No blocking open question prevents this persistence plan. Known boundaries must remain explicit:

| Residual / failure risk | Owner and destination |
| --- | --- |
| CHECK UNKNOWN accepts some absent/null nested JSON; JSON presence is not semantic validation; independent admission/event grant FKs do not enforce pair consistency | Sharing/Backend/Security; later sharing-service typed semantic validation and Q26 mapping; no unauthorized schema hardening here |
| Guard cannot prove caller held grant lock, one admission causes one count, event existed, correct verified input, or SQL transaction used expected revision | Sharing/Backend; excluded admit/transition services on the same connection; SQL harness is backstop/transaction evidence only |
| Same-state EXPIRED/REVOKED revocation_generation +1 with revision +1 is structurally accepted but service-forbidden; the guard does not enforce expiry's exactly-once increment | Sharing/Backend; excluded owner transition/replay service, especially expiry persistence and deterministic same-action replay; §7.4 S2 structural-success/rollback evidence is not service authorization |
| Same-state ACTIVE extraction-code salt/hash replacement with code_generation +1, revocation_generation +1 and revision +1 is structurally accepted but service-forbidden; it is not an owner action, unlock, or `CODE_ROTATED` success | Sharing/Backend; excluded owner transition service destination; §7.4 S2 structural-success/rollback evidence, no ACTIVE→ACTIVE rotation action and no new DDL prohibition |
| One UPDATE can mix otherwise structurally valid action changes (ACTIVE → LOCKED plus strict expiry increase); structurally accepted but not an authorized owner action | Sharing/Backend; excluded owner transition service must issue one named action/transition per conditional update; §7.4 S2 probe and rollback evidence, no mixed-action DDL ban |
| Shareable SOLIDIFIED projection, creation/action fingerprint/hash derivation, replay ordering, current ownership/risk/member checks | Evidence/Sharing/Security; later owner-collaborator/services and access-policy design, not SQL service expansion |
| KDF/code/SMS/limits/expiry/rate policy Q12 and ticket/skew/revocation Q22 absent | Security/Product/Sharing; policy approval and later service/ticket work; no production values guessed |
| Legal retention/erasure, object lock/media/KMS, production deployment identity Q3/Q11/Q17/Q23 | Legal/Security/Infrastructure/Platform/Database; policy/storage/deployment gates |
| PostgreSQL deparse spelling and privileged provisioning/environment prerequisites | Backend/Database/QA; focused PostgreSQL 16/Flyway tests before slice acceptance; exact oracle, never loosen assertions |
| Cumulative tests/CI expected-set drift; cleanup/blocked-writer failures | Implementer/Backend QA; per-slice source=CI=XML checks and local cleanup assertions; fail loudly, do not widen timeouts blindly |
| PA8 deferred missing nonunit revision negative witness | Evidence/Backend QA; post-PA8 test hardening remains separate; PA9 adds full nonunit revision negatives for its own grant, no V008 implementation change |
| Pre-existing dirty controller and untracked .codex / Windows git indirection | Controller; preserve intake bytes, distinguish task delta, use read-only explicit Git metadata; no repository repair or unauthorized cleanup |
| Runtime evidence not executed by planner; remote CI/production readiness unproven | Controller/QA; implementation acceptance/CI and later production acceptance, not plan claims |

Why not over-designed: exactly three existing-design tables, 31 checks, six ordinary FKs, five secondary indexes, three local trigger helpers, four triggers and exact privileges; no new policy, table, service, action, durable column, infrastructure or runtime dependency. Three slices correspond to actual persistence guarantees, not speculative layering. No drift: all changed-file permissions map directly to V009 persistence or its cumulative executable evidence and gate bookkeeping. Service/risk/ticket policy is named as residual ownership, not silently implemented or described as proven.

## 11. Implementation completion report contract

Each later slice report must contain:

1. Task/work-unit/gate/slice identifiers, accepted plan path+SHA256, base/head/source diff identity and exact allowed/actual changed paths; separate controller bookkeeping and intake dirt.
2. Decisions implemented against this plan, exact inventory totals/ACL sets and any discrepancy or stopped authority conflict (no self-approved deviation).
3. Exact added/renamed methods and full source=CI=Failsafe set/count (60/62/64); Surefire=10 separately; zero failures/errors/skips or precise NOT RUN/blocker.
4. Commands actually run, tool/container versions, elapsed bounds, exit codes, retained focused and full log/XML hashes/locations and source provenance. Do not present planned commands as executed evidence.
5. Check/constraint SQLSTATE+name matrix, guard diagnostic evidence, legal/negative lifecycle/revision matrix, rollback/commit/race ordering outcomes, fresh-connection no-residue and trigger/deferred-constraint restoration evidence.
6. V001–V008 canonical Git-byte hash/equality results; frozen V009 bytes after S1; migration history and staged-prefix/live-fence proof; CI/README consistency and unchanged application/function baseline.
7. Residual risks with owner/destination, unresolved blockers, non-goal boundary and author completion status. Do not declare gate acceptance, review pass, production readiness, merge/PR state or external CI success without controller evidence/authority.

## 12. Plan artifact validation and author completion

The only repository write authorized and performed by this author is the new plan at `docs/gateflow/pa9-v009-sharing-foundation-plan.md`. Plan SHA256 is computed from final artifact bytes after writing and reported externally in the author completion message; embedding its own hash would be circular. Later implementation records must quote that externally supplied hash and pin the accepted artifact rather than invent a self-hash.

Author validation: run read-only `git diff --check`, new-file `git diff --no-index --check /dev/null <artifact>` (untracked files are not covered by ordinary diff), verify HEAD/base and task delta, compare baseline controller diff/SHA and .codex SHA, inspect exact untracked list, confirm no existing tracked file changed by this author and no application/test/migration execution occurred. Existing controller dirt is reported, not discarded or claimed as this author's work.

Author completion statement: this document supplies the executable plan, exact file boundaries, three behavioral slices, seven new methods plus one rename, 57→60→62→64 cumulative Failsafe progression and unchanged ten Surefire tests. No unresolved blocking incompatibility was found. No implementation, Maven, Docker, PostgreSQL, network call, Git mutation or later gate is entered. Stop after plan-artifact validation; status is `plan-complete` only, subject to subsequent independent gate authorization.
