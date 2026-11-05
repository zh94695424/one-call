# PA14 sharing command foundation — aggregate review fix 3

- Task: `pa14-aggregate-deepreview-fix3-gpt-20261105-041301`.
- Date: 2026-11-05, Asia/Shanghai.
- Status: validated bounded fix; stopped without re-review or external actions.
- Authority: current `docs/gateflow/project-execution-control.md`, the accepted PA14 plan, and the bounded findings in `docs/reviews/code-review-20261105-035429.md` (A-01 through A-04) and `docs/reviews/code-review-20261105-041036.md` (finding 1). The user authorizes only these aggregate corrections; prior accepted work and deferred owner questions remain unchanged.

## Accepted corrections

### A-01 — shipped commit-failure rollback

`SharingCommandPersistenceConfiguration.sharingCommandTransactionManager` now enables `setRollbackOnCommitFailure(true)`. No other transaction settings change.

The existing `CustomerShareCommandsIT.createAppendFailureRollsBackGrantAuditAndOutbox` obtains the actual Spring-configured manager and template, asserts their shared identity and the shipped rollback setting, and substitutes only a test DataSource/connection proxy. Both that manager and the existing transaction-aware jOOQ context use the same proxy DataSource. The connection throws SQLState `08006` immediately before physical commit; it delegates actual rollback and all other connection operations. The test observes exactly one commit attempt and one rollback, confirms audit append was reached, asserts sanitized `INTERNAL`, and checks the database has no durable grant, audit or Outbox facts. It then restores the original DataSource and jOOQ connection provider before the pre-existing race assertions. No separately configured manager/template supplies the rollback behavior.

The existing `assertNoFacts` helper additionally checks Outbox `aggregate_id` directly for every attempted grant ID captured by the SQL probe. This detects orphan Outbox rows even when the corresponding grant does not exist; the prior join-only assertion could not. The existing failure helper also requires a cause-free exception with an exact code-only message, preventing database/secret canaries from escaping via either message or cause.

### Aggregate finding 1 — declared database material violations

The command service now classifies SQL exceptions found through runtime-exception causes and JDBC next-exception chains. Traversal is identity/cycle-safe and does not attach diagnostic exceptions to the outward error.

- SQLState `23514` maps to `SERVICE_UNAVAILABLE`, which the unchanged Problem writer maps to HTTP 503.
- SQLState `P0001` maps to `SERVICE_UNAVAILABLE` only for the exact declared V009 guard messages: `SHARE_GRANT_IMMUTABLE`, `SHARE_GRANT_EXPIRY_DECREASE`, `SHARE_GRANT_GENERATION_INVALID`, `SHARE_GRANT_CODE_ROTATION_INVALID`, `SHARE_GRANT_STATUS_TERMINAL`, `SHARE_GRANT_STATUS_TRANSITION`, `SHARE_GRANT_EXPIRY_INVALID`, `SHARE_GRANT_EXTENSION_INVALID`, `SHARE_GRANT_REVISION_INVALID`, `SHARE_ADMISSION_REFUSED`, and `SHARING_ROW_APPEND_ONLY`. PostgreSQL's `ERROR: ` prefix and subsequent context lines are handled without exposing them.
- SQLState `23505` remains `INTERNAL`, including `uq_share_grant_token_hash`; uniqueness takes precedence if encountered alongside a material error in the inspected chain. The pre-existing token lookup and real token uniqueness race remain frozen as `INTERNAL`.
- Unknown `P0001`, foreign-key `23503`, generic `XX000`, commit `08006`, and other unexpected runtime failures remain `INTERNAL`. Existing explicit command errors and transaction-start unavailability keep their prior handling.

Assertions are added only inside the existing append/commit-failure IT. A real PostgreSQL grant INSERT with invalid `maxAdmissions=0` exercises V009 CHECK/`23514`. A real, command-role-authorized expiry-decrease UPDATE after grant, Outbox and audit append exercises V009 `P0001`. The remaining declared guard messages and an unknown guard are injected as JDBC causes after actual audit append. A JDBC next-exception matrix covers `23514`, `23505` with the token constraint canary, `23503` and `XX000`, also after all three real writes. Each case asserts the exact sanitized code and no durable facts. No migration, ACL, constraint or trigger is changed or bypassed.

### A-02 — equivalent expiry text and replay

The existing `CustomerShareCommandServiceTests.createBuildsDeterministicIdentityAndFingerprint` now parses `2026-11-05T12:00:00.123456+02:00` to the same Instant as `2026-11-05T10:00:00.123456Z`, for every verification mode. It proves an offset-form retry replays the original state without insert, Outbox, audit, token or code calls. Resetting only the in-memory stored grant then exercises a fresh offset-form creation against the identical evidence/workspace fixture and compares deterministic share ID, creation fingerprint, scope digest, stored `creation.expiresAt` UTC text, typed expiry and returned state. A Z-form retry of that creation also replays. Random grant IDs/generated secrets remain outside the compared identity material.

### A-03 — ambiguous current binding and captured OPEN risk

The existing `CustomerShareCommandsIT.createRiskAndPolicyChecksPrecedeReplay` first seeds a second ACTIVE verified binding for the same subject and proves `SHARE_RISK_BLOCKED` before creation lookup, with no writes or durable facts. It retires that second binding, seeds evidence explicitly capturing it and an OPEN risk on it, and proves the captured-binding branch is also blocked without creation lookup, writes or durable facts. The captured risk query is observed under restored `sharing_command_role`. Existing current-binding risk, replay, captured-without-current success and policy assertions remain intact.

### A-04 — transition route rejection matrix

The existing `CustomerSharesWebTests.exactGetAndPostRoutesPassHighestPriorityFilter` now covers both `revocations` and `code-rotations` under `/pa12`:

- Exact-shape GET, HEAD, OPTIONS, PUT, PATCH and DELETE; the existing exact-shape POST rejection remains.
- POST extra segment, trailing slash, encoded share ID `%73hare:fixture`, encoded collection `%73hares`, share-ID segment `;v=1`, and wrong singular suffix.
- POST encoded suffix aliases `%72evocations` and `%63ode-rotations`.

Every case resets and asserts zero selection/verification counters. Non-HEAD responses use the existing exact Problem assertion: sanitized 404 `NOT_FOUND`, fixed title/body keys, trace ID and no-store/security headers. HEAD requires 404, an empty transport body and the same Problem content type/cache/security headers. No transition endpoint, header semantics, query behavior or public success response is implemented or changed.

## Exact write scope

1. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceConfiguration.java`.
2. `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java`.
3. `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java` — two existing test bodies, the existing no-facts/failure helpers, and removal of three obsolete imports only.
4. `backend/src/test/java/com/onecall/archtests/sharing/CustomerShareCommandServiceTests.java` — the existing deterministic identity/fingerprint test body only.
5. `backend/src/test/java/com/onecall/archtests/CustomerSharesWebTests.java` — the existing exact-route test body only.
6. This new fix-3 artifact.

No test method is added, removed or renamed. All other non-generated workspace paths are compared to a pre-edit SHA-256 manifest of 1,309 files. This supplied snapshot has no `.git` metadata; validation uses that manifest, original Java copies and Git's no-index whitespace check, not an index or another checkout. Scratch scripts/logs are outside the workspace at `/tmp/pa14-aggregate-fix3/`. Original CRLF bytes are retained for the five Java files; the new artifact is UTF-8 without BOM and uses LF.

## Runtime validation

Only a normalized temporary copy of the existing Maven wrapper is executed. The workspace wrapper and `.mvn` files are unchanged:

```sh
mkdir -p /tmp/pa14-aggregate-fix3/wrapper
sed 's/\r$//' backend/mvnw > /tmp/pa14-aggregate-fix3/wrapper/mvnw
ln -s "$PWD/backend/.mvn" /tmp/pa14-aggregate-fix3/wrapper/.mvn
export JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH
cd backend
sh /tmp/pa14-aggregate-fix3/wrapper/mvnw -version
sh /tmp/pa14-aggregate-fix3/wrapper/mvnw -B -ntp -Pcontainer-smoke -Dtest=CustomerShareCommandServiceTests,CustomerSharesWebTests,SharingCommandPersistenceConfigurationTests,SharingQueryBoundaryTests -Dit.test=CustomerShareCommandsIT verify > /tmp/pa14-aggregate-fix3/focused.log 2>&1
sh /tmp/pa14-aggregate-fix3/wrapper/mvnw -B -ntp -Pcontainer-smoke verify > /tmp/pa14-aggregate-fix3/full-container.log 2>&1
```

- Maven 3.9.9; Eclipse Adoptium Java 21.0.13 at `/opt/jdk21`; Docker client/server 29.1.3.
- Testcontainers uses `postgres:16`, image `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`; actual Flyway runtime reports PostgreSQL 16.15.
- Focused: PASS, 17 Surefire and all 8 command Failsafe tests, zero failures/errors/skips; 58.405 seconds, finished `2026-11-05T04:21:53+08:00`.
- Two initial fixture runs are retained: `focused-initial.log` exposed unrelated random evidence/workspace differences between independent unit fixtures, corrected by reusing the same fixture; `focused-acl-fixture.log` exposed the intentional command-role denial of UPDATE on immutable `max_admissions`, corrected by exercising the authorized expiry-decrease guard instead. No production/schema/ACL change was made to accommodate either fixture.
- Full `-B -ntp -Pcontainer-smoke verify`: PASS, 77 Surefire tests across 16 classes and 108 Failsafe tests across 6 classes, 185 combined, zero failures/errors/skips; 02:38 minutes, finished `2026-11-05T04:24:52+08:00`. All 8 command IT methods pass against PostgreSQL 16.15. Expected negative migration fixtures log rolled-back migration failures; the final XML reports and Failsafe verify are green. Log: `/tmp/pa14-aggregate-fix3/full-container.log`.
- Exact unchanged CI source/class/method/XML inventory: PASS, Surefire 77; Failsafe `64+10+10+8+8+8=108`; combined 185; zero skips/errors/failures. Log: `/tmp/pa14-aggregate-fix3/inventory.log`.
- Final scope/whitespace: PASS against all 1,309 original non-generated files. Exactly the six allowed paths differ; existing method identities and text outside the accepted test/helper bodies are preserved, aside from the three obsolete imports. All other workspace bytes, including protected contracts and prior artifacts, are unchanged. Original Java CRLF and new artifact UTF-8/LF, no BOM, no trailing whitespace and clean final newlines pass; Git no-index `diff --check` has no diagnostics. Log: `/tmp/pa14-aggregate-fix3/scope-whitespace.log`.

The exact Python inventory heredoc from `.github/workflows/ci.yml`, step `assert container smoke actually ran`, is extracted without content changes except removing YAML indentation into `/tmp/pa14-aggregate-fix3/inventory.py` and executed from `backend`:

```sh
(cd backend && python3 /tmp/pa14-aggregate-fix3/inventory.py) > /tmp/pa14-aggregate-fix3/inventory.log 2>&1
python3 /tmp/pa14-aggregate-fix3/validate-scope.py "$PWD" > /tmp/pa14-aggregate-fix3/scope-whitespace.log 2>&1
```

The scope script requires exactly the six paths above to differ, preserves all existing test identities and byte-equivalent text outside the explicitly named test/helper bodies (apart from the obsolete imports), verifies original Java CRLF encoding and clean final newlines, and runs:

```sh
git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --no-index --check /tmp/pa14-aggregate-fix3/whitespace-before /tmp/pa14-aggregate-fix3/whitespace-after
```

## Limits and stop

- Commit injection fails before physical commit. It proves the shipped manager attempts and completes rollback in that condition, not recovery of a transaction already committed by the server or an inherently ambiguous network outcome.
- Database classification is defense in depth; Java-side material validation, production fail-closed providers and unapproved production policy/KDF/TTL/HMAC/provisioning decisions remain unchanged. No production enablement or new HTTP command integration is claimed.
- Accepted plan, Control, prior review/fix artifacts, V001–V010, OpenAPI, docs/12/14/15, POM/CI/README, query behavior, public success responses and later-slice code remain untouched. Deferred design/evidence-owner questions are not resolved by this task.
- No staging, commit, push, PR action, merge, approval, ready action, reviewer request, external comment or re-review is performed. Stop after this bounded fix and validation.
