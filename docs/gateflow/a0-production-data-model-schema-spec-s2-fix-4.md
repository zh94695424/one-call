# A0 S2 fix 4 artifact (fix-3 re-review corrections, specification only)

- Task label: `a0-spec-s2-fix4-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix`, bounded S2 fix 4 for the fix-3 independent re-review finding F3-01 / OQ-F3-1 and the OQ-F3-2 IVR consent event-identity clarification
- Runtime/provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Binding checkpoint: S1 accepted at `5404d9caa73b72c412ca2065bef19dfb46d154d3`. S2 is **fix-4 author-complete / independent re-review pending**; this is not S2 acceptance, not an A0 checkpoint and not production/runtime completion.
- Binding inputs: accepted plan SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; plan fix-6 SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`; fix-3 re-review `docs/reviews/code-review-20261022-212225.md` SHA256 `C302524CDBF11FFA7C84A158BDCF2019B5FB3C03E0DA879197010B626717BB25`; binding adjudication `S2 fix 3 independent re-review and fix 4 adjudication` in `docs/gateflow/project-execution-control.md`; prior fix-3 artifact SHA256 `497F15CA7C27AD17DBFA0F87E6CBC86C97D8DEF34684118F362B282C05BEFADD`; unchanged structural script SHA256 `EE2A0F17C8486250EE25FDBFAE9196D53F8B5599BE82E1BF17C8BC5F06B78545`.
- Pre-fix-4 specification: SHA256 `8676D05B72EA38FBEF75E131C4FBC8163506418BC05760D1B339210E06BE31C5`, 641156 bytes / 3982 lines.

## 1. Scope, tool and changed files

| Path | Change | SHA256 after fix 4 |
| --- | --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | Cumulative specification; two bounded corrections plus fix-4 status metadata | `BDFE41EF0B0A278EC196C2E93926BDD1666A8D986135E14DE3099B9491CC72BF` |
| `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-4.md` | This fix artifact (new) | Reported in the completion report; not self-embedded |

The specification is 3982 lines / 643834 bytes after fix 4. The change is prose, one exact grant projection, service/transaction wording, fingerprint wording, test assertions and status metadata only. No table, column, invariant, wave, slice, action, reason-registry value, SQL function, migration, workflow or wire contract was added, removed or reordered.

No controller, plan, prior implementation/fix/review artifact, source document, prototype, screenshot, `logo/preview.html`, old review, `.git`, config or credential path was modified. No stage, commit, push, PR, merge, provider change or S3 work was performed. No other agent or provider was invoked. The pre-existing dirty screenshot/prototype/logo/controller paths and older untracked review were not touched and are not used as scope evidence.

Tool note: all edits, including this new artifact, were applied with the verified Windows patch executable `/mnt/c/Users/Administrator/AppData/Local/OpenAI/Codex/bin/896a0166b774037a/codex.exe --codex-run-as-apply-patch`, using Windows absolute targets `D:/codex-call/...` and one safely quoted patch argument per invocation. No Edit/Write/Python or shell-redirection fallback wrote a target file, and no write probe was made against an existing historical file.

## 2. Correction status

All accepted corrections are **已修复** at the specification level. None is claimed as runtime-proven.

| Correction | Status | Concrete correction locations | Evidence |
| --- | --- | --- | --- |
| F3-01 / OQ-F3-1 order-side purchase-consent `source_ref` read | 已修复 | §10.12 exact `catalog_order_role` `SELECT` on `activation_consent.consent_record`; T-S2-41, T-S2-51, T-S2-55 | `source_ref` is added to the exact column projection, so the order owner can execute the existing `event_id = source_ref = purchaseConsentEventId` validation. The grant remains read-only with no `INSERT`/`UPDATE`/`DELETE`, no foreign row lock and unchanged `activation_consent` schema `USAGE`. The purpose text names the validation. T-S2-41 and T-S2-51 assert that the exact-column read succeeds and that unlisted `consent_record` columns and every foreign write or row lock are denied; T-S2-55 asserts the full order-side validation executes under that grant. No owner handoff, schema object or broader privilege was redesigned. |
| OQ-F3-2 IVR `SERVICE_ACTIVATION` consent event identity | 已修复 | §4.36 append-only control; §9.11 step 6; §10.13 `record_activation_confirmation`; §10.17 `SERVICE_ACTIVATION` fingerprint row; T-S2-54 | On the first accepted IVR append, `event_domain = IVR_CONFIRM` and `event_id` is exactly the already validated nonblank 1-128 confirmation `eventId`; `source_ref` remains `ivr-session:` + canonical lowercase session UUID (48 characters), independent of that transport event ID. A same-semantic retry with a new transport event ID returns the first decision/consent before any append, never rewrites the first consent `event_id` and adds no consent. The transport `eventId` remains excluded from the `SERVICE_ACTIVATION` semantic fingerprint. No new wire encoding, event identity, field, action or policy is introduced. |

Status metadata refresh (not a normative correction): the header task/gate/status lines, the S2 status paragraphs and the §13.1 `STALE S2 STATUS` assertion now name fix 4 instead of fix 3. No contract, count or frozen block changed.

The seven other fix-3 corrections (EVENT-ID, POLICY-READ, ACTIVATION-REF, POLICY-PUBLISH, EVENT-PAYLOAD, C-01, C-02), prior R-01..R-08 and F-01..F-16 closures, accepted S1 rules and all named deferrals remain unchanged. No accepted ownership, lock order, 30-second task TTL, permanent trial, `UNKNOWN` occupancy, savepoint category-2/category-3 outcome or direct-authorization chain was reopened.

## 3. Preserved baseline and S1 semantics

- 44 tables; 40 invariants; 9 waves (`3/6/4/7/6/7/3/5/3`); slices `S1:13,S2:23,S3:8`; lifecycle `28/16`; sensitive `28`; FK `74 FK + 3 ALTER_FK + 2 DEFERRABLE_FK + 2 NO_FK = 81`; 24 key sets; five function-wave rows (four V001 functions plus one V006 `issue_first_trial`).
- Exactly 36 detailed dictionaries and 428 accepted columns (S1 13/121, S2 23/307) in accepted order; all nine frozen comment blocks and the 40 invariant / 28 open-input / 9 wave row sets remain byte-identical to the accepted plan.
- No SQL function beyond the four V001 functions and the V006 `issue_first_trial`. The activation-consent services, including `record_activation_confirmation` and `append_purchase_consent`, remain application services on one connection/one local transaction with `Propagation.MANDATORY` and no function-wave entry.

## 4. Validation commands and results

All commands were run from `/mnt/d/codex-call` on 2026-10-22. These are document/shell checks only. No PostgreSQL, Flyway, privilege, lock, savepoint, concurrency, Testcontainers, IVR, payment-provider, KMS or cloud execution was performed.

1. **Unchanged structural script**: `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` -> exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`. Script SHA256 matches `EE2A0F17C8486250EE25FDBFAE9196D53F8B5599BE82E1BF17C8BC5F06B78545`; it was executed unchanged.
2. **Read-only Windows checker**: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s2-document.ps1"` -> exit `0` with `FrozenBlocks=9`, `Dictionaries=36`, `Columns=428`, `S1Columns=121`, `S2Columns=307`, `Bytes=643834`, `SHA256=BDFE41EF0B0A278EC196C2E93926BDD1666A8D986135E14DE3099B9491CC72BF`, `Errors=[]`. The checker was not edited or weakened.
3. **Document-level checks**: the unchanged §13.1 command block and §13.6 S2 control-coverage block were extracted and piped to `bash` -> exit `0` each.
4. **Whitespace/EOF**: first three bytes `23 20 41` (UTF-8 without BOM); CR byte count `0`; trailing-whitespace lines `0`; final two bytes `64 0A` (exactly one terminal LF); `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` -> exit `0`.
5. **Scope**: branch `codex/project-execution`; `git rev-parse HEAD` -> `5404d9caa73b72c412ca2065bef19dfb46d154d3`; `git diff --cached --name-only` -> empty; no `.sql` or `db/migration/` path in `git status`. Relative to the work-unit start, only the specification is modified and this artifact is added; the pre-existing dirty paths were untouched.

Limitations: the structural script, PowerShell checker and §13 checks prove document structure, frozen equality, exact dictionary/count equality and whitespace only. They do not prove PostgreSQL privileges, SQL semantics, constraint behavior, lock behavior, savepoints, Flyway execution, concurrency or production behavior. The §13.7 matrix is a document requirement list, not executed test code.

## 5. Docs decision

- The allowed write scope is the cumulative specification `docs/14-production-data-model-and-flyway-schema-spec.md` and the new `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-4.md`. The controller, plan, prior artifacts, prototypes, screenshots and old review were read-only.
- **Documentation decision: pass at the specification level pending independent re-review.** F3-01/OQ-F3-1 and OQ-F3-2 are corrected in place, aligned with the existing transaction, owner-service, fingerprint and test contracts. No duplicated competing normative contract was appended. S2 acceptance, checkpoint/commit, S3 authoring and full A0 completion are not claimed.

## 6. Residual risk

### fixed-current (document evidence)

| Item | Evidence / limitation |
| --- | --- |
| F3-01/OQ-F3-1 and OQ-F3-2 | Specification sections and assertions in §2 above; structural/dictionary/frozen/whitespace checks pass. These are document assertions only. |
| Frozen baseline, exact columns, S1 semantics | Unchanged structural script and read-only checker; no S1 behavior rewrite and no new schema object. |
| Scope and hashes | Only the specification is modified and this artifact is added relative to the work-unit start; binding input hashes match controller-recorded values. |

### covered-later-approved-slice (named owner/destination)

| Risk / item | Owner / destination |
| --- | --- |
| S3 evidence/media/sharing dictionaries, the deferred evidence cycle and final 44-table closure | S3 author; V008-V009/S3 gate. S3 remains planned and is not claimed authored. This is the only later-approved-S3 scope claimed here. |

### assigned-later-work-unit (named owner/destination)

| Risk / item | Owner / destination |
| --- | --- |
| Executable PostgreSQL 16 DDL, Flyway V001-V007, role/privilege provisioning, lock/savepoint/category/concurrency/failure-injection tests and the §13.7 matrix execution | Backend/Database/Platform; implementation, migration and deployment work units (Q17/Q23). No runtime test is claimed. |
| Runtime role provisioning (LOGIN/`SET ROLE`/pool mapping) for same-connection owner services and the rank-4 collaborator execution mechanism | Platform/Database/Security; deployment-security design and executable privilege tests (Q17/Q23). |
| IVR protocol/line integration, B-leg local uniqueness/persistence/lost-response recovery and media behavior | Telephony/IVR; Q28 and IVR protocol/line integration. |
| Final wire encoding, order OpenAPI field names and IVR JSON-schema version, including the purchase-consent identity, the IVR consent event-ID representation and the activation source-reference encoding | IVR/Backend/Security/Product/Operations/catalog-order; Q15/Q26 and later protocol/OpenAPI work. Assigned-later-work-unit, not promised S3 work. |
| KMS/envelope/HMAC rotation, authentication/session design and legal retention/erasure | Security/Infrastructure/Legal/Platform; Q1/Q2/Q3/Q19/Q21 and later security/legal units. |
| Production prices/quantities, allocation policy/cap, approved schedules/windows, prompt/policy content, duration tolerance, authorization window, TTL configuration key and organization task-access role matrix | Product/Finance/Operations/Entitlement/IVR/Backend; Q4/Q5/Q8/Q10/Q18/Q20/Q25/Q27 and later Phase C/IVR/acceptance work. |
| Enterprise ownership/transfer and exact role-to-claim/task permission policy | Product/Subject-number and Organization-access/Security; deferred fail-closed policy work. |
| Independent `catalog_offer` withdrawal entry | Product/Operations/catalog-order / catalog operations OpenAPI work (OQ-4); S2 relies on version offsale. |
| Call-event JSONB check strength and §13.7 executable presence/reconstruction assertions | Backend/Database/IVR; executable implementation and test work. Explicitly deferred here and not strengthened by this fix; the existing source/receipt writer contract remains binding. |

## 7. Completion statement

**Author-complete; independent S2 re-review required.** The next gate after this fix return is independent re-review; it is never immediate S2 acceptance or S3 work. No S2 acceptance, checkpoint/commit, S3 authoring, full A0 completion, production/runtime result, executable SQL or migration is claimed.

No credential, provider-secret configuration, WorkBuddy or unrelated personal file was read. No stage, commit, push, PR, issue, merge, package install or network write was performed. Stop after the two allowed paths.
