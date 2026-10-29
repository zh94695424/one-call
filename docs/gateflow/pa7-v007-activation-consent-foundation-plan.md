# PA7 V007 Activation and Consent Foundation - Implementation Plan

- Task label: `pa7-v007-plan-gpt56-20261029-184753`
- Work unit: `PA7-v007-activation-consent-foundation`
- Gate: `plan`
- Date: 2026-10-29
- Base: `6b7edcd37961d14eacba68d5a38d9773fa9bcff9`
- Branch: `codex/pa7-v007-activation-consent-foundation`
- Binding controller: `docs/gateflow/project-execution-control.md:28`
- Binding architecture: `docs/12-technology-stack-and-system-architecture.md:206`, `docs/12-technology-stack-and-system-architecture.md:222`, `docs/12-technology-stack-and-system-architecture.md:434`, `docs/12-technology-stack-and-system-architecture.md:460`
- Binding schema: `docs/14-production-data-model-and-flyway-schema-spec.md:1902`, `docs/14-production-data-model-and-flyway-schema-spec.md:1961`, `docs/14-production-data-model-and-flyway-schema-spec.md:1991`, `docs/14-production-data-model-and-flyway-schema-spec.md:2407`, `docs/14-production-data-model-and-flyway-schema-spec.md:3622`, `docs/14-production-data-model-and-flyway-schema-spec.md:3778`, `docs/14-production-data-model-and-flyway-schema-spec.md:4101`, `docs/14-production-data-model-and-flyway-schema-spec.md:4205`

This is a code-generation-ready plan for one forward-only Flyway migration and its cumulative database evidence. It does not implement V007 during the plan gate. Goal Confirmation is the binding scope contract; this plan does not add a new requirement, application service, policy value or future-wave object.

## 1. Goal, Motivation, Success and Boundary

### 1.1 Goal and alignment

Implement exactly `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` after this plan is accepted. V007 must:

1. Create `activation_consent.consent_record`.
2. Create `activation_consent.service_activation`.
3. Create `activation_consent.service_activation_transition`.
4. Add `fk_customer_order_consent` from `catalog_order.customer_order.purchase_consent_id` to `activation_consent.consent_record.id` as an ordinary immediate, validated, non-deferrable `ON UPDATE RESTRICT ON DELETE RESTRICT` FK.
5. Add only the local trigger functions, triggers and ACL changes required by the frozen V007 dictionaries and grant matrix.

This exactly matches the controller goal at `docs/gateflow/project-execution-control.md:37-46` and the frozen V007 order at `docs/14-production-data-model-and-flyway-schema-spec.md:4231-4236`.

### 1.2 Motivation

V004 deliberately created `customer_order.purchase_consent_id` as non-null with no FK. V004 also created the `activation_consent` schema and its owner/runtime roles but no tables. V005 created the IVR session parent. V006 created the trial/entitlement parents and left activation/consent absent. V007 is therefore the smallest closed persistence increment that supplies consent facts, the per-subject activation head, the append-only transition ledger and the delayed order-to-consent referential backstop before V008 evidence/media and V009 sharing.

### 1.3 Frozen success signals

- A clean PostgreSQL 16 database migrates V001 through V007 in order with no skipped or failed migration.
- V001-V006 remain byte-identical to their tracked blobs; V007 is the only new migration.
- `activation_consent` contains exactly three tables and no sequence.
- The table dictionaries contain exactly 43 columns: 19 `consent_record`, 6 `service_activation`, 18 `service_activation_transition`.
- Exact V007 structural totals are: 31 named checks (15/3/13), 3 PKs, 5 ordinary unique constraints, 4 standalone secondary indexes, 12 total index objects, 10 table-local ordinary immediate FKs plus 1 late ordinary immediate `ALTER_FK`, 6 defaulted columns, 0 `id` defaults, 2 local `SECURITY INVOKER` trigger functions, 3 triggers and 0 SQL service functions.
- `consent_record` and `service_activation_transition` are append-only/frozen; `service_activation` exposes only the mutable `status`, `revision`, `updated_at` head columns while `id`, `subject_id`, `created_at` are trigger-protected.
- Source/channel/actor shapes, consent enums and lengths, transition revision progression, broad transition matrix, unique action identity and unique `ACTIVATION_STATUS` identity are executable database backstops.
- The late order-consent FK is present only after V007, immediate, not deferrable and restrictive in both directions.
- Owner/runtime/cross-module ACLs are exact; runtime roles have no schema `CREATE`, no undeclared column access, no foreign write or row-lock path, no business-table `DELETE`/`TRUNCATE`, and `PUBLIC` has zero schema/table/function privilege.
- Focused inventory, ACL, FK, default, check, trigger, rollback and bounded concurrency evidence passes without weakening any V001-V006 behavior assertion.
- Source `@Test` names, CI expected names and Failsafe XML names are equal: 45 current methods, 48 after Slice S1, 51 after Slice S2.
- Default build, PostgreSQL 16 container profile, boot smoke and production fat-jar dependency firewall pass with zero skips/failures/errors.

### 1.4 Non-goals

- No `activation-consent.lock_service_activation`, IVR activation confirmation, MINIAPP/WEB activation, purchase-consent append, operations suspend/resume or consent-gap recovery application service.
- No `issue_first_trial` orchestration, IVR action selection, task/reservation selection, Outbox/audit orchestration or transaction-category service implementation.
- No V008 evidence/media object, recording, evidence record/artifact/event or reciprocal deferred FK.
- No V009 sharing object.
- No API, controller, JPA entity/repository, runtime DataSource/Flyway wiring, frontend, cloud/Tencent resource, provider integration or production acceptance claim.
- No new role or schema; both activation-consent roles and the schema already exist from V004.
- No SQL service function. The two V007 functions are owner-local trigger controls and do not enter the five-row published function-wave registry.
- No guessed policy code/version, prompt version/content, source reference, event ID, fingerprint, wire encoding, TTL, trial eligibility, product value or operations rule.

### 1.5 Scope boundary

Implementation may change only the paths in section 3 for the active slice. Gateflow controller/review bookkeeping remains controller-owned. Any discovered contradiction that changes a column, constraint, transition, privilege, method acceptance condition or scope stops the affected slice; it is reported rather than resolved by editing a binding source.

## 2. First-Principles Judgment and Repository Evidence

### 2.1 Why this is one migration boundary

- The module owns consent snapshots, `ServiceActivation` and transitions and may depend on subject-number (`docs/12-technology-stack-and-system-architecture.md:224-244`).
- PostgreSQL/Flyway and concurrent uniqueness are the prescribed data evidence (`docs/12-technology-stack-and-system-architecture.md:434-456`).
- The frozen linear order places V007 after the existing V006 prefix and before V008/V009 (`docs/14-production-data-model-and-flyway-schema-spec.md:4103-4113`).
- The current migration directory contains exactly V001-V006. V004 already created `activation_consent_owner`, `activation_consent_role` and schema USAGE boundaries (`backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql:10-24`, `backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql:483-495`).
- V004 explicitly leaves `purchase_consent_id` without an FK until V007 (`backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql:1-8`).
- V006 follows the executable role pattern required for V007: create owned objects under `SET LOCAL ROLE <owner>`, revoke/grant under the owning role, `RESET ROLE`, perform foreign-owner grant waves under each owner, `RESET ROLE`, then add FKs as the migration identity (`backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql:12`, `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql:623-692`, `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql:694-772`).
- `PostgresContainerIT` currently has exactly 45 unique `@Test` methods and explicitly proves V007 tables, grants and the order-consent FK are absent (`backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java:37-49`, `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java:7168-7227`).
- CI enumerates those 45 names and fails on count, skip, failure, error or set drift (`.github/workflows/ci.yml:60-137`).
- README documents the 45-method V006 boundary and must be synchronized rather than expanded into production claims (`backend/README.md:1-3`, `backend/README.md:102-115`).

### 2.2 Apparent contradictions and reconciliation

1. **Architecture Phase A mentions consent/internal activation while the current controller cites Phase B.** Section 15 is coarse implementation sequencing; the frozen database order and current controller select the exact V007 persistence step after V006. V007 does not import Phase B IVR orchestration or Phase C purchasing behavior.
2. **The order FK is “deferred to V007” but must be non-deferrable.** “Deferred” describes migration-wave timing from V004 to V007. PostgreSQL metadata must be `condeferrable=false`, `condeferred=false`; it is not a `DEFERRABLE` constraint.
3. **The transition matrix check is broader than the service matrix.** This is intentional. The database accepts the documented shape; future owner services reject `OPERATIONS INACTIVE→ACTIVE`, non-operations `ACTIVE→SUSPENDED`, and all client/IVR resume cases. V007 must not tighten the check or claim service enforcement.
4. **`consent_record.action` allows `DECLINE`, while accepted activation/purchase service prose appends `ACCEPT`.** The table is a generic explicit decision fact. V007 preserves both enum values; future service paths decide whether a declined request produces a stored fact or a typed refusal.
5. **“No SQL service function” coexists with two SQL functions.** The prohibition is on published service functions. `reject_row_mutation` and `guard_service_activation` are local `SECURITY INVOKER` trigger functions with no function-wave row.
6. **The activation head defaults to `INACTIVE`, revision 1, while an initial transition may be 0→1.** The database dictionary permits a default head shape and separately constrains transition increments. Future owner services are responsible for atomically creating/updating the head with a matching ledger row; V007 must not invent a cross-table trigger.

No blocking contradiction remains after these source-consistent distinctions. Any implementation evidence to the contrary is a blocking open question.

## 3. Exact Paths and Ownership

| Path | Slice | Writer/owner | Exact allowed change |
| --- | --- | --- | --- |
| `docs/gateflow/pa7-v007-activation-consent-foundation-plan.md` | Plan gate | Plan author | This artifact only |
| `backend/src/main/resources/db/migration/V007__activation_and_consent.sql` | S1 | V007 implementation author; DB objects owned as section 5 | New migration only |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | S1/S2 | V007 implementation author | Cumulative prefix evolution, exact V007 assertions, fixtures and six new methods |
| `.github/workflows/ci.yml` | S1/S2 | V007 implementation author | Expected Failsafe method names/count and V007 labels only |
| `backend/README.md` | S1/S2 | V007 implementation author | Bounded V007 inventory/evidence/count synchronization only |
| `docs/gateflow/pa7-v007-activation-consent-foundation-s1-implementation.md` | S1 | Implementation evidence author | New S1 evidence artifact |
| `docs/gateflow/pa7-v007-activation-consent-foundation-s2-implementation.md` | S2 | Implementation evidence author | New S2 evidence artifact |
| `docs/gateflow/project-execution-control.md` | Later gates only | Phaseflow controller | Gate status/evidence bookkeeping; never written by an implementation author |

Review artifact paths are assigned by the controller at review dispatch and are not guessed here. No other source, test, POM, contract, frontend, deployment or Git-state path is authorized.

## 4. Exact V007 Catalog Inventory

### 4.1 Headline totals

| Item | Exact V007 total |
| --- | ---: |
| Tables | 3 |
| Columns | 43 = 19 + 6 + 18 |
| Named checks | 31 = 15 + 3 + 13 |
| Primary keys | 3 |
| Ordinary unique constraints | 5 = 1 + 1 + 3 |
| Standalone secondary indexes | 4 = 2 + 1 + 1 |
| Total index objects | 12 = 3 PK-backed + 5 unique-backed + 4 standalone |
| Table-local ordinary immediate FKs | 10 = 4 + 1 + 5 |
| Late ordinary immediate `ALTER_FK` | 1 |
| V007 FK decisions | 11 |
| New `NO_FK` decisions | 0 |
| Deferrable FKs | 0 |
| Defaulted columns | 6 = 1 + 4 + 1 |
| `id` defaults / sequences | 0 / 0 |
| Trigger functions | 2 |
| Triggers | 3 |
| SQL service functions | 0 |

### 4.2 `activation_consent.consent_record` — 19 columns

| Column | Type | Null | Default | Frozen constraint role |
| --- | --- | --- | --- | --- |
| `id` | `uuid` | NO | none | PK `pk_consent_record` |
| `subject_id` | `uuid` | NO | none | FK `fk_consent_subject` |
| `purpose` | `text` | NO | none | `ck_consent_purpose` |
| `channel` | `text` | NO | none | `ck_consent_channel` |
| `event_domain` | `text` | NO | none | `ck_consent_event_domain`; `uq_consent_event` |
| `policy_code` | `text` | NO | none | `ck_consent_policy_code` |
| `policy_version` | `text` | NO | none | `ck_consent_policy_version` |
| `prompt_version` | `text` | NO | none | `ck_consent_prompt_version` |
| `action` | `text` | NO | none | `ck_consent_action` |
| `source_type` | `text` | NO | none | `ck_consent_source_type`; source shape |
| `source_ref` | `text` | NO | none | `ck_consent_source_ref` |
| `ivr_session_id` | `uuid` | YES | none | FK `fk_consent_session`; source shape |
| `account_id` | `uuid` | YES | none | FK `fk_consent_account`; source shape |
| `employee_id` | `uuid` | YES | none | FK `fk_consent_employee`; source shape |
| `snapshot` | `jsonb` | NO | none | `ck_consent_snapshot_type` |
| `snapshot_sha256` | `text` | NO | none | `ck_consent_snapshot_sha` |
| `event_id` | `text` | NO | none | `ck_consent_event_id`; `uq_consent_event` |
| `request_fingerprint` | `text` | NO | none | `ck_consent_fingerprint` |
| `accepted_at` | `timestamptz` | NO | `now()` | decision time |

Exact checks:

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

Keys/indexes/FKs:

- PK `pk_consent_record (id)`.
- Unique `uq_consent_event (event_domain, event_id)`.
- Index `ix_consent_subject_purpose (subject_id, purpose, accepted_at DESC)`.
- Index `ix_consent_session (ivr_session_id)`.
- FK `fk_consent_subject (subject_id) → subject_number.business_subject(id)`.
- FK `fk_consent_session (ivr_session_id) → ivr.inbound_session(id)`.
- FK `fk_consent_account (account_id) → customer_iam.customer_account(id)`.
- FK `fk_consent_employee (employee_id) → operations_iam.employee_account(id)`.
- Every FK is `ON UPDATE RESTRICT ON DELETE RESTRICT`, immediate, validated and non-deferrable.

### 4.3 `activation_consent.service_activation` — 6 columns

| Column | Type | Null | Default | Frozen constraint role |
| --- | --- | --- | --- | --- |
| `id` | `uuid` | NO | none | PK `pk_service_activation` |
| `subject_id` | `uuid` | NO | none | unique `uq_service_activation_subject`; FK |
| `status` | `text` | NO | `'INACTIVE'` | `ck_service_activation_status` |
| `revision` | `integer` | NO | `1` | `ck_service_activation_revision` |
| `created_at` | `timestamptz` | NO | `now()` | immutable head identity |
| `updated_at` | `timestamptz` | NO | `now()` | `ck_service_activation_time_order` |

Exact checks:

- `ck_service_activation_status`: `status IN ('INACTIVE','ACTIVE','SUSPENDED')`.
- `ck_service_activation_revision`: `revision >= 1`.
- `ck_service_activation_time_order`: `updated_at >= created_at`.

Keys/indexes/FK:

- PK `pk_service_activation (id)`.
- Unique `uq_service_activation_subject (subject_id)`.
- Index `ix_service_activation_status (status)`.
- FK `fk_service_activation_subject (subject_id) → subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`, immediate, validated, non-deferrable.

### 4.4 `activation_consent.service_activation_transition` — 18 columns

| Column | Type | Null | Default | Frozen constraint role |
| --- | --- | --- | --- | --- |
| `id` | `uuid` | NO | none | PK `pk_service_activation_transition` |
| `activation_id` | `uuid` | NO | none | activation FK; revision unique |
| `subject_id` | `uuid` | NO | none | subject FK |
| `from_status` | `text` | NO | none | status and matrix checks |
| `to_status` | `text` | NO | none | status and matrix checks |
| `from_revision` | `integer` | NO | none | nonnegative |
| `to_revision` | `integer` | NO | none | exactly `from_revision + 1`; revision unique |
| `source_channel` | `text` | NO | none | channel and channel/domain shape |
| `source_action_domain` | `text` | NO | none | action unique; channel/actor shape |
| `source_action_event_id` | `text` | NO | none | action unique; nonblank 1-128 |
| `status_event_domain` | `text` | NO | none | exactly `ACTIVATION_STATUS`; status unique |
| `status_event_id` | `text` | NO | none | status unique; nonblank 1-128 |
| `request_fingerprint` | `text` | NO | none | lowercase SHA-256 shape |
| `actor_account_id` | `uuid` | YES | none | account FK; actor shape |
| `actor_employee_id` | `uuid` | YES | none | employee FK; actor shape |
| `reason_code` | `text` | NO | none | nonblank 1-64 |
| `consent_record_id` | `uuid` | YES | none | consent FK; actor shape |
| `occurred_at` | `timestamptz` | NO | `now()` | transition time |

Exact checks:

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

Keys/indexes/FKs:

- PK `pk_service_activation_transition (id)`.
- Unique `uq_activation_transition_revision (activation_id, to_revision)`.
- Unique `uq_activation_transition_action (source_action_domain, source_action_event_id)`.
- Unique `uq_activation_transition_status_event (status_event_domain, status_event_id)`.
- Index `ix_activation_transition_occurred (subject_id, occurred_at DESC)`.
- FK `fk_activation_transition_activation (activation_id) → activation_consent.service_activation(id)`.
- FK `fk_activation_transition_subject (subject_id) → subject_number.business_subject(id)`.
- FK `fk_activation_transition_actor_account (actor_account_id) → customer_iam.customer_account(id)`.
- FK `fk_activation_transition_actor_employee (actor_employee_id) → operations_iam.employee_account(id)`.
- FK `fk_activation_transition_consent (consent_record_id) → activation_consent.consent_record(id)`.
- Every FK is `ON UPDATE RESTRICT ON DELETE RESTRICT`, immediate, validated and non-deferrable.

### 4.5 Late order-consent FK and FK classifications

- Add exactly:

```sql
ALTER TABLE catalog_order.customer_order
    ADD CONSTRAINT fk_customer_order_consent
    FOREIGN KEY (purchase_consent_id)
    REFERENCES activation_consent.consent_record(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
```

- It is the V007 `ALTER_FK` decision from the accepted FK catalog, not a `DEFERRABLE_FK`.
- V007 has 10 ordinary table-local `FK` decisions plus this one ordinary immediate `ALTER_FK`.
- V007 adds no `NO_FK` decision. The two existing root IVR `call_id` `NO_FK` decisions remain unchanged.
- `consent_record.source_ref` is opaque text and not an FK decision. No inferred FK is added.
- V007 has no deferred-to-later FK. The only accepted deferrable constraints are the two V008 evidence-cycle FKs; they remain absent.
- No FK permits cascade, `SET NULL`, `SET DEFAULT` or forward reference.

### 4.6 Local functions and triggers

Create under `activation_consent_owner`:

1. `activation_consent.reject_row_mutation() RETURNS trigger`
   - `LANGUAGE plpgsql`, `SECURITY INVOKER`.
   - `SET search_path = activation_consent, pg_temp`.
   - Always raises SQLSTATE `P0001` for an attempted update/delete.
   - No generated IDs, transaction control, dynamic SQL or cross-schema call.
   - No runtime direct-execute grant; this follows the existing append-only-function pattern in V004-V006.
2. `activation_consent.guard_service_activation() RETURNS trigger`
   - `LANGUAGE plpgsql`, `SECURITY INVOKER`.
   - `SET search_path = activation_consent, pg_temp`.
   - Rejects any change to `id`, `subject_id` or `created_at` using `IS DISTINCT FROM`; otherwise returns `NEW`.
   - Does not enforce the service transition matrix, head/ledger matching, lock order or revision increment.

Create exactly these triggers:

- `trg_consent_record_immutable`: `BEFORE UPDATE OR DELETE ON activation_consent.consent_record FOR EACH ROW EXECUTE FUNCTION activation_consent.reject_row_mutation()`.
- `trg_service_activation_guard`: `BEFORE UPDATE ON activation_consent.service_activation FOR EACH ROW EXECUTE FUNCTION activation_consent.guard_service_activation()`.
- `trg_activation_transition_immutable`: `BEFORE UPDATE OR DELETE ON activation_consent.service_activation_transition FOR EACH ROW EXECUTE FUNCTION activation_consent.reject_row_mutation()`.

The SQLSTATE is frozen for executable evidence; no new product/domain error token is specified by this plan.

### 4.7 Exact ACL and revoke waves

The V004-created schema USAGE grants on `activation_consent` to `activation_consent_owner`, `activation_consent_role`, `ivr_role` and `catalog_order_role` remain unchanged. V007 does not recreate roles/schema or grant schema `CREATE`.

Under `SET LOCAL ROLE activation_consent_owner`:

- `REVOKE ALL` on all three V007 tables from `PUBLIC`.
- `REVOKE ALL` on both V007 trigger functions from `PUBLIC`.
- Grant `activation_consent_role` `SELECT, INSERT` on all three V007 tables.
- Grant `activation_consent_role` `UPDATE (status, revision, updated_at)` on `service_activation` only.
- Grant `activation_consent_role` `EXECUTE` on `guard_service_activation()` only.
- Grant `ivr_role` exact column `SELECT` on:
  - `service_activation(id, subject_id, status, revision)`.
  - `consent_record(subject_id, purpose, channel, policy_code, policy_version, prompt_version, action, event_domain, event_id, accepted_at)`.
- Grant `catalog_order_role` exact column `SELECT` on:
  - `service_activation(id, subject_id, status, revision)`.
  - `consent_record(id, subject_id, purpose, channel, event_domain, event_id, policy_code, policy_version, prompt_version, action, source_type, source_ref, account_id, request_fingerprint, accepted_at)`.
- Grant no V007 table privilege to any other role.
- Grant no direct `EXECUTE` on `reject_row_mutation()` to a runtime role.

After `RESET ROLE`, owner-specific cross-module waves are:

1. `SET LOCAL ROLE subject_number_owner`
   - `GRANT USAGE ON SCHEMA subject_number TO activation_consent_role`.
   - `GRANT SELECT (id, owner_account_id, owner_organization_id, revision) ON subject_number.business_subject TO activation_consent_role`.
   - `RESET ROLE`.
2. `SET LOCAL ROLE customer_iam_owner`
   - `GRANT USAGE ON SCHEMA customer_iam TO activation_consent_role`.
   - `GRANT SELECT (id, status) ON customer_iam.customer_account TO activation_consent_role`.
   - `RESET ROLE`.
3. `SET LOCAL ROLE organization_access_owner`
   - `GRANT USAGE ON SCHEMA organization_access TO activation_consent_role`.
   - `GRANT SELECT (id, status) ON organization_access.organization TO activation_consent_role`.
   - `GRANT SELECT (organization_id, account_id, role, status) ON organization_access.organization_membership TO activation_consent_role`.
   - `RESET ROLE`.

Explicit denials to assert:

- At latest, `activation_consent_role` has exactly the V007-added `USAGE` grants on `subject_number`, `customer_iam` and `organization_access`, each without `CREATE`, and only the declared column reads listed in section 4.7. Its existing `activation_consent` and other prior-wave schema privileges remain intact; it has no `operations_iam` schema USAGE or table privilege, and later operations flows use the owner collaborator.
- No runtime role has `CREATE` on `activation_consent`, `subject_number`, `customer_iam` or `organization_access`.
- No runtime role has whole-table `UPDATE`, `DELETE`, `TRUNCATE`, `REFERENCES` or `TRIGGER` on V007 tables.
- `ivr_role` and `catalog_order_role` have no unlisted V007 column read, table-level `SELECT`, write or row-lock-enabling privilege.
- `activation_consent_role` has no foreign write or foreign row-lock privilege.
- `PUBLIC` has no schema USAGE/CREATE, table privilege or function EXECUTE.

## 5. Exact DDL Order and Execution Context

The migration follows the established V004-V006 executable pattern, not a mechanical table-only split:

1. Assert by construction that the schema/roles already exist; do not create or alter them.
2. `SET LOCAL ROLE activation_consent_owner`.
3. Create `consent_record` with its columns, defaults, PK, unique and 15 checks, but no FK yet.
4. Create `ix_consent_subject_purpose`, then `ix_consent_session`.
5. Create `service_activation` with its columns, defaults, PK, unique and 3 checks, but no FK yet.
6. Create `ix_service_activation_status`.
7. Create `service_activation_transition` with its columns, default, PK, three uniques and 13 checks, but no FK yet.
8. Create `ix_activation_transition_occurred`.
9. Create `reject_row_mutation()` and `guard_service_activation()`.
10. Create the three triggers.
11. Apply the activation-owner `PUBLIC`, owned-table, cross-reader and trigger-function ACL statements in section 4.7.
12. `RESET ROLE` before touching objects owned by any other module or adding cross-schema FKs.
13. Run the `subject_number_owner`, `customer_iam_owner` and `organization_access_owner` grant waves, with `RESET ROLE` after each wave.
14. As the migration identity, add the 10 table-local FKs in parent-safe order:
    1. four `consent_record` FKs;
    2. `fk_service_activation_subject`;
    3. `fk_activation_transition_activation`;
    4. `fk_activation_transition_subject`;
    5. `fk_activation_transition_actor_account`;
    6. `fk_activation_transition_actor_employee`;
    7. `fk_activation_transition_consent`.
15. As the migration identity, add late `fk_customer_order_consent` after `consent_record` exists.
16. End with no active local role and no V008+ statement.

No `SET ROLE` may span a foreign-owner grant or the FK tail. No `ALTER OWNER`, role membership, default privilege or schema `CREATE` grant is added.

## 6. Database Backstops Versus Future Service Rules

### 6.1 Consent facts

Database-owned in V007:

- Explicit `ACCEPT`/`DECLINE` enum shape.
- Purpose, channel, event-domain and source-type registries.
- Exactly one source identity column is non-null according to `source_type`.
- Channel/source compatibility.
- Object-shaped redacted snapshot, lowercase 64-hex snapshot hash and fingerprint.
- Nonblank bounded policy, prompt, source-ref and event identities.
- Domain-scoped uniqueness `(event_domain, event_id)`.
- Parent existence and append-only update/delete rejection.

Future service-owned and not implemented:

- Current policy/prompt/key/content validation.
- Explicit acceptance rules for activation/purchase.
- Exact deterministic source-ref and fingerprint construction.
- Replay-versus-conflict behavior beyond the unique database key.
- Subject/account/channel/purpose currentness and purchase-order semantic matching.

### 6.2 Activation head

Database-owned in V007:

- One row per subject.
- Status registry, positive revision and timestamp order.
- Immutable `id`, `subject_id`, `created_at`.
- Runtime update surface restricted to `status`, `revision`, `updated_at`.

Future service-owned and not implemented:

- Subject lifecycle advisory lock and activation row lock.
- Conditional `WHERE revision = expected_revision` update and bounded reread.
- Exact head-to-ledger atomic matching.
- `INACTIVE→ACTIVE`, operations suspend/resume authorization and suspended IVR rejection.
- Trial call/reuse behavior.

### 6.3 Transition ledger

Database-owned in V007:

- Append-only facts.
- Allowed status symbols and broad matrix.
- `to_revision = from_revision + 1`.
- Unique `(activation_id, to_revision)`.
- Unique action identity and unique `ACTIVATION_STATUS` event identity.
- Channel/action-domain shape.
- Operations rows require an employee actor and no consent; non-operations rows require consent.
- Parent existence and bounded identity/fingerprint/reason shapes.

Future service-owned and not implemented:

- Narrow source/state matrix: only IVR/client first activation, operations suspend and operations resume.
- Replay/conflict mapping, deterministic status event ID and fingerprint semantics.
- Actor authorization and account/employee currentness.
- Same-transaction consent/head/transition/trial/Outbox/audit orchestration.

Tests must prove the broad check definition exactly and may demonstrate, inside a rolled-back transaction, that a shape-valid row can still be service-invalid. They must not tighten the check or call that row business-approved.

## 7. Cumulative Assertion Evolution

### 7.1 Staged migration-prefix evidence

Preserve the existing V005 prefix proof and add V004/V006 boundaries in the same PostgreSQL container before applying latest:

1. In the single class `@BeforeAll` staged setup, migrate the clean database to target `004`; record `migrationsExecuted=4`, no `activation_consent` table and no `fk_customer_order_consent`.
2. Immediately after target `004` succeeds and before configuring or migrating target `005`, `@BeforeAll` must invoke a dedicated staged helper such as `probeV004OrphanConsentOrderAndRollback()`. The helper starts a transaction; creates only the minimum valid customer/account, organization, subject and catalog/version/product parents required by the existing V004 order insert; inserts a fully valid `customer_order` using a random non-null `purchase_consent_id` for which no `consent_record` exists; asserts the insert succeeds at V004; rolls the whole transaction back; and verifies the probe order and every probe parent are absent. The helper returns an immutable result such as `V004OrphanConsentProbeEvidence` containing the generated probe IDs plus explicit `insertSucceeded` and `rollbackVerified` facts. `@BeforeAll` assigns that returned value exactly once to a private static field such as `stagedV004OrphanConsentEvidence`; the field starts unassigned/null, is not mutated after assignment and is read only by later tests. The helper may roll back in failure cleanup, but it must rethrow every SQL/assertion/runtime failure; `@BeforeAll` must fail immediately and must not catch-and-continue or advance migration unless the successful immutable evidence value was returned and assigned.
3. Only after `stagedV004OrphanConsentEvidence` is captured, retarget/migrate to `005`; record `migrationsExecuted=1` and preserve all existing V005 absence assertions: no V006 function/tables/late reservation FK, nullable reservation column.
4. Retarget/migrate to `006`; record `migrationsExecuted=1`; assert exactly seven entitlement tables, zero activation tables, no V007 tables, functions, triggers, order-consent FK or V007 table-level/column grants. Preserve all V004-created and other prior-wave schema privileges; specifically assert `activation_consent_role` has no `USAGE` on `subject_number`, `customer_iam` or `organization_access` until V007. Do not assert absence of `USAGE` on `activation_consent`, `platform`, `audit` or any other prior-wave schema.
5. Remove target and migrate latest/V007; record `migrationsExecuted=1`; assert exact V001-V007 Flyway history and no V008/V009 object. The required order is therefore V001-V004 migration → dedicated V004 probe/rollback → immutable evidence capture → V005 migration → V006 migration → latest/V007 migration → JUnit `@Test` execution; no test ordering mechanism or second container participates.

This preserves absence evidence at the prefix where absence is true instead of deleting assertions that become false in the final cumulative database.

### 7.2 Existing method/helper evolution

- Rename `v001ThroughV006MigrateCleanPostgres16WithoutSkipping` to `v001ThroughV007MigrateCleanPostgres16WithoutSkipping`; add V007 history and the 4→1→1→1 staged execution assertions, including that the assigned-once V004 evidence is non-null and successful before the later migration results are accepted.
- Rename `v004ForeignKeyAndAbsentConsentFkBackstops` to `v004ForeignKeyAndStagedConsentFkBackstops`; retain all 11 V004 FK definitions and V004-time absence proof, but do not attempt the V004 insert from this `@Test`. Instead, assert the immutable `stagedV004OrphanConsentEvidence` captured by `@BeforeAll` reports insert success and verified rollback with no probe rows remaining. Then, against the final V007 database, separately prove a fully valid order succeeds only with a real method-local `consent_record` parent and a fully valid order using a random non-null orphan `purchase_consent_id` is rejected with SQLSTATE `23503` and exact constraint `fk_customer_order_consent`; assert the final late-FK definition/properties separately. This evolution is count-neutral, uses the existing single container and single `@BeforeAll`, and adds no dynamic ordering, downgrade, committed orphan, disabled FK or extra `@Test` method.
- Replace final-database `assertNoV007PlusObjects` with:
  - staged-V006 `assertNoV007PlusObjects` evidence before V007;
  - final `assertNoV008PlusObjects` evidence after V007.
- Evolve `assertV006PresenceAndLateFk` to assert V006 objects still exist, V007 now exists and both late FKs are immediate/non-deferrable; keep a staged-V006 variant for V007 absence.
- Add V007 table/check/index/FK/default/function/trigger constants and exact PostgreSQL 16 `pg_get_constraintdef` maps; compare whole normalized definitions, never substring semantics.
- Update class Javadoc, wave labels, table totals and exact method count from PA6/V006/45 to PA7/V007/51.
- Update cumulative schema-USAGE matrices so V004/V005/V006 retain `activation_consent_role`'s existing `activation_consent` and other prior-wave schema privileges, and V006 specifically lacks only the three V007 schema additions: `subject_number`, `customer_iam` and `organization_access`. At latest, assert exactly those three new `USAGE` grants without `CREATE`, retaining `activation_consent` and all other prior-wave grants; assert their exact table/column reads: `subject_number.business_subject(id, owner_account_id, owner_organization_id, revision)`, `customer_iam.customer_account(id, status)`, `organization_access.organization(id, status)`, and `organization_access.organization_membership(organization_id, account_id, role, status)`. Preserve the `operations_iam` denial.
- Update cumulative full-role/full-column matrices in V004/V005/V006 helpers to admit only the four exact activation-consent S1 read sets. Do not convert those matrices to spot checks.
- Preserve the existing `ivr_role` and `catalog_order_role` S1/entitlement grants and add their exact V007 activation-table reads only in the V007 matrix.
- Preserve exact PUBLIC/function matrices and add the two local V007 functions with correct owner, invoker flag, search path and execute boundary.
- Preserve every V001-V006 check, trigger, unique, FK, runtime denial, concurrency and rollback assertion.

### 7.3 Existing fixture evolution caused by the late FK

Every final-V007-database valid `customer_order` insert must reference a real method-local `consent_record`; random orphan purchase-consent IDs fail the new FK. Preserve the distinct V004 behavior through the dedicated `@BeforeAll` helper and its immutable captured evidence, which the later `v004ForeignKeyAndStagedConsentFkBackstops` test asserts without rerunning the V004 insert. Update all final-database helper call sites found through `insertCustomerOrder`, direct `INSERT INTO catalog_order.customer_order`, `customerOrderProbeSql` positive controls and V004/V006 concurrency/setup paths.

- Add one bounded fixture helper that inserts a valid `PURCHASE`/`WEB`/`ACCOUNT` consent using existing method-local subject/account parents, object snapshot, 64-hex hashes and unique `(PURCHASE,event_id)`.
- Do not let `catalog_order_role` insert that row; fixture setup runs as migration identity or `activation_consent_role` according to the assertion.
- Invalid CHECK probes may keep an orphan consent ID only when the exact check is guaranteed to fail before FK evaluation; every positive control and FK-unrelated valid insert uses a real parent.
- Missing-parent FK probes isolate one parent at a time. The new `fk_customer_order_consent` probe uses all other valid parents and expects `23503` plus exact constraint name.
- Committed/concurrency cleanup deletes child orders before consent parents. Because consent is append-only, cleanup may disable only `trg_consent_record_immutable` under the migration identity inside one rollback-on-error cleanup transaction, delete only collected method-local IDs, restore the trigger and assert `tgenabled`; runtime delete/truncate denial evidence remains separate and unchanged.
- Prefer transaction rollback for non-concurrency methods. No global `TRUNCATE`, `CASCADE`, unscoped delete or permanent trigger disable.

### 7.4 Exact method evolution

Current 45 methods → Slice S1 48 → Slice S2 51. Two existing methods are renamed without changing the count. Six additions are justified by non-overlapping observable contracts:

**Slice S1 additions**

1. `v007ActivationConsentInventoryAndGuardShapesAreExact`
2. `v007OwnerPublicRuntimeAndCrossModuleAclsAreExact`
3. `v007ForeignKeyAndLateOrderConsentFkBackstops`

**Slice S2 additions**

4. `v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops`
5. `v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops`
6. `concurrentV007ActivationAndTransitionIdentityHasOneWinner`

Final expected 51-name set:

```text
v001ThroughV007MigrateCleanPostgres16WithoutSkipping
v001ThroughV003InventoryMatchesTheFrozenContract
brokenMigrationFailsVisibly
idempotencyNewReplayConflictAndFinalizeOnce
committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow
concurrentIdempotencyRetryObservesOneWinner
outboxReplayConflictRollbackAndDenylist
auditIsAppendOnlyAndRuntimeWritesAreDenied
auditReasonAndDenylistRulesAreEnforced
functionsHaveFixedSearchPathAndNoTransactionControl
ownerPublicAndRoleExecutionBoundariesAreExact
hostileSearchPathCannotRedirectFunctionEffects
mutableHeadsEnforceLifecycleRevisionAndNamedChecks
localImmutabilityGuardsRejectRelationAndRotationChanges
identityCollisionAndMembershipRevokeRegrantSemantics
concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner
runtimeAndCrossModulePrivilegesAreExactAndWritesDenied
lastEnabledAdminTransactionIsSerializedAndFailsClosed
v003SubjectNumberCatalogAndGuardsAreExact
v003OwnerPublicAndRuntimeAclsAreExact
subjectOwnerSetOnceAndRealNameSeparation
bindingLifecycleTerminalityAndActiveNumberUniqueness
appendOnlyClaimAndForeignKeyBackstops
riskOpenClearReopenRevisionProtocol
concurrentActiveBindingUniquenessHasOneWinner
v004CatalogOrderInventoryAndGuardsAreExact
v004OwnerPublicRuntimeAndCrossModuleAclsAreExact
v004CatalogVersionLifecycleAndGuardBackstops
v004OrderPaymentLifecycleAndAppendOnlyBackstops
v004ForeignKeyAndStagedConsentFkBackstops
concurrentCatalogPublicationAndCommerceIdentityHasOneWinner
v005IvrTaskSessionCallInventoryAndGuardShapesAreExact
v005OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005TaskTtlLifecycleAndGuardBackstops
v005SessionCallCanonicalCallerAndReciprocalLinkBackstops
v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops
v005ForeignKeyAndDeferredReservationFkBackstops
concurrentV005TaskClaimAndActiveCallHasOneWinner
v006EntitlementReservationUsageInventoryAndGuardShapesAreExact
v006OwnerPublicRuntimeAndCrossModuleAclsAreExact
v006IssueFirstTrialGrantReplayConflictAndPolicySemantics
v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops
v006ReservationUsageForeignKeyAndLateFkBackstops
v006NestedOutboxAtomicityAndReplayRollbackEvidence
concurrentV006FirstTrialAndReservationIdentityHasOneWinner
v007ActivationConsentInventoryAndGuardShapesAreExact
v007OwnerPublicRuntimeAndCrossModuleAclsAreExact
v007ForeignKeyAndLateOrderConsentFkBackstops
v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops
v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops
concurrentV007ActivationAndTransitionIdentityHasOneWinner
```

No placeholder, `@Disabled`, assumption, Docker skip or relaxed set comparison is allowed.

## 8. Behavioral Implementation Slices

Two slices are the minimum practical count. One slice would combine migration/security/catalog acceptance with heavier behavior/concurrency debugging; three would mechanically split one migration or one cumulative test class without creating a new contract boundary.

### 8.1 Slice S1 — Runnable V007 catalog, security and referential boundary

**Objective.** Produce the complete V007 migration and prove exact inventory, owner/runtime/cross-module/PUBLIC ACLs, all 11 FK decisions and staged-prefix timing while preserving a runnable V001-V007 prefix.

**Allowed paths/changes.** New V007 migration; cumulative S1 changes in `PostgresContainerIT`; CI expected set/count 48; README V007 S1 summary/count; S1 implementation artifact. No S2 behavior methods yet.

**Dependencies.** Accepted V001-V006 prefix at base commit; existing roles/schema; PostgreSQL 16; current 45-method suite; exact dictionaries and grant matrix in sections 4-5.

**Required implementation.** Complete migration in section 5; staged V004/V005/V006/latest setup; exact V007 constants/maps; three S1 methods; all cumulative matrix/fixture changes required for the final database.

**Invariants.** V001-V006 blobs unchanged; no V008+ object; no service function; no schema/role recreation; no undeclared grant; no weakened prior method; order fixtures use real consent parents.

**Test methods/assertions.**

- `v007ActivationConsentInventoryAndGuardShapesAreExact`: exact 3/43/31/3 PK/5 unique/4 secondary/12 index/2 function/3 trigger/6 default/0 id-default/0 sequence inventory; owners; exact function flags/search paths/no transaction control/generated IDs; exact trigger relation/timing/event/function; no V008+ object.
- `v007OwnerPublicRuntimeAndCrossModuleAclsAreExact`: exhaustive schema/table/column/function matrix, positive legal activation update control, runtime `42501` denials, owner trigger `P0001`, zero PUBLIC, zero foreign writes/row locks/CREATE, no operations-iam access.
- `v007ForeignKeyAndLateOrderConsentFkBackstops`: exact 11 definitions/properties; staged V004/V006 absence and final presence; all missing-parent `23503` probes with exact names; representative parent-delete RESTRICT; a final-database valid order succeeds with a real consent parent and a fully valid order with a random non-null orphan `purchase_consent_id` fails with SQLSTATE `23503` and exact constraint `fk_customer_order_consent`; rollback leaves no fixtures.

**Expected outcome.** Full V001-V007 prefix applies; exactly 48 named container tests pass; CI/source/XML sets equal; README truthfully describes S1 database evidence only.

**Completion signal.** One final full PostgreSQL 16 profile reports 48 tests, zero skipped/failure/error; default verify, boot/firewall, V001-V006 immutability, diff/check/status validations pass; S1 artifact records commands and hashes.

**Stop condition.** Any required change to V001-V006, binding docs, POM/application code, schema/role set, frozen dictionary, narrow transition rules or method acceptance criteria; any unexplained catalog/ACL mismatch; any need for V008+.

### 8.2 Slice S2 — Executable lifecycle, rollback and bounded concurrency backstops

**Objective.** Prove the database-visible consent/head/transition behavior and fail-closed uniqueness/rollback/concurrency semantics without adding application-service behavior.

**Allowed paths/changes.** `PostgresContainerIT` S2 methods/helpers; CI expected set/count 51; README final V007 evidence/count; S2 implementation artifact. V007 is frozen after accepted S1 unless the controller explicitly authorizes a bounded contract correction.

**Dependencies.** Accepted S1 migration/catalog/ACL/FK evidence and V007 hash.

**Required implementation and assertions.**

- `v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops`:
  - valid IVR/session, account and employee source/channel shapes;
  - all 15 checks with exact `23514` + constraint name and valid positive controls;
  - default `accepted_at` populated by PostgreSQL;
  - domain-scoped unique event winner, same text in another domain allowed;
  - owner update/delete rejected `P0001`; runtime update/delete/truncate rejected `42501`;
  - forced later failure rolls back the consent insert; no service replay/currentness claim.
- `v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops`:
  - activation defaults `INACTIVE`, revision 1, database timestamps and time-order check;
  - one activation per subject;
  - legal runtime update of only status/revision/updated_at; immutable owner changes rejected `P0001`, runtime immutable-column change `42501`;
  - all 3 head checks and 13 transition checks with exact names and positive controls;
  - exact broad matrix, including rolled-back shape-valid/service-invalid controls so no narrower DB claim is made;
  - transition `from_revision/to_revision` progression, three unique identities, actor/channel shapes and append-only denial;
  - a transaction that inserts consent, head and transition, updates the head, then forces failure leaves no partial fact/head change.
- `concurrentV007ActivationAndTransitionIdentityHasOneWinner`:
  - bounded two-connection, distinct `application_name`, `statement_timeout`, future timeout and observed lock wait; no sleep-only assertion;
  - one winner for `uq_consent_event`;
  - one winner for `uq_service_activation_subject`;
  - one winner each for transition revision, action and status-event identities with loser `23505` + exact constraint;
  - conditional activation-head update from one expected revision affects exactly one transaction; loser observes zero update after serialization and rereads current state;
  - whole-transaction forced failure leaves no partial consent/head/transition;
  - method-local FK-safe cleanup with named trigger restoration.

**Invariants.** Tests use transient SQL only as database probes and label service behavior deferred. They do not assert deterministic fingerprint content, policy currentness, authorization, lock-service ordering, Outbox/audit, trial orchestration or narrow service matrix.

**Expected outcome.** Exactly 51 methods pass and demonstrate all V007 claims that the database can actually enforce.

**Completion signal.** One final full PostgreSQL 16 profile reports 51 tests, zero skipped/failure/error and exact set equality; bounded concurrency repeat is green; default verify, boot/firewall, migration immutability and whitespace checks pass; V007 hash is unchanged from accepted S1.

**Stop condition.** Any need to alter the accepted S1 migration without controller authorization; any test that can pass only by weakening a prior assertion, granting cleanup privilege to a runtime role, disabling a trigger permanently, adding service code or claiming a non-database rule.

## 9. Validation Commands and Expected Assertions

### 9.1 Static inventory and method-set checks

```bash
set -euo pipefail

EXPECTED_METHODS="${EXPECTED_METHODS:-48}"
case "$EXPECTED_METHODS" in
  48|51) ;;
  *) echo "EXPECTED_METHODS must be 48 (S1) or 51 (S2)" >&2; exit 1 ;;
esac

# Exact sorted migration set: seven files V001-V007; rejects missing, extra or V008+ files.
expected_migrations=$(printf '%s\n' \
  V001__platform_schemas_roles_idempotency_outbox_audit.sql \
  V002__identity_organization_and_employee.sql \
  V003__subject_number_and_claim.sql \
  V004__catalog_order_and_payment.sql \
  V005__ivr_task_session_call.sql \
  V006__trial_entitlement_issuance_reservation_usage.sql \
  V007__activation_and_consent.sql)
actual_migrations=$(find backend/src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' -printf '%f\n' | sort)
test "$(printf '%s\n' "$actual_migrations" | wc -l)" -eq 7
test "$actual_migrations" = "$expected_migrations"

# Source method names/count; expected 48 in S1, 51 in S2, no duplicates.
perl -ne 'if (/\@Test/) {$t=1; next} if ($t && /void\s+([A-Za-z0-9_]+)\s*\(/) {print "$1\n"; $t=0}' \
  backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java \
  | tee /tmp/pa7-source-methods.txt
test "$(wc -l < /tmp/pa7-source-methods.txt)" -eq "$EXPECTED_METHODS"
test "$(sort /tmp/pa7-source-methods.txt | uniq -d | wc -l)" -eq 0

# V001-V006 tracked blobs must remain exact.
test "$(git hash-object backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql)" = d7e2e6879701a5db6517434cbb9b22d5c8f2d707
test "$(git hash-object backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql)" = 562302e19e8d7b84399ed3dff44eab00e411cfae
test "$(git hash-object backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql)" = 4c274b53c7ff75416e16e7ae8dd7d0d9973170b7
test "$(git hash-object backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql)" = 3197138f7f7b5740f2bd0b6a4cd7b0cbd76b2cf7
test "$(git hash-object backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql)" = f0d318199e33801a1cefbab87db4def07cd81dc0
test "$(git hash-object backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql)" = ea2114edfd663344389a914696af19350c23a939
git diff --exit-code -- backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql \
  backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql \
  backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql \
  backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql \
  backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql \
  backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql
```

Expected: exactly the sorted V001-V007 filename set exists (count 7), source method count equals `EXPECTED_METHODS` (48 for S1 or 51 for S2), duplicate set is empty and all six frozen blob IDs match. Run the same block unchanged with `EXPECTED_METHODS=48` at S1 and `EXPECTED_METHODS=51` at S2.

### 9.2 Build and PostgreSQL 16 evidence

```bash
cd backend
./mvnw -B -ntp verify
docker info

# Optional focused iteration, one method at a time.
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT#v007ActivationConsentInventoryAndGuardShapesAreExact verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT#v007OwnerPublicRuntimeAndCrossModuleAclsAreExact verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT#v007ForeignKeyAndLateOrderConsentFkBackstops verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT#v007ConsentShapeDefaultsAppendOnlyAndRollbackBackstops verify
./mvnw -B -ntp -Pcontainer-smoke -Dit.test=PostgresContainerIT#v007ActivationHeadTransitionRevisionMatrixAndImmutabilityBackstops verify

# Bounded concurrency repeat; never unbounded.
for attempt in 1 2 3; do
  ./mvnw -B -ntp -Pcontainer-smoke \
    -Dit.test=PostgresContainerIT#concurrentV007ActivationAndTransitionIdentityHasOneWinner verify
done

# Exactly one final full run per accepted slice checkpoint.
./mvnw -B -ntp -Pcontainer-smoke verify
```

Expected default verify: existing 10 Surefire tests, zero skipped/failure/error. Expected final Failsafe: one `PostgresContainerIT` XML, 48 tests in S1 or 51 in S2, zero skipped/failure/error. PostgreSQL image remains the current pinned 16.x Testcontainers baseline; no Docker-unavailable skip path exists.

### 9.3 Source/CI/XML exact equality

After the final container run, execute a bounded read-only comparison equivalent to the CI parser:

```bash
cd backend
python3 - <<'PY'
import os, pathlib, re, xml.etree.ElementTree as ET

expected_methods = int(os.environ.get('EXPECTED_METHODS', '48'))
assert expected_methods in (48, 51)

root = pathlib.Path('..')
java = (root / 'backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java').read_text()
source = set(re.findall(r'@Test\s+void\s+([A-Za-z0-9_]+)\s*\(', java))
ci = (root / '.github/workflows/ci.yml').read_text()
block = ci.split('expected = {', 1)[1].split('}', 1)[0]
expected = set(re.findall(r'"([A-Za-z0-9_]+)"', block))
xml_path = next(pathlib.Path('target/failsafe-reports').glob('TEST-*PostgresContainerIT.xml'))
suite = ET.parse(xml_path).getroot()
cases = {case.get('name') for case in suite.findall('testcase')}
assert len(source) == expected_methods
assert source == expected == cases
assert int(suite.get('tests', '0')) == expected_methods
assert int(suite.get('skipped', '0')) == 0
assert int(suite.get('failures', '0')) == 0
assert int(suite.get('errors', '0')) == 0
print('source/CI/XML method sets equal:', len(source))
PY
```

Run the unchanged comparison at both checkpoints by setting `EXPECTED_METHODS=48` for S1 and `EXPECTED_METHODS=51` for S2 before invoking it. Source, CI and XML names/counts are checked against the same value; CI retains its inline fail-closed XML parser and updates all V006 labels/error text to V007 S1/S2 accurately.

### 9.4 Required database assertions

The container suite must prove:

- exact columns/nullability/defaults and exact normalized check definitions;
- exact PK/unique/index/FK sets and PostgreSQL metadata (`confupdtype/confdeltype='r'`, `condeferrable=false`, `condeferred=false`, `convalidated=true`);
- final V007 valid orders use real consent parents; a fully valid orphan-consent order fails with SQLSTATE `23503` and exact `fk_customer_order_consent`, separately from the successful rolled-back V004 probe;
- every missing parent raises `23503` with exact FK name;
- every invalid check raises `23514` with exact check name and has a valid positive control;
- every duplicate identity raises `23505` with exact unique name;
- owner append-only mutation raises `P0001`; runtime undeclared mutation raises `42501`;
- legal runtime head update succeeds only for granted columns;
- forced transaction failures roll back every method-local row/update;
- concurrency has one winner, one exact loser/current-state outcome and no partial rows;
- PUBLIC and undeclared role probes are negative while every declared grant has a positive probe;
- staged V004/V006 absence and final V007 presence are both executable.

### 9.5 Boot, fat-jar and generated-report firewall

```bash
cd backend
timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
if grep -Eq 'BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)' target/jar-index.txt; then
  echo 'forbidden runtime dependency in fat jar'
  exit 1
fi
```

Expected: boot starts; no Flyway/PostgreSQL/Testcontainers or other forbidden runtime dependency enters the production jar.

### 9.6 Whitespace and scope checks

```bash
set -euo pipefail
git diff --check -- \
  backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java \
  .github/workflows/ci.yml backend/README.md

# V007 and new evidence artifacts may be untracked; exit 1 means a clean new-file diff.
for path in \
  backend/src/main/resources/db/migration/V007__activation_and_consent.sql \
  docs/gateflow/pa7-v007-activation-consent-foundation-s1-implementation.md \
  docs/gateflow/pa7-v007-activation-consent-foundation-s2-implementation.md; do
  if test -e "$path"; then
    output=$(git -c core.autocrlf=false diff --no-index --check /dev/null "$path" 2>&1) || status=$?
    test "${status:-0}" -eq 1
    test -z "$output"
    unset status
  fi
done

git status --short
```

Expected: no whitespace error; only active-slice allowed paths differ from the task-start baseline; no stage/commit/branch/PR mutation is performed by implementation tasks unless separately authorized by the controller.

## 10. Documentation, Risks, Questions and Reporting

### 10.1 Documentation decision

- Update `backend/README.md` because it is the executable backend evidence guide and currently states V001-V006/45 and V007 absence.
- Synchronize title/summary, V007 inventory, S1/S2 method counts, CI count and evidence boundary.
- Preserve module boundaries, toolchain pins, contract jobs, boot/firewall language and the statement that local/CI evidence is not production acceptance.
- Do not edit architecture/schema sources; they are binding inputs, not implementation output.
- Gateflow implementation/review/controller artifacts are process evidence, not product documentation.

### 10.2 Residual risks and destinations

| Risk | Classification | Owner / destination |
| --- | --- | --- |
| Narrow activation transition authorization and source/state matrix are not DB-enforced | Intentional service boundary | Activation-consent application-service work unit; docs/14 §9.11, §9.19, §9.25, §10.20 |
| Head/ledger atomic matching and lock order are not enforced by a cross-table trigger | Intentional service boundary | Activation-consent/IVR/catalog-order transaction implementation |
| Policy/prompt currentness, deterministic source refs and semantic fingerprints are not generated by V007 | Deferred, not guessed | Product/activation-consent/API/IVR owners; docs/14 §10.17/§10.20 |
| Purchase consent/order same-transaction semantic validation is absent | Deferred application service | Catalog-order + activation-consent purchase work |
| Operations employee authorization is absent and activation role has no operations-iam access | Intentional collaborator boundary | Operations-IAM lock collaborator + activation operations service |
| Runtime role provisioning, authentication and production role switching | Deferred production wiring | Platform/Database/Security (Q17/Q23 destination) |
| V008 reciprocal deferred FKs and V009 sharing | Future waves | PA8/PA9 |
| Append-only committed fixture cleanup is delicate | Test-only implementation risk | V007 test author; method-local IDs, migration identity, trigger restore assertions |
| Forward-only migration has no destructive down migration | Accepted Flyway model | Database operations; future expand/contract migration if change is needed |

These risks are classifications, not new PA7 requirements.

### 10.3 Open questions

No blocking open question is known at plan completion. If implementation proves that the frozen column/check/FK/grant/trigger dictionaries cannot coexist on PostgreSQL 16 or that an acceptance assertion requires a schema/state/scope change, stop and report the exact source lines, SQL/catalog evidence and affected slice. Do not amend a source of truth or silently choose an alternative.

### 10.4 Completion-report format

Each implementation slice report must include:

1. Task label, provider/runtime and slice.
2. Changed paths and SHA-256/blob hash for V007, test class, CI, README and slice artifact.
3. Exact inventory totals and active method evolution (45→48 or 48→51).
4. Exact commands run and outcomes: default tests, focused/full PostgreSQL 16, XML equality, boot/firewall, immutable blobs, whitespace/scope.
5. ACL positive/negative, staged-V004 orphan-insert success/rollback, final `fk_customer_order_consent` orphan rejection (`23503`), other FK/check/trigger/default/rollback/concurrency evidence summary.
6. Any command not run, permission denial or environment blocker.
7. Blocking questions and residual risks with owners/destinations.
8. Completion status: `complete`, `blocked` or `failed`; never claim a later gate.

The final PA7 report must explicitly state that V001-V006 are unchanged, V007 is the only migration added, no V008+ or application-service behavior was introduced, and production acceptance was not performed.

## 11. Explicit No-Overdesign / No-Goal-Drift Argument

- Exactly three frozen tables, two local trigger functions, three triggers and one late FK are added; no extra table, column, enum, registry, view, materialized view, sequence, policy table or workflow engine.
- No generic event-sourcing framework is introduced. The transition table is the frozen activation ledger only.
- No cross-table trigger attempts to implement service orchestration, authorization, lock order, head/ledger atomicity, trial issuance or order validation.
- No narrow service rule is moved into a stricter CHECK than the source specifies.
- No broad database permission is granted to simplify tests or future code.
- No SQL service function, API, JPA/runtime wiring, provider/cloud resource or future-wave object enters either slice.
- Existing cumulative tests are evolved where final V007 makes prior absence false; their original prefix claims are retained as staged executable evidence rather than deleted. The V004 fully valid orphan-consent insert remains a transactional success probe rolled back before advancing, while final V007 valid orders use real consent parents and the same orphan is rejected by the exact late FK.
- Six new methods are the minimum justified set that separates catalog, ACL, FK, consent behavior, activation/transition behavior and bounded concurrency. A seventh method would duplicate those boundaries; fewer would combine materially different failure domains and make review less fail-closed.
- Two slices satisfy the controller cost model. A third slice would be mechanical; one slice would obscure the accepted migration/security checkpoint before concurrency work.
- Goal Confirmation remains the binding scope contract. Deferred risks remain classified and owned; they do not become PA7 requirements.

## 12. Plan-Gate Completion Status

- Derived inventory: 3 tables; 43 columns (19/6/18); 31 checks (15/3/13); 3 PKs; 5 uniques; 4 secondary/12 total indexes; 10 local FKs + 1 immediate late FK; 6 defaults; 2 trigger functions; 3 triggers; 0 service functions; 0 new NO_FK; 0 deferrable FK.
- Derived slices: two — S1 catalog/security/FK boundary, S2 behavior/rollback/concurrency boundary.
- Derived method evolution: 45 current → 48 S1 → 51 S2, with two count-neutral renames and six additions.
- Blocking questions: none.
- Plan status: complete for independent review; no implementation or later gate entered.
