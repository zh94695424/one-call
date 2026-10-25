# A3 S3 implementation report — media, COS, audit, high-risk and redaction

## 1. Task and base

- Work unit: `A3-production-security-design-spec`
- Slice: S3 — short-lived upload/playback media authority, Tencent COS private-object policy, 39 audit/access events with explicit persistence routes, 10 high-risk employee operations, 10 call-class coverage rows, deterministic pre-emit redaction, fail-closed matrix and cumulative `INV-A3-01..21`.
- Task label: `a3-security-s3-impl-ds-20261026-01`
- Runtime/provider: Claude Code / `ds` / DeepSeek session model `deepseek-flash[1m]`
- Gate: `implementation` only.
- Base commit: `b20be2edfa576e4e27bdf94e067335df25e9cf88` (`gateflow: accept A3-production-security-design-spec S2`).
- Status: implementation author-complete; executable author validation is permission-blocked in this session. This is **not** a checkpoint or review entry.

## 2. Exact changed and created paths

Modified only:

1. `docs/17-production-security-design-spec.md` — SHA-256 `f4341a77344183ed571e7a7000899e352918ad8613676d182e1d976773405cfb`
2. `security/v1/security-design-v1.json` — SHA-256 `0b8d86fea7e793238cc60413ec2e6c9e509c0f4a0b24034e1300b8184dfaffdf`
3. `security/validate.mjs` — SHA-256 `d838482cf852e94fa0b534097cfd459fd58b41974623aa814442a9c2cd6743ec`

Created exactly eight fixtures:

| Fixture | SHA-256 | Negative |
| --- | --- | --- |
| `security/v1/fixtures/sec-s3-01-upload-credential-scope.json` | `8692b021036344ab38c92c3bfd3918072e5fca1e938564312c116c2bd1a3df52` | no |
| `security/v1/fixtures/sec-s3-02-playback-credential-scope.json` | `888515aa3d85116772ac0309a5a02af6cff3fccbb2ab31dd86ef24a643b23a32` | yes |
| `security/v1/fixtures/sec-s3-03-cos-private-policy.json` | `1500d8e740c21c36d6f7262b9b0fd0d93fc7604c5d9e62f6c28620fdf5300759` | no |
| `security/v1/fixtures/sec-s3-04-audit-access-denied.json` | `44d37893d95ac702b997d3b15528e9233a82ad03df73858f3350439080e263dc` | yes |
| `security/v1/fixtures/sec-s3-05-high-risk-controls.json` | `ea7e1fe7cac5848d16a3357494e82713aef0e59d82485904a9930f5ab34ee2b5` | no |
| `security/v1/fixtures/sec-s3-06-redaction-deterministic.json` | `d5d4ef5059814ff3af5e538a2303fcf86f5c82382d09a773966251a6121ead50` | no |
| `security/v1/fixtures/sec-s3-07-credential-url-leak.json` | `cd05587b03875b1ceb78e118e4a61f088fbc4ab5390d894899859ab58a00bfc1` | yes |
| `security/v1/fixtures/sec-s3-08-end-to-end-security.json` | `6ac683a1541f38869a6338045dbc40d8a69506175e41aa38c9f506180275fa42` | no |

Created this report:

- `docs/gateflow/a3-production-security-design-spec-s3-implementation.md`

No other path was intentionally changed. The controller, accepted plan, A0/A1/A2 sources, accepted S1/S2 fixtures, OpenAPI/protocol files, prototypes, screenshots, `shared/onecall-business.js` and historical review were not modified. Pre-existing dirty prototype/screenshot paths and `docs/reviews/plan-review-20261021-205118.md` remain untouched. The scoped check confirms no modifications under `docs/14`, `docs/15`, `docs/16`, `api/openapi`, `protocol/ivr/v1` or accepted S1/S2 fixture files.

## 3. Registry, marker and fixture counts

- `mediaPolicies=2` (`U1` upload, `P1` playback)
- `auditEvents=39`; `persistence=39`; `access_event=2` (`AUDIT_READ`, `AUDIT_READ_DENIED`); `append_fact=37`
- `highRiskActions=10`
- `auditCoverage=10`
- `failClosedMatrix=14` rows
- `cosPolicy` = one frozen object
- `redactionPolicy` = one frozen object with 30 deny-list keys and 28 token classes
- `invariants=21` (`INV-A3-01..21`; S1 6, S2 +5, S3 +10)
- S3 fixtures `8`, explicit S3 negatives `3`; cumulative fixtures `23` (`6+9+8`), cumulative negatives `10` (`2+7+3`)
- Preserved S1/S2 registries: `keyClasses=20`, `dataClasses=28`, `deferredInputs=23` (`18 Q + 5 CV`), `trustSurfaces=5`, `authDomains=5`, `trustBoundaries=7`, `serviceIdentities=6`, `credentialClasses=10`, `signatureProfiles=2`
- Marker blocks: existing `security-key-map`, `security-data-map`, `security-deferred-map`, `security-domain-map`, `security-service-map`, `security-credential-map`; new `security-media-map` (2), `security-audit-map` (39), `security-high-risk-map` (10), `security-coverage-map` (10), `security-failclosed-map` (14)

## 4. Security decisions

1. **Upload U1.** The only primary shape is a short-lived, single-object Tencent COS presigned PUT credential. Issuance is gated on CV-4 verifying method/object/expiry and supported size restrictions; otherwise issuance fails closed and implementation stops. No custom HMAC token, signing-gateway fallback, public URL, object-key contract, fixed TTL, region, SKU, WORM or object-lock capability is invented. `onecall:evidence-upload:v1` remains an issuance-side policy classification, not a COS audience claim. Completion re-observes object identity, SHA-256, size, MIME and duration and never solidifies evidence.
2. **Playback P1.** C10 is an opaque short-lived bearer presented as `Authorization: Bearer <credential>` to the authenticated media-gateway data plane. The bearer alone resolves share/admission/generation/object/purpose; the client supplies no COS key/bucket/version or public resource identifier. Playback grants no write/list/delete. No gateway URL/route is asserted; transport topology remains `R-A3-MEDIA-GATEWAY-TRANSPORT`.
3. **COS.** Bucket remains private, non-anonymous, TLS-only, public-access-blocked, SSE-KMS under K10 and versioned. Raw/derived/transcript/summary/export classes are separated and derived objects never overwrite raw objects. Region, storage class, retention, object lock/WORM, private endpoint and delivery path are typed deferred Q11/CV-4; the document explicitly does not claim them.
4. **Audit.** PostgreSQL `audit.audit_fact` remains the source of truth. `AUDIT_READ` and `AUDIT_READ_DENIED` are `access_event` rows and never recursively call `audit.append_fact`; the other 37 rows are `append_fact`. Reads are employee-only (fixed ADMIN/OPS) and project only status/revision/reasonCode.
5. **High-risk operations.** Exactly ten employee mutations; only ADMIN/OPS; role, state, expected revision, reason source and audit are checked, with domain mutation plus audit in one transaction and full rollback on audit failure. `ACTION_DERIVED` is only the A0 `reason_code` technical projection. UI second confirmation remains runtime-unverified and is not asserted as implemented.
6. **Redaction.** The pipeline runs allow-list → recursive deny-list at any depth → credential/header/URL pattern controls → destination policy → emit. Tokens are the fixed `[REDACTED:<CLASS>]` form. Unknown fields or failures block/drop and alert; nothing is emitted before redaction. Support views expose only masked/redacted/internal ID/status and never playback.
7. **Fail closed.** KMS/Secret Manager/Redis nonce/COS/session/HMAC/audit/redaction/MFA/provider dependency failures use existing A1 Problem codes and produce no plaintext, credential, evidence, media fact, redial, reservation, settlement or duplicate A2 side effect. General database unavailability is covered by the `DATABASE_ACCESS` coverage row and the audit-append/transaction-rollback matrix row; no extra row or code was invented.

## 5. Validation ledger

**Important limitation.** This session's environment denied `node`, `npm` and interpreter execution with the exact permission response `This command requires approval`. Per the environment gate, the author did not retry or bypass it. Consequently the executable ledger below is **NOT RUN** by the author; the controller must run it and is the authoritative executable evidence. No output in this section is represented as observed validator output.

| # | Command | Author result |
| --- | --- | --- |
| 1 | `node --check security/validate.mjs` | NOT RUN — permission denied (`This command requires approval`) |
| 2 | `cd api/openapi && npm run check:s3` | NOT RUN — permission denied |
| 3 | `node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md` | NOT RUN — permission denied |
| 4 | `node security/validate.mjs --stage s1 ... --stage s2 ... --stage s3 ...` with model `security/v1/security-design-v1.json`, fixtures `security/v1/fixtures`, spec `docs/17-production-security-design-spec.md`, A1 bundle, A0 spec, A2 model and A2 fixtures | NOT RUN — permission denied |
| 5 | Two identical S3 runs and stdout SHA-256 comparison | NOT RUN — permission denied |
| 6 | Focused mutation probes under temporary files outside the repository | NOT RUN — `node` and `/tmp` writes denied |
| 7 | Static encoding/whitespace/scope checks | RUN |

Static observations that did run:

- `file -bi docs/17-production-security-design-spec.md security/v1/security-design-v1.json security/validate.mjs` → `text/plain; charset=utf-8`, `application/json; charset=utf-8`, `text/plain; charset=utf-8`.
- `git diff --check -- docs/17-production-security-design-spec.md security` → exit `0`, no output.
- Grep conflict-marker scan over the document and `security/` → no `<<<<<<<`/`=======`/`>>>>>>>` markers.
- Grep trailing-whitespace scan over the document and `security/` → 0 matches.
- Grep URL/`sec-s4-`/unapproved-invariant scan over `docs/17` and `security/v1` → 0 matches. The only matches of those patterns are the intended validator regex literals in `security/validate.mjs`.
- Grep count checks on the model → `"policyId"=2`, `"persistence"=39`, `"persistence": "access_event"=2`, `"persistence": "append_fact"=37`, `"operationId"=10`, `"callClass"=10`, `"failureId"=14`, `"id": "INV-A3-*"=21`.
- Scoped `git status` shows only the three modified core files, the eight new S3 fixtures, this report, and the pre-existing unrelated dirty paths; no A0/A1/A2, accepted S1/S2 fixture, controller or plan content was changed by this task.

Expected S3 stdout when the controller runs the validator (from the accepted plan, not observed here):

```text
stage=s3 trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 signature_profiles=2 media_policies=2 audit_events=39 audit_persistence=39 audit_access_events=2 audit_append_facts=37 high_risk_actions=10 audit_coverage=10 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=21 fixtures=23 passed=23 failed=0 negative=10 slice_fixtures=6+9+8
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s3)
```

## 6. Mutation evidence

Executable mutation evidence is **not available** in this session for the reason above. The controller must run the following probes against temporary copies outside the repository and confirm each is rejected:

1. Remove one S3 audit row, add a 40th row, or alter an actor/trigger/persistence cell → `model.auditEvents` deep-equality, count 39, or marker `doc_missing/doc_extra` failure.
2. Change `AUDIT_READ` or `AUDIT_READ_DENIED` to `append_fact`, or change another row to `access_event` → structural 2/37 persistence failure and marker drift.
3. Remove or rename an S3 section (`mediaPolicies`, `cosPolicy`, `auditEvents`, `highRiskActions`, `auditCoverage`, `redactionPolicy`, `failClosedMatrix`) → required-section failure.
4. Add an unapproved S3 invariant/fixture/section or an `INV-A3-22+` token → `stage_field_premature` failure.
5. Escalate upload to read/list/delete, cross-object, cross-purpose, expired, or oversize → S3-01 denial branches.
6. Escalate playback to write, cross-object, cross-generation, expired, revoked, or client-supplied object key/bucket/version → S3-02 denial branches.
7. Expose object key/bucket/version/permanent URL or claim WORM/object-lock/region/SKU → COS marker/model or S3-03 denial.
8. Recursive/nested deny-list removal, masked-key blocking, unknown-field non-blocking, or non-deterministic token → S3-06 explicit redacted-event/token assertions.
9. Audit read from a non-employee domain, UPDATE/DELETE/TRUNCATE, raw snapshot, or recursive `audit.append_fact` → S3-04 denial and zero-recursion assertions.
10. High-risk wrong role/state/reason/revision or audit failure without rollback → S3-05 assertions (`ROLLED_BACK`, `domainMutationRolledBack=true`, `partialWrite=false`).
11. Fail-closed dependency with plaintext fallback, credential issuance, evidence solidification, redial, reservation/settlement or duplicate A2 side effect → S3-08 dependency assertions.
12. Remove a fixture, add an extra fixture, rename an S3 fixture, or mark an S3 negative incorrectly → fixture ID/count/negative failures.

The static model/validator already rejects the corresponding model, marker, count, section, invariant and fixture-shape mutations by construction; only runtime confirmation remains controller-owned.

## 7. Upstream drift evidence

- A0 sources are unchanged; the accepted `44` tables and `28` sensitive rows remain the model's upstream constants and the document's read-only authority.
- A1 sources are unchanged; the accepted `49` operations and `6` security schemes remain the model's upstream constants. The A1 `check:s3` run is controller-owned and NOT RUN here.
- A2 sources are unchanged; the accepted `12` messages, `11` event types, `22` identities, `15` invariants and digest `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` remain the model's upstream constants. The A2 validator is controller-owned and NOT RUN here.
- Expected drift evidence after the controller run: `a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0`.
- No new table, column, state, endpoint, header, Problem code, employee role or production numeric/policy value was added.

## 8. Residual risks, owners and destinations

| Risk | Owner / destination |
| --- | --- |
| `R-A3-A1-EMPLOYEE-REASON-PROJECTION` — `ACTION_DERIVED` is a technical projection, not employee motive; UI second confirmation is runtime-unverified | Product/Operations + API governance; future A1/UI refinement |
| `R-A3-A1-UPLOAD-CREDENTIAL-SHAPE` — CV-4-gated COS presigned PUT; unverified capability fails closed with no fallback | Security/Backend/Evidence; API/evidence review |
| `R-A3-MEDIA-GATEWAY-TRANSPORT` — gateway origin/path, CDN/LB, range/streaming and topology remain deployment inputs | Infrastructure/Security; media gateway deployment/storage capacity |
| `R-A3-COS-CAPABILITY` — region/private endpoint/SSE-KMS/versioning/presign/object-lock/WORM unverified | Infrastructure/Security; storage/capacity |
| `R-A3-NO-WORM` — no COS WORM/object-lock claim | Infrastructure/Security; storage policy |
| `R-A3-REDACTION-RUNTIME` — edge/gateway/analytics/log collector configuration unverified | Platform/Security/Observability; logging review |
| `R-A3-AUDIT-VOLUME` — audit/access volume, retention and DLQ undefined (Q13) | Operations/Backend; runbook/observability |
| `R-A3-SUPPORT-VIEW` — no operations playback; future break-glass outside A3 | Product/Security/Operations; future work unit |
| `R-A3-REGION-SKU` — no region/SKU/price/capacity selection | Infrastructure/Security; capacity/deployment |
| `R-A3-LEGAL-RETENTION` — retention/erasure/backup undefined (Q3) | Legal/Security; launch review |
| `R-A3-KEY-DESTRUCTION`, `R-A3-NONCE-STORE`, `R-A3-MTLS-CAPABILITY`, `R-A3-PROVIDER-VERIFY`, `R-A3-SESSION-LIFECYCLE` | Existing S1/S2 owners and destinations unchanged |

## 9. Real-cloud and runtime limitations

This task did not provision or call Tencent KMS, Secret Manager, CAM, COS, Redis, PostgreSQL, IVR lines, media gateway, log platform, analytics or any payment provider. It did not create real credentials, upload/download media, execute a presigned URL, verify SSE-KMS or versioning, test a real WAF/TLS path, run a real database transaction/rollback or demonstrate runtime redaction at an edge collector. The fixtures are security-model assertions, not production acceptance or penetration tests. No WORM, object-lock, region, SKU, capacity, TTL, retention or KDF value is claimed.

## 10. Completion statement

- The S3 implementation, machine model, canonical document, validator extensions and exactly eight S3 fixtures are authored within the allowed paths.
- The controller, plan, A0/A1/A2 sources, accepted S1/S2 fixtures, prototypes, screenshots, shared business logic and Git state were not modified.
- No stage, commit, push, PR creation/modification, merge or review/checkpoint entry was performed.
- Because the environment denied `node`/`npm` execution, the required executable validator runs, two-run hash comparison and mutation probes remain **controller-owned and NOT RUN**. This implementation is therefore author-complete but **not checkpoint-ready**; the next action must be controller validation of the exact hashes above, followed by the independent code review gate if and only if that validation passes.
