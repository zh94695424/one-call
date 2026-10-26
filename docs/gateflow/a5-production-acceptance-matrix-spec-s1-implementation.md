# A5 S1 implementation report (STATUS: PASS)

- Current gate / task: `S1 review fix 1` / `a5-production-acceptance-matrix-s1-review-fix1-ds-20261026-01`
- Initial S1 task label: `a5-production-acceptance-matrix-s1-fix1-ds-20261026-01`
- Work unit / gate: `A5-production-acceptance-matrix-spec` / `S1 implementation` (fix 1)
- Base commit: `e36b350` (accepted plan checkpoint `bc6ab32f12b0301b5e29b23af419d9a4fb9a7ea4`)
- Provider/model: deepseek-flash[1m]
- Result: `PASS acceptance-validation (s1)`, exit `0`, reproducible twice.

## Changed paths (seven allowlisted S1 paths only)

1. `acceptance/validate.mjs`
2. `acceptance/v1/acceptance-matrix-v1.json`
3. `docs/19-production-acceptance-matrix.md`
4. `acceptance/fixtures/s1-01-matrix-integrity.json`
5. `acceptance/fixtures/s1-02-expectation-safety.json`
6. `acceptance/fixtures/s1-03-a0-state-shapes.json`
7. `docs/gateflow/a5-production-acceptance-matrix-spec-s1-implementation.md` (this report)

The transient generator was used only while iterating and has been removed from this report.
The frozen A1 regression command regenerated the gitignored artifacts
`tmp/openapi-client/onecall-v1.bundle.json` and `tmp/openapi-client/onecall-v1.d.ts`; no tracked
path and no A0-A4 contract/source was modified.

## Root causes fixed

Controller corrections preserved: `FROZEN_RESULT` from the A0 Allowed states cell;
`STATE_CELL_ARROW` edge from the Allowed states arrow; generic ordering accepts the union of all
allowed ordering values.

1. **Duplicate refs** — the generator now deduplicates `zeroOrBoundedBasisRefs`, `zeroBasisRefs`,
   `terminalAbsorption.sourceRefs`, cleanup/idempotency source refs and scenario/outcome refs with a
   deterministic `uniq` helper. Duplicate-ref validation was not weakened.
2. **DOC12:S12 category classification** — the resolver split the ref at the wrong boundary
   (`S12#IVR` became part of the prefix). The prefix is now derived from the ref before `#`.
3. **A4/DOC12 heading parsing** — heading recognition required a trailing dot. It now accepts the
   accepted numbered shapes `## 1. ...` and `### 8.3 ...`/`### 17.1 ...` without relaxing the
   number/heading form.
4. **A4 formula refs** — `parseA4` only collected `formulaId` fields and the resolver sliced
   `A4:FORMULA:` one character short. It now also collects `formulaIds[]` from
   `compute(inputs.template.json)`, and resolves with the full prefix length.
   A4 fixture identity now accepts stage-1 `id` and S2 `fixture` (actual accepted sources).
5. **Deployment actor identity** — `deployment/capacity` is non-runtime tooling, not an A3 service
   identity. Deployment steps use the A0 owner module `platform-reliability` as `actorRef`; the
   plan-authorized `scenario.service = deployment/capacity` exception is unchanged and no fake A3
   service was added.
6. **Evidence event refs** — A0 §10.24 evidence lifecycle/recording facts were referenced as
   `A0:OUTBOX:*`. They now use the accepted `A0:EVIDENCE-EVENT:*` kind in source refs and
   Outbox-category effect entries (`EVIDENCE_CREATED`, `EVIDENCE_RECORDING_VALIDATED`,
   `EVIDENCE_RECORDING_QUARANTINED`). No event was added to the §10.17 registry.
7. **`A5-SUBJ-RECOVERY-01`** — the diagnostic ID was the scenario ID; the actual defect was a
   missing verification anchor. Added the source-backed anchor
   `A1:OP:createCustomerSubjectClaim` (already the scenario's operation); no self-reference exists.

Additional S1 causes exposed after the above and fixed inside the approved contract:

8. N/A applicability compared `unit/axis` cell keys against the plan's `unit:axis` registry keys.
9. Path/family uniqueness: removed the path-only playback operation from the family SHARE NORMAL
   source refs so the required source/outcome difference exists (plan §8.3).
10. A0 state table parsing accepts a row without a trailing pipe (actual Call record row with its
    attached note); fixture expected edges are in the plan-mandated `(from,to)` lexicographic order.
11. `transitionRef` values are validated only by the dedicated state-machine phase, so unknown-edge
    and derived-endpoint forms produce `A5_TRANSITION_REF_INVALID` instead of a generic ref error.
12. Terminal absorption now rejects a positive domain write in a `POST_TERMINAL` delivery
    (`A5_TERMINAL_MUTATION`); the fixture mutation now represents the write in the stimulus delta.

No diagnostic was suppressed, no fail-closed check relaxed, no expected primary code changed to
match broken behavior, and no upstream ID was invented.

## Exact validation commands and outputs

Bootstrap (interpreter permission available this session):

```bash
node --version
# v22.23.4
node --check acceptance/validate.mjs
# exit 0
test -f tmp/openapi-client/onecall-v1.bundle.json
# exit 0
```

Frozen S1 command (run twice):

```bash
node acceptance/validate.mjs --stage s1 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures
```

Both runs: exit `0`, byte-identical stdout, sha256
`085752aebfe43c33837fac5f13cf840f7e75d5466b5270c33e4cca8063751280`.

```text
stage=s1 matrix=A5-PRODUCTION-ACCEPTANCE-MATRIX-V1 axes=6 units=16 cells=96 represented=90 not_applicable=6 deferred=0 scenarios=90 source_refs=547 unresolved=0 ambiguous=0 drift=0 a3_residuals=18 a0_state_rows=31 a0_shape_unrecognized=0 owner_upstream_unresolved=0 residual_owner_unknown=0 transition_ref_invalid=0 path_family_duplicate=0 external_projection_contradiction=0 evidence_owner_missing=0 coverage_missing=0 coverage_duplicate=0 coverage_illegal=0 na_unbacked=0 terminal_mutation=0 duplicate_side_effect=0 ordering_regression=0 denied_with_business_effect=0 caller_control=0 synthetic_acceptance=0 runtime_evidence=NOT_RUN production_accepted=false spec_coverage=COMPLETE fixtures=3 passed=3 failed=0 negative=3 deterministic=true
PASS acceptance-validation (s1)
```

## Determinism evidence

Transient generator run twice from the repository root
(`node --input-type=module < docs/gateflow/a5-production-acceptance-matrix-spec-s1-implementation.md`),
outputs byte-identical; generator summary `{"scenarios":90,"coverage":96,"represented":90,"na":6}`.
Stable hashes:

```text
07c2bde1251b3d18bde667b904a1674358c3ebe664cbe0a4cb39094e9f86b055  acceptance/v1/acceptance-matrix-v1.json
985a048dd05974c1e0f608f8e90799699320499ef44a2de1a0ef12884700eda2  docs/19-production-acceptance-matrix.md
5a30efe22d13e92037223c6c1ca90802df95f423d212412c04b3d6b5e3984007  acceptance/fixtures/s1-01-matrix-integrity.json
1181ede27e8a8b2363df8198e5d103081e4c30b71779fd6ad54bbc2f1fb6eba5  acceptance/fixtures/s1-02-expectation-safety.json
606057ac762e25a3674e43b5ef518281f9ec290770e4ee06ac018af9f364329c  acceptance/fixtures/s1-03-a0-state-shapes.json
```

All four generated JSON files parse (`JSON.parse` exit 0). `git diff --no-index --check` reports no
whitespace errors on the seven paths.

## A1-A4 regression results (plan §10.1)

- A1: `( cd api/openapi && npm run check:s3 )` → `PASS check-doc-map (s3)`, `total=49`,
  `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1`,
  `a0_catalog=44`, zero doc/a0/path/source duplicates, exit `0`. The lockfile-installed
  `node_modules` was already present, so `npm ci` was not re-run; `check:s3` executed its full
  lint/bundle/generate/typecheck/doc-map chain and regenerated the unique bundle.
- A2: `PASS protocol-validation (s3)`, `messages=12 event_types=11 identities=22 fixtures=25
  passed=25 failed=0 negative=3`, `a1_operations=49 ivr_operations=6`, `a0_tables=44`,
  `event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`,
  exit `0`.
- A3: `PASS security-validation (s3)`, `trust_surfaces=5 auth_domains=5 trust_boundaries=7
  credential_classes=10 invariants=21 fixtures=23 passed=23 failed=0 negative=10`, drift `0`,
  exit `0`.
- A4: `PASS capacity-validation stage=s2`, `groups=9 fixtureCases=113 passed=113 failed=0`,
  `productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false`, exit `0`.

## Coverage counters

16 units (9 families + 7 paths), 6 axes, 96 cells, 90 represented scenarios, 6 contract-backed
NOT_APPLICABLE cells (`FAM-SUBJECT:TIMEOUT`, `FAM-DEPLOYMENT:TIMEOUT`,
`PATH-DEPLOY-ACCEPTANCE:TIMEOUT`, `FAM-DEPLOYMENT:OUT_OF_ORDER`,
`PATH-DEPLOY-ACCEPTANCE:OUT_OF_ORDER`, `FAM-DEPLOYMENT:PERMISSION_DENIED`), 0 deferred, exactly 2
approved slices (S1/S2), root `runtimeEvidence=NOT_RUN`, `productionAccepted=false`.

## Residual risks / uncovered areas

- Runtime, line, cloud, manual and load execution remain intentionally uncovered and `NOT_RUN`;
  this is specification coverage and model/fixture consistency, not production acceptance.
- `A5-RES-02`: the acceptance authority registry is intentionally empty, so a future `PASS`
  remains unreachable before an authorized authority exists.
- `A5-RES-05`/`A5-RES-09`: deep upstream semantic drift and authoring judgment are bounded by the
  A0-A4 resolvers and independent review, not proven semantically complete by this validator.
- `A5-RES-06`: deployment timeout/out-of-order/permission-denied cells remain contract-backed N/A;
  any future A4 semantic change requires an A5 revision.
- `A5-RES-12`: A2/A3/A4 synthetic fixtures are not acceptance evidence.
- Environment note: `npm ci` was not repeated because the installed dependency tree was already
  present; the semantic A1 `check:s3` regression passed and regenerated the bundle.

## Scope confirmation

Only the seven allowlisted S1 paths were authored. No A0-A4 spec/model/validator/fixture, prototype,
screenshot, `docs/README.md`, control document, plan/review artifact, `.codex-tasks/` or Git metadata
was modified. No destructive command, staging, commit, push, PR, merge, issue action, control-doc
update or S2/later-gate work was performed. Existing user-owned screenshot changes and untracked
`docs/reviews/plan-review-20261021-205118.md` were preserved.

## S1 review fix 1 record (2026-10-26)

- Task label: `a5-production-acceptance-matrix-s1-review-fix1-ds-20261026-01`
- Scope: controller-accepted findings `F-01`..`F-04` from
  `docs/reviews/code-review-20261026-160938.md` only. Defense-in-depth residual notes
  (`classifyShape`, summary display constants, A3 audit compatibility list) were not touched.
- Paths changed in this fix: `acceptance/validate.mjs`,
  `acceptance/fixtures/s1-01-matrix-integrity.json`,
  `acceptance/fixtures/s1-02-expectation-safety.json`, this report. The accepted matrix
  (`acceptance/v1/acceptance-matrix-v1.json`, sha256 `07c2bde1...b055`) and
  `docs/19-production-acceptance-matrix.md` (sha256 `985a048d...eda2`) were not modified.

### F-01 (high) — BOUNDED quantitative interpretation

- Added one quantity interpretation: `EXACT` is fixed `value`; `BOUNDED` is `[min,max]`
  (`effectQuantity`/`effectMax`).
- Duplicate retry-step, `PERMISSION_DENIED` overall, `OUT_OF_ORDER` second-settlement and
  `POST_TERMINAL` step checks now use `max` (BOUNDED is zero only when `max===0`; `max>0`
  participates) instead of reading the EXACT-only `value` field. `stepDomainSum` now sums maxima.
- Ledger reconciliation no longer skips a BOUNDED overall. It computes the baseline + step-delta
  interval `[Σmin, Σmax]`; a BOUNDED overall must contain it (`overallMin <= sum.min &&
  sum.max <= overallMax`); EXACT overall keeps strict equality.
- Normalized allowlist keys from `A0:TABLE:<name>` to bare table names so the plan's denial
  allowlist and duplicate replay/idempotency exceptions actually apply with basis refs. No accepted
  scenario gained or lost an exception.
- `OUT_OF_ORDER` second-settlement detection now compares normalized table names. Removed
  `activation_consent.service_activation` from that set only: the accepted ACTIV/X-ACTIV OOO
  scenarios legitimately write it twice in two ordered transitions (activate → suspend), which plan
  §8.3 does not define as a repeated settlement/release/usage fact. No matrix data changed.

### F-02 (medium) — closed ExternalAssertion and resolved timingRef

- New `validateExternalAssertion` enforces the closed field set (`status`, `action`, `reasonCode`,
  `problemCode`, `processingStatus`, `terminalStatus`, `counts.*` with non-empty suffix), the closed
  op set (`EQUALS|UNCHANGED|NULL|NOT_NULL`), required `valueRef` for `EQUALS`/`UNCHANGED`, and full
  resolver validation of any present `valueRef`.
- Applied to both root `expected.external[]` and every step `expectedResponse`; reconciliation still
  runs after shapes pass.
- `axis=TIMEOUT` now requires every declared `timingRef` to be a resolved contract/policy input
  (`A0:Q:`, `A0:TABLE:`, `A2:IDENTITY:`, `A3:DEFERRED:`, `A3:CV:`); bare values such as `"30"` fail
  `A5_SCENARIO_FIELD_INVALID`, prefixed unresolved refs fail `A5_SOURCE_REF_UNRESOLVED`.

### F-03 (medium) — validator-owned minimum evidence tiers

- Added the independent `UNIT_MINIMUM_TIERS` contract derived from plan §4.2 (not from model/doc
  markers): base families/paths `INTEGRATION`; `FAM-IVR`/`PATH-ACTIVATION-TRIAL`/
  `PATH-IVR-SETTLEMENT` add `LINE`; `FAM-MEDIA-EVIDENCE`/`FAM-SHARING`/`PATH-RECORDING-EVIDENCE`/
  `PATH-SHARE-ACCESS` add `CLOUD`; `FAM-DEPLOYMENT` = `INTEGRATION+CLOUD`;
  `PATH-DEPLOY-ACCEPTANCE` = `INTEGRATION+CLOUD+MANUAL`; security units keep `INTEGRATION`
  (scenario-specific stronger `CLOUD` remains legal).
- Uses exact-set inclusion per unit. Coordinated model + docs downgrade fails
  `A5_EVIDENCE_TIER_INSUFFICIENT` in the semantics phase before doc parity.

### F-04 (low) — closed-schema gaps

- Every `StateAssertion` (preconditions, step `expectedState`, final `persisted`) must have a
  non-empty `basisRefs` array (`A5_SCENARIO_FIELD_MISSING`); normal ref resolution still applies.
- `duplicateReplay.responseOutcome` must be exactly `FIRST_RESULT` or `CONFLICT`.
- `deployment/capacity` service is now accepted only for `FAM-DEPLOYMENT` and
  `PATH-DEPLOY-ACCEPTANCE`; other units must resolve to an A0 module or A3 service identity.
- Scenario IDs are validated against the exact plan §4.1 regex in the required phase, independent of
  fixture lookup.

### Fixtures added (focused adversarial cases)

- `s1-01-matrix-integrity.json` (23 → 25 cases): `service-exception-leak`,
  `scenario-id-regex-invalid`.
- `s1-02-expectation-safety.json` (18 → 35 cases): `duplicate-bounded-domain-delta`,
  `denied-bounded-overall`, `out-of-order-bounded-second-settlement`,
  `post-terminal-bounded-write`, `effect-ledger-bounded-mismatch`, `external-field-invalid`,
  `external-op-invalid`, `external-value-ref-invalid`, `external-value-ref-unresolved`,
  `step-response-field-invalid`, `timeout-bare-timing-ref`, `timeout-unresolved-timing-ref`,
  `ivr-tier-downgrade`, `deployment-tier-downgrade`, `deployment-path-tier-downgrade`,
  `missing-state-basisrefs`, `duplicate-response-outcome-invalid`.
- `s1-03-a0-state-shapes.json` unchanged (13 cases). Every case matched its exact primary code set;
  no expected code was changed to match unintended behavior.

### Exact validation evidence

```bash
node --check acceptance/validate.mjs   # exit 0
node acceptance/validate.mjs --stage s1 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures
```

- Run after the A1 bundle regeneration, twice: exit `0` both times, byte-identical stdout and empty
  stderr, sha256 `085752aebfe43c33837fac5f13cf840f7e75d5466b5270c33e4cca8063751280` (unchanged
  because accepted data and counters did not change).
- Preserved counters: `units=16 cells=96 represented=90 not_applicable=6 deferred=0 scenarios=90
  source_refs=547 unresolved=0 ambiguous=0 drift=0 a3_residuals=18 a0_state_rows=31
  a0_shape_unrecognized=0 owner_upstream_unresolved=0 residual_owner_unknown=0
  transition_ref_invalid=0 path_family_duplicate=0 external_projection_contradiction=0
  evidence_owner_missing=0 coverage_missing=0 coverage_duplicate=0 coverage_illegal=0
  na_unbacked=0 terminal_mutation=0 duplicate_side_effect=0 ordering_regression=0
  denied_with_business_effect=0 caller_control=0 synthetic_acceptance=0
  runtime_evidence=NOT_RUN production_accepted=false spec_coverage=COMPLETE
  fixtures=3 passed=3 failed=0 negative=3 deterministic=true`.
- All generated JSON inputs parse: matrix, three fixtures, A1 bundle, A2 model, A3 model.
  No trailing whitespace on any of the seven S1 paths. `tmp/` probe outputs and the regenerated
  A1 bundle/`node_modules` are gitignored.
- Fix-1 file hashes: `acceptance/validate.mjs` `5b930f2218bea6f38d1f2da03ae65a22912c79eb7823dea170d4541df85364dc`;
  `s1-01-matrix-integrity.json` `1c78a075479fb5fc4d4cf6c5052a7fc72304d91ffba85c60fd262f18b3951e9e`;
  `s1-02-expectation-safety.json` `c0210a138f6a9d07fab5b587404b09858388addf52ee3ddab162d722b1f6c1e8`;
  matrix and `docs/19` hashes unchanged (`07c2bde1...b055`, `985a048d...eda2`).

### Adversarial probes rerun (equivalent review probes)

| Probe | Result |
| --- | --- |
| `p1` denied overall BOUNDED max>0 | exit 1 `A5_DENIED_WITH_BUSINESS_EFFECT` |
| `p2` duplicate retry BOUNDED + contained overall | exit 1 `A5_DUPLICATE_SIDE_EFFECT` |
| `p14` OOO overall BOUNDED max>1 | exit 1 `A5_ORDERING_REGRESSION` |
| `p15` denied mixed allowlist EXACT + BOUNDED business write | exit 1 `A5_DENIED_WITH_BUSINESS_EFFECT` |
| `p16` POST_TERMINAL step BOUNDED max>0 | exit 1 `A5_TERMINAL_MUTATION` |
| `p21` coordinated model+doc IVR tier downgrade | exit 1 `A5_EVIDENCE_TIER_INSUFFICIENT` |
| `p23` external field free text | exit 1 `A5_SCENARIO_FIELD_INVALID` |
| `p31` bare `"30"` timingRef | exit 1 `A5_SCENARIO_FIELD_INVALID` |
| `p26` final assertion without basisRefs | exit 1 `A5_SCENARIO_FIELD_MISSING` |
| `p13` `responseOutcome=MAGIC_OUTCOME` | exit 1 `A5_SCENARIO_FIELD_INVALID` |
| `p28` non-deployment unit service exception | exit 1 `A5_SCENARIO_FIELD_INVALID` |
| `p22` coordinated scenario ID rename | exit 1 `A5_SCENARIO_FIELD_INVALID` |

Positive controls also passed: BOUNDED overall containing the sum
(`overall [0,1]` for sum `1`), BOUNDED zero in a duplicate retry step (`max=0`), and BOUNDED zero
in a denied overall all exit `0`. A second coordinated downgrade to `MODEL_FIXTURE` on
`PATH-IVR-SETTLEMENT` also failed `A5_EVIDENCE_TIER_INSUFFICIENT` despite matching docs.

### A1-A4 regression (plan §10.1)

- A1: `( cd api/openapi && npm ci && npm run check:s3 )` → `PASS check-doc-map (s3)`, `total=49`,
  `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1`,
  `a0_catalog=44`, zero doc/a0/path/source duplicates, exit `0`; bundle regenerated.
- A2: `PASS protocol-validation (s3)`, `messages=12 event_types=11 identities=22 fixtures=25
  passed=25 failed=0 negative=3`, `a1_operations=49 ivr_operations=6`, `a0_tables=44`,
  `event_registry_sha256=c6b769d4...f4e9`, exit `0`.
- A3: `PASS security-validation (s3)`, `trust_surfaces=5 auth_domains=5 trust_boundaries=7
  credential_classes=10 invariants=21 fixtures=23 passed=23 failed=0 negative=10`, drift `0`,
  exit `0`.
- A4: `PASS capacity-validation stage=s2`, `groups=9 fixtureCases=113 passed=113 failed=0`,
  `productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false`, exit `0`.

### Classified residual risks

- `LOW` — `OUT_OF_ORDER` no longer flags `service_activation` counts >1; this matches plan §8.3's
  settlement/release/usage scope and the accepted activate→suspend scenarios. Replayed or duplicate
  activation remains covered by the `DUPLICATE` axis and edge/state checks.
- `LOW` — Effect `audit`/`outbox`/`accessEvents` evidence mirroring still reads `value` directly;
  BOUNDED audit/outbox entries are not in F-01's domain-write hard-gate scope. Ledger containment
  still validates their overall interval against baseline + deltas.
- Runtime, line, cloud, manual and load execution remain `NOT_RUN`; `productionAccepted=false`.
  This fix validates spec/fixture consistency only, not production acceptance.
- `A5-RES-02` (empty authority registry) and the other accepted residual risks are unchanged.

## S1 review fix 2 record (2026-10-26)

- Task label: `a5-production-acceptance-matrix-s1-review-fix2-ds-20261026-01`
- Scope: controller-accepted findings `F-R1-01`..`F-R1-04` from
  `docs/reviews/code-review-20261026-165122.md` only. The accepted matrix
  (`acceptance/v1/acceptance-matrix-v1.json`, sha256 `07c2bde1...b055`), `docs/19`
  (sha256 `985a048d...eda2`), `s1-01`, `s1-03`, all A0-A4 sources, prototypes, screenshots,
  docs index, plan/reviews/control, `.codex-tasks/` and S2 were not modified.
- Paths changed in this fix: `acceptance/validate.mjs`,
  `acceptance/fixtures/s1-02-expectation-safety.json`, this report.

### F-R1-01 (low) — ExternalAssertion basisRefs

- `validateExternalAssertion` now requires a non-empty `basisRefs` array for both root
  `expected.external[]` and step `expectedResponse`; missing/empty emits
  `A5_SCENARIO_FIELD_MISSING`. Ref collection/resolution is unchanged, so a prefixed
  but unresolved basis ref still emits `A5_SOURCE_REF_UNRESOLVED`.
- Fixtures: `external-basisrefs-missing-root`, `external-basisrefs-missing-step`.
  Probes `f2-external-basisrefs-missing-root/-step` and `f2-external-basisref-unresolved`
  match those exact single codes.

### F-R1-02 (low) — out-of-order counts per ref

- OUT_OF_ORDER second-settlement detection now aggregates `overall.domainWrites` by
  normalized `tableKey` and sums `BOUNDED` upper bounds before applying the existing
  protected-ref `>1` threshold. Split entries can no longer evade the rule.
  `activation_consent.service_activation` remains excluded; no generic activation counter
  was added.
- Fixtures: `out-of-order-split-entry-second-settlement` (two EXACT 1 entries totaling 2
  with matching delta -> `A5_ORDERING_REGRESSION`) and `out-of-order-single-count-control`
  (single EXACT 1 -> pass). Probe `adv-ooo-entry-split-two-exact-ones` now fails with only
  `A5_ORDERING_REGRESSION`.

### F-R1-03 (low) — overall-only effect refs

- Ledger reconciliation now iterates the union of refs present in baseline, every step
  delta, and overall. A ref absent from baseline/deltas has expected sum zero: an overall
  interval containing zero is valid (EXACT 0 / BOUNDED [0,0] proven), while a positive
  EXACT overall or an interval not containing zero emits `A5_EFFECT_LEDGER_MISMATCH`.
  Existing missing-overall and BOUNDED-containment behavior is preserved.
- Fixtures: `effect-ledger-overall-only-business-ref` (positive overall-only business ref
  -> mismatch) and `effect-ledger-overall-only-zero-control` (EXACT 0 -> pass).

### F-R1-04 (low) — timingRef policy classification

- Replaced prefix-only admission with the closed `TIMING_POLICY_REFS` registry of the six
  frozen anchors: `A0:TABLE:entitlement_metering.trial_policy_version`,
  `A2:IDENTITY:upload expiry`, `A3:DEFERRED:Q1`, `A3:DEFERRED:Q6`, `A3:DEFERRED:Q22`,
  `A3:DEFERRED:Q25`. Shape/prefix validation and ref resolution still run first: a random
  resolvable table (`A0:TABLE:catalog_order.customer_order`) or non-policy
  `A3:DEFERRED:Q17` emits `A5_SCENARIO_FIELD_INVALID`, while `A3:DEFERRED:Q999` still emits
  `A5_SOURCE_REF_UNRESOLVED`. All six anchors are used by the accepted matrix and pass S1.
- Fixture: `timing-ref-random-table`. Probes cover Q17, Q999, and an allowed-anchor control.

### Fixture count change

- `s1-02-expectation-safety.json`: 35 -> 42 cases (+7 above). `s1-01` remains 25 cases and
  `s1-03` remains 13. The harness still passes exact primary-code sets; the two positive
  controls use an empty expected-code set and exit `0`.

### Exact validation evidence

```bash
node --check acceptance/validate.mjs                       # exit 0
node acceptance/validate.mjs --stage s1 ... (plan §10.2 full arguments)  # twice
```

- Frozen S1 run twice: exit `0` both times, empty stderr, byte-identical stdout, sha256
  `085752aebfe43c33837fac5f13cf840f7e75d5466b5270c33e4cca8063751280` (unchanged because the
  summary does not print fixture case counts).
- Preserved counters: `units=16 cells=96 represented=90 not_applicable=6 deferred=0
  scenarios=90 source_refs=547 ... ordering_regression=0 duplicate_side_effect=0
  denial=0 caller_control=0 synthetic_acceptance=0 runtime_evidence=NOT_RUN
  production_accepted=false spec_coverage=COMPLETE fixtures=3 passed=3 failed=0 negative=3
  deterministic=true`.
- Probe suite: 48 probes (36 prior + 12 new) with exact expected code/exit matching,
  `probe_failures=0`. The prior `p15` probe was backed with a matching step delta for its
  allowlist overall entry so the new ledger-union rule does not mask the denial code under
  test; it still emits only `A5_DENIED_WITH_BUSINESS_EFFECT`.
- A1: `( cd api/openapi && npm run check:s3 )` -> `PASS check-doc-map (s3)`, `total=49`,
  `a0_catalog=44`, exit `0`.
- A2: `PASS protocol-validation (s3)`, `fixtures=25 passed=25 failed=0`,
  `event_registry_sha256=c6b769d4...f4e9`, exit `0`.
- A3: `PASS security-validation (s3)`, `fixtures=23 passed=23 failed=0 negative=10`,
  drift `0`, exit `0`.
- A4: `PASS capacity-validation stage=s2`, `fixtureCases=113 passed=113 failed=0`,
  `productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false`,
  exit `0`.
- All four S1 JSON inputs parse; no trailing whitespace on any of the seven S1 paths.

### Hashes after fix 2

```text
07c2bde1251b3d18bde667b904a1674358c3ebe664cbe0a4cb39094e9f86b055  acceptance/v1/acceptance-matrix-v1.json   (unchanged)
985a048dd05974c1e0f608f8e90799699320499ef44a2de1a0ef12884700eda2  docs/19-production-acceptance-matrix.md       (unchanged)
72b8fc126e20ab5317679f8e47995def6a837bb40212b9b4f6b768ba17007d05  acceptance/validate.mjs
1c78a075479fb5fc4d4cf6c5052a7fc72304d91ffba85c60fd262f18b3951e9e  acceptance/fixtures/s1-01-matrix-integrity.json
2cd763e923f0b368b8e3319535cd7e1e85c0ceaebc9623da066c0d44f8689c7c  acceptance/fixtures/s1-02-expectation-safety.json
606057ac762e25a3674e43b5ef518281f9ec290770e4ee06ac018af9f364329c  acceptance/fixtures/s1-03-a0-state-shapes.json
```

### Scope and classified residual risks

- Only the three paths above were authored; `tmp/` probe outputs and the regenerated A1
  bundle are gitignored. No runtime/cloud/line/manual execution, no staging, commit, push,
  PR, merge, issue/control update, review or S2 work. `UNIT_MINIMUM_TIERS` and the
  `service_activation` exclusion were left unchanged as directed.
- `LOW` — `TIMING_POLICY_REFS` is an exact fail-closed registry; a future legitimate
  contract/policy timing input requires an explicit validator update.
- `LOW` — OUT_OF_ORDER aggregates `BOUNDED` upper bounds, so `[0,2]` is flagged even if the
  actual count is 1; this is the fail-closed reading of "no second settlement/release/usage".
- `LOW` — an overall-only `BOUNDED` interval that contains zero (e.g. `[0,2]`) is accepted by
  the union rule, consistent with the plan's interval-containment semantics; positive EXACT
  and non-zero-containing intervals are rejected.
- Runtime/line/cloud/manual execution remains `NOT_RUN`, `productionAccepted=false`;
  `A5-RES-02` and the other accepted residual risks are unchanged.

## S1 review fix 3 record (2026-10-26)

- Task label: `a5-production-acceptance-matrix-s1-review-fix3-ds-20261026-01`
- Scope: controller-promoted `F-R2-01` only, from
  `docs/reviews/code-review-20261026-173204.md` (the `['']` residual risk). The accepted
  matrix (`acceptance/v1/acceptance-matrix-v1.json`, sha256 `07c2bde1...b055`), `docs/19`
  (sha256 `985a048d...eda2`), `s1-01`, `s1-03`, all A0-A4 sources, prototypes, screenshots,
  docs index, plan/reviews/control, `.codex-tasks/` and S2 were not modified.
- Paths changed in this fix: `acceptance/validate.mjs`,
  `acceptance/fixtures/s1-02-expectation-safety.json`, this report.

### F-R2-01 (low) — basisRefs element shape

- Added one shared assertion-level check (`BASIS_REF_SHAPE` + `invalidBasisRefs`): every
  present `basisRefs` element must be a non-empty string with a recognized source-ref prefix
  (`A0:`/`A1:`/`A2:`/`A3:`/`A4:`/`DOC12:`/`DOC14:`). Missing, non-array or zero-length arrays
  keep `A5_SCENARIO_FIELD_MISSING`; empty/whitespace strings, non-string elements and free
  text emit `A5_SOURCE_REF_INVALID`.
- Applied to ExternalAssertion root `expected.external[]` and step `expectedResponse`
  (`validateExternalAssertion`) and to StateAssertion precondition, step `expectedState` and
  final `expected.persisted` (`validateSemantics.apply`), so the two assertion contracts do
  not drift.
- `collectRefs` and `resolveRef` are unchanged: valid-shaped unresolved/ambiguous/legacy
  values still surface only through the refs phase as `A5_SOURCE_REF_UNRESOLVED`,
  `A5_SOURCE_REF_AMBIGUOUS` or `A5_LEGACY_REFERENCE_FORBIDDEN` (refs phase short-circuits
  semantics, so no duplicate `A5_SOURCE_REF_INVALID`). Duplicate-ref detection and every
  accepted F-01..F-04/F-R1-01..04 behavior are preserved.
- `['']` no longer passes assertion shape validation; the re-review 2 residual is closed.
- Fixtures: `external-basisrefs-empty-string-root`, `external-basisrefs-free-text-step`,
  `state-basisrefs-invalid` (exact single-code expectations).

### Fixture count change

- `s1-02-expectation-safety.json`: 42 -> 45 cases (+3 above). `s1-01` remains 25 cases and
  `s1-03` remains 13; all three files pass with exact primary-code-set matching.

### Exact validation evidence

```bash
node --check acceptance/validate.mjs                       # exit 0
node acceptance/validate.mjs --stage s1 ... (plan §10.2 full arguments)  # twice
```

- Frozen S1 run twice (after A1 bundle regeneration): exit `0` both times, empty stderr,
  byte-identical stdout, sha256
  `085752aebfe43c33837fac5f13cf840f7e75d5466b5270c33e4cca8063751280`.
- Preserved counters: `units=16 cells=96 represented=90 not_applicable=6 deferred=0
  scenarios=90 source_refs=547 unresolved=0 ambiguous=0 drift=0 ... runtime_evidence=NOT_RUN
  production_accepted=false spec_coverage=COMPLETE fixtures=3 passed=3 failed=0 negative=3
  deterministic=true`.
- Focused basisRefs probes (40 cases, exact code-set matching, exit `0`): missing / `[]` /
  `['']` / whitespace-only / free text / non-string / valid-shaped unresolved / recognized
  controls across root ExternalAssertion, step ExternalAssertion, precondition, step state and
  final persisted. Missing/empty arrays yield only `A5_SCENARIO_FIELD_MISSING`; shape-invalid
  entries yield only `A5_SOURCE_REF_INVALID`; valid-shaped unresolved refs yield only
  `A5_SOURCE_REF_UNRESOLVED` (no double-report).
- F-01..F-04 and F-R1-01..04 focused regression probes (19 cases: BOUNDED denial/duplicate/
  POST_TERMINAL, external/timing shapes, tier downgrades, closed-schema gaps, external
  basisRefs missing, OOO split/control, ledger overall-only/zero, timing registry/control):
  exit `0`, all exact code sets matched.
- A1: `( cd api/openapi && npm run check:s3 )` -> `PASS check-doc-map (s3)`, `total=49`,
  `a0_catalog=44`, exit `0`; bundle regenerated (gitignored).
- A2: `PASS protocol-validation (s3)`, `fixtures=25 passed=25 failed=0`,
  `event_registry_sha256=c6b769d4...f4e9`, exit `0`.
- A3: `PASS security-validation (s3)`, `fixtures=23 passed=23 failed=0 negative=10`,
  drift `0`, exit `0`.
- A4: `PASS capacity-validation stage=s2`, `fixtureCases=113 passed=113 failed=0`,
  `productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false`,
  exit `0`.
- All four S1 JSON inputs parse (`s1-02` cases=45); seven S1 paths have no trailing
  whitespace.

### Hashes after fix 3

```text
07c2bde1251b3d18bde667b904a1674358c3ebe664cbe0a4cb39094e9f86b055  acceptance/v1/acceptance-matrix-v1.json   (unchanged)
985a048dd05974c1e0f608f8e90799699320499ef44a2de1a0ef12884700eda2  docs/19-production-acceptance-matrix.md       (unchanged)
dffe950708e109132000ac9165ae536d79d7a3d29bca88a3c03e378032cc9d3d  acceptance/validate.mjs
1c78a075479fb5fc4d4cf6c5052a7fc72304d91ffba85c60fd262f18b3951e9e  acceptance/fixtures/s1-01-matrix-integrity.json   (unchanged)
6d041cc8689c817fbd40bad17ec67d93234645e34e20eee59e234fccdc330846  acceptance/fixtures/s1-02-expectation-safety.json
606057ac762e25a3674e43b5ef518281f9ec290770e4ee06ac018af9f364329c  acceptance/fixtures/s1-03-a0-state-shapes.json   (unchanged)
```

### Scope and classified residual risks

- Only the three paths above were authored; probe outputs under gitignored `tmp/` only. No
  runtime/cloud/line/manual execution, no staging, commit, push, PR, merge, issue/control
  update, review or S2 work.
- `LOW` — the element shape check is assertion-level and intentionally limited to
  `basisRefs`; other ref arrays keep their existing closed/collector contracts.
- `LOW` — phase ordering is preserved: a basisRef carrying a legacy marker or a recognized
  but unresolved prefix is reported by the refs phase (`A5_LEGACY_REFERENCE_FORBIDDEN` /
  `A5_SOURCE_REF_UNRESOLVED`) before semantics, which is fail-closed and avoids
  double-reporting.
- Runtime/line/cloud/manual execution remains `NOT_RUN`, `productionAccepted=false`;
  `A5-RES-02` and the other accepted residual risks are unchanged.
