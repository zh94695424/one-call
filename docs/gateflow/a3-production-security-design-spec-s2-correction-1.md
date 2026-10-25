# A3 S2 correction 1 — deny initial 401/403 observation without a complete transport tuple

> Work unit：`A3-production-security-design-spec`
>
> Slice：S2 — authentication domains, service identities, signing, replay and IVR-outbox policy
>
> Task label：`a3-security-s2-correction1-ds-20261026-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Gate：`fix`（bounded original-author correction only；no self-review, no subsequent gate）
>
> Base：accepted S1 checkpoint `88079d208e90ebc176ab54a25734c38bbf7732aa` (`gateflow: accept A3-production-security-design-spec S1`)
>
> Accepted finding：`F-S2-R1-01` (High) — controller correction entry `A3 S2 fix 1 return and controller correction entry`, 2026-10-26
>
> Prior fix artifact：`docs/gateflow/a3-production-security-design-spec-s2-fix-1.md`, SHA-256 `8AD9579B54181A51A617138E0F5789693B76E304E247E0E0F033E44FDA4E5E48`
>
> Stop condition：bounded correction author-complete; controller revalidation and a fresh MiMo re-review are required before any acceptance.

## 1. Scope and allowed writes

Only `security/validate.mjs` and `security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` were modified. This artifact is the only new file. The canonical document, machine model, S2-04, implementation/fix reports, controller, prior reviews, accepted A0/A1/A2/S1 artifacts, OpenAPI/protocol sources, prototypes, screenshots and shared business logic were not modified. Fixture IDs, fixture counts, the seven-negative set, slice assignment and the final exact assertion semantics were not changed; no Problem code, reason, state, event or numeric policy value was invented.

## 2. Exact changed files and SHA-256

| Path | Lines | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| `security/validate.mjs` | 2778 | 139298 | `3E6850D03E8C0EF6ADE51F214FD3A682190EE0B174E15D5BA04E19DE840A20FE` |
| `security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` | 320 | 15182 | `63E0CFB56FE052E1372B0102CD914AAFAB3046CEFF8E0B33EB6B42CF329A9CFB` |
| `docs/gateflow/a3-production-security-design-spec-s2-correction-1.md` | this artifact | this artifact | reported in the final response |

Unchanged and independently re-hashed:

| Path | SHA-256 | Comparison |
| --- | --- | --- |
| `security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json` | `9C96D7B7B1560C6BD1BE426E33A86C3F2AB3E1759ED435709D3F451385AF9249` | equals fix-1 report §2 |
| `docs/17-production-security-design-spec.md` | `4D0BF8CA6DB20F0E6C95E4D68C7B59CF3C8D63AB5193D56F2C84A9B7FB0A0DE7` | equals S2 implementation report §2 |
| `security/v1/security-design-v1.json` | `A9B196317247E3B7DB18E7E947AF6FAA586EDA6981881B586CD66CA5898EE438` | equals S2 implementation report §2 |

The six accepted S1 fixtures remain byte-identical to the accepted checkpoint:

```text
844b67a4f10a8dcbcd690df6093e9de33dc8bc964ac210a4850e89d1f806f576  sec-s1-01-envelope-purpose-roundtrip.json
2acec51fd57e468af58546f35a9dd1021de2cbedad75c5610f3963e45155371d  sec-s1-02-hmac-index-equality.json
5cbfa710dc2a88b6a44f87590b3ba580370e51b5e528db4f349f1f88282142c5  sec-s1-03-cross-purpose-denied.json
6a9fbd95d4971a2b551bcda28ca209fc173a9e3a9ca1d4c00841b7acb3737206  sec-s1-04-rotation-historical-replay.json
caa71dffc8c2d015130bf01f0ce95ae69dae7ba963a819275dc157a5eb107c7d  sec-s1-05-key-unavailable-failclosed.json
00d85e4401e6e31f858804ab600a6e3d1e694a4e1c2f938f00588da6c4b2c0e2  sec-s1-06-append-only-rotation-safety.json
```

## 3. Corrected edge — initial observation must not create an entry without transport

Controller evidence: after fix 1, deleting both `transportNonce` and `transportTimestamp` from the first `observe-401` step still produced `passed=15 failed=0`, because the creation path ran before transport validation and the observation path skipped all transport checks when both fields were absent (`security/validate.mjs` fix-1 lines 1493-1509). That created a security-blocked entry with `previousNonce = null` and `previousTimestamp = null`.

`case 'ivr-outbox'` now applies two guards:

1. **Before creating a new entry for a first 401/403 observation** (`security/validate.mjs:1422-1439`): after the existing non-empty structured-identity requirement, the step must carry a complete valid tuple — `transportTimestamp` matching `^[0-9]{1,12}$` and `transportNonce` matching `[A-Za-z0-9_-]{1,128}`. If either field is missing, partial, `null`, empty or malformed, the step returns the existing `403 FORBIDDEN / STALE_TRANSPORT` result and returns **before** `state.outbox.set(...)`. No orphan entry is created and no new Problem code is added.
2. **Existing-entry observations** (`security/validate.mjs:1510-1537`): a supplied tuple keeps the existing complete-format validation and baseline update. If the tuple is omitted entirely, it is accepted only when the entry already holds a complete valid previous baseline (`previousNonce`/`previousTimestamp` both valid); otherwise the step returns `403 FORBIDDEN / STALE_TRANSPORT` (fail closed). This is what allows `reblock-403` to omit transport after `recovery-403-accepted` recorded `0908/1700000600` at send time, without letting a null-baseline entry pass.

Identity handling, payload immutability, policy-attempt ordering, recovery freshness, baseline advancement on accepted recovery, `PENDING`, redacted alerts and zero A2 effects are unchanged from fix 1.

## 4. Durable regression step added to `sec-s2-09`

One step, `initial-observation-transport-omitted-denied` (`security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json:277-295`), uses a **new** event identity and payload (`TEST_ONLY_OUTBOX_EVENT_ORPHAN` / `TEST_ONLY_OUTBOX_PAYLOAD_ORPHAN`), supplies no transport fields, and expects:

```json
{
  "status": "DENIED",
  "statusCode": 403,
  "problemCode": "FORBIDDEN",
  "reason": "STALE_TRANSPORT",
  "outboxState": "PENDING",
  "securityBlocked": false,
  "identityChanges": 0,
  "payloadChanges": 0,
  "a2BusinessSideEffects": 0
}
```

Because the step would have created a third entry before the correction, the unchanged final exact assertion (`outboxEntries=2`, `securityBlockedEntries=1`) is the durable no-orphan proof; no assertion value was relaxed or changed. Fixture metadata remains `id=sec-s2-09-ivr-outbox-auth-blocked`, `slice=s2`, `negative=true`; the file now has 24 steps, and the repository still has 15 fixtures (6 S1 + 9 S2) with exactly seven negatives.

Why it fails before and passes after:

- **Before:** the first-observation omission path created the entry, marked it blocked with null baselines and returned `DENIED 401/403`; the new expected `403 STALE_TRANSPORT`/`securityBlocked=false` does not match, and `outboxEntries` would become 3.
- **After:** the creation guard returns `403 STALE_TRANSPORT` before `state.outbox.set(...)`; the entry is never created, so the final exact `outboxEntries=2` holds.

## 5. Static trace of the outbox sequence under the corrected branch

Not executed (see §6); a line-by-line trace of the 24 steps against the corrected branch:

| Step(s) | Entry state | Result |
| --- | --- | --- |
| `observe-401` | create E401, baseline `0901/1700000000` | `DENIED 401 OBSERVED_UNAUTHENTICATED`, blocked |
| seven policy denials | existing E401 | unchanged 403 reasons |
| `observe-403` | create E403, baseline `0902/1700000100` | `DENIED 403 OBSERVED_FORBIDDEN`, blocked |
| `recovery-retry` (E401) | fresh `0903/1700000200` | `OK RETRY_AFTER_RECOVERY`, baseline advanced |
| stale/omitted/null/empty/malformed/partial **recovery** steps and identity-defect recovery steps | existing E403, still blocked | recovery path rejects missing/invalid/reused tuples with `403 STALE_TRANSPORT` and identity defects with `IDENTITY_MUTATION_FORBIDDEN` |
| `recovery-403-accepted` | fresh `0908/1700000600` | `OK RETRY_AFTER_RECOVERY`, baseline advanced |
| `reblock-403` | omission allowed by valid baseline `0908/1700000600` | `DENIED 401 OBSERVED_UNAUTHENTICATED`, blocked |
| `reused-accepted-transport-denied` | `0908/1700000600` equals baseline | `403 STALE_TRANSPORT` |
| `initial-observation-transport-omitted-denied` | new EORPHAN, no entry created | `403 STALE_TRANSPORT`, `securityBlocked=false` |
| `assert-outbox-policy` | E401 + E403 only | `outboxEntries=2`, `securityBlockedEntries=1` |

## 6. Validation ledger

Working directory `/mnt/d/codex-call`. This session's Bash permission gate denied every Node/npm script execution (`This command requires approval`; no process was started, so no exit code, stdout or counter exists). This is a harness-permission limitation, not product evidence. Each required runtime command was attempted once and is recorded below; the controller must rerun them on this final tree (the S2 stdout hash necessarily differs from fix 1 because the fixture changed).

| # | Check | Command | Observed |
| --- | --- | --- | --- |
| 1 | Validator syntax | `node --check security/validate.mjs` | **BLOCKED** — `This command requires approval`; no exit code |
| 2 | A1 contract | `npm --prefix api/openapi run check:s3` | **BLOCKED** — `This command requires approval`; no exit code |
| 3 | A2 protocol | `node protocol/ivr/validate.mjs --stage s3 ...` | **BLOCKED** — `This command requires approval`; no exit code |
| 4 | S1 stage | `node security/validate.mjs --stage s1 ...` | **BLOCKED** — `This command requires approval`; no exit code |
| 5 | S2 stage (twice) | `node security/validate.mjs --stage s2 ...` | **BLOCKED** on the first invocation; no exit code. The identical rerun was not retried because the gate denies the command class, so byte-identical output cannot be claimed from this session |
| 6 | Real-evaluator initial-observation omission probe + outbox count | temporary evaluator probe | **BLOCKED** by the same Node gate. The behavior is instead frozen as the durable fixture step in §4 for controller execution |
| 7 | Fixture inventory | Glob + Grep on `security/v1/fixtures` | 15 JSON fixtures = 6 S1 + 9 S2; exactly 7 files with `"negative": true`; no S3 fixture and no new fixture file |
| 8 | `sec-s2-09` structure | `jq '{id, slice, negative, steps: (.steps | length)}'` | `id=sec-s2-09-ivr-outbox-auth-blocked`, `slice=s2`, `negative=true`, `steps=24` |
| 9 | Fixture JSON syntax | `jq empty security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` | exit 0, no output |
| 10 | Changed-file hashes | `sha256sum` on the two changed paths and S2-04/doc/model | Hashes in §2; S2-04, document and model unchanged |
| 11 | S1 fixture hashes | `sha256sum` on the six `sec-s1-*.json` files | All six equal the accepted S1 checkpoint values listed in §2 |
| 12 | Exact-path diff whitespace | `git diff --check -- security/validate.mjs security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` | exit 0, no output. The fixture is untracked, so `git diff` does not inspect it; step 13 covers it directly |
| 13 | Conflict markers / trailing whitespace | Grep `^(<<<<<<<\|=======\|>>>>>>>)` and `[ \t]+$` on `security` and the changed files | none |
| 14 | Encoding / BOM | `file -bi` on both changed paths | `text/plain; charset=us-ascii` and `application/json; charset=us-ascii`; valid UTF-8 subsets, no BOM |
| 15 | Repository state | `git rev-parse HEAD`, `git status --porcelain` on the write scope | HEAD `88079d208e90ebc176ab54a25734c38bbf7732aa`; only `security/validate.mjs` modified, `sec-s2-09` remains an untracked S2 fixture, and this artifact added; no staging/commit/branch change |

Frozen S2 counters expected from a successful controller run (not observed here): `trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 signature_profiles=2 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=11 fixtures=15 passed=15 failed=0 negative=7 slice_fixtures=6+9`, with `drift=0`, `doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0`, `stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0`. Fixture-step count is not a validator counter; the only structural fixture change is the single added step.

## 7. Preserved behavior and no-side-effect declaration

- Recovery identity presence/equality, complete-format transport validation, reuse rejection, baseline advancement on accepted recovery and recovery-signal validation are unchanged from fix 1.
- Hot retry, redial, re-resolve, reservation and settlement remain rejected in the same order with the same reasons; the new guards run only on entry creation and 401/403 observation.
- Entries remain `PENDING`; event identity and payload are never rewritten; `outboxIdentityChanges`, `outboxPayloadChanges`, `a2BusinessSideEffects` and all A2 counters remain zero.
- The ending-`A` signature fix and `sec-s2-04` are untouched.
- Other fixtures using `ivr-outbox` (`sec-s2-08`) have no 401/403 observation; its baseline creation supplies a full tuple, so the new guards are not exercised there.

## 8. Residual risks

| ID / area | Remaining gap | Owner / destination |
| --- | --- | --- |
| `A3 S2 correction-1 runtime validation` | Node/npm execution was permission-blocked in this author session, so syntax, A1/A2, S1/S2 two-run counters and the real-evaluator omission/outbox-count probe are not author-observed | Controller; rerun the exact §6 commands before re-review |
| `F-S2-R1-01` | Author-complete with durable initial-observation coverage; independent re-review has not occurred | Next gate: fresh MiMo re-review, then controller acceptance |
| `R-A3-A2-AUTH-OUTBOX` | 401/403 `PENDING`/security-blocked policy is frozen, but Q8/Q13/Q25 retry/backoff/alert numbers and real Redis/IVR integration remain unverified | IVR/Backend; A2/IVR integration review |
| `R-A3-MTLS-CAPABILITY`, `R-A3-PROVIDER-VERIFY`, `R-A3-SESSION-LIFECYCLE`, `R-A3-NONCE-STORE`, `R-A3-HMAC-ROTATION-REPLAY` | Real mTLS, provider verification, session/OTP, Redis atomicity and rotation/replay compatibility remain unverified | As previously classified |
| S3 scope | Media upload/playback credentials, COS private policy, audit access, high-risk controls and deterministic redaction remain out of scope | S3 implementation gate |

## 9. No-runtime/no-cloud and no-gate-work declaration

No Java/runtime service, cloud resource, real credential, KMS/Secret Manager/CAM/COS call, Redis/PostgreSQL call, IVR line, log collector, analytics system or payment provider was implemented or contacted. The evaluator and fixtures are static security-model assertions, not production acceptance or penetration testing.

No staging, commit, push, PR creation/modification, merge, branch creation, review, self-review, S3 work or controller edit was performed. The correction stops here for controller validation and a fresh independent re-review.
