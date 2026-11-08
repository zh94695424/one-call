# PA20 bounded PR-CI fix 1 — canonical public OpenAPI digest

## Identity and gate

- Task: `pa20-pr-ci-fix1-gpt-20261109`; date: 2026-11-09 (Asia/Shanghai).
- Work unit: `PA20-sharing-media-gateway-transport-controlled-reader-provider`.
- Gate: fix after exact-head hosted CI failure, not a new implementation or acceptance gate.
- Controller-supplied evidence: PR #31 head `d2178a84e6b8958699639c2123251ae212d429d3`, CI run `42508244765`; `contract-regressions` PASS, `java-runtime` FAIL in `SharingQueryBoundaryTests.gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges:384`. These hosted results were supplied, not independently queried or rerun by this task. Local HEAD inspection confirms that head.

## Root cause and frozen contract

The test hashed raw public OpenAPI checkout bytes and pinned the CRLF representation. GitHub/Linux uses LF, so identical contract content failed the byte digest assertion. This is a test-portability defect, not a contract change. Controller reports base and head Git blob `327f1f25e7cb6e6a4d461a79eb3f258818bc2fbd` for `api/openapi/onecall-v1.openapi.yaml`; read-only local HEAD inspection confirms that blob. Both SHA-256 values were independently reproduced locally:

| Representation | UTF-8 bytes | Raw SHA-256 |
| --- | ---: | --- |
| LF (canonical) | 259656 | `ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30` |
| CRLF (original local checkout) | 266916 | `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68` |

## Exact bounded change

- `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`: replace `Files.readAllBytes(...)` with explicit UTF-8 `Files.readString(...)`, `.replace("\r\n", "\n")`, and UTF-8 `.getBytes(...)`; retain SHA-256 and hexadecimal encoding, and pin the canonical LF digest above.
- `docs/gateflow/pa20-sharing-media-gateway-transport-controlled-reader-provider-implementation-report.md`: record the later hosted failure, checkout-versus-canonical distinction, bounded correction and measured local validation without replacing historical Controller evidence with a new full-suite claim.
- Add this single CI-fix artifact. No other repository path is written by this task. All other assertions and exact test identities/inventory remain unchanged; no test is added, disabled, renamed or skipped. Public OpenAPI, production sources, Control, migrations, plan, other tests, CI, POM and prior artifacts are not edited.

## Validation commands and results

Validation uses an isolated copy at `/tmp/pa20-pr-ci-fix1-gpt-20261109/bounded`, containing the backend sources/POM without existing `target` outputs and copies of the public and gateway OpenAPI documents. Only the isolated public document is rewritten between LF and CRLF. Repository build outputs and Controller reports are not regenerated. A preliminary broader copy included unnecessary OpenAPI tooling dependencies; it is not the evidence root below.

Toolchain: `JAVA_HOME=/opt/jdk21`, `PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"`; `mvn -version` reports Apache Maven **3.9.9**, Eclipse Adoptium Java **21.0.13**, UTF-8. All Maven commands use offline cached dependencies, without POM or toolchain changes.

From `/tmp/pa20-pr-ci-fix1-gpt-20261109/bounded/backend`:

```sh
export JAVA_HOME=/opt/jdk21
export PATH="$JAVA_HOME/bin:/opt/maven-3.9.9/bin:$PATH"
mvn -o -B -ntp -DskipTests test-compile
# Isolated public OpenAPI initially canonicalized to LF.
mvn -o -B -ntp '-Dtest=SharingQueryBoundaryTests#gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges' -Dsurefire.failIfNoSpecifiedTests=true surefire:test
# Rewrite only the isolated public OpenAPI from LF to CRLF; repeat.
mvn -o -B -ntp '-Dtest=SharingQueryBoundaryTests#gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges' -Dsurefire.failIfNoSpecifiedTests=true surefire:test
```

- `test-compile`: **BUILD SUCCESS**, compiling 140 production and 70 test source files with release 21; completed `2026-11-09T02:15:06+08:00`. Existing compiler notices include JAXB annotation enum availability, annotation processing, deprecated APIs and unchecked operations; none failed compilation and no unrelated correction was attempted.
- Focused LF run: **BUILD SUCCESS**, **1 test, 0 failures, 0 errors, 0 skips**; completed `2026-11-09T02:15:08+08:00`.
- Focused CRLF run: **BUILD SUCCESS**, **1 test, 0 failures, 0 errors, 0 skips**; completed `2026-11-09T02:15:10+08:00`.
- LF preparation decodes UTF-8, replaces CRLF with LF and encodes UTF-8; CRLF preparation replaces LF bytes with CRLF in that canonical isolated file. Python `hashlib.sha256` assertions verified the respective raw hashes before testing. Saved Surefire XML records the exact method identity for each representation.
- Logs under `/tmp/pa20-pr-ci-fix1-gpt-20261109/`: `bounded-test-compile.log`, `bounded-focused-lf.log`, `bounded-focused-crlf.log`; preserved XML: `bounded-focused-lf.xml`, `bounded-focused-crlf.xml`.
- A complete-source comparison against the pre-edit test snapshot passed after applying only the intended document-read and digest substitutions (text comparison normalizes source line endings). Thus every other assertion and test declaration is preserved; this is not a claim of rerunning the full source=CI=XML inventory gate.
- `sha256sum -c /tmp/pa20-pr-ci-fix1-gpt-20261109/baseline/untouched.sha256` verifies unchanged repository public OpenAPI, pre-existing `docs/gateflow/project-execution-control.md` changes and `.codex` bytes. Read-only Git inspection uses `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"` because the existing `.git` file points to a Windows path; no Git metadata is repaired.

## Residuals, owners and completion

- **Completed locally:** the bounded portability correction, both line-ending focused runs, JDK 21 / Maven 3.9.9 test compilation and the two required documentation updates. Exactly the three authorized task paths are changed; the pre-existing modified Control file and untracked `.codex` are left untouched.
- **Controller post-fix validation passed:** exact `container-smoke clean verify` completed `BUILD SUCCESS` at `2026-11-08T18:28:19Z` in 10m51s; fresh Surefire `29/196` and Failsafe `13/204` had zero failures/errors/skips, `MediaGatewayTransportIT` passed 6/6, and the unchanged embedded inventory passed exactly `42/400`, POM 13, line scan 401. Log: `controller-runtime/pa20-pr-ci-fix1-full-verify.log`, 820,318 bytes / SHA256 `CF1780C0A3D3DAB984EAED4940A939EB4EB3A69FE9CBC8D75D4038DF26B93277`.
- **Controller/CI owns:** commit/push the bounded correction and verify a new exact-head hosted CI run. Historical hosted success does not exist and is not claimed.
- **No expanded product scope:** existing production verifier/private-byte authority and deployment residuals retain their owners in the implementation report. This test-only fix grants no production-readiness or acceptance claim.
- No staging, commit, push, PR action, review request, approval, ready action, merge, external comment or issue mutation is performed. Stop at this bounded correction and return evidence to Controller.
