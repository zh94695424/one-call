# PA14 Slice 1 — Code-Review Fix 1 (B-1 Only)

## Authority and scope

- Task: `pa14-s1-code-review-fix1-gpt-20261105-021013`, November 5, 2026, Asia/Shanghai.
- Accepted plan: `docs/gateflow/pa14-sharing-command-service-foundation-plan.md`, owned implementation path at line 254.
- Implementation evidence read: `docs/gateflow/pa14-sharing-command-service-foundation-s1-implementation.md`.
- Reviewer A: `docs/reviews/code-review-20261104-223526.md`, PASS with zero material findings. Reviewer B: `docs/reviews/code-review-20261104-221740.md`, accepted B-1 filename/class inventory mismatch. Only B-1 authorizes this change; review observations and later-slice work are not implemented.
- The plan freezes non-exported `com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java`; the implementation used `CustomerShareCommandServiceImpl.java`. Rename the implementation and all production/test references to the frozen identity. No compatibility alias is retained.

## Exact changed-file inventory

Repository-relative paths; the rename is one logical file change:

1. Rename `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/CustomerShareCommandServiceImpl.java` to `backend/src/main/java/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.java`; change only class and constructor names.
2. `backend/src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceConfiguration.java`: update implementation import and constructor reference.
3. `backend/src/test/java/com/onecall/archtests/sharing/CustomerShareCommandServiceTests.java`: update import, construction and static helper references.
4. `backend/src/test/java/com/onecall/archtests/container/CustomerShareCommandsIT.java`: update import, construction and static helper references.
5. `backend/src/test/java/com/onecall/archtests/architecture/SharingQueryBoundaryTests.java`: replace the exact implementation filename in the architecture inventory.
6. Add this artifact, `docs/gateflow/pa14-sharing-command-service-foundation-s1-code-review-fix1.md`.

The five Java files retain their exact bytes except for replacement of the old implementation name with the frozen name. Constructor argument types/order, fields, helper signatures, behavior, transaction/security semantics, package exports and every test method/assertion are preserved. The old implementation file is absent. Historical implementation evidence and submitted reviews remain unchanged rather than rewriting their provenance.

## Environment and exact validation commands

All source work stays in the supplied isolated workspace. Temporary wrapper, baseline snapshots, extracted CI script and logs use `/tmp/pa14-s1-code-review-fix1/`; no other checkout or Git metadata is modified. This snapshot has no `.git` metadata (`git status --short` reports “not a git repository”), so whitespace validation uses Git's no-index diff mode and scope validation uses a pre-edit SHA-256 manifest of all 1,302 non-`target` files.

From workspace root, normalize only a temporary copy of the existing Maven wrapper and retain its original wrapper properties:

```sh
mkdir -p /tmp/pa14-s1-code-review-fix1/wrapper
sed 's/\r$//' backend/mvnw > /tmp/pa14-s1-code-review-fix1/wrapper/mvnw
ln -s "$PWD/backend/.mvn" /tmp/pa14-s1-code-review-fix1/wrapper/.mvn
export JAVA_HOME=/opt/jdk21 PATH=/opt/jdk21/bin:$PATH
cd backend
sh /tmp/pa14-s1-code-review-fix1/wrapper/mvnw -version
```

Result: Maven `3.9.9`, Eclipse Adoptium Java `21.0.13+9`, native Linux `/opt/jdk21`. The inherited Windows Java path is not used for builds. The workspace wrapper, POM and toolchain pins are unchanged.

Exact Maven commands from `backend/`, with the environment above:

```sh
set -o pipefail
sh /tmp/pa14-s1-code-review-fix1/wrapper/mvnw -B -ntp -Dtest=CustomerShareCommandServiceTests,SharingCommandPersistenceConfigurationTests,SharingQueryBoundaryTests clean test 2>&1 | tee /tmp/pa14-s1-code-review-fix1/focused.log
sh /tmp/pa14-s1-code-review-fix1/wrapper/mvnw -B -ntp -Pcontainer-smoke verify > /tmp/pa14-s1-code-review-fix1/full-container.log 2>&1
```

- Focused clean compilation/test: PASS, 90 production and 48 test source files compiled with release 21; 7 tests (4 command service + 2 persistence configuration + 1 exact architecture inventory), zero failures/errors/skips. Clean compilation removes any stale old-name class files.
- Full `-B -ntp -Pcontainer-smoke verify`: PASS, 77 Surefire tests across 16 classes plus 108 Failsafe tests across 6 classes, 185 combined, zero failures/errors/skips. All 8 `CustomerShareCommandsIT` methods pass. Maven elapsed time `02:18 min`, finished `2026-11-05T02:16:07+08:00`. Log: `/tmp/pa14-s1-code-review-fix1/full-container.log`. Negative migration/rollback fixtures intentionally log migration failures; the final test reports and Failsafe verify are green.
- Environment commands: `docker version --format 'Docker client {{.Client.Version}}; server {{.Server.Version}}'` and `docker image inspect postgres:16 --format 'Image {{.Id}}; tags {{json .RepoTags}}'` succeed. Docker client/server are `29.1.3`; actual Testcontainers executions report PostgreSQL `16.15` from `postgres:16`, image `sha256:9e2cf6287f25c3151f4c9421ccce2b17f3e60061c111a29165e0d2cdd9ff0a97`.

The existing CI inventory heredoc is extracted verbatim, removing only YAML indentation; `.github/workflows/ci.yml` is not edited. From workspace root:

```sh
python3 - <<'PY'
from pathlib import Path
import textwrap
workflow = Path('.github/workflows/ci.yml').read_text()
step = workflow.split('      - name: assert container smoke actually ran\n', 1)[1].split('      - name:', 1)[0]
script = step.split("python3 - <<'PY'\n", 1)[1].rsplit('          PY', 1)[0]
Path('/tmp/pa14-s1-code-review-fix1/inventory.py').write_text(textwrap.dedent(script))
PY
cd backend
python3 /tmp/pa14-s1-code-review-fix1/inventory.py | tee /tmp/pa14-s1-code-review-fix1/inventory.log
```

Result: PASS, exact source/CI/XML method and class identities, Surefire 77 and Failsafe `64+10+10+8+8+8=108`, combined 185, zero skips/errors/failures. This also checks for unexpected/missing source classes, duplicate/missing report cases and conditional/disabled tests. Log: `/tmp/pa14-s1-code-review-fix1/inventory.log`.

Additional packaged-class check, from workspace root:

```sh
python3 - <<'PY'
from pathlib import Path
import zipfile
archive = Path('backend/target/onecall-runtime-0.0.1-SNAPSHOT.jar')
with zipfile.ZipFile(archive) as product:
    names = product.namelist()
    assert 'BOOT-INF/classes/com/onecall/core/sharing/internal/commandservice/DefaultCustomerShareCommandService.class' in names
    assert not any('CustomerShareCommandServiceImpl' in name for name in names)
print('PASS: packaged JAR contains frozen implementation class, no old-name class or alias')
PY
```

Result: PASS; only the frozen implementation identity is packaged, with no old-name class or alias.

Final scope/whitespace command, from workspace root:

```sh
python3 /tmp/pa14-s1-code-review-fix1/validate-fix.py | tee /tmp/pa14-s1-code-review-fix1/scope-whitespace.log
```

The validation script compares each changed Java file byte-for-byte against its pre-edit snapshot after only the class-name substitution; checks no stale Java references/old file and the exact plan path; checks the full SHA-256 manifest permits only the five Java changes plus this artifact; then runs this equivalent whitespace check on aligned pre/post copies, including the new artifact:

```sh
git -c core.whitespace=blank-at-eol,blank-at-eof,space-before-tab,cr-at-eol diff --no-index --check /tmp/pa14-s1-code-review-fix1/whitespace-before /tmp/pa14-s1-code-review-fix1/whitespace-after
```

Result: PASS for all five byte-exact mechanical Java changes, no stale source references, exact accepted-plan implementation path, unchanged protected files and whitespace. Git no-index returns 1 because the compared trees differ, with no stdout/stderr whitespace diagnostics. The first scope-script invocation incorrectly treated that status as fatal; the validation-only harness now requires status 0 or 1 and empty stdout/stderr, rejecting whitespace diagnostics or command errors. No source adjustment was needed. Six logical changed files total (the implementation rename, four reference/inventory updates and this artifact); seven filesystem paths when representing the rename as removal plus addition. Log: `/tmp/pa14-s1-code-review-fix1/scope-whitespace.log`.

## Residual risks and stopping boundary

- No behavior/security/transaction change is intended or authorized; this fix resolves only the frozen implementation identity. Existing policy/KDF/TTL decisions and fail-closed production providers remain unchanged. Reviewer A's open questions and unrelated coverage observations remain outside B-1.
- Compilation emits existing JAXB annotation/deprecation and Mockito dynamic-agent warnings; none warrants an unrelated dependency/tooling change here.
- No accepted plan, Control, submitted review, V001–V010, OpenAPI, docs/12/14/15, README totals, API/query behavior, schema, package export or later-slice source is changed. No tests are added, weakened, disabled or skipped.
- No stage, commit, push, PR action, merge, approval, ready/reviewer request, external comment, re-review or further slice is performed. Stop at this bounded fix and its validation; no production enablement or acceptance is implied.
