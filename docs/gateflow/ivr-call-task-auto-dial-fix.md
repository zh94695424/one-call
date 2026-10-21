# IVR 呼入自动外呼设计修订 Fix

## Gate / scope

- Gate: aggregate deepreview fix, finding `C1`.
- Aggregate deepreview re-review artifact: `../reviews/code-review-20261021-215258.md`.
- Scope: define `authorizationExpiresAt` behavior for the task-triggered B-leg operation and record its validation cases.
- Changed file: `../13-ivr-call-task-and-inbound-design.md`.

## Accepted finding

The resolve response freezes an authorization deadline, but the prior design did not say whether a delayed IVR retry could start a B leg after that deadline. That left stale external dialing behavior undefined.

## Fix

- Define `authorizationExpiresAt` as the latest time to start a new B-leg operation, not the call completion deadline.
- If B leg has not started by the deadline, IVR must not place it; it reports an idempotent no-outbound terminal result so the backend releases the reservation and fails the task.
- If the B-leg operation started before the deadline, the current call may continue within its frozen recording limit; the deadline alone does not hang it up or release its reservation.
- If no line request was ever issued, IVR reports `OUTBOUND_NOT_STARTED` with reason `AUTHORIZATION_EXPIRED` through the existing call-events endpoint; the backend idempotently fails the task and releases its reservation only after accepting that authoritative terminal fact.
- Uncertain line outcomes remain in reconciliation and never become permission to redial.
- Add acceptance cases for an unstarted expired operation and a B leg started before expiry but answered after it.

## Validation

- `git diff --check`: pass.
- Focused semantic re-review: pass; the design identifies the existing event endpoint, idempotency identity, required terminal reason, IVR proof condition and backend reservation-settlement condition. Uncertain line outcomes remain in reconciliation.
- Aggregate deepreview re-review: pass; finding `C1` fixed and accepted in `../reviews/code-review-20261021-215258.md`.
- Accepted deepreview commit: pending.

## Residual risk

- Real IVR clock synchronization, durable operation identity, line reconciliation, and terminal-event settlement remain assigned to backend/IVR implementation and real-line integration.
