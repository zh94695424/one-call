# PA8 V008 Evidence Foundation — Plan Gate

> Task: `pa8-v008-plan-mimopro-20261030-153434` (plan-fix-1 applied: `pa8-planfix-mimopro-20261030-164854`; A/A reconciliation applied: `pa8-reconcile-mimopro-20261030-172013`; plan-fix-2 applied: `pa8-planfix3-mimo26-20261030-174746`; plan-fix-3 applied: `pa8-planfix4-mimo26-20261030-180824`)
> Gate: `plan` only. No implementation in this gate.
> **Status: READY for independent re-review then implementation.** User explicitly APPROVED A/A reconciliation of BD1/BD2 on 2026-10-30 (MiMo-V2.6-Pro remains plan/fix author). PR-001/PR-005 remain resolved at the design-contract level (see `docs/gateflow/pa8-v008-evidence-foundation-spec-reconciliation.md`); S1 executable evidence for PR-001 is completed by plan-fix-2. RR-001..006 from `docs/reviews/plan-review-20261030-174104.md` are fixed in plan-fix-2 (see `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-2.md`). RR-007/RR-008 from `docs/reviews/plan-review-20261030-180559.md` are accepted as concrete plan execution/evidence defects and are fixed here (see `docs/gateflow/pa8-v008-evidence-foundation-plan-fix-3.md`). No re-review acceptance is claimed. The repaired findings (PR-002/003/004/006/007/008/009 and RR-001..008) still require independent re-review.
> Base commit: `02dfa319bc3b4a1aa29a4252b9269c67aae688aa` (PR #15 merged).
> Authoritative design: `docs/14-production-data-model-and-flyway-schema-spec.md` §4.39–4.43, §5.5, §6.4, §7.4, §9.0.2, §9.27–9.28, §10.21–10.24, §11.1, §11.5, §13.8–13.9 (as amended by the A/A reconciliation); `docs/12-technology-stack-and-system-architecture.md` §5/§6/§14/§15.
> Controller: `docs/gateflow/project-execution-control.md` active PA8 checkpoint (supersedes PA7).

---

## 1. Goal and first-principles alignment

Implement the **frozen V008 persistence contract only**: create `V008__evidence_media.sql` and executable PostgreSQL/Flyway cumulative evidence (migration + transaction-rollback + negative + concurrency tests) for the five `evidence` tables, their catalog/ACL/immutability/revision/lifecycle backstops, and the reciprocal deferred creation cycle. Preserve V001–V007 bytes. Do not implement SQL/application services, upload APIs/workers, COS/KMS provisioning, provider/cloud/IVR integration, production wiring, speculative hardening, or any V009 sharing object.

Why this is the right cut: doc-12 §6 assigns `evidence` ownership of 采集会话/证据元数据/对象清单/哈希与固证状态 and forbids core domain from depending on media-engine/WeChat/AI/notary SDKs. Doc-14 §11.5 step 1 is explicit that V008 creates the `evidence` schema and `evidence_role` only — `sharing_role` does not exist until V009 (F-07). The database contract is the only artifact that can be proven with migration bytes and container tests; application services (§10.23) are named future obligations and are out of this gate.

Success = PostgreSQL/Flyway cumulative evidence aligns with tests, CI, README. Stop and report material design contradiction rather than silently amending it.

---

## 2. Scope fence

### 2.1 In scope

| Item | Source |
| --- | --- |
| Migration `V008__evidence_media.sql` | doc-14 §11.1 line 4112; §11.5 nine-step order |
| 5 tables, 56 columns, 39 named checks, 5 PKs, 5 ordinary unique + 2 partial unique, 8 secondary indexes, 10 FKs (8 ordinary immediate RESTRICT + 2 DEFERRABLE INITIALLY DEFERRED), 10 default columns | §4.39–4.43, §5.5 |
| 5 local SECURITY INVOKER trigger functions, 7 triggers | §10.22 |
| Roles `evidence_owner` + `evidence_role` (NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE) and schema `evidence` | §2.12, §11.5 step 1 |
| V008 grant matrix (evidence_role only; no sharing) | §10.21 |
| Container tests proving catalog/ACL/FK/lifecycle/immutability/creation-cycle/transaction-rollback/concurrency (transaction rollback evidence only; no down/repeatable/U/R/target-reverse migration) | §13.9 T-S3-08…12, T-S3-28; existing PostgresContainerIT patterns |
| Final-state updates to `assertSchemasAndRoles` / `assertNoV008PlusObjects` / CI expected-set / README **without weakening** staged prefix witnesses | PostgresContainerIT.java |
| Rename `v001ThroughV007MigrateCleanPostgres16WithoutSkipping` → V008 variant | ci.yml + PostgresContainerIT.java |
| CI label strings PA7 V007 → PA8 V008 | ci.yml |

### 2.2 Explicitly out of scope

- V009 `sharing` schema/tables/roles/grants (`evidence_share_grant`, `share_admission`, `share_access_event`) and any `sharing_role` reference in V008 (F-07).
- SQL service functions. Function wave stays **5 rows** (4 V001 + 1 V006 `issue_first_trial`). No `evidence.*` SQL function is created (§10.21 "No S3 function-wave row").
- Application services `evidence.open_recording_upload`, `record_upload_observed`, `complete_recording`, `create_and_solidify`, `read_shareable_projection` (§10.23) — future Java obligations, not this gate.
- Outbox append orchestration / event identity registry implementation (§10.24) — database-level unique keys exist; append is a future service obligation.
- Upload APIs/workers, COS/KMS provisioning, provider/cloud/IVR integration, production wiring.
- Changes to V001–V007 migration bytes.
- Speculative hardening, extra indexes, extra checks, extra grants, extra roles.
- Commit/stage/push/PR/merge. Controller file `docs/gateflow/project-execution-control.md` is read-only (already dirty with an unrelated +14/−1 change — exclude from all side effects).

---

## 3. Frozen V008 inventory (derived, not copied from PA7)

All names/types/checks below are frozen by doc-14 §4.39–4.43. Implementer must match them exactly.

### 3.1 Schema and roles (§11.5 step 1)

```
CREATE ROLE evidence_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE evidence_role  NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE SCHEMA evidence AUTHORIZATION evidence_owner;
GRANT USAGE ON SCHEMA evidence TO evidence_role;   -- without CREATE; evidence_role only
```

Do **not** reference `sharing_owner` / `sharing_role` / `sharing` anywhere in V008.

### 3.2 Table `evidence.media_object` (§4.39) — 11 columns, 9 checks, append-only

| Column | Type | Null | Default | Keys / checks |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_media_object` |
| storage_provider | text | NO | none | `ck_media_object_provider` = `'COS'` |
| bucket | text | NO | none | `ck_media_object_bucket` length 1–255 btrim≠'' |
| object_key | text | NO | none | `ck_media_object_key` length 1–1024 btrim≠''; `uq_media_object_version` |
| object_version_id | text | NO | none | `ck_media_object_version` length 1–1024 btrim≠''; `uq_media_object_version` |
| sha256 | text | NO | none | `ck_media_object_sha256` `~ '^[0-9a-f]{64}$'` |
| size_bytes | bigint | NO | none | `ck_media_object_size` `> 0` |
| mime_type | text | NO | none | `ck_media_object_mime` length 1–255 btrim≠'' |
| crypto_key_ref | text | NO | none | `ck_media_object_key_ref` length 1–128 btrim≠'' |
| worm_status | text | NO | `'PENDING'` | `ck_media_object_worm_status` IN (`PENDING`,`CONFIRMED`,`UNSUPPORTED`,`FAILED`) |
| created_at | timestamptz | NO | `now()` | — |

- UNIQUE `uq_media_object_version (bucket, object_key, object_version_id)`
- INDEX `ix_media_object_sha256 (sha256)`
- Trigger `trg_media_object_immutable` BEFORE UPDATE OR DELETE → `evidence.reject_row_mutation()`

### 3.3 Table `evidence.recording` (§4.40) — 15 columns, 11 checks, mutable head

| Column | Type | Null | Default | Keys / checks |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_recording` |
| call_id | text | NO | none | FK `fk_recording_call` |
| recording_id | text | NO | none | `ck_recording_protocol_id`; UNIQUE `uq_recording_protocol_id` |
| media_object_id | uuid | YES | none | FK `fk_recording_media_object` |
| declared_format | text | NO | none | `ck_recording_declared_format` length 1–64 |
| declared_size | bigint | NO | none | `ck_recording_declared_size` `> 0` |
| declared_duration | integer | NO | none | `ck_recording_declared_duration` `> 0` |
| declared_sha256 | text | NO | none | `ck_recording_declared_sha256` `~ '^[0-9a-f]{64}$'` |
| validated_duration | integer | YES | none | `ck_recording_validated_duration` NULL OR `> 0` |
| validated_sha256 | text | YES | none | `ck_recording_validated_sha256` NULL OR `~ '^[0-9a-f]{64}$'` |
| status | text | NO | `'DECLARED'` | `ck_recording_status`; `ck_recording_state_shape` |
| upload_expires_at | timestamptz | NO | none | `ck_recording_upload_expiry` `> created_at` |
| created_at | timestamptz | NO | `now()` | — |
| validated_at | timestamptz | YES | none | part of `ck_recording_state_shape` |
| revision | integer | NO | `1` | `ck_recording_revision` `>= 1` |

Check texts (exact):

- `ck_recording_protocol_id`: `length(recording_id) BETWEEN 1 AND 128 AND btrim(recording_id) <> ''`
- `ck_recording_declared_format`: `length(declared_format) BETWEEN 1 AND 64 AND btrim(declared_format) <> ''`
- `ck_recording_declared_size`: `declared_size > 0`
- `ck_recording_declared_duration`: `declared_duration > 0`
- `ck_recording_declared_sha256`: `declared_sha256 ~ '^[0-9a-f]{64}$'`
- `ck_recording_validated_duration`: `validated_duration IS NULL OR validated_duration > 0`
- `ck_recording_validated_sha256`: `validated_sha256 IS NULL OR validated_sha256 ~ '^[0-9a-f]{64}$'`
- `ck_recording_status`: `status IN ('DECLARED','UPLOADED','VALIDATED','QUARANTINED')`
- `ck_recording_state_shape`:
  ```
  (status = 'DECLARED' AND media_object_id IS NULL AND validated_duration IS NULL
       AND validated_sha256 IS NULL AND validated_at IS NULL)
  OR (status = 'UPLOADED' AND media_object_id IS NOT NULL AND validated_duration IS NULL
       AND validated_sha256 IS NULL AND validated_at IS NULL)
  OR (status = 'VALIDATED' AND media_object_id IS NOT NULL AND validated_duration IS NOT NULL
       AND validated_sha256 IS NOT NULL AND validated_at IS NOT NULL)
  OR (status = 'QUARANTINED' AND validated_at IS NOT NULL)
  ```
- `ck_recording_upload_expiry`: `upload_expires_at > created_at`
- `ck_recording_revision`: `revision >= 1`

Keys / indexes / FKs:

- PK `pk_recording (id)`
- UNIQUE `uq_recording_protocol_id (recording_id)`
- INDEX `ix_recording_call (call_id, created_at DESC)`
- INDEX `ix_recording_pending (status, upload_expires_at)`
- FK `fk_recording_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`
- FK `fk_recording_media_object (media_object_id) REFERENCES evidence.media_object(id) ON UPDATE RESTRICT ON DELETE RESTRICT`

State machine (§7.4, §4.40): `DECLARED → UPLOADED → VALIDATED|QUARANTINED`; `DECLARED → QUARANTINED` only on expiry/impossibility. `VALIDATED` and `QUARANTINED` terminal. Guard trigger `trg_recording_guard` BEFORE UPDATE.

### 3.4 Table `evidence.evidence_record` (§4.41) — 17 columns, 13 checks, mutable head

| Column | Type | Null | Default | Keys / checks |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_evidence_record` |
| subject_id | uuid | NO | none | FK `fk_evidence_subject` |
| source_type | text | NO | none | `ck_evidence_source_type` = `'CALL_RECORDING'` |
| source_ref_id | uuid | NO | none | `ck_evidence_source_ref` NOT NULL; **NO_FK** (polymorphic) |
| source_event_domain | text | NO | none | `ck_evidence_source_domain` = `'EVIDENCE'`; `uq_evidence_source_event` |
| source_event_id | text | NO | none | `ck_evidence_source_event_id` length 1–128; `uq_evidence_source_event` |
| call_id | text | YES | none | FK `fk_evidence_call` |
| session_id | uuid | YES | none | FK `fk_evidence_session` |
| reservation_id | uuid | YES | none | FK `fk_evidence_reservation` |
| identity_at_capture | jsonb | NO | none | `ck_evidence_capture_shape`; `ck_evidence_capture_subject` |
| consent_version | text | NO | none | `ck_evidence_consent_version` length 1–64 |
| status | text | NO | `'PENDING'` | `ck_evidence_status`; `ck_evidence_state_shape` |
| revision | integer | NO | `1` | `ck_evidence_revision` `>= 1` |
| snapshot | jsonb | NO | none | `ck_evidence_snapshot_shape` |
| snapshot_sha256 | text | NO | none | `ck_evidence_snapshot_sha256` `~ '^[0-9a-f]{64}$'` |
| created_at | timestamptz | NO | `now()` | — |
| solidified_at | timestamptz | YES | none | part of `ck_evidence_state_shape` |

Check texts (exact):

- `ck_evidence_source_type`: `source_type = 'CALL_RECORDING'`
- `ck_evidence_source_ref`: `source_ref_id IS NOT NULL`
- `ck_evidence_source_domain`: `source_event_domain = 'EVIDENCE'`
- `ck_evidence_source_event_id`: `length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> ''`
- `ck_evidence_capture_shape`: `jsonb_typeof(identity_at_capture) = 'object' AND identity_at_capture ? 'subjectId' AND identity_at_capture ? 'capturedAt' AND identity_at_capture ? 'sourceChannel'`
- `ck_evidence_capture_subject`: `identity_at_capture->>'subjectId' = subject_id::text`
- `ck_evidence_consent_version`: `length(consent_version) BETWEEN 1 AND 64 AND btrim(consent_version) <> ''`
- `ck_evidence_status`: `status IN ('PENDING','SOLIDIFIED','QUARANTINED','FAILED')`
- `ck_evidence_state_shape`: `(status = 'PENDING' AND solidified_at IS NULL) OR (status = 'SOLIDIFIED' AND solidified_at IS NOT NULL) OR (status IN ('QUARANTINED','FAILED') AND solidified_at IS NULL)`
- `ck_evidence_revision`: `revision >= 1`
- `ck_evidence_snapshot_shape`: `jsonb_typeof(snapshot) = 'object' AND snapshot ? 'capture' AND snapshot ? 'content' AND jsonb_typeof(snapshot->'source') = 'object' AND snapshot->>'sourceEventDomain' = 'EVIDENCE' AND snapshot->>'sourceEventId' = source_event_id AND (snapshot->'source'->>'callId') IS NOT DISTINCT FROM call_id AND (snapshot->'source'->>'sessionId') IS NOT DISTINCT FROM session_id::text AND (snapshot->'source'->>'subjectId') IS NOT DISTINCT FROM subject_id::text AND (snapshot->'source'->>'reservationId') IS NOT DISTINCT FROM reservation_id::text AND (snapshot->'source'->>'recordingId') IS NOT DISTINCT FROM source_ref_id::text`
- `ck_evidence_snapshot_sha256`: `snapshot_sha256 ~ '^[0-9a-f]{64}$'`
- `ck_evidence_link_shape`: `session_id IS NULL OR call_id IS NOT NULL`

Keys / indexes / FKs:

- PK `pk_evidence_record (id)`
- UNIQUE `uq_evidence_source_event (source_event_domain, source_event_id)`
- INDEX `ix_evidence_subject (subject_id, created_at DESC)`
- INDEX `ix_evidence_call (call_id)`
- INDEX `ix_evidence_status (status, created_at)`
- FK `fk_evidence_subject (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT`
- FK `fk_evidence_call (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT`
- FK `fk_evidence_session (session_id) REFERENCES ivr.inbound_session(id) ON UPDATE RESTRICT ON DELETE RESTRICT`
- FK `fk_evidence_reservation (reservation_id) REFERENCES entitlement_metering.usage_reservation(id) ON UPDATE RESTRICT ON DELETE RESTRICT`
- Deferred FK `fk_evidence_source_event (source_event_domain, source_event_id) REFERENCES evidence.evidence_event(event_domain, event_id) DEFERRABLE INITIALLY DEFERRED` — **ALTER after both tables + `uq_evidence_event_domain_id` exist**; `confupdtype='a'`, `confdeltype='a'` (BD1)

State machine (§7.4): `PENDING → SOLIDIFIED|QUARANTINED|FAILED`; all terminal absorbing. Guard trigger `trg_evidence_record_guard` BEFORE UPDATE. INV-25 names the function `evidence.guard_evidence_record`.

### 3.5 Table `evidence.evidence_artifact` (§4.42) — 5 columns, 1 check, append-only

| Column | Type | Null | Default | Keys / checks |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_evidence_artifact` |
| evidence_id | uuid | NO | none | FK `fk_evidence_artifact_evidence`; `uq_evidence_artifact` |
| media_object_id | uuid | NO | none | FK `fk_evidence_artifact_media`; `uq_evidence_artifact` |
| artifact_role | text | NO | none | `ck_evidence_artifact_role` IN (`ORIGINAL`,`DERIVED`); `uq_evidence_artifact` |
| created_at | timestamptz | NO | `now()` | — |

Keys / indexes / FKs:

- PK `pk_evidence_artifact (id)`
- UNIQUE `uq_evidence_artifact (evidence_id, artifact_role, media_object_id)`
- Partial UNIQUE `uq_evidence_original_artifact (evidence_id) WHERE artifact_role = 'ORIGINAL'`
- INDEX `ix_evidence_artifact_media (media_object_id)`
- FK `fk_evidence_artifact_evidence (evidence_id) REFERENCES evidence.evidence_record(id) ON UPDATE RESTRICT ON DELETE RESTRICT`
- FK `fk_evidence_artifact_media (media_object_id) REFERENCES evidence.media_object(id) ON UPDATE RESTRICT ON DELETE RESTRICT`

Trigger `trg_evidence_artifact_immutable` BEFORE UPDATE OR DELETE. Artifact role rank ORIGINAL=0|DERIVED=1 is a service-layer fact (§2.12); DB check is the named enum only. Partial unique is a named schema control, not an FK target (frozen 24-key candidate catalog unchanged).

### 3.6 Table `evidence.evidence_event` (§4.43) — 8 columns, 5 checks, append-only

| Column | Type | Null | Default | Keys / checks |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_evidence_event` |
| evidence_id | uuid | NO | none | FK `fk_evidence_event_record`; `uq_evidence_created_event` |
| event_domain | text | NO | none | `ck_evidence_event_domain` = `'EVIDENCE'`; `uq_evidence_event_domain_id` |
| event_id | text | NO | none | `ck_evidence_event_id` length 1–128; `uq_evidence_event_domain_id` |
| event_type | text | NO | none | `ck_evidence_event_type` IN (`EVIDENCE_CREATED`,`EVIDENCE_SOLIDIFIED`,`EVIDENCE_QUARANTINED`,`EVIDENCE_FAILED`) |
| occurred_at | timestamptz | NO | `now()` | — |
| payload_redacted | jsonb | NO | none | `ck_evidence_event_payload` `jsonb_typeof = 'object'` |
| payload_sha256 | text | NO | none | `ck_evidence_event_payload_sha256` `~ '^[0-9a-f]{64}$'` |

Keys / indexes / FKs:

- PK `pk_evidence_event (id)`
- UNIQUE `uq_evidence_event_domain_id (event_domain, event_id)` — composite FK target
- Partial UNIQUE `uq_evidence_created_event (evidence_id) WHERE event_type = 'EVIDENCE_CREATED'`
- INDEX `ix_evidence_event_record (evidence_id, occurred_at)`
- Deferred FK `fk_evidence_event_record (evidence_id) REFERENCES evidence.evidence_record(id) DEFERRABLE INITIALLY DEFERRED` — **ALTER after both tables exist**; `confupdtype='a'`, `confdeltype='a'` (BD1)

Trigger `trg_evidence_event_immutable` BEFORE UPDATE OR DELETE.

### 3.7 Reciprocal deferred creation cycle (§4.43, §10.22, INV-25)

Two ordinary deferred constraints (frozen mode class `DEFERRABLE_FK`, not `ALTER_FK`). **BD1 resolved (user-approved 2026-10-30):** ordinary S3 FKs remain `ON UPDATE RESTRICT ON DELETE RESTRICT` (`confupdtype='r'`, `confdeltype='r'`); the two reciprocal V008 creation-cycle FKs retain the §11.5 EXACT DDL with default `NO ACTION` DEFERRABLE INITIALLY DEFERRED (`confupdtype='a'`, `confdeltype='a'`, `condeferrable=true`, `condeferred=true`). This plan reproduces the §11.5 statements verbatim and does **not** add RESTRICT to them. Catalog pin: eight ordinary V008 FKs `r`/`r`; two deferred cycle FKs `a`/`a`. No cascade.

```sql
ALTER TABLE evidence.evidence_record
  ADD CONSTRAINT fk_evidence_source_event
  FOREIGN KEY (source_event_domain, source_event_id)
  REFERENCES evidence.evidence_event(event_domain, event_id)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE evidence.evidence_event
  ADD CONSTRAINT fk_evidence_event_record
  FOREIGN KEY (evidence_id)
  REFERENCES evidence.evidence_record(id)
  DEFERRABLE INITIALLY DEFERRED;
```

Two CONSTRAINT TRIGGERs, installed **after both deferred FKs**:

| Trigger | Timing | Predicate at commit |
| --- | --- | --- |
| `trg_evidence_record_creation_cycle` | `CONSTRAINT TRIGGER AFTER INSERT OR UPDATE OF source_event_domain, source_event_id, status ON evidence.evidence_record DEFERRABLE INITIALLY DEFERRED FOR EACH ROW` | Exactly one `EVIDENCE_CREATED` event for `NEW.id`, with `event_domain = NEW.source_event_domain = 'EVIDENCE'`, `event_id = NEW.source_event_id`, `evidence_id = NEW.id`; else raise `EVIDENCE_CREATION_CYCLE_VIOLATION` |
| `trg_evidence_event_creation_cycle` | `CONSTRAINT TRIGGER AFTER INSERT OR UPDATE OF evidence_id, event_domain, event_id, event_type ON evidence.evidence_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW` | For an `EVIDENCE_CREATED` event: owner record exists, `record.source_event_domain = NEW.event_domain = 'EVIDENCE'`, `record.source_event_id = NEW.event_id`, `NEW.evidence_id = record.id`, exactly one creation event for that record; else raise `EVIDENCE_CREATION_CYCLE_VIOLATION`. Non-creation events skip |

Either row-insertion order (record-first or event-first) is valid until commit. Negative outcomes are **classified**, not uniform (see S2 classification table): FK-satisfied cycle contradictions (decoy non-creation / wrong-owner crosslink / source-mismatch decoy constructions that let both deferred FKs pass) fail at commit with class-level `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` — no named-trigger specificity, no deferred-queue-order reliance, no SQLSTATE union; genuinely absent parents fail at commit with `23503` naming the deferred FK (RI check triggers are queued before `trg_*`); a duplicate `EVIDENCE_CREATED` fails immediately with `23505` on `uq_evidence_created_event`. A failed transaction leaves no record, event, artifact, or Outbox row. Do not promise one trigger error for all cases. **BD2 resolved (user-approved 2026-10-30):** T-S3-09/T-S3-11 wording is amended in doc-14 to the classified contract; use a valid FK-satisfied decoy non-creation event to prove the cycle guard (do not convert the case into missing-ANY-event). No author-executed PostgreSQL runtime verification is claimed in this plan. A **controller-executed** PostgreSQL 16.15 probe (`/tmp/pa8-fk-probe.kKLLwK/result.log`) established that INSERT checks defer under both NO ACTION and RESTRICT, while parent DELETE/UPDATE with RESTRICT is immediate/nondeferrable even on a `DEFERRABLE INITIALLY DEFERRED` constraint and NO ACTION parent-delete defers to COMMIT. No equivalence claim. The deferred action is the approved BD1 choice above (NO ACTION).

### 3.8 Trigger functions (5) and triggers (7)

Function names (implementation decision, following the frozen `reject_row_mutation` / `guard_<object>` naming already used in V001–V007; trigger names are frozen):

| Function | Trigger | Timing |
| --- | --- | --- |
| `evidence.reject_row_mutation()` | `trg_media_object_immutable` | BEFORE UPDATE OR DELETE media_object |
| `evidence.guard_recording()` | `trg_recording_guard` | BEFORE UPDATE recording |
| `evidence.guard_evidence_record()` | `trg_evidence_record_guard` | BEFORE UPDATE evidence_record (INV-25) |
| `evidence.reject_row_mutation()` | `trg_evidence_artifact_immutable` | BEFORE UPDATE OR DELETE evidence_artifact |
| `evidence.reject_row_mutation()` | `trg_evidence_event_immutable` | BEFORE UPDATE OR DELETE evidence_event |
| `evidence.guard_evidence_record_creation_cycle()` | `trg_evidence_record_creation_cycle` | CONSTRAINT, DEFERRABLE INITIALLY DEFERRED |
| `evidence.guard_evidence_event_creation_cycle()` | `trg_evidence_event_creation_cycle` | CONSTRAINT, DEFERRABLE INITIALLY DEFERRED |

All functions: `LANGUAGE plpgsql SECURITY INVOKER SET search_path = evidence, pg_temp`, no transaction control. `REVOKE ALL ... FROM PUBLIC`. `GRANT EXECUTE ... TO evidence_role` only. Not function-wave entries (§10.22).

`reject_row_mutation()` raises `CONSENT_TRANSITION_APPEND_ONLY`-style stable error; use the same stable code pattern as prior waves. For evidence append-only rejection the error text should be stable and table-agnostic (match V003–V007 `reject_row_mutation` body). `guard_recording` / `guard_evidence_record` enforce the immutable-column and terminal-transition lists from §10.22 verbatim.

### 3.9 V008 grant matrix (§10.21, F-07)

**Owned tables** (owner `evidence_owner`; runtime `evidence_role`; `REVOKE ALL FROM PUBLIC`; no DELETE/TRUNCATE):

| Table | SELECT | INSERT | UPDATE columns |
| --- | --- | --- | --- |
| `evidence.media_object` | all | all | none |
| `evidence.recording` | all | all | `media_object_id`, `validated_duration`, `validated_sha256`, `status`, `validated_at`, `revision` |
| `evidence.evidence_record` | all | all | `status`, `solidified_at`, `revision` |
| `evidence.evidence_artifact` | all | all | none |
| `evidence.evidence_event` | all | all | none |

**Cross-module grants** (issued under each parent owner, V007 `SET LOCAL ROLE` pattern):

| Issued as | Grantee | Schema USAGE | Column SELECT |
| --- | --- | --- | --- |
| `subject_number_owner` | `evidence_role` | `subject_number` | `business_subject(id)` |
| `ivr_owner` | `evidence_role` | `ivr` | `call_record(call_id, session_id, subject_id)`; `inbound_session(id, call_id, subject_id)` |
| `entitlement_metering_owner` | `evidence_role` | `entitlement_metering` | `usage_reservation(id, call_id, subject_id)` |
| `platform_reliability_owner` | `evidence_role` | `platform` | EXECUTE `platform.outbox_append` only (no table privilege) |
| `audit_owner` | `evidence_role` | `audit` | EXECUTE `audit.append_fact` only (no table privilege) |

**F-07 hard fence:** zero references to `sharing_role`, `sharing_owner`, or `sharing` schema in V008. Zero grants of evidence tables to any S1/S2 role. `evidence_role` receives no `CREATE`, `DELETE`, `TRUNCATE`, idempotency functions, or platform/audit table privilege.

### 3.10 Inventory totals (do not copy PA7 numbers)

| Class | Count | Detail |
| --- | --- | --- |
| Tables | 5 | media_object, recording, evidence_record, evidence_artifact, evidence_event |
| Columns | 56 | 11+15+17+5+8 |
| Named checks | 39 | 9+11+13+1+5 |
| Primary keys | 5 | one per table |
| Ordinary unique keys | 5 | uq_media_object_version, uq_recording_protocol_id, uq_evidence_source_event, uq_evidence_artifact, uq_evidence_event_domain_id |
| Partial unique keys | 2 | uq_evidence_original_artifact, uq_evidence_created_event |
| Secondary indexes | 8 | ix_media_object_sha256; ix_recording_call, ix_recording_pending; ix_evidence_subject, ix_evidence_call, ix_evidence_status; ix_evidence_artifact_media; ix_evidence_event_record |
| FKs | 10 | 8 ordinary immediate RESTRICT (`confupdtype='r'`, `confdeltype='r'`) + 2 DEFERRABLE INITIALLY DEFERRED (`confupdtype='a'`, `confdeltype='a'`) |
| Default columns | 10 | worm_status; status×2; revision×2; created_at×4 (media/recording/record/artifact); occurred_at (event) |
| Trigger functions | 5 | reject_row_mutation, guard_recording, guard_evidence_record, guard_evidence_record_creation_cycle, guard_evidence_event_creation_cycle |
| Triggers | 7 | 3 immutable + 2 guard + 2 deferred creation-cycle |
| SQL service functions | 0 | V008 adds zero service functions; the frozen function wave stays the 5 named service functions (4 V001 SECURITY DEFINER + `issue_first_trial`) |
| Roles created | 2 | evidence_owner, evidence_role |
| Schemas created | 1 | evidence |

FK mode contribution to the frozen 81-decision catalog (§5.5): V008 adds **10** rows = 8 ordinary + 2 DEFERRABLE_FK. No `ALTER_FK`, no `NO_FK` besides `source_ref_id` which is already counted as NO_FK at the catalog level.

---

## 4. DDL order (§11.5 nine steps — order-sensitive)

Migration file: `backend/src/main/resources/db/migration/V008__evidence_media.sql`. Pattern = V007: create all tables with inline named checks/keys/indexes under `SET LOCAL ROLE evidence_owner` **without any cross-schema FK clause** (no REFERENCES to `subject_number`/`ivr`/`entitlement_metering` at CREATE TABLE time; those parents are owned by other roles and `evidence_owner` holds no REFERENCES on them). Then `RESET ROLE`, then add every FK by `ALTER TABLE ... ADD CONSTRAINT` under the **migration identity** (superuser/migration role, not `evidence_owner`/`evidence_role`) exactly as `V007__activation_and_consent.sql:256-312`. Do not grant REFERENCES to runtime or owner roles.

1. **Schema/roles.** `CREATE ROLE evidence_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;` `CREATE ROLE evidence_role` same attrs; `CREATE SCHEMA evidence AUTHORIZATION evidence_owner;` `GRANT USAGE ON SCHEMA evidence TO evidence_role;` (no CREATE). No `sharing_role` reference.
2. **`evidence.media_object`** under `SET LOCAL ROLE evidence_owner`. Checks/unique/index only; no parent FK.
3. **`evidence.recording`** after media_object. Columns + checks + `uq_recording_protocol_id` + indexes only. **No FK clauses here** (`fk_recording_call`, `fk_recording_media_object` are added later by ALTER).
4. **`evidence.evidence_record`** columns + checks + `uq_evidence_source_event` + indexes only. **No FK clauses here** (`fk_evidence_subject`, `fk_evidence_call`, `fk_evidence_session`, `fk_evidence_reservation` and the composite source-event FK are all later ALTERs).
5. **`evidence.evidence_artifact`** after evidence_record + media_object. Columns + checks + `uq_evidence_artifact` + partial `uq_evidence_original_artifact` + index only. **No FK clauses here.**
6. **`evidence.evidence_event`** columns + checks + `uq_evidence_event_domain_id` + partial `uq_evidence_created_event` + index only. **No FK clauses here.**
7. **`RESET ROLE`** (back to migration identity). Add the **8 ordinary FKs** by `ALTER TABLE ... ADD CONSTRAINT ... ON UPDATE RESTRICT ON DELETE RESTRICT` in the frozen order (recording→call, recording→media_object, evidence→subject, evidence→call, evidence→session, evidence→reservation, artifact→evidence, artifact→media_object). Then add the **2 deferred cycle FKs** (statements exactly as §3.7) after both tables and `uq_evidence_event_domain_id` exist. No REFERENCES grant is issued to any role.
8. **Functions + BEFORE triggers** (reject_row_mutation, guard_recording, guard_evidence_record + their BEFORE triggers) under `SET LOCAL ROLE evidence_owner`. Then the two `CONSTRAINT TRIGGER ... DEFERRABLE INITIALLY DEFERRED` creation-cycle triggers, after both deferred FKs. `GRANT EXECUTE` on each trigger function only to `evidence_role`.
9. **§10.21 grants.** Owned-table grants as `evidence_owner`. Cross-module grants under `SET LOCAL ROLE subject_number_owner` / `ivr_owner` / `entitlement_metering_owner` / `platform_reliability_owner` / `audit_owner` (V007 pattern). No function created. No S1/S2 table altered.

**No rollback migration.** The repository is forward-only (V001–V007 versioned migrations only; no U/R/down files). Rollback is proven only by transactional forced-failure tests (T-S3-12 and the recording-only rollback probe): a valid write set is inserted in one transaction, a failure is forced before COMMIT, and zero rows survive. A test-local cleanup that never changes the migration set is allowed. Do not create `R__`/`U008`/`R008`/`target()`-reverse artifacts.

**Order-sensitive correctness cases the tests must prove:**

| Case | Expected |
| --- | --- |
| Record then event (same tx) | Commits |
| Event then record (same tx) | Commits |
| Record + decoy non-creation event sharing `(event_domain, event_id)`, COMMIT | `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` (FK-satisfied cycle class); no rows survive |
| Record with genuinely absent event parent, COMMIT | `23503` naming `fk_evidence_source_event` (not the cycle error) |
| Event with genuinely absent record parent, COMMIT | `23503` naming `fk_evidence_event_record` |
| Two `EVIDENCE_CREATED` for one evidence_id | Immediate `23505` on `uq_evidence_created_event` |
| Wrong `event_type` with FK-satisfying decoy | `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` |
| Event `evidence_id` ≠ record `id`, both parents present | `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` (isolate so the named parent is present; do not accept a SQLSTATE union) |
| Record `source_event_*` ≠ matching creation-event identity | FK-satisfied source-mismatch decoy: R.source=(EVIDENCE,X); noncreation decoy D.event=(EVIDENCE,X), D.evidence_id=R; creation event C.event=(EVIDENCE,Y), C.evidence_id=R; X≠Y; distinct PKs; both deferred FKs satisfied. Events D,C then record R (committed valid parent prerequisites first). → class-level `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` only (no named-trigger specificity, no queue-order reliance, no SQLSTATE union). Either pair insertion order still succeeds for a matching pair (T-S3-08). |
| Failure after record/event/artifact/Outbox write, before COMMIT | Full tx rollback; no survivor (T-S3-12) |
| Missing parent FK insert (recording→call, evidence→subject, artifact→evidence, etc.) | `23503` |
| Parent delete with RESTRICT child (ordinary FKs) | `23503` (immediate). Unprotected parents (`business_subject`, `call_record`, `inbound_session`, `usage_reservation`, `evidence_record`) delete directly. Protected `media_object` parent: savepoint-scoped `DISABLE TRIGGER trg_media_object_immutable` solely to reach the named FK, then `23503`; rollback and assert `tgenabled='O'`. Do not weaken constraints. |
| Parent delete involving deferred event FK (BD1 = NO ACTION) | Committed valid `evidence_record`+`EVIDENCE_CREATED` pair, **no artifact children**; new transaction `DELETE evidence_record` (guard is UPDATE-only) succeeds at `executeUpdate`; `expectCommitFailure(..., "23503", "fk_evidence_event_record")` at COMMIT (controller probe `/tmp/pa8-fk-probe.kKLLwK/result.log`). Snapshot starting rows; the pair **survives** after the failed commit (observe survival first). Then finally clean that method-local pair with the shared method-local cleanup helper — never conflate observation with residue. No pending INSERT trigger can preempt the target in that transaction. Do not use `expectSqlFailure`. |
| Update/delete on media_object/artifact/event | `reject_row_mutation` raise |
| Terminal status rewrite on recording/evidence_record | guard raise |
| Immutable column rewrite on recording/evidence_record | guard raise |

---

## 5. Database contracts vs future application services

| Database contract (this gate) | Future application service (NOT this gate) |
| --- | --- |
| Tables, checks, keys, indexes, FKs, defaults | `evidence.open_recording_upload` (§10.23) |
| Guard/append-only/creation-cycle triggers | `evidence.record_upload_observed` |
| Role/column/schema ACLs | `evidence.complete_recording` (T-S3-01) |
| Unique keys usable as service dedupe witnesses | `evidence.create_and_solidify` (T-S3-02) |
| Deferred FK + cycle trigger commit enforcement | `evidence.read_shareable_projection` |
| Outbox unique-key shape exists in platform.outbox_event | Outbox append orchestration (§10.24 identities) |

§10.24 event/Outbox identity registry (F-10) is a service obligation. The DB only provides the unique keys (`uq_evidence_source_event`, `uq_evidence_event_domain_id`, `uq_evidence_created_event`, recording-conflict dedupe shape). No V008 test may require an Outbox row to be created by a SQL function.

---

## 6. Allowed files (exactly these; anything else is a gate violation)

| File | Change |
| --- | --- |
| `backend/src/main/resources/db/migration/V008__evidence_media.sql` | **create** (only new migration file; no down/repeatable/U/R file) |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edit: staged-V007 prefix witnesses, final-state updates, 6 new `@Test` methods, migration-method rename, helper methods (`expectCommitFailure` introduced in S1 where first used; S2 reuses) |
| `.github/workflows/ci.yml` | edit: expected-set + PA8 V008 labels |
| `backend/README.md` | edit: 51→57 method boundary, V008 inventory prose, CI description |
| `docs/gateflow/pa8-v008-evidence-foundation-plan.md` | this file (already written; implementation must not rewrite it except to record acceptance) |

**Forbidden:** `docs/gateflow/project-execution-control.md` (dirty, unrelated), V001–V007 SQL, `docs/12-*`, `docs/14-*`, any `src/main/java/**`, any sharing/V009 file, any new SQL function, any credential/auth file.

### 6.1 V001–V007 SHA-256 freeze (must remain byte-identical)

| File | SHA-256 |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `V004__catalog_order_and_payment.sql` | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| `V005__ivr_task_session_call.sql` | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| `V007__activation_and_consent.sql` | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |

S1 must add a file-digest witness (or equivalent) asserting these seven digests are unchanged after V008 lands.

---

## 7. Test baseline and target

### 7.1 Verified baseline

| Source | Count | Notes |
| --- | --- | --- |
| `@Test` methods in `PostgresContainerIT` | **51** | extracted from source; matches CI expected-set exactly |
| Default surefire (`mvn test`) | 10 | unit/module tests in `target/surefire-reports/TEST-*.xml`; unchanged by PA8 — never assert 54/57 here |
| CI container path | `-Pcontainer-smoke verify` | java-runtime job; Python asserts exact 51 names from **Failsafe** `target/failsafe-reports/TEST-*PostgresContainerIT.xml`, skipped=0/failures=0/errors=0 |
| CI labels still say | `PA7 V007 S2 test count (51)` | must become PA8 V008 |

### 7.2 Target after PA8

| Item | Value |
| --- | --- |
| Container `@Test` methods | **57** (51 + 6 new) |
| Renamed | `v001ThroughV007MigrateCleanPostgres16WithoutSkipping` → `v001ThroughV008MigrateCleanPostgres16WithoutSkipping` |
| CI expected-set | add 6 names, apply rename, change count label to 57 / PA8 V008 |
| Function-wave count asserted | still the **5** named service functions (not total `pg_proc`) |
| Schema count asserted | **11** (10 + `evidence`); `sharing` still absent |
| Role count asserted | +2 (`evidence_owner`, `evidence_role`); `sharing_owner`/`sharing_role` still absent |

### 7.3 New test method names (exact)

S1 (slice 1):

1. `v008EvidenceInventoryAndGuardShapesAreExact`
2. `v008OwnerPublicRuntimeAndCrossModuleAclsAreExact`
3. `v008ForeignKeyAndDeferredCreationCycleFkBackstops`

S2 (slice 2):

4. `v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops`
5. `v008CreationCycleBothOrdersAndTerminalLifecycleBackstops`
6. `concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner`

These follow the V007 naming convention (`v007*` × 5 + `concurrentV007*`).

---

## 8. Final-state updates without weakening prefix witnesses

### 8.1 Existing assertions that must change (final state)

Every row below is on the full V008 connection and is a guaranteed red if not updated. Do not delete or weaken entries; extend the exact lists.

| Location | Current | After V008 |
| --- | --- | --- |
| `assertSchemasAndRoles` (PostgresContainerIT ~5527–5587) | 10 schemas, `pg_namespace IN ('evidence','sharing')` count=0, roles = S1+V004 exactly, four S3 roles count=0 | 11 schemas including `evidence=evidence_owner`; `sharing` still absent from the non-pg list and `pg_namespace IN ('sharing')` count=0; roles = S1+V004+`V008_ROLES` (`evidence_owner`,`evidence_role`; NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE); `sharing_owner`/`sharing_role` count=0. **Define a separate `V008_ROLES` constant.** Do **not** append evidence roles to `S1_ROLES`/`V004_ROLES` — numerous prior ACL/schema matrices iterate those shared constants and would then fail on evidence_role's V008 grants. |
| `assertFunctionsAndTrigger` (PostgresContainerIT ~6293–6463) | exact list of all 35 non-system functions (6298–6333) and all 33 non-internal triggers (6430–6463) | Append the 5 `evidence.*` functions (`evidence.guard_evidence_event_creation_cycle`, `evidence.guard_evidence_record`, `evidence.guard_evidence_record_creation_cycle`, `evidence.guard_recording`, `evidence.reject_row_mutation`) to the all-schema function list (total 40). Append the 7 `evidence.*` triggers (`trg_media_object_immutable`, `trg_recording_guard`, `trg_evidence_record_guard`, `trg_evidence_artifact_immutable`, `trg_evidence_event_immutable`, `trg_evidence_record_creation_cycle`, `trg_evidence_event_creation_cycle`) to the all-schema trigger list (total 40). Keep existing per-schema signature/owner/SECURITY-INVOKER lists unchanged; add a parallel evidence block. |
| `assertRiskOpenClearReopenRevisionProtocol` (PostgresContainerIT ~17536–17539) | `count(*) ... nspname IN ('evidence','sharing')` is zero | Change this full-connection V003-method assertion to `nspname IN ('sharing')` count=0 only. Keep the historical evidence-absent intent in the staged-V007 capture (`stagedV007EvidenceSchemaAbsent`). V008 creates `evidence`, so the combined query is a guaranteed red. **Single choice — no alternative.** |
| Migration-method history block (PostgresContainerIT ~1184–1252) | rows 001–007 then `resultSet.next()` is false | Add the V008 row (`version 008`, description `evidence media`, type `SQL`, script `V008__evidence_media.sql`, `success` true) and then `resultSet.next()` is false. Add the staged-V007 `MigrateResult`/`migrationsExecuted=1` assertion to the renamed migration method. |
| `assertNoV008PlusObjects` (~7688–7748) | evidence/sharing schemas absent; four S3 roles absent; all 8 V008+ table names absent in evidence/sharing | Split semantics: on the **full** connection this becomes `assertNoV009PlusObjects`-equivalent — evidence present with exactly `V008_TABLES` (5 names); sharing schema and 3 sharing tables still absent; `evidence_owner`/`evidence_role` present; `sharing_owner`/`sharing_role` absent; V008 table names still absent from `activation_consent`/`entitlement_metering`; function-wave count still 5 (the frozen five service functions; see §8.1 function-wave note); `fk_customer_order_consent` still present; no speculative service-function names |
| Callers `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact`, `v007ActivationConsentInventoryAndGuardShapesAreExact` | call `assertNoV008PlusObjects(fullConnection)` | keep the forward-reference intent by asserting against staged-V007 prefix booleans **and** the new final-state helper; do not silently delete the V006/V007 witnesses |

**Function-wave note (PR-006):** "function wave stays 5" means the frozen five *service function wave* rows (the four V001 SECURITY DEFINER functions `platform.idempotency_begin`, `platform.idempotency_complete`, `platform.outbox_append`, `audit.append_fact` plus `entitlement_metering.issue_first_trial`), **not** a total `pg_proc` count. After V008 the database has 40 user-defined functions (35 + 5 evidence trigger functions). Assert: (a) the five named service functions by name/signature with `prosecdef=true`; (b) the count of SECURITY DEFINER user functions is exactly 5; (c) V008 adds only the five expected SECURITY INVOKER trigger functions and zero new SECURITY DEFINER / service-wave rows. Do **not** assert total `pg_proc` = 5.

### 8.2 Prefix witnesses (must be preserved / extended)

Existing staged booleans captured in `@BeforeAll` stay as-is and remain asserted by their wave methods:

- `stagedV004ActivationConsentTablesAbsent`, `stagedV004OrderConsentFkAbsent`
- `stagedV005IssueFirstTrialAbsent`, `stagedV005LateFkAbsent`, `stagedV005EntitlementTablesAbsent`, `stagedV005ReservationIdNullable`
- `stagedV006EntitlementTablesExact`, `stagedV006ActivationConsentTablesAbsent`, `stagedV006NoV007Objects`, `stagedV006SubjectUsageAbsent`, `stagedV006CustomerIamUsageAbsent`, `stagedV006OrganizationAccessUsageAbsent`

**New staged-V007 prefix capture** (Flyway `target(007)` before full migration), immutable booleans:

- `stagedV007EvidenceSchemaAbsent`
- `stagedV007EvidenceRolesAbsent`
- `stagedV007EvidenceTablesAbsent` (all 5 names)
- `stagedV007SharingSchemaAbsent` / `stagedV007SharingRolesAbsent` (still true after V008 — keep asserting)
- `stagedV007FunctionWaveStillFive`

`@BeforeAll` order becomes: `target(004)` → probes → `target(005)` → `target(006)` → `target(007)` → capture V008-absence → full migration (V008).

Do not weaken any existing prefix assertion. The V006/V007 "no later-wave objects" intent is preserved by the new staged-V007 booleans plus the final-state "no V009" fence.

---

## 9. Slices (2)

Two independently verifiable slices, matching the accepted PA7 shape (S1 catalog/ACL/FK, S2 lifecycle/immutability/concurrency). A third slice is unnecessary: V008 has no late ALTER_FK of the V007 `fk_customer_order_consent` kind (the two deferred cycle FKs belong to the same wave and are covered in S1/S2), and no function/service wave.

### Slice 1 — Migration + catalog + ACL + FK/staged-prefix (3 methods)

**Allowed files:** `V008__evidence_media.sql` only (no rollback file), `PostgresContainerIT.java`, `ci.yml`, `backend/README.md`.

**Prerequisites:** none (base is merged main).

**Implementation decisions:**

- Create `V008__evidence_media.sql` following §11.5 nine-step order and the V007 `SET LOCAL ROLE`/`RESET ROLE` pattern. Exact inventory from §3.
- Pin function names exactly as §3.8.
- Add staged-V007 `@BeforeAll` captures (§8.2).
- Update `assertSchemasAndRoles` and the V008-absence helper per §8.1.
- Rename migration test method to `v001ThroughV008MigrateCleanPostgres16WithoutSkipping`.
- Add 3 S1 `@Test` methods:

  **`v008EvidenceInventoryAndGuardShapesAreExact`**
  - Assert 5 tables in `evidence`, exact 56 columns in order, 39 named checks with exact names+definitions, 5 PKs, 5 ordinary + 2 partial uniques, 8 secondary indexes, 10 defaults, 5 trigger functions with `SECURITY INVOKER` + `SET search_path = evidence, pg_temp`, 7 triggers with exact timing/operation, function-wave count still 5, no `sharing` object, V001–V007 file digests match §6.1.
  - Pattern: reuse `queryStrings`/`queryLong`/`toRegclass` catalog helpers from V004–V007 inventory methods.

  **`v008OwnerPublicRuntimeAndCrossModuleAclsAreExact`**
  - Assert `evidence_owner`/`evidence_role` attrs (NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE).
  - `REVOKE ALL FROM PUBLIC` on all 5 tables + 5 functions, proven via the PA7 `aclexplode`/`grantee = 0` expansion pattern (PostgresContainerIT ~8647–8675). **Do not** use `has_schema_privilege('public', ...)`: PUBLIC is ACL pseudo-grantee OID 0, not a login. Expand `coalesce(n.nspacl, acldefault('n', n.nspowner))`, `coalesce(c.relacl, acldefault('r', c.relowner))`, `coalesce(a.attacl, acldefault('c', c.relowner))`, `coalesce(p.proacl, acldefault('f', p.proowner))` and assert zero PUBLIC rows on the `evidence` schema, the five tables, every column and the five functions.
  - `evidence_role` exact table privileges: SELECT+INSERT on all 5; UPDATE column list exactly as §3.9 (recording 6 cols, evidence_record 3 cols); no DELETE/TRUNCATE; append-only tables have no UPDATE. Assert `has_table_privilege(...,'UPDATE')` is false even where column UPDATE exists.
  - Schema fence: `evidence_role` has `USAGE` but not `CREATE` on `evidence`; every S1/S2 role (the full `S1_ROLES` + `V004_ROLES` lists) has **no** `USAGE` and no `CREATE` on `evidence`.
  - Function fence: every S1/S2 role has **no** `EXECUTE` on any of the five `evidence.*` trigger functions. `evidence_role` has EXECUTE on exactly those five and on `platform.outbox_append`/`audit.append_fact`, and **no** EXECUTE on `platform.idempotency_begin`/`platform.idempotency_complete`.
  - Cross-module: USAGE without CREATE on `platform`,`audit`,`subject_number`,`ivr`,`entitlement_metering`; EXECUTE only `outbox_append`/`append_fact`; column SELECT exactly as §3.9. Exhaustive per-table/column negatives: no INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER on any foreign parent table; no column privilege outside the declared SELECT lists.
  - **Foreign row-lock negatives (required by §9.0.2/§10.21):** as `evidence_role`, `SELECT <granted columns> FROM subject_number.business_subject ... FOR SHARE` and `... FOR UPDATE` must fail `42501`; same for `ivr.call_record`, `ivr.inbound_session`, `entitlement_metering.usage_reservation`. These prove the design's "no foreign row-lock privilege" fence is executable, not just a catalog allow-list.
  - Additional negatives: `evidence_role` denied DELETE/TRUNCATE/UPDATE-of-immutable-column on owned tables, denied platform/audit table DML, denied `sharing` (N/A — schema absent). `sharing_owner`/`sharing_role` still absent. No S1/S2 role has any evidence table/column privilege. Keep the existing platform/audit denials.
  - Pattern: reuse `expectSqlFailure` + role-switch helpers from `v007OwnerPublicRuntimeAndCrossModuleAclsAreExact`, and the PA7 `aclexplode` PUBLIC helper and `FOR SHARE`/`FOR UPDATE` 42501 probes (PostgresContainerIT ~2664–2741, ~8647–8675).

  **`v008ForeignKeyAndDeferredCreationCycleFkBackstops`**
  - Assert all 10 FKs exist with exact names, columns. Catalog pin (BD1): the 8 ordinary FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT` (`confupdtype='r'`, `confdeltype='r'`); the 2 deferred cycle FKs are `DEFERRABLE INITIALLY DEFERRED` with `confupdtype='a'`, `confdeltype='a'` (§11.5 exact DDL, default NO ACTION), `condeferrable=true`, `condeferred=true`. Assert all four catalog columns for all 10 FKs.
  - `23503` missing-parent probes for every ordinary FK (statement-time, `expectSqlFailure`). Named child isolation per FK so an unrelated FK cannot win.
  - RESTRICT parent-delete probes with **pinned parent/child fixtures** (one named child per probe):
    | FK | Parent (delete) | Only child present | Mechanism |
    | --- | --- | --- | --- |
    | `fk_recording_call` | `ivr.call_record` | one `recording` | direct DELETE → `23503` |
    | `fk_recording_media_object` | `evidence.media_object` | one `recording` (no artifact) | savepoint-scoped `DISABLE TRIGGER trg_media_object_immutable` solely to reach FK → `23503`; rollback; assert `tgenabled='O'` |
    | `fk_evidence_subject` | `subject_number.business_subject` | one `evidence_record` | direct DELETE → `23503` |
    | `fk_evidence_call` | `ivr.call_record` | one `evidence_record` | direct DELETE → `23503` |
    | `fk_evidence_session` | `ivr.inbound_session` | one `evidence_record` | direct DELETE → `23503` |
    | `fk_evidence_reservation` | `entitlement_metering.usage_reservation` | one `evidence_record` | direct DELETE → `23503` |
    | `fk_evidence_artifact_evidence` | `evidence.evidence_record` | one artifact (no event) | direct DELETE → `23503` (guard is UPDATE-only) |
    | `fk_evidence_artifact_media` | `evidence.media_object` | one artifact (no recording) | savepoint-scoped `DISABLE TRIGGER trg_media_object_immutable` solely to reach FK → `23503`; rollback; assert `tgenabled='O'` |
  - Protected-parent rule: never claim a literal DELETE of `media_object`/`evidence_event` reaches the FK while `trg_*_immutable` is enabled (that path raises `P0001`). The savepoint-scoped DISABLE TRIGGER is migration-test-identity/`evidence_owner`-scoped, used **solely** to reach the FK, then rolled back with full `pg_trigger.tgenabled` + constraint-catalog restoration asserted. Do not weaken or drop any named constraint.
  - Deferred parent-delete (`fk_evidence_event_record`): commit a valid `evidence_record` + matching `EVIDENCE_CREATED` pair with **no artifact children**; snapshot starting row counts. In a **new** transaction `DELETE FROM evidence.evidence_record WHERE id = ?` (guard is UPDATE-only, so DELETE is not trigger-blocked). `executeUpdate` succeeds; `expectCommitFailure(connection, "23503", "fk_evidence_event_record", ...)`. After the failed commit, assert the snapshot pair still exists (rollback restores the pair — do **not** claim the pair vanishes). **Observe survival first, then** finally clean that method-local pair with the shared method-local cleanup helper — never conflate the survival observation with leftover residue. No pending INSERT creation-cycle trigger can preempt the target in that delete-only transaction. Do **not** use `evidence_event` as the deleted parent.
  - Deferred cycle catalog: both FKs present; `uq_evidence_event_domain_id` is the composite target; `fk_customer_order_consent` (V007 late FK) still present; staged-V007 absence booleans still true; final-state evidence-present/sharing-absent holds.
  - **`expectCommitFailure` is introduced here (S1)** because this method first needs it. Signature: `expectCommitFailure(connection, sqlstate, messageFragment, setup)` — dedicated transaction, run setup, `COMMIT`, assert thrown `SQLException` SQLSTATE/message, prove zero survivors where applicable. S2 **reuses** it; do not re-add.
  - Pattern: reuse `expectSqlFailure(connection, "23503", ...)` and staged boolean assertions from `v007ForeignKeyAndLateOrderConsentFkBackstops`; V007 representative parent-delete shape at `PostgresContainerIT.java:8253-8277`; V007 named-trigger restore pattern at `:10068-10123`.

**Tests/commands:**

```bash
export GIT_DIR=... GIT_WORK_TREE=...   # Windows gitdir pointer
export JAVA_HOME=/opt/jdk21
export PATH=/opt/maven-3.9.9/bin:$JAVA_HOME/bin:$PATH
mvn -Pcontainer-smoke verify
```

**Assertions (slice 1 complete iff):**

- Flyway applies V001–V008 cleanly (renamed migration test green).
- V001–V007 SHA-256 digests unchanged.
- Catalog = §3.10 totals exactly.
- ACL = §3.9 exactly; every negative probe hits the expected SQLSTATE/exception.
- FK = 10 with correct modes; missing-parent and RESTRICT probes green.
- Staged V004/V005/V006/V007 prefix witnesses all true; final state = evidence present / sharing absent.
- Container report = 54 methods (51 + 3), 0 skipped/failures/errors, from **Failsafe** XML `target/failsafe-reports/TEST-*PostgresContainerIT.xml` (not Surefire). Default Surefire stays 10 unit/module tests, unchanged. **Mandated sequencing:** S1 updates source, CI expected-set (54 names including the rename) and README together; S2 updates them to 57 names and the PA8 V008 label. Each accepted slice must pass exact source=CI=Failsafe-XML equality and zero skipped/failures/errors. There is **no** alternative that defers the CI expected-set to slice 2 — that alternative cannot keep S1 green.

**Stop conditions (slice 1):**

- Any inventory count mismatch with §3.10.
- Any grant that references `sharing_role`/`sharing_owner`/`sharing`.
- Any SQL service function created.
- Any V001–V007 byte change.
- Material design contradiction (stop and report; do not amend frozen design).

### Slice 2 — Lifecycle / immutability / deferred-cycle / rollback / concurrency (3 methods)

**Allowed files:** `PostgresContainerIT.java`, `ci.yml`, `backend/README.md`. (Migration file frozen after slice 1 acceptance unless a defect is found; a defect forces a stop-and-report, not a silent amend.)

**Prerequisites:** slice 1 accepted; catalog/ACL/FK green.

**Implementation decisions:**

- Add 3 S2 `@Test` methods. **`expectCommitFailure` is REUSED from S1** (introduced in `v008ForeignKeyAndDeferredCreationCycleFkBackstops` for the deferred parent-delete). Do **not** add it again in S2. Do not reuse `expectSqlFailure` (`PostgresContainerIT.java:3626-3651`, savepoint + `executeUpdate` only) for commit-time checks. Do not use `expectCommitFailure` for RESTRICT parent actions (those are immediate). Do not claim author-executed PostgreSQL runtime verification in this plan; every SQLSTATE below is an implementer assertion to be confirmed on PostgreSQL 16 at implementation. Exception: the RESTRICT-vs-NO-ACTION parent-action timing facts are **controller-executed** (PostgreSQL 16.15, fix-1 §3.1.2) and are not author-verified.

- **Negative-case classification (BD2 resolved; do not promise one trigger error for all cases). Each row pins one construction — no vague union of errors):**

  | Class | Construction (exact) | Expected |
  | --- | --- | --- |
  | FK-satisfied cycle contradiction (T-S3-09, noncreation-decoy) | Insert record + a **decoy non-creation event** with the same `(event_domain='EVIDENCE', event_id=NEW.source_event_id)`, distinct PK, non-`EVIDENCE_CREATED` `event_type`, so the composite deferred FK passes and the record-side creation-cycle trigger is the only remaining check | COMMIT fails with `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` from `trg_evidence_record_creation_cycle`. This is the construction that proves T-S3-09's "no matching `EVIDENCE_CREATED`" **without changing DB behavior** and without converting the case into missing-ANY-event. |
  | Genuinely absent parent | Insert record with `source_event_*` pointing at no `evidence_event` row at all | Deferred FK `fk_evidence_source_event` fires first (RI trigger name `RI_ConstraintTrigger_*` sorts before `trg_*`); COMMIT fails `23503` naming `fk_evidence_source_event`. Assert `23503` + exact constraint name. This is **not** `EVIDENCE_CREATION_CYCLE_VIOLATION`. |
  | Genuinely absent record (event without record) | Insert `evidence_event` with `evidence_id` pointing at no record | Deferred FK `fk_evidence_event_record` fires first; COMMIT fails `23503` naming `fk_evidence_event_record`. |
  | Duplicate creation event (T-S3-10) | Second `EVIDENCE_CREATED` for one `evidence_id` with **distinct `event_id` and distinct PK** so only the partial unique can fail | Immediate `23505` on `uq_evidence_created_event`, not `uq_evidence_event_domain_id`, not a commit-time cycle error. |
  | Wrong-owner (T-S3-11, two-record/two-event crosslink) | Record A `source_event_*` = `(EVIDENCE, evt-A)` + Record B `source_event_*` = `(EVIDENCE, evt-B)`; Event A is `EVIDENCE_CREATED` with `event_id=evt-A` but `evidence_id=B`; Event B is `EVIDENCE_CREATED` with `event_id=evt-B` but `evidence_id=A`. Distinct PKs and distinct `event_id`s. Both deferred FKs satisfied. Committed valid parent prerequisites (subject/call as required) first. Pinned insertion order: events first, then records, then COMMIT. | `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` at COMMIT from the creation-cycle triggers. Class-level contract only — do **not** claim per-trigger isolation (the owner record's own cycle may also fail under the single stable error code). No nonexistent parent, no shared unique source. |
  | Mismatched identity / source-mismatch (T-S3-11) | FK-satisfied decoy, three rows: record R.source=(EVIDENCE,X); noncreation decoy D.event=(EVIDENCE,X), D.evidence_id=R; creation event C.event=(EVIDENCE,Y), C.evidence_id=R; X≠Y; distinct PKs. Both deferred FKs satisfied (record-side source FK via D; `fk_evidence_event_record` via both). Committed valid parent prerequisites first; insert events D,C then record R. | Class-level `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` at COMMIT only. No named-trigger specificity; no deferred-queue-order reliance; no SQLSTATE union. Do **not** accept `{23503, EVIDENCE_CREATION_CYCLE_VIOLATION}`. Matching-pair orders still commit (T-S3-08). |
  | Parent delete involving deferred-NO-ACTION event FK (BD1 approved) | Committed valid record+creation-event pair, no artifact children; new tx delete `evidence_record` (see S1 fixture) | `executeUpdate` succeeds; failure is at COMMIT. Use `expectCommitFailure` (S1 helper), not `expectSqlFailure`. Pair survives. |

  BD2 is resolved: the classified table above is the executable contract. T-S3-09/T-S3-11 in doc-14 are amended to match. Do not weaken assertions or disable a named constraint outside a savepoint that restores it.

- Add 3 S2 `@Test` methods:

  **`v008MediaRecordingShapeDefaultsAppendOnlyAndRollbackBackstops`**
  - **Role pin:** owner/`evidence_owner` (or migration identity) executes every guard/append-only probe that must raise `P0001`. Never run an immutable-trigger proof as `evidence_role` — that is `42501` and stays in S1's ACL method.
  - **Recording prerequisite:** every `recording` insert first commits a valid `ivr.call_record` parent (and a valid `media_object` when `media_object_id` is set). No recording fixture may insert without a valid call parent.
  - Named-check negative probes for every check on `media_object` and `recording` (savepoint-isolated, `expectSqlFailure(..., "23514", checkName, ...)`), including `ck_recording_state_shape` for all four states. For `ck_recording_status` (enum vs shape overlap): savepoint-drop preemptor `ck_recording_state_shape` using the V007 `expectV007CheckWithIsolatedPreempting` pattern (`PostgresContainerIT.java:8889-8924`), probe `ck_recording_status`, rollback, assert full catalog restoration. Full mapping: see the 39-check table below.
  - Default-column witnesses: `worm_status='PENDING'`, `status='DECLARED'`, `revision=1`, `created_at` `now()`. (`occurred_at` default belongs to the event-bearing method.)
  - Append-only: UPDATE/DELETE on `media_object` raises `reject_row_mutation` as owner (`P0001`).
  - Guard: immutable-column rewrite on `recording` rejected as owner (`P0001`); terminal `VALIDATED`/`QUARANTINED` → other status rejected; clearing set `media_object_id` rejected.
  - **Recording-only rollback (not T-S3-12):** in one tx insert valid `call_record` parent + `media_object` + `recording` (all successful), **observe inserted counts > 0**, then force a **distinct sentinel failure** (a separate invalid statement after the valid writes — do not reuse a missing-parent error as the only failure), then zero survivors. This fixture does **not** insert artifact/event/Outbox (those require an `evidence_record` via `fk_evidence_artifact_evidence`/`fk_evidence_event_record` and the creation cycle).
  - Recording state machine edges that the DB can see: DECLARED→UPLOADED (set media_object_id+status), UPLOADED→VALIDATED (set validation cols), DECLARED→QUARANTINED (set validated_at), invalid edges rejected.

  **`v008CreationCycleBothOrdersAndTerminalLifecycleBackstops`**
  - **Role pin:** owner/`evidence_owner` (or migration identity) for every `P0001` guard/append-only proof. Runtime `42501` denials remain only in S1 ACL.
  - Valid fixture prerequisites for every cycle/lifecycle probe: committed parent rows for `subject_number.business_subject`, a valid `ivr.call_record` whenever `call_id`/`recording` is used, optional `ivr.inbound_session`/`entitlement_metering.usage_reservation`, one `media_object`, then the record/event pair in the tested insertion order, then at most one `ORIGINAL` artifact. A fixture that inserts artifact/event without an `evidence_record` is invalid (FK + cycle reject it).
  - T-S3-08: record-first and event-first insertion orders both commit in one tx; exactly one `EVIDENCE_CREATED` + one `ORIGINAL` artifact. After assertions, clean method-local committed pairs/artifacts with the shared method-local cleanup helper.
  - T-S3-09 (noncreation-decoy construction, pinned above): COMMIT fails `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`; no survivors. Genuinely-absent-parent class → `23503` via `expectCommitFailure` (see classification table). Do not collapse the two classes into one expected error.
  - T-S3-10: two `EVIDENCE_CREATED` for one evidence_id with **distinct `event_id` and distinct PKs** → immediate `23505` on `uq_evidence_created_event` only.
  - T-S3-11 wrong-owner: **two-record/two-event crosslink** (pinned above, unchanged); class-level `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`; no per-trigger isolation claim. Mismatched source identity: FK-satisfied decoy construction R/D/C as in the classification table (not a two-row event-first fixture). Isolate target violations; do not accept a SQLSTATE union.
  - T-S3-12 (the **full** valid evidence/artifact/Outbox rollback): insert record + creation event + `ORIGINAL` artifact + a simulated Outbox row in one tx (all FK/cycle-satisfying), **observe inserted counts > 0**, force a **distinct sentinel failure** after those successful writes, then zero survivors. This is the only T-S3-12 witness.
  - Evidence-record guard as owner: immutable captured columns rejected (`P0001`); `PENDING → SOLIDIFIED|QUARANTINED|FAILED` allowed with revision+1 and correct `solidified_at` shape; terminal absorbing; `ck_evidence_state_shape` probes (preemptor `ck_evidence_status` / `ck_evidence_state_shape` overlap handled like the recording pair: savepoint-drop `ck_evidence_state_shape` to isolate `ck_evidence_status`, rollback, restore).
  - Evidence-event / artifact append-only probes as owner (`P0001`).
  - Non-creation lifecycle events (`EVIDENCE_SOLIDIFIED` etc.) do not satisfy the creation cycle.
  - Event-bearing default witness: `occurred_at` defaults to `now()`.

  **`concurrentV008EvidenceCreationAndRecordingIdentityHasOneWinner`**
  - Bounded two-connection race (existing `application_name` + `pg_stat_activity` + `statement_timeout` helper): two txs inserting the same `uq_evidence_source_event` / same `uq_recording_protocol_id` / same `uq_media_object_version` / same `uq_evidence_created_event` (distinct PKs/`event_id` so only the named unique can fail) — exactly one winner, loser gets unique-violation or waits then fails.
  - Both-record-and-event pair inserted concurrently under the same source `eventId` — one commits, one does not.
  - Rollback interaction with deferred triggers: a losing concurrent tx that already inserted record+event rolls back completely (deferred triggers do not leave orphans).
  - **Method-local cleanup (never shared fixtures; shared local-id cleanup helper, not a new framework):** record every method-local id (artifacts, events, records, recordings, media objects, and method-local IVR/reservation/subject parents). Name the three immutable V008 triggers exactly — `trg_media_object_immutable`, `trg_evidence_artifact_immutable`, `trg_evidence_event_immutable`. In one transaction: `DISABLE TRIGGER` those three; FK-safe delete in order artifacts → events → records → recording → media → method-local IVR/reservation/subject parents (each parent domain in its existing own FK-safe order); `ENABLE TRIGGER`; assert `pg_trigger.tgenabled='O'` restored for each. Deferred cycle pair (record+events) deleted together. No pending creation-trigger INSERT/UPDATE in the cleanup tx. After the cleanup transaction commits, on a **fresh connection** assert per-id zero rows for every recorded fixture id/table (not shared/global counts). Owner/migration identity deletes rows with correct schema ownership (`evidence_owner`/migration identity for evidence tables; `ivr_owner`/`subject_number_owner`/`entitlement_metering_owner` for parents). Do not delete rows created by other methods.

**39-check witness map (every named check has exactly one positive or negative witness; savepoint-isolated `23514` with exact constraint name unless noted). Coverage rule: 38 checks are proven by a `23514` negative (or isolated-preempting `23514`) plus a positive control; `ck_evidence_source_ref` is the sole **enforced-by-NOTNULL exception** (see its row). Keep the exact CHECK catalog entry; do not drop `NOT NULL` and do not claim `23514` fired for it:**

| Table | Check | Witness |
| --- | --- | --- |
| media_object | `ck_media_object_provider` | negative: `storage_provider <> 'COS'` |
| media_object | `ck_media_object_bucket` | negative: empty/overlong `bucket` |
| media_object | `ck_media_object_key` | negative: empty/overlong `object_key` |
| media_object | `ck_media_object_version` | negative: empty/overlong `object_version_id` |
| media_object | `ck_media_object_sha256` | negative: non-64-hex `sha256` |
| media_object | `ck_media_object_size` | negative: `size_bytes <= 0` |
| media_object | `ck_media_object_mime` | negative: empty/overlong `mime_type` |
| media_object | `ck_media_object_key_ref` | negative: empty/overlong `crypto_key_ref` |
| media_object | `ck_media_object_worm_status` | negative: status outside enum |
| recording | `ck_recording_protocol_id` | negative: empty/overlong `recording_id` |
| recording | `ck_recording_declared_format` | negative: empty/overlong `declared_format` |
| recording | `ck_recording_declared_size` | negative: `declared_size <= 0` |
| recording | `ck_recording_declared_duration` | negative: `declared_duration <= 0` |
| recording | `ck_recording_declared_sha256` | negative: non-64-hex `declared_sha256` |
| recording | `ck_recording_validated_duration` | negative: `validated_duration <= 0` (non-NULL) |
| recording | `ck_recording_validated_sha256` | negative: non-64-hex `validated_sha256` (non-NULL) |
| recording | `ck_recording_status` | negative: status outside enum. Preemptor `ck_recording_state_shape` must be savepoint-dropped (V007 `expectV007CheckWithIsolatedPreempting` pattern) so the exact `23514` names `ck_recording_status`; rollback + full catalog restoration |
| recording | `ck_recording_state_shape` | negative: each of the four illegal shape combinations (DECLARED with media/validation; UPLOADED with validation; VALIDATED missing a validation col; QUARANTINED without `validated_at`) |
| recording | `ck_recording_upload_expiry` | negative: `upload_expires_at <= created_at` |
| recording | `ck_recording_revision` | negative: `revision < 1` |
| evidence_record | `ck_evidence_source_type` | negative: `source_type <> 'CALL_RECORDING'` |
| evidence_record | `ck_evidence_source_ref` | **enforced-by-NOTNULL exception.** Column `source_ref_id uuid NOT NULL` preempts the redundant CHECK (`source_ref_id IS NOT NULL`). Production-schema witness: `expectSqlFailure(..., "23502", ..., NULL source_ref_id)` — expect `23502`, **not** `23514`, and do **not** claim `ck_evidence_source_ref` fired. Positive witness: catalog still lists the exact CHECK definition; a valid row with non-NULL `source_ref_id` commits. Do **not** drop `NOT NULL`. |
| evidence_record | `ck_evidence_source_domain` | negative: `source_event_domain <> 'EVIDENCE'` |
| evidence_record | `ck_evidence_source_event_id` | negative: empty/overlong `source_event_id` |
| evidence_record | `ck_evidence_capture_shape` | negative: non-object / missing required keys |
| evidence_record | `ck_evidence_capture_subject` | negative: `subjectId` ≠ `subject_id` |
| evidence_record | `ck_evidence_consent_version` | negative: empty/overlong `consent_version` |
| evidence_record | `ck_evidence_status` | negative: status outside enum. Preemptor `ck_evidence_state_shape` must be savepoint-dropped (V007 isolated-preempting pattern) so the exact `23514` names `ck_evidence_status`; rollback + full catalog restoration |
| evidence_record | `ck_evidence_state_shape` | negative: PENDING with `solidified_at`; SOLIDIFIED without; terminal with `solidified_at` |
| evidence_record | `ck_evidence_revision` | negative: `revision < 1` |
| evidence_record | `ck_evidence_snapshot_shape` | negative: non-object / missing `capture`/`content` / mismatched `source` fields |
| evidence_record | `ck_evidence_snapshot_sha256` | negative: non-64-hex `snapshot_sha256` |
| evidence_record | `ck_evidence_link_shape` | negative: `session_id` set with `call_id` NULL |
| evidence_artifact | `ck_evidence_artifact_role` | negative: `artifact_role` outside `ORIGINAL`/`DERIVED` |
| evidence_event | `ck_evidence_event_domain` | negative: `event_domain <> 'EVIDENCE'` |
| evidence_event | `ck_evidence_event_id` | negative: empty/overlong `event_id` |
| evidence_event | `ck_evidence_event_type` | negative: `event_type` outside enum |
| evidence_event | `ck_evidence_event_payload` | negative: non-object `payload_redacted` |
| evidence_event | `ck_evidence_event_payload_sha256` | negative: non-64-hex `payload_sha256` |

**Tests/commands:** same `mvn -Pcontainer-smoke verify`; CI expected-set now 57 names, labels `PA8 V008`. Source, CI expected-set and **Failsafe** container IT XML (`target/failsafe-reports/TEST-*PostgresContainerIT.xml`) must be equal at 57 (`skipped=0/failures=0/errors=0`). Default Surefire (`target/surefire-reports/TEST-*.xml`) stays the separate unchanged 10 unit/module tests — do **not** require Surefire to report 57.

**Assertions (slice 2 complete iff):**

- All 39 named checks have a positive or negative witness per the table above, with `ck_evidence_source_ref` explicitly covered as the enforced-by-NOTNULL exception (`23502` + catalog/positive witness; no `23514` claim).
- Both insertion orders commit. FK-satisfied cycle contradictions raise `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION` at COMMIT with zero survivors; genuinely-absent parents raise `23503` at COMMIT; duplicates raise immediate `23505`. Do not claim one error class for all negatives.
- Append-only + guard + revision + terminal-absorbing backstops green.
- Recording-only rollback leaves no survivor. Full T-S3-12 rollback (record+event+artifact+Outbox) leaves no survivor. No service implementation required — raw SQL in the test tx is sufficient to prove the DB contract.
- Bounded concurrency has exactly one winner per unique key; no orphan deferred rows.
- Container report = 57 methods, 0 skipped/failures/errors; README + CI labels say PA8 V008.

**Stop conditions (slice 2):**

- Deferred cycle does not fire at COMMIT.
- Either insertion order is rejected.
- Any append-only table is mutable.
- Any survivor after forced rollback.
- Frozen T-S3-09/T-S3-11 classified wording cannot be satisfied for the class it names (stop and report; do not weaken the assertion).
- Material design contradiction (stop and report).

### Slice-count rationale

2 slices = the minimum that keeps catalog/ACL/FK independently verifiable from lifecycle/concurrency, matching the accepted PA7 shape. A 3rd slice would only be justified by a late cross-wave ALTER_FK or a function wave; V008 has neither. Progressive source=CI=Failsafe-XML inventory: 51 → 54 (slice 1) → 57 (slice 2), cumulative full green at each accepted slice. `expectCommitFailure` is introduced in S1 and reused in S2.

---

## 10. Helper patterns to reuse (do not invent new frameworks)

From `PostgresContainerIT.java`:

| Pattern | Use for |
| --- | --- |
| `queryStrings` / `queryLong` / `toRegclass` | catalog, ACL, FK inventories |
| `expectSqlFailure(connection, sqlstate, constraintOrMessage, sql, params)` | named-check `23514`, missing-parent `23503`, guard/immutable raises — **statement-time only** |
| **`expectCommitFailure(connection, sqlstate, messageFragment, setup)`** (new in **S1**, reused in S2) | commit-time deferred FK/trigger failures (`23503` / `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`); dedicated tx, COMMIT, assert, zero survivors. **Not** for RESTRICT parent actions (immediate even when DEFERRABLE). First use: S1 deferred parent-delete (`fk_evidence_event_record`). |
| Savepoint-isolated overlapping checks | one tx, many negative probes, no cross-contamination |
| `aclexplode` / `grantee = 0` PUBLIC expansion (PostgresContainerIT ~8647–8675) | PUBLIC zero on evidence schema/table/column/function; **never** `has_schema_privilege('public', ...)` |
| `FOR SHARE` / `FOR UPDATE` 42501 probes (PostgresContainerIT ~2664–2741) | foreign row-lock denial on the four cross-module parents |
| `@BeforeAll` staged Flyway `target(N)` + immutable booleans | V007-prefix V008-absence witnesses |
| Bounded two-connection race with `application_name`, `pg_stat_activity` wait detection, `statement_timeout` | unique-winner races |
| Dependency-order FK-safe cleanup | leave container clean across methods; immediate children first (artifact → event → record → recording → media → method-local IVR/reservation/subject parents) |
| Shared method-local cleanup helper (V007 `cleanupV007S2LocalRows` shape at `:10068-10167`) | S1 committed parent-delete pair (after survival observation) and S2 committed pairs/concurrency rows; `DISABLE` the three named immutable triggers; ordered delete including `recording`; `ENABLE`; `tgenabled='O'`; fresh-connection per-id zero residue |
| Savepoint-scoped `DISABLE TRIGGER` + `tgenabled` restoration | protected-parent FK probes (`trg_media_object_immutable` only, solely to reach FK) |
| V007 `expectV007CheckWithIsolatedPreempting` (`:8889-8924`) | enum-vs-shape overlap: savepoint-drop `ck_recording_state_shape` / `ck_evidence_state_shape`, probe enum check, rollback, catalog restore |
| Method-local id collection (UUIDs per test) | parallel-safe isolation |

From `V007__activation_and_consent.sql`:

| Pattern | Use for |
| --- | --- |
| `SET LOCAL ROLE <owner>` … `RESET ROLE` | table/function/trigger/cross-module grants |
| Inline named `CONSTRAINT`s on CREATE TABLE | all checks/keys |
| `CREATE FUNCTION ... SECURITY INVOKER SET search_path = <schema>, pg_temp` | 5 trigger functions |
| `REVOKE ALL ... FROM PUBLIC` then targeted `GRANT` | ACL |
| Migration-identity `ALTER TABLE ... ADD CONSTRAINT` after `RESET ROLE` | the 8 ordinary FKs + 2 deferred cycle FKs |

From `ci.yml`:

| Pattern | Use for |
| --- | --- |
| Python expected-set of exact `@Test` names | 57-name set |
| `tests == len(expected)`, `skipped=0`, `failures=0`, `errors=0` | cumulative green |
| Label strings | PA8 V008 |

---

## 11. Environment

| Item | Value |
| --- | --- |
| JDK | Temurin 21 (`/opt/jdk21`) |
| Maven | `/opt/maven-3.9.9` |
| Docker | healthy WSL Docker, `postgres:16.15` |
| Git | Windows gitdir pointer → launcher must export `GIT_DIR` / `GIT_WORK_TREE` |
| Container test path | `mvn -Pcontainer-smoke verify` |
| Network/credentials | none required; no auth files read; no secrets printed |

---

## 12. Docs decision

- **Authoritative:** `docs/14-production-data-model-and-flyway-schema-spec.md` (V008 contract) + `docs/12-technology-stack-and-system-architecture.md` (module boundaries) + controller active PA8 checkpoint.
- **Not amended:** `docs/12-*` and `docs/14-*` are read-only in this gate. Any contradiction is a stop-and-report, not a silent edit.
- **Resolved design decisions (BD1/BD2):** user-approved A/A reconciliation on 2026-10-30. BD1: ordinary S3 FKs RESTRICT; two reciprocal V008 creation-cycle FKs retain §11.5 exact DDL with default NO ACTION DEFERRABLE INITIALLY DEFERRED (`confupdtype='a'`, `confdeltype='a'`). BD2: classified enforcement (FK-satisfied cycle contradiction → `P0001`/`EVIDENCE_CREATION_CYCLE_VIOLATION`; genuinely absent parent → `23503` exact deferred constraint; duplicate creation event → immediate `23505` on `uq_evidence_created_event`). No unresolved alternatives or pick-one steps remain. Recorded in `docs/gateflow/pa8-v008-evidence-foundation-spec-reconciliation.md`. Historical fix-1 and the initial planreview remain read-only.
- **Gateflow plan artifact:** this file. Implementation acceptance may append a short "accepted" note; content of inventories must not be rewritten to match a discovered defect — fix the code or stop.
- **README / CI:** operational witnesses, updated to match the accepted implementation (57 methods, PA8 V008, V008 inventory prose, "transaction rollback" not "rollback migration").

---

## 13. Risks, owners, destinations

| Risk | Owner | Destination / mitigation |
| --- | --- | --- |
| Deferred cycle trigger does not fire at COMMIT (wrong timing/name) | implementer | S2 `v008CreationCycleBothOrdersAndTerminalLifecycleBackstops`; compare trigger catalog to §10.22 row-for-row |
| Both insertion orders not actually supported (DDL-order bug) | implementer | S2 T-S3-08 both orders; S1 asserts FKs added only after both tables |
| `assertNoV008PlusObjects` callers silently drop prefix witnesses | implementer | §8 staged-V007 booleans; S1 asserts all staged flags true after full migration |
| Accidental `sharing_role` grant (F-07) | implementer | S1 ACL test greps `pg_*` for any `sharing` reference; CI failure |
| SQL service function creep (function wave ≠ 5) | implementer | S1 inventory asserts the five named service functions by signature with `prosecdef=true` (four V001 + `entitlement_metering.issue_first_trial`), asserts SECURITY DEFINER user-function count is exactly 5, and asserts V008 adds only the five SECURITY INVOKER trigger functions. Do not assert total `pg_proc` = 5 (post-V008 total is 40). |
| V001–V007 byte drift | implementer | §6.1 digest witness in S1; allowed-files fence |
| CI expected-set drift (count or names) | implementer | Python exact-name-set assertion; update incrementally per slice |
| Concurrent unique-winner race flaky | implementer | existing bounded race helper; method-local ids; `statement_timeout` |
| Down/repeatable rollback file invented (R__/U008/R008/target-reverse) | implementer | **Forbidden.** Transaction-rollback evidence only (T-S3-12 + recording-only rollback). Migration set is forward-only V001–V008. |
| Design contradiction discovered (column/check/FK name or mode differs from frozen doc) | implementer | **STOP and report**; do not amend doc-14 or the plan inventory |
| Controller dirty file accidentally committed | implementer | forbidden-files fence; `git status` check before any future commit (out of this gate) |

---

## 14. Completion format (implementation gate report)

When implementation (future gate) completes, report:

1. **Verdict:** `complete` | `blocked`
2. **Artifact paths:** `V008__evidence_media.sql`, test/CI/README diffs
3. **Exact files changed** (must match §6 allowed list; no rollback/down/U/R migration file)
4. **Validation actually performed:** commands run, method counts (51→54→57), digest check, CI expected-set equality, transaction-rollback witnesses
5. **Slice count and rationale** (2, as above)
6. **Blocking questions** (empty if complete; BD1/BD2 are resolved — see reconciliation artifact)
7. **Residual owners** (Q11 WORM, Q22 ticket TTL, legal retention, etc. — unchanged from doc-14 pending list)

---

## 15. Implementation checklist (for the coding gate)

- [ ] Confirm BD1/BD2 resolutions are recorded (reconciliation artifact); PR-002/003/004/006/007/008/009 and RR-001..008 repairs still need independent re-review before implementation (no re-review acceptance claimed)
- [ ] Read §3 inventories and §4 nine-step order before writing a line of SQL
- [ ] Create `V008__evidence_media.sql` (only new migration file; no rollback/down/U/R file)
- [ ] Confirm V001–V007 digests match §6.1
- [ ] Extend `@BeforeAll` with staged-V007 prefix captures
- [ ] Update `assertSchemasAndRoles` + `assertFunctionsAndTrigger` + history block + V003 schema-absence + V008-absence helper (§8) without weakening staged flags; define `V008_ROLES` separate from `S1_ROLES`/`V004_ROLES`
- [ ] Rename migration test; add 3 S1 methods (including `expectCommitFailure` helper where first used); update CI expected-set (54) + README together; run `mvn -Pcontainer-smoke verify`; assert source=CI=**Failsafe** XML (`target/failsafe-reports/TEST-*PostgresContainerIT.xml`) at 54, Surefire stays 10
- [ ] Accept slice 1
- [ ] Reuse `expectCommitFailure`; add 3 S2 methods with the 39-check map (NOTNULL exception explicit); update CI expected-set (57) + labels; update README; run `mvn -Pcontainer-smoke verify`; assert source=CI=**Failsafe** XML at 57, Surefire stays 10
- [ ] Accept slice 2
- [ ] Final report in the §14 format
- [ ] **STOP** (no commit/push/PR in the plan gate; implementation gate follows controller instructions)
