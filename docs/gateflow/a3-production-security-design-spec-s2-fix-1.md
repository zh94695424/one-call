# A3 S2 fix 1 — IVR-outbox recovery freshness/identity and deterministic invalid-signature construction

> Work unit：`A3-production-security-design-spec`
>
> Slice：S2 — authentication domains, service identities, signing, replay and IVR-outbox policy
>
> Task label：`a3-security-s2-fix1-ds-20261025-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Gate：`fix`（bounded original-author fix only；no self-review, no subsequent gate）
>
> Base：accepted S1 checkpoint `88079d208e90ebc176ab54a25734c38bbf7732aa` (`gateflow: accept A3-production-security-design-spec S1`)
>
> Accepted review：`docs/reviews/code-review-20261025-154806.md`, SHA-256 `A31BAE39710C077DC0C949F5AD32ED5CE33E40D82F78F6D834103930843BAD2C`
>
> Accepted findings：`F-S2-R1-01` (High), `F-S2-R1-02` (Medium)
>
> Stop condition：bounded fix author-complete; controller revalidation and a fresh MiMo re-review are required before any acceptance.

## 1. Scope and allowed writes

Only `security/validate.mjs`, `security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json` and `security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` were modified. This artifact is the only new file. The canonical document, machine model, implementation report, controller, prior reviews, accepted A0/A1/A2/S1 artifacts, OpenAPI/protocol sources, prototypes, screenshots and shared business logic were not modified. Fixture IDs were not changed, no fixture file was added, and no numeric production TTL/window/backoff value, Problem code, A2 state/event or S3 field was invented.

## 2. Exact changed files and SHA-256

| Path | Lines | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| `security/validate.mjs` | 2750 | 138170 | `3F098EBCA77E3886146118544BD0CD8E25FBF56194C6CA5015B2BB091BA22655` |
| `security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json` | 173 | 7039 | `9C96D7B7B1560C6BD1BE426E33A86C3F2AB3E1759ED435709D3F451385AF9249` |
| `security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` | 301 | 14512 | `2C2079540F78B9625867C808C8328DFB5D96102522FE867C26B30CEFE5FAD1EB` |
| `docs/gateflow/a3-production-security-design-spec-s2-fix-1.md` | this artifact | this artifact | reported in the final response |

Unchanged and independently re-hashed:

| Path | SHA-256 | Comparison |
| --- | --- | --- |
| `docs/17-production-security-design-spec.md` | `4D0BF8CA6DB20F0E6C95E4D68C7B59CF3C8D63AB5193D56F2C84A9B7FB0A0DE7` | equals S2 implementation report §2 |
| `security/v1/security-design-v1.json` | `A9B196317247E3B7DB18E7E947AF6FAA586EDA6981881B586CD66CA5898EE438` | equals S2 implementation report §2 |
| `docs/reviews/code-review-20261025-154806.md` | `A31BAE39710C077DC0C949F5AD32ED5CE33E40D82F78F6D834103930843BAD2C` | equals controller block |
| `docs/gateflow/a3-production-security-design-spec-s2-implementation.md` | `96BAB75C12513D023285E3FEAA26AE309661D5EF0B840FCCB2C014FC8040D18A` | equals S2 implementation report §2 |

The six accepted S1 fixtures remain byte-identical to the accepted checkpoint:

```text
844b67a4f10a8dcbcd690df6093e9de33dc8bc964ac210a4850e89d1f806f576  sec-s1-01-envelope-purpose-roundtrip.json
2acec51fd57e468af58546f35a9dd1021de2cbedad75c5610f3963e45155371d  sec-s1-02-hmac-index-equality.json
5cbfa710dc2a88b6a44f87590b3ba580370e51b5e528db4f349f1f88282142c5  sec-s1-03-cross-purpose-denied.json
6a9fbd95d4971a2b551bcda28ca209fc173a9e3a9ca1d4c00841b7acb3737206  sec-s1-04-rotation-historical-replay.json
caa71dffc8c2d015130bf01f0ce95ae69dae7ba963a819275dc157a5eb107c7d  sec-s1-05-key-unavailable-failclosed.json
00d85e4401e6e31f858804ab600a6e3d1e694a4e1c2f938f00588da6c4b2c0e2  sec-s1-06-append-only-rotation-safety.json
```

## 3. `F-S2-R1-01` — IVR-outbox recovery must require the same non-empty identity and fresh non-empty transport

### 3.1 New local predicates

`security/validate.mjs` now defines, inside `createMachine()` so they are pure evaluator helpers:

- `isValidDomainIdentity(identity)`: requires a non-null, non-array object with at least one own value; every value must be a non-empty string. It does **not** require `callId`/`eventId`/`recordingId`/`decisionId` together, because A2 messages own different identity subsets.
- `isValidIvrTimestamp(value)`: string matching `^[0-9]{1,12}$` and satisfying the A1 1–64 length envelope.
- `isValidIvrNonce(value)`: string matching `[A-Za-z0-9_-]{1,128}`.

No new Problem code, reason, state, event or numeric policy value was added. Invalid values reuse the existing `403 FORBIDDEN` mappings: `IDENTITY_MUTATION_FORBIDDEN` for identity defects and `STALE_TRANSPORT` for transport defects.

### 3.2 Evaluation order in `case 'ivr-outbox'`

The branch now runs in this order:

1. **Entry creation.** If the entry does not exist and `create !== false`, the step must carry a valid structured non-empty `domainIdentity`. Invalid identity rejects with `403 FORBIDDEN / IDENTITY_MUTATION_FORBIDDEN` **before** the entry is created, so an empty/omitted identity can never become a stored baseline. A valid identity is stored verbatim (the previous `step.domainIdentity || null` fallback is gone).
2. **Existing entry.** Payload mutation is checked first (unchanged). `mutateEventId: true` is checked next (unchanged). If `domainIdentity` is supplied, it must be valid and exactly `deepEqual` to the stored identity; invalid, empty or mismatched identity rejects with `403 FORBIDDEN / IDENTITY_MUTATION_FORBIDDEN` (this applies to every action, not only recovery).
3. **Policy attempts.** Hot retry, redial, re-resolve, reservation and settlement retain their existing flags, order and reasons. These steps are not forced to carry an identity.
4. **401/403 observation.** The entry is marked security-blocked. The observation must carry a valid identity equal to the stored identity, otherwise `IDENTITY_MUTATION_FORBIDDEN`. Transport is validated only when supplied: if either `transportNonce` or `transportTimestamp` is present, both must satisfy the frozen formats; otherwise the step is rejected with `403 FORBIDDEN / STALE_TRANSPORT`, the entry remains blocked, and the baseline is not advanced. When both are valid, the tuple becomes the prior-delivery baseline.
5. **Recovery retry.** Only when `entry.securityBlocked` is true and a recovery signal is present: the signal must be one of the four existing values; `step.domainIdentity` must be present, valid and exactly equal to the stored identity; both transport values must be valid strings; both must differ from the stored prior-delivery values (reused or partially reused tuples are `STALE_TRANSPORT`). On acceptance the entry is unblocked **and** `entry.previousNonce` / `entry.previousTimestamp` are advanced to the accepted tuple, so a later block/recovery cycle cannot reuse that tuple.
6. **No-op inspection.** Existing `OUTBOX_UNCHANGED` behavior is retained for non-observing, non-recovering inspection steps.

The entry stays in `PENDING` throughout; event ID and payload are never rewritten; `outboxIdentityChanges`, `outboxPayloadChanges`, `a2BusinessSideEffects` and the A2 counters remain zero.

## 4. `F-S2-R1-02` — invalid-signature construction must be guaranteed non-identical

`security/validate.mjs` now mutates the last character of a syntactically valid `v1.<keyId>.<base64url>` signature to a **different** character in the same base64url alphabet:

```js
const validSignature = headers['X-ONECALL-SIGNATURE'];
const replacement = validSignature.endsWith('A') ? 'B' : 'A';
headers['X-ONECALL-SIGNATURE'] = `${validSignature.slice(0, -1)}${replacement}`;
```

This is always non-identical (`s.slice(0, -1) + c !== s` unless `c` is the existing last character, which the branch excludes). The value stays format-valid, so evaluation reaches key lookup and constant-time signature verification and returns `401 UNAUTHENTICATED / SIGNATURE_INVALID` rather than falling through to `OK`.

`sec-s2-04`'s existing `bad-signature` step now uses the controller-proven collision timestamp `1700000019` with matching `receivedAt: 1700000019`; the body, path, nonce and material are unchanged. The controller proved that this material derives the ending-`A` signature `v1.TEST_ONLY_K17_V1.qNGJzl7JjmI9YuVDdsDJ8Ta9xtK0EiFnNVdhY3seLKA`, which the old last-character-to-`A` mutation left unchanged (fixture result `OK/200/ACCEPTED`, `passed=14 failed=1`). The new replacement changes the trailing `A` to `B` and yields the expected 401. The fixture ID, step count and `negative: true` flag are unchanged.

## 5. Regression steps added to `sec-s2-09`

`stale-transport-denied` now supplies the stored E403 domain identity (previously omitted). Eleven steps were added before the final assert; all reuse the existing two outbox entries, so `outboxEntries` stays 2 and the negative-fixture count stays 7.

| Added step | Case | Expected result |
| --- | --- | --- |
| `omitted-transport-denied` | recovery with no transport fields | `403 FORBIDDEN / STALE_TRANSPORT`, still blocked |
| `null-transport-denied` | both transport fields `null` | `403 FORBIDDEN / STALE_TRANSPORT` |
| `empty-transport-denied` | both transport fields `""` | `403 FORBIDDEN / STALE_TRANSPORT` |
| `malformed-transport-denied` | nonce with spaces; timestamp with a letter | `403 FORBIDDEN / STALE_TRANSPORT` |
| `partially-reused-transport-denied` | fresh nonce, reused timestamp | `403 FORBIDDEN / STALE_TRANSPORT` |
| `omitted-recovery-identity-denied` | valid fresh transport, no identity | `403 FORBIDDEN / IDENTITY_MUTATION_FORBIDDEN` |
| `empty-recovery-identity-denied` | identity value `""` | `403 FORBIDDEN / IDENTITY_MUTATION_FORBIDDEN` |
| `mismatched-recovery-identity-denied` | valid but different `callId` | `403 FORBIDDEN / IDENTITY_MUTATION_FORBIDDEN` |
| `recovery-403-accepted` | valid identity + fresh well-formed transport after recovery signal | `200 OK / RETRY_AFTER_RECOVERY`, unblocked, baseline advanced |
| `reblock-403` | new 401 observation without a new transport tuple | `401 UNAUTHENTICATED / OBSERVED_UNAUTHENTICATED`, blocked |
| `reused-accepted-transport-denied` | reuse the tuple accepted by `recovery-403-accepted` | `403 FORBIDDEN / STALE_TRANSPORT` |

The final assert remains exact: `outboxEntries=2`, `securityBlockedEntries=1` (E401 is unblocked after its recovery; E403 is re-blocked and its accepted tuple is then rejected as reused), with zero identity/payload changes and zero A2 events/calls/reservations/settlements/redials/re-resolves.

Why these fail before and pass after:

- Omitted/null/empty/malformed transport: the pre-fix inequality treated these as fresh and returned `OK / RETRY_AFTER_RECOVERY`; the new steps expect `403 STALE_TRANSPORT`, so the old evaluator fails them.
- Omitted recovery identity: the pre-fix guard short-circuited on a falsy step identity; the new step expects `403 IDENTITY_MUTATION_FORBIDDEN`, so the old evaluator fails it. An empty identity captured at block and replayed at recovery was also accepted because both sides were `deepEqual`; creation-time validation now prevents that capture, and the empty/mismatched steps remain durable coverage.
- Reused accepted transport: the pre-fix recovery did not advance `previousNonce`/`previousTimestamp`; `reused-accepted-transport-denied` would see `0908/1700000600` as unequal to the original baseline `0902/1700000100` and accept it, while the fixed evaluator sees the advanced baseline and rejects it as stale.
- `partially-reused-transport-denied` and `mismatched-recovery-identity-denied` were already rejected by the pre-fix inequality/truthy-mismatch paths; they are retained as durable regression coverage for the now-explicit format and identity guards.
- The collision step in `sec-s2-04` is the controller-proven fail-before case: old code leaves an ending-`A` signature unchanged (`OK/200/ACCEPTED`, `passed=14 failed=1`); new code changes it and returns the expected `401 SIGNATURE_INVALID`.

## 6. Validation ledger

Working directory `/mnt/d/codex-call`. This session's Bash permission gate denied every Node/npm execution (`This command requires approval`; no process was started, so no exit code, stdout or counter exists). This is a harness-permission limitation, not product evidence. The required runtime commands were attempted and are recorded below; the controller must rerun them on this final tree.

| # | Check | Command | Observed |
| --- | --- | --- | --- |
| 1 | Validator syntax | `node --check security/validate.mjs` | **BLOCKED** — `This command requires approval`; no exit code |
| 2 | A1 contract | `cd api/openapi && npm run check:s3` | **BLOCKED** — `npm run check:s3` requires approval; no exit code |
| 3 | A2 protocol | `node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md` | **BLOCKED** — requires approval; no exit code |
| 4 | S1 stage | `node security/validate.mjs --stage s1 --model security/v1/security-design-v1.json --fixtures security/v1/fixtures --spec docs/17-production-security-design-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md --a2-model protocol/ivr/v1/ivr-protocol-v1.json --a2-fixtures protocol/ivr/v1/fixtures` | **BLOCKED** — requires approval; no exit code |
| 5 | S2 stage (twice) | same arguments with `--stage s2` | **BLOCKED** — requires approval; no exit code, so byte-identical output cannot be claimed from this session |
| 6 | Focused omission/null/empty/malformed/reuse/identity probes and ending-`A` probe | temporary evaluator probes | **BLOCKED** by the same Node permission gate. The probe cases are now durable steps in `sec-s2-09`/`sec-s2-04` for controller execution; no temporary probe file was created. |
| 7 | Fixture inventory | Glob/Grep on `security/v1/fixtures` | 6 S1 + 9 S2 = 15 JSON fixtures; exactly 7 files with `"negative": true`; no S3 fixture and no new fixture file |
| 8 | Changed-file hashes | `sha256sum` on the three changed paths and the model/document/report | Hashes in §2; model and document match the S2 implementation baseline; validator and two fixtures changed as intended |
| 9 | S1 fixture hashes | `sha256sum` on the six `sec-s1-*.json` files | All six equal the accepted S1 checkpoint values listed in §2 |
| 10 | Diff whitespace | `git diff --check -- security/validate.mjs security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json docs/gateflow/a3-production-security-design-spec-s2-fix-1.md` | exit 0, no output for the three tracked paths; the artifact is untracked, so it was checked separately by Grep `[ \t]+$` (none) |
| 11 | Conflict markers | Grep `^(<<<<<<<\|=======\|>>>>>>>)` on the changed files, fixtures and artifact | none |
| 12 | Trailing whitespace | Grep `[ \t]+$` on the changed files and artifact | none |
| 13 | Encoding / BOM | `file -bi` on the three changed paths and the artifact | `us-ascii` (valid UTF-8 subset) for the modified paths; `utf-8` for the artifact; no BOM |
| 14 | Repository state | `git rev-parse HEAD`, `git status --porcelain` | HEAD `88079d208e90ebc176ab54a25734c38bbf7732aa`; only the pre-existing S2 working-tree files plus this fix's three paths and artifact are present; no staging/commit/branch change |

Frozen S2 counters expected from a successful controller run (not observed here): `trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 signature_profiles=2 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=11 fixtures=15 passed=15 failed=0 negative=7 slice_fixtures=6+9`, with `drift=0`, `doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0`, `stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0`. These are unchanged by construction: no fixture ID, count or negative flag changed and no model/document byte changed.

## 7. Upstream and S1 regression evidence

- The canonical document and model are byte-identical to the S2 implementation report (`4D0BF8CA…`, `A9B19631…`); no A0/A1/A2 source, OpenAPI, protocol model/fixture, accepted S1 artifact or controller file was touched.
- The six S1 fixture hashes match the accepted checkpoint exactly (listed in §2).
- All six S1 fixtures use only `encrypt`, `decrypt`, `lookup`, `persist`, `rotate`, `retire`, `request` or `assert` actions. The modified evaluator branches (`ivr-outbox`, IVR `signatureMode: invalid`) are unreachable under the S1 stage, and S1 stage evaluation filters to S1 fixtures only, so S1 projection output is unchanged.
- The `sec-s2-09` fixture metadata still has `slice: "s2"`, `negative: true` and the same ID/filename; the `sec-s2-04` fixture retains its ID, `slice`, `negative` flag and step inventory.

## 8. Residual risks

| ID / area | Remaining gap | Owner / destination |
| --- | --- | --- |
| `A3 S2 fix-1 runtime validation` | Node/npm execution was permission-blocked in this author session, so the runtime counters and byte-identical S1/S2 outputs are not author-observed | Controller; rerun the exact §6 commands before re-review |
| `F-S2-R1-01` / `F-S2-R1-02` | Author-complete with durable fixture coverage; independent re-review has not occurred | Next gate: fresh MiMo re-review, then controller acceptance |
| `R-A3-A2-AUTH-OUTBOX` | 401/403 `PENDING`/security-blocked policy is frozen, but Q8/Q13/Q25 retry/backoff/alert numbers and real Redis/IVR integration remain unverified | IVR/Backend; A2/IVR integration review |
| `R-A3-MTLS-CAPABILITY` | Real mTLS issuance, pinning, clock synchronization and carrier identity remain unverified (Q7/Q15) | Telephony/IVR/Security |
| `R-A3-PROVIDER-VERIFY` | Vendor signature algorithm/canonicalization/payload/window remain Q6/Q15 | Payments/Backend |
| `R-A3-SESSION-LIFECYCLE` | Session/OTP/WeChat login, employee MFA and TTL remain Q1/Q19 | Identity/Backend |
| `R-A3-NONCE-STORE` | Real Redis `SET NX` atomicity, TTL and failover remain unverified (Q25) | Security/Backend |
| `R-A3-HMAC-ROTATION-REPLAY` | S1 HMAC rotation / A2 fingerprint runtime compatibility remains | Security/IVR/Backend |
| S3 scope | Media upload/playback credentials, COS private policy, audit access, high-risk controls and deterministic redaction remain out of scope | S3 implementation gate |

## 9. No-runtime/no-cloud and no-gate-work declaration

No Java/runtime service, cloud resource, real credential, KMS/Secret Manager/CAM/COS call, Redis/PostgreSQL call, IVR line, log collector, analytics system or payment provider was implemented or contacted. The evaluator and fixtures are static security-model assertions, not production acceptance or penetration testing.

No staging, commit, push, PR creation/modification, merge, branch creation, review, self-review, S3 work or controller edit was performed. The fix stops here for controller validation and a fresh independent re-review.
