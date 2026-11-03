# PA13 sharing-role least-privilege plan fix 2

- Task label: `pa13-sharing-role-least-privilege-plan-fix2-retry-gpt-20261103-194127`
- Work unit: `PA13-sharing-role-least-privilege`
- Gate: `fix`, entry `plan-fix-2`
- Status: `plan-fix-2-retry-complete`
- Date: 2026-11-03
- Frozen input identity from the control: revised plan raw-CRLF SHA-256 `32569007C927009AD094D8A4E560B38A9000B1582FFB8530E7BD6A1AE7E0E51A`, canonical-LF SHA-256 `253953C6A5063B2F147BA01A9AB742A419FCB6DD648C1F6AC19F4D5096B909C5`; fix-1 raw-CRLF SHA-256 `38A4F22900B10E2F2CCA7F5B332555464622DF3034531CA15676FCE68C8D0840`, canonical-LF SHA-256 `5671B82D08A2F71B6B37AAF73E75B34859F0D58BFCAA5BAF4E3CA7C0D081527E`. These identify prior inputs only; no prior history is rewritten.

## Accepted finding disposition

All accepted Reviewer A findings A-01..A-04 and Reviewer B findings R-01..R-09 are closed by explicit plan corrections. The goal and direction remain unchanged: V001-V009 byte-identical; one forward-only V010; read-only creator-scoped `sharing_role`; separate unwired `sharing_command_role`; no source/API/OpenAPI behavior changes, view, or SECURITY DEFINER.

| Findings | Correction in revised plan |
| --- | --- |
| A-01, R-03 | `PostgresContainerIT.initialMigration.migrationsExecuted` changes from 1 to 2; do not add an extra target-009 stage. |
| A-02, R-04 | Existing read-only query probes remain `25006`; separate writable, savepoint-isolated probes assert `42501`. State PostgreSQL read-only check precedence; retain the independent authorized read-only proof at `25006`. |
| A-03, R-06 | Update docs/14 §2.12 and cited current-state sites 45/51/243/4773/4855; README sites 15/41/185/304/305/312/321/322; all other plan §3/§4/§10, rollout and traceability sites; CI inventory sites. Mark PA9 counts historical. |
| A-04 | Catalog ACL enumeration expands `coalesce(acl, acldefault(...))` for namespaces, relations, attributes, and procedures so NULL ACLs cannot yield vacuous results. |
| R-01 | Move future-write probes in `v009OwnerPublicRuntimeAndCrossModuleAclsAreExact`, `v009ForeignKeyAndRestrictDeleteBackstops`, `v009ShapeDefaultsAndAppendOnlyBackstops`, `v009GrantLifecycleRevisionMatrixAndImmutabilityBackstops`, `v009AdmissionCountOnceLimitAndAtomicRollbackBackstops`, and `concurrentV009AdmissionIdentityLimitAndLifecycleHasOneWinner`, plus the complete role-carrying helper tree, to `sharing_command_role`: `v009Fixture`, `v009S3Configure`, `v009S2Defaults`, role-sensitive `v009S2KeysAndNulls`, `v009S2Mutation`, `v009S2FrozenColumns`, and relevant callers. |
| R-02 | `assertV009AclCatalog` closed-world grantee guard includes `sharing_command_role`; PUBLIC, grant-option, and undeclared-grantee counts remain zero. Retain or replace membership evidence with the new IT. |
| R-05 | Revoke V009 lines 5-7 schema grants as migration superuser before owner blocks; pin deterministic grantor handling to V010-created grants and explicitly allowed bootstrap identities. |
| R-07 | Standalone `GenericContainer("postgres:16")`, per-run admin credentials, no fixture reuse/file, two synthetic logins provisioned in `@BeforeAll`, exactly eight plain `@Test void` methods. |
| R-08 | Label every digest raw-CRLF or canonical-LF; record both for frozen files. |
| R-09 | Require pre-edit V001-V009 raw-byte and raw-CRLF/canonical-LF manifest, compare bytes/digests before completion, use LF for V010, and qualify `git diff --check` for new-file/CRLF behavior. |
| Inventory cross-cutting | CI `container_expected`, source/CI sets, 92→100 Failsafe, 159→167 combined, printed summary; retain 64-name guard and 67 Surefire; Flyway 10. |

## Files and scope

- Revised only `docs/gateflow/pa13-sharing-role-least-privilege-plan.md` and created this fix artifact.
- No source, migration, test, POM, CI, README, docs/14, control, review, Git metadata, or private helper file was modified. No implementation, re-review dispatch, commit, stage, push, PR action, approval, or external comment was performed.

## Validation

- Cross-checked the binding re-review adjudication, both formal re-review artifacts, fix-1, active checkpoint, and directly relevant source/test/migration/docs/CI inventory called out by the accepted findings.
- Arithmetic: the existing 64 `PostgresContainerIT` methods remain unchanged; exactly eight new `SharingRoleLeastPrivilegeIT` methods are added. Existing Failsafe classes total `64 + 10 + 10 + 8 = 92`, and with the new IT total `64 + 10 + 10 + 8 + 8 = 100`. Surefire remains 67; combined total is 167; Flyway history is 10 after V010.
- Direct file inspection and the following digest commands verify the two allowed outputs. Raw-CRLF hashes are over exact on-disk bytes; canonical-LF hashes normalize CRLF to LF before hashing. No Git status claim is made because this worktree may not resolve its `.git` indirection.

- Hash-label erratum (corrected in plan-fix-3): review-snapshot plan raw-CRLF SHA-256 `5A7E8BA2E21C6CC02A790A251421C20BE391957B4087651CD2AF66E309F393EB`, canonical-LF SHA-256 `F4B9C39EC92B85A8D41601EA3139B7FFA8119DCDF5A521F1ECADB0760CC1CEA5`; fix-2 raw-CRLF SHA-256 `A032A8A394DC1BB46972CDE145DF0763F4A5682D72FF7260C507C20735B7BB16`, canonical-LF SHA-256 `17FE383F59BD27438C469AF577F4CE0463E4DB0B8AB2BCC1CE66E48C66E6BDE2`. Review snapshot/archive bytes are CRLF while the authoring worktree files were LF; historical fix-2 evidence is preserved, not rewritten.
- Repository source spot-checks confirmed `initialMigration` at `PostgresContainerIT.java:1236` currently expects 1; all six named V009 methods and role-sensitive helpers exist; existing sharing fixtures assert `25006` in read-only transactions; therefore the plan prescribes an increment to 2 and a distinct writable `42501` probe rather than an expected-code-only edit.
- Validation limits: Docker/PostgreSQL/Maven suites were not run because this is a plan-only gate. The existing source/test/migration/docs/CI files were not changed. Git status/diff was not used as scope evidence; direct write actions targeted only the two allowed artifacts. `git diff --check` was not run because no repository edits other than these documentation artifacts are authorized and this runner may not resolve `.git`.
