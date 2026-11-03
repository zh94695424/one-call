# PA13 sharing-role least-privilege plan-fix-4

- Task label: `pa13-sharing-role-least-privilege-plan-fix-4-retry-gpt-20261103-212527`
- Work unit: `PA13-sharing-role-least-privilege`
- Gate: `fix`, entry `plan-fix-4`
- Status: `plan-fix-4-retry-complete`
- Current round: `plan-fix-4` justified same-provider completion retry; prior task `pa13-sharing-role-least-privilege-plan-fix-4-gpt-20261103-211832` reported incomplete work.
- Date: 2026-11-03
- Active plan: `docs/gateflow/pa13-sharing-role-least-privilege-plan.md`
- Scope: accepted bounded corrections only; goal, exact role/ACL semantics, migration history, and all other binding inventories preserved.

## Corrections recorded

- Added `backend/README.md:308` and `:326` to the exhaustive current-state documentation delta, with required post-V010 historical/current wording; retained all previously named README sites. `docs/README.md:121` was checked and recorded as a verified non-site because its statements remain true after V010.
- Added `PostgresContainerIT.assertNoV008PlusObjects` to the pre-V010 role-absence fence list, alongside `assertNoV009PlusObjects` and `stagedV007SharingRolesAbsent`; the absence assertion covers `sharing_command_role`.
- Pinned the standalone `SharingRoleLeastPrivilegeIT` bootstrap administrator to `fixture_admin` with a per-run password. The retained bootstrap-grantor allow-list is exactly `onecall` for `PostgresContainerIT` and exactly `fixture_admin` for this standalone IT, not an interchangeable pair.
- Assigned the entire `PostgresContainerIT.v009OwnerPublicRuntimeAndCrossModuleAclsAreExact` body, lines 3066-3131, to `sharing_command_role`, including grant-row `FOR UPDATE`, positive cross-module reads, and `FOR UPDATE`/`FOR SHARE` denial probes. Retained query-role evidence is the exact query ACL/catalog matrix, untouched production query, and `/me` and `/shares` ITs—not that V009 behavior block.
- Made the enumerated per-fixture allowed-grantor set the sole prescribed exact grantor comparison. A normalized effective ACL is supplementary only.
- Refreshed the plan header for this fix round; previous hashes are explicitly historical, not current-plan identity.
- Fixed docs/14 wave terminology: V010 is Flyway history/current inventory entry ten and a privilege-only incremental migration outside the nine frozen structural waves. Required locations are §1, §2.12, §11 rollout, and T-S3 traceability; V001-V009 history remains unchanged.

## Validation boundary

- Cross-checked requested source locations, including backend README lines 308/326, the `assertNoV008PlusObjects` role query, the V009 behavior method, and `docs/README.md:121`.
- Retry validation checks all nine requested corrections in the final plan, including both standalone fixture descriptions, the earlier `F-A-01/B2-08` line, sole exact grantor assertion, all four docs/14 wave-wording sites, and removal of generic admin-credential wording.
- Plan correction is documentation-only. No source, migration, test, POM, CI, README, docs/14, control, review, fix-2, or fix-3 artifact was modified.
- Git status/diff validation is blocked by the existing `.git` pointer to an unavailable Windows path; no Git pass is claimed. Only the two allowed documents are patch targets.
- No independent re-review was performed or requested in this gate. Runtime/container validation is outside this documentation correction.
- Residual risk: downstream implementation must preserve every frozen ACL, SQLSTATE, role-matrix, migration-byte, and test-inventory constraint in the active plan.

## Hashes

Plan digests are computed after the final plan edit. Authoring files use LF: canonical-LF also equals their actual raw-byte digest. The raw-CRLF label identifies the review/archive serialization obtained by replacing canonical LF with CRLF, not the raw LF worktree bytes. Historical identities in the plan remain explicitly historical.

- Final plan raw-CRLF SHA-256: `81CADB23DDEBFDDB17827C58282A94507B883212AA122B9F3BA0CDDF4C34B816`.
- Final plan canonical-LF SHA-256: `02731EAD18571FE5D8153FFF3B15A73BB9A09E8883FB145936505BDB0DEC60EA`.
- Fix-4 artifact final raw-CRLF and canonical-LF/actual raw-byte SHA-256 digests are reported in the runner completion report after its final edit; embedding its own post-edit digest here would be self-referential.
