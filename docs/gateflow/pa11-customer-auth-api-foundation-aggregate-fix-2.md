# PA11 aggregate deepreview fix 2

- Task label: `pa11-aggregate-fix2-gpt-20261102-01`
- Gate: `aggregate deepreview -> re-review -> fix`
- Work unit: `PA11-customer-auth-api-foundation`
- Status: `aggregate-fix-2-complete`

## Scope

This bounded fix addresses only the two controller-accepted post-fix re-review findings. No product behavior was changed beyond moving the existing package-private trace helper to the class that owns the trace request attribute. No tests, test counts, plan, control, prior review/fix artifacts, OpenAPI, migrations, design/security documents, generated/private files, commit, stage, push, PR, or review-gate action was modified or performed.

## Accepted findings

1. Reviewer A F-1 / Reviewer B N1: the live CI validation narrative in `backend/README.md` now states 49 Surefire methods in 11 classes, 84 Failsafe methods in 3 classes (`64+10+10`), 133 combined, zero failures/errors/skips, default and opt-in web packaged-JAR smokes, and the production-JAR firewall with 82 adversarial probes. Historical PA checkpoints remain preserved.
2. Reviewer B N2: the package-private `trace(HttpServletRequest)` helper moved from `CustomerMeController` to `CustomerBoundaryFilter`, beside `TRACE_ATTRIBUTE`. Both `CustomerMeController` and `CustomerApiErrorController` now call `CustomerBoundaryFilter.trace(request)`. Fallback UUID generation, response shape, mapping, and bean set remain unchanged.

## Rejected finding

- Reviewer B N3 remains rejected by the controller: the defensive account/channel/PERSONAL/status validation is explicitly required by accepted plan sections 5-6 and the frozen six-method service test. It is deterministic defense-in-depth with no runtime defect and is not removed or reinterpreted in this work unit.

## Changed paths

- `backend/README.md`
- `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`
- `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerApiErrorController.java`
- `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerMeController.java`
- `docs/gateflow/pa11-customer-auth-api-foundation-aggregate-fix-2.md`

## Validation

- Confirmed the former helper owner has no remaining source references.
- Confirmed both controllers call `CustomerBoundaryFilter.trace`.
- Confirmed the README no longer describes the live job as only 64 tests and a single smoke; it now contains the complete executable contract above.
- Focused Maven command attempted: `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -f backend/pom.xml -B -ntp -Dtest=CustomerSessionBoundaryTests,CustomerMeWebTests test`. The runner outcome is reported separately because sandbox/runtime permissions may prevent web binding or test-agent attachment.
- `git diff --check` passed through the mounted Git metadata; the default worktree Git link itself points at an invalid Windows-form `.git` path. The mounted worktree contains unrelated pre-existing changes, so no single clean product diff is claimed here.

## Residual owner items

- Controller/Backend QA owns any rerun of the focused 13-test command when web binding and test-agent attachment are permitted.
- Reviewer A F-2 post-fix JAR/diff hash remains controller-owned and was not calculated or changed by this author.

## Artifact

This artifact hash is calculated after all edits and is reported in the final handoff; it is not embedded here to avoid self-reference.

## Correction 2b

Controller intake found that the previous edit at line 39 missed the reviewer-cited live CI bullet. The line-289 `java-runtime` bullet is now corrected.
