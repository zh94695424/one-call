# PA2 V002 identity, organization and employee foundation final closeout

## Work unit

- ID: `PA2-v002-identity-organization-employee-foundation`
- Type: architecture-sensitive migration and persistence feature work unit
- Draft PR: #10, https://github.com/zh94695424/one-call/pull/10
- Branch: `codex/pa2-v002-identity-organization-employee-foundation`
- Base: merged PA1 `main` commit `d93c380b9f0025675931f950d95ca4cfef45647b`
- Accepted PR-review checkpoint: `5cb5c5b0dbdc7252af1d1c25c8da0feebea8ca27`

## What changed

- Added frozen migration `V002__identity_organization_and_employee.sql` with six account, authentication-identity, identity-profile, organization, membership and operations-employee tables.
- Added the exact 47-column, 34-check, 10 PK/unique, 18-index, 16-default and four immediate RESTRICT/RESTRICT FK contract, together with six local immutability guards and narrow owned/cross-module grants.
- Added fail-closed PostgreSQL 16 evidence for inventory, lifecycle/revision behavior, immutable relations, identity collisions, membership revoke/regrant behavior, least privilege and the test-only last-enabled-admin transaction protocol.
- Kept V001 byte-for-byte unchanged and kept Flyway, PostgreSQL JDBC and Testcontainers out of the production runtime artifact.
- Added no V003+ object, API, login/session/credential/OTP implementation, frontend, runtime DataSource/Flyway wiring, cloud provisioning or later-domain workflow.

## Verification

- Default Maven verification passed 10/10 tests.
- Independent PR review ran two complete PostgreSQL `16.15` container profiles at 18/18, plus focused identity/membership concurrency, last-admin and runtime-privilege tests at 1/1.
- Boot smoke, production fat-jar dependency firewall, exact source/CI/XML method-set checks, migration hashes and fresh live catalog/ACL/semantic probes passed.
- V001 SHA-256 remains `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`; V002 SHA-256 is `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5`.
- GitHub Actions run `40512932592` passed `java-runtime` and `contract-regressions` on exact PR head `5cb5c5b0dbdc7252af1d1c25c8da0feebea8ca27`.
- At closeout intake, local HEAD, `origin/codex/pa2-v002-identity-organization-employee-foundation` and PR #10 head all matched that exact checkpoint; the PR remained Open, Draft and `CLEAN`.

## Review and findings

- The plan-review predicate-evidence finding was fixed and independently re-reviewed before implementation.
- All accepted S1 and S3 findings, including evidence-only corrections, reached final `已修复` status; S2 reported no material finding.
- The first aggregate-review process artifact was rejected after an unauthorized Git-config mutation and is preserved only as incident evidence. The fresh aggregate retry passed with no material finding and an unchanged config hash.
- Independent PR review `docs/reviews/pr-10-review-20261027-212826.md` passed with zero material findings; SHA-256 `B9AF0FCED344D2CFED10E2E1B5EC4967F5855DFBBFA791CBE3DD97C78B27172B`.
- The PR reviewer disclosed one read-only `git config --get` query contrary to the literal dispatch command ban. It changed no Git metadata, the repository config hash remained exact at every checkpoint, and the controller accepted it as a non-blocking process deviation. Future review dispatches must enforce the literal ban.
- No accepted finding remains unresolved, and no blocking open question or unclassified risk remains.

## Documentation

- Binding implementation plan: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md`.
- Execution controller: `docs/gateflow/project-execution-control.md`.
- Aggregate review: `docs/reviews/code-review-20261027-210424.md`.
- PR review: `docs/reviews/pr-10-review-20261027-212826.md`.
- Runtime-facing evidence summary: `backend/README.md` and `.github/workflows/ci.yml`.

## Remaining risks and owners

- Floating `postgres:16` test image: Platform/CI owns digest pinning in supply-chain hardening; no digest is guessed in PA2.
- Q2 provider-subject key rotation, Q17 production migration identity/role provisioning and Q23 deployment `SECURITY DEFINER` capability remain with Security/Platform/Database for later approved work units.
- Login/account-status gating and identity bind ordering/classification remain with the customer-iam application service.
- Membership pair advisory locking and terminal `REVOKED` behavior remain with the organization-access application service.
- Last-enabled-admin advisory/count/update orchestration remains with the operations-iam application service; PA2 proves only the persistence contract and a test protocol.
- Optional `wait_event='advisory'` assertion precision, a direct identity-profile parent-delete IT probe, exact S2/S3 evidence wording, bounded concurrency timing and the test-only global enabled-admin precondition remain Backend/QA persistence-test hardening.
- Lifecycle audit append behavior remains application-service work over the accepted PA1 audit primitive.
- Windows LF/CRLF conversion remains optional repository hardening owned by Platform/CI.
- This work unit proves build, migration and synthetic database behavior only. Production cloud, carrier/IVR, load, restore, security and manual acceptance remain in later architecture phases and the acceptance matrix.

## Issue status

This feature work unit has no associated GitHub issue. No issue link, closing keyword, closeout comment or issue mutation is required or claimed.

## Closeout state and next entry point

`PA2-v002-identity-organization-employee-foundation` reached `final closeout pass`. Draft PR #10 remains unmerged by design; no approval, ready-for-review action, reviewer request, external comment, merge, branch deletion or production-acceptance claim occurred.

After the user merges PR #10, update local `main` from `origin/main`, create a new isolated branch/worktree from that merged base, and start a fresh Phaseflow goal confirmation for the tentative next bounded Phase A work unit `PA3-v003-subject-number-and-claim-foundation`. Its candidate boundary is only `V003__subject_number_and_claim.sql` and executable evidence for the frozen subject, caller-number binding, claim and risk-state persistence contract in `docs/14` section 11.3. V004+, application workflows and production provisioning require separate confirmed scope.
