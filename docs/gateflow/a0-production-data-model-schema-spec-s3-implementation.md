# A0 S3 Implementation Artifact

- Task label: `a0-spec-s3-impl-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate / slice: `implementation`, S3 evidence/media/sharing closure (V008-V009)
- Runtime / provider: Claude Code / `ds` / DeepSeek-V4.1-Flash (configured runner)
- Date: 2026-10-22
- Status: S3 author-complete; independent S3 code review pending. S1 and S2 remain accepted. No S3 semantic acceptance, full A0 completion, production implementation or runtime test is claimed.

## 1. Prerequisites and binding inputs

- Accepted S1 checkpoint: `5404d9caa73b72c412ca2065bef19dfb46d154d3`; accepted S1 specification SHA256 `B7FE164B701F8813A75212C4D8EC4624A97867FB7289EA652F3FE2283E510099`.
- Accepted S2 checkpoint: `8934df90c15dcd8173486c6ad753ab27c91ac765`; accepted S2 specification SHA256 `BDFE41EF0B0A278EC196C2E93926BDD1666A8D986135E14DE3099B9491CC72BF` (643834 bytes / 3982 lines). Independent review `docs/reviews/code-review-20261022-215322.md` passed the material S2 contracts and deferred only F4-01.
- Accepted plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`, SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`.
- Fix-6 input: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`, SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`.
- The nine frozen structural blocks, 44 catalog rows, 40 invariant rows, nine wave rows, three slice membership blocks, 28-sensitive row set, 28 open-input rows, 24 key sets and five function-wave rows were preserved byte-for-byte. The accepted S1/S2 semantics, global lock order, `FIRST-TRIAL`, seven IVR actions/null direct purpose, authenticated replay, 30-second task TTL/auto-dial and `UNKNOWN`/usage preservation were not redesigned.

## 2. Changed paths

Only these two paths were written by this task:

1. `docs/14-production-data-model-and-flyway-schema-spec.md` (cumulative specification; S3 sections and document-status integration).
2. `docs/gateflow/a0-production-data-model-schema-spec-s3-implementation.md` (this new artifact).

No executable SQL, Flyway migration, Maven/Spring/Java code, API/OpenAPI contract, UI, cloud resource, credential, prototype, screenshot, Git/config file, historical review/fix artifact, control document or old untracked review was written. No stage/commit/push/PR/merge occurred.

## 3. Authored S3 result

### 3.1 Exact dictionaries and columns

The eight accepted V008-V009 dictionaries were expanded with every accepted column exactly once, in accepted order:

| Table | Columns | Key S3 content |
| --- | --- | --- |
| `evidence.media_object` | 11 | Immutable private object provider/bucket/key/version/SHA-256/size/MIME/key-ref/WORM observation; no bytes or permanent URL. |
| `evidence.recording` | 15 | `DECLARED/UPLOADED/VALIDATED/QUARANTINED`; declared facts never win over server hash/duration/MIME/size; exact call FK; upload expiry and retry/quarantine convergence. |
| `evidence.evidence_record` | 17 | Immutable captured subject/call/session/reservation/identity/consent/snapshot; `PENDING/SOLIDIFIED/QUARANTINED/FAILED`; one matching creation event; source-ref polymorphic internal reference without a new FK decision. |
| `evidence.evidence_artifact` | 5 | One original plus separate derived objects; no overwrite/repoint; append-only. |
| `evidence.evidence_event` | 8 | `EVIDENCE_CREATED` plus solidification/quarantine/failure events; composite unique target; deferred reverse FK; partial unique creation ownership. |
| `sharing.evidence_share_grant` | 20 | Frozen content/verification scope; digest-only capability token; salted slow-KDF code; designated-mobile HMAC; immutable content hash; status/generation/revision; no plaintext secret. |
| `sharing.share_admission` | 7 | Durable one-time admission; unique key/no; mode/result; trigger-backed count; immutable. |
| `sharing.share_access_event` | 11 | Append-only verification/admission/ticket/playback/transition/failure events with deterministic domain/event identities and admission-time generation/expiry witness. |

Total: 44 dictionaries / 522 columns = S1 121 + S2 307 + S3 94. The exact S3 column names and order match the accepted catalog; no column, table, SQL function or function-wave row was added.

### 3.2 Evidence cycle

- `evidence_record(source_event_domain, source_event_id)` references `evidence_event(event_domain, event_id)` through the composite unique key; `evidence_event.evidence_id` references `evidence_record.id` in the reverse direction.
- Both FKs are `DEFERRABLE INITIALLY DEFERRED`, added by `ALTER TABLE` only after both V008 tables and the composite unique key exist. There is no single-column event-id FK and no one-sided cycle.
- The creation event type is exactly `EVIDENCE_CREATED`; partial unique `(evidence_id) WHERE event_type = 'EVIDENCE_CREATED'` permits exactly one owner.
- Two owner-local `SECURITY INVOKER` deferred constraint triggers (`trg_evidence_record_creation_cycle`, `trg_evidence_event_creation_cycle`) validate at commit that the creation event type, source identity and owning `evidence_id` match the record. Missing/duplicate/wrong-type/wrong-owner/mismatched facts raise `EVIDENCE_CREATION_CYCLE_VIOLATION`; the whole transaction rolls back and no creation fact survives. Record+event may be inserted in either order in one local transaction. Outbox/audit append in the same transaction.
- No cross-schema SQL function or function-wave entry was added. The triggers are local structural controls, not published services.

### 3.3 Recording/media and evidence lifecycle

- IVR owns telephony, call/leg, recording-source and authoritative usage facts; `evidence` owns media/evidence metadata and lifecycle. Media workers submit validated results; they never own or directly write evidence lifecycle.
- Server object version/hash/size/MIME/duration/malware validation wins; declared hash/duration never wins. Mismatch, unknown storage, expired upload, late/out-of-order completion and partial crashes converge to `QUARANTINED`, a retryable non-solid state, or the first terminal outcome without overwriting the original object/version or fabricating evidence.
- Evidence is created for registered or unregistered/unverified IVR users from the validated capture. `identity_at_capture` and consent version are immutable; later claims, account changes, membership changes, suspension or real-name changes never rewrite captured subject/session/consent/history.
- Call completion and evidence solidification remain independent.

### 3.4 Sharing

- Grant scope is frozen to one `SOLIDIFIED` evidence record; content/verification snapshots and hash are immutable. The creator's current account/workspace access is checked through exact S1 reads under ranks 7/8/9 where applicable; no foreign row lock or write.
- Historical-evidence access also re-reads the exact current `subject_number` ownership/binding/risk projection. Any `OPEN` risk on the evidence subject's captured/active binding denies grant creation, admission, reconstruction and media renewal with `SHARE_RISK_BLOCKED` and no count/ticket; a risk-open committed before the re-read is observed, and a later risk-open governs subsequent access without rewriting historical facts. `OPEN` risk blocks historical-evidence access, not outbound dialing by itself.
- Verification modes are `NONE`, `EXTRACTION_CODE`, `DESIGNATED_MOBILE` and `ORGANIZATION_MEMBER`. `NONE` is link possession, never verified identity. Code uses a per-grant/generation salt and salted slow KDF digest. Designated mobile stores only a keyed HMAC and key version in the immutable snapshot; no plaintext phone. Organization-member verification requires current active membership and the grant organization.
- The capability token has at least 128 random bits; only its SHA-256 digest is stored. No plaintext token/code/phone/object URL is stored or logged. KDF parameters, admission limits, expiry defaults, rate-limit thresholds, ticket TTL and access-role policy remain approved-input/owner destinations; absent/ambiguous policy fails closed.
- Admission is a durable PostgreSQL fact: grant row lock, current status/time/generation/code-generation/mode/verification/count checks, unique `(grant, admission_key)` and `(grant, admission_no)`, one count increment, immutable admission, same-key retry returns the original admission. Page opens, prefetch and failed verification never consume an admission. Reaching `max_admissions` blocks new admissions only and never bumps `revocation_generation`; the last admitted session remains within its recorded session/grant validity. After session expiry the same key returns `SHARE_SESSION_EXPIRED` without a new ticket; a new admission/recheck/count is required, preventing unlimited reuse.
- PostgreSQL commits the admission and `ADMISSION_GRANTED` witness (including admission-time revocation/code generations, grant/admission/session expiry) before the Redis ticket. The ticket key is derived from `share_grant_id + admission_id`. Redis failure/lost response/lost cache reconstructs from the durable admission without a second count; current status/time/generation must match the witness, so a stale admission cannot bypass revoke, code rotation, expiry or wrong-tenant policy. Redis unavailability fails closed; no successful media authorization is claimed. Short signed/segmented credentials define a bounded revoke window; no recall of buffered/already-issued bytes is promised.
- Revoke/code rotation/expiry/lock/extension transitions use conditional revision/generation updates, deterministic access events, same-action replay/conflict and audit actor identity. Locking blocks new admission/verification only; it does not itself invalidate an already-admitted session. Extension does not retroactively extend a recorded admission.

### 3.5 Lock, privilege and security boundaries

- S1/S2 global order is unchanged: caller (1) → needed canonical number (2) → subject lifecycle (3) → owned session/current call/activation (4) → subject owner (5) → binding/risk (6) → organization (7) → membership pair (8) → account (9) → platform idempotency (10) → task claim (11) → balances/reservation/usage (12) → employee/admin (13) → Outbox (14) → audit (15).
- S3 takes no caller/number/subject/session/call/activation/idempotency/task/balance/employee lock and no foreign row lock. Sharing takes only needed S1 suffix ranks 7/8/9 advisory locks before its owner grant row; evidence takes only evidence-owned recording/record rows. Owner-local orders are explicit. No lock is held across COS/Redis/media-worker I/O.
- S1 whole-transaction bounded restart and S2 phase-B savepoint are untouched. S3 uses whole-transaction rollback/bounded retry for evidence cycles and admissions; no savepoint.
- `evidence_role`/`sharing_role` receive only their own schema/table privileges, exact cross-module SELECT columns with schema USAGE (including sharing's exact subject ownership/binding/risk projection), and EXECUTE on `platform.outbox_append`/`audit.append_fact`. No foreign DML, row-lock privilege, `DELETE`/`TRUNCATE`, schema `CREATE` or platform/audit table write. No S3 SQL function exists.

### 3.6 Migration and closure

- V008 creates `media_object → recording → evidence_record → evidence_artifact → evidence_event`, then both deferred cycle FKs (after both tables/unique key), then the guard/append-only and deferred creation triggers. V009 creates the grant → admission → access event, then the sharing guards/triggers. Parent waves are V002/V003/V005/V006/V008 or same-wave; no forward reference, no backfill, no executable migration.
- The final matrix covers 44 tables / 522 columns / 13-23-8 slices / 81 FK decisions (74 `FK` + 3 `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK`) / 24 key sets / 40 invariants / 28 mutable + 16 append-only / 28 sensitive tables / nine waves / five functions / S1-S3 services and test matrices.

### 3.7 F4-01 closure

F4-01 is closed as a documentation-status-only correction. The header, §1, §13.1 and T-S2-65 now state S1 accepted at `5404d9c`, S2 accepted at `8934df9`, and S3 author-complete / independent review pending. The obsolete `fix-3` row label was removed; no historical artifact or S2 business semantics was changed. Current assertion evidence for the reviewer: T-S2-65 is the current-status row; §13.1 greps the current S2/S3 status; the S3 artifact records the closure.

## 4. Validation commands and actual outputs

All commands were run read-only from `/mnt/d/codex-call`; no PostgreSQL, Flyway, COS, Redis, media-gateway or Testcontainers execution is claimed.

### 4.1 Original unchanged structural script

```bash
sha256sum /tmp/a0s1/structural-v2-run.sh
# ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545
FILE=docs/14-production-data-model-and-flyway-schema-spec.md \
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md \
bash /tmp/a0s1/structural-v2-run.sh
```

Actual output:

```text
fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81
catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24
```

Actual exit: `0`. This script was not modified or weakened; it validates the frozen structural baseline, wave/slice membership, FK targets/modes, function waves, key sets and whitespace.

### 4.2 Read-only S3 document checker (unchanged)

```bash
powershell.exe -NoProfile -ExecutionPolicy Bypass \
  -File 'C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s3-document.ps1'
```

Actual output:

```json
{
    "FrozenBlocks": 9,
    "Dictionaries": 44,
    "Columns": 522,
    "SliceColumns": { "S1": 121, "S3": 94, "S2": 307 },
    "Bytes": 792328,
    "SHA256": "957E2C753291B56061484A76EFE1BF3A7F0A2C1F72AA2269D80D3B861CA82D11",
    "Errors": []
}
```

Actual exit: `0`. Expected `FrozenBlocks9`, `Dictionaries44`, `Columns522`, `SliceColumns S1=121/S2=307/S3=94`, `Errors=[]` all matched.

### 4.3 Exact S3 dictionary field-order comparison

The read-only loop compared each dictionary's first column in accepted catalog order with its accepted field list. Actual output:

```text
evidence.media_object exact 11 columns
evidence.recording exact 15 columns
evidence.evidence_record exact 17 columns
evidence.evidence_artifact exact 5 columns
evidence.evidence_event exact 8 columns
sharing.evidence_share_grant exact 20 columns
sharing.share_admission exact 7 columns
sharing.share_access_event exact 11 columns
ALL_EXACT
```

Actual exit: `0`; no missing, extra or reordered S3 column.

### 4.4 Embedded document checks

The S1/S2/S3 dictionary and control blocks embedded in §13 were extracted and executed unchanged:

```text
13.1 Document-level checks: exit 0
13.2 S1 per-table coverage: exit 0
13.2.1 S2 per-table coverage: exit 0
13.2.2 S3 per-table coverage: exit 0
13.8 S3 control coverage: exit 0
```

The S3 control counts were: 8 lifecycle statements / 8 sensitive statements / 8 revision-or-append-only statements / 16 S3 FK detail rows / 13 sensitive-detail table rows / two creation-cycle trigger references / five T-S3 transaction sequences / three S3 contract sections / unchanged five function-wave rows.

### 4.5 Whitespace, encoding and terminal LF

```text
trailing_whitespace_lines=0
CR_count=0
bytes=792328
lines=4817
final_bytes=64 0a
```

The final two bytes are `d` and one LF; the file ends with exactly one LF, has no CR, no trailing whitespace and no blank line at EOF.

### 4.6 Hashes and scope evidence

```text
957e2c753291b56061484a76efe1bf3a7f0a2c1f72aa2269d80d3b861ca82d11  docs/14-production-data-model-and-flyway-schema-spec.md
149472d291dc19f81497a0599c85122bdda311dc92417c3e2086667130b231a5  docs/gateflow/a0-production-data-model-schema-spec-plan.md
8976f64a215a2b7b5cb116a14d5123aff91f754f512b6fd212e0d8c0c7045f59  docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md
```

`git status --porcelain` shows exactly `M docs/14-production-data-model-and-flyway-schema-spec.md` and `?? docs/gateflow/a0-production-data-model-schema-spec-s3-implementation.md` as task-owned paths. All other dirty paths are the pre-existing control-document edit, `logo/preview.html`, the QA screenshot set and the old untracked `docs/reviews/plan-review-20261021-205118.md`; they were not written, deleted, staged or committed by this task. No `.sql` or `db/migration` path exists. A full pre-dispatch hash manifest was not captured by this author; the controller's independent manifest remains the authoritative scope check. The accepted plan and fix-6 hashes are unchanged.

## 5. Docs decision

The cumulative specification remains `docs/14-production-data-model-and-flyway-schema-spec.md`; its S3 dictionaries and behavior are now authored alongside the accepted S1/S2 content. This new artifact is the S3 authoring/gate record. No other documentation, design source, controller, prototype, screenshot, historical artifact or Git metadata was changed. No API/OpenAPI document was created.

## 6. Goal alignment and no-over-design

- Every S3 addition maps to the confirmed evidence/media/sharing goal or a necessary correctness/security condition: media identity/version/hash, server validation, immutable evidence capture, reciprocal creation ownership, original/derived artifacts, share content/verification/admission/revocation/ticket facts.
- No table, column, invariant, SQL function, function-wave row, eighth IVR action, fourth slice, migration file, provider configuration, credential or speculative workflow was added. The eight tables/94 columns were already accepted; S3 only expands them.
- Designated-mobile verification uses the existing `content_snapshot` JSONB for its keyed HMAC/key version rather than adding a column. This is an immutable authorization binding inside the accepted snapshot column; no raw phone is stored. `NONE` is not treated as verified identity.
- No production KDF parameter, admission limit, expiry, ticket TTL, retention period, access-role matrix, WORM/object-lock duration or provider value is guessed. Q2/Q3/Q7/Q10/Q11/Q12/Q15/Q22/Q23 and the sharing access-policy input remain assigned; absent/ambiguous policy fails closed.
- S3 preserves the modular-monolith, module ownership, one local transaction, S1/S2 lock order, S1 whole-transaction restart, S2 phase-B savepoint and all accepted S1/S2 contracts.

## 7. Limitations and residual risks

### 7.1 Limitations

- Validation is document-level only. No PostgreSQL instance, migration, constraint, deferred trigger, privilege, row-lock, savepoint, concurrency, COS object/version/WORM, Redis ticket/cache, media-gateway, IVR line or Testcontainers suite was executed.
- The S3 creation-cycle SQL, trigger timing/operation syntax, admission guard and sharing grants/privileges are implementation contracts that must be proven in a real PostgreSQL 16 migration and privilege suite.
- The designated-mobile HMAC-in-`content_snapshot` representation is a specification choice using the accepted column; a reviewer may require a different approved verification-policy representation (which would require explicit scope/change control, not a silent column addition).
- S3 semantic acceptance, independent review, full A0 acceptance and production readiness are not claimed.

### 7.2 Residual-risk classification

| Risk | Classification | Owner / destination |
| --- | --- | --- |
| F4-01 stale S2 status assertion | fixed-current | Closed in T-S2-65/header/§13.1; independent S3 review to confirm. |
| S3 dictionaries/invariants/transactions/closure authored | fixed-current | This artifact and the cumulative specification; independent S3 review. |
| No executable PostgreSQL/Flyway/privilege/trigger/concurrency test | assigned-later-work-unit | Backend/Database; implementation migration and Testcontainers suite. |
| Evidence cycle deferred trigger and both insertion orders not executed | assigned-later-work-unit | Evidence/Database/Backend; implementation migration suite. |
| COS object/version/WORM/KMS capability and media validation not executed | assigned-later-work-unit | Infrastructure/Security/Backend; storage/security/capacity and media integration work. |
| Redis ticket TTL, cache-loss reconstruction, revocation window and media-gateway behavior not executed | assigned-later-work-unit | Security/Sharing/Backend; security design, sharing policy and media-gateway integration. |
| Share-code KDF parameters, admission limits, expiry/rate-limit/access-role policy unapproved | assigned-later-work-unit | Security/Product/Sharing; sharing policy and authorization design (Q12 and the access-policy input). |
| Legal retention, erasure, backup deletion and WORM retention unapproved | assigned-later-work-unit | Legal/Security/Infrastructure; retention/deletion policy (Q3/Q11). |
| Subject-risk clearing and historical-evidence access policy beyond the approved `OPEN` block | assigned-later-work-unit | Product/Risk/Operations; claim-risk and historical-evidence access policy. |
| Exact record/share wire/event encodings and public API contract unapproved | assigned-later-work-unit | Backend/Security; IVR protocol and OpenAPI/security design (Q15/Q22/Q26). |
| S3 object/evidence/share sensitive redaction and no-plaintext rules need executable checks | assigned-later-work-unit | Evidence/Sharing/Backend/Security; implementation redaction and audit tests. |
| No later approved slice exists after S3 | no later-approved-slice residual | S3 is the final approved slice; no fourth slice or future dictionary gap is planned. |
| S1/S2 runtime implementation, first-trial/IVR/commerce concurrency, payment/COS/Redis integration remain open | assigned-later-work-unit | Backend/Database/IVR/Payments/Infrastructure/Security/Operations; their named prior destinations. |

## 8. Completion report

Conclusion: S3 evidence/media/sharing authoring is complete at the document level; independent S3 code review is the next gate. Result: eight accepted S3 dictionaries/94 columns, INV-15/24/25/26/39 expansions, reciprocal deferred evidence cycle, sharing admission/ticket/access contracts, S3 lock/privilege/trigger contracts, V008-V009 migration order, final cross-slice matrix and F4-01 status closure are authored without changing the frozen baseline or adding schema/function/scope. Checks: unchanged structural script exit 0 with the exact baseline; unchanged S3 checker exit 0 with `FrozenBlocks9/Dictionaries44/Columns522/S1=121/S2=307/S3=94/Errors=[]`; exact S3 field-order comparison `ALL_EXACT`; embedded S1/S2/S3 checks exit 0; zero trailing whitespace/CR, exactly one terminal LF; accepted plan/fix-6 hashes unchanged; only the two allowed task paths written. Limitations: no PostgreSQL/Flyway/privilege/trigger/concurrency/COS/Redis/media-gateway/production execution; S3 semantic/full-A0/production acceptance is not claimed. Changed files: the cumulative specification and this artifact. No stage/commit/push/PR/merge was performed, and no later gate was entered.
