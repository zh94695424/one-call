# PA14 PR #22 PR-Review Fix 1 Controller Validation

- Work unit: `PA14-sharing-command-service-foundation` (backend feature).
- Gate: PR Review -> fix -> re-review.
- PR: `https://github.com/zh94695424/one-call/pull/22`.
- Base: `259aeb458883c484e15077c3f7a157ef521e2203`.
- Initial reviewed head: `b18278bd1958690609f54bebc4fac516bfa6124d`.
- Fix task: `pa14-pr22-review-fix1-gpt-20261105`.
- Date: 2026-11-05 (Asia/Shanghai).
- Status: `fix-complete`; corrected candidate awaits fresh independent PR re-review.

## Initial PR review intake

The initial mandatory A/B review used byte-identical snapshots:

- Freeze: `C:\Users\Administrator\.codex\worktrees\pa14-sharing-command-service-foundation\pa14-pr22-review-freeze-20261105-090000`.
- PR diff SHA256: `837727ffca73522f5ef9e36a55718cf4a0edd18e7b3090e394892d70e912d329`.
- Manifest SHA256: `3f9538568a8087a88e5ba0e220854c96d8b9ddfaeb3716c8abd1eee15e66b1d4`.
- Changed paths: 86.
- A: Claude runner / DeepSeek, valid JSON `success` / `is_error=false` / `end_turn`, exit 0, empty stderr. Artifact `docs/reviews/code-review-20261105-095233.md`, SHA256 `467E8D3871433083F2561E1C488740A6C1907D6C4CB2CEA5D6567373984A3917`, verdict pass with one medium finding.
- B: Claude runner / MiMo 26 Flash, valid JSON `success` / `is_error=false` / `end_turn`, exit 0, empty stderr. Artifact `docs/reviews/code-review-20261105-100748.md`, SHA256 `29AD4DDFEF074BD19360C95CB44A8D1B9524F861B7DB957360D3398D1A6C9968`, verdict pass with zero material findings.
- Both reviewers submitted before the Controller read either conclusion; their prior source snapshots contained no peer artifact.

## Finding adjudication

Reviewer A's medium finding is `accepted`.

Frozen `docs/14-production-data-model-and-flyway-schema-spec.md` section 2.12 requires:

- `scopeDigest` over canonical `{content, verification}`.
- `content_sha256` over the complete persisted `{content, verification, creation}`.

V009 persists exactly those three members. The initial implementation, unit oracle, container read-back oracle, competing-grant helper and accepted plan used different wrapper names (`contentObject`, `verificationObject`, `creationObject`), so an independent verifier following the frozen schema would compute a different durable hash. This is a real contract/auditability defect before any production rows exist. Reviewer B's zero-finding conclusion does not override the direct source evidence.

## Accepted fix

The fix changes exactly these five paths:

1. `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java`
2. `backend/src/test/java/com/onecall/archtests/sharing/CustomerShareCommandServiceTests.java`
3. `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java`
4. `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`
5. `docs/gateflow/pa14-sharing-command-service-foundation-pr-review-fix-1.md`

The service now computes `scopeDigest` from the persisted `content`/`verification` members and `content_sha256` directly from the complete persisted snapshot. Test oracles independently use the same persisted member names. The plan's three contradictory formula passages now state one contract matching frozen docs/14. No migration, schema, OpenAPI, production provider, error mapping, lock, transaction or future-slice behavior changed.

Current SHA256:

- Service: `29ea9521e4ebf364bc6f358767082b55c59f022243026a2d659513882f15e018`.
- Unit test: `63744290f167d5b072d62c9abccae22d9c53228bdf0c6b31207bbd5d5f64db5b`.
- Container IT: `cc6c23485b47432efb51489752fa5e81bd8fe64a2c504caeaac547ee52427fd7`.
- Plan: `7c94510b4f8358ae79d8b73cde8f9aebf45a0476845eb1b059d83bbb15de5dee`.
- Author fix artifact: `ce4ec05cf4f83677e52a735df51a6c395c6256ea543ea99ff9e701e592ec4902`.

## Controller validation

- `git diff --check`: pass.
- Changed tracked source/test/plan scope before adding Controller/reviewer evidence: exactly the four intended paths; no wrapper member names remain.
- Targeted Surefire `CustomerShareCommandServiceTests`: 4 tests, 0 failures, 0 errors, 0 skipped.
- Focused Failsafe `CustomerShareCommandsIT`: 8 tests, 0 failures, 0 errors, 0 skipped on PostgreSQL 16.15 with Flyway applying V001-V010 through v010.
- Source/CI inventory remains 16 Surefire classes / 77 methods and 6 Failsafe classes / 108 methods, 185 combined.
- The focused PostgreSQL run used the installed Docker Desktop Docker socket from WSL and disabled only Testcontainers Ryuk for this invocation because Ryuk could not mount the Desktop guest socket. The test fixture stopped its PostgreSQL container and no PostgreSQL container remained; no product test was skipped or disabled.
- A full local `mvnw.cmd -Pcontainer-smoke verify` attempt reached 77 Surefire methods but was stopped by the execution host's AF_UNIX loopback sandbox: 56 methods passed and 21 pre-existing web/socket tests failed with `Unable to establish loopback connection`; Failsafe was not reached in that attempt. Those 21 are environment errors, not claims of product success. The changed service tests passed within that run.
- The author's artifact records `needs-more-evidence` from its restricted runner. Controller focused PostgreSQL evidence above closes that evidence gap for this fix. Remote CI on the corrected pushed commit remains required later in the PR gate.

## Residual risks

- The existing gateway/security request-body limit, evidence projection semantics, design-doc snapshot naming ambiguity and production provider/policy inputs retain their prior owners and destinations. None is changed or newly admitted by this fix.
- The corrected candidate is not yet accepted: two fresh independent reviewers must review the same frozen corrected diff without reading each other's conclusion before submission.
- The corrected local candidate is not yet pushed. PR head remains the initial `b18278b` until the fixed candidate passes PR re-review and receives the accepted PR review checkpoint.

No merge, GitHub approval, ready-for-review action, reviewer request or external comment is authorized or performed.
