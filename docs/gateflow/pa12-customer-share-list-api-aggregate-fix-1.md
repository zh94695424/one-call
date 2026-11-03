# PA12 aggregate deep-review fix 1

- Task: `pa12-aggregate-fix1b-gpt-20261103`.
- Gate: `aggregate deepreview fix`.
- Status: `s1-aggregate-fix-1-complete-pending-re-review`.
- Accepted High finding: PA12-ACL-01 is `deferred-with-owner`, **not fixed**.
- Scope: revise PA12 ACL success claims and clarify what its direct-login negative checks establish. V001-V009 remain frozen; no least-privilege ACL remediation is implemented or claimed.

## Disposition and contract

V009 grants `sharing_role` table-wide SELECT and INSERT and lifecycle UPDATE on `sharing.evidence_share_grant`. Consequently, direct secret-column reads after `SET ROLE sharing_role` are currently permitted. PA12 neither denies nor tests these reads as denied. The existing SQLSTATE `42501` secret-column assertion is against a fresh direct sharing-login connection without `SET ROLE`; it is not role-effective least-privilege evidence.

PA12 success is limited to: the HTTP adapter invokes the frozen creator-scoped PA10 query through the qualified sharing graph; the response projects exactly the frozen 11 metadata fields and no secrets; the application transaction is read-only and an otherwise-permitted write fails with `25006`; SQL, response, and log canaries omit secret values; the direct sharing login lacks only the negative privileges actually asserted; and stable pagination, lifecycle behavior, and workspace isolation remain proven.

Least-privilege role/view/grant redesign and a role-effective negative test are owned by a separate Backend/Database/Security work unit. The destination must be identified before production rollout. No V009 migration or production ACL change is part of this fix.

## Changed paths

The author-owned correction set is exactly these five paths:

- `backend/README.md`
- `backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java`
- `backend/src/test/java/com/onecall/archtests/container/CustomerSharesIT.java`
- `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerShareCursorCodecTests.java`
- `docs/gateflow/pa12-customer-share-list-api-aggregate-fix-1.md`

Controller-owned Gateflow bookkeeping also changed these two paths:

- `docs/gateflow/pa12-customer-share-list-api-plan.md`
- `docs/gateflow/project-execution-control.md`

The complete aggregate-fix delta is therefore seven paths: the five author-owned corrections above plus the Controller-owned plan and control records.

Four accepted Low findings are closed: combined limit/cursor validation precedence and valid cursor replay with a changed limit; full frozen 11-field HTTP projection checks for personal and organization rows; mutation-sensitive non-zero code/revocation generation serialization; and current README labels identifying the PA12 `/shares` slice. The existing eight integration test names, all 26 PA12 method names, workspace isolation, and aggregate inventory are preserved.

All paths outside the seven-path fix delta are content-unchanged, including V001-V009, OpenAPI, design/security/protocol/acceptance/deployment/configuration, production Java, CI, POM and historical review/fix artifacts.

## Freeze packaging rule

The 54-path SHA-256 manifest, complete 54-entry `full.patch`, and CR-normalized replay together are the candidate-content authority. During the re-review correction freeze, `full.patch` is regenerated with intent-to-add for the previously untracked fix artifact, so every manifest path has one diff entry.

Twelve out-of-manifest files have raw EOL-only differences from base: the two known customer web Java files and ten historical Gateflow documents. Their CR-normalized content is identical to base, they are not candidate content changes, and they must not be staged. Byte-for-byte reproduction of those twelve working-tree EOL states is not claimed.

## Verification

- The Controller's first full run after the generation-test change failed only in `CustomerShareCursorCodecTests.wirePageHasOnlyFrozenFieldsAndNullableCursor`: non-`EXTRACTION_CODE` summaries incorrectly used non-zero `codeGeneration`.
- The test was corrected to use `codeGeneration=17` only for `EXTRACTION_CODE` and `0` otherwise, with `revocationGeneration=29` for all entries and explicit serialized-value assertions.
- The final clean run, `mvn -B -ntp clean -Pcontainer-smoke verify`, completed with BUILD SUCCESS in 2:17.
- Surefire: 67 tests / 13 classes. Failsafe: 92 tests / 4 classes (`64+10+10+8`). Combined: 159 tests, with zero failures, errors, or skips.
- PostgreSQL 16.15 was used; Flyway applied V001-V009; `CustomerSharesIT` passed 8/8.
- V009 is byte-identical to base; the V001-V009 migration set remains frozen.
- `git diff --check` passed.
- The four accepted Low findings are closed. PA12-ACL-01 remains `deferred-with-owner`, not fixed.
- Status remains `s1-aggregate-fix-1-complete-pending-re-review`.

## Residual risk and ownership

Because V009 is frozen, `sharing_role` can currently read secret columns through its table-wide SELECT grant. Backend/Database/Security owns the least-privilege role/view/grant redesign and role-effective negative test in a separate work unit; that work must have a defined destination before production rollout. PA12 does not close this risk.

No commit, push, PR, approval, ready-state change, reviewer request, comment, re-review, or later-gate action was performed by the original author task. Controller correction and subsequent review intake are recorded in the project control document.
