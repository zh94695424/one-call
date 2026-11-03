# PA12 customer share list API — bounded plan fix 3

- Work unit: `PA12-customer-share-list-api`.
- Task label: `pa12-customer-share-list-api-plan-fix3-gpt-20261102-151500`.
- Date: 2026-11-02. Current gate: `fix`, after independent re-review 2. Status: `plan-fix-3-complete`.
- Authority: PA12 goal and re-review-2 Controller adjudication in `docs/gateflow/project-execution-control.md`. Author completion is not review acceptance, gate advancement, implementation permission or runtime validation.
- Exact repository write boundary: modify `docs/gateflow/pa12-customer-share-list-api-plan.md`; add this artifact. No source/test/control/prior artifact/CI/README, repository `tmp/`, Git metadata or user/private file edits. External validation records live under `/tmp/pa12-fix3-validation/`, outside the repository.

## 1. Frozen inputs

All five supplied SHA-256 identities were recomputed from local bytes before editing and matched. The original plan bytes were copied externally before the first edit; final input revalidation checks that copy and the four unchanged inputs. Historical artifact contents and hashes are not rewritten to reflect subsequent corrections.

| Frozen input | SHA-256 |
| --- | --- |
| Pre-fix-3 plan, `docs/gateflow/pa12-customer-share-list-api-plan.md` | `19fa08ee650f4379a5202532682310fc92a4449901fd8b92361b04b78b5d5e0f` |
| Prior fix, `docs/gateflow/pa12-customer-share-list-api-plan-fix-2.md` | `955f4a41bef4cd8b67bb69cc648ede301a931e27504d19c949d05e132dc75746` |
| Re-review A, `docs/reviews/plan-review-20261102-142745.md` | `a25f435e2ff5e924f337250e24ce8b6a1528b33ed773f79a399e1b9768461fef` |
| Re-review B, `docs/reviews/plan-review-20261102-150037.md` | `39d5a77cd05f839a1e35ebf66a8395f3d2e6499a9a18ae87477f08c8a333ab2a` |
| Controller, `docs/gateflow/project-execution-control.md` | `067d3d98834ce711e99362606d4e91389d128644fad0caa33e6c7870c03e2005` |

## 2. Findings and exact dispositions

| Finding | Controller disposition / applied correction |
| --- | --- |
| B3-01 | Accepted; applied in §3. Replace the exact `hasMessage` with `hasMessageStartingWith("No named interface named 'customer-query' found!")`. Retain the whole `ApplicationModules.of(...).verify()` expression, `IllegalArgumentException` and explicit non-`Violations` rule. Attribute the suffix ` Original dependency declaration: customer_iam -> sharing::customer-query.` to the frozen review's executed Modulith 2.1.2 evidence; no new library probe is claimed. |
| B3-02 | Accepted; applied in §7. Add `web/CustomerSharesController.java` to the existing file list in `CustomerApiBoundaryTests.profileAndVerifierArtifactsCannotEscapeIntoProduction()`, pinning `@Profile("web")` and `ConditionalOnWebApplication.Type.SERVLET`. Keep all three existing methods, the exact class inventory and all other bans. |
| B3-03 | Accepted; applied in §9 and revision/stop metadata. Replace the stale current-fix-only-fix-1 sentence with the historical fix-1/fix-2 chain and current fix-3 two-document authorization. Prior artifacts/provenance remain untouched. |
| B3-04 | Accepted; applied in §9. Enumerate and correlate actual current README sites at `:39`, `:265`, `:280`, `:289`, `:298`; explicitly preserve historical `:226`, `:303`, `:311`, `:320`. Add `application-persistence.yml` alongside unchanged default/web configuration in both boundary statements. No README/config edit in this task. |
| B3-05 / A O-1 | Accepted; applied in §9. Correct the frozen 64-name migration literal location to `.github/workflows/ci.yml:79-144`; identify `:147-159` as assertions. Preserve both blocks and the two existing ten-method container sets byte-for-byte. |
| A O-2 | Accepted; applied in §2. Use accurate existing caller/name behavior for `CustomerRequestPrincipal`; bearer redaction belongs to `OpaqueCustomerBearer`. No production edit. |
| A O-3 | Accepted; applied in §8.1 web #6. Choose a separately programmatically started/closed random-port `web` context with `server.servlet.context-path=/pa12` and explicit test-owned verifier/service mocks. In the existing method, assert authenticated success for both prefixed exact paths, denied prefixed aliases/non-GET/error paths with zero verifier/query calls, and no boundary access for unprefixed customer URLs. No persistence/fault serializer, extra method/class/report or production test hook; ten web methods remain. |
| A O-5 | Accepted; applied in §7. Explicitly keep sharing's module declaration unchanged while customer_iam's declaration receives the intended one-line named dependency change. |
| A O-4 | Rejected-with-reason; no named codec exception type added. Internal-versus-request failure behavior is already testable through the controller/web error path (INTERNAL versus CURSOR_INVALID); naming an implementation-local exception type expands plan scope without improving correctness. Existing codec/web scenarios remain binding. |

All accepted corrections are precision-only. No other decision is reopened, no additional finding is promoted, and no implementation-local codec exception type is prescribed.

## 3. Changed sections and unchanged boundary

- Header: current fix-3 task/status and historical fix-2 lineage.
- §2: principal caller/name wording and correct bearer-redaction attribution only.
- §3: fixed-prefix message assertion and attributed actual suffix only; whole-expression call, throwable type, seam/export breadth and non-`Violations` distinction remain intact.
- §7: explicit controller profile assertion home and unambiguous sharing/customer_iam declaration wording; no write-set, method or class expansion.
- §8.1: only web #6 gains the concrete non-empty context-path mechanism; method name, ten-method suite and other scenarios remain intact. §8.2 is unchanged.
- §9: current fix chain, README source-site correlation, CI literal/assertion ranges and persistence configuration freeze.
- §10: current fix-3 interval provenance/validation and stop condition; historical drift and fix-2 validation record remain distinct.

Numbered §§1, 4, 5 and 6 are byte-identical to the frozen plan; all 26 planned method names are identical. Exactly one read-only end-to-end S1 remains for `GET /api/v1/customer/shares`. Preserve `/me`, `/shares`, selection/Accept precedence, named seam/export, creator/workspace isolation, ACTIVE lifecycle, cursor/limit/order, errors, roles/pools, read-only proof and secret-free projection. No new production file beyond the approved plan; no provider/session adapter/store, TTL/revocation policy or other non-goal enters scope.

V001–V009, OpenAPI, docs/11/12/14/15/17, default/web/persistence configuration, dependencies/firewall, migrations/grants/indexes and all other non-goals remain unchanged. Future source/test/CI/README instructions are not permission to edit those files now.

## 4. Plan-only validation

- Structure: ten numbered sections remain in order; one S1; identical ordered inventory of 26 new method names. The other 25 enumerated method descriptions are byte-identical; only web #6's description is expanded. The existing three `CustomerApiBoundaryTests` methods and eight `ModuleBoundaryTests` methods are not expanded in count.
- Source baseline re-derived with CI's ordinary `@Test` method regex: 49 Surefire methods / 11 classes and 84 Failsafe methods / 3 classes. None of the 26 planned names collides with the baseline. Two session + six codec + ten web methods add 18; eight container methods add eight.
- Arithmetic: `2+6+10=18`, `49+18=67`, `11+2=13`; `84+8=92`, `3+1=4`, `64+10+10+8=92`; `67+92=159`. Cursor maximum remains `46+ceil(21*128/8)=382` bytes and `ceil(382*8/6)=510` unpadded characters, within 512.
- Scenario review: unchanged §§4–6 preserve cursor validation/order, HTTP error/selection/Accept precedence, ACTIVE checks, roles and read-only ownership. All eight integration scenarios and other unit/web scenarios are unchanged. Web #6 specifies a test-owned independent context, success/deny calls and cleanup inside its existing method, not another test/report or production seam. B3-01 preserves call site/type/non-`Violations`; B3-02 names an existing assertion rather than assuming generic scanning covers the new controller. No runtime scenario pass is claimed.
- Source correlation: README `:39` is current inventory/command breakdown; `:265` is the empty customer_iam dependency row; `:280` says discovered production edges are empty; `:289` is java-runtime inventory; `:298` is current CI/evidence totals. These become the exact named edge and 67/13 + 92/4, 159 aggregate where applicable. `:226`, `:303`, `:311`, `:320` describe earlier checkpoint evidence and are preserved. CI `:79-144` parses to exactly 64 literal names; `:147-159` contains source/XML/count assertions, not that literal. Both blocks are frozen.
- Both outputs are checked for trailing spaces/tabs. The original plan copy and four unchanged inputs are rehashed; source/tests/README/CI/config/contracts and prior artifacts are protected by the interval comparison below. Output SHA-256 values are computed externally in `/tmp/pa12-fix3-validation/output-sha256.txt` and returned to the Controller, not embedded in either output.

Validation is local text/source inspection, structure/scenario comparison, arithmetic and filesystem hashing only. The initial `python` command was unavailable; the same local validation used `python3`. An initial CI-source check incorrectly looked for the assertion's diagnostic at line 159; inspection showed the condition at 159 and diagnostic at 160, and the corrected check pins the actual condition. No source/CI change was needed. No Maven, Java launcher, Modulith replay, Docker/container, HTTP, npm, network, GitHub or external action was performed. Executed-library evidence remains attributed to the frozen review.

## 5. Two-path interval provenance

Before the first repository edit, an external copy of the plan and a SHA-256 manifest of 1,164 repository regular files were captured under `/tmp/pa12-fix3-validation/`. The scan excludes `.git`, repository `tmp/` and symlinks, and does not expand into user/private paths. Compare the final scan with `manifest-before.json`: the only changed regular file is the allowed plan, the only added regular file is this fix-3 artifact, and no scanned regular file is deleted. No additional interval drift is observed in the scanned set. Final scan/delta records are external in `manifest-after.json` and `interval-delta.json`.

This is a two-output, bounded-interval result, not global clean-tree or Git head/index provenance. `git status --short` failed read-only because `.git` points to the Linux-unusable Windows-style `D:/codex-call/.git/worktrees/codex-call10`; no metadata repair, staging or other Git mutation was attempted. Excluded paths are not covered by the comparison; no claim is made about their interval contents.

Historical drift remains separate: fix-1 disclosed seven unrelated additions, `tmp/env-config/agent-tools.zsh`, `tmp/env-config/codex-agent-run`, `tmp/env-config/qwen38max-config.toml`, `tmp/env-config/sub-agents-SKILL.md`, `tmp/install_qwen38max.sh`, `tmp/probe_qwen_max.sh`, `tmp/probe_qwen_models.sh`, plus later control-document bytes. Fix-2's scoped no-new-drift result and current fix-3's scoped result do not erase those disclosures or attribute their producers. No such path, control document or historical artifact is edited/deleted/reverted here.

## 6. Residual owners, destinations and stop

| Residual | Owner / destination |
| --- | --- |
| Implementation and executable acceptance, including prefix assertion, controller conditions, context-path tests and README/CI updates | Backend QA + Architecture/CI; future separately authorized S1, within existing method/class and production-file budgets. No runtime pass here. |
| Independent review and next gate action | Gateflow Controller; independent review only after Controller authorization. This fixer does not dispatch reviewers or enter re-review/implementation. |
| Git provenance, excluded paths and historical concurrent drift | Workspace controller/environment owner; independent reconciliation before any global provenance claim, not metadata repair or unrelated reversion in this task. |
| Composition exception and future docs/12 reconciliation; cycle-closing edges | Architecture + Gateflow Controller; existing future reconciliation/extraction destination, no design change now. |
| Trusted selection issuance, provider/session adapter/store, TTL/revocation | Customer IAM/Identity + Security; separately authorized integration, production remains fail-closed without a verifier. |
| Missing supporting index, unsigned cursor policy and production login/image hardening | Backend/Database/Platform capacity; Backend/API governance + Security; Platform/DBA respectively, at unchanged plan §9 destinations. |
| Rate-limit/audit/connector, revocation timing, observability, source guards/JAR reproducibility and runtime environment | Existing Security/Platform, Backend/Security, Observability, QA/CI and environment owners at unchanged plan §9 destinations; not closed by this fix. |

Stop at `status: plan-fix-3-complete`, current gate `fix`. Only the two allowed documents are author outputs; no implementation, staging/commit/push, PR, reviewer dispatch, re-review entry or gate advancement. Final output hashes are published externally; Controller owns the next authorized action.
