# PA9 V009 S1 code-review fix 1

- Task label: `pa9-v009-s1-codefix-gpt-20261031-01`.
- Work unit: `PA9-v009-sharing-persistence-foundation`; gate: **S1 code-review fix only**.
- Date: October 31, 2026, Asia/Shanghai.
- status: s1-code-fix-complete
- Accepted plan: `docs/gateflow/pa9-v009-sharing-foundation-plan.md`, SHA256 `f314abe1b4bde8032fba787e66e0ee3764033d02db1955530337071403b92c57` (verified canonical LF and current raw bytes).
- Observed unchanged HEAD: `de74c57617a6aa377523e0717d6511457d5682cf`. Author completion is not S1 acceptance, a review pass, remote CI success or production readiness.

## Inputs and controller dispositions

Read both reviews, `docs/reviews/code-review-20261031-144807.md` (A) and `docs/reviews/code-review-20261031-151005.md` (B), the accepted plan, current six-path implementation delta and relevant migration/digest/live-fence/test helpers. The four existing files changed by this fix matched the review candidate's raw SHA256 values at intake. The existing six-path implementation delta is distinct from this fix's five-path incremental delta.

| Finding | Controller disposition | Applied result |
| --- | --- | --- |
| A P1 | Accepted | README overview states current cumulative total 60 and explicitly identifies the three PA9 V009 S1 catalog/ACL/FK additions; PA8's six additions remain correctly attributed. |
| B OBS-3 / plan S1 acceptance pin | Accepted | Rename the existing digest helper through V009, update both existing call sites, and add the final V009 resource/digest pair. Preserve every V001–V008 pin and the existing classpath LF-normalization/hash implementation. |
| B OBS-4, two stale inventory javadocs | Accepted documentation cleanup | V007 describes the exact final V009/no-V010 live fence. V008 describes that same live fence, staged V008 sharing-absence evidence and V001–V009 digest witness. No fence behavior changes. |
| B OBS-2 | Rejected; no action | Existing `assertV009LiveFence` already asserts `catalog_order.fk_customer_order_consent`; no duplicate assertion added. |
| B OBS-5 | Rejected; no action | Dedicated Flyway-chain test already checks V009 description `sharing`; live fence is not widened. |
| B OBS-1 and OBS-6–8 | Residual/optional only | No additional hardening, tests, assertions, DDL or later-slice behavior implemented. |

## Exact changed sections and paths

Only these five repository paths are written by this task:

1. `backend/README.md:3`: replace the stale current 57 count with 60 and explicitly describe the three PA9 S1 additions.
2. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`: V007 inventory javadoc at line 2533; V008 inventory javadoc at line 2616; existing V008/V009 inventory call sites at lines 2629/3047; helper declaration at line 24826; V009 map entry at line 24845. The helper is now `assertV001ThroughV009MigrationDigestsUnchanged`.
3. `docs/gateflow/pa9-v009-sharing-foundation-s1-implementation.md:10`: add fix-specific scope and runtime-evidence qualifications; update the digest-helper description at line 95. Original implementation runtime/source-hash evidence remains historical and both blocked-attempt suffixes remain verbatim.
4. `docs/gateflow/pa9-v009-sharing-foundation-s1-completion-1.md:8`: add fix summary, non-actions and explicit controller rerun/re-review boundary; preserve historical results.
5. `docs/gateflow/pa9-v009-sharing-foundation-s1-code-fix-1.md:1`: new fix artifact with dispositions, scope, hashes, validation and completion status.

No other repository path is written. The existing migration/CI implementation changes, controller bookkeeping, private untracked `.codex` and two review artifacts are intake state, not this fix's writes. The Windows `.git` pointer is not repaired; read-only Git uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call7`, `GIT_WORK_TREE=$PWD`, `GIT_OPTIONAL_LOCKS=0`.

## Updated raw file hashes

SHA256 of final on-disk bytes, not a claim that these revised source bytes were tested:

| Changed existing path | Intake SHA256 | Updated SHA256 |
| --- | --- | --- |
| `backend/README.md` | `47f77226ebbb3f6be1fe991428391e81b02156d3e57c03103c8d6b485bd5ad3c` | `0d1c480f34cdfe01f0995da92a78d0c0134f21b27ec7262faee03f13dd4499e8` |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `b8fa61fd3f3b0eb180ebc52e6a4e67aa392854231a9bf16c2e62d9925bc1f55b` | `4d44085748803275f204e15de2d9ad439d1e9a31b463e626d1c0ba85f9066d07` |
| `docs/gateflow/pa9-v009-sharing-foundation-s1-implementation.md` | `44d3801bea6de9c311406f751173f2a66b6c67a44b5681b545d81586a93edc02` | `e2166db03d29807ac3fd00e24524fb75b6fe88e691bb26d29b17de117c490748` |
| `docs/gateflow/pa9-v009-sharing-foundation-s1-completion-1.md` | `daae34ac3dc73a6d2ca9912d6da8aae27b796f7e8aa2b2c7fde7216cd19d2863` | `377340af7513f7f282be37048f8fab435d6052ae9407ab91c5dc8d6978e73abd` |

This new artifact's own final SHA256 is reported externally in the author response, avoiding a circular embedded self-hash. Prior implementation-run source hashes remain preserved as pre-fix evidence, not replaced with unexecuted source identities.

## Read-only validation results

- Exact incremental Java/README comparison against saved intake copies: **PASS**. Java differs only by the two accepted javadocs, three helper-name occurrences (declaration plus two calls) and V009 map entry. README differs only by the accepted overview replacement.
- Static method inventory: **PASS**, exactly 60 distinct source `@Test` method names, unchanged in identity and order from intake, equal to the unchanged CI expected literal set. No test method is added or renamed. This is not a new Failsafe XML or runtime claim.
- Digest witness static check: **PASS**, nine resource/digest entries; all original eight pins unchanged; each resource's canonical LF bytes match its pin. The `readClasspathBytes` / `toLfBytes` / `sha256Hex` code and the source suffix beginning at `readClasspathBytes` are byte-identical to intake. The existing digest-loop code remains unchanged.
- V009 SQL bytes: **UNCHANGED**. Intake and final raw SHA256, also canonical LF/classpath pin, are `ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae`. `sha256sum --check /tmp/pa9-v009-s1-codefix-01/protected.sha256` passes for all V001–V009 files, accepted plan and CI. CI remains `6603d01f8d9f024fba378173194b172d6e482e261f63db61552690e2ca5800a0`.
- Stale current-count scan of README, CI and both S1 records: **PASS**. README/CI contain no remaining 57 reference; the implementation record's historical `57 MiB` memory observation is not a test-count claim and remains untouched. Immutable plan/historical gate artifacts are not rewritten.
- Scoped tracked whitespace: configured `git diff --check --` with the four changed existing paths exits **0**, with only configured LF-to-CRLF informational warnings and no whitespace errors. New fix-artifact no-index whitespace check returns **1** for its new-file difference, without whitespace-error diagnostics. Independent five-path trailing-space/tab and final-newline scan passes.
- An exploratory tracked check with `-c core.autocrlf=false` returned **2** because it interpreted existing CRLF checkout lines as newly added trailing whitespace. No line-ending cleanup is applied. The configured scoped check and independent CRLF-aware scan above are the authoritative whitespace results. Fix-only Java no-index check with `core.whitespace=cr-at-eol` returns **1** for the difference, with LF-to-CRLF warnings only.
- Scope/provenance: four existing changed paths compared against saved intake; this fifth artifact is the only new fix path. Saved controller diff comparison exits **0**. Protected-file checksum comparison passes; original historical blocked-attempt suffixes in both records remain byte-identical. No controller/plan/design/CI/POM/resource/application edits, migration writes or Git metadata mutation are performed.

## Execution and stop boundary

Maven, Docker, PostgreSQL, focused/full runtime tests and fresh A/B review are **NOT RUN** by this code-fix task. Existing implementation test results are historical pre-fix evidence only. Controller owns focused/full reruns and fresh A/B re-review before deciding S1 acceptance. No S2/S3 entry, commit, stage, push, PR, approval or external comment. Stop after this S1 code fix.
