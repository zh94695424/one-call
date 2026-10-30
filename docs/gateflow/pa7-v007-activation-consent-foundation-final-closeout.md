# PA7 V007 activation and consent foundation — final closeout

## Work unit and delivery

- ID: `PA7-v007-activation-consent-foundation`
- Branch: `codex/pa7-v007-activation-consent-foundation`
- Merged base: `6b7edcd37961d14eacba68d5a38d9773fa9bcff9`
- Draft PR: https://github.com/zh94695424/one-call/pull/15
- Reviewed PR head: `fe8a47e575c2c0e08c0f91aabe8bcf7e3245bf60`
- Accepted PR-review checkpoint: `431edba2239cdff1abd52629e358c771ae259a3b`, pushed and equal to remote branch / PR head.
- Status: `final closeout pass` for database persistence backstops.

V007 creates consent records, one activation head per subject and the immutable transition ledger. The late `catalog_order.customer_order.purchase_consent_id` FK now requires a real consent parent. The migration has exactly three tables / 43 columns, 31 named checks, three PKs, five ordinary unique keys, 12 index objects, 11 immediate validated non-deferrable FKs, six defaults, two SECURITY INVOKER trigger functions and three triggers. Runtime/cross-module grants are exact and PUBLIC access is zero. No SQL service function was introduced.

The cumulative suite evolves from 45 to 51 methods. It preserves the staged V004 orphan-consent witness before later migrations, uses real final-state consent parents, verifies exact catalog/ACL/FK contracts and exercises lifecycle, immutable rows/head identity, rollback and bounded two-connection uniqueness/expected-revision races. CI and backend README match the delivered evidence.

V001–V006 remain unchanged. Frozen V007 SHA256 is `BD919CF367F6713D26C7E6F1F7148E81D275C838E5E307CDE13EBD014E17D659`; final test SHA256 is `D489CDBA6780F4A83FA49D52D14D932BC7B246A81FC7A0B71D0D6F85FB5192C0`.

## Verification and finding status

- Accepted checkpoints: plan `64b576a`, S1 `4aedc78`, S2 `e7eb179`, aggregate `86438ed`, PR review `431edba`.
- Controller final PostgreSQL 16.15 full verify: 51 tests, zero failures/errors/skips; default Surefire 10/0/0/0. Source, CI and XML contain exactly the same 51 unique names.
- Three separately retained focused concurrency runs each pass 1/0/0/0. Final aggregate ACL fix was independently retested with the full suite: 30.257 seconds, completed `2026-10-30T14:04:36+08:00`. Evidence: `backend/target/pa7-aggregate-fix-controller.{log,xml}` and `pa7-s2-concurrency-repeat-{1,2,3}.{log,xml}`.
- Packaged Boot 4.1.2 startup and forbidden-runtime-dependency firewall passed earlier on unchanged production sources/resource bytes. No production datasource/Flyway wiring was added.
- All accepted plan/slice/aggregate findings are final `已修复`. Aggregate operations-IAM latent table/column-grant coverage was corrected and independently re-reviewed. Two final PR reviewers found no material issue; pending-CI observations in their snapshots are superseded by live exact-head checks.
- GitHub CI run `41013426767` passed both jobs on reviewed head `fe8a47e`; run `41014884344` passed `java-runtime` (67 seconds) and `contract-regressions` (34 seconds) on accepted PR-review head `431edba`. PR is Open/Draft/MERGEABLE/CLEAN.
- No accepted finding, blocking question or unclassified risk remains. Runtime evidence was controller-executed; reviewers inspected it read-only and do not claim independent Maven execution.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| Legacy V004 parent-delete exact FK name depends on multiple referencing constraints / trigger order; stale cleanup comment says two rather than three triggers. | BackendQA / next approved test-maintenance work unit. |
| Extra operations-actor negative variants, live runtime head subject-id denial and function-language/dynamic-SQL catalog pins are optional coverage. | BackendQA / future bounded coverage work. |
| NOLOGIN owner-level head DELETE and append-only table TRUNCATE are outside runtime privileges; production owner credentials/membership require discipline. | PlatformSecurity / provisioning rehearsal and Activation service lifecycle policy. |
| Head/ledger atomicity, lock order, narrower service transition policy, consent/prompt currentness and semantic fingerprints are service-owned. | Activation / IVR / Catalog / Product application-service work units. |
| Runtime role authentication/provisioning and Q17/Q23 are not exercised through production connections. | Platform / Database / Security production wiring gates. |
| Bounded races are not production load, chaos or recovery testing; retained target logs/XML are local build artifacts rather than committed binaries. | QA / CI / Platform acceptance and evidence-retention work. |
| IVR/cloud/media/provider integration, Tencent deployment, API/client flows and V008/V009 are not delivered here. | Telephony / Infrastructure / API / client owners; PA8 / PA9 persistence work. |

Historical blocked S1 reports are preserved intentionally; corrected supplements and controller records supply current evidence. S2 report resume revision and check-probe-role comment were corrected. Failed incomplete author attempt, bounded retry, command denials, output-path collision correction and network-proxy recovery are recorded in the controller; none is treated as a passing test.

## Documentation, issue status and next entry point

The plan, implementation/fix/review artifacts, synchronized README/CI and execution controller are delivered with this closeout. This architecture-sensitive work unit is not GitHub-issue-backed: no issue keyword, closeout comment or mutation applies. No merge, approval, mark-ready action, reviewer request, external comment or branch deletion occurred.

After the user merges PR #15, fetch/update target `main` from `origin/main`, create a fresh isolated branch/worktree from that merged base and start Phaseflow goal confirmation for tentative `PA8-v008-evidence-foundation`, bounded to the frozen V008 persistence contract and executable evidence. Confirm its exact goal and dictionaries before planning. Do not start PA8 from this unmerged branch, reuse PA7 scope for application services or interpret this closeout as production readiness.
