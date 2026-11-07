# PA18 sharing media authorization and playback - code-review fix 1

- Work unit: `PA18-sharing-media-authorization-and-playback`
- Gate: `fix` after independent code review
- Controller: Phaseflow
- Implementation boundary: exactly 17 paths from the accepted plan; no schema, migration, ACL, OpenAPI, POM, frontend or production-policy expansion
- Status: `code-review fix complete; Controller intake and fresh A/B code re-review next`

## Controller decision

The accepted findings from code reviews A `docs/reviews/code-review-20261107-140455.md` and B `docs/reviews/code-review-20261107-143826.md` are assigned `fixed` for this bounded fix gate. Evidence consists of current source inspection, strengthened unit/web/architecture tests, real PostgreSQL production-wiring integration tests under the frozen V001-V012 schema, and the complete Linux Java 21 test runs recorded below. This decision does not replace the required independent re-review.

The review gate remains open until fresh A and B reviewers inspect the same frozen post-fix input and submit independently.

## Accepted finding disposition

| Finding | Final status | Evidence |
|---|---|---|
| A F-01 / B F-B18-01: all 30 PA18 tests were unconditional placeholders and the new path was never executed | `fixed` | All 30 exact named methods now execute behavioral or real boundary assertions: 9 service, 6 web, 1 architecture, 13 PostgreSQL media IT and 1 ACL IT. Repository-wide Java `assertThat(true)` count is zero. Surefire has 119 tests, Failsafe 176 including `PublicShareAdmissionIT=37` and `SharingAdmissionRoleLeastPrivilegeIT=9`. |
| A F-02 / B F-B18-02: authoritative `FOR UPDATE` used a read-only transaction and would return 503 | `fixed` | Non-locking preliminary lookup uses the read-only template; the authoritative grant lock and reads use the read-write transaction. The real PostgreSQL route success returns 200 and appends one success event. |
| A F-03 / B F-B18-04: grant lock preceded member lifecycle locks | `fixed` | Member organization, membership and account locks are acquired in rank order before `lockGrant`. Service call-trace and PostgreSQL member path tests cover the order. |
| A F-04 / B F-B18-03: ended session/admission mapped to policy-unavailable 503 | `fixed` | Authoritative and post-transaction checks return `SHARE_SESSION_EXPIRED`; missing/malformed/non-positive witness bounds retain `SHARE_SESSION_POLICY_UNAVAILABLE`. PostgreSQL denial precedence test covers both session and admission windows. |
| A F-05 / B F-B18-05: provider outage on a success replay appended FAILURE | `fixed` | A prior same-purpose/same-fingerprint success is tracked as replay. Provider outage returns `DEPENDENCY_UNAVAILABLE` without appending a later FAILURE; the first success fact remains unchanged. |
| A F-06 / B F-B18-06: FAILURE identity used the success ID plus `:failure` | `fixed` | FAILURE identity is independently computed as `share-access:` plus SHA-256 of canonical `{shareGrantId, shareAdmissionId, eventType=FAILURE, requestKeyDigest}`. Unit and PostgreSQL tests rederive and assert it. |
| A F-07 / B F-B18-07: generation mismatch preempted REVOKED/EXPIRED 410 behavior | `fixed` | Terminal status and grant time are evaluated before generation mismatch. PostgreSQL tests cover REVOKED, EXPIRED, time-expired, pure rotation and session/admission expiry. |
| A F-08: missing `valid_from` validation and incomplete PA17 predicate order | `fixed` | Current authority order is terminal/expiry, witness validity, future-valid/malformed impossible state, session/admission time, generation, member state, then captured/current binding and risk. Impossible future validity fails closed as `DEPENDENCY_UNAVAILABLE`. |
| A F-09 / B F-B18-11: selected workspace logic differed from PA17 | `fixed` | Controller requires an ORGANIZATION workspace with a non-null active organization and mirrors PA17's zero/multiple-active-organization `FORBIDDEN` behavior. Web matrix covers present, absent, unavailable, non-member and invalid-token cases. |
| A F-10: witness mode/ownership consistency was incomplete | `fixed` | Witness mode must match admission and grant; exact key sets and event identity/hash are checked; subject ownership is compared with grant owner/account. Malformed and changed-recipient cases deny without provider/event. |
| A F-11: a throwing provider bypassed durable failure | `fixed` | Runtime provider exceptions, `Unavailable`, `Malformed`, invalid credentials and out-of-bound expiry all append one deterministic redacted FAILURE before returning 503. |
| A F-12: PA18 UTC formatting differed from PA17 | `fixed` | Success and failure payloads use PA17's zero-or-exactly-six-fractional-digit UTC formatter; canonical identity assertions cover the output. |
| A F-13 / B F-B18-10: whole-file EOL churn obscured the logical diff | `fixed` | Every implementation path, the implementation report, Control and the code-fix artifact contain zero CR bytes in the working tree. Raw and `--ignore-space-at-eol` numstats agree; the logical diff is reviewable and `git diff --check` passes. |
| A F-14 / B F-B18-09 and B F-B18-08: implementation report and CI inventory text overstated evidence | `fixed` | The implementation report now distinguishes demonstrated behavior, bounded PostgreSQL proof, pending re-review and deferred production values. CI mechanically asserts actual-method `103+162=265 -> 119+176=295`, labelled comment-inclusive `266 -> 296`, exact source=CI=XML names and ending classes `37`/`9`; negative probes reject both recipes drifting. |

## Validation

Linux Java 21 Maven container `docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21`, Docker host networking, Testcontainers `postgres:16`:

- `mvn -q test`: Surefire `20` classes / `119` tests, zero failures/errors/skips.
- `mvn -q -Pcontainer-smoke verify`: Surefire `119/119`; Failsafe `9` classes / `176` tests, zero failures/errors/skips; combined `295`.
- PostgreSQL version: `16.15`, all 12 frozen migrations applied.
- Failsafe per-class ending counts: `PublicShareAdmissionIT=37`, `SharingAdmissionRoleLeastPrivilegeIT=9`.
- Exact workflow inventory executed against current source and XML: passed `20/119`, `9/176`, `295`, actual-method `103+162=265 -> 119+176=295`, comment-inclusive line scan `266 -> 296`, sole comment hit in `PostgresContainerIT`, negative probes rejected.
- `rg -n 'assertThat\(true\)' backend/src/test/java`: zero matches.
- `git diff --check`: exit 0.
- V001-V012, `api/openapi/onecall-v1.openapi.yaml` and `backend/pom.xml`: no diff/status changes.
- No `.pa18-*` or `.codex` private file is part of the working change set.

The verification Maven output includes expected negative-test log lines from `PostgresContainerIT` (for example an intentionally rejected V010 migration), followed by the test's successful rollback/recovery assertions. The authoritative XML is all green.

## Changed implementation paths

Implementation and CI allowlist (exact 17; 10 production with 3 new and 7 modified, 6 tests, 1 workflow):

1. `.github/workflows/ci.yml`
2. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java`
3. `backend/src/main/java/com/onecall/core/customer_iam/internal/web/PublicSharesController.java`
4. `backend/src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareAdmissionJsonBinder.java`
5. `backend/src/main/java/com/onecall/core/sharing/internal/publicadmission/MediaAuthorizationRequest.java`
6. `backend/src/main/java/com/onecall/core/sharing/internal/publicadmission/ShareMediaAuthorizationService.java`
7. `backend/src/main/java/com/onecall/core/sharing/internal/admission/DefaultShareMediaAuthorizationService.java`
8. `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareAdmissionRepository.java`
9. `backend/src/main/java/com/onecall/core/sharing/internal/admission/JooqShareAdmissionRepository.java`
10. `backend/src/main/java/com/onecall/core/sharing/internal/admission/ShareMediaCredentialProvider.java`
11. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/ShareAdmissionPersistenceConfiguration.java`

Existing tests (6):

12. `backend/src/test/java/com/onecall/archtests/sharing/ShareAdmissionServiceTests.java`
13. `backend/src/test/java/com/onecall/archtests/PublicShareWebTests.java`
14. `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`
15. `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`
16. `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java`
17. `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java`

The implementation report and this fix artifact are permitted evidence files outside the 17-path code allowlist. Control bookkeeping and the two submitted code-review artifacts are gate metadata.

## Residual risks

- Production Q22 TTL/skew/revocation/signing, Redis and media-provider values remain unavailable; the implementation fails closed and deterministic tests do not certify production provider idempotency. Owner/destination: Security, Platform and Sharing production-integration work.
- Media gateway/COS/object resolution, streaming/range delivery and buffered-byte recall remain outside PA18. Owner/destination: media delivery work unit.
- `SHARE_GENERATION_STALE` continues to carry the frozen operation's only listed 409 semantic-conflict code. This contract-level conflation is recorded and requires no PA18 code/schema change.
- Full remote CI and merge remain unverified/unperformed by this fix gate; validation is the recorded local Linux Java 21/PostgreSQL 16 execution.

## Completion

Evidence creation is complete for the `fix` gate. The Controller accepts all previously accepted findings as `fixed` for re-review purposes and will now freeze this exact working tree for fresh, peer-isolated A/B reviews.

`code-review fix complete; Controller intake and fresh A/B code re-review next`
