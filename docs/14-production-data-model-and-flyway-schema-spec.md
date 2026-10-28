# A0 Production Data Model and Initial Flyway Schema Specification

- Task label: `a0-spec-pr1-fix1-ds-20261023-01`; historical labels remain `a0-spec-aggregate-fix2-ds-20261023-01`, `a0-spec-aggregate-fix1-ds-20261022-01`, `a0-spec-s1-impl-ds-20261022-01`, `a0-spec-s1-fix1-ds-20261022-01`, `a0-spec-s1-fix2-ds-20261022-01`, `a0-spec-s1-eof-fix-ds-20261022-01`, `a0-spec-s2-implementation-ds-20261022-01`, `a0-spec-s2-fix1-ds-20261022-01`, `a0-spec-s2-fix2-ds-20261022-01`, `a0-spec-s2-fix3-ds-20261022-01`, `a0-spec-s2-fix4-ds-20261022-01`, `a0-spec-s3-impl-ds-20261022-01`, `a0-spec-s3-fix1-ds-20261022-01` and `a0-spec-s3-fix2-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `PR review fix` (bounded PR fix 1), cumulative S1-S3 specification. Slice S1 (V001-V003) is accepted at checkpoint `5404d9caa73b72c412ca2065bef19dfb46d154d3`; slice S2 (V004-V007) is accepted at checkpoint `8934df90c15dcd8173486c6ad753ab27c91ac765`; slice S3 (V008-V009) is accepted at checkpoint `9ad33e8727596366c0a844b7dffcce3eb028891b`. Aggregate deepreview `docs/reviews/code-review-20261023-002432.md` is accepted at checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`; aggregate fix 1 corrected the earlier 11 accepted findings, and aggregate fix 2 corrected the bounded F-01/F-02 access-identity residual and the three replay/digest clarifications. This PR fix 1 corrects the five accepted PR-review findings and the accepted ticket-expiry clarification without changing the accepted data-model shape. PR lifecycle and current gate state are authoritative only in `docs/gateflow/project-execution-control.md`; aggregate or runtime acceptance beyond the recorded checkpoint is not claimed.
- Status: S1 accepted at commit `5404d9caa73b72c412ca2065bef19dfb46d154d3` with specification SHA256 `B7FE164B701F8813A75212C4D8EC4624A97867FB7289EA652F3FE2283E510099`; independent semantic review `docs/reviews/code-review-20261022-191047.md` and EOF-only focused review `docs/reviews/code-review-20261022-191848.md` passed. S2 accepted at commit `8934df90c15dcd8173486c6ad753ab27c91ac765` with specification SHA256 `BDFE41EF0B0A278EC196C2E93926BDD1666A8D986135E14DE3099B9491CC72BF` (643834 bytes / 3982 lines); independent review `docs/reviews/code-review-20261022-215322.md` passed all material S2 contracts. S3 is accepted at commit `9ad33e8727596366c0a844b7dffcce3eb028891b` with specification SHA256 `CACE2720EF166C0EBBFCA0B95EEED1392CB356B5A81F2B010AEB2CA42750D4D6`; independent S3 reviews `docs/reviews/code-review-20261022-232555.md` and the S3 review chain passed, closing F4-01 by the accepted status integration and T-S2-65. Aggregate fix 1 corrected F-01..F-11 and the four accepted clarifications; aggregate fix 2 corrected the remaining bounded access-identity and replay clarifications and is accepted at checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`; this PR fix 1 corrects PR-01..PR-05 and OQ-PR-01. The durable specification records that checkpoint and does not embed a volatile next-gate claim; PR lifecycle and current gate state are authoritative only in `docs/gateflow/project-execution-control.md`. Runtime readiness and production readiness are not claimed. No PostgreSQL, Flyway, privilege, lock, savepoint, COS or Redis execution is claimed.
- Date: 2026-10-23
- Accepted plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`, SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`
- Accepted plan review: `docs/reviews/plan-review-20261022-174730.md`, SHA256 `C2432DA173BB4CB61D5554B1A2BF77C7DC5FA4AE79CE5CED1118741BBA34FDCA`
- Plan fix-6 input: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`, SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`
- S1 reviews: `docs/reviews/code-review-20261022-181806.md` (initial fail), `docs/reviews/code-review-20261022-184720.md` (F-10/F-11/F-12), accepted `docs/reviews/code-review-20261022-191047.md` (SHA256 `5F3068271C48DA54DABB26D8942CA5C26E06658A98F4945DF91C17F347C7103A`) and EOF-only `docs/reviews/code-review-20261022-191848.md` (SHA256 `B30410B5ECBC181FD1581C3DCC375110203E551E17A6BC408C2C1D6D47F29E6F`)
- S2 review and fix inputs: `docs/reviews/code-review-20261022-194702.md` (F-01..F-15, OQ-1..OQ-4), re-review `docs/reviews/code-review-20261022-202738.md` (R-01..R-08 and OQ-1..OQ-8), fix-2 re-review `docs/reviews/code-review-20261022-205538.md` (purchase-consent identity finding and Open Questions 1-5 plus C-01/C-02), fix-3 re-review `docs/reviews/code-review-20261022-212225.md` (F3-01 and OQ-F3-1/OQ-F3-2), controller sections `S2 independent review result and fix 1 adjudication`, `S2 fix 1 independent re-review and fix 2 adjudication`, `S2 fix 2 independent re-review and fix 3 adjudication` and `S2 fix 3 independent re-review and fix 4 adjudication` in `docs/gateflow/project-execution-control.md` (all adjudicated fixes/OQ outcomes are binding), and the fix records `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-1.md`, `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-2.md`, `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-3.md` and `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-4.md`
- Historical S1 fix artifacts: `docs/gateflow/a0-production-data-model-schema-spec-s1-fix-1.md`, `docs/gateflow/a0-production-data-model-schema-spec-s1-fix-2.md`, `docs/gateflow/a0-production-data-model-schema-spec-s1-eof-fix.md`, and `docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md`; all remain immutable
- Controller: `docs/gateflow/project-execution-control.md` (the sole authority for PR lifecycle and current gate state); accepted S1 checkpoint `5404d9c`; accepted S2 checkpoint `8934df9`; accepted S3 checkpoint `9ad33e8`; accepted aggregate deepreview checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`; aggregate fix 1 task label `a0-spec-aggregate-fix1-ds-20261022-01`; aggregate fix 2 task label `a0-spec-aggregate-fix2-ds-20261023-01`; PR fix 1 task label `a0-spec-pr1-fix1-ds-20261023-01`
- Prompt/task provider: `ds` / DeepSeek-V4.1-Flash
- Deliverable class: documentation only. No executable Flyway migration, DDL file, Maven/Spring project, service code, API, UI, cloud resource, production data or credential is produced or claimed.

## Status, freezing and slice discipline

The machine-readable table catalog, baseline matrix, sensitive inventory, foreign-key catalog, candidate-key catalog, function-wave block, the nine exact wave rows and the exact S1/S2/S3 slice membership blocks are copied verbatim from the accepted plan as **FROZEN PLANNED BASELINE**. They preserve global closure and are not evidence that S2 or S3 dictionaries, transactions or tests are complete.

S1 is accepted and preserved. The following are authored as completed S1 content in this document:

- the 13 V001-V003 table dictionaries, including every accepted column, type, nullability, default, key, check, index, foreign key, revision/immutability rule, lifecycle/deletion rule and sensitive representation;
- the S1 enforcement expansion for INV-01 through INV-07, INV-27 through INV-31, INV-33 and the S1 portion of INV-40;
- the four V001/S1 cross-schema function contracts: `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append` and `audit.append_fact`;
- S1 privilege, isolation, conditional-update, state and transaction contracts and their deterministic document checks.

S2 is accepted at checkpoint `8934df90c15dcd8173486c6ad753ab27c91ac765`. The authored S2 content is:

- the 23 V004-V007 table dictionaries, including every accepted column, type, nullability, default, key, check, index, foreign key, revision/immutability rule, lifecycle/deletion rule and sensitive representation;
- the 22 S2 invariant enforcement portions, S2 state machines, transaction sequences, published application-service contracts, typed decision snapshot, outcome classification and behavior test matrix;
- the one V006 SQL function contract `entitlement_metering.issue_first_trial`, created only in V006 after its four dependency tables exist.

S3 is authored in this document and is accepted at checkpoint `9ad33e8727596366c0a844b7dffcce3eb028891b`; this specification, including the bounded PR fix 1, preserves its accepted contracts without changing the accepted S3 semantics. The authored S3 content is:

- the eight V008-V009 table dictionaries, including every accepted column, type, nullability, default, named key/check/index/foreign key, revision/immutability rule, lifecycle/deletion rule and sensitive representation;
- the five S3 invariant portions INV-15, INV-24, INV-25, INV-26 and INV-39, the evidence/recording/share state machines, the S3 transaction sequences, owner service contracts, grant/guard/constraint-trigger security contracts and the S3 behavior test matrix;
- the reciprocal evidence creation cycle, including the composite `(source_event_domain, source_event_id)` deferred FK, the reverse deferred `evidence_id` FK, both `DEFERRABLE INITIALLY DEFERRED` constraints added after both V008 tables exist, the partial unique creation-event key and the two commit-time constraint triggers;
- the sharing capability-token, verification, admission-count, generation, expiry/rotation/revocation/lock/extension and derived playback-ticket contracts, including durable ticket/access events and PostgreSQL-commit-before-Redis ordering;
- the final cross-slice ownership, lock-order, lifecycle, sensitive-field, FK-order, migration and consistency closure.

`entitlement_metering.issue_first_trial` is authored in V006/S2, not a planned-only V001/S1 function. The function-wave block remains exactly five rows: four V001 functions plus the one V006 function. The S2 application services, including the owner-service collaborators added by fix 1, are application services on one connection and one transaction and add no function-wave row. S3 adds no SQL function and no function-wave row; its creation-cycle enforcement is a pair of owner-local deferred constraint triggers, not a callable cross-schema function. The frozen structural blocks are baseline closure, not acceptance evidence for any slice.

Baseline counts remain: 44 tables; 40 invariants; 9 migration waves; 3 slices with membership 13/23/8; 28 mutable-head and 16 append-only/frozen tables; 81 FK decisions = 74 ordinary `FK` + 3 late-added `ALTER_FK` + 2 truly `DEFERRABLE_FK` + 2 `NO_FK`; 24 key sets; 5 function-wave rows = 4 V001 functions + 1 V006 function.

## 1. Scope, Status and Source Relationship

### 1.1 Purpose

This document is the initial PostgreSQL 16 data-model and Flyway-schema specification for the first production baseline. S1 (V001-V003) is accepted. S2 (V004-V007) is accepted. S3 (V008-V009) is accepted at checkpoint `9ad33e8727596366c0a844b7dffcce3eb028891b`; aggregate deepreview is accepted at checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`. PR lifecycle and current gate state are authoritative only in `docs/gateflow/project-execution-control.md`. The document is cumulative: a later implementation work unit can generate V001-V009 migrations and module code without inventing S1, S2 or S3 business rules.

S1 covers platform idempotency, transactional Outbox, append-only audit, natural-person customer accounts, login identities, account-level real-name profiles, organizations, organization memberships, operations employee identities, business subjects, business-number bindings, subject claims and claim-risk state.

S2 covers the immutable catalog/version/offer/benefit snapshot, customer order with `PURCHASE` idempotency, verified payment order/events, IVR pending tasks, inbound sessions, the canonical call parent, legs, IVR events, frozen IVR decisions, the first-trial policy/grant and permanent `FIRST-TRIAL` uniqueness, generic entitlement grants/balances, durable per-line/installment/type issuance state, usage reservation/settlement/reconciliation, explicit consent records, service activation and its transition ledger. S2 owns the one new V006 `entitlement_metering.issue_first_trial` SQL contract and the S2 application services.

S3 covers private media-object identity/version/hash metadata, recording upload/completion/validation/quarantine, the immutable evidence record with reciprocal `EVIDENCE_CREATED` ownership, original and derived evidence artifacts, evidence events, share grants with capability-token/code/recipient verification, durable admissions with count-once semantics, and append-only share access events from which short-lived Redis playback tickets are derived. S3 owns no SQL function and no platform idempotency scope; its only cross-row enforcement beyond ordinary keys/FKs is the owner-local deferred evidence creation-cycle trigger pair.

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

### 1.4 S1 non-goals (historical) and S2 non-goals

S1 does not create or specify the S2/S3 table dictionaries, IVR task/session/call tables, consent/activation tables, catalog/order/payment tables, entitlement/issuance/reservation/usage tables, evidence tables, sharing tables or `entitlement_metering.issue_first_trial`. It does not create executable SQL, a Maven project, API/UI code, cloud assets, credentials, production data, retention periods or provider-specific policy. It does not change the 44/40/9/3 baseline or authorize a fourth slice.

S2 does not author the S3 evidence/media/sharing dictionaries, their transactions or the evidence composite deferred FK cycle; it preserves those eight tables as frozen planned membership only. S2 adds no table, SQL function beyond the one accepted V006 `issue_first_trial`, eighth IVR action, fourth slice, executable migration, production service/API/UI, cloud resource, credential, guessed retention/capacity/policy value or provider configuration. The four V001 contracts remain S1-owned and unchanged.

S3 adds no table, column, SQL function, function-wave row, eighth IVR action, fourth slice, executable migration, production service/API/UI, cloud resource, credential or guessed retention/capacity/policy value. It does not redesign S1 ownership/lifecycle, S2 IVR/activation/commerce/entitlement behavior, the frozen 44-table/40-invariant/9-wave/3-slice baseline, the 28/16 lifecycle split, the 28 sensitive tables, the 81 FK decisions or the 24 key sets. It does not create an online support, refund, invoice, role-designer, AI, notary or workflow engine.

### 1.5 Exact S2 table membership

The 23 S2 tables below are the exact V004-V007 membership. Together with the 13 S1 tables they form the frozen 44-table baseline. S3 contributes the remaining eight V008-V009 tables, authored in §4.39-§4.46.

| # | Table | Wave |
| --- | --- | --- |
| 1 | catalog_order.catalog_product | V004 |
| 2 | catalog_order.catalog_version | V004 |
| 3 | catalog_order.catalog_offer | V004 |
| 4 | catalog_order.catalog_benefit_line | V004 |
| 5 | catalog_order.customer_order | V004 |
| 6 | catalog_order.payment_order | V004 |
| 7 | catalog_order.payment_event | V004 |
| 8 | ivr.outbound_call_task | V005 |
| 9 | ivr.inbound_session | V005 |
| 10 | ivr.call_record | V005 |
| 11 | ivr.call_leg | V005 |
| 12 | ivr.call_event | V005 |
| 13 | ivr.ivr_decision | V005 |
| 14 | entitlement_metering.trial_policy_version | V006 |
| 15 | entitlement_metering.entitlement_grant | V006 |
| 16 | entitlement_metering.trial_grant | V006 |
| 17 | entitlement_metering.entitlement_balance | V006 |
| 18 | entitlement_metering.entitlement_issuance | V006 |
| 19 | entitlement_metering.usage_reservation | V006 |
| 20 | entitlement_metering.usage_fact | V006 |
| 21 | activation_consent.consent_record | V007 |
| 22 | activation_consent.service_activation | V007 |
| 23 | activation_consent.service_activation_transition | V007 |

S2 status is **accepted at checkpoint `8934df9`**; S3 status is **accepted at checkpoint `9ad33e8`**; aggregate deepreview is **accepted at checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`**. PR lifecycle and current gate state are authoritative only in `docs/gateflow/project-execution-control.md`. The frozen machine-readable catalog, matrix, sensitive inventory, FK catalog, key catalog, function-wave block, nine wave rows, three slice membership blocks, 40 invariant rows and 28 open-input rows are unchanged.

### 1.6 Exact S3 table membership

The eight S3 tables below are the exact V008-V009 membership. Together with the 13 S1 and 23 S2 tables they form the frozen 44-table baseline. S3 has no forward dependency on a later slice because no later slice exists.

| # | Table | Wave |
| --- | --- | --- |
| 1 | evidence.media_object | V008 |
| 2 | evidence.recording | V008 |
| 3 | evidence.evidence_record | V008 |
| 4 | evidence.evidence_artifact | V008 |
| 5 | evidence.evidence_event | V008 |
| 6 | sharing.evidence_share_grant | V009 |
| 7 | sharing.share_admission | V009 |
| 8 | sharing.share_access_event | V009 |

S3 consumes only S1/S2 interfaces: the subject/call/session/reservation identities and immutable captured facts from S1/S2, the existing platform Outbox/audit functions, and the completed evidence media metadata. It does not require an S1/S2 table to reference an S3 table, and no S1/S2 migration is altered.

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

- For the `IVR_CONFIRM`/`ACTIVATION_NEXT_ACTION`, `INBOUND_RESOLVE` and `DIRECT_AUTHORIZATION` scopes, the access identity input is `accessNumberHmac`: the lowercase 64-hex HMAC-SHA-256, under the same stable approved phone-number HMAC key and contract as the other caller/number HMACs (§2.6), of the canonical normalized access number supplied by the authenticated IVR request. It is a validated ephemeral fingerprint input only: only the resulting `request_fingerprint` is stored; it is never a new table or column, never a response field, never logged, audited, emitted, placed in a redacted snapshot or returned, and it is never persisted separately. The trusted IVR service must authenticate and supply the canonical access number before any fingerprint/replay resolution; missing or invalid HMAC input fails closed before any effect. `accessNumberMasked` remains a display-only value that is not collision-resistant and is not authoritative for replay identity: the same canonical access number with otherwise identical semantics, including a regenerated transport event ID, replays, while a different canonical access number conflicts before frozen replay even when its display mask is identical.

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

### 2.9 S2 schemas, protocol identities and registries

- S2 schemas are `catalog_order`, `ivr`, `entitlement_metering` and `activation_consent`. Each has one NOLOGIN, non-superuser owner role and one runtime role: `catalog_order_owner`/`catalog_order_role`, `ivr_owner`/`ivr_role`, `entitlement_metering_owner`/`entitlement_metering_role` and `activation_consent_owner`/`activation_consent_role`. The migration identity creates them in V004.
- S2 protocol identities are owned internally and are not natural keys: `ivr.outbound_call_task.task_id`, `ivr.inbound_session.call_id` and `ivr.call_record.call_id` (opaque IVR identifiers, `text`), `catalog_order.customer_order.order_no`, `catalog_order.payment_order.merchant_order_no`, `catalog_order.payment_event.provider_event_id`, and the IVR `event_id`/`source_event_id` values. No phone number, callee number, provider transaction ID, order snapshot hash or HMAC is a protocol identity or public key.
- `call_record.call_id` is the canonical call-scoped foreign-key parent. `inbound_session.call_id` is the root IVR protocol identity with no parent. All call-linked rows name `call_record.call_id`; `ivr.outbound_call_task.claimed_call_id` is the one late-added same-wave FK from task to call.
- The entitlement-type registry for the first baseline is `RECORDING_SECONDS`, `AI_SECONDS`, `EVIDENCE_COUNT` and `STORAGE_BYTES`. IVR reservations and usage facts in this baseline use recording seconds (`1 unit = 1 second`); `authorizedRecordingSeconds` is the explicit identity conversion of `usage_reservation.authorized_units`. No production quantity, price or validity value is fixed by this registry.
- The IVR action registry is exactly the seven accepted actions `GUIDE_ACTIVATION`, `DIAL_TASK_CALLEE`, `ENTER_CALLEE`, `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED` and `REJECT_CALL_IN_PROGRESS`. `INBOUND_RESOLVE`, `ACTIVATION_NEXT_ACTION` and `DIRECT_AUTHORIZATION` are decision purposes, not actions. `DIRECT_AUTHORIZATION` is never an eighth action.
- The stable IVR reason registry is `ACTIVATION_REQUIRED`, `CONSENT_REQUIRED`, `CALLER_UNTRUSTED`, `SESSION_IDENTITY_MISMATCH`, `CALL_IN_PROGRESS`, `CONFIRMATION_NOT_ALLOWED`, `ACTIVATION_SUSPENDED`, `CALLER_BINDING_REVOKED`, `WORKSPACE_ACCESS_REVOKED`, `TASK_ACCESS_REVOKED`, `ORGANIZATION_INACTIVE`, `ACCOUNT_DISABLED`, `CALL_STATE_UNRESOLVED`, `TASK_CONFLICT`, `TASK_CLAIMED`, `NO_AVAILABLE_ENTITLEMENT`, `NO_VALID_TASK`, `TASK_EXPIRED`, `TASK_UNAVAILABLE`, `DIRECT_AUTHORIZED` and `DATA_INTEGRITY`. Reason codes are stable codes, never free text.
- The baseline event-domain registry is unchanged from S1: `ACTIVATION`, `IVR_CONFIRM`, `ACTIVATION_STATUS`, `OPERATIONS`, `IVR_EVENT`, `IVR_DIRECT_AUTH`, `EVIDENCE`, `PAYMENT`, `SHARE_ACCESS`, `PURCHASE` and `SUBJECT_CLAIM`. S2 uses `IVR_EVENT` for `call_event`, `IVR_CONFIRM` for confirmation decisions and consent, `IVR_DIRECT_AUTH` for direct-authorization source events, `ACTIVATION_STATUS` for activation transition fact identity, `PAYMENT` for payment-driven issuance, `PURCHASE` for order/consent idempotency and `ACTIVATION`/`OPERATIONS` for activation transitions. The same string may exist in different domains; it is never reused within one domain.
- `platform.idempotency_begin`/`complete` are used by S2 with scope `IVR_CONFIRM` (key `eventId`, canonical confirmation fingerprint) and `PURCHASE` (key `account_id || ':' || idempotency_key`, canonical order fingerprint). The scope registry remains exactly `IVR_CONFIRM` and `PURCHASE`; task creation uses its own `(created_by_account_id, idempotency_key)` key and direct authorization uses the call/purpose decision, not a third platform scope. Each IVR endpoint validates its transport/source event ID as nonblank 1-128 characters before `idempotency_begin` or any domain effect (stable `IVR_ARGUMENT_INVALID` otherwise); the platform key column keeps its general 1-512 capacity and the order client key remains 1-475. The exact semantic fields and Outbox identities are the bounded registry in §10.17.
- The S2 allocation-policy input is an approved server-side code/version reference, not a new database registry or client-controlled ordering. No trial-first, FIFO, stacking, offset or arbitrary expiry/UUID balance ordering is approved by Q5/Q25; the selector accepts only an approved reference that resolves to exactly one eligible balance and otherwise fails closed (§10.18).
- S2 event-domain usage is `IVR_EVENT` for `call_event` and usage/task facts, `IVR_CONFIRM` for confirmation decisions, `IVR_DIRECT_AUTH` for direct-authorization decisions, `ACTIVATION` for trial facts, `ACTIVATION_STATUS` for activation transition facts, `PURCHASE` for orders and purchase consent, `PAYMENT` for payment events and payment-driven issuance, and `OPERATIONS` for catalog/trial-policy publication facts. The 21-value IVR reason registry is unchanged; policy/allocation denial detail is a service-level typed code retained only in the encrypted internal payload/audit and maps to an existing stored reason.

### 2.10 S2 revision, immutability and snapshot conventions

- Every S2 mutable-head table uses `revision integer NOT NULL DEFAULT 1 CHECK (revision >= 1)` and conditional `revision` updates. Append-only S2 tables have no revision and reject update/delete by privilege plus trigger.
- Frozen snapshots use the section 2.5 canonical JSON and SHA-256. The commercial, order, trial-policy, grant, consent and usage snapshots are immutable after insertion; a later catalog, policy or consent change never rewrites them.
- A decision's `response_snapshot` is the typed redacted snapshot of section 10.14; `response_payload_ciphertext` is the encrypted internal payload under the section 2.6 envelope. `response_sha256` hashes the canonical redacted snapshot, and the encrypted payload's redacted projection must equal that snapshot. Replay decrypts and returns the same immutable payload; it never recomputes current state.
- Mutable S2 state is always expressed by status plus revision or terminal time, never by delete. `catalog_product`, `catalog_benefit_line`, `payment_event`, `ivr_decision`, `trial_grant`, `entitlement_grant`, `usage_fact`, `consent_record` and `service_activation_transition` are append-only/frozen. The other 14 S2 tables are mutable heads. This preserves the frozen 28/16 lifecycle split across all 44 tables.
- No S2 table stores an order-level entitlement status. `customer_order.payment_status` is payment-only; issuance state is owned by `entitlement_metering.entitlement_issuance`, one durable row per `(order_id, installment_no, entitlement_type)`.

### 2.11 S2 nullability and key conventions

- Every S2 `id` is an application-generated `uuid` with no default. The V006 `issue_first_trial` function generates its inserted `id` values with `pg_catalog.gen_random_uuid()` inside the function body; it accepts no `p_id` argument and no table gains an `id` default.
- Hash columns are lowercase 64-hex SHA-256/HMAC text with named CHECK constraints. Ciphertext columns use the `v1.<key_ref>.<base64url>` envelope with named CHECK constraints. Masked columns are display-only.
- Currency columns are three uppercase ISO-4217 characters; money columns are `bigint` cents and never floating point. Unit columns are integer/bigint values, never float.
- Every S2 FK is `ON UPDATE RESTRICT ON DELETE RESTRICT`. Same-wave ALTER FKs are ordinary immediate constraints, not deferrable. S2 has no deferrable FK and no cascade.

### 2.12 S3 schemas, identities, registries and immutability

- S3 schemas are `evidence` and `sharing`. Each has one NOLOGIN, NOSUPERUSER, NOCREATEDB owner role and one runtime role: `evidence_owner`/`evidence_role` and `sharing_owner`/`sharing_role`. The migration identity creates `evidence_role` in V008 and `sharing_role` in V009. V008 grants evidence schema `USAGE` only to `evidence_role`; V009 creates `sharing_role` first and only then grants it evidence schema `USAGE`. `PUBLIC` receives nothing.
- S3 protocol identities are opaque internal values, never natural keys: `evidence.recording.recording_id` (1-128 characters, unique), `evidence.evidence_record.source_event_id` (1-128 characters in domain `EVIDENCE`), and `sharing.evidence_share_grant.share_id` (1-128 characters, unique). A public share URL carries a high-entropy capability token, never `share_id`, `evidence_id`, `object_key`, `object_version_id` or a permanent media URL. `sharing.share_admission.admission_key` is an opaque server-derived digest/key, never raw verification input.
- The evidence event-domain registry for `evidence.evidence_event` is exactly `EVIDENCE`; the sharing access event-domain registry is exactly `SHARE_ACCESS`. Event IDs are nonblank 1-128 characters and unique within their domain. `evidence.evidence_record.source_event_domain` is exactly `EVIDENCE` and its `(source_event_domain, source_event_id)` is the accepted-column source identity of the one `EVIDENCE_CREATED` event. `source_event_id` is exactly the validated stable source event `eventId` (nonblank 1-128), never a hash or derivation of mutable capture, link or snapshot facts; the matching `EVIDENCE_CREATED.event_id` is that same value.
- The evidence artifact-role registry is exactly `ORIGINAL` and `DERIVED` in this baseline. The fixed ordering rank for these roles is the non-lexical `ORIGINAL = 0`, `DERIVED = 1`; no lexical role-name ordering is used anywhere. An original object is never overwritten by a derived object; derived objects are separate `media_object` rows and separate artifact rows. AI, notary, transcript and summary outputs are outside this baseline and add no role or table.
- The recording-status registry is `DECLARED`, `UPLOADED`, `VALIDATED`, `QUARANTINED`; the evidence-status registry is `PENDING`, `SOLIDIFIED`, `QUARANTINED`, `FAILED`; the share-grant status registry is `ACTIVE`, `EXPIRED`, `REVOKED`, `LOCKED`. These are the only status values; no catch-all evidence or sharing status exists.
- The share verification-mode registry is `NONE`, `EXTRACTION_CODE`, `DESIGNATED_MOBILE`, `ORGANIZATION_MEMBER`. `NONE` means link possession only and is never recorded as a verified identity. `DESIGNATED_MOBILE` means the specified phone has been proven by an approved trusted SMS verification for the exact normalized designated number and the intended share/action, consistent with `docs/07-web-capability-hub-spec.md:141`; comparing a supplied number's HMAC or merely knowing the number is not possession proof. The grant stores only a keyed HMAC of the normalized designated number and its key version inside the immutable `content_snapshot.verification` member, never a plaintext number or OTP. OTP lifecycle, delivery and numeric parameters remain Security/Q12 inputs; absent an approved trusted SMS verification input the flow fails closed and creates no admission. `ORGANIZATION_MEMBER` requires `owner_organization_id` and verifies the authenticated recipient's current active membership. `EXTRACTION_CODE` uses a per-grant salt and salted slow KDF digest bound to the grant and `code_generation`.
- The share access-event registry is `PAGE_OPENED`, `PREFETCH_IGNORED`, `VERIFICATION_FAILED`, `LOCKED`, `ADMISSION_GRANTED`, `TICKET_ISSUED`, `TICKET_FAILED`, `PLAYBACK_AUTHORIZED`, `SEEK_AUTHORIZED`, `RENEWED`, `EXTENDED`, `CODE_ROTATED`, `REVOKED`, `EXPIRED` and `FAILURE`. Page opens, prefetches and failed verification are recorded only as events and consume no admission. Required ticket/access event IDs are deterministic server-derived values, never random per retry: `admission-granted:<grant_uuid>:<admission_uuid>`, `ticket-issued:` plus the lowercase SHA-256 of canonical `{shareGrantId, admissionId}`, `ticket-failed:` plus the lowercase SHA-256 of canonical `{shareGrantId, admissionId, admissionKeyDigest}`, and `share-access:` plus the lowercase SHA-256 of canonical `{shareGrantId, shareAdmissionId, eventType, requestKeyDigest}` for playback/seek/renewal/failure events. The raw request key is never stored; only its digest is persisted inside the redacted event payload. `PAGE_OPENED`, `PREFETCH_IGNORED` and `VERIFICATION_FAILED` use `share-observation:` + the lowercase SHA-256 of canonical `{shareGrantId, eventType, requestKeyDigest}` (82 characters) from the grant ID, event type and the digest of the stable nonblank request/action key (validated 1-128); their payload fingerprint is the lowercase SHA-256 of canonical `{shareGrantId, eventType, result, reasonCode (null-safe), requestKeyDigest, verificationMode (null-safe)}`. Raw request keys and secrets are never stored. Same identity and payload fingerprint returns the first event; same identity with a changed fingerprint is a stable conflict and appends nothing; a different request key is a distinct observation. These events create no admission and consume no count.
- Transition action identities for `REVOKE`, `CODE_ROTATE`, `EXPIRE`, `EXTEND` and `LOCK` are deterministic: event ID `share-transition:` plus the lowercase SHA-256 of canonical `{shareGrantId, actionType, sourceIdentity}` (81 characters), where `sourceIdentity` is `{kind: 'REQUEST', actionRequestId}` with the stable authenticated client action request ID (validated nonblank 1-128) for revoke, code rotation or extension, `{kind: 'EXPIRY', expiresAt}` with the grant's recorded approved expiry for system/owner expiry, or `{kind: 'REVISION', sourceRevision}` with the pre-update grant revision on which a system/owner lock decision was made. `actionRequestDigest` is the lowercase SHA-256 of the exact stable action request ID; the raw ID is not stored. A post-update current revision or generated code/salt is never an identity input. Different actions or distinct requests hash distinctly. The redacted event payload stores `actionType`, `actionRequestDigest` or the exact expiry/source revision fact, `actionFingerprint` and the resulting status/generation/revision, and never a plaintext code, capability token or generated secret. `actionFingerprint` is the lowercase SHA-256 of the section 2.5 canonical JSON `{shareGrantId, actionType, reasonCode (null-safe), newExpiresAt (EXTEND only, null-safe), actionRequestDigest | expiresAt | sourceRevision}` excluding generated code/salt/hash, post-update status/generation/revision and only generated or observed transport, network, persistence or server-decision timestamps (for example `createdAt`, `updatedAt`, `occurredAt`, `decidedAt` and equivalent server times). The approved business input `newExpiresAt` (EXTEND only) and the recorded `expiresAt` fact (EXPIRE identity) are never excluded from `actionFingerprint`; a same EXTEND action identity with a changed `newExpiresAt` is a stable conflict and mutates nothing. A same-action retry reads the deterministic event by identity and returns the first recorded outcome before any current status/revision precondition; a same identity with a changed fingerprint is a stable conflict; a different action request is a distinct transition. This fixes only internal semantics; wire encoding remains Q15.
- `sharing.share_admission.admission_key` is deterministically derived as `admission:` plus the lowercase SHA-256 of canonical `{shareGrantId, admissionRequestId}`, where `admissionRequestId` is the authenticated public request's stable opaque idempotency input (validated nonblank 1-128). The raw request ID is never stored; only the derived key is. A same request ID reproduces the same key; a different request is a different admission attempt. A ticket/session expiry is not a new idempotency key: after `sessionExpiresAt`, the session is expired and the same key returns the original admission fact without minting a new ticket; a new admission with a new request ID/recheck/count is required. The `ADMISSION_GRANTED` payload records `verificationMode` and, for `ORGANIZATION_MEMBER`, the original trusted recipient binding `recipientAccountId` and `recipientOrganizationId` (canonical lowercase UUID text), plus `revocationGeneration`, `codeGeneration`, `grantExpiresAt`, `admissionExpiresAt` and `sessionExpiresAt`. `admissionExpiresAt` is the grant `expires_at` frozen at admission and is never recomputed or extended. `sessionExpiresAt = min(admitted_at + approved Q22 ticket TTL, admissionExpiresAt)`; absent or unapproved Q22 TTL fails closed. Each issued playback ticket derives its absolute `ticketExpiresAt` and Redis TTL exactly as §9.31 states. No extra admission TTL, database column or policy is introduced, and a later grant extension never lengthens an existing admission or session.
- `sharing.evidence_share_grant.share_id` is a deterministic creation identity derived only from the trusted authenticated creator account and the stable creation-request/action ID: `share:` plus the lowercase SHA-256 of the canonical JSON `{creatorAccountId, creationRequestId}` (70 characters), using the canonical lowercase 36-character account UUID and the exact nonblank 1-128 creation request ID (UTF-8 code points, no Unicode normalization or case folding). The raw creation request ID is not stored. The initial scope and request parameters are frozen in the existing typed redacted `content_snapshot.creation` object `{schemaVersion: 1, creatorAccountId, ownerOrganizationId (null-safe), evidenceId, verificationMode, downloadAllowed, expiresAt (RFC 3339 UTC), maxAdmissions (null-safe), scopeDigest, creationRequestDigest, creationFingerprint}`, where `scopeDigest` is the lowercase SHA-256 of the section 2.5 canonical JSON of `{content, verification}` (the frozen content and verification members), `creationRequestDigest` is the lowercase SHA-256 of the exact creation request ID, and `creationFingerprint` is the lowercase SHA-256 of the section 2.5 canonical JSON of those semantic fields excluding `creationFingerprint` itself, generated IDs, capability/code secrets and only generated or observed transport, network, persistence or server-decision timestamps (for example `createdAt`, `updatedAt` and equivalent server times). The approved business input `expiresAt` (RFC 3339 UTC) is part of those semantic fields and is never excluded by the timestamp rule; a same creation identity with a changed `expiresAt` returns `SHARE_CREATION_CONFLICT` and mutates nothing. Repeating the same creation action reproduces `share_id`; before any state-dependent mutation the service recomputes the creation fingerprint and compares it with the frozen value. Same identity and fingerprint return the existing grant metadata only, with no new grant, no new plaintext capability token and no new event. Same identity with changed scope/parameters returns stable `SHARE_CREATION_CONFLICT` and mutates nothing; a changed scope is never a second grant under the same request ID. A usable replacement requires a new `creationRequestId`, which derives a new `share_id`, a new grant and a new one-time random capability token; the unusable prior grant may be revoked through the existing owner revoke transition. There is no token reconstruction, reissuance action or recoverable plaintext capability secret. The capability token itself remains high-entropy random and is returned only once at first creation; only its digest is stored.
- The evidence owner exposes one read-only, same-connection application collaborator `evidence.read_shareable_projection` for sharing. For a `SOLIDIFIED` evidence record it returns exactly `{evidenceId, subjectId, status, sourceType, sourceRefId, identityAtCapture, artifacts[]}`, where each artifact is `{artifactRole, mediaSha256, mediaSizeBytes, mediaMimeType, validatedDurationSeconds}`. `artifacts` is sorted by the fixed non-lexical artifact-role rank ascending, where `ORIGINAL = 0` and `DERIVED = 1`, then by `mediaSha256`, `mediaSizeBytes` and `mediaMimeType` ascending, so the array order is reproducible from the returned facts and is never the lexicographic order of the role names. `validatedDurationSeconds` is the `evidence.recording.validated_duration` of the one recording whose `media_object_id` validates that exact media object (null-safe; the baseline `ORIGINAL` artifact is the validated recording). The collaborator returns no media-object id, bucket, object key, object version, bytes, permanent URL, crypto key reference or raw protected value, performs no sharing write and takes no sharing or other foreign row lock. `sharing.create_share_grant` calls it on the same connection with `Propagation.MANDATORY` for a `SOLIDIFIED` record before its owner-local grant mutation, and constructs `content_snapshot.content` as exactly `{schemaVersion: 1, evidenceId, subjectId, sourceType, sourceRefId, identityAtCapture, artifacts}` using that order. `scopeDigest` is the lowercase SHA-256 of the section 2.5 canonical JSON of `{content, verification}`, and `content_sha256` is the lowercase SHA-256 of the section 2.5 canonical JSON of the complete `content_snapshot` `{content, verification, creation}`. The content member is reproducible byte-for-byte from these immutable facts, contains no object identity/bytes/URL/share secret, and a same-identity retry with changed content conflicts; final wire encoding remains Q15.
- Every `requestKeyDigest` used by the `share-observation:` and `share-access:` identities is explicitly the lowercase hexadecimal SHA-256 of the exact stable nonblank request/action key, validated at 1-128 UTF-8 code points with no Unicode normalization and no case folding. Raw keys are never stored, logged, audited or emitted. The declared 82-character `share-observation:` identity length, the `share-access:` identity formula and the separate payload fingerprints are unchanged.
- Share-creation replay projection ordering: for an existing grant creation identity, `sharing.create_share_grant` calls the read-only same-connection `evidence.read_shareable_projection` collaborator before recomputing or comparing the frozen `content_snapshot.creation` fingerprint, just as for a new creation, but still before any sharing state-dependent mutation. It reconstructs the deterministic server-derived `content` from the returned projection plus the current validated request parameters. Exact immutable `SOLIDIFIED` evidence/artifact/media facts with otherwise same request semantics return the existing grant metadata only; changed or inconsistent artifact/media/content facts under the same identity return `SHARE_CREATION_CONFLICT` and mutate nothing; an absent, unresolvable or not-`SOLIDIFIED` projection fails closed with the existing typed error and creates or mutates no grant, token or event. This adds no foreign row lock and does not redesign current access or risk policy.
- Every S3 `id` is an application-generated `uuid` with no table default. Hash columns are lowercase 64-hex text. Times are UTC `timestamptz`; admission, expiry and generation checks use `clock_timestamp()` after lock acquisition, never request time or transaction-start `now()`.
- The three S3 mutable heads are `evidence.recording`, `evidence.evidence_record` and `sharing.evidence_share_grant`; each carries `revision integer NOT NULL DEFAULT 1 CHECK (revision >= 1)` and conditional updates. The five S3 append-only tables are `evidence.media_object`, `evidence.evidence_artifact`, `evidence.evidence_event`, `sharing.share_admission` and `sharing.share_access_event`; they have no revision, reject update/delete by privilege and trigger, and are never deleted through ordinary administration. This preserves the frozen 28 mutable-head / 16 append-only closure (S1 11/2, S2 14/9, S3 3/5).
- `evidence.evidence_record` and `evidence.evidence_event` are the only same-wave reciprocal cycle. Their composite source FK and reverse evidence FK are both `DEFERRABLE INITIALLY DEFERRED`, added by exact `ALTER TABLE` statements after both V008 tables exist. The two `EVIDENCE_CREATED` ownership checks are enforced at commit by owner-local deferred constraint triggers, not by a new SQL function-wave entry. Until commit, either insertion order is valid; after commit, exactly one matching creation event owns each evidence record and no creation event may own a different evidence identity.
- `sharing.evidence_share_grant.expires_at` and `max_admissions` have no guessed defaults. The approved sharing-policy input must explicitly select a validity window and either a positive admission limit or explicit unlimited; absent or unapproved input fails closed with no grant. No retention period, KDF parameter value, capability-token length beyond the required minimum 128 random bits, admission limit, expiry default, rate-limit threshold or revocation-window value is invented.
- S3 stores no plaintext capability token, share code, designated phone, OTP, object URL, object key, bucket name or object version in logs, access events, Outbox or audit. Object identity is internal to the evidence owner and the media gateway; the public sharing layer receives only a short-lived signed media credential bound to the grant, admission, generation and expiry.

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
| catalog-order | catalog_order | catalog_order_owner | catalog_order_role | catalog_order.catalog_product, catalog_order.catalog_version, catalog_order.catalog_offer, catalog_order.catalog_benefit_line, catalog_order.customer_order, catalog_order.payment_order, catalog_order.payment_event |
| ivr | ivr | ivr_owner | ivr_role | ivr.outbound_call_task, ivr.inbound_session, ivr.call_record, ivr.call_leg, ivr.call_event, ivr.ivr_decision |
| entitlement-metering | entitlement_metering | entitlement_metering_owner | entitlement_metering_role | entitlement_metering.trial_policy_version, entitlement_metering.entitlement_grant, entitlement_metering.trial_grant, entitlement_metering.entitlement_balance, entitlement_metering.entitlement_issuance, entitlement_metering.usage_reservation, entitlement_metering.usage_fact |
| activation-consent | activation_consent | activation_consent_owner | activation_consent_role | activation_consent.consent_record, activation_consent.service_activation, activation_consent.service_activation_transition |
| evidence | evidence | evidence_owner | evidence_role | evidence.media_object, evidence.recording, evidence.evidence_record, evidence.evidence_artifact, evidence.evidence_event |
| sharing | sharing | sharing_owner | sharing_role | sharing.evidence_share_grant, sharing.share_admission, sharing.share_access_event |

All owner and runtime roles are NOLOGIN, NOSUPERUSER and NOCREATEDB. The migration identity is the only role that creates S1, S2 or S3 objects. `subject_identity_verification_role` is a narrow service role that may `SELECT` the subject `id` plus the real-name/status/revision/update-time columns and may update only the subject-level real-name columns, revision and update time; it cannot alter owners, source, bindings or claims. It receives `USAGE` on `subject_number` and `audit` and `EXECUTE` on `audit.append_fact` so its same-transaction verification fact can be audited. `operations_service_role` is a NOLOGIN S1 service role used only to execute `audit.append_fact`; it receives `USAGE` on `audit` and no table write privilege.

The four S2 runtime roles are NOLOGIN, NOSUPERUSER and NOCREATEDB. They receive `USAGE` on their own schema, `USAGE` on `platform` and `audit`, `USAGE` without `CREATE` on the exact S1 schemas whose declared cross-module columns they read, `EXECUTE` on the four S1 functions, and only the exact owned/cross-module table grants in §10.12. PostgreSQL requires containing-schema `USAGE` in addition to a table/column privilege; every cross-module `SELECT` in §10.12 is paired with that schema `USAGE` in the reader's creation wave. No S2 role receives schema `CREATE` or any foreign table write. The `ivr_role` is the application identity used by the IVR-facing application services; the WAF/mTLS service authentication of the IVR protocol is an API/security concern outside this schema. `ivr.select_call_action`, `ivr.record_activation_next_action`, `activation-consent.record_activation_confirmation`, `activation-consent.lock_service_activation`, `entitlement-metering.reserve_call_usage`, `ivr.lock_reconciliation_caller` and `operations-iam.lock_employee_administrator` are application services running under the owning runtime role on the same connection; they are not `SECURITY DEFINER` SQL functions and add no function-wave row. `ivr.lock_reconciliation_caller` is the rank-1 IVR-owned lock-only collaborator used by entitlement reconciliation phases A and C; under `ivr_role` it reads the existing `ivr.call_record.caller_number_hmac` for the immutable `callId` and acquires `ivr.caller:<canonical_caller_hmac>`, takes no row lock, writes/appends/enqueues nothing and grants `entitlement_metering_role` no `ivr` privilege. `activation-consent.lock_service_activation` is the rank-4 lock-only collaborator; it locks an existing activation row under the owner role, creates/updates/appends nothing, and returns the current facts. `operations-iam.lock_employee_administrator` is the rank-13 operations-iam-owned lock-only collaborator; under `operations_iam_role` it locks the authenticated employee row and takes the administrator advisory lock, creates/updates/appends nothing, and returns the current facts.

The two S3 runtime roles are NOLOGIN, NOSUPERUSER and NOCREATEDB. `evidence_role` receives `USAGE` on `evidence`, `subject_number`, `platform`, `audit`, `ivr` and `entitlement_metering` for its exact declared reads, `EXECUTE` only on `platform.outbox_append` and `audit.append_fact`, and only the owned/cross-module grants in §10.21. `sharing_role` receives `USAGE` on `sharing`, `evidence`, `subject_number`, `customer_iam`, `organization_access`, `platform` and `audit`, `EXECUTE` only on `platform.outbox_append` and `audit.append_fact`, and only the exact evidence/account/organization/membership/subject-ownership/risk column grants in §10.21. Neither role receives `EXECUTE` on `platform.idempotency_begin` or `platform.idempotency_complete`, any platform/audit table privilege, schema `CREATE`, `DELETE`, `TRUNCATE` or any foreign table write. The `subject_number` schema `USAGE` accompanies the exact declared subject/ownership/binding/risk column `SELECT`s and grants no table write. S3 owner services run on the caller's connection under the owning role. S3 adds no SQL function and no function-wave row; the evidence creation-cycle trigger functions are owner-local trigger functions, not published services.

### 3.2 Table ownership and write paths

| Table | Owner module | Wave | Slice | Read-only collaborators | Permitted write path |
| --- | --- | --- | --- | --- | --- |
| platform.idempotency_record | platform-reliability | V001 | S1 | all domain owners | platform.idempotency_begin/complete only |
| platform.outbox_event | platform-reliability | V001 | S1 | all domain owners | platform.outbox_append for append; platform_dispatcher_role for dispatcher columns |
| audit.audit_fact | operations-audit | V001 | S1 | all domain owners | audit.append_fact only |
| customer_iam.customer_account | customer-iam | V002 | S1 | organization-access, subject-number, ivr, catalog-order, activation-consent (exact §10.12 `SELECT` only) | customer_iam_role |
| customer_iam.auth_identity | customer-iam | V002 | S1 | none | customer_iam_role |
| customer_iam.identity_profile | customer-iam | V002 | S1 | none | customer_iam_role |
| organization_access.organization | organization-access | V002 | S1 | customer-iam, subject-number, operations-iam, ivr, catalog-order, activation-consent (exact §10.12 `SELECT` only) | organization_access_role |
| organization_access.organization_membership | organization-access | V002 | S1 | customer-iam, subject-number, ivr, catalog-order, activation-consent (exact §10.12 `SELECT` only) | organization_access_role |
| operations_iam.employee_account | operations-iam | V002 | S1 | operations-audit; activation-consent operations lifecycle via the operations-iam lock-only collaborator (no table/row-lock privilege) | operations_iam_role |
| subject_number.business_subject | subject-number | V003 | S1 | customer-iam, organization-access, ivr, catalog-order, entitlement-metering, activation-consent (exact §10.12/§10.21 `SELECT` only), sharing (exact §10.21 ownership/risk projection only) | subject_number_role for subject creation (both owners null) and verified-personal owner set; subject_identity_verification_role for real-name columns |
| subject_number.caller_number_binding | subject-number | V003 | S1 | customer-iam, organization-access, ivr (exact §10.12 `SELECT` only), sharing (exact §10.21 risk projection only) | subject_number_role |
| subject_number.subject_claim | subject-number | V003 | S1 | customer-iam, organization-access | subject_number_role insert only; no update/delete |
| subject_number.subject_risk_state | subject-number | V003 | S1 | customer-iam, organization-access, ivr (exact §10.12 `SELECT` only), sharing (exact §10.21 historical-evidence risk projection only) | subject_number_role |
| catalog_order.catalog_product | catalog-order | V004 | S2 | none for S2; frozen snapshot/fact handoff only | catalog_order_role insert only; no update/delete |
| catalog_order.catalog_version | catalog-order | V004 | S2 | none for S2; frozen snapshot/fact handoff only | catalog_order_role |
| catalog_order.catalog_offer | catalog-order | V004 | S2 | none for S2; frozen snapshot/fact handoff only | catalog_order_role |
| catalog_order.catalog_benefit_line | catalog-order | V004 | S2 | none for S2; frozen snapshot handoff only | catalog_order_role insert only; no update/delete |
| catalog_order.customer_order | catalog-order | V004 | S2 | entitlement-metering (exact §10.12 `SELECT`); ivr and activation-consent fact handoff only | catalog_order_role insert; update payment status/terminal fields only |
| catalog_order.payment_order | catalog-order | V004 | S2 | none | catalog_order_role |
| catalog_order.payment_event | catalog-order | V004 | S2 | entitlement-metering | catalog_order_role insert only; no update/delete |
| ivr.outbound_call_task | ivr | V005 | S2 | none; owner-service fact handoff only | ivr_role |
| ivr.inbound_session | ivr | V005 | S2 | activation-consent fact handoff only (IVR passes session facts; no table `SELECT`) | ivr_role |
| ivr.call_record | ivr | V005 | S2 | evidence (S3); entitlement-metering reconciliation obtains the caller HMAC and rank-1 lock only through the `ivr.lock_reconciliation_caller` lock-only collaborator (never reads IVR tables directly, no row lock) | ivr_role |
| ivr.call_leg | ivr | V005 | S2 | none | ivr_role |
| ivr.call_event | ivr | V005 | S2 | none; entitlement-metering receives validated settlement facts only | ivr_role |
| ivr.ivr_decision | ivr | V005 | S2 | none | ivr_role insert only; no update/delete; only through `ivr.select_call_action` |
| entitlement_metering.trial_policy_version | entitlement-metering | V006 | S2 | none; activation-consent receives the resolved policy/version via the entitlement owner service | entitlement_metering_role |
| entitlement_metering.entitlement_grant | entitlement-metering | V006 | S2 | ivr, catalog-order (exact §10.12 `SELECT` only) | insert only; no update/delete |
| entitlement_metering.trial_grant | entitlement-metering | V006 | S2 | none; fact handoff via `issueFirstTrial` result | insert only through `issue_first_trial`; no update/delete |
| entitlement_metering.entitlement_balance | entitlement-metering | V006 | S2 | ivr (exact §10.12 `SELECT` only) | entitlement_metering_role; `issue_first_trial` insert |
| entitlement_metering.entitlement_issuance | entitlement-metering | V006 | S2 | catalog-order (exact §10.12 `SELECT` only) | entitlement_metering_role |
| entitlement_metering.usage_reservation | entitlement-metering | V006 | S2 | ivr fact handoff via reserve/settle services only (no table `SELECT`) | `reserve_call_usage` insert; `entitlement_metering_role` lifecycle update |
| entitlement_metering.usage_fact | entitlement-metering | V006 | S2 | ivr fact handoff via settlement result; evidence (S3) | insert only; no update/delete |
| activation_consent.consent_record | activation-consent | V007 | S2 | ivr, catalog-order (exact §10.12 `SELECT` only) | insert only through the owning service; no update/delete |
| activation_consent.service_activation | activation-consent | V007 | S2 | ivr (exact §10.12 `SELECT`); the row lock is taken only by the activation-consent lock-only collaborator | `record_activation_confirmation`, `record_client_activation` and operations lifecycle service |
| activation_consent.service_activation_transition | activation-consent | V007 | S2 | ivr fact handoff via the owner service result (no table `SELECT`) | insert only through the owning service; no update/delete |
| evidence.media_object | evidence | V008 | S3 | none; internal object identity only; shareable hash/size/MIME/duration facts are returned only through the evidence-owner read collaborator | evidence_role insert only; no update/delete |
| evidence.recording | evidence | V008 | S3 | none; IVR/media-worker result handoff only | evidence_role; the `record_upload_observed` DECLARED→UPLOADED transition and status/validation/revision columns only after insert |
| evidence.evidence_record | evidence | V008 | S3 | sharing reads the exact §10.21 SELECT columns, and content/media facts come through the evidence-owner read_shareable_projection collaborator; no other module writes it | evidence_role insert and PENDING → SOLIDIFIED/QUARANTINED/FAILED status update |
| evidence.evidence_artifact | evidence | V008 | S3 | sharing receives artifact-role/media facts only through the evidence-owner read_shareable_projection collaborator; no foreign row lock | evidence_role insert only; no update/delete |
| evidence.evidence_event | evidence | V008 | S3 | none; deferred creation-cycle verification inside the evidence owner | evidence_role insert only; no update/delete |
| sharing.evidence_share_grant | sharing | V009 | S3 | none; creator/workspace authorization uses S1 exact reads | sharing_role; owner transitions and revisions only |
| sharing.share_admission | sharing | V009 | S3 | none; admission is the authorization fact | sharing_role insert only; no update/delete |
| sharing.share_access_event | sharing | V009 | S3 | none; durable access/ticket facts | sharing_role insert only; no update/delete |

Collaborators are not co-owners. They may read stable columns and call a published service; they receive no write privilege. No module writes another module's table directly. In S2, `ivr` owns `ivr.ivr_decision` and writes it only through `ivr.select_call_action`; `activation-consent` and `entitlement-metering` never write it, and `ivr` never writes consent, activation, trial, grant, balance, reservation or usage tables directly. This matrix is conceptual ownership shape, not a grant list: entries marked `SELECT` name only the exact columns granted in §10.12, while entries marked `fact handoff` receive owner-validated facts through a published owner service and hold no table privilege, no row lock and no write path. Do not infer a grant from a collaborator name.

In S3, the `evidence` owner is the only writer of `evidence.*`; the `sharing` owner is the only writer of `sharing.*`. Sharing reads evidence status/content through the exact §10.21 projection and never writes evidence. Evidence does not read or write sharing. IVR and media workers submit validated facts to the evidence owner service; they do not own or update evidence lifecycle rows. No S1/S2 role receives any S3 table privilege, and no S3 role writes another module's table.

### 3.3 Identity and workspace answers

**Which identity can log in?** Only an active `customer_iam.auth_identity` row whose `provider` is `WECHAT` or `PHONE`, whose `verified_at` is set, and whose parent `customer_iam.customer_account` is `ACTIVE`. The identity is resolved by `(provider, provider_scope, provider_subject_hmac)` and maps to exactly one account. A business-number binding, subject claim, organization membership, organization, business subject or employee account is never a login identity. Operations employees authenticate only with `operations_iam.employee_account` in their separate issuer domain; they do not use customer identities.

**Uniqueness, unbind and replace races.** The unique provider/subject HMAC key serializes identity creation. A concurrent insert loser reads the winner: the same account is an idempotent success, a different account is a stable `IDENTITY_CONFLICT`; no account merge or identity migration is performed. Disabling an identity is a conditional revision update; an identity cannot be rebound to another account. A phone number can be both a login identity and a business number only when separate `auth_identity` and `caller_number_binding` rows are each created through their own flow; neither row infers the other.

**Workspace selection and access.** The personal workspace is derived from an active customer account; it has no separate table and always exists for an active account. Organization workspaces are derived from `organization_access.organization_membership` rows with `status = 'ACTIVE'` whose organization is `ACTIVE`. The server supplies the workspace list and role; the client cannot submit a role, account, organization or workspace claim. A disabled account cannot log in; a disabled or suspended organization and a revoked or suspended membership do not appear as usable workspaces. Revocation removes only that organization's access and never deletes the natural-person account or personal space. Role changes are server-side conditional updates with revision and audit; users cannot self-elevate.

**Business numbers, claims and login.** Binding or claiming a business number never creates an `auth_identity`, never changes a customer account's login identities, never changes subject real-name status, and never rewrites evidence, trial or usage facts. An IVR-created subject may be unowned and unverified: both `business_subject` owner columns may be null, `real_name_status` may remain `UNVERIFIED`, and a binding may exist before any account or subject claim. Such a subject can later be claimed by a verified personal account; a personal claim sets the owner only when null and conflicts when a different owner exists. An organization-scoped claim is fail-closed in S1 until the deferred enterprise ownership policy is approved, and it never chooses personal versus organization ownership from an optional workspace ID.

## 4. Complete Table Dictionary

### 4.1 Frozen planned table catalog (all 44 tables)

The following catalog is the accepted 44-table planned baseline. It is verbatim from the accepted plan. Status is recorded outside the block: rows 1-13 are expanded as S1, rows 14-36 as S2 and rows 37-44 as S3 in this document. The block itself is unchanged.

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
| `ivr.call_record` | `ivr` | V005 | S2 | `id`, `call_id`, `session_id`, `task_id`, `subject_id`, `caller_ciphertext`, `caller_number_hmac`, `caller_masked`, `callee_ciphertext`, `callee_masked`, `access_number_masked`, `status`, `node_id`, `protocol_version`, `started_at`, `bridged_at`, `recording_started_at`, `recording_ended_at`, `ended_at`, `authoritative_seconds`, `end_reason`, `created_at`, `revision` | Unique `call_id` and `session_id`; unique non-null `task_id`; partial unique active `caller_number_hmac` for `ACTIVE/UNKNOWN`; canonical call-scoped FK parent; a live call is created or bound as `ACTIVE` before a non-terminal decision/reservation, while a call pre-empted by another active call is created terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS` so its `REJECT_CALL_IN_PROGRESS` decision still has a parent; the other-active-call guard excludes the current `callId`; only a never-started/rejectable current `ACTIVE` row may be finalized `REJECTED`, while a fresh attempt on a current `UNKNOWN` call with inconsistent line state returns a stable reconciliation/integrity refusal and never finalizes `UNKNOWN` to `REJECTED` or releases it; a terminal reject touches only the current call's own row and never an unrelated competing call; the partial unique index is a backstop; party fields are ciphertext/masked and immutable; mutable head |
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

Every S1 and S2 dictionary below is complete for the accepted column set. "Default" means the database default used when the application omits the column; "none" means the application must supply the value. Caller-owned rows use an application-generated UUID with no database default. The function-inserted row types listed in §2.2 and §2.11 generate `id` with `pg_catalog.gen_random_uuid()` inside the function body; no table default and no function `p_id` argument exists. Every foreign key is `ON UPDATE RESTRICT ON DELETE RESTRICT`. Hash columns enforce lowercase 64-character SHA-256 or HMAC-SHA-256 hexadecimal text unless a narrower rule is stated. Every `request_fingerprint` is canonical semantic SHA-256 as defined in §2.5. S1 dictionaries are §4.3-§4.15; S2 dictionaries are §4.16-§4.38.

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
| risk_checked_at | timestamptz | NO | none | none | Database claim-decision timestamp. For a fresh predicate rejection before any risk read it is the claim-decision time and the audit snapshot records `riskEvaluation = NOT_REQUIRED`; for a path that reads risk it is the actual current risk-decision time. It is never a proof-attempt time and never claims an unperformed risk lookup. |
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

### 4.16 catalog_order.catalog_product

Purpose: immutable stable product identity. Owner: `catalog-order`. Wave V004; slice S2. Lifecycle: append-only/frozen; no draft status and no product draft-delete exception. Deletion: no update/delete privilege; the product is never hard-deleted.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_catalog_product` | Application-generated stable identity |
| product_code | text | NO | none | UNIQUE `uq_catalog_product_code`; CHECK `ck_catalog_product_code` | Stable server-owned code, length 2-64, uppercase code shape |
| product_family | text | NO | none | CHECK `ck_catalog_product_family` | Stable family code, length 1-64; display copy is not stored here |
| created_at | timestamptz | NO | `now()` | none | Creation time |

Named checks:

- `ck_catalog_product_code`: `product_code ~ '^[A-Z][A-Z0-9_-]{1,63}$'`.
- `ck_catalog_product_family`: `product_family ~ '^[A-Z][A-Z0-9_]{0,63}$'`.

Keys and indexes:

- Primary key `pk_catalog_product (id)`.
- Unique `uq_catalog_product_code (product_code)`.
- Index `ix_catalog_product_family (product_family, created_at)`.
- No foreign key.

Append-only control: `catalog_order.reject_row_mutation()` trigger `trg_catalog_product_immutable` rejects update/delete. `catalog_order_role` has `SELECT` and `INSERT` only. Product identity never changes; a new product requires a new row.

Sensitive representation: no sensitive column; `product_code` is an internal catalog code, not a public key.

### 4.17 catalog_order.catalog_version

Purpose: immutable published commercial version with mutable sale lifecycle. Owner: `catalog-order`. Wave V004; slice S2. Lifecycle: mutable head (`sale_status`, `revision`); commercial content and snapshot are immutable after publication. Deletion: no hard delete; only a never-published, unreferenced draft is a future deletion candidate, not implemented in this baseline.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_catalog_version` | Application-generated identity |
| product_id | uuid | NO | none | FK `fk_catalog_version_product`; part of `uq_catalog_version_no` and `uq_catalog_version_active` | Stable product identity |
| version_no | integer | NO | none | CHECK `ck_catalog_version_no`; part of `uq_catalog_version_no` | Positive monotonic version within the product |
| sale_status | text | NO | `'DRAFT'` | CHECK `ck_catalog_version_sale_status` | `DRAFT`, `ACTIVE` or `OFFSALE` |
| offer_type | text | NO | none | CHECK `ck_catalog_version_offer_type` | `SUBSCRIPTION`, `ONE_TIME`, `ADD_ON` or `QUOTE` |
| audience | text | NO | none | CHECK `ck_catalog_version_audience`; part of `ck_catalog_version_audience_shape` | `PERSONAL` or `ENTERPRISE` |
| price_cents | bigint | YES | none | CHECK `ck_catalog_version_price` | Server-computed integer cents; NULL only for `QUOTE` |
| currency | text | NO | none | CHECK `ck_catalog_version_currency` | Uppercase ISO-4217 code |
| billing_cycle | text | NO | none | CHECK `ck_catalog_version_billing_matrix` | `MONTHLY`, `QUARTERLY`, `ANNUAL`, `ONCE` or `QUOTE` |
| installments | integer | NO | none | CHECK `ck_catalog_version_billing_matrix` | Positive installment count; `1` for non-subscription |
| grant_interval_months | integer | NO | none | CHECK `ck_catalog_version_billing_matrix` | Positive for subscriptions; `0` otherwise |
| snapshot | jsonb | NO | none | CHECK `ck_catalog_version_snapshot_type` | Canonical commercial snapshot |
| snapshot_sha256 | text | NO | none | CHECK `ck_catalog_version_snapshot_sha` | SHA-256 of the canonical redacted snapshot |
| created_by_employee_id | uuid | NO | none | FK `fk_catalog_version_employee` | Operations employee that created the draft |
| created_at | timestamptz | NO | `now()` | none | Draft creation time |
| published_at | timestamptz | YES | none | CHECK `ck_catalog_version_published_shape` | Non-null exactly when the version is no longer `DRAFT`; set once |
| revision | integer | NO | `1` | CHECK `ck_catalog_version_revision` | Conditional-update revision |

Named checks:

- `ck_catalog_version_no`: `version_no >= 1`.
- `ck_catalog_version_sale_status`: `sale_status IN ('DRAFT','ACTIVE','OFFSALE')`.
- `ck_catalog_version_offer_type`: `offer_type IN ('SUBSCRIPTION','ONE_TIME','ADD_ON','QUOTE')`.
- `ck_catalog_version_audience`: `audience IN ('PERSONAL','ENTERPRISE')`.
- `ck_catalog_version_audience_shape`: `(audience = 'ENTERPRISE') = (offer_type = 'QUOTE')`.
- `ck_catalog_version_price`: `(price_cents IS NULL) = (offer_type = 'QUOTE') AND (price_cents IS NULL OR price_cents >= 0)`.
- `ck_catalog_version_currency`: `currency ~ '^[A-Z]{3}$'`.
- `ck_catalog_version_billing_matrix`: `(offer_type = 'SUBSCRIPTION' AND billing_cycle IN ('MONTHLY','QUARTERLY','ANNUAL') AND installments >= 1 AND grant_interval_months >= 1) OR (offer_type IN ('ONE_TIME','ADD_ON') AND billing_cycle = 'ONCE' AND installments = 1 AND grant_interval_months = 0) OR (offer_type = 'QUOTE' AND billing_cycle = 'QUOTE' AND installments = 1 AND grant_interval_months = 0 AND price_cents IS NULL)`.
- `ck_catalog_version_snapshot_type`: `jsonb_typeof(snapshot) = 'object'`.
- `ck_catalog_version_snapshot_sha`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_catalog_version_published_shape`: `(sale_status = 'DRAFT') = (published_at IS NULL)`.
- `ck_catalog_version_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_catalog_version (id)`.
- Unique `uq_catalog_version_no (product_id, version_no)`.
- Partial unique `uq_catalog_version_active (product_id) WHERE sale_status = 'ACTIVE'` — at most one active version per product.
- Index `ix_catalog_version_product_status (product_id, sale_status, version_no DESC)`.
- FK `fk_catalog_version_product (product_id) REFERENCES catalog_order.catalog_product(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_catalog_version_employee (created_by_employee_id) REFERENCES operations_iam.employee_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `product_id`, `version_no`, offer/audience/price/currency/billing/installment/grant columns, `snapshot`, `snapshot_sha256`, `created_by_employee_id`, `created_at` and `published_at` are immutable once `published_at` is set. `catalog_order.guard_catalog_version_published` rejects any such change after publication. Draft content and `sale_status` use conditional revision updates. Publication is the §9.26 serialized owner transaction: under `catalog_order.publication:<product_id>` it first retires the previous `ACTIVE` version to `OFFSALE` with revision +1, then sets the candidate `published_at` and `ACTIVE` with revision +1, and appends audit/Outbox in one transaction. Retiring the old current version first is mandatory because `uq_catalog_version_active` is an immediate partial unique index; the transaction must never attempt to activate the candidate while the previous version still qualifies, and no constraint deferral or temporary second `ACTIVE` row is allowed. A caller whose expected current active version/revision is stale returns a stable `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW` and changes nothing; a concurrent loser never receives a raw unique violation and never creates a second `ACTIVE` version. `ACTIVE → OFFSALE` uses the same serialization and never rewrites an order, consent, grant or issuance row. Only a `DRAFT` with `published_at IS NULL` that is referenced by no offer or order is a future deletion candidate; no deletion exists in this baseline.

Sensitive representation: `snapshot` carries commercial terms only and is redacted before hashing; no protected value. `snapshot_sha256` is a hash, not a public key.

### 4.18 catalog_order.catalog_offer

Purpose: channel-specific sale offer for one catalog version. Owner: `catalog-order`. Wave V004; slice S2. Lifecycle: mutable head; withdrawal stops new orders only. Deletion: no hard delete; status retirement only.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_catalog_offer` | Application-generated identity |
| catalog_version_id | uuid | NO | none | FK `fk_catalog_offer_version`; part of `uq_catalog_offer_version_channel` | Parent commercial version |
| channel | text | NO | none | CHECK `ck_catalog_offer_channel`; part of `uq_catalog_offer_version_channel` | `MINIAPP` or `WEB` |
| offer_status | text | NO | `'ACTIVE'` | CHECK `ck_catalog_offer_status` | `ACTIVE` or `WITHDRAWN` |
| version_token | text | NO | none | CHECK `ck_catalog_offer_token` | Content/channel-specific token derived server-side; required by order creation |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| revision | integer | NO | `1` | CHECK `ck_catalog_offer_revision` | Conditional-update revision |

Named checks:

- `ck_catalog_offer_channel`: `channel IN ('MINIAPP','WEB')`.
- `ck_catalog_offer_status`: `offer_status IN ('ACTIVE','WITHDRAWN')`.
- `ck_catalog_offer_token`: `length(version_token) BETWEEN 1 AND 128 AND btrim(version_token) <> ''`.
- `ck_catalog_offer_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_catalog_offer (id)`.
- Unique `uq_catalog_offer_version_channel (catalog_version_id, channel)`.
- Index `ix_catalog_offer_channel_status (channel, offer_status)`.
- FK `fk_catalog_offer_version (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `catalog_version_id` and `channel` are immutable; `catalog_order.guard_catalog_offer` rejects their change. `offer_status`, `version_token` and `revision` use conditional updates. A withdrawn offer blocks new orders but never invalidates an existing order snapshot. `version_token` changes when content changes; it is never accepted from the client without comparison to the stored active offer. This S2 specification does not implement or claim an independent `catalog_offer` withdrawal endpoint or transaction; whether one exists is deferred to Product/Operations/catalog-order in the catalog operations OpenAPI work (OQ-4). Existing catalog-version `OFFSALE` already blocks new orders, and the accepted `ACTIVE`/`WITHDRAWN` state set is unchanged.

Sensitive representation: no sensitive column; the token is a server-derived business token, not a credential or capability token.

### 4.19 catalog_order.catalog_benefit_line

Purpose: immutable benefit line for one catalog version. Owner: `catalog-order`. Wave V004; slice S2. Lifecycle: append-only/frozen with the version. Deletion: no update/delete; never hard-deleted.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_catalog_benefit_line` | Application-generated identity |
| catalog_version_id | uuid | NO | none | FK `fk_catalog_benefit_version`; part of `uq_catalog_benefit_version_type` | Parent commercial version |
| entitlement_type | text | NO | none | CHECK `ck_catalog_benefit_type`; part of `uq_catalog_benefit_version_type` | Baseline registry in §2.9 |
| quantity | bigint | NO | none | CHECK `ck_catalog_benefit_quantity` | Positive granted quantity in the type's unit |
| validity_days | integer | NO | none | CHECK `ck_catalog_benefit_validity` | Positive validity days |
| grant_interval_months | integer | NO | none | CHECK `ck_catalog_benefit_interval` | Non-negative grant interval; `0` when not installmented |

Named checks:

- `ck_catalog_benefit_type`: `entitlement_type IN ('RECORDING_SECONDS','AI_SECONDS','EVIDENCE_COUNT','STORAGE_BYTES')`.
- `ck_catalog_benefit_quantity`: `quantity >= 1`.
- `ck_catalog_benefit_validity`: `validity_days >= 1`.
- `ck_catalog_benefit_interval`: `grant_interval_months >= 0`.

Keys and indexes:

- Primary key `pk_catalog_benefit_line (id)`.
- Unique `uq_catalog_benefit_version_type (catalog_version_id, entitlement_type)`.
- Index `ix_catalog_benefit_version (catalog_version_id)`.
- FK `fk_catalog_benefit_version (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_catalog_benefit_line_immutable` uses `catalog_order.reject_row_mutation()`; `catalog_order_role` has `SELECT` and `INSERT` only. Lines are immutable with their version and are never rewritten by a later catalog publication.

Sensitive representation: no sensitive column.

### 4.20 catalog_order.customer_order

Purpose: immutable purchase snapshot with a mutable payment lifecycle. Owner: `catalog-order`. Wave V004; slice S2. Lifecycle: mutable head for payment status/terminal time only; commercial snapshot, consent reference, fingerprint and amount are immutable. Deletion: no hard delete; payment-lifetime retirement only.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_customer_order` | Application-generated order identity |
| order_no | text | NO | none | UNIQUE `uq_customer_order_no`; CHECK `ck_customer_order_no` | Opaque server-generated order number, length 1-64 |
| account_id | uuid | NO | none | FK `fk_customer_order_account`; part of `uq_customer_order_idempotency` | Purchasing natural-person account |
| owner_organization_id | uuid | YES | none | FK `fk_customer_order_organization` | Organization workspace when organization-scoped |
| subject_id | uuid | NO | none | FK `fk_customer_order_subject` | Business subject whose activation/trial is purchased |
| channel | text | NO | none | CHECK `ck_customer_order_channel` | `MINIAPP` or `WEB` only; IVR cannot create an order |
| catalog_version_id | uuid | NO | none | FK `fk_customer_order_version` | Exact frozen commercial version |
| catalog_offer_id | uuid | NO | none | FK `fk_customer_order_offer` | Exact channel offer |
| purchase_consent_id | uuid | NO | none | FK `fk_customer_order_consent` added by `ALTER` in V007 | Explicit purchase consent; the column exists in V004 without the FK |
| amount_cents | bigint | NO | none | CHECK `ck_customer_order_amount` | Server-derived integer cents, never client-supplied |
| currency | text | NO | none | CHECK `ck_customer_order_currency` | Uppercase ISO-4217 code |
| payment_status | text | NO | `'UNPAID'` | CHECK `ck_customer_order_payment_status` | `UNPAID`, `UNKNOWN`, `PAID` or `CLOSED`; payment only, never entitlement |
| snapshot | jsonb | NO | none | CHECK `ck_customer_order_snapshot_type` | Exact frozen order snapshot |
| snapshot_sha256 | text | NO | none | CHECK `ck_customer_order_snapshot_sha` | SHA-256 of the canonical redacted snapshot |
| version_token | text | NO | none | CHECK `ck_customer_order_version_token` | The exact token accepted at creation |
| idempotency_key | text | NO | none | CHECK `ck_customer_order_idempotency`; part of `uq_customer_order_idempotency` | Client key, length 1-475; the derived platform key is `account_id || ':' || idempotency_key`, so a canonical 36-character account UUID plus the colon fits the platform 512-character limit (36 + 1 + 475 = 512) |
| request_fingerprint | text | NO | none | CHECK `ck_customer_order_fingerprint` | Canonical semantic purchase fingerprint |
| revision | integer | NO | `1` | CHECK `ck_customer_order_revision` | Conditional-update revision |
| created_at | timestamptz | NO | `now()` | none | Order creation time |
| paid_at | timestamptz | YES | none | CHECK `ck_customer_order_payment_shape` | Non-null exactly when `payment_status = 'PAID'` |
| closed_at | timestamptz | YES | none | CHECK `ck_customer_order_payment_shape` | Non-null exactly when `payment_status = 'CLOSED'` |

Named checks:

- `ck_customer_order_no`: `length(order_no) BETWEEN 1 AND 64 AND btrim(order_no) <> ''`.
- `ck_customer_order_channel`: `channel IN ('MINIAPP','WEB')`.
- `ck_customer_order_amount`: `amount_cents >= 0`.
- `ck_customer_order_currency`: `currency ~ '^[A-Z]{3}$'`.
- `ck_customer_order_payment_status`: `payment_status IN ('UNPAID','UNKNOWN','PAID','CLOSED')`.
- `ck_customer_order_payment_shape`: `((payment_status = 'PAID') = (paid_at IS NOT NULL)) AND ((payment_status = 'CLOSED') = (closed_at IS NOT NULL))`.
- `ck_customer_order_time_order`: `(paid_at IS NULL OR paid_at >= created_at) AND (closed_at IS NULL OR closed_at >= created_at)`.
- `ck_customer_order_snapshot_type`: `jsonb_typeof(snapshot) = 'object'`.
- `ck_customer_order_snapshot_sha`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_customer_order_version_token`: `length(version_token) BETWEEN 1 AND 128 AND btrim(version_token) <> ''`.
- `ck_customer_order_idempotency`: `length(idempotency_key) BETWEEN 1 AND 475 AND btrim(idempotency_key) <> ''`.
- `ck_customer_order_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.
- `ck_customer_order_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_customer_order (id)`.
- Unique `uq_customer_order_no (order_no)`.
- Unique `uq_customer_order_idempotency (account_id, idempotency_key)`.
- Index `ix_customer_order_subject_status (subject_id, payment_status, created_at DESC)`.
- Index `ix_customer_order_organization (owner_organization_id, created_at DESC)`.
- Index `ix_customer_order_version (catalog_version_id)`.
- FK `fk_customer_order_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_customer_order_organization (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_customer_order_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_customer_order_version (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_customer_order_offer (catalog_offer_id) REFERENCES catalog_order.catalog_offer(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_customer_order_consent (purchase_consent_id) REFERENCES activation_consent.consent_record(id) ON UPDATE RESTRICT ON DELETE RESTRICT` — added by `ALTER TABLE` in V007; ordinary immediate, not deferrable.
- No entitlement-status column. Order-level entitlement is a read-only computed projection over the `entitlement_metering.entitlement_issuance` rows for the order (inputs: `order_id`, `installment_no`, `entitlement_type`, `status`, `grant_id`, `issued_at`, `last_error_code`) joined to `entitlement_metering.entitlement_grant` (`id`, `subject_id`, `entitlement_type`, `granted_units`, `valid_from`, `expires_at`) by `grant_id = grant.id`; `catalog_order_role` receives only those exact `SELECT` columns plus schema `USAGE` (§10.12), with no write and no row lock. The projection is never stored on the order.

Revision/immutability: `order_no`, `account_id`, `owner_organization_id`, `subject_id`, `channel`, `catalog_version_id`, `catalog_offer_id`, `purchase_consent_id`, `amount_cents`, `currency`, `snapshot`, `snapshot_sha256`, `version_token`, `idempotency_key`, `request_fingerprint` and `created_at` are immutable; `catalog_order.guard_customer_order_snapshot` rejects any change. Only `payment_status`, `paid_at`, `closed_at`, `revision` may change, under the order row lock and verified payment evidence. The order is created only from `MINIAPP`/`WEB` after server-side account/workspace/subject access, ordinary-client real-name, current active activation, explicit current purchase consent (§9.17, §10.20), an exact ACTIVE offer/version token and server amount computation. The client key is validated at 1-475 characters before any mutation; a length of 476-512 is rejected with `IDEMPOTENCY_ARGUMENT_INVALID` (or the stable order-argument code) and creates no order, consent or idempotency row. `platform.idempotency_begin('PURCHASE', account_id || ':' || idempotency_key, request_fingerprint)` and `complete(..., 'ORDER', orderId, redactedOrderSnapshot, snapshotSha256)` run in the same transaction; identical retry returns the first immutable order, changed input conflicts. A same-key retry checks platform replay before appending a second purchase consent.

Sensitive representation: `snapshot` is historical commercial data and is redacted or encrypted for any protected value before hashing; `request_fingerprint` and `snapshot_sha256` are hashes, not public keys. No full number, callee or identity value is stored.

### 4.21 catalog_order.payment_order

Purpose: payment attempt for one customer order with independently observable status. Owner: `catalog-order`. Wave V004; slice S2. Lifecycle: mutable head driven only by verified provider evidence. Deletion: no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_payment_order` | Application-generated identity |
| order_id | uuid | NO | none | UNIQUE `uq_payment_order_order`; FK `fk_payment_order_order` | Exactly one payment order per customer order in the baseline |
| provider | text | NO | none | CHECK `ck_payment_order_provider` | Baseline `WECHAT`; a new provider requires an approved migration |
| merchant_order_no | text | NO | none | UNIQUE `uq_payment_order_merchant`; CHECK `ck_payment_order_merchant_no` | Provider-scoped merchant order number |
| provider_transaction_id | text | YES | none | Partial UNIQUE `uq_payment_order_transaction`; CHECK `ck_payment_order_transaction` | Provider transaction identity when known |
| status | text | NO | `'UNPAID'` | CHECK `ck_payment_order_status`; `ck_payment_order_paid_shape` | `UNPAID`, `UNKNOWN`, `PAID` or `CLOSED` |
| amount_cents | bigint | NO | none | CHECK `ck_payment_order_amount` | Must equal the frozen order amount |
| currency | text | NO | none | CHECK `ck_payment_order_currency` | Uppercase ISO-4217 code |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| updated_at | timestamptz | NO | `now()` | CHECK `ck_payment_order_time_order` | Last verified status update |
| revision | integer | NO | `1` | CHECK `ck_payment_order_revision` | Conditional-update revision |

Named checks:

- `ck_payment_order_provider`: `provider IN ('WECHAT')`.
- `ck_payment_order_merchant_no`: `length(merchant_order_no) BETWEEN 1 AND 64 AND btrim(merchant_order_no) <> ''`.
- `ck_payment_order_transaction`: `provider_transaction_id IS NULL OR (length(provider_transaction_id) BETWEEN 1 AND 128 AND btrim(provider_transaction_id) <> '')`.
- `ck_payment_order_status`: `status IN ('UNPAID','UNKNOWN','PAID','CLOSED')`.
- `ck_payment_order_paid_shape`: `status <> 'PAID' OR provider_transaction_id IS NOT NULL`.
- `ck_payment_order_amount`: `amount_cents >= 0`.
- `ck_payment_order_currency`: `currency ~ '^[A-Z]{3}$'`.
- `ck_payment_order_time_order`: `updated_at >= created_at`.
- `ck_payment_order_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_payment_order (id)`.
- Unique `uq_payment_order_order (order_id)`.
- Unique `uq_payment_order_merchant (merchant_order_no)`.
- Partial unique `uq_payment_order_transaction (provider_transaction_id) WHERE provider_transaction_id IS NOT NULL`.
- Index `ix_payment_order_status_time (status, updated_at)`.
- FK `fk_payment_order_order (order_id) REFERENCES catalog_order.customer_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `order_id`, `provider`, `merchant_order_no`, `amount_cents`, `currency` and `created_at` are immutable; `catalog_order.guard_payment_order_identity` rejects their change. `status`, `provider_transaction_id`, `updated_at` and `revision` change only from verified provider evidence under the order/payment row lock. `UNKNOWN` blocks duplicate checkout until the original order is queried; `PAID` and `CLOSED` are terminal for the ordinary flow. Payment status never implies entitlement issuance.

Sensitive representation: `merchant_order_no` and `provider_transaction_id` are provider-scoped internal identifiers; they are never returned as public keys, and access is audited.

### 4.22 catalog_order.payment_event

Purpose: append-only verified/unverified provider payment event. Owner: `catalog-order`. Wave V004; slice S2. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_payment_event` | Application-generated identity |
| payment_order_id | uuid | NO | none | FK `fk_payment_event_order` | Parent payment order |
| provider | text | NO | none | CHECK `ck_payment_event_provider`; part of `uq_payment_event_provider_event` | Baseline `WECHAT` |
| provider_event_id | text | NO | none | CHECK `ck_payment_event_event_id`; part of `uq_payment_event_provider_event` | Unique within the provider event domain |
| event_type | text | NO | none | CHECK `ck_payment_event_type` | Stable provider event code, length 1-64 |
| signature_verified | boolean | NO | `false` | none | Only verified events may advance payment |
| payload_ciphertext | text | NO | none | CHECK `ck_payment_event_payload_ciphertext` | Encrypted raw provider payload |
| payload_sha256 | text | NO | none | CHECK `ck_payment_event_payload_sha` | SHA-256 of the stored encrypted/redacted payload |
| transaction_id | text | YES | none | CHECK `ck_payment_event_transaction` | Provider transaction identity when supplied |
| amount_cents | bigint | NO | none | CHECK `ck_payment_event_amount` | Provider-reported amount in cents |
| occurred_at | timestamptz | NO | none | none | Provider event time; audit only |
| received_at | timestamptz | NO | `now()` | none | Server receipt time |

Named checks:

- `ck_payment_event_provider`: `provider IN ('WECHAT')`.
- `ck_payment_event_event_id`: `length(provider_event_id) BETWEEN 1 AND 128 AND btrim(provider_event_id) <> ''`.
- `ck_payment_event_type`: `length(event_type) BETWEEN 1 AND 64 AND btrim(event_type) <> ''`.
- `ck_payment_event_payload_ciphertext`: `payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_payment_event_payload_sha`: `payload_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_payment_event_transaction`: `transaction_id IS NULL OR (length(transaction_id) BETWEEN 1 AND 128 AND btrim(transaction_id) <> '')`.
- `ck_payment_event_amount`: `amount_cents >= 0`.

Keys and indexes:

- Primary key `pk_payment_event (id)`.
- Unique `uq_payment_event_provider_event (provider, provider_event_id)`.
- Index `ix_payment_event_order_time (payment_order_id, received_at DESC)`.
- Partial index `ix_payment_event_verified (payment_order_id, received_at) WHERE signature_verified`.
- FK `fk_payment_event_order (payment_order_id) REFERENCES catalog_order.payment_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_payment_event_immutable` uses `catalog_order.reject_row_mutation()`; `catalog_order_role` has `SELECT` and `INSERT` only. Multiple verified events may advance one order; an unverified event is retained but never advances payment. Duplicate `(provider, provider_event_id)` returns the stored first processing result at the service layer, not a second row.

Sensitive representation: `payload_ciphertext` is encrypted; `payload_sha256` is a hash. Provider event/transaction IDs are internal and never client-visible public keys.

### 4.23 ivr.outbound_call_task

Purpose: short-lived single-slot outbound dial task created by the mini-program. Owner: `ivr`. Wave V005; slice S2. Lifecycle: mutable head until terminal; terminal rows are immutable. Deletion: no hard delete; expiry/replacement/cancellation retain history.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_outbound_call_task` | Application-generated identity |
| task_id | text | NO | none | UNIQUE `uq_outbound_call_task_task_id`; CHECK `ck_outbound_task_task_id` | Opaque server-generated protocol ID, length 1-64 |
| caller_binding_id | uuid | NO | none | FK `fk_outbound_task_binding` | Expected business-number binding |
| subject_id | uuid | NO | none | FK `fk_outbound_task_subject` | Stable subject |
| owner_account_id | uuid | YES | none | FK `fk_outbound_task_owner_account`; part of `ck_outbound_task_single_owner` | Personal owner |
| owner_organization_id | uuid | YES | none | FK `fk_outbound_task_owner_org`; part of `ck_outbound_task_single_owner` | Organization owner |
| created_by_account_id | uuid | NO | none | FK `fk_outbound_task_creator`; part of `uq_outbound_task_idempotency` | Actual creating account |
| caller_number_hmac | text | NO | none | CHECK `ck_outbound_task_caller_hmac`; partial unique `uq_outbound_task_live_caller` | Canonical caller HMAC |
| callee_ciphertext | text | NO | none | CHECK `ck_outbound_task_callee_ciphertext` | Encrypted callee; never logged or returned by ordinary APIs |
| callee_masked | text | NO | none | CHECK `ck_outbound_task_callee_masked` | Display-only masked callee |
| business_type | text | NO | none | CHECK `ck_outbound_task_business_type` | Approved call-business code, length 1-64; baseline `EVIDENCE_CALL` |
| status | text | NO | `'PENDING'` | CHECK `ck_outbound_task_status`; `ck_outbound_task_terminal_shape` | `PENDING`, `CLAIMED`, `REPLACED`, `CANCELLED`, `EXPIRED`, `REJECTED`, `COMPLETED` or `FAILED` |
| reason_code | text | YES | none | CHECK `ck_outbound_task_reason` | Stable reason code when applicable |
| ttl_seconds | integer | NO | `30` | CHECK `ck_outbound_task_ttl` | Server-configurable default 30; frozen at creation |
| expires_at | timestamptz | NO | none | CHECK `ck_outbound_task_expiry` | Database absolute expiry computed at creation |
| idempotency_key | text | NO | none | CHECK `ck_outbound_task_idempotency_key`; part of `uq_outbound_task_idempotency` | Client idempotency key |
| request_fingerprint | text | NO | none | CHECK `ck_outbound_task_fingerprint` | Canonical task-request fingerprint |
| claimed_call_id | text | YES | none | FK `fk_outbound_task_claimed_call` added by `ALTER` in V005; UNIQUE `uq_outbound_task_claimed_call` | Set once on a successful task claim; the reciprocal `call_record.task_id` must equal this task's `id` |
| claimed_at | timestamptz | YES | none | CHECK `ck_outbound_task_claimed_shape` | Non-null exactly when `claimed_call_id` is set |
| terminal_at | timestamptz | YES | none | CHECK `ck_outbound_task_terminal_shape` | Non-null exactly for a terminal status |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| updated_at | timestamptz | NO | `now()` | CHECK `ck_outbound_task_time_order` | Last state update |
| revision | integer | NO | `1` | CHECK `ck_outbound_task_revision` | Conditional-update revision |

Named checks:

- `ck_outbound_task_task_id`: `length(task_id) BETWEEN 1 AND 64 AND btrim(task_id) <> ''`.
- `ck_outbound_task_single_owner`: `num_nonnulls(owner_account_id, owner_organization_id) = 1`.
- `ck_outbound_task_caller_hmac`: `caller_number_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_outbound_task_callee_ciphertext`: `callee_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_outbound_task_callee_masked`: `length(callee_masked) BETWEEN 1 AND 64 AND btrim(callee_masked) <> ''`.
- `ck_outbound_task_business_type`: `length(business_type) BETWEEN 1 AND 64 AND btrim(business_type) <> ''`.
- `ck_outbound_task_status`: `status IN ('PENDING','CLAIMED','REPLACED','CANCELLED','EXPIRED','REJECTED','COMPLETED','FAILED')`.
- `ck_outbound_task_reason`: `reason_code IS NULL OR (length(reason_code) BETWEEN 1 AND 64 AND btrim(reason_code) <> '')`.
- `ck_outbound_task_ttl`: `ttl_seconds >= 1`.
- `ck_outbound_task_expiry`: `expires_at > created_at`.
- `ck_outbound_task_idempotency_key`: `length(idempotency_key) BETWEEN 1 AND 512 AND btrim(idempotency_key) <> ''`.
- `ck_outbound_task_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.
- `ck_outbound_task_claimed_shape`: `(claimed_call_id IS NULL) = (claimed_at IS NULL)`.
- `ck_outbound_task_terminal_shape`: `(status IN ('PENDING','CLAIMED')) = (terminal_at IS NULL)`.
- `ck_outbound_task_time_order`: `updated_at >= created_at AND (terminal_at IS NULL OR terminal_at >= created_at)`.
- `ck_outbound_task_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_outbound_call_task (id)`.
- Unique `uq_outbound_call_task_task_id (task_id)`.
- Unique `uq_outbound_task_idempotency (created_by_account_id, idempotency_key)`.
- Partial unique `uq_outbound_task_live_caller (caller_number_hmac) WHERE status IN ('PENDING','CLAIMED')` — one non-terminal task per canonical caller across all accounts/workspaces.
- Unique `uq_outbound_task_claimed_call (claimed_call_id) WHERE claimed_call_id IS NOT NULL`.
- Index `ix_outbound_task_subject_status (subject_id, status, created_at DESC)`.
- Index `ix_outbound_task_org_status (owner_organization_id, status, created_at DESC)`.
- Index `ix_outbound_task_creator_status (created_by_account_id, status)`.
- FK `fk_outbound_task_binding (caller_binding_id) REFERENCES subject_number.caller_number_binding(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_outbound_task_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_outbound_task_owner_account (owner_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_outbound_task_owner_org (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_outbound_task_creator (created_by_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_outbound_task_claimed_call (claimed_call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT` — added by `ALTER TABLE` in V005 after `call_record` exists; ordinary immediate, not deferrable.

Revision/immutability: `task_id`, `caller_binding_id`, `subject_id`, owner columns, `created_by_account_id`, `caller_number_hmac`, `callee_ciphertext`, `callee_masked`, `business_type`, `ttl_seconds`, `expires_at`, `idempotency_key`, `request_fingerprint` and `created_at` are immutable; `ivr.guard_outbound_call_task` rejects any change. `status`, `reason_code`, `claimed_call_id`, `claimed_at`, `terminal_at`, `updated_at` and `revision` use conditional updates. The default `ttl_seconds` is 30 and server-configurable, but a duplicate, retry or replacement never refreshes an existing row's `expires_at`; a replacement creates a new row and marks the old one `REPLACED`. `CLAIMED` can never be replaced or cancelled and never returns to `PENDING`. The reciprocal link is exact: a successful claim sets `claimed_call_id` and the current call's `task_id` in the same tentative branch, requires `outbound_call_task.claimed_call_id = call_record.call_id` and `call_record.task_id = outbound_call_task.id`, and rejects any arbitrary relink, task switch or second call; a category-2 rollback clears both assignments while retaining the pre-savepoint call.

Sensitive representation: `caller_number_hmac` is lookup-only; `callee_ciphertext` is encrypted; `callee_masked` is display-only. The full callee exists only in the authenticated IVR internal decision payload.

### 4.24 ivr.inbound_session

Purpose: immutable-origin IVR session for one `callId`, carrying trusted caller and confirmation state. Owner: `ivr`. Wave V005; slice S2. Lifecycle: mutable head for session status/confirmation; origin identity is immutable. Deletion: no hard delete; audit history retained.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_inbound_session` | Application-generated identity |
| call_id | text | NO | none | UNIQUE `uq_inbound_session_call`; CHECK `ck_inbound_session_call_id` | Root IVR protocol identity; no parent FK |
| subject_id | uuid | YES | none | FK `fk_inbound_session_subject` | Resolved subject when a canonical caller matches |
| trusted_caller | boolean | NO | none | CHECK `ck_inbound_session_caller_shape` | Whether the line supplied a trusted canonical caller |
| caller_ciphertext | text | YES | none | CHECK `ck_inbound_session_caller_ciphertext` | Encrypted caller when trusted |
| caller_number_hmac | text | YES | none | CHECK `ck_inbound_session_caller_hmac` | Canonical caller HMAC when trusted |
| caller_masked | text | YES | none | CHECK `ck_inbound_session_caller_masked` | Display-only masked caller |
| access_number_masked | text | NO | none | CHECK `ck_inbound_session_access_masked` | Masked access number; display-only, non-unique and non-authoritative for replay; no full access number |
| node_id | text | NO | none | CHECK `ck_inbound_session_node` | IVR node identity, length 1-64 |
| protocol_version | text | NO | none | CHECK `ck_inbound_session_protocol` | IVR protocol version, length 1-32 |
| prompt_version | text | NO | none | CHECK `ck_inbound_session_prompt` | Frozen prompt version, length 1-64 |
| status | text | NO | `'RECEIVED'` | CHECK `ck_inbound_session_status`; `ck_inbound_session_confirmed_shape` | `RECEIVED`, `CONFIRMED`, `ABANDONED` or `REJECTED` |
| confirmed_subject_id | uuid | YES | none | FK `fk_inbound_session_confirmed_subject` | Subject confirmed by key `1`; set only on confirmation |
| guidance_status | text | YES | none | CHECK `ck_inbound_session_guidance` | `ACTIVATION_REQUIRED` or `CONSENT_REQUIRED` when guidance was returned |
| created_at | timestamptz | NO | `now()` | none | Session creation time |
| confirmed_at | timestamptz | YES | none | CHECK `ck_inbound_session_confirmed_shape` | Non-null exactly when `CONFIRMED` |
| revision | integer | NO | `1` | CHECK `ck_inbound_session_revision` | Conditional-update revision |

Named checks:

- `ck_inbound_session_call_id`: `length(call_id) BETWEEN 1 AND 128 AND btrim(call_id) <> ''`.
- `ck_inbound_session_caller_shape`: `trusted_caller = FALSE OR (caller_ciphertext IS NOT NULL AND caller_number_hmac IS NOT NULL AND caller_masked IS NOT NULL)`.
- `ck_inbound_session_caller_ciphertext`: `caller_ciphertext IS NULL OR caller_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_inbound_session_caller_hmac`: `caller_number_hmac IS NULL OR caller_number_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_inbound_session_caller_masked`: `caller_masked IS NULL OR (length(caller_masked) BETWEEN 1 AND 64 AND btrim(caller_masked) <> '')`.
- `ck_inbound_session_access_masked`: `length(access_number_masked) BETWEEN 1 AND 64 AND btrim(access_number_masked) <> ''`.
- `ck_inbound_session_node`: `length(node_id) BETWEEN 1 AND 64 AND btrim(node_id) <> ''`.
- `ck_inbound_session_protocol`: `length(protocol_version) BETWEEN 1 AND 32 AND btrim(protocol_version) <> ''`.
- `ck_inbound_session_prompt`: `length(prompt_version) BETWEEN 1 AND 64 AND btrim(prompt_version) <> ''`.
- `ck_inbound_session_status`: `status IN ('RECEIVED','CONFIRMED','ABANDONED','REJECTED')`.
- `ck_inbound_session_guidance`: `guidance_status IS NULL OR guidance_status IN ('ACTIVATION_REQUIRED','CONSENT_REQUIRED')`.
- `ck_inbound_session_confirmed_shape`: `((status = 'CONFIRMED') = (confirmed_at IS NOT NULL)) AND (status <> 'CONFIRMED' OR (trusted_caller = TRUE AND confirmed_subject_id IS NOT NULL))`.
- `ck_inbound_session_time_order`: `confirmed_at IS NULL OR confirmed_at >= created_at`.
- `ck_inbound_session_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_inbound_session (id)`.
- Unique `uq_inbound_session_call (call_id)`.
- Index `ix_inbound_session_hmac_status (caller_number_hmac, status, created_at DESC)`.
- Index `ix_inbound_session_subject_time (subject_id, created_at DESC)`.
- FK `fk_inbound_session_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_inbound_session_confirmed_subject (confirmed_subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- No FK on `call_id`; it is the root IVR protocol identity.

Revision/immutability: `call_id`, `trusted_caller`, caller columns, `access_number_masked`, `node_id`, `protocol_version`, `prompt_version` and `created_at` are immutable; `ivr.guard_inbound_session` rejects their change. Only `status`, `confirmed_subject_id`, `guidance_status`, `confirmed_at` and `revision` change conditionally. A `CONFIRMED` session is terminal for confirmation-state purposes and is never re-rejected by replay. A fresh confirmation requires the original `(call_id, INBOUND_RESOLVE)` decision to be `GUIDE_ACTIVATION`; an already-`CONFIRMED` session with no `ACTIVATION_NEXT_ACTION` decision is a data-integrity failure. A fresh `SUSPENDED` or competing-call rejection may mark only a never-confirmed session `REJECTED`.

Sensitive representation: caller ciphertext/HMAC/masked and masked access number; no full caller or access number is a public key or ordinary log field.

### 4.25 ivr.call_record

Purpose: canonical call-scoped parent for task, session, legs, decisions, reservations and evidence. Owner: `ivr`. Wave V005; slice S2. Lifecycle: mutable head; `UNKNOWN` remains occupied until reconciliation; terminal states are immutable. Deletion: no hard delete.

Authorized errata (PA5 plan fix 1, 2026-10-28): by explicit user decision (option B), `created_at timestamptz NOT NULL DEFAULT now()` is added to `ivr.call_record` so the frozen `ix_call_record_subject_time (subject_id, ended_at, created_at)` can be created literally. `ivr.call_record` therefore has 23 columns and the six-table V005 total is 103 columns (23/17/23/11/14/15); the defaulted-column total is 17 (`ivr.call_record` contributes `status`, `created_at` and `revision`). No check, key, index, FK, guard, trigger, grant or V001-V004 contract changed; `created_at` stays outside `ivr_role`'s frozen `UPDATE` column allowlist, so runtime writes are INSERT-only and owner-level immutability is service-owned. This is an authorized schema errata, not a semantic-preserving transcription correction. Closure is recorded in `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan-fix-1.md`; no executable 103-column PostgreSQL evidence is claimed by this note.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_call_record` | Application-generated identity |
| call_id | text | NO | none | UNIQUE `uq_call_record_call`; CHECK `ck_call_record_call_id` | Canonical call protocol identity and FK parent |
| session_id | uuid | NO | none | UNIQUE `uq_call_record_session`; FK `fk_call_record_session` | One call record per inbound session |
| task_id | uuid | YES | none | UNIQUE `uq_call_record_task`; FK `fk_call_record_task` | NULL until a successful task-backed claim; then exactly one null-to-that-task assignment and immutable |
| subject_id | uuid | YES | none | FK `fk_call_record_subject` | Resolved subject when available |
| caller_ciphertext | text | NO | none | CHECK `ck_call_record_caller_ciphertext` | Encrypted caller |
| caller_number_hmac | text | NO | none | CHECK `ck_call_record_caller_hmac`; partial unique `uq_call_record_active_caller` | Canonical caller HMAC |
| caller_masked | text | NO | none | CHECK `ck_call_record_caller_masked` | Display-only masked caller |
| callee_ciphertext | text | YES | none | CHECK `ck_call_record_callee_ciphertext` | Encrypted callee when one was authorized |
| callee_masked | text | YES | none | CHECK `ck_call_record_callee_masked` | Display-only masked callee |
| access_number_masked | text | NO | none | CHECK `ck_call_record_access_masked` | Masked access number; display-only, non-unique and non-authoritative for replay |
| status | text | NO | `'ACTIVE'` | CHECK `ck_call_record_status`; `ck_call_record_terminal_shape` | `ACTIVE`, `ENDED`, `FAILED`, `UNKNOWN` or `REJECTED` |
| node_id | text | NO | none | CHECK `ck_call_record_node` | IVR node identity |
| protocol_version | text | NO | none | CHECK `ck_call_record_protocol` | IVR protocol version |
| started_at | timestamptz | YES | none | CHECK `ck_call_record_time_order` | Null only for a call rejected before start |
| bridged_at | timestamptz | YES | none | CHECK `ck_call_record_time_order` | A/B bridge time |
| recording_started_at | timestamptz | YES | none | CHECK `ck_call_record_time_order` | Recording start |
| recording_ended_at | timestamptz | YES | none | CHECK `ck_call_record_time_order` | Recording end |
| ended_at | timestamptz | YES | none | CHECK `ck_call_record_terminal_shape`; `ck_call_record_time_order` | Non-null exactly for terminal `ENDED`/`FAILED`/`REJECTED` |
| authoritative_seconds | integer | YES | none | CHECK `ck_call_record_authoritative_seconds` | Authoritative IVR recording seconds; never negative |
| end_reason | text | YES | none | CHECK `ck_call_record_end_reason` | Stable reason; `CALL_IN_PROGRESS` for a competing-call rejection |
| created_at | timestamptz | NO | `now()` | none | Row-creation timestamp; set on INSERT; insert-only for runtime (outside the frozen `UPDATE` column grant); owner-level immutability is service-owned |
| revision | integer | NO | `1` | CHECK `ck_call_record_revision` | Conditional-update revision |

Named checks:

- `ck_call_record_call_id`: `length(call_id) BETWEEN 1 AND 128 AND btrim(call_id) <> ''`.
- `ck_call_record_caller_ciphertext`: `caller_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_call_record_caller_hmac`: `caller_number_hmac ~ '^[0-9a-f]{64}$'`.
- `ck_call_record_caller_masked`: `length(caller_masked) BETWEEN 1 AND 64 AND btrim(caller_masked) <> ''`.
- `ck_call_record_callee_ciphertext`: `callee_ciphertext IS NULL OR callee_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_call_record_callee_masked`: `callee_masked IS NULL OR (length(callee_masked) BETWEEN 1 AND 64 AND btrim(callee_masked) <> '')`.
- `ck_call_record_access_masked`: `length(access_number_masked) BETWEEN 1 AND 64 AND btrim(access_number_masked) <> ''`.
- `ck_call_record_status`: `status IN ('ACTIVE','ENDED','FAILED','UNKNOWN','REJECTED')`.
- `ck_call_record_node`: `length(node_id) BETWEEN 1 AND 64 AND btrim(node_id) <> ''`.
- `ck_call_record_protocol`: `length(protocol_version) BETWEEN 1 AND 32 AND btrim(protocol_version) <> ''`.
- `ck_call_record_terminal_shape`: `(status IN ('ACTIVE','UNKNOWN')) = (ended_at IS NULL)`.
- `ck_call_record_time_order`: `(started_at IS NULL OR ended_at IS NULL OR ended_at >= started_at) AND (started_at IS NULL OR bridged_at IS NULL OR bridged_at >= started_at) AND (started_at IS NULL OR recording_started_at IS NULL OR recording_started_at >= started_at) AND (recording_started_at IS NULL OR recording_ended_at IS NULL OR recording_ended_at >= recording_started_at)`.
- `ck_call_record_authoritative_seconds`: `authoritative_seconds IS NULL OR (authoritative_seconds >= 0 AND status IN ('ENDED','FAILED'))`.
- `ck_call_record_end_reason`: `end_reason IS NULL OR (length(end_reason) BETWEEN 1 AND 64 AND btrim(end_reason) <> '')`.
- `ck_call_record_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_call_record (id)`.
- Unique `uq_call_record_call (call_id)`.
- Unique `uq_call_record_session (session_id)`.
- Unique `uq_call_record_task (task_id) WHERE task_id IS NOT NULL`.
- Partial unique `uq_call_record_active_caller (caller_number_hmac) WHERE status IN ('ACTIVE','UNKNOWN')` — one active or unknown call per canonical caller across accounts/workspaces; the current `callId` is excluded by the predicate in the transaction, not by this index.
- Index `ix_call_record_subject_time (subject_id, ended_at, created_at)`.
- Index `ix_call_record_task (task_id)`.
- FK `fk_call_record_session (session_id) REFERENCES ivr.inbound_session(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_call_record_task (task_id) REFERENCES ivr.outbound_call_task(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_call_record_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- No FK on `call_id`; it is the canonical call-scoped parent created in resolve or direct authorization.

Revision/immutability: `call_id`, `session_id`, `task_id`, party ciphertext/masked fields, `access_number_masked`, `node_id`, `protocol_version` and `started_at` are immutable; `ivr.guard_call_record_parties` rejects their change. `status`, bridge/recording/end times, `authoritative_seconds`, `end_reason` and `revision` use conditional updates. A live call is created or bound `ACTIVE` before a non-terminal decision/reservation. A competing call is created terminal `REJECTED` with `end_reason = 'CALL_IN_PROGRESS'` when absent or finalized `REJECTED` only when it is the current never-started/rejectable `ACTIVE` row; an unrelated competing call and a current `UNKNOWN` row are never mutated. `UNKNOWN` is never finalized `REJECTED` or released; it converges to `ENDED`/`FAILED` only through reconciliation. The partial unique index is a backstop; a conflict is mapped after a deterministic reread, never returned as a raw constraint error. `task_id` is NULL on the pre-savepoint call and may be assigned exactly once from NULL to the id of the task whose `claimed_call_id` equals this call, inside the successful tentative claim branch under `ivr.guard_call_record_parties`; any later change, clear, arbitrary relink, task switch or second call is rejected, and a category-2 rollback removes only that assignment while the call row survives with `task_id = NULL`.

Sensitive representation: caller/callee ciphertext and masked values; caller HMAC for lookup. No full party number is a public identifier or ordinary log field.

### 4.26 ivr.call_leg

Purpose: A-leg/B-leg fact for one call. Owner: `ivr`. Wave V005; slice S2. Lifecycle: mutable head with revision; terminal leg state retained. Deletion: no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_call_leg` | Application-generated identity |
| call_id | text | NO | none | FK `fk_call_leg_call`; part of `uq_call_leg_id` | Root call |
| leg_id | text | NO | none | CHECK `ck_call_leg_leg_id`; part of `uq_call_leg_id` | IVR leg identity unique within the call |
| leg_type | text | NO | none | CHECK `ck_call_leg_type` | `CALLER` or `CALLEE` |
| direction | text | NO | none | CHECK `ck_call_leg_direction` | `INBOUND` for the caller leg, `OUTBOUND` for the callee leg |
| state | text | NO | none | CHECK `ck_call_leg_state` | `RINGING`, `ANSWERED`, `ENDED` or `FAILED` |
| started_at | timestamptz | NO | none | none | Leg start time |
| answered_at | timestamptz | YES | none | CHECK `ck_call_leg_time_order` | Answer time when answered |
| ended_at | timestamptz | YES | none | CHECK `ck_call_leg_time_order` | Terminal leg time |
| end_reason | text | YES | none | CHECK `ck_call_leg_end_reason` | Stable leg end reason |
| revision | integer | NO | `1` | CHECK `ck_call_leg_revision` | Conditional-update revision |

Named checks:

- `ck_call_leg_leg_id`: `length(leg_id) BETWEEN 1 AND 64 AND btrim(leg_id) <> ''`.
- `ck_call_leg_type`: `leg_type IN ('CALLER','CALLEE')`.
- `ck_call_leg_direction`: `(leg_type = 'CALLER' AND direction = 'INBOUND') OR (leg_type = 'CALLEE' AND direction = 'OUTBOUND')`.
- `ck_call_leg_state`: `state IN ('RINGING','ANSWERED','ENDED','FAILED')`.
- `ck_call_leg_time_order`: `(answered_at IS NULL OR answered_at >= started_at) AND (ended_at IS NULL OR ended_at >= started_at) AND (answered_at IS NULL OR ended_at IS NULL OR ended_at >= answered_at)`.
- `ck_call_leg_end_reason`: `end_reason IS NULL OR (length(end_reason) BETWEEN 1 AND 64 AND btrim(end_reason) <> '')`.
- `ck_call_leg_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_call_leg (id)`.
- Unique `uq_call_leg_id (call_id, leg_id)`.
- Index `ix_call_leg_call_type (call_id, leg_type)`.
- FK `fk_call_leg_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `call_id`, `leg_id`, `leg_type`, `direction` and `started_at` are immutable; `ivr.guard_call_leg` rejects their change. State and timing use conditional revision. Caller and callee legs belong to one root call; recording and bridge facts remain on the call record. Leg state does not imply evidence solidification.

Sensitive representation: no party value on this table; it links to the call record.

### 4.27 ivr.call_event

Purpose: append-only IVR event fact within the IVR event domain. Owner: `ivr`. Wave V005; slice S2. Lifecycle: mutable head only for processing status/revision; raw payload and event identity are immutable. Deletion: no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_call_event` | Application-generated identity |
| call_id | text | NO | none | FK `fk_call_event_call` | Root call |
| event_domain | text | NO | `'IVR_EVENT'` | CHECK `ck_call_event_domain`; part of `uq_call_event_domain_id` | Constant IVR event domain |
| event_id | text | NO | none | CHECK `ck_call_event_event_id`; part of `uq_call_event_domain_id` | Unique within the IVR event domain |
| sequence_no | integer | YES | none | CHECK `ck_call_event_sequence`; partial unique `uq_call_event_sequence` | Optional monotonic sequence |
| event_type | text | NO | none | CHECK `ck_call_event_type` | Stable IVR event type |
| occurred_at | timestamptz | NO | none | none | IVR event time |
| received_at | timestamptz | NO | `now()` | none | Server receipt time |
| payload_ciphertext | text | NO | none | CHECK `ck_call_event_payload_ciphertext` | Encrypted raw payload |
| payload_redacted | jsonb | NO | none | CHECK `ck_call_event_payload_redacted` | Redacted view |
| payload_sha256 | text | NO | none | CHECK `ck_call_event_payload_sha` | SHA-256 of the canonical redacted source payload at `payload_redacted.source.payload` |
| processing_status | text | NO | `'RECEIVED'` | CHECK `ck_call_event_processing_status` | `RECEIVED`, `APPLIED`, `IGNORED` or `QUARANTINED` |
| reason_code | text | YES | none | CHECK `ck_call_event_reason` | Stable processing reason |
| revision | integer | NO | `1` | CHECK `ck_call_event_revision` | Conditional-update revision |

Named checks:

- `ck_call_event_domain`: `event_domain = 'IVR_EVENT'`.
- `ck_call_event_event_id`: `length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''`.
- `ck_call_event_sequence`: `sequence_no IS NULL OR sequence_no >= 1`.
- `ck_call_event_type`: `length(event_type) BETWEEN 1 AND 64 AND btrim(event_type) <> ''`.
- `ck_call_event_payload_ciphertext`: `payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_call_event_payload_redacted`: `jsonb_typeof(payload_redacted) = 'object' AND payload_redacted ? 'source' AND payload_redacted ? 'receipt' AND jsonb_typeof(payload_redacted->'source') = 'object' AND jsonb_typeof(payload_redacted->'receipt') = 'object' AND (payload_redacted - 'source' - 'receipt') = '{}'::jsonb AND (payload_redacted->'source' - 'eventType' - 'legId' - 'sequenceNo' - 'payload') = '{}'::jsonb AND (payload_redacted->'receipt' - 'payloadSha256' - 'eventFingerprint') = '{}'::jsonb AND (payload_redacted->'source') ? 'payload' AND (payload_redacted->'receipt') ? 'payloadSha256' AND (payload_redacted->'receipt') ? 'eventFingerprint' AND payload_redacted->'receipt'->>'payloadSha256' = payload_sha256`.
- `ck_call_event_payload_sha`: `payload_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_call_event_processing_status`: `processing_status IN ('RECEIVED','APPLIED','IGNORED','QUARANTINED')`.
- `ck_call_event_reason`: `reason_code IS NULL OR (length(reason_code) BETWEEN 1 AND 64 AND btrim(reason_code) <> '')`.
- `ck_call_event_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_call_event (id)`.
- Unique `uq_call_event_domain_id (event_domain, event_id)` — the same IVR event ID cannot repeat on another call.
- Partial unique `uq_call_event_sequence (call_id, sequence_no) WHERE sequence_no IS NOT NULL`.
- Index `ix_call_event_call_time (call_id, occurred_at)`.
- Index `ix_call_event_processing (processing_status, received_at)`.
- FK `fk_call_event_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `call_id`, `event_domain`, `event_id`, `sequence_no`, `event_type`, `occurred_at`, `received_at`, payload columns and `created` identity are immutable; only `processing_status`, `reason_code` and `revision` may change, and only once from `RECEIVED` to one terminal `APPLIED`/`QUARANTINED`/`IGNORED` outcome. The first stored payload and that first terminal outcome are absorbing: no later delivery changes them. The immutable `payload_redacted` has exactly two server-written top-level members: `source` = `{eventType, legId, sequenceNo, payload}`, the validated original redacted source event (`legId` and `sequenceNo` null-safe; `payload` is an object containing only the redacted non-protected event body), and `receipt` = `{payloadSha256, eventFingerprint}`, server-generated receipt metadata. `payload_sha256` is the lowercase SHA-256 of the section 2.5 canonical JSON of `payload_redacted.source.payload` only and is computed before any receipt metadata is added. The canonical event fingerprint is the §10.17 call-event fingerprint over `callId`, `eventId`, `source.eventType`, `source.legId`, that digest and `source.sequenceNo`; it is stored as `receipt.eventFingerprint`. `receipt` is excluded from the payload hash and is never accepted from a client: the server always recomputes both values, so a client cannot select receipt metadata or bypass source-hash checking. Reconstruction never depends on the indexed mutable `sequence_no` (which may be `NULL` for a sequence conflict) or on any mutable status. Only a duplicate event whose recomputed fingerprint equals the stored fingerprint replays the first processing outcome and mutates nothing. A changed fingerprint/type/sequence always returns a stable `EVENT_ID_CONFLICT` with no mutation, including when the first stored outcome was `SEQUENCE_CONFLICT`; it never overwrites the first payload/status or changes call/leg/settlement facts, and any optional conflict audit uses that same event identity and adds no schema object. For an unknown parent `call_id`, the service returns stable `CALL_UNKNOWN` before any insert: no `call_event`, call, leg or settlement write and no raw FK. A known call whose leg ID does not belong to it may persist one durable `QUARANTINED` `LEG_UNKNOWN` receipt. `RECEIVED`/`APPLIED` durable event receipt is distinct from accepted authoritative usage; only §9.24 may hand an authoritative terminal fact to settlement. Processing status/reason changes are conditional on revision and never rewrite the raw payload or event identity.

Sensitive representation: raw payload is encrypted; `payload_redacted` is redacted and hashed. No raw event payload is logged or returned.

### 4.28 ivr.ivr_decision

Purpose: frozen, purpose-discriminated IVR decision and typed response. Owner: `ivr`. Wave V005; slice S2. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_ivr_decision` | Application-generated decision identity |
| session_id | uuid | NO | none | FK `fk_ivr_decision_session` | Inbound session |
| call_id | text | NO | none | FK `fk_ivr_decision_call`; part of `uq_ivr_decision_call_purpose` | Canonical call |
| purpose | text | NO | none | CHECK `ck_ivr_decision_purpose`; part of `uq_ivr_decision_call_purpose` | `INBOUND_RESOLVE`, `ACTIVATION_NEXT_ACTION` or `DIRECT_AUTHORIZATION` |
| source_event_domain | text | YES | none | CHECK `ck_ivr_decision_source` | Null for `INBOUND_RESOLVE`; `IVR_CONFIRM` for `ACTIVATION_NEXT_ACTION`; `IVR_DIRECT_AUTH` for `DIRECT_AUTHORIZATION` |
| source_event_id | text | YES | none | CHECK `ck_ivr_decision_source`; part of `uq_ivr_decision_source` | Source event identity; nonblank 1-128 when present |
| request_fingerprint | text | NO | none | CHECK `ck_ivr_decision_fingerprint` | Canonical per-purpose semantic fingerprint |
| action | text | YES | none | CHECK `ck_ivr_decision_action`; `ck_ivr_decision_shape` | One of seven actions, or null only for successful `DIRECT_AUTHORIZATION` |
| reason_code | text | NO | none | CHECK `ck_ivr_decision_reason`; `ck_ivr_decision_direct_reason` | Stable reason; `DIRECT_AUTHORIZED` exactly when `action` is null |
| response_snapshot | jsonb | NO | none | CHECK `ck_ivr_decision_snapshot` | Typed redacted action snapshot |
| response_payload_ciphertext | text | NO | none | CHECK `ck_ivr_decision_payload` | Encrypted internal payload |
| response_sha256 | text | NO | none | CHECK `ck_ivr_decision_sha` | SHA-256 of the canonical redacted snapshot |
| key_version | integer | NO | none | CHECK `ck_ivr_decision_key_version` | Positive encryption key-generation reference |
| reservation_id | uuid | YES | none | FK `fk_ivr_decision_reservation` added by `ALTER` in V006 | Committed reservation; null when no reservation |
| decided_at | timestamptz | NO | `now()` | none | Decision commit time; never in a fingerprint |

Named checks:

- `ck_ivr_decision_purpose`: `purpose IN ('INBOUND_RESOLVE','ACTIVATION_NEXT_ACTION','DIRECT_AUTHORIZATION')`.
- `ck_ivr_decision_source`: `(purpose = 'INBOUND_RESOLVE' AND source_event_domain IS NULL AND source_event_id IS NULL) OR (purpose = 'ACTIVATION_NEXT_ACTION' AND source_event_domain = 'IVR_CONFIRM' AND length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> '') OR (purpose = 'DIRECT_AUTHORIZATION' AND source_event_domain = 'IVR_DIRECT_AUTH' AND length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> '')`.
- `ck_ivr_decision_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.
- `ck_ivr_decision_action`: `action IS NULL OR action IN ('GUIDE_ACTIVATION','DIAL_TASK_CALLEE','ENTER_CALLEE','GUIDE_PURCHASE','REJECT','REJECT_SUSPENDED','REJECT_CALL_IN_PROGRESS')`.
- `ck_ivr_decision_reason`: `reason_code IN ('ACTIVATION_REQUIRED','CONSENT_REQUIRED','CALLER_UNTRUSTED','SESSION_IDENTITY_MISMATCH','CALL_IN_PROGRESS','CONFIRMATION_NOT_ALLOWED','ACTIVATION_SUSPENDED','CALLER_BINDING_REVOKED','WORKSPACE_ACCESS_REVOKED','TASK_ACCESS_REVOKED','ORGANIZATION_INACTIVE','ACCOUNT_DISABLED','CALL_STATE_UNRESOLVED','TASK_CONFLICT','TASK_CLAIMED','NO_AVAILABLE_ENTITLEMENT','NO_VALID_TASK','TASK_EXPIRED','TASK_UNAVAILABLE','DIRECT_AUTHORIZED','DATA_INTEGRITY')`.
- `ck_ivr_decision_direct_reason`: `(action IS NULL) = (reason_code = 'DIRECT_AUTHORIZED')`.
- `ck_ivr_decision_shape`: `(purpose IN ('INBOUND_RESOLVE','ACTIVATION_NEXT_ACTION') AND action IS NOT NULL AND ((action = 'DIAL_TASK_CALLEE' AND reservation_id IS NOT NULL) OR (action <> 'DIAL_TASK_CALLEE' AND reservation_id IS NULL))) OR (purpose = 'DIRECT_AUTHORIZATION' AND ((action IS NULL AND reservation_id IS NOT NULL) OR (action IN ('GUIDE_PURCHASE','REJECT','REJECT_SUSPENDED','REJECT_CALL_IN_PROGRESS') AND reservation_id IS NULL)))`.
- `ck_ivr_decision_snapshot`: `jsonb_typeof(response_snapshot) = 'object'`.
- `ck_ivr_decision_payload`: `response_payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'`.
- `ck_ivr_decision_sha`: `response_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_ivr_decision_key_version`: `key_version >= 1`.

Keys and indexes:

- Primary key `pk_ivr_decision (id)`.
- Unique `uq_ivr_decision_call_purpose (call_id, purpose)` — the post-activation decision never overwrites the original resolve decision.
- Partial unique `uq_ivr_decision_source (call_id, source_event_domain, source_event_id) WHERE source_event_id IS NOT NULL`.
- Index `ix_ivr_decision_call_time (call_id, decided_at)`.
- FK `fk_ivr_decision_session (session_id) REFERENCES ivr.inbound_session(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_ivr_decision_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_ivr_decision_reservation (reservation_id) REFERENCES entitlement_metering.usage_reservation(id) ON UPDATE RESTRICT ON DELETE RESTRICT` — added by `ALTER TABLE` in V006; ordinary immediate, not deferrable.

Append-only control: `trg_ivr_decision_immutable` uses `ivr.reject_row_mutation()`; `ivr_role` has `SELECT` and `INSERT` only, and the only insert path is `ivr.select_call_action` (with `ivr.record_activation_next_action` as a thin wrapper). The same `(call_id, purpose)` with the same canonical fingerprint returns the first frozen decision; a changed fingerprint is a stable conflict before any side effect. The stored typed snapshot and encrypted payload are never recomputed or overwritten.

Sensitive representation: `response_snapshot` is the redacted typed snapshot; `response_payload_ciphertext` is the encrypted internal payload. The full task callee exists only under `task.calleeNumber` for `DIAL_TASK_CALLEE`, and the full confirmed callee only under `authorization.calleeNumber` for a successful `DIRECT_AUTHORIZATION` (`action` null). Both are prohibited from the redacted snapshot, logs, audit, Outbox and customer responses.

Navigation correction (F-15, normative detail outside the frozen catalog): the frozen §4.1 catalog row for this table preserves the historical phrase "stable registry in section 11" byte-for-byte because §4.1 is immutable. The normative reason-code registry is §2.9, and the exact allowed values are enforced by the named CHECK `ck_ivr_decision_reason` in this section. Section 11 is the linear Flyway migration order and does not contain a reason registry. An implementer must use §2.9 and `ck_ivr_decision_reason`; no schema, column, check or behavior change is implied by this navigation note.

### 4.29 entitlement_metering.trial_policy_version

Purpose: versioned first-trial policy with permanently fixed campaign identity. Owner: `entitlement-metering`. Wave V006; slice S2. Lifecycle: mutable head for `enabled`/revision; published content is immutable. Deletion: no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_trial_policy_version` | Application-generated identity |
| campaign_key | text | NO | none | CHECK `ck_trial_policy_campaign`; part of `uq_trial_policy_campaign_version` and `uq_trial_policy_enabled` | Always `FIRST-TRIAL` |
| version_no | integer | NO | none | CHECK `ck_trial_policy_version_no`; part of `uq_trial_policy_campaign_version` | Positive version |
| granted_seconds | integer | NO | none | CHECK `ck_trial_policy_granted_seconds` | Positive trial recording seconds |
| valid_days | integer | NO | none | CHECK `ck_trial_policy_valid_days` | Positive validity days |
| enabled | boolean | NO | `true` | part of `uq_trial_policy_enabled` | Only an enabled published version is current |
| snapshot | jsonb | NO | none | CHECK `ck_trial_policy_snapshot_type` | Canonical policy snapshot |
| snapshot_sha256 | text | NO | none | CHECK `ck_trial_policy_snapshot_sha` | SHA-256 of the canonical snapshot |
| published_by_employee_id | uuid | NO | none | FK `fk_trial_policy_employee` | Publishing employee |
| published_at | timestamptz | YES | none | none | Null for a draft; set once at publication and then immutable |
| revision | integer | NO | `1` | CHECK `ck_trial_policy_revision` | Conditional-update revision |

Named checks:

- `ck_trial_policy_campaign`: `campaign_key = 'FIRST-TRIAL'`.
- `ck_trial_policy_version_no`: `version_no >= 1`.
- `ck_trial_policy_granted_seconds`: `granted_seconds >= 1`.
- `ck_trial_policy_valid_days`: `valid_days >= 1`.
- `ck_trial_policy_snapshot_type`: `jsonb_typeof(snapshot) = 'object'`.
- `ck_trial_policy_snapshot_sha`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_trial_policy_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_trial_policy_version (id)`.
- Unique `uq_trial_policy_campaign_version (campaign_key, version_no)`.
- Partial unique `uq_trial_policy_enabled (campaign_key) WHERE enabled AND published_at IS NOT NULL` — at most one enabled published first-trial policy.
- Index `ix_trial_policy_enabled (enabled, published_at)`.
- FK `fk_trial_policy_employee (published_by_employee_id) REFERENCES operations_iam.employee_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `campaign_key` is permanently `FIRST-TRIAL`; `version_no`, `granted_seconds`, `valid_days`, `snapshot`, `snapshot_sha256`, `published_by_employee_id` and `published_at` are immutable once published. `entitlement_metering.guard_trial_policy_version` rejects a change after `published_at` is set. Publication is one owner `UPDATE` that, while `OLD.published_at IS NULL` and after the previous enabled published version has been disabled, sets `published_by_employee_id` to the actual authenticated publishing employee, `published_at`, `enabled = true` and `revision + 1` together; the guard must allow that one statement and reject any later change to publisher identity or published content. It is a publisher fact, not a draft-owner label, and no same-employee-only rule is invented (OQ-5). Publication/disable is the §9.26 serialized owner transaction under `entitlement_metering.trial_policy:FIRST-TRIAL`: a new version first disables the previous enabled published version (`enabled = false`, revision +1) and then performs that single candidate publication `UPDATE`, so the immediate partial unique `uq_trial_policy_enabled` sees at most one qualifying row at every statement; disable sets `enabled = false`. A stale expected-current-version loser returns a stable `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW` with no change, never a raw unique error and never two enabled published versions. The sole authoritative policy-selection linearization point is the enabled/published/expected-version read performed by the V006 `issue_first_trial` function itself (§10.9); the function retains that immutable version and content in local values through the grant/Outbox. The application wrapper lookup is preliminary only and has no selection authority. A missing, disabled or stale policy fails closed at that single read; a disable committed before that read is observed, and a disable committed after that read does not revoke the already-selected grant. Replay and already-granted return the immutable prior trial result before any new policy selection, and there is no second enabled recheck. The publication advisory key is never combined with the IVR call-path order, and no stale caller-supplied policy is accepted (OQ-6). A new rule requires a new version; an existing grant keeps its frozen snapshot and never changes with a later policy. Trial `valid_days` uses the explicit §10.19 calculation (`granted_at + valid_days * interval '1 day'`, UTC) with no guessed calendar rule. The runtime configuration key for the 30-second task TTL is not stored in this table (Q18).

Sensitive representation: no protected value; the snapshot contains policy facts only.

### 4.30 entitlement_metering.entitlement_grant

Purpose: immutable entitlement grant for a trial or a paid order line/installment. Owner: `entitlement-metering`. Wave V006; slice S2. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_entitlement_grant` | Application-generated grant identity |
| subject_id | uuid | NO | none | FK `fk_entitlement_grant_subject` | Stable subject |
| source_type | text | NO | none | CHECK `ck_entitlement_grant_source`; `ck_entitlement_grant_source_shape` | `TRIAL` or `PURCHASE` |
| source_ref_id | uuid | NO | none | none | Internal source fact: the trial-policy version for `TRIAL`, the order for `PURCHASE`; polymorphic and intentionally not an FK |
| entitlement_type | text | NO | none | CHECK `ck_entitlement_grant_type`; part of `uq_entitlement_grant_paid` | Baseline registry in §2.9 |
| granted_units | bigint | NO | none | CHECK `ck_entitlement_grant_units` | Positive units; recording grants use seconds |
| valid_from | timestamptz | NO | none | none | Start of validity |
| expires_at | timestamptz | NO | none | CHECK `ck_entitlement_grant_expiry` | End of validity |
| order_id | uuid | YES | none | FK `fk_entitlement_grant_order`; part of `uq_entitlement_grant_paid` | Paid order; null for `TRIAL` |
| installment_no | integer | YES | none | CHECK `ck_entitlement_grant_installment`; part of `uq_entitlement_grant_paid` | Paid installment; null for `TRIAL` |
| snapshot | jsonb | NO | none | CHECK `ck_entitlement_grant_snapshot_type` | Frozen grant snapshot (order/version/policy facts) |
| snapshot_sha256 | text | NO | none | CHECK `ck_entitlement_grant_snapshot_sha` | SHA-256 of the canonical snapshot |
| granted_at | timestamptz | NO | `now()` | none | Grant time |

Named checks:

- `ck_entitlement_grant_source`: `source_type IN ('TRIAL','PURCHASE')`.
- `ck_entitlement_grant_source_shape`: `(source_type = 'TRIAL' AND order_id IS NULL AND installment_no IS NULL) OR (source_type = 'PURCHASE' AND order_id IS NOT NULL AND installment_no IS NOT NULL AND installment_no >= 1 AND source_ref_id = order_id)`.
- `ck_entitlement_grant_type`: `entitlement_type IN ('RECORDING_SECONDS','AI_SECONDS','EVIDENCE_COUNT','STORAGE_BYTES')`.
- `ck_entitlement_grant_units`: `granted_units >= 1`.
- `ck_entitlement_grant_expiry`: `expires_at > valid_from`.
- `ck_entitlement_grant_installment`: `installment_no IS NULL OR installment_no >= 1`.
- `ck_entitlement_grant_snapshot_type`: `jsonb_typeof(snapshot) = 'object'`.
- `ck_entitlement_grant_snapshot_sha`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.

Keys and indexes:

- Primary key `pk_entitlement_grant (id)`.
- Partial unique `uq_entitlement_grant_paid (order_id, installment_no, entitlement_type) WHERE source_type = 'PURCHASE'` — one paid grant per order line, installment and type.
- Index `ix_entitlement_grant_subject_type (subject_id, entitlement_type, expires_at)`.
- Index `ix_entitlement_grant_order (order_id, installment_no)`.
- FK `fk_entitlement_grant_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_entitlement_grant_order (order_id) REFERENCES catalog_order.customer_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_entitlement_grant_immutable` uses `entitlement_metering.reject_row_mutation()`; `entitlement_metering_role` has `SELECT` and `INSERT` only. Trial source uniqueness is enforced by `trial_grant.subject_id`; the paid partial unique enforces the order/installment/type key. A grant is never updated after insertion.

Sensitive representation: the snapshot contains frozen commercial/grant facts only; no protected value. `source_ref_id` and `order_id` are internal.

### 4.31 entitlement_metering.trial_grant

Purpose: permanent first-trial fact with persisted granting action identity and fingerprint. Owner: `entitlement-metering`. Wave V006; slice S2. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_trial_grant` | Trial fact identity |
| subject_id | uuid | NO | none | UNIQUE `uq_trial_grant_subject`; FK `fk_trial_grant_subject` | One permanent first trial per subject |
| campaign_key | text | NO | none | CHECK `ck_trial_grant_campaign` | Always `FIRST-TRIAL` |
| trial_policy_version_id | uuid | NO | none | FK `fk_trial_grant_policy` | Frozen policy version |
| entitlement_grant_id | uuid | NO | none | UNIQUE `uq_trial_grant_grant`; FK `fk_trial_grant_grant` | The trial's immutable grant |
| source_action_domain | text | NO | none | CHECK `ck_trial_grant_action_domain`; part of `uq_trial_grant_source` | `IVR_CONFIRM` or `ACTIVATION` |
| source_action_event_id | text | NO | none | CHECK `ck_trial_grant_event_id`; part of `uq_trial_grant_source` | Granting action event identity |
| request_fingerprint | text | NO | none | CHECK `ck_trial_grant_fingerprint` | Canonical granting-action fingerprint |
| granted_at | timestamptz | NO | `now()` | none | First grant time |
| expires_at | timestamptz | NO | none | CHECK `ck_trial_grant_expiry` | Grant expiry |

Named checks:

- `ck_trial_grant_campaign`: `campaign_key = 'FIRST-TRIAL'`.
- `ck_trial_grant_action_domain`: `source_action_domain IN ('IVR_CONFIRM','ACTIVATION')`.
- `ck_trial_grant_event_id`: `length(source_action_event_id) BETWEEN 1 AND 128 AND btrim(source_action_event_id) <> ''`.
- `ck_trial_grant_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.
- `ck_trial_grant_expiry`: `expires_at > granted_at`.

Keys and indexes:

- Primary key `pk_trial_grant (id)`.
- Unique `uq_trial_grant_subject (subject_id)` — permanent; a new campaign key, policy version, channel, claim or import cannot create a second first trial.
- Unique `uq_trial_grant_grant (entitlement_grant_id)`.
- Unique `uq_trial_grant_source (source_action_domain, source_action_event_id)`.
- Index `ix_trial_grant_campaign_time (campaign_key, granted_at)`.
- FK `fk_trial_grant_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_trial_grant_policy (trial_policy_version_id) REFERENCES entitlement_metering.trial_policy_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_trial_grant_grant (entitlement_grant_id) REFERENCES entitlement_metering.entitlement_grant(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_trial_grant_immutable` uses `entitlement_metering.reject_row_mutation()`; only `issue_first_trial` inserts this row and its balance in the same activation transaction. The same `(source_action_domain, source_action_event_id)` with the same `request_fingerprint` replays the original ids; the same action with a changed fingerprint raises `TRIAL_ACTION_CONFLICT`; a different action for an already-granted subject returns `ALREADY_GRANTED` with the original ids. No second trial, grant or balance is ever created.

Sensitive representation: no protected value; the source action identity and fingerprint are internal replay facts.

### 4.32 entitlement_metering.entitlement_balance

Purpose: current reserved/consumed balance for one grant. Owner: `entitlement-metering`. Wave V006; slice S2. Lifecycle: mutable head updated only by reservation/settlement; the grant relation and granted units are immutable. Deletion: no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_entitlement_balance` | Balance identity |
| grant_id | uuid | NO | none | UNIQUE `uq_entitlement_balance_grant`; FK `fk_entitlement_balance_grant` | One balance per grant |
| granted_units | bigint | NO | none | CHECK `ck_entitlement_balance_granted` | Copied from the immutable grant; positive |
| reserved_units | bigint | NO | `0` | CHECK `ck_entitlement_balance_units` | Units held by non-terminal reservations |
| consumed_units | bigint | NO | `0` | CHECK `ck_entitlement_balance_units` | Settled units |
| status | text | NO | `'AVAILABLE'` | CHECK `ck_entitlement_balance_status`; `ck_entitlement_balance_status_shape` | `AVAILABLE`, `EXHAUSTED` or `EXPIRED` |
| revision | integer | NO | `1` | CHECK `ck_entitlement_balance_revision` | Conditional-update revision |
| updated_at | timestamptz | NO | `now()` | none | Last reservation/settlement update |

Named checks:

- `ck_entitlement_balance_granted`: `granted_units >= 1`.
- `ck_entitlement_balance_units`: `reserved_units >= 0 AND consumed_units >= 0 AND reserved_units + consumed_units <= granted_units`.
- `ck_entitlement_balance_status`: `status IN ('AVAILABLE','EXHAUSTED','EXPIRED')`.
- `ck_entitlement_balance_status_shape`: `(status = 'EXHAUSTED') = (consumed_units = granted_units)`.
- `ck_entitlement_balance_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_entitlement_balance (id)`.
- Unique `uq_entitlement_balance_grant (grant_id)`.
- Index `ix_entitlement_balance_status (status)`.
- FK `fk_entitlement_balance_grant (grant_id) REFERENCES entitlement_metering.entitlement_grant(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `grant_id` and `granted_units` are immutable; `entitlement_metering.guard_entitlement_balance` rejects their change. `reserved_units`, `consumed_units`, `status`, `revision` and `updated_at` change only in the same transaction as the reservation/settlement/issuance operation, under the balance row lock. A reservation increments `reserved_units`; settlement atomically decrements `reserved_units`, increments `consumed_units` and sets the derived `status` in the same conditional `UPDATE`, so when the settled units reach the granted units the row becomes `EXHAUSTED` immediately and `ck_entitlement_balance_status_shape` holds at every statement (OQ-8). A reservation that merely fills units leaves `status = 'AVAILABLE'` because `EXHAUSTED` requires `consumed_units = granted_units`; release decrements `reserved_units` without setting `EXHAUSTED`. The CHECK allows no overcommit. `EXHAUSTED` is a derived status, not a second balance row.

Sensitive representation: no protected value.

### 4.33 entitlement_metering.entitlement_issuance

Purpose: durable per-order/installment/entitlement-type issuance state. Owner: `entitlement-metering`. Wave V006; slice S2. Lifecycle: mutable head with revision and attempt count. Deletion: no hard delete; terminal `ISSUED` retained.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_entitlement_issuance` | Issuance identity |
| order_id | uuid | NO | none | FK `fk_issuance_order`; part of `uq_issuance_line` | Paid order |
| installment_no | integer | NO | none | CHECK `ck_issuance_installment`; part of `uq_issuance_line` | Positive installment |
| entitlement_type | text | NO | none | CHECK `ck_issuance_type`; part of `uq_issuance_line` | Baseline registry in §2.9 |
| status | text | NO | `'PENDING'` | CHECK `ck_issuance_status`; `ck_issuance_state_shape` | `PENDING`, `ISSUED` or `FAILED` |
| attempt_count | integer | NO | `0` | CHECK `ck_issuance_attempt_count` | Non-negative retry count |
| revision | integer | NO | `1` | CHECK `ck_issuance_revision` | Conditional-update revision |
| last_error_code | text | YES | none | CHECK `ck_issuance_last_error` | Stable error; required for `FAILED` |
| grant_id | uuid | YES | none | UNIQUE `uq_issuance_grant`; FK `fk_issuance_grant` | Grant written on success |
| source_event_domain | text | NO | none | CHECK `ck_issuance_event_domain` | Payment event domain; baseline `PAYMENT` |
| source_event_id | text | NO | none | CHECK `ck_issuance_event_id` | Verified payment event identity |
| first_attempt_at | timestamptz | YES | none | none | First attempt time |
| issued_at | timestamptz | YES | none | CHECK `ck_issuance_state_shape` | Non-null exactly when `ISSUED` |
| updated_at | timestamptz | NO | `now()` | none | Last attempt/state update |

Named checks:

- `ck_issuance_installment`: `installment_no >= 1`.
- `ck_issuance_type`: `entitlement_type IN ('RECORDING_SECONDS','AI_SECONDS','EVIDENCE_COUNT','STORAGE_BYTES')`.
- `ck_issuance_status`: `status IN ('PENDING','ISSUED','FAILED')`.
- `ck_issuance_state_shape`: `(status = 'ISSUED') = (grant_id IS NOT NULL AND issued_at IS NOT NULL) AND (status = 'FAILED') = (last_error_code IS NOT NULL)`.
- `ck_issuance_attempt_count`: `attempt_count >= 0`.
- `ck_issuance_revision`: `revision >= 1`.
- `ck_issuance_last_error`: `last_error_code IS NULL OR (length(last_error_code) BETWEEN 1 AND 64 AND btrim(last_error_code) <> '')`.
- `ck_issuance_event_domain`: `source_event_domain = 'PAYMENT'`.
- `ck_issuance_event_id`: `length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> ''`.
- `ck_issuance_time_order`: `issued_at IS NULL OR first_attempt_at IS NULL OR issued_at >= first_attempt_at`.

Keys and indexes:

- Primary key `pk_entitlement_issuance (id)`.
- Unique `uq_issuance_line (order_id, installment_no, entitlement_type)` — exactly one durable issuance row per line, installment and type.
- Partial unique `uq_issuance_grant (grant_id) WHERE grant_id IS NOT NULL`.
- Index `ix_issuance_order_status (order_id, status)`.
- Index `ix_issuance_event (source_event_domain, source_event_id)`.
- FK `fk_issuance_order (order_id) REFERENCES catalog_order.customer_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_issuance_grant (grant_id) REFERENCES entitlement_metering.entitlement_grant(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `order_id`, `installment_no`, `entitlement_type`, `source_event_domain` and `source_event_id` are immutable; `entitlement_metering.guard_entitlement_issuance` rejects their change. `status`, `attempt_count`, `last_error_code`, `grant_id`, `first_attempt_at`, `issued_at`, `updated_at` and `revision` use conditional updates. `PENDING → ISSUED`, `PENDING → FAILED` and `FAILED → PENDING` retry are allowed; `ISSUED` is terminal. A retry never duplicates the grant because the grant's paid partial unique and `grant_id` uniqueness enforce once-only issuance. The frozen order snapshot — not current catalog state — determines the line.

Sensitive representation: no protected value; the source event and grant IDs are internal.

### 4.34 entitlement_metering.usage_reservation

Purpose: one reservation per call with frozen authorized seconds and explicit settlement/reconciliation. Owner: `entitlement-metering`. Wave V006; slice S2. Lifecycle: mutable head; one terminal transition. Deletion: no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_usage_reservation` | Reservation identity |
| call_id | text | NO | none | UNIQUE `uq_usage_reservation_call`; FK `fk_usage_reservation_call` | One reservation per call |
| balance_id | uuid | NO | none | FK `fk_usage_reservation_balance` | Balance debited |
| subject_id | uuid | NO | none | FK `fk_usage_reservation_subject` | Stable subject |
| authorized_units | bigint | NO | none | CHECK `ck_usage_reservation_authorized` | Frozen recording seconds in this baseline |
| status | text | NO | `'HELD'` | CHECK `ck_usage_reservation_status`; `ck_usage_reservation_state_shape` | `HELD`, `RECONCILING`, `SETTLED` or `RELEASED` |
| authorization_expires_at | timestamptz | NO | none | none | Last instant to start the B-leg; never renewed by replay |
| settled_units | bigint | NO | `0` | CHECK `ck_usage_reservation_settled`; `ck_usage_reservation_state_shape` | Settled units; zero until settlement |
| reconciliation_due_at | timestamptz | YES | none | CHECK `ck_usage_reservation_state_shape` | Required in `RECONCILING` |
| reconciliation_attempt_count | integer | NO | `0` | CHECK `ck_usage_reservation_reconciliation_attempts` | Non-negative |
| last_reconciled_at | timestamptz | YES | none | none | Last authoritative reconciliation time |
| authority_source | text | YES | none | CHECK `ck_usage_reservation_authority`; `ck_usage_reservation_state_shape` | `IVR` or `OPERATIONS`; required for a terminal state |
| last_error_code | text | YES | none | CHECK `ck_usage_reservation_last_error` | Stable failure/reconciliation code |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| terminal_at | timestamptz | YES | none | CHECK `ck_usage_reservation_state_shape` | Non-null exactly for `SETTLED`/`RELEASED` |
| revision | integer | NO | `1` | CHECK `ck_usage_reservation_revision` | Conditional-update revision |

Named checks:

- `ck_usage_reservation_authorized`: `authorized_units >= 1`.
- `ck_usage_reservation_status`: `status IN ('HELD','RECONCILING','SETTLED','RELEASED')`.
- `ck_usage_reservation_settled`: `settled_units >= 0 AND settled_units <= authorized_units`.
- `ck_usage_reservation_state_shape`: `(status = 'HELD' AND terminal_at IS NULL AND settled_units = 0 AND reconciliation_due_at IS NULL) OR (status = 'RECONCILING' AND terminal_at IS NULL AND settled_units = 0 AND reconciliation_due_at IS NOT NULL) OR (status = 'SETTLED' AND terminal_at IS NOT NULL AND settled_units > 0 AND authority_source IS NOT NULL) OR (status = 'RELEASED' AND terminal_at IS NOT NULL AND settled_units = 0 AND authority_source IS NOT NULL)`.
- `ck_usage_reservation_reconciliation_attempts`: `reconciliation_attempt_count >= 0`.
- `ck_usage_reservation_authority`: `authority_source IS NULL OR authority_source IN ('IVR','OPERATIONS')`.
- `ck_usage_reservation_last_error`: `last_error_code IS NULL OR (length(last_error_code) BETWEEN 1 AND 64 AND btrim(last_error_code) <> '')`.
- `ck_usage_reservation_time_order`: `terminal_at IS NULL OR terminal_at >= created_at`.
- `ck_usage_reservation_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_usage_reservation (id)`.
- Unique `uq_usage_reservation_call (call_id)` — one reservation per call.
- Index `ix_usage_reservation_balance_status (balance_id, status)`.
- Index `ix_usage_reservation_reconcile (status, reconciliation_due_at)`.
- FK `fk_usage_reservation_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_usage_reservation_balance (balance_id) REFERENCES entitlement_metering.entitlement_balance(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_usage_reservation_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `call_id`, `balance_id`, `subject_id`, `authorized_units`, `authorization_expires_at` and `created_at` are immutable; `entitlement_metering.guard_usage_reservation` rejects their change. Status/timing/error fields use conditional revision. The reservation is created in the same transaction as the task claim and frozen decision, so no `CLAIMED` task commits without a reservation. Settlement is at most once; a late or duplicate event returns the first settlement and never recharges. Unused units are released once. `UNKNOWN` use keeps the reservation in `RECONCILING` and blocks a new reservation; it is never released. Expiry of `authorization_expires_at` does not release a started call and never renews the authorization.

Sensitive representation: no party or media value; the call link is internal.

Settlement boundary (F-10, normative detail outside the frozen catalog): the frozen §4.1 catalog row for this table contains the historical phrase "actual use cannot exceed authorized units plus approved tolerance". No tolerance value is approved (Q8 remains open), so the normative baseline is exactly the named CHECK `ck_usage_reservation_settled` (`settled_units <= authorized_units`) with no tolerance parameter, field or clamp. A reported authoritative duration within `1..authorized_units` may settle normally. A reported duration greater than `authorized_units` is not silently clamped to the authorized value, not settled, not released and not accepted as evidence; the reservation moves or stays in `RECONCILING` with a stable quarantine code and is reconciled by the approved later process (§9.15, Q8/Q21). Known zero use is distinct from unknown use: known zero use releases the held units once with `settled_units = 0` and creates no positive `usage_fact`; unknown use keeps `RECONCILING` occupied and is never released. The phrase "plus approved tolerance" grants no current tolerance and cannot be implemented as an implicit overrun allowance.

### 4.35 entitlement_metering.usage_fact

Purpose: append-only authoritative usage fact per reservation/type and source event. Owner: `entitlement-metering`. Wave V006; slice S2. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_usage_fact` | Usage fact identity |
| reservation_id | uuid | NO | none | FK `fk_usage_fact_reservation`; part of `uq_usage_fact_reservation_type` | Reservation |
| balance_id | uuid | NO | none | FK `fk_usage_fact_balance` | Balance |
| call_id | text | NO | none | FK `fk_usage_fact_call` | Call |
| usage_type | text | NO | none | CHECK `ck_usage_fact_type`; part of `uq_usage_fact_reservation_type` | Baseline `RECORDING_SECONDS` |
| units | bigint | NO | none | CHECK `ck_usage_fact_units` | Positive settled units |
| source_event_domain | text | NO | none | CHECK `ck_usage_fact_event_domain`; part of `uq_usage_fact_source` | Baseline `IVR_EVENT` |
| source_event_id | text | NO | none | CHECK `ck_usage_fact_event_id`; part of `uq_usage_fact_source` | Authoritative IVR terminal/usage event |
| occurred_at | timestamptz | NO | none | none | Authoritative event time |
| created_at | timestamptz | NO | `now()` | none | Insertion time |
| snapshot | jsonb | NO | none | CHECK `ck_usage_fact_snapshot_type` | Canonical usage snapshot |
| snapshot_sha256 | text | NO | none | CHECK `ck_usage_fact_snapshot_sha` | SHA-256 of the canonical snapshot |

Named checks:

- `ck_usage_fact_type`: `usage_type IN ('RECORDING_SECONDS')`.
- `ck_usage_fact_units`: `units >= 1`.
- `ck_usage_fact_event_domain`: `source_event_domain = 'IVR_EVENT'`.
- `ck_usage_fact_event_id`: `length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> ''`.
- `ck_usage_fact_snapshot_type`: `jsonb_typeof(snapshot) = 'object'`.
- `ck_usage_fact_snapshot_sha`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.

Keys and indexes:

- Primary key `pk_usage_fact (id)`.
- Unique `uq_usage_fact_reservation_type (reservation_id, usage_type)` — one settlement fact per reservation and type.
- Unique `uq_usage_fact_source (source_event_domain, source_event_id)` — a terminal IVR event settles at most one usage fact.
- Index `ix_usage_fact_call (call_id)`.
- FK `fk_usage_fact_reservation (reservation_id) REFERENCES entitlement_metering.usage_reservation(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_usage_fact_balance (balance_id) REFERENCES entitlement_metering.entitlement_balance(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_usage_fact_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_usage_fact_immutable` uses `entitlement_metering.reject_row_mutation()`; `entitlement_metering_role` has `SELECT` and `INSERT` only. The unique reservation/type and source/event keys make a duplicate terminal event converge to the first settlement without a second fact. Negative, reversed, missing or conflicting terminal facts are quarantined and never produce a valid usage fact.

Sensitive representation: usage metadata and a redacted snapshot only; no raw media or protected value.

### 4.36 activation_consent.consent_record

Purpose: append-only explicit consent snapshot for activation, recording or purchase. Owner: `activation-consent`. Wave V007; slice S2. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_consent_record` | Consent identity |
| subject_id | uuid | NO | none | FK `fk_consent_subject` | Consenting subject |
| purpose | text | NO | none | CHECK `ck_consent_purpose` | `SERVICE_ACTIVATION`, `EVIDENCE_RECORDING` or `PURCHASE` |
| channel | text | NO | none | CHECK `ck_consent_channel` | `IVR`, `MINIAPP`, `WEB` or `OPERATIONS` |
| event_domain | text | NO | none | CHECK `ck_consent_event_domain`; part of `uq_consent_event` | `IVR_CONFIRM`, `ACTIVATION`, `PURCHASE` or `OPERATIONS` |
| policy_code | text | NO | none | CHECK `ck_consent_policy_code` | Stable policy code |
| policy_version | text | NO | none | CHECK `ck_consent_policy_version` | Current policy version validated before append |
| prompt_version | text | NO | none | CHECK `ck_consent_prompt_version` | Prompt version presented to the subject |
| action | text | NO | none | CHECK `ck_consent_action` | Explicit `ACCEPT` or `DECLINE` only |
| source_type | text | NO | none | CHECK `ck_consent_source_type` | `IVR_SESSION`, `ACCOUNT` or `EMPLOYEE` |
| source_ref | text | NO | none | CHECK `ck_consent_source_ref` | Opaque stable source reference; not a public key |
| ivr_session_id | uuid | YES | none | FK `fk_consent_session`; part of `ck_consent_source_shape` | IVR session source |
| account_id | uuid | YES | none | FK `fk_consent_account`; part of `ck_consent_source_shape` | Account source |
| employee_id | uuid | YES | none | FK `fk_consent_employee`; part of `ck_consent_source_shape` | Operations employee source |
| snapshot | jsonb | NO | none | CHECK `ck_consent_snapshot_type` | Canonical redacted consent snapshot |
| snapshot_sha256 | text | NO | none | CHECK `ck_consent_snapshot_sha` | SHA-256 of the canonical snapshot |
| event_id | text | NO | none | CHECK `ck_consent_event_id`; part of `uq_consent_event` | Unique within `event_domain` |
| request_fingerprint | text | NO | none | CHECK `ck_consent_fingerprint` | Canonical consent fingerprint for replay comparison |
| accepted_at | timestamptz | NO | `now()` | none | Consent decision time |

Named checks:

- `ck_consent_purpose`: `purpose IN ('SERVICE_ACTIVATION','EVIDENCE_RECORDING','PURCHASE')`.
- `ck_consent_channel`: `channel IN ('IVR','MINIAPP','WEB','OPERATIONS')`.
- `ck_consent_event_domain`: `event_domain IN ('IVR_CONFIRM','ACTIVATION','PURCHASE','OPERATIONS')`.
- `ck_consent_policy_code`: `length(policy_code) BETWEEN 1 AND 64 AND btrim(policy_code) <> ''`.
- `ck_consent_policy_version`: `length(policy_version) BETWEEN 1 AND 64 AND btrim(policy_version) <> ''`.
- `ck_consent_prompt_version`: `length(prompt_version) BETWEEN 1 AND 64 AND btrim(prompt_version) <> ''`.
- `ck_consent_action`: `action IN ('ACCEPT','DECLINE')`.
- `ck_consent_source_type`: `source_type IN ('IVR_SESSION','ACCOUNT','EMPLOYEE')`.
- `ck_consent_source_ref`: `length(source_ref) BETWEEN 1 AND 128 AND btrim(source_ref) <> ''`.
- `ck_consent_source_shape`: `(source_type = 'IVR_SESSION' AND ivr_session_id IS NOT NULL AND account_id IS NULL AND employee_id IS NULL) OR (source_type = 'ACCOUNT' AND account_id IS NOT NULL AND ivr_session_id IS NULL AND employee_id IS NULL) OR (source_type = 'EMPLOYEE' AND employee_id IS NOT NULL AND ivr_session_id IS NULL AND account_id IS NULL)`.
- `ck_consent_channel_shape`: `(channel = 'IVR' AND source_type = 'IVR_SESSION') OR (channel IN ('MINIAPP','WEB') AND source_type = 'ACCOUNT') OR (channel = 'OPERATIONS' AND source_type = 'EMPLOYEE')`.
- `ck_consent_snapshot_type`: `jsonb_typeof(snapshot) = 'object'`.
- `ck_consent_snapshot_sha`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_consent_event_id`: `length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''`.
- `ck_consent_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.

Keys and indexes:

- Primary key `pk_consent_record (id)`.
- Unique `uq_consent_event (event_domain, event_id)`.
- Index `ix_consent_subject_purpose (subject_id, purpose, accepted_at DESC)`.
- Index `ix_consent_session (ivr_session_id)`.
- FK `fk_consent_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_consent_session (ivr_session_id) REFERENCES ivr.inbound_session(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_consent_account (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_consent_employee (employee_id) REFERENCES operations_iam.employee_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_consent_record_immutable` uses `activation_consent.reject_row_mutation()`; `activation_consent_role` has `SELECT` and `INSERT` only. The accepted branch of `activation-consent.record_activation_confirmation` (IVR), `activation-consent.record_client_activation` (`MINIAPP`/`WEB` `SERVICE_ACTIVATION`) or `activation-consent.append_purchase_consent` (`PURCHASE`) inserts the row on the same connection/transaction; the order service never inserts it directly. A missing key `1`, an absent explicit accept, a stale prompt/policy version/key/content, a source/account/subject/channel/purpose mismatch or an invalid source produces no accepted row. The snapshot and fingerprint are immutable; a later consent version does not rewrite an earlier record. For `PURCHASE`, the service appends one explicit `ACCEPT` with the §10.17 consent fingerprint and the deterministic 81-character `purchaseConsentEventId` as both `event_id` and `source_ref`, and the order service validates purpose/subject/channel/source/account/current policy and action in the same transaction; an explicit decline is a typed refusal with no accepted row and no order. For `SERVICE_ACTIVATION` (`IVR`, `MINIAPP` or `WEB`), `request_fingerprint` is exactly the §10.17 `SERVICE_ACTIVATION` consent scope: it binds subject, null-safe account, channel, source type, stable source reference, purpose, policy code/version, prompt version and action `ACCEPT`, and excludes the transport `eventId`, timestamps, generated consent ID, snapshot hash and current revision/status. The stable source reference is exact: `ivr-session:` + the canonical lowercase session UUID (48 characters) for IVR, independent of the transport event ID; `activation:` + the lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, activationEventId}` (75 characters) for `MINIAPP`/`WEB`, from the authenticated canonical account and the stable activation action ID; and the deterministic 81-character `purchaseConsentEventId` for `PURCHASE`. For IVR, the first accepted `SERVICE_ACTIVATION` append stores `event_domain = 'IVR_CONFIRM'` and `event_id` equal to the already validated nonblank 1-128 confirmation `eventId`; `source_ref` remains the 48-character `ivr-session:` reference, independent of that transport event ID. A same-semantic retry with a new transport event ID returns the first result before any append and never rewrites the first consent `event_id` or adds a consent. The activation transition fingerprint binds this immutable consent fingerprint (never the generated consent ID or the current transition revision), so a changed policy/prompt/source semantic is a stable conflict rather than a replay; a regenerated IVR confirmation event ID with identical semantics replays the first IVR decision and consent before any second append.

Sensitive representation: `snapshot` is redacted and hashed; it contains policy/prompt references and no raw protected value. `source_ref` is an opaque internal reference, never a share code or capability token.

### 4.37 activation_consent.service_activation

Purpose: current internal service activation per subject. Owner: `activation-consent`. Wave V007; slice S2. Lifecycle: mutable head with revision and transition ledger. Deletion: no hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_service_activation` | Activation identity |
| subject_id | uuid | NO | none | UNIQUE `uq_service_activation_subject`; FK `fk_service_activation_subject` | One activation row per subject |
| status | text | NO | `'INACTIVE'` | CHECK `ck_service_activation_status` | `INACTIVE`, `ACTIVE` or `SUSPENDED` |
| revision | integer | NO | `1` | CHECK `ck_service_activation_revision` | Must equal the latest expected transition revision |
| created_at | timestamptz | NO | `now()` | none | Creation time |
| updated_at | timestamptz | NO | `now()` | CHECK `ck_service_activation_time_order` | Last transition time |

Named checks:

- `ck_service_activation_status`: `status IN ('INACTIVE','ACTIVE','SUSPENDED')`.
- `ck_service_activation_revision`: `revision >= 1`.
- `ck_service_activation_time_order`: `updated_at >= created_at`.

Keys and indexes:

- Primary key `pk_service_activation (id)`.
- Unique `uq_service_activation_subject (subject_id)`.
- Index `ix_service_activation_status (status)`.
- FK `fk_service_activation_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability: `id`, `subject_id` and `created_at` are immutable; `activation_consent.guard_service_activation` rejects their change. `status`, `revision` and `updated_at` change only through the owning service under the subject lifecycle advisory lock and the activation row lock, appending exactly one matching transition. `INACTIVE → ACTIVE` is the first activation and can be reached by the IVR confirmation path (`source_action_domain = IVR_CONFIRM`) or the authenticated `MINIAPP`/`WEB` client path (`source_action_domain = ACTIVATION`, §9.25/§10.20); both require current explicit consent, a validated event identity/fingerprint and a non-suspended subject. `ACTIVE → SUSPENDED` is an operations action. `SUSPENDED → ACTIVE` is operations-only resume and never calls `issue_first_trial`. An IVR confirmation of a `SUSPENDED` subject is rejected with `REJECT_SUSPENDED` and leaves the row unchanged; a confirmation/activation of an already-`ACTIVE` row appends no duplicate transition. Operations resume also appends exactly one transition and never reissues a trial/grant/balance.

Sensitive representation: no sensitive column.

### 4.38 activation_consent.service_activation_transition

Purpose: append-only activation transition ledger with unique action and status-event identity. Owner: `activation-consent`. Wave V007; slice S2. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_service_activation_transition` | Transition identity |
| activation_id | uuid | NO | none | FK `fk_activation_transition_activation`; part of `uq_activation_transition_revision` | Parent activation |
| subject_id | uuid | NO | none | FK `fk_activation_transition_subject` | Subject |
| from_status | text | NO | none | CHECK `ck_activation_transition_status`; `ck_activation_transition_matrix` | Previous status |
| to_status | text | NO | none | CHECK `ck_activation_transition_status`; `ck_activation_transition_matrix` | New status |
| from_revision | integer | NO | none | CHECK `ck_activation_transition_from_revision` | `0` only for the initial creation transition |
| to_revision | integer | NO | none | CHECK `ck_activation_transition_to_revision`; part of `uq_activation_transition_revision` | `from_revision + 1` |
| source_channel | text | NO | none | CHECK `ck_activation_transition_channel`; `ck_activation_transition_channel_shape` | `IVR`, `MINIAPP`, `WEB` or `OPERATIONS` |
| source_action_domain | text | NO | none | CHECK `ck_activation_transition_action_domain`; part of `uq_activation_transition_action` | `ACTIVATION`, `IVR_CONFIRM` or `OPERATIONS` |
| source_action_event_id | text | NO | none | CHECK `ck_activation_transition_action_event`; part of `uq_activation_transition_action` | Non-null for every domain, including `OPERATIONS` |
| status_event_domain | text | NO | none | CHECK `ck_activation_transition_status_domain`; part of `uq_activation_transition_status_event` | Always `ACTIVATION_STATUS` |
| status_event_id | text | NO | none | CHECK `ck_activation_transition_status_event_id`; part of `uq_activation_transition_status_event` | Unique `ACTIVATION_STATUS` fact identity |
| request_fingerprint | text | NO | none | CHECK `ck_activation_transition_fingerprint` | Canonical transition fingerprint; non-null for every domain |
| actor_account_id | uuid | YES | none | FK `fk_activation_transition_actor_account`; part of `ck_activation_transition_actor_shape` | Human account when applicable |
| actor_employee_id | uuid | YES | none | FK `fk_activation_transition_actor_employee`; part of `ck_activation_transition_actor_shape` | Required for `OPERATIONS` |
| reason_code | text | NO | none | CHECK `ck_activation_transition_reason` | Stable reason code |
| consent_record_id | uuid | YES | none | FK `fk_activation_transition_consent` | Required for a consent-driven transition |
| occurred_at | timestamptz | NO | `now()` | none | Transition time |

Named checks:

- `ck_activation_transition_status`: `from_status IN ('INACTIVE','ACTIVE','SUSPENDED') AND to_status IN ('INACTIVE','ACTIVE','SUSPENDED')`.
- `ck_activation_transition_matrix`: `(from_status = 'INACTIVE' AND to_status = 'ACTIVE') OR (from_status = 'ACTIVE' AND to_status = 'SUSPENDED') OR (from_status = 'SUSPENDED' AND to_status = 'ACTIVE' AND source_action_domain = 'OPERATIONS')`.
- `ck_activation_transition_from_revision`: `from_revision >= 0`.
- `ck_activation_transition_to_revision`: `to_revision = from_revision + 1 AND to_revision >= 1`.
- `ck_activation_transition_channel`: `source_channel IN ('IVR','MINIAPP','WEB','OPERATIONS')`.
- `ck_activation_transition_channel_shape`: `(source_channel = 'OPERATIONS' AND source_action_domain = 'OPERATIONS') OR (source_channel = 'IVR' AND source_action_domain = 'IVR_CONFIRM') OR (source_channel IN ('MINIAPP','WEB') AND source_action_domain = 'ACTIVATION')`.
- `ck_activation_transition_action_domain`: `source_action_domain IN ('ACTIVATION','IVR_CONFIRM','OPERATIONS')`.
- `ck_activation_transition_action_event`: `length(source_action_event_id) BETWEEN 1 AND 128 AND btrim(source_action_event_id) <> ''`.
- `ck_activation_transition_status_domain`: `status_event_domain = 'ACTIVATION_STATUS'`.
- `ck_activation_transition_status_event_id`: `length(status_event_id) BETWEEN 1 AND 128 AND btrim(status_event_id) <> ''`.
- `ck_activation_transition_fingerprint`: `request_fingerprint ~ '^[0-9a-f]{64}$'`.
- `ck_activation_transition_actor_shape`: `(source_action_domain = 'OPERATIONS' AND actor_employee_id IS NOT NULL AND consent_record_id IS NULL) OR (source_action_domain <> 'OPERATIONS' AND consent_record_id IS NOT NULL)`.
- `ck_activation_transition_reason`: `length(reason_code) BETWEEN 1 AND 64 AND btrim(reason_code) <> ''`.

Keys and indexes:

- Primary key `pk_service_activation_transition (id)`.
- Unique `uq_activation_transition_revision (activation_id, to_revision)`.
- Unique `uq_activation_transition_action (source_action_domain, source_action_event_id)`.
- Unique `uq_activation_transition_status_event (status_event_domain, status_event_id)`.
- Index `ix_activation_transition_occurred (subject_id, occurred_at DESC)`.
- FK `fk_activation_transition_activation (activation_id) REFERENCES activation_consent.service_activation(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_activation_transition_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_activation_transition_actor_account (actor_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_activation_transition_actor_employee (actor_employee_id) REFERENCES operations_iam.employee_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_activation_transition_consent (consent_record_id) REFERENCES activation_consent.consent_record(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_activation_transition_immutable` uses `activation_consent.reject_row_mutation()`; `activation_consent_role` has `SELECT` and `INSERT` only. Every transition carries a unique action event identity, a unique `ACTIVATION_STATUS` fact identity and a request fingerprint, including `OPERATIONS`. The transition revision increments exactly one from the current activation revision; a concurrent loser rereads and returns the current committed state or a stable conflict. Operations resume appends only `SUSPENDED → ACTIVE` and never reissues a trial. `ck_activation_transition_matrix` is only a broad DB shape bound; it cannot distinguish the allowed source/state pairs. The sole owner service enforces the narrower matrix: `INACTIVE → ACTIVE` only for `IVR_CONFIRM` or `ACTIVATION`; `ACTIVE → SUSPENDED` only for `OPERATIONS`; `SUSPENDED → ACTIVE` only for `OPERATIONS` resume. A non-`OPERATIONS` `ACTIVE → SUSPENDED`, an `OPERATIONS` `INACTIVE → ACTIVE`, and any client/IVR suspend or resume are rejected by the service before insert even where the broad check would accept the row shape (OQ-7).

Sensitive representation: no sensitive column; actor IDs are internal and consent references are internal.

### 4.39 evidence.media_object

Purpose: immutable private object identity, version and server-computed hash metadata for one uploaded original or derived media object. Owner: `evidence`. Wave V008; slice S3. Lifecycle: append-only/frozen. Deletion: no update/delete; object bytes never enter PostgreSQL.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_media_object` | Application-generated object metadata identity |
| storage_provider | text | NO | none | CHECK `ck_media_object_provider` | Approved provider code; baseline `COS`; no bucket, region or credential default |
| bucket | text | NO | none | CHECK `ck_media_object_bucket` | Private bucket name; never a public URL or response field |
| object_key | text | NO | none | CHECK `ck_media_object_key`; part of `uq_media_object_version` | Controlled immutable key; never exposed publicly |
| object_version_id | text | NO | none | CHECK `ck_media_object_version`; part of `uq_media_object_version` | Object-storage version identity; required for evidence use |
| sha256 | text | NO | none | CHECK `ck_media_object_sha256` | Server-computed lowercase SHA-256 of the stored bytes |
| size_bytes | bigint | NO | none | CHECK `ck_media_object_size` | Server-observed size, greater than zero |
| mime_type | text | NO | none | CHECK `ck_media_object_mime` | Server-sniffed MIME type, nonblank, at most 255 characters |
| crypto_key_ref | text | NO | none | CHECK `ck_media_object_key_ref` | Opaque server-side encryption key reference; never returned |
| worm_status | text | NO | `'PENDING'` | CHECK `ck_media_object_worm_status` | `PENDING`, `CONFIRMED`, `UNSUPPORTED` or `FAILED`; evidence of capability observation only |
| created_at | timestamptz | NO | `now()` | none | Metadata registration time |

Named checks:

- `ck_media_object_provider`: `storage_provider = 'COS'`.
- `ck_media_object_bucket`: `length(bucket) BETWEEN 1 AND 255 AND btrim(bucket) <> ''`.
- `ck_media_object_key`: `length(object_key) BETWEEN 1 AND 1024 AND btrim(object_key) <> ''`.
- `ck_media_object_version`: `length(object_version_id) BETWEEN 1 AND 1024 AND btrim(object_version_id) <> ''`.
- `ck_media_object_sha256`: `sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_media_object_size`: `size_bytes > 0`.
- `ck_media_object_mime`: `length(mime_type) BETWEEN 1 AND 255 AND btrim(mime_type) <> ''`.
- `ck_media_object_key_ref`: `length(crypto_key_ref) BETWEEN 1 AND 128 AND btrim(crypto_key_ref) <> ''`.
- `ck_media_object_worm_status`: `worm_status IN ('PENDING','CONFIRMED','UNSUPPORTED','FAILED')`.

Keys and indexes:

- Primary key `pk_media_object (id)`.
- Unique `uq_media_object_version (bucket, object_key, object_version_id)`.
- Index `ix_media_object_sha256 (sha256)` for duplicate-byte detection only; it is not an authorization or public lookup key.

Append-only control: `trg_media_object_immutable` uses `evidence.reject_row_mutation()`; `evidence_role` has `SELECT` and `INSERT` only. A media object is created only after a successful upload identity or derived-object write; an object version/hash is never overwritten or repointed. `worm_status` is an observed capability fact only; Q11 remains the owner of WORM/storage-class/retention decisions, and no status value promises retention.

Sensitive representation: `bucket`, `object_key`, `object_version_id` and `crypto_key_ref` are internal storage references; they are never returned by a public API, written to logs, encoded in a share URL or included in a redacted event. `sha256` and `size_bytes` are internal validation metadata. No object bytes or permanent URL is stored.

### 4.40 evidence.recording

Purpose: mutable recording-upload and validation state for one IVR recording identity, separating client declarations from server-validated media facts. Owner: `evidence`. Wave V008; slice S3. Lifecycle: mutable head. Deletion: status retirement only; no ordinary hard delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_recording` | Application-generated recording state identity |
| call_id | text | NO | none | FK `fk_recording_call` | Exact S2 canonical call linkage |
| recording_id | text | NO | none | CHECK `ck_recording_protocol_id`; UNIQUE `uq_recording_protocol_id` | Opaque IVR recording identity; retries reuse it |
| media_object_id | uuid | YES | none | FK `fk_recording_media_object` | Set only after the upload object is identified; immutable once set |
| declared_format | text | NO | none | CHECK `ck_recording_declared_format` | IVR/worker declaration; never trusted over validation |
| declared_size | bigint | NO | none | CHECK `ck_recording_declared_size` | Declared byte count, greater than zero |
| declared_duration | integer | NO | none | CHECK `ck_recording_declared_duration` | Declared whole seconds, greater than zero; declaration only |
| declared_sha256 | text | NO | none | CHECK `ck_recording_declared_sha256` | Declared lowercase SHA-256; never wins over the server hash |
| validated_duration | integer | YES | none | CHECK `ck_recording_validated_duration` | Server-validated whole seconds; set once |
| validated_sha256 | text | YES | none | CHECK `ck_recording_validated_sha256` | Server-recomputed lowercase SHA-256; set once |
| status | text | NO | `'DECLARED'` | CHECK `ck_recording_status`; `ck_recording_state_shape` | `DECLARED`, `UPLOADED`, `VALIDATED` or `QUARANTINED` |
| upload_expires_at | timestamptz | NO | none | CHECK `ck_recording_upload_expiry` | Expiry of the controlled upload session; no default or extension |
| created_at | timestamptz | NO | `now()` | none | Upload-state creation time |
| validated_at | timestamptz | YES | none | part of `ck_recording_state_shape` | Time the server validation or quarantine decision was recorded |
| revision | integer | NO | `1` | CHECK `ck_recording_revision` | Conditional state/validation update |

Named checks:

- `ck_recording_protocol_id`: `length(recording_id) BETWEEN 1 AND 128 AND btrim(recording_id) <> ''`.
- `ck_recording_declared_format`: `length(declared_format) BETWEEN 1 AND 64 AND btrim(declared_format) <> ''`.
- `ck_recording_declared_size`: `declared_size > 0`.
- `ck_recording_declared_duration`: `declared_duration > 0`.
- `ck_recording_declared_sha256`: `declared_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_recording_validated_duration`: `validated_duration IS NULL OR validated_duration > 0`.
- `ck_recording_validated_sha256`: `validated_sha256 IS NULL OR validated_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_recording_status`: `status IN ('DECLARED','UPLOADED','VALIDATED','QUARANTINED')`.
- `ck_recording_state_shape`: `(status = 'DECLARED' AND media_object_id IS NULL AND validated_duration IS NULL AND validated_sha256 IS NULL AND validated_at IS NULL) OR (status = 'UPLOADED' AND media_object_id IS NOT NULL AND validated_duration IS NULL AND validated_sha256 IS NULL AND validated_at IS NULL) OR (status = 'VALIDATED' AND media_object_id IS NOT NULL AND validated_duration IS NOT NULL AND validated_sha256 IS NOT NULL AND validated_at IS NOT NULL) OR (status = 'QUARANTINED' AND validated_at IS NOT NULL)`.
- `ck_recording_upload_expiry`: `upload_expires_at > created_at`.
- `ck_recording_revision`: `revision >= 1`.

Keys and indexes:

- Primary key `pk_recording (id)`.
- Unique `uq_recording_protocol_id (recording_id)`.
- Index `ix_recording_call (call_id, created_at DESC)`.
- Index `ix_recording_pending (status, upload_expires_at)` for the owner completion/expiry path.
- FK `fk_recording_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_recording_media_object (media_object_id) REFERENCES evidence.media_object(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Revision/immutability and retry control: `DECLARED → UPLOADED → VALIDATED` or `QUARANTINED`; the explicit failure edge `DECLARED → QUARANTINED` is allowed only when the upload expired or storage/identity failure makes completion impossible, and it creates no successful media fact. The `DECLARED → UPLOADED` transition is the explicit evidence-owner `evidence.record_upload_observed` application service: after the upload completes, it records the exact object/version/hash/size/MIME observations, inserts or reuses only that immutable media object and sets `media_object_id`/`UPLOADED`; a same-object/version/hash retry is idempotent, a changed identity is `RECORDING_IDENTITY_CONFLICT` with no mutation, and no external I/O occurs while the recording row lock is held. Only an `UPLOADED` row proceeds to validation/quarantine; a `DECLARED` completion returns `UPLOAD_OBSERVATION_REQUIRED`. `UPLOADED → QUARANTINED` applies to a different object version, size, MIME, duration or hash or a failed validation. Only `DECLARED`/`UPLOADED` may transition to `QUARANTINED`; `VALIDATED` and `QUARANTINED` are terminal for the recording row. The evidence owner locks the recording row, re-reads `upload_expires_at` with `clock_timestamp()`, and accepts a completion only before expiry. A same-`recording_id` retry with the same object version and server hash returns the first validated or quarantined outcome without a second media object or recording. A different object version, size, MIME, duration or hash for the same `recording_id` while the row is still `DECLARED`/`UPLOADED` transitions once to `QUARANTINED` and never overwrites the first server facts or original object. After `VALIDATED` or `QUARANTINED`, a conflicting callback is refused with the stable `RECORDING_IDENTITY_CONFLICT` (an identical replay returns the original terminal outcome and code) and appends exactly one redacted deterministic Outbox conflict fact; it changes no recording status, media object, validation fact or revision. The conflict fact's Outbox dedupe key is `recording-conflict:` plus the lowercase SHA-256 of the canonical conflict input `{recordingId, originalStatus, originalValidatedSha256, presentedObjectVersionDigest, presentedSha256, presentedSizeBytes, presentedMime, presentedDuration, reasonCode}`, where the object-version/key material is hashed and never stored. The same conflicting callback returns the existing Outbox ID and appends no second fact; a different conflicting payload has a different digest and appends one distinct conflict fact without touching the original. A partial crash before commit leaves only the previous `DECLARED`/`UPLOADED` state; an uncommitted object metadata insert, conflict fact or evidence fact is absent and the same retry converges. `media_object_id`, validation fields and `validated_at` are immutable once set. The accepted-hash comparison uses the server-recomputed `validated_sha256`; `declared_sha256` is evidence of the declaration, not a success proof.
Post-terminal conflict fact: the Outbox row is in event domain `EVIDENCE`, aggregate type `RECORDING`, aggregate id `recording.id` and event type `RECORDING_CONFLICT`, appended through `platform.outbox_append`; this path writes exactly one Outbox conflict fact and no audit row, so a duplicate callback returns the existing Outbox ID and writes nothing. The Outbox unique key is the single dedupe witness. Its redacted payload contains only `recordingId`, `originalStatus`, `conflictDigest`, `presentedSha256`, `presentedSizeBytes`, `presentedMime`, `presentedDuration` and `reasonCode`; object key/version material is never present.

Sensitive representation: `declared_sha256` and `validated_sha256` are hashes only; `recording_id` is an opaque internal/protocol identity. No media bytes, object key/version, bucket or permanent URL is stored in this table.

### 4.41 evidence.evidence_record

Purpose: mutable evidence lifecycle head with immutable captured subject, call/session/reservation links, identity-at-capture, consent version and frozen evidence snapshot; exactly one reciprocal `EVIDENCE_CREATED` event owns it. Owner: `evidence`. Wave V008; slice S3. Lifecycle: mutable head. Deletion: status retirement only; captured facts are never rewritten by a later claim or account change.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_evidence_record` | Application-generated evidence identity |
| subject_id | uuid | NO | none | FK `fk_evidence_subject` | Immutable subject at capture; later claims do not rewrite it |
| source_type | text | NO | none | CHECK `ck_evidence_source_type` | Baseline source is `CALL_RECORDING` |
| source_ref_id | uuid | NO | none | CHECK `ck_evidence_source_ref` | Internal source object ID; for `CALL_RECORDING` equals the `recording.id`; service-validated polymorphic reference, not an FK decision |
| source_event_domain | text | NO | none | CHECK `ck_evidence_source_domain`; part of `uq_evidence_source_event` | Always `EVIDENCE` |
| source_event_id | text | NO | none | CHECK `ck_evidence_source_event_id`; part of `uq_evidence_source_event` | Validated stable source event `eventId`; exactly equals the one `EVIDENCE_CREATED.event_id`; never derived from mutable capture/link/snapshot facts |
| call_id | text | YES | none | FK `fk_evidence_call` | Exact call linkage; for the baseline `CALL_RECORDING` source it is required non-null and must equal the referenced recording's `call_id` |
| session_id | uuid | YES | none | FK `fk_evidence_session` | Exact inbound-session linkage; only with a call |
| reservation_id | uuid | YES | none | FK `fk_evidence_reservation` | Exact usage-reservation linkage when applicable |
| identity_at_capture | jsonb | NO | none | CHECK `ck_evidence_capture_shape`; `ck_evidence_capture_subject` | Immutable redacted identity reference; may include internal `bindingId`; no raw number or live identity |
| consent_version | text | NO | none | CHECK `ck_evidence_consent_version` | Captured recording-notice/consent version; no default or inferred consent |
| status | text | NO | `'PENDING'` | CHECK `ck_evidence_status`; `ck_evidence_state_shape` | `PENDING`, `SOLIDIFIED`, `QUARANTINED` or `FAILED` |
| revision | integer | NO | `1` | CHECK `ck_evidence_revision` | Conditional status update |
| snapshot | jsonb | NO | none | CHECK `ck_evidence_snapshot_shape` | Canonical redacted evidence snapshot; immutable after insert |
| snapshot_sha256 | text | NO | none | CHECK `ck_evidence_snapshot_sha256` | Lowercase SHA-256 of the canonical snapshot |
| created_at | timestamptz | NO | `now()` | none | Evidence creation time |
| solidified_at | timestamptz | YES | none | part of `ck_evidence_state_shape` | Set exactly when `SOLIDIFIED` |

Named checks:

- `ck_evidence_source_type`: `source_type = 'CALL_RECORDING'`.
- `ck_evidence_source_ref`: `source_ref_id IS NOT NULL`.
- `ck_evidence_source_domain`: `source_event_domain = 'EVIDENCE'`.
- `ck_evidence_source_event_id`: `length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> ''`.
- `ck_evidence_capture_shape`: `jsonb_typeof(identity_at_capture) = 'object' AND identity_at_capture ? 'subjectId' AND identity_at_capture ? 'capturedAt' AND identity_at_capture ? 'sourceChannel'`.
- `ck_evidence_capture_subject`: `identity_at_capture->>'subjectId' = subject_id::text`.
- `ck_evidence_consent_version`: `length(consent_version) BETWEEN 1 AND 64 AND btrim(consent_version) <> ''`.
- `ck_evidence_status`: `status IN ('PENDING','SOLIDIFIED','QUARANTINED','FAILED')`.
- `ck_evidence_state_shape`: `(status = 'PENDING' AND solidified_at IS NULL) OR (status = 'SOLIDIFIED' AND solidified_at IS NOT NULL) OR (status IN ('QUARANTINED','FAILED') AND solidified_at IS NULL)`.
- `ck_evidence_revision`: `revision >= 1`.
- `ck_evidence_snapshot_shape`: `jsonb_typeof(snapshot) = 'object' AND snapshot ? 'capture' AND snapshot ? 'content' AND jsonb_typeof(snapshot->'source') = 'object' AND snapshot->>'sourceEventDomain' = 'EVIDENCE' AND snapshot->>'sourceEventId' = source_event_id AND (snapshot->'source'->>'callId') IS NOT DISTINCT FROM call_id AND (snapshot->'source'->>'sessionId') IS NOT DISTINCT FROM session_id::text AND (snapshot->'source'->>'subjectId') IS NOT DISTINCT FROM subject_id::text AND (snapshot->'source'->>'reservationId') IS NOT DISTINCT FROM reservation_id::text AND (snapshot->'source'->>'recordingId') IS NOT DISTINCT FROM source_ref_id::text`.
- `ck_evidence_snapshot_sha256`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_evidence_link_shape`: `session_id IS NULL OR call_id IS NOT NULL`.

Keys and indexes:

- Primary key `pk_evidence_record (id)`.
- Unique `uq_evidence_source_event (source_event_domain, source_event_id)`.
- Index `ix_evidence_subject (subject_id, created_at DESC)`.
- Index `ix_evidence_call (call_id)`.
- Index `ix_evidence_status (status, created_at)` for the owner lifecycle path.
- FK `fk_evidence_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_evidence_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_evidence_session (session_id) REFERENCES ivr.inbound_session(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_evidence_reservation (reservation_id) REFERENCES entitlement_metering.usage_reservation(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- Deferred FK `fk_evidence_source_event (source_event_domain, source_event_id) REFERENCES evidence.evidence_event(event_domain, event_id) DEFERRABLE INITIALLY DEFERRED`, added by `ALTER TABLE` after both V008 tables exist.

Revision/immutability: `subject_id`, `source_type`, `source_ref_id`, `source_event_domain`, `source_event_id`, `call_id`, `session_id`, `reservation_id`, `identity_at_capture`, `consent_version`, `snapshot`, `snapshot_sha256` and `created_at` are immutable under `evidence.guard_evidence_record`. `status`, `solidified_at` and `revision` change only through the evidence owner service: `PENDING → SOLIDIFIED`, `PENDING → QUARANTINED` or `PENDING → FAILED`; all terminal states are absorbing. A solidification retry after `SOLIDIFIED` returns the first immutable snapshot/artifact facts and does not append a second creation event. A late or conflicting recording/validation result after solidification follows the recording terminal-conflict contract in §4.40/§9.27: it is refused with the stable recording conflict and one redacted conflict fact, and cannot rewrite the evidence record, status, facts or snapshot. A later personal claim, account change, organization membership change or subject real-name change never updates any captured column or the snapshot.

Source-reference note: `source_ref_id` is the module-internal target of `source_type`. For the only baseline source, the evidence owner validates that it identifies a `VALIDATED` recording whose `call_id` equals the evidence record's `call_id`, whose trusted call/session/subject chain agrees with the supplied evidence and reservation links, and whose media facts match the frozen snapshot; any cross-call, cross-session, cross-subject or cross-reservation mismatch is `EVIDENCE_LINK_MISMATCH` and creates no evidence fact. It intentionally has no FK in the accepted 81-decision catalog because the source-type target is polymorphic, like the platform aggregate reference; it is never a public key.

Sensitive representation: `identity_at_capture`, `consent_version` and `snapshot` are immutable internal facts. They contain no raw caller/callee number, provider subject, identity number, object key/URL, media bytes or share token; the capture object carries only the subject ID, an internal binding reference if applicable, a caller HMAC/masked projection, source channel and capture time. `snapshot_sha256` is a hash only.

### 4.42 evidence.evidence_artifact

Purpose: immutable original-versus-derived artifact manifest for one evidence record. Owner: `evidence`. Wave V008; slice S3. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_evidence_artifact` | Application-generated artifact-link identity |
| evidence_id | uuid | NO | none | FK `fk_evidence_artifact_evidence`; part of `uq_evidence_artifact` | Parent evidence record |
| media_object_id | uuid | NO | none | FK `fk_evidence_artifact_media`; part of `uq_evidence_artifact` | Distinct original or derived object |
| artifact_role | text | NO | none | CHECK `ck_evidence_artifact_role`; part of `uq_evidence_artifact` | `ORIGINAL` or `DERIVED` |
| created_at | timestamptz | NO | `now()` | none | Manifest insertion time |

Named checks:

- `ck_evidence_artifact_role`: `artifact_role IN ('ORIGINAL','DERIVED')`.

Keys and indexes:

- Primary key `pk_evidence_artifact (id)`.
- Unique `uq_evidence_artifact (evidence_id, artifact_role, media_object_id)`.
- Partial unique `uq_evidence_original_artifact (evidence_id) WHERE artifact_role = 'ORIGINAL'` ensures one original artifact per evidence record.
- Index `ix_evidence_artifact_media (media_object_id)`.
- FK `fk_evidence_artifact_evidence (evidence_id) REFERENCES evidence.evidence_record(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_evidence_artifact_media (media_object_id) REFERENCES evidence.media_object(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_evidence_artifact_immutable` uses `evidence.reject_row_mutation()`; `evidence_role` has `SELECT` and `INSERT` only. An original artifact is inserted atomically with evidence creation/solidification. A derived artifact is a separate media object and a separate row; it never replaces, repoints or overwrites the original. The partial unique is a named schema control not used as an FK target, so the frozen 24-key candidate catalog is unchanged.

Sensitive representation: no direct sensitive value; both IDs are internal. Object keys and versions remain in `media_object` and are never joined into a public response.

### 4.43 evidence.evidence_event

Purpose: append-only evidence event ledger and the composite unique target for the reciprocal evidence-creation cycle. Owner: `evidence`. Wave V008; slice S3. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_evidence_event` | Application-generated event identity |
| evidence_id | uuid | NO | none | FK `fk_evidence_event_record`; part of `uq_evidence_created_event` | Owning evidence record; reverse side of the deferred cycle |
| event_domain | text | NO | none | CHECK `ck_evidence_event_domain`; part of `uq_evidence_event_domain_id`; composite FK target | Always `EVIDENCE` |
| event_id | text | NO | none | CHECK `ck_evidence_event_id`; part of `uq_evidence_event_domain_id`; composite FK target | Stable event identity, 1-128 characters |
| event_type | text | NO | none | CHECK `ck_evidence_event_type` | `EVIDENCE_CREATED`, `EVIDENCE_SOLIDIFIED`, `EVIDENCE_QUARANTINED` or `EVIDENCE_FAILED` |
| occurred_at | timestamptz | NO | `now()` | none | Event occurrence time; never part of a replay comparison |
| payload_redacted | jsonb | NO | none | CHECK `ck_evidence_event_payload` | Redacted metadata only; never media bytes or raw party identifiers |
| payload_sha256 | text | NO | none | CHECK `ck_evidence_event_payload_sha256` | Lowercase SHA-256 of the canonical redacted payload |

Named checks:

- `ck_evidence_event_domain`: `event_domain = 'EVIDENCE'`.
- `ck_evidence_event_id`: `length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''`.
- `ck_evidence_event_type`: `event_type IN ('EVIDENCE_CREATED','EVIDENCE_SOLIDIFIED','EVIDENCE_QUARANTINED','EVIDENCE_FAILED')`.
- `ck_evidence_event_payload`: `jsonb_typeof(payload_redacted) = 'object'`.
- `ck_evidence_event_payload_sha256`: `payload_sha256 ~ '^[0-9a-f]{64}$'`.

Keys and indexes:

- Primary key `pk_evidence_event (id)`.
- Unique `uq_evidence_event_domain_id (event_domain, event_id)`; this is the composite FK target.
- Partial unique `uq_evidence_created_event (evidence_id) WHERE event_type = 'EVIDENCE_CREATED'`; exactly one creation event per evidence record.
- Index `ix_evidence_event_record (evidence_id, occurred_at)`.
- FK `fk_evidence_event_record (evidence_id) REFERENCES evidence.evidence_record(id) DEFERRABLE INITIALLY DEFERRED`; added by `ALTER TABLE` after both V008 tables exist.

Reciprocal creation contract: the record's `(source_event_domain, source_event_id)` must equal the one `EVIDENCE_CREATED` event's `(event_domain, event_id)` and that event's `evidence_id` must equal the record `id`. Both FKs are deferred and added after both tables exist, so the record and event insert atomically in either order in one local transaction. The two owner-local constraint triggers `trg_evidence_record_creation_cycle` (on `evidence_record`) and `trg_evidence_event_creation_cycle` (on `evidence_event`) are `DEFERRABLE INITIALLY DEFERRED` and run at commit. They reject a missing, duplicate, wrong-type, wrong-evidence-owner or mismatched creation event with the stable SQL error `EVIDENCE_CREATION_CYCLE_VIOLATION`; a failed transaction leaves no record, event, artifact or Outbox fact. Non-creation evidence events are append-only lifecycle facts and do not satisfy or replace the one creation event.

Append-only control: `trg_evidence_event_immutable` uses `evidence.reject_row_mutation()`; `evidence_role` has `SELECT` and `INSERT` only. Event identity, type, payload and hash never change. A solidification/quarantine/failure event is appended once when the corresponding record status transition commits; the §10.24 identity and payload fingerprint make a same-semantic retry return the first event and a changed payload under the same identity an `EVIDENCE_EVENT_CONFLICT`, with no second event.

Sensitive representation: `payload_redacted` carries internal evidence IDs, event type, status/reason code, hash prefixes and masked projections only; no media bytes, raw caller/callee number, object key/URL, code or token. `payload_sha256` is a hash only.

### 4.44 sharing.evidence_share_grant

Purpose: mutable share-authorization head that freezes one evidence content scope, deterministic creation identity/fingerprint, verification mode, capability-token digest, validity, admission limit and revocation generation. Owner: `sharing`. Wave V009; slice S3. Lifecycle: mutable head. Deletion: status retirement only; revoke deletes future access, never the evidence.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_evidence_share_grant` | Application-generated grant identity |
| share_id | text | NO | none | CHECK `ck_share_grant_share_id`; UNIQUE `uq_share_grant_share_id` | Opaque internal share identity, deterministically derived from the authenticated creator account and stable creation request ID; the scope is compared through the frozen creation fingerprint, never the public capability |
| capability_token_hash | text | NO | none | CHECK `ck_share_grant_token_hash`; UNIQUE `uq_share_grant_token_hash` | Lowercase SHA-256 of a capability token with at least 128 random bits; plaintext token never stored |
| evidence_id | uuid | NO | none | FK `fk_share_grant_evidence` | Exact solidified evidence scope |
| created_by_account_id | uuid | NO | none | FK `fk_share_grant_creator_account` | Authenticated creator account |
| owner_organization_id | uuid | YES | none | FK `fk_share_grant_owner_organization`; `ck_share_grant_org_member` | Workspace scope; required for `ORGANIZATION_MEMBER` |
| verification_mode | text | NO | none | CHECK `ck_share_grant_verification_mode` | `NONE`, `EXTRACTION_CODE`, `DESIGNATED_MOBILE` or `ORGANIZATION_MEMBER` |
| code_salt | text | YES | none | CHECK `ck_share_grant_code_salt`; part of `ck_share_grant_verification_shape` | Per-grant/per-generation salt; no plaintext code |
| code_hash | text | YES | none | CHECK `ck_share_grant_code_hash`; part of `ck_share_grant_verification_shape` | Salted slow-KDF digest bound to grant and code generation; no KDF parameter value is fixed here |
| code_generation | integer | NO | `0` | CHECK `ck_share_grant_code_generation`; part of `ck_share_grant_verification_shape` | Zero for non-code modes; increments on code rotation |
| valid_from | timestamptz | NO | `now()` | `ck_share_grant_validity` | Frozen grant start |
| expires_at | timestamptz | NO | none | `ck_share_grant_validity` | Explicit approved validity end; no guessed default |
| max_admissions | integer | YES | none | `ck_share_grant_admissions` | Explicit positive limit or NULL for explicit unlimited; no default |
| admissions_used | integer | NO | `0` | `ck_share_grant_admissions` | Incremented once per successful admission under the grant lock |
| download_allowed | boolean | NO | `false` | none | Explicit creator/permission decision |
| content_snapshot | jsonb | NO | none | CHECK `ck_share_grant_snapshot_shape`; `ck_share_grant_snapshot_binding`; `ck_share_grant_mobile_binding` | Frozen content scope, typed creation identity/fingerprint and immutable verification binding; no object key/URL |
| content_sha256 | text | NO | none | CHECK `ck_share_grant_content_sha256` | Lowercase SHA-256 of the canonical content snapshot |
| status | text | NO | `'ACTIVE'` | CHECK `ck_share_grant_status` | `ACTIVE`, `EXPIRED`, `REVOKED` or `LOCKED` |
| revocation_generation | integer | NO | `0` | CHECK `ck_share_grant_revocation_generation` | Monotonic generation; increments on revoke, code rotation and expiry, never at the admission limit |
| revision | integer | NO | `1` | CHECK `ck_share_grant_revision` | Conditional owner transitions |

Named checks:

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

Keys and indexes:

- Primary key `pk_evidence_share_grant (id)`.
- Unique `uq_share_grant_share_id (share_id)`.
- Unique `uq_share_grant_token_hash (capability_token_hash)`.
- Index `ix_share_grant_evidence (evidence_id, status)`.
- Index `ix_share_grant_expiry (status, expires_at)` for the owner expiry path.
- FK `fk_share_grant_evidence (evidence_id) REFERENCES evidence.evidence_record(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_share_grant_creator_account (created_by_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_share_grant_owner_organization (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Creation, verification and state control: the service first derives `share_id` from the authenticated creator account and stable `creationRequestId` and reads any existing grant before any state-dependent mutation. An existing identity is handled by the read-only §2.12 evidence projection and the frozen creation-fingerprint comparison: the service calls `evidence.read_shareable_projection` before recomputing or comparing the fingerprint, reconstructs the deterministic content from the projection plus current request parameters, returns the existing grant metadata only for exact immutable `SOLIDIFIED` facts and the same fingerprint (no new grant/token/event), returns `SHARE_CREATION_CONFLICT` with no mutation for changed scope/parameters or changed/inconsistent artifact/media/content facts, and fails closed on an absent, unresolvable or not-`SOLIDIFIED` projection. A new creation is allowed only for a `SOLIDIFIED` evidence record and only after the creator's current account/workspace access predicate passes under the S1 organization/membership/account advisory locks and exact §10.21 reads. The exact role/resource matrix that grants recording-access permission remains an approved authorization/policy input (owner Product/Security/Sharing; destination sharing policy and authorization design); absent or ambiguous policy fails closed with no grant. The server generates at least 128 random capability bits, returns the plaintext token once at first creation and stores only its SHA-256 digest; a lost creation response returns metadata only, and a usable replacement requires a new `creationRequestId`/new grant/token (the unusable prior grant may be revoked through the existing owner transition). There is no token reconstruction, reissuance action or recoverable secret. For `EXTRACTION_CODE`, it generates an approved code, a fresh salt and a salted slow-KDF digest bound to the grant and `code_generation`; neither the code nor its plaintext is stored or logged. For `DESIGNATED_MOBILE`, only the keyed HMAC of the normalized designated number and its key version are frozen inside `content_snapshot.verification`; no plaintext number or OTP is stored. Admission requires an approved trusted SMS proof for that exact normalized HMAC and intended share/action (consistent with `docs/07-web-capability-hub-spec.md:141`); HMAC comparison/knowledge alone is insufficient, and absent approved verification input fails closed. `NONE` is link possession only and is never treated as identity verification. `ORGANIZATION_MEMBER` requires `owner_organization_id` and verifies the authenticated recipient's current active membership under the logical pair lock. The `content` member is exactly the §2.12 evidence-owner `read_shareable_projection` projection (evidence id/status/source/capture identity, artifact roles and media SHA-256/size/MIME/validated duration facts) with artifacts ordered by the §2.12 fixed non-lexical role rank (`ORIGINAL = 0`, `DERIVED = 1`) before the media hash/size/MIME tie-breakers, and is reproducible from those immutable facts; it never contains a media-object id, object key, object version, bucket, bytes or permanent URL.

Revision/immutability and state transitions: `ACTIVE → EXPIRED`, `ACTIVE → REVOKED`, `ACTIVE → LOCKED`, `LOCKED → REVOKED` and `LOCKED → EXPIRED` are allowed through the existing owner revoke/expiry transitions; `LOCKED → ACTIVE` is allowed only through owner code rotation; `EXPIRED` and `REVOKED` are terminal. There is no invented non-code unlock transition or non-code `LOCKED → ACTIVE` policy. An extension is a conditional `expires_at` increase on an `ACTIVE`, not-yet-expired grant with revision +1; it does not change the frozen content, verification binding or generation and neither retroactively extends an admission recorded under the earlier expiry nor lengthens an existing admission/session. Revoke increments `revocation_generation` by one and appends one `REVOKED` access event. Code rotation replaces `code_salt`/`code_hash`, increments `code_generation` and `revocation_generation` by one, sets a `LOCKED` grant back to `ACTIVE`, and appends one `CODE_ROTATED` event. Expiry sets `EXPIRED` and increments `revocation_generation` by one exactly once when observed/persisted; until then, `clock_timestamp() >= expires_at` still denies. Reaching `max_admissions` never changes status, never increments `revocation_generation` and never invalidates the last admitted session. `LOCKED` blocks new admission/verification attempts only; it does not itself increment generation or invalidate an already-admitted session, and an existing unexpired admission may still be replayed/reconstructed. Every mutating transition is a conditional update with expected `revision`; a same-action retry first resolves its deterministic transition event identity/fingerprint from §2.12 and returns the first recorded outcome before any current status/revision precondition; a zero-row update performs one bounded reread and returns the current state or a stable conflict. The capability-token hash, evidence/creator/workspace scope, verification mode, `valid_from`, `content_snapshot` and `content_sha256` are immutable.

Sensitive representation: `capability_token_hash`, `code_salt` and `code_hash` are one-way/random values; the plaintext capability token and code are never stored, logged, placed in `payload_redacted` or returned after creation. `content_snapshot`/`content_sha256` never expose object keys, bucket, version, permanent URL, raw designated number, OTP, raw creation request ID or unshared evidence; `creationRequestDigest` and `creationFingerprint` are hashes only. `DESIGNATED_MOBILE` stores only the HMAC and key version.

Historical-evidence risk rule: before creating a grant and before every admission, reconstruction or media renewal, the sharing owner re-reads the exact §10.21 subject ownership/risk projection, including the already-redacted `evidence_record.identity_at_capture` column. The captured binding is `identity_at_capture.bindingId` when present; otherwise the service resolves the evidence subject's current active binding set. Any `OPEN` risk in the resolved set denies with `SHARE_RISK_BLOCKED` and creates no grant/admission/ticket; an absent/unresolvable binding or risk projection fails closed. A captured binding that differs from the current active binding is evaluated through the captured reference, and an absent/unresolvable captured or current projection fails closed. A risk-open committed before the re-read is observed, and a later risk-open governs subsequent access without rewriting historical grants, admissions or evidence. `OPEN` risk blocks historical-evidence sharing; it is not converted into a blanket outbound-dialing ban. This uses only read-only exact S1 columns and takes no foreign row lock.

### 4.45 sharing.share_admission

Purpose: append-only durable admission fact; one successful admission counts once and is the PostgreSQL authorization source for the derived playback ticket. Owner: `sharing`. Wave V009; slice S3. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_share_admission` | Application-generated admission identity |
| share_grant_id | uuid | NO | none | FK `fk_share_admission_grant`; part of `uq_share_admission_key` and `uq_share_admission_no` | Parent grant |
| admission_key | text | NO | none | CHECK `ck_share_admission_key`; part of `uq_share_admission_key` | Deterministic `admission:` + SHA-256 of `{shareGrantId, admissionRequestId}`; no raw verification/request input |
| admission_no | integer | NO | none | CHECK `ck_share_admission_no`; part of `uq_share_admission_no`; guard trigger | Monotonic count within the grant; one-based |
| verification_mode | text | NO | none | CHECK `ck_share_admission_mode`; guard trigger | Must equal the parent grant's mode |
| verification_result | text | NO | none | CHECK `ck_share_admission_result`; `ck_share_admission_result_shape` | `VERIFIED` for code/mobile/member; `NOT_REQUIRED` for `NONE`; `NONE` is never recorded as verified identity |
| admitted_at | timestamptz | NO | `now()` | none | Admission commit time |

Named checks:

- `ck_share_admission_key`: `length(admission_key) BETWEEN 1 AND 128 AND btrim(admission_key) <> ''`.
- `ck_share_admission_no`: `admission_no >= 1`.
- `ck_share_admission_mode`: `verification_mode IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER')`.
- `ck_share_admission_result`: `verification_result IN ('VERIFIED','NOT_REQUIRED')`.
- `ck_share_admission_result_shape`: `(verification_mode = 'NONE' AND verification_result = 'NOT_REQUIRED') OR (verification_mode IN ('EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER') AND verification_result = 'VERIFIED')`.

Keys and indexes:

- Primary key `pk_share_admission (id)`.
- Unique `uq_share_admission_key (share_grant_id, admission_key)`.
- Unique `uq_share_admission_no (share_grant_id, admission_no)`.
- Index `ix_share_admission_grant_time (share_grant_id, admitted_at)`.
- FK `fk_share_admission_grant (share_grant_id) REFERENCES sharing.evidence_share_grant(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control and admission count: the sharing owner service (not the evidence owner) locks the grant row `FOR UPDATE`, re-reads status/time/generation/authorization and the current count with `clock_timestamp()`, and inserts at most one admission. After those current checks it looks up an existing admission by `(share_grant_id, admission_key)` before applying the limit: an existing admission with an unexpired session returns the original admission and reissues the derived ticket without a limit test, new `ADMISSION_GRANTED`, insertion or increment, even when `admissions_used = max_admissions`; an existing admission whose session has expired returns the original admission fact with `SHARE_SESSION_EXPIRED` and no new ticket or session. Only a new key is subject to `max_admissions`. `sharing.guard_share_admission` is a `BEFORE INSERT` owner-local trigger that rechecks the parent grant exists, the mode matches, the grant is `ACTIVE` and inside its validity window, the limit is not exceeded, and `NEW.admission_no = grant.admissions_used + 1`. The transaction inserts the admission and its `ADMISSION_GRANTED` access event, then conditionally updates the grant count/revision; a zero-row update or trigger failure rolls everything back. For `ORGANIZATION_MEMBER`, the `ADMISSION_GRANTED` event records the original trusted recipient account/organization pair from the authenticated admission; reconstruction and renewal require the same authenticated recipient and current active organization/membership/account predicates. A `LOCKED` grant permits replay/reconstruction of an existing unexpired admission but forbids a new admission. Opening the verification page, prefetching the link and failed verification create no admission. Admissions are immutable; a later revoke/rotation/expiry never rewrites the historical row, but current grant policy and the recorded admission generation govern whether a session may be reconstructed. `max_admissions` blocks only new admissions; the last admitted session remains within its session/grant validity.

Sensitive representation: `admission_key` is an opaque digest/key; no raw phone, code, token, IP or verification input is stored. `verification_mode`/`verification_result` are codes; the admission row contains no contact data or media URL.

### 4.46 sharing.share_access_event

Purpose: append-only durable access, verification, admission, ticket, playback, transition and failure ledger for sharing; the source of the admission-time generation/expiry facts used to reconstruct a ticket. Owner: `sharing`. Wave V009; slice S3. Lifecycle: append-only/frozen. Deletion: no update/delete.

| Column | Type | Null | Default | Key / constraint | Notes |
| --- | --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_share_access_event` | Application-generated event row identity |
| share_grant_id | uuid | NO | none | FK `fk_share_access_event_grant` | Parent grant |
| share_admission_id | uuid | YES | none | FK `fk_share_access_event_admission`; `ck_share_access_event_admission_shape` | Admission when the event is admission- or ticket-scoped; NULL for page/pre-admission events |
| event_domain | text | NO | none | CHECK `ck_share_access_event_domain`; part of `uq_share_access_event_domain_id` | Always `SHARE_ACCESS` |
| event_id | text | NO | none | CHECK `ck_share_access_event_id`; part of `uq_share_access_event_domain_id` | Deterministic event identity; 1-128 characters |
| event_type | text | NO | none | CHECK `ck_share_access_event_type` | Registry of §2.12 |
| result | text | NO | none | CHECK `ck_share_access_event_result` | `SUCCESS`, `DENIED`, `FAILED` or `IGNORED` |
| occurred_at | timestamptz | NO | `now()` | none | Event occurrence time; never a replay identity |
| risk_digest | text | YES | none | CHECK `ck_share_access_event_risk_digest` | Optional lowercase digest of risk inputs; never raw IP/phone |
| payload_redacted | jsonb | NO | none | CHECK `ck_share_access_event_payload`; `ck_share_access_event_admission_facts` | Redacted event metadata; no code/token/phone/full URL |
| payload_sha256 | text | NO | none | CHECK `ck_share_access_event_payload_sha256` | Lowercase SHA-256 of the canonical redacted payload |

Named checks:

- `ck_share_access_event_domain`: `event_domain = 'SHARE_ACCESS'`.
- `ck_share_access_event_id`: `length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''`.
- `ck_share_access_event_type`: `event_type IN ('PAGE_OPENED','PREFETCH_IGNORED','VERIFICATION_FAILED','LOCKED','ADMISSION_GRANTED','TICKET_ISSUED','TICKET_FAILED','PLAYBACK_AUTHORIZED','SEEK_AUTHORIZED','RENEWED','EXTENDED','CODE_ROTATED','REVOKED','EXPIRED','FAILURE')`.
- `ck_share_access_event_result`: `result IN ('SUCCESS','DENIED','FAILED','IGNORED')`.
- `ck_share_access_event_risk_digest`: `risk_digest IS NULL OR risk_digest ~ '^[0-9a-f]{64}$'`.
- `ck_share_access_event_payload`: `jsonb_typeof(payload_redacted) = 'object'`.
- `ck_share_access_event_payload_sha256`: `payload_sha256 ~ '^[0-9a-f]{64}$'`.
- `ck_share_access_event_admission_shape`: `event_type NOT IN ('ADMISSION_GRANTED','TICKET_ISSUED','TICKET_FAILED','PLAYBACK_AUTHORIZED','SEEK_AUTHORIZED','RENEWED') OR share_admission_id IS NOT NULL`.
- `ck_share_access_event_admission_facts`: `event_type <> 'ADMISSION_GRANTED' OR (payload_redacted ? 'verificationMode' AND payload_redacted->>'verificationMode' IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER') AND payload_redacted ? 'revocationGeneration' AND payload_redacted ? 'codeGeneration' AND payload_redacted ? 'grantExpiresAt' AND payload_redacted ? 'admissionExpiresAt' AND payload_redacted ? 'sessionExpiresAt' AND (payload_redacted->>'verificationMode' <> 'ORGANIZATION_MEMBER' OR (payload_redacted ? 'recipientAccountId' AND payload_redacted ? 'recipientOrganizationId')))`.

Keys and indexes:

- Primary key `pk_share_access_event (id)`.
- Unique `uq_share_access_event_domain_id (event_domain, event_id)`.
- Index `ix_share_access_event_grant (share_grant_id, occurred_at DESC)`.
- Index `ix_share_access_event_admission (share_admission_id, occurred_at)`.
- FK `fk_share_access_event_grant (share_grant_id) REFERENCES sharing.evidence_share_grant(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.
- FK `fk_share_access_event_admission (share_admission_id) REFERENCES sharing.share_admission(id) ON UPDATE RESTRICT ON DELETE RESTRICT`.

Append-only control: `trg_share_access_event_immutable` uses `sharing.reject_row_mutation()`; `sharing_role` has `SELECT` and `INSERT` only. Event identity and payload are immutable. The admission transaction writes exactly one `ADMISSION_GRANTED` event with the admission-time `verificationMode`, the original `recipientAccountId`/`recipientOrganizationId` for `ORGANIZATION_MEMBER`, `revocation_generation`, `code_generation`, `grantExpiresAt`, `admissionExpiresAt` and `sessionExpiresAt` in `payload_redacted`; this is the durable generation/expiry and recipient-binding witness used when Redis is lost. Ticket issue/failure, playback, seek, renewal, extension, code rotation, revoke, expiry and lock outcomes append once with deterministic event identities; transition identities/fingerprints follow §2.12. Duplicate retries return the first event and do not append a second. A ticket failure never changes the admission count. `PAGE_OPENED`, `PREFETCH_IGNORED` and `VERIFICATION_FAILED` events may have a NULL admission and never consume one.

Sensitive representation: `risk_digest` and `payload_redacted` contain only hashes, opaque admission/grant references, generation/time facts, result/reason codes and masked projections; no raw IP address, phone number, share code, capability token, object key/version, bucket or full URL. `payload_sha256` is a hash only.

## 5. Foreign-Key Catalog

### 5.1 Frozen planned FK catalog (81 decisions)

The following block is the accepted 81-row FK decision catalog. It is verbatim from the accepted plan. S1 expands its 11 rows in §5.3, S2 expands its 54 rows in §5.4 and S3 expands the remaining 16 rows in §5.5. Mode names are exactly `FK`, `ALTER_FK`, `DEFERRABLE_FK` and `NO_FK`.

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

### 5.4 S2 foreign-key detail (54 decisions)

All S2 FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`. Parent waves are never later than the FK wave. Three S2 decisions are same-wave `ALTER_FK`: `outbound_call_task.claimed_call_id` (V005), `ivr_decision.reservation_id` (V006) and `customer_order.purchase_consent_id` (V007). Two `NO_FK` rows are the root IVR protocol identities. S2 has no `DEFERRABLE_FK`; the only two deferred constraints are the S3 evidence creation cycle in §5.5.

| Child column | Exact parent column | Parent wave | FK wave | Mode | Creation timing |
| --- | --- | --- | --- | --- | --- |
| catalog_order.catalog_version.product_id | catalog_order.catalog_product.id | V004 | V004 | FK | parent first in V004 |
| catalog_order.catalog_version.created_by_employee_id | operations_iam.employee_account.id | V002 | V004 | FK | with V004 table creation |
| catalog_order.catalog_offer.catalog_version_id | catalog_order.catalog_version.id | V004 | V004 | FK | parent first in V004 |
| catalog_order.catalog_benefit_line.catalog_version_id | catalog_order.catalog_version.id | V004 | V004 | FK | parent first in V004 |
| catalog_order.customer_order.account_id | customer_iam.customer_account.id | V002 | V004 | FK | with V004 table creation |
| catalog_order.customer_order.owner_organization_id | organization_access.organization.id | V002 | V004 | FK | with V004 table creation |
| catalog_order.customer_order.subject_id | subject_number.business_subject.id | V003 | V004 | FK | with V004 table creation |
| catalog_order.customer_order.catalog_version_id | catalog_order.catalog_version.id | V004 | V004 | FK | same-wave parent first |
| catalog_order.customer_order.catalog_offer_id | catalog_order.catalog_offer.id | V004 | V004 | FK | same-wave parent first |
| catalog_order.customer_order.purchase_consent_id | activation_consent.consent_record.id | V007 | V007 | ALTER_FK | column in V004; ordinary immediate FK added by ALTER in V007 |
| catalog_order.payment_order.order_id | catalog_order.customer_order.id | V004 | V004 | FK | same-wave parent first |
| catalog_order.payment_event.payment_order_id | catalog_order.payment_order.id | V004 | V004 | FK | same-wave parent first |
| ivr.outbound_call_task.caller_binding_id | subject_number.caller_number_binding.id | V003 | V005 | FK | with V005 table creation |
| ivr.outbound_call_task.subject_id | subject_number.business_subject.id | V003 | V005 | FK | with V005 table creation |
| ivr.outbound_call_task.owner_account_id | customer_iam.customer_account.id | V002 | V005 | FK | with V005 table creation |
| ivr.outbound_call_task.owner_organization_id | organization_access.organization.id | V002 | V005 | FK | with V005 table creation |
| ivr.outbound_call_task.created_by_account_id | customer_iam.customer_account.id | V002 | V005 | FK | with V005 table creation |
| ivr.outbound_call_task.claimed_call_id | ivr.call_record.call_id | V005 | V005 | ALTER_FK | same-wave ALTER after call_record; ordinary immediate, not deferrable |
| ivr.inbound_session.subject_id | subject_number.business_subject.id | V003 | V005 | FK | with V005 table creation |
| ivr.inbound_session.confirmed_subject_id | subject_number.business_subject.id | V003 | V005 | FK | with V005 table creation |
| ivr.inbound_session.call_id | - | - | - | NO_FK | root IVR protocol identity with no durable parent |
| ivr.call_record.call_id | - | - | - | NO_FK | canonical call-scoped parent created in resolve or direct authorization |
| ivr.call_record.session_id | ivr.inbound_session.id | V005 | V005 | FK | same-wave parent first |
| ivr.call_record.task_id | ivr.outbound_call_task.id | V005 | V005 | FK | same-wave; nullable for the direct path |
| ivr.call_leg.call_id | ivr.call_record.call_id | V005 | V005 | FK | same-wave parent first |
| ivr.call_event.call_id | ivr.call_record.call_id | V005 | V005 | FK | same-wave parent first |
| ivr.ivr_decision.session_id | ivr.inbound_session.id | V005 | V005 | FK | same-wave parent first |
| ivr.ivr_decision.call_id | ivr.call_record.call_id | V005 | V005 | FK | same-wave parent first |
| ivr.ivr_decision.reservation_id | entitlement_metering.usage_reservation.id | V006 | V006 | ALTER_FK | column in V005; ordinary immediate FK added by ALTER in V006 |
| entitlement_metering.trial_policy_version.published_by_employee_id | operations_iam.employee_account.id | V002 | V006 | FK | with V006 table creation |
| entitlement_metering.entitlement_grant.subject_id | subject_number.business_subject.id | V003 | V006 | FK | with V006 table creation |
| entitlement_metering.entitlement_grant.order_id | catalog_order.customer_order.id | V004 | V006 | FK | with V006 table creation |
| entitlement_metering.trial_grant.subject_id | subject_number.business_subject.id | V003 | V006 | FK | with V006 table creation |
| entitlement_metering.trial_grant.trial_policy_version_id | entitlement_metering.trial_policy_version.id | V006 | V006 | FK | same-wave parent first |
| entitlement_metering.trial_grant.entitlement_grant_id | entitlement_metering.entitlement_grant.id | V006 | V006 | FK | same-wave parent first |
| entitlement_metering.entitlement_balance.grant_id | entitlement_metering.entitlement_grant.id | V006 | V006 | FK | same-wave parent first |
| entitlement_metering.entitlement_issuance.order_id | catalog_order.customer_order.id | V004 | V006 | FK | with V006 table creation |
| entitlement_metering.entitlement_issuance.grant_id | entitlement_metering.entitlement_grant.id | V006 | V006 | FK | same-wave nullable unique |
| entitlement_metering.usage_reservation.call_id | ivr.call_record.call_id | V005 | V006 | FK | with V006 table creation |
| entitlement_metering.usage_reservation.balance_id | entitlement_metering.entitlement_balance.id | V006 | V006 | FK | same-wave parent first |
| entitlement_metering.usage_reservation.subject_id | subject_number.business_subject.id | V003 | V006 | FK | with V006 table creation |
| entitlement_metering.usage_fact.reservation_id | entitlement_metering.usage_reservation.id | V006 | V006 | FK | same-wave parent first |
| entitlement_metering.usage_fact.balance_id | entitlement_metering.entitlement_balance.id | V006 | V006 | FK | same-wave parent first |
| entitlement_metering.usage_fact.call_id | ivr.call_record.call_id | V005 | V006 | FK | with V006 table creation |
| activation_consent.consent_record.subject_id | subject_number.business_subject.id | V003 | V007 | FK | with V007 table creation |
| activation_consent.consent_record.ivr_session_id | ivr.inbound_session.id | V005 | V007 | FK | nullable for MINIAPP/WEB |
| activation_consent.consent_record.account_id | customer_iam.customer_account.id | V002 | V007 | FK | nullable for IVR/OPERATIONS |
| activation_consent.consent_record.employee_id | operations_iam.employee_account.id | V002 | V007 | FK | nullable for IVR/MINIAPP/WEB |
| activation_consent.service_activation.subject_id | subject_number.business_subject.id | V003 | V007 | FK | unique; with V007 table creation |
| activation_consent.service_activation_transition.activation_id | activation_consent.service_activation.id | V007 | V007 | FK | same-wave parent first |
| activation_consent.service_activation_transition.subject_id | subject_number.business_subject.id | V003 | V007 | FK | with V007 table creation |
| activation_consent.service_activation_transition.actor_account_id | customer_iam.customer_account.id | V002 | V007 | FK | nullable |
| activation_consent.service_activation_transition.actor_employee_id | operations_iam.employee_account.id | V002 | V007 | FK | required for OPERATIONS |
| activation_consent.service_activation_transition.consent_record_id | activation_consent.consent_record.id | V007 | V007 | FK | same-wave nullable for OPERATIONS |

S2 keeps the frozen 74 ordinary `FK`, 3 `ALTER_FK`, 2 `DEFERRABLE_FK` (S3 only) and 2 `NO_FK` decisions. No S2 FK points forward in wave order, and no S2 table adopts another module's identity as its primary key.

### 5.5 S3 foreign-key detail (16 decisions)

All S3 FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`; no cascade exists. Parent waves are never later than the FK wave. Fourteen are ordinary immediate `FK` constraints. The two remaining decisions are the reciprocal evidence creation cycle: `evidence_record → evidence_event` over the composite `(event_domain, event_id)` and `evidence_event → evidence_record` over `evidence_id`. Both are `DEFERRABLE INITIALLY DEFERRED` and are added by `ALTER TABLE` after both V008 tables and their unique keys exist. There is no S3 `ALTER_FK` or `NO_FK` decision. `evidence.evidence_record.source_ref_id` is a service-validated polymorphic internal source reference and is not an FK decision; it is not a public key and does not add a row to the accepted 81-decision catalog.

| Child column | Exact parent column | Parent wave | FK wave | Mode | Creation timing |
| --- | --- | --- | --- | --- | --- |
| evidence.recording.call_id | ivr.call_record.call_id | V005 | V008 | FK | with V008 table creation |
| evidence.recording.media_object_id | evidence.media_object.id | V008 | V008 | FK | parent first in V008; column nullable until upload identity exists |
| evidence.evidence_record.subject_id | subject_number.business_subject.id | V003 | V008 | FK | with V008 table creation |
| evidence.evidence_record.call_id | ivr.call_record.call_id | V005 | V008 | FK | with V008 table creation; nullable for non-call sources |
| evidence.evidence_record.session_id | ivr.inbound_session.id | V005 | V008 | FK | with V008 table creation; nullable and only with a call |
| evidence.evidence_record.reservation_id | entitlement_metering.usage_reservation.id | V006 | V008 | FK | with V008 table creation; nullable when no reservation applies |
| evidence.evidence_record.source_event_domain,evidence.evidence_record.source_event_id | evidence.evidence_event.event_domain,evidence.evidence_event.event_id | V008 | V008 | DEFERRABLE_FK | ALTER after both V008 tables and `uq_evidence_event_domain_id`; `DEFERRABLE INITIALLY DEFERRED`; composite circular creation link |
| evidence.evidence_artifact.evidence_id | evidence.evidence_record.id | V008 | V008 | FK | same-wave parent first |
| evidence.evidence_artifact.media_object_id | evidence.media_object.id | V008 | V008 | FK | same-wave parent first |
| evidence.evidence_event.evidence_id | evidence.evidence_record.id | V008 | V008 | DEFERRABLE_FK | ALTER after both V008 tables; `DEFERRABLE INITIALLY DEFERRED`; opposite side of the evidence creation cycle |
| sharing.evidence_share_grant.evidence_id | evidence.evidence_record.id | V008 | V009 | FK | with V009 table creation |
| sharing.evidence_share_grant.created_by_account_id | customer_iam.customer_account.id | V002 | V009 | FK | with V009 table creation |
| sharing.evidence_share_grant.owner_organization_id | organization_access.organization.id | V002 | V009 | FK | with V009 table creation; nullable except `ORGANIZATION_MEMBER` |
| sharing.share_admission.share_grant_id | sharing.evidence_share_grant.id | V009 | V009 | FK | same-wave parent first |
| sharing.share_access_event.share_grant_id | sharing.evidence_share_grant.id | V009 | V009 | FK | same-wave parent first |
| sharing.share_access_event.share_admission_id | sharing.share_admission.id | V009 | V009 | FK | same-wave parent first; nullable only for page/pre-admission/failure events |

The two deferred FKs are the only cycle edges; neither is an `ALTER_FK` ordinary-immediate constraint. The deferred constraint triggers are installed after these constraints in V008. The frozen totals remain 81 FK decisions = 74 ordinary `FK` + 3 late-added `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK`; the S3 ordinary rows are part of the 74 and the two deferred rows are the only `DEFERRABLE_FK` rows. No parent is created after its child FK wave, and no S3 table is used by a later slice because none exists.

## 6. Invariant Matrix

### 6.1 Frozen planned invariant catalog (40 invariants)

The following 40 invariant rows are verbatim from the accepted plan. Each has one owner, named collaborators, a primary enforcement class and a verified slice. S1 expands its rows in §6.2, S2 expands its portions in §6.3 and S3 expands its portions in §6.4; the frozen rows themselves are unchanged.

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

S1 has no later-slice prerequisite. The S1 invariants use only V001-V003 tables and functions. S2 and S3 invariant portions are authored in §6.3 and §6.4; S1 does not claim their verification.

### 6.3 S2 invariant enforcement expansion

This table makes the S2 portions generation-ready. "Location" names the exact constraint, condition, owner service or transaction rule. "Real behavior assertion" is a document-level requirement for the later executable suite; no PostgreSQL or IVR test was run in this authoring.

| Invariant (S2 portion) | Owner | Primary class | Exact S2 location | Real behavior assertion and evidence requirement |
| --- | --- | --- | --- | --- |
| INV-08 (S2) | activation-consent | DB+TX+SERVICE | `consent_record` explicit `ACCEPT`/`DECLINE` and unique `(event_domain, event_id)`; `service_activation` and transition revision; `issue_first_trial`; `ivr.select_call_action`; §9.11 and §9.20 | No key `1`, no explicit accept, missing/stale prompt or policy/content, or a `SUSPENDED` fresh subject produces no accepted consent, transition, trial/grant/balance, reservation or success Outbox. Replay/identity handling precedes every gate. An `ACTIVE` activation with missing/expired consent returns `GUIDE_ACTIVATION`/`CONSENT_REQUIRED`; stale confirmation content returns `REJECT`/`CONSENT_REQUIRED` before accepted side effects. A category-2 domain no-dial result commits activation/trial and the session stays `CONFIRMED`; category 3 rolls back every write. |
| INV-09 (S2) | activation-consent | DB+TX | `uq_service_activation_subject`; `uq_activation_transition_revision`, `uq_activation_transition_action`, `uq_activation_transition_status_event`; `ck_activation_transition_to_revision`; §9.11 | One activation row per subject; every transition has one unique action event, one unique `ACTIVATION_STATUS` event and a fingerprint; `to_revision = from_revision + 1`; concurrent transitions serialize on the subject lifecycle and activation row lock and only one revision wins; a loser rereads and returns the current state or a stable conflict. |
| INV-10 (S2) | activation-consent | TX | `service_activation` status and subject lifecycle lock; §9.11, §9.19 | `SUSPENDED` cannot self-resume through IVR or ordinary confirmation. A fresh suspended confirmation returns `REJECT_SUSPENDED`, leaves activation suspended and writes no accepted consent/trial/reservation/Outbox. Existing decision/idempotency replay returns the original result and never changes a `CONFIRMED` session to `REJECTED`. Operations resume appends only `SUSPENDED → ACTIVE` with its own event identity and never calls `issue_first_trial`. |
| INV-11 (S2) | ivr | DB+TX | `uq_outbound_task_live_caller`; `uq_call_record_active_caller`; canonical-caller lock; §9.12-§9.14 | One non-terminal task and one `ACTIVE`/`UNKNOWN` call per canonical caller across all accounts/workspaces. Both fresh paths exclude the current `callId`. A competing call receives `REJECT_CALL_IN_PROGRESS` with a terminal `REJECTED` parent only when the current call is absent or a never-started/rejectable `ACTIVE` row, no claim/reservation, and the competing row is untouched. An existing current `UNKNOWN` row is referenced unchanged. The index conflict is mapped after a deterministic reread, never exposed raw. |
| INV-12 (S2) | ivr | DB+TX | `outbound_call_task.ttl_seconds`/`expires_at`, `ck_outbound_task_expiry`; conditional claim with `expires_at > clock_timestamp()`; §9.12 and §9.13 | Only a `PENDING` task unexpired under database time after lock acquisition is claimable. The default 30-second TTL is frozen at creation and server-configurable; a duplicate, retry, replacement or resolve replay never extends `expires_at`. Exactly 30 seconds or later is expired; a stale `PENDING` converges to `EXPIRED` under the caller lock before a new task. |
| INV-13 (S2) | ivr | TX | One IVR-owned local transaction; conditional claim; current `call_record`; `reserve_call_usage`; typed decision; §9.11, §9.13, §9.14 | A successful task-backed decision atomically claims the task, creates or binds the current `ACTIVE` call, freezes one decision, creates one reservation and commits only after the active-call guard, current caller/subject/binding/owner-workspace lifecycle, activation/consent, `PENDING`/revision/unexpired task and positive entitlement pass. No `CLAIMED` task commits without its reservation. A direct no-task path applies only binding/subject/activation/consent/entitlement and no login/membership/real-name predicate. A technical failure rolls back all of these facts together; an expected claim/reservation denial rolls back to the same-connection attempt savepoint and freezes one no-dial decision. |
| INV-14 (S2) | ivr | DB+TX | `uq_ivr_decision_call_purpose`; `uq_ivr_decision_source`; `uq_call_event_domain_id`; one `ivr.select_call_action`; §9.11-§9.14 and §9.21 | Both entries use one ordered predicate implementation. Same `callId`/purpose with the same canonical fingerprint returns the first decision and frozen action; a changed fingerprint or immutable caller/access/node conflict is stable conflict. `ACTIVATION_NEXT_ACTION` never overwrites `INBOUND_RESOLVE`; a repeated resolve returns the original `GUIDE_ACTIVATION`. Each IVR event ID is unique in `IVR_EVENT`. A concurrent unique loser rereads and returns the existing decision or a stable conflict. |
| INV-16 (S2) | entitlement-metering | DB+TX | `trial_grant` campaign check, `uq_trial_grant_subject`, `uq_trial_grant_source`; `issue_first_trial`; subject lock; §9.11, §10.9 | Exactly one permanent `FIRST-TRIAL` per subject regardless of policy version, channel, claim, re-entry or a new campaign key. The granting action domain/event/fingerprint are persisted. Same action plus same fingerprint returns original ids as `REPLAY`; same action plus changed fingerprint returns `TRIAL_ACTION_CONFLICT`; a different action returns `ALREADY_GRANTED` original ids. No second grant or balance exists in any case. |
| INV-17 (S2) | entitlement-metering | DB | `uq_entitlement_grant_paid`; `ck_entitlement_grant_source_shape`; append-only grant | Grants are immutable. A paid grant is unique by order/installment/type; a trial grant is unique through `trial_grant.subject_id` and `trial_grant.entitlement_grant_id`. A source mismatch or duplicate insert fails; no grant row is updated or repurposed. |
| INV-18 (S2) | entitlement-metering | DB+TX | `ck_entitlement_balance_units`; balance row lock; `revision`; §9.15, §9.18 | `reserved_units + consumed_units <= granted_units` always. Reservation increments reserved; settlement decrements reserved and increments consumed; release decrements reserved. Each update is conditional on the balance revision and affects exactly one row; concurrent reservation/settlement cannot overcommit. |
| INV-19 (S2) | entitlement-metering | DB+TX+SERVICE | `uq_usage_reservation_call`; `uq_usage_fact_reservation_type`; state/unit checks; same-connection savepoint; §9.11, §9.13-§9.16 | One reservation per call, one terminal transition and one settlement fact. A task claim never commits without its reservation. Authorized units never exceed committed availability. Unused units release once; late/duplicate events do not recharge. A domain denial after a tentative claim rolls back to the attempt savepoint and freezes one no-dial decision; a technical failure rolls back the whole transaction. |
| INV-20 (S2) | catalog-order | DB+TX+SERVICE | `uq_catalog_product_code`; `uq_catalog_version_active`; published immutability guard; catalog matrix checks; §7.3 and §9.17 | One immutable product identity; at most one `ACTIVE` version per product. A published version's commercial content and benefit lines are immutable; a new publication atomically offsales the previous active version. The type/billing/grant matrix and enterprise-quote boundary are checked server-side and by CHECK. |
| INV-21 (S2) | catalog-order | DB+TX+SERVICE | `uq_customer_order_idempotency`; `request_fingerprint`; platform `PURCHASE`; §9.17 | Orders come only from `MINIAPP`/`WEB` and require account/workspace/subject access, ordinary-client real-name, current active activation, explicit purchase consent and an exact `ACTIVE` offer/version token. The amount is server-derived. The immutable snapshot, consent reference and fingerprint are frozen. `begin`/`complete` in the same transaction: identical retry returns the first order, changed input conflicts, and no second order or entitlement-side status is created. |
| INV-22 (S2) | catalog-order | DB+TX | `uq_payment_event_provider_event`; `signature_verified`; order/payment row lock; §9.18 | Payment events are unique by provider/event. An unverified event never advances payment. `UNKNOWN` blocks duplicate checkout until the original order is queried. `PAID` and entitlement issuance are independently observable; a duplicate verified event returns the first processing result and never produces a second payment fact. |
| INV-23 (S2) | entitlement-metering | DB+TX+SERVICE | `uq_issuance_line`; `uq_issuance_grant`; frozen order snapshot; same-transaction grant/balance/Outbox; §9.18 | Issuance is idempotent by order/installment/type and consumes the frozen order snapshot, never current catalog state. Grant, balance and Outbox commit in one transaction; a partial failure records `FAILED` and retry never duplicates the grant. No module writes another module's issuance or order status directly. |
| INV-32 (S2) | ivr | DB+TX | `uq_outbound_task_idempotency`; identical-tuple check; canonical-caller lock; §9.12 | Task create/replace/cancel is idempotent and workspace/creator-scoped. Same key/input returns the existing task without TTL refresh; same key/changed fingerprint conflicts. An identical caller/callee/business/workspace `PENDING` duplicate returns the existing task without refresh. Replace/cancel is allowed only for the current workspace's and account's `PENDING` task. A `CLAIMED` task cannot be replaced or cancelled. |
| INV-34 (S2) | ivr | DB+TX | `uq_ivr_decision_call_purpose`; `uq_ivr_decision_source`; canonical-caller lock; no-task positive check; direct purpose/action CHECK; §9.14 | Direct DTMF authorization accepts exactly two current purpose chains: (a) `INBOUND_RESOLVE = ENTER_CALLEE` with no later superseding/conflicting decision, or (b) `INBOUND_RESOLVE = GUIDE_ACTIVATION` + `CONFIRMED` session + committed `ACTIVATION_NEXT_ACTION = ENTER_CALLEE`. It rejects an obsolete earlier `ENTER_CALLEE`, a later no-dial decision, an unconfirmed/contradictory chain, a non-empty `taskId`, any task-backed fallback and any `PENDING`/`CLAIMED` task after a positive no-task check; it excludes the current `callId` from the active-call guard. It reuses current caller/activation/consent/single-balance predicates with no registration/login/real-name prerequisite. Success freezes purpose `DIRECT_AUTHORIZATION`, `action` null, `taskMatch=NONE`, null task fields, committed reservation/seconds/cutoff from the selected balance and the callee only under `authorization.calleeNumber` in the encrypted payload. A refusal uses a permitted non-dial action with no reservation. Retry returns the first result and never redials; changed confirmation content conflicts. |
| INV-35 (S2) | platform-reliability | DB+SERVICE | Composite uniques on `call_event`, `ivr_decision`, `usage_fact`, `consent_record`; platform `IVR_CONFIRM` fingerprint; §10.14 | Event/action identity is unique within its actual domain. The same string may exist in different domains. The same `IVR_CONFIRM` event reused on another call conflicts because `callId` is in the fingerprint; changed confirmation content for the same event also conflicts. `usage_fact` source identity is domain-scoped; a payment event may drive multiple issuance rows but never a duplicate grant. |
| INV-36 (S2) | entitlement-metering | DB+TX+SERVICE | `uq_issuance_line`; issuance state shape; conditional revision; privilege matrix; §9.18 | One durable issuance row per order/installment/type with owned `PENDING`/`FAILED`/`ISSUED`, attempt count, revision, error code and grant link. Partial line failure is observable; no other module writes issuance or order status. Retry moves `FAILED → PENDING`, increments attempts and never duplicates the grant. |
| INV-37 (S2) | activation-consent | DB+TX | Platform `IVR_CONFIRM` scope; `request_fingerprint`; `uq_ivr_decision_call_purpose`; `source_action_event_id` NOT NULL; §9.11 and §9.21 | The wrapper authenticates immutable call/session/caller identity, handles an existing `(callId, ACTIVATION_NEXT_ACTION)` decision and event-level `REPLAY`/`CONFLICT` before `SUSPENDED` or any accepted side effect. A new event ID with the same fingerprint returns the existing decision and completes the new event row with that decision ID; changed input rolls back the `NEW` row and conflicts. A `CONFIRMED` session is never moved to `REJECTED`. A fresh confirmation requires the original `GUIDE_ACTIVATION` resolve. The first result is the next-action decision ID for every accepted branch, including already `ACTIVE` and call-rejected outcomes, and the rejected decision ID for a suspended fresh rejection. Category 1 writes no accepted side effect; category 2 commits activation/trial with the no-dial decision; category 3 commits nothing. |
| INV-38 (S2) | entitlement-metering | DB+TX+SERVICE | `RECONCILING` state; reconciliation fields; caller lock; conditional terminal update; `uq_usage_fact_source`; §9.16 | A reservation with a missing, late or duplicate end event is reconciled against authoritative IVR usage. Known use is settled first and the remainder released once. Unknown use remains `RECONCILING`, keeps the active-call guard occupied and blocks a new reservation; it is never released. A late authoritative event converges once; a duplicate returns the first settlement. |
| INV-40 (S2 portion) | platform-reliability | DB+TX+SERVICE | V006 `entitlement_metering.issue_first_trial`; fixed `search_path`; named grants; no transaction control; §10.9 and §10.11 | `issue_first_trial` is created only in V006 after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance`; it is `SECURITY DEFINER`, owned by `entitlement_metering_owner`, fixed `search_path`, `PUBLIC` denied, EXECUTE only to `activation_consent_role`/`entitlement_metering_role`; it performs no transaction control; a forced failure rolls back trial/grant/balance/Outbox with the caller; `GRANTED`/`REPLAY`/`ALREADY_GRANTED` and conflict semantics hold. The four V001 functions remain unchanged and S1-owned; the shared IVR action service is not a SQL function. |

S2 invariants use only V001-V007 tables and the five registered functions. No S2 invariant requires S3 evidence or sharing.

### 6.4 S3 invariant enforcement expansion

This table makes the five S3 invariant portions generation-ready. "Location" names the exact constraint, key, owner service, trigger or transaction rule. "Real behavior assertion" is a document-level requirement for the later executable suite; no PostgreSQL or Redis test was run in this authoring.

| Invariant (S3 portion) | Owner | Primary class | Exact S3 location | Real behavior assertion and evidence requirement |
| --- | --- | --- | --- | --- |
| INV-15 (S3) | ivr | DB | `fk_recording_call`; `fk_evidence_call`, `fk_evidence_session`, `fk_evidence_reservation`; unique reservation/call keys from S2; separate recording/evidence/call status columns; §5.5 and §9.27-§9.28 | Every recording names exactly one `ivr.call_record.call_id`; an evidence record's optional call/session/reservation links must resolve to the same call/subject identities, and a supplied session requires a call. Call completion never sets evidence status; recording validation and evidence solidification are separate transactions/statuses. A terminal call with no recording/evidence is valid; an evidence record never claims a call/session/reservation that does not match its frozen capture facts. |
| INV-24 (S3) | evidence | DB+SERVICE | `media_object` unique `(bucket, object_key, object_version_id)` and immutable hash/size/MIME/version; `recording` declared-vs-validated columns and state shape; `evidence.complete_recording` owner service; §9.27 | Server-observed object version, hash, size, MIME, duration and malware result determine `VALIDATED`; a client-declared hash/duration never wins. Bytes stay in private storage; PostgreSQL stores metadata only. A duplicate completion with the same object version/server hash returns the first result; a conflicting version/hash/size/MIME/duration quarantines without overwriting the original media object. An overwrite attempt on `media_object` or its identity/version/hash fails. |
| INV-25 (S3) | evidence | DB+TX | `uq_evidence_source_event`; `uq_evidence_event_domain_id`; `uq_evidence_created_event`; both deferred FKs; `trg_evidence_record_creation_cycle` and `trg_evidence_event_creation_cycle`; `evidence.guard_evidence_record`; §9.28 | Exactly one `EVIDENCE_CREATED` event owns each evidence record; its `(event_domain,event_id)` equals the record's `(source_event_domain,source_event_id)` and its `evidence_id` equals the record ID. Record and event insert atomically in either order in one local transaction; a missing, duplicate, wrong-type, wrong-owner or mismatched creation event fails at commit with `EVIDENCE_CREATION_CYCLE_VIOLATION`. A failed transaction leaves no record, event, artifact or Outbox fact. Captured subject/session/identity/consent and all later evidence snapshots are immutable across claims and account changes. |
| INV-26 (S3) | sharing | DB+TX | Immutable `content_snapshot`/`content_sha256`; unique admission key/no; `sharing.guard_share_admission`; grant row lock and conditional status/generation/revision update; §9.29-§9.30 | Share content/evidence scope and verification binding are frozen. A successful admission increments `admissions_used` once under the grant row lock; the admission key/no are unique and immutable. Revoke and code rotation increment `revocation_generation`; expiry persists `EXPIRED` with a generation increment and denies even before the write; all three block new or renewed sessions. Reaching `max_admissions` blocks only new admissions and never bumps the generation. A verification failure/open/prefetch consumes no admission. |
| INV-39 (S3) | sharing | DB+TX+SERVICE | Durable `share_admission`; deterministic ticket event identities; `ADMISSION_GRANTED` generation/expiry/recipient witness; PostgreSQL-commit-before-Redis contract; §9.30-§9.31 | PostgreSQL admission commits before any Redis ticket. The ticket key is derived from `share_grant_id + admission_id`. Same-key retry returns the original admission and reissues the ticket without incrementing; an existing admission is resolved before the limit and an expired original never renews. Lost response, lost cache or Redis failure never increments twice; a Redis failure fails closed with a durable `TICKET_FAILED` event and no successful media authorization. Reconstruction compares the admission-time generation/expiry witness with the current grant status/time/generation and, for `ORGANIZATION_MEMBER`, re-authenticates the original recipient and current active organization/membership/account, so a stale admission cannot bypass revoke, code rotation, expiry, membership revocation or a wrong-tenant check. No network call is made while a PostgreSQL row lock is held. |

Final cross-slice closure: INV-30 (deletion/retirement) now covers all 44 tables with no S3 hard delete or cascade; INV-31 (sensitive fields) now covers the S3 object/evidence/share representations and forbids plaintext tokens/codes/phones/URLs; INV-40 (function/security boundaries) remains exactly the five registered functions, and S3 adds no function-wave row, no `SECURITY DEFINER` published function and no cross-schema write path. The S3 trigger functions are owner-local `SECURITY INVOKER` controls described in §10.22. S1, S2 and S3 owners/enforcement classes remain single-owner; no invariant has shared ownership.

## 7. State Machines

### 7.1 S1 state machines

These are the state machines authored in S1. The S2 state machines are authored in §7.3; S3 state machines are authored in §7.4.

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

### 7.3 S2 state machines

These are the S2 state machines authored in this slice. Every mutable transition is a conditional update with expected `revision`; a zero-row update performs one bounded reread and returns the current state or a stable conflict. Append-only decision, grant, consent and transition facts have no state transition.

| Aggregate | Allowed states | Allowed transitions and guards | Owner |
| --- | --- | --- | --- |
| Catalog version | `DRAFT`, `ACTIVE`, `OFFSALE` | `DRAFT → ACTIVE` on publication: under the owner publication lock, first retires the previous `ACTIVE` version to `OFFSALE` with revision +1, then sets the candidate `published_at` and `ACTIVE` with revision +1; content immutable; `ACTIVE → OFFSALE`; `OFFSALE` terminal for sale. Only one `ACTIVE` version per product at every statement (immediate partial unique); published content never changes. | catalog-order |
| Customer order | `UNPAID`, `UNKNOWN`, `PAID`, `CLOSED` (payment dimension only) | `UNPAID → UNKNOWN/PAID/CLOSED`; `UNKNOWN → UNPAID/PAID/CLOSED`; `PAID` and `CLOSED` terminal for the ordinary flow. `UNKNOWN` blocks a second checkout until the original order is queried. Only verified provider events advance payment; the commercial snapshot never changes. | catalog-order |
| Payment order | `UNPAID`, `UNKNOWN`, `PAID`, `CLOSED` | Same verified-evidence transitions as the order; `PAID` requires a provider transaction ID; `UNKNOWN` is not failure and is resolved by querying the original merchant order. Status changes under the payment/order row lock with revision. | catalog-order |
| Entitlement issuance | `PENDING`, `ISSUED`, `FAILED` | `PENDING → ISSUED` (grant, balance and Outbox in one transaction); `PENDING → FAILED` (records `last_error_code`); `FAILED → PENDING` retry under the issuance row lock; `ISSUED` terminal. One row per order/installment/type. | entitlement-metering |
| Outbound task | `PENDING`, `CLAIMED`, `REPLACED`, `CANCELLED`, `EXPIRED`, `REJECTED`, `COMPLETED`, `FAILED` | `PENDING → CLAIMED` only by the shared IVR service after all predicates and with the claim/reservation transaction; the successful claim also assigns the current pre-savepoint call's `task_id` from NULL to that task and requires reciprocal `claimed_call_id`/`task_id` equality; a category-2 rollback clears both assignments while the call survives with `task_id = NULL`; `PENDING → REPLACED/CANCELLED/EXPIRED/REJECTED`; `CLAIMED → COMPLETED/FAILED`; every terminal state is immutable. A duplicate returns the existing task without TTL refresh. `CLAIMED` cannot be replaced or cancelled and never returns to `PENDING`. | ivr |
| Inbound session | `RECEIVED`, `CONFIRMED`, `ABANDONED`, `REJECTED` | `RECEIVED → CONFIRMED` only with trusted caller, explicit key `1`, current consent/prompt, same confirmed subject, a non-suspended subject re-read under the lifecycle lock and exactly one committed `ACTIVATION_NEXT_ACTION`; `RECEIVED → ABANDONED/REJECTED`; `CONFIRMED` is terminal for confirmation-state purposes and is never re-rejected by replay. A fresh suspended/competing-call rejection may reject only a never-confirmed session. | ivr |
| IVR decision | Frozen purpose-specific result (no status transition) | First response per `(call_id, purpose)` is immutable. `INBOUND_RESOLVE`, `ACTIVATION_NEXT_ACTION` and `DIRECT_AUTHORIZATION` are distinct; the post-activation decision never rewrites the resolve decision. Same fingerprint replays; changed fingerprint conflicts before side effects. Only `ivr.select_call_action` inserts. | ivr |
| Direct authorization | `NONE → AUTHORIZED` or a frozen refusal | Requires a valid current purpose chain with no task and no task ID: either the frozen `INBOUND_RESOLVE = ENTER_CALLEE` with no superseding/conflicting decision, or `INBOUND_RESOLVE = GUIDE_ACTIVATION` plus a `CONFIRMED` session and a committed `ACTIVATION_NEXT_ACTION = ENTER_CALLEE`. Rejects an obsolete earlier `ENTER_CALLEE`, a later no-dial decision, any task-backed fallback, and a non-empty taskId. One reservation per call from the selected balance; success has `action` null with committed reservation fields and the encrypted `authorization.calleeNumber`; refusal uses a permitted non-dial action and no reservation. The shared current caller/activation/consent/no-task/entitlement predicates exclude no registration/login/real-name prerequisite. Replay returns the first result and never redials; cutoff never renews. | ivr |
| Call record | `ACTIVE`, `ENDED`, `FAILED`, `UNKNOWN`, `REJECTED` | `ACTIVE → ENDED/FAILED/UNKNOWN`; `UNKNOWN → ENDED/FAILED` only through authoritative reconciliation; a competing call is created terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS`; only a never-started/rejectable current `ACTIVE` row may become `REJECTED`; `REJECTED` terminal and never holds the active guard. A current `UNKNOWN` row is never finalized `REJECTED` or released. | ivr | `task_id` is NULL until a successful task claim assigns it exactly once from NULL to that task and is immutable thereafter; an arbitrary relink, task switch or second call is rejected.
| Usage reservation | `HELD`, `RECONCILING`, `SETTLED`, `RELEASED` | `HELD → SETTLED/RELEASED/RECONCILING`; `RECONCILING → SETTLED/RELEASED`; one terminal transition. Known use settles first and the remainder releases once; unknown use stays `RECONCILING` and is never released; `authorization_expires_at` never renews on replay. | entitlement-metering |
| Trial | `GRANTED` (derived `EXHAUSTED`/`EXPIRED` from balance and time) | No row transition; `trial_grant` is immutable. One permanent row per subject; replay and already-granted return the original ids and never mutate the grant. | entitlement-metering |
| Service activation | `INACTIVE`, `ACTIVE`, `SUSPENDED` | `INACTIVE → ACTIVE` only through accepted IVR confirmation (`IVR_CONFIRM`) or MINIAPP/WEB client activation (`ACTIVATION`); `ACTIVE → SUSPENDED` only through an `OPERATIONS` suspend; `SUSPENDED → ACTIVE` only through an `OPERATIONS` resume. The broad `ck_activation_transition_matrix` check is shape-only; the sole owner service rejects every other source/state pair (OQ-7). An IVR confirmation of `SUSPENDED` rejects with `REJECT_SUSPENDED`; resume never calls `issue_first_trial`. Revision increases by one with each transition. | activation-consent |
| Consent record | `ACCEPT`, `DECLINE` fixed at insert | No transition; append-only. A missing/invalid/stale explicit consent creates no accepted record; no inferred consent exists. | activation-consent |

S2 transition and terminal rules:

- `ISSUED` issuance, `PAID`/`CLOSED` ordinary payment, all terminal task states, `REJECTED`/`ENDED`/`FAILED` calls, `SETTLED`/`RELEASED` reservations, `OFFSALE` versions and terminal transitions are immutable.
- `UNKNOWN` is an occupied call state, not a failure, success or redial permission. It converges only through reconciliation and never releases unknown use.
- A normal domain no-dial result is not a terminal failure of the activation/trial facts. Category 1, category 2 and category 3 in §10.15 are distinct; no blanket rollback changes valid activation/trial.
- No state machine combines payment, entitlement, call and evidence into one catch-all status. `customer_order.payment_status` is payment-only; issuance state is separate; call and evidence states are separate.
- The deprecated action `DIRECT_AUTHORIZATION` as an action is never used; it is only a purpose. There is no eighth action.

### 7.4 S3 state machines

These are the S3 state machines authored in this slice. Every mutable transition is a conditional update with expected `revision`; a zero-row update performs one bounded reread and returns the current state or a stable conflict. Append-only media, artifact, evidence-event, admission and access-event facts have no state transition. No S3 state machine combines media validation, evidence solidification and sharing authorization into one catch-all status.

| Aggregate | Allowed states | Allowed transitions and guards | Owner |
| --- | --- | --- | --- |
| Recording | `DECLARED`, `UPLOADED`, `VALIDATED`, `QUARANTINED` | `DECLARED → UPLOADED` only through `evidence.record_upload_observed` when the controlled upload completes and the exact object/version/hash observation exists (a same-object/version/hash retry is idempotent, a changed identity conflicts and mutates nothing); `UPLOADED → VALIDATED` only after server rehash, MIME/size/duration/malware validation and exact declared/server fact comparison; `UPLOADED → QUARANTINED` on any mismatch, storage identity conflict, unknown storage outcome or failed malware check; a `DECLARED` row cannot proceed to validation/quarantine and returns `UPLOAD_OBSERVATION_REQUIRED`; `DECLARED → QUARANTINED` only when the upload expired or became permanently impossible, producing no successful media fact. Only `DECLARED`/`UPLOADED` may transition to `QUARANTINED`; `VALIDATED` and `QUARANTINED` are terminal. The owner locks the row and re-reads `upload_expires_at` with `clock_timestamp()`; a completion at/after expiry is refused/quarantined. A same-identity retry returns the first terminal outcome. After `VALIDATED`/`QUARANTINED`, a conflicting callback is refused with a stable recording conflict and one deterministic redacted conflict fact, changing no status/media/validation fact/revision; pre-terminal conflicting facts quarantine once without overwriting the first server facts. | evidence |
| Evidence record | `PENDING`, `SOLIDIFIED`, `QUARANTINED`, `FAILED` | `PENDING → SOLIDIFIED` only after the recording/media validation, exact subject/call/session/reservation linkage and frozen snapshot are valid and the one matching `EVIDENCE_CREATED` event is in the same transaction; `PENDING → QUARANTINED` when validation or identity linkage fails; `PENDING → FAILED` only for a terminal non-retryable evidence fact failure with no solid evidence. All three terminal states are absorbing. The evidence record and creation event insert atomically in either order; the deferred cycle triggers validate at commit. A later claim/account/membership/real-name change never changes any state or captured fact. | evidence |
| Evidence creation cycle | One matching `EVIDENCE_CREATED` event per evidence ID; other evidence events are append-only history | The record and creation event may be inserted in either order in one local transaction. At commit, `record.source_event_domain = event.event_domain = 'EVIDENCE'`, `record.source_event_id = event.event_id`, `event.evidence_id = record.id`, and exactly one creation event exists for the record. Missing, duplicate, wrong-type, wrong-owner or mismatched facts raise `EVIDENCE_CREATION_CYCLE_VIOLATION` and roll back the whole transaction. A failed transaction leaves no partial creation fact. Non-creation events never satisfy the creation requirement and are never updated. | evidence |
| Share grant | `ACTIVE`, `EXPIRED`, `REVOKED`, `LOCKED` | `ACTIVE → EXPIRED` at/after `expires_at`, persisting `EXPIRED` and incrementing `revocation_generation` once; `ACTIVE → REVOKED` on an authorized revoke, incrementing `revocation_generation`; `ACTIVE → LOCKED` on the approved verification-failure/lock policy without bumping generation or invalidating already-admitted sessions; `LOCKED → ACTIVE` only by code rotation, which replaces the code/salt, increments `code_generation` and `revocation_generation`, and appends `CODE_ROTATED`; `LOCKED → REVOKED`/`LOCKED → EXPIRED` through the existing owner revoke/expiry transitions, remaining terminal; no invented non-code unlock exists. `ACTIVE → ACTIVE` extension only before current expiry, increasing `expires_at` with revision +1, no generation change and no lengthening of an existing admission/session. `EXPIRED`/`REVOKED` are terminal. `max_admissions` reached blocks only new admissions and changes no status/generation. Every transition is a conditional revision update with an audit/access event; the same action retry resolves its deterministic §2.12 transition identity/fingerprint and returns the first recorded outcome before current status/revision checks; changed content conflicts; a stale loser rereads and returns the current state or a stable conflict. | sharing |
| Share admission and derived session | Admission is an immutable one-time fact; the playback session is derived, not a PostgreSQL state | A `NONE`/code/mobile/member verification success under the current grant locks and checks status/time/generation/authorization; an existing `admission_key` is resolved before the limit so it returns the original unexpired admission/session without a cap test, increment or second `ADMISSION_GRANTED`, and an expired session returns the original admission with no ticket. Only a new admission is limit-checked; it inserts with `admission_no = admissions_used + 1`, appends one `ADMISSION_GRANTED` event recording admission-time generations/expiry and the original `ORGANIZATION_MEMBER` recipient account/organization binding, increments `admissions_used`/revision once, commits, and only then derives the Redis ticket from `share_grant_id + admission_id`. Page open, prefetch and failed verification create no admission. After the limit, new admissions are denied; the last admitted session still plays/seeks/renews within its recorded admission/session/grant validity and current generation, and `LOCKED` still permits that existing replay/reconstruction while blocking a new admission. Once that session expires, a new admission is required and counts/rechecks again. A stale admission after revoke/rotation/expiry/generation mismatch is denied; an `ORGANIZATION_MEMBER` reconstruction or renewal also requires the same authenticated recipient and current active organization/membership/account. | sharing |
| Share access event | Append-only success/denied/failed/ignored outcomes | Each verification, admission, ticket issue/failure, playback/seek/renewal, extension, code rotation, revoke, expiry, lock or generic failure appends one immutable event with a deterministic domain/event identity. A same-semantic retry returns the first event and appends nothing. `ADMISSION_GRANTED` records the admission-time generation/expiry witness and the original `ORGANIZATION_MEMBER` recipient binding; ticket events are redacted and do not change admission count. Events never store a plaintext code, token, phone, object key/URL or media bytes. | sharing |

S3 transition and terminal rules:

- `VALIDATED` recording, `SOLIDIFIED`/`QUARANTINED`/`FAILED` evidence, `EXPIRED`/`REVOKED` shares, every admission and every access event are immutable for their created facts. A later status transition appends history/Outbox but never rewrites the prior fact. A post-terminal recording conflict appends one deterministic redacted conflict fact and never changes the recording or prior media fact.
- A recording/evidence storage failure or unknown result never fabricates successful media metadata or evidence; it remains non-solid, is quarantined or retried under the same identities, and is never converted into a positive evidence fact by retry.
- Revocation/rotation/expiry invalidates future access; it never deletes the evidence, admission history or access events. Already-issued short-lived media credentials are not recalled; their approved TTL defines the bounded stale-access window (Q22).
- Reaching the admission limit is not revocation. It blocks only new admissions and never bumps `revocation_generation`; the last admitted session keeps its valid session/grant window, and a same-key replay of that session is resolved before the limit. A grant extension never lengthens an admission or session already recorded.
- `NONE` verification is link possession, not identity. It is stored as `NOT_REQUIRED`, never `VERIFIED`, and never becomes an organization/member identity assertion.
- No state machine invents a support, refund, invoice, role-designer, AI, notary, online-extension workflow or permanent-object-URL state.

## 8. Sensitive-Field Inventory

### 8.1 Frozen planned sensitive-field inventory (28 rows)

The following block is verbatim from the accepted plan. It covers all 44 tables, including the S3 rows authored in §4.39-§4.46. The frozen inventory is structural coverage; the detailed S3 representation is in §8.6.

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

### 8.5 S2 sensitive-field detail

| Table.column(s) | Representation and key reference | Prohibited exposure | S2 control |
| --- | --- | --- | --- |
| catalog_order.catalog_product | No sensitive value | Product code is an internal catalog key | No sensitive column |
| catalog_order.catalog_version.snapshot / snapshot_sha256 | Canonical commercial snapshot and hash; no protected value | No raw party, provider or credential data | Redaction before hashing; publication immutability guard |
| catalog_order.catalog_offer | No sensitive value | Token is a business version token, not a credential | No sensitive column |
| catalog_order.catalog_benefit_line | No sensitive value | Quantities are commercial facts | No sensitive column |
| catalog_order.customer_order.snapshot / snapshot_sha256 | Canonical order snapshot and hash | No protected value; historical amounts only | Immutable snapshot guard; server-derived amount |
| catalog_order.payment_order.merchant_order_no / provider_transaction_id | Provider-scoped internal identifiers | Never client-visible public keys | Restricted role; no public projection |
| catalog_order.payment_event.payload_ciphertext / payload_sha256 | Encrypted provider payload plus hash; provider_event_id / transaction_id internal | No plaintext provider payload or event ID in logs | Envelope CHECK; append-only; restricted read |
| ivr.outbound_call_task.caller_number_hmac / callee_ciphertext / callee_masked | HMAC lookup, encrypted callee, masked callee | Full callee only in the authenticated IVR internal decision payload; never in task APIs/logs | Envelope/HMAC checks; guard trigger |
| ivr.inbound_session.caller_ciphertext / caller_number_hmac / caller_masked / access_number_masked | Ciphertext, HMAC, masked | No full caller or access number in public keys/logs | Envelope/HMAC checks; immutable origin guard |
| ivr.call_record.caller_ciphertext / caller_number_hmac / caller_masked / callee_ciphertext / callee_masked / access_number_masked | Ciphertext, HMAC, masked | Party values are never public identifiers | Immutable party guard; envelope/HMAC checks |
| ivr.call_leg | No party value | Links to the call record only | No sensitive column |
| ivr.call_event.payload_ciphertext / payload_redacted / payload_sha256 | Encrypted raw payload, redacted view and hash | Raw event payload never logged or returned | Envelope/redaction checks; append event identity |
| ivr.ivr_decision.response_snapshot / response_payload_ciphertext / response_sha256 | Typed redacted snapshot plus encrypted internal payload; task callee only under `task.calleeNumber` for `DIAL_TASK_CALLEE`; confirmed callee only under `authorization.calleeNumber` for successful `DIRECT_AUTHORIZATION`; key_version | Redacted snapshot/log/audit/customer responses carry only masked values; replay decrypts the same payload; direct success never populates task fields | Purpose/action/nullability CHECK; append-only; replay decryption equality; field-level tests |
| entitlement_metering.trial_policy_version.snapshot / snapshot_sha256 | Canonical policy snapshot and hash | Policy facts only | Redaction before hashing |
| entitlement_metering.entitlement_grant.snapshot / snapshot_sha256 | Canonical grant snapshot and hash | Historical grant facts only | Immutable grant |
| entitlement_metering.trial_grant | No protected value | Source action identity/fingerprint are internal | Append-only |
| entitlement_metering.entitlement_balance | No protected value | Numeric balance only | Row lock and CHECK |
| entitlement_metering.entitlement_issuance | No protected value | Internal state and error code | Conditional revision |
| entitlement_metering.usage_reservation | No party/media value | Internal call/balance link | Immutable authorization fields |
| entitlement_metering.usage_fact.snapshot / snapshot_sha256 | Canonical usage snapshot and hash | Usage metadata only | Append-only; no raw media |
| activation_consent.consent_record.snapshot / snapshot_sha256 / request_fingerprint | Canonical redacted consent snapshot, hash and semantic fingerprint | No plaintext protected value; source_ref is opaque | Append-only; explicit action; event unique |
| activation_consent.service_activation | No sensitive column | Current status and revision only | Row lock; transition ledger |
| activation_consent.service_activation_transition | No sensitive column | Actor IDs and consent reference are internal | Append-only; unique action/status events |

The full callee is never written to `response_snapshot`, logs, audit, Outbox or a customer response. The redacted snapshot and logs use `calleeMasked` and task/decision IDs only. A direct authorization's redacted projection uses a masked callee; `authorization.calleeNumber` exists only in the encrypted payload. The protected-key denylist in §10.7 applies to every S2 platform/audit/Outbox snapshot.

### 8.6 S3 sensitive-field detail

| Table.column(s) | Representation and key reference | Prohibited exposure | S3 control |
| --- | --- | --- | --- |
| evidence.media_object.bucket / object_key / object_version_id / crypto_key_ref | Internal private-storage reference | Never a public URL, response field, share URL, log field or Outbox payload | Internal evidence/media-gateway only; no public projection |
| evidence.media_object.sha256 / size_bytes / mime_type | Server-computed validation metadata | Hash prefixes only if the approved log policy permits | One-way/validation only; immutable |
| evidence.recording.declared_sha256 / validated_sha256 | Hash-only declaration and server result | Declared hash never overrides validation; no media bytes | Hash checks; server recomputation |
| evidence.evidence_record.identity_at_capture / consent_version / snapshot / snapshot_sha256 | Immutable redacted identity reference, consent version and canonical evidence snapshot | No raw number, live identity, object key/URL, media bytes or share token | Redaction before hashing; immutable capture guard |
| evidence.evidence_artifact | Internal parent/media IDs and artifact role | No object identity in a public response | Internal manifest only |
| evidence.evidence_event.payload_redacted / payload_sha256 | Redacted metadata and hash | Never media bytes or raw party identifiers | Append-only; redaction check |
| sharing.evidence_share_grant.capability_token_hash | One-way SHA-256 digest of at least 128 random capability bits | Plaintext token only in the creation response/URL; never stored, logged or evented | One-way digest; unique index |
| sharing.evidence_share_grant.code_salt / code_hash / code_generation | Per-grant/per-generation random salt and salted slow-KDF digest | No plaintext code, no reversible encoding, no ordinary log; bound to grant and generation | KDF/salt; rotation increments generation; no defaults |
| sharing.evidence_share_grant.content_snapshot / content_sha256 | Frozen content scope plus typed creation digest/fingerprint, designated-mobile HMAC/key version and verification mode | Never object key/URL, raw designated phone, OTP, raw creation request ID, unshared evidence or public evidence ID | Hashes/HMAC and key version only; canonical hash; immutable scope |
| sharing.share_admission.admission_key | Opaque server-derived admission key/digest | Never raw phone, code, token, IP or verification input | Opaque key; unique per grant |
| sharing.share_access_event.risk_digest / payload_redacted / payload_sha256 | Hash and redacted metadata, including the admission-time generation/expiry and original organization-recipient witness | No raw IP, phone, code, token, object key/URL, full URL or media bytes | Redaction before hashing; append-only; deterministic identity |

The S3 tables never store a plaintext capability token, extraction code, designated phone, OTP, object URL, bucket/key/version in a public column, or media bytes. The capability token is returned only once at grant creation (or reissued for an already-authorized admission) and only the digest is durable. The media gateway receives object identity through the authenticated internal evidence/media interface, never through the public share page. Ticket/access events contain only internal IDs, generations, expiry and result/reason codes; cache values are short-lived and non-authoritative. The §10.7 denylist applies to every S3 Outbox/audit payload.

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

### 9.0.1 S2 lock and transaction integration contract

S2 consumes the exact global acquisition order below. It is one total order; no transaction takes an earlier key after a later key:

1. canonical-caller advisory `ivr.caller:<canonical_caller_hmac>` (task create/replace/cancel/claim, initial resolve, post-activation confirmation, direct authorization, usage settlement and reconciliation; reconciliation obtains rank 1 only through the `ivr.lock_reconciliation_caller` collaborator);
2. needed canonical-number advisory key(s), sorted ascending by HMAC when more than one is needed;
3. subject lifecycle advisory `subject_number.subject:<subject_id>`;
4. `ivr.inbound_session` and current `ivr.call_record` row locks in that order (IVR-owned), then the existing `activation_consent.service_activation` row lock at the same rank, taken through the activation-consent-owned `lock_service_activation` collaborator for foreign callers or directly by the activation-consent client-activation service as the owning module; a missing activation row is serialized by the already-held subject lifecycle advisory (3) and the collaborator locks nothing. A foreign role never takes the activation row lock directly and never acquires rank 4 after rank 10;
5. subject-owner read; S1 owns the `business_subject` row lock, while S2 uses a non-locking read under the subject lifecycle advisory;
6. caller-binding and risk rows; S2 reads under the canonical-number and subject-lifecycle locks, or calls a `subject_number`-owned same-transaction collaborator if a row lock is genuinely required;
7. organization lifecycle advisory, then the owner's organization row lock in the owning module;
8. membership lifecycle advisory keyed by `organization_access.membership:<organization_id>:<account_id>` with canonical lowercase UUID text, then the owner's membership row lock in the owning module;
9. account lifecycle advisory, then the owner's account row lock in the owning module;
10. `platform.idempotency_record` row for S2 confirmation/purchase/idempotent action entries. This is the explicit S2 placement: the row is acquired after locks 1-9 and **before** any tentative task claim or reservation. It is compatible with S1's accepted begin-before-effects/no-savepoint contract because S1's `begin` also precedes its side effects; no S1 or S2 transaction acquires a task row or reservation before this row, and the caller/session/access locks serialize the flow. Any entry that uses `IVR_CONFIRM` or `PURCHASE` follows this placement; a transaction must never take a task/reservation key and only then acquire the idempotency row. The rank-4 activation row lock is taken before this row; `record_activation_confirmation`, `record_client_activation` and `append_purchase_consent` reuse the held activation lock and never first acquire rank 4 after this row;
11. candidate task row and its conditional claim; reservation/balance suborder follows;
12. reservation/balance suborder: balance rows in ascending UUID, then the `usage_reservation` insert/lock, then `usage_fact` on settlement; no earlier key is taken after entering this suborder; reconciliation calls `ivr.lock_reconciliation_caller` for rank 1, discovers `balance_id` with a non-locking read, then uses this exact suborder in every locked phase and holds no PostgreSQL or advisory lock across the authoritative IVR query (§9.16);
13. employee row plus the administrator advisory lock (operations actions only; operations actions do not take task/reservation keys). A non-operations-iam caller obtains these at rank 13 through the operations-iam-owned `operations-iam.lock_employee_administrator` lock-only collaborator before its owner-local activation write; the collaborator writes nothing and `activation_consent_role` receives no foreign table or row-lock privilege;
14. Outbox insert;
15. audit insert.

- `activation-consent.lock_service_activation` is a minimal activation-consent-owned, same-connection `Propagation.MANDATORY` lock-only collaborator invoked at rank 4 by foreign callers (IVR confirmation and order creation) before ranks 7/8/9/10; the activation-consent client-activation service takes the same rank-4 row lock directly under its owner role. Under `activation_consent_role` the collaborator locks an existing `service_activation` row for the subject and returns its current facts; it creates, updates and appends nothing. A missing row is serialized by the already-held subject lifecycle advisory (3), so the collaborator locks nothing and the later owner service may insert the first row under that advisory. Later owner services reuse the held lock; re-entrant already-held locks are not fresh inversions. IVR locks only its own session/current-call rows; foreign organization/membership/account lifecycle rows use their advisory locks plus exact `SELECT` re-reads, or a named owner collaborator. Every entry that locks an activation row (IVR confirmation, client activation, order creation and operations lifecycle) composes in this total order; read-only initial resolve and direct authorization use exact `SELECT` re-reads under the subject advisory and take no activation row lock.
- `operations-iam.lock_employee_administrator` is a minimal operations-iam-owned, same-connection `Propagation.MANDATORY` lock-only collaborator invoked at rank 13 by the activation-consent operations lifecycle service before its owner-local activation write. Under `operations_iam_role` it takes `operations_iam.admin_mutation` and locks the authenticated employee row, returns the current employee/administrator facts and creates, updates and appends nothing. `activation_consent_role` receives no `operations_iam` schema USAGE, table privilege or row-lock privilege; this collaborator is the only cross-module operations lock path.
- `ivr.lock_reconciliation_caller` is a minimal IVR-owned, same-connection `Propagation.MANDATORY` lock-only collaborator invoked by the entitlement-owner reconciliation in phases A and C. Given the immutable `callId`, under `ivr_role` it reads the existing `ivr.call_record.caller_number_hmac` and acquires the rank-1 canonical-caller advisory lock `ivr.caller:<canonical_caller_hmac>`; it takes no row lock, writes, appends and enqueues nothing, and returns only a stable success, stable `CALL_UNKNOWN` not-found outcome or stable `DATA_INTEGRITY` outcome. The entitlement owner then discovers and locks only its own balance/reservation rows in the existing item-12 order. `entitlement_metering_role` receives no `ivr` schema USAGE, table privilege or row-lock privilege; this collaborator is the only cross-module reconciliation caller-lock path, and no PostgreSQL or advisory lock is held across the phase-B IVR network query.
- Standalone owner publication transactions additionally take the owner-scoped advisory key `catalog_order.publication:<product_id>` or `entitlement_metering.trial_policy:FIRST-TRIAL` before their owner row updates. These keys are not combined with the IVR call-path order and are never taken while holding a task/reservation key; they are transaction-control serialization, not a new table or product rule.
- `ivr.select_call_action` derives the entry mode and purpose from the authenticated endpoint/session, never from a caller-supplied field. It is one application service used by `INITIAL_RESOLVE` and `POST_ACTIVATION`; direct authorization reuses its ordered predicates. `ivr.record_activation_next_action` is a thin IVR entry wrapper that delegates and persists no decision of its own.
- All task/claim/current-call/reservation/frozen-authorization effects are one local transaction through the published owner services. `activation-consent` and `entitlement-metering` never write `ivr.ivr_decision`; `ivr` never writes consent, activation, trial, grant, balance, reservation or usage tables directly.
- A cross-owner row lock is taken by the owning module's same-transaction application-service collaborator under the owner role on the same connection. A cross-module role never takes `FOR UPDATE`/`FOR SHARE` on another module's table and never receives `UPDATE` on it. Cross-owner reads use the exact §10.12 `SELECT` columns under the corresponding advisory lock and re-read after acquiring it. The runtime roles are NOLOGIN; how an application connection is provisioned, authenticated, or switched to an owner/service identity (a LOGIN role, `SET ROLE`, connection-pool role mapping or equivalent) is Q17/Q23 deployment-security input and is **not** specified or claimed here. OQ-2 remains deferred to Platform/Database/Security with the executable role-provisioning/privilege tests; the same-connection same-transaction collaborator requirement is the contract.
- S1's request-ID unique-loser path remains whole-transaction rollback plus a bounded restart; S1 has no savepoint. S2's one same-connection savepoint is established **after** accepted activation/trial and **before** the shared selector performs any tentative task claim, reservation or balance debit. A category-2 normal domain denial executes `ROLLBACK TO SAVEPOINT`, then freezes exactly one no-dial decision/completion and commits the retained activation/trial; the no-dial decision/completion are written after the rollback so they survive. A category-3 SQL/lock/I/O/Outbox exception never rolls back to the savepoint and is never swallowed: the whole transaction rolls back. For an initial-resolve or direct-authorization attempt there are no accepted activation/trial facts to preserve, but the savepoint is placed before the tentative claim/reservation in the same attempt and is used only for the same category-2 typed denial; a technical error still rolls back the whole transaction. The savepoint never protects an S1 claim path and never commits independently.
- Replay and immutable identity checks precede expiry cleanup and every accepted effect. `expires_at > clock_timestamp()` is evaluated after the claim lock, not from `now()` or a request timestamp. No lock is held across a network call.

### 9.0.2 S3 lock, transaction and external-boundary integration

S3 preserves the S1/S2 global order in §9.0.1. It takes no canonical-caller, canonical-number, subject-lifecycle, IVR session/call/activation, idempotency, task, balance/reservation, employee/admin or foreign row lock. Its cross-module reads are exact `SELECT`s under the corresponding S1/S2 advisory lock where a lifecycle predicate applies, and it never takes a cross-owner `FOR SHARE`/`FOR UPDATE`.

S3 owner-local acquisition order is explicit and total within a transaction:

1. For a share grant whose creator acts in an organization workspace, the sharing service acquires the S1 ranks 7 (`organization_access.organization:<organization_id>`), 8 (`organization_access.membership:<organization_id>:<account_id>` using canonical lowercase UUID pair text, including the absent/create/re-grant case) and 9 (`customer_iam.account:<account_id>`) advisory locks in that order, then re-reads the exact §10.21 organization/membership/account columns after the lock. A personal share takes only rank 9. These advisory locks are the S1 transaction-control locks; no foreign row lock is taken.
2. For `ORGANIZATION_MEMBER` admission, ticket reconstruction, ticket renewal or media playback/seek/renewal, the sharing service re-authenticates the recipient bound in the original `ADMISSION_GRANTED` witness and takes the same ranks 7, 8 and 9 before locking or re-reading the share grant, because recipient and tenant membership are current authorization predicates. A different authenticated account/tenant, a revoked/suspended membership or a disabled account fails closed. A `NONE`, `EXTRACTION_CODE` or `DESIGNATED_MOBILE` path takes no S1 lifecycle lock; `NONE` remains bearer-only and is never recorded as a verified identity.
3. The sharing owner row order is `sharing.evidence_share_grant` (`FOR UPDATE` for admission/transition), then the `sharing.share_admission` insert, then the `sharing.share_access_event` insert, then the conditional grant count/revision update. No share event insert is followed by a grant lock in the same transaction.
4. The evidence owner row order is `evidence.recording` (`FOR UPDATE` for completion/validation), then the `evidence.media_object` insert, then the `evidence.evidence_record` insert/`FOR UPDATE`, then the `evidence.evidence_artifact` and `evidence.evidence_event` inserts. The evidence creation cycle may insert the record and creation event in either order; the two deferred triggers run only at commit, after both row sets exist.
5. Outbox and audit appends use the existing `platform.outbox_append`/`audit.append_fact` functions at the S1/S2 ranks 14/15 after all owner writes. No S3 service takes a foreign lifecycle key after an S3 owner lock; current S3 paths either take ranks 7-9 before the owner lock or take no earlier key at all. No S3 transaction takes rank 10-12 because it has no platform idempotency scope, task claim or balance debit.

- PostgreSQL locks and the admitting/authorizing transaction are released or committed before Redis ticket derivation and before any media-gateway I/O. A reconstruction re-reads the durable admission/witness and closes its PostgreSQL transaction before issuing a replacement ticket. If revoke/expiry commits before the reconstruction read, it governs and the reconstruction is denied; if a valid reconstruction/ticket already completed before a later revoke, the already-issued short-lived media credential is bounded only by the approved Q22 credential TTL/revocation window, and no buffered bytes are claimed recalled.

- `evidence.complete_recording` and `evidence.create_and_solidify` run one local transaction per persistence attempt. Object-storage HEAD/read, hash computation, MIME sniffing, duration probing and malware scanning are performed by the media adapter/worker **before** the PostgreSQL row lock; their validated result is then submitted to the evidence owner service. No PostgreSQL lock is held over a COS, Redis or media-worker network call.
- A recording/evidence unique loser or a deferred-cycle violation aborts the whole S3 transaction. The service does not read the aborted transaction; it starts a bounded retry, reacquires the owner row lock, re-reads current status and replay identity, and returns the first committed outcome or a stable conflict. S3 uses no savepoint and does not alter S1's whole-transaction restart or S2's phase-B savepoint.
- The sharing admission transaction commits the admission, its `ADMISSION_GRANTED` access event and the grant count/revision together. Only after that commit does the sharing service derive and write the Redis ticket. A ticket write/append failure is returned fail-closed; it never rolls back or double-increments the committed admission. A lost response/cache or Redis failure triggers a PostgreSQL re-read and reconstruction, not a second admission.
- Every S3 conditional update uses `AND revision = :expected_revision` and sets `revision = revision + 1`. A zero-row update performs one bounded reread and returns the current state or a stable conflict. Volatile expiry checks use `clock_timestamp()` after the owner lock, never request time.
- No S1/S2 table gains an S3 FK, trigger or write path. S1/S2 migrations V001-V007 and the five function-wave rows are unchanged. S3 consumes the existing `evidence`-parent call/session/reservation IDs read-only; it does not mutate IVR, entitlement, subject, account or organization facts.

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
2. Lock order: read the binding's immutable identity; a zero-row initial binding read returns `CLAIM_BINDING_NOT_FOUND` immediately with no claim row, no owner change and no further lock or write. Otherwise acquire the canonical-number advisory lock for its `number_hmac`, acquire the subject lifecycle advisory lock `subject_number.subject:<subject_id>`, and lock the `business_subject` row `FOR UPDATE`; a zero-row subject lock returns `CLAIM_SUBJECT_NOT_FOUND` with no claim row. Then lock the `caller_number_binding` row `FOR SHARE` and the `subject_risk_state` rows in ascending UUID. When `organization_id` is supplied, acquire the organization lifecycle advisory lock and the membership lifecycle advisory lock for the logical pair `organization_access.membership:<organization_id>:<account_id>` (the pair comes from the request, not from a discovered membership ID); a personal claim has no pair and takes neither lock. Then acquire the account lifecycle advisory lock, in the global order of §9.0. Under each lock, `SELECT` the exact §10.8 columns and re-read every predicate; no pre-lock value is reused. A zero-row organization read returns `CLAIM_ORGANIZATION_NOT_FOUND` and a zero-row account read returns `CLAIM_ACCOUNT_NOT_FOUND`, each with no claim row.
3. **Current predicates (also applied to replay and duplicate).** Reject with the stable code when: the binding is missing (`CLAIM_BINDING_NOT_FOUND`); `binding.subject_id <> target subject_id` (`CLAIM_SUBJECT_BINDING_MISMATCH`); the binding is not `ACTIVE` or `verified_at` is null (`CLAIM_BINDING_NOT_ACTIVE`); a supplied canonical number does not HMAC to `binding.number_hmac` (`CLAIM_NUMBER_MISMATCH`); the authenticated account is not `ACTIVE` (`CLAIM_ACCOUNT_INACTIVE`); an organization context has a non-active organization (`CLAIM_ORGANIZATION_INACTIVE`) or no active membership for `(organization_id, account_id)` (`CLAIM_MEMBERSHIP_INACTIVE`); when replaying or duplicating an existing accepted organization-scoped claim, the current approved-access conjunction in step 7 must also hold (`CLAIM_ACCESS_NOT_APPROVED`); a fresh organization-scoped request that passes the organization/membership predicates proceeds to the step 7 policy boundary. A stored `ACCEPTED` claim is never returned when any current predicate fails. Existence is validated before any outcome row: an unknown target subject, authenticated account or supplied organization returns `CLAIM_SUBJECT_NOT_FOUND`, `CLAIM_ACCOUNT_NOT_FOUND` or `CLAIM_ORGANIZATION_NOT_FOUND` with no claim row, and a raw foreign-key error is never the protocol outcome. For a fresh `request_id`, a predicate failure appends exactly one `REJECTED` row only when the target subject, authenticated account and supplied organization all exist and the binding and verification reference are resolvable; an existing inactive parent may yield a `REJECTED` row only in that case. A missing binding (`CLAIM_BINDING_NOT_FOUND`) or invalid reference (`CLAIM_VERIFICATION_REF_INVALID`) writes no claim row. For an existing `request_id` the service returns the stable error and appends only an audit fact, because the claim row is append-only; the replay identity comparison in step 5 governs any mismatch and never discloses the stored outcome. A fresh predicate rejection written before step 4's risk read stores the database claim-decision time in `risk_checked_at` and a typed `riskEvaluation = NOT_REQUIRED` in the audit `after_snapshot`; it must not claim a risk lookup or result. A path that evaluates risk stores the actual current risk-decision time.
4. **Risk first.** Read all `subject_risk_state` rows for the locked binding. If any row is `OPEN`, return `CLAIM_RISK_BLOCKED` before replay or same-account-duplicate success. For a fresh `request_id` append exactly one `REJECTED` row with `risk_checked_at`; for an existing `request_id` return the blocked outcome without mutating the row and append only an audit fact. `OPEN` risk is not converted into a dialing ban. For this risk-evaluating path `risk_checked_at` is the actual current risk-decision time.
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
3. Create: acquire the subject lifecycle advisory lock and the subject row `FOR SHARE` (the binding and subject are owned by `subject_number`). A zero-row subject read returns stable `BINDING_SUBJECT_NOT_FOUND` with no binding write and no owner or access change. Otherwise verify the subject owner/access has already been authorized, then insert the new `ACTIVE` binding. The partial unique active-number index and unique `(subject_id, number_hmac)` are backstops. A unique loser aborts the whole transaction; the service starts a bounded retry from the beginning, reacquiring the canonical-number and subject locks and re-reading, and maps the current winner deterministically: same-subject identical `ACTIVE` is an idempotent success; same-subject `DISABLED` is `BINDING_DISABLED_CONFLICT`; same-subject `RETIRED` is `BINDING_RETIRED_CONFLICT`; another subject's `ACTIVE` row is `BINDING_ACTIVE_CONFLICT`; any other state is a stable current-state conflict. It never exposes a raw unique error or returns a non-active row as success.
4. Replace: after acquiring both number locks in sorted order, acquire the subject lifecycle advisory lock and the subject row `FOR SHARE`; a zero-row subject read returns stable `BINDING_SUBJECT_NOT_FOUND` with no binding, owner or access change. Then lock the old and destination binding rows in ascending UUID (`FOR UPDATE`), and re-read them. The old binding must be `ACTIVE` and belong to the requested subject; otherwise return the current state or a stable conflict with no change. Validate the destination number as in step 2. Conditionally retire the old binding (`ACTIVE → RETIRED`, revision + 1) and insert a new binding for the new number, re-enable the same-subject `DISABLED` destination row, or reuse the same-subject `ACTIVE` destination row, in one transaction. If the new number belongs to another subject, if the destination row is `RETIRED`, or if any conditional update/unique insert loses, the entire transaction rolls back and the old binding remains `ACTIVE`; a bounded retry from the beginning reacquires both canonical-number locks in sorted order and re-reads, then maps the loser to a stable current-state/conflict response. A same-number replacement is `BINDING_SAME_NUMBER` before any row change.
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

### 9.11 T-S2-01 Activation confirmation with first trial and next action

This is one IVR-owned local transaction. The IVR endpoint authenticates the service and protocol version and validates `callId`, `sessionId`, node and immutable caller/subject/access-number identity **before** reading or disclosing any stored result.

1. Authenticate the IVR service/protocol and immutable call/session identity. Validate the confirmation `eventId` is nonblank and 1-128 characters before `idempotency_begin` or any domain effect; a blank or over-128 ID is refused with the stable protocol argument error `IVR_ARGUMENT_INVALID` and writes nothing (the general 1-512 platform key capacity is unchanged). Acquire the canonical-caller lock; acquire the subject lifecycle advisory lock when a subject is resolved. Lock only the IVR-owned `inbound_session` and current `call_record`; then call `activation-consent.lock_service_activation` at rank 4 to take the existing `service_activation` row lock through the owner role (a missing row is serialized by the subject advisory lock and locks nothing). Re-read the locked rows after acquisition. The IVR never takes a cross-owner `FOR UPDATE`. Hold the current task-backed access rows through the §9.0.1 organization, membership and owner/creator account advisory locks plus exact `SELECT` re-reads when the call is task-backed; only the owning module takes its own row lock. Do the same on a best-effort basis for the no-task path only for predicates that exist.
2. Call `platform.idempotency_begin('IVR_CONFIRM', eventId, canonicalConfirmationFingerprint)` after the caller/session/access locks and **before** any task/reservation (explicit S2 placement in §9.0.1 item 10). The canonical confirmation fingerprint is exactly §10.17: it includes `purpose`, `protocolVersion`, `callId`, `sessionId`, the exact immutable caller HMAC and `accessNumberHmac` (the ephemeral server-computed HMAC of the authenticated canonical normalized access number per §2.5; never persisted or emitted), plus subject/confirmed-subject/binding identity, `nodeId`, prompt/consent fields and confirmation key/content, and excludes `eventId`. A regenerated event ID with identical semantic fields matches and replays the first result; a changed session/protocol/binding/access number/callee/prompt/key/content or a binding replacement/revocation/rebinding is a stable conflict before any frozen decision or effect. A different canonical access number with an identical display mask conflicts because the fingerprint uses `accessNumberHmac`, not `accessNumberMasked`; missing or invalid HMAC input fails closed before any effect.
   - `REPLAY`: validate the stored caller/call identity and return the stored first result; no consent, trial, claim, reservation or Outbox effect.
   - `CONFLICT`: changed input for the same event ID; reject atomically with no side effect.
   - Only `NEW` continues.
3. Read the existing `(callId, ACTIVATION_NEXT_ACTION)` decision **before** the `SUSPENDED` check and before any consent/trial/claim/reservation effect.
   - Existing decision and matching stored canonical fingerprint: complete this event's `NEW` row with the existing decision ID and frozen result, enqueue no new side effect and commit. A replay never renews authorization.
   - Existing decision and different fingerprint: roll back the transaction (discarding the `NEW` row), return a stable semantic conflict and write no accepted effect.
   - `CONFIRMED` session without a decision: fail closed as `DATA_INTEGRITY`, change no state.
4. A fresh confirmation requires the original `(callId, INBOUND_RESOLVE)` decision to be `GUIDE_ACTIVATION`; any other action returns `REJECT`/`CONFIRMATION_NOT_ALLOWED` with no consent or activation effect. This is the IVR confirmation chain; the separate direct-authorization chain is §9.14.
5. **Phase A (pre-activation).** Apply the identity, fresh-origin and other-active/`UNKNOWN`-call guard (excluding the current `callId`) and the fresh `SUSPENDED` gate. For a fresh `SUSPENDED` subject, fail closed: write no accepted consent, transition, trial/grant/balance, reservation or success Outbox; persist exactly one rejected `ACTIVATION_NEXT_ACTION` decision with action `REJECT_SUSPENDED`, source domain `IVR_CONFIRM`; optionally append one audit fact; mark the inbound session `REJECTED` only when it is not already `CONFIRMED`; call `platform.idempotency_complete` with the rejected decision ID; commit. For an identity failure, invalid origin or an already-inconsistent current `UNKNOWN` attempt, create no accepted effect and no unauthorized persistent write, discard any `NEW` row and return the stable refusal. The current `UNKNOWN` call, an already-`CONFIRMED` session and any unrelated competing call are never destructively changed.
6. **Phase B (accepted activation/consent).** Call `activation-consent.record_activation_confirmation` with `Propagation.MANDATORY`. It validates the current prompt and consent policy-version/key/content before appending anything. Invalid/stale content returns `REJECT`/`CONSENT_REQUIRED` with no accepted write; the wrapper does not reimplement the predicate. On the accepted branch it appends the `consent_record` with `event_domain = 'IVR_CONFIRM'`, `event_id` equal to the already validated nonblank 1-128 confirmation `eventId`, `source_ref = 'ivr-session:' || lower(session_id)` (the 48-character deterministic value, independent of the transport event ID) and the immutable §10.17 `SERVICE_ACTIVATION` fingerprint; a same-semantic retry with a new transport event ID returns the first decision and consent before any append and never rewrites the first consent `event_id` or adds a consent. It creates or updates `service_activation` and appends exactly one transition with `to_revision = from_revision + 1`; an already-`ACTIVE` row keeps its status and appends no duplicate transition, and the confirmation anchor is the new transition ID or the current latest transition ID. It reuses the rank-4 activation row lock taken at step 1 (or inserts the first row under the subject advisory when none existed) and never acquires rank 4 after the idempotency row. It returns `consentRecordId`, `confirmationAnchorId` and `activationStatus`.
7. Call `entitlement-metering.issueFirstTrial` with `Propagation.MANDATORY`. The V006 function performs the sole authoritative enabled/published/expected-version policy read and retains the selected immutable content through the grant/Outbox; the wrapper lookup is preliminary only. Replay and already-granted return the immutable prior trial result before any new policy selection, and there is no second enabled recheck. It alone writes `trial_grant`, `entitlement_grant`, `entitlement_balance` and Outbox, and returns `GRANTED`, `REPLAY` or `ALREADY_GRANTED`. An existing trial is never reissued; a changed-fingerprint conflict or an inconsistent trial row fails closed.
8. **Establish the same-connection attempt savepoint now**, after the accepted activation/trial writes and **before** the shared selector can perform any tentative claim, reservation or balance debit. No claim/reservation may exist in the transaction at this point.
9. Call `ivr.select_call_action` with entry mode `POST_ACTIVATION` and the validated approved allocation-policy input (§10.18). It re-applies the shared ordered predicates: the current-call-excluded active/`UNKNOWN` guard, current activation/consent, caller/subject/binding and owner-workspace access including organization/membership/owner/creator account lifecycle, the bounded `PENDING`/revision/`clock_timestamp()` task predicate and the single-eligible-balance/positive-entitlement predicate. It persists exactly one `ACTIVATION_NEXT_ACTION` decision:
   - `DIAL_TASK_CALLEE` only when every task and entitlement predicate passes: the conditional claim atomically sets `outbound_call_task.claimed_call_id = call_record.call_id` and assigns the current pre-savepoint call's `task_id` from NULL to that task's `id`, requires reciprocal equality, rejects any non-null/different task, arbitrary relink, task switch or second call, and then takes one reservation from the selected balance and freezes the typed response;
   - `GUIDE_PURCHASE` when `NO_AVAILABLE_ENTITLEMENT` (no positive available entitlement), with no claim or reservation;
   - `ENTER_CALLEE` only after a positive no-`PENDING`/`CLAIMED`-task check and a positive selected balance;
   - `REJECT` for revoked access/binding/integrity or a policy/integrity allocation denial, `REJECT_CALL_IN_PROGRESS` for a competing active call, and `REJECT_SUSPENDED` only when the phase-A gate applies.
10. The shared selector writes the `DIAL_TASK_CALLEE` decision only after its tentative claim/link/reservation branch succeeds; it writes no dial decision while the branch can still be rolled back. If a phase-B task-access, organization/membership/account, binding, entitlement, allocation or active-call condition blocks dialing, execute `ROLLBACK TO SAVEPOINT` and then commit the category-2 result: valid activation/trial retained, session `CONFIRMED`, exactly one no-dial decision and its completion, no claim, no call `task_id` assignment and no reservation. The pre-savepoint call survives with `task_id = NULL`; a later attempt within the task TTL may claim and link the same task again. A zero-row conditional claim/link or an expected reservation denial follows the same path with one bounded reread and a deterministic non-dial result; never a raw constraint error and never an arbitrary second task. The no-dial decision/completion are written after the rollback so they survive.
11. Call `platform.idempotency_complete(recordId, expectedRevision, 'ACTIVATION_CONFIRMATION', nextActionDecisionId, redactedResultSnapshot, resultSha256)` exactly once. The decision ID is the stable non-null first result for every branch, including already `ACTIVE` and call-rejected outcomes; the rejected decision ID is the first result for a suspended fresh rejection. Enqueue Outbox/audit for activation facts and, when claimed, task/call/reservation facts; commit.
12. A claim cannot commit without its reservation and reciprocal task/call equality. A technical failure in either phase (SQL error, lock/advisory timeout, storage I/O, decision/completion/Outbox failure) is category 3: it does **not** roll back to the attempt savepoint; it rolls back the `NEW` row, completion, consent, activation, transition, entitlement, tentative claim/link, call `task_id` assignment, reservation, decision and Outbox, commits no decision and returns a bounded retryable error. A database exception is never swallowed and a rollback-only transaction is never used to commit.
13. Serial-order outcomes are exact. If an operations suspension or an access revocation commits **before** the confirmation acquires the relevant lifecycle/access locks, the fresh path observes the suspended/revoked state and returns the category-1 suspended refusal or the category-2 no-dial access refusal; when valid activation/trial already committed in this transaction, they remain committed with the session `CONFIRMED` and no claim/reservation. If the confirmation/claim commits **first** while every predicate passed, a frozen `DIAL_TASK_CALLEE` with its claim and reservation may survive a later revocation; the later revocation governs only later calls. There is no post-commit access re-read, decision reversal or invented rejection. A replay after suspension with a `CONFIRMED` session returns the original decision and never changes `CONFIRMED → REJECTED`; a fresh suspended confirmation on a never-confirmed session may be rejected under the round-6 write-set assertion in §13.7.

### 9.12 T-S2-02 Task create, replace and cancel

1. Resolve the authenticated account/workspace, `callerBindingId` and client idempotency key. Acquire the canonical-caller lock. Verify the binding belongs to the subject and is usable by the workspace, the creating account is `ACTIVE`, and for an organization workspace the organization is `ACTIVE` and the creator membership is `ACTIVE` with current task access; verify no other `ACTIVE`/`UNKNOWN` call exists for that canonical caller. These lifecycle rows are read under their exact advisory locks and re-read after acquisition; no pre-lock value is reused.
2. **Replay first.** If `(created_by_account_id, idempotency_key)` exists, same `request_fingerprint` returns the existing task without TTL refresh or housekeeping; a changed fingerprint returns a stable conflict. This keyed replay precedes expiry housekeeping and every accepted task effect.
3. Under the lock, expire stale `PENDING` tasks using `clock_timestamp()` (`now >= expires_at` becomes `EXPIRED` with `terminal_at`); this housekeeping never refreshes a live row and runs only when no keyed replay returned.
4. If an identical caller/callee/business/workspace `PENDING` task exists (after housekeeping), return it without refreshing TTL. A task frozen to another workspace/creator is not returned as success and never discloses its callee.
5. Create `PENDING` with server `ttl_seconds` (default 30, configurable) and `expires_at = created_at + ttl_seconds`. A create never revives a terminal task.
6. Replace is allowed only for the current workspace's and current account's `PENDING` task after rechecking the current account/organization/membership lifecycle under their locks: mark it `REPLACED` with `terminal_at` and insert the new task in the same transaction. A `CLAIMED` task cannot be replaced or cancelled.
7. Cancel is allowed only for the current workspace's and current account's `PENDING` task after the same lifecycle recheck. Claim, replace and cancel compete on the same caller lock and conditional status update; exactly one wins; losers return the current state or a stable conflict. Enqueue Outbox/audit; commit.

### 9.13 T-S2-03 IVR initial resolve

1. Authenticate the IVR service/protocol and validate immutable `callId`/node/caller identity; normalize the trusted caller and acquire the canonical-caller lock. If the caller is missing/untrusted, return `REJECT`/`CALLER_UNTRUSTED` with no task claim/reservation and no accepted activation/trial.
2. Read any existing `(callId, INBOUND_RESOLVE)` decision **before** task-expiry housekeeping, subject find-or-create, session/call creation or any other mutation. The §10.17 initial-resolve fingerprint includes `purpose`, `protocolVersion`, `callId`, `sessionId`, the immutable caller HMAC, `accessNumberHmac` (the ephemeral server-computed HMAC of the authenticated canonical normalized access number per §2.5; never persisted or emitted), `subjectId`/`callerBindingId` and node/entry identity. The exact same call/session/binding/access identity and fingerprint returns the frozen decision and action unchanged; a binding replacement, revocation or rebinding (changed `callerBindingId`/`subjectId`), a changed session/protocol/node/access number, or any other changed fingerprint is a stable conflict before the frozen decision is returned and mutates nothing. A different canonical access number with an identical display mask conflicts because the fingerprint uses `accessNumberHmac`, not `accessNumberMasked`; missing or invalid HMAC input fails closed before replay or any effect. Replay precedes housekeeping and every accepted effect.
3. Only when no decision exists, acquire the canonical-number lock and call `subject-number.find_or_create_ivr_subject` (§10.20) with the trusted normalized number, on the same connection and in the same transaction, **before** the subject/session/call/activation locks. It reuses the unique active binding or atomically creates one unowned `IVR` subject and one active binding with both owner columns null; no customer account, login identity, owner or enterprise claim is created. The returned subject is then re-read under the subject lifecycle advisory and the IVR session/current-call locks in the §9.0.1 order; the activation row is a non-locking exact `SELECT` re-read under the subject advisory (this entry needs no activation row lock), and the owner lock-only collaborator is used only if a later side-effecting activation write requires its row lock. Disabled/retired/conflicting binding and concurrent-unique outcomes are fail-closed or reused exactly as §10.20 states; a technical failure rolls the tentative subject/binding back with the resolve.
4. Create or reuse the inbound session and expire stale `PENDING` tasks using `clock_timestamp()`. Housekeeping runs only after replay and the find-or-create identity step.
5. For a task-backed call, acquire the organization lifecycle advisory (7), the membership pair advisory (8) and the owner/creator account lifecycle advisory (9) after the session/call locks and before the task/entitlement checks, and re-read the exact §10.12 columns; only the owning module takes its own row lock. Apply the shared ordered predicates: the other `ACTIVE`/`UNKNOWN` call guard excludes the current `callId`; a competing call yields `REJECT_CALL_IN_PROGRESS` with a terminal `REJECTED` current call record (only when absent or a never-started/rejectable `ACTIVE` row) and no claim/reservation; a fresh `SUSPENDED` subject yields `REJECT_SUSPENDED`; no activation or missing current consent yields `GUIDE_ACTIVATION` (`ACTIVATION_REQUIRED`/`CONSENT_REQUIRED`); current caller/subject/binding/owner-workspace access, activation/consent, the bounded unique task and the §10.18 single-eligible-balance/positive-entitlement predicate are checked in order. Organization/membership/account lifecycle applies only to the task-backed path; the no-task direct path requires no customer login, membership or real-name.
6. Create or bind the current call record `ACTIVE` for any non-terminal action before the attempt savepoint, with `task_id = NULL`; the call is outside the tentative branch and survives a category-2 rollback. Then establish the same-connection attempt savepoint before any tentative task claim/reservation/balance debit.
7. For `DIAL_TASK_CALLEE`, require the task `PENDING`, bound to the current caller/subject/binding/owner workspace, with current creator/workspace access and expected revision. Inside the tentative branch, after the task lock is held, atomically (a) conditionally update the task with `expires_at > clock_timestamp()` and `claimed_call_id = call_record.call_id`, and (b) conditionally assign the current call's `task_id` from NULL to that task's `id`. The update is allowed only under the existing IVR owner guard and exact UPDATE grant, requires reciprocal equality `task.claimed_call_id = call.call_id` and `call.task_id = task.id`, and rejects a non-null/different task, arbitrary relink, task switch or second call. Then call `entitlement-metering.reserve_call_usage` with the validated allocation policy in the same transaction; freeze the typed decision. For `ENTER_CALLEE` (only after a positive no-`PENDING`/`CLAIMED`-task check), `GUIDE_PURCHASE` or `GUIDE_ACTIVATION`, write no claim, link or reservation.
8. A 0-row claim/link or expected reservation/typed allocation denial executes `ROLLBACK TO SAVEPOINT`, which removes both the task claim/link and the call's null-to-task assignment while the pre-savepoint call survives with `task_id = NULL`; the service performs one bounded reread and freezes a deterministic non-dial decision (and its completion where applicable). The same task may be claimed and linked again by a later attempt within its TTL. A technical failure is category 3 and rolls back the whole transaction including task, call record, session, reservation, decision, event, and any tentative subject/binding side effects; it never rolls back only to the savepoint. A current `UNKNOWN` call is never destructively finalized. Enqueue Outbox/audit and commit before returning an authorization.

### 9.14 T-S2-04 Direct DTMF authorization

1. Authenticate the IVR service and immutable call/session/caller identity; validate the direct-authorization confirmation `eventId` is nonblank and 1-128 characters before any decision read, idempotency or domain effect, refusing a blank or over-128 ID with stable `IVR_ARGUMENT_INVALID` and no write; acquire the canonical-caller lock and the same subject/IVR session/current-call locks as initial resolve. Foreign organization/membership/account rows use their advisory locks plus exact `SELECT` re-reads; the activation row is a non-locking exact read under the subject advisory. Read the existing `(callId, DIRECT_AUTHORIZATION)` decision first. The §10.17 direct fingerprint includes `purpose`, `protocolVersion`, `callId`, `sessionId`, the immutable caller HMAC and `accessNumberHmac` (the ephemeral server-computed HMAC of the authenticated canonical normalized access number per §2.5; never persisted or emitted), prompt version, entered-callee HMAC and confirmation key/content, and excludes the transport `confirmationEventId`: the exact same semantics with a regenerated transport event ID replays the frozen authorization; changed session/protocol/access number/callee/prompt/key/content is a stable conflict. A different canonical access number with an identical display mask conflicts because the fingerprint uses `accessNumberHmac`, not `accessNumberMasked`; missing or invalid HMAC input fails closed before replay or any effect. Replay precedes every check and effect; a replayed success never creates a second reservation or starts a redial.
2. Require a valid **current purpose chain**, not necessarily `INBOUND_RESOLVE = ENTER_CALLEE`:
   - (a) **Direct inbound chain**: the frozen `(callId, INBOUND_RESOLVE)` decision is `ENTER_CALLEE`, and no later `ACTIVATION_NEXT_ACTION` or other decision supersedes or conflicts with it; or
   - (b) **Post-activation chain**: the frozen `(callId, INBOUND_RESOLVE)` decision is `GUIDE_ACTIVATION`, the inbound session is `CONFIRMED` with the same subject, and exactly one committed `(callId, ACTIVATION_NEXT_ACTION)` decision is `ENTER_CALLEE`. This is the legitimate `GUIDE_ACTIVATION → CONFIRMED → ENTER_CALLEE → direct DTMF authorization` path permitted by docs/13 §6.3-6.4 and the accepted plan D23/INV-34.
   The request must not carry a non-empty `taskId` or any task-backed fallback. Reject a task-backed decision/call (non-null `call_record.task_id` or `claimed_call_id`), a later decision whose action is a no-dial action (`GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS`), an unconfirmed or contradictory session/subject purpose chain, an obsolete earlier `ENTER_CALLEE` superseded by a later no-dial decision, and any request that tries to re-enter or rewrite the original resolve. The original `INBOUND_RESOLVE` decision is immutable and is never updated to `ENTER_CALLEE`.
3. After a positive no-task check, recheck no `PENDING`/`CLAIMED` task for the caller and no other `ACTIVE`/`UNKNOWN` call excluding the current `callId`. A competing call yields `REJECT_CALL_IN_PROGRESS` with no reservation and finalizes only the current call's record when it is absent or a never-started/rejectable `ACTIVE` row. An existing current `UNKNOWN` row is referenced unchanged.
4. Reuse the shared trusted-caller, subject, activation/current consent and single-eligible-balance/positive-entitlement checks (§10.18) without a customer login, membership, real-name or platform-registration prerequisite. Create or bind the current `call_record ACTIVE` if needed; then establish the same-connection attempt savepoint before the tentative reservation/claim branch; create or reuse exactly one reservation for the `callId` from the selected balance.
5. Freeze the typed decision. Success has purpose `DIRECT_AUTHORIZATION`, `action` null, stable `reasonCode = DIRECT_AUTHORIZED`, `taskMatch = NONE`, null task fields, the committed `reservationId`/`authorizedRecordingSeconds`/`authorizationExpiresAt` and the confirmed callee only under `authorization.calleeNumber` in the encrypted payload. It never returns `ENTER_CALLEE` after the callee was confirmed and never introduces an eighth action. A direct refusal freezes a permitted non-dial action with no reservation and no authorization object; an allocation policy/integrity denial uses `REJECT`/`DATA_INTEGRITY` with the typed code only in the encrypted internal payload/audit.
6. Enqueue Outbox/audit and commit. An expected reservation/typed allocation denial executes `ROLLBACK TO SAVEPOINT` and freezes the refusal; a technical failure is category 3 and rolls back everything. Retry returns the first decision and never authors a second reservation or a redial. `authorizationExpiresAt` is copied from the reservation and is the last instant to start the B-leg. The IVR persists one unique `callId` B-leg operation in its existing local authoritative store before any line request; that local operation, not business `ivr.call_leg`, proves outbound uniqueness (Q28). A task-backed dial, when separately authorized through the task path, always uses the frozen task callee and never asks the caller to confirm it; the direct DTMF path does not fall back to it.

### 9.15 T-S2-05 Usage settlement

1. Accept the authoritative end/usage fact by `(call_id, event_domain, event_id)` from the validated IVR owner handoff (§9.24); lock the selected balance row(s) in ascending UUID and then the reservation, per the §9.0.1 item 12 suborder (never the reservation before a later balance row); validate the reservation identity, source event identity, occurrence time, end reason and authoritative units. Receipt of a durable `call_event` row is not itself proof of authoritative usage; only the validated terminal usage outcome may settle.
2. Apply the F-10 boundary exactly:
   - **Known use within the authorized boundary** (`1 <= reportedUnits <= authorized_units`): settle once. In one conditional balance `UPDATE` keyed by the locked `id`/expected revision, decrement `reserved_units` by `authorized_units`, increment `consumed_units` by `reportedUnits`, and set the derived `status` in the same statement (`EXHAUSTED` exactly when the new `consumed_units = granted_units`, otherwise unchanged), so `ck_entitlement_balance_status_shape` holds immediately (OQ-8). Set the reservation's `settled_units = reportedUnits`, `status = 'SETTLED'`, `terminal_at`, `authority_source = 'IVR'`; append exactly one `usage_fact` with `units = reportedUnits` and the same source event; append Outbox/audit; commit. The unused remainder (`authorized_units - reportedUnits`) is released once in the same transaction.
   - **Reported units greater than `authorized_units`**: do not clamp to `authorized_units`, do not settle, do not release and do not create a usage fact or evidence. Move/maintain the reservation in `RECONCILING` with `reconciliation_due_at`, increment `reconciliation_attempt_count`, set `last_error_code = 'DURATION_EXCEEDS_AUTHORIZED'`, quarantine the event/terminal fact for audit and alert. This includes a value within any hypothetical future tolerance because no tolerance is approved (Q8). Only an approved later reconciliation outcome may resolve it.
   - **Known zero use** (`reportedUnits = 0`, with an authority-confirmed terminal fact that no effective recording occurred): transition `HELD`/`RECONCILING → RELEASED` with `settled_units = 0`, release the reserved units once, set `authority_source = 'IVR'` and `terminal_at`; create **no** `usage_fact` because `units >= 1` is required. Known zero use is not fabricated positive usage, and release never marks the balance `EXHAUSTED`.
   - **Negative, missing, reversed, mismatched or contradictory facts**: quarantine for audit and reconciliation; no usage fact, no release, no settlement. `UNKNOWN` authority keeps the reservation occupied (`HELD` until expiry, then `RECONCILING`) and never releases it.
3. A late or duplicate event returns the first settlement/release and never recharges or re-releases. The unique `(reservation_id, usage_type)` and `(source_event_domain, source_event_id)` keys converge duplicates. Contradictory terminal facts are never applied as success.
4. Unused units release at most once. A reservation whose call never started and whose authority confirms no usage becomes `RELEASED` with `settled_units = 0`; a started call with known zero effective recording also releases once with no usage fact. Unknown use never releases, and an over-authorized report never releases.
4a. The balance counters and derived status are never updated by separate statements: each settlement or release uses one conditional `UPDATE` that satisfies the balance CHECKs at every intermediate point. An `UNKNOWN` or excess-duration outcome writes no balance counter and no status, so uncertain use stays occupied and is never released (OQ-8).
5. Any SQL/lock/I/O/Outbox failure is category 3: the whole settlement transaction rolls back; no swallowed exception and no partial settle/release.

### 9.16 T-S2-06 Reservation reconciliation

1. **Phase A — short locked state transition.** A scheduler scans `HELD` reservations whose `authorization_expires_at` has passed and whose call has no accepted terminal usage fact. For each candidate it opens one short owner transaction and calls the IVR-owned `ivr.lock_reconciliation_caller` collaborator with `Propagation.MANDATORY` on the same connection. Given the immutable `callId`, under `ivr_role` that collaborator reads the existing `ivr.call_record.caller_number_hmac` and acquires the rank-1 canonical-caller advisory lock `ivr.caller:<canonical_caller_hmac>`; it takes no row lock and writes/appends/enqueues nothing. On its stable success outcome, the entitlement owner reads the reservation's immutable `balance_id` with a non-locking `SELECT` (never a later-order reservation row lock); locks the selected balance row(s) in ascending UUID order and then the `usage_reservation` row `FOR UPDATE`, which is the §9.0.1 item 12 suborder; re-reads status/time/revision and the balance facts under the locks; conditionally moves `HELD → RECONCILING` with `reconciliation_due_at` and `reconciliation_attempt_count + 1`, or, for an already-`RECONCILING` row whose due time has arrived, refreshes the attempt/due facts; commits. A `CALL_UNKNOWN` not-found or `DATA_INTEGRITY` outcome acquires no lock and performs no balance/reservation write; the scheduler records only the stable attempt/error and never treats the outcome as no-use or a release. Phase A writes no release, no usage fact and no unknown-use release; it records the durable `RECONCILING` retry/alert facts and releases every PostgreSQL and advisory lock before phase B.
2. **Phase B — lock-free authoritative IVR query.** After the phase-A commit, query the authoritative self-developed IVR record by `callId` with no PostgreSQL row lock and no advisory lock held. The query may be slow or unavailable; a timeout/unknown outcome is not converted into success, no-use or release. The IVR remains the external authoritative record; the entitlement role does not read IVR tables directly.
3. **Phase C — ordered relock and convergence.** Open a new owner transaction and call `ivr.lock_reconciliation_caller` with `Propagation.MANDATORY` and the same immutable `callId`; on its stable success outcome it holds rank 1 and has taken no row lock or written anything. The entitlement owner then discovers `balance_id` with a non-locking read; locks the balance row(s) in ascending UUID order and then the reservation `FOR UPDATE`; re-reads status/revision and the current durable state; then calls the `entitlement-metering` owner settlement service, which reuses the held rank-12 locks and the same suborder. A `CALL_UNKNOWN` not-found or `DATA_INTEGRITY` outcome acquires no lock and performs no balance/reservation write.
   - Terminal known usage within `1..authorized_units`: settle used units, release the unused remainder once, append exactly one usage fact, set `SETTLED`, and append Outbox.
   - Known zero/no effective recording: set `RELEASED` with `settled_units = 0` and append no positive usage fact.
   - Reported use above `authorized_units`: keep `RECONCILING` with `DURATION_EXCEEDS_AUTHORIZED`, quarantine/alert and release nothing until an approved reconciliation.
   - Unknown, slow or unavailable authority: keep `RECONCILING`, increment `reconciliation_attempt_count`, set `reconciliation_due_at` and `last_error_code`, keep the active-call guard and the reserved units occupied, and release nothing. A later scheduler attempt repeats phases A-C.
   Commit and release all locks before any further external I/O.
4. A late authoritative event may converge `RECONCILING` once; duplicate events return the first result. `UNKNOWN` is never treated as no usage or permission to redial. Reconciliation never silently clamps, fabricates usage, releases over-authorized use or treats a later entitlement expiry/suspension as erasing already-observed usage. No PostgreSQL or advisory lock is held across the phase-B network query; no reconciliation phase holds the reservation row lock before a later balance row lock. Reconciliation and settlement therefore share one serial order (balance rows ascending UUID, then reservation) and cannot deadlock on that suborder. The canonical-caller key is obtained only through `ivr.lock_reconciliation_caller` under `ivr_role`; the entitlement role never reads `ivr.call_record` directly and takes no foreign row lock.


### 9.17 T-S2-07 Order creation

1. Authenticate the account and derive the workspace server-side; acquire the subject lifecycle advisory lock (3); then call `activation-consent.lock_service_activation` at rank 4 to take the existing activation row lock through the owner role (a missing row is serialized by subject (3) and locks nothing); then acquire the organization lifecycle advisory (7), the membership lifecycle advisory for the logical pair (8) and the account lifecycle advisory (9) in the §9.0.1 order, re-reading current authority with the exact §10.12 columns after each advisory lock. Only the owning module takes its own row lock; the order service never takes a foreign row lock. Verify subject access, ordinary-client real-name, current active service activation and the current `ACTIVE` offer/version token. Validate the client key length at 1-475 before any mutation (a 476-512 key is rejected with no order/consent/idempotency write, F-13); reject a new checkout while an existing order for the same subject/offer is `UNKNOWN` and require a query of that original order.
2. Compute the amount on the server and derive `purchaseConsentEventId` exactly once as `purchase-consent:` + the lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, clientIdempotencyKey}` from the authenticated canonical lowercase account UUID and the exact client key, a fixed 81-character value used for both the consent `event_id` and its `source_ref`; the order client key remains 1-475 and the platform key 1-512, with no truncation, random per-attempt identity, caller-chosen alternative or schema expansion (OQ-1). Build the §10.17 `PURCHASE` order fingerprint from the immutable semantic fields, including that stable identity. Call `platform.idempotency_begin('PURCHASE', account_id || ':' || idempotency_key, request_fingerprint)` after the caller/session/access locks and the rank-4 activation lock, and before any order or consent append (S2 placement §9.0.1 item 10).
3. `REPLAY` returns the first completed order result and appends no second consent. `CONFLICT` rejects changed input atomically. Only `NEW` continues.
4. For `NEW`, call `activation-consent.append_purchase_consent` with `Propagation.MANDATORY` (§10.20) using the current prompt/policy version/key/content, the authenticated subject/account/channel and the derived 81-character `purchaseConsentEventId` as both the consent `event_id` and `source_ref`. It reuses the rank-4 activation lock taken at step 1 and never acquires rank 4 after the idempotency row. It returns the one immutable `purchase_consent_id` for an explicit `ACCEPT`; missing/stale/declined/mismatched consent returns a typed denial, and the order transaction rolls back the `NEW` idempotency row and any tentative consent append so no partial row commits. In the same transaction validate the consent's `purpose = 'PURCHASE'`, `subject_id`, `channel`, `source_type = 'ACCOUNT'`, `account_id`, current policy/prompt fields, `action = 'ACCEPT'`, `event_domain/event_id`, `source_ref` and fingerprint; any mismatch fails closed before order insert and rolls back the same writes. The order service does not insert `consent_record` directly and does not infer consent.
5. Insert the order once with `request_fingerprint`, the exact §10.19 frozen commerce/schedule snapshot built from the validated approved catalog input, `UNPAID` payment state, the validated consent reference and the accepted version token. Call `platform.idempotency_complete(recordId, revision, 'ORDER', orderId, redactedOrderSnapshot, snapshotSha256)` in the same transaction; append Outbox/audit; commit. A forced failure rolls back the order, consent, idempotency record and Outbox together. An enterprise quote cannot enter personal self-service payment; no order-level entitlement status is written.

### 9.18 T-S2-08 Payment to entitlement issuance

1. Verify the provider event and insert one `payment_event`; lock the payment order and update payment status only from verified evidence; enqueue Outbox in the same transaction. This payment fact is independent of issuance.
2. Receive a validated approved `installmentNo` input (the due-installment trigger remains Q20 and is not inferred). For each benefit line of that installment, the consumer inserts or claims one `entitlement_issuance` row by `(order_id, installment_no, entitlement_type)`, locks it, reads the frozen §10.19 order snapshot (never current catalog state), requires exactly one matching schedule entry for the line/installment/type, and validates units/validity window/quantity/type against the approved schedule. It writes the grant, balance and Outbox in one transaction, sets `grant_id`/`ISSUED`, and increments `attempt_count`/`revision` conditionally. `valid_from`/`expires_at` are copied from the validated approved schedule entry; the grant's `snapshot`/`snapshot_sha256` copy the immutable order/version/line facts and the consumed schedule entry, never current catalog state; no due date, grace period or calendar arithmetic is computed and no price/quantity/date is guessed.
3. Missing/unapproved/ambiguous timing, a snapshot/schedule/type/quantity mismatch or a non-matching installment records stable `FAILED`/`last_error_code` with no grant, no balance and no guessed units; the payment fact remains `PAID` and independently observable. Retry moves `FAILED → PENDING` under the issuance row lock and never duplicates the grant. An order-level entitlement view is a computed projection. No module writes another module's issuance or order status directly.
4. The §10.19 snapshot is immutable and historical; a later catalog publication/offsale or policy change never rewrites it. A partial multi-line failure is observable per row.

### 9.19 T-S2-09 Operations suspend and resume

1. An operations suspend runs in the activation-consent-owned operations lifecycle service, which acquires the subject lifecycle advisory lock and its own `service_activation` row lock, then, before any owner-local activation write, calls `operations-iam.lock_employee_administrator` with `Propagation.MANDATORY`. That operations-iam-owned collaborator takes the administrator advisory lock and the authenticated employee row under `operations_iam_role` at rank 13, writes/appends nothing and returns the current facts; `activation_consent_role` takes no foreign row lock and receives no `operations_iam` privilege. The service then appends exactly one `ACTIVE → SUSPENDED` transition with `source_action_domain = 'OPERATIONS'`, non-null action event identity, fingerprint, employee actor and reason; it updates status/revision and enqueues audit. A non-`OPERATIONS` suspend is rejected by the service even though the broad DB check would accept the shape (OQ-7). Suspension blocks new accepted activation/reservation but preserves history.
2. Resume follows the same subject/activation/`operations-iam.lock_employee_administrator` employee/administrator lock order and appends only `SUSPENDED → ACTIVE` with its own action event identity, employee actor and reason. It never calls `issue_first_trial`, never creates a trial/grant/balance and never rewrites consent or evidence.
3. A concurrent IVR confirmation serializes on the subject lifecycle lock: if suspend commits first, the fresh confirmation returns `REJECT_SUSPENDED`; if the confirmation commits first, the later suspend does not undo the granted trial and a replay after suspend returns the original decision without changing `CONFIRMED → REJECTED`.

### 9.20 T-S2-10 Consent-gap recovery

1. On initial resolve, an `ACTIVE` activation whose required current consent is missing or expired returns `GUIDE_ACTIVATION` with stable `CONSENT_REQUIRED`, no task claim and no reservation.
2. The same confirmation path presents the current prompt, validates the current policy version/key/content and appends the current consent. It returns `GUIDE_ACTIVATION`/`CONSENT_REQUIRED` when the confirmation content is stale/invalid, and on accepted current consent it reuses the existing trial (`ALREADY_GRANTED`/`REPLAY`) rather than issuing a second trial.
3. Phase B then requires the resulting consent to be current. A stale or invalid confirmation content is rejected before any accepted consent/transition/trial append. The consent gap never rewrites the original resolve decision.

### 9.21 T-S2-11 IVR replay, conflict and cutoff precedence

1. Event-level `IVR_CONFIRM`: same key/fingerprint returns the stored first result; same key/changed fingerprint, including a different `callId`, is a stable conflict; only `NEW` continues.
2. Call/purpose-level: an existing `(callId, ACTIVATION_NEXT_ACTION)` is compared by its own stored canonical fingerprint. A match with a new event ID completes that event's `NEW` row with the existing decision ID and frozen result, and performs no consent/trial/claim/reservation/Outbox. A mismatch rolls back the transaction and returns a stable semantic conflict.
3. Only when no call/purpose decision exists does a fresh confirmation proceed. The unique `(call_id, purpose)` index is a concurrency backstop: a loser rolls back, rereads and returns the existing decision on a fingerprint match or a stable conflict otherwise.
4. Existing-result handling precedes the `SUSPENDED` check and every confirmation side effect. A completed decision is never overwritten and a `CONFIRMED` session never becomes `REJECTED`.
5. A replay at or after `authorizationExpiresAt` returns the same frozen cutoff and never renews it or authorizes a new B-leg. A B-leg started before the cutoff continues under the frozen authorized seconds. A lost response is recovered by `callId`; no second B-leg is started.

### 9.22 S2 transaction assertions (document level)

The §13.7 S2 test matrix is the normative assertion list. At minimum, the specification requires: exactly one `NEW` winner per idempotency key; replay returns the first immutable result; changed input conflicts with the `NEW` row rolled back; a task claim never commits without a reservation; category 1, 2 and 3 outcomes are distinct; the attempt savepoint is established after accepted activation/trial and before any tentative claim/reservation, and a category-2 rollback preserves activation/trial while category 3 rolls back every write; the order path and IVR confirmation take the rank-4 activation row lock through the owner lock-only collaborator before ranks 7/8/9 and the idempotency row, and later consent/activation services reuse it; a single eligible balance is selected under an approved policy or the attempt fails closed with no claim/reservation; a task claim never commits without a reservation and no second balance is debited; known zero use releases once without a positive usage fact while unknown use stays occupied and excess use is quarantined; exactly one publication/trial-policy winner commits after retiring the previous current row first, and a stale loser returns a stable current state; both direct-authorization purpose chains are accepted and task-backed/obsolete/later-no-dial chains are rejected; an unknown parent call returns `CALL_UNKNOWN` before insert and the first call-event terminal outcome is absorbing; no cross-owner table write or row-lock privilege is used; no post-commit access re-read reverses an issued authorization; and a duplicate/reordered call event never double-settles or releases unknown use.

### 9.23 S1 deterministic behavior assertions

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
- Claim early-rejection risk timestamp: a fresh binding/account/organization/membership/access predicate rejection that occurs before the step-4 risk read writes the database claim-decision time into `risk_checked_at` and a typed `riskEvaluation = NOT_REQUIRED` audit snapshot, and never claims a risk lookup or result; a risk-evaluating path stores the actual current risk-decision time. The column remains NOT NULL and append-only.
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

### 9.24 T-S2-12 Call-event ingestion, processing and settlement handoff (F-08)

1. Authenticate the IVR service identity and validate the immutable `callId`, `eventId`, `eventType`, `sequenceNo` (when supplied), `occurredAt`, payload hash and leg ID (when supplied). The `eventId` must be nonblank and 1-128 characters; a blank or over-128 ID is refused with the stable protocol argument error `IVR_ARGUMENT_INVALID` before any idempotency row or domain effect, while the general platform key capacity remains 1-512. Normalize and compute the §10.17 call-event fingerprint. Acquire the canonical-caller lock when the event can affect usage.
2. **Replay and conflict first, before any mutation.** Recompute the §10.17 call-event fingerprint from the request. Read the existing `(event_domain='IVR_EVENT', event_id)` row and recompute its fingerprint from the immutable `payload_redacted` members (`source.eventType`, `source.legId`, `source.sequenceNo`, the digest of `source.payload`), never from the indexed mutable `sequence_no`.
   - Same stored fingerprint: return the stored first processing outcome (`APPLIED`/`QUARANTINED`/`IGNORED`) and mutate nothing.
   - Changed fingerprint/type/sequence: return a stable `EVENT_ID_CONFLICT` without updating the original row, payload, status, call/leg or settlement, including when the first stored outcome was `SEQUENCE_CONFLICT`; an optional conflict audit uses that same event identity and adds no schema object.
3. **Validate the parent call before insert.** A missing/unknown `call_id` (no `ivr.call_record` parent) returns stable `CALL_UNKNOWN` with no `call_event`, call, leg or settlement write and no raw FK. Never create a placeholder call or nullable parent link.
4. **Acquire the owned state locks before sequence detection or insert.** In the §9.0.1 rank-4 suborder, lock the owned `ivr.inbound_session` row first and then the current `ivr.call_record` row, and re-read both after acquisition. Validate session/task/leg identity: a leg ID that does not belong to the call is classified `LEG_UNKNOWN` and receives no call/leg mutation or settlement (an unknown parent call was already refused in step 3 with no row). No reservation/balance lock is taken here.
5. **Detect the sequence conflict and insert once, while the call lock is held.** Insert exactly one `call_event` row for the known call with `processing_status = 'RECEIVED'`, the encrypted raw payload and the exact §4.27 `payload_redacted` shape (`source` plus server-written `receipt`), where `payload_sha256` hashes only the canonical `source.payload` and `receipt.eventFingerprint` holds the fingerprint from step 2. A `LEG_UNKNOWN` classification from step 4 is persisted by this same insert as `processing_status = 'QUARANTINED'` with `reason_code = 'LEG_UNKNOWN'`. If a different `event_id` reuses an already-used non-null `(call_id,sequence_no)`, detect it under the held call lock before insert and persist the new event with indexed `sequence_no = NULL`, `processing_status = 'QUARANTINED'` and `reason_code = 'SEQUENCE_CONFLICT'`; the original sequence and fingerprint remain in `payload_redacted`, so only a same-semantic retry of that same event returns this first `QUARANTINED` outcome. Never expose the partial unique violation as a protocol error and never apply the event.
6. Apply event facts deterministically:
   - outbound/answer/bridge/recording start/recording end/end events set only forward-consistent call or leg facts; an event that would move a terminal call/leg backwards, contradict an already accepted terminal fact or supply a conflicting authoritative duration is `QUARANTINED` (`CONTRADICTORY_FACT`) and is never applied;
   - an out-of-order but consistent event is applied once; the missing sequence gap does not by itself block an authoritative terminal fact;
   - a terminal end event with an accepted authoritative source sets `call_record.status` to `ENDED`/`FAILED` and writes the accepted `ended_at`, `end_reason` and `authoritative_seconds`; a terminal end event whose line/call outcome is not authoritative writes `status = 'UNKNOWN'` and the stable reason only, leaving `ended_at` and `authoritative_seconds` NULL so `ck_call_record_terminal_shape` and `ck_call_record_authoritative_seconds` hold immediately; `UNKNOWN` remains occupied, is never finalized `REJECTED` or released, and only authoritative reconciliation may later move it to `ENDED`/`FAILED` with the accepted terminal time and duration;
   - `call_leg` updates use the same forward/terminal rules; unrelated calls/legs are never touched.
   Set `processing_status` to exactly one terminal `APPLIED`/`QUARANTINED`/`IGNORED` with a stable reason and conditional revision; that first terminal outcome is absorbing and is never changed by a later delivery. A duplicate applied event returns its first outcome.
7. **Settlement handoff.** If the event is an accepted authoritative terminal/usage fact and the call has a reservation, call `entitlement-metering.settle_call_usage` with the validated immutable `callId`, reservation ID, `(IVR_EVENT,eventId)`, authoritative units, occurrence time and authority source in the same transaction, using `Propagation.MANDATORY`. The entitlement owner performs §9.15, appends at most one `usage_fact`/terminal transition/Outbox and never reads or writes IVR tables. Durable `call_event` receipt (`RECEIVED`/`APPLIED`) is distinct from accepted authoritative usage; a non-authoritative, missing, negative, contradictory or over-authorized event produces no settlement and no release.
8. Duplicates, late and out-of-order events converge to one first settlement/outcome; a duplicate end event returns the first settlement and never recharges. A second terminal event with a conflicting duration is quarantined and alertable; no second usage fact or release.
9. No double settlement: the reservation's one terminal transition, the unique `usage_fact` keys and the §9.15 state/outcome rules are authoritative. `UNKNOWN` never releases. The task/call terminal handoff is owner-safe: `ivr` owns call/leg/call_event status; `entitlement-metering` owns reservation/balance/usage. No cross-owner DML.
10. Enqueue Outbox/audit and commit. Any SQL/lock/I/O/Outbox failure is category 3: roll back the event row, processing status, call/leg updates, settlement, usage fact and Outbox together. A database exception is never swallowed. T-S2-05 starts from the accepted end event produced by this sequence.

### 9.25 T-S2-13 MINIAPP/WEB activation and first trial (F-05)

1. Authenticate an active account and derive the personal or organization workspace server-side. Validate current subject access and the current prompt/policy version/key/content. The service re-reads `customer_account(id,status)`, `organization(id,status)`, `organization_membership(organization_id,account_id,role,status)` and `business_subject(id,owner_account_id,owner_organization_id,revision)` under the rank-7/8/9 and subject advisory locks using the exact §10.12 grants; it takes no foreign row lock and never writes those rows. The client supplies only the explicit accept/decline, the stable activation event ID and the semantic fields; it cannot supply account, workspace, subject ownership, role, policy or trial outcome. The stable activation event ID must be nonblank and 1-128 characters, refused with `IVR_ARGUMENT_INVALID` before any replay read or effect; the server derives `source_ref` from the authenticated account and that exact event ID.
2. Acquire the subject lifecycle advisory lock (3), lock its own existing `service_activation` row (4) as the owning module, then acquire the organization lifecycle advisory (7), the membership pair advisory (8) and the account lifecycle advisory (9) in the §9.0.1 order. Re-read all current lifecycle/access/consent predicates with the exact §10.12 columns after the advisory locks; no cross-owner row lock is taken.
3. Replay first: read the unique transition by `(source_action_domain='ACTIVATION', source_action_event_id=eventId)` and the §10.17 activation-transition fingerprint. The transition fingerprint binds the immutable `SERVICE_ACTIVATION` consent fingerprint; comparisons use request semantic inputs and persisted immutable originals, never a generated consent ID or the current transition revision. Same fingerprint returns the first consent/activation/trial result and performs no effect; changed fingerprint is a stable conflict; a different stable action event at the current revision is a distinct transition or a stable current-state conflict. A missing/declined/stale consent returns a typed `CONSENT_REQUIRED`/`ACTIVATION_CONSENT_DECLINED` refusal with no accepted append.
4. On a fresh accepted decision, call `activation-consent.record_client_activation` (§10.20) with `Propagation.MANDATORY`: append exactly one immutable `SERVICE_ACTIVATION` consent (`channel = 'MINIAPP'|'WEB'`, `source_type = 'ACCOUNT'`, `source_ref` = `activation:` + the lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, activationEventId}` (75 characters) from the authenticated canonical account and stable action ID, `action = 'ACCEPT'`, purpose `SERVICE_ACTIVATION`, event domain `ACTIVATION`, the validated stable event ID, and the exact §10.17 `SERVICE_ACTIVATION` consent fingerprint); create or update the one `service_activation` row; append exactly one transition (`INACTIVE → ACTIVE`, `to_revision = from_revision + 1`) whose fingerprint binds that immutable consent fingerprint; call `entitlement-metering.issueFirstTrial` with `source_action_domain = 'ACTIVATION'`, the same event ID and the canonical granting-action fingerprint. The service reuses the rank-4 activation lock taken at step 2 and never re-acquires rank 4. An already-`ACTIVE` row appends no duplicate transition and reuses the existing trial outcome (`ALREADY_GRANTED`/`REPLAY`); it never reissues a trial.
5. Do not invent trial eligibility: the only trial gate is the validated current activation consent plus the V006 policy/function outcomes. No login identity, owner, real-name or enterprise fact is created by this service. Category 1 (pre-activation refusal) appends no accepted consent/transition/trial; category 3 rolls back all writes; a normal client refusal is a committed refusal result only if it does not require accepted activation.
6. Append Outbox/audit and commit. A fresh activation and its trial, transition and Outbox commit together. No `NEW` platform idempotency scope is added; the unique event identity/fingerprint is the replay key.

### 9.26 T-S2-14 Catalog-version and trial-policy publication (F-09)

1. Authenticate the operations employee and validate the allowed catalog/trial-policy publication action. The employee identity is authentication context, not a cross-module table write. Publication does not mutate employee state and therefore does not take the administrator-mutation advisory lock; it takes only the owner publication serialization in step 2 and the owning module's rows.
2. Acquire the owner serialization:
   - for a catalog version, `pg_advisory_xact_lock(hashtextextended('catalog_order.publication:<product_id>', 0))`;
   - for a trial policy, `pg_advisory_xact_lock(hashtextextended('entitlement_metering.trial_policy:FIRST-TRIAL', 0))`.
   These are owner-scoped transaction advisory namespaces, not new tables, functions or product rules. They serialize publication/offsale/disable for the same product or campaign without requiring `UPDATE` on the append-only product row.
3. Catalog publication: with the caller's expected current active version/revision (or explicit "no active"), re-read under the lock. Require the candidate `DRAFT` with `published_at IS NULL`, validated immutable content and benefit lines, and `version_no` uniqueness. If the current active version differs from the caller's expectation, return stable `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW` with no change (the loser rereads and returns the current state; it does not retry by selecting another version). On success, in one transaction: **first** set the previous `ACTIVE` version `sale_status = 'OFFSALE'` with revision +1; **then** set the candidate `published_at` and `sale_status = 'ACTIVE'` with revision +1; leave published content, benefit lines, historical orders and grants byte-identical; append audit and the §10.17 `CATALOG_VERSION_PUBLISHED`/`CATALOG_VERSION_OFFSALE` Outbox identities; commit. The old-current retirement must precede the candidate activation because `uq_catalog_version_active` is an immediate partial unique index; no statement may see two qualifying `ACTIVE` rows. An already-`ACTIVE` candidate with the same expected state returns the current immutable state (idempotent); a raw unique violation is mapped to a stable conflict after one bounded reread, never exposed.
4. Catalog offsale: under the same lock, require the version `ACTIVE` (or already `OFFSALE` for an idempotent replay), set `OFFSALE`, revision +1, append audit/Outbox. It stops new orders only; it never changes a frozen order, consent, grant or issuance row. This S2 specification does not implement or claim an independent `catalog_offer` withdrawal endpoint/transaction; that decision belongs to Product/Operations/catalog-order and the catalog operations OpenAPI work (OQ-4), and version offsale remains the S2 control that blocks new orders.
5. Trial-policy publication: under the trial-policy lock, require the candidate never-published (`published_at IS NULL`), validate its immutable content, **first** disable the previously enabled published version (`enabled = false`, revision +1), **then** publish the candidate with ONE owner `UPDATE` that, while the candidate's `OLD.published_at IS NULL`, sets `published_by_employee_id` to the actual authenticated publishing employee, `published_at`, `enabled = true` and `revision + 1` together, in the same transaction, so the immediate partial unique `uq_trial_policy_enabled` sees at most one qualifying row at every statement. The guard admits that single statement and rejects any later change to publisher identity or published content; no separate pre-publication publisher statement is required, the publisher value is the actual authenticated publisher at publication, not a draft-owner label, and no same-employee-only rule is invented (OQ-5). A stale expected-current-version loser returns `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW` with no change. The sole authoritative policy read used by issuance is the V006 `issue_first_trial` function's enabled/published/expected-version read inside the entitlement owner (§10.9): replay and already-granted return prior immutable results before any new policy selection; a missing/disabled/stale policy fails closed at that read; a disable committed before that read is observed and fails closed; a disable committed after it does not revoke the already-selected grant; the application wrapper lookup is preliminary only and there is no second enabled recheck (OQ-6). Trial-policy disable sets `enabled = false` with revision +1 under the same lock; an already-disabled policy is an idempotent current state. Published content, snapshot and history are never rewritten; existing grants keep their frozen policy snapshot.
6. Concurrent publications have exactly one winner under the owner lock; the loser returns the stable current-state/conflict outcome, never a raw unique error and never a second active version or enabled policy. A technical failure rolls back all rows, audit and Outbox together. No generic workflow, version-approval engine or new table is introduced.

### 9.27 T-S3-01 Recording upload completion and media validation

1. Authenticate the IVR or media-worker service identity through its authenticated service context and validate `recording_id` as nonblank 1-128 characters. A blank/oversized identity is refused with a stable argument error before any row lock or write. The IVR remains the owner of telephony, call/leg, recording-source and authoritative usage facts; `evidence` owns media/evidence metadata and lifecycle. The media worker is a task executor only; it submits a validated result to the evidence owner service and never writes `evidence` tables directly.
2. Perform the object-storage HEAD/read, server-side SHA-256 rehash, MIME sniff, size/duration verification and malware scan in the media adapter **before** the PostgreSQL transaction. Record the exact object version, bucket, key, hash, size, MIME, duration and malware outcome in the submitted result. No PostgreSQL lock is held over this network/CPU work.
3. Open one local transaction and lock the `evidence.recording` row by `recording_id` `FOR UPDATE`. Re-read `status`, `upload_expires_at`, declared facts and `revision` with `clock_timestamp()`. Completion accepts only an `UPLOADED` row; a `DECLARED` row returns `UPLOAD_OBSERVATION_REQUIRED` with no media object, recording transition or evidence fact and must first call `evidence.record_upload_observed`. A same-identity completion whose server facts equal an existing `VALIDATED`/`QUARANTINED` outcome returns the first terminal outcome without a new media object or event. While the row is `UPLOADED`, a different object version/hash/size/MIME/duration transitions once to `QUARANTINED` (stable `RECORDING_IDENTITY_CONFLICT`/`MEDIA_VALIDATION_FAILED`) and never overwrites the first server facts or an original media object. After the row is already `VALIDATED`/`QUARANTINED`, a differing callback is refused with stable `RECORDING_IDENTITY_CONFLICT` and appends exactly one deterministic redacted Outbox conflict fact via `platform.outbox_append`; it performs no recording update, media-object insert, fact overwrite or revision change. The conflict fact uses the §4.40 `recording-conflict:<conflictDigest>` dedupe key and a redacted payload with `recordingId`, `originalStatus`, `conflictDigest`, `presentedSha256`, `presentedSizeBytes`, `presentedMime`, `presentedDuration` and `reasonCode` only; object version/key material is hashed, never stored. The same conflicting callback returns the existing Outbox ID and appends no second fact; a changed conflicting payload has a different digest and appends one distinct fact.
4. If `upload_expires_at <= clock_timestamp()` and the row is still `DECLARED`/`UPLOADED`, the completion or upload-observation is refused and the row transitions once to `QUARANTINED` with `UPLOAD_EXPIRED` and no successful media/evidence fact; a terminal row is not updated and a differing callback follows step 3. If the storage result is unavailable/unknown, no `media_object` is inserted, the row remains non-solid and the call returns a bounded retryable `STORAGE_UNKNOWN`; a later retry with the same `recording_id` converges. A failed malware scan transitions a non-terminal row to `QUARANTINED`/`MALWARE_DETECTED` and never creates a valid media object.
5. For a valid completion of an `UPLOADED` row, reuse the immutable `evidence.media_object` recorded by `evidence.record_upload_observed`; do not insert a second object. Validate its server facts and set `validated_duration`, `validated_sha256`, `validated_at`, `status = 'VALIDATED'` and `revision + 1` in one conditional update; `media_object_id` is already set and immutable. A declared/validated mismatch quarantines; the declared hash/duration never wins. A concurrent media-object unique loser with the same identity/hash reads the winner; a different identity/hash is a stable conflict/quarantine.
6. Append the exact §10.24 redacted Outbox identity for the outcome (`EVIDENCE_RECORDING_VALIDATED` or `EVIDENCE_RECORDING_QUARANTINED`) and its payload fingerprint, plus an audit fact where the outcome is security-relevant, then commit. The same semantic retry returns the existing Outbox ID and appends nothing; a changed payload under the same dedupe key raises `OUTBOX_DEDUPE_CONFLICT` and appends nothing. A technical error rolls back the media object, recording transition, Outbox and audit together; the previous `DECLARED`/`UPLOADED` state remains and a retry can converge. No evidence record is created or solidified in this transaction.
7. Late/duplicate/out-of-order completion callbacks with the same `recording_id` and identical object facts return the first terminal outcome without a second event. A conflicting late callback after `VALIDATED`/`QUARANTINED` is refused with stable `RECORDING_IDENTITY_CONFLICT` and one deduplicated redacted Outbox conflict fact as in step 3; it is never applied over the first server facts and never performs a forbidden terminal status change. A partial crash after validation but before commit leaves no media object/status update/conflict fact; the same retry recomputes and converges. Call completion and evidence solidification remain independent facts.

Upload-observation replay ordering: `evidence.record_upload_observed` first derives/resolves the deterministic recording/object identity and immutable observation fingerprint. An existing identical observation returns the first committed `UPLOADED` result before any current recording-status precondition and mutates nothing; an existing same identity with a changed fingerprint returns `RECORDING_IDENTITY_CONFLICT` and mutates nothing. Only a genuinely new observation requires the current `DECLARED` status, locks the row, re-reads it and performs one `DECLARED → UPLOADED` update. Object/network validation remains before the PostgreSQL transaction and row lock, and only `UPLOADED` proceeds to validation/quarantine.

Stable S3 media reason codes include `UPLOAD_EXPIRED`, `STORAGE_UNKNOWN`, `MALWARE_DETECTED`, `MEDIA_VALIDATION_FAILED` and `RECORDING_IDENTITY_CONFLICT`. They are service-level codes carried in redacted events/audit; no media bytes, bucket, key, version or permanent URL is logged.

### 9.28 T-S3-02 Evidence creation and solidification with the reciprocal cycle

1. Authenticate the evidence owner service and validate the stable creation source event `eventId` (nonblank 1-128); that exact validated `eventId` is the creation `source_event_id`. Evidence creation does not require customer registration, login, real-name verification or a subject claim; an unregistered/unverified IVR user's evidence is created from the validated capture with the immutable identity-at-capture fact. Read `evidence_record` by `(source_event_domain = 'EVIDENCE', source_event_id = eventId)`. Under the same stable `eventId`, exact subject/call/session/reservation/recording/consent/snapshot facts return the first evidence/event/artifact result; changed subject/call/session/reservation/recording/consent/snapshot facts return `EVIDENCE_SOURCE_CONFLICT` and create no second record, creation event, artifact or Outbox fact. A different validated source event is a distinct creation identity. The lookup identity is never derived from `consentVersion`, an optional link, the snapshot or any semantic fact that must conflict.
2. Require the referenced recording to be `VALIDATED` and to have a media object whose server hash/size/MIME/duration/version match the submitted validation facts. Require `recording.call_id = evidence_record.call_id`; a cross-call recording is `EVIDENCE_LINK_MISMATCH` and creates no evidence fact. The evidence owner re-reads its own recording/media rows; it does not trust a declared hash or a worker-only success. A recording that is not `VALIDATED` or a media identity mismatch yields a typed evidence refusal (`EVIDENCE_MEDIA_NOT_VALIDATED`) and creates no evidence fact.
3. Validate the exact S1/S2 identities with read-only exact `SELECT`s and require all supplied trusted links to agree: `subject_number.business_subject(id)`; `ivr.call_record(call_id, session_id, subject_id)` with `call_record.subject_id = evidence_record.subject_id`; when a session is supplied, `ivr.inbound_session(id, call_id, subject_id)` with `inbound_session.id = evidence_record.session_id`, `inbound_session.call_id = evidence_record.call_id` and `inbound_session.subject_id = evidence_record.subject_id`, and `call_record.session_id = evidence_record.session_id`; when a reservation is supplied, `entitlement_metering.usage_reservation(id, call_id, subject_id)` with `reservation.call_id = evidence_record.call_id` and `reservation.subject_id = evidence_record.subject_id`. The recording's call/session/subject chain identified in step 2 is the trusted anchor. Any null required trusted link, missing parent or cross-call/cross-session/cross-subject/cross-reservation mismatch returns stable `EVIDENCE_LINK_MISMATCH` with no evidence record, creation event or artifact and no raw FK error. Missing optional session/reservation links remain NULL; a session without a call is rejected by `ck_evidence_link_shape`.
4. Construct the immutable `identity_at_capture` object (subject ID, internal binding reference if any, caller HMAC/masked projection, source channel, capture time) and the canonical `snapshot` (`capture`, `content` with recording/media hash/size/MIME/duration/artifact roles, an exact `source` object `{callId, sessionId, subjectId, reservationId, recordingId}` (canonical lowercase UUID text or JSON null for the optional session/reservation), `sourceEventDomain`, `sourceEventId` and `consentVersion`). The `source` members must equal the persisted link columns and `recordingId` must equal `source_ref_id`; `ck_evidence_snapshot_shape` enforces that equality, while the recording-to-call equality is the step-2 service predicate. The snapshot contains no raw number, object key/URL, media bytes or share token; `snapshot_sha256` hashes the canonical redacted form. The captured consent version is supplied by the validated capture fact; it is never inferred or defaulted.
5. In one local transaction, set the record's `source_event_id` to the validated source event `eventId` exactly as validated in step 1 (no hash or derivation), insert the `evidence_record` in `PENDING`, and insert exactly one `EVIDENCE_CREATED` event whose `(event_domain, event_id)` equals the record's `(source_event_domain, source_event_id)` — that is, the same validated `eventId` — and whose `evidence_id` equals the record ID. The two inserts may occur in either order. Insert the `ORIGINAL` `evidence_artifact` for the validated original media object. The record's `source_ref_id` identifies the validated recording; the owner service validates that polymorphic source target. The creation event's payload fingerprint is its §10.24 canonical redacted payload; a same-identity retry with the same fingerprint returns the first record/event/artifact result, and changed subject/call/session/reservation/recording/consent/snapshot facts under that same stable identity are `EVIDENCE_SOURCE_CONFLICT` with no second record, event, artifact or Outbox fact.
6. If every validation passes, set `status = 'SOLIDIFIED'`, `solidified_at`, `revision + 1` and append the `EVIDENCE_SOLIDIFIED` evidence event with the §10.24 identity (type plus record ID) and payload fingerprint. If a validation/linkage/storage fact fails, transition to `QUARANTINED` or `FAILED` with the stable redacted reason and append the matching `EVIDENCE_QUARANTINED`/`EVIDENCE_FAILED` event with its §10.24 identity and fingerprint; the creation event still must match the record at commit. The creation event itself is never rewritten by a later solidification/quarantine/failure event. A same-identity retry with the same payload returns the first lifecycle event and appends nothing; a changed payload under that identity is `EVIDENCE_EVENT_CONFLICT` and appends nothing.
7. At commit, both deferred FKs and the two deferred constraint triggers validate: exactly one `EVIDENCE_CREATED` event exists for the record, its type is `EVIDENCE_CREATED`, its domain/event identity equals the record source, and its owner `evidence_id` equals the record ID. A missing/duplicate/wrong-type/wrong-owner/mismatched creation event raises `EVIDENCE_CREATION_CYCLE_VIOLATION` and rolls back the entire transaction, including record, event, artifact, Outbox and audit. A failed transaction leaves no creation fact.
8. Append the domain Outbox event (with the §10.24 identity where the registry names one) and audit fact in the same transaction. A solidification retry returns the first committed result and does not append a second creation event, lifecycle event or original artifact; a changed payload under the same identity is a stable conflict. A later claim, account change, organization membership change, subject suspension or real-name change never updates `subject_id`, `call_id`, `session_id`, `reservation_id`, `identity_at_capture`, `consent_version`, `snapshot` or the original artifact. Call completion does not imply evidence completion, and evidence solidification does not complete or alter a call.

Stable S3 evidence codes include `EVIDENCE_MEDIA_NOT_VALIDATED`, `EVIDENCE_LINK_MISMATCH`, `EVIDENCE_SOURCE_CONFLICT` and `EVIDENCE_CREATION_CYCLE_VIOLATION`. They are redacted service/DB outcomes, never raw data.

### 9.29 T-S3-03 Share grant creation and owner transitions

1. Authenticate the creator account. Derive `share_id` from the authenticated canonical account and stable `creationRequestId` and read any existing grant by that unique identity first. Whether or not a grant exists, require the target evidence record to be `SOLIDIFIED` and call the evidence-owner `evidence.read_shareable_projection` collaborator on the same connection with `Propagation.MANDATORY`; for an existing grant, call it before recomputing or comparing the frozen `content_snapshot.creation` fingerprint, just as for a new creation, but still before any state-dependent check or mutation. Reconstruct the deterministic server-derived `content` from the returned projection plus the current validated request parameters, then recompute and compare the frozen creation fingerprint: exact immutable `SOLIDIFIED` facts with otherwise same request semantics return the existing grant metadata only with no new grant/token/event; changed scope/parameters or changed/inconsistent artifact/media/content facts return stable `SHARE_CREATION_CONFLICT` with no mutation; an absent, unresolvable or not-`SOLIDIFIED` projection fails closed with the existing typed error and creates or mutates no grant, token or event. The collaborator returns only the exact redacted evidence ID/status/source/capture identity, artifact roles and approved media SHA-256/size/MIME/validated duration facts needed to build the frozen content. Do not read media-object identity, bytes, bucket, key, version, permanent URL or raw protected data, and take no foreign row lock. Re-read the exact current subject ownership/risk projection, including the captured `identity_at_capture.bindingId` when present or otherwise the evidence subject's current active binding set; any `OPEN` risk returns `SHARE_RISK_BLOCKED` and no grant, and an absent/unresolvable binding/risk projection fails closed. Consume the approved evidence-access policy input for the creator/workspace and evidence scope; absent, unapproved or ambiguous policy fails closed with `SHARE_ACCESS_POLICY_UNAVAILABLE` and no grant. The exact role/resource policy remains assigned to Product/Security/Sharing and the sharing policy/authorization design; this specification does not invent it.
Replay projection ordering for step 1: for an existing grant creation identity, step 1 calls the read-only same-connection `evidence.read_shareable_projection` collaborator before recomputing or comparing the frozen `content_snapshot.creation` fingerprint, just as for a new creation, but still before any sharing state-dependent mutation. It reconstructs the deterministic server-derived `content` from the returned projection plus the current validated request parameters. Exact immutable `SOLIDIFIED` evidence/artifact/media facts with otherwise same request semantics return the existing grant metadata only; changed or inconsistent artifact/media/content facts under the same identity return `SHARE_CREATION_CONFLICT` and mutate nothing; an absent, unresolvable or not-`SOLIDIFIED` projection fails closed with the existing typed error and creates or mutates no grant, token or event. The collaborator remains read-only, same-connection and owner-collaborator-only, takes no foreign row lock and does not redesign current access or risk policy.

2. Derive the workspace scope server-side. For an organization share/creator context, acquire the §9.0.1 ranks 7/8/9 advisory locks in order and re-read the exact organization/membership/account columns; a non-active organization, non-active membership or disabled account fails closed. A personal share takes rank 9. No foreign row lock is taken and no S1/S2 table is written.
3. Generate at least 128 random capability bits server-side, return the plaintext capability token only once at first creation and persist only the lowercase SHA-256 digest. For `EXTRACTION_CODE`, generate the approved code, a fresh salt and a salted slow-KDF digest bound to the grant and `code_generation`; no KDF parameters or code length are invented (Q12). For `DESIGNATED_MOBILE`, normalize the designated number server-side and persist only its keyed HMAC and key version in `content_snapshot.verification`; admission also requires an approved trusted SMS possession proof for that exact normalized number HMAC and the intended share/action, and absent approved verification input fails closed (no OTP service/table is added). For `ORGANIZATION_MEMBER`, require `owner_organization_id`. For `NONE`, store no verification binding and never record identity verification.
4. Build and freeze the typed `content_snapshot` from the exact §2.12 evidence-owner projection (content scope, verification mode/binding and the `creation` object with its digest/fingerprint from §2.12); `scopeDigest` is the canonical `{content, verification}` digest and `content_sha256` is the canonical complete-snapshot digest, exactly as §2.12 states; freeze `share_id`, evidence, creator, workspace, `valid_from`, `expires_at`, `max_admissions`, `download_allowed` and revision 1. No object key, bucket, version, permanent URL, raw creation request ID or plaintext secret enters the snapshot. Insert one grant; unique `share_id`/token-hash losers reread and return the existing grant only for the same creator/scope/fingerprint or a stable conflict. Append audit/Outbox and commit. A lost creation response returns metadata only; a usable replacement requires a new `creationRequestId`/new grant/token, and the unusable prior grant may be revoked through the existing owner revoke. There is no token reissuance/reconstruction action and no guessed expiry/limit default.
5. For revoke, code rotation, expire, lock or extend, first resolve the deterministic §2.12 transition event identity and fingerprint. If that `SHARE_ACCESS` event already exists with the same fingerprint, return its first recorded outcome before any current status/revision check and append nothing. If it exists with a changed fingerprint, return a stable conflict and mutate nothing. Otherwise lock the grant `FOR UPDATE`, re-read current status/revision/generation/time and apply only the allowed §7.4 transition; a different action request is a distinct transition. The same-transaction audit fact records the authenticated actor and action; the access event stores `actionType`, the stable request digest or expiry/source-revision fact and `actionFingerprint`, never a new mutable result or generated secret. Apply the conditional status/generation/revision update, append exactly one access event and one audit fact, and commit. A zero-row update performs one bounded reread and returns the current state or a stable conflict; no second status update or generation increment occurs.
6. Revoke increments `revocation_generation`, appends `REVOKED` and makes new admission/renewal fail; it is allowed from `LOCKED` as well as `ACTIVE`. Code rotation replaces salt/hash, increments `code_generation` and `revocation_generation`, clears `LOCKED` to `ACTIVE` only as the explicit code-rotation unlock path, and appends `CODE_ROTATED`; old codes and old sessions fail. Expiry persists `EXPIRED` with one generation increment when observed; `clock_timestamp() >= expires_at` denies even before persistence, and `LOCKED → EXPIRED` is allowed through this owner expiry. Extension increases `expires_at` only on an `ACTIVE` not-yet-expired grant with revision +1, appends `EXTENDED`, and neither extends an admission recorded under the earlier expiry nor lengthens an existing admission/session. Locking appends `LOCKED`, blocks new verification/admission and does not increment generation or invalidate an already-admitted session. There is no invented non-code `LOCKED → ACTIVE` unlock. Reaching the admission limit changes no status and no generation.
7. No plaintext code, capability token, designated phone or object URL is written to an access event, audit, Outbox or log. A technical error rolls back the status, event and audit together; the prior grant state remains.

### 9.30 T-S3-04 Share admission, count-once and generation witness

1. Authenticate the public share request and validate the capability token shape. Hash the presented token and resolve the grant by `capability_token_hash` without a row lock. A missing/retired token returns a uniform denial (`SHARE_TOKEN_INVALID`) without disclosing whether a grant/evidence exists. Require the approved Q22 session/ticket TTL before creating a new admission; absent, unapproved or ambiguous TTL policy fails closed with `SHARE_SESSION_POLICY_UNAVAILABLE` and creates no admission. An existing same-key admission replay uses its recorded `sessionExpiresAt`/`admissionExpiresAt` and does not recompute or extend them. `PAGE_OPENED`, `PREFETCH_IGNORED` and failed-verification facts append at most one deterministic access event using the §2.12 `share-observation:` identity and payload fingerprint and never create an admission; a same-semantic retry returns the first event, a changed payload under the same identity conflicts and appends nothing, and a different request key is a distinct observation.
2. Mode verification is required only to create a new admission and is performed outside the PostgreSQL row lock: extraction-code salted slow KDF; designated-mobile comparison of the stored HMAC plus an approved trusted SMS possession proof for the exact normalized number HMAC and intended share/action (knowledge or HMAC comparison alone is insufficient); organization-member authenticated-account current membership; `NONE` no identity verification. A failed verification appends `VERIFICATION_FAILED` and, under the approved lock policy, may set `LOCKED`; it consumes no admission and does not increment the count. A request whose durable admission key already exists is resolved by steps 3-5 and does not perform a fresh verification as a new attempt; any pre-computed verification result is discarded on the replay path. `NONE` is never promoted to a verified identity.
3. Open one local transaction. For `ORGANIZATION_MEMBER` (new admission or existing admission replay/reconstruction), acquire ranks 7/8/9 in §9.0.1 order and re-read the exact organization/membership/account predicates; a wrong tenant/account is `SHARE_WRONG_TENANT`, a revoked/suspended membership or disabled account fails closed, and an existing admission requires the authenticated recipient to equal its witness `recipientAccountId`/`recipientOrganizationId`. Re-read the exact §10.21 subject ownership/risk projection using the captured binding or the subject's active binding set; any `OPEN` risk denies with `SHARE_RISK_BLOCKED`, creates no admission and changes no count, and an absent/unresolvable projection fails closed. A risk-open committed before this re-read is observed; a later risk-open does not retroactively erase a committed admission but governs subsequent renewal/reconstruction. The sharing owner locks the grant row `FOR UPDATE` and re-reads status, mode, `code_generation`, `revocation_generation`, `valid_from`, `expires_at`, `max_admissions`, `admissions_used` and `revision` with `clock_timestamp()`. At or after `expires_at`, persist `EXPIRED` with one generation increment, append `EXPIRED`, commit the denial and return no admission. For `REVOKED`/`EXPIRED` or a generation/mode mismatch, deny with `SHARE_REVOKED`/`SHARE_GENERATION_STALE`; no count changes. `LOCKED` permits only the existing-admission replay path below; a new admission under `LOCKED` is denied with `SHARE_LOCKED`. If the mode/code generation/generation changed since verification, deny/re-verify under the locked state; a stale verification never authorizes.
4. Existing admission before the limit. If an admission with the same `(share_grant_id, admission_key)` exists, read its `ADMISSION_GRANTED` witness and require the current `revocation_generation`/`code_generation` to equal the witness. While the session is still valid (`clock_timestamp() < sessionExpiresAt` and `clock_timestamp() < admissionExpiresAt`, with the current grant/time checks above passing), return that original admission, append no second `ADMISSION_GRANTED`, apply no `max_admissions` test, and reissue the derived ticket without incrementing. If the session has expired, return the original admission fact with `SHARE_SESSION_EXPIRED` and no new ticket; a new admission with a new request ID/key must recheck the current policy and count. The same key never mints a second session, and an expired original is never renewed.
5. Only when no admission exists for the key and the current grant status is `ACTIVE`, apply the new-admission limit and insert. A `LOCKED` grant with no existing admission denies with `SHARE_LOCKED`. If `max_admissions` is not NULL and `admissions_used >= max_admissions`, deny with `SHARE_ADMISSION_LIMIT_REACHED`, change no status/generation and do not invalidate the last admitted session. Otherwise insert one immutable admission with `admitted_at = clock_timestamp()`, `admission_no = admissions_used + 1` and the current verification mode/result; the owner-local `BEFORE INSERT` guard rechecks grant existence, mode, active/validity/limit and `admission_no`. Append exactly one `ADMISSION_GRANTED` access event with the admission-time `verificationMode`, the original `ORGANIZATION_MEMBER` recipient account/organization binding when applicable, `revocation_generation`, `code_generation`, `grantExpiresAt`, `admissionExpiresAt` and `sessionExpiresAt` in the redacted payload, where `admissionExpiresAt = grant.expires_at` frozen at admission and `sessionExpiresAt = min(admitted_at + approved Q22 ticket TTL, admissionExpiresAt)`. The per-ticket absolute `ticketExpiresAt` and positive Redis TTL are computed only after commit as §9.31 states. Then conditionally update `admissions_used = admission_no`, `revision + 1` and commit. Any zero-row update/guard/unique/event failure rolls the whole transaction back; the same-key retry rereads and returns the first committed admission or a stable conflict.
6. Only after the PostgreSQL commit, derive the Redis playback ticket from `share_grant_id + admission_id` (see T-S3-05). A ticket failure never rolls back the admission and never increments the count; it appends `TICKET_FAILED` and returns fail-closed. A lost response returns the original admission and reissues the ticket. A page open, prefetch or failed verification never consumes an admission.
7. Race assertions are explicit. Admission vs revoke/rotation/expiry: if the lifecycle transition commits first, the admission lock observes the new status/generation and denies without count; if the admission commits first with all predicates valid, it counts once and its session is valid within the admission-time expiry/generation, while the later transition governs later admissions/renewals/reconstruction. Duplicate same-key admission: exactly one row/count and the same result for every retry, including when the first response/Redis ticket is lost at `max_admissions`; the retry is resolved before the limit, so the count stays one. Different-key concurrent admissions at the last slot: the immediate unique/guard and conditional count update allow exactly one winner; the other returns limit/conflict and changes no generation. Max reached: a new key increments no count and bumps no generation; an existing unexpired same-key session remains replayable, and `LOCKED` permits that replay while denying new keys. Expiry boundary: `now < expires_at` and `now < sessionExpiresAt`/`admissionExpiresAt` are required; exactly at/after expiry is a denial. Verification failure/lock and wrong tenant: no admission, no count and a redacted access event.

Every `requestKeyDigest` referenced by §9.30 and §2.12 is the lowercase hexadecimal SHA-256 of the exact stable nonblank request/action key, validated at 1-128 UTF-8 code points with no Unicode normalization and no case folding; raw request keys are never stored, logged, audited or emitted. This preserves the declared 82-character `share-observation:` identity length and the separate payload fingerprints.

### 9.31 T-S3-05 Ticket issue, reconstruction, media renewal and Redis failure

1. PostgreSQL commits the admission and its `ADMISSION_GRANTED` generation/expiry/recipient witness before any Redis ticket, and all PostgreSQL locks are released before the Redis call. The Redis key is deterministically `share-ticket:<share_grant_id>:<share_admission_id>`; the ticket value contains the grant/admission identities, admission-time revocation and code generations, `admissionExpiresAt`, `sessionExpiresAt`, the absolute `ticketExpiresAt`, a signed short-lived media credential reference and no unapproved policy value. Define `ticketExpiresAt = min(clock_timestamp() + approved Q22 ticket TTL, sessionExpiresAt, admissionExpiresAt, current grant expires_at)`; no TTL value, skew allowance or revocation-window value is invented. The Redis TTL is exactly `ticketExpiresAt - clock_timestamp()` only when that difference is positive; when it is zero or negative, no ticket is issued and the flow fails closed with the existing `SHARE_SESSION_EXPIRED`/policy outcome. A same-key retry within the valid session reissues this ticket; after `sessionExpiresAt` the same key returns the admission fact with `SHARE_SESSION_EXPIRED` and no new ticket, so the retry key cannot become unlimited reuse.
2. Issue the ticket with a short signed/segmented media credential. Only after Redis accepts the value append exactly one `TICKET_ISSUED` event with the deterministic `ticket-issued:` identity and redacted generation/expiry/payload hash. If the durable `TICKET_ISSUED` append fails, delete/expire the Redis ticket and return a bounded retryable failure; if deletion also fails, the signed ticket's absolute `ticketExpiresAt` is the bounded exposure and the failure is audited. A Redis write failure appends exactly one deterministic `TICKET_FAILED` event and returns fail-closed; no successful media authorization is claimed. A `TICKET_FAILED` event does not change the admission count or create a second admission.
3. Reconstruct on lost response, lost cache or Redis failure: for `ORGANIZATION_MEMBER`, first re-authenticate the recipient and take S1 ranks 7/8/9; only after those locks are held, read the admission and its `ADMISSION_GRANTED` witness, the current grant and the exact §10.21 subject ownership/risk projection. Then require the authenticated account to equal the witness `recipientAccountId`, the current active membership pair to equal `recipientOrganizationId`/`recipientAccountId`, the organization to be `ACTIVE`, the membership to be `ACTIVE` (not revoked/suspended), the account to be `ACTIVE`, and the grant `owner_organization_id` to equal the witness organization; a changed account/tenant, revoked/suspended member or disabled account is denied under `SHARE_WRONG_TENANT`/the approved membership refusal. Require the admission to exist, no `OPEN` risk for the evidence subject's captured/active binding, the current status to be `ACTIVE` or (for an already-admitted session) `LOCKED`, the current `revocation_generation`/`code_generation` to equal the admission-time witness, `clock_timestamp() < sessionExpiresAt`, `clock_timestamp() < admissionExpiresAt` and `clock_timestamp() < current expires_at`. If all hold, commit/close the PostgreSQL read and reissue the same derived ticket and return the first admission; if risk/session/expiry/generation/membership fails, deny with the stable code, append a redacted failure event where appropriate and do not increment or admit again; a new session requires a new admission/key/count. If revoke/rotation/expiry commits before this reconstruction read, it governs; if a valid ticket was already returned before a later revoke, the Q22 credential TTL is the bounded window and no recall is claimed. A stale successful admission therefore cannot bypass revoke, code rotation, expiry, current risk, membership revocation or the wrong-tenant check. The order is total: trusted recipient re-authentication and ranks 7/8/9 precede every sharing grant read or lock; all PostgreSQL reads/locks are released before Redis or media I/O.
4. Media playback/seek/renewal validates the short signed credential and rechecks the current grant status/time/generation and the exact captured/current binding plus `subject_risk_state` projection against the recorded witness. For `ORGANIZATION_MEMBER` it first re-authenticates the same recipient and takes ranks 7/8/9 before any grant read/lock, then requires the current active organization/membership/account. `OPEN` risk or an absent/unresolvable binding/risk projection denies historical-evidence playback/seek/renewal with `SHARE_RISK_BLOCKED`, issues no media credential and appends no access event; a changed/revoked member or tenant is denied fail-closed. Expired Redis sessions require a new admission, which rechecks current policy and counts; reaching the limit blocks the new admission while the last admitted session may continue within its recorded admission/session/grant validity. No PostgreSQL lock is held across the media gateway/Redis call; the PostgreSQL read/transaction closes before issuance.
5. Revocation uses short signed/segmented credentials and a bounded revocation window (Q22). Already-buffered or already-issued bytes are not promised to be recalled; the credential TTL/window is the explicit limit. Revoke/rotation/expiry invalidates subsequent ticket issuance/renewal and reconstruction, not the historical admission/access facts. Redis unavailable fails closed for new playback authorization; PostgreSQL remains the truthful source and durable admissions/events are not rewritten.
6. `TICKET_ISSUED` and `TICKET_FAILED` are durable, redacted and idempotent by their deterministic `SHARE_ACCESS` identities. A same-semantic retry returns the first event; a changed request is a distinct redacted event or a stable conflict. No event contains a plaintext capability token, code, phone, object URL/key/version, IP or media bytes.

## 10. Cross-Schema Function Contracts

### 10.1 Frozen function-wave baseline

The accepted plan has five function-wave rows: four V001/S1 functions and one V006/S2 function. The block is verbatim. S1 authored the four V001 contracts; S2 now authors the V006 `entitlement_metering.issue_first_trial` contract. The four V001 contracts remain unchanged and S1-owned. The S2 IVR action selection, reservation and activation-confirmation contracts are application services, not additional SQL functions, so the function-wave block remains five rows (4 V001 + 1 V006).

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

### 10.9 entitlement_metering.issue_first_trial (V006/S2)

Signature target:

`entitlement_metering.issue_first_trial(p_subject_id uuid, p_campaign_key text, p_trial_policy_version_id uuid, p_action_domain text, p_action_event_id text, p_request_fingerprint text) RETURNS TABLE (trial_grant_id uuid, entitlement_grant_id uuid, entitlement_balance_id uuid, outcome text, first_granted_at timestamptz)`

| Item | Contract |
| --- | --- |
| Owner / mode | `entitlement_metering_owner`; `SECURITY DEFINER`; `SET search_path = entitlement_metering, platform, pg_temp`; created in V006 after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance`; not created in V001-V005. The nested `platform.outbox_append` call executes with this owner's privileges, so the owner itself receives `USAGE` without `CREATE` on `platform` and `EXECUTE` on `platform.outbox_append` (internal owner capabilities; not a runtime-role grant and not `PUBLIC`). |
| Caller roles | `EXECUTE` to `activation_consent_role` and `entitlement_metering_role` only; both hold `USAGE` on `entitlement_metering` and `platform` without `CREATE`; `PUBLIC` receives no EXECUTE and no table privilege. The application service wrapper uses `Propagation.MANDATORY`; no transaction is created, committed or rolled back by the function. |
| Arguments | `p_subject_id` non-null existing subject; `p_campaign_key` exactly `FIRST-TRIAL`; `p_trial_policy_version_id` the expected policy version id, with the function itself performing the sole authoritative enabled/published/version-checked read and failing closed if it is missing, disabled or stale; `p_action_domain` `IVR_CONFIRM` or `ACTIVATION`; non-empty `p_action_event_id` length 1-128; lowercase canonical SHA-256 `p_request_fingerprint` |
| Outcomes | `GRANTED` with the new trial/grant/balance ids and `first_granted_at`; `REPLAY` with the original ids for the same `(action_domain, action_event_id)` and the same fingerprint; `ALREADY_GRANTED` with the original ids when the subject has a trial granted by a different action identity |
| Errors | `TRIAL_ARGUMENT_INVALID`; `TRIAL_CAMPAIGN_INVALID` for a campaign other than `FIRST-TRIAL`; `TRIAL_ACTION_CONFLICT` for the same action identity with a changed fingerprint; `TRIAL_DATA_INTEGRITY` when an existing trial lacks its grant or balance or the persisted source identity is inconsistent; `TRIAL_POLICY_NOT_FOUND` for a missing, disabled or stale expected policy version at the function's sole authoritative read |
| Semantics | Acquire the subject lifecycle advisory lock (re-entrant with the activation transaction). Read the permanent `trial_grant` for the subject first. If a trial exists with the same `(source_action_domain, source_action_event_id)` and the same fingerprint, return its original ids as `REPLAY`; if the same action identity has a changed fingerprint, raise `TRIAL_ACTION_CONFLICT`; if a different action identity granted the trial, return the original ids as `ALREADY_GRANTED`. These prior-result outcomes perform no new policy selection and do not re-read enabled state. Only when no trial exists does the function perform its single authoritative read of `trial_policy_version` for `campaign_key = 'FIRST-TRIAL' AND enabled AND published_at IS NOT NULL AND id = p_trial_policy_version_id`; a missing, disabled or stale version raises `TRIAL_POLICY_NOT_FOUND` and creates nothing. The function retains the selected version's immutable `granted_seconds`, `valid_days` and `snapshot` in local values through the grant and Outbox, and performs no second enabled recheck. It then inserts one `entitlement_grant` (`source_type = 'TRIAL'`, `source_ref_id = p_trial_policy_version_id`, the retained granted units and validity window, frozen policy snapshot), one `trial_grant` (`campaign_key`, policy version, action domain/event and fingerprint), one `entitlement_balance` and one Outbox event through `platform.outbox_append`, then returns `GRANTED`. Never insert a second trial, grant or balance. A trial without its grant or balance raises `TRIAL_DATA_INTEGRITY` and alerts. The function generates inserted `id` values with `pg_catalog.gen_random_uuid()`; there is no `p_id` argument. |
| Atomicity | Runs on the caller's connection and transaction; any exception aborts the caller's transaction and rolls back consent/activation/transition, trial, grant, balance and Outbox together. It contains no `COMMIT`, `ROLLBACK`, autonomous transaction or transaction-level `SET`; a database exception is never swallowed. |
| Redaction | The Outbox and result snapshots contain internal ids, campaign/policy facts and redacted status only; no raw number, callee, identity or secret. The §10.7 denylist applies. |

First-trial result semantics are explicit: `GRANTED` means this invocation inserted the one trial; `REPLAY` means the same action identity and fingerprint already produced it; `ALREADY_GRANTED` means another action for an already-granted subject reuses the original trial. A changed fingerprint for the same action conflicts and never reissues. The caller returns the original ids for `REPLAY` and `ALREADY_GRANTED` and never creates a second balance. On `GRANTED`, the function captures `granted_at` once from `clock_timestamp()`, sets `valid_from = granted_at` and `expires_at = granted_at + (policy.valid_days * interval '1 day')` in UTC, and stores the frozen policy snapshot; it does not infer month arithmetic, business calendars, grace periods or any production validity value beyond the approved positive `valid_days` (Q4/Q10/Q20). The V006 function itself performs the sole authoritative enabled/published/expected-version policy read and retains its immutable content; any application-wrapper lookup is preliminary only and has no selection authority. No cross-module trial-policy read grant is needed.

Policy-selection linearization (OQ-6): the V006 `issue_first_trial` function performs exactly one authoritative read of the enabled published `FIRST-TRIAL` `trial_policy_version` inside the entitlement owner transaction (`campaign_key = 'FIRST-TRIAL' AND enabled AND published_at IS NOT NULL` with the caller's expected version id), but only after the replay/already-granted check shows no existing trial. That single function-internal read is the policy-selection linearization point for the grant; the selected version id and its immutable content (`granted_seconds`, `valid_days`, `snapshot`) are retained in local values through the grant and Outbox. Any wrapper-level lookup is preliminary/advisory only and must never supply an unchecked or stale policy; it is not a second authority. Missing, disabled or stale state fails closed at that one read with `TRIAL_POLICY_NOT_FOUND` and creates no grant. If a publication/disable commits before the authoritative read, the read observes the new state; if a disable commits after the authoritative read, it does not revoke the already-selected grant, which keeps its frozen policy snapshot. Replay and already-granted outcomes return the immutable prior result before any new policy selection. There is no second enabled recheck, savepoint exception or lock inversion, and the publication advisory key is never combined with the IVR call-path lock order.

### 10.10 S1 function verification requirements

The later executable suite must prove each S1 function's owner is NOLOGIN/NOSUPERUSER, `PUBLIC` has no EXECUTE, the fixed `search_path` cannot be shadowed, and no function contains transaction control. It must also prove same-key replay/conflict, finalize-once, committed-`NEW` alerting, same-transaction rollback, Outbox dedupe, audit append-only, protected-key rejection and cross-role denial. It must additionally prove each function caller has schema `USAGE` without `CREATE`, the declared cross-module `SELECT` columns are readable while all cross-module writes are denied, no cross-owner row lock is attempted, and the guard triggers reject rebinding while legitimate status/revision/time/key-version updates succeed. S1 records these as document requirements; no function was executed against PostgreSQL in this S1 authoring.

### 10.11 S2 function verification requirements

The executable suite must prove `entitlement_metering.issue_first_trial` is absent from V001-V005, created only in V006 after its four dependency tables, owned by `entitlement_metering_owner`, `SECURITY DEFINER`, not executable by `PUBLIC`, fixed to `search_path = entitlement_metering, platform, pg_temp`, and free of transaction control. It must prove EXECUTE is granted only to `activation_consent_role` and `entitlement_metering_role`; the NOLOGIN `entitlement_metering_owner` has `USAGE` on `platform` and `EXECUTE` on `platform.outbox_append` so the nested Outbox call succeeds under `SECURITY DEFINER`, and no runtime role or `PUBLIC` receives those owner grants; a forced failure (including a forced nested Outbox failure) rolls back trial/grant/balance/Outbox with the activation transaction; `GRANTED` inserts exactly one trial/grant/balance/Outbox; same action plus same fingerprint returns the original ids and `REPLAY`; same action plus changed fingerprint raises `TRIAL_ACTION_CONFLICT`; a different action returns the original ids and `ALREADY_GRANTED`; an inconsistent existing trial raises `TRIAL_DATA_INTEGRITY`; the function performs the sole authoritative enabled/published/expected-version read; replay and already-granted return prior ids before any policy selection; a missing, disabled or stale expected version raises `TRIAL_POLICY_NOT_FOUND`; there is no second enabled recheck after the authoritative read; and no second trial/grant/balance is created for any replay, retry, policy-version, channel, claim or import path. S2 records these as document requirements; no PostgreSQL test is claimed.

### 10.12 S2 grant matrix

S2 runtime roles receive `USAGE` without `CREATE` on `platform` and `audit`, `EXECUTE` on the four S1 functions per §10.8, and the S2-specific grants below. No S2 runtime role receives `DELETE` or `TRUNCATE` on any business table. `PUBLIC` receives nothing. Every declared cross-module `SELECT` below is paired with `USAGE` without `CREATE` on the containing S1 or S2 schema for the same grantee in that role's creation/first-read wave (F-01); a column/table privilege without schema `USAGE` is not a valid contract. Schema `USAGE` never implies a table privilege or a write path.

Function, schema and platform grants:

| Object | Owner | S2 grants |
| --- | --- | --- |
| `catalog_order` schema | `catalog_order_owner` | `USAGE` without `CREATE` to `catalog_order_role`, `entitlement_metering_role`; no table privilege from schema USAGE |
| `ivr` schema | `ivr_owner` | `USAGE` without `CREATE` to `ivr_role`; no other S2 role |
| `entitlement_metering` schema | `entitlement_metering_owner` | `USAGE` without `CREATE` to `entitlement_metering_role`, `activation_consent_role`, `ivr_role` and `catalog_order_role` (read-only entitlement projections); no table privilege from schema USAGE |
| `activation_consent` schema | `activation_consent_owner` | `USAGE` without `CREATE` to `activation_consent_role`, `ivr_role`, `catalog_order_role` (exact reads only) |
| `customer_iam` schema | `customer_iam_owner` | `USAGE` without `CREATE` to `ivr_role`, `catalog_order_role` and `activation_consent_role` for their declared account/profile reads; existing S1 `subject_number_role` grant unchanged. No table write or `CREATE`. |
| `organization_access` schema | `organization_access_owner` | `USAGE` without `CREATE` to `ivr_role`, `catalog_order_role` and `activation_consent_role` for their declared organization/membership reads; existing S1 grants unchanged. No table write or `CREATE`. |
| `subject_number` schema | `subject_number_owner` | `USAGE` without `CREATE` to `ivr_role`, `catalog_order_role`, `entitlement_metering_role` and `activation_consent_role` for their declared subject reads; existing S1 grants unchanged. No table write or `CREATE`. |
| `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append` | `platform_reliability_owner` | `EXECUTE` to `catalog_order_role`, `ivr_role`, `entitlement_metering_role`, `activation_consent_role`; existing S1 grants unchanged |
| `audit.append_fact` | `audit_owner` | `EXECUTE` to the same four S2 roles; no table privilege |
| `entitlement_metering.issue_first_trial` | `entitlement_metering_owner` | `EXECUTE` only to `activation_consent_role`, `entitlement_metering_role`; `PUBLIC` denied; created V006 |
| `platform` schema and `platform.outbox_append` (internal owner capability) | `platform_reliability_owner` | `USAGE` without `CREATE` on `platform` and `EXECUTE` on `platform.outbox_append` to the NOLOGIN `entitlement_metering_owner` only, so the `SECURITY DEFINER` body's nested append succeeds. This is not a runtime-role grant, not a table grant and not `PUBLIC`. |

Owned-table column grants (no `DELETE`/`TRUNCATE`):

| Table | Role | `SELECT` | `INSERT` | `UPDATE` columns |
| --- | --- | --- | --- | --- |
| `catalog_order.catalog_product` | `catalog_order_role` | all | all | none (append-only) |
| `catalog_order.catalog_version` | `catalog_order_role` | all | all | draft content columns, `sale_status`, `offer_type`, `audience`, `price_cents`, `currency`, `billing_cycle`, `installments`, `grant_interval_months`, `snapshot`, `snapshot_sha256`, `published_at`, `revision` |
| `catalog_order.catalog_offer` | `catalog_order_role` | all | all | `offer_status`, `version_token`, `revision` |
| `catalog_order.catalog_benefit_line` | `catalog_order_role` | all | all | none (append-only) |
| `catalog_order.customer_order` | `catalog_order_role` | all | all | `payment_status`, `paid_at`, `closed_at`, `revision` |
| `catalog_order.payment_order` | `catalog_order_role` | all | all | `status`, `provider_transaction_id`, `updated_at`, `revision` |
| `catalog_order.payment_event` | `catalog_order_role` | all | all | none (append-only) |
| `ivr.outbound_call_task` | `ivr_role` | all | all | `status`, `reason_code`, `claimed_call_id`, `claimed_at`, `terminal_at`, `updated_at`, `revision` |
| `ivr.inbound_session` | `ivr_role` | all | all | `status`, `confirmed_subject_id`, `guidance_status`, `confirmed_at`, `revision` |
| `ivr.call_record` | `ivr_role` | all | all | `task_id`, `status`, `started_at`, `bridged_at`, `recording_started_at`, `recording_ended_at`, `ended_at`, `authoritative_seconds`, `end_reason`, `revision` |
| `ivr.call_leg` | `ivr_role` | all | all | `state`, `answered_at`, `ended_at`, `end_reason`, `revision` |
| `ivr.call_event` | `ivr_role` | all | all | `processing_status`, `reason_code`, `revision` |
| `ivr.ivr_decision` | `ivr_role` | all | all | none (append-only; insert only through `ivr.select_call_action`) |
| `entitlement_metering.trial_policy_version` | `entitlement_metering_role` | all | all | `published_by_employee_id`, `enabled`, `published_at`, `revision` (the single publication `UPDATE` sets the actual authenticated publisher, `published_at` once from NULL, `enabled` and `revision` together while `OLD.published_at IS NULL`; no separate publisher statement) |
| `entitlement_metering.entitlement_grant` | `entitlement_metering_role` | all | all | none (append-only) |
| `entitlement_metering.trial_grant` | `entitlement_metering_role` | all | none (function only) | none (append-only) |
| `entitlement_metering.entitlement_balance` | `entitlement_metering_role` | all | all | `reserved_units`, `consumed_units`, `status`, `revision`, `updated_at` |
| `entitlement_metering.entitlement_issuance` | `entitlement_metering_role` | all | all | `status`, `attempt_count`, `last_error_code`, `grant_id`, `first_attempt_at`, `issued_at`, `updated_at`, `revision` |
| `entitlement_metering.usage_reservation` | `entitlement_metering_role` | all | all | `status`, `settled_units`, `reconciliation_due_at`, `reconciliation_attempt_count`, `last_reconciled_at`, `authority_source`, `last_error_code`, `terminal_at`, `revision` |
| `entitlement_metering.usage_fact` | `entitlement_metering_role` | all | all | none (append-only) |
| `activation_consent.consent_record` | `activation_consent_role` | all | all | none (append-only) |
| `activation_consent.service_activation` | `activation_consent_role` | all | all | `status`, `revision`, `updated_at` |
| `activation_consent.service_activation_transition` | `activation_consent_role` | all | all | none (append-only) |

Cross-module `SELECT` grants (no `INSERT`/`UPDATE`/`DELETE`, no row-lock privilege):

| Grantee | Table | Exact columns | Purpose |
| --- | --- | --- | --- |
| `ivr_role` | `customer_iam.customer_account` | `id`, `status` | Owner/creator account lifecycle |
| `ivr_role` | `organization_access.organization` | `id`, `status` | Organization `ACTIVE` predicate |
| `ivr_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` | Membership lifecycle and role under the logical pair lock |
| `ivr_role` | `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` | Subject owner/access predicate |
| `ivr_role` | `subject_number.caller_number_binding` | `id`, `subject_id`, `number_hmac`, `status`, `verified_at`, `revision` | Current binding predicate |
| `ivr_role` | `subject_number.subject_risk_state` | `caller_number_binding_id`, `risk_type`, `state`, `revision` | Claim-risk policy read; not a blanket dial ban |
| `ivr_role` | `activation_consent.service_activation` | `id`, `subject_id`, `status`, `revision` | Current activation predicate |
| `ivr_role` | `activation_consent.consent_record` | `subject_id`, `purpose`, `channel`, `policy_code`, `policy_version`, `prompt_version`, `action`, `event_domain`, `event_id`, `accepted_at` | Current-consent predicate |
| `ivr_role` | `entitlement_metering.entitlement_balance` | `id`, `grant_id`, `granted_units`, `reserved_units`, `consumed_units`, `status` | Routing snapshot only, not spend authorization; `granted_units` is required to compute `granted_units - reserved_units - consumed_units` under the declared read grant |
| `ivr_role` | `entitlement_metering.entitlement_grant` | `id`, `subject_id`, `entitlement_type`, `granted_units`, `valid_from`, `expires_at` | Routing snapshot validity and the declared `balance.granted_units = grant.granted_units` predicate (OQ-2) |
| `catalog_order_role` | `customer_iam.customer_account` | `id`, `status` | Account access |
| `catalog_order_role` | `customer_iam.identity_profile` | `account_id`, `real_name_status` | Ordinary-client real-name condition |
| `catalog_order_role` | `organization_access.organization` | `id`, `status` | Workspace access |
| `catalog_order_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` | Workspace access |
| `catalog_order_role` | `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` | Subject access |
| `catalog_order_role` | `activation_consent.service_activation` | `id`, `subject_id`, `status`, `revision` | Current activation |
| `catalog_order_role` | `activation_consent.consent_record` | `id`, `subject_id`, `purpose`, `channel`, `event_domain`, `event_id`, `policy_code`, `policy_version`, `prompt_version`, `action`, `source_type`, `source_ref`, `account_id`, `request_fingerprint`, `accepted_at` | Purchase-consent subject/purpose/channel/source/policy currentness and `event_id = source_ref = purchaseConsentEventId` validation (F-07); read-only, no foreign row lock |
| `activation_consent_role` | `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` | Subject existence, ownership and revision for client activation |
| `activation_consent_role` | `customer_iam.customer_account` | `id`, `status` | Authenticated account active predicate for client activation |
| `activation_consent_role` | `organization_access.organization` | `id`, `status` | Organization workspace `ACTIVE` predicate for client activation |
| `activation_consent_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` | Current membership/role predicate for client activation |
| `entitlement_metering_role` | `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` | Subject/owner facts |
| `entitlement_metering_role` | `catalog_order.customer_order` | `id`, `account_id`, `owner_organization_id`, `subject_id`, `catalog_version_id`, `snapshot`, `snapshot_sha256`, `payment_status`, `currency`, `created_at` | Frozen paid-order snapshot |
| `entitlement_metering_role` | `catalog_order.payment_order` | `id`, `order_id`, `status`, `amount_cents`, `currency`, `updated_at` | Verified payment facts |
| `entitlement_metering_role` | `catalog_order.payment_event` | `id`, `payment_order_id`, `provider`, `provider_event_id`, `event_type`, `signature_verified`, `amount_cents`, `occurred_at` | Source event identity |
| `catalog_order_role` | `entitlement_metering.entitlement_issuance` | `order_id`, `installment_no`, `entitlement_type`, `status`, `grant_id`, `issued_at`, `last_error_code` | Computed order-level entitlement projection (R-04); read-only |
| `catalog_order_role` | `entitlement_metering.entitlement_grant` | `id`, `subject_id`, `entitlement_type`, `granted_units`, `valid_from`, `expires_at` | Computed order-level entitlement projection inputs (R-04); read-only |

- A cross-module reader re-reads after acquiring the lifecycle advisory lock and never takes `FOR SHARE`/`FOR UPDATE` on another module's table. If a row lock is genuinely required, the owning module exposes a same-transaction application-service collaborator under its own role; no cross-module row-lock privilege exists. `activation_consent_role` has no `operations_iam` schema USAGE, table privilege or row-lock privilege: the operations suspend/resume lifecycle obtains the authenticated employee row and administrator advisory lock through the operations-iam-owned `operations-iam.lock_employee_administrator` collaborator on the same connection under `operations_iam_role`, and that collaborator writes/appends nothing and is not a SQL function.
- `ivr.lock_reconciliation_caller` is a minimal IVR-owned, same-connection `Propagation.MANDATORY` lock-only collaborator invoked by the entitlement-owner reconciliation in phases A and C. Under `ivr_role` it reads the existing `ivr.call_record.caller_number_hmac` for the immutable `callId` and takes `ivr.caller:<canonical_caller_hmac>` at rank 1; it returns only a stable success, stable `CALL_UNKNOWN` not-found or stable `DATA_INTEGRITY` outcome, takes no row lock, writes/appends/enqueues nothing and is not a SQL function. `entitlement_metering_role` receives no `ivr` schema USAGE, table privilege or row-lock privilege and does not read `ivr.call_record` directly; the entitlement owner then discovers and locks only its own balance/reservation rows in the item-12 order, and all PostgreSQL/advisory locks are released before the phase-B IVR network query.
- `ivr_role` has `SELECT`/`INSERT` on `ivr_decision` and no `UPDATE`; the insert path is the IVR-owned service. `activation_consent_role` and `entitlement_metering_role` have no `ivr_decision` privilege.
- The S2 sensitive columns are not added to any public response projection. The `platform` and `audit` functions reject protected plaintext, and no S2 role receives platform/audit table privileges.

### 10.13 S2 published application-service contracts

These are application services inside one local transaction; they are not SQL `SECURITY DEFINER` functions, do not add a function-wave row and never commit or roll back independently.

| Published service | Owner | Called by / transaction | Writes | Returns |
| --- | --- | --- | --- | --- |
| `activation-consent.record_activation_confirmation` | activation-consent | IVR confirmation transaction with `Propagation.MANDATORY`, after replay and phase-A guards | Validates the current prompt and consent policy-version/key/content before any accepted append; accepted branch only writes `consent_record` (`event_domain = 'IVR_CONFIRM'`, `event_id` equal to the already validated nonblank 1-128 confirmation `eventId`, the transport-independent 48-character `ivr-session:` `source_ref`, and the §10.17 `SERVICE_ACTIVATION` fingerprint), `service_activation`, `service_activation_transition`; reuses the rank-4 activation row lock taken by the lock-only collaborator and takes no IVR row lock | Invalid/stale content: stable `REJECT`/`CONSENT_REQUIRED` with no accepted ids. Accepted: `consentRecordId`, `confirmationAnchorId` (new transition ID, or current latest transition ID for an already-`ACTIVE` row), `activationStatus` |
| `entitlement-metering.issueFirstTrial` | entitlement-metering | IVR confirmation and MINIAPP/WEB client-activation transactions with `Propagation.MANDATORY`; wraps `issue_first_trial` | The V006 `issue_first_trial` function performs the single authoritative enabled/published/expected-version `FIRST-TRIAL` policy read inside the entitlement owner (the OQ-6 selection linearization point), retains that exact immutable version/content in local values through the grant and Outbox, and writes trial, grant, balance and Outbox; replay/already-granted returns prior immutable results before any policy selection; a wrapper lookup is preliminary only and there is no second enabled recheck | `GRANTED`/`REPLAY`/`ALREADY_GRANTED` with original ids and `firstGrantedAt`; no trial-policy table read is granted to activation-consent |
| `entitlement-metering.reserve_call_usage` | entitlement-metering | The IVR confirmation, resolve and direct-authorization transactions with `Propagation.MANDATORY` | Selects exactly one eligible balance under the validated approved allocation policy (§10.18), inserts `usage_reservation` and conditionally updates the selected `entitlement_balance` under its row lock; no direct write to another module and no second balance debit | `reservationId`, `authorizedUnits`, `authorizationExpiresAt`, `selectedBalanceId`, `balanceSnapshot`; absent/unapproved/ambiguous policy, no entitlement, stale availability, cross-subject balance or invalid authorization input returns a typed domain denial for the attempt savepoint, while a technical error propagates to full rollback |
| `activation-consent.record_client_activation` | activation-consent | MINIAPP/WEB activation/order entry with `Propagation.MANDATORY` | One immutable `SERVICE_ACTIVATION` consent with the exact §10.17 fingerprint, the bound transition, activation and first-trial call; re-reads account/organization/membership/subject with the exact §10.12 grants; see §10.20 | Existing/first consent and transition ids, activation status, trial outcome |
| `activation-consent.append_purchase_consent` | activation-consent | Catalog-order creation with `Propagation.MANDATORY` | One immutable `PURCHASE` consent keyed by the derived 81-character `purchaseConsentEventId` (`purchase-consent:` + lowercase SHA-256 of `{accountId, clientIdempotencyKey}`), used as both `event_id` and `source_ref` (OQ-1); reuses the rank-4 activation lock and acquires no rank-4 lock after idempotency; see §10.20 | `consentRecordId` or a typed denial |
| `subject-number.find_or_create_ivr_subject` | subject-number | IVR initial resolve/direct attempt with `Propagation.MANDATORY` | Reuses the unique active binding or atomically creates an unowned IVR subject and active binding; no account/login/owner; see §10.20 | `subjectId`, `bindingId`, outcome |
| `ivr.process_call_event` | ivr | IVR event endpoint | Replay/conflict comparison from immutable stored payload, parent-call validation before insert, call-event insert/dedupe/apply/quarantine, absorbing first terminal outcome, call/leg update and settlement handoff; an unknown parent call returns `CALL_UNKNOWN` with no insert; see §9.24 | `callEventId`, `processingStatus`, applied/quarantine reason |
| `entitlement-metering.settle_call_usage` | entitlement-metering | IVR call-event owner service with `Propagation.MANDATORY` | Reservation settlement/release/quarantine from validated IVR facts, with the derived balance status set in the same conditional update (§9.15, OQ-8); see §9.24 | Terminal reservation outcome and first settlement identity |
| `catalog-order.publish_catalog_version` / `catalog-order.offsale_catalog_version` | catalog-order | Operations catalog endpoint | Serialized publication/offsale that retires the previous `ACTIVE` version before activating the candidate, with immutable content, audit/Outbox and stable loser; see §9.26 | Current version/status or stable conflict |
| `entitlement-metering.publish_trial_policy` / `disable_trial_policy` | entitlement-metering | Operations trial-policy endpoint | Serialized publication/disable that disables the previous enabled policy before publishing the candidate in one owner UPDATE that sets the actual authenticated publisher, `published_at`, `enabled` and `revision` together while `OLD.published_at IS NULL`, and keeps immutable content/audit/Outbox with a stable loser; see §9.26 | Current policy version/enabled state or stable conflict |
| `ivr.select_call_action` | ivr | Initial resolve (`entryMode = INITIAL_RESOLVE`) and the confirmation wrapper (`entryMode = POST_ACTIVATION`); direct authorization reuses its ordered predicates | One `ivr_decision` for the derived purpose, written only after the branch's tentative claim/reservation succeeds (a typed denial writes no dial decision inside the tentative branch); for `DIAL_TASK_CALLEE` the conditional task claim, current `ACTIVE` call binding and the reservation service call; for a successful direct authorization, `action` null with committed reservation fields and no task; for `REJECT_CALL_IN_PROGRESS`, the current call's terminal `REJECTED` record and never the competing row | `decisionId`, `action` (null only for successful direct authorization), `reasonCode`, typed snapshot and encrypted internal payload |
| `ivr.record_activation_next_action` | ivr | Thin IVR entry wrapper inside the confirmation transaction | Delegates all predicates and writes to `ivr.select_call_action`; persists no decision of its own | `nextActionDecisionId`, `action`, typed snapshot |
| `operations-iam.lock_employee_administrator` | operations-iam | Activation-consent operations suspend/resume with `Propagation.MANDATORY`, before the owner-local activation write | Rank-13 lock-only collaborator. Takes `operations_iam.admin_mutation` and locks the authenticated employee row under `operations_iam_role`, returns the current employee/administrator facts and creates, updates, appends and enqueues nothing; it takes no foreign row lock, grants `activation_consent_role` no `operations_iam` privilege and is not a SQL function. | Current employee/administrator facts only |
| `ivr.lock_reconciliation_caller` | ivr | Entitlement-owner reconciliation phases A and C with `Propagation.MANDATORY` on the same connection, before the owner-local balance/reservation locks | Rank-1 lock-only collaborator. Given the immutable `callId`, reads the existing `ivr.call_record.caller_number_hmac` under `ivr_role` and takes `ivr.caller:<canonical_caller_hmac>`; returns only a stable success, stable `CALL_UNKNOWN` not-found or stable `DATA_INTEGRITY` outcome, takes no row lock, and creates, updates, appends and enqueues nothing; `entitlement_metering_role` receives no `ivr` privilege and is not a SQL function. | Stable success/not-found/integrity outcome only; no row facts |

The confirmation transaction is one local IVR-owned transaction. The cross-owner services use `Propagation.MANDATORY` and never commit or roll back independently. The endpoint wrappers add only authentication, replay handling and the confirmation gate; all ordered predicates and every `ivr_decision` write live in `ivr.select_call_action`. A repeated confirmation with the same event ID or a new event ID with the same semantic fingerprint returns the stored `nextActionDecisionId`, action and frozen typed result; changed input conflicts with no side effect. The original `INBOUND_RESOLVE` decision is never updated.

The self-developed IVR owns the actual outbound B-leg, A/B bridge, recording, recording cutoff and the authoritative call/media/usage facts it reports; ONECALL owns accounts, activation, task/claim, entitlement, reservation, settlement and evidence state. The IVR does not create an order, claim payment, complete a payment or run a payment loop; it can only play a purchase guide to the WeChat mini-program. A matched task is taken once on inbound and its frozen callee is auto-dialed after committed authorization without asking the caller to confirm the number. There is no latest-callee lookup, FIFO queue, arbitrary second task or fallback to a historical callee.

### 10.14 Typed frozen action response snapshot

Every `ivr_decision` stores one immutable, purpose-discriminated snapshot. `response_snapshot` is the typed redacted JSON used for replay, audit and ordinary inspection; `response_payload_ciphertext` is the encrypted internal payload returned only to the authenticated IVR. `response_sha256` is the SHA-256 of the canonical redacted snapshot; the encrypted payload's redacted projection must equal that snapshot, and replay decrypts the same immutable payload rather than recomputing current state. The platform idempotency `result_snapshot` stores only the redacted typed result (decision ID, purpose, action or null, reason and masked task/authorization fields) and never duplicates the full callee.

| Field | Type / enum | Nullability | Source and rule |
| --- | --- | --- | --- |
| `decisionId` | uuid | NOT NULL | The frozen `ivr_decision.id`; the idempotency first result |
| `callId` | text | NOT NULL | Root protocol identity; included in every fingerprint |
| `purpose` | `INBOUND_RESOLVE`, `ACTIVATION_NEXT_ACTION`, `DIRECT_AUTHORIZATION` | NOT NULL | Derived from the authenticated endpoint/session, never caller-supplied |
| `action` | the seven actions, or null | NOT NULL for `INBOUND_RESOLVE`/`ACTIVATION_NEXT_ACTION`; NULL for successful `DIRECT_AUTHORIZATION`; a direct refusal uses `GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED` or `REJECT_CALL_IN_PROGRESS` | Selected by the shared service; `DIRECT_AUTHORIZATION` is a purpose, not an eighth action |
| `callerState` | `UNREGISTERED`, `INACTIVE`, `ACTIVE`, `SUSPENDED` | NOT NULL | docs/13 stable enum; `UNREGISTERED` does not block IVR guidance/direct activation/trial |
| `taskMatch` | `CLAIMED`, `NONE`, `EXPIRED`, `REJECTED`, `CONFLICT` | NOT NULL | Section 10.1 matrix; `NONE` means no candidate was selected/evaluated, not proof that none exists |
| `reasonCode` | stable registry in §2.9 | NOT NULL | `DIRECT_AUTHORIZED` exactly when `action` is null; no free-text reason |
| `availableRecordingSeconds` | integer >= 0 | NOT NULL | Routing snapshot only, not spend authorization. It is the available units of the single balance selected by the same validated approved allocation-policy input and eligible-balance predicate used by `entitlement-metering.reserve_call_usage` at the same locked snapshot (§10.18); it never sums multiple balances (no stacking). When no balance is eligible, or the approved policy is absent/unapproved/ambiguous, it is `0` and no dial/reservation occurs. |
| `balanceAsOf` | timestamptz | NOT NULL | Database transaction snapshot time |
| `task.taskId` | uuid | NULL unless a candidate/claimed task was evaluated; always NULL for `DIRECT_AUTHORIZATION` | Frozen task identity; `CLAIMED` requires it |
| `task.businessType` | text | NULL unless a task exists; always NULL for `DIRECT_AUTHORIZATION` | Frozen task business type |
| `task.calleeNumber` | text | NULL in the redacted snapshot; NOT NULL only inside the encrypted internal payload when `action = DIAL_TASK_CALLEE` | Task-path callee only; never used for direct authorization |
| `task.calleeMasked` | text | NULL unless a task exists; always NULL for `DIRECT_AUTHORIZATION` | Safe display/log value |
| `authorization.calleeNumber` | text | NULL except a successful `DIRECT_AUTHORIZATION`, and then only inside the encrypted internal payload | Confirmed direct callee; never in task fields, the redacted snapshot, logs, audit or customer responses |
| `reservationId` | uuid | NOT NULL iff `action = DIAL_TASK_CALLEE` or (`purpose = DIRECT_AUTHORIZATION` and `action` is NULL); otherwise NULL | Committed reservation only; a direct refusal has none |
| `authorizedRecordingSeconds` | integer > 0 | NOT NULL iff a reservation exists | Committed `usage_reservation.authorized_units` under the baseline 1 unit = 1 second conversion |
| `authorizationExpiresAt` | timestamptz | NOT NULL iff a reservation exists | Copied from the committed reservation; last instant to start the B-leg; replay never renews |
| `decisionAt` | timestamptz | NOT NULL | Decision commit time; never part of a fingerprint |

Action-specific consistency rules:

- `DIAL_TASK_CALLEE`: `taskMatch = CLAIMED`; `taskId`, `businessType`, `reservationId`, `authorizedRecordingSeconds` and `authorizationExpiresAt` are non-null; the encrypted internal payload contains the full `calleeNumber` under `task.calleeNumber`. The response is returned only after the task claim, reservation and decision commit in one transaction. A technical failure rolls all three back; an expected claim/reservation denial yields a frozen no-dial decision (category 2) with no `DIAL_TASK_CALLEE`. The single balance selected for the reservation is the same balance whose available units produced `availableRecordingSeconds`; a changed balance between the routing snapshot and the locked conditional debit is a typed reservation denial, never a different balance debit.
- Successful `DIRECT_AUTHORIZATION` (`action` null): `taskMatch = NONE`; task fields are null; the reservation fields are non-null; the encrypted payload contains the confirmed callee only under `authorization.calleeNumber`. It never carries a task ID, never uses task fields for the callee, never returns `ENTER_CALLEE` after the callee was entered/confirmed and is returned only after the reservation and decision commit.
- Direct refusal (`purpose = DIRECT_AUTHORIZATION`, action one of the four permitted non-dial actions): reservation fields and authorization object are null; no claim is made; the callee is not stored.
- `ENTER_CALLEE`, `GUIDE_PURCHASE`, `GUIDE_ACTIVATION`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS` on `INBOUND_RESOLVE`/`ACTIVATION_NEXT_ACTION`: reservation fields are null; no claim is made. `ENTER_CALLEE` is only the pre-input no-task instruction and requires a positive no-`PENDING`/`CLAIMED` task check.
- `availableRecordingSeconds` may be zero or positive for any action and never overrides authorized seconds.
- The full callee is never written to `response_snapshot`, logs, audit, Outbox or a customer response. The redacted snapshot and logs use `calleeMasked` and task/decision IDs only. A direct authorization's redacted projection uses a masked callee; `authorization.calleeNumber` exists only in the encrypted payload.

B-leg and replay rules: after a committed `DIAL_TASK_CALLEE` or successful `DIRECT_AUTHORIZATION`, the IVR must persist a unique `callId` B-leg operation in its existing local authoritative store before issuing any line request; that local durable operation, not business `ivr.call_leg`, proves outbound uniqueness. The executable local key/protocol and lost-response recovery remain deferred to the IVR protocol work unit (Q28), and no business schema expansion is added. The IVR must not start a new B-leg at or after `authorizationExpiresAt`; a response lost or an unknown line outcome is reconciled/recovered by `callId` without a second dial. The task path never prompts the caller to confirm the callee. A replay after the cutoff returns the same frozen payload and cutoff, never renews the authorization and never becomes redial permission; a B-leg started before the cutoff continues under the frozen authorized seconds.

### 10.15 S2 transaction outcome contract

One classification applies to every fresh decision path:

1. **Category 1 — refusal before accepted activation/trial.** A fresh `SUSPENDED` subject, an invalid/stale confirmation origin or content, an identity/session mismatch or the phase-A active/`UNKNOWN`-call guard rejects before any accepted consent, transition, trial/grant/balance or reservation. An authenticated fresh normal rejection persists only its stable rejected decision (with `IVR_CONFIRM` completion where the entry uses it), the required `call_record` parent (creating a terminal `REJECTED` current call when absent or finalizing its own never-started/rejectable `ACTIVE` row) and, only where the existing rejection branch provides for it — a fresh `SUSPENDED` or `REJECT_CALL_IN_PROGRESS` rejection — marks a not-yet-confirmed session `REJECTED`. It writes no accepted side effect. A current `UNKNOWN` call, an already-`CONFIRMED` session and any unrelated competing call are never destructively changed. An identity/authentication failure or a semantic/fingerprint conflict produces no accepted effect and no unauthorized persistent write and discards any `NEW` idempotency row (and uncommitted parent) created for the attempt.
2. **Category 2 — normal domain no-dial result after valid activation/trial.** Once accepted consent, activation and any first-trial outcome are validly established, a later shared-selection domain result — revoked task/workspace/binding/owner/creator access, `NO_AVAILABLE_ENTITLEMENT`, an absent/unapproved/ambiguous allocation policy, a stale/changed balance selection, task expiry/cancellation/replacement, a competing active/`UNKNOWN` call or an expected reservation denial — commits the accepted activation/trial facts, keeps the session `CONFIRMED`, commits exactly one frozen no-dial decision (and its completion where the entry uses `IVR_CONFIRM`) and commits no claim or reservation. A tentative claim/reservation is undone by `ROLLBACK TO SAVEPOINT` at the same-connection savepoint established after the activation/trial writes and before the tentative claim; the no-dial decision and completion are then written after the rollback and committed. There is no autonomous transaction, independent service or independently committed subtransaction. For an initial-resolve or direct-authorization attempt there are no activation/trial facts to preserve, but the same savepoint/no-dial freeze rule applies to an expected claim/reservation denial; the session/call identity created before the savepoint survives a category-2 rollback, and a technical failure still rolls back the whole transaction.
3. **Category 3 — technical transaction failure.** A SQL error, lock/advisory timeout, storage I/O error, decision/completion/Outbox write failure or any other unexpected runtime error rolls back every write in the local transaction, including the `NEW` idempotency row, consent/activation/trial/grant/balance, any tentative claim/reservation and the decision/Outbox. No decision commits, no completion commits, and the caller receives a bounded retryable error. A database exception is never swallowed and a rollback-only transaction is never used to commit.

A non-technical domain refusal never rolls back valid activation/trial, and a technical failure never commits them. Pre-confirmation guards run before consent/activation/trial; the shared fresh selection runs after successful activation/trial in the same locked transaction. The current-caller, subject-lifecycle, access and task locks are held across both phases, so an access revocation either commits before the confirmation acquires those locks or after the confirmation commits; tests assert the two serial orders in §13.7 and never an interleaving while the same locks are already held. The only committed outcomes are a category-1 rejected decision (plus a permitted terminal current call), a category-2 frozen no-dial decision with valid activation/trial where applicable, or a successful frozen decision with its optional claim/reservation and exact completion. A replay returns an already committed decision; category 3 commits nothing.

### 10.16 S2 local immutability guard triggers

The following `BEFORE UPDATE` trigger functions are local structural controls owned by the schema owner, `SECURITY INVOKER`, with no transaction control and no function-wave entry. They complement the column grants; they are not callable cross-schema service APIs.

| Trigger | Guard predicate |
| --- | --- |
| `catalog_order.reject_row_mutation` | Reject update/delete on the append-only catalog product, benefit line and payment event tables. |
| `catalog_order.guard_catalog_version_published` | Reject a change to published commercial columns, `snapshot`, `snapshot_sha256`, `created_by_employee_id`, `created_at` or `published_at` once `published_at` is set; allow `sale_status`/`revision`. |
| `catalog_order.guard_catalog_offer` | Reject changes to `catalog_version_id` or `channel`. |
| `catalog_order.guard_customer_order_snapshot` | Reject changes to every immutable commercial/consent/fingerprint column; allow only payment status/terminal time/revision. |
| `catalog_order.guard_payment_order_identity` | Reject changes to `order_id`, `provider`, `merchant_order_no`, `amount_cents`, `currency` or `created_at`. |
| `ivr.guard_outbound_call_task` | Reject changes to task identity/callee/ttl/expiry/idempotency columns; reject a terminal → non-terminal transition; allow `claimed_call_id` only from NULL to one existing call in the reciprocal claim transaction and reject any later change, clear or task switch. |
| `ivr.guard_inbound_session` | Reject changes to `call_id`, `trusted_caller`, caller/access/node/protocol/prompt fields or `created_at`. |
| `ivr.guard_call_record_parties` | Reject changes to `call_id`, `session_id`, party ciphertext/masked values, `access_number_masked`, `node_id`, `protocol_version` or `started_at`; allow `task_id` only from NULL to the `id` of the task whose `claimed_call_id` equals this call, exactly once, and reject any later change, clear, arbitrary relink or different task. |
| `ivr.guard_call_leg` | Reject changes to `call_id`, `leg_id`, `leg_type`, `direction` or `started_at`. |
| `ivr.reject_row_mutation` | Reject update/delete on append-only `ivr_decision`. |
| `entitlement_metering.guard_trial_policy_version` | Reject changes to campaign/version/granted seconds/valid days/snapshot/publisher/`published_at` after publication; while `OLD.published_at IS NULL`, allow the one owner publication `UPDATE` that sets `published_by_employee_id` to the actual authenticated publisher, `published_at` once from NULL, `enabled` and `revision` together, and reject any later change to publisher identity or content. The guard admits that single statement and does not require a separate pre-publication publisher update. |
| `entitlement_metering.guard_entitlement_grant` | Reject any update/delete; the grant is append-only. |
| `entitlement_metering.guard_trial_grant` | Reject any update/delete; only `issue_first_trial` inserts. |
| `entitlement_metering.guard_entitlement_balance` | Reject changes to `grant_id` or `granted_units`. |
| `entitlement_metering.guard_entitlement_issuance` | Reject changes to `order_id`, `installment_no`, `entitlement_type` or source event columns. |
| `entitlement_metering.guard_usage_reservation` | Reject changes to `call_id`, `balance_id`, `subject_id`, `authorized_units`, `authorization_expires_at` or `created_at`. |
| `entitlement_metering.reject_row_mutation` | Reject update/delete on append-only `usage_fact`. |
| `activation_consent.reject_row_mutation` | Reject update/delete on append-only `consent_record` and `service_activation_transition`. |
| `activation_consent.guard_service_activation` | Reject changes to `id`, `subject_id` or `created_at`; allow status/revision/time through the owning service. |

The guard triggers are installed in the same V004-V007 wave as their tables. The migration grants `EXECUTE` on each guard function only to the owning runtime role; direct invocation is not a supported API. No guard trigger relaxes the append-only, revision, ownership or event-identity rules.

### 10.17 S2 semantic fingerprint and Outbox identity registry (F-11)

This is a bounded **persisted-data registry** for the semantic fingerprint and Outbox identity of each S2 fact. It fixes which persisted fields cause replay versus conflict and which fields identify an Outbox fact. It does not define a generic event/JSON framework, does not add a table, column or SQL function, and does not fix wire encoding or the final IVR JSON-schema version (Q15/Q26). Those remain with their named later owners.

Canonicalization rules:

- A fingerprint is the lowercase SHA-256 of the section 2.5 canonical JSON of the exact semantic field set below. The field list names the semantic set; the serializer sorts object keys by UTF-16 code units in the normal canonical way.
- Optional values are either an explicit JSON `null` (null-safe fields) or omitted when the documented field is absent. UUIDs are canonical lowercase 36-character text. Enums are the exact uppercase codes. Text is UTF-8 with no transport padding. Numbers are ECMAScript shortest round-trip JSON numbers.
- Every emitted persisted field must be readable with the declared owner/cross-module grants; an owning service builds its own payload from its own immutable rows. Request semantic inputs named below (for example `confirmationContent`, `enteredCalleeHmac`, `accessNumberHmac`) are validated inputs, not stored columns; `accessNumberHmac` is the ephemeral §2.5 HMAC of the authenticated canonical normalized access number and is never persisted, logged, audited, emitted, snapshotted or returned. If a persisted field cannot be read under the declared grants, the contract is invalid.
- Excluded from every semantic fingerprint: transport request IDs, network timestamps (`receivedAt`/`createdAt`/`updatedAt`/`decidedAt`/`occurredAt`/`acceptedAt`/`terminalAt`), generated identities other than the persisted fact identity named below, and every mutable balance, status, result, task, reservation or revision value.
- A repair or re-encryption that preserves the logical fields does not change the fingerprint. A changed callee is normalized to its canonical business-number form before hashing and is never persisted in the fingerprint input.
- For consent-driven activation scopes, the comparison basis is the persisted immutable consent fingerprint plus request semantic inputs; a newly generated consent ID, transition ID or current revision/status is never a fingerprint input (R-08).

| Fingerprint scope | Exact semantic fields (null-safe where the column is nullable) | Exclusions and replay rule |
| --- | --- | --- |
| `IVR_CONFIRM` event and `ACTIVATION_NEXT_ACTION` decision | `purpose`, `protocolVersion`, `callId`, `sessionId`, `callerNumberHmac`, `accessNumberHmac`, `subjectId` (null-safe), `confirmedSubjectId` (null-safe), `callerBindingId` (null-safe), `nodeId`, `promptVersion`, `policyCode`, `policyVersion`, `confirmationKey`, `confirmationContent` | Excludes `eventId`, `requestId`, decision/idempotency result IDs, all timestamps and every mutable balance/activation/task/status value. The exact same semantics, including a regenerated transport event ID, produces the same fingerprint and REPLAYS the first decision; the same event ID or a new event ID with any changed field conflicts. A binding replacement, revocation or rebinding changes `callerBindingId`/`subjectId`/`confirmedSubjectId` and therefore conflicts before the frozen decision is returned; a stale decision is never reused. A different canonical access number yields a different `accessNumberHmac` and conflicts even when the display-only `accessNumberMasked` is identical; missing or invalid HMAC input fails closed before replay. |
| `INBOUND_RESOLVE` decision | `purpose`, `protocolVersion`, `callId`, `sessionId`, `callerNumberHmac`, `accessNumberHmac`, `subjectId` (null-safe), `callerBindingId` (null-safe), `entryMode`, `nodeId` | Excludes `requestId`, transport IDs, task/balance/status, decision IDs and all timestamps. The exact same call/session/binding/access identity and fingerprint replays the frozen decision before task housekeeping; a replacement, revocation or rebinding of the captured binding changes `callerBindingId`/`subjectId` and is a stable conflict before the frozen decision is returned, never a stale replay. A different canonical access number conflicts through `accessNumberHmac` even when the display-only mask is identical; missing or invalid HMAC input fails closed before replay. |
| `DIRECT_AUTHORIZATION` decision | `purpose`, `protocolVersion`, `callId`, `sessionId`, `callerNumberHmac`, `accessNumberHmac`, `promptVersion`, `enteredCalleeHmac` (server-computed from the normalized entered callee; the normalized value is never persisted), `confirmationKey`, `confirmationContent` | Excludes the transport confirmation event ID (`confirmationEventId`), task/balance/status, reservation/authorization results and all timestamps. A regenerated transport event ID with identical semantics produces the same fingerprint and REPLAYS the first authorization; a changed session, protocol version, access identity, prompt, entered callee, confirmation key or content conflicts. The entered callee exists only as its HMAC in the fingerprint input and is never stored in plaintext. The access identity input is `accessNumberHmac`, so a different canonical access number conflicts even when the display-only mask is identical; missing or invalid HMAC input fails closed before replay. |
| `trial_grant` granting action — `IVR_CONFIRM` | `callId`, `sessionId`, `confirmedSubjectId`, `policyVersionId`, `policyCode`, `policyVersion`, `promptVersion`, `sourceActionDomain`, `sourceActionEventId` | Excludes granted-at/expiry (derived from the policy and database time), generated grant/balance IDs and mutable status. Same domain/event and same fingerprint replays the original IDs; same event and changed fingerprint is `TRIAL_ACTION_CONFLICT`; a different event for an already-granted subject is `ALREADY_GRANTED`. |
| `trial_grant` granting action — `ACTIVATION` | `accountId`, `activationEventId`, `channel`, `consentRequestFingerprint`, `policyVersionId`, `sourceActionDomain`, `subjectId` | Same replay/conflict/`ALREADY_GRANTED` rules as the IVR-confirm scope; the immutable consent fingerprint replaces the generated consent ID. |
| `PURCHASE` order | `accountId`, `amountCents`, `catalogOfferId`, `catalogVersionId`, `channel`, `currency`, `ownerOrganizationId`, `purchaseConsentEventId` (the derived 81-character identity), `subjectId`, `versionToken` | Excludes the idempotency key (it is the platform key), generated order/consent IDs, `createdAt`, payment state, payment evidence, issuance state and the snapshot hash. Identical retry returns the first order; any changed purchase semantic field conflicts. |
| `PURCHASE` consent | `accountId`, `action`, `channel`, `eventDomain`, `eventId`, `policyCode`, `policyVersion`, `promptVersion`, `purpose`, `sourceType`, `subjectId` | `eventId` is the derived 81-character `purchaseConsentEventId`, which is also persisted as `source_ref`; it is not a separate caller-chosen value. Excludes `acceptedAt`, generated consent ID, snapshot hash and mutable current policy state. Same `(eventDomain,eventId)` and same fingerprint returns the first consent ID; changed fields conflict; a different event is a distinct consent unless the order replay already returned the first order. |
| `SERVICE_ACTIVATION` consent (IVR, `MINIAPP`, `WEB`) | `accountId` (null-safe; null for IVR), `action`, `channel`, `eventDomain`, `policyCode`, `policyVersion`, `promptVersion`, `purpose`, `sourceRef`, `sourceType`, `subjectId` | `sourceRef` is exact and transport-independent: `ivr-session:` + the canonical lowercase session UUID (48 characters) for IVR; `activation:` + the lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, activationEventId}` (75 characters) for `MINIAPP`/`WEB`. The IVR row uses `eventDomain = 'IVR_CONFIRM'` and `event_id` equal to the already validated nonblank 1-128 confirmation `eventId`, while `sourceRef` stays the 48-character `ivr-session:` value independent of that transport ID. Excludes the transport `eventId`, `acceptedAt`, generated consent ID, snapshot hash and current revision/status. The same semantic fields with a regenerated transport event ID fingerprint identically (the IVR decision fingerprint also excludes `eventId` and returns the first result before any append, so the original consent `event_id` is never rewritten and no second consent is added); any changed policy/prompt/source/action field is a stable conflict, never a replay. |
| Task creation | `businessType`, `calleeNumberNormalized` (ephemeral input, hashed only), `callerBindingId`, `callerNumberHmac`, `createdByAccountId`, `ownerAccountId`, `ownerOrganizationId`, `subjectId` | Excludes the client idempotency key (part of `(created_by_account_id,idempotency_key)`), task protocol ID, TTL/expiry, status and timestamps. Same key/input returns the existing task without TTL refresh; changed input conflicts. |
| Call event | `callId`, `eventId`, `eventType`, `legId`, `payloadSha256`, `sequenceNo` | `payloadSha256` is the SHA-256 of the §4.27 canonical `payload_redacted.source.payload` only; the semantic inputs and digest are hashed before the `receipt` metadata is added, and `receipt` (`payloadSha256`, `eventFingerprint`) is server-written metadata excluded from the hash and fingerprint input. Excludes `receivedAt`, `sequence_no` when absent (null-safe), processing status/reason, revision and authoritative result. The original values and fingerprint are stored explicitly in the immutable `payload_redacted.source`/`receipt`; replay recomputes from them even when the indexed `sequence_no` is `NULL` for a sequence conflict. Only a same-semantic duplicate with the same stored fingerprint returns the first terminal processing outcome and mutates nothing; a changed payload/type/sequence always returns a stable conflict with no mutation, even when the first outcome was `SEQUENCE_CONFLICT`, and never overwrites the first outcome or applies as success. |
| Activation transition | `accountId`, `actorEmployeeId`, `channel`, `consentRequestFingerprint`, `reasonCode`, `sourceActionDomain`, `sourceActionEventId`, `statusEventDomain`, `statusEventId`, `subjectId`, `toStatus` | Excludes generated transition ID, generated consent ID, `fromRevision`, `fromStatus` and `occurredAt`; it binds the immutable `SERVICE_ACTIVATION` consent fingerprint. Same action event plus same fingerprint replays the first transition; changed input conflicts; a different stable action event at the current revision is a distinct transition or a stable current-state conflict. |
| Payment event processing | `amountCents`, `eventType`, `paymentOrderId`, `payloadSha256`, `provider`, `providerEventId`, `signatureVerified`, `transactionId` | Excludes `occurredAt`/`receivedAt`, processing status and any payment result. Same provider event with the same fingerprint returns the first processing result; changed content is quarantined, never applied. |
| Usage settlement/release | `callId`, `reservationId`, `sourceEventDomain`, `sourceEventId`, `units`, `usageType` | Excludes `created_at`, terminal status, balance values and snapshot hash. The unique source event and reservation/type keys make a duplicate converge to the first outcome; `units = 0` is the known-zero-use release outcome only when the authoritative outcome is known. |

Stable source identities (OQ-1). `purchaseConsentEventId` is not a freshly generated identity and has no caller-chosen variant. The order service derives it as `purchase-consent:` + the lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, clientIdempotencyKey}`, using the authenticated canonical lowercase 36-character account UUID and the exact client idempotency key (UTF-8 code points, no Unicode normalization or case folding). The result is always 81 characters and is used as both the consent `event_id` and the purchase consent `source_ref`. A retry of the same order key and semantic input reproduces the same identity and fingerprint; a fresh random per-attempt ID, truncation, alternative derivation or schema expansion is forbidden. Changed semantic purchase data still conflicts under the existing order/consent fingerprints. The order client key remains 1-475 characters and the platform key 1-512; 74/75/128/475-character keys are accepted, a 476-character key is rejected before any effect, and same-key replay returns the first result. The exact wire field name and encoding remain the order OpenAPI owner's work (Q15/Q26), not this schema contract.

Outbox identity registry. Every row below is appended through `platform.outbox_append` in the domain transaction. `dedupe_key` is derived only from persisted immutable identities; `payload_sha256` hashes the canonical redacted payload fields listed. A replay of the persisted fact computes the same domain/dedupe key and hash, so the function returns the existing Outbox ID and appends no duplicate event. A different hash under the same key is `OUTBOX_DEDUPE_CONFLICT`, never a second event.

| Event domain | Dedupe key | Aggregate type / ID | Event type | Exact redacted payload fields | Produced by |
| --- | --- | --- | --- | --- | --- |
| `ACTIVATION` | `trial_grant:<trial_grant_id>` | `TRIAL_GRANT` / `trial_grant_id` | `FIRST_TRIAL_GRANTED` | `trialGrantId`, `entitlementGrantId`, `entitlementBalanceId`, `subjectId`, `campaignKey`, `trialPolicyVersionId`, `grantedUnits`, `validFrom`, `expiresAt`, `grantedAt` | `issue_first_trial` |
| `ACTIVATION_STATUS` | `activation_transition:<transition_id>` | `SERVICE_ACTIVATION` / `activation_id` | `ACTIVATION_TRANSITION_APPLIED` | `activationId`, `transitionId`, `subjectId`, `fromStatus`, `toStatus`, `fromRevision`, `toRevision`, `sourceChannel`, `sourceActionDomain`, `sourceActionEventId`, `statusEventDomain`, `statusEventId`, `occurredAt` | activation-consent owner service |
| `PURCHASE` | `consent_record:<consent_id>` | `CONSENT_RECORD` / `consent_id` | `PURCHASE_CONSENT_RECORDED` | `consentId`, `subjectId`, `accountId`, `channel`, `purpose`, `eventId`, `policyCode`, `policyVersion`, `promptVersion`, `action`, `snapshotSha256`, `acceptedAt` | purchase-consent owner service |
| `PURCHASE` | `customer_order:<order_id>` | `CUSTOMER_ORDER` / `order_id` | `ORDER_CREATED` | `orderId`, `orderNo`, `accountId`, `ownerOrganizationId`, `subjectId`, `channel`, `catalogVersionId`, `catalogOfferId`, `purchaseConsentId`, `amountCents`, `currency`, `versionToken`, `snapshotSha256`, `createdAt` | catalog-order order service |
| `PAYMENT` | `payment_event:<payment_event_id>` | `PAYMENT_ORDER` / `payment_order_id` | `PAYMENT_EVENT_APPLIED` | `paymentEventId`, `paymentOrderId`, `orderId`, `provider`, `providerEventId`, `eventType`, `transactionId`, `amountCents`, `signatureVerified`, `occurredAt` | catalog-order payment service |
| `PAYMENT` | `entitlement_issuance:<issuance_id>` | `ENTITLEMENT_ISSUANCE` / `issuance_id` | `ENTITLEMENT_ISSUED` | `issuanceId`, `orderId`, `installmentNo`, `entitlementType`, `grantId`, `sourceEventDomain`, `sourceEventId`, `issuedAt` | entitlement-metering issuance service |
| `IVR_CONFIRM` | `ivr_decision:<decision_id>` | `IVR_DECISION` / `decision_id` | `IVR_CONFIRMATION_DECIDED` | `decisionId`, `callId`, `purpose`, `action`, `reasonCode`, `reservationId`, `taskId`, `authorizedRecordingSeconds`, `authorizationExpiresAt` | `ivr.select_call_action` |
| `IVR_DIRECT_AUTH` | `ivr_decision:<decision_id>` | `IVR_DECISION` / `decision_id` | `IVR_DIRECT_AUTHORIZATION_DECIDED` | `decisionId`, `callId`, `action`, `reasonCode`, `reservationId`, `authorizedRecordingSeconds`, `authorizationExpiresAt` | `ivr.select_call_action` |
| `IVR_EVENT` | `outbound_task:<task_id>:created` | `OUTBOUND_CALL_TASK` / `task_id` | `TASK_CREATED` | `taskId`, `callerBindingId`, `subjectId`, `ownerAccountId`, `ownerOrganizationId`, `createdByAccountId`, `businessType`, `expiresAt`, `createdAt` | task create service |
| `IVR_EVENT` | `outbound_task:<task_id>:replaced:<new_task_id>` | `OUTBOUND_CALL_TASK` / `task_id` | `TASK_REPLACED` | `taskId`, `newTaskId`, `subjectId`, `ownerAccountId`, `ownerOrganizationId`, `reasonCode`, `terminalAt` | task replace service |
| `IVR_EVENT` | `outbound_task:<task_id>:cancelled` | `OUTBOUND_CALL_TASK` / `task_id` | `TASK_CANCELLED` | `taskId`, `subjectId`, `ownerAccountId`, `ownerOrganizationId`, `reasonCode`, `terminalAt` | task cancel service |
| `IVR_EVENT` | `outbound_task:<task_id>:claimed:<call_id>` | `OUTBOUND_CALL_TASK` / `task_id` | `TASK_CLAIMED` | `taskId`, `callId`, `subjectId`, `ownerAccountId`, `ownerOrganizationId`, `claimedAt`, `reservationId` | `ivr.select_call_action` |
| `IVR_EVENT` | `call_event:<event_id>` | `CALL_RECORD` / `call_record_id` | `CALL_EVENT_APPLIED` | `callEventId`, `callId`, `eventId`, `eventType`, `sequenceNo`, `occurredAt`, `authoritativeSeconds` | call-event owner service |
| `IVR_EVENT` | `usage_reservation:<reservation_id>:settled` or `...:released` | `USAGE_RESERVATION` / `reservation_id` | `USAGE_SETTLED` or `USAGE_RELEASED` | `reservationId`, `callId`, `balanceId`, `subjectId`, `authorizedUnits`, `settledUnits`, `sourceEventDomain`, `sourceEventId`, `terminalStatus`, `terminalAt` | entitlement-metering settlement service |
| `OPERATIONS` | `catalog_version:<version_id>:published` or `...:offsale` | `CATALOG_VERSION` / `catalog_version_id` | `CATALOG_VERSION_PUBLISHED` or `CATALOG_VERSION_OFFSALE` | `catalogVersionId`, `productId`, `versionNo`, `saleStatus`, `publishedAt`, `snapshotSha256` | catalog-order publication service |
| `OPERATIONS` | `trial_policy_version:<id>:published` or `...:disabled` | `TRIAL_POLICY_VERSION` / `trial_policy_version_id` | `TRIAL_POLICY_PUBLISHED` or `TRIAL_POLICY_DISABLED` | `trialPolicyVersionId`, `campaignKey`, `versionNo`, `grantedSeconds`, `validDays`, `enabled`, `publishedAt`, `snapshotSha256` | entitlement-metering trial-policy service |

Activation event identities are deterministic and exact: for a client/IVR transition, `source_action_event_id` is the validated channel action event (the confirmation `eventId` or the client activation `eventId`), validated nonblank 1-128 characters before any effect, and `status_event_id` is `'activation-status:' || lower(activation_id) || ':' || to_revision` with canonical lowercase UUID text and decimal revision. The transition's own generated `id` is used only for its Outbox dedupe key (`activation_transition:<transition_id>`). This gives every `ACTIVATION_STATUS` fact one exact identity per activation revision without a new column, table or function. An `OPERATIONS` transition uses its own stable operations action event and the same status-event derivation.

An S2 caller may use only these identities for the listed facts. A new fact with the same domain/dedupe key but a different hash is a conflict, not a second event. The wire encoding, consumer JSON schema version and transport envelope remain Q15/Q26.

### 10.18 Balance selection, available-seconds projection and authorized units (F-04)

This section defines the single eligible-balance selector used by both the IVR routing snapshot and the reservation service. It does not approve any allocation product rule. Q5/Q25 do not approve trial-first, FIFO, stacking, offset order or any arbitrary balance ordering; this baseline must not invent one.

Inputs (all server-validated; the client cannot force a balance, policy or amount):

- `allocationPolicy`: a server-side **approved allocation-policy reference** `{policyCode, policyVersion}` selected from the owning module's approved configuration/registry. An absent, unknown, disabled or unapproved policy reference fails closed with typed `ALLOCATION_POLICY_UNAVAILABLE`. A client-supplied policy token is accepted only if the approved IVR protocol later defines it (Q15/Q26) and it still resolves to an approved reference; a token cannot name an arbitrary policy.
- `requestedUnits`: the positive requested recording seconds from the validated approved IVR/request input, when that input exists.
- `perCallCapUnits`: the positive approved per-call cap from the approved IVR/entitlement configuration.
- At least one of `requestedUnits` or `perCallCapUnits` must be present and valid; otherwise the service fails closed with typed `AUTHORIZATION_INPUT_UNAVAILABLE`. No default duration is invented. `authorization_expires_at` is a separate approved/caller-supplied window input (Q25) and is frozen once; it is not derived from either unit input.
- The owning Product/Finance/Entitlement and IVR/Backend later owners approve the policy code/version and the two numeric inputs; no production value is fixed here.

Eligible balance predicate. For a reservation with subject `s`, after the §9.0.1 locks and under the balance/grant snapshot at database time `t = clock_timestamp()`:

```
balance.status = 'AVAILABLE'
grant.subject_id = s
grant.entitlement_type = 'RECORDING_SECONDS'
grant.valid_from <= t AND t < grant.expires_at
balance.granted_units = grant.granted_units
balance.granted_units - balance.reserved_units - balance.consumed_units > 0
```

`availableUnits(balance) = balance.granted_units - balance.reserved_units - balance.consumed_units`. The selector reads at most one row per grant and never sums across balances. A future or expired grant is ineligible even when it has unused units (docs/12 §9.1). `EXHAUSTED` remains the consumed-equals-granted marker; a fully reserved but unconsumed balance is not `EXHAUSTED` and has `availableUnits = 0`. The IVR routing projection evaluates this predicate from its exact §10.12 columns (`entitlement_balance.granted_units/reserved_units/consumed_units/status/grant_id` plus `entitlement_grant.subject_id/entitlement_type/granted_units/valid_from/expires_at`); `entitlement_grant.granted_units` is granted to `ivr_role` so the declared equality is evaluable (OQ-2). The owning reservation service additionally verifies `balance.granted_units = grant.granted_units` and the immutable grant identity under its own locks. No role reads an undeclared column.

Selection mechanics:

1. Read all eligible balances for the subject under the canonical-caller/subject locks using the exact §10.12 columns.
2. The approved policy must resolve deterministically to exactly one eligible balance:
   - the baseline-validated mechanical form `SOLE_ELIGIBLE` selects only when exactly one eligible balance exists;
   - a future approved policy may return one `balanceId` that must belong to the eligible set; returning a non-eligible ID is typed `ALLOCATION_POLICY_INVALID`.
   - zero eligible balances is typed `NO_AVAILABLE_ENTITLEMENT` (a no-entitlement denial, not a policy error);
   - more than one candidate, or a policy that would require trial-first/FIFO/stacking/expiry ordering, is typed `ALLOCATION_AMBIGUOUS` or `ALLOCATION_POLICY_UNAVAILABLE`; the service does not choose by row order, UUID, expiry or any other hidden tie-break.
3. Every absent/unapproved/ambiguous policy or non-selected candidate ends the attempt with **no task claim and no reservation**, and produces a typed domain denial for the category-2 no-dial path. The redacted frozen decision maps `NO_AVAILABLE_ENTITLEMENT` to `GUIDE_PURCHASE`/`NO_AVAILABLE_ENTITLEMENT`; a policy/integrity denial maps to `REJECT`/`DATA_INTEGRITY`, with the typed allocation code retained only in the encrypted internal payload and audit, never in the redacted snapshot or as a new reason registry value.

Authorized units:

```
authorizedUnits = min(present requestedUnits, present perCallCapUnits, availableUnits(selectedBalance))
```

At least one approved bound is present; the result must be a positive integer. If it is `0` or below, the typed denial is `NO_AVAILABLE_ENTITLEMENT`; if a bound input is present but invalid/non-positive, the typed denial is `AUTHORIZATION_INPUT_UNAVAILABLE`. `authorizedUnits` is frozen into `usage_reservation.authorized_units`; `authorizedRecordingSeconds` is its explicit 1:1 conversion. No demo or default duration is used.

Conditional debit and routing consistency:

- The reservation service locks the selected balance row, re-reads the grant validity and the exact predicate, and conditionally updates:
  `SET reserved_units = reserved_units + :authorizedUnits, revision = revision + 1, updated_at = clock_timestamp() WHERE id = :selectedBalanceId AND status = 'AVAILABLE' AND revision = :expectedRevision AND granted_units - reserved_units - consumed_units >= :authorizedUnits AND granted_units = :immutableGrantUnits`.
  A zero-row update performs one bounded reread and returns typed `BALANCE_SELECTION_STALE`/`NO_AVAILABLE_ENTITLEMENT`; it never debits a different balance or commits a partial claim. The subject/type/validity predicates are repeated in the same locked transaction; a balance whose `grant.subject_id` differs from the reservation subject is `BALANCE_SUBJECT_MISMATCH` and is never debited.
- The routing snapshot and the reservation service call the same selector with the same approved policy reference. `availableRecordingSeconds` is the selected balance's available units at the routing snapshot, not a sum and not an authorization. The reservation re-evaluates under the balance row lock; if availability changed, the typed denial follows the category-2 path (no dial, no claim, no reservation) and the routing snapshot is never treated as a spend authorization.
- Expiry is evaluated as a predicate; the read/selection path does not mutate `status`. The owning service may conditionally mark a balance `EXPIRED` only while holding its row lock and only when no live reservation or unknown use depends on it; marking never releases units. It never marks a balance `EXPIRED` merely because the current time passed while a call or reconciliation is active, and it never marks a fully consumed balance `EXPIRED`: `EXHAUSTED` (consumed = granted) remains the derived status required by `ck_entitlement_balance_status_shape`.
- There is no cross-subject balance id, no caller-supplied balance id, no partial multi-balance debit and no hidden retry against another balance.

### 10.19 Frozen typed commerce and schedule snapshots (F-12)

Order/benefit snapshot (version 1). `catalog_order.catalog_version.snapshot` stores the version/product/benefit/schedule/terms fields; `catalog_order.customer_order.snapshot` copies those immutable fields and adds the chosen `catalogOfferId`, `channel` and `versionToken`. The example below is the order snapshot; the catalog-version snapshot omits only the offer-specific fields it does not own.

```json
{
  "schemaVersion": 1,
  "catalogVersionId": "<uuid>",
  "catalogOfferId": "<uuid>",
  "channel": "<MINIAPP|WEB>",
  "versionToken": "<exact accepted server token>",
  "productCode": "<approved stable code>",
  "versionNo": 1,
  "offerType": "<SUBSCRIPTION|ONE_TIME|ADD_ON|QUOTE>",
  "audience": "<PERSONAL|ENTERPRISE>",
  "priceCents": "<approved integer cents or null for QUOTE>",
  "currency": "<approved ISO-4217>",
  "billingCycle": "<approved cycle>",
  "installments": "<approved positive count>",
  "benefitLines": [
    {
      "lineKey": "<catalog_benefit_line.id>",
      "entitlementType": "<registry type>",
      "quantity": "<approved positive units per installment>",
      "validityDays": "<approved positive days>",
      "grantIntervalMonths": "<approved non-negative months>",
      "schedule": [
        {
          "installmentNo": 1,
          "entitlementType": "<same registry type>",
          "units": "<approved positive units>",
          "validFrom": "<approved absolute timestamptz>",
          "expiresAt": "<approved absolute timestamptz>"
        }
      ]
    }
  ],
  "terms": "<approved immutable commercial terms object>"
}
```

Rules:

- `lineKey` is the immutable `catalog_benefit_line.id`; there is exactly one object per benefit line and one schedule entry per `installmentNo` in `1..catalog_version.installments` for that type.
- A schedule entry's `entitlementType` equals its line; `units` equals the approved line `quantity`; `validFrom`/`expiresAt` are **copied from the validated approved schedule input** captured for that version. The order service never computes due dates, grace periods or calendar arithmetic (Q20), never reconstructs a schedule from current catalog state, and never invents a date, price or quantity. If the approved schedule for a line/installment is missing, ambiguous or inconsistent, publication fails closed and the line is not sellable; an unapproved or missing timing input at issuance does not falsify the payment fact and does not issue a guessed grant.
- Publication validates: `schemaVersion = 1`; exact object/array types; `catalogVersionId`/`catalogOfferId`/`channel`/`versionToken` match the exact immutable version and offer used for the order; `installments >= 1`; line count/keys/types match the immutable benefit lines; each schedule covers exactly `1..installments`; each window satisfies `expiresAt > validFrom`; units/quantity/validity are positive; money is integer cents; no protected value. The canonical redacted snapshot hash is computed after validation. The snapshot is immutable after publication and is never recomputed from a later catalog change.
- The approved schedule input is an approved catalog/commercial input owned by Product/Finance/Operations (Q4, Q20). The registry does not approve a due-date trigger, grace/retry rule or calendar arithmetic. The validated input may be supplied as immutable absolute windows; any later approved payment-relative rule must be explicitly approved before it can replace them.
- Issuance consumes the frozen snapshot, not the current catalog row. The issuance service receives a validated approved `installmentNo` (the due-installment input; Q20) and requires one matching schedule entry for each benefit line/type. Missing/ambiguous timing, a non-matching installment, a snapshot/line mismatch, a window already wholly in the past at issuance (`expires_at <= clock_timestamp()`) or a payment event that cannot be tied to a verified paid fact leaves the payment fact unchanged and records a stable issuance `FAILED`/`last_error_code` with no grant and no guessed quantity/date; retry is allowed once the approved input is valid.
- Trial dates use one explicitly supported approved validity-unit calculation with a clear database-time source: `granted_at` is captured once from `clock_timestamp()` in the granting transaction; `valid_from = granted_at`; `expires_at = granted_at + (valid_days * interval '1 day')` in UTC. No month arithmetic, business-calendar or grace rule is inferred. A later approved policy that requires another arithmetic rule needs an explicit approved rule; the policy's `valid_days` remains the approved positive input (Q4/Q10).
- The snapshot registry is versioned (`schemaVersion`), so a later approved wire/JSON schema (Q15/Q26) can add a new version without rewriting historical snapshots. This document writes version 1 only.

### 10.20 Additional S2 published application-service contracts (F-05, F-07, F-08, F-09, OQ-1)

These are application services on one connection and one local transaction; they are not SQL functions and add no function-wave row. Cross-owner calls use `Propagation.MANDATORY` under the owning runtime role and never commit or roll back independently. Each service authenticates its caller, validates current access/grants/lifecycle, and re-reads after locks; no caller-supplied role, owner, amount, balance, policy or result is trusted.

| Service | Owner | Called by | Exact contract |
| --- | --- | --- | --- |
| `activation-consent.lock_service_activation` | activation-consent | IVR confirmation and order creation on the same connection, `Propagation.MANDATORY`; the owner's MINIAPP/WEB client-activation service takes the same rank-4 row lock directly | Rank-4 lock-only collaborator. Locks an existing `service_activation` row for the subject under `activation_consent_role` and returns its current facts; a missing row is serialized by the already-held subject lifecycle advisory and locks nothing. It creates, updates, appends and enqueues nothing, takes no foreign row lock and adds no SQL function. Later owner services (`record_activation_confirmation`, `record_client_activation`, `append_purchase_consent`) reuse the held lock and never acquire rank 4 after the idempotency row. |
| `subject-number.find_or_create_ivr_subject` | subject-number | IVR initial resolve/direct attempt on the same connection, `Propagation.MANDATORY` | Inputs: trusted normalized canonical number, `number_type`, server-computed `number_hmac`, ciphertext/masked value, `source = 'IVR'`, database time. Runs only after the authenticated immutable replay checks and under the canonical-number advisory lock, before subject/session/call/activation locks. Reads the unique `ACTIVE` binding for `number_hmac`: if one exists, returns its `subject_id`; an active binding whose subject later fails an activation/consent/access predicate still returns the stable subject identity, and the later ordered predicates make the decision. A `DISABLED`/`RETIRED` binding is never treated as active. If none exists, atomically inserts one `business_subject` with both owner columns null, `source = 'IVR'`, real-name `UNVERIFIED`, and one `caller_number_binding` with `status = 'ACTIVE'`, `verified_at = clock_timestamp()` (trusted-line observation), using the same transaction. It never creates a customer account, `auth_identity`, owner, organization claim or real-name fact. A `DISABLED` binding for the number is not revived and returns stable `IVR_NUMBER_BINDING_DISABLED`; a `RETIRED` history row does not block a new active binding for the newly resolved subject; more than one active binding is `IVR_NUMBER_BINDING_CONFLICT` and fails closed. A unique loser rolls back the whole resolve and retries bounded from the start; after the winner commits it reuses the winner's subject and never creates a second active binding. A category-3 failure rolls back the tentative subject/binding/session/call; a normal refusal decision may commit the stable subject/binding identity with the refusal because it is not activation, trial or consent. `ALREADY_GRANTED`/`REPLAY`/`ACTIVE` existing bindings are reused without writes. No enterprise ownership policy is invented. |
| `activation-consent.record_client_activation` | activation-consent | MINIAPP/WEB activation entry with `Propagation.MANDATORY` | Inputs: authenticated active account, server-derived workspace/personal context, subject with current access, exactly one explicit `ACCEPT` for the current prompt/policy, channel `MINIAPP` or `WEB`, `source_type = 'ACCOUNT'`, `source_ref` = `activation:` + the lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, activationEventId}` (75 characters) from the authenticated canonical account and stable action ID, event domain `ACTIVATION`, the stable `eventId` validated nonblank 1-128 characters before any effect, and the exact §10.17 `SERVICE_ACTIVATION` consent fingerprint. Lock order: subject lifecycle advisory (3), its own existing `service_activation` row (4), organization advisory (7), membership pair advisory (8), account advisory (9). Re-reads `customer_account(id,status)`, `organization(id,status)`, `organization_membership(organization_id,account_id,role,status)` and `business_subject(id,owner_account_id,owner_organization_id,revision)` with the exact §10.12 grants; takes no foreign row lock. Replay: find the unique transition by `(ACTIVATION,eventId)` before any append; the transition fingerprint binds the immutable consent fingerprint, so same fingerprint returns the first consent/transition/activation result and changed policy/prompt/source input is a stable conflict. If activation is already `ACTIVE` and the same legitimate action replays, return the existing ids and append no second transition. Otherwise append one `SERVICE_ACTIVATION` consent row (purpose `SERVICE_ACTIVATION`, action `ACCEPT`), create/transition `service_activation` `INACTIVE → ACTIVE`, append exactly one transition (`to_revision = from_revision + 1`) with the §10.17 `source_action_event_id`/deterministic `status_event_id`, then call `entitlement-metering.issueFirstTrial` with `source_action_domain = 'ACTIVATION'`, the same `eventId` and the canonical granting-action fingerprint. Do not invent a second trial eligibility rule; the trial outcome is `GRANTED`/`REPLAY`/`ALREADY_GRANTED` from the V006 function. Missing/stale/declined consent returns a typed `CONSENT_REQUIRED`/`ACTIVATION_CONSENT_DECLINED` domain refusal with no accepted append; category 3 rolls back everything; no login identity is created by IVR and no new table/function is used. |
| `activation-consent.append_purchase_consent` | activation-consent | catalog-order order creation on the same connection, `Propagation.MANDATORY` | Validates the authenticated active account, subject with current access, current prompt/policy version/key/content, explicit `ACCEPT` only, channel `MINIAPP`/`WEB`, source `ACCOUNT`, purpose `PURCHASE`, event domain `PURCHASE`, the derived 81-character `purchaseConsentEventId` (OQ-1) used as both `event_id` and `source_ref`, and canonical consent fingerprint (§10.17). The identity is exactly `purchase-consent:` + the lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, clientIdempotencyKey}` from the authenticated canonical account and exact client key; the order client key remains 1-475 and no caller-chosen alternative exists. Reuses the rank-4 activation lock taken by the order entry and never acquires rank 4 after the idempotency row. Appends exactly one immutable `consent_record` with the redacted snapshot/hash and returns its ID. A missing/stale/mismatched/declined consent returns a typed `PURCHASE_CONSENT_REQUIRED`/`PURCHASE_CONSENT_STALE`/`PURCHASE_CONSENT_DECLINED` denial and appends no accepted row; a replay of the same event/fingerprint returns the first ID; changed input conflicts. The order service then validates in the same transaction: consent `purpose = PURCHASE`, `subject_id = order.subject_id`, `channel = order.channel`, `source_type = ACCOUNT`, `account_id = authenticated account`, current policy/prompt fields, `event_domain`, `event_id = source_ref = purchaseConsentEventId`, `action = ACCEPT`, and `request_fingerprint`; any mismatch fails the order with no order row. No new table/column/function. |
| `ivr.process_call_event` | ivr | IVR event endpoint; may call `entitlement-metering.settle_call_usage` on the same connection | Replay/conflict comparison first, from the immutable stored `payload_redacted` fingerprint inputs (original type/leg/sequence/hash). Validates the parent `call_id` before any insert: an unknown call returns stable `CALL_UNKNOWN` with no `call_event`/call/leg/settlement write and no raw FK. For a known call it inserts/dedupes one `call_event` by `(IVR_EVENT,event_id)`, validates session/leg identity and sequence, applies or quarantines the event once, updates call/leg facts and the absorbing first terminal `processing_status`, and hands an authoritative terminal event with a reservation to settlement. See §9.24. A duplicate with the same stored fingerprint returns the first processing outcome and appends/changes nothing; a changed input returns a stable conflict; a contradictory fact is `QUARANTINED` and alertable, never applied as success. `RECEIVED` durable receipt is distinct from accepted authoritative usage. No order/payment/consent write. |
| `entitlement-metering.settle_call_usage` | entitlement-metering | IVR call-event owner service on the same connection, `Propagation.MANDATORY` | Receives validated immutable `callId`, reservation identity, source `(IVR_EVENT, eventId)`, authoritative units, occurrence time and authority source from the IVR owner service. Locks the reservation and selected balance in the §9.0.1 suborder, validates the predicate and identity, settles/releases/quarantines per §9.15 with the derived balance status set in the same conditional `UPDATE` (OQ-8), appends at most one `usage_fact`, one terminal transition and Outbox/audit. It does not read or write IVR-owned tables; the IVR owner remains the authoritative source of the call fact. A duplicate/late event returns the first outcome; contradictory/excess/negative facts are quarantined and never release unknown use. |
| `catalog-order.publish_catalog_version` and `catalog-order.offsale_catalog_version` | catalog-order | Operations catalog publication/offsale endpoint | Acquire the owner publication serialization (§9.0.1), re-read with the caller's expected current version/revision, validate DRAFT/ACTIVE and immutable content, first set the previous `ACTIVE` version `OFFSALE` with revision +1 and then set the candidate `published_at` and `ACTIVE` with revision +1 (or perform `ACTIVE → OFFSALE`), in one transaction; append audit and the §10.17 Outbox identity. A concurrent/stale caller returns a stable `CATALOG_PUBLICATION_CONFLICT`/current state, never a raw unique error; historical orders/grants are never rewritten. This service does not claim an independent `catalog_offer` withdrawal entry (OQ-4). See §9.26. |
| `entitlement-metering.publish_trial_policy` and `disable_trial_policy` | entitlement-metering | Operations trial-policy publication/disable endpoint | Acquire the trial-policy serialization, require the caller's expected current enabled/published version, first disable the previous enabled published version (revision +1), then publish the candidate with ONE owner UPDATE that, while `OLD.published_at IS NULL`, sets the actual authenticated publisher in `published_by_employee_id`, `published_at`, `enabled = true` and revision +1 together, or disable the current version, in one transaction; the guard admits that statement and then freezes publisher identity/content; immutable content/history preserved; audit/Outbox appended; a stale/concurrent loser returns a stable `TRIAL_POLICY_PUBLICATION_CONFLICT`/current state. Existing grants keep their frozen policy. The sole authoritative issuance policy read is the enabled/published/expected-version read performed by the V006 `issue_first_trial` function; replay/already-granted returns prior immutable results before any new policy selection; the wrapper lookup is preliminary only and there is no second enabled recheck (OQ-6). See §9.26. |

### 10.21 S3 grant matrix

S3 runtime roles receive `USAGE` without `CREATE` on their own schema, `USAGE` on `platform` and `audit`, the exact cross-module schema `USAGE` and column `SELECT` below, and `EXECUTE` only on `platform.outbox_append` and `audit.append_fact`. Neither role receives platform idempotency functions, table privileges on platform/audit, `CREATE`, `DELETE`, `TRUNCATE` or any foreign table write. `PUBLIC` receives nothing. A schema `USAGE` never implies a table privilege.

| Object | Owner | S3 grants |
| --- | --- | --- |
| `evidence` schema | `evidence_owner` | `USAGE` without `CREATE` to `evidence_role` and `sharing_role` |
| `sharing` schema | `sharing_owner` | `USAGE` without `CREATE` to `sharing_role` only |
| `platform` schema and `platform.outbox_append` | `platform_reliability_owner` | `USAGE` without `CREATE` and `EXECUTE` to `evidence_role`, `sharing_role`; no table privilege |
| `audit` schema and `audit.append_fact` | `audit_owner` | `USAGE` without `CREATE` and `EXECUTE` to `evidence_role`, `sharing_role`; no table privilege |
| `subject_number` schema | `subject_number_owner` | `USAGE` without `CREATE` to `evidence_role` for its exact subject-existence read and to `sharing_role` for its exact ownership/binding/risk reads |
| `ivr` schema | `ivr_owner` | `USAGE` without `CREATE` to `evidence_role` for its exact call/session reads; no write or row-lock privilege |
| `entitlement_metering` schema | `entitlement_metering_owner` | `USAGE` without `CREATE` to `evidence_role` for its exact reservation read; no write or row-lock privilege |
| `customer_iam` schema | `customer_iam_owner` | `USAGE` without `CREATE` to `sharing_role` for its exact account-lifecycle read |
| `organization_access` schema | `organization_access_owner` | `USAGE` without `CREATE` to `sharing_role` for its exact organization/membership reads |

Owned-table column grants (no `DELETE`/`TRUNCATE`):

| Table | Role | `SELECT` | `INSERT` | `UPDATE` columns |
| --- | --- | --- | --- | --- |
| `evidence.media_object` | `evidence_role` | all | all | none (append-only) |
| `evidence.recording` | `evidence_role` | all | all | `media_object_id`, `validated_duration`, `validated_sha256`, `status`, `validated_at`, `revision` |
| `evidence.evidence_record` | `evidence_role` | all | all | `status`, `solidified_at`, `revision` |
| `evidence.evidence_artifact` | `evidence_role` | all | all | none (append-only) |
| `evidence.evidence_event` | `evidence_role` | all | all | none (append-only) |
| `sharing.evidence_share_grant` | `sharing_role` | all | all | `status`, `admissions_used`, `revocation_generation`, `revision`, `expires_at` (extension only), `code_salt`, `code_hash`, `code_generation` |
| `sharing.share_admission` | `sharing_role` | all | all | none (append-only) |
| `sharing.share_access_event` | `sharing_role` | all | all | none (append-only) |

Cross-module `SELECT` grants (no `INSERT`/`UPDATE`/`DELETE`, no foreign row-lock privilege):

| Grantee | Table | Exact columns | Purpose |
| --- | --- | --- | --- |
| `evidence_role` | `subject_number.business_subject` | `id` | Subject existence for the evidence FK/link validation |
| `evidence_role` | `ivr.call_record` | `call_id`, `session_id`, `subject_id` | Exact call/session/subject linkage |
| `evidence_role` | `ivr.inbound_session` | `id`, `call_id`, `subject_id` | Exact session linkage |
| `evidence_role` | `entitlement_metering.usage_reservation` | `id`, `call_id`, `subject_id` | Exact reservation linkage |
| `sharing_role` | `evidence.evidence_record` | `id`, `subject_id`, `status`, `source_type`, `source_ref_id`, `call_id`, `session_id`, `reservation_id`, `identity_at_capture`, `created_at` | Solidified-evidence scope, content binding and captured-binding risk projection |
| `sharing_role` | `evidence.evidence_artifact` | `evidence_id`, `media_object_id`, `artifact_role` | Frozen artifact-role scope; no object identity exposure |
| `sharing_role` | `customer_iam.customer_account` | `id`, `status` | Creator account lifecycle |
| `sharing_role` | `organization_access.organization` | `id`, `status` | Workspace organization lifecycle |
| `sharing_role` | `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` | Creator/recipient membership and role under the logical pair lock |
| `sharing_role` | `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` | Historical-evidence ownership/access policy; read-only |
| `sharing_role` | `subject_number.caller_number_binding` | `id`, `subject_id`, `status`, `verified_at`, `revision` | Captured/active binding projection for the risk check |
| `sharing_role` | `subject_number.subject_risk_state` | `caller_number_binding_id`, `risk_type`, `state`, `revision` | `OPEN` risk blocks historical-evidence access; never a blanket dialing ban |

- A cross-module reader re-reads after acquiring the corresponding lifecycle advisory lock and never takes `FOR SHARE`/`FOR UPDATE` on another module's table. Sharing does not read evidence media-object bucket/key/version; the media gateway receives object identity only through the internal evidence/media interface.
- The `sharing_role` `evidence_record` projection includes only the already-redacted `identity_at_capture` JSONB needed to resolve the captured binding for the historical-evidence risk rule; it grants no object identity, media bytes, object key/version or broad all-column evidence access.
- Sharing builds `content_snapshot.content`, `scopeDigest` and `content_sha256` only through the evidence-owner `evidence.read_shareable_projection` collaborator (§2.12/§10.23); the exact media SHA-256/size/MIME/validated duration facts are returned by that owner under `evidence_role`, not by a direct `sharing_role` grant on `evidence.media_object` or `evidence.recording`. No object id, key, version, bucket, bytes, permanent URL or crypto key reference is exposed, and no foreign row lock is taken.
- `sharing_role` has `INSERT` but no `UPDATE` on admissions/access events; the admission count is changed only on the grant row, and only the sharing owner service locks that grant row. `evidence_role` has no sharing table or schema privilege and no `INSERT` on platform/audit tables; it uses the two functions only. No S1/S2 role receives any S3 table or schema privilege.
- S3 adds no function-wave row. The four V001 functions retain their S1 contracts; `issue_first_trial` retains its V006 contract. S3 uses `platform.outbox_append` and `audit.append_fact` only.

### 10.22 S3 guard and deferred constraint triggers

The S3 triggers are owner-local structural controls. They are `SECURITY INVOKER`, owned by `evidence_owner` or `sharing_owner`, declare `SET search_path = evidence, pg_temp` or `SET search_path = sharing, pg_temp`, contain no transaction control, are not callable cross-schema service APIs and add no function-wave entry. The migration grants `EXECUTE` on each trigger function only to the owning runtime role; direct invocation is not a supported API.

| Trigger | Timing / operation | Guard predicate |
| --- | --- | --- |
| `evidence.trg_media_object_immutable` | `BEFORE UPDATE OR DELETE` on `evidence.media_object`, row | Reject every update/delete by `evidence.reject_row_mutation()`. |
| `evidence.trg_recording_guard` | `BEFORE UPDATE` on `evidence.recording`, row | Reject changes to `id`, `call_id`, `recording_id`, `declared_format`, `declared_size`, `declared_duration`, `declared_sha256`, `upload_expires_at` or `created_at`; reject a terminal `VALIDATED`/`QUARANTINED` → any other status; reject clearing/changing a set `media_object_id`; reject setting validation columns outside the allowed `VALIDATED`/`QUARANTINED` shape. A post-terminal conflicting callback is refused by the owner service before any update and appends only the deduplicated redacted conflict fact; it never attempts a terminal status change. |
| `evidence.trg_evidence_record_guard` | `BEFORE UPDATE` on `evidence.evidence_record`, row | Reject changes to `id`, `subject_id`, `source_type`, `source_ref_id`, `source_event_domain`, `source_event_id`, `call_id`, `session_id`, `reservation_id`, `identity_at_capture`, `consent_version`, `snapshot`, `snapshot_sha256` or `created_at`; reject a terminal status → any other status; allow only the one PENDING → terminal transition with revision. |
| `evidence.trg_evidence_artifact_immutable` | `BEFORE UPDATE OR DELETE` on `evidence.evidence_artifact`, row | Reject every update/delete. |
| `evidence.trg_evidence_event_immutable` | `BEFORE UPDATE OR DELETE` on `evidence.evidence_event`, row | Reject every update/delete. |
| `evidence.trg_evidence_record_creation_cycle` | `CONSTRAINT TRIGGER AFTER INSERT OR UPDATE OF source_event_domain, source_event_id, status ON evidence.evidence_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW` | At commit, require exactly one `EVIDENCE_CREATED` event for `NEW.id`, with `event_domain = NEW.source_event_domain = 'EVIDENCE'`, `event_id = NEW.source_event_id` and `evidence_id = NEW.id`; otherwise raise `EVIDENCE_CREATION_CYCLE_VIOLATION`. |
| `evidence.trg_evidence_event_creation_cycle` | `CONSTRAINT TRIGGER AFTER INSERT OR UPDATE OF evidence_id, event_domain, event_id, event_type ON evidence.evidence_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW` | At commit, for an `EVIDENCE_CREATED` event, require its owner record to exist, `record.source_event_domain = NEW.event_domain = 'EVIDENCE'`, `record.source_event_id = NEW.event_id`, `NEW.evidence_id = record.id`, and exactly one creation event for that record; otherwise raise `EVIDENCE_CREATION_CYCLE_VIOLATION`. Non-creation events skip this check. |
| `sharing.trg_share_grant_guard` | `BEFORE UPDATE` on `sharing.evidence_share_grant`, row | Reject changes to `id`, `share_id`, `capability_token_hash`, `evidence_id`, `created_by_account_id`, `owner_organization_id`, `verification_mode`, `valid_from`, `max_admissions`, `download_allowed`, `content_snapshot` or `content_sha256`; reject a decrease of `expires_at`, a generation decrease, a terminal (`EXPIRED`/`REVOKED`) status → any other status, or a `code_generation` change without a matching salt/hash replacement. `LOCKED` is non-terminal: permit `LOCKED → REVOKED`/`EXPIRED` through the owner revoke/expiry transitions and `LOCKED → ACTIVE` only with a matching code rotation; reject any other `LOCKED` transition. |
| `sharing.trg_share_admission_guard` | `BEFORE INSERT` on `sharing.share_admission`, row | Re-read the parent grant without acquiring a new lock; require existence, `NEW.verification_mode = grant.verification_mode`, `grant.status = 'ACTIVE'`, `clock_timestamp() >= grant.valid_from`, `clock_timestamp() < grant.expires_at`, the limit not exceeded, and `NEW.admission_no = grant.admissions_used + 1`; otherwise raise the stable admission refusal. The service holds the grant `FOR UPDATE` in the same transaction. |
| `sharing.trg_share_admission_immutable` | `BEFORE UPDATE OR DELETE` on `sharing.share_admission`, row | Reject every update/delete by `sharing.reject_row_mutation()`. |
| `sharing.trg_share_access_event_immutable` | `BEFORE UPDATE OR DELETE` on `sharing.share_access_event`, row | Reject every update/delete by `sharing.reject_row_mutation()`. |

The two creation-cycle triggers are the only deferred S3 controls. They are installed in V008 after both deferred FKs, so a record/event pair can be inserted in either order and is validated once at commit. The trigger function runs with the invoker's privileges (`evidence_role` has `SELECT` on both evidence tables and owns the insert), so it introduces no privilege escalation and no cross-schema write. A trigger error aborts the transaction; it cannot return a partial success. The admission guard is an immediate row trigger under the already-held grant lock; it is a backstop for new-admission mode/count/validity, not a substitute for the service's conditional update. An existing same-key admission replay returns before any insert, so it is never re-evaluated as a new admission or rejected by the limit. No guard relaxes append-only, revision, ownership, creation-cycle, generation or count rules.

### 10.23 S3 published application-service contracts

These are application services on one connection and one local transaction; they are not SQL functions, add no function-wave row, never commit or roll back independently and never hold a PostgreSQL lock across COS, Redis or media-worker I/O. Each authenticates its caller and validates current facts.

| Service | Owner | Called by | Exact contract |
| --- | --- | --- | --- |
| `evidence.open_recording_upload` | evidence | IVR/media upload entry | Validates the authenticated IVR/worker service, `call_id`, `recording_id` and declared facts; inserts one `DECLARED` recording with an explicitly approved `upload_expires_at` and no media object; returns a scoped short-lived upload credential. A same-identity request with identical declarations returns the existing `DECLARED`/`UPLOADED` row without TTL refresh; changed declarations conflict. No evidence lifecycle or media object exists yet. |
| `evidence.record_upload_observed` | evidence | IVR/media upload completion, before validation | Applies the OQ-6 upload-observation transition. Validates the authenticated IVR/worker service, `recording_id`, exact object identity/version and server-observed hash/size/MIME facts; the object facts are computed outside the transaction. It first derives/resolves the deterministic recording/object identity and immutable observation fingerprint: an existing identical observation returns the first committed `UPLOADED` result before any current recording-status precondition and mutates nothing; an existing same identity with a changed fingerprint returns `RECORDING_IDENTITY_CONFLICT` and mutates nothing. Only a genuinely new observation then opens one short transaction, locks the recording row, re-reads status/expiry/revision, requires `DECLARED`, `upload_expires_at > clock_timestamp()` and no prior media object, inserts or reuses only the immutable `evidence.media_object` for that exact `(bucket, object_key, object_version_id)`, and performs one conditional `DECLARED → UPLOADED` update setting `media_object_id` and `status = 'UPLOADED'` with revision +1. Missing/expired/terminal state fails closed. Only `UPLOADED` proceeds to validation/quarantine; no external I/O occurs while the row lock is held. | `recordingId`, `mediaObjectId`, `UPLOADED` or a stable conflict |
| `evidence.complete_recording` | evidence | IVR/media worker with a validated server result | Applies T-S3-01: lock the recording, re-read expiry/state, accept server validation facts, and for an `UPLOADED` row alone reuse the immutable media object recorded by `evidence.record_upload_observed` and transition to `VALIDATED`/`QUARANTINED`; a `DECLARED` row returns `UPLOAD_OBSERVATION_REQUIRED` with no media/evidence fact and must first pass `record_upload_observed`. A post-terminal conflicting callback returns stable `RECORDING_IDENTITY_CONFLICT` and appends only the deduplicated redacted conflict fact without changing the terminal row. Appends Outbox/audit and commits. Never solidifies evidence. |
| `evidence.create_and_solidify` | evidence | Evidence owner service after validated recording/media facts | Applies T-S3-02: validate the stable source event `eventId` and the source/recording/media, require `recording.call_id = evidence_record.call_id` and the exact trusted call/session/subject/reservation links (`EVIDENCE_LINK_MISMATCH` otherwise, with no fact), insert the evidence record with `source_event_id = eventId` and one matching `EVIDENCE_CREATED` event with `event_id = eventId` in either order, insert the original artifact, transition to `SOLIDIFIED`/`QUARANTINED`/`FAILED`, append the lifecycle event and Outbox/audit, commit under both deferred FKs/triggers. Under the same stable `eventId`, exact replay returns the first evidence/event/artifact result and changed subject/call/session/reservation/recording/consent/snapshot facts return `EVIDENCE_SOURCE_CONFLICT` with no second record, creation event, artifact or Outbox fact. Returns the evidence/event/artifact IDs. |
| `evidence.read_shareable_projection` | evidence | `sharing.create_share_grant` on the same connection with `Propagation.MANDATORY`, before the owner-local grant mutation, including on the existing-identity replay path before the frozen creation-fingerprint comparison | Read-only owner-local projection for a `SOLIDIFIED` record. Returns only `{evidenceId, subjectId, status, sourceType, sourceRefId, identityAtCapture, artifacts[]}` with each artifact `{artifactRole, mediaSha256, mediaSizeBytes, mediaMimeType, validatedDurationSeconds}`, ordered by the fixed non-lexical artifact-role rank (`ORIGINAL = 0`, `DERIVED = 1`) then `mediaSha256`/`mediaSizeBytes`/`mediaMimeType` ascending. It exposes no media-object id, bucket, object key, version, bytes, permanent URL or crypto key reference, writes nothing, takes no foreign row lock and adds no broad `sharing_role` grant. | Current redacted evidence/artifact/media facts for the frozen content; `EVIDENCE_NOT_SOLIDIFIED` when not solidified |
| `sharing.create_share_grant` | sharing | Authenticated creator/workspace | Applies §9.29 steps 1-4: derive `share_id` from creator + stable `creationRequestId`; for an existing or new creation, call `evidence.read_shareable_projection` and reconstruct the deterministic `content` from the projection plus the current request parameters before any state-dependent check, and for an existing grant before recomputing/comparing the frozen `content_snapshot.creation` fingerprint (exact immutable facts and same fingerprint return metadata only; changed scope, `expiresAt` or artifact/media/content facts is `SHARE_CREATION_CONFLICT`; an absent/unresolvable/not-`SOLIDIFIED` projection fails closed); otherwise verify solidified evidence and approved access policy, lock/read S1 ranks 7/8/9 where applicable, freeze content/verification/creation/validity/limit, generate the capability token (128+ random bits) and optional code/mobile binding, persist only digests/HMACs, insert one grant, append audit/Outbox, commit. Returns the one-time capability token and share facts on first creation only; never returns an object URL/key and never reconstructs/reissues a lost token. |
| `sharing.apply_share_transition` | sharing | Authenticated creator/operations action | Applies §9.29 steps 5-7: resolve the deterministic §2.12 transition event identity/fingerprint first; a same-action retry returns the first recorded outcome before current state checks, changed content conflicts. Otherwise revoke, code rotation/unlock, expire, lock or extend under the grant row lock with current status/revision/generation; `LOCKED → REVOKED`/`EXPIRED` are allowed and `LOCKED → ACTIVE` only through code rotation. Appends one deterministic access event, audit/Outbox and one conditional update. Emits no plaintext code/token. |
| `sharing.admit_share` | sharing | Public share request | Applies T-S3-04: token lookup, mode verification for a new admission (designated mobile includes trusted SMS proof), optional organization recipient re-authentication under ranks 7/8/9, grant row lock/re-read, current status/time/generation/authorization checks, then existing-admission replay by key before any limit test, then for a new key the count/limit check, one admission and `ADMISSION_GRANTED` witness (including recipient binding for `ORGANIZATION_MEMBER`), one conditional count/revision update, commit. Returns the original unexpired admission on a same-key retry at or below the limit without a new cap test/increment; no ticket is written inside this transaction. |
| `sharing.issue_or_reconstruct_ticket` | sharing | Post-commit admission retry, lost response/cache recovery, media renewal | Applies T-S3-05: derive the Redis key from grant/admission and write a short signed/segmented ticket with the absolute `ticketExpiresAt = min(clock_timestamp() + approved Q22 ticket TTL, sessionExpiresAt, admissionExpiresAt, current grant expires_at)` and a Redis TTL equal to the positive `ticketExpiresAt - clock_timestamp()` only, or reconstruct from the durable admission + `ADMISSION_GRANTED` witness after rechecking current grant/generation/time and session/admission expiry. For `ORGANIZATION_MEMBER`, first re-authenticate the witness recipient and take ranks 7/8/9 under the current active organization/membership/account, before reading the admission/witness, grant or risk projection and before any grant lock; a changed account/tenant or revoked/suspended member fails closed. It re-reads the exact captured/current binding and `subject_risk_state` projection and denies `SHARE_RISK_BLOCKED` on `OPEN` or an absent/unresolvable projection. PostgreSQL locks/transaction close before Redis I/O. Fails closed on Redis unavailability; never increments admissions, never changes a generation and never returns an object URL/key. |
| `sharing.authorize_media_request` | sharing | Authenticated media gateway/playback/seek/renewal | Validates the short signed credential and the recorded/current generation and time. For `ORGANIZATION_MEMBER`, re-authenticates the same witness recipient and takes S1 ranks 7/8/9 before any grant read/lock. Re-reads the exact current grant status/time/generation and the exact captured/current binding plus `subject_number.subject_risk_state` projection: the captured binding is `identity_at_capture.bindingId` when present, otherwise the evidence subject's current active binding set. `OPEN` risk, or an absent/unresolvable binding/risk projection, returns `SHARE_RISK_BLOCKED` and issues no media credential and no access event; no admission count or generation changes. Otherwise appends one redacted playback/seek/renewal event and allows the request only within the session/admission and current grant validity. Never recalls already-issued bytes; the approved credential TTL is the bounded revoke window (Q22); PostgreSQL reads/locks are released and the transaction closes before the media-gateway/Redis I/O. |
| `sharing.record_access_event` | sharing | Sharing owner services | Appends one deterministic, redacted `SHARE_ACCESS` event for page open/prefetch/verification failure/lock/admission/ticket/playback/seek/renewal/extension/rotation/revoke/expiry/failure; same-semantic retry returns the first event. It never consumes an admission, alters the count or writes plaintext input. `PAGE_OPENED`, `PREFETCH_IGNORED` and `VERIFICATION_FAILED` use the exact §2.12 `share-observation:` identity and payload fingerprint from the grant ID, event type and request-key digest; a same-semantic retry returns the first event, a changed payload under the same identity is a stable conflict and appends nothing, and the raw request key is never stored. |

The S3 services consume only S1/S2 read contracts and the two existing platform/audit functions. No S3 service writes an S1/S2 table, no S1/S2 service calls an S3 service for its own transaction, and no S3 service is a `SECURITY DEFINER` SQL function.

### 10.24 S3 evidence lifecycle event and Outbox identity registry (F-10)

This is a bounded persisted-data registry for the current evidence lifecycle events and evidence Outbox facts. It adds no table, column, SQL function, function-wave row, action or wire encoding (Q15). Every derived identity is a lowercase SHA-256 over the section 2.5 canonical JSON of only persisted immutable source facts; the single exception is the `EVIDENCE_CREATED` identity, which is the validated stable source event `eventId` itself, never a hash of mutable capture facts. No random UUID or current timestamp is an identity or payload-fingerprint input.

- Every event/dedupe identity is at most 128 characters: a derived identity is a fixed prefix plus the 64-hex digest and fits `evidence_event.event_id` 1-128, `outbox_event.dedupe_key` 1-512 and `payload_sha256` 64-hex, while the `EVIDENCE_CREATED` identity is the validated 1-128 source event ID and also fits `evidence_event.event_id`.
- In these formulas `recordingId` is `evidence.recording.id` canonical lowercase UUID text, `mediaObjectId` is `evidence.media_object.id` canonical lowercase UUID text, `callId` is `ivr.call_record.call_id`, and `subjectId`/`sessionId`/`reservationId` are canonical lowercase UUID text (null-safe where noted). `validatedSha256`/`presentedSha256` are lowercase 64-hex values, and `validatedDuration`/`presentedSizeBytes`/`presentedDuration` are integers.
- A payload fingerprint is the lowercase SHA-256 of the exact canonical redacted payload listed below. For `evidence.evidence_event`, the same `(event_domain, event_id)` with the same `payload_sha256` returns the first event and appends nothing; a changed `payload_sha256` under the same identity is a stable conflict and appends nothing — `EVIDENCE_SOURCE_CONFLICT` for the `EVIDENCE_CREATED` event, whose identity is the validated stable source event, and `EVIDENCE_EVENT_CONFLICT` for a lifecycle event. For `platform.outbox_append`, the same `(event_domain, dedupe_key)` with the same payload fingerprint returns the existing Outbox ID and appends nothing; a changed payload fingerprint raises `OUTBOX_DEDUPE_CONFLICT` and appends nothing.
- A same-semantic retry therefore returns the first event/Outbox ID and appends no second fact. A different semantic fact under the same identity is a conflict, never a second lifecycle event.

| Event type | Event identity (`evidence.evidence_event.event_id`) | Exact identity inputs | Exact redacted payload (payload-fingerprint inputs) |
| --- | --- | --- | --- |
| `EVIDENCE_CREATED` | the validated source event `eventId` supplied with the creation request (nonblank 1-128), which is exactly the record's persisted `source_event_id` and this event's `event_id`; it is never a hash or derivation of mutable capture/link/snapshot facts | the validated source event ID only | `{evidenceId, sourceEventId, eventType, subjectId, callId, sessionId (null-safe), reservationId (null-safe), recordingId, snapshotSha256, consentVersion}` |
| `EVIDENCE_SOLIDIFIED` | `evidence-solidified:` + lowercase SHA-256 of canonical `{evidenceId, eventType}` (84 characters) | the record ID and event type | `{evidenceId, sourceEventId, eventType, status, snapshotSha256, artifactRoles (rank-ordered: ORIGINAL = 0, DERIVED = 1), reasonCode (null-safe)}` |
| `EVIDENCE_QUARANTINED` | `evidence-quarantined:` + lowercase SHA-256 of canonical `{evidenceId, eventType}` (85 characters) | the record ID and event type | `{evidenceId, sourceEventId, eventType, status, snapshotSha256, artifactRoles (rank-ordered: ORIGINAL = 0, DERIVED = 1), reasonCode}` |
| `EVIDENCE_FAILED` | `evidence-failed:` + lowercase SHA-256 of canonical `{evidenceId, eventType}` (80 characters) | the record ID and event type | `{evidenceId, sourceEventId, eventType, status, snapshotSha256, reasonCode}` |

For a terminal lifecycle event, the event type is fixed by the record's one terminal transition, so `{evidenceId, eventType}` is stable across retries; a conflicting terminal attempt is refused before appending and cannot produce a second type identity. The creation event identity is exactly the validated source event `eventId` and the reciprocal record source identity; changed subject/call/session/reservation/recording/consent/snapshot facts under that stable identity return `EVIDENCE_SOURCE_CONFLICT` and append nothing, while exact replay returns the first record/event/artifact result. `artifactRoles` is the rank-ordered artifact-role list from the immutable artifact set, using the fixed non-lexical rank `ORIGINAL = 0`, `DERIVED = 1` (section 2.12).

Recording Outbox facts (event domain `EVIDENCE`, aggregate type `RECORDING`, aggregate id `recording.id`):

| Event type | Dedupe key | Exact identity inputs | Exact redacted payload (payload-fingerprint inputs) |
| --- | --- | --- | --- |
| `EVIDENCE_RECORDING_VALIDATED` | `evidence-recording-validated:` + lowercase SHA-256 of canonical `{recordingId, mediaObjectId, validatedSha256, validatedDuration}` (93 characters) | persisted `recording.id`, `recording_id`, immutable `media_object_id`, `validated_sha256`, `validated_duration` | `{recordingId, mediaObjectId, validatedSha256, validatedDuration, callId}` |
| `EVIDENCE_RECORDING_QUARANTINED` | `evidence-recording-quarantined:` + lowercase SHA-256 of canonical `{recordingId, reasonCode, presentedSha256, presentedSizeBytes, presentedMime, presentedDuration}` (95 characters) | persisted `recording.id`, `recording_id`, stable reason and server-observed presented facts; object key/version is never an input | `{recordingId, reasonCode, presentedSha256, presentedSizeBytes, presentedMime, presentedDuration, callId}` |

The existing terminal recording-conflict fact is unchanged: event domain `EVIDENCE`, aggregate type `RECORDING`, aggregate id `recording.id`, event type `RECORDING_CONFLICT`, dedupe key `recording-conflict:` + the lowercase SHA-256 of the §4.40 canonical conflict input, and the same/changed-retry contract exactly as §4.40. Platform/audit append failures remain category 3 in §9.27/§9.28 and roll back the domain fact; no Outbox or event row survives a failed transaction.

## 11. Linear Flyway Migration Order

### 11.1 Frozen nine-wave migration baseline

The following nine rows are the accepted migration order. They are verbatim from the accepted plan. S1 authored V001-V003; S2 authored V004-V007; S3 authors V008-V009 in §11.5. The rows themselves are unchanged.

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

The exact slice membership blocks are verbatim from the accepted plan. S1 is 13 authored tables; S2 is 23 authored tables; S3 is 8 authored tables in this slice. The frozen membership is structural closure; authored dictionaries and transactions are in §4.39-§4.46 and §9.27-§9.31.

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

### 11.4 S2 V004-V007 intra-wave creation order

V004: catalog, order and payment.

1. Create schemas `catalog_order`, `ivr`, `entitlement_metering` and `activation_consent`; create the four owner/runtime role pairs from §2.9; revoke `CREATE` on all business schemas from `PUBLIC`. Grant the four S2 runtime roles `USAGE` without `CREATE` on `platform` and `audit` and `EXECUTE` on the four S1 functions per §10.8-§10.12; grant `USAGE` without `CREATE` on `catalog_order` to `catalog_order_role`/`entitlement_metering_role`, on `activation_consent` to `activation_consent_role`/`ivr_role`/`catalog_order_role`, and on `entitlement_metering` to `entitlement_metering_role`/`activation_consent_role`/`ivr_role` according to §10.12. No table privilege is implied by schema `USAGE`.
2. Create `catalog_order.catalog_product` → `catalog_order.catalog_version` (FK to product and employee) → `catalog_order.catalog_offer` and `catalog_order.catalog_benefit_line` (FK to version) → `catalog_order.customer_order` (FKs to account, organization, subject, version and offer; `purchase_consent_id` is NOT NULL but its FK is deferred to V007) → `catalog_order.payment_order` → `catalog_order.payment_event`.
3. Add all V004 indexes and named checks from §4.16-§4.22. Create the catalog/order/payment guard and append-only triggers from §10.16.
4. Apply V004 owned-table grants and the cross-module `SELECT` grants from §10.12. For `catalog_order_role`'s declared reads, grant `USAGE` without `CREATE` on `customer_iam`, `organization_access` and `subject_number` (F-01); the already-created S1 schemas are readable but no S1 table write is granted. No V004 table references a table created later; `purchase_consent_id` intentionally has no FK in this wave.
5. Do not create any entitlement, IVR, activation or S3 table in V004. Do not create `issue_first_trial`.

V005: IVR task, session, call, leg, event and decision.

1. Create `ivr.outbound_call_task` (FKs to binding, subject, accounts and organization; `claimed_call_id` column without the FK) → `ivr.inbound_session` (FKs to subject and confirmed subject; `call_id` has no FK) → `ivr.call_record` (unique `call_id`, FK to session and task; `call_id` has no parent FK) → `ivr.call_leg` (FK to call) → `ivr.call_event` (FK to call) → `ivr.ivr_decision` (FKs to session and call; `reservation_id` column without the FK).
2. Add all V005 indexes and named checks from §4.23-§4.28, including the partial unique caller indexes.
3. `ALTER TABLE ivr.outbound_call_task ADD CONSTRAINT fk_outbound_task_claimed_call FOREIGN KEY (claimed_call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT` after `call_record` exists. This is an ordinary immediate `ALTER_FK`, not deferrable.
4. Create the IVR guard/append-only triggers from §10.16 and apply the V005 grants and cross-module `SELECT` grants. Grant `ivr_role` `USAGE` without `CREATE` on its own `ivr` schema and on `customer_iam`, `organization_access` and `subject_number` for its declared lifecycle/subject reads (F-01); no foreign table write or `CREATE` is granted. The `ivr_decision.reservation_id` FK is not added in V005.
5. Do not create an S2 SQL function in V005. `ivr.select_call_action` and `ivr.record_activation_next_action` are application services.

V006: trial, entitlement, issuance, reservation and usage.

1. Create `entitlement_metering.trial_policy_version` (FK to employee) → `entitlement_metering.entitlement_grant` (FKs to subject and order) → `entitlement_metering.trial_grant` (FKs to subject, policy and grant; unique subject/grant/source) → `entitlement_metering.entitlement_balance` (FK to grant) → `entitlement_metering.entitlement_issuance` (FKs to order and grant) → `entitlement_metering.usage_reservation` (FKs to call, balance and subject) → `entitlement_metering.usage_fact` (FKs to reservation, balance and call).
2. Add all V006 indexes and named checks from §4.29-§4.35 and create the entitlement guard/append-only triggers from §10.16.
3. Create `entitlement_metering.issue_first_trial` only now, after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance` exist. Revoke all from `PUBLIC`; grant `EXECUTE` only to `activation_consent_role` and `entitlement_metering_role`; grant `USAGE` on `entitlement_metering` and `platform` without `CREATE` to those roles. Grant the NOLOGIN `entitlement_metering_owner` itself `USAGE` without `CREATE` on `platform` and `EXECUTE` on `platform.outbox_append` so the function body's nested append executes under `SECURITY DEFINER` (F-02); these are internal owner capabilities, not runtime or `PUBLIC` grants.
4. `ALTER TABLE ivr.ivr_decision ADD CONSTRAINT fk_ivr_decision_reservation FOREIGN KEY (reservation_id) REFERENCES entitlement_metering.usage_reservation(id) ON UPDATE RESTRICT ON DELETE RESTRICT` after the reservation table exists. This is an ordinary immediate `ALTER_FK`, not deferrable.
5. Apply the V006 owned-table and cross-module grants from §10.12. Grant `entitlement_metering_role` `USAGE` without `CREATE` on `subject_number` for its declared subject/owner read (F-01); `ivr_role` already holds the declared read grants including `entitlement_balance.granted_units` and `entitlement_grant.granted_units` (F-03/OQ-2). Grant `catalog_order_role` `USAGE` without `CREATE` on `entitlement_metering` and the exact read-only columns on `entitlement_issuance(order_id, installment_no, entitlement_type, status, grant_id, issued_at, last_error_code)` and `entitlement_grant(id, subject_id, entitlement_type, granted_units, valid_from, expires_at)` for the computed order-level entitlement projection (R-04); no write or foreign row lock is granted. No V006 table or function references an S3 table.

V007: consent and activation.

1. Create `activation_consent.consent_record` (FKs to subject, session, account and employee) → `activation_consent.service_activation` (unique subject FK) → `activation_consent.service_activation_transition` (FKs to activation, subject, actor account/employee and consent).
2. Add all V007 indexes and named checks from §4.36-§4.38 and create the activation guard/append-only triggers from §10.16.
3. `ALTER TABLE catalog_order.customer_order ADD CONSTRAINT fk_customer_order_consent FOREIGN KEY (purchase_consent_id) REFERENCES activation_consent.consent_record(id) ON UPDATE RESTRICT ON DELETE RESTRICT` after `consent_record` exists. This is an ordinary immediate `ALTER_FK`, not deferrable.
4. Apply the V007 owned-table and cross-module grants from §10.12. Grant `activation_consent_role` `USAGE` without `CREATE` on `subject_number`, `customer_iam` and `organization_access` for its declared subject/account/workspace reads, with the exact `SELECT` columns on `business_subject(id, owner_account_id, owner_organization_id, revision)`, `customer_account(id, status)`, `organization(id, status)` and `organization_membership(organization_id, account_id, role, status)` (F-01/R-03); no foreign write, row lock or `CREATE` is granted. The activation-confirmation, lock-only activation collaborator, reservation, client-activation, purchase-consent, call-event, publication and IVR action-selection services are application services, not SQL functions; no function is created in V007.

### 11.5 S3 V008-V009 intra-wave creation order

V008: evidence media and evidence creation cycle.

1. Create schema `evidence` with owner `evidence_owner`; create only the NOLOGIN `evidence_role`; grant `USAGE` without `CREATE` on `evidence` to `evidence_role` only. Do not reference `sharing_role` in V008: it does not exist until V009.
2. Create `evidence.media_object` with its PK, unique `(bucket, object_key, object_version_id)`, named checks and indexes. It has no parent FK.
3. Create `evidence.recording` after `media_object`; add `fk_recording_call` to `ivr.call_record.call_id` (parent V005) and `fk_recording_media_object` to `evidence.media_object.id` (same-wave parent first), plus its checks/indexes.
4. Create `evidence.evidence_record` with its ordinary FKs to `subject_number.business_subject`, `ivr.call_record`, `ivr.inbound_session` and `entitlement_metering.usage_reservation`; create its unique `(source_event_domain, source_event_id)`, indexes and checks. Do **not** add the composite source-event FK yet because `evidence_event` does not exist.
5. Create `evidence.evidence_artifact` after `evidence_record` and `media_object`; add both ordinary FKs and the unique/partial-unique/index set.
6. Create `evidence.evidence_event` with its `event_domain` check, unique `(event_domain, event_id)`, partial unique creation-event index and checks. Do **not** add the reverse `evidence_id` FK at table creation; the cycle FKs are added only after both tables exist.
7. Add the two deferred cycle constraints by exact `ALTER TABLE` statements, in either order:
   - `ALTER TABLE evidence.evidence_record ADD CONSTRAINT fk_evidence_source_event FOREIGN KEY (source_event_domain, source_event_id) REFERENCES evidence.evidence_event(event_domain, event_id) DEFERRABLE INITIALLY DEFERRED;`
   - `ALTER TABLE evidence.evidence_event ADD CONSTRAINT fk_evidence_event_record FOREIGN KEY (evidence_id) REFERENCES evidence.evidence_record(id) DEFERRABLE INITIALLY DEFERRED;`
   Both are ordinary deferred constraints, not `ALTER_FK` in the accepted FK-mode sense; the frozen mode count classifies them as the two `DEFERRABLE_FK` decisions. Both reference already-created PK/UNIQUE keys and introduce no forward wave.
8. Create the evidence guard/append-only trigger functions and `BEFORE` triggers from §10.22. After both deferred FKs exist, create the two `CONSTRAINT TRIGGER ... DEFERRABLE INITIALLY DEFERRED` creation-cycle triggers. Grant `EXECUTE` on each trigger function only to `evidence_role`.
9. Apply the V008 owned-table and cross-module grants from §10.21. Grant `evidence_role` `EXECUTE` on `platform.outbox_append` and `audit.append_fact` and `USAGE` without `CREATE` on `platform`/`audit`, `subject_number`, `ivr` and `entitlement_metering` for the exact declared reads. No V008 table references V009, no function is created, and no S1/S2 table is altered.

V009: sharing authorization, admissions and access events.

1. Create schema `sharing` with owner `sharing_owner`; create the NOLOGIN `sharing_role` first; only after the role exists, grant `USAGE` without `CREATE` on `sharing` to `sharing_role` and on `evidence` to `sharing_role`. This is the first grant to `sharing_role`; no V008 statement references it.
2. Create `sharing.evidence_share_grant` with FKs to the V008 `evidence.evidence_record`, V002 `customer_iam.customer_account` and V002 `organization_access.organization`; create its unique `share_id`/capability-token-hash keys, checks and indexes.
3. Create `sharing.share_admission` after the grant; add `fk_share_admission_grant`, unique `(share_grant_id, admission_key)` and `(share_grant_id, admission_no)`, checks and indexes.
4. Create `sharing.share_access_event` after the grant and admission; add `fk_share_access_event_grant` and the nullable `fk_share_access_event_admission`, unique `(event_domain, event_id)`, checks and indexes.
5. Create the sharing guard/append-only trigger functions and triggers from §10.22; grant `EXECUTE` only to `sharing_role`. The admission `BEFORE INSERT` guard and the grant/admission/access-event append-only triggers are installed in this wave.
6. Apply the V009 owned-table and cross-module grants from §10.21: `sharing_role` receives exact reads on `evidence.evidence_record` (including the already-redacted `identity_at_capture`)/`evidence.evidence_artifact`, `customer_iam.customer_account`, `organization_access.organization`/`organization_membership`, `subject_number.business_subject`/`caller_number_binding`/`subject_risk_state`, `EXECUTE` only on `platform.outbox_append`/`audit.append_fact`, and `USAGE` without `CREATE` on the corresponding schemas. It receives no platform idempotency function, no platform/audit table privilege and no evidence write. No V009 table is referenced by S1/S2, no function is created and no evidence row is updated by sharing.

No S3 wave contains a forward reference. V008 parent waves are V003/V005/V006 and same-wave V008; V009 parent waves are V002/V008 and same-wave V009. Role order is also forward-safe: V008 creates and grants only `evidence_role`; V009 creates `sharing_role` before any grant to it, including its evidence schema `USAGE`. The V008 cycle is created in the order `media_object → recording → evidence_record → evidence_artifact → evidence_event → both deferred FKs → triggers`; the record/event insert order at runtime may be either way because deferral applies only to row insertion, not to DDL creation. There is no data backfill and no executable migration in this document.

## 12. Deletion, Retention and Retention-Hook Policy

### 12.1 S1 lifecycle classes

| Class | S1 tables | Rule |
| --- | --- | --- |
| Mutable head | platform.idempotency_record, platform.outbox_event, customer_iam.customer_account, customer_iam.auth_identity, customer_iam.identity_profile, organization_access.organization, organization_access.organization_membership, operations_iam.employee_account, subject_number.business_subject, subject_number.caller_number_binding, subject_number.subject_risk_state | Status/terminal-time retirement and revision-controlled updates; no ordinary hard delete |
| Append-only or frozen | audit.audit_fact, subject_number.subject_claim | No update/delete privilege; append-only trigger; stable identity and hash where applicable |

Every S1 table has exactly one class. The all-44 classification remains the frozen baseline matrix; S2 lifecycle details are authored in §12.5 and S3 details in §12.7-§12.8.

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

### 12.5 S2 lifecycle classes

| Class | S2 tables | Rule |
| --- | --- | --- |
| Mutable head | catalog_order.catalog_version, catalog_offer, customer_order, payment_order; ivr.outbound_call_task, inbound_session, call_record, call_leg, call_event; entitlement_metering.trial_policy_version, entitlement_balance, entitlement_issuance, usage_reservation; activation_consent.service_activation | Status/terminal-time retirement, conditional revision, no ordinary hard delete |
| Append-only or frozen | catalog_order.catalog_product, catalog_benefit_line, payment_event; ivr.ivr_decision; entitlement_metering.entitlement_grant, trial_grant, usage_fact; activation_consent.consent_record, service_activation_transition | No update/delete privilege; append-only trigger; stable identity and hash where applicable |

The 14 mutable and 9 append-only S2 tables plus the S1 11/2 and S3 3/5 produce the frozen 28 mutable-head / 16 append-only closure. The S2 split is not a third lifecycle class; payment status, issuance status, reservation status and call status are independent state dimensions.

### 12.6 S2 deletion and retention policy

- No S2 runtime role has `DELETE` or `TRUNCATE`. Every S2 FK is `ON DELETE RESTRICT`; no cascade exists.
- `catalog_product`, `catalog_benefit_line` and `payment_event` are append-only. `catalog_version` retires by `OFFSALE`; only a never-published, unreferenced `DRAFT` is a future deletion candidate, and no deletion is implemented. Published, referenced or historical rows are never hard-deleted.
- Orders, payment facts, trial/grant/balance/usage facts, decisions, consent and activation transitions are retained. Payment status and entitlement issuance remain independently observable; neither is deleted to hide a failure.
- Task and call rows retire by terminal status/time; `UNKNOWN` remains occupied until reconciliation. Evidence/media and sharing lifecycle detail is in §12.7-§12.8; retention periods remain legal/security inputs.
- No S2 table stores a retention period, erasure procedure, backup-deletion rule or legal-hold column. These remain Q3/Q13/Q21 (and later legal/security) inputs; this specification does not guess a duration.
- `entitlement_issuance` and `usage_reservation` retries are operational states, not deletion triggers. A committed `NEW` idempotency row remains an alert and reconciliation input.
- Quarantined call events, over-authorized duration facts, contradictory terminal facts and unknown-use reservations are retained for audit/reconciliation. They are never deleted to make a retry succeed, never silently clamped and never converted into positive usage or evidence without the approved reconciliation outcome.

### 12.7 S3 lifecycle classes

| Class | S3 tables | Rule |
| --- | --- | --- |
| Mutable head | evidence.recording, evidence.evidence_record, sharing.evidence_share_grant | Conditional revision/status transitions; no ordinary hard delete; terminal facts preserved |
| Append-only or frozen | evidence.media_object, evidence.evidence_artifact, evidence.evidence_event, sharing.share_admission, sharing.share_access_event | No update/delete privilege; append-only trigger; stable identity/hash and event identity where applicable |

The three mutable and five append-only S3 tables complete the frozen 28 mutable-head / 16 append-only closure (S1 11/2, S2 14/9, S3 3/5). This is not a third lifecycle class. Recording validation, evidence solidification and share authorization remain independent state dimensions.

### 12.8 S3 deletion and retention policy

- No S3 runtime role has `DELETE` or `TRUNCATE`. Every S3 FK is `ON DELETE RESTRICT`; no cascade exists. Media object, artifact, evidence event, admission and access-event rows are never updated or deleted through ordinary administration.
- Recording and evidence rows retire only by terminal status (`VALIDATED`/`QUARANTINED`; `SOLIDIFIED`/`QUARANTINED`/`FAILED`). A share grant retires by `EXPIRED`/`REVOKED`; revoke removes future access capability but never deletes the evidence, admission or access history. Admission and access-event rows are permanent audit facts for the baseline.
- Revoke/expiry/code rotation invalidates future session issuance/renewal but does not delete or rewrite the original evidence, media object, artifact, admission or prior access events. Already-issued short-lived media credentials are not recalled; their approved TTL defines the bounded stale-access window (Q22).
- No S3 table stores a retention period, erasure procedure, backup-deletion rule, WORM duration, object-lock retention or legal-hold column. Media/evidence retention, object-lock/WORM capability, legal erasure and backup deletion remain Q3/Q11 and their named security/legal/storage owners. A `worm_status` value records an observed capability, not a retention promise.
- Quarantined recordings, non-solid evidence, failed validation facts and denial/failure access events are retained for audit/reconciliation and are never deleted to make a retry succeed or converted into positive evidence/share facts.

## 13. Deterministic Validation Commands

### 13.1 Document-level S1, S2 and S3 checks

Run from the repository root. These are documentation checks only; they do not execute PostgreSQL or migrations. `FILE` is the cumulative S1+S2+S3 specification.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
test -s "$FILE"
grep -q -F 'accessNumberHmac' "$FILE" || { echo "MISSING ACCESS NUMBER HMAC TERM"; exit 1; }
for section in "Scope, Status and Source Relationship" "Database Conventions" "Module and Schema Ownership Matrix" "Complete Table Dictionary" "Foreign-Key Catalog" "Invariant Matrix" "State Machines" "Sensitive-Field Inventory" "Transaction Sequences" "Cross-Schema Function Contracts" "Linear Flyway Migration Order" "Deletion, Retention and Retention-Hook Policy" "Deterministic Validation Commands" "Open Production Inputs" "Goal-Alignment and No-Over-Design Check" "Completion Report Template"; do
  grep -q -E "^## [0-9]+\. $section$" "$FILE" || { echo "MISSING SECTION: $section"; exit 1; }
done
test "$(grep -c -E '^### 4\.[0-9]+ [a-z_]+\.[a-z_]+$' "$FILE")" -eq 44 || { echo "DICTIONARY HEADING COUNT FAILED"; exit 1; }
test "$(grep -c -E '^### 10\.[3-6] (platform|audit)\.[a-z_]+$' "$FILE")" -eq 4 || { echo "S1 FUNCTION CONTRACT COUNT FAILED"; exit 1; }
test "$(grep -c -E '^\| INV-[0-9][0-9] \(S1' "$FILE")" -eq 14 || { echo "S1 INVARIANT EXPANSION COUNT FAILED"; exit 1; }
test "$(grep -c -E '^\| INV-[0-9][0-9] \(S2' "$FILE")" -eq 22 || { echo "S2 INVARIANT EXPANSION COUNT FAILED"; exit 1; }
test "$(grep -c -E '^\| INV-[0-9][0-9] \(S3' "$FILE")" -eq 5 || { echo "S3 INVARIANT EXPANSION COUNT FAILED"; exit 1; }
test "$(awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && NF' "$FILE" | wc -l)" -eq 5 || { echo "FUNCTION WAVE COUNT FAILED"; exit 1; }
test "$(awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && /^platform\.|^audit\./ && /[|]V001[|]/' "$FILE" | wc -l)" -eq 4 || { echo "V001 FUNCTION COUNT FAILED"; exit 1; }
test "$(awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && /entitlement_metering\.issue_first_trial[|]V006[|]/' "$FILE" | wc -l)" -eq 1 || { echo "V006 FUNCTION WAVE FAILED"; exit 1; }
test "$(grep -c '^### 10\.9 entitlement_metering\.issue_first_trial' "$FILE")" -eq 1 || { echo "V006 FUNCTION CONTRACT MISSING"; exit 1; }
# The frozen structural script in 13.3 additionally rejects the legacy
# audit-schema underscore token, the stale FK-mode token and stale baseline
# counts. Those forbidden strings are intentionally not repeated here.
! grep -q -E '4[3] tables|3[5] invariants' "$FILE" || { echo "STALE BASELINE COUNT"; exit 1; }
! grep -q -E 'S2 dep[ei]nds on S3|S2 requ[i]res S3|S1 dep[ei]nds on S[23]|S1 requ[i]res S[23]' "$FILE" || { echo "LATER-SLICE PREREQUISITE"; exit 1; }
test -z "$(grep -n 'section 10\.4' "$FILE")" || { echo "STALE DENYLIST NAVIGATION"; exit 1; }
test -z "$(grep -n 'section 10\.5' "$FILE")" || { echo "STALE FUNCTION-GRANT NAVIGATION"; exit 1; }
test -z "$(grep -n 'organization_access\.membership:<membership_id>' "$FILE")" || { echo "STALE MEMBERSHIP-ID LOCK KEY"; exit 1; }
test -z "$(grep -n 'docs/12 section 8\.3' "$FILE")" || { echo "STALE OWNERSHIP CITATION"; exit 1; }
grep -q 'S2 status is \*\*accepted at checkpoint `8934df9`\*\*' "$FILE" || { echo "STALE S2 STATUS"; exit 1; }
grep -q 'S3 status is \*\*accepted at checkpoint `9ad33e8`\*\*' "$FILE" || { echo "STALE S3 STATUS"; exit 1; }
grep -q 'Navigation correction (F-15' "$FILE" || { echo "MISSING F-15 NAVIGATION NOTE"; exit 1; }
grep -q 'ck_ivr_decision_reason' "$FILE" || { echo "MISSING F-15 NAMED CHECK"; exit 1; }
for term in CLAIM_REPLAY_CONFLICT CLAIM_CONTEXT_CONFLICT CLAIM_ORGANIZATION_OWNERSHIP_UNAPPROVED CLAIM_SUBJECT_NOT_FOUND CLAIM_ACCOUNT_NOT_FOUND CLAIM_ORGANIZATION_NOT_FOUND CLAIM_CONCURRENCY_EXHAUSTED CLAIM_DATA_INTEGRITY BINDING_DISABLED_CONFLICT BINDING_SUBJECT_NOT_FOUND 'pg_catalog.gen_random_uuid' 'subject_number.subject:' 'customer_iam.account:' 'organization_access.membership:' 'docs/12 section 7.2' PURCHASE REJECT_SUSPENDED ACTIVATION_NEXT_ACTION DIRECT_AUTHORIZATION DIRECT_AUTHORIZED 'authorization.calleeNumber' authorizedRecordingSeconds authorizationExpiresAt REJECT_CALL_IN_PROGRESS NO_AVAILABLE_ENTITLEMENT CONSENT_REQUIRED CALL_STATE_UNRESOLVED RECONCILING savepoint select_call_action record_activation_next_action ALREADY_GRANTED TRIAL_ACTION_CONFLICT 'taskMatch = NONE' 'expires_at > clock_timestamp' 'one permanent first trial per subject' 'find_or_create_ivr_subject' 'record_client_activation' 'append_purchase_consent' 'process_call_event' 'settle_call_usage' 'publish_catalog_version' 'publish_trial_policy' 'ALLOCATION_POLICY_UNAVAILABLE' 'ALLOCATION_AMBIGUOUS' 'ALLOCATION_POLICY_INVALID' 'AUTHORIZATION_INPUT_UNAVAILABLE' 'BALANCE_SELECTION_STALE' 'DURATION_EXCEEDS_AUTHORIZED' 'CATALOG_PUBLICATION_CONFLICT' 'TRIAL_POLICY_PUBLICATION_CONFLICT' 'EVENT_ID_CONFLICT' 'CONTRADICTORY_FACT' 'ACTIVATION_CONSENT_DECLINED' 'PURCHASE_CONSENT_REQUIRED' 'IVR_NUMBER_BINDING_DISABLED' 'catalog_order.publication:' 'entitlement_metering.trial_policy:FIRST-TRIAL' 'SOLE_ELIGIBLE' 'schemaVersion' 'INBOUND_RESOLVE = ENTER_CALLEE' 'ACTIVATION_NEXT_ACTION = ENTER_CALLEE' 'ck_ivr_decision_reason' 'ivr.lock_reconciliation_caller' 'operations-iam.lock_employee_administrator'; do
  grep -q -F "$term" "$FILE" || { echo "MISSING S1/S2 CONTRACT TERM: $term"; exit 1; }
done
for term in EVIDENCE_CREATED EVIDENCE_CREATION_CYCLE_VIOLATION 'ORIGINAL = 0' 'ticketExpiresAt' ADMISSION_GRANTED TICKET_ISSUED TICKET_FAILED SHARE_ADMISSION_LIMIT_REACHED SHARE_SESSION_EXPIRED SHARE_SESSION_POLICY_UNAVAILABLE SHARE_RISK_BLOCKED 'sessionExpiresAt' 'share-ticket:' 'share_grant_id + admission_id' 'DEFERRABLE INITIALLY DEFERRED' 'evidence.guard_evidence_record' 'sharing.guard_share_admission' 'source_event_domain, source_event_id' 'recording_id' 'capability_token_hash' 'code_generation' 'revocation_generation' 'admission_no' ORGANIZATION_MEMBER DESIGNATED_MOBILE EXTRACTION_CODE NOT_REQUIRED SHARE_ACCESS 'ticket-issued:' 'ticket-failed:' 'EVIDENCE_SOURCE_CONFLICT' 'source_event_id = eventId' 'evidence-solidified:' 'evidence-quarantined:' 'evidence-failed:' 'evidence-recording-validated:' 'evidence-recording-quarantined:' 'EVIDENCE_RECORDING_VALIDATED' 'EVIDENCE_RECORDING_QUARANTINED' 'EVIDENCE_EVENT_CONFLICT' 'evidence.read_shareable_projection' 'share-observation:' 'evidence.record_upload_observed' 'UPLOAD_OBSERVATION_REQUIRED' 'EVIDENCE_NOT_SOLIDIFIED'; do
  grep -q -F "$term" "$FILE" || { echo "MISSING S3 CONTRACT TERM: $term"; exit 1; }
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

### 13.2.1 S2 per-table dictionary coverage

This `awk` program verifies that each of the 23 S2 dictionary sections exists once and contains the exact accepted column count. It is a document check, not a PostgreSQL check.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
awk '
BEGIN {
  need["catalog_order.catalog_product"]=4; need["catalog_order.catalog_version"]=17; need["catalog_order.catalog_offer"]=7;
  need["catalog_order.catalog_benefit_line"]=6; need["catalog_order.customer_order"]=21; need["catalog_order.payment_order"]=11;
  need["catalog_order.payment_event"]=12; need["ivr.outbound_call_task"]=23; need["ivr.inbound_session"]=17;
  need["ivr.call_record"]=23; need["ivr.call_leg"]=11; need["ivr.call_event"]=14; need["ivr.ivr_decision"]=15;
  need["entitlement_metering.trial_policy_version"]=11; need["entitlement_metering.entitlement_grant"]=13;
  need["entitlement_metering.trial_grant"]=10; need["entitlement_metering.entitlement_balance"]=8;
  need["entitlement_metering.entitlement_issuance"]=14; need["entitlement_metering.usage_reservation"]=16;
  need["entitlement_metering.usage_fact"]=12; need["activation_consent.consent_record"]=19;
  need["activation_consent.service_activation"]=6; need["activation_consent.service_activation_transition"]=18;
}
/^## / { t=""; next }
/^### / {
  if ($0 ~ /^### 4\.[0-9]+ [a-z_]+\.[a-z_]+$/) { t=$3; seen[t]++ } else { t="" }
  next
}
t!="" && /^\| [a-z0-9_]+ \| (uuid|text|timestamptz|integer|bigint|jsonb|boolean) \|/ { c[t]++ }
END {
  bad=0
  for (k in need) {
    if (seen[k]!=1) { print "S2 DICTIONARY COUNT " k " " seen[k]+0; bad=1 }
    if (c[k]!=need[k]) { print "S2 COLUMN COUNT " k " " c[k]+0 " != " need[k]; bad=1 }
  }
  exit bad
}
' "$FILE"
```

Expected: exit 0 with no output. The 23 S2 dictionaries total 308 column rows; together with the 121 S1 rows the cumulative detailed total is 429.

Exact accepted-column-name comparison (catalog field 6 versus dictionary first column) for all 23 S2 tables:

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
ok=1
for t in catalog_order.catalog_product catalog_order.catalog_version catalog_order.catalog_offer catalog_order.catalog_benefit_line catalog_order.customer_order catalog_order.payment_order catalog_order.payment_event ivr.outbound_call_task ivr.inbound_session ivr.call_record ivr.call_leg ivr.call_event ivr.ivr_decision entitlement_metering.trial_policy_version entitlement_metering.entitlement_grant entitlement_metering.trial_grant entitlement_metering.entitlement_balance entitlement_metering.entitlement_issuance entitlement_metering.usage_reservation entitlement_metering.usage_fact activation_consent.consent_record activation_consent.service_activation activation_consent.service_activation_transition; do
  catcols=$(awk -F'|' -v t="$t" '{x=$2; gsub(/[ `]/,"",x); if (x==t) print $6}' "$FILE" | grep -o '`[a-z0-9_]*`' | tr -d '`' | sort -u)
  dictcols=$(awk -v t="$t" '
    $0 ~ "^### 4\\.[0-9]+ " t "$" {f=1; next}
    /^### / {f=0}
    f && /^\| [a-z0-9_]+ \| (uuid|text|timestamptz|integer|bigint|jsonb|boolean) \|/ {
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

Expected: 23 `exact` lines with counts `4,17,7,6,21,11,12,23,17,23,11,14,15,11,13,10,8,14,16,12,19,6,18` (308 total) and `ALL_EXACT`. This proves that no accepted S2 column was dropped, renamed or replaced.

### 13.2.2 S3 per-table dictionary coverage

This `awk` program verifies that each of the eight S3 dictionary sections exists once and contains the exact accepted column count. It is a document check, not a PostgreSQL check.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
awk '
BEGIN {
  need["evidence.media_object"]=11; need["evidence.recording"]=15; need["evidence.evidence_record"]=17;
  need["evidence.evidence_artifact"]=5; need["evidence.evidence_event"]=8;
  need["sharing.evidence_share_grant"]=20; need["sharing.share_admission"]=7; need["sharing.share_access_event"]=11;
}
/^## / { t=""; next }
/^### / {
  if ($0 ~ /^### 4\.[0-9]+ [a-z_]+\.[a-z_]+$/) { t=$3; seen[t]++ } else { t="" }
  next
}
t!="" && /^\| [a-z0-9_]+ \| (uuid|text|timestamptz|integer|bigint|jsonb|boolean) \|/ { c[t]++ }
END {
  bad=0
  for (k in need) {
    if (seen[k]!=1) { print "S3 DICTIONARY COUNT " k " " seen[k]+0; bad=1 }
    if (c[k]!=need[k]) { print "S3 COLUMN COUNT " k " " c[k]+0 " != " need[k]; bad=1 }
  }
  exit bad
}
' "$FILE"
```

Expected: exit 0 with no output. The eight S3 dictionaries total 94 column rows; together with the 121 S1 and 308 S2 rows the cumulative detailed total is 523.

Exact accepted-column-name comparison (catalog field 6 versus dictionary first column) for all eight S3 tables:

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
ok=1
for t in evidence.media_object evidence.recording evidence.evidence_record evidence.evidence_artifact evidence.evidence_event sharing.evidence_share_grant sharing.share_admission sharing.share_access_event; do
  catcols=$(awk -F'|' -v t="$t" '{x=$2; gsub(/[ `]/,"",x); if (x==t) print $6}' "$FILE" | grep -o '`[a-z0-9_]*`' | tr -d '`' | sort -u)
  dictcols=$(awk -v t="$t" '
    $0 ~ "^### 4\\.[0-9]+ " t "$" {f=1; next}
    /^### / {f=0}
    f && /^\| [a-z0-9_]+ \| (uuid|text|timestamptz|integer|bigint|jsonb|boolean) \|/ {
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

Expected: eight `exact` lines with counts `11,15,17,5,8,20,7,11` (94 total) and `ALL_EXACT`. This proves that no accepted S3 column was dropped, renamed or replaced. The separate read-only PowerShell checker also compares each dictionary's fields in order against the accepted plan catalog and expects `FrozenBlocks=9`, `Dictionaries=44`, `Columns=523`, `SliceColumns S1=121/S2=308/S3=94`, `Errors=[]`.

### 13.3 Frozen structural-script-v2 run

Extract the exact accepted structural script and run it unchanged against this document with `FILE` and `FIX2` explicitly set. `FIX2` is the preserved fix-6 artifact; it is a file-presence gate for the frozen baseline, not an S1 content dependency.

```bash
awk '/^# structural-script-v2$/{f=1} f{print} /^# end structural-script-v2$/{exit}' docs/gateflow/a0-production-data-model-schema-spec-plan.md > /tmp/a0-structural-v2.sh
FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0-structural-v2.sh
```

Expected: exit 0; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`. This proves the frozen 44-table baseline, rendered wave/slice membership, FK targets/modes, function waves and key sets. It does not prove dictionary field order (checked separately in §13.2-§13.2.2), PostgreSQL execution, COS/Redis behavior or production behavior.

### 13.4 Scope, hash and cleanliness checks

```bash
sha256sum docs/gateflow/a0-production-data-model-schema-spec-plan.md
# expected: 149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5
sha256sum docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md
# expected: 8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59
git status --porcelain=v1 | grep -E '\.sql$|db/migration/' && exit 1 || true
# New artifacts are untracked or modified, so a global git diff --check is not
# the authoritative gate. The targeted blank-at-EOL scan below is equivalent.
test -z "$(grep -n -E '[[:blank:]]+$' docs/14-production-data-model-and-flyway-schema-spec.md docs/gateflow/a0-production-data-model-schema-spec-s3-implementation.md docs/gateflow/a0-production-data-model-schema-spec-s2-fix-4.md docs/gateflow/a0-production-data-model-schema-spec-s2-fix-3.md docs/gateflow/a0-production-data-model-schema-spec-s2-fix-2.md docs/gateflow/a0-production-data-model-schema-spec-s2-fix-1.md docs/gateflow/a0-production-data-model-schema-spec-s2-implementation.md docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md docs/gateflow/a0-production-data-model-schema-spec-s1-fix-1.md)"
# Whole-new-file diff comparison: exit 1 with zero output diagnostics is a
# content difference, not a whitespace failure.
git diff --no-index --check /dev/null docs/14-production-data-model-and-flyway-schema-spec.md >/tmp/a0-s2-ws.out; rc=$?; test "$rc" -eq 1 && test ! -s /tmp/a0-s2-ws.out
git diff --no-index --check /dev/null docs/gateflow/a0-production-data-model-schema-spec-s2-fix-1.md >/tmp/a0-s2-fix1-ws.out; rc=$?; test "$rc" -eq 1 && test ! -s /tmp/a0-s2-fix1-ws.out
git diff --no-index --check /dev/null docs/gateflow/a0-production-data-model-schema-spec-s2-fix-2.md >/tmp/a0-s2-fix2-ws.out; rc=$?; test "$rc" -eq 1 && test ! -s /tmp/a0-s2-fix2-ws.out
git diff --no-index --check /dev/null docs/gateflow/a0-production-data-model-schema-spec-s2-fix-3.md >/tmp/a0-s2-fix3-ws.out; rc=$?; test "$rc" -eq 1 && test ! -s /tmp/a0-s2-fix3-ws.out
git diff --no-index --check /dev/null docs/gateflow/a0-production-data-model-schema-spec-s2-implementation.md >/tmp/a0-s2-art-ws.out; rc=$?; test "$rc" -eq 1 && test ! -s /tmp/a0-s2-art-ws.out
git diff --no-index --check /dev/null docs/gateflow/a0-production-data-model-schema-spec-s3-implementation.md >/tmp/a0-s3-art-ws.out; rc=$?; test "$rc" -eq 1 && test ! -s /tmp/a0-s3-art-ws.out
```

Expected: plan and fix-6 hashes unchanged; no `.sql` or `db/migration` path in `git status`; zero trailing-whitespace lines in the current specification and the S1/S2/S3 implementation artifacts including fix-2, fix-3 and fix-4; each whole-new-file whitespace check exits 1 with no diagnostic output (the comparison-status exit is expected for a new file). The current S3 specification ends with exactly one LF and contains no CR or blank line at EOF. A global `git diff --check` is intentionally not used because the pre-existing, out-of-scope `logo/preview.html` change contains trailing whitespace. Pre-existing modified QA screenshots, `logo/preview.html` and the unrelated `docs/reviews/plan-review-20261021-205118.md` are outside this work unit and must remain untouched.

### 13.5 S1 control-coverage assertions

These checks prove that every S1 dictionary names its lifecycle, sensitive representation, revision/immutability or append-only control, and that the S1 FK detail has exactly 11 rows. They are document checks only.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
test "$(sed -n '/^### 4.3 /,/^### 4.16 /p' "$FILE" | grep -c '^Purpose:.*Lifecycle:')" -eq 13 || { echo "S1 LIFECYCLE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.3 /,/^### 4.16 /p' "$FILE" | grep -c '^Sensitive representation:')" -eq 13 || { echo "S1 SENSITIVE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.3 /,/^### 4.16 /p' "$FILE" | grep -c -E '^(Revision/immutability|Append-only control):')" -eq 13 || { echo "S1 REVISION/IMMUTABILITY COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 5.3 S1 foreign-key detail/,/^### 5.4 /p' "$FILE" | grep -c '| FK |')" -eq 11 || { echo "S1 FK DETAIL COUNT FAILED"; exit 1; }
test "$(sed -n '/^### 8.2 S1 sensitive-field detail/,/^### 8.3/p' "$FILE" | grep -c '^| ')" -ge 20 || { echo "S1 SENSITIVE DETAIL FAILED"; exit 1; }
```

Expected: exit 0. The exact S1 FK count is 11; the S1 sensitive detail covers every S1 table that stores a protected value and explicitly marks the others as non-sensitive.

### 13.6 S2 control-coverage assertions

These checks prove that every S2 dictionary names its lifecycle, sensitive representation, revision/immutability or append-only control, and that the S2 FK detail has exactly 54 rows. They are document checks only.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
test "$(sed -n '/^### 4.16 /,/^### 4.39 /p' "$FILE" | grep -c '^Purpose:.*Lifecycle:')" -eq 23 || { echo "S2 LIFECYCLE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.16 /,/^### 4.39 /p' "$FILE" | grep -c '^Sensitive representation:')" -eq 23 || { echo "S2 SENSITIVE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.16 /,/^### 4.39 /p' "$FILE" | grep -c -E '^(Revision/immutability|Append-only control):')" -eq 23 || { echo "S2 REVISION/IMMUTABILITY COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 5.4 S2 foreign-key detail/,/^### 5.5 /p' "$FILE" | grep -c -E '\| (FK|ALTER_FK|NO_FK) \|')" -eq 54 || { echo "S2 FK DETAIL COUNT FAILED"; exit 1; }
test "$(sed -n '/^### 8.5 S2 sensitive-field detail/,/^## 9\./p' "$FILE" | grep -c '^| ')" -ge 23 || { echo "S2 SENSITIVE DETAIL FAILED"; exit 1; }
test "$(grep -c '^### 10\.9 entitlement_metering\.issue_first_trial' "$FILE")" -eq 1 || { echo "V006 FUNCTION CONTRACT FAILED"; exit 1; }
test "$(grep -c '^### 9\.11 T-S2-01' "$FILE")" -eq 1 || { echo "S2 ACTIVATION SEQUENCE MISSING"; exit 1; }
```

Expected: exit 0. The exact S2 FK count is 54; the S2 dictionary coverage is 23/23/23 with one lifecycle, one sensitive representation and one revision/append-only statement per table. The range endpoints stop before the S3 insertion at §4.39 and bound the S2 FK detail to §5.4-before-§5.5, so only §4.16-§4.38 S2 coverage is counted; the expected counts are not weakened.

### 13.7 S2 behavior test matrix

These are document-level requirements for the later executable suite. They are not executed tests and do not claim PostgreSQL, IVR, payment-provider or production behavior.

| ID | Assertion | Expected outcome | Class |
| --- | --- | --- | --- |
| T-S2-01 | Create an order with a fresh `PURCHASE` key; replay the same key and semantic input. | Exactly one order and one completed idempotency result; replay returns the first immutable order and snapshot. | DB+TX |
| T-S2-02 | Reuse the `PURCHASE` key with changed amount, subject, offer, version token or consent. | Stable conflict; no second order, no overwritten snapshot. | DB+TX |
| T-S2-03 | Apply a verified payment event, an unverified event and a duplicate verified event. | Only the verified event advances payment; a duplicate returns the first processing result; unverified never advances. | DB+TX |
| T-S2-04 | Pay a multi-line/multi-installment order with one benefit failing once, then retry, using validated approved installment/schedule inputs. | One durable issuance row per order/installment/type; each grant copies the matching frozen snapshot schedule entry (units/validity window) and never current catalog state; a missing/ambiguous schedule fails with `attempt_count`/`last_error_code` and no grant; retry reaches `ISSUED` without duplicating a grant. | DB+TX+SERVICE |
| T-S2-05 | Grant the first trial, then change policy version, channel, campaign key, claim, re-entry and replay the action. | Exactly one permanent `FIRST-TRIAL` per subject; policy/version/channel/claim changes never reissue; no second grant or balance. | DB+TX |
| T-S2-06 | Invoke first-trial with the same action and fingerprint, same action and changed fingerprint, and a different action. | `REPLAY` original ids; `TRIAL_ACTION_CONFLICT`; `ALREADY_GRANTED` original ids. No new grant/balance in any case. | DB+TX |
| T-S2-07 | Force a failure inside the activation transaction after trial/grant/balance/Outbox writes. | All trial/grant/balance/Outbox and the `NEW` idempotency row roll back together; no orphan trial/balance. | TX |
| T-S2-08 | Create tasks for the same canonical caller across different accounts/workspaces. | At most one `PENDING`/`CLAIMED` task globally for that caller; a second live task fails closed and does not disclose the callee. | DB+TX |
| T-S2-09 | Retry a task request with the same key/input and with a changed input. | Same key/input returns the existing task without refreshing `expires_at`; changed input conflicts. An identical tuple may return the existing task without TTL refresh. | DB+TX |
| T-S2-10 | Claim at 29.999 seconds, at exactly 30 seconds, and after 30 seconds; then create a new task. | 29.999 succeeds if database time is before expiry; 30 or later fails; stale `PENDING` is archived to `EXPIRED` under the caller lock; no configured TTL is invented. | DB+TX |
| T-S2-11 | Replace or cancel a `PENDING` task owned by another workspace/account; then try a `CLAIMED` task. | Stable ownership rejection with no mutation and no callee disclosure; `CLAIMED` cannot be replaced or cancelled. The owner's `PENDING` replace/cancel wins atomically. | DB+TX |
| T-S2-12 | Present a current call whose own record is `ACTIVE`/`UNKNOWN` with no other call. | The current `callId` is excluded from the guard; the call proceeds and never blocks itself. | TX |
| T-S2-13 | Present another `ACTIVE`/`UNKNOWN` call for the same caller while the current call is absent or never-started `ACTIVE`. | `REJECT_CALL_IN_PROGRESS`, no claim/reservation, valid terminal `REJECTED` current parent; the competing call and its decision are untouched. | DB+TX |
| T-S2-14 | Resolve a valid pending task with positive entitlement. | One local transaction: conditional `PENDING → CLAIMED` with `claimed_call_id = call_record.call_id`, exactly one `call_record.task_id` assignment from NULL to that task, reciprocal equality, current `ACTIVE` call binding, one reservation, one frozen `DIAL_TASK_CALLEE` decision; all commit together and the pre-savepoint call is not duplicated. | DB+TX |
| T-S2-15 | Lose the conditional claim/link (expiry/revision), then lose a reservation race; retry the same task from the same pre-savepoint call and from a new call. | A 0-row claim/link causes one bounded reread and a non-dial decision; reservation denial rolls back to the same-connection savepoint, which clears both `claimed_call_id` and the call's `task_id` while the pre-savepoint call survives `task_id = NULL`; a retry within the task TTL may claim/link the same task again; no `CLAIMED` task without reservation and no arbitrary relink, task switch or second call. | TX |
| T-S2-16 | Replay the same call/purpose fingerprint; then send a new event ID with the same confirmation fingerprint. | Replay returns the first decision/action; the new event completes with the existing decision ID and repeats no consent/trial/claim/reservation/Outbox. The confirmation event ID is validated nonblank 1-128 before `idempotency_begin`: a 128-character ID is accepted, and a blank or 129-character ID is refused with `IVR_ARGUMENT_INVALID` and no idempotency/domain write while the general 1-512 platform key is unchanged. | DB+TX |
| T-S2-17 | Reuse the same event ID with changed consent/prompt/content, and a new event ID with a changed fingerprint; between attempts replace, revoke or rebind the captured caller binding. | Stable conflict; a binding replacement/revocation/rebinding changes the §10.17 binding/subject identity and conflicts before any frozen decision is returned; any `NEW` row inserted for the attempt is rolled back; no accepted effect. | DB+TX |
| T-S2-18 | Reuse the same `IVR_CONFIRM` event ID for another `callId`. | Stable conflict because `callId` is in the fingerprint; no second activation/trial/dial. | DB+TX |
| T-S2-19 | Replay initial resolve with the same fingerprint after a task would have expired; repeat with changed caller/access/node/session/protocol identity and after a binding replacement, revocation or rebinding. | Replay of the exact same call/session/binding/access identity returns the frozen first result before expiry housekeeping; a changed binding/subject/session/protocol/node identity is a stable conflict and mutates nothing; a different canonical access number whose display mask is identical is a stable conflict before frozen replay because the fingerprint uses the ephemeral `accessNumberHmac`, not `accessNumberMasked`, and missing/invalid HMAC input fails closed with no mutation; a stale resolve decision is never reused after rebinding. | DB+TX |
| T-S2-20 | Inject a fault into any shared IVR predicate and run both initial resolve and post-activation selection. | Both entries change consistently; no predicate exists only in one wrapper; `ivr.select_call_action` is the single implementation. | SERVICE |
| T-S2-21 | Direct authorization with a task ID, with a remaining `PENDING`/`CLAIMED` task, with a task-backed call, or without either valid current purpose chain (direct inbound `ENTER_CALLEE` or `GUIDE_ACTIVATION` + `CONFIRMED` + `ACTIVATION_NEXT_ACTION = ENTER_CALLEE`). | Rejected; never falls through to the task path, never dials, never creates a reservation; an obsolete earlier `ENTER_CALLEE` superseded by a later no-dial decision is refused. | DB+TX |
| T-S2-22 | Successful direct authorization after either valid current purpose chain (F-16). | Purpose `DIRECT_AUTHORIZATION`, `action` null, `taskMatch = NONE`, null task fields, committed reservation/seconds/cutoff, callee only under encrypted `authorization.calleeNumber`; no `ENTER_CALLEE` is returned after confirmation and the original resolve decision is never rewritten. | DB+TX |
| T-S2-23 | Direct authorization refusal (no entitlement/access/suspended/competing call). | One permitted non-dial action, no reservation, no authorization object; no task claim. | DB+TX |
| T-S2-24 | Replay a `DIAL_TASK_CALLEE`/direct authorization at or after `authorizationExpiresAt`; lose the response and retry the direct authorization with a regenerated transport `IVR_DIRECT_AUTH` event ID and identical semantics. | Same frozen cutoff and payload return; cutoff is never renewed; no second B-leg or reservation; a regenerated event ID with identical semantics replays the first authorization; changed session/protocol/callee/prompt/key/content conflicts; local `callId` operation recovers the original. | SERVICE |
| T-S2-25 | Fresh confirmation of a `SUSPENDED` subject with no prior trial, and with an existing trial. | `REJECT_SUSPENDED`; no accepted consent/transition/trial/grant/balance/reservation/Outbox; existing trial is not reissued; activation stays suspended. | DB+TX |
| T-S2-26 | Round-6 write-set assertion: a fresh suspended/in-progress category-1 refusal targets a current session that has never been confirmed, while an already-`CONFIRMED` session, a current `UNKNOWN` call and unrelated calls exist. | Only the never-confirmed current session may be marked `REJECTED`; the already-`CONFIRMED` session, current `UNKNOWN` row and unrelated calls remain byte-for-byte unchanged. The full §10.15 category-1 write set is asserted, not an abbreviated one. | DB+TX |
| T-S2-27 | Initial resolve on an `ACTIVE` activation with missing/expired current consent; then confirm with current consent. | `GUIDE_ACTIVATION`/`CONSENT_REQUIRED` and no task/reservation; the confirmation appends current consent and returns the existing trial (`ALREADY_GRANTED`/`REPLAY`) rather than a second trial. | DB+TX |
| T-S2-28 | Confirmation with stale prompt/policy version/key/content. | `REJECT`/`CONSENT_REQUIRED` before any accepted consent/transition/trial append; no accepted effect. | DB+TX |
| T-S2-29 | Confirmation transitions and an operations suspend/resume. | Every transition has unique action/`ACTIVATION_STATUS` event identity and fingerprint; `to_revision = from_revision + 1`; concurrent transitions have one winner; operations resume never calls `issue_first_trial`. Both suspend and resume call the operations-iam-owned `operations-iam.lock_employee_administrator` at rank 13 before the owner-local activation write; that collaborator takes the administrator advisory lock and employee row under `operations_iam_role`, writes/appends nothing, and `activation_consent_role` receives no `operations_iam` schema/table/row-lock privilege. The owner service rejects a non-`OPERATIONS` `ACTIVE → SUSPENDED`, an `OPERATIONS` `INACTIVE → ACTIVE`, and any client/IVR suspend or resume, even where the broad `ck_activation_transition_matrix` shape admits the row (OQ-7). | DB+TX |
| T-S2-30 | Review an already-`CONFIRMED` session after operations suspension; repeat a fresh confirmation. | Replay returns the original decision and the session stays `CONFIRMED`; a fresh suspended path rejects only a never-confirmed session. | DB+TX |
| T-S2-31 | Race an access revocation against a task-backed confirmation in both serial orders. | Revocation first: category-2 no-dial access rejection with valid activation/trial retained, `CONFIRMED` session, no claim/reservation. Confirmation/claim first: frozen `DIAL_TASK_CALLEE` with claim/reservation survives; later revocation governs later calls only. No post-commit reread or reversal. | TX |
| T-S2-32 | Expected reservation denial after a tentative claim but after valid activation/trial. | Roll back to the same-connection savepoint; one no-dial decision commits; activation/trial and `CONFIRMED` session remain; no claim/reservation. | TX |
| T-S2-33 | Force a technical error at each write boundary (claim, reservation, decision, completion, Outbox). | Every write including the `NEW` row rolls back; no decision/completion commits; bounded retryable error; no swallowed database exception. | TX |
| T-S2-34 | Concurrent reservations and settlements on one selected balance, with multiple/expired/future/cross-subject candidates; add a concurrent reconciliation and settlement against the same reservation/balance. | `reserved + consumed <= granted` always; the conditional revision update affects one row; no overcommit, negative balance, cross-subject debit or second-balance fallback; an expired/future/fully reserved balance is never selected; a single-unambiguous candidate is debited once; absent/ambiguous policy fails closed; settlement and every locked reconciliation phase acquire the balance row(s) in ascending UUID before the reservation, so the two orders serialize without deadlock and reconciliation never holds a reservation lock while later acquiring a balance. Both reconciliation phases A and C obtain rank 1 only through `ivr.lock_reconciliation_caller` under `ivr_role`, which reads the caller HMAC, takes no row lock and writes/appends/enqueues nothing; the entitlement owner takes no direct `ivr.call_record` read or lock. | DB+TX |
| T-S2-35 | Partial use settlement and a second terminal event. | Actual units settle once, the remainder releases once, one usage fact exists; a repeat returns the first settlement and does not recharge. | DB+TX |
| T-S2-36 | Missing, late, duplicate, zero, excess and unknown end facts (OQ-8/aggregate F-03/F-04); make the authoritative IVR query slow, timed out and unavailable. | Reconciliation runs the §9.16 three phases: phase A commits `HELD → RECONCILING` in a short transaction and releases all locks, phase B queries the authoritative IVR with no PostgreSQL or advisory lock held, and phase C relocks in the rank-12 suborder (balance rows ascending UUID, then reservation) and re-reads before converging. Both phases A and C obtain the rank-1 canonical-caller lock only through `ivr.lock_reconciliation_caller` under `ivr_role`; that collaborator reads the existing caller HMAC, takes no row lock and writes/appends/enqueues nothing, while the entitlement owner discovers and locks only its own balance/reservation rows. Missing known-end facts settle before release; late events converge once; duplicates return the first settlement; known zero use releases once with no positive usage fact; reported use above `authorized_units` stays `RECONCILING` with `DURATION_EXCEEDS_AUTHORIZED` and no settle/clamp/release/fact; unknown/slow/unavailable authority stays `RECONCILING` with durable attempt/due/error facts and never releases. A settlement that reaches the granted units sets `EXHAUSTED` in the same conditional balance `UPDATE` as its counters, and an excess/unknown outcome writes no balance counter or status. | DB+TX+SERVICE |
| T-S2-37 | Deliver a non-authoritative terminal end event, then a current call `UNKNOWN` with an inconsistent fresh attempt, then an authoritative terminal reconciliation. | The initial non-authoritative terminal event persists `call_record.status = 'UNKNOWN'` with the stable reason only and `ended_at`/`authoritative_seconds` NULL, satisfying `ck_call_record_terminal_shape` and `ck_call_record_authoritative_seconds` without a CHECK failure; the fresh attempt returns `CALL_STATE_UNRESOLVED` with no destructive change, reservation or dial; only the authoritative terminal reconciliation moves `UNKNOWN` to `ENDED`/`FAILED` with the accepted `ended_at`, `end_reason` and `authoritative_seconds`; it never converges to `REJECTED` and never releases. | DB+TX |
| T-S2-38 | Early refusal before task evaluation; then `ENTER_CALLEE`/direct authorization with no task. | Early refusal reports `taskMatch = NONE` with null task fields and makes no "no task exists" claim; `ENTER_CALLEE`/direct requires a positive no-`PENDING`/`CLAIMED` check. | DB+TX |
| T-S2-39 | Inspect the frozen snapshot for `DIAL_TASK_CALLEE`, direct success and direct refusal. | Exact non-null reservation/seconds/cutoff for the first two; direct success has `action` null and no task fields; direct refusal has no reservation/authorization; redacted snapshots contain no full callee; `response_sha256` matches. | DB+SERVICE |
| T-S2-40 | Effective-entitlement routing snapshot versus spend authorization. | `availableRecordingSeconds` is computed from the declared `entitlement_balance.granted_units`/`reserved_units`/`consumed_units` plus the grant validity window under the same approved allocation policy as reservation; the routing predicate reads `entitlement_grant.granted_units` to evaluate the documented equality (OQ-2), while the reservation rechecks it under the locked balance; it is routing-only and may be zero; only the committed reservation's authorized units govern; a concurrent balance change cannot make the snapshot a spend authorization and never causes a different balance to be debited. | TX |
| T-S2-41 | No cross-owner write or row-lock privilege; schema `USAGE` (F-01/R-02/R-03/R-04/aggregate F-05). | Every S2 role can execute every declared cross-module `SELECT` only with the schema `USAGE` and exact column grant in §10.12, including `activation_consent_role`'s account/organization/membership/subject-ownership reads, `catalog_order_role`'s issuance/grant projection reads and its purchase-consent validation read of the exact §10.12 columns including `source_ref` (the order-side `event_id = source_ref = purchaseConsentEventId` check executes); no S2 role can insert/update/delete another module's table, `CREATE` in a schema, or take a cross-owner `FOR SHARE`/`FOR UPDATE`; `ivr_role` cannot lock `service_activation`, while `activation-consent.lock_service_activation` holds that row lock under the owner role; `activation_consent_role` cannot read or lock `operations_iam.employee_account`, while `operations-iam.lock_employee_administrator` takes the administrator advisory lock and employee row under `operations_iam_role` and writes nothing; owner services run on the same connection under the owner role; reconciliation phases A and C obtain the canonical-caller lock only through `ivr.lock_reconciliation_caller` under `ivr_role`, `entitlement_metering_role` cannot read `ivr.call_record` or lock IVR rows, and that collaborator writes/appends/enqueues nothing; no raw unique error is a protocol success. | DB+TX |
| T-S2-42 | Function creation and grants (F-02). | Only V006 creates `issue_first_trial` after its four tables; V001-V005 never create it; EXECUTE is limited to the two runtime roles; the NOLOGIN `entitlement_metering_owner` has the internal `platform` `USAGE` and `outbox_append` EXECUTE needed for the nested call, and no runtime/`PUBLIC` role gets those capabilities; fixed `search_path`; no transaction control; forced nested-Outbox failure rolls back atomically. | DB |
| T-S2-43 | No S2 prerequisite on S3. | V004-V007 migrate and their listed behavior is testable without any V008/V009 table, function or service. The frozen S3 membership remains planned only. | Structural |
| T-S2-44 | IVR behavior boundaries. | The IVR never creates an order or claims payment; purchase guidance only. A direct no-task caller can activate/trial/record without customer registration, login, enterprise membership or real-name. A matched task is taken once and auto-dials the frozen callee without asking the caller to confirm the number. | SERVICE |
| T-S2-45 | Call parent and FK timing. | `call_record.call_id` is the canonical parent for task/session/legs/events/decision/reservation/usage; `inbound_session.call_id` and `call_record.call_id` are the two `NO_FK` roots; `claimed_call_id`, decision reservation and purchase consent FKs are added only in their later same-wave `ALTER` steps. The task/call link is reciprocal and exact: the current call is created before the savepoint with `task_id = NULL`; the tentative claim sets `outbound_call_task.claimed_call_id` and assigns `call_record.task_id` once from NULL to that task under the existing IVR guard/UPDATE grant; success requires `task.claimed_call_id = call.call_id` and `call.task_id = task.id`; no arbitrary relink, task switch or second call exists. | DB |
| T-S2-46 | Action enum and `DIRECT_AUTHORIZATION` purpose. | Exactly seven actions; `DIRECT_AUTHORIZATION` is a purpose; a successful direct authorization has `action` null; there is no eighth action and no direct success as `ENTER_CALLEE` after confirmation. | DB+TX |
| T-S2-47 | Order snapshot and entitlement-status absence. | The order stores no entitlement status; a later catalog publication/offsale never changes the frozen snapshot or amount; issuance is the only per-line state. | DB+SERVICE |
| T-S2-48 | Payment `UNKNOWN` checkout. | A second checkout is blocked while an existing order is `UNKNOWN`; querying the original order resolves the state; a duplicate callback does not create a second order or entitlement. | DB+TX |
| T-S2-49 | Historical trial and policy change. | An existing trial/grant keeps its frozen policy snapshot and expiry after a new policy version is enabled; a later first-trial attempt is not reissued. For OQ-6, the V006 `issue_first_trial` function performs the sole authoritative enabled/published/expected-version read and retains its immutable content; replay/already-granted returns prior ids before any policy selection; a disable committed before that read is observed (missing/disabled/stale fails closed with `TRIAL_POLICY_NOT_FOUND`), while a disable committed after that read does not revoke the already-selected trial; the wrapper lookup is preliminary and there is no second enabled recheck. | DB+TX |
| T-S2-50 | B-leg reproducibility. | The IVR persists one local `callId` B-leg operation before the line request; a lost response/unknown line outcome recovers or reconciles that operation and never issues a second dial; business `ivr.call_leg` records facts only. | SERVICE |
| T-S2-51 | Privilege allow/deny/nested owner (F-01/F-02/F-03/R-03/R-04/OQ-2/aggregate F-05). | Each S2 role can execute every declared cross-module `SELECT` (schema `USAGE` plus exact column grant) and its granted functions; `activation_consent_role` can read the declared account/organization/membership/subject-ownership columns, and `catalog_order_role` can read the declared issuance/grant projection columns and the declared purchase-consent validation columns including `source_ref`, executing the read-only `event_id = source_ref = purchaseConsentEventId` check without a foreign row lock; every foreign write, foreign row lock, `DELETE`, `TRUNCATE`, schema `CREATE` and unlisted `consent_record` column read is denied; `activation_consent_role` has no `operations_iam` schema USAGE, table privilege or row-lock privilege, while `operations-iam.lock_employee_administrator` runs under `operations_iam_role`, takes only the administrator advisory lock and authenticated employee row, and writes/appends/enqueues nothing. The NOLOGIN `entitlement_metering_owner` has only internal platform `USAGE`/`outbox_append` EXECUTE and the first-trial nested Outbox append succeeds; forced nested failure rolls it back with the caller. `ivr_role` can read `entitlement_balance.granted_units` and `entitlement_grant.granted_units` and compute available seconds. Reconciliation phases A and C take the canonical-caller lock only through `ivr.lock_reconciliation_caller` under `ivr_role`; `entitlement_metering_role` cannot read or lock `ivr.call_record`, and the collaborator writes/appends/enqueues nothing. | DB |
| T-S2-52 | Balance selection and authorization (F-04). | One eligible balance with an approved `SOLE_ELIGIBLE` policy is selected and debited exactly once; zero eligible returns `NO_AVAILABLE_ENTITLEMENT`; two eligible with no single-candidate policy returns `ALLOCATION_AMBIGUOUS`; absent/unapproved policy returns `ALLOCATION_POLICY_UNAVAILABLE`; none of these claims a task, creates a reservation or debits a balance. A cross-subject balance ID, future grant, expired grant, `status <> AVAILABLE`, fully reserved balance or stale conditional update is a typed denial with no debit. `authorizedUnits = min(present approved request/cap, available)` and is positive; absent/invalid bounds return `AUTHORIZATION_INPUT_UNAVAILABLE` with no invented default. Routing and reservation select the same balance; a changed balance between them yields a no-dial denial. | DB+TX |
| T-S2-53 | Order client-key boundary (F-13/OQ-1). | 74/75/128/475-character client keys with a canonical 36-character account UUID plus colon are accepted; the 475-character key produces a 512-character platform key and creates the order, and each accepted key derives an 81-character `purchaseConsentEventId` (`purchase-consent:` + lowercase SHA-256) that fits the consent `event_id`/`source_ref` 1-128 contract. 476 and 512-character client keys are rejected before any order/consent/idempotency row, and the same key reproduces the same consent identity. The check matches `ck_customer_order_idempotency` and the platform 1-512 key contract. | DB+TX |
| T-S2-54 | MINIAPP/WEB activation and trial (F-05/R-03/R-08). | A fresh accepted activation appends one current `ACCOUNT`/`MINIAPP`/`WEB` consent with the exact `SERVICE_ACTIVATION` fingerprint, one `INACTIVE → ACTIVE` transition and one trial outcome in one transaction; the service re-reads current account/organization/membership/subject ownership with the exact §10.12 grants and takes no foreign row lock; `source_ref` is exactly `activation:` + lowercase SHA-256 of `{accountId, activationEventId}` (75 characters) for MINIAPP/WEB and `ivr-session:` + canonical lowercase session UUID (48 characters) for the IVR consent, independent of the transport event ID; the first accepted IVR `SERVICE_ACTIVATION` consent stores `event_domain = 'IVR_CONFIRM'` and `event_id` exactly equal to the validated confirmation `eventId`; the activation event ID is validated nonblank 1-128 before any effect; the transition fingerprint binds the immutable consent fingerprint, so a changed policy/prompt/source with the same stable action event conflicts and is not replayed from a generated consent ID or current revision; a regenerated IVR confirmation event ID with identical semantics replays the first IVR decision and returns the first consent before any append without rewriting the original consent `event_id` or adding a consent; same event/fingerprint replays the first ids without a second consent/transition/trial; already-`ACTIVE` appends no duplicate transition and returns `ALREADY_GRANTED`/`REPLAY`; missing/stale/declined consent returns a typed refusal with no accepted append; concurrent confirmations have one winner; no login identity/owner is created. | DB+TX |
| T-S2-55 | Purchase consent (F-07/OQ-1). | Missing, declined, stale policy/prompt, wrong subject/account/channel/purpose/source, or an already-consumed event yields a typed denial and no order; a valid explicit `ACCEPT` appends exactly one `PURCHASE` consent and the order stores its ID; 74/75/128/475-character client keys each derive the same fixed 81-character `purchaseConsentEventId`, used for both the consent `event_id` and `source_ref`, and a 476-character key is rejected before any effect; Unicode/exact-key input is hashed without normalization or case folding, so a composed/decomposed or differently cased key is a different identity; same event/fingerprint replays without a second consent; a same-order-key/semantic retry reproduces the same deterministic `purchaseConsentEventId` and fingerprint (no truncation, random regenerated identity or caller-chosen alternative); changed semantic purchase data still conflicts under the existing fingerprint; the order cannot reference another subject's or another purpose's consent; the order-side validation of `purpose`/`subject_id`/`channel`/`source_type`/`account_id`/current policy/prompt/`event_domain`/`event_id = source_ref = purchaseConsentEventId`/`action`/`request_fingerprint` executes under the exact §10.12 `catalog_order_role` `consent_record` `SELECT` including `source_ref`, read-only with no foreign row lock, and reads of unlisted `consent_record` columns or any foreign write are denied; no raw CHECK/unique violation is exposed as success; consent and order roll back together on forced failure. | DB+TX |
| T-S2-56 | Call-event ingestion, duplicate, reorder and settlement handoff (F-08/F-10/R-06/R-07/C-01/C-02). | The event ID is validated nonblank 1-128 before any idempotency/domain effect; an unknown parent call returns stable `CALL_UNKNOWN` with no `call_event`/call/leg/settlement write and no raw FK; a known call with an unknown leg may persist one `QUARANTINED` `LEG_UNKNOWN` receipt; ingestion acquires the owned `inbound_session` then current `call_record` locks at rank 4 before sequence detection/insert; `payload_redacted` has exactly the §4.27 `source`/`receipt` shape, `payload_sha256` hashes only `source.payload`, the fingerprint is recomputed from that digest plus immutable inputs, and the stored `receipt` metadata is excluded from the hash and cannot be supplied or bypassed by a client; a duplicate event with the same stored fingerprint returns the first terminal outcome and creates no second row/usage fact; a changed fingerprint/type/sequence always returns a stable conflict and is never applied, including after a first `SEQUENCE_CONFLICT` outcome; a same-semantic sequence-conflict retry returns the first `QUARANTINED`/`SEQUENCE_CONFLICT` outcome reconstructed from the original sequence in the immutable payload even when the indexed `sequence_no` is `NULL`; a changed re-delivery after `APPLIED` leaves `APPLIED` and the call/leg/settlement facts unchanged; an out-of-order consistent event converges once; a terminal-reversing/contradictory terminal event is quarantined; a non-authoritative terminal receipt persists `UNKNOWN` with the stable reason and `ended_at`/`authoritative_seconds` NULL so both call-record CHECKs hold; a known end within `1..authorized_units` settles once and releases the remainder; known zero releases once with no positive usage fact; a report above `authorized_units` stays `RECONCILING` with no settle/release/fact; `UNKNOWN` stays occupied and never releases; no double settlement and no cross-owner DML. | DB+TX+SERVICE |
| T-S2-57 | Catalog publication/offsale race (F-09/R-01). | With a pre-existing `ACTIVE` version, the winner first retires it to `OFFSALE` with revision +1 and then sets the candidate `published_at`/`ACTIVE` with revision +1, so exactly one `ACTIVE` row exists at every statement and the immediate partial unique never rejects the normal second publication; two concurrent publications for one product with the same expected current version have one winner; the stale loser returns `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW` and changes nothing; existing orders/grants/snapshots are byte-identical; a later offsale stops new orders only; no raw unique error is exposed. | DB+TX |
| T-S2-58 | Semantic fingerprint and Outbox replay (F-11/aggregate F-01/F-02). | For each §10.17 scope, changing an included field changes the fingerprint and conflicts; changing an excluded field (transport event ID, timestamps, mutable status/balance/reservation results) does not; the direct scope includes session/protocol/access identity and excludes `confirmationEventId`, while the resolve/confirmation scopes include session/binding/subject identity; the direct, resolve and confirmation scopes include the ephemeral `accessNumberHmac` rather than the display-only `accessNumberMasked`, so a different canonical access number with an identical mask conflicts before frozen replay and missing/invalid HMAC input fails closed, and these HMAC inputs are never persisted, logged or emitted; a new event ID with the same semantic confirmation or direct-authorization fields replays the first decision; a binding replacement/revocation/rebinding conflicts before frozen replay; each S2 fact appends its exact Outbox domain/dedupe key/aggregate/type/payload hash; replaying the fact returns the existing Outbox ID and appends no duplicate; same key/different hash raises `OUTBOX_DEDUPE_CONFLICT`; every persisted payload field is readable under the declared grants. | DB+TX |
| T-S2-59 | Frozen commerce/schedule snapshots (F-12). | A multi-installment order copies the validated approved per-installment/type/units/window schedule into the immutable snapshot; issuance reads the snapshot, not current catalog state; a later catalog offsale/publication/policy change does not alter the order or prior grant; a missing/ambiguous/unsupported schedule fails closed with no guessed grant while the payment fact remains observable; a snapshot/schedule/type/quantity/window mismatch fails issuance; trial dates use `granted_at + valid_days * interval '1 day'` with `clock_timestamp()` and satisfy `expires_at > valid_from`; no Q20 due/grace/calendar value is invented. | DB+TX |
| T-S2-60 | Direct authorization positive chains (F-16/aggregate F-01). | (a) `INBOUND_RESOLVE = ENTER_CALLEE` with no superseding decision and (b) `GUIDE_ACTIVATION` + `CONFIRMED` + committed `ACTIVATION_NEXT_ACTION = ENTER_CALLEE` each authorize one reservation, freeze `action` null/`taskMatch = NONE`/`DIRECT_AUTHORIZED`, keep the resolve decision immutable and return the callee only under `authorization.calleeNumber`; both accept an unregistered/unverified caller with no login/membership/real-name prerequisite. A retry with the identical semantic direct-authorization fields and a regenerated transport event ID replays the first authorization; changed session/protocol/access number (including a different canonical access number with an identical display mask, through `accessNumberHmac`)/callee/prompt/key/content conflicts and never creates a second reservation. | DB+TX |
| T-S2-61 | Direct authorization refusal regressions (F-16/aggregate F-01). | A task-backed call, non-empty taskId, `PENDING`/`CLAIMED` task, obsolete earlier `ENTER_CALLEE` superseded by a later no-dial decision, unconfirmed/contradictory session, or competing call is rejected with a permitted non-dial action and no reservation; a retry with a regenerated `IVR_DIRECT_AUTH` event ID but changed session/protocol/access number (including a different canonical access number with an identical display mask, through `accessNumberHmac`)/callee/prompt/key/content is a stable conflict and never redials; the task path never falls back to DTMF, a task-backed dial uses the frozen callee without confirmation, and a replay never redials or rewrites resolve. | DB+TX |
| T-S2-62 | IVR subject find-or-create (OQ-1). | A trusted normalized number with no binding atomically creates one unowned `IVR` subject and one active binding with both owner columns null and no account/login/owner; a second concurrent resolve under the canonical-number lock reuses the winner and creates no second active binding; a `DISABLED` binding is not revived; `RETIRED` history may be followed by a new active binding without erasing history; conflicting/multiple active bindings fail closed; a unique loser or category-3 failure leaves no partial subject/binding after the bounded retry/rollback. | DB+TX |
| T-S2-63 | Idempotency placement, savepoint and category outcomes (F-06/R-02/R-05). | The `IVR_CONFIRM` idempotency row is acquired after caller/session/access locks and before any tentative claim/reservation; the IVR confirmation and the order path take the rank-4 activation lock through `activation-consent.lock_service_activation` before ranks 7/8/9 and before the idempotency row; `record_activation_confirmation`, `record_client_activation` and `append_purchase_consent` reuse that held lock and never first acquire rank 4 after rank 10; replay precedes task-expiry housekeeping and every effect; the attempt savepoint is established after accepted activation/trial and before the tentative claim/reservation; a category-2 denial executes `ROLLBACK TO SAVEPOINT`, keeps activation/trial/session `CONFIRMED` and commits one no-dial decision/completion with no claim/reservation; a category-3 SQL/lock/I/O/Outbox error rolls back everything and is not swallowed; no transaction takes a task/reservation then the idempotency row. | DB+TX |
| T-S2-64 | Trial-policy publication/disable (F-09/R-01/OQ-5/OQ-6). | With a pre-existing enabled published policy, the winner first disables it with revision +1 and then publishes the candidate with ONE owner UPDATE that, while `OLD.published_at IS NULL`, sets the actual authenticated publisher, `published_at`, `enabled = true` and revision +1 together, so exactly one enabled published row exists at every statement and the immediate partial unique never rejects the normal second publication; the guard admits that statement and rejects later changes to publisher identity/content, and no separate pre-publication publisher statement is required; concurrent trial-policy publications have one winner under the campaign lock; the stale loser returns `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW`; disable is idempotent and appends one audit/Outbox fact; the publisher column is frozen after publication and no same-employee-only rule is applied; the V006 `issue_first_trial` function performs the sole authoritative enabled/published/expected-version read, replay/already-granted returns prior immutable results before any new policy selection, a disable committed before that read is observed while a disable after it does not revoke the selected grant, the wrapper lookup is preliminary only, and there is no second enabled recheck; published content/snapshot history and existing trial grants never change; no raw unique error. | DB+TX |
| T-S2-65 | Status/frozen-navigation document assertions (F-14/F-15/F4-01/aggregate F-11). | The document status states S1 accepted at checkpoint `5404d9c`, S2 accepted at checkpoint `8934df9` after independent review, S3 accepted at checkpoint `9ad33e8` after independent review, and aggregate deepreview accepted at checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`; aggregate fix 1 and aggregate fix 2 are complete; PR lifecycle and current gate state are authoritative only in `docs/gateflow/project-execution-control.md`, and the durable specification embeds no volatile next-gate assertion; `issue_first_trial` is authored in V006. `§4.28` points to §2.9 and `ck_ivr_decision_reason`; §4.1 remains byte-identical to the accepted plan and is not edited for the low-severity navigation correction. F4-01 remains closed by the accepted S3 status alignment; no historical artifact is rewritten and runtime readiness is not claimed. | Document |
| T-S2-66 | IVR source event-ID bounds (fix-3 Open Question 1). | For IVR confirmation, direct authorization and call-event ingestion, a blank or 129-character event ID is refused with stable `IVR_ARGUMENT_INVALID` before `idempotency_begin`, any decision/domain write or any `call_event` insert; 1- and 128-character IDs are accepted and persist in the 1-128 S2 source-event columns; the general platform 1-512 key capacity and the 1-475 order client key are unchanged. | DB+TX |

All S2 assertions are document requirements for a later Testcontainers/IVR/payment integration suite. No production test is claimed. The S3 assertions in §13.9 are the corresponding evidence/sharing requirements for a later PostgreSQL/Testcontainers/media-gateway/Redis integration suite.

### 13.8 S3 control-coverage assertions

These checks prove that every S3 dictionary names its lifecycle, sensitive representation, revision/immutability or append-only control, and that the S3 FK detail has exactly 16 rows. They are document checks only.

```bash
FILE=docs/14-production-data-model-and-flyway-schema-spec.md
test "$(sed -n '/^### 4.39 /,/^## 5\./p' "$FILE" | grep -c '^Purpose:.*Lifecycle:')" -eq 8 || { echo "S3 LIFECYCLE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.39 /,/^## 5\./p' "$FILE" | grep -c '^Sensitive representation:')" -eq 8 || { echo "S3 SENSITIVE COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 4.39 /,/^## 5\./p' "$FILE" | grep -c -E '^(Revision/immutability|Append-only control)[^:]*:')" -eq 8 || { echo "S3 REVISION/IMMUTABILITY COVERAGE FAILED"; exit 1; }
test "$(sed -n '/^### 5.5 S3 foreign-key detail/,/^## 6\./p' "$FILE" | grep -c -E '\| (FK|DEFERRABLE_FK) \|')" -eq 16 || { echo "S3 FK DETAIL COUNT FAILED"; exit 1; }
test "$(sed -n '/^### 8.6 S3 sensitive-field detail/,/^## 9\./p' "$FILE" | grep -c '^| ')" -ge 11 || { echo "S3 SENSITIVE DETAIL FAILED"; exit 1; }
test "$(grep -c -E 'trg_evidence_(record|event)_creation_cycle' "$FILE")" -ge 2 || { echo "S3 DEFERRED CYCLE TRIGGERS MISSING"; exit 1; }
test "$(grep -c -E '^### 9\.2[7-9] T-S3|^### 9\.3[01] T-S3' "$FILE")" -eq 5 || { echo "S3 TRANSACTION SEQUENCES MISSING"; exit 1; }
test "$(grep -c -E '^### 10\.2[1-3] ' "$FILE")" -eq 3 || { echo "S3 CONTRACT SECTIONS MISSING"; exit 1; }
test "$(awk '/^<!-- function-wave:start -->$/{f=1;next} /^<!-- function-wave:end -->$/{f=0} f && NF' "$FILE" | wc -l)" -eq 5 || { echo "S3 CHANGED FUNCTION WAVE"; exit 1; }
```

Expected: exit 0. Every S3 dictionary states one lifecycle, one sensitive representation and one revision/append-only rule; the S3 FK detail has 16 decisions; the two deferred creation-cycle triggers, five S3 transaction sequences and three S3 contract sections exist; the function-wave block remains exactly five rows.

### 13.9 S3 behavior test matrix

These are document-level requirements for the later executable suite. They are not executed tests and do not claim PostgreSQL, COS, Redis, media-gateway or production behavior.

| ID | Assertion | Expected outcome | Class |
| --- | --- | --- | --- |
| T-S3-01 | Compare the final document with the accepted plan catalog, frozen blocks, wave/slice membership, FK/key matrix and function-wave block. | 44 dictionaries / 523 exact columns in accepted order (S1 121, S2 308, S3 94); nine frozen blocks byte-equal; 44/40/9/3; FK 81 = 74/3/2/2; 24 key sets; five functions; no parent later than its FK wave. | Document |
| T-S3-02 | Open a controlled upload, record the exact object/version/hash/size/MIME observation through `evidence.record_upload_observed` (`DECLARED → UPLOADED`), then complete validation with a valid server hash, size, MIME, duration and malware result. | One media object, one `UPLOADED → VALIDATED` recording with server facts and `revision + 1`; declared facts never override; a `DECLARED` completion without the observation returns `UPLOAD_OBSERVATION_REQUIRED` and creates no media/evidence fact; no evidence row/status is created by the recording completion. | DB+TX |
| T-S3-03 | Repeat T-S3-02 with the same recording ID and identical object version/server hash, including repeating the upload observation and validation. | Returns the first `UPLOADED`/`VALIDATED` outcome; no second media object, recording, access fact or count; no TTL/expiry refresh. | DB+TX |
| T-S3-04 | Retry `evidence.record_upload_observed` with a changed object version/hash; retry an `UPLOADED` recording with a different object fact; then apply an identical duplicate and a different conflicting callback after `VALIDATED`. | A changed observation identity returns `RECORDING_IDENTITY_CONFLICT` and mutates nothing. Before terminal: one `QUARANTINED` transition with a stable conflict; first server facts and original media object remain unchanged. After `VALIDATED`: the identical callback returns the first outcome; a conflicting callback returns `RECORDING_IDENTITY_CONFLICT`, changes no status/media/validation fact/revision, and writes exactly one deduplicated redacted Outbox conflict fact (the same conflicting payload reuses it; a changed conflicting payload adds one distinct fact); no solid evidence is altered. | DB+TX |
| T-S3-05 | Complete at exactly `upload_expires_at` and after it. | Completion at/after expiry is refused and the recording is quarantined with `UPLOAD_EXPIRED`; no media/evidence success; a later same-identity retry cannot revive it. | DB+TX |
| T-S3-06 | Inject storage unavailable/unknown, a late callback and a crash before commit at the upload-observation and validation write boundaries. | No fabricated media object or validation; prior state remains; the observation and validation retries converge under the same identities; a conflicting late callback after terminal is refused with `RECORDING_IDENTITY_CONFLICT` and one deduplicated redacted conflict fact, never a forbidden terminal status change or overwrite of the first terminal fact. | DB+TX |
| T-S3-07 | Store a recording with bytes, object key and version metadata. | PostgreSQL contains metadata only; no media bytes, bucket/key/version or permanent URL is public or logged; the server-recomputed hash wins. | DB+SERVICE |
| T-S3-08 | Insert an evidence record and its one `EVIDENCE_CREATED` event using the same validated source `eventId` in record-first order and, separately, event-first order in one transaction; then replay the same `eventId` with identical facts and with changed `consentVersion`, optional link, recording or snapshot facts. | Both insertion orders commit under the two deferred FKs; exactly one original artifact and one matching creation event exist; exact replay returns the first record/event/artifact result; changed facts under the same stable `eventId` return `EVIDENCE_SOURCE_CONFLICT` and create no second record, event, artifact or Outbox fact; the frozen snapshot/identity/consent facts are readable. | DB+TX |
| T-S3-09 | Insert an evidence record with no matching `EVIDENCE_CREATED` event and commit. | Commit fails with `EVIDENCE_CREATION_CYCLE_VIOLATION`; no record, event, artifact or Outbox fact survives. This is the negative deferred witness. | DB+TX |
| T-S3-10 | Insert two `EVIDENCE_CREATED` events for one evidence ID. | The partial unique/creation-cycle check rejects the second; exactly one creation event exists; no second ownership fact. | DB |
| T-S3-11 | Insert a creation event with the wrong `event_type`, a different owning `evidence_id`, or a source event identity that does not equal the record source. | Commit fails at the deferred trigger/constraint with `EVIDENCE_CREATION_CYCLE_VIOLATION`; no mismatched creation fact survives. | DB+TX |
| T-S3-12 | Force a failure after the record, creation event, artifact or Outbox write. | The whole transaction rolls back; no creation fact, media/evidence row or Outbox/audit row survives; a bounded retry starts from current state. | TX |
| T-S3-13 | Claim the subject, change the account, revoke membership or change subject real-name after evidence solidification. | `subject_id`, `call_id`, `session_id`, `reservation_id`, `identity_at_capture`, `consent_version`, snapshot and original artifact remain byte-identical; historical evidence access is governed by the current approved policy, not by rewriting capture facts. | DB+SERVICE |
| T-S3-14 | Create evidence with a validated recording from call A and an evidence record for call B; then cross the session, subject or reservation links; also supply a missing parent. | `EVIDENCE_LINK_MISMATCH` (or a stable not-found for a missing parent) with no evidence row/creation event/artifact and no raw FK; `recording.call_id` must equal `evidence_record.call_id`; call/session/subject/reservation links must agree and the snapshot `source` must equal the persisted links; a session without a call is rejected by `ck_evidence_link_shape`. | DB+TX |
| T-S3-15 | Create a share grant for a `SOLIDIFIED` evidence record with a valid creator/workspace access policy, no `OPEN` risk, and a valid snapshot; repeat the same `creationRequestId` with the same scope, with changed scope, with changed `expiresAt`, and after a changed artifact/media fact; lose the creation response; repeat with `OPEN` risk; exercise a captured binding that differs from the current active binding and an absent/unresolvable binding. | The valid case calls `evidence.read_shareable_projection`, creates exactly one grant with immutable content/verification/creation scope, and reproduces `content_snapshot.content`, `scopeDigest` and `content_sha256` byte-for-byte from the returned immutable facts with artifacts ordered by the fixed non-lexical role rank (`ORIGINAL = 0`, `DERIVED = 1`) before the media hash/size/MIME tie-breakers (a two-artifact ORIGINAL/DERIVED set must hash identically under that rank, never under lexical role-name order); the content contains no media-object id, bucket, object key/version, bytes, permanent URL or unshared evidence and no plaintext code/token. The same-identity replay path also calls `evidence.read_shareable_projection` and reconstructs `content` before recomputing or comparing the frozen creation fingerprint. Same identity with exact immutable facts and the same fingerprint lost-response retry returns metadata only with no new grant/token/event; changed scope, changed `expiresAt` or changed/inconsistent artifact/media/content facts under the same creation identity returns `SHARE_CREATION_CONFLICT` with no new grant/token/event and no other mutation; an absent, unresolvable or not-`SOLIDIFIED` projection fails closed with the existing typed error and creates no grant/token/event; a usable replacement requires a new `creationRequestId`/grant/token. The captured binding is evaluated from `identity_at_capture.bindingId` when present; an `OPEN` captured/current risk returns `SHARE_RISK_BLOCKED` with no grant, an absent/unresolvable projection fails closed, and risk does not become a blanket dialing ban. | DB+TX |
| T-S3-16 | Generate a capability token, extraction-code salt/hash, rotate the code, and retry revoke/rotate/extend/lock/expire with the same and with changed action content, including a changed EXTEND `newExpiresAt`. | At least 128 random token bits and only its SHA-256 digest are persisted; the plaintext token is returned once and a lost response returns metadata only with no reissuance/reconstruction; the code uses a fresh salt and salted slow KDF bound to grant/generation; rotation increments `code_generation` and `revocation_generation`; old code/sessions fail. Each transition's §2.12 event identity/fingerprint is deterministic; a same-action retry returns the first recorded outcome before current revision/state checks, changed content (including a changed EXTEND `newExpiresAt`) returns a stable conflict with no status/revision/generation change, no second event and no other mutation, and a different action is distinct; no plaintext code/token/phone appears in logs/events. | DB+SERVICE |
| T-S3-17 | Admit with `NONE`, `DESIGNATED_MOBILE` (trusted SMS proof, HMAC knowledge only, and absent verification input), `ORGANIZATION_MEMBER`, a wrong-tenant organization member, and a captured binding that differs from the current active binding. | `NONE` records `NOT_REQUIRED`, never `VERIFIED`; designated mobile requires an approved trusted SMS possession proof for the exact normalized phone HMAC and intended share/action, denies knowledge/HMAC-only input, and fails closed when approved verification input is absent; organization member requires current active membership and the grant organization; a wrong tenant/revoked/suspended membership or disabled account is denied with no admission. The captured binding drives the risk check when present and an absent/unresolvable projection fails closed. | DB+TX |
| T-S3-18 | Admit successfully, reach the last slot, lose the response/Redis ticket/cache, retry the same admission key, then retry the same key after session expiry and attempt a new key at the limit. | Exactly one admission/count increment, one `ADMISSION_GRANTED` event and one derived ticket identity; the same-key retry at the limit returns the original unexpired admission and reissues the ticket without a cap test or increment; after `sessionExpiresAt` the same key returns `SHARE_SESSION_EXPIRED` with no ticket; a new key at the limit returns `SHARE_ADMISSION_LIMIT_REACHED` with no generation change; admission mode/no/key are immutable. | DB+TX |
| T-S3-19 | Open the verification page, prefetch the link and fail verification repeatedly until `LOCKED`; retry each observation with the same request key and with a changed semantic payload; then replay an existing admission and attempt a new key. | Observation events use the exact §2.12 `share-observation:` identity and payload fingerprint, where every `requestKeyDigest` is the lowercase SHA-256 of the exact stable nonblank 1-128 code-point request/action key with no Unicode normalization or case folding; a same-semantic retry returns the first event and appends nothing, while a changed payload under the same identity conflicts and appends nothing; a different request key is a distinct observation. No admission, no admission count change and no ticket authorization success; the verification-failure lock policy may set `LOCKED` without bumping generation or invalidating the last admitted session; an existing unexpired admission still replays/reconstructs, while a new admission is denied as locked; raw request keys/secrets are never stored, logged or emitted. | DB+TX |
| T-S3-20 | Reach `max_admissions`, then have the last admitted session play/seek/renew and an existing sender retry the same key and a new recipient attempt admission. | The last admitted session remains valid within its admission/grant validity; the same-key retry returns the original admission and ticket without a limit test; a new key is denied with `SHARE_ADMISSION_LIMIT_REACHED`; `revocation_generation` is unchanged. | DB+TX |
| T-S3-21 | Let the derived session expire, then retry the original admission key; extend the grant; also expire the grant. | Before `sessionExpiresAt` the same key reissues the ticket; after it the same key returns the admission with `SHARE_SESSION_EXPIRED` and no new ticket, so a new admission/recheck/count is required and the old key cannot become unlimited reuse; every issued ticket records the absolute `ticketExpiresAt = min(clock_timestamp() + approved Q22 ticket TTL, sessionExpiresAt, admissionExpiresAt, current grant expires_at)` and uses a Redis TTL only for the positive `ticketExpiresAt - clock_timestamp()` difference, issuing nothing and failing closed otherwise; extension never lengthens that admission/session or `admissionExpiresAt`; grant expiry denies and persists `EXPIRED` once with a generation increment; at/after the exact expiry boundary is denied. | DB+TX+SERVICE |
| T-S3-22 | Race an admission with revoke, code rotation, expiry and subject-risk open in both serial orders; revoke/suspend an organization recipient or change tenant before reconstruction and before renewal; also attempt `LOCKED → REVOKED`/`EXPIRED`. | Transition/risk first: admission observes the new status/generation/risk and denies with no count. Admission first: it counts once and its session is valid only within the admission-time witness; the later transition/risk governs subsequent renewal/reconstruction; no post-commit reversal. `ORGANIZATION_MEMBER` reconstruction/renewal re-authenticates the witness recipient and fails closed on changed account/tenant, revoked/suspended membership or disabled account. If a revoke commits before reconstruction it governs; a ticket returned before a later revoke is bounded only by the Q22 credential TTL. `LOCKED` may move to `REVOKED`/`EXPIRED` through the owner transitions. | TX |
| T-S3-23 | Race multiple different admission keys at the last available slot and duplicate same-key admissions. | Exactly one different-key winner/count/event; the loser returns limit/conflict with no count/generation change; exactly one row exists for a duplicate key and all retries return it. | DB+TX |
| T-S3-24 | Commit an admission, then make Redis unavailable before ticket issue; retry the same admission and inspect the count. | PostgreSQL admission/event/count commit before Redis; no successful media authorization is claimed on Redis failure; a durable `TICKET_FAILED` event is appended; the retry reuses the admission and never increments twice. | DB+TX+SERVICE |
| T-S3-25 | Lose the response, ticket or cache after a successful `ORGANIZATION_MEMBER` admission; reconstruct within and after the session window; revoke or suspend the member or change tenant before reconstruction/renewal; then revoke or rotate code and reconstruct again. | For every reconstruction/renewal, trusted recipient re-authentication and S1 ranks 7/8/9 occur before any sharing grant read or lock; all PostgreSQL reads/locks are released before Redis/media I/O. Within the session window and with the same active recipient the original admission/ticket identity is reconstructed without a second admission; after the session window the same key returns `SHARE_SESSION_EXPIRED` with no new ticket and requires a new admission/count; a changed account/tenant, revoked/suspended membership or disabled account is denied; after revoke/rotation a stale admission is denied with `SHARE_GENERATION_STALE`/`SHARE_REVOKED`; ticket/access identities are deterministic and no admission count changes. | DB+TX+SERVICE |
| T-S3-26 | Exercise playback, seek, renewal, the short credential TTL and media-gateway/Redis failure; after admission, set the captured/current subject risk to `OPEN` and separately make the binding/risk projection absent or unresolvable. | Current status/time/generation and the exact captured/current binding plus `subject_risk_state` projection are re-read for playback, seek and renewal; `OPEN` or absent/unresolvable returns `SHARE_RISK_BLOCKED` with no media credential, no access event and no admission count/generation change; `ORGANIZATION_MEMBER` re-authenticates the witness recipient and takes ranks 7/8/9 before any grant read/lock; PostgreSQL locks are released before Redis/media I/O; Redis unavailable fails closed; already-issued/buffered bytes are not claimed recalled; the credential TTL is the bounded revoke window; one redacted success event per semantic request. | DB+TX+SERVICE |
| T-S3-27 | Attempt every cross-module read/write/row-lock and inspect grants/functions, including subject_number USAGE, the sharing_role `identity_at_capture` read, the evidence-owner `read_shareable_projection` collaborator and the admission grant-row lock. | `evidence_role`/`sharing_role` can read only the exact §10.21 columns with the declared schema USAGE (including `subject_number` for the exact subject/ownership/binding/risk reads) and can execute exactly `platform.outbox_append` and `audit.append_fact`; execution of `platform.idempotency_begin`/`complete` is denied; every foreign write, foreign row lock, `DELETE`, `TRUNCATE`, schema `CREATE`, platform/audit table write and undeclared function/column read is denied; `sharing_role` has no direct `evidence.media_object` or `evidence.recording` grant, while `evidence.read_shareable_projection` returns only the declared redacted evidence/artifact/media facts under `evidence_role` and takes no foreign row lock; only the sharing owner service locks the grant row and `evidence_role` has no sharing privilege; no S3 function-wave row exists. | DB |
| T-S3-28 | Apply only V001-V007 and then V008-V009; on a clean database verify role existence before every grant. | V001-V007 are unaffected by S3; V008 creates `evidence_role` and grants evidence schema USAGE only to it, creates the evidence cycle only after both tables/unique keys and adds both deferred FKs/triggers; V009 creates `sharing_role` before granting it sharing/evidence schema USAGE and creates sharing after solidified-evidence parents; no wave has a table or role forward reference and no later slice exists. | Structural |
| T-S3-29 | Inspect every S3 redacted event/Outbox/audit payload, including the post-terminal recording conflict fact, transition events and the §10.24 evidence lifecycle/Outbox identities. | No plaintext capability token, code, designated phone, OTP, object URL/key/version, raw IP or media bytes; the conflict fact contains only the recording ID, statuses, digest, hashes and bounded presentation facts; evidence lifecycle/Outbox identities and payload fingerprints follow §10.24 deterministically, including `artifactRoles` in the fixed non-lexical `ORIGINAL = 0`, `DERIVED = 1` rank order; `payload_sha256`/`content_sha256` match canonical redacted forms. | DB+SERVICE |
| T-S3-30 | Exercise the positive deferred cycle witness and both negative witnesses together in one sequence. | Matching record/event pair commits; missing and mismatched pairs fail at commit; the failed witnesses leave no record/event/artifact/Outbox; the successful pair remains exactly one ownership fact. | DB+TX |
| T-S3-31 | Retry each evidence lifecycle event and evidence Outbox fact with the same semantics and with changed payload under the same identity; inspect every recorded identity and dedupe key. | `EVIDENCE_CREATED`, `EVIDENCE_SOLIDIFIED`, `EVIDENCE_QUARANTINED`, `EVIDENCE_FAILED`, `EVIDENCE_RECORDING_VALIDATED` and `EVIDENCE_RECORDING_QUARANTINED` follow the §10.24 identities and payload fingerprints: `EVIDENCE_CREATED.event_id` is exactly the validated source event `eventId` and the record's `source_event_id`, not a derived hash, while the lifecycle/Outbox facts use their deterministic derived identities; the same semantic retry returns the first event/Outbox ID and appends nothing; changed subject/call/session/reservation/recording/consent/snapshot facts under the same stable `EVIDENCE_CREATED` identity is `EVIDENCE_SOURCE_CONFLICT`, and a changed payload under a lifecycle identity is `EVIDENCE_EVENT_CONFLICT` or `OUTBOX_DEDUPE_CONFLICT`, with nothing appended; every event/dedupe identity is 1-128 characters, and the existing terminal `RECORDING_CONFLICT` fact is unchanged; no random ID or current timestamp is used. | DB+TX |
| T-S3-32 | Exercise the OQ-6 upload-observation transition twice with the same object/version/hash and with a changed identity; attempt `complete_recording` directly on `DECLARED`. | The same observation returns the first `UPLOADED` outcome with one immutable media object and no new revision/event, and a repeat against an already-`UPLOADED`/`VALIDATED` row returns that first outcome before any current `DECLARED` precondition; a same identity with a changed fingerprint returns `RECORDING_IDENTITY_CONFLICT` with no mutation, while only a genuinely new observation requires current `DECLARED`; `complete_recording` on `DECLARED` returns `UPLOAD_OBSERVATION_REQUIRED` and creates no media/evidence fact; only `UPLOADED` proceeds to validation/quarantine; no external I/O occurs while the recording row lock is held. | DB+TX |

All S3 assertions are document requirements for a later executable suite. No production test is claimed.

### 13.10 Final cumulative closure matrix

| Dimension | Final assertion | Evidence and limitation |
| --- | --- | --- |
| Tables / columns | 44 tables and 523 accepted columns in accepted order; S1 13/121, S2 23/308, S3 8/94. | §4.3-§4.46 dictionaries; §13.2-§13.2.2 exact comparisons; read-only PowerShell checker expected `Dictionaries=44`, `Columns=523`, `Errors=[]`. |
| Waves / slices | Nine waves with 3/6/4/7/6/7/3/5/3 tables; S1 13, S2 23, S3 8; no later-slice prerequisite. | Frozen catalog/matrix/wave/slice blocks; §11; structural script exit 0. |
| Ownership | 12 owning schemas/roles (platform-reliability, operations-audit, customer-iam, organization-access, operations-iam, subject-number, catalog-order, ivr, entitlement-metering, activation-consent, evidence, sharing); every table/invariant one owner; no cross-schema table write. | §3; §10.8/§10.12/§10.21; structural owner checks. |
| Invariants | 40 single-owner invariants; S1 14, S2 22, S3 5 expansion portions; DB/TX/SERVICE named for each. | §6; frozen 40 rows unchanged; §13.8 count checks. |
| Lifecycle | 28 mutable-head / 16 append-only; S1 11/2, S2 14/9, S3 3/5; no cascade and no ordinary hard delete. | §8.2/§12; matrix lifecycle column; structural script `lifecycle=28/16`. |
| Sensitive | 28 sensitive tables in the frozen matrix; S3 7 flagged tables plus full S3 detail; no plaintext token/code/phone/URL/secrets. | §8.1/§8.6; sensitive coverage check; §10.7 denylist. |
| FKs / keys | 81 decisions = 74 `FK` + 3 `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK`; 24 candidate key sets; every parent key declared and no parent after its FK wave. | §5; structural script `fk_rows=81 key_sets=24`; §5.5 deferred cycle detail. |
| Functions / services | Five function-wave rows (four V001 + one V006); S3 adds no SQL function or function-wave row; S1/S2/S3 owner services are same-connection application services. | §10; structural function-wave check; no `SECURITY DEFINER` S3 function. |
| Migration | V001-V009 forward-only, no backfill; V008 cycle DDL order and both deferred FKs/triggers explicit; V009 sharing after evidence. | §11; structural script wave membership; document-only limitation. |
| Tests | S1/S2/S3 document assertion matrices cover normal, replay, conflict, failure, race, recovery, privilege and deferred-cycle witnesses. | §13.5-§13.9; no executable PostgreSQL/COS/Redis test was run. |
| Deferred inputs | Every unresolved production input retains a named owner/destination; no retention, KDF parameter, admission limit, expiry, capacity, price, credential or policy is guessed. | §14; frozen 28 Q rows unchanged; S3-specific owners named in §14. |

The matrix closes the specification structure across S1-S3. It is not a claim that the future migrations, privileges, constraints, concurrency paths, COS validation or Redis ticket flow were executed. Aggregate deepreview is accepted at checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`; PR lifecycle and current gate state are authoritative only in `docs/gateflow/project-execution-control.md`. This matrix is not a claim of production acceptance or a further slice.

### 13.11 Honest limitations

S1, S2 and S3 ran only document checks and shell/awk/PowerShell validation. No PostgreSQL instance, Flyway migration, constraint, trigger, function, privilege, row-lock/savepoint/concurrency test, COS object/version/WORM validation, Redis ticket/cache test, media-gateway test, IVR line, payment provider or Testcontainers suite was executed. A green structural script, the exact S3 dictionary checker and the S1/S2/S3 test matrices prove document structure, membership and planned assertions only; they do not prove PostgreSQL behavior, executable migrations, privilege feasibility, race outcomes, COS/Redis behavior or production behavior. The S3 creation-cycle, admission-count and ticket-reconstruction contracts must be validated by the later executable migration/integration work unit before any production claim.

## 14. Open Production Inputs

The following 28 open inputs are copied from the accepted plan. They are not guessed, defaulted or silently converted into schema values. S1-relevant inputs are Q1, Q2, Q3, Q9, Q13, Q14, Q16, Q17, Q19, Q23 and Q24; S2-relevant inputs are named below; S3-relevant inputs are Q2/Q3/Q7/Q10/Q11/Q12/Q15/Q22/Q23 plus the S3-specific authorization-policy input named below. All inputs retain their frozen owner/destination rows.

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

S2-relevant open inputs are Q4 (production prices, quantities and catalog approval), Q5 (renewal/stacking/refund), Q6 (payment provider identifiers/payload), Q7/Q8 (SIP/line, media and duration tolerance), Q10 (recording prompt/consent wording), Q15/Q26 (IVR event/wire mappings and snapshot schema version), Q16 (task-key retention/abuse limits), Q18 (TTL configuration key), Q20 (installment due-date/grace), Q21 (reconciliation cadence), Q24 (call-blocking risk), Q25 (authorization window/clock skew), Q27 (task-access role matrix) and Q28 (IVR-local B-leg key/recovery). None is guessed by S2. The 30-second pending-task TTL is the source default and is frozen per task; the production configuration key/approval remains Q18. `authorizationExpiresAt` has no approved window value, so S2 stores the committed reservation value and invents none (Q25). The organization task-access role matrix is not narrowed (Q27); only current lifecycle/role/resource checks are required. B-leg uniqueness stays IVR-local (Q28).

Fix 1 introduces **validated inputs, not new open-input rows**. The approved allocation-policy code/version and the per-call cap/request bound (§10.18) are Product/Finance/Entitlement inputs with IVR/Backend, destination Phase C/IVR protocol and Q5/Q25-adjacent review; no policy code or numeric value is approved here. The approved immutable per-installment schedule/windows and due-installment input (§10.19) are Product/Finance/Operations inputs for Q4/Q20; no date, grace or calendar rule is inferred. Current prompt/policy content remains Q10; the authoritative-duration tolerance remains Q8 (none approved); publication serialization is an owner transaction, not a product policy. The first-time IVR subject/binding find-or-create is owned by subject-number (OQ-1) and creates no account/enterprise ownership. The 28-row open-input block remains byte-identical; these inputs are named in the service contracts and residuals rather than added as rows.

Review OQ outcomes are recorded without changing the frozen input rows. OQ-1 is accepted and implemented as the §10.20 `subject-number.find_or_create_ivr_subject` collaborator plus §9.13 placement. OQ-2 remains **deferred-with-owner** to Platform/Database/Security / deployment-security design and executable role-provisioning/privilege tests (Q17/Q23); the specification requires same-connection owner services but claims no LOGIN/`SET ROLE` mechanism. OQ-3 remains **deferred-with-owner** to the existing Product/Finance/IVR/Backend owners / Q5/Q8/Q20/Q25 and the named later IVR/commerce/acceptance work; fix 1 clarifies validated inputs and fail-closed boundaries, not production values or policies. OQ-4 (trial-policy publication path) is covered by accepted F-09 and §9.26; no separate open-input row or service beyond the owner transaction is added.

Fix 2 re-review OQ outcomes use the re-review's separate numbering and add no open-input row. OQ-1 is **accepted** as a bounded identity clarification: `purchaseConsentEventId` is a deterministic account-scoped source identity derived from existing order inputs, never a fresh random ID per retry (§10.17/§10.20); fix 3 fixes the exact 81-character SHA-256 derivation. OQ-2 is **accepted** as precise read alignment: `ivr_role` also reads `entitlement_grant.granted_units` so the routing predicate is evaluable; the locked reservation authorization is unchanged. OQ-3 is **accepted** as matrix reconciliation: §3.2 distinguishes exact `SELECT` collaborators from owner-service fact handoffs and grants no extra access. OQ-4 is **deferred-with-owner** to Product/Operations/catalog-order and the catalog operations OpenAPI work; S2 specifies no independent `catalog_offer` withdrawal endpoint or transaction and relies on version offsale to block new orders. OQ-5 is **accepted**: the actual authenticated publisher is recorded in the existing `published_by_employee_id` in the publication update and frozen on publication; no same-employee-only rule is invented; fix 3 requires one owner UPDATE that sets publisher, `published_at`, `enabled` and revision together while `OLD.published_at IS NULL`. OQ-6 is **accepted** as a serialization clarification: the V006 `issue_first_trial` function's single authoritative enabled-policy read is the selection linearization point and that immutable version/content is retained through the grant (§10.9/§10.13); fix 3 adds replay-before-selection and excludes a second enabled recheck. OQ-7 is **accepted** as an enforcement-layer clarification: the DB transition check is a broad shape bound and the sole owner service enforces the narrower source/state matrix (§4.38). OQ-8 is **accepted**: settlement sets the derived `EXHAUSTED` status in the same conditional balance `UPDATE` as its counters (§9.15).

Fix-2 re-review corrections and controller clarifications are recorded without adding an open-input row or a new schema object. The purchase identity is now the fixed 81-character `purchase-consent:` + lowercase SHA-256 derivation, with no truncation, random or caller-chosen alternative (§10.17/§9.17/§10.20). IVR confirmation, direct-authorization and call-event source event IDs are validated nonblank 1-128 before `idempotency_begin` or any domain effect (§9.11/§9.14/§9.24/§10.17; the general 1-512 platform key and 1-475 order key are unchanged). The V006 `issue_first_trial` function is the sole authoritative enabled/published/expected-version policy read, retains the selected content through grant/Outbox, returns replay/already-granted results before any new selection and performs no second enabled recheck (§10.9/§10.11). `SERVICE_ACTIVATION` source references are exact and deterministic (`ivr-session:` + 36-character session UUID = 48; `activation:` + SHA-256 = 75), independent of the transport event ID (§4.36/§9.11/§9.25/§10.17). Trial-policy publication is one owner `UPDATE` setting publisher, `published_at`, `enabled` and revision together while `OLD.published_at IS NULL`, admitted by the guard and then frozen (§4.29/§9.26/§10.16/§10.20). Call-event `payload_redacted` distinguishes the hashed `source` payload from server-generated `receipt` metadata, removing the hash/fingerprint circularity (§4.27/§9.24/§10.17). Call-event ingestion acquires the owned session then current-call locks before sequence detection/insert (C-01), and a changed fingerprint always returns a stable conflict, including after an original `SEQUENCE_CONFLICT` (C-02) (§4.27/§9.24/§10.17). No product rule, policy value, workflow or provider input is invented; Q15/Q26 wire encoding remains with its named later owner.

S3-relevant open inputs are used, not guessed: Q2/Q11 for COS/KMS/object-lock and WORM capability (owner Security/Infrastructure; destination storage/security/capacity work); Q3 for legal retention/erasure/backup deletion (Legal/Security); Q7/Q10 for recording format/size, buffer and recording-notice/consent wording (Telephony/IVR and Product/Legal); Q12 for share-code KDF parameters, admission limits, expiry defaults and rate-limit thresholds (Security/Product; destination security design and sharing policy); Q15/Q22 for share/evidence wire values, Redis ticket TTL, clock-skew and revocation propagation window (Backend/Security and Security/Sharing); and Q23 for deployment role provisioning and `SECURITY DEFINER`/trigger-function ownership execution (Platform/Database/Security). The approved evidence-access/role/resource policy that permits creating a share grant for a given evidence scope is a Product/Security/Sharing input with destination sharing policy and authorization design; absent or ambiguous policy fails closed with `SHARE_ACCESS_POLICY_UNAVAILABLE`, and S3 does not invent a role matrix. The exact WORM/object-lock status vocabulary is recorded as an observed fact only; no retention or object-lock duration is stored. The 28-row block remains byte-identical; these inputs are named in the S3 service/trigger contracts and residual risks rather than added as rows.

Aggregate fix 1 adds no open-input row. Its four accepted clarifications are recorded here with owners/destinations: OQ-1 is a bounded S1 specification/test clarification (`subject_claim.risk_checked_at` early rejection; owner Subject-number/Backend; destination the S1 claim test suite), OQ-4 is a bounded S3 identity/test clarification for `PAGE_OPENED`/`PREFETCH_IGNORED`/`VERIFICATION_FAILED` (owner Sharing/Backend; destination the S3 access-event test suite), OQ-5 is a bounded S3 ordering clarification (owner Sharing/Backend; destination the S3 reconstruction/renewal tests) and OQ-6 is a bounded evidence state-machine clarification (owner Evidence/Backend; destination the S3 recording tests). OQ-2 (`maskedCaller`) remains deferred to IVR/Subject-number/Backend / Q15/Q26 task API and exact response-projection design; OQ-3 (`download_allowed` enforcement) remains deferred to Product/Security/Sharing / Q15 media credential/API policy. OQ-7 is closed only by the accepted F-08 evidence-owner `read_shareable_projection` contract; final wire encoding remains Q15. None of these adds a table, column, SQL function, wave, slice, action, domain policy, retention/KDF/TTL value or provider configuration.

Aggregate fix 2 adds no open-input row. Its three accepted clarifications are recorded here with owners/destinations: OQ-R1 makes the existing-creation replay path call the read-only `evidence.read_shareable_projection` and reconstruct the deterministic content before comparing the frozen creation fingerprint (owner Sharing/Backend; destination the S3 sharing-creation test suite); OQ-R2 explicitly defines `requestKeyDigest` as the lowercase SHA-256 of the exact stable nonblank 1-128 code-point request/action key with no normalization or case folding (owner Sharing/Backend; destination the S3 access-event test suite); OQ-R3 orders `evidence.record_upload_observed` replay before the current `DECLARED` precondition (owner Evidence/Backend; destination the S3 recording test suite). The F-01/F-02 correction adds the ephemeral `accessNumberHmac` fingerprint input and no column, response field, log, audit, snapshot or event value. None of these adds a table, column, SQL function, wave, slice, action, domain policy, retention/KDF/TTL value or provider configuration.

## 15. Goal-Alignment and No-Over-Design Check

### 15.1 Goal alignment

| Confirmed goal area | S1/S2/S3 evidence |
| --- | --- |
| Account/login identity | S1 customer_account, auth_identity, identity_profile; login only through active identity + active account |
| Organization membership | S1 organization and organization_membership; workspace access derived from active membership; no independent login |
| Employee identity | S1 employee_account in a separate issuer domain; last-admin guard |
| Business subject/number binding | S1 business_subject, caller_number_binding; HMAC lookup; owner exclusivity; no login inference |
| Consent/activation | S2 consent_record, service_activation and transition ledger; explicit current consent, revision transitions, operations-only resume and one activation per subject |
| IVR/trial/entitlement/calls | S2 outbound task, inbound session, canonical call record, legs, events, frozen decisions, first-trial grant, entitlement balance, reservation and usage facts; one shared action service |
| Commerce/payment/issuance | S2 immutable catalog/version/offer/benefit, PURCHASE-idempotent order, verified payment events and durable per-line issuance |
| Evidence/media/sharing | S3 media object, recording validation, immutable evidence record/snapshot, reciprocal `EVIDENCE_CREATED` ownership, original/derived artifacts, share grants, durable admissions and derived playback tickets; all eight tables authored |
| Idempotency/Outbox/audit | S1 platform/audit tables and four V001 functions; S2 uses the IVR_CONFIRM/PURCHASE scopes, Outbox and audit without new write paths |
| Sensitive fields | Enveloped ciphertext, HMAC, masked and redacted representations; typed IVR snapshot with encrypted callee payload; S3 digest-only capability tokens, salted code KDF, HMAC-only designated mobile, redacted access events; function denylist |
| Deletion policy | No delete/cascade; status retirement; append-only controls; retention inputs assigned and not guessed |
| Migration order | Nine frozen waves; V001-V009 authored with no forward reference; V008 creation-cycle DDL and V009 sharing order explicit |

### 15.2 No-over-design check

- S1 adds no table, function contract, domain or slice beyond the accepted baseline. The only S1 tables are the exact 13 V001-V003 tables.
- S2 adds no table beyond the exact 23 V004-V007 tables and no SQL function beyond the accepted V006 `issue_first_trial`. It adds no eighth IVR action, no fourth slice, no generic workflow engine, no event-sourcing layer, no microservice split and no provider configuration.
- S1 does not author S2/S3 dictionaries, transactions, state machines or tests; S2 does not author S3 dictionaries or behavior; S3 authors only evidence/sharing and the final status/cross-slice integration. The frozen 44-table catalog, matrix, FK/key/sensitive/function-wave and wave/slice blocks are copied for closure and are unchanged.
- The S2 application services (`ivr.select_call_action`, `ivr.record_activation_next_action`, `activation-consent.lock_service_activation`, `activation-consent.record_activation_confirmation`, `activation-consent.record_client_activation`, `activation-consent.append_purchase_consent`, `subject-number.find_or_create_ivr_subject`, `ivr.process_call_event`, `entitlement-metering.reserve_call_usage`, `entitlement-metering.settle_call_usage`, `catalog-order.publish_catalog_version`/`offsale_catalog_version`, `entitlement-metering.publish_trial_policy`/`disable_trial_policy`) are application services inside one local transaction on one connection, not SQL functions. `activation-consent.lock_service_activation` is lock-only: it takes the rank-4 activation row lock under the owner role and writes nothing. The only SQL function is V006 `issue_first_trial`; fixes 1-3 add no function-wave entry.
- S2 uses one local PostgreSQL transaction and one intentional same-connection savepoint for a tentative phase-B claim/reservation. The savepoint is a transaction-scoped control, not an autonomous transaction, independent service or committed subtransaction.
- S2 stores no order-level entitlement status; `entitlement_issuance` is the PR-004-required durable per-line/installment/type fact and `customer_order.payment_status` remains payment-only. No S2 table duplicates entitlement or evidence state.
- S2 does not add a generic workflow engine, provider-specific pricing, a production configuration key, capacity value, retention period, credential or cost estimate. The 30-second pending-task TTL is the source default; the production key remains Q18. `authorizationExpiresAt` has no approved value and stores the committed reservation value only.
- Cross-module lifecycle serialization uses the exact S1 advisory namespaces, including the stable logical membership pair key, and owner-only row locks. S2 adds two owner-scoped transaction advisory serializations for catalog publication and trial-policy publication; they are transaction control, not tables, functions or product rules. S2 adds no cross-module DML grant, cross-owner row-lock privilege, ID-discovery loop or hidden query service.
- Fix 1 adds no table, column, invariant, wave, slice, action, reason-registry value or SQL function. It adds exact privilege `USAGE`/owner grants, a bounded fingerprint/Outbox data registry, one approved-policy selector contract, typed snapshot/schedule validation, and application-service transactions for existing state machines. No tolerance, production price/quantity/date, trial-first/FIFO/stacking rule or authentication provisioning mechanism is invented; Q8/Q20/Q25 and the deployment Q17/Q23 remain with their owners.
- Fix 2 adds no table, column, invariant, wave, slice, action, reason-registry value or SQL function. It corrects the publication statement order, names one activation-consent-owned lock-only application collaborator, adds exact read/schema-usage alignments for client activation and the computed order entitlement projection, makes the unknown-call and call-event first-outcome contracts executable, adds the `SERVICE_ACTIVATION` consent fingerprint scope, and states the accepted OQ-1..OQ-8 clarifications. No product rule, policy value, operations workflow or provider input is invented.
- Fix 3 adds no table, column, invariant, wave, slice, action, reason-registry value or SQL function. It fixes the purchase-consent identity length with the 81-character SHA-256 derivation, bounds IVR source event IDs at 1-128 before effects, makes the V006 function the sole authoritative policy read with replay-before-selection, fixes deterministic activation source references, publishes a trial policy in one owner `UPDATE`, removes the call-event payload/fingerprint circularity, and corrects the call-event lock/sequence ordering and changed-fingerprint conflict rule. No product rule, policy value, workflow or provider input is invented; final wire encoding remains Q15/Q26 work.
- S3 adds no table, column, invariant, wave, slice, action, SQL function, function-wave row, provider configuration or business policy. It authors the eight already-frozen V008-V009 tables, the five accepted S3 invariant portions, the already-accepted reciprocal deferred evidence cycle, the accepted share admission/ticket contract and the final closure. It does not redesign S1/S2, add a fourth slice or invent a sharing access-role matrix, KDF parameter, admission limit, expiry, retention period, COS/KMS product, Redis TTL or credential.
- S3's service/trigger additions are owner-local application services and `SECURITY INVOKER` trigger functions, not SQL `SECURITY DEFINER` functions or cross-schema write paths. The creation-cycle triggers are installed in V008 after both deferred FKs; the sharing admission guard is a local backstop; no S3 object appears in the function-wave registry.
- S3 preserves the S1 stable membership-pair namespace, exact S1/S2/S3 global lock order, S1 whole-transaction bounded restart, S2 phase-B savepoint, permanent `FIRST-TRIAL`, seven actions/null direct purpose, authenticated replay before mutable cleanup, 30-second task TTL/auto-dial and usage/UNKNOWN preservation. S3 takes only needed S1 suffix ranks 7-9 before its own owner locks, or no S1 lock for local evidence/ticket work.
- A direct no-task IVR caller remains eligible for activation/trial/recording without registration, login, enterprise membership or real-name; no S2 predicate imposes a dialing ban or rewrites historical evidence.
- Every S1, S2 and S3 table has one owner and write path. Every S1/S2/S3 invariant portion has one owner and an exact enforcement location.
- Unresolved inputs are assigned to owners/destinations in §14; no policy is invented to make a check green. No S2 behavior requires S3, and S3 has no later-slice prerequisite because S3 is the final approved slice.
- Aggregate fix 1 adds no table, column, invariant, wave, slice, action, reason-registry value or SQL function. It restores the exact S2 semantic fingerprints and three-phase reconciliation/lock order, names the operations-iam lock-only collaborator and the evidence-owner shareable projection, makes the task/call reciprocal link writable once, fixes S3 role/grant order and playback risk, adds bounded deterministic evidence identities, and aligns status metadata. It invents no product, retention, KDF, TTL, pricing or provider policy; Q15/Q26 wire work and Q17/Q23 deployment provisioning remain with their owners.

- Aggregate fix 2 adds no table, column, invariant, wave, slice, action, reason-registry value or SQL function. It replaces the display-mask access identity in the three §10.17 scopes with the ephemeral `accessNumberHmac` fingerprint input, makes the existing-share-creation replay path read the evidence-owner projection before fingerprint comparison, names `requestKeyDigest` as lowercase SHA-256 of the exact stable request/action key, and orders upload-observation replay before the current `DECLARED` precondition. It invents no product, retention, KDF, TTL, pricing or provider policy; Q15/Q26 wire work and Q17/Q23 deployment provisioning remain with their owners.

## 16. Completion Report Template

The S1 author used this template in `docs/gateflow/a0-production-data-model-schema-spec-s1-implementation.md`. The S2 author used it in `docs/gateflow/a0-production-data-model-schema-spec-s2-implementation.md`; the bounded S2 fix 1, fix 2, fix 3 and fix 4 record their evidence in `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-1.md`, `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-2.md`, `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-3.md` and `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-4.md`. The S3 author uses it in `docs/gateflow/a0-production-data-model-schema-spec-s3-implementation.md`. It is not a claim of independent review acceptance.

- Task label, provider and gate
- Changed artifact paths
- Binding inputs, accepted S1/S2 checkpoints and hashes
- S1/S2 acceptance state and S3 tables/invariants/services/behavior authored
- S3 dictionary coverage (8 tables, 94 columns), the reciprocal evidence cycle, sharing contracts, owner services and trigger security
- Frozen baseline counts and membership preserved
- Exact validation commands, exits and outputs, including the unchanged structural script with `FILE` and `FIX2`
- Docs decision
- Residual risks classified as fixed-current, later-approved-slice or later-work-unit, with owner and destination
- Open production inputs preserved (including Q18/Q24/Q27/Q28)
- Uncovered areas and explicit limitations
- Completion or blocked statement
- Explicit statement that no independent S3 review, later slice, Git stage/commit/push/PR/merge or production test was performed
