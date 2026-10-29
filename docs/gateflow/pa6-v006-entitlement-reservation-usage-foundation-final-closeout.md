# PA6 V006 entitlement reservation usage foundation - final closeout

## Work unit

- ID: `PA6-v006-entitlement-reservation-usage-foundation`
- Branch: `codex/pa6-v006-entitlement-reservation-usage-foundation`
- Base: `main` at `aa2b0925e6b8ffdac4587f2e44c4c0dde393d649`
- Draft PR: https://github.com/zh94695424/one-call/pull/14
- Reviewed PR head: `3a08e611338309774ba5c31fce1b54aa9e651c54`
- Accepted PR-review checkpoint: `b7ac228fb1a7d09cc9ac96368c110bdcb155dc25`
- Draft-PR-pass bookkeeping head: `27214a256263c4b2b38c69303e69d31bf2993fe9`
- Status: `final closeout pass`

## Delivered

PA6 adds the V006 Flyway persistence foundation for trial policies, entitlement grants, first-trial facts, entitlement balances, issuance, usage reservations and usage facts. It also adds the approved `issue_first_trial` SECURITY DEFINER function, the late `ivr.ivr_decision.reservation_id` foreign key and the exact owner/runtime/cross-module privilege boundary.

The cumulative PostgreSQL evidence now contains 45 executable container methods and synchronized CI inventory. V001-V005 remain byte-identical, V006 remains frozen at SHA-256 `830ED14C0ACAB61BB2CE74FD2F3A314239FC133EF59B76D53B08E1F95F36C475`, and the authorized docs/14 change is limited to the two trigger-owner corrections. No V007+ object, application service, API/JPA/runtime DataSource wiring, cloud resource or production deployment was introduced.

## Verification

- Accepted plan, S1, S2, aggregate deepreview and PR-review checkpoints are present.
- Final PostgreSQL 16.15 evidence is 45 tests, 0 failures, 0 errors and 0 skipped; source, CI and accepted XML method sets are exactly 45/45/45.
- Default Surefire evidence is 10/0/0/0; boot smoke and fat-jar firewall passed.
- Aggregate findings A-01 through A-04 and B-01 through B-03 are fixed and independently re-reviewed.
- PR Reviewer A (DeepSeek) and Reviewer B (MiMo Pro) independently reported no material finding against the same frozen PR head.
- GitHub Actions runs `40852343596`, `40855272736` and `40855718693` passed `java-runtime` and `contract-regressions` at the reviewed, accepted-review and draft-pass heads respectively.
- PR #14 remains Open/Draft/MERGEABLE/CLEAN. No merge, approval, Ready-for-review transition, reviewer request, external comment or issue mutation was performed.

## Finding status

All accepted plan, slice, aggregate and PR-review findings are final `已修复`. No accepted finding, blocking open question or unclassified residual remains. Reviewer B's inability to query live GitHub metadata is superseded by controller-observed PR facts and green exact-head checks.

## Residual risks and owners

| Residual | Owner | Destination |
| --- | --- | --- |
| One duplicated V007-FK absence probe uses the wrong schema while two correct probes compensate; helper names and a currently empty task-ID cleanup registry are imprecise. | Backend container-test maintainer | Next approved `PostgresContainerIT` maintenance touch. |
| Some negative probes depend on PostgreSQL 16 CHECK/FK evaluation order; expiry evidence runs in UTC only. | Database/Platform test owners | PostgreSQL or deployment-timezone upgrade validation. |
| Fresh-source collisions surface native `23505`; issuance terminality, settle-once, policy publication, reservation/settlement and Outbox delivery remain service-owned. | Entitlement-metering and IVR service owners | Later application-service work units; do not retrofit into V006. |
| Failsafe XML is not retained in-tree, although accepted local evidence and exact-head GitHub logs are green. | CI/controller owner | Evidence-retention improvement in a later CI work unit. |
| The late FK assumes existing V005 reservation references are null or valid; production role membership and migration identity are not exercised by the Testcontainers superuser. | Platform/DBA | Production provisioning and migration rehearsal gate. |
| `activation_consent_role` execution is catalog-proven but not exercised through an actual function call; mutable-head owner DELETE remains discipline/privilege enforced. | Backend test and service-design owners | V007/service-layer evidence and delete-policy validation. |
| Two-connection races prove bounded winner semantics, not production load, deadlock, chaos or policy-disable races. | Platform/QA | Later load, chaos and production acceptance work. |
| Telephony, protocol/wire mapping, API/JPA/runtime wiring, Tencent Cloud deployment, observability, backup/restore and production acceptance remain outside PA6. | Named IVR/API/Platform/Security owners | Their approved later work units and production gates. |

## Documentation and issue status

The PA6 plan, implementation/fix artifacts, A/B review artifacts, project control record and this closeout are the durable Gateflow evidence. This architecture-sensitive work unit is not backed by a GitHub issue, so no closing keyword or issue closeout comment applies.

## Next entry point

After PR #14 is merged, update local `main` from `origin/main`, create a fresh isolated branch/worktree from that merged base and start Phaseflow goal confirmation for tentative `PA7-v007-activation-consent-foundation`, bounded to `V007__activation_and_consent.sql` and its executable persistence evidence. Do not start PA7 from this unmerged branch, reuse PA6 authorization for application services, or treat the tentative ID as authority to expand beyond the frozen V007 contract.
