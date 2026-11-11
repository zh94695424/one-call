# PA28 Customer Subject-Detail Read Foundation - Final Closeout

## Result

- Work unit: `PA28-customer-subject-detail-read-foundation`.
- Type: architecture-sensitive authenticated customer read feature over
  existing PostgreSQL persistence.
- Gate state: `final closeout pass`.
- Work unit status: **completed**.
- Draft PR: https://github.com/zh94695424/one-call/pull/39.
- Base: `main` `3f534ac58c6471f139dacb551ea438cc20205dc4`.
- Reviewed PR head before this closeout:
  `9272ab5f6bad4213b27755b3ee541ab7133a814b`.
- PR remains OPEN/Draft/CLEAN with no requested reviewers. No merge,
  approval, mark-ready action, reviewer request, external review comment or
  issue mutation occurred.

## What changed

The service now implements only:

```text
GET /api/v1/customer/subjects/{subjectId}
```

Success returns exactly:

```text
subjectId
activationStatus
evidenceCount
orderCount
```

The endpoint uses the existing frozen `CustomerSubjectDetail` schema. It does
not emit optional `numberMasked`, `trial` or `consentRequirements`; `claimState`
is not a schema field.

Access is derived only from the verified customer session:

- personal access requires `owner_account_id` equality;
- organization access requires the selected ACTIVE organization,
  `owner_organization_id` equality, ACTIVE membership, and one of
  `ADMINISTRATOR`, `MEMBER`, `EVIDENCE_MANAGER` or `AUDITOR`;
- missing/unowned/foreign-workspace subjects return the existing uniform 404;
- inactive account/organization/membership and OPEN risk on any subject binding
  return the existing FORBIDDEN 403 boundary.

The read executes in one customer-role read-only
`PROPAGATION_REQUIRES_NEW` / `ISOLATION_REPEATABLE_READ` transaction. Lifecycle,
ownership, all-binding risk, activation and both counts share one snapshot.
There are no business writes, request-path locks or additional database pools.

V016 is append-only and privilege-only. It adds exactly eight tuples:

- evidence and catalog_order schema USAGE;
- risk `caller_number_binding_id,state` SELECT;
- evidence `subject_id` SELECT;
- order `subject_id,account_id,owner_organization_id` SELECT.

Customer privileges become 34, evidence remains 16, other roles remain 219,
global PA25 becomes 269 and history becomes 16. OpenAPI and V001-V015 remain
byte-identical.

## Verification

Implementation and Controller evidence:

- focused additions: 64 passed;
- focused PostgreSQL/HTTP: 64 accepted unit/configuration tests plus 88
  six-class Failsafe tests passed;
- full `clean -Pcontainer-smoke verify`:
  - Surefire 44 classes / 375 tests;
  - Failsafe 19 classes / 302 tests;
  - zero failures, errors or skips;
- exact raw source = CI = XML inventory:
  - 63 classes / 677 tests;
  - 613 pre-existing identities preserved;
  - 64 additions;
  - marker scan 678;
  - 19 POM Failsafe includes;
- contract validators, default/web JAR smokes and dependency/JAR firewall
  passed;
- V016 Flyway checksum `1669606497`; file SHA256
  `988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f`;
- PA25 independently verifies 269 effective tuples and exact 16-row history;
- tracked `backend/mvnw` remains byte-identical; the external LF wrapper was
  used only for execution;
- Controller `git diff --check` passed.

Hosted PR checks:

- `contract-regressions`: PASS, 35 seconds;
- `java-runtime`: PASS, 16 minutes 25 seconds.

## Documentation

Updated:

- `backend/README.md`;
- `docs/14-production-data-model-and-flyway-schema-spec.md`;
- `docs/17-production-security-design-spec.md`;
- `docs/operations/pa25-production-database-identity-provisioning-runbook.md`;
- cumulative PA28 plan and allowlist arithmetic;
- S1 implementation/fix reports;
- plan-review, code-review, aggregate-review and PR-review artifacts;
- current execution Control and this summary.

The documentation describes only the four-field read foundation and marks
production/live readiness `NOT EXECUTED / NOT CLAIMED`.

## Findings

- `PA28-PLAN-REV-01..07`: fixed and re-reviewed.
- `PA28-S1-IMPL-01`: exact declared-method expectation `10 -> 12`; fixed.
- `PA28-S1-IMPL-02`: controller completion-failure seam now reaches the real
  service while preserving both 503 assertions and `finally` restoration;
  fixed.
- `PA28-S1-IMPL-03`: lifecycle `CapturedOutput` injection preserves the
  ordinary no-argument XML test identity; fixed.
- S1 Code Review A/B: both `pass-with-risks`, zero material findings.
- Aggregate Deepreview A/B: both `pass-with-risks`, zero material findings.
- PR Review A/B: both `pass-with-risks`, zero material findings.

No accepted material finding requires another fix or re-review.

## Residual risk and owners

- Security/Database: application-level authorization is not PostgreSQL RLS;
  predicate-column raw reads remain possible within the trusted application
  boundary. Repeatable-read is snapshot-consistent, not linearizable
  revocation.
- Backend/Test: independent order-reader SQL-failure injection, object-comment
  mutation probe and the reflection-based controller provider seam belong to
  post-PA28 hardening.
- Backend/Architecture: qualified-DSL/bean-name inversion remains a deferred
  architectural cleanup.
- Backend/Database: representative production EXPLAIN/cardinality/SLO
  performance remains unmeasured.
- Dependency/Security: locked npm advisories remain outside this work unit.
- Platform/Database/Security/Operations: production credentials, connections,
  TLS/`pg_hba`, provisioning, rotation, exclusive administration, session
  invalidation, pool drain, Q17/Q23 and live acceptance remain
  `NOT EXECUTED / NOT CLAIMED`.

No unclassified residual risk remains.

## Issue and merge status

This work unit is not tied to a GitHub issue, so no closing keyword, issue
closeout comment or issue mutation is required or claimed.

The PR is an OPEN Draft for user review. The user must merge it. This
Controller did not merge, approve, mark ready, request reviewers, delete the
branch or comment externally.

## Next entry point

After merging PR #39:

1. fetch updated `origin/main`;
2. create a fresh managed worktree and `codex/` branch from the merge commit;
3. run a new Phaseflow goal confirmation for the next bounded work unit.

A tentative candidate is
`PA29-customer-evidence-read-foundation`, covering the still-unimplemented
`listCustomerEvidence` / `getCustomerEvidence` contract. This is only a
post-merge candidate: PA28 goal confirmation does not authorize its plan,
schema, permissions or implementation.
