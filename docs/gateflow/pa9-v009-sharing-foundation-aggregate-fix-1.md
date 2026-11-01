# PA9 V009 Sharing Foundation — Aggregate Fix 1

## Identity, gate and accepted inputs

- Task: `pa9-v009-aggregate-codefix-gpt-20261031-01`; work unit `PA9-v009-sharing-persistence-foundation`; aggregate-review **fix only**. Author evidence, not independent re-review or acceptance.
- Branch: `codex/pa9-v009-sharing-foundation`; intake and unchanged final HEAD `96db75a17f6ea2b657cfb06d30dfcdf8d88f4dfc`.
- Base `835f2ecece1ac8b5c79ab794427424ea380ac052`, plan `ee4db90d6d7dd52384e75ccb8e7148cdf1ea08b9`, S1 `9b5edd68769745de12efda178bbca106f9719328`, S2 `6111a535918ca0d53be893351d736558fa5aaa3a`, S3 `3815ed2b50e39968f731799c2a07fcc3cabbca2e`: each passed `git merge-base --is-ancestor <commit> HEAD`.
- Accepted plan `docs/gateflow/pa9-v009-sharing-foundation-plan.md`: canonical SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`.
- Aggregate A `docs/reviews/code-review-20261031-194433.md`: canonical SHA256 `fd22fd053cd0053a46cd88a819487236cc1e1b0eddd64b38b24dbb94508df7c5`; B `docs/reviews/code-review-20261031-194648.md`: canonical SHA256 `661e865a2ec03cfa8b405fa36dfb3cda15bc112f456cf2845ba956069c426ada`. Both read in full; both remain raw-byte unchanged. Their historical review HEAD is not this task's intake HEAD.
- Runtime/provider: OpenAI assistant through Codex CLI; Linux/WSL2 amd64, Temurin JDK 21.0.13 at `/opt/jdk21`, Maven 3.9.9 at `/opt/maven-3.9.9/bin/mvn`; Docker client 29.1.3, Docker Desktop engine 29.9.2; real Testcontainers `postgres:16`, server PostgreSQL 16.15 (Flyway JDBC log). No subagent or alternate provider used. Actual backing model/provider routing is not independently attested by terminal/database output.

Only four repository paths are written: the container IT, current I3/E3 records, and this new artifact. All repository edits use `apply_patch`. Generated Maven output remains confined to ignored `backend/target`; scratch logs/scripts/report snapshots are external under `/tmp/pa9-aggregate-fix-01`, never `build/`.

## Accepted finding dispositions

| Item | Final author disposition and bounded change |
| --- | --- |
| AF-1 | Fixed: class-level javadoc now describes PA0–PA9, V001–V009, V008 evidence and V009 sharing catalog/ACL/fences/lifecycle/atomic admission/rollback/concurrency, exactly 64 container methods. |
| AF-2 | Fixed: V006 method javadoc retains inventory/guard/Outbox constants and describes final V009 live fence, no V010/extra object, and staged V008 prefix booleans. Its body is unchanged. |
| AF-3 | Fixed: after row lock/current count/revision, `v009S3Admit` checks current status in ACTIVE/LOCKED and `clock_timestamp()` within `[valid_from, expires_at)` before same-key fact lookup. Replay remains before the unchanged new-admission ACTIVE/generation/limit check. REVOKED/EXPIRED cannot return replay. No service, ticket/session implementation or policy is introduced. |
| B admission-key 129 | Rejected non-finding; no change to admission-key boundary logic or its probes. |
| Evidence provenance | Added canonical Git-content identities and explicit environment-specific raw identity explanation; no historical review/fix artifact rewritten. |

The existing sequential S3 method retains ACTIVE/LOCKED replay with original admission/event/count, max=1 and new-key denial. The existing concurrency method still runs all 18 Lock races. Its lifecycle helper now additionally probes the already-admitted key after committed legal REVOKE and EXPIRY transitions (admission-first order). The probes supply the **current** generation 1, assert `DENIED`, null returned IDs, count 1/revision 3, exact unchanged grant/admission/event JSON before/after, commit, then compare the same snapshot through a fresh connection. Thus denial is not merely the old new-key generation mismatch; it is a current-state same-key check. LOCK replay still returns original IDs and count/revision without mutation. No new `@Test` method is added.

The frozen sequence reference is `docs/14-production-data-model-and-flyway-schema-spec.md` (append-only control/admission-count paragraph): current checks precede replay; LOCKED permits existing-admission replay but not new admission. This harness change models only the accepted SQL status/time ordering and immutable fact lookup, not excluded authorization, recipient checks, session reconstruction or ticket issuance.

## Canonical Git-content versus raw-checkout provenance

Historical S3 Java/CI/README “raw checkout” hashes came from the controller's **mixed LF/CRLF working tree**. The preflight in this supplied controller worktree reproduces all three historical raw values exactly; CRLF-to-LF normalization equals the committed LF blob for each. Those raw values are **not reproducible from canonical committed LF blobs alone**, nor from a uniform CRLF rendering: the per-line working-tree newline layout is additional environment-specific information. They are not canonical repository identities or a content discrepancy. Historical review artifacts are preserved rather than repaired.

Canonical committed identities are reproducible without checkout filters using `git show HEAD:<path> | sha256sum` (or `git cat-file -p HEAD:<path>`). For these three files HEAD content has no CRLF. Uniform CRLF comparisons below are computed by replacing every canonical LF with CRLF, not by changing any repository file.

| Intake item | Canonical committed LF SHA256 | Historical/controller mixed-newline raw SHA256 |
| --- | --- | --- |
| Java `PostgresContainerIT.java` | `5e6a4fd04d06c94dddd443551fbdcc25839cccfeb1cb489a434fed27eed45d0d` | `9f089e03efd3bafce68e949dd590d8aaf8f29a943c90a9da43829a33f8e9dcb1` |
| CI `.github/workflows/ci.yml` | `7bffd120aa3da912977aca923a375cc814dc67803614a55fdfd0204df6b1d55f` | `9f04d0b0a710be0143b7977aa20a28f3492f0d281ac59f7bbb456da862f09238` |
| `backend/README.md` | `93cdb627cdd481e2873ac98078ccc6e0485555c65ce56ef37676cbce6f074b41` | `c9395e749738e7c1435840bf88d8026ff8a4b32879efb9817a1b8356db73132f` |

Uniform CRLF SHA256 for the same intake canonical blobs: Java `406ebe92fd8fc8b14476f8a1f3b4b8eb39dbe1dd0ba9d6f04579d8aef3e56a6a`; CI `af0029c369245b9b76e15c7016e96204e697234809f23ec3b05dd1d3028aa688`; README `d5a00c9d63910bc1e65cccc2b952ef820195e3cb3c6eb62ef915349220c4063e`. These are distinct from both canonical and controller raw identities, explaining aggregate A's clean-checkout observation.

Final **uncommitted** Java LF-normalized content SHA256: `336f2b0669da4fb4d981affc46d4ca1a5eea334509602428c77327bb8edc890f`. Final environment-specific raw SHA256: `2403afc612a1b7c9f460272387fff0d7b44b769f73fff34a16e16417b91ba8f6`. This final normalized identity is not claimed as an existing committed blob. CI and README retain both intake canonical and raw identities unchanged. Final raw and normalized SHA256 for all four changed files, including I3/E3 and this artifact, are recorded externally in `/tmp/pa9-aggregate-fix-01/final-manifest.json` after the records are complete, avoiding a circular self-hash.

V009 canonical SHA256 remains `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`. Each V001–V008 canonical blob equals base and its accepted plan §7.2 pin; all nine raw migration files also remain unchanged from preflight. Exactly V001–V009 exist, no V010. Plan, design, controller, CI, README, POM, application/test resources, historical reviews/fix records and all other tracked bytes are unchanged.

## Actual commands and validation results

Git's supplied `.git` pointer uses a Windows `D:/...` path; initial ordinary `git status/branch/rev-parse` failed under Linux. Read-only Git commands thereafter use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7` and `GIT_WORK_TREE=<this checkout>`. No metadata/config repair is performed. Pre-existing untracked `.codex` and `build/` are inventoried and left unchanged. A supplemental unconfigured `mvn -version` invocation selected inherited Windows Java and failed launcher lookup; the explicit required JDK invocation succeeds. All actual compile/test runs use the required Linux JDK/Maven and succeed on their first attempt; no test failure, retry, skip flag or weakened assertion is hidden.

Maven commands run from `backend` with `JAVA_HOME=/opt/jdk21` and `/opt/jdk21/bin` first on PATH. `timeout 240s` is an external bound, not a widening of test timeouts. All commands below exit 0.

| Command | Actual result |
| --- | --- |
| `python3 /tmp/pa9-aggregate-fix-01/audit.py preflight /tmp/pa9-aggregate-fix-01/preflight-xml` | Branch/intake/base/slice ancestry and plan/A/B/V009 SHA256 pins pass; V001–V008 base/pin equality; source=CI=retained preflight XML exact 64; baseline frozen raw-byte inventory captured. Preflight XML is prior evidence, not a new test run. |
| `/opt/maven-3.9.9/bin/mvn -B -ntp test-compile` | BUILD SUCCESS; 7.129 s, finished 2026-10-31 19:54:31 +08:00. |
| `timeout 240s /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009AdmissionCountOnceLimitAndAtomicRollbackBackstops+concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner' verify` | BUILD SUCCESS; 40.179 s, finished 19:55:50 +08:00; Failsafe exactly 2, Surefire exactly 10, zero failures/errors/skips. |
| **Exactly one** `timeout 240s /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify` | BUILD SUCCESS; 59.458 s, finished 19:57:18 +08:00; Failsafe exactly 64 (46.64 s suite), Surefire exactly 10 (1+8+1); zero failures/errors/skips. No second full run. |
| `python3 /tmp/pa9-aggregate-fix-01/audit.py final` | Independent source regex, CI literal set and final Failsafe XML names have exactly 64 distinct entries and identical sets; XML header 64/0/0/0; Surefire totals 10/0/0/0; no Disabled/Assumptions; frozen raw bytes, allowed changed paths, untouched `.codex`/`build/`, empty staged diff, unchanged HEAD and `git diff --check` pass. Repeated after final record writes. |
| `cd backend && python3 /tmp/pa9-aggregate-fix-01/ci-validator.py` | Actual CI heredoc extracted unchanged/dedented; `container-smoke report assertions passed` against final reports. |
| Independent marker/XML parse of focused and full snapshots | Each has exactly 18 `PA9 S3 observed Lock:`, seven rollback/no-residue and two cleanup markers. Each contains ACTIVE REPLAY, LOCKED REPLAY, REVOKE DENIED, EXPIRY DENIED, LOCK REPLAY and zero failure/error/skip XML counters. |
| `docker ps -a --format '{{.ID}} {{.Image}} {{.Names}}'` | Empty after cleanup; no remaining PostgreSQL/Ryuk container. |

The final scope/whitespace audit also checks the new artifact's whitespace without staging it (`git diff --no-index --check /dev/null <artifact>`); existing tracked diff whitespace is clean. Git's LF-to-CRLF advisory warnings do not indicate a changed frozen file. Historical base-to-HEAD whitespace observations are not repaired.

### Evidence identities (SHA256)

All paths in this table are under `/tmp/pa9-aggregate-fix-01`; logs and report snapshots are external author evidence, not remote CI.

| Evidence | SHA256 |
| --- | --- |
| `focused.log` | `1bcd9735aab97789eeb3474f7dafff57850bfc7fe9a58aa31091ba379d6a9ae4` |
| `focused-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `2af450c4809c7ef7d8994669e7bea0fbab8c11ec95414455928c0c25f982a7fa` |
| `full.log` | `d9002f902896c38c49349b932fc7d5f9f7749e3d624802029e63029ceda9209c` |
| `full-xml/TEST-com.onecall.archtests.container.PostgresContainerIT.xml` | `e9e98b4d8a6292ce38f568b4142c72f88fc62a435df9ef00812248e92ca968a7` |
| `full-xml/TEST-com.onecall.archtests.OneCallApplicationTests.xml` | `73288430fedfce1961761a13b4a940d19acad863b074235fded50ff489340197` |
| `full-xml/TEST-com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests.xml` | `8fba0f0753a3d5b12169c805ff36013efb8a690f48d917ca9dd582cde795b202` |
| `full-xml/TEST-com.onecall.archtests.architecture.ModuleBoundaryTests.xml` | `9a30f66785dd3981df4b0d65af65ab282c71d627077814c46babb85f5adc5029` |

`preflight.json`, `baseline.json`, `post-test-audit.json`, `final-audit.json`, `runtime.txt`, `test-compile.log`, `ci-validator.log`, report snapshots, the audit/validator scripts and the non-circular final manifest preserve command outputs/identities outside the repository. No extra Maven execution is needed for evidence-only documentation writes.

## Residuals and stop

Unchanged and not addressed: synthetic timing/5-second expiry and bounded two-connection races (Backend QA), CHECK UNKNOWN/presence-only JSON and excluded service validation (Sharing/Backend/Security), production policy/provisioning/storage/legal/KMS (Security/Product/Platform/Database), and remote CI/provider attestation/evidence archival (Controller/QA). These local database results are not throughput/deadlock/production authorization, remote CI success or production readiness. No accepted plan/design, migration, admission-key 129, race timeout, CHECK, provisioning or policy change is made.

AF-1/AF-2/AF-3 are fixed by author evidence; provenance is recorded additively. No stage, commit, push, branch creation, PR, issue, review request, review, approval, external comment or other external workflow action occurs. Stop after aggregate-fix records and checks. Fresh independent aggregate A/B re-review and any acceptance remain separate controller actions.
