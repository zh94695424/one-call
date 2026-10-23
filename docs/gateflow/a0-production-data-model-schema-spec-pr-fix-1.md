# A0 PR fix 1 completion record

- Task label: `a0-spec-pr1-fix1-ds-20261023-01`
- Gate: `PR review fix` (bounded PR fix 1) for `A0-production-data-model-schema-spec`
- Work unit: `A0-production-data-model-schema-spec`
- Runtime/provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-23
- Status: author-complete; stopped after the specification and this artifact. Independent PR re-review is controller-owned and has not occurred.

## Binding inputs

- PR review: `docs/reviews/pr-1-review-20261023-094149.md`, SHA256 `44434E457BB3C9BAD99D6DFC590D79E4BC811FFA98CC33B0A17818E7580C3E3B`; PR-01..PR-05 and OQ-PR-01 accepted.
- Controller: `docs/gateflow/project-execution-control.md` (read-only), section `PR #1 independent review adjudication and fix entry`; accepted aggregate deepreview checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61`; current PR lifecycle/gate state is authoritative only there.
- Accepted plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`, SHA256 `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`, line 633 (`source_event_id = eventId`).
- Plan fix-6: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`, SHA256 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`.
- Pre-fix specification: `docs/14-production-data-model-and-flyway-schema-spec.md`, SHA256 `7EC9540389D5EFE42468FF64C438B46EEFEB6FC495B1694776A7C60FFD681FC1`, 873199 bytes / 4882 lines (PR-target content).
- Read-only context: the design source, historical plan/fix/review artifacts, prototypes, screenshots, source, configuration and Git metadata.

## Write method and scope

- Native `apply_patch` is unavailable in this environment; the controller's verified Windows fallback path is outside the permitted workspace and its execution was denied. Repository writes therefore used the harness's patch-equivalent Edit/Write tools with exact string replacement, not wholesale rewrites or shell redirection.
- Changed repository paths: `docs/14-production-data-model-and-flyway-schema-spec.md` (modified) and this artifact (new). No other path was written.
- The controller, PR review artifact, accepted plan/fix-6, all historical artifacts, `docs/reviews/plan-review-20261021-205118.md`, `logo/preview.html`, screenshots, source, configuration and Git metadata remain untouched.
- No stage, commit, push, PR mutation, issue change, install, network write, credential/provider change or later gate was performed.

## Accepted finding-by-finding corrections

All statements are author-reported fixed-current text, not independent acceptance.

| Finding | Correction and exact sections |
| --- | --- |
| PR-01 evidence creation stable identity | `§2.12` states that `evidence_record.source_event_id` is exactly the validated stable source event `eventId` (nonblank 1-128), never a hash of mutable facts, and equals the one `EVIDENCE_CREATED.event_id`. `§9.28` steps 1 and 5 read by that stable `eventId`, set the record's `source_event_id` to it with no derivation, and state that exact replay returns the first record/event/artifact result while changed subject/call/session/reservation/recording/consent/snapshot facts return `EVIDENCE_SOURCE_CONFLICT` with no second record, creation event, artifact or Outbox fact. `§10.23` `evidence.create_and_solidify` states the same contract. `§10.24` changes the `EVIDENCE_CREATED` identity row to the validated source event ID, defines the creation-event payload conflict as `EVIDENCE_SOURCE_CONFLICT`, and keeps lifecycle-event conflicts as `EVIDENCE_EVENT_CONFLICT`. `§4.41` notes the stable source event. `T-S3-08` and `T-S3-31` assert replay, changed-fact conflict and the no-second-fact outcome. `§13.1` removes the obsolete `evidence-created:` required term and adds `EVIDENCE_SOURCE_CONFLICT` and `source_event_id = eventId`. No table, column, key, role or function was added. |
| PR-02 artifact ordering | `§2.12` defines one fixed non-lexical rank `ORIGINAL = 0`, `DERIVED = 1`. `§2.12` `evidence.read_shareable_projection`, `§4.44` content construction, `§10.23` projection contract and `§10.24` lifecycle `artifactRoles` payloads all use that rank before the existing `mediaSha256`/`mediaSizeBytes`/`mediaMimeType` ascending tie-breakers. `T-S3-15`, `T-S3-29` and `T-S3-31` assert the rank-ordered array and digest reproducibility. `scopeDigest`, `content_sha256` and `creationFingerprint` inherit the same deterministic content. No schema object changed. |
| PR-03 durable acceptance metadata | Header lines 5-6 and 14, `§1.1`, `§1.5`, `T-S2-65` and `§13.10` now record aggregate deepreview acceptance at checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61` and state that PR lifecycle/current gate state is authoritative only in `docs/gateflow/project-execution-control.md`. The durable specification contains no pending-aggregate-re-review or volatile next-gate assertion. Historical artifacts are unchanged. |
| PR-04 reconciliation caller-lock ownership | Added the IVR-owned, same-connection, `Propagation.MANDATORY`, lock-only application collaborator `ivr.lock_reconciliation_caller`. Given the immutable `callId`, it reads the existing `ivr.call_record.caller_number_hmac` under `ivr_role`, acquires `ivr.caller:<canonical_caller_hmac>` at rank 1, returns only stable success, stable `CALL_UNKNOWN` not-found or stable `DATA_INTEGRITY`, takes no row lock and writes/appends/enqueues nothing. Named consistently in `§3.1`, `§3.2`, `§9.0.1` items 1/12 and the collaborator bullet, `§9.16` phases A/C and step 4, the `§10.12` grant/owner clarification, the `§10.13` published-service contract, `T-S2-34`, `T-S2-36`, `T-S2-41`, `T-S2-51` and the `§13.1` required-term check. The entitlement owner then discovers and locks only its own balance/reservation rows in the existing item-12 order. No SQL function, cross-schema grant, table, column or role was added, and no PostgreSQL/advisory lock is held across phase-B IVR network I/O. |
| PR-05 UNKNOWN terminal shape | `§9.24` step 6 now splits terminal handling: an accepted authoritative terminal event writes `ENDED`/`FAILED` with accepted `ended_at`, `end_reason` and `authoritative_seconds`; a non-authoritative terminal receipt writes `UNKNOWN` and the stable reason only, leaving `ended_at` and `authoritative_seconds` NULL so `ck_call_record_terminal_shape` and `ck_call_record_authoritative_seconds` hold immediately. Only authoritative reconciliation may later move `UNKNOWN` to `ENDED`/`FAILED`. `T-S2-37` and `T-S2-56` assert the initial non-authoritative receipt, CHECK compatibility and later convergence. No schema check changed. |
| OQ-PR-01 ticket expiry formula | `§9.31` step 1 defines `ticketExpiresAt = min(clock_timestamp() + approved Q22 ticket TTL, sessionExpiresAt, admissionExpiresAt, current grant expires_at)` and derives the Redis TTL as `ticketExpiresAt - clock_timestamp()` only when positive; a non-positive difference issues no ticket and fails closed. `§2.12`, `§9.30`, the `§10.23` ticket service and `T-S3-21` reference the same formula. No Q22 TTL, clock-skew, revocation-window or policy value was selected or invented; Q22 ownership is unchanged. |

## Consistency and non-regression

- The obsolete evidence-created hash identity, lexical artifact ordering, pending aggregate re-review, unnamed reconciliation caller-lock actor, `UNKNOWN` with `ended_at`/`authoritative_seconds`, and duration-vs-timestamp TTL wording are absent.
- Prior accepted semantics outside the directly necessary cross-references/assertions are preserved: ownership boundaries, product policy, action/reason registries, TTL/retention values, provider inputs, migration waves, slices, FK modes, lifecycle and sensitive classifications.
- The nine frozen blocks, 44-table/522-column dictionaries (121/307/94), 40 invariants, nine waves, slices 13/23/8, FK 74/3/2/2 = 81, lifecycle 28/16, sensitive 28, 24 key sets, five function-wave rows and ten required embedded validation blocks remain in place.

## Frozen shape

44 tables; 522 columns split S1=121/S2=307/S3=94; 40 invariants; nine migration waves (3/6/4/7/6/7/3/5/3); slices S1=13/S2=23/S3=8; FK modes 74/3/2/2 = 81; lifecycle 28/16; sensitive 28; key sets 24; function-wave rows 5. No table, column, invariant, wave, slice, FK decision, key set, action, reason registry, role or policy was added or removed. The only new named contract object is the application-service collaborator `ivr.lock_reconciliation_caller` required by PR-04; it is not a SQL function or database role.

## Changed files and hashes

- `docs/14-production-data-model-and-flyway-schema-spec.md`: modified; 885692 bytes / 4885 lines; SHA256 `5CC3A7FC4F1CD27D961459339CABE9E814B931A921F48DF145122449C9049303`.
- `docs/gateflow/a0-production-data-model-schema-spec-pr-fix-1.md`: this new artifact; its final digest is reported in the author final response because a file cannot embed its own final digest.

## Validation commands and results

### Executed document checks

- `sha256sum docs/14-production-data-model-and-flyway-schema-spec.md` before edits: `7EC9540389D5EFE42468FF64C438B46EEFEB6FC495B1694776A7C60FFD681FC1`, matching the PR-target content and the review-recorded hash.
- `sha256sum` after edits: `5CC3A7FC4F1CD27D961459339CABE9E814B931A921F48DF145122449C9049303`.
- `wc -l -c`: `4885` lines / `885692` bytes (pre-fix `4882` / `873199`; +3 net lines).
- `git diff --numstat -- docs/14-production-data-model-and-flyway-schema-spec.md`: `55` insertions, `52` deletions.
- `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md`: exit `0`, no diagnostics.
- `file`: UTF-8 Unicode text. First bytes `od -An -tx1 -N3`: `23 20 41` (no BOM). Final bytes `od -An -tx1`: `64 0a` (exactly one final LF). CR count via `tail -n +1 ... | tr -cd '\r' | wc -c`: `0`.
- Trailing-whitespace scan of the specification: `0` matching lines.
- Read-only count reconstruction for the frozen shape and structural script:
  - dictionary headings `44`; dictionary column rows `522`; one `Purpose: ... Lifecycle:` and one `Sensitive representation:` per dictionary (`44` each); `Revision/immutability`/`Append-only control` coverage `44`.
  - invariant rows `40` (S1 expansion `14`, S2 `22`, S3 `5`).
  - wave rows `9`; slice membership rows `13`/`23`/`8` (total `44`).
  - lifecycle matrix `MUTABLE_HEAD=28`, `APPEND_ONLY=16`; sensitive inventory rows `28`.
  - FK rows `81` = `74` plain `FK` + `3` `ALTER_FK` + `2` `DEFERRABLE_FK` + `2` `NO_FK` (the raw plain-FK text count is `75` because the embedded checker source itself contains `'| FK |'`).
  - candidate-key rows `24`; function-wave rows `5` (four V001 + one V006).
  - `S2 status is **accepted at checkpoint \`8934df9\`**` and `S3 status is **accepted at checkpoint \`9ad33e8\`**` each occur once; the accepted aggregate checkpoint `2002cc9c2bff89600588a8d437cd894869f08c61` is recorded.
  - `ivr.lock_reconciliation_caller` occurs `15` times; `ticketExpiresAt = min(clock_timestamp() + approved Q22 ticket TTL, sessionExpiresAt, admissionExpiresAt, current grant expires_at)` occurs `3` times; `EVIDENCE_SOURCE_CONFLICT` occurs `9` times; `source_event_id = eventId` occurs `3` times; `ORIGINAL = 0` occurs `10` times.
  - absent: `pending independent aggregate re-review` (`0`), `evidence-created:` (`0`), `ticket TTL is the minimum` (`0`), `next gate is` (`0`).
- Frozen input hashes: plan `149472D291DC19F81497A0599C85122BDDA311DC92417C3E2086667130B231A5`; plan-fix-6 `8976F64A215A2B7B5CB116A14D5123AFF91F754F512B6FD212E0D8C0C7045F59`. Both match.
- Scope: pre-edit and post-edit `git status --porcelain` differ only by this specification modification and this new artifact; the pre-existing controller modification, screenshots, `logo/preview.html` and untracked historical reviews remain byte-identical.

### Reconstruction limitation

- The required `/tmp/a0s1/structural-v2-run.sh` execution was attempted with `FILE=... FIX2=... bash /tmp/a0s1/structural-v2-run.sh`, but this harness denies access to `/tmp/a0s1` and denies shell-interpreter, `awk`, `sed`, `grep`-via-Bash and PowerShell execution. The same script text is embedded verbatim in the accepted plan (`§13.3`, lines 1109-1374), and every frozen block it reads is byte-unchanged. Its expected result is reconstructed from those unchanged blocks and the counts above: `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`; exit `0`. This is a reconstructed result, not an executed one.
- The exact 44-table/522-column PowerShell checker was not executed for the same reason; its structural assertions are reconstructed by the `44` heading and `522` column counts, the unchanged per-table column-name/order rows (the only dictionary edit is a Notes cell in `§4.41`), and the unchanged nine frozen blocks. `SliceColumns S1=121/S2=307/S3=94` is preserved because no dictionary column row was added, removed or renamed.
- The ten required embedded blocks were not executed as shell programs; their counts, section boundaries, status phrases, required terms and absence checks were reconstructed with read-only Grep/Read/wc/od/git checks. The only embedded-check text changed is the `§13.1` required-term list directly affected by PR-01/PR-02; no count or expected value was weakened, and no dictionary/range check was altered.

## Docs decision

- Structural/docs gate: reconstructed PASS (document-only) for the frozen 44/522 (121/307/94), 40 invariants, nine waves, slices 13/23/8, FK 74/3/2/2 = 81, lifecycle 28/16, sensitive 28, keys 24, nine frozen blocks, ten embedded-block-equivalent checks, encoding/whitespace and two-path scope. The structural script and exact checker were not executed due to the harness limitation above; no runtime or CI result is claimed.
- Semantic/docs readiness: author-reported fixed-current for PR-01..PR-05 and OQ-PR-01. Independent PR re-review remains mandatory and controller-owned; no PR acceptance, re-review pass, merge, runtime readiness or production readiness is claimed.

## Residual risks and owners/destinations

| Residual | Classification | Owner / destination |
| --- | --- | --- |
| Executable PostgreSQL 16/Flyway migration, roles/privileges, triggers/deferred constraints, row locks/savepoints/READ COMMITTED concurrency and category-1/2/3 behavior, including the new `ivr.lock_reconciliation_caller` role-switch/privilege/deadlock path | later-work-unit; not executed here | Backend/Database/Platform/Security/IVR + Testcontainers; implementation/migration/Testcontainers gate |
| Structural script, exact 44/522 checker and ten embedded blocks must be rerun in an environment that permits `/tmp`, shell and PowerShell execution | validation-tooling limitation of this run; not a semantic defect | Controller/independent PR re-review; unrestricted validation environment |
| Stable `eventId` uniqueness/validation at the upstream evidence-creation interface and executable same-identity conflict/replay tests | current-scope contract fixed in text; executable proof pending | Evidence/Backend/IVR; S3 recording/evidence tests |
| Canonical-JSON artifact rank ordering across implementations and final wire/content encoding | text fixed; executable/wire proof pending | Evidence/Sharing/Backend; Q15/Q26 and S3 sharing tests |
| Authoritative IVR reconciliation implementation that converges `UNKNOWN` to `ENDED`/`FAILED` and executable non-authoritative-terminal CHECK tests | text fixed; executable proof pending | IVR/Backend; IVR protocol and S3/S2 call-event tests |
| Approved Q22 ticket TTL, clock-skew and revocation-window values; Redis TTL/clock behavior | deferred security/product input; no value selected | Security/Sharing/Backend; Q22 security design |
| COS object version/hash validation, KMS/envelope, WORM/object-lock and media/malware/duration probing | later-work-unit | Infrastructure/Security/Backend; Q2/Q11 |
| OTP/SMS proof, KDF parameters, admission/expiry defaults and rate limits | later-work-unit | Security/Product/Sharing; Q12 |
| Wire/OpenAPI encoding, IVR protocol mapping, JSON-schema version | later-work-unit | Backend/IVR/Security; Q15/Q26 |
| Retention/erasure/backup deletion/legal hold | later-work-unit | Legal/Security; Q3/Q11/Q13 |
| IVR-local B-leg unique operation and lost-response recovery | later-work-unit | Telephony/IVR; Q28 |
| Production prices/quantities/trial wording, due-date/grace, tolerance, allocation policy, task-access role matrix and call-blocking risk | later-work-unit | Product/Finance/Operations/Risk/IVR; Q4/Q5/Q8/Q10/Q20/Q24/Q25/Q27 |
| Deployment role provisioning and whether the deployment identity may create `SECURITY DEFINER` functions/triggers | deferred deployment input | Platform/Database/Security; Q17/Q23 |

No unclassified residual risk remains. The five findings and the ticket-expiry clarification are current-scope and fixed in the specification text; executable, runtime, CI and production proof is not claimed.

## Limitations and stop status

- No PostgreSQL 16, Flyway, DDL, role/privilege, trigger, deferrable constraint, row lock, savepoint, READ COMMITTED concurrency, COS/KMS/WORM, Redis/media gateway, IVR line, payment provider, Testcontainers, CI or production execution occurred.
- Document checks prove structure, frozen equality, required-term presence and cleanliness only; they do not prove database, transaction, privilege, concurrency or external-system behavior.
- No controller, PR review, accepted plan/fix-6, historical artifact, checker, source, prototype, screenshot or Git metadata was modified. No stage, commit, push, PR mutation, issue change or later gate was performed.
- Stopped after this bounded fix artifact and validation report. Independent PR re-review is the next controller-owned gate.

## Artifact path

`docs/gateflow/a0-production-data-model-schema-spec-pr-fix-1.md`

## Completion status

Author-complete for the bounded PR fix. Independent PR re-review has not occurred and must not be inferred from this artifact.
