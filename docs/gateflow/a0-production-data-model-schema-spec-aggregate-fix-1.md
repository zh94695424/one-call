# A0 aggregate fix 1 completion record

- Task label: `a0-spec-aggregate-fix1-ds-20261022-01`
- Gate: `fix` (aggregate round 1) for `A0-production-data-model-schema-spec`
- Runtime/provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Status: author-complete; stopped after this artifact and document validation. Independent aggregate re-review is required before any acceptance, commit, push or PR.

## Binding inputs

- Current HEAD / accepted S3 checkpoint: `9ad33e8727596366c0a844b7dffcce3eb028891b`.
- Accepted chain: plan `4a98fbb`, S1 `5404d9caa73b72c412ca2065bef19dfb46d154d3`, S2 `8934df90c15dcd8173486c6ad753ab27c91ac765`, S3 `9ad33e8727596366c0a844b7dffcce3eb028891b`.
- Pre-fix specification input: `docs/14-production-data-model-and-flyway-schema-spec.md`, SHA256 `CACE2720EF166C0EBBFCA0B95EEED1392CB356B5A81F2B010AEB2CA42750D4D6`.
- Aggregate review: `docs/reviews/code-review-20261022-234349.md`, SHA256 `EBF57A4636BB6E5FEBE0E812B21F18104E6B71A0E81147C9F0F4D2660B9A1905`.
- Controller: `docs/gateflow/project-execution-control.md`, SHA256 `D0E0333265AACD5185F31B5F11F4135ACCE54E445C5CC96AA379B0C7D461FFE4`; its `Aggregate deepreview result and fix 1 adjudication` section is the binding correction contract.
- Design truth: `docs/12-technology-stack-and-system-architecture.md`, relevant `docs/09`, `docs/10`, `docs/11`, `docs/13`, accepted plan/fix-6 and current S1/S2/S3 artifacts.
- Scope: specification-only. No executable SQL, migration, runtime, API, UI, cloud, credential or product-policy change.

## Write method and scope

- All repository edits used the verified Windows fallback `/mnt/c/Users/Administrator/AppData/Local/OpenAI/Codex/bin/896a0166b774037a/codex.exe --codex-run-as-apply-patch` with one safely quoted patch argument and Windows absolute targets. Native `apply_patch` was unavailable; no Edit/Write/Python/shell-write fallback was used for a repository file.
- Changed paths: `docs/14-production-data-model-and-flyway-schema-spec.md` (modified) and this artifact (new). No other task-owned path was written.
- The controller, accepted plan/fix-6, historical reviews/artifacts, checkers, source, prototypes, screenshots, `logo/preview.html` and Git metadata remain read-only. Pre-existing dirty screenshots and the old untracked `docs/reviews/plan-review-20261021-205118.md` were preserved.
- No stage, commit, push, PR, merge, issue modification, install, network write, credential/provider/WorkBuddy change or later gate was performed.

## Finding dispositions

All items below are author-reported fixed-current corrections; this is not independent acceptance.

| Finding | Disposition and current location |
| --- | --- |
| F-01 direct fingerprint | `§10.17` `DIRECT_AUTHORIZATION` row now uses `purpose`, `protocolVersion`, `callId`, `sessionId`, `callerNumberHmac`, `accessNumberMasked`, `promptVersion`, `enteredCalleeHmac`, `confirmationKey` and `confirmationContent`, and excludes the transport `confirmationEventId` and all timestamps/mutable results. `§9.14` step 1 requires the same semantics with a regenerated transport event ID to replay and a changed session/protocol/access/callee/prompt/key/content to conflict. `T-S2-24`, `T-S2-58`, `T-S2-60` and `T-S2-61` assert both sides. No schema change. |
| F-02 resolve/confirmation fingerprints | `§10.17` `IVR_CONFIRM`/`ACTIVATION_NEXT_ACTION` and `INBOUND_RESOLVE` rows now include purpose/protocol/call/session, caller HMAC/access-number, subject/confirmed-subject/binding identity and the existing semantic fields. `§9.11` step 2 and `§9.13` step 2 state that replacement/revocation/rebinding changes the identity and conflicts before any frozen decision; exact semantics still replay. `T-S2-17`, `T-S2-19` and `T-S2-58` assert it. No schema change. |
| F-03 reconciliation network boundary | `§9.16` is now three phases: A is a short locked `HELD → RECONCILING` transaction that commits and releases all locks; B queries the authoritative self-developed IVR with no PostgreSQL or advisory lock; C relocks in order, re-reads and settles/releases/retains `RECONCILING`. Slow/unavailable/unknown authority keeps durable `RECONCILING` attempt/due/error facts and never releases unknown use. `§9.0.1` item 12 and `T-S2-36` align. |
| F-04 rank-12 order | `§9.16` phases A and C discover `balance_id` with a non-locking read, then lock balance row(s) in ascending UUID before the `usage_reservation`, and reuse the entitlement owner settlement service. `§9.15` step 1 now states the same suborder. `T-S2-34` and `T-S2-36` assert reconciliation-vs-settlement serial order and no reservation-before-balance deadlock. |
| F-05 operations owner lock | `§9.0.1` item 13 and its new note define the operations-iam-owned, same-connection, lock-only application collaborator `operations-iam.lock_employee_administrator`; `§9.19` calls it before the owner-local activation write. `§3.1`, `§3.2`, `§10.12` and `§10.13` record that it writes/appends nothing, is not a SQL function and gives `activation_consent_role` no foreign table/row-lock grant. `T-S2-29`, `T-S2-41` and `T-S2-51` assert the privilege/order boundary. |
| F-06 task/call reciprocal link | `§4.23`/`§4.25`, `§7.3`, `§9.13` steps 6-8 and `§9.11` steps 9/10/12 define the exact link: the pre-savepoint call has `task_id = NULL`; the tentative claim after the task lock sets `claimed_call_id` and permits exactly one null-to-that-task `call_record.task_id` assignment under the existing IVR guard and exact UPDATE grant; success requires reciprocal equality. Category-2 rollback clears both assignments while the call survives; category 3 rolls back all; retry may link again; no relink/task switch/second call. `§10.12` adds `task_id` to the exact UPDATE grant and `§10.16` narrows both guards. `T-S2-14`, `T-S2-15` and `T-S2-45` assert it. No table/column/function added. |
| F-07 role order | `§2.12`, `§11.5` V008/V009 and the V008/V009 closing paragraph now state that V008 creates and grants only `evidence_role`; V009 creates `sharing_role` first and only then grants it sharing/evidence schema USAGE. `T-S3-28` asserts clean-database role existence before every grant. Tables and waves are unchanged. |
| F-08 shareable evidence projection | `§2.12` defines the evidence-owner, same-connection, read-only collaborator `evidence.read_shareable_projection`, its exact redacted fields, artifact ordering and exact `content_snapshot.content`/`scopeDigest`/`content_sha256` construction. `§9.29`, `§10.21`, `§10.23`, `§3.2` and `§4.44` align; it exposes no media-object id/key/version/bucket/bytes/URL/crypto reference, writes nothing, takes no foreign row lock and adds no broad direct grant. `T-S3-15` and `T-S3-27` assert deterministic replay/conflict and privilege. Final wire encoding remains Q15. |
| F-09 playback risk | `§10.23` `sharing.authorize_media_request`, `§9.31` step 4 and `T-S3-26` now require re-reading the exact captured/current binding and `subject_risk_state` projection for playback, seek and renewal under the existing order; `OPEN` or absent/unresolvable returns `SHARE_RISK_BLOCKED`, issues no media credential/event and changes no count/generation; PostgreSQL locks are released before media/Redis I/O. |
| F-10 evidence identities | `§10.24` adds the bounded deterministic registry for `EVIDENCE_CREATED`, `EVIDENCE_SOLIDIFIED`, `EVIDENCE_QUARANTINED`, `EVIDENCE_FAILED`, `EVIDENCE_RECORDING_VALIDATED` and `EVIDENCE_RECORDING_QUARANTINED`, with fixed prefixes, lowercase SHA-256 over exact persisted immutable facts, 1-128-character identities and separate payload fingerprints. Changed payload under the same identity conflicts; same-semantic retry returns the first fact and appends nothing; no random ID/current timestamp is used. `§9.27`, `§9.28`, `§4.43`, `T-S3-29` and `T-S3-31` align; the existing terminal `RECORDING_CONFLICT` fact is preserved. |
| F-11 status | Header task/gate/status/date/controller lines, `§1.1`, `§1.5`, the `§13.1` embedded status assertion, `T-S2-65` and `§13.10` now state S3 accepted at full checkpoint `9ad33e8727596366c0a844b7dffcce3eb028891b` (short `9ad33e8` only where existing style uses it) and aggregate fix 1 pending independent aggregate re-review. No historical artifact was rewritten; aggregate/runtime acceptance is not claimed. |

## Accepted clarification dispositions

| Clarification | Disposition and current location |
| --- | --- |
| OQ-1 claim timestamp | `§4.14` `risk_checked_at` and `§9.6` steps 3/4 define the pre-risk fresh-rejection timestamp as the database claim-decision time with a typed audit `riskEvaluation = NOT_REQUIRED`; risk-evaluating paths store the actual current risk-decision time. `§9.23` adds the assertion. NOT NULL and append-only shape are preserved. |
| OQ-4 observational sharing events | `§2.12`, `§9.30` step 1, `§10.23` `sharing.record_access_event` and `T-S3-19` define deterministic `share-observation:` identities and payload fingerprints for `PAGE_OPENED`, `PREFETCH_IGNORED` and `VERIFICATION_FAILED` from grant ID, event type and a digest of a stable nonblank request/action key plus exact semantic fields. Raw keys/secrets are not stored; same-semantic retry returns the first event; changed content conflicts. No new action/table/column or final wire encoding. |
| OQ-5 reconstruction order | `§9.31` step 3, the `§10.23` ticket reconstruction service and `T-S3-25` now state that trusted recipient re-authentication and S1 ranks 7/8/9 precede any sharing grant read/lock, and all PostgreSQL locks are released before Redis/media I/O. |
| OQ-6 upload observation | `§4.40`, `§7.4`, `§9.27` steps 3-5 and the new `§10.23` `evidence.record_upload_observed` service define the evidence-owner `DECLARED → UPLOADED` transition with the exact already-registered object/version/hash observation before validation. Same object/version/hash retry is idempotent; changed identity conflicts and mutates nothing; only `UPLOADED` proceeds to validation/quarantine; no external I/O occurs under PG locks and no schema object is added. `T-S3-02/03/04/06/32` assert it. |
| OQ-2 / OQ-3 / OQ-7 | OQ-2 (`maskedCaller`) remains deferred to IVR/Subject-number/Backend / Q15/Q26 task API and exact response-projection design. OQ-3 (`download_allowed` enforcement) remains deferred to Product/Security/Sharing / Q15 media credential/API policy. OQ-7 is closed only by accepted F-08's evidence-owner read collaborator; final wire encoding remains Q15. |

## Changed files

- `docs/14-production-data-model-and-flyway-schema-spec.md` — modified. Current SHA256 `6767D10CBD59574C83B9BE5B358D8F0B4FBF0D6554B05879DC21AB1EA470DBC8`; 860667 bytes / 4867 lines.
- `docs/gateflow/a0-production-data-model-schema-spec-aggregate-fix-1.md` — this new artifact.
- Controller SHA256 remains `D0E0333265AACD5185F31B5F11F4135ACCE54E445C5CC96AA379B0C7D461FFE4`; aggregate review SHA256 remains `EBF57A4636BB6E5FEBE0E812B21F18104E6B71A0E81147C9F0F4D2660B9A1905`; accepted plan fix-6 SHA256 remains `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`.

## Validation commands and actual results

- `sha256sum /tmp/a0s1/structural-v2-run.sh` → `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545` (required hash).
- `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` → exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`.
- Exact read-only checker `C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s3-document.ps1` → exit `0`; `FrozenBlocks=9`, `Dictionaries=44`, `Columns=522`, `SliceColumns S1=121/S2=307/S3=94`, `Bytes=860667`, `SHA256=6767D10CBD59574C83B9BE5B358D8F0B4FBF0D6554B05879DC21AB1EA470DBC8`, `Errors=[]`.
- Ten required embedded blocks across `§13.1`, `§13.2`, `§13.2.1`, `§13.2.2`, `§13.5`, `§13.6` and `§13.8` were extracted unchanged after the intentional `§13.1` update and executed: all ten exit `0`; the dictionary sections report two blocks each (`ALL_EXACT`), and `§13.6` retains its `23/23/23/54` expected counts. Two additional `§13.3`/`§13.4` shell blocks also exit `0`; they are not counted in the required ten.
- Intentional embedded-block delta: the `§13.1` S3 status assertion changed from `grep -q 'S3 is \*\*author-complete / independent review pending\*\*'` to `grep -q 'S3 status is \*\*accepted at checkpoint `9ad33e8`\*\*'`; the S2 term loop gained `'operations-iam.lock_employee_administrator'` and the S3 term loop gained the new evidence-identity, `share-observation:`, `evidence.read_shareable_projection`, `evidence.record_upload_observed`, `UPLOAD_OBSERVATION_REQUIRED`, `EVIDENCE_NOT_SOLIDIFIED` and related terms. Structural purpose (status consistency, required-term presence, no trailing whitespace) is preserved and all ten blocks pass.
- Encoding/cleanliness: UTF-8 no BOM (first bytes `23 20 41`); CR count `0`; trailing-whitespace lines `0`; the specification ends in exactly one LF; `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` exits `0`.
- Scope: only the specification is modified and this artifact added. The controller, aggregate review, accepted plan/fix-6, historical artifacts, checkers, source, prototypes/screenshots and Git metadata are unchanged. Pre-existing dirty screenshots and the old untracked plan review remain untouched.

## Docs decision

- Structural/docs validation passes for the frozen 44/44/9/3 shape, 522 columns, nine frozen blocks, ten required embedded blocks, encoding and two-path scope. This is not runtime acceptance.
- Semantic readiness is author-reported only. The 11 accepted findings and four accepted clarifications are current-text corrected, but independent aggregate re-review must decide whether they are exact and whether any regression remains.
- No aggregate acceptance, accepted deepreview commit, push, PR, merge, full A0 completion or production readiness is claimed.

## Residual risks and owners/destinations

| Residual | Classification | Owner / destination |
| --- | --- | --- |
| Executable PostgreSQL/Flyway migration, role provisioning, privileges, trigger/deferred-constraint syntax, row-lock/savepoint/READ COMMITTED concurrency and category-1/2/3 behavior | later-work-unit; not executed here | Backend/Database/Platform/Security + Testcontainers; implementation/migration/Testcontainers gate |
| COS object version/hash validation, KMS/envelope, WORM/object-lock and media/malware/duration probing | later-work-unit; not executed | Infrastructure/Security/Backend; storage/security/capacity work (Q2/Q11) |
| Redis ticket TTL/cache loss/revocation propagation/clock skew | later-work-unit; not executed | Security/Sharing/Backend; Q22/security design |
| OTP/SMS proof, KDF parameters, admission limits/expiry defaults, rate limits | later-work-unit; not executed | Security/Product/Sharing; Q12/security+sharing policy |
| Wire/OpenAPI encoding, IVR protocol mapping, JSON-schema version and final content-member encoding | later-work-unit; not executed | Backend/IVR/Security; Q15/Q26 |
| Retention/erasure/backup deletion/legal hold | later-work-unit; not executed | Legal/Security; Q3/Q11/Q13 |
| IVR-local B-leg unique operation/lost-response recovery | later-work-unit; not executed | Telephony/IVR; Q28 |
| Production prices/quantities/trial wording, due-date/grace, tolerance, allocation policy, task-access role matrix and call-blocking risk | later-work-unit; not executed | Product/Finance/Operations/Risk/IVR; Q4/Q5/Q8/Q10/Q20/Q24/Q25/Q27 |
| Deployment role provisioning and whether the deployment identity may create `SECURITY DEFINER` functions/triggers | later-work-unit; not executed | Platform/Database/Security; Q17/Q23 |

No unclassified residual risk or external issue remains for the fixed-current findings. These risks are not defects proven by this fix and are not claimed as implemented, tested or production-ready.

## Limitations and stop status

- No PostgreSQL 16, Flyway, DDL, role/privilege, trigger, deferrable constraint, row lock, savepoint, READ COMMITTED concurrency, COS/KMS/WORM, Redis/media gateway, IVR line, payment provider, Testcontainers or production execution occurred.
- Structural/embedded checks prove document structure, membership, frozen equality, required-term presence and cleanliness only; they do not prove database, transaction, privilege or external-system behavior.
- No controller, accepted plan, historical artifact, checker, source, prototype, screenshot or Git metadata was modified. No stage, commit, push, PR, merge, issue modification, install, network write, credential/provider/WorkBuddy change or later gate was performed.
- Stopped after this bounded fix artifact and validation report. Independent aggregate re-review is mandatory before any aggregate acceptance or remote delivery.
