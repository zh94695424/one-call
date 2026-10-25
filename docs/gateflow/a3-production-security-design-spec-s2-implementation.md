# A3 S2 implementation — authentication domains, service identities, signing and replay rejection

> Work unit：`A3-production-security-design-spec`
>
> Slice：S2 — authentication domains, service identities, credentials, IVR/provider signing, nonce/replay rejection and IVR-local 401/403 outbox policy
>
> Task label：`a3-security-s2-impl-ds-20261025-01`
>
> Provider/model：Claude Code / `ds` / `deepseek-flash[1m]`
>
> Gate：`implementation` (S2 author completion only)
>
> Base：accepted S1 checkpoint `88079d208e90ebc176ab54a25734c38bbf7732aa` (`gateflow: accept A3-production-security-design-spec S1`)
>
> Accepted plan：`docs/gateflow/a3-production-security-design-spec-plan.md`, SHA-256 `12E4136EE557D604FF680F1B162265C68171C5C724EF2888FB69D429007405F1`
>
> Stop condition：S2 author-complete; controller runs hash-bound executable validation before any acceptance, review or S3 work.

## 1. Gate, scope and binding inputs

- Binding contract: the A3 section of `docs/gateflow/project-execution-control.md`, accepted plan §§4.1/4.7/4.8/5.1–5.4/6.2–6.3/7/8.3, accepted S1 artifacts at checkpoint `88079d208e90ebc176ab54a25734c38bbf7732aa`, and accepted A0/A1/A2 sources.
- S2 freezes only: five trust surfaces/auth domains and A1 audiences/operation counts; seven trust boundaries; six internal service identities with least privilege; ten credential classes; two signature profiles; audience registry; credential/session lifecycle and nonce policy without numeric TTL/window values; deterministic IVR and provider mTLS + signature + timestamp + nonce/replay evaluation; A2 identity/exactly-once preservation on authentication failure; the IVR-local outbox 401/403 `PENDING`/security-blocked policy; `INV-A3-07..11` (cumulative 11); nine S2 fixtures (cumulative 15, seven negatives).
- No S3 model section, invariant text or fixture ID was authored. No A0/A1/A2 accepted artifact, OpenAPI source, protocol source/fixture, prototype, screenshot, shared business logic, controller, accepted plan/review/fix or historical review was modified.
- No new A1 operation/header/schema/field/Problem code/role, A0 table/column/state or A2 message/event/identity/invariant/digest was added. No production TTL, replay window, backoff, region, SKU, price, capacity, WORM/object-lock or vendor algorithm value was invented.

## 2. Exact changed files and SHA-256

Only the three allowed existing paths were modified and exactly nine approved fixture files plus this report were created. Every other repository path remained read-only.

| Path | Lines | Bytes | SHA-256 |
| --- | ---: | ---: | --- |
| `docs/17-production-security-design-spec.md` | 490 | 56845 | `4D0BF8CA6DB20F0E6C95E4D68C7B59CF3C8D63AB5193D56F2C84A9B7FB0A0DE7` |
| `security/v1/security-design-v1.json` | 1439 | 57883 | `A9B196317247E3B7DB18E7E947AF6FAA586EDA6981881B586CD66CA5898EE438` |
| `security/validate.mjs` | 2678 | 134975 | `F5A5F80ECBA2DC584B3C3F96BA0B93C02C33251D1ACD26FF38D16D4CCD129738` |
| `security/v1/fixtures/sec-s2-01-five-domains-accepted.json` | 134 | 4273 | `32AEA8A37CF790800ADE48FDB90A6B3F820831FC4FB920D32F48B51877C41669` |
| `security/v1/fixtures/sec-s2-02-cross-domain-rejected.json` | 130 | 5307 | `DDC5E77F7F62DC621990C9B8CABDF7ED226A9CE4CA0E77C37A1F7AEF259762D1` |
| `security/v1/fixtures/sec-s2-03-service-identity-least-privilege.json` | 121 | 4636 | `AE0C3F9A0363F1B5F0CD4346F23CEDCEE0A037C13DFF7E2797F0EB6ACF07AD59` |
| `security/v1/fixtures/sec-s2-04-ivr-signature-invalid.json` | 173 | 7039 | `07013888B8F717BF0E82DD6565C0E7DB9FC6445182927A5FE298ED683E5CEC1C` |
| `security/v1/fixtures/sec-s2-05-ivr-replay-rejected.json` | 127 | 5158 | `3BA136E05ACB8D6E7A23B08406A686FC1818CB62D7288CDC80400E99ABC44E39` |
| `security/v1/fixtures/sec-s2-06-provider-signature-replay.json` | 187 | 6249 | `1113D3C870E9E4CC679BC1D1B7FD09675EED613878FF6446C3A204502BDB3C57` |
| `security/v1/fixtures/sec-s2-07-signing-key-rotation.json` | 173 | 6866 | `88A4DE4FE4B7FF64BFB759A5E744253BEA3C32CE26DF0A2BB45C30BD0D6783DF` |
| `security/v1/fixtures/sec-s2-08-failclosed-dependency-preserves-a2.json` | 133 | 6379 | `4DEA3B0C14F55840C18A4E4E6C9367F1139CA7DCAA64E8875EF80071BF1BF712` |
| `security/v1/fixtures/sec-s2-09-ivr-outbox-auth-blocked.json` | 184 | 7462 | `955093669A1CC550CA400DC80B695B9650C0CE9115F338C096564AB42380979A` |
| `docs/gateflow/a3-production-security-design-spec-s2-implementation.md` | this report | this report | reported in the final response; re-verifiable by the controller |

The six S1 fixture hashes are unchanged from the accepted checkpoint:

```text
844b67a4f10a8dcbcd690df6093e9de33dc8bc964ac210a4850e89d1f806f576  sec-s1-01-envelope-purpose-roundtrip.json
2acec51fd57e468af58546f35a9dd1021de2cbedad75c5610f3963e45155371d  sec-s1-02-hmac-index-equality.json
5cbfa710dc2a88b6a44f87590b3ba580370e51b5e528db4f349f1f88282142c5  sec-s1-03-cross-purpose-denied.json
6a9fbd95d4971a2b551bcda28ca209fc173a9e3a9ca1d4c00841b7acb3737206  sec-s1-04-rotation-historical-replay.json
caa71dffc8c2d015130bf01f0ce95ae69dae7ba963a819275dc157a5eb107c7d  sec-s1-05-key-unavailable-failclosed.json
00d85e4401e6e31f858804ab600a6e3d1e694a4e1c2f938f00588da6c4b2c0e2  sec-s1-06-append-only-rotation-safety.json
```

S1 model sections (`modelVersion`, `specVersion`, `upstream`, `envelopeProfile`, `hmacProfile`, `keyStates`, `rotationPolicy`, `recoveryPolicy`, `replayCompatibility`, `keyClasses`, `dataClasses`, `deferredInputs`) and `INV-A3-01..06` were compared against checkpoint `88079d2` and are byte-equivalent after canonical JSON serialization.

## 3. Registries, counts and document markers

- Model top-level sections at S2: S1 sections plus `trustSurfaces` (5), `authDomains` (5), `trustBoundaries` (7), `serviceIdentities` (6), `credentialClasses` (10), `signatureProfiles` (2), `noncePolicy` and `audienceRegistry` (11 audience rows). No S3 section exists.
- Auth domains and operation counts exactly match A1: `customer` 18, `employee` 21, `ivr-service` 6, `public-capability` 3, `provider` 1; audiences exactly `customer`, `employee`, `ivr-service`, `public-capability`, `provider`.
- Service identities: `core-api`, `async-worker`, `scheduler`, `media-ai-worker`, `ivr-gateway`, `media-gateway`. IVR/provider external identities are not internal service identities. Every allowed-call row is explicit; no wildcard action/resource exists; frontend units are absent from the registry.
- Credential classes `C01..C10` match plan §5.4. C09 records `onecall:evidence-upload:v1` only as an issuance-side policy classification, not a COS embedded audience.
- Signature profiles: `ivr-request-v1` and `provider-callback-v1`. IVR names/headers/canonical string/order/error mapping are frozen; A1's TIMESTAMP 1-64 and NONCE 1-128 are preserved and no signature header length is asserted. Provider vendor algorithm/canonicalization/payload/window remain Q6/Q15 deferred.
- Invariants: cumulative exactly 11, `INV-A3-01..06` unchanged plus `INV-A3-07..11`.
- Document marker blocks: `security-domain-map` 5×7, `security-service-map` 6×5, `security-credential-map` 10×8, in addition to the unchanged S1 `security-key-map` 20×7, `security-data-map` 28×8 and `security-deferred-map` 23×6. Row order and values equal the model projections.
- Fixtures: 15 total (`6+9`); S2 slice files exactly nine; explicit negatives 7 (`sec-s1-03`, `sec-s1-05`, `sec-s2-02`, `sec-s2-04`, `sec-s2-05`, `sec-s2-06`, `sec-s2-09`).

## 4. Implementation decisions

1. **Stage projection.** `--stage s1` validates the S1 projection; it accepts a cumulative model/document only when the S2 registries/markers, if present, are exact. It evaluates exactly the six S1 fixtures and rejects S3 content. `--stage s2` requires the complete cumulative S1+S2 model/document and evaluates exactly 15 fixtures. `--stage s3` is rejected as not implemented in this slice. The historical S1 checkpoint files also pass the current `--stage s1` validator, so S1 behavior is not weakened.
2. **Cross-domain rejection.** A credential is resolved to its owning domain and audience; a request path resolves to a required domain by prefix. Mismatched domains/audiences return existing A1 `UNAUTHENTICATED` (401), unsupported capability returns `SHARE_TOKEN_INVALID` (404), and role/access denial returns `FORBIDDEN` (403). The machine records zero session/call/decision/task/reservation/settlement/event side effects.
3. **Service least privilege.** Calls are allowed only by exact membership in the frozen `allowedCalls` list. Unknown identities, frontend identities, `*` calls and audit reads are denied with `FORBIDDEN`; a frontend secret attempt is reported as rejected, never granted.
4. **IVR signing.** The signature value is `v1.<keyId>.<base64url(HMAC-SHA-256(K17, canonicalString))>`; the fixture keyId is the Secret Manager version label. Verification order is mTLS → required headers/lengths → timestamp window → signature verify (active/verify-only keyId, constant-time) → Redis `SET NX` → A1/A2. The canonical string is the frozen six-line value with the exact-body SHA-256 and no trailing newline.
5. **IVR failures.** Missing/bad mTLS, missing/malformed headers, bad signature, out-of-window timestamp, unknown/disabled key and nonce replay return `401 UNAUTHENTICATED`; a disallowed service identity returns `403 FORBIDDEN`; unavailable nonce store or Secret Manager returns `503 SERVICE_UNAVAILABLE`. All occur before A2 facts.
6. **Nonce/replay.** Nonce uniqueness is `(serviceIdentity, nonce)` with Redis `SET NX`; no numeric TTL/window is frozen. Transport timestamp/nonce may refresh on retry; A2 `eventId`/payload may not. A retry after a transient failure returns the A2 first result with zero new event identity, redial or reservation.
7. **Provider callback.** The verification order is mTLS → headers → provider profile (Q6) → window → signature → event identity/fingerprint → nonce → payment. An exact event/fingerprint duplicate returns 200 duplicate without consuming a nonce; a new or changed event must pass the nonce check; a changed payload returns 409 after nonce acceptance. No duplicate payment or entitlement is created.
8. **IVR outbox 401/403.** The same entry remains `PENDING` and is logically marked security-blocked where local metadata exists. Hot retry, identity mutation, payload mutation, redial, re-resolve, reservation and settlement are denied. Alerts carry fixed redaction tokens and no credential/signature/nonce. Retry is allowed only after an explicit credential/key/certificate/service-authorization recovery signal, with the same domain identity and immutable payload plus a fresh timestamp and nonce. No A2 state/event/digest is added.
9. **Fixture evaluation.** The evaluator computes outcomes from model sections, fixture materials and prior step state; it never reads `expected` to choose a branch. Unknown actions, missing/wrong-type fields and dependency failures fail closed. Fixture window/TTL inputs are deterministic evaluator inputs only, not production Q25 values.

## 5. Validation commands and observed outcomes

Run from `/mnt/d/codex-call`.

| Check | Command | Observed |
| --- | --- | --- |
| Validator syntax | `node --check security/validate.mjs` | exit 0, no output |
| A1 contract | `cd api/openapi && npm run check:s3` | exit 0; `operations=49 customer=18 operations=21 ivr=6 public-share=3 provider-callback=1 total=49`; `PASS check-doc-map (s3)` |
| A2 protocol | `node protocol/ivr/validate.mjs --stage s3 ...` | exit 0; `messages=12 event_types=11 identities=22 fixtures=25 passed=25 negative=3 slice_fixtures=6+11+8`; digest `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`; `PASS protocol-validation (s3)` |
| S1 stage on final cumulative files | `node security/validate.mjs --stage s1 ...` (two runs) | both exit 0, stderr empty, byte-identical stdout SHA-256 `3E991F1D7A075E826E70E7C2654568019A6A3644383ECECE4699AD82B2CAB0C6`; `20/28/23/18/5/6/6/2` and zero drift/doc/token/stage/fixture counters |
| S2 stage on final cumulative files | `node security/validate.mjs --stage s2 ...` (two runs) | both exit 0, stderr empty, byte-identical stdout SHA-256 `5BA15F8113D70A6E3FBA115FE790788DB7410753BE478701D131C51970F6A55F` |
| Historical S1 checkpoint | `git show 88079d2:...` model/doc plus unchanged S1 fixtures, `--stage s1` | exit 0; same `20/28/23/18/5/6/6/2` projection |
| Mutation matrix | `/tmp/a3-s2-probes.mjs` temporary copies | 60 probes, `probe_failures=0`; unmutated control passes, every mutation is rejected |
| Encoding | `file -bi` on the three modified paths | UTF-8; no BOM |
| Conflict markers | line-start grep | none |
| Trailing whitespace | `grep -n '[[:blank:]]$'` | none |
| Diff whitespace | `git diff --check -- docs/17... security` | exit 0 |
| URLs | grep on doc/model/S2 fixtures | none |

Exact S2 counter block:

```text
stage=s2 trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 signature_profiles=2 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=11 fixtures=15 passed=15 failed=0 negative=7 slice_fixtures=6+9
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s2)
```

## 6. Mutation matrix

All probes ran against temporary copies; the repository was not modified. The control copy passed. Rejected mutations:

- **Registry shape:** missing/extra/duplicate auth domain, service identity and credential class; wrong-type operation count; missing `noncePolicy`; missing/duplicate signature profile; IVR signature header drift; future invariant; future model section; model URL.
- **Service privilege:** wildcard allowed call; frontend service identity with a secret; unknown service; denied audit read; denied COS write/DB call.
- **Documentation:** domain marker row drift; missing domain marker; future section token; URL.
- **Fixture input mutation:** valid-domain credential swapped; cross-domain path changed; missing/bad signature made valid; malformed timestamp; out-of-window window widened; unknown key made known; disabled key started active; replayed nonce made fresh; Redis outage made available; provider changed-payload nonce replayed; provider duplicate payload changed; outbox hot retry/identity/payload/redial/re-resolve/reservation/settlement attempts disabled; stale transport made fresh.
- **Fixture expected mutation:** expected domain changed, proving expected values are compared not used to decide.
- **Fixture structure:** unknown action; missing required field; future invariant text; future S3 file; extra ID; duplicate ID; missing S1 fixture.
- **Upstream drift:** A0 catalog row removed; A0 sensitive row added; A1 operation removed; A1 IVR nonce header renamed; A2 event type removed; A2 digest changed.
- **Stage:** `--stage s3` rejected as not implemented; future invariant rejected under `--stage s1`.

## 7. Upstream and S1 regression evidence

- A0: 44 tables and 28 sensitive rows unchanged; every data-class inventory row still matches; no table/column/state added.
- A1: 49 operations (`18/21/6/3/1`), 6 security schemes, five audiences, IVR/provider header names and lengths unchanged; no operation/schema/field/Problem code/role added.
- A2: 12 messages, 11 event types, 22 identities, 15 invariants, 25 fixtures and digest unchanged; no A2 state/event/digest/classification added. The 401/403 outbox policy is local security metadata only.
- S1: six fixture hashes unchanged; S1 model sections and `INV-A3-01..06` unchanged; two S1-stage runs on cumulative files reproduce the accepted stdout hash `3E991F...`; the historical S1 checkpoint files also validate.

## 8. Documentation decision

- `docs/17-production-security-design-spec.md` remains the single human canonical A3 specification; `security/v1/security-design-v1.json` remains its only machine projection. No README, second security document, JSON Schema copy or A1/A2 modification was created.
- S1 sections and marker blocks are preserved; S2 adds trust surface/domain, trust boundary, service identity, credential, signature/nonce and outbox-policy material plus the cumulative invariant table and S2 validation/fixtures. `docs/16` remains the A2 authority; this document does not restate its state machine.
- Numeric Q1/Q6/Q7/Q8/Q12/Q13/Q15/Q17/Q19/Q22/Q23/Q25/Q27/Q28 and CV-1..5 inputs remain typed deferred with unchanged owner/destination; no production value was selected.

## 9. Residuals and uncovered areas

| ID / area | Remaining gap | Owner / destination |
| --- | --- | --- |
| `R-A3-A2-AUTH-OUTBOX` | 401/403 `PENDING`/security-blocked policy is frozen, but Q8/Q13/Q25 retry/backoff/alert numbers and real Redis/IVR integration remain unverified | IVR/Backend; A2/IVR integration review |
| `R-A3-MTLS-CAPABILITY` | Real mTLS certificate issuance, pinning, clock synchronization and carrier identity are unverified (Q7/Q15) | Telephony/IVR/Security; line integration |
| `R-A3-PROVIDER-VERIFY` | Vendor signature algorithm, canonicalization, payload fields and window remain Q6/Q15 | Payments/Backend; payment adapter |
| `R-A3-SESSION-LIFECYCLE` | Session/OTP/WeChat login, employee MFA and TTLs remain Q1/Q19 | Identity/Backend; authentication design |
| `R-A3-NONCE-STORE` | Real Redis `SET NX` atomicity, TTL and failover behavior are unverified (Q25) | Security/Backend; production readiness |
| `R-A3-HMAC-ROTATION-REPLAY` | S1 HMAC rotation/A2 fingerprint runtime compatibility remains | Security/IVR/Backend; Q3/Q25 |
| `R-A3-KMS-CAPABILITY`, `R-A3-COS-CAPABILITY`, `R-A3-KEY-DESTRUCTION`, `R-A3-LEGAL-RETENTION`, `R-A3-NO-WORM`, `R-A3-REGION-SKU` | Unchanged S1 capability/retention residuals | Security/Infrastructure/Legal as stated in `docs/17` §17 |
| S3 scope | Media upload/playback credentials, COS private policy, audit access, high-risk controls and deterministic redaction are not implemented | S3 implementation gate |

## 10. Static and no-runtime claim

No Java/runtime service, cloud resource, real credential, KMS/Secret Manager/CAM/COS call, Redis/PostgreSQL call, IVR line, log collector, analytics system or payment provider was implemented or contacted. The fixtures and machine are static security-model assertions, not production acceptance or penetration testing. The validator derives key bytes deterministically from `TEST_ONLY_*` labels and uses fixed evaluator nonces; it does not claim production CSPRNG behavior.

## 11. Completion status

S2 is **author-complete** at `/mnt/d/codex-call` on branch `codex/a3-production-security-design-spec`. Exactly the allowed paths were written. No S1 fixture, S1/A0/A1/A2 accepted contract, controller, accepted plan/review/fix, OpenAPI/protocol source, prototype, screenshot or shared business logic changed. No staging, commit, push, PR, merge, branch change, controller update, self-review, code review or S3 work was performed.

Controller can verify the artifacts by recomputing the SHA-256 values in §2, running the exact commands in §5, and replaying the probe categories in §6 against temporary copies.
