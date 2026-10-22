# A0 S3 Fix 2 Artifact

- Task label: `a0-spec-s3-fix2-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate / slice: `fix`, S3 evidence/media/sharing closure (V008-V009), bounded fix 2
- Runtime / provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Status: S3 fix 2 is author-complete; independent S3 re-review is pending. S1 and S2 remain accepted. No S3 acceptance, accepted checkpoint, full A0 completion, production implementation, runtime execution or later gate is claimed.
- Binding inputs: controller adjudication `S3 fix 1 independent re-review and fix 2 adjudication` in `docs/gateflow/project-execution-control.md`; independent review `docs/reviews/code-review-20261022-231015.md` (SHA256 `1E81748AC1E24AB8261EB64165B780042DC1FD28450BE056C27BF682E6F9AE2A`); accepted S1 checkpoint `5404d9caa73b72c412ca2065bef19dfb46d154d3`; accepted S2 checkpoint/current HEAD `8934df90c15dcd8173486c6ad753ab27c91ac765`.
- Pre-fix cumulative specification: SHA256 `4EEBC41AC51AD20DF91F053A703A51AB5E1628B03A556A3D20BD2ED641F61782`, 820924 bytes / 4822 lines.
- Post-fix cumulative specification: SHA256 `CACE2720EF166C0EBBFCA0B95EEED1392CB356B5A81F2B010AEB2CA42750D4D6`, 822025 bytes / 4822 lines.

## 1. Scope and exact write boundary

This is a documentation-only bounded correction of the final approved S3 slice. It is not a controller or reviewer artifact.

Writable paths for this task were exactly:

1. `docs/14-production-data-model-and-flyway-schema-spec.md` (cumulative specification).
2. `docs/gateflow/a0-production-data-model-schema-spec-s3-fix-2.md` (this new artifact; confirmed absent before the first edit).

No control document, accepted plan or plan fix-6, historical review/fix/implementation artifact, checker/script, frozen block, expected count, executable SQL, migration, service, API, UI, prototype, screenshot, `logo/preview.html`, Git metadata, credential, provider configuration or WorkBuddy file was written. Repository edits were applied through the harness's patch-style Edit/Write tools; no shell redirection, sed/awk rewrite or historical-file probe was used. No stage, commit, push, PR, merge, issue, install or provider change was made. Pre-existing dirty files were preserved.

Scope is the code-generation-ready PostgreSQL 16 / Flyway specification only. No new table, column, SQL function, function-wave row, action, event type, role, migration wave, slice, policy value, wire shape or secret representation is introduced.

## 2. Changed paths

| Path | State | Note |
| --- | --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | modified | Four bounded S3 edits on top of the pre-existing uncommitted S3 author/fix content; accepted S1/S2 content and all frozen blocks preserved. |
| `docs/gateflow/a0-production-data-model-schema-spec-s3-fix-2.md` | new | This artifact. |

No other path was written by this task.

## 3. OQ-2/OQ-6 finding disposition

Finding: `docs/reviews/code-review-20261022-231015.md` OQ-2/OQ-6-部分修复-[中]. The creation and EXTEND action fingerprint field lists named the approved business inputs `expiresAt` and `newExpiresAt`, but the same clauses then excluded broad `timestamps` / `all timestamps`. Under that literal wording a generator could omit the requested validity input, so the same creation identity with changed expiry content, or the same EXTEND action identity with a changed extension target, would recompute the frozen fingerprint and replay the first outcome instead of returning a stable conflict.

Disposition: `fixed-current`; held for independent re-review. No external issue and no later approved slice exist for it.

### 3.1 Before and after semantics

- Before: `creationFingerprint` listed `expiresAt` but excluded `timestamps`; EXTEND `actionFingerprint` listed `newExpiresAt` but excluded `all timestamps`. Because the existing grant/event is resolved by its deterministic identity before mutable checks, a literal generator could make changed expiry or extension content replay the first outcome.
- After: both exclusions are narrowed to only generated or observed transport, network, persistence or server-decision timestamps (for example `createdAt`, `updatedAt`, `occurredAt`, `decidedAt` and equivalent server times). The approved business input `expiresAt` remains included in `creationFingerprint`; the approved business input `newExpiresAt` remains included in the EXTEND `actionFingerprint`; the recorded `expiresAt` EXPIRE identity fact is likewise never excluded. Same identity with changed `expiresAt` returns `SHARE_CREATION_CONFLICT` and mutates nothing. Same EXTEND action identity with changed `newExpiresAt` returns a stable conflict and mutates nothing.
- Unchanged ordering and authority: the deterministic creation/event identity is still resolved first; the fingerprint comparison still occurs before any state-dependent mutation; a same identity plus same fingerprint still returns the first recorded grant metadata or first transition outcome; digest-only secrets, no token reconstruction, and all schema/ownership boundaries are preserved.

### 3.2 Exact locations changed

| Location | Before | After |
| --- | --- | --- |
| §2.12:248 (`actionFingerprint`) | Field list included `newExpiresAt (EXTEND only, null-safe)`; exclusion said `all timestamps`. | Exclusion narrowed to generated/observed transport/network/persistence/server-decision timestamps; `newExpiresAt` (EXTEND) and the recorded `expiresAt` EXPIRE fact are explicitly never excluded; same EXTEND identity with changed `newExpiresAt` is a stable conflict with no mutation. |
| §2.12:250 (`creationFingerprint`) | Field list included `expiresAt (RFC 3339 UTC)`; exclusion said `timestamps`. | Exclusion narrowed to the same generated/observed timestamp set; approved business input `expiresAt` is explicitly part of the hashed semantic fields; same creation identity with changed `expiresAt` returns `SHARE_CREATION_CONFLICT` and mutates nothing. |
| §13.9:4675 (`T-S3-15`) | Replayed the same `creationRequestId` with the same and changed scope only. | Adds a changed-`expiresAt` retry and asserts `SHARE_CREATION_CONFLICT` with no new grant/token/event and no other mutation. |
| §13.9:4676 (`T-S3-16`) | Retried transitions with the same and changed action content only. | Adds a changed EXTEND `newExpiresAt` case and asserts stable conflict with no status/revision/generation change, no second event and no other mutation. |

Directly affected service-sequence wording was inspected and already expressed the required behavior; it was not changed: §9.29 step 1 ("changed scope/parameters returns stable `SHARE_CREATION_CONFLICT` with no mutation") and step 5 ("If it exists with a changed fingerprint, return a stable conflict and mutate nothing"). The §2.5 canonicalization/global convention was inspected and left unchanged to avoid altering accepted S1/S2 content; the two S3 fingerprint clauses now define their own exact timestamp set and explicit business-input inclusions, removing the literal ambiguity.

## 4. Preserved closure and nonregression

- S3-01 through S3-08 remain independently closed on the current text; OQ-1, OQ-3, OQ-4 and OQ-5 remain independently closed; F4-01 remains closed. OQ-2/OQ-6 remain unaccepted pending the next independent re-review.
- Nine frozen blocks unchanged; 44 dictionaries / 522 columns (S1 121, S2 307, S3 94); 40 invariants; 9 waves; slices 13/23/8; 81 FK decisions (74/3/2/2); lifecycle 28/16; 28 sensitive tables; 24 key sets; four V001 plus one V006 function-wave rows; S3 eight V008-V009 tables and no SQL function.
- Accepted S1/S2 semantics, ownership and lock order, the reciprocal `EVIDENCE_CREATED` cycle, first-result authority, retry-before-mutable-state ordering, digest-only capability/code storage, no token reconstruction, no PostgreSQL lock across Redis/COS/media I/O, and all schema/role boundaries are unchanged.

## 5. Validation commands and actual results

All commands were read-only from `/mnt/d/codex-call` except the two allowed path writes. No checker, frozen block, expected count or structural script was edited.

| Check | Actual result |
| --- | --- |
| `sha256sum /tmp/a0s1/structural-v2-run.sh` | `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545` (expected; unchanged) |
| `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` | exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24` |
| Exact read-only checker via `powershell.exe -NoProfile -ExecutionPolicy Bypass -File 'C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s3-document.ps1'` | exit `0`; `FrozenBlocks=9`, `Dictionaries=44`, `Columns=522`, `SliceColumns={S1:121,S3:94,S2:307}`, `Bytes=822025`, `SHA256=CACE2720EF166C0EBBFCA0B95EEED1392CB356B5A81F2B010AEB2CA42750D4D6`, `Errors=[]` |
| All ten original embedded bash blocks across §13.1, §13.2, §13.2.1, §13.2.2, §13.5, §13.6, §13.8 (unchanged) | 10/10 exit `0`; §13.2/§13.2.1/§13.2.2 each contain two original blocks and print exact-name results with `ALL_EXACT` (13/23/8 tables; S3 counts 11,15,17,5,8,20,7,11 = 94). No block, checker or expected assertion was changed. |
| §13.6 measured counts | lifecycle `23`, sensitive `23`, revision/append-only `23`, S2 FK detail `54` |
| `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` | exit `0` |
| `git diff --no-index --check /dev/null docs/gateflow/a0-production-data-model-schema-spec-s3-fix-2.md` | exit `1` with zero diagnostic bytes (expected whole-new-file comparison status; no whitespace diagnostic) |
| Specification encoding and EOF | UTF-8 valid; no BOM; CR count `0`; trailing-whitespace line count `0`; single terminal LF (`final_bytes=640a`); 822025 bytes / 4822 lines |
| Fix artifact encoding and EOF | UTF-8 valid; no BOM; CR count `0`; trailing-whitespace line count `0`; single terminal LF |
| Task-owned scope | `M docs/14-production-data-model-and-flyway-schema-spec.md` and `?? docs/gateflow/a0-production-data-model-schema-spec-s3-fix-2.md` only; no `.sql` or `db/migration/` path; pre-existing dirty control/prototype/screenshot/logo/old-review paths were not written |
| Accepted plan / plan fix-6 hashes | plan `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; fix-6 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59` (unchanged) |

Limitations: these are static document checks only. They prove structure, counts, exact dictionary order, embedded assertions, whitespace and encoding, not executable SQL behavior, concurrency or runtime security. The full repository path manifest was not independently recomputed; the two write-boundary paths and preservation of pre-existing dirty paths were checked.

## 6. Docs decision

Only the cumulative specification and this new fix-2 artifact were written. The specification change is the four bounded normative edits in section 3.2. The controller, accepted S1/S2 checkpoints, accepted plan/fix-6, historical reviews/fixes, checkers/scripts, source/config, prototypes, screenshots, `logo/preview.html` and Git metadata remain read-only. No stage, commit, push, PR, merge, issue, install or provider change was made.

## 7. Residual risks and destinations

| Risk | Classification | Owner / destination |
| --- | --- | --- |
| OQ-2/OQ-6 expiry-fingerprint clarification | fixed-current, pending independent re-review | S3 author/controller; next independent S3 re-review. No external issue exists. |
| System/owner LOCK identity uses the pre-update `sourceRevision` | accepted clarification residual | Security/Sharing; Q12/sharing policy and the executable transition-retry test. |
| `content_snapshot.content` exact artifact-role/media-fact members and `scopeDigest` reproducibility | assigned-later-work-unit | Sharing/Backend; Q15/wire and the implementation contract. No schema change is authorized. |
| PostgreSQL/Flyway/privilege/trigger/lock/savepoint/concurrency execution | assigned-later-work-unit | Backend/Database; implementation migration and Testcontainers suite. |
| COS object/version/WORM/KMS and media validation | assigned-later-work-unit | Infrastructure/Security/Backend; Q2/Q11 storage/security/capacity work. |
| Redis ticket/cache/revocation-window behavior | assigned-later-work-unit | Security/Sharing/Backend; Q22 and media-gateway integration. |
| OTP lifecycle, KDF parameters, admission/expiry/rate-limit and access-role policy | requiring-userdecision | Security/Product/Sharing; Q12/Q22 and the named sharing policy/authorization design. |
| Wire/event encodings and OpenAPI contract | assigned-later-work-unit | Backend/Security; Q15/Q26. |
| Deployment role provisioning and trigger/function ownership | assigned-later-work-unit | Platform/Database/Security; Q17/Q23. |
| Legal retention/erasure/backup deletion | requiring-userdecision | Legal/Security; Q3. |
| No later approved slice after S3 | no-later-approved-slice residual | S3 is the final approved slice; this fix creates no fourth slice. |
| S3-01..S3-08, OQ-1/OQ-3/OQ-4/OQ-5, F4-01 | fixed-current and independently closed | S3 author/controller; preserved unchanged by this fix. |

These classifications concern ownership and destination, not acceptance.

## 8. Runtime limitation and completion

No PostgreSQL, Flyway, privilege, trigger, row-lock, savepoint, concurrency, COS, KMS, WORM, Redis, media-gateway, IVR or production test was executed. No runtime, cloud, security or production-readiness result is claimed.

Completion status: bounded S3 fix 2 is author-complete. The OQ-2/OQ-6 finding is corrected in the current specification and the two affected tests now assert changed-expiry and changed-extension-target conflict with no mutation. Validation passes with the exits recorded above. The task stops after this artifact and validation report; it does not enter re-review or any later gate.

OQ-2/OQ-6 remain unaccepted until the next independent re-review. No S3 acceptance, accepted checkpoint/commit, full A0 completion, production readiness, later gate or other work unit is claimed.
