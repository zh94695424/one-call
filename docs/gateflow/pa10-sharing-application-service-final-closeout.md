# PA10 sharing application-service foundation final closeout

## Status

- Work unit: `PA10-sharing-application-service-foundation`
- Final closeout: **pass**
- Draft PR: [#18](https://github.com/zh94695424/one-call/pull/18)
- Base: `main` / `bdfafc706ce7c1f9f515fa27434139e31769576f`
- Branch: `codex/pa10-sharing-application-service`

## What changed

- Added the minimal query-only PostgreSQL runtime foundation and the internal
  read-only `listCustomerShares` application-service path.
- Added trusted account plus optional organization context, strict same-creator
  workspace predicates, current ACTIVE lifecycle rechecks, a fresh read-only
  `REQUIRES_NEW` transaction, transaction-local `sharing_role`, exact 11-field
  secret-free projection, DB-native keyset pagination, profile-gated
  Hikari/Spring JDBC/jOOQ, test/CI-only Flyway and a six-field sanitized
  diagnostic.
- Added exact unit/configuration/boundary/PostgreSQL test inventories, CI/JAR
  guards and capability documentation.
- No HTTP/auth/mutation/admission/ticket/media/cloud/policy surface or
  migration/schema/ACL/OpenAPI/design/security change was added.

## Verification

- Targeted Surefire: 11, zero failures/errors/skips.
- Default verification: 21 Surefire, zero failures/errors/skips.
- Container profile: 21 Surefire + 74 Failsafe (64 historical + 10 new),
  zero failures/errors/skips on PostgreSQL 16.15.
- Source, CI literal and XML method inventories match exactly.
- Enforcer, JAR dependency firewall, database-free default JAR and unchanged
  Node contract regressions pass.
- Remote GitHub CI at final reviewed product head `649123f`: `java-runtime`
  and `contract-regressions` both pass in run `41305072060`.
- Final review/closeout push `09efaba` matches the remote branch and Draft PR
  head; subsequent closeout documentation does not alter the product tree.
- V001-V009, OpenAPI, design/security docs, protocol, deployment and acceptance
  contracts remain unchanged.

## Review closure

- Plan and plan re-review: accepted.
- S1 code review and fixed-code re-review: accepted; F1/F3/F5 closed.
- Aggregate deepreview: accepted, no material product finding.
- PR review: accepted, A/B both `pass-with-risks`, no material code finding.

## Remaining risks and owners

| Risk or deferred boundary | Owner / destination |
| --- | --- |
| Future R2DBC/Netty driver guard names and complete transitive inventory (`AGG1`) | Backend/CI + Security |
| Local Alpine image versus Debian CI provenance (`AGG2`) and fixture-start refusal | Backend/CI + Platform/Database |
| Real customer authentication and trusted adapter context | Identity/Backend/Security |
| Q15 opaque wire cursor | Backend/API/Security |
| Q17/Q23 production provisioning, role membership and ownership transfer | Platform/Database/Security |
| Sharing policy, Q12/Q22 values, mutation/admission/ticket/media/playback | Product/Sharing/Security |
| Production collation, creator/order index, scan/sort capacity, timeout/SLO | Backend/Database/Platform Reliability |
| Downstream serializer/edge/log-collector redaction (`R-A3-REDACTION-RUNTIME`) | Security/Observability/API |

## Completion boundary

The work unit is complete through Draft PR, accepted independent PR review,
remote verification and final closeout. It is not merged, approved, marked
ready, remotely assigned to reviewers, or declared production-ready. After the
user merges PR #18, fetch the updated `main` and start the next Phaseflow work
unit from the merged baseline.
