# A0 S3 Fix 1 Artifact

- Task label: `a0-spec-s3-fix1-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate / slice: `implementation`, S3 evidence/media/sharing closure (V008-V009), bounded fix 1
- Runtime / provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Status: S3 fix 1 is author-complete; independent S3 re-review is pending. S1 and S2 remain accepted. No S3 acceptance, full A0 completion, production implementation, runtime execution or later gate is claimed.
- Binding inputs: controller final S3 adjudication in `docs/gateflow/project-execution-control.md`; independent review `docs/reviews/code-review-20261022-222928.md` (SHA256 `2F8B98CB77759B40B959B0A47E3C850E6568AB44DE7BAC9A91FD893D5FA87EFA`); accepted plan SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; accepted plan fix-6 SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`.
- Pre-fix cumulative specification recorded by the controller: SHA256 `957E2C753291B56061484A76EFE1BF3A7F0A2C1F72AA2269D80D3B861CA82D11`, 792328 bytes / 4817 lines.
- Post-fix cumulative specification: SHA256 `4EEBC41AC51AD20DF91F053A703A51AB5E1628B03A556A3D20BD2ED641F61782`, 820924 bytes / 4822 lines.

## 1. Scope and exact write boundary

This is a documentation-only bounded fix of the final approved S3 slice. It is not a controller or reviewer artifact.

Writable paths for this task were exactly:

1. `docs/14-production-data-model-and-flyway-schema-spec.md` (cumulative specification).
2. `docs/gateflow/a0-production-data-model-schema-spec-s3-fix-1.md` (this new artifact; confirmed absent before the first edit).

All specification edits were applied through the provided Windows `--codex-run-as-apply-patch` executable. No `Edit`/`Write`/Python/shell-write fallback was used for local repository files. No control document, accepted plan, plan fix-6, historical review/fix artifact, checker/script, source code, API/OpenAPI contract, UI, prototype, screenshot, `logo/preview.html`, Git metadata, credential, provider configuration or WorkBuddy file was written. No stage, commit, push, PR, merge, issue, install or provider change was made. The old untracked plan review and the pre-existing dirty screenshots remain untouched.

Scope is the code-generation-ready PostgreSQL 16 / Flyway specification only. No executable SQL, Flyway migration, runtime service, API, UI, cloud resource or production policy is produced or claimed. No new table, column, SQL function, function-wave row, action, slice or business policy is introduced.

## 2. Changed paths

| Path | State | Note |
| --- | --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | modified | Incremental S3 fix-1 edits on top of the pre-existing uncommitted S3 author content; all S1/S2 accepted content and all frozen blocks preserved. |
| `docs/gateflow/a0-production-data-model-schema-spec-s3-fix-1.md` | new | This artifact. |

No other path was written by this task. Pre-existing dirty control/prototype/screenshot/logo/old-review paths were not touched.

## 3. Correction matrix (14 current corrections)

All 14 rows below are classified `fixed-current` and are `author-complete; independent S3 re-review pending`. They are not assigned-later-work-unit items and are not external/tracked-existing issues. F4-01 is handled separately in section 4.

| ID | Class / severity | Locations changed | Change | Verification | Status |
| --- | --- | --- | --- | --- | --- |
| S3-01 | Finding / high | §2.12, §4.45, §4.46 witness text, §7.4, §9.30, §10.23, T-S3-18, T-S3-20, T-S3-21, T-S3-22 | Existing `(share_grant_id, admission_key)` is resolved after current status/time/generation/authorization checks but before any `max_admissions` test. An existing unexpired admission returns the original session and reissues the ticket without a cap test, increment or second `ADMISSION_GRANTED`; an expired session returns the original fact with `SHARE_SESSION_EXPIRED` and no ticket. The cap applies only to a new key. `LOCKED` permits existing replay/reconstruction but denies a new admission. | Re-read §4.45/§7.4/§9.30 ordering; T-S3-18 now covers last-slot admitted plus lost response/Redis/cache at max (count = 1), expired same key (no ticket), new key cap refusal, and exact one `ADMISSION_GRANTED`; T-S3-20/21/22 cover last-session usability, expiry and races. | fixed-current; author-complete; independent re-review pending |
| S3-02 | Finding / high | §2.12, §4.45, §4.46 check, §7.4, §9.0.2, §9.30, §9.31, §10.21, §10.23, T-S3-17, T-S3-19, T-S3-22, T-S3-25, T-S3-27 | `ADMISSION_GRANTED` now records `verificationMode` and, for `ORGANIZATION_MEMBER`, the original trusted `recipientAccountId`/`recipientOrganizationId` pair. Reconstruction, renewal, playback/seek authorization and admission replay re-authenticate the matching recipient and current active organization/membership/account under S1 ranks 7/8/9 before the owner-local grant read/lock; changed account/tenant, revoked/suspended member or disabled account fails closed. The witness carries no secret or PII. PostgreSQL locks/transactions close before Redis/media I/O. | Re-read §9.0.2 rank 2, §9.30 step 3, §9.31 steps 3-4 and the §4.46 admission-facts check; T-S3-17/19/22/25 assert revocation, suspension, tenant change, `LOCKED` existing-session replay and serial revoke/reconstruction windows with the Q22 credential bound. | fixed-current; author-complete; independent re-review pending |
| S3-03 | Finding / medium | §4.44 risk rule, §10.21 evidence_record projection, T-S3-15, T-S3-17, T-S3-27 | `sharing_role` receives only the existing redacted `evidence.evidence_record.identity_at_capture` column in addition to its prior exact projection. No broad/all-column grant, no media-object identity and no evidence write is added. | Re-read §10.21 row and risk rule; T-S3-15/17 cover captured binding different from current active binding, `OPEN` risk and absent/unresolvable projection fail-closed; T-S3-27 asserts the exact column and no broad grant. | fixed-current; author-complete; independent re-review pending |
| S3-04 | Finding / medium | §4.40, §4.41 late-result prose, §7.4, §9.27, §10.22, T-S3-04, T-S3-06, T-S3-29 | Only non-terminal `DECLARED`/`UPLOADED` may transition to `QUARANTINED`. After terminal `VALIDATED`/`QUARANTINED`, a conflicting callback returns stable `RECORDING_IDENTITY_CONFLICT` (identical replay returns the first outcome/code), changes no status/media/validation fact/revision, and appends exactly one deterministic redacted Outbox conflict fact (`EVIDENCE`/`RECORDING`/`RECORDING_CONFLICT`). The dedupe key is `recording-conflict:<conflictDigest>` over the bounded canonical conflict input; the same conflicting payload returns the existing Outbox ID and writes no second fact, while changed conflicting content creates one distinct fact. No audit row and no new table/column are added. | Re-read §4.40 conflict paragraph, §7.4 recording row, §9.27 steps 3/4/7 and the §10.22 guard; T-S3-04 asserts before-terminal quarantine and post-`VALIDATED` duplicate/conflicting callbacks with original facts byte-identical; T-S3-06/29 cover crash, late callback and redaction. | fixed-current; author-complete; independent re-review pending |
| S3-05 | Finding / medium | §4.41 call/snapshot/source-ref text, §9.28 steps 2-4, T-S3-14 | Evidence creation now explicitly requires `recording.call_id = evidence_record.call_id`, the trusted `call_record` session/subject links, the supplied inbound-session links and the supplied reservation `call_id`/`subject_id` equality. A cross-call/session/subject/reservation or required-null link is `EVIDENCE_LINK_MISMATCH` with no evidence record, creation event or artifact. The canonical snapshot `source` object is typed and `ck_evidence_snapshot_shape` enforces its link equality to the persisted columns; the deferred reciprocal creation cycle and immutable capture are unchanged. | Re-read §9.28 steps 2-4 and §4.41 checks/notes; T-S3-14 covers recording from call A with evidence for call B, session/subject/reservation crossings, missing parents, snapshot source shape and no fact. | fixed-current; author-complete; independent re-review pending |
| S3-06 | Finding / medium | §13.6 | S2 lifecycle/sensitive/revision ranges now end at `§4.39` and the S2 FK range at `§5.5`, so only `§4.16-§4.38` S2 coverage is counted. Expected counts remain 23/23/23/54 and no checker is weakened. | Exact §13.6 block now exits `0` (pre-fix it exited `1` with `S2 LIFECYCLE COVERAGE FAILED`); measured counts are 23/23/23/54; all other embedded blocks still exit `0`. | fixed-current; author-complete; independent re-review pending |
| S3-07 | Finding / low | §3.1, §10.21, §11.5 V009 step 6, T-S3-27 | The S3 role summary now matches the grant matrix: `evidence_role`/`sharing_role` receive `EXECUTE` only on `platform.outbox_append` and `audit.append_fact`, no platform idempotency functions, and the exact `subject_number` schema `USAGE` for their declared subject/ownership/binding/risk reads. V009 grants are aligned. | Re-read §3.1/§10.21/§11.5; T-S3-27 asserts positive reads/two functions, forbidden idempotency execution, forbidden sharing grants for `evidence_role` and the exact schema USAGE. | fixed-current; author-complete; independent re-review pending |
| S3-08 | Finding / low | §4.45, §9.30, §10.21, T-S3-18, T-S3-27 | The grant-row `FOR UPDATE` lock and admission count are explicitly owned by the sharing owner service, not the evidence owner. `evidence_role` receives no sharing-row privilege and takes no sharing lock. | Re-read §4.45/§9.30/§10.21; T-S3-27 asserts the sharing-owner lock and no `evidence_role` sharing access. | fixed-current; author-complete; independent re-review pending |
| OQ-1 | Question / clarification | §2.12, §4.46, §7.4, §9.30 step 5, §9.31 step 1, T-S3-18, T-S3-21 | `admissionExpiresAt` is the grant `expires_at` frozen at admission; `sessionExpiresAt = min(admitted_at + approved Q22 ticket TTL, admissionExpiresAt)`. No extra TTL or column is added; a grant extension never lengthens an existing admission/session; absent/unapproved Q22 TTL fails closed for a new admission. Existing replay uses the recorded witness. | Re-read §2.12/§9.30/§9.31 and the §4.46 witness check; T-S3-18/21 assert bounded expiry, extension non-lengthening and no ticket after session expiry. | fixed-current; author-complete; independent re-review pending |
| OQ-2 | Question / clarification | §2.12, §4.44 snapshot check/creation text, §9.29 steps 1/4, §10.23, T-S3-15, T-S3-16 | `share_id` depends only on the trusted creator account and stable `creationRequestId` (`share:` + lowercase SHA-256, 70 characters). The typed redacted `content_snapshot.creation` freezes `{schemaVersion, creatorAccountId, ownerOrganizationId, evidenceId, verificationMode, downloadAllowed, expiresAt, maxAdmissions, scopeDigest, creationRequestDigest, creationFingerprint}`; `scopeDigest` hashes the frozen content/verification scope and the fingerprint covers the initial request/scope parameters. A retry compares the fingerprint before state-dependent mutation: same identity/fingerprint returns metadata only, changed scope is `SHARE_CREATION_CONFLICT`, and a replacement requires a new `creationRequestId`. No table/column/platform idempotency function and no raw creation request ID or secret is stored. | Re-read §2.12 and §4.44 check; T-S3-15 covers lost-response retry, changed scope, new-request replacement and captured-binding risk; T-S3-16 covers no reissuance. | fixed-current; author-complete; independent re-review pending |
| OQ-3 | Question / clarification | §2.12, §4.44, §9.29 step 3, §9.30 step 2, §10.23, T-S3-17 | `DESIGNATED_MOBILE` now requires trusted SMS possession proof for the exact normalized phone HMAC and the intended share/action, consistent with `docs/07-web-capability-hub-spec.md:141`. HMAC comparison or knowing the number alone is insufficient; OTP lifecycle/parameters remain Security/Q12; absent approved verification input fails closed. Only the HMAC/key version is stored; no OTP service/table or false operator real-name proof is added. | Re-read §2.12/§4.44/§9.29/§9.30; T-S3-17 asserts trusted-proof admission, knowledge/HMAC-only denial and absent-input fail-closed. | fixed-current; author-complete; independent re-review pending |
| OQ-4 | Question / clarification | §4.44, §7.4, §9.29 step 6, §10.22, T-S3-22 | `LOCKED → REVOKED` and `LOCKED → EXPIRED` are allowed through the existing owner revoke/expiry transitions and remain terminal. `LOCKED → ACTIVE` remains only through extraction-code rotation; no non-code unlock action/policy is invented. | Re-read §4.44/§7.4/§9.29/§10.22; T-S3-22 exercises the allowed `LOCKED` exits and rejects the invented non-code unlock. | fixed-current; author-complete; independent re-review pending |
| OQ-5 | Question / clarification | §2.12, §4.44, §9.29 steps 3/4, §10.23, T-S3-15, T-S3-16 | The random capability plaintext is returned once; only its digest is retained. A lost creation response retry returns metadata only with no new token; a usable replacement requires a new `creationRequestId`/grant/token, and the unusable prior grant may be revoked through the existing owner transition. No token reconstruction, reissuance action or recoverable plaintext secret exists. | Re-read §2.12/§4.44/§9.29/§10.23; T-S3-15/16 assert metadata-only retry, no reissuance, replacement identity and digest-only storage. | fixed-current; author-complete; independent re-review pending |
| OQ-6 | Question / clarification | §2.12, §4.44, §7.4, §9.29 step 5, §10.22, §10.23, T-S3-16, T-S3-22 | Revoke/code-rotate/expire/extend/lock use deterministic `SHARE_ACCESS` event identity `share-transition:` + lowercase SHA-256 of canonical `{shareGrantId, actionType, sourceIdentity}` (81 characters) and an `actionFingerprint`. `sourceIdentity` is a stable client action request for client actions, or the recorded expiry / pre-update source revision for system expiry/lock; it never depends on the post-update current revision or generated code/salt. Same action/fingerprint returns the first recorded outcome before current status/revision preconditions; changed content conflicts; different actions are distinct. No new action/table/function or wire contract is added. | Re-read §2.12 transition identities, §4.44 state transitions and §9.29 step 5; T-S3-16/22 assert deterministic retry-before-current-state, changed-content conflict, distinct actions and `LOCKED` exits. | fixed-current; author-complete; independent re-review pending |

### 3.1 High-severity semantic trace

- Admission ordering now reads: current status/time/generation/authorization checks, then existing `(share_grant_id, admission_key)` replay, then the new-key limit/count insert. The same key cannot be converted into a new admission by a lost response, Redis loss or a fresh verification attempt; the limit never denies recovery of the last admitted unexpired session, and an expired original never renews.
- Reconstruction/renewal read the fixed original witness and current policy facts. For `ORGANIZATION_MEMBER` the authenticated recipient must still match the original account/organization pair and the current active organization/membership/account before the grant read/lock; a removal, suspension, tenant change or disabled account denies. `NONE` remains bearer-only and is never recorded as verified identity. Generation, risk, session and admission expiry checks remain.
- Revocation before the reconstruction read governs; a ticket returned before a later revoke is bounded only by the approved Q22 credential TTL, and already-issued bytes are not claimed recalled. PostgreSQL locks are not held across Redis, COS or media-worker I/O.

## 4. F4-01 disposition

F4-01 is `已修复` by the prior S3 author and independently confirmed closed in `docs/reviews/code-review-20261022-222928.md`. This fix preserves the current header, §13.1 and T-S2-65 alignment: S1 accepted at `5404d9c`, S2 accepted at `8934df9` after independent review, and S3 author-complete / independent review pending. Historical artifacts were not rewritten. The current task label is `a0-spec-s3-fix1-ds-20261022-01`; the S3 implementation label remains historical.

## 5. Preserved invariants and nonregression

- Nine frozen blocks byte-for-byte; 44 dictionaries / 522 columns (S1 13/121, S2 23/307, S3 8/94); 40 invariants; 9 waves; slices 13/23/8; 81 FK decisions (74/3/2/2); lifecycle 28/16; sensitive 28; 24 key sets; four V001 plus one V006 function-wave rows.
- No new table, column, function, action, slice or policy. S3 remains eight V008-V009 tables with 94 columns and no SQL function.
- INV-15/24/25/26/39 and the reciprocal `EVIDENCE_CREATED` constraints, both insertion orders, partial unique creation key and deferred triggers are preserved.
- S1/S2 accepted semantics are preserved: global order caller 1 -> sorted canonical number 2 -> subject 3 -> owned session/current call/activation 4 -> subject owner 5 -> binding/risk 6 -> organization 7 -> membership 8 -> account 9 -> idempotency 10 -> task 11 -> balance/reservation/usage 12 -> employee 13 -> Outbox 14 -> audit 15; owner-only writes; no foreign `FOR UPDATE`; PostgreSQL commit before Redis; no PostgreSQL locks across network I/O; S1 rollback and S2 savepoint categories; 30-second configurable TTL/auto-dial; permanent `FIRST-TRIAL`; `UNKNOWN` occupancy; both direct-authorization chains; unregistered IVR trial/evidence; no capture rewrite.
- The specification remains document-only and does not claim PostgreSQL, Flyway, privilege, trigger, row-lock, savepoint, concurrency, COS, KMS, Redis, media-gateway, IVR or production execution.

## 6. Validation commands and actual exits

All commands were read-only from `/mnt/d/codex-call` except the apply_patch writes to the two allowed paths. The external structural script and read-only checker were not modified.

| Check | Actual result |
| --- | --- |
| `sha256sum /tmp/a0s1/structural-v2-run.sh` | `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545` (expected; unchanged) |
| `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` | exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24` |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File 'C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s3-document.ps1'` | exit `0`; `FrozenBlocks=9`, `Dictionaries=44`, `Columns=522`, `SliceColumns={S1:121,S3:94,S2:307}`, `Bytes=820924`, `SHA256=4EEBC41AC51AD20DF91F053A703A51AB5E1628B03A556A3D20BD2ED641F61782`, `Errors=[]` |
| Exact §13.1 block, unchanged | exit `0`; no missing section/term; trailing-whitespace check passes |
| Exact §13.2/§13.2.1/§13.2.2 blocks, unchanged | exit `0`; exact-name comparisons retain 13/23/8 and S3 94-column coverage |
| Exact §13.5 block, unchanged | exit `0` |
| Exact §13.6 block, fixed ranges | exit `0`; measured S2 lifecycle/sensitive/revision `23/23/23` and S2 FK `54`; pre-fix block exited `1` with `S2 LIFECYCLE COVERAGE FAILED` |
| Exact §13.8 block, unchanged | exit `0` |
| `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` | exit `0` |
| `git diff --no-index --check /dev/null docs/gateflow/a0-production-data-model-schema-spec-s3-fix-1.md` | exit `1` with zero diagnostic bytes (expected whole-new-file comparison status; no whitespace diagnostic) |
| `sha256sum` of accepted plan / plan fix-6 | plan `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; fix-6 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59` (both unchanged) |
| Specification encoding and EOF | UTF-8 without BOM; byte count `820924`; line count `4822`; CR count `0`; trailing-whitespace count `0`; final byte `0a` (one terminal LF) |
| Scope/status | This task wrote only the specification and this new artifact. Pre-existing dirty control/prototype/screenshot/logo/old-review paths remain untouched. No `.sql` or `db/migration/` path was created. |

Pre-fix failure evidence is reproducible from the controller/review record: the unchanged §13.6 block exited `1` with `S2 LIFECYCLE COVERAGE FAILED` against the pre-fix specification. The fixed block with the bounded range endpoints now exits `0` without weakening expected counts. No external checker was changed.

The specification hash is computed after all fix-1 edits. This artifact cannot contain its own SHA256; the artifact hash is reported in the completion response.

## 7. Residual risk ownership and destination

The 14 current corrections are `fixed-current` and pending independent S3 re-review. They are not assigned-later-work-unit findings and no external issue exists for them. Actual runtime/cloud/policy/wire risks retain named owners/destinations:

| Risk | Classification | Owner / destination |
| --- | --- | --- |
| S3-01..S3-08 and OQ-1..OQ-6 | fixed-current, pending independent review | S3 author/controller; independent S3 re-review before any S3 acceptance. |
| F4-01 current status integration | fixed-current | Closed by the prior S3 author and confirmed in the independent review; no historical rewrite. |
| PostgreSQL/Flyway/privilege/trigger/lock/savepoint/concurrency execution | assigned-later-work-unit | Backend/Database; implementation migration and Testcontainers suite. |
| COS object/version/WORM/KMS and media validation | assigned-later-work-unit | Infrastructure/Security/Backend; Q2/Q11 storage/security/capacity work. |
| Redis ticket/cache/revocation-window behavior | assigned-later-work-unit | Security/Sharing/Backend; Q22 and media-gateway integration. |
| OTP lifecycle, KDF parameters, admission/expiry/rate-limit and access-role policy | requiring-userdecision | Security/Product/Sharing; Q12/Q22 and the named sharing policy/authorization design. |
| Wire/event encodings and OpenAPI contract | assigned-later-work-unit | Backend/Security; Q15/Q26. |
| Deployment role provisioning and trigger/function ownership | assigned-later-work-unit | Platform/Database/Security; Q17/Q23. |
| Legal retention/erasure/backup deletion | requiring-userdecision | Legal/Security; Q3. |
| No later approved slice after S3 | no-later-approved-slice residual | S3 is the final approved slice; this fix does not create or promise a fourth slice. |

These classifications concern ownership and destination, not acceptance. No runtime or cloud behavior is guaranteed by this document.

## 8. Docs decision, limitations and completion

- Docs decision: only `docs/14-production-data-model-and-flyway-schema-spec.md` and this new artifact were written. The specification differential is the bounded fix on top of the pre-existing uncommitted S3 author content. The controller, accepted plan/fix-6, historical S3 implementation artifact, prior reviews/fixes, checkers/scripts, source/config, prototypes, screenshots, `logo/preview.html` and Git metadata remain read-only.
- Limitations: document checks prove structure, counts, exact dictionary coverage, embedded shell checks, whitespace and encoding only. No PostgreSQL, Flyway, privilege, trigger, lock, savepoint, concurrency, COS, KMS, Redis, media-gateway, IVR, payment or production test was executed. The full repository manifest was not recomputed; the two write-boundary paths and pre-existing dirty-path preservation were checked.
- Completion: all 14 current corrections are authored and marked fixed-current pending independent S3 re-review. The structural script, read-only S3 checker, all relevant embedded §13 blocks, exact-path whitespace check, encoding/EOF checks and scope checks pass with the exits recorded above. The fix stops here.
- No S3 acceptance, S3 checkpoint/commit, full A0 completion, production readiness, later gate or other work unit is claimed. No commit, push, PR, merge, issue, install or provider change was made.
