# PA14 Sharing Command Code Rotation Final Closeout

## Delivered Scope

PA14 implements the final backend sharing-command behavior:

`POST /api/v1/customer/shares/{shareId}/code-rotations`

The endpoint:

- requires the trusted customer boundary and one exact `Idempotency-Key`;
- accepts only a closed `{}` body or optional non-negative `expectedCodeGeneration`;
- permits only an unexpired `LOCKED` + `EXTRACTION_CODE` grant to become `ACTIVE`;
- resolves exact persisted replay before current state/policy/material checks;
- returns the new extraction code once on the first successful action and only persisted metadata on replay;
- evaluates rotation policy before material generation, with production policy absent and fail-closed at 503;
- increments code generation, revocation generation and revision exactly once through one conditional update;
- writes the exact access event, Outbox fact and audit fact in one transaction and rolls all effects back on failure;
- preserves create, revoke, list and `/me`.

Implementation remains within the accepted 20-path allowlist: 19 existing files plus the new internal `ShareRotationPolicyProvider`. V001-V010, OpenAPI, POM, application YAML/properties, design documents, public admission/media and frontend are unchanged. No production policy, KDF, TTL, key or provisioning value was selected.

## Verification

- Fresh independent aggregate review, and repeated implementation/code-review runs, execute PostgreSQL **16.15**.
- Full clean verification: **79 Surefire + 118 Failsafe = 197**, zero failures, errors or skips.
- Exact source = CI = XML inventory passes; Failsafe breakdown is `64+10+10+8+8+18=118`.
- Focused suite passes 29/29; rotation command IT passes 18/18.
- Default and opt-in web JAR smokes pass.
- Production-JAR firewall passes all **82** adversarial probes.
- GitHub Actions run `42056869835` on PR head `3888e64af44ddffda96e471d59c06d87f03290e8` completed success for both `contract-regressions` and `java-runtime`.

## Documentation

- Goal, accepted plan, plan fix, implementation evidence and all rollback/concurrency/secret/fact contracts are recorded under `docs/gateflow/` and `docs/reviews/`.
- Both plan findings A-F-01 and A-OQ-1 are fixed.
- The sole accepted code-review finding CR-F1 is fixed: rotation now asserts exactly all eight OpenAPI error mappings, including `UNAUTHENTICATED/401`, without revoke-only stale/410 leakage.
- Every code-review and aggregate-review artifact is committed; the intermediate incomplete aggregate review is retained as truthful history and is not treated as acceptance.
- No GitHub issue is associated with this work unit, so issue linking, closing keywords, issue comments and issue mutation are not applicable.

## Review And Finding Status

- Independent plan review A/B and fixed-plan re-review A/B passed.
- Independent implementation code review A/B found the bounded CR-F1 test gap, which was fixed and re-reviewed by A/B.
- Final aggregate Deepreview by the GPT Code Primary Judge completed all 34 branch-changed-path coverage and independently reran the full 197 tests, exact inventory, JAR smokes and 82 firewall probes: **PASS WITH RISKS, 0 material findings**.
- Independent PR review A/B both returned **PASS WITH RISKS, 0 material findings** against PR #24 head `778e051`, including source tests, CI/head/Draft agreement, protected paths, PR-body consistency and CR-F1 closure.
- No accepted finding remains open; no post-review fix or separate re-review is required.

## Draft PR

PR #24: https://github.com/zh94695424/one-call/pull/24

- Title: `feat(sharing): add customer extraction-code rotation`
- Base: `main`
- Head: `codex/pa14-sharing-code-rotation`
- State: OPEN
- Draft: true
- Mergeable: `MERGEABLE`
- Review decision: empty
- Final PR-review/acceptance head before this closeout record: `3888e64af44ddffda96e471d59c06d87f03290e8`

The PR remains intentionally unmerged and Draft. It was not approved, marked ready, assigned reviewers, sent for external comments or merged.

## Residual Risks And Owners

| Residual | Owner / destination |
| --- | --- |
| Production rotation allow/deny policy, approved code KDF/KDF parameters and material custody remain unset; missing adapters fail closed. | Security / Product / Sharing Backend -> approved provider/security design and provisioning gate. |
| Docker/Testcontainers bridge is local and temporary; actual `postgres:16` patch can drift within major 16; remote runtime and load evidence remain separate. | Platform / Controller + Backend / Database -> stable runner, recorded server image/version and production-scale validation. |
| Frozen V009 structurally admits some service-forbidden grant shapes. | Sharing Backend / Database -> retain exact service-level state/mode/witness/row-field tests; no DDL change in this slice. |
| Lost first-response plaintext code cannot be recovered by replay, and gateway rate-limit/abuse policy is outside this endpoint. | Sharing / Product + Security / Platform -> replacement UX and gateway-owned controls; preserve secret-free replay. |
| Canonical JSON helper/test-oracle coupling remains a maintainability risk; current payloads are simple and literal canonical vectors exist. | Sharing Backend -> preserve independent vectors; refactor a neutral owner only when justified by contract change. |
| Production ingress body limits, provider latency/cancellation, contention and security/load acceptance are not established by bounded local tests. | Security / Platform + Sharing Backend -> later operational/SLO/security work. |

Every residual has an owner and destination; none is unclassified or blocking for this bounded work unit.

## Final Status And Next Entry

`draft-PR-pass` is accepted: implementation, all independent review loops, aggregate Deepreview, accepted slice/deepreview checkpoints, final push and exact-head GitHub Actions are complete; no accepted finding remains.

**PA14-sharing-command-code-rotation is complete at `final closeout pass`.** This is implementation and review evidence only, not production acceptance.

After the user merges PR #24, fetch the updated `main` and begin a fresh Phaseflow goal confirmation for the next bounded sharing work unit, tentatively `PA15-sharing-public-admission-and-ticket`, based on the frozen admission/ticket contracts. Do not continue from this unmerged branch or reuse this goal confirmation for that later scope.
