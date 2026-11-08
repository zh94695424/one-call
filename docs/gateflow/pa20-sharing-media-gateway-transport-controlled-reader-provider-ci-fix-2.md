# PA20 bounded PR-CI fix 2

## Gate and evidence

- Task: `pa20-pr-ci-fix2-gpt-20261109`; date: 2026-11-09; Gateflow gate: `fix`.
- Work unit: `PA20-sharing-media-gateway-transport-controlled-reader-provider`.
- Controller-supplied hosted evidence: PR #31 final controller head `fba18840fcd58e8290c724b42b0c813f83214f70`, containing only Control bookkeeping after code head `432ed60c660b4b5c88eec0ea70be444ba45165e0`; run `42510900019`: `contract-regressions` PASS, `java-runtime` failed only at `MediaGatewayTransportIT.realHttpFullAndRangeUseQualifiedAuthorizationThenExactBytes:60`, in `transport.counts(0)`. Read-only local Git HEAD equals the final controller head. Hosted evidence was not independently fetched or rerun.
- Root cause: the client can receive all exact `Content-Length` bytes before the server filter finishes its final `session.close()` bookkeeping. Immediate terminal-counter assertions race with that cleanup under Linux scheduling. The two passing Controller Docker runs do not disprove the race or establish a pass for this correction.
- Production lifecycle remains unchanged: writes finish, close is attempted once, then flush; failure aborts once and closes once. No production fix is warranted by this test-observation race.

## Exact change and boundaries

1. `backend/src/test/java/com/onecall/archtests/container/MediaGatewayTransportIT.java`: add the static `org.awaitility.Awaitility.await` import; wrap only the three existing counter assertions in `await().atMost(Duration.ofSeconds(5)).untilAsserted(...)`. Every poll retains exact equality: `source.opens == 1`, `source.abortAttempts == aborts`, `source.closeAttempts == 1`. Awaitility is already available; no dependency changes. After successful observation, the existing `source.requested` bounds (`count > 0 && count <= 8192`) and sealed checkout-count assertions still run unchanged.
2. `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-implementation-report.md`: append this hosted finding, root cause, bounded correction and measured validation; update the final disposition to distinguish historical passes from pending fix-2 Controller checks.
3. Add this single new artifact: `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-ci-fix-2.md`.

No tests are added, removed, renamed, weakened, disabled or skipped. All six transport test declarations and all other assertions remain byte-identical to the pre-edit snapshot. Control, production sources, OpenAPI, migrations, CI/POM, other tests, plans and prior artifacts are untouched. Pre-existing untracked `.codex` is not edited. No staging, commit, push, PR mutation, review request, merge, approval, ready action, external comment or issue mutation is performed.

## Commands and results

Toolchain: Apache Maven 3.9.9, Eclipse Adoptium Temurin JDK 21.0.13, UTF-8, Linux/WSL. Build evidence is isolated under `/tmp/pa20-pr-ci-fix2-gpt-20261109` so repository build outputs and Controller reports are not written. The backend copy excludes `backend/target`; no repository POM, source or dependency is adjusted for validation.

From the repository root:

```sh
mkdir -p /tmp/pa20-pr-ci-fix2-gpt-20261109/bounded
tar --exclude=backend/target -cf - backend | tar -xf - -C /tmp/pa20-pr-ci-fix2-gpt-20261109/bounded
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
set -o pipefail
cd /tmp/pa20-pr-ci-fix2-gpt-20261109/bounded/backend
mvn -o -B -ntp -DskipTests test-compile 2>&1 | tee /tmp/pa20-pr-ci-fix2-gpt-20261109/test-compile.log
mvn -o -B -ntp -Dtest=MediaGatewayWebTests -Dsurefire.failIfNoSpecifiedTests=true surefire:test 2>&1 | tee /tmp/pa20-pr-ci-fix2-gpt-20261109/focused-unit.log
```

- `test-compile`: exit 0, **BUILD SUCCESS**, 140 production and 70 test source files compiled with release 21; completed `2026-11-09T02:44:37+08:00`, 11.373 seconds. Existing JAXB annotation enum, annotation-processing, deprecated API and unchecked-operation notices are non-fatal; no unrelated correction attempted.
- Focused non-Docker unit check: exit 0, **BUILD SUCCESS**, `MediaGatewayWebTests`: **18 tests, 0 failures, 0 errors, 0 skips**; completed `2026-11-09T02:44:51+08:00`, 4.370 seconds. Covers existing exact full/range bytes, close/abort/flush lifecycle and failure behavior. This does not run `MediaGatewayTransportIT` or reproduce the server/client scheduling race.
- Exact-byte Python comparison of the pre-edit snapshot against the current IT passed after applying only the intended import and counter wrapper; original/current `@Test` count is 6/6. No other assertion or test declaration changed.
- Read-only Git commands use `git --git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD" ...` because the existing `.git` points to a Windows path. Initial ordinary `git status --short` could not resolve that pointer; no Git metadata was repaired. Explicit-path `rev-parse HEAD` confirmed the controller head; `rev-parse 432ed60c` resolved the full code head above. `diff --check` passed (exit 0); Git emitted existing checkout LF/CRLF conversion warnings.
- Baseline SHA-256 manifests are `/tmp/pa20-pr-ci-fix2-gpt-20261109/baseline/tracked.sha256` and `baseline/untracked.sha256`. Final comparison checks all tracked checkout bytes, allowing only the IT and report to differ, verifies `.codex` unchanged, and checks that exactly the one authorized artifact is newly untracked. No full source=CI=XML inventory validation is claimed.

### SHA-256 evidence

- Pre-edit IT snapshot `/tmp/pa20-pr-ci-fix2-gpt-20261109/baseline/MediaGatewayTransportIT.java`: `7426e1978118b019d00342d769cb1484205d8787957b9ff316ee38af189220e2`.
- Corrected repository IT: `0994520cf3f8ac654b3813a59eaced74726e378fac7d0211fb2de4bedc93719e`.

- `/tmp/pa20-pr-ci-fix2-gpt-20261109/test-compile.log`: `66a81b8615d1d902e47282d8f4cb30c916a51c77aecfdfde8c77ea33ae0f8a3b`.
- `/tmp/pa20-pr-ci-fix2-gpt-20261109/focused-unit.log`: `a6137dfe203260a575870a73464311461e3e7a5d20c9c38d4652f0a77c3cad33`.
- `/tmp/pa20-pr-ci-fix2-gpt-20261109/bounded/backend/target/surefire-reports/TEST-com.onecall.archtests.MediaGatewayWebTests.xml`: `49f67c59b039b47b1fe19c5869da1378f3b2ab02849b9cfba5c7bc4c6e49f9a1`.

Final authorized-path hashes are supplied in `/tmp/pa20-pr-ci-fix2-gpt-20261109/final-paths.sha256` and the handoff evidence. The artifact's own hash is external to avoid a self-referential digest.

## Residual owners and completion

- **Completed locally:** bounded helper-only correction, required report and single artifact, offline test compilation and 18 focused non-Docker unit checks. Local completion is not gate acceptance.
- **Controller full validation passed:** exact `container-smoke clean verify` completed `BUILD SUCCESS` at `2026-11-08T18:58:27Z` in 10m39s. Fresh Surefire `29/196`, Failsafe `13/204`, `MediaGatewayTransportIT` 6/6, all with zero failures/errors/skips; embedded inventory passed exactly `42/400`, POM 13, line scan 401. Log `controller-runtime/pa20-pr-ci-fix2-full-verify.log`, 820,302 bytes / SHA256 `55415453144FA6A9548469F88E41A59BFEF6C5554A780164B92631DF18E0C467`.
- **Controller/CI owns:** commit/push this correction and verify a new exact-head hosted CI run. Historical Controller passes are not reused as hosted proof.
- **Gate remains `fix`:** Controller owns subsequent Git/PR actions and gate disposition; none are performed here.
- **Production residuals unchanged:** Security/Sharing/Platform retain verifier provenance, wire format, key rotation and Q22 timing/revocation ownership; Evidence/Platform/Security retain private COS/KMS byte authority and timeouts; existing deployment, redaction, buffering and resource-limit owners remain as recorded in the implementation report. No production-readiness claim or expanded scope.
