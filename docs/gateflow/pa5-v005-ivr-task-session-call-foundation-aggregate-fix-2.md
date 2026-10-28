# PA5 V005 aggregate deepreview fix-2

| Field | Value |
| --- | --- |
| Task | `pa5-v005-aggregate-deepreview-fix2` |
| Gate | bounded aggregate deepreview re-review fix only |
| Date | 2026-10-29 |
| Input | Reviewer B finding `F-PA5-AGG-B-01` and `F-PA5-AGG-B-02` from `docs/reviews/code-review-20261029-033938.md` |
| Authority | controller adjudication; this artifact does not self-accept, commit, push, approve or merge |

## Scope

This fix addresses only the review launcher status propagation and the plan's exact guard-predicate summary. It does not change the V005 migration, test methods, `docs/14`, control bookkeeping, CI, or Git history.

| Path | Change |
| --- | --- |
| `.pa5-s2-review-launch.sh` | Retains both background reviewer PIDs, waits for each child explicitly, and exits non-zero when any reviewer subshell fails. The per-role `.exit`, `.stderr`, JSON and artifact evidence paths remain unchanged. |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md` | Extends section 5.2 with the exact terminal task tuple freeze, terminal call tuple freeze, UNKNOWN same-state rewrite rejection, and the only legal UNKNOWN -> ENDED/FAILED convergence. |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-aggregate-fix-2.md` | This implementation artifact. |

No schema, inventory, SQL migration, test, `docs/14`, control document, CI file, or unrelated runner was modified.

## Fix details

`review_pids` is populated by `launch`. The parent loops over the two PIDs with `wait`, accumulates a non-zero aggregate status, prints the existing completion marker, and emits `REVIEW_RUNNERS_FAILED` plus exit status 1 when a reviewer fails or times out. A successful pair preserves the prior zero exit and `REVIEW_RUNNERS_EXITED` marker.

Plan section 5.2 now mirrors the implemented V005 guards: terminal task rows freeze status/reason/claim linkage/terminal time/update time/revision; `ENDED`, `FAILED` and `REJECTED` call rows are absorbing; UNKNOWN same-state fact rewrites and UNKNOWN transitions other than authoritative ENDED/FAILED are rejected.

## Validation

- `bash -n .pa5-s2-review-launch.sh` passed.
- `bash -n .pa5-run-bounded.sh` passed as the launcher dependency.
- Static check confirms `review_pids`, explicit `wait "$pid"`, aggregate failure status and `REVIEW_RUNNERS_FAILED` are present.
- `git diff --check` passed for the fix paths.
- No V005 SQL or container test was rerun because this fix changes only controller tooling and plan prose; the prior private V005 validation remains the evidence for database behavior.

## Residual limits

- GitHub Actions, exact-head CI, production deployment and IVR/provider acceptance remain unverified.
- The historical `.pa5-s2-*` launcher is controller tooling; future manifests must keep its scope aligned with the controller-owned runner set recorded by the aggregate-fix-1 artifact.
- This artifact awaits independent controller adjudication and re-review; it is not an acceptance or merge record.
