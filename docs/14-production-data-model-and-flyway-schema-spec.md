# A0 Production Data Model and Initial Flyway Schema Specification

- Task label: `a0-spec-s1-impl-ds-20261022-01`; bounded fix labels `a0-spec-s1-fix1-ds-20261022-01` and `a0-spec-s1-fix2-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `implementation`, Slice S1 (V001-V003) only; current state is bounded fix 2 after independent S1 re-review round 1 failed on F-10/F-11/F-12
- Status: S1 fix 2 applied to the adjudicated findings; independent re-review pending. S2 (V004-V007) and S3 (V008-V009) are not authored in this document.
- Date: 2026-10-22
- Accepted plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`, SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`
- Accepted plan review: `docs/reviews/plan-review-20261022-174730.md`, SHA256 `C2432DA173BB4CB61D5554B1A2BF77C7DC5FA4AE79CE5CED1118741BBA34FDCA`
- S1 review (fix 1): `docs/reviews/code-review-20261022-181806.md`, SHA256 `E813BFDA4BA09FC1E99416FFB878CD0739D2EEAA1C5C6EDC7C36F1653D7E4726`; F-01 through F-09 accepted for fix 1
- S1 re-review round 1 (fix 2): `docs/reviews/code-review-20261022-184720.md`, SHA256 `0774D8B9B38C22124ADAC9FE67B3D0CC80160ECD53FDB59EF4DF5C3F6874FC6F`; F-10/F-11/F-12 and OQ-1 through OQ-9 adjudicated for this fix
- Fix artifacts: `docs/gateflow/a0-production-data-model-schema-spec-s1-fix-1.md` (historical, unchanged) and `docs/gateflow/a0-production-data-model-schema-spec-s1-fix-2.md` (this fix)
- Controller: `docs/gateflow/project-execution-control.md`; accepted-plan checkpoint `4a98fbb`
- Prompt/task provider: `ds` / DeepSeek-V4.1-Flash
- Deliverable class: documentation only. No executable Flyway migration, DDL file, Maven/Spring project, service code, API, UI, cloud resource, production data or credential is produced or claimed.

## Status, freezing and slice discipline

The machine-readable table catalog, baseline matrix, sensitive inventory, foreign-key catalog, candidate-key catalog, function-wave block, the nine exact wave rows and the exact S1/S2/S3 slice membership blocks are copied verbatim from the accepted plan as **FROZEN PLANNED BASELINE**. They preserve global closure and are not evidence that S2 or S3 dictionaries, transactions or tests are complete.

Only the following are authored as completed S1 specification in this document:

- the 13 V001-V003 table dictionaries, including every accepted column, type, nullability, default, key, check, index, foreign key, revision/immutability rule, lifecycle/deletion rule and sensitive representation;
- the S1 enforcement expansion for INV-01 through INV-07, INV-27 through INV-31, INV-33 and the S1 portion of INV-40;
- the four V001/S1 cross-schema function contracts: `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append` and `audit.append_fact`;
- S1 privilege, isolation, conditional-update, state and transaction contracts and their deterministic document checks.

`entitlement_metering.issue_first_trial` remains visibly planned for V006/S2. It is not created, required or tested by S1. S2 and S3 tables, dictionaries, transactions, state machines and tests are intentionally reserved for their later authors and are not replaced by this document.

Baseline counts remain: 44 tables; 40 invariants; 9 migration waves; 3 slices with membership 13/23/8; 28 mutable-head and 16 append-only/frozen tables; 81 FK decisions = 74 ordinary `FK` + 3 late-added `ALTER_FK` + 2 truly `DEFERRABLE_FK` + 2 `NO_FK`; 24 key sets; 5 function-wave rows = 4 V001 functions + 1 planned V006 function.

## 1. Scope, Status and Source Relationship

### 1.1 Purpose

This document is the initial PostgreSQL 16 data-model and Flyway-schema specification for the first production baseline. It defines the S1 foundation so a later implementation work unit can generate V001-V003 migrations and module code without inventing S1 business rules.

S1 covers platform idempotency, transactional Outbox, append-only audit, natural-person customer accounts, login identities, account-level real-name profiles, organizations, organization memberships, operations employee identities, business subjects, business-number bindings, subject claims and claim-risk state.

### 1.2 Source relationship

The binding business and architecture sources are:

- `docs/12-technology-stack-and-system-architecture.md` for the modular-monolith/PostgreSQL boundary, module ownership, one local transaction, identity separation and data lifecycle;
- `docs/11-account-identity-and-workspaces.md` for customer accounts, login identities, workspace membership and the rule that business numbers do not become login identities;
- `docs/09-business-activation-and-lean-admin.md` for business subjects, claim behavior, real-name separation, consent/activation and trial uniqueness;
- `docs/10-cross-channel-rules.md` for independent state dimensions, action-domain idempotency, immutable first results and claim-risk priority;
- `docs/13-ivr-call-task-and-inbound-design.md` for subject/number trust boundaries, canonical caller HMAC use and the later IVR contracts that consume S1.

The accepted plan is the frozen structural handoff. This document does not modify it, the controller, the business sources, historical reviews, source code, prototypes, screenshots or Git metadata.

### 1.3 Exact S1 table membership

| # | Table |
| --- | --- |
| 1 | platform.idempotency_record |
| 2 | platform.outbox_event |
| 3 | audit.audit_fact |
| 4 | customer_iam.customer_account |
| 5 | customer_iam.auth_identity |
| 6 | customer_iam.identity_profile |
| 7 | organization_access.organization |
| 8 | organization_access.organization_membership |
| 9 | operations_iam.employee_account |
| 10 | subject_number.business_subject |
| 11 | subject_number.caller_number_binding |
| 12 | subject_number.subject_claim |
| 13 | subject_number.subject_risk_state |

### 1.4 S1 non-goals

S1 does not create or specify the S2/S3 table dictionaries, IVR task/session/call tables, consent/activation tables, catalog/order/payment tables, entitlement/issuance/reservation/usage tables, evidence tables, sharing tables or `entitlement_metering.issue_first_trial`. It does not create executable SQL, a Maven project, API/UI code, cloud assets, credentials, production data, retention periods or provider-specific policy. It does not change the 44/40/9/3 baseline or authorize a fourth slice.

## 2. Database Conventions

### 2.1 Schemas, roles and ownership

- One PostgreSQL database; one schema per owning module. S1 schemas are `platform`, `audit`, `customer_iam`, `organization_access`, `operations_iam` and `subject_number`.
- Each schema and its tables are owned by a NOLOGIN, non-superuser owner role. Runtime application roles are separate NOLOGIN roles with least privilege.
- A module runtime role receives `USAGE` on its own schema and only the table privileges explicitly listed in this document. It never receives `INSERT`, `UPDATE` or `DELETE` on another module's tables.
- Cross-module reads use `SELECT` on the exact columns listed in section 10.8 and, where a foreign key requires it, `REFERENCES`. Cross-module writes occur only through the four named functions in section 10.8 or a later accepted published service.
- A cross-schema function caller needs both `USAGE` on the containing schema (without `CREATE`) and `EXECUTE` on the function. Schema `USAGE` alone grants no table privilege and no write path. `operations_service_role` and `subject_identity_verification_role` receive only the schema `USAGE`, table `SELECT`/narrow column privileges and function `EXECUTE` listed in section 10.8.
- Cross-module lifecycle and access serialization uses the owner lifecycle advisory-lock namespaces in section 9.0 plus a `SELECT` of the declared columns. The membership namespace is the stable logical pair `organization_access.membership:<organization_id>:<account_id>`, never a membership row ID. The owning module takes the same advisory lock and then its own row lock. A cross-module role never takes a `FOR UPDATE`/`FOR SHARE` row lock on another module's table and therefore never needs an `UPDATE` grant on that table. An unlocked read-only workspace projection is not authorization serialization and is rechecked per protected request (section 9.4).
- Immutable identity, membership, number-binding and subject-owner relation columns are protected by narrow column-level `UPDATE` grants and local `BEFORE UPDATE` guard triggers defined in section 10.8.1. These trigger functions are local structural controls; they are not entries in the function-wave registry and are not published cross-schema functions.
- `PUBLIC` receives no schema `CREATE` or `USAGE`, no table privilege and no function `EXECUTE`. The `public` schema is not used for business objects. `pg_temp` is placed last in every function `search_path`.
- Migration DDL is executed by a dedicated migration identity that is not used by runtime services. The migration identity creates schemas, owner roles, tables, constraints, indexes, triggers and functions in the frozen wave order.

### 2.2 Identifiers and protocol identities

- Every table primary key is a `uuid` named `id` with no database default. Caller-owned rows are application-generated. The three function-inserted row types — `platform.idempotency_record` by `platform.idempotency_begin`, `platform.outbox_event` by `platform.outbox_append`, and `audit.audit_fact` by `audit.append_fact` — generate `id` inside the function body with the PostgreSQL 16 built-in `pg_catalog.gen_random_uuid()`. No function gains a `p_id` argument and no table gains an `id` default. No database sequence is a business identity.
- Protocol identifiers such as `call_id`, `task_id`, `recording_id`, `order_no` and `share_id` are separate opaque values created by their owning module. S1 has no external protocol identifier except the opaque `request_id` on `subject_claim`.
- No phone number, WeChat subject, identity number, organization name, employee login name, HMAC or ciphertext is a primary key, unique public identifier or response key.
- `request_id` is an internal idempotency reference with a unique constraint; it is not returned as a customer-facing number.

### 2.3 Types, time and money

- Text state values are `text` with named `CHECK` constraints. PostgreSQL enum types are not used, so state changes are expand/contract-friendly.
- Times are `timestamptz` in UTC. Defaults use `now()` for ordinary row creation. Security and TTL predicates that require the current wall clock use `clock_timestamp()` after lock acquisition, never transaction-start `now()` or a client timestamp.
- Counts are `integer` or `bigint` as stated per column; durations and money are integer values. No floating-point money, duration or units exist.
- Boolean columns are used only where a two-valued fact is genuinely independent; state is otherwise a named code.
- `jsonb` is used only for canonical snapshots and redacted payloads, not as a substitute for relational state, keys or ownership.

### 2.4 Revisions and conditional updates

- Every mutable-head table declares `revision integer NOT NULL DEFAULT 1 CHECK (revision >= 1)`.
- A conditional update is `UPDATE ... SET ..., revision = revision + 1 WHERE id = :id AND revision = :expected_revision` and must affect exactly one row. A zero-row update triggers one bounded reread and a stable conflict or current-state response; a raw unique or revision error is never exposed as a protocol success.
- Revisions are monotonic integers. Timestamps are never used as optimistic concurrency tokens.
- Append-only and frozen tables have no revision column unless the accepted catalog explicitly includes one; their identity and snapshot columns are never updated.

### 2.5 Canonical snapshots and fingerprints

- Canonical JSON is an RFC 8785-compatible serialization: object keys sorted by UTF-16 code units, no insignificant whitespace, UTF-8 encoding, and ECMAScript-compatible shortest round-trip number formatting. Inputs must be finite JSON values; non-JSON database values are converted by the owning application service before hashing.
- Every `request_fingerprint` is the lowercase hexadecimal SHA-256 digest of the canonical JSON of its documented semantic fields. It excludes transport event IDs where the documented fingerprint excludes them, timestamps, database-generated IDs and mutable balance/status values.
- Every persisted `result_sha256`, `snapshot_sha256`, `payload_sha256` and `content_sha256` is a lowercase hexadecimal SHA-256 digest of the exact canonical redacted snapshot or payload described for that column.
- Hash columns use `text` with `CHECK (value ~ '^[0-9a-f]{64}$')`; they are never public identifiers.

### 2.6 Sensitive-value representation

- Ciphertext columns store a versioned envelope, never plaintext: `v1.<key_ref>.<base64url(nonce || authenticated_ciphertext_and_tag)>`. `key_ref` is an opaque KMS key identifier/version. The AEAD algorithm and KMS product are security-design inputs (Q2); the envelope shape is the schema contract.
- `customer_iam.auth_identity` stores an explicit `key_version` integer in addition to the envelope for operational rotation.
- Lookup columns use HMAC-SHA-256 with a separate HMAC key, 64 lowercase hex characters. HMAC values are used for equality, uniqueness and indexed lookup only; they are never returned as the underlying number or subject.
- Masked columns store a display-only value such as `138****6208`. Masking is performed server-side and is not reversible.
- Redacted JSON must not contain a raw full number, provider subject, identity number, legal name, callee, share code, OTP, object key, provider secret or permanent media URL. The four S1 functions reject the documented protected-key denylist in section 10.7.
- No OTP, share code, provider secret, access token or permanent object URL is stored in S1 tables.

### 2.7 Foreign keys and deletion

- Every foreign key is `ON UPDATE RESTRICT ON DELETE RESTRICT`; no `ON DELETE CASCADE` and no cross-module delete grant exists.
- A cross-wave FK column may be created before its parent table, but the FK is added only in the wave that creates the parent. Same-wave circular FKs are added after both tables exist.
- S1 FK rows are ordinary immediate `FK` constraints. S1 has no `ALTER_FK`, `DEFERRABLE_FK` or `NO_FK` decision.

### 2.8 Function conventions

- A cross-schema function is `SECURITY DEFINER`, owned by the owning NOLOGIN module owner, and declares `SET search_path = <owning schema>, pg_temp` at definition time. Function bodies schema-qualify every object.
- `PUBLIC` has no `EXECUTE`. Each function is granted only to the runtime roles named in section 10.8, and each of those roles receives `USAGE` on the function's schema without `CREATE`.
- A function contains no `COMMIT`, `ROLLBACK`, autonomous transaction or transaction control. It executes on the caller's connection and in the caller's transaction. Any raised exception aborts the caller's transaction, so the domain write, function write and completion roll back together.
- The function owner is not a superuser and cannot log in. `SECURITY DEFINER` never inherits a caller-controlled `search_path`.

## 3. Module and Schema Ownership Matrix

### 3.1 S1 modules and roles

| Module | Schema | Owning role | S1 runtime/service roles | S1 tables |
| --- | --- | --- | --- | --- |
| platform-reliability | platform | platform_reliability_owner | platform_reliability_role, platform_dispatcher_role | platform.idempotency_record, platform.outbox_event |
| operations-audit | audit | audit_owner | audit_reader_role | audit.audit_fact |
| customer-iam | customer_iam | customer_iam_owner | customer_iam_role | customer_iam.customer_account, customer_iam.auth_identity, customer_iam.identity_profile |
| organization-access | organization_access | organization_access_owner | organization_access_role | organization_access.organization, organization_access.organization_membership |
| operations-iam | operations_iam | operations_iam_owner | operations_iam_role | operations_iam.employee_account |
| subject-number | subject_number | subject_number_owner | subject_number_role, subject_identity_verification_role | subject_number.business_subject, subject_number.caller_number_binding, subject_number.subject_claim, subject_number.subject_risk_state |

All owner and runtime roles are NOLOGIN, NOSUPERUSER and NOCREATEDB. The migration identity is the only role that creates S1 objects. `subject_identity_verification_role` is a narrow service role that may `SELECT` the subject `id` plus the real-name/status/revision/update-time columns and may update only the subject-level real-name columns, revision and update time; it cannot alter owners, source, bindings or claims. It receives `USAGE` on `subject_number` and `audit` and `EXECUTE` on `audit.append_fact` so its same-transaction verification fact can be audited. `operations_service_role` is a NOLOGIN S1 service role used only to execute `audit.append_fact`; it receives `USAGE` on `audit` and no table write privilege.

### 3.2 Table ownership and write paths

| Table | Owner module | Wave | Slice | Read-only collaborators | Permitted write path |
| --- | --- | --- | --- | --- | --- |
| platform.idempotency_record | platform-reliability | V001 | S1 | all domain owners | platform.idempotency_begin/complete only |
| platform.outbox_event | platform-reliability | V001 | S1 | all domain owners | platform.outbox_append for append; platform_dispatcher_role for dispatcher columns |
| audit.audit_fact | operations-audit | V001 | S1 | all domain owners | audit.append_fact only |
| customer_iam.customer_account | customer-iam | V002 | S1 | organization-access, subject-number | customer_iam_role |
| customer_iam.auth_identity | customer-iam | V002 | S1 | none | customer_iam_role |
| customer_iam.identity_profile | customer-iam | V002 | S1 | none | customer_iam_role |
| organization_access.organization | organization-access | V002 | S1 | customer-iam, subject-number, operations-iam | organization_access_role |
| organization_access.organization_membership | organization-access | V002 | S1 | customer-iam, subject-number | organization_access_role |
| operations_iam.employee_account | operations-iam | V002 | S1 | operations-audit | operations_iam_role |
| subject_number.business_subject | subject-number | V003 | S1 | customer-iam, organization-access | subject_number_role for subject creation (both owners null) and verified-personal owner set; subject_identity_verification_role for real-name columns |
| subject_number.caller_number_binding | subject-number | V003 | S1 | customer-iam, organization-access | subject_number_role |
| subject_number.subject_claim | subject-number | V003 | S1 | customer-iam, organization-access | subject_number_role insert only; no update/delete |
| subject_number.subject_risk_state | subject-number | V003 | S1 | customer-iam, organization-access | subject_number_role |

Collaborators are not co-owners. They may read stable columns and call a published service; they receive no write privilege. No module writes another module's table directly.

### 3.3 Identity and workspace answers

**Which identity can log in?** Only an active `customer_iam.auth_identity` row whose `provider` is `WECHAT` or `PHONE`, whose `verified_at` is set, and whose parent `customer_iam.customer_account` is `ACTIVE`. The identity is resolved by `(provider, provider_scope, provider_subject_hmac)` and maps to exactly one account. A business-number binding, subject claim, organization membership, organization, business subject or employee account is never a login identity. Operations employees authenticate only with `operations_iam.employee_account` in their separate issuer domain; they do not use customer identities.

**Uniqueness, unbind and replace races.** The unique provider/subject HMAC key serializes identity creation. A concurrent insert loser reads the winner: the same account is an idempotent success, a different account is a stable `IDENTITY_CONFLICT`; no account merge or identity migration is performed. Disabling an identity is a conditional revision update; an identity cannot be rebound to another account. A phone number can be both a login identity and a business number only when separate `auth_identity` and `caller_number_binding` rows are each created through their own flow; neither row infers the other.

**Workspace selection and access.** The personal workspace is derived from an active customer account; it has no separate table and always exists for an active account. Organization workspaces are derived from `organization_access.organization_membership` rows with `status = 'ACTIVE'` whose organization is `ACTIVE`. The server supplies the workspace list and role; the client cannot submit a role, account, organization or workspace claim. A disabled account cannot log in; a disabled or suspended organization and a revoked or suspended membership do not appear as usable workspaces. Revocation removes only that organization's access and never deletes the natural-person account or personal space. Role changes are server-side conditional updates with revision and audit; users cannot self-elevate.

**Business numbers, claims and login.** Binding or claiming a business number never creates an `auth_identity`, never changes a customer account's login identities, never changes subject real-name status, and never rewrites evidence, trial or usage facts. An IVR-created subject may be unowned and unverified: both `business_subject` owner columns may be null, `real_name_status` may remain `UNVERIFIED`, and a binding may exist before any account or subject claim. Such a subject can later be claimed by a verified personal account; a personal claim sets the owner only when null and conflicts when a different owner exists. An organization-scoped claim is fail-closed in S1 until the deferred enterprise ownership policy is approved, and it never chooses personal versus organization ownership from an optional workspace ID.

## 4. Complete Table Dictionary

### 4.1 Frozen planned table catalog (all 44 tables)

The following catalog is the accepted 44-table planned baseline. It is verbatim from the accepted plan. S1 status is recorded outside the block: rows 1-13 are expanded in this document; the remaining 31 rows are planned for S2/S3 and are not S1 implementation.

<!-- catalog:start -->
| Table | Owning module | Wave | Slice | Minimum columns and keys | Critical constraints and lifecycle |
| --- | --- | --- | --- | --- | --- |
| `platform.idempotency_record` | `platform-reliability` | V001 | S1 | `id`, `scope`, `idempotency_key`, `request_fingerprint`, `status`, `result_type`, `result_id`, `result_snapshot`, `result_sha256`, `created_at`, `completed_at`, `revision` | Unique `(scope, idempotency_key)`; status `NEW/COMPLETED`; `NEW` requires all result columns and `completed_at` to be NULL, `COMPLETED` requires all result columns and `completed_at` to be non-null; `platform.idempotency_complete` conditionally fills the result exactly once and sets `COMPLETED`, `completed_at`, `revision + 1`; a completed result is never overwritten; the stored fingerprint is the canonical semantic fingerprint for its scope, not a transport timestamp; same key/different fingerprint is a stable conflict; for `IVR_CONFIRM` the key is `eventId` and the fingerprint includes `callId`, so the same event reused on another call conflicts while a regenerated event ID with identical semantic confirmation matches and replays the first result; `NEW` is the only outcome that may execute side effects and a semantic conflict rolls the `NEW` row back; `revision` starts at 1; snapshot is redacted or encrypted; mutable head with a finalize-once result |
| `platform.outbox_event` | `platform-reliability` | V001 | S1 | `id`, `event_domain`, `dedupe_key`, `aggregate_type`, `aggregate_id`, `event_type`, `payload_redacted`, `payload_ciphertext`, `payload_sha256`, `status`, `attempt_count`, `available_at`, `published_at`, `revision` | Unique `(event_domain, dedupe_key)`; inserted in the domain transaction through `platform.outbox_append`; payload is redacted or encrypted and immutable; dispatcher state transitions are conditional and bounded by `revision`; mutable head |
| `audit.audit_fact` | `operations-audit` | V001 | S1 | `id`, `occurred_at`, `actor_type`, `actor_id`, `action_code`, `object_type`, `object_id`, `reason_code`, `before_snapshot`, `after_snapshot`, `request_id`, `trace_id` | Append-only; no update/delete privilege; inserted only through `audit.append_fact` in schema `audit`; actor and object identifiers remain internal; snapshots are redacted or encrypted |
| `customer_iam.customer_account` | `customer-iam` | V002 | S1 | `id`, `display_name`, `status`, `revision`, `created_at`, `updated_at` | Status `ACTIVE/DISABLED`; revision conditional updates; no public natural key; mutable head |
| `customer_iam.auth_identity` | `customer-iam` | V002 | S1 | `id`, `account_id`, `provider`, `provider_scope`, `provider_subject_ciphertext`, `provider_subject_hmac`, `status`, `verified_at`, `key_version`, `revision` | Unique `(provider, provider_scope, provider_subject_hmac)`; no nickname as subject; one identity maps to one account; status changes use conditional revision; mutable head |
| `customer_iam.identity_profile` | `customer-iam` | V002 | S1 | `id`, `account_id`, `real_name_status`, `legal_name_ciphertext`, `id_number_ciphertext`, `id_number_hmac`, `verification_ref_ciphertext`, `verified_at`, `revision` | Unique `account_id`; account-level real-name state is independent of login success and business-number possession; it is not the subject-level real-name fact; mutable head |
| `organization_access.organization` | `organization-access` | V002 | S1 | `id`, `display_name`, `status`, `revision`, `created_at`, `updated_at` | Status `ACTIVE/SUSPENDED/DISABLED`; no login identity; mutable head |
| `organization_access.organization_membership` | `organization-access` | V002 | S1 | `id`, `organization_id`, `account_id`, `role`, `department_label`, `status`, `revision` | Partial unique `(organization_id, account_id)` for non-revoked membership; roles fixed to administrator/member/evidence manager/auditor; mutable head |
| `operations_iam.employee_account` | `operations-iam` | V002 | S1 | `id`, `login_name_hmac`, `login_name_masked`, `auth_issuer`, `auth_subject_hmac`, `display_name`, `role`, `status`, `revision` | Unique `login_name_hmac` and `(auth_issuer, auth_subject_hmac)`; role `ADMIN/OPS`; credentials remain outside business tables; last enabled admin is transaction-protected; mutable head |
| `subject_number.business_subject` | `subject-number` | V003 | S1 | `id`, `owner_account_id`, `owner_organization_id`, `source`, `real_name_status`, `real_name_verified_at`, `revision`, `created_at`, `updated_at` | At most one owner of type personal account or organization (`num_nonnulls(...) <= 1`); an IVR-created subject may be unowned until a verified claim; claim sets the owner only when null and conflicts if a different owner exists; subject real-name is separate from activation and login; claim/IVR never writes it; mutable head |
| `subject_number.caller_number_binding` | `subject-number` | V003 | S1 | `id`, `subject_id`, `number_type`, `number_ciphertext`, `number_hmac`, `number_masked`, `status`, `verified_at`, `revision` | Partial unique `number_hmac` for `ACTIVE`; unique `(subject_id, number_hmac)`; historical retired bindings remain; mutable head |
| `subject_number.subject_claim` | `subject-number` | V003 | S1 | `id`, `subject_id`, `account_id`, `organization_id`, `verification_method`, `verification_ref_ciphertext`, `request_id`, `status`, `risk_checked_at`, `verified_at` | Unique `request_id`; partial unique `(subject_id, account_id)` where `status = ACCEPTED`; status is set at insert and never updated; rejected or conflict attempts remain append-only; append-only |
| `subject_number.subject_risk_state` | `subject-number` | V003 | S1 | `id`, `caller_number_binding_id`, `risk_type`, `state`, `source`, `observed_at`, `cleared_at`, `revision` | Unique `(caller_number_binding_id, risk_type)`; `OPEN` risk blocks number claims and historical-evidence access regardless of replay; it is not by itself a ban on new outbound calling, and any future call-blocking risk predicate requires a separately approved policy; mutable head |
| `catalog_order.catalog_product` | `catalog-order` | V004 | S2 | `id`, `product_code`, `product_family`, `created_at` | Unique `product_code`; immutable stable product identity; no draft status and no product draft-delete exception; append-only |
| `catalog_order.catalog_version` | `catalog-order` | V004 | S2 | `id`, `product_id`, `version_no`, `sale_status`, `offer_type`, `audience`, `price_cents`, `currency`, `billing_cycle`, `installments`, `grant_interval_months`, `snapshot`, `snapshot_sha256`, `created_by_employee_id`, `created_at`, `published_at`, `revision` | Partial unique active version per product; `DRAFT/ACTIVE/OFFSALE`; commercial content immutable after publication; draft content and sale status use conditional revision; type/billing/grant matrix checks; enterprise quote cannot use personal values; only never-published and unreferenced drafts are a future deletion candidate, not implemented here; mutable head |
| `catalog_order.catalog_offer` | `catalog-order` | V004 | S2 | `id`, `catalog_version_id`, `channel`, `offer_status`, `version_token`, `created_at`, `revision` | Unique `(catalog_version_id, channel)`; `version_token` is content/channel-specific; offer status uses conditional revision; withdrawn offers stop new orders only; mutable head |
| `catalog_order.catalog_benefit_line` | `catalog-order` | V004 | S2 | `id`, `catalog_version_id`, `entitlement_type`, `quantity`, `validity_days`, `grant_interval_months` | Unique `(catalog_version_id, entitlement_type)`; positive values only; immutable with the version; append-only |
| `catalog_order.customer_order` | `catalog-order` | V004 | S2 | `id`, `order_no`, `account_id`, `owner_organization_id`, `subject_id`, `channel`, `catalog_version_id`, `catalog_offer_id`, `purchase_consent_id`, `amount_cents`, `currency`, `payment_status`, `snapshot`, `snapshot_sha256`, `version_token`, `idempotency_key`, `request_fingerprint`, `revision`, `created_at`, `paid_at`, `closed_at` | Unique `order_no` and `(account_id, idempotency_key)`; order creation uses platform scope `PURCHASE` with key `account_id || ':' || idempotency_key`, begin/fingerprint/first-order-result completion in the same transaction; identical retry returns the first order and changed input conflicts; channel `MINIAPP/WEB`; amount, snapshot, fingerprint and consent reference immutable; payment status only; entitlement status is not stored and is derived from `entitlement_issuance`; `purchase_consent_id` ordinary FK is added in V007; mutable head |
| `catalog_order.payment_order` | `catalog-order` | V004 | S2 | `id`, `order_id`, `provider`, `merchant_order_no`, `provider_transaction_id`, `status`, `amount_cents`, `currency`, `created_at`, `updated_at`, `revision` | Unique `order_id`, `merchant_order_no` and non-null provider transaction; status changes only from verified provider evidence; mutable head |
| `catalog_order.payment_event` | `catalog-order` | V004 | S2 | `id`, `payment_order_id`, `provider`, `provider_event_id`, `event_type`, `signature_verified`, `payload_ciphertext`, `payload_sha256`, `transaction_id`, `amount_cents`, `occurred_at`, `received_at` | Unique `(provider, provider_event_id)`; append-only; multiple verified events may advance one order; unverified events never advance payment; append-only |
| `ivr.outbound_call_task` | `ivr` | V005 | S2 | `id`, `task_id`, `caller_binding_id`, `subject_id`, `owner_account_id`, `owner_organization_id`, `created_by_account_id`, `caller_number_hmac`, `callee_ciphertext`, `callee_masked`, `business_type`, `status`, `reason_code`, `ttl_seconds`, `expires_at`, `idempotency_key`, `request_fingerprint`, `claimed_call_id`, `claimed_at`, `terminal_at`, `created_at`, `updated_at`, `revision` | Unique `task_id`; unique `(created_by_account_id, idempotency_key)`; same key/input returns the existing task without TTL refresh; same key/different fingerprint is a conflict; an identical caller/callee/business/workspace `PENDING` duplicate may return the existing task without TTL refresh; the default `ttl_seconds` is 30 at creation, server-configurable, frozen into `expires_at`, and never refreshed by a duplicate; exactly one of `owner_account_id`/`owner_organization_id`; partial unique `caller_number_hmac` for `PENDING/CLAIMED`; unique claimed call; a task is claimable only by the shared IVR action service when it is `PENDING`, bound to the current trusted caller/subject/binding and owner workspace, its creator still has current task access, its revision matches, `expires_at > clock_timestamp()` is re-evaluated in the conditional claim update, and a reservation commits in the same transaction; for an organization task, `organization.status = 'ACTIVE'`, the creator account is `ACTIVE` and the creator membership is `ACTIVE` with the current task-access role and resource access; for a personal-owner task, the applicable owner and creator account rows are `ACTIVE`; those lifecycle rows are locked in the shared order before the claim; `claimed_call_id` FK is added in V005 after `call_record`; mutable head |
| `ivr.inbound_session` | `ivr` | V005 | S2 | `id`, `call_id`, `subject_id`, `trusted_caller`, `caller_ciphertext`, `caller_number_hmac`, `caller_masked`, `access_number_masked`, `node_id`, `protocol_version`, `prompt_version`, `status`, `confirmed_subject_id`, `guidance_status`, `created_at`, `confirmed_at`, `revision` | Unique `call_id`; `call_id` is the root IVR protocol identity with no parent FK; created in resolve/direct authorization; a fresh `POST_ACTIVATION` confirmation is allowed only when the original `(call_id, INBOUND_RESOLVE)` decision is `GUIDE_ACTIVATION`; `CONFIRMED` requires trusted caller, key `1`, current consent version, same confirmed subject, a non-suspended subject re-read under the subject lifecycle lock and exactly one `ACTIVATION_NEXT_ACTION` decision committed with it; a `SUSPENDED` subject cannot reach `CONFIRMED`; a confirmed session is never moved to `REJECTED` by replay; mutable head |
| `ivr.call_record` | `ivr` | V005 | S2 | `id`, `call_id`, `session_id`, `task_id`, `subject_id`, `caller_ciphertext`, `caller_number_hmac`, `caller_masked`, `callee_ciphertext`, `callee_masked`, `access_number_masked`, `status`, `node_id`, `protocol_version`, `started_at`, `bridged_at`, `recording_started_at`, `recording_ended_at`, `ended_at`, `authoritative_seconds`, `end_reason`, `revision` | Unique `call_id` and `session_id`; unique non-null `task_id`; partial unique active `caller_number_hmac` for `ACTIVE/UNKNOWN`; canonical call-scoped FK parent; a live call is created or bound as `ACTIVE` before a non-terminal decision/reservation, while a call pre-empted by another active call is created terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS` so its `REJECT_CALL_IN_PROGRESS` decision still has a parent; the other-active-call guard excludes the current `callId`; only a never-started/rejectable current `ACTIVE` row may be finalized `REJECTED`, while a fresh attempt on a current `UNKNOWN` call with inconsistent line state returns a stable reconciliation/integrity refusal and never finalizes `UNKNOWN` to `REJECTED` or releases it; a terminal reject touches only the current call's own row and never an unrelated competing call; the partial unique index is a backstop; party fields are ciphertext/masked and immutable; mutable head |
| `ivr.call_leg` | `ivr` | V005 | S2 | `id`, `call_id`, `leg_id`, `leg_type`, `direction`, `state`, `started_at`, `answered_at`, `ended_at`, `end_reason`, `revision` | Unique `(call_id, leg_id)`; FK to `call_record.call_id`; caller and callee legs belong to one root call; state changes use conditional revision; mutable head |
| `ivr.call_event` | `ivr` | V005 | S2 | `id`, `call_id`, `event_domain`, `event_id`, `sequence_no`, `event_type`, `occurred_at`, `received_at`, `payload_ciphertext`, `payload_redacted`, `payload_sha256`, `processing_status`, `reason_code`, `revision` | `event_domain` check `IVR_EVENT`; unique `(event_domain, event_id)` so an event ID cannot repeat on another call; partial unique `(call_id, sequence_no)` when supplied; FK to `call_record.call_id`; processing status uses conditional revision; mutable head |
| `ivr.ivr_decision` | `ivr` | V005 | S2 | `id`, `session_id`, `call_id`, `purpose`, `source_event_domain`, `source_event_id`, `request_fingerprint`, `action`, `reason_code`, `response_snapshot`, `response_payload_ciphertext`, `response_sha256`, `key_version`, `reservation_id`, `decided_at` | Unique `(call_id, purpose)`; purposes are `INBOUND_RESOLVE`, `DIRECT_AUTHORIZATION` and `ACTIVATION_NEXT_ACTION`, so the post-activation decision never overwrites the original frozen `GUIDE_ACTIVATION` resolve decision; partial unique `(call_id, source_event_domain, source_event_id)` when supplied; same call/purpose with a different canonical fingerprint is a conflict, while the same fingerprint returns the existing decision and frozen result before any side effect; `action` is one of `GUIDE_ACTIVATION`, `DIAL_TASK_CALLEE`, `ENTER_CALLEE`, `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED` and `REJECT_CALL_IN_PROGRESS` and is NOT NULL for `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION`; `DIRECT_AUTHORIZATION` is a purpose, not an eighth action, so a successful direct authorization has `action` NULL with a non-null `reservation_id`, while a direct refusal uses one of the permitted non-dial actions (`GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS`) with a null `reservation_id` and never `DIAL_TASK_CALLEE` or `ENTER_CALLEE`; a named CHECK enforces this purpose/action/nullability shape; `reason_code` uses the stable registry in section 11; `response_snapshot` is the typed redacted action snapshot and `response_payload_ciphertext` is the encrypted internal payload (full task callee only for `DIAL_TASK_CALLEE` under `task.calleeNumber`, and the full confirmed callee only for successful `DIRECT_AUTHORIZATION` under `authorization.calleeNumber`); `ACTIVATION_NEXT_ACTION` and `INBOUND_RESOLVE` are written only through the IVR-owned `ivr.select_call_action` service, with `ivr.record_activation_next_action` as a thin entry wrapper; FK to `call_record.call_id` and `inbound_session`; `reservation_id` FK is added in V006; frozen append-only decision |
| `entitlement_metering.trial_policy_version` | `entitlement-metering` | V006 | S2 | `id`, `campaign_key`, `version_no`, `granted_seconds`, `valid_days`, `enabled`, `snapshot`, `snapshot_sha256`, `published_by_employee_id`, `published_at`, `revision` | `campaign_key` check `FIRST-TRIAL`; unique `(campaign_key, version_no)`; partial unique enabled current campaign; content snapshot is immutable after publication while lifecycle flags use conditional revision; mutable head |
| `entitlement_metering.entitlement_grant` | `entitlement-metering` | V006 | S2 | `id`, `subject_id`, `source_type`, `source_ref_id`, `entitlement_type`, `granted_units`, `valid_from`, `expires_at`, `order_id`, `installment_no`, `snapshot`, `snapshot_sha256`, `granted_at` | Source exclusivity checks; paid partial unique `(order_id, installment_no, entitlement_type)`; trial source uniqueness delegates to `trial_grant`; immutable grant |
| `entitlement_metering.trial_grant` | `entitlement-metering` | V006 | S2 | `id`, `subject_id`, `campaign_key`, `trial_policy_version_id`, `entitlement_grant_id`, `source_action_domain`, `source_action_event_id`, `request_fingerprint`, `granted_at`, `expires_at` | `campaign_key` check `FIRST-TRIAL`; unique `subject_id` permanently; unique `entitlement_grant_id`; unique `(source_action_domain, source_action_event_id)`; source identity and fingerprint are non-null and immutable; same action identity plus same fingerprint replays the original ids, same action identity plus changed fingerprint conflicts, and another action for an already-granted subject returns `ALREADY_GRANTED` original ids without a second grant or balance; grant facts immutable; append-only |
| `entitlement_metering.entitlement_balance` | `entitlement-metering` | V006 | S2 | `id`, `grant_id`, `granted_units`, `reserved_units`, `consumed_units`, `status`, `revision`, `updated_at` | Unique `grant_id`; non-negative units; `reserved_units + consumed_units <= granted_units`; updated only by reservation/settlement transaction; mutable head |
| `entitlement_metering.entitlement_issuance` | `entitlement-metering` | V006 | S2 | `id`, `order_id`, `installment_no`, `entitlement_type`, `status`, `attempt_count`, `revision`, `last_error_code`, `grant_id`, `source_event_domain`, `source_event_id`, `first_attempt_at`, `issued_at`, `updated_at` | Unique `(order_id, installment_no, entitlement_type)`; unique non-null `grant_id`; status `PENDING/ISSUED/FAILED`; `FAILED` requires `last_error_code`; `ISSUED` requires `grant_id` and `issued_at`; `installment_no >= 1` and validated against the frozen order snapshot; mutable head |
| `entitlement_metering.usage_reservation` | `entitlement-metering` | V006 | S2 | `id`, `call_id`, `balance_id`, `subject_id`, `authorized_units`, `status`, `authorization_expires_at`, `settled_units`, `reconciliation_due_at`, `reconciliation_attempt_count`, `last_reconciled_at`, `authority_source`, `last_error_code`, `created_at`, `terminal_at`, `revision` | Unique `call_id`; FK to `call_record.call_id`; status `HELD/RECONCILING/SETTLED/RELEASED`; one terminal transition guarded by `revision`; settlement at most once; `authorized_units` are frozen recording seconds in the baseline (1 unit = 1 second), so `authorizedRecordingSeconds` is an explicit identity conversion of the committed value; `authorization_expires_at` is the last instant to start the B-leg, not the end of a running call, and is never renewed by replay; the reservation commits in the same transaction as the task claim and frozen decision, so no claimed task can commit without it; actual use cannot exceed authorized units plus approved tolerance; unknown use keeps `RECONCILING` and is never released; mutable head |
| `entitlement_metering.usage_fact` | `entitlement-metering` | V006 | S2 | `id`, `reservation_id`, `balance_id`, `call_id`, `usage_type`, `units`, `source_event_domain`, `source_event_id`, `occurred_at`, `created_at`, `snapshot`, `snapshot_sha256` | Unique `(reservation_id, usage_type)` and unique `(source_event_domain, source_event_id)`; FKs to reservation, balance and call record; append-only; positive units only; append-only |
| `activation_consent.consent_record` | `activation-consent` | V007 | S2 | `id`, `subject_id`, `purpose`, `channel`, `event_domain`, `policy_code`, `policy_version`, `prompt_version`, `action`, `source_type`, `source_ref`, `ivr_session_id`, `account_id`, `employee_id`, `snapshot`, `snapshot_sha256`, `event_id`, `request_fingerprint`, `accepted_at` | Unique `(event_domain, event_id)`; explicit accept/decline only; snapshot is redacted or encrypted and immutable; `request_fingerprint` is required for replay comparison; no inferred consent; append-only |
| `activation_consent.service_activation` | `activation-consent` | V007 | S2 | `id`, `subject_id`, `status`, `revision`, `created_at`, `updated_at` | Unique `subject_id`; status `INACTIVE/ACTIVE/SUSPENDED`; current revision must match the transition ledger; confirmation and operations suspend/resume read the status under the subject lifecycle lock and the activation row lock; a `SUSPENDED` row cannot be activated by IVR confirmation and only an `OPERATIONS` transition may append `SUSPENDED → ACTIVE`; resume never calls the first-trial service; mutable head |
| `activation_consent.service_activation_transition` | `activation-consent` | V007 | S2 | `id`, `activation_id`, `subject_id`, `from_status`, `to_status`, `from_revision`, `to_revision`, `source_channel`, `source_action_domain`, `source_action_event_id`, `status_event_domain`, `status_event_id`, `request_fingerprint`, `actor_account_id`, `actor_employee_id`, `reason_code`, `consent_record_id`, `occurred_at` | Unique `(activation_id, to_revision)`, unique `(source_action_domain, source_action_event_id)` and unique `(status_event_domain, status_event_id)`; `to_revision = from_revision + 1`; `source_action_event_id` and `request_fingerprint` are NOT NULL for every domain, including `OPERATIONS`; action domains are `ACTIVATION`, `IVR_CONFIRM` or `OPERATIONS`; `status_event_domain = ACTIVATION_STATUS`; append-only |
| `evidence.media_object` | `evidence` | V008 | S3 | `id`, `storage_provider`, `bucket`, `object_key`, `object_version_id`, `sha256`, `size_bytes`, `mime_type`, `crypto_key_ref`, `worm_status`, `created_at` | Unique `(bucket, object_key, object_version_id)`; immutable identity/version/hash; no object bytes, bucket/key public output or permanent URL; append-only |
| `evidence.recording` | `evidence` | V008 | S3 | `id`, `call_id`, `recording_id`, `media_object_id`, `declared_format`, `declared_size`, `declared_duration`, `declared_sha256`, `validated_duration`, `validated_sha256`, `status`, `upload_expires_at`, `created_at`, `validated_at`, `revision` | Unique `recording_id`; FK to `call_record.call_id` and `media_object`; one recording identity per call/recording pair; status changes use conditional revision; server-computed hash and duration win; mismatch quarantines; mutable head |
| `evidence.evidence_record` | `evidence` | V008 | S3 | `id`, `subject_id`, `source_type`, `source_ref_id`, `source_event_domain`, `source_event_id`, `call_id`, `session_id`, `reservation_id`, `identity_at_capture`, `consent_version`, `status`, `revision`, `snapshot`, `snapshot_sha256`, `created_at`, `solidified_at` | `source_event_domain` check `EVIDENCE`; unique `(source_event_domain, source_event_id)`; composite FK `(source_event_domain, source_event_id)` → `evidence_event(event_domain, event_id)` is `DEFERRABLE INITIALLY DEFERRED`; FKs to subject, call record, inbound session and reservation; exactly one `EVIDENCE_CREATED` event must match at commit through the deferred constraint trigger; captured subject/session/identity/consent are immutable; mutable head |
| `evidence.evidence_artifact` | `evidence` | V008 | S3 | `id`, `evidence_id`, `media_object_id`, `artifact_role`, `created_at` | Unique `(evidence_id, artifact_role, media_object_id)`; immutable; original and derived roles remain distinct; append-only |
| `evidence.evidence_event` | `evidence` | V008 | S3 | `id`, `evidence_id`, `event_domain`, `event_id`, `event_type`, `occurred_at`, `payload_redacted`, `payload_sha256` | `event_domain` check `EVIDENCE`; unique `(event_domain, event_id)` as the composite FK target; `evidence_id` FK to `evidence_record.id` is `DEFERRABLE INITIALLY DEFERRED` (the opposite side of the creation cycle); partial unique `(evidence_id)` where `event_type = EVIDENCE_CREATED`; exactly one creation event owns each evidence record; payload is redacted metadata, not media bytes; append-only |
| `sharing.evidence_share_grant` | `sharing` | V009 | S3 | `id`, `share_id`, `capability_token_hash`, `evidence_id`, `created_by_account_id`, `owner_organization_id`, `verification_mode`, `code_salt`, `code_hash`, `code_generation`, `valid_from`, `expires_at`, `max_admissions`, `admissions_used`, `download_allowed`, `content_snapshot`, `content_sha256`, `status`, `revocation_generation`, `revision` | Unique `share_id` and capability-token hash; `admissions_used <= max_admissions` when a limit is set; content scope immutable; access checks require current status/generation; no plaintext code or permanent URL; mutable head |
| `sharing.share_admission` | `sharing` | V009 | S3 | `id`, `share_grant_id`, `admission_key`, `admission_no`, `verification_mode`, `verification_result`, `admitted_at` | Unique `(share_grant_id, admission_key)` and `(share_grant_id, admission_no)`; successful admission increments count once under row lock; admission row is immutable; same admission key returns this row and reissues the derived ticket without incrementing; append-only |
| `sharing.share_access_event` | `sharing` | V009 | S3 | `id`, `share_grant_id`, `share_admission_id`, `event_domain`, `event_id`, `event_type`, `result`, `occurred_at`, `risk_digest`, `payload_redacted`, `payload_sha256` | `event_domain` check `SHARE_ACCESS`; unique `(event_domain, event_id)`; append-only; records verify, lock, admission, ticket issue, ticket failure, playback, extension, rotation, revoke, expiry and failure outcomes; payload is redacted; append-only |
<!-- catalog:end -->

### 4.1.1 Frozen baseline matrix (44 rows)

The accepted table-to-owner-to-wave-to-slice-to-lifecycle-to-sensitive map is verbatim from the accepted plan. It is a structural baseline, not an S2/S3 implementation claim.

<!-- baseline-matrix:start -->
platform.idempotency_record|platform-reliability|V001|S1|MUTABLE_HEAD|Y
platform.outbox_event|platform-reliability|V001|S1|MUTABLE_HEAD|Y
audit.audit_fact|operations-audit|V001|S1|APPEND_ONLY|Y
customer_iam.customer_account|customer-iam|V002|S1|MUTABLE_HEAD|N
customer_iam.auth_identity|customer-iam|V002|S1|MUTABLE_HEAD|Y
customer_iam.identity_profile|customer-iam|V002|S1|MUTABLE_HEAD|Y
organization_access.organization|organization-access|V002|S1|MUTABLE_HEAD|N
organization_access.organization_membership|organization-access|V002|S1|MUTABLE_HEAD|N
operations_iam.employee_account|operations-iam|V002|S1|MUTABLE_HEAD|Y
subject_number.business_subject|subject-number|V003|S1|MUTABLE_HEAD|N
subject_number.caller_number_binding|subject-number|V003|S1|MUTABLE_HEAD|Y
subject_number.subject_claim|subject-number|V003|S1|APPEND_ONLY|Y
subject_number.subject_risk_state|subject-number|V003|S1|MUTABLE_HEAD|N
catalog_order.catalog_product|catalog-order|V004|S2|APPEND_ONLY|N
catalog_order.catalog_version|catalog-order|V004|S2|MUTABLE_HEAD|Y
catalog_order.catalog_offer|catalog-order|V004|S2|MUTABLE_HEAD|N
catalog_order.catalog_benefit_line|catalog-order|V004|S2|APPEND_ONLY|N
catalog_order.customer_order|catalog-order|V004|S2|MUTABLE_HEAD|Y
catalog_order.payment_order|catalog-order|V004|S2|MUTABLE_HEAD|Y
catalog_order.payment_event|catalog-order|V004|S2|APPEND_ONLY|Y
ivr.outbound_call_task|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.inbound_session|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.call_record|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.call_leg|ivr|V005|S2|MUTABLE_HEAD|N
ivr.call_event|ivr|V005|S2|MUTABLE_HEAD|Y
ivr.ivr_decision|ivr|V005|S2|APPEND_ONLY|Y
entitlement_metering.trial_policy_version|entitlement-metering|V006|S2|MUTABLE_HEAD|Y
entitlement_metering.entitlement_grant|entitlement-metering|V006|S2|APPEND_ONLY|Y
entitlement_metering.trial_grant|entitlement-metering|V006|S2|APPEND_ONLY|N
entitlement_metering.entitlement_balance|entitlement-metering|V006|S2|MUTABLE_HEAD|N
entitlement_metering.entitlement_issuance|entitlement-metering|V006|S2|MUTABLE_HEAD|N
entitlement_metering.usage_reservation|entitlement-metering|V006|S2|MUTABLE_HEAD|N
entitlement_metering.usage_fact|entitlement-metering|V006|S2|APPEND_ONLY|Y
activation_consent.consent_record|activation-consent|V007|S2|APPEND_ONLY|Y
activation_consent.service_activation|activation-consent|V007|S2|MUTABLE_HEAD|N
activation_consent.service_activation_transition|activation-consent|V007|S2|APPEND_ONLY|N
evidence.media_object|evidence|V008|S3|APPEND_ONLY|Y
evidence.recording|evidence|V008|S3|MUTABLE_HEAD|Y
evidence.evidence_record|evidence|V008|S3|MUTABLE_HEAD|Y
evidence.evidence_artifact|evidence|V008|S3|APPEND_ONLY|N
evidence.evidence_event|evidence|V008|S3|APPEND_ONLY|Y
sharing.evidence_share_grant|sharing|V009|S3|MUTABLE_HEAD|Y
sharing.share_admission|sharing|V009|S3|APPEND_ONLY|Y
sharing.share_access_event|sharing|V009|S3|APPEND_ONLY|Y
<!-- baseline-matrix:end -->

### 4.2 S1 dictionary conventions

Every S1 dictionary below is complete for the accepted column set. "Default" means the database default used when the application omits the column; "none" means the application must supply the value. Caller-owned rows use an application-generated UUID with no database default. The three function-inserted row types listed in section 2.2 generate `id` with `pg_catalog.gen_random_uuid()` inside the function body; no table default and no function `p_id` argument exists. Every foreign key is `ON UPDATE RESTRICT ON DELETE RESTRICT`. Hash columns enforce lowercase 64-character SHA-256 or HMAC-SHA-256 hexadecimal text unless a narrower rule is stated. Every `request_fingerprint` is canonical semantic SHA-256 as defined in section 2.5.

Table-level named constraints and indexes are listed after each column table. Index names are stable generation targets. Partial indexes use the exact state predicate shown.

### 4.3 platform.idempotency_record

Purpose: durable action-domain idempotency record with a write-once first result. Owner: `platform-reliability`. Wave V001; slice S1. Lifecycle: mutable head with exactly one `NEW → COMPLETED` finalization; the completed result is immutable. Deletion: no ordinary delete; retention is a later operations input (Q13).

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_idempotency_record` | Generated by `platform.idempotency_begin` with `pg_catalog.gen_random_uuid()`; no table default; never a public key |
| scope | text | NO | none | named CHECK `ck_idempotency_scope` | `IVR_CONFIRM` or `PURCHASE` in the accepted baseline; a new scope requires an approved later migration |
| idempotency_key | text | NO | none | part of `uq_idempotency_scope_key` | Non-empty, length 1-512; the transport event ID for `IVR_CONFIRM`, `account_id || ':' || client key` for `PURCHASE` |
| request_fingerprint | text | NO | none | CHECK `ck_idempotency_fingerprint` | Lowercase SHA-256 hex of the canonical semantic fingerprint for the scope |
| status | text | NO | `'NEW'` | CHECK `ck_idempotency_status` | `NEW` or `COMPLETED`; no `CONFLICT` state |
| result_type | text | YES | none | CHECK `ck_idempotency_result_type` | Null for `NEW`; non-empty stable result type for `COMPLETED` |
| result_id | uuid | YES | none | none | Null for `NEW`; the first immutable result identity for `COMPLETED` |
| result_snapshot | jsonb | YES | none | CHECK `ck_idempotency_result_snapshot_type` | Null for `NEW`; redacted or encrypted canonical typed result for `COMPLETED` |
| result_sha256 | text | YES | none | CHECK `ck_idempotency_result_sha256` | Null for `NEW`; lowercase SHA-256 of the canonical redacted result for `COMPLETED` |
| created_at | timestamptz | NO | `now()` | none | Transaction creation time |
| completed_at | timestamptz | YES | none | CHECK `ck_idempotency_completed_after_created` | Null for `NEW`; set once by `platform.idempotency_complete` using `clock_timestamp()` |
| revision | integer | NO | `1` | CHECK `ck_idempotency_revision` | `1` for `NEW`, exactly `2` for `COMPLETED` |

Named checks:

- `ck_idempotency_scope`: `scope IN ('IVR_CONFIRM','PURCHASE')`.
- `ck_idempotency_key`: `length(idempotency_key) BETWEEN 1 AND 512 AND btrim(idempotency_key) <> ''`.
- `ck_idempotency_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.
- `ck_idempotency_status`: `status IN ('NEW','COMPLETED')`.
- `ck_idempotency_result_type`: `result_type IS NULL OR (length(result_type) BETWEEN 1 AND 64 AND btrim(result_type) <> '')`.
- `ck_idempotency_result_snapshot_type`: `result_snapshot IS NULL OR jsonb_typeof(result_snapshot) IN ('object','array')`.
- `ck_idempotency_result_sha256`: `result_sha256 IS NULL OR result_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_idempotency_revision`: `revision >= 1`.
- `ck_idempotency_state_shape`: `(status = 'NEW' AND result_type IS NULL AND result_id IS NULL AND result_snapshot IS NULL AND result_sha256 IS NULL AND completed_at IS NULL AND revision = 1) OR (status = 'COMPLETED' AND result_type IS NOT NULL AND result_id IS NOT NULL AND result_snapshot IS NOT NULL AND result_sha256 IS NOT NULL AND completed_at IS NOT NULL AND revision = 2)`.
- `ck_idempotency_completed_after_created`: `completed_at IS NULL OR completed_at >= created_at`.

Keys and indexes:

- Primary key `pk_idempotency_record (id)`.
- Unique `uq_idempotency_scope_key (scope, idempotency_key)`.
- Partial index `ix_idempotency_new_alert (created_at) WHERE status = 'NEW'` for the committed-`NEW` alert query.
- No foreign key.

Revision/immutability: `platform.idempotency_begin` inserts `NEW` at revision 1; `platform.idempotency_complete` is the only path that updates the row to `COMPLETED` at revision 2. The result columns and `completed_at` are immutable after completion; no update may change a completed row. A committed `NEW` row is an alertable contract violation, not a stored success.

Sensitive representation: `result_snapshot` is redacted or encrypted before insertion; `result_sha256` hashes the redacted snapshot. Full numbers, provider subjects, identity values, callee numbers, share codes and secrets are prohibited. The function denylist in section 10.7 applies.

### 4.4 platform.outbox_event

Purpose: transactional Outbox fact for at-least-once asynchronous delivery. Owner: `platform-reliability`. Wave V001; slice S1. Lifecycle: mutable head for dispatcher state only; the event identity and payload are immutable. Deletion: no ordinary delete; retention and dead-letter thresholds are later operations inputs (Q13).

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_outbox_event` | Generated by `platform.outbox_append` with `pg_catalog.gen_random_uuid()`; no table default |
| event_domain | text | NO | none | part of `uq_outbox_domain_dedupe`; CHECK `ck_outbox_event_domain` | One of the baseline action/event domains; schema-qualified domain registry |
| dedupe_key | text | NO | none | part of `uq_outbox_domain_dedupe` | Non-empty stable domain-scoped key, length 1-512 |
| aggregate_type | text | NO | none | CHECK `ck_outbox_aggregate_type` | Stable aggregate type, length 1-128 |
| aggregate_id | uuid | NO | none | none | Internal aggregate identity; polymorphic reference with no FK |
| event_type | text | NO | none | CHECK `ck_outbox_event_type` | Stable event type, length 1-128 |
| payload_redacted | jsonb | YES | none | CHECK `ck_outbox_payload_present` | Redacted payload; at least one of this or ciphertext is required |
| payload_ciphertext | text | YES | none | CHECK `ck_outbox_ciphertext_shape` | Encrypted payload envelope; at least one of this or redacted is required |
| payload_sha256 | text | NO | none | CHECK `ck_outbox_payload_sha256` | Lowercase SHA-256 of the canonical redacted payload, or of the ciphertext envelope when no redacted payload exists |
| status | text | NO | `'PENDING'` | CHECK `ck_outbox_status` | `PENDING`, `PUBLISHED` or `FAILED` |
| attempt_count | integer | NO | `0` | CHECK `ck_outbox_attempt_count` | Non-negative dispatcher attempt count |
| available_at | timestamptz | NO | `now()` | none | Initial enqueue time; dispatcher backoff may move it forward |
| published_at | timestamptz | YES | none | CHECK `ck_outbox_published_shape` | Non-null exactly when `status = 'PUBLISHED'` |
| revision | integer | NO | `1` | CHECK `ck_outbox_revision` | Monotonic dispatcher revision |

Named checks:

- `ck_outbox_event_domain`: `event_domain IN ('ACTIVATION','IVR_CONFIRM','ACTIVATION_STATUS','OPERATIONS','IVR_EVENT','IVR_DIRECT_AUTH','EVIDENCE','PAYMENT','SHARE_ACCESS','PURCHASE','SUBJECT_CLAIM')`.
- `ck_outbox_dedupe_key`: `length(dedupe_key) BETWEEN 1 AND 512 AND btrim(dedupe_key) <> ''`.
- `ck_outbox_aggregate_type`: `length(aggregate_type) BETWEEN 1 AND 128 AND btrim(aggregate_type) <> ''`.
- `ck_outbox_event_type`: `length(event_type) BETWEEN 1 AND 128 AND btrim(event_type) <> ''`.
- `ck_outbox_payload_present`: `payload_redacted IS NOT NULL OR payload_ciphertext IS NOT NULL`.
- `ck_outbox_ciphertext_shape`: `payload_ciphertext IS NULL OR payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_outbox_payload_sha256`: `payload_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_outbox_status`: `status IN ('PENDING','PUBLISHED','FAILED')`.
- `ck_outbox_attempt_count`: `attempt_count >= 0`.
- `ck_outbox_published_shape`: `(status = 'PUBLISHED') = (published_at IS NOT NULL)`.
- `ck_outbox_published_after_available`: `published_at IS NULL OR published_at >= available_at`.
- `ck_outbox_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_outbox_event (id)`.
- Unique `uq_outbox_domain_dedupe (event_domain, dedupe_key)`.
- Partial index `ix_outbox_pending_available (available_at, id) WHERE status = 'PENDING'`.
- Partial index `ix_outbox_failed_available (available_at, id) WHERE status = 'FAILED'`.
- No foreign key; `aggregate_id` is an intentional polymorphic reference and is not a foreign key.

Revision/immutability: domain roles append through `platform.outbox_append` only. The dispatcher role may update only `status`, `attempt_count`, `available_at`, `published_at` and `revision`; it cannot update event identity, payload or hash. `PUBLISHED` is terminal. `FAILED` may return to `PENDING` under a later dispatcher attempt, with `attempt_count` increment and a new `available_at`; payload remains byte-for-byte unchanged.

Sensitive representation: payload is redacted or encrypted; `payload_redacted` must not contain raw numbers, provider subjects, identities, callee, object keys or provider secrets. The function denylist in section 10.7 applies.

### 4.5 audit.audit_fact

Purpose: append-only internal audit fact. Owner: `operations-audit`. Wave V001; slice S1. Lifecycle: append-only/frozen. Deletion: no update or delete privilege; retention is a later legal/operations input (Q3, Q13).

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_audit_fact` | Generated by `audit.append_fact` with `pg_catalog.gen_random_uuid()`; no table default |
| occurred_at | timestamptz | NO | none | none | Caller-supplied event time; not a mutable state token |
| actor_type | text | NO | none | CHECK `ck_audit_actor_type` | `ACCOUNT`, `EMPLOYEE`, `SERVICE` or `SYSTEM` |
| actor_id | uuid | YES | none | CHECK `ck_audit_actor_id` | Required for `ACCOUNT`, `EMPLOYEE` and `SERVICE`; null only for `SYSTEM` |
| action_code | text | NO | none | CHECK `ck_audit_action_code` | Stable uppercase code, length 2-128 |
| object_type | text | NO | none | CHECK `ck_audit_object_type` | Stable internal object type, length 1-64 |
| object_id | uuid | YES | none | CHECK `ck_audit_object_id` | Required unless `object_type = 'SYSTEM'` |
| reason_code | text | YES | none | CHECK `ck_audit_reason_code` | Required for high-risk actions; otherwise nullable |
| before_snapshot | jsonb | YES | none | CHECK `ck_audit_before_type` | Redacted or encrypted internal snapshot |
| after_snapshot | jsonb | YES | none | CHECK `ck_audit_after_type` | Redacted or encrypted internal snapshot |
| request_id | text | YES | none | none | Internal request correlation; length 1-128 when present |
| trace_id | text | YES | none | none | Internal trace correlation; length 1-128 when present |

Named checks:

- `ck_audit_actor_type`: `actor_type IN ('ACCOUNT','EMPLOYEE','SERVICE','SYSTEM')`.
- `ck_audit_actor_id`: `(actor_type = 'SYSTEM') OR actor_id IS NOT NULL`.
- `ck_audit_action_code`: `action_code ~ '^[A-Z][A-Z0-9_]{1,127}$'`.
- `ck_audit_object_type`: `length(object_type) BETWEEN 1 AND 64 AND btrim(object_type) <> ''`.
- `ck_audit_object_id`: `object_id IS NOT NULL OR object_type = 'SYSTEM'`.
- `ck_audit_reason_code`: `reason_code IS NULL OR (length(reason_code) BETWEEN 1 AND 128 AND btrim(reason_code) <> '')`.
- `ck_audit_before_type`: `before_snapshot IS NULL OR jsonb_typeof(before_snapshot) IN ('object','array')`.
- `ck_audit_after_type`: `after_snapshot IS NULL OR jsonb_typeof(after_snapshot) IN ('object','array')`.
- `ck_audit_request_id`: `request_id IS NULL OR length(request_id) BETWEEN 1 AND 128`.
- `ck_audit_trace_id`: `trace_id IS NULL OR length(trace_id) BETWEEN 1 AND 128`.

Keys and indexes:

- Primary key `pk_audit_fact (id)`.
- Index `ix_audit_object_time (object_type, object_id, occurred_at DESC)`.
- Index `ix_audit_actor_time (actor_type, actor_id, occurred_at DESC)`.
- Index `ix_audit_action_time (action_code, occurred_at DESC)`.
- Index `ix_audit_occurred_at (occurred_at DESC)`.
- No foreign key; `object_id` is an intentional internal polymorphic reference.

Append-only control: a `BEFORE UPDATE OR DELETE` trigger `trg_audit_fact_immutable` calls `audit.reject_row_mutation()` and raises a stable exception. The trigger function is a structural guard, not a cross-schema function contract. `operations-audit` may `SELECT`; no role other than `audit_owner` has table write privilege, and the only write path is `audit.append_fact`. `TRUNCATE` is revoked.

Sensitive representation: both snapshots are redacted or encrypted; no plaintext secret, raw number, identity number, provider subject, callee or object key. The function denylist in section 10.7 applies.

### 4.6 customer_iam.customer_account

Purpose: natural-person customer account. Owner: `customer-iam`. Wave V002; slice S1. Lifecycle: mutable head, status retirement; no hard delete. A personal workspace is derived from this row and has no separate table.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_customer_account` | Stable internal identity; no public natural key |
| display_name | text | NO | none | CHECK `ck_customer_account_display_name` | Server-supplied display name, length 1-200 |
| status | text | NO | `'ACTIVE'` | CHECK `ck_customer_account_status` | `ACTIVE` or `DISABLED` |
| revision | integer | NO | `1` | CHECK `ck_customer_account_revision` | Conditional-update revision |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| updated_at | timestamptz | NO | `now()` | CHECK `ck_customer_account_time_order` | Last successful state update |

Named checks:

- `ck_customer_account_display_name`: `length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''`.
- `ck_customer_account_status`: `status IN ('ACTIVE','DISABLED')`.
- `ck_customer_account_revision`: `revision >= 1`.
- `ck_customer_account_time_order`: `updated_at >= created_at`.

Keys and indexes:

- Primary key `pk_customer_account (id)`.
- Index `ix_customer_account_status (status)`.
- No natural-key unique index. No foreign key.

Revision/immutability: all status/display updates are conditional on the expected revision and increment it. `id` and `created_at` are immutable; `customer_iam_role` has column `UPDATE` only on `display_name`, `status`, `revision` and `updated_at`, and `customer_iam.guard_customer_account` rejects any other changed column. Account disable/re-enable takes the account lifecycle advisory lock and the account row lock in section 9.0. `DISABLED` blocks login; re-enabling is an operations action with reason and audit, never a customer self-service transition. Account rows are never deleted; disabling removes login and workspace access but preserves the personal identity and historical facts.

Sensitive representation: no sensitive column on this table. `display_name` must not be used as a login name, phone number or public key.

### 4.7 customer_iam.auth_identity

Purpose: a verified WeChat or login-phone identity that can authenticate one customer account. Owner: `customer-iam`. Wave V002; slice S1. Lifecycle: mutable head; status and verification metadata use revision. No credential, OTP or session material is stored.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_auth_identity` | Internal identity |
| account_id | uuid | NO | none | FK `fk_auth_identity_account` | Exactly one account per identity |
| provider | text | NO | none | CHECK `ck_auth_identity_provider` | `WECHAT` or `PHONE` |
| provider_scope | text | NO | none | CHECK `ck_auth_identity_scope` | App identifier or phone-login scope, length 1-128 |
| provider_subject_ciphertext | text | NO | none | CHECK `ck_auth_identity_ciphertext` | Versioned encrypted provider subject |
| provider_subject_hmac | text | NO | none | part of `uq_auth_identity_subject`; CHECK `ck_auth_identity_hmac` | HMAC-SHA-256 lookup value |
| status | text | NO | `'ACTIVE'` | CHECK `ck_auth_identity_status` | `ACTIVE` or `DISABLED` |
| verified_at | timestamptz | NO | none | none | Verification time; an unverified identity cannot be inserted |
| key_version | integer | NO | none | CHECK `ck_auth_identity_key_version` | Opaque positive key-generation reference |
| revision | integer | NO | `1` | CHECK `ck_auth_identity_revision` | Conditional-update revision |

Named checks:

- `ck_auth_identity_provider`: `provider IN ('WECHAT','PHONE')`.
- `ck_auth_identity_scope`: `length(provider_scope) BETWEEN 1 AND 128 AND btrim(provider_scope) <> ''`.
- `ck_auth_identity_ciphertext`: `provider_subject_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_auth_identity_hmac`: `provider_subject_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_auth_identity_status`: `status IN ('ACTIVE','DISABLED')`.
- `ck_auth_identity_key_version`: `key_version >= 1`.
- `ck_auth_identity_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_auth_identity (id)`.
- Unique `uq_auth_identity_subject (provider, provider_scope, provider_subject_hmac)`.
- Index `ix_auth_identity_account_status (account_id, status)`.
- FK `fk_auth_identity_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: status changes and verification updates are conditional revision updates. The provider/account relation (`id`, `account_id`, `provider`, `provider_scope`) is immutable and `customer_iam.guard_auth_identity` rejects any change to it. `customer_iam_role` has column `UPDATE` only on `status`, `verified_at`, `key_version` and `revision`; it cannot write the provider-subject ciphertext or HMAC. A provider-subject re-encryption/key-rotation operation is a later controlled security operation (Q2): it may change the ciphertext, HMAC and `key_version` together only while preserving the logical provider subject and the immutable relation columns, and it is not a normal runtime service path. Changing provider or subject requires a new identity row under the unique constraint. An identity is never rebound to another account. A concurrent identity insert loser reads the existing row and returns an idempotent success for the same account or a stable `IDENTITY_CONFLICT` for a different account.

Sensitive representation: `provider_subject_ciphertext` and `provider_subject_hmac` are ciphertext/HMAC only; the HMAC is not a public identifier and neither value is returned. `key_version` is internal.

### 4.8 customer_iam.identity_profile

Purpose: account-level real-name profile, independent of login success and independent of subject-level real-name. Owner: `customer-iam`. Wave V002; slice S1. Lifecycle: mutable head with revision; verified sensitive fields are set only by the identity-verification flow. Deletion: no ordinary delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_identity_profile` | Internal identity |
| account_id | uuid | NO | none | UNIQUE `uq_identity_profile_account`; FK `fk_identity_profile_account` | One profile per account |
| real_name_status | text | NO | `'UNVERIFIED'` | CHECK `ck_identity_profile_status` | `UNVERIFIED` or `VERIFIED` |
| legal_name_ciphertext | text | YES | none | CHECK `ck_identity_profile_legal_ciphertext` | Versioned encrypted legal name; null when unverified |
| id_number_ciphertext | text | YES | none | CHECK `ck_identity_profile_id_ciphertext` | Versioned encrypted identity number; null when unverified |
| id_number_hmac | text | YES | none | CHECK `ck_identity_profile_id_hmac` | HMAC lookup value; null when unverified |
| verification_ref_ciphertext | text | YES | none | CHECK `ck_identity_profile_verification_ref` | Encrypted verification reference; null when unverified |
| verified_at | timestamptz | YES | none | CHECK `ck_identity_profile_verified_shape` | Non-null exactly when verified |
| revision | integer | NO | `1` | CHECK `ck_identity_profile_revision` | Conditional-update revision |

Named checks:

- `ck_identity_profile_status`: `real_name_status IN ('UNVERIFIED','VERIFIED')`.
- `ck_identity_profile_legal_ciphertext`: `legal_name_ciphertext IS NULL OR legal_name_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_identity_profile_id_ciphertext`: `id_number_ciphertext IS NULL OR id_number_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_identity_profile_id_hmac`: `id_number_hmac IS NULL OR id_number_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_identity_profile_verification_ref`: `verification_ref_ciphertext IS NULL OR verification_ref_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_identity_profile_state_shape`: `(real_name_status = 'UNVERIFIED' AND legal_name_ciphertext IS NULL AND id_number_ciphertext IS NULL AND id_number_hmac IS NULL AND verification_ref_ciphertext IS NULL AND verified_at IS NULL) OR (real_name_status = 'VERIFIED' AND legal_name_ciphertext IS NOT NULL AND id_number_ciphertext IS NOT NULL AND id_number_hmac IS NOT NULL AND verification_ref_ciphertext IS NOT NULL AND verified_at IS NOT NULL)`.
- `ck_identity_profile_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_identity_profile (id)`.
- Unique `uq_identity_profile_account (account_id)`.
- FK `fk_identity_profile_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `id` and `account_id` are immutable; `customer_iam.guard_identity_profile` rejects a change to either relation column. `customer_iam_role` has column `UPDATE` only on the real-name/protected fields, `verified_at` and `revision`. Verification and re-verification are conditional revision updates by `customer_iam_role`; a status reset to `UNVERIFIED` must clear all protected fields in the same update. This account-level profile never substitutes for `subject_number.business_subject.real_name_status` and is never written by a claim, binding, IVR, activation or trial flow.

Sensitive representation: all four protected columns are encrypted envelopes; `id_number_hmac` is a lookup value only. No plaintext legal name, identity number or verification reference is returned or logged.


### 4.9 organization_access.organization

Purpose: organization workspace identity. It is not a login account and has no shared credential. Owner: `organization-access`. Wave V002; slice S1. Lifecycle: mutable head with status; no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_organization` | Internal identity |
| display_name | text | NO | none | CHECK `ck_organization_display_name` | Display name, length 1-200 |
| status | text | NO | `'ACTIVE'` | CHECK `ck_organization_status` | `ACTIVE`, `SUSPENDED` or `DISABLED` |
| revision | integer | NO | `1` | CHECK `ck_organization_revision` | Conditional-update revision |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| updated_at | timestamptz | NO | `now()` | CHECK `ck_organization_time_order` | Last state update |

Named checks:

- `ck_organization_display_name`: `length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''`.
- `ck_organization_status`: `status IN ('ACTIVE','SUSPENDED','DISABLED')`.
- `ck_organization_revision`: `revision >= 1`.
- `ck_organization_time_order`: `updated_at >= created_at`.

Keys and indexes:

- Primary key `pk_organization (id)`.
- Index `ix_organization_status (status)`.
- No login-identity or credential column. No foreign key.

Revision/immutability: `id` and `created_at` are immutable; `organization_access_role` has column `UPDATE` only on `display_name`, `status`, `revision` and `updated_at`, and `organization_access.guard_organization` rejects any other changed column. Status changes are conditional revision updates with reason and audit, under the organization lifecycle advisory lock and row lock in section 9.0. `SUSPENDED` and `DISABLED` organizations do not appear as usable workspaces and are explicit access predicates for later task-backed IVR calls. The organization row is never deleted; retirement is status-based.

Sensitive representation: no sensitive column on this table.

### 4.10 organization_access.organization_membership

Purpose: natural-person membership in one organization workspace. It grants workspace access and role after login; it grants no independent login. Owner: `organization-access`. Wave V002; slice S1. Lifecycle: mutable head; revocation is terminal for the row, and a later membership is a new row.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_organization_membership` | Internal identity |
| organization_id | uuid | NO | none | FK `fk_membership_organization`; part of `uq_membership_live` | Organization workspace |
| account_id | uuid | NO | none | FK `fk_membership_account`; part of `uq_membership_live` | Natural-person account |
| role | text | NO | none | CHECK `ck_membership_role` | `ADMINISTRATOR`, `MEMBER`, `EVIDENCE_MANAGER` or `AUDITOR` |
| department_label | text | YES | none | CHECK `ck_membership_department` | Display-only department label, length 1-200 when present |
| status | text | NO | `'ACTIVE'` | CHECK `ck_membership_status` | `ACTIVE`, `SUSPENDED` or `REVOKED` |
| revision | integer | NO | `1` | CHECK `ck_membership_revision` | Conditional-update revision |

Named checks:

- `ck_membership_role`: `role IN ('ADMINISTRATOR','MEMBER','EVIDENCE_MANAGER','AUDITOR')`.
- `ck_membership_department`: `department_label IS NULL OR (length(department_label) BETWEEN 1 AND 200 AND btrim(department_label) <> '')`.
- `ck_membership_status`: `status IN ('ACTIVE','SUSPENDED','REVOKED')`.
- `ck_membership_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_organization_membership (id)`.
- Partial unique `uq_membership_live (organization_id, account_id) WHERE status <> 'REVOKED'`.
- Index `ix_membership_account_status (account_id, status)`.
- Index `ix_membership_organization_status_role (organization_id, status, role)`.
- FK `fk_membership_organization (organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_membership_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `id`, `organization_id` and `account_id` are immutable; `organization_access.guard_membership` rejects a change to any of them. `organization_access_role` has column `UPDATE` only on `role`, `department_label`, `status` and `revision`. `ACTIVE ↔ SUSPENDED` and `ACTIVE`/`SUSPENDED → REVOKED` are conditional revision updates under the membership lifecycle advisory lock and row lock in section 9.0. The advisory key is the stable logical pair `organization_access.membership:<organization_id>:<account_id>` with canonical lowercase UUID components, not the membership ID; a revoked row and a replacement row for the same organization/account therefore share the same lock key, and create/suspend/revoke/re-grant serialize with all pair readers. `REVOKED` is terminal for the row; re-granting membership inserts a new row, which the partial unique permits. Role and department changes use revision and audit. The table has no auth credential, login subject, password, OTP or session column; membership is never accepted as an authentication fact.

Sensitive representation: no sensitive column on this table; `department_label` is display-only and must not embed protected identifiers.

### 4.11 operations_iam.employee_account

Purpose: ONECALL operations employee identity in a separate authentication domain. Owner: `operations-iam`. Wave V002; slice S1. Lifecycle: mutable head; last enabled administrator is transaction-protected. Credentials and sessions remain outside business tables.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_employee_account` | Internal employee identity |
| login_name_hmac | text | NO | none | UNIQUE `uq_employee_login_hmac`; CHECK `ck_employee_login_hmac` | HMAC of the backend login name |
| login_name_masked | text | NO | none | CHECK `ck_employee_login_masked` | Masked display value, length 1-128 |
| auth_issuer | text | NO | none | part of `uq_employee_issuer_subject`; CHECK `ck_employee_issuer` | Separate operations issuer, length 1-128 |
| auth_subject_hmac | text | NO | none | part of `uq_employee_issuer_subject`; CHECK `ck_employee_subject_hmac` | HMAC of the issuer subject |
| display_name | text | NO | none | CHECK `ck_employee_display_name` | Display name, length 1-200 |
| role | text | NO | none | CHECK `ck_employee_role` | `ADMIN` or `OPS` |
| status | text | NO | `'ACTIVE'` | CHECK `ck_employee_status` | `ACTIVE` or `DISABLED` |
| revision | integer | NO | `1` | CHECK `ck_employee_revision` | Conditional-update revision |

Named checks:

- `ck_employee_login_hmac`: `login_name_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_employee_login_masked`: `length(login_name_masked) BETWEEN 1 AND 128 AND btrim(login_name_masked) <> ''`.
- `ck_employee_issuer`: `length(auth_issuer) BETWEEN 1 AND 128 AND btrim(auth_issuer) <> ''`.
- `ck_employee_subject_hmac`: `auth_subject_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_employee_display_name`: `length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''`.
- `ck_employee_role`: `role IN ('ADMIN','OPS')`.
- `ck_employee_status`: `status IN ('ACTIVE','DISABLED')`.
- `ck_employee_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_employee_account (id)`.
- Unique `uq_employee_login_hmac (login_name_hmac)`.
- Unique `uq_employee_issuer_subject (auth_issuer, auth_subject_hmac)`.
- Partial index `ix_employee_enabled_admin (id) WHERE role = 'ADMIN' AND status = 'ACTIVE'` for the last-admin guard.
- Index `ix_employee_auth_issuer_status (auth_issuer, status)`.
- No foreign key; employee identity is independent of customer accounts and business subjects.

Revision/immutability: `id`, `login_name_hmac`, `auth_issuer` and `auth_subject_hmac` are immutable; `operations_iam.guard_employee_account` rejects a change to any of them. `operations_iam_role` has column `UPDATE` only on `display_name`, `role`, `status` and `revision`. Role/status changes are conditional revision updates and audit. The last enabled `ADMIN` cannot be disabled or demoted: the operations service takes transaction advisory lock `pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation', 0))`, counts enabled administrators and updates only if the count would remain at least one. Logout/session revocation is an operations-iam service action outside this table. Credentials, password hashes, OTPs and sessions are not stored here. `DISABLED` employees cannot authenticate; re-enabling is an operations action with reason and audit.

Sensitive representation: login name and issuer subject are HMAC-only for lookup; `login_name_masked` is display-only. The masked value must not be the full login name.

### 4.12 subject_number.business_subject

Purpose: stable business subject for one business number, independent of login identity, account real-name and activation. Owner: `subject-number`. Wave V003; slice S1. Lifecycle: mutable head; owner is set at most once from null, real-name is independent, and the row is never deleted.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_business_subject` | Stable subject identity |
| owner_account_id | uuid | YES | none | FK `fk_subject_owner_account`; part of `ck_subject_single_owner` | At most one personal owner |
| owner_organization_id | uuid | YES | none | FK `fk_subject_owner_organization`; part of `ck_subject_single_owner` | At most one organization owner |
| source | text | NO | none | CHECK `ck_subject_source` | `IVR`, `MINIAPP`, `WEB` or `OPERATIONS` |
| real_name_status | text | NO | `'UNVERIFIED'` | CHECK `ck_subject_real_name_status` | `UNVERIFIED` or `VERIFIED` |
| real_name_verified_at | timestamptz | YES | none | CHECK `ck_subject_real_name_shape` | Non-null exactly when verified |
| revision | integer | NO | `1` | CHECK `ck_subject_revision` | Conditional-update revision |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| updated_at | timestamptz | NO | `now()` | CHECK `ck_subject_time_order` | Last update |

Named checks:

- `ck_subject_single_owner`: `num_nonnulls(owner_account_id, owner_organization_id) <= 1`.
- `ck_subject_source`: `source IN ('IVR','MINIAPP','WEB','OPERATIONS')`.
- `ck_subject_real_name_status`: `real_name_status IN ('UNVERIFIED','VERIFIED')`.
- `ck_subject_real_name_shape`: `(real_name_status = 'VERIFIED') = (real_name_verified_at IS NOT NULL)`.
- `ck_subject_revision`: `revision >= 1`.
- `ck_subject_time_order`: `updated_at >= created_at`.

Keys and indexes:

- Primary key `pk_business_subject (id)`.
- Index `ix_subject_owner_account (owner_account_id)`.
- Index `ix_subject_owner_organization (owner_organization_id)`.
- Index `ix_subject_source_status (source, real_name_status)`.
- FK `fk_subject_owner_account (owner_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_subject_owner_organization (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `id`, `source` and `created_at` are immutable. `subject_number_role` has `INSERT` only on the non-owner creation columns (`id`, `source`, `revision`, `created_at`, `updated_at`) and column `UPDATE` only on `owner_account_id`, `revision` and `updated_at`; it has no `INSERT` privilege on either owner column and no `UPDATE` privilege on `owner_organization_id`. Every S1 subject creation source, including `OPERATIONS`, starts with both owner columns null, and an optional workspace or source never selects ownership. A subject may be created with both owner columns null and real-name `UNVERIFIED`. A verified personal claim sets `owner_account_id` only when both owner columns are null, using a conditional update with expected revision, and may do so in the same transaction as creation or later; the owner is then immutable for the subject, and a different owner is a conflict, never a transfer. An organization-scoped claim never sets an owner in S1. The owner columns and FKs remain for the legacy explicit enterprise relation and for reads; no S1 runtime role can create or transfer an enterprise owner. `subject_number.guard_subject_owner` rejects any change to `id`, `source` or `created_at`, rejects clearing or changing an owner that is already set, and rejects real-name changes unless `current_user = 'subject_identity_verification_role'`. A new organization-scoped claim is fail-closed under section 9.6; it never sets `owner_organization_id` from an optional organization ID. No claim, binding, IVR, activation, trial or order path writes `real_name_status` or `real_name_verified_at`. Those two columns are updated only by `subject_identity_verification_role` through the separately approved subject-identity verification flow, which has `SELECT` on `id`, the real-name/status columns, `revision` and `updated_at`, and column `UPDATE` only on the two real-name columns, `revision` and `updated_at`. The subject-number claim runtime role has no update privilege on them. A verified subject may later be claimed; claim does not change real-name.

Sensitive representation: no raw number or identity value on this table. `source` and real-name state are internal facts, not public identifiers.

### 4.13 subject_number.caller_number_binding

Purpose: binding of one canonical business number to one business subject. It is not a login identity and does not grant account access. Owner: `subject-number`. Wave V003; slice S1. Lifecycle: mutable head with status; retired history remains.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_caller_number_binding` | Internal binding identity |
| subject_id | uuid | NO | none | FK `fk_binding_subject`; part of `uq_binding_subject_number` | Stable subject |
| number_type | text | NO | none | CHECK `ck_binding_number_type` | `MOBILE` or `LANDLINE` |
| number_ciphertext | text | NO | none | CHECK `ck_binding_number_ciphertext` | Versioned encrypted canonical number |
| number_hmac | text | NO | none | part of `uq_binding_subject_number` and `uq_binding_active_number`; CHECK `ck_binding_number_hmac` | HMAC lookup and concurrency key |
| number_masked | text | NO | none | CHECK `ck_binding_number_masked` | Display-only masked number, length 1-64 |
| status | text | NO | `'ACTIVE'` | CHECK `ck_binding_status` | `ACTIVE`, `DISABLED` or `RETIRED` |
| verified_at | timestamptz | YES | none | CHECK `ck_binding_verified_shape` | Required for an `ACTIVE` binding |
| revision | integer | NO | `1` | CHECK `ck_binding_revision` | Conditional-update revision |

Named checks:

- `ck_binding_number_type`: `number_type IN ('MOBILE','LANDLINE')`.
- `ck_binding_number_ciphertext`: `number_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_binding_number_hmac`: `number_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_binding_number_masked`: `length(number_masked) BETWEEN 1 AND 64 AND btrim(number_masked) <> ''`.
- `ck_binding_status`: `status IN ('ACTIVE','DISABLED','RETIRED')`.
- `ck_binding_verified_shape`: `status <> 'ACTIVE' OR verified_at IS NOT NULL`.
- `ck_binding_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_caller_number_binding (id)`.
- Unique `uq_binding_subject_number (subject_id, number_hmac)`.
- Partial unique `uq_binding_active_number (number_hmac) WHERE status = 'ACTIVE'`.
- Index `ix_binding_subject_status (subject_id, status)`.
- Index `ix_binding_hmac_status (number_hmac, status)`.
- FK `fk_binding_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `id`, `subject_id`, `number_type`, `number_ciphertext`, `number_hmac` and `number_masked` are immutable; `subject_number.guard_caller_number_binding` rejects any change to them. `subject_number_role` has column `UPDATE` only on `status`, `verified_at` and `revision`. Create, disable, re-enable and retire are conditional revision updates. Replacement creates a new row and retires the old row; it never rewrites the number identity in place. `RETIRED` is terminal; a historical row remains and is never deleted. Creating a second `ACTIVE` binding for the same number is prevented by the partial unique key and the two-number canonical lock rule in section 9.7. A binding change never creates an `auth_identity`, never changes `business_subject.real_name_status` and never rewrites evidence, trial or usage facts.

Sensitive representation: ciphertext for storage, HMAC for equality/lookup, masked for display. The full number is never a public key, log field or response field.

### 4.14 subject_number.subject_claim

Purpose: append-only verified relation attempt between a subject and an account/organization. Owner: `subject-number`. Wave V003; slice S1. Lifecycle: append-only/frozen; status is set at insert and never updated. No account, login identity, real-name value or evidence is created or rewritten by this table.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_subject_claim` | Internal claim identity |
| subject_id | uuid | NO | none | FK `fk_claim_subject` | Claimed subject |
| account_id | uuid | NO | none | FK `fk_claim_account`; part of `uq_claim_accepted_subject_account` | Claiming natural-person account |
| organization_id | uuid | YES | none | FK `fk_claim_organization` | Organization workspace when the claim is organization-scoped |
| verification_method | text | NO | none | CHECK `ck_claim_verification_method` | `SMS`, `WECHAT_PHONE`, `CALLER_CHALLENGE` or `MANUAL_REVIEW` |
| verification_ref_ciphertext | text | NO | none | CHECK `ck_claim_verification_ref` | Versioned encrypted verification evidence reference |
| request_id | text | NO | none | UNIQUE `uq_claim_request`; CHECK `ck_claim_request_id` | Opaque request identity, length 1-128 |
| status | text | NO | none | CHECK `ck_claim_status` | `ACCEPTED`, `REJECTED` or `CONFLICT`; set at insert |
| risk_checked_at | timestamptz | NO | none | none | Time the current risk decision was taken under the claim locks; it is not a proof-attempt time |
| verified_at | timestamptz | YES | none | CHECK `ck_claim_verified_shape` | Non-null exactly for `ACCEPTED`; the accepted-claim decision timestamp, taken at or after `risk_checked_at`, never the original proof-attempt time |

Named checks:

- `ck_claim_verification_method`: `verification_method IN ('SMS','WECHAT_PHONE','CALLER_CHALLENGE','MANUAL_REVIEW')`.
- `ck_claim_verification_ref`: `verification_ref_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_claim_request_id`: `length(request_id) BETWEEN 1 AND 128 AND btrim(request_id) <> ''`.
- `ck_claim_status`: `status IN ('ACCEPTED','REJECTED','CONFLICT')`.
- `ck_claim_verified_shape`: `(status = 'ACCEPTED') = (verified_at IS NOT NULL)`.
- `ck_claim_verified_after_risk`: `verified_at IS NULL OR verified_at >= risk_checked_at`.

Keys and indexes:

- Primary key `pk_subject_claim (id)`.
- Unique `uq_claim_request (request_id)`.
- Partial unique `uq_claim_accepted_subject_account (subject_id, account_id) WHERE status = 'ACCEPTED'`.
- Index `ix_claim_subject_status_time (subject_id, status, risk_checked_at DESC)`.
- Index `ix_claim_account_status (account_id, status)`.
- FK `fk_claim_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_claim_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_claim_organization (organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `BEFORE UPDATE OR DELETE` trigger `trg_subject_claim_immutable` calls `subject_number.reject_row_mutation()` and raises. `subject_number_role` has `INSERT` and `SELECT` only; no `UPDATE`, `DELETE` or `TRUNCATE`. The claim status and verified time are chosen before the single insert. Rejected and conflict attempts remain as history.

Sensitive representation: `verification_ref_ciphertext` is encrypted verification evidence, never a public claim identifier and never a raw number or code.

### 4.14.1 Verification-reference payload and claim replay identity

The accepted `verification_ref_ciphertext` column is the immutable claim-evidence reference and, without adding a column, the claim replay-identity carrier. Its decrypted plaintext is canonical JSON (section 2.5) inside the versioned encrypted envelope of section 2.6:

```json
{"schemaVersion":1,"subjectId":"<uuid>","bindingId":"<uuid>","numberHmac":"<64 lowercase hex>","verificationMethod":"SMS|WECHAT_PHONE|CALLER_CHALLENGE|MANUAL_REVIEW","proofRef":"<opaque stable verification-attempt reference, 1-128 characters>"}
```

- `schemaVersion` is `1` for S1; another version is unresolvable until a later migration defines it.
- `subjectId` must equal the row `subject_id`; `bindingId` must identify the verified `caller_number_binding`; `numberHmac` must equal that binding's `number_hmac`; `verificationMethod` must equal the row column; `proofRef` is the server-generated stable identity of the verification attempt (SMS/WeChat provider attempt, caller-challenge ID or manual-review reference). It is not a raw code, OTP, full number or PII. The `proofRef` may encode the original attempt or observation time inside the opaque reference; that timing is immutable verification evidence only and never populates `subject_claim.verified_at`.
- The owning service decrypts the envelope and compares logical values server-side. Ciphertext byte equality is never used as proof equality because AEAD encryption is randomized.
- A missing, malformed, wrong-version, undecryptable or incomplete payload is unresolvable. The claim is rejected with a stable fail-closed error and creates no accepted row and no owner change.
- The payload is written once with the append-only claim row and is immutable. Replay and same-account-duplicate comparison uses `subject_id`, null-safe `organization_id`, `account_id`, `verification_method` plus the decrypted `bindingId`, `numberHmac` and `proofRef`. A changed verification method is a context conflict for both replay and a same-account duplicate; it cannot reuse or disclose the previously accepted result. No new column, request-fingerprint column or platform idempotency scope is added.
- `verification_ref_ciphertext` is never returned as a public claim identifier.

### 4.15 subject_number.subject_risk_state

Purpose: current claim-risk state for one number binding. Owner: `subject-number`. Wave V003; slice S1. Lifecycle: mutable head with revision. `OPEN` blocks number claims and historical-evidence access; it is not by itself a ban on new outbound calling.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_subject_risk_state` | Internal risk fact |
| caller_number_binding_id | uuid | NO | none | FK `fk_risk_binding`; part of `uq_risk_binding_type` | Risk is scoped to a binding |
| risk_type | text | NO | none | CHECK `ck_risk_type`; part of `uq_risk_binding_type` | `RECYCLED`, `CONFLICT` or `MANUAL_REVIEW` |
| state | text | NO | `'OPEN'` | CHECK `ck_risk_state` | `OPEN` or `CLEARED` |
| source | text | NO | none | CHECK `ck_risk_source` | `CLAIM_CHECK`, `OPERATIONS` or `IMPORT` |
| observed_at | timestamptz | NO | `now()` | none | First observation time for the current record |
| cleared_at | timestamptz | YES | none | CHECK `ck_risk_cleared_shape` | Non-null exactly when cleared |
| revision | integer | NO | `1` | CHECK `ck_risk_revision` | Conditional-update revision |

Named checks:

- `ck_risk_type`: `risk_type IN ('RECYCLED','CONFLICT','MANUAL_REVIEW')`.
- `ck_risk_state`: `state IN ('OPEN','CLEARED')`.
- `ck_risk_source`: `source IN ('CLAIM_CHECK','OPERATIONS','IMPORT')`.
- `ck_risk_cleared_shape`: `(state = 'CLEARED') = (cleared_at IS NOT NULL)`.
- `ck_risk_cleared_after_observed`: `cleared_at IS NULL OR cleared_at >= observed_at`.
- `ck_risk_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_subject_risk_state (id)`.
- Unique `uq_risk_binding_type (caller_number_binding_id, risk_type)`.
- Partial index `ix_risk_open (caller_number_binding_id, observed_at) WHERE state = 'OPEN'`.
- Index `ix_risk_binding_state (caller_number_binding_id, state)`.
- FK `fk_risk_binding (caller_number_binding_id) REFERENCES subject_number.caller_number_binding(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `id`, `caller_number_binding_id` and `risk_type` are immutable; `subject_number.guard_subject_risk_state` rejects a change to them. `subject_number_role` has column `UPDATE` only on `state`, `source`, `observed_at`, `cleared_at` and `revision`. Open, re-open and clear are conditional revision updates. A cleared state may be re-opened only by an approved claim-risk or operations flow with reason and audit. Opening or clearing risk takes the same canonical-number lock used by claim binding, so claim/risk outcomes serialize. `OPEN` does not invent a dialing ban; later IVR access predicates may check risk only under a separately approved policy (Q24).

Sensitive representation: no raw number, identity or evidence content. The binding FK and risk type are internal facts.

## 5. Foreign-Key Catalog

### 5.1 Frozen planned FK catalog (81 decisions)

The following block is the accepted 81-row FK decision catalog. It is verbatim from the accepted plan. S1 expands only its 11 rows; the remaining 70 rows are planned S2/S3 handoff and are not S1 implementation. Mode names are exactly `FK`, `ALTER_FK`, `DEFERRABLE_FK` and `NO_FK`.

<!-- fk-matrix:start -->
customer_iam.auth_identity.account_id|customer_iam.customer_account.id|V002|V002|FK|same wave
customer_iam.identity_profile.account_id|customer_iam.customer_account.id|V002|V002|FK|same wave
organization_access.organization_membership.organization_id|organization_access.organization.id|V002|V002|FK|same wave
organization_access.organization_membership.account_id|customer_iam.customer_account.id|V002|V002|FK|same wave
subject_number.business_subject.owner_account_id|customer_iam.customer_account.id|V002|V003|FK|cross wave
subject_number.business_subject.owner_organization_id|organization_access.organization.id|V002|V003|FK|cross wave
subject_number.caller_number_binding.subject_id|subject_number.business_subject.id|V003|V003|FK|same wave
subject_number.subject_claim.subject_id|subject_number.business_subject.id|V003|V003|FK|same wave
subject_number.subject_claim.account_id|customer_iam.customer_account.id|V002|V003|FK|cross wave
subject_number.subject_claim.organization_id|organization_access.organization.id|V002|V003|FK|cross wave
subject_number.subject_risk_state.caller_number_binding_id|subject_number.caller_number_binding.id|V003|V003|FK|same wave
catalog_order.catalog_version.product_id|catalog_order.catalog_product.id|V004|V004|FK|same wave
catalog_order.catalog_version.created_by_employee_id|operations_iam.employee_account.id|V002|V004|FK|cross wave
catalog_order.catalog_offer.catalog_version_id|catalog_order.catalog_version.id|V004|V004|FK|same wave
catalog_order.catalog_benefit_line.catalog_version_id|catalog_order.catalog_version.id|V004|V004|FK|same wave
catalog_order.customer_order.account_id|customer_iam.customer_account.id|V002|V004|FK|cross wave
catalog_order.customer_order.owner_organization_id|organization_access.organization.id|V002|V004|FK|cross wave
catalog_order.customer_order.subject_id|subject_number.business_subject.id|V003|V004|FK|cross wave
catalog_order.customer_order.catalog_version_id|catalog_order.catalog_version.id|V004|V004|FK|same wave
catalog_order.customer_order.catalog_offer_id|catalog_order.catalog_offer.id|V004|V004|FK|same wave
catalog_order.customer_order.purchase_consent_id|activation_consent.consent_record.id|V007|V007|ALTER_FK|column in V004; ordinary immediate FK added by ALTER in V007, not deferrable
catalog_order.payment_order.order_id|catalog_order.customer_order.id|V004|V004|FK|same wave
catalog_order.payment_event.payment_order_id|catalog_order.payment_order.id|V004|V004|FK|same wave
ivr.outbound_call_task.caller_binding_id|subject_number.caller_number_binding.id|V003|V005|FK|cross wave
ivr.outbound_call_task.subject_id|subject_number.business_subject.id|V003|V005|FK|cross wave
ivr.outbound_call_task.owner_account_id|customer_iam.customer_account.id|V002|V005|FK|cross wave
ivr.outbound_call_task.owner_organization_id|organization_access.organization.id|V002|V005|FK|cross wave
ivr.outbound_call_task.created_by_account_id|customer_iam.customer_account.id|V002|V005|FK|cross wave
ivr.outbound_call_task.claimed_call_id|ivr.call_record.call_id|V005|V005|ALTER_FK|same-wave ALTER after call_record; ordinary immediate FK, not deferrable
ivr.inbound_session.subject_id|subject_number.business_subject.id|V003|V005|FK|cross wave
ivr.inbound_session.confirmed_subject_id|subject_number.business_subject.id|V003|V005|FK|cross wave
ivr.inbound_session.call_id|-|-|-|NO_FK|root IVR protocol identity with no durable parent
ivr.call_record.call_id|-|-|-|NO_FK|canonical call-scoped parent created in resolve or direct authorization
ivr.call_record.session_id|ivr.inbound_session.id|V005|V005|FK|same wave
ivr.call_record.task_id|ivr.outbound_call_task.id|V005|V005|FK|same wave nullable for direct path
ivr.call_leg.call_id|ivr.call_record.call_id|V005|V005|FK|same wave
ivr.call_event.call_id|ivr.call_record.call_id|V005|V005|FK|same wave
ivr.ivr_decision.session_id|ivr.inbound_session.id|V005|V005|FK|same wave
ivr.ivr_decision.call_id|ivr.call_record.call_id|V005|V005|FK|same wave
ivr.ivr_decision.reservation_id|entitlement_metering.usage_reservation.id|V006|V006|ALTER_FK|column in V005; ordinary immediate FK added by ALTER in V006, not deferrable
entitlement_metering.trial_policy_version.published_by_employee_id|operations_iam.employee_account.id|V002|V006|FK|cross wave
entitlement_metering.entitlement_grant.subject_id|subject_number.business_subject.id|V003|V006|FK|cross wave
entitlement_metering.entitlement_grant.order_id|catalog_order.customer_order.id|V004|V006|FK|cross wave
entitlement_metering.trial_grant.subject_id|subject_number.business_subject.id|V003|V006|FK|cross wave
entitlement_metering.trial_grant.trial_policy_version_id|entitlement_metering.trial_policy_version.id|V006|V006|FK|same wave
entitlement_metering.trial_grant.entitlement_grant_id|entitlement_metering.entitlement_grant.id|V006|V006|FK|same wave unique
entitlement_metering.entitlement_balance.grant_id|entitlement_metering.entitlement_grant.id|V006|V006|FK|same wave unique
entitlement_metering.entitlement_issuance.order_id|catalog_order.customer_order.id|V004|V006|FK|cross wave
entitlement_metering.entitlement_issuance.grant_id|entitlement_metering.entitlement_grant.id|V006|V006|FK|same wave nullable unique
entitlement_metering.usage_reservation.call_id|ivr.call_record.call_id|V005|V006|FK|cross wave
entitlement_metering.usage_reservation.balance_id|entitlement_metering.entitlement_balance.id|V006|V006|FK|same wave
entitlement_metering.usage_reservation.subject_id|subject_number.business_subject.id|V003|V006|FK|cross wave
entitlement_metering.usage_fact.reservation_id|entitlement_metering.usage_reservation.id|V006|V006|FK|same wave
entitlement_metering.usage_fact.balance_id|entitlement_metering.entitlement_balance.id|V006|V006|FK|same wave
entitlement_metering.usage_fact.call_id|ivr.call_record.call_id|V005|V006|FK|cross wave
activation_consent.consent_record.subject_id|subject_number.business_subject.id|V003|V007|FK|cross wave
activation_consent.consent_record.ivr_session_id|ivr.inbound_session.id|V005|V007|FK|cross wave nullable for MINIAPP and WEB
activation_consent.consent_record.account_id|customer_iam.customer_account.id|V002|V007|FK|cross wave nullable
activation_consent.consent_record.employee_id|operations_iam.employee_account.id|V002|V007|FK|cross wave nullable
activation_consent.service_activation.subject_id|subject_number.business_subject.id|V003|V007|FK|cross wave unique
activation_consent.service_activation_transition.activation_id|activation_consent.service_activation.id|V007|V007|FK|same wave
activation_consent.service_activation_transition.subject_id|subject_number.business_subject.id|V003|V007|FK|cross wave
activation_consent.service_activation_transition.actor_account_id|customer_iam.customer_account.id|V002|V007|FK|cross wave nullable
activation_consent.service_activation_transition.actor_employee_id|operations_iam.employee_account.id|V002|V007|FK|cross wave required for OPERATIONS
activation_consent.service_activation_transition.consent_record_id|activation_consent.consent_record.id|V007|V007|FK|same wave nullable for OPERATIONS
evidence.recording.call_id|ivr.call_record.call_id|V005|V008|FK|cross wave
evidence.recording.media_object_id|evidence.media_object.id|V008|V008|FK|same wave
evidence.evidence_record.subject_id|subject_number.business_subject.id|V003|V008|FK|cross wave
evidence.evidence_record.call_id|ivr.call_record.call_id|V005|V008|FK|cross wave nullable
evidence.evidence_record.session_id|ivr.inbound_session.id|V005|V008|FK|cross wave nullable
evidence.evidence_record.reservation_id|entitlement_metering.usage_reservation.id|V006|V008|FK|cross wave nullable
evidence.evidence_record.source_event_domain,evidence.evidence_record.source_event_id|evidence.evidence_event.event_domain,evidence.evidence_event.event_id|V008|V008|DEFERRABLE_FK|composite circular creation link; DEFERRABLE INITIALLY DEFERRED
evidence.evidence_artifact.evidence_id|evidence.evidence_record.id|V008|V008|FK|same wave
evidence.evidence_artifact.media_object_id|evidence.media_object.id|V008|V008|FK|same wave
evidence.evidence_event.evidence_id|evidence.evidence_record.id|V008|V008|DEFERRABLE_FK|opposite side of the evidence creation cycle; DEFERRABLE INITIALLY DEFERRED
sharing.evidence_share_grant.evidence_id|evidence.evidence_record.id|V008|V009|FK|cross wave
sharing.evidence_share_grant.created_by_account_id|customer_iam.customer_account.id|V002|V009|FK|cross wave
sharing.evidence_share_grant.owner_organization_id|organization_access.organization.id|V002|V009|FK|cross wave nullable
sharing.share_admission.share_grant_id|sharing.evidence_share_grant.id|V009|V009|FK|same wave
sharing.share_access_event.share_grant_id|sharing.evidence_share_grant.id|V009|V009|FK|same wave
sharing.share_access_event.share_admission_id|sharing.share_admission.id|V009|V009|FK|same wave nullable before admission
<!-- fk-matrix:end -->

### 5.2 Frozen candidate-key catalog (24 key sets)

The following block is the accepted key-set catalog. Every parent column set referenced by the FK catalog appears here as `PK` or `UNIQUE`. It is verbatim from the accepted plan.

<!-- key-catalog:start -->
customer_iam.customer_account|id|PK
organization_access.organization|id|PK
subject_number.business_subject|id|PK
subject_number.caller_number_binding|id|PK
catalog_order.catalog_product|id|PK
catalog_order.catalog_version|id|PK
catalog_order.catalog_offer|id|PK
catalog_order.customer_order|id|PK
catalog_order.payment_order|id|PK
operations_iam.employee_account|id|PK
ivr.inbound_session|id|PK
ivr.outbound_call_task|id|PK
ivr.call_record|call_id|UNIQUE
entitlement_metering.trial_policy_version|id|PK
entitlement_metering.entitlement_grant|id|PK
entitlement_metering.entitlement_balance|id|PK
entitlement_metering.usage_reservation|id|PK
activation_consent.consent_record|id|PK
activation_consent.service_activation|id|PK
evidence.media_object|id|PK
evidence.evidence_record|id|PK
evidence.evidence_event|event_domain,event_id|UNIQUE
sharing.evidence_share_grant|id|PK
sharing.share_admission|id|PK
<!-- key-catalog:end -->

### 5.3 S1 foreign-key detail

All S1 foreign keys are ordinary immediate `FK` constraints created in the child's wave. They are `ON UPDATE RESTRICT ON DELETE RESTRICT`. Parent rows exist in V002 before the V003 subject rows. No S1 FK is deferrable, late-added or intentionally absent.

| Child column | Exact parent column | Parent wave | FK wave | Mode | Nullability | S1 creation timing |
| --- | --- | --- | --- | --- | --- | --- |
| customer_iam.auth_identity.account_id | customer_iam.customer_account.id | V002 | V002 | FK | NOT NULL | with V002 table creation |
| customer_iam.identity_profile.account_id | customer_iam.customer_account.id | V002 | V002 | FK | NOT NULL | with V002 table creation |
| organization_access.organization_membership.organization_id | organization_access.organization.id | V002 | V002 | FK | NOT NULL | with V002 table creation |
| organization_access.organization_membership.account_id | customer_iam.customer_account.id | V002 | V002 | FK | NOT NULL | with V002 table creation |
| subject_number.business_subject.owner_account_id | customer_iam.customer_account.id | V002 | V003 | FK | NULL | with V003 table creation |
| subject_number.business_subject.owner_organization_id | organization_access.organization.id | V002 | V003 | FK | NULL | with V003 table creation |
| subject_number.caller_number_binding.subject_id | subject_number.business_subject.id | V003 | V003 | FK | NOT NULL | same-wave parent first |
| subject_number.subject_claim.subject_id | subject_number.business_subject.id | V003 | V003 | FK | NOT NULL | same-wave parent first |
| subject_number.subject_claim.account_id | customer_iam.customer_account.id | V002 | V003 | FK | NOT NULL | with V003 table creation |
| subject_number.subject_claim.organization_id | organization_access.organization.id | V002 | V003 | FK | NULL | with V003 table creation |
| subject_number.subject_risk_state.caller_number_binding_id | subject_number.caller_number_binding.id | V003 | V003 | FK | NOT NULL | same-wave parent first |

S1 has no `NO_FK` decision. The platform tables' `aggregate_id` and the audit table's `object_id` are polymorphic internal references documented outside the FK catalog and are not accepted FK decisions. No S1 foreign key permits a business number, membership, subject or employee row to substitute for a login identity.

## 6. Invariant Matrix

### 6.1 Frozen planned invariant catalog (40 invariants)

The following 40 invariant rows are verbatim from the accepted plan. Each has one owner, named collaborators, a primary enforcement class and a verified slice. S1 expands its own rows in section 6.2; S2/S3 rows remain frozen handoff and are not authored here.

| INV-01 | `customer-iam` | `organization-access`, `operations-iam`, `subject-number` | Login identity, business-number binding, organization membership and employee identity never substitute for one another | DB+SERVICE | Separate tables and schemas with no cross-assignment FK; service tests reject a phone, membership, claim or employee record used as login identity | S1 | Account/login and business-subject distinction |
| INV-02 | `customer-iam` | none | Effective provider identity is unique within provider scope and maps to one account | DB | Unique `(provider, provider_scope, provider_subject_hmac)`; one-account FK | S1 | Login identity correctness |
| INV-03 | `organization-access` | `customer-iam` | At most one non-revoked membership for an organization and account; membership grants no independent login | DB | Partial unique `(organization_id, account_id)` where non-revoked; membership has no auth credential column | S1 | Organization membership |
| INV-04 | `operations-iam` | none | Employee accounts use a separate issuer and subject; the last enabled `ADMIN` cannot be disabled or demoted | DB+TX | Unique `(auth_issuer, auth_subject_hmac)`; admin-row lock and enabled-admin count check in one transaction | S1 | Employee identity |
| INV-05 | `subject-number` | `customer-iam`, `organization-access` | A subject has at most one personal-account or organization owner; an IVR-created subject may be unowned until a verified claim, and an existing different owner is a conflict | DB+TX | Check `num_nonnulls(owner_account_id, owner_organization_id) <= 1`; owner FKs; claim transaction sets owner only when null | S1 | Business subject ownership |
| INV-06 | `subject-number` | none | One canonical number has at most one active business binding | DB | Partial unique `number_hmac` where `ACTIVE`; unique `(subject_id, number_hmac)` | S1 | Number binding and IVR lookup |
| INV-07 | `subject-number` | none | Claim appends a verified relation; open conflict or recycle risk is checked before replay or same-account duplicate handling | TX+SERVICE | Canonical-number lock; risk-state read; append-only claim; rejected attempts retained; service test | S1 | Cross-channel claim rules |
| INV-08 | `activation-consent` | `ivr`, `entitlement-metering` | Activation requires an explicit, current, immutable consent snapshot and a subject that is not `SUSPENDED`; no key `1` or explicit client confirmation means no activation, trial or reservation; replay/identity handling precedes every gate. A fresh `SUSPENDED` confirmation is a category-1 pre-activation refusal: stable `REJECT_SUSPENDED`, no accepted consent/transition/trial/grant/balance/reservation/success Outbox, and a `CONFIRMED` session is never moved to `REJECTED`. An `ACTIVE` activation with missing/expired current consent returns `GUIDE_ACTIVATION` with stable `CONSENT_REQUIRED` on initial resolve so the same confirmation path appends current consent without reissuing the trial; stale/invalid confirmation content is rejected with `CONSENT_REQUIRED` before accepted side effects. First-trial grant, balance and Outbox are written by `issueFirstTrial` in the activation transaction. After valid activation/trial, a category-2 normal domain no-dial result commits activation/trial with the session `CONFIRMED` and no claim/reservation, while a category-3 technical failure rolls back all writes including the `NEW` row and activation/trial; no failure category writes the other category's facts | DB+TX+SERVICE | Consent FK and snapshot fields; transaction validates consent version, action and current activation status under the subject lifecycle lock; `Propagation.MANDATORY` call to `entitlement-metering.issueFirstTrial`; suspended accept/reject, consent-gap, replay-before-suspend, no-CONFIRMED-to-REJECTED, category-1/2/3 outcome and no-side-effect tests | S2 | Consent/activation, fail-closed suspension, consent-gap recovery, deterministic replay, atomic first trial and unambiguous transaction outcomes |
| INV-09 | `activation-consent` | `ivr` | Activation has one row per subject; each transition owns one unique action event, one unique `ACTIVATION_STATUS` fact event, a request fingerprint and increments revision by exactly one | DB+TX | Unique `subject_id`; unique `(activation_id, to_revision)`, `(source_action_domain, source_action_event_id)` and `(status_event_domain, status_event_id)`; check `to_revision = from_revision + 1`; row lock | S2 | Activation history |
| INV-10 | `activation-consent` | `ivr`, `entitlement-metering` | `SUSPENDED` cannot self-resume through IVR or ordinary client confirmation; a suspended fresh confirmation is rejected with `REJECT_SUSPENDED` and leaves the activation suspended; an existing completed confirmation/decision is replayed before the suspension check and an already `CONFIRMED` session is never changed to `REJECTED`; an operations resume appends only `SUSPENDED → ACTIVE` without reissuing a first trial | TX | Read current state under the subject lifecycle lock and conditionally update only from `INACTIVE/ACTIVE`; reject a fresh suspended path before consent/transition/trial/reservation/Outbox; check existing decision/idempotency first; operations resume path is separate, requires its own event identity and never calls the first-trial service | S2 | Cross-channel lifecycle and stable replay |
| INV-11 | `ivr` | none | One canonical caller has at most one non-terminal task and at most one active or unknown call across all accounts and organizations; both fresh IVR decision paths exclude the current `callId` from the guard, so a call never blocks itself; a competing call receives stable `REJECT_CALL_IN_PROGRESS` with no claim/reservation and a non-active `call_record` parent (terminal `REJECTED` when newly created; an existing current `UNKNOWN` row is referenced unchanged and never finalized); the unique index is a backstop, not the protocol response, and a terminal reject touches only the current call's row | DB+TX | Partial unique `caller_number_hmac` on `outbound_call_task` for `PENDING/CLAIMED` and on `call_record` for `ACTIVE/UNKNOWN`; canonical-caller advisory lock and `call_id <> current_call_id` predicate on both resolve and direct paths; deterministic reread on index conflict | S2 | IVR single-slot task and call |
| INV-12 | `ivr` | none | Only a `PENDING` task whose `expires_at` is ahead of database time after lock acquisition can be claimed; the default pending TTL is 30 seconds, is server-configurable, is frozen into `ttl_seconds`/`expires_at` at creation, and a duplicate or retry never extends or refreshes it | DB+TX | State and timestamp checks; lock then `clock_timestamp()` recheck; conditional update | S2 | Configurable default 30-second TTL without duplicate refresh |
| INV-13 | `ivr` | `entitlement-metering` | On either task-backed entry path, a successful decision claims the task, creates or binds one active call record, freezes the decision, creates one reservation and commits all-or-nothing only after the active-call guard, current caller/subject/binding/owner-workspace access (organization `ACTIVE`, creator/owner account `ACTIVE`, membership `ACTIVE` with current role and resource access for an organization task), active activation/consent, `PENDING`/revision/unexpired task and positive effective entitlement checks pass; a direct IVR path with no task applies only the binding/subject/activation/consent/entitlement checks and no customer login/membership predicate | TX | One shared IVR transaction; the conditional claim includes `expires_at > clock_timestamp()` and the expected revision; a category-3 technical failure rolls back task, call record, reservation, decision and event side effects together, while an expected domain claim/reservation denial rolls back only to the same-connection attempt savepoint and freezes one no-dial decision; concurrency and reservation-competition tests | S2 | Atomic claim/call/reservation/authorization with explicit domain-versus-technical outcomes |
| INV-14 | `ivr` | none | Both public entries share one action-selection service; same `callId` and purpose with the same canonical fingerprint returns the first decision and frozen action; a changed fingerprint or conflicting immutable caller/access/node identity is a stable conflict; the `ACTIVATION_NEXT_ACTION` decision is a distinct purpose and never overwrites the original `INBOUND_RESOLVE` `GUIDE_ACTIVATION` decision, so a repeated resolve returns that original decision; a new confirmation event with an existing call/purpose decision either returns it on a fingerprint match or conflicts with no lingering `NEW` row; each call event is unique in the IVR event domain | DB+TX | Unique `(call_id, purpose)` with purposes `INBOUND_RESOLVE`, `DIRECT_AUTHORIZATION` and `ACTIVATION_NEXT_ACTION`, and `(event_domain, event_id)` on `call_event`; partial unique `(call_id, source_event_domain, source_event_id)`; canonical fingerprint comparison under the canonical-caller/call lock before side effects | S2 | Idempotent IVR protocol, shared selection and preserved first decision |
| INV-15 | `ivr` | `evidence` | Task, session, call, legs, reservation and evidence remain explicitly linked with named FK parents; call completion does not imply evidence solidification | DB | FK catalog in section 12; unique call/session/task and recording identities; separate status columns | S3 | IVR/evidence linkage |
| INV-16 | `entitlement-metering` | `activation-consent`, `platform-reliability` | Exactly one permanent FIRST-TRIAL grant exists per subject; claims, rule changes, re-entry, a new campaign key or a cross-channel import do not reissue it; the granting action domain/event ID/fingerprint are persisted, same action plus same fingerprint replays the original ids, same action plus changed fingerprint conflicts, and another action for an already-granted subject returns `ALREADY_GRANTED` original ids without a second grant or balance | DB+TX | `trial_grant.campaign_key` check `FIRST-TRIAL`; unique `subject_id`; unique `(source_action_domain, source_action_event_id)`; subject-key lock; immutable grant | S2 | Trial uniqueness and provenance |
| INV-17 | `entitlement-metering` | none | Entitlement grants are immutable and unique by trial campaign or `(order, installment, entitlement type)` | DB | Unique constraints and source-exclusivity checks; append-only grant rows | S2 | Entitlement issuance |
| INV-18 | `entitlement-metering` | none | Reserved plus consumed units never exceed granted units; balance updates lock the balance row and use conditional revision | DB+TX | Check; balance-row lock; revision conditional update | S2 | Reservation/usage concurrency |
| INV-19 | `entitlement-metering` | `ivr` | One reservation per call, one terminal transition and one settlement fact; a task claim never commits without its reservation and a reservation never authorizes more than the committed available units; unused units release once; late or duplicate events do not recharge; a normal domain reservation denial after a tentative claim rolls back to the same-connection attempt savepoint and commits one no-dial decision, while a technical failure rolls back the whole transaction | DB+TX+SERVICE | Unique `call_id`; state and unit checks; reservation and claim in one transaction with a same-connection savepoint before the tentative claim; conditional terminal update; unique usage fact; settlement/reconciliation and forced-failure tests | S2 | IVR usage settlement with explicit domain-versus-technical denial |
| INV-20 | `catalog-order` | none | One immutable product identity; at most one active version per product; published version and benefit lines are immutable; type, billing and grant matrix is checked server-side | DB+TX+SERVICE | Unique `product_code`; partial unique active version per product; named checks; immutable published columns; service test | S2 | Catalog versioning |
| INV-21 | `catalog-order` | `activation-consent`, `customer-iam`, `subject-number`, `platform-reliability` | Orders only come from `MINIAPP/WEB`, require account/workspace/subject access, the ordinary-client real-name condition and current active service activation, carry an immutable exact catalog snapshot, purchase consent and server-derived amount, and cannot overwrite history after offsale; creation is bound to platform scope `PURCHASE` with a request fingerprint and the first immutable order result so identical retry returns the first order and changed input conflicts | DB+TX+SERVICE | Unique `(account_id, idempotency_key)`; `request_fingerprint`; platform `PURCHASE` begin/complete in the same transaction; immutable snapshot/fingerprint; service revalidates account/workspace/subject access, ordinary-client real-name, current activation, server amount and version token | S2 | Purchase, idempotent order creation and historical snapshots |
| INV-22 | `catalog-order` | none | Payment events are unique by provider and event; only verified events advance payment; `UNKNOWN` blocks duplicate checkout and is resolved by querying the original order; `PAID` and entitlement issuance remain independently observable | DB+TX | Unique `(provider, provider_event_id)`; signature-verification flag; order row lock and conditional payment update; service blocks a second checkout while `UNKNOWN` | S2 | Payment truth |
| INV-23 | `entitlement-metering` | `catalog-order` | Issuance is idempotent by order, installment and entitlement type, consumes the frozen order snapshot, not current catalog state, and writes grant, balance and Outbox in one transaction | DB+TX+SERVICE | Unique `(order_id, installment_no, entitlement_type)`; snapshot-equality test; same-transaction grant/balance/Outbox | S2 | Entitlement delivery |
| INV-24 | `evidence` | none | Media identity, object version and hash are immutable; bytes stay in object storage; client-declared hash or duration never wins over server validation | DB+SERVICE | Immutable metadata columns; object uniqueness; server rehash and duration validation test | S3 | Evidence/media metadata |
| INV-25 | `evidence` | `subject-number`, `ivr` | Exactly one `EVIDENCE_CREATED` event owns each evidence record; the event's `(event_domain, event_id)` equals the record's `(source_event_domain, source_event_id)`; evidence keeps captured subject, session, identity-at-capture and consent version through later claims or account changes; record and creation event insert atomically | DB+TX | Unique `(source_event_domain, source_event_id)`; unique `(event_domain, event_id)`; partial unique `(evidence_id)` for `EVIDENCE_CREATED`; composite FK with both cycle sides `DEFERRABLE INITIALLY DEFERRED`; deferred constraint trigger validates exactly one identity at commit; immutable capture columns | S3 | Reciprocal evidence ownership |
| INV-26 | `sharing` | none | Share content scope is frozen; successful admissions are counted once; revoke, rotate or expiry changes generation and blocks new or renewed access | DB+TX | Immutable content snapshot; unique admission key/number; grant row lock; status and generation recheck | S3 | Sharing authorization |
| INV-27 | `platform-reliability` | all domain owners | Idempotency is scoped by action domain; `begin` returns `NEW`, `REPLAY` or `CONFLICT`; only `NEW` may execute side effects; `platform.idempotency_complete` finalizes the first result exactly once in the same transaction; same key with changed input is rejected; a replay after committed completion returns the stored first result; an unfinalized committed `NEW` row is an alertable contract violation; for `IVR_CONFIRM`, call/purpose-level existing-decision handling precedes side effects and suspension, a matching semantic fingerprint with a new event ID reuses the existing decision, a changed fingerprint rolls back the `NEW` row and returns a stable conflict, and an event reused on another call conflicts because `callId` is in the fingerprint | DB+TX+SERVICE | Unique `(scope, idempotency_key)`; `status/result/completed_at/revision` checks; conditional write-once completion; canonical semantic fingerprint comparison; event/call-purpose precedence tests; begin/complete function contracts and concurrency tests | S1 | Cross-channel idempotency |
| INV-28 | `platform-reliability` | all domain owners | Outbox insertion shares the domain transaction, has a stable domain-scoped dedupe key, is appended through `platform.outbox_append` and is delivered at least once | DB+TX+SERVICE | Unique `(event_domain, dedupe_key)`; in-transaction function; dispatcher and consumer tests | S1 | Reliable asynchronous facts |
| INV-29 | `operations-audit` | none | Audit facts are append-only and cannot be updated or deleted through ordinary administration; the only write path is `audit.append_fact`, owned by `operations-audit` with `search_path = audit, pg_temp`, appending `audit.audit_fact` | DB | Revoked table privileges; append-only trigger; restricted `audit.append_fact`; privilege test | S1 | Append-only audit |
| INV-30 | `platform-reliability` | all domain owners | Business facts are retired by status or terminal time, not hard delete; FKs restrict deletion; no cross-module cascade; only unreferenced never-published catalog version drafts are a future deletion candidate | DB | `ON DELETE RESTRICT`; no delete grants; lifecycle classification in section 8.2; final closure in S3 | S1 | Deletion policy |
| INV-31 | `platform-reliability` | all domain owners | Full phone, WeChat subject, identity number, callee, share code and object URL are not public keys and are not stored or logged as plaintext secrets; the full IVR callee exists only in the encrypted internal action payload — under `task.calleeNumber` for an authenticated `DIAL_TASK_CALLEE` response or under `authorization.calleeNumber` for a successful `DIRECT_AUTHORIZATION` (`action` null) — while the typed snapshot, audit, logs and customer responses carry only the masked value; a direct success never places the callee in task fields | DB+SERVICE | Column representation contract in section 8.1; action-snapshot nullability/redaction tests; log and response checks; replay-decryption test; final closure in S3 | S1 | Sensitive-field handling and callee boundary |
| INV-32 | `ivr` | none | Task create, replace and cancel is idempotent and workspace/creator-scoped; an identical `PENDING` duplicate returns the existing task without TTL refresh; stale `PENDING` expires under the caller lock; a `CLAIMED` task cannot be replaced or cancelled | DB+TX | Unique `(created_by_account_id, idempotency_key)`; fingerprint and identical-tuple checks; canonical-caller lock; workspace/creator checks; conditional state update | S2 | IVR task lifecycle |
| INV-33 | `subject-number` | `customer-iam` | Subject real-name status is independent of login, claim, activation and trial; claim never writes it | DB+SERVICE | `business_subject.real_name_status`; claim columns cannot update it; service test | S1 | Real-name and subject distinction |
| INV-34 | `ivr` | `entitlement-metering` | Direct DTMF authorization takes the canonical-caller lock, requires the frozen `ENTER_CALLEE` no-task path, rejects a task ID and any `PENDING/CLAIMED` task after a positive no-task check, rechecks no other `ACTIVE/UNKNOWN` call excluding the current `callId`, reuses the shared identity/subject/activation/consent/entitlement predicate order without requiring a customer login, membership or real-name, creates or binds the active call guard, creates one reservation, and freezes a success with `action` null plus the committed reservation fields (never an `ENTER_CALLEE` instruction after the callee was confirmed); a direct refusal uses an existing non-dial action with no reservation; retry returns the first result and changed confirmation content conflicts | DB+TX | Unique `(call_id, purpose)` and partial unique `(call_id, source_event_domain, source_event_id)`; canonical fingerprint comparison; caller lock; active-call partial unique; no-task check; reservation uniqueness; purpose/action/nullability test; concurrency tests | S2 | Direct IVR path within the seven-action contract |
| INV-35 | `platform-reliability` | all domain owners | Event and action identity is unique within its actual domain; domain tables persist the domain and event identity; the same string may exist in different domains, but the same `IVR_CONFIRM` event ID reused for another call conflicts because the semantic fingerprint includes `callId`; changed confirmation content for the same event ID also conflicts | DB+SERVICE | Composite unique constraints on `call_event`, `ivr_decision`, `usage_fact`, `consent_record`, `evidence_event`, `share_access_event`; platform `IVR_CONFIRM` fingerprint/key semantics; event-domain registry and service tests | S2 | Idempotency and event identity |
| INV-36 | `entitlement-metering` | `catalog-order` | Every order, installment and entitlement type has one durable issuance row whose `PENDING/FAILED/ISSUED` state, attempt count, revision, error code and grant link are owned by `entitlement-metering`; partial line failure is observable; no module directly writes another module's issuance or order status | DB+TX+SERVICE | Unique issuance key; state checks; conditional revision; multi-line and partial-failure tests; privilege test | S2 | Durable entitlement issuance state |
| INV-37 | `activation-consent` | `ivr`, `platform-reliability` | Activation confirmation replay is bound to the canonical semantic fingerprint and first immutable result through `platform.idempotency_begin`/`platform.idempotency_complete`; the wrapper authenticates and validates immutable call/session/caller identity, then handles an existing `(call_id, ACTIVATION_NEXT_ACTION)` decision and the event-level `REPLAY`/`CONFLICT` before any `SUSPENDED` check or consent/trial/claim/reservation effect; a new event ID with the same fingerprint returns the existing decision and frozen action and completes that event's row with the existing decision ID, while changed input returns a stable conflict with the `NEW` row rolled back; a `CONFIRMED` session is never moved to `REJECTED`; a fresh confirmation requires the original resolve action `GUIDE_ACTIVATION`; the completed `result_id`/`result_type` is the `ACTIVATION_NEXT_ACTION` decision ID for every accepted branch, including already `ACTIVE` and call-rejected outcomes, and the rejected decision ID for a suspended fresh rejection; a category-1 rejection commits no accepted activation side effect, a category-2 domain no-dial result commits valid activation/trial with its no-dial decision, and a category-3 technical failure commits neither the decision nor any other write; every transition, including `OPERATIONS`, has a non-null action event identity | DB+TX | Platform idempotency scope `IVR_CONFIRM` with event key and canonical semantic fingerprint; write-once completion; `request_fingerprint` NOT NULL; `ivr_decision` unique `(call_id, purpose)`; `source_action_event_id` NOT NULL and unique per domain; replay/conflict/cross-call/first-result identity and category-outcome tests | S2 | Deterministic activation replay, preserved session state and a stable result per branch |
| INV-38 | `entitlement-metering` | `ivr` | A reservation whose end event is missing, late or duplicated is reconciled against authoritative IVR usage; known use is settled first, the remainder is released once; unknown use remains occupied and blocks a new reservation and is never released | DB+TX+SERVICE | `RECONCILING` state; reconciliation fields; caller lock; conditional terminal update; missing/late/duplicate tests | S2 | Safe reservation recovery |
| INV-39 | `sharing` | `platform-reliability` | The durable admission is the authorization fact; the Redis playback ticket is derived and reconstructible; same `admission_key` retry returns the admission and reissues the ticket without incrementing; Redis loss cannot change admission count | DB+TX+SERVICE | Unique admission key; derived ticket key; access events for issue/failure; Redis-failure and lost-cache tests | S3 | Count-once sharing across Redis failure |
| INV-40 | `platform-reliability` | `operations-audit`, `entitlement-metering`, all domain owners | Cross-schema idempotency begin/complete, Outbox, `audit.append_fact` and first-trial functions are `SECURITY DEFINER`, owned by the owning module, have a fixed `search_path`, named EXECUTE grants, no transaction control, exception-propagating rollback, first-result replay/finalize-once and redacted/encrypted payloads; the four platform/audit functions are created and verified in V001/S1 and `issue_first_trial` is created and verified only in V006/S2 after its tables exist; the shared IVR action-selection contract is an IVR-owned application service inside the local transaction, not an additional SQL function | DB+TX+SERVICE | Function signatures, creation waves and grants in sections 11-12; S1 privilege/atomicity/replay/redaction/`search_path` tests for the four platform/audit functions; S2 tests for `issue_first_trial` and for the shared application service | S1 for the four platform/audit functions; S2 for `issue_first_trial` and the shared service | Safe DB-enforced ownership without a new function surface |

### 6.2 S1 invariant enforcement expansion

This table makes the S1 portions generation-ready. "Location" names the exact constraint, index, transaction rule or function behavior. "Real behavior assertion" is a document-level requirement for the later executable test suite; no PostgreSQL test was run in S1.

| Invariant (S1 portion) | Owner | Primary class | Exact S1 location | Real behavior assertion and evidence requirement |
| --- | --- | --- | --- | --- |
| INV-01 (S1) | customer-iam | DB+SERVICE | Five separate tables/schemas; `fk_auth_identity_account` only links identity to account; no FK accepts a binding, membership, subject or employee row; login/workspace resolution reads only `auth_identity`, `customer_account`, `organization_membership` and `organization.status` through the exact section 10.8 grants | A request that presents a binding HMAC, membership ID, business subject ID or employee ID as a login subject is rejected; no database FK can convert one identity class into another; a disabled account or identity cannot authenticate; a suspended/disabled organization or non-active membership is excluded from the usable workspace list; the workspace listing takes no lifecycle advisory lock and is not an authorization lease, so every protected request rechecks current authority; membership alone cannot create a customer session |
| INV-02 (S1) | customer-iam | DB | `uq_auth_identity_subject (provider, provider_scope, provider_subject_hmac)`, `fk_auth_identity_account`, `customer_iam.guard_auth_identity`; identity bind and account disable take the same account lifecycle advisory lock and account row lock | Two concurrent inserts of the same provider tuple produce one row; the loser reads the row and returns the same account for an identical bind or `IDENTITY_CONFLICT` for a different account; a direct update that tries to change `account_id`, `provider`, `provider_scope` or the provider-subject tuple is rejected by the guard; bind-versus-disable has two serial orders (disable first rejects the bind, bind first retains the identity but login remains account-state gated); one identity never maps to two accounts |
| INV-03 (S1) | organization-access | DB | `uq_membership_live (organization_id, account_id) WHERE status <> 'REVOKED'`; no credential column | Concurrent non-revoked membership inserts produce one live row; a revoked row permits a new membership row; a membership ID cannot authenticate and a role is never accepted from the client |
| INV-04 (S1) | operations-iam | DB+TX | `uq_employee_login_hmac`, `uq_employee_issuer_subject`, `ix_employee_enabled_admin`; transaction advisory lock `operations_iam.admin_mutation` + enabled-admin count | Two concurrent disable/demote actions serialize; the action that would leave zero enabled `ADMIN` rows fails with a stable error and changes nothing; employee issuer/subject is separate from customer login and business-number identity |
| INV-05 (S1) | subject-number | DB+TX | `ck_subject_single_owner`; owner FKs; `subject_number.guard_subject_owner`; conditional owner update in section 9.6; creation grants omit both owner INSERT columns and the `owner_organization_id` UPDATE column | Every S1 creation source, including `OPERATIONS`, starts with both owner columns null; a subject can remain unowned; a verified personal claim sets `owner_account_id` only from null, in the same transaction or later; an existing owner is never cleared or changed; a different owner yields `CONFLICT` with no owner change; a fresh organization-scoped claim is refused without owner change because enterprise ownership policy is deferred; no S1 runtime role creates or transfers an enterprise owner; personal and organization ownership are mutually exclusive |
| INV-06 (S1) | subject-number | DB | `uq_binding_active_number (number_hmac) WHERE status = 'ACTIVE'`; `uq_binding_subject_number (subject_id, number_hmac)` | Concurrent active binding of one canonical number to two subjects yields one winner and one stable conflict; retired history remains; binding a number never creates a login identity |
| INV-07 (S1) | subject-number | TX+SERVICE | Canonical-number advisory lock; subject lifecycle advisory and subject row lock; membership-pair advisory lock; existence validation of subject/account/organization before any claim row; risk read on the current binding before replay/duplicate success; append-only claim insert; `uq_claim_request`; `uq_claim_accepted_subject_account`; decrypted `verification_ref_ciphertext` logical comparison; whole-transaction bounded retry on a request-ID unique loser | An `OPEN` risk blocks a claim even when the same `request_id` was previously accepted; risk evaluation precedes replay and same-account duplicate handling; a request replay must match stored subject/account, null-safe organization, method, binding/number and proof identity or returns `CLAIM_REPLAY_CONFLICT` with no stored-success disclosure; a same-account accepted claim for a different organization, method or binding is `CLAIM_CONTEXT_CONFLICT`, never a reuse; unknown subject/account/organization IDs return the stable `*_NOT_FOUND` code with no claim row and never a raw FK error, while an existing inactive parent with valid FKs and proof may append one `REJECTED` row; a blocked new request is retained as exactly one `REJECTED` row, while a repeated blocked request returns the same blocked outcome without mutating an existing claim; an owner conflict is retained as `CONFLICT`; accepted claims inconsistent with the current/null owner return `CLAIM_DATA_INTEGRITY` with no repair; a request-ID unique loser rolls back the whole transaction and retries from the start, and exhaustion returns `CLAIM_CONCURRENCY_EXHAUSTED`; `verified_at` is the accepted decision time at or after `risk_checked_at`; a fresh enterprise claim refuses with `ORGANIZATION_OWNERSHIP_UNAPPROVED` and no owner change |
| INV-27 (S1 platform portion) | platform-reliability | DB+TX+SERVICE | `uq_idempotency_scope_key`; state-shape check; write-once completion; `platform.idempotency_begin`/`complete`; `ix_idempotency_new_alert` | Same key/same fingerprint returns `REPLAY` with the first result; same key/changed fingerprint returns `CONFLICT` with no side effect; only `NEW` executes the side effect; `complete` succeeds once, the same hash returns `ALREADY_COMPLETED`, a different hash/revision fails; a forced failure after `begin` leaves no row; a committed `NEW` row is detected by the alert query; IVR call/purpose precedence is an S2 caller obligation, not reimplemented in the platform function |
| INV-28 (S1) | platform-reliability | DB+TX+SERVICE | `uq_outbox_domain_dedupe`; `payload_sha256`; immutable payload columns; `platform.outbox_append`; partial pending/failed indexes | Outbox insert appears only if the domain transaction commits; a rolled-back domain leaves no Outbox row; same domain/dedupe key and same hash returns the existing id; different hash raises `OUTBOX_DEDUPE_CONFLICT`; the dispatcher cannot alter payload identity or bytes |
| INV-29 (S1) | operations-audit | DB | Revoked table privileges; `trg_audit_fact_immutable`; `audit.append_fact`; no update/delete function | A domain role cannot insert, update, delete or truncate `audit.audit_fact`; `audit.append_fact` appends one row; update and delete attempts fail; `PUBLIC` cannot execute the function; a failed caller transaction leaves no audit row |
| INV-30 (S1) | platform-reliability | DB | `ON DELETE RESTRICT` on all S1 FKs; no delete grants; status retirement; append-only controls | Deleting a parent with children fails; deleting an S1 row through a runtime role fails; no cascade exists; retiring an account, organization, membership, binding or employee uses status and preserves history |
| INV-31 (S1) | platform-reliability | DB+SERVICE | Enveloped ciphertext, HMAC and masked columns in sections 4.5-4.15; function denylist in section 10.7; snapshot/result hashes | A plaintext protected key in an idempotency result, Outbox payload or audit snapshot is rejected; ciphertext and HMAC are accepted; HMAC is never returned as the underlying value; masked values contain no full number; identity, claim and employee sensitive fields are never logged or exposed |
| INV-33 (S1) | subject-number | DB+SERVICE | `business_subject.real_name_status`/`real_name_verified_at`; column-level grants: `subject_number_role` has no update on those columns, `subject_identity_verification_role` has `SELECT` on `id`, status, revision and `updated_at` plus update only on the real-name/revision/time columns; `subject_number.guard_subject_owner` rejects a real-name change by any other current role; claim writes only owner/source/revision/timestamps | A claim transaction inserts only `subject_claim` and updates owner columns; it cannot set subject real-name; the verification role can locate the subject by `id` and is the only writer of real-name columns; login, claim, binding, activation and trial flows leave subject real-name unchanged |
| INV-40 (S1 portion) | platform-reliability | DB+TX+SERVICE | Four V001 function definitions; fixed `search_path`; owner and grant matrix in section 10; no transaction control; V001 does not create `issue_first_trial` | Each S1 function's owner is NOLOGIN/NOSUPERUSER; `PUBLIC` has no EXECUTE; `search_path` shadowing cannot redirect an unqualified reference; a forced failure rolls back domain and function writes together; protected plaintext is rejected; `issue_first_trial` is absent from V001 and its dependency tables |

S1 has no later-slice prerequisite. The S1 invariants use only V001-V003 tables and functions. S2/S3 invariants are not claimed as verified by this document.

## 7. State Machines

### 7.1 S1 state machines

These are the only state machines authored in S1. S2/S3 state machines are reserved in section 7.3.

| Aggregate | Allowed states | Allowed transitions and guards | Owner |
| --- | --- | --- | --- |
| Customer account | `ACTIVE`, `DISABLED` | `ACTIVE → DISABLED` on account retirement or security action; `DISABLED → ACTIVE` only through an operations action with reason and audit; a disabled account cannot be selected by login or workspace resolution; revision must match | customer-iam |
| Customer login identity | `ACTIVE`, `DISABLED` | `ACTIVE → DISABLED` on revocation; `DISABLED → ACTIVE` only after re-verification and with revision; the provider/subject tuple and `account_id` are immutable under `customer_iam.guard_auth_identity`; bind and account disable serialize on the same account lifecycle advisory lock and row lock; an identity never moves to another account | customer-iam |
| Account real-name profile | `UNVERIFIED`, `VERIFIED` | `UNVERIFIED → VERIFIED` after the approved identity-verification flow; `VERIFIED → VERIFIED` re-verification with revision; `VERIFIED → UNVERIFIED` only by an approved reset that clears protected fields in the same update | customer-iam |
| Organization | `ACTIVE`, `SUSPENDED`, `DISABLED` | `ACTIVE → SUSPENDED`, `SUSPENDED → ACTIVE`, `ACTIVE`/`SUSPENDED → DISABLED`; `DISABLED` is terminal for everyday operations; revision and audit required; `SUSPENDED`/`DISABLED` remove workspace usability | organization-access |
| Organization membership | `ACTIVE`, `SUSPENDED`, `REVOKED` | `ACTIVE ↔ SUSPENDED`; `ACTIVE`/`SUSPENDED → REVOKED`; `REVOKED` is terminal for the row; a new row may be inserted after revocation; role/department changes use revision and audit | organization-access |
| Employee account | `ACTIVE`, `DISABLED` | `ACTIVE → DISABLED` with the last-enabled-admin guard; `DISABLED → ACTIVE` only through an operations action with reason and audit; role `ADMIN`/`OPS` changes use revision and the same guard | operations-iam |
| Subject ownership | `(owner null, owner null)`, `(account, null)`, `(null, organization)` | Only null → personal account through an accepted personal claim; a fresh organization-scoped claim is fail-closed until the deferred enterprise ownership policy approves it; no transfer; a different owner is a conflict; `subject_number.guard_subject_owner` rejects clearing or changing a set owner; real-name is a separate dimension | subject-number |
| Caller-number binding | `ACTIVE`, `DISABLED`, `RETIRED` | `ACTIVE ↔ DISABLED`; `ACTIVE`/`DISABLED → RETIRED`; `RETIRED` is terminal; replacement acquires the old and new canonical-number locks in sorted order, creates a new row and retires the old row in one transaction; an active number is unique; a same-number replacement is a stable conflict | subject-number |
| Subject claim | `ACCEPTED`, `REJECTED`, `CONFLICT` | Insert-only outcome; no transition; status is chosen before the single insert; replay/duplicate returns an accepted outcome only after the stored subject/account/organization/method/binding/number/proof identity matches and current risk/lifecycle/access predicates pass; no update or delete | subject-number |
| Subject risk state | `OPEN`, `CLEARED` | `OPEN → CLEARED` and `CLEARED → OPEN` through approved claim-risk/operations flows with revision and audit; `OPEN` blocks claims and historical-evidence access, not outbound calling by itself | subject-number |
| Idempotency record | `NEW`, `COMPLETED` | `NEW → COMPLETED` exactly once in the same transaction as the side effect; `REPLAY`/`CONFLICT` are outcomes, not stored states; a committed `NEW` is an alert | platform-reliability |
| Outbox event | `PENDING`, `PUBLISHED`, `FAILED` | `PENDING → PUBLISHED`; `PENDING → FAILED`; `FAILED → PENDING` for a bounded retry; `PUBLISHED` is terminal; only dispatcher columns and revision change | platform-reliability |

### 7.2 S1 transition and terminal rules

- Every mutable transition is a conditional update with expected `revision`; a zero-row update performs one bounded reread and returns the current state or a stable conflict.
- `DISABLED` on customer account, identity and employee is a retirement state for the baseline; only an approved operations action can re-enable. `REVOKED` membership and `RETIRED` binding are terminal for their row. `PUBLISHED` Outbox and `COMPLETED` idempotency are terminal.
- A subject owner is never transferred. A subject can remain unowned and unverified indefinitely; this is a valid IVR-created state, not an incomplete-row error.
- Append-only tables (`audit.audit_fact`, `subject_number.subject_claim`) have no state transition. Their status/outcome is fixed at insertion and their rows are immutable.
- The state machine never treats a business number, membership, subject or employee row as a login state. Login eligibility is only an active identity plus an active account.

### 7.3 Reserved S2/S3 state machines

The accepted plan's S2/S3 state machines for activation, activation confirmation, IVR task creation/replacement/cancellation/claim, inbound session, direct DTMF authorization, call record, trial/entitlement/issuance/reservation/usage, catalog/order/payment, evidence and sharing are not authored in S1. Their frozen handoff remains the accepted plan. S2/S3 authors must consume the S1 platform/identity/subject contracts and must not treat this section as a substitute for their own detailed dictionaries.

## 8. Sensitive-Field Inventory

### 8.1 Frozen planned sensitive-field inventory (28 rows)

The following block is verbatim from the accepted plan. It covers all 44 tables, including S2/S3 rows that are not yet authored. The frozen inventory does not claim that S2/S3 column dictionaries are complete.

<!-- sensitive:start -->
| Column(s) | Representation | Prohibited exposure |
| --- | --- | --- |
| `customer_iam.auth_identity.provider_subject_ciphertext` / `provider_subject_hmac` | Ciphertext plus independent HMAC lookup | Never returned; HMAC is not a public identifier |
| `customer_iam.identity_profile.legal_name_ciphertext` / `id_number_ciphertext` / `id_number_hmac` / `verification_ref_ciphertext` | Ciphertext, HMAC, masked display only | No plaintext in API, logs, audit snapshots or prototype fixtures |
| `operations_iam.employee_account.login_name_hmac` / `auth_subject_hmac` | HMAC plus masked login name | Credentials and employee session material stay outside business tables |
| `subject_number.caller_number_binding.number_ciphertext` / `number_hmac` / `number_masked` | Ciphertext, HMAC, masked | HMAC used for uniqueness/lookup, never returned as the number |
| `subject_number.subject_claim.verification_ref_ciphertext` | Ciphertext | Verification evidence is not a public claim identifier |
| `ivr.outbound_call_task.caller_number_hmac` / `callee_ciphertext` / `callee_masked` | HMAC, ciphertext, masked | IVR-only callee plaintext; never in ordinary logs or client responses |
| `ivr.inbound_session.caller_ciphertext` / `caller_number_hmac` / `caller_masked` / `access_number_masked` | Ciphertext, HMAC, masked | Access number and caller are not public keys |
| `ivr.ivr_decision.response_snapshot` / `response_payload_ciphertext` | Typed redacted snapshot plus encrypted internal payload; the full task callee exists only under `task.calleeNumber` for `DIAL_TASK_CALLEE`, and the full confirmed callee only under `authorization.calleeNumber` for a successful `DIRECT_AUTHORIZATION` (`action` null) | Authenticated IVR internal response only; snapshot, logs, audit and customer responses use the masked callee; no permanent media or callee leakage; replay decrypts the same immutable payload; direct success never populates task fields |
| `ivr.call_record.caller_ciphertext` / `caller_masked` / `callee_ciphertext` / `callee_masked` / `access_number_masked` | Ciphertext and masked; caller HMAC for lookup | Call parties are never public identifiers |
| `ivr.call_event.payload_ciphertext` / `payload_redacted` | Encrypted raw payload plus redacted view and SHA-256 | Raw event payload never logged or returned |
| `catalog_order.payment_event.payload_ciphertext` | Ciphertext plus SHA-256 | Provider payload is never returned or logged in plaintext |
| `catalog_order.payment_order.merchant_order_no` / `provider_transaction_id`; `payment_event.provider_event_id` / `transaction_id` | Provider-scoped internal identifiers | Never returned to clients as public keys; logs restricted and access audited |
| `catalog_order.catalog_version.snapshot` / `snapshot_sha256` | Canonical commercial snapshot and hash | No protected value unless encrypted or redacted before hashing |
| `catalog_order.customer_order.snapshot` / `snapshot_sha256` | Canonical order snapshot and hash | Historical commercial facts only; protected values encrypted or redacted |
| `entitlement_metering.trial_policy_version.snapshot` / `snapshot_sha256` | Canonical policy snapshot and hash | Policy facts only; no protected value |
| `entitlement_metering.entitlement_grant.snapshot` / `snapshot_sha256` | Canonical grant snapshot and hash | Historical grant facts only; no protected value |
| `entitlement_metering.usage_fact.snapshot` / `snapshot_sha256` | Canonical usage snapshot and hash | Usage metadata only; no raw media or protected value |
| `activation_consent.consent_record.snapshot` / `snapshot_sha256` | Canonical consent snapshot and hash | No plaintext protected value; prompt and policy references only |
| `evidence.media_object.bucket` / `object_key` / `object_version_id` / `crypto_key_ref` | Internal storage reference | Never exposed in public URLs or client responses |
| `evidence.recording.declared_sha256` / `validated_sha256` | Hash only | Declared hash never overrides server validation |
| `evidence.evidence_record.identity_at_capture` / `consent_version` / `snapshot` | Immutable status/version reference and canonical snapshot; no raw PII | Captured identity is a reference, not a live identity or raw number |
| `evidence.evidence_event.payload_redacted` / `payload_sha256` | Redacted metadata and hash | Never contains media bytes or raw party identifiers |
| `sharing.evidence_share_grant.capability_token_hash` / `code_hash` / `code_salt` | One-way digest and salt | No plaintext capability token or share code stored |
| `sharing.evidence_share_grant.content_snapshot` / `content_sha256` | Internal content-scope snapshot and hash | Never expose object keys, internal evidence IDs or unshared evidence |
| `sharing.share_admission.admission_key` | Opaque idempotency key or digest | Never expose raw verification input or contact data |
| `sharing.share_access_event.risk_digest` / `payload_redacted` | Hash and redacted metadata | No raw IP, phone or full URL in ordinary audit output |
| `audit.audit_fact.before_snapshot` / `after_snapshot` | Redacted or encrypted snapshot | No plaintext secret or raw number |
| `platform.outbox_event.payload_redacted` / `payload_ciphertext` / `platform.idempotency_record.result_snapshot` | Redacted or encrypted domain payload | No secret or raw number; consumers receive only authorized fields |
<!-- sensitive:end -->

### 8.2 S1 sensitive-field detail

| Table.column(s) | Representation and key reference | Prohibited exposure | S1 control |
| --- | --- | --- | --- |
| customer_iam.customer_account | No sensitive value | Display name is not a login name, phone or public key | No sensitive column |
| customer_iam.auth_identity.provider_subject_ciphertext | Versioned ciphertext envelope with embedded `key_ref`; explicit `key_version` | Never returned, logged or used as a public key | Envelope CHECK; HMAC lookup; no response projection |
| customer_iam.auth_identity.provider_subject_hmac | HMAC-SHA-256, 64 hex | Never returned; not a number | Unique lookup key only |
| customer_iam.identity_profile.legal_name_ciphertext | Versioned ciphertext envelope | No plaintext in API, logs, audit or fixtures | Envelope CHECK; verification flow only |
| customer_iam.identity_profile.id_number_ciphertext | Versioned ciphertext envelope | No plaintext or reversible display | Envelope CHECK |
| customer_iam.identity_profile.id_number_hmac | HMAC-SHA-256, 64 hex | Never returned as the identity number | Lookup only |
| customer_iam.identity_profile.verification_ref_ciphertext | Versioned ciphertext envelope | Verification evidence is not a public identifier | Envelope CHECK |
| organization_access.organization, organization_access.organization_membership | No sensitive identifier column; department label is display-only | Organization name and department do not become login or subject keys | No sensitive column |
| operations_iam.employee_account.login_name_hmac | HMAC-SHA-256, 64 hex | Credentials and session material stay outside business tables | Unique lookup only |
| operations_iam.employee_account.auth_subject_hmac | HMAC-SHA-256, 64 hex | Never returned as an issuer subject | Unique lookup only |
| operations_iam.employee_account.login_name_masked | Masked display value | Never the full login name in logs or responses | Display only |
| subject_number.business_subject | No number or identity value | Owner/real-name state is not a login or evidence key | No sensitive column |
| subject_number.caller_number_binding.number_ciphertext | Versioned ciphertext envelope | Never a public key or log field | Envelope CHECK |
| subject_number.caller_number_binding.number_hmac | HMAC-SHA-256, 64 hex | Never returned as the number | Uniqueness and lookup only |
| subject_number.caller_number_binding.number_masked | Masked display value | No full number in client responses or logs | Display only |
| subject_number.subject_claim.verification_ref_ciphertext | Versioned ciphertext envelope carrying the immutable binding/number/proof replay identity; decrypted only server-side | Verification evidence is not a public claim identifier | Envelope CHECK; append-only row; logical-value replay comparison |
| subject_number.subject_claim.request_id | Opaque internal request identity | Not a customer-facing identifier; no contact data | Unique internal key only |
| subject_number.subject_risk_state | No raw number or evidence content | Risk type/state are internal facts | FK to binding; no raw value |
| platform.idempotency_record.result_snapshot | Redacted or encrypted typed result; SHA-256 of redacted form | No full number, provider subject, callee, share code or secret | Function denylist; completion hash; immutable after finalize |
| platform.outbox_event.payload_redacted / payload_ciphertext | Redacted JSON or encrypted envelope; SHA-256 | No protected plaintext; consumers receive only authorized fields | Function denylist; immutable payload columns |
| audit.audit_fact.before_snapshot / after_snapshot | Redacted or encrypted snapshots | No plaintext secret, raw number or identity value | Function denylist; append-only table |

### 8.3 Prohibited public keys and response fields

The following are never primary keys, unique public identifiers, URL path segments, customer response fields or ordinary log fields: full phone or landline number, WeChat `openid`/`unionid`/subject, identity number, legal name, callee number, share/capability token, OTP, provider event/transaction ID, object key, object version ID, bucket name, KMS key reference, HMAC value, ciphertext envelope, session token and Redis ticket.

S1 response projections expose only internal IDs where authorized, masked display values, status codes, revision and timestamps. `customer_iam.auth_identity` responses expose no provider subject or HMAC. `subject_number` responses expose no full number; the masked value and subject/binding internal IDs are sufficient. Audit and Outbox snapshots are never copied into application logs.

### 8.4 HMAC, encryption and rotation boundaries

- HMAC and encryption keys are separate and are not stored in S1 tables. The ciphertext envelope carries an opaque key reference; `auth_identity.key_version` supports rotation for that table.
- An HMAC rotation policy, KMS product, key hierarchy and envelope algorithm are open security inputs Q2. S1 fixes only the column representation, 64-hex HMAC shape and versioned ciphertext envelope.
- Equality and uniqueness use HMAC; decryption is permitted only inside the authenticated owning service. A database role cannot decrypt a value by reading the table.
- Redaction and masking happen before hashing and before storage. A hash of a plaintext protected value does not make the plaintext acceptable in a snapshot.

## 9. Transaction Sequences

### 9.0 S1 isolation, lock, privilege and conditional-update contract

- Default isolation is `READ COMMITTED`. S1 does not depend on `SERIALIZABLE`; serialization comes from unique constraints, row locks, transaction advisory locks and conditional updates.
- Advisory locks use `pg_advisory_xact_lock(hashtextextended(<namespace-key>, 0))` and are transaction-scoped. The exact namespaces are:
  - canonical caller: `ivr.caller:<canonical_caller_hmac>`;
  - canonical number: `subject_number.number:<number_hmac>`;
  - subject lifecycle: `subject_number.subject:<subject_id>`;
  - organization lifecycle: `organization_access.organization:<organization_id>`;
  - membership lifecycle: `organization_access.membership:<organization_id>:<account_id>`, the stable logical pair, with both components rendered as canonical lowercase UUID text (36 characters, no braces or uppercase); never a membership row ID;
  - account lifecycle: `customer_iam.account:<account_id>`;
  - operations administrator mutation: `operations_iam.admin_mutation`.
- The membership pair key is derived by the owner from its own row's immutable `organization_id`/`account_id` before acquiring the lock and re-reading; a create already knows the pair from the request. A cross-module `subject_number_role` claim reader takes the same pair key because it already holds `organization_id` and `account_id`; it needs no membership ID and no additional column grant. Under the pair advisory lock the reader queries the single non-revoked `organization_membership` row for the pair and requires `status = 'ACTIVE'`; absent, `SUSPENDED` or `REVOKED` fails closed. A revoked row and its replacement share the key, and no ID-discovery loop exists.
- The lock calls use the built-in `pg_catalog.pg_advisory_xact_lock`; no new database function is created. If deployment revokes `PUBLIC` EXECUTE on that built-in, the runtime roles above receive explicit EXECUTE only for the advisory-lock functions.
- Global S1/S2 lock order (deadlock-avoiding): (1) canonical-caller advisory (S2 IVR only; S1 never takes it); (2) canonical-number advisory, one key for a one-number action and both keys sorted ascending by HMAC for replacement; S2 acquires any needed canonical-number key after the canonical-caller key and before the subject/session/call/activation keys, preserving the accepted surrounding order; (3) subject lifecycle advisory; (4) IVR session, current call and activation locks (S2 only; S1 never takes them); (5) subject-owner read — S1 takes the `business_subject` row lock because it owns the table, while S2 takes a non-locking read under the already-held subject lifecycle advisory; (6) caller-binding and risk rows in ascending UUID; S1 locks its own rows, while S2 reads under the canonical-number and subject lifecycle advisory locks (or uses a `subject_number`-owned same-transaction collaborator if a row lock is required); (7) organization lifecycle advisory and, only in the owning module, the organization row lock; (8) membership lifecycle advisory keyed by the logical `(organization_id, account_id)` pair and, only in the owning module, the membership row lock; (9) account lifecycle advisory and, only in the owning module, the account row lock; (10) task row (S2); (11) employee row plus the administrator advisory lock; (12) idempotency row; (13) Outbox insert; (14) audit insert. Multiple rows of one type are locked in ascending UUID.
- The combined subject-owner/current-binding step is internally ordered subject first, then binding. S1 locks `business_subject` before `caller_number_binding`; S2 reads the subject owner before the current caller binding at step (5). S2 keeps its surrounding canonical-caller, subject-lifecycle, session, call and activation order from the accepted plan; S1 has no session/call/activation step, so its order is canonical-number, subject-lifecycle, subject row, binding/risk, organization, membership, account.
- Cross-module lifecycle and access rows used by an authorization or side-effecting mutation transaction are never locked with a cross-owner `FOR SHARE`/`FOR UPDATE`, because PostgreSQL row locking would otherwise require an `UPDATE` grant on the other module's table. The owner module takes the lifecycle advisory lock and then its own row lock; the cross-module caller takes the same advisory lock and re-reads the exact `SELECT` columns after acquiring it. If an S2 flow genuinely needs a row lock on another module's row (for example `service_activation`), the owning module exposes a published same-transaction application-service collaborator that runs on the same connection under its own runtime role and takes that row lock; there is no cross-module DML privilege and no new database function. A revocation committed before the caller acquires the advisory lock is observed; a caller/claim that acquires it first completes before the revocation, giving the two serial orders required by S2. An unlocked read-only workspace listing (T-S1-04) is an advisory projection, not an authorization lease and not this lock contract: every protected request rechecks current organization/membership/account authority, and every side-effecting transaction uses the locks above.
- The canonical-number advisory lock protects claim, binding create/disable/re-enable/retire/replacement, risk-open/clear and canonical-number lookup decisions. A replacement acquires both the old and new number keys in sorted order before any binding row. The canonical-caller advisory lock protects S2 task create/replace/cancel/claim and IVR call establishment.
- No lock is held across an external network call. S1 has no external calls.
- A conditional update must include `AND revision = :expected_revision` and set `revision = revision + 1`. A zero-row result is not automatically a success; the service performs one bounded reread and returns the current state or a stable conflict.
- A runtime role cannot write another module's table. Platform and audit tables have no domain-role table grants. The only cross-schema write paths are the four functions in section 10.8. Subject real-name columns have column-level grants that exclude the claim runtime role.
- A function call is on the same connection and in the caller's transaction. Any exception propagates; there is no inner commit, autonomous transaction or independent subtransaction in S1.
- S1 does not call S2, does not hold session/call/activation locks, and holds no lock across a later-slice service call. Bind/disable take the account advisory lock then the account row; organization and membership writers take their own advisory lock then row; replacement takes the sorted canonical-number locks then binding rows. No writer takes an earlier-order key after a later-order key.
- S2 handoff: the exact namespaces above, including the logical membership pair key, and the subject-before-binding suborder are binding on S2. Where the accepted plan says "read/`FOR SHARE`" for a cross-owner lifecycle or binding row, S2 uses the owner lifecycle/canonical-number advisory lock plus a `SELECT` re-read, or an owner-service same-transaction collaborator if it truly needs the row lock; it does not assume a cross-owner row-lock privilege. S2 must acquire any needed canonical-number key after the canonical-caller key and before the subject/session/call/activation keys, preserving the accepted surrounding order. S2 must not hold an S1 advisory/row lock and then acquire an earlier-order key; it must carry the two serial-order revocation/confirmation assertions and the current organization/membership/account lifecycle predicates forward.

### 9.1 T-S1-01 Idempotent request with first-result finalization

1. The caller service opens one local transaction and derives the canonical semantic fingerprint. It never includes a transport timestamp, mutable balance/status or generated result identity.
2. The caller invokes `platform.idempotency_begin(scope, key, fingerprint)`.
3. `NEW`: the function inserted a `NEW` row at revision 1. The caller may execute the domain side effect exactly once and must not invoke another `begin` for the same key in the same transaction. The caller then invokes `platform.idempotency_complete(record_id, revision, result_type, result_id, redacted_result, result_sha256)` in the same transaction.
4. `REPLAY`: the stored row is `COMPLETED` and the fingerprint matches. The caller returns the stored first result and executes no side effect. The caller must authenticate and validate immutable caller/session identity before disclosing a result; the platform function does not perform protocol authentication.
5. `CONFLICT`: the same `(scope, key)` has a different fingerprint. The caller returns a stable conflict and executes no side effect.
6. `COMPLETED`: `complete` updates the row once and returns `COMPLETED`. If the row is already completed with the same result hash, it returns `ALREADY_COMPLETED` without changing anything. A different result hash, a revision mismatch or a missing record raises a stable conflict/not-found error.
7. Atomic rollback: if the domain side effect or `complete` raises, the caller's transaction aborts. The `NEW` row, result, completion, domain rows, Outbox and audit rows all roll back. A rolled-back transaction can never leave a committed `NEW` row.
8. Same-key retry: a second transaction with the same key and fingerprint waits on the unique key/row lock. If the winner committed, the loser observes `COMPLETED` and replays. If the winner rolled back, the loser's insert proceeds and returns `NEW`; exactly one `NEW` winner executes side effects.
9. Committed-`NEW` contract violation: if a row is committed as `NEW` (for example, a caller violated the same-transaction requirement), the next `begin` raises `IDEMPOTENCY_INCOMPLETE`; `ix_idempotency_new_alert` identifies it for operations. This is detectable and alertable, not silently treated as success.
10. No cross-owner write: the caller writes only its own domain tables plus the platform row through the function. It cannot update another module's data.

### 9.2 T-S1-02 Outbox append in the domain transaction

1. The domain mutation and `platform.outbox_append(...)` execute in the same local transaction.
2. The function inserts an immutable Outbox row with `status = 'PENDING'`, `attempt_count = 0`, `available_at = now()` and `revision = 1`.
3. A second append with the same `(event_domain, dedupe_key)` and the same payload hash returns the existing `outbox_id`; a different payload hash raises `OUTBOX_DEDUPE_CONFLICT`.
4. If the domain transaction rolls back, the Outbox row is absent. No business request publishes directly to RabbitMQ outside the Outbox.
5. After commit, `platform_dispatcher_role` claims pending/failed rows with a conditional revision update, publishes at least once, and records `PUBLISHED` or a bounded `FAILED`/retry state. The payload and event identity never change. Consumers deduplicate by `(event_domain, dedupe_key)` and are idempotent.
6. The dispatcher cannot update payload, `payload_sha256`, `aggregate_id` or event identity; its column grants are listed in section 10.8.

### 9.3 T-S1-03 Audit append

1. The caller appends audit facts through `audit.append_fact` in the same transaction as the audited domain mutation when the operation is auditable.
2. The function validates actor/object identity, the high-risk reason rule and the redacted/encrypted snapshot shape, then inserts exactly one immutable row.
3. `operations-audit` and the audit reader role can read. No runtime role can update, delete or truncate the table.
4. If the caller transaction rolls back, the audit row is absent. If the audit append raises, the domain mutation rolls back as well; no swallowed exception is permitted.
5. High-risk actions require `reason_code`. The S1 baseline treats every employee-actor action as high-risk and also requires a reason for `IDENTITY_PROFILE_VERIFY`, `SUBJECT_OWNER_SET`, `RISK_STATE_CHANGE`, `EMPLOYEE_ACCOUNT_CHANGE`, `ORGANIZATION_STATUS_CHANGE`, `MEMBERSHIP_CHANGE` and `IDEMPOTENCY_INCOMPLETE_ALERT`.

### 9.4 T-S1-04 Customer login resolution and workspace selection

1. Resolve the provider identity by `(provider, provider_scope, provider_subject_hmac)`, requiring `auth_identity.status = 'ACTIVE'`. The HMAC is computed server-side; the raw subject is never used as a lookup key.
2. Load the single parent `customer_account`; require `status = 'ACTIVE'`. A disabled or missing account fails closed. No account or identity is created by a failed login.
3. The personal workspace is derived from the active account. Organization workspaces are the memberships with `status = 'ACTIVE'` whose `organization.status = 'ACTIVE'`; role comes from the membership row. This read uses `customer_iam_role`'s exact `SELECT` grants on `organization_access.organization(id, display_name, status)` and `organization_access.organization_membership(organization_id, account_id, role, status)` in section 10.8, plus `USAGE` on `organization_access`. `organization.status` is part of the INV-01 read set. Suspended/revoked memberships and suspended/disabled organizations are excluded. The listing is an unlocked, read-only advisory projection: it takes no lifecycle advisory lock and no row lock, and it is not an authorization lease.
4. The server returns the allowed workspace list and role. The client cannot submit `account_id`, `organization_id`, `role` or a workspace claim that is not derived from the authenticated identity. Every protected request rechecks current account, organization and membership authority (and resource ownership where applicable) from the exact grants; every side-effecting transaction uses the section 9.0 lifecycle locks. A stale workspace projection never authorizes an operation. Session storage and revocation remain a later authentication-design input (Q1, Q19); S1 does not implement an authentication or session subsystem.
5. A login identity switch clears the previous account's selected workspace and local service context; a workspace is restored only if the new account has a current membership. No S1 write is required for the read-only resolution; login/session storage is a later authentication-design input (Q1, Q19).

### 9.5 T-S1-05 Login identity bind and collision

1. The service verifies the provider subject. In one transaction it acquires the account lifecycle advisory lock `customer_iam.account:<account_id>`, then selects the parent `customer_account` row `FOR UPDATE` (the owning `customer_iam` table) and requires `status = 'ACTIVE'`. A missing account returns `ACCOUNT_NOT_FOUND`; a non-active account returns `ACCOUNT_DISABLED`; neither inserts an identity.
2. It inserts `auth_identity` with ciphertext, HMAC, `verified_at`, `key_version` and revision 1. The unique `(provider, provider_scope, provider_subject_hmac)` key serializes concurrent binds. The loser reads the winner: the same `account_id` is an idempotent bind; a different account is `IDENTITY_CONFLICT`. There is no automatic account merge or identity migration.
3. The account disable/re-enable writer takes the same account lifecycle advisory lock, then the same account row lock, and only then conditionally updates status with revision and audit. The two serial orders are:
   - disable commits first: the bind waits, re-reads `DISABLED` and returns `ACCOUNT_DISABLED`; no identity row is inserted;
   - bind commits first: the identity row exists, then the disable commits; the account is `DISABLED`, the identity is retained, and login fails because T-S1-04 requires the parent account to be `ACTIVE`.
4. Login eligibility is never inferred from an identity row alone. A disabled account cannot authenticate and no identity bind path bypasses the account-status check. The identity relation is immutable under `customer_iam.guard_auth_identity`; a later re-enable of the account makes the retained identity usable only through the normal active-identity plus active-account check.
5. A phone number can become a login identity only through this provider flow. A business-number binding, subject claim or IVR fact cannot insert or update `auth_identity`.

### 9.6 T-S1-06 Subject claim with risk preemption

1. Inputs are the target `subject_id`, the verified `caller_number_binding_id` (or a canonical number that resolves to one binding), the authenticated `account_id`, an optional `organization_id`, the `verification_method`, the verification proof reference, and the opaque `request_id`. The service derives the canonical number HMAC from the binding; the client cannot supply an HMAC as proof. It decrypts and validates the `verification_ref_ciphertext` payload in section 4.14.1 before any claim row. A missing, incomplete, malformed, wrong-version or undecryptable reference returns `CLAIM_VERIFICATION_REF_INVALID` and writes no accepted row and no owner change.
2. Lock order: read the binding's immutable identity, acquire the canonical-number advisory lock for its `number_hmac`, acquire the subject lifecycle advisory lock `subject_number.subject:<subject_id>`, and lock the `business_subject` row `FOR UPDATE`; a zero-row subject lock returns `CLAIM_SUBJECT_NOT_FOUND` with no claim row. Then lock the `caller_number_binding` row `FOR SHARE` and the `subject_risk_state` rows in ascending UUID. Then acquire the organization lifecycle advisory lock for a supplied `organization_id`, the membership lifecycle advisory lock for the logical pair `organization_access.membership:<organization_id>:<account_id>` (the pair comes from the request, not from a discovered membership ID), and the account lifecycle advisory lock, in the global order of section 9.0. Under each lock, `SELECT` the exact section 10.8 columns and re-read every predicate; no pre-lock value is reused. A zero-row organization read returns `CLAIM_ORGANIZATION_NOT_FOUND` and a zero-row account read returns `CLAIM_ACCOUNT_NOT_FOUND`, each with no claim row.
3. **Current predicates (also applied to replay and duplicate).** Reject with the stable code when: the binding is missing (`CLAIM_BINDING_NOT_FOUND`); `binding.subject_id <> target subject_id` (`CLAIM_SUBJECT_BINDING_MISMATCH`); the binding is not `ACTIVE` or `verified_at` is null (`CLAIM_BINDING_NOT_ACTIVE`); a supplied canonical number does not HMAC to `binding.number_hmac` (`CLAIM_NUMBER_MISMATCH`); the authenticated account is not `ACTIVE` (`CLAIM_ACCOUNT_INACTIVE`); an organization context has a non-active organization (`CLAIM_ORGANIZATION_INACTIVE`) or no active membership for `(organization_id, account_id)` (`CLAIM_MEMBERSHIP_INACTIVE`); when replaying or duplicating an existing accepted organization-scoped claim, the current approved-access conjunction in step 7 must also hold (`CLAIM_ACCESS_NOT_APPROVED`); a fresh organization-scoped request that passes the organization/membership predicates proceeds to the step 7 policy boundary. A stored `ACCEPTED` claim is never returned when any current predicate fails. Existence is validated before any outcome row: an unknown target subject, authenticated account or supplied organization returns `CLAIM_SUBJECT_NOT_FOUND`, `CLAIM_ACCOUNT_NOT_FOUND` or `CLAIM_ORGANIZATION_NOT_FOUND` with no claim row, and a raw foreign-key error is never the protocol outcome. For a fresh `request_id`, a predicate failure appends exactly one `REJECTED` row only when the target subject, authenticated account and supplied organization all exist and the binding and verification reference are resolvable; an existing inactive parent may yield a `REJECTED` row only in that case. A missing binding (`CLAIM_BINDING_NOT_FOUND`) or invalid reference (`CLAIM_VERIFICATION_REF_INVALID`) writes no claim row. For an existing `request_id` the service returns the stable error and appends only an audit fact, because the claim row is append-only; the replay identity comparison in step 5 governs any mismatch and never discloses the stored outcome.
4. **Risk first.** Read all `subject_risk_state` rows for the locked binding. If any row is `OPEN`, return `CLAIM_RISK_BLOCKED` before replay or same-account-duplicate success. For a fresh `request_id` append exactly one `REJECTED` row with `risk_checked_at`; for an existing `request_id` return the blocked outcome without mutating the row and append only an audit fact. `OPEN` risk is not converted into a dialing ban.
5. **Replay identity.** Read `subject_claim` by `request_id`. If present, compare the stored `subject_id`, `account_id`, null-safe `organization_id` and `verification_method` to the current request, and decrypt the stored verification reference to compare `schemaVersion`, `bindingId`, `numberHmac` and `proofRef` to the current binding/proof. Any mismatch returns `CLAIM_REPLAY_CONFLICT` with no new row and never discloses the stored outcome. An unresolvable stored reference returns `CLAIM_REPLAY_UNRESOLVABLE` with no success. If the stored status is `ACCEPTED`, require the stored owner to equal the claim's account (organization null) or organization (organization non-null), exactly one owner column set; a null/different owner returns `CLAIM_DATA_INTEGRITY`. If the stored row is organization-scoped and the step 7 approved-access conjunction fails, return `CLAIM_ACCESS_NOT_APPROVED` with no new row and no stored success. With identity, owner, current predicates, the step 7 organization approved-access conjunction when the stored row is organization-scoped, and risk all passing, return the stored `ACCEPTED` outcome without a new row. A stored `REJECTED`/`CONFLICT` is returned as the stored non-success outcome after the identity check, without mutation.
6. **Same-account duplicate.** If no request replay exists, read the `ACCEPTED` claim for `(subject_id, account_id)`; the partial unique permits at most one. If its null-safe organization, `verification_method` and decrypted binding/number/proof identity match the current request and the owner/current predicates/risk and, for an organization-scoped row, the step 7 approved-access conjunction pass, return it without a new row. If an exact duplicate's organization-scoped approved-access conjunction fails, return `CLAIM_ACCESS_NOT_APPROVED` and append one `REJECTED` row with the new `request_id`; no owner change. A different organization, verification method or binding/number/proof identity returns `CLAIM_CONTEXT_CONFLICT` and appends exactly one `CONFLICT` row with the new `request_id`; no owner change and no disclosure of the old accepted result. A null/inconsistent owner returns `CLAIM_DATA_INTEGRITY`.
7. **Owner decision and enterprise fail-closed.** For S1, current approved organization access means `organization.status = 'ACTIVE'`, an `ACTIVE` membership for `(organization_id, account_id)`, a membership role in the accepted four-role set, and an explicit subject `owner_organization_id` equal to that organization. The exact role-to-claim permission is deferred to Product/Subject-number and Organization-access; S1 does not narrow the accepted role set, invent a new permission predicate or grant a fresh enterprise authorization. A fresh enterprise claim refuses regardless, and an existing accepted enterprise relation is reused only under this conjunction. With the subject row locked:
   - Integrity check before any owner write: under the subject lock, inspect the accepted claims for the subject. An accepted claim is consistent only when it matches the effective owner: personal owner `owner_account_id` requires an accepted personal claim with that `account_id` and null `organization_id`; organization owner `owner_organization_id` requires an accepted claim with that `organization_id`; and a null owner must have no accepted claim at all, because an accepted claim always has an owner. Any inconsistency — another account or organization, or any accepted claim on a null-owner subject — returns `CLAIM_DATA_INTEGRITY` with no repair, no new accepted row and no owner write. This detects pre-existing corrupted state only (for example a seeded inconsistent accepted claim); it is not a repair workflow and never rewrites a claim.
   - `organization_id` is null (personal claim): if both owner columns are null, set `owner_account_id = account_id` with a conditional update and `revision + 1`; if `owner_account_id = account_id`, the relation is consistent; if `owner_account_id` is a different account or `owner_organization_id` is set, return `CLAIM_OWNER_CONFLICT` and append `CONFLICT` with no owner change. A verified personal claim may set the owner in the same transaction that creates the subject or later; every S1 creation source starts with both owner columns null. This preserves the approved verified-personal claim of an unowned IVR subject.
   - `organization_id` is non-null: do not choose personal versus organization ownership from an optional workspace ID. If an owner exists and is not that organization, return `CLAIM_OWNER_CONFLICT`; if the owner is null or already that organization, a **fresh** organization-scoped claim returns `CLAIM_ORGANIZATION_OWNERSHIP_UNAPPROVED` and appends one `REJECTED` row with no owner change. This is the explicit fail-closed boundary while docs/11 defers enterprise ownership/transfer policy and docs/12 requires refusal when ownership is ambiguous. An existing accepted organization-scoped claim may be reused only through steps 5 or 6 after full identity/ownership/current-access matching; it never creates a second accepted relation and never transfers ownership. The enterprise owner/destination remains Product/Subject-number; S1 does not invent that policy.
8. Insert at most one `subject_claim` row for a fresh request with status `ACCEPTED`, `REJECTED` or `CONFLICT` as decided, `risk_checked_at`, and `verified_at` only for `ACCEPTED`. `verified_at` is the accepted-claim decision timestamp taken at or after `risk_checked_at`; it is never the original proof-attempt time, which remains inside the immutable verification reference. The row is append-only and the verification reference is immutable. If the insert loses a concurrent `request_id` race, the unique violation aborts the whole transaction. The service must not read in the aborted transaction. It retries the entire claim transaction from the beginning a bounded number of times, reacquiring every advisory lock, re-reading every predicate (including current risk) and rechecking the replay identity; it returns the existing outcome only when the full subject/account/organization/method/binding/number/proof identity matches, otherwise a stable conflict. If the retry budget is exhausted, it returns `CLAIM_CONCURRENCY_EXHAUSTED` with no claim row from the failed attempts and no owner change. S1 uses no savepoint or subtransaction for this path, and no tentative owner write survives a rollback. The partial unique accepted key prevents two accepted claims for one subject/account.
9. Append one audit fact through `audit.append_fact` in the same transaction when the operation is audited, using the stable reason code for the outcome. Commit. The claim path writes only `subject_claim`, the subject owner columns on a personal null-to-account decision, and audit; it never writes `auth_identity`, account, binding status, risk state, subject real-name, evidence, trial, usage or historical facts. The claim does not require subject real-name verification, activation or a customer login beyond the authenticated claimant; an unregistered/unverified subject remains eligible for the S2 direct IVR activation/trial path, and no claim rule imposes a dialing ban.
10. Claim concurrency with risk and lifecycle writers is serialized by the canonical-number, subject-lifecycle, binding-row, organization, membership-pair and account locks above. A risk-open or revocation committed first is observed and blocks; a claim committed first is not retroactively invalidated by later risk, and the later risk governs future claims/historical-evidence access without rewriting the committed claim or evidence. A later replay of that `request_id` under current `OPEN` risk returns `CLAIM_RISK_BLOCKED` while the immutable historical `ACCEPTED` row remains stored; it is not disclosed as a successful replay. No claim path rewrites historical evidence.

Stable claim outcome codes:

| Condition | Returned code | Fresh-request row |
| --- | --- | --- |
| Target subject does not exist | `CLAIM_SUBJECT_NOT_FOUND` | none |
| Authenticated account does not exist | `CLAIM_ACCOUNT_NOT_FOUND` | none |
| Supplied organization does not exist | `CLAIM_ORGANIZATION_NOT_FOUND` | none |
| Missing binding | `CLAIM_BINDING_NOT_FOUND` | none |
| Binding subject differs from target | `CLAIM_SUBJECT_BINDING_MISMATCH` | `REJECTED` |
| Binding not `ACTIVE` or unverified | `CLAIM_BINDING_NOT_ACTIVE` | `REJECTED` |
| Canonical number HMAC mismatch | `CLAIM_NUMBER_MISMATCH` | `REJECTED` |
| Existing authenticated account not `ACTIVE` | `CLAIM_ACCOUNT_INACTIVE` | `REJECTED` |
| Existing organization not `ACTIVE` | `CLAIM_ORGANIZATION_INACTIVE` | `REJECTED` |
| No `ACTIVE` membership for the pair | `CLAIM_MEMBERSHIP_INACTIVE` | `REJECTED` |
| Access not currently approved | `CLAIM_ACCESS_NOT_APPROVED` | `REJECTED` |
| Open risk | `CLAIM_RISK_BLOCKED` | `REJECTED` |
| Request replay identity mismatch | `CLAIM_REPLAY_CONFLICT` | none |
| Stored replay reference unresolvable | `CLAIM_REPLAY_UNRESOLVABLE` | none |
| Request-ID unique race retry budget exhausted | `CLAIM_CONCURRENCY_EXHAUSTED` | none |
| Same-account duplicate with different organization/binding/number/proof | `CLAIM_CONTEXT_CONFLICT` | `CONFLICT` |
| Different existing owner | `CLAIM_OWNER_CONFLICT` | `CONFLICT` |
| Fresh organization-scoped claim pending policy | `CLAIM_ORGANIZATION_OWNERSHIP_UNAPPROVED` | `REJECTED` |
| Accepted row owner inconsistent | `CLAIM_DATA_INTEGRITY` | none |
| Invalid input verification reference | `CLAIM_VERIFICATION_REF_INVALID` | none |

### 9.7 T-S1-07 Number binding create, disable, re-enable, retire and replace

1. Normalize the canonical number and derive its HMAC server-side; the client cannot supply the HMAC as proof. For a single-number action acquire the canonical-number advisory lock for its HMAC. For replacement derive the old and new HMACs, acquire both canonical-number locks in ascending HMAC order before any binding row, and return `BINDING_SAME_NUMBER` with no mutation when old and new are the same number.
2. Under the lock(s), read the non-terminal binding rows for the new `number_hmac` and the same-subject row for `(subject_id, number_hmac)`. If an `ACTIVE` binding belongs to another subject, return `BINDING_ACTIVE_CONFLICT`; never transfer or merge the subject. If it belongs to the same subject and is the requested `ACTIVE` binding, an identical create is an idempotent success and a replacement with that same number is `BINDING_SAME_NUMBER`. If it is a different `ACTIVE` binding for the same subject, a replacement reuses that destination and retires only the old binding; a create returns the existing `ACTIVE` binding as the idempotent result. A same-subject `DISABLED` row is never revived by a create: return stable `BINDING_DISABLED_CONFLICT` with no mutation and require the explicit re-enable action in step 6. A `RETIRED` row is terminal and returns `BINDING_RETIRED_CONFLICT`. A create never reports a `DISABLED` or `RETIRED` row as an active success.
3. Create: acquire the subject lifecycle advisory lock and the subject row `FOR SHARE` (the binding and subject are owned by `subject_number`), verify the subject exists and its owner/access has already been authorized, then insert the new `ACTIVE` binding. The partial unique active-number index and unique `(subject_id, number_hmac)` are backstops. A unique loser aborts the whole transaction; the service starts a bounded retry from the beginning, reacquiring the canonical-number and subject locks and re-reading, and maps the current winner deterministically: same-subject identical `ACTIVE` is an idempotent success; same-subject `DISABLED` is `BINDING_DISABLED_CONFLICT`; same-subject `RETIRED` is `BINDING_RETIRED_CONFLICT`; another subject's `ACTIVE` row is `BINDING_ACTIVE_CONFLICT`; any other state is a stable current-state conflict. It never exposes a raw unique error or returns a non-active row as success.
4. Replace: after acquiring both number locks in sorted order, acquire the subject lifecycle advisory lock and the subject row `FOR SHARE`, then lock the old and destination binding rows in ascending UUID (`FOR UPDATE`), and re-read them. The old binding must be `ACTIVE` and belong to the requested subject; otherwise return the current state or a stable conflict with no change. Validate the destination number as in step 2. Conditionally retire the old binding (`ACTIVE → RETIRED`, revision + 1) and insert a new binding for the new number, re-enable the same-subject `DISABLED` destination row, or reuse the same-subject `ACTIVE` destination row, in one transaction. If the new number belongs to another subject, if the destination row is `RETIRED`, or if any conditional update/unique insert loses, the entire transaction rolls back and the old binding remains `ACTIVE`; a bounded retry from the beginning reacquires both canonical-number locks in sorted order and re-reads, then maps the loser to a stable current-state/conflict response. A same-number replacement is `BINDING_SAME_NUMBER` before any row change.
5. Disable: acquire the canonical-number lock and lock the owned binding row `FOR UPDATE`; conditionally update `ACTIVE → DISABLED`. The number is no longer the active unique binding but historical rows remain.
6. Re-enable: acquire the canonical-number lock and lock the owned binding row `FOR UPDATE`; read the active binding for the number. If another active binding exists, return `BINDING_ACTIVE_CONFLICT`; otherwise conditionally update `DISABLED → ACTIVE`. A `RETIRED` row returns `BINDING_RETIRED_CONFLICT`. Re-enable is a separate explicit action; a create never performs it. Both create/re-enable serial orders are defined: if re-enable commits first, a later create sees the same-subject `ACTIVE` row and returns an idempotent success; if create runs first while the row is `DISABLED`, it returns `BINDING_DISABLED_CONFLICT` with no mutation, and the explicit re-enable afterwards still succeeds unless another active binding exists.
7. Retire: acquire the canonical-number lock and lock the owned binding row `FOR UPDATE`; conditionally update `ACTIVE`/`DISABLED → RETIRED`. `RETIRED` is terminal; rows are never deleted. A number may later be bound to a different subject through a new row because the active unique key is released and the historical subject/number key is subject-scoped. Unbinding never rewrites evidence, trial, usage, subject real-name or login identity.
8. Concurrent create/replace/re-enable/disable/retire serialize on the sorted canonical-number lock(s) and then binding rows; exactly one conditional update wins and losers receive the current state or a stable conflict. Planned assertions: replacement versus create for the same new number by another subject has one winner and one `BINDING_ACTIVE_CONFLICT`; replacement versus re-enable for the same new number has one winner and the loser leaves the old binding `ACTIVE`; same-number replacement is `BINDING_SAME_NUMBER` with no change; a replacement unique loser rolls back and retains the old active binding; create versus create has one winner and an idempotent or stable-conflict loser; create versus same-subject `DISABLED` returns `BINDING_DISABLED_CONFLICT` with the row unchanged, and the subsequent explicit re-enable follows the two serial orders in step 6; create versus re-enable never returns the disabled row as an active success and never exposes a raw unique violation; disable versus claim serializes on the canonical-number lock, so the claim sees the disabled state or the disable governs later claims.

### 9.8 T-S1-08 Subject risk open, clear and re-open

1. Acquire the canonical-number lock for the binding and lock the binding/risk rows.
2. Upsert one risk row by `(caller_number_binding_id, risk_type)`. Opening sets `state = 'OPEN'`, `cleared_at = NULL`, `source`, `observed_at` and increments revision. Clearing sets `state = 'CLEARED'`, `cleared_at = clock_timestamp()` and increments revision. A re-open is a conditional update from `CLEARED` with a new observation and audit reason.
3. Risk writers and claim writers use the same canonical-number lock, so an `OPEN` cannot slip between a claim's risk read and its owner update.
4. `OPEN` blocks number claims and historical-evidence access. It is not by itself a predicate for refusing a new outbound call; any future call-blocking risk rule is Q24, owned by Product/Risk/IVR and not invented here.

### 9.9 T-S1-09 Last enabled operations administrator

1. The employee mutation transaction takes `pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation', 0))`.
2. It locks the target employee row and counts `employee_account` rows with `role = 'ADMIN' AND status = 'ACTIVE'`.
3. If the target is the last enabled administrator and the action would disable or demote it, the transaction raises a stable error and changes nothing.
4. Otherwise it conditionally updates role/status with revision + 1 and appends an audit fact. Concurrent admin mutations serialize on the advisory lock.

### 9.10 T-S1-10 Account, organization, membership and binding lifecycle writers

- Customer account disable/re-enable acquires `customer_iam.account:<account_id>`, then the account row `FOR UPDATE`, and updates only `customer_iam.customer_account` with revision and audit. Identity bind uses the same advisory lock and row lock in section 9.5.
- Organization suspend/resume/disable acquires `organization_access.organization:<organization_id>`, then the organization row `FOR UPDATE`, and updates only `organization_access.organization` with revision and audit.
- Membership role/suspend/revoke and re-grant acquire the logical pair advisory lock `organization_access.membership:<organization_id>:<account_id>` (canonical lowercase UUID text), never a membership-ID key. The owner reads its immutable pair from its own row before locking and re-reads after the lock; a create already knows the pair from the request. Under the pair lock the owner reads the unique non-revoked row for the pair, requires the expected current state, then locks that row `FOR UPDATE` and updates only `organization_access.organization_membership` with revision and audit. For the explicit create/re-grant action an absent row is the create branch, and a `REVOKED` row is replaced by a new row under the same key; a role/suspend/revoke action on an absent row returns the stable not-found/inactive result and mutates nothing. If the writer must also read or lock the account, it acquires the account lifecycle advisory lock later in the global order. Revocation is terminal for the row; a later grant inserts a new row that shares the same pair key, so revoke/re-grant serializes with every pair reader.
- Binding disable/re-enable/retire acquires the canonical-number advisory lock, then the binding row `FOR UPDATE`, and updates only `subject_number.caller_number_binding` with revision and audit. Replacement follows section 9.7.
- Subject owner and real-name writers acquire `subject_number.subject:<subject_id>`, then the subject row `FOR UPDATE`; the real-name writer is the only role allowed to change the real-name columns.
- These writers take the owning module's lifecycle advisory lock and then its own row lock in the global order. Cross-module claim/confirmation readers take the same advisory lock and re-read the exact `SELECT` columns; they do not take a cross-owner row lock. A refused access change and a committed claim therefore have two serial orders, never a mid-lock interleaving. S1 fixes the S1 writer contract and exact namespaces that make the S2 serialization possible.
- No lifecycle writer deletes a row, rewrites evidence or creates a login identity.

### 9.11 Reserved S2/S3 transaction sequences

The accepted plan's activation confirmation with first-trial issuance, task create/replace/cancel, IVR resolve, direct DTMF authorization, usage settlement, reservation reconciliation, order creation, payment-to-issuance, evidence solidification and share admission with ticket reconstruction are not authored in S1. They remain frozen handoff for S2/S3. This document does not claim those transactions were designed, executed or tested.

S2 handoff for canonical-number acquisition: whenever an S2 IVR claim/authorization path needs the canonical-number advisory lock, it acquires that key after the canonical-caller key and before the subject/session/call/activation keys, preserving the accepted surrounding order. S1 records the placement only; it does not author S2 transaction code, tables or tests.

### 9.12 S1 deterministic behavior assertions

The following are document-level S1 requirements for the later executable suite; no PostgreSQL or concurrency test was run in S1:

- Same-key/same-fingerprint begin returns `REPLAY` with the first result; same-key/different-fingerprint returns `CONFLICT`; no second side effect.
- A forced failure after `begin` and before commit leaves no idempotency row; a forced failure after `complete` rolls back the completion and domain writes.
- A committed `NEW` row raises `IDEMPOTENCY_INCOMPLETE` on the next begin and is listed by `ix_idempotency_new_alert`.
- `complete` finalizes once; a same-hash repeat returns `ALREADY_COMPLETED`; a different hash or revision fails and never overwrites.
- Outbox and audit rows exist only when the domain transaction commits; duplicate Outbox append with the same hash is idempotent and a different hash conflicts.
- No domain role can write platform/audit tables or another module's table; each can execute only its granted functions.
- Identity collision returns the same account or `IDENTITY_CONFLICT`; it never creates a second account for one provider subject.
- Claim/risk serialization has exactly two outcomes: risk-open first blocks the claim; claim first commits the accepted claim and owner, and a later risk-open affects future claims/evidence access without rewriting the past claim.
- An accepted claim always has an owner; a null-owner accepted claim fails closed and is never silently repaired.
- Subject real-name cannot be changed by the claim role; only `subject_identity_verification_role` can update the two real-name columns.
- Append-only audit and claim rows reject update/delete; a parent delete with children fails; no cascade exists.
- Claim predicates: a binding for another subject, a `DISABLED`/`RETIRED` binding, an unverified binding, a canonical-number HMAC mismatch, a disabled account, a suspended/disabled organization or a non-active membership is rejected with its stable `CLAIM_*` code, no accepted row and no owner change. The check uses post-lock values.
- Claim replay identity: reusing one `request_id` with a different subject, account, organization, method, binding, number or proof returns `CLAIM_REPLAY_CONFLICT` and never the stored success; a missing or incomplete stored verification reference returns `CLAIM_REPLAY_UNRESOLVABLE`; a stored `ACCEPTED` row with a null/different owner returns `CLAIM_DATA_INTEGRITY`.
- Risk preemption: an accepted `request_id` replayed while its binding has `OPEN` risk returns `CLAIM_RISK_BLOCKED`, not the stored accepted result; risk is read before replay/duplicate success and the immutable row is not mutated.
- Organization context: a same-account request for a different organization than the stored accepted claim returns `CLAIM_CONTEXT_CONFLICT` and never reuses or discloses the old accepted outcome; it does not collapse on the `(subject_id, account_id)` partial key. A changed `verification_method` on a same-account duplicate is the same stable context conflict and never reuses or discloses the old accepted result.
- Unknown referenced identities: a target subject, authenticated account or supplied organization ID with no row returns `CLAIM_SUBJECT_NOT_FOUND`, `CLAIM_ACCOUNT_NOT_FOUND` or `CLAIM_ORGANIZATION_NOT_FOUND` with no claim row; these boundary cases never produce a raw foreign-key error. An existing but inactive organization or account, or an absent/inactive membership for a pair whose organization/account/subject exist, appends exactly one `REJECTED` row with a stable code.
- Request-ID unique race: the unique loser's whole transaction is rolled back; the retry starts from the beginning in a new transaction, reacquires every lock and rechecks current risk/lifecycle/identity; no read occurs in the aborted transaction, no tentative owner write survives, and exhaustion returns `CLAIM_CONCURRENCY_EXHAUSTED` with no S1 savepoint or subtransaction.
- Claim data integrity: a seeded accepted claim inconsistent with the current or null subject owner returns `CLAIM_DATA_INTEGRITY` with no repair, no new accepted claim and no owner write.
- Subject creation ownership: every S1 creation source, including `OPERATIONS`, inserts both owner columns null; the runtime role has no INSERT privilege on either owner column and no UPDATE privilege on `owner_organization_id`; a verified personal claim sets only `owner_account_id`, in the same transaction or later. No optional workspace or source selects ownership.
- Claim timestamps: `verified_at` is the accepted-claim decision time at or after `risk_checked_at`; the original proof-attempt timing stays in the immutable verification reference.
- Membership pair serialization: readers and the owner use `organization_access.membership:<organization_id>:<account_id>` with canonical lowercase UUID components; absent membership fails closed, create and revoke/re-grant use the same key, the claim role needs only the declared organization/membership/account columns and no membership ID, and role/revoke/re-grant serial orders are asserted.
- Workspace projection: the T-S1-04 list takes no lifecycle advisory lock and is not an authorization lease; each protected request rechecks current authority and side-effecting transactions hold the section 9.0 locks.
- Enterprise ownership boundary: a fresh organization-scoped claim returns `CLAIM_ORGANIZATION_OWNERSHIP_UNAPPROVED` with no owner change for an unowned or same-organization subject; a different existing owner returns `CLAIM_OWNER_CONFLICT`; a personal claim of an unowned IVR subject is accepted and sets only `owner_account_id`; an existing accepted organization-scoped claim is reused only after full identity/ownership/current-access matching.
- Identity bind/disable: both serial orders are asserted. Disable first rejects the bind with no identity row; bind first retains the identity but login remains blocked while the account is `DISABLED`. A direct attempt to change `auth_identity.account_id`/provider tuple or `identity_profile.account_id` fails.
- Replacement: replace acquires both canonical-number locks in sorted order, rejects a same-number replacement with `BINDING_SAME_NUMBER`, and returns `BINDING_ACTIVE_CONFLICT` for another subject's number. A create/re-enable/replace race has one winner; a replacement loser rolls back and the old binding remains `ACTIVE`; no raw unique error is exposed.
- Cross-module privilege: `customer_iam_role` and `subject_number_role` can `SELECT` only the declared organization/membership/account columns and cannot `INSERT`/`UPDATE`/`DELETE` those tables; no cross-owner row lock is taken; the lifecycle advisory locks serialize the owner writer and the cross-module reader.
- Function IDs: `platform.idempotency_begin`, `platform.outbox_append` and `audit.append_fact` insert a non-null UUID from `pg_catalog.gen_random_uuid()`, no table has an `id` default, and no function signature has a `p_id` argument.
- Subject real-name: `subject_identity_verification_role` can locate the subject by `id` and update only the real-name/revision/time columns; it cannot change owner, source or binding columns, and claim/IVR/activation paths leave real-name unchanged.
- An unowned/unverified direct IVR subject is not made ineligible for a personal claim, activation, trial or future calling by any S1 claim rule; no claim imposes a dialing ban or rewrites historical evidence.
- S1 never rewrites evidence, trial, usage or order facts; evidence ownership and immutability are specified and tested in S3, not claimed here.
- The last enabled administrator cannot be disabled or demoted.
- `entitlement_metering.issue_first_trial` is absent from V001 and from every S1 dependency.

## 10. Cross-Schema Function Contracts

### 10.1 Frozen function-wave baseline

The accepted plan has five function-wave rows: four V001/S1 functions and one visibly planned V006/S2 function. The block is verbatim. S1 authors only the four V001 contracts.

<!-- function-wave:start -->
platform.idempotency_begin|V001|platform.idempotency_record
platform.idempotency_complete|V001|platform.idempotency_record
platform.outbox_append|V001|platform.outbox_event
audit.append_fact|V001|audit.audit_fact
entitlement_metering.issue_first_trial|V006|entitlement_metering.trial_policy_version,entitlement_metering.entitlement_grant,entitlement_metering.trial_grant,entitlement_metering.entitlement_balance
<!-- function-wave:end -->

### 10.2 Common S1 function contract

All four S1 functions:

- are `SECURITY DEFINER`, owned by the named NOLOGIN module owner, and not executable by `PUBLIC`;
- declare `SET search_path = platform, pg_temp` or `SET search_path = audit, pg_temp` and schema-qualify every referenced object;
- execute on the caller's connection in the caller's transaction; they contain no `COMMIT`, `ROLLBACK`, `START TRANSACTION`, autonomous transaction or transaction-level `SET`;
- raise a stable exception on invalid input or semantic conflict; the exception aborts the caller's transaction and rolls back the domain and function writes together;
- never swallow a database exception and never use a rollback-only transaction to commit a success;
- accept only redacted/encrypted snapshots and reject the documented protected-key denylist;
- treat identity, result and payload data as immutable after the documented write point;
- are granted only to the runtime roles in section 10.8; each caller also receives `USAGE` on the function's schema without `CREATE`. Cross-schema callers do not need table privileges.

The function owner must not be a superuser or a login role. Deployment role provisioning and whether the migration identity can create `SECURITY DEFINER` functions are Q17/Q23.

### 10.3 platform.idempotency_begin

Signature target:

`platform.idempotency_begin(p_scope text, p_key text, p_fingerprint text) RETURNS TABLE (record_id uuid, revision integer, outcome text, result_type text, result_id uuid, result_snapshot jsonb, result_sha256 text)`

| Item | Contract |
| --- | --- |
| Owner / mode | `platform_reliability_owner`; `SECURITY DEFINER`; `SET search_path = platform, pg_temp`; V001 |
| Caller roles | S1 domain runtime roles and later S2/S3 domain runtime roles after their wave grants |
| Arguments | `p_scope` = `IVR_CONFIRM` or `PURCHASE`; `p_key` non-empty length 1-512; `p_fingerprint` lowercase canonical SHA-256 hex |
| Outcomes | `NEW` with `record_id`, `revision = 1`, null result fields; `REPLAY` with the stored result fields; `CONFLICT` with no result fields |
| Errors | `IDEMPOTENCY_ARGUMENT_INVALID`; `IDEMPOTENCY_INCOMPLETE` for an existing `NEW` row not created by this invocation |
| Semantics | `INSERT ... (id, scope, idempotency_key, request_fingerprint, status, revision) VALUES (pg_catalog.gen_random_uuid(), ...) ON CONFLICT (scope, idempotency_key) DO NOTHING`, then select the row `FOR UPDATE`. A won insert returns `NEW`. An existing `COMPLETED` row with the same fingerprint returns `REPLAY`; a different fingerprint returns `CONFLICT`. An existing `NEW` row raises `IDEMPOTENCY_INCOMPLETE`; the same-transaction re-begin is not a supported success path. The function generates the inserted `id` with `pg_catalog.gen_random_uuid()`; there is no table default and no `p_id` argument. Never updates a stored result. |
| Atomicity | The `NEW` insert is in the caller's transaction. The caller must call `complete` in the same transaction; a rollback removes the row. The function performs no transaction control. |
| Redaction | No snapshot is written by `begin`. The caller must pass only redacted/encrypted results to `complete`. |

### 10.4 platform.idempotency_complete

Signature target:

`platform.idempotency_complete(p_record_id uuid, p_expected_revision integer, p_result_type text, p_result_id uuid, p_result_snapshot jsonb, p_result_sha256 text) RETURNS text`

| Item | Contract |
| --- | --- |
| Owner / mode | `platform_reliability_owner`; `SECURITY DEFINER`; `SET search_path = platform, pg_temp`; V001 |
| Caller roles | The same runtime roles granted `idempotency_begin` |
| Arguments | `p_record_id` non-null; `p_expected_revision` positive; `p_result_type` non-empty length 1-64; `p_result_id` non-null; `p_result_snapshot` object/array or encrypted envelope; `p_result_sha256` lowercase SHA-256 hex of the redacted result |
| Outcomes | `COMPLETED` (this call filled the result), `ALREADY_COMPLETED` (same result hash already stored) |
| Errors | `IDEMPOTENCY_NOT_FOUND`; `IDEMPOTENCY_CONFLICT` for a different result hash, a revision mismatch, or a non-`NEW` row with a different result; `IDEMPOTENCY_ARGUMENT_INVALID`; `IDEMPOTENCY_INCOMPLETE` never applies here |
| Semantics | One conditional update: `SET status = 'COMPLETED', result_type = ..., result_id = ..., result_snapshot = ..., result_sha256 = ..., completed_at = clock_timestamp(), revision = revision + 1 WHERE id = p_record_id AND status = 'NEW' AND revision = p_expected_revision`. If one row updates, return `COMPLETED`. On a zero-row update, the function selects the row `FOR UPDATE` and maps it deterministically: `COMPLETED` with the same hash returns `ALREADY_COMPLETED`; a different hash, a `NEW` row with a mismatched revision, or any other non-`NEW` result state raises `IDEMPOTENCY_CONFLICT`; a missing row raises `IDEMPOTENCY_NOT_FOUND`. A completed result can never be overwritten. |
| Atomicity | Must run in the same caller transaction as `begin` and the side effect. Any exception aborts all writes; no completion is committed independently. No transaction control. |
| Redaction | The snapshot is redacted or an encrypted envelope; the denylist in section 10.7 applies. The stored result is immutable. |

### 10.5 platform.outbox_append

Signature target:

`platform.outbox_append(p_event_domain text, p_dedupe_key text, p_aggregate_type text, p_aggregate_id uuid, p_event_type text, p_payload_redacted jsonb, p_payload_ciphertext text, p_payload_sha256 text) RETURNS uuid`

| Item | Contract |
| --- | --- |
| Owner / mode | `platform_reliability_owner`; `SECURITY DEFINER`; `SET search_path = platform, pg_temp`; V001 |
| Caller roles | S1 domain runtime roles and later S2/S3 domain runtime roles after their wave grants |
| Arguments | Non-empty domain/dedupe/aggregate/event codes; valid aggregate UUID; at least one of redacted payload or ciphertext envelope; lowercase SHA-256 hash |
| Outcomes | Existing or new `outbox_id` |
| Errors | `OUTBOX_DEDUPE_CONFLICT` for the same `(event_domain, dedupe_key)` and a different payload hash; `OUTBOX_PAYLOAD_INVALID` for missing payload, bad envelope, protected plaintext or invalid hash |
| Semantics | Append one immutable row in the caller's transaction using an insert that resolves the unique `(event_domain, dedupe_key)` key, with `id = pg_catalog.gen_random_uuid()` generated in the function body (no table default, no `p_id` argument), then select the existing or inserted row. Same domain/dedupe key and same hash returns the existing id; a different hash raises `OUTBOX_DEDUPE_CONFLICT`. Concurrent same-key callers wait on the unique key; one insert wins and the loser observes the winner's hash. The event identity and payload are never updated. The dispatcher changes only `status`, `attempt_count`, `available_at`, `published_at` and `revision`. |
| Atomicity | A rollback removes the Outbox row. No direct MQ publish occurs in the function. |
| Redaction | `payload_redacted` is redacted; `payload_ciphertext` is an encrypted envelope; the denylist applies to redacted JSON. |

### 10.6 audit.append_fact

Signature target:

`audit.append_fact(p_occurred_at timestamptz, p_actor_type text, p_actor_id uuid, p_action_code text, p_object_type text, p_object_id uuid, p_reason_code text, p_before_snapshot jsonb, p_after_snapshot jsonb, p_request_id text, p_trace_id text) RETURNS uuid`

| Item | Contract |
| --- | --- |
| Owner / mode | `audit_owner`; `SECURITY DEFINER`; `SET search_path = audit, pg_temp`; V001 |
| Caller roles | S1 domain runtime roles, `operations_service_role`, and later S2/S3 domain runtime roles after their wave grants; no table write privilege is granted |
| Arguments | `actor_type` in the four actor classes; `actor_id` required except `SYSTEM`; non-empty action/object codes; object ID required unless `SYSTEM`; reason required for high-risk actions; redacted/encrypted snapshots; optional request/trace IDs |
| Outcomes | New `audit_id` |
| Errors | `AUDIT_ARGUMENT_INVALID`; `AUDIT_REASON_REQUIRED` for an employee-actor or listed high-risk action without a reason |
| Semantics | Insert exactly one immutable `audit.audit_fact` row with `id = pg_catalog.gen_random_uuid()` generated in the function body (no table default, no `p_id` argument). No update, delete, truncate or second write function exists. The table trigger rejects mutation even if a future privilege is misconfigured. |
| Atomicity | Runs in the caller's transaction; a rollback removes the audit fact. An audit exception aborts the caller's business transaction. |
| Redaction | Snapshots are redacted JSON or an encrypted envelope; the denylist applies. No plaintext secret or full protected value. |

### 10.7 Protected-key denylist and encrypted envelope

The four functions reject a redacted JSON object that contains any of the following keys, case-insensitively, at any depth: `phone`, `mobile`, `callerNumber`, `calleeNumber`, `providerSubject`, `identityNumber`, `idNumber`, `legalName`, `shareCode`, `otp`, `objectKey`, `objectVersionId`, `bucket`, `providerTransactionId`, `providerEventId`, `secret`, `token`, `password`, `hmac`, `ciphertext`, `keyRef`, `kmsKey`.

Masked variants such as `calleeNumberMasked` and explicitly non-sensitive business fields are allowed. An encrypted snapshot is the single-key object `{"enc":"v1.<key_ref>.<base64url>"}`; an encrypted result follows the same shape. The function validates the envelope shape; it does not decrypt. Redacted and encrypted representations must not be mixed in one object.

### 10.8 S1 grant matrix

Function, schema and platform/audit table grants:

| Object | Owner | PUBLIC | S1 runtime grants |
| --- | --- | --- | --- |
| `platform` schema | `platform_reliability_owner` | none | `USAGE` without `CREATE` to `customer_iam_role`, `organization_access_role`, `operations_iam_role`, `subject_number_role`; later S2/S3 roles in their waves. No table privileges. |
| `audit` schema | `audit_owner` | none | `USAGE` without `CREATE` to `customer_iam_role`, `organization_access_role`, `operations_iam_role`, `subject_number_role`, `subject_identity_verification_role`, `operations_service_role`, `audit_reader_role`; later S2/S3 roles in their waves. No table privileges to domain roles. |
| `organization_access` schema | `organization_access_owner` | none | `USAGE` without `CREATE` to `customer_iam_role` and `subject_number_role` for the declared read paths. |
| `customer_iam` schema | `customer_iam_owner` | none | `USAGE` without `CREATE` to `subject_number_role` for the declared account-status read. |
| `platform.idempotency_begin` | `platform_reliability_owner` | none | `EXECUTE` to `customer_iam_role`, `organization_access_role`, `operations_iam_role`, `subject_number_role`; later S2/S3 roles in their waves. |
| `platform.idempotency_complete` | `platform_reliability_owner` | none | Same `EXECUTE` set as `begin`. |
| `platform.outbox_append` | `platform_reliability_owner` | none | Same `EXECUTE` set as `begin`. |
| `audit.append_fact` | `audit_owner` | none | `EXECUTE` to the four S1 domain runtime roles, `subject_identity_verification_role`, `operations_service_role`; later S2/S3 roles in their waves. |
| `platform.idempotency_record` | `platform_reliability_owner` | none | No domain table grants; `platform_reliability_role` `SELECT` for operations; functions only for writes. |
| `platform.outbox_event` | `platform_reliability_owner` | none | `platform_dispatcher_role` `SELECT` and `UPDATE` on `status`, `attempt_count`, `available_at`, `published_at`, `revision` only; no insert/delete; `platform_reliability_role` `SELECT`. |
| `audit.audit_fact` | `audit_owner` | none | `audit_reader_role` `SELECT` only; no insert/update/delete/truncate; function only for writes. |

Cross-module `SELECT` grants (no `INSERT`/`UPDATE`/`DELETE`, no row-lock privilege):

| Grantee | Table | Exact columns | Purpose |
| --- | --- | --- | --- |
| `customer_iam_role` | `organization_access.organization` | `id`, `display_name`, `status` | Workspace list and `organization.status = 'ACTIVE'` |
| `customer_iam_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` | Workspace membership and role; unlocked advisory projection, no lifecycle lock |
| `subject_number_role` | `organization_access.organization` | `id`, `status` | Claim organization-context existence/status |
| `subject_number_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` | Claim membership/role status under the logical pair lock; no membership `id` needed |
| `subject_number_role` | `customer_iam.customer_account` | `id`, `status` | Claim authenticated-account existence/`ACTIVE` predicate |

Owned-table column grants:

| Table | Role | `SELECT` | `INSERT` columns | `UPDATE` columns |
| --- | --- | --- | --- | --- |
| `customer_iam.customer_account` | `customer_iam_role` | all | all | `display_name`, `status`, `revision`, `updated_at` |
| `customer_iam.auth_identity` | `customer_iam_role` | all | all | `status`, `verified_at`, `key_version`, `revision` |
| `customer_iam.identity_profile` | `customer_iam_role` | all | all | `real_name_status`, `legal_name_ciphertext`, `id_number_ciphertext`, `id_number_hmac`, `verification_ref_ciphertext`, `verified_at`, `revision` |
| `organization_access.organization` | `organization_access_role` | all | all | `display_name`, `status`, `revision`, `updated_at` |
| `organization_access.organization_membership` | `organization_access_role` | all | all | `role`, `department_label`, `status`, `revision` |
| `operations_iam.employee_account` | `operations_iam_role` | all | all | `display_name`, `role`, `status`, `revision` |
| `subject_number.business_subject` | `subject_number_role` | all | `id`, `source`, `revision`, `created_at`, `updated_at` (no owner columns; every creation starts both owner columns null) | `owner_account_id`, `revision`, `updated_at` (no `owner_organization_id`) |
| `subject_number.business_subject` | `subject_identity_verification_role` | `id`, `real_name_status`, `real_name_verified_at`, `revision`, `updated_at` | none | `real_name_status`, `real_name_verified_at`, `revision`, `updated_at` |
| `subject_number.caller_number_binding` | `subject_number_role` | all | all | `status`, `verified_at`, `revision` |
| `subject_number.subject_claim` | `subject_number_role` | all | all | none |
| `subject_number.subject_risk_state` | `subject_number_role` | all | all | `state`, `source`, `observed_at`, `cleared_at`, `revision` |

- No S1 runtime role receives `DELETE` or `TRUNCATE` on any business table. `PUBLIC` receives no schema `USAGE`/`CREATE`, table privilege or function `EXECUTE`.
- `operations_service_role` is a NOLOGIN S1 service role used only for audit append; it receives `USAGE` on `audit` and `EXECUTE` on `audit.append_fact`, with no table privilege.
- `subject_identity_verification_role` receives `USAGE` on `subject_number` and `audit`, the narrow `business_subject` read/update columns above, and `EXECUTE` on `audit.append_fact`; it cannot write owner, source, binding, claim or risk rows.
- Cross-module lifecycle/access reads inside an authorization or side-effecting transaction use the lifecycle advisory locks in section 9.0; the membership key is the logical `(organization_id, account_id)` pair and the exact columns above are sufficient, so no membership `id` grant or privilege widening is required. The T-S1-04 workspace listing is an unlocked read-only advisory projection, not an authorization lease. No cross-owner `FOR SHARE`/`FOR UPDATE` privilege is granted, because that would require `UPDATE` on another module's table. Where an S2 flow needs a cross-owner row lock, the owning module exposes a same-transaction application-service collaborator under its own role, not a cross-module table grant and not a new published database function.
- `subject_number_role` has no `INSERT` privilege on `business_subject.owner_account_id`/`owner_organization_id` and no `UPDATE` privilege on `owner_organization_id`. Every S1 subject creation starts with both owner columns null; only the verified personal claim sets `owner_account_id` from null. The owner columns and FKs remain for the legacy explicit enterprise relation and reads; no S1 runtime role can create or transfer a fresh enterprise owner.
- Later S2/S3 roles are created and granted in their own waves; S1 does not create `issue_first_trial`.

### 10.8.1 Local immutability guard triggers

The following `BEFORE UPDATE` trigger functions are local structural controls, owned by the schema owner, `SECURITY INVOKER`, with no transaction control and no entry in the function-wave registry. They raise a stable SQL error on a forbidden change; they are not callable cross-schema service APIs.

| Trigger | Guard predicate |
| --- | --- |
| `customer_iam.guard_customer_account` | Reject changes to `id` or `created_at`. |
| `customer_iam.guard_auth_identity` | Reject changes to `id`, `account_id`, `provider` or `provider_scope`. Reject a change to `provider_subject_ciphertext` or `provider_subject_hmac` unless both change together with `key_version = OLD.key_version + 1`; that rotation path is a later controlled security operation (Q2) that must preserve the logical provider subject and is not a runtime service path. |
| `customer_iam.guard_identity_profile` | Reject changes to `id` or `account_id`. |
| `organization_access.guard_organization` | Reject changes to `id` or `created_at`. |
| `organization_access.guard_membership` | Reject changes to `id`, `organization_id` or `account_id`. |
| `operations_iam.guard_employee_account` | Reject changes to `id`, `login_name_hmac`, `auth_issuer` or `auth_subject_hmac`. |
| `subject_number.guard_subject_owner` | Reject changes to `id`, `source` or `created_at`; reject clearing or changing an owner that is already set; reject a real-name change when `current_user <> 'subject_identity_verification_role'`. |
| `subject_number.guard_caller_number_binding` | Reject changes to `id`, `subject_id`, `number_type`, `number_ciphertext`, `number_hmac` or `number_masked`. |
| `subject_number.guard_subject_risk_state` | Reject changes to `id`, `caller_number_binding_id` or `risk_type`. |

The append-only triggers `audit.reject_row_mutation` and `subject_number.reject_row_mutation` remain the only triggers on their tables. The guard triggers are installed in the same V002/V003 wave as their tables and are not counted as published functions. The migration grants `EXECUTE` on each guard trigger function only to its owning module runtime role(s) (`customer_iam_role`, `organization_access_role`, `operations_iam_role`, `subject_number_role` and `subject_identity_verification_role` as applicable); direct invocation is not a supported API and no cross-schema `EXECUTE` is granted.

### 10.9 Planned V006/S2 first-trial function (not authored in S1)

`entitlement_metering.issue_first_trial` is visibly planned in the frozen function-wave block as a V006/S2 function. It is not created, called, required or tested by S1. Its detailed contract remains the accepted plan's frozen handoff and must be authored in S2 after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance` exist. S1 actions must not depend on it.

### 10.10 S1 function verification requirements

The later executable suite must prove each S1 function's owner is NOLOGIN/NOSUPERUSER, `PUBLIC` has no EXECUTE, the fixed `search_path` cannot be shadowed, and no function contains transaction control. It must also prove same-key replay/conflict, finalize-once, committed-`NEW` alerting, same-transaction rollback, Outbox dedupe, audit append-only, protected-key rejection and cross-role denial. It must additionally prove each function caller has schema `USAGE` without `CREATE`, the declared cross-module `SELECT` columns are readable while all cross-module writes are denied, no cross-owner row lock is attempted, and the guard triggers reject rebinding while legitimate status/revision/time/key-version updates succeed. S1 records these as document requirements; no function was executed against PostgreSQL in this S1 authoring.

## 11. Linear Flyway Migration Order

### 11.1 Frozen nine-wave migration baseline

The following nine rows are the accepted migration order. They are verbatim from the accepted plan. S1 authors only V001-V003; V004-V009 remain planned S2/S3 handoff.

| V001 | `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `platform.idempotency_record`, `platform.outbox_event`, `audit.audit_fact` | Every later module needs idempotency begin/complete, Outbox, audit and the four platform/audit functions; no entitlement table or function is created here |
| V002 | `V002__identity_organization_and_employee.sql` | `customer_iam.customer_account`, `customer_iam.auth_identity`, `customer_iam.identity_profile`, `organization_access.organization`, `organization_access.organization_membership`, `operations_iam.employee_account` | Stable account, organization and employee identities are parents for subject, task, order and evidence facts |
| V003 | `V003__subject_number_and_claim.sql` | `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_claim`, `subject_number.subject_risk_state` | Activation, IVR, orders and evidence require a stable subject and number binding |
| V004 | `V004__catalog_order_and_payment.sql` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `catalog_order.customer_order`, `catalog_order.payment_order`, `catalog_order.payment_event` | Entitlement issuance consumes immutable order snapshots and verified payment events |
| V005 | `V005__ivr_task_session_call.sql` | `ivr.outbound_call_task`, `ivr.inbound_session`, `ivr.call_record`, `ivr.call_leg`, `ivr.call_event`, `ivr.ivr_decision` | Reservation and evidence require call/session/task identities; `call_record` is the canonical call parent |
| V006 | `V006__trial_entitlement_issuance_reservation_usage.sql` | `entitlement_metering.trial_policy_version`, `entitlement_metering.entitlement_grant`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_balance`, `entitlement_metering.entitlement_issuance`, `entitlement_metering.usage_reservation`, `entitlement_metering.usage_fact` | Issuance, reservation and usage have order, call and grant parents from earlier waves or the same wave; `entitlement_metering.issue_first_trial` is created here after its four tables exist |
| V007 | `V007__activation_and_consent.sql` | `activation_consent.consent_record`, `activation_consent.service_activation`, `activation_consent.service_activation_transition` | Consent/activation link to IVR sessions created in V005 and orders created in V004; activation remains separate from trial/reservation tables and calls their published service |
| V008 | `V008__evidence_media.sql` | `evidence.media_object`, `evidence.recording`, `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.evidence_event` | Evidence links call, reservation and server-validated media objects; creation-event ownership is enforced at commit |
| V009 | `V009__sharing.sql` | `sharing.evidence_share_grant`, `sharing.share_admission`, `sharing.share_access_event` | Sharing requires solidified evidence and appends access facts after all parents exist |

### 11.2 Frozen slice membership baseline

The exact slice membership blocks are verbatim from the accepted plan. S1 is 13 tables; S2 is 23 planned tables; S3 is 8 planned tables. The frozen membership is structural closure, not proof that S2/S3 dictionaries are authored.

<!-- slice-S1-tables:start -->
- `platform.idempotency_record`
- `platform.outbox_event`
- `audit.audit_fact`
- `customer_iam.customer_account`
- `customer_iam.auth_identity`
- `customer_iam.identity_profile`
- `organization_access.organization`
- `organization_access.organization_membership`
- `operations_iam.employee_account`
- `subject_number.business_subject`
- `subject_number.caller_number_binding`
- `subject_number.subject_claim`
- `subject_number.subject_risk_state`
<!-- slice-S1-tables:end -->

<!-- slice-S2-tables:start -->
- `catalog_order.catalog_product`
- `catalog_order.catalog_version`
- `catalog_order.catalog_offer`
- `catalog_order.catalog_benefit_line`
- `catalog_order.customer_order`
- `catalog_order.payment_order`
- `catalog_order.payment_event`
- `ivr.outbound_call_task`
- `ivr.inbound_session`
- `ivr.call_record`
- `ivr.call_leg`
- `ivr.call_event`
- `ivr.ivr_decision`
- `entitlement_metering.trial_policy_version`
- `entitlement_metering.entitlement_grant`
- `entitlement_metering.trial_grant`
- `entitlement_metering.entitlement_balance`
- `entitlement_metering.entitlement_issuance`
- `entitlement_metering.usage_reservation`
- `entitlement_metering.usage_fact`
- `activation_consent.consent_record`
- `activation_consent.service_activation`
- `activation_consent.service_activation_transition`
<!-- slice-S2-tables:end -->

<!-- slice-S3-tables:start -->
- `evidence.media_object`
- `evidence.recording`
- `evidence.evidence_record`
- `evidence.evidence_artifact`
- `evidence.evidence_event`
- `sharing.evidence_share_grant`
- `sharing.share_admission`
- `sharing.share_access_event`
<!-- slice-S3-tables:end -->

### 11.3 S1 V001-V003 intra-wave creation order

V001: platform schemas, roles and audit.

1. Create schemas `platform`, `audit`, `customer_iam`, `organization_access`, `operations_iam` and `subject_number`.
2. Create NOLOGIN, NOSUPERUSER owner/runtime roles from section 3.1 and `operations_service_role`; revoke `CREATE` on `public` and all business schemas from `PUBLIC`.
3. Create `platform.idempotency_record`, then `platform.outbox_event`, then `audit.audit_fact`, with their named checks and indexes.
4. Create the `audit.audit_fact` append-only trigger and function `audit.reject_row_mutation()`.
5. Create `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append` and `audit.append_fact` after their tables exist. Revoke all from `PUBLIC`; grant `EXECUTE` and the containing-schema `USAGE` (without `CREATE`) per section 10.8.
6. Grant platform/audit table/index/sequence privileges and function `EXECUTE` per section 10.8 for the objects created in V001. Grant the platform/audit schema `USAGE` to the exact function callers. Do not grant domain roles any platform/audit table privilege. Do not create `entitlement_metering.issue_first_trial`.

V002: identity, organization and employee tables.

1. Create `customer_iam.customer_account` → `customer_iam.auth_identity` → `customer_iam.identity_profile`.
2. Create `organization_access.organization` → `organization_access.organization_membership`.
3. Create `operations_iam.employee_account`.
4. Add the V002 ordinary foreign keys and indexes from sections 4.6-4.11 and 5.3.
5. Create and install the V002 guard triggers in section 10.8.1: `customer_iam.guard_customer_account`, `customer_iam.guard_auth_identity`, `customer_iam.guard_identity_profile`, `organization_access.guard_organization`, `organization_access.guard_membership` and `operations_iam.guard_employee_account`.
6. Apply the narrow owned-table column grants and revoke `DELETE`/`TRUNCATE`. Grant `organization_access` schema `USAGE` to `customer_iam_role` and `subject_number_role`; grant `customer_iam` schema `USAGE` to `subject_number_role`; grant the cross-module `SELECT` columns in section 10.8. Grant no cross-module write or row-lock privilege.

V003: subject, binding, claim and risk tables.

1. Create `subject_number.business_subject`.
2. Create `subject_number.caller_number_binding` with the FK to the subject.
3. Create `subject_number.subject_claim` with its FKs and indexes.
4. Create `subject_number.subject_risk_state` with its FK and indexes.
5. Create the `subject_claim` append-only trigger and function `subject_number.reject_row_mutation()`, and the V003 guard triggers `subject_number.guard_subject_owner`, `subject_number.guard_caller_number_binding` and `subject_number.guard_subject_risk_state`.
6. Apply the real-name column grants: `subject_number_role` receives no update on `real_name_status`/`real_name_verified_at`; `subject_identity_verification_role` receives `SELECT` on `id` plus the real-name/status columns and update only on those real-name columns plus revision/updated_at.
7. Grant `subject_identity_verification_role` `USAGE` on `subject_number` and `audit` and `EXECUTE` on `audit.append_fact`.
8. Apply the remaining S1 grants from section 10.8, including schema `USAGE` for `subject_number`. Do not grant `subject_number_role` `INSERT` on either `business_subject` owner column or `UPDATE` on `owner_organization_id`; subject creation inserts both owner columns null. No cross-slice table, function or prerequisite is created.

All S1 FK columns and constraints are created in the wave shown in section 5.3. No S1 wave has a forward reference. Flyway checksums are immutable; S1 expects no destructive rollback. Future changes use expand/contract.

### 11.4 S2/S3 migration reservation

V004-V009 and their intra-wave details, `entitlement_metering.issue_first_trial` in V006, the evidence composite deferred FK cycle in V008 and all sharing tables in V009 are not authored here. Their frozen wave rows and slice membership remain the accepted handoff. S1 does not require, create or test any S2/S3 object.

## 12. Deletion, Retention and Retention-Hook Policy

### 12.1 S1 lifecycle classes

| Class | S1 tables | Rule |
| --- | --- | --- |
| Mutable head | platform.idempotency_record, platform.outbox_event, customer_iam.customer_account, customer_iam.auth_identity, customer_iam.identity_profile, organization_access.organization, organization_access.organization_membership, operations_iam.employee_account, subject_number.business_subject, subject_number.caller_number_binding, subject_number.subject_risk_state | Status/terminal-time retirement and revision-controlled updates; no ordinary hard delete |
| Append-only or frozen | audit.audit_fact, subject_number.subject_claim | No update/delete privilege; append-only trigger; stable identity and hash where applicable |

Every S1 table has exactly one class. The all-44 classification remains the frozen baseline matrix; S2/S3 lifecycle details are authored in their own slices.

### 12.2 S1 deletion policy

- No S1 runtime role has `DELETE` or `TRUNCATE` on any business table. All FKs are `ON DELETE RESTRICT`; no cascade exists.
- `audit.audit_fact` and `subject_number.subject_claim` reject update/delete through table privileges and triggers. `platform.idempotency_record` and `platform.outbox_event` are never deleted by ordinary administration; their completed/published facts remain.
- Customer accounts, identities, profiles, organizations, memberships, employees, subjects, bindings and risk rows retire by status or terminal timestamp. A revoked membership or retired binding remains as history; a new row may be inserted where the partial unique permits.
- A subject is never deleted to remove an owner or claim. A claim result is never deleted to hide a conflict. An identity is never deleted to free a provider subject; it is disabled and retained, subject to the later legal policy.
- No S1 table has a retention-period default, erasure procedure, backup-deletion procedure or legal-hold column. Those are legal/security/operations inputs (Q3, Q13, Q16), not schema guesses.

### 12.3 Retention hooks

- Retention hooks are not implemented in S1. Any future retention or erasure capability requires an approved policy, a controlled function or migration, audit evidence and controller approval; it must not bypass append-only controls by direct table deletion.
- The only accepted future deletion candidate in the 44-table baseline is a never-published, unreferenced `catalog_order.catalog_version` draft. It is an S2 object and is not implemented or authorized by S1.
- `platform.idempotency_record` and `platform.outbox_event` retention is an operations input (Q13); a committed `NEW` idempotency row is an alert and reconciliation input, not a deletion trigger.
- Legal retention, user erasure, backup deletion and audit retention remain Q3. No period, duration or erasability claim is made here.

### 12.4 Immutability controls summary

- `audit.audit_fact`: revoked write privileges, no update/delete function, append-only trigger.
- `subject_number.subject_claim`: insert-only grant, no update/delete, append-only trigger; replay identity is compared from the decrypted immutable verification reference.
- `platform.idempotency_record`: completed result columns immutable; only the single finalization update is allowed; no delete.
- `platform.outbox_event`: payload and event identity immutable; dispatcher column grants only; no delete.
- Identity relation: `auth_identity.account_id`/provider tuple and `identity_profile.account_id` are immutable under narrow `UPDATE` grants and guard triggers.
- Organization membership: `organization_id`/`account_id` immutable; a revoked membership is replaced by a new row; the lifecycle advisory key is the logical pair, so revoke/re-grant serializes with all pair readers.
- Caller-number binding: `subject_id` and number identity immutable; replacement creates a new row and retires the old row; a create never revives a `DISABLED` row.
- Subject owner: set-once from null under `subject_number.guard_subject_owner`; S1 writes only `owner_account_id` through a verified personal claim; no transfer, no enterprise ownership guess.
- Subject real-name: separate verification role with `SELECT` on `id`; claim runtime role has no update privilege and the guard rejects other writers.
- Membership revocation and binding retirement are terminal for the row and are never overwritten.

## 13. Deterministic Validation Commands

### 13.1 Document-level S1 checks

Run from the repository root. These are documentation checks only; they do not execute PostgreSQL or migrations. `FILE` is the S1 specification.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
test -s "$FILE"
for section in "Scope, Status and Source Relationship" "Database Conventions" "Module and Schema Ownership Matrix" "Complete Table Dictionary" "Foreign-Key Catalog" "Invariant Matrix" "State Machines" "Sensitive-Field Inventory" "Transaction Sequences" "Cross-Schema Function Contracts" "Linear Flyway Migration Order" "Deletion, Retention and Retention-Hook Policy" "Deterministic Validation Commands" "Open Production Inputs" "Goal-Alignment and No-Over-Design Check" "Completion Report Template"; do
  grep -q -E "^## [0-9]+\. $section$" "$FILE" || { echo "MISSING SECTION: $section"; exit 1; }
done
test "$(grep -c -E '^### 4\.[0-9]+ [a-z_]+\.[a-z_]+$' "$FILE")" -eq 13 || { echo "S1 DICTIONARY HEADING COUNT FAILED"; exit 1; }
test "$(grep -c -E '^### 10\.[3-6] (platform|audit)\.[a-z_]+$' "$FILE")" -eq 4 || { echo "S1 FUNCTION CONTRACT COUNT FAILED"; exit 1; }
test "$(grep -c -E '^\| INV-[0-9][0-9] \(S1' "$FILE")" -eq 14 || { echo "S1 INVARIANT EXPANSION COUNT FAILED"; exit 1; }
test "$(awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && NF' "$FILE" | wc -l)" -eq 5 || { echo "FUNCTION WAVE COUNT FAILED"; exit 1; }
test "$(awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && /^platform\.|^audit\./ && /[|]V001[|]/' "$FILE" | wc -l)" -eq 4 || { echo "V001 FUNCTION COUNT FAILED"; exit 1; }
# The frozen structural script in 13.3 additionally rejects the legacy
# audit-schema underscore token, the stale FK-mode token and stale baseline
# counts. Those forbidden strings are intentionally not repeated here.
! grep -q -E '4[3] tables|3[5] invariants' "$FILE" || { echo "STALE BASELINE COUNT"; exit 1; }
! grep -q -E 'S2 dep[ei]nds on S3|S2 requ[i]res S3|S1 dep[ei]nds on S[23]|S1 requ[i]res S[23]' "$FILE" || { echo "LATER-SLICE PREREQUISITE"; exit 1; }
test -z "$(grep -n 'section 10\.4' "$FILE")" || { echo "STALE DENYLIST NAVIGATION"; exit 1; }
test -z "$(grep -n 'section 10\.5' "$FILE")" || { echo "STALE FUNCTION-GRANT NAVIGATION"; exit 1; }
test -z "$(grep -n 'organization_access\.membership:<membership_id>' "$FILE")" || { echo "STALE MEMBERSHIP-ID LOCK KEY"; exit 1; }
test -z "$(grep -n 'docs/12 section 8\.3' "$FILE")" || { echo "STALE OWNERSHIP CITATION"; exit 1; }
for term in CLAIM_REPLAY_CONFLICT CLAIM_CONTEXT_CONFLICT CLAIM_ORGANIZATION_OWNERSHIP_UNAPPROVED CLAIM_SUBJECT_NOT_FOUND CLAIM_ACCOUNT_NOT_FOUND CLAIM_ORGANIZATION_NOT_FOUND CLAIM_CONCURRENCY_EXHAUSTED CLAIM_DATA_INTEGRITY BINDING_DISABLED_CONFLICT 'pg_catalog.gen_random_uuid' 'subject_number.subject:' 'customer_iam.account:' 'organization_access.membership:' 'docs/12 section 7.2'; do
  grep -q -F "$term" "$FILE" || { echo "MISSING FIX CONTRACT TERM: $term"; exit 1; }
done
test -z "$(grep -n -E '[[:blank:]]+$' "$FILE")" || { echo "TRAILING WHITESPACE"; exit 1; }
```

### 13.2 S1 per-table dictionary coverage

This `awk` program verifies that each of the 13 S1 dictionary sections exists once and contains the exact accepted column count. It is a document check, not a PostgreSQL check.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
awk '
BEGIN {
  need["platform.idempotency_record"]=12; need["platform.outbox_event"]=14; need["audit.audit_fact"]=12;
  need["customer_iam.customer_account"]=6; need["customer_iam.auth_identity"]=10; need["customer_iam.identity_profile"]=9;
  need["organization_access.organization"]=6; need["organization_access.organization_membership"]=7;
  need["operations_iam.employee_account"]=9; need["subject_number.business_subject"]=9;
  need["subject_number.caller_number_binding"]=9; need["subject_number.subject_claim"]=10; need["subject_number.subject_risk_state"]=8;
}
/^## / { t=""; next }
/^### / {
  if ($0 ~ /^### 4\.[0-9]+ [a-z_]+\.[a-z_]+$/) { t=$3; seen[t]++ } else { t="" }
  next
}
t!="" && /^\| [a-z0-9_]+ \| (uuid|text|timestamptz|integer|jsonb) \|/ { c[t]++ }
END {
  bad=0
  for (k in need) {
    if (seen[k]!=1) { print "DICTIONARY COUNT " k " " seen[k]+0; bad=1 }
    if (c[k]!=need[k]) { print "COLUMN COUNT " k " " c[k]+0 " != " need[k]; bad=1 }
  }
  exit bad
}
' "$FILE"
```

Expected: exit 0 with no output. The accepted column totals are 121 S1 column rows.

Exact accepted-column-name comparison (catalog field 6 versus dictionary first column) for all 13 S1 tables:

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
ok=1
for t in platform.idempotency_record platform.outbox_event audit.audit_fact customer_iam.customer_account customer_iam.auth_identity customer_iam.identity_profile organization_access.organization organization_access.organization_membership operations_iam.employee_account subject_number.business_subject subject_number.caller_number_binding subject_number.subject_claim subject_number.subject_risk_state; do
  catcols=$(awk -F'|' -v t="$t" '{x=$2; gsub(/[ `]/,"",x); if (x==t) print $6}' "$FILE" | grep -o '`[a-z0-9_]*`' | tr -d '`' | sort -u)
  dictcols=$(awk -v t="$t" '
    $0 ~ "^### 4\\.[0-9]+ " t "$" {f=1; next}
    /^### / {f=0}
    f && /^\| [a-z0-9_]+ \| (uuid|text|timestamptz|integer|jsonb) \|/ {
      n=split($0,a,"|"); gsub(/ /,"",a[2]); print a[2]
    }' "$FILE" | sort -u)
  if [ "$catcols" = "$dictcols" ]; then
    printf '%s exact %s columns\n' "$t" "$(printf '%s\n' "$dictcols" | wc -l)"
  else
    echo "$t MISMATCH"; ok=0
  fi
done
test "$ok" -eq 1 && echo ALL_EXACT
```

Expected: 13 `exact` lines and `ALL_EXACT`. This proves that no accepted S1 column was dropped, renamed or replaced.

### 13.3 Frozen structural-script-v2 run

Extract the exact accepted structural script and run it unchanged against this document with `FILE` and `FIX2` explicitly set. `FIX2` is the preserved fix-6 artifact; it is a file-presence gate for the frozen baseline, not an S1 content dependency.

```bash
awk '/^# structural-script-v2$/{f=1} f{print} /^# end structural-script-v2$/{exit}' docs/gateflow/a0-production-data-model-schema-spec-plan.md > /tmp/a0-structural-v2.sh
FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0-structural-v2.sh
```

Expected: exit 0; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`. This proves the frozen 44-table scaffold only. It does not prove S2/S3 dictionaries or production behavior.

### 13.4 Scope, hash and cleanliness checks

```bash
sha256sum docs/gateflow/a0-production-data-model-schema-spec-plan.md
# expected: 149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5
sha256sum docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md
# expected: 8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59
git status --porcelain=v1 | grep -E '\.sql$|db/migration/' && exit 1 || true
# New artifacts are untracked, so a global git diff --check does not inspect them.
# The targeted blank-at-EOL scan below is the equivalent whitespace gate.
test -z "$(grep -n -E '[[:blank:]]+$' docs/14-production-data-model-and-flyway-schema-spec.md docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md docs/gateflow/a0-production-data-model-schema-spec-s1-fix-1.md)"
```

Expected: plan and fix-6 hashes unchanged; no `.sql` or `db/migration` path in `git status`; zero trailing-whitespace lines in the current specification, the historical implementation artifact and the fix-1 artifact. A global `git diff --check` is intentionally not used as the S1 gate because the pre-existing, out-of-scope `logo/preview.html` change contains trailing whitespace; the targeted scan is authoritative for the new artifacts. Pre-existing modified QA screenshots, `logo/preview.html` and the unrelated `docs/reviews/plan-review-20261021-205118.md` are outside this work unit and must remain untouched.

### 13.5 S1 control-coverage assertions

These checks prove that every S1 dictionary names its lifecycle, sensitive representation, revision/immutability or append-only control, and that the S1 FK detail has exactly 11 rows. They are document checks only.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
test "$(sed -n '/^### 4.3 /,/^## 5\./p' "$FILE" | grep -c '^Purpose:.*Lifecycle:')" -eq 13 || { echo "S1 LIFECYCLE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.3 /,/^## 5\./p' "$FILE" | grep -c '^Sensitive representation:')" -eq 13 || { echo "S1 SENSITIVE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.3 /,/^## 5\./p' "$FILE" | grep -c -E '^(Revision/immutability|Append-only control):')" -eq 13 || { echo "S1 REVISION/IMMUTABILITY COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 5.3 S1 foreign-key detail/,/^## 6\./p' "$FILE" | grep -c '| FK |')" -eq 11 || { echo "S1 FK DETAIL COUNT FAILED"; exit 1; }
test "$(sed -n '/^### 8.2 S1 sensitive-field detail/,/^### 8.3/p' "$FILE" | grep -c '^| ')" -ge 20 || { echo "S1 SENSITIVE DETAIL FAILED"; exit 1; }
```

Expected: exit 0. The exact S1 FK count is 11; the S1 sensitive detail covers every S1 table that stores a protected value and explicitly marks the others as non-sensitive.

### 13.6 Honest limitations

S1 ran only document checks and shell/awk validation. No PostgreSQL instance, Flyway migration, constraint, function, privilege, concurrency test or Testcontainers suite was executed. A green structural script proves document structure and frozen membership only; it does not prove S2/S3 detailed dictionaries or any production behavior.

## 14. Open Production Inputs

The following 28 open inputs are copied from the accepted plan. They are not guessed, defaulted or silently converted into schema values. S1-relevant inputs are Q1, Q2, Q3, Q9, Q13, Q14, Q16, Q17, Q19, Q23 and Q24; the remaining inputs retain their frozen owners/destinations for later slices.

| ID | Unresolved input | Why it is not guessed | Owner | Destination |
| --- | --- | --- | --- | --- |
| Q1 | Employee authentication source, MFA policy and credential rotation | Security architecture is a later design gate; business tables only store issuer/subject references | Security/Backend | Security design and operations IAM implementation |
| Q2 | Envelope-encryption/KMS products, key hierarchy and rotation | Provider selection is deferred; schema stores ciphertext, key reference and HMAC index only | Security/Infrastructure | Security design and COS/KMS validation |
| Q3 | Legal retention, erasure rights, backup deletion and audit retention | Legal approval is not available | Legal/Security | Retention and deletion policy; production launch review |
| Q4 | Production prices, entitlement quantities, seat counts, trial wording and catalog approval | Demo values are explicitly not approved | Product/Operations/Finance | Catalog approval and Phase C configuration |
| Q5 | Renewal, stacking, offset order, auto-renew, refund and tax/invoice behavior | Current baseline keeps these offline or future | Product/Finance | Phase C business rules; offline support process |
| Q6 | WeChat payment merchant/app identifiers, payload fields and callback retention | Vendor integration is not yet contracted into the schema | Payments/Backend | Payment adapter and OpenAPI/security design |
| Q7 | SIP line/access number, callee display, recording format/size, buffer and time synchronization | Real IVR platform and line integration are pending | Telephony/IVR | IVR protocol and line integration |
| Q8 | Authorized-versus-reported duration tolerance and IVR SLO/timeouts | Requires real-line measurement and acceptance policy | IVR/Backend/QA | IVR protocol and capacity/acceptance work |
| Q9 | Number recycling and conflict-resolution rules | Current design only detects risk; automatic transfer is forbidden | Product/Risk/Operations | Claim-risk policy and operations runbook |
| Q10 | Recording notice/consent wording and prompt-version lifecycle | Legal/product approval is pending | Product/Legal | Consent policy and IVR prompt approval |
| Q11 | COS object-lock/WORM capability, storage class and media retention | Cloud capability and legal retention are unapproved | Infrastructure/Security | Storage/security/capacity work |
| Q12 | Share-code KDF parameters, admission limits, expiry defaults and rate-limit thresholds | No approved production policy; schema stores the facts but no defaults | Security/Product | Security design and sharing policy |
| Q13 | Audit/Outbox retention, DLQ thresholds and replay operations | Operational policy is deferred | Operations/Backend | Operations runbook and observability design |
| Q14 | Subject real-name verification provider, evidence and re-verification policy | Real-name source is not contracted; schema only stores subject-level status and protected references | Security/Product | Verification adapter and identity design |
| Q15 | Exact event-domain wire values and formats for `ACTIVATION`, `IVR_CONFIRM`, `IVR_EVENT`, `IVR_DIRECT_AUTH`, `EVIDENCE`, `PAYMENT`, `SHARE_ACCESS` and other domains | Domain names are fixed by business rules, but wire format and validation rules are API/security decisions | Backend/Security | IVR protocol and OpenAPI/security design |
| Q16 | Task idempotency-key retention, replacement audit detail and abuse limits | No approved production policy; baseline stores the fact and unique key but no retention default | Product/Backend | IVR protocol and operations policy |
| Q17 | Flyway role provisioning, schema ownership transfer and least-privilege grant execution | Deployment identity and database ownership are environment-specific | Platform/Database | Deployment/security design and production readiness |
| Q18 | Production configuration key/approval for the configurable pending-task TTL, config versioning/audit and scheduler cadence | The plan states the source default of 30 seconds as the baseline default and freezes it per task; the production configuration key, approval and scheduler SLO are pending and are not invented here | Product/Backend | IVR protocol and operations configuration |
| Q19 | WeChat appid/openid/unionid mapping and multi-app identity association | The source requires clarification, but account merge and login-identity migration are explicit non-goals | Identity/Backend | Authentication design and security review |
| Q20 | Installment due-date trigger, grace/retry policy and scheduler cadence for future `entitlement_issuance` rows | The baseline creates the durable slot and enforces uniqueness, but the source does not approve a due-date or grace rule | Product/Finance/Backend | Entitlement issuance design and Phase C configuration |
| Q21 | Reservation reconciliation scan cadence, alert thresholds and manual adjustment approval | Operational policy is deferred; the baseline defines states and never releases unknown use | Operations/Backend/IVR | Operations runbook and reconciliation design |
| Q22 | Redis playback-ticket TTL, clock-skew allowance and revocation propagation window | Security/product policy is not approved; the baseline derives the key and never makes Redis authoritative | Security/Sharing | Security design and sharing policy |
| Q23 | Function/role provisioning order and whether the deployment role can create SECURITY DEFINER functions | Environment-specific database ownership; the baseline specifies the contract but not the deployment identity | Platform/Database/Security | Deployment/security design and production readiness |
| Q24 | Any call-blocking subject-risk rule beyond the approved `OPEN` blocks on number claims and historical-evidence access | The approved policy does not ban future dialing for claim-only risk; inventing a blanket ban would contradict `docs/10` | Product/Risk/IVR | Claim-risk and IVR policy; launch-readiness review |
| Q25 | Exact `authorizationExpiresAt` window, clock-skew allowance and configurable value | The source requires a monitored clock and a window covering the call plus skew, but no production value is approved | IVR/Entitlement/Backend | IVR protocol, configuration and capacity/acceptance work |
| Q26 | Typed snapshot JSON-schema version and IVR wire mapping for the stable reason/taskMatch enums | The field contract is fixed here, but wire encoding and version negotiation are API/security decisions | IVR/Backend/Security | IVR protocol and OpenAPI/security design |
| Q27 | Exact organization role(s) that hold the task-create/claim-revalidation permission | The plan makes organization/creator/owner account and membership lifecycle status mandatory predicates; only the approved role-to-permission matrix remains a security/authorization decision | Organization-access/Security | Authorization design and S2 policy tests |
| Q28 | Durable IVR-local B-leg operation key, persistence protocol and lost-response recovery | The business `ivr.call_leg` table records callbacks/facts and does not prove local outbound uniqueness; the authoritative unique operation belongs to the existing IVR platform, whose executable key/protocol is a later IVR implementation decision | Telephony/IVR | IVR protocol and line integration; no business schema expansion |

No unresolved input is filled with a demo value, a guessed retention period or a provider-specific identifier. Fields may exist as nullable production inputs only where the S1 state machine explicitly permits them.

The enterprise claim/ownership rule is **not** added as a new open-input row; the frozen 28-row block is unchanged. docs/11 section 2 and section 6 defer organizational ownership/transfer policy to the future business-subject service, and docs/12 section 7.2 (line 265) requires refusing ambiguous ownership. S1 therefore preserves the approved verified-personal claim of an unowned IVR subject and fails closed for a fresh organization-scoped claim (`CLAIM_ORGANIZATION_OWNERSHIP_UNAPPROVED`) without selecting personal versus organization ownership from an optional workspace ID. Owner: Product/Subject-number; destination: enterprise claim/ownership policy and its S1 follow-up.

## 15. Goal-Alignment and No-Over-Design Check

### 15.1 Goal alignment

| Confirmed goal area | S1 evidence |
| --- | --- |
| Account/login identity | customer_account, auth_identity, identity_profile; login only through active identity + active account |
| Organization membership | organization and organization_membership; workspace access derived from active membership; no independent login |
| Employee identity | employee_account in a separate issuer domain; last-admin guard |
| Business subject/number binding | business_subject, caller_number_binding; HMAC lookup; owner exclusivity; no login inference |
| Consent/activation | Not authored in S1; S1 provides the identity/subject/binding contracts consumed by S2 |
| IVR/trial/entitlement/calls | Not authored in S1; S1 provides subject/binding/risk and platform controls consumed by S2 |
| Commerce/payment/issuance | Not authored in S1; S2 owns these tables |
| Evidence/media/sharing | Not authored in S1; S3 owns these tables |
| Idempotency/Outbox/audit | Both platform tables and audit table, four V001 functions, S1 privilege/atomicity/replay contracts |
| Sensitive fields | Enveloped ciphertext, HMAC, masked and redacted representations; function denylist |
| Deletion policy | No delete/cascade; status retirement; append-only controls; retention inputs assigned |
| Migration order | Nine frozen waves; V001-V003 authored; no forward reference |

### 15.2 No-over-design check

- S1 adds no table, function contract, domain or slice beyond the accepted baseline. The only S1 tables are the exact 13 V001-V003 tables.
- S1 does not author S2/S3 dictionaries, transactions, state machines or tests. The frozen 44-table catalog, matrix, FK/key/sensitive/function-wave and wave/slice blocks are copied for closure, not claimed as implementation.
- S1 does not add a generic workflow engine, event-sourcing layer, microservice split, provider configuration, capacity value, retention period, credential or cost estimate.
- S1 uses one local PostgreSQL transaction and four small cross-schema functions. `issue_first_trial` remains a visibly planned V006/S2 function and is not pulled into S1.
- Claim replay uses the existing encrypted `verification_ref_ciphertext`; no new column, request-fingerprint column or platform idempotency scope is added.
- Cross-module lifecycle serialization uses existing transaction advisory locks, including the stable logical membership pair key; no new table, cross-module DML grant, column grant, ID-discovery loop or published database function is added. The guard trigger functions in section 10.8.1 are local structural controls, not registry functions.
- The subject real-name column grants are a minimal DB control for the accepted independence rule; they add no table or function. Removing the owner-column INSERT and `owner_organization_id` UPDATE privileges narrows an unsupported write path without adding scope.
- A fresh organization-scoped claim fails closed; the deferred enterprise ownership/transfer policy is not guessed, and the exact role-to-claim permission remains with Product/Subject-number and Organization-access rather than being narrowed by S1.
- Every S1 table has one owner and write path. Every S1 invariant portion has one owner and an exact enforcement location.
- Unresolved inputs are assigned to owners/destinations in section 14; no policy is invented to make a check green.

## 16. Completion Report Template

The S1 author uses this template in `docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md`. It is not a claim of independent review acceptance.

- Task label, provider and gate
- Changed artifact paths
- S1 tables, invariants and functions completed
- Frozen baseline counts and membership preserved
- Exact validation commands, exits and outputs
- Docs decision
- Residual risks classified as fixed-current, later-approved-slice or later-work-unit, with owner and destination
- Uncovered areas and explicit limitations
- Completion or blocked statement
- Explicit statement that no independent review, later slice, Git stage/commit/push/PR/merge or production test was performed
