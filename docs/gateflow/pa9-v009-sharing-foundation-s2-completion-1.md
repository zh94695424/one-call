# PA9 V009 S2 completion record 1

- Task `pa9-v009-s2-implementation-gpt-20261031-04`; work unit `PA9-v009-sharing-persistence-foundation`; gate implementation S2 only; 2026-10-31 Asia/Shanghai.
- Status: **implementation-complete; local validation passed; NOT review/controller acceptance or production readiness**.
- Provider/runtime: OpenAI coding assistant / Codex CLI on Bash/WSL in the existing Windows-managed worktree. Controller route is GPT `gpt-6.1-sol` / high; exact provider-side model identity is not independently exposed here.
- Branch `codex/pa9-v009-sharing-foundation`, unchanged HEAD `a9f9ea3a59edfb6e6ede5a75a0d3e2c1e1ea56df`; accepted S1 ancestor `9b5edd68769745de12efda178bbca106f9719328` verified.
- Accepted plan `docs/gateflow/pa9-v009-sharing-foundation-plan.md`, SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57`; read with current control checkpoint before editing.

## Delivered paths and identity

Exactly five task-owned paths: three sources below; `docs/gateflow/pa9-v009-sharing-foundation-s2-implementation.md`; this new completion record. No other source/control/design/migration path edited. Pre-existing untracked `.codex` and `build/` remain; Maven-generated outputs are not task source artifacts and no manual edit/cleanup occurs there. Tested-source diff SHA256 `957d6c1e35093e7119f011721356117efcc0fce8efbdc6648c56c6af4a20c684`; all five final file hashes are emitted externally, avoiding circular self-hashes.

| Tested source | SHA256 |
| --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `8d11f3f29e63d16d9eb7a23a014bcf4976193aac3363ab62b49eaaa9b55e2d26` |
| `.github/workflows/ci.yml` | `38dad8de228dde846d21f171c15ab81be6790ca6ee65c5c7a925df86610d9f43` |
| `backend/README.md` | `8b1e50d41a13d8e641b97df83bc2ac6635d88302cab07da6dca15c5963b266cc` |

Only the two approved S2 methods are added; all prior 60 @Test bodies remain identical; both S3 methods absent. Cumulative source = CI literal expected-set = Failsafe testcase **exact set/total 62**, independently extracted; Surefire **10**, separately parsed. All zero failures/errors/skips in the final run. CI continues rejecting missing/duplicate/failed/error/skipped inventories.

## Actual command outcomes

All Maven commands execute from the repository root with `JAVA_HOME=/opt/jdk21`, `PATH=/opt/jdk21/bin:$PATH`, installed `/opt/maven-3.9.9/bin/mvn`, `-f backend/pom.xml -B -ntp`; no wrapper modification or skip flag. Bounds are external `timeout`, never widened. Wall elapsed values include the Maven launcher; Maven-reported times remain in logs.

| Actual command suffix (after the common Maven prefix) | Bound | Exit | Wall elapsed | Reports |
| --- | --- | --- | --- | --- |
| `test-compile` | 600 s | 0 | 10.595 s | compilation, no test execution claim |
| `verify` | 600 s | 0 | 12.940 s | 10 Surefire, 0 failures/errors/skips |
| `-Pcontainer-smoke '-Dit.test=PostgresContainerIT#v009ShapeDefaultsAndAppendOnlyBackstops+v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops' verify`, attempt 1 | 600 s | 1 | 31.946 s | 2 Failsafe, 0 failures / 1 error / 0 skips; 10 Surefire pass |
| Same focused command, attempt 2 | 600 s | 0 | 35.173 s | 2 Failsafe + 10 Surefire, all zero failures/errors/skips |
| Same focused command, final attempt 3 after test-only restoration/coverage hardening | 600 s | 0 | 33.755 s | 2 Failsafe + 10 Surefire, all zero failures/errors/skips |
| `-Pcontainer-smoke '-Dit.test=PostgresContainerIT#v007ForeignKeyAndLateOrderConsentFkBackstops+v008EvidenceInventoryAndGuardShapesAreExact+v008OwnerPublicRuntimeAndCrossModuleAclsAreExact+v008ForeignKeyAndDeferredCreationCycleFkBackstops+v008CreationCycleBothOrdersAndTerminalLifecycleBackstops' verify` | 900 s | 0 | 24.005 s | 5 Failsafe + 10 Surefire, all zero failures/errors/skips |
| `-Pcontainer-smoke verify` (exactly one final cumulative invocation) | 1500 s | 0 | 44.201 s | **62 Failsafe + 10 Surefire, all zero failures/errors/skips** |

Actual failed focused attempt 1 is retained, not counted as a pass: the deferred V008 evidence creation-cycle trigger ran at fixture COMMIT under sharing_role, producing `42501/permission denied for table evidence_event`. The test authoring fix executes `SET CONSTRAINTS ALL IMMEDIATE` while still evidence_owner, before switching to sharing_role. No migration, privilege or assertion weakening. The shape method already passed in that failed attempt. Attempt 2 passed; final focused attempt 3 additionally checks restoration in finally, invalid/non-code status/rotation shapes, and generation deltas from both ACTIVE and LOCKED. No full run was attempted until those changes passed.

An early independent inventory audit also failed on stale README cumulative 60 references; those were corrected to 62 before the final focused/full evidence. No failed audit is presented as a pass. The final audit uses a numeric-count-specific README check rather than an indiscriminate substring search.

The real PostgreSQL 16.15/Flyway path runs inside Docker Desktop Engine 29.9.2 (CLI 29.1.3, Desktop 4.97.0), local postgres:16 alias/image provenance in I2; Java 21.0.13+9 / Maven 3.9.9 / Testcontainers 2.0.5 / Flyway 12.4.0. Exactly one final full run, **44.201 s wall, exit 0, 62+10**; full log SHA256 `056e0691f58b75a31c4338e337cf13a1452087c68b4e965c05cdb332b2d72c16`, Failsafe XML SHA256 `1ce2de0e3022d93396b4b81feb137e49c53d0782ce3899fdac6e3a4a4c57f116`. Final focused log `d086190d5a9ee351b40242239d6756e6f19f9e0c95f499403683c948a950ea35`, XML `c844767d8e078f2ab022a5fa1fd3c3045dd4398111e060eb3bdaece0ea02d0a8`. Failed attempt 1 and all focused/default/fence/full XML/log hashes are retained in I2, not hidden or described as passing.

## Proven evidence and limits

- All 31 named CHECKs have real valid/boundary controls plus **123** concrete false exact-server-name `23514` probes; CHECK UNKNOWN and nested absent/JSON-null behavior preserved. Nine omitted defaults, all 32 NOT NULL columns with `23502` table/column, and all eight PK/UNIQUE names with `23505` are exercised.
- Owner append-only no-op UPDATE/DELETE produces exact `P0001/SHARING_ROW_APPEND_ONLY`; runtime denies are `42501`. All 12 frozen grant columns independently prove owner immutable/runtime denial.
- All 16 status pairs; six legal named directed edges; legal revision +1 versus old/old−1/old+2; eight stale-WHERE explicit rollback/reread/fresh-restoration probes. Extension, early expiry, status/terminality, decrease/partial rotation/generation-coupling diagnostics match exact frozen labels. 109 labeled mutation probes in final output.
- Same-state terminal and LOCKED generation increments, ACTIVE code rotation and mixed lock+extension succeed structurally but are explicitly **service-forbidden**; only LOCKED→ACTIVE is a legal rotation action. No new DDL prohibition or authorization claim.
- Successful probes roll back: complete row/history snapshots, savepoint restoration, transaction rollback and fresh-connection equality; scoped owner CHECK/guard isolation restores exact catalog/triggers unconditionally. Committed lifecycle fixture setup is method-local; cleanup and fresh absence/catalog/V008-FK checks pass.
- Count-update witness proves only revision shape. No S3 admission count-once/limit/admission-event-count rollback transaction/concurrency implementation or claim.

## Frozen bytes and final validation

V009 raw SHA256 **`ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`** unchanged. All V001–V008 original-base canonical Git blobs equal intake HEAD blobs, no migration diff; checkout resources also match after LF representation normalization. Exact eight hashes, staged-chain/live-fence and unchanged five-service evidence are in I2.

Independent audit and actual extracted CI XML validator both exit 0. Boot jar smoke exits 0 in 2.032 s; unchanged production dependency firewall passes. No remote CI run claimed; unchanged Node/OpenAPI/IVR/security contract jobs not run in this local S2 gate. Final post-record exact-scope/whitespace/newline and frozen-byte audit is required before author handoff; actual final outcome is appended only after it runs. Final `/tmp/pa9-s2-04/SHA256SUMS` records retained evidence and final five-path hashes externally.

## Residual risks and stop

No unresolved local blocker. Independent review/controller acceptance is pending, not inferred. Sharing/Backend/Security retains JSON semantics, service authorization and action/replay discipline, plus all S3 atomicity/races. Product/Security retains unapproved KDF/TTL/limit policy; Platform/Database retains production provisioning/image pinning. Local raw model identity and remote CI/PR state are not independently verified. No sharing application service/API/worker/integration, V010, SQL change or production policy delivered.

No Git stage/commit/branch/remote/push/PR/issue/review/approval/external-comment action occurred. Stop after final S2 records/validation; no later gate entered.

## Final post-record result

Post-write independent audit exits 0, including exact source=CI=XML 62, Surefire 10, old-method preservation, five-path scope, frozen SQL/plan/ancestor and newline/trailing-whitespace checks. `git diff --check` exits 0; both untracked record no-index whitespace checks exit 1 for new-file differences with no whitespace diagnostics. Only the three authorized tracked sources plus these two new records form the task delta; .codex/build were already untracked at intake. No staged delta or remaining test container. The audit is repeated on these final record bytes; final five-path and retained-log/XML SHA256s are saved externally in `/tmp/pa9-s2-04/SHA256SUMS`. No local blocker remains; stop at implementation S2 author completion.
