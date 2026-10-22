# A0 aggregate fix 2 completion record

- Task label: `a0-spec-aggregate-fix2-ds-20261023-01`
- Gate: `fix` (aggregate round 2) for `A0-production-data-model-schema-spec`
- Runtime/provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-23
- Status: author-complete; stopped after this artifact and document validation. Independent aggregate re-review is required before any acceptance, commit, push or PR.

## Binding inputs

- HEAD / accepted S3 checkpoint: `9ad33e8727596366c0a844b7dffcce3eb028891b`.
- Pre-fix specification: `docs/14-production-data-model-and-flyway-schema-spec.md`, SHA256 `6767D10CBD59574C83B9BE5B358D8F0B4FBF0D6554B05879DC21AB1EA470DBC8`.
- Aggregate fix 1: `docs/gateflow/a0-production-data-model-schema-spec-aggregate-fix-1.md`, SHA256 `65EC60EB0F09426C2899954A143A81AFD685EC1E7EED0923B87A61E369DB9064`.
- Independent re-review: `docs/reviews/code-review-20261023-000946.md`, SHA256 `3960781081D1D552EF67635EC6EA2D837BBEB734C1566AEBE59DAC0AEE866460`.
- Controller: `docs/gateflow/project-execution-control.md`, SHA256 `0EC9AC9C1BED80C55F7E0B03146F67C882CA1E5812C7A40EBEC5EDBF8DD632EB`; the section `Aggregate fix 1 independent re-review and fix 2 adjudication` is binding.
- Read-only context: the accepted plan and fix-6, `docs/12-technical-stack-and-system-architecture.md`, `docs/13-ivr-call-task-and-inbound-design.md`, and the current fingerprint, service and test sections.
- Scope: specification-only. No executable SQL, migration, runtime, API, UI, cloud, credential or product-policy change.

## Write method and scope

- All repository edits used the verified Windows fallback `/mnt/c/Users/Administrator/AppData/Local/OpenAI/Codex/bin/896a0166b774037a/codex.exe --codex-run-as-apply-patch` with one safely quoted patch argument and Windows absolute targets. Native `apply_patch` was unavailable; no Edit/Write/Python/shell redirection was used for a repository file.
- Changed paths: `docs/14-production-data-model-and-flyway-schema-spec.md` (modified) and this artifact (new). No other task-owned path was written.
- The controller, accepted plan/fix-6, historical reviews/artifacts, checkers, source, prototypes, screenshots, `logo/preview.html` and Git metadata remain read-only. Pre-existing dirty files were preserved.
- No stage, commit, push, PR, merge, issue modification, install, network write, credential/provider/WorkBuddy change or later gate was performed.

## Exact corrections

All items below are author-reported fixed-current corrections; this is not independent acceptance.

| Correction | Disposition and current location |
| --- | --- |
| 1. Collision-safe access identity (F-01/F-02) | `§2.5` defines `accessNumberHmac` as the lowercase 64-hex HMAC-SHA-256, under the same stable approved phone-number HMAC key and contract as the other caller/number HMACs, of the authenticated canonical normalized access number. It is an ephemeral fingerprint input only: only `request_fingerprint` is stored; it is never a new table/column/response field and is never persisted separately, logged, audited, emitted, snapshotted or returned. Missing/invalid HMAC input fails closed before effects. `§9.11` step 2, `§9.13` step 2, `§9.14` step 1 and the `§10.17` canonicalization bullet plus the `IVR_CONFIRM`/`ACTIVATION_NEXT_ACTION`, `INBOUND_RESOLVE` and `DIRECT_AUTHORIZATION` rows use `accessNumberHmac` instead of `accessNumberMasked` as the access identity. `§4.24`/`§4.25` `access_number_masked` descriptors now state display-only, non-unique and non-authoritative for replay. A same canonical access number with otherwise identical semantics, including a regenerated transport event ID, replays; a different canonical access number with an identical mask conflicts before frozen replay. `T-S2-19`, `T-S2-58`, `T-S2-60` and `T-S2-61` assert the same-mask conflict, HMAC-only identity and fail-closed input. No key-version column, key policy or schema object was added. |
| 2. OQ-R1 replay projection ordering | `§2.12` adds the share-creation replay ordering bullet; `§9.29` replaces the step-1 wording and adds the replay projection paragraph; `§4.44` aligns the creation/state-control note; `§10.23` `evidence.read_shareable_projection` and `sharing.create_share_grant` state the same order; `T-S3-15` asserts it. For an existing creation identity the service calls the read-only same-connection evidence projection before recomputing/comparing the frozen creation fingerprint, just as for new creation, but before any sharing state-dependent mutation. It reconstructs the deterministic server-derived `content` from the projection plus current validated request parameters. Exact immutable `SOLIDIFIED` facts with otherwise same request semantics replay metadata only; changed/inconsistent artifact/media/content facts under the same identity return `SHARE_CREATION_CONFLICT`; absent/unresolvable/not-`SOLIDIFIED` projection fails closed with the existing typed error and no mutation/token/event. The collaborator stays read-only, same-connection and owner-only, with no foreign row lock and no access/risk policy redesign. |
| 3. OQ-R2 request-key digest | `§2.12` and `§9.30` explicitly define every `requestKeyDigest` used by `share-observation:` and `share-access:` identities as the lowercase hexadecimal SHA-256 of the exact stable nonblank request/action key, validated at 1-128 UTF-8 code points with no Unicode normalization and no case folding. Raw keys are never stored, logged, audited or emitted. The declared 82-character `share-observation:` identity length, the `share-access:` identity formula and the separate payload fingerprints are unchanged. `T-S3-19` asserts the definition. |
| 4. OQ-R3 upload replay ordering | `§10.23` `evidence.record_upload_observed` and the `§9.27` upload-observation paragraph state the ordering; `T-S3-32` asserts it. The service first derives/resolves the deterministic recording/object identity and immutable observation fingerprint. An existing identical observation returns the first committed `UPLOADED` result before any current recording-status precondition and mutates nothing; an existing same identity with a changed fingerprint returns `RECORDING_IDENTITY_CONFLICT` and mutates nothing. Only a genuinely new observation requires current `DECLARED`, locks the row, re-reads it and performs one `DECLARED -> UPLOADED` update. Object/network validation remains before the PostgreSQL transaction and lock, and only `UPLOADED` proceeds to validation/quarantine. |

## Non-regression and status metadata

- F-03..F-11 and OQ-1/OQ-4/OQ-5/OQ-6 remain independently closed and were not materially changed. OQ-2 and OQ-3 remain deferred; OQ-7 remains closed by F-08.
- The header task/gate/status/controller labels, `T-S2-65` and the `§14`/`§15.2` notes now record aggregate fix 2 pending independent aggregate re-review. No historical artifact was rewritten.
- No table, column, SQL function, role, action, wave, slice, invariant, key set or product policy was added. The frozen 44 tables / 522 columns (121/307/94), 40 invariants, nine waves, slices 13/23/8, FK 74/3/2/2 = 81, lifecycle 28/16, sensitive 28, keys 24 and five function-wave rows are preserved.

## Changed files and hashes

- `docs/14-production-data-model-and-flyway-schema-spec.md`: modified; 873199 bytes / 4882 lines; SHA256 `7EC9540389D5EFE42468FF64C438B46EEFEB6FC495B1694776A7C60FFD681FC1`.
- `docs/gateflow/a0-production-data-model-schema-spec-aggregate-fix-2.md`: this new artifact. Its own SHA256 is reported by the author final response after creation; a file cannot embed its own final digest.

## Validation commands and actual results

- `sha256sum /tmp/a0s1/structural-v2-run.sh` returned `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545`, matching the required hash.
- `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` returned exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`.
- Exact read-only checker `C:\Users\Administrator\AppData\Local\Temp\onecall-a0-spec-20261022-175451\check-s3-document.ps1` returned exit `0`; `FrozenBlocks=9`, `Dictionaries=44`, `Columns=522`, `SliceColumns S1=121/S2=307/S3=94`, `Bytes=873199`, `SHA256=7EC9540389D5EFE42468FF64C438B46EEFEB6FC495B1694776A7C60FFD681FC1`, `Errors=[]`.
- The ten current required embedded blocks (`§13.1` x1, `§13.2` x2, `§13.2.1` x2, `§13.2.2` x2, `§13.5` x1, `§13.6` x1, `§13.8` x1) were extracted from the current document and executed; every block returned exit `0`. The dictionary sections report `ALL_EXACT` for S1 121, S2 307 and S3 94 columns; `§13.6` retains its expected counts. `§13.1` now includes an added required-term check for `accessNumberHmac`; no existing check, count or expected value was weakened or removed.
- Encoding/cleanliness: first bytes `23 20 41` (UTF-8, no BOM); CR count `0`; trailing-whitespace lines `0`; the document ends in exactly one LF; `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` returned exit `0`.
- Scope: `git status --porcelain` shows only the specification modified and this artifact added by this work unit, alongside the pre-existing controller modification, pre-existing aggregate fix 1/review untracked files, pre-existing screenshots and `logo/preview.html`. No Git metadata was changed.

## Docs decision

- Structural/docs gate: PASS (document-only) for the frozen 44/40/9/3 shape, 522 columns, nine frozen blocks, ten required embedded blocks, encoding, whitespace and two-path scope.
- Semantic/docs readiness: author-reported only. Independent aggregate re-review must decide whether the four corrections are exact and whether any regression remains.
- No aggregate acceptance, accepted deepreview commit, push/PR, full A0 completion, runtime readiness or production readiness is claimed.

## Residual risks and owners/destinations

| Residual | Classification | Owner / destination |
| --- | --- | --- |
| Executable PostgreSQL 16/Flyway migration, role provisioning, schema/table/column/function privileges, trigger/deferred-constraint syntax, row-lock/savepoint/READ COMMITTED concurrency and category-1/2/3 behavior | later-work-unit; not executed here | Backend/Database/Platform/Security + Testcontainers; implementation/migration/Testcontainers gate |
| COS object version/hash validation, KMS/envelope, WORM/object-lock and media/malware/duration probing | later-work-unit; not executed here | Infrastructure/Security/Backend; storage/security/capacity work (Q2/Q11) |
| Redis ticket TTL/cache loss/revocation propagation/clock skew | later-work-unit; not executed here | Security/Sharing/Backend; Q22/security design |
| OTP/SMS proof, KDF parameters, admission limits/expiry defaults and rate limits | later-work-unit; not executed here | Security/Product/Sharing; Q12/security+sharing policy |
| Wire/OpenAPI encoding, IVR protocol mapping, JSON-schema version and final content-member encoding | later-work-unit; not executed here | Backend/IVR/Security; Q15/Q26 |
| Retention/erasure/backup deletion/legal hold | later-work-unit; not executed here | Legal/Security; Q3/Q11/Q13 |
| IVR-local B-leg unique operation/lost-response recovery | later-work-unit; not executed here | Telephony/IVR; Q28 |
| Production prices/quantities/trial wording, due-date/grace, tolerance, allocation policy, task-access role matrix and call-blocking risk | later-work-unit; not executed here | Product/Finance/Operations/Risk/IVR; Q4/Q5/Q8/Q10/Q20/Q24/Q25/Q27 |
| Deployment role provisioning and whether the deployment identity may create `SECURITY DEFINER` functions/triggers | later-work-unit; not executed here | Platform/Database/Security; Q17/Q23 |
| F-01/F-02 access identity | fixed-current in author text; independent acceptance still pending | Spec author + IVR/Subject-number/Backend; aggregate re-review |
| Sharing replay projection, request-key digest and upload replay ordering | fixed-current in author text; independent acceptance still pending | Sharing/Evidence/Backend; aggregate re-review |

No unclassified residual risk or external issue remains for the fixed-current findings. These risks are not defects proven by this fix and are not claimed as implemented, tested or production-ready.

## Limitations and stop status

- No PostgreSQL 16, Flyway, DDL, role/privilege, trigger, deferrable constraint, row lock, savepoint, READ COMMITTED concurrency, COS/KMS/WORM, Redis/media gateway, IVR line, payment provider, Testcontainers or production execution occurred.
- Structural/embedded checks prove document structure, membership, frozen equality, required-term presence and cleanliness only; they do not prove database, transaction, privilege, concurrency or external-system behavior.
- No controller, accepted plan, historical artifact, checker, source, prototype, screenshot or Git metadata was modified. No stage, commit, push, PR, merge, issue modification, install, network write, credential/provider/WorkBuddy change or later gate was performed.
- Stopped after this bounded fix artifact and validation report. Independent aggregate re-review is mandatory before any aggregate acceptance or remote delivery.
