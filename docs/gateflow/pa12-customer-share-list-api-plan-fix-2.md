# PA12 customer share list API — bounded plan fix 2

- Work unit: `PA12-customer-share-list-api`.
- Task label: `pa12-customer-share-list-api-plan-fix2-gpt-20261102-01`.
- Date: 2026-11-02. Current gate: `fix`. Status: `plan-fix-2-complete`.
- Authority: PA12 goal, closed decisions and re-review adjudication in `docs/gateflow/project-execution-control.md`. This is author plan-fix completion only, not review acceptance, gate advancement, implementation or runtime validation.
- Exact author write boundary: modify `docs/gateflow/pa12-customer-share-list-api-plan.md`; add this artifact. All source, tests, control, prior artifacts, CI, README, OpenAPI, design/security documents, `tmp/`, Git metadata and user/private files remain outside the write boundary.

## 1. Frozen input identities

All five supplied SHA-256 identities were recomputed from local bytes before editing and matched. The controller document was read only, including its PA12 goal and accepted precision adjudication.

| Frozen input | SHA-256 |
| --- | --- |
| Plan before fix-2, `docs/gateflow/pa12-customer-share-list-api-plan.md` | `96b1497ee0e9523a0f91273ec41ea514ef74e75e1b70dc96553df5f928c4679d` |
| Prior fix, `docs/gateflow/pa12-customer-share-list-api-plan-fix-1.md` | `51ca8df654f63b9824d904f73dd6f0d75b3a2eaaaf60da1909069db23f906230` |
| Re-review A, `docs/reviews/plan-review-20261102-131456.md` | `a27a0deb835a38ad5c5aba1307d510b22669b0b399570f693a48d844ca974af9` |
| Re-review B, `docs/reviews/plan-review-20261102-135249.md` | `e4b62a0014317055831c050a2d74e9990369022225a7197db929c568b6f58157` |
| Binding controller document, `docs/gateflow/project-execution-control.md` | `cdce5985c1a711f6843d2e6d23c65535ccc0bb8bd644ea3f21de8e0432484ae6` |

The pre-write plan copy and full-tree regular-file SHA-256 manifest were captured outside the repository in `/tmp/pa12-fix2-validation/`. Final output hashes are computed externally and returned to the controller, not embedded in either output; neither document contains a self-referential output hash.

## 2. Exact accepted findings and dispositions

| Accepted finding | Disposition | Concrete correction |
| --- | --- | --- |
| A R-01 — unqualified clean-tree/no-other-change assertion contradicts fix-1 drift | Applied; author disposition complete | §10 removes global equality/provenance claims, separately enumerates fix-1's seven concurrent `tmp/` additions and later control bytes, and distinguishes this interval's output comparison from Git or historical provenance. No unrelated path is edited, deleted or reverted. |
| A R-02 — both-root named-interface assertions lack a concrete write-set home | Applied; author disposition complete | §3 and §7 name existing `ModuleBoundaryTests.verifiesNoCyclesOrBoundaryViolations()` / `ApplicationModules.of(OneCallApplication.class)` and `rejectsInternalPackageAccessFixture()` / `ApplicationModules.of(InternalFixtureRoot.class, INCLUDE_TEST_FIXTURES)`. Both methods receive explicit positive named-target/member and negative bare-target/persistence/default-API model/source-policy assertions. The fixture method retains its actual provider/internal model rejection and separately obtains the production member model; it does not falsely claim its `consumer`/`provider` root discovers sharing. No new fixture/source file or test method is inferred. Both-root library probes remain supporting evidence, not a purported project replay. |
| B2-01 — missing/misspelled named-interface failure attributed to discovery alone | Applied; author disposition complete | §3 requires a throwable assertion around the entire `ApplicationModules.of(...).verify()` expression (or exact include-all equivalent), type `IllegalArgumentException`, message `No named interface named 'customer-query' found!`. `of(...)` and `getDirectDependencies()` return normally. Never expect `Violations` for this mistake class; generic internal-access fixture `Violations` remains a different, preserved case. No production annotation mutation is proposed to create a test failure. |
| B2-02 — fault context and cross-package mapper support need executable, inventory-safe homes | Applied; author disposition complete | §7 and §8.1#9 require programmatic `SpringApplicationBuilder(OneCallApplication.class, ...).profiles("web").properties("server.port=0", ...).run()` context construction/closure within the existing web method, following `CustomerMeFixture`'s current pattern. Nested fault configuration is test-free support, not `@Nested`, another `@SpringBootTest` class or an extra report. §7 / §8.1#6 choose the permitted duplicate literal normal web-mapper builder/configuration in the two new codec/web test files, excluding the fault module. No inaccessible cross-package helper, fifth new test/support source file or production bridge. |
| B2-03 — both Surefire aggregate sites must be enumerated | Applied; author disposition complete | §9 lists `.github/workflows/ci.yml:162-163` and `.github/workflows/ci.yml:284` explicitly, along with Failsafe `:285` and summary string `:286`. The 64-method migration literal set at `:147-159` and the two existing ten-method container sets remain frozen. |

No other finding was reopened, promoted into scope or weakened. These are precision corrections to the accepted plan, not new product or architecture choices.

## 3. Changed sections and unchanged boundary

- Header: add current task/gate/completion metadata while keeping historical original/fix-1 records explicitly historical.
- §3: concrete discovery-method/model/source-policy assertion homes and corrected whole-expression throwable assertion. Exact named seam, export breadth, members, map, composition exception, graph and negative fixtures remain binding.
- §7: cross-reference exact architecture methods; executable programmatic serialization-fault context; duplicate literal mapper support in the two existing allowlisted new files, with no new report/source class.
- §8.1: clarify only the existing codec wire method and web negotiation/fault method's support mechanisms. No method name or method/class count changes. §8.2 is unchanged.
- §9: enumerate both Surefire aggregates, Failsafe aggregate and summary-string sites. Existing residual owners, command instructions, firewall and frozen container inventories remain unchanged.
- §10: correct provenance wording, preserve separately enumerated drift/Git/user-data boundaries, and specify fix-2 manifest/hash validation and stop condition.

Exactly one end-to-end read-only S1 remains for `GET /api/v1/customer/shares`. Preserve server-attested selection, `/shares`-only structure validation after Accept, unchanged `/me`, creator/workspace isolation, ACTIVE lifecycle rechecks, frozen cursor/limit, secret-free projection, exact roles/error precedence/fault seams, and one `sharing::customer-query` transport-composition exception. No mutation, provider/session adapter/store, TTL/revocation policy, frontend, schema, V001–V009, grant, migration, index, OpenAPI, architecture/design/security, default configuration, dependency or endpoint expansion. Future implementation instructions are not authorization to edit implementation files now.

## 4. Plan-only validation and provenance

- Structure: ten numbered plan sections remain in sequence; unchanged one-S1 outcome. The five accepted findings have explicit dispositions, and the discovery methods, programmatic fault-context pattern, mapper location and all four aggregate/summary sites are named.
- Baseline re-derived from local Java sources with CI's `@Test\s+(?:public\s+)?void\s+(\w+)\s*\(` regex: 49 Surefire methods / 11 classes; 84 Failsafe methods / 3 classes, exactly `64+10+10`.
- Planned lists re-counted and compared with the frozen pre-write plan: exactly the same two session, six codec, ten web and eight container method names. No conditional, nested-test, parameterized or extra-report substitute is added. The existing eight `ModuleBoundaryTests` methods remain eight.
- Arithmetic: `2+6+10=18`, `49+18=67`, `11+2=13`; `84+8=92`, `3+1=4`, `64+10+10+8=92`; `67+92=159`. No arithmetic error or scope adjustment. Cursor bounds remain `46+ceil(21*128/8)=382` bytes and `ceil(382*8/6)=510` unpadded characters, within 512.
- Diff/structure checks: changes limited to header and §§3, 7, 8.1, 9, 10; unchanged §§1, 2, 4, 5, 6 and §8.2. No trailing spaces/tabs in either output. Frozen prior fix, both re-reviews and control hashes are rechecked after writing.
- Full-tree comparison for this fix-2 interval: only the plan's bytes change and this artifact is added; no other regular-file modification/addition/deletion is observed relative to the external pre-write manifest. This interval result does not assert global cleanliness or erase the independently disclosed fix-1 drift. Any newly observed drift must instead be enumerated and attributed only if independently known; never hidden by reverting it.

Historical drift remains explicitly separate: fix-1 disclosed `tmp/env-config/agent-tools.zsh`, `tmp/env-config/codex-agent-run`, `tmp/env-config/qwen38max-config.toml`, `tmp/env-config/sub-agents-SKILL.md`, `tmp/install_qwen38max.sh`, `tmp/probe_qwen_max.sh`, `tmp/probe_qwen_models.sh`, and later control-document bytes. Their producers are not attributed here, and none is edited/deleted. A no-new-drift result during fix-2 is not a retroactive clean-tree result for fix-1.

`git status --short` was attempted read-only and failed because `.git` points to the unusable Windows-style `D:/codex-call/.git/worktrees/codex-call10` path from Linux. No head/index/clean-tree provenance is claimed or repaired. No Maven, Java launcher, library probe, Docker/container, HTTP, npm, GitHub, network or external action was performed in fix-2; executed library evidence is attributed to the frozen reviews, not newly executed project validation. All validation here is local, plan-only structural/hash/arithmetic inspection.

## 5. Residual owners, destinations and stop condition

| Residual | Owner / destination |
| --- | --- |
| Project implementation and executable acceptance evidence, including discovery assertions, inventory-safe fault context/mapper and CI aggregate updates | Backend QA + Architecture/CI; future authorized S1 implementation under the frozen plan and method/class budgets. No project runtime pass is claimed here. |
| Unusable Git provenance and historical concurrent control/`tmp/` drift | Workspace controller/environment owner; reconcile independently before any global provenance claim. No metadata repair, user-data access expansion or unrelated reversion in this task. |
| Composition exception, allowed-but-not-implemented cycle-closing edges, recorded security-stack reconciliation | Architecture + Gateflow controller; future authorized docs/12 reconciliation/composition extraction before implementing cycle-closing edges. No design edit here. |
| Trusted selection issuance, provider-neutral session adapter/storage, TTL/revocation | Customer IAM/Identity + Security; separate authorized integration; production stays fail-closed without a verifier. |
| No supporting list index, unsigned cursor policy, production login/image hardening | Backend/Database/Platform capacity; Backend/API governance + Security; Platform/DBA respectively, at the unchanged §9 destinations. No index/migration/key-policy/provisioning additions to S1. |
| Existing rate-limit/audit/connector, in-flight revocation, observability, source-guard/JAR reproducibility and runtime environment residuals | Existing Security/Platform, Backend/Security, Observability, QA/CI and environment owners; unchanged §9 destinations, not silently closed by this documentation fix. |

Stop at `status: plan-fix-2-complete`, current gate `fix`, after these two documents, plan-only verification and external publication of both output hashes. No implementation, staging, commit, push, PR, reviewer dispatch, re-review entry or gate advancement. Gateflow controller owns the next authorized action and subsequent independent review; no closed decision is reopened.
