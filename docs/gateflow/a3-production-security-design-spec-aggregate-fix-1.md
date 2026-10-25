# A3 aggregate deepreview fix 1 — dead aggregate evidence, canonical-string pin, order discriminators and provider order precision

## 1. Task, provider and base

- Work unit: `A3-production-security-design-spec`
- Gate: aggregate deepreview `fix` only (author correction of controller-accepted aggregate findings; no review, checkpoint, merge or later gate)
- Task label: `a3-security-aggregate-fix1-ds-20261026-01`
- Provider: DeepSeek (`ds`), Claude Code runner
- Repository: `/mnt/d/codex-call`
- Accepted-slice head: `c3f66e299760e0413dbd89d17e43d5db19f74995`
- Accepted review: `docs/reviews/code-review-20261026-014918.md` (SHA-256 `40B2957E4B7FAFE6E1EA666AD4F649324A608FF64EBFCF0CEF48054577290E8A`)
- Controller truth (read-only): `docs/gateflow/project-execution-control.md`, section `A3 aggregate deepreview adjudication and bounded-fix entry`
- Status: **author-complete within the allowed writes. Executable Node/npm validation is permission-blocked in this session and remains controller-owned. This artifact is not a review and does not claim aggregate acceptance.**

## 2. Accepted findings and disposition

| Finding | Severity | Controller decision | Author disposition |
| --- | --- | --- | --- |
| `F-AGG-01` — construction-guaranteed aggregate zero-side-effect state fields and literal summary values presented as discriminating evidence | Medium | accepted | **fixed (author)**: dead state/summary/step keys removed; retained evidence is live/derived; no new counter added |
| `F-AGG-02` — IVR canonical string signs and verifies through one helper and no fixture pins the six-line byte contract | Low | accepted | **fixed (author)**: one fixed canonical-string literal pinned on an existing successful IVR request |
| `F-AGG-03` — `sec-s2-04` cannot discriminate timestamp-window vs signature-verify order | Low | accepted | **fixed (author)**: durable out-of-window + invalid-signature cross-product step expecting `TIMESTAMP_OUT_OF_WINDOW` |
| `F-AGG-04` — provider `verificationOrder` combines duplicate-before-nonce and conflict-after-nonce into one ambiguous item | Low | accepted | **fixed (author)**: split in model, validator frozen projection and canonical spec; machine/fixtures/public codes unchanged |
| `F-AGG-C1-01` — stale canonical status says S3 awaits independent review | Low | accepted (aggregate open question 4) | **fixed (author)**: stable slice-acceptance wording; live Gateflow state delegated to the controller document |

## 3. Exact code changes

### 3.1 `F-AGG-01` — remove construction-guaranteed aggregate evidence

`security/validate.mjs` state object (`:1068`) no longer initializes any of:

`a2Calls`, `a2Reservations`, `a2Settlements`, `a2Redials`, `a2ReResolves`, `outboxIdentityChanges`, `outboxPayloadChanges`, `recursiveAppendFactCalls`, `redactionRawExposures`, `supportPlaybackAllowed`, `state.evidenceSolidified`.

`machineSummary()` (`:2653`) no longer exposes those keys or the literal summary values `lastAdminPreserved: true` and `newEmployeeRolesCreated: 0`.

Construction-guaranteed step-result fields were removed from the evaluator and corresponding fixture expectations:

- `ivr-outbox` (`:1810`): `hotRetryPerformed`, `identityChanges`, `payloadChanges`, `a2BusinessSideEffects`.
- `upload-credential` (`:1980`): `evidenceSolidified` on all phases.
- `audit-access` (`:2349`): `recursiveAppendFactCalls`.
- `high-risk` (`:2398`): `lastAdminPreserved`, `newEmployeeRolesCreated`.
- `dependency` (`:2571`): `evidenceSolidified`, `a2BusinessSideEffects`, `noDuplicateA2SideEffects`.

No counter was invented. Every retained aggregate key is a state value mutated by an executed path and read independently by `machineSummary`:

- `a2Events` — written at `:1703`; `nonceSeen` — written at `:1692`; `outboxEntries` — written at `:1849`; `securityBlockedEntries` — from `entry.securityBlocked = true` at `:1893`.
- `payments` / `entitlements` — incremented at `:1799-1800`; `providerEvents` — written at `:1798`.
- `uploadIssued` — `:2047`; `uploadDenied` — deny branches; `playbackIssued` — `:2220`; `playbackDenied` — deny branches; `cosDenied` — `:2337`.
- `auditReads` / `auditReadDenied` / `auditAccessEventsEmitted` / `auditFactsAppended` — incremented in the audit paths (`:2370-2387`, `:2418-2441`, etc.).
- `highRiskCommitted` / `highRiskRolledBack` / `highRiskDenied` — incremented at `:2440` and the deny/rollback branches.
- `redactionEmits` — `:2506`; `redactionBlocks` — block branch.

Honest replacement mapping:

- Replay/no-redial/second-reservation claims in `sec-s2-05` now use live `a2Events` plus live `nonceSeen` (`2`: two accepted nonces; the rejected replay and the unavailable store did not consume) and the already-derived step fields `a2FirstResultReused` / `nonceConsumed`.
- Outbox identity/payload preservation in `sec-s2-08` / `sec-s2-09` now uses live `outboxEntries` / `securityBlockedEntries` plus step-derived `sameEventId`, `samePayload`, `securityBlocked`, `retried`, `freshTransport`, `alertSignal`.
- Audit no-recursive-fact claims in `sec-s3-04` / `sec-s3-08` now use live `auditAccessEventsEmitted`, `auditFactsAppended`, `auditReads`, `auditReadDenied`, and step-derived `accessEvent` / `persistenceRoute` / `projection`.
- Upload/playback/COS claims now use live `uploadIssued`, `uploadDenied`, `playbackIssued`, `playbackDenied`, `cosDenied`, plus step-derived `mediaFactCreated`, `serverObserved`, binding flags.
- Redaction/support claims now use live `redactionEmits` / `redactionBlocks` and step-derived `rawExposed`, `playbackAllowed`, `playbackDenied`.
- High-risk claims now use live `highRiskCommitted` / `highRiskRolledBack` / `highRiskDenied` / `auditFactsAppended` and step-derived `mutationCommitted`, `domainMutationRolledBack`.

Invariant text (`INV-A3-01` … `INV-A3-21`), registry counts, negative counts and stage boundaries were not changed. The only doc table clarification is `sec-s3-05`: "registry 中无 last-admin 移除或角色创建操作" replaces the removed literal-assertion phrasing; INV-A3-19 remains the authoritative "only ADMIN/OPS" invariant.

### 3.2 `F-AGG-02` — independently pinned canonical string

`security/v1/fixtures/sec-s2-05-ivr-replay-rejected.json`, step `first-accepted-request`, now fixes:

- input method `post` (lowercase; exercises the frozen uppercase normalization),
- raw path `/api/v1/ivr/calls/TEST_ONLY_CALL%5F0001/events` (percent-encoded; exercises "not decoded"),
- body `TEST_ONLY_IVR_EVENT_BODY_0001`,
- timestamp `1700000000`, nonce `TEST_ONLY_NONCE_0501`,

and asserts the exact six-line result:

```text
ONECALL-IVR-V1
POST
/api/v1/ivr/calls/TEST_ONLY_CALL%5F0001/events
1700000000
TEST_ONLY_NONCE_0501
8408196e4719317b7762eedf0f36e164d0f7d3077a345d0e923f456222341453
```

The body hash was computed independently with `sha256sum` over the exact 29 UTF-8 bytes `TEST_ONLY_IVR_EVENT_BODY_0001` (file size observed as 29, no trailing newline); it was not produced by `signatureFor`. The literal is stored in the fixture and is compared only after execution (`validate.mjs:3668`); `machine.run` never reads `step.expected`.

A symmetric composition mutation now changes `result.canonicalString` (`validate.mjs:1718`) and fails the fixed literal: CRLF separator, reordered lines, lowercase output method, URL-decoded path, altered body hash or a trailing newline all diverge. The other successful requests in the same fixture use the same lowercase method and encoded path for coherence.

### 3.3 `F-AGG-03` — timestamp-before-signature cross product

`security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json` adds step `out-of-window-and-invalid-signature`:

- valid mTLS, lowercase/uppercase-insensitive method `POST`, path `/api/v1/ivr/inbound/resolve`, nonce `TEST_ONLY_NONCE_0409`;
- timestamp `1600000000`, `receivedAt` `1700000000`, window `300` (out of window);
- `signatureMode: "invalid"` (the evaluator builds a valid signature and flips its last character);
- expected `401 UNAUTHENTICATED` / `TIMESTAMP_OUT_OF_WINDOW`, `timestampFormatValid: true`, `a2Processed: false`.

Machine order is timestamp window at `validate.mjs:1666` before signature verification at `:1683`. Swapping those two blocks would produce `SIGNATURE_INVALID` for this input and fail the expected reason. Fixture inventory, ID and negative classification are unchanged.

### 3.4 `F-AGG-04` — split provider order

`security/v1/security-design-v1.json` and the validator's frozen `EXPECTED_SIGNATURE_PROFILES` (`security/validate.mjs:291`) now both contain:

1. provider mTLS identity
2. headers
3. provider profile Q6
4. timestamp window
5. constant-time signature verify
6. exact-duplicate event and payload fingerprint check (200 early exit; no nonce consumption)
7. nonce SET NX for a new event or changed payload
8. changed-payload conflict 409 `PAYMENT_EVENT_CONFLICT` after the nonce check
9. payment processing

`docs/17-production-security-design-spec.md:367` states the same order. Machine behavior is unchanged: exact duplicate returns 200 before the nonce store (`validate.mjs:1775-1782`); nonce replay is checked before changed-payload conflict (`:1787-1796`); the 409 is reachable only after nonce consumption (`:1791`). `sec-s2-06` still pins exact-duplicate 200 with a reused nonce, new-event 200 with nonce consumption, changed-payload 409 after a fresh nonce, and replayed-nonce + changed payload `401 NONCE_REPLAY`. No public code, counter or fixture negative changed.

### 3.5 `F-AGG-C1-01` — stable canonical status

`docs/17-production-security-design-spec.md`:

- line 15 now reads: `S1/S2/S3 slice checkpoint 均已 accepted；live aggregate/PR Gateflow 状态仅以 docs/gateflow/project-execution-control.md 为权威。`
- the completion paragraph (line 713) replaces the stale "S3 author completion 后停止；不进入 code review/checkpoint" lifecycle sentence with the same stable delegation to the controller document.

No live Gateflow/PR state is asserted in the canonical document.

## 4. Changed files and SHA-256

All paths below are existing files except the one required artifact. No other file was modified by this task.

| File | SHA-256 |
| --- | --- |
| `docs/17-production-security-design-spec.md` | `449A994102D8DED7FE1DF01D7DE85073CE0BD11023F2DC9595262D23BD227F20` |
| `security/v1/security-design-v1.json` | `A5B2D254E009959DBF990216DAF36FF58CE8645CC87236A8AC419F8A1D3BE8A8` |
| `security/validate.mjs` | `54D29155BD1312219D12AFE8457DBF12A50624E6CB420972297A7DB5B79C4EEC` |
| `security/v1/fixtures/sec-s2-01-five-domains-accepted.json` | `88D30725C8C0FAF9D665E1ED938F2390A419354BD6B644E943EFF870191DAD71` |
| `security/v1/fixtures/sec-s2-02-cross-domain-rejected.json` | `AE5B1B55E8B46754A82253C15514B46D65AEA1EA715BF2B9D743D9B34D937BF3` |
| `security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json` | `A67813A1338ACFAAEBAE630F454D878280A5DB2F7B13F7BDAB039A46E8096E58` |
| `security/v1/fixtures/sec-s2-05-ivr-replay-rejected.json` | `CEF771C1730085BBC1DF2E92184F73A9CDE2DB3EC2A1FE2F4D395F46A5C3CAC2` |
| `security/v1/fixtures/sec-s2-06-provider-signature-replay.json` | `22B1A2E9A123097124D3E397619DAAA0689DD95DDCF8124E4434B9EC54D3AF67` |
| `security/v1/fixtures/sec-s2-07-signing-key-rotation.json` | `2233303A7DAB14CECCD8517598F5E363BAAE9AD85798EFEAD5C0C0FE205B4E23` |
| `security/v1/fixtures/sec-s2-08-failclosed-dependency-preserves-a2.json` | `DB54DB5F0B1118A43F64AB8FD138153461F8CFDA4EB847C4D37AFBF0191763F5` |
| `security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` | `5BB36BEDCC74F8A3923CCD8CD4A55C4390FDCD4119A88867C7D5C9648B324FC0` |
| `security/v1/fixtures/sec-s3-01-upload-credential-scope.json` | `2838B5A74BBD2FF1D8E075680437A6D0DF5B241DE381571BEBE26B0953F0BB97` |
| `security/v1/fixtures/sec-s3-02-playback-credential-scope.json` | `C120E4A7A90FBEFD516D4877FD58BA147276D5F53C95A860F6F4F4B8A7318E3B` |
| `security/v1/fixtures/sec-s3-03-cos-private-policy.json` | `59386369ED214500C43850E9BC435DB9307B6960178162DF0B3C710206B201D7` |
| `security/v1/fixtures/sec-s3-04-audit-access-denied.json` | `BB0BD7C0B33A7CA3D34B5F69D728BDF3814026BAC4990B3DA73847EB99F1A3C6` |
| `security/v1/fixtures/sec-s3-05-high-risk-controls.json` | `FDE2B353EF3B9561C23272EA82AAF1FA54D4DA90039DBD79D06E5B89C1B5DF1E` |
| `security/v1/fixtures/sec-s3-06-redaction-deterministic.json` | `A5F1042878D539987E9311836F21882A6B778451079F7546ADE718A29840871C` |
| `security/v1/fixtures/sec-s3-07-credential-url-leak.json` | `E40502228251D452A713CC80AC90279C7E273A08758692808D3CD0097AA79E71` |
| `security/v1/fixtures/sec-s3-08-end-to-end-security.json` | `39F1345EAF49C9BD1F78F1D6E9C2D6B995D6A24EC5EB67AE42CE81641023A0AE` |
| `docs/gateflow/a3-production-security-design-spec-aggregate-fix-1.md` | this artifact; hash reported in the completion report |

Unchanged fixtures in the 23-file inventory: `sec-s1-01` … `sec-s1-06` (6), `sec-s2-03` (1); unchanged S2/S3 fixtures by design: none else.

## 5. Fixture, counter and stage evidence

- Fixture inventory: **23** JSON files, IDs unchanged. Slices `6 + 9 + 8`.
- Negative classifications: **10** (`sec-s1-03`, `sec-s1-05`; `sec-s2-02`, `-04`, `-05`, `-06`, `-09`; `sec-s3-02`, `-04`, `-07`). No negative flag changed.
- Model registries (unchanged sections; static counts): `keyClasses` 20, `dataClasses` 28, `deferredInputs` 23, `trustSurfaces` 5, `trustBoundaries` 7, `signatureProfiles` 2, `auditEvents` 39 (`2 access_event` + `37 append_fact`), `mediaPolicies` 2, `highRiskActions` 10, `auditCoverage` 10, invariants 21. The only model edit is the provider `verificationOrder` text; no array length changed.
- `security/validate.mjs` frozen expectations for those registries were not changed, so the S1 `20/28/23`, S2 `5/5/7/6/10/2`, S3 `2/39/39/2/37/10/10` outputs cannot drift from this diff. The validator's provider projection was updated in lockstep with the model.
- A2 upstream impact: `protocol/ivr/**` and `api/openapi/**` are untouched; the model's A2 digest `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` is present and unchanged. A2 stage-S3 expected `12/11/22/25/15`, 25 fixtures.
- Dead-key absence: grep over `security/**` and `docs/17-production-security-design-spec.md` for all 13 accepted keys plus `identityChanges`, `payloadChanges`, `a2BusinessSideEffects`, `hotRetryPerformed`, `noDuplicateA2SideEffects` returns zero matches.
- Expected-driven execution: `step.expected` appears only as a fixture-shape presence check (`validate.mjs:3624`) and a post-execution comparison (`:3668`); no occurrence inside `machine.run` or any action handler.

## 6. Commands and observed results (honest record)

| Check | Command | Observed |
| --- | --- | --- |
| Validator syntax | `node --check security/validate.mjs` | **permission-gated: denied** (`This command requires approval`) |
| A1 check | `cd api/openapi && npm run check:s3` | **permission-gated: denied** (`npm run check:s3` requires approval) |
| A2 stage-S3 | documented `node protocol/ivr/validate.mjs --stage s3 …` | **permission-gated: denied** |
| Security S1 | documented `node security/validate.mjs --stage s1 …` | **permission-gated: denied** |
| Security S2 | documented `node security/validate.mjs --stage s2 …` | **permission-gated: denied** |
| Security S3 | documented `node security/validate.mjs --stage s3 …` | **permission-gated: denied** |
| Second byte-identical runs | re-run S1/S2/S3 | not executable in this session; controller-owned |
| JSON parse | `jq empty …` / JSON loader | **permission-gated: denied**; `file` classifies the model and every edited fixture as `JSON data`; every edit was a whole-key deletion or a single literal insertion inspected by diff |
| UTF-8 | `file docs/17… security/v1/security-design-v1.json security/validate.mjs` | `Unicode text, UTF-8 text` / `JSON data` / `UTF-8 text executable` |
| Exact-path whitespace | `git diff --check -- docs/17-production-security-design-spec.md security/v1/security-design-v1.json security/validate.mjs security/v1/fixtures` | exit `0`, no output |
| Changed-file scope | `git status --porcelain -- …` | exactly the 3 core files, 16 causally edited fixtures and the one new artifact shown in §4 |
| Model counts | `Grep` over `security/v1/security-design-v1.json` | `invariantId` 21; `actionCode` 39; `profileId` 2; `policyId` 2; `operationId` 10; `callClass` 10; `credentialId` 10; digest 1 |
| Fixture inventory / negatives | `Grep` over `security/v1/fixtures` | 23 IDs, 10 `"negative": true` |

Permission denial is a runner constraint, not a product failure. It is also not authorization to broaden scope. The controller must independently execute the documented commands and the two-run byte-identical check on these exact hashes.

## 7. Mutation / discrimination evidence (static)

Because Node execution is denied, the following are code-path proofs tied to the exact fixture literals; the controller can execute each mutation against this tree.

1. **Symmetric canonical-composition mutation** — any change inside `canonicalIvrString` (`validate.mjs:1140`) changes the success result `canonicalString` (`:1718`). The fixture literal in `sec-s2-05` is fixed and compared after execution, so a CRLF join, non-uppercased method (input is lowercase `post`; expected `POST`), URL-decoded `%5F`, reordered lines, different body hash or appended trailing newline all fail `sec-s2-05`.
2. **Timestamp/signature order swap** — moving signature verification before the timestamp-window block makes `sec-s2-04` step `out-of-window-and-invalid-signature` return `SIGNATURE_INVALID` instead of the pinned `TIMESTAMP_OUT_OF_WINDOW`. The current check order is `:1666` (timestamp) then `:1683` (signature).
3. **Provider order drift** — any divergence between the model JSON and `EXPECTED_SIGNATURE_PROFILES` (`:291`) fails `validateS2Model`'s exact `deepEqual`; if both change, `sec-s2-06` exact-duplicate (reused nonce 0602 → 200) and `changed-payload-after-nonce-replay` (reused nonce → `401 NONCE_REPLAY`, not 409) fail against the machine order at `:1775-1796`.
4. **Removed dead keys** — all accepted dead keys are absent (grep evidence in §5). Retained aggregate evidence is behavior-derived with the write sites cited in §3.1; no construction-only counter was retained in `machineSummary` or in the edited fixture expectations.

## 8. Prior accepted findings — nonregression

| Finding | Evidence in current `security/validate.mjs` |
| --- | --- |
| `F-S1-C1-01` | `isAeadKeyClass` `:847`; encrypt `:1236`, decrypt `:1292`, A2 replay `:1461` |
| `F-S1-R1-01` | accepted base history untouched; no stale SHA introduced |
| `F-S2-R1-01` | `isValidDomainIdentity`/`isValidIvrTimestamp`/`isValidIvrNonce` `:1201-1215`; creation/recovery guards `:1813`, `:1828`, `:1877`, `:1894`, `:1908`, `:1922`, `:1950`, `:1953`; orphan step in `sec-s2-09` retained |
| `F-S2-R1-02` | complement replacement `:1629` |
| `F-S3-C1-01` | `/fails? closed/` `:3249` with exact media-policy comparison |
| `F-S3-R1-01` | `MASKED_DISPLAY_SAFE_CLASSES` `:937`; single classifier `:941-948`; used by redaction and support-view |
| `F-S3-R1-02` | positive PUT gate `request.method !== 'PUT'` `:2076` |

No prior fix site was edited by this task.

## 9. Docs decision

- Canonical spec edits are limited to: the stable accepted-status header/completion wording (`F-AGG-C1-01`), the provider order split (`F-AGG-04`), and fixture-table alignment for the two new S2 discriminators. No registry/marker table row was added or removed; no invariant text changed.
- The spec continues to disclaim runtime/cloud execution and does not assert live Gateflow/PR state.

## 10. Residual risks and uncovered areas

- Executable validation, including the two byte-identical S1/S2/S3 runs and mutation probes, is **not independently executed here**. The controller must run it and should reject this artifact if any stage/count/digest differs.
- The canonical-string fixture pins the six-line byte contract and the body hash; it does not independently pin the HMAC value or the key-label-to-secret derivation. Those remain covered by existing signature/replay fixtures and static review.
- Aggregate zero-side-effect claims are now enforced by live counters plus step status/reason/identity behavior. There is still no live counter for an IVR redial/reservation/settlement/re-resolve because the reference evaluator has no such side-effect path; this remains a model-level construction boundary, explicitly not a production proof.
- `sideEffects` zero objects, fail-closed dependency booleans such as `redial`/`mediaFactCreated`, and other per-step negative result fields outside the accepted dead-key list were left unchanged to avoid expanding scope; they are negative result shapes, not aggregate counters.
- All A3 residual rows `R-A3-*` (KMS/Secret Manager/COS capability, WORM/object-lock, region/SKU, nonce-store atomicity, mTLS, provider vendor details, session lifecycle, media-gateway topology, redaction runtime, audit volume, support break-glass) remain in force and are not reduced.

## 11. Scope confirmation and completion status

- Modified exactly: `docs/17-production-security-design-spec.md`, `security/v1/security-design-v1.json`, `security/validate.mjs`, and 16 existing `security/v1/fixtures/sec-s*.json` files causally required by the five accepted fixes.
- Created exactly one artifact: this file.
- No controller, review, plan/slice/fix artifact, A0/A1/A2 source, package file, prototype, screenshot, `shared/*`, or untracked `docs/reviews/plan-review-20261021-205118.md` was modified.
- No stage, commit, push, PR/issue mutation, merge, branch switch, reset, restore or later-gate action was performed.
- Completion status: **author-complete; executable validation permission-blocked; awaiting controller validation and a fresh aggregate re-review. Not accepted, not a checkpoint.**
