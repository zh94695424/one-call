# PA24 — K20 explicit-version rollout and CVM platform-acceptance plan

## 1. Status, authority and execution boundary

- Task: `pa24-secret-manager-rollout-plan-gpt-20261110-01`; work unit: `PA24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`; date: **2026-11-10**; gate: **plan only**; author provider/runtime: **GPT / Codex CLI**. The dispatch requests `gpt-6.1-sol` / high; the session cannot independently attest that model identifier. No delegation occurred.
- Binding contract: the active **PA24 Goal Confirmation** in `docs/gateflow/project-execution-control.md:15`. The G-labels below are traceability shorthand for that confirmation, not new goals, success conditions or permission to weaken acceptance.
- Workspace: `C:\Users\Administrator\.codex\worktrees\pa24-secret-manager-live-rotation\codex-call`; branch: `codex/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`; requested product base: `d46140d012acb42574503dc049c2b8778fc116e5`.
- Observed local HEAD: `ed9487273e3ce006b904aeeaa9ae7a9623f685d7` (`gateflow: confirm PA24 goal`), immediately after the requested merge base. Read-only base-to-HEAD comparison changes **only Control**; the product baseline is the merged PA23 tree. This is not an unreported product drift or permission to reset the branch. The initial worktree status has only pre-existing untracked `.codex`; preserve and exclude it.
- Native Git fails because `.git` contains a Windows drive path. Read-only inspection works with `GIT_OPTIONAL_LOCKS=0 git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call26 --work-tree="$PWD" ...`. Do not edit `.git`, the index or other worktrees to repair this.
- Read inputs: current Control/PA23 history; architecture `docs/12-technology-stack-and-system-architecture.md`; security `docs/17-production-security-design-spec.md`; PA23 plan and final closeout; all five current keysource production classes and six keysource test classes; current authority/provider, playback configuration and configuration tests; relevant architecture guards; POM; CI source/XML inventory, smokes, firewall and contract steps. Pinned SDK source archives were inspected locally, without executing the SDK or retrieving anything.
- PA23 closeout describes its then-open draft PR. The newer active Control records PR #34 merged at the requested base; use that newer state. PA23 historical successful runs are provenance only, not tests executed in this turn.
- **Original planning status: PLAN AUTHOR-COMPLETE, NOT ACCEPTED.** The subsequent independent reviews and current `fix` gate are recorded in Control. Bounded Plan Fix 1 (`pa24-secret-manager-rollout-plan-fix1-gpt-20261110-01`, 2026-11-10) corrects only the accepted evidence-lifetime/classification finding and writes only this plan and `docs/gateflow/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance-plan-fix-1.md`. Original planning-only write/stop language below is historical; this fix stops before re-review and authorizes no S1 execution. No build, application test, cloud probe, provisioning, credential acquisition or later gate is executed.

## 2. First-principles decision and goal alignment

**Production-code changes required: NO. Test/POM/CI changes required: NO.** The gap is an operator-ready deployment sequence and an honest, measurable acceptance/evidence contract, not missing key rotation behavior. Reuse the merged implementation and deterministic tests read-only. The one future slice writes one composed runbook/evidence document; it adds zero Java methods, properties, dependencies, workflows or runtime services.

| Trace | Confirmed goal / decision | Concrete evidence and consequence |
| --- | --- | --- |
| G1 | CVM attached-instance-role only | `TencentSsmSecretStringClient.productionClient` directly constructs `new CvmRoleCredential()` and a three-argument `SsmClient`; client tests capture both constructors. No default/env/profile/OIDC chain, static credential input or non-CVM mode is added. Actual role attachment and usability remain Platform/Security evidence. |
| G2 | Explicit immutable A → B → C version rollout | Existing `MediaPlaybackTencentKeySourceIntegrationTests` already has `explicitVersionRolloutVerifiesOldAndNewKeys`, `overlapReplaysExactCredentialForEveryPurpose` and `removedRecordedKidNeverMintsReplacement`. Each configured source is a fresh provider/client with a fixed version; the real parser and authority are used. Do not replace this with mutable-version fixtures or duplicate tests for activity. |
| G3 | Fail closed, explicit file, no fallback | `Settings.create`, exact selector conditions, response identity checks, strict parser, all-error mapping and valid→failure tests already close the failure paths. Zero/many provider cardinality is checked before a read. Preserve file's independent explicit selection. |
| G4 | PA21/PA22 wire, authority, state and redaction unchanged | The sole `MediaPlaybackCredentialAuthority` snapshots before token access; fresh issuance selects ACTIVE, replay selects the recorded kid, and verify decodes against the current snapshot. Tencent integration tests retain state/evidence/open denial and output canaries. No authority, codec, service, expiry or current-state edit. |
| G5 | Truthful measurable timeout/retry/liveness limitations | Profile construction is tested at default and min/max values. Pinned SDK source confirms metadata has no configured timeout and SSM has no total-call/DNS deadline. Document external measurement and release-blocking suitability decisions; do not claim a new deadline or disable underlying retries. |
| G6 | Concrete operator sequencing, rollback, retirement and evidence | Sections 4–7 specify the deployment checklist, mixed-version hazard, approvals, failure handling and evidence records. The future document makes this the reusable runbook and records actual deterministic results separately from unexecuted live rows. |
| G7 | Smallest bounded repository work; real cloud inputs external | One documentation/acceptance slice, one future document, unchanged inventories. Platform/Security supplies real region/resource/version/CAM values and owns CV-2/CV-3 suitability; Operations owns measurements; no fictitious cloud fixtures or live acceptance claim. |

### Direct implementation reasoning

Paths here are relative to `backend/src/main/java/com/onecall/core/sharing/internal/` unless fully qualified.

| Class / method | Actual behavior; required PA24 disposition |
| --- | --- |
| `keysource/TencentSsmSecretStringClient.read`, `client`, `productionClient` | Lazy synchronized client construction, exact configured SecretName/VersionId, one application `GetSecretValue` invocation per valid read, exact response identity, nonblank string and no nonempty binary; SDK/runtime exceptions become null. HTTPS POST, TC3, debug/unsigned false, fixed endpoint, no backup/proxy/trust override. Reuse unchanged. |
| `keysource/TencentSsmMediaPlaybackSigningKeyProvider.currentKeys`, `Settings.create` | Every valid call rereads; null/invalid settings avoid client reads; reporting UTF-8 encoding and 65,536-byte limit feed the unchanged parser; checked/runtime failures yield only Unavailable, owned buffers are cleared. Settings are captured, not dynamically refreshed. Reuse unchanged. |
| `keysource/MediaPlaybackKeyBundleParser.parse` | Closed schema-v1, exactly one ACTIVE, zero/many VERIFY_ONLY, unique valid kids, canonical 32-byte material, whole-bundle rejection and canonical ordering. Reuse unchanged; no second registry. |
| `keysource/MediaPlaybackKeySourceConfiguration`, `FileSelection.matches`, `TencentSelection.matches` | Exact case-sensitive selection; file ignores unselected Tencent metadata. Tencent plus any `bundle-path` property presence selects neither built-in provider, before resolving its value. Invalid selected Tencent settings produce an unavailable provider without startup cloud reads. Reuse unchanged. |
| `keysource/FileMediaPlaybackSigningKeyProvider.currentKeys`, `readBoundedSource` | Explicit normalized absolute regular-file source, no symlinks, bounded checked read, no search/fallback. Remains available only through explicit file selection, never a rollout recovery route. |
| `credential/MediaPlaybackCredentialAuthority.snapshot`, `issueOrReplay`, `verify` | Exactly one provider before read; invalid/unavailable set yields Unavailable. Valid set lacking K1 yields old verification Invalid and old replay Unavailable. Fresh requests use the ACTIVE kid. Replay retains recorded anchor, expiry and exact credential; absent anchor key never mints a replacement. |
| `playback/MediaPlaybackConfiguration.mediaPlaybackCredentialAuthority` | One unconditional shared issuer/verifier using the existing provider seam; no new role bean or competing authority. Existing consumers/state checks remain mandatory. |

Two important limits do **not** justify product changes:

1. `version-id` is validated as an opaque identifier, not against a cloud-backed immutable-version registry. The code neither discovers aliases nor infers the operator's intended version. Do not add a speculative `latest/current` string blacklist: it cannot prove immutability and could reject a legitimate opaque ID. Platform/Security must prove the supplied exact VersionId is immutable, not a moving stage/alias, before release. A missing/nonexistent/wrong-echo version fails closed; a mistakenly selected **other valid** version with a matching response is not distinguishable by the adapter. The approved deployment manifest/evidence comparison must block that configuration error.
2. There is no enforceable end-to-end deadline. A blocked SDK/metadata/DNS call can remain in progress without issuing a credential; once it surfaces an exception, the existing mapping is closed. Fail-closed is not a guarantee of timely Unavailable. If deployment cannot accept this limitation, stop for Platform/Security disposition and a separately authorized design; do not smuggle in a custom metadata client, executor deadline or retry framework.

## 3. Exact existing configuration and bundle contract

Use the literal prefix `onecall.sharing.media-playback.signing-key-source.`. Notation in this document is not a new configuration format. Real values belong only to approved external deployment configuration, never examples committed here.

| Property suffix | Decision / validation / failure behavior | Trace |
| --- | --- | --- |
| `provider` | Exactly `tencent-ssm` for this deployment. No default, trimming, case folding, compound values or profile inference. Missing/invalid/unresolvable selection means no built-in provider. `file` remains an explicit separate route, not this cloud deployment. | G1/G3 |
| `bundle-path` | Must be **absent**, not blank, on every Tencent instance. Any property presence suppresses the built-in Tencent source and never selects file. Inspect all effective property layers without dumping values. | G3 |
| `tencent.region` | Required; current ASCII pattern `[a-z0-9]+(?:-[a-z0-9]+)+`, maximum 64 characters; real region and fixed endpoint reachability supplied/validated by Platform. No region default/probe. | G1/G3/G7 |
| `tencent.secret-name` | Required opaque exact value; 1–1,024 UTF-16 units, no edge whitespace, controls, `${` or unpaired surrogates. No trim, URL decoding or resource-name synthesis. | G2/G3 |
| `tencent.version-id` | Required opaque exact value; 1–256 UTF-16 units, same validation. External manifest proves approved immutable ID; no stage/latest/current alias, implicit version or alternate-version lookup. | G2/G3/G6 |
| `tencent.connect-timeout-seconds` | Absent defaults to `2`; only ASCII `1`–`5`; sets **connect and write** seconds. No extra write-timeout property. | G5 |
| `tencent.read-timeout-seconds` | Absent defaults to `3`; only ASCII `1`–`10`; sets read seconds. No signs, leading zero, whitespace, units, zero or infinity. Neither timeout is Q22. | G5 |

Invalid/missing selected settings, including placeholder resolution failure, yield one unavailable Tencent provider with no SDK/metadata read. No SecretId/SecretKey/token/profile/endpoint/debug/retry/cache/TTL properties are added. Spring binding of nonsecret metadata does not authorize reading credentials or a secret bundle from environment variables. Avoid configuration/actuator dumps.

Bundle composition, unchanged from PA22:

- Exact root fields `schemaVersion` (integer `1`) and `keys` (nonempty array); exact entry fields `kid`, `role`, `keyHex`. Kid pattern `[A-Za-z0-9_-]{1,32}`, unique across the entire set; role exactly ACTIVE or VERIFY_ONLY; `keyHex` exactly 64 lowercase hex characters representing 32 bytes, HMAC-SHA-256/256-bit K20 only.
- Exactly one ACTIVE; zero/many VERIFY_ONLY sorted by kid. Unknown fields/states, duplicate JSON fields/kids, zero/multiple ACTIVE, bad key bytes/encoding, BOM, trailing tokens, excessive depth or source size reject the **entire** set. Never filter bad entries or accept a partial bundle.
- A, B and C are symbolic immutable **secret versions**, not K20 kids and not production IDs. A contains K1 ACTIVE; B contains K2 ACTIVE and the **same K1 bytes under the same K1 kid** VERIFY_ONLY; C contains the **same K2 bytes under K2** ACTIVE and no K1. K1/K2 are descriptive labels; use approved independent production material, not checked-in synthetic test bytes.
- Kid-to-material continuity is externally checked without emitting material; never reassign an old kid to different bytes. The parser intentionally has no cross-version memory. Equal material under distinct kids does not alias identities; existing tests prove MAC/kid binding and no replay substitution.
- Fresh provider instances/restarts with the exact next version perform rollout. Retaining a lazy SDK client and SDK temporary role-credential refresh state is **not** a signing-key cache, moving-version watcher or background key rotation.

## 4. Operator sequence and immutable rollout acceptance

This is the specification for the future runbook, **not authorization to execute cloud work**. Every live step below is initially `NOT EXECUTED — external Platform/Security input required`. Use existing approved deployment controls, not new deployment manifests or topology. Operators must not call the provider/authority a source of business authorization.

### Preconditions and evidence custody

1. Platform/Security supplies an approved external manifest: environment, application artifact digest, CVM instance identities/attached role references, CAM trust and least-privilege review, actual region, exact secret resource and immutable IDs for A/B/C, effective configuration revision, approved timeout choices and network/DNS assumptions. Record only a redacted reference/digest and owner in the repository document. No real resource/account/role/version values, responses, credentials or material enter the repo.
2. CV-2 evidence must establish immutable version semantics, SecretString support, response SecretName/VersionId equality to the configured values, version availability/disable/recovery behavior in the chosen region. CV-3 evidence must establish CVM role attachment and read access restricted to the required action/resource/environment. The application only calls `GetSecretValue`; actual CAM action/resource syntax and any unavoidable provider policy granularity must be validated externally, not fabricated here. No wildcard policy is accepted as a shortcut; unsupported least privilege blocks live acceptance.
3. Check every effective configuration layer: exact `tencent-ssm`, no `bundle-path`, approved literal region/name/version, valid timeout values, no inline/static/env/profile credentials, no debug or body capture. Do not follow aliases even if a vendor accepts one as VersionId. Verify deployed artifact and per-instance configuration against the approved manifest without rendering sensitive values.
4. Sharing/Security supplies eligible synthetic acceptance requests for PLAYBACK, SEEK and RENEW with fixed identity/facts and known-valid state. Preserve opaque credential/anchor/expiry **only in protected ephemeral memory** for comparisons; never export the bearer, anchor contents, payload or MAC. The separate timestamp-only evidence fields below do not authorize exporting the credential/anchor. Sharing/Security + Operations must supply the protected harness/custody reference and approved retention of that ephemeral comparison state through the required observations; missing or lost state makes the affected row NOT EXECUTED. The fixed 60-second window in existing unit fixtures is test data, not production TTL/skew/propagation approval.
5. Before the A evidence sequence, Security/Sharing provides an externally approved validity/clock/skew reference and maximum outstanding credential/session/replay horizon, including propagation/drain assumptions and retirement conditions; Operations records last possible K1 issuance and fleet/in-flight drain evidence. The horizon governs safe production retention of B, not a promise that an A-issued evidence credential survives through C. Confirm an approved ordinary validity window can cover A issuance, A→B quiesce/restart and all B checks; if not, block overlap acceptance for owner disposition. No numeric retention/overlap interval or Q22 value is chosen here. Missing inputs block the corresponding live evidence and **live retirement**, not plan authoring or deterministic C tests.

### Evidence/validity contract

For every relevant A/B/C issue, verify and replay observation, record an evidence ID/purpose and symbolic credential cohort/kid, `issuedAt`, `notBefore`, `expiresAt`, current UTC timestamp at operation start and completion, and externally approved validity/horizon and clock/skew references. Use the A-recorded timestamps again for old K1 at B/C, not a newly inferred expiry. Record time-valid-at-observation TRUE/FALSE/UNKNOWN, expected/actual authority result, evidence class (VALID-TIME OVERLAP, KEY-ABSENCE or FRESH-ISSUANCE), disposition (PASS/FAIL/INVALID/NOT EXECUTED), owner and protected provenance/custody reference. Record the deployed artifact/configuration revision, per-instance symbolic source/version and independent approved bundle/identity/response-equality attestation reference, including K1 continuity at B and K2-only at C. Real identifiers and material stay outside the repository. Missing timestamps, provenance or approvals cannot pass.

The authority uses microsecond-normalized time with `issuedAt == notBefore`, `issuedAt <= now` and `now < expiresAt`; there is no runtime skew allowance to extend expiry. Evaluate both operation timestamps under the approved clock/skew uncertainty basis; if the validity window is not established throughout the observation, classify time-valid evidence INVALID (or NOT EXECUTED if no qualifying observation was made). Skew approval constrains evidence confidence, not codec behavior. Retain all other claim, request-bound and current-state requirements; time validity alone is not authorization.

- **B valid-time overlap:** old K1 verification must be Verified and replay must return exactly the recorded credential/anchor/expiry for each purpose while time-valid. If quiesce/restart or any check crosses the validity boundary, record the actual result (including Invalid verify or Malformed replay), mark B evidence INVALID/NOT EXECUTED and block overlap acceptance/reopening. Never call an expiry failure overlap success. A new attempt requires an explicitly approved safe A/cohort sequence; do not silently return serving B to A after K2 issuance, extend/reissue the recorded credential, alter a clock, invent a hidden long-lived credential or select Q22 values.
- **C key-absence:** the recorded old K1 credential/anchor may be observed after expiry, with its time-valid field explicitly FALSE. Independently establish the current fixed C source identity/version and valid K2-only bundle, with no K1, for each observation; a bare Invalid result is insufficient because expiry can also cause Invalid. In the merged authority, recorded-kid lookup precedes claim validity on replay, so absent K1 yields Unavailable; verify decodes only through the current snapshot, so K1 cannot be selected and yields Invalid. Preserve the original otherwise well-formed replay request/bounds. Together with independent source evidence these results support key absence/no replacement, not continued old-credential validity, usable-session continuity, instantaneous revocation or live-cloud acceptance from a deterministic test. Missing source evidence makes the key-absence row INVALID/NOT EXECUTED; source-wide Unavailable is not a retirement pass. Fresh K2 issuance **and verification** must still be time-valid.
- **Optional time-valid old-K1 C requirement:** if Security/Sharing requires this additional observation, it is a separate approved acceptance-cohort/external-input path, not the production B-retention sequence. Before any such execution, owners must supply cohort isolation/non-serving controls, immutable A/B/C provenance and continuity, authorized harness/custody, ordinary policy-compliant validity and clock/skew references, and a schedule covering its A→B→C observations before expiry. This bounded path cannot shorten serving B retention, restore K1 to serving C, issue an undisclosed long-lived credential or override Q22. Without those approvals or a feasible window, mark that additional row NOT EXECUTED and block that additional acceptance requirement. Controlled-clock deterministic C evidence remains repository-only, never a live-cloud substitute. No cohort execution or cloud action is authorized here.

### Checkable A/B/C table (repeat for all three purposes)

All requests retain the same share/admission, generation, object, facts, purpose and bounded expiry when replaying. Apply the evidence/validity contract to every observation. Existing deterministic tests keep clocks within validity; production evidence proves B before expiry, then C key absence after safe retention without asserting old K1 remains time-valid.

| Version / operation | Required result and invariant | Existing deterministic oracle | Trace |
| --- | --- | --- | --- |
| A: issue K1, verify, replay | Time-valid Fresh Issued with K1, Verified; replay same credential bytes, anchor and expiry, no new identity or admission recount. Record timestamps and approved references for later B/C observations. | `tencentBundleIssuesActiveForEveryPurpose` plus A setup in rollout tests; authority `advancingClockReplayReturnsFirstCredential`, `freshAuthorityReconstructsCommittedAnchor` and golden purpose tests | G2/G4 |
| B: old K1 verify/replay | VALID-TIME OVERLAP only: Verified under VERIFY_ONLY K1; exact byte-identical replay with recorded K1 anchor/expiry despite clock advance **within validity**. No ACTIVE replacement or TTL extension. Boundary crossed/unknown: INVALID/NOT EXECUTED, block acceptance. | `explicitVersionRolloutVerifiesOldAndNewKeys`, `overlapReplaysExactCredentialForEveryPurpose` (NOW+1 within fixed test expiry) | G2/G4 |
| B: fresh issuance | Time-valid Issued under K2 ACTIVE, Verified; fresh credential differs from the K1 credential. Record its own validity timestamps. | `explicitVersionRolloutVerifiesOldAndNewKeys`, `tencentBundleIssuesActiveForEveryPurpose` | G2 |
| C: old K1 verify/replay | KEY-ABSENCE only: independently proven current C/K2-only source, **Invalid** verification, **Unavailable** recorded replay, no replacement. Old K1 may be expired; record validity explicitly, do not infer key absence from expiry/Invalid alone. Separate optional time-valid C path above if required. This is not a source-wide outage. | `removedRecordedKidNeverMintsReplacement`; `equalMaterialNeverAliasesRecordedKid`; authority replay lookup/codec decode ordering. Existing fixed-clock oracle is not a post-retention live result. | G2/G3/G4 |
| C: fresh issuance | Time-valid Issued under K2 **and Verified**; old K1 removal does not invalidate the otherwise valid source. Record fresh K2's own timestamps, not old K1's. | `removedRecordedKidNeverMintsReplacement` asserts fresh Issued/K2; B/full-claim tests prove unchanged K2 verification path, not a new live C measurement | G2/G4 |
| Any source failure | Provider Unavailable; fresh/replay/verify Unavailable; no token access before key availability, no state/evidence/controlled open after failed verification. Never confuse source failure with valid-source unknown-kid Invalid. | `unavailableSourcePrecedesOpaqueTokenAccess`, `unavailableSourcePreventsStateEvidenceAndOpen`, provider/client failure suites | G3/G4 |

### Fleet sequencing and mixed-version safety

1. Establish A on the acceptance cohort with a fresh fixed-A configuration. Obtain protected K1 credentials for each purpose and record assertion outcomes, timestamp-only validity fields and nonsecret evidence references. Confirm the approved remaining validity covers A→B quiesce/restart and B observations; lack of a feasible window blocks that evidence sequence. No stage pointer or in-place mutation represents A → B.
2. Prepare immutable B externally; independently confirm K1 continuity and K2 ACTIVE. **A cannot verify fresh K2 credentials.** Therefore this plan uses an operator-controlled **quiesced cutover**: pause admission/issuance and media traffic through existing controls, drain in-flight operations, stop/remove all A-configured serving instances, restart/reconfigure the whole acceptance serving cohort to explicit B, run B checks before reopening traffic. Do not declare ordinary A/B rolling deployment safe or solve it with an extra key registry/compatibility service. Inability to quiesce/drain is an external deployment blocker requiring owner disposition, not authorization for topology changes.
3. At B, check old K1 exact replay/verification while time-valid and fresh time-valid K2 issuance/verification for all purposes. Preserve this dated overlap evidence before expiry; crossing/uncertain validity makes it INVALID/NOT EXECUTED and blocks acceptance/reopening, not grounds for calling expiry overlap success. Confirm the full cohort is B before reopening. Valid→outage tests/evidence must show no last-good-key use; do not poll the cloud from an in-repo watcher. Observe per-call cost/quota/latency under the approved external load model.
4. After collecting valid B evidence, keep serving B until the approved maximum outstanding credential/session/replay horizon from last possible K1 issuance, propagation/drain basis and all owners' retirement preconditions are satisfied. Preserve the earlier B evidence as historical proof, not a claim that K1 stays time-valid throughout retention. Do not shorten retention to fit an evidence credential's expiry. C preparation removes K1 from an otherwise valid new bundle; it does not insert `DISABLED`/`DESTROYED` roles (those make a malformed bundle and take the whole source unavailable).
5. Quiesce/drain again and switch the full serving cohort from B to fixed C before declaring retirement effective. A B instance still has K1 and can verify/replay it; an in-flight operation can finish using its already obtained snapshot. There is no instantaneous fleet-wide revocation guarantee. Collect C KEY-ABSENCE evidence with the recorded old K1 input, possibly now expired, plus independently established current C/K2-only source evidence; require old verification Invalid/replay Unavailable/no replacement and fresh time-valid K2 issuance/verification. Link to the earlier valid B evidence without presenting the C result as time-valid K1 overlap. If required, track the separate approved time-valid C cohort path or mark it NOT EXECUTED; never extend an expired real credential to obtain evidence.
6. Removing K1 from C is **logical acceptance retirement**, not deletion/destruction of secret versions A/B or bytes already in process/buffers. Disable/destroy/recovery and historical-version access are separately authorized Platform/Security lifecycle actions, audited externally. Already buffered media or issued credentials cannot be recalled; current-state checks and existing expiry semantics remain authoritative.

### Rollback and recovery boundaries

- Before K2 issuance, returning the whole quiesced cohort to A can be approved only if A and K1 remain authorized/available. After any K2 issuance, **B → A is not a compatible rollback**: A lacks K2 and breaks its verification/replay. Prefer keeping the approved B config while rolling back compatible application code, without restoring stale config; revalidate all state/wire constraints. If that is unsafe, remain unavailable and escalate rather than fall back.
- If Security explicitly requires reactivating K1 after K2 issuance, a separately approved **new immutable rollback bundle** must contain K1 ACTIVE plus unchanged K2 VERIFY_ONLY, preserving both recorded kids. This is a conditional operator recovery option, not a fourth mandatory rollout stage, dynamic source, new production code or cloud action authorized here. Validate both old credential populations and new K1 issuance before reopening. Do not edit immutable B in place.
- C → B can restore K1 verification/replay and therefore **undo retirement**. Permit only through explicit Security authorization when K1 has not been compromised, disabled/destroyed or prohibited by policy; quiesce/drain and document the changed acceptance state. Never claim a C retirement pass remains effective afterward. Destroyed material is not recoverable via backup plaintext or a file/env fallback.
- Bad config/CAM/credential/source failure means stop progression, keep traffic closed, repair the approved configuration/identity externally, restart as needed and repeat the table. A later valid read may recover; no automatic alternate version, stale keys or file route is allowed. Audit/control evidence for rotation/retirement/recovery follows existing Security design; this slice does not implement a new audit writer or claim CloudAudit replaces the business audit source.

## 5. Timeout, retry and liveness evidence contract

The local pinned `common-sources.jar` SHA-256 is `87691d98a96b114f05dedb24d0a6d8cacd9b753d3cf871ef10dcb5afb6a5ffb3`; `ssm-sources.jar` is `e2ec0758b918233795b149016c20fef624de868cc55c56134646bdddb55c9c2a`. These match the PA23 evidence ledger. The sources are under `/mnt/c/Users/Administrator/AppData/Local/Temp/pa23-sdk`, external read-only evidence, not new repo dependencies or proof of live platform behavior.

| Boundary | Verified static/deterministic truth | Required future external measurement; no present live result | Trace |
| --- | --- | --- | --- |
| SSM connect/read/write | Current factory maps default **2/3/2 seconds** and min/max connect/read 1/1 and 5/10; write always follows connect. Tests capture actual HttpProfile/ClientProfile arguments, not elapsed wall time. | Record selected values, scenario, request count/concurrency, monotonic start/end, duration in milliseconds, success/unavailable/in-progress/censored count, and process/thread observations for normal and impaired SSM connection/read/write cases. Distinguish configured phase limits from observed full invocation time. | G5 |
| CVM role metadata | `CvmRoleCredential` uses `HttpURLConnection` without configured connect/read/call timeout; credential `getSnapshot` serializes refresh. SSM HttpProfile does not bound it. Temporary role-credential refresh is SDK behavior, not key-bundle rotation. | Separately observe cold acquisition and refresh under healthy and unreachable/unresponsive metadata; record elapsed/censored results and waiting request/thread count. A measurement stop budget must be supplied by Platform/Operations. Stop/kill an external probe process using approved operational controls when needed; this does not cancel or bound the production call. | G1/G5 |
| DNS / total SSM call | Pinned HttpConnection builds OkHttp connect/read/write timeouts only. No configured total `callTimeout` or DNS deadline. Durations need not fit 2+3+2 or any calculated sum. | Record DNS success/failure/stall, full call elapsed, cold/warm client/credential state, response progression and censoring separately. Never infer an upper bound or a production SLO from one finite sample. | G5 |
| Retry / attempts | One application `GetSecretValue` invocation per valid read; no app/`AbstractClient.retry`. Underlying OkHttp connection retries remain possible; profile exposes no disabling toggle here. | Count application invocations and observed transport attempts separately under an approved fault case; metadata accesses are separate again. If transport attempts cannot be measured without leaking payloads, record **unobserved**; do not infer single-attempt behavior from Mockito counts or cloud logs alone. | G3/G5 |
| Capacity / availability | No signing-key cache; issue, verify and replay each take a current snapshot and may read SSM. Successful startup is lazy and does not prove cloud usability. | Operations supplies sample size, concurrency/load, duration, P95/P99 method, error/throttle rates, cost/quota assessment, thread saturation and recovery results. Report sample count/units; no latency, retry, quota or availability threshold is invented here. | G5/G7 |

Each evidence record must include: evidence ID, G trace, artifact/config digest reference, CVM/role review reference, scenario and perturbation approval, UTC timestamps plus monotonic elapsed milliseconds, configured phase timeouts, cold/warm/refresh condition, completed/in-progress/censored result, sample size, application/transport/metadata count or explicit unobserved status, nonsecret assertion booleans, owner, disposition and protected evidence location reference. Never capture payloads, signing headers, credential triples, tokens, raw SDK errors or raw resources as labels.

**Acceptance decision:** bounded phase profiles and fail-closed mapping can pass deterministically; finite end-to-end/metadata/DNS limits and zero transport retries **cannot** be claimed. Platform/Security and Operations must explicitly accept the measured limitations against their external production requirements, or block platform acceptance. Missing measurements/threshold ownership remain OPEN/NOT EXECUTED, not pass, waiver-by-default or a requirement to change code in this plan. No live execution or fault injection is authorized by this planning artifact.

## 6. Existing tests and exact assertion obligations

Test paths in the first six rows are under `backend/src/test/java/com/onecall/core/sharing/internal/keysource/`. Run existing plain named tests; add **zero** methods or classes. No metadata/SSM/STS HTTP client, real credentials or live network in automated tests; SDK clients/constructors remain mocked. Temporary synthetic files are file-regression fixtures only.

| Exact file / existing methods | Assertions to preserve / inspect in results | Trace |
| --- | --- | --- |
| `MediaPlaybackTencentKeySourceIntegrationTests.java` (10) | All A/B/C methods in section 4; `equalMaterialNeverAliasesRecordedKid`; `transportAndParserFailuresHaveNoFallback`; `tencentClaimsStillRequireCurrentState`; `sdkToAuthorityFlowNeverLeaksCanaries`; both source-unavailable ordering/downstream tests. All purposes, real authority/parser, new fixed-version configurations. | G2/G3/G4 |
| `TencentSsmSecretStringClientTests.java` (8) | `explicitNameAndVersionReachOneSdkRead`; `roleOnlyFactoryIsLazyAndHasNoStaticFallback`; `clientRegionHttpsTimeoutAndRetrySettingsAreExact`; `nullMissingBinaryAndMismatchedResponsesFailClosed`; `authCamAndResourceStateFailuresAreUnavailable`; `throttleUnknownAndServerFailuresAreUnavailable`; `networkTlsAndTimeoutFailuresAreUnavailable`; `clientFailuresAndStringFormsNeverLeak`. Wrong/missing response identity, credential/factory/runtime failure, CAM denial, not-found/disabled/destroyed, DNS/TLS/connect/reset/timeout, throttle and unknown codes all fail closed; no second application call/fallback. Constructor mocks do not prove live CVM suitability. | G1/G2/G3/G5 |
| `TencentSsmMediaPlaybackSigningKeyProviderTests.java` (18) | Missing/invalid metadata/timeouts never read client; each valid currentKeys reads exactly once; valid→throw/null/malformed→valid never serves last-good; character/UTF-8 byte limits, surrogate/base64/syntax/schema/role/duplicate/material failures; snapshots/copies/buffer clearing; no alternative/file source and opaque redaction. | G2/G3/G4/G5 |
| `MediaPlaybackKeyBundleParserTests.java` (12) | Closed schema and duplicate detection, exact one ACTIVE and canonical VERIFY_ONLY, encoding/size/depth/hex/kid/state rejection, independent copies and redaction. Invalid C with no ACTIVE must remain Unavailable, not be confused with valid C lacking only K1. | G2/G3/G4 |
| `FileMediaPlaybackSigningKeyProviderTests.java` (10) | Explicit path, symlink/regular-file and TOCTOU/size checks, IO/permissions, no searched fallback, per-call reread, corruption/removal Unavailable, snapshots and redaction remain unchanged. | G3/G4 |
| `MediaPlaybackFileKeySourceIntegrationTests.java` (8) | Explicit file issue/verify/replay/overlap/removal and current-state/redaction regression; this does not authorize using file when Tencent fails. | G3/G4 |
| `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` (19) | Exact selector and pre-value-resolution path conflict; invalid selected metadata no SDK/role read; stale Tencent metadata cannot suppress file; one authority across profiles; custom/Tencent/file ambiguity in both orders including Primary/Ordered decoys before reads; file independence. | G1/G3/G4 |
| `backend/src/test/java/com/onecall/archtests/sharing/MediaPlaybackCredentialAuthorityTests.java` (24) | Golden PLAYBACK/SEEK/RENEW, ACTIVE-only, VERIFY_ONLY replay, absent/invalid provider/kid, microsecond clocks, fresh authority reconstruction, no replay kid/expiry substitution, witness upper bound, legacy P2 unavailable and redaction. | G2/G3/G4 |
| `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` (8) | Especially `keySourceUsesOnlyLocalReadAndExistingProviderBoundary`, `tencentKeySourceConfinesSdkAndPreservesExistingAuthority`, `playbackStateAndOpenPreserveReadOnlyNoProjectionBoundary`: exact five keysource paths/imports, sole SDK client, direct CVM, fixed profile/read API, no static/default/env/profile/OIDC/fallback/cache/watcher/thread/retry/mutation, 81 internal files and unchanged credential boundary. Retain all guards. | G1/G3/G4/G5/G7 |

Regression validation includes all 13 existing PostgreSQL IT classes, especially PublicShareAdmissionIT 42, MediaPlaybackIT 9, MediaGatewayTransportIT 8 and SharingAdmissionRoleLeastPrivilegeIT 9; all current wire/state/current membership/risk/revocation/generation/expiry/object/window checks remain. No new ACL, schema, DB mutation, public status/code, endpoint or serialization change. Existing C10 cannot bypass state checks just because cloud key retrieval succeeds.

Redaction remains end-to-end: no bundle, key bytes/hex/base64, SecretString/binary, credential triple, Authorization/header, playback token/payload/MAC, config resource/version, SDK request ID, raw response, exception message/cause or parser detail in logs/errors/JSON/toString/test failure output. Compare sensitive values via boolean/equality checks that do not render them; use synthetic canaries only. SDK debug stays false. Buffer clearing does not erase Java/SDK Strings or offer HSM isolation. External probes must not enable body logging/tracing to measure attempts.

## 7. One future slice, exact allowlist and documentation decision

**S1 — make the existing CVM-only immutable rollout operationally checkable and record bounded acceptance evidence.** Trace: G1–G7. One slice is sufficient because there is no identity/timeout enforcement code gap to separate from overlap behavior; splitting by client/config/tests/docs would manufacture layers of work without independent behavior. Maximum-three exception is not needed.

Ordered execution after plan acceptance **and separate implementation authorization**:

1. Reconfirm unchanged product baseline and section 8 inventory. If a concrete confirmed-goal defect is discovered, stop and return exact class/method, failing behavior/path and test evidence for a bounded plan revision; do not expand this allowlist or quietly implement it.
2. Create the single document below with reusable runbook sections implementing sections 3–6, an A/B/C checklist, per-instance cutover/rollback/retirement gates, failure/redaction table and evidence template. Carry section 4's timestamp/clock/approval/source-provenance fields and INVALID/NOT EXECUTED blockers into every relevant observation; distinguish early valid-time B proof, safe B retention, later C key-absence proof and fresh time-valid K2 proof. Include the optional time-valid C path and its approvals only if required, otherwise record its requirement disposition. Include an explicit external-input register with all live rows NOT EXECUTED; do not invent region/resource/version/CAM values or probe thresholds.
3. Execute the deterministic validation commands in section 9 only on the separately authorized validation runner. Populate a separate **Observed repository validation** section in the same document with actual commands, exit codes, versions, exact inventories, results and failures/skips. Keep **External live platform acceptance** clearly separate and unexecuted. No cloud access is needed to complete S1's repository acceptance contract.
4. Check documentation against every G trace and existing test assertion, inspect unchanged source/CI/POM inventories and protected paths, and emit section 11's completion report. Stop; the Controller owns all later gates.

**Current write allowlist:** only `docs/gateflow/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance-plan.md` (this file).

**Future S1 repository write allowlist:** only new `docs/gateflow/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance-s1-implementation.md`. Its title must identify it as **CVM immutable-version rollout runbook and repository acceptance evidence**; its runbook remains reusable independently of its dated execution record. This composed document serves both operator contract and S1 report, avoiding a duplicate runbook/report pair. This is future intent, not current permission. Corrections to this plan require a separately authorized plan-fix gate.

All inspected source/test files, `backend/pom.xml`, `.github/workflows/ci.yml`, resources/YAML, README, Control, architecture/security canonical specs and models, acceptance/protocol/capacity/OpenAPI files, migrations/ACL/V001–V013, deployment manifests and existing Gateflow/review artifacts are **read-only**. No additional source/module/file is affected. Ordinary ignored build outputs (`backend/target`, locked-toolchain generated output) may exist only on the future separately authorized validation runner, are not deliverables and must never be committed. No new dependency, profile, property or test inventory is planned.

**Documentation decision:** do not edit docs/12 or docs/17; their topology/custody/C10/redaction/CV/Q22 contracts are already correct. Do not alter the canonical acceptance model to claim runtime/cloud completion or expand validators merely to inspect a runbook. The plan and the one future composed document are the exact PA24 artifacts. No final-closeout/review artifact is preauthorized here.

## 8. Fresh measured baseline and unchanged S1 target

Measured from the current merged product tree on 2026-11-10, not copied from PA23 counts. The PA22 reusable scanner was extended **in memory only** to include current `pa23_additions`, then executed against actual source and CI. It verifies exact method-name sets and POM includes; it does not execute tests. The current firewall's literal/static portion, through its 116 adversarial probes and before any runtime-manifest/JAR read, was executed successfully. No POM/CI file was modified.

| Inventory | Fresh observed baseline | S1 target / interpretation |
| --- | --- | --- |
| Surefire source=CI classes / methods | **36 / 309**, exact sets match | Unchanged; fresh XML required on future validation |
| Failsafe source=CI classes / methods | **13 / 214**, exact sets match | Unchanged; real PostgreSQL tests, not cloud acceptance |
| Total source=CI classes / methods | **49 / 523** | Unchanged; **zero added tests** |
| Annotation-line scan | **524**; sole historical comment hit in PostgresContainerIT | Unchanged; 523 methods, not 524 tests |
| Focused suite (nine classes) | **117 methods** = 18+8+10+12+10+8+19+24+8 | Unchanged; not run in this turn |
| Sharing internal / keysource / credential production files | **81 / 5 / 3** | Unchanged |
| Keysource test classes / methods | **6 / 66** | Unchanged |
| POM direct dependencies / managed BOM imports | **12 / 1** | Unchanged; SSM 3.2.46 direct compile dependency, not a new addition |
| POM normal/profile plugins / Failsafe includes | **3 / 1 / 13** | Unchanged |
| Firewall approved coordinates / dependency-only / packaged counterparts | **72 / 8 / 64** literal entries | Unchanged; actual runtime manifest absent locally |
| Firewall packaged filenames / sole jarmode / positive-first collisions | **65 / 1 / 7** | Unchanged; actual production JAR absent locally |
| Historical firewall baseline and additions | **37 / 34**, deltas **35 / 31**, removals **0 / 0** | Unchanged |
| Firewall static rejection probes | **116 / 116 pass**, including historical 82 plus 34 SDK mutations | Static manifest/probe evidence only, not actual-JAR validation |
| Current local TEST XML / production JAR / runtime manifest / jar index | **0 / absent / absent / absent** | No current source=CI=XML or runtime pass claimed |

Fresh POM inspection: Boot **4.1.2**, Java/release **21**, Modulith **2.1.2**, Testcontainers **2.0.5**, Enforcer **3.6.3**; CI pins Temurin **21.0.13+9**, Maven **3.9.9**, Node **22.23.4**. Preserve pins, scopes, convergence and dependency bans. The architecture's older runtime-family recommendation is not permission to downgrade. Direct artifacts, in current POM order: `tencentcloud-sdk-java-ssm`, `spring-boot-starter-webmvc`, `spring-jdbc`, `jooq`, `HikariCP`, `spring-boot-starter`, `spring-modulith-starter-core`, `spring-boot-starter-test`, `testcontainers`, `postgresql`, `flyway-core`, `flyway-database-postgresql`. Managed import: `spring-modulith-bom`. Normal plugins: Boot, Surefire, Enforcer; profile plugin: Failsafe.

Failsafe includes remain exactly `**/PostgresContainerIT.java`, `**/SharingQueryServiceIT.java`, `**/CustomerMeIT.java`, `**/CustomerSharesIT.java`, `**/SharingRoleLeastPrivilegeIT.java`, `**/CustomerShareCommandsIT.java`, `**/PublicSharesIT.java`, `**/PublicShareAdmissionIT.java`, `**/SharingAdmissionRoleLeastPrivilegeIT.java`, `**/EvidenceMediaReadRoleLeastPrivilegeIT.java`, `**/EvidenceMediaReadIT.java`, `**/MediaPlaybackIT.java`, `**/MediaGatewayTransportIT.java`.

CI impact: **none**. Do not add a `pa24_additions` dictionary or rename PA23 historical inventory messages to simulate implementation. Preserve historical method totals `112+178=290`, `128+192=320`, `162+198=360`, `196+204=400`, `231+214=445`, `266+214=480`, current `309+214=523`; annotation scans `291/321/361/401/446/481/524`. Future clean XML must match unchanged exact class/method pairs, zero skips/errors/failures. No firewall wildcard or waived check. A `docs/gateflow/`-only change is not covered by the workflow's `docs/*.md` path filter; do not claim CI automatically ran or broaden CI merely to force it. Use the separately authorized local/validation runner and report actual evidence.

## 9. Reproducible measurements and future validation commands

### Read-only commands executed during planning

In addition to source/document inspection and read-only Git identity/base-diff checks, the following baseline measurement commands were executed successfully. They do not build, run application tests, read credentials, make network requests or write repository files.

```bash
python3 - <<'PY'
from pathlib import Path
text = Path('docs/gateflow/pa22-sharing-media-production-key-custody-and-provider-integration-plan.md').read_text()
scan = text.split('### Reproducible read-only source/CI/POM inventory scan', 1)[1].split("python3 - <<'PY'\n", 1)[1].split('\nPY', 1)[0]
scan = scan.replace("'pa21_additions', 'pa22_additions'", "'pa21_additions', 'pa22_additions', 'pa23_additions'").replace("'pa21_additions.items()', 'pa22_additions.items()'", "'pa21_additions.items()', 'pa22_additions.items()', 'pa23_additions.items()'")
exec(compile(scan, '<readonly-pa24-inventory>', 'exec'))
PY
python3 - <<'PY'
from pathlib import Path
import textwrap
workflow = Path('.github/workflows/ci.yml').read_text()
section = workflow.split('      - name: production jar dependency firewall\n', 1)[1]
block = textwrap.dedent(section.split("python3 - <<'PYCODE'\n", 1)[1].split('\n          PYCODE', 1)[0])
static = block.split('resolved = parse_coordinates(Path("target/pa11-runtime-dependencies.txt")', 1)[0]
environment = {}
exec(compile(static, '<pa24-static-firewall>', 'exec'), environment)
for key in ('baseline_resolved', 'baseline_jars', 'approved', 'dependency_only', 'counterparts', 'packaged_literal', 'collisions'):
    print(key, len(environment[key]))
print('static rejection probes', environment['probes'])
PY
```

### Future deterministic runner only — not executed in this planning gate

Require the pinned JDK/Maven, Node where applicable, and Docker/PostgreSQL 16.15 environment used by the existing CI/container tests. Use an isolated environment with no Tencent credentials, no selected production keysource configuration and no inherited cloud properties; do not print the environment. Mocked SDK/role construction must remain mocked. Offline planning evidence is not a substitute for these runs.

```bash
cd backend
java -version
./mvnw -B -ntp -v
docker info
./mvnw -B -ntp -Dtest=TencentSsmMediaPlaybackSigningKeyProviderTests,TencentSsmSecretStringClientTests,MediaPlaybackTencentKeySourceIntegrationTests,MediaPlaybackKeyBundleParserTests,FileMediaPlaybackSigningKeyProviderTests,MediaPlaybackFileKeySourceIntegrationTests,MediaPlaybackConfigurationTests,MediaPlaybackCredentialAuthorityTests,SharingQueryBoundaryTests test
./mvnw -B -ntp -Pcontainer-smoke clean verify
./mvnw -B -ntp dependency:tree -Dverbose -DoutputFile=target/pa24-dependency-tree.txt
cd ..
```

Assert focused **9/117** then clean Surefire **36/309**, Failsafe **13/214**, all zero skipped/errors/failures. Run the checked-in full inventory heredoc after clean verify, not against stale or mixed-candidate XML:

```bash
python3 - <<'PY'
import os, textwrap
from pathlib import Path
workflow = Path('.github/workflows/ci.yml').read_text()
block = textwrap.dedent(workflow.split("python3 - <<'PY'\n", 1)[1].split('\n          PY', 1)[0])
os.chdir('backend')
exec(compile(block, '.github/workflows/ci.yml:container-inventory', 'exec'))
PY
```

Run the **existing** `default and opt-in web jar smokes`, **complete** `production jar dependency firewall` (including its `jar tf` and Maven dependency-list commands), and contract-regression steps verbatim on that isolated runner. This helper selects their exact checked-in run blocks without adding a script or changing CI; invoke only after the separately authorized clean build:

```bash
python3 - <<'PY'
import itertools, os, subprocess, textwrap
from pathlib import Path
root = Path.cwd()
workflow = (root / '.github/workflows/ci.yml').read_text()
for name in ('default and opt-in web jar smokes', 'production jar dependency firewall'):
    section = workflow.split('      - name: ' + name + '\n', 1)[1]
    lines = section.split('        run: |\n', 1)[1].splitlines()
    run = '\n'.join(itertools.takewhile(lambda line: not line.strip() or line.startswith('          '), lines))
    subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', textwrap.dedent(run)], cwd=root / 'backend', env=os.environ.copy(), check=True)
PY
```

For contracts, execute the exact `.github/workflows/ci.yml` `contract-regressions` job's Node assertion, `npm ci`, `npm run check:s3`, A2 s3, A3 s3, A4 s2 and A5 s1/s2 commands/arguments, including `--check-doc-links`. This helper runs those existing steps with their checked-in working directories, without adding a runner script or changing arguments:

```bash
python3 - <<'PY'
import itertools, os, subprocess, textwrap
from pathlib import Path
root = Path.cwd()
workflow = (root / '.github/workflows/ci.yml').read_text()
job = workflow.split('  contract-regressions:\n', 1)[1]
for name in ('assert exact node version', 'install locked A1 toolchain', 'A1 s3', 'A2 s3', 'A3 s3', 'A4 s2', 'A5 s1', 'A5 s2'):
    step = job.split('      - name: ' + name + '\n', 1)[1].split('\n      - ', 1)[0]
    directory = root
    if '        working-directory: ' in step:
        directory = root / step.split('        working-directory: ', 1)[1].splitlines()[0]
    value = step.split('        run: ', 1)[1]
    if value.startswith('|\n'):
        lines = value.splitlines()[1:]
        run = textwrap.dedent('\n'.join(itertools.takewhile(lambda line: not line.strip() or line.startswith('          '), lines)))
    else:
        run = value.splitlines()[0]
    subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', run], cwd=directory, env=os.environ.copy(), check=True)
PY
```

No model/spec edits. Source-tree/CRLF environmental failures must be reported with exact exits and owner, not fixed outside scope or suppressed. Historical PA23 exact-head CI passed these jobs; that is not a new PA24 result.

Required complete firewall result remains 72 coordinates / 65 JARs / 8 dependency-only / 64 counterparts / sole packager / seven collisions / 116 probes. Default/web smokes must stay cloud-inert with no source selected and retain existing HTTP/problem/header/redaction assertions. Full verification preserves all public contracts, database/ACL/migration tests and current-state checks. Failures or missing prerequisites are explicit FAIL/BLOCKED/NOT RUN, never fabricated green evidence. No live Tencent test belongs to these commands.

Before handoff, use the explicit read-only Git prefix as needed for `diff --check`, `diff --name-only`, `diff --exit-code -- backend .github/workflows/ci.yml api protocol security acceptance deployment`, and `ls-files --others --exclude-standard`. Compare Control, canonical docs, README/resources and all tracked paths against entry hashes; include untracked artifacts in scope checking and leave pre-existing `.codex` untouched. Confirm the only task-created repository file matches the applicable gate's allowlist. Record artifact byte size/SHA-256 after final edit; do not insert a self-referential hash inside that artifact.

## 10. Residuals and external input owners

No blocking planning question remains. These are explicit **live acceptance dependencies**, not choices delegated to the implementation author or automatic authorization for cloud execution.

| Residual / open input | Owner / required disposition | Trace |
| --- | --- | --- |
| Real region/resource/version IDs, immutable-version/SecretString/response-echo capabilities | Platform/Security, CV-2 deployment acceptance; supply protected manifest and evidence or block live release | G2/G3/G7 |
| Actual CVM attachment/trust, least-privilege CAM policy/resource granularity and environment isolation | Platform/Security, CV-3; only attached-instance-role route accepted; non-CVM/TKE/OIDC requires a different goal | G1/G7 |
| Metadata and DNS/total-call liveness, shared credential-refresh wait, default underlying connection retry | Platform/Security + Operations; measured censored/completed behavior and explicit suitability decision; no fabricated bound | G5 |
| Per-request SSM quota, latency, throttling, availability, cost and thread saturation | Platform capacity/Security + Operations; approved external load/sample/threshold evidence | G5/G7 |
| Q22 maximum TTL/skew/propagation and relevant outstanding replay/session horizon; last K1 issuance and retention approval; approved ordinary A→B evidence window | Security/Sharing + Operations; supply validity/clock/horizon references before observations, block invalid B overlap and live C until approved retention/drain evidence exists; never shorten retention for evidence; no numeric value chosen here | G2/G4/G6 |
| Timestamp/provenance capture and protected harness/custody through A/B/C; optional time-valid old-K1 C acceptance requirement | Sharing/Security + Operations; implement section 4's evidence classification in the future S1 runbook, supply independently approved source/bundle attestations, retain no credential material in artifacts. If additional time-valid C evidence is required, approve a separate feasible non-serving cohort/input path or mark it NOT EXECUTED and block that requirement; deterministic evidence is not live acceptance | G2/G4/G6/G7 |
| Whole-cohort quiesce/drain, mixed-version exposure, rollback authorization and audit/evidence routing | Platform deployment + Security + Operations; use existing controls, preserve current-state checks, never silently restore retired/compromised K1 | G4/G6 |
| Java/SDK String lifetime, logging/collector drift and artifact/advisory drift | Security + Build/Observability; redaction checks are not heap erasure, HSM or proof of all runtime egress | G4/G5 |
| Future deterministic runner/Docker/CRLF prerequisites and no current XML/JAR | Backend QA/build owner; run section 9 under separate authorization and record actual results | G7 |

CV-1/KMS, CV-4/COS/WORM/private endpoints, CV-5 supplementary cloud logs, legal retention and unrelated identity/business/security work are not closed by this adapter/runbook. Do not add KMS/COS calls or change their capability declarations. Operations evidence cannot replace required Security/Sharing policy approval.

## 11. Completion report and binding stop

Planning completion report must contain: task/work unit, GPT/Codex runtime and model-attestation limit, gate/date, author-completion exit status (not gate acceptance), sole artifact path/byte size/SHA-256, observed HEAD versus requested product base, core **no production/test/POM/CI changes** decision, single S1 and future one-document allowlist, freshly measured source=CI/POM/firewall inventory, exact read-only validation performed versus future tests not run, residual owners, pre-existing `.codex` exclusion, and every prohibition below.

Future S1 report, in the one allowlisted composed document, must additionally contain: artifact/configuration provenance references; G-to-decision/test/runbook coverage; changed-path inventory; actual toolchain and command exits; focused/clean source=CI=XML totals and zero skips/errors/failures or explicit failure; real runtime/JAR firewall/smoke/contract evidence; separate external live rows all NOT EXECUTED unless a later explicit cloud gate authorizes them; section 4's per-observation issuedAt/notBefore/expiresAt/current timestamps, validity/clock/horizon approvals, validity booleans, source/version/K2-only attestations and expected/actual result/classification; links from valid B overlap through approved retention to C key absence without asserting old-K1 time validity, plus optional time-valid C requirement disposition/owner; redaction and protected-surface checks; all unresolved owners; and no gate-advancement claim. A documentation-only pass is not production readiness or live acceptance.

**Binding stop: return this plan and stop before plan review.** Do not delegate; edit Control/source/tests/POM/CI/resources/README or existing artifacts; acquire/use real credentials or secrets; provision or contact Tencent Secret Manager/KMS/CAM/COS; run live cloud tests; create secret fixtures; stage/commit/push or create a branch; open/mutate a PR; approve; mark ready; request reviewers; comment externally or mutate issues; dispatch/enter review, implementation or any later gate. No cloud provisioning, automation, cache, watcher, moving stage/latest/current alias, fallback source, second registry/authority, credential abstraction, retry framework, new public API/DTO, DB/ACL/migration/V001–V013 change, topology/COS/player/CDN/TLS/LB/WAF change or Q22 numeric choice is authorized. No independent review, acceptance, merge or live-platform pass is claimed.
