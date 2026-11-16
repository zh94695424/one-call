# PA36 plan fix2 — bounded re-review finding closure

Task: `pa36-evidence-role-ivr-acl-plan-fix2-gpt-20261116-01`
Date: 2026-11-16 (Asia/Shanghai)
Status: planning correction and Controller handoff only; **not a passed gate or implementation authorization**.

## 1. Authority and output boundary

Read Active PA36 Control in `docs/gateflow/project-execution-control.md`, the cumulative plan, fix1, and independent re-review artifacts `docs/reviews/plan-review-20261116-080510.md` (A3, fail) and `docs/reviews/plan-review-20261116-111954.md` (B3, pass-with-risks). Controller accepted four deduplicated findings `PA36-PLAN-REREVIEW-01` through `04` and selected full capability-boundary13 iteration, not the reviewers' alternative monitored-only iteration. Review conclusions are inputs, not authorization to implement.

Fix2 entry HEAD: `17e1dd9ee1aaf99f36573596587be19136937441`. Fix1 entry lineage in the cumulative plan remains historical; no repository pointer/configuration was changed. Read-only Git uses the existing WSL-compatible explicit `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call22 --work-tree="$PWD"`.

Only these two repository paths are task output:
- Update `docs/gateflow/pa36-evidence-role-ivr-association-acl-plan.md`.
- Add `docs/gateflow/pa36-evidence-role-ivr-association-acl-plan-fix-2.md` (this artifact).

No source/test/migration/docs17/runbook/README/CI/POM/Control/prior-plan/review edit, staging, commit, branch, push, PR, merge, approve, mark-ready, reviewer request, external comment, issue mutation or implementation action is authorized or performed. Private `.codex` is not task output. All plan test/migration/verifier instructions describe later implementation obligations, not actions executed here.

## 2. Accepted-finding dispositions

| Controller finding | Review mapping | Cumulative-plan closure |
| --- | --- | --- |
| `PA36-PLAN-REREVIEW-01` | A-01 High + B3-01 Medium | §§6 row37, 7.3 and 7.3.1 explicitly name `DatabaseRuntimeAclRbacHardeningIT.assertFrozen()` as the third current tail. Select unfiltered directory `hasSize(17)`→exact18, independently pin exact old17 paths union sole V018 in the new boundary test. Preserve thirteen V001–V013 LF pins, historical target013/applied13 and V014 migration/negative semantics. §6 expressly allows this edit; the sweep rule requires a version-bounded filter before calling a directory assertion historical. |
| `PA36-PLAN-REREVIEW-02` | A-02 Medium + B3-02 Low | §§6 row7, 7.3 and 7.3.1 enumerate full capability boundary13, 91 pairs, allowed14/rejected77, both SET forms, separate extra-target denials and ten NOLOGIN password-negative probes replacing `EFFECTIVE.subList(0,4)`. Catalog role count35 is not the old rejected35 or a suite55 oracle; PA25 method count remains20. |
| `PA36-PLAN-REREVIEW-03` | A-03 Medium + B3-03 Low | §5 names actual `core/evidence/internal/operationsread/JooqOperationsEvidenceCounter.java` via `OperationsReadContext.Role.EVIDENCE`. §7.1 requires verified consumer names/paths in current docs17 and resolution of every named production type under `backend/src/main/java`; no nonexistent reader remains in the cumulative plan. |
| `PA36-PLAN-REREVIEW-04` | A-04 Low | §§6 row37, 7.3 and 7.3.1 correct V014 independent oracle28 = customer12+evidence16, old81/final28; 247/261 belong to `CustomerBusinessNumberReadPrivilegeIT`. Historical docs17 maps are21 for activation boundary/IT and22 for lifecycle (includes docs12), each retaining17 migration rows. Source literals remain untouched. |

These are author plan dispositions, not independent reviewer sign-off. No finding is deferred to an implementer's design choice.

### Exact graph and probe derivation

- Keep seven logins, nine exact-ACL-monitored roles, four separately validated shared-context targets, thirteen capability roles, thirteen managed names and seventeen full boundary names. Do not put context-target ACL inventories into PA25's410 oracle or put external logins into the inner capability target loop.
- `deniesEveryUnapprovedRoleTargetIncludingOwnersAndPredefinedRoles` iterates LOGINS7 × (EFFECTIVE9 union context4) =91. Allowed = LQ1 + LC1 + LW2 + LO7 + direct self3 =14; rejected =91−14 =77. Counters advance once per pair after both SET forms, not twice:182 command-form probes. Retain separate owner/predefined/other-login denials outside these totals. The alternative10/53 on only nine monitored targets is expressly not selected.
- Catalog project roles35 =31 migration-defined +4 external logins (historical34 =31+3). This graph35 and the old rejected-pair literal35 are unrelated; neither is a suite/test count or `suite55` acceptance target. Keep PA25's20 identities, new suites8+16 and full109 classes/1232 methods separately defined.
- Negative password targets are capability13 minus direct LOGIN3 = ten NOLOGIN roles: sharing, customer IAM, sharing command, evidence, Operations IAM, IVR, subject number, activation consent, entitlement metering and catalog order. Set synthetic nonempty password on each, reject both apply/check, restore PASSWORD NULL in `finally`; preserve valid direct-login passwords, seven wrong-password TCP denials and session cleanup. This extends existing methods only.
- Both live frozen-directory callers remain required: `v014FreshChainHasExactHistoryAndImmutablePredecessors` and `v014IsPrivilegeOnlyAndLeavesUnrelatedBoundariesEquivalent`. Do not append V018 to the historical V001–V013 `FROZEN` map or reinterpret its keys as a directory filter.

### Preserved scope and arithmetic

| Inventory | Entry | Planned final |
| --- | --- | --- |
| S1 implementation paths |43 =40 existing +3 future-new|unchanged;36 test/helper +7 non-test paths|
| Shared Operations pool / graph |existing shared pool; Controller-selected topology|seven LO SET edges,11 total, no new pool/role hierarchy|
| Surefire classes / methods |76 /704|77 /712 (+1 /8)|
| Failsafe classes / methods |31 /504|32 /520 (+1 /16)|
| All classes / methods |107 /1208|109 /1232 (+2 /24)|
| Exact Failsafe includes |31|32|
| Existing affected classes / methods |31 /443 =8 /48 +23 /395|same existing identities|
| Owned compatibility focus |existing31 /443|33 /467 =9 /56 +24 /411|
| Annotation-line scan |1209, including one frozen comment|1233 with the same comment|
| PA25 independent arrays |ACL288 / history17 / owners12 / routines43|ACL410 / history18 / owners12 / routines43|
| ACL accounting |288 + Operations14 + IVR106 =408|410 =408+2; original-seven290; evidence16→18|
| V014 frozen ACL arrays |old81 / final28|unchanged; final customer12 + evidence16|
| Historical docs17 maps |activation boundary21 / activation IT21 / lifecycle22|unchanged; each contains17 migration entries|

Option A, the two owner-issued non-grantable column SELECTs, no broad SELECT/write/schema/API/application change, V001–V017 byte freeze, single atomic S1 and paused PA35 all remain unchanged. §7.4 historical CI start/subtraction/merge arithmetic and §8 focused/full commands are unchanged.

Canonical identity manifest format: sorted fully qualified `class#method\n`, UTF-8/LF:
- Existing107 /1208 SHA256: `89262b28603810de7858f39f2bcedf4e598137fc6cf54ecdb8c6249f8dc67843`.
- Planned109 /1232 SHA256: `a082db1f77d93e9c76ed20500ca52c9e5c8b96fdc97b7140f0fa7d1972f58b23`.

## 3. Recomputed hashes and protected inputs

Cumulative-plan entry:76,298 bytes /478 lines / SHA256 `cad87e0e558a0db04678cfcb0ce547f085d31fb082fbe0afcf4f1ddae046ca21`.

Cumulative-plan fix2:81,620 bytes /482 lines / SHA256 `bd50534fa7eef6e7288d6e5c1854f5c67499697eef155a1c8e2d29567de6555c`. This artifact does not embed its own circular hash; its final digest is reported at handoff.

Selected protected input raw SHA256 values, identical to entry:

| Path | Before = after SHA256 |
| --- | --- |
| `docs/gateflow/project-execution-control.md` | `0e15257a2554581941c9db860cc49b17812a0b46d898b8391a4f733927177e5a` |
| `docs/gateflow/pa36-evidence-role-ivr-association-acl-plan-fix-1.md` | `d7cef4d2af342f262b0f3870ca37a185bd9b9dbedbf26ebd3e32b844abb3eb67` |
| `docs/reviews/plan-review-20261116-080510.md` | `5b969f1bb3a4321c4341d95fdf86dec5f0e69f52fd8dad5c04bd6d3aa2eb08a4` |
| `docs/reviews/plan-review-20261116-111954.md` | `bdab7acff1a3b6111228cc38b17fa527db707a2519d628e36e1e4c47abbeaceb` |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | `e3d739e22d1c49dfca9b0a4dfa717c384adfc0b15f097f16795d1072253efdd8` |
| `docs/17-production-security-design-spec.md` | `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` |
| `.github/workflows/ci.yml` | `2c4addc3a09c2418869c6252e06be9257011d0fc34751289db6c15292887faf0` |
| `backend/pom.xml` | `6a0e01aa11959149a55cd731118809d9efc9231ee11fd645292d490a2d9ea19a` |

All seventeen migration raw SHA256 values still match cumulative-plan §2. Independently recomputed LF digests and signed Flyway per-line CRC32 values match the existing hash/history pins, including all thirteen V001–V013 LF pins in `DatabaseRuntimeAclRbacHardeningIT`. V017 remains checksum−41822162; docs17 raw/LF pins remain `7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e` / `8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f`. V018 does not exist yet, so no candidate checksum/digest is invented.

## 4. Static validation and limits

Read-only/static validation uses existing repository formats; temporary snapshot/report files are under `/tmp`, outside the repository:

1. Reconstructed executable CI's inventory-only dictionary/merge construction (no CI command or report validation executed), compared every source method identity, checked76/704 +31/504, parsed exact POM includes31 and recomputed both identity digests above. Projecting only the listed8+16 new methods yields109/1232 and32 includes; it does not create source methods or XML evidence.
2. Recounted sequential unique implementation rows1–43 (40 existing/3 new), all31 existing affected class method counts (48+395=443), compatibility totals33/467, and exact8/16 planned new identity lists. No existing identity changes.
3. Independently enumerated the selected seven-login/thirteen-target graph, proving91/14/77 and182 SET forms, eleven edges, managed13/full boundary17 and ten NOLOGIN password-negative targets. These are static model checks, not PostgreSQL execution.
4. Parsed actual PA25 frozen/history/owner/routine JSON arrays (288/17/12/43), V014 old/final arrays81/28, and all three docs17 maps21/22/21 with17 migration rows each. Recomputed every V001–V017 raw/LF digest and signed line CRC32; verified actual13 LF pins. No normalization or source edit occurred.
5. Resolved declared production types for `DefaultCustomerShareCommandService`, `JooqSharingCommandRepository`, `ShareableEvidenceProjectionReader`, `OperationsReadContext` and `JooqOperationsEvidenceCounter`; verified the actual evidence counter calls `context.scoped(Role.EVIDENCE, ...)`. Cumulative-plan amplification inventory no longer names the nonexistent reader.
6. Compared all2,113 protected entry-file hashes (all entry files except the cumulative plan), exact new-path set and final read-only Git diff scope. Only the cumulative plan and this new artifact are outputs. `git diff --check` passes; no staged path or repository plumbing edit.

Not executed: Maven/JUnit/Failsafe, Docker/PostgreSQL, Flyway migration/replay, PA25 apply/check, hosted CI, production TCP/TLS/credentials/rotation/session drain or deployment. No test XML or runtime success is claimed. Future implementation still requires PostgreSQL16 focused/full validation and109/1232 zero-failure/error/skip XML evidence.

**Handoff:** stop at these two planning files for fresh independent peer-isolated A/B re-review. Controller owns any subsequent dispatch, authorization and PA35 resumption. This task does not trigger re-review, request reviewers or perform implementation/remote-state actions.
