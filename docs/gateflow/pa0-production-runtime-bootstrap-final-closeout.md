# PA0 production runtime bootstrap final closeout

## Work unit

- ID: `PA0-production-runtime-bootstrap`
- Draft PR: #8, https://github.com/zh94695424/one-call/pull/8
- Branch: `codex/pa0-production-runtime-bootstrap`
- Final reviewed-content head: recorded by the accepted PR-review checkpoint and final push in `docs/gateflow/project-execution-control.md`.

## What changed

- Added the Boot `4.1.2` / Spring Modulith `2.1.2` Java 21 runtime scaffold with 14 explicit Modulith package boundaries.
- Added exact allowed-dependency assertions, deny-all leaf checks, cycle/forbidden/internal negative fixtures, classpath dependency firewall checks, boot context smoke and production-jar firewall checks.
- Added the explicit `container-smoke` Maven profile and real PostgreSQL 16 Testcontainers JDBC smoke with fail-closed lifecycle and no skip path.
- Added the read-only GitHub Actions workflow for Java/container smoke and A1-A5 contract regressions, then corrected the Temurin provider coordinate and Maven wrapper executable bit after the first CI run.
- Aligned the active PA0 plan with the provider coordinate `21.0.13+9.0.LTS` and semantic pin `21.0.13+9`.

## Verification

- Local controller evidence: default `clean verify` passed with 10 tests and no skips; `-Pcontainer-smoke verify` passed with one PostgreSQL IT, `tests=1`, `skipped=0`, `failures=0`, `errors=0`; boot jar smoke and runtime dependency firewall passed.
- Local A1-A5 regressions passed with the recorded S3/S2 validators.
- GitHub Actions run `40423035301` passed both `java-runtime` and `contract-regressions` on code head `b779515b88ab54a725c9c8dfecf7c722579ba733`.
- GitHub Actions run `40424979426` passed both jobs on the exact reviewed PR head `fc3cc8e2f4318fd18ef48c7b382d5fa7d2e822a0`: `java-runtime` completed the exact toolchain assertions, Docker requirement, `-Pcontainer-smoke verify`, failsafe report assertions, boot-jar smoke and dependency firewall; `contract-regressions` completed A1-A5. This is the final PR-head CI evidence.
- PR re-review `docs/reviews/pr-8-review-20261027-080332.md` passed after closing `PR8-01`.

## Review and findings

- S1 and S2 implementation reviews passed.
- The PR review initially found one medium documentation/source-of-truth drift (`PR8-01`): the active plan used the pre-fix setup-java coordinate. The plan was corrected and the re-review closed the finding.
- The re-review artifact references the earlier green run `40423035301`; after the accepted review checkpoint was pushed, run `40424979426` reran both jobs against the exact reviewed PR head and passed.
- No blocking open question or unclassified residual remains.

## Documentation

- Active plan: `docs/gateflow/pa0-production-runtime-bootstrap-plan.md`.
- Execution controller: `docs/gateflow/project-execution-control.md`.
- PR review artifacts: `docs/reviews/pr-8-review-20261027-080113.md` and `docs/reviews/pr-8-review-20261027-080332.md`.

## Remaining risks and owners

- `PA0-R-05`: floating `postgres:16` test-only image tag; Backend/Platform, later CI hardening.
- `PA0-R-07`: Actions major-tag pinning; Security/Platform, CI hardening/release governance.
- `PA0-R-08`: path filters and branch-protection required-check interaction; Platform/CI repository governance.
- `PA0-R-09`: future cross-cutting module edges require architecture review; Backend/Architecture, first domain work unit.
- `PA0-R-12`: formatter/checkstyle remains outside this bounded work unit; Backend/Platform, later code-quality work.
- `PA0-R-13`: allowed-dependency map follows the current architecture prose; Architecture, architecture change control.
- `PA0-R-14`: Spring Modulith `2.1.x` is recorded as `support=none`; Platform/Security and release governance accepted this user-required target residual.
- The work unit provides build/static/synthetic evidence only. It does not establish production acceptance, live carrier/IVR behavior, Tencent Cloud capability, load, restore, penetration, or manual acceptance.

## Closeout state

`PA0-production-runtime-bootstrap` reached `final closeout pass`. Draft PR #8 is Open, Draft, and has a clean merge state; it remains unmerged by design. After the user merges PR #8, update local `main` from `origin/main` and begin fresh goal confirmation for the next bounded Phase A work unit; do not continue domain implementation from this unmerged branch.
