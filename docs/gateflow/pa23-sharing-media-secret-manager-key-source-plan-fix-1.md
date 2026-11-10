# PA23 — Plan FIX-1: explicit file selection non-interference

## 1. Task, authority and status

- Task label: `pa23-plan-fix1-gpt-20261110-01`.
- Work unit: `PA23-sharing-media-secret-manager-key-source`; date: **2026-11-10**; provider/runtime: GPT / Codex CLI, without independent model-version attestation.
- Gate: **fix only, after independent plan review**. Binding authority is the active PA23 Goal Confirmation and plan-review adjudication in `docs/gateflow/project-execution-control.md`; Control is read-only.
- Status: **FIX-1 AUTHOR-COMPLETE — PENDING INDEPENDENT RE-REVIEW**. The accepted finding is a **fixed candidate** only. This artifact claims neither runtime success, reviewer acceptance, plan acceptance nor implementation authorization.
- Authorized repository writes are exactly `docs/gateflow/pa23-sharing-media-secret-manager-key-source-plan.md` and this new artifact, `docs/gateflow/pa23-sharing-media-secret-manager-key-source-plan-fix-1.md`.

## 2. Reviewed inputs and exact accepted finding

Both independent reviews targeted the same **429 LF lines / 61,112 bytes** of the pre-fix plan:

| Input / review | Reviewed plan SHA256 / artifact SHA256 |
| --- | --- |
| Review A target plan | `A76A3224196A2D87E1D3242EF1CE2EF176908F38D045EA6B823E508CA7201E76` |
| Review B target plan | `A76A3224196A2D87E1D3242EF1CE2EF176908F38D045EA6B823E508CA7201E76` |
| `docs/reviews/plan-review-20261110-090319.md` | `59CD3C9DBA3CABE0DACAB1BFAAC05A8F48193332BA0DA8FFC71FB59F08C925A4` |
| `docs/reviews/plan-review-20261110-092110.md` | `5B95268AA1441E318BB394E004ABE6C90D97F676B90FC895733DA91CFA70EB67` |

The pre-fix plan and both review-artifact hashes were measured locally and match those reviewed inputs. Both reviews record frozen Control SHA256 `568E3CEF3F88E0023EBD229C1513B29575111693A77A9FA652CC1B987F14FEB7`; that is historical freeze provenance, not a claim that current Control still has those bytes after Controller adjudication.

**Exactly one accepted material finding:** Review A's Medium `explicit-file-selection-can-be-suppressed-by-foreign-tencent-metadata`. When `provider=file`, unrelated/stale `tencent.*` metadata must not suppress the explicitly selected file provider. The contract requires no Tencent provider in a conflict; it does not require zero built-in providers. Preserve existing explicit file/provider-path semantics. Tencent selection with `bundle-path` still suppresses Tencent with no implicit file fallback; unknown/invalid/compound selectors create no built-in provider; zero/one/many ambiguity-before-read remains unchanged.

Review B returned `pass-with-risks` with **no material finding** and two non-blocking questions. The Controller expressly held that B's zero-finding conclusion does **not override** the direct Goal Confirmation/code contradiction accepted from A. This fix follows that adjudication without changing either review or treating B as acceptance of the revised plan. No second material finding or new goal is introduced.

Direct local code evidence: the current `MediaPlaybackKeySourceConfiguration` selects file explicitly and reads only `bundle-path`, maps path-resolution exceptions to a null path and constructs the existing file provider. Existing `MediaPlaybackConfigurationTests` retain one unavailable file provider for missing/invalid paths and ambiguity-before-read in either custom-provider registration order. `SharingQueryBoundaryTests` pins the file/parser boundary, bean surface and authority restrictions. None of these source/test files is edited.

## 3. Before / after rules

| Case | Before FIX-1 | After FIX-1 candidate |
| --- | --- | --- |
| Exact `provider=file` plus foreign Tencent metadata | Section 5 suppressed the file provider when an unspecified Tencent metadata property was present; section 9D required zero built-ins. | Exactly one existing file provider, independent of all unselected Tencent fields. Do not inspect/resolve/validate `tencent.region`, `tencent.secret-name`, `tencent.version-id`, `tencent.connect-timeout-seconds` or `tencent.read-timeout-seconds`; their presence/value cannot suppress or trigger file construction/read. Only explicit file selection/path behavior governs file use. No Tencent provider, client adapter, SDK client, role credential or metadata client is constructed/read. |
| Exact file selection with missing/invalid/unresolved path | PA22 creates one file provider that is unavailable on read. | Same provider cardinality and path behavior; stale Tencent fields cannot alter it. |
| Exact `provider=tencent-ssm` plus any `bundle-path` presence | Suppress Tencent, no fallback. | Retained and made exact: key presence via Spring Environment `containsProperty`, before path value resolution, including empty/blank/invalid/unresolved values; zero built-ins, no file construction/read. Presence-inspection failure also suppresses Tencent without fallback. |
| Exact Tencent without a path, incomplete/invalid Tencent settings | One permanently unavailable selected provider, without client/network initialization. | Unchanged, including required metadata, timeout and placeholder failures. |
| Missing/unknown/invalid/compound selector | No built-in provider. | Unchanged, including blank/whitespace/case-variant/unresolved selector or selector-resolution error. Metadata alone never selects a source. |
| Zero / one / many total providers, including custom providers | Existing sole authority fails closed for zero/many; cardinality precedes reads. | Unchanged. One selected provider does not override custom providers; both registration orders and priority/order decoys remain covered, with no reads or SDK/metadata initialization on ambiguity. |

This preserves the confirmed goal: file remains explicitly selectable, never an implicit fallback, while only the selected Tencent adapter can perform the single explicit-version SSM read. Ignoring irrelevant metadata removes an availability/scope divergence without relaxing fail-closed selected-Tencent validation or altering the sole authority.

## 4. Changed sections and six retained tests

- Section 1 updates the task/gate/status, fixed-candidate disposition and exactly-two-document write boundary.
- Section 2 G2 and section 7's configuration-failure row narrow conflict wording consistently; no blanket foreign-Tencent/file suppression remains.
- Section 5 states exact presence/non-interference semantics and preserves explicit file/path and selector failures.
- Section 8 aligns Spring composition, without changing factories, private seams, custom-provider treatment or authority cardinality.
- Section 9D replaces `tencentAndFileMetadataConflictsCreateNoBuiltinProvider` with `staleTencentMetadataDoesNotSuppressExplicitFileProvider`; assertions expand inside the same six methods, not through new methods. Section 13 updates status/stop and distinguishes completed initial reviews from pending re-review/acceptance.

All six configuration additions are retained, with exactly one rename:

1. `tencentSourceRequiresExactExplicitSelection`
2. `tencentMissingMetadataIsUnavailableWithoutNetwork`
3. `staleTencentMetadataDoesNotSuppressExplicitFileProvider`
4. `tencentSharesSoleAuthorityAcrossProfiles`
5. `tencentAndCustomProvidersAreAmbiguousBeforeAnyRead`
6. `explicitFileSelectionRemainsIndependentOfTencentClient`

The stale-metadata method loops over each of the five listed Tencent fields, individually and combined, with absent baseline, empty, whitespace, valid, invalid and unresolved-placeholder forms. Each explicit-file/valid-path case requires exactly one file provider, successful real-authority issue/verify and zero Tencent provider/client-adapter/SDK-client/role-credential/metadata-client construction or Tencent/metadata reads, both before and after file use. Unselected Tencent property lookup/resolution/validation is forbidden. Existing missing/invalid/unresolved file-path expectations remain covered.

Tencent-plus-`bundle-path` assertions stay within `tencentSourceRequiresExactExplicitSelection`: every path-presence form yields zero built-in providers, zero Tencent/client/SDK/credential/metadata construction and zero file construction/read. Ambiguity assertions retain two custom providers, Tencent+custom, file+custom (including stale Tencent fields), manually registered Tencent+file, both provider registration orders and configuration/provider ordering, plus `@Primary`/`Ordered` decoys; neither contender is read. All existing 13 configuration methods remain unchanged future regression obligations.

## 5. Unchanged scope, counts and evidence

- One composed **S1**, the same **10 future implementation paths**, two new production classes and exactly one pinned direct `com.tencentcloudapi:tencentcloud-sdk-java-ssm:3.2.46` dependency; no extra slice, dependency, seam, public surface or authority.
- Exactly **6 configuration additions**, 13 → 19; exactly **43 total additions = 18 + 8 + 10 + 6 + 1**; architecture 7 → 8, authority 24 unchanged; focused target 117 unchanged.
- Inventory target unchanged: Surefire **36/309**, Failsafe **13/214**, total **49/523** classes/methods; current baseline **46/480**, annotation scan **481 → 524**, same frozen comment. These are plan targets, not executed results.
- Sections 4, 6 and 10–12 retain all SDK artifact/hash/API, direct service-role credential, request/response/redaction, timeout/retry, scope, inventory and firewall evidence verbatim. No cloud/credential/timeout/firewall decision is reopened.
- Firewall remains **72 coordinates / 65 packaged jars**, **+17/+17**, dependency-only 8, counterparts 64, historical 37/34, deltas 35/31, seven exact collisions, sole unchanged jarmode and **116 = 82 + 34** probes; no broad exemption or new probe execution claim.
- No database/schema/ACL/migration/deployment/resource/public-API change, cloud provisioning, live acceptance, generic secret platform, credential fallback, cache/TTL, retry framework or Q22 decision.

## 6. Documentation-only validation performed

- Read the required local inputs, binding Goal Confirmation/adjudication and current file/path/custom-provider/architecture evidence; hash-checked the pre-fix plan and both reviews against the frozen targets.
- Inspected the bounded before/after plan diff and conflict/status references; statically checked exactly six unique section 9D names, the one-name replacement, unchanged 6/43 and 49/523 accounting, and verbatim preservation of sections 4, 6 and 10–12. This is document validation, not an implementation/test/CI-inventory run.
- Checked both edited documents for valid UTF-8, LF only, no trailing whitespace and exactly one final newline; recorded final byte sizes and SHA256 in the completion report.
- Ran `git diff --check -- docs/gateflow/pa23-sharing-media-secret-manager-key-source-plan.md docs/gateflow/pa23-sharing-media-secret-manager-key-source-plan-fix-1.md` and inspected `git status --short`, using the existing explicit `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call25 --work-tree="$PWD"` override for the Windows Git pointer; no Git metadata rewrite. Both documents are untracked, so also checked the complete documents and before/after diff directly rather than treating an empty tracked diff as coverage.
- Pre-existing modified Control, untracked plan/reviews and private `.codex` state were present on entry. Control, reviews and the three required source/test files remain byte-identical to their entry hashes; private helper state remains untouched. The only new repository artifact is this FIX-1 document.

## 7. Residual risks, owners and binding stop

| Residual / remaining obligation | Owner / destination |
| --- | --- |
| Re-review the asymmetric selector and looped assertion matrices; fixed candidate is not accepted closure. | Independent re-reviewers and Controller, only after this handoff; no dispatch by this author. |
| Concrete pinned-client mockability, constructor capture, SDK `TCLog` source/probe verification, response-identity behavior, narrow architecture-test mechanics and actual source=CI=XML/graph reproduction remain implementation/code-review obligations. | Implementation/build/Security and code reviewers after separate authorization; no new plan blocker or added seam here. |
| CVM-only suitability, absent metadata timeouts, absent SSM total `callTimeout`/DNS deadline and underlying connection retries remain disclosed limitations, not bounded end-to-end latency. | Platform/Security; Operations for SLO/runtime validation. No custom timeout/metadata client or fallback. |
| Per-call SSM latency, quota, cost, throttling, CAM/region/endpoint/CVM-role/resource/version provisioning, response-echo live validation and immutable rollout. | Platform/Security capacity/deployment owners and Operations; deferred deployment/live obligations, not acceptance in this unit. |
| Java/SDK String memory, future SDK/advisory/logging drift, key-overlap/old-kid removal and unselected Q22 timing policy. | Security/Build and Platform/Security + sharing owner, as in plan section 13. |

**Stop before independent re-review.** No source/test/POM/CI/Control/resource/README/review-artifact or supplied-evidence edits; no implementation, tests, builds, application/Docker/PostgreSQL execution or inventory/firewall probe runs; no network/cloud/CVM metadata/STS/SSM access, credential acquisition, provisioning or live acceptance. No staging, commit, push, branch creation, PR open/mutation, approval, mark-ready, reviewer request, external comment, issue mutation, review dispatch, re-review entry or later-gate entry occurred or is authorized. No runtime or reviewer acceptance is claimed. Only the two permitted documentation paths change; all other scope and owners remain binding.
