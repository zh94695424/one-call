# PA31 Customer Subject Activation Foundation — Implementation Fix-4

## 1. Identity, authority and result

- Task: `pa31-subject-activation-implementation-fix4-gpt-20261113-01`; date: 2026-11-13; existing S1 of `PA31-customer-personal-subject-activation-foundation` only.
- Runtime: supplied Codex CLI, Linux/WSL/bash; no subagents/provider switch. Exact executing model/provider metadata is not independently attested. Toolchain: `/opt/jdk21`, `/opt/maven-3.9.9/bin/mvn`; approval policy `never`, no escalation.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa31-next-work-unit/codex-call`; branch `codex/pa31-next-work-unit`; unchanged HEAD `297e7c2e349d5cd6f0bb45dd94309a4ec0311137` and empty staged diff.
- Inputs: Active PA31 Control, cumulative accepted plan/fixes/final re-reviews, S1 implementation/fix-1/fix-2/fix-3, independent A `docs/reviews/code-review-20261113-150800.md` and B `docs/reviews/code-review-20261113-161810.md`. Only A's three Controller-accepted findings are addressed; no redesign or implementation restart.
- Outcome: **uncommitted bounded fix candidate for Controller PG16 rerun**. No post-fix PostgreSQL/full-suite acceptance or later-gate readiness is claimed.

Controller independently confirmed the pre-fix candidate passed clean PG16: Surefire59/520 + Failsafe25/404 =84/924, zero failures/errors/skips; focused34/0/0/0; source=CI=XML, JAR/web smoke, 116 dependency probes, OpenAPI `check:s3`, frozen20/exact40. These are supplied Controller baseline results, not this child's post-fix execution.

## 2. Accepted finding dispositions

### PA31-CR-A-01 — Medium — 已修复

`organizationAllAllowedRolesCanActivateAccessibleSubject` retains its identity and all four role cases: ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER and AUDITOR. Each case now uses the real HTTP route with the fixture's organization verified session, boundary filter, controller, command composer and real owner/database service, asserting200/ACTIVE and the exact successful eight-counter write set. It no longer substitutes a hand-built ActivationCaller for that path.

`CustomerSubjectActivationFixture.reset()` now seeds organization Y in addition to authorized organization X; the existing ACTIVE membership remains only for X. `foreignPersonalAndOrganizationSubjectsAreNotFound` retains all prior personal cases and adds X's organization caller against Y's subject directly and over HTTP, plus an absent-subject HTTP control. Both HTTP denials assert404/NOT_FOUND, exactly type/title/status/code/traceId, application/problem+json, no-store/nosniff and no account/organization/subject UUID disclosure. Every denial preserves all eight domain/database counters at zero: consent, activation head, transition, trial, entitlement grant, balance, Outbox and audit.

`verifiedSessionSuppliesAccountChannelAndWorkspace` retains personal coverage and adds an organization VerifiedCustomerSession. A spoofed organization parameter/role header does not replace the selected organization: the real command composer supplies the expected organization ActivationCaller to the owner seam. This web assertion executes locally; the expanded real PG16 organization/isolation matrix is delivered and compiled but awaits Controller execution.

### PA31-CR-A-02 — Low — 已修复

Existing `acceptUsesExactRouteAndFrozenResultShape` and real-HTTP `storedResultsProblemsAuditOutboxAndLogsAreRedacted` now assert the frozen success contract rather than just names:

- Exactly six top-level keys: activationStatus, activationRevision, transitionId, consentId, trialOutcome, trial; exactly two trial keys: grantedSeconds, expiresAt.
- String ACTIVE status; positive integral activationRevision; string transitionId/consentId parsed by UUID.fromString and equal to their canonical UUID.toString forms.
- String GRANTED for this fresh success; positive integral grantedSeconds; string expiresAt parsed and round-tripped with Instant.parse/toString.

No response serializer/OpenAPI/value contract changes; no new test identities. The web assertions pass locally; the real-database HTTP assertions compile and await Controller execution.

### PA31-CR-A-03 — Low — 已修复

Only CustomerSubjectActivationsController gains a private route-specific8192-byte maximum. After the existing authenticated/trusted-context preflight, readNBytes(8193) bounds allocation independently of Content-Length. An8193-byte result is rejected through the existing VALIDATION_FAILED/422 path before JSON parsing or activate; bodies at/below8192 bytes still go through the existing closed binder. There is no global framework, dependency, schema or OpenAPI change.

`bodyRejectsMalformedUtf8TrailingJsonAndInvalidLengths` keeps all existing cases and adds valid JSON padded with whitespace to8193 bytes. Both normal declared-length and simulated unknown-length/chunked servlet requests assert the frozen422 problem/headers; the owner command/caller capture lists remain empty (no domain invocation/write). A valid padded8192-byte request succeeds, proving the inclusive boundary.

Existing `webAndMiniappUseTrustedChannelAndClosedRequest` additionally sends oversized valid JSON through the actual HTTP server with both BodyPublishers.ofString (known length) and BodyPublishers.ofInputStream (unknown length/chunked), for both trusted channels. It asserts422/VALIDATION_FAILED, the five-key problem/no-store/nosniff and all eight counters zero before the ordinary successful request. The fixture's existing string POST helper delegates to a BodyPublisher overload; no test method/class is added. The real-HTTP chunked cases are compiled but not executed locally because Docker is inaccessible.

## 3. Exact task delta and preserved design

Only these six repository paths differ from the outside-repository entry snapshot:

1. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerSubjectActivationsController.java` — private body limit and bounded read before JSON binding.
2. `backend/src/test/java/com/onecall/archtests/CustomerSubjectActivationsWebTests.java` — existing-method organization propagation, exact success contract and bounded-body assertions.
3. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationFixture.java` — second organization seeded by reset; HTTP publisher helper overload.
4. `backend/src/test/java/com/onecall/archtests/container/CustomerSubjectActivationsIT.java` — existing-method real organization HTTP, cross-organization404/zero writes, typed success and known/chunked overflow matrices.
5. `docs/gateflow/pa31-customer-subject-activation-foundation-s1-implementation.md` — current status and fix-4 addendum.
6. `docs/gateflow/pa31-customer-subject-activation-foundation-s1-fix-4.md` — this artifact.

The four code/test paths are already in accepted40; cumulative inventory remains15 existing updates +25 additions =40 implementation paths. Documentation is accounted separately. No accepted plan/fix/review/Control, OpenAPI/docs14/docs15, V001–V017, .codex, CI/POM/dependency or unrelated path is edited. No migration/V018/new grant is introduced.

One-slice design, lock order, same-connection transaction/owner-role handoff/restoration, conditional revisions, immutable descriptors, replay/DECLINE/SUSPENDED semantics and redaction remain unchanged. No sensitive logging or public owner/policy/fingerprint leakage is added. All existing test identities remain:86 new =52 unit +34 container;838 historical;59/520 unit +25/404 container =84/924; annotation925; POM25. No added/renamed/deleted/disabled/skipped test method/class or weakened assertion.

## 4. Executed validation

Evidence root: `/tmp/pa31-fix4/evidence/`. Maven environment: `JAVA_HOME=/opt/jdk21`, `PATH=/opt/jdk21/bin:/usr/local/bin:/usr/bin:/bin`.

```bash
# Repository root
/opt/maven-3.9.9/bin/mvn -B -ntp -f backend/pom.xml test-compile
# backend working directory
/opt/maven-3.9.9/bin/mvn -B -ntp \
  -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar \
  -Dtest=CustomerSubjectActivationsWebTests,CustomerSubjectActivationServiceTests,ActivationTrialServiceTests,CustomerSubjectActivationConfigurationTests,CustomerSubjectActivationBoundaryTests,ModuleBoundaryTests,CustomerBusinessNumberBoundaryTests,CustomerSubjectDetailBoundaryTests,SharingQueryBoundaryTests,CustomerEvidenceBoundaryTests,CustomerApiBoundaryTests test
python3 /tmp/pa31-retry1/ci-source-only.py
# Repository root
docker version
python3 /tmp/pa31-fix4/evidence/audit.py
GIT_OPTIONAL_LOCKS=0 git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call8 --work-tree="$PWD" diff --check
```

| Command / evidence | Exit | Exact result |
| --- | --- | --- |
| test-compile; `test-compile.log` | 0 | Production and all118 test/fixture sources compile; BUILD SUCCESS. Existing annotation-processing/JAXB/deprecation/unchecked warnings only. |
| Explicit-agent focused selection; `focused84.log`, `focused84-xml/`, `xml-summary.json` | 0 | Exactly11 selected classes/84 methods =52 new +32 existing; failures0/errors0/skips0. XML copied by exact selected class, not totals from stale reports. |
| Static source/CI script; `static-ci.log` | 0 |59 unit classes/520 methods +25 container classes/404 methods =84/924; annotation925; POM includes25; historical subtraction identities/arithmetic preserved. |
| Supplementary current-CI AST custody check; `ci-custody.log` | 0 | Executed static extraction matches current checked-in CI: all23 dictionary ASTs and13 addition-loop ASTs identical. |
| Entry/path/frozen/identity/Git audit; `audit.log`, `audit-result.json` | 0 | Exactly six task paths/four accepted implementation paths; cumulative40; unchanged test identities; new52/34, historical838; frozen20 raw hashes; exact V001–V017/noV018; protected inputs and branch/HEAD/index unchanged. |
| Mapped read-only Git diff --check; `diff-check.log` | 0 | No whitespace errors. Supplementary six-path whitespace check covers untracked additions too. |
| docker version; `docker-version.log` | 1 | CLI29.1.3 present; daemon access denied at unix:///var/run/docker.sock. |
| Focused Docker/PostgreSQL16 | NOT RUN | Daemon inaccessible; no Failsafe or34-method PG16 execution/pass claimed. |

The existing Windows gitfile is unchanged; read-only Git uses the explicit Linux git-dir mapping rather than repairing metadata. Non-generated entry snapshot covers1945 files; generated Maven target/build outputs are excluded from product-path custody. A grouped read initially used root-relative documentation paths from backend and returned file-not-found; the reads were corrected to root. This was a read-only setup issue, not a test/product failure. No network/escalation/remote operation was attempted.

## 5. Residual risk and Controller handoff

All three accepted A findings are **已修复** in code/assertions. Local evidence is compile +84 focused unit/architecture passes +static custody, not post-fix database proof. The newly expanded real HTTP organization/isolation/typed-response/chunked-overflow matrices still require Controller's focused34 PostgreSQL16 rerun, zero failures/errors/skips, and full59/520 +25/404 validation/inventory on this exact uncommitted candidate. Prior Controller clean results cannot be relabeled as this candidate's rerun. No full suite, JAR/web smoke, 116 probes or OpenAPI check was rerun by this child.

The8192-byte cap applies only to this activation route; other controllers and deployment-wide ingress protection are unchanged/out of scope. No product-blocking contradiction was found; the remaining risk is unexecuted real-database/server evidence in this sandbox. Return the candidate and stop: no commit/stage/push/branch change/PR/merge/approval/mark-ready/reviewer request/external comment/issue mutation/re-review/another gate occurred.
