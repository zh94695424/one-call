# PA40B-S1 bounded code-review fix 1

- Task: `pa40b-code-review-fix1-gpt-20261124-02`; date: 2026-11-24.
- Gate: `code review fix` for sole slice `PA40B-S1`. Stops before independent re-review; no approval/readiness claim.
- Workspace identity confirmed using explicit `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call` and current `GIT_WORK_TREE`: HEAD `2f8490f9015fde5a0e292afd04881c0ecfb87565`; frozen merge base `32cf930a16803deb6381615e197c02e73ac5c22e`. Worked directly on the expected dirty implementation candidate, not either review snapshot.
- Binding inputs: current Control PA40B checkpoint, accepted plan, implementation report, reviews `code-review-20261124-150658.md` and `code-review-20261124-151308.md`. This is the fresh retry after the no-edit false dirty-worktree blocker.

## Findings fixed

- `PA40B-CR-B-02` (accepted Low): README now documents account/workspace/scope/endpoint/sort replay as `CURSOR_INVALID` (400); only normalized channel/audience/offerType change after valid scope/MAC is `CURSOR_FILTER_MISMATCH` (400). No-secret/no-default/fail-closed behavior and all other wording, including lazy dependency failure behavior, are preserved. Historical body and the four existing backtick Java source-path references are unchanged.
- `PA40B-CR-B-04` (accepted Low): the existing frozen-input test reads actual bytes and hashes them directly. It retains all 21 canonical LF hashes, checks canonical LF content, and limits raw bytes to the canonical LF or canonical CRLF representation, following the raw/LF dual guard precedent. The existing test method accepts in-memory LF and CRLF, rejects CR-only, and retains and strengthens the one-byte `X` prefix negative probe for every input. No test method/class/identity or frozen-file/hash additions.

## Exact write set

1. `backend/README.md` — only the cursor-error sentence corrected.
2. `backend/src/test/java/com/onecall/archtests/architecture/CustomerCatalogOfferBoundaryTests.java` — raw-byte hashing/representation guard and existing-method positive/negative probes.
3. `backend/src/test/java/com/onecall/archtests/architecture/OperationsIvrSessionReadBoundaryTests.java` — only the two live README pin slots and C27 digest changed.
4. `backend/src/test/java/com/onecall/archtests/container/OperationsIvrSessionReadsIT.java` — only the same two live README pin slots and C27 digest changed.
5. `docs/gateflow/pa40b-customer-catalog-offer-read-implementation.md` — exact line/byte/SHA256 rows refreshed for all four changed candidate paths, current README/C27 digests and finding closures/validation supplement.
6. `docs/gateflow/pa40b-customer-catalog-offer-read-code-review-fix-1.md` — this artifact.

No production code, migration, OpenAPI, docs12/14/15/17, PA25 SQL/runbook, POM, CI, other tests, accepted plan/reviews, Control or private helper was edited. All historical26/F26/H26/raw/LF/deletion-mode pins and IVR negative probes remain unchanged.

## Current digests

- README raw checkout: 591 newline-delimited lines, 125920 bytes, SHA256 `d8065005190dfecc1bc4809f936777927e3581c29a346cf709a5dcef37d912ee`.
- README canonical LF / both current-pin slots in each IVR verifier: `95e6e5e042792ddd5878817f4536b5901fc74749a0bd49095ec2161c49a2f70d`.
- C27 aggregate in both verifiers: `738652e487145167e32c07fc315ed0bb2dbdf4f4f4e2295712eb11a4b57cb75e`.
- Strengthened boundary test: 135 lines, 11232 bytes, SHA256 `04a13dc2e65f6ae2cae04d41d45b5fdd9031afaa675360bfb4e6ae805b21dad1`.

## Validation

- Tools: Linux `/opt/jdk21` (21.0.13), `/opt/maven-3.9.9`; offline Maven cache sufficed. Default PATH Java points at a Windows executable unavailable under this sandbox, so Linux tools were selected without changing configuration.
- `git diff --check`: PASS (checkout line-ending warnings only).
- `mvn -o -B -ntp -f backend/pom.xml -Dtest=CustomerCatalogOfferBoundaryTests,OperationsIvrSessionReadBoundaryTests test`: final PASS, 5 + 8 = 13 methods, zero failures/errors/skips. Log: `/tmp/pa40b-fix1-02/surefire-final.log`. An initial overlapping run caught the unrecomputed README pin; final rerun compiled and tested the updated pins.
- The frozen-input method's passing assertions prove canonical LF/CRLF acceptance and CR-only/one-byte-content rejection for all 21 inputs, using the same guard as actual file bytes. All 19 migrations and both OpenAPI inputs remain frozen.
- `mvn -o -B -ntp -f backend/pom.xml -Pcontainer-smoke -Dit.test=OperationsIvrSessionReadsIT failsafe:integration-test failsafe:verify`: BLOCKED before PostgreSQL16 startup in `@BeforeAll`, Docker socket `Operation not permitted`; one setup error, not a test assertion failure. Log: `/tmp/pa40b-fix1-02/failsafe.log`. No sandbox bypass or fixture/test change attempted.
- Extracted the complete Python ledger verbatim from current `.github/workflows/ci.yml` to `/tmp/pa40b-fix1-02/ci-ledger.py`. Against current repository reports after the blocked Failsafe attempt it FAILS `wrong aggregate test inventory`; log `/tmp/pa40b-fix1-02/ledger-current.log`. This limitation is not hidden by restoring old XML.
- Preserved Failsafe reports before attempting the rerun. In a separate `/tmp/pa40b-fix1-02/ledger-evidence` projection, the unmodified ledger uses current source/POM/current Surefire plus those explicitly historical Failsafe reports: PASS 110/1071 + 42/701 = 152/1772, scan 1773, POM 42, zero historical report failures/errors/skips, intact complete historical ladder through PA40A 1018/666 and PA40B 1026/682. Log: `/tmp/pa40b-fix1-02/ledger-preserved-evidence.log`. This proves inventory continuity, not a fresh post-fix integration run.
- Final status/byte audit compares this task's initial dirty candidate rather than HEAD/base: only the authorized six paths change; the two pre-existing review artifacts and excluded private `.pa40b-*` / `.codex` helpers remain pre-existing. No unexpected new repository path.

## Residual risks and stop

- Required fresh PostgreSQL16 IVR validation and a green ledger on current reports remain blocked by sandbox Docker access. Backend/Test-CI owner must rerun the focused Failsafe guard and extracted ledger in a Docker-capable environment; no current full-suite or hosted-green claim.
- TRACE 500 versus planned 404 stays deferred to separate Backend/API boundary maintenance. The plan-manifest omission stays deferred to Phaseflow/Test-CI plan maintenance. Trusted channel/audience derivation remains unchanged because the service-side backstop is required by the accepted plan.
- Existing production custody/rotation, hosted CI billing, non-CNY fail-closed behavior and other implementation/review residual risks remain unchanged.
- No staging, commit, push, PR, merge, approval, mark-ready, external reviewer dispatch/comment or re-review. Stop after this artifact and the implementation-report update.
