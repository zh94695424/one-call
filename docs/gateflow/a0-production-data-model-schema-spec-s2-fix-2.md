# A0 S2 fix 2 artifact (R-01..R-08, specification only)

- Task label: `a0-spec-s2-fix2-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix`, bounded S2 fix 2 for independent re-review findings R-01..R-08 and the accepted OQ-1..OQ-8 clarifications
- Runtime/provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Binding checkpoint: S1 accepted at `5404d9caa73b72c412ca2065bef19dfb46d154d3`. S2 is **fix-2 author-complete / independent re-review pending**; this is not S2 acceptance, not an A0 checkpoint and not production/runtime completion.
- Binding inputs: accepted plan SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; plan fix-6 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`; independent re-review `docs/reviews/code-review-20261022-202738.md` SHA256 `0A6A4165D24F99D151897421E8ABFF1126123521FCC6F2C184B5E9F11EC3E594`; S2 implementation checkpoint `8DA033D09B8AEB226E9B1D5CD414D7652995DF8CFC807B8145C4C73574948D76`; S2 fix-1 artifact `ACE32311336F69C91A4953B265F7BBEE6DC9D8CEDC47AEE566D31B1E73EB55FA`; unchanged structural script `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545`.
- Pre-fix-2 specification checkpoint: SHA256 `69CDE93B0E13442AC766AD2EAC353F53095B3D62D8C355AFADCCA301DA16A354`, 593859 bytes / 3958 lines (controller-recorded fix-1 result).

## 1. Scope, tool and changed files

| Path | Change | SHA256 after fix 2 |
| --- | --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | Cumulative specification; bounded fix-2 corrections | `C85750A90E703B60C09D002450C91EEEC7247F2D4F6CA01D2020E46116EEF7A8` |
| `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-2.md` | This fix artifact (new) | Reported in the completion report; not self-embedded |

The specification is 3977 lines / 624268 bytes after fix 2. The change is specification prose, dictionary control text, transaction/service contracts, grant rows and test-matrix assertions only. No table, column, invariant, wave, slice, action, reason-registry value, SQL function or migration was added, removed or reordered.

No controller, plan, prior implementation/fix/review artifact, source document, prototype, screenshot, `logo/preview.html`, old review, `.git`, config or credential path was modified. No stage, commit, push, PR, merge or S3 work was performed. No other agent or provider was invoked.

Tool note: no native `apply_patch` tool is exposed in this session. All edits, including this new artifact, used the verified Windows patch executable `/mnt/c/Users/Administrator/AppData/Local/OpenAI/Codex/bin/896a0166b774037a/codex.exe --codex-run-as-apply-patch` with Windows absolute targets. No shell redirection, Python write or alternate editor wrote a target file.

## 2. Finding status

All accepted findings are **已修复** at the specification level. None is claimed as runtime-proven.

| Finding | Status | Concrete correction locations | Evidence |
| --- | --- | --- | --- |
| R-01 publication writes the new ACTIVE/enabled version before retiring the old one | 已修复 | §9.26 steps 3/5; §4.17; §4.29; §7.3; §10.20 publication rows; T-S2-57/T-S2-64 | Catalog publication first sets the previous `ACTIVE` version `OFFSALE` (revision +1), then sets the candidate `published_at`/`ACTIVE` (revision +1). Trial-policy publication first sets the previous enabled published version `enabled = false` (revision +1), then sets the candidate `published_at` once, records the authenticated publisher and `enabled = true` (revision +1). Both state that the immediate partial unique indexes are satisfied at every statement, with no deferral or temporary second current row. Tests assert a pre-existing current row and one winner. |
| R-02 IVR confirmation takes a cross-owner `service_activation FOR UPDATE` | 已修复 | §9.0.1 item 4/item 10/new bullet; §9.11 steps 1/6; §9.13 steps 3/5; §9.14 step 1; §9.17 steps 1/2/4; §9.19 step 1; §3.1; §10.12; §10.13; §10.20; T-S2-41/T-S2-63 | IVR locks only its own `inbound_session` and current `call_record`; the existing activation row lock is taken at rank 4 by the activation-consent-owned `lock_service_activation` collaborator under `activation_consent_role`. A missing row is serialized by the subject lifecycle advisory and the collaborator locks/creates nothing. Foreign organization/membership/account rows use advisory locks plus exact `SELECT` re-reads. Later owner services reuse the held rank-4 lock and never acquire it after idempotency. |
| R-03 client activation re-reads account/organization/membership without grants | 已修复 | §3.2; §9.25 steps 1/2; §10.12 schema and cross-module rows; §10.20 `record_client_activation`; §11.4 V007 step 4; T-S2-41/T-S2-51/T-S2-54 | `activation_consent_role` receives schema `USAGE` without `CREATE` on `customer_iam` and `organization_access` and exact `SELECT` on `customer_account(id,status)`, `organization(id,status)`, `organization_membership(organization_id,account_id,role,status)` and `business_subject(id,owner_account_id,owner_organization_id,revision)`. No foreign write, row lock or broad table grant is added. |
| R-04 catalog-order projection reads issuance/grant without grants | 已修复 | §4.20; §10.12; §11.4 V006 step 5; T-S2-41/T-S2-51 | `catalog_order_role` receives `USAGE` without `CREATE` on `entitlement_metering` and exact `SELECT` on `entitlement_issuance(order_id, installment_no, entitlement_type, status, grant_id, issued_at, last_error_code)` and `entitlement_grant(id, subject_id, entitlement_type, granted_units, valid_from, expires_at)`. §4.20 documents the join and confirms no stored order-level entitlement status, no write and no row lock. |
| R-05 order creation takes the activation row lock after idempotency | 已修复 | §9.0.1 item 10/new bullet; §9.17 steps 1/2/4; T-S2-63 | The order path acquires subject (3), the rank-4 activation lock through the owner collaborator, organization/membership/account advisory locks (7/8/9), and only then the idempotency row (10); `append_purchase_consent` reuses the held activation lock. The total order now has no rank-4-after-rank-10 exception and no foreign row lock. |
| R-06 `CALL_UNKNOWN` quarantine cannot persist because `call_id` is NOT NULL FK | 已修复 | §9.24 steps 2-5; §4.27 revision prose; §10.13/§10.20 `process_call_event`; T-S2-56 | Replay/conflict is checked first; the parent `call_id` is validated before any insert. An unknown parent call returns stable `CALL_UNKNOWN` with no `call_event`/call/leg/settlement write and no raw FK. A known call with an unknown leg may persist one durable `QUARANTINED` `LEG_UNKNOWN` receipt. No nullable FK, placeholder call or new column is introduced. |
| R-07 call-event replay can lose the first outcome or regress APPLIED | 已修复 | §4.27; §9.24 steps 2-6; §10.17 call-event row; T-S2-56 | The first stored payload and terminal `APPLIED`/`QUARANTINED`/`IGNORED` outcome are absorbing. `payload_redacted` explicitly preserves original `eventType`, `legId`, original `sequenceNo`, `payloadSha256` and the event fingerprint, so replay comparison works even when the indexed `sequence_no` is `NULL` for a sequence conflict. Same-semantic retry returns the first outcome; changed input returns a stable conflict without changing the first row or call/leg/settlement facts; any conflict audit uses the same event identity and no new schema object. |
| R-08 no exact `SERVICE_ACTIVATION` consent fingerprint; transition ignores consent content | 已修复 | §4.36; §9.25 steps 3/4; §10.17 `SERVICE_ACTIVATION` consent/activation-transition/trial-grant rows; §10.13/§10.20; T-S2-54 | The `SERVICE_ACTIVATION` consent scope enumerates subject, null-safe account, channel, source type, stable `sourceRef`, purpose, policy code/version, prompt version and action, and excludes transport `eventId`, timestamps, generated consent ID, snapshot hash and current revision/status. The activation-transition and ACTIVATION trial fingerprints bind the immutable consent fingerprint instead of `consentRecordId`/`fromRevision`/`fromStatus`. Source-action identity is distinguished from transport event identity; a regenerated IVR confirmation event ID with identical semantics still replays the first IVR decision. |

## 3. OQ handling

| OQ | Outcome | Record |
| --- | --- | --- |
| OQ-1 stable `purchaseConsentEventId` | **accepted/implemented** | §10.17 stable-source-identity paragraph defines deterministic `purchase-consent:<canonical lowercase account UUID>:<client idempotency key>` (or an explicit caller-stable value normalized to the same order key); §9.17 steps 2/4 and §10.20 use it; T-S2-55 asserts same-key retry stability. Final wire encoding remains the order OpenAPI owner (Q15/Q26). |
| OQ-2 `grant.granted_units` for routing | **accepted/implemented** | §10.12 adds `entitlement_grant.granted_units` to `ivr_role`; §10.18 evaluates the documented equality from declared columns; §11.4 V006 step 5 and T-S2-40/T-S2-51 assert it. The locked reservation authorization is unchanged. |
| OQ-3 collaborator matrix reconciliation | **accepted/implemented** | §3.2 marks exact `SELECT` collaborators versus owner-service fact handoffs, adds the actually granted S2 readers and removes conceptual reads with no grants. No extra access is granted; settlement still receives IVR facts without reading IVR tables. |
| OQ-4 catalog-offer withdrawal | **deferred-with-owner** | §4.18, §9.26 step 4, §10.20 and §14 state that S2 does not implement or claim an independent `catalog_offer` withdrawal endpoint/transaction; version `OFFSALE` blocks new orders. Owner: Product/Operations/catalog-order / catalog operations OpenAPI work. |
| OQ-5 publisher identity | **accepted/implemented** | §4.29, §9.26 step 5, §10.12 bounded update column, §10.16 guard and §10.20 record the actual authenticated publisher in `published_by_employee_id` while `published_at IS NULL` and freeze it on publication. No same-employee-only rule or draft-owner relabel is added; T-S2-64 covers it. |
| OQ-6 policy snapshot linearization | **accepted/implemented** | §10.9 (new policy-selection paragraph), §10.13 `issueFirstTrial`, §4.29 and §9.26 step 5 name the single authoritative enabled-published read as the selection linearization point and retain that exact immutable version/content through the grant. Wrapper lookup is preliminary; disable-before-read is observed/fails closed, disable-after-read does not revoke; publication and IVR lock orders are not combined. T-S2-49/T-S2-64 cover both interleavings. |
| OQ-7 DB matrix versus service rule | **accepted/implemented** | §4.38, §7.3 and §9.19 state that `ck_activation_transition_matrix` is a broad DB shape bound and the sole owner service rejects the narrower forbidden source/state pairs. T-S2-29 adds the negative assertions. Accepted S1 semantics are unchanged. |
| OQ-8 immediate `EXHAUSTED` status | **accepted/implemented** | §4.32, §9.15 step 2/4a, §10.20 `settle_call_usage` and T-S2-36 specify one conditional balance `UPDATE` that sets counters and the derived `EXHAUSTED` status together; `UNKNOWN`/excess outcomes write no balance counter or status and never release uncertain use. |

## 4. Preserved baseline and S1 semantics

- 44 tables; 40 invariants; 9 waves (`3/6/4/7/6/7/3/5/3`); slices `S1:13,S2:23,S3:8`; lifecycle `28/16`; sensitive `28`; FK `74 FK + 3 ALTER_FK + 2 DEFERRABLE_FK + 2 NO_FK = 81`; 24 key sets; five function-wave rows (four V001 + one V006).
- Exactly 36 detailed dictionaries and 428 accepted columns (S1 13/121, S2 23/307), with accepted column order unchanged; nine frozen comment blocks and the 40 invariant / 28 open-input / 9 wave row sets remain byte-identical to the accepted plan.
- No SQL function beyond the four V001 functions and the one V006 `issue_first_trial`. The new `activation-consent.lock_service_activation` and every other collaborator are application services on one connection/one local transaction with `Propagation.MANDATORY` and no function-wave entry.
- S1 ownership, claim replay/risk/fail-closed enterprise boundary, no-savepoint whole-transaction retry, stable logical membership-pair lock, canonical-number placement and owner-only row-lock/cross-module read contracts are preserved.
- One non-terminal task/caller, 30-second configurable task TTL using DB time, immutable initial resolve, both direct-authorization purpose chains, `UNKNOWN` occupancy/no redial, category-2 savepoint retention/category-3 full rollback, owner-only writes and all unapproved policy deferrals are preserved.

## 5. Validation commands and results

All commands were run from `/mnt/d/codex-call` on 2026-10-22. These are document/shell checks only. No PostgreSQL, Flyway, privilege, lock, savepoint, concurrency, Testcontainers, IVR, payment-provider, KMS or cloud execution was performed.

1. `sha256sum /tmp/a0s1/structural-v2-run.sh` -> `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545` (expected match).
2. `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` -> exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`.
3. Read-only Windows checker `C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s2-document.ps1` -> exit `0`; `FrozenBlocks=9`, `Dictionaries=36`, `Columns=428`, `S1Columns=121`, `S2Columns=307`, `Bytes=624268`, `SHA256=C85750A90E703B60C09D002450C91EEEC7247F2D4F6CA01D2020E46116EEF7A8`, `Errors=[]`. Exactly nine frozen blocks equal the accepted plan and all 428 columns match the accepted order.
4. §13.1 document-level checks -> exit `0`; §13.6 S2 control-coverage checks -> exit `0`.
5. Whitespace/EOF: CR count `0`; trailing-whitespace lines `0`; terminal byte `10` (exactly one LF); UTF-8 without BOM; `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` -> exit `0`.
6. Scope: branch `codex/project-execution`; `git rev-parse HEAD` -> `5404d9caa73b72c412ca2065bef19dfb46d154d3`; `git diff --cached --name-only` -> empty; no `.sql` or `db/migration/` path in `git status`. Only the specification is modified by this fix and this artifact is added; pre-existing dirty screenshots, `logo/preview.html`, the controller file, the old review and prior S2 artifacts are preserved untouched.
7. Binding hashes rechecked: plan `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; fix-6 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`; fix-1 `ACE32311336F69C91A4953B265F7BBEE6DC9D8CEDC47AEE566D31B1E73EB55FA`; re-review `0A6A4165D24F99D151897421E8ABFF1126123521FCC6F2C184B5E9F11EC3E594`; S2 implementation `8DA033D09B8AEB226E9B1D5CD414D7652995DF8CFC807B8145C4C73574948D76`.

Limitations: the structural script, PowerShell checker and §13 checks prove document structure, frozen-block equality, exact dictionary/count equality and whitespace only. They do not prove PostgreSQL privileges, SQL semantics, lock behavior, savepoints, Flyway execution, concurrency or production behavior. The §13.7 matrix is a document requirement list, not executed test code.

## 6. Residual risks

### fixed-current (document evidence)

| Item | Evidence / limitation |
| --- | --- |
| R-01..R-08 corrections and OQ-1..OQ-8 dispositions | Specification sections in §2 and §3 above; structural/dictionary/frozen/whitespace checks pass. These are document assertions only. |
| Frozen baseline, exact columns, S1 semantics | Unchanged structural script and the read-only checker; no S1 behavior rewrite and no new schema object. |
| Scope and hashes | Only the specification is modified and this artifact is added; binding input hashes match controller-recorded values. |

### covered-later-approved-slice (named owner/destination)

| Risk / item | Owner / destination |
| --- | --- |
| S3 evidence/media/sharing dictionaries, deferred evidence cycle and final 44-table closure | S3 author; V008-V009/S3 gate. S3 remains planned and is not claimed authored. |
| Final wire encoding, order OpenAPI field names and IVR JSON-schema version (including OQ-1 identity encoding and OQ-4 offer withdrawal decision) | IVR/Backend/Security/Product/Operations/catalog-order; Q15/Q26 and later protocol/OpenAPI work. |

### assigned-later-work-unit (named owner/destination)

| Risk / item | Owner / destination |
| --- | --- |
| Executable PostgreSQL 16 DDL, Flyway V001-V007, privilege/schema-usage assertions, lock-order/savepoint/category tests, concurrency/Testcontainers suites | Backend/Database/Platform; implementation, migration and deployment work units (Q17/Q23). No runtime test is claimed. |
| Runtime role provisioning (LOGIN/`SET ROLE`/pool mapping) for same-connection owner services and the R-02/R-03/R-05 privilege/lock boundaries, including the `lock_service_activation` collaborator execution mechanism | Platform/Database/Security; deployment-security design and executable privilege tests (OQ-2/Q17/Q23). The same-connection same-transaction contract is specified; the provisioning mechanism is not. |
| IVR B-leg local uniqueness/persistence/lost-response recovery, line/media behavior and authoritative call facts | Telephony/IVR; Q28 and IVR protocol/line integration. |
| KMS/envelope/HMAC rotation, authentication/session design, legal retention/erasure | Security/Infrastructure/Legal/Platform; Q1/Q2/Q3/Q19/Q21 and later security/legal units. |
| Production prices/quantities, allocation policy/cap, approved schedules/windows, prompt/policy content, duration tolerance, authorization window, TTL configuration key and organization task-access role matrix | Product/Finance/Operations/Entitlement/IVR/Backend; Q4/Q5/Q8/Q10/Q18/Q20/Q25/Q27 and later Phase C/IVR/acceptance work. |

## 7. Coverage limitations

- No PostgreSQL instance, Flyway run, privilege/grant execution, row-lock/concurrency test, savepoint/category test, Testcontainers suite, IVR line, payment provider, KMS/cloud resource or production data was used. No runtime pass is claimed.
- The static checks cannot prove that the specified owner-role collaborator can be provisioned on the same connection at runtime; that remains the named deployment-security input (Q17/Q23).
- The specification remains a document deliverable. Independent S2 re-review is the next gate; this artifact does not accept S2, does not advance to S3 and does not authorize implementation.
- Pre-existing modified QA screenshots, `logo/preview.html`, the controller file, the old review and prior S2 artifacts are outside this work unit and were not changed. No stage, commit, push, PR, merge or provider change was performed.

## 8. Completion statement

**Author-complete; independent re-review required.** No S2 acceptance, no S3 authoring, no Git stage/commit/push/PR/merge and no production/runtime test is claimed.
