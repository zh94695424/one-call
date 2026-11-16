# PA36 final closeout

## Work unit and outcome

- Work unit: `PA36-evidence-role-ivr-association-acl`.
- Type: architecture-sensitive PostgreSQL security prerequisite for the paused PA35 Operations IVR detail read.
- Final status: **final closeout pass**.
- Draft PR: https://github.com/zh94695424/one-call/pull/46.
- Reviewed/accepted PR head before this controller-only closeout checkpoint: `dda535d6d854d4bd5328ee0cec035ee21e399472`.
- Base: `5200355695d39b6aeba446631c8f3f041b96e9ba`.

## What changed

- Added V018 only. It grants `evidence_role` owner-issued, non-grantable column `SELECT` on `evidence.evidence_record(session_id,call_id)`, with fail-closed authority/history/provenance/topology/replay guards and exact evidence16 -> evidence18 target-state checks.
- Preserved V001-V017 and all production Java/OpenAPI bytes. The only new production resource is V018.
- Extended the existing shared Operations-read login to the exact seven non-inheriting SET roles and retained the PA25 topology of 7 logins / 9 exact-ACL-monitored roles / 11 edges.
- Updated PA25 to history 18 and the independent ACL oracle to 53 customer / 290 seven-role / 18 evidence / 410 total tuples, plus the 408/288/16 predecessor state.
- Updated current-only docs17, PA25 runbook and backend README sections. Historical bodies below the unique PA36 delimiter remain byte-identical. Current digests distinguish repository-canonical LF from standard CRLF materialization.
- Added and updated ACL, migration, historical/current-boundary, CI, POM and XML inventory tests without removing or skipping a test identity.
- Corrected the forbidden-delegation test to PostgreSQL's warning/no-op semantics: it now proves in the same transaction that the unauthorized `GRANT` does not confer `call_id` SELECT, then rolls back and rechecks the independent 410-tuple oracle.
- Increased only the `java-runtime` CI timeout from 30 to 45 minutes after the prior exact-head run was cancelled at the old cap.

## Verification

- Controller final `-Pcontainer-smoke clean verify`: BUILD SUCCESS in 35:31.
- Surefire: 77 classes / 712 tests.
- Failsafe: 32 classes / 520 tests.
- Total: 109 classes / 1232 tests.
- Failures: 0. Errors: 0. Skipped: 0.
- Fresh/upgrade PostgreSQL 16.15 migration and Flyway validation passed repeatedly.
- Focused corrected delegation method: 1/1 passed.
- V018 canonical LF SHA256: `892C607787C7BFEB0E8EED0A6235F14563FAB515872EE6558263E2FEADA84817`.
- V018 Flyway checksum: `1606293398`.
- Final exact-head GitHub Actions run `43931272275` on accepted PR head `dda535d6d854d4bd5328ee0cec035ee21e399472`:
  - `contract-regressions`: SUCCESS in 38s.
  - `java-runtime`: SUCCESS in 34m17s.
- PR #46 remained OPEN / DRAFT / MERGEABLE at the verified acceptance point. No merge was performed.

## Review and finding status

- Initial independent PR review A: `docs/reviews/pr-46-review-20261116-232547.md`, `893EC0257255300AFF52C54799A1278CF0BF697E8CE63244D77DC8F1F3882EEE`.
- Initial independent PR review B: `docs/reviews/pr-46-review-20261116-234541.md`, `C65A1E893B90B6B7D34B64F9250591CCC12B6CADB184A951837503EAC9AFE905`.
- Final independent re-review A fallback (Qwen3.8 Max after two invalid DeepSeek provider completions): `docs/reviews/pr-46-review-20261117-023506.md`, `5B9A67A5C5BE54101D4B90EEA87C4B6A357D66674A9E24BFAE4B5D0FF431258B`.
- Final independent re-review B (Qwen3.8 Flash): `docs/reviews/pr-46-review-20261117-020314.md`, `FF5B69C338B3A0001143540C98EE2C906B98FB46ED14DCC52ACB08C59EB01F63`.

| Finding | Final disposition |
| --- | --- |
| `PR46-B-01` | `accepted` and fixed. Java timeout is 45; exact-head hosted Java CI passed in 34m17s. |
| `PR46-B-02` | `accepted` and fixed. LF/CRLF digests and signed checksum are explicit; historical bodies and pins are synchronized. |
| `PR46-B-03` | `accepted` and fixed by controller bookkeeping with verified PR title/body/draft/base/head metadata. |
| `PR46-C-01` | `accepted` test-gap finding and fixed. Expected SQLSTATE 42501 was `rejected-with-reason`; the final warning/no-op privilege-outcome assertion passed. |
| `PR46-RRA3-07` / `PR46-RRB-02` | `deferred-with-owner` to Backend/DBA test-hardening for an optional warning-channel assertion, future exception tolerance and `session_id` delegation variant. The security outcome is already independently covered. |
| `PR46-RRA3-08` | `deferred-with-owner` to Test Infrastructure for additional current-section identity pins on README/runbook. |
| `PR46-RRA3-13` | `deferred-with-owner` to Platform/Security + Backend/DBA for a later context-target-role ACL inventory slice. |
| `PR46-RRA3-14` / `PR46-RRB-01` | `deferred-with-owner` / `rejected-with-reason`: docs14 remains separately owned history, and the README 30-minute sentence is explicitly non-current historical evidence below the PA36 delimiter. |
| `PR46-RRA3-12` | resolved by direct controller comparison: the fix cycle changed `ci.yml` only by the timeout value; the complete PR diff is 71 paths after adding evidence artifacts. |
| `PR46-RRB-03` | accepted controller framing correction: fix3's 11 -> 12 count is its round working set, while the frozen reviewed PR candidate is 69 paths. |
| `PR46-RRB-04` | resolved by exact-head hosted run `43931272275` with both jobs SUCCESS. |

No blocking or unclassified review finding remains.

## Documentation

- Current runtime/security documentation: docs17, PA25 runbook and backend README.
- Gateflow evidence: cumulative plan/fixes, one implementation slice, implementation fixes, plan/code/aggregate reviews, PR review/fixes/re-review and this closeout.
- docs14 was intentionally untouched. Its stale V016-era "Current" section remains `PA36-AGG-B-01`, owned by Technical Documentation + Backend/DBA.

## Residual risks and owners

- **Backend/DBA and Test Infrastructure:** diagnostic-channel/session-id probe breadth and current-section identity pins. Destination: later test-hardening.
- **Platform/Security + Backend/DBA:** context-target ACL inventories and future CI budget/duration monitoring. Destination: later ACL/platform slice.
- **Technical Documentation + Backend/DBA:** docs14 history cleanup with all dependent pins updated atomically.
- **Platform/Security/Operations:** credentials, TLS/pg_hba, rotation and old-password refusal, pool drain/stale sessions, marker custody and exclusive cluster administration remain production deployment work.
- **Security/Evidence/Sharing + PA35 owner:** every `evidence_role` holder gains read access to both association scalars; raw payload/log/response minimization and application-side IVR detail behavior remain PA35/security work.
- **Infrastructure/Security/Backend:** COS/KMS/WORM/media handling.
- **Legal/Security:** retention policy.

## Issue status

PA36 is not linked to a GitHub issue. No closing keyword, issue closeout comment or issue mutation is required or claimed.

## Next entry point

The user should merge Draft PR #46. After merge, fetch the updated `origin/main`, create a fresh managed worktree and `codex/` branch from that merge commit, preserve all older worktrees/private helpers, and run a fresh Phaseflow goal confirmation for the paused PA35 Operations IVR detail read now that the database association-read prerequisite is available.

Do not reuse this unmerged PA36 branch as the next work-unit base. PA35 must revalidate its own contract against the merged main before plan or implementation.

## Completion

- `draft-PR-pass`: passed.
- `final closeout`: passed.
- PA36 work unit: **completed**.

No merge, approval, mark-ready action, reviewer request, external comment, issue mutation or branch deletion was performed.
