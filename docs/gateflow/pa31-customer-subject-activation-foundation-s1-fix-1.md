# PA31 Customer Subject Activation Foundation — Implementation Fix-1

## 1. Identity and boundary

- Task label: `pa31-subject-activation-implementation-fix1-gpt-20261113-01`.
- Runtime/provider: Codex CLI implementation child, requested GPT `gpt-6.1-sol` / high; provider metadata is not independently exposed by this runtime.
- Date: 2026-11-13. Branch: `codex/pa31-next-work-unit`. Control checkpoint: `297e7c2e349d5cd6f0bb45dd94309a4ec0311137`.
- This fix was limited to the accepted 40 implementation paths, the existing S1 report, and this single artifact. No source, test, migration, OpenAPI, plan, Control or other out-of-bound path was changed.

## 2. Failure diagnosis

The shared root cause is a frozen privilege mismatch in the ordinary ACCEPT completion path. `JooqSubjectActivationRepository.appendOutbox` issues the transition Outbox call as `activation_consent_role`. The existing V001 ACL grants `platform` schema USAGE and `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` EXECUTE only to `customer_iam_role`, `organization_access_role`, `operations_iam_role` and `subject_number_role`. V007 grants the activation role its activation tables and subject/access reads, but does not grant it platform Outbox access. V006 grants the activation role EXECUTE on `entitlement_metering.issue_first_trial`, which does not cover the separate transition Outbox call.

Thus ACCEPTs that reach the transition Outbox call fail with PostgreSQL privilege denial and are sanitized by `DefaultCustomerSubjectActivationService.mapped`; the same shared failure explains first accept, replay/concurrency-dependent ACCEPT setup, organization ACCEPTs, role least-privilege ACCEPTs and the HTTP 500 symptoms. DECLINE and validation/access branches that do not issue a trial/transition Outbox do not exercise this missing privilege. The expected `CONSENT_REQUIRED` around policy linearization can also be obscured by the same broad failure mapping when the test orchestration reaches the shared completion path, but it is not the primary defect.

## 3. Exact evidence

- `backend/src/main/java/com/onecall/core/activation_consent/internal/command/JooqSubjectActivationRepository.java`: `appendOutbox` calls `platform.outbox_append` directly under the activation role.
- `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql`: the platform grant list omits `activation_consent_role`.
- `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql`: `issue_first_trial` EXECUTE is granted to `activation_consent_role`, but platform Outbox EXECUTE is granted only to `entitlement_metering_owner` for the SECURITY DEFINER function's internal call.
- `backend/src/main/resources/db/migration/V007__activation_and_consent.sql`: activation table/schema grants contain no platform schema or Outbox grant.
- Controller evidence: focused PostgreSQL16 ran exactly 34 methods and reported 5 failures, 15 errors, 0 skips; the supplied XML/log show sanitized `SubjectActivationException` on ordinary ACCEPT paths and HTTP 500s.

## 4. Changes and preserved behavior

No Java, test, fixture, migration or grant change was made. A source-only workaround would either require a new privilege, alter the frozen owner/role design, or move the transition Outbox responsibility into an owner path not authorized by the accepted plan. The user-specified stop rule therefore applies: the only truthful repair requires new schema/grant authority or a newly authorized design. No tests were weakened, skipped, deleted, renamed or disabled; all 86 new identities and 838 historical identities remain untouched.

## 5. Validation

- Controller focused PostgreSQL16 rerun: **NOT RUN by this child**; it is the required next Controller gate after authorization of the ACL/design repair. The prior Controller run is the failing evidence, not a pass claim.
- Maven `test-compile` and focused unit/architecture rerun: **NOT RUN / blocked** because Maven attempted to resolve `org.springframework.boot:spring-boot-starter-parent:4.1.2` from Maven Central and this child runtime had DNS/network failure (`repo.maven.apache.org: Temporary failure in name resolution`).
- Docker/Testcontainers: **NOT RUN**; Docker CLI is present but `/var/run/docker.sock` returns `permission denied`.
- Static source review: completed for the shared call path and V001/V006/V007 ACLs. No source fix is claimed.
- Frozen20, V018, arithmetic and exact 40-path audit: no implementation-path changes were made; the pre-existing S1 inventory remains the applicable baseline. This artifact plus the S1 report are the only fix documentation additions/updates.

## 6. Residual risk and readiness

Residual risk is blocking: the candidate is not ready for code review or PostgreSQL16 rerun until the Controller/user authorizes an in-scope ACL/design correction. The next decision must explicitly choose one of: authorize the required platform grant and update the frozen custody accounting, or authorize a minimal owner-side transition-Outbox design with corresponding plan/scope changes. This child must not invent either change.

Readiness: **NOT READY for Controller PostgreSQL16 rerun** under the current authority. No later gate is entered or claimed. No commit, push, PR, merge, approval, reviewer request, external comment, issue mutation or code review action occurred.
