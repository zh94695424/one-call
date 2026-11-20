# PA38 plan fix8 — final validation hardening

## 1. Identity and binding custody

- Task: `pa38-operations-catalog-sale-lifecycle-plan-fix8-gpt-20261118-01`.
- Date: November 18, 2026, Asia/Shanghai.
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa38-next-work-unit/codex-call`.
- Branch: `codex/pa38-next-work-unit`.
- Full resolved HEAD: `49d42b2a154e9a445a34ef0a0801076a6601e208`; tree `5034910465b8b9e97bfeabe80c192f7742670897`.
- Status: **`plan-fix-complete`** at specification level only; stop for targeted final independent A/B review. No implementation, commit, staging, push, PR, merge, approval or Controller acceptance is claimed.

Binding inputs were read completely and verified: Active PA38 Control; original plan `ddd636b6ce1f32c5e6fc68aadaa655d4c145a9df6f203ff83bea3ce7bf165c2b`; fix7 `1dd1008537fb6f6637e6ca243e15c20e2f24d6e4d6b585af8a311d3a28215ef0`; re-review7 A `CA069793D85B34FBFEA6E48B081225DCDBC74D6FE527A80C42E15DD2C132798A`; re-review7 B `E6A6BFEFA9C9A849149B426ADAB69DBF397CE66144CDCC7CB1E1503640DD7CF7`; real source/tests/config; and the `.git` pointer.

| Artifact | SHA256 / result |
| --- | --- |
| Original cumulative plan | `ddd636b6ce1f32c5e6fc68aadaa655d4c145a9df6f203ff83bea3ce7bf165c2b`; 131,722 bytes / 786 lines |
| Final cumulative plan | `337e27f7cbb7a1f31c0acba474fbe375ad1e587b88e34a3b6089c04c137fd8de`; 157,167 bytes / 1,124 lines |
| Fix7 | `1dd1008537fb6f6637e6ca243e15c20e2f24d6e4d6b585af8a311d3a28215ef0`; unchanged |
| Re-review7 A | `ca069793d85b34fbfea6e48b081225dcdbc74d6fe527a80c42e15dd2c132798a`; unchanged |
| Re-review7 B | `e6a6bfefa9c9a849149b426adab69dbf397ce66144cdcc7cb1e1503640dd7cf7`; unchanged |

## 2. RR7 closure

### `PA38-RR7-01` — Docker failure-path capture and Server proof

The standalone preflight uses guarded captures for Maven/JDK, Docker Client, Docker Server and server-backed info:

```bash
if output=$(command 2>&1); then rc=0; else rc=$?; fi
printf '... raw exit=%s\n%s\n' "$rc" "$output"
```

It retains `set -euo pipefail`, captures complete diagnostics before enforcing the canonical zero-exit gate, and requires nonempty Server data plus nonempty `ServerVersion|OperatingSystem|KernelVersion` fields from `docker info --format`. Client `29.1.3` alone cannot pass.

### `PA38-RR7-02` — Git identity, raw pins and changed-path allowlist

Every standalone block requires Controller-supplied, non-defaulted `PA38_EXPECTED_HEAD`, `PA38_EXPECTED_TREE` and `PA38_EXPECTED_BRANCH`, then asserts them against resolved Git. The pointer is read from `.git`, converted with `wslpath -u`, checked with `test -d`, and used through one `PA38_GIT` array. Required values for this task are:

```text
PA38_EXPECTED_HEAD=49d42b2a154e9a445a34ef0a0801076a6601e208
PA38_EXPECTED_TREE=5034910465b8b9e97bfeabe80c192f7742670897
PA38_EXPECTED_BRANCH=codex/pa38-next-work-unit
```

Six existing raw pins remain: OpenAPI, docs12, docs14, docs15, docs17 and PA25. Fix8 adds the deterministic raw-byte aggregate for all 18 migrations:

```text
53453fd3bd0c3aa0e8d073c59f8bc10c490f0348fa89fea75e19fa083dc46a70
```

Serialization is sorted relative POSIX filename + one ASCII space + lowercase raw SHA256 + LF, with a final LF, then UTF-8 SHA256. The aggregate is required before and after implementation; no EOL normalization is applied.

The final implementation changed-path assertion unions tracked changes against `HEAD` and every untracked path (`git ls-files --others -z`, without ignore exclusions), excludes only `.codex`, `.pa*-*`, `target`, `tmp` and `node_modules` components, and compares exactly against the 31 implementation paths plus `docs/gateflow/pa38-operations-catalog-sale-lifecycle-implementation.md`. Missing and extra paths fail. The frozen pathspec diff and raw pins remain independent checks.

### `PA38-RR7-03` — Self-sufficient retry shell

Every standalone preflight/smoke/gate block redeclares `set -euo pipefail`, `JAVA_HOME=/opt/jdk21`, the JDK-prefixed `PATH`, `PA38_MAVEN=(/opt/maven-3.9.9/bin/mvn -B -ntp)`, candidate/validation roots and expected Git variables. It asserts `: "${PA38_MAVEN[0]:?...}"`, root guards and the candidate `.git` pointer. The restore hint prints every binding, including the Maven array. No blank launcher record is accepted.

### `PA38-RR7-04` — Fresh projection refuses merge

Setup creates a fresh `mktemp -d` parent and a nonexistent `candidate` child, asserts the destination is absent, and uses `copytree` without `dirs_exist_ok`. A second setup against an existing root fails loudly. Environment retry reuses only the existing root for preflight, smoke and gates; changed candidate bytes are the only reason for a new root.

### `PA38-RR7-05` — Pin wording

The plan states consistently: six existing raw pins (OpenAPI, docs12/14/15/17 and PA25), plus the new 18-migration aggregate. Counts and meanings are not conflated.

## 3. Validation evidence and actual exits

- `bash -n`: 12 Bash blocks, exit `0`.
- Embedded/standalone Python compile: 8 blocks, exit `0`.
- Stale drive-hardcoded conversion, merge flag, partial restore and pin-count wording searches: exit `0`.
- Six existing raw pins: all exact; exit `0`.
- 18 migrations: exactly V001–V018, no V019; aggregate above; exit `0`.
- Resolved Git status/HEAD/tree/branch and frozen diff: HEAD/tree/branch match the expected variables; diff check and frozen pathspec checks exit `0`.
- Deliberate allowlist tests: exact fixture exit `0`; deliberate untracked extra exit `1`; ignored untracked extra exit `1`; missing required path exit `1`; staged-plus-untracked union exit `0`; explicit private/generated exclusions exit `0`.
- Expected identity mismatch tests: HEAD/tree/branch mismatch each exit `1`.
- Fresh projection setup exit `0`; second setup against existing destination exit `1`.
- Actual host preflight exit `1`: Maven/JDK `0`; Docker Client `1` with client text `29.1.3`; Docker Server `1` with empty Server field and Docker socket permission denial; server-backed info `1`. Smoke and canonical Maven/container gates were not run because the environment stop is binding.
- Mock-only Docker controls: reachable Server/info exit `0`; blank Server/server-data control exit `1`. These are shell tests, not runtime evidence.
- `git diff --check` through resolved Git: exit `0`.

No source, test, POM, CI, OpenAPI, migration, Control, review or prior fix artifact was edited. Only the cumulative plan and this fix8 record are authorized documentation changes.

## 4. Preserved contracts and residuals

Preserved: the two routes and goal; 31 implementation paths = 22 new + 9 modified; 290/180; 97/934; 38/630; 135/1,564; scan 1,565; POM 38; modules/edges 14/13; dependencies 5; manifest 310; six-class/52-method selector; V001–V018/no V019; all RR4/RR5/RR6 and earlier transaction, authority, role, state, audit, Outbox, filter, frozen-input and future-slice boundaries. The implementation report is separately named and does not become a 32nd implementation code path.

Residuals remain environment/ownership items only: Docker daemon access, Testcontainers/JVM self-attach and all runtime gates require a repaired host; Controller must checkpoint accepted bookkeeping before supplying final dispatch identity and append fix4–8/re-review history to Control after targeted A/B acceptance. No design open question or waiver is introduced.

**Final status: `plan-fix-complete`; stop for targeted final independent A/B review.**
