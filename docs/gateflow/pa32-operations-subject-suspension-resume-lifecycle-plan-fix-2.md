# PA32 bounded plan fix 2

- Task label: `pa32-operations-lifecycle-plan-fix2-gpt-20261113-01`.
- Runtime/provider (dispatch metadata): Codex / GPT `gpt-6.1-sol`, reasoning `high`.
- Date: 2026-11-13, Asia/Shanghai. Gate: **fix**, documentation only.
- Base HEAD verified read-only: `8206f2a8f8a40cd58793efdda55aa41843af5d5f`.
- Prior cumulative-plan SHA256: `eb39d37f83b3d53fa4a950645815d59e63d03d7e12c33023d904bb0b7b6a1f97`.
- Prior fix-1 SHA256 (unchanged): `32159bbd4580ad8599eecf0c65e80a052b86877d5907008fbb0caf71c984abb0`.
- Binding authority: PA32 Goal Confirmation text and Controller adjudication in `docs/gateflow/project-execution-control.md`. Control hashes remain historical provenance, never a mutable-byte authority; fix-1 is historical evidence, not the current failure-mapping instruction.
- Authorized output: modify the cumulative plan and add this fix-2 artifact only. No source/test/fixture/inventory addition is necessary. The gate artifact is not an implementation path.

## 1. Controller dispositions and exact sections

| Controller finding | Fix-2 disposition, pending Controller acceptance | Cumulative-plan sections changed |
| --- | --- | --- |
| PA32-PLAN-REV-03 completion + PA32-PLAN-REV-06 (Medium) | **Corrected.** One authoritative condition table assigns transaction/role/connection/timeout/dependency unavailability to `UNAVAILABLE`, and malformed/unusable employee row/facts/identity or unknown status/role/revision shape to `INTEGRITY`. Both typed kinds map to lifecycle `SERVICE_UNAVAILABLE`/503. Unknown programming defects outside that typed contract map to `INTERNAL`/500. Valid missing/disabled/ADMIN cases retain401/403. Redacted throwable carriers, rollback-before-translation and controller sanitization remain mandatory. | §§5.1, 5.2, 6, 7.3; §8.3 tests3/12/17; §8.5 tests1/6 |
| PA32-PLAN-REV-07 (Low) | **Corrected.** The existing owner class has exactly `@Component("employeeAdministratorLock")`, persistence profile and existing property condition. Tests assert the exact name and absence of the implementation-derived default name. Manager/DSL constructor qualifiers stay exact. Owner selection deliberately counts all published-type candidates, without a bean-name qualifier or `@Primary` preference hiding ambiguity. | §§4, 6; §8.4 test5 |
| PA32-PLAN-REV-08 (Low) | **Corrected.** Missing/ambiguous employee-lock collaborators do not fail startup. Owner construction only retains `ObjectProvider<EmployeeAdministratorLock>`; each command resolves `stream().limit(2)` before transaction/locks. Zero/two candidates or lazy resolution failure become redacted `UNAVAILABLE` then per-call503 with no writes. Existing configuration test6 must assert successful context refresh/owner creation and subsequent sanitized503, not accept startup failure instead. Credential/unrelated configuration startup failures stay separately redacted. | §§6, 7.3; §8.4 tests5/6/8 |

The existing named malformed-facts test now explicitly asserts `INTEGRITY` => `SERVICE_UNAVAILABLE`/503, never500. Existing service and boundary assertions cover both kinds, unknown-defect500, rollback after handoff and no raw throwable/message/input/SQL serialization. No method, class, test file or inventory entry was added or renamed.

## 2. Nonblocking consistency clarifications

- **Cross-subject same-key outcome:** §7.3 and §8.6 test18/concurrency instructions require one200/one409 and only one committed transition/Outbox/audit set. Global action lookup can detect the committed winner; the global unique constraint remains a backstop, not a required exception mechanism. Existing constraints and global lookup are unchanged.
- **Changed actor:** §8.6 test7 and §8.8 require two distinct ACTIVE OPS fixture employees with independent verified opaque tokens, plus the ADMIN fixture. The second actor must pass authorization so409 actually tests fingerprint conflict. No fixture path/method is added.
- **Audit key evidence:** §7.4 records docs/17 §17 and `security/validate.mjs`'s exact audit-access projection keys: `status`, `revision`, `reasonCode`. Their high-risk metadata says `status/revision/reason`; the validator returns that descriptive `beforeAfter` string and does not validate literal JSON snapshot keys. The existing planned snapshots remain exactly `{status, revision, reasonCode}` and SQL `p_reason_code` is separate. No new claim that the validator freezes a literal snapshot schema; Backend/Security must reverify the mapping at implementation time without policy edits.
- **Task bookkeeping:** §§12–13 now accurately identify fix-2's two-artifact write scope, validations, review-input limitation and Controller stop point.

## 3. Preservation proofs

| Closed finding / frozen boundary | Proof of preservation |
| --- | --- |
| REV-01 | §3 remains byte-identical: preserve `containsExactlyInAnyOrder`, add `operations_iam` exactly once, use the exactly-eight description and exact target multiset `sharing, evidence, subject_number, activation_consent, evidence, catalog_order, entitlement_metering, operations_iam`. §4's existing assertion instruction and nine-method preservation are unchanged. Actual `ModuleBoundaryTests` still has nine methods; no source edit. |
| REV-02 | Operations-IAM-owned persistence component remains in the same allowlisted class. Exact `operationsLifecycleTransactionManager` and `operationsLifecycleDsl` constructor qualifiers, same resource, no extra pool/manager, web-only absence, persistence-only construction and published-interface-only activation imports are preserved. Only bean name and missing/ambiguous resolution timing are made exact. |
| REV-04 | §7.1 is byte-identical: no undeclared/new foreign read or row lock; declared V007-granted nonlocking `subject_number.business_subject.id` read retained; no operations-iam access under activation role. |
| REV-05 | §11 is byte-identical: historical plan-authoring/review-freeze Control hashes remain provenance and Goal Confirmation text remains binding. Control and fix-1 bytes are unchanged. |
| Goal, authorization and no-trial boundary | §§1–3 are byte-identical. Exactly two endpoints, independent opaque employee audience, current ACTIVE OPS, server authority, no customer/IVR lifecycle command, no production provider and no trial/consent/entitlement/foreign mutation expansion. |
| SQL, lock order and state/replay identities | §§7.1–7.2 are byte-identical. Same subject3 → activation4 → declared read5 → employee13 → Outbox14 → audit15 order; same role handoffs, status pairs, revision increment, global action identity, eleven-key fingerprint and stored replay identity. §7.4's executable Outbox/audit mapping is unchanged; only source-naming evidence is appended. |
| Public contracts, schema, policy, privileges | Full before/after regular-file hashes preserve OpenAPI, V001–V017, docs/12/14/15/17, security validator, source, tests, CI and POM. No new error, role/grant, schema/API or goal change. The employee-context correction uses the already frozen503. |
| Slice, allowlist and exact inventory | One implementation slice; same36 path entries in the same order, same22 new production +9 new test/fixture +5 changed paths. Every named §8.1–8.7 method and class/count is unchanged. §9 arithmetic and §10 future validation instructions are byte-identical. |

| Frozen arithmetic | Before fix-2 | After fix-2 |
| --- | ---: | ---: |
| Implementation paths: new production / new test-fixture / changed | 22 / 9 / 5 = 36 | 22 / 9 / 5 = 36 |
| Added Surefire classes / methods | 5 / 54 | 5 / 54 |
| Added Failsafe classes / methods | 2 / 36 | 2 / 36 |
| Target Surefire classes / methods | 64 / 574 | 64 / 574 |
| Target Failsafe classes / methods | 27 / 440 | 27 / 440 |
| Target total classes / methods | 91 / 1014 | 91 / 1014 |
| Target annotation-token lines / Failsafe includes | 1015 / 27 | 1015 / 27 |

## 4. Validations and limitations

- Read binding Goal Confirmation/Controller adjudication first, cumulative plan, fix-1 and all available PA32 plan-review artifacts: `plan-review-20261113-213328.md`, `plan-review-20261113-213705.md`, `plan-review-20261113-220911.md`, `plan-review-20261113-222112.md`. **Only four PA32 plan-review artifacts exist here, not the requested six.** The six earlier same-date artifacts identify PA31. No missing PA32 evidence is fabricated or inferred accepted; Controller owns reconciliation.
- Inspected relevant actual employee/session, lifecycle/repository/configuration, same-connection role-handoff source; architecture assertion; OpenAPI routes; docs/14/15/17; V001/V002/V004/V007 and the migration inventory; security validator projection/high-risk branches; CI literals/inventories and POM. No implementation was entered.
- Recomputed actual test-source baseline as59/520 +25/404 =84/924, retained nine `ModuleBoundaryTests` methods, and verified POM's25 exact Failsafe includes against source. Compared all existing §8.1–8.7 plan method-name lists/counts (8/12/18/8/8/24/12), same36 allowlist paths and unchanged §9 arithmetic. An initial ad-hoc substring counter included annotation-name prefixes such as `@TestConfiguration` (947); corrected it to CI's exact `@Test\b` token rule and confirmed925 annotation-token lines. This is read-only source/plan evidence, not a runtime/XML acceptance result.
- Captured an initial SHA256 manifest of1,956 regular files, excluding `.git` metadata and the preexisting `.codex` entry. Final comparison permits exactly the cumulative-plan modification and this new artifact; no deletion or other byte change. Prior plan/fix-1 hashes above were measured before editing. Frozen-file/Control/review/fix-1 hashes are preserved, not merely assumed from Git's tracked diff.
- Ran read-only `git diff --check` with explicit existing Linux gitdir/worktree, fsmonitor disabled and `--ignore-submodules=all`; it passes. Checked the untracked plan against its saved pre-fix bytes and this artifact against `/dev/null` using `git -C /tmp diff --no-index --check`; no whitespace diagnostics (exit1 denotes differing content). UTF-8/no-BOM/no-CR/no-trailing-whitespace/single-final-LF checks cover both outputs.
- Plain Git discovery fails because `.git` names a Windows gitdir. An initial diff check also hit that diagnostic; explicit gitdir plus ignored submodule discovery resolves it without editing metadata. Initial Controller modification, untracked plans/fix-1/reviews and `.codex` remain preexisting, not attributed to this task. No clean-worktree claim.
- No Maven, Docker, migrations, database queries, installs, build/test execution, container/remote mutation, staging, commit, fetch/pull, PR/reviewer/issue action or re-review dispatch. Spring startup/cardinality behavior, actual rollback/locks, PostgreSQL16/ACL behavior, packaged firewall, XML equality and hosted CI remain future authorized implementation evidence, not results of this documentation fix.

## 5. Residual ownership and completion

- **Backend:** implement the exact condition table, lazy per-call cardinality/no-write behavior and unchanged named assertions; prove both typed failure kinds sanitize to503 and unknown defects remain outside the typed contract. No new file/test method is authorized by these corrections.
- **Backend/Security:** preserve and recheck audit projection/snapshot naming; test changed actor with the second ACTIVE OPS fixture; assert concurrency outcome rather than exception mechanism.
- **Security/Operations IAM deployment and Database/Platform:** production verifier and existing same-login role authority remain external prerequisites. Missing capabilities fail closed; no provider, grant, provisioning or capacity expansion is delivered here.
- **Backend/CI:** later source=CI=POM=XML, PostgreSQL16, JAR/firewall and hosted-run evidence remain mandatory under §10. No test skip/count weakening or runtime success claim.
- **Controller:** reconcile the four-versus-six review-input discrepancy and adjudicate the corrected findings and preservation evidence. If implementation requires schema/API/privilege/goal expansion, stop for authorization rather than extending PA32.
- **Completion:** the bounded documentation fix is complete; REV-03 completion and REV-06..08 are corrected in the cumulative plan, pending Controller acceptance. REV-01/02/04/05 and every frozen count/boundary remain preserved. **Stop for Controller adjudication; do not enter re-review or implementation.** Final output SHA256 values are reported outside the files to avoid self-referential hashes.
