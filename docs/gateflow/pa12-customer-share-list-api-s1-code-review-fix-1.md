# PA12 customer share list API — code-review fix 1

- Gate: `fix`
- Task: `pa12-code-review-fix1-gpt-final-report-20261102-205800`
- Workspace: `/tmp/pa12-code-review-fix1-workspace-20261102-1900`
- Status: `s1-code-review-fix-1-complete-pending-re-review`

## Scope and disposition

This fix addresses the accepted review findings through acceptance-test and documentation changes; it adds no production-code changes. Controller-verified accepted Primary F-01/F-02, A F1-F10, and B F-01-F-09 are implemented as applicable. B F-10 remains rejected by the Controller as Git-invisible EOL-only churn. This report records completion of the fix gate only; it does not claim fresh re-review acceptance.

Non-database coverage now includes the exact `OneCallApplication` `/pa12` context matrix; web-only provider-absent 503/default no-DB context; exact named-interface/module/member/consumer assertions; verifier/query-provider missing, ambiguous, construction, and outage matrices; duplicate bearer and query/header/cookie/body/attribute/principal spoof coverage; `CustomerSharePage.Item` serializer-fault clean 500 with no partial output; complete codec malformed/bounds/Unicode/max/actual-mapper assertions; expanded `/me` and configuration compatibility; and truthful README TRACE/named assertions.

Database coverage is recorded in `docs/gateflow/pa12-customer-share-list-api-s1-code-review-fix-1-db.md`: PostgreSQL fixture and integration coverage for ordering/cursors, ACTIVE lifecycle, ACL 42501/25006, no-write/secret snapshots, connection ownership, statement/mapping/commit/disconnect faults, and pool restoration.

## Changed paths

- `backend/README.md`
- `backend/src/test/java/com/onecall/archtests/CustomerMeWebTests.java`
- `backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java`
- `backend/src/test/java/com/onecall/archtests/CustomerWebConfigurationTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/CustomerApiBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`
- `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`
- `backend/src/test/java/com/onecall/core/customer_iam/internal/web/CustomerShareCursorCodecTests.java`
- `docs/gateflow/pa12-customer-share-list-api-s1-code-review-fix-1.md`

## Validation

- Controller focused run: 49/49 passed; zero failures, errors, or skips.
- Controller final clean run: `mvn -B -ntp clean -Pcontainer-smoke verify` completed with `BUILD SUCCESS` in 2:06.
- Surefire: 67 tests across 13 reports; Failsafe: 92 tests across 4 reports (`64+10+10+8`); combined 159, with zero failures, errors, or skips.
- PostgreSQL 16.15; Flyway V001-V009; `CustomerSharesIT` 8/8.
- Default and opt-in web packaged-JAR smokes passed.
- Production-JAR dependency firewall passed: exact baseline 37/34, web 55/48, 47 counterparts, 8 dependency-only, one packager, and 82 adversarial probes.
- `git diff --check` passed. Protected V001-V009/OpenAPI/design/security/protocol/acceptance/deployment/application-config diff is empty.

## Documentation decision

README TRACE guidance and named-interface assertions are truthful and aligned with the implemented checks. No production implementation change was needed for this fix.

## Residual risks

- No production verifier/session store/TTL: Identity/Backend/Security.
- No supporting list index/capacity: Backend/Database/Platform.
- Successful response buffering is unbounded: Backend/Web and Platform/Capacity.
- Future additional API routes must explicitly extend the filter allowlist: Backend/Architecture.
- The unkeyed cursor binds filters; it is not an authentication token: Backend/API and Security.

No commit, push, PR, approval, ready declaration, reviewer request, or reviewer comment is claimed.
