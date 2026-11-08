# PA19 sharing media gateway playback foundation — implementation report

## 1. Disposition and authority

- Task: `pa19-sharing-media-gateway-playback-foundation-ci-inventory-fix-gpt-20261108`; date: 2026-11-08; gate: implementation fix before code review. This reconciles the retained continuation-2 implementation report.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa19-sharing-media-playback-resume/codex-call`; branch: `codex/pa19-sharing-media-gateway-playback-delivery-resume`; unchanged HEAD: `6fe91a9ce210a91965b27771675019878d5d89fe`.
- Accepted plan SHA256: `da9f33775d0641fa92236e500bcdb27fa1944f1629d783697e38e478f0fe6b2a`. The original task, both cleanup adjudications and frozen plan remain authority. Continuation 2 resolves the previously reported append-only fixture contradiction; it is no longer an implementation blocker.
- Author disposition: `implementation-complete-controller-validated-pending-independent-review`. The one approved provider-neutral composed slice and all 20 author paths are implemented as candidate work. This resumes and preserves the retained 18-path implementation; it does not replace it with a different design. Controller PostgreSQL/Testcontainers full `clean verify` passed with fresh green XML; the exact embedded inventory also passes after the CI ordering fix. Independent code review remains pending. This report is not acceptance, readiness, approval, a GitHub CI result or gate advancement.
- No staging, commit, branch creation, push, PR/reviewer mutation, review dispatch, merge, approval or external comment occurred. Control remains Controller-owned and unchanged. The pre-existing untracked `.codex` marker remains untouched and excluded.

## 2. Exact author path set

Against HEAD, the actual candidate contains **13 new + 7 modified = 20 paths**, exactly the accepted 21-path set minus Controller-owned `docs/gateflow/project-execution-control.md`. Actual-minus-author-allowlist is empty; author-allowlist-minus-actual is empty. The `.codex` marker and ignored Maven-generated `backend/target` output are not author delivery paths. No existing partial path was removed. This follow-up changes only `.github/workflows/ci.yml` and this report; all other retained candidate paths remain untouched.

| Kind | Actual path |
| --- | --- |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/OpaquePlaybackCredential.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/VerifiedMediaClaim.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaCredentialVerifier.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/PlaybackStateReader.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/SharingPlaybackStateReader.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/ControlledMediaReader.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackService.java` |
| New | `backend/src/main/java/com/onecall/core/sharing/internal/playback/MediaPlaybackConfiguration.java` |
| Modified | `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionRepository.java` |
| Modified | `backend/src/main/java/com/onecall/core/sharing/internal/admission/JooqShareAdmissionRepository.java` |
| Modified | `backend/src/main/java/com/onecall/core/sharing/package-info.java` |
| New | `backend/src/test/java/com/onecall/archtests/sharing/MediaPlaybackServiceTests.java` |
| New | `backend/src/test/java/com/onecall/archtests/sharing/SharingPlaybackStateReaderTests.java` |
| New | `backend/src/test/java/com/onecall/archtests/MediaPlaybackConfigurationTests.java` |
| Modified | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` |
| Modified | `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java` |
| New | `backend/src/test/java/com/onecall/archtests/container/MediaPlaybackIT.java` |
| Modified | `backend/pom.xml` |
| Modified | `.github/workflows/ci.yml` |
| New | `docs/gateflow/pa19-sharing-media-gateway-playback-foundation-implementation-report.md` |

The POM's canonical diff is exactly one added Failsafe include, `**/MediaPlaybackIT.java`; no dependency, plugin or version changes. The CI change is confined to the existing inventory gate: explicit PA19 names, exact sets, aggregates and historical arithmetic. Other workflow jobs, dependency firewalls, contract validators and all old inventory names remain intact.

## 3. Implemented contract and test evidence

The orchestration retains the ordered ambient-transaction/input/verifier/claim/audience/purpose/local-time/state/evidence/final-time/open fences. It accepts no caller object location or identifier selector. Results are sealed and technical failures discard dependency details. State and evidence are checked before one exact bounded controlled open, with only in-memory equality validation after that boundary. No bytes, public transport, storage client or production credential format is implemented.

`SharingPlaybackStateReader` copies, rather than mutates, the qualified admission template into read-only REPEATABLE_READ/REQUIRES_NEW. It uses the sole new principal-checked, non-locking `findGrantById` SELECT and existing reads. It validates the PA18 canonical witness/hash, both generations, frozen/current bounds, membership, ownership, binding and risk without admissions, events or lifecycle mutations. An ACTIVE grant extension is monotonic; it cannot extend an already-frozen admission/session/claim cap. Sharing's module refinement is only `evidence::media-read`, beside the existing `evidence::share-projection` edge.

| Owner tests | Evidence implemented |
| --- | --- |
| `MediaPlaybackServiceTests`, 20 exact accepted names | Ordered denial/unavailable behavior, unchanged opaque verifier input, purpose/time/window/scope checks, real state adapter tuple failures, metadata authority, exactly one open, no post-open dependency/clock access and redacted root/nested JSON/string/log output. |
| `SharingPlaybackStateReaderTests`, 8 exact accepted names | Canonical witness and typed bounds, returned identity mismatches, generation/time precedence, current membership/ownership/binding/risk, transaction configuration/closure, faults and extension with revision+1 without frozen-bound renewal. |
| `MediaPlaybackConfigurationTests`, 4 exact accepted names | Inert/default/web behavior, explicit qualified opt-in, both provider registration orders, missing authorities and multiple candidates including primary-marked candidates. Defaults stay service-local Unavailable; no production adapter is registered. |
| Architecture tests, 2 new methods | Eight-file private partition, named-interface-only evidence imports, exact service/handle shapes, no write/lock/issuer/projection/provider-I/O path and preserved legacy maps/deny-lists. ModuleBoundaryTests remains 9 methods. |

Three retained unit method names did not match the accepted plan and were corrected, with CI synchronized: `validPlaybackResolvesVerifiedEvidenceAndOpensExactlyBoundedHandle`, `activeAndAdmittedLockedStateAllowWithoutMutation`, `grantAdmissionAndEvidenceTupleMustMatchVerifiedIdentity`. All 20/8/4/6 ordered method-name lists now match the accepted plan exactly. Additional assertions stay inside those methods. Log capture is injected through lifecycle methods, not test parameters, so Surefire's method names remain exact rather than gaining parameter-signature suffixes.

### Controller integration execution: six accepted methods

All six accepted methods **executed successfully in the Controller full run on PostgreSQL 16.15**: `MediaPlaybackIT` reports 6 tests, zero failures/errors/skips, in 44.75 seconds. The behavior below is executed test coverage, not only source intent; it is not a separate author-sandbox Docker run:

1. `qualifiedAdmissionAndEvidenceRolesResolveOneObjectAndOpenOutsideTransactions`: actual dedicated-role Hikari pools, current_user=session_user tracing, sharing read-only REPEATABLE_READ, evidence read-only single 15-column/one-parameter query, ACTIVE/LOCKED scope equality, transaction closure and no sharing-state mutation.
2. `committedRevocationRotationAndAdmissionMismatchDenyBeforeEvidence`: separate committed admin changes, actual code/revocation rotation, independent trusted-claim generation mismatches, existing cross-share/admission tuples and evidence mismatch, with zero evidence/open calls and unchanged post-mutation snapshots.
3. `witnessExpiryMembershipBindingAndRiskChangesFailClosedWithoutWrites`: legitimate extension and unchanged frozen rows/hash/session/admission/event/counts, one revision increment, successful bounded open, every frozen-bound overrun denied before evidence, deterministic frozen exclusive expiry, real membership/account/organization/binding/risk denials and schema-permitted inconsistent ownership/witness fixtures.
4. `missingAmbiguousAndInconsistentEvidenceNeverOpensControlledRead`: real PA19A Missing/duplicate/recording relationship/status results, the bounded adjudicated lifecycle below, and nullable-duration consumer failure through the evidence API seam.
5. `readFailuresPreserveAdmissionRevisionCountsAndAccessEvents`: actual role-password/login failures with pool eviction/restoration, connection failure, throwing controlled reader, identical durable snapshots and successful recovery attempts.
6. `typedOpenHasNoPostOpenDatabaseIoAndNeverDisclosesPrivateFacts`: checkout/SQL/repository/verifier/evidence/clock fences after open, captured JSON/string/log canaries, retained exact internal scope and real-role negative media/location/sharing access checks without SET ROLE escalation.

The nested fixture reuses unchanged `SharingQueryFixture.start/reset/admin/execute`, which configures `postgres:16` and asserts exactly 13 migrations. New subjects/bindings/media/recordings/evidence/artifacts/grants are inserted in FK order; normal scenarios use unchanged `DefaultShareAdmissionService` and deterministic providers for real admissions/canonical witnesses. Pools and DSL contexts are separate by owner. Test-only passwords are reset and resources closed.

Two constraints are handled explicitly rather than weakening guards: subject ownership cannot be changed after assignment, so the contradictory ownership case inserts a separate FK-valid grant/admission/witness fixture against an already-admitted evidence object, leaving the original subject/admission/witness untouched. The malformed-witness case inserts an additional guard-valid admission and malformed immutable event at creation, never updates a durable witness. Both happen outside the extension subcase; playback is measured only after fixture commits. PostgreSQL's VALIDATED recording shape forbids null validated_duration, so the nullable-duration case wraps a real resolved object with the existing API's optional-duration representation, not a disabled recording constraint. The expiry equality seam substitutes only repository time after performing the real now() query; it does not change database rows or add a production clock policy.

### Adjudicated duplicate fixture: Controller runtime proof

The successful Controller execution of `missingAmbiguousAndInconsistentEvidenceNeverOpensControlledRead` exercised the complete DROP/INSERT/cleanup/TRIGGER+INDEX restoration lifecycle below, including the final named-consumer-index rejection. Only this method drops schema guards. In that method's isolated container it drops exactly `evidence.uq_evidence_original_artifact`, `trg_evidence_artifact_immutable` on `evidence.evidence_artifact` and `trg_media_object_immutable` on `evidence.media_object`. It inserts exactly one additional media row and one second ORIGINAL artifact with fresh IDs, then invokes the actual evidence reader through playback and asserts Unavailable/one resolve/zero opens.

Its `finally` deletes only the added artifact ID constrained by its evidence/media IDs and the added media ID; recreates both exact BEFORE UPDATE OR DELETE triggers calling `evidence.reject_row_mutation()`; recreates the exact unique partial evidence_id/ORIGINAL index; and commits restoration atomically. It then verifies both enabled trigger definitions against their captured production definitions, the canonical exact index definition and absence of both added rows. The final attempted ORIGINAL uses a different, already-existing FK-valid media object; it asserts SQLSTATE `23505` **and the specific `uq_evidence_original_artifact` name**, avoiding a false positive from the separate evidence/role/media uniqueness constraint. No failure is swallowed. Restoration or verification failure fails the test.

The reject function is never dropped/replaced/altered. No replication-mode change, trigger disabling, other guard drop, migration edit, privilege grant or production-path relaxation is present. The green Controller method result and fresh XML establish execution of this lifecycle and all restoration/PostgreSQL assertions, not merely source intent. No SQL-by-SQL trace or deployed-system proof is claimed.

## 4. Measured source/CI/POM/XML arithmetic

| Inventory | Baseline | Final source and CI |
| --- | ---: | ---: |
| Surefire | 22 classes / 128 methods | **25 / 162** |
| Failsafe | 11 classes / 192 methods | **12 / 198** |
| Combined | 33 classes / 320 methods | **37 / 360** |
| Comment-inclusive line scan | 321 | **361** |
| POM Failsafe includes | 11 | **12**, exact CI class set |

Unit arithmetic is `128+20+8+4+2=162`; integration is `192+6=198`; total `320+34+6=360`. Failsafe per-class arithmetic is `64+10+10+8+12+18+8+37+9+8+8+6=198`. Historical PA18 additions remain `[9,6,1,13,1]`; separate PA19 additions are `[20,8,4,2,6]`. Subtracting both gives historical `112/178=290` and line scan 291; subtracting only PA19 gives its starting `128/192=320` and line scan 321; raw final is `162/198=360` and 361. The sole non-method hit remains the old PostgresContainerIT comment.

The CI fix moves the exact `pa19_additions` definition and merge loop before source-class equality, so all four new PA19 classes are visible when checked. Class/method sets and order, historical subtraction, arithmetic, source annotation checks, unsupported-test rejection, POM include equality, XML `exact_inventory` calls and unrelated workflow steps remain unchanged. No new method, dependency, skip or weaker assertion is added.

The exact embedded CI Python inventory was extracted without assertion changes and executed from `backend/` against the fresh Controller green Surefire/Failsafe XML; exit **0**. It verifies unique names, exact source=CI=XML class/method equality, annotation restrictions, all historical sets, all 12 POM includes and zero skips/errors/failures. Exact post-fix output:

```text
PA19 exact source=CI=XML inventory passed: 25 Surefire classes/162 methods; 12 Failsafe classes/198 methods (64+10+10+8+12+18+8+37+9+8+8+6=198); combined 37/360; historical 112+178=290 -> PA19-start 128+192=320 -> current 162+198=360; comment-inclusive word-boundary line scans 291 -> 321 -> 361 (one PostgresContainerIT comment hit, not a method); Failsafe includes 12; zero skips/errors/failures
```

The final Controller result is **25 Surefire classes / 162 methods + 12 Failsafe classes / 198 methods = 37 classes / 360 methods**, zero failures/errors/skips in both suites, including **MediaPlaybackIT 6/6** on **PostgreSQL 16.15**. These fresh XML reports replace the earlier mixed author-sandbox reports as the current validation evidence. Historical author runs were 45 focused green tests, 138 feasible non-socket green tests and an attempted full Surefire run with 21 socket-permission errors (CustomerMeWebTests 8, CustomerSharesWebTests 10, CustomerWebConfigurationTests 3); those limitations are not failures in the final Controller run.

## 5. Commands, toolchain and limitations

### Final authoritative Controller run

The Controller ran the following exact PowerShell command from `C:\Users\Administrator\.codex\worktrees\pa19-sharing-media-playback-resume\codex-call` (confirmed from its retained execution record):

```powershell
$log='C:\Users\Administrator\.codex\worktrees\pa19-sharing-media-playback-resume\controller-runtime\implementation-full-verify-retry1.log'; docker run --rm --name pa19-playback-full-verify-retry1 --add-host=host.docker.internal:192.168.65.254 -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -e TESTCONTAINERS_RYUK_DISABLED=true -v 'C:\Users\Administrator\.codex\worktrees\pa19-sharing-media-playback-resume\codex-call:/workspace' -v '//var/run/docker.sock:/var/run/docker.sock' -w /workspace/backend maven:3.9.9-eclipse-temurin-21 mvn -B -ntp -Pcontainer-smoke clean verify 2>&1 | Tee-Object -FilePath $log; exit $LASTEXITCODE
```

Environment: Maven 3.9.9 container `maven:3.9.9-eclipse-temurin-21`; Eclipse Adoptium Java **21.0.7**, `java.home=/opt/java/openjdk`; Linux/WSL2 `6.18.33.2-microsoft-standard-WSL2`; working directory `/workspace/backend`; Docker Desktop server **29.9.2**, API **1.56**, Unix socket `/var/run/docker.sock`; Testcontainers **2.0.5**, host override `host.docker.internal` and Ryuk disabled as shown. The observed database is **PostgreSQL 16.15**, image `postgres:16`, image ID `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`.

Authoritative log: `C:\Users\Administrator\.codex\worktrees\pa19-sharing-media-playback-resume\controller-runtime\implementation-full-verify-retry1.log` (WSL: `/mnt/c/Users/Administrator/.codex/worktrees/pa19-sharing-media-playback-resume/controller-runtime/implementation-full-verify-retry1.log`). The UTF-16 log ends with **BUILD SUCCESS**, total time **10:36 min**, exact final runtime timestamp **`2026-11-08T07:44:32Z`** (`2026-11-08T15:44:32+08:00`). This is Controller runtime validation, not an author rerun or a GitHub CI/PR result.

### Earlier author-sandbox commands and limitations

The checkout's `.git` contains a Windows path. Read-only Git inspection uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call21` and `GIT_WORK_TREE` set to this checkout; Git metadata is not edited. The default Windows Java shim fails with WSL `UtilBindVsockAnyPort:309: socket failed 1`. The earlier author Linux toolchain was `/opt/jdk21` (Temurin 21.0.13) and `/opt/maven-3.9.9`, distinct from the final Controller container above. Existing `/root/.m2/repository` artifacts were copied into `/tmp/pa19-m2`; those author Maven invocations were offline, with no new dependency/plugin or network fetch.

Historical executed commands (author Maven runs use `JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH`):

```bash
# Repository root: complete main/test compilation, including all six IT methods
/opt/maven-3.9.9/bin/mvn -q -o -f backend/pom.xml -Dmaven.repo.local=/tmp/pa19-m2 -DskipTests test-compile

# backend/: attempted full Surefire run (21 socket-restriction errors)
/opt/maven-3.9.9/bin/mvn -q -o -Dmaven.repo.local=/tmp/pa19-m2 -DargLine=-javaagent:/tmp/pa19-m2/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar test

# Repository root: feasible non-socket selection (138 green)
/opt/maven-3.9.9/bin/mvn -q -o -f backend/pom.xml -Dmaven.repo.local=/tmp/pa19-m2 -DargLine=-javaagent:/tmp/pa19-m2/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar '-Dtest=*Tests,!CustomerMeWebTests,!CustomerSharesWebTests,!CustomerWebConfigurationTests' test

# Repository root: final focused run (45 green; all IT source also compiled)
/opt/maven-3.9.9/bin/mvn -q -o -f backend/pom.xml -Dmaven.repo.local=/tmp/pa19-m2 -DargLine=-javaagent:/tmp/pa19-m2/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar -Dtest=MediaPlaybackServiceTests,SharingPlaybackStateReaderTests,MediaPlaybackConfigurationTests,SharingQueryBoundaryTests,ModuleBoundaryTests test

# Repository root: actual CI source/POM assertions and independent exact sets
python3 /tmp/pa19-validate.py
docker info --format '{{.ServerVersion}}'
```

The earlier Mockito startup agent was command-local because inline self-attachment is unavailable in this sandbox; no POM workaround is added. The author Docker probe failed with permission denied connecting to `/var/run/docker.sock`; no escalation is available. Controller subsequently executed the real full clean/container run recorded above; no Testcontainers result was simulated, skipped or represented by fabricated XML. No separate default/opt-in web-jar smoke execution or hosted CI pass is claimed here.

Execution logs are `/tmp/pa19-compile-final.log`, `/tmp/pa19-unit.log`, `/tmp/pa19-feasible-unit.log`, `/tmp/pa19-focused-final.log`, `/tmp/pa19-static-final.log`, `/tmp/pa19-identity.log` and `/tmp/pa19-frozen-diff.log`. Current fresh Controller XML is under `backend/target/surefire-reports` and `backend/target/failsafe-reports`. Post-fix exact inventory output is also retained in `/tmp/pa19-ci-inventory-post-fix.log`. `git diff --check` passes; new author files are also whitespace-checked using `git diff --no-index --check /dev/null <path>` (exit 1 means a nonempty new-file diff, not a whitespace error). Final exact path comparison and frozen-boundary checks are repeated after this report is written.

## 6. Immutable inputs and identity

All **V001–V013 raw working-file SHA256 values match the accepted plan's starting manifest**; all canonical Git blobs also match HEAD. There are exactly 13 migration files and no V014/new migration, role, schema or privilege change. Inherited CRLF migration bytes are preserved, not normalized against LF repository blobs. `api/` is unchanged: every file matches both canonical HEAD and its existing Git checkout-filter bytes. The raw OpenAPI YAML SHA256 is `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`.

The accepted plan retains its supplied SHA256. The fix, all four review artifacts, Control, PA18/PA19A behavior/source/tests, other fixtures/resources, design documents, OpenAPI and frontend paths have no candidate diff. The complete migration/OpenAPI raw manifest and canonical comparisons are in `/tmp/pa19-identity.log`; the frozen-path diff is empty. Final report identity is emitted outside this report to avoid a self-hash.

## 7. Residual ownership and handoff

| Residual | Owner / disposition |
| --- | --- |
| Fresh PostgreSQL 16 / Testcontainers, fixture restore execution, least-privilege and complete green XML gate | Controller runtime validation completed on PostgreSQL 16.15: 25/162 + 12/198 = 37/360, zero failures/errors/skips; post-fix exact inventory passed. Evidence informs later independent review/decision, not acceptance. Author Docker/socket restrictions remain local validation limitations, not product capability. |
| Independent code review and gate decision | Controller; not requested, performed or advanced here. |
| Authenticated issuance-time claim provenance, signature/key/wire verifier | Security + Platform + Sharing provider integration; default verifier remains Unavailable. |
| Actual private-location/storage controlled reader and byte delivery | Platform + Security + Evidence provider integration; default reader remains Unavailable; no cloud/resource/transport implementation claimed. |
| TTL/skew/revocation propagation and snapshot-to-open race | Security + Sharing + Platform / Q22; exact current bounds implemented without invented numeric policy or continuous byte recall. |
| Gateway/player/origin/CDN/range contract | Infrastructure + Security / `R-A3-MEDIA-GATEWAY-TRANSPORT`; outside this slice. |
| Deployed collector/edge/analytics redaction | Platform + Security + Observability / `R-A3-REDACTION-RUNTIME`; local redaction tests are not deployed-system proof. |

No genuine unresolved implementation contradiction remains under adjudication 2. Production provider deployment is deliberately absent, and future cross-module providers still require the accepted named-interface/module-map and docs/12 placement review. The Controller-validated implementation candidate is handed back with independent review still pending, without initiating that review or making a PR, hosted CI, approval, acceptance or readiness claim.
