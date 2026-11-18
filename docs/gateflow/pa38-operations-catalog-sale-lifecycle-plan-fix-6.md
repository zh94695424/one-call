# PA38 plan fix6 — accepted re-review5 validation closure

## 1. Identity and binding custody

- Task: `pa38-operations-catalog-sale-lifecycle-plan-fix6-gpt-20261118-01`.
- Date: November 18, 2026, Asia/Shanghai.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`.
- Branch: `codex/pa38-next-work-unit`.
- Resolved full HEAD: `49d42b2a154e9a445a34ef0a0801076a6601e208`; branch/HEAD unchanged.
- Status: **`plan-fix-complete`**, closing accepted RR5-01..03 at plan/specification level only and stopping before final independent A/B re-review. No implementation, executed canonical gate, runtime acceptance, new review verdict or Controller acceptance is claimed.

| Binding artifact | Verified SHA256 / disposition |
| --- | --- |
| Original cumulative plan | `fb58fcc1d398de1211f534fec9640b340c62963ec1b23a8d51c5c7e6b9d47677`; 115,385 bytes |
| Final cumulative plan | `376ca7a2d73eb3bd8b6b71609f9256a1e56f51d5e26a6fa2e58a3ecc03a791ac`; 124,168 bytes |
| Frozen fix5 | `dfb850edc6dcf6d962b35b03e88549f7a72b403db742a8c1c1832aa1c7b228a1`; unchanged |
| Frozen re-review5 A, `docs/reviews/plan-review-20261118-143412.md` | `6787f7a6661f73869e479a7310f98435d7156bce77303c29d6991df0d28ab554`; unchanged |
| Frozen re-review5 B retry, `docs/reviews/plan-review-20261118-152444.md` | `2fbc4da679f997449eb91128e903f00a9120fa0fce53f6fae856dd05f2d7764f`; unchanged |
| Frozen re-review4 A, `docs/reviews/plan-review-20261118-135749.md` | `a9c04ae6ddf2775ef4a6ad95d08a75e578aad712f6681d0fbf5e4a77b9ccaf19`; unchanged |
| Frozen re-review4 B, `docs/reviews/plan-review-20261118-140719.md` | `79470ce113b2d6b16ad32431a9606d4c5955514004dec5c2cdcaea50d5862c13`; unchanged |
| Frozen Control | `67aff86c889738bea0a91b5807f3c5c2833de61048e2853426bd2e3620ed639a`; unchanged |
| Frozen fix4 | `cb8f4ceae417a622b084961748c02addb83e0426fde31665c490e1968df14eae`; unchanged |
| Frozen blocked implementation evidence | `c45b3ad3769aab00f1b3f52f2b83246ba98e00c878d4fef4c0741b886252e16d`; unchanged historical evidence, not rewritten as successful |

Only these two documentation paths are changed by this task:

1. `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan.md`.
2. `docs/gateflow/pa38-operations-catalog-sale-lifecycle-plan-fix-6.md`.

The cumulative plan was already modified relative to HEAD at intake; fix4/fix5 and the four supplied re-review artifacts were already untracked alongside private helpers. Those are preserved pre-existing state, not task additions or permission to edit them. This artifact's final size and SHA256 are returned externally, avoiding a self-referential digest.

## 2. Accepted closure matrix

| Finding | Plan destination | Closure and executable obligation |
| --- | --- | --- |
| `PA38-PLAN-RR5-01` (Medium) | Status; §11.0–§11.3; §12; §13 | Pin the single POSIX Maven/JDK pairing below, keep the LF projection under WSL `/tmp`, require bound-projection launcher/JDK/Docker preflight before source implementation and on candidate regeneration. All canonical Maven commands use the same `PA38_MAVEN` array and same projection. No CRLF-wrapper execution/normalization, Windows interop fallback, primary-checkout canonical gate or unlisted launcher. Failed preflight stops for environment resolution. |
| `PA38-PLAN-RR5-02` (Medium) | §11.0; §12; §13 | Enumerate exactly both raw frozen-document identities below. Raw diagnostics are optional, supplemental and non-binding; only canonical LF-projection runs establish acceptance. Additional raw assertions and any canonical assertions block; environment failures stop/resolve rather than becoming product-failure claims. No waiver or test/fixture/expected-hash edit. |
| `PA38-PLAN-RR5-03` (Low) | §11.3; §12; §13 | Require `clean -Pcontainer-smoke verify`, success-chained shipped CI/XML oracle against that clean sweep's own XML, then deterministic 135-file XML custody and both PA38 Failsafe XML hashes. Never use focused-selector reports for final totals. Generated XML/reports are not committed. |
| Citation precision | §1; §5.1; §12 | Correct OperationsSubjects method span to `:130-134`; Control authorization to `:123-125`. Current renewed dispatch must bind final fix6 plan hash and31 paths, not historical Control `:120`. |
| Projection exclusion | §11.0 | Add general `.pa*-*` private-helper exclusion alongside `.codex` and retained `.pa38-*`, at every depth. No older PA-private helpers enter the candidate-bound projection. |
| Materialization question | §11.0; §12 | Committed PA35 blob is LF; Windows worktree is CRLF. Independent size, ending-count and exact conversion equality evidence below; no unmeasured blob hash is claimed. |
| Source-order precision | §5.1; §13 | Literal source-order assertions constrain only the first `operationsPath.matches`; §9.3's behavioral zero-customer-verifier/object-identity sentinel is the binding guard for the new branch position. |

RR4-01..03 closure and prior cumulative REV/RR/RR2 semantics remain binding. Exact identical Operations/Customer predicates, malformed `publications;x` missing/invalid-credential Operations404 precedence, no downstream/authentication work, all frozen source oracles, REQUEST registrations and unchanged ERROR behavior remain required. No new test identity, method rename or fixture change is introduced by fix6.

## 3. Exact host and preflight contract

The retained Bash shell defines:

```bash
set -euo pipefail
export JAVA_HOME=/opt/jdk21
export PATH="/opt/jdk21/bin:$PATH"
PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)
```

The full §11.0 setup binds the exact stable candidate inventory to one Git-less `/tmp/pa38-lf-validation.XXXXXX` projection, excludes private/generated paths and normalizes **only** the projected PA35 document. Primary/frozen bytes remain unchanged. Before source implementation, run `"${PA38_MAVEN[@]}" -v` from `$PA38_VALIDATION_ROOT/backend`, requiring `Apache Maven 3.9.9`, `Java version: 21.0.13,` and runtime `/opt/jdk21`; require successful `docker info` and `docker version --format '{{.Server.Version}}'` with a nonempty server version. Record launcher array, JAVA_HOME/PATH, projection backend, versions and actual individual exits. Controller independently verified Docker server `29.1.3`; that observation is not proof of access from a child sandbox.

Every §11.1–§11.3 Maven command uses `"${PA38_MAVEN[@]}"` in the same bound projection. The shipped Python CI/XML oracle uses that projection's backend and XML. Candidate changes invalidate previous validation: regenerate, repeat preflight and rerun all gates. A failed launcher/JDK/Docker preflight returns an environment blocker **before source implementation**; never run canonical gates from the primary checkout or substitute an unlisted launcher. Do not execute or normalize frozen CRLF `backend/mvnw`, edit wrapper properties or use a Windows-wrapper fallback. Windows interop is unnecessary.

**Actual fix6 preliminary host check, not canonical projection evidence:** from the unchanged primary `backend`, the pinned Maven `-v` command with the required JAVA_HOME/PATH exited **0**, reporting Maven **3.9.9**, Java **21.0.13**, runtime `/opt/jdk21`. `docker info --format '{{.ServerVersion}}'` and `docker version --format '{{.Server.Version}}'` each exited **1**, reporting permission denied at `unix:///var/run/docker.sock`. No daemon server version was independently observable here. No projection, Maven build/test goal or generated test report was created. The environment blocker is returned to Controller/validation-host owner; no workaround, approval escalation or product failure is claimed. The mandatory bound-projection preflight and all canonical gates remain unexecuted future evidence.

## 4. Both raw materialization identities and blob evidence

Exactly these two expected raw frozen-document failures consume the same PA35 document and use identical LF-terminated removal mutations:

- Surefire: `OperationsIvrSessionReadBoundaryTests.frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact:207`, with removal mutations at `:201` and `:204`.
- Failsafe: `OperationsIvrSessionReadsIT.postgres16UsesExactlyFrozenEighteenMigrations:183`, with removal mutations at `:177` and `:180`.

Both replace `bareLine + "\n"` and `tableLine + "\n"` with empty strings. On raw CRLF text these replacements are no-ops, so `assertThatThrownBy` fails; the single-document LF projection addresses both. A Surefire-only diagnostic exposes only the first identity and does not establish absence of the Failsafe twin. No third expected raw frozen-document failure is authorized.

Independent non-mutating measurement used `Path.read_bytes()` for the worktree and explicit-gitdir `git show HEAD:docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md` for the committed blob:

| Representation | Bytes | CRLF | Bare LF |
| --- | ---: | ---: | ---: |
| Windows worktree raw bytes | 105,116 | 1,344 | 0 |
| Committed Git blob | 103,772 | 0 | 1,344 |

Difference is exactly **1,344 bytes**. `raw.replace(b"\r\n", b"\n") == blob` passed: LF storage and checkout expansion are proven, not inferred from a snapshot alone. No blob hash is claimed without measurement. Neither primary document nor Git configuration was changed; a legitimate future PA35 revision requires explicit re-pinning.

Raw diagnostics are supplemental/non-binding; **only canonical LF-projection runs are acceptance gates**. Any additional raw assertion failure or any canonical assertion failure, including either identity above, blocks. Launcher, Docker, JVM-agent/self-attach, missing dependency or similar environment failure is an environment stop/resolve item, never relabeled as a product failure or accepted as a result. Resolve and rerun before acceptance. No waiver, test/fixture/expected-hash edit, frozen-primary normalization or core.autocrlf change is allowed.

## 5. Clean full gate and XML custody

The canonical final sweep is:

```bash
(cd "$PA38_VALIDATION_ROOT/backend" && "${PA38_MAVEN[@]}" clean -Pcontainer-smoke verify)
```

The actual plan §11.3 executable block success-chains that sweep and the shipped CI Python oracle in the same shell invocation, then emits custody evidence. The oracle runs only after the clean full sweep succeeds, against **that sweep's own** XML. No intervening Maven/selector run, report copy or focused-selector report may supply final totals. `clean` removes previous focused/aborted sweep reports. Expected exact totals remain Surefire **97/934**, Failsafe **38/630**, zero skipped/failures/errors; the shipped source=CI=POM=XML oracle must exit0. These are planned gates, not executed fix6 results.

Exact custody algorithm:

1. Enumerate only `target/surefire-reports/TEST-*.xml` and `target/failsafe-reports/TEST-*.xml` from the final clean sweep, relative to the projection backend; require97 Surefire +38 Failsafe =135 files.
2. Sort their backend-relative POSIX filenames in ordinal order.
3. Compute lowercase SHA256 of each XML's **raw bytes**; do not normalize XML.
4. Serialize every entry as `filename + single ASCII space + lowercase SHA256 + LF`, including the final LF, concatenate in sorted order, UTF-8 encode and compute lowercase SHA256 of that byte sequence.
5. Record the complete filename/hash inventory,135-file count and aggregate, plus both individual PA38 Failsafe filenames/SHA256 values:
   - `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionLifecycleIT.xml`.
   - `target/failsafe-reports/TEST-com.onecall.archtests.container.OperationsCatalogVersionRoleLeastPrivilegeIT.xml`.

Report files remain generated projection output, **not committed**. If sweep, oracle or custody fails, resolve and rerun the clean full sequence. No future XML hash or result is fabricated here.

## 6. Preserved inventories and actual validation

The same two confirmed routes and one PA38-S1 goal remain unchanged. Allowlist **31 =22 new +9 modified**: production **11 new +5 modified**, tests **11 new +2 modified**, build/CI **2 modified**. Final Java **290/180**, Surefire **97/934**, Failsafe **38/630**, focused **135/1,564**, scan **1,565**, POM **38**, modules/edges **14/13**, catalog dependencies **5**, manifest **310** with digest deferred until implementation, resources **21/1**, V001–V018/no-V019 remain binding. New test identities remain **8 Surefire classes/86 methods** and **2 Failsafe classes/38 methods**, plus one zero-test fixture. Existing regression selector remains six classes/**52 methods** =8+8+8+8+12+8. Neither implementation allowlist nor production/test identity sections expand.

Changed plan destinations: identity/status, §1 dispatch citation/hash custody, §5.1 citation/source-order precision, §11 introduction/setup/commands/full-gate custody, §12 residual ownership and §13 hand-back/evidence. Product contracts in §2–§4/§6–§10 and both route predicates are preserved; §9 identities and §10 oracle arithmetic are unchanged.

| Actual command/check | Exit and disposition |
| --- | --- |
| Initial ordinary `git branch --show-current`, `git rev-parse HEAD`, `git status --short` | Each128: Windows `D:/...` gitdir cannot resolve natively. Recovered with read-only explicit gitdir/worktree overrides; no metadata/config edit. |
| Explicit-gitdir branch/full HEAD/status; `sha256sum` binding inputs; source/document searches and reads | 0; branch/HEAD and supplied plan/fix5/re-review5/re-review4 hashes match. |
| Pinned Maven `-v`, JAVA_HOME `/opt/jdk21`, required PATH prefix | 0; Maven3.9.9 / Java21.0.13. Preliminary primary-backend version check only, not canonical validation. |
| `docker info --format '{{.ServerVersion}}'` | 1; Docker socket permission denied, environment blocker returned. |
| `docker version --format '{{.Server.Version}}'` | 1; same environment blocker; actual server version unavailable in this sandbox. |
| Read-only Python worktree/blob byte/count/conversion assertions | 0;105,116/103,772 bytes,1344 CRLF vs1344 LF, exact conversion equality. |
| First generated documentation patch attempt | 1; incompatible unified-diff hunk header, no edit from that attempt. Reissued with supported `@@` headers and succeeded (0); no source or auxiliary file created. |
| Bash `bash -n` / embedded Python `compile` validation | 0; all9 Bash blocks parse and embedded Python compiles without executing build/test/setup/oracle bodies. |
| Python launcher/dual-identity/stale-reference checks | 0; six Maven command lines use the named array (one optional raw diagnostic, five canonical), exactly one clean full sweep; no executable `./mvnw`, Windows command fallback or stale citation. Both full identities occur in §11.0/§12/§13. |
| Explicit-gitdir `git -c core.safecrlf=false diff --check` | 0; no whitespace errors. |
| New fix6 `git diff --no-index --check /dev/null` and Python whitespace checks | No-index exit1 reflects the new file differing from `/dev/null`, with no whitespace diagnostics; Python exit0 verifies both task documents have LF endings, final newline and no trailing whitespace. |
| Final frozen-hash, scope, inventory/identity, syntax, diff and size/SHA256 checks | 0; final values returned in hand-back; no unauthorized path changed. |

Git checks use process-local `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call25`, `GIT_WORK_TREE` equal to the bound worktree and `GIT_OPTIONAL_LOCKS=0`; `-c core.safecrlf=false` only suppresses advisory warnings, not persistent configuration changes. No source/test/POM/CI/OpenAPI/migration/Control/review/fix1–5/implementation artifact/private helper/wrapper/generated output was edited. No staging, commit, push, branch change, PR/issue mutation, merge, approval, readiness mark, reviewer request, external comment, implementation or new review occurred.

## 7. Residual owners and destinations

| Owner | Residual / destination |
| --- | --- |
| Controller | Final independent peer-isolated A/B plan re-review, then separately authorized implementation dispatch bound to final fix6 plan SHA256 and31 paths; preserve RR3-OPS-01..05. Stop before those actions here. |
| Controller + validation-host owner | Resolve child-sandbox Docker socket access before source implementation; repeat exact bound-projection Maven/JDK/Docker preflight. Controller's server29.1.3 observation does not waive child-host checks. |
| Backend test-boundary owner | Execute every canonical LF-projection gate and clean full sweep/own-XML oracle/custody; resolve any JVM-agent/self-attach/dependency issue as environment stop, without fixture/test/hash edits or waivers. |
| Backend Catalog-order + Operations IAM | Implement two commands, unchanged-identity full-chain/sentinel proof, exact role/authority/rollback/fact/concurrency assertions; recompute310-entry production manifest digest only after implementation. Plan closure is not runtime closure. |
| Backend; Platform/Operations + Security | Same-pool bounded read/lock/statement-cancel proof before acceptance; sustained saturation, read503/recovery and capacity at deployment readiness, no new pool/SLO. |
| Platform/Security/Operations | Production LO enablement, secrets, exact seven PA25 SET memberships and pool health at deployment readiness. |
| Database/Security + Catalog-order | Frozen catalog-role over-privilege/effective ACL drift: V00442-tuple matrix and negative probes at implementation; no ACL/migration repair here. |
| Security / Operations IAM | Accepted lock-free authority witness timing and existing verifier availability/sanitization boundary; no employee/admin lock. |
| Platform/Operations | Audit/Outbox retention, dispatch, monitoring and retry remain operational readiness work. |
| API governance / future slices | Draft authoring/replacement, trial policy, customer filtering, UI second confirmation require fresh goal confirmation; no added route/schema behavior. |

**Final status: `plan-fix-complete`; stop before final A/B re-review.** Docker access remains an explicit implementation environment blocker, not an unresolved plan-design finding or a product failure.
