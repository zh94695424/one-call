# PA14 Slice 1 — Code-Review Fix 2 (A-F1 / A-F2)

## Authority and boundary

- Task: `pa14-s1-code-review-fix2-gpt-20261105-030254`, November 5, 2026, Asia/Shanghai.
- Inputs: accepted `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`, current `docs/gateflow/project-execution-control.md`, reviews `docs/reviews/code-review-20261105-023911.md` and `docs/reviews/code-review-20261105-025130.md`, existing slice-1 implementation and fix-1 artifacts, and docs/14 section 9.0's membership lifecycle rule.
- Only accepted A-F1 (medium) and A-F2 (low) are addressed. The current gate remains bounded fix 2; this artifact does not enter re-review or imply acceptance.

## Findings and changes

### A-F1 — Select the live membership lifecycle row

`JooqSharingCommandRepository.readActiveCallerContext` previously selected all memberships for the organization/account pair, including historical REVOKED rows. Add `AND m.status <> 'REVOKED'` to select the single live row guaranteed by V002's existing partial unique index. Continue requiring ACTIVE account, organization and membership and the existing allowed role vocabulary. Missing, revoked-only or SUSPENDED membership still fails closed. Advisory locks, privileges, personal-workspace behavior and grant/subject ownership semantics are unchanged.

### A-F2 — Expand existing PostgreSQL IT methods

No test or helper method is added, renamed, removed, disabled or parameterized. Only these existing bodies change:

- `createRiskAndPolicyChecksPrecedeReplay`: insert a schema-valid PENDING evidence record and its matching EVIDENCE_CREATED event in one transaction; assert sanitized `FORBIDDEN` (declared 403) and no grant/Outbox/audit append SQL.
- `createConcurrentSameAndDifferentIdentities`: revoke the existing membership; assert revoked-only organization create returns sanitized `FORBIDDEN` with no writes; insert a new ACTIVE membership for the same pair; verify PostgreSQL contains both REVOKED and ACTIVE rows; assert organization create succeeds and the stored grant retains the trusted account and organization owner.
- `evidenceProjectionUsesCommandTransactionConnection`: inject a subject mismatch into the repository reference after a real PostgreSQL reference read, keeping the real evidence projection and transaction; assert sanitized `SERVICE_UNAVAILABLE` (declared 503), with no append SQL. Also insert a distinct media row with identical projection metadata and a second DERIVED artifact to produce real projection ambiguity; assert the same sanitized 503/no-write outcome and restoration of `sharing_command_role`.

The existing `fails` assertion requires both the exact typed code and an exception message equal to that code, excluding SQL, identifiers and internal canaries from the exception message. These are command-service IT assertions; HTTP status rendering remains covered by the existing unchanged web tests, not a newly introduced HTTP server.

## Exact changed paths

1. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/JooqSharingCommandRepository.java` — only the non-revoked SQL predicate.
2. `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java` — only the three existing method bodies listed above.
3. `docs/gateflow/pa14-sharing-command-service-foundation-s1-code-review-fix2.md` — this new artifact.

## Environment and validation commands

This supplied snapshot has no `.git` metadata. A pre-edit SHA-256 manifest covers all 1,305 non-`target` files; temporary source snapshots, scripts, wrapper and logs reside outside the workspace under `/tmp/pa14-s1-code-review-fix2/`. No Git index or other checkout is used.

From workspace root, use only a normalized temporary copy of the existing Maven wrapper, preserving the original wrapper properties and all workspace wrapper bytes:

```sh
mkdir -p /tmp/pa14-s1-code-review-fix2/wrapper
sed 's/\r$//' backend/mvnw > /tmp/pa14-s1-code-review-fix2/wrapper/mvnw
ln -s "$PWD/backend/.mvn" /tmp/pa14-s1-code-review-fix2/wrapper/.mvn
export JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH
cd backend
sh /tmp/pa14-s1-code-review-fix2/wrapper/mvnw -version
sh /tmp/pa14-s1-code-review-fix2/wrapper/mvnw -B -ntp -Pcontainer-smoke -Dtest=CustomerShareCommandServiceTests,SharingCommandPersistenceConfigurationTests,SharingQueryBoundaryTests -Dit.test=CustomerShareCommandsIT verify > /tmp/pa14-s1-code-review-fix2/focused.log 2>&1
sh /tmp/pa14-s1-code-review-fix2/wrapper/mvnw -B -ntp -Pcontainer-smoke verify > /tmp/pa14-s1-code-review-fix2/full-container.log 2>&1
```

- Wrapper reports Maven 3.9.9 and Eclipse Adoptium Java 21.0.13, runtime `/opt/jdk21`.
- `docker version --format 'client {{.Client.Version}} server {{.Server.Version}}'`: client/server 29.1.3. `docker image inspect postgres:16 --format 'Image {{.Id}}; tags {{json .RepoTags}}'`: image `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`, tag `postgres:16`. Actual Testcontainers/Flyway execution reports PostgreSQL 16.15.
- Focused validation: PASS, 7 Surefire tests and all 8 `CustomerShareCommandsIT` methods, zero failures/errors/skips; elapsed 43.334 seconds, finished `2026-11-05T03:08:11+08:00`. An initial fixture attempt hit `uq_evidence_artifact` because it reused the same physical media row. Corrected only the fixture to use a distinct media row with duplicate projection metadata; no schema constraint or trigger was bypassed. The initial failed run is retained at `/tmp/pa14-s1-code-review-fix2/focused-initial.log`.

- Full `-B -ntp -Pcontainer-smoke verify`: PASS, 77 Surefire tests across 16 classes plus 108 Failsafe tests across 6 classes, 185 combined, zero failures/errors/skips. All 8 `CustomerShareCommandsIT` methods pass. Elapsed `02:17 min`, finished `2026-11-05T03:10:44+08:00`. Negative migration fixtures intentionally log rollback failures; the final reports and Failsafe verify are green. Log: `/tmp/pa14-s1-code-review-fix2/full-container.log`.
- Exact CI source/class/method/XML inventory: PASS, Surefire 77; Failsafe `64+10+10+8+8+8=108`; combined 185, zero skips/errors/failures. No inventory expectations or test identities change. Log: `/tmp/pa14-s1-code-review-fix2/inventory.log`.
- Final scope/whitespace: PASS against all 1,305 pre-edit non-generated files: exactly the three allowed paths differ; the production change is only the lifecycle predicate; all IT changes are confined to the three existing method bodies; every method identity and all unrelated/protected bytes are preserved. Git no-index reports no whitespace diagnostics. Log: `/tmp/pa14-s1-code-review-fix2/scope-whitespace.log`.

The exact CI Python inventory heredoc is extracted without changing its content, removing only YAML indentation. From workspace root:

```sh
python3 - <<'PY'
from pathlib import Path
import textwrap
workflow = Path('.github/workflows/ci.yml').read_text()
step = workflow.split('      - name: assert container smoke actually ran\n', 1)[1].split('      - name:', 1)[0]
script = step.split("python3 - <<'PY'\n", 1)[1].rsplit('          PY', 1)[0]
Path('/tmp/pa14-s1-code-review-fix2/inventory.py').write_text(textwrap.dedent(script))
PY
(cd backend && python3 /tmp/pa14-s1-code-review-fix2/inventory.py) > /tmp/pa14-s1-code-review-fix2/inventory.log 2>&1
python3 /tmp/pa14-s1-code-review-fix2/validate-scope.py > /tmp/pa14-s1-code-review-fix2/scope-whitespace.log 2>&1
```

The scope script requires exactly the three paths above to differ from the full pre-edit manifest, byte-exact production-file equality after only the one predicate substitution, identical method identities and byte-exact IT content outside the three existing methods. It checks aligned before/after copies, including this new artifact, with:

```sh
git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --no-index --check /tmp/pa14-s1-code-review-fix2/whitespace-before /tmp/pa14-s1-code-review-fix2/whitespace-after
```

Git no-index status 0 or 1 is accepted only with empty stdout/stderr; any whitespace diagnostic or command error fails validation.

## Residual risks and stop

- The reference inconsistency assertion uses a test-only spy after a real database read; real projection ambiguity is separately seeded in PostgreSQL without disabling constraints. No concurrent evidence mutation or HTTP end-to-end test is claimed.
- Production policy/KDF/TTL/HMAC/provisioning decisions and fail-closed providers remain unchanged. No production enablement is implied. Other review observations and later-slice behavior remain outside this task.
- Accepted plan, Control, prior review/fix artifacts, V001–V010, OpenAPI, docs/12/14/15, README inventory, POM/CI, query behavior and later-slice sources are not changed.
- No staging, commit, push, PR action, merge, approval, ready action, reviewer request, external comment, re-review or later slice is performed. Stop after this bounded fix and validation.
