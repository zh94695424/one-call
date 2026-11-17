# PA35 PR #47 review fix 1

## Status and authority

- Date: 2026-11-17. Author task: `pa35-pr47-review-fix1-gpt-20261117-01`.
- This is the bounded documentation/bookkeeping correction after accepted dual PR review, not a new design or implementation gate. Author corrections are pending Controller intake and fresh independent A/B re-review; no re-review or final acceptance is claimed.
- Binding inputs: `docs/gateflow/project-execution-control.md`, `docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md`, `docs/gateflow/pa35-operations-ivr-session-read-foundation-implementation.md`, and the two unchanged review artifacts below.
- Exact write set: modify `docs/gateflow/project-execution-control.md` and add this one artifact, `docs/gateflow/pa35-pr47-review-fix-1.md`. No production, test, POM, CI, migration, OpenAPI, design, plan, implementation-report, existing-review or frozen-contract byte is changed. V001-V018 and accepted code remain unchanged.

## Reviewed freeze and artifact custody

These are the reviewed frozen facts, not a fresh GitHub query or the eventual fixed checkpoint:

| Identity | Frozen value |
|---|---|
| Base | `9a4fbaddb0fb8f619559734cc4a5e048a44096c1` |
| Reviewed head | `085c34dfbcf75b8abe97bf8f7027c73c524a7afe` |
| Diff SHA256 | `1e8b8014c3de8cbbd7b2a703e32191a6e3f67a16ce0fc76004944e4825f7ab26` |
| Diff scope | 61 paths |
| PR metadata | `OPEN`, Draft, `mergeable=MERGEABLE`, `mergeStateStatus=UNSTABLE`, zero review requests, no checks reported at freeze |
| Reviewer A artifact | `docs/reviews/pr-47-review-20261117-144215.md` |
| Reviewer A SHA256 | `86fddfa7f13238f5026bdd377ece8453d50789c0b480e51c3a0018a96842a051` |
| Reviewer B artifact | `docs/reviews/pr-47-review-20261117-145325.md` |
| Reviewer B SHA256 | `512777042075026ccdd6154af1a2d70015caae4b7c025b4e9508d17b1d5faadd` |

Both artifact hashes were recomputed from the supplied workspace bytes and match the accepted inputs. `UNSTABLE` is not evidence of a failed test or a CI pass. Control now attributes `MERGEABLE/CLEAN` only to the earlier `a8a15866bd056268f7fa315753ec4592f3c16483` / `08ed132f5434f449402ff9c201cfdbbbc88d28dc` checkpoint history; it does not describe the reviewed head that way.

### Runners, structural validation and isolation

- A: Claude Code / DeepSeek; retained runner task/output label `review-a`, run directory `/tmp/sub-agents.pa35-pr47-a.EANNVs`, completion `review-a.json`. The completion is valid success JSON, `is_error=false`, 154 turns, with sandbox permission denials. A lacked Bash execution; that limitation does not invalidate its completed artifact. The A artifact does not stamp a separate task ID, so this record uses the verifiable runner task label rather than inventing one.
- B: `codex-agent-run` / Qwen3.8 Flash (`qwen38flash`); runner output label `review-b`, run directory `/tmp/sub-agents.pa35-pr47-b.iiHXTW`; task `pa35-pr47-review-b-qwen38flash-20261117-01`. The output has 356 nonempty valid JSONL events, exactly one `turn.completed`, zero failed/error events and a valid final artifact. B recovered one pre-model `unsupported call: cmd` stderr diagnostic; this is separate from the successful model event stream and does not invalidate the artifact.
- Local runner JSON/JSONL parsing independently confirmed these structural results. The sandbox denials and recovered diagnostic are evidence-custody qualifications, not new source findings or reasons to discard either review.
- The accepted custody record states A/B snapshots were peer-isolated and contained 2,341 baseline files each after excluding the submitted artifact and B's runner-created `.codex` marker. Both independently rehashed the same 61-path diff, metadata and body. No peer snapshot or private evidence is modified by this fix.
- `.pa35-review/evidence/static.log` was an earlier superseded static result. `final-static.log` / `static-results.json` are the current static evidence. This clarification does not change any checker, evidence file or reported runtime result.

## Accepted finding dispositions

All three accepted findings are Low and nonblocking. Their bookkeeping corrections do not broaden PA35.

| Accepted ID / source references | Author disposition | Remaining gate / owner |
|---|---|---|
| `PA35-PR47-RR1-01`; A `F-PR47-01`, B `F-B-01` (deduplicated) | Author-corrected in Control: exact reviewed base/head/diff/scope and metadata recorded; CLEAN retained only as earlier checkpoint history | Controller intake and fresh A/B re-review |
| `PA35-PR47-RR1-B-02`; B `F-B-02` | Exact replacement validation bullet prepared below; native results and external LF-only success explicitly separated | Controller must apply the PR-body edit, then fresh A/B re-review; remote finding closure is not yet claimed |
| `PA35-PR47-RR1-B-03`; B `F-B-03` | Author-corrected in the existing `PA35-S1-RR-B-01` / `F-RB-01` registry rationale by naming the surviving generic guard | Fresh A/B re-review; underlying residual stays `deferred-with-owner`, future test-hardening/Backend |

The surviving generic guard is `EvidenceRoleIvrAssociationAclIT#operationsLoginSetsExactlySevenRolesAndRestoresSessionContext`. It iterates `OperationsReadContext.Role.values()`, exercises the IVR arm and verifies restoration to the Operations role, same backend/session context, read-only and repeatable-read context. It reduces the risk but does not replace the deferred per-file fixture-provider guard. The original deferred disposition, owner and future test-hardening destination remain intact; no test change or new implementation requirement follows.

## Exact replacement PR-body text

Controller only: replace the existing validation bullet beginning `JAR smoke/dependency firewall and A1-A5 validators passed` with the following exact Markdown bullet, preserving the rest of the PR body. This author does not call `gh`, edit the remote body or claim that the replacement has already been applied.

```markdown
- JAR smoke/dependency firewall passed. Native-checkout A1-A5 exit results were A1=0, A2=1, A3=0, A4=0, A5s1=2, A5s2=2; all six A-stage commands passed only in the disposable external LF projection (A1=0, A2=0, A3=0, A4=0, A5s1=0, A5s2=0). The native A2/A5 failures reflect pre-existing CRLF-sensitive A0/A5 checker behavior; no frozen input or checker source was changed.
```

This matches the implementation report's native-checkout and `/tmp/pa35-lf-validation` results: A2's existing CRLF-sensitive A0 parser reported missing CHECK/column rows; A5 reported `state table header changed in section 7.1`. No failure was repaired by changing a frozen input or checker source. LF projection success is not native-checkout success, hosted-CI proof or production readiness.

## Validation and scope proof

- Recomputed A/B artifact SHA256 values match the full values above. Accepted finding references are reconciled across Control and this artifact, including deduplicated A/B IDs and the preserved `PA35-S1-RR-B-01` / `F-RB-01` residual.
- A pre-edit SHA256 manifest captured all 3,428 starting workspace files, including already-present accepted review artifacts, `.codex`, the supplied prompt, generated files and targets. Post-edit comparison passed: 3,429 files, exactly one Control modification and this one added artifact, zero removals and no other byte change. The pre-existing untracked files are not part of this fix.
- The workspace's Git worktree pointer uses a Windows `D:/...` location. Local validation uses `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call23` and `GIT_WORK_TREE` set to this workspace, without editing `.git`, staging or changing repository configuration. Starting HEAD is the reviewed `085c34dfbcf75b8abe97bf8f7027c73c524a7afe`; starting tracked diff is empty.
- `git diff --check` passed (exit 0); its 60 existing LF-to-CRLF conversion warnings are advisory, not whitespace failures or file edits. Exact tracked/untracked path inspection and the starting-manifest comparison passed. Both review hashes match; all nine accepted/deduplicated/residual finding references occur in both records; exact frozen identities, native exits and the generic guard match. Control has no unqualified reviewed-head CLEAN claim.
- The new artifact's strict UTF-8/LF check passed: no BOM, U+FFFD, conflict marker or trailing whitespace, and a final LF. Control retains its original CRLF representation. The new artifact is also checked separately with `git diff --no-index --check -- /dev/null docs/gateflow/pa35-pr47-review-fix-1.md` because it remains untracked.
- No runtime suite or A-stage validator is rerun for this documentation-only fix; earlier runtime evidence is neither replaced nor upgraded by these checks.

## Controller handoff and residual risks

Next entry: Controller applies the exact PR-body replacement via `gh pr edit`, imports/accepts the bounded fix and pushes the fixed checkpoint, then dispatches fresh peer-isolated A/B `re-review` on a newly frozen head/diff/body/metadata. That future checkpoint cannot be identified by this author before Controller creates it. No re-review or final acceptance has occurred here.

Residuals: the PR-body edit remains unapplied by this author; native CRLF-sensitive checker behavior remains assigned to Test/Platform/tooling; the per-file fixture-provider guard remains deferred to future test-hardening/Backend. Existing external identity/secret delivery, hosted CI, capacity and live/cloud/media acceptance gates remain owner-routed and unchanged. The runner limitations and superseded static result are custody qualifications only, not additional source findings.

No staging, commit, push, GitHub call, PR creation/update, approval, ready transition, reviewer request, external comment, merge, close, branch deletion or issue mutation is performed by this fix author. No `.codex`, private snapshot, generated file or target is modified.
