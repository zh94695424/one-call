# PA32 Operations Subject Suspension/Resume Lifecycle - Final Closeout

- Work unit: `PA32-operations-subject-suspension-resume-lifecycle`
- Type: architecture-sensitive authenticated Operations commands over existing PostgreSQL persistence
- Branch: `codex/pa32-next-work-unit`
- Base: `main` at `8206f2a8f8a40cd58793efdda55aa41843af5d5f`
- Draft PR: #43 `https://github.com/zh94695424/one-call/pull/43`
- Accepted PR-review head: `1778ffb955fe3dfbd86e624186cc19db4b2ec2f8`
- Status: `final closeout pass`; production readiness remains NOT CLAIMED

## What changed

Implemented exactly the two frozen Operations commands:

- `POST /api/v1/operations/subjects/{subjectId}/suspensions`
- `POST /api/v1/operations/subjects/{subjectId}/resumptions`

The delivered behavior:

- requires the independent opaque `employeeSession`, exact employee audience and one authoritative ACTIVE `OPS` employee;
- never accepts employee identity, role or source domain from the client;
- resolves zero/two/uncreatable verifier or employee-lock collaborators as sanitized fail-closed errors;
- uses one authoritative transaction and the frozen subject, activation, employee/administrator, Outbox and audit lock/role order;
- permits only `ACTIVE -> SUSPENDED` and `SUSPENDED -> ACTIVE`, with one revision advance, one matching transition, one Outbox fact and one safe audit fact on first success;
- returns the first immutable ten-field transition on exact replay and no duplicate transition/audit/Outbox effects;
- rejects changed action semantics or stale state as the frozen `409 ACTIVATION_STATE_CONFLICT`;
- does not reissue a trial or mutate consent, grant, balance, reservation, evidence or order data on resume;
- validates canonical UUID input, the closed two-field body, 1-128 code-point action identity, 1-64 code-point reason and exact error precedence;
- conceals all non-matching Operations routes and preserves the customer/public API boundaries.

OpenAPI, docs/14, docs/15, docs/17 and V001-V017 are byte-identical. No V018, schema/ACL change, production provider/KMS, credential, live acceptance or production-readiness claim was added.

## Verification

- Focused PostgreSQL 16.15: 78 Surefire + 70 Failsafe methods, zero failures/errors/skips.
- Full clean `-Pcontainer-smoke verify`: 64 classes / 574 Surefire methods + 27 classes / 440 Failsafe methods = 91/1014, zero failures/errors/skips.
- Exact source=CI=POM=XML inventory passed: annotation token `1015`, 27 Failsafe includes, migrations exactly V001-V017.
- Packaged default/web JAR smokes passed 17 bounded HTTP probes with no DB/verifier adapter or sensitive leakage.
- Production dependency firewall passed baseline37/34, ssm72/65, counterparts64, dependency-only8, packager1 and 116 adversarial probes.
- A1 and checked-in-current A3/A4 passed directly. A2 and A5 S1/S2 passed against repository-external LF-equivalent copies of unchanged CRLF documents with zero A0/A1 drift.
- Hosted run `43538353838` on accepted PR-review head `1778ffb9`: `java-runtime` passed in 27m30s and `contract-regressions` passed in 28s.
- Every PostgreSQL container used by Controller validation reported PostgreSQL 16.15 and validated/applied exactly V001-V017.

## Independent review chain

- Plan review A/B, accepted plan fix-1/fix-2 and final plan re-review A/B passed after `PA32-PLAN-REV-01..08` closed.
- Code review A/B and targeted re-review A/B passed after `PA32-CR-A-01` and `PA32-CR-A-02` closed.
- Aggregate deepreview A/B found three Low test-evidence gaps and the composed clock-domain risk; `PA32-AGG-L-01..03` were fixed in the existing 8/18 methods.
- `F-1` clock-domain risk remains `deferred-with-owner`: positive customer-JVM-ahead/DB-behind skew can cause a transient sanitized 503 with rollback; negative skew is not reachable across current writers.
- `F-2` residual fragmentation closed in the consolidated owner/destination matrix.
- Targeted aggregate re-review found `PA32-AGG-RR-L-01` wording inaccuracy; the positive-only clock statement was corrected and both targeted reviewers passed it without a new material finding.
- PR review A/B passed the complete 62-path PR diff with no material finding.
- A/B submissions remained peer-isolated until both completed.

## Finding status

- `PA32-PLAN-REV-01..08`: fixed/closed.
- `PA32-CR-A-01` employee-lock reverse serialization: fixed/closed.
- `PA32-CR-A-02` deterministic failed-backend PID reuse: fixed/closed.
- `PA32-AGG-L-01..03`: fixed/closed.
- `PA32-AGG-RR-L-01`: fixed/closed.
- B `F-1`: `deferred-with-owner`.
- B `F-2`: fixed/closed.

PR-review open questions are adjudicated:

- non-JSON `Accept -> INTERNAL 500`: rejected-with-reason because the accepted plan explicitly prohibits introducing 406 and the OpenAPI default is Internal;
- canonical lowercase UUID requirement: rejected-with-reason because plan section 5.3 explicitly freezes lowercase canonical UUID text;
- broader non-OPERATIONS transition validation: deferred-with-owner to Backend data-integrity hardening; no current writer can create the rejected row;
- duplicated employee-audience/text-bound definitions: deferred-with-owner to Operations IAM / Backend maintainability cleanup.

No accepted finding or blocking open question remains.

## Residual risks and owners

| Residual | Owner / destination |
| --- | --- |
| Production employee-session verifier, issuer/store, expiry/revocation, MFA and Q1 | Backend / Security / Operations IAM production deployment |
| Dedicated credentials, non-inherited SET ROLE memberships, TLS, rotation, UTC/pool drain and PostgreSQL16 provisioning | Database / Platform / Security |
| Positive customer-JVM-ahead/DB-behind clock skew can transiently fail Operations with sanitized 503 | Backend / Database / Platform / Operations; follow-up activation-clock-hardening work unit before production readiness |
| UTF-8 deployment and non-ASCII `Idempotency-Key` live-header policy | Backend / Database / Platform / API |
| Audit diagnostic suppression and literal snapshot-key reconciliation | Backend / Security |
| Response-delivery-loss semantics | Backend / Security / Operations production runbook |
| Global `operations_iam.admin_mutation` serialization throughput and capacity tuning | Operations / Backend / Platform |
| Future Operations routes must extend the two-POST filter allowlist and customer pass-through in lockstep | Backend / Operations API |
| PostgreSQL16/Hikari/jOOQ upgrade-sensitive fail-closed assertions | Backend / Test / Platform |
| `RATE_LIMITED`/429 contract retained without a producing throttler | Backend / API; accepted plan boundary |
| Live production acceptance, multi-replica behavior, credential rotation and operational readiness | Platform / Operations / Security |

## PR and issue state

PR #43 is Open and Draft against `main`. The workflow did not merge, approve, mark ready for review, request reviewers, send an external comment or mutate an issue.

This work unit is not tied to a GitHub issue; issue link, closing keyword and closeout comment are not applicable.

## Next entry point

After the user reviews and merges PR #43:

1. fetch the updated target `main`;
2. create a fresh managed worktree and `codex/` branch from that merge commit;
3. reread the project execution control and applicable design sources;
4. run a fresh Phaseflow goal confirmation for the next bounded work unit.

No next work unit is authorized or defined by this closeout.
