# PA32 slice 1 Controller validation

## Candidate and scope

- Accepted plan commit / review HEAD: `d12e53493f12f3a61991c7fe13be77a5323fab86`.
- Reviewed product base: `8206f2a8f8a40cd58793efdda55aa41843af5d5f`.
- Implementation candidate: exactly the accepted 36-path allowlist plus the S1 implementation, validation-fix-1 and validation-fix-2 gate artifacts.
- Pre-existing private `.codex` remains untracked and excluded from review and commits.
- Production readiness and hosted CI remain **NOT CLAIMED**.

## Implementation fixes

Validation-fix-1 changed only two allowlisted test/fixture files and added its report. It retained the SQL cause from the fixture's aborted restoration instrumentation so the production mapper classifies the already-specified technical failure as sanitized 503, and initialized only PA32's owned PostgreSQL container as UTF8 while retaining C collation. Production code did not change.

Validation-fix-2 changed one existing test assertion from stale customer `CONSENT_REQUIRED` to the frozen PA31 result for a fresh ACCEPT after Operations has suspended the subject: `FORBIDDEN`. No production, test count or inventory changed.

## Focused PostgreSQL16

- Toolchain: Temurin `21.0.13+9`, Maven wrapper `3.9.9`, Docker `29.1.3`, PostgreSQL `16.15`.
- Initial focused run: Surefire `78/78`; Failsafe `70` executed with two failures.
- After fix-1: Surefire `78/78`; Failsafe `69/70`; the prior failures were closed and the remaining assertion was the stale race expectation.
- Final focused run after fix-2: **Surefire 78/78, Failsafe 70/70, zero failures/errors/skips**.
- Each started container validated and applied exactly V001-V017 through V017. The PA32 fixture asserted `server_encoding=UTF8`.
- Evidence: `/tmp/pa32-s1-controller-validation/focused-pass` and `/tmp/pa32-s1-controller-validation/focused-controller.log` for the pass immediately before fix-2; the final focused pass is in the terminal/run logs for Controller session `22096`.

## Full clean acceptance

`./mvnw -B -ntp -DargLine=-javaagent:<pinned-byte-buddy> -Pcontainer-smoke clean verify` completed:

- Surefire: **64 classes / 574 methods, 0 failures, 0 errors, 0 skipped**.
- Failsafe: **27 classes / 440 methods, 0 failures, 0 errors, 0 skipped**.
- Total: **91 classes / 1014 methods**.
- Build result: **SUCCESS**, 26 minutes 37 seconds.
- Evidence: `/tmp/pa32-s1-controller-validation/full-clean.log`.

The extracted checked-in CI inventory step then passed against the fresh full XML:

`PA32 exact source=CI=XML inventory passed: 64/574 + 27/440 = 91/1014; annotation1015; POM27; zero skips/errors/failures`.

## Packaged and contract checks

- Checked-in default/opt-in web JAR smoke passed, including both Operations endpoint 401/503/404 negative probes and no DB/provider adapter startup.
- Checked-in production dependency firewall passed: baseline `37/34`, ssm `72/65`, counterparts `64`, dependency-only `8`, packager `1`, 116 adversarial probes, and the production employee-verifier implementation ban.
- OpenAPI A1 `check:s3` passed with 49 exact operations and no doc-map drift.
- A3 security `s3` passed with 23 fixtures and zero drift.
- A4 capacity `s2` passed with 113 fixtures/471 non-finite probes and no acceptance overclaim.
- The Windows checkout uses CRLF; exact local A2/A5 document parsing therefore reported line-ending artifacts. A2 against an LF-equivalent temporary copy of the unchanged docs/14 passed with all A0 drift zero; A5 s1/s2 against LF-equivalent temporary copies of unchanged docs/14 and docs/19 both passed with matrix drift zero. No frozen document or validator was edited. Hosted Linux CI remains the authoritative CRLF-independent execution.

## Frozen boundaries and inventory

- OpenAPI, docs/12, docs/14, docs/15, docs/17 and V001-V017 retain their accepted raw-byte hashes.
- Migration inventory remains exactly 17 files with no V018.
- The `36` implementation paths remain `22` new production + `9` new test/fixture + `5` changed.
- No new dependency, grant, role, schema, migration, public error or OpenAPI change was introduced.
- `git diff --check` passes; review scope excludes `.codex`, `target`, `node_modules`, `tmp`, generated OpenAPI clients and temporary validation helpers.

## Code-review fix revalidation

Independent code review accepted two Low test-evidence gaps: reverse-order employee-revocation serialization and deterministic reuse of the failed physical pool backend. Bounded fix `pa32-s1-code-review-fix1-gpt-20261114-01` changed only the two existing PA32 IT test bodies and added its fix artifact; production code and all 90 PA32 test identities remained unchanged.

The fixed candidate was rerun from a clean target:

- Focused PostgreSQL16: **78/78 Surefire + 70/70 Failsafe**, zero failures/errors/skips.
- Full `clean -Pcontainer-smoke verify`: **574/574 + 440/440 = 91/1014**, zero failures/errors/skips, build `SUCCESS` in 25 minutes 59 seconds.
- Checked-in source=CI=POM=XML inspection passed again with `64/574 + 27/440`, annotation `1015` and `27` Failsafe includes.
- The reverse-order test observed the real disable backend waiting behind the Operations locks; the failed-restoration test asserted reuse of the exact failed backend PID and restored normal pool capacity.

These checks close the two accepted code-review evidence findings. Fresh independent A/B re-review of the fixed cumulative diff remains required before the slice can be accepted.

## Residual risks and next gate

The production employee-session provider/issuer/MFA/Q1, production credentials and same-login SET ROLE provisioning, multi-replica operation and live acceptance remain external deployment work. Hosted CI on a pushed PR head is not run. An audit snapshot naming recheck and real concurrency/rollback coverage are included in the accepted implementation tests, but broader production fault injection and response-delivery-loss semantics remain Backend/Security/Operations risks.

This evidence supports moving to fresh independent A/B code review against one frozen 36-path candidate. It does not itself accept the implementation slice and does not authorize push, PR, merge, approval, mark-ready, reviewer request or external comment.
