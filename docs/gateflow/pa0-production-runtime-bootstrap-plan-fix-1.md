# PA0 production runtime bootstrap — bounded plan fix 1

- Task label: `pa0-plan-fix1-ds-20261026-01`
- Gate: `fix` (bounded plan fix only; no implementation, no re-review)
- Work unit: `PA0-production-runtime-bootstrap`
- Base commit: `1686a22ac9f7715b876ac233d7202badccfd0569`
- Target plan: `docs/gateflow/pa0-production-runtime-bootstrap-plan.md`
- Accepted review: `docs/reviews/plan-review-20261026-224322.md`
- Controller adjudication: PA0 section at the top of `docs/gateflow/project-execution-control.md` (pre-existing dirty path, not modified by this task)
- Date: 2026-10-26

## 1. Conclusion

All four controller-accepted findings (`F-PA0-PR-01`…`F-PA0-PR-04`) are fixed in the plan as bounded textual/contract corrections. No goal, non-goal, success-signal scope, two-slice structure, source code, tests, workflow, control document or review artifact was changed. The two deny-all / Docker-supply open questions are closed with controller-proven evidence; the Boot support-window and Maven checksum questions remain fail-closed S1 preflights.

**Final status: `已修复` for `F-PA0-PR-01`, `F-PA0-PR-02`, `F-PA0-PR-03` and `F-PA0-PR-04`.**

## 2. Finding dispositions

### F-PA0-PR-01 — exact allowed-dependency map is not executable-asserted

- Severity: Medium. Status: `已修复`.
- Before: §6.4 only asserted the exact module name set and `@ApplicationModule` presence; `verify()` ran on an empty production edge graph, so a widened/narrowed/omitted configured allow-list could still pass. §13.6 only recorded an "allowedDependencies table hash".
- After:
  - §6.4 adds one explicit `Map<String, Set<String>> EXPECTED_ALLOWED` that mirrors the §6.2 table row-for-row, including leaf empty sets.
  - New `assertsExactAllowedDependencyMapFromPlan62` reflects each production `package-info` `@ApplicationModule.allowedDependencies()` and asserts exact set equality with that map. It also explicitly rejects the default `OPEN_TOKEN` value.
  - Existing exact module-set assertion is retained (`discoversExactlyTheFrozenModuleSet`); annotation presence is retained as a dedicated `everyExpectedModuleDeclaresApplicationModule` test.
  - New `discoveredProductionEdgesStayInsideTheConfiguredMap` asserts every Modulith-discovered direct dependency is a subset of the configured map and that the global discovered edge set is empty (no cross-module marker edges yet). This does not weaken the configured map equality.
  - §6.4 defines four stable mutation probes that must make `ModuleBoundaryTests` fail and turn green again after restore: widen `ivr` +`catalog_order`; narrow `ivr` −`subject_number`; omit a leaf attribute (bare `@ApplicationModule`, default `OPEN_TOKEN`); change `organization_access` `customer_iam`→`ivr`. S1 must record all four results, not a report hash alone.
  - §11.1 invariant 2 and §13.6 report fields now require the map assertion and probe results; the report-hash-only field is removed.
- Verification point: any widening, narrowing, omission or value change of one configured dependency fails a test by construction; the four probes give a fixed reproduction procedure.

### F-PA0-PR-02 — leaf deny-all encoding is ambiguous/wrong

- Severity: Medium. Status: `已修复` (no ArchUnit added).
- Before: §6.3 instructed "无 outgoing edge 的模块写 `@ApplicationModule`（空 allowedDependencies）", which is the library default `{ OPEN_TOKEN }` (unrestricted); §6.2/§6.5/§13.5 left "empty might not be strict" as an unresolved fallback.
- After:
  - §6.2 records the controller-inspected Spring Modulith `1.4.14` semantics: `allowedDependencies()` defaults to `{ OPEN_TOKEN }` (`OPEN_TOKEN = "¯\\_(ツ)_/¯"`) meaning unrestricted; the published Javadoc states "Declaring an empty array will allow no dependencies to other modules. To not restrict the dependencies at all, leave the attribute at its default value."; core `ApplicationModule.getAllowedDependencies()` treats only the singleton `OPEN_TOKEN` as open and otherwise constructs closed allowed dependencies. Therefore explicit `allowedDependencies = {}` is deny-all.
  - §6.3 now requires **explicit** `@ApplicationModule(allowedDependencies = {})` for every leaf module (`customer_iam`, `subject_number`, `operations_audit`, `platform_reliability`) and bans bare `@ApplicationModule` for leaves.
  - §6.4's exact-map test rejects the default `OPEN_TOKEN` for every module.
  - §6.5's forbidden fixture now declares explicit `allowedDependencies = {}` and must prove that the consumer→provider edge throws `Violations`.
  - §13.5 stop condition is narrowed to "implemented Modulith behavior diverges from the published `1.4.14` explicit-`{}` deny-all semantics, or the negative tests would have to be weakened/deleted"; the speculative "maybe empty is not strict" stop/question wording is removed.
- Verification point: forbidden fixture with explicit `{}` must fail closed; leaf default-open is rejected by the exact-map test.

### F-PA0-PR-03 — Java patch is not exact-pinned

- Severity: Medium. Status: `已修复`.
- Before: Java was pinned only as `21`/Temurin `21`; CI used `java-version: '21'`; residual `PA0-R-10` deferred the patch pin.
- After:
  - §4.1 pins **Temurin `21.0.13+9`** (OpenJDK version `21.0.13+9-LTS`), citing the official Adoptium feature-release API `https://api.adoptium.net/v3/info/feature_releases/21/ga` retrieved 2026-10-26, which returned `jdk-21.0.13+9` marked LTS. The prior installed `21.0.12.1+1` is explicitly marked diagnostic, not the selected pin.
  - §4.5 adds one minimal repository pin file `backend/.java-version` containing exactly `21.0.13+9`, justifies `backend/` over the repository root (only Java build lives there; root is Node contract harness), and explicitly forbids an installer script or global installation action.
  - §9.3 uses `actions/setup-java@v4` with `distribution: temurin`, exact `java-version: '21.0.13+9'`, no `check-latest`; the toolchain step now asserts `.java-version`, Maven 3.9.9, Java patch `21.0.13` and Temurin build `+9`.
  - `maven.compiler.release=21` is retained; §4.3/S1 preflight must prove patch `21.0.13` and build `+9`; S1/controller must select or install the exact JDK before claiming S1 validation, and an environment that cannot resolve `jdk-21.0.13+9` is a fail-closed S1 blocker.
  - §3.1/§3.2 add the pin file; §11.1 allowed paths/commands/completion include it; §13.3 `PA0-R-10` is updated to the exact pin (no deferred exact-JDK residual); no major-only success statement remains for the pin.
- Verification point: `java -version` / `./mvnw -v` and CI assertions both require patch `21.0.13` + build `+9`; changing the pin fails conformance.

### F-PA0-PR-04 — S2 Docker completion predicate conflicts with Gateflow sequencing

- Severity: Medium. Status: `已修复`.
- Before: §11.2 said S2 was incomplete without a Docker-capable run while also allowing `NOT RUN` to be handed to controller/PR, leaving a first-CI-only path ambiguous.
- After: S2 completion is split into three non-substitutable states, aligned across §1.2/§1.3, §7.4, §9.6, §11.2, §12, §13.3, §13.5 and §13.6:
  1. **author code-complete** — files/workflow/profile exist and static/local non-container checks pass; author Docker may be `NOT RUN` only with exact reason; this is **not** an accepted slice.
  2. **controller container-evidence complete** — before the S2 accepted-slice checkpoint, controller must run `./mvnw -B -ntp -Pcontainer-smoke verify` in a Docker-capable environment and prove exactly one intended test with `skipped=0`, `failures=0`, `errors=0` plus the SQL assertions. If the controller daemon is unavailable, Gateflow must stop and request Docker; it must not accept/commit S2 and must not defer this primary proof to a later PR run.
  3. **post-push CI evidence complete** — after later Gateflow push/draft-PR gates, real GitHub Actions jobs must pass before `draft-PR-pass`/final closeout. This is additional workflow evidence, not a substitute for state 2.
  - §11.2 adds an explicit accept/no-accept matrix; `NOT RUN` is allowed only on the author row, and an accepted S2 checkpoint requires `controller_container_smoke=PASS`.
  - §13.6 adds `S2 author_code_complete`, `S2 controller_container_smoke`, and `S2 post_push_ci_container_smoke` fields and states the controller-PASS requirement.
- Verification point: no reading of `NOT RUN` can produce an accepted S2 checkpoint; the controller pre-push run is the sole acceptance trigger.

## 3. Open-question cleanup

- **Closed — deny-all semantics (review OQ-01):** explicit `allowedDependencies = {}` is deny-all and bare default `OPEN_TOKEN` is unrestricted, per published Modulith `1.4.14` source/Javadoc; recorded in §6.2/§6.3/§6.4/§6.5.
- **Closed — Docker supply (review OQ-02):** controller pre-push Docker-capable run is the S2 accepted-checkpoint requirement; post-push CI is additional evidence only; recorded in §7.4/§9.6/§11.2/§13.4.
- **Retained fail-closed S1 preflights (not accepted review findings, not guessed):** `VQ-02` Maven 3.9.9 distribution SHA-256; `VQ-05` Boot 3.5 support/security window.
- **Retained classified residuals:** floating `postgres:16` (`PA0-R-05`/`VQ-04`), Actions major-tag supply chain (`PA0-R-07`/`VQ-06`), path-filter/branch-protection interaction (`PA0-R-08`), future cross-cutting edges (`PA0-R-09`/`VQ-07`), single-module future restructure (`VQ`/`RSK-03` class), formatter/checkstyle gap (`PA0-R-12`), prose-to-cap translation drift (`PA0-R-13`).

## 4. Changed files

Exactly two paths were written by this task:

1. `docs/gateflow/pa0-production-runtime-bootstrap-plan.md` (amended; pre-existing untracked path)
2. `docs/gateflow/pa0-production-runtime-bootstrap-plan-fix-1.md` (new fix artifact)

No other path was created or modified. The pre-existing dirty/untracked files (including `docs/gateflow/project-execution-control.md`, specs, reviews, screenshots, prototypes, `.codex-tasks/`, `tmp/`) were not touched.

## 5. Validation

- `git diff --check -- docs/gateflow/pa0-production-runtime-bootstrap-plan.md`: clean (no output, exit 0). The plan is untracked, so the tracked-diff check is empty by construction; direct checks below cover it.
- Direct trailing-whitespace scan (`[ \t]+$`) over the fixed plan: 0 matches.
- Direct CR (`\r`) scan over the fixed plan: 0 matches.
- Original plan SHA-256: `D4A8E3DAC7A716BBD4ED3D0ACE816C2EE07DAEBB798D76ED6071423FD34F803E`.
- Fixed plan SHA-256: `3377F1DB5E1035C82287BD2A51B006A40DF44E40400F66AA68DB090EAA6D3DE9`.
- Fixed plan line count: 1341.
- Fix artifact SHA-256/line count: computed externally after write and returned in the controller-facing completion report; a file cannot embed its own final hash.
- Accepted review artifact SHA-256 re-checked: `c7955d37862336cda6d09e3b257c3984d97e0d57adf863099afac23e09e8736f` matches the controller record and is unmodified.
- Scoped `git status --porcelain` shows the control document as the pre-existing ` M` path, the accepted review and the plan as pre-existing untracked paths, and this fix artifact as the only new path; this task did not write the control document or review.
- Stale-wording searches over the fixed plan:
  - bare-leaf annotation guidance: absent; §6.3 now requires explicit `allowedDependencies = {}`.
  - default `OPEN_TOKEN` ambiguity: absent as an unresolved question; semantics are proven and the exact-map test rejects the token.
  - major-only `java-version: '21'`: absent; CI uses `21.0.13+9`.
  - deferred exact-JDK residual: absent; `PA0-R-10` is updated to the exact pin + preflight-blocker destination.
  - ambiguous S2 `NOT RUN` acceptance: absent; §11.2 defines three states and the accept matrix.
  - first-CI-only completion language: absent; post-push CI is explicitly additional and non-substituting.
- Binding scope unchanged: §1.1/§1.4 goals and non-goals, success signals except the accepted pin/Docker/assertion corrections, the two-slice structure, S1/S2 behavior boundaries and all A0-A5 non-goals are preserved.

## 6. Residual risks

- `PA0-R-01`/`VQ-01`: Boot/Modulith/Testcontainers GA compatibility still requires S1 Maven resolution evidence.
- `PA0-R-02`/`VQ-02`: Maven 3.9.9 zip SHA-256 still requires S1 retrieval/verification; omission is a blocker.
- `PA0-R-03`: controller Docker daemon must be started before S2 acceptance; Gateflow stops otherwise.
- `PA0-R-04`: real GitHub Actions evidence remains post-push.
- `PA0-R-05`: `postgres:16` remains a floating test tag without digest/patch pin.
- `PA0-R-06`/`VQ-05`: Boot 3.5 support/security window remains a fail-closed S1 preflight.
- `PA0-R-07`/`PA0-R-08`: Actions commit-SHA hardening and path-filter/branch-protection interaction remain Platform/Security/CI governance residuals.
- `PA0-R-09`: future cross-cutting Java edges require explicit §6.2 revision and architecture review.
- `PA0-R-10`: exact Temurin `21.0.13+9` is pinned, but the execution environment must select/install it before S1; an unresolvable release blocks S1.
- `PA0-R-11`..`PA0-R-13`: CI-success semantics, missing formatter/checkstyle and prose-to-cap drift remain as previously classified.

## 7. Artifact path

- This fix artifact: `docs/gateflow/pa0-production-runtime-bootstrap-plan-fix-1.md`
- Fixed plan: `docs/gateflow/pa0-production-runtime-bootstrap-plan.md`

## 8. Unchanged scope and honesty statement

This task only amended the plan and created this fix artifact. It did not implement source code, tests, workflows or the repository pin file; did not modify the control document, review artifact, specs, prototypes, screenshots, `.codex-tasks/` or `tmp/`; and did not stage, commit, push, create/edit PRs or issues, merge, switch branches, start Docker/services, install software, or enter re-review/implementation. All pre-existing dirty paths remain untouched.
