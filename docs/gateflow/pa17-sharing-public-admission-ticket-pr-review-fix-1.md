# PA17 Sharing Public Admission Ticket — PR Review Fix 1

## Finding and correction

Hosted CI run `42308771587` at PR head `819e8645c0c0a10bbcf141a6e4f7961c22278006` failed only `SharingAdmissionRoleLeastPrivilegeIT.v001ThroughV011BytesAndAclSnapshotsRemainUnchanged`. The expected SHA-256 pins represented Windows working-tree CRLF bytes, whereas CI checked out canonical LF content. The test now reads each migration as bytes, converts only each `0D 0A` pair to `0A`, and hashes the result. The eleven expected values were replaced with the supplied canonical LF hashes. All database/ACL assertions remain unchanged, the map remains V001–V011 with `hasSize(11)`, and the class remains at 8 `@Test` methods.

## Integrity and validation

- Migration files V001–V011 were not edited. The conversion skips only the CR byte when it immediately precedes LF, preserving all other bytes.
- Canonical LF hashes for all eleven current migration files match the expected pins; the map contains 11 entries and the IT has 8 `@Test` methods.
- The helper’s byte loop changes a byte only for a `0D 0A` pair: it skips the CR and writes the LF; every other input byte is written unchanged. Migration files were not among the three paths edited in this bounded task.
- Docker is unavailable (`docker info` cannot access `/var/run/docker.sock`), so the requested targeted Maven integration test and full-suite runtime validation were not run.
- Git checks, including `git diff --check` and an independently verified changed-path listing, could not run: this worktree’s `.git` file references unavailable `D:/codex-call/.git/worktrees/codex-call17` metadata.

## Risks and status

Residual risk is limited to the test-side normalization helper and its scoped use in this snapshot check; it intentionally normalizes CRLF but not lone CR bytes. No production code or migration behavior changed. Completion status: bounded PR review fix implemented and static hash/method-count checks passed; runtime and Git-integrity checks are unavailable in this environment. No claim is made here about Controller runtime.
