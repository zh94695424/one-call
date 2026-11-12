# PA29 Customer Evidence Read Foundation - Final Closeout

## Result

- Work unit: `PA29-customer-evidence-read-foundation`.
- Type: architecture-sensitive authenticated customer read feature over existing PostgreSQL persistence.
- Gate state: `final closeout pass`.
- Work unit status: **completed**.
- Draft PR: https://github.com/zh94695424/one-call/pull/40.
- Base: `main` `ef9d0823975ef66d1df3b70e921709d8562b4dcb`.
- Reviewed and accepted PR head: `cf1b169f400d67c75c7921ec10d24ee1bbca737f`.
- PR remains OPEN/Draft/MERGEABLE/CLEAN with no requested reviewers. No merge, approval, mark-ready action, reviewer request, external review comment or issue mutation occurred.

## What changed

The customer API now implements exactly:

```text
GET /api/v1/customer/evidence
GET /api/v1/customer/evidence/{evidenceId}
```

The list returns the frozen `EvidencePage` shape and the detail returns the frozen `EvidenceDetail` shape. Safe evidence facts are projected from evidence, artifact, media and recording metadata only; media bytes, object keys, permanent URLs, sharing credentials, live identity, snapshot/event payloads and lifecycle mutation remain absent.

Access is derived only from the verified customer session:

- personal access requires the evidence subject's `owner_account_id` to match the active account;
- organization access requires the selected ACTIVE organization, `owner_organization_id` match, ACTIVE membership and one of `ADMINISTRATOR`, `MEMBER`, `EVIDENCE_MANAGER` or `AUDITOR`;
- missing, unowned, foreign-workspace and other-subject detail returns the uniform 404;
- inactive account/organization/membership and any OPEN risk on a binding of an accessible evidence subject return the existing 403 boundary.

List risk is checked across all accessible subjects with evidence before pagination, including off-page subjects. Detail risk is checked after ownership. All four evidence statuses are visible. Keyset order is `createdAt DESC, evidenceId DESC`, with default limit 20, inclusive 1-100, limit+1 lookahead and an endpoint/account/workspace-bound HMAC cursor.

Reads use one lazy customer-role read-only `PROPAGATION_REQUIRES_NEW` / `ISOLATION_REPEATABLE_READ` transaction. Lifecycle, ownership, risk, headers, artifact roles and detail artifacts share one snapshot. There are no business writes, request-path locks or extra database pools.

V017 is append-only and privilege-only. It adds exactly 19 owner-issued column SELECT tuples:

- `evidence_record`: 8 columns;
- `evidence_artifact`: 3 columns;
- `media_object`: 4 columns;
- `recording`: 4 columns.

All use grantee `customer_iam_role`, grantor `evidence_owner`, no grant option. Customer privileges become 53, evidence remains 16, other roles remain 219, global PA25 becomes 288 from 269+19, and history is 17. V001-V016 and OpenAPI remain byte-identical.

V017 accepts only the exact V016 pre-state or exact V017 final state. The first in-migration validation pass pairs old ACL with 16 history rows/no row above rank16, or final ACL with 17 rows plus the exact rank17 V017 row. It rejects V016-ACL/V017-history, V017-ACL/V016-history, and final-ACL/cardinality-17-without-the-exact-V017-row before any grant. The second pass permits the legitimate fresh-migration window before Flyway records row17.

## Verification

Controller-final local evidence:

- clean container-smoke verify:
  - Surefire 49 classes / 417 tests;
  - Failsafe 21 classes / 336 tests;
  - total 70 classes / 753 tests;
  - zero failures, errors or skips;
- exact source=CI=XML inventory: 49/417 + 21/336 = 70/753;
- annotation scan 754 with one historical comment;
- 21 exact POM Failsafe includes;
- 76 test additions: 42 Surefire and 34 Failsafe;
- default/nonweb and opt-in web packaged-JAR smokes passed;
- dependency firewall passed its 72-coordinate/65-JAR boundary and 116 adversarial probes;
- contract validators A1-A5 passed;
- V017 raw SHA256 `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad`, Flyway checksum `-41822162`;
- PA25 independently validates 288 effective ACL tuples, history17, exact predecessor checks and rejection without repair;
- V016 checksum `1669606497` and frozen V001-V016/OpenAPI identities remain unchanged;
- all 54 implementation paths, two fix artifacts, eight code-review artifacts, two aggregate artifacts and two PR-review artifacts are accounted for; private `.pa29-*` inputs remain untracked and excluded;
- `git diff --check` passes.

Hosted PR checks on final head `cf1b169f`:

- `contract-regressions`: PASS;
- `java-runtime`: PASS, including PostgreSQL container smoke, exact inventory, packaged-JAR smokes and dependency firewall.

## Documentation

Updated:

- `backend/README.md`;
- `docs/17-production-security-design-spec.md`;
- `docs/operations/pa25-production-database-identity-provisioning-runbook.md`;
- accepted PA29 plan and exact allowlist arithmetic;
- S1 implementation, fix-1/fix-2 and all review artifacts;
- current execution Control and this closeout.

Implementation report §9 remains the historical fix-1 narrative; the fix-2 artifact and Controller validation evidence are the authoritative account of the final V017 seal. Current-boundary documentation preserves the production/live limitation and valid-artifact fail-closed semantics.

## Findings

- `PA29-PLAN-REV-01..12`: fixed and re-reviewed.
- `PA29-S1-IMPL-01`: typed Mockito UUID matcher amendment; fixed.
- `PA29-S1-IMPL-02`: historical V016/current V017 privilege-test targeting and rank17-to-rank18 fixture repair; fixed.
- `A-LOW-1`: V017 ACL/history hybrid rejection; fixed.
- `A-LOW-2`: V016/269 direct oracle assertion restoration; fixed.
- `A-RR-LOW-1`: exact rank17 V017-row requirement and third non-SQL-history witness; fixed.
- Initial Code Review A/B: zero material findings, two low findings accepted and fixed.
- Re-review rounds and Aggregate Deepreview A/B: zero new material findings; all accepted findings fixed.
- PR Review A/B: both `PASS-WITH-RISKS`, zero material findings.

No accepted material finding remains open and no further fix or re-review is required.

## Residual risk and owners

- Security/Database: `identity_at_capture` remains a column-readable JSONB at the raw customer-role boundary; application SQL exposes only the validated scalar `capturedAt`. This is a trusted-application boundary, not JSON-path security.
- Security/Database: authorization is enforced by application predicates and exact ACLs, not RLS. `REPEATABLE_READ` provides snapshot consistency, not linearizable revocation.
- Backend/Database: representative tenant-cardinality EXPLAIN/SLO and large-subject-set bind-array capacity evidence remain unmeasured.
- Backend/Test: independent SQL-failure injection and broader contention campaigns belong to post-PA29 hardening.
- Dependency/Security: locked npm advisories remain outside this work unit.
- Platform/Database/Security/Operations: production credentials, TLS/`pg_hba`, migrator custody, deployment, rotation, pool drain, session invalidation, Q17/Q23 and live acceptance remain **NOT EXECUTED / NOT CLAIMED**.

No unclassified residual risk remains.

## Issue and merge status

This work unit is not tied to a GitHub issue, so no closing keyword, issue closeout comment or issue mutation is required or claimed.

The PR is an OPEN Draft for user review. The user must merge it. This Controller did not merge, approve, mark ready, request reviewers, delete the branch or post an external review comment.

## Next entry point

After merging PR #40:

1. fetch updated `origin/main`;
2. create a fresh managed worktree and `codex/` branch from that merge commit;
3. run a new Phaseflow goal confirmation for the next bounded customer/evidence work unit.

The tentative next work-unit identifier is `PA30`; its scope, contract, schema, permissions and implementation are not authorized by PA29 closeout and must be confirmed from the updated main and design sources in a fresh turn.
