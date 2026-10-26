# A5 S2 implementation report (STATUS: PASS, author-complete, review-fix 1)

- Task label: `a5-production-acceptance-matrix-s2-implementation-ds-20261026-01`
- Fix task label: `a5-production-acceptance-matrix-s2-review-fix1-ds-20261026-01`
- Provider/model: DeepSeek (`ds`) / `deepseek-flash[1m]`
- Work unit / gate: `A5-production-acceptance-matrix-spec` / `S2 implementation + bounded review-fix 1`
- Base commit: accepted S1 checkpoint `b954e6ea9ecc3585d761c334e2581e9fbc172512`
- Accepted plan checkpoint: `bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058`
- Slice ID: `S2` — execution evidence admission, aggregate derivation and repository regression wiring.
- Result: `PASS acceptance-validation (s2)`, exit `0`, byte-identical twice; S1 unchanged
  (`PASS acceptance-validation (s1)`, exit `0`, byte-identical to the accepted S1 output).

## Changed paths (cumulative S2 inventory)

The original S2 implementation used six allowlisted paths. Review-fix 1 modified exactly four
of them (paths 2-4 below plus this report); the model and `docs/README.md` are byte-identical to
the S2 implementation under review.

| # | Path | Lines | SHA-256 |
| --- | --- | ---: | --- |
| 1 | `acceptance/v1/acceptance-matrix-v1.json` | 28272 | `fcb0480d332fc827bcb2b97a32eed28802b931a196efb0ed16be14b7511cd1ce` |
| 2 | `acceptance/validate.mjs` | 3808 | `b7287836f5f62f307e25aaa0bc625ff9d9cb6bf73d56a53697b6e15b76a17d7e` |
| 3 | `acceptance/fixtures/s2-01-evidence-admission.json` | 3169 | `a155930c2f698fe0c4e54b564908948396548ee86f08cd4889d3bbd46653bfbf` |
| 4 | `docs/19-production-acceptance-matrix.md` | 474 | `ce67f1834f547d0b85a6fe3465f7df6cc0c214dc88dc0a4389f90ff252d7c068` |
| 5 | `docs/README.md` | 152 | `314d757f102eb3c74e5436ec0e85def78e9882f034f63e5d8492af8ae9408bac` |
| 6 | `docs/gateflow/a5-production-acceptance-matrix-spec-s2-implementation.md` | this report | self-hash omitted by definition |

The model diff is purely additive (102 added lines at the root: `evidencePolicy`, empty
`authorityRegistry`). The docs/README diff is exactly one added navigation row. S1 fixtures,
S1 report, plan/reviews/control, A0-A4 sources and prototypes were not modified.

## Evidence contract and admission decisions implemented

Model additive contract:

- `evidencePolicy` (`A5-EVIDENCE-POLICY-V1`): evidence classes
  `STATIC_SPEC|MODEL_FIXTURE|SYNTHETIC|RUNTIME`; spec-only tiers `STATIC_SPEC|MODEL_FIXTURE`;
  runtime tiers `INTEGRATION|LINE|CLOUD|MANUAL`; the nine frozen artifact roles; the closed
  artifact/manifest/record/authority field lists; `maximumAdmittedState=EVIDENCE_VALID`;
  `unreachableResidual=A5-RES-02`. Validator-owned constants compare these arrays exactly, so a
  coordinated model/doc downgrade cannot relax admission.
- `authorityRegistry` is exactly `[]`. This is the controller-accepted OQ-03 decision. Any
  non-empty row is rejected with `A5_ACCEPTANCE_AUTHORITY_INVALID`; no authority, approver,
  production environment or policy value was invented.

EvidenceArtifact (`evidenceRef`): `artifactId`, closed `role`, opaque non-secret `locatorRef`,
lowercase 64-hex `sha256`, `capturedAt` inside `[executedAt, recordedAt]`, `producerRef`,
`redactionRef`. URL/query/COS key/bucket/version/token/bearer/credential locators are rejected.
`producerRef`/`redactionRef` must match the accepted source-ref shape and resolve through the
existing resolver; free text, an unrecognized prefix or an unresolved ref is
`A5_EVIDENCE_REF_INVALID`.

EvidenceManifest: `manifestId`, `scenarioId`, `evidenceOwner`, `evidenceClass`, `evidenceTiers`,
`executedAt`, `recordedAt`, `artifacts[]`, `observations[]`, `manifestDigest`.
`manifestDigest` is SHA-256 of recursively sorted-key canonical JSON excluding itself.
Caller fields `result`/`pass`/`productionAccepted`/`execution` are rejected
(`A5_CALLER_CONTROL_FORBIDDEN`); `accepted` is rejected as caller-asserted PASS
(`A5_CALLER_ASSERTED_PASS`).

AcceptanceRecord: `recordId`, `scenarioId`, `manifestId`, `manifestDigest`, `authorityRef`,
`acceptedAt`, `result∈{PASS,FAIL}`. It must reference an admitted manifest and its exact digest.
A PASS additionally requires an approved authority; with the empty registry no PASS is reachable.
A FAIL record rejects acceptance but grants nothing. Records are terminal and immutable.

Admission rules implemented (all deterministic; no network, env, wall clock or randomness;
explicit ledger `asOf` only):

- caller/synthetic/tier: synthetic runtime rejected; `STATIC_SPEC`/`MODEL_FIXTURE` never satisfy
  runtime tiers; missing required tier => `A5_EVIDENCE_TIER_INSUFFICIENT` and BLOCKED.
- owner: manifest evidenceOwner must equal the scenario evidenceOwner; absent/`null` is
  `A5_EVIDENCE_OWNER_MISSING`, while any present non-string or unequal string is
  `A5_EVIDENCE_OWNER_MISMATCH` (no type bypass); no static delegation. `manifestId`/`scenarioId`
  are closed non-empty strings.
- artifact: required role coverage derived from expected persisted/audit/outbox/metric/log/access
  evidence plus LINE/CLOUD/MANUAL tiers; missing role => `A5_EVIDENCE_ARTIFACT_MISSING`; malformed
  artifact/role/locator/sha256 => `A5_EVIDENCE_REF_INVALID`.
- digest: artifact `sha256` shape and manifest canonical digest; mismatch =>
  `A5_EVIDENCE_DIGEST_MISMATCH`.
- time: UTC `Z` format, `executedAt <= recordedAt <= asOf`, captured window; future/ordering =>
  `A5_EVIDENCE_TIME_INVALID`.
- state contradiction: an observation `(targetRef, field)` must belong to the scenario's step
  `expectedState` or final `persisted` assertions, use an allowed mutable state and agree with the
  final projection/terminal rules; an out-of-scope aggregate claim, observed ENDED→ACTIVE or a
  conflicting final value => `A5_EVIDENCE_STATE_CONTRADICTION`.
- authority/evidence: missing manifest => `A5_ACCEPTANCE_EVIDENCE_MISSING`; no approved authority
  => `A5_ACCEPTANCE_AUTHORITY_MISSING`; non-empty registry/synthetic authority =>
  `A5_ACCEPTANCE_AUTHORITY_INVALID`.
- immutability/replay: same manifestId with different digest/content, record digest mismatch,
  `record.scenarioId` differing from the referenced manifest, a reused `recordId` with a distinct
  body, or conflicting PASS/FAIL records => `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION`; identical
  manifest replay is admitted exactly once.
- terminal FAIL: a structurally valid FAIL record transitions the admitted manifest/scenario to
  terminal FAIL; aggregate precedence is rejected → blocked → FAIL → all-admitted
  `EVIDENCE_VALID` → `EVIDENCE_SUBMITTED`. No public counter is added.
- count-only: model root `accepted`/`productionAccepted` remains caller control; 96 cells/90
  scenarios never substitute for evidence.
- stage ordering: s1 rejects `--evidence`/`--check-doc-links` as invalid arguments; s2 accepts
  optional `--evidence` and `--check-doc-links`; absent evidence yields zero counters and
  `runtime_evidence=NOT_RUN`.

## Fixture inventory and exact-code coverage

`acceptance/fixtures/s2-01-evidence-admission.json` (`kind: EVIDENCE_MANIFEST`, 42 focused cases;
exact primary-code sets and optional counter expectations):

| Group | Cases | Expected primary code(s) |
| --- | --- | --- |
| caller | `caller-forged-pass`, `caller-accepted` | `A5_CALLER_CONTROL_FORBIDDEN`, `A5_CALLER_ASSERTED_PASS` |
| synthetic/tier | `synthetic-runtime`, `model-fixture-as-runtime`, `missing-line-tier` | `A5_SYNTHETIC_EVIDENCE_REJECTED`, `A5_EVIDENCE_TIER_INSUFFICIENT` |
| artifact/ref/digest/time | `bad-artifact-digest`, `digest-mismatch`, `future-timestamp`, `missing-artifact`, `invalid-locator-ref`, `missing-artifact-field`, `time-order`; fix-1 adds `locator-bucket`, `locator-version`, `locator-bare-token`, `locator-bearer`, `producerref-freeform`, `producerref-unrecognized-prefix`, `producerref-unresolved`, `redactionref-freeform` | `A5_EVIDENCE_REF_INVALID`, `A5_EVIDENCE_DIGEST_MISMATCH`, `A5_EVIDENCE_TIME_INVALID`, `A5_EVIDENCE_ARTIFACT_MISSING` |
| owner/state | `owner-mismatch`, `manifest-missing-owner`, `state-contradiction`, `manifest-invalid-shape`; fix-1 adds `owner-non-string-number`, `owner-non-string-object`, `observation-out-of-scope` | `A5_EVIDENCE_OWNER_MISMATCH`, `A5_EVIDENCE_OWNER_MISSING`, `A5_EVIDENCE_STATE_CONTRADICTION`, `A5_EVIDENCE_MANIFEST_INVALID` |
| authority/evidence | `no-authority`, `synthetic-authority`, `authority-row-without-basis`, `accept-without-evidence` | `A5_ACCEPTANCE_AUTHORITY_MISSING`, `A5_ACCEPTANCE_AUTHORITY_INVALID`, `A5_ACCEPTANCE_EVIDENCE_MISSING` |
| immutability/replay | `manifest-immutability`, `record-immutability`, `record-digest-mismatch`, `manifest-replay-same-digest`; fix-1 adds `record-scenario-mismatch`, `record-scenario-mismatch-pass`, `record-id-duplicate` | `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION`; replay exits 0 with `admitted=1` |
| count-only | `count-only-acceptance` | `A5_ACCEPTANCE_EVIDENCE_MISSING` + `A5_CALLER_CONTROL_FORBIDDEN` |
| terminal FAIL | `fail-record-state` | exit 0; `admitted=1`; `runtimeEvidence=FAIL`; `productionAccepted=false` |
| non-PASS positives | `positive-model-evidence` (fixture-only in-memory `[MODEL_FIXTURE]` profile), `positive-runtime-evidence` | exit 0; `admitted=1`; `productionAccepted=false` |

The MODEL_FIXTURE positive control uses a fixture-only in-memory scenario profile so it can prove
manifest admissibility/`EVIDENCE_VALID` while the real matrix keeps its runtime tier minimums and
the empty authority registry. It does not mutate the model or produce PASS.

CLI probes (separate from the fixture harness):

| Probe | Result |
| --- | --- |
| valid RUNTIME IDENT ledger | exit 0; `evidence_manifests=1 evidence_admitted=1 acceptance_records=0 blocked=0 runtime_evidence=EVIDENCE_SUBMITTED production_accepted=false` |
| same ledger + PASS acceptance record with authorityRef | exit 1; `ISSUE A5_ACCEPTANCE_AUTHORITY_MISSING ...` |
| synthetic runtime ledger | exit 1; `ISSUE A5_SYNTHETIC_EVIDENCE_REJECTED ...` |
| s1 + `--evidence` / s2 missing evidence file / s1 + `--check-doc-links` | exit 2 invalid-arguments |

## Review-fix 1 (bounded findings F-S2-01..F-S2-07)

- Fix task label: `a5-production-acceptance-matrix-s2-review-fix1-ds-20261026-01`
- Review artifact: `docs/reviews/code-review-20261026-104337.md`
  (SHA-256 `4144A3AFD9072A73BBDE3728CE08172539D4B9F47E17E3E6E9223D9E795CAA43`)
- Scope: exactly `acceptance/validate.mjs`, `acceptance/fixtures/s2-01-evidence-admission.json`,
  `docs/19-production-acceptance-matrix.md` and this report. Model, docs README, S1
  fixtures/report, plan/reviews/control, A0-A4 sources and prototypes were not modified.
- No review, control edit, staging, commit, push, PR/issue or later gate was performed.

Finding-by-finding disposition:

| Finding | Disposition |
| --- | --- |
| `F-S2-01` owner typing | `evidenceOwner` absent/`null` keeps `A5_EVIDENCE_OWNER_MISSING`; any present non-string or unequal string now raises `A5_EVIDENCE_OWNER_MISMATCH`. Fixture `owner-non-string-number`, `owner-non-string-object`. |
| `F-S2-02` record identity | Manifest `manifestId`/`scenarioId` and record `recordId`/`scenarioId`/`manifestId` are closed non-empty strings; `record.scenarioId` must equal the referenced manifest's `scenarioId`; a reused `recordId` with a distinct body is an immutability violation. Existing per-scenario terminal conflict checks remain. Fixture `record-scenario-mismatch`, `record-scenario-mismatch-pass`, `record-id-duplicate` (two manifests admitted, only the reused recordId conflicts). |
| `F-S2-03` FAIL terminal state | A structurally valid FAIL record sets the admitted manifest/scenario state to terminal `FAIL`; `runtime_evidence` precedence is rejected → blocked → FAIL → all-admitted `EVIDENCE_VALID` → `EVIDENCE_SUBMITTED`. No public counter added. Fixture `fail-record-state` (`runtimeEvidence=FAIL`); CLI 90-manifest probe confirms all-admitted + FAIL ⇒ `runtime_evidence=FAIL`, `production_accepted=false`. |
| `F-S2-04` observation scope | Observation `(targetRef, field)` must be owned by the scenario's step `expectedState` or final `persisted` assertions; out-of-scope claims raise `A5_EVIDENCE_STATE_CONTRADICTION`. Fixture `observation-out-of-scope` (Share grant `REVOKED`). |
| `F-S2-05` locator blacklist | Added lowercase `bucket`, `version`, `token` (subsumes `token=`) and `bearer` to the existing fail-closed substring inspection. Fixture `locator-bucket`, `locator-version`, `locator-bare-token`, `locator-bearer`. |
| `F-S2-06` provenance refs | `producerRef`/`redactionRef` must match the accepted source-ref shape and resolve through `resolveRef`; free text, unknown prefix or unresolved ref ⇒ `A5_EVIDENCE_REF_INVALID`. Fixture `producerref-freeform`, `producerref-unrecognized-prefix`, `producerref-unresolved`, `redactionref-freeform`. |
| `F-S2-07` report bookkeeping | Corrected base commit `b954e6ea9ecc3585d761c334e2581e9fbc172512` and accepted plan checkpoint `bc6ab32a3f0fe3fec0d3b8504b45d8bf0184a058`; both verified with `git cat-file -t` = `commit`. |

Fix-run validation (repository root, Node v22.23.4):

| Command | Exit | Result |
| --- | ---: | --- |
| `node --check acceptance/validate.mjs` | 0 | syntax OK |
| s1 plan-§10.2 command ×2 | 0, 0 | byte-identical stdout, SHA-256 `085752aebfe43c33837fac5f13cf840f7e75d5466b5270c33e4cca8063751280` (unchanged), empty stderr |
| s2 plan-§10.2 command ×2 | 0, 0 | byte-identical stdout, SHA-256 `810029be650c4b6e06694ecf26389eff7e6d90e237dd50bd3285c6e26536dba9` (unchanged), `runtime_evidence=NOT_RUN production_accepted=false` |
| s2 `--check-doc-links` | 0 | same stdout hash; docs/19 links and README row resolve |
| A1 `npm run check:s3` | 0 | `PASS check-doc-map (s3)` total=49, a0_catalog=44 |
| A2 stage s3 | 0 | `PASS protocol-validation (s3)` fixtures=25 passed=25 failed=0 negative=3, registry digest `c6b769d4…f4e9` |
| A3 stage s3 | 0 | `PASS security-validation (s3)` fixtures=23 passed=23 failed=0 negative=10, drift 0 |
| A4 stage s2 | 0 | `PASS capacity-validation stage=s2` fixtureCases=113 passed=113 failed=0, all readiness flags false |
| 90-manifest CLI ledger (no record) | 0 | `evidence_manifests=90 evidence_admitted=90 runtime_evidence=EVIDENCE_VALID production_accepted=false` |
| 90-manifest CLI ledger + valid FAIL record | 0 | `evidence_manifests=90 evidence_admitted=90 acceptance_records=1 runtime_evidence=FAIL production_accepted=false` |
| 90-manifest CLI ledger + PASS record + authorityRef | 1 | `ISSUE A5_ACCEPTANCE_AUTHORITY_MISSING record.REC-PASS-ALL.authorityRef` (PASS still unreachable) |
| all 42 S2 fixture cases | 0 | exact primary-code sets and counter expectations pass inside the s2 stage |
| per-finding CLI probes (15 cases) | 1 (or 0 for `fail-record-state`) | `owner-*` ⇒ OWNER_MISMATCH; `record-*` ⇒ IMMUTABILITY (recordId / scenarioId); `observation-out-of-scope` ⇒ STATE_CONTRADICTION; `locator-*`/`producerref-*`/`redactionref-*` ⇒ REF_INVALID; `fail-record-state` ⇒ exit 0 `runtime_evidence=FAIL` |

Controller open-question dispositions honored:

- no closed observation-object field registry or recursive caller-key scan was added;
  `F-S2-04` closes the state-ownership defect through the scenario's own assertion pairs;
- `evidence_manifests` remains raw submitted-entry count and `evidence_admitted` unique admitted
  count (identical replay `manifests=2 admitted=1` remains intended);
- no `failed_records` or other public counter was added; terminal FAIL is exposed through
  `runtime_evidence=FAIL`.

Honest limitations of this fix:

- The locator rule is a lowercase substring gate; an opaque path containing `version`, `bucket`,
  `token` or `bearer` as part of an unrelated word is rejected (fail-closed by design, as the
  review accepted). No storage locator scheme was invented.
- `recordId` uniqueness is enforced within a ledger; cross-ledger/immutable-store enforcement
  remains external (`A5-RES-01`/`A5-RES-08`).
- Observation scope is derived from the scenario's own `expectedState`/`persisted` pairs; A5 still
  does not read external evidence bytes or prove that the underlying business state occurred.
- `runtime_evidence=FAIL` is a static derivation from a structurally valid FAIL record; no
  runtime/manual/cloud/line execution was performed or claimed.

## Deterministic outputs and A0-A4 regression evidence

Exact commands (repository root; A1 bundle prerequisite present/generated):

```bash
node --check acceptance/validate.mjs                         # exit 0
node acceptance/validate.mjs --stage s1 ...                  # plan §10.2 full args, twice
node acceptance/validate.mjs --stage s2 ...                  # plan §10.2 full args, twice
node acceptance/validate.mjs --stage s2 ... --check-doc-links
```

- S1 twice: exit `0`, empty stderr, byte-identical stdout, SHA-256
  `085752aebfe43c33837fac5f13cf840f7e75d5466b5270c33e4cca8063751280` — unchanged from the
  accepted S1 checkpoint. Counters remain `units=16 cells=96 represented=90 not_applicable=6
  deferred=0 scenarios=90 source_refs=547 unresolved=0 ambiguous=0 drift=0 a3_residuals=18
  a0_state_rows=31 a0_shape_unrecognized=0 owner_upstream_unresolved=0
  residual_owner_unknown=0 transition_ref_invalid=0 path_family_duplicate=0
  external_projection_contradiction=0 evidence_owner_missing=0 coverage_missing=0
  coverage_duplicate=0 coverage_illegal=0 na_unbacked=0 terminal_mutation=0
  duplicate_side_effect=0 ordering_regression=0 denied_with_business_effect=0 caller_control=0
  synthetic_acceptance=0 runtime_evidence=NOT_RUN production_accepted=false
  spec_coverage=COMPLETE fixtures=3 passed=3 failed=0 negative=3 deterministic=true`.
- S2 twice: exit `0`, empty stderr, byte-identical stdout, SHA-256
  `810029be650c4b6e06694ecf26389eff7e6d90e237dd50bd3285c6e26536dba9`.
- Review-fix 1 re-ran the exact s1/s2/`--check-doc-links` commands and all A1-A4 regressions;
  both A5 output hashes are unchanged (the fixture case count is internal and not a public
  counter).

```text
stage=s2 matrix=A5-PRODUCTION-ACCEPTANCE-MATRIX-V1 axes=6 units=16 cells=96 represented=90 not_applicable=6 deferred=0 scenarios=90 source_refs=547 unresolved=0 ambiguous=0 drift=0 a3_residuals=18 a0_state_rows=31 a0_shape_unrecognized=0 owner_upstream_unresolved=0 residual_owner_unknown=0 transition_ref_invalid=0 path_family_duplicate=0 external_projection_contradiction=0 evidence_owner_missing=0 coverage_missing=0 coverage_duplicate=0 coverage_illegal=0 na_unbacked=0 terminal_mutation=0 duplicate_side_effect=0 ordering_regression=0 denied_with_business_effect=0 caller_control=0 synthetic_acceptance=0 evidence_manifests=0 evidence_admitted=0 acceptance_records=0 blocked=0 runtime_evidence=NOT_RUN production_accepted=false spec_coverage=COMPLETE fixtures=4 passed=4 failed=0 negative=4 deterministic=true
PASS acceptance-validation (s2)
```
- `--check-doc-links`: exit `0`, same stdout hash; docs/19 relative links resolve and
  docs/README has exactly one resolvable docs/19 row.
- A1 `( cd api/openapi && npm run check:s3 )`: exit `0`, `PASS check-doc-map (s3)`, `total=49`,
  `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1`, `a0_catalog=44`.
- A2 stage s3: exit `0`, `PASS protocol-validation (s3)`, `messages=12 event_types=11
  identities=22 fixtures=25 passed=25 failed=0 negative=3`,
  `event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`.
- A3 stage s3: exit `0`, `PASS security-validation (s3)`, `trust_surfaces=5 auth_domains=5
  trust_boundaries=7 credential_classes=10 invariants=21 fixtures=23 passed=23 failed=0
  negative=10`, drift `0`.
- A4 stage s2: exit `0`, `PASS capacity-validation stage=s2`, `groups=9 fixtureCases=113
  passed=113 failed=0`, `productionReady=false procurementReady=false cloudVerified=false
  a3RiskClosed=false`.

## Docs/README/link result

- `docs/19` §9 replaced the S2 placeholder with the frozen evidencePolicy/artifact/manifest/record
  contract, admission table, state/aggregate derivation and fixture inventory; §2/§10/§12 updated
  for s2 and `--check-doc-links`.
- `docs/README.md` adds exactly one row with the plan-frozen text; the validator confirms one
  unique row and a resolvable target.
- `git diff --check` on the S2 allowlist is clean. The only pre-existing whitespace note is
  `docs/README.md` line 3 (Markdown hard break present in HEAD); the file was normalized to the
  repository LF baseline so the S2 diff is one line. All A5 JSON files parse.

## No-runtime / no-production-acceptance statement

No runtime, database, queue, cache, payment adapter, carrier/SIP/IVR line, COS/KMS/CAM cloud,
load, recovery, penetration or manual/legal execution was performed or claimed. A2/A3/A4 fixtures
and A1 static checks remain model/spec evidence, not A5 runtime evidence. The static S2 slice can
at most admit a structurally valid manifest to `EVIDENCE_VALID`/`SPEC_VERIFIED`; with the empty
`authorityRegistry` it never emits `PASS` and never sets `productionAccepted=true`.

## Residual risks (owner / destination) and uncovered areas

| Residual | Owner | Destination |
| --- | --- | --- |
| `A5-RES-02` acceptance authority deliberately absent; PASS unreachable | `Product/Security/Operations` | release governance; future approved external authority |
| `A5-RES-01` immutable evidence store, retention, authorization/WORM not selected | `Infrastructure/Security` | evidence platform + launch governance |
| `A5-RES-08` manifest integrity relies on external authorization/immutable storage; A5 checks references only | `Infrastructure/Security` | evidence platform |
| `A5-RES-03` real integration/line/cloud/manual execution environment absent | `Telephony/IVR`, `Infrastructure/Security`, `Backend/QA` | authorized production execution gate |
| `A5-RES-12` A2/A3/A4 synthetic fixtures may be misread as acceptance evidence | `Backend/QA` | A5 evidence honesty + independent review |
| `A5-RES-07` no CI enforcement; commands are manual | `Platform/CI` | repository CI policy before production merge |

Bounded limitations stated honestly:

- Artifact `sha256` is structurally validated and the manifest digest is recomputed; A5 does not
  read or re-hash the external artifact content, enforce WORM/retention, or prove authorization.
- Observations are bounded state projections, not a full business-state transcript.
- Access-event evidence is covered by the `AUDIT_EXPORT` role (the frozen artifact-role list has
  no separate access-event role).
- The fixture-only `[MODEL_FIXTURE]` profile proves admissibility semantics only; the real matrix
  retains runtime tier minimums and empty authority.
- Uncovered: real DB constraints/locks/transactions, Redis/RabbitMQ/COS/KMS/CAM, payment provider,
  IVR line/B-leg/recording, cross-node callId, real recovery, SLO/RPO/RTO, capacity/cloud
  capability, penetration, production redaction configuration and any runtime behavior.

## Completion status

Review-fix 1 author-complete, awaiting controller revalidation. All seven accepted findings
(`F-S2-01`..`F-S2-07`) are dispositioned; `PASS acceptance-validation (s2)` and
`PASS acceptance-validation (s1)` are reproducible with unchanged hashes; the 42 S2 fixture cases
pass; A1-A4 regressions are green; docs/index/link and scope checks pass. No staging, commit,
push, PR/issue, merge, control update or review was performed.
