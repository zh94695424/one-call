# PA40A customer catalog offer read ACL — final closeout

## Delivery

PA40A delivers only the PostgreSQL least-privilege prerequisite for the later customer catalog-offer HTTP read:

- V019 is the sole new migration. It grants two product, ten version, five offer and five catalog-benefit columns to `customer_iam_role`: exactly 22 owner-issued, non-grantable column `SELECT` tuples.
- Customer effective ACL changes from 53 to 75. PA25 changes from 410 to 432 while preserving seven current logins, nine monitored roles, eleven membership edges, marker slots, owners, settings, default privileges and all non-target capabilities.
- V001-V018 remain byte-frozen. Fresh install, V018 upgrade and replay converge to the same exact graph and fail closed on partial, extra, wrong-grantor or grantable state.
- No HTTP endpoint, application Java, OpenAPI, row filtering/RLS, new pool/identity, production credential or later offer behavior is included.

## Verification

- Controller XML: Surefire 105 classes / 1026 tests and Failsafe 41 classes / 682 tests, zero failures/errors/skips; 146 / 1708 total. `CustomerCatalogOfferReadPrivilegesIT` passed 16/16 on PostgreSQL 16.
- The final full aggregate was a disclosed split run after Docker disappeared at 16/41 Failsafe classes, completed in bounded batches on the same compiled candidate. No uninterrupted all-green `clean verify` is claimed.
- Aggregate fix 1 verified the current 27-path canonical oracle as `58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98`; canonical content was accepted under LF, CRLF and attribute materialization, and one-byte mutation was rejected on every current path. Four boundary classes passed 32/32; `OperationsIvrSessionReadsIT` passed 24/24 and `CustomerCatalogOfferReadPrivilegesIT` 16/16 in a clean projection.
- Source=CI=POM inventory is 105/1026 + 41/682 = 146/1708, annotation scan 1709, POM 41, migrations 19. Historical F26/H26 and historical PA25 reconstruction remain unchanged.
- A2 IVR S3 and local A5 acceptance S2 retain pre-existing source/document input failures on untouched inputs and are not reported as passes.

## Findings and review chain

- Plan reviews, coding reviews and aggregate reviews all have durable artifacts.
- Code review: A zero findings; B's only Low (`PA40A-CR-B-01`, historical comment-only `@Test` coupling) is `deferred-with-owner` to a future Test/CI inventory-hardening work unit because the accepted plan freezes that comment and the 1709 ladder.
- Aggregate review accepted one blocking provenance defect (`PA40A-AGG-A-01` / `PA40A-AGG-B-01`): current raw hashes were reachable only from the pre-commit mixed-EOL worktree. Fix 1 moved current verification to committed canonical content; fresh A/B aggregate re-review independently closed the finding.
- PR review: A and B both return `pass-with-risks`, zero new findings, and confirm the final 76-path PR is the accepted ACL prerequisite with all prior findings closed or properly deferred.

## Residual risks and owners

- Role-wide reads without endpoint/row filtering: resulting ACTIVE/channel/customer predicates, cursor and HTTP operation belong to the later PA40 customer catalog-offer read slice and Backend/Security.
- Production TLS/pg_hba, credential custody/rotation, pool drain, live rollback-by-backup and bootstrap ordering: Database/Platform/Security/Operations, PA25 runbook Q17/Q23.
- Host representation and the pre-existing comment-only annotation convention: Test/CI maintenance. Historical OpenAPI raw proof intentionally depends on the unchanged `eol=crlf` attribute.
- Cooperative advisory locking does not constrain a privileged concurrent administrator: Database/Platform operational control.
- Hosted CI is externally unavailable: both GitHub Actions jobs failed before any step with an account payment/spending-limit annotation. Controller evidence is recorded, but hosted green is NOT claimed; Platform/account owner must rerun exact-head CI after billing recovery.

## PR and lifecycle

- Draft PR: https://github.com/zh94695424/one-call/pull/51
- Accepted PR review checkpoint: `bde9a51992b5b3ec10490a9ee0ba0194b619352b`
- The PR remained Open/Draft/Mergeable. No merge, approval, mark-ready, reviewer request or external comment was performed by the Controller.
- Issue linkage is not applicable; this work unit is not tied to a GitHub issue.

## Next entry point

After the user merges PR #51, fetch the updated `origin/main` from the target base and start a fresh Phaseflow goal confirmation for the later customer catalog-offer HTTP read slice. That next work unit must depend on this merged ACL foundation and must not reuse this unmerged branch or treat PA40A as authorization for endpoint/RLS/front-end scope.
