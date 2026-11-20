# PA38 operations catalog sale lifecycle - final closeout

## Delivered scope

PA38 S1 implements the two frozen Operations catalog lifecycle commands:

- `POST /api/v1/operations/catalog/versions/{versionId}/publications`
- `POST /api/v1/operations/catalog/versions/{versionId}/offsales`

Both routes require an ACTIVE server-derived `ADMIN`. Publication runs under a
per-product advisory lock, validates the expected ACTIVE/revision, retires the
previous ACTIVE before activating a never-published DRAFT candidate, and commits
the CAS lifecycle update, audit facts and Operations Outbox events atomically.
Offsale advances one expected ACTIVE version to OFFSALE with the same transaction
and fact guarantees. Exact current-state replay is a zero-write success; stale
and conflicting operations make no lifecycle or fact change.

The implementation preserves the frozen OpenAPI, docs12/14/15/17, existing
database role families and V001-V018. No V019 migration or new broad privilege
was introduced.

## Verification

- Product candidate: 32 paths / 572,735 shipped raw bytes /
  `db38b4375c29addd13bfbb8cacde90a172bc32469a736b6b70d7190768500555`.
- Committed LF blob projection: 32 paths / 568,803 bytes /
  `53fccb695d05d796214fd8e15e8dfc3a915a614b4cd4a245979f8fdc17aebe79`.
- Production manifest: 310 /
  `1e0a68c8d766bbac953579975c75787c4801b45540c92e4d9c21ddae4662b045`.
- Java: 290 main / 180 test. Migrations: V001-V018, no V019.
- Test inventory: 135 classes / 1,564 methods, split 97/934 Surefire and
  38/630 Failsafe, with the 1,565-line source scan oracle.
- Final source/config CI: run `44577373056` at
  `3a0a3ea9c777dd16ba40dde970ac39d667b832bf`, tree
  `ddb5722f88d7cf6e433cd42b6fe8d3cd59318956`.
- Accepted PR-review CI: run `44595668761` at
  `07c7b056e3433ec7b0dbe93340a8311672a8c008`.
- The final closeout commit is controller/documentation-only and has the same
  reviewed product candidate. Its exact-head CI result is verified after push.

## Review closure

- Aggregate re-reviews RR14-RR18 converged on documentation-only ledger defects;
  RR18 A/B both passed with zero findings.
- First PR review: A `pass-with-risks`; B requested changes for validation
  evidence and deterministic EOL. The reported product surface had no
  authorization, lifecycle, concurrency, replay or atomicity defect.
- The first fix added deterministic `.gitattributes`, explicit CI triggers and
  exact source/config CI. The initial raw-hash failures for PA25 and OpenAPI were
  reproduced and corrected without changing their frozen content.
- PR re-review2 and re-review3 closed the accepted findings. Remaining B findings
  are Low evidence/change-control items with named owners; no High, Medium or
  product defect remains.
- Issue linkage is not applicable: this work unit has no issue and the repository
  defines no issue-link gate.

## Residual owners

| Residual | Owner / destination |
| --- | --- |
| Mixed CRLF/LF repository hygiene | Backend/CI, dedicated repository hygiene work |
| Self-contained XML/evidence bundle custody | Controller + Backend evidence maintenance |
| Floating `postgres:16`, `txid_current()` modernization and CI timeout headroom | Platform/CI, source/config hardening with its own exact-head CI |
| Outbox ordering, retention and dispatch | Platform/Operations, dispatcher/consumer contract work |
| Shared pool capacity | Platform/Operations + Security |
| Broad frozen ACLs | Database/Security + Catalog-order |
| Recovery-shape validation and service-test hardening | Backend |
| Independent catalog-sale kill switch | Platform/Operations/Security deployment control |
| Retired-object audit projection | Catalog-order/Operations/Audit |
| Route/problem-writer/contract maintainability | Backend/API-governance/Catalog-order |
| Future `snapshot_sha256` canonical binding | future draft-write/API-governance slice |
| Unreachable 429 producer | API governance |
| Lock-free authority race, commit-then-cleanup stale retry, slow-client/unread body, TOCTOU and forward/include behavior | Platform/Operations/Backend/Security as recorded in Control |
| Later API/business slices | API governance + Catalog-order |

No residual is unowned or unclassified.

## Pull request

- Draft PR #49: `https://github.com/zh94695424/one-call/pull/49`
- Base: `main`
- Head branch: `codex/pa38-next-work-unit`
- Accepted PR-review head: `07c7b056e3433ec7b0dbe93340a8311672a8c008`
- PR state at acceptance: OPEN, Draft, MERGEABLE, CLEAN
- No merge, approval, mark-ready action, reviewer request, external comment or
  issue mutation was performed.

## Next entry point

After the user merges PR #49, fetch the updated `origin/main`, create a fresh
managed worktree and `codex/` branch from the merge commit, preserve older
worktrees/private files, reread Control and applicable design sources, and run a
fresh Phaseflow goal confirmation for the next bounded work unit. No next work
unit scope is defined or authorized by PA38.
