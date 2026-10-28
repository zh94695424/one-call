# PA5 V005 IVR task/session/call foundation - aggregate deepreview fix-1

| Field | Value |
| --- | --- |
| Task label | `pa5-v005-aggregate-deepreview-fix1` |
| Gate | aggregate deepreview fix only |
| Fix author | implementation support agent (no approval or merge authority) |
| Date | 2026-10-29 |
| Accepted S2 checkpoint | `cbe61368b21a2bb2272959329d2309a5a9d49f33` |
| Frozen review inputs | `docs/reviews/code-review-20261028-224508.md`, `docs/reviews/code-review-20261028-224509.md`, `docs/reviews/code-review-20261028-232133.md` |
| V005 post-fix SHA-256 | `2F4CDEF48FD5B1CEC9B5F4479E99DF3359073C94BF2C45CBC5B2CF8E87BA9310` |

This artifact records the bounded fixes for the three findings in aggregate review A. It does not self-accept the fixes, create a commit, push, open a PR, or approve/merge any branch.

## 1. Scope and changed paths

The fix is limited to existing V005 predicates, existing V005 trigger guards, and additive probes inside the existing V005 S2 behavior methods. No table, column, key, index, trigger, function, role, grant, migration version, or test method was added.

| Path | Change |
| --- | --- |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | Tightened `inbound_session` confirmation shape and `call_event` redacted-payload shape; added absorbing terminal-state guard checks for task/call rows. |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | Updated the frozen PostgreSQL deparse string; added negative probes for the three findings, a savepoint-isolated confirmed-subject FK probe, and an `UNKNOWN -> UNKNOWN` terminal-fact rewrite probe. |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | Synchronized the confirmed-subject predicate, absorbing task/call terminal semantics, typed event redacted shape, T-S2-09/12/37/56 wording, and aggregate-fix errata. |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md` | Added the bounded aggregate-fix amendment to §5.1/§5.2/§6.3 without changing V005 inventory, wave, or method counts. |
| `backend/README.md` | Added database-only PA5 aggregate-fix backstop/probe evidence; no production-service claim was added. |
| `.pa5-contract-lf-validate.sh`, `.pa5-contract-validate.sh`, `.pa5-controller-validate.sh`, `.pa5-fixed-intake.sh`, `.pa5-intake.sh`, `.pa5-run-bounded.sh` | Controller-owned runner updates already present in the workspace; behavior was preserved and syntax/timeout probes passed. Historical `.pa5-s1-*`/`.pa5-s2-*` launch/prompt files are outside this fix. |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-aggregate-fix-1.md` | This author artifact. |

No changes were made to `.github/workflows/ci.yml`, accepted plan-fix history, review artifacts, or application service/API/UI code. The six listed runner files were controller-owned changes already present in the workspace; this fix records their scope but does not alter their behavior. Historical `.pa5-s1-*` and `.pa5-s2-*` launch/prompt files were not included.

## 2. Findings fixed

### F-PA5-AGG-A-01 - terminal task/call rows could be rewritten

`ivr.guard_outbound_call_task` now rejects any substantive update when the old task status is terminal, including status/reason, claim linkage, terminal timestamp, update timestamp, and revision. The existing terminal-to-live rejection remains in place.

`ivr.guard_call_record_parties` now treats `ENDED`, `FAILED`, and `REJECTED` as absorbing terminal states. Their status, task/linkage, bridge/recording/end facts, authoritative seconds, reason, and revision cannot be rewritten. An `UNKNOWN` call may only converge to `ENDED` or `FAILED`; an `UNKNOWN -> UNKNOWN` fact/revision rewrite and `UNKNOWN -> REJECTED` are rejected. The valid `UNKNOWN -> ENDED` path remains covered.

The test method `v005TaskTtlLifecycleAndGuardBackstops` covers a terminal task rewrite. `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops` covers terminal call fact rewrite, `UNKNOWN -> REJECTED`, `UNKNOWN -> UNKNOWN` fact rewrite, and the legal `UNKNOWN -> ENDED` convergence.

### F-PA5-AGG-A-02 - confirmed subject identity was not bound to session subject

`ck_inbound_session_confirmed_shape` now requires a trusted, non-null confirmed subject equal to `subject_id` for `CONFIRMED`, and requires `confirmed_subject_id IS NULL` for every non-`CONFIRMED` status. Existing status/timestamp coupling remains unchanged.

The session probe set now includes a mismatched confirmed subject and a non-confirmed row carrying a confirmed subject; both assert exact `23514` and `ck_inbound_session_confirmed_shape`.

### F-PA5-AGG-A-03 - redacted event JSONB accepted missing/ill-typed members

`ck_call_event_payload_redacted` now requires `source.eventType` as a nonblank string, `source.legId` and `source.sequenceNo` as present values with their documented string/number-or-JSON-null types, and `receipt.eventFingerprint` as a lowercase 64-character SHA-256 string. Existing exact top-level keys, payload object, receipt hash type, and payload hash equality checks remain unchanged.

The event probe set now rejects each missing source member and JSON-null, numeric, and non-hash-string event fingerprints with exact `23514` and the named shape check.

## 3. Test isolation correction

The stricter confirmed-shape predicate makes an unisolated missing-parent probe for `fk_inbound_session_confirmed_subject` impossible when `subject_id` and `confirmed_subject_id` must match. The FK probe therefore uses the established PostgreSQL transactional savepoint technique: as `ivr_owner`, it drops only the preempting `fk_inbound_session_subject`, inserts one missing subject id in both columns, asserts exact `23503` and `fk_inbound_session_confirmed_subject`, rolls back to the savepoint, and verifies both FKs are restored. The committed migration is not weakened.

The deparse map records PostgreSQL 16's canonical rendering of `IS NOT DISTINCT FROM` as `NOT (... IS DISTINCT FROM ...)`; this is a test expectation synchronization only.

## 4. Validation

| Check | Result |
| --- | --- |
| Focused V005 methods | `mvn -B -ntp -Pcontainer-smoke "-Dit.test=PostgresContainerIT#v005TaskTtlLifecycleAndGuardBackstops+v005SessionCallCanonicalCallerAndReciprocalLinkBackstops+v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops+v005ForeignKeyAndDeferredReservationFkBackstops" verify` -> 4/0/0/0, BUILD SUCCESS |
| Full container profile | `mvn -B -ntp -Pcontainer-smoke verify` -> Failsafe 38/0/0/0, skipped 0, BUILD SUCCESS |
| Default test phase inside profile | Surefire 10/0/0/0 |
| V005 test method count | 38 `@Test` methods; no method was added or removed |
| Whitespace | `git diff --check` exit 0 |
| Controller runner checks | `bash -n` passed for the six listed runners; bounded timeout probe through `.pa5-run-bounded.sh` passed with the expected timeout exit and no unbounded child process |

The final full run used PostgreSQL 16.15 through Testcontainers and completed in about 25 seconds. The expected broken-migration fixture logs an intentional Flyway failure; the test suite still completed with 38 passing methods and zero failures/errors/skips.

## 5. Docs, runner, and acceptance disposition

- `docs/14`, the PA5 plan, and `backend/README.md` are now synchronized with the implementation semantics. The six controller-owned runners are bounded/syntax-validated; no historical launch/prompt runner files were changed.
- The source-level implementation and binding prose now match the reviewed terminal/shape semantics. Independent A/B re-review and controller adjudication remain required; this artifact does not self-accept.
- This artifact is evidence for the fix gate only. It is not an accepted deepreview result, production acceptance, GitHub Actions result, or PR state.

## 6. Residual risks and next owner

| Risk | Classification | Owner / destination |
| --- | --- | --- |
| Independent re-review of all three findings and regression against the frozen aggregate contract | needs re-review | controller / reviewers A and B |
| Local Testcontainers green is not GitHub Actions or production acceptance | process | later CI and production gates |
| Claim/reservation orchestration, expiry housekeeping, event absorption/replay, `UNKNOWN` reconciliation service behavior and decision selection remain out of V005 | deferred by plan | later IVR/entitlement service waves |
| Owner-level immutability of fields outside the database guard/grant backstop remains service-owned | deferred residual | IVR service wave |
| Savepoint DDL isolation is PostgreSQL-specific and follows existing suite precedent | accepted test technique / residual | database test maintainer |

**Current status:** implementation complete; validation green; aggregate deepreview re-review and controller adjudication pending. No commit or merge was performed.
