# PA35 Plan Fix 6 — Closure Artifact

Task label: `pa35-operations-ivr-session-read-plan-fix6-gpt-20261117-01`
Work unit: `PA35-operations-ivr-session-read-foundation`
Gate: bounded `plan fix6`; author correction only
Date: 2026-11-17 (Asia/Shanghai)

## Scope and intake

This artifact records closure of accepted findings `PA35-PR-F5-A-01`,
`PA35-PR-F5-B-01`, `PA35-PR-F5-B-02` and `PA35-PR-F5-B-03`. Only the
cumulative plan and this artifact are authorized. No source, test, migration,
OpenAPI, CI, POM, README, control, review or Git metadata file is edited.
The binding intake HEAD is `8b9b04fcede3d46bce229e092aa4947599eb9b16`.

## Corrections

- Row30 preserves the complete `src/main/java` plus `src/main/resources`
  production-tree walk with only V018 excluded. It authorizes changing the
  current count `282` to exact final `291` and replacing only the current
  aggregate after the nine authorized PA35 production files exist. The
  implementation report must record that recomputed SHA-256 and the exact
  291-file canonical-LF basis. Historical V001–V018, public-contract,
  method-identity, ACL, topology and other assertions remain unchanged.
- Focused and full verification explicitly execute
  `v001ThroughV017AndPublicContractsRemainByteFrozen`.
- The pair-map serialization is now path UTF-8 bytes, one NUL, 32 raw bytes
  from the RAW SHA-256, then 32 raw bytes from the canonical-LF SHA-256, with
  no inter-digest delimiter and no row terminator. F8/G1 require
  `ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409` and
  reject UTF-8 hex-text, delimiter and row-terminator interpretations.
- Row30 enumerates all current CI totals: 712/520, sum 1232, token 1233,
  77/32, both XML expected totals, diagnostics 109/1232 and 77/712+32/520,
  explicit PA35 starts `[712, 520]` and `1233`, final 788/556/1344/1345/83/34,
  and POM 34/36. Historical subtraction chains, including 520/404 and
  1208/1209, remain protected.
- Owner adapters use distinct narrow ports by type via `ObjectProvider`; the
  existing `one(ObjectProvider)` exactly-one, fail-closed behavior is reused.
  Component names are diagnostic only. E6/E8 and H4/H5 cover zero,
  ambiguous, uncreatable and removed-provider cases without new identities.

## Static validation

The fix6 verifier confirms the current baseline source/CI/POM inventory is
`109 classes / 1232 methods`, token scan `1233`, and POM `32` scoped Failsafe
includes (`34` global includes). The plan manifest remains exactly `30`
paths: `20` new and `10` existing. The future inventory remains `117`
classes / `1344` methods, token scan `1345`, with `83` Surefire classes,
`34` Failsafe classes, and POM `34` scoped includes / `36` global includes.
The eight planned classes retain exactly `112` method identities.

The verifier confirms row30 remains the existing path and the production
delta remains nine new production files plus two content-modified existing
production files, yielding the required final current-tree basis of `291`
files; no path or test identity was added or removed. It confirms the pair-map
formula and expected digest, including the binary-vs-UTF-8-hex negative.

Text checks confirm UTF-8 without BOM or U+FFFD, no conflict markers, no
trailing whitespace, and `git diff --check` for both authorized paths. No
Maven, Docker, full CI, runtime or production verification was run.

## Stop

The cumulative plan and this fix6 artifact are the complete bounded output.
The next gate is a fresh independent A/B Planreview. Do not stage, commit,
push, open or mutate a PR/issue, request review, mark ready, enter a later
gate, or claim implementation/runtime/production acceptance.
