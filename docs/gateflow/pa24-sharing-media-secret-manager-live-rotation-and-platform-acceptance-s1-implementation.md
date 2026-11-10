# PA24 — CVM immutable-version rollout runbook and repository acceptance evidence

## 1. Status, authority and scope

- Task label: `pa24-secret-manager-rollout-s1-evidence-fix2-gpt-20261110-01`; earlier evidence fix `pa24-secret-manager-rollout-s1-evidence-fix1-gpt-20261110-01`; original S1 task `pa24-secret-manager-rollout-s1-implementation-gpt-20261110-01`; work unit `PA24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`; date 2026-11-10; gate `implementation`; requested route Codex / GPT `gpt-6.1-sol` / high. The session cannot independently attest model identity.
- Binding Goal Confirmation and current implementation gate: `docs/gateflow/project-execution-control.md` (active PA24 section; gate entered from accepted plan checkpoint).
- Accepted-plan checkpoint and Controller validation source HEAD: `17ec0afb6242857e9d6f50ede931d3e30742e2b5` (`gateflow: accept plan for PA24-sharing-media-secret-manager-live-rotation-and-platform-acceptance`). Accepted fixed plan SHA-256 `CBCC122964E2BA0C4BF9D4E0458CCA95AA3BBBC00484D48D673B799A0CDFEDCF`; Plan Fix 1 SHA-256 `712254DE6A09BA4E4C8313FD35175432AAA675F7BE696540D4CDA32CE9F234AF`.
- Four plan reviews: initial A `docs/reviews/plan-review-20261110-141317.md`, SHA-256 `9265684F7840D09722E1C6039C05D57A2366523A4B531F8D273908B00713FBC0`; initial B `docs/reviews/plan-review-20261110-142200.md`, SHA-256 `5D20DA7E2E0E6CABC0D109F553F8657866B059CC644D5047E48F65E261E0EF7A`; re-review A `docs/reviews/plan-review-20261110-144408.md`, SHA-256 `F92EDFD267D990793032A335455F0FBF1A07D417B1CC4E66376BA825DAAAEBBD`; re-review B `docs/reviews/plan-review-20261110-145224.md`, SHA-256 `8BC0F3D2CAABA4AE7282EDD97583BE4103D6383C1F53A075D357A7E2CBE6B186`.
- Historical Control hash observed during original implementation: SHA-256 `a4e64bef72fd8c7542a79520b2d4d20518845d8eabe518cf2cf04728f3d319ca`; earlier evidence fix: SHA-256 `C54182C60AFD9B4D18E95CF751C6B36029A38B9C064221B29AFA86F2E18EBD0A`. Current Control read for this durable-evidence fix: SHA-256 `FD2906E17F985725454728E71D06DAE9A3971EE48CFEE71D3996A1291F688783`; its active PA24 record supplies the durable Controller validation and supersession below. These are provenance snapshots, not Control edits; the accepted checkpoint and fix artifact retain their historical Control digests.
- Repository validation status: **PASS, separately captured Controller evidence** for the focused 117 tests, full 49/523 source=CI=XML inventory, production JAR smokes, complete dependency firewall and all eight contract steps (§8). The original sandbox failures remain disclosed as superseded runner limitations, not product failures. This evidence fix does not rerun those suites.
- This artifact is both a reusable operator runbook and a dated repository-validation record. It does not authorize or claim live cloud work, production readiness, approval, merge, CI success, gate acceptance or code review. Runtime/provider behavior is unchanged; no Q22 value is selected.
- One allowed repository write: this existing S1 document only; no new file. Only evidence status/provenance, observed validation, G7 traceability and author handoff are updated. No source, test, POM, CI, migration, Control, canonical document, existing plan/review, script, configuration or Git metadata is changed. No stage/commit/push/PR/approval/ready/reviewer request/external comment/Tencent contact/credential/resource/cloud action is performed. Stop before code review.

## 2. Reusable rollout contract

### 2.1 Exact configuration and source boundary

For the initial Tencent CVM deployment, use the exact prefix `onecall.sharing.media-playback.signing-key-source.` and this existing contract. These are names and symbolic values only; never place real values in this document.

| Property | Required contract |
| --- | --- |
| `provider` | Exactly `tencent-ssm`; explicit, case-sensitive, no trim, default, profile inference or compound selection. `file` is a separate explicitly selected route, never fallback. |
| `bundle-path` | Absent, not blank, at every effective configuration layer. Any presence suppresses Tencent and does not select file. Inspect presence without rendering values. |
| `tencent.region` | Required literal; ASCII `[a-z0-9]+(?:-[a-z0-9]+)+`, at most 64 characters. No default or region probing. Platform supplies the real approved region and endpoint/network evidence externally. |
| `tencent.secret-name` | Required exact opaque literal, 1–1,024 UTF-16 units; no edge whitespace, controls, `${`, or unpaired surrogates. No trim, decode or synthesized resource. |
| `tencent.version-id` | Required exact opaque literal, 1–256 UTF-16 units, same validation. Platform manifest proves the selected ID is immutable; no stage/latest/current alias or alternate lookup. |
| `tencent.connect-timeout-seconds` | Optional; default `2`; ASCII integer `1`–`5`. Configures connect and write phases. No separate write property. |
| `tencent.read-timeout-seconds` | Optional; default `3`; ASCII integer `1`–`10`. No sign, leading zero, whitespace, unit, zero or infinity. Neither value is Q22. |

Invalid/missing selected settings or unresolved placeholders fail closed without SDK/metadata read. No static SecretId/SecretKey/token, default/env/profile/OIDC chain, debug/body capture, alternate provider, fallback, cache, moving alias, watcher, second registry/authority, retry framework, new service or dynamic selection is permitted. The Tencent implementation uses only direct CVM attached-instance-role credentials (`CvmRoleCredential`) and the existing SSM client route. Non-CVM identity, including TKE/OIDC, is outside this goal. It requests `GetSecretValue`; exact request/response secret-name and version equality is required. The adapter cannot distinguish a mistakenly selected other valid immutable version if its response matches; compare every effective setting and response attestation with the approved external manifest.

### 2.2 Exact bundle and symbolic continuity

The complete SecretString is one strict schema-v1 JSON bundle: exact root fields `schemaVersion` (integer `1`) and nonempty `keys`; each entry has exactly `kid`, `role`, `keyHex`. Kid matches `[A-Za-z0-9_-]{1,32}` and is unique. Role is exactly `ACTIVE` or `VERIFY_ONLY`. `keyHex` is exactly 64 lowercase hexadecimal characters (32 bytes, HMAC-SHA-256/256-bit K20 material). There is exactly one ACTIVE; any VERIFY_ONLY entries are sorted by kid. Unknown/duplicate fields, duplicate kids, unknown states, zero/multiple ACTIVE, malformed bytes/encoding, BOM, trailing tokens, excessive size/depth or any bad entry reject the whole set; never filter or partially accept.

| Symbolic immutable version | Required contents |
| --- | --- |
| A | K1 ACTIVE. |
| B | K2 ACTIVE plus identical K1 material under identical K1 kid as VERIFY_ONLY. |
| C | Identical K2 material under K2 kid as ACTIVE; no K1 entry. |

A/B/C are labels for separately provisioned immutable versions, not real version IDs or kids. K1/K2 are symbolic kids, not key material. Platform/Security establishes continuity out of band and attests it without exporting bytes. Never reassign a kid to different material. Each stage uses a fresh provider/client configuration pinned to that exact version. No parser cross-version memory is implied.

## 3. Preconditions, approvals and evidence custody

All real platform and cohort evidence in this PA24 record is `NOT EXECUTED`. These are future prerequisites, not implied approvals.

| Input / owner | Required external evidence and destination | Missing/failed disposition |
| --- | --- | --- |
| CV-2: Platform/Security | Protected approved manifest: environment, artifact digest, symbolic A/B/C references mapped to actual immutable IDs, fixed region/resource, SecretString support, version availability/disable/recovery behavior, request/response identity-equality capability, effective configuration revision. Store actual identifiers only in the approved protected manifest; repo stores a redacted reference/digest. | Block platform acceptance and release; wrong or unproven immutable version is not accepted. |
| CV-3: Platform/Security | CVM identity inventory, actual attached-instance-role/trust evidence, environment isolation and CAM least-privilege review for only needed read action/resource/environment; prove the application has no static/default/env/profile/OIDC route. Validate actual CAM action/resource granularity externally; do not invent policy syntax or accept wildcard as shortcut. | Block; non-CVM route or unsupported least privilege requires a different approved goal. |
| Harness/custody: Sharing/Security + Operations | Approved synthetic PLAYBACK/SEEK/RENEW requests, fixed identity/facts and known-valid state; protected ephemeral comparison memory; external protected evidence-store/custody reference and retention/recovery method through the required B/C observations. Record only opaque reference and assertion booleans here. | Missing, lost or unapproved state: affected evidence `NOT EXECUTED`; no repository credential fixture. |
| Validity/clock/horizon: Security/Sharing + Operations | Approved ordinary validity, clock/skew uncertainty and propagation basis; maximum outstanding credential/session/replay horizon, last possible K1 issuance and in-flight/fleet drain basis; decision whether extra time-valid C evidence is required. Provide references before A. | No invented TTL, skew, interval or Q22 number. Blocks invalid B overlap and live retirement/C; missing feasible A-to-B observation window blocks B. |
| Deployment/quiesce/recovery: Platform deployment + Security + Operations | Existing admission/traffic pause, whole-cohort stop/restart and drain procedure, downtime approval, recovery authority and evidence/audit destination; all-instance config/artifact verification. | Cannot quiesce/drain means stop; no rolling substitute or topology change. |
| Capacity/liveness: Platform capacity/Security + Operations | Approved load/sample and threshold method; quota, throttle, availability, latency, cost, thread-saturation and recovery assessments; metadata/DNS/total-call measurement budget and suitability decision. | Keep open/blocked; no threshold or bound invented here. |
| Secret lifecycle: Platform/Security | External immutable-version lifecycle, historical access, disable/destroy authorization and recovery policy, distinct from logical K1 removal. | K1 removal from C only means logical retirement; no cloud lifecycle action implied. |
| Runtime observability: Security + Build/Observability | Redaction/logging/collector review, dependency/advisory drift review, Java/SDK String-lifetime risk. | Redaction is not heap erasure, HSM isolation or proof of all egress. |
| Deterministic runner: Backend QA/build owner | Pinned JDK/Maven/Node and Docker/PostgreSQL 16.15 runner; clean checkout/EOL-compatible environment, cached/approved dependencies, report artifacts. | Record exact BLOCKED/FAIL/NOT RUN; never edit wrapper/source/CI to work around. |

Evidence references must identify the protected system and owner without embedding secret-bearing URLs, raw cloud labels, account/role/resource/version IDs, response bodies, credentials, key material, payloads, MACs, tokens or raw SDK errors. A continuity or source attestation must be independently established per instance/version, not inferred solely from authority results: record booleans/references for fixed C identity/version, valid complete K2-only bundle and absence of K1; source-wide failure is not key-absence proof.

## 4. Timestamp, classification and disposition rules

For each observation record evidence ID, G-trace, purpose, symbolic version/cohort/kid, artifact/configuration digest reference, protected manifest/custody reference, `issuedAt`, `notBefore`, `expiresAt`, UTC operation-start and operation-completion times, approved validity/clock/skew/horizon references, time-valid-at-observation `TRUE`/`FALSE`/`UNKNOWN`, expected and actual authority result, source identity/version/bundle attestation reference, assertion booleans, owner and disposition. Record timestamps only; credential, replay anchor, expiry-bearing object, request facts and comparison values remain inside approved protected ephemeral state. Evaluate both start and completion under approved clock/skew uncertainty. Authority time rules are microsecond-normalized `issuedAt == notBefore`, `issuedAt <= now`, and `now < expiresAt`; runtime skew does not extend validity.

Evidence classes are exactly `VALID-TIME OVERLAP`, `KEY-ABSENCE`, and `FRESH-ISSUANCE`. Dispositions are `PASS`, `FAIL`, `INVALID`, or `NOT EXECUTED`. Missing approvals, timestamps, provenance, custody, source attestation or unknown validity cannot pass. `INVALID` means an observation occurred but cannot prove the intended assertion; `NOT EXECUTED` means no qualifying observation occurred. A validity boundary crossed or uncertain makes required time-valid evidence `INVALID` (or `NOT EXECUTED` if no observation); block acceptance/reopening. Never relabel expiry as overlap.

- B old-K1 proof is time-valid only: verification `Verified`, replay exact byte-identical recorded credential/anchor/expiry for each purpose, no substitute kid and no TTL/expiry extension. Keep the same request identity, facts, purpose and bounds. If the check crosses expiry, record actual outcomes (which can be Invalid verification or Malformed replay) but classify evidence `INVALID`/`NOT EXECUTED`, block overlap acceptance/reopening, and do not silently revert serving B to A after K2 issuance. Only an explicitly approved safe new A/cohort sequence can retry.
- Full B serving retention is not the evidence credential's validity interval. Keep B until the approved maximum outstanding credential/session/replay horizon from last possible K1 issuance, propagation/drain basis and all retirement preconditions are satisfied. Preserve B evidence as historical proof. Never shorten safe retention to fit the evidence; do not imply old K1 remains time-valid throughout it.
- C old-K1 proof is `KEY-ABSENCE`, not time-valid overlap: use the original protected K1 input even if expired and record its actual time validity (possibly FALSE). Independently attest current fixed C source/version and valid complete K2-only bundle, with K1 absent, for each observation. Under current authority semantics, recorded-kid absence yields old replay `Unavailable`; verification with K1 absent yields `Invalid`, but bare `Invalid` is insufficient because expiry also yields Invalid. Require no replacement and no source-wide failure. This establishes key absence/no replacement only—not old K1 validity, session continuity, instantaneous fleet revocation or cloud acceptance from deterministic tests.
- Fresh K2 issuance and verification at B and C must each be independently time-valid and recorded using their own timestamps. Optional time-valid old-K1 C evidence is a separate Security/Sharing decision: if required, owners must approve a feasible, isolated non-serving cohort, ordinary policy-valid timing, provenance and custody. It cannot shorten serving B retention, restore K1 to serving C or introduce a hidden long-lived credential. Otherwise mark the optional row `NOT EXECUTED` and block that additional requirement.

## 5. Checkable A/B/C acceptance matrix

Run every applicable row for each purpose `PLAYBACK`, `SEEK`, and `RENEW`, with protected harness custody and known-valid state. All live rows below are presently **`NOT EXECUTED`**; named tests are deterministic repository oracles, not live evidence.

| Stage/check | Expected authority result and invariant | Deterministic oracle / evidence class |
| --- | --- | --- |
| A fresh issue | `Issued`, kid K1 ACTIVE; time-valid; preserve protected input and timestamp record. | `MediaPlaybackTencentKeySourceIntegrationTests.tencentBundleIssuesActiveForEveryPurpose`; A setup in rollout tests. `FRESH-ISSUANCE`. |
| A verify/replay | `Verified`; replay returns exact same bytes, anchor and expiry; no new identity or admission recount. | `advancingClockReplayReturnsFirstCredential`, `freshAuthorityReconstructsCommittedAnchor`, purpose golden tests. `FRESH-ISSUANCE`. |
| A absent K2 | Old A snapshot cannot verify K2-era credential; absent kid gives `Invalid`, replay gives `Unavailable`. This is why mixed A/B serving is unsafe. | `disabledAbsentAndUnknownKeysFailClosed`; authority decode/recorded-kid ordering. Repository oracle only. |
| B old K1 verify/replay | Within approved validity only: `Verified`; byte-identical replay of protected credential/anchor/expiry with same request bounds; K1 remains same kid/material as VERIFY_ONLY, no replacement kid, no TTL extension. Out of/uncertain validity: `INVALID`/`NOT EXECUTED`, acceptance blocked. | `explicitVersionRolloutVerifiesOldAndNewKeys`, `overlapReplaysExactCredentialForEveryPurpose` (fixed-clock fixture advances within its test validity). `VALID-TIME OVERLAP`. |
| B fresh issuance/verify | `Issued` using K2 ACTIVE and `Verified`; own timestamps time-valid; differs from A credential as a boolean assertion only. | `explicitVersionRolloutVerifiesOldAndNewKeys`, `tencentBundleIssuesActiveForEveryPurpose`. `FRESH-ISSUANCE`. |
| C old K1 verify/replay | With independent valid C/K2-only source evidence: verification `Invalid`; exact recorded replay `Unavailable`; no substitute kid or replacement; same otherwise well-formed replay request/bounds. Record old K1 validity explicitly, do not infer absence from expiry. Source failure is not a pass. | `removedRecordedKidNeverMintsReplacement`, `equalMaterialNeverAliasesRecordedKid`, `disabledAbsentAndUnknownKeysFailClosed`; repository oracle only. `KEY-ABSENCE`. |
| C fresh K2 issue/verify | `Issued` K2 then `Verified`, each time-valid; C remains a valid source with K2 and no K1. | `removedRecordedKidNeverMintsReplacement` asserts fresh K2 issuance; existing full-claim verification oracle covers unchanged path, not a new live C measurement. `FRESH-ISSUANCE`. |
| Any source/CAM/credential/bundle failure | `Unavailable` for source-dependent issue, verify and replay; no token access before key availability, state/evidence/controlled open after failed verification, no last-good use or alternate source. | `transportAndParserFailuresHaveNoFallback`, `unavailableSourcePrecedesOpaqueTokenAccess`, `unavailableSourcePreventsStateEvidenceAndOpen`, client/provider failure suites. `FAIL` for a tested operational failure; unavailable observation remains not a retirement pass. |
| Valid source, unknown/absent kid | Distinguish from source outage: verification `Invalid`; recorded replay `Unavailable`; do not substitute another kid. | `disabledAbsentAndUnknownKeysFailClosed`, `removedRecordedKidNeverMintsReplacement`. `KEY-ABSENCE` only with independent C source attestation. |
| Optional time-valid K1 at C | Execute only if separately approved non-serving cohort and valid ordinary evidence window exist; else exact disposition `NOT EXECUTED`, owner Security/Sharing, blocks only that extra requirement. | No deterministic controlled-clock test is live evidence. |

No result bypasses existing state/current-membership/admission/expiry/object/window checks. Deterministic synthetic fixtures do not establish cloud role, real bundle, quotas, latency, time-valid live credentials or retirement.

### Whole-cohort sequence and rollback boundary

1. **Prepare A:** Platform/Security approves CV-2/CV-3 manifest, immutable version, role/CAM, effective config and artifact; Sharing/Security + Operations approve harness/custody, evidence request facts, validity/clock/skew and feasible A-to-B window. Verify all effective properties without printing values. If any gate is missing, stop before live cohort execution.
2. **A establish:** pin all acceptance-serving instances to fixed A; independently reconcile every instance/artifact/config with protected manifest; execute and preserve A rows for all purposes. Operations captures last possible K1 issuance and current fleet/drain state.
3. **Prepare B:** provision/version only through separately approved external controls, outside this task; independently attest identical K1 kid/material and K2 ACTIVE, immutable version, SecretString/response identity and role access. Approve traffic/admission quiesce and drain; ensure B overlap can finish inside approved ordinary validity window.
4. **A→B cutover:** pause admission and media traffic using existing controls, drain in-flight work, stop/remove every A-configured serving instance, restart/reconfigure the entire acceptance serving cohort to explicit fixed B, verify all instances, run B checks before reopening. Do not use rolling mixed-version deployment: A cannot verify fresh K2. If quiesce, drain or valid window fails, keep closed and block; no topology workaround.
5. **B serve/retain:** reopen only after whole-cohort B checks and approvals; preserve valid-time B evidence. Keep serving B until the entire approved outstanding credential/session/replay horizon from last possible K1 issue and propagation/drain/retirement conditions pass. Do not reduce retention for evidence feasibility. B→A after any K2 issuance is incompatible because A cannot verify K2.
6. **Prepare C and B→C:** independently validate fixed C/K2-only source and bundle, K1 absence, artifact/config, recovery authority and full retention completion. Quiesce/pause/drain again, stop/remove all B-configured instances, restart the complete cohort to fixed C, verify every instance, run old-K1 `KEY-ABSENCE` and fresh time-valid K2 checks, then reopen only after all required rows pass. A lingering B instance still has K1; no instantaneous revocation claim.
7. **Recovery:** before any K2 issuance, whole-cohort A return may be considered only with authorized, available K1. After K2 issuance, do not return to A. Keep approved B while rolling back compatible application code if safe; otherwise remain unavailable and escalate. A separately approved new immutable recovery bundle K1 ACTIVE + unchanged K2 VERIFY_ONLY is optional, not a mandatory stage; test both populations before reopening (see the conditional recovery register in §7). C→B restores K1 verification/replay and undoes retirement; require explicit Security approval and K1 not compromised, disabled/destroyed or prohibited by policy. Use the approved existing whole-cohort control: quiesce/pause admission and media traffic, drain in-flight work, stop/remove every C-configured serving instance, restart/reconfigure the complete acceptance serving cohort to explicit fixed B, and verify every instance/artifact/effective configuration against the protected manifest. Re-run the applicable time-valid B overlap (old-K1 exact replay/verification) and fresh time-valid K2 issuance/verification checks for every purpose before reopening; expired or uncertain validity is INVALID/NOT EXECUTED and blocks reopening, not overlap success. Document the changed acceptance state and explicitly invalidate the former C retirement claim before reopening; that retirement pass no longer remains effective. No rolling C→B recovery or mixed C/B serving is permitted; if any required control, check, authorization or record is missing, keep traffic closed and escalate. Never recover destroyed material via plaintext backup, file, environment or alternate source.
8. **Retirement distinction:** removal of K1 from C is logical acceptance retirement only. Cloud disable/destroy, historical version access, audit, retention and recovery are separately authorized Platform/Security lifecycle actions. Do not infer deletion or erase already-buffered media/issued credentials.

No real cloud action is authorized or performed by PA24 S1. All eight operational steps remain external/future.

## 6. Timeout, retry, redaction and evidence-record template

### Liveness truth—no stronger promise

| Layer | Exact current contract | Required evidence / limit |
| --- | --- | --- |
| SSM phases | Configured connect and write use connect setting (default 2 s; allowed 1–5); read uses read setting (default 3 s; allowed 1–10). These are phase profile settings, not a full-call bound. | Record selected approved phase settings and elapsed behavior; do not sum them into a deadline. |
| CVM metadata | `CvmRoleCredential` metadata HTTP has no configured connect/read/call timeout; credential snapshot refresh may serialize/wait. | Separately measure cold acquisition, refresh, unreachable/unresponsive metadata, waiting callers/threads and censored/in-progress calls under externally approved stop budget. A probe stop does not cancel/bound production call. |
| DNS / total call | No configured SSM total-call deadline or DNS deadline; full invocation can exceed phase values or remain blocked. | Measure full call, DNS, cold/warm state and censoring; no inferred upper bound or SLO. |
| Retry | One application `GetSecretValue` invocation per valid read; no application retry or `AbstractClient.retry`; underlying OkHttp connection retries may occur and no disabling switch is exposed here. Metadata access is separate. | Count application calls and transport attempts separately under approved fault case; mark unobservable attempts `UNOBSERVED`. Mockito counts/cloud logs alone do not establish one transport attempt. |
| Suitability | Fail-closed result mapping is not guaranteed timely `Unavailable`; a stalled SDK/metadata/DNS call may remain in progress. | Platform/Security + Operations accept measured limitations against externally supplied requirements or block acceptance. No invented limits for quota, latency, availability, cost or threads. |

For measurements include evidence ID/G, approved scenario/perturbation, artifact/config and role review references, UTC start/end plus monotonic elapsed ms, configured phase values, cold/warm/refresh, request concurrency/sample size, completed/in-progress/censored counts, application/transport/metadata counts or `UNOBSERVED`, nonsecret assertion booleans, owner, protected destination and PASS/FAIL/INVALID/NOT EXECUTED. No payload/body capture for attempt measurement.

### Redaction and protected surfaces

Never record or render bundle, key bytes/hex/base64, SecretString/binary, credential triple, authorization/header, playback token/payload/MAC, anchor contents, actual resource/version/account/role labels, SDK request ID, response, raw exception/message/cause, parser detail or sensitive config. Compare bytes/credentials only inside protected harness; export boolean/equality result and opaque evidence reference. SDK debug remains false; no HTTP body logging/tracing. Check logs, errors, JSON, `toString`, test reports, CI artifacts and repository diff for canaries and secret-bearing values. Buffer clearing does not erase Java/SDK `String` copies, establish heap erasure or provide HSM isolation. Redaction review is not proof of all process/collector egress.

### Reusable evidence record (copy per purpose/check)

```text
Evidence ID: <opaque-symbol>
G trace / stage / check / purpose: <G1-G7> / <A|B|C> / <check> / <PLAYBACK|SEEK|RENEW>
Disposition / evidence class: <PASS|FAIL|INVALID|NOT EXECUTED> / <VALID-TIME OVERLAP|KEY-ABSENCE|FRESH-ISSUANCE>
Owner / protected destination reference / custody reference: <owner> / <opaque-ref> / <opaque-ref>
Artifact digest ref / config revision ref / manifest CV-2 ref / CV-3 role-CAM ref: <refs>
Symbolic source and immutable version / instance cohort: <A|B|C> / <opaque-ref> / <cohort-ref>
Symbolic expected bundle assertions: K1 same kid/material at B <T|F|U>; K2 ACTIVE <T|F|U>; K1 absent at C <T|F|U>; response identity exact <T|F|U>; source read valid <T|F|U>
Credential symbolic kid / issuedAt / notBefore / expiresAt: <K1|K2> / <UTC> / <UTC> / <UTC>
Operation start UTC / completion UTC / monotonic elapsed ms: <UTC> / <UTC> / <number>
Approved validity, clock/skew, horizon, last-K1-issue/drain refs: <opaque refs>
Time-valid throughout observation: <TRUE|FALSE|UNKNOWN>
Expected result / actual result: <authority result> / <authority result>
Protected comparisons: exact replay bytes <T|F|U>; anchor/expiry unchanged <T|F|U>; substitute kid <T|F|U>; TTL extended <T|F|U>; no replacement <T|F|U>; fresh K2 verify <T|F|U>
Failure class: <NONE|SOURCE/CAM/CREDENTIAL|MALFORMED_BUNDLE|IDENTITY_MISMATCH|VALIDITY_BOUNDARY|HARNESS/CUSTODY|QUOTA/LATENCY/LIVENESS|OTHER>
Redaction/protected-surface check reference / findings: <opaque-ref> / <nonsecret summary>
Actual secrets, payloads, tokens, MACs, credentials, anchors, real IDs, raw SDK errors: NEVER ENTER THIS RECORD
```

## 7. External input register and owner disposition

Every real CVM/platform/CAM/Secret Manager/quota/latency/metadata/DNS/transport/live-cohort line remains `NOT EXECUTED`; no live row was converted into evidence in this task.

| Live row / destination | Owner | Current disposition / unblock condition |
| --- | --- | --- |
| CV-2 immutable manifest, SecretString, response identity, A/B/C versions | Platform/Security; protected deployment acceptance system; repo only redacted reference | `NOT EXECUTED`; supply protected proof before deployment; no values in repo. |
| CV-3 CVM attachment/trust, CAM least privilege, isolation | Platform/Security; protected CVM/CAM acceptance record | `NOT EXECUTED`; exact direct instance-role route only; fail closed if role/policy cannot be proven. |
| Effective config/artifact/per-instance reconciliation | Platform deployment + Security; protected manifest/release record | `NOT EXECUTED`; prove exact fixed version and absence of credentials/fallback without printing config. |
| Harness, synthetic requests, credential/anchor custody through retention | Sharing/Security + Operations; protected harness/evidence vault | `NOT EXECUTED`; name approved tool/store, custodian, retention/recovery; lost state cannot pass. |
| Ordinary validity/clock/skew, B window, Q22 and maximum outstanding horizon | Security/Sharing + Operations; protected policy/clock reference | `NOT EXECUTED`; numeric values deliberately unselected; blocks B on invalid window and live C on missing retention/drain. |
| Independent B K1 and C K2-only bundle/source attestations | Platform/Security + Operations; protected attestation record | `NOT EXECUTED`; non-egressing continuity method, fixed C identity/version and K1 absence must be attested independently. |
| Optional time-valid old-K1 C evidence | Security/Sharing decision; Platform/Operations non-serving cohort record | `NOT EXECUTED` until requirement and feasible isolated path separately approved; otherwise blocks only that added requirement. |
| Whole-cohort quiesce/drain, downtime, recovery and audit | Platform deployment + Security + Operations; existing change/audit system | `NOT EXECUTED`; no mixed-version rolling, no new topology, no rollback without explicit authorization. |
| Conditional new immutable recovery bundle: K1 ACTIVE + unchanged K2 VERIFY_ONLY | Security/Sharing + Platform deployment/Operations; existing protected recovery/change/audit system | `NOT EXECUTED`; separately approved only, never edit immutable B in place. Validate both old credential populations and new K1 issuance before reopening; document the changed acceptance state. This mirror has no in-repo deterministic oracle and is not a fourth automatic stage; no cloud action is authorized here. |
| Quota/latency/throttling/cost/availability/thread saturation | Platform capacity/Security + Operations; approved load-evidence system | `NOT EXECUTED`; sample/threshold and suitability decision supplied externally, never invented. |
| Metadata/DNS/total-call/cold-refresh/retry behavior | Platform/Security + Operations; protected observability evidence | `NOT EXECUTED`; record censored and completed observations; accept limitation or block. |
| Disable/destroy/historical access and logical retirement | Platform/Security; cloud lifecycle and audit system | `NOT EXECUTED`; C removes K1 logically only; cloud action requires separate authorization. |
| Java/SDK String, heap/collector/logging/advisory review | Security + Build/Observability; protected security review | `NOT EXECUTED`; no HSM or heap-erasure claim. |
| Deterministic runner prerequisites/XML/firewall outputs | Backend QA/build owner; CI-equivalent isolated runner | PASS on the separate Controller runner (§8); original sandbox FAIL/BLOCKED results are superseded runner limitations, retained below. Not hosted-CI or live-platform acceptance. |

CV-1/KMS, CV-4/COS/WORM/private endpoint, CV-5 supplementary cloud logs, legal retention and unrelated identity/business work are not closed here. No KMS/COS calls or capability declarations are added.

## 8. Observed repository validation — 2026-11-10

### Current evidence: separate Controller validation and provenance

The Controller's clean source came from accepted HEAD `17ec0afb6242857e9d6f50ede931d3e30742e2b5`, extracted with `git -c core.autocrlf=false archive HEAD`. This exercised the **checked-in LF `backend/mvnw`**, without editing source, CI or Git metadata. The earlier archive's default autocrlf/Windows-checkout CRLF wrapper failure was an extraction/runner limitation, not a defect in the checked-in wrapper. No normalization, wrapper substitution or assertion relaxation was needed for the Controller pass.

Controller runner versions: Temurin `21.0.13+9`, Maven wrapper `3.9.9`, Docker `29.1.3`, real PostgreSQL `16.15`, Node `22.23.4`, npm `10.9.9`. No Tencent credentials or production keysource configuration were passed. These are repository-only deterministic results, not historical PA23 results or hosted-CI execution.

Evidence attribution: WSL restarted and cleared the earlier `/tmp` Controller validation artifacts. The Controller reran validation on the clean `D:` snapshot and retained the seven artifacts below at `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence`. After that `/tmp` loss, this evidence-fix author directly read the durable logs and JSON through the WSL mount `/mnt/d/codex-temp/pa24-code-review-20261110.yhNPsc/a/evidence` and independently rechecked all seven byte counts and SHA-256 digests against the current Control's active PA24 record and the supplied evidence. They match exactly. The ledger records **Controller-captured validation, directly rechecked durable evidence**, not an author rerun of the suites. The artifacts remain retained on `D:`; no unprovided start times or per-step durations are inferred.

| Controller validation | Observed result / exit | Evidence |
| --- | --- | --- |
| Required nine-class focused wrapper suite (exact class list in historical command table below) | PASS / `0`: 117 tests, zero failures/errors/skips. | `focused.log` |
| `./mvnw -B -ntp -Pcontainer-smoke clean verify` | PASS / `0`, 8m42s: Surefire 36 classes / 309 tests; Failsafe 13 classes / 214 tests; total 49 classes / 523 tests; zero failures/errors/skips in both suites. Production JAR: 39,257,237 bytes. | `full-verify.log`; `report-summary.json` |
| Checked-in inventory assertions | PASS: exact source=CI=XML `36/309 + 13/214 = 49/523`; annotation line scan `524`; POM `13`; zero skips/errors/failures. The line scan is not an additional executable test. | `inventory.log`; `report-summary.json` |
| Existing default and opt-in web JAR smokes; complete production dependency firewall | PASS / `0`: 72 approved coordinates / 65 packaged JARs / 8 dependency-only / 64 counterparts / sole packager / seven approved collisions / 116 adversarial probes. This is the complete runtime/JAR firewall, not merely the earlier static rejection segment. | `smokes-firewall.log` |
| Checked-in contract regressions | PASS: all eight steps exit `0`: Node pin, locked `npm ci`, A1 s3, A2 s3, A3 s3, A4 s2, A5 s1, A5 s2. | `contract-regressions.log`; per-step records in `validation-summary.json` |

`validation-summary.json` records exit `0` and elapsed seconds for both smoke/firewall steps (web JAR smokes `10.462`, dependency firewall `3.121`) and all eight contract steps (Node pin `0.075`, locked A1 install `64.536`, A1 s3 `63.824`, A2 s3 `0.138`, A3 s3 `0.157`, A4 s2 `0.373`, A5 s1 `2.334`, A5 s2 `2.913`). `report-summary.json` records Surefire 36/309 and Failsafe 13/214, each with zero failures/errors/skips, and the existing production JAR's 39,257,237-byte size; it does not supply a JAR digest.

All paths in this immutable evidence ledger are external Controller artifacts, not repository deliverables:

| Exact Controller artifact path | Bytes | SHA-256 (Controller-recorded; author-rechecked) |
| --- | --- | --- |
| `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence\focused.log` | 12,235 | `7D4C181551098D7B4B69A9A2A9A2B616C49DBD6C2CE1BAF71B0A7B48AE52FC03` |
| `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence\full-verify.log` | 458,026 | `EAB8020CE769C1FC718EFD23617DD2C2C64ACD2A341563CB4B3B01CF64EC502F` |
| `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence\smokes-firewall.log` | 1,395 | `38C8AF65CC618CCD70598B58394A8F52509D87D60738121A850B18115C49F0D5` |
| `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence\contract-regressions.log` | 26,688 | `7F5545EEEF80820B77D48801FCE9C0F8ED79FA1F61230E44F35D2EE730B6902F` |
| `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence\validation-summary.json` | 957 | `57E754693B2772B3FF35A80E55E11D8FEDCD621ABE18398461E23A46FCAFBB29` |
| `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence\inventory.log` | 348 | `76147467BD1F4988BF998069B48DC173E54A43229CDC5F738B8D39437850DF6C` |
| `D:\codex-temp\pa24-code-review-20261110.yhNPsc\a\evidence\report-summary.json` | 722 | `733E8EAEF0F432CFF27C4A6A0BDC2126B5A1FC7533803B987AA48B6F864BBD30` |

### Historical author sandbox — isolation and measurement method (superseded)

The remainder of this section retains the original sandbox observations for auditability. Every FAIL/BLOCKED/NOT RUN below is **historical and superseded by the separate Controller validation above**, not a current deterministic acceptance failure or a product-failure attribution. In particular, Docker socket denial, JVM attach/socket restrictions, wrapper CRLF extraction, DNS/npm failures and downstream contract blocks/determinism failures describe that unsuitable runner. Passing Controller runs did not erase those observations or require product changes.

Validation was attempted against a byte-for-byte file snapshot under `/tmp/pa24-s1-validation/repo`, not the Windows-mounted task worktree. The command environment was an explicit allowlist (`PATH`, pinned `JAVA_HOME`, isolated `HOME`, `LANG`, `LC_ALL`, `TMPDIR`); no Tencent/cloud credentials, selected production keysource configuration or inherited application/cloud properties were passed. No network/cloud calls were authorized. This environment had network unavailable; Maven artifacts were already cached. The snapshot itself excluded pre-existing `.codex` and task prompt metadata. Build/test outputs were confined to ignored snapshot `backend/target`; no repository helper script or temporary repo file was created.

| Tool / prerequisite | Observed version/result |
| --- | --- |
| Java | Temurin OpenJDK `21.0.13+9-LTS`, exit 0 (`java -version`). |
| Maven installed | Apache Maven `3.9.9`, Java 21.0.13, exit 0 (`mvn -B -ntp -v`). |
| Maven wrapper | Required `./mvnw -B -ntp -v` exit 126: `/bin/sh^M: bad interpreter`; wrapper is CRLF in the Windows checkout. It was not normalized or edited. |
| Docker client | Docker `29.1.3`, exit 0 version output. |
| Docker daemon | Docker `version`, `info`, and image inspect exit 1: permission denied at `/var/run/docker.sock` (`/var/run/dify.sock`). PostgreSQL image `postgres:16.15` could not be inspected; Docker/PostgreSQL prerequisite unavailable. `psql --version` exit 127 (`psql` not installed). |
| Node / npm | Node `v22.23.4`, npm `10.9.9`; commands exit 0. |
| Git | Git `2.34.1`. Native worktree Git pointer uses a Windows path; read-only status used explicit existing Git metadata path. No metadata repair. |
| Network | Disabled/unavailable in isolated runner. npm registry lookup failed `EAI_AGAIN`; no retry requiring external access was attempted. |

### Historical sandbox commands, durations and outcomes (superseded)

Times are UTC start times on 2026-11-10; elapsed durations are wall milliseconds. “Installed Maven” reruns are diagnostic substitution only, not a pass for required wrapper commands. No result is green unless exit and assertions actually pass.

| Command (working directory `backend`, unless noted) | Start UTC / duration ms / exit | Result |
| --- | --- | --- |
| `java -version` | `07:08:00.899771Z` / 153.046 / 0 | Version above. |
| `./mvnw -B -ntp -v` | `07:08:01.051950Z` / 1.160 / 126 | FAIL, CRLF shebang. |
| `mvn -B -ntp -v` | `07:08:01.053194Z` / 212.053 / 0 | Installed Maven version above. |
| `docker version` | `07:08:01.265371Z` / 38.649 / 1 | Client shown; daemon socket permission denied. |
| `docker info` | `07:08:01.304138Z` / 1,821.988 / 1 | Daemon inaccessible. |
| `docker image inspect postgres:16.15 --format "{{.Id}}"` | `07:08:03.126304Z` / 14.449 / 1 | Daemon inaccessible; pinned image not verified. |
| `psql --version` | `07:08:03.140925Z` / 1.291 / 127 | Client absent. |
| `node --version` / `npm --version` | `07:08:03.142309Z` / 16.299 / 0; `07:08:03.158755Z` / 150.670 / 0 | Versions above. |
| Required nine-class `./mvnw -B -ntp -Dtest=TencentSsmMediaPlaybackSigningKeyProviderTests,TencentSsmSecretStringClientTests,MediaPlaybackTencentKeySourceIntegrationTests,MediaPlaybackKeyBundleParserTests,FileMediaPlaybackSigningKeyProviderTests,MediaPlaybackFileKeySourceIntegrationTests,MediaPlaybackConfigurationTests,MediaPlaybackCredentialAuthorityTests,SharingQueryBoundaryTests test` | `07:08:03.309598Z` / 1.698 / 126 | NOT RUN; CRLF wrapper fails before Maven. |
| Required `./mvnw -B -ntp -Pcontainer-smoke clean verify` | `07:08:03.311402Z` / 1.384 / 126 | NOT RUN; CRLF wrapper fails before Maven. |
| `mvn -B -ntp -Dtest=...nine classes... test` (diagnostic installed-Maven invocation) | `07:10:07.792808Z` / 41,414.491 / 1 | FAIL: 117 tests discovered; 4 failures, 44 errors, 0 skipped. Sandbox denies socket operations (`Operation not permitted`) and JVM self-attach for Byte Buddy inline Mockito (`Could not self-attach to current VM`). Test execution is not a pass. |
| `mvn -B -ntp -Pcontainer-smoke clean verify` (diagnostic installed-Maven invocation) | `07:10:50.983808Z` / 45,399.846 / 1 | FAIL: Surefire 36 classes/309 tests, 4 failures, 145 errors, 0 skipped; no Failsafe XML/containers. Test reports attribute failures to denied socket operations and Byte Buddy attach restriction. Required clean source=CI=XML acceptance not met. |
| `mvn -B -ntp dependency:tree -Dverbose -DoutputFile=target/pa24-dependency-tree.txt` | `07:11:38.804442Z` / 2,153.192 / 0 | Diagnostic dependency tree generated in isolated ignored target only. |

| Checked-in validation step | Exact invocation source | Duration / exit / disposition |
| --- | --- | --- |
| Checked-in inventory block | `.github/workflows/ci.yml` container-inventory heredoc, invoked verbatim from `backend` | 27.044 ms / 1 / FAIL: `missing report: target/failsafe-reports/TEST-*PostgresContainerIT.xml`; no Failsafe tests executed. |
| Existing default and opt-in web-jar smokes | CI named run block, invoked verbatim | 115.31 ms / 1 / BLOCKED: missing generated `target/boot.log` after failed build; no smoke ran. |
| Complete production JAR dependency firewall | CI named run block, invoked verbatim | 1.883 ms / 1 / BLOCKED: no production jar/index (`target/jar-index.txt` absent); runtime manifest/JAR/JAR-index checks not executed. |
| Firewall static rejection block | Checked-in firewall Python block through the runtime-manifest read boundary, executed in memory | `07:20:20.083672Z` / 21.924 ms / 0; observed 37 baseline resolved coordinates, 34 historical packaged-JAR baseline entries, 72 approved coordinates, 8 dependency-only, 64 counterparts, 65 packaged literals, 7 collisions, 116 static rejection probes. Static-only; not the complete firewall or current runtime evidence. |
| Node exact-version assertion | CI contract-regressions named step | 5.084 ms / 0 / PASS, Node `v22.23.4`. |
| `npm ci` locked A1 toolchain | CI contract-regressions named step | 70,850.387 ms / 1 / FAIL: network DNS `EAI_AGAIN`, npm reports “Exit handler never called”; dependency install incomplete. |
| A1 s3 | CI contract-regressions named step | 215.569 ms / 127 / BLOCKED: `redocly: not found` after incomplete npm install. |
| A2 s3 | CI contract-regressions named step | 27.293 ms / 1 / BLOCKED: generated A1 bundle missing. |
| A3 s3 | CI contract-regressions named step | 35.873 ms / 1 / BLOCKED: generated A1 bundle missing. |
| A4 s2 | CI contract-regressions named step | 334.795 ms / 0 / PASS: 113/113 fixtures, 471 nonfinite probes, 3 repeats, 1 determinism check. |
| A5 s1 | CI contract-regressions named step | 83.814 ms / 1 / FAIL: `A5_DETERMINISM_FAILURE`, `determinism.probe two-probe`. |
| A5 s2 | CI contract-regressions named step | 83.713 ms / 1 / FAIL: `A5_DETERMINISM_FAILURE`, `determinism.probe two-probe`. |

### Historical sandbox counts and firewall limitations (superseded)

The plan’s static expected baseline remains 36/309 Surefire, 13/214 Failsafe, total 49/523. The historical sandbox attempt produced Surefire XML for 36 classes/309 methods, but 4 failures and 145 errors, and **zero Failsafe XML** (13 classes/214 methods not run). That attempt's required clean assertions of 36/309 and 13/214 with zero failures/errors/skips and exact source/CI/XML equality were **FAIL/BLOCKED**, not pass. Its focused XML identifies the requested nine classes and 117 methods but has 4 failures/44 errors; that sandbox did not meet 117/117 clean. The separate Controller run now supplies the clean 117 and 49/523 results; these historical XML counts are not combined with or substituted for its fresh XML.

Expected firewall inventory remains 72 coordinates / 65 packaged JARs / 8 dependency-only / 64 counterparts / one sole packager / 7 collisions / 116 probes. The historical sandbox's static rejection segment observed 72/65/8/64/7/116 and the accepted 37/34 baseline literals. Sole-packager and collision runtime assertions, actual runtime manifest, packaged JAR, jar index and complete firewall were not verified in that attempt: the checked-in full step stopped before those checks. Static rejection probes do not replace complete firewall acceptance. The current complete firewall PASS is supported only by the separate Controller run and `smokes-firewall.log`, not by that static segment.

The original runner was unsuitable for acceptance: wrapper CRLF extraction, Docker socket denial, absent Postgres client/image inspection, network/DNS outage, JVM attach/socket restrictions and observed contract determinism failures. The Controller subsequently supplied a suitable clean Linux runner and passing PA24 deterministic evidence above; the original request to Backend QA/build for a suitable runner/rerun is superseded, not an outstanding deterministic blocker. Backend QA/build ownership and the prohibition on modifying source, CI, wrapper, tests or assertions to create green results are unchanged. Historical PA23 results are not PA24 evidence.

### Current acceptance boundary

Required deterministic repository validation is PASS on the Controller runner. All Tencent/CVM/CAM/Secret Manager and live A/B/C evidence, including real identity/version/continuity, quota/latency/DNS/metadata/retry and cohort behavior, remains **`NOT EXECUTED`** under §7. No production-readiness, live-cloud, hosted-CI, approval, implementation-gate acceptance or merge claim follows from this repository evidence.

## 9. G-to-decision, test and runbook traceability

**CR-01 fix validation/traceability:** the accepted plan §4 retirement-undo requirement is closed in this runbook by the §5 step-7 whole-cohort C→B contract (approved quiesce/pause/drain, removal of every C instance, complete fixed-B restart/reconfiguration, protected-manifest verification, applicable time-valid B overlap and fresh K2 rechecks, changed-acceptance-state documentation and explicit former C retirement invalidation) together with the §7 conditional recovery-register entry (Security/Sharing ownership, separate approval, no in-repo deterministic oracle, not a fourth automatic stage). This is documentation-only finding closure for independent re-review, not an executed recovery, live acceptance or gate-acceptance claim; all live rows remain `NOT EXECUTED`.

| Goal trace | Decision / S1 evidence | Deterministic evidence | External acceptance still required |
| --- | --- | --- | --- |
| G1 CVM attached role only | §2.1 direct CvmRoleCredential route; §3 CV-3 register; §5 per-instance checks; §7 CV-3 row. | `TencentSsmSecretStringClientTests.roleOnlyFactoryIsLazyAndHasNoStaticFallback`; `SharingQueryBoundaryTests.tencentKeySourceConfinesSdkAndPreservesExistingAuthority`. | Actual CVM identity/trust, CAM least privilege/isolation; not executed. |
| G2 immutable explicit A/B/C | §§2.1–2.2; §§4–5 timestamped immutable-version checks and whole-cohort sequence; §7 CV-2 and source attestation. | Named A/B/C integration test oracles in §5. | Real immutable IDs, SecretString, echo and material continuity; not executed. |
| G3 fail closed/no fallback | §§2.1–2.2; §5 failure/unknown-kid discrimination; redaction in §6. | `transportAndParserFailuresHaveNoFallback`, source-unavailable ordering, provider/client suites, boundary tests. | Actual CAM/source/failure behavior; not executed. |
| G4 authority/state/redaction unchanged | §§4–6 exact results, replay invariants, unchanged state checks and redaction. | Authority 24-method suite, Tencent integration and canary tests listed §5. | Protected live harness/custody; not executed. |
| G5 honest liveness | §6 exact phase/no-total/DNS/metadata/retry limits and measurement schema. | Client/provider configuration tests. | External latency/quota/retry/censoring measurements and suitability decision; not executed. |
| G6 operator rollout/retirement | §3 approvals; §4 evidence validity; §5 per-purpose matrix, quiesce, rollback and logical/cloud retirement distinction. | Existing A/B/C integration test oracles; no new tests. | Authorized whole-cohort operations, approvals and audit; not executed. |
| G7 reproducibility/evidence | §§7–8 Controller provenance, exact artifact ledger and retained superseded runner limitations; §10 changed-path/redaction integrity. | Controller PASS: focused 117; source=CI=XML 36/309 + 13/214 = 49/523, zero failures/errors/skips; web JAR smokes; complete 72/65/8/64/sole-packager/7/116 firewall; eight contract steps exit 0. | Live rows remain NOT EXECUTED; no hosted-CI, production-readiness or gate-acceptance claim. |

## 10. Changed-path inventory, provenance and unresolved owners

- Sole evidence-fix task write: `docs/gateflow/pa24-sharing-media-secret-manager-live-rotation-and-platform-acceptance-s1-implementation.md` (this existing document); no new file. Original S1 entry: 43,214 bytes / SHA-256 `23F4B1C5A96E9EDD6BCA6C42C589B49B985A9C4046930B3A7D2E9761255C6DCF`. This durable-evidence fix began from the earlier evidence fix's 50,860 bytes / SHA-256 `581CE163208E2B3A585E3BD9965394A13393130E896B015212B3A0514143FFEE`. The original ignored generated validation files were confined to the isolated `/tmp` snapshot; after WSL cleared the earlier Controller `/tmp` artifacts, validation was rerun on the clean `D:` snapshot. The directly rechecked Controller artifacts in §8 are retained on `D:` and remain external, not repository deliverables.
- No tracked/source/test/POM/CI/migration/Control/canonical-document/plan/review/script/configuration change. V001–V008 are protected and unchanged. Pre-existing `.codex` and task prompt metadata are excluded, not task-created or deliverables.
- Original read-only entry hashes were recorded outside the repository before validation. This evidence fix keeps a read-only in-memory baseline of non-S1 repository files and existing Git metadata for final comparison; no baseline file or metadata repair is created. Native Git's Windows pointer is inspected through explicit existing Git paths with optional locks disabled. The pre-existing Control modification and untracked `.codex`/prompt metadata are not task writes. Final verification is reported in the author handoff. Raw secret-bearing material was not acquired or used. No cloud credentials, production keysource selection, cloud call, provisioning or Tencent contact occurred.
- Unresolved acceptance owners remain Platform/Security (CV-2/CV-3, immutable resource/version and role/CAM); Platform deployment + Security + Operations (config reconciliation, quiesce/drain/recovery); Sharing/Security + Operations (harness/custody, validity/clock/skew/horizon, optional C decision); Platform capacity/Security + Operations (quota/latency/liveness/thresholds); Security + Build/Observability (redaction/String lifecycle). Their live rows remain `NOT EXECUTED` in §7. Backend QA/build remains the deterministic evidence owner; its runner, clean 49/523 XML, Docker/PostgreSQL, firewall and contract obligations have passing Controller evidence in §8, rather than an outstanding sandbox-rerun blocker. No owner or live-row disposition is changed.

## 11. Author handoff and binding stop

Completion status: **IMPLEMENTATION AUTHOR-COMPLETE; STOP BEFORE CODE REVIEW.** This is author completion of the documentation/evidence slice only—not implementation gate acceptance, production readiness, live-cloud acceptance, hosted-CI success, approval or merge. Required deterministic validation passed on the separate Controller runner: focused 117; full 49/523 with exact source=CI=XML and zero failures/errors/skips; production JAR smokes and complete firewall; all eight contract steps exit 0. The exact seven-artifact durable Controller ledger, including inventory and report summaries, and direct author byte-count/SHA-256 recheck after WSL `/tmp` loss are in §8; the evidence is retained on `D:`. Earlier sandbox failures remain disclosed as superseded runner limitations, not product failures or outstanding deterministic blockers. No code change is justified or made. Final author integrity checks cover strict UTF-8/LF, one final LF, no trailing whitespace, this sole S1 task write, unchanged non-S1 bytes/Git metadata and protected V001–V008; final S1 bytes/SHA-256 are reported outside this document to avoid a self-referential digest. No commit, stage, push, branch, PR action, approval, ready/reviewer action, external comment, Tencent contact, real credential, resource provisioning or live acceptance occurred. Keep every external/live row `NOT EXECUTED` until an explicitly authorized future cloud gate supplies and accepts its evidence.
