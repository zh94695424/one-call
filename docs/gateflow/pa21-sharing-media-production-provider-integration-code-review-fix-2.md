# PA21 code-review fix-2

- Task: `pa21-code-review-fix2-gpt-20261109`.
- Work unit: `PA21-sharing-media-production-provider-integration`; gate: code-review fix-2.
- Date: 2026-11-09 (Asia/Shanghai).
- Inputs read: binding plan §10.2, `docs/reviews/code-review-20261109-221643.md`, `docs/reviews/code-review-20261109-230813.md`, current configuration tests, and directly referenced authority/configuration/claim/request/key fixtures and CI inventory.
- This is a fix and validation report, not review acceptance or readiness. Fresh independent A/B re-review remains required and has not been performed.

## Exact changed files

1. `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java`
2. `docs/gateflow/pa21-sharing-media-production-provider-integration-code-review-fix-2.md` (new; this report)

No production source, other test, CI, POM, plan, Control, previous artifact, API/OpenAPI, migration, Git metadata, or `.codex` file is edited. Maven and annotation-processor output is isolated under `/tmp/pa21-fix2/workspace`, not the checkout. No external system is changed.

## Finding disposition

**F-PA21-CR-B-04: addressed in code; independent re-review pending.**

- The existing `singleAuthorityEvidenceStateAndReaderAreUsedWithoutReplacement` method now captures the actual `VerifiedMediaClaim` supplied to `PlaybackStateReader.check` by the real configured `MediaPlaybackService` after real authority verification. No verifier replacement, fake verifier counter, or second authority is introduced.
- The token is produced by the real configured authority's `issueOrReplay` using independently supplied request facts and a known `K1` replay anchor. Supplying the anchor makes the exact issuance/not-before instants test inputs rather than values read back from the claim, verifier, or issuance result. The existing fresh-issuance `token(...)` helper remains unchanged and still supplies the service-level MAC-tamper case and other callers.
- Record equality pins every field: audience `onecall:media-playback:v1`; grant/admission/evidence/object IDs `new UUID(0, 1/2/3/4)` respectively; revocation generation **7** and code generation **11**; purpose `PLAYBACK`; issuedAt = locally chosen current instant truncated to microseconds minus one second; notBefore = issuedAt (required by the replay-anchor contract); expiresAt = issuedAt plus 60 seconds; duration **10** seconds; full-object window **[Duration.ZERO, Duration.ofSeconds(10))**. The expected record is built independently before playback; no expected value is taken from the observed claim.
- Distinct nonzero generation values also pin the request-to-claim generation ordering. Exact instant equality includes the microsecond component; no tolerance/range assertion substitutes for equality.
- `providers(...)` now has a claim-capture overload; the original signature delegates to it for existing callers. The unused randomized `claim` local and its unused `now` are removed.
- Both registration orders, single authority/verifier/issuer identities, `Opened`, downstream `calls == 3`, duplicate-provider startup failures, the persistence resolver matrix, and service-level MAC-tampered `Denied(INVALID_CREDENTIAL)` with the counter still **3** remain asserted. No test method is added, removed, renamed, disabled, or skipped.

## Validation environment and commands

Evidence/log root: `/tmp/pa21-fix2`. Logs are local scratch evidence, not new repository artifacts.

Preparation from the checkout:

```sh
mkdir -p /tmp/pa21-fix2/workspace/backend
cp backend/pom.xml /tmp/pa21-fix2/workspace/backend/
cp -a backend/src /tmp/pa21-fix2/workspace/backend/
```

After the initial correction below, the changed Java file was copied again to the same temporary source path. Every Maven command ran with working directory `/tmp/pa21-fix2/workspace` and this environment:

```sh
export JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:/opt/maven-3.9.9/bin:/usr/bin:/bin
```

Maven `-version` confirmed **Apache Maven 3.9.9 / Eclipse Adoptium Java 21.0.13**. The explicit environment avoids the shell's default Windows-mounted Java executable, which cannot launch in this sandbox. Offline dependencies were already available; none were downloaded.

| Actual command/check | Result |
| --- | --- |
| `mvn -o -B -ntp -f backend/pom.xml -DskipTests test-compile` | Final **exit 0, BUILD SUCCESS**; `test-compile-final.log`. All production/test sources compile. Annotation processor reports `/tmp/pa21-fix2/workspace/./build/generated-spring-modulith/javadoc.json`, outside the checkout. |
| `mvn -o -B -ntp -f backend/pom.xml -DargLine=-javaagent:/root/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar -Dtest=MediaPlaybackConfigurationTests,MediaPlaybackCredentialAuthorityTests,MediaPlaybackServiceTests,ShareAdmissionServiceTests test` | Final **exit 0, BUILD SUCCESS; 80 tests, 0 failures/errors/skips**; `focused-final.log`. Configuration 9, authority 24, playback service 21, admission service 26. Expected fail-closed Spring startup warnings are negative-test evidence. |
| `python3 - <<'PY'` extracting the existing CI inventory block with `ast`/`textwrap`, executing the static section plus its `expected`/`expected_query` prerequisites, then comparing every source method set and rejecting conditional test annotations | **Exit 0**; `static-inventory-final.log`. All existing static assertions and exact method dictionaries pass: **30/231 + 13/214 = 43 classes / 445 methods**, line scan **446**, POM Failsafe includes **13**, configuration methods **9**. CI is read, not edited; XML execution checks are not represented as full-suite evidence. |
| `python3 - <<'PY'` comparing the four fresh temporary Surefire XML method sets against their source files and checking failures/errors/skips | **Exit 0**; `fresh-focused-identities.log`: **4 classes / 80 exact method identities**, all green. No imported full-suite XML is used as fresh evidence. |
| `cmp backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java /tmp/pa21-fix2/workspace/backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` | **Exit 0**: final checkout source is byte-identical to the tested temporary copy. |
| `docker info --format '{{.ServerVersion}}'` | **Exit 1**; `docker.log`: permission denied connecting to `unix:///var/run/docker.sock`. Docker is inaccessible here. Full verify/container execution is not attempted or claimed; Controller will run full verify. |
| `git diff --check` | **Exit 128**; `git-diff-check.log`: the worktree gitfile resolves to an unusable Windows `D:/codex-call/.git/worktrees/codex-call23` location under Linux. Git metadata is not repaired or modified. |
| `git -C /tmp diff --no-index --check /tmp/pa21-fix2/MediaPlaybackConfigurationTests.before.java <absolute-checkout-test-path>` | **No whitespace diagnostics**; `no-index-final.log`. Exit **1** means compared contents differ; no-index fallback does not repair or replace the unavailable native worktree check. |
| `git -C /tmp diff --no-index --check /dev/null <absolute-checkout-fix-2-report-path>` | **No whitespace diagnostics**; `report-whitespace.log`; exit **1** for the new report. |
| Python SHA-256 comparison against `/tmp/pa21-fix2/baseline.json`, plus source-copy comparison and generated-file timestamp scan | `final-scope.log`: exactly the two allowed paths differ among inventoried inputs; all other inventoried bytes, including frozen V001–V008/public OpenAPI/POM/CI and previous artifacts, remain unchanged. The 1,641-file baseline excludes Git metadata, dependency trees and generated/build directories; no claim of baseline hashing those exclusions is made. The checkout `backend/target`/`build` timestamp scan reports no newer files. All validation output paths are temporary. |

### Initial validation corrections (not final-source passes)

- Initial compile passed, but the first focused run returned **exit 1: 80 tests, 0 failures, 1 error, 0 skips** (`focused-initial.log`): the proposed distinct not-before instant violated `ReplayAnchor`'s required `issuedAtMicros == notBeforeMicros`. The fixture now supplies equal input instants. Request generation argument order was also corrected to code **11**, revocation **7**, matching the independently expected claim. Both final compile and focused run above use the corrected bytes.
- The first static-inventory extraction failed with `NameError: expected` because the extracted block depends on two earlier constant dictionaries. The final extraction includes those original prerequisites and passes without changing CI or weakening any static assertion.

## Residual risks and stop state

- Fresh independent **A/B re-review remains required and has not been performed**. This report does not close the review gate, approve the candidate, declare readiness, or request reviewers.
- Controller retains the fresh socket/Docker-enabled **full verify** and all-suite source/CI/XML gate. This session proves compilation and the focused 80-test set, not a new all-green 445-test run.
- Native `git diff --check` remains blocked by the existing host/worktree path mapping. The no-index checks have no whitespace diagnostics, but do not substitute for Controller's native worktree check.
- The positive configuration fixture uses a supplied replay anchor solely to make exact times independent inputs; it does not test persistence provenance of that anchor. Fresh issuance, anchor policy and service/persistence trust remain covered by their existing tests and unchanged implementation. The existing approximately one-minute validity window remains a runtime-clock sensitivity if the process is paused for that duration.
- Production key custody/rotation, clock skew, evidence-drift availability, future sub-window policy, authority caller trust and cross-purpose request-key concurrency remain existing owner-routed risks; no production policy or trust boundary changes here.
- **Stop after fix and validation.** No stage, commit, branch, push, PR, merge, approval, ready transition, reviewer request, external comment, other gate action, or external-system mutation is performed.
