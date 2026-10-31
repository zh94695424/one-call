# PA9 V009 Sharing Foundation — S3 Completion 1

## Current aggregate-fix evidence — 2026-10-31

Task `pa9-v009-aggregate-codefix-gpt-20261031-01`; aggregate-review **fix only**, intake/current HEAD `96db75a17f6ea2b657cfb06d30dfcdf8d88f4dfc`. This section supersedes current-state claims below; all earlier execution history and raw identities remain historical. The complete author evidence, commands, identities and residuals are in `docs/gateflow/pa9-v009-sharing-foundation-aggregate-fix-1.md`. This is not aggregate re-review acceptance.

- AF-1 fixed: cumulative PA0–PA9/V001–V009 class javadoc explicitly covers V008/V009 and exactly 64 methods.
- AF-2 fixed: V006 inventory javadoc describes the final V009/no-V010 live fence and retained staged V008 prefix booleans; its body is unchanged.
- AF-3 fixed: SQL-only harness checks current ACTIVE/LOCKED status and statement-clock validity before fact replay; new admissions retain the existing ACTIVE/generation/limit checks. ACTIVE/LOCKED same-key replay passes; legal REVOKED/EXPIRED transitions are followed by same-key DENIED witnesses with current generation, exact unchanged admission/event/count/revision rows and a committed fresh-connection comparison. All 18 existing Lock races remain. No production service or policy is added; admission-key 129 logic is unchanged (B non-finding).
- Provenance corrected additively: historical S3 raw hashes describe the controller's mixed-LF/CRLF working tree, not canonical committed LF blobs. Intake canonical Git-content SHA256: Java `5e6a4fd04d06c94dddd443551fbdcc25839cccfeb1cb489a434fed27eed45d0d`, CI `7bffd120aa3da912977aca923a375cc814dc67803614a55fdfd0204df6b1d55f`, README `93cdb627cdd481e2873ac98078ccc6e0485555c65ce56ef37676cbce6f074b41`. Final uncommitted Java LF-normalized SHA256 `336f2b0669da4fb4d981affc46d4ca1a5eea334509602428c77327bb8edc890f`; environment-specific raw `2403afc612a1b7c9f460272387fff0d7b44b769f73fff34a16e16417b91ba8f6`. CI/README remain byte-identical; historical review/fix artifacts are not rewritten.

Actual commands from `backend`, with `JAVA_HOME=/opt/jdk21` and that JDK first on PATH; Maven `/opt/maven-3.9.9/bin/mvn`:

| Command / audit | Actual result |
| --- | --- |
| `mvn -B -ntp test-compile` | exit 0, BUILD SUCCESS, 7.129 s |
| `timeout 240s mvn -B -ntp -Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009AdmissionCountOnceLimitAndAtomicRollbackBackstops+concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner' verify` | exit 0, 40.179 s; Failsafe 2 + Surefire 10, zero failures/errors/skips; ACTIVE/LOCKED replay, REVOKE/EXPIRY denial, 18 Lock/7 rollback/2 cleanup markers |
| Exactly one `timeout 240s mvn -B -ntp -Pcontainer-smoke verify` | exit 0, 59.458 s; Failsafe 64 + Surefire 10, zero failures/errors/skips; same markers |
| `python3 /tmp/pa9-aggregate-fix-01/audit.py preflight /tmp/pa9-aggregate-fix-01/preflight-xml`, then `python3 /tmp/pa9-aggregate-fix-01/audit.py final` | exit 0; base/slice ancestry, accepted plan/A/B pins, V001–V008 base equality/plan pins and frozen V009 verified; independent source=CI=XML exact 64, Surefire 10; allowed scope/frozen raw bytes/empty index/whitespace verified |
| `cd backend && python3 /tmp/pa9-aggregate-fix-01/ci-validator.py` (unchanged extracted CI heredoc) | exit 0, `container-smoke report assertions passed` |
| `docker ps -a --format '{{.ID}} {{.Image}} {{.Names}}'` | exit 0, empty after cleanup |

Runtime/provider: OpenAI assistant through Codex CLI, Linux/WSL2 amd64, Temurin 21.0.13, Maven 3.9.9, real Docker Desktop engine 29.9.2 and Testcontainers `postgres:16` / PostgreSQL 16.15. No subagent; backing model/provider routing is not independently terminal-attested. Logs/XML/audits and final non-circular file-hash manifest are under `/tmp/pa9-aggregate-fix-01`. Full log SHA256 `d9002f902896c38c49349b932fc7d5f9f7749e3d624802029e63029ceda9209c`; full Failsafe XML `e9e98b4d8a6292ce38f568b4142c72f88fc62a435df9ef00812248e92ca968a7`.

Bounded-race/timing, CHECK UNKNOWN, policy/provisioning and remote-CI residuals are unchanged and not addressed. V001–V009, CI, README, plan/design/controller, historical artifacts and pre-existing `.codex`/`build/` bytes remain untouched. No stage/commit/push/PR/review/approval/comment or external workflow action. Stop at author aggregate fix; fresh aggregate A/B re-review is separate.

## Current S3 code-fix evidence — 2026-10-31

This section supersedes the historical pre-fix source/evidence identities below, not their retained execution history. Task `pa9-v009-s3-codefix-gpt-20261031-01`, intake/current HEAD `209b658e249ef26e22be69db77726de63cc255f4`, gate **fix** only. F1 is fixed: README lines 3/117 now say 64; its only remaining 62 explicitly describes the accepted S2 checkpoint. F2 is fixed: only `v009S3Fixture()` changed; its outer `Exception | Error` handler independently removes the possibly committed synthetic account and cleans the committed V008 fixture, suppresses cleanup failures, and rethrows the original setup failure. Rollback failures also become suppressed rather than replacing that failure. All 64 test bodies, normal S3 cleanup and every other helper/assertion are unchanged.

| Current item | SHA256 |
| --- | --- |
| Java, raw checkout | `9f089e03efd3bafce68e949dd590d8aaf8f29a943c90a9da43829a33f8e9dcb1` |
| README, raw checkout | `c9395e749738e7c1435840bf88d8026ff8a4b32879efb9817a1b8356db73132f` |
| CI, unchanged raw checkout | `9f04d0b0a710be0143b7977aa20a28f3492f0d281ac59f7bbb456da862f09238` |
| Three-source diff against current intake HEAD | `b79ab38c3e4cd736a350ed53d89e64845f50d605bdb075ec4e63d236894981ad` |
| V009, unchanged canonical Git bytes | `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae` |
| Focused Failsafe XML | `cac228428c3ddb091507a21b8c17713169414e5b46cb0a3f682abcf289b8d9fb` |
| Full Failsafe XML | `f9e604fe5137050c65850f81d87fc1f06baa510c26e1b6ac3c02554919ff5f61` |
| Focused Maven log | `27ed625d3ddde2179a015982a5631becf2e454aa6a3822eb3ca72bd461ce5c8e` |
| Full Maven log | `e8387ae9a5e719f575e7cdcb6d5f17dabd682ca7f2de959b6753e8c4e05341ad` |
| Real-PostgreSQL setup-failure probe log | `9699a8a6cd8df2283dcde8a891a4d2e4859836c5aed858d0181c6be6ce9045cf` |

Required author validation passed on Java 21.0.13/Maven 3.9.9/Docker Engine 29.9.2/PostgreSQL 16.15: test-compile; focused S3 **2 Failsafe + 10 Surefire**; exactly one bounded full run **64 Failsafe + 10 Surefire**, zero failures/errors/skips. Independent source=CI=XML exact-set parsing and the unmodified CI validator passed. Each run retained 18 Lock observations, seven rollback/no-residue cases and two cleanup/restoration markers. Five additional external JDBC fault probes cover setup connection failure, assertion plus rollback failure, post-commit failure, account-cleanup close failure and V008-cleanup close failure. Every probe retained original exception identity, checked suppression, and restored exact all-table rows plus trigger/FK flags. Normal setup/cleanup also restored the same baseline. No test container remains; V001–V009 and CI are frozen.

Current logs/XML/scripts, final record hashes and non-circular manifest: `/tmp/pa9-s3-fix-01/`. Full commands, review identities, findings, final I3/E3 hashes, failed scratch attempts and residual ownership are in `docs/gateflow/pa9-v009-sharing-foundation-s3-code-fix-1.md`. One scratch javac invocation inadvertently enabled the Modulith annotation processor and regenerated pre-existing root `build/generated-spring-modulith/javadoc.json` (current bytes `[]\n`); no manual build edit occurred and it remains excluded, but strict no-build-write compliance is not claimed. The retry used `-proc:none` outside the checkout. Earlier source/evidence and failed-attempt records below remain historical, not current validation.

Runtime/provider: OpenAI assistant through Codex CLI on WSL2; actual backing model/provider route is not independently attested by shell/database output. Residual ownership is unchanged: Backend QA owns synthetic five-second timing and bounded races/CRLF parsing; Sharing/Backend/Security owns service semantics and CHECK UNKNOWN boundaries; Security/Product owns policy; Platform/Database/Security/Legal owns provisioning/storage; Controller/QA owns remote CI, archival/provider attestation and separate fresh dual re-review. No remote CI or external workflow action occurred. This is author fix completion, **not re-review acceptance**; no stage/commit/push/PR/review/approval/external comment occurred.

## Historical pre-fix S3 record

## Task and author outcome

- Task label: `pa9-v009-s3-implementation-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`; gate: `implementation S3` only, final approved slice; date: 2026-10-31, Asia/Shanghai.
- Runtime/provider: Codex CLI/GPT in the OpenAI assistant session; controller-requested `gpt-6.1-sol` / `high`. Actual backing provider/model identity is not independently attested by the local terminal evidence; no provider substitution or delegation occurred.
- Status: **S3 author implementation-complete; required local database validation passed**. This is not controller acceptance, independent review, remote CI green, PR state or production readiness.
- Intake/final HEAD: `a95f96dfa4ebb4e73271d1c53c2e792eb1c98087`; branch: `codex/pa9-v009-sharing-foundation`; accepted S2 ancestor: `6111a535918ca0d53be893351d736558fa5aaa3a`, verified exit 0.
- Accepted plan: `docs/gateflow/pa9-v009-sharing-foundation-plan.md`, unchanged SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`. Goal Confirmation/current checkpoint, accepted plan, frozen V009 and existing S1/S2 fixtures/helpers were read before editing; no authority deviation was introduced.

## Exact path and source identity

Exactly five authored repository paths: Java test (T), CI (C), README (R), S3 implementation record (I3) and this completion record (E3). No other tracked source or unapproved new source is part of this task. Existing untracked `.codex`/`build/` are kept distinct; generated Maven/ignored Node/bundle output and retained logs are not committed artifacts.

| Changed path | Final raw checkout SHA256 |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `eb84f41a547f7c3d675f4d6106cc2ddb475b928d90eb5f32f9e18d114a022175` |
| `.github/workflows/ci.yml` | `9f04d0b0a710be0143b7977aa20a28f3492f0d281ac59f7bbb456da862f09238` |
| `backend/README.md` | `1c1f94b31674121446484033d3e7d706b2cff641de4e5035a17dc443e769bcb9` |
| `docs/gateflow/pa9-v009-sharing-foundation-s3-implementation.md` | `78e99a936851b2279613f8d98eda45e2f1d24cdbcd8a0c09ff382d64653729ac` |
| `docs/gateflow/pa9-v009-sharing-foundation-s3-completion-1.md` | Self-hash recorded externally in `/tmp/pa9-s3-01/SHA256SUMS` and the author final response, not circularly embedded |

T/C/R diff against intake SHA256: `4dc8808070a44a7827020f8ae09560e6bdbc61b92dddd016548689f3e8f8965f`. Final source provenance is unchanged across the final focused run and single final full run. I3 contains the exact 64-name shared source/CI/XML set, commands, tool/image versions, separate raw/canonical migration hashes, race/rollback outcomes and all retained run XML hashes. External `/tmp/pa9-s3-01` includes the tested diff, source hashes, XML, actual validator and independent audit scripts/results; no source hash is inferred from intended commands.

## Implemented boundary

- Added only `v009AdmissionCountOnceLimitAndAtomicRollbackBackstops` and `concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner` as test methods, plus private fixture/SQL helpers. All 62 accepted test method bodies and all 31 existing V009 helper bodies remain identical to canonical intake source.
- SQL-only real-sharing_role admission transaction proves owning grant lock/current read, replay-before-limit/INSERT, next admission insertion, exact synthetic redacted `ADMISSION_GRANTED`, one-row expected-revision/count update and commit. Original-key fact replay works at max=1 and while LOCKED; new keys are refused. It is not production authorization, session/ticket reconstruction or service consumption evidence.
- Observation events and a separately committed `TICKET_FAILED` shape preserve count/admission facts; event-domain/id replay and exact unique collision identity are asserted. No ticket is issued and no production hash/KDF/TTL policy is invented.
- Seven whole-transaction rollback cases cover sentinel, exact duplicate event/key/number constraints, guard refusal, explicit rollback after zero-row stale expected revision/current reread, and transition/event plus rollback-only existing audit/Outbox appends. Fresh connections prove full baseline state/history and nested-row absence.
- Eighteen bounded two-connection READ COMMITTED races cover separate grant share-ID/token-hash and event identities (winner commit/rollback), same key including max=1, distinct last-slot keys (winner commit/rollback), and admission vs revoke/rotation/expiry/LOCK in both orders. Every case observes a real backend Lock wait before winner release and pins fresh identities/count/status/generations/revision and sentinel absence. Legal lock-then-rotation has two separately conditional updates; its final revision explicitly accounts for the prerequisite lock rather than treating forbidden ACTIVE same-state rotation as an owner action.
- Finally cleanup restores the two sharing immutable triggers and existing V008 named trigger/constraint controls, checks all scoped sharing IDs absent and reasserts V009 catalog/triggers/six FKs plus V008 FK flags. No explicit foreign row lock, sleep-only race, global bypass, all-row cleanup, leaked trigger state or new production service was added.

## Executed validation

Java `21.0.13+9` (Temurin), Maven `3.9.9`, real Docker Engine `29.9.2`/Desktop `4.97.0` and actual PostgreSQL `16.15` (`postgres:16`, observed image/repository digest `sha256:0cefaabd882e42bcb2a2f3dc56d3ce4770560ba62fd7f197f1a2e008fd5731ab`). Maven uses the installed `/opt/maven-3.9.9/bin/mvn`, `JAVA_HOME=/opt/jdk21`, `PATH=/opt/jdk21/bin:$PATH`. Exact command expressions, timeouts, logs and source provenance are in I3; elapsed times below are actual integer wall seconds.

| Validation | Exit / elapsed | Result |
| --- | --- | --- |
| test-compile corrected retry | 0 / 8 s | Compiles without skip flags |
| default verify | 0 / 13 s | Surefire exactly 10, failures/errors/skips 0 |
| final-source focused S3 verify | 0 / 46 s | Failsafe exactly 2 plus Surefire 10, all zero failures/errors/skips |
| targeted V007/V008 fence verify | 0 / 24 s | Failsafe exactly 5 plus Surefire 10, all clean |
| Exactly one final bounded full container-smoke verify | 0 / 58 s | **Failsafe exactly 64 plus Surefire exactly 10; failures/errors/skips all 0** |
| Independent source/CI/XML audit | 0 / 20 s | Totals, distinct names and exact sets all equal 64; unit XML separately totals 10 |
| Actual CI XML validator, unmodified extracted heredoc | 0 / less than 1 s | Passed against actual final backend reports |
| Boot jar smoke / dependency firewall | 0 / 2 s; 0 / less than 1 s | Started marker + normal exit; no forbidden packaged runtime dependencies |
| Unchanged contract-regression job on external canonical-LF HEAD snapshot | 0 / 59 s | Node pin, A1 S3, A2 S3, A3 S3, A4 S2, A5 S1/S2 all passed |

Final full run finished at 2026-10-31 17:45:37 +08:00. Failsafe XML SHA256: `20cdcb931c7a1972a40981c3f2ed2715eaff34b4c464c4394ffdeb605135b95d`; full log SHA256: `e812a99b69dbf137550e485bf75f5f5d4fb222a7d47946a98f38bd0de594bc43`. Three final Surefire XML hashes and every focused/fence/failed-run XML are enumerated in I3 and the external manifest. `reports-summary.json` parses all retained report totals independently. All 31 CHECK/32 NOT NULL/eight key probes from accepted S2 remain present in final XML; cumulative catalog/ACL/FK/function tests are preserved.

### Failed attempts retained

- Initial ordinary Git reads failed on the Windows `.git` pointer. Explicit read-only Git metadata resolved them; no metadata/config repair or repository mutation was made.
- First test-compile exited 1 in 7 s on ambiguous AssertJ overload for SQLException/Iterable. Only the new assertion cast was corrected; log retained.
- Focused attempt 1 exited 1 in 35 s: 2 tests, one failure, no error/skip; all 18 races passed, but the test tried direct audit SELECT under sharing_role. Only the privileged test observer was corrected (append functions still execute under sharing_role). Focused attempt 2 then passed 2+10 in 47 s; attempt 3 strengthened a bounded stale reread and independent loser-token oracle, then passed 2+10 on final source. No predicate/privilege was weakened.
- The first generated README patch was rejected before modifying files because of unsupported hunk syntax; the scoped supported patch succeeded. One scratch CI-extraction attempt used the wrong cwd; root extraction succeeded and the actual validator passed. These are setup diagnostics, not successful test executions.
- Supplemental raw-checkout contract execution exited 1 in 42 s; initial `git archive HEAD` snapshot repeated that failure in 69 s because it inherited CRLF conversion. A1 passed; A2 reported A0 registry/column parse drift. No source was repaired. A new external `git -c core.autocrlf=false archive HEAD` snapshot was asserted against canonical Git A0 bytes (zero CRLF); every unchanged contract step then passed. Original failed logs/results remain retained. Windows-checkout parser portability remains an explicit QA/CI residual, not remote CI success.

## Frozen evidence and final audit

V009 remains raw-byte identical to intake Git, SHA256 `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`. V001–V008 canonical Git blobs equal original base `835f2ec` and intake/final HEAD; checkout resources match those canonical bytes after the existing CRLF/LF comparison, with their unchanged raw checkout hashes separately recorded in I3. No migration, POM, application source, test resource, accepted plan/design, controller, prior artifact or V010 change occurred. Controller remains `82e20669b483a71d1d9d1e328373c38c3803e13264d40d8c618f09f149ec53e0`; `.codex` remains empty and unchanged.

Actual post-record `python3 /tmp/pa9-s3-01/audit.py /tmp/pa9-s3-01/final-xml` exits 0 in 15 s: source=CI=XML exact 64, separate Surefire 10, all 62 accepted test bodies/31 V009 helper bodies unchanged, frozen migrations/plan/HEAD/ancestor and only the five allowed task paths verified. `git diff --check` exits 0 in 13 s; each new-record `git diff --no-index --check /dev/null <path>` exits 1 solely for the new-file difference with zero diagnostic bytes. Cached diff is empty (exit 0), pre-existing `.codex`/`build/` stay separately identified, and no test container remains. After this outcome note, the same independent audit is repeated into `final-scope-audit.json` and the external manifest is generated from final bytes, including this record's non-circular self-hash. No further Maven full run is required or performed for these evidence-only record writes.

## Residual risks and stop

Sharing/Backend/Security retains excluded production service replay/authorization/typed JSON/binding/risk/recipient/fingerprint/hash obligations; frozen CHECK UNKNOWN and independent grant/admission FKs are not hardened by this work. Security/Product owns Q12/Q22 code/KDF/TTL/limit/ticket policy. Platform/Database/Security owns provisioning/image pinning/storage/legal/KMS. Backend QA owns bounded-race/throughput/deadlock limitations and CRLF-sensitive contract parsing; canonical-local success is not direct-Windows-checkout or remote CI success. Controller owns provider attestation, evidence archival and any subsequent independent review/acceptance. There is no unresolved local S3 database blocker; these residuals remain outside this gate.

No sharing service/API/worker, Redis/COS/KMS integration, ticket issuance/reconstruction, playback, production policy or V010 was implemented. No stage, commit, push, branch creation, PR/issue/reviewer request, code review, approval, external comment or other external workflow action occurred. Stop at author S3 completion; do not enter a later gate or claim acceptance/production readiness.
