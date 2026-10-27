# PA0 production runtime bootstrap — bounded plan fix 2

- Task label: `pa0-plan-fix2-ds-20261026-01`
- Gate: `fix` (bounded plan fix only; no implementation, no re-review)
- Work unit: `PA0-production-runtime-bootstrap`
- Base commit: `1686a22ac9f7715b876ac233d7202badccfd0569`
- Target plan: `docs/gateflow/pa0-production-runtime-bootstrap-plan.md`
- Initial review: `docs/reviews/plan-review-20261026-224322.md` (historical, immutable)
- Fix 1 artifact: `docs/gateflow/pa0-production-runtime-bootstrap-plan-fix-1.md` (historical, immutable)
- Re-review 1: `docs/reviews/plan-review-20261026-230211.md` (historical, immutable)
- Controller adjudication: PA0 gate record at the top of `docs/gateflow/project-execution-control.md` (pre-existing dirty path, not modified by this task)
- Date: 2026-10-26

## 1. Conclusion

Exactly one controller-promoted finding, `F-PA0-RR-01`, is fixed by replacing the invalid Adoptium citation in the current plan with the exact parameterized controller query URL and stating the response fields precisely. The four closed initial findings `F-PA0-PR-01`…`F-PA0-PR-04` were not revisited. No goal, non-goal, two-slice structure, version pin, source code, test, workflow, control document or prior artifact was changed.

**Final status: `已修复` for `F-PA0-RR-01`.**

## 2. Finding disposition

### F-PA0-RR-01 — invalid Adoptium evidence URL in the plan

- Severity per controller promotion: bounded evidence-citation defect.
- **HTTP evidence (controller, 2026-10-26):**
  - `https://api.adoptium.net/v3/info/feature_releases/21/ga` → **HTTP 404** (`curl --max-time 15`), so the plan's citation was not a valid authoritative query.
  - `https://api.adoptium.net/v3/assets/feature_releases/21/ga?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse&page_size=5` → **HTTP 200**, with a release entry carrying `release_name=jdk-21.0.13+9`, `openjdk_version=21.0.13+9-LTS`, and LTS metadata.
- **Before (plan §4.1 Java row, base hash `3377F1DB5E1035C82287BD2A51B006A40DF44E40400F66AA68DB090EAA6D3DE9`):**
  - "controller 于 2026-10-26 查询 Adoptium feature-release API（`https://api.adoptium.net/v3/info/feature_releases/21/ga`）返回 `jdk-21.0.13+9`，标记 LTS；…"
- **After (plan §4.1 Java row, fixed hash `5C3DC6C2FB9E0FF976B566249CEFBA49E64CCF4068573224190A8E670B317D51`):**
  - "controller 于 2026-10-26 以参数化查询 Adoptium assets/feature-release API（`https://api.adoptium.net/v3/assets/feature_releases/21/ga?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse&page_size=5`）获得 HTTP 200，release entry 返回 `release_name=jdk-21.0.13+9`、`openjdk_version=21.0.13+9-LTS` 及 LTS metadata；…"
- Unchanged by this fix: the `21.0.13+9` / `21.0.13+9-LTS` pin string, `maven.compiler.release=21`, Enforcer `[21,22)`, the diagnostic-only status of `21.0.12.1+1`, the §4.2 `CONTROLLER_OBSERVED` / `PREFLIGHT_REQUIRED` tiers, and the S1 fail-closed installability/runtime proof (`java -version` / `./mvnw -v` must show patch `21.0.13` + Temurin build `+9`; an unresolvable `jdk-21.0.13+9` blocks S1).
- **Supersession:** the historical fix-1 artifact (§2 `F-PA0-PR-03`) and re-review-1 artifact (line 20 / §3 `F-PA0-PR-03` / `RSK-RR-01`) preserve the earlier `info/feature_releases` citation as their historical record and are intentionally not rewritten. For the **current** plan, this fix-2 artifact and the amended §4.1 citation supersede that URL; the current plan contains no `api.adoptium.net/v3/info/feature_releases` text.

## 3. Changed files

Exactly two paths were written by this task:

1. `docs/gateflow/pa0-production-runtime-bootstrap-plan.md` (amended; pre-existing untracked path)
2. `docs/gateflow/pa0-production-runtime-bootstrap-plan-fix-2.md` (new fix artifact)

No other path was created or modified. The control document, prior fix artifact, initial review, re-review 1, specs, source, tests, workflows, screenshots, prototypes, `.codex-tasks/` and `tmp/` remain untouched.

## 4. Validation

- Starting plan hash verified **before** editing: `3377F1DB5E1035C82287BD2A51B006A40DF44E40400F66AA68DB090EAA6D3DE9` (matches the controller-expected plan fix-1 hash). Line count then: 1341.
- Fixed plan SHA-256: `5C3DC6C2FB9E0FF976B566249CEFBA49E64CCF4068573224190A8E670B317D51`. Line count: 1341 (single table-cell replacement; no line add/remove).
- `git diff --check -- docs/gateflow/pa0-production-runtime-bootstrap-plan.md`: clean, exit 0, no output. The plan is untracked, so tracked-diff output is empty by construction; the direct checks below cover it.
- Direct CR (`\r`) scan over the fixed plan: 0 matches.
- Direct trailing-whitespace (`[ \t]+$`) scan over the fixed plan: 0 matches.
- Direct CR and trailing-whitespace scans over this fix artifact: 0 matches (written without either).
- Citation searches over the current plan:
  - `api.adoptium.net/v3/info/feature_releases`: **0 matches**.
  - `api.adoptium.net/v3/assets/feature_releases`: exactly **1 match**, at §4.1 line 196, with the full parameter string.
- The same `info/feature_releases` string remains only in the immutable historical fix-1 artifact and the control document's adjudication prose; both were left unmodified as required.
- Binding artifacts re-hashed before reliance:
  - initial review `c7955d37862336cda6d09e3b257c3984d97e0d57adf863099afac23e09e8736f`
  - fix-1 artifact `c5418a756ba124efbcad107e3a7f9a9b987ba7e077b9e901ff51433f546d900b`
  - re-review 1 `f7f73e470ebfc248fd749740079d9a01e184cdab4aace37bf407cbd0398458c8`
  - each matches the value recorded when its gate closed; none was modified in this task.
- Changed-file proof: `git status --porcelain docs/gateflow` lists the same pre-existing ` M` paths and the same pre-existing untracked `pa0-production-runtime-bootstrap-plan.md` / `pa0-production-runtime-bootstrap-plan-fix-1.md` as at task start, plus exactly one new untracked entry, `pa0-production-runtime-bootstrap-plan-fix-2.md`. No delete/rename/stage occurred. Pre-existing dirty paths elsewhere in the tree (specs, reviews, screenshots, prototypes, `deployment/`, `protocol/`, `security/`, `shared/`, `logo/`, `.codex-tasks/`) are untouched by this task.
- Scope confirmation: `F-PA0-PR-01..04` fixes (exact allowed-dependency map + four probes, explicit-`{}` deny-all leaves, exact Temurin `21.0.13+9` pin mechanics, three-state S2 Docker evidence) are unchanged; Goal Confirmation, non-goals, A0–A5 boundaries and the two-slice S1/S2 structure are unchanged.
- This fix artifact's own SHA-256/line count are computed after write and returned in the controller-facing completion report; a file cannot embed its own final hash.

## 5. Residual risks

- `RSK-RR-01` (Low, from re-review 1) is now resolved for the current plan: the citation is the controller-proven HTTP 200 assets URL. The historical artifacts keep the old citation by design; readers must treat fix-2 as the superseding source.
- `PA0-R-10` / OQ-RR-01 remains: S1 must still prove live installability of exact Temurin `21.0.13+9` in the execution environment via `java -version` / `./mvnw -v`; unresolvable release is a fail-closed S1 blocker.
- All other previously accepted residuals (`PA0-R-01`…`PA0-R-13`, `RSK-RR-02`, `RSK-01`…`RSK-07`, `VQ-02`, `VQ-05`) are unchanged and remain owned as classified in the plan/re-review.

## 6. Artifact path

- This fix artifact: `docs/gateflow/pa0-production-runtime-bootstrap-plan-fix-2.md`
- Fixed plan: `docs/gateflow/pa0-production-runtime-bootstrap-plan.md`

## 7. Unchanged scope and honesty statement

This task only amended one citation in the plan and created this fix artifact. It did not implement source, tests, workflows or the JDK pin file; did not modify the control document, prior review/fix artifacts, specs, prototypes, screenshots, `.codex-tasks/` or `tmp/`; and did not stage, commit, push, create/edit PRs or issues, merge, switch branches, install software, start services/Docker, or enter re-review/implementation.
