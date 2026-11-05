# PA14 sharing command service foundation — aggregate review fix 5

## Scope

This bounded correction addresses the remaining accepted medium test-evidence finding in `docs/reviews/code-review-20261105-073932.md`. Only existing `CustomerShareCommandsIT` methods and this artifact are in scope. No test method is added, removed, renamed, disabled, or parameterized; no production behavior or frozen contract is changed.

## Evidence Added

The existing PostgreSQL create integration method now issues successful requests with `downloadAllowed=true` and `maxAdmissions=3` for every verification mode. It binds returned state to the request and database row: active status, evidence and verification mode, zero admissions, revision 1, revocation generation 0, requested limits, mode-specific code generation, and the SHA-256 digest of the returned capability token. It checks persisted owner organization values, redacted Outbox identifiers/status/counters/fingerprint/time, and audit reason (`NULL`) plus the exact after-snapshot status/revision. Outbox creation fingerprint is checked against the persisted creation snapshot.

The existing replay integration method verifies the request-derived limit values survive replay, checks token/code providers and durable writes remain unchanged, and asserts changed `downloadAllowed` or `maxAdmissions` under the same creation identity conflicts. The successful create method also covers a non-`ORGANIZATION_MEMBER` create in an organization workspace and verifies the grant retains that owner organization.

## Validation

The workspace Maven wrapper is not modified or executed directly. A CRLF-normalized temporary copy is used with Eclipse Temurin Java 21.0.13 and PostgreSQL 16 Testcontainers.

- Focused PostgreSQL command tests: PASS, `CustomerShareCommandsIT`, 8 tests, zero failures/errors/skips, against PostgreSQL 16.15 using Java 21.0.13.
- Full `clean -Pcontainer-smoke verify`: PASS, 77 Surefire and 108 Failsafe tests (185 combined), zero failures/errors/skips; PostgreSQL 16.15. The final clean run includes all assertions recorded above.
- Exact CI inventory assertion: PASS using the unchanged `assert container smoke actually ran` heredoc from `.github/workflows/ci.yml`; verifies 77/108/185 and zero skips/errors/failures.
- Scope and whitespace checks: PASS; only `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java` and this artifact were edited. No trailing whitespace; artifact is UTF-8/LF with one terminal newline. The source test method inventory remains unchanged. The supplied workspace has no Git metadata, so scope was tracked by the explicit edit list rather than a Git diff.

## Boundaries

No production source, Control, plan, prior review/fix artifacts, V001–V010, OpenAPI, docs/12/14/15, POM/CI/README, public behavior, or later slice is changed. No stage, commit, push, PR, merge, approval, ready action, reviewer request, external comment, or re-review is performed.
