# A0 S2 fix 3 artifact (fix-2 re-review corrections, specification only)

- Task label: `a0-spec-s2-fix3-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix`, bounded S2 fix 3 for the fix-2 re-review purchase-consent identity finding, review Open Questions 1-5 and controller clarifications C-01/C-02
- Runtime/provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Binding checkpoint: S1 accepted at `5404d9caa73b72c412ca2065bef19dfb46d154d3`. S2 is **fix-3 author-complete / independent re-review pending**; this is not S2 acceptance, not an A0 checkpoint and not production/runtime completion.
- Binding inputs: accepted plan SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; plan fix-6 SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`; fix-2 re-review `docs/reviews/code-review-20261022-205538.md` SHA256 `D43B955A096E1C3A52BE2F5041727B9A6032628FDCA4CB1207F292CB900F6C65`; binding adjudication section `S2 fix 2 independent re-review and fix 3 adjudication` in `docs/gateflow/project-execution-control.md`; prior fix-2 artifact `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-2.md` SHA256 `0250B5A25AFAA4F5B0F5DD3E5C32B12404ABBC78F7DE58416C796DB31A3DF4F6`; unchanged structural script SHA256 `EE2A0F17C8486250EE25FDBFAE9196D53F8B5599BE82E1BF17C8BC5F06B78545`.
- Pre-fix-3 specification checkpoint (controller-recorded fix-2 result): SHA256 `C85750A90E703B60C09D002450C91EEEC7247F2D4F6CA01D2020E46116EEF7A8`, 624268 bytes / 3977 lines.

## 1. Scope, tool and changed files

| Path | Change | SHA256 after fix 3 |
| --- | --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | Cumulative specification; bounded fix-3 corrections | `8676D05B72EA38FBEF75E131C4FBC8163506418BC05760D1B339210E06BE31C5` |
| `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-3.md` | This fix artifact (new) | Reported in the completion report; not self-embedded |

The specification is 3982 lines / 641156 bytes after fix 3. The change is specification prose, dictionary check text, transaction/service contracts and test-matrix assertions only. No table, column, invariant, wave, slice, action, reason-registry value, SQL function or migration was added, removed or reordered.

No controller, plan, prior implementation/fix/review artifact, source document, prototype, screenshot, `logo/preview.html`, old review, `.git`, config or credential path was modified. No stage, commit, push, PR, merge, provider change or S3 work was performed. No other agent or provider was invoked.

Tool note: all edits, including this new artifact, were applied with the verified Windows patch executable `/mnt/c/Users/Administrator/AppData/Local/OpenAI/Codex/bin/896a0166b774037a/codex.exe --codex-run-as-apply-patch`, using Windows absolute targets `D:/codex-call/...` and one safely quoted patch argument per invocation. No Edit/Write/Python or shell-redirection fallback wrote a target file.

## 2. Correction status

All accepted corrections are **已修复** at the specification level. None is claimed as runtime-proven.

| Correction | Status | Concrete correction locations | Evidence |
| --- | --- | --- | --- |
| PURCHASE-ID (fix-2 re-review finding OQ-1, distinct from review Open Question 1) | 已修复 | §10.17 stable-source-identities paragraph and `PURCHASE` fingerprint rows; §9.17 steps 2/4; §10.13 and §10.20 `append_purchase_consent`; §4.36; T-S2-53/T-S2-55 | The only derivation is `purchase-consent:` + lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, clientIdempotencyKey}` from the authenticated canonical lowercase account UUID and the exact client key. It is always 81 characters and is used for both the consent `event_id` and `source_ref`. No truncation, random per-attempt value, caller-chosen alternative or schema expansion. The order client key stays 1-475 and the platform key 1-512. T-S2-53/T-S2-55 assert 74/75/128/475 acceptance, 476 rejection before effects, Unicode/exact-key handling, same-input replay and changed-semantic conflict. Final wire encoding remains Q15/Q26. |
| EVENT-ID (review Open Question 1) | 已修复 | §2.9 idempotency/event-domain bullet; §4.28 `ck_ivr_decision_source` and column note; §9.11 step 1; §9.14 step 1; §9.24 step 1; §9.25 step 1; §10.17 activation-event paragraph; §10.20 `record_client_activation`; T-S2-16/T-S2-66 | IVR confirmation, direct-authorization and call-event source event IDs are nonblank 1-128 characters and are refused with stable `IVR_ARGUMENT_INVALID` before `idempotency_begin` or any domain effect. All 1-128 S2 source-event checks and the decision-source check agree. The general platform key remains 1-512 and the order key 1-475; accepted S1 semantics are unchanged. |
| POLICY-READ (review Open Question 2) | 已修复 | §4.29; §9.11 step 7; §9.26 step 5; §10.9 Arguments/Errors/Semantics and the linearization paragraphs; §10.11; §10.12; §10.13 `issueFirstTrial`; §10.20 `publish_trial_policy`; T-S2-49/T-S2-64 | The V006 `issue_first_trial` function performs the sole authoritative enabled/published/expected-version read, retains the selected immutable content in local values through grant/Outbox, and fails closed with `TRIAL_POLICY_NOT_FOUND` for missing/disabled/stale state. Replay/already-granted returns the prior immutable result before any new policy selection. The wrapper lookup is preliminary only and there is no second enabled recheck. Disable before selection refuses; disable after selection does not invalidate the grant. Signature, owner, lock order and function count are unchanged. Obsolete planned-V006 count wording is corrected to `4 V001 functions + 1 V006 function`. |
| ACTIVATION-REF (review Open Question 3) | 已修复 | §4.36; §9.11 step 6; §9.25 steps 1/4; §10.17 `SERVICE_ACTIVATION` fingerprint row; §10.20 `record_client_activation`; T-S2-54 | IVR `SERVICE_ACTIVATION` uses `ivr-session:` + canonical lowercase session UUID (48 characters), independent of the transport event ID. MINIAPP/WEB uses `activation:` + lowercase SHA-256 of the section 2.5 canonical JSON `{accountId, activationEventId}` (75 characters) from the authenticated canonical account and stable action ID. The existing semantic fingerprint binds these exact references. No new field, action, workflow or wire contract is added. |
| POLICY-PUBLISH (review Open Question 4) | 已修复 | §4.29; §9.26 step 5; §10.12 update-column row; §10.13 publication row; §10.16 guard; §10.20 `publish_trial_policy`; T-S2-64 | After retiring the old current policy, ONE owner `UPDATE` sets the actual authenticated `published_by_employee_id`, `published_at`, `enabled` and revision together while `OLD.published_at IS NULL`. The guard admits that single statement and rejects any later change to publisher identity or content. No separate pre-publication publisher statement is required and no same-employee-only rule is introduced. |
| EVENT-PAYLOAD (review Open Question 5) | 已修复 | §4.27 payload column note, `ck_call_event_payload_redacted` and revision/immutability paragraph; §9.24 steps 2/4/5; §10.17 call-event row; T-S2-56 | The existing JSONB has exactly two server-written members: `source` = `{eventType, legId, sequenceNo, payload}` and `receipt` = `{payloadSha256, eventFingerprint}`. `payload_sha256` hashes only the canonical `source.payload` and equals `receipt.payloadSha256`. The semantic fingerprint is computed from that digest plus immutable event inputs, then `receipt` is persisted as server metadata excluded from the hash and fingerprint input. Reconstruction never uses the mutable indexed `sequence_no`; a client cannot choose receipt metadata or bypass source-hash checking. No new column or final wire schema. |
| C-01 call-event lock/sequence ordering | 已修复 | §9.24 steps 2-5; §10.20 `process_call_event`; T-S2-56 | Replay/conflict and parent-call validation remain before mutation. The owned `ivr.inbound_session` then current `ivr.call_record` rank-4 locks are acquired and re-read before sequence detection or insert. `LEG_UNKNOWN` classification and the one `call_event` insert occur after those locks. Downstream reservation/settlement order is unchanged and no new lock is added. |
| C-02 changed-fingerprint conflict | 已修复 | §4.27; §9.24 step 2 and step 5; §10.17 call-event row; T-S2-56 | Only an identical semantic input replays the first outcome. A changed fingerprint/type/sequence always returns stable `EVENT_ID_CONFLICT` with no mutation, including when the first outcome was `SEQUENCE_CONFLICT`. First terminal outcomes and original payloads remain absorbing; 4.27/9.24/10.17/assertions now agree. |

Prior R-01..R-08 and F-01..F-16 closures remain in place; no accepted S1 rule, direct-authorization chain, savepoint/category outcome, owner-only write, policy deferral or named later-work-unit owner was reopened.

## 3. Preserved baseline and S1 semantics

- 44 tables; 40 invariants; 9 waves (`3/6/4/7/6/7/3/5/3`); slices `S1:13,S2:23,S3:8`; lifecycle `28/16`; sensitive `28`; FK `74 FK + 3 ALTER_FK + 2 DEFERRABLE_FK + 2 NO_FK = 81`; 24 key sets; five function-wave rows (four V001 functions plus one V006 `issue_first_trial`).
- Exactly 36 detailed dictionaries and 428 accepted columns (S1 13/121, S2 23/307), with accepted column order unchanged; all nine frozen comment blocks and the 40 invariant / 28 open-input / 9 wave row sets remain byte-identical to the accepted plan.
- No SQL function beyond the four V001 functions and the V006 `issue_first_trial`. The activation-consent switch, purchase-consent, call-event, publication and selection collaborators remain application services on one connection/one local transaction with `Propagation.MANDATORY` and no function-wave entry.
- Accepted S1 ownership and claim behavior, 30-second configurable DB-time task TTL, one non-terminal task/caller, immutable original resolve, both direct-authorization purpose chains, `UNKNOWN` occupancy/no redial, savepoint category-2 retention/category-3 rollback, owner-only writes and all unapproved policy deferrals are preserved.

## 4. Validation commands and results

All commands were run from `/mnt/d/codex-call` on 2026-10-22. These are document/shell checks only. No PostgreSQL, Flyway, privilege, lock, savepoint, concurrency, Testcontainers, IVR, payment-provider, KMS or cloud execution was performed.

1. **Unchanged structural script**: `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` -> exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`. Script SHA256 matches `EE2A0F17C8486250EE25FDBFAE9196D53F8B5599BE82E1BF17C8BC5F06B78545`; it was executed unchanged.
2. **Read-only Windows checker**: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s2-document.ps1"` -> exit `0`; `FrozenBlocks=9`, `Dictionaries=36`, `Columns=428`, `S1Columns=121`, `S2Columns=307`, `Bytes=641156`, `SHA256=8676D05B72EA38FBEF75E131C4FBC8163506418BC05760D1B339210E06BE31C5`, `Errors=[]`.
3. **Document-level checks**: the unchanged §13.1 command block and §13.6 S2 control-coverage block -> exit `0` each.
4. **Whitespace/EOF**: first three bytes `23 20 41` (UTF-8 without BOM); CR byte count `0`; trailing-whitespace lines `0`; final two bytes `100 10` (exactly one terminal LF); `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` -> exit `0`.
5. **Scope**: branch `codex/project-execution`; `git rev-parse HEAD` -> `5404d9caa73b72c412ca2065bef19dfb46d154d3`; `git diff --cached --name-only` -> empty; no `.sql` or `db/migration/` path in `git status`. Relative to the pre-fix-3 checkpoint, only the specification is modified and this artifact is added; the pre-existing dirty QA screenshots, `logo/preview.html`, controller modification, old review and prior S2 artifacts are preserved untouched.
6. **Binding hashes rechecked**: plan `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; fix-6 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`; fix-2 re-review `D43B955A096E1C3A52BE2F5041727B9A6032628FDCA4CB1207F292CB900F6C65`; fix-2 artifact `0250B5A25AFAA4F5B0F5DD3E5C32B12404ABBC78F7DE58416C796DB31A3DF4F6`.

Limitations: the structural script, PowerShell checker and §13 checks prove document structure, frozen-block equality, exact dictionary/count equality and whitespace only. They do not prove PostgreSQL privileges, SQL semantics, constraint behavior, lock behavior, savepoints, Flyway execution, concurrency or production behavior. The §13.7 matrix is a document requirement list, not executed test code.

## 5. Docs decision

- The allowed write scope is the cumulative specification `docs/14-production-data-model-and-flyway-schema-spec.md` and the new `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-3.md`. The controller, plan, prior artifacts, prototypes, screenshots and old review are outside this work unit and were read-only.
- **Documentation decision: pass at the specification level pending independent re-review.** The accepted fix-2 re-review finding and Open Questions/controller clarifications are corrected in place, with affected identities, validations, transactions, privileges, immutable facts and assertions aligned. No duplicated competing normative contract was appended. S2 acceptance, checkpoint/commit, S3 authoring and full A0 completion are not claimed.

## 6. Residual risk

### fixed-current (document evidence)

| Item | Evidence / limitation |
| --- | --- |
| PURCHASE-ID, EVENT-ID, POLICY-READ, ACTIVATION-REF, POLICY-PUBLISH, EVENT-PAYLOAD, C-01 and C-02 | Specification sections in §2 above; structural/dictionary/frozen/whitespace checks pass. These are document assertions only. |
| Frozen baseline, exact columns, S1 semantics | Unchanged structural script and read-only checker; no S1 behavior rewrite and no new schema object. |
| Scope and hashes | Only the specification is modified and this artifact is added relative to the pre-fix-3 checkpoint; binding input hashes match controller-recorded values. |

### covered-later-approved-slice (named owner/destination)

| Risk / item | Owner / destination |
| --- | --- |
| S3 evidence/media/sharing dictionaries, the deferred evidence cycle and final 44-table closure | S3 author; V008-V009/S3 gate. S3 remains planned and is not claimed authored. This is the only later-approved-S3 scope mentioned by this artifact. |

### assigned-later-work-unit (named owner/destination)

| Risk / item | Owner / destination |
| --- | --- |
| Executable PostgreSQL 16 DDL, Flyway V001-V007, role/privilege provisioning, lock/savepoint/category/concurrency/failure-injection tests and the §13.7 matrix execution | Backend/Database/Platform; implementation, migration and deployment work units (Q17/Q23). No runtime test is claimed. |
| Runtime role provisioning (LOGIN/`SET ROLE`/pool mapping) for same-connection owner services and the rank-4 collaborator execution mechanism | Platform/Database/Security; deployment-security design and executable privilege tests (Q17/Q23). |
| IVR protocol/line integration, B-leg local uniqueness/persistence/lost-response recovery and media behavior | Telephony/IVR; Q28 and IVR protocol/line integration. |
| Final wire encoding, order OpenAPI field names and IVR JSON-schema version, including the purchase-consent identity encoding, the activation source-reference encoding, event-ID wire representation and the final `payload_redacted` wire schema | IVR/Backend/Security/Product/Operations/catalog-order; Q15/Q26 and later protocol/OpenAPI work. This is assigned-later-work-unit, not promised S3 work. |
| KMS/envelope/HMAC rotation, authentication/session design and legal retention/erasure | Security/Infrastructure/Legal/Platform; Q1/Q2/Q3/Q19/Q21 and later security/legal units. |
| Production prices/quantities, allocation policy/cap, approved schedules/windows, prompt/policy content, duration tolerance, authorization window, TTL configuration key and organization task-access role matrix | Product/Finance/Operations/Entitlement/IVR/Backend; Q4/Q5/Q8/Q10/Q18/Q20/Q25/Q27 and later Phase C/IVR/acceptance work. |
| Enterprise ownership/transfer and exact role-to-claim/task permission policy | Product/Subject-number and Organization-access/Security; deferred fail-closed policy work. |
| Independent `catalog_offer` withdrawal entry | Product/Operations/catalog-order / catalog operations OpenAPI work (OQ-4); S2 relies on version offsale. |

## 7. Scope evidence and preserved pre-existing state

- The specification is the only file changed by this fix and this artifact is the only new file. The pre-existing modified QA screenshots, `logo/preview.html`, controller modification, older untracked review and prior S2 artifacts were not touched. No new dirty-file claim is made beyond `git status`/hash evidence recorded above.
- No credential, provider-secret configuration, WorkBuddy or unrelated personal file was read. The patch tool and the two named checkers were the only write/validation tooling used.
- No stage, commit, push, PR, issue, merge, package install or network write was performed.

## 8. Completion statement

**Author-complete; independent S2 re-review required.** The next gate after this fix return is independent re-review; it is never immediate S2 acceptance or S3 work. No S2 acceptance, checkpoint/commit, S3 authoring, full A0 completion, production/runtime result, executable SQL or migration is claimed.
