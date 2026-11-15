# PA34 activation-history cumulative plan — fix 1

Task: `pa34-operations-activation-history-plan-fix1-gpt-20261115-01`.

Date: 2026-11-15 (Asia/Shanghai). Work unit: `PA34-operations-activation-history-read-foundation`. Gate: **fix only**.

Managed worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa34-next-work-unit/codex-call`.

## 1. Authority, inputs and completion boundary

The binding PA34 Goal Confirmation and accepted fix decisions in `docs/gateflow/project-execution-control.md` remain unchanged. This fix modifies only the cumulative plan and creates this evidence artifact. It does not authorize implementation, declare either review passed, schedule re-review, claim acceptance or claim production readiness. The Controller owns the next fresh peer-isolated A/B re-review.

Inputs read directly: the PA34 Control block, the complete cumulative plan, both complete final reviews, the plan's 21-entry frozen raw-byte manifest, relevant frozen OpenAPI/docs/12/14/15/17 and V007 constraints/grants, current shared read properties/binder/codec/context/service/configuration, Operations/Customer boundaries, and existing source/test/fixture/POM/CI inventory and log/snapshot patterns. Every frozen manifest file was read as raw bytes for SHA-256 verification. The original MiMo B provider failure produced no artifact; the B fallback below is the independent B input, not a replacement peer review generated here.

| Input | Raw SHA-256 at fix start; unchanged at completion |
| --- | --- |
| Control | `c45579e34248574697deb69a222a1d3bed3a65060b51b8a60a0dfdc4d90743fd` |
| A: `docs/reviews/plan-review-20261115-033925.md` | `cd1cadcf68eb86a36ee049c0a6fba9b944c4abd5074125a480d8203db289ef76` |
| B fallback: `docs/reviews/plan-review-20261115-035649.md` | `00a59c3d094049175253959192b3efbc4c284407a5c3e9e872b1020253553d97` |

The current goal remains exactly one `GET /api/v1/operations/activations` / `listOperationsActivations`, a three-field `ActivationPage`, an eleven-field `ActivationSummary`, ACTIVE ADMIN/OPS employee authority, stable cursor pagination and one read-only owner-local snapshot. No endpoint, filter, field, table, migration, grant, role, dependency or readiness claim is added.

## 2. Exact cumulative-plan identity

Path: `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md`.

| Measurement | Before fix (reviewed input) | After fix |
| --- | --- | --- |
| Raw SHA-256 | `dfcc2c6f1dbf1f48c55e6d2fc2747306e8c899352f5bb51429d40248cc305730` | `5f1e649c4234210f5a1a40e3597658b5ab1419dd4cbf029784ae2170533c5662` |
| Bytes | 76,108 | 83,335 |
| Lines (LF count) | 585 | 574 |

Delta: **+7,227 bytes / -11 lines**. Hashes are over exact raw bytes, not normalized alternatives. This fix artifact's own final hash/bytes/lines are reported outside the artifacts to avoid a self-referential digest.

## 3. Finding-by-finding disposition

All four findings are **fixed in the cumulative plan; independent re-review pending**. These are author fix dispositions, not Controller acceptance or runtime results.

### PA34-PLAN-REV-01 — Low — transition legality layers

- Sections3, 4.3 and6 now distinguish V007's nine DB-persistable shapes from docs/14's five domain-legal shapes. The frozen database CHECKs are not changed or treated as the narrower domain oracle.
- Successful shapes: INACTIVE→ACTIVE via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION; ACTIVE→SUSPENDED via OPERATIONS/OPERATIONS; SUSPENDED→ACTIVE via OPERATIONS/OPERATIONS.
- Rejected DB-shape-only combinations: INACTIVE→ACTIVE via OPERATIONS/OPERATIONS; ACTIVE→SUSPENDED via IVR/IVR_CONFIRM, MINIAPP/ACTIVATION or WEB/ACTIVATION. Each is an integrity failure of the entire page, including when encountered only as lookahead: sanitized503, no dropping rows or partial response.
- Existing unit identities `allPersistableStatusChannelAndDomainCombinationsProjectExactly` and `nullMalformedAndInvalidLookaheadRowsFailClosed` classify/inject the rejected four. Existing IT identity `allNinePersistableTransitionCombinationsReturnAllEnums` retains its name but now explicitly means five successful shapes plus four negative classifications, not nine successful outputs.
- Real PostgreSQL negative fixtures satisfy all unchanged V007 FK/actor/consent/unique/CHECK constraints. They are isolated from successful fixtures and tested beside valid rows and as lookahead. Constraint-impossible malformed facts use raw-port unit/MockMvc probes; constraints/triggers are never disabled. No migration edit is needed or authorized.

### PA34-PLAN-REV-02 — Medium — raw facts and exclusive validation ownership

- Sections3/4.1 define the port return as `List<TransitionFacts>`, never public wire `ActivationSummary`. The new carrier is nested in the already-planned port file, so it adds no file.
- Exact carrier components: `UUID transitionId`, `UUID subjectId`, `String statusEventId`, `String fromStatus`, `String toStatus`, `Integer fromRevision`, `Integer toRevision`, `String sourceChannel`, `String sourceActionDomain`, `String reasonCode`, `Instant occurredAt`. All eleven values may be null; no carrier semantic constructor validation or unboxing prevents injection. Its diagnostic representation is redacted.
- Owner reader only maps SQL columns, using nullable `Integer` reads and nullable timestamp conversion. It neither turns NULL into zero nor validates field/domain invariants nor constructs wire summaries. Connection/role/query/conversion failures remain dependency503.
- The service exclusively validates raw facts, nulls, revisions/overflow, enums/domain shapes, code-point bounds, time precision, ordering/seek, uniqueness and bounded page count, including every lookahead row. It constructs public summaries only after validation. Public `fromRevision` and `toRevision` are primitive `int`; boxed values remain only at the raw boundary.
- The existing malformed-row unit method now explicitly stubs the port with invalid raw facts, mixes good and invalid rows, tests every nullable field and proves whole-page failure. The same stub is exercised through MockMvc for sanitized503 and absence of partial item/cursor output. Reflection/source assertions pin both types and validation ownership. No test method is added or renamed.

### PA34-PLAN-REV-03 — Medium — existing read cursor key ring

- Sections1/3/4/5/6/10/11 now require reuse of existing `operationsSubjectReadProperties`, `OperationsSubjectReadProperties.Cursor.validatedKeys()` and `KeyEntry.bytes()` with the unchanged shared binder, precedence, indexed-key checks and redaction. The service uses `ObjectProvider<OperationsSubjectReadProperties>` after authority/query validation; it does not instantiate the subject service/codec.
- The rejected activation-specific cursor prefix and property class are removed from the cumulative plan. There is no replacement property class, binding bean, fallback prefix, operator-visible secret or unrelated deployment dependency. Only `onecall.operations-subject-read.cursor.*` supplies the ring. Missing/invalid shared material fails closed503 even on first/empty pages.
- Codec purpose remains exactly `onecall:operations-activation-list:cursor:v1\0`; sort anchor remains exactly `occurredAt desc, transitionId desc`. AAD order is purpose bytes, exact version/key-ID header, UTF-8 sort bytes terminated by NUL, then 32 raw SHA-256 bytes of UTF-8 `{}` (canonical empty filter object without BOM/newline). This satisfies docs/15's normalized-filter-digest convention without a client filter or mismatch branch.
- The fixed digest/sort live in AAD, not the payload. Anchor28 + tag16 + nonce12 + header2 + key-ID length = `58 + keyIdLength` binary bytes; maximum90 bytes/120 unpadded base64url characters. No count change is hidden by the digest addition. No binding to `limit`, employee, expiry or anchor existence is introduced.
- Existing codec/configuration test methods now prove same-ring PA33 subject tokens and activation tokens are mutually invalid as `CURSOR_INVALID`, plus wrong sort/empty digest/purpose AAD failures. They prove shared-ring sufficiency, activation-style keys' insufficiency, absence of a separate class/binder/operator secret and shared malformed-key/first/empty failure behavior. Legacy method names are retained for exact CI identities; corrected assertion cells are authoritative.
- Section5 deletes the proposed property-file row and renumbers all following future S1 rows. No replacement file is introduced; exact arithmetic is in section5 of this artifact.

### PA34-PLAN-REV-04 — Medium — captured-log canaries

- Existing `OperationsActivationReadsIT.onlyTransitionFactsAreReadAndNoHiddenFieldsLeak` now captures logs across success, malformed cursor400, tampered cursor400, dependency503, domain-integrity503 and empty200 paths.
- Assertions exclude canary cursor ciphertext/material, employee UUID/identity, SQL text/JDBC URL/detail, key material and raw sensitive values from captured logs and applicable responses/problems. Capture inspects rendered messages, argument/MDC values and throwable/stack detail, not only formatted messages.
- A harmless test marker proves capture is active; sensitive canaries are not deliberately logged. Fixture setup/direct-SQL constraint probes stay outside request-path capture. Existing logging infrastructure is used without a dependency, with listener/appender cleanup in finally.
- There are still exactly18 methods in this IT class and10 in the role IT class:18+10=28. No nineteenth method, substitute padding class or replacement test set is planned.

## 4. Accepted clarifications

| Clarification | Cumulative-plan disposition |
| --- | --- |
| docs/15 normalized-filter digest | Section4.2 pins SHA-256 of canonical UTF-8 `{}` in authenticated cursor input, with exact sort/purpose; no `CURSOR_FILTER_MISMATCH` is exposed. |
| Public versus raw revision types | Section3 mapping, section4.1 signatures and section6 reflection/malformed-row assertions pin public primitive `int` versus nullable raw `Integer`. |
| Same observed database snapshot | Sections4.4/6 require test-only direct-JDBC `txid_current_snapshot()` observations on the same transaction-bound connection at authority and transition SELECT boundaries; assert equal snapshots/backend/connection. Existing unit orchestration and G/H IT methods own the evidence. Probe re-entry is guarded and cleanup is in finally; no production query/connection or new method is added. |
| Unicode string bounds | Section3 states OpenAPI `maxLength` counts Unicode code points, not Java UTF-16 units. Existing unit/IT cases cover astral characters and code-point boundaries; PostgreSQL U+0020 `btrim` semantics remain unchanged. |

## 5. Exact affected-path and inventory arithmetic

The cumulative plan remains a single S1 slice. The deleted future production path is `backend/src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadProperties.java`. It was old row04; old05–25 become new04–24. Existing property/binder/config source stays read-only and is reused, not copied or modified.

| Manifest measure | Before | After |
| --- | --- | --- |
| Future S1 implementation rows, including Controller checkpoint | 25 | 24 |
| Future new files | 20 | 19 |
| Future existing files | 5 | 5 |
| Implementer-owned future paths, excluding Controller checkpoint | 24 | 23 |
| New production Java | 9 | 8 |
| New test/helper Java | 10 | 10 |
| Plan plus future S1 paths | 26 (21 new/5 existing relative to merged PA33) | 25 (20 new/5 existing relative to merged PA33) |
| Additional fix evidence artifact, outside S1 | 0 | 1 |
| Plan, fix artifact and future S1 combined | 26 (21 new/5 existing) | 26 (21 new/5 existing) |

The final combined count staying26 does not hide the removal: S1 alone decreases by exactly one future row and one new file, and the separately authorized fix report adds one documentation artifact outside S1. The raw carrier is nested; no replacement implementation path is necessary.

Exact final S1 manifest follows. Prefix `M` means `backend/src/main/java/com/onecall/core/`; `T` means `backend/src/test/java/`. Concatenation yields the complete literal path; no glob is an allowlist entry.

| Final ID | State | Exact path |
| --- | --- | --- |
| 01 | new | M operations_iam/subjectread/OperationsActivationReadService.java |
| 02 | new | M operations_iam/subjectread/OperationsActivationReadPorts.java |
| 03 | new | M operations_iam/internal/activationread/OperationsActivationReadRequest.java |
| 04 | new | M operations_iam/internal/activationread/OperationsActivationCursorCodec.java |
| 05 | new | M operations_iam/internal/activationread/OperationsActivationReadConfiguration.java |
| 06 | new | M operations_iam/internal/activationread/DefaultOperationsActivationReadService.java |
| 07 | new | M operations_iam/internal/web/OperationsActivationReadController.java |
| 08 | new | M activation_consent/internal/operationsread/JooqOperationsActivationHistoryReader.java |
| 09 | existing | M operations_iam/internal/web/OperationsBoundaryFilter.java |
| 10 | existing | M customer_iam/internal/web/CustomerBoundaryFilter.java |
| 11 | new | T com/onecall/archtests/OperationsActivationReadWebTests.java |
| 12 | new | T com/onecall/archtests/OperationsActivationReadConfigurationTests.java |
| 13 | new | T com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadRequestTests.java |
| 14 | new | T com/onecall/core/operations_iam/internal/activationread/OperationsActivationCursorCodecTests.java |
| 15 | new | T com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadServiceTests.java |
| 16 | new | T com/onecall/archtests/architecture/OperationsActivationReadBoundaryTests.java |
| 17 | new | T com/onecall/archtests/operations/OperationsActivationReadTestProviders.java |
| 18 | new | T com/onecall/archtests/container/OperationsActivationReadsIT.java |
| 19 | new | T com/onecall/archtests/container/OperationsActivationReadRoleLeastPrivilegeIT.java |
| 20 | new | T com/onecall/archtests/container/OperationsActivationReadFixture.java |
| 21 | existing | backend/pom.xml |
| 22 | existing | .github/workflows/ci.yml |
| 23 | existing; Controller-owned | docs/gateflow/project-execution-control.md |
| 24 | new | docs/gateflow/pa34-operations-activation-history-read-foundation-implementation.md |

Rows01–08 are the exact new production set; rows09–10 are the existing filters. The activationread package has exactly four new filenames, rows03–06. The sole reader maps raw facts without creating a new module edge. Existing named interfaces/module maps, owner adapters and the 12-edge target multiset remain unchanged.

| Planned test class | Methods, unchanged |
| --- | --- |
| A: OperationsActivationReadWebTests | 12 |
| B: OperationsActivationReadConfigurationTests | 8 |
| C: OperationsActivationReadRequestTests | 8 |
| D: OperationsActivationCursorCodecTests | 10 |
| E: OperationsActivationReadServiceTests | 14 |
| F: OperationsActivationReadBoundaryTests | 8 |
| G: OperationsActivationReadsIT | 18 |
| H: OperationsActivationReadRoleLeastPrivilegeIT | 10 |

Static parsing confirms all88 unique class/method identities remain in their existing eight manifests; no methods were added, removed or renamed. A–F:12+8+8+10+14+8=60. G–H:18+10=28. Helpers retain zero test annotations. Direct current-source counting reproduces70/644 Surefire +29/476 Failsafe =99/1120, token-line1121 and29 unique POM Failsafe includes. Planned final inventory stays **76/704 +31/504 =107/1208**, token-line **1209**, POM includes **31**. Source=CI=POM=XML verification and historical subtraction-chain instructions remain implementation requirements, not runtime evidence from this fix.

## 6. Static validation and preservation

All checks below are read-only, except writing the two authorized Markdown artifacts through patches. No private helper/manifest, temporary output, target or tmp artifact is written.

| Check | Result |
| --- | --- |
| Direct raw-byte verification of the plan's21 unique SHA-256 manifest entries | PASS, Python exit0; all21 match. Includes OpenAPI, docs/14/15/17 and all17 migrations. |
| Migration filename/version equality | PASS, exit0; exact manifest filename set, versions001–017 inclusive,17 SQL files, no V018 or additional SQL migration. |
| Exact future path manifest | PASS, exit0; sequential01–24,24 unique resolved paths,19 new absent paths/5 existing paths,8 new production and10 new test/helper Java. |
| Exact test manifests and inventory | PASS, exit0; A–H counts12/8/8/10/14/8/18/10;60+28=88. Current source/POM arithmetic reproduced statically; no test executed. |
| Stale-design checks | PASS, exit0; cumulative plan contains neither removed property-class name nor removed activation cursor prefix; port seam no longer returns public summary. |
| Cursor arithmetic/static input | PASS, exit0; exact purpose/sort-with-NUL strings present; canonical empty digest fixed in AAD;28-byte anchor/44-byte ciphertext/58+ID frame unchanged; no limit binding. |
| Conflict markers, U+FFFD, trailing whitespace and Markdown fence balance | PASS, exit0; both authorized artifacts checked as UTF-8; zero findings. |
| Tracked `git diff --check` | PASS, exit0; no diagnostics. |
| Untracked P/F whitespace checks | `git -c core.autocrlf=false diff --no-index --check /dev/null <artifact>`: each exit1 for differing untracked content, with zero stdout/stderr diagnostics; no whitespace errors. No index/config write occurs. |
| Changed-path scope | PASS; only the plan is modified by this fix and this fix artifact is newly created. Existing tracked diff remains only the preexisting Control modification; its fix-start raw hash is unchanged. No other untracked path is added. |
| Review/Control/prompt baseline preservation | PASS; Control and both review hashes match section1; all five preexisting prompt hashes match fix-start values; `.codex` stays zero bytes. |

Plain Linux Git cannot resolve this worktree's Windows `D:/...` gitdir pointer. Read-only status/diff commands use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call18`, `GIT_COMMON_DIR=/mnt/d/codex-call/.git`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`. The pointer, configuration, index and Git metadata are not repaired or written. An initial no-index check emitted only the existing autocrlf warning; the final transient `-c core.autocrlf=false` invocation suppresses that warning without changing raw files or stored config. Exit1 on no-index difference is not represented as an exit0 check.

For preservation, the five prompt hashes retained are:

| Preexisting read-only path | Raw SHA-256 |
| --- | --- |
| `.pa34-plan-fix1-prompt.md` | `19f99edbe67aa72ca76086b3aa9a2c4be216d1a9bb6abe6a00889a909ab1f613` |
| `.pa34-plan-prompt.md` | `93a3416ddd275a6e7e3d2a5aefd0059f3f3b528bc55afe73d758c321b7822137` |
| `.pa34-planreview-A-prompt.md` | `2ee36b5bc8f5063a94494dc38154701a57a01134ba461c94f0bdbd1e64a2ebc8` |
| `.pa34-planreview-B-prompt.md` | `a512d3cbcaeff6c2b63f4984395472fe0c4a6b3a3871b97332408c38bd6d23f9` |
| `.pa34-planreview-B2-prompt.md` | `dd3ad9c10c1a6bdc700fb6ce482e2e5754077f15e6bfb9637073cde6d5ae307f` |

## 7. Residual risks and destinations

| Residual | Classification / owner | Destination and boundary |
| --- | --- | --- |
| Ledger-wide occurredAt/id sort lacks a matching global V007 index | Bounded performance residual; Backend/DBA | Implementation report and deployment-capacity/production-readiness assessment; limit+1 with timeout/budget can fail503 at scale. No index or migration is added. A required schema/grant change needs renewed authorization. |
| Snapshot-relative employee revocation and fresh continuation snapshots | Accepted semantics; Backend/Security | Implementation report and existing security assessment; not linearizable revocation or cross-page snapshot pinning. No expiry/extra cursor field. |
| Connector canonicalization/rejection may precede filters | Test/environment nuance; Backend | Real HTTP probes and implementation report; prove no data/auth success, and sanitized concealment where filters receive the request. Do not relax admission to force a connector-specific status. |
| Independent employee verifier, employee/role/read-login provisioning | Existing external prerequisite; Security/Operations IAM and DBA/Backend | Existing IAM/read-pool deployment work; absent capabilities fail503. No production adapter, credential, role or grant is provisioned. |
| Shared cursor-ring custody/rotation and AEAD nonce uniqueness | Existing external prerequisite; Security/Backend | Existing subject-read key custody/deployment process and implementation report; activation shares the ring with purpose/AAD separation. No separate operator secret is introduced; bad/missing ring fails503. |
| Toolchain/Docker/network and mandatory runtime validation | Validation pending; Controller/implementer | Authorized implementation validation only; no skip, waiver or readiness inference. |
| Unexpected frozen-input, schema, privilege or API need | Blocking scope question; Controller/user | Stop for renewed Goal Confirmation/authorization. No silent scope expansion or V018. |

## 8. Runtime and formal completion

**Runtime NOT RUN:** no Maven/build/test, application launch, Docker/PostgreSQL container, XML generation, JAR/firewall smoke, Node install, contract bundle/client generation or A1–A5 validator is run or claimed. Static source/POM counting and raw-byte checks do not substitute for future implementation proof. No source, test, migration, OpenAPI, docs/12–19, POM, CI, package/config or runtime-identity file is edited.

Exact paths changed by this fix:

1. `docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md` — cumulative correction.
2. `docs/gateflow/pa34-operations-activation-history-plan-fix-1.md` — this complete fix evidence artifact.

All four accepted findings and accepted clarifications are incorporated in the plan without a count exception or additional S1 slice. **Fix writing and static verification complete; independent re-review pending.** No review artifact is edited or added; no staging, commit, push, branch/PR mutation, approval, external comment, re-review dispatch or implementation/later gate is performed. Stop for the Controller.
