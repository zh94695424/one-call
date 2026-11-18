# PA38 plan fix7 — accepted re-review6 evidence closure

## 1. Identity and binding custody

- Task: `pa38-operations-catalog-sale-lifecycle-plan-fix7-gpt-20261118-02`.
- Date: November 18, 2026, Asia/Shanghai.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`.
- Branch: `codex/pa38-next-work-unit`.
- Resolved full HEAD: `49d42b2a154e9a445a34ef0a0801076a6601e208`; tree `5034910465b8b9e97bfeabe80c192f7742670897`. Both remain unchanged.
- Status: **`plan-fix-complete`** at specification level only; stop before final independent A/B re-review. No implementation, new review verdict, canonical runtime acceptance or Controller acceptance is claimed.
- The prior fix7 `...-01` attempt was Controller-blocked before editing because the two re-review6 artifacts were absent. It is neither a review nor an edit input. Both imported re-review6 artifacts now match their exact bindings.

| Binding artifact | Verified SHA256 / disposition |
| --- | --- |
| Original cumulative plan | `376ca7a2d73eb3bd8b6b71609f9256a1e56f51d5e26a6fa2e58a3ecc03a791ac`; 124,168 bytes |
| Final cumulative plan | `ddd636b6ce1f32c5e6fc68aadaa655d4c145a9df6f203ff83bea3ce7bf165c2b`; 131,722 bytes / 786 physical lines |
| Frozen fix6 | `e560cff0b5ef9076d79cd94cea7d2e07d475d2709a398a3405be22f7424449d4`; unchanged |
| Re-review6 A, `docs/reviews/plan-review-20261118-155523.md` | `17d0ea81e21be90ab7ace5d7fd61c95c1335038c5cfbc7fd8f4091fecaa44f8e`; unchanged |
| Re-review6 B, `docs/reviews/plan-review-20261118-160756.md` | `5e02fe9c0b7755b39ee4b8e7fb62583bb8256cb541734e29212add74881f3f98`; unchanged |
| Frozen Control | `67aff86c889738bea0a91b5807f3c5c2833de61048e2853426bd2e3620ed639a`; unchanged |
| Frozen blocked implementation artifact | `c45b3ad3769aab00f1b3f52f2b83246ba98e00c878d4fef4c0741b886252e16d`; unchanged historical blocker |

The active Control checkpoint was read through its PA38 stop at bounded fix4; later bookkeeping is Controller-owned. After final independent A/B acceptance, Controller must append the fix4–7/re-review history and final plan hash before renewed implementation dispatch. Fix7 does not edit Control or infer later acceptance from its historical dispatch.

## 2. Accepted closure matrix

| Accepted finding | Plan destinations | Closure / executable obligation |
| --- | --- | --- |
| `PA38-PLAN-RR6-01` — A Medium | §2.1, §11.4, §13 | Derive the exact real gitdir from the Windows `.git` pointer using the accepted `sed`/`tr` recipe; assert its directory and use the process-local `PA38_GIT` array for every static Git invocation. Unresolved ordinary Git is an environment stop, never a skip. Require both five pinned before/after raw-byte hashes measured before implementation and the exact resolved-Git frozen diff exiting 0. Keep the existing OpenAPI hash; no metadata/config edit. |
| `PA38-PLAN-RR6-02` — B F1 | §11.0, §12, §13 | Record Client version, Server version and `docker info` separately with each raw exit, plus a server-specific observation. Require success and a nonempty Server field; explicitly attribute the server version to that field, never a client banner. Client-only `29.1.3` cannot pass. |
| `PA38-PLAN-RR6-03` — B F2 plus A open-question clarification | §11.0, §12, §13 | Separate creation/materialization from standalone preflight. Record/export one exact root and its candidate inventory. Environment-only retries repair host access and rerun preflight/smoke against that same root, without new `mktemp` or weakened `set -euo pipefail`. Only changed candidate bytes permit a new root; record why both exist, both inventories and the single final evidence root. Add the existing-class JVM-agent/self-attach/build smoke after successful launcher/Docker preflight and before implementation. Smoke failure stops for environment resolution, not test weakening or acceptance credit. |
| `PA38-PLAN-RR6-04` — B F3 | Cumulative prose, especially §5.1, §9.3, §11, §12, §13 | Repair missing spaces adjacent to digits, including every listed line/POM/regression/Surefire/guard/Failsafe/full-total/Operations/auth/provider/mutation/command token family. Preserve numbers and semantics. Do not rewrite historical fix6. |

Historical frozen fix6 token `V00442-tuple` means **V004 42-tuple**, not a migration named V00442. Controller must carry this clarification into acceptance bookkeeping without editing fix6. The live plan retains the correct V004 42-tuple meaning.

## 3. Git environment and frozen-byte evidence

The actual `.git` file is `gitdir: D:/codex-call/.git/worktrees/codex-call25`. The exact accepted derivation resolves `PA38_GITDIR=/mnt/d/codex-call/.git/worktrees/codex-call25`; `test -d` exited **0**. All Git operations in this task used `PA38_GIT=(env GIT_DIR="$PA38_GITDIR" GIT_WORK_TREE="$PA38_CANDIDATE_ROOT" GIT_OPTIONAL_LOCKS=0 git -c core.safecrlf=false)`; neither `.git` nor repository config was changed. No ordinary unresolved Git command was used as a substitute or silently skipped.

Resolved status, branch, full HEAD, tree, frozen diff and whitespace gate each exited **0**. The final plan's exact §11.4 Bash block was also extracted and executed against the primary candidate: **exit 0**, all six pinned hash checks OK, exactly 18 migrations and no V019, exact frozen diff against `HEAD` empty. The `HEAD` comparison covers staged and unstaged tracked changes; status and the independent full file inventory additionally cover pre-existing/untracked paths.

These five raw-byte SHA256 values were captured before fix7 editing and before implementation, rechecked after edits, and pinned in §2.1/§11.4 for mandatory before-implementation and after-gates equality. Both hash equality and resolved-Git frozen diff are required, not alternatives:

| Frozen path | Before = after SHA256 |
| --- | --- |
| `docs/12-technology-stack-and-system-architecture.md` | `9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db` |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | `0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31` |
| `docs/15-production-openapi-surface-spec.md` | `db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0` |
| `docs/17-production-security-design-spec.md` | `b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205` |
| `backend/ops/postgresql/pa25-runtime-identities.sql` | `b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9` |

OpenAPI remains `0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68`. Every migration, source/test/POM/CI file, review/fix1–6 artifact, Control, private helper, `.git` pointer and pre-existing generated file remains byte-identical to task entry. A pre-edit SHA256 inventory captured **3,356 files**, including private/generated files; final comparison permits only the cumulative plan modification and the new fix7 artifact. Initial Git status already contained the cumulative plan modification and untracked historical artifacts/private helpers; those are not new fix7 edits or authorization to remove them.

## 4. Actual launcher and Docker diagnostics

These were separate non-mutating preliminary host diagnostics from the unchanged primary backend/host, **not bound-projection preflight or acceptance evidence**. Individual exits were printed by separate commands, not by weakening a canonical fail-fast shell.

| Actual command | Raw exit | Observation |
| --- | ---: | --- |
| `JAVA_HOME=/opt/jdk21`, required PATH prefix, `/opt/maven-3.9.9/bin/mvn -B -ntp -v` | 0 | Apache Maven 3.9.9; Java version 21.0.13; runtime `/opt/jdk21` |
| `/opt/jdk21/bin/java -version` | 0 | Temurin/OpenJDK 21.0.13+9-LTS |
| `docker version --format '{{.Client.Version}}'` | 1 | Prints Client `29.1.3`, then Docker API socket permission denied; a visible client version does not make this command successful |
| `docker version --format '{{.Server.Version}}'` | 1 | Empty Server value; permission denied at `unix:///var/run/docker.sock` |
| `docker info` | 1 | Client banner reports 29.1.3; Server section reports socket permission denied, not reachable server data |

**No server version was obtained from the Server field.** Client `29.1.3` is not server evidence. The daemon/Testcontainers environment blocker remains with Controller + validation-host owner. No permission escalation, socket/config workaround, Maven build/test goal, container, validation projection or new XML/report was created in fix7. The bounded smoke and all canonical gates were not run, rather than claimed passed or skipped for acceptance.

## 5. Single-root retry and smoke custody

The plan now has distinct setup and host-preflight Bash blocks. Setup records/exports the exact root and prints the candidate inventory SHA256. Environment repair retries only the standalone preflight and subsequent smoke on that same root. A replacement root is permitted only for changed candidate bytes, with both roots/inventories, reason, retired-root disposition and final evidence root recorded. Never relax `set -euo pipefail` to gather diagnostics; collect individual diagnostic exits before the canonical gate sequence or by separate commands against the same root. Final preflight, all canonical gates, shipped CI oracle and XML custody name one final root and candidate inventory.

After launcher/JDK/Docker preflight succeeds, the required bounded existing-class smoke is exactly:

```bash
(cd "$PA38_VALIDATION_ROOT/backend" && \
  "${PA38_MAVEN[@]}" -Dtest=OperationsSubjectReadConfigurationTests test)
```

This checks JVM agent/self-attach and build execution **before implementation**, not PA38 acceptance totals. Failure is an environment stop; tests are never weakened. New candidate bytes invalidate prior acceptance and require renewed setup/preflight/smoke and all canonical gates. RR4/RR5's two raw CRLF identities, one-document LF normalization, six-class/52-method regression selector and clean full-sweep/own-XML oracle/135-file custody remain intact; no focused-selector report supplies final totals.

## 6. Validation and preserved contracts

| Actual check | Exit / result |
| --- | --- |
| All supplied binding SHA256 checks, full HEAD/branch/tree and pre-edit frozen hashes | 0; exact matches |
| Derive/test `PA38_GITDIR`; resolved-Git status/HEAD/tree/frozen diff | 0; exact branch, HEAD and tree above |
| Execute the plan's exact static §11.4 block, including hash/migration/frozen diff checks | 0; all checks pass |
| `bash -n` on all 11 plan Bash blocks; compile all four standalone/embedded Python blocks without executing their bodies | 0; syntax valid |
| Listed stale-token search and additional prose-token scan | 0; no stale missing-space tokens; identifiers, hashes and numbers preserved |
| Byte comparison of unchanged ownership/allowlist/state/fact/CI/manifest sections, allowing only specified prose spacing | 0; no predicate, identity, count or manifest algorithm drift |
| Resolved-Git `diff --check`, both documents' UTF-8/LF/final-newline/trailing-whitespace checks | 0; no whitespace errors |
| Final task-entry inventory comparison and binding/frozen rechecks | 0; only the two authorized documents differ |
| Maven/JDK and all three Docker commands | Actual exits in §4; Docker environment stop explicitly returned |

Preserved: same two routes/goal; **31 paths = 22 new + 9 modified**; production/test Java **290/180**; Surefire **97/934**; Failsafe **38/630**; combined **135/1,564**; source scan **1,565**; POM **38**; modules/edges **14/13**; catalog dependencies **5**; production manifest **310** with digest recomputed only after implementation; six-class/**52-method** selector; resources **21/1**; V001–V018/no V019; every RR4/RR5 and prior runtime/role/transaction/state/audit/Outbox/filter/frozen-boundary contract.

## 7. Residual owners and stop

- **Controller:** final independent A/B re-review and acceptance; then append fix4–7/re-review history, final hash and V004 clarification to Control and bind any separately authorized implementation dispatch to the final plan and 31 paths. None occurs here.
- **Controller + validation-host owner:** repair Docker server access; repeat the recorded-root preflight and JVM-agent/build smoke before implementation. Current diagnostics do not establish daemon or agent reachability.
- **Backend test-boundary owner:** execute all canonical gates, clean full sweep, own-XML CI oracle and 135-file custody on one final root/inventory; environment failures stop, assertions block, no waiver or fixture/hash weakening.
- **Backend Catalog-order + Operations IAM:** implement the two commands and all unchanged full-chain/sentinel/authority/rollback/fact/concurrency assertions; recompute manifest digest only after production bytes exist. Plan closure is not runtime closure.
- **Backend + Platform/Operations/Security:** bounded same-pool read/lock/statement-cancel evidence at implementation; sustained saturation/read 503/recovery and capacity at deployment readiness, with no second pool or new SLO.
- **Platform/Security/Operations, Database/Security + Catalog-order, Security and API governance:** retain existing owners for production LO secrets/enablement/seven memberships/pool health, frozen-role over-privilege and ACL probes, lock-free authority timing/verifier availability, Audit/Outbox operations and all future draft/trial/customer/UI slices; no new schema, grant or policy here.

Only the cumulative plan and this fix7 record are changed. No staging, commit, push, PR/issue mutation, merge, approval, readiness mark, reviewer request, external comment, implementation or new review occurred. Fix7's own final SHA256/size are returned in the hand-back rather than recursively embedded in this file.

**Final status: `plan-fix-complete`; stop before final independent A/B re-review.**
